package lila.commentary.analysis

import lila.commentary.*
import lila.commentary.model.NarrativeContext
import lila.commentary.analysis.claim.{ ClaimAuthorityResolver, ClaimAuthorityTier, OpeningFamilyClaimResolver }
import lila.commentary.analysis.semantic.{ RelationObservationCatalog, RelationObservationDescriptor }
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.analysis.structure.WeaknessTargetProfile
import lila.commentary.analysis.PlanEvidenceEvaluator.{ EvaluatedPlan, UserFacingPlanEligibility }
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme }
import chess.format.{ Fen, Uci }
import chess.variant.Standard

object MoveReviewPlayerPayloadBuilder:

  private val Schema = "chesstory.move_review.player_surface.v2"
  private val MinDecisionSurfaceGapCp = 35
  private val ClearDecisionSurfaceGapCp = 60
  private val ExactComparativeSource = "exact"
  private val MinWeaknessTargetOutcomePlies = 5
  private val SquareToken = """[a-h][1-8]""".r
  private val MoveReviewLedgerLineSources = Set("probe", "decision_compare", "variation", "authoring")

  def decisionComparisonSurface(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs]
  ): Option[MoveReviewPlayerDecisionComparison] =
    decisionComparisonSurface(
      comparison = DecisionComparisonBuilder.digest(ctx, refs),
      lineEvidence = LineConsequenceEvaluator.surfaceCandidate(ctx, refs)
    )

  private[analysis] def decisionComparisonSurface(
      comparison: Option[DecisionComparisonDigest],
      lineEvidence: Option[LineConsequenceEvidence]
  ): Option[MoveReviewPlayerDecisionComparison] =
    for
      digest <- comparison
      evidence <- lineEvidence
      if evidence.surfaceReady
      if hasComparableDecisionShape(digest, evidence)
      if hasDecisionSurfaceReason(digest)
      secondary <- safeDecisionSecondaryText(evidence)
    yield MoveReviewPlayerDecisionComparison(
      kicker = "Decision point",
      gapLabel = digest.cpLossVsChosen.map(decisionGapLabel),
      chosenSan = cleanMove(digest.chosenMove),
      engineSan = cleanMove(digest.engineBestMove),
      comparedSan = cleanMove(digest.comparedMove),
      deferredSan = None,
      secondaryText = Some(secondary),
      chosenMatchesBest = digest.chosenMatchesBest
    )

  def build(
      ctx: NarrativeContext,
      moveReviewExplanation: Option[MoveReviewExplanation],
      moveReviewLedger: Option[MoveReviewStrategicLedger],
      refs: Option[MoveReviewRefs],
      evaluatedPlans: List[EvaluatedPlan],
      authoringSurface: AuthoringEvidenceSurface,
      supportedLocalRows: List[MoveReviewPlayerSurfaceRow] = Nil,
      decisionComparisonSurface: Option[MoveReviewPlayerDecisionComparison] = None,
      strategyPack: Option[StrategyPack] = None
  ): MoveReviewPlayerSurface =
    val knownSans = refs.toList.flatMap(_.variations.flatMap(_.moves.map(_.san))).map(normalizeSan).toSet
    val promotedPlans = evaluatedPlans.filter(PlanEvidenceEvaluator.isMainAdmittedPlan).sortBy(_.hypothesis.rank)
    val practicalRows = practicalPlanRows(evaluatedPlans)
    val openingRows = openingFamilyRow(ctx).toList
    val relationRows = strategicRelationRows(strategyPack)
    val explanationRows = moveReviewExplanation.toList.flatMap(explanationSupportRows(_, knownSans))
    val referenceRows = if explanationRows.isEmpty then referenceLineRows(ctx, refs, knownSans) else Nil
    val summaryRows =
      (
        mainPlanRow(promotedPlans).toList ++
          openingRows ++
          practicalRows ++
          sanitizeRows(supportedLocalRows, knownSans) ++
          explanationRows ++
          referenceRows
      )
        .distinctBy(row => (row.label, row.text))
    MoveReviewPlayerSurface(
      schema = Schema,
      title =
        moveReviewExplanation.flatMap(explanation => cleanOpt(Some(explanation.title)))
          .orElse(ctx.playedSan.flatMap(san => cleanOpt(Some(s"Move review: $san")))),
      summaryRows = summaryRows,
      advancedRows =
        (relationRows ++ advancedRows(ctx, promotedPlans, evaluatedPlans))
          .distinctBy(row => (row.label, row.text, row.authority.flatMap(_.token)))
          .take(8),
      decisionComparison = decisionComparisonSurface.map(comparison =>
        sanitizeDecisionComparison(enrichDecisionTargetComparison(ctx, comparison))
      ),
      probeRows = ledgerRows(moveReviewLedger, knownSans),
      authorRows = authorRows(authoringSurface, knownSans)
    )

  private def strategicRelationRows(strategyPack: Option[StrategyPack]): List[MoveReviewPlayerSurfaceRow] =
    strategyPack.toList
      .flatMap(_.strategicIdeas)
      .flatMap(strategicRelationRow)
      .distinctBy(row => (row.authority.flatMap(_.token), row.authority.flatMap(_.target), row.text))
      .take(2)

  private def strategicRelationRow(idea: StrategyIdeaSignal): Option[MoveReviewPlayerSurfaceRow] =
    strategicRelationDescriptor(idea)
      .flatMap { descriptor =>
        val relation = descriptor.relationKind
        val focus = strategicRelationFocus(idea)
        if focus.isEmpty then None
        else
          val focusText = focus.take(3).mkString(", ")
          val text =
            if focusText.nonEmpty then s"The checked line gives ${descriptor.publicLabel} evidence around $focusText."
            else s"The checked line gives ${descriptor.publicLabel} evidence."
          row(
            label = descriptor.surfaceRowLabel,
            text = text,
            tone = Some("relation")
          ).map(
            _.copy(
              authority =
                Some(
                  MoveReviewSurfaceAuthority(
                    kind = MoveReviewSurfaceAuthority.StrategicRelation,
                    token = Some(relation),
                    target = relationTargetFromIdea(idea, descriptor, focus)
                  )
                )
            )
          )
      }

  private def strategicRelationDescriptor(idea: StrategyIdeaSignal) =
    RelationObservationCatalog.descriptorForEvidence(idea.relationKind, idea.evidenceRefs)

  private def strategicRelationFocus(idea: StrategyIdeaSignal): List[String] =
    idea.relationFocusSquares.flatMap(validSquare).distinct

  private def relationTargetFromIdea(
      idea: StrategyIdeaSignal,
      descriptor: RelationObservationDescriptor,
      focus: List[String]
  ): Option[String] =
    Option.when(idea.relationKind.nonEmpty)(idea.targetSquare)
      .flatten
      .flatMap(validSquare)
      .filter(focus.contains)
      .orElse(descriptor.fallbackTarget(focus))

  private def validSquare(raw: String): Option[String] =
    val cleaned = clean(raw).toLowerCase
    Option.when(cleaned.matches("""[a-h][1-8]"""))(cleaned)

  private def mainPlanRow(promotedPlans: List[EvaluatedPlan]): Option[MoveReviewPlayerSurfaceRow] =
    val plans =
      promotedPlans
        .flatMap(plan => cleanOpt(Some(plan.hypothesis.planName)))
        .distinct
        .take(2)
    row("Main plans", plans.mkString(" / "))

  private def advancedRows(
      ctx: NarrativeContext,
      promotedPlans: List[EvaluatedPlan],
      evaluatedPlans: List[EvaluatedPlan]
  ): List[MoveReviewPlayerSurfaceRow] =
    val planRows =
      promotedPlans.take(2).flatMap(planRowsFromPromotedPlan)
    val promotedRows = planRows.distinctBy(row => (row.label, row.text))
    (promotedRows ++ practicalAdvancedRows(ctx, evaluatedPlans, promotedPlans, slots = 8 - promotedRows.size)).take(8)

  private def planRowsFromPromotedPlan(plan: EvaluatedPlan): List[MoveReviewPlayerSurfaceRow] =
    val hypothesis = plan.hypothesis
    List(
      row("Execution", hypothesis.executionSteps.take(2).mkString(" - ")),
      row("Objective", hypothesis.preconditions.take(2).mkString(" - ")),
      Option.when(isProphylaxisPlan(plan)) {
        val text =
          cleanOpt(Some(hypothesis.planName))
            .orElse(cleanOpt(hypothesis.failureModes.headOption))
            .getOrElse("")
        row("Prophylaxis", text)
      }.flatten
    ).flatten

  private def practicalPlanRows(plans: List[EvaluatedPlan]): List[MoveReviewPlayerSurfaceRow] =
    plans
      .filter(PlanEvidenceEvaluator.isBoundedPracticalSupportPlan)
      .sortBy(_.hypothesis.rank)
      .flatMap(practicalPlanRow)
      .take(2)

  private def openingFamilyRow(ctx: NarrativeContext): Option[MoveReviewPlayerSurfaceRow] =
    for
      opening <- openingName(ctx)
      family <- OpeningFamilyCatalog.default.familiesForOpening(opening).headOption
      decision <- OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        OpeningFamilyClaimResolver.OpeningFamilyClaim(family.wireKey),
        OpeningFamilyClaimResolver.OpeningFamilyMatchProof(
          opening = Some(opening),
          phase = openingProofPhase(ctx),
          ply = ctx.ply,
          fen = rawOpt(ctx.fen)
        )
      )
      if decision.tier == ClaimAuthorityTier.SupportedLocal && decision.vetoReasons.isEmpty
      surfaceRow <- row(
        label = "Opening family",
        text = s"This still fits the ${family.structureLabel} structure.",
        tone = Some("opening")
      )
    yield surfaceRow.copy(
      authority =
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.OpeningFamily,
            openingFamily = Some(family.wireKey),
            target = openingRouteTarget(ctx, family.wireKey),
            openingBook = ctx.openingData.flatMap(MoveReviewOpeningBookMetadata.fromReference)
          )
        )
    )

  private def openingRouteTarget(ctx: NarrativeContext, familyKey: String): Option[String] =
    Fen.read(Standard, Fen.Full(ctx.fen)).flatMap { _ =>
      OpeningRouteCatalog.default.routes.iterator
        .filter(route => route.family == familyKey)
        .filter(route => OpeningFamilyCatalog.default.targetAllowed(familyKey, route.targetSquare))
        .flatMap(route =>
          PieceRouteEvidence
            .fromContext(ctx, route)
            .filter(OpeningRouteTargetEvidence.checkRouteEvidence)
            .map(_ => route.targetSquare)
        )
        .toList
        .headOption
    }

  private def openingName(ctx: NarrativeContext): Option[String] =
    cleanOpt(ctx.openingData.flatMap(_.name))
      .orElse(openingEventName(ctx).flatMap(name => cleanOpt(Some(name))))

  private def openingEventName(ctx: NarrativeContext): Option[String] =
    ctx.openingEvent.collect {
      case lila.commentary.model.OpeningEvent.Intro(_, name, _, _) => name
    }

  private def openingProofPhase(ctx: NarrativeContext): String =
    rawOpt(ctx.phase.current).orElse(rawOpt(ctx.header.phase)).getOrElse("")

  private def practicalPlanRow(plan: EvaluatedPlan): Option[MoveReviewPlayerSurfaceRow] =
    cleanOpt(Some(plan.hypothesis.planName)).flatMap { name =>
      val text =
        if plan.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly then
          s"The structure points toward $name as a practical plan."
        else s"The checked line keeps $name viable as a practical plan."
      row(
        label = "Practical plan",
        text = text,
        tone = Some("practical")
      ).map(_.copy(authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))))
    }

  private def practicalAdvancedRows(
      ctx: NarrativeContext,
      plans: List[EvaluatedPlan],
      promotedPlans: List[EvaluatedPlan],
      slots: Int
  ): List[MoveReviewPlayerSurfaceRow] =
    if slots <= 0 then Nil
    else
      plans
        .filter(PlanEvidenceEvaluator.isBoundedPracticalSupportPlan)
        .filterNot(plan => hasPromotedSibling(plan, promotedPlans))
        .sortBy(_.hypothesis.rank)
        .flatMap(plan => practicalAdvancedRowsForPlan(ctx, plan))
        .distinctBy(row => (row.label, row.text))
        .take(slots)

  private def hasPromotedSibling(plan: EvaluatedPlan, promotedPlans: List[EvaluatedPlan]): Boolean =
    promotedPlans.exists(promoted =>
      sameTheme(plan, promoted) || executionOverlapRatio(plan, promoted) >= 0.7
    )

  private def sameTheme(left: EvaluatedPlan, right: EvaluatedPlan): Boolean =
    val leftTheme = cleanToken(left.hypothesis.themeL1)
    val rightTheme = cleanToken(right.hypothesis.themeL1)
    leftTheme.nonEmpty && leftTheme == rightTheme

  private def executionOverlapRatio(practical: EvaluatedPlan, promoted: EvaluatedPlan): Double =
    val practicalAtoms = executionAtoms(practical.hypothesis.executionSteps)
    if practicalAtoms.isEmpty then 0.0
    else
      val promotedAtoms = executionAtoms(promoted.hypothesis.executionSteps).toSet
      practicalAtoms.count(promotedAtoms.contains).toDouble / practicalAtoms.size.toDouble

  private def executionAtoms(steps: List[String]): List[String] =
    steps.flatMap { step =>
      val token = cleanToken(step)
      val squares = SquareToken.findAllIn(token).toList
      if squares.nonEmpty then squares else List(token).filter(_.nonEmpty)
    }.distinct

  private def cleanToken(value: String): String =
    value.toLowerCase.replaceAll("""[^a-z0-9]+""", " ").trim

  private def practicalAdvancedRowsForPlan(ctx: NarrativeContext, plan: EvaluatedPlan): List[MoveReviewPlayerSurfaceRow] =
    val hypothesis = plan.hypothesis
    List(
      practicalTargetRow(ctx, plan),
      row("Practical objective", hypothesis.preconditions.take(2).mkString(" - "), tone = Some("practical")),
      row("Practical steps", hypothesis.executionSteps.take(2).mkString(" - "), tone = Some("practical"))
    ).flatten.map(_.copy(authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))))

  private def practicalTargetRow(
      ctx: NarrativeContext,
      plan: EvaluatedPlan
  ): Option[MoveReviewPlayerSurfaceRow] =
    Option.when(isWeaknessPlan(plan)) {
      WeaknessTargetProfile.fromFenForMover(ctx.fen).find(target => practicalTargetSurvivesBestLine(ctx, target)).flatMap { target =>
        val text =
          s"The current structure gives ${sideLabel(target.weakSide)} a weak ${targetKindLabel(target.kind)} on ${target.targetSquare} to pressure."
        row("Practical target", text, tone = Some("practical"))
      }
    }.flatten

  private def isWeaknessPlan(plan: EvaluatedPlan): Boolean =
    plan.themeL1 == PlanTheme.WeaknessFixation.id ||
      plan.subplanId.exists(id =>
        Set(
          PlanKind.StaticWeaknessFixation.id,
          PlanKind.MinorityAttackFixation.id,
          PlanKind.BackwardPawnTargeting.id,
          PlanKind.IQPInducement.id
        ).contains(id)
      )

  private def sideLabel(side: _root_.chess.Color): String =
    if side.white then "White" else "Black"

  private def targetKindLabel(kind: String): String =
    kind match
      case WeaknessTargetProfile.BackwardPawn => "backward pawn"
      case WeaknessTargetProfile.IsolatedPawn => "isolated pawn"
      case WeaknessTargetProfile.IQP          => "isolated queen pawn"
      case WeaknessTargetProfile.DoubledPawn  => "doubled pawn"
      case WeaknessTargetProfile.FixedPawn    => "fixed pawn"
      case _                                  => "pawn"

  private def practicalTargetSurvivesBestLine(
      ctx: NarrativeContext,
      target: WeaknessTargetProfile
  ): Boolean =
    ctx.engineEvidence.flatMap(_.best) match
      case None => true
      case Some(line) =>
        WeaknessTargetProfile
          .lineOutcomeFromFen(
            fen = ctx.fen,
            moves = line.moves,
            targetSquare = target.targetSquare,
            resultingFen = line.resultingFen
          )
          .exists { outcome =>
            outcome.status == WeaknessTargetProfile.ResolvedByPressure ||
              (
                outcome.status == WeaknessTargetProfile.Persistent &&
                  (line.resultingFen.nonEmpty || line.moves.size >= MinWeaknessTargetOutcomePlies)
              )
          }

  private def enrichDecisionTargetComparison(
      ctx: NarrativeContext,
      comparison: MoveReviewPlayerDecisionComparison
  ): MoveReviewPlayerDecisionComparison =
    if comparison.targetComparison.nonEmpty || comparison.chosenMatchesBest then comparison
    else comparison.copy(targetComparison = bestVsChosenTargetComparison(ctx))

  private def bestVsChosenTargetComparison(ctx: NarrativeContext): Option[MoveReviewDecisionTargetComparison] =
    for
      evidence <- ctx.engineEvidence
      bestLine <- evidence.best
      chosenMove <- ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
      if bestLine.moves.headOption.map(NarrativeUtils.normalizeUciMove).forall(_ != chosenMove)
      chosenLine <- evidence.variations.find(line =>
        line.moves.headOption.map(NarrativeUtils.normalizeUciMove).contains(chosenMove)
      )
      bestTarget <- lineTarget(ctx, bestLine)
      chosenTarget <- lineTarget(ctx, chosenLine)
      if bestTarget.targetSquare != chosenTarget.targetSquare || bestTarget.kind != chosenTarget.kind
    yield MoveReviewDecisionTargetComparison(
      chosenTarget = chosenTarget.targetSquare,
      chosenTargetKind = chosenTarget.kind,
      bestTarget = bestTarget.targetSquare,
      bestTargetKind = bestTarget.kind
    )

  private def lineTarget(
      ctx: NarrativeContext,
      line: lila.commentary.model.strategic.VariationLine
  ): Option[WeaknessTargetProfile] =
    WeaknessTargetProfile
      .targetsAfterLineFromFen(
        fen = ctx.fen,
        moves = line.moves,
        resultingFen = line.resultingFen
      )
      .headOption

  private def explanationSupportRows(
      explanation: MoveReviewExplanation,
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    val sanMoves = cleanList(explanation.shortLine.toList.flatMap(_.san))
    val lineText = Option.when(sanMoves.nonEmpty)(s"Short line: ${sanMoves.mkString(" ")}.")
    val learningPoint =
      explanation.pvInterpretation
        .flatMap(interpretation => cleanOpt(Some(interpretation.learningPoint)))
        .map(value => value.replaceAll("""\.+$""", ""))
    lineText.toList.flatMap { line =>
      val text = learningPoint.fold(line)(point => s"$point. $line")
      row(
        label = "Checked line",
        text = text,
        refSans = refSans(sanMoves, knownSans),
        tone = Some("line")
      )
    }

  private def referenceLineRows(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    refs.toList
      .flatMap(ref => preferredVariation(ctx, ref).toList)
      .flatMap { variation =>
        val sanMoves = cleanList(variation.moves.map(_.san)).take(5)
        Option.when(sanMoves.nonEmpty) {
          row(
            label = "Checked line",
            text = s"Short line: ${sanMoves.mkString(" ")}.",
            refSans = refSans(sanMoves, knownSans),
            tone = Some("line")
          )
        }.flatten
      }

  private def preferredVariation(
      ctx: NarrativeContext,
      refs: MoveReviewRefs
  ): Option[MoveReviewVariationRef] =
    val playedUci = ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
    val playedSan = ctx.playedSan.map(normalizeMove).filter(_.nonEmpty)
    refs.variations
      .find { variation =>
        variation.moves.headOption.exists { move =>
          playedUci.exists(_ == NarrativeUtils.normalizeUciMove(move.uci)) ||
            playedSan.exists(_ == normalizeMove(move.san))
        }
      }
      .orElse(refs.variations.headOption)

  private def ledgerRows(
      moveReviewLedger: Option[MoveReviewStrategicLedger],
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    moveReviewLedger.toList.flatMap { ledger =>
      List(ledger.primaryLine, ledger.resourceLine).flatten.flatMap { line =>
        val source = clean(line.source)
        val sanMoves = cleanList(line.sanMoves)
        Option.when(MoveReviewLedgerLineSources.contains(source) && sanMoves.nonEmpty) {
          val text =
            cleanOpt(line.note)
              .orElse(cleanOpt(Some(sanMoves.mkString(" "))))
          row(
            label = line.title,
            text = text.getOrElse(""),
            refSans = refSans(sanMoves, knownSans)
          )
        }.flatten
      }
    }

  private def authorRows(
      authoringSurface: AuthoringEvidenceSurface,
      knownSans: Set[String]
  ): List[MoveReviewPlayerAuthorRow] =
    val evidenceRows =
      authoringSurface.evidence.take(2).flatMap(summary => authorEvidenceRow(summary, knownSans))
    if evidenceRows.nonEmpty then evidenceRows
    else authoringSurface.questions.take(2).flatMap(authorQuestionRow)

  private def authorEvidenceRow(
      summary: AuthorEvidenceSummary,
      knownSans: Set[String]
  ): Option[MoveReviewPlayerAuthorRow] =
    val question = cleanOpt(Some(summary.question))
    question.map { q =>
      MoveReviewPlayerAuthorRow(
        title = humanizeToken(summary.questionKind),
        status = clean(summary.status),
        question = q,
        why = cleanOpt(summary.why),
        meta = Nil,
        branches =
          summary.branches.take(2).flatMap { branch =>
            row(
              label = branch.keyMove,
              text = branch.line,
              refSans = refSans(List(branch.keyMove), knownSans)
            )
          }
      )
    }

  private def authorQuestionRow(summary: AuthorQuestionSummary): Option[MoveReviewPlayerAuthorRow] =
    cleanOpt(Some(summary.question)).map { question =>
      MoveReviewPlayerAuthorRow(
        title = humanizeToken(summary.kind),
        status = "question_only",
        question = question,
        why = cleanOpt(summary.why),
        meta = Nil
      )
    }

  private def isProphylaxisPlan(plan: EvaluatedPlan): Boolean =
    plan.themeL1 == PlanTheme.RestrictionProphylaxis.id ||
      plan.subplanId.exists(id =>
        Set(
          PlanKind.ProphylaxisRestraint.id,
          PlanKind.BreakPrevention.id,
          PlanKind.FlankClamp.id,
          PlanKind.CentralSpaceBind.id,
          PlanKind.MobilitySuppression.id,
          PlanKind.KeySquareDenial.id
        ).contains(id)
      )

  private def row(
      label: String,
      text: String,
      refSans: List[String] = Nil,
      tone: Option[String] = None
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      cleanLabel <- cleanOpt(Some(label))
      cleanText <- cleanOpt(Some(text))
    yield MoveReviewPlayerSurfaceRow(
      label = cleanLabel,
      text = cleanText,
      tone = tone.flatMap(value => cleanOpt(Some(value))),
      source = None,
      refSans = cleanList(refSans)
    )

  private def sanitizeRows(
      rows: List[MoveReviewPlayerSurfaceRow],
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    rows.flatMap { sourceRow =>
      for
        cleanLabel <- cleanOpt(Some(sourceRow.label))
        cleanText <- cleanOpt(Some(sourceRow.text))
      yield MoveReviewPlayerSurfaceRow(
        label = cleanLabel,
        text = cleanText,
        tone = sourceRow.tone.flatMap(value => cleanOpt(Some(value))),
        source = None,
        refSans = refSans(sourceRow.refSans, knownSans),
        authority = sourceRow.authority
      )
    }

  private def sanitizeDecisionComparison(
      comparison: MoveReviewPlayerDecisionComparison
  ): MoveReviewPlayerDecisionComparison =
    comparison.copy(
      kicker = clean(comparison.kicker),
      gapLabel = cleanOpt(comparison.gapLabel),
      chosenSan = cleanOpt(comparison.chosenSan),
      engineSan = cleanOpt(comparison.engineSan),
      comparedSan = cleanOpt(comparison.comparedSan),
      deferredSan = cleanOpt(comparison.deferredSan),
      secondaryText = cleanOpt(comparison.secondaryText)
    )

  private def hasTwoComparableMoves(digest: DecisionComparisonDigest): Boolean =
    List(digest.chosenMove, digest.engineBestMove, digest.comparedMove)
      .flatten
      .flatMap(move => cleanMove(Some(move)))
      .map(normalizeMove)
      .distinct
      .size >= 2

  private def hasComparableDecisionShape(
      digest: DecisionComparisonDigest,
      evidence: LineConsequenceEvidence
  ): Boolean =
    hasTwoComparableMoves(digest) || hasLaterLineDivergence(digest, evidence)

  private def hasLaterLineDivergence(
      digest: DecisionComparisonDigest,
      evidence: LineConsequenceEvidence
  ): Boolean =
    val compared = cleanMove(digest.comparedMove)
    val anchorMoves = List(digest.chosenMove, digest.engineBestMove).flatten.flatMap(move => cleanMove(Some(move)))
    compared.exists { comparedMove =>
      anchorMoves.exists(anchor => normalizeMove(anchor) == normalizeMove(comparedMove)) &&
        evidence.kind != LineConsequenceKind.PreviewOnly &&
        triggerPly(evidence).exists(_ > 1)
    }

  private def hasDecisionSurfaceReason(digest: DecisionComparisonDigest): Boolean =
    digest.cpLossVsChosen.exists(_ >= MinDecisionSurfaceGapCp) ||
      digest.practicalAlternative ||
      (
        digest.comparativeConsequence.exists(_.trim.nonEmpty) &&
          digest.comparativeSource.exists(_.toLowerCase.contains(ExactComparativeSource))
      )

  private def decisionGapLabel(cp: Int): String =
    if cp >= ClearDecisionSurfaceGapCp then s"${cp}cp"
    else s"${cp}cp slight"

  private def triggerPly(evidence: LineConsequenceEvidence): Option[Int] =
    evidence.triggerSan
      .flatMap(trigger =>
        evidence.sanMoves.indexWhere(san => normalizeMove(san) == normalizeMove(trigger)) match
          case -1  => None
          case idx => Some(idx + 1)
      )

  private def safeDecisionSecondaryText(evidence: LineConsequenceEvidence): Option[String] =
    Option
      .when(evidence.kind != LineConsequenceKind.PreviewOnly)(clean(evidence.playerSentence).trim)
      .filter(_.nonEmpty)

  private def cleanMove(raw: Option[String]): Option[String] =
    raw.map(clean).map(_.trim).filter(_.nonEmpty)

  private def normalizeMove(raw: String): String =
    raw.replaceAll("""[+#?!]+$""", "").toLowerCase

  private def refSans(sans: List[String], knownSans: Set[String]): List[String] =
    val cleaned = cleanList(sans)
    if knownSans.isEmpty then cleaned
    else cleaned.filter(san => knownSans.contains(normalizeSan(san)))

  private def clean(value: String): String =
    UserFacingSignalSanitizer.sanitize(value)

  private def cleanOpt(value: Option[String]): Option[String] =
    value.map(clean).map(_.trim).filter(_.nonEmpty)

  private def cleanList(values: List[String]): List[String] =
    values.flatMap(value => cleanOpt(Some(value))).distinct

  private def rawOpt(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.nonEmpty)

  private def normalizeSan(value: String): String =
    Option(value).getOrElse("").replaceAll("[+#?!]+", "").trim.toLowerCase

  private def humanizeToken(raw: String): String =
    clean(raw.replaceAll("([a-z])([A-Z])", "$1 $2").replace("_", " ").replace("-", " "))
      .split("\\s+")
      .filter(_.nonEmpty)
      .map(part => part.take(1).toUpperCase + part.drop(1).toLowerCase)
      .mkString(" ")

private[commentary] object NeutralizeKeyBreakSurfaceGate:

  val MissingNamedBreak = "surface:named_break_missing"
  val PlayedMoveCollision = "surface:played_move_collision"
  val PlayedMoveUnverified = "surface:played_move_unverified"

  final case class Decision(token: Option[String], rejectReason: Option[String]):
    def admitted: Boolean = token.nonEmpty && rejectReason.isEmpty

  def decideForPlanPacket(
      plan: QuestionPlan,
      packet: PlayerFacingClaimPacket,
      ctx: NarrativeContext
  ): Decision =
    val planToken = plan.timingWitness.flatMap(_.namedBreak).flatMap(canonicalToken)
    val packetToken = packetBreakToken(packet)
    (planToken, packetToken) match
      case (Some(left), Some(right)) if left == right =>
        decideToken(left, Some(ctx))
      case _ =>
        Decision(None, Some(MissingNamedBreak))

  def decideForPacket(
      packet: PlayerFacingClaimPacket,
      ctx: NarrativeContext
  ): Decision =
    decideForPacket(packet, Some(ctx))

  def decideForPacket(
      packet: PlayerFacingClaimPacket,
      ctx: Option[NarrativeContext]
  ): Decision =
    packetBreakToken(packet)
      .map(token => decideToken(token, ctx))
      .getOrElse(Decision(None, Some(MissingNamedBreak)))

  def surfaceText(token: String): String =
    s"On the checked line, this stops the $token break before it appears."

  private def packetBreakToken(packet: PlayerFacingClaimPacket): Option[String] =
    packet.proofPathWitness.exactSliceProof.collect {
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken) => breakToken
    }.flatMap(canonicalToken)

  private def decideToken(token: String, ctx: Option[NarrativeContext]): Decision =
    singleSquareToken(token) match
      case Some(square) =>
        ctx.flatMap(legalPlayedTargetSquare) match
          case Some(target) if target == square => Decision(None, Some(PlayedMoveCollision))
          case Some(_)                         => Decision(Some(token), None)
          case None                            => Decision(None, Some(PlayedMoveUnverified))
      case None => Decision(Some(token), None)

  private def canonicalToken(raw: String): Option[String] =
    val lower = Option(raw).map(_.trim.toLowerCase).getOrElse("")
    if lower.isEmpty ||
        lower.contains("_") ||
        lower.contains(":") ||
        lower.contains("|") ||
        lower.contains(" ") ||
        lower.contains("counterplay") ||
        lower.contains("neutralize")
    then None
    else
      val hasEllipsis = lower.startsWith("...")
      val core = if hasEllipsis then lower.drop(3) else lower
      Option.when(core.matches("""[a-h][1-8]""") || core.matches("""[a-h][1-8]-[a-h][1-8]""")) {
        s"${if hasEllipsis then "..." else ""}$core"
      }

  private def singleSquareToken(token: String): Option[String] =
    val core = token.stripPrefix("...")
    Option.when(core.matches("""[a-h][1-8]"""))(core)

  private def legalPlayedTargetSquare(ctx: NarrativeContext): Option[String] =
    for
      position <- Fen.read(Standard, Fen.Full(ctx.fen))
      uci <- ctx.playedMove.map(_.trim.toLowerCase).filter(_.nonEmpty)
      move <- Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)
    yield move.dest.key

private[commentary] object CentralBreakTimingSurfaceGate:

  val MissingExactWitness = "surface:central_break_exact_witness_missing"
  val MalformedBreakToken = "surface:central_break_token_malformed"

  final case class Decision(token: Option[String], rejectReason: Option[String]):
    def admitted: Boolean = token.nonEmpty && rejectReason.isEmpty

  def decide(witness: CentralBreakTimingWitness.Witness): Decision =
    val token = Option(witness.breakToken).map(_.trim).filter(_.nonEmpty)
    token match
      case Some(value) =>
        classifyRouteToken(value) match
          case RouteToken.Valid(canonical) => Decision(Some(canonical), None)
          case RouteToken.Malformed        => Decision(None, Some(MalformedBreakToken))
      case None =>
        Decision(None, Some(MissingExactWitness))

  def surfaceText(witness: CentralBreakTimingWitness.Witness, token: String): String =
    if witness.sourceTags.contains("board:played_break") then
      s"On the checked line, this also plays the $token break at this moment."
    else s"On the checked line, this also leaves the $token break available on this branch."

  private enum RouteToken:
    case Valid(token: String)
    case Malformed

  private def classifyRouteToken(raw: String): RouteToken =
    val lower = Option(raw).map(_.trim.toLowerCase).getOrElse("")
    val hasEllipsis = lower.startsWith("...")
    val core = if hasEllipsis then lower.drop(3) else lower
    core.split("-", 2).toList match
      case from :: to :: Nil
          if from.matches("""[a-h][1-8]""") && to.matches("""[a-h][1-8]""") =>
        RouteToken.Valid(s"${if hasEllipsis then "..." else ""}$from-$to")
      case _ => RouteToken.Malformed

private[commentary] object MoveReviewSupportedLocalSurfaceRows:

  private val CounterplayBreakLabel = "Counterplay break"
  private val CentralBreakLabel = "Central break"
  private val CentralLiquidationLabel = "Central liquidation"
  private val CentralChallengeLabel = "Central challenge"

  def build(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      truthContract: Option[DecisiveTruthContract]
  ): List[MoveReviewPlayerSurfaceRow] =
    val planRows =
      List(rankedPlans.primary, rankedPlans.secondary).flatten
        .flatMap(plan => rowForPlan(ctx, inputs, truthContract, plan))
    val packetRows =
      mainPathClaims(inputs)
        .flatMap(claim => rowForClaim(ctx, inputs, truthContract, claim))
    val exactRows =
      (planRows ++ packetRows)
        .distinctBy(row => (row.label, row.text))
        .take(1)
    if exactRows.nonEmpty then exactRows
    else practicalCentralRow(ctx, inputs, truthContract).toList

  private def practicalCentralRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewPlayerSurfaceRow] =
    if tacticalPracticalVeto(inputs, truthContract) then None
    else
      CentralBreakTimingWitness.practical(ctx).flatMap { practical =>
        val token = practical.token.trim
        Option.when(validRouteToken(token)) {
          practical.kind match
            case CentralBreakTimingWitness.PracticalKind.Liquidation =>
              row(
                CentralLiquidationLabel,
                s"The move releases central tension through ${displayRoute(token)}.",
                authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralLiquidation, token = Some(token)))
              )
            case CentralBreakTimingWitness.PracticalKind.Challenge =>
              row(
                CentralChallengeLabel,
                s"The move challenges the center through ${displayRoute(token)}.",
                authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralChallenge, token = Some(token)))
              )
        }.flatten
      }

  private def tacticalPracticalVeto(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    inputs.truthMode == PlayerFacingTruthMode.Tactical ||
      inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical) ||
      truthContract.exists(contract =>
        contract.truthClass == DecisiveTruthClass.Blunder ||
          contract.truthClass == DecisiveTruthClass.MissedWin ||
          (contract.reasonFamily == DecisiveReasonKind.TacticalRefutation && contract.isBad) ||
          contract.failureMode == FailureInterpretationMode.TacticalRefutation
      )

  private def validRouteToken(token: String): Boolean =
    token.matches("""(?:\.\.\.)?[a-h][1-8]-[a-h][1-8]""")

  private def displayRoute(token: String): String =
    token.stripPrefix("...")

  private def rowForPlan(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[MoveReviewPlayerSurfaceRow] =
    Option.when(isNeutralizeKeyBreakPlan(plan)) {
      ClaimAuthorityResolver.supportedLocalNeutralizeKeyBreakTimingAdmission(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract,
        plan = plan
      )
    }.flatten
      .filter(admission =>
        admission.decision.tier == ClaimAuthorityTier.SupportedLocal && admission.decision.vetoReasons.isEmpty
      )
      .flatMap { admission =>
        NeutralizeKeyBreakSurfaceGate.decideForPlanPacket(plan, admission.packet, ctx).token
      }
      .flatMap(token =>
        row(
          CounterplayBreakLabel,
          NeutralizeKeyBreakSurfaceGate.surfaceText(token),
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some(token)))
        )
      )

  private def rowForClaim(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      claim: MainPathScopedClaim
  ): Option[MoveReviewPlayerSurfaceRow] =
    claim.packet.flatMap { packet =>
      if packet.proofSource == ProofSourceId.CounterplayAxisSuppression.wireKey &&
          packet.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey
      then
        val decision =
          ClaimAuthorityResolver.supportedLocalNeutralizeKeyBreakPacketDecision(
            ctx = Some(ctx),
            inputs = inputs,
            truthContract = truthContract,
            packet = packet
          )
        Option
          .when(decision.tier == ClaimAuthorityTier.SupportedLocal && decision.vetoReasons.isEmpty)(packet)
          .flatMap(packet => NeutralizeKeyBreakSurfaceGate.decideForPacket(packet, ctx).token)
          .flatMap(token =>
            row(
              CounterplayBreakLabel,
              NeutralizeKeyBreakSurfaceGate.surfaceText(token),
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some(token)))
            )
          )
      else if packet.proofSource == CentralBreakTimingWitness.ProofSource &&
          packet.proofFamily == CentralBreakTimingWitness.ProofFamily
      then
        ClaimAuthorityResolver
          .supportedLocalCentralBreakTimingAdmission(
            ctx = Some(ctx),
            inputs = inputs,
            truthContract = truthContract,
            packet = packet
          )
          .filter(admission =>
            admission.decision.tier == ClaimAuthorityTier.SupportedLocal &&
              admission.decision.vetoReasons.isEmpty
          )
          .flatMap { admission =>
            CentralBreakTimingSurfaceGate
              .decide(admission.witness)
              .token
              .map(token =>
                token ->
                  CentralBreakTimingSurfaceGate.surfaceText(admission.witness, token)
              )
          }
          .flatMap { case (token, text) =>
            row(
              CentralBreakLabel,
              text,
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralBreak, token = Some(token)))
            )
          }
        else None
      }

  private def mainPathClaims(inputs: QuestionPlannerInputs): List[MainPathScopedClaim] =
    inputs.mainBundle.toList.flatMap { bundle =>
      List(bundle.mainClaim, bundle.lineScopedClaim).flatten
    }

  private def isNeutralizeKeyBreakPlan(plan: QuestionPlan): Boolean =
    plan.timingWitness.exists(_.proofFamily == ProofFamilyId.NeutralizeKeyBreak.wireKey)

  private def row(
      label: String,
      text: String,
      authority: Option[MoveReviewSurfaceAuthority]
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      cleanLabel <- cleanOpt(label)
      cleanText <- cleanOpt(text)
    yield MoveReviewPlayerSurfaceRow(
      label = cleanLabel,
      text = cleanText,
      tone = None,
      source = None,
      refSans = Nil,
      authority = authority
    )

  private def cleanOpt(value: String): Option[String] =
    Option(value)
      .map(UserFacingSignalSanitizer.sanitize)
      .map(_.trim)
      .filter(_.nonEmpty)
