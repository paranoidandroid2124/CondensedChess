package lila.llm

import play.api.libs.json.*

object PlanTier:
  val Basic = "basic"
  val Pro = "pro"

  def normalize(raw: String): String =
    Option(raw).map(_.trim.toLowerCase) match
      case Some(Pro) => Pro
      case _         => Basic

object LlmLevel:
  val Polish = "polish"
  val Active = "active"

  def normalize(raw: String): String =
    Option(raw).map(_.trim.toLowerCase) match
      case Some(Active) => Active
      case _            => Polish

case class EvalData(cp: Int, mate: Option[Int], pv: Option[List[String]])
object EvalData:
  given Reads[EvalData] = Json.reads[EvalData]
  given Writes[EvalData] = Json.writes[EvalData]

case class PositionContext(opening: Option[String], phase: String, ply: Int, variant: Option[String] = None)
object PositionContext:
  given Reads[PositionContext] = Json.reads[PositionContext]
  given Writes[PositionContext] = Json.writes[PositionContext]

case class CommentRequest(
    fen: String,
    lastMove: Option[String],
    eval: Option[EvalData],
    context: PositionContext,
    // Bookmaker Stage 2: optional MultiPV payload (sent by client-side Stockfish)
    variations: Option[List[lila.llm.model.strategic.VariationLine]] = None,
    // Bookmaker Stage 2 (optional): probe evidence from client to enable a1/a2 sub-branches
    probeResults: Option[List[lila.llm.model.ProbeResult]] = None,
    // Bookmaker Stage 2 (optional): explorer data from client to bypass server-side I/O
    openingData: Option[lila.llm.model.OpeningReference] = None,
    // Bookmaker delta: optional post-move position to compute before/after differences.
    afterFen: Option[String] = None,
    afterEval: Option[EvalData] = None,
    afterVariations: Option[List[lila.llm.model.strategic.VariationLine]] = None,
    // State Passing: persist plan state between individual move queries
    planStateToken: Option[lila.llm.analysis.PlanStateTracker] = None,
    endgameStateToken: Option[lila.llm.model.strategic.EndgamePatternState] = None
)
object CommentRequest:
  val MinBookmakerPly = 5
  private def moveNumberFromPly(ply: Int): Int = math.max(1, (ply + 1) / 2)
  given Reads[CommentRequest] = Json.reads[CommentRequest]

  def validateBookmaker(request: CommentRequest): Either[GameAnalysisValidationError, CommentRequest] =
    if request.context.ply < MinBookmakerPly then
      Left(
        GameAnalysisValidationError(
          "bookmaker_too_early",
          s"Bookmaker opens from move ${moveNumberFromPly(MinBookmakerPly)}. Continue a few moves first."
        )
      )
    else Right(request)

case class MoveRefV1(
    refId: String,
    san: String,
    uci: String,
    fenAfter: String,
    ply: Int,
    moveNo: Int,
    marker: Option[String]
)
object MoveRefV1:
  given Writes[MoveRefV1] = Json.writes[MoveRefV1]

case class VariationRefV1(
    lineId: String,
    scoreCp: Int,
    mate: Option[Int],
    depth: Int,
    moves: List[MoveRefV1]
)
object VariationRefV1:
  given Writes[VariationRefV1] = Json.writes[VariationRefV1]

case class BookmakerRefsV1(
    schema: String = "chesstory.refs.v1",
    startFen: String,
    startPly: Int,
    variations: List[VariationRefV1]
)
object BookmakerRefsV1:
  given Writes[BookmakerRefsV1] = Json.writes[BookmakerRefsV1]

case class PolishMetaV1(
    provider: String,
    model: Option[String],
    sourceMode: String,
    validationPhase: String,
    validationReasons: List[String],
    cacheHit: Boolean,
    promptTokens: Option[Int],
    cachedTokens: Option[Int],
    completionTokens: Option[Int],
    estimatedCostUsd: Option[Double],
    strategyCoverage: Option[StrategyCoverageMetaV1] = None
)
object PolishMetaV1:
  given Writes[PolishMetaV1] = Json.writes[PolishMetaV1]

