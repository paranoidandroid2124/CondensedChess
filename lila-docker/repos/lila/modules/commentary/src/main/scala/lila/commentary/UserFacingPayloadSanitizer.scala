package lila.commentary

import lila.commentary.analysis.PlanEvidenceEvaluator.{ EvaluatedPlan, UserFacingPlanEligibility }
import lila.commentary.analysis.UserFacingSignalSanitizer
import lila.commentary.model.StrategicPlanExperiment
import lila.commentary.model.authoring.PlanHypothesis
import lila.strategicPuzzle.StrategicPuzzle.*

object UserFacingPayloadSanitizer:

  def sanitize(response: CommentResponse): CommentResponse =
    sanitize(response, admittedPlans = Nil)

  def sanitize(
      response: CommentResponse,
      admittedPlans: List[EvaluatedPlan]
  ): CommentResponse =
    sanitize(
      response = response,
      admittedPlans = admittedPlans,
      previouslyAdmittedPlanKeys = Set.empty
    )

  def sanitizeCachedMoveReview(response: CommentResponse): CommentResponse =
    sanitize(
      response = response,
      admittedPlans = Nil,
      previouslyAdmittedPlanKeys = cachedMoveReviewPlanKeys(response)
    )

  private def sanitize(
      response: CommentResponse,
      admittedPlans: List[EvaluatedPlan],
      previouslyAdmittedPlanKeys: Set[String]
  ): CommentResponse =
    val admittedPlanKeys =
      previouslyAdmittedPlanKeys ++ admittedPlans
        .filter(isTypedProbeBackedPlan)
        .map(plan => planKey(plan.hypothesis))
        .toSet
    val sanitizedPlans =
      response.mainStrategicPlans
        .filter(plan => admittedPlanKeys.contains(planKey(plan)))
        .map(sanitizePlanHypothesis)
    val allowedPlanKeys = sanitizedPlans.map(planKey).toSet
    val allowedPlanIds = sanitizedPlans.filter(_.subplanId.isEmpty).map(planIdKey).toSet
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
      moveReviewLedger = response.moveReviewLedger.filter(_ => sanitizedPlans.nonEmpty).map(sanitizeMoveReviewLedger),
      moveReviewExplanation = response.moveReviewExplanation.map(sanitizeMoveReviewExplanation),
      moveReviewPlayerSurface = response.moveReviewPlayerSurface.map(sanitizeMoveReviewPlayerSurface)
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

  private def sanitizeMoveReviewLedger(ledger: MoveReviewStrategicLedger): MoveReviewStrategicLedger =
    ledger.copy(
      motifLabel = clean(ledger.motifLabel),
      stageLabel = clean(ledger.stageLabel),
      stageReason = cleanOpt(ledger.stageReason),
      prerequisites = cleanList(ledger.prerequisites),
      conversionTrigger = cleanOpt(ledger.conversionTrigger),
      primaryLine = ledger.primaryLine.map(sanitizeLedgerLine),
      resourceLine = ledger.resourceLine.map(sanitizeLedgerLine)
    )

  private def sanitizeLedgerLine(line: MoveReviewLedgerLine): MoveReviewLedgerLine =
    line.copy(
      title = clean(line.title),
      note = cleanOpt(line.note)
    )

  private def sanitizeMoveReviewExplanation(explanation: MoveReviewExplanation): MoveReviewExplanation =
    explanation.copy(
      title = clean(explanation.title),
      prose = clean(explanation.prose),
      qualityLabel = cleanOpt(explanation.qualityLabel),
      reasonTags = cleanList(explanation.reasonTags),
      shortLine = explanation.shortLine.map(sanitizeMoveReviewShortLine),
      pvInterpretation = explanation.pvInterpretation.map(sanitizeMoveReviewPvInterpretation),
      source = clean(explanation.source)
    )

  private def sanitizeMoveReviewShortLine(line: MoveReviewShortLine): MoveReviewShortLine =
    line.copy(
      san = cleanList(line.san),
      uci = cleanList(line.uci),
      lineId = cleanOpt(line.lineId),
      source = clean(line.source)
    )

  private def sanitizeMoveReviewPvInterpretation(interpretation: MoveReviewPvInterpretation): MoveReviewPvInterpretation =
    interpretation.copy(
      linePurpose = clean(interpretation.linePurpose),
      confirms = cleanList(interpretation.confirms),
      tension = clean(interpretation.tension),
      opponentReplyMeaning = cleanOpt(interpretation.opponentReplyMeaning),
      learningPoint = clean(interpretation.learningPoint),
      supportedByLineId = cleanOpt(interpretation.supportedByLineId),
      confidence = clean(interpretation.confidence)
    )

  private def sanitizeMoveReviewPlayerSurface(surface: MoveReviewPlayerSurface): MoveReviewPlayerSurface =
    surface.copy(
      schema = clean(surface.schema),
      title = cleanOpt(surface.title),
      summaryRows = surface.summaryRows.flatMap(sanitizeMoveReviewPlayerSurfaceRow),
      advancedRows = surface.advancedRows.flatMap(sanitizeMoveReviewPlayerSurfaceRow),
      decisionComparison = surface.decisionComparison.map(sanitizeMoveReviewPlayerDecisionComparison),
      probeRows = surface.probeRows.flatMap(sanitizeMoveReviewPlayerSurfaceRow),
      authorRows = surface.authorRows.flatMap(sanitizeMoveReviewPlayerAuthorRow)
    )

  private def sanitizeMoveReviewPlayerSurfaceRow(
      row: MoveReviewPlayerSurfaceRow
  ): Option[MoveReviewPlayerSurfaceRow] =
    for
      label <- cleanOpt(Some(row.label))
      text <- cleanOpt(Some(row.text))
    yield row.copy(
      label = label,
      text = text,
      tone = cleanOpt(row.tone),
      source = None,
      refSans = cleanList(row.refSans),
      authority = sanitizeMoveReviewSurfaceAuthority(row.authority)
    )

  private def sanitizeMoveReviewSurfaceAuthority(
      authority: Option[MoveReviewSurfaceAuthority]
  ): Option[MoveReviewSurfaceAuthority] =
    authority.flatMap { raw =>
      val kind = clean(raw.kind)
      val sanitized =
        MoveReviewSurfaceAuthority(
          kind = kind,
          token = cleanOpt(raw.token).filter(validSurfaceAuthorityToken),
          openingFamily = cleanOpt(raw.openingFamily).filter(validSurfaceAuthorityKey),
          target = cleanOpt(raw.target).filter(validSurfaceAuthorityTarget)
        )
      Option.when(validSurfaceAuthority(sanitized))(sanitized)
    }

  private def validSurfaceAuthority(authority: MoveReviewSurfaceAuthority): Boolean =
    authority.kind match
      case MoveReviewSurfaceAuthority.CounterplayBreak | MoveReviewSurfaceAuthority.CentralBreak =>
        authority.token.nonEmpty &&
          authority.openingFamily.isEmpty &&
          authority.target.isEmpty
      case MoveReviewSurfaceAuthority.OpeningFamily =>
        authority.openingFamily.nonEmpty &&
          authority.token.isEmpty
      case _ =>
        false

  private def validSurfaceAuthorityToken(token: String): Boolean =
    token.matches("""(?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?""")

  private def validSurfaceAuthorityKey(key: String): Boolean =
    key.matches("""[a-z][a-z0-9_]{1,40}""")

  private def validSurfaceAuthorityTarget(target: String): Boolean =
    target.matches("""[a-h][1-8]""")

  private def sanitizeMoveReviewPlayerDecisionComparison(
      comparison: MoveReviewPlayerDecisionComparison
  ): MoveReviewPlayerDecisionComparison =
    comparison.copy(
      kicker = clean(comparison.kicker),
      gapLabel = cleanOpt(comparison.gapLabel),
      chosenSan = cleanOpt(comparison.chosenSan),
      engineSan = cleanOpt(comparison.engineSan),
      comparedSan = cleanOpt(comparison.comparedSan),
      deferredSan = None,
      secondaryText = cleanOpt(comparison.secondaryText)
    )

  private def sanitizeMoveReviewPlayerAuthorRow(
      row: MoveReviewPlayerAuthorRow
  ): Option[MoveReviewPlayerAuthorRow] =
    for
      title <- cleanOpt(Some(row.title))
      status <- cleanOpt(Some(row.status))
      question <- cleanOpt(Some(row.question))
    yield row.copy(
      title = title,
      status = status,
      question = question,
      why = cleanOpt(row.why),
      meta = Nil,
      branches = row.branches.flatMap(sanitizeMoveReviewPlayerSurfaceRow)
    )

  private def isTypedProbeBackedPlan(plan: EvaluatedPlan): Boolean =
    plan.userFacingEligibility == UserFacingPlanEligibility.ProbeBacked &&
      plan.supportProbeIds.exists(_.trim.nonEmpty)

  private def cachedMoveReviewPlanKeys(response: CommentResponse): Set[String] =
    if response.moveReviewPlayerSurface.nonEmpty &&
      response.mainStrategicPlans.nonEmpty &&
      response.mainStrategicPlans.forall(_.evidenceSources.isEmpty)
    then response.mainStrategicPlans.map(planKey).toSet
    else Set.empty

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
    allowedPlanKeys.contains(planKey(planId, subplanId)) ||
      (subplanId.forall(_.trim.isEmpty) && allowedPlanIds.contains(planIdKey(planId)))

  private def sanitizeRuntimeShell(shell: RuntimeShell): RuntimeShell =
    shell.copy(
      prompt = clean(shell.prompt),
      plans = shell.plans.map(sanitizePuzzlePlan),
      proof = sanitizeRuntimeProofLayer(shell.proof),
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
    UserFacingSignalSanitizer.sanitize(value)

  private def cleanOpt(value: Option[String]): Option[String] =
    value.map(clean).map(_.trim).filter(_.nonEmpty)

  private def cleanList(values: List[String]): List[String] =
    values.map(clean).map(_.trim).filter(_.nonEmpty)
