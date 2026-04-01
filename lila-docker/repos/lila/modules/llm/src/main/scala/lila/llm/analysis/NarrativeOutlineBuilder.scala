package lila.llm.analysis

import chess.*
import chess.format.Fen
import chess.variant.Standard
import lila.llm.model._
import lila.llm.model.authoring._
import lila.llm.model.strategic.VariationLine
import lila.llm.analysis.ThemeTaxonomy.{ ThemeL1, ThemeResolver, SubplanCatalog, SubplanId }

/**
 * NarrativeOutlineBuilder: SSOT for "what to say"
 *
 * Decision engine for narrative structure.
 * All "what to say" decisions happen here; Renderer only handles phrasing.
 */
object NarrativeOutlineBuilder:
  private val MaxOpeningPrecedents = 3
  private val PrecedentConfidenceThreshold = 0.62
  private val PrefixFamilyLimits = Map(
    "sequence" -> 1,
    "strategic_shift" -> 1,
    "engine" -> 1
  )
  private enum FragmentAuthority:
    case render_only
    case support_only
    case unsafe_as_truth
    case unsafe_as_lesson
    case candidate_for_future_lesson
    case requires_move_linked_anchor
  private case class AuthorityTaggedFragment(
    text: String,
    authority: FragmentAuthority,
    moveLinkedAnchor: Boolean = false,
    sceneGrounded: Boolean = false,
    evidenceBacked: Boolean = false,
    plannerOwned: Boolean = false,
    contractConsistent: Boolean = false,
    generalized: Boolean = false
  ):
    def rawText: String = Option(text).map(_.trim).getOrElse("")
    def hasRawText: Boolean = rawText.nonEmpty
    def groundedForAdmission: Boolean =
      moveLinkedAnchor || sceneGrounded || evidenceBacked || plannerOwned || contractConsistent
    def releasedText: String =
      if !hasRawText then ""
      else
        authority match
          case FragmentAuthority.unsafe_as_truth           => ""
          case FragmentAuthority.unsafe_as_lesson          => ""
          case FragmentAuthority.candidate_for_future_lesson => ""
          case FragmentAuthority.requires_move_linked_anchor =>
            if moveLinkedAnchor && groundedForAdmission then rawText else ""
          case FragmentAuthority.support_only =>
            if generalized && !groundedForAdmission then ""
            else rawText
          case _ => rawText

  private def supportFragment(
    text: String,
    moveLinkedAnchor: Boolean = false,
    sceneGrounded: Boolean = false,
    evidenceBacked: Boolean = false,
    plannerOwned: Boolean = false,
    contractConsistent: Boolean = false,
    generalized: Boolean = false
  ): AuthorityTaggedFragment =
    AuthorityTaggedFragment(
      text = text,
      authority = FragmentAuthority.support_only,
      moveLinkedAnchor = moveLinkedAnchor,
      sceneGrounded = sceneGrounded,
      evidenceBacked = evidenceBacked,
      plannerOwned = plannerOwned,
      contractConsistent = contractConsistent,
      generalized = generalized
    )

  private def releasedFragmentText(
    fragments: List[AuthorityTaggedFragment]
  ): String =
    fragments
      .map(_.releasedText)
      .filter(_.nonEmpty)
      .mkString(" ")
      .trim
  private def rawFragmentText(
    fragments: Iterable[AuthorityTaggedFragment]
  ): String =
    fragments
      .iterator
      .map(_.rawText)
      .filter(_.nonEmpty)
      .mkString(" ")
      .trim

  private case class BoardAnchor(text: String, consumedThreat: Boolean = false, consumedFact: Boolean = false)
  private case class AlternativeEngineSignal(
    rank: Option[Int],
    cpLoss: Option[Int],
    bestSan: Option[String]
  )
  private case class PrecedentSignal(
    triggerMove: String,
    replyMove: Option[String],
    pivotMove: Option[String],
    mechanism: OpeningBranchMechanism,
    confidence: Double
  )
  private case class PrecedentComparisonSummaryFamilies(
    ordinary: List[AuthorityTaggedFragment],
    sharedLesson: List[AuthorityTaggedFragment]
  )
  private case class PrecedentComparisonFragments(
    header: AuthorityTaggedFragment,
    items: List[AuthorityTaggedFragment],
    summary: Option[AuthorityTaggedFragment],
    sharedLesson: Option[AuthorityTaggedFragment]
  ):
    def renderedText: String =
      releasedFragmentText(List(header) ++ items ++ List(summary, sharedLesson).flatten)
  private case class OpeningPrecedentFragments(
    body: List[AuthorityTaggedFragment],
    summary: Option[AuthorityTaggedFragment] = None,
    sharedLesson: Option[AuthorityTaggedFragment] = None
  ):
    def bodyText: String = releasedFragmentText(body)
    def renderedText: String =
      releasedFragmentText(body ++ List(summary, sharedLesson).flatten)
    def fragmentCount: Int =
      body.count(_.hasRawText) + List(summary, sharedLesson).flatten.count(_.hasRawText)
    def nonEmpty: Boolean = renderedText.nonEmpty
  private case class AnnotationHintFragments(
    tagOnly: Option[AuthorityTaggedFragment] = None,
    difficulty: Option[AuthorityTaggedFragment] = None,
    terminal: Option[AuthorityTaggedFragment] = None
  ):
    def renderedHints: List[String] =
      terminal.orElse(tagOnly).orElse(difficulty).toList.map(_.releasedText).filter(_.nonEmpty)
  private case class AnnotationTextProjection(
    coreText: AuthorityTaggedFragment,
    severityTail: Option[AuthorityTaggedFragment] = None
  ):
    def renderedText: String =
      releasedFragmentText(List(coreText) ++ severityTail.toList)
  private case class RookEndgameFrame(
    attacker: Color,
    defender: Color,
    pawn: Square,
    attackerKing: Square,
    defenderKing: Square,
    attackerRook: Square,
    defenderRook: Square
  )

  private case class OpeningPrecedentLine(
    text: String,
    score: Int,
    year: Int,
    game: ExplorerGame,
    overlap: Int,
    metadataScore: Int,
    sequenceKey: String
  )
  private case class CrossBeatRepetitionState(
    usedStems: scala.collection.mutable.Set[String],
    prefixCounts: scala.collection.mutable.Map[String, Int],
    usedHypothesisFamilies: scala.collection.mutable.Set[String],
    usedHypothesisStems: scala.collection.mutable.Set[String],
    usedDifferencePrefixes: scala.collection.mutable.Set[String],
    usedDifferenceTails: scala.collection.mutable.Set[String]
  )
  private case class SelectedHypothesis(
    card: HypothesisCard,
    sourceMove: String
  )
  private case class WrapUpFragments(
    planner: List[AuthorityTaggedFragment],
    practical: Option[AuthorityTaggedFragment],
    compensation: Option[AuthorityTaggedFragment]
  ):
    def orderedFragments: List[AuthorityTaggedFragment] =
      planner.filter(_.hasRawText) ++
        List(practical, compensation).flatten.filter(_.hasRawText)
    def renderedText: String =
      releasedFragmentText(orderedFragments)
  private enum PrecedentRole:
    case Sequence
    case StrategicTransition
    case DecisionDriver

  def build(
      ctx: NarrativeContext,
      rec: TraceRecorder,
      truthContract: Option[DecisiveTruthContract] = None,
      strategyPack: Option[lila.llm.StrategyPack] = None
  ): (NarrativeOutline, OutlineDiagnostics) =
    val bead = Math.abs(ctx.hashCode)
    val beats = scala.collection.mutable.ListBuffer.empty[OutlineBeat]
    val crossBeatState = CrossBeatRepetitionState(
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashMap.empty[String, Int].withDefaultValue(0),
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashSet.empty[String]
    )
    var diag = OutlineDiagnostics()

    val isAnnotation = isMoveAnnotation(ctx)
    val questions = ctx.authorQuestions.sortBy(-_.priority).take(3)
    diag = diag.copy(selectedQuestions = questions)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack, truthContract)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract)
    diag =
      diag.copy(
        plannerPrimary = rankedPlans.primary.map(plan => s"${plan.questionKind}:${plan.fallbackMode}"),
        plannerSecondary = rankedPlans.secondary.map(plan => s"${plan.questionKind}:${plan.fallbackMode}"),
        plannerRejected =
          rankedPlans.rejected.map(rejected =>
            s"${rejected.questionKind}:${rejected.fallbackMode}:${rejected.reasons.mkString("+")}"
          ),
        plannerSceneType = Some(rankedPlans.ownerTrace.sceneType.wireName),
        plannerOwnerCandidates = rankedPlans.ownerTrace.ownerCandidateLabels,
        plannerAdmittedFamilies = rankedPlans.ownerTrace.admittedFamilyLabels,
        plannerDroppedFamilies = rankedPlans.ownerTrace.droppedFamilyLabels,
        plannerSupportMaterialSeparation = rankedPlans.ownerTrace.supportMaterialSeparationLabels,
        plannerProposedFamilyMappings = rankedPlans.ownerTrace.proposedFamilyMappingLabels,
        plannerDemotionReasons = rankedPlans.ownerTrace.demotionReasons,
        plannerSelectedQuestion = rankedPlans.ownerTrace.selectedQuestion.map(_.toString),
        plannerSelectedOwnerFamily = rankedPlans.ownerTrace.selectedOwnerFamily.map(_.wireName),
        plannerSelectedOwnerSource = rankedPlans.ownerTrace.selectedOwnerSource,
        surfaceReplayOutcome =
          Some(if rankedPlans.primary.nonEmpty then "outline_planner_owned" else "outline_no_primary")
      )

    val availablePurposes = ctx.authorEvidence.map(_.purpose).toSet
    diag = diag.copy(usedEvidencePurposes = availablePurposes)

    val missingPurposes = EvidencePlanner.getMissingPurposesFromEvidence(questions, ctx.authorEvidence)
    diag = diag.copy(missingEvidencePurposes = missingPurposes)

    // 1. MOVE HEADER
    if isAnnotation then
      buildMoveHeader(ctx, rec).foreach(beat => beats += annotateBeat(beat))

    // 2. CONTEXT
    beats += annotateBeat(buildContextBeat(ctx, rec, bead))

    // 3. DECISION POINT
    buildDecisionBeat(ctx, questions, rankedPlans, plannerInputs, rec).foreach(beat => beats += annotateBeat(beat))

    // 4. EVIDENCE (from authorEvidence OR engineEvidence fallback)
    buildEvidenceBeat(ctx, questions, rankedPlans, rec).foreach(beat => beats += annotateBeat(beat))

    // 5. TEACHING POINT (lower threshold for visibility)
    buildTeachingBeat(ctx, rankedPlans, rec).foreach(beat => beats += annotateBeat(beat))

    val collapsedEarlyOpening = EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx, truthContract)

    // 6. MAIN MOVE
    val moveLevelPrecedent =
      Option.when(!collapsedEarlyOpening)(buildContextPrecedentSentence(ctx, bead)).flatten
    val mainMoveBeat = annotateBeat(buildMainMoveBeat(ctx, rec, isAnnotation, bead, moveLevelPrecedent, crossBeatState))
    if mainMoveBeat.text.nonEmpty then beats += mainMoveBeat
    LogicReconstructor.analyze(ctx).foreach { recon =>
      beats += annotateBeat(OutlineBeat(
        kind = OutlineBeatKind.PsychologicalVerdict,
        text = recon.description,
        conceptIds = List(s"psych_${recon.kind.toString.toLowerCase}"),
        focusPriority = 58
      ))
    }

    // 7. OPENING THEORY
    buildOpeningTheoryBeat(
      ctx,
      rec,
      suppressPrecedents = collapsedEarlyOpening || moveLevelPrecedent.nonEmpty
    ).foreach(beat => beats += annotateBeat(beat))

    // 8. ALTERNATIVES
    val altBeat = annotateBeat(buildAlternativesBeat(ctx, rec, bead, crossBeatState))
    if altBeat.text.nonEmpty then beats += altBeat

    // 9. WRAP-UP
    if !collapsedEarlyOpening then
      buildWrapUpBeat(ctx, rankedPlans, bead).foreach(beat => beats += annotateBeat(beat))

    (NarrativeOutline(beats.toList, Some(diag)), diag)

  private def hasImmediateTacticalPriority(
      ctx: NarrativeContext,
      plans: List[PlanHypothesis]
  ): Boolean =
    val planTaggedTactical =
      plans.headOption.exists { p =>
        val lowName = p.planName.toLowerCase
        val lowId = p.planId.toLowerCase
        lowName.contains("immediate tactical gain") ||
        lowName.contains("mating") ||
        lowId.contains("counterplay")
      }
    val urgentThreat =
      ctx.threats.toThem.headOption.exists(t => t.lossIfIgnoredCp >= Thresholds.URGENT_THREAT_CP || t.kind.equalsIgnoreCase("Mate"))
    planTaggedTactical || urgentThreat

  private def summarizeStrategicEvidence(
      ctx: NarrativeContext,
      plans: List[PlanHypothesis]
  ): String =
    val redundantKeys = primaryStrategicKeys(ctx, plans)
    val sourceTokens =
      plans
        .flatMap(_.evidenceSources)
        .flatMap { source =>
          val semanticKeys = evidenceSemanticKeys(source)
          FullGameDraftNormalizer
            .humanizeEvidenceSource(source)
            .filter(_ => !(semanticKeys.nonEmpty && semanticKeys.subsetOf(redundantKeys)))
        }
        .filter(_.nonEmpty)
    val probePurposes =
      ctx.authorEvidence
        .map(_.purpose)
        .flatMap { purpose =>
          val semanticKeys = evidenceSemanticKeys(purpose)
          FullGameDraftNormalizer
            .humanizeEvidenceSource(purpose)
            .filter(_ => !(semanticKeys.nonEmpty && semanticKeys.subsetOf(redundantKeys)))
        }
        .filter(_.nonEmpty)
        .distinct
        .take(2)
    (sourceTokens ++ probePurposes).distinct.take(4).mkString(", ")

  private val SubplanNarrativeSlots: Map[String, ThemeSlots] = Map(
    "prophylaxis_restraint" -> ThemeSlots(
      idea = "Prophylaxis route: first deny the opponent break windows before expanding.",
      evidence = "Evidence should show opponent break attempts losing force after our preparatory moves.",
      hold = "If break denial is not visible in probe replies, this restraint route stays conditional."
    ),
    "outpost_entrenchment" -> ThemeSlots(
      idea = "Entrenchment route: stabilize a piece on a durable outpost and build play around it.",
      evidence = "Evidence should confirm the outpost cannot be chased profitably in key reply lines.",
      hold = "If the outpost is removable with tempo, this route is deferred."
    ),
    "rook_pawn_march" -> ThemeSlots(
      idea = "Rook-pawn march route: use flank pawn expansion to build attacking infrastructure.",
      evidence = "Evidence must show hook/contact creation survives central counterplay in probe branches.",
      hold = "If center breaks punish the pawn march timing, the plan is held."
    ),
    "hook_creation" -> ThemeSlots(
      idea = "Hook-creation route: fix a pawn contact point and attack behind it.",
      evidence = "Evidence should show files/diagonals open after hook pressure under best defense.",
      hold = "If the hook dissolves or cannot be fixed, this route is postponed."
    ),
    "central_break_timing" -> ThemeSlots(
      idea = "Break-timing route: prepare a central break only after coordination is complete.",
      evidence = "Evidence needs stable center control before and after the break attempt.",
      hold = "If break timing loses control or material, keep it as a latent plan."
    ),
    "forcing_tactical_shot" -> ThemeSlots(
      idea = "Forcing tactical route overrides slow planning when concrete gain is available.",
      evidence = "Evidence requires forcing continuations that hold under best defensive replies.",
      hold = "If forcing lines are not stable, strategic plans regain priority."
    )
  )

  private def slotsForPlan(plan: PlanHypothesis): ThemeSlots =
    subplanIdOfHypothesis(plan)
      .flatMap(SubplanNarrativeSlots.get)
      .orElse(subplanIdOfHypothesis(plan).flatMap(generatedSubplanSlots))
      .getOrElse(ThemeNarrativeSlots.forTheme(themeIdOfHypothesis(plan)))

  private def generatedSubplanSlots(subplanId: String): Option[ThemeSlots] =
    SubplanId
      .fromId(subplanId)
      .flatMap(sp => SubplanCatalog.specs.get(sp).map(spec => sp -> spec))
      .map { case (sp, spec) =>
        val route = sp.id.replace("_", " ")
        val required =
          if spec.requiredSignals.nonEmpty then
            spec.requiredSignals
              .map {
                case "replyPvs"       => "best-reply probe lines"
                case "l1Delta"        => "stable structural change"
                case "futureSnapshot" => "future-position snapshots"
                case "keyMotifs"      => "supporting tactical motifs"
                case other            => other.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase
              }
              .mkString(" and ")
          else "probe contract"
        ThemeSlots(
          idea = s"${route.capitalize} route: ${spec.objective}.",
          evidence = s"Evidence should show $required over a ${spec.horizon}-term horizon.",
          hold = "If required signals are missing or refuted, this route remains conditional."
        )
      }

  private def themeIdOfHypothesis(plan: PlanHypothesis): String =
    ThemeL1.fromId(plan.themeL1)
      .filter(_ != ThemeL1.Unknown)
      .map(_.id)
      .getOrElse(ThemeResolver.fromHypothesis(plan).id)

  private def subplanIdOfHypothesis(plan: PlanHypothesis): Option[String] =
    plan.subplanId
      .flatMap(SubplanId.fromId)
      .map(_.id)
      .orElse(ThemeResolver.subplanFromHypothesis(plan).map(_.id))

  private def routeNameRestatesPrimarySubplan(plan: PlanHypothesis): Boolean =
    val primarySubplan = subplanIdOfHypothesis(plan).flatMap(SubplanId.fromId)
    primarySubplan.exists { subplan =>
      ThemeResolver.subplanFromPlanName(plan.planName).contains(subplan) ||
      ThemeResolver.subplanFromPlanId(plan.planId).contains(subplan)
    }

  private def primaryStrategicKeys(
      ctx: NarrativeContext,
      plans: List[PlanHypothesis]
  ): Set[String] =
    val planKeys =
      plans.headOption.toList.flatMap { plan =>
        val theme = ThemeResolver.fromHypothesis(plan)
        val subplan = ThemeResolver.subplanFromHypothesis(plan)
        val directKeys =
          plan.evidenceSources.flatMap(evidenceSemanticKeys) ++
            evidenceSemanticKeys(plan.planName) ++
            evidenceSemanticKeys(plan.planId)
        directKeys ++
          Option.when(theme != ThemeL1.Unknown)(s"theme:${theme.id}") ++
          subplan.map(sp => s"subplan:${sp.id}")
      }
    planKeys.toSet

  private def evidenceSemanticKeys(raw: String): Set[String] =
    val themeKeys =
      List(
        ThemeResolver.fromEvidenceSource(raw),
        ThemeResolver.fromPlanName(raw),
        ThemeResolver.fromPlanId(raw),
        ThemeResolver.fromSeedId(raw)
      ).filter(_ != ThemeL1.Unknown).map(theme => s"theme:${theme.id}")
    val subplanKeys =
      List(
        ThemeResolver.subplanFromEvidenceSource(raw),
        ThemeResolver.subplanFromPlanName(raw),
        ThemeResolver.subplanFromPlanId(raw),
        ThemeResolver.subplanFromSeedId(raw)
      ).flatten.map(subplan => s"subplan:${subplan.id}")
    (themeKeys ++ subplanKeys).toSet

  private def renderPreconditions(plan: PlanHypothesis): Option[String] =
    val items = plan.preconditions.map(FullGameDraftNormalizer.humanizeConstraint).filter(_.nonEmpty).take(2)
    Option.when(items.nonEmpty)(items.mkString("; "))

  private def strategicPlanNames(ctx: NarrativeContext): List[String] =
    StrategicNarrativePlanSupport.evidenceBackedPlanNames(ctx)

  private def topStrategicPlanName(ctx: NarrativeContext): Option[String] =
    strategicPlanNames(ctx).headOption

  def isMoveAnnotation(ctx: NarrativeContext): Boolean =
    ctx.playedMove.isDefined && ctx.playedSan.isDefined

  private def buildBookmakerPracticalWrapSentence(pa: PracticalInfo): Option[String] =
    val verdict = Option(pa.verdict).map(_.trim).filter(_.nonEmpty)
    val drivers = summarizePracticalDrivers(pa.biasFactors, limit = 2)
    if verdict.isEmpty && drivers.isEmpty then None
    else
      val verdictText = verdict.map(_.stripSuffix(".").toLowerCase)
      val sentence =
        (verdictText, drivers) match
          case (Some(v), ds) if ds.nonEmpty => s"Practically, that leaves a $v task because ${ds.mkString(" and ")}."
          case (Some(v), _)                 => s"Practically, that leaves a $v task."
          case (None, ds) if ds.nonEmpty    => s"Practically, the workload is defined by ${ds.mkString(" and ")}."
          case _                            => ""
      Option.when(sentence.nonEmpty)(sentence)

  private def buildBookmakerCompensationWrapSentence(
    signal: CompensationInterpretation.Signal
  ): Option[String] =
    val plan = signal.summary.map(_.trim).filter(_.nonEmpty)
    val vectors = summarizeCompensationVectorLabels(signal.vectors, limit = 2)
    if signal.investedMaterial.forall(_ <= 0) || (plan.isEmpty && vectors.isEmpty) then None
    else
      val vectorText = Option.when(vectors.nonEmpty)(s" through ${vectors.mkString(" and ")}").getOrElse("")
      val planText = plan.getOrElse("practical follow-up")
      Some(s"The investment still has to justify itself through $planText$vectorText.")

  private def buildMoveHeader(ctx: NarrativeContext, rec: TraceRecorder): Option[OutlineBeat] =
    ctx.playedSan.map { san =>
      val moveNum = (ctx.ply + 1) / 2
      val prefix = if ctx.ply % 2 == 1 then s"$moveNum." else s"$moveNum..."
      val bead = Math.abs(ctx.hashCode)
      
      val evaluation = OpeningGoals.analyze(ctx)

      val text = evaluation match
        case Some(eval) =>
          val desc = NarrativeLexicon.getGoalStatusDescription(bead, eval)
          if desc.nonEmpty then s"$prefix $san: $desc"
          else s"$prefix $san"
        case None => s"$prefix $san"

      rec.use("playedSan", san, "Move header")
      OutlineBeat(kind = OutlineBeatKind.MoveHeader, text = text, anchors = List(san))
    }

  private def buildContextBeat(ctx: NarrativeContext, rec: TraceRecorder, bead: Int): OutlineBeat =
    val parts = scala.collection.mutable.ListBuffer[AuthorityTaggedFragment]()
    val concepts = scala.collection.mutable.ListBuffer[String]()
    val phase = ctx.phase.current
    val conceptSummaryMotifs = ctx.semantic.toList.flatMap(_.conceptSummary).map(_.trim).filter(_.nonEmpty).distinct
    val derivedContextMotifs = collectDerivedContextMotifs(ctx)
    val conceptMotifs = (conceptSummaryMotifs ++ derivedContextMotifs).distinct
    val deltaMotifs = ctx.delta.map(_.newMotifs).getOrElse(Nil)
    val counterfactualMotifs = Nil
    val motifs = (deltaMotifs ++ counterfactualMotifs ++ conceptMotifs).distinct
    val motifSignals = motifs.map(normalizeMotifKey).filter(_.nonEmpty)
    val highTensionByMotif =
      motifSignals.exists { m =>
        List(
          "mate",
          "sacrifice",
          "king_hunt",
          "smothered",
          "greek_gift",
          "fork",
          "skewer",
          "deflection",
          "interference",
          "zwischenzug"
        ).exists(m.contains)
      }
    val highTensionByThreat =
      ctx.threats.toUs.headOption.exists(t => t.lossIfIgnoredCp >= 250 || t.kind.equalsIgnoreCase("Mate"))
    val highTension = highTensionByMotif || highTensionByThreat
    val motifHash = motifSignals.foldLeft(0)((acc, m) => acc ^ Math.abs(m.hashCode))

    // Position statement and Asymmetric Imbalance
    val evalOpt = rankedEngineVariations(ctx).headOption.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp))
    val strategicNames = strategicPlanNames(ctx)
    val wPlan = strategicNames.headOption
    val bPlan = strategicNames.lift(1)
    
    // Extract Imbalance if evaluation is balanced (-80 to +80)
    val isBalanced = evalOpt.exists(cp => cp >= -80 && cp <= 80)
    val imbalanceOpt = if (isBalanced) buildImbalanceContrast(ctx) else None

    val evalText = imbalanceOpt match {
      case Some((whiteAdv, blackAdv)) =>
        NarrativeLexicon.getEvaluativeImbalanceStatement(bead ^ 0x1b873593, evalOpt.getOrElse(0), whiteAdv, blackAdv, ply = ctx.ply)
      case None =>
        evalOpt.map(cp => NarrativeLexicon.getEvaluativePlanStatement(bead ^ 0x1b873593, cp, wPlan, bPlan, ply = ctx.ply)).getOrElse("unclear")
    }

    val openingSeed = bead ^ Math.abs(phase.hashCode) ^ evalOpt.getOrElse(0) ^ motifHash ^ 0x1b873593
    val openingPart = NarrativeLexicon.getOpening(openingSeed, phase, evalText, tactical = highTension, ply = ctx.ply)
    val keyFact = pickKeyFact(ctx)
    val salientConceptMotifs =
      conceptMotifs.filter(NarrativeLexicon.isMotifPrefixSignal)
    val conceptLeadMotifs =
      if salientConceptMotifs.nonEmpty then salientConceptMotifs.take(2)
      else if ctx.ply % 3 == 0 then conceptMotifs.take(2)
      else Nil
    val deltaMotifSignals = deltaMotifs.map(normalizeMotifKey).filter(_.nonEmpty)
    val counterfactualMotifSignals = counterfactualMotifs.map(normalizeMotifKey).filter(_.nonEmpty)
    val conceptSummarySignals = conceptSummaryMotifs.map(normalizeMotifKey).filter(_.nonEmpty)
    val derivedContextSignals = derivedContextMotifs.map(normalizeMotifKey).filter(_.nonEmpty)
    val trustedConceptThemeSignals =
      conceptSummaryMotifs
        .map(_.trim)
        .filter(_.nonEmpty)
        .filter { raw =>
          val motif = normalizeMotifKey(raw)
          val stableStrategicConcept =
            List(
              "minority_attack",
              "bad_bishop",
              "bishop_pair",
              "opposite_bishops",
              "isolated_pawn",
              "hanging_pawns",
              "outpost",
              "open_file",
              "semi_open_file",
              "color_complex"
            ).exists(motif.contains)
          motif.nonEmpty &&
          motifPhaseCompatible(motif, phase) && (
            derivedContextSignals.exists(sig => motifSignalMatches(sig, motif)) ||
            deltaMotifSignals.exists(sig => motifSignalMatches(sig, motif)) ||
            counterfactualMotifSignals.exists(sig => motifSignalMatches(sig, motif)) ||
            keyFact.exists(f => motifCorroboratedByFact(motif, f)) ||
            motifCorroboratedByThreat(motif, ctx.threats.toUs) ||
            motifCorroboratedByPawnPlay(motif, ctx.pawnPlay) ||
            stableStrategicConcept
          )
        }
        .distinct
    val motifPrefixCandidates =
      (conceptLeadMotifs ++ derivedContextMotifs ++ deltaMotifs ++ counterfactualMotifs)
        .map(_.trim)
        .filter(_.nonEmpty)
        .filter(m => motifPhaseCompatible(m, phase))
        .distinct
        .filter { raw =>
          isTrustedMotifPrefixCandidate(
            rawMotif = raw,
            deltaSignals = deltaMotifSignals,
            counterfactualSignals = counterfactualMotifSignals,
            conceptSummarySignals = conceptSummarySignals,
            derivedSignals = derivedContextSignals,
            keyFact = keyFact,
            threatsToUs = ctx.threats.toUs,
            pawnPlay = ctx.pawnPlay,
            phase = phase
          )
        }
        .take(4)
    val endgamePatternPrefix =
      ctx.semantic
        .flatMap(_.endgameFeatures.flatMap(_.primaryPattern))
        .flatMap(p => NarrativeLexicon.getEndgamePatternPrefix(bead ^ motifHash ^ 0x6f2b3d17, p))
    val endgameContinuityText =
      ctx.semantic
        .flatMap(_.endgameFeatures)
        .flatMap(buildEndgameContinuitySentence)
    val endgameCausalityText =
      ctx.semantic
        .flatMap(_.endgameFeatures)
        .flatMap(info => buildEndgameCausalitySentence(ctx, info))
    val motifPrefix = NarrativeLexicon.getMotifPrefix(bead ^ motifHash, motifPrefixCandidates, ply = ctx.ply)

    val boardAnchor = buildBoardAnchor(ctx, keyFact, bead)
    boardAnchor.foreach(a => parts += supportFragment(a.text, sceneGrounded = true))

    endgamePatternPrefix
      .map(_.trim)
      .filter(_.nonEmpty)
      .foreach(text => parts += supportFragment(text, sceneGrounded = true, generalized = true))
    endgameContinuityText
      .map(_.trim)
      .filter(_.nonEmpty)
      .foreach(text => parts += supportFragment(text, sceneGrounded = true, generalized = true))
    endgameCausalityText
      .map(_.trim)
      .filter(_.nonEmpty)
      .foreach(text => parts += supportFragment(text, sceneGrounded = true, generalized = true))
    motifPrefix
      .map(_.trim)
      .filter(_.nonEmpty)
      .foreach(text => parts += supportFragment(text, sceneGrounded = true, generalized = true))
    Option(openingPart.trim)
      .filter(_.nonEmpty)
      .foreach(text => parts += AuthorityTaggedFragment(text, FragmentAuthority.unsafe_as_truth))

    val existingThemeText = rawFragmentText(parts.toList)
    val themeSignalPool =
      (derivedContextMotifs ++ deltaMotifs ++ counterfactualMotifs ++ trustedConceptThemeSignals)
        .distinct
    buildThemeKeywordSentence(
      motifs = themeSignalPool,
      existingText = existingThemeText,
      bead = bead ^ 0x6d2b79f5,
      ply = ctx.ply,
      phase = phase
    )
      .foreach(text => parts += supportFragment(text, sceneGrounded = true, generalized = true))
    val canonicalTermPool =
      (conceptSummaryMotifs ++ derivedContextMotifs ++ deltaMotifs ++ counterfactualMotifs).distinct
    buildCanonicalMotifTermSentence(
      motifs = canonicalTermPool,
      existingText = rawFragmentText(parts.toList),
      bead = bead ^ 0x31af9d42,
      ply = ctx.ply,
      phase = phase
    ).foreach(text => parts += supportFragment(text, sceneGrounded = true, generalized = true))

    // Main threat if exists (adds drama)
    ctx.threats.toUs.headOption.filter(_.lossIfIgnoredCp >= 30).foreach { t =>
      rec.use("threats.toUs[0]", t.kind, "Context threat")
      if !boardAnchor.exists(_.consumedThreat) then
        parts +=
          supportFragment(
            NarrativeLexicon.getThreatStatement(bead, t.kind, t.lossIfIgnoredCp, Some(ctx)),
            sceneGrounded = true,
            generalized = true
          )
      concepts += s"threat_${t.kind}"
    }

    // Top strategic route (partition-authoritative by default). Filter speculative tactical labels unless board evidence supports them.
    topStrategicPlanName(ctx).filter { planName =>
      val planKey = normalizeMotifKey(planName)
      val needsTacticalProof =
        List("sacrifice", "mate", "smothered", "trap", "combination").exists(planKey.contains)
      val sacrificeSpecific =
        planKey.contains("sacrifice")
      val hasSacrificeEvidence =
        ctx.candidates.exists { c =>
          val evidenceLow = c.tacticEvidence.mkString(" ").toLowerCase
          val whyLow = c.whyNot.getOrElse("").toLowerCase
          evidenceLow.contains("sacrifice") ||
            evidenceLow.contains("exchange_sacrifice") ||
            whyLow.contains("sacrifice")
        } ||
          ctx.delta.map(_.newMotifs.mkString(" ").toLowerCase.contains("sacrifice")).getOrElse(false)
      if !needsTacticalProof then true
      else if sacrificeSpecific then hasSacrificeEvidence
      else
        val hasTacticalProof =
          ctx.candidates.exists { c =>
            c.tags.exists(tag => tag == CandidateTag.Sharp || tag == CandidateTag.TacticalGamble) ||
              c.facts.exists {
                case _: Fact.Fork | _: Fact.Pin | _: Fact.Skewer | _: Fact.HangingPiece => true
                case _                                                                   => false
              }
          } ||
            ctx.threats.toUs.exists(t =>
              t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP || t.kind.toLowerCase.contains("mate")
            )
        hasTacticalProof
    }.foreach { planName =>
      rec.use("strategic.main[0]", planName, "Context plan")
      parts +=
        supportFragment(
          NarrativeLexicon.getPlanStatement(
            bead ^ Math.abs(planName.hashCode) ^ 0x2b2b2b,
            planName,
            Some(ctx),
            ply = ctx.ply
          ),
          sceneGrounded = true,
          generalized = true
        )
      concepts += s"plan_$planName"
    }
    buildOpeningContextSentence(ctx, rawFragmentText(parts.toList)).foreach { openingText =>
      rec.use("openingData", openingText, "Context opening")
      parts += supportFragment(openingText, sceneGrounded = true, generalized = true)
      concepts += "opening_context"
    }
    buildStrategicStackContextSentence(ctx, rawFragmentText(parts.toList)).foreach { stackText =>
      rec.use("mainStrategicPlans", stackText, "Context strategic stack")
      parts += supportFragment(stackText, sceneGrounded = true, generalized = true)
      concepts += "strategic_stack"
    }
    buildStructuralContextSentence(ctx, rawFragmentText(parts.toList)).foreach { structureText =>
      rec.use("semantic.planAlignment", structureText, "Context structure")
      parts += supportFragment(structureText, sceneGrounded = true, generalized = true)
      concepts += "structural_context"
    }
    buildStrategicFlowContextSentence(ctx, rawFragmentText(parts.toList)).foreach { flowText =>
      rec.use("strategicFlow", flowText, "Context flow")
      parts += supportFragment(flowText, sceneGrounded = true, generalized = true)
      concepts += "strategic_flow"
    }
    buildOpponentPlanContextSentence(ctx, rawFragmentText(parts.toList)).foreach { opponentText =>
      rec.use("opponentPlan", opponentText, "Context opponent")
      parts += supportFragment(opponentText, sceneGrounded = true, generalized = true)
      concepts += "opponent_plan"
    }
    buildMetaContextSentence(ctx, rawFragmentText(parts.toList)).foreach { metaText =>
      rec.use("meta.choiceType", metaText, "Context meta")
      parts += supportFragment(metaText, sceneGrounded = true, generalized = true)
      concepts += "decision_context"
    }

    // One concrete, verified observation to avoid generic boilerplate.
    if !boardAnchor.exists(_.consumedFact) then
      keyFact.foreach { fact =>
        val factText = NarrativeLexicon.getFactStatement(bead ^ Math.abs(fact.hashCode), fact, ctx)
        if factText.nonEmpty then
          parts += supportFragment(factText, sceneGrounded = true, generalized = true)
      }
    ctx.pawnPlay.breakFile.foreach { br =>
      rec.use("pawnPlay.breakFile", br, "Context break")
      parts +=
        supportFragment(
          NarrativeLexicon.getPawnPlayStatement(bead, br, ctx.pawnPlay.breakImpact, ctx.pawnPlay.tensionPolicy),
          sceneGrounded = true,
          generalized = true
        )
      concepts += "pawn_break_ready"
    }

    OutlineBeat(
      kind = OutlineBeatKind.Context,
      text = releasedFragmentText(parts.toList),
      conceptIds = concepts.toList,
      focusPriority = 100,
      fullGameEssential = true
    )

  private def buildDecisionBeat(
    ctx: NarrativeContext,
    questions: List[AuthorQuestion],
    rankedPlans: RankedQuestionPlans,
    plannerInputs: QuestionPlannerInputs,
    rec: TraceRecorder
  ): Option[OutlineBeat] =
    rankedPlans.primary match
      case Some(plan) =>
        rec.use(s"planner.primary.${plan.questionId}", plan.claim, "Decision point")
        val text =
          List(
            Some(plan.claim),
            plan.contrast.filterNot(contrast => plan.claim.equalsIgnoreCase(contrast))
          ).flatten.mkString(" ").trim
        Some(
          OutlineBeat(
            kind = OutlineBeatKind.DecisionPoint,
            text = text,
            questionIds = List(plan.questionId),
            questionKinds = List(plan.questionKind),
            expectedEvidencePurposes = plan.evidence.map(_.purposes).getOrElse(Nil),
            anchors =
              questions.find(_.id == plan.questionId).toList.flatMap(_.anchors) ++
                ctx.decision.flatMap(_.focalPoint.map(renderTargetRef)).toList,
            requiresEvidence = plan.evidence.nonEmpty,
            focusPriority = 96,
            fullGameEssential = true,
            supportKinds = Option.when(plan.evidence.nonEmpty)(List(OutlineBeatKind.Evidence)).getOrElse(Nil)
          )
        )
      case None if questions.nonEmpty =>
        plannerInputs.factualFallback.map { fallback =>
          OutlineBeat(
            kind = OutlineBeatKind.DecisionPoint,
            text = fallback,
            focusPriority = 96,
            fullGameEssential = true
          )
        }
      case None =>
        buildDecisionNarrativeText(ctx, None).map { text =>
          ctx.decision.foreach { d =>
            rec.use("decision.logicSummary", d.logicSummary, "Decision rationale")
          }
          ctx.meta.flatMap(_.whyNot).foreach { whyNot =>
            rec.use("meta.whyNot", whyNot, "Decision meta")
          }
          OutlineBeat(
            kind = OutlineBeatKind.DecisionPoint,
            text = text,
            anchors = ctx.decision.flatMap(_.focalPoint.map(renderTargetRef)).toList,
            focusPriority = 96,
            fullGameEssential = true
          )
        }

  private def buildDecisionNarrativeText(
    ctx: NarrativeContext,
    alignedQuestion: Option[String]
  ): Option[String] =
    val parts = scala.collection.mutable.ListBuffer[String]()
    alignedQuestion.map(ensureSentence).foreach(parts += _)
    ctx.decision.flatMap(buildDecisionRationaleSentence).foreach(parts += _)
    ctx.meta.flatMap(buildMetaDecisionSentence).foreach(parts += _)
    val alternativeSentence =
      AlternativeNarrativeSupport.sentence(ctx)
        .map(ensureSentence)
        .filter(_.nonEmpty)
    alternativeSentence.foreach { sentence =>
      val existing = parts.mkString(" ").toLowerCase
      val isCritical = CriticalAnnotationPolicy.shouldPrioritizeClaim(ctx)
      if parts.nonEmpty then
        if !existing.contains(sentence.toLowerCase) then parts += sentence
      else if !isCritical then
        parts += sentence
    }
    Option.when(parts.nonEmpty)(parts.mkString(" ").trim)

  private def buildStrategicFlowContextSentence(
    ctx: NarrativeContext,
    existingText: String
  ): Option[String] =
    ctx.strategicFlow
      .map(ensureSentence)
      .filter(sentenceIsNovel(_, existingText))

  private def buildOpeningContextSentence(
    ctx: NarrativeContext,
    existingText: String
  ): Option[String] =
    val sentence =
      ctx.openingEvent.flatMap {
        case OpeningEvent.OutOfBook(_, _, _) =>
          ctx.openingData.flatMap(_.name).map(name => s"This move already leaves the main $name reference paths.")
        case OpeningEvent.Novelty(_, cpLoss, _, _) =>
          val cost = if cpLoss > 0 then s" with only a small engine cost" else ""
          Some(s"This is effectively a novelty rather than a standard theory branch$cost.")
        case OpeningEvent.TheoryEnds(_, sampleCount) =>
          Some(s"Reference theory is already thinning out here, with only about $sampleCount games left in sample.")
        case OpeningEvent.BranchPoint(divergingMoves, _, _) if divergingMoves.nonEmpty =>
          Some(s"The opening now branches around ${joinWithOr(divergingMoves.take(3))}.")
        case OpeningEvent.Intro(_, name, _, _) =>
          Some(s"The game is still tracking $name territory.")
        case _ =>
          ctx.openingData.flatMap(_.name).map { name =>
            if ctx.openingData.exists(_.totalGames >= 12) then s"The structure still tracks known $name ideas."
            else s"Known $name references are already starting to thin out."
          }
      }
    sentence
      .map(ensureSentence)
      .filter(sentenceIsNovel(_, existingText))

  private def buildStrategicStackContextSentence(
    ctx: NarrativeContext,
    existingText: String
  ): Option[String] =
    val mainPlans = StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx)
    val primary = mainPlans.headOption
    val secondary = mainPlans.lift(1)

    val base =
      primary match
        case Some(main) if secondary.isDefined =>
          val backup = secondary.get.planName
          Some(s"The main plan remains ${main.planName}, with $backup as the backup route.")
        case Some(main) =>
          Some(s"The main plan remains ${main.planName}.")
        case None =>
          None

    base
      .map(ensureSentence)
      .filter(sentenceIsNovel(_, existingText))

  private def compactNarrativeReason(raw: String): String =
    Option(raw)
      .map(_.trim.replaceAll("\\s+", " "))
      .filter(_.nonEmpty)
      .map(_.stripPrefix("because ").stripSuffix("."))
      .getOrElse("")

  private def buildStructuralContextSentence(
    ctx: NarrativeContext,
    existingText: String
  ): Option[String] =
    val structure = ctx.semantic.flatMap(_.structureProfile)
    val sentence =
      ctx.semantic.flatMap(_.planAlignment).flatMap { alignment =>
        val structureClause = renderStructureProfileClause(structure)
        val band = alignment.band.trim.toLowerCase
        val intent = alignment.narrativeIntent.map(compactNarrativeReason).filter(_.nonEmpty)
        val risk = alignment.narrativeRisk.map(compactNarrativeReason).filter(_.nonEmpty)
        val reasons = alignment.reasonCodes.flatMap(humanizeAlignmentReason).distinct.take(2)
        def withLead(body: String): String =
          structureClause.map(lead => s"$lead $body").getOrElse(body)
        val base =
          band match
            case "offplan" =>
              intent
                .map(i => withLead(s"The move leans off the clean structural route because the position would rather $i."))
                .getOrElse(withLead("The move leans off the clean structural route."))
            case "playable" =>
              intent
                .map(i => withLead(s"The structure still supports $i, but move order matters."))
                .getOrElse(withLead("The fit is playable, but move order matters."))
            case "unknown" =>
              withLead("The structural read is still noisy, so concrete calculation has to confirm the plan.")
            case _ =>
              intent
                .map(i => withLead(s"The structure is coherent with $i."))
                .getOrElse(withLead("The move stays structurally coherent."))
        val caveat =
          risk
            .orElse(reasons.headOption)
            .filter(text => !base.toLowerCase.contains(text.toLowerCase))
            .map(text => s"The main structural caveat is $text.")
        Some(List(base, caveat.getOrElse("")).filter(_.trim.nonEmpty).mkString(" ").trim)
      }.orElse {
        ctx.semantic.flatMap(_.preventedPlans.headOption).flatMap(renderStructuralProphylaxisSentence)
      }
    sentence
      .map(ensureSentence)
      .filter(sentenceIsNovel(_, existingText))

  private def renderStructureProfileClause(
    structure: Option[StructureProfileInfo]
  ): Option[String] =
    structure.flatMap { sp =>
      val primary = Option(sp.primary).map(_.trim).filter(text => text.nonEmpty && !text.equalsIgnoreCase("Unknown"))
      val center = Option(sp.centerState).map(_.trim.toLowerCase).filter(_.nonEmpty)
      (primary, center) match
        case (Some(p), Some(c)) => Some(s"Structurally, the pawn structure is $p, with a $c center.")
        case (Some(p), None)    => Some(s"Structurally, the pawn structure is $p.")
        case (None, Some(c))    => Some(s"Structurally, the center is $c.")
        case _                  => None
    }

  private def humanizeAlignmentReason(raw: String): Option[String] =
    Option(raw).map(_.trim.toUpperCase).filter(_.nonEmpty).map {
      case "PA_MATCH"     => "the expected structure plan is present"
      case "PRECOND_MISS" => "some structural preconditions are still missing"
      case "ANTI_PLAN"    => "the move order fights the structure's cleanest route"
      case "LOW_CONF"     => "the structure classification is still uncertain"
      case code if code.startsWith("TOP_") => "the current top plan disagrees with the structure template"
      case code => code.toLowerCase.replace('_', ' ')
    }

  private def renderStructuralProphylaxisSentence(prevented: PreventedPlanInfo): Option[String] =
    val target =
      prevented.preventedThreatType.map(_.trim).filter(_.nonEmpty)
        .orElse(prevented.breakNeutralized.map(file => s"$file-break"))
    val impact =
      Option.when(prevented.counterplayScoreDrop >= 100)(s"blunting roughly ${prevented.counterplayScoreDrop}cp of counterplay")
    target.flatMap { t =>
      prevented.sourceScope match
        case FactScope.Now =>
          val tail = impact.map(i => s" and $i").getOrElse("")
          Some(s"Structurally, the move is also prophylactic because it cuts out $t$tail.")
        case _ =>
          prevented.citationLine.flatMap { citation =>
            val body =
              impact match
                case Some(i) => s"the opponent's counterplay through $t would come alive, $i"
                case None    => s"the opponent's counterplay through $t would come alive"
            LineScopedCitation.afterClause(citation, body)
          }
    }

  private def buildOpponentPlanContextSentence(
    ctx: NarrativeContext,
    existingText: String
  ): Option[String] =
    ctx.opponentPlan.flatMap { plan =>
      val evidenceClause =
        plan.evidence.headOption
          .map(_.trim)
          .filter(text => text.nonEmpty && text.length <= 72)
          .map(ev => s", backed by $ev")
          .getOrElse("")
      val base =
        if plan.isEstablished then s"The opponent already has ${plan.name} in motion$evidenceClause."
        else s"The opponent's main counterplan is ${plan.name}$evidenceClause."
      Option.when(sentenceIsNovel(base, existingText))(base)
    }

  private def buildMetaContextSentence(
    ctx: NarrativeContext,
    existingText: String
  ): Option[String] =
    ctx.meta.flatMap { meta =>
      val choiceSentence = meta.choiceType match
        case ChoiceType.OnlyMove => "Concrete pressure leaves very little choice here."
        case ChoiceType.NarrowChoice => "The margins are narrow, so move order matters."
        case ChoiceType.StyleChoice => "There is still some stylistic freedom here."
        case ChoiceType.Complex => "Several sharp branches keep the decision complex."
      val concurrencySentence =
        meta.planConcurrency.secondary.map { secondary =>
          meta.planConcurrency.relationship match
            case rel if rel.toLowerCase.contains("synergy") =>
              s"${meta.planConcurrency.primary} and $secondary reinforce each other."
            case rel if rel.toLowerCase.contains("conflict") =>
              s"${meta.planConcurrency.primary} and $secondary compete for priority."
            case _ =>
              s"${meta.planConcurrency.primary} and $secondary both matter in the position."
        }
      val merged = (choiceSentence :: concurrencySentence.toList).mkString(" ").trim
      Option.when(sentenceIsNovel(merged, existingText))(merged)
    }

  private def buildDecisionRationaleSentence(
    decision: DecisionRationale
  ): Option[String] =
    val parts = scala.collection.mutable.ListBuffer[String]()
    val idea = normalizeDecisionIdea(decision.logicSummary)
    if idea.nonEmpty then
      parts += s"The idea is straightforward: $idea."
    decision.focalPoint
      .map(renderTargetRef)
      .filter(_.nonEmpty)
      .foreach(label => parts += s"The focal point is $label.")
    buildDecisionDeltaSentence(decision.delta).foreach(parts += _)
    Option.when(parts.nonEmpty)(parts.mkString(" ").trim)

  private def buildDecisionDeltaSentence(delta: PVDelta): Option[String] =
    val actions = List(
      delta.resolvedThreats.headOption.map(threat => s"neutralizes $threat"),
      delta.newOpportunities.headOption.map(target => s"creates pressure on ${renderOpportunity(target)}"),
      delta.planAdvancements.headOption.map(renderPlanAdvancement)
    ).flatten
    val gainSentence =
      actions match
        case Nil => None
        case one :: Nil => Some(s"It mainly $one.")
        case a :: b :: _ => Some(s"It mainly $a and $b.")
    val concessionSentence =
      delta.concessions.headOption
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(concession => s"The tradeoff is $concession.")
    val rendered = List(gainSentence, concessionSentence).flatten.mkString(" ").trim
    Option.when(rendered.nonEmpty)(rendered)

  private def buildMetaDecisionSentence(meta: MetaSignals): Option[String] =
    meta.whyNot
      .map(_.replace("[Verified]", "").replaceAll("\\s+", " ").trim)
      .filter(_.nonEmpty)
      .filter(text => LineScopedCitation.hasInlineCitation(text) && !LineScopedCitation.hasSourceLabelOnly(text))
      .map(ensureSentence)
      .orElse {
        meta.errorClass.map { error =>
          if error.isTactical then "The alternative failure mode is tactical rather than cosmetic."
          else "The alternative failure mode is positional rather than tactical."
        }
      }
      .orElse {
        meta.divergence.flatMap(_.punisherMove.map(move => s"The punishment starts with $move."))
      }

  private def normalizeDecisionIdea(raw: String): String =
    Option(raw).getOrElse("")
      .trim
      .replace(" -> ", "; then ")
      .replace("->", "; then ")
      .replace("(probe needed for validation)", "probe confirmation is still needed")
      .replaceAll("\\s+", " ")
      .replace(" ;", ";")
      .stripSuffix(".")
      .stripPrefix("The idea is ")
      .stripPrefix("the idea is ")
      .trim match
        case "" => ""
        case text => lowerCaseFirst(text)

  private def renderPlanAdvancement(raw: String): String =
    val clean = Option(raw).getOrElse("").trim
    if clean.isEmpty then "advances the plan"
    else if clean.startsWith("Removed:") then s"removes ${clean.stripPrefix("Removed:").trim} as a blocker"
    else if clean.startsWith("Met:") then s"meets ${clean.stripPrefix("Met:").trim}"
    else s"advances $clean"

  private def renderOpportunity(raw: String): String =
    Option(raw).getOrElse("").trim match
      case "" => "the position"
      case text => text

  private def renderTargetRef(target: TargetRef): String =
    target match
      case TargetSquare(key) => key
      case TargetFile(file) => s"$file-file"
      case TargetPiece(role, square) => s"${NarrativeUtils.humanize(role)} on $square"

  private def sentenceIsNovel(candidate: String, existingText: String): Boolean =
    val candidateStem = normalizeStem(candidate)
    val existingStem = normalizeStem(existingText)
    candidateStem.nonEmpty && candidateStem != existingStem && !existingText.toLowerCase.contains(candidateStem)

  private def annotateBeat(
      beat: OutlineBeat,
      supportKindsOverride: Option[List[OutlineBeatKind]] = None
  ): OutlineBeat =
    beat.copy(
      branchScoped = LineScopedCitation.hasInlineCitation(beat.text) && !LineScopedCitation.hasSourceLabelOnly(beat.text),
      supportKinds = supportKindsOverride.getOrElse(beat.supportKinds)
    )

  private def ensureSentence(raw: String): String =
    val clean = Option(raw).getOrElse("").trim
    if clean.isEmpty then ""
    else if ".!?".contains(clean.last) then clean
    else s"$clean."

  private def lowerCaseFirst(raw: String): String =
    val clean = Option(raw).getOrElse("").trim
    if clean.isEmpty then ""
    else s"${clean.head.toLower}${clean.drop(1)}"

  /**
   * Build evidence beat from authorEvidence OR fallback to engineEvidence.
   * This ensures we always show a)/b) alternatives when engine data is available.
   */
  private def buildEvidenceBeat(
    ctx: NarrativeContext,
    questions: List[AuthorQuestion],
    rankedPlans: RankedQuestionPlans,
    rec: TraceRecorder
  ): Option[OutlineBeat] =
    rankedPlans.primary.flatMap(_.evidence).map { evidence =>
      rec.use("planner.evidence", evidence.purposes.mkString(","), "Evidence from planner")
      OutlineBeat(
        kind = OutlineBeatKind.Evidence,
        text = evidence.text,
        questionIds = rankedPlans.primary.toList.map(_.questionId),
        questionKinds = rankedPlans.primary.toList.map(_.questionKind),
        evidencePurposes = evidence.purposes,
        expectedEvidencePurposes = evidence.purposes,
        evidenceSourceIds = evidence.sourceIds,
        branchScoped = evidence.branchScoped
      )
    }.orElse {
      if questions.nonEmpty then None
      else
    // Primary: use authorEvidence if available
        val relevantEvidence = ctx.authorEvidence.filter { ev =>
          questions.exists(_.id == ev.questionId)
        }

        if relevantEvidence.nonEmpty then
          val branches = dedupeEvidenceBranches(relevantEvidence.flatMap(_.branches)).take(3)
          val renderedBranches =
            branches.flatMap { b =>
              LineScopedCitation.evidenceBranchDisplayLine(b).map(line => b -> line)
            }
          if renderedBranches.size >= 2 then
            val labels = List("a)", "b)", "c)")
            val formatted = renderedBranches.zip(labels).map { case ((b, line), label) =>
              val evalPart = b.evalCp.map(cp => s" (${formatCp(cp)})").getOrElse("")
              s"$label $line$evalPart"
            }
            val purposes = relevantEvidence.map(_.purpose).distinct
            val qKinds = questions.filter(q => relevantEvidence.exists(_.questionId == q.id)).map(_.kind).distinct

            rec.use("authorEvidence", purposes.mkString(","), "Evidence from authorEvidence")
            Some(OutlineBeat(
              kind = OutlineBeatKind.Evidence,
              text = formatted.mkString("\n"),
              questionIds = relevantEvidence.map(_.questionId).distinct,
              questionKinds = qKinds,
              evidencePurposes = purposes,
              expectedEvidencePurposes =
                questions
                  .filter(q => relevantEvidence.exists(_.questionId == q.id))
                  .flatMap(_.evidencePurposes)
                  .distinct
            ))
          else None
        else
          // Fallback: use engineEvidence variations
          ctx.engineEvidence.flatMap { ev =>
            val variations =
              sortVariationsForSideToMove(ctx.fen, ev.variations)
                .flatMap(v => LineScopedCitation.strategicCitation(ctx.fen, ctx.ply + 1, v).map(citation => v -> citation))
                .take(3)
            if variations.size >= 2 then
              val labels = List("a)", "b)", "c)")
              val formatted = variations.zip(labels).map { case ((v, citation), label) =>
                s"$label $citation (${formatCp(v.scoreCp)})"
              }
              rec.use("engineEvidence.variations", variations.size.toString, "Evidence fallback from engine PV")
              Some(OutlineBeat(
                kind = OutlineBeatKind.Evidence,
                text = formatted.mkString("\n"),
                evidencePurposes = List("engine_alternatives"),
                expectedEvidencePurposes = questions.flatMap(_.evidencePurposes).distinct
              ))
            else None
          }
    }

  /**
   * Build teaching beat with lower threshold (cpLoss > 20 instead of > 50).
   * Also adds fallback for counterfactual without motifs.
   */
  private def buildTeachingBeat(
    ctx: NarrativeContext,
    rankedPlans: RankedQuestionPlans,
    rec: TraceRecorder
  ): Option[OutlineBeat] =
    rankedPlans.primary.flatMap(_.consequence).filter(_.beat == QuestionPlanConsequenceBeat.TeachingPoint).map { consequence =>
      OutlineBeat(
        kind = OutlineBeatKind.TeachingPoint,
        text = consequence.text,
        conceptIds = List("planner_consequence"),
        focusPriority = 82
      )
    }.orElse {
      ctx.counterfactual.flatMap { cf =>
        val motifOpt =
          cf.missedMotifs
            .filter(_.category == MotifCategory.Tactical)
            .sortBy(_.plyIndex)
            .headOption
            .orElse {
              cf.missedMotifs
                .filterNot(_.category == MotifCategory.King)
                .sortBy(_.plyIndex)
                .headOption
            }

        val hasTacticalTheme = motifOpt.exists(_.category == MotifCategory.Tactical)
        val shouldShow = cf.cpLoss >= 50 && (hasTacticalTheme || cf.cpLoss >= 150)
        if !shouldShow then None
        else
          rec.use("counterfactual", cf.userMove, "Teaching point")
          counterfactualTeachingSentence(ctx, cf, motifOpt).map { text =>
            OutlineBeat(
              kind = OutlineBeatKind.TeachingPoint,
              text = text,
              conceptIds = List("teaching_counterfactual"),
              anchors = List(cf.bestMove)
            )
          }
      }
    }

  private def buildMainMoveBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    isAnnotation: Boolean,
    bead: Int,
    precedentTextOpt: Option[String],
    crossBeatState: CrossBeatRepetitionState
  ): OutlineBeat =
    if isAnnotation then
      val playedSan = ctx.playedSan.getOrElse("")
      val playedUci = ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
      val truthContract =
        DecisiveTruth.derive(
          ctx = ctx,
          comparisonOverride = DecisionComparisonBuilder.build(ctx)
        )
      val engineBest = rankedEngineVariations(ctx).headOption
      val engineBestUci = engineBest
        .flatMap(_.moves.headOption)
        .map(NarrativeUtils.normalizeUciMove)
        .filter(_.nonEmpty)
      val engineBestSan = engineBest
        .flatMap(_.ourMove.map(_.san))
        .filter(_.trim.nonEmpty)
        .orElse(engineBestUci.map(uci => NarrativeUtils.uciToSanOrFormat(ctx.fen, uci)).filter(_.trim.nonEmpty))
      val best = engineBestUci
        .flatMap(bestU => ctx.candidates.find(_.uci.exists(cu => NarrativeUtils.uciEquivalent(cu, bestU))))
        .orElse(ctx.candidates.headOption)
      val bestSan =
        truthContract.verifiedBestMove
          .orElse(engineBestSan)
          .orElse(best.map(_.move).filter(_.trim.nonEmpty))
          .getOrElse("")
      val bestUci =
        Option.when(truthContract.allowConcreteBenchmark || truthContract.chosenMatchesBest) {
          engineBestUci.orElse(best.flatMap(_.uci).map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty))
        }.flatten
      val playedRank =
        if truthContract.allowConcreteBenchmark then playedMoveRank(ctx, playedUci, playedSan)
        else Option.when(truthContract.chosenMatchesBest)(1)
      val cpLoss = resolveCpLoss(ctx, playedUci, playedSan, playedRank)
      val isBest = truthContract.chosenMatchesBest
      val isInvestment = truthContract.isInvestment
      val isConstructive = isBest || isInvestment

      val playedCand = playedUci.flatMap(uci => ctx.candidates.find(_.uci.exists(cu => NarrativeUtils.uciEquivalent(cu, uci))))
      val bestCand = best

      val baseText =
        if isInvestment then NarrativeLexicon.getAnnotationInvestment(bead, playedSan)
        else if isBest then NarrativeLexicon.getAnnotationPositive(bead, playedSan)
        else
          if truthContract.allowConcreteBenchmark then
            NarrativeLexicon.getAnnotationNegative(bead, playedSan, bestSan, truthContract.cpLoss)
          else
            NarrativeLexicon.getAnnotationNegativeWithoutBenchmark(bead, playedSan, truthContract.cpLoss)

      val detailText =
        if isInvestment then
          buildInvestmentAnnotationDetail(ctx, truthContract)
        else if isBest then
          playedCand.flatMap { c =>
            val b = bead ^ Math.abs(c.move.hashCode)
            val evidenceOpt = c.tacticEvidence.headOption.map(s => s.substring(0, 1).toLowerCase + s.substring(1))
            val intent = NarrativeLexicon.getIntent(b, preferredIntent(c), evidenceOpt, ply = ctx.ply)
            val isTerminal = isTerminalAnnotationMove(ctx, playedSan, bestCand)
            val hintFragments =
              annotationHintFragments(b, c.tags, c.practicalDifficulty, c.move, ctx.phase.current, isTerminal)
            val alert = c.tacticalAlert.map(_.trim).filter(_.nonEmpty).map(a => s"Note: $a.").getOrElse("")
            val alignmentNote =
              c.alignmentBand.map(_.trim.toLowerCase) match
                case Some("offplan") => "Structure note: this route is playable only with precise follow-up."
                case Some("unknown") => "Structure note: verify tactical details before committing."
                case Some("onbook") => "Structure note: this keeps the strategic structure coherent."
                case _ => ""
            val intentSentence = if intent.nonEmpty then s"It $intent." else ""
            val combined =
              (List(intentSentence) ++ hintFragments.renderedHints ++ List(alert, alignmentNote))
                .filter(_.trim.nonEmpty)
                .mkString(" ")
            Option.when(combined.nonEmpty)(combined)
          }
        else
          val b = bead ^ Math.abs(playedSan.hashCode)
          val missedMotif = None
          val whyNot = playedCand.flatMap(_.whyNot.map(_.trim).filter(_.nonEmpty))
          val alert = None
          val bestReply = None
          val bestIntent =
            if truthContract.allowConcreteBenchmark then
              bestCand.map { c =>
                val evidenceOpt = c.tacticEvidence.headOption.map(s => s.substring(0, 1).toLowerCase + s.substring(1))
                val intent = NarrativeLexicon.getIntent(b ^ Math.abs(c.move.hashCode), preferredIntent(c), evidenceOpt, ply = ctx.ply + 1)
                if intent.nonEmpty then s"Better is **$bestSan**; it $intent."
                else s"Better is **$bestSan** to keep tighter control of the position."
              }.getOrElse {
                if bestSan.nonEmpty then s"Better is **$bestSan** to keep tighter control of the position."
                else ""
              }
            else ""
          val rankContext =
            Option.when(truthContract.allowConcreteBenchmark) {
              NarrativeLexicon.getEngineRankContext(
                bead = b ^ 0x27d4eb2f,
                rank = playedRank,
                bestSan = bestSan,
                cpLoss = truthContract.cpLoss
              )
            }.flatten
          val reason = buildConcreteAnnotationIssue(
            bead = b ^ 0x6d2b79f5,
            playedSan = playedSan,
            playedUci = playedUci,
            bestSan = bestSan,
            bestUci = bestUci,
            cpLoss = truthContract.cpLoss,
            playedRank = playedRank,
            missedMotif = missedMotif,
            whyNot = whyNot,
            alert = alert,
            playedCand = playedCand,
            bestReply = bestReply,
            threatsToUs = ctx.threats.toUs,
            contextHint = Math.abs(playedSan.hashCode),
            currentPly = ctx.ply,
            allowConcreteBenchmark = truthContract.allowConcreteBenchmark
          )
          val combined = composeCausalAnnotation(
            rankContext = rankContext,
            reason = reason,
            bestIntent = bestIntent,
            bead = b ^ 0x3f84d5b5,
            tacticalEmphasis = CriticalAnnotationPolicy.shouldUseTacticalEmphasis(
              ctx = ctx,
              cpLoss = cpLoss,
              missedMotifPresent = missedMotif.nonEmpty,
              hasForcingReply = bestReply.exists(isForcingReplySan)
            ),
            usedStems = crossBeatState.usedStems.toSet,
            prefixCounts = crossBeatState.prefixCounts.toMap
            )
          Option.when(combined.nonEmpty)(combined)

      val hypothesisText =
        buildMainHypothesisNarrative(
          ctx = ctx,
          focusCandidate = playedCand.orElse(bestCand),
          supportCandidate = bestCand.filter(b => !playedCand.contains(b)).orElse(ctx.candidates.lift(1)),
          bead = bead ^ Math.abs(playedSan.hashCode) ^ 0x7f4a7c15,
          crossBeatState = crossBeatState
        ).getOrElse("")

      val deltaText = buildDeltaAfterMoveText(ctx, bead).getOrElse("")
      val alternativeSupportText =
        Option.when(CriticalAnnotationPolicy.shouldPrioritizeClaim(ctx)) {
          AlternativeNarrativeSupport.sentence(ctx).map(ensureSentence).filter(_.nonEmpty).getOrElse("")
        }.filter(_.nonEmpty).getOrElse("")
      val precedentText = precedentTextOpt.getOrElse("")
      if precedentText.nonEmpty then
        rec.use("openingData.sampleGames", "1", "Move-level precedent")
      val shouldUsePrecedentFallback =
        precedentText.isEmpty &&
          ctx.openingData.exists(_.sampleGames.isEmpty) &&
          ctx.openingEvent.exists(isCoreOpeningEvent)
      val precedentBridge =
        if !shouldUsePrecedentFallback then ""
        else buildPrecedentFallbackSentence(ctx, bead ^ 0x56f839d3, scope = "main").getOrElse("")
      val rawText = List(
        baseText,
        detailText.getOrElse(""),
        alternativeSupportText,
        hypothesisText,
        deltaText,
        precedentBridge,
        precedentText
      )
        .filter(_.trim.nonEmpty)
        .mkString(" ")
      val practicalText = buildPracticalMainMoveSentence(ctx, rawText).getOrElse("")
      val enrichedRawText =
        List(rawText, practicalText)
          .filter(_.trim.nonEmpty)
          .mkString(" ")
      val tonedText = harmonizeAnnotationTone(enrichedRawText, cpLoss, isConstructive, bead ^ Math.abs(playedSan.hashCode))
      val text = enforceAnnotationPolarity(tonedText, cpLoss, isConstructive, bead ^ Math.abs(bestSan.hashCode)).renderedText
      if text.trim.nonEmpty then trackTemplateUsage(text, crossBeatState.usedStems, crossBeatState.prefixCounts)

      OutlineBeat(
        kind = OutlineBeatKind.MainMove,
        text = text,
        anchors = List(playedSan, bestSan).filter(_.nonEmpty).distinct,
        focusPriority = 92,
        fullGameEssential = true
      )
    else
      ctx.candidates.headOption.map { main =>
        rec.use("candidates[0]", main.move, "Main move")
        val evidenceOpt = main.tacticEvidence.headOption.map(s => s.substring(0, 1).toLowerCase + s.substring(1))
        val intent = NarrativeLexicon.getIntent(bead, preferredIntent(main), evidenceOpt, ply = ctx.ply)
        val engineBest = rankedEngineVariations(ctx).headOption.orElse(ctx.engineEvidence.flatMap(_.best))
        val evalScore = engineBest.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)).getOrElse(0)
        val evalTerm = NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x85ebca6b, evalScore, ply = ctx.ply)
        val replySan = engineBest.flatMap(v => variationReplySan(ctx.fen, v))
        val consequence = engineBest.flatMap(variationConsequenceClause).getOrElse("")

        val text = NarrativeLexicon.getMainFlow(
          bead = bead,
          move = main.move,
          annotation = main.annotation,
          intent = intent,
          replySan = replySan,
          sampleRest = None,
          evalTerm = evalTerm,
          consequence = consequence
        )
        val hypothesisText =
          buildMainHypothesisNarrative(
            ctx = ctx,
            focusCandidate = Some(main),
            supportCandidate = ctx.candidates.lift(1),
            bead = bead ^ Math.abs(main.move.hashCode) ^ 0x4f6cdd1d,
            crossBeatState = crossBeatState
          ).getOrElse("")
        val prophylaxisText = buildProphylaxisMainMoveSentence(ctx, bead)
        val precedentText = precedentTextOpt
        precedentText.foreach(_ => rec.use("openingData.sampleGames", "1", "Move-level precedent"))
        val shouldUsePrecedentFallback =
          precedentText.isEmpty &&
            ctx.openingData.exists(_.sampleGames.isEmpty) &&
            ctx.openingEvent.exists(isCoreOpeningEvent)
        val precedentBridge =
          if !shouldUsePrecedentFallback then ""
          else buildPrecedentFallbackSentence(ctx, bead ^ 0x4f6cdd1d, scope = "main").getOrElse("")
        val mergedText =
          List(
            text,
            hypothesisText,
            prophylaxisText.getOrElse(""),
            buildPracticalMainMoveSentence(ctx, text).getOrElse(""),
            precedentBridge,
            precedentText.getOrElse("")
          )
            .filter(_.trim.nonEmpty)
            .mkString(" ")
        if mergedText.trim.nonEmpty then trackTemplateUsage(mergedText, crossBeatState.usedStems, crossBeatState.prefixCounts)

        OutlineBeat(
          kind = OutlineBeatKind.MainMove, 
          text = mergedText, 
          anchors = List(main.move),
          focusPriority = 92,
          fullGameEssential = true
        )
      }.getOrElse(OutlineBeat(OutlineBeatKind.MainMove, ""))

  private def buildOpeningTheoryBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    suppressPrecedents: Boolean
  ): Option[OutlineBeat] =
    val openingRef = ctx.openingData
    val openingText = openingRef.filter(_.totalGames >= 5).flatMap { ref =>
      ref.name.map { name =>
        rec.use("openingData", name, "Opening theory")
        val bead = Math.abs(ctx.hashCode)
        NarrativeLexicon.getOpeningReference(bead, name, ref.totalGames, 0.5)
      }
    }
    val precedentFragments =
      if suppressPrecedents then OpeningPrecedentFragments(Nil)
      else buildOpeningPrecedentSnippets(ctx, openingRef, Math.abs(ctx.hashCode) ^ 0x4b1d0f6a)
    if precedentFragments.nonEmpty then
      rec.use("openingData.sampleGames", precedentFragments.fragmentCount.toString, "Opening precedents")
    val branchSummary =
      Option.when(!suppressPrecedents) {
        OpeningPrecedentBranching.summarySentence(ctx, openingRef, requireFocus = false)
      }.flatten
    branchSummary.foreach(_ => rec.use("openingData.sampleGames", "branch", "Opening branch summary"))
    val branchRelation =
      Option.when(!suppressPrecedents) {
        OpeningPrecedentBranching.relationSentence(ctx, openingRef, requireFocus = false)
      }.flatten
    branchRelation.foreach(_ => rec.use("openingData.sampleGames", "relation", "Opening branch relation"))

    val precedentBridge =
      if precedentFragments.nonEmpty || openingText.isEmpty || !openingRef.exists(_.sampleGames.isEmpty) then ""
      else buildPrecedentFallbackSentence(ctx, Math.abs(ctx.hashCode) ^ 0x19f8b4ad, scope = "opening").getOrElse("")
    val text = List(
      openingText.getOrElse(""),
      branchSummary.getOrElse(""),
      branchRelation.getOrElse(""),
      precedentBridge,
      precedentFragments.renderedText
    )
      .filter(_.trim.nonEmpty)
      .mkString(" ")
      .trim
    if text.isEmpty then None
    else
      val anchors = openingRef.flatMap(_.name).map(_.split(" ").take(2).toList).getOrElse(Nil)
      val concepts =
        if precedentFragments.nonEmpty then List("opening_theory", "opening_precedent")
        else List("opening_theory")
      Some(OutlineBeat(
        kind = OutlineBeatKind.OpeningTheory,
        text = text,
        conceptIds = concepts,
        anchors = anchors,
        focusPriority = 82
      ))

  private def buildPrecedentFallbackSentence(
    ctx: NarrativeContext,
    bead: Int,
    scope: String
  ): Option[String] =
    val planHint =
      topStrategicPlanName(ctx)
        .map(_.replaceAll("""[_\-]+""", " ").trim.toLowerCase)
        .filter(_.nonEmpty)
    val evalCp = rankedEngineVariations(ctx).headOption.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp))
    val evalHint = evalCp.map(cp => NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x7f4a7c15, cp, ply = ctx.ply))
    val templates =
      scope.trim.toLowerCase match
        case "opening" =>
          List(
            "At this branch, practical handling matters more than memorized reference games.",
            "This node is best treated as a live practical decision rather than a model-game recall test.",
            "From here, over-the-board plan execution matters more than historical comparison."
          )
        case _ =>
          val planAware = planHint.map(p => s"around $p").getOrElse("from this structure")
          List(
            s"In practical terms, the key is to keep plans coherent $planAware.",
            s"The position is decided more by accurate follow-up than by historical templates $planAware.",
            s"From this point, practical move-order discipline is the main guide $planAware."
          )
    val rendered = NarrativeLexicon.pick(bead ^ 0x2f6e2b1, templates)
    val withEval =
      evalHint match
        case Some(eval) if scope.trim.equalsIgnoreCase("opening") => s"$rendered $eval."
        case _                                                    => rendered
    Option.when(withEval.trim.nonEmpty)(withEval.trim)

  private def buildOpeningPrecedentSnippets(
    ctx: NarrativeContext,
    openingRef: Option[OpeningReference],
    bead: Int
  ): OpeningPrecedentFragments =
    if !ctx.openingEvent.exists(isCoreOpeningEvent) then OpeningPrecedentFragments(Nil)
    else
      val lines = rankedOpeningPrecedentLines(ctx, openingRef, requireFocus = false).take(MaxOpeningPrecedents)
      if lines.isEmpty then OpeningPrecedentFragments(Nil)
      else if shouldUsePrecedentComparison(ctx, lines, requireFocus = false) then
        val comparison = buildPrecedentComparisonFragments(lines, bead)
        OpeningPrecedentFragments(
          body = List(comparison.header) ++ comparison.items,
          summary = comparison.summary,
          sharedLesson = comparison.sharedLesson
        )
      else
        OpeningPrecedentFragments(
          body =
            lines
              .map(line =>
                supportFragment(
                  text = renderPrecedentBlock(line, bead),
                  evidenceBacked = true
                )
              )
              .filter(_.hasRawText)
        )

  private def buildContextPrecedentSentence(ctx: NarrativeContext, bead: Int): Option[String] =
    val introOnly = ctx.openingEvent.exists {
      case OpeningEvent.Intro(_, _, _, _) => true
      case _                              => false
    }
    if introOnly then None
    else
      val lines = rankedOpeningPrecedentLines(ctx, ctx.openingData, requireFocus = true).take(MaxOpeningPrecedents)
      if lines.isEmpty then None
      else if shouldUsePrecedentComparison(ctx, lines, requireFocus = true) then
        Some(buildPrecedentComparisonFragments(lines, bead).renderedText)
      else
        val rep = OpeningPrecedentBranching.representativeSentence(ctx, ctx.openingData, requireFocus = true)
        val relation = OpeningPrecedentBranching.relationSentence(ctx, ctx.openingData, requireFocus = true)
        List(rep, relation).flatten match
          case Nil =>
            lines.headOption.map(line => renderPrecedentBlock(line, bead))
          case parts =>
            Some(parts.mkString(" "))

  private def shouldUsePrecedentComparison(
    ctx: NarrativeContext,
    lines: List[OpeningPrecedentLine],
    requireFocus: Boolean
  ): Boolean =
    if lines.size < 2 then false
    else
      val branchLikeEvent = ctx.openingEvent.exists {
        case OpeningEvent.BranchPoint(_, _, _) => true
        case OpeningEvent.OutOfBook(_, _, _)   => true
        case OpeningEvent.TheoryEnds(_, _)     => true
        case OpeningEvent.Novelty(_, _, _, _)  => true
        case _                                 => false
      }
      val highConfidenceSignals =
        lines
          .flatMap(buildPrecedentSignal)
          .count(_.confidence >= PrecedentConfidenceThreshold)
      branchLikeEvent || (requireFocus && highConfidenceSignals >= 2)

  private def renderPrecedentComparison(
    lines: List[OpeningPrecedentLine],
    bead: Int
  ): String =
    buildPrecedentComparisonFragments(lines, bead).renderedText

  private def buildPrecedentComparisonFragments(
    lines: List[OpeningPrecedentLine],
    bead: Int
  ): PrecedentComparisonFragments =
    val ranked = lines.take(MaxOpeningPrecedents)
    val rankedWithSignals =
      ranked.map { line =>
        line -> buildPrecedentSignal(line).filter(_.confidence >= PrecedentConfidenceThreshold)
      }
    val header = supportFragment(
      text = NarrativeLexicon.pick(bead ^ 0x57f1a235, List(
        "Comparable master branches from this split:",
        "At this branch, master games diverged in three practical directions:",
        "Reference branches from elite games at this point:"
      )),
      evidenceBacked = true
    )
    val usedStems = scala.collection.mutable.HashSet.empty[String]
    val prefixCounts = scala.collection.mutable.HashMap.empty[String, Int].withDefaultValue(0)
    val seenSequenceKeys = scala.collection.mutable.HashSet.empty[String]
    val mechanismUseCounts = scala.collection.mutable.HashMap.empty[OpeningBranchMechanism, Int].withDefaultValue(0)

    val items = rankedWithSignals.zipWithIndex.map { case ((line, signal), idx) =>
      val label = ('A' + idx).toChar
      val itemSeed = bead ^ Math.abs(line.text.hashCode) ^ ((idx + 1) * 0x9e3779b9)
      val hasRepeatedSequence = line.sequenceKey.nonEmpty && seenSequenceKeys.contains(line.sequenceKey)
      if line.sequenceKey.nonEmpty then seenSequenceKeys += line.sequenceKey
      val baseLineText =
        if hasRepeatedSequence then
          formatOpeningPrecedentRepeatedSnippet(line.game, itemSeed).getOrElse(line.text).trim
        else line.text.trim
      val role =
        idx match
          case 0 => PrecedentRole.Sequence
          case 1 => PrecedentRole.StrategicTransition
          case _ => PrecedentRole.DecisionDriver
      val roleTemplates = signal.toList.flatMap { s =>
        role match
          case PrecedentRole.Sequence =>
            (0 until 4).toList.map { step =>
              NarrativeLexicon.getPrecedentRouteLine(
                bead = itemSeed ^ (step * 0x27d4eb2f),
                triggerMove = s.triggerMove,
                replyMove = s.replyMove,
                pivotMove = s.pivotMove
              )
            }
          case PrecedentRole.StrategicTransition =>
            val mechanismOccurrence = mechanismUseCounts(s.mechanism)
            mechanismUseCounts.update(s.mechanism, mechanismOccurrence + 1)
            val mechanismLabel =
              precedentMechanismLabel(s.mechanism, itemSeed ^ 0x7f4a7c15, mechanismOccurrence)
            (0 until 4).toList.map { step =>
              NarrativeLexicon.getPrecedentStrategicTransitionLine(
                bead = itemSeed ^ (step * 0x7f4a7c15),
                mechanism = mechanismLabel
              )
            }
          case PrecedentRole.DecisionDriver =>
            val mechanismOccurrence = mechanismUseCounts(s.mechanism)
            mechanismUseCounts.update(s.mechanism, mechanismOccurrence + 1)
            val mechanismLabel =
              precedentMechanismLabel(s.mechanism, itemSeed ^ 0x6d2b79f5, mechanismOccurrence)
            (0 until 4).toList.map { step =>
              NarrativeLexicon.getPrecedentDecisionDriverLine(
                bead = itemSeed ^ (step * 0x6d2b79f5),
                mechanism = mechanismLabel
              )
            }
      }.map(_.trim).filter(_.nonEmpty).distinct
      val roleLine =
        if roleTemplates.isEmpty then ""
        else
          val selected = selectNonRepeatingTemplate(
            templates = roleTemplates,
            seed = itemSeed ^ 0x4f1bbcdc,
            usedStems = usedStems.toSet,
            prefixCounts = prefixCounts.toMap,
            prefixLimits = PrefixFamilyLimits
          )
          trackTemplateUsage(selected, usedStems, prefixCounts)
          selected

      val parts = List(baseLineText, roleLine).filter(_.nonEmpty)
      supportFragment(
        text = s"$label) ${parts.mkString(" ")}",
        evidenceBacked = true
      )
    }

    val summaryFamilies =
      buildPrecedentComparisonSummaryFamilies(
        rankedWithSignals.flatMap(_._2.map(_.mechanism))
      )
    val (summary, sharedLesson) =
      selectPrecedentComparisonSummaryFragments(
        families = summaryFamilies,
        seed = bead ^ 0x3c6ef372,
        usedStems = usedStems.toSet,
        prefixCounts = prefixCounts.toMap,
        prefixLimits = PrefixFamilyLimits
      )
    summary.foreach(fragment => trackTemplateUsage(fragment.rawText, usedStems, prefixCounts))
    sharedLesson.foreach(fragment => trackTemplateUsage(fragment.rawText, usedStems, prefixCounts))

    PrecedentComparisonFragments(
      header = header,
      items = items,
      summary = summary,
      sharedLesson = sharedLesson
    )

  private def precedentMechanismLabel(mechanism: OpeningBranchMechanism, seed: Int, occurrence: Int): String =
    val variants =
      mechanism match
        case OpeningBranchMechanism.TacticalPressure =>
          List(
            "forcing tactical pressure around king safety and move order",
            "forcing tactical pressure tied to king safety and tempo",
            "tactical pressure built on forcing move-order threats"
          )
        case OpeningBranchMechanism.ExchangeCascade =>
          List(
            "exchange timing that simplified into a cleaner structure",
            "a cascade of exchanges that clarified the structure",
            "exchange sequencing that reduced the position to a cleaner frame"
          )
        case OpeningBranchMechanism.PromotionRace =>
          List(
            "promotion threats forcing both sides into tempo-driven play",
            "promotion pressure that turned play into a tempo race",
            "promotion motifs that forced tempo-accurate decisions"
          )
        case OpeningBranchMechanism.StructuralTransformation =>
          List(
            "pawn-structure transformation that redirected long-term plans",
            "structural pawn shifts that changed long-plan priorities",
            "pawn-skeleton changes that rerouted strategic plans"
          )
        case OpeningBranchMechanism.InitiativeSwing =>
          List(
            "initiative swings created by faster piece activity",
            "initiative shifts driven by quicker piece deployment",
            "tempo swings created by faster piece coordination"
          )
    if variants.isEmpty then ""
    else
      val base = Math.floorMod(seed, variants.size)
      val idx = Math.floorMod(base + occurrence, variants.size)
      variants(idx)

  private def precedentMechanismSummaryLabel(mechanism: OpeningBranchMechanism): String =
    mechanism match
      case OpeningBranchMechanism.TacticalPressure =>
        "forcing tactical pressure around king safety"
      case OpeningBranchMechanism.ExchangeCascade =>
        "exchange timing and simplification control"
      case OpeningBranchMechanism.PromotionRace =>
        "promotion threats and tempo races"
      case OpeningBranchMechanism.StructuralTransformation =>
        "pawn-structure transformation and plan rerouting"
      case OpeningBranchMechanism.InitiativeSwing =>
        "initiative swings from piece activity"

  private def selectPrecedentComparisonSummaryFragments(
    families: PrecedentComparisonSummaryFamilies,
    seed: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    prefixLimits: Map[String, Int]
  ): (Option[AuthorityTaggedFragment], Option[AuthorityTaggedFragment]) =
    (
      selectPrecedentSummaryFragment(
        templates = families.ordinary,
        seed = seed,
        usedStems = usedStems,
        prefixCounts = prefixCounts,
        prefixLimits = prefixLimits
      ),
      selectPrecedentSummaryFragment(
        templates = families.sharedLesson,
        seed = seed ^ 0x51ed270b,
        usedStems = usedStems,
        prefixCounts = prefixCounts,
        prefixLimits = prefixLimits
      )
    )

  private def selectPrecedentSummaryFragment(
    templates: List[AuthorityTaggedFragment],
    seed: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    prefixLimits: Map[String, Int]
  ): Option[AuthorityTaggedFragment] =
    val usable = templates.filter(_.hasRawText)
    if usable.isEmpty then None
    else
      val selected = selectNonRepeatingTemplate(
        templates = usable.map(_.rawText),
        seed = seed,
        usedStems = usedStems,
        prefixCounts = prefixCounts,
        prefixLimits = prefixLimits
      )
      usable.find(_.rawText == selected)

  private def buildPrecedentComparisonSummaryFamilies(
    mechanisms: List[OpeningBranchMechanism]
  ): PrecedentComparisonSummaryFamilies =
    if mechanisms.isEmpty then PrecedentComparisonSummaryFamilies(Nil, Nil)
    else
      val grouped = mechanisms.groupBy(identity).view.mapValues(_.size).toMap
      val dominant = grouped.maxBy(_._2)._1
      val dominantLabel = precedentMechanismSummaryLabel(dominant)
      val diversity = grouped.size
      if diversity >= 2 then
        PrecedentComparisonSummaryFamilies(
          ordinary = List(
            supportFragment(
              text = s"Across these branches, results changed by which side better handled $dominantLabel.",
              evidenceBacked = true,
              generalized = true
            ),
            supportFragment(
              text = s"Common pattern: the side that managed $dominantLabel more accurately got the practical edge.",
              evidenceBacked = true,
              generalized = true
            )
          ),
          sharedLesson = List(
            AuthorityTaggedFragment(
              text = s"Shared lesson: this split is decided less by result labels and more by control of $dominantLabel.",
              authority = FragmentAuthority.unsafe_as_lesson,
              evidenceBacked = true,
              generalized = true
            )
          )
        )
      else
        PrecedentComparisonSummaryFamilies(
          ordinary = List(
            supportFragment(
              text = s"All cited branches revolve around $dominantLabel.",
              evidenceBacked = true,
              generalized = true
            ),
            supportFragment(
              text = s"The recurring practical theme across these games is $dominantLabel.",
              evidenceBacked = true,
              generalized = true
            ),
            supportFragment(
              text = s"These precedent lines point to one key driver: $dominantLabel.",
              evidenceBacked = true,
              generalized = true
            )
          ),
          sharedLesson = Nil
        )

  private def renderPrecedentBlock(
    line: OpeningPrecedentLine,
    bead: Int
  ): String =
    val anchorMove = line.game.pgn.flatMap(raw => OpeningPrecedentBranching.precedentSanMoves(Some(raw)).headOption)
    val lead = NarrativeLexicon.getPrecedentLead(
      bead = bead ^ Math.abs(line.text.hashCode),
      factualLine = line.text,
      anchorMove = anchorMove
    )
    val mechanismLine =
      buildPrecedentSignal(line)
        .filter(_.confidence >= PrecedentConfidenceThreshold)
        .map { signal =>
          NarrativeLexicon.getPrecedentMechanismLine(
            bead = bead ^ Math.abs(signal.triggerMove.hashCode) ^ Math.abs(signal.mechanism.toString.hashCode),
            triggerMove = signal.triggerMove,
            replyMove = signal.replyMove,
            pivotMove = signal.pivotMove,
            mechanism = signal.mechanism.toString
          )
        }
        .filter(_.trim.nonEmpty)
    List(lead.trim, mechanismLine.getOrElse("").trim).filter(_.nonEmpty).mkString(" ")

  private def rankedOpeningPrecedentLines(
    ctx: NarrativeContext,
    openingRef: Option[OpeningReference],
    requireFocus: Boolean
  ): List[OpeningPrecedentLine] =
    val focusMoves = openingPrecedentFocusMoves(ctx)
    openingRef.toList
      .flatMap(_.sampleGames)
      .flatMap { game =>
        formatOpeningPrecedentSnippet(game).map { text =>
          val overlap = openingPrecedentOverlap(game, focusMoves)
          val metadataScore = openingPrecedentMetadataScore(game)
          val score = openingPrecedentScore(overlap, metadataScore, requireFocus)
          OpeningPrecedentLine(
            text = text,
            score = score,
            year = game.year,
            game = game,
            overlap = overlap,
            metadataScore = metadataScore,
            sequenceKey = openingPrecedentSequenceKey(game)
          )
        }
      }
      .filter(_.score > 0)
      .sortBy(line => (-line.score, -line.year, line.text))

  private def openingPrecedentFocusMoves(ctx: NarrativeContext): Set[String] =
    val played = ctx.playedSan.toList
    val best = rankedEngineVariations(ctx).headOption.flatMap(_.ourMove.map(_.san)).toList
    val candidateMoves = ctx.candidates.take(3).map(_.move)
    val openingTopMoves = ctx.openingData.toList.flatMap(_.topMoves.take(2).map(_.san))
    (played ++ best ++ candidateMoves ++ openingTopMoves)
      .map(normalizeMoveToken)
      .filter(_.nonEmpty)
      .toSet

  private def openingPrecedentOverlap(game: ExplorerGame, focusMoves: Set[String]): Int =
    if focusMoves.isEmpty then 0
    else openingPrecedentMoveTokens(game.pgn).count(focusMoves.contains)

  private def openingPrecedentMetadataScore(game: ExplorerGame): Int =
    (if game.year > 0 then 3 else 0) +
      (if game.winner.isDefined then 2 else 0) +
      (if OpeningPrecedentBranching.normalizePlayerName(game.white.name).isDefined then 1 else 0) +
      (if OpeningPrecedentBranching.normalizePlayerName(game.black.name).isDefined then 1 else 0) +
      (if game.event.exists(_.trim.nonEmpty) then 2 else 0) +
      (if game.pgn.exists(_.trim.nonEmpty) then 3 else 0)

  private def openingPrecedentScore(overlap: Int, metadataScore: Int, requireFocus: Boolean): Int =
    val overlapBonus = overlap match
      case n if n >= 2 => 10
      case 1           => 6
      case _ if requireFocus => -8
      case _           => 0
    metadataScore + overlapBonus

  private def buildPrecedentSignal(line: OpeningPrecedentLine): Option[PrecedentSignal] =
    line.game.pgn.flatMap { raw =>
      val sanMoves = OpeningPrecedentBranching.precedentSanMoves(Some(raw))
      sanMoves.headOption.map { trigger =>
        val reply = sanMoves.lift(1)
        val pivot = sanMoves.lift(2)
        val captures = sanMoves.count(_.contains("x"))
        val checks = sanMoves.count(m => m.contains("+") || m.contains("#"))
        val promotions = sanMoves.count(_.contains("="))
        val pawnPushes = sanMoves.count(isLikelyPawnMove)
        val pieceMoves = sanMoves.count(isPieceMove)
        val forcingDensity =
          if sanMoves.nonEmpty then (captures + checks + promotions).toDouble / sanMoves.size.toDouble
          else 0.0
        val mechanismScores = Map(
          OpeningBranchMechanism.TacticalPressure ->
            (checks * 2 + captures + Option.when(forcingDensity >= 0.45)(1).getOrElse(0)),
          OpeningBranchMechanism.ExchangeCascade ->
            (captures * 2 + Option.when(captures >= 2)(2).getOrElse(0) + Option.when(pieceMoves >= 2)(1).getOrElse(0)),
          OpeningBranchMechanism.PromotionRace ->
            (promotions * 3 + Option.when(captures >= 1)(1).getOrElse(0) + Option.when(checks >= 1)(1).getOrElse(0)),
          OpeningBranchMechanism.StructuralTransformation ->
            (pawnPushes * 2 + Option.when(captures <= 1)(1).getOrElse(0) + Option.when(pieceMoves >= 1)(1).getOrElse(0)),
          OpeningBranchMechanism.InitiativeSwing ->
            (pieceMoves + Option.when(captures == 1)(1).getOrElse(0) + Option.when(checks == 0)(1).getOrElse(0))
        )
        val mechanism = OpeningPrecedentBranching.inferMechanismFromSanMoves(sanMoves)
        val sortedScores = mechanismScores.values.toList.sorted(using Ordering[Int].reverse)
        val dominance = sortedScores match
          case top :: second :: _ => ((top - second).max(0).min(3)).toDouble / 3.0
          case top :: Nil         => (top.min(3)).toDouble / 3.0
          case _                  => 0.0
        val overlapConfidence = line.overlap match
          case n if n >= 3 => 1.0
          case 2           => 0.85
          case 1           => 0.65
          case _           => 0.35
        val metadataConfidence = (line.metadataScore.toDouble / 12.0).min(1.0)
        val sequenceConfidence =
          if sanMoves.size >= 6 then 1.0
          else if sanMoves.size >= 4 then 0.75
          else 0.55
        val confidence =
          (0.40 * overlapConfidence) +
            (0.25 * metadataConfidence) +
            (0.20 * sequenceConfidence) +
            (0.15 * dominance)
        PrecedentSignal(
          triggerMove = trigger,
          replyMove = reply,
          pivotMove = pivot,
          mechanism = mechanism,
          confidence = confidence.max(0.0).min(1.0)
        )
      }
    }

  private def openingPrecedentMoveTokens(pgn: Option[String]): Set[String] =
    val results = Set("1-0", "0-1", "1/2-1/2", "*")
    pgn.toList
      .flatMap(_.split("\\s+").toList)
      .map(normalizeMoveToken)
      .filter(token => token.nonEmpty && !results.contains(token))
      .toSet

  private def isCoreOpeningEvent(event: OpeningEvent): Boolean = event match
    case OpeningEvent.BranchPoint(_, _, _) => true
    case OpeningEvent.OutOfBook(_, _, _)   => true
    case OpeningEvent.TheoryEnds(_, _)     => true
    case OpeningEvent.Novelty(_, _, _, _)  => true
    case OpeningEvent.Intro(_, _, _, _)    => false

  private def formatOpeningPrecedentSnippet(game: ExplorerGame): Option[String] =
    val whiteName = OpeningPrecedentBranching.normalizePlayerName(game.white.name)
    val blackName = OpeningPrecedentBranching.normalizePlayerName(game.black.name)
    val year = Option.when(game.year > 0)(game.year)
    val sanSnippet = game.pgn.map(_.trim).filter(_.nonEmpty).map(shortOpeningPrecedentSan)
    val winnerInfo = game.winner.flatMap { color =>
      val winner = if color == chess.White then whiteName else blackName
      winner.map { winnerName =>
        val result = if color == chess.White then "1-0" else "0-1"
        (winnerName, result)
      }
    }

    for
      white <- whiteName
      black <- blackName
      y <- year
      line <- sanSnippet
      if line.nonEmpty
      (winnerName, result) <- winnerInfo
    yield
      val eventSuffix = game.event.map(_.trim).filter(_.nonEmpty).map(ev => s", $ev").getOrElse("")
      s"In $white-$black ($y$eventSuffix), after $line, $winnerName won ($result)."

  private def formatOpeningPrecedentRepeatedSnippet(game: ExplorerGame, bead: Int): Option[String] =
    val whiteName = OpeningPrecedentBranching.normalizePlayerName(game.white.name)
    val blackName = OpeningPrecedentBranching.normalizePlayerName(game.black.name)
    val year = Option.when(game.year > 0)(game.year)
    val winnerInfo = game.winner.flatMap { color =>
      val winner = if color == chess.White then whiteName else blackName
      winner.map { winnerName =>
        val result = if color == chess.White then "1-0" else "0-1"
        (winnerName, result)
      }
    }
    for
      white <- whiteName
      black <- blackName
      y <- year
      (winnerName, result) <- winnerInfo
    yield
      val eventSuffix = game.event.map(_.trim).filter(_.nonEmpty).map(ev => s", $ev").getOrElse("")
      NarrativeLexicon.pick(bead ^ 0x5f356495, List(
        s"In $white-$black ($y$eventSuffix), a near-identical move-order still led to $winnerName winning ($result).",
        s"In $white-$black ($y$eventSuffix), a similar branch ended with $winnerName winning ($result).",
        s"In $white-$black ($y$eventSuffix), the same structural branch again produced $winnerName winning ($result)."
      ))

  private def shortOpeningPrecedentSan(line: String): String =
    val tokens = Option(line).getOrElse("").trim.split("\\s+").toList.filter(_.nonEmpty)
    val clipped = tokens.take(8).mkString(" ")
    if tokens.size > 8 then s"$clipped..." else clipped

  private def openingPrecedentSequenceKey(game: ExplorerGame): String =
    game.pgn
      .map(raw => OpeningPrecedentBranching.precedentSanMoves(Some(raw)))
      .getOrElse(Nil)
      .take(3)
      .map(normalizeMoveToken)
      .filter(_.nonEmpty)
      .mkString("|")

  private def isLikelyPawnMove(move: String): Boolean =
    Option(move).getOrElse("").trim.matches("""^[a-h](?:x[a-h])?[1-8](?:=[QRBN])?[+#]?$""")

  private def isPieceMove(move: String): Boolean =
    Option(move).getOrElse("").headOption.exists(ch => "KQRBN".contains(ch))

  private def buildAlternativesBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    bead: Int,
    crossBeatState: CrossBeatRepetitionState
  ): OutlineBeat =
    val played = ctx.playedSan.map(_.trim.toLowerCase)
    val deduped = ctx.candidates
      .drop(1)
      .foldLeft(List.empty[CandidateInfo]) { (acc, c) =>
        if acc.exists(_.move.trim.equalsIgnoreCase(c.move.trim)) then acc else acc :+ c
      }
    val alts = deduped.filterNot(c => played.contains(c.move.trim.toLowerCase)).take(2)
    if alts.isEmpty then return OutlineBeat(OutlineBeatKind.Alternatives, "")

    val ranked = rankedEngineVariations(ctx)
    val bestScore = ranked.headOption.map(_.effectiveScore)
    val bestSan = ranked.headOption
      .flatMap(_.ourMove.map(_.san))
      .orElse(
        ranked.headOption
          .flatMap(_.moves.headOption)
          .map(uci => NarrativeUtils.uciToSanOrFormat(ctx.fen, uci))
          .map(_.trim)
          .filter(_.nonEmpty)
      )
    val mainCandidate = ctx.candidates.headOption
    val signals = alts.map(c => alternativeEngineSignal(ctx, c, ranked, bestScore, bestSan))
    alts.foreach(c => rec.use(s"candidates[${c.move}]", c.move, "Alternative"))

    val attempted = (0 until 3).toList.map { pass =>
      val usedFamilies = scala.collection.mutable.HashSet.empty[String]
      val usedStems = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedStems
      val prefixCounts = scala.collection.mutable.HashMap.empty[String, Int].withDefaultValue(0)
      prefixCounts ++= crossBeatState.prefixCounts
      val usedHypothesisFamilies = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedHypothesisFamilies
      val usedHypothesisStems = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedHypothesisStems
      val usedDifferencePrefixes = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedDifferencePrefixes
      val usedDifferenceTails = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedDifferenceTails
      val passSeed = bead ^ (pass * 0x9e3779b9)
      val lines = alts.zipWithIndex.map { case (c, i) =>
        val localSeed = passSeed ^ ((i + 1) * 0x45d9f3b)
        val role = if i == 0 then "engine_primary" else "practical_secondary"
        val (line, family) = renderAlternativeDiversified(
          c = c,
          idx = i,
          bead = localSeed,
          usedFamilies = usedFamilies.toSet,
          signal = signals(i),
          usedStems = usedStems.toSet,
          prefixCounts = prefixCounts.toMap,
          role = role
        )
        usedFamilies += family
        val withDifference =
          appendAlternativeHypothesisDifference(
            baseLine = line,
            alternative = c,
            mainCandidate = mainCandidate,
            signal = signals(i),
            bead = localSeed ^ 0x6d2b79f5,
            usedStems = usedStems.toSet,
            prefixCounts = prefixCounts.toMap,
            usedHypothesisFamilies = usedHypothesisFamilies,
            usedHypothesisStems = usedHypothesisStems,
            usedDifferencePrefixes = usedDifferencePrefixes,
            usedDifferenceTails = usedDifferenceTails
          )
        trackTemplateUsage(withDifference, usedStems, prefixCounts)
        withDifference
      }
      (
        lines,
        alternativesRepetitionPenalty(lines),
        usedHypothesisFamilies.toSet,
        usedHypothesisStems.toSet,
        usedDifferencePrefixes.toSet,
        usedDifferenceTails.toSet
      )
    }
    val bestAttempt = attempted.minBy(_._2)
    val lines = bestAttempt._1
    crossBeatState.usedHypothesisFamilies ++= bestAttempt._3
    crossBeatState.usedHypothesisStems ++= bestAttempt._4
    crossBeatState.usedDifferencePrefixes ++= bestAttempt._5
    crossBeatState.usedDifferenceTails ++= bestAttempt._6
    lines.foreach(line => trackTemplateUsage(line, crossBeatState.usedStems, crossBeatState.prefixCounts))
    OutlineBeat(kind = OutlineBeatKind.Alternatives, text = lines.mkString("\n"), anchors = alts.map(_.move))

  private def buildWrapUpBeat(
    ctx: NarrativeContext,
    rankedPlans: RankedQuestionPlans,
    bead: Int
  ): Option[OutlineBeat] =
    val cpWhite = rankedEngineVariations(ctx).headOption.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)).getOrElse(0)
    val fragments = WrapUpFragments(
      planner =
        rankedPlans.primary
          .toList
          .flatMap(_.consequence)
          .filter(_.beat == QuestionPlanConsequenceBeat.WrapUp)
          .map(_.text)
          .map(text => supportFragment(text, plannerOwned = true, generalized = true)),
      practical =
        ctx.semantic
          .flatMap(_.practicalAssessment)
          .map(pa => supportFragment(buildPracticalWrapUpSentence(pa, bead, cpWhite, ctx.ply), sceneGrounded = true, generalized = true)),
      compensation =
        effectiveCompensationSignal(ctx)
          .map(signal => supportFragment(buildCompensationWrapUpSentence(signal, bead), sceneGrounded = true, generalized = true))
    )
    val text = fragments.renderedText

    if text.isEmpty then None
    else
      Some(
        OutlineBeat(
          kind = OutlineBeatKind.WrapUp,
          text = text,
          conceptIds = List("practical_assessment"),
          focusPriority = 72
        )
      )

  private def buildPracticalMainMoveSentence(
    ctx: NarrativeContext,
    existingText: String
  ): Option[String] =
    val sentence =
      ctx.semantic.flatMap(_.practicalAssessment).flatMap { pa =>
        val verdict = pa.verdict.trim.toLowerCase
        val drivers = summarizePracticalDrivers(pa.biasFactors, limit = 2)
        if verdict.isEmpty && drivers.isEmpty then None
        else if verdict.contains("conversion") then Some("The practical task is less about a new tactic than converting the edge cleanly.")
        else if verdict.contains("defen") then Some("Practically, the move also keeps the defensive task manageable.")
        else if verdict.contains("counter") then Some("Practically, the move matters because it limits the opponent's easiest counterplay.")
        else
          val verdictText = if verdict.nonEmpty then s"Practically, the key task is ${verdict.stripSuffix(".")}" else "Practically, the task is defined by the easiest plans to handle"
          val driverText =
            Option.when(drivers.nonEmpty)(s", with the workload driven by ${drivers.mkString(" and ")}")
              .getOrElse("")
          Some(s"$verdictText$driverText.")
      }.orElse {
        effectiveCompensationSignal(ctx).flatMap { signal =>
          signal.summary.map(_.trim).filter(_.nonEmpty).map { plan =>
            val vectors = summarizeCompensationVectorLabels(signal.vectors, limit = 2)
            val vectorText =
              Option.when(vectors.nonEmpty)(s", especially through ${vectors.mkString(" and ")}")
                .getOrElse("")
            s"Any compensation still has to justify itself through $plan$vectorText."
          }
        }
      }
    sentence
      .map(ensureSentence)
      .filter(sentenceIsNovel(_, existingText))

  private def buildProphylaxisMainMoveSentence(
    ctx: NarrativeContext,
    bead: Int
  ): Option[String] =
    ctx.semantic.flatMap(_.preventedPlans.headOption).flatMap { pp =>
      val target =
        pp.preventedThreatType.map(_.trim).filter(_.nonEmpty)
          .orElse(pp.breakNeutralized.map(file => s"$file-break"))
      val impact = Option.when(pp.counterplayScoreDrop >= 100)(s"blunting roughly ${pp.counterplayScoreDrop}cp of counterplay")
      val sentence =
        pp.sourceScope match
          case FactScope.Now =>
            target match
              case Some(t) =>
                Some(s"The move is prophylactic too: it cuts out $t${impact.map(i => s" while $i").getOrElse("")}.")
              case None if pp.counterplayScoreDrop >= 120 =>
                Some(s"The move is prophylactic too, stripping away roughly ${pp.counterplayScoreDrop}cp of counterplay.")
              case _ =>
                Option.when(pp.planId.trim.nonEmpty)(NarrativeLexicon.getPreventedPlanStatement(bead, pp.planId))
          case _ =>
            pp.citationLine.flatMap { citation =>
              val targetText = target.getOrElse("the opponent's easiest counterplay")
              val body =
                impact match
                  case Some(i) => s"$targetText becomes available again, $i"
                  case None    => s"$targetText becomes available again"
              LineScopedCitation.afterClause(citation, body)
            }
      sentence
    }

  private def buildPracticalWrapUpSentence(
    pa: PracticalInfo,
    bead: Int,
    cpWhite: Int,
    ply: Int
  ): String =
    val drivers = summarizePracticalDrivers(pa.biasFactors, limit = 2)
    val lowerVerdict = pa.verdict.trim.toLowerCase
    if lowerVerdict.nonEmpty && drivers.nonEmpty then
      s"Practically, the position is ${lowerVerdict.stripSuffix(".")} because ${drivers.mkString(" and ")}."
    else
      val seed = bead ^ Math.abs(pa.verdict.hashCode) ^ (cpWhite << 1)
      NarrativeLexicon.getPracticalVerdict(seed, pa.verdict, cpWhite, ply = ply)

  private def buildCompensationWrapUpSentence(
    signal: CompensationInterpretation.Signal,
    bead: Int
  ): String =
    val vectors = summarizeCompensationVectorLabels(signal.vectors, limit = 2)
    val plan = signal.summary.map(_.trim).filter(_.nonEmpty).getOrElse("practical play")
    signal.investedMaterial match
      case Some(investment) if vectors.nonEmpty =>
        s"If the ${investment}cp investment is to make sense, it still depends on $plan, driven by ${vectors.mkString(" and ")}."
      case Some(investment) =>
        s"If the ${investment}cp investment is to make sense, it still depends on $plan."
      case None =>
        NarrativeLexicon.getCompensationStatement(bead, plan, "Sufficient")

  private def summarizePracticalDrivers(
    biasFactors: List[PracticalBiasInfo],
    limit: Int
  ): List[String] =
    biasFactors
      .sortBy(bf => -Math.abs(bf.weight))
      .take(limit)
      .flatMap { bf =>
        val factor = Option(bf.factor).map(_.trim).filter(_.nonEmpty)
        val description = Option(bf.description).map(_.trim).filter(_.nonEmpty)
        factor.map { f =>
          description match
            case Some(d) if !d.equalsIgnoreCase(f) => s"${f.toLowerCase} ($d)"
            case _                                 => f.toLowerCase
        }
      }

  private def summarizeCompensationVectors(
    returnVector: Map[String, Double],
    limit: Int
  ): List[String] =
    returnVector.toList
      .sortBy { case (_, weight) => -weight }
      .take(limit)
      .flatMap { case (label, weight) =>
        Option(label).map(_.trim).filter(_.nonEmpty).map(l => s"$l (${f"$weight%.1f"})")
      }

  private def summarizeCompensationVectorLabels(
    vectors: List[String],
    limit: Int
  ): List[String] =
    vectors
      .map(_.replaceAll("\\([^)]*\\)", " "))
      .map(_.replace('_', ' '))
      .map(_.trim.toLowerCase.replaceAll("\\s+", " "))
      .filter(_.nonEmpty)
      .take(limit)

  private def effectiveCompensationSignal(
    ctx: NarrativeContext
  ): Option[CompensationInterpretation.Signal] =
    CompensationInterpretation.effectiveSemanticDecision(ctx).map(_.decision.signal)

  private def buildMainHypothesisNarrative(
    ctx: NarrativeContext,
    focusCandidate: Option[CandidateInfo],
    supportCandidate: Option[CandidateInfo],
    bead: Int,
    crossBeatState: CrossBeatRepetitionState
  ): Option[String] =
    val primary = selectHypothesis(focusCandidate, crossBeatState)
      .orElse(selectHypothesis(supportCandidate, crossBeatState))
    primary.map { first =>
      val secondary =
        selectSecondaryHypothesis(
          primary = first.card,
          from = List(focusCandidate, supportCandidate).flatten.distinct,
          state = crossBeatState
        )

      val observation = buildHypothesisObservation(ctx, focusCandidate.orElse(supportCandidate), bead ^ 0x11f17f1d)
      val hypothesis = NarrativeLexicon.getHypothesisClause(
        bead = bead ^ Math.abs(first.sourceMove.hashCode),
        claim = first.card.claim,
        confidence = first.card.confidence,
        horizon = first.card.horizon,
        axis = first.card.axis
      )
      val validation = NarrativeLexicon.getHypothesisValidationClause(
        bead = bead ^ 0x517cc1b7,
        supportSignals = first.card.supportSignals,
        conflictSignals = first.card.conflictSignals,
        confidence = first.card.confidence
      )
      val longBridge =
        if first.card.horizon == HypothesisHorizon.Long && hasLongHorizonSupportSignal(first.card.supportSignals) then
          val bridgeCandidates =
            List(0, 1, 2, 3, 4, 5).map { idx =>
              NarrativeLexicon.getLongHorizonBridgeClause(
                bead = bead ^ (0x3124bcf5 + idx * 0x9e3779b9),
                move = first.sourceMove,
                axis = first.card.axis
              )
            }.distinct
          val usedHypothesisNarrativeStems = Set(
            normalizeStem(observation),
            normalizeStem(hypothesis),
            normalizeStem(validation)
          ).filter(_.nonEmpty)
          Some(
            selectNonRepeatingTemplate(
              templates = bridgeCandidates,
              seed = bead ^ 0x19f8b4ad,
              usedStems = crossBeatState.usedStems.toSet ++ usedHypothesisNarrativeStems,
              prefixCounts = crossBeatState.prefixCounts.toMap,
              prefixLimits = PrefixFamilyLimits
            )
          )
        else None
      val practical = NarrativeLexicon.getHypothesisPracticalClause(
        bead = bead ^ 0x4f6cdd1d,
        horizon = first.card.horizon,
        axis = first.card.axis,
        move = first.sourceMove
      )
      val supportBridge = secondary.map { extra =>
        NarrativeLexicon.getSupportingHypothesisClause(
          bead = bead ^ Math.abs(extra.sourceMove.hashCode) ^ 0x2f6e2b1,
          claim = extra.card.claim,
          confidence = extra.card.confidence,
          axis = extra.card.axis
        )
      }.getOrElse("")
      val text = List(observation, hypothesis, validation, longBridge.getOrElse(""), supportBridge, practical)
        .filter(_.trim.nonEmpty)
        .mkString(" ")
      trackHypothesisStemUsage(text, crossBeatState)
      text.trim
    }

  private def appendAlternativeHypothesisDifference(
    baseLine: String,
    alternative: CandidateInfo,
    mainCandidate: Option[CandidateInfo],
    signal: AlternativeEngineSignal,
    bead: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    usedHypothesisFamilies: scala.collection.mutable.Set[String],
    usedHypothesisStems: scala.collection.mutable.Set[String],
    usedDifferencePrefixes: scala.collection.mutable.Set[String],
    usedDifferenceTails: scala.collection.mutable.Set[String]
  ): String =
    val mainHyp =
      pickHypothesisForDifference(mainCandidate, usedHypothesisFamilies, usedHypothesisStems)
    val altHyp =
      pickHypothesisForDifference(Some(alternative), usedHypothesisFamilies, usedHypothesisStems)
    val altClaim =
      altHyp
        .map(_.claim)
        .map(_.trim)
        .filter(_.nonEmpty)
        .filterNot { claim =>
          val stem = normalizeMoveNeutralClaimStem(claim)
          stem.nonEmpty && usedHypothesisStems.contains(stem)
        }
    val differenceVariants = NarrativeLexicon.getAlternativeHypothesisDifferenceVariants(
      bead = bead,
      alternativeMove = alternative.move,
      mainMove = mainCandidate.map(_.move).getOrElse(signal.bestSan.getOrElse("the principal move")),
      mainAxis = mainHyp.map(_.axis),
      alternativeAxis = altHyp.map(_.axis),
      alternativeClaim = altClaim,
      confidence = altHyp.map(_.confidence).getOrElse(0.42),
      horizon = altHyp.map(_.horizon).orElse(mainHyp.map(_.horizon)).getOrElse(HypothesisHorizon.Medium)
    )
    val difference = selectDifferenceVariant(
      variants = differenceVariants,
      seed = bead ^ 0x2f6e2b1,
      usedStems = usedStems ++ usedHypothesisStems.toSet,
      prefixCounts = prefixCounts,
      usedDifferencePrefixes = usedDifferencePrefixes,
      usedDifferenceTails = usedDifferenceTails
    )
    val rendered = List(baseLine.trim, difference.trim).filter(_.nonEmpty).mkString(" ").trim
    val normalized = normalizeAlternativeTemplateLine(rendered, bead ^ 0x63d5a6f1)
    (mainHyp.toList ++ altHyp.toList).foreach { h =>
      usedHypothesisFamilies += hypothesisFamily(h)
      val stem = normalizeMoveNeutralClaimStem(h.claim)
      if stem.nonEmpty then usedHypothesisStems += stem
    }
    selectNonRepeatingTemplate(
      templates = List(normalized),
      seed = bead ^ 0x19f8b4ad,
      usedStems = usedStems ++ usedHypothesisStems.toSet,
      prefixCounts = prefixCounts,
      prefixLimits = PrefixFamilyLimits
    )

  private def selectHypothesis(
    candidate: Option[CandidateInfo],
    state: CrossBeatRepetitionState
  ): Option[SelectedHypothesis] =
    candidate.flatMap { c =>
      val sorted = c.hypotheses.sortBy(h => -h.confidence)
      val picked = sorted.find { h =>
        val family = hypothesisFamily(h)
        val stem = normalizeMoveNeutralClaimStem(h.claim)
        !state.usedHypothesisFamilies.contains(family) &&
        (stem.isEmpty || !state.usedHypothesisStems.contains(stem))
      }.orElse(sorted.headOption)
      picked.map { h =>
        state.usedHypothesisFamilies += hypothesisFamily(h)
        val stem = normalizeMoveNeutralClaimStem(h.claim)
        if stem.nonEmpty then state.usedHypothesisStems += stem
        SelectedHypothesis(card = h, sourceMove = c.move)
      }
    }

  private def selectSecondaryHypothesis(
    primary: HypothesisCard,
    from: List[CandidateInfo],
    state: CrossBeatRepetitionState
  ): Option[SelectedHypothesis] =
    val pool =
      from.flatMap { c =>
        c.hypotheses.map(h => SelectedHypothesis(card = h, sourceMove = c.move))
      }
    val filtered =
      pool.filter { sh =>
        sh.card.axis != primary.axis &&
        sh.card.claim != primary.claim &&
        !state.usedHypothesisFamilies.contains(hypothesisFamily(sh.card)) &&
        !state.usedHypothesisStems.contains(normalizeMoveNeutralClaimStem(sh.card.claim))
      }
    val picked = filtered.sortBy(sh => -sh.card.confidence).headOption
      .orElse(pool.find(sh => sh.card.axis != primary.axis && sh.card.claim != primary.claim))
    picked.foreach { sh =>
      state.usedHypothesisFamilies += hypothesisFamily(sh.card)
      val stem = normalizeMoveNeutralClaimStem(sh.card.claim)
      if stem.nonEmpty then state.usedHypothesisStems += stem
    }
    picked

  private def pickHypothesisForDifference(
    candidate: Option[CandidateInfo],
    usedFamilies: scala.collection.mutable.Set[String],
    usedStems: scala.collection.mutable.Set[String]
  ): Option[HypothesisCard] =
    candidate.flatMap { c =>
      val sorted = c.hypotheses.sortBy(h => -h.confidence)
      val picked = sorted.find { h =>
        val stem = normalizeMoveNeutralClaimStem(h.claim)
        !usedFamilies.contains(hypothesisFamily(h)) &&
        (stem.isEmpty || !usedStems.contains(stem))
      }.orElse(sorted.headOption)
      picked.foreach { h =>
        usedFamilies += hypothesisFamily(h)
        val stem = normalizeMoveNeutralClaimStem(h.claim)
        if stem.nonEmpty then usedStems += stem
      }
      picked
    }

  private def buildHypothesisObservation(
    ctx: NarrativeContext,
    candidate: Option[CandidateInfo],
    bead: Int
  ): String =
    val rawObservation =
      candidate.flatMap(_.tacticalAlert.map(_.trim).filter(_.nonEmpty))
        .orElse(
          ctx.threats.toUs.headOption.map { t =>
            val sq = t.square.map(s => s" on $s").getOrElse("")
            s"${t.kind.toLowerCase} pressure$sq remains unresolved"
          }
        )
        .orElse(candidate.flatMap(_.whyNot.map(_.trim).filter(_.nonEmpty)))
        .orElse(
          candidate.map { c =>
            val move = c.move
            NarrativeLexicon.pick(bead ^ Math.abs(move.hashCode), List(
              s"$move reshapes the practical balance",
              s"$move redirects the strategic route",
              s"$move shifts which plan branch is simplest to handle",
              s"$move alters the strategic map for both sides",
              s"$move shifts the practical focus of the position",
              s"The move $move introduces a new strategic branch"
            ))
          }
        )
        .getOrElse("the position still has competing strategic routes")
    NarrativeLexicon.getHypothesisObservationClause(bead, rawObservation)

  private def trackHypothesisStemUsage(text: String, state: CrossBeatRepetitionState): Unit =
    val stem = normalizeHypothesisStem(text)
    if stem.nonEmpty then state.usedHypothesisStems += stem

  private def hasLongHorizonSupportSignal(signals: List[String]): Boolean =
    signals.exists(_.toLowerCase.contains("long-horizon"))

  private def normalizeHypothesisStem(text: String): String =
    Option(text).getOrElse("")
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(5)
      .mkString(" ")

  private val leadingMoveTokenRegex =
    """(?i)^(?:\*\*)?(?:O-O-O|O-O|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?|[a-h][1-8](?:=[QRBN])?[+#]?)(?:\*\*)?[,:]?\s+""".r

  private def normalizeMoveNeutralClaimStem(text: String): String =
    val stripped = leadingMoveTokenRegex.replaceFirstIn(Option(text).getOrElse("").trim, "")
    normalizeHypothesisStem(stripped)

  private def hypothesisFamily(card: HypothesisCard): String =
    s"${card.axis.toString.toLowerCase}:${normalizeHypothesisStem(card.claim)}"

  private def selectDifferenceVariant(
    variants: List[String],
    seed: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    usedDifferencePrefixes: scala.collection.mutable.Set[String],
    usedDifferenceTails: scala.collection.mutable.Set[String]
  ): String =
    val clean = variants.map(_.trim).filter(_.nonEmpty).distinct
    if clean.isEmpty then ""
    else
      val start = Math.floorMod(seed, clean.size)
      val rotated = (0 until clean.size).toList.map(i => clean(Math.floorMod(start + i, clean.size)))
      val unseenPrefixPool =
        rotated.filter { v =>
          val prefix = normalizeDifferencePrefix(v)
          prefix.nonEmpty && !usedDifferencePrefixes.contains(prefix)
        }
      val unseenTailPool =
        rotated.filter { v =>
          val tail = normalizeDifferenceTail(v)
          tail.nonEmpty && !usedDifferenceTails.contains(tail)
        }
      val unseenBothPool =
        rotated.filter { v =>
          val prefix = normalizeDifferencePrefix(v)
          val tail = normalizeDifferenceTail(v)
          prefix.nonEmpty && tail.nonEmpty &&
            !usedDifferencePrefixes.contains(prefix) &&
            !usedDifferenceTails.contains(tail)
        }
      val pool =
        if unseenBothPool.nonEmpty then unseenBothPool
        else if unseenPrefixPool.nonEmpty then unseenPrefixPool
        else if unseenTailPool.nonEmpty then unseenTailPool
        else rotated
      val selected =
        selectNonRepeatingTemplate(
          templates = pool,
          seed = seed ^ 0x63d5a6f1,
          usedStems = usedStems,
          prefixCounts = prefixCounts,
          prefixLimits = PrefixFamilyLimits
        )
      val prefix = normalizeDifferencePrefix(selected)
      if prefix.nonEmpty then usedDifferencePrefixes += prefix
      val tail = normalizeDifferenceTail(selected)
      if tail.nonEmpty then usedDifferenceTails += tail
      selected

  private def normalizeDifferencePrefix(text: String): String =
    Option(text).getOrElse("")
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(4)
      .mkString(" ")

  private def normalizeDifferenceTail(text: String): String =
    val clauses =
      Option(text).getOrElse("")
        .split("""[.!?]""")
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
    val tailRaw = clauses.lastOption.getOrElse(Option(text).getOrElse(""))
    tailRaw
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(6)
      .mkString(" ")

  private def pickKeyFact(ctx: NarrativeContext): Option[Fact] =
    ctx.facts
      .filter(_.scope == FactScope.Now)
      .filterNot {
        case _: Fact.TargetPiece    => true
        case _: Fact.DoubleCheck    => true
        case _: Fact.ActivatesPiece => true
        case _                      => false
      }
      .sortBy {
        case _: Fact.HangingPiece  => 0
        case _: Fact.Pin           => 1
        case _: Fact.Fork          => 2
        case _: Fact.Skewer        => 3
        case _: Fact.PawnPromotion => 4
        case _: Fact.WeakSquare    => 5
        case _: Fact.Outpost       => 6
        case _: Fact.Opposition    => 7
        case _: Fact.KingActivity  => 8
        case _                     => 99
      }
      .headOption

  private def buildDeltaAfterMoveText(ctx: NarrativeContext, bead: Int): Option[String] =
    if !ctx.deltaAfterMove then None
    else
      ctx.delta.flatMap { d =>
        val moverIsWhite = ctx.ply % 2 == 1
        val mover = if moverIsWhite then "White" else "Black"
        val moverCp = if moverIsWhite then d.evalChange else -d.evalChange

        val b = bead ^ Math.abs(mover.hashCode) ^ Math.abs(d.hashCode)
        val evalPart = NarrativeLexicon.getEvalSwingAfterMoveStatement(b, mover, moverCp)

        val phasePart =
          d.phaseChange.flatMap { s =>
            val raw = s.stripPrefix("Transition from ").trim
            raw.split(" to ", 2).toList match
              case from :: to :: Nil if from.nonEmpty && to.nonEmpty =>
                Some(NarrativeLexicon.getPhaseTransitionStatement(b ^ 0x1f1f1f, from, to))
              case _ => None
          }

        val highlightPart: Option[String] =
          phasePart.orElse {
            d.openFileCreated.map(f => NarrativeLexicon.getOpenFileCreatedStatement(b ^ 0x2f2f2f, f))
          }.orElse {
            d.structureChange.map(sc => NarrativeLexicon.getStructureChangeStatement(b ^ 0x3f3f3f, sc))
          }.orElse {
            d.newMotifs.headOption.map(m => NarrativeLexicon.getMotifAppearsStatement(b ^ 0x4f4f4f, NarrativeUtils.humanize(m)))
          }.orElse {
            d.lostMotifs.headOption.map(m => NarrativeLexicon.getMotifFadesStatement(b ^ 0x5f5f5f, NarrativeUtils.humanize(m)))
          }

        val combined = List(evalPart, highlightPart).flatten.filter(_.trim.nonEmpty).mkString(" ")
        Option.when(combined.nonEmpty)(combined)
      }

  private def annotationHintFragments(
    bead: Int,
    tags: List[CandidateTag],
    practicalDifficulty: String,
    moveHint: String,
    phase: String,
    isTerminalMove: Boolean
  ): AnnotationHintFragments =
    if isTerminalMove then
      AnnotationHintFragments(
        terminal = Some(
          supportFragment(
            text = NarrativeLexicon.getAnnotationTerminalMoveHint(bead, moveHint),
            moveLinkedAnchor = true,
            sceneGrounded = true,
            generalized = true
          )
        )
      )
    else
      AnnotationHintFragments(
        tagOnly =
          tags
            .flatMap(tag => NarrativeLexicon.getAnnotationTagOnlyHint(bead, tag, moveHint))
            .headOption
            .map(text => supportFragment(text, moveLinkedAnchor = true, sceneGrounded = true, generalized = true)),
        difficulty =
          NarrativeLexicon
            .getAnnotationDifficultyHint(bead, practicalDifficulty, moveHint, phase)
            .map(text => supportFragment(text, moveLinkedAnchor = true, sceneGrounded = true, generalized = true))
      )

  private def isTerminalAnnotationMove(
    ctx: NarrativeContext,
    playedSan: String,
    best: Option[CandidateInfo]
  ): Boolean =
    playedSan.contains("#") ||
      best.exists(_.move.contains("#")) ||
      rankedEngineVariations(ctx).headOption.flatMap(_.mate).orElse(ctx.engineEvidence.flatMap(_.best.flatMap(_.mate))).exists(m => Math.abs(m) <= 1)

  private def formatCp(cp: Int): String =
    val sign = if cp >= 0 then "+" else ""
    val pawns = cp.toDouble / 100
    f"$sign$pawns%.1f"

  private def composeCausalAnnotation(
    rankContext: Option[String],
    reason: String,
    bestIntent: String,
    bead: Int,
    tacticalEmphasis: Boolean,
    usedStems: Set[String],
    prefixCounts: Map[String, Int]
  ): String =
    val rank = rankContext.map(_.trim).filter(_.nonEmpty)
    val issue = Option(reason).map(_.trim).filter(_.nonEmpty)
    val better = Option(bestIntent).map(_.trim).filter(_.nonEmpty)

    val reasonBridge =
      Option.when(rank.nonEmpty && issue.nonEmpty) {
        selectNonRepeatingTemplate(
          templates =
            if tacticalEmphasis then
              List(
                "The tactical verdict is immediate:",
                "The tactical problem is clear:",
                "The tactical punishment is straightforward:",
                "The tactical point is concrete:",
                "The tactical flaw is immediate:"
              )
            else
              List(
                "From a practical perspective,",
                "In strategic terms,",
                "That makes the practical picture clear:",
                "So the practical verdict is straightforward:",
                "Viewed through a practical lens,",
                "The practical takeaway is immediate:"
              ),
          seed = bead ^ 0x24d8f59c,
          usedStems = usedStems ++ rank.toSet.map(normalizeStem),
          prefixCounts = prefixCounts,
          prefixLimits = PrefixFamilyLimits
        )
      }.filter(_.nonEmpty)

    val betterBridge =
      Option.when(issue.nonEmpty && better.nonEmpty) {
        selectNonRepeatingTemplate(
          templates = List(
            "Therefore,",
            "As a result,",
            "So,",
            "For that reason,",
            "Consequently,",
            "Accordingly,"
          ),
          seed = bead ^ 0x3b5296f1,
          usedStems = usedStems ++ issue.toSet.map(normalizeStem),
          prefixCounts = prefixCounts,
          prefixLimits = PrefixFamilyLimits
        )
      }.filter(_.nonEmpty)

    val reasonClause =
      issue.map { r =>
        reasonBridge.map(b => s"$b $r").getOrElse(r)
      }
    val betterClause =
      better.map { b =>
        betterBridge.map(conn => s"$conn $b").getOrElse(b)
      }

    List(rank.getOrElse(""), reasonClause.getOrElse(""), betterClause.getOrElse(""))
      .filter(_.trim.nonEmpty)
      .mkString(" ")

  private def buildConcreteAnnotationIssue(
    bead: Int,
    playedSan: String,
    playedUci: Option[String],
    bestSan: String,
    bestUci: Option[String],
    cpLoss: Int,
    playedRank: Option[Int],
    missedMotif: Option[String],
    whyNot: Option[String],
    alert: Option[String],
    playedCand: Option[CandidateInfo],
    bestReply: Option[String],
    threatsToUs: List[ThreatRow],
    contextHint: Int,
    currentPly: Int,
    allowConcreteBenchmark: Boolean = true
  ): String =
    val severity = Thresholds.classifySeverity(cpLoss)
    val tacticalIssue =
      Option.when(cpLoss >= Thresholds.MISTAKE_CP) {
        (missedMotif.map(_.trim).filter(_.nonEmpty), bestReply.filter(isForcingReplySan)) match
          case (Some(motif), Some(reply)) if severity == "blunder" =>
            Some(s"Issue: this is a tactical blunder; it misses the idea of $motif and allows ...$reply by force.")
          case (Some(motif), Some(reply)) =>
            Some(s"Issue: this misses the tactical idea of $motif and allows ...$reply.")
          case (Some(motif), _) if severity == "blunder" =>
            Some(s"Issue: this is a tactical blunder; it misses the idea of $motif.")
          case (Some(motif), _) =>
            Some(s"Issue: this misses the tactical idea of $motif.")
          case (None, Some(reply)) if severity == "blunder" =>
            Some(s"Issue: this is a tactical blunder; after this, ...$reply forces the issue.")
          case (None, Some(reply)) =>
            Some(s"Issue: after this, ...$reply gives the opponent a forcing tactical reply.")
          case _ => None
      }.flatten
    val threatIssue = unresolvedThreatIssue(threatsToUs, playedSan, playedUci, bestSan, bestUci)
    val factIssue = playedCand.flatMap(c => extractFactConsequence(c, currentPly)).map(s => s"Issue: $s")
    val alertIssue = alert.map(a => s"Issue: ${a.stripSuffix(".")}.")
    val whyNotIssue = whyNot.flatMap(humanizeWhyNot).map(r => s"Issue: $r.")
    val motifIssue =
      Option.when(cpLoss >= Thresholds.INACCURACY_CP) {
        missedMotif.map(m => s"Issue: this bypasses the tactical idea of $m.")
      }.flatten
    val hasConcreteEvidence = List(tacticalIssue, threatIssue, factIssue, alertIssue, whyNotIssue, motifIssue).flatten.nonEmpty
    val replyIssue =
      Option.when(cpLoss >= Thresholds.INACCURACY_CP && hasConcreteEvidence && tacticalIssue.isEmpty) {
        bestReply.filter(isForcingReplySan).map(r => s"Issue: after this, ...$r gives the opponent a forcing reply.")
      }.flatten
    val rankIssue =
      if !allowConcreteBenchmark then None
      else
        playedRank match
          case Some(r) if r >= 3 =>
            Some(s"Issue: this is only the ${ordinal(r)} engine option. ${buildSeverityTail(bead ^ 0x3d12ab77, cpLoss, contextHint ^ r)}")
          case Some(2) if cpLoss >= Thresholds.INACCURACY_CP =>
            Some("Issue: this is second-tier compared with the engine's main continuation.")
          case None if cpLoss >= Thresholds.INACCURACY_CP =>
            Some("Issue: this move falls outside the sampled principal lines.")
          case _ => None
    val fallbackIssue = Option.when(cpLoss >= Thresholds.INACCURACY_CP)(s"Issue: ${defaultIssueBySeverity(bead, cpLoss)}.")
    val cause =
      List(tacticalIssue, threatIssue, factIssue, alertIssue, whyNotIssue, motifIssue, replyIssue, rankIssue, fallbackIssue)
        .flatten
        .find(_.trim.nonEmpty)
        .getOrElse("")
    val consequence = buildIssueConsequence(bead, cpLoss, bestReply, threatsToUs, playedCand)
    val linkedConsequence =
      consequence.map { c =>
        val bridge = selectNonRepeatingTemplate(
          templates = List(
            "Therefore,",
            "As a result,",
            "So,",
            "For that reason,",
            "Consequently,",
            "Accordingly,"
          ),
          seed = bead ^ contextHint ^ 0x5f356495,
          usedStems = Set(normalizeStem(cause)),
          prefixCounts = Map.empty,
          prefixLimits = PrefixFamilyLimits
        )
        if cause.trim.nonEmpty && bridge.nonEmpty then s"$bridge $c" else c
      }

    List(cause, linkedConsequence.getOrElse("")).filter(_.trim.nonEmpty).mkString(" ")

  private def unresolvedThreatIssue(
    threatsToUs: List[ThreatRow],
    playedSan: String,
    playedUci: Option[String],
    bestSan: String,
    bestUci: Option[String]
  ): Option[String] =
    threatsToUs
      .filter(_.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP)
      .find { t =>
        val playedHandles = defenseMatches(t.bestDefense, playedSan, playedUci)
        val bestHandles = defenseMatches(t.bestDefense, bestSan, bestUci)
        !playedHandles && (bestHandles || t.bestDefense.nonEmpty)
      }
      .map { t =>
        val kind = t.kind.toLowerCase
        val square = t.square.map(s => s" on $s").getOrElse("")
        val seed = t.hashCode ^ playedSan.hashCode
        NarrativeLexicon.pick(seed, List(
          s"Issue: this does not neutralize the $kind threat$square.",
          s"Issue: the $kind threat$square remains a concern after this move.",
          s"Issue: it leaves the $kind threat$square unresolved.",
          s"Issue: $kind pressure$square is not addressed by this continuation."
        ))
      }

  private def defenseMatches(bestDefense: Option[String], san: String, uci: Option[String]): Boolean =
    bestDefense.exists { raw =>
      val defense = normalizeMoveToken(raw)
      val sanNorm = normalizeMoveToken(san)
      val bySan = sanNorm.nonEmpty && (defense == sanNorm || defense.startsWith(sanNorm))
      val byUci = uci.exists(u => defense == normalizeMoveToken(u))
      bySan || byUci
    }

  private def normalizeMoveToken(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
      .replaceAll("""^\d+\.(?:\.\.)?\s*""", "")
      .replaceAll("""^\.{2,}\s*""", "")
      .replaceAll("""[+#?!]+$""", "")
      .replaceAll("\\s+", "")

  private def normalizeMotifKey(raw: String): String =
    Option(raw).getOrElse("").trim
      .replaceAll("([a-z])([A-Z])", "$1_$2")
      .toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")

  private def buildThemeKeywordSentence(
    motifs: List[String],
    existingText: String,
    bead: Int,
    ply: Int,
    phase: String
  ): Option[String] = {
    val existingLower = existingText.toLowerCase
    val salient = motifs
      .map(normalizeMotifKey)
      .filter(_.nonEmpty)
      .filter(m => motifPhaseCompatible(m, phase))
      .filterNot(m => existingLower.contains(m.replace("_", " ")))
      .take(2)
    if salient.isEmpty then None
    else
      val terms = salient.take(2).map(m => s"**${m.replace("_", " ")}**")
      val text = terms match
        case List(t1, t2) => s"Themes include $t1 and $t2."
        case List(t1)     => s"Key theme: $t1."
        case _            => ""

      val localSeed = bead ^ (ply * 0x3f1ab)
      val polished = NarrativeLexicon.pickWithPlyRotation(
        localSeed,
        ply,
        List(
          text,
          s"The position revolves around ${terms.mkString(" and ")}.",
          s"The main strategic fight centers on ${terms.mkString(" and ")}."
        )
      )
      Some(polished)
  }

  private def buildCanonicalMotifTermSentence(
    motifs: List[String],
    existingText: String,
    bead: Int,
    ply: Int,
    phase: String
  ): Option[String] =
    val canonicalTerms = motifs
      .map(normalizeMotifKey)
      .filter(_.nonEmpty)
      .filter(m => motifPhaseCompatible(m, phase))
      .flatMap(canonicalTermForMotif)
      .distinct
    val existingLower = existingText.toLowerCase
    val missingTerms =
      canonicalTerms.filterNot { term =>
        existingLower.contains(term.toLowerCase)
      }
    if missingTerms.isEmpty then None
    else
      // Prioritize high-specificity concept motifs over generic positional ones
      val highPriority = Set(
        "stalemate", "repeat", "zugzwang", "perpetual check", "fortress",
        "smothered mate", "underpromotion", "interference", "zwischenzug",
        "greek gift", "king hunt", "deflection"
      )
      val prioritized = missingTerms.sortBy(t => if highPriority(t.toLowerCase) then 0 else 1)
      val selected = prioritized.take(2)
      val rendered =
        selected match
          case List(one) =>
            NarrativeLexicon.pickWithPlyRotation(
              bead,
              ply,
              List(
                s"Concrete motif term: $one.",
                s"The practical keyword here is $one.",
                s"A concrete motif to track is $one."
              )
            )
          case List(one, two) =>
            NarrativeLexicon.pickWithPlyRotation(
              bead,
              ply,
              List(
                s"Concrete motif terms: $one and $two.",
                s"Practical motif keywords are $one and $two.",
                s"The relevant motif labels here are $one and $two."
              )
            )
          case _ => ""
      Option.when(rendered.trim.nonEmpty)(rendered.trim)

  private def canonicalTermForMotif(rawMotif: String): Option[String] =
    val motif = normalizeMotifKey(rawMotif)
    if motif.isEmpty then None
    else if motif.contains("passed_pawn") || motif.contains("promotion_race") then Some("passed pawn")
    else if motif.contains("pawn_storm") then Some("pawn storm")
    else if motif.contains("zwischenzug") then Some("zwischenzug")
    else if motif.contains("interference") then Some("interference")
    else if motif.contains("zugzwang") then Some("zugzwang")
    else if motif.contains("smothered_mate") || motif.contains("smothered") then Some("smothered mate")
    else if motif.contains("trapped_piece") || motif.contains("trapped") then Some("trapped")
    else if motif.contains("prophylaxis") then Some("prophylactic")
    else if motif.contains("isolated_pawn") || motif == "iqp" then Some("isolated")
    else if motif.contains("deflection") then Some("deflection")
    else if motif.contains("king_hunt") then Some("king hunt")
    else if motif.contains("battery") then Some("battery")
    else if motif.contains("bishop_pair") then Some("bishop pair")
    else if motif.contains("opposite_bishops") || motif.contains("opposite_color_bishops") then Some("opposite-colored bishops")
    else if motif.contains("simplification") then Some("simplification")
    else if motif.contains("knight_domination") || motif.contains("domination") then Some("dominate")
    else if motif.contains("novelty") then Some("novelty")
    else if motif.contains("rook_lift") then Some("rook lift")
    else if motif.contains("stalemate") then Some("stalemate")
    else if motif.contains("underpromotion") then Some("underpromotion")
    else if motif.contains("repetition") || motif.contains("repeat") then Some("repeat")
    else None

  private def buildImbalanceContrast(ctx: NarrativeContext): Option[(String, String)] =
    ctx.semantic.flatMap { semantic =>
      def formatTag(tag: PositionalTagInfo): Option[String] = tag.tagType match {
        case "BishopPairAdvantage" => Some("the Bishop pair") // Was 'BishopPair' - brittle string
        case "OpenFile" => tag.file.map(f => s"control of the $f-file")
        case "Outpost" => tag.square.map(s => s"a strong outpost on $s")
        case "PassedPawn" => tag.square.map(s => s"a passed pawn on $s")
        case "SpaceAdvantage" => Some("a space advantage")
        // Other tags that are valid positional advantages
        case "ConnectedRooks" => Some("connected rooks")
        case _ => None
      }

      val whiteTags = semantic.positionalFeatures.filter(_.color == "White")
      val blackTags = semantic.positionalFeatures.filter(_.color == "Black")

      val whiteAdv = whiteTags.flatMap(formatTag).headOption
        .orElse(semantic.structuralWeaknesses.filter(_.owner == "Black").headOption.map(w => s"pressure on Black's ${w.squareColor} squares"))

      val blackAdv = blackTags.flatMap(formatTag).headOption
        .orElse(semantic.structuralWeaknesses.filter(_.owner == "White").headOption.map(w => s"pressure on White's ${w.squareColor} squares"))

      (whiteAdv, blackAdv) match {
        case (Some(w), Some(b)) if w != b => Some((w, b)) // Only return if both exist and are distinct
        case _ => None
      }
    }

  private val EndgameTransitionPattern = raw"(.+)\((.+)\)\s*→\s*(.+)\((.+)\)".r

  private def buildEndgameContinuitySentence(info: EndgameInfo): Option[String] =
    info.transition.flatMap {
      case EndgameTransitionPattern(fromRaw, fromHintRaw, toRaw, toHintRaw) =>
        val fromLabel = humanizeEndgamePattern(fromRaw)
        val toLabel = humanizeEndgamePattern(toRaw)
        val fromTask = endgameTaskPhrase(fromHintRaw)
        val toTask = endgameTaskPhrase(toHintRaw)
        Some(
          if fromRaw.equalsIgnoreCase("none") then
            s"A new $toLabel pattern has emerged, giving the position a clearer $toTask."
          else if toRaw.equalsIgnoreCase("none") then
            s"The $fromLabel pattern has dissolved, so the earlier $fromTask no longer holds automatically."
          else
            s"The endgame geometry has shifted from $fromLabel to $toLabel, turning the position from a $fromTask into a $toTask."
        )
      case _ => None
    }.orElse {
      info.primaryPattern.flatMap { pattern =>
        Option.when(info.patternAge >= 2) {
          val duration =
            if info.patternAge >= 8 then "for several plies"
            else if info.patternAge >= 4 then "for multiple plies"
            else s"for ${info.patternAge} plies"
          val label = humanizeEndgamePattern(pattern)
          val task = endgameTaskPhrase(info.theoreticalOutcomeHint)
          s"The $label structure has held $duration, so the same $task remains in force."
        }
      }
    }

  private def humanizeEndgamePattern(raw: String): String =
    val normalized = Option(raw).getOrElse("").trim
    if normalized.isEmpty then "endgame pattern"
    else if normalized.equalsIgnoreCase("none") then "no stable endgame pattern"
    else
      normalized
        .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
        .replace('_', ' ')

  private def endgameTaskPhrase(rawHint: String): String =
    Option(rawHint).getOrElse("").trim.toLowerCase match
      case "win"     => "winning method"
      case "draw"    => "drawing setup"
      case "unclear" => "technical plan"
      case other if other.nonEmpty => s"${other} technical task"
      case _ => "technical plan"

  private def buildEndgameCausalitySentence(ctx: NarrativeContext, info: EndgameInfo): Option[String] =
    parseBoard(ctx.fen).flatMap { board =>
      info.transition.flatMap {
        case EndgameTransitionPattern(fromRaw, _, toRaw, _) =>
          val lossClause = endgamePatternLossClause(board, fromRaw)
          val gainClause =
            Option.when(!toRaw.equalsIgnoreCase("none"))(endgamePatternHoldClause(board, toRaw, transitioned = true)).flatten
          (lossClause, gainClause) match
            case (Some(loss), Some(gain)) => Some(s"$loss $gain")
            case (Some(loss), None)       => Some(loss)
            case (None, Some(gain))       => Some(gain)
            case _                        => None
        case _ => None
      }.orElse {
        info.primaryPattern.flatMap(pattern =>
          Option.when(info.patternAge >= 2)(endgamePatternHoldClause(board, pattern, transitioned = false)).flatten
        )
      }
    }

  private def endgamePatternLossClause(board: Board, rawPattern: String): Option[String] =
    Option(rawPattern).map(_.trim).filter(_.nonEmpty).flatMap {
      case pattern if pattern.equalsIgnoreCase("Lucena")          => lucenaLossClause(board)
      case pattern if pattern.equalsIgnoreCase("PhilidorDefense") => philidorLossClause(board)
      case pattern if pattern.equalsIgnoreCase("VancuraDefense")  => vancuraLossClause(board)
      case pattern if pattern.equalsIgnoreCase("WrongRookPawnWrongBishopFortress") => Some("The wrong-rook-pawn fortress has failed because the defender can no longer sit on the promotion corner against the wrong-colored bishop.")
      case pattern if pattern.equalsIgnoreCase("OutsidePasserDecoy") => Some("The outside passer decoy has failed because the remote passer no longer drags the enemy king away from the main pawn mass.")
      case pattern if pattern.equalsIgnoreCase("ConnectedPassers") => Some("The connected passers plan has failed because the pawns are no longer advancing together with king support.")
      case pattern if pattern.equalsIgnoreCase("KeySquaresOppositionBreakthrough") => Some("The key-squares breakthrough has failed because the king no longer controls the entry squares needed to escort the pawn through.")
      case pattern if pattern.equalsIgnoreCase("TriangulationZugzwang") => Some("The triangulation zugzwang has failed because the spare triangulation tempo is gone, so the side to move is no longer being squeezed.")
      case pattern if pattern.equalsIgnoreCase("BreakthroughSacrifice") => Some("The breakthrough sacrifice no longer works because the pawn wedge cannot force open a passer at the right moment.")
      case pattern if pattern.equalsIgnoreCase("Shouldering") => Some("The shouldering plan has failed because the stronger king no longer keeps the enemy king pushed off the pawn's path.")
      case pattern if pattern.equalsIgnoreCase("RetiManeuver") => Some("The Reti maneuver has failed because the king can no longer combine pursuit of the passer with support for its own pawn.")
      case pattern if pattern.equalsIgnoreCase("ShortSideDefense") => Some("The short-side defense has failed because the defender has lost the checking distance and side-room needed to harass the king.")
      case pattern if pattern.equalsIgnoreCase("OppositeColoredBishopsDraw") => Some("The opposite-colored bishops draw has failed because the defender can no longer blockade on the bishop's color complex.")
      case pattern if pattern.equalsIgnoreCase("GoodBishopRookPawnConversion") => Some("The good-bishop rook-pawn conversion has failed because the bishop and king no longer control the promotion corner together.")
      case pattern if pattern.equalsIgnoreCase("KnightBlockadeRookPawnDraw") => Some("The knight blockade draw has failed because the knight no longer controls the promotion square and blockade ring.")
      case pattern if pattern.equalsIgnoreCase("QueenVsAdvancedPawn") => Some("The queen-versus-pawn balance has failed because the defender no longer keeps the advanced pawn far enough from the king to hold theory.")
      case pattern if pattern.equalsIgnoreCase("TarraschDefenseActive") => Some("The Tarrasch defense has failed because the rook is no longer actively checking from behind the pawn.")
      case pattern if pattern.equalsIgnoreCase("PassiveRookDefense") => Some("The passive rook defense has failed because the rook can no longer sit behind the pawn and hold the file.")
      case pattern if pattern.equalsIgnoreCase("RookAndBishopVsRookDraw") => Some("The rook-and-bishop-versus-rook draw has failed because the defender has lost the safe checking or corner setup.")
      case pattern if pattern.equalsIgnoreCase("SameColoredBishopsBlockade") => Some("The same-colored bishops blockade has failed because the defender can no longer lock the pawn chain on the shared color complex.")
      case _ => None
    }

  private def endgamePatternHoldClause(board: Board, rawPattern: String, transitioned: Boolean): Option[String] =
    Option(rawPattern).map(_.trim).filter(_.nonEmpty).flatMap {
      case pattern if pattern.equalsIgnoreCase("Lucena")          => lucenaHoldClause(board, transitioned)
      case pattern if pattern.equalsIgnoreCase("PhilidorDefense") => philidorHoldClause(board, transitioned)
      case pattern if pattern.equalsIgnoreCase("VancuraDefense")  => vancuraHoldClause(board, transitioned)
      case pattern if pattern.equalsIgnoreCase("WrongRookPawnWrongBishopFortress") =>
        Some(s"${if transitioned then "The wrong-rook-pawn fortress now holds because" else "The wrong-rook-pawn fortress still holds because"} the defender remains on the promotion corner and the bishop cannot force the right-colored entry squares.")
      case pattern if pattern.equalsIgnoreCase("OutsidePasserDecoy") =>
        Some(s"${if transitioned then "The outside passer decoy now works because" else "The outside passer decoy still works because"} the remote passer is still dragging the enemy king away from the real breakthrough wing.")
      case pattern if pattern.equalsIgnoreCase("ConnectedPassers") =>
        Some(s"${if transitioned then "The connected passers plan now works because" else "The connected passers plan still works because"} the pawns advance together and the king still supports their front.")
      case pattern if pattern.equalsIgnoreCase("KeySquaresOppositionBreakthrough") =>
        Some(s"${if transitioned then "The key-squares breakthrough now works because" else "The key-squares breakthrough still works because"} the king still controls the critical entry squares in front of the pawn.")
      case pattern if pattern.equalsIgnoreCase("TriangulationZugzwang") =>
        Some(s"${if transitioned then "The triangulation zugzwang now holds because" else "The triangulation zugzwang still holds because"} one side still keeps a spare king tempo to force the move order.")
      case pattern if pattern.equalsIgnoreCase("BreakthroughSacrifice") =>
        Some(s"${if transitioned then "The breakthrough sacrifice now works because" else "The breakthrough sacrifice still works because"} the pawn wedge still creates a forced passer once the center is opened.")
      case pattern if pattern.equalsIgnoreCase("Shouldering") =>
        Some(s"${if transitioned then "The shouldering plan now works because" else "The shouldering plan still works because"} the king still keeps the opposing king shoved off the pawn's route.")
      case pattern if pattern.equalsIgnoreCase("RetiManeuver") =>
        Some(s"${if transitioned then "The Reti maneuver now works because" else "The Reti maneuver still works because"} the king can still chase the passer while staying inside its own support route.")
      case pattern if pattern.equalsIgnoreCase("ShortSideDefense") =>
        Some(s"${if transitioned then "The short-side defense now holds because" else "The short-side defense still holds because"} the defender still has checking room on the short side and lateral space for the rook.")
      case pattern if pattern.equalsIgnoreCase("OppositeColoredBishopsDraw") =>
        Some(s"${if transitioned then "The opposite-colored bishops draw now holds because" else "The opposite-colored bishops draw still holds because"} each bishop still controls different color complexes, limiting direct penetration.")
      case pattern if pattern.equalsIgnoreCase("GoodBishopRookPawnConversion") =>
        Some(s"${if transitioned then "The good-bishop rook-pawn conversion now works because" else "The good-bishop rook-pawn conversion still works because"} the bishop matches the promotion corner and the king still escorts the pawn.")
      case pattern if pattern.equalsIgnoreCase("KnightBlockadeRookPawnDraw") =>
        Some(s"${if transitioned then "The knight blockade draw now holds because" else "The knight blockade draw still holds because"} the knight still covers the promotion square and keeps the rook pawn fixed.")
      case pattern if pattern.equalsIgnoreCase("QueenVsAdvancedPawn") =>
        Some(s"${if transitioned then "The queen-versus-pawn balance now holds because" else "The queen-versus-pawn balance still holds because"} the queen side still keeps the advanced pawn contained by checking distance.")
      case pattern if pattern.equalsIgnoreCase("TarraschDefenseActive") =>
        Some(s"${if transitioned then "The Tarrasch defense now holds because" else "The Tarrasch defense still holds because"} the rook remains active behind the pawn with checking play available.")
      case pattern if pattern.equalsIgnoreCase("PassiveRookDefense") =>
        Some(s"${if transitioned then "The passive rook defense now holds because" else "The passive rook defense still holds because"} the rook still stays behind the pawn and keeps the file blocked.")
      case pattern if pattern.equalsIgnoreCase("RookAndBishopVsRookDraw") =>
        Some(s"${if transitioned then "The rook-and-bishop-versus-rook draw now holds because" else "The rook-and-bishop-versus-rook draw still holds because"} the defender still has the known safe setup against mating nets.")
      case pattern if pattern.equalsIgnoreCase("SameColoredBishopsBlockade") =>
        Some(s"${if transitioned then "The same-colored bishops blockade now holds because" else "The same-colored bishops blockade still holds because"} the defender still locks the pawns on the bishop's shared color complex.")
      case _ => None
    }

  private def lucenaLossClause(board: Board): Option[String] =
    lucenaFrame(board, strict = false).flatMap { frame =>
      val promo = promotionSquare(frame.pawn, frame.attacker)
      promo.flatMap { promotion =>
        if chebyshev(frame.attackerKing, promotion) > 1 then
          Some("Lucena has broken down because the stronger king is no longer beside the promotion square.")
        else if chebyshev(frame.defenderKing, promotion) < 2 then
          Some("Lucena has broken down because the defending king has reached the promotion-square zone.")
        else if frame.attackerRook.file == frame.pawn.file then
          Some("Lucena has broken down because the rook has fallen onto the pawn file instead of building a bridge from the side.")
        else if fileDistance(frame.attackerRook.file, frame.pawn.file) < 2 then
          Some("Lucena has broken down because the rook no longer has the lateral bridge-building distance.")
        else None
      }
    }.orElse(
      Some("Lucena has broken down because the winning side no longer keeps the promotion-square king plus bridge-building rook setup.")
    )

  private def lucenaHoldClause(board: Board, transitioned: Boolean): Option[String] =
    lucenaFrame(board, strict = true).map { frame =>
      val opener = if transitioned then "Lucena now works because" else "Lucena still works because"
      val kingCutoff =
        if promotionSquare(frame.pawn, frame.attacker).exists(promo => chebyshev(frame.defenderKing, promo) >= 2) then
          " and the defending king is still cut off from the promotion square"
        else ""
      s"$opener the stronger king stays beside the promotion square, the rook keeps bridge-building distance$kingCutoff."
    }

  private def philidorLossClause(board: Board): Option[String] =
    philidorFrame(board, strict = false).flatMap { frame =>
      if !isPhilidorBarrierHeld(frame) then
        Some("Philidor no longer holds because the defending rook has left the barrier rank.")
      else if !isPhilidorDefenderKingAhead(frame) then
        Some("Philidor no longer holds because the defending king is no longer in front of the pawn.")
      else if !isPhilidorAttackingKingBehind(frame) then
        Some("Philidor no longer holds because the stronger king has crossed the barrier.")
      else None
    }.orElse(
      Some("Philidor no longer holds because the barrier-rank defense has been lost.")
    )

  private def philidorHoldClause(board: Board, transitioned: Boolean): Option[String] =
    philidorFrame(board, strict = true).map { frame =>
      val opener = if transitioned then "Philidor now holds because" else "Philidor still holds because"
      val kingFront =
        if isPhilidorDefenderKingAhead(frame) then " and the defending king stays in front of the pawn" else ""
      s"$opener the rook still guards the barrier rank, and the stronger king has not crossed it$kingFront."
    }

  private def vancuraLossClause(board: Board): Option[String] =
    vancuraFrame(board, strict = false).flatMap { frame =>
      if relativeRank(frame.pawn, frame.attacker) != 6 || !isFlankPawn(frame.pawn) then
        Some("Vancura no longer holds because the pawn is no longer the sixth-rank rook pawn required for the side-checking setup.")
      else if frame.defenderRook.rank != frame.pawn.rank then
        Some("Vancura no longer holds because the defending rook has left the pawn's rank, so the side-checking defense is gone.")
      else if fileDistance(frame.defenderRook.file, frame.pawn.file) < 1 then
        Some("Vancura no longer holds because the defending rook has no side-checking room.")
      else if isRookBehindPawn(board, frame.attacker, frame.pawn) then
        Some("Vancura no longer holds because the attacking rook has reached a behind-the-pawn setup.")
      else None
    }.orElse {
      advancedFlankPawnFrame(board).flatMap { frame =>
        if relativeRank(frame.pawn, frame.attacker) != 6 then
          Some("Vancura no longer holds because the pawn is no longer the sixth-rank rook pawn required for the side-checking setup.")
        else if frame.defenderRook.rank != frame.pawn.rank then
          Some("Vancura no longer holds because the defending rook has left the pawn's rank, so the side-checking defense is gone.")
        else if fileDistance(frame.defenderRook.file, frame.pawn.file) < 1 then
          Some("Vancura no longer holds because the defending rook has no side-checking room.")
        else if isRookBehindPawn(board, frame.attacker, frame.pawn) then
          Some("Vancura no longer holds because the attacking rook has reached a behind-the-pawn setup.")
        else None
      }
    }.orElse(
      Some("Vancura no longer holds because the rook has lost the side-checking formation on the pawn's rank.")
    )

  private def vancuraHoldClause(board: Board, transitioned: Boolean): Option[String] =
    vancuraFrame(board, strict = true).map { _ =>
      val opener = if transitioned then "Vancura now holds because" else "Vancura still holds because"
      s"$opener the rook stays on the pawn's rank with side-checking room, and the attacking rook is not yet behind the pawn."
    }

  private def parseBoard(fen: String): Option[Board] =
    Fen.read(Standard, Fen.Full(fen)).map(_.board)

  private def lucenaFrame(board: Board, strict: Boolean): Option[RookEndgameFrame] =
    rookEndgameFrame(
      board,
      pawnFilter = (pawn, attacker) =>
        !isRookPawn(pawn) &&
          relativeRank(pawn, attacker) >= (if strict then 6 else 5) &&
          (if strict then isPassedPawn(board, pawn, attacker) else true)
    )

  private def philidorFrame(board: Board, strict: Boolean): Option[RookEndgameFrame] =
    rookEndgameFrame(
      board,
      pawnFilter = (pawn, attacker) => relativeRank(pawn, attacker) <= (if strict then 5 else 6)
    )

  private def vancuraFrame(board: Board, strict: Boolean): Option[RookEndgameFrame] =
    rookEndgameFrame(
      board,
      pawnFilter = (pawn, attacker) =>
        isFlankPawn(pawn) &&
          relativeRank(pawn, attacker) >= (if strict then 6 else 5) &&
          (if strict then isPassedPawn(board, pawn, attacker) else true)
    )

  private def rookEndgameFrame(
      board: Board,
      pawnFilter: (Square, Color) => Boolean
  ): Option[RookEndgameFrame] =
    List(Color.White, Color.Black)
      .flatMap { attacker =>
        val defender = !attacker
        if !isPureRookEndgame(board, attacker, defender) then Nil
        else
          val pawns = board.byPiece(attacker, Pawn).squares
            .filter(pawn => pawnFilter(pawn, attacker))
            .toList
            .sortBy(pawn => -relativeRank(pawn, attacker))
          val frame =
            for
              pawn <- pawns.headOption
              attackerKing <- board.kingPosOf(attacker)
              defenderKing <- board.kingPosOf(defender)
              attackerRook <- board.byPiece(attacker, Rook).squares.headOption
              defenderRook <- board.byPiece(defender, Rook).squares.headOption
            yield RookEndgameFrame(attacker, defender, pawn, attackerKing, defenderKing, attackerRook, defenderRook)
          frame.toList
      }
      .sortBy(frame => -relativeRank(frame.pawn, frame.attacker))
      .headOption

  private def advancedFlankPawnFrame(board: Board): Option[RookEndgameFrame] =
    List(Color.White, Color.Black)
      .flatMap { attacker =>
        val defender = !attacker
        val pawns = board.byPiece(attacker, Pawn).squares
          .filter(pawn => isFlankPawn(pawn) && relativeRank(pawn, attacker) >= 5)
          .toList
          .sortBy(pawn => -relativeRank(pawn, attacker))
        val frame =
          for
            pawn <- pawns.headOption
            attackerKing <- board.kingPosOf(attacker)
            defenderKing <- board.kingPosOf(defender)
            attackerRook <- board.byPiece(attacker, Rook).squares.headOption
            defenderRook <- board.byPiece(defender, Rook).squares.headOption
          yield RookEndgameFrame(attacker, defender, pawn, attackerKing, defenderKing, attackerRook, defenderRook)
        frame.toList
      }
      .sortBy(frame => -relativeRank(frame.pawn, frame.attacker))
      .headOption

  private def isPhilidorBarrierHeld(frame: RookEndgameFrame): Boolean =
    frame.defenderRook.rank == philidorBarrierRank(frame.attacker)

  private def isPhilidorDefenderKingAhead(frame: RookEndgameFrame): Boolean =
    if frame.attacker.white then frame.defenderKing.rank.value > frame.pawn.rank.value
    else frame.defenderKing.rank.value < frame.pawn.rank.value

  private def isPhilidorAttackingKingBehind(frame: RookEndgameFrame): Boolean =
    relativeRank(frame.attackerKing, frame.attacker) <= 5

  private def philidorBarrierRank(attacker: Color): Rank =
    if attacker.white then Rank.Sixth else Rank.Third

  private def isPureRookEndgame(board: Board, attacker: Color, defender: Color): Boolean =
    val matA = sideMaterial(board, attacker)
    val matD = sideMaterial(board, defender)
    matA.rooks == 1 &&
      matD.rooks == 1 &&
      matA.queens == 0 &&
      matD.queens == 0 &&
      matA.knights == 0 &&
      matA.bishops == 0 &&
      matD.knights == 0 &&
      matD.bishops == 0

  private case class SideMaterial(rooks: Int, queens: Int, bishops: Int, knights: Int)

  private def sideMaterial(board: Board, color: Color): SideMaterial =
    SideMaterial(
      rooks = board.byPiece(color, Rook).count,
      queens = board.byPiece(color, Queen).count,
      bishops = board.byPiece(color, Bishop).count,
      knights = board.byPiece(color, Knight).count
    )

  private def isPassedPawn(board: Board, pawn: Square, color: Color): Boolean =
    val enemyPawns = board.byPiece(!color, Pawn).squares
    enemyPawns.forall { enemy =>
      val fileClose = (enemy.file.value - pawn.file.value).abs <= 1
      val blocksForward =
        if color.white then enemy.rank.value >= pawn.rank.value
        else enemy.rank.value <= pawn.rank.value
      !(fileClose && blocksForward)
    }

  private def relativeRank(square: Square, color: Color): Int =
    if color.white then square.rank.value + 1 else 8 - square.rank.value

  private def isRookPawn(square: Square): Boolean =
    square.file == File.A || square.file == File.H

  private def isFlankPawn(square: Square): Boolean =
    square.file == File.A || square.file == File.B || square.file == File.G || square.file == File.H

  private def promotionSquare(pawn: Square, color: Color): Option[Square] =
    Square.at(pawn.file.value, if color.white then 7 else 0)

  private def isRookBehindPawn(board: Board, color: Color, pawn: Square): Boolean =
    board.byPiece(color, Rook).squares.exists { rook =>
      rook.file == pawn.file &&
        (if color.white then rook.rank.value < pawn.rank.value else rook.rank.value > pawn.rank.value)
    }

  private def chebyshev(a: Square, b: Square): Int =
    math.max((a.file.value - b.file.value).abs, (a.rank.value - b.rank.value).abs)

  private def fileDistance(a: File, b: File): Int =
    (a.value - b.value).abs


  private def collectDerivedContextMotifs(ctx: NarrativeContext): List[String] =
    val semantic = ctx.semantic.toList
    val positional = semantic.flatMap(_.positionalFeatures.flatMap(positionalTagMotifs))
    val weaknesses = semantic.flatMap(_.structuralWeaknesses.flatMap(weakComplexMotifs))
    val endgame = semantic.flatMap(_.endgameFeatures.toList.flatMap(endgameMotifs))
    val evidence = ctx.candidates.flatMap(_.tacticEvidence.flatMap(tacticEvidenceMotifs))
    val conceptMotifs = semantic.flatMap(_.conceptSummary).flatMap(conceptToMotif)
    (positional ++ weaknesses ++ endgame ++ evidence ++ conceptMotifs)
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct

  /** Maps conceptSummary labels to motif IDs that canonicalTermForMotif can resolve. */
  private def conceptToMotif(concept: String): Option[String] =
    val low = concept.trim.toLowerCase.replaceAll("[\\s_-]+", "_")
    if low.contains("stalemate") then Some("stalemate_trick")
    else if low.contains("repetition") || low.contains("repeat") then Some("repetition_threat")
    else if low.contains("perpetual") then Some("perpetual_check")
    else if low.contains("fortress") then Some("fortress")
    else if low.contains("zugzwang") then Some("zugzwang")
    else None

  private def endgameMotifs(info: EndgameInfo): List[String] =
    val motifs = scala.collection.mutable.ListBuffer[String]()
    // Priority: promotion race > forced draw resource > king activity.
    if info.ruleOfSquare.equalsIgnoreCase("Fails") || info.rookEndgamePattern.equalsIgnoreCase("RookBehindPassedPawn") then
      motifs += "promotion_race"
    if info.isZugzwang || info.zugzwangLikelihood >= 0.65 || info.theoreticalOutcomeHint.equalsIgnoreCase("Draw") then
      motifs += "forced_draw_resource"
    if info.hasOpposition || !info.oppositionType.equalsIgnoreCase("None") then
      motifs += "opposition"
    if info.rookEndgamePattern.equalsIgnoreCase("KingCutOff") then motifs += "king_cut_off"
    if info.kingActivityDelta > 0 then motifs += "king_activity"
    info.primaryPattern.foreach { p =>
      val low = p.toLowerCase
      if low.contains("lucena") then motifs += "lucena"
      else if low.contains("philidor") then motifs += "philidor"
      else if low.contains("vancura") then motifs += "vancura"
      else if low.contains("triangulation") then motifs += "triangulation"
      else if low.contains("outsidepasser") then motifs += "outside_passer"
      else if low.contains("connectedpassers") then motifs += "connected_passers"
      else if low.contains("oppositecoloredbishops") then motifs += "opposite_bishops"
      else if low.contains("wrongrookpawnwrongbishopfortress") then motifs += "wrong_bishop_fortress"
      else if low.contains("shortsidedefense") then motifs += "short_side_defense"
      else if low.contains("breakthroughsacrifice") then motifs += "breakthrough_sacrifice"
      else if low.contains("shouldering") then motifs += "shouldering"
      else if low.contains("retimaneuver") then motifs += "reti_maneuver"
      else if low.contains("goodbishoprookpawnconversion") then motifs += "good_bishop_rook_pawn"
      else if low.contains("knightblockaderookpawndraw") then motifs += "knight_blockade_rook_pawn"
    }
    motifs.distinct.toList

  private def positionalTagMotifs(tag: PositionalTagInfo): List[String] =
    val key = normalizeMotifKey(tag.tagType)
    val detail = normalizeMotifKey(tag.detail.getOrElse(""))
    val fileHint = tag.file.map(_.trim).filter(_.nonEmpty).getOrElse("")
    if key.contains("minority_attack") || key.contains("minorityattack") || detail.contains("minority_attack") then
      List("minority_attack")
    else if key.contains("pawn_majority") || key.contains("pawnmajority") then
      List("pawn_storm")
    else if key.contains("hanging_pawns") || key.contains("hangingpawns") then
      List("hanging_pawns")
    else if key.contains("bad_bishop") || key.contains("badbishop") then
      List("bad_bishop")
    else if key.contains("good_bishop") || key.contains("goodbishop") then
      List("good_bishop")
    else if key.contains("bishop_pair") || key.contains("bishoppair") then
      List("bishop_pair")
    else if key.contains("opposite_color_bishops") || key.contains("oppositecolorbishops") then
      List("opposite_bishops")
    else if key.contains("color_complex") || key.contains("colorcomplex") then
      List("color_complex")
    else if key.contains("semi_open_file_control") || key.contains("semiopenfilecontrol") then
      List(s"semi_open_file_control${if fileHint.nonEmpty then s"_$fileHint" else ""}")
    else if key.contains("rook_on_seventh") || key.contains("rookonseventh") || key.contains("seventh_rank_invasion") then
      List("rook_on_seventh")
    else if key.contains("rook_behind_passed_pawn") || key.contains("rookbehindpassedpawn") then
      List("rook_behind_passed_pawn")
    else if key.contains("king_cut_off") || key.contains("kingcutoff") then
      List("king_cut_off")
    else if key.contains("doubled_rooks") || key.contains("doubledrooks") then
      List("doubled_rooks")
    else if key.contains("connected_rooks") || key.contains("connectedrooks") then
      List("connected_rooks")
    else if key.contains("open_file") || key == "openfile" then
      List(s"open_file${if fileHint.nonEmpty then s"_$fileHint" else ""}")
    else if key.contains("outpost") then
      List("outpost")
    else Nil

  private def weakComplexMotifs(w: WeakComplexInfo): List[String] =
    val cause = normalizeMotifKey(w.cause)
    if cause.contains("hanging_pawns") then List("hanging_pawns")
    else Nil

  private def tacticEvidenceMotifs(raw: String): List[String] =
    val normalized = normalizeMotifKey(raw)
    val low = Option(raw).getOrElse("").toLowerCase
    if normalized.startsWith("maneuver") then List("maneuver")
    else if normalized.startsWith("domination") then List("domination")
    else if normalized.startsWith("trapped_piece") || normalized.startsWith("trappedpiece") then
      val pieceHint =
        if low.contains("queen") then List("trapped_piece_queen")
        else if low.contains("rook") then List("trapped_piece_rook")
        else Nil
      "trapped_piece" :: pieceHint
    else if normalized.startsWith("knight_vs_bishop") || normalized.startsWith("knightvsbishop") then List("knight_vs_bishop")
    else if normalized.startsWith("blockade") then List("blockade")
    else if normalized.startsWith("smothered_mate") || normalized.startsWith("smotheredmate") then List("smothered_mate")
    else if normalized.startsWith("pin") then
      if low.contains("queen") then List("pin_queen", "pin")
      else List("pin")
    else if normalized.startsWith("skewer") then
      if low.contains("queen") then List("skewer_queen", "skewer")
      else List("skewer")
    else if normalized.startsWith("xray") || normalized.startsWith("x_ray") then
      if low.contains("queen") then List("xray_queen", "xray")
      else List("xray")
    else if normalized.startsWith("battery") then List("battery")
    else if normalized.contains("exchange_sacrifice") || normalized.contains("exchangesacrifice") || normalized.contains("sacrifice_roi") then
      List("exchange_sacrifice")
    else Nil

  private def isTrustedMotifPrefixCandidate(
    rawMotif: String,
    deltaSignals: List[String],
    counterfactualSignals: List[String],
    conceptSummarySignals: List[String],
    derivedSignals: List[String],
    keyFact: Option[Fact],
    threatsToUs: List[ThreatRow],
    pawnPlay: PawnPlayTable,
    phase: String
  ): Boolean =
    val motif = normalizeMotifKey(rawMotif)
    if motif.isEmpty || !NarrativeLexicon.isMotifPrefixSignal(motif) || !motifPhaseCompatible(motif, phase) then false
    else
      val fromDelta = deltaSignals.exists(sig => motifSignalMatches(sig, motif))
      val fromCounterfactual = counterfactualSignals.exists(sig => motifSignalMatches(sig, motif))
      val fromConceptSummary = conceptSummarySignals.exists(sig => motifSignalMatches(sig, motif))
      val fromDerived = derivedSignals.exists(sig => motifSignalMatches(sig, motif))
      val corroboratedByBoard =
        keyFact.exists(f => motifCorroboratedByFact(motif, f)) ||
          motifCorroboratedByThreat(motif, threatsToUs) ||
          motifCorroboratedByPawnPlay(motif, pawnPlay)

      fromDelta || fromCounterfactual || fromDerived || (fromConceptSummary && corroboratedByBoard)

  private def motifPhaseCompatible(rawMotif: String, phase: String): Boolean =
    val motif = normalizeMotifKey(rawMotif)
    val p = Option(phase).getOrElse("").trim.toLowerCase
    if motif.isEmpty then false
    else if p.contains("endgame") then
      !List(
        "minority_attack",
        "pawn_storm",
        "greek_gift",
        "smothered_mate",
        "rook_lift",
        "novelty"
      ).exists(motif.contains)
    else if p.contains("opening") then
      !List(
        "zugzwang",
        "opposition",
        "king_cut_off",
        "rook_behind_passed_pawn",
        "lucena",
        "philidor",
        "vancura",
        "triangulation",
        "outside_passer",
        "connected_passers",
        "wrong_bishop_fortress",
        "short_side_defense",
        "breakthrough_sacrifice",
        "shouldering",
        "reti_maneuver",
        "good_bishop_rook_pawn",
        "knight_blockade_rook_pawn",
        "promotion_race",
        "forced_draw_resource"
      ).exists(motif.contains)
    else true

  private def motifSignalMatches(rawSignal: String, rawMotif: String): Boolean =
    val signal = normalizeMotifKey(rawSignal)
    val motif = normalizeMotifKey(rawMotif)
    if signal.isEmpty || motif.isEmpty then false
    else
      signal == motif ||
        signal.contains(motif) ||
        motif.contains(signal) ||
        signal.replace("_", "").contains(motif.replace("_", "")) ||
        motif.replace("_", "").contains(signal.replace("_", ""))

  private def motifCorroboratedByFact(motif: String, fact: Fact): Boolean =
    fact match
      case _: Fact.Pin =>
        motif.contains("pin") || motif.contains("xray")
      case _: Fact.Skewer =>
        motif.contains("skewer") || motif.contains("xray")
      case _: Fact.Fork =>
        motif.contains("fork") || motif.contains("deflection")
      case _: Fact.HangingPiece =>
        motif.contains("trapped_piece") || motif.contains("battery")
      case _: Fact.WeakSquare =>
        List("minority_attack", "color_complex", "bad_bishop", "good_bishop", "outpost").exists(motif.contains)
      case _: Fact.Outpost =>
        List("outpost", "knight_domination", "maneuver", "knight_vs_bishop").exists(motif.contains)
      case _: Fact.Opposition =>
        motif.contains("opposition")
      case _: Fact.KingActivity =>
        motif.contains("king_cut_off") || motif.contains("passed_pawn")
      case _ => false

  private def motifCorroboratedByThreat(motif: String, threatsToUs: List[ThreatRow]): Boolean =
    val tacticalMotif =
      List(
        "king_hunt",
        "smothered",
        "greek_gift",
        "deflection",
        "interference",
        "pin",
        "skewer",
        "xray",
        "battery",
        "zwischenzug",
        "trapped_piece",
        "exchange_sacrifice"
      ).exists(motif.contains)
    val strategicMotif =
      List(
        "open_file",
        "rook_on_seventh",
        "rook_behind_passed_pawn",
        "king_cut_off",
        "passed_pawn",
        "pawn_storm",
        "minority_attack",
        "hanging_pawns",
        "isolated_pawn",
        "iqp",
        "color_complex",
        "simplification",
        "liquidate",
        "blockade"
      ).exists(motif.contains)

    threatsToUs.exists { t =>
      val kind = t.kind.toLowerCase
      val urgent = t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP || kind.contains("mate")
      (tacticalMotif && (kind.contains("mate") || kind.contains("material") || urgent)) ||
      (strategicMotif && (kind.contains("positional") || urgent))
    }

  private def motifCorroboratedByPawnPlay(motif: String, pawnPlay: PawnPlayTable): Boolean =
    val hasPawnSignal =
      pawnPlay.breakReady ||
        pawnPlay.breakFile.nonEmpty ||
        pawnPlay.primaryDriver.toLowerCase.contains("break")
    hasPawnSignal &&
      List("pawn_break", "liquidate", "liquidation", "minority_attack", "passed_pawn", "hanging_pawns", "open_file")
        .exists(motif.contains)

  private def resolveCpLoss(
    ctx: NarrativeContext,
    playedUci: Option[String],
    playedSan: String,
    playedRank: Option[Int]
  ): Int =
    val fromCounterfactual = ctx.counterfactual.map(_.cpLoss).getOrElse(0)
    val fromEngine = estimateCpLossFromEngine(ctx, playedUci, playedSan)
    fromEngine
      .orElse {
        Option.when(fromCounterfactual > 0)(fromCounterfactual)
      }
      .orElse {
        playedRank match
          case Some(r) if r >= 3 => Some(30)
          case Some(2)           => Some(20)
          case _                 => None
      }
      .getOrElse(0)

  private def estimateCpLossFromEngine(
    ctx: NarrativeContext,
    playedUci: Option[String],
    playedSan: String
  ): Option[Int] =
    val ranked = rankedEngineVariations(ctx)
    for
      best <- ranked.headOption
      playedScore <- {
        val playedNormUci = playedUci.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
        val playedNormSan = normalizeMoveToken(playedSan)
        ranked.collectFirst {
          case v if playedNormUci.exists(u => v.moves.headOption.exists(m => NarrativeUtils.uciEquivalent(m, u))) =>
            v.effectiveScore
          case v if playedNormSan.nonEmpty && v.ourMove.exists(m => normalizeMoveToken(m.san) == playedNormSan) =>
            v.effectiveScore
        }
      }
    yield cpLossForSideToMove(ctx.fen, best.effectiveScore, playedScore)

  private def playedMoveRank(
    ctx: NarrativeContext,
    playedUci: Option[String],
    playedSan: String
  ): Option[Int] =
    val playedNormUci = playedUci.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
    val playedNormSan = normalizeMoveToken(playedSan)
    rankedEngineVariations(ctx).zipWithIndex.collectFirst {
      case (v, i) if playedNormUci.exists(u => v.moves.headOption.exists(m => NarrativeUtils.uciEquivalent(m, u))) =>
        i + 1
      case (v, i) if playedNormSan.nonEmpty && v.ourMove.exists(m => normalizeMoveToken(m.san) == playedNormSan) =>
        i + 1
    }

  private def rankedEngineVariations(ctx: NarrativeContext) =
    sortVariationsForSideToMove(ctx.fen, ctx.engineEvidence.toList.flatMap(_.variations))

  private def sortVariationsForSideToMove(fen: String, vars: List[VariationLine]): List[VariationLine] =
    if fenSideToMoveIsWhite(fen) then vars.sortBy(v => -v.effectiveScore)
    else vars.sortBy(_.effectiveScore)

  private def fenSideToMoveIsWhite(fen: String): Boolean =
    Option(fen).getOrElse("").trim.split("\\s+").drop(1).headOption.contains("w")

  private def cpLossForSideToMove(fen: String, bestScore: Int, playedScore: Int): Int =
    if fenSideToMoveIsWhite(fen) then (bestScore - playedScore).max(0)
    else (playedScore - bestScore).max(0)

  private def ordinal(n: Int): String =
    val suffix =
      if n % 100 >= 11 && n % 100 <= 13 then "th"
      else
        n % 10 match
          case 1 => "st"
          case 2 => "nd"
          case 3 => "rd"
          case _ => "th"
    s"$n$suffix"

  private def humanizeWhyNot(raw: String): Option[String] =
    val cleaned = Option(raw).getOrElse("").trim
      .replaceAll("""\(?[-+]?\d+(?:\.\d+)?\s*cp\)?""", "")
      .replaceAll("""\(\s*[-+]?\d+(?:\.\d+)?\s*\)""", "")
      .replaceAll("""\s{2,}""", " ")
      .replaceAll("""\s+\.""", ".")
      .stripSuffix(".")
      .replaceAll("(?i)\\binferior\\b", "less precise")
      .replaceAll("(?i)\\binaccuracy\\b", "practical concession")
      .replaceAll("(?i)\\bmistake\\b", "practical error")
      .trim
    Option.when(cleaned.nonEmpty)(cleaned)

  private def buildBoardAnchor(ctx: NarrativeContext, keyFact: Option[Fact], bead: Int): Option[BoardAnchor] =
    val urgentThreat =
      ctx.threats.toUs
        .sortBy(t => -t.lossIfIgnoredCp)
        .headOption
        .filter(t => t.kind.equalsIgnoreCase("Mate") || t.lossIfIgnoredCp >= 80)

    urgentThreat.map { t =>
      val kind = t.kind.trim.toLowerCase
      val square = t.square.map(_.trim).filter(_.nonEmpty).map(s => s" on $s").getOrElse("")
      val text = NarrativeLexicon.pick(bead ^ 0x4e67c6a7, List(
        s"The immediate concrete issue is the $kind threat$square.",
        s"On the board right now, handling the $kind threat$square is the priority.",
        s"The position currently hinges on the $kind threat$square.",
        s"Immediate focus centers on the $kind threat$square.",
        s"The tactical priority is clearly the $kind threat$square.",
        s"Checking the stability of the $kind situation$square is paramount."
      ))
      BoardAnchor(text = text, consumedThreat = true)
    }.orElse {
      keyFact.flatMap { fact =>
        val text = NarrativeLexicon.getFactStatement(bead ^ 0x3c79ac49, fact, ctx).trim
        Option.when(text.nonEmpty)(BoardAnchor(text = text, consumedFact = true))
      }
    }.orElse {
      ctx.pawnPlay.breakFile.map { br =>
        val file = br.trim
        val fileLabel = if file.toLowerCase.contains("file") then file else s"$file-file"
        val text = NarrativeLexicon.pick(bead ^ 0x1f123bb5, List(
          s"$fileLabel pressure is the concrete lever in the current position.",
          s"The $fileLabel dynamic is a major factor in the struggle.",
          s"Both sides are currently focused on the $fileLabel channel.",
          s"Control of the $fileLabel is the main positional prize.",
          s"The structural fight revolves around $fileLabel possibilities.",
          s"Pressure is sharpening along the $fileLabel."
        ))
        BoardAnchor(text = text)
      }
    }

  private def buildIssueConsequence(
    bead: Int,
    cpLoss: Int,
    bestReply: Option[String],
    threatsToUs: List[ThreatRow],
    playedCand: Option[CandidateInfo]
  ): Option[String] =
    val threatConsequence =
      threatsToUs.find(_.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP).map { t =>
        val kind = t.kind.toLowerCase
        if kind.contains("mate") then "Consequence: king safety deteriorates immediately and the attack becomes forcing."
        else if kind.contains("material") then "Consequence: material pressure becomes harder to contain in practical play."
        else "Consequence: the opponent dictates the play while your pieces are tied to defense."
      }

    val factConsequence =
      playedCand.flatMap(_.facts.collectFirst {
        case Fact.WeakSquare(square, _, _, _) =>
          s"Consequence: ${square.key} can become a long-term target."
        case Fact.HangingPiece(square, role, _, _, _) =>
          s"Consequence: the ${roleLabel(role)} on ${square.key} can become a direct tactical target."
      })

    val replyConsequence =
      Option.when(cpLoss >= Thresholds.INACCURACY_CP) {
        bestReply.filter(isForcingReplySan).map { r =>
          if cpLoss >= Thresholds.BLUNDER_CP then
            s"Consequence: the opponent has ...$r by force, and the tactical damage is immediate."
          else s"Consequence: the opponent can answer with ...$r and seize the initiative."
        }
      }.flatten

    val severityConsequence =
      Option.when(cpLoss >= Thresholds.INACCURACY_CP) {
        Thresholds.classifySeverity(cpLoss) match
          case "blunder" =>
            NarrativeLexicon.pick(bead ^ 0x7f4a7c15, List(
              "Consequence: tactical control flips immediately and conversion becomes straightforward.",
              "Consequence: king safety and coordination collapse at once.",
              "Consequence: the opponent gets a forcing route with little counterplay."
            ))
          case "mistake" =>
            NarrativeLexicon.pick(bead ^ 0x7f4a7c15, List(
              "Consequence: the opponent improves with forcing moves while your position stays passive.",
              "Consequence: structure or king safety is compromised without compensation.",
              "Consequence: practical control shifts and defense becomes uncomfortable."
            ))
          case "inaccuracy" =>
            NarrativeLexicon.pick(bead ^ 0x7f4a7c15, List(
              "Consequence: the opponent gets the easier plan and more comfortable piece play.",
              "Consequence: piece coordination loosens and counterplay appears.",
              "Consequence: you lose structural clarity and give up practical initiative."
            ))
          case _ => ""
      }.filter(_.nonEmpty)

    factConsequence
      .orElse(threatConsequence)
      .orElse(replyConsequence)
      .orElse(severityConsequence)

  private def isForcingReplySan(reply: String): Boolean =
    val r = Option(reply).getOrElse("").trim
    r.nonEmpty && (r.contains("+") || r.contains("#") || r.contains("x"))

  private def buildInvestmentAnnotationDetail(
      ctx: NarrativeContext,
      contract: DecisiveTruthContract
  ): Option[String] =
    val prefix =
      contract.surfaceMode match
        case TruthSurfaceMode.InvestmentExplain =>
          contract.truthClass match
            case DecisiveTruthClass.WinningInvestment     => "The investment works because the payoff rests on"
            case DecisiveTruthClass.CompensatedInvestment => "The investment still works because the payoff rests on"
            case _                                        => "The move works because the payoff rests on"
        case TruthSurfaceMode.MaintenancePreserve =>
          "The compensation still has to be preserved through"
        case TruthSurfaceMode.ConversionExplain =>
          "The conversion works because the payoff rests on"
        case _ =>
          "The move works because the payoff rests on"
    contract.verifiedPayoffAnchor
      .map(carrier => s"$prefix $carrier.")
      .orElse(
        CompensationInterpretation
          .effectiveSemanticDecision(ctx)
          .filter(_.decision.accepted)
          .flatMap { semanticDecision =>
            val summaryCarrier = normalizedInvestmentCarrier(semanticDecision.compensation.conversionPlan)
            val vectorCarrier =
              semanticDecision.compensation.returnVector.toList
                .sortBy { case (_, weight) => -weight }
                .map(_._1)
                .flatMap(normalizedInvestmentCarrier)
                .headOption
            summaryCarrier
              .map(carrier => s"$prefix $carrier.")
              .orElse(vectorCarrier.map(carrier => s"$prefix $carrier."))
          }
      )

  private def normalizedInvestmentCarrier(raw: String): Option[String] =
    Option(raw)
      .map(UserFacingSignalSanitizer.sanitize)
      .map(_.trim.stripSuffix("."))
      .filter(_.nonEmpty)
      .map(
        _.replaceFirst("(?i)^a path to compensation through\\s+", "")
          .replaceFirst("(?i)^compensation carrier:\\s*", "")
          .replaceFirst("(?i)^the move gives up material because\\s+", "")
          .replaceAll("\\s+", " ")
          .trim
      )
      .filter(_.nonEmpty)

  private def harmonizeAnnotationTone(text: String, cpLoss: Int, isBest: Boolean, contextHint: Int): AnnotationTextProjection =
    if text.trim.isEmpty then
      AnnotationTextProjection(
        coreText =
          AuthorityTaggedFragment(
            "",
            FragmentAuthority.requires_move_linked_anchor,
            moveLinkedAnchor = true,
            sceneGrounded = true
          )
      )
    else if isBest || cpLoss <= 35 then
      AnnotationTextProjection(
        coreText =
          AuthorityTaggedFragment(
            softenNearBestTone(text).trim,
            FragmentAuthority.requires_move_linked_anchor,
            moveLinkedAnchor = true,
            sceneGrounded = true,
            generalized = true
          )
      )
    else if cpLoss >= Thresholds.INACCURACY_CP && !containsNegativeTone(text) then
      AnnotationTextProjection(
        coreText =
          AuthorityTaggedFragment(
            text.trim,
            FragmentAuthority.requires_move_linked_anchor,
            moveLinkedAnchor = true,
            sceneGrounded = true,
            generalized = true
          ),
        severityTail =
          Some(
            AuthorityTaggedFragment(
              buildSeverityTail(Math.abs(text.hashCode) ^ 0x239b961b, cpLoss, contextHint),
              FragmentAuthority.unsafe_as_truth
            )
          )
      )
    else
      AnnotationTextProjection(
        coreText =
          AuthorityTaggedFragment(
            text.trim,
            FragmentAuthority.requires_move_linked_anchor,
            moveLinkedAnchor = true,
            sceneGrounded = true,
            generalized = true
          )
      )

  private def softenNearBestTone(text: String): String =
    List(
      ("(?i)\\bblunder\\b", "detour"),
      ("(?i)\\bmistake\\b", "detour"),
      ("(?i)\\binaccuracy\\b", "detour"),
      ("(?i)\\bimprecise\\b", "less direct"),
      ("(?i)\\binferior\\b", "less direct"),
      ("(?i)\\berror\\b", "detour"),
      ("(?i)\\bdrops\\b", "concedes"),
      ("(?i)\\bloses\\b", "concedes"),
      ("(?i)\\bslip\\b", "tempo loss"),
      ("(?i)\\bmisses\\b", "bypasses")
    ).foldLeft(text) { case (acc, (pattern, replacement)) =>
      acc.replaceAll(pattern, replacement)
    }

  private def containsNegativeTone(text: String): Boolean =
    val low = text.toLowerCase
    List("blunder", "mistake", "inaccuracy", "imprecise", "misses", "slip", "inferior", "drops", "loses", "error")
      .exists(low.contains)

  private def enforceAnnotationPolarity(
    text: AnnotationTextProjection,
    cpLoss: Int,
    isBest: Boolean,
    contextHint: Int
  ): AnnotationTextProjection =
    if text.renderedText.trim.isEmpty then text
    else
      val nearBest = isBest || cpLoss <= 25
      val severeError = cpLoss >= 140
      val containsBenchmarkStrongPositive = containsBenchmarkStrongPositiveLexicon(text.coreText.rawText)

      val neutralized =
        if nearBest then neutralizeBenchmarkNegativeLexicon(text.coreText.rawText)
        else text.coreText.rawText

      val softenedPositive =
        if severeError && containsBenchmarkStrongPositive then
          neutralizeBenchmarkStrongPositiveLexicon(neutralized)
        else neutralized

      val severityTail =
        if severeError && !containsBenchmarkNegativeLexicon(softenedPositive) then
          text.severityTail.orElse(
            Some(
              AuthorityTaggedFragment(
                buildSeverityTail(Math.abs(softenedPositive.hashCode) ^ 0x6f4b1321, cpLoss, contextHint),
                FragmentAuthority.unsafe_as_truth
              )
            )
          )
        else text.severityTail

      AnnotationTextProjection(
        coreText =
          AuthorityTaggedFragment(
            softenedPositive.trim,
            FragmentAuthority.requires_move_linked_anchor,
            moveLinkedAnchor = true,
            sceneGrounded = true,
            generalized = true
          ),
        severityTail = severityTail.filter(_.hasRawText)
      )

  private def buildSeverityTail(bead: Int, cpLoss: Int, contextHint: Int): String =
    val seed = bead ^ contextHint ^ Math.abs(cpLoss) ^ 0x5bd1e995
    val templates =
      Thresholds.classifySeverity(cpLoss) match
        case "blunder" =>
          List(
            "This is a blunder, so forcing control shifts to the opponent.",
            "Because this blunder loosens coordination, the opponent gets a direct conversion route.",
            "This blunder loses tactical control; as a result, recovery becomes difficult.",
            "This blunder concedes initiative, therefore the defensive workload spikes immediately.",
            "This blunder gives the opponent a forcing path, while your counterplay resources shrink.",
            "A decisive blunder that collapses defensive stability and permits forcing progress.",
            "This severe blunder hands over practical control in a single sequence.",
            "Tactical stability is lost after this blunder, making the subsequent task significantly harder."
          )
        case "mistake" =>
          List(
            "This is a clear mistake, so practical control swings away quickly.",
            "This mistake yields an easier conversion plan, because your coordination is slower.",
            "This mistake concedes initiative, and as a result your defensive options narrow.",
            "This mistake gives the opponent the cleaner continuation, while your plan becomes reactive.",
            "This mistake leaves you defending without counterplay, therefore every tempo matters.",
            "A noticeable mistake that complicates the defensive task unnecessarily.",
            "This mistake allows the opponent to stabilize an advantage with less effort.",
            "Coordination is disrupted by this mistake, leading to a harder practical fight."
          )
        case _ =>
          List(
            "This is an inaccuracy, so the opponent's play becomes easier to handle.",
            "This inaccuracy gives up practical initiative, because the move-order becomes less precise.",
            "This inaccuracy drifts from the best plan; as a result, defensive workload increases.",
            "This inaccuracy leaves the opponent with a smoother sequence, while your structure is harder to coordinate.",
            "This inaccuracy hands over simpler choices, therefore practical pressure rises.",
            "A slight inaccuracy from the best route eases the opponent's defensive duties.",
            "This inaccuracy mildly softens the pressure compared to the strongest line.",
            "The resulting position from this inaccuracy is slightly less challenging to handle for the opponent."
          )
    selectNonRepeatingTemplate(
      templates = templates,
      seed = seed,
      usedStems = Set(normalizeStem("This is a mistake that gives the opponent easier play.")),
      prefixCounts = Map.empty,
      prefixLimits = PrefixFamilyLimits
    )

  private def containsBenchmarkNegativeLexicon(text: String): Boolean =
    val low = Option(text).getOrElse("").toLowerCase
    List("blunder", "mistake", "inaccuracy", "misses", "slip", "inferior", "drops", "loses")
      .exists(term => low.matches(s""".*\\b$term\\b.*"""))

  private def containsBenchmarkStrongPositiveLexicon(text: String): Boolean =
    val low = text.toLowerCase
    List("best move", "excellent choice", "strong move", "very accurate", "precise move", "fully sound")
      .exists(low.contains)

  private def neutralizeBenchmarkNegativeLexicon(text: String): String =
    List(
      ("(?i)\\bblunder\\b", "detour"),
      ("(?i)\\bmistake\\b", "detour"),
      ("(?i)\\binaccuracy\\b", "detour"),
      ("(?i)\\bmisses\\b", "bypasses"),
      ("(?i)\\bslip\\b", "tempo loss"),
      ("(?i)\\binferior\\b", "less direct"),
      ("(?i)\\bdrops\\b", "concedes"),
      ("(?i)\\bloses\\b", "concedes")
    ).foldLeft(text) { case (acc, (pattern, replacement)) =>
      acc.replaceAll(pattern, replacement)
    }

  private def neutralizeBenchmarkStrongPositiveLexicon(text: String): String =
    List(
      ("(?i)\\bbest move\\b", "reference move"),
      ("(?i)\\bexcellent choice\\b", "practical option"),
      ("(?i)\\bstrong move\\b", "practical move"),
      ("(?i)\\bvery accurate\\b", "playable"),
      ("(?i)\\bprecise move\\b", "reference move"),
      ("(?i)\\bfully sound\\b", "playable")
    ).foldLeft(text) { case (acc, (pattern, replacement)) =>
      acc.replaceAll(pattern, replacement)
    }

  private def extractFactConsequence(candidate: CandidateInfo, currentPly: Int): Option[String] =
    val facts = candidate.facts
    val prioritized = facts.sortBy {
      case _: Fact.HangingPiece => 0
      case _: Fact.Pin          => 1
      case _: Fact.Fork         => 2
      case _: Fact.Skewer       => 3
      case _: Fact.WeakSquare   => 4
      case _                    => 99
    }

    prioritized.collectFirst(Function.unlift { fact =>
      val body = factConsequenceBody(fact)
      fact.scope match
        case FactScope.Now =>
          body
        case FactScope.CandidatePv =>
          val citation =
            LineScopedCitation.tacticalCitationFromSanMoves(currentPly + 1, candidate.lineSanMoves, candidate.lineMotifs)
              .orElse(LineScopedCitation.strategicCitationFromSanMoves(currentPly + 1, candidate.lineSanMoves))
          for
            cited <- citation
            b <- body
            sentence <- LineScopedCitation.afterClause(cited, b)
          yield sentence
        case _ =>
          None
    })

  private def factConsequenceBody(fact: Fact): Option[String] =
    Option(fact).collect {
      case Fact.HangingPiece(square, role, _, defenders, _) if defenders.isEmpty =>
        s"it leaves the ${roleLabel(role)} on ${square.key} hanging."
      case Fact.Pin(_, _, pinned, pinnedRole, behind, behindRole, _, _) =>
        s"it allows a pin on ${pinned.key}, tying the ${roleLabel(pinnedRole)} to the ${roleLabel(behindRole)} on ${behind.key}."
      case Fact.Fork(attacker, attackerRole, targets, _) if targets.nonEmpty =>
        val targetText = targets.take(2).map { case (sq, r) => s"${roleLabel(r)} on ${sq.key}" } match
          case a :: b :: Nil => s"$a and $b"
          case a :: Nil      => a
          case _             => "multiple targets"
        s"it allows a fork by the ${roleLabel(attackerRole)} on ${attacker.key} against $targetText."
      case Fact.Skewer(attacker, attackerRole, front, frontRole, back, backRole, _) =>
        s"it allows a skewer: ${roleLabel(attackerRole)} on ${attacker.key} can hit ${roleLabel(frontRole)} on ${front.key} and then ${roleLabel(backRole)} on ${back.key}."
      case Fact.WeakSquare(square, _, _, _) =>
        s"it creates a durable weakness on ${square.key}."
    }

  private def counterfactualTeachingSentence(
    ctx: NarrativeContext,
    cf: lila.llm.model.strategic.CounterfactualMatch,
    motifOpt: Option[Motif]
  ): Option[String] =
    val citation =
      LineScopedCitation.tacticalCitation(ctx.fen, ctx.ply + 1, cf.bestLine, cf.missedMotifs)
        .orElse(LineScopedCitation.strategicCitation(ctx.fen, ctx.ply + 1, cf.bestLine))
    citation.flatMap { cited =>
      cf.causalThreat match
        case Some(ct) =>
          LineScopedCitation.afterClause(cited, s"the line ${ct.narrative}")
        case None =>
          val body =
            motifOpt match
              case Some(Motif.Fork(_, targets, _, _, _, _, _)) if targets.size >= 2 =>
                s"the fork against the ${targets(0).toString.toLowerCase} and ${targets(1).toString.toLowerCase} appears"
              case Some(Motif.Pin(_, pinned, _, _, _, _, _, _, _)) =>
                s"the pin against the ${pinned.toString.toLowerCase} appears"
              case Some(Motif.Skewer(_, front, back, _, _, _, _, _, _)) =>
                s"the skewer against the ${front.toString.toLowerCase} and ${back.toString.toLowerCase} appears"
              case Some(Motif.Capture(_, captured, _, lila.llm.model.Motif.CaptureType.Winning, _, _, _, _)) =>
                s"winning the ${captured.toString.toLowerCase} becomes available"
              case Some(Motif.DiscoveredAttack(_, _, target, _, _, _, _, _, _)) =>
                s"the discovered attack on the ${target.toString.toLowerCase} appears"
              case Some(m) =>
                s"the idea of ${NarrativeUtils.humanize(motifName(m))} appears"
              case None =>
                "the practical refutation becomes clear"
          LineScopedCitation.afterClause(cited, body)
    }

  private def roleLabel(role: chess.Role): String = role.toString.toLowerCase

  private def defaultIssueBySeverity(bead: Int, cpLoss: Int): String =
    Thresholds.classifySeverity(cpLoss) match
      case "blunder" =>
        NarrativeLexicon.pick(bead, List(
          "this allows a forcing tactical sequence against your king or material",
          "this collapses coordination and gives the opponent a direct conversion route",
          "this fails to meet the immediate tactical threat and the position unravels"
        ))
      case "mistake" =>
        NarrativeLexicon.pick(bead, List(
          "this hands over the initiative and creates long-term defensive burdens",
          "this concedes either structure or king safety without enough return",
          "this lets the opponent improve with simple, forcing moves"
        ))
      case "inaccuracy" =>
        NarrativeLexicon.pick(bead, List(
          "this gives the opponent the easier plan to execute",
          "this loosens piece coordination and invites counterplay",
          "this drifts from the cleanest structure-preserving continuation"
        ))
      case _ => ""

  private def motifName(m: lila.llm.model.Motif): String =
    m.getClass.getSimpleName.replaceAll("\\$", "")

  private def joinWithOr(items: List[String]): String =
    items match
      case Nil => ""
      case one :: Nil => one
      case a :: b :: Nil => s"$a or $b"
      case xs => xs.dropRight(1).mkString(", ") + s", or ${xs.last}"

  private def dedupeEvidenceBranches(branches: List[EvidenceBranch]): List[EvidenceBranch] =
    val seen = scala.collection.mutable.HashSet.empty[String]
    branches.filter { b =>
      val id = LineScopedCitation.evidenceBranchSignature(b).getOrElse("").trim
      if id.isEmpty then true
      else if seen.contains(id) then false
      else
        seen += id
        true
    }

  private def variationLeadSan(fen: String, variation: VariationLine): String =
    variation.ourMove
      .map(_.san.trim)
      .filter(_.nonEmpty)
      .orElse {
        variation.moves.headOption
          .map(uci => NarrativeUtils.uciToSanOrFormat(fen, uci).trim)
          .filter(_.nonEmpty)
      }
      .orElse(variation.moves.headOption)
      .getOrElse("")

  private def variationReplySan(fen: String, variation: VariationLine): Option[String] =
    variation.theirReply
      .map(_.san.trim)
      .filter(_.nonEmpty)
      .orElse {
        variation.moves.lift(1)
          .map(uci => NarrativeUtils.uciToSanOrFormat(NarrativeUtils.uciListToFen(fen, variation.moves.take(1)), uci).trim)
          .filter(_.nonEmpty)
      }

  private def variationEvidenceClause(variation: VariationLine): String =
    val lowerTags = variation.tags.map(_.toString.toLowerCase).toSet
    if lowerTags.contains("sharp") then "evidence indicates tactical volatility in the branch"
    else if lowerTags.contains("simplification") then "evidence favors simplification pressure"
    else if lowerTags.contains("prophylaxis") then "evidence stresses prophylactic coverage"
    else if lowerTags.contains("solid") then "evidence keeps structure handling stable"
    else "evidence keeps strategic commitments coherent"

  private def variationConsequenceClause(variation: VariationLine): Option[String] =
    Option.when(variation.moves.nonEmpty)(s"Evidence trend: ${variationEvidenceClause(variation)}")

  private def normalizeStem(text: String): String =
    Option(text).getOrElse("")
      .toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .filter(_.nonEmpty)
      .take(7)
      .mkString(" ")

  private def prefixFamilyOf(text: String): Option[String] =
    val low = Option(text).getOrElse("").trim.toLowerCase
    if low.isEmpty then None
    else if
      low.startsWith("line route") ||
      low.startsWith("the branch follows") ||
      low.startsWith("the move path") ||
      low.contains("route is")
    then Some("sequence")
    else if
      low.startsWith("strategic shift") ||
      low.startsWith("strategically") ||
      low.startsWith("the practical turning factor") ||
      low.startsWith("the recurring practical theme") ||
      low.startsWith("shared lesson")
    then Some("strategic_shift")
    else if
      low.startsWith("engine") ||
      low.contains(" engine ") ||
      low.contains("multi pv") ||
      low.contains("principal engine")
    then Some("engine")
    else None

  private def selectNonRepeatingTemplate(
    templates: List[String],
    seed: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    prefixLimits: Map[String, Int]
  ): String =
    val clean = templates.map(_.trim).filter(_.nonEmpty).distinct
    if clean.isEmpty then ""
    else
      val start = Math.floorMod(seed, clean.size)
      val rotated = (0 until clean.size).toList.map(i => clean(Math.floorMod(start + i, clean.size)))
      def withinPrefixLimit(t: String): Boolean =
        prefixFamilyOf(t).forall { family =>
          prefixCounts.getOrElse(family, 0) < prefixLimits.getOrElse(family, Int.MaxValue)
        }
      rotated
        .find(t => !usedStems.contains(normalizeStem(t)) && withinPrefixLimit(t))
        .orElse(rotated.find(withinPrefixLimit))
        .getOrElse(rotated.head)

  private def trackTemplateUsage(
    template: String,
    usedStems: scala.collection.mutable.Set[String],
    prefixCounts: scala.collection.mutable.Map[String, Int]
  ): Unit =
    val stem = normalizeStem(template)
    if stem.nonEmpty then usedStems += stem
    prefixFamilyOf(template).foreach { family =>
      prefixCounts.update(family, prefixCounts.getOrElse(family, 0) + 1)
    }

  private def alternativeEngineSignal(
    ctx: NarrativeContext,
    candidate: CandidateInfo,
    ranked: List[VariationLine],
    bestScore: Option[Int],
    bestSan: Option[String]
  ): AlternativeEngineSignal =
    val matched = ranked.zipWithIndex.collectFirst {
      case (v, idx) if variationMatchesCandidate(candidate, v) => (idx + 1, v.effectiveScore)
    }
    val rank = matched.map(_._1)
    val cpLoss = for
      best <- bestScore
      (_, score) <- matched
    yield cpLossForSideToMove(ctx.fen, best, score)
    AlternativeEngineSignal(rank = rank, cpLoss = cpLoss, bestSan = bestSan)

  private def variationMatchesCandidate(candidate: CandidateInfo, variation: VariationLine): Boolean =
    val candUci = candidate.uci.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
    val candSan = normalizeMoveToken(candidate.move)
    candUci.exists(u => variation.moves.headOption.exists(m => NarrativeUtils.uciEquivalent(m, u))) ||
      (candSan.nonEmpty && variation.ourMove.exists(m => normalizeMoveToken(m.san) == candSan))

  private def formatCpGap(cpLoss: Int): String =
    f"${cpLoss.toDouble / 100}%.1f pawns"

  private def appendAlternativeEngineContrast(
    line: String,
    signal: AlternativeEngineSignal,
    bead: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    role: String
  ): String =
    val contrastSeed =
      bead ^
        (signal.rank.getOrElse(0) * 0x45d9f3b) ^
        (signal.cpLoss.getOrElse(0) * 131) ^
        (Math.abs(signal.bestSan.getOrElse("").hashCode) * 17)

    val shouldAttachContrast =
      signal.rank.exists {
        case r if r >= 3 => true
        case 2           => signal.cpLoss.exists(_ >= 25) || Math.floorMod(contrastSeed, 3) != 0
        case _           => false
      }
    val preferPracticalWording = role.equalsIgnoreCase("practical_secondary")

    val contrastTemplates: List[String] =
      if !shouldAttachContrast then Nil
      else signal.rank match
        case Some(2) =>
          val bestRef = signal.bestSan.map(s => s"**$s**").getOrElse("the top engine move")
          signal.cpLoss match
            case Some(loss) if loss <= 20 =>
              if preferPracticalWording then
                List(
                  s"$bestRef still holds the cleaner route, but the practical gap is narrow.",
                  s"This remains playable over the board, with $bestRef keeping only a small edge.",
                  s"Compared with $bestRef, the difference is modest in practical play."
                )
              else
                List(
                  s"Engine order keeps $bestRef first, though this remains close in practical terms.",
                  s"$bestRef still tops the engine list, but the gap here is narrow.",
                  s"The engine shows a slight preference for $bestRef, while this stays near-equivalent over the board.",
                  s"$bestRef stays the main reference move, and the practical margin is slim.",
                  s"The reference line still starts with $bestRef, but practical margins are thin.",
                  s"$bestRef keeps a narrow technical lead while this option stays playable."
                )
            case Some(loss) =>
              if preferPracticalWording then
                List(
                  s"Compared with $bestRef, this concedes about ${formatCpGap(loss)} in practical terms.",
                  s"The practical cost versus $bestRef is roughly ${formatCpGap(loss)}.",
                  s"Against $bestRef, the score gap is about ${formatCpGap(loss)}."
                )
              else
                List(
                  s"The engine still points to $bestRef as cleaner, by about ${formatCpGap(loss)}.",
                  s"In engine terms, $bestRef holds roughly a ${formatCpGap(loss)} edge.",
                  s"The practical gap to $bestRef is around ${formatCpGap(loss)}.",
                  s"Engine preference remains with $bestRef, with roughly a ${formatCpGap(loss)} edge in practical terms.",
                  s"Compared with $bestRef, engine evaluation drops by roughly ${formatCpGap(loss)}."
                )
            case None =>
              if preferPracticalWording then
                List(
                  s"$bestRef remains the cleaner benchmark continuation in this structure.",
                  s"Practical handling is usually easier from $bestRef.",
                  s"$bestRef is still the stable benchmark line."
                )
              else
                List(
                  s"Engine order still favors $bestRef as the cleaner continuation.",
                  s"$bestRef remains the engine reference in this structure.",
                  s"The principal engine route still starts with $bestRef.",
                  s"In the sampled lines, $bestRef remains the benchmark move."
                )
        case Some(r) if r >= 3 =>
          val bestRef = signal.bestSan.map(s => s"**$s**").getOrElse("the primary reference line")
          signal.cpLoss match
            case Some(loss) if loss <= 70 =>
              if preferPracticalWording then
                List(
                  s"As a ${ordinal(r)} practical-tier choice, this trails $bestRef by about ${formatCpGap(loss)}.",
                  s"As a ${ordinal(r)} option, this line trails $bestRef by around ${formatCpGap(loss)}.",
                  s"The ${ordinal(r)} choice is workable, but $bestRef still leads by roughly ${formatCpGap(loss)}."
                )
              else
                List(
                  s"Engine ranking places this around ${ordinal(r)}, with $bestRef ahead by about ${formatCpGap(loss)}.",
                  s"This continuation stays in the ${ordinal(r)} engine group, while $bestRef keeps roughly a ${formatCpGap(loss)} edge.",
                  s"In engine order, $bestRef remains first and this ${ordinal(r)} choice trails by around ${formatCpGap(loss)}."
                )
            case Some(loss) =>
              if preferPracticalWording then
                List(
                  s"As a lower-tier option (around ${ordinal(r)}), this trails $bestRef by roughly ${formatCpGap(loss)}.",
                  s"The score gap to $bestRef is substantial here (about ${formatCpGap(loss)}).",
                  s"This ${ordinal(r)} line leaves a large practical deficit versus $bestRef (about ${formatCpGap(loss)})."
                )
              else
                List(
                  s"This sits in a lower engine tier (about ${ordinal(r)}), and $bestRef leads by roughly ${formatCpGap(loss)}.",
                  s"Engine ranking is clear here: around ${ordinal(r)} for this line, while $bestRef is ahead by ${formatCpGap(loss)}.",
                  s"In engine terms this continuation drops to about ${ordinal(r)}, with $bestRef up by ${formatCpGap(loss)}.",
                  s"Around $bestRef, the principal engine route stays cleaner; this ${ordinal(r)} option is behind by about ${formatCpGap(loss)}."
                )
            case None =>
              if preferPracticalWording then
                List(
                  s"This sits in a lower practical tier (around ${ordinal(r)}), while $bestRef remains the benchmark.",
                  s"$bestRef stays the stable reference line, with this branch in a lower tier.",
                  s"As a ${ordinal(r)} option, this is less reliable than $bestRef."
                )
              else
                List(
                  s"This is a lower-ranked engine option (around ${ordinal(r)}), while $bestRef remains the stable benchmark.",
                  s"Engine ordering puts this below the principal choices; $bestRef is the cleaner reference line.",
                  s"The sampled engine set keeps this in a lower tier, with $bestRef as the anchor line."
                )
        case _ => Nil

    val base = line.trim
    val contrast =
      if contrastTemplates.isEmpty then None
      else
        Some(
          selectNonRepeatingTemplate(
            templates = contrastTemplates,
            seed = contrastSeed ^ 0x11f17f1d,
            usedStems = usedStems,
            prefixCounts = prefixCounts,
            prefixLimits = PrefixFamilyLimits
          )
        )
    contrast match
      case Some(extra) if base.nonEmpty => normalizeAlternativeTemplateLine(s"$base $extra", bead)
      case _                            => normalizeAlternativeTemplateLine(base, bead)

  private def appendStrategicImplication(
    line: String,
    move: String,
    role: String,
    plan: String,
    diffLabel: String,
    signal: AlternativeEngineSignal,
    bead: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int]
  ): String =
    val cleanedPlan = plan.replaceAll("""[_\-]+""", " ").trim
    val genericPlans = Set("unknown", "development", "positional maneuvering", "quiet move", "general play")
    val planHint =
      if cleanedPlan.nonEmpty && !genericPlans.contains(cleanedPlan) then s" around $cleanedPlan"
      else ""
    val informativeDiff =
      diffLabel.nonEmpty &&
      diffLabel != "unknown" &&
      List("complex", "sharp", "precise", "narrow", "forcing", "tactical", "risky", "volatile", "critical")
        .exists(diffLabel.contains)
    val practicalHint =
      if informativeDiff then s" The practical burden is $diffLabel." else ""
    val templates =
      if role.equalsIgnoreCase("engine_primary") then
        List(
          s"With **$move**, conversion around **$move** can stay smoother$planHint, but initiative around **$move** can swing when **$move** hands away a tempo.",
          s"Handled precisely, **$move** keeps piece harmony and king cover aligned$planHint through the next phase.",
          s"From a practical-conversion view, **$move** stays reliable$planHint when defensive timing and coverage stay coordinated.",
          s"**$move** keeps practical burden manageable$planHint by preserving coordination before exchanges."
        )
      else
        List(
          s"In practical terms, **$move** is judged by conversion ease$planHint, because defensive coordination can diverge quickly.$practicalHint",
          s"After **$move**, king safety and tempo stay linked, so one inaccurate sequence can hand over initiative$planHint.$practicalHint",
          s"With **$move**, a move-order slip can expose coordination gaps$planHint, and recovery windows are short.$practicalHint",
          s"After **$move**, sequence accuracy matters because coordination and activity can separate quickly$planHint.$practicalHint",
          s"Strategically, **$move** needs connected follow-up through the next phase$planHint, or initiative control leaks away.$practicalHint"
        )
    val implication = selectNonRepeatingTemplate(
      templates = templates.map(_.trim).filter(_.nonEmpty).distinct,
      seed = bead ^ (signal.rank.getOrElse(0) * 0x7f4a7c15) ^ Math.abs(move.hashCode),
      usedStems = usedStems,
      prefixCounts = prefixCounts,
      prefixLimits = PrefixFamilyLimits
    )
    if line.trim.isEmpty then implication.trim
    else s"${line.trim} ${implication.trim}".trim

  private def preferredIntent(c: CandidateInfo): String =
    c.structureGuidance
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(c.planAlignment)

  private def renderAlternativeDiversified(
    c: CandidateInfo,
    idx: Int,
    bead: Int,
    usedFamilies: Set[String],
    signal: AlternativeEngineSignal,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    role: String
  ): (String, String) =
    val rawReason = c.whyNot.flatMap(humanizeWhyNot).map(_.trim).filter(_.nonEmpty).map(_.stripSuffix("."))
    val move = c.move.trim
    val plan = preferredIntent(c).trim.toLowerCase
    val alignmentBand = c.alignmentBand.map(_.trim.toLowerCase)
    val diff = c.practicalDifficulty.trim.toLowerCase
    val diffLabel = diff.replaceAll("""[_\-]+""", " ").trim
    val informativePracticalHint =
      diffLabel.nonEmpty &&
      diffLabel != "unknown" &&
      List("complex", "sharp", "precise", "narrow", "forcing", "tactical", "risky", "volatile", "critical")
        .exists(diffLabel.contains)
    val practicalHint =
      if informativePracticalHint then
        s" Practical burden: $diffLabel."
      else ""
    val bandHint =
      alignmentBand match
        case Some("offplan") => "The structural route is fragile unless the follow-up is precise."
        case Some("unknown") => "Structural read is uncertain, so concrete verification is essential."
        case Some("onbook") => "The continuation stays structurally coherent with accurate handling."
        case _ => ""

    val preferredFamilies: List[String] =
      if alignmentBand.contains("offplan") then List("tradeoff", "practical", "strategic", "generic")
      else if alignmentBand.contains("onbook") then List("technical", "strategic", "practical", "generic")
      else if rawReason.nonEmpty then List("tradeoff", "practical", "strategic", "generic")
      else if alignmentBand.contains("unknown") then List("practical", "strategic", "generic")
      else if c.tags.contains(CandidateTag.Sharp) || c.tags.contains(CandidateTag.TacticalGamble) || diff.contains("complex") then
        List("dynamic", "strategic", "practical", "generic")
      else if c.tags.contains(CandidateTag.Solid) || c.tags.contains(CandidateTag.Converting) || diff.contains("clean") then
        List("technical", "strategic", "practical", "generic")
      else
        List("strategic", "practical", "dynamic", "technical", "generic")

    val family = preferredFamilies.find(f => !usedFamilies.contains(f)).getOrElse(preferredFamilies.headOption.getOrElse("generic"))
    val localBead = bead ^ Math.abs(move.hashCode) ^ (idx + 1) * 0x9e3779b9
    val reason = rawReason.map(r => diversifyAlternativeReason(r, localBead))

    val baseTemplates =
      family match
        case "tradeoff" =>
          val r = reason.getOrElse("it concedes dynamic chances")
          List(
            s"**$move** is playable, but $r.",
            s"**$move** can work, although $r.",
            s"From a practical angle, **$move** is viable, yet $r.",
            s"With **$move**, the tradeoff is concrete: $r.",
            s"**$move** stays in range, though $r.",
            s"**$move** is serviceable over the board, but $r."
          )
        case "dynamic" =>
          List(
            s"**$move** keeps the game dynamic and can lead to sharper play.",
            s"**$move** invites complications and active piece play.",
            s"With **$move**, the position stays tense and tactical.",
            s"**$move** keeps tactical pressure alive and asks for concrete calculation."
          )
        case "technical" =>
          List(
            s"**$move** is the cleaner technical route, aiming for a stable structure.",
            s"**$move** heads for a controlled position with fewer tactical swings.",
            s"**$move** favors structural clarity and methodical handling over complications.",
            s"**$move** is a clean technical route that lightens defensive duties.",
            s"**$move** aims for a technically manageable position with clear conversion paths.",
            s"**$move** keeps the game in a technical channel where precise handling is rewarded."
          )
        case "strategic" =>
          val planHint =
            if plan.nonEmpty && plan != "unknown" then
              val cleaned = plan.replaceAll("""[_\-]+""", " ").trim
              s" around $cleaned"
            else ""
          List(
            s"**$move** is a strategic alternative$planHint.",
            s"**$move** points to a different strategic plan$planHint.",
            s"**$move** takes the game into another strategic channel$planHint.",
            s"**$move** keeps a coherent strategic direction$planHint.",
            s"Strategically, **$move** is a viable reroute$planHint."
          )
        case "practical" =>
          List(
            s"In practical play, **$move** is viable, and the next two moves must stay accurate.",
            s"**$move** is viable over the board, though move-order precision matters.",
            s"**$move** is playable in practice, but concrete calculation is required.",
            s"Over the board, **$move** is acceptable if tactical details are controlled.",
            s"**$move** remains practical, but one inaccurate follow-up can change the assessment.$practicalHint",
            s"**$move** can be handled in practical play, but sequencing accuracy is non-negotiable.$practicalHint",
            s"Real-game handling of **$move** is possible, though the follow-up order must stay exact.$practicalHint"
          )
        case _ =>
          List(NarrativeLexicon.getAlternative(localBead, move, c.whyNot.flatMap(humanizeWhyNot)))

    val line =
      selectNonRepeatingTemplate(
        templates = baseTemplates.map(_.trim).filter(_.nonEmpty).distinct,
        seed = localBead ^ 0x3124bcf5,
        usedStems = usedStems,
        prefixCounts = prefixCounts,
        prefixLimits = PrefixFamilyLimits
      )

    val contrasted =
      appendAlternativeEngineContrast(
        line = line,
        signal = signal,
        bead = localBead ^ 0x63d5a6f1,
        usedStems = usedStems + normalizeStem(line),
        prefixCounts = prefixCounts,
        role = role
      )
    val withImplication =
      appendStrategicImplication(
        line = contrasted,
        move = move,
        role = role,
        plan = plan,
        diffLabel = diffLabel,
        signal = signal,
        bead = localBead ^ 0x6d2b79f5,
        usedStems = usedStems + normalizeStem(contrasted),
        prefixCounts = prefixCounts
      )
    val withBand =
      if bandHint.nonEmpty then s"${withImplication.trim} $bandHint".trim
      else withImplication

    (withBand, family)

  private def alternativesRepetitionPenalty(lines: List[String]): Int =
    val stems = lines.map(normalizeAlternativeStem).filter(_.nonEmpty)
    val stemPenalty =
      stems.groupBy(identity).valuesIterator.map(c => (c.size - 1).max(0) * 15).sum

    val ngramPenalty = lines.map(normalizeAlternativeTokens).map { tokens =>
      val tri = ngramRepeatPenalty(tokens, 3, threshold = 2, weight = 6)
      val four = ngramRepeatPenalty(tokens, 4, threshold = 2, weight = 10)
      tri + four
    }.sum

    stemPenalty + ngramPenalty

  private def normalizeAlternativeStem(line: String): String =
    normalizeStem(line)

  private def normalizeAlternativeTokens(line: String): List[String] =
    line.toLowerCase
      .replaceAll("""\*\*[^*]+\*\*""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .replaceAll("""\b\d+(?:\.\d+)?\b""", " ")
      .replaceAll("""[^a-z\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .split(" ")
      .toList
      .filter(_.nonEmpty)

  private def ngramRepeatPenalty(tokens: List[String], n: Int, threshold: Int, weight: Int): Int =
    if tokens.lengthCompare(n) < 0 then 0
    else
      tokens
        .sliding(n)
        .map(_.mkString(" "))
        .toList
        .groupBy(identity)
        .valuesIterator
        .map(_.size)
        .map(c => (c - threshold + 1).max(0) * weight)
        .sum

  private def normalizeAlternativeTemplateLine(line: String, bead: Int): String =
    val compact = line.replaceAll("""\s+""", " ").trim
    if compact.isEmpty then compact
    else
      val fixedPractical = compact
        .replace(
          "is a workable practical choice, but it leaves little room for imprecision.",
          NarrativeLexicon.pick(bead ^ 0x2a2a2a2a, List(
            "remains practical, but move-order precision is critical.",
            "is playable, though concrete follow-up accuracy is mandatory."
          ))
        )
        .replace(
          "keeps the game on its most coherent technical track.",
          NarrativeLexicon.pick(bead ^ 0x4b4b4b4b, List(
            "keeps the continuation strategically clean and practical.",
            "preserves structural clarity while keeping options flexible.",
            "stays on a stable plan that is easier to execute.",
            "maintains practical control without forcing complications."
          ))
        )
      val fixedLead =
        if fixedPractical.contains("Against the main move") then
          fixedPractical.replaceFirst(
            "Against the main move",
            NarrativeLexicon.pick(bead ^ 0x2f6e2b1, List("Compared with", "Relative to", "Versus"))
          )
        else fixedPractical
      fixedLead

  private def diversifyAlternativeReason(reason: String, bead: Int): String =
    val cleaned = reason.trim.stripSuffix(".")
    val slightConcession = """(?i)^slight practical concession after\s+(.+)$""".r
    val decisiveLoss = """(?i)^decisive loss after\s+(.+)$""".r
    val significantDisadvantage = """(?i)^significant disadvantage after\s+(.+)$""".r

    cleaned match
      case slightConcession(rest) =>
        val localSeed = bead ^ 0x1a2b3c4d ^ Math.abs(rest.hashCode)
        NarrativeLexicon.pick(localSeed, List(
          s"it yields a modest practical concession once $rest appears",
          s"after $rest, execution around $rest eases the defensive task",
          s"it grants a cleaner practical route to the opponent after $rest"
        ))
      case decisiveLoss(rest) =>
        val localSeed = bead ^ 0x2b3c4d5e ^ Math.abs(rest.hashCode)
        NarrativeLexicon.pick(localSeed, List(
          s"it runs into a decisive sequence after $rest",
          s"the line becomes losing after $rest",
          s"it allows a forcing collapse after $rest"
        ))
      case significantDisadvantage(rest) =>
        val localSeed = bead ^ 0x3c4d5e6f ^ Math.abs(rest.hashCode)
        NarrativeLexicon.pick(localSeed, List(
          s"it yields a notable disadvantage after $rest",
          s"the position worsens materially after $rest",
          s"it concedes a significant practical deficit after $rest"
        ))
      case _ => cleaned
