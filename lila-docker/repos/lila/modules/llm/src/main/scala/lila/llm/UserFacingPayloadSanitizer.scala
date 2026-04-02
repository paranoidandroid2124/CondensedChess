package lila.llm

import lila.llm.analysis.UserFacingSignalSanitizer
import lila.llm.model.StrategicPlanExperiment
import lila.llm.model.authoring.PlanHypothesis
import lila.strategicPuzzle.StrategicPuzzle.*

object UserFacingPayloadSanitizer:

  def sanitize(response: CommentResponse): CommentResponse =
    val sanitizedPlans =
      response.mainStrategicPlans
        .filter(isProbeBackedPlan)
        .map(sanitizePlanHypothesis)
    val allowedPlanKeys = sanitizedPlans.map(planKey).toSet
    val allowedPlanIds = sanitizedPlans.map(planIdKey).toSet
    val allowedPlanNames = sanitizedPlans.map(_.planName.trim.toLowerCase).toSet
    response.copy(
      commentary = clean(response.commentary),
      concepts = cleanList(response.concepts),
      probeRequests = Nil,
      authorQuestions = Nil,
      authorEvidence = Nil,
      mainStrategicPlans = sanitizedPlans,
      strategicPlanExperiments =
        response.strategicPlanExperiments
          .filter(experiment => planExperimentAllowed(experiment.planId, experiment.subplanId, allowedPlanKeys, allowedPlanIds))
          .map(sanitizeStrategicPlanExperiment),
      planStateToken = response.planStateToken.filter(_ => sanitizedPlans.nonEmpty),
      strategyPack = response.strategyPack.flatMap(pack => sanitizeStrategyPack(pack, allowedPlanNames)),
      signalDigest = response.signalDigest.map(sanitizeSignalDigest),
      bookmakerLedger = response.bookmakerLedger.filter(_ => sanitizedPlans.nonEmpty).map(sanitizeBookmakerLedger)
    )

  def sanitize(response: GameChronicleResponse): GameChronicleResponse =
    val sanitizedMoments = response.moments.map(sanitizeMoment)
    val retainedPlanNames =
      sanitizedMoments
        .flatMap(_.mainStrategicPlans.map(_.planName))
        .flatMap(planName => cleanOpt(Some(planName)))
        .map(normalize)
        .toSet
    val sanitizedThemes = sanitizeChronicleThemes(response.themes, retainedPlanNames)
    response.copy(
      intro = clean(response.intro),
      moments = sanitizedMoments,
      conclusion = sanitizeChronicleConclusion(response.conclusion, response.themes, sanitizedThemes),
      themes = sanitizedThemes,
      strategicThreads = response.strategicThreads.flatMap(thread => sanitizeStrategicThread(thread, retainedPlanNames))
    )

  def sanitize(payload: BootstrapPayload): BootstrapPayload =
    val shell = sanitizeRuntimeShell(payload.runtimeShell)
    payload.copy(
      puzzle =
        payload.puzzle.copy(
          dominantFamily = payload.puzzle.dominantFamily.map(sanitizeDominantFamily),
          runtimeShell = payload.puzzle.runtimeShell.map(sanitizeRuntimeShell)
        ),
      runtimeShell = shell
    )

  private def sanitizeMoment(moment: GameChronicleMoment): GameChronicleMoment =
    val sanitizedPlans =
      moment.mainStrategicPlans
        .filter(isProbeBackedPlan)
        .map(sanitizePlanHypothesis)
    val allowedPlanKeys = sanitizedPlans.map(planKey).toSet
    val allowedPlanIds = sanitizedPlans.map(planIdKey).toSet
    val allowedPlanNames = sanitizedPlans.map(_.planName.trim.toLowerCase).toSet
    moment.copy(
      moveClassification = cleanOpt(moment.moveClassification),
      narrative = clean(moment.narrative),
      selectionLabel = cleanOpt(moment.selectionLabel),
      selectionReason = cleanOpt(moment.selectionReason),
      concepts = Nil,
      strategyPack = moment.strategyPack.flatMap(pack => sanitizeStrategyPack(pack, allowedPlanNames)),
      signalDigest = moment.signalDigest.map(sanitizeSignalDigest),
      authorQuestions = Nil,
      authorEvidence = Nil,
      mainStrategicPlans = sanitizedPlans,
      strategicPlanExperiments =
        moment.strategicPlanExperiments
          .filter(experiment => planExperimentAllowed(experiment.planId, experiment.subplanId, allowedPlanKeys, allowedPlanIds))
          .map(sanitizeStrategicPlanExperiment),
      activeStrategicNote = cleanOpt(moment.activeStrategicNote),
      activeStrategicIdeas = moment.activeStrategicIdeas.map(sanitizeActiveIdeaRef),
      activeStrategicRoutes = moment.activeStrategicRoutes.map(sanitizeActiveRouteRef),
      activeStrategicMoves = moment.activeStrategicMoves.map(sanitizeActiveMoveRef),
      activeDirectionalTargets = moment.activeDirectionalTargets.map(sanitizeDirectionalTarget),
      activeBranchDossier = moment.activeBranchDossier.map(sanitizeBranchDossier),
      strategicThread = moment.strategicThread.map(sanitizeThreadRef)
    )

  private def sanitizePlanHypothesis(plan: PlanHypothesis): PlanHypothesis =
    plan.copy(
      planName = clean(plan.planName),
      preconditions = cleanList(plan.preconditions),
      executionSteps = cleanList(plan.executionSteps),
      failureModes = cleanList(plan.failureModes),
      viability = plan.viability.copy(risk = clean(plan.viability.risk)),
      refutation = cleanOpt(plan.refutation),
      evidenceSources = Nil
    )

  private def sanitizeStrategicPlanExperiment(experiment: StrategicPlanExperiment): StrategicPlanExperiment =
    experiment.copy(themeL1 = clean(experiment.themeL1))

  private def sanitizeStrategyPack(
      pack: StrategyPack,
      allowedPlanNames: Set[String]
  ): Option[StrategyPack] =
    val sanitizedPlans =
      pack.plans
        .filter(plan => allowedPlanNames.contains(plan.planName.trim.toLowerCase))
        .map(sanitizeSidePlan)
    Option.when(sanitizedPlans.nonEmpty) {
      pack.copy(
        plans = sanitizedPlans,
        pieceRoutes = pack.pieceRoutes.map(sanitizePieceRoute),
        pieceMoveRefs = pack.pieceMoveRefs.map(sanitizePieceMoveRef),
        directionalTargets = pack.directionalTargets.map(sanitizeDirectionalTarget),
        longTermFocus = cleanList(pack.longTermFocus),
        evidence = cleanList(pack.evidence),
        signalDigest = pack.signalDigest.map(sanitizeSignalDigest)
      )
    }

  private def sanitizeSidePlan(plan: StrategySidePlan): StrategySidePlan =
    plan.copy(
      planName = clean(plan.planName),
      priorities = cleanList(plan.priorities),
      riskTriggers = cleanList(plan.riskTriggers)
    )

  private def sanitizePieceRoute(route: StrategyPieceRoute): StrategyPieceRoute =
    route.copy(
      purpose = clean(route.purpose),
      evidence = cleanList(route.evidence)
    )

  private def sanitizePieceMoveRef(moveRef: StrategyPieceMoveRef): StrategyPieceMoveRef =
    moveRef.copy(
      idea = clean(moveRef.idea),
      tacticalTheme = cleanOpt(moveRef.tacticalTheme),
      evidence = cleanList(moveRef.evidence)
    )

  private def sanitizeDirectionalTarget(target: StrategyDirectionalTarget): StrategyDirectionalTarget =
    target.copy(
      strategicReasons = cleanList(target.strategicReasons),
      prerequisites = cleanList(target.prerequisites),
      evidence = cleanList(target.evidence)
    )

  private def sanitizeSignalDigest(digest: NarrativeSignalDigest): NarrativeSignalDigest =
    digest.copy(
      opening = cleanOpt(digest.opening),
      strategicStack = Nil,
      latentPlan = None,
      latentReason = None,
      decisionComparison = digest.decisionComparison.map(sanitizeDecisionComparison),
      authoringEvidence = None,
      practicalVerdict = cleanOpt(digest.practicalVerdict),
      practicalFactors = cleanList(digest.practicalFactors),
      compensation = cleanOpt(digest.compensation),
      compensationVectors = cleanList(digest.compensationVectors),
      structuralCue = cleanOpt(digest.structuralCue),
      structureProfile = cleanOpt(digest.structureProfile),
      alignmentReasons = cleanList(digest.alignmentReasons),
      deploymentPurpose = None,
      deploymentContribution = None,
      prophylaxisPlan = None,
      prophylaxisThreat = None,
      dominantIdeaKind = None,
      dominantIdeaGroup = None,
      dominantIdeaReadiness = None,
      dominantIdeaFocus = None,
      secondaryIdeaKind = None,
      secondaryIdeaGroup = None,
      secondaryIdeaFocus = None,
      decision = None,
      strategicFlow = None,
      opponentPlan = None,
      preservedSignals = Nil,
      openingRelationClaim = cleanOpt(digest.openingRelationClaim),
      endgameTransitionClaim = cleanOpt(digest.endgameTransitionClaim)
    )

  private def sanitizeDecisionComparison(digest: DecisionComparisonDigest): DecisionComparisonDigest =
    digest.copy(
      chosenMove = cleanOpt(digest.chosenMove),
      engineBestMove = cleanOpt(digest.engineBestMove),
      deferredMove = None,
      deferredReason = None,
      deferredSource = None,
      evidence = None
    )

  private def sanitizeBookmakerLedger(ledger: BookmakerStrategicLedgerV1): BookmakerStrategicLedgerV1 =
    ledger.copy(
      motifLabel = clean(ledger.motifLabel),
      stageLabel = clean(ledger.stageLabel),
      stageReason = cleanOpt(ledger.stageReason),
      prerequisites = cleanList(ledger.prerequisites),
      conversionTrigger = cleanOpt(ledger.conversionTrigger),
      primaryLine = ledger.primaryLine.map(sanitizeLedgerLine),
      resourceLine = ledger.resourceLine.map(sanitizeLedgerLine)
    )

  private def sanitizeLedgerLine(line: BookmakerLedgerLineV1): BookmakerLedgerLineV1 =
    line.copy(
      title = clean(line.title),
      note = cleanOpt(line.note)
    )

  private def sanitizeActiveIdeaRef(ref: ActiveStrategicIdeaRef): ActiveStrategicIdeaRef =
    ref.copy(focusSummary = clean(ref.focusSummary))

  private def sanitizeActiveRouteRef(ref: ActiveStrategicRouteRef): ActiveStrategicRouteRef =
    ref.copy(purpose = clean(ref.purpose))

  private def sanitizeActiveMoveRef(ref: ActiveStrategicMoveRef): ActiveStrategicMoveRef =
    ref.copy(
      label = clean(ref.label),
      san = cleanOpt(ref.san)
    )

  private def sanitizeBranchDossier(dossier: ActiveBranchDossier): ActiveBranchDossier =
    dossier.copy(
      chosenBranchLabel = clean(dossier.chosenBranchLabel),
      engineBranchLabel = cleanOpt(dossier.engineBranchLabel),
      deferredBranchLabel = cleanOpt(dossier.deferredBranchLabel),
      whyChosen = cleanOpt(dossier.whyChosen),
      whyDeferred = cleanOpt(dossier.whyDeferred),
      opponentResource = cleanOpt(dossier.opponentResource),
      routeCue = dossier.routeCue.map(sanitizeRouteCue),
      moveCue = dossier.moveCue.map(sanitizeMoveCue),
      evidenceCue = cleanOpt(dossier.evidenceCue),
      continuationFocus = cleanOpt(dossier.continuationFocus),
      practicalRisk = cleanOpt(dossier.practicalRisk),
      threadLabel = cleanOpt(dossier.threadLabel),
      threadStage = cleanOpt(dossier.threadStage),
      threadSummary = cleanOpt(dossier.threadSummary),
      threadOpponentCounterplan = cleanOpt(dossier.threadOpponentCounterplan)
    )

  private def sanitizeRouteCue(cue: ActiveBranchRouteCue): ActiveBranchRouteCue =
    cue.copy(purpose = clean(cue.purpose))

  private def sanitizeMoveCue(cue: ActiveBranchMoveCue): ActiveBranchMoveCue =
    cue.copy(
      label = clean(cue.label),
      san = cleanOpt(cue.san),
      source = clean(cue.source)
    )

  private def sanitizeStrategicThread(
      thread: ActiveStrategicThread,
      retainedPlanNames: Set[String]
  ): Option[ActiveStrategicThread] =
    Option.when(retainedPlanNames.contains(normalize(thread.themeLabel))) {
      thread.copy(
        themeLabel = clean(thread.themeLabel),
        summary = "",
        opponentCounterplan = None
      )
    }

  private def sanitizeThreadRef(ref: ActiveStrategicThreadRef): ActiveStrategicThreadRef =
    ref.copy(
      themeLabel = clean(ref.themeLabel),
      stageLabel = clean(ref.stageLabel)
    )

  private def isProbeBackedPlan(plan: PlanHypothesis): Boolean =
    plan.evidenceSources.exists(_.trim.equalsIgnoreCase("probe_backed:validated_support"))

  private def sanitizeChronicleThemes(
      themes: List[String],
      retainedPlanNames: Set[String]
  ): List[String] =
    cleanList(themes).filter(theme => retainedPlanNames.contains(normalize(theme)))

  private def sanitizeChronicleConclusion(
      conclusion: String,
      originalThemes: List[String],
      sanitizedThemes: List[String]
  ): String =
    val cleaned = clean(conclusion)
    val droppedThemes =
      cleanList(originalThemes).map(normalize).toSet -- sanitizedThemes.map(normalize)
    Option
      .when(
        cleaned.nonEmpty &&
          !droppedThemes.exists(theme => theme.nonEmpty && normalize(cleaned).contains(theme))
      )(cleaned)
      .getOrElse("")

  private def planKey(plan: PlanHypothesis): String =
    planKey(plan.planId, plan.subplanId)

  private def planIdKey(plan: PlanHypothesis): String =
    planIdKey(plan.planId)

  private def planKey(planId: String, subplanId: Option[String]): String =
    s"${Option(planId).getOrElse("").trim.toLowerCase}|${subplanId.getOrElse("").trim.toLowerCase}"

  private def planIdKey(planId: String): String =
    Option(planId).getOrElse("").trim.toLowerCase

  private def planExperimentAllowed(
      planId: String,
      subplanId: Option[String],
      allowedPlanKeys: Set[String],
      allowedPlanIds: Set[String]
  ): Boolean =
    allowedPlanKeys.contains(planKey(planId, subplanId)) || allowedPlanIds.contains(planIdKey(planId))

  private def sanitizeRuntimeShell(shell: RuntimeShell): RuntimeShell =
    shell.copy(
      prompt = clean(shell.prompt),
      rootChoices = shell.rootChoices.map(sanitizeShellChoice),
      nodes = shell.nodes.map(sanitizePlayerNode),
      terminals = shell.terminals.map(sanitizeTerminalReveal)
    )

  private def sanitizeDominantFamily(family: DominantFamilySummary): DominantFamilySummary =
    family.copy(anchor = clean(family.anchor))

  private def sanitizeShellChoice(choice: ShellChoice): ShellChoice =
    choice.copy(
      label = cleanOpt(choice.label),
      feedback = clean(choice.feedback)
    )

  private def sanitizePlayerNode(node: PlayerNode): PlayerNode =
    node.copy(
      prompt = clean(node.prompt),
      badMoveFeedback = clean(node.badMoveFeedback),
      choices = node.choices.map(sanitizeShellChoice)
    )

  private def sanitizeTerminalReveal(reveal: TerminalReveal): TerminalReveal =
    reveal.copy(
      title = clean(reveal.title),
      summary = clean(reveal.summary),
      commentary = clean(reveal.commentary),
      anchor = cleanOpt(reveal.anchor),
      opening = cleanOpt(reveal.opening)
    )

  private def clean(value: String): String =
    UserFacingSignalSanitizer.sanitize(value)

  private def cleanOpt(value: Option[String]): Option[String] =
    value.map(clean).map(_.trim).filter(_.nonEmpty)

  private def cleanList(values: List[String]): List[String] =
    values.map(clean).map(_.trim).filter(_.nonEmpty)

  private def normalize(raw: String): String =
    Option(raw)
      .getOrElse("")
      .replace("**", "")
      .replaceAll("""[^\p{L}\p{N}\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .toLowerCase