case class StrategyCoverageMetaV1(
    mode: String,
    enforced: Boolean,
    threshold: Double,
    availableCategories: Int,
    coveredCategories: Int,
    requiredCategories: Int,
    coverageScore: Double,
    passesThreshold: Boolean,
    planSignals: Int,
    planHits: Int,
    routeSignals: Int,
    routeHits: Int,
    focusSignals: Int,
    focusHits: Int
)
object StrategyCoverageMetaV1:
  given Writes[StrategyCoverageMetaV1] = Json.writes[StrategyCoverageMetaV1]

case class StrategySidePlan(
    side: String,
    horizon: String,
    planName: String,
    priorities: List[String] = Nil,
    riskTriggers: List[String] = Nil
)
object StrategySidePlan:
  given Writes[StrategySidePlan] = Json.writes[StrategySidePlan]

object RouteSurfaceMode:
  val Exact = "exact"
  val Toward = "toward"
  val Hidden = "hidden"

object StrategicIdeaKind:
  val PawnBreak = "pawn_break"
  val SpaceGainOrRestriction = "space_gain_or_restriction"
  val TargetFixing = "target_fixing"
  val LineOccupation = "line_occupation"
  val OutpostCreationOrOccupation = "outpost_creation_or_occupation"
  val MinorPieceImbalanceExploitation = "minor_piece_imbalance_exploitation"
  val Prophylaxis = "prophylaxis"
  val KingAttackBuildUp = "king_attack_build_up"
  val FavorableTradeOrTransformation = "favorable_trade_or_transformation"
  val CounterplaySuppression = "counterplay_suppression"

  val all = List(
    PawnBreak,
    SpaceGainOrRestriction,
    TargetFixing,
    LineOccupation,
    OutpostCreationOrOccupation,
    MinorPieceImbalanceExploitation,
    Prophylaxis,
    KingAttackBuildUp,
    FavorableTradeOrTransformation,
    CounterplaySuppression
  )

object StrategicIdeaGroup:
  val StructuralChange = "structural_change"
  val PieceAndLineManagement = "piece_and_line_management"
  val InteractionAndTransformation = "interaction_and_transformation"

  val all = List(
    StructuralChange,
    PieceAndLineManagement,
    InteractionAndTransformation
  )

object StrategicIdeaReadiness:
  val Ready = "ready"
  val Build = "build"
  val Premature = "premature"
  val Blocked = "blocked"

object DirectionalTargetReadiness:
  val Build = "build"
  val Premature = "premature"
  val Blocked = "blocked"
  val Contested = "contested"

case class StrategyPieceRoute(
    ownerSide: String,
    piece: String,
    from: String,
    route: List[String],
    purpose: String,
    strategicFit: Double,
    tacticalSafety: Double,
    surfaceConfidence: Double,
    surfaceMode: String,
    evidence: List[String] = Nil
):
  def side: String = ownerSide
  def confidence: Double = surfaceConfidence
object StrategyPieceRoute:
  def apply(
      side: String,
      piece: String,
      from: String,
      route: List[String],
      purpose: String,
      confidence: Double,
      evidence: List[String]
  ): StrategyPieceRoute =
    new StrategyPieceRoute(
      ownerSide = side,
      piece = piece,
      from = from,
      route = route,
      purpose = purpose,
      strategicFit = confidence,
      tacticalSafety = confidence,
      surfaceConfidence = confidence,
      surfaceMode =
        if confidence >= 0.82 then RouteSurfaceMode.Exact
        else if confidence >= 0.55 then RouteSurfaceMode.Toward
        else RouteSurfaceMode.Hidden,
      evidence = evidence
    )

  def apply(
      side: String,
      piece: String,
      from: String,
      route: List[String],
      purpose: String,
      confidence: Double
  ): StrategyPieceRoute =
    apply(side, piece, from, route, purpose, confidence, Nil)

  given Writes[StrategyPieceRoute] = Json.writes[StrategyPieceRoute]

case class StrategyPieceMoveRef(
    ownerSide: String,
    piece: String,
    from: String,
    target: String,
    idea: String,
    tacticalTheme: Option[String] = None,
    evidence: List[String] = Nil
)
object StrategyPieceMoveRef:
  given Writes[StrategyPieceMoveRef] = Json.writes[StrategyPieceMoveRef]

