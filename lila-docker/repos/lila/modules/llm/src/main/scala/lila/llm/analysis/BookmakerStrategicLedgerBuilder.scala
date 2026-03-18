package lila.llm.analysis

import lila.llm.{ BookmakerLedgerLineV1, BookmakerRefsV1, BookmakerStrategicLedgerV1, DecisionComparisonDigest, NarrativeSignalDigest, StrategyPack }
import lila.llm.model.{ NarrativeContext, ProbeResult }
import lila.llm.model.authoring.QuestionEvidence
import lila.llm.model.strategic.{ EndgamePatternState, PlanLifecyclePhase }

object BookmakerStrategicLedgerBuilder:

  private final case class MotifDef(
      key: String,
      label: String,
      markers: List[String],
      allowedThemes: Set[ThemeTaxonomy.ThemeL1] = Set.empty,
      preferredSubplans: Set[ThemeTaxonomy.SubplanId] = Set.empty
  )

  private final case class MotifChoice(
      motif: MotifDef,
      score: Double
  )

  private final case class StageChoice(
      key: String,
      label: String,
      reason: Option[String]
  )

  private final case class PlanProfile(
      themes: Set[ThemeTaxonomy.ThemeL1],
      subplans: Set[ThemeTaxonomy.SubplanId]
  )

  private final case class LineCandidate(
      title: String,
      sanMoves: List[String],
      scoreCp: Option[Int],
      mate: Option[Int],
      note: Option[String],
      source: String,
      weight: Double
  ):
    def toWire: Option[BookmakerLedgerLineV1] =
      val normalizedMoves = sanMoves.map(_.trim).filter(_.nonEmpty).take(4)
      Option.when(title.trim.nonEmpty && normalizedMoves.nonEmpty)(
        BookmakerLedgerLineV1(
          title = title.trim,
          sanMoves = normalizedMoves,
          scoreCp = scoreCp,
          mate = mate,
          note = note.map(_.trim).filter(_.nonEmpty),
          source = source
        )
      )

  private val PieceRouteMotif = MotifDef("piece_route", "Piece route", Nil)

  private val Motifs = List(
    MotifDef(
      key = "rook_pawn_march",
      label = "Rook-pawn march",
      markers = List("rook pawn march", "h pawn lever", "rook lift", "kingside clamp"),
      allowedThemes = Set(ThemeTaxonomy.ThemeL1.FlankInfrastructure),
      preferredSubplans = Set(
        ThemeTaxonomy.SubplanId.RookPawnMarch,
        ThemeTaxonomy.SubplanId.HookCreation,
        ThemeTaxonomy.SubplanId.RookLiftScaffold
      )
    ),
    MotifDef(
      key = "entrenched_piece",
      label = "Entrenched piece",
      markers = List("entrenched piece", "entrenched", "french chain", "fixed knight"),
      allowedThemes = Set(
        ThemeTaxonomy.ThemeL1.PieceRedeployment,
        ThemeTaxonomy.ThemeL1.WeaknessFixation
      ),
      preferredSubplans = Set(
        ThemeTaxonomy.SubplanId.OutpostEntrenchment,
        ThemeTaxonomy.SubplanId.WorstPieceImprovement
      )
    ),
    MotifDef(
      key = "minority_attack",
      label = "Minority attack",
      markers = List("minority attack", "carlsbad"),
      allowedThemes = Set(ThemeTaxonomy.ThemeL1.WeaknessFixation),
      preferredSubplans = Set(ThemeTaxonomy.SubplanId.MinorityAttackFixation)
    ),
    MotifDef(
      key = "outpost_reinforcement",
      label = "Outpost reinforcement",
      markers = List("outpost reinforcement", "outpost", "strong square", "anchor square"),
      allowedThemes = Set(ThemeTaxonomy.ThemeL1.PieceRedeployment),
      preferredSubplans = Set(ThemeTaxonomy.SubplanId.OutpostEntrenchment)
    ),
    MotifDef(
      key = "color_complex",
      label = "Color complex",
      markers = List("color complex", "colour complex", "color complex weakness"),
      allowedThemes = Set(
        ThemeTaxonomy.ThemeL1.SpaceClamp,
        ThemeTaxonomy.ThemeL1.WeaknessFixation
      ),
      preferredSubplans = Set(
        ThemeTaxonomy.SubplanId.FlankClamp,
        ThemeTaxonomy.SubplanId.StaticWeaknessFixation
      )
    ),
    MotifDef(
      key = "opposite_bishops_conversion",
      label = "Opposite bishops conversion",
      markers = List(
        "opposite color bishops",
        "opposite colored bishops draw",
        "good bishop rook pawn conversion"
      ),
      allowedThemes = Set(ThemeTaxonomy.ThemeL1.AdvantageTransformation),
      preferredSubplans = Set(
        ThemeTaxonomy.SubplanId.OppositeBishopsConversion,
        ThemeTaxonomy.SubplanId.SimplificationConversion,
        ThemeTaxonomy.SubplanId.PasserConversion,
        ThemeTaxonomy.SubplanId.InvasionTransition
      )
    ),
    MotifDef(
      key = "active_passive_exchange",
      label = "Active-passive exchange",
      markers = List("active passive exchange", "exchange active pieces", "leave passive pieces"),
      allowedThemes = Set(ThemeTaxonomy.ThemeL1.FavorableExchange),
      preferredSubplans = Set(
        ThemeTaxonomy.SubplanId.SimplificationWindow,
        ThemeTaxonomy.SubplanId.DefenderTrade
      )
    ),
    MotifDef(
      key = "compensation_attack",
      label = "Compensation attack",
      markers = List("compensation attack", "exchange sacrifice", "long term compensation", "mating attack", "dark square bind")
    ),
    MotifDef(
      key = "counterplay_restraint",
      label = "Counterplay restraint",
      markers = List("counterplay restraint", "counterplay cut", "prophylaxis", "deny break"),
      allowedThemes = Set(ThemeTaxonomy.ThemeL1.RestrictionProphylaxis),
      preferredSubplans = Set(
        ThemeTaxonomy.SubplanId.ProphylaxisRestraint,
        ThemeTaxonomy.SubplanId.BreakPrevention,
        ThemeTaxonomy.SubplanId.KeySquareDenial
      )
    ),
    MotifDef(
      key = "opening_branch",
      label = "Opening branch",
      markers = List("opening branch", "opening theory", "theory"),
      allowedThemes = Set(ThemeTaxonomy.ThemeL1.OpeningPrinciples),
      preferredSubplans = Set(ThemeTaxonomy.SubplanId.OpeningDevelopment)
    )
  )

  def build(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      refs: Option[BookmakerRefsV1],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState]
  ): Option[BookmakerStrategicLedgerV1] =
    val digest = strategyPack.flatMap(_.signalDigest).orElse(NarrativeSignalDigestBuilder.build(ctx))
    val thesis = StrategicThesisBuilder.build(ctx)
    val decision = digest.flatMap(_.decisionComparison).orElse(DecisionComparisonBuilder.digest(ctx))
    val routeSignal = hasRouteSignal(ctx, strategyPack, digest)
    val carryOver = hasCarryOver(ctx, planStateToken, endgameStateToken)
    val planProfile = collectPlanProfile(ctx)
    val prerequisites = collectPrerequisites(ctx).take(2)
    val conversionTrigger = collectConversionTrigger(ctx, endgameStateToken)
    val motif = pickMotif(ctx, thesis, digest, planProfile, endgameStateToken, conversionTrigger)
    val primaryLine = choosePrimaryLine(ctx, decision, motif.map(_.motif.key), refs, probeResults)
    val resourceLine = chooseResourceLine(ctx, decision, refs, probeResults)
    val stage = classifyStage(
      ctx = ctx,
      thesis = thesis,
      digest = digest,
      decision = decision,
      planStateToken = planStateToken,
      endgameStateToken = endgameStateToken,
      carryOver = carryOver,
      prerequisites = prerequisites,
      conversionTrigger = conversionTrigger,
      routeSignal = routeSignal,
      primaryLine = primaryLine,
      resourceLine = resourceLine
    )
    val finalMotif =
      motif.orElse(
        Option.when(
          routeSignal && (carryOver || prerequisites.nonEmpty || primaryLine.exists(_.source != "variation") || resourceLine.exists(_.source != "variation"))
        )(MotifChoice(PieceRouteMotif, 1.0))
      )

    Option.when(
      shouldEmitLedger(
        motif = finalMotif,
        stage = stage,
        carryOver = carryOver,
        prerequisites = prerequisites,
        conversionTrigger = conversionTrigger,
        primaryLine = primaryLine,
        resourceLine = resourceLine,
        routeSignal = routeSignal
      )
    ) {
      val resolvedMotif = finalMotif.getOrElse(sys.error("ledger emission requires a motif"))
      BookmakerStrategicLedgerV1(
        motifKey = resolvedMotif.motif.key,
        motifLabel = resolvedMotif.motif.label,
        stageKey = stage.key,
        stageLabel = stage.label,
        carryOver = carryOver,
        stageReason = stage.reason,
        prerequisites = prerequisites,
        conversionTrigger = conversionTrigger,
        primaryLine = primaryLine.flatMap(_.toWire),
        resourceLine = resourceLine.flatMap(_.toWire)
      )
    }

  private def pickMotif(
      ctx: NarrativeContext,
      thesis: Option[StrategicThesis],
      digest: Option[NarrativeSignalDigest],
      planProfile: PlanProfile,
      endgameStateToken: Option[EndgamePatternState],
      conversionTrigger: Option[String]
  ): Option[MotifChoice] =
    val endgamePattern = currentEndgamePattern(ctx, endgameStateToken)
    val conversionReady =
      conversionTrigger.nonEmpty ||
        ctx.planContinuity.exists(_.phase == PlanLifecyclePhase.Fruition) ||
        endgamePattern.exists(isConversionEndgamePattern)
    val oppositePlanReady =
      hasOppositeBishopsConversionPlan(planProfile) && conversionReady
    val tokens = collectStructuredTokens(ctx, digest, endgamePattern)
    val scores = scala.collection.mutable.Map.empty[String, Double].withDefaultValue(0.0)

    Motifs.foreach { motif =>
      if motifEligible(motif, ctx, oppositePlanReady) then
        motif.markers.foreach { marker =>
          val normalizedMarker = normalize(marker)
          if tokens.exists(token => token.contains(normalizedMarker)) then
            scores.update(motif.key, scores(motif.key) + 1.0)
        }
    }

    thesis.foreach {
      case StrategicThesis(StrategicLens.Compensation, _, _, _, _) =>
        scores.update("compensation_attack", scores("compensation_attack") + 2.5)
      case StrategicThesis(StrategicLens.Prophylaxis, _, _, _, _) =>
        scores.update("counterplay_restraint", scores("counterplay_restraint") + 2.5)
      case StrategicThesis(StrategicLens.Opening, _, _, _, _) =>
        scores.update("opening_branch", scores("opening_branch") + 2.0)
      case _ =>
    }

    if CompensationInterpretation.effectiveSemanticDecision(ctx).exists(_.decision.signal.investedMaterial.exists(_ > 0)) then
      scores.update("compensation_attack", scores("compensation_attack") + 3.0)
    if ctx.semantic.exists(_.preventedPlans.nonEmpty) then
      scores.update("counterplay_restraint", scores("counterplay_restraint") + 3.0)
    if ctx.openingData.flatMap(_.name).exists(_.trim.nonEmpty) || ctx.openingEvent.isDefined then
      scores.update("opening_branch", scores("opening_branch") + 2.0)
    if oppositePlanReady && hasOppositeBishopsSignal(ctx) then
      scores.update("opposite_bishops_conversion", scores("opposite_bishops_conversion") + 3.0)

    Motifs.foreach { motif =>
      planAlignmentScore(motif, planProfile).foreach { bonus =>
        scores.update(motif.key, scores(motif.key) + bonus)
      }
    }

    scores.toList
      .sortBy { case (key, score) => (-score, key) }
      .headOption
      .flatMap { case (key, score) =>
        Option.when(score > 0.0)(
          Motifs.find(_.key == key).map(MotifChoice(_, score))
        ).flatten
      }

  private def collectStructuredTokens(
      ctx: NarrativeContext,
      digest: Option[NarrativeSignalDigest],
      endgamePattern: Option[String]
  ): Set[String] =
    (
      ctx.mainStrategicPlans.flatMap(plan => List(plan.planId, plan.themeL1)) ++
        ctx.planContinuity.toList.flatMap(_.planId) ++
        ctx.semantic.toList.flatMap { semantic =>
          semantic.conceptSummary ++
            semantic.positionalFeatures.map(_.tagType) ++
            semantic.structureProfile.toList.flatMap(profile =>
              List(profile.primary, profile.centerState) ++ profile.evidenceCodes
            ) ++
            semantic.endgameFeatures.toList.flatMap(endgame =>
              List(endgame.primaryPattern, endgame.transition, Option(endgame.theoreticalOutcomeHint))
                .flatten
            ) ++
            semantic.planAlignment.toList.flatMap(alignment =>
              alignment.matchedPlanIds ++ alignment.missingPlanIds ++ alignment.reasonCodes
            )
        } ++
        endgamePattern.toList ++
        digest.toList.flatMap(d =>
          List(
            d.structureProfile,
            d.centerState,
            d.prophylaxisPlan,
            d.compensation
          ).flatten
        )
    ).flatMap(normalizedToken).toSet

  private def collectPlanProfile(ctx: NarrativeContext): PlanProfile =
    val themes =
      (
        ctx.mainStrategicPlans.flatMap { hypothesis =>
          val theme = ThemeTaxonomy.ThemeResolver.fromHypothesis(hypothesis)
          Option.when(theme != ThemeTaxonomy.ThemeL1.Unknown)(theme)
        } ++
          ctx.planContinuity.toList.flatMap(_.planId).flatMap { raw =>
            val theme = ThemeTaxonomy.ThemeResolver.fromPlanId(raw)
            Option.when(theme != ThemeTaxonomy.ThemeL1.Unknown)(theme)
          }
      ).toSet

    val subplans =
      (
        ctx.mainStrategicPlans.flatMap(ThemeTaxonomy.ThemeResolver.subplanFromHypothesis) ++
          ctx.planContinuity.toList.flatMap(_.planId).flatMap(ThemeTaxonomy.ThemeResolver.subplanFromPlanId)
      ).toSet

    PlanProfile(themes = themes, subplans = subplans)

  private def motifEligible(
      motif: MotifDef,
      ctx: NarrativeContext,
      oppositePlanReady: Boolean
  ): Boolean =
    motif.key match
      case "opposite_bishops_conversion" =>
        hasOppositeBishopsSignal(ctx) && oppositePlanReady
      case _ => true

  private def planAlignmentScore(
      motif: MotifDef,
      planProfile: PlanProfile
  ): Option[Double] =
    val subplanBonus =
      Option.when(motif.preferredSubplans.intersect(planProfile.subplans).nonEmpty)(2.2)
    val themeBonus =
      Option.when(motif.allowedThemes.intersect(planProfile.themes).nonEmpty)(1.1)
    subplanBonus.orElse(themeBonus)

  private def hasOppositeBishopsSignal(ctx: NarrativeContext): Boolean =
    ctx.semantic.exists(_.positionalFeatures.exists(_.tagType == "OppositeColorBishops"))

  private def hasOppositeBishopsConversionPlan(planProfile: PlanProfile): Boolean =
    planProfile.subplans.contains(ThemeTaxonomy.SubplanId.OppositeBishopsConversion) ||
      (
        planProfile.themes.contains(ThemeTaxonomy.ThemeL1.AdvantageTransformation) &&
          planProfile.subplans.exists(subplan =>
            Set(
              ThemeTaxonomy.SubplanId.SimplificationConversion,
              ThemeTaxonomy.SubplanId.PasserConversion,
              ThemeTaxonomy.SubplanId.InvasionTransition
            ).contains(subplan)
          )
      )

  private def currentEndgamePattern(
      ctx: NarrativeContext,
      endgameStateToken: Option[EndgamePatternState]
  ): Option[String] =
    endgameStateToken.flatMap(_.activePattern).filter(_.trim.nonEmpty)
      .orElse(ctx.semantic.flatMap(_.endgameFeatures).flatMap(_.primaryPattern).filter(_.trim.nonEmpty))

  private def isConversionEndgamePattern(raw: String): Boolean =
    val low = normalize(raw)
    low.contains("good bishop rook pawn conversion") ||
      low.contains("opposite colored bishops draw")

  private def classifyStage(
      ctx: NarrativeContext,
      thesis: Option[StrategicThesis],
      digest: Option[NarrativeSignalDigest],
      decision: Option[DecisionComparisonDigest],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState],
      carryOver: Boolean,
      prerequisites: List[String],
      conversionTrigger: Option[String],
      routeSignal: Boolean,
      primaryLine: Option[LineCandidate],
      resourceLine: Option[LineCandidate]
  ): StageChoice =
    ctx.planContinuity.map(_.phase) match
      case Some(PlanLifecyclePhase.Failure | PlanLifecyclePhase.Aborted) =>
        StageChoice(
          key = "blocked",
          label = "Blocked",
          reason =
            ctx.planContinuity.flatMap(_.abortedReason)
              .orElse(Some("Plan continuity was interrupted"))
        )
      case _ if thesis.exists(_.lens == StrategicLens.Prophylaxis) || digest.exists(d => d.prophylaxisPlan.isDefined || d.prophylaxisThreat.isDefined) =>
        val counterplayText =
          List(
            digest.flatMap(_.prophylaxisThreat).map(v => s"cuts out $v"),
            digest.flatMap(_.prophylaxisPlan).map(v => s"supports $v"),
            digest.flatMap(_.counterplayScoreDrop).map(v => s"${v}cp of counterplay is stripped away")
          ).flatten
        StageChoice(
          key = "restrain",
          label = "Restrain",
          reason = Option.when(counterplayText.nonEmpty)(counterplayText.mkString(" · ")).orElse(Some("Counterplay denial is the main task"))
        )
      case _ if ctx.planContinuity.exists(_.phase == PlanLifecyclePhase.Fruition) || conversionTrigger.nonEmpty || thesis.exists(_.lens == StrategicLens.Compensation) =>
        val conversionText =
          conversionTrigger
            .map(trigger => s"the edge now has to cash out through $trigger")
            .orElse(endgameStateToken.flatMap(_.activePattern).map(pattern => s"conversion has shifted into $pattern"))
            .orElse(Some("The accumulated edge is ready to be converted"))
        StageChoice(
          key = "convert",
          label = "Convert",
          reason = conversionText
        )
      case _ if ctx.planContinuity.exists(_.phase == PlanLifecyclePhase.Execution) || primaryLine.exists(_.source != "variation") || decision.exists(_.engineBestPv.nonEmpty) =>
        val executionReason =
          decision.flatMap(_.evidence)
            .orElse(ctx.decision.map(_.logicSummary))
            .map(trimSentence)
            .filter(_.nonEmpty)
            .orElse(Some("The current move is carrying the main route forward"))
        StageChoice(
          key = "execute",
          label = "Execute",
          reason = executionReason
        )
      case _ if carryOver || prerequisites.nonEmpty || routeSignal || resourceLine.exists(_.source != "variation") =>
        val buildReason =
          ctx.planContinuity.map(c => s"${c.planName} has been carried for ${c.consecutivePlies} plies")
            .orElse(prerequisites.headOption)
            .orElse(digest.flatMap(_.deploymentPurpose).map(trimSentence))
            .orElse(planStateToken.flatMap(activeTokenPlan).map(plan => s"$plan is still being assembled"))
            .map(trimSentence)
        StageChoice(
          key = "build",
          label = "Build",
          reason = buildReason.orElse(Some("The move improves the setup before the break"))
        )
      case _ =>
        StageChoice(
          key = "setup",
          label = "Setup",
          reason =
            planStateToken.flatMap(activeTokenPlan)
              .map(plan => s"$plan is still in its preparatory stage")
              .orElse(Some("The long plan is still being arranged"))
        )

  private def shouldEmitLedger(
      motif: Option[MotifChoice],
      stage: StageChoice,
      carryOver: Boolean,
      prerequisites: List[String],
      conversionTrigger: Option[String],
      primaryLine: Option[LineCandidate],
      resourceLine: Option[LineCandidate],
      routeSignal: Boolean
  ): Boolean =
    val strongStage = stage.key match
      case "blocked" | "restrain" | "convert" | "execute" => true
      case _                                               => false

    motif.nonEmpty &&
      (strongStage ||
        carryOver ||
        prerequisites.nonEmpty ||
        conversionTrigger.nonEmpty ||
        primaryLine.nonEmpty ||
        resourceLine.nonEmpty ||
        routeSignal)

  private def hasRouteSignal(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      digest: Option[NarrativeSignalDigest]
  ): Boolean =
    strategyPack.exists(_.pieceRoutes.nonEmpty) ||
      ctx.semantic.exists(_.pieceActivity.exists(_.keyRoutes.nonEmpty)) ||
      digest.exists(d => d.deploymentPiece.isDefined || d.deploymentRoute.nonEmpty || d.deploymentPurpose.isDefined)

  private def hasCarryOver(
      ctx: NarrativeContext,
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState]
  ): Boolean =
    ctx.planContinuity.exists(_.consecutivePlies >= 2) ||
      planStateToken.exists(token => activeTokenPlan(token).isDefined) ||
      endgameStateToken.exists(_.patternAge > 0)

  private def activeTokenPlan(token: PlanStateTracker): Option[String] =
    token.history.values.toList
      .flatMap(state => List(state.primary, state.secondary).flatten)
      .sortBy(c => (-c.consecutivePlies, -c.commitmentScore))
      .headOption
      .map(_.planName.trim)
      .filter(_.nonEmpty)

  private def collectPrerequisites(ctx: NarrativeContext): List[String] =
    (
      ctx.mainStrategicPlans.headOption.toList.flatMap(_.preconditions) ++
        ctx.plans.top5.headOption.toList.flatMap(_.missingPrereqs)
    ).map(trimSentence)
      .filter(_.nonEmpty)
      .distinct

  private def collectConversionTrigger(
      ctx: NarrativeContext,
      endgameStateToken: Option[EndgamePatternState]
  ): Option[String] =
    CompensationInterpretation.effectiveSemanticDecision(ctx)
      .flatMap(_.decision.signal.summary)
      .map(_.trim)
      .filter(_.nonEmpty)
      .orElse(ctx.decision.toList.flatMap(_.delta.planAdvancements).headOption.map(trimSentence).filter(_.nonEmpty))
      .orElse(endgameStateToken.flatMap(_.activePattern).map(trimSentence).filter(_.nonEmpty))

  private def choosePrimaryLine(
      ctx: NarrativeContext,
      decision: Option[DecisionComparisonDigest],
      motifKey: Option[String],
      refs: Option[BookmakerRefsV1],
      probeResults: List[ProbeResult]
  ): Option[LineCandidate] =
    val supportiveProbe =
      probeResults.flatMap(result => supportiveProbeCandidate(ctx, result, motifKey))
        .sortBy(candidate => (-candidate.weight, candidate.title))
        .headOption

    supportiveProbe
      .orElse(decisionLineCandidate(decision))
      .orElse(refVariationCandidate(refs, preferredIndex = 0))

  private def chooseResourceLine(
      ctx: NarrativeContext,
      decision: Option[DecisionComparisonDigest],
      refs: Option[BookmakerRefsV1],
      probeResults: List[ProbeResult]
  ): Option[LineCandidate] =
    val authoringBranch =
      ctx.authorEvidence.flatMap(authoringBranchCandidates)
        .sortBy(candidate => (-candidate.weight, candidate.title))
        .headOption
    val decisionBranch = decisionResourceLineCandidate(decision)
    val negativeProbe =
      probeResults.flatMap(resourceProbeCandidate(ctx, _))
        .sortBy(candidate => (-candidate.weight, candidate.title))
        .headOption
    val hasDeferredSignal =
      ctx.whyAbsentFromTopMultiPV.nonEmpty ||
        decision.exists(d => d.deferredMove.exists(_.trim.nonEmpty) || d.deferredReason.exists(_.trim.nonEmpty))
    val whyNotVariation =
      Option.when(hasDeferredSignal)(refVariationCandidate(refs, preferredIndex = 1)).flatten

    authoringBranch.orElse(decisionBranch).orElse(negativeProbe).orElse(whyNotVariation)

  private def supportiveProbeCandidate(
      ctx: NarrativeContext,
      result: ProbeResult,
      motifKey: Option[String]
  ): Option[LineCandidate] =
    val alignedMotif =
      motifKey.exists(key => result.keyMotifs.exists(m => normalize(m).contains(normalize(key))))
    val likelySupport = result.deltaVsBaseline >= -40 || alignedMotif
    Option.when(likelySupport) {
      val noteBits = List(
        result.objective.map(trimSentence).filter(_.nonEmpty),
        result.purpose.map(trimSentence).filter(_.nonEmpty),
        Option.when(result.deltaVsBaseline != 0)(s"${result.deltaVsBaseline}cp vs baseline")
      ).flatten.distinct
      LineCandidate(
        title = "Probe continuation",
        sanMoves = probeSanMoves(ctx, result),
        scoreCp = Some(result.evalCp),
        mate = result.mate,
        note = Option.when(noteBits.nonEmpty)(noteBits.mkString(" | ")),
        source = "probe",
        weight = 3.0 + Option.when(alignedMotif)(0.7).getOrElse(0.0) + result.depth.getOrElse(0) / 100.0
      )
    }.flatMap(candidate => candidate.toWire.map(_ => candidate))

  private def resourceProbeCandidate(
      ctx: NarrativeContext,
      result: ProbeResult
  ): Option[LineCandidate] =
    Option.when(result.deltaVsBaseline < 0) {
      val noteBits = List(
        result.objective.map(trimSentence).filter(_.nonEmpty),
        result.purpose.map(trimSentence).filter(_.nonEmpty),
        Option.when(result.deltaVsBaseline != 0)(s"${result.deltaVsBaseline}cp vs baseline")
      ).flatten.distinct
      LineCandidate(
        title = "Probe resource",
        sanMoves = probeSanMoves(ctx, result),
        scoreCp = Some(result.evalCp),
        mate = result.mate,
        note = Option.when(noteBits.nonEmpty)(noteBits.mkString(" | ")),
        source = "probe",
        weight = 2.4 + result.depth.getOrElse(0) / 100.0
      )
    }.flatMap(candidate => candidate.toWire.map(_ => candidate))

  private def probeSanMoves(
      ctx: NarrativeContext,
      result: ProbeResult
  ): List[String] =
    val baseFen = result.fen.filter(_.trim.nonEmpty).getOrElse(ctx.fen)
    val uciMoves = result.probedMove.toList ++ result.bestReplyPv.take(3)
    NarrativeUtils.uciListToSan(baseFen, uciMoves).map(_.trim).filter(_.nonEmpty).take(4)

  private def decisionLineCandidate(
      decision: Option[DecisionComparisonDigest]
  ): Option[LineCandidate] =
    decision.flatMap { digest =>
      Option.when(digest.engineBestPv.nonEmpty) {
        LineCandidate(
          title = "Engine path",
          sanMoves = digest.engineBestPv.take(4),
          scoreCp = digest.engineBestScoreCp,
          mate = None,
          note = digest.evidence.map(trimSentence).filter(_.nonEmpty),
          source = "decision_compare",
          weight = 2.0
        )
      }
    }.flatMap(candidate => candidate.toWire.map(_ => candidate))

  private def decisionResourceLineCandidate(
      decision: Option[DecisionComparisonDigest]
  ): Option[LineCandidate] =
    decision.flatMap { digest =>
      val deferredMove = digest.deferredMove.map(_.trim).filter(_.nonEmpty)
      val sanMoves =
        deferredMove match
          case Some(move) if digest.engineBestMove.contains(move) && digest.engineBestPv.nonEmpty =>
            digest.engineBestPv.take(4)
          case Some(move) => List(move)
          case None       => Nil
      Option.when(sanMoves.nonEmpty) {
        LineCandidate(
          title = "Deferred branch",
          sanMoves = sanMoves,
          scoreCp = digest.engineBestScoreCp,
          mate = None,
          note = digest.deferredReason.map(trimSentence).filter(_.nonEmpty),
          source = "decision_compare",
          weight = 2.5
        )
      }
    }.flatMap(candidate => candidate.toWire.map(_ => candidate))

  private def refVariationCandidate(
      refs: Option[BookmakerRefsV1],
      preferredIndex: Int
  ): Option[LineCandidate] =
    refs.flatMap { refsValue =>
      refsValue.variations.lift(preferredIndex).orElse(refsValue.variations.headOption)
    }.flatMap { variation =>
      val candidate = LineCandidate(
        title = if preferredIndex == 0 then "Aligned line" else "Deferred branch",
        sanMoves = variation.moves.take(4).map(_.san.trim).filter(_.nonEmpty),
        scoreCp = Some(variation.scoreCp),
        mate = variation.mate,
        note = Option.when(variation.depth > 0)(s"depth ${variation.depth}"),
        source = "variation",
        weight = if preferredIndex == 0 then 1.0 else 0.8
      )
      candidate.toWire.map(_ => candidate)
    }

  private def authoringBranchCandidates(evidence: QuestionEvidence): List[LineCandidate] =
    evidence.branches
      .map { branch =>
        val noteBits = List(
          Option.when(branch.keyMove.trim.nonEmpty)(s"key move ${branch.keyMove.trim}"),
          branch.depth.map(depth => s"depth $depth"),
          Option.when(evidence.purpose.trim.nonEmpty)(trimSentence(evidence.purpose))
        ).flatten
        LineCandidate(
          title = "Authoring branch",
          sanMoves = sanitizeSanLine(branch.line),
          scoreCp = branch.evalCp,
          mate = branch.mate,
          note = Option.when(noteBits.nonEmpty)(noteBits.mkString(" | ")),
          source = "authoring",
          weight = 2.6
        )
      }
      .filter(_.toWire.isDefined)

  private def sanitizeSanLine(raw: String): List[String] =
    Option(raw).getOrElse("")
      .replace('\n', ' ')
      .split("\\s+")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .filterNot(token => token.matches("^[0-9]+\\.{1,3}$"))
      .filterNot(token => token == "..." || token == "…")
      .take(4)

  private def normalizedToken(raw: String): Option[String] =
    Option(raw).map(normalize).filter(_.nonEmpty)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("")
      .replaceAll("([a-z])([A-Z])", "$1 $2")
      .trim
      .toLowerCase
      .replace('_', ' ')
      .replace('-', ' ')
      .replaceAll("""[^a-z0-9\s]+""", " ")
      .replaceAll("""\s+""", " ")
      .trim

  private def trimSentence(raw: String): String =
    Option(raw).getOrElse("").trim.stripSuffix(".")
