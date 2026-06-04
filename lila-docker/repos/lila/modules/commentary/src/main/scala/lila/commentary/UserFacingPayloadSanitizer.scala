package lila.commentary

import lila.commentary.analysis.PlanEvidenceEvaluator
import lila.commentary.analysis.PlanEvidenceEvaluator.EvaluatedPlan
import lila.commentary.analysis.MoveReviewSupportedLocalSurfaceRows
import lila.commentary.analysis.OpeningFamilyCatalog
import lila.commentary.analysis.UserFacingSignalSanitizer
import lila.commentary.analysis.semantic.RelationObservationCatalog
import lila.commentary.model.StrategicPlanExperiment
import lila.commentary.model.authoring.PlanHypothesis
import lila.strategicPuzzle.StrategicPuzzle.*

object UserFacingPayloadSanitizer:

  private val MoveReviewLedgerSchema = "chesstory.move_review.ledger.v1"
  private val CurrentMoveReviewPlayerSurfaceSchema = "chesstory.move_review.player_surface.v2"
  private val MoveReviewPlayerSurfaceSchemas =
    Set("chesstory.move_review.player_surface.v1", CurrentMoveReviewPlayerSurfaceSchema)
  private val MoveReviewLedgerLineSources = Set("probe", "decision_compare", "variation", "authoring")
  private val StrategicRelationTokens =
    RelationObservationCatalog.ImplementedKinds

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
        .filter(PlanEvidenceEvaluator.isMainAdmittedPlan)
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
      moveReviewLedger = response.moveReviewLedger.filter(_ => sanitizedPlans.nonEmpty).flatMap(sanitizeMoveReviewLedger),
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
        strategicIdeas = Nil,
        longTermFocus = cleanPublicSupportList(pack.longTermFocus),
        evidence = cleanPublicSupportList(pack.evidence),
        signalDigest = pack.signalDigest.map(sanitizeSignalDigest)
      )
    }

  private def sanitizeSidePlan(plan: StrategySidePlan): StrategySidePlan =
    plan.copy(
      planName = clean(plan.planName),
      priorities = cleanPublicSupportList(plan.priorities),
      riskTriggers = cleanPublicSupportList(plan.riskTriggers)
    )

  private def sanitizePieceRoute(route: StrategyPieceRoute): StrategyPieceRoute =
    route.copy(
      purpose = clean(route.purpose),
      evidence = cleanPublicSupportList(route.evidence)
    )

  private def sanitizePieceMoveRef(moveRef: StrategyPieceMoveRef): StrategyPieceMoveRef =
    moveRef.copy(
      idea = clean(moveRef.idea),
      tacticalTheme = cleanOpt(moveRef.tacticalTheme),
      evidence = cleanPublicSupportList(moveRef.evidence)
    )

  private def sanitizeDirectionalTarget(target: StrategyDirectionalTarget): StrategyDirectionalTarget =
    target.copy(
      strategicReasons = cleanPublicSupportList(target.strategicReasons),
      prerequisites = cleanPublicSupportList(target.prerequisites),
      evidence = cleanPublicSupportList(target.evidence)
    )

  private def cleanPublicSupportList(values: List[String]): List[String] =
    cleanList(values.filterNot(relationSupportLeak)).filterNot(relationSupportLeak)

  private def relationSupportLeak(value: String): Boolean =
    value.trim.toLowerCase.startsWith("deferred_") ||
      RelationObservationCatalog.deferredFallbackForMotifTag(value).nonEmpty ||
      RelationObservationCatalog.relationWitnessOnlyMotifTag(value) ||
      RelationObservationCatalog.pvDrawResourceOnlyMotifTag(value)

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

  private def sanitizeMoveReviewLedger(ledger: MoveReviewStrategicLedger): Option[MoveReviewStrategicLedger] =
    val motifKey = clean(ledger.motifKey)
    val stageKey = clean(ledger.stageKey)
    for
      _ <- Option.when(ledger.schema == MoveReviewLedgerSchema)(())
      _ <- Option.when(validSurfaceAuthorityKey(motifKey) && validSurfaceAuthorityKey(stageKey))(())
      motifLabel <- cleanOpt(Some(ledger.motifLabel))
      stageLabel <- cleanOpt(Some(ledger.stageLabel))
    yield
      ledger.copy(
        schema = MoveReviewLedgerSchema,
        motifKey = motifKey,
        motifLabel = motifLabel,
        stageKey = stageKey,
        stageLabel = stageLabel,
        stageReason = cleanOpt(ledger.stageReason),
        prerequisites = cleanPublicSupportList(ledger.prerequisites),
        conversionTrigger = cleanOpt(ledger.conversionTrigger),
        primaryLine = ledger.primaryLine.flatMap(sanitizeLedgerLine),
        resourceLine = ledger.resourceLine.flatMap(sanitizeLedgerLine)
      )

  private def sanitizeLedgerLine(line: MoveReviewLedgerLine): Option[MoveReviewLedgerLine] =
    val source = clean(line.source)
    val sanMoves = cleanList(line.sanMoves)
    for
      title <- cleanOpt(Some(line.title))
      _ <- Option.when(MoveReviewLedgerLineSources.contains(source) && sanMoves.nonEmpty)(())
    yield
      line.copy(
        title = title,
        sanMoves = sanMoves,
        note = cleanOpt(line.note),
        source = source
      )

  private def sanitizeMoveReviewExplanation(explanation: MoveReviewExplanation): MoveReviewExplanation =
    explanation.copy(
      title = clean(explanation.title),
      prose = clean(explanation.prose),
      qualityLabel = cleanOpt(explanation.qualityLabel),
      reasonTags = cleanList(explanation.reasonTags),
      shortLine = explanation.shortLine.map(sanitizeMoveReviewShortLine),
      pvInterpretation = explanation.pvInterpretation.map(sanitizeMoveReviewPvInterpretation),
      source = clean(explanation.source),
      factFragments = None
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
    val schema = sanitizeMoveReviewPlayerSurfaceSchema(surface.schema)
    val allowAuthority =
      schema == CurrentMoveReviewPlayerSurfaceSchema
    val summaryRows =
      surface.summaryRows.flatMap(row =>
        sanitizeMoveReviewPlayerSurfaceRow(row, allowStrategicRelation = false, allowAuthority = allowAuthority)
      )
    val suppressedRelationKinds =
      MoveReviewSupportedLocalSurfaceRows.relationKindsForRows(summaryRows)
    val advancedRows =
      surface.advancedRows
        .flatMap(row =>
          sanitizeMoveReviewPlayerSurfaceRow(row, allowStrategicRelation = true, allowAuthority = allowAuthority)
        )
        .filterNot(row => strategicRelationSuppressedBySummary(row, suppressedRelationKinds))
    surface.copy(
      schema = schema,
      title = cleanOpt(surface.title),
      summaryRows = summaryRows,
      advancedRows = advancedRows,
      decisionComparison = surface.decisionComparison.map(sanitizeMoveReviewPlayerDecisionComparison),
      probeRows = surface.probeRows.flatMap(row =>
        sanitizeMoveReviewPlayerSurfaceRow(row, allowStrategicRelation = false, allowAuthority = allowAuthority)
      ),
      authorRows = surface.authorRows.flatMap(row => sanitizeMoveReviewPlayerAuthorRow(row, allowAuthority))
    )

  private def strategicRelationSuppressedBySummary(
      row: MoveReviewPlayerSurfaceRow,
      suppressedRelationKinds: Set[String]
  ): Boolean =
    row.authority.exists(authority =>
      authority.kind == MoveReviewSurfaceAuthority.StrategicRelation &&
        authority.token.exists(suppressedRelationKinds.contains)
    )

  private def sanitizeMoveReviewPlayerSurfaceSchema(schema: String): String =
    val normalized =
      Option(schema).getOrElse("").trim.replaceAll("""\s*\.\s*""", ".")
    if MoveReviewPlayerSurfaceSchemas.contains(normalized) then normalized
    else CurrentMoveReviewPlayerSurfaceSchema

  private def sanitizeMoveReviewPlayerSurfaceRow(
      row: MoveReviewPlayerSurfaceRow,
      allowStrategicRelation: Boolean,
      allowAuthority: Boolean
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
      authority =
        if allowAuthority then sanitizeMoveReviewSurfaceAuthority(row.authority, allowStrategicRelation)
        else None
    )

  private def sanitizeMoveReviewSurfaceAuthority(
      authority: Option[MoveReviewSurfaceAuthority],
      allowStrategicRelation: Boolean
  ): Option[MoveReviewSurfaceAuthority] =
    authority.flatMap { raw =>
      val kind = clean(raw.kind)
      val openingFamily = cleanOpt(raw.openingFamily).filter(validSurfaceAuthorityKey)
      val target = cleanOpt(raw.target).filter(validSurfaceAuthorityTarget)
      val sanitized =
        MoveReviewSurfaceAuthority(
          kind = kind,
          token = cleanSurfaceAuthorityToken(kind, raw.token),
          openingFamily = openingFamily,
          target =
            if kind == MoveReviewSurfaceAuthority.OpeningFamily then
              target.filter(square => openingFamily.exists(family => openingTargetAllowed(family, square)))
            else target,
          openingBook =
            if kind == MoveReviewSurfaceAuthority.OpeningFamily then
              raw.openingBook.flatMap(MoveReviewOpeningBookMetadata.sanitize)
            else None
        )
      Option.when(validSurfaceAuthority(sanitized, allowStrategicRelation))(sanitized)
    }

  private def validSurfaceAuthority(
      authority: MoveReviewSurfaceAuthority,
      allowStrategicRelation: Boolean
  ): Boolean =
    authority.kind match
      case MoveReviewSurfaceAuthority.CounterplayBreak =>
        authority.token.nonEmpty &&
          authority.openingFamily.isEmpty &&
          authority.target.isEmpty &&
          authority.openingBook.isEmpty
      case MoveReviewSurfaceAuthority.CentralBreak |
          MoveReviewSurfaceAuthority.CentralLiquidation |
          MoveReviewSurfaceAuthority.CentralChallenge =>
        authority.token.exists(validSurfaceAuthorityRouteToken) &&
          authority.openingFamily.isEmpty &&
          authority.target.isEmpty &&
          authority.openingBook.isEmpty
      case MoveReviewSurfaceAuthority.PracticalPlan =>
        authority.token.isEmpty &&
          authority.openingFamily.isEmpty &&
          authority.target.isEmpty &&
          authority.openingBook.isEmpty
      case MoveReviewSurfaceAuthority.OpeningFamily =>
        authority.openingFamily.nonEmpty &&
          authority.token.isEmpty
      case MoveReviewSurfaceAuthority.StrategicRelation =>
        allowStrategicRelation &&
          authority.token.exists(validStrategicRelationToken) &&
          authority.target.nonEmpty &&
          authority.openingFamily.isEmpty &&
          authority.openingBook.isEmpty
      case _ =>
        false

  private def cleanSurfaceAuthorityToken(kind: String, token: Option[String]): Option[String] =
    cleanOpt(token).filter { value =>
      if kind == MoveReviewSurfaceAuthority.StrategicRelation then validStrategicRelationToken(value)
      else validSurfaceAuthorityToken(value)
    }

  private def validSurfaceAuthorityToken(token: String): Boolean =
    token.matches("""(?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?""")

  private def validSurfaceAuthorityRouteToken(token: String): Boolean =
    token.matches("""(?:\.\.\.)?[a-h][1-8]-[a-h][1-8]""")

  private def validSurfaceAuthorityKey(key: String): Boolean =
    key.matches("""[a-z][a-z0-9_]{1,40}""")

  private def validStrategicRelationToken(token: String): Boolean =
    validSurfaceAuthorityKey(token) && StrategicRelationTokens.contains(token)

  private def validSurfaceAuthorityTarget(target: String): Boolean =
    target.matches("""[a-h][1-8]""")

  private def openingTargetAllowed(openingFamily: String, target: String): Boolean =
    OpeningFamilyCatalog.default.targetAllowed(openingFamily, target)

  private def sanitizeMoveReviewPlayerDecisionComparison(
      comparison: MoveReviewPlayerDecisionComparison
  ): MoveReviewPlayerDecisionComparison =
    comparison.copy(
      kicker = clean(comparison.kicker),
      gapLabel = cleanOpt(comparison.gapLabel),
      chosenSan = cleanOpt(comparison.chosenSan),
      engineSan = cleanOpt(comparison.engineSan),
      comparedSan = cleanOpt(comparison.comparedSan),
      secondaryText = cleanOpt(comparison.secondaryText),
      targetComparison = sanitizeMoveReviewDecisionTargetComparison(comparison.targetComparison)
    )

  private def sanitizeMoveReviewDecisionTargetComparison(
      comparison: Option[MoveReviewDecisionTargetComparison]
  ): Option[MoveReviewDecisionTargetComparison] =
    comparison.flatMap { raw =>
      val sanitized =
        MoveReviewDecisionTargetComparison(
          chosenTarget = clean(raw.chosenTarget),
          chosenTargetKind = clean(raw.chosenTargetKind),
          bestTarget = clean(raw.bestTarget),
          bestTargetKind = clean(raw.bestTargetKind)
        )
      Option.when(
        validSurfaceAuthorityTarget(sanitized.chosenTarget) &&
          validSurfaceAuthorityTarget(sanitized.bestTarget) &&
          validSurfaceAuthorityKey(sanitized.chosenTargetKind) &&
          validSurfaceAuthorityKey(sanitized.bestTargetKind)
      )(sanitized)
    }

  private def sanitizeMoveReviewPlayerAuthorRow(
      row: MoveReviewPlayerAuthorRow,
      allowAuthority: Boolean
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
      branches = row.branches.flatMap(branch =>
        sanitizeMoveReviewPlayerSurfaceRow(branch, allowStrategicRelation = false, allowAuthority = allowAuthority)
      )
    )

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
