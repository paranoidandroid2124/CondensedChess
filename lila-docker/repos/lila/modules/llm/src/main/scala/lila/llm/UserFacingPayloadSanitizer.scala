package lila.llm

import play.api.libs.json.*
import lila.strategicPuzzle.StrategicPuzzle.*

object UserFacingPayloadSanitizer:

  def sanitize(response: CommentResponse): CommentResponse =
    response.copy(
      commentary = clean(response.commentary),
      concepts = cleanList(response.concepts),
      probeRequests = Nil,
      authorQuestions = Nil,
      authorEvidence = Nil,
      mainStrategicPlans = Nil,
      strategicPlanExperiments = Nil,
      planStateToken = None,
      strategyPack = None,
      signalDigest = response.signalDigest.map(sanitizeSignalDigest),
      bookmakerLedger = None
    )

  def sanitize(response: GameChronicleResponse): GameChronicleResponse =
    response.copy(
      intro = clean(response.intro),
      moments = response.moments.map(sanitizeMoment),
      conclusion = clean(response.conclusion),
      themes = cleanList(response.themes),
      strategicThreads = Nil
    )

  def sanitize(payload: BootstrapPayload): BootstrapPayload =
    val shell = sanitizeRuntimeShell(payload.runtimeShell)
    payload.copy(
      puzzle =
        payload.puzzle.copy(
          dominantFamily = payload.puzzle.dominantFamily.map(sanitizeDominantFamily),
          runtimeShell = None
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
      strategyPack = None,
      signalDigest = moment.signalDigest.map(sanitizeSignalDigest),
      authorQuestions = Nil,
      authorEvidence = Nil,
      mainStrategicPlans = Nil,
      strategicPlanExperiments = Nil,
      activeStrategicNote = cleanOpt(moment.activeStrategicNote),
      activeStrategicIdeas = moment.activeStrategicIdeas.map(sanitizeActiveIdeaRef),
      activeStrategicRoutes = moment.activeStrategicRoutes.map(sanitizeActiveRouteRef),
      activeStrategicMoves = moment.activeStrategicMoves.map(sanitizeActiveMoveRef),
      activeDirectionalTargets = moment.activeDirectionalTargets.map(sanitizeDirectionalTarget),
      activeBranchDossier = moment.activeBranchDossier.map(sanitizeBranchDossier),
      strategicThread = moment.strategicThread.map(sanitizeThreadRef)
    )

  private def sanitizeSignalDigest(digest: NarrativeSignalDigest): NarrativeSignalDigest =
    digest.copy(
      opening = cleanOpt(digest.opening),
      strategicStack = Nil,
      latentPlan = None,
      latentReason = None,
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

  private def sanitizeActiveIdeaRef(ref: ActiveStrategicIdeaRef): ActiveStrategicIdeaRef =
    ref.copy(focusSummary = clean(ref.focusSummary))

  private def sanitizeActiveRouteRef(ref: ActiveStrategicRouteRef): ActiveStrategicRouteRef =
    ref.copy(purpose = clean(ref.purpose))

  private def sanitizeActiveMoveRef(ref: ActiveStrategicMoveRef): ActiveStrategicMoveRef =
    ref.copy(
      label = clean(ref.label),
      san = cleanOpt(ref.san)
    )

  private def sanitizeDirectionalTarget(target: StrategyDirectionalTarget): StrategyDirectionalTarget =
    target.copy(
      strategicReasons = cleanList(target.strategicReasons),
      prerequisites = cleanList(target.prerequisites),
      evidence = cleanList(target.evidence)
    )

  private def sanitizeBranchDossier(dossier: ActiveBranchDossier): ActiveBranchDossier =
    dossier.copy(
      chosenBranchLabel = clean(dossier.chosenBranchLabel),
      engineBranchLabel = cleanOpt(dossier.engineBranchLabel),
      deferredBranchLabel = cleanOpt(dossier.deferredBranchLabel),
      whyChosen = cleanOpt(dossier.whyChosen),
      whyDeferred = cleanOpt(dossier.whyDeferred),
      opponentResource = cleanOpt(dossier.opponentResource),
      routeCue = dossier.routeCue.map(cue => cue.copy(purpose = clean(cue.purpose))),
      moveCue =
        dossier.moveCue.map(cue =>
          cue.copy(
            label = clean(cue.label),
            san = cleanOpt(cue.san),
            source = clean(cue.source)
          )
        ),
      evidenceCue = cleanOpt(dossier.evidenceCue),
      continuationFocus = cleanOpt(dossier.continuationFocus),
      practicalRisk = cleanOpt(dossier.practicalRisk),
      threadLabel = cleanOpt(dossier.threadLabel),
      threadStage = cleanOpt(dossier.threadStage),
      threadSummary = cleanOpt(dossier.threadSummary),
      threadOpponentCounterplan = cleanOpt(dossier.threadOpponentCounterplan)
    )

  private def sanitizeThreadRef(ref: ActiveStrategicThreadRef): ActiveStrategicThreadRef =
    ref.copy(
      themeLabel = clean(ref.themeLabel),
      stageLabel = clean(ref.stageLabel)
    )

  private def sanitizeRuntimeShell(shell: RuntimeShell): RuntimeShell =
    shell.copy(
      prompt = clean(shell.prompt),
      proof = sanitizeRuntimeProofLayer(shell.proof),
      plans = shell.plans.map(sanitizePuzzlePlan),
      terminals = shell.terminals.map(sanitizeTerminalReveal)
    )

  private def sanitizeRuntimeProofLayer(proof: RuntimeProofLayer): RuntimeProofLayer =
    proof.copy(
      rootChoices = proof.rootChoices.map(sanitizeShellChoice),
      nodes = proof.nodes.map(sanitizePlayerNode),
      forcedReplies = proof.forcedReplies
    )

  private def sanitizeDominantFamily(family: DominantFamilySummary): DominantFamilySummary =
    family.copy(anchor = clean(family.anchor))

  private def sanitizeShellChoice(choice: ShellChoice): ShellChoice =
    choice.copy(
      label = cleanOpt(choice.label),
      feedback = clean(choice.feedback)
    )

  private def sanitizePlanStart(start: PlanStart): PlanStart =
    start.copy(
      label = cleanOpt(start.label),
      feedback = clean(start.feedback)
    )

  private def sanitizePuzzlePlan(plan: PuzzlePlan): PuzzlePlan =
    plan.copy(
      anchor = cleanOpt(plan.anchor),
      task = clean(plan.task),
      feedback = clean(plan.feedback),
      allowedStarts = plan.allowedStarts.map(sanitizePlanStart)
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
      opening = cleanOpt(reveal.opening),
      planTask = cleanOpt(reveal.planTask),
      whyPlan = cleanOpt(reveal.whyPlan),
      whyMove = cleanOpt(reveal.whyMove),
      acceptedStarts = cleanList(reveal.acceptedStarts),
      featuredStart = cleanOpt(reveal.featuredStart)
    )

  private def clean(value: String): String =
    Option(value).getOrElse("").replace("**", "").trim

  private def cleanOpt(value: Option[String]): Option[String] =
    value.map(clean).filter(_.nonEmpty)

  private def cleanList(values: List[String]): List[String] =
    values.map(clean).filter(_.nonEmpty)
