package lila.commentary.analysis

import chess.*
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.model._
import lila.commentary.model.authoring._
import lila.commentary.model.strategic.VariationLine
import lila.commentary.analysis.render.*
import lila.commentary.analysis.render.FragmentAuthority.{ rawFragmentText, releasedFragmentText, supportFragment }
import lila.commentary.analysis.semantic.RelationObservationCatalog
/**
 * NarrativeOutlineBuilder: SSOT for "what to say"
 *
 * Decision engine for narrative structure.
 * All "what to say" decisions happen here; Renderer only handles phrasing.
 */
object NarrativeOutlineBuilder:
  private val LikelyPawnMovePattern = """^[a-h](?:x[a-h])?[1-8](?:=[QRBN])?[+#]?$""".r
  private val NegativeLexiconPattern = """\b(blunder|mistake|inaccuracy|misses|slip|inferior|drops|loses)\b""".r

  private val NegativeLexiconReplacements = List(
    ("(?i)\\bblunder\\b".r, "detour"),
    ("(?i)\\bmistake\\b".r, "detour"),
    ("(?i)\\binaccuracy\\b".r, "detour"),
    ("(?i)\\bmisses\\b".r, "bypasses"),
    ("(?i)\\bslip\\b".r, "tempo loss"),
    ("(?i)\\binferior\\b".r, "less direct"),
    ("(?i)\\bdrops\\b".r, "concedes"),
    ("(?i)\\bloses\\b".r, "concedes")
  )

  private val StrongPositiveLexiconReplacements = List(
    ("(?i)\\bbest move\\b".r, "reference move"),
    ("(?i)\\bexcellent choice\\b".r, "practical option"),
    ("(?i)\\bstrong move\\b".r, "practical move"),
    ("(?i)\\bvery accurate\\b".r, "playable"),
    ("(?i)\\bprecise move\\b".r, "reference move"),
    ("(?i)\\bfully sound\\b".r, "playable")
  )

  private val MaxOpeningPrecedents = 3
  private val PrecedentConfidenceThreshold = 0.62
  private val PrecedentTemplateSteps = (0 until 4).toList
  private val PrefixFamilyLimits = Map(
    "sequence" -> 1,
    "strategic_shift" -> 1,
    "engine" -> 1
  )
  private val GenericAlternativePlans = Set("unknown", "development", "positional maneuvering", "quiet move", "general play")
  private val InformativeAlternativeDifficultyTerms =
    List("complex", "sharp", "precise", "narrow", "forcing", "tactical", "risky", "volatile", "critical")
  private val TacticalAnnotationReasonBridges =
    List(
      "The tactical verdict is immediate:",
      "The tactical problem is clear:",
      "The tactical punishment is straightforward:",
      "The tactical point is concrete:",
      "The tactical flaw is immediate:"
    )
  private val PracticalAnnotationReasonBridges =
    List(
      "From a practical perspective,",
      "In strategic terms,",
      "That makes the practical picture clear:",
      "So the practical verdict is straightforward:",
      "Viewed through a practical lens,",
      "The practical takeaway is immediate:"
    )
  private val ConsequenceBridges =
    List(
      "Therefore,",
      "As a result,",
      "So,",
      "For that reason,",
      "Consequently,",
      "Accordingly,"
    )
  private val HighPriorityCanonicalMotifTerms =
    Set(
      "stalemate", "repeat", "zugzwang", "perpetual check", "fortress",
      "smothered mate", "underpromotion", "interference", "zwischenzug",
      "greek gift", "king hunt", "deflection"
    )
  private val ThreatCorroboratedTacticalMotifs =
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
    )
  private val ThreatCorroboratedStrategicMotifs =
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
    )
  private val StableContextThemeMotifs =
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
    )
  private case class BoardAnchor(text: String, consumedThreat: Boolean = false, consumedFact: Boolean = false)
  private case class ContextMotifFrame(
    phase: String,
    conceptSummaryMotifs: List[String],
    derivedContextMotifs: List[String],
    deltaMotifs: List[String],
    motifHash: Int,
    highTension: Boolean,
    trustedConceptThemeSignals: List[String],
    motifPrefixCandidates: List[String]
  )
  private case class ContextMotifSources(
    conceptSummaryMotifs: List[String],
    derivedContextMotifs: List[String],
    deltaMotifs: List[String],
    conceptLeadMotifs: List[String],
    motifSignals: List[String],
    deltaMotifSignals: List[String],
    conceptSummarySignals: List[String],
    derivedContextSignals: List[String]
  )
  private case class AlternativeEngineSignal(
    rank: Option[Int],
    cpLoss: Option[Int],
    bestSan: Option[String]
  )
  private case class AlternativeDiversifiedMaterial(
    move: String,
    plan: String,
    diffLabel: String,
    practicalHint: String,
    bandHint: String,
    family: String,
    reason: Option[String],
    localBead: Int
  )
  private case class AlternativeHypothesisDifferenceMaterial(
    mainHypothesis: Option[HypothesisCard],
    alternativeHypothesis: Option[HypothesisCard],
    alternativeClaim: Option[String],
    mainMove: String
  )
  private case class PrecedentSignal(
    triggerMove: String,
    replyMove: Option[String],
    pivotMove: Option[String],
    mechanism: OpeningBranchMechanism,
    confidence: Double
  )
  private case class PrecedentMoveFeatures(
    sanMoves: List[String],
    triggerMove: String,
    replyMove: Option[String],
    pivotMove: Option[String],
    captures: Int,
    checks: Int,
    promotions: Int,
    pawnPushes: Int,
    pieceMoves: Int,
    forcingDensity: Double
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
  private case class PrecedentComparisonRenderState(
    usedStems: scala.collection.mutable.Set[String],
    prefixCounts: scala.collection.mutable.Map[String, Int],
    seenSequenceKeys: scala.collection.mutable.Set[String],
    mechanismUseCounts: scala.collection.mutable.Map[OpeningBranchMechanism, Int]
  )
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
  private case class CausalAnnotationParts(
    rank: Option[String],
    issue: Option[String],
    better: Option[String]
  )
  private case class AnnotationEngineBest(
    uci: Option[String],
    san: Option[String],
    candidate: Option[CandidateInfo]
  )
  private case class AnnotationBestProjection(
    san: String,
    uci: Option[String],
    candidate: Option[CandidateInfo]
  )
  private case class AnnotationMainMoveMaterial(
    playedSan: String,
    playedUci: Option[String],
    truthContract: DecisiveTruthContract,
    bestSan: String,
    bestUci: Option[String],
    playedRank: Option[Int],
    cpLoss: Int,
    playedCand: Option[CandidateInfo],
    bestCand: Option[CandidateInfo],
    isBest: Boolean,
    isInvestment: Boolean
  ):
    def isConstructive: Boolean =
      isBest || isInvestment
  private case class ConcreteAnnotationIssueInput(
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
    allowConcreteBenchmark: Boolean
  )
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
  private case class AlternativeRenderAttempt(
    lines: List[String],
    penalty: Int,
    usedHypothesisFamilies: Set[String],
    usedHypothesisStems: Set[String],
    usedDifferencePrefixes: Set[String],
    usedDifferenceTails: Set[String]
  )
  private case class AlternativeRenderState(
    usedFamilies: scala.collection.mutable.Set[String],
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
  private case class OutlinePlannerMaterial(
    questions: List[AuthorQuestion],
    plannerInputs: QuestionPlannerInputs,
    rankedPlans: RankedQuestionPlans,
    diagnostics: OutlineDiagnostics
  )
  private enum PrecedentRole:
    case Sequence
    case StrategicTransition
    case DecisionDriver

  def build(
      ctx: NarrativeContext,
      rec: TraceRecorder,
      truthContract: Option[DecisiveTruthContract] = None,
      strategyPack: Option[lila.commentary.StrategyPack] = None
  ): (NarrativeOutline, OutlineDiagnostics) =
    val bead = Math.abs(ctx.hashCode)
    val crossBeatState = CrossBeatRepetitionState(
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashMap.empty[String, Int].withDefaultValue(0),
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashSet.empty[String],
      scala.collection.mutable.HashSet.empty[String]
    )
    val isAnnotation = isMoveAnnotation(ctx)
    val plannerMaterial = buildOutlinePlannerMaterial(ctx, strategyPack, truthContract)
    val beats =
      buildOutlineBeats(
        ctx = ctx,
        rec = rec,
        truthContract = truthContract,
        bead = bead,
        isAnnotation = isAnnotation,
        crossBeatState = crossBeatState,
        plannerMaterial = plannerMaterial
    )
    (NarrativeOutline(beats, Some(plannerMaterial.diagnostics)), plannerMaterial.diagnostics)

  private def buildOutlinePlannerMaterial(
      ctx: NarrativeContext,
      strategyPack: Option[lila.commentary.StrategyPack],
      truthContract: Option[DecisiveTruthContract]
  ): OutlinePlannerMaterial =
    val questions =
      ctx.authorQuestions
        .sortBy(question => (question.priority, question.kind.toString, question.id))
        .take(3)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack, truthContract)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract)
    OutlinePlannerMaterial(
      questions = questions,
      plannerInputs = plannerInputs,
      rankedPlans = rankedPlans,
      diagnostics = outlineDiagnostics(ctx, questions, rankedPlans)
    )

  private def outlineDiagnostics(
      ctx: NarrativeContext,
      questions: List[AuthorQuestion],
      rankedPlans: RankedQuestionPlans
  ): OutlineDiagnostics =
    val availablePurposes = ctx.authorEvidence.map(_.purpose).toSet
    val missingPurposes = EvidencePlanner.getMissingPurposesFromEvidence(questions, ctx.authorEvidence)
    OutlineDiagnostics(
      selectedQuestions = questions,
      usedEvidencePurposes = availablePurposes,
      missingEvidencePurposes = missingPurposes,
      plannerPrimary = rankedPlans.primary.map(plan => s"${plan.questionKind}:${plan.fallbackMode}"),
      plannerSecondary = rankedPlans.secondary.map(plan => s"${plan.questionKind}:${plan.fallbackMode}"),
      plannerRejected =
        rankedPlans.rejected.map(rejected =>
          s"${rejected.questionKind}:${rejected.fallbackMode}:${rejected.reasons.mkString("+")}"
        ),
      plannerSceneType = Some(rankedPlans.ownerTrace.sceneType.wireName),
      plannerOwnerCandidates = rankedPlans.ownerTrace.ownerCandidateLabels,
      plannerAdmittedOwners = rankedPlans.ownerTrace.admittedPlannerOwnerLabels,
      plannerDroppedOwners = rankedPlans.ownerTrace.droppedPlannerOwnerLabels,
      plannerSupportMaterialSeparation = rankedPlans.ownerTrace.supportMaterialSeparationLabels,
      plannerProposedOwnerMappings = rankedPlans.ownerTrace.proposedOwnerMappingLabels,
      plannerDemotionReasons = rankedPlans.ownerTrace.demotionReasons,
      plannerSelectedQuestion = rankedPlans.ownerTrace.selectedQuestion.map(_.toString),
      plannerSelectedOwnerKind = rankedPlans.ownerTrace.selectedPlannerOwnerKind.map(_.wireName),
      plannerSelectedSource = rankedPlans.ownerTrace.selectedPlannerSource,
      surfaceReplayOutcome =
        Some(if rankedPlans.primary.nonEmpty then "outline_planner_owned" else "outline_no_primary")
    )

  private def buildOutlineBeats(
      ctx: NarrativeContext,
      rec: TraceRecorder,
      truthContract: Option[DecisiveTruthContract],
      bead: Int,
      isAnnotation: Boolean,
      crossBeatState: CrossBeatRepetitionState,
      plannerMaterial: OutlinePlannerMaterial
  ): List[OutlineBeat] =
    val beats = scala.collection.mutable.ListBuffer.empty[OutlineBeat]

    if isAnnotation then
      buildMoveHeader(ctx, rec).foreach(beat => beats += annotateBeat(beat))

    beats += annotateBeat(buildContextBeat(ctx, rec, bead))

    buildDecisionBeat(
      ctx,
      plannerMaterial.questions,
      plannerMaterial.rankedPlans,
      plannerMaterial.plannerInputs,
      rec
    ).foreach(beat => beats += annotateBeat(beat))

    buildEvidenceBeat(ctx, plannerMaterial.questions, plannerMaterial.rankedPlans, rec)
      .foreach(beat => beats += annotateBeat(beat))

    buildTeachingBeat(ctx, plannerMaterial.rankedPlans, rec).foreach(beat => beats += annotateBeat(beat))

    val collapsedEarlyOpening = EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx, truthContract)
    val moveLevelPrecedent =
      if collapsedEarlyOpening then None else buildContextPrecedentSentence(ctx, bead)
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

    buildOpeningTheoryBeat(
      ctx,
      rec,
      suppressPrecedents = collapsedEarlyOpening || moveLevelPrecedent.nonEmpty
    ).foreach(beat => beats += annotateBeat(beat))

    val altBeat = annotateBeat(buildAlternativesBeat(ctx, rec, bead, crossBeatState))
    if altBeat.text.nonEmpty then beats += altBeat

    if !collapsedEarlyOpening then
      buildWrapUpBeat(ctx, plannerMaterial.rankedPlans, bead).foreach(beat => beats += annotateBeat(beat))

    beats.toList

  private def strategicPlanNames(ctx: NarrativeContext): List[String] =
    StrategicNarrativePlanSupport.evidenceBackedPlanNames(ctx)

  private def topStrategicPlanName(ctx: NarrativeContext): Option[String] =
    strategicPlanNames(ctx).headOption

  def isMoveAnnotation(ctx: NarrativeContext): Boolean =
    ctx.playedMove.isDefined && ctx.playedSan.isDefined

  private def joinNonBlank(parts: Iterable[String]): String =
    parts.filter(_.trim.nonEmpty).mkString(" ")

  private def buildMoveHeader(ctx: NarrativeContext, rec: TraceRecorder): Option[OutlineBeat] =
    ctx.playedSan.map { san =>
      val moveNum = (ctx.ply + 1) / 2
      val prefix = if ctx.ply % 2 == 1 then s"$moveNum." else s"$moveNum..."
      val bead = Math.abs(ctx.hashCode)
      
      val evaluation = ctx.openingGoalEvaluation

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
    val keyFact = pickKeyFact(ctx)
    val motifFrame = buildContextMotifFrame(ctx, keyFact)
    val openingPart = buildContextOpeningPart(ctx, motifFrame, bead)
    val boardAnchor = appendContextLeadFragments(parts, ctx, motifFrame, keyFact, openingPart, bead)

    appendContextThreat(parts, concepts, rec, ctx, boardAnchor, bead)
    appendContextPlan(parts, concepts, rec, ctx, bead)
    appendRecordedContexts(parts, concepts, rec, ctx)
    appendContextFact(parts, ctx, boardAnchor, keyFact, bead)
    appendContextPawnBreak(parts, concepts, rec, ctx, bead)

    OutlineBeat(
      kind = OutlineBeatKind.Context,
      text = releasedFragmentText(parts.toList),
      conceptIds = concepts.toList,
      focusPriority = 100,
      fullGameEssential = true
    )

  private def buildContextMotifFrame(
      ctx: NarrativeContext,
      keyFact: Option[Fact]
  ): ContextMotifFrame =
    val phase = ctx.phase.current
    val sources = contextMotifSources(ctx)
    ContextMotifFrame(
      phase = phase,
      conceptSummaryMotifs = sources.conceptSummaryMotifs,
      derivedContextMotifs = sources.derivedContextMotifs,
      deltaMotifs = sources.deltaMotifs,
      motifHash = contextMotifHash(sources.motifSignals),
      highTension = contextHighTension(ctx, sources.motifSignals),
      trustedConceptThemeSignals = trustedContextThemeSignalsFromSources(ctx, keyFact, phase, sources),
      motifPrefixCandidates = contextMotifPrefixCandidatesFromSources(ctx, keyFact, phase, sources)
    )

  private def trustedContextThemeSignalsFromSources(
    ctx: NarrativeContext,
    keyFact: Option[Fact],
    phase: String,
    sources: ContextMotifSources
  ): List[String] =
    trustedContextThemeSignals(
      conceptSummaryMotifs = sources.conceptSummaryMotifs,
      derivedContextSignals = sources.derivedContextSignals,
      deltaMotifSignals = sources.deltaMotifSignals,
      keyFact = keyFact,
      ctx = ctx,
      phase = phase
    )

  private def contextMotifPrefixCandidatesFromSources(
    ctx: NarrativeContext,
    keyFact: Option[Fact],
    phase: String,
    sources: ContextMotifSources
  ): List[String] =
    contextMotifPrefixCandidates(
      conceptLeadMotifs = sources.conceptLeadMotifs,
      derivedContextMotifs = sources.derivedContextMotifs,
      deltaMotifs = sources.deltaMotifs,
      deltaMotifSignals = sources.deltaMotifSignals,
      conceptSummarySignals = sources.conceptSummarySignals,
      derivedContextSignals = sources.derivedContextSignals,
      keyFact = keyFact,
      ctx = ctx,
      phase = phase
    )

  private def contextMotifSources(ctx: NarrativeContext): ContextMotifSources =
    val conceptSummaryMotifs = ctx.semantic.toList.flatMap(_.conceptSummary).map(_.trim).filter(_.nonEmpty).distinct
    val derivedContextMotifs = collectDerivedContextMotifs(ctx)
    val conceptMotifs = (conceptSummaryMotifs ++ derivedContextMotifs).distinct
    val deltaMotifs = ctx.delta.map(_.newMotifs).getOrElse(Nil)
    val motifs = (deltaMotifs ++ conceptMotifs).distinct
    ContextMotifSources(
      conceptSummaryMotifs = conceptSummaryMotifs,
      derivedContextMotifs = derivedContextMotifs,
      deltaMotifs = deltaMotifs,
      conceptLeadMotifs = contextConceptLeadMotifs(ctx, conceptMotifs),
      motifSignals = motifs.map(normalizeMotifKey).filter(_.nonEmpty),
      deltaMotifSignals = deltaMotifs.map(normalizeMotifKey).filter(_.nonEmpty),
      conceptSummarySignals = conceptSummaryMotifs.map(normalizeMotifKey).filter(_.nonEmpty),
      derivedContextSignals = derivedContextMotifs.map(normalizeMotifKey).filter(_.nonEmpty)
    )

  private def contextConceptLeadMotifs(ctx: NarrativeContext, conceptMotifs: List[String]): List[String] =
    val salientConceptMotifs = conceptMotifs.filter(NarrativeLexicon.isMotifPrefixSignal)
    if salientConceptMotifs.nonEmpty then salientConceptMotifs.take(2)
    else if ctx.ply % 3 == 0 then conceptMotifs.take(2)
    else Nil

  private def contextHighTension(ctx: NarrativeContext, motifSignals: List[String]): Boolean =
    motifSignals.exists(tacticalTensionMotif) ||
      ctx.threats.toUs.headOption.exists(t => t.lossIfIgnoredCp >= 250 || t.kind.equalsIgnoreCase("Mate"))

  private def contextMotifHash(motifSignals: List[String]): Int =
    motifSignals.foldLeft(0)((acc, m) => acc ^ Math.abs(m.hashCode))

  private def buildContextOpeningPart(
      ctx: NarrativeContext,
      motifFrame: ContextMotifFrame,
      bead: Int
  ): String =
    val evalOpt = rankedEngineVariations(ctx).headOption.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp))
    val strategicNames = strategicPlanNames(ctx)
    val wPlan = strategicNames.headOption
    val bPlan = strategicNames.lift(1)
    val isBalanced = evalOpt.exists(cp => cp >= -80 && cp <= 80)
    val imbalanceOpt = if (isBalanced) buildImbalanceContrast(ctx) else None
    val evalText = imbalanceOpt match
      case Some((whiteAdv, blackAdv)) =>
        NarrativeLexicon.getEvaluativeImbalanceStatement(bead ^ 0x1b873593, evalOpt.getOrElse(0), whiteAdv, blackAdv, ply = ctx.ply)
      case None =>
        evalOpt.map(cp => NarrativeLexicon.getEvaluativePlanStatement(bead ^ 0x1b873593, cp, wPlan, bPlan, ply = ctx.ply)).getOrElse("unclear")
    val openingSeed = bead ^ Math.abs(motifFrame.phase.hashCode) ^ evalOpt.getOrElse(0) ^ motifFrame.motifHash ^ 0x1b873593
    NarrativeLexicon.getOpening(openingSeed, motifFrame.phase, evalText, tactical = motifFrame.highTension, ply = ctx.ply)

  private def appendContextLeadFragments(
      parts: scala.collection.mutable.ListBuffer[AuthorityTaggedFragment],
      ctx: NarrativeContext,
      motifFrame: ContextMotifFrame,
      keyFact: Option[Fact],
      openingPart: String,
      bead: Int
  ): Option[BoardAnchor] =
    val boardAnchor = buildBoardAnchor(ctx, keyFact, bead)
    val endgameFeatures = ctx.semantic.flatMap(_.endgameFeatures)
    val motifPrefix = NarrativeLexicon.getMotifPrefix(bead ^ motifFrame.motifHash, motifFrame.motifPrefixCandidates, ply = ctx.ply)

    boardAnchor.foreach(a => parts += supportFragment(a.text, sceneGrounded = true))
    appendSupportText(parts, endgameFeatures.flatMap(buildEndgameContinuitySentence))
    appendSupportText(parts, endgameFeatures.flatMap(info => buildEndgameCausalitySentence(ctx, info)))
    appendSupportText(parts, motifPrefix)
    Option(openingPart.trim)
      .filter(_.nonEmpty)
      .foreach(text => parts += AuthorityTaggedFragment(text, FragmentAuthority.unsafe_as_truth))
    appendContextThemeFragments(parts, ctx, motifFrame, bead)
    boardAnchor

  private def appendContextThemeFragments(
      parts: scala.collection.mutable.ListBuffer[AuthorityTaggedFragment],
      ctx: NarrativeContext,
      motifFrame: ContextMotifFrame,
      bead: Int
  ): Unit =
    buildThemeKeywordSentence(
      motifs = (motifFrame.derivedContextMotifs ++ motifFrame.deltaMotifs ++ motifFrame.trustedConceptThemeSignals).distinct,
      existingText = rawFragmentText(parts.toList),
      bead = bead ^ 0x6d2b79f5,
      ply = ctx.ply,
      phase = motifFrame.phase
    ).foreach(text => parts += supportFragment(text, sceneGrounded = true, generalized = true))
    buildCanonicalMotifTermSentence(
      motifs = (motifFrame.conceptSummaryMotifs ++ motifFrame.derivedContextMotifs ++ motifFrame.deltaMotifs).distinct,
      existingText = rawFragmentText(parts.toList),
      bead = bead ^ 0x31af9d42,
      ply = ctx.ply,
      phase = motifFrame.phase
    ).foreach(text => parts += supportFragment(text, sceneGrounded = true, generalized = true))

  private def appendRecordedContexts(
      parts: scala.collection.mutable.ListBuffer[AuthorityTaggedFragment],
      concepts: scala.collection.mutable.ListBuffer[String],
      rec: TraceRecorder,
      ctx: NarrativeContext
  ): Unit =
    val recordedContexts: List[(() => Option[String], String, String, String)] =
      List(
        (() => buildOpeningContextSentence(ctx, rawFragmentText(parts.toList)), "openingData", "Context opening", "opening_context"),
        (() => buildStrategicStackContextSentence(ctx, rawFragmentText(parts.toList)), "mainStrategicPlans", "Context strategic stack", "strategic_stack"),
        (() => buildStructuralContextSentence(ctx, rawFragmentText(parts.toList)), "semantic.planAlignment", "Context structure", "structural_context"),
        (() => buildStrategicFlowContextSentence(ctx, rawFragmentText(parts.toList)), "strategicFlow", "Context flow", "strategic_flow"),
        (() => buildOpponentPlanContextSentence(ctx, rawFragmentText(parts.toList)), "opponentPlan", "Context opponent", "opponent_plan"),
        (() => buildMetaContextSentence(ctx, rawFragmentText(parts.toList)), "meta.choiceType", "Context meta", "decision_context")
      )
    recordedContexts.foreach { case (sentence, source, label, concept) =>
      appendRecordedContext(parts, concepts, rec, sentence(), source, label, concept)
    }

  private def trustedContextThemeSignals(
      conceptSummaryMotifs: List[String],
      derivedContextSignals: List[String],
      deltaMotifSignals: List[String],
      keyFact: Option[Fact],
      ctx: NarrativeContext,
      phase: String
  ): List[String] =
    conceptSummaryMotifs
      .map(_.trim)
      .filter(_.nonEmpty)
      .filter(raw =>
        trustedContextThemeSignal(
          raw = raw,
          derivedContextSignals = derivedContextSignals,
          deltaMotifSignals = deltaMotifSignals,
          keyFact = keyFact,
          ctx = ctx,
          phase = phase
        )
      )
      .distinct

  private def trustedContextThemeSignal(
    raw: String,
    derivedContextSignals: List[String],
    deltaMotifSignals: List[String],
    keyFact: Option[Fact],
    ctx: NarrativeContext,
    phase: String
  ): Boolean =
    val motif = normalizeMotifKey(raw)
    motif.nonEmpty &&
      motifPhaseCompatible(motif, phase) &&
      contextThemeHasSupport(motif, derivedContextSignals, deltaMotifSignals, keyFact, ctx)

  private def contextThemeHasSupport(
    motif: String,
    derivedContextSignals: List[String],
    deltaMotifSignals: List[String],
    keyFact: Option[Fact],
    ctx: NarrativeContext
  ): Boolean =
    derivedContextSignals.exists(sig => motifSignalMatches(sig, motif)) ||
      deltaMotifSignals.exists(sig => motifSignalMatches(sig, motif)) ||
      keyFact.exists(f => CommentaryIdeaSurface.motifCorroboratedByFact(motif, f)) ||
      motifCorroboratedByThreat(motif, ctx.threats.toUs) ||
      motifCorroboratedByPawnPlay(motif, ctx.pawnPlay) ||
      motifMatchesAny(motif, StableContextThemeMotifs)

  private def contextMotifPrefixCandidates(
      conceptLeadMotifs: List[String],
      derivedContextMotifs: List[String],
      deltaMotifs: List[String],
      deltaMotifSignals: List[String],
      conceptSummarySignals: List[String],
      derivedContextSignals: List[String],
      keyFact: Option[Fact],
      ctx: NarrativeContext,
      phase: String
  ): List[String] =
    (conceptLeadMotifs ++ derivedContextMotifs ++ deltaMotifs)
      .map(_.trim)
      .filter(_.nonEmpty)
      .filter(m => motifPhaseCompatible(m, phase))
      .distinct
      .filter { raw =>
        isTrustedMotifPrefixCandidate(
          rawMotif = raw,
          deltaSignals = deltaMotifSignals,
          conceptSummarySignals = conceptSummarySignals,
          derivedSignals = derivedContextSignals,
          keyFact = keyFact,
          threatsToUs = ctx.threats.toUs,
          pawnPlay = ctx.pawnPlay,
          phase = phase
        )
      }
      .take(4)

  private def appendContextThreat(
      parts: scala.collection.mutable.ListBuffer[AuthorityTaggedFragment],
      concepts: scala.collection.mutable.ListBuffer[String],
      rec: TraceRecorder,
      ctx: NarrativeContext,
      boardAnchor: Option[BoardAnchor],
      bead: Int
  ): Unit =
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

  private def appendContextPlan(
      parts: scala.collection.mutable.ListBuffer[AuthorityTaggedFragment],
      concepts: scala.collection.mutable.ListBuffer[String],
      rec: TraceRecorder,
      ctx: NarrativeContext,
      bead: Int
  ): Unit =
    topStrategicPlanName(ctx).filter(contextPlanHasSufficientProof(ctx, _)).foreach { planName =>
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

  private def appendContextFact(
      parts: scala.collection.mutable.ListBuffer[AuthorityTaggedFragment],
      ctx: NarrativeContext,
      boardAnchor: Option[BoardAnchor],
      keyFact: Option[Fact],
      bead: Int
  ): Unit =
    if !boardAnchor.exists(_.consumedFact) then
      keyFact.foreach { fact =>
        val factText = NarrativeLexicon.getFactStatement(bead ^ Math.abs(fact.hashCode), fact, ctx)
        if factText.nonEmpty then
          parts += supportFragment(factText, sceneGrounded = true, generalized = true)
      }

  private def appendContextPawnBreak(
      parts: scala.collection.mutable.ListBuffer[AuthorityTaggedFragment],
      concepts: scala.collection.mutable.ListBuffer[String],
      rec: TraceRecorder,
      ctx: NarrativeContext,
      bead: Int
  ): Unit =
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

  private def appendSupportText(
      parts: scala.collection.mutable.ListBuffer[AuthorityTaggedFragment],
      textOpt: Option[String]
  ): Unit =
    textOpt
      .map(_.trim)
      .filter(_.nonEmpty)
      .foreach(text => parts += supportFragment(text, sceneGrounded = true, generalized = true))

  private def appendRecordedContext(
      parts: scala.collection.mutable.ListBuffer[AuthorityTaggedFragment],
      concepts: scala.collection.mutable.ListBuffer[String],
      rec: TraceRecorder,
      textOpt: Option[String],
      source: String,
      label: String,
      concept: String
  ): Unit =
    textOpt.foreach { text =>
      rec.use(source, text, label)
      parts += supportFragment(text, sceneGrounded = true, generalized = true)
      concepts += concept
    }

  private def contextPlanHasSufficientProof(ctx: NarrativeContext, planName: String): Boolean =
    val planKey = normalizeMotifKey(planName)
    val needsTacticalProof =
      List("sacrifice", "mate", "smothered", "trap", "combination").exists(planKey.contains)
    if !needsTacticalProof then true
    else if planKey.contains("sacrifice") then
      ctx.candidates.exists { c =>
        val evidenceLow = c.tacticEvidence.mkString(" ").toLowerCase
        val whyLow = c.whyNot.getOrElse("").toLowerCase
        evidenceLow.contains("sacrifice") ||
          evidenceLow.contains("exchange_sacrifice") ||
          whyLow.contains("sacrifice")
      } || ctx.delta.map(_.newMotifs.mkString(" ").toLowerCase.contains("sacrifice")).getOrElse(false)
    else
      ctx.candidates.exists { c =>
        c.tags.exists(tag => tag == CandidateTag.Sharp || tag == CandidateTag.TacticalGamble) ||
          c.facts.exists(CommentaryIdeaSurface.isTacticalProofFact)
      } ||
        ctx.threats.toUs.exists(t =>
          t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP || t.kind.toLowerCase.contains("mate")
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
        Some(plannerDecisionBeat(ctx, questions, plan, rec))
      case None =>
        plannerInputs.factualFallback.map(decisionFallbackBeat)

  private def plannerDecisionBeat(
    ctx: NarrativeContext,
    questions: List[AuthorQuestion],
    plan: QuestionPlan,
    rec: TraceRecorder
  ): OutlineBeat =
    val renderedClaim = plan.prefixKind.render(plan.claim)
    rec.use(s"planner.primary.${plan.questionId}", renderedClaim, "Decision point")
    OutlineBeat(
      kind = OutlineBeatKind.DecisionPoint,
      text = decisionBeatText(plan, renderedClaim),
      questionIds = List(plan.questionId),
      questionKinds = List(plan.questionKind),
      expectedEvidencePurposes = plan.evidence.map(_.purposes).getOrElse(Nil),
      anchors = decisionBeatAnchors(ctx, questions, plan),
      requiresEvidence = plan.evidence.nonEmpty,
      focusPriority = 96,
      fullGameEssential = true,
      supportKinds = decisionBeatSupportKinds(plan)
    )

  private def decisionBeatText(plan: QuestionPlan, renderedClaim: String): String =
    List(
      Some(renderedClaim),
      plan.contrast.filterNot(contrast => renderedClaim.equalsIgnoreCase(contrast))
    ).flatten.mkString(" ").trim

  private def decisionBeatAnchors(
    ctx: NarrativeContext,
    questions: List[AuthorQuestion],
    plan: QuestionPlan
  ): List[String] =
    questions.find(_.id == plan.questionId).toList.flatMap(_.anchors) ++
      ctx.decision.flatMap(_.focalPoint.map(renderTargetRef)).toList

  private def decisionBeatSupportKinds(plan: QuestionPlan): List[OutlineBeatKind] =
    Option.when(plan.evidence.nonEmpty)(List(OutlineBeatKind.Evidence)).getOrElse(Nil)

  private def decisionFallbackBeat(fallback: String): OutlineBeat =
    OutlineBeat(
      kind = OutlineBeatKind.DecisionPoint,
      text = fallback,
      focusPriority = 96,
      fullGameEssential = true
    )

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
    val mainPlans = ctx.strategicPlanEvidence.mainAdmittedPlanHypotheses
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
    val sentence =
      structuralAlignmentSentence(ctx).orElse {
        ctx.semantic.flatMap(_.preventedPlans.headOption).flatMap(renderStructuralProphylaxisSentence)
      }
    sentence
      .map(ensureSentence)
      .filter(sentenceIsNovel(_, existingText))

  private def structuralAlignmentSentence(ctx: NarrativeContext): Option[String] =
    ctx.semantic.flatMap { semantic =>
      semantic.planAlignment.map { alignment =>
        val base = structuralAlignmentBase(alignment, renderStructureProfileClause(semantic.structureProfile))
        joinNonBlank(List(base, structuralAlignmentCaveat(alignment, base).getOrElse("")))
      }
    }

  private def structuralAlignmentBase(
    alignment: PlanAlignmentInfo,
    structureClause: Option[String]
  ): String =
    val intent = alignment.narrativeIntent.map(compactNarrativeReason).filter(_.nonEmpty)
    def withLead(body: String): String =
      structureClause.map(lead => s"$lead $body").getOrElse(body)
    alignment.band.trim.toLowerCase match
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

  private def structuralAlignmentCaveat(
    alignment: PlanAlignmentInfo,
    base: String
  ): Option[String] =
    val risk = alignment.narrativeRisk.map(compactNarrativeReason).filter(_.nonEmpty)
    val reason = alignment.reasonCodes.flatMap(humanizeAlignmentReason).distinct.take(2).headOption
    risk
      .orElse(reason)
      .filter(text => !base.toLowerCase.contains(text.toLowerCase))
      .map(text => s"The main structural caveat is $text.")

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

  private def buildEvidenceBeat(
    ctx: NarrativeContext,
    questions: List[AuthorQuestion],
    rankedPlans: RankedQuestionPlans,
    rec: TraceRecorder
  ): Option[OutlineBeat] =
    buildPlannerEvidenceBeat(rankedPlans, rec)
      .orElse(if questions.isEmpty then buildEngineEvidenceBeat(ctx, questions, rec) else None)

  private def buildPlannerEvidenceBeat(
      rankedPlans: RankedQuestionPlans,
      rec: TraceRecorder
  ): Option[OutlineBeat] =
    rankedPlans.primary.flatMap { plan =>
      plan.evidence.map { evidence =>
        rec.use("planner.evidence", evidence.purposes.mkString(","), "Evidence from planner")
        OutlineBeat(
          kind = OutlineBeatKind.Evidence,
          text = evidence.text,
          questionIds = List(plan.questionId),
          questionKinds = List(plan.questionKind),
          evidencePurposes = evidence.purposes,
          expectedEvidencePurposes = evidence.purposes,
          evidenceSourceIds = evidence.sourceIds,
          branchScoped = evidence.branchScoped
        )
      }
    }

  private def buildEngineEvidenceBeat(
      ctx: NarrativeContext,
      questions: List[AuthorQuestion],
      rec: TraceRecorder
  ): Option[OutlineBeat] =
    ctx.engineEvidence.flatMap { ev =>
      val variations =
        sortVariationsForSideToMove(ctx.fen, ev.variations)
          .flatMap(v => LineScopedCitation.strategicCitation(ctx.fen, ctx.ply + 1, v).map(citation => v -> citation))
          .take(3)
      Option.when(variations.size >= 2) {
        val labels = List("a)", "b)", "c)")
        val formatted = variations.zip(labels).map { case ((v, citation), label) =>
          s"$label $citation (${formatCp(v.scoreCp)})"
        }
        rec.use("engineEvidence.variations", variations.size.toString, "Evidence fallback from engine PV")
        OutlineBeat(
          kind = OutlineBeatKind.Evidence,
          text = formatted.mkString("\n"),
          evidencePurposes = List("engine_alternatives"),
          expectedEvidencePurposes = questions.flatMap(_.evidencePurposes).distinct
        )
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
    plannerTeachingBeat(rankedPlans).orElse(counterfactualTeachingBeat(ctx, rec))

  private def plannerTeachingBeat(rankedPlans: RankedQuestionPlans): Option[OutlineBeat] =
    rankedPlans.primary.flatMap(_.consequence).filter(_.beat == QuestionPlanConsequenceBeat.TeachingPoint).map { consequence =>
      OutlineBeat(
        kind = OutlineBeatKind.TeachingPoint,
        text = consequence.text,
        conceptIds = List("planner_consequence"),
        focusPriority = 82
      )
    }

  private def counterfactualTeachingBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder
  ): Option[OutlineBeat] =
    ctx.counterfactual.flatMap { cf =>
      val motifOpt = counterfactualTeachingMotif(cf)
      if !shouldShowCounterfactualTeaching(cf, motifOpt) then None
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

  private def counterfactualTeachingMotif(
    cf: lila.commentary.model.strategic.CounterfactualMatch
  ): Option[Motif] =
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

  private def shouldShowCounterfactualTeaching(
    cf: lila.commentary.model.strategic.CounterfactualMatch,
    motifOpt: Option[Motif]
  ): Boolean =
    val hasTacticalTheme = motifOpt.exists(_.category == MotifCategory.Tactical)
    cf.cpLoss >= 50 && (hasTacticalTheme || cf.cpLoss >= 150)

  private def buildMainMoveBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    isAnnotation: Boolean,
    bead: Int,
    precedentTextOpt: Option[String],
    crossBeatState: CrossBeatRepetitionState
  ): OutlineBeat =
    if isAnnotation then
      buildAnnotationMainMoveBeat(ctx, rec, bead, precedentTextOpt, crossBeatState)
    else
      buildCandidateMainMoveBeat(ctx, rec, bead, precedentTextOpt, crossBeatState)

  private def buildAnnotationMainMoveBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    bead: Int,
    precedentTextOpt: Option[String],
    crossBeatState: CrossBeatRepetitionState
  ): OutlineBeat =
    val material = buildAnnotationMainMoveMaterial(ctx)
    val text = annotationMainMoveText(ctx, rec, material, bead, precedentTextOpt, crossBeatState)
    trackAnnotationMainMoveText(text, crossBeatState)

    OutlineBeat(
      kind = OutlineBeatKind.MainMove,
      text = text,
      anchors = List(material.playedSan, material.bestSan).filter(_.nonEmpty).distinct,
      focusPriority = 92,
      fullGameEssential = true
    )

  private def annotationMainMoveText(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    material: AnnotationMainMoveMaterial,
    bead: Int,
    precedentTextOpt: Option[String],
    crossBeatState: CrossBeatRepetitionState
  ): String =
    val rawText = annotationRawMainMoveText(ctx, rec, material, bead, precedentTextOpt, crossBeatState)
    val practicalText = buildPracticalMainMoveSentence(ctx, rawText).getOrElse("")
    val enrichedRawText = joinNonBlank(List(rawText, practicalText))
    projectAnnotationMainMoveText(enrichedRawText, material, bead).renderedText

  private def annotationRawMainMoveText(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    material: AnnotationMainMoveMaterial,
    bead: Int,
    precedentTextOpt: Option[String],
    crossBeatState: CrossBeatRepetitionState
  ): String =
    joinNonBlank(
      List(
        annotationBaseText(material, bead),
        annotationDetailText(ctx, material, bead, crossBeatState).getOrElse("")
      ) ++ annotationSupportText(ctx, rec, material, bead, precedentTextOpt, crossBeatState)
    )

  private def projectAnnotationMainMoveText(
    rawText: String,
    material: AnnotationMainMoveMaterial,
    bead: Int
  ): AnnotationTextProjection =
    val tonedText =
      harmonizeAnnotationTone(
        rawText,
        material.cpLoss,
        material.isConstructive,
        bead ^ Math.abs(material.playedSan.hashCode)
      )
    enforceAnnotationPolarity(
      tonedText,
      material.cpLoss,
      material.isConstructive,
      bead ^ Math.abs(material.bestSan.hashCode)
    )

  private def trackAnnotationMainMoveText(text: String, crossBeatState: CrossBeatRepetitionState): Unit =
    if text.trim.nonEmpty then trackTemplateUsage(text, crossBeatState.usedStems, crossBeatState.prefixCounts)

  private def buildAnnotationMainMoveMaterial(ctx: NarrativeContext): AnnotationMainMoveMaterial =
    val playedSan = ctx.playedSan.getOrElse("")
    val playedUci = normalizeMoveUci(ctx.playedMove)
    val truthContract = annotationTruthContract(ctx)
    val best = annotationBestProjection(ctx, truthContract)
    val playedRank = annotationPlayedRank(ctx, truthContract, playedUci, playedSan)
    AnnotationMainMoveMaterial(
      playedSan = playedSan,
      playedUci = playedUci,
      truthContract = truthContract,
      bestSan = best.san,
      bestUci = best.uci,
      playedRank = playedRank,
      cpLoss = resolveCpLoss(ctx, playedUci, playedSan, playedRank),
      playedCand = playedUci.flatMap(uci => candidateMatchingUci(ctx, uci)),
      bestCand = best.candidate,
      isBest = truthContract.chosenMatchesBest,
      isInvestment = truthContract.isInvestment
    )

  private def annotationTruthContract(ctx: NarrativeContext): DecisiveTruthContract =
    DecisiveTruth.derive(
      ctx = ctx,
      comparisonOverride = DecisionComparisonBuilder.build(ctx)
    )

  private def annotationBestProjection(
    ctx: NarrativeContext,
    truthContract: DecisiveTruthContract
  ): AnnotationBestProjection =
    val engineBest = annotationEngineBest(ctx)
    val san =
      truthContract.verifiedBestMove
        .orElse(engineBest.san)
        .orElse(engineBest.candidate.map(_.move).filter(_.trim.nonEmpty))
        .getOrElse("")
    val uci =
      Option.when(truthContract.allowConcreteBenchmark || truthContract.chosenMatchesBest) {
        engineBest.uci.orElse(normalizeMoveUci(engineBest.candidate.flatMap(_.uci)))
      }.flatten
    AnnotationBestProjection(san = san, uci = uci, candidate = engineBest.candidate)

  private def annotationEngineBest(ctx: NarrativeContext): AnnotationEngineBest =
    val variation = rankedEngineVariations(ctx).headOption
    val uci = normalizeMoveUci(variation.flatMap(_.moves.headOption))
    val san = variation
      .flatMap(_.ourMove.map(_.san))
      .filter(_.trim.nonEmpty)
      .orElse(uci.map(move => NarrativeUtils.uciToSanOrFormat(ctx.fen, move)).filter(_.trim.nonEmpty))
    val candidate =
      uci
        .flatMap(move => candidateMatchingUci(ctx, move))
        .orElse(ctx.candidates.headOption)
    AnnotationEngineBest(uci = uci, san = san, candidate = candidate)

  private def annotationPlayedRank(
    ctx: NarrativeContext,
    truthContract: DecisiveTruthContract,
    playedUci: Option[String],
    playedSan: String
  ): Option[Int] =
    if truthContract.allowConcreteBenchmark then playedMoveRank(ctx, playedUci, playedSan)
    else Option.when(truthContract.chosenMatchesBest)(1)

  private def annotationBaseText(
    material: AnnotationMainMoveMaterial,
    bead: Int
  ): String =
    if material.isInvestment then NarrativeLexicon.getAnnotationInvestment(bead, material.playedSan)
    else if material.isBest then NarrativeLexicon.getAnnotationPositive(bead, material.playedSan)
    else if material.truthContract.allowConcreteBenchmark then
      NarrativeLexicon.getAnnotationNegative(bead, material.playedSan, material.bestSan, material.truthContract.cpLoss)
    else
      NarrativeLexicon.getAnnotationNegativeWithoutBenchmark(bead, material.playedSan, material.truthContract.cpLoss)

  private def annotationDetailText(
    ctx: NarrativeContext,
    material: AnnotationMainMoveMaterial,
    bead: Int,
    crossBeatState: CrossBeatRepetitionState
  ): Option[String] =
    if material.isInvestment then
      buildInvestmentAnnotationDetail(ctx, material.truthContract)
    else if material.isBest then
      buildPositiveAnnotationDetail(ctx, material.playedSan, material.playedCand, material.bestCand, bead)
    else
      buildNegativeAnnotationDetail(ctx, material, bead, crossBeatState)

  private def annotationSupportText(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    material: AnnotationMainMoveMaterial,
    bead: Int,
    precedentTextOpt: Option[String],
    crossBeatState: CrossBeatRepetitionState
  ): List[String] =
    List(
      annotationAlternativeSupportText(ctx),
      annotationHypothesisSupportText(ctx, material, bead, crossBeatState),
      buildDeltaAfterMoveText(ctx, bead).getOrElse(""),
      annotationPrecedentSupportText(ctx, rec, precedentTextOpt, bead)
    )

  private def annotationAlternativeSupportText(ctx: NarrativeContext): String =
    AlternativeNarrativeSupport
      .sentence(ctx)
      .map(ensureSentence)
      .filter(_.nonEmpty)
      .getOrElse("")

  private def annotationHypothesisSupportText(
    ctx: NarrativeContext,
    material: AnnotationMainMoveMaterial,
    bead: Int,
    crossBeatState: CrossBeatRepetitionState
  ): String =
    buildMainHypothesisNarrative(
      ctx = ctx,
      focusCandidate = material.playedCand.orElse(material.bestCand),
      supportCandidate = material.bestCand.filter(b => !material.playedCand.contains(b)).orElse(ctx.candidates.lift(1)),
      bead = bead ^ Math.abs(material.playedSan.hashCode) ^ 0x7f4a7c15,
      crossBeatState = crossBeatState
    ).getOrElse("")

  private def annotationPrecedentSupportText(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    precedentTextOpt: Option[String],
    bead: Int
  ): String =
    mainPrecedentText(
      ctx = ctx,
      rec = rec,
      precedentTextOpt = precedentTextOpt,
      fallbackSeed = bead ^ 0x56f839d3,
      recordWhenDefined = false,
      fallbackWhenBlank = true
    )

  private def buildPositiveAnnotationDetail(
    ctx: NarrativeContext,
    playedSan: String,
    playedCand: Option[CandidateInfo],
    bestCand: Option[CandidateInfo],
    bead: Int
  ): Option[String] =
    playedCand.flatMap { c =>
      val b = bead ^ Math.abs(c.move.hashCode)
      val isTerminal = isTerminalAnnotationMove(ctx, playedSan, bestCand)
      val hintFragments =
        annotationHintFragments(b, c.tags, c.practicalDifficulty, c.move, ctx.phase.current, isTerminal)
      val combined =
        joinNonBlank(
          List(positiveAnnotationIntentSentence(ctx, c, b)) ++
            hintFragments.renderedHints ++
            positiveAnnotationNotes(c)
        )
      Option.when(combined.nonEmpty)(combined)
    }

  private def positiveAnnotationIntentSentence(ctx: NarrativeContext, candidate: CandidateInfo, bead: Int): String =
    val evidenceOpt = candidate.tacticEvidence.headOption.map(s => s.substring(0, 1).toLowerCase + s.substring(1))
    val intent = NarrativeLexicon.getIntent(bead, preferredIntent(candidate), evidenceOpt, ply = ctx.ply)
    if intent.nonEmpty then s"It $intent." else ""

  private def positiveAnnotationNotes(candidate: CandidateInfo): List[String] =
    val alert = candidate.tacticalAlert.map(_.trim).filter(_.nonEmpty).map(a => s"Note: $a.").getOrElse("")
    val alignmentNote = candidate.alignmentBand.map(_.trim.toLowerCase) match
      case Some("offplan") => "Structure note: this route is playable only with precise follow-up."
      case Some("unknown") => "Structure note: verify tactical details before committing."
      case Some("onbook") => "Structure note: this keeps the strategic structure coherent."
      case _ => ""
    List(alert, alignmentNote)

  private def buildNegativeAnnotationDetail(
    ctx: NarrativeContext,
    material: AnnotationMainMoveMaterial,
    bead: Int,
    crossBeatState: CrossBeatRepetitionState
  ): Option[String] =
    val b = bead ^ Math.abs(material.playedSan.hashCode)
    val combined = composeCausalAnnotation(
      rankContext = negativeAnnotationRankContext(material.truthContract, material.playedRank, material.bestSan, b),
      reason = negativeAnnotationIssue(ctx, material, b),
      bestIntent = negativeAnnotationBestIntent(ctx, material.truthContract, material.bestCand, material.bestSan, b),
      bead = b ^ 0x3f84d5b5,
      tacticalEmphasis = shouldUseNegativeAnnotationTacticalEmphasis(ctx, material.cpLoss),
      usedStems = crossBeatState.usedStems.toSet,
      prefixCounts = crossBeatState.prefixCounts.toMap
    )
    Option.when(combined.nonEmpty)(combined)

  private def negativeAnnotationBestIntent(
      ctx: NarrativeContext,
      truthContract: DecisiveTruthContract,
      bestCand: Option[CandidateInfo],
      bestSan: String,
      bead: Int
  ): String =
    if truthContract.allowConcreteBenchmark then
      bestCand.map { c =>
        val evidenceOpt = c.tacticEvidence.headOption.map(s => s.substring(0, 1).toLowerCase + s.substring(1))
        val intent = NarrativeLexicon.getIntent(bead ^ Math.abs(c.move.hashCode), preferredIntent(c), evidenceOpt, ply = ctx.ply + 1)
        if intent.nonEmpty then s"Better is **$bestSan**; it $intent."
        else s"Better is **$bestSan** to keep tighter control of the position."
      }.getOrElse {
        if bestSan.nonEmpty then s"Better is **$bestSan** to keep tighter control of the position."
        else ""
      }
    else ""

  private def negativeAnnotationRankContext(
      truthContract: DecisiveTruthContract,
      playedRank: Option[Int],
      bestSan: String,
      bead: Int
  ): Option[String] =
    Option.when(truthContract.allowConcreteBenchmark) {
      NarrativeLexicon.getEngineRankContext(
        bead = bead ^ 0x27d4eb2f,
        rank = playedRank,
        bestSan = bestSan,
        cpLoss = truthContract.cpLoss
      )
    }.flatten

  private def negativeAnnotationIssue(
      ctx: NarrativeContext,
      material: AnnotationMainMoveMaterial,
      bead: Int
  ): String =
    val playedSan = material.playedSan
    buildConcreteAnnotationIssue(
      ConcreteAnnotationIssueInput(
        bead = bead ^ 0x6d2b79f5,
        playedSan = playedSan,
        playedUci = material.playedUci,
        bestSan = material.bestSan,
        bestUci = material.bestUci,
        cpLoss = material.truthContract.cpLoss,
        playedRank = material.playedRank,
        missedMotif = None,
        whyNot = material.playedCand.flatMap(candidateWhyNotObservation),
        alert = None,
        playedCand = material.playedCand,
        bestReply = None,
        threatsToUs = ctx.threats.toUs,
        contextHint = Math.abs(playedSan.hashCode),
        currentPly = ctx.ply,
        allowConcreteBenchmark = material.truthContract.allowConcreteBenchmark
      )
    )

  private def shouldUseNegativeAnnotationTacticalEmphasis(ctx: NarrativeContext, cpLoss: Int): Boolean =
    CriticalAnnotationPolicy.shouldUseTacticalEmphasis(
      ctx = ctx,
      cpLoss = cpLoss,
      missedMotifPresent = false,
      hasForcingReply = false
    )

  private def candidateMatchingUci(ctx: NarrativeContext, uci: String): Option[CandidateInfo] =
    ctx.candidates.find(_.uci.exists(cu => NarrativeUtils.uciEquivalent(cu, uci)))

  private def buildCandidateMainMoveBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    bead: Int,
    precedentTextOpt: Option[String],
    crossBeatState: CrossBeatRepetitionState
  ): OutlineBeat =
    ctx.candidates.headOption.map { main =>
      rec.use("candidates[0]", main.move, "Main move")
      val text = candidateMainFlowText(ctx, main, bead)
      val mergedText =
        joinNonBlank(
          text :: candidateMainMoveSupportText(ctx, rec, main, text, bead, precedentTextOpt, crossBeatState)
        )
      if mergedText.trim.nonEmpty then trackTemplateUsage(mergedText, crossBeatState.usedStems, crossBeatState.prefixCounts)

      OutlineBeat(
        kind = OutlineBeatKind.MainMove,
        text = mergedText,
        anchors = List(main.move),
        focusPriority = 92,
        fullGameEssential = true
      )
    }.getOrElse(OutlineBeat(OutlineBeatKind.MainMove, ""))

  private def candidateMainFlowText(
      ctx: NarrativeContext,
      main: CandidateInfo,
      bead: Int
  ): String =
    val evidenceOpt = main.tacticEvidence.headOption.map(s => s.substring(0, 1).toLowerCase + s.substring(1))
    val intent = NarrativeLexicon.getIntent(bead, preferredIntent(main), evidenceOpt, ply = ctx.ply)
    val engineBest = rankedEngineVariations(ctx).headOption.orElse(ctx.engineEvidence.flatMap(_.best))
    val evalScore = engineBest.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)).getOrElse(0)
    val evalTerm = NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x85ebca6b, evalScore, ply = ctx.ply)
    val replySan = engineBest.flatMap(v => variationReplySan(ctx.fen, v))
    val consequence = engineBest.flatMap(variationConsequenceClause).getOrElse("")
    NarrativeLexicon.getMainFlow(
      bead = bead,
      move = main.move,
      annotation = main.annotation,
      intent = intent,
      replySan = replySan,
      sampleRest = None,
      evalTerm = evalTerm,
      consequence = consequence
    )

  private def candidateMainMoveSupportText(
      ctx: NarrativeContext,
      rec: TraceRecorder,
      main: CandidateInfo,
      mainText: String,
      bead: Int,
      precedentTextOpt: Option[String],
      crossBeatState: CrossBeatRepetitionState
  ): List[String] =
    val hypothesisText = candidateMainHypothesisText(ctx, main, bead, crossBeatState)
    val precedentText =
      mainPrecedentText(
        ctx = ctx,
        rec = rec,
        precedentTextOpt = precedentTextOpt,
        fallbackSeed = bead ^ 0x4f6cdd1d,
        recordWhenDefined = true,
        fallbackWhenBlank = false
      )
    List(
      hypothesisText,
      buildProphylaxisMainMoveSentence(ctx, bead).getOrElse(""),
      buildPracticalMainMoveSentence(ctx, mainText).getOrElse(""),
      precedentText
    )

  private def candidateMainHypothesisText(
    ctx: NarrativeContext,
    main: CandidateInfo,
    bead: Int,
    crossBeatState: CrossBeatRepetitionState
  ): String =
    buildMainHypothesisNarrative(
      ctx = ctx,
      focusCandidate = Some(main),
      supportCandidate = ctx.candidates.lift(1),
      bead = bead ^ Math.abs(main.move.hashCode) ^ 0x4f6cdd1d,
      crossBeatState = crossBeatState
    ).getOrElse("")

  private def mainPrecedentText(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    precedentTextOpt: Option[String],
    fallbackSeed: Int,
    recordWhenDefined: Boolean,
    fallbackWhenBlank: Boolean
  ): String =
    val precedentText = precedentTextOpt.getOrElse("")
    val hasPrecedent = if fallbackWhenBlank then precedentText.nonEmpty else precedentTextOpt.nonEmpty
    val shouldRecord = if recordWhenDefined then precedentTextOpt.nonEmpty else precedentText.nonEmpty
    if shouldRecord then
      rec.use("openingData.sampleGames", "1", "Move-level precedent")
    val fallbackText =
      if hasPrecedent || !ctx.openingData.exists(_.sampleGames.isEmpty) || !ctx.openingEvent.exists(isCoreOpeningEvent) then ""
      else buildPrecedentFallbackSentence(ctx, fallbackSeed, scope = "main").getOrElse("")
    joinNonBlank(List(fallbackText, precedentText))

  private def buildOpeningTheoryBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    suppressPrecedents: Boolean
  ): Option[OutlineBeat] =
    val openingRef = ctx.openingData
    val openingText = openingTheoryReferenceText(ctx, rec, openingRef)
    val precedentFragments = openingTheoryPrecedents(ctx, rec, openingRef, suppressPrecedents)
    val branchSummary = openingTheoryBranchSummary(ctx, rec, openingRef, suppressPrecedents)
    val branchRelation = openingTheoryBranchRelation(ctx, rec, openingRef, suppressPrecedents)
    val precedentBridge = openingTheoryPrecedentBridge(ctx, openingRef, openingText, precedentFragments)
    openingTheoryBeat(
      openingRef = openingRef,
      precedentFragments = precedentFragments,
      text = openingTheoryText(openingText, branchSummary, branchRelation, precedentBridge, precedentFragments)
    )

  private def openingTheoryReferenceText(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    openingRef: Option[OpeningReference]
  ): Option[String] =
    openingRef.filter(_.totalGames >= 5).flatMap { ref =>
      ref.name.map { name =>
        rec.use("openingData", name, "Opening theory")
        NarrativeLexicon.getOpeningReference(Math.abs(ctx.hashCode), name, ref.totalGames, 0.5)
      }
    }

  private def openingTheoryPrecedents(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    openingRef: Option[OpeningReference],
    suppressPrecedents: Boolean
  ): OpeningPrecedentFragments =
    val fragments =
      if suppressPrecedents then OpeningPrecedentFragments(Nil)
      else buildOpeningPrecedentSnippets(ctx, openingRef, Math.abs(ctx.hashCode) ^ 0x4b1d0f6a)
    if fragments.nonEmpty then
      rec.use("openingData.sampleGames", fragments.fragmentCount.toString, "Opening precedents")
    fragments

  private def openingTheoryBranchSummary(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    openingRef: Option[OpeningReference],
    suppressPrecedents: Boolean
  ): Option[String] =
    val summary =
      Option.when(!suppressPrecedents) {
        OpeningPrecedentBranching.summarySentence(ctx, openingRef, requireFocus = false)
      }.flatten
    summary.foreach(_ => rec.use("openingData.sampleGames", "branch", "Opening branch summary"))
    summary

  private def openingTheoryBranchRelation(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    openingRef: Option[OpeningReference],
    suppressPrecedents: Boolean
  ): Option[String] =
    val relation =
      Option.when(!suppressPrecedents) {
        OpeningPrecedentBranching.relationSentence(ctx, openingRef, requireFocus = false)
      }.flatten
    relation.foreach(_ => rec.use("openingData.sampleGames", "relation", "Opening branch relation"))
    relation

  private def openingTheoryPrecedentBridge(
    ctx: NarrativeContext,
    openingRef: Option[OpeningReference],
    openingText: Option[String],
    precedentFragments: OpeningPrecedentFragments
  ): String =
    if precedentFragments.nonEmpty || openingText.isEmpty || !openingRef.exists(_.sampleGames.isEmpty) then ""
    else buildPrecedentFallbackSentence(ctx, Math.abs(ctx.hashCode) ^ 0x19f8b4ad, scope = "opening").getOrElse("")

  private def openingTheoryText(
    openingText: Option[String],
    branchSummary: Option[String],
    branchRelation: Option[String],
    precedentBridge: String,
    precedentFragments: OpeningPrecedentFragments
  ): String =
    List(
      openingText.getOrElse(""),
      branchSummary.getOrElse(""),
      branchRelation.getOrElse(""),
      precedentBridge,
      precedentFragments.renderedText
    )
      .filter(_.trim.nonEmpty)
      .mkString(" ")
      .trim

  private def openingTheoryBeat(
    openingRef: Option[OpeningReference],
    precedentFragments: OpeningPrecedentFragments,
    text: String
  ): Option[OutlineBeat] =
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
    val rendered =
      NarrativeLexicon.pick(
        bead ^ 0x2f6e2b1,
        precedentFallbackTemplates(scope, precedentFallbackPlanHint(ctx))
      )
    val withEval = appendOpeningPrecedentFallbackEval(ctx, scope, rendered, bead)
    Option.when(withEval.trim.nonEmpty)(withEval.trim)

  private def precedentFallbackPlanHint(ctx: NarrativeContext): Option[String] =
    topStrategicPlanName(ctx)
      .map(_.replaceAll("""[_\-]+""", " ").trim.toLowerCase)
      .filter(_.nonEmpty)

  private def precedentFallbackTemplates(scope: String, planHint: Option[String]): List[String] =
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

  private def appendOpeningPrecedentFallbackEval(
    ctx: NarrativeContext,
    scope: String,
    rendered: String,
    bead: Int
  ): String =
    val evalCp =
      rankedEngineVariations(ctx).headOption.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp))
    evalCp
      .map(cp => NarrativeLexicon.evalOutcomeClauseFromCp(bead ^ 0x7f4a7c15, cp, ply = ctx.ply))
      .filter(_ => scope.trim.equalsIgnoreCase("opening"))
      .map(eval => s"$rendered $eval.")
      .getOrElse(rendered)

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

  private def buildPrecedentComparisonFragments(
    lines: List[OpeningPrecedentLine],
    bead: Int
  ): PrecedentComparisonFragments =
    val ranked = lines.take(MaxOpeningPrecedents)
    val rankedWithSignals =
      ranked.map { line =>
        line -> buildPrecedentSignal(line).filter(_.confidence >= PrecedentConfidenceThreshold)
      }
    val header = precedentComparisonHeader(bead)
    val renderState = PrecedentComparisonRenderState(
      usedStems = scala.collection.mutable.HashSet.empty[String],
      prefixCounts = scala.collection.mutable.HashMap.empty[String, Int].withDefaultValue(0),
      seenSequenceKeys = scala.collection.mutable.HashSet.empty[String],
      mechanismUseCounts = scala.collection.mutable.HashMap.empty[OpeningBranchMechanism, Int].withDefaultValue(0)
    )
    val items = buildPrecedentComparisonItems(rankedWithSignals, bead, renderState)
    val (summary, sharedLesson) = selectTrackedPrecedentComparisonSummary(rankedWithSignals, bead, renderState)

    PrecedentComparisonFragments(
      header = header,
      items = items,
      summary = summary,
      sharedLesson = sharedLesson
    )

  private def precedentComparisonHeader(bead: Int): AuthorityTaggedFragment =
    supportFragment(
      text = NarrativeLexicon.pick(bead ^ 0x57f1a235, List(
        "Comparable master branches from this split:",
        "At this branch, master games diverged in three practical directions:",
        "Reference branches from elite games at this point:"
      )),
      evidenceBacked = true
    )

  private def buildPrecedentComparisonItems(
    rankedWithSignals: List[(OpeningPrecedentLine, Option[PrecedentSignal])],
    bead: Int,
    renderState: PrecedentComparisonRenderState
  ): List[AuthorityTaggedFragment] =
    rankedWithSignals.zipWithIndex.map { case ((line, signal), idx) =>
      buildPrecedentComparisonItem(line, signal, idx, bead, renderState)
    }

  private def buildPrecedentComparisonItem(
    line: OpeningPrecedentLine,
    signal: Option[PrecedentSignal],
    idx: Int,
    bead: Int,
    renderState: PrecedentComparisonRenderState
  ): AuthorityTaggedFragment =
    val label = ('A' + idx).toChar
    val itemSeed = bead ^ Math.abs(line.text.hashCode) ^ ((idx + 1) * 0x9e3779b9)
    val role = precedentRoleForIndex(idx)
    val roleLine = precedentComparisonRoleLine(role, signal, itemSeed, renderState)
    val parts =
      List(
        precedentComparisonBaseLineText(line, itemSeed, renderState),
        roleLine
      ).filter(_.nonEmpty)
    supportFragment(
      text = s"$label) ${parts.mkString(" ")}",
      evidenceBacked = true
    )

  private def precedentComparisonBaseLineText(
    line: OpeningPrecedentLine,
    itemSeed: Int,
    renderState: PrecedentComparisonRenderState
  ): String =
    val hasRepeatedSequence =
      line.sequenceKey.nonEmpty && renderState.seenSequenceKeys.contains(line.sequenceKey)
    if line.sequenceKey.nonEmpty then renderState.seenSequenceKeys += line.sequenceKey
    if hasRepeatedSequence then
      formatOpeningPrecedentRepeatedSnippet(line.game, itemSeed).getOrElse(line.text).trim
    else line.text.trim

  private def precedentRoleForIndex(idx: Int): PrecedentRole =
    idx match
      case 0 => PrecedentRole.Sequence
      case 1 => PrecedentRole.StrategicTransition
      case _ => PrecedentRole.DecisionDriver

  private def precedentComparisonRoleLine(
    role: PrecedentRole,
    signal: Option[PrecedentSignal],
    itemSeed: Int,
    renderState: PrecedentComparisonRenderState
  ): String =
    val roleTemplates = signal.toList.flatMap { s =>
      precedentRoleTemplates(role, s, itemSeed, renderState.mechanismUseCounts)
    }.map(_.trim).filter(_.nonEmpty).distinct
    if roleTemplates.isEmpty then ""
    else
      val selected = selectNonRepeatingTemplate(
        templates = roleTemplates,
        seed = itemSeed ^ 0x4f1bbcdc,
        usedStems = renderState.usedStems.toSet,
        prefixCounts = renderState.prefixCounts.toMap,
        prefixLimits = PrefixFamilyLimits
      )
      trackTemplateUsage(selected, renderState.usedStems, renderState.prefixCounts)
      selected

  private def selectTrackedPrecedentComparisonSummary(
    rankedWithSignals: List[(OpeningPrecedentLine, Option[PrecedentSignal])],
    bead: Int,
    renderState: PrecedentComparisonRenderState
  ): (Option[AuthorityTaggedFragment], Option[AuthorityTaggedFragment]) =
    val summaryFamilies =
      buildPrecedentComparisonSummaryFamilies(
        rankedWithSignals.flatMap(_._2.map(_.mechanism))
      )
    val (summary, sharedLesson) =
      selectPrecedentComparisonSummaryFragments(
        families = summaryFamilies,
        seed = bead ^ 0x3c6ef372,
        usedStems = renderState.usedStems.toSet,
        prefixCounts = renderState.prefixCounts.toMap,
        prefixLimits = PrefixFamilyLimits
      )
    summary.foreach(fragment => trackTemplateUsage(fragment.rawText, renderState.usedStems, renderState.prefixCounts))
    sharedLesson.foreach(fragment => trackTemplateUsage(fragment.rawText, renderState.usedStems, renderState.prefixCounts))
    (summary, sharedLesson)

  private def precedentRoleTemplates(
    role: PrecedentRole,
    signal: PrecedentSignal,
    itemSeed: Int,
    mechanismUseCounts: scala.collection.mutable.Map[OpeningBranchMechanism, Int]
  ): List[String] =
    role match
      case PrecedentRole.Sequence =>
        PrecedentTemplateSteps.map { step =>
          NarrativeLexicon.getPrecedentRouteLine(
            bead = itemSeed ^ (step * 0x27d4eb2f),
            triggerMove = signal.triggerMove,
            replyMove = signal.replyMove,
            pivotMove = signal.pivotMove
          )
        }
      case PrecedentRole.StrategicTransition =>
        val mechanismLabel = nextPrecedentMechanismLabel(signal, itemSeed ^ 0x7f4a7c15, mechanismUseCounts)
        PrecedentTemplateSteps.map { step =>
          NarrativeLexicon.getPrecedentStrategicTransitionLine(
            bead = itemSeed ^ (step * 0x7f4a7c15),
            mechanism = mechanismLabel
          )
        }
      case PrecedentRole.DecisionDriver =>
        val mechanismLabel = nextPrecedentMechanismLabel(signal, itemSeed ^ 0x6d2b79f5, mechanismUseCounts)
        PrecedentTemplateSteps.map { step =>
          NarrativeLexicon.getPrecedentDecisionDriverLine(
            bead = itemSeed ^ (step * 0x6d2b79f5),
            mechanism = mechanismLabel
          )
        }

  private def nextPrecedentMechanismLabel(
    signal: PrecedentSignal,
    seed: Int,
    mechanismUseCounts: scala.collection.mutable.Map[OpeningBranchMechanism, Int]
  ): String =
    val occurrence = mechanismUseCounts(signal.mechanism)
    mechanismUseCounts.update(signal.mechanism, occurrence + 1)
    precedentMechanismLabel(signal.mechanism, seed, occurrence)

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
      if grouped.size >= 2 then diversifiedPrecedentSummaryFamilies(dominantLabel)
      else focusedPrecedentSummaryFamilies(dominantLabel)

  private def diversifiedPrecedentSummaryFamilies(dominantLabel: String): PrecedentComparisonSummaryFamilies =
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

  private def focusedPrecedentSummaryFamilies(dominantLabel: String): PrecedentComparisonSummaryFamilies =
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
    line.game.pgn.flatMap(precedentMoveFeatures).map { features =>
      PrecedentSignal(
        triggerMove = features.triggerMove,
        replyMove = features.replyMove,
        pivotMove = features.pivotMove,
        mechanism = OpeningPrecedentBranching.inferMechanismFromSanMoves(features.sanMoves),
        confidence = precedentSignalConfidence(line, features)
      )
    }

  private def precedentMoveFeatures(rawPgn: String): Option[PrecedentMoveFeatures] =
    val sanMoves = OpeningPrecedentBranching.precedentSanMoves(Some(rawPgn))
    sanMoves.headOption.map { trigger =>
      val captures = sanMoves.count(_.contains("x"))
      val checks = sanMoves.count(m => m.contains("+") || m.contains("#"))
      val promotions = sanMoves.count(_.contains("="))
      val forcingDensity =
        if sanMoves.nonEmpty then (captures + checks + promotions).toDouble / sanMoves.size.toDouble
        else 0.0
      PrecedentMoveFeatures(
        sanMoves = sanMoves,
        triggerMove = trigger,
        replyMove = sanMoves.lift(1),
        pivotMove = sanMoves.lift(2),
        captures = captures,
        checks = checks,
        promotions = promotions,
        pawnPushes = sanMoves.count(isLikelyPawnMove),
        pieceMoves = sanMoves.count(isPieceMove),
        forcingDensity = forcingDensity
      )
    }

  private def precedentSignalConfidence(line: OpeningPrecedentLine, features: PrecedentMoveFeatures): Double =
    val overlapConfidence = line.overlap match
      case n if n >= 3 => 1.0
      case 2           => 0.85
      case 1           => 0.65
      case _           => 0.35
    val metadataConfidence = (line.metadataScore.toDouble / 12.0).min(1.0)
    val sequenceConfidence =
      if features.sanMoves.size >= 6 then 1.0
      else if features.sanMoves.size >= 4 then 0.75
      else 0.55
    val confidence =
      (0.40 * overlapConfidence) +
        (0.25 * metadataConfidence) +
        (0.20 * sequenceConfidence) +
        (0.15 * precedentSignalDominance(features))
    confidence.max(0.0).min(1.0)

  private def precedentSignalDominance(features: PrecedentMoveFeatures): Double =
    val sortedScores = precedentMechanismScores(features).values.toList.sorted(using Ordering[Int].reverse)
    sortedScores match
      case top :: second :: _ => ((top - second).max(0).min(3)).toDouble / 3.0
      case top :: Nil         => (top.min(3)).toDouble / 3.0
      case _                  => 0.0

  private def precedentMechanismScores(features: PrecedentMoveFeatures): Map[OpeningBranchMechanism, Int] =
    Map(
      OpeningBranchMechanism.TacticalPressure ->
        (features.checks * 2 + features.captures + Option.when(features.forcingDensity >= 0.45)(1).getOrElse(0)),
      OpeningBranchMechanism.ExchangeCascade ->
        (features.captures * 2 + Option.when(features.captures >= 2)(2).getOrElse(0) + Option.when(features.pieceMoves >= 2)(1).getOrElse(0)),
      OpeningBranchMechanism.PromotionRace ->
        (features.promotions * 3 + Option.when(features.captures >= 1)(1).getOrElse(0) + Option.when(features.checks >= 1)(1).getOrElse(0)),
      OpeningBranchMechanism.StructuralTransformation ->
        (features.pawnPushes * 2 + Option.when(features.captures <= 1)(1).getOrElse(0) + Option.when(features.pieceMoves >= 1)(1).getOrElse(0)),
      OpeningBranchMechanism.InitiativeSwing ->
        (features.pieceMoves + Option.when(features.captures == 1)(1).getOrElse(0) + Option.when(features.checks == 0)(1).getOrElse(0))
    )

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
    LikelyPawnMovePattern.matches(Option(move).getOrElse("").trim)

  private def isPieceMove(move: String): Boolean =
    Option(move).getOrElse("").headOption.exists(ch => "KQRBN".contains(ch))

  private def buildAlternativesBeat(
    ctx: NarrativeContext,
    rec: TraceRecorder,
    bead: Int,
    crossBeatState: CrossBeatRepetitionState
  ): OutlineBeat =
    val alts = alternativeCandidates(ctx)
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

    val bestAttempt =
      (0 until 3).toList
        .map(pass => renderAlternativeAttempt(alts, signals, mainCandidate, bead, pass, crossBeatState))
        .minBy(_.penalty)
    applyAlternativeAttempt(bestAttempt, crossBeatState)
    val lines = bestAttempt.lines
    OutlineBeat(kind = OutlineBeatKind.Alternatives, text = lines.mkString("\n"), anchors = alts.map(_.move))

  private def alternativeCandidates(ctx: NarrativeContext): List[CandidateInfo] =
    val played = ctx.playedSan.map(_.trim.toLowerCase)
    ctx.candidates
      .drop(1)
      .foldLeft(List.empty[CandidateInfo]) { (acc, c) =>
        if acc.exists(_.move.trim.equalsIgnoreCase(c.move.trim)) then acc else acc :+ c
      }
      .filterNot(c => played.contains(c.move.trim.toLowerCase))
      .take(2)

  private def renderAlternativeAttempt(
      alts: List[CandidateInfo],
      signals: List[AlternativeEngineSignal],
      mainCandidate: Option[CandidateInfo],
      bead: Int,
      pass: Int,
      crossBeatState: CrossBeatRepetitionState
  ): AlternativeRenderAttempt =
    val renderState = alternativeRenderState(crossBeatState)
    val passSeed = bead ^ (pass * 0x9e3779b9)
    val lines =
      alts.zipWithIndex.map { case (c, i) =>
        renderAlternativeAttemptLine(c, signals(i), mainCandidate, passSeed, i, renderState)
      }
    AlternativeRenderAttempt(
      lines = lines,
      penalty = alternativesRepetitionPenalty(lines),
      usedHypothesisFamilies = renderState.usedHypothesisFamilies.toSet,
      usedHypothesisStems = renderState.usedHypothesisStems.toSet,
      usedDifferencePrefixes = renderState.usedDifferencePrefixes.toSet,
      usedDifferenceTails = renderState.usedDifferenceTails.toSet
    )

  private def alternativeRenderState(crossBeatState: CrossBeatRepetitionState): AlternativeRenderState =
    val prefixCounts = scala.collection.mutable.HashMap.empty[String, Int].withDefaultValue(0)
    prefixCounts ++= crossBeatState.prefixCounts
    AlternativeRenderState(
      usedFamilies = scala.collection.mutable.HashSet.empty[String],
      usedStems = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedStems,
      prefixCounts = prefixCounts,
      usedHypothesisFamilies = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedHypothesisFamilies,
      usedHypothesisStems = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedHypothesisStems,
      usedDifferencePrefixes = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedDifferencePrefixes,
      usedDifferenceTails = scala.collection.mutable.HashSet.empty[String] ++ crossBeatState.usedDifferenceTails
    )

  private def renderAlternativeAttemptLine(
    alternative: CandidateInfo,
    signal: AlternativeEngineSignal,
    mainCandidate: Option[CandidateInfo],
    passSeed: Int,
    idx: Int,
    renderState: AlternativeRenderState
  ): String =
    val localSeed = alternativeAttemptSeed(passSeed, idx)
    val (line, family) = renderAlternativeAttemptBase(alternative, signal, localSeed, idx, renderState)
    renderState.usedFamilies += family
    val withDifference = appendAlternativeAttemptDifference(line, alternative, mainCandidate, signal, localSeed, renderState)
    trackTemplateUsage(withDifference, renderState.usedStems, renderState.prefixCounts)
    withDifference

  private def alternativeAttemptSeed(passSeed: Int, idx: Int): Int =
    passSeed ^ ((idx + 1) * 0x45d9f3b)

  private def alternativeAttemptRole(idx: Int): String =
    if idx == 0 then "engine_primary" else "practical_secondary"

  private def renderAlternativeAttemptBase(
    alternative: CandidateInfo,
    signal: AlternativeEngineSignal,
    localSeed: Int,
    idx: Int,
    renderState: AlternativeRenderState
  ): (String, String) =
    renderAlternativeDiversified(
      c = alternative,
      idx = idx,
      bead = localSeed,
      usedFamilies = renderState.usedFamilies.toSet,
      signal = signal,
      usedStems = renderState.usedStems.toSet,
      prefixCounts = renderState.prefixCounts.toMap,
      role = alternativeAttemptRole(idx)
    )

  private def appendAlternativeAttemptDifference(
    line: String,
    alternative: CandidateInfo,
    mainCandidate: Option[CandidateInfo],
    signal: AlternativeEngineSignal,
    localSeed: Int,
    renderState: AlternativeRenderState
  ): String =
    appendAlternativeHypothesisDifference(
      baseLine = line,
      alternative = alternative,
      mainCandidate = mainCandidate,
      signal = signal,
      bead = localSeed ^ 0x6d2b79f5,
      usedStems = renderState.usedStems.toSet,
      prefixCounts = renderState.prefixCounts.toMap,
      usedHypothesisFamilies = renderState.usedHypothesisFamilies,
      usedHypothesisStems = renderState.usedHypothesisStems,
      usedDifferencePrefixes = renderState.usedDifferencePrefixes,
      usedDifferenceTails = renderState.usedDifferenceTails
    )

  private def applyAlternativeAttempt(
      attempt: AlternativeRenderAttempt,
      crossBeatState: CrossBeatRepetitionState
  ): Unit =
    crossBeatState.usedHypothesisFamilies ++= attempt.usedHypothesisFamilies
    crossBeatState.usedHypothesisStems ++= attempt.usedHypothesisStems
    crossBeatState.usedDifferencePrefixes ++= attempt.usedDifferencePrefixes
    crossBeatState.usedDifferenceTails ++= attempt.usedDifferenceTails
    attempt.lines.foreach(line => trackTemplateUsage(line, crossBeatState.usedStems, crossBeatState.prefixCounts))

  private def buildWrapUpBeat(
    ctx: NarrativeContext,
    rankedPlans: RankedQuestionPlans,
    bead: Int
  ): Option[OutlineBeat] =
    val text = wrapUpFragments(ctx, rankedPlans, bead).renderedText
    Option.when(text.nonEmpty)(wrapUpBeat(text))

  private def wrapUpFragments(
    ctx: NarrativeContext,
    rankedPlans: RankedQuestionPlans,
    bead: Int
  ): WrapUpFragments =
    val cpWhite = wrapUpEvalCpWhite(ctx)
    WrapUpFragments(
      planner = plannerWrapUpFragments(rankedPlans),
      practical = practicalWrapUpFragment(ctx, bead, cpWhite),
      compensation = compensationWrapUpFragment(ctx, bead)
    )

  private def wrapUpEvalCpWhite(ctx: NarrativeContext): Int =
    rankedEngineVariations(ctx).headOption.map(_.scoreCp).orElse(ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)).getOrElse(0)

  private def plannerWrapUpFragments(rankedPlans: RankedQuestionPlans): List[AuthorityTaggedFragment] =
    rankedPlans.primary
      .toList
      .flatMap(_.consequence)
      .filter(_.beat == QuestionPlanConsequenceBeat.WrapUp)
      .map(_.text)
      .map(text => supportFragment(text, plannerOwned = true, generalized = true))

  private def practicalWrapUpFragment(
    ctx: NarrativeContext,
    bead: Int,
    cpWhite: Int
  ): Option[AuthorityTaggedFragment] =
    ctx.semantic
      .flatMap(_.practicalAssessment)
      .map(pa => supportFragment(buildPracticalWrapUpSentence(pa, bead, cpWhite, ctx.ply), sceneGrounded = true, generalized = true))

  private def compensationWrapUpFragment(
    ctx: NarrativeContext,
    bead: Int
  ): Option[AuthorityTaggedFragment] =
    effectiveCompensationSignal(ctx)
      .map(signal => supportFragment(buildCompensationWrapUpSentence(signal, bead), sceneGrounded = true, generalized = true))

  private def wrapUpBeat(text: String): OutlineBeat =
    OutlineBeat(
      kind = OutlineBeatKind.WrapUp,
      text = text,
      conceptIds = List("practical_assessment"),
      focusPriority = 72
    )

  private def buildPracticalMainMoveSentence(
    ctx: NarrativeContext,
    existingText: String
  ): Option[String] =
    val sentence =
      ctx.semantic
        .flatMap(_.practicalAssessment)
        .flatMap(practicalMainMoveSentence)
        .orElse(compensationMainMoveSentence(ctx))
    sentence
      .map(ensureSentence)
      .filter(sentenceIsNovel(_, existingText))

  private def practicalMainMoveSentence(pa: PracticalInfo): Option[String] =
    val verdict = pa.verdict.trim.toLowerCase
    val drivers = summarizePracticalDrivers(pa.biasFactors, limit = 2)
    if verdict.isEmpty && drivers.isEmpty then None
    else if verdict.contains("conversion") then Some("The practical task is less about a new tactic than keeping the technical route clean.")
    else if verdict.contains("defen") then Some("Practically, the move also keeps the defensive task manageable.")
    else if verdict.contains("counter") then Some("Practically, the move matters because it limits the opponent's easiest counterplay.")
    else Some(generalPracticalMainMoveSentence(verdict, drivers))

  private def generalPracticalMainMoveSentence(verdict: String, drivers: List[String]): String =
    val verdictText =
      if verdict.nonEmpty then s"Practically, the key task is ${verdict.stripSuffix(".")}"
      else "Practically, the task is defined by the easiest plans to handle"
    val driverText =
      Option.when(drivers.nonEmpty)(s", with the workload driven by ${drivers.mkString(" and ")}")
        .getOrElse("")
    s"$verdictText$driverText."

  private def compensationMainMoveSentence(ctx: NarrativeContext): Option[String] =
    effectiveCompensationSignal(ctx).flatMap { signal =>
      signal.summary.map(_.trim).filter(_.nonEmpty).map { plan =>
        val vectors = summarizeCompensationVectorLabels(signal.vectors, limit = 2)
        val vectorText =
          Option.when(vectors.nonEmpty)(s", especially through ${vectors.mkString(" and ")}")
            .getOrElse("")
        s"Any compensation still has to justify itself through $plan$vectorText."
      }
    }

  private def buildProphylaxisMainMoveSentence(
    ctx: NarrativeContext,
    bead: Int
  ): Option[String] =
    ctx.semantic.flatMap(_.preventedPlans.headOption).flatMap { plan =>
      val target = preventedPlanTarget(plan)
      val impact = preventedPlanImpact(plan)
      plan.sourceScope match
        case FactScope.Now => immediateProphylaxisSentence(plan, target, impact, bead)
        case _             => citedProphylaxisSentence(plan, target, impact)
    }

  private def preventedPlanTarget(plan: PreventedPlanInfo): Option[String] =
    plan.preventedThreatType.map(_.trim).filter(_.nonEmpty)
      .orElse(plan.breakNeutralized.map(file => s"$file-break"))

  private def preventedPlanImpact(plan: PreventedPlanInfo): Option[String] =
    Option.when(plan.counterplayScoreDrop >= 100)(s"blunting roughly ${plan.counterplayScoreDrop}cp of counterplay")

  private def immediateProphylaxisSentence(
    plan: PreventedPlanInfo,
    target: Option[String],
    impact: Option[String],
    bead: Int
  ): Option[String] =
    target match
      case Some(t) =>
        Some(s"The move is prophylactic too: it cuts out $t${impact.map(i => s" while $i").getOrElse("")}.")
      case None if plan.counterplayScoreDrop >= 120 =>
        Some(s"The move is prophylactic too, stripping away roughly ${plan.counterplayScoreDrop}cp of counterplay.")
      case _ =>
        Option.when(plan.planId.trim.nonEmpty)(NarrativeLexicon.getPreventedPlanStatement(bead, plan.planId))

  private def citedProphylaxisSentence(
    plan: PreventedPlanInfo,
    target: Option[String],
    impact: Option[String]
  ): Option[String] =
    plan.citationLine.flatMap { citation =>
      val targetText = target.getOrElse("the opponent's easiest counterplay")
      val body =
        impact match
          case Some(i) => s"$targetText becomes available again, $i"
          case None    => s"$targetText becomes available again"
      LineScopedCitation.afterClause(citation, body)
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
      val text = hypothesisNarrativeClauses(ctx, focusCandidate, supportCandidate, bead, crossBeatState, first, secondary)
        .filter(_.trim.nonEmpty)
        .mkString(" ")
      trackHypothesisStemUsage(text, crossBeatState)
      text.trim
    }

  private def hypothesisNarrativeClauses(
      ctx: NarrativeContext,
      focusCandidate: Option[CandidateInfo],
      supportCandidate: Option[CandidateInfo],
      bead: Int,
      crossBeatState: CrossBeatRepetitionState,
      first: SelectedHypothesis,
      secondary: Option[SelectedHypothesis]
  ): List[String] =
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
    List(
      observation,
      hypothesis,
      validation,
      longHorizonHypothesisBridge(bead, crossBeatState, first, observation, hypothesis, validation).getOrElse(""),
      supportingHypothesisClause(bead, secondary).getOrElse(""),
      NarrativeLexicon.getHypothesisPracticalClause(
        bead = bead ^ 0x4f6cdd1d,
        horizon = first.card.horizon,
        axis = first.card.axis,
        move = first.sourceMove
      )
    )

  private def longHorizonHypothesisBridge(
      bead: Int,
      crossBeatState: CrossBeatRepetitionState,
      first: SelectedHypothesis,
      observation: String,
      hypothesis: String,
      validation: String
  ): Option[String] =
    Option.when(first.card.horizon == HypothesisHorizon.Long && hasLongHorizonSupportSignal(first.card.supportSignals)) {
      val bridgeCandidates =
        List(0, 1, 2, 3, 4, 5).map { idx =>
          NarrativeLexicon.getLongHorizonBridgeClause(
            bead = bead ^ (0x3124bcf5 + idx * 0x9e3779b9),
            move = first.sourceMove,
            axis = first.card.axis
          )
        }.distinct
      val usedHypothesisNarrativeStems =
        Set(normalizeStem(observation), normalizeStem(hypothesis), normalizeStem(validation)).filter(_.nonEmpty)
      selectNonRepeatingTemplate(
        templates = bridgeCandidates,
        seed = bead ^ 0x19f8b4ad,
        usedStems = crossBeatState.usedStems.toSet ++ usedHypothesisNarrativeStems,
        prefixCounts = crossBeatState.prefixCounts.toMap,
        prefixLimits = PrefixFamilyLimits
      )
    }

  private def supportingHypothesisClause(
      bead: Int,
      secondary: Option[SelectedHypothesis]
  ): Option[String] =
    secondary.map { extra =>
      NarrativeLexicon.getSupportingHypothesisClause(
        bead = bead ^ Math.abs(extra.sourceMove.hashCode) ^ 0x2f6e2b1,
        claim = extra.card.claim,
        confidence = extra.card.confidence,
        axis = extra.card.axis
      )
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
    val material = alternativeHypothesisDifferenceMaterial(
      alternative = alternative,
      mainCandidate = mainCandidate,
      signal = signal,
      usedHypothesisFamilies = usedHypothesisFamilies,
      usedHypothesisStems = usedHypothesisStems
    )
    val difference = selectAlternativeHypothesisDifference(
      material = material,
      alternative = alternative,
      bead = bead,
      usedStems = usedStems,
      prefixCounts = prefixCounts,
      usedHypothesisStems = usedHypothesisStems,
      usedDifferencePrefixes = usedDifferencePrefixes,
      usedDifferenceTails = usedDifferenceTails
    )
    val normalized = normalizedAlternativeHypothesisDifference(baseLine, difference, bead)
    trackAlternativeHypothesisDifferenceUsage(material, usedHypothesisFamilies, usedHypothesisStems)
    selectNonRepeatingAlternativeHypothesisDifference(normalized, bead, usedStems, prefixCounts, usedHypothesisStems)

  private def alternativeHypothesisDifferenceMaterial(
    alternative: CandidateInfo,
    mainCandidate: Option[CandidateInfo],
    signal: AlternativeEngineSignal,
    usedHypothesisFamilies: scala.collection.mutable.Set[String],
    usedHypothesisStems: scala.collection.mutable.Set[String]
  ): AlternativeHypothesisDifferenceMaterial =
    val mainHypothesis =
      pickHypothesisForDifference(mainCandidate, usedHypothesisFamilies, usedHypothesisStems)
    val alternativeHypothesis =
      pickHypothesisForDifference(Some(alternative), usedHypothesisFamilies, usedHypothesisStems)
    AlternativeHypothesisDifferenceMaterial(
      mainHypothesis = mainHypothesis,
      alternativeHypothesis = alternativeHypothesis,
      alternativeClaim = alternativeHypothesisClaim(alternativeHypothesis, usedHypothesisStems),
      mainMove = mainCandidate.map(_.move).getOrElse(signal.bestSan.getOrElse("the principal move"))
    )

  private def alternativeHypothesisClaim(
    alternativeHypothesis: Option[HypothesisCard],
    usedHypothesisStems: scala.collection.mutable.Set[String]
  ): Option[String] =
    alternativeHypothesis
      .map(_.claim)
      .map(_.trim)
      .filter(_.nonEmpty)
      .filterNot { claim =>
        val stem = normalizeMoveNeutralClaimStem(claim)
        stem.nonEmpty && usedHypothesisStems.contains(stem)
      }

  private def selectAlternativeHypothesisDifference(
    material: AlternativeHypothesisDifferenceMaterial,
    alternative: CandidateInfo,
    bead: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    usedHypothesisStems: scala.collection.mutable.Set[String],
    usedDifferencePrefixes: scala.collection.mutable.Set[String],
    usedDifferenceTails: scala.collection.mutable.Set[String]
  ): String =
    val differenceVariants = NarrativeLexicon.getAlternativeHypothesisDifferenceVariants(
      bead = bead,
      alternativeMove = alternative.move,
      mainMove = material.mainMove,
      mainAxis = material.mainHypothesis.map(_.axis),
      alternativeAxis = material.alternativeHypothesis.map(_.axis),
      alternativeClaim = material.alternativeClaim,
      confidence = material.alternativeHypothesis.map(_.confidence).getOrElse(0.42),
      horizon = material.alternativeHypothesis.map(_.horizon).orElse(material.mainHypothesis.map(_.horizon)).getOrElse(HypothesisHorizon.Medium)
    )
    selectDifferenceVariant(
      variants = differenceVariants,
      seed = bead ^ 0x2f6e2b1,
      usedStems = usedStems ++ usedHypothesisStems.toSet,
      prefixCounts = prefixCounts,
      usedDifferencePrefixes = usedDifferencePrefixes,
      usedDifferenceTails = usedDifferenceTails
    )

  private def normalizedAlternativeHypothesisDifference(baseLine: String, difference: String, bead: Int): String =
    val rendered = List(baseLine.trim, difference.trim).filter(_.nonEmpty).mkString(" ").trim
    normalizeAlternativeTemplateLine(rendered, bead ^ 0x63d5a6f1)

  private def trackAlternativeHypothesisDifferenceUsage(
    material: AlternativeHypothesisDifferenceMaterial,
    usedHypothesisFamilies: scala.collection.mutable.Set[String],
    usedHypothesisStems: scala.collection.mutable.Set[String]
  ): Unit =
    (material.mainHypothesis.toList ++ material.alternativeHypothesis.toList).foreach { h =>
      usedHypothesisFamilies += hypothesisFamily(h)
      val stem = normalizeMoveNeutralClaimStem(h.claim)
      if stem.nonEmpty then usedHypothesisStems += stem
    }

  private def selectNonRepeatingAlternativeHypothesisDifference(
    normalized: String,
    bead: Int,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    usedHypothesisStems: scala.collection.mutable.Set[String]
  ): String =
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
    val rawObservation = hypothesisObservationText(ctx, candidate, bead)
    NarrativeLexicon.getHypothesisObservationClause(bead, rawObservation)

  private def hypothesisObservationText(
    ctx: NarrativeContext,
    candidate: Option[CandidateInfo],
    bead: Int
  ): String =
    candidate.flatMap(candidateTacticalObservation)
      .orElse(unresolvedThreatObservation(ctx))
      .orElse(candidate.flatMap(candidateWhyNotObservation))
      .orElse(candidate.map(c => candidateFallbackObservation(c, bead)))
      .getOrElse("the position still has competing strategic routes")

  private def candidateTacticalObservation(candidate: CandidateInfo): Option[String] =
    candidate.tacticalAlert.map(_.trim).filter(_.nonEmpty)

  private def unresolvedThreatObservation(ctx: NarrativeContext): Option[String] =
    ctx.threats.toUs.headOption.map { t =>
      val sq = t.square.map(s => s" on $s").getOrElse("")
      s"${t.kind.toLowerCase} pressure$sq remains unresolved"
    }

  private def candidateWhyNotObservation(candidate: CandidateInfo): Option[String] =
    candidate.whyNot.map(_.trim).filter(_.nonEmpty)

  private def candidateFallbackObservation(candidate: CandidateInfo, bead: Int): String =
    val move = candidate.move
    NarrativeLexicon.pick(bead ^ Math.abs(move.hashCode), List(
      s"$move changes the candidate comparison",
      s"$move redirects the strategic route",
      s"$move shifts which plan branch is simplest to handle",
      s"$move alters the strategic map for both sides",
      s"$move shifts the position's main focus",
      s"The move $move introduces a new strategic branch"
    ))

  private def trackHypothesisStemUsage(text: String, state: CrossBeatRepetitionState): Unit =
    val stem = normalizeHypothesisStem(text)
    if stem.nonEmpty then state.usedHypothesisStems += stem

  private def hasLongHorizonSupportSignal(signals: List[String]): Boolean =
    signals.exists(_.toLowerCase.contains("long-horizon"))

  private def normalizeHypothesisStem(text: String): String =
    normalizeStem(text)
      .split(" ")
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
      val pool = differenceVariantPool(rotated, usedDifferencePrefixes, usedDifferenceTails)
      val selected =
        selectNonRepeatingTemplate(
          templates = pool,
          seed = seed ^ 0x63d5a6f1,
          usedStems = usedStems,
          prefixCounts = prefixCounts,
          prefixLimits = PrefixFamilyLimits
        )
      trackDifferenceVariantUsage(selected, usedDifferencePrefixes, usedDifferenceTails)
      selected

  private def differenceVariantPool(
    rotated: List[String],
    usedDifferencePrefixes: scala.collection.mutable.Set[String],
    usedDifferenceTails: scala.collection.mutable.Set[String]
  ): List[String] =
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
    if unseenBothPool.nonEmpty then unseenBothPool
    else if unseenPrefixPool.nonEmpty then unseenPrefixPool
    else if unseenTailPool.nonEmpty then unseenTailPool
    else rotated

  private def trackDifferenceVariantUsage(
    selected: String,
    usedDifferencePrefixes: scala.collection.mutable.Set[String],
    usedDifferenceTails: scala.collection.mutable.Set[String]
  ): Unit =
    val prefix = normalizeDifferencePrefix(selected)
    if prefix.nonEmpty then usedDifferencePrefixes += prefix
    val tail = normalizeDifferenceTail(selected)
    if tail.nonEmpty then usedDifferenceTails += tail

  private def normalizeDifferencePrefix(text: String): String =
    normalizeStem(text)
      .split(" ")
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
    normalizeStem(tailRaw)
      .split(" ")
      .take(6)
      .mkString(" ")

  private def pickKeyFact(ctx: NarrativeContext): Option[Fact] =
    ctx.facts
      .filter(_.scope == FactScope.Now)
      .filter(CommentaryIdeaSurface.isKeyFactEligible)
      .sortBy(CommentaryIdeaSurface.keyFactPriority)
      .headOption

  private def buildDeltaAfterMoveText(ctx: NarrativeContext, bead: Int): Option[String] =
    if !ctx.deltaAfterMove then None
    else ctx.delta.flatMap(deltaAfterMoveText(ctx, _, bead))

  private def deltaAfterMoveText(ctx: NarrativeContext, delta: MoveDelta, bead: Int): Option[String] =
    val moverIsWhite = ctx.ply % 2 == 1
    val mover = if moverIsWhite then "White" else "Black"
    val moverCp = if moverIsWhite then delta.evalChange else -delta.evalChange
    val b = bead ^ Math.abs(mover.hashCode) ^ Math.abs(delta.hashCode)
    val combined =
      List(
        NarrativeLexicon.getEvalSwingAfterMoveStatement(b, mover, moverCp),
        deltaAfterMoveHighlight(delta, b)
      ).flatten.filter(_.trim.nonEmpty).mkString(" ")
    Option.when(combined.nonEmpty)(combined)

  private def deltaAfterMoveHighlight(delta: MoveDelta, bead: Int): Option[String] =
    deltaPhaseTransitionText(delta.phaseChange, bead)
      .orElse(delta.openFileCreated.map(f => NarrativeLexicon.getOpenFileCreatedStatement(bead ^ 0x2f2f2f, f)))
      .orElse(delta.structureChange.map(sc => NarrativeLexicon.getStructureChangeStatement(bead ^ 0x3f3f3f, sc)))
      .orElse(delta.newMotifs.headOption.map(m => NarrativeLexicon.getMotifAppearsStatement(bead ^ 0x4f4f4f, NarrativeUtils.humanize(m))))
      .orElse(delta.lostMotifs.headOption.map(m => NarrativeLexicon.getMotifFadesStatement(bead ^ 0x5f5f5f, NarrativeUtils.humanize(m))))

  private def deltaPhaseTransitionText(phaseChange: Option[String], bead: Int): Option[String] =
    phaseChange.flatMap { s =>
      val raw = s.stripPrefix("Transition from ").trim
      raw.split(" to ", 2).toList match
        case from :: to :: Nil if from.nonEmpty && to.nonEmpty =>
          Some(NarrativeLexicon.getPhaseTransitionStatement(bead ^ 0x1f1f1f, from, to))
        case _ => None
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
      terminalAnnotationHintFragments(bead, moveHint)
    else
      nonTerminalAnnotationHintFragments(bead, tags, practicalDifficulty, moveHint, phase)

  private def terminalAnnotationHintFragments(bead: Int, moveHint: String): AnnotationHintFragments =
    AnnotationHintFragments(
      terminal = Some(annotationHintSupportFragment(NarrativeLexicon.getAnnotationTerminalMoveHint(bead, moveHint)))
    )

  private def nonTerminalAnnotationHintFragments(
    bead: Int,
    tags: List[CandidateTag],
    practicalDifficulty: String,
    moveHint: String,
    phase: String
  ): AnnotationHintFragments =
    AnnotationHintFragments(
      tagOnly =
        tags
          .flatMap(tag => NarrativeLexicon.getAnnotationTagOnlyHint(bead, tag, moveHint))
          .headOption
          .map(annotationHintSupportFragment),
      difficulty =
        NarrativeLexicon
          .getAnnotationDifficultyHint(bead, practicalDifficulty, moveHint, phase)
          .map(annotationHintSupportFragment)
    )

  private def annotationHintSupportFragment(text: String): AuthorityTaggedFragment =
    supportFragment(text, moveLinkedAnchor = true, sceneGrounded = true, generalized = true)

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
    val parts = causalAnnotationParts(rankContext, reason, bestIntent)
    val reasonBridge =
      causalAnnotationReasonBridge(
        enabled = parts.rank.nonEmpty && parts.issue.nonEmpty,
        tacticalEmphasis = tacticalEmphasis,
        bead = bead,
        usedStems = usedStems ++ parts.rank.toSet.map(normalizeStem),
        prefixCounts = prefixCounts
      )
    val betterBridge =
      consequenceBridge(
        enabled = parts.issue.nonEmpty && parts.better.nonEmpty,
        seed = bead ^ 0x3b5296f1,
        usedStems = usedStems ++ parts.issue.toSet.map(normalizeStem),
        prefixCounts = prefixCounts
      )
    renderCausalAnnotationParts(parts, reasonBridge, betterBridge)

  private def causalAnnotationParts(
    rankContext: Option[String],
    reason: String,
    bestIntent: String
  ): CausalAnnotationParts =
    CausalAnnotationParts(
      rank = rankContext.map(_.trim).filter(_.nonEmpty),
      issue = Option(reason).map(_.trim).filter(_.nonEmpty),
      better = Option(bestIntent).map(_.trim).filter(_.nonEmpty)
    )

  private def renderCausalAnnotationParts(
    parts: CausalAnnotationParts,
    reasonBridge: Option[String],
    betterBridge: Option[String]
  ): String =
    val reasonClause = bridgedCausalAnnotationClause(parts.issue, reasonBridge)
    val betterClause = bridgedCausalAnnotationClause(parts.better, betterBridge)
    List(parts.rank.getOrElse(""), reasonClause.getOrElse(""), betterClause.getOrElse(""))
      .filter(_.trim.nonEmpty)
      .mkString(" ")

  private def bridgedCausalAnnotationClause(text: Option[String], bridge: Option[String]): Option[String] =
    text.map(t => bridge.map(b => s"$b $t").getOrElse(t))

  private def causalAnnotationReasonBridge(
      enabled: Boolean,
      tacticalEmphasis: Boolean,
      bead: Int,
      usedStems: Set[String],
      prefixCounts: Map[String, Int]
  ): Option[String] =
    Option.when(enabled) {
      selectNonRepeatingTemplate(
        templates =
          if tacticalEmphasis then TacticalAnnotationReasonBridges
          else PracticalAnnotationReasonBridges,
        seed = bead ^ 0x24d8f59c,
        usedStems = usedStems,
        prefixCounts = prefixCounts,
        prefixLimits = PrefixFamilyLimits
      )
    }.filter(_.nonEmpty)

  private def consequenceBridge(
      enabled: Boolean,
      seed: Int,
      usedStems: Set[String],
      prefixCounts: Map[String, Int]
  ): Option[String] =
    Option.when(enabled) {
      selectNonRepeatingTemplate(
        templates = ConsequenceBridges,
        seed = seed,
        usedStems = usedStems,
        prefixCounts = prefixCounts,
        prefixLimits = PrefixFamilyLimits
      )
    }.filter(_.nonEmpty)

  private def buildConcreteAnnotationIssue(input: ConcreteAnnotationIssueInput): String =
    val cause = concreteAnnotationCause(input)
    val consequence =
      buildIssueConsequence(input.bead, input.cpLoss, input.bestReply, input.threatsToUs, input.playedCand)
    val linkedConsequence = linkedIssueConsequence(input.bead, input.contextHint, cause, consequence)

    List(cause, linkedConsequence.getOrElse("")).filter(_.trim.nonEmpty).mkString(" ")

  private def concreteAnnotationCause(input: ConcreteAnnotationIssueInput): String =
    val severity = Thresholds.classifySeverity(input.cpLoss)
    val tacticalIssue = tacticalAnnotationIssue(input.cpLoss, severity, input.missedMotif, input.bestReply)
    val baseIssues = concreteAnnotationBaseIssues(input, tacticalIssue)
    val hasConcreteEvidence = baseIssues.flatten.nonEmpty
    val replyIssue =
      Option.when(input.cpLoss >= Thresholds.INACCURACY_CP && hasConcreteEvidence && tacticalIssue.isEmpty) {
        input.bestReply.filter(isForcingReplySan).map(r => s"Issue: after this, ...$r gives the opponent a forcing reply.")
      }.flatten
    val rankIssue =
      rankAnnotationIssue(input.bead, input.cpLoss, input.contextHint, input.playedRank, input.allowConcreteBenchmark)
    val fallbackIssue =
      Option.when(input.cpLoss >= Thresholds.INACCURACY_CP)(s"Issue: ${defaultIssueBySeverity(input.bead, input.cpLoss)}.")
    (baseIssues ++ List(replyIssue, rankIssue, fallbackIssue))
      .flatten
      .find(_.trim.nonEmpty)
      .getOrElse("")

  private def concreteAnnotationBaseIssues(
    input: ConcreteAnnotationIssueInput,
    tacticalIssue: Option[String]
  ): List[Option[String]] =
    List(
      tacticalIssue,
      unresolvedThreatIssue(input.threatsToUs, input.playedSan, input.playedUci, input.bestSan, input.bestUci),
      input.playedCand.flatMap(c => extractFactConsequence(c, input.currentPly)).map(s => s"Issue: $s"),
      input.alert.map(a => s"Issue: ${a.stripSuffix(".")}."),
      input.whyNot.flatMap(humanizeWhyNot).map(r => s"Issue: $r."),
      Option.when(input.cpLoss >= Thresholds.INACCURACY_CP) {
        input.missedMotif.map(m => s"Issue: this bypasses the tactical idea of $m.")
      }.flatten
    )

  private def tacticalAnnotationIssue(
      cpLoss: Int,
      severity: String,
      missedMotif: Option[String],
      bestReply: Option[String]
  ): Option[String] =
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

  private def rankAnnotationIssue(
      bead: Int,
      cpLoss: Int,
      contextHint: Int,
      playedRank: Option[Int],
      allowConcreteBenchmark: Boolean
  ): Option[String] =
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

  private def linkedIssueConsequence(
      bead: Int,
      contextHint: Int,
      cause: String,
      consequence: Option[String]
  ): Option[String] =
    consequence.map { c =>
      val bridge = consequenceBridge(
        enabled = true,
        seed = bead ^ contextHint ^ 0x5f356495,
        usedStems = Set(normalizeStem(cause)),
        prefixCounts = Map.empty
      )
      if cause.trim.nonEmpty then bridge.map(b => s"$b $c").getOrElse(c) else c
    }

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
  ): Option[String] =
    val selected = missingThemeKeywords(motifs, existingText, phase).take(2)
    renderThemeKeywordSentence(selected, bead, ply)

  private def missingThemeKeywords(
    motifs: List[String],
    existingText: String,
    phase: String
  ): List[String] =
    val existingLower = existingText.toLowerCase
    motifs
      .map(normalizeMotifKey)
      .filter(_.nonEmpty)
      .filter(m => motifPhaseCompatible(m, phase))
      .flatMap(themeKeywordForMotif)
      .filterNot(term => existingLower.contains(term.toLowerCase))
      .distinct

  private def renderThemeKeywordSentence(
    selected: List[String],
    bead: Int,
    ply: Int
  ): Option[String] =
    if selected.isEmpty then None
    else
      val terms = selected.take(2).map(term => s"**$term**")
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

  private def themeKeywordForMotif(motif: String): Option[String] =
    deferredRelationCanonicalTerm(motif) match
      case Some(term) => term
      case None       => Some(motif.replace("_", " "))

  private def buildCanonicalMotifTermSentence(
    motifs: List[String],
    existingText: String,
    bead: Int,
    ply: Int,
    phase: String
  ): Option[String] =
    val selected = missingCanonicalMotifTerms(motifs, existingText, phase).take(2)
    val rendered = renderCanonicalMotifTerms(selected, bead, ply)
    Option.when(rendered.trim.nonEmpty)(rendered.trim)

  private def missingCanonicalMotifTerms(
    motifs: List[String],
    existingText: String,
    phase: String
  ): List[String] =
    val existingLower = existingText.toLowerCase
    val missingTerms = motifs
      .map(normalizeMotifKey)
      .filter(_.nonEmpty)
      .filter(m => motifPhaseCompatible(m, phase))
      .flatMap(canonicalTermForMotif)
      .distinct
      .filterNot { term =>
        existingLower.contains(term.toLowerCase)
      }
    missingTerms.sortBy(t => if HighPriorityCanonicalMotifTerms(t.toLowerCase) then 0 else 1)

  private def renderCanonicalMotifTerms(
    selected: List[String],
    bead: Int,
    ply: Int
  ): String =
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

  private def canonicalTermForMotif(rawMotif: String): Option[String] =
    val motif = normalizeMotifKey(rawMotif)
    if motif.isEmpty then None
    else
      deferredRelationCanonicalTerm(motif) match
        case Some(term) => term
        case None       => ordinaryCanonicalTermForMotif(motif)

  private def deferredRelationCanonicalTerm(motif: String): Option[Option[String]] =
    if RelationObservationCatalog.relationWitnessOnlyMotifTag(motif) then Some(None)
    else RelationObservationCatalog.deferredFallbackForMotifTag(motif).map(_.label)

  private def tacticalTensionMotif(motif: String): Boolean =
    !RelationObservationCatalog.relationWitnessOnlyMotifTag(motif) &&
      RelationObservationCatalog.deferredFallbackForMotifTag(motif).isEmpty &&
      List(
        "mate",
        "sacrifice",
        "king_hunt",
        "smothered",
        "greek_gift",
        "fork",
        "skewer",
        "deflection",
        "interference"
      ).exists(motif.contains)

  private def ordinaryCanonicalTermForMotif(motif: String): Option[String] =
    if motif.contains("passed_pawn") || motif.contains("promotion_race") then Some("passed pawn")
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
      case EndgameTransitionPattern(fromRaw, _, toRaw, _) =>
        val fromLabel = humanizeEndgamePattern(fromRaw)
        val toLabel = humanizeEndgamePattern(toRaw)
        Some(
          if fromRaw.equalsIgnoreCase("none") then
            s"A new endgame structure is visible around $toLabel, so the context has changed."
          else if toRaw.equalsIgnoreCase("none") then
            s"The earlier endgame structure around $fromLabel has dissolved, so that context is no longer stable by itself."
          else
            s"The endgame structure has shifted from $fromLabel to $toLabel."
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
          s"The $label has stayed visible $duration, keeping that endgame context in view."
        }
      }
    }

  private def humanizeEndgamePattern(raw: String): String =
    val normalized = Option(raw).getOrElse("").trim
    if normalized.isEmpty then "endgame shape"
    else if normalized.equalsIgnoreCase("none") then "no stable endgame shape"
    else if normalized.equalsIgnoreCase("Lucena") then "bridge-building rook-pawn shape"
    else if normalized.equalsIgnoreCase("PhilidorDefense") then "barrier-rank rook defense"
    else if normalized.equalsIgnoreCase("VancuraDefense") then "side-checking rook defense"
    else if normalized.equalsIgnoreCase("WrongRookPawnWrongBishopFortress") then "wrong-corner rook-pawn setup"
    else if normalized.equalsIgnoreCase("OutsidePasserDecoy") then "outside-passer decoy shape"
    else if normalized.equalsIgnoreCase("ConnectedPassers") then "connected-passer shape"
    else if normalized.equalsIgnoreCase("KeySquaresOppositionBreakthrough") then "king-and-key-square breakthrough shape"
    else if normalized.equalsIgnoreCase("TriangulationZugzwang") then "triangulation-tempo shape"
    else if normalized.equalsIgnoreCase("BreakthroughSacrifice") then "pawn-breakthrough shape"
    else if normalized.equalsIgnoreCase("Shouldering") then "king-shouldering shape"
    else if normalized.equalsIgnoreCase("RetiManeuver") then "king-race pursuit shape"
    else if normalized.equalsIgnoreCase("ShortSideDefense") then "short-side rook defense"
    else if normalized.equalsIgnoreCase("OppositeColoredBishopsDraw") then "opposite-colored bishops blockade"
    else if normalized.equalsIgnoreCase("GoodBishopRookPawnConversion") then "bishop-and-rook-pawn setup"
    else if normalized.equalsIgnoreCase("KnightBlockadeRookPawnDraw") then "knight rook-pawn blockade"
    else if normalized.equalsIgnoreCase("QueenVsAdvancedPawn") then "queen-versus-advanced-pawn checking setup"
    else if normalized.equalsIgnoreCase("TarraschDefenseActive") then "active rook-behind-pawn setup"
    else if normalized.equalsIgnoreCase("PassiveRookDefense") then "passive rook-behind-pawn setup"
    else if normalized.equalsIgnoreCase("RookAndBishopVsRookDraw") then "rook-and-bishop versus rook defensive geometry"
    else if normalized.equalsIgnoreCase("SameColoredBishopsBlockade") then "same-colored bishops blockade"
    else
      "endgame shape"

  private def buildEndgameCausalitySentence(ctx: NarrativeContext, info: EndgameInfo): Option[String] =
    parseBoard(ctx.fen).flatMap { board =>
      info.transition.flatMap {
        case EndgameTransitionPattern(fromRaw, _, toRaw, _) =>
          val lossClause = endgamePatternLossClause(board, fromRaw)
          val gainClause =
            if toRaw.equalsIgnoreCase("none") then None else endgamePatternHoldClause(board, toRaw, transitioned = true)
          (lossClause, gainClause) match
            case (Some(loss), Some(gain)) => Some(s"$loss $gain")
            case (Some(loss), None)       => Some(loss)
            case (None, Some(gain))       => Some(gain)
            case _                        => None
        case _ => None
      }.orElse {
        info.primaryPattern.flatMap(pattern =>
          if info.patternAge >= 2 then endgamePatternHoldClause(board, pattern, transitioned = false) else None
        )
      }
    }

  private def endgamePatternLossClause(board: Board, rawPattern: String): Option[String] =
    Option(rawPattern).map(_.trim).filter(_.nonEmpty).flatMap {
      case pattern if pattern.equalsIgnoreCase("Lucena")          => lucenaLossClause(board)
      case pattern if pattern.equalsIgnoreCase("PhilidorDefense") => philidorLossClause(board)
      case pattern if pattern.equalsIgnoreCase("VancuraDefense")  => vancuraLossClause(board)
      case pattern if pattern.equalsIgnoreCase("WrongRookPawnWrongBishopFortress") => Some("The wrong-corner rook-pawn setup has loosened because the defender can no longer sit on the promotion corner against the wrong-colored bishop.")
      case pattern if pattern.equalsIgnoreCase("OutsidePasserDecoy") => Some("The outside passer decoy is no longer visible because the remote passer no longer drags the enemy king away from the main pawn mass.")
      case pattern if pattern.equalsIgnoreCase("ConnectedPassers") => Some("The connected passers shape has loosened because the pawns are no longer advancing together with king support.")
      case pattern if pattern.equalsIgnoreCase("KeySquaresOppositionBreakthrough") => Some("The key-squares breakthrough shape has loosened because the king no longer controls the entry squares needed to escort the pawn through.")
      case pattern if pattern.equalsIgnoreCase("TriangulationZugzwang") => Some("The triangulation-tempo shape has loosened because the spare king tempo is gone, so the move-order squeeze is no longer visible.")
      case pattern if pattern.equalsIgnoreCase("BreakthroughSacrifice") => Some("The pawn-breakthrough shape is no longer visible because the pawn wedge no longer opens a passer at the right moment.")
      case pattern if pattern.equalsIgnoreCase("Shouldering") => Some("The shouldering shape has loosened because the stronger king no longer keeps the enemy king pushed off the pawn's path.")
      case pattern if pattern.equalsIgnoreCase("RetiManeuver") => Some("The king-race pursuit shape has loosened because the king can no longer combine pursuit of the passer with support for its own pawn.")
      case pattern if pattern.equalsIgnoreCase("ShortSideDefense") => Some("The short-side defense setup has loosened because the defender has lost the checking distance and side-room needed to harass the king.")
      case pattern if pattern.equalsIgnoreCase("OppositeColoredBishopsDraw") => Some("The opposite-colored bishops blockade has loosened because the defender can no longer control the bishop's color complex.")
      case pattern if pattern.equalsIgnoreCase("GoodBishopRookPawnConversion") => Some("The bishop-and-rook-pawn setup has loosened because the bishop and king no longer control the promotion corner together.")
      case pattern if pattern.equalsIgnoreCase("KnightBlockadeRookPawnDraw") => Some("The knight rook-pawn blockade has loosened because the knight no longer controls the promotion square and blockade ring.")
      case pattern if pattern.equalsIgnoreCase("QueenVsAdvancedPawn") => Some("The queen-versus-pawn checking setup has loosened because the defender no longer keeps checking distance against the advanced pawn.")
      case pattern if pattern.equalsIgnoreCase("TarraschDefenseActive") => Some("The active rook-behind-pawn setup has loosened because the rook is no longer actively checking from behind the pawn.")
      case pattern if pattern.equalsIgnoreCase("PassiveRookDefense") => Some("The passive rook setup has loosened because the rook can no longer sit behind the pawn and block the file.")
      case pattern if pattern.equalsIgnoreCase("RookAndBishopVsRookDraw") => Some("The rook-and-bishop-versus-rook defensive geometry has loosened because the defender has lost checking distance or corner geometry.")
      case pattern if pattern.equalsIgnoreCase("SameColoredBishopsBlockade") => Some("The same-colored bishops blockade has loosened because the defender can no longer lock the pawn chain on the shared color complex.")
      case _ => None
    }

  private def endgamePatternHoldClause(board: Board, rawPattern: String, transitioned: Boolean): Option[String] =
    Option(rawPattern).map(_.trim).filter(_.nonEmpty).flatMap {
      case pattern if pattern.equalsIgnoreCase("Lucena")          => lucenaHoldClause(board, transitioned)
      case pattern if pattern.equalsIgnoreCase("PhilidorDefense") => philidorHoldClause(board, transitioned)
      case pattern if pattern.equalsIgnoreCase("VancuraDefense")  => vancuraHoldClause(board, transitioned)
      case pattern if pattern.equalsIgnoreCase("WrongRookPawnWrongBishopFortress") =>
        Some(s"${if transitioned then "The wrong-corner rook-pawn setup is now visible because" else "The wrong-corner rook-pawn setup remains visible because"} the defender remains on the promotion corner and the bishop does not control the entry color.")
      case pattern if pattern.equalsIgnoreCase("OutsidePasserDecoy") =>
        Some(s"${if transitioned then "The outside passer decoy is now visible because" else "The outside passer decoy remains visible because"} the remote passer keeps the enemy king away from the main pawn mass.")
      case pattern if pattern.equalsIgnoreCase("ConnectedPassers") =>
        Some(s"${if transitioned then "The connected passers are now visible because" else "The connected passers remain visible because"} the pawns advance together and the king still supports their front.")
      case pattern if pattern.equalsIgnoreCase("KeySquaresOppositionBreakthrough") =>
        Some(s"${if transitioned then "The key-squares breakthrough pattern is now visible because" else "The key-squares breakthrough pattern remains visible because"} the king still controls the critical entry squares in front of the pawn.")
      case pattern if pattern.equalsIgnoreCase("TriangulationZugzwang") =>
        Some(s"${if transitioned then "The triangulation-tempo shape is now visible because" else "The triangulation-tempo shape remains visible because"} one side still keeps a spare king tempo to shape the move order.")
      case pattern if pattern.equalsIgnoreCase("BreakthroughSacrifice") =>
        Some(s"${if transitioned then "The pawn-breakthrough shape is now visible because" else "The pawn-breakthrough shape remains visible because"} the pawn wedge still points at a passer once the center opens.")
      case pattern if pattern.equalsIgnoreCase("Shouldering") =>
        Some(s"${if transitioned then "The shouldering pattern is now visible because" else "The shouldering pattern remains visible because"} the king still keeps the opposing king off the pawn's route.")
      case pattern if pattern.equalsIgnoreCase("RetiManeuver") =>
        Some(s"${if transitioned then "The king-race pursuit shape is now visible because" else "The king-race pursuit shape remains visible because"} the king can chase the passer while staying near its own support route.")
      case pattern if pattern.equalsIgnoreCase("ShortSideDefense") =>
        Some(s"${if transitioned then "The short-side defense setup is now visible because" else "The short-side defense setup remains visible because"} the defender still has checking room on the short side and lateral space for the rook.")
      case pattern if pattern.equalsIgnoreCase("OppositeColoredBishopsDraw") =>
        Some(s"${if transitioned then "The opposite-colored bishops blockade is now visible because" else "The opposite-colored bishops blockade remains visible because"} each bishop controls a different color complex, limiting direct penetration.")
      case pattern if pattern.equalsIgnoreCase("GoodBishopRookPawnConversion") =>
        Some(s"${if transitioned then "The bishop-and-rook-pawn setup is now visible because" else "The bishop-and-rook-pawn setup remains visible because"} the bishop matches the promotion corner and the king still escorts the pawn.")
      case pattern if pattern.equalsIgnoreCase("KnightBlockadeRookPawnDraw") =>
        Some(s"${if transitioned then "The knight rook-pawn blockade is now visible because" else "The knight rook-pawn blockade remains visible because"} the knight still covers the promotion square and keeps the rook pawn fixed.")
      case pattern if pattern.equalsIgnoreCase("QueenVsAdvancedPawn") =>
        Some(s"${if transitioned then "The queen-versus-pawn checking setup is now visible because" else "The queen-versus-pawn checking setup remains visible because"} the queen side still keeps checking distance against the advanced pawn.")
      case pattern if pattern.equalsIgnoreCase("TarraschDefenseActive") =>
        Some(s"${if transitioned then "The active rook-behind-pawn setup is now visible because" else "The active rook-behind-pawn setup remains visible because"} the rook remains active behind the pawn with checking play available.")
      case pattern if pattern.equalsIgnoreCase("PassiveRookDefense") =>
        Some(s"${if transitioned then "The passive rook setup is now visible because" else "The passive rook setup remains visible because"} the rook still stays behind the pawn and keeps the file blocked.")
      case pattern if pattern.equalsIgnoreCase("RookAndBishopVsRookDraw") =>
        Some(s"${if transitioned then "The rook-and-bishop-versus-rook defensive geometry is now visible because" else "The rook-and-bishop-versus-rook defensive geometry remains visible because"} the defender still has checking distance or corner geometry against mating nets.")
      case pattern if pattern.equalsIgnoreCase("SameColoredBishopsBlockade") =>
        Some(s"${if transitioned then "The same-colored bishops blockade is now visible because" else "The same-colored bishops blockade remains visible because"} the defender still locks the pawns on the bishop's shared color complex.")
      case _ => None
    }

  private def lucenaLossClause(board: Board): Option[String] =
    lucenaFrame(board, strict = false).flatMap { frame =>
      val promo = promotionSquare(frame.pawn, frame.attacker)
      promo.flatMap { promotion =>
        if chebyshev(frame.attackerKing, promotion) > 1 then
          Some("The bridge-building rook-pawn geometry has loosened because the stronger king is no longer beside the promotion square.")
        else if chebyshev(frame.defenderKing, promotion) < 2 then
          Some("The bridge-building rook-pawn geometry has loosened because the defending king has reached the promotion-square zone.")
        else if frame.attackerRook.file == frame.pawn.file then
          Some("The bridge-building rook-pawn geometry has loosened because the rook has fallen onto the pawn file instead of building a bridge from the side.")
        else if fileDistance(frame.attackerRook.file, frame.pawn.file) < 2 then
          Some("The bridge-building rook-pawn geometry has loosened because the rook no longer has the lateral bridge-building distance.")
        else None
      }
    }.orElse(
      Some("The bridge-building rook-pawn geometry has loosened because the stronger side no longer keeps the promotion-square king plus bridge-building rook setup.")
    )

  private def lucenaHoldClause(board: Board, transitioned: Boolean): Option[String] =
    lucenaFrame(board, strict = true).map { frame =>
      val opener =
        if transitioned then "The bridge-building rook-pawn geometry is now visible because"
        else "The bridge-building rook-pawn geometry remains visible because"
      val kingCutoff =
        if promotionSquare(frame.pawn, frame.attacker).exists(promo => chebyshev(frame.defenderKing, promo) >= 2) then
          " and the defending king is still cut off from the promotion square"
        else ""
      s"$opener the stronger king stays beside the promotion square, the rook keeps bridge-building distance$kingCutoff."
    }

  private def philidorLossClause(board: Board): Option[String] =
    philidorFrame(board, strict = false).flatMap { frame =>
      if !isPhilidorBarrierHeld(frame) then
        Some("The barrier-rank rook defense has loosened because the defending rook has left the barrier rank.")
      else if !isPhilidorDefenderKingAhead(frame) then
        Some("The barrier-rank rook defense has loosened because the defending king is no longer in front of the pawn.")
      else if !isPhilidorAttackingKingBehind(frame) then
        Some("The barrier-rank rook defense has loosened because the stronger king has crossed the barrier.")
      else None
    }.orElse(
      Some("The barrier-rank rook defense has loosened because the barrier-rank defense has been lost.")
    )

  private def philidorHoldClause(board: Board, transitioned: Boolean): Option[String] =
    philidorFrame(board, strict = true).map { frame =>
      val opener =
        if transitioned then "The barrier-rank rook defense is now visible because"
        else "The barrier-rank rook defense remains visible because"
      val kingFront =
        if isPhilidorDefenderKingAhead(frame) then " and the defending king stays in front of the pawn" else ""
      s"$opener the rook still guards the barrier rank, and the stronger king has not crossed it$kingFront."
    }

  private def vancuraLossClause(board: Board): Option[String] =
    vancuraFrame(board, strict = false).flatMap { frame =>
      if relativeRank(frame.pawn, frame.attacker) != 6 || !isFlankPawn(frame.pawn) then
        Some("The side-checking rook defense has loosened because the pawn is no longer the sixth-rank rook pawn required for the side-checking setup.")
      else if frame.defenderRook.rank != frame.pawn.rank then
        Some("The side-checking rook defense has loosened because the defending rook has left the pawn's rank, so the side-checking setup is gone.")
      else if fileDistance(frame.defenderRook.file, frame.pawn.file) < 1 then
        Some("The side-checking rook defense has loosened because the defending rook has no side-checking room.")
      else if isRookBehindPawn(board, frame.attacker, frame.pawn) then
        Some("The side-checking rook defense has loosened because the attacking rook has reached a behind-the-pawn setup.")
      else None
    }.orElse {
      advancedFlankPawnFrame(board).flatMap { frame =>
        if relativeRank(frame.pawn, frame.attacker) != 6 then
          Some("The side-checking rook defense has loosened because the pawn is no longer the sixth-rank rook pawn required for the side-checking setup.")
        else if frame.defenderRook.rank != frame.pawn.rank then
          Some("The side-checking rook defense has loosened because the defending rook has left the pawn's rank, so the side-checking setup is gone.")
        else if fileDistance(frame.defenderRook.file, frame.pawn.file) < 1 then
          Some("The side-checking rook defense has loosened because the defending rook has no side-checking room.")
        else if isRookBehindPawn(board, frame.attacker, frame.pawn) then
          Some("The side-checking rook defense has loosened because the attacking rook has reached a behind-the-pawn setup.")
        else None
      }
    }.orElse(
      Some("The side-checking rook defense has loosened because the rook has lost the side-checking formation on the pawn's rank.")
    )

  private def vancuraHoldClause(board: Board, transitioned: Boolean): Option[String] =
    vancuraFrame(board, strict = true).map { _ =>
      val opener =
        if transitioned then "The side-checking rook defense is now visible because"
        else "The side-checking rook defense remains visible because"
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
    val evidence = ctx.candidates.flatMap(_.tacticEvidence.flatMap(tacticEvidenceMotifs))
    val conceptMotifs = semantic.flatMap(_.conceptSummary).flatMap(conceptToMotif)
    (positional ++ weaknesses ++ evidence ++ conceptMotifs)
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct

  /** Maps conceptSummary labels to motif IDs that canonicalTermForMotif can resolve. */
  private def conceptToMotif(concept: String): Option[String] =
    if endgameContextOnlyConcept(concept) then None
    else
      val low = concept.trim.toLowerCase.replaceAll("[\\s_-]+", "_")
      if
        RelationObservationCatalog.relationWitnessOnlyMotifTag(concept) ||
          RelationObservationCatalog.pvDrawResourceOnlyMotifTag(concept)
      then None
      else if low.contains("stalemate") then Some("stalemate_trick")
      else if low.contains("repetition") || low.contains("repeat") then Some("repetition_threat")
      else if low.contains("perpetual") then Some("perpetual_check")
      else if low.contains("fortress") then Some("fortress")
      else if low.contains("zugzwang") then Some("zugzwang")
      else None

  private def endgameContextOnlyConcept(raw: String): Boolean =
    val low = raw.trim.toLowerCase.replaceAll("[\\s_-]+", "_")
    low == "zugzwang_pressure" ||
      low == "key_square_control" ||
      low == "king_advancing" ||
      low == "king_retreating" ||
      low == "rule_of_the_square_holds" ||
      low == "rule_of_the_square_fails" ||
      low == "rook_behind_passed_pawn" ||
      low == "king_cut_off" ||
      low.endsWith("_opposition") ||
      low.startsWith("endgame_pattern") ||
      low.startsWith("endgame_developing") ||
      low.startsWith("endgame_continuation") ||
      low.startsWith("endgame_sustained") ||
      low.startsWith("pattern_shift")

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
      val fromConceptSummary = conceptSummarySignals.exists(sig => motifSignalMatches(sig, motif))
      val fromDerived = derivedSignals.exists(sig => motifSignalMatches(sig, motif))
      val corroboratedByBoard =
        keyFact.exists(f => CommentaryIdeaSurface.motifCorroboratedByFact(motif, f)) ||
          motifCorroboratedByThreat(motif, threatsToUs) ||
          motifCorroboratedByPawnPlay(motif, pawnPlay)

      fromDelta || fromDerived || (fromConceptSummary && corroboratedByBoard)

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

  private def motifCorroboratedByThreat(motif: String, threatsToUs: List[ThreatRow]): Boolean =
    val tacticalMotif = motifMatchesAny(motif, ThreatCorroboratedTacticalMotifs)
    val strategicMotif = motifMatchesAny(motif, ThreatCorroboratedStrategicMotifs)
    threatsToUs.exists(threatCorroboratesMotif(_, tacticalMotif, strategicMotif))

  private def motifMatchesAny(motif: String, candidates: List[String]): Boolean =
    candidates.exists(motif.contains)

  private def threatCorroboratesMotif(
    threat: ThreatRow,
    tacticalMotif: Boolean,
    strategicMotif: Boolean
  ): Boolean =
    val kind = threat.kind.toLowerCase
    val urgent = threat.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP || kind.contains("mate")
    (tacticalMotif && (kind.contains("mate") || kind.contains("material") || urgent)) ||
      (strategicMotif && (kind.contains("positional") || urgent))

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
        val playedNormUci = normalizeMoveUci(playedUci)
        val playedNormSan = normalizeMoveToken(playedSan)
        ranked.collectFirst {
          case v if variationMatchesMove(v, playedNormUci, playedNormSan) =>
            v.effectiveScore
        }
      }
    yield cpLossForSideToMove(ctx.fen, best.effectiveScore, playedScore)

  private def playedMoveRank(
    ctx: NarrativeContext,
    playedUci: Option[String],
    playedSan: String
  ): Option[Int] =
    val playedNormUci = normalizeMoveUci(playedUci)
    val playedNormSan = normalizeMoveToken(playedSan)
    rankedEngineVariations(ctx).zipWithIndex.collectFirst {
      case (v, i) if variationMatchesMove(v, playedNormUci, playedNormSan) =>
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
    urgentBoardThreat(ctx).map(threatBoardAnchor(_, bead))
      .orElse(factBoardAnchor(ctx, keyFact, bead))
      .orElse(pawnBreakBoardAnchor(ctx, bead))

  private def urgentBoardThreat(ctx: NarrativeContext): Option[ThreatRow] =
    ctx.threats.toUs
      .sortBy(t => -t.lossIfIgnoredCp)
      .headOption
      .filter(t => t.kind.equalsIgnoreCase("Mate") || t.lossIfIgnoredCp >= 80)

  private def threatBoardAnchor(t: ThreatRow, bead: Int): BoardAnchor =
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

  private def factBoardAnchor(
    ctx: NarrativeContext,
    keyFact: Option[Fact],
    bead: Int
  ): Option[BoardAnchor] =
    keyFact.flatMap { fact =>
      val text = NarrativeLexicon.getFactStatement(bead ^ 0x3c79ac49, fact, ctx).trim
      Option.when(text.nonEmpty)(BoardAnchor(text = text, consumedFact = true))
    }

  private def pawnBreakBoardAnchor(ctx: NarrativeContext, bead: Int): Option[BoardAnchor] =
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

  private def buildIssueConsequence(
    bead: Int,
    cpLoss: Int,
    bestReply: Option[String],
    threatsToUs: List[ThreatRow],
    playedCand: Option[CandidateInfo]
  ): Option[String] =
    val threatConsequence = threatIssueConsequence(threatsToUs)
    val factConsequence =
      playedCand.flatMap(_.facts.iterator.flatMap(CommentaryIdeaSurface.issueConsequence).toList.headOption)
    val replyConsequence = replyIssueConsequence(cpLoss, bestReply)
    val severityConsequence = severityIssueConsequence(bead, cpLoss)

    factConsequence
      .orElse(threatConsequence)
      .orElse(replyConsequence)
      .orElse(severityConsequence)

  private def threatIssueConsequence(threatsToUs: List[ThreatRow]): Option[String] =
    threatsToUs.find(_.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP).map { t =>
      val kind = t.kind.toLowerCase
      if kind.contains("mate") then "Consequence: king safety deteriorates immediately and the attack becomes forcing."
      else if kind.contains("material") then "Consequence: material pressure becomes harder to contain in practical play."
      else "Consequence: the opponent dictates the play while your pieces are tied to defense."
    }

  private def replyIssueConsequence(
      cpLoss: Int,
      bestReply: Option[String]
  ): Option[String] =
    Option.when(cpLoss >= Thresholds.INACCURACY_CP) {
      bestReply.filter(isForcingReplySan).map { r =>
        if cpLoss >= Thresholds.BLUNDER_CP then
          s"Consequence: the opponent has ...$r by force, and the tactical damage is immediate."
        else s"Consequence: the opponent can answer with ...$r and seize the initiative."
      }
    }.flatten

  private def severityIssueConsequence(
      bead: Int,
      cpLoss: Int
  ): Option[String] =
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

  private def isForcingReplySan(reply: String): Boolean =
    val r = Option(reply).getOrElse("").trim
    r.nonEmpty && (r.contains("+") || r.contains("#") || r.contains("x"))

  private def buildInvestmentAnnotationDetail(
      ctx: NarrativeContext,
      contract: DecisiveTruthContract
  ): Option[String] =
    val prefix = investmentAnnotationPrefix(contract)
    contract.verifiedPayoffAnchor
      .map(carrier => investmentAnnotationSentence(prefix, carrier))
      .orElse(semanticInvestmentCarrier(ctx).map(carrier => investmentAnnotationSentence(prefix, carrier)))

  private def investmentAnnotationPrefix(contract: DecisiveTruthContract): String =
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

  private def investmentAnnotationSentence(prefix: String, carrier: String): String =
    s"$prefix $carrier."

  private def semanticInvestmentCarrier(ctx: NarrativeContext): Option[String] =
    CompensationInterpretation
      .effectiveSemanticDecision(ctx)
      .filter(_.decision.accepted)
      .flatMap { semanticDecision =>
        normalizedInvestmentCarrier(semanticDecision.compensation.conversionPlan)
          .orElse(semanticInvestmentReturnVectorCarrier(semanticDecision.compensation.returnVector))
      }

  private def semanticInvestmentReturnVectorCarrier(returnVector: Map[String, Double]): Option[String] =
    returnVector.toList
      .sortBy { case (_, weight) => -weight }
      .map(_._1)
      .flatMap(normalizedInvestmentCarrier)
      .headOption

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
    val trimmed = text.trim
    if trimmed.isEmpty then emptyAnnotationProjection
    else if isBest || cpLoss <= 35 then
      annotationProjection(softenNearBestTone(trimmed).trim)
    else if cpLoss >= Thresholds.INACCURACY_CP && !containsNegativeTone(text) then
      annotationProjection(trimmed, severityTail = Some(annotationSeverityTail(text, cpLoss, contextHint)))
    else
      annotationProjection(trimmed)

  private def emptyAnnotationProjection: AnnotationTextProjection =
    AnnotationTextProjection(
      coreText =
        AuthorityTaggedFragment(
          "",
          FragmentAuthority.requires_move_linked_anchor,
          moveLinkedAnchor = true,
          sceneGrounded = true
        )
    )

  private def annotationProjection(
    coreText: String,
    severityTail: Option[AuthorityTaggedFragment] = None
  ): AnnotationTextProjection =
    AnnotationTextProjection(
      coreText =
        AuthorityTaggedFragment(
          coreText,
          FragmentAuthority.requires_move_linked_anchor,
          moveLinkedAnchor = true,
          sceneGrounded = true,
          generalized = true
        ),
      severityTail = severityTail
    )

  private def annotationSeverityTail(text: String, cpLoss: Int, contextHint: Int): AuthorityTaggedFragment =
    AuthorityTaggedFragment(
      buildSeverityTail(Math.abs(text.hashCode) ^ 0x239b961b, cpLoss, contextHint),
      FragmentAuthority.unsafe_as_truth
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
      val softenedPositive = annotationPolarityCoreText(text.coreText.rawText, nearBest, severeError)
      val severityTail =
        enforcedAnnotationSeverityTail(text, softenedPositive, severeError, cpLoss, contextHint)
      AnnotationTextProjection(
        coreText = annotationProjection(softenedPositive.trim).coreText,
        severityTail = severityTail.filter(_.hasRawText)
      )

  private def annotationPolarityCoreText(text: String, nearBest: Boolean, severeError: Boolean): String =
    val neutralized =
      if nearBest then neutralizeBenchmarkNegativeLexicon(text)
      else text
    if severeError && containsBenchmarkStrongPositiveLexicon(text) then
      neutralizeBenchmarkStrongPositiveLexicon(neutralized)
    else neutralized

  private def enforcedAnnotationSeverityTail(
    projection: AnnotationTextProjection,
    coreText: String,
    severeError: Boolean,
    cpLoss: Int,
    contextHint: Int
  ): Option[AuthorityTaggedFragment] =
    if severeError && !containsBenchmarkNegativeLexicon(coreText) then
      projection.severityTail.orElse(
        Some(
          AuthorityTaggedFragment(
            buildSeverityTail(Math.abs(coreText.hashCode) ^ 0x6f4b1321, cpLoss, contextHint),
            FragmentAuthority.unsafe_as_truth
          )
        )
      )
    else projection.severityTail

  private def buildSeverityTail(bead: Int, cpLoss: Int, contextHint: Int): String =
    val seed = bead ^ contextHint ^ Math.abs(cpLoss) ^ 0x5bd1e995
    val templates =
      Thresholds.classifySeverity(cpLoss) match
        case "blunder" =>
          List(
            "This is a blunder, so forcing control shifts to the opponent.",
            "Because this blunder loosens coordination, the opponent gets a direct technical route.",
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
            "This mistake yields an easier technical plan, because your coordination is slower.",
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
    Option(text).exists(t => NegativeLexiconPattern.findFirstIn(t.toLowerCase).isDefined)

  private def containsBenchmarkStrongPositiveLexicon(text: String): Boolean =
    val low = text.toLowerCase
    List("best move", "excellent choice", "strong move", "very accurate", "precise move", "fully sound")
      .exists(low.contains)

  private def neutralizeBenchmarkNegativeLexicon(text: String): String =
    NegativeLexiconReplacements.foldLeft(text) { case (acc, (pattern, replacement)) =>
      pattern.replaceAllIn(acc, replacement)
    }

  private def neutralizeBenchmarkStrongPositiveLexicon(text: String): String =
    StrongPositiveLexiconReplacements.foldLeft(text) { case (acc, (pattern, replacement)) =>
      pattern.replaceAllIn(acc, replacement)
    }

  private def extractFactConsequence(candidate: CandidateInfo, currentPly: Int): Option[String] =
    val facts = candidate.facts
    val prioritized = facts.sortBy(CommentaryIdeaSurface.factPriority)

    prioritized.collectFirst(Function.unlift { fact =>
      val body = CommentaryIdeaSurface.consequenceBody(fact)
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

  private def counterfactualTeachingSentence(
    ctx: NarrativeContext,
    cf: lila.commentary.model.strategic.CounterfactualMatch,
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
              case Some(Motif.Capture(_, captured, _, lila.commentary.model.Motif.CaptureType.Winning, _, _, _, _)) =>
                s"winning the ${captured.toString.toLowerCase} becomes available"
              case Some(Motif.DiscoveredAttack(_, _, target, _, _, _, _, _, _)) =>
                s"the discovered attack on the ${target.toString.toLowerCase} appears"
              case Some(m) =>
                s"the idea of ${NarrativeUtils.humanize(motifName(m))} appears"
              case None =>
                "the practical refutation becomes clear"
          LineScopedCitation.afterClause(cited, body)
    }

  private def defaultIssueBySeverity(bead: Int, cpLoss: Int): String =
    Thresholds.classifySeverity(cpLoss) match
      case "blunder" =>
        NarrativeLexicon.pick(bead, List(
          "this allows a forcing tactical sequence against your king or material",
          "this collapses coordination and gives the opponent a direct technical route",
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

  private def motifName(m: lila.commentary.model.Motif): String =
    m.getClass.getSimpleName.replaceAll("\\$", "")

  private def joinWithOr(items: List[String]): String =
    items match
      case Nil => ""
      case one :: Nil => one
      case a :: b :: Nil => s"$a or $b"
      case xs => xs.dropRight(1).mkString(", ") + s", or ${xs.last}"

  private def variationReplySan(fen: String, variation: VariationLine): Option[String] =
    variation.theirReply
      .map(_.san.trim)
      .filter(_.nonEmpty)
      .orElse {
        (for
          first <- variation.moves.headOption
          reply <- variation.moves.lift(1)
          afterFirst <- MoveReviewPvLine.legalFenAfter(fen, first)
        yield NarrativeUtils.uciToSanOrFormat(afterFirst, reply).trim)
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
    variationMatchesMove(variation, normalizeMoveUci(candidate.uci), normalizeMoveToken(candidate.move))

  private def variationMatchesMove(
    variation: VariationLine,
    normalizedUci: Option[String],
    normalizedSan: String
  ): Boolean =
    normalizedUci.exists(u => variation.moves.headOption.exists(m => NarrativeUtils.uciEquivalent(m, u))) ||
      (normalizedSan.nonEmpty && variation.ourMove.exists(m => normalizeMoveToken(m.san) == normalizedSan))

  private def normalizeMoveUci(raw: Option[String]): Option[String] =
    raw.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)

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
    val contrastSeed = alternativeEngineContrastSeed(signal, bead)
    val preferPracticalWording = role.equalsIgnoreCase("practical_secondary")
    val base = line.trim
    selectAlternativeEngineContrast(
      signal = signal,
      seed = contrastSeed,
      preferPracticalWording = preferPracticalWording,
      usedStems = usedStems,
      prefixCounts = prefixCounts
    ) match
      case Some(extra) if base.nonEmpty => normalizeAlternativeTemplateLine(s"$base $extra", bead)
      case _                            => normalizeAlternativeTemplateLine(base, bead)

  private def alternativeEngineContrastSeed(signal: AlternativeEngineSignal, bead: Int): Int =
    bead ^
      (signal.rank.getOrElse(0) * 0x45d9f3b) ^
      (signal.cpLoss.getOrElse(0) * 131) ^
      (Math.abs(signal.bestSan.getOrElse("").hashCode) * 17)

  private def shouldAttachAlternativeEngineContrast(signal: AlternativeEngineSignal, seed: Int): Boolean =
    signal.rank.exists {
      case r if r >= 3 => true
      case 2           => signal.cpLoss.exists(_ >= 25) || Math.floorMod(seed, 3) != 0
      case _           => false
    }

  private def selectAlternativeEngineContrast(
    signal: AlternativeEngineSignal,
    seed: Int,
    preferPracticalWording: Boolean,
    usedStems: Set[String],
    prefixCounts: Map[String, Int]
  ): Option[String] =
    val templates =
      if shouldAttachAlternativeEngineContrast(signal, seed) then alternativeContrastTemplates(signal, preferPracticalWording)
      else Nil
    Option.when(templates.nonEmpty) {
      selectNonRepeatingTemplate(
        templates = templates,
        seed = seed ^ 0x11f17f1d,
        usedStems = usedStems,
        prefixCounts = prefixCounts,
        prefixLimits = PrefixFamilyLimits
      )
    }

  private def alternativeContrastTemplates(signal: AlternativeEngineSignal, preferPracticalWording: Boolean): List[String] =
    signal.rank match
      case Some(2) =>
        val bestRef = signal.bestSan.map(s => s"**$s**").getOrElse("the top engine move")
        rankTwoContrastTemplates(bestRef, signal.cpLoss, preferPracticalWording)
      case Some(r) if r >= 3 =>
        val bestRef = signal.bestSan.map(s => s"**$s**").getOrElse("the primary reference line")
        lowerRankContrastTemplates(r, bestRef, signal.cpLoss, preferPracticalWording)
      case _ => Nil

  private def rankTwoContrastTemplates(bestRef: String, cpLoss: Option[Int], preferPracticalWording: Boolean): List[String] =
    cpLoss match
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
        val gap = formatCpGap(loss)
        if preferPracticalWording then
          List(
            s"Compared with $bestRef, this concedes about $gap in practical terms.",
            s"The practical cost versus $bestRef is roughly $gap.",
            s"Against $bestRef, the score gap is about $gap."
          )
        else
          List(
            s"The engine still points to $bestRef as cleaner, by about $gap.",
            s"In engine terms, $bestRef holds roughly a $gap edge.",
            s"The practical gap to $bestRef is around $gap.",
            s"Engine preference remains with $bestRef, with roughly a $gap edge in practical terms.",
            s"Compared with $bestRef, engine evaluation drops by roughly $gap."
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

  private def lowerRankContrastTemplates(
    rank: Int,
    bestRef: String,
    cpLoss: Option[Int],
    preferPracticalWording: Boolean
  ): List[String] =
    val rankLabel = ordinal(rank)
    cpLoss match
      case Some(loss) if loss <= 70 =>
        val gap = formatCpGap(loss)
        if preferPracticalWording then
          List(
            s"As a $rankLabel practical-tier choice, this trails $bestRef by about $gap.",
            s"As a $rankLabel option, this line trails $bestRef by around $gap.",
            s"The $rankLabel choice is workable, but $bestRef still leads by roughly $gap."
          )
        else
          List(
            s"Engine ranking places this around $rankLabel, with $bestRef ahead by about $gap.",
            s"This continuation stays in the $rankLabel engine group, while $bestRef keeps roughly a $gap edge.",
            s"In engine order, $bestRef remains first and this $rankLabel choice trails by around $gap."
          )
      case Some(loss) =>
        val gap = formatCpGap(loss)
        if preferPracticalWording then
          List(
            s"As a lower-tier option (around $rankLabel), this trails $bestRef by roughly $gap.",
            s"The score gap to $bestRef is substantial here (about $gap).",
            s"This $rankLabel line leaves a large practical deficit versus $bestRef (about $gap)."
          )
        else
          List(
            s"This sits in a lower engine tier (about $rankLabel), and $bestRef leads by roughly $gap.",
            s"Engine ranking is clear here: around $rankLabel for this line, while $bestRef is ahead by $gap.",
            s"In engine terms this continuation drops to about $rankLabel, with $bestRef up by $gap.",
            s"Around $bestRef, the principal engine route stays cleaner; this $rankLabel option is behind by about $gap."
          )
      case None =>
        if preferPracticalWording then
          List(
            s"This sits in a lower practical tier (around $rankLabel), while $bestRef remains the benchmark.",
            s"$bestRef stays the stable reference line, with this branch in a lower tier.",
            s"As a $rankLabel option, this is less reliable than $bestRef."
          )
        else
          List(
            s"This is a lower-ranked engine option (around $rankLabel), while $bestRef remains the stable benchmark.",
            s"Engine ordering puts this below the principal choices; $bestRef is the cleaner reference line.",
            s"The sampled engine set keeps this in a lower tier, with $bestRef as the anchor line."
          )

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
    val implication = selectNonRepeatingTemplate(
      templates = strategicImplicationTemplates(move, role, plan, diffLabel).map(_.trim).filter(_.nonEmpty).distinct,
      seed = strategicImplicationSeed(move, signal, bead),
      usedStems = usedStems,
      prefixCounts = prefixCounts,
      prefixLimits = PrefixFamilyLimits
    )
    appendImplicationLine(line, implication)

  private def strategicImplicationTemplates(
    move: String,
    role: String,
    plan: String,
    diffLabel: String
  ): List[String] =
    val planHint = alternativePlanLabel(plan).map(p => s" around $p").getOrElse("")
    val practicalHint = informativeAlternativeDifficulty(diffLabel).map(p => s" The practical burden is $p.").getOrElse("")
    if role.equalsIgnoreCase("engine_primary") then
      List(
        s"With **$move**, the technical route can stay smoother$planHint, but initiative around **$move** can swing when **$move** hands away a tempo.",
        s"Handled precisely, **$move** keeps piece harmony and king cover aligned$planHint through the next phase.",
        s"From a practical handling view, **$move** stays reliable$planHint when defensive timing and coverage stay coordinated.",
        s"**$move** keeps practical burden manageable$planHint by preserving coordination before exchanges."
      )
    else
      List(
        s"In practical terms, **$move** is judged by technical ease$planHint, because defensive coordination can diverge quickly.$practicalHint",
        s"After **$move**, king safety and tempo stay linked, so one inaccurate sequence can hand over initiative$planHint.$practicalHint",
        s"With **$move**, a move-order slip can expose coordination gaps$planHint, and recovery windows are short.$practicalHint",
        s"After **$move**, sequence accuracy matters because coordination and activity can separate quickly$planHint.$practicalHint",
        s"Strategically, **$move** needs connected follow-up through the next phase$planHint, or initiative control leaks away.$practicalHint"
      )

  private def strategicImplicationSeed(move: String, signal: AlternativeEngineSignal, bead: Int): Int =
    bead ^ (signal.rank.getOrElse(0) * 0x7f4a7c15) ^ Math.abs(move.hashCode)

  private def appendImplicationLine(line: String, implication: String): String =
    if line.trim.isEmpty then implication.trim
    else s"${line.trim} ${implication.trim}".trim

  private def preferredIntent(c: CandidateInfo): String =
    c.structureGuidance
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(c.planAlignment)

  private def alternativePlanLabel(plan: String): Option[String] =
    val cleaned = Option(plan).getOrElse("").replaceAll("""[_\-]+""", " ").trim.toLowerCase
    Option.when(cleaned.nonEmpty && !GenericAlternativePlans.contains(cleaned))(cleaned)

  private def alternativeDifficultyLabel(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""[_\-]+""", " ").trim.toLowerCase

  private def informativeAlternativeDifficulty(label: String): Option[String] =
    Option.when(
      label.nonEmpty &&
        label != "unknown" &&
        InformativeAlternativeDifficultyTerms.exists(label.contains)
    )(label)

  private def alternativeFamilyOrder(
    c: CandidateInfo,
    alignmentBand: Option[String],
    rawReason: Option[String],
    diffLabel: String
  ): List[String] =
    if alignmentBand.contains("offplan") then List("tradeoff", "practical", "strategic", "generic")
    else if alignmentBand.contains("onbook") then List("technical", "strategic", "practical", "generic")
    else if rawReason.nonEmpty then List("tradeoff", "practical", "strategic", "generic")
    else if alignmentBand.contains("unknown") then List("practical", "strategic", "generic")
    else if c.tags.contains(CandidateTag.Sharp) || c.tags.contains(CandidateTag.TacticalGamble) || diffLabel.contains("complex") then
      List("dynamic", "strategic", "practical", "generic")
    else if c.tags.contains(CandidateTag.Solid) || c.tags.contains(CandidateTag.Converting) || diffLabel.contains("clean") then
      List("technical", "strategic", "practical", "generic")
    else
      List("strategic", "practical", "dynamic", "technical", "generic")

  private def alternativeBandHint(alignmentBand: Option[String]): String =
    alignmentBand match
      case Some("offplan") => "The structural route is fragile unless the follow-up is precise."
      case Some("unknown") => "Structural read is uncertain, so concrete verification is essential."
      case Some("onbook") => "The continuation stays structurally coherent with accurate handling."
      case _ => ""

  private def alternativeBaseTemplates(
    c: CandidateInfo,
    family: String,
    move: String,
    plan: String,
    reason: Option[String],
    practicalHint: String,
    localBead: Int
  ): List[String] =
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
          s"**$move** aims for a technically manageable position with clear follow-up paths.",
          s"**$move** keeps the game in a technical channel where precise handling is rewarded."
        )
      case "strategic" =>
        val planHint = alternativePlanLabel(plan).map(p => s" around $p").getOrElse("")
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
    val material = alternativeDiversifiedMaterial(c, idx, bead, usedFamilies)
    val line = alternativeBaseLine(c, material, usedStems, prefixCounts)
    val contrasted = appendAlternativeDiversifiedContrast(line, material, signal, usedStems, prefixCounts, role)
    val withImplication = appendAlternativeDiversifiedImplication(contrasted, material, signal, usedStems, prefixCounts, role)
    (appendAlternativeBandHint(withImplication, material.bandHint), material.family)

  private def appendAlternativeDiversifiedContrast(
    line: String,
    material: AlternativeDiversifiedMaterial,
    signal: AlternativeEngineSignal,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    role: String
  ): String =
    appendAlternativeEngineContrast(
      line = line,
      signal = signal,
      bead = material.localBead ^ 0x63d5a6f1,
      usedStems = usedStems + normalizeStem(line),
      prefixCounts = prefixCounts,
      role = role
    )

  private def appendAlternativeDiversifiedImplication(
    line: String,
    material: AlternativeDiversifiedMaterial,
    signal: AlternativeEngineSignal,
    usedStems: Set[String],
    prefixCounts: Map[String, Int],
    role: String
  ): String =
    appendStrategicImplication(
      line = line,
      move = material.move,
      role = role,
      plan = material.plan,
      diffLabel = material.diffLabel,
      signal = signal,
      bead = material.localBead ^ 0x6d2b79f5,
      usedStems = usedStems + normalizeStem(line),
      prefixCounts = prefixCounts
    )

  private def alternativeDiversifiedMaterial(
    c: CandidateInfo,
    idx: Int,
    bead: Int,
    usedFamilies: Set[String]
  ): AlternativeDiversifiedMaterial =
    val rawReason = c.whyNot.flatMap(humanizeWhyNot).map(_.trim).filter(_.nonEmpty).map(_.stripSuffix("."))
    val move = c.move.trim
    val plan = preferredIntent(c).trim.toLowerCase
    val alignmentBand = c.alignmentBand.map(_.trim.toLowerCase)
    val diffLabel = alternativeDifficultyLabel(c.practicalDifficulty)
    val practicalHint = informativeAlternativeDifficulty(diffLabel).map(p => s" Practical burden: $p.").getOrElse("")
    val bandHint = alternativeBandHint(alignmentBand)
    val preferredFamilies = alternativeFamilyOrder(c, alignmentBand, rawReason, diffLabel)

    val family = preferredFamilies.find(f => !usedFamilies.contains(f)).getOrElse(preferredFamilies.headOption.getOrElse("generic"))
    val localBead = bead ^ Math.abs(move.hashCode) ^ (idx + 1) * 0x9e3779b9
    val reason = rawReason.map(r => diversifyAlternativeReason(r, localBead))
    AlternativeDiversifiedMaterial(
      move = move,
      plan = plan,
      diffLabel = diffLabel,
      practicalHint = practicalHint,
      bandHint = bandHint,
      family = family,
      reason = reason,
      localBead = localBead
    )

  private def alternativeBaseLine(
    c: CandidateInfo,
    material: AlternativeDiversifiedMaterial,
    usedStems: Set[String],
    prefixCounts: Map[String, Int]
  ): String =
    val baseTemplates =
      alternativeBaseTemplates(
        c = c,
        family = material.family,
        move = material.move,
        plan = material.plan,
        reason = material.reason,
        practicalHint = material.practicalHint,
        localBead = material.localBead
      )
    selectNonRepeatingTemplate(
      templates = baseTemplates.map(_.trim).filter(_.nonEmpty).distinct,
      seed = material.localBead ^ 0x3124bcf5,
      usedStems = usedStems,
      prefixCounts = prefixCounts,
      prefixLimits = PrefixFamilyLimits
    )

  private def appendAlternativeBandHint(line: String, bandHint: String): String =
    if bandHint.nonEmpty then s"${line.trim} $bandHint".trim
    else line

  private def alternativesRepetitionPenalty(lines: List[String]): Int =
    val stems = lines.map(normalizeStem).filter(_.nonEmpty)
    val stemPenalty =
      stems.groupBy(identity).valuesIterator.map(c => (c.size - 1).max(0) * 15).sum

    val ngramPenalty = lines.map(normalizeAlternativeTokens).map { tokens =>
      val tri = ngramRepeatPenalty(tokens, 3, threshold = 2, weight = 6)
      val four = ngramRepeatPenalty(tokens, 4, threshold = 2, weight = 10)
      tri + four
    }.sum

    stemPenalty + ngramPenalty

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