case class StrategyDirectionalTarget(
    targetId: String,
    ownerSide: String,
    piece: String,
    from: String,
    targetSquare: String,
    readiness: String,
    strategicReasons: List[String] = Nil,
    prerequisites: List[String] = Nil,
    evidence: List[String] = Nil
)
object StrategyDirectionalTarget:
  given Writes[StrategyDirectionalTarget] = Json.writes[StrategyDirectionalTarget]

case class StrategyIdeaSignal(
    ideaId: String,
    ownerSide: String,
    kind: String,
    group: String,
    readiness: String,
    focusSquares: List[String] = Nil,
    focusFiles: List[String] = Nil,
    focusDiagonals: List[String] = Nil,
    focusZone: Option[String] = None,
    beneficiaryPieces: List[String] = Nil,
    confidence: Double,
    evidenceRefs: List[String] = Nil
)
object StrategyIdeaSignal:
  given Writes[StrategyIdeaSignal] = Json.writes[StrategyIdeaSignal]

case class ActiveStrategicIdeaRef(
    ideaId: String,
    ownerSide: String,
    kind: String,
    group: String,
    readiness: String,
    focusSummary: String,
    confidence: Double
)
object ActiveStrategicIdeaRef:
  given Writes[ActiveStrategicIdeaRef] = Json.writes[ActiveStrategicIdeaRef]

case class NarrativeSignalDigest(
    opening: Option[String] = None,
    strategicStack: List[String] = Nil,
    latentPlan: Option[String] = None,
    latentReason: Option[String] = None,
    decisionComparison: Option[DecisionComparisonDigest] = None,
    authoringEvidence: Option[String] = None,
    practicalVerdict: Option[String] = None,
    practicalFactors: List[String] = Nil,
    compensation: Option[String] = None,
    compensationVectors: List[String] = Nil,
    investedMaterial: Option[Int] = None,
    structuralCue: Option[String] = None,
    structureProfile: Option[String] = None,
    centerState: Option[String] = None,
    alignmentBand: Option[String] = None,
    alignmentReasons: List[String] = Nil,
    deploymentOwnerSide: Option[String] = None,
    deploymentPiece: Option[String] = None,
    deploymentRoute: List[String] = Nil,
    deploymentPurpose: Option[String] = None,
    deploymentContribution: Option[String] = None,
    deploymentStrategicFit: Option[Double] = None,
    deploymentTacticalSafety: Option[Double] = None,
    deploymentSurfaceConfidence: Option[Double] = None,
    deploymentSurfaceMode: Option[String] = None,
    prophylaxisPlan: Option[String] = None,
    prophylaxisThreat: Option[String] = None,
    counterplayScoreDrop: Option[Int] = None,
    dominantIdeaKind: Option[String] = None,
    dominantIdeaGroup: Option[String] = None,
    dominantIdeaReadiness: Option[String] = None,
    dominantIdeaFocus: Option[String] = None,
    secondaryIdeaKind: Option[String] = None,
    secondaryIdeaGroup: Option[String] = None,
    secondaryIdeaFocus: Option[String] = None,
    decision: Option[String] = None,
    strategicFlow: Option[String] = None,
    opponentPlan: Option[String] = None,
    preservedSignals: List[String] = Nil,
    openingRelationClaim: Option[String] = None,
    endgameTransitionClaim: Option[String] = None
):
  def deploymentConfidence: Option[Double] = deploymentSurfaceConfidence
object NarrativeSignalDigest:
  given Writes[NarrativeSignalDigest] = Json.writes[NarrativeSignalDigest]

case class BookmakerLedgerLineV1(
    title: String,
    sanMoves: List[String] = Nil,
    scoreCp: Option[Int] = None,
    mate: Option[Int] = None,
    note: Option[String] = None,
    source: String
)
object BookmakerLedgerLineV1:
  given Writes[BookmakerLedgerLineV1] = Json.writes[BookmakerLedgerLineV1]

case class BookmakerStrategicLedgerV1(
    schema: String = "chesstory.bookmaker.ledger.v1",
    motifKey: String,
    motifLabel: String,
    stageKey: String,
    stageLabel: String,
    carryOver: Boolean,
    stageReason: Option[String] = None,
    prerequisites: List[String] = Nil,
    conversionTrigger: Option[String] = None,
    primaryLine: Option[BookmakerLedgerLineV1] = None,
    resourceLine: Option[BookmakerLedgerLineV1] = None
)
object BookmakerStrategicLedgerV1:
  given Writes[BookmakerStrategicLedgerV1] = Json.writes[BookmakerStrategicLedgerV1]

