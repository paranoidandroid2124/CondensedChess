package lila.llm.analysis

import lila.llm.model.*

private[llm] enum QuietMoveIntentClass:
  case PieceImprovement
  case KingSafety
  case CounterplayRestraint
  case TechnicalConversionStep

private[llm] final case class QuietMoveIntentClaim(
    intentClass: QuietMoveIntentClass,
    claimText: String,
    evidenceLine: Option[String],
    sourceKind: String,
    quantifier: PlayerFacingClaimQuantifier = PlayerFacingClaimQuantifier.Existential,
    modalityTier: PlayerFacingClaimModalityTier = PlayerFacingClaimModalityTier.Available,
    attributionGrade: PlayerFacingClaimAttributionGrade = PlayerFacingClaimAttributionGrade.StateOnly,
    stabilityGrade: PlayerFacingClaimStabilityGrade = PlayerFacingClaimStabilityGrade.Unknown,
    provenanceClass: PlayerFacingClaimProvenanceClass = PlayerFacingClaimProvenanceClass.Deferred,
    certificateStatus: PlayerFacingCertificateStatus = PlayerFacingCertificateStatus.Invalid,
    taintFlags: Set[PlayerFacingClaimTaintFlag] = Set.empty,
    ontologyFamily: PlayerFacingClaimOntologyFamily = PlayerFacingClaimOntologyFamily.Unknown
):
  def lens: StrategicLens =
    intentClass match
      case QuietMoveIntentClass.CounterplayRestraint => StrategicLens.Prophylaxis
      case QuietMoveIntentClass.TechnicalConversionStep => StrategicLens.Practical
      case _ => StrategicLens.Structure

  def allowsUserFacing: Boolean =
    PlayerFacingClaimCertification.allowsWeakMainClaim(
      certificateStatus = certificateStatus,
      quantifier = quantifier,
      attribution = attributionGrade,
      stability = stabilityGrade,
      provenance = provenanceClass,
      taintFlags = taintFlags
    )

