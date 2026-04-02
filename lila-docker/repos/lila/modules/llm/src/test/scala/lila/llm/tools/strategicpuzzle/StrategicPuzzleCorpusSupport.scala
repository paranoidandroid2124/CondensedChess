package lila.llm.tools.strategicpuzzle

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, StandardOpenOption }
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter

import scala.jdk.CollectionConverters.*

import chess.*
import chess.format.{ Fen, Uci }
import chess.variant.Standard
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

import lila.llm.*
import lila.llm.analysis.NarrativeUtils
import lila.llm.model.strategic.VariationLine

object StrategicPuzzleCorpusSupport:

  val Schema = "chesstory.strategicPuzzle.v1"
  val RunnerVersion = "strategic-puzzle-corpus-v1"

  val StructuralEvalAbsMaxPawns = 2.0
  val StructuralMinTotalPieces = 12
  val StructuralMinLegalMoves = 18
  val StructuralMinSumNonPawnMaterial = 20
  val StructuralMinSideNonPawnMaterial = 6
  val RootMultiPv = 6
  val RootDepth = 14
  val RootCandidateCpLossMax = 120
  val RootFullCreditCpLossMax = 90
  val RootTacticalGapCp = 110
  val RootGapPenaltyCp = 80
  val AfterMultiPv = 3
  val AfterDepth = 12
  val MaxAcceptedMoves = 4
  val OpeningCap = 15
  val DominantIdeaCap = 150
  val MinSideRatio = 0.35
  val PublishTarget = 1000

  final case class CetSeed(
      id: String,
      fen: String,
      evaluation: Option[Double],
      whiteIsBetter: Option[Boolean],
      whiteToMove: Boolean,
      opening: Option[String],
      eco: Option[String],
      lichessURL: Option[String],
      firestoreName: Option[String],
      createTime: Option[String],
      updateTime: Option[String]
  ):
    lazy val fenKey: String = normalizeFenKey(fen)
    lazy val ply: Int = plyFromFen(fen)
    lazy val sideToMove: String = if whiteToMove then "white" else "black"

  object CetSeed:
    private val idReads: Reads[String] =
      Reads {
        case JsString(value) => JsSuccess(value)
        case JsNumber(value) => JsSuccess(value.toBigInt.toString)
        case other           => JsError(s"unsupported seed id: $other")
      }
    given Reads[CetSeed] =
      (
        (JsPath \ "id").read(using idReads) and
          (JsPath \ "fen").read[String] and
          (JsPath \ "evaluation").readNullable[Double] and
          (JsPath \ "whiteIsBetter").readNullable[Boolean] and
          (JsPath \ "whiteToMove").readWithDefault[Boolean](true) and
          (JsPath \ "opening").readNullable[String] and
          (JsPath \ "ECO").readNullable[String] and
          (JsPath \ "lichessURL").readNullable[String] and
          (JsPath \ "_firestore_name").readNullable[String] and
          (JsPath \ "_create_time").readNullable[String] and
          (JsPath \ "_update_time").readNullable[String]
      )(CetSeed.apply)
    given Writes[CetSeed] = Json.writes[CetSeed]

  final case class StructuralMetrics(
      fenKey: String,
      totalPieces: Int,
      whiteNonPawnMaterial: Int,
      blackNonPawnMaterial: Int,
      sumNonPawnMaterial: Int,
      whiteMinorMajorPieces: Int,
      blackMinorMajorPieces: Int,
      legalMoves: Int,
      seedEvalPawns: Option[Double]
  )
  object StructuralMetrics:
    given Reads[StructuralMetrics] = Json.reads[StructuralMetrics]
    given Writes[StructuralMetrics] = Json.writes[StructuralMetrics]

  final case class StructuralScreening(
      passed: Boolean,
      reasons: List[String],
      metrics: StructuralMetrics
  )
  object StructuralScreening:
    given Reads[StructuralScreening] = Json.reads[StructuralScreening]
    given Writes[StructuralScreening] = Json.writes[StructuralScreening]

  final case class SourcePayload(
      provider: String,
      dataset: String,
      seedId: String,
      opening: Option[String],
      eco: Option[String],
      seedEvalPawns: Option[Double],
      whiteIsBetter: Option[Boolean],
      lichessURL: Option[String],
      importedAt: Option[String]
  )
  object SourcePayload:
    given Reads[SourcePayload] = Json.reads[SourcePayload]
    given Writes[SourcePayload] = Json.writes[SourcePayload]

  final case class PositionPayload(
      fen: String,
      fenKey: String,
      phase: String,
      ply: Int,
      sideToMove: String
  )
  object PositionPayload:
    given Reads[PositionPayload] = Json.reads[PositionPayload]
    given Writes[PositionPayload] = Json.writes[PositionPayload]

  final case class Stage01StructuralPassRow(
      source: SourcePayload,
      position: PositionPayload,
      screening: StructuralScreening
  )
  object Stage01StructuralPassRow:
    given Reads[Stage01StructuralPassRow] = Json.reads[Stage01StructuralPassRow]
    given Writes[Stage01StructuralPassRow] = Json.writes[Stage01StructuralPassRow]

  final case class EnginePv(
      rank: Int,
      cp: Option[Int],
      mate: Option[Int],
      depth: Int,
      moves: List[String]
  ):
    def firstMove: Option[String] = moves.headOption
    def effectiveCp: Int = mate.fold(cp.getOrElse(0))(mateScoreToCp)
    def toVariationLine(startFen: String): VariationLine =
      VariationLine(
        moves = moves,
        scoreCp = cp.getOrElse(effectiveCp),
        mate = mate,
        depth = depth,
        resultingFen = Option.when(moves.nonEmpty)(NarrativeUtils.uciListToFen(startFen, moves))
      )

  object EnginePv:
    given Reads[EnginePv] =
      (
        (JsPath \ "rank").read[Int] and
          (JsPath \ "cp").readNullable[Int] and
          (JsPath \ "mate").readNullable[Int] and
          (JsPath \ "depth").readWithDefault[Int](0) and
          (JsPath \ "moves").read[String].map(parseMoveString)
      )(EnginePv.apply)
    given Writes[EnginePv] = Json.writes[EnginePv]

  final case class CandidateMove(
      rank: Int,
      uci: String,
      san: String,
      scoreCp: Int,
      mate: Option[Int],
      cpLoss: Int,
      depth: Int,
      afterFen: String
  )
  object CandidateMove:
    given Reads[CandidateMove] = Json.reads[CandidateMove]
    given Writes[CandidateMove] = Json.writes[CandidateMove]

  final case class RootAnalysis(
      depth: Int,
      multiPv: Int,
      evalCp: Int,
      gapCp: Option[Int],
      nearBestCount: Int,
      candidateMoves: List[CandidateMove],
      variations: List[VariationLine]
  )
  object RootAnalysis:
    given Reads[RootAnalysis] = Json.reads[RootAnalysis]
    given Writes[RootAnalysis] = Json.writes[RootAnalysis]

  final case class RootStrategicPreScan(
      passed: Boolean,
      reasons: List[String],
      strategicIdeaCount: Int,
      pieceRouteCount: Int,
      directionalTargetCount: Int,
      dominantIdeaKind: Option[String]
  )
  object RootStrategicPreScan:
    given Reads[RootStrategicPreScan] = Json.reads[RootStrategicPreScan]
    given Writes[RootStrategicPreScan] = Json.writes[RootStrategicPreScan]

  final case class Stage02RootAnalysisRow(
      source: SourcePayload,
      position: PositionPayload,
      screening: StructuralScreening,
      rootAnalysis: RootAnalysis,
      preScan: RootStrategicPreScan
  )
  object Stage02RootAnalysisRow:
    given Reads[Stage02RootAnalysisRow] = Json.reads[Stage02RootAnalysisRow]
    given Writes[Stage02RootAnalysisRow] = Json.writes[Stage02RootAnalysisRow]

  final case class ExplanationGate(
      passed: Boolean,
      reasons: List[String],
      commentaryChars: Int,
      signalCategoryCount: Int,
      strategyCoveragePassed: Option[Boolean],
      sourceMode: String
  )
  object ExplanationGate:
    given Reads[ExplanationGate] = Json.reads[ExplanationGate]
    given Writes[ExplanationGate] = Json.writes[ExplanationGate]

  final case class MoveFamilySummary(
      key: Option[String],
      dominantIdeaKind: Option[String],
      anchor: Option[String],
      robust: Boolean,
      signalRichness: Int
  )
  object MoveFamilySummary:
    given Reads[MoveFamilySummary] = Json.reads[MoveFamilySummary]
    given Writes[MoveFamilySummary] = Json.writes[MoveFamilySummary]

  final case class Stage03MoveAnalysisRow(
      seedId: String,
      fenKey: String,
      move: CandidateMove,
      family: MoveFamilySummary,
      explanation: ExplanationGate,
      response: JsObject,
      generatedAt: String
  )
  object Stage03MoveAnalysisRow:
    given Reads[Stage03MoveAnalysisRow] = Json.reads[Stage03MoveAnalysisRow]
    given Writes[Stage03MoveAnalysisRow] = Json.writes[Stage03MoveAnalysisRow]

  final case class DominantFamilySummary(
      key: String,
      dominantIdeaKind: String,
      anchor: String,
      memberCount: Int,
      averageCpLoss: Double,
      averageSignalRichness: Double
  )
  object DominantFamilySummary:
    given Reads[DominantFamilySummary] = Json.reads[DominantFamilySummary]
    given Writes[DominantFamilySummary] = Json.writes[DominantFamilySummary]

  final case class PuzzleMoveDoc(
      uci: String,
      san: String,
      cpLoss: Int,
      scoreCp: Int,
      mate: Option[Int],
      depth: Int,
      familyKey: Option[String],
      dominantIdeaKind: Option[String],
      anchor: Option[String],
      signalRichness: Int,
      explanation: ExplanationGate,
      afterFen: String,
      commentResponse: JsObject
  )
  object PuzzleMoveDoc:
    given Reads[PuzzleMoveDoc] = Json.reads[PuzzleMoveDoc]
    given Writes[PuzzleMoveDoc] = Json.writes[PuzzleMoveDoc]

  final case class QualityScore(
      middlegameScore: Int,
      familyScore: Int,
      explanationScore: Int,
      diversityBonus: Int,
      total: Int
  )
  object QualityScore:
    given Reads[QualityScore] = Json.reads[QualityScore]
    given Writes[QualityScore] = Json.writes[QualityScore]

  final case class GenerationMeta(
      generatedAt: String,
      runnerVersion: String,
      selectionStatus: String
  )
  object GenerationMeta:
    given Reads[GenerationMeta] = Json.reads[GenerationMeta]
    given Writes[GenerationMeta] = Json.writes[GenerationMeta]

  final case class StrategicPuzzleDoc(
      id: String,
      schema: String,
      source: SourcePayload,
      position: PositionPayload,
      screening: StructuralScreening,
      rootAnalysis: RootAnalysis,
      dominantFamily: DominantFamilySummary,
      acceptedMoves: List[PuzzleMoveDoc],
      partialMoves: List[PuzzleMoveDoc],
      alternateMoves: List[PuzzleMoveDoc],
      qualityScore: QualityScore,
      generationMeta: GenerationMeta
  )
  object StrategicPuzzleDoc:
    given Reads[StrategicPuzzleDoc] = Json.reads[StrategicPuzzleDoc]
    given Writes[StrategicPuzzleDoc] = Json.writes[StrategicPuzzleDoc]

  final case class RejectRow(
      seedId: String,
      fen: String,
      fenKey: String,
      stage: String,
      reasons: List[String]
  )
  object RejectRow:
    given Reads[RejectRow] = Json.reads[RejectRow]
    given Writes[RejectRow] = Json.writes[RejectRow]

  final case class SelectionOutcome(
      autoPublish: List[StrategicPuzzleDoc],
      reserve: List[StrategicPuzzleDoc]
  )

  final case class StructuralComputation(
      position: PositionPayload,
      screening: StructuralScreening
  )

  final case class RootScreenResult(
      rootAnalysis: RootAnalysis,
      reasons: List[String]
  ):
    def passed: Boolean = reasons.isEmpty

  final case class FamilyCandidate(
      summary: DominantFamilySummary,
      members: List[Stage03MoveAnalysisRow]
  )

  def normalizeFenKey(fen: String): String =
    fen.trim.split("\\s+").take(4).mkString(" ")

  def plyFromFen(fen: String): Int =
    val fields = fen.trim.split("\\s+")
    if fields.length < 6 then 0
    else
      val side = fields(1)
      val fullMove = fields(5).toIntOption.getOrElse(1).max(1)
      (fullMove - 1) * 2 + (if side == "b" then 1 else 0)

  def isoNow(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

  def stablePuzzleId(fen: String, dominantFamilyKey: String, acceptedMoves: List[PuzzleMoveDoc]): String =
    val acceptedHead = acceptedMoves.map(_.uci).sorted.mkString(",")
    s"spz_${sha256Hex(s"$fen|$dominantFamilyKey|$acceptedHead").take(24)}"

  def sourcePayload(seed: CetSeed): SourcePayload =
    SourcePayload(
      provider = "chessevaluationtraining",
      dataset = "chessevaluationtraining_positions",
      seedId = seed.id,
      opening = clean(seed.opening),
      eco = clean(seed.eco),
      seedEvalPawns = seed.evaluation,
      whiteIsBetter = seed.whiteIsBetter,
      lichessURL = clean(seed.lichessURL),
      importedAt = seed.createTime
    )

  def structuralComputation(seed: CetSeed): Either[String, StructuralComputation] =
    Fen
      .read(Standard, Fen.Full(seed.fen))
      .toRight(s"invalid fen for seed ${seed.id}")
      .map { pos =>
        val board = pos.board
        val whiteNonPawn = nonPawnMaterial(board, Color.White)
        val blackNonPawn = nonPawnMaterial(board, Color.Black)
        val metrics =
          StructuralMetrics(
            fenKey = seed.fenKey,
            totalPieces = board.occupied.count,
            whiteNonPawnMaterial = whiteNonPawn,
            blackNonPawnMaterial = blackNonPawn,
            sumNonPawnMaterial = whiteNonPawn + blackNonPawn,
            whiteMinorMajorPieces = minorMajorPieces(board, Color.White),
            blackMinorMajorPieces = minorMajorPieces(board, Color.Black),
            legalMoves = pos.legalMoves.size,
            seedEvalPawns = seed.evaluation
          )
        val reasons =
          List(
            Option.when(metrics.totalPieces <= 7)("tablebase_like_piece_count"),
            Option.when(metrics.totalPieces < StructuralMinTotalPieces)("too_few_total_pieces"),
            Option.when(metrics.sumNonPawnMaterial < StructuralMinSumNonPawnMaterial)("too_little_non_pawn_material"),
            Option.when(metrics.whiteNonPawnMaterial < StructuralMinSideNonPawnMaterial)("white_non_pawn_material_too_low"),
            Option.when(metrics.blackNonPawnMaterial < StructuralMinSideNonPawnMaterial)("black_non_pawn_material_too_low"),
            Option.when(metrics.legalMoves < StructuralMinLegalMoves)("legal_move_count_too_low"),
            Option.when(metrics.seedEvalPawns.exists(_.abs > StructuralEvalAbsMaxPawns))("seed_eval_too_extreme")
          ).flatten
        StructuralComputation(
          position =
            PositionPayload(
              fen = seed.fen,
              fenKey = seed.fenKey,
              phase = "middlegame",
              ply = seed.ply,
              sideToMove = seed.sideToMove
            ),
          screening = StructuralScreening(passed = reasons.isEmpty, reasons = reasons, metrics = metrics)
        )
      }

  def screenRootPosition(fen: String, lines: List[EnginePv]): Either[List[String], RootScreenResult] =
    val normalized = lines.sortBy(_.rank).take(RootMultiPv)
    if normalized.isEmpty then Left(List("root_engine_returned_no_lines"))
    else if normalized.exists(_.mate.isDefined) then Left(List("root_engine_detected_mate"))
    else
      val bestCp = normalized.head.effectiveCp
      val candidateMoves =
        normalized.flatMap { pv =>
          for
            uci <- pv.firstMove
            afterFen <- applySingleMove(fen, uci)
          yield
            CandidateMove(
              rank = pv.rank,
              uci = uci,
              san = sanForMove(fen, uci),
              scoreCp = pv.effectiveCp,
              mate = pv.mate,
              cpLoss = (bestCp - pv.effectiveCp).max(0),
              depth = pv.depth,
              afterFen = afterFen
            )
        }.filter(_.cpLoss <= RootCandidateCpLossMax).sortBy(cm => (cm.cpLoss, cm.rank)).take(RootMultiPv)
      val gapCp = normalized.lift(1).map(second => bestCp - second.effectiveCp)
      val nearBestCount = candidateMoves.count(_.cpLoss <= RootFullCreditCpLossMax)
      val reasons =
        List(
          Option.when(candidateMoves.isEmpty)("no_candidates_within_cp_band"),
          Option.when(gapCp.exists(_ > RootTacticalGapCp) && nearBestCount <= 1)("root_gap_indicates_tactical_single_answer")
        ).flatten
      if reasons.nonEmpty then Left(reasons)
      else
        Right(
          RootScreenResult(
            rootAnalysis =
              RootAnalysis(
                depth = normalized.map(_.depth).maxOption.getOrElse(0),
                multiPv = normalized.size,
                evalCp = bestCp,
                gapCp = gapCp,
                nearBestCount = nearBestCount,
                candidateMoves = candidateMoves,
                variations = normalized.map(_.toVariationLine(fen))
              ),
            reasons = Nil
          )
        )

  def summarizeRootPreScan(strategyPack: Option[StrategyPack], digest: Option[NarrativeSignalDigest]): RootStrategicPreScan =
    val resolvedDigest = digest.orElse(strategyPack.flatMap(_.signalDigest))
    val passes =
      strategyPack.exists(p =>
        p.strategicIdeas.nonEmpty || p.pieceRoutes.nonEmpty || p.directionalTargets.nonEmpty
      ) || resolvedDigest.flatMap(_.dominantIdeaKind).exists(_.trim.nonEmpty)
    val reasons =
      if passes then Nil
      else
        List(
          "no_root_strategic_signals"
        )
    RootStrategicPreScan(
      passed = passes,
      reasons = reasons,
      strategicIdeaCount = strategyPack.map(_.strategicIdeas.size).getOrElse(0),
      pieceRouteCount = strategyPack.map(_.pieceRoutes.size).getOrElse(0),
      directionalTargetCount = strategyPack.map(_.directionalTargets.size).getOrElse(0),
      dominantIdeaKind = resolvedDigest.flatMap(_.dominantIdeaKind).flatMap(nonBlank)
    )

  def explanationGate(response: CommentResponse): ExplanationGate =
    buildExplanationGate(
      response = response,
      requireLlmPolish = true,
      minCommentaryChars = 140,
      requireCoveragePass = true
    )

  def prePolishExplanationGate(response: CommentResponse): ExplanationGate =
    buildExplanationGate(
      response = response,
      requireLlmPolish = false,
      minCommentaryChars = 80,
      requireCoveragePass = false
    )

  private def buildExplanationGate(
      response: CommentResponse,
      requireLlmPolish: Boolean,
      minCommentaryChars: Int,
      requireCoveragePass: Boolean
  ): ExplanationGate =
    val resolvedDigest = response.signalDigest.orElse(response.strategyPack.flatMap(_.signalDigest))
    val signalCount = signalCategoryCount(response, resolvedDigest)
    val coveragePassed = response.polishMeta.flatMap(_.strategyCoverage).map(_.passesThreshold)
    val reasons =
      List(
        Option.when(requireLlmPolish && response.sourceMode != "llm_polished")("source_mode_not_llm_polished"),
        Option.when(response.strategyPack.isEmpty && resolvedDigest.flatMap(_.dominantIdeaKind).forall(_.trim.isEmpty))("missing_strategy_signals"),
        Option.when(response.commentary.trim.length < minCommentaryChars)("commentary_too_short"),
        Option.when(signalCount < 2)("insufficient_signal_categories"),
        Option.when(requireCoveragePass && coveragePassed.contains(false))("strategy_coverage_failed")
      ).flatten
    ExplanationGate(
      passed = reasons.isEmpty,
      reasons = reasons,
      commentaryChars = response.commentary.trim.length,
      signalCategoryCount = signalCount,
      strategyCoveragePassed = coveragePassed,
      sourceMode = response.sourceMode
    )

  def familySummary(response: CommentResponse, cpLoss: Int): MoveFamilySummary =
    familySummary(response, cpLoss, explanationGate(response))

  def prePolishFamilySummary(response: CommentResponse, cpLoss: Int): MoveFamilySummary =
    familySummary(response, cpLoss, prePolishExplanationGate(response))

  private val GenericTaskKeys = Set(
    "activity",
    "centralize",
    "consolidate",
    "improve_piece_placement",
    "improve_pieces",
    "keep_improving",
    "keep_pressure",
    "maintain_pressure",
    "play_positionally",
    "quiet_improvement",
    "stabilize",
    "wait"
  )

  private val BoardSquarePattern = raw"\b([a-h][1-8])\b".r
  private val FilePattern = raw"\b([a-h])(?:-|\s)?file\b".r
  private val ZonePattern =
    raw"\b(queenside|kingside|center|centre|dark squares|light squares|dark-square complex|light-square complex)\b".r
  private val TaskLexemes = Set(
    "bind",
    "break",
    "clamp",
    "counterplay",
    "endgame",
    "ending",
    "entry",
    "file",
    "infiltration",
    "initiative",
    "outpost",
    "pawn",
    "passed",
    "rook",
    "queen",
    "trade",
    "transition"
  )

  private def normalizedWords(raw: String): List[String] =
    Option(raw)
      .map(_.trim.toLowerCase.replaceAll("[^a-z0-9]+", " "))
      .filter(_.nonEmpty)
      .toList
      .flatMap(_.split("\\s+").toList)
      .filter(_.nonEmpty)

  private def normalizedTaskKey(raw: String, maxWords: Int): Option[String] =
    val words = normalizedWords(raw)
    Option.when(words.nonEmpty && words.size <= maxWords)(words.mkString("_"))

  private def boardAnchorFromText(raw: String): Option[String] =
    val lowered = Option(raw).map(_.trim.toLowerCase).filter(_.nonEmpty)
    lowered.flatMap(text =>
      BoardSquarePattern.findFirstMatchIn(text).map(_.group(1)).orElse {
        FilePattern.findFirstMatchIn(text).map(m => s"${m.group(1)}_file")
      }.orElse {
        ZonePattern.findFirstMatchIn(text).flatMap(m => normalizedTaskKey(m.group(1).replace("centre", "center"), 3))
      }
    )

  private def boundedPhraseKey(raw: String, maxWords: Int = 4): Option[String] =
    val words = normalizedWords(raw)
    val key = words.mkString("_")
    val hasBoardLexeme =
      boardAnchorFromText(raw).isDefined ||
        words.exists(w => Set("queenside", "kingside", "center", "centre", "dark", "light").contains(w))
    val hasTaskLexeme = words.exists(TaskLexemes.contains)
    Option.when(
      words.nonEmpty &&
        words.size <= maxWords &&
        (hasBoardLexeme || hasTaskLexeme) &&
        !GenericTaskKeys.contains(key)
    )(key)

  private def directionalAnchor(response: CommentResponse): Option[String] =
    response.strategyPack.flatMap(_.directionalTargets.headOption.map(_.targetSquare)).flatMap(nonBlank).map(_.trim.toLowerCase)

  private def routeDestinationAnchor(digest: Option[NarrativeSignalDigest]): Option[String] =
    digest.flatMap(_.deploymentRoute.lastOption).flatMap(nonBlank).map(_.trim.toLowerCase)

  private def focusAnchor(digest: Option[NarrativeSignalDigest]): Option[String] =
    digest.flatMap(_.dominantIdeaFocus).flatMap(boardAnchorFromText)

  private def counterplayAnchor(digest: Option[NarrativeSignalDigest]): Option[String] =
    List(
      digest.flatMap(_.prophylaxisThreat),
      digest.flatMap(_.opponentPlan),
      digest.flatMap(_.prophylaxisPlan)
    ).flatten.flatMap(raw => boardAnchorFromText(raw).orElse(boundedPhraseKey(raw))).headOption

  private def transitionAnchor(digest: Option[NarrativeSignalDigest]): Option[String] =
    digest.flatMap(_.endgameTransitionClaim).flatMap(boundedPhraseKey(_))

  private def routePurposeAnchor(
      response: CommentResponse,
      digest: Option[NarrativeSignalDigest]
  ): Option[String] =
    response.strategyPack.toList.flatMap(_.pieceRoutes.map(_.purpose)).flatMap(raw => boundedPhraseKey(raw)).headOption
      .orElse(digest.flatMap(_.deploymentPurpose).flatMap(raw => boundedPhraseKey(raw)))

  private def boundedTaskAnchor(
      response: CommentResponse,
      digest: Option[NarrativeSignalDigest],
      kind: Option[String]
  ): Option[String] =
    val prophylaxisKinds = Set(StrategicIdeaKind.Prophylaxis, StrategicIdeaKind.CounterplaySuppression)
    val transitionKinds = Set(StrategicIdeaKind.FavorableTradeOrTransformation)
    val candidates =
      if kind.exists(prophylaxisKinds.contains) then
        List(
          counterplayAnchor(digest),
          directionalAnchor(response),
          focusAnchor(digest),
          routeDestinationAnchor(digest),
          routePurposeAnchor(response, digest),
          transitionAnchor(digest)
        )
      else if kind.exists(transitionKinds.contains) then
        List(
          transitionAnchor(digest),
          counterplayAnchor(digest),
          directionalAnchor(response),
          focusAnchor(digest),
          routeDestinationAnchor(digest),
          routePurposeAnchor(response, digest)
        )
      else
        List(
          directionalAnchor(response),
          focusAnchor(digest),
          routeDestinationAnchor(digest),
          routePurposeAnchor(response, digest),
          counterplayAnchor(digest),
          transitionAnchor(digest)
        )
    candidates.flatten.headOption

  def familySummary(response: CommentResponse, cpLoss: Int, gate: ExplanationGate): MoveFamilySummary =
    val resolvedDigest = response.signalDigest.orElse(response.strategyPack.flatMap(_.signalDigest))
    val kind = resolvedDigest.flatMap(_.dominantIdeaKind).flatMap(nonBlank)
    val anchor = boundedTaskAnchor(response, resolvedDigest, kind)
    val key = kind.zip(anchor).headOption.map { case (ideaKind, resolvedAnchor) =>
      s"$ideaKind|$resolvedAnchor"
    }
    MoveFamilySummary(
      key = key,
      dominantIdeaKind = kind,
      anchor = anchor,
      robust = kind.isDefined && anchor.isDefined && cpLoss <= RootFullCreditCpLossMax && gate.passed,
      signalRichness = signalRichness(response)
    )

  def chooseDominantFamily(rows: List[Stage03MoveAnalysisRow]): Option[FamilyCandidate] =
    val grouped =
      rows
        .filter(row => row.family.robust && row.family.key.isDefined)
        .groupBy(_.family.key.get)
        .toList
        .flatMap { case (key, members) =>
          val ideaKind = members.flatMap(_.family.dominantIdeaKind).headOption
          val anchor = members.flatMap(_.family.anchor).headOption
          ideaKind.zip(anchor).headOption.map { case (dominantIdeaKind, resolvedAnchor) =>
            FamilyCandidate(
              summary =
                DominantFamilySummary(
                  key = key,
                  dominantIdeaKind = dominantIdeaKind,
                  anchor = resolvedAnchor,
                  memberCount = members.size,
                  averageCpLoss = average(members.map(_.move.cpLoss.toDouble)),
                  averageSignalRichness = average(members.map(_.family.signalRichness.toDouble))
                ),
              members = members.sortBy(row => (row.move.cpLoss, -row.family.signalRichness, row.move.rank))
            )
          }
        }
    grouped.sortBy(fc => (-fc.summary.memberCount, fc.summary.averageCpLoss, -fc.summary.averageSignalRichness, fc.summary.key)).headOption

  def buildPuzzleMoveDoc(row: Stage03MoveAnalysisRow): PuzzleMoveDoc =
    PuzzleMoveDoc(
      uci = row.move.uci,
      san = row.move.san,
      cpLoss = row.move.cpLoss,
      scoreCp = row.move.scoreCp,
      mate = row.move.mate,
      depth = row.move.depth,
      familyKey = row.family.key,
      dominantIdeaKind = row.family.dominantIdeaKind,
      anchor = row.family.anchor,
      signalRichness = row.family.signalRichness,
      explanation = row.explanation,
      afterFen = row.move.afterFen,
      commentResponse = row.response
    )

  def assignCredits(
      dominantFamily: DominantFamilySummary,
      rows: List[Stage03MoveAnalysisRow]
  ): (List[PuzzleMoveDoc], List[PuzzleMoveDoc], List[PuzzleMoveDoc]) =
    val accepted =
      rows
        .filter(row => row.family.key.contains(dominantFamily.key) && row.move.cpLoss <= RootFullCreditCpLossMax && row.explanation.passed)
        .sortBy(row => (row.move.cpLoss, -row.family.signalRichness, row.move.rank))
        .take(MaxAcceptedMoves)
        .map(buildPuzzleMoveDoc)

    val partialRows =
      rows
        .filter(row =>
          row.move.cpLoss <= RootCandidateCpLossMax &&
            !row.family.key.contains(dominantFamily.key) &&
            row.family.robust &&
            row.explanation.passed
        )
        .sortBy(row => (row.move.cpLoss, -row.family.signalRichness, row.move.rank))
        .take(MaxAcceptedMoves)

    val partial = partialRows.map(buildPuzzleMoveDoc)
    val alternate = partialRows.filter(_.move.cpLoss <= RootFullCreditCpLossMax + 20).map(buildPuzzleMoveDoc)
    (accepted, partial, alternate)

  def scorePuzzleDoc(
      doc: StrategicPuzzleDoc,
      openingFrequency: Int,
      familyFrequency: Int,
      sideFrequency: Int,
      poolSize: Int
  ): StrategicPuzzleDoc =
    val middlegameScore = computeMiddlegameScore(doc)
    val familyScore = computeFamilyScore(doc)
    val explanationScore = computeExplanationScore(doc)
    val diversityBonus =
      computeDiversityBonus(
        openingFrequency = openingFrequency,
        familyFrequency = familyFrequency,
        sideFrequency = sideFrequency,
        poolSize = poolSize
      )
    val gapPenalty = if doc.rootAnalysis.gapCp.exists(_ > RootGapPenaltyCp) then 4 else 0
    val total = (middlegameScore + familyScore + explanationScore + diversityBonus - gapPenalty).max(0).min(100)
    doc.copy(
      qualityScore =
        QualityScore(
          middlegameScore = middlegameScore,
          familyScore = familyScore,
          explanationScore = explanationScore,
          diversityBonus = diversityBonus,
          total = total
        )
    )

  def selectAutoPublish(
      docs: List[StrategicPuzzleDoc],
      targetCount: Int = PublishTarget
  ): SelectionOutcome =
    val whiteFloor = math.ceil(targetCount * MinSideRatio).toInt
    val blackFloor = math.ceil(targetCount * MinSideRatio).toInt

    val selectedWhite =
      greedySelect(
        candidates = docs.filter(_.position.sideToMove == "white"),
        limit = whiteFloor,
        alreadySelected = Nil
      )
    val selectedBlack =
      greedySelect(
        candidates = docs.filter(_.position.sideToMove == "black").filterNot(d => selectedWhite.exists(_.id == d.id)),
        limit = blackFloor,
        alreadySelected = selectedWhite
      )
    val preselected = selectedWhite ::: selectedBlack
    val remaining = docs.filterNot(d => preselected.exists(_.id == d.id))
    val filled =
      preselected ::: greedySelect(
        candidates = remaining,
        limit = (targetCount - preselected.size).max(0),
        alreadySelected = preselected
      )
    val publish = filled.take(targetCount)
    val reserve = docs.filterNot(doc => publish.exists(_.id == doc.id)).sortBy(doc => (-doc.qualityScore.total, doc.id))
    SelectionOutcome(
      autoPublish = publish.sortBy(doc => (-doc.qualityScore.total, doc.id)),
      reserve = reserve
    )

  def moveAnalysisKey(row: Stage03MoveAnalysisRow): String = s"${row.seedId}:${row.move.uci}"

  def readJsonl[T: Reads](path: Path): Either[String, List[T]] =
    if !Files.exists(path) then Right(Nil)
    else
      val lines = Files.readAllLines(path, StandardCharsets.UTF_8).asScala.toList.filter(_.trim.nonEmpty)
      lines.zipWithIndex.foldLeft[Either[String, List[T]]](Right(Nil)) { case (acc, (line, idx)) =>
        for
          existing <- acc
          parsed <- Json.parse(line).validate[T].asEither.left.map(err => s"${path.getFileName}: line ${idx + 1}: $err")
        yield existing :+ parsed
      }

  def writeJson(path: Path, value: JsValue): Unit =
    ensureParent(path)
    Files.writeString(path, Json.prettyPrint(value) + "\n", StandardCharsets.UTF_8)

  def writeJsonl[T: Writes](path: Path, rows: Iterable[T]): Unit =
    ensureParent(path)
    val body = rows.iterator.map(row => Json.stringify(Json.toJson(row))).mkString("\n")
    Files.writeString(path, if body.nonEmpty then body + "\n" else "", StandardCharsets.UTF_8)

  def appendJsonl[T: Writes](path: Path, row: T): Unit =
    ensureParent(path)
    Files.writeString(
      path,
      Json.stringify(Json.toJson(row)) + "\n",
      StandardCharsets.UTF_8,
      StandardOpenOption.CREATE,
      StandardOpenOption.APPEND
    )

  def runDirName(now: Instant = Instant.now()): String =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC).format(now)

  def parseMoveString(raw: String): List[String] =
    raw.trim.split("\\s+").toList.filter(_.nonEmpty)

  def bestEvalData(lines: List[VariationLine]): Option[EvalData] =
    lines.headOption.map(line => EvalData(cp = line.effectiveScore, mate = line.mate, pv = Option.when(line.moves.nonEmpty)(line.moves)))

  def responseJson(response: CommentResponse): JsObject =
    Json.toJson(response).as[JsObject]

  private def greedySelect(
      candidates: List[StrategicPuzzleDoc],
      limit: Int,
      alreadySelected: List[StrategicPuzzleDoc]
  ): List[StrategicPuzzleDoc] =
    val selected = scala.collection.mutable.ListBuffer.from(alreadySelected)
    val remaining = scala.collection.mutable.ArrayBuffer.from(candidates)
    val picked = scala.collection.mutable.ListBuffer.empty[StrategicPuzzleDoc]

    while picked.size < limit && remaining.nonEmpty do
      val eligible =
        remaining.filter { doc =>
          selected.count(_.source.opening == doc.source.opening) < OpeningCap &&
            selected.count(_.dominantFamily.dominantIdeaKind == doc.dominantFamily.dominantIdeaKind) < DominantIdeaCap
        }
      if eligible.isEmpty then remaining.clear()
      else
        val next =
          eligible.maxBy { doc =>
            val openingFamilyCount =
              selected.count { existing =>
                existing.source.opening == doc.source.opening && existing.dominantFamily.key == doc.dominantFamily.key
              }
            val dynamicPenalty = openingFamilyCount * 2
            doc.qualityScore.total - dynamicPenalty
          }
        picked += next
        selected += next
        remaining -= next

    picked.toList

  private def computeMiddlegameScore(doc: StrategicPuzzleDoc): Int =
    val metrics = doc.screening.metrics
    val material = normalized(metrics.sumNonPawnMaterial.toDouble, lower = 20.0, upper = 32.0)
    val legalMoves = normalized(metrics.legalMoves.toDouble, lower = 18.0, upper = 40.0)
    val evalBalance = 1.0 - normalized(doc.rootAnalysis.evalCp.abs.toDouble, lower = 0.0, upper = 200.0)
    math.round(30.0 * (0.45 * material + 0.35 * legalMoves + 0.20 * evalBalance)).toInt.clamp(0, 30)

  private def computeFamilyScore(doc: StrategicPuzzleDoc): Int =
    val acceptedCount = doc.acceptedMoves.size
    val countScore = if acceptedCount >= 2 then 1.0 else 0.78
    val cpScore = 1.0 - normalized(doc.dominantFamily.averageCpLoss, lower = 0.0, upper = RootFullCreditCpLossMax.toDouble)
    val signalScore = normalized(doc.dominantFamily.averageSignalRichness, lower = 2.0, upper = 12.0)
    math.round(30.0 * (0.4 * countScore + 0.35 * cpScore + 0.25 * signalScore)).toInt.clamp(0, 30)

  private def computeExplanationScore(doc: StrategicPuzzleDoc): Int =
    val accepted = doc.acceptedMoves
    val avgCommentaryChars = average(accepted.map(_.explanation.commentaryChars.toDouble))
    val avgSignalCount = average(accepted.map(_.explanation.signalCategoryCount.toDouble))
    val coverageScore =
      if accepted.exists(_.explanation.strategyCoveragePassed.contains(true)) then 1.0 else 0.85
    val commentaryScore = normalized(avgCommentaryChars, lower = 140.0, upper = 420.0)
    val signalScore = normalized(avgSignalCount, lower = 2.0, upper = 4.0)
    math.round(25.0 * (0.5 * commentaryScore + 0.35 * signalScore + 0.15 * coverageScore)).toInt.clamp(0, 25)

  private def computeDiversityBonus(
      openingFrequency: Int,
      familyFrequency: Int,
      sideFrequency: Int,
      poolSize: Int
  ): Int =
    val openingRarity = 1.0 - normalized(openingFrequency.toDouble, lower = 1.0, upper = 15.0)
    val familyRarity = 1.0 - normalized(familyFrequency.toDouble, lower = 1.0, upper = 150.0)
    val sideRatio = if poolSize <= 0 then 0.5 else sideFrequency.toDouble / poolSize.toDouble
    val sideBalance = 1.0 - ((sideRatio - 0.5).abs / 0.5).min(1.0)
    math.round(15.0 * (0.45 * openingRarity + 0.40 * familyRarity + 0.15 * sideBalance)).toInt.clamp(0, 15)

  private def signalCategoryCount(
      response: CommentResponse,
      digest: Option[NarrativeSignalDigest]
  ): Int =
    List(
      digest.exists(d =>
        d.deploymentRoute.nonEmpty ||
          d.deploymentPurpose.exists(_.trim.nonEmpty) ||
          d.deploymentContribution.exists(_.trim.nonEmpty) ||
          response.strategyPack.exists(_.pieceRoutes.nonEmpty)
      ),
      digest.exists(d =>
        d.dominantIdeaKind.exists(_.trim.nonEmpty) ||
          d.dominantIdeaFocus.exists(_.trim.nonEmpty) ||
          d.strategicFlow.exists(_.trim.nonEmpty) ||
          response.mainStrategicPlans.nonEmpty ||
          response.strategyPack.exists(p => p.strategicIdeas.nonEmpty || p.plans.nonEmpty)
      ),
      digest.exists(d =>
        d.opponentPlan.exists(_.trim.nonEmpty) ||
          d.prophylaxisThreat.exists(_.trim.nonEmpty) ||
          d.counterplayScoreDrop.isDefined
      ),
      digest.exists(d =>
        d.practicalVerdict.exists(_.trim.nonEmpty) ||
          d.practicalFactors.nonEmpty ||
          d.compensation.exists(_.trim.nonEmpty) ||
          d.compensationVectors.nonEmpty ||
          d.preservedSignals.nonEmpty
      )
    ).count(identity)

  private def signalRichness(response: CommentResponse): Int =
    val digest = response.signalDigest.orElse(response.strategyPack.flatMap(_.signalDigest))
    val digestFields =
      List(
        digest.flatMap(_.dominantIdeaKind),
        digest.flatMap(_.dominantIdeaFocus),
        digest.flatMap(_.deploymentPurpose),
        digest.flatMap(_.deploymentContribution),
        digest.flatMap(_.opponentPlan),
        digest.flatMap(_.practicalVerdict),
        digest.flatMap(_.compensation)
      ).count(_.exists(_.trim.nonEmpty)) +
        digest.map(_.deploymentRoute.size).getOrElse(0) +
        digest.map(_.practicalFactors.size).getOrElse(0) +
        digest.map(_.compensationVectors.size).getOrElse(0)
    response.strategyPack.map { pack =>
      pack.strategicIdeas.size + pack.pieceRoutes.size + pack.directionalTargets.size + pack.plans.size + digestFields
    }.getOrElse(digestFields + response.mainStrategicPlans.size)

  private def applySingleMove(fen: String, uci: String): Option[String] =
    Fen.read(Standard, Fen.Full(fen)).flatMap { pos =>
      Uci(uci)
        .collect { case move: Uci.Move => move }
        .flatMap(pos.move(_).toOption)
        .map(move => Fen.write(move.after).value)
    }

  private def sanForMove(fen: String, uci: String): String =
    NarrativeUtils.uciListToSan(fen, List(uci)).headOption.getOrElse(uci)

  private def nonPawnMaterial(board: Board, color: Color): Int =
    board.byPiece(color, Knight).count * 3 +
      board.byPiece(color, Bishop).count * 3 +
      board.byPiece(color, Rook).count * 5 +
      board.byPiece(color, Queen).count * 9

  private def minorMajorPieces(board: Board, color: Color): Int =
    board.byPiece(color, Knight).count +
      board.byPiece(color, Bishop).count +
      board.byPiece(color, Rook).count +
      board.byPiece(color, Queen).count

  private def normalized(value: Double, lower: Double, upper: Double): Double =
    if upper <= lower then 1.0
    else ((value - lower) / (upper - lower)).max(0.0).min(1.0)

  private def mateScoreToCp(mate: Int): Int =
    if mate > 0 then 100000 - mate else -100000 - mate

  private def average(values: Iterable[Double]): Double =
    if values.isEmpty then 0.0 else values.sum / values.size.toDouble

  private def ensureParent(path: Path): Unit =
    Option(path.getParent).foreach(parent => Files.createDirectories(parent))

  private def sha256Hex(text: String): String =
    val digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))
    digest.map("%02x".format(_)).mkString

  private def clean(value: Option[String]): Option[String] =
    value.map(_.trim).filter(_.nonEmpty)

  private def nonBlank(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.nonEmpty)

  extension (i: Int)
    private def clamp(minValue: Int, maxValue: Int): Int = i.max(minValue).min(maxValue)
