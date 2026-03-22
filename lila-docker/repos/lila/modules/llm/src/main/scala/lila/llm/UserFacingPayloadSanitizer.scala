package lila.llm

import lila.llm.analysis.UserFacingSignalSanitizer
import lila.llm.model.StrategicPlanExperiment
import lila.llm.model.authoring.{ LatentPlanNarrative, PlanHypothesis }
import lila.strategicPuzzle.StrategicPuzzle.*

object UserFacingPayloadSanitizer:

  def sanitize(response: CommentResponse): CommentResponse =
    response.copy(
      commentary = clean(response.commentary),
      concepts = cleanList(response.concepts),
      authorQuestions = response.authorQuestions.map(sanitizeAuthorQuestion),
      authorEvidence = response.authorEvidence.map(sanitizeAuthorEvidence),
      mainStrategicPlans = response.mainStrategicPlans.map(sanitizePlanHypothesis),
      strategicPlanExperiments = response.strategicPlanExperiments.map(sanitizeStrategicPlanExperiment),
      latentPlans = response.latentPlans.map(sanitizeLatentPlan),
      whyAbsentFromTopMultiPV = cleanList(response.whyAbsentFromTopMultiPV),
      strategyPack = response.strategyPack.map(sanitizeStrategyPack),
      signalDigest = response.signalDigest.map(sanitizeSignalDigest),
      bookmakerLedger = response.bookmakerLedger.map(sanitizeBookmakerLedger)
    )

  def sanitize(response: GameChronicleResponse): GameChronicleResponse =
    response.copy(
      intro = clean(response.intro),
      moments = response.moments.map(sanitizeMoment),
      conclusion = clean(response.conclusion),
      themes = cleanList(response.themes),
      strategicThreads = response.strategicThreads.map(sanitizeStrategicThread)
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
    moment.copy(
      moveClassification = cleanOpt(moment.moveClassification),
      narrative = clean(moment.narrative),
      selectionLabel = cleanOpt(moment.selectionLabel),
      selectionReason = cleanOpt(moment.selectionReason),
      concepts = cleanList(moment.concepts),
      strategyPack = moment.strategyPack.map(sanitizeStrategyPack),
      signalDigest = moment.signalDigest.map(sanitizeSignalDigest),
      authorQuestions = moment.authorQuestions.map(sanitizeAuthorQuestion),
      authorEvidence = moment.authorEvidence.map(sanitizeAuthorEvidence),
      mainStrategicPlans = moment.mainStrategicPlans.map(sanitizePlanHypothesis),
      strategicPlanExperiments = moment.strategicPlanExperiments.map(sanitizeStrategicPlanExperiment),
      latentPlans = moment.latentPlans.map(sanitizeLatentPlan),
      whyAbsentFromTopMultiPV = cleanList(moment.whyAbsentFromTopMultiPV),
      activeStrategicNote = cleanOpt(moment.activeStrategicNote),
      activeStrategicIdeas = moment.activeStrategicIdeas.map(sanitizeActiveIdeaRef),
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
      refutation = cleanOpt(plan.refutation)
    )

  private def sanitizeLatentPlan(plan: LatentPlanNarrative): LatentPlanNarrative =
    plan.copy(
      planName = clean(plan.planName),
      whyAbsentFromTopMultiPv = clean(plan.whyAbsentFromTopMultiPv)
    )

  private def sanitizeStrategicPlanExperiment(experiment: StrategicPlanExperiment): StrategicPlanExperiment =
    experiment.copy(themeL1 = clean(experiment.themeL1))

  private def sanitizeStrategyPack(pack: StrategyPack): StrategyPack =
    pack.copy(
      plans = pack.plans.map(sanitizeSidePlan),
      pieceRoutes = pack.pieceRoutes.map(sanitizePieceRoute),
      pieceMoveRefs = pack.pieceMoveRefs.map(sanitizePieceMoveRef),
      directionalTargets = pack.directionalTargets.map(sanitizeDirectionalTarget),
      longTermFocus = cleanList(pack.longTermFocus),
      evidence = cleanList(pack.evidence),
      signalDigest = pack.signalDigest.map(sanitizeSignalDigest)
    )

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
      strategicStack = cleanList(digest.strategicStack),
      latentPlan = cleanOpt(digest.latentPlan),
      latentReason = cleanOpt(digest.latentReason),
      decisionComparison = digest.decisionComparison.map(sanitizeDecisionComparison),
      authoringEvidence = cleanOpt(digest.authoringEvidence),
      practicalVerdict = cleanOpt(digest.practicalVerdict),
      practicalFactors = cleanList(digest.practicalFactors),
      compensation = cleanOpt(digest.compensation),
      compensationVectors = cleanList(digest.compensationVectors),
      structuralCue = cleanOpt(digest.structuralCue),
      structureProfile = cleanOpt(digest.structureProfile),
      alignmentReasons = cleanList(digest.alignmentReasons),
      deploymentPurpose = cleanOpt(digest.deploymentPurpose),
      deploymentContribution = cleanOpt(digest.deploymentContribution),
      prophylaxisPlan = cleanOpt(digest.prophylaxisPlan),
      prophylaxisThreat = cleanOpt(digest.prophylaxisThreat),
      dominantIdeaFocus = cleanOpt(digest.dominantIdeaFocus),
      secondaryIdeaFocus = cleanOpt(digest.secondaryIdeaFocus),
      decision = cleanOpt(digest.decision),
      strategicFlow = cleanOpt(digest.strategicFlow),
      opponentPlan = cleanOpt(digest.opponentPlan),
      preservedSignals = cleanList(digest.preservedSignals)
    )

  private def sanitizeDecisionComparison(digest: DecisionComparisonDigest): DecisionComparisonDigest =
    digest.copy(
      chosenMove = cleanOpt(digest.chosenMove),
      engineBestMove = cleanOpt(digest.engineBestMove),
      deferredMove = cleanOpt(digest.deferredMove),
      deferredReason = cleanOpt(digest.deferredReason),
      deferredSource = cleanOpt(digest.deferredSource),
      evidence = cleanOpt(digest.evidence)
    )

  private def sanitizeAuthorQuestion(question: AuthorQuestionSummary): AuthorQuestionSummary =
    question.copy(
      question = clean(question.question),
      why = cleanOpt(question.why),
      anchors = cleanList(question.anchors),
      latentPlanName = cleanOpt(question.latentPlanName)
    )

  private def sanitizeAuthorEvidence(evidence: AuthorEvidenceSummary): AuthorEvidenceSummary =
    evidence.copy(
      question = clean(evidence.question),
      why = cleanOpt(evidence.why),
      purposes = cleanList(evidence.purposes),
      branches = evidence.branches.map(sanitizeEvidenceBranch),
      probeObjectives = cleanList(evidence.probeObjectives),
      linkedPlans = cleanList(evidence.linkedPlans)
    )

  private def sanitizeEvidenceBranch(branch: EvidenceBranchSummary): EvidenceBranchSummary =
    branch.copy(
      keyMove = clean(branch.keyMove),
      line = clean(branch.line)
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

  private def sanitizeStrategicThread(thread: ActiveStrategicThread): ActiveStrategicThread =
    thread.copy(
      themeLabel = clean(thread.themeLabel),
      summary = clean(thread.summary),
      opponentCounterplan = cleanOpt(thread.opponentCounterplan)
    )

  private def sanitizeThreadRef(ref: ActiveStrategicThreadRef): ActiveStrategicThreadRef =
    ref.copy(
      themeLabel = clean(ref.themeLabel),
      stageLabel = clean(ref.stageLabel)
    )

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
