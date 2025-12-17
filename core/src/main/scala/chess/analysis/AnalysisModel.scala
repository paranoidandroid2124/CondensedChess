package chess
package analysis

import chess.opening.Opening
import chess.analysis.ConceptLabeler.ConceptLabels
import chess.analysis.FeatureExtractor.PositionFeatures

object AnalysisModel:
  
  // Phase 17: New Tags
  enum EvaluationTag:
    case Unknown
    case GeometryGood, GeometryBad
    case SpaceGood, SpaceBad
    case EndgameTechniqueGood, EndgameTechniqueBad
    case WrongBishopDraw

  /** 엔진/멀티PV 설정. 환경변수로 덮어쓰기 가능. */
  final case class EngineConfig(
      shallowDepth: Int = 6,
      deepDepth: Int = 14,
      shallowTimeMs: Int = 200,
      deepTimeMs: Int = 500,
      maxMultiPv: Int = 3,
      extraDepthDelta: Int = 4,
      extraTimeMs: Int = 300,
      experimentCapPerPly: Int = 5,
      maxRetries: Int = 2
  )

  final case class TimeControl(
      initialSeconds: Int,
      incrementSeconds: Int
  ) {
    def isBlitz: Boolean = initialSeconds <= 180
    def isRapid: Boolean = initialSeconds > 180 && initialSeconds <= 600
    def isClassical: Boolean = initialSeconds > 600
  }

  final case class PlayerContext(
      whiteElo: Option[Int],
      blackElo: Option[Int],
      timeControl: Option[TimeControl]
  )

  object EngineConfig:
    def fromEnv(): EngineConfig =
      def intEnv(key: String, default: Int) =
        EnvLoader.get(key).flatMap(_.toIntOption).filter(_ > 0).getOrElse(default)
      EngineConfig(
        shallowDepth = intEnv("ANALYZE_SHALLOW_DEPTH", 6), // Keep shallow to represent "intuition"
        deepDepth = intEnv("ANALYZE_DEEP_DEPTH", 18),      // Increase deep to find hidden tactics
        shallowTimeMs = intEnv("ANALYZE_SHALLOW_MS", 100), // Fast check (glance)
        deepTimeMs = intEnv("ANALYZE_DEEP_MS", 2500),      // Give enough time to reach high depth
        maxMultiPv = intEnv("ANALYZE_MAX_MULTIPV", 3),
        extraDepthDelta = intEnv("ANALYZE_EXTRA_DEPTH_DELTA", 4),
        extraTimeMs = intEnv("ANALYZE_EXTRA_MS", 300),
        experimentCapPerPly = intEnv("ANALYZE_EXPERIMENT_CAP", 5),
        maxRetries = intEnv("ANALYZE_MAX_RETRIES", 2)
      )

  final case class EngineLine(move: String, winPct: Double, cp: Option[Int], mate: Option[Int], pv: List[String])
  final case class EngineEval(depth: Int, lines: List[EngineLine])
  final case class Concepts(
      dynamic: Double,
      drawish: Double,
      imbalanced: Double,
      tacticalDepth: Double,
      blunderRisk: Double,
      pawnStorm: Double,
      fortress: Double,
      colorComplex: Double,
      badBishop: Double,
      goodKnight: Double,
      rookActivity: Double,
      kingSafety: Double,
      dry: Double,
      comfortable: Double,
      unpleasant: Double,
      engineLike: Double,
      conversionDifficulty: Double,
      sacrificeQuality: Double,
      alphaZeroStyle: Double
  )

  final case class PlyOutput(
      ply: Ply,
      turn: Color,
      san: String,
      uci: String,
      fen: String,
      fenBefore: String,
      legalMoves: Int,
      features: FeatureExtractor.SideFeatures,
      evalBeforeShallow: EngineEval,
      evalBeforeDeep: EngineEval,
      winPctBefore: Double,
      winPctAfterForPlayer: Double,
      deltaWinPct: Double,
      epBefore: Double,
      epAfter: Double,
      epLoss: Double,
      judgement: String,
      special: Option[String],
      conceptsBefore: Concepts,
      concepts: Concepts,
      conceptDelta: Concepts,
      bestVsSecondGap: Option[Double],
      bestVsPlayedGap: Option[Double],
      semanticTags: List[String],
      mistakeCategory: Option[String],
      phaseLabel: Option[String] = None,
      phase: String = "middlegame",
      shortComment: Option[String] = None,
      studyTags: List[String] = Nil,
      studyScore: Double = 0.0,
      practicality: Option[PracticalityScorer.Score] = None,
      materialDiff: Double = 0.0,
      bestMaterialDiff: Option[Double] = None,
      tacticalMotif: Option[String] = None,
      roles: List[String] = Nil,
      conceptLabels: Option[ConceptLabels] = None,
      fullFeatures: Option[PositionFeatures] = None,
      playedEvalCp: Option[Int] = None,
      hypotheses: List[Branch] = Nil
  )

  final case class Output(
      opening: Option[Opening.AtPly],
      openingStats: Option[OpeningExplorer.Stats],
      timeline: Vector[PlyOutput],
      oppositeColorBishops: Boolean,
      critical: Vector[CriticalNode],
      openingSummary: Option[String] = None,
      bookExitComment: Option[String] = None,
      openingTrend: Option[String] = None,
      summaryText: Option[String] = None,
      root: Option[TreeNode] = None,
      studyChapters: Vector[StudyChapter] = Vector.empty,
      pgn: String = "",
      accuracyWhite: Option[Double] = None,
      accuracyBlack: Option[Double] = None,
      book: Option[BookModel.Book] = None  // Phase 4.6 Book JSON
  )

  final case class Branch(move: String, winPct: Double, pv: List[String], label: String, comment: Option[String] = None, cp: Option[Int] = None, mate: Option[Int] = None)
  final case class CriticalNode(
      ply: Ply,
      reason: String,
      deltaWinPct: Double,
      branches: List[Branch],
      bestVsSecondGap: Option[Double] = None,
      bestVsPlayedGap: Option[Double] = None,
      forced: Boolean = false,
      legalMoves: Option[Int] = None,
      comment: Option[String] = None,
      mistakeCategory: Option[String] = None,
      tags: List[String] = Nil,
      practicality: Option[PracticalityScorer.Score] = None,
      opponentRobustness: Option[Double] = None,
      isPressurePoint: Boolean = false
  )
  final case class TreeNode(
      ply: Int,
      san: String,
      uci: String,
      fen: String,
      eval: Double,
      evalType: String,
      judgement: String,
      glyph: String,
      tags: List[String],
      bestMove: Option[String],
      bestEval: Option[Double],
      pv: List[String],
      comment: Option[String],
      children: List[TreeNode],
      nodeType: String = "mainline", // "mainline", "critical", "sideline"
      concepts: Option[Concepts] = None,
      features: Option[FeatureExtractor.SideFeatures] = None,
      practicality: Option[PracticalityScorer.Score] = None
  )
  final case class StudyLine(label: String, pv: List[String], winPct: Double, cp: Option[Int] = None, mate: Option[Int] = None)
  final case class StudyChapter(
      id: String,
      anchorPly: Int,
      fen: String,
      played: String,
      best: Option[String],
      deltaWinPct: Double,
      tags: List[String],
      lines: List[StudyLine],
      summary: Option[String] = None,
      studyScore: Double = 0.0,
      phase: String = "middlegame",
      winPctBefore: Double = 50.0,
      winPctAfter: Double = 50.0,
      practicality: Option[PracticalityScorer.Score] = None,
      metadata: Option[StudyChapterMetadata] = None,
      rootNode: Option[TreeNode] = None
  )

  final case class StudyChapterMetadata(
      name: String,
      description: String
  )