case class DecisionComparisonDigest(
    chosenMove: Option[String] = None,
    engineBestMove: Option[String] = None,
    engineBestScoreCp: Option[Int] = None,
    engineBestPv: List[String] = Nil,
    cpLossVsChosen: Option[Int] = None,
    deferredMove: Option[String] = None,
    deferredReason: Option[String] = None,
    deferredSource: Option[String] = None,
    evidence: Option[String] = None,
    practicalAlternative: Boolean = false,
    chosenMatchesBest: Boolean = false,
    comparedMove: Option[String] = None,
    comparativeConsequence: Option[String] = None,
    comparativeSource: Option[String] = None
)
object DecisionComparisonDigest:
  given Writes[DecisionComparisonDigest] = Json.writes[DecisionComparisonDigest]

case class StrategyPack(
    schema: String = "chesstory.strategyPack.v2",
    sideToMove: String,
    plans: List[StrategySidePlan] = Nil,
    pieceRoutes: List[StrategyPieceRoute] = Nil,
    pieceMoveRefs: List[StrategyPieceMoveRef] = Nil,
    directionalTargets: List[StrategyDirectionalTarget] = Nil,
    strategicIdeas: List[StrategyIdeaSignal] = Nil,
    longTermFocus: List[String] = Nil,
    evidence: List[String] = Nil,
    signalDigest: Option[NarrativeSignalDigest] = None
)
object StrategyPack:
  given Writes[StrategyPack] = Json.writes[StrategyPack]

case class ActiveStrategicRouteRef(
    routeId: String,
    ownerSide: String,
    piece: String,
    route: List[String],
    purpose: String,
    strategicFit: Double,
    tacticalSafety: Double,
    surfaceConfidence: Double,
    surfaceMode: String
):
  def confidence: Double = surfaceConfidence
object ActiveStrategicRouteRef:
  def apply(
      routeId: String,
      piece: String,
      route: List[String],
      purpose: String,
      confidence: Double
  ): ActiveStrategicRouteRef =
    new ActiveStrategicRouteRef(
      routeId = routeId,
      ownerSide = "white",
      piece = piece,
      route = route,
      purpose = purpose,
      strategicFit = confidence,
      tacticalSafety = confidence,
      surfaceConfidence = confidence,
      surfaceMode =
        if confidence >= 0.82 then RouteSurfaceMode.Exact
        else if confidence >= 0.55 then RouteSurfaceMode.Toward
        else RouteSurfaceMode.Hidden
    )

  given Writes[ActiveStrategicRouteRef] = Json.writes[ActiveStrategicRouteRef]

case class ActiveStrategicMoveRef(
    label: String,
    source: String,
    uci: String,
    san: Option[String] = None,
    fenAfter: Option[String] = None
)
object ActiveStrategicMoveRef:
  given Writes[ActiveStrategicMoveRef] = Json.writes[ActiveStrategicMoveRef]

case class ActiveBranchRouteCue(
    routeId: String,
    ownerSide: String,
    piece: String,
    route: List[String],
    purpose: String,
    strategicFit: Double,
    tacticalSafety: Double,
    surfaceConfidence: Double,
    surfaceMode: String
):
  def confidence: Double = surfaceConfidence
object ActiveBranchRouteCue:
  def apply(
      routeId: String,
      piece: String,
      route: List[String],
      purpose: String,
      confidence: Double
  ): ActiveBranchRouteCue =
    new ActiveBranchRouteCue(
      routeId = routeId,
      ownerSide = "white",
      piece = piece,
      route = route,
      purpose = purpose,
      strategicFit = confidence,
      tacticalSafety = confidence,
      surfaceConfidence = confidence,
      surfaceMode =
        if confidence >= 0.82 then RouteSurfaceMode.Exact
        else if confidence >= 0.55 then RouteSurfaceMode.Toward
        else RouteSurfaceMode.Hidden
    )

  given Writes[ActiveBranchRouteCue] = Json.writes[ActiveBranchRouteCue]

