package lila.commentary.analysis

import lila.commentary.*
import lila.commentary.model.{ NarrativeContext, TargetSquare }
import lila.commentary.analysis.claim.{ ClaimAuthorityResolver, OpeningFamilyClaimResolver }
import lila.commentary.analysis.semantic.{ RelationObservationCatalog, RelationObservationDescriptor, RelationSurfaceRowKind }
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
  private val MaxStrategicRelationRows = 4
  private val MaxCompensationRows = 2
  private[commentary] val MoveReviewLedgerLineSources = Set("probe", "decision_compare", "variation", "authoring")
  private[analysis] val PracticalPlanAuthority =
    Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))

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
      strategyPack: Option[StrategyPack] = None,
      truthContract: Option[DecisiveTruthContract] = None
  ): MoveReviewPlayerSurface =
    val knownSans = refs.toList.flatMap(_.variations.flatMap(_.moves.map(_.san))).map(normalizeSan).toSet
    val supportBlocked = MoveReviewSurfaceTruthVeto.truthContractSurfaceVeto(truthContract)
    val compensationBlocked = truthContract.exists(contract => !contract.compensationProseAllowed)
    val promotedPlans =
      if supportBlocked then Nil
      else evaluatedPlans.filter(PlanEvidenceEvaluator.isMainAdmittedPlan).sortBy(_.hypothesis.rank)
    val practicalRows = if supportBlocked then Nil else practicalPlanRows(evaluatedPlans)
    val openingRows = if supportBlocked then Nil else openingFamilyRow(ctx).toList
    val compensationRows = if supportBlocked || compensationBlocked then Nil else compensationAdvancedRows(strategyPack)
    val localRows = if supportBlocked then Nil else sanitizeRows(supportedLocalRows, knownSans)
    val relationRows =
      if supportBlocked then Nil
      else strategicRelationRows(strategyPack, MoveReviewSupportedLocalSurfaceRows.relationKindsForRows(localRows))
    val explanationRows = moveReviewExplanation.toList.flatMap(explanationSupportRows(_, knownSans))
    val referenceRows = if explanationRows.isEmpty then referenceLineRows(ctx, refs, knownSans) else Nil
    val summaryRows =
      distinctVisibleRows(
        (
          mainPlanRow(promotedPlans).toList ++
            openingRows ++
            practicalRows ++
            localRows ++
            explanationRows ++
            referenceRows
        )
      )
    MoveReviewPlayerSurface(
      schema = Schema,
      title =
        moveReviewExplanation.flatMap(explanation => cleanOpt(Some(explanation.title)))
          .orElse(ctx.playedSan.flatMap(san => cleanOpt(Some(s"Move review: $san")))),
      summaryRows = summaryRows,
      advancedRows =
        (relationRows ++ compensationRows ++ (if supportBlocked then Nil else advancedRows(ctx, promotedPlans, evaluatedPlans)))
          .distinctBy(row => (row.label, row.text, row.authority.flatMap(_.token)))
          .take(8),
      decisionComparison = decisionComparisonSurface.map(comparison =>
        sanitizeDecisionComparison(enrichDecisionTargetComparison(ctx, comparison))
      ),
      probeRows = ledgerRows(moveReviewLedger, knownSans),
      authorRows = authorRows(authoringSurface, knownSans)
    )

  private def strategicRelationRows(
      strategyPack: Option[StrategyPack],
      suppressedRelationKinds: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    strategyPack.toList
      .flatMap(_.strategicIdeas)
      .zipWithIndex
      .flatMap { case (idea, index) =>
        strategicRelationDescriptor(idea)
          .filterNot(descriptor => suppressedRelationKinds.contains(descriptor.relationKind))
          .map(descriptor => (descriptor, idea, index))
      }
      .sortBy { case (descriptor, _, index) => (descriptor.surfacePriority, index) }
      .flatMap { case (descriptor, idea, _) => strategicRelationRow(idea, descriptor) }
      .distinctBy(row => (row.authority.flatMap(_.token), row.authority.flatMap(_.target), row.text))
      .take(MaxStrategicRelationRows)

  private def compensationAdvancedRows(strategyPack: Option[StrategyPack]): List[MoveReviewPlayerSurfaceRow] =
    val surface = StrategyPackSurface.from(strategyPack)
    if !compensationRowsEligible(surface) then Nil
    else
      val primary = CompensationDisplayPhrasing.compensationWhyNowText(surface).map("Compensation" -> _)
      val support =
        CompensationDisplayPhrasing
          .dedupeCompensationSupport(
            primary.map(_._2).getOrElse(""),
            CompensationDisplayPhrasing.compensationPersistenceText(surface).toList ++
              CompensationDisplayPhrasing.compensationObjectiveText(surface).toList
          )
          .map("Compensation condition" -> _)
      distinctVisibleRows(
        (primary.toList ++ support).flatMap { case (label, text) =>
          row(label, text, tone = Some("practical")).map(
            _.copy(authority = PracticalPlanAuthority)
          )
        }
      )
        .take(MaxCompensationRows)

  private def compensationRowsEligible(surface: StrategyPackSurface.Snapshot): Boolean =
    surface.strictCompensationPosition &&
      surface.compensationContractResolved &&
      surface.strictCompensationSubtype.exists(_.durablePressure) &&
      StrategyPackSurface.strictCompensationSubtypeLabel(surface).nonEmpty &&
      CompensationDisplayPhrasing.compensationNarrationEligible(surface)

  private def strategicRelationRow(
      idea: StrategyIdeaSignal,
      descriptor: RelationObservationDescriptor
  ): Option[MoveReviewPlayerSurfaceRow] =
    val relation = descriptor.relationKind
    val focus = strategicRelationFocus(idea)
    if focus.isEmpty then None
    else
      val focusText = focus.take(3).mkString(", ")
      val tailFocusText = focus.tail.take(3).mkString(" and ")
      val anchorText = s" around $focusText"
      val text =
        descriptor.publicLabel match
          case "defender-trade" if focus.size >= 2 =>
            s"The checked line uses a ${descriptor.publicLabel} around ${focus.head} through the exchange on ${focus(1)}."
          case "bad-piece liquidation" if focus.size >= 2 =>
            s"The checked line uses ${descriptor.publicLabel} from ${focus.head} through the exchange on ${focus(1)}."
          case "overload" if focus.size >= 2 =>
            s"The checked line creates ${descriptor.publicLabel} pressure on ${focus.head} across $tailFocusText."
          case "deflection" if focus.size >= 3 =>
            s"The checked line creates a ${descriptor.publicLabel} motif on ${focus.head} by attacking ${focus(1)} from ${focus(2)}."
          case "discovered-attack" if focus.size >= 3 =>
            s"The checked line creates a ${descriptor.publicLabel} from ${focus.head} through ${focus(1)} toward ${focus(2)}."
          case "double-check" if focus.size >= 2 =>
            s"The checked line creates ${descriptor.publicLabel} pressure on ${focus.head} from $tailFocusText."
          case "back-rank mate" if focus.size >= 2 =>
            s"The checked line reaches a ${descriptor.publicLabel} pattern around ${focus.head} from $tailFocusText."
          case "mate net" if focus.size >= 2 =>
            s"The checked line reaches a ${descriptor.publicLabel} around ${focus.head} from $tailFocusText."
          case "Greek gift" if focus.size >= 2 =>
            s"The checked line starts a ${descriptor.publicLabel} sacrifice from ${focus.head} toward ${focus(1)}."
          case "stalemate resource" if focus.size >= 2 =>
            s"The checked line keeps a ${descriptor.publicLabel} available around ${focus.head} via ${focus(1)}."
          case "perpetual-check resource" if focus.size >= 2 =>
            s"The checked line keeps a ${descriptor.publicLabel} available around ${focus.head} from $tailFocusText."
          case "fork" if focus.size >= 2 =>
            s"The checked line creates a ${descriptor.publicLabel} from ${focus.head} across $tailFocusText."
          case "hanging piece" if focus.size >= 2 =>
            s"The checked line creates ${descriptor.publicLabel} pressure from ${focus.head} on ${focus(1)}."
          case "decoy" if focus.size >= 3 =>
            s"The checked line creates a ${descriptor.publicLabel} motif on ${focus(1)} that pulls from ${focus(2)}."
          case "trapped-piece" if focus.size >= 2 =>
            s"The checked line limits piece mobility with ${descriptor.publicLabel} pressure on ${focus(1)} from ${focus.head}."
          case "key-square restriction" if focus.size >= 2 =>
            s"The checked line limits piece mobility with ${descriptor.publicLabel} on ${focus(1)} from ${focus.head}."
          case "zwischenzug" if focus.size >= 3 =>
            s"The checked line turns the move order with ${descriptor.publicLabel} from ${focus.head} before the recapture on ${focus(1)}."
          case "x-ray" if focus.size >= 3 =>
            s"The checked line uses ${descriptor.publicLabel} geometry from ${focus.head} through ${focus(1)} toward ${focus(2)}."
          case "clearance" if focus.size >= 3 =>
            s"The checked line uses ${descriptor.publicLabel} geometry with ${focus(1)} clearing the line from ${focus.head} toward ${focus(2)}."
          case "battery" if focus.size >= 3 =>
            s"The checked line uses ${descriptor.publicLabel} geometry between ${focus.head} and ${focus(1)} toward ${focus(2)}."
          case "pin" if focus.size >= 3 =>
            s"The checked line uses ${descriptor.publicLabel} geometry from ${focus.head} through ${focus(1)} toward ${focus(2)}."
          case "skewer" if focus.size >= 3 =>
            s"The checked line uses ${descriptor.publicLabel} geometry from ${focus.head} through ${focus(1)} toward ${focus(2)}."
          case "interference" if focus.size >= 3 =>
            s"The checked line uses ${descriptor.publicLabel} geometry with ${focus.head} between ${focus(1)} and ${focus(2)}."
          case _ =>
            descriptor.surfaceRowKind match
              case RelationSurfaceRowKind.DrawResource =>
                s"The checked line keeps a ${descriptor.publicLabel} available$anchorText."
              case RelationSurfaceRowKind.MoveOrder =>
                s"The checked line turns the move order with ${descriptor.publicLabel}$anchorText."
              case RelationSurfaceRowKind.MobilityRestriction =>
                s"The checked line limits piece mobility with ${descriptor.publicLabel}$anchorText."
              case RelationSurfaceRowKind.LineGeometry =>
                s"The checked line uses ${descriptor.publicLabel} geometry$anchorText."
              case RelationSurfaceRowKind.TacticalRelation =>
                s"The checked line creates a ${descriptor.publicLabel} motif$anchorText."
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

  private def strategicRelationDescriptor(idea: StrategyIdeaSignal) =
    RelationObservationCatalog.descriptorForEvidence(idea.relationKind, idea.evidenceRefs)

  private def strategicRelationFocus(idea: StrategyIdeaSignal): List[String] =
    idea.relationFocusSquares.flatMap(validSquare).distinct

  private def relationTargetFromIdea(
      idea: StrategyIdeaSignal,
      descriptor: RelationObservationDescriptor,
      focus: List[String]
  ): Option[String] =
    idea.relationKind.flatMap(_ => idea.targetSquare)
      .flatMap(validSquare)
      .filter(focus.contains)
      .orElse(descriptor.fallbackTarget(focus))

  private def validSquare(raw: String): Option[String] =
    val cleaned = clean(raw).toLowerCase
    if cleaned.matches("""[a-h][1-8]""") then Some(cleaned) else None

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
    val promotedRows = distinctVisibleRows(planRows)
    (promotedRows ++ practicalAdvancedRows(ctx, evaluatedPlans, promotedPlans, slots = 8 - promotedRows.size)).take(8)

  private def planRowsFromPromotedPlan(plan: EvaluatedPlan): List[MoveReviewPlayerSurfaceRow] =
    val hypothesis = plan.hypothesis
    val promotedRows =
      row("Execution", hypothesis.executionSteps.take(2).mkString(" - ")).toList ++
        row("Objective", hypothesis.preconditions.take(2).mkString(" - ")).toList
    val prophylaxisRows =
      if isProphylaxisPlan(plan) then
        val text =
          cleanOpt(Some(hypothesis.planName))
            .orElse(cleanOpt(hypothesis.failureModes.headOption))
            .getOrElse("")
        row("Prophylaxis", text).toList
      else Nil
    promotedRows ++ prophylaxisRows

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
      if decision.supportedLocalWithoutTacticalVeto
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
      ).map(_.copy(authority = PracticalPlanAuthority))
    }

  private def practicalAdvancedRows(
      ctx: NarrativeContext,
      plans: List[EvaluatedPlan],
      promotedPlans: List[EvaluatedPlan],
      slots: Int
  ): List[MoveReviewPlayerSurfaceRow] =
    if slots <= 0 then Nil
    else
      distinctVisibleRows(
        plans
          .filter(PlanEvidenceEvaluator.isBoundedPracticalSupportPlan)
          .filterNot(plan => hasPromotedSibling(plan, promotedPlans))
          .sortBy(_.hypothesis.rank)
          .flatMap(plan => practicalAdvancedRowsForPlan(ctx, plan))
      )
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
      if squares.nonEmpty then squares
      else if token.nonEmpty then List(token)
      else Nil
    }.distinct

  private def cleanToken(value: String): String =
    value.toLowerCase.replaceAll("""[^a-z0-9]+""", " ").trim

  private def practicalAdvancedRowsForPlan(ctx: NarrativeContext, plan: EvaluatedPlan): List[MoveReviewPlayerSurfaceRow] =
    val hypothesis = plan.hypothesis
    (
      practicalTargetRow(ctx, plan).toList ++
        row("Practical objective", hypothesis.preconditions.take(2).mkString(" - "), tone = Some("practical")).toList ++
        row("Practical steps", hypothesis.executionSteps.take(2).mkString(" - "), tone = Some("practical")).toList
    ).map(_.copy(authority = PracticalPlanAuthority))

  private def practicalTargetRow(
      ctx: NarrativeContext,
      plan: EvaluatedPlan
  ): Option[MoveReviewPlayerSurfaceRow] =
    val targetHints = practicalTargetHints(plan)
    if isWeaknessPlan(plan) || targetHints.nonEmpty then
      practicalCurrentTarget(ctx, targetHints)
        .map(target =>
          practicalTargetRowFor(
            s"The current structure gives ${sideLabel(target.weakSide)} a weak ${targetKindLabel(target.kind)} on ${target.targetSquare} to pressure."
          )
        )
        .orElse(
          practicalEndpointTarget(ctx, targetHints).map(target =>
            practicalTargetRowFor(
              s"The checked line leaves ${sideLabel(target.weakSide)} a weak ${targetKindLabel(target.kind)} on ${target.targetSquare} to pressure."
            )
          )
        )
    else None

  private def practicalCurrentTarget(
      ctx: NarrativeContext,
      targetHints: Set[String]
  ): Option[WeaknessTargetProfile] =
    WeaknessTargetProfile
      .fromFenForMover(ctx.fen)
      .filter(target => targetHints.isEmpty || targetHints.contains(target.targetSquare))
      .find(target => practicalTargetSurvivesBestLine(ctx, target))

  private def practicalEndpointTarget(
      ctx: NarrativeContext,
      targetHints: Set[String]
  ): Option[WeaknessTargetProfile] =
    (if targetHints.nonEmpty then ctx.engineEvidence.flatMap(_.best) else None)
      .filter(line => line.moves.nonEmpty && (line.resultingFen.nonEmpty || line.moves.size >= MinWeaknessTargetOutcomePlies))
      .flatMap(line =>
        WeaknessTargetProfile
          .targetsAfterLineFromFen(
            fen = ctx.fen,
            moves = line.moves,
            resultingFen = line.resultingFen
          )
          .filter(target => targetHints.contains(target.targetSquare))
          .headOption
      )

  private def practicalTargetRowFor(
      text: String
  ): MoveReviewPlayerSurfaceRow =
    row("Practical target", text, tone = Some("practical"))
      .getOrElse(MoveReviewPlayerSurfaceRow(label = "Practical target", text = text, tone = Some("practical")))

  private def practicalTargetHints(plan: EvaluatedPlan): Set[String] =
    WeaknessTargetProfile.targetHintSquares(plan.hypothesis.evidenceSources).toSet

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
      case None => false
      case Some(line) if line.moves.isEmpty => false
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
      if bestLine.moves.headOption.map(NarrativeUtils.normalizeUciMove).exists(_ != chosenMove)
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
    val learningPoint =
      explanation.pvInterpretation
        .flatMap(interpretation => cleanOpt(Some(interpretation.learningPoint)))
        .map(value => value.replaceAll("""\.+$""", ""))
    if sanMoves.isEmpty then Nil
    else
      val line = s"Short line: ${sanMoves.mkString(" ")}."
      val text = learningPoint.fold(line)(point => s"$point. $line")
      row(
        label = "Checked line",
        text = text,
        refSans = refSans(sanMoves, knownSans),
        tone = Some("line")
      ).toList

  private def referenceLineRows(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      knownSans: Set[String]
  ): List[MoveReviewPlayerSurfaceRow] =
    refs.toList
      .flatMap(ref => preferredVariation(ctx, ref).toList)
      .flatMap { variation =>
        val sanMoves = cleanList(variation.moves.map(_.san)).take(5)
        if sanMoves.nonEmpty then
          row(
            label = "Checked line",
            text = s"Short line: ${sanMoves.mkString(" ")}.",
            refSans = refSans(sanMoves, knownSans),
            tone = Some("line")
          )
        else None
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
      (ledger.primaryLine.toList ++ ledger.resourceLine.toList).flatMap { line =>
        val source = clean(line.source)
        val sanMoves = cleanList(line.sanMoves)
        if MoveReviewLedgerLineSources.contains(source) && sanMoves.nonEmpty then
          val text =
            cleanOpt(line.note)
              .orElse(cleanOpt(Some(sanMoves.mkString(" "))))
          row(
            label = line.title,
            text = text.getOrElse(""),
            refSans = refSans(sanMoves, knownSans)
          )
        else None
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

  private[analysis] def distinctVisibleRows(rows: List[MoveReviewPlayerSurfaceRow]): List[MoveReviewPlayerSurfaceRow] =
    rows.distinctBy(row => (row.label, row.text))

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
      secondaryText = cleanOpt(comparison.secondaryText)
    )

  private def hasTwoComparableMoves(digest: DecisionComparisonDigest): Boolean =
    (digest.chosenMove.toList ++ digest.engineBestMove.toList ++ digest.comparedMove.toList)
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
    val anchorMoves = (digest.chosenMove.toList ++ digest.engineBestMove.toList).flatMap(move => cleanMove(Some(move)))
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
    if evidence.kind == LineConsequenceKind.PreviewOnly then None
    else
      val text = clean(evidence.playerSentence).trim
      if text.nonEmpty then Some(text) else None

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

  private[analysis] def cleanOpt(value: Option[String]): Option[String] =
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

private[commentary] object MoveReviewSurfaceTruthVeto:

  def truthContractSurfaceVeto(truthContract: Option[DecisiveTruthContract]): Boolean =
    truthContract.exists(_.blocksStrategicSupport)

private[commentary] object BreakSurfaceToken:

  def canonical(raw: String): Option[String] =
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
      if core.matches("""[a-h][1-8]""") || core.matches("""[a-h][1-8]-[a-h][1-8]""") then
        Some(s"${if hasEllipsis then "..." else ""}$core")
      else None

  def canonicalRoute(raw: String): Option[String] =
    canonical(raw).filter(_.stripPrefix("...").contains("-"))

  def singleSquare(token: String): Option[String] =
    canonical(token)
      .map(_.stripPrefix("..."))
      .filter(_.matches("""[a-h][1-8]"""))

  def displayRoute(token: String): String =
    token.stripPrefix("...")

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
    val planToken = plan.timingWitness.flatMap(_.namedBreak).flatMap(BreakSurfaceToken.canonical)
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
    }.flatMap(BreakSurfaceToken.canonical)

  private def decideToken(token: String, ctx: Option[NarrativeContext]): Decision =
    BreakSurfaceToken.singleSquare(token) match
      case Some(square) =>
        ctx.flatMap(legalPlayedTargetSquare) match
          case Some(target) if target == square => Decision(None, Some(PlayedMoveCollision))
          case Some(_)                         => Decision(Some(token), None)
          case None                            => Decision(None, Some(PlayedMoveUnverified))
      case None => Decision(Some(token), None)

  private def legalPlayedTargetSquare(ctx: NarrativeContext): Option[String] =
    for
      position <- Fen.read(Standard, Fen.Full(ctx.fen))
      uci <- ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(MoveReviewExchangeAnalyzer.isUciMove)
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
        BreakSurfaceToken.canonicalRoute(value) match
          case Some(canonical) => Decision(Some(canonical), None)
          case None            => Decision(None, Some(MalformedBreakToken))
      case None =>
        Decision(None, Some(MissingExactWitness))

  def surfaceText(witness: CentralBreakTimingWitness.Witness, token: String): String =
    if witness.sourceTags.contains("board:played_break") then
      s"On the checked line, this also plays the $token break at this moment."
    else s"On the checked line, this also leaves the $token break available on this branch."

