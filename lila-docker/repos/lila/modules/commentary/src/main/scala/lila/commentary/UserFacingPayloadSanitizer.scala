package lila.commentary

import lila.commentary.analysis.PlanEvidenceEvaluator
import lila.commentary.analysis.PlanEvidenceEvaluator.EvaluatedPlan
import lila.commentary.analysis.MoveReviewPlayerPayloadBuilder
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

  private val SurfaceAuthorityTokenPattern = """(?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?""".r
  private val SurfaceAuthorityRouteTokenPattern = """(?:\.\.\.)?[a-h][1-8]-[a-h][1-8]""".r
  private val SurfaceAuthorityKeyPattern = """[a-z][a-z0-9_]{1,40}""".r
  private val ChessSquarePattern = """[a-h][1-8]""".r

  private val CoordinationTextPattern =
    """The checked line coordinates pressure on ([a-h][1-8]) from ([a-h][1-8]) and ([a-h][1-8])\.""".r

  private val ColorComplexTextPattern =
    """The checked line keeps the (bishop|knight) on ([a-h][1-8]) attacking ([a-h][1-8]) in the (dark|light)-square complex\.""".r

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
    if sanitizedPlans.nonEmpty then
      Some(
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
      )
    else None

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
    values.flatMap { value =>
      if relationSupportLeak(value) then None
      else
        val cleaned = clean(value).trim
        if cleaned.nonEmpty && !relationSupportLeak(cleaned) then Some(cleaned) else None
    }

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
    if ledger.schema != MoveReviewLedgerSchema || !validSurfaceAuthorityKey(motifKey) || !validSurfaceAuthorityKey(stageKey)
    then None
    else
      for
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
    if !MoveReviewPlayerPayloadBuilder.MoveReviewLedgerLineSources.contains(source) || sanMoves.isEmpty then None
    else
      for title <- cleanOpt(Some(line.title))
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
        sanitizeMoveReviewPlayerSurfaceRow(
          row,
          allowStrategicRelation = summaryRowAllowsStrategicRelation(row),
          allowAuthority = allowAuthority
        )
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

  private def summaryRowAllowsStrategicRelation(row: MoveReviewPlayerSurfaceRow): Boolean =
    row.authority.exists { authority =>
      val label = cleanOpt(Some(row.label))
      val token = cleanOpt(authority.token)
      val target = cleanOpt(authority.target).filter(validSurfaceAuthorityTarget)
      clean(authority.kind) == MoveReviewSurfaceAuthority.StrategicRelation &&
      target.nonEmpty &&
      (
        label.contains("Defender trade") && token.contains("defender_trade") ||
          label.contains("Bad piece trade") && token.contains("bad_piece_liquidation")
      )
    }

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
        if allowAuthority then sanitizeMoveReviewSurfaceAuthority(row.authority, allowStrategicRelation, Some(label), Some(text))
        else None
    )

  private def sanitizeMoveReviewSurfaceAuthority(
      authority: Option[MoveReviewSurfaceAuthority],
      allowStrategicRelation: Boolean,
      rowLabel: Option[String],
      rowText: Option[String]
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
            else if kind == MoveReviewSurfaceAuthority.PracticalPlan then
              target.filter(square => practicalPlanTargetAllowed(rowLabel, rowText, square))
            else target,
          openingBook =
            if kind == MoveReviewSurfaceAuthority.OpeningFamily then
              raw.openingBook.flatMap(MoveReviewOpeningBookMetadata.sanitize)
            else None
        )
      val unsupportedPracticalTarget =
        kind == MoveReviewSurfaceAuthority.PracticalPlan &&
          target.nonEmpty &&
          !rowLabel.exists(exactPracticalTargetLabel)
      if !unsupportedPracticalTarget && validSurfaceAuthority(sanitized, allowStrategicRelation, rowLabel, rowText) then
        Some(sanitized)
      else None
    }

  private def validSurfaceAuthority(
      authority: MoveReviewSurfaceAuthority,
      allowStrategicRelation: Boolean,
      rowLabel: Option[String],
      rowText: Option[String]
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
          (authority.target.isEmpty ||
            authority.target.exists(target => practicalPlanTargetAllowed(rowLabel, rowText, target))) &&
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
    SurfaceAuthorityTokenPattern.matches(token)

  private def validSurfaceAuthorityRouteToken(token: String): Boolean =
    SurfaceAuthorityRouteTokenPattern.matches(token)

  private def validSurfaceAuthorityKey(key: String): Boolean =
    SurfaceAuthorityKeyPattern.matches(key)

  private def validStrategicRelationToken(token: String): Boolean =
    validSurfaceAuthorityKey(token) && RelationObservationCatalog.isImplementedKind(token)

  private def validSurfaceAuthorityTarget(target: String): Boolean =
    ChessSquarePattern.matches(target)

  private def practicalPlanTargetAllowed(
      rowLabel: Option[String],
      rowText: Option[String],
      target: String
  ): Boolean =
    val square = target.trim.toLowerCase
    rowLabel.zip(rowText).exists { case (label, text) =>
      label match
        case "Fixed target" =>
          text == s"The checked line keeps $square fixed as the target."
        case "Minority attack" =>
          text == s"The checked line keeps $square as the minority-attack fixed target."
        case "IQP target" =>
          text == s"The checked line leaves $square as an isolated pawn target."
        case "Simplification" =>
          text == s"The checked line keeps the same local edge after the exchange on $square."
        case "Knight outpost" =>
          text == s"The checked line puts the knight on the $square outpost."
        case "File entry" =>
          text == s"The checked line keeps pressure on $square through the ${square.take(1)}-file."
        case "Target coordination" =>
          text match
            case CoordinationTextPattern(targetSq, left, right) =>
              targetSq == square && left != right
            case _ => false
        case "Color complex" =>
          text match
            case ColorComplexTextPattern(role, from, targetSq, complex) =>
              targetSq == square && squareColorOf(square).contains(complex) && roleCanAttackSquare(role, from, square)
            case _ => false
        case _ =>
          false
    }

  private def squareCoords(raw: String): Option[(Int, Int)] =
    val square = raw.trim.toLowerCase
    Option.when(ChessSquarePattern.matches(square))((square.charAt(0) - 'a' + 1, square.charAt(1).asDigit))

  private def squareColorOf(raw: String): Option[String] =
    squareCoords(raw).map { case (file, rank) =>
      if (file + rank) % 2 == 0 then "dark" else "light"
    }

  private def roleCanAttackSquare(role: String, from: String, target: String): Boolean =
    (squareCoords(from), squareCoords(target)) match
      case (Some((fromFile, fromRank)), Some((targetFile, targetRank))) =>
        val fileDelta = math.abs(fromFile - targetFile)
        val rankDelta = math.abs(fromRank - targetRank)
        role.trim.toLowerCase match
          case "bishop" => fileDelta == rankDelta && fileDelta > 0
          case "knight" => (fileDelta == 1 && rankDelta == 2) || (fileDelta == 2 && rankDelta == 1)
          case _        => false
      case _ => false

  private def exactPracticalTargetLabel(label: String): Boolean =
    label == "Fixed target" || label == "Minority attack" || label == "IQP target" ||
      label == "Simplification" || label == "Knight outpost" || label == "File entry" ||
      label == "Target coordination" || label == "Color complex"

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
      targetComparison = sanitizeMoveReviewDecisionTargetComparison(comparison.targetComparison),
      refSans = cleanList(comparison.refSans)
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
      if validSurfaceAuthorityTarget(sanitized.chosenTarget) &&
          validSurfaceAuthorityTarget(sanitized.bestTarget) &&
          validSurfaceAuthorityKey(sanitized.chosenTargetKind) &&
          validSurfaceAuthorityKey(sanitized.bestTargetKind)
      then Some(sanitized)
      else None
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