case class ActiveBranchMoveCue(
    label: String,
    uci: String,
    san: Option[String] = None,
    source: String
)
object ActiveBranchMoveCue:
  given Writes[ActiveBranchMoveCue] = Json.writes[ActiveBranchMoveCue]

case class ActiveBranchDossier(
    dominantLens: String,
    chosenBranchLabel: String,
    engineBranchLabel: Option[String] = None,
    deferredBranchLabel: Option[String] = None,
    whyChosen: Option[String] = None,
    whyDeferred: Option[String] = None,
    opponentResource: Option[String] = None,
    routeCue: Option[ActiveBranchRouteCue] = None,
    moveCue: Option[ActiveBranchMoveCue] = None,
    evidenceCue: Option[String] = None,
    continuationFocus: Option[String] = None,
    practicalRisk: Option[String] = None,
    comparisonGapCp: Option[Int] = None,
    threadLabel: Option[String] = None,
    threadStage: Option[String] = None,
    threadSummary: Option[String] = None,
    threadOpponentCounterplan: Option[String] = None
)
object ActiveBranchDossier:
  given Writes[ActiveBranchDossier] = Json.writes[ActiveBranchDossier]

case class ActiveStrategicThread(
    threadId: String,
    side: String,
    themeKey: String,
    themeLabel: String,
    summary: String,
    seedPly: Int,
    lastPly: Int,
    representativePlies: List[Int] = Nil,
    opponentCounterplan: Option[String] = None,
    continuityScore: Double
)
object ActiveStrategicThread:
  given Writes[ActiveStrategicThread] = Json.writes[ActiveStrategicThread]

case class ActiveStrategicThreadRef(
    threadId: String,
    themeKey: String,
    themeLabel: String,
    stageKey: String,
    stageLabel: String
)
object ActiveStrategicThreadRef:
  given Writes[ActiveStrategicThreadRef] = Json.writes[ActiveStrategicThreadRef]

case class AuthorQuestionSummary(
    id: String,
    kind: String,
    priority: Int,
    question: String,
    why: Option[String] = None,
    anchors: List[String] = Nil,
    confidence: String,
    latentPlanName: Option[String] = None,
    latentSeedId: Option[String] = None
)
object AuthorQuestionSummary:
  given Writes[AuthorQuestionSummary] = Json.writes[AuthorQuestionSummary]

case class EvidenceBranchSummary(
    keyMove: String,
    line: String,
    evalCp: Option[Int] = None,
    mate: Option[Int] = None,
    depth: Option[Int] = None,
    sourceId: Option[String] = None
)
object EvidenceBranchSummary:
  given Writes[EvidenceBranchSummary] = Json.writes[EvidenceBranchSummary]

case class AuthorEvidenceSummary(
    questionId: String,
    questionKind: String,
    question: String,
    why: Option[String] = None,
    status: String,
    purposes: List[String] = Nil,
    branchCount: Int = 0,
    branches: List[EvidenceBranchSummary] = Nil,
    pendingProbeIds: List[String] = Nil,
    pendingProbeCount: Int = 0,
    probeObjectives: List[String] = Nil,
    linkedPlans: List[String] = Nil
)
object AuthorEvidenceSummary:
  given Writes[AuthorEvidenceSummary] = Json.writes[AuthorEvidenceSummary]

case class CommentResponse(
  commentary: String,
  concepts: List[String],
  variations: List[lila.llm.model.strategic.VariationLine] = Nil,
  probeRequests: List[lila.llm.model.ProbeRequest] = Nil,
  authorQuestions: List[AuthorQuestionSummary] = Nil,
  authorEvidence: List[AuthorEvidenceSummary] = Nil,
  mainStrategicPlans: List[lila.llm.model.authoring.PlanHypothesis] = Nil,
  strategicPlanExperiments: List[lila.llm.model.StrategicPlanExperiment] = Nil,
  planStateToken: Option[lila.llm.analysis.PlanStateTracker] = None,
  endgameStateToken: Option[lila.llm.model.strategic.EndgamePatternState] = None,
  sourceMode: String = "rule",
  model: Option[String] = None,
  refs: Option[BookmakerRefsV1] = None,
  polishMeta: Option[PolishMetaV1] = None,
  planTier: String = PlanTier.Basic,
  llmLevel: String = LlmLevel.Polish,
  strategyPack: Option[StrategyPack] = None,
  signalDigest: Option[NarrativeSignalDigest] = None,
  bookmakerLedger: Option[BookmakerStrategicLedgerV1] = None
)
object CommentResponse:
  given Writes[CommentResponse] = Json.writes[CommentResponse]