private[commentary] object MoveReviewSupportedLocalSurfaceRows:
  import MoveReviewPlayerPayloadBuilder.PracticalPlanAuthority

  private val MaxSupportedLocalRows = 2
  private val CounterplayBreakLabel = "Counterplay break"
  private val CentralBreakLabel = "Central break"
  private val CentralLiquidationLabel = "Central liquidation"
  private val CentralChallengeLabel = "Central challenge"
  private val OpeningBreakLabel = "Opening break"
  private val OpeningOutpostLabel = "Opening outpost"
  private val KnightOutpostLabel = "Knight outpost"
  private val OpeningBreakGoalTokens =
    List("attack", "break", "challenge", "chipper", "equalizer", "expansion", "liberator", "liquidator", "release", "storm")
  private val KingCenterSquares =
    List(_root_.chess.Square.D4, _root_.chess.Square.E4, _root_.chess.Square.D5, _root_.chess.Square.E5)
  private val BishopPairLabel = "Bishop pair"
  private val OppositeColorBishopsLabel = "Opposite-color bishops"
  private val PieceImprovementLabel = "Piece improvement"
  private val KingSafetyLabel = "King safety"
  private val KingActivationLabel = "King activation"
  private val TechnicalConversionLabel = "Technical conversion"
  private val RookPawnMarchLabel = "Rook-pawn march"
  private val HookCreationLabel = "Hook creation"
  private val RookLiftLabel = "Rook lift"
  private val SeventhRankEntryLabel = "Seventh-rank entry"
  private val RookBehindPasserLabel = "Rook behind passer"
  private val ConnectedRooksLabel = "Connected rooks"
  private val DoubledRooksLabel = "Doubled rooks"
  private val XRayPressureLabel = "X-ray pressure"
  private val DoubleCheckLabel = "Double check"
  private val DeflectionLabel = "Deflection"
  private val DiscoveredAttackLabel = "Discovered attack"
  private val InterferenceLabel = "Interference"
  private val BatteryPressureLabel = "Battery pressure"
  private val PinPressureLabel = "Pin pressure"
  private val DecoyLabel = "Decoy"
  private val TrappedPieceLabel = "Trapped piece"
  private val DominationLabel = "Domination"
  private val ZwischenzugLabel = "Zwischenzug"
  private val PasserBlockadeLabel = "Passer blockade"
  private val ConnectedPassersLabel = "Connected passers"
  private val OutsidePasserLabel = "Outside passer"
  private val PassedPawnAdvanceLabel = "Passed pawn advance"
  private val RouteDenialLabel = "Route denial"
  private val DualAxisBindLabel = "Break and entry"
  private val CounterplayRestraintLabel = "Counterplay restraint"
  private val ColorComplexLabel = "Color complex"
  private val FileEntryLabel = "File entry"
  private val IqpTargetLabel = "IQP target"
  private val SimplificationLabel = "Simplification"
  private val BackRankMateLabel = "Back-rank mate"
  private val MateNetLabel = "Mate net"
  private val SmotheredMateLabel = "Smothered mate"
  private val ArabianMateLabel = "Arabian mate"
  private val BodensMateLabel = "Boden's mate"
  private val AnastasiaMateLabel = "Anastasia's mate"
  private val HookMateLabel = "Hook mate"
  private val CornerMateLabel = "Corner mate"
  private val GreekGiftLabel = "Greek gift"
  private val StalemateResourceLabel = "Stalemate resource"
  private val PerpetualCheckResourceLabel = "Perpetual check"
  private val OverloadedDefenderLabel = "Overloaded defender"
  private val ForkLabel = "Fork"
  private val HangingPieceLabel = "Hanging piece"
  private val SkewerLabel = "Skewer"
  private val DefenderTradeLabel = "Defender trade"
  private val BadPieceTradeLabel = "Bad piece trade"
  private val QueenTradeLabel = "Queen trade"
  private val MatePatternLabels =
    Map(
      "smothered_mate" -> SmotheredMateLabel,
      "arabian_mate" -> ArabianMateLabel,
      "bodens_mate" -> BodensMateLabel,
      "anastasia_mate" -> AnastasiaMateLabel,
      "hook_mate" -> HookMateLabel,
      "corner_mate" -> CornerMateLabel
    )
  private val MatePatternPhrases =
    Map(
      "smothered_mate" -> "smothered mate",
      "arabian_mate" -> "Arabian mate",
      "bodens_mate" -> "Boden's mate",
      "anastasia_mate" -> "Anastasia's mate",
      "hook_mate" -> "hook mate",
      "corner_mate" -> "corner mate"
    )
  private val PracticalRelationKindByLabel =
    Map(
      XRayPressureLabel -> MoveReviewExchangeAnalyzer.RelationKind.XRay,
      DoubleCheckLabel -> MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
      DeflectionLabel -> MoveReviewExchangeAnalyzer.RelationKind.Deflection,
      DiscoveredAttackLabel -> MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
      InterferenceLabel -> MoveReviewExchangeAnalyzer.RelationKind.Interference,
      BatteryPressureLabel -> MoveReviewExchangeAnalyzer.RelationKind.Battery,
      PinPressureLabel -> MoveReviewExchangeAnalyzer.RelationKind.Pin,
      DecoyLabel -> MoveReviewExchangeAnalyzer.RelationKind.Decoy,
      TrappedPieceLabel -> MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
      DominationLabel -> MoveReviewExchangeAnalyzer.RelationKind.Domination,
      ZwischenzugLabel -> MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
      BackRankMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
      MateNetLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      SmotheredMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      ArabianMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      BodensMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      AnastasiaMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      HookMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      CornerMateLabel -> MoveReviewExchangeAnalyzer.RelationKind.MateNet,
      GreekGiftLabel -> MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
      StalemateResourceLabel -> MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
      PerpetualCheckResourceLabel -> MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
      OverloadedDefenderLabel -> MoveReviewExchangeAnalyzer.RelationKind.Overload,
      ForkLabel -> MoveReviewExchangeAnalyzer.RelationKind.Fork,
      HangingPieceLabel -> MoveReviewExchangeAnalyzer.RelationKind.HangingPiece,
      SkewerLabel -> MoveReviewExchangeAnalyzer.RelationKind.Skewer,
      DefenderTradeLabel -> MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade,
      BadPieceTradeLabel -> MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation
    )
  private val IqpInducementFamily =
    ProofFamilyId.fromPlanKind(PlanKind.IQPInducement).map(_.wireKey).getOrElse(PlanKind.IQPInducement.id)
  private val SimplificationWindowFamily =
    ProofFamilyId.fromPlanKind(PlanKind.SimplificationWindow).map(_.wireKey).getOrElse(PlanKind.SimplificationWindow.id)
  private val DefenderTradeFamily = ProofFamilyId.fromPlanKind(PlanKind.DefenderTrade).map(_.wireKey).getOrElse(PlanKind.DefenderTrade.id)
  private val BadPieceLiquidationFamily =
    ProofFamilyId.fromPlanKind(PlanKind.BadPieceLiquidation).map(_.wireKey).getOrElse(PlanKind.BadPieceLiquidation.id)
  private val QueenTradeShieldFamily =
    ProofFamilyId.fromPlanKind(PlanKind.QueenTradeShield).map(_.wireKey).getOrElse(PlanKind.QueenTradeShield.id)
  private val PositionProbeProofSources =
    Set(
      ProofSourceId.ExactTargetFixation.wireKey,
      ProofSourceId.CarlsbadFixedTargetProbe.wireKey,
      ProofSourceId.TargetFocusedCoordinationProbe.wireKey,
      ProofSourceId.ColorComplexSqueezeProbe.wireKey
    )
  private[commentary] def relationKindsForRows(rows: List[MoveReviewPlayerSurfaceRow]): Set[String] =
    rows
      .filter(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.PracticalPlan))
      .flatMap(row => PracticalRelationKindByLabel.get(row.label))
      .toSet

  def build(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      truthContract: Option[DecisiveTruthContract]
  ): List[MoveReviewPlayerSurfaceRow] =
    val planRows =
      (rankedPlans.primary.toList ++ rankedPlans.secondary.toList)
        .flatMap(plan => rowForPlan(ctx, inputs, truthContract, plan))
    val packetRows =
      mainPathClaims(inputs)
        .flatMap(claim => rowForClaim(ctx, inputs, truthContract, claim))
    val packetAndPlanRows =
      MoveReviewPlayerPayloadBuilder.distinctVisibleRows(planRows ++ packetRows)
    val typedSurfaceRows =
      MoveReviewPlayerPayloadBuilder.distinctVisibleRows(
        (
          namedRouteNetworkRow(ctx, inputs, truthContract).toList ++
            dualAxisBindRow(ctx, inputs, truthContract).toList ++
            restrictedDefenseConversionRow(ctx, inputs, truthContract).toList
        )
      )
    val exactRows =
      if packetAndPlanRows.nonEmpty && typedSurfaceRows.nonEmpty then
        MoveReviewPlayerPayloadBuilder.distinctVisibleRows(
          packetAndPlanRows.take(MaxSupportedLocalRows - 1) ++ typedSurfaceRows.take(1)
        )
          .take(MaxSupportedLocalRows)
      else (packetAndPlanRows ++ typedSurfaceRows).take(MaxSupportedLocalRows)
    if exactRows.nonEmpty then exactRows
    else if tacticalPracticalVeto(inputs, truthContract) then Nil
    else
      lazy val topPvReplayCache = PlayedTopPvReplayCache(ctx)
      backRankMatePracticalRow(topPvReplayCache)
        .orElse(mateNetPracticalRow(topPvReplayCache))
        .orElse(greekGiftPracticalRow(topPvReplayCache))
        .orElse(drawResourcePracticalRow(ctx, topPvReplayCache))
        .orElse(doubleCheckPracticalRow(topPvReplayCache))
        .orElse(defenderTradePracticalRow(ctx, topPvReplayCache))
        .orElse(badPieceTradePracticalRow(topPvReplayCache))
        .orElse(queenTradePracticalRow(topPvReplayCache))
        .orElse(zwischenzugPracticalRow(ctx, topPvReplayCache))
        .orElse(trappedPiecePracticalRow(ctx, topPvReplayCache))
        .orElse(dominationPracticalRow(ctx, topPvReplayCache))
        .orElse(forkPracticalRow(topPvReplayCache))
        .orElse(overloadPracticalRow(topPvReplayCache))
        .orElse(decoyPracticalRow(topPvReplayCache))
        .orElse(deflectionPracticalRow(topPvReplayCache))
        .orElse(discoveredAttackPracticalRow(topPvReplayCache))
        .orElse(hangingPiecePracticalRow(topPvReplayCache))
        .orElse(skewerPracticalRow(topPvReplayCache))
        .orElse(xrayPracticalRow(topPvReplayCache))
        .orElse(interferencePracticalRow(topPvReplayCache))
        .orElse(batteryPracticalRow(topPvReplayCache))
        .orElse(pinPracticalRow(topPvReplayCache))
        .orElse(practicalCentralRow(ctx))
        .orElse(openingPracticalGoalRow(ctx, topPvReplayCache))
        .orElse(knightOutpostPracticalRow(topPvReplayCache))
        .orElse(bishopPairPracticalRow(topPvReplayCache))
        .orElse(oppositeColorBishopsPracticalRow(topPvReplayCache))
        .orElse(flankPawnPracticalRow(ctx, topPvReplayCache))
        .orElse(rookLiftPracticalRow(topPvReplayCache))
        .orElse(seventhRankPracticalRow(topPvReplayCache))
        .orElse(rookBehindPasserPracticalRow(topPvReplayCache))
        .orElse(passerBlockadePracticalRow(topPvReplayCache))
        .orElse(fileEntryPracticalRow(topPvReplayCache))
        .orElse(connectedRooksPracticalRow(topPvReplayCache))
        .orElse(doubledRooksPracticalRow(topPvReplayCache))
        .orElse(connectedPassersPracticalRow(topPvReplayCache))
        .orElse(outsidePasserPracticalRow(topPvReplayCache))
        .orElse(passedPawnPracticalRow(topPvReplayCache))
        .orElse(kingActivationPracticalRow(topPvReplayCache))
        .orElse(quietIntentRow(ctx, inputs))
        .toList

  private def practicalCentralRow(ctx: NarrativeContext): Option[MoveReviewPlayerSurfaceRow] =
    CentralBreakTimingWitness.practical(ctx).flatMap { practical =>
      BreakSurfaceToken.canonicalRoute(practical.token).flatMap { token =>
        practical.kind match
          case CentralBreakTimingWitness.PracticalKind.Liquidation =>
            row(
              CentralLiquidationLabel,
              s"The move releases central tension through ${BreakSurfaceToken.displayRoute(token)}.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralLiquidation, token = Some(token)))
            )
          case CentralBreakTimingWitness.PracticalKind.Challenge =>
            row(
              CentralChallengeLabel,
              s"The move challenges the center through ${BreakSurfaceToken.displayRoute(token)}.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralChallenge, token = Some(token)))
            )
      }
    }

  private def openingPracticalGoalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    ctx.openingGoalEvaluation.flatMap { goal =>
      if openingBreakGoalEligible(ctx, goal, replayCache) then
        row(
          OpeningBreakLabel,
          openingBreakText(goal),
          authority = PracticalPlanAuthority
        )
      else
        openingOutpostSquare(goal, replayCache).flatMap { square =>
          row(
            OpeningOutpostLabel,
            s"The checked opening structure has put a knight on the $square outpost.",
            authority = PracticalPlanAuthority
          )
        }
    }

  private def openingBreakGoalEligible(ctx: NarrativeContext, goal: OpeningGoals.Evaluation, replayCache: PlayedTopPvReplayCache): Boolean =
    openingGoalPracticalStatus(goal) &&
      goal.confidence >= 0.70 &&
      openingBreakGoalName(goal.goalName) &&
      openingGoalTriggeredByPlayedMove(ctx, goal) &&
      replayCache.playedMove.exists { case (_, position, move) =>
        move.piece.role == _root_.chess.Pawn &&
          move.piece.color == position.color &&
          move.dest.file.value >= 1 &&
          move.dest.file.value <= 5
      }

  private def openingGoalTriggeredByPlayedMove(ctx: NarrativeContext, goal: OpeningGoals.Evaluation): Boolean =
    ctx.playedMove
      .map(MoveReviewPvLine.normalizeUci)
      .filter(MoveReviewExchangeAnalyzer.isUciMove)
      .exists(played =>
        OpeningGoals.allGoals.exists(definition =>
          Option(goal.goalName).exists(name => definition.name.equalsIgnoreCase(name.trim)) &&
            definition.triggers(played)
        )
      )

  private def openingOutpostSquare(goal: OpeningGoals.Evaluation, replayCache: PlayedTopPvReplayCache): Option[String] =
    if goal.status == OpeningGoals.Status.Achieved &&
        goal.confidence >= 0.85 &&
        openingOutpostGoalName(goal.goalName)
    then
      goal.supportedEvidence.collectFirst {
        case OpeningOutpostEvidence(square)
            if replayCache.playedMove.exists { case (_, position, move) =>
              move.piece.role == _root_.chess.Knight &&
                move.piece.color == position.color &&
                move.dest.key == square.toLowerCase
            } =>
          square.toLowerCase
      }
    else None

  private def openingGoalPracticalStatus(goal: OpeningGoals.Evaluation): Boolean =
    goal.status == OpeningGoals.Status.Achieved || goal.status == OpeningGoals.Status.Partial

  private def openingBreakText(goal: OpeningGoals.Evaluation): String =
    val name = openingBreakDisplayName(goal.goalName)
    goal.status match
      case OpeningGoals.Status.Achieved =>
        s"$name is supported by the checked opening structure."
      case OpeningGoals.Status.Partial =>
        s"$name is on the board, but ${openingBreakCaution(goal.missingEvidence)} still needs care."
      case _ =>
        s"$name remains only a practical opening cue."

  private def openingBreakDisplayName(goalName: String): String =
    Option(goalName)
      .getOrElse("")
      .trim
      .replaceAll("""(?i)\b([a-h])-pawn\s+Break\b""", "$1-pawn advance")
      .replaceAll("""(?i)\b([a-h])-pawn\s+Challenge\b""", "$1-pawn challenge")

  private def openingBreakCaution(missingEvidence: List[String]): String =
    missingEvidence
      .map(_.trim.toLowerCase)
      .find(_.nonEmpty)
      .getOrElse("the follow-up")

  private def openingBreakGoalName(name: String): Boolean =
    val lower = Option(name).getOrElse("").trim.toLowerCase
    lower.nonEmpty &&
      !lower.contains("preparation") &&
      OpeningBreakGoalTokens.exists(lower.contains)

  private val OpeningOutpostGoalNames =
    Set("dutch e4 outpost", "queen's indian e4 outpost", "bogo-indian e4 outpost")

  private val OpeningOutpostEvidence = """(?i)\b([a-h][1-8])\s+outpost occupied\b""".r

  private def openingOutpostGoalName(name: String): Boolean =
    OpeningOutpostGoalNames.contains(Option(name).getOrElse("").trim.toLowerCase)

  private def playedTopPvReplay(
      ctx: NarrativeContext,
      maxPlies: Int
  ): Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]] =
    for
      played <- ctx.playedMove.map(MoveReviewPvLine.normalizeUci).filter(MoveReviewExchangeAnalyzer.isUciMove)
      replay <- MoveReviewExchangeAnalyzer
        .boundedTopReplay(ctx.fen, ctx.engineEvidence.toList.flatMap(_.variations), maxPlies = maxPlies)
      if replay.headOption.exists(_.uci == played)
    yield replay

  private final class PlayedTopPvReplayCache(ctx: NarrativeContext):
    private val replays =
      scala.collection.mutable.Map.empty[Int, Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]]]

    def replay(maxPlies: Int): Option[List[MoveReviewExchangeAnalyzer.BoundedReplayStep]] =
      replays.getOrElseUpdate(maxPlies, playedTopPvReplay(ctx, maxPlies))

    private lazy val playedStep: Option[MoveReviewExchangeAnalyzer.BoundedReplayStep] =
      replay(maxPlies = 1).flatMap(_.headOption)

    lazy val playedMove: Option[(String, chess.Position, chess.Move)] =
      playedStep.map(step => (step.uci, step.before, step.move))

    lazy val playedMoveAfter: Option[(String, chess.Position, chess.Move, chess.Position)] =
      playedStep.map(step => (step.uci, step.before, step.move, step.after))

  private def explicitTargetSquares(ctx: NarrativeContext): List[String] =
    ctx.decision.toList
      .flatMap(_.focalPoint.collect { case TargetSquare(key) => key })
      .map(_.trim.toLowerCase)
      .filter(_.matches("""[a-h][1-8]"""))
      .distinct

  private def playedTopPvRelationRow(
      replayCache: PlayedTopPvReplayCache,
      maxPlies: Int,
      label: String
  )(
      witnessFromReplay: (List[MoveReviewExchangeAnalyzer.BoundedReplayStep], String) => Option[MoveReviewExchangeAnalyzer.RelationWitness],
      surfaceFromWitness: MoveReviewExchangeAnalyzer.RelationWitness => Option[MoveReviewExchangeAnalyzer.RelationPracticalSurface] =
        witness => MoveReviewExchangeAnalyzer.relationPracticalSurfaceFromWitness(witness),
      labelFromSurface: MoveReviewExchangeAnalyzer.RelationPracticalSurface => Option[String] = _ => None
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      replay <- replayCache.replay(maxPlies)
      played <- replay.headOption.map(_.uci)
      witness <- witnessFromReplay(replay, played)
      surface <- surfaceFromWitness(witness)
      row <- row(
        labelFromSurface(surface).getOrElse(label),
        surface.text,
        authority = PracticalPlanAuthority
      )
    yield row

  private def knightOutpostPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if knightOutpostMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.Knight)
      if outpostSquare(after.board, move.dest, move.piece.color)
      row <- row(
        KnightOutpostLabel,
        s"The checked line puts the knight on the ${move.dest.key} outpost.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def knightOutpostMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Knight &&
      move.piece.color == position.color &&
      advancedOutpostSquare(move.dest, move.piece.color)

  private def outpostSquare(board: chess.Board, square: chess.Square, color: chess.Color): Boolean =
    val supportedByPawn =
      board.attackers(square, color).intersects(board.byPiece(color, _root_.chess.Pawn))
    val attackedByEnemyPawn =
      board.attackers(square, !color).intersects(board.byPiece(!color, _root_.chess.Pawn))
    supportedByPawn && !attackedByEnemyPawn

  private def advancedOutpostSquare(square: chess.Square, color: chess.Color): Boolean =
    if color.white then square.rank.value >= _root_.chess.Rank.Fourth.value
    else square.rank.value <= _root_.chess.Rank.Fifth.value

  private def bishopPairPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if bishopPairCaptureMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.Bishop)
      if bishopPairFor(after.board, move.piece.color) && !bishopPairFor(after.board, !move.piece.color)
      row <- row(
        BishopPairLabel,
        "The checked capture keeps the bishop pair on the board.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def bishopPairCaptureMove(position: chess.Position, move: chess.Move): Boolean =
    move.captures &&
      move.piece.role == _root_.chess.Bishop &&
      move.piece.color == position.color

  private def bishopPairFor(board: chess.Board, color: chess.Color): Boolean =
    board.byPiece(color, _root_.chess.Bishop).count >= 2

  private def oppositeColorBishopsPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if materialClarifyingCapture(position, move)
      if oppositeColorBishopsOnly(after.board)
      row <- row(
        OppositeColorBishopsLabel,
        "The checked capture leaves opposite-colored bishops on the board.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def materialClarifyingCapture(position: chess.Position, move: chess.Move): Boolean =
    move.captures && move.piece.color == position.color

  private def oppositeColorBishopsOnly(board: chess.Board): Boolean =
    val whiteBishop = board.byPiece(_root_.chess.Color.White, _root_.chess.Bishop).squares.headOption
    val blackBishop = board.byPiece(_root_.chess.Color.Black, _root_.chess.Bishop).squares.headOption
    board.byPiece(_root_.chess.Color.White, _root_.chess.Bishop).count == 1 &&
      board.byPiece(_root_.chess.Color.Black, _root_.chess.Bishop).count == 1 &&
      board.queens.isEmpty &&
      board.rooks.isEmpty &&
      board.knights.isEmpty &&
      whiteBishop.zip(blackBishop).exists { case (white, black) => white.isLight != black.isLight }

  private def quietIntentRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs
  ): Option[MoveReviewPlayerSurfaceRow] =
    inputs.quietIntent
      .filter(quietIntentPubliclySupported)
      .flatMap(intent => quietIntentLegalMove(ctx, intent).map(move => intent -> move))
      .flatMap { case (intent, move) =>
        val square = move.dest.key
        val piece = quietRoleLabel(move.piece.role)
        intent.intentClass match
          case QuietMoveIntentClass.PieceImprovement =>
            row(
              PieceImprovementLabel,
              s"The checked move improves the $piece by placing it on $square.",
              authority = PracticalPlanAuthority
            )
          case QuietMoveIntentClass.KingSafety =>
            val text =
              if quietCastleMove(move) then "The checked move castles to improve king safety."
              else s"The checked move brings the king to $square for safety."
            row(KingSafetyLabel, text, authority = PracticalPlanAuthority)
          case QuietMoveIntentClass.TechnicalConversionStep =>
            row(
              TechnicalConversionLabel,
              s"The checked move improves the $piece on $square for conversion.",
              authority = PracticalPlanAuthority
            )
          case QuietMoveIntentClass.CounterplayRestraint => None
      }

  private def flankPawnPracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, _, move, afterPosition) <- replayCache.playedMoveAfter
      if rookPawnAdvance(move)
      before <- PositionAnalyzer.extractStrategicState(ctx.fen)
      after <- PositionAnalyzer.extractStrategicState(Fen.write(afterPosition).value)
      row <- flankPawnRowFor(move, before, after)
    yield row

  private def flankPawnRowFor(
      move: chess.Move,
      before: StrategicStateFeatures,
      after: StrategicStateFeatures
  ): Option[MoveReviewPlayerSurfaceRow] =
    val white = move.piece.color.white
    val hookBefore = if white then before.whiteHookCreationChance else before.blackHookCreationChance
    val hookAfter = if white then after.whiteHookCreationChance else after.blackHookCreationChance
    val marchAfter = if white then after.whiteRookPawnMarchReady else after.blackRookPawnMarchReady
    if !hookBefore && hookAfter then
      row(
        HookCreationLabel,
        s"The checked rook-pawn move creates a flank hook on ${move.dest.key}.",
        authority = PracticalPlanAuthority
      )
    else if marchAfter then
      row(
        RookPawnMarchLabel,
        s"The checked line advances the rook pawn to ${move.dest.key} for flank space.",
        authority = PracticalPlanAuthority
      )
    else None

  private def rookPawnAdvance(move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Pawn &&
      move.orig.file == move.dest.file &&
      (move.dest.file == _root_.chess.File.A || move.dest.file == _root_.chess.File.H)

  private def rookLiftPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move) <- replayCache.playedMove
      if rookLiftMove(position, move)
      row <- row(
        RookLiftLabel,
        s"The checked line lifts the rook to ${move.dest.key} as attacking infrastructure.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def rookLiftMove(position: chess.Position, move: chess.Move): Boolean =
    val backRank = if move.piece.color.white then _root_.chess.Rank.First else _root_.chess.Rank.Eighth
    val liftRank = if move.piece.color.white then _root_.chess.Rank.Third else _root_.chess.Rank.Sixth
    !move.captures &&
      move.piece.role == _root_.chess.Rook &&
      move.piece.color == position.color &&
      move.orig.rank == backRank &&
      move.dest.rank == liftRank

  private def seventhRankPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if seventhRankEntryMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.Rook)
      row <- row(
        SeventhRankEntryLabel,
        seventhRankEntryText(move),
        authority = PracticalPlanAuthority
      )
    yield row

  private def seventhRankEntryMove(position: chess.Position, move: chess.Move): Boolean =
    val entryRank = if move.piece.color.white then _root_.chess.Rank.Seventh else _root_.chess.Rank.Second
    !move.captures &&
      move.piece.role == _root_.chess.Rook &&
      move.piece.color == position.color &&
      move.dest.rank == entryRank &&
      move.orig.rank != entryRank

  private def seventhRankEntryText(move: chess.Move): String =
    val rankLabel = if move.piece.color.white then "seventh rank" else "second rank"
    s"The checked line puts the rook on the $rankLabel at ${move.dest.key}."

  private def rookBehindPasserPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if rookBehindPasserMove(position, move)
      pawn <- rookBehindPassedPawn(after, move)
      row <- row(
        RookBehindPasserLabel,
        s"The checked line places the rook behind the passed pawn on ${pawn.key}.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def rookBehindPasserMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Rook &&
      move.piece.color == position.color

  private def rookBehindPassedPawn(position: chess.Position, move: chess.Move): Option[chess.Square] =
    if position.board.occupied.count <= 12 then
      val color = move.piece.color
      val passers = PositionAnalyzer.passedPawns(color, pawnsFor(position.board, color), pawnsFor(position.board, !color))
      passers
        .filter(_.file == move.dest.file)
        .filter(pawn => if color.white then move.dest.rank.value < pawn.rank.value else move.dest.rank.value > pawn.rank.value)
        .sortBy(pawn => math.abs(pawn.rank.value - move.dest.rank.value))
        .headOption
        .filter(_ => position.board.pieceAt(move.dest).exists(piece => piece.color == color && piece.role == _root_.chess.Rook))
    else None

  private def passerBlockadePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if passerBlockadeMove(position, move)
      pawn <- blockadedPassedPawn(after, move)
      row <- row(
        PasserBlockadeLabel,
        s"The checked line blockades the passed pawn on ${pawn.key} with the knight.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def passerBlockadeMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Knight &&
      move.piece.color == position.color

  private def blockadedPassedPawn(position: chess.Position, move: chess.Move): Option[chess.Square] =
    val pawnColor = !move.piece.color
    val passers = PositionAnalyzer.passedPawns(pawnColor, pawnsFor(position.board, pawnColor), pawnsFor(position.board, !pawnColor))
    passers
      .filter(pawn => advancedPassedPawn(pawn, pawnColor))
      .find(pawn => passedPawnStopSquare(pawn, pawnColor).contains(move.dest))
      .filter(_ => position.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.Knight))

  private def passedPawnStopSquare(square: chess.Square, color: chess.Color): Option[chess.Square] =
    val rank = square.rank.value + (if color.white then 1 else -1)
    _root_.chess.Square.at(square.file.value, rank)

  private def advancedPassedPawn(square: chess.Square, color: chess.Color): Boolean =
    if color.white then square.rank.value >= _root_.chess.Rank.Fourth.value
    else square.rank.value <= _root_.chess.Rank.Fifth.value

  private def fileEntryPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if majorFileEntryMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == move.piece.role)
      fileKind <- fileEntryKind(after.board, move)
      row <- row(
        FileEntryLabel,
        s"The checked line places the ${quietRoleLabel(move.piece.role)} on the $fileKind ${move.dest.file.char.toString.toLowerCase}-file.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def majorFileEntryMove(position: chess.Position, move: chess.Move): Boolean =
    val backRank = if move.piece.color.white then _root_.chess.Rank.First else _root_.chess.Rank.Eighth
    !move.captures &&
      (move.piece.role == _root_.chess.Rook || move.piece.role == _root_.chess.Queen) &&
      move.piece.color == position.color &&
      move.orig.file != move.dest.file &&
      move.dest.rank != backRank

  private def fileEntryKind(board: chess.Board, move: chess.Move): Option[String] =
    if openFile(board, move.dest.file) then Some("open")
    else if semiOpenFileFor(board, move.dest.file, move.piece.color) then Some("semi-open")
    else None

  private def openFile(board: chess.Board, file: chess.File): Boolean =
    (board.pawns & _root_.chess.Bitboard.file(file)).isEmpty

  private def semiOpenFileFor(board: chess.Board, file: chess.File, color: chess.Color): Boolean =
    val mask = _root_.chess.Bitboard.file(file)
    val ours = board.pawns & board.byColor(color) & mask
    val theirs = board.pawns & board.byColor(!color) & mask
    ours.isEmpty && theirs.nonEmpty

  private def xrayPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = XRayPressureLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.xrayWitness(replay, played)
    )

  private def deflectionPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 2, label = DeflectionLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.deflectionWitness(replay, played)
    )

  private def discoveredAttackPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = DiscoveredAttackLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.discoveredAttackWitness(replay, played)
    )

  private def backRankMatePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = BackRankMateLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.backRankMateWitness(replay, played)
    )

  private def mateNetPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(
      replayCache,
      maxPlies = 1,
      label = MateNetLabel
    )(
      (replay, played) => MoveReviewExchangeAnalyzer.mateNetWitness(replay, played),
      surfaceFromWitness = witness =>
        MoveReviewExchangeAnalyzer.relationPracticalSurfaceFromWitness(witness, patternId => MatePatternPhrases.get(patternId)),
      labelFromSurface = surface => surface.patternId.flatMap(MatePatternLabels.get)
    )

  private def greekGiftPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 8, label = GreekGiftLabel)(
      (replay, played) =>
        MoveReviewExchangeAnalyzer.greekGiftWitness(
          replay.take(1),
          played,
          continuationLines = List(replay.map(_.uci))
        )
    )

  private def drawResourcePracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    ctx.engineEvidence.flatMap(_.best).flatMap { topLine =>
      for
        replay <- replayCache.replay(maxPlies = 12)
        played <- replay.headOption.map(_.uci)
        witness <-
          MoveReviewExchangeAnalyzer
            .stalemateTrapWitness(replay, played, engineScoreCp = Some(topLine.scoreCp), engineMate = topLine.mate)
            .orElse(
              MoveReviewExchangeAnalyzer.perpetualCheckWitness(
                replay,
                played,
                engineScoreCp = Some(topLine.scoreCp),
                engineMate = topLine.mate
              )
            )
        surface <- MoveReviewExchangeAnalyzer.relationPracticalSurfaceFromWitness(witness)
        label <-
          witness.kind match
            case MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap  => Some(StalemateResourceLabel)
            case MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck => Some(PerpetualCheckResourceLabel)
            case _                                                      => None
        row <- row(label, surface.text, authority = PracticalPlanAuthority)
      yield row
    }

  private def doubleCheckPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = DoubleCheckLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.doubleCheckWitness(replay, played)
    )

  private def defenderTradePracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 3, label = DefenderTradeLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.defenderTradeRelationWitness(replay, played, explicitTargetSquares(ctx))
    )

  private def badPieceTradePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 4, label = BadPieceTradeLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.badPieceLiquidationRelationWitness(replay, played)
    )

  private def queenTradePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      replay <- replayCache.replay(maxPlies = 2)
      _ <- MoveReviewExchangeAnalyzer.queenTradeShieldLine(replay)
      row <- row(
        QueenTradeLabel,
        "This exchange moves the game into the queenless branch.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def forkPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = ForkLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.forkWitness(replay, played)
    )

  private def overloadPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = OverloadedDefenderLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.overloadWitness(replay, played)
    )

  private def interferencePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = InterferenceLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.interferenceWitness(replay, played)
    )

  private def hangingPiecePracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = HangingPieceLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.hangingPieceWitness(replay, played)
    )

  private def trappedPiecePracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = TrappedPieceLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.trappedPieceWitness(replay, played, explicitTargetSquares(ctx))
    )

  private def dominationPracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = DominationLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.dominationWitness(replay, played, explicitTargetSquares(ctx))
    )

  private def zwischenzugPracticalRow(
      ctx: NarrativeContext,
      replayCache: PlayedTopPvReplayCache
  ): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 2, label = ZwischenzugLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.zwischenzugWitness(replay, played, explicitTargetSquares(ctx))
    )

  private def skewerPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = SkewerLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.skewerWitness(replay, played)
    )

  private def batteryPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = BatteryPressureLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.batteryWitness(replay, played)
    )

  private def pinPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 1, label = PinPressureLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.pinWitness(replay, played)
    )

  private def decoyPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    playedTopPvRelationRow(replayCache, maxPlies = 3, label = DecoyLabel)(
      (replay, played) => MoveReviewExchangeAnalyzer.decoyWitness(replay, played)
    )

  private def connectedRooksPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if rookCoordinationMove(position, move)
      rank <- connectedRooksRank(after.board, move.piece.color)
      row <- row(
        ConnectedRooksLabel,
        s"The checked line connects the rooks on the ${rankLabel(rank)} rank.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def doubledRooksPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if rookCoordinationMove(position, move)
      file <- doubledRooksFile(after.board, move.piece.color)
      row <- row(
        DoubledRooksLabel,
        s"The checked line doubles the rooks on the ${file.char.toString.toLowerCase}-file.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def rookCoordinationMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.piece.role == _root_.chess.Rook &&
      move.piece.color == position.color

  private def connectedRooksRank(board: chess.Board, color: chess.Color): Option[chess.Rank] =
    val rooks = board.byPiece(color, _root_.chess.Rook).squares.toList
    if rooks.size == 2 && rooks.head.rank == rooks(1).rank && clearRankBetween(board, rooks.head, rooks(1)) then
      Some(rooks.head.rank)
    else None

  private def doubledRooksFile(board: chess.Board, color: chess.Color): Option[chess.File] =
    val rooks = board.byPiece(color, _root_.chess.Rook).squares.toList
    if rooks.size == 2 && rooks.head.file == rooks(1).file && clearFileBetween(board, rooks.head, rooks(1)) then
      Some(rooks.head.file)
    else None

  private def clearRankBetween(board: chess.Board, first: chess.Square, second: chess.Square): Boolean =
    val minFile = math.min(first.file.value, second.file.value)
    val maxFile = math.max(first.file.value, second.file.value)
    (minFile + 1 until maxFile).forall(file =>
      _root_.chess.Square.at(file, first.rank.value).forall(square => !board.occupied.contains(square))
    )

  private def clearFileBetween(board: chess.Board, first: chess.Square, second: chess.Square): Boolean =
    val minRank = math.min(first.rank.value, second.rank.value)
    val maxRank = math.max(first.rank.value, second.rank.value)
    (minRank + 1 until maxRank).forall(rank =>
      _root_.chess.Square.at(first.file.value, rank).forall(square => !board.occupied.contains(square))
    )

  private def rankLabel(rank: chess.Rank): String =
    rank match
      case _root_.chess.Rank.First   => "first"
      case _root_.chess.Rank.Second  => "second"
      case _root_.chess.Rank.Third   => "third"
      case _root_.chess.Rank.Fourth  => "fourth"
      case _root_.chess.Rank.Fifth   => "fifth"
      case _root_.chess.Rank.Sixth   => "sixth"
      case _root_.chess.Rank.Seventh => "seventh"
      case _root_.chess.Rank.Eighth  => "eighth"

  private def connectedPassersPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if endgamePawnAdvanceMove(position, move)
      pair <- connectedPassedPair(after, move.piece.color)
      row <- row(
        ConnectedPassersLabel,
        s"The checked line leaves connected passers on ${pair._1.key} and ${pair._2.key}.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def connectedPassedPair(position: chess.Position, color: chess.Color): Option[(chess.Square, chess.Square)] =
    if position.board.occupied.count <= 12 then
      val passers =
        PositionAnalyzer
          .passedPawns(color, pawnsFor(position.board, color), pawnsFor(position.board, !color))
          .filter(pawn => advancedPassedPawn(pawn, color))
          .sortBy(square => (square.file.value, square.rank.value))
      val enemyPassers =
        PositionAnalyzer.passedPawns(!color, pawnsFor(position.board, !color), pawnsFor(position.board, color))
      if enemyPassers.isEmpty then
        passers
          .combinations(2)
          .collectFirst {
            case List(first, second)
                if fileDistance(first.file, second.file) == 1 &&
                  (first.rank.value - second.rank.value).abs <= 1 =>
              first -> second
          }
      else None
    else None

  private def outsidePasserPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if endgamePawnAdvanceMove(position, move)
      passer <- outsidePassedPawn(after, move.piece.color)
      row <- row(
        OutsidePasserLabel,
        s"The checked line leaves an outside passer on ${passer.key}.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def outsidePassedPawn(position: chess.Position, color: chess.Color): Option[chess.Square] =
    if position.board.occupied.count <= 12 then
      val board = position.board
      val passers =
        PositionAnalyzer
          .passedPawns(color, pawnsFor(board, color), pawnsFor(board, !color))
          .filter(pawn => advancedPassedPawn(pawn, color))
          .filter(pawn => pawn.file.value <= 1 || pawn.file.value >= 6)
          .sortBy(pawn => -relativeRank(pawn, color))
      val enemyPassers = PositionAnalyzer.passedPawns(!color, pawnsFor(board, !color), pawnsFor(board, color))
      val ownPawns = pawnsFor(board, color).squares.toList
      if enemyPassers.isEmpty then
        passers.find { passer =>
          ownPawns.exists(pawn => pawn != passer && fileDistance(pawn.file, passer.file) >= 3) &&
            !board.kingPosOf(!color).exists(enemyKingBlocksPasser(_, passer, color))
        }
      else None
    else None

  private def endgamePawnAdvanceMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.promotion.isEmpty &&
      move.piece.role == _root_.chess.Pawn &&
      move.piece.color == position.color &&
      move.orig.file == move.dest.file &&
      (if move.piece.color.white then move.dest.rank.value > move.orig.rank.value else move.dest.rank.value < move.orig.rank.value)

  private def fileDistance(first: chess.File, second: chess.File): Int =
    (first.value - second.value).abs

  private def relativeRank(square: chess.Square, color: chess.Color): Int =
    if color.white then square.rank.value else 7 - square.rank.value

  private def enemyKingBlocksPasser(enemyKing: chess.Square, passer: chess.Square, color: chess.Color): Boolean =
    enemyKing.file == passer.file &&
      (if color.white then enemyKing.rank.value > passer.rank.value else enemyKing.rank.value < passer.rank.value)

  private def passedPawnPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if endgamePawnAdvanceMove(position, move)
      if afterMovePassedPawn(after, move)
      row <- row(
        PassedPawnAdvanceLabel,
        s"The checked line advances the passed pawn to ${move.dest.key}.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def afterMovePassedPawn(position: chess.Position, move: chess.Move): Boolean =
    PositionAnalyzer.passedPawns(move.piece.color, pawnsFor(position.board, move.piece.color), pawnsFor(position.board, !move.piece.color)).contains(move.dest)

  private def pawnsFor(board: chess.Board, color: chess.Color): _root_.chess.Bitboard =
    board.pawns & board.byColor(color)

  private def kingActivationPracticalRow(replayCache: PlayedTopPvReplayCache): Option[MoveReviewPlayerSurfaceRow] =
    for
      (_, position, move, after) <- replayCache.playedMoveAfter
      if kingActivationMove(position, move)
      if after.board.pieceAt(move.dest).exists(piece => piece.color == move.piece.color && piece.role == _root_.chess.King)
      if kingActivityImproves(position.board, after.board, move.piece.color, move.orig, move.dest)
      row <- row(
        KingActivationLabel,
        s"The checked line activates the king on ${move.dest.key} for the endgame.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def kingActivationMove(position: chess.Position, move: chess.Move): Boolean =
    !move.captures &&
      move.castle.isEmpty &&
      move.piece.role == _root_.chess.King &&
      move.piece.color == position.color &&
      position.board.occupied.count <= 10

  private def kingActivityImproves(
      before: chess.Board,
      after: chess.Board,
      color: chess.Color,
      orig: chess.Square,
      dest: chess.Square
  ): Boolean =
    val beforeCenter = kingCenterDistance(orig)
    val afterCenter = kingCenterDistance(dest)
    val beforeMobility = (orig.kingAttacks & ~before.byColor(color)).count
    val afterMobility = (dest.kingAttacks & ~after.byColor(color)).count
    afterCenter < beforeCenter && afterMobility >= beforeMobility

  private def kingCenterDistance(square: chess.Square): Int =
    KingCenterSquares
      .map(center => math.max((square.file.value - center.file.value).abs, (square.rank.value - center.rank.value).abs))
      .min

  private def quietIntentPubliclySupported(intent: QuietMoveIntentClaim): Boolean =
    intent.allowsUserFacing &&
      quietIntentPacketMatchesClass(intent) &&
      intent.packet.scope == PlayerFacingPacketScope.MoveLocal &&
      intent.packet.releaseRisks.isEmpty &&
      intent.packet.suppressionReasons.isEmpty

  private def quietIntentPacketMatchesClass(intent: QuietMoveIntentClaim): Boolean =
    intent.packet.proofFamily == intent.intentClass.proofFamily &&
      intent.packet.proofSource == intent.sourceKind &&
      intent.packet.claimGate.ontologyFamily == intent.intentClass.ontologyFamily

  private def quietIntentLegalMove(ctx: NarrativeContext, intent: QuietMoveIntentClaim): Option[chess.Move] =
    (for
      position <- Fen.read(Standard, Fen.Full(ctx.fen))
      uci <- ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(MoveReviewExchangeAnalyzer.isUciMove)
      move <- Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)
    yield move).filter(move => quietIntentMoveMatches(intent, move))

  private def quietIntentMoveMatches(intent: QuietMoveIntentClaim, move: chess.Move): Boolean =
    val anchors = quietIntentAnchorSquares(intent)
    val anchorOk = anchors.nonEmpty && anchors.forall(_ == move.dest.key)
    val quietNonCapturePiece =
      !move.captures &&
        move.piece.role != _root_.chess.Pawn
    intent.intentClass match
      case QuietMoveIntentClass.PieceImprovement =>
        anchorOk &&
          quietNonCapturePiece &&
          move.piece.role != _root_.chess.King
      case QuietMoveIntentClass.TechnicalConversionStep =>
        anchorOk && quietNonCapturePiece
      case QuietMoveIntentClass.KingSafety =>
        anchorOk &&
          move.piece.role == _root_.chess.King &&
          (
            intent.sourceKind == "king_move" ||
              (Set("castle_short", "castle_long").contains(intent.sourceKind) && quietCastleMove(move))
          )
      case QuietMoveIntentClass.CounterplayRestraint => false

  private def quietIntentAnchorSquares(intent: QuietMoveIntentClaim): Set[String] =
    (intent.packet.anchorTerms ++ intent.packet.proofPathWitness.ownerSeedTerms)
      .map(_.trim.toLowerCase)
      .filter(_.matches("""[a-h][1-8]"""))
      .toSet

  private def quietCastleMove(move: chess.Move): Boolean =
    Set("e1g1", "e1c1", "e8g8", "e8c8").contains(s"${move.orig.key}${move.dest.key}")

  private def quietRoleLabel(role: chess.Role): String =
    role match
      case _root_.chess.King   => "king"
      case _root_.chess.Queen  => "queen"
      case _root_.chess.Rook   => "rook"
      case _root_.chess.Bishop => "bishop"
      case _root_.chess.Knight => "knight"
      case _root_.chess.Pawn   => "pawn"

  private def tacticalPracticalVeto(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    inputs.truthMode == PlayerFacingTruthMode.Tactical ||
      inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical) ||
      MoveReviewSurfaceTruthVeto.truthContractSurfaceVeto(truthContract)

  private def namedRouteNetworkRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.namedRouteNetworkSurfaceDecision(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract
      )
    decision
      .filter(_.supportedLocalWithoutTacticalVeto)
      .flatMap(_ => inputs.namedRouteNetworkSurface.flatMap(routeNetworkRow))

  private def routeNetworkRow(
      network: RouteNetworkBindProof.SurfaceNetwork
  ): Option[MoveReviewPlayerSurfaceRow] =
    row(
      RouteDenialLabel,
      network.routeDenialText("The checked line"),
      authority = PracticalPlanAuthority
    )

  private def dualAxisBindRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.dualAxisBindSurfaceDecision(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract
      )
    decision
      .filter(_.supportedLocalWithoutTacticalVeto)
      .flatMap(_ => inputs.dualAxisBindSurface.flatMap(dualAxisBindSurfaceRow))

  private def dualAxisBindSurfaceRow(
      contract: TwoAxisBindProof.Contract
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      breakAxis <- contract.primaryAxis.filter(axis => normalizeSurfaceToken(axis.kind) == "break_axis")
      entryAxis <- contract.corroboratingAxes.find(axis => normalizeSurfaceToken(axis.kind) == "entry_axis")
      breakLabel <- surfaceAxisLabel(breakAxis.label)
      entryLabel <- surfaceAxisLabel(entryAxis.label)
      row <- row(
        DualAxisBindLabel,
        s"The checked line keeps the $breakLabel break shut while keeping $entryLabel unavailable.",
        authority = PracticalPlanAuthority
      )
    yield row

  private def restrictedDefenseConversionRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.restrictedDefenseConversionSurfaceDecision(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract
      )
    decision
      .filter(_.supportedLocalWithoutTacticalVeto)
      .flatMap(_ =>
        inputs.restrictedDefenseConversionSurface.flatMap(contract => restrictedDefenseConversionSurfaceRow(ctx, contract))
      )

  private def restrictedDefenseConversionSurfaceRow(
      ctx: NarrativeContext,
      contract: RestrictedDefenseConversionProof.Contract
  ): Option[MoveReviewPlayerSurfaceRow] =
    val replySan =
      for
        played <- ctx.playedMove
        reply <- contract.bestDefenseFound
        afterFen <- MoveReviewPvLine.legalFenAfter(ctx.fen, played)
        san <- NarrativeUtils.uciToSan(afterFen, reply)
        cleanSan <- MoveReviewPlayerPayloadBuilder.cleanOpt(Option(san))
      yield cleanSan
    val text =
      replySan
        .map(san => s"The checked line keeps the conversion route intact after $san.")
        .getOrElse("The checked line keeps the best defense narrow and the conversion route intact.")
    row(TechnicalConversionLabel, text, authority = PracticalPlanAuthority)

  private def rowForPlan(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      plan: QuestionPlan
  ): Option[MoveReviewPlayerSurfaceRow] =
    val admission =
      ClaimAuthorityResolver.supportedLocalNeutralizeKeyBreakTimingAdmission(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract,
        plan = plan
      )
    admission
      .filter(_.decision.supportedLocalWithoutTacticalVeto)
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
        if decision.supportedLocalWithoutTacticalVeto then
          NeutralizeKeyBreakSurfaceGate
            .decideForPacket(packet, ctx)
            .token
            .flatMap { token =>
              row(
                CounterplayBreakLabel,
                NeutralizeKeyBreakSurfaceGate.surfaceText(token),
                authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some(token)))
              )
            }
        else None
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
          .filter(_.decision.supportedLocalWithoutTacticalVeto)
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
      else if positionProbePacket(packet)
      then
        admittedPositionProbeRow(ctx, inputs, truthContract, packet)
      else if counterplayRestraintPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(counterplayRestraintRow)
      else if packet.proofSource == ProofSourceId.LocalFileEntryBind.wireKey &&
          packet.proofFamily == ProofFamilyId.HalfOpenFilePressure.wireKey
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(localFileEntryRow)
      else if outpostOccupationPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(outpostOccupationRow)
      else if iqpInducementPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(iqpInducementRow)
      else if simplificationWindowPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(simplificationWindowRow)
      else if exchangeOwnershipPacket(packet)
      then
        supportedLocalMoveDeltaRow(ctx, inputs, truthContract, packet)(exchangeOwnershipRow)
      else None
      }

  private def supportedLocalMoveDeltaRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  )(
      buildRow: PlayerFacingClaimPacket => Option[MoveReviewPlayerSurfaceRow]
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract,
        packet = packet
      )
    if decision.supportedLocalWithoutTacticalVeto then buildRow(packet)
    else None

  private def positionProbePacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.scope == PlayerFacingPacketScope.PositionLocal &&
      PositionProbeProofSources.contains(packet.proofSource)

  private def counterplayRestraintPacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofSource == ProofSourceId.ProphylacticMove.wireKey &&
      packet.proofFamily == ProofFamilyId.CounterplayRestraint.wireKey

  private def outpostOccupationPacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofSource == PlayerFacingTruthModePolicy.OutpostEntrenchmentProofSource &&
      packet.proofFamily == PlayerFacingTruthModePolicy.OutpostEntrenchmentProofFamily

  private def iqpInducementPacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofSource == ProofSourceId.IQPInducementProbe.wireKey &&
      packet.proofFamily == IqpInducementFamily

  private def simplificationWindowPacket(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofSource == SimplificationWindowFamily &&
      packet.proofFamily == SimplificationWindowFamily

  private def matchedExactSliceProof[A](
      packet: PlayerFacingClaimPacket
  )(build: PartialFunction[PlayerFacingExactSliceProof, Option[A]]): Option[A] =
    packet.proofPathWitness.exactSliceProof.flatMap { proof =>
      if build.isDefinedAt(proof) && PlayerFacingExactSliceProofFacts.matchesPacket(packet, proof) then build(proof)
      else None
    }

  private def admittedPositionProbeRow(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    val decision =
      ClaimAuthorityResolver.decidePositionProbe(
        ctx = Some(ctx),
        inputs = inputs,
        truthContract = truthContract,
        packet = packet
      )
    if decision.admitted && decision.vetoReasons.isEmpty then positionProbeRow(packet)
    else None

  private def positionProbeRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare) =>
        val target = targetSquare.toLowerCase
        row(
          "Fixed target",
          s"The checked line keeps $target fixed as the target.",
          authority = PracticalPlanAuthority
        )
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(targetSquare, true) =>
        val target = targetSquare.toLowerCase
        row(
          "Minority attack",
          s"The checked line keeps $target as the minority-attack fixed target.",
          authority = PracticalPlanAuthority
        )
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(targetSquare, supportFromSquares, _) =>
        val target = targetSquare.toLowerCase
        val support = supportFromSquares.map(_.toLowerCase).distinct.take(2).mkString(" and ")
        row(
          "Target coordination",
          s"The checked line coordinates pressure on $target from $support.",
          authority = PracticalPlanAuthority
        )
      case PlayerFacingExactSliceProof.ColorComplexSqueeze(
            targetSquare,
            squareColor,
            minorPieceRole,
            minorPieceSquare
          ) =>
        val target = targetSquare.toLowerCase
        val complex = squareColor.toLowerCase
        val role = minorPieceRole.toLowerCase
        val from = minorPieceSquare.toLowerCase
        row(
          ColorComplexLabel,
          s"The checked line keeps the $role on $from attacking $target in the $complex-square complex.",
          authority = PracticalPlanAuthority
        )
    }

  private def counterplayRestraintRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.ProphylacticRestraint(resourceToken) =>
        row(
          CounterplayRestraintLabel,
          counterplayRestraintText(resourceToken),
          authority = PracticalPlanAuthority
        )
    }

  private def counterplayRestraintText(resourceToken: String): String =
    val token = resourceToken.trim.toLowerCase
    if token.startsWith("denied_resource:") then
      val resource = token.stripPrefix("denied_resource:")
      s"The checked line keeps ${counterplayResourceLabel(resource)} restrained."
    else s"The checked line keeps $token unavailable as a counterplay resource."

  private def counterplayResourceLabel(resource: String): String =
    resource match
      case "break"                => "the opponent's break"
      case "entry_square"         => "the opponent's entry square"
      case "forcing_threat"       => "the forcing threat"
      case "piece_activity"       => "the opponent's piece activity"
      case "counterplay_route"    => "the counterplay route"
      case "route_node"           => "the route node"
      case "reroute_square"       => "the reroute square"
      case "pressure"             => "the pressure resource"
      case "color_complex_escape" => "the color-complex escape"
      case other                  => other.replace('_', ' ')

  private def iqpInducementRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.IqpInducement(targetSquare, _) =>
        row(
          IqpTargetLabel,
          s"The checked line leaves ${targetSquare.toLowerCase} as an isolated pawn target.",
          authority = PracticalPlanAuthority
        )
    }

  private def outpostOccupationRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.OutpostOccupation(pieceRole, square) =>
        row(
          KnightOutpostLabel,
          s"The checked line puts the ${pieceRole.toLowerCase} on the ${square.toLowerCase} outpost.",
          authority = PracticalPlanAuthority
        )
    }

  private def simplificationWindowRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    simplificationExchangeSquare(packet).flatMap(square =>
      row(
        SimplificationLabel,
        s"The checked line keeps the same local edge after the exchange on $square.",
        authority = PracticalPlanAuthority
      )
    )

  private def simplificationExchangeSquare(packet: PlayerFacingClaimPacket): Option[String] =
    (
      packet.proofPathWitness.continuationTerms ++
        packet.proofPathWitness.structureTransitionTerms ++
        packet.proofPathWitness.ownerSeedTerms
    ).flatMap(exchangeSquareTerm).headOption

  private def exchangeSquareTerm(term: String): Option[String] =
    val lower = Option(term).getOrElse("").trim.toLowerCase
    if lower.startsWith("exchange_square:") then
      Some(lower.stripPrefix("exchange_square:").trim).filter(_.matches("""[a-h][1-8]"""))
    else None

  private def exchangeOwnershipPacket(packet: PlayerFacingClaimPacket): Boolean =
    (packet.proofSource == PlayerFacingTruthModePolicy.DefenderTradeProofSource &&
      packet.proofFamily == DefenderTradeFamily) ||
      (packet.proofSource == PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource &&
        packet.proofFamily == BadPieceLiquidationFamily) ||
      (packet.proofSource == PlayerFacingTruthModePolicy.QueenTradeShieldProofSource &&
        packet.proofFamily == QueenTradeShieldFamily)

  private def exchangeOwnershipRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    if packet.proofSource == PlayerFacingTruthModePolicy.DefenderTradeProofSource &&
        packet.proofFamily == DefenderTradeFamily
    then
      defenderTradeBranch(packet).flatMap { case (defender, exchange, target) =>
        row(
          DefenderTradeLabel,
          s"The checked line trades on $exchange to remove the defender from $defender, loosening $target.",
          authority = PracticalPlanAuthority
        )
      }
    else if packet.proofSource == PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource &&
        packet.proofFamily == BadPieceLiquidationFamily
    then
      badPieceLiquidationBranch(packet).flatMap { case (badPiece, exchange) =>
        row(
          BadPieceTradeLabel,
          s"The checked line trades on $exchange to clear the bad piece from $badPiece.",
          authority = PracticalPlanAuthority
        )
      }
    else if packet.proofSource == PlayerFacingTruthModePolicy.QueenTradeShieldProofSource &&
        packet.proofFamily == QueenTradeShieldFamily
    then queenTradeShieldRow(packet)
    else None

  private def defenderTradeBranch(packet: PlayerFacingClaimPacket): Option[(String, String, String)] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.DefenderTrade(defender, exchange, target) =>
        Some((defender.trim.toLowerCase, exchange.trim.toLowerCase, target.trim.toLowerCase))
    }

  private def badPieceLiquidationBranch(packet: PlayerFacingClaimPacket): Option[(String, String)] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.BadPieceLiquidation(badPiece, exchange) =>
        Some((badPiece.trim.toLowerCase, exchange.trim.toLowerCase))
    }

  private def queenTradeShieldRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.QueenTradeShield(_) =>
        row(
          QueenTradeLabel,
          "This exchange moves the game into the queenless branch.",
          authority = PracticalPlanAuthority
        )
    }

  private def localFileEntryRow(
      packet: PlayerFacingClaimPacket
  ): Option[MoveReviewPlayerSurfaceRow] =
    matchedExactSliceProof(packet) {
      case PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare) =>
        val displayFile = file.toLowerCase.stripSuffix("-file") + "-file"
        val entry = entrySquare.toLowerCase
        row(
          FileEntryLabel,
          s"The checked line keeps pressure on $entry through the $displayFile.",
          authority = PracticalPlanAuthority
        )
    }

  private def mainPathClaims(inputs: QuestionPlannerInputs): List[MainPathScopedClaim] =
    inputs.mainBundle.toList.flatMap { bundle =>
      bundle.mainClaim.toList ++ bundle.lineScopedClaim.toList
    }

  private def row(
      label: String,
      text: String,
      authority: Option[MoveReviewSurfaceAuthority]
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      cleanLabel <- MoveReviewPlayerPayloadBuilder.cleanOpt(Option(label))
      cleanText <- MoveReviewPlayerPayloadBuilder.cleanOpt(Option(text))
    yield MoveReviewPlayerSurfaceRow(
      label = cleanLabel,
      text = cleanText,
      tone = None,
      source = None,
      refSans = Nil,
      authority = authority
    )

  private def surfaceAxisLabel(value: String): Option[String] =
    MoveReviewPlayerPayloadBuilder.cleanOpt(Option(value))
      .map(_.toLowerCase)
      .map(_.replaceAll("""(?i)^neutralized[-_ ]break[: ]""", ""))
      .map(_.replaceAll("\\s+", " "))
      .filter(label => label.matches("""\.{0,3}[a-h][1-8](,[a-h][1-8])*""") || label.matches("""\.{0,3}[a-h][1-8]-[a-h][1-8]"""))

  private def normalizeSurfaceToken(value: String): String =
    Option(value).getOrElse("").trim.toLowerCase
