package chess
package analysis

import chess.opening.Opening

object AnalysisModel:

  /** 엔진/멀티PV 설정. 환경변수로 덮어쓰기 가능. */
  final case class EngineConfig(
      shallowDepth: Int = 8,
      deepDepth: Int = 12,
      shallowTimeMs: Int = 200,
      deepTimeMs: Int = 500,
      maxMultiPv: Int = 3,
      extraDepthDelta: Int = 4,
      extraTimeMs: Int = 300
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
        sys.env.get(key).flatMap(_.toIntOption).filter(_ > 0).getOrElse(default)
      EngineConfig(
        shallowDepth = intEnv("ANALYZE_SHALLOW_DEPTH", 8),
        deepDepth = intEnv("ANALYZE_DEEP_DEPTH", 12),
        shallowTimeMs = intEnv("ANALYZE_SHALLOW_MS", 200),
        deepTimeMs = intEnv("ANALYZE_DEEP_MS", 500),
        maxMultiPv = intEnv("ANALYZE_MAX_MULTIPV", 3),
        extraDepthDelta = intEnv("ANALYZE_EXTRA_DEPTH_DELTA", 4),
        extraTimeMs = intEnv("ANALYZE_EXTRA_MS", 300)
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
      shortComment: Option[String] = None,
      studyTags: List[String] = Nil,
      studyScore: Double = 0.0,
      practicality: Option[PracticalityScorer.Score] = None
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
      accuracyBlack: Option[Double] = None
  )

  final case class Branch(move: String, winPct: Double, pv: List[String], label: String)
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
      features: Option[FeatureExtractor.SideFeatures] = None
  )
  final case class StudyLine(label: String, pv: List[String], winPct: Double)
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