private[llm] object QuietMoveIntentBuilder:

  private final case class MoveShape(
      piece: String,
      targetSquare: Option[String],
      isCapture: Boolean,
      castlesShort: Boolean,
      castlesLong: Boolean
  )

  def build(
      ctx: NarrativeContext,
      candidateEvidenceLines: List[String] = Nil
  ): Option[QuietMoveIntentClaim] =
    val shape = moveShape(ctx)
    counterplayRestraint(ctx, candidateEvidenceLines)
      .orElse(kingSafety(ctx, shape, candidateEvidenceLines))
      .orElse(technicalConversionStep(ctx, shape, candidateEvidenceLines))
      .orElse(pieceImprovement(ctx, shape, candidateEvidenceLines))
      .filter(_.allowsUserFacing)

  def exactFactualSentence(ctx: NarrativeContext): Option[String] =
    moveShape(ctx).flatMap { shape =>
      if shape.castlesShort then Some("This castles.")
      else if shape.castlesLong then Some("This castles long.")
      else if shape.isCapture then
        shape.targetSquare.map(square => s"This captures on $square.")
          .orElse(Some("This captures."))
      else if shape.piece == "pawn" then
        shape.targetSquare.map(square => s"This is a pawn move to $square.")
      else
        shape.targetSquare.map(square => s"This puts the ${shape.piece} on $square.")
    }.flatMap(clean)

  private def counterplayRestraint(
      ctx: NarrativeContext,
      candidateEvidenceLines: List[String]
  ): Option[QuietMoveIntentClaim] =
    Option.unless(HeavyPieceLocalBindValidation.blocksPlayerFacingShell(ctx)) {
      val preventedPlans =
        ctx.semantic.toList.flatMap(_.preventedPlans)
      val certifiedFileEntryPair =
        LocalFileEntryBindCertification.certifiedSurfacePair(ctx)
      val primaryPlan =
        preventedPlans.find(plan =>
          plan.sourceScope == FactScope.Now &&
            (
              plan.counterplayScoreDrop > 0 ||
                plan.mobilityDelta < 0 ||
                plan.breakNeutralized.exists(_.trim.nonEmpty)
            )
        )
      certifiedFileEntryPair
        .map { pair =>
          certifyQuietClaim(
            ctx = ctx,
            anchorSquare = Some(pair.entrySquare),
            claim =
              QuietMoveIntentClaim(
                intentClass = QuietMoveIntentClass.CounterplayRestraint,
                claimText =
                  s"This keeps ${pair.entrySquare} closed and takes the ${pair.file} away as a counterplay route.",
                evidenceLine =
                  candidateEvidenceLines.flatMap(clean).find(lineMentionsFileEntryPair(_, pair))
                    .orElse(primaryPlan.flatMap(plan =>
                      candidateEvidenceLines.flatMap(clean).find(lineMentionsPreventedPlan(_, plan))
                    )),
                sourceKind = "prevented_plan"
              )
          )
        }
        .orElse {
          primaryPlan.flatMap { plan =>
            val claim =
              plan.breakNeutralized.flatMap(clean).map(file => s"This keeps $file from coming right away.")
                .orElse(
                  plan.deniedSquares.headOption.flatMap(clean).map(square => s"This keeps the opponent out of $square.")
                )
                .orElse(
                  plan.preventedThreatType.flatMap(clean).map(threat => s"This cuts out the opponent's $threat.")
                )
            claim.map { text =>
              certifyQuietClaim(
                ctx = ctx,
                anchorSquare = plan.deniedSquares.headOption,
                claim =
                  QuietMoveIntentClaim(
                    intentClass = QuietMoveIntentClass.CounterplayRestraint,
                    claimText = text,
                    evidenceLine = candidateEvidenceLines.flatMap(clean).find(lineMentionsPreventedPlan(_, plan)),
                    sourceKind = "prevented_plan"
                  )
              )
            }
          }
        }
    }.flatten

  private def kingSafety(
      ctx: NarrativeContext,
      shape: Option[MoveShape],
      candidateEvidenceLines: List[String]
  ): Option[QuietMoveIntentClaim] =
    shape.flatMap { move =>
      if move.castlesShort then
        Some(
          certifyQuietClaim(
            ctx = ctx,
            anchorSquare = move.targetSquare,
            claim =
              QuietMoveIntentClaim(
                intentClass = QuietMoveIntentClass.KingSafety,
                claimText = "This castles to keep the king safer.",
                evidenceLine = quietEvidenceLine(candidateEvidenceLines, move.targetSquare),
                sourceKind = "castle_short"
              )
          )
        )
      else if move.castlesLong then
        Some(
          certifyQuietClaim(
            ctx = ctx,
            anchorSquare = move.targetSquare,
            claim =
              QuietMoveIntentClaim(
                intentClass = QuietMoveIntentClass.KingSafety,
                claimText = "This castles long to tuck the king away and connect the rooks.",
                evidenceLine = quietEvidenceLine(candidateEvidenceLines, move.targetSquare),
                sourceKind = "castle_long"
              )
          )
        )
      else if move.piece == "king" then
        move.targetSquare.map { square =>
          certifyQuietClaim(
            ctx = ctx,
            anchorSquare = Some(square),
            claim =
              QuietMoveIntentClaim(
                intentClass = QuietMoveIntentClass.KingSafety,
                claimText =
                  if ctx.phase.current.equalsIgnoreCase("Endgame") then s"This improves the king for the ending by bringing it to $square."
                  else s"This improves the king's safety by stepping to $square.",
                evidenceLine = quietEvidenceLine(candidateEvidenceLines, Some(square)),
                sourceKind = "king_move"
              )
          )
        }
      else None
    }

  private def technicalConversionStep(
      ctx: NarrativeContext,
      shape: Option[MoveShape],
      candidateEvidenceLines: List[String]
  ): Option[QuietMoveIntentClaim] =
    val endgameish =
      ctx.phase.current.equalsIgnoreCase("Endgame") ||
        ctx.semantic.exists(_.endgameFeatures.isDefined)
    Option.when(endgameish) {
      shape.flatMap { move =>
        move.targetSquare.flatMap { square =>
          if move.piece == "king" then
            Some(
              certifyQuietClaim(
                ctx = ctx,
                anchorSquare = Some(square),
                claim =
                  QuietMoveIntentClaim(
                    intentClass = QuietMoveIntentClass.TechnicalConversionStep,
                    claimText = s"This is a technical conversion step, improving the king on $square.",
                    evidenceLine = quietEvidenceLine(candidateEvidenceLines, Some(square)),
                    sourceKind = "technical_king"
                  )
              )
            )
          else if !move.isCapture && move.piece != "pawn" then
            Some(
              certifyQuietClaim(
                ctx = ctx,
                anchorSquare = Some(square),
                claim =
                  QuietMoveIntentClaim(
                    intentClass = QuietMoveIntentClass.TechnicalConversionStep,
                    claimText = s"This is a technical conversion step, improving the ${move.piece} on $square.",
                    evidenceLine = quietEvidenceLine(candidateEvidenceLines, Some(square)),
                    sourceKind = "technical_piece"
                  )
              )
            )
          else None
        }
      }
    }.flatten

  private def pieceImprovement(
      ctx: NarrativeContext,
      shape: Option[MoveShape],
      candidateEvidenceLines: List[String]
  ): Option[QuietMoveIntentClaim] =
    shape.flatMap { move =>
      Option.when(!move.isCapture && !move.castlesShort && !move.castlesLong && move.piece != "pawn" && move.piece != "king") {
        move.targetSquare.map { square =>
          certifyQuietClaim(
            ctx = ctx,
            anchorSquare = Some(square),
            claim =
              QuietMoveIntentClaim(
                intentClass = QuietMoveIntentClass.PieceImprovement,
                claimText = s"This improves the ${move.piece} by placing it on $square.",
                evidenceLine = quietEvidenceLine(candidateEvidenceLines, Some(square)),
                sourceKind = "piece_improvement"
              )
          )
        }
      }.flatten
    }

  private def certifyQuietClaim(
      ctx: NarrativeContext,
      anchorSquare: Option[String],
      claim: QuietMoveIntentClaim
  ): QuietMoveIntentClaim =
    val provenanceClass = quietClaimProvenance(ctx)
    val quantifier = quietClaimQuantifier(ctx, provenanceClass)
    val stabilityGrade = quietClaimStability(ctx, provenanceClass)
    val attributionGrade = quietAttributionGrade(ctx, claim.intentClass, anchorSquare)
    val taintFlags = quietTaintFlags(ctx, provenanceClass, quantifier)
    val certificateStatus =
      if provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then PlayerFacingCertificateStatus.Invalid
      else if attributionGrade == PlayerFacingClaimAttributionGrade.StateOnly then PlayerFacingCertificateStatus.Invalid
      else if PlayerFacingClaimCertification.blocksMainClaim(taintFlags) then PlayerFacingCertificateStatus.Invalid
      else if quantifier == PlayerFacingClaimQuantifier.Universal then PlayerFacingCertificateStatus.Valid
      else PlayerFacingCertificateStatus.WeaklyValid
    claim.copy(
      quantifier = quantifier,
      modalityTier = quietModalityTier(claim.intentClass, quantifier),
      attributionGrade = attributionGrade,
      stabilityGrade = stabilityGrade,
      provenanceClass = provenanceClass,
      certificateStatus = certificateStatus,
      taintFlags = taintFlags,
      ontologyFamily = quietOntologyFamily(claim.intentClass)
    )

  private def quietClaimProvenance(
      ctx: NarrativeContext
  ): PlayerFacingClaimProvenanceClass =
    val evidenceBacked =
      StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx).nonEmpty ||
        ctx.strategicPlanExperiments.exists(_.evidenceTier == "evidence_backed")
    val pvCoupled =
      ctx.strategicPlanExperiments.exists(_.evidenceTier == "pv_coupled")
    val deferred =
      ctx.strategicPlanExperiments.exists(_.evidenceTier == "deferred") ||
        ctx.probeRequests.nonEmpty
    if evidenceBacked then PlayerFacingClaimProvenanceClass.ProbeBacked
    else if pvCoupled then PlayerFacingClaimProvenanceClass.PvCoupled
    else if deferred then PlayerFacingClaimProvenanceClass.Deferred
    else PlayerFacingClaimProvenanceClass.StructuralOnly

  private def quietClaimQuantifier(
      ctx: NarrativeContext,
      provenanceClass: PlayerFacingClaimProvenanceClass
  ): PlayerFacingClaimQuantifier =
    if provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingClaimQuantifier.LineConditioned
    else if ctx.strategicPlanExperiments.exists(exp =>
        exp.evidenceTier == "evidence_backed" &&
          exp.bestReplyStable &&
          exp.futureSnapshotAligned &&
          !exp.moveOrderSensitive
      ) then
      PlayerFacingClaimQuantifier.Universal
    else if ctx.strategicPlanExperiments.exists(exp =>
        exp.evidenceTier == "evidence_backed" &&
          (exp.bestReplyStable || exp.futureSnapshotAligned)
      ) then
      PlayerFacingClaimQuantifier.BestResponse
    else PlayerFacingClaimQuantifier.Existential

  private def quietClaimStability(
      ctx: NarrativeContext,
      provenanceClass: PlayerFacingClaimProvenanceClass
  ): PlayerFacingClaimStabilityGrade =
    if provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingClaimStabilityGrade.Unknown
    else if ctx.strategicPlanExperiments.exists(exp =>
        exp.evidenceTier == "evidence_backed" &&
          (exp.bestReplyStable || exp.futureSnapshotAligned) &&
          !exp.moveOrderSensitive
      ) then
      PlayerFacingClaimStabilityGrade.Stable
    else if ctx.strategicPlanExperiments.exists(_.evidenceTier == "evidence_backed") then
      PlayerFacingClaimStabilityGrade.Unstable
    else PlayerFacingClaimStabilityGrade.Unknown

  private def quietAttributionGrade(
      ctx: NarrativeContext,
      intentClass: QuietMoveIntentClass,
      anchorSquare: Option[String]
  ): PlayerFacingClaimAttributionGrade =
    val square = anchorSquare.flatMap(clean).map(normalize)
    val siblings = ctx.candidates.drop(1).take(3)
    if square.isEmpty && intentClass != QuietMoveIntentClass.KingSafety then
      PlayerFacingClaimAttributionGrade.StateOnly
    else if siblings.isEmpty then PlayerFacingClaimAttributionGrade.Distinctive
    else
      val keywords =
        intentClass match
          case QuietMoveIntentClass.CounterplayRestraint =>
            List("prevent", "deny", "counterplay", "keep out")
          case QuietMoveIntentClass.KingSafety =>
            List("castle", "king", "safe")
          case QuietMoveIntentClass.TechnicalConversionStep =>
            List("technical", "conversion", "improve")
          case QuietMoveIntentClass.PieceImprovement =>
            List("improve", "activate", "centralize")
      val shared = siblings.exists { candidate =>
        val low = candidateText(candidate)
        val squareHit = square.exists(low.contains)
        val keywordHits = keywords.count(low.contains)
        (squareHit && keywordHits >= 1) ||
          (intentClass == QuietMoveIntentClass.KingSafety && keywordHits >= 1)
      }
      if shared then PlayerFacingClaimAttributionGrade.AnchoredButShared
      else PlayerFacingClaimAttributionGrade.Distinctive

  private def quietTaintFlags(
      ctx: NarrativeContext,
      provenanceClass: PlayerFacingClaimProvenanceClass,
      quantifier: PlayerFacingClaimQuantifier
  ): Set[PlayerFacingClaimTaintFlag] =
    List(
      Option.when(provenanceClass == PlayerFacingClaimProvenanceClass.PvCoupled)(PlayerFacingClaimTaintFlag.PvCoupled),
      Option.when(provenanceClass == PlayerFacingClaimProvenanceClass.Deferred)(PlayerFacingClaimTaintFlag.Deferred),
      Option.when(provenanceClass == PlayerFacingClaimProvenanceClass.StructuralOnly)(PlayerFacingClaimTaintFlag.StructuralOnly),
      Option.when(quantifier == PlayerFacingClaimQuantifier.LineConditioned)(PlayerFacingClaimTaintFlag.BranchConditioned)
    ).flatten.toSet

  private def quietModalityTier(
      intentClass: QuietMoveIntentClass,
      quantifier: PlayerFacingClaimQuantifier
  ): PlayerFacingClaimModalityTier =
    intentClass match
      case QuietMoveIntentClass.CounterplayRestraint =>
        if quantifier == PlayerFacingClaimQuantifier.Existential then PlayerFacingClaimModalityTier.Supports
        else PlayerFacingClaimModalityTier.Removes
      case QuietMoveIntentClass.KingSafety =>
        if quantifier == PlayerFacingClaimQuantifier.Existential then PlayerFacingClaimModalityTier.Available
        else PlayerFacingClaimModalityTier.Supports
      case QuietMoveIntentClass.TechnicalConversionStep | QuietMoveIntentClass.PieceImprovement =>
        if quantifier == PlayerFacingClaimQuantifier.Existential then PlayerFacingClaimModalityTier.Available
        else PlayerFacingClaimModalityTier.Supports

  private def quietOntologyFamily(
      intentClass: QuietMoveIntentClass
  ): PlayerFacingClaimOntologyFamily =
    intentClass match
      case QuietMoveIntentClass.CounterplayRestraint => PlayerFacingClaimOntologyFamily.LongTermRestraint
      case QuietMoveIntentClass.KingSafety           => PlayerFacingClaimOntologyFamily.KingSafety
      case QuietMoveIntentClass.TechnicalConversionStep => PlayerFacingClaimOntologyFamily.TechnicalConversion
      case QuietMoveIntentClass.PieceImprovement     => PlayerFacingClaimOntologyFamily.PieceImprovement

  private def candidateText(candidate: CandidateInfo): String =
    normalize(
      List(
        candidate.move,
        candidate.planAlignment,
        candidate.structureGuidance.getOrElse(""),
        candidate.whyNot.getOrElse("")
      ).mkString(" ")
    )

  private def quietEvidenceLine(
      candidateEvidenceLines: List[String],
      anchorSquare: Option[String]
  ): Option[String] =
    candidateEvidenceLines
      .flatMap(clean)
      .find { line =>
        val low = normalize(line)
        anchorSquare.exists(square => low.contains(normalize(square))) ||
          containsAny(low, List("centralize", "improve", "activate", "king", "line:"))
      }

  private def lineMentionsPreventedPlan(text: String, plan: PreventedPlanInfo): Boolean =
    val low = normalize(text)
    plan.breakNeutralized.map(normalize).exists(low.contains) ||
      plan.deniedSquares.map(normalize).exists(low.contains) ||
      plan.preventedThreatType.map(normalize).exists(low.contains)

  private def lineMentionsFileEntryPair(
      text: String,
      pair: LocalFileEntryBindCertification.SurfacePair
  ): Boolean =
    val low = normalize(text)
    low.contains(normalize(pair.file)) && low.contains(normalize(pair.entrySquare))

  private def moveShape(ctx: NarrativeContext): Option[MoveShape] =
    ctx.playedSan.flatMap(normalizedSan).map { san =>
      val targetSquare = """([a-h][1-8])""".r.findAllMatchIn(san).map(_.group(1)).toList.lastOption
      val piece =
        san.headOption.collect {
          case 'K' => "king"
          case 'Q' => "queen"
          case 'R' => "rook"
          case 'B' => "bishop"
          case 'N' => "knight"
        }.getOrElse("pawn")
      MoveShape(
        piece = piece,
        targetSquare = targetSquare,
        isCapture = san.contains("x"),
        castlesShort = san == "O-O",
        castlesLong = san == "O-O-O"
      )
    }

  private def normalizedSan(raw: String): Option[String] =
    Option(raw)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replaceAll("""[!?+#]+$""", ""))

  private def clean(raw: String): Option[String] =
    Option(raw)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(UserFacingSignalSanitizer.sanitize)

  private def normalize(raw: String): String =
    Option(raw)
      .getOrElse("")
      .replace("**", "")
      .replaceAll("""[^\p{L}\p{N}\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .toLowerCase

  private def containsAny(text: String, needles: List[String]): Boolean =
    needles.exists(text.contains)