case class BookmakerResult(
    response: CommentResponse,
    cacheHit: Boolean,
    diagnosticPlanSidecar: Option[lila.llm.analysis.PlanEvidenceEvaluator.DiagnosticPlanSidecar] = None
)

object AsyncGameAnalysisDurability:
  val EphemeralMemory = "ephemeral_memory"

case class GameAnalysisValidationError(
    code: String,
    message: String
)
object GameAnalysisValidationError:
  given Writes[GameAnalysisValidationError] = Json.writes[GameAnalysisValidationError]

case class AsyncGameAnalysisSubmitResponse(
    jobId: String,
    status: String,
    statusToken: String,
    refineToken: String,
    durability: String = AsyncGameAnalysisDurability.EphemeralMemory,
    expiresAtMs: Long
)
object AsyncGameAnalysisSubmitResponse:
  given Writes[AsyncGameAnalysisSubmitResponse] = Json.writes[AsyncGameAnalysisSubmitResponse]

case class AsyncGameAnalysisStatusResponse(
    jobId: String,
    status: String,
    createdAtMs: Long,
    updatedAtMs: Long,
    expiresAtMs: Long,
    durability: String = AsyncGameAnalysisDurability.EphemeralMemory,
    result: Option[GameChronicleResponse] = None,
    error: Option[String] = None
)
object AsyncGameAnalysisStatusResponse:
  given Writes[AsyncGameAnalysisStatusResponse] = Json.writes[AsyncGameAnalysisStatusResponse]

case class AnalysisOptions(style: String, focusOn: List[String])
object AnalysisOptions:
  given Reads[AnalysisOptions] = Json.reads[AnalysisOptions]

case class ProbeResultsByPlyEntry(
    ply: Int,
    results: List[lila.llm.model.ProbeResult]
)
object ProbeResultsByPlyEntry:
  given Reads[ProbeResultsByPlyEntry] = Json.reads[ProbeResultsByPlyEntry]
  given Writes[ProbeResultsByPlyEntry] = Json.writes[ProbeResultsByPlyEntry]

case class FullAnalysisRequest(
    pgn: String,
    evals: List[MoveEval],
    options: AnalysisOptions,
    probeResultsByPly: Option[List[ProbeResultsByPlyEntry]] = None,
    variant: Option[String] = None
)
object FullAnalysisRequest:
  val MaxPgnChars = 200000
  val MinGameChroniclePly = 9
  private def moveNumberFromPly(ply: Int): Int = math.max(1, (ply + 1) / 2)

  given Reads[FullAnalysisRequest] = Json.reads[FullAnalysisRequest]

  def validateGameChronicle(request: FullAnalysisRequest): Either[GameAnalysisValidationError, FullAnalysisRequest] =
    val normalizedPgn = Option(request.pgn).map(_.trim).getOrElse("")
    if normalizedPgn.isEmpty then
      Left(GameAnalysisValidationError("invalid_pgn", "PGN payload is empty."))
    else if normalizedPgn.length > MaxPgnChars then
      Left(GameAnalysisValidationError("invalid_pgn", s"PGN payload exceeds $MaxPgnChars characters."))
    else
      lila.llm.PgnAnalysisHelper.extractPlyDataStrict(normalizedPgn) match
        case Left(err)     => Left(GameAnalysisValidationError("invalid_pgn", s"PGN payload is invalid: $err"))
        case Right(plyData) =>
          val totalPly = plyData.lastOption.map(_.ply).getOrElse(0)
          if totalPly < MinGameChroniclePly then
            Left(
              GameAnalysisValidationError(
                "game_chronicle_too_short",
                s"Game Chronicle opens from move ${moveNumberFromPly(MinGameChroniclePly)}. Let the game develop a little more first."
              )
            )
          else Right(request.copy(pgn = normalizedPgn))
