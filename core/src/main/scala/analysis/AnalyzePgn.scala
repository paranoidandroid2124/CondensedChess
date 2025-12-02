package chess
package analysis

import chess.format.Fen
import chess.format.pgn.PgnStr
import chess.opening.{ Opening, OpeningDb }

import scala.io.Source

/** 간단한 PGN→타임라인 CLI.
  *
  * 사용법: sbt "core/runMain chess.analysis.AnalyzePgn path/to/game.pgn"
  * 출력: JSON 한 줄 (timeline + opening + per-ply feature 스냅샷)
  */
object AnalyzePgn:

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
      shortComment: Option[String] = None
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
      root: Option[TreeNode] = None
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
      tags: List[String] = Nil
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
      children: List[TreeNode]
  )

  private def clamp01(d: Double): Double = math.max(0.0, math.min(1.0, d))
  private def pctInt(d: Double): Int = math.round(if d > 1.0 then d else d * 100.0).toInt
  private def moveLabel(ply: Ply, san: String, turn: Color): String =
    val moveNumber = (ply.value + 1) / 2
    val sep = if turn == Color.White then "." else "..."
    s"$moveNumber$sep $san"

  private def buildOpeningNotes(
      opening: Option[Opening.AtPly],
      openingStats: Option[OpeningExplorer.Stats],
      timeline: Vector[PlyOutput]
  ): (Option[String], Option[String], Option[String]) =
    val openingName = opening.map(_.opening.name.value).getOrElse("Opening")
    val summary = openingStats.map { os =>
      val topMove = os.topMoves.headOption
        .map { tm =>
          val win = tm.winPct.map(pctInt).map(w => s", win $w%").getOrElse("")
          s"top move ${tm.san} (${tm.games} games$win)"
        }
        .getOrElse("no top move data")
      val gamesStr = os.games.map(g => s"$g games").getOrElse("few games")
      val source = s"source=${os.source}"
      s"$openingName book to ply ${os.bookPly} (novelty ${os.noveltyPly}, $gamesStr, $topMove, $source)"
    }

    val exitPly = openingStats.map(_.bookPly + 1).orElse(opening.map(op => op.ply.value + 1))
    val bookExit = for
      ep <- exitPly
      move <- timeline.find(_.ply.value == ep)
    yield
      val label = moveLabel(move.ply, move.san, move.turn)
      val topAlt = openingStats.flatMap(_.topMoves.headOption).map { tm =>
        val win = tm.winPct.map(pctInt).map(w => s", win $w%").getOrElse("")
        s"${tm.san} (${tm.games} games$win)"
      }
      val notableGame = openingStats.flatMap(_.topGames.headOption).map { g =>
        val date = g.date.map(d => s" $d").getOrElse("")
        s"${g.white}-${g.black}$date"
      }
      val topMoveStr = topAlt.map(m => s"book line: $m").getOrElse("book line unknown")
      val notableStr = notableGame.map(g => s"; notable game $g").getOrElse("")
      s"Left book at $label playing ${move.san}; $topMoveStr$notableStr."

    val trend = openingStats.flatMap { os =>
      val buckets = os.yearBuckets
      val recent = buckets.getOrElse("2020_plus", 0) + buckets.getOrElse("2018_2019", 0)
      val prior = buckets.getOrElse("pre2012", 0) + buckets.getOrElse("2012_2017", 0)
      val span = (os.minYear, os.maxYear) match
        case (Some(minY), Some(maxY)) if minY > 0 && maxY > 0 => s" (${minY}-${maxY})"
        case _ => ""
      if recent >= 10 && prior > 0 && recent.toDouble / prior >= 1.5 then
        Some(s"Line popularity rising post-2018: recent ${recent} vs prior ${prior} games$span.")
      else if recent >= 5 && prior > 0 && recent.toDouble / prior <= 0.6 then
        Some(s"Line less common after 2018: recent ${recent} vs prior ${prior} games$span.")
      else if recent + prior >= 8 then
        Some(s"Line stable across eras: recent ${recent}, prior ${prior}$span.")
      else None
    }

    (summary, bookExit, trend)

  private val pieceValues: Map[Role, Double] = Map(
    Pawn   -> 1.0,
    Knight -> 3.0,
    Bishop -> 3.0,
    Rook   -> 5.0,
    Queen  -> 9.0,
    King   -> 0.0
  )

  private def material(board: Board, color: Color): Double =
    board.piecesOf(color).toList.map { case (_, piece) => pieceValues.getOrElse(piece.role, 0.0) }.sum

  def main(args: Array[String]): Unit =
    if args.length != 1 then
      System.err.println("usage: AnalyzePgn <path/to/game.pgn>")
      sys.exit(1)
    val path = args(0)
    val pgn = Source.fromFile(path).mkString
    analyzeToJson(pgn, EngineConfig.fromEnv()) match
      case Left(err) =>
        System.err.println(err)
        sys.exit(1)
      case Right(json) =>
        println(json)

  /** PGN 문자열을 받아 Review JSON을 반환. 실패 시 에러 메시지 리턴. */
  def analyzeToJson(pgn: String, config: EngineConfig = EngineConfig.fromEnv(), llmRequestedPlys: Set[Int] = Set.empty): Either[String, String] =
    analyze(pgn, config, llmRequestedPlys).map(render)

  /** PGN 문자열을 받아 도메인 Output을 반환. */
  def analyze(pgn: String, config: EngineConfig = EngineConfig.fromEnv(), llmRequestedPlys: Set[Int] = Set.empty): Either[String, Output] =
    Replay.mainline(PgnStr(pgn)).flatMap(_.valid) match
      case Left(err) => Left(s"PGN 파싱 실패: ${err.value}")
      case Right(replay) =>
        val sans = replay.chronoMoves.map(_.toSanStr)
        val opening = OpeningDb.search(sans)
        val openingStats = OpeningExplorer.explore(opening, replay.chronoMoves.map(_.toSanStr).toList)
        val client = new StockfishClient()
        val (timeline, finalGame) = buildTimeline(replay, client, config, opening)
        val critical = detectCritical(timeline, client, config, llmRequestedPlys)
        val oppositeColorBishops = FeatureExtractor.hasOppositeColorBishops(finalGame.position.board)
        val root = Some(buildTree(timeline, critical))
        val (openingSummary, bookExitComment, openingTrend) = buildOpeningNotes(opening, openingStats, timeline)
        Right(Output(opening, openingStats, timeline, oppositeColorBishops, critical, openingSummary, bookExitComment, openingTrend, root = root))

  private def buildTimeline(replay: Replay, client: StockfishClient, config: EngineConfig, opening: Option[Opening.AtPly]): (Vector[PlyOutput], Game) =
    var game = replay.setup
    val entries = Vector.newBuilder[PlyOutput]
    var prevDeltaForOpp: Double = 0.0
    replay.chronoMoves.foreach { mod =>
      val player = game.position.color
      val legalCount = game.position.legalMoves.size
      val multiPv = math.min(config.maxMultiPv, math.max(1, legalCount))
      val fenBefore = Fen.write(game.position, game.ply.fullMoveNumber).value
      val evalBeforeShallow = evalFen(client, fenBefore, config.shallowDepth, multiPv, Some(config.shallowTimeMs))
      val evalBeforeDeep = evalFen(client, fenBefore, config.deepDepth, multiPv, Some(config.deepTimeMs))
      val winBefore = evalBeforeDeep.lines.headOption.map(_.winPct).getOrElse(50.0)

      val nextGame = mod.applyGame(game)
      val fenAfter = Fen.write(nextGame.position, nextGame.ply.fullMoveNumber).value
      val statusAfter = nextGame.position.status
      val winnerAfter = nextGame.position.winner
      val (winAfterForPlayer, _) =
        statusAfter match
          case Some(Status.Mate) =>
            val sideToMoveWin = if winnerAfter.contains(nextGame.position.color) then 100.0 else 0.0
            val moverWin = if winnerAfter.contains(player) then 100.0 else 0.0
            (moverWin, sideToMoveWin)
          case Some(Status.Stalemate | Status.Draw | Status.VariantEnd) =>
            (50.0, 50.0)
          case _ =>
            val evalAfter = evalFen(client, fenAfter, config.deepDepth, 1, Some(config.deepTimeMs))
            val side = evalAfter.lines.headOption.map(_.winPct).getOrElse(50.0)
            val mover = if player == nextGame.position.color then side else 100.0 - side
            (mover, side)
      val delta = winAfterForPlayer - winBefore
      val epBefore = clamp01(winBefore / 100.0)
      val epAfter = clamp01(winAfterForPlayer / 100.0)
      val epLoss = math.max(0.0, epBefore - epAfter)

      val baseJudgement =
        statusAfter match
          case Some(Status.Mate) => "mate"
          case Some(Status.Stalemate | Status.Draw | Status.VariantEnd) => "draw"
          case _ =>
            if epLoss <= 0.0 then "best"
            else if epLoss <= 0.05 then "good" // excellent 통합
            else if epLoss <= 0.10 then "inaccuracy"
            else if epLoss <= 0.20 then "mistake"
            else "blunder"

      // 희생 감지: 내 기물 가치가 줄었는지
      val materialBefore = material(game.position.board, player)
      val materialAfter = material(nextGame.position.board, player)
      val sacrificed = materialAfter < materialBefore - 1.5 // 말 하나 이상 희생

      val specialBrilliant =
        sacrificed &&
          (baseJudgement == "best" || baseJudgement == "excellent") &&
          epLoss <= 0.02 &&
          epBefore <= 0.7 && epBefore >= 0.3

      val greatMove =
        !specialBrilliant && (baseJudgement == "best" || baseJudgement == "excellent" || baseJudgement == "good") && (
          (epBefore < 0.35 && epAfter >= 0.55) || // 패배권 → 승리권
            (epAfter - epBefore) >= 0.2 // 큰 전환
        )

      val miss =
        (baseJudgement == "inaccuracy" || baseJudgement == "mistake" || baseJudgement == "blunder") &&
          prevDeltaForOpp <= -20 // 직전에 상대가 큰 실수(내 입장에서는 큰 득점)를 했는데 놓침

      val special =
        if specialBrilliant then Some("brilliant")
        else if greatMove then Some("great")
        else if miss then Some("miss")
        else None

      val inBook = opening.exists(op => nextGame.ply.value <= op.ply.value)
      val finalJudgement =
        if inBook then "book"
        else baseJudgement

      val moverFeaturesBefore = FeatureExtractor.sideFeatures(game.position, player)
      val oppFeaturesBefore = FeatureExtractor.sideFeatures(game.position, !player)
      val moverFeatures = FeatureExtractor.sideFeatures(nextGame.position, player)
      val oppFeatures = FeatureExtractor.sideFeatures(nextGame.position, !player)
      ConceptScorer.computeBreakAndInfiltration(nextGame.position, nextGame.position.color)
      val conceptScoresBefore = ConceptScorer.score(
        features = moverFeaturesBefore,
        oppFeatures = oppFeaturesBefore,
        evalShallowWin = evalBeforeShallow.lines.headOption.map(_.winPct).getOrElse(winBefore),
        evalDeepWin = evalBeforeDeep.lines.headOption.map(_.winPct).getOrElse(winBefore),
        multiPvWin = evalBeforeDeep.lines.map(_.winPct),
        position = game.position,
        sideToMove = game.position.color
      )
      val conceptScores = ConceptScorer.score(
        features = moverFeatures,
        oppFeatures = oppFeatures,
        evalShallowWin = evalBeforeShallow.lines.headOption.map(_.winPct).getOrElse(winBefore),
        evalDeepWin = evalBeforeDeep.lines.headOption.map(_.winPct).getOrElse(winBefore),
        multiPvWin = evalBeforeDeep.lines.map(_.winPct),
        position = nextGame.position,
        sideToMove = nextGame.position.color
      )
      val concepts = Concepts(
        dynamic = conceptScores.dynamic,
        drawish = conceptScores.drawish,
        imbalanced = conceptScores.imbalanced,
        tacticalDepth = conceptScores.tacticalDepth,
        blunderRisk = conceptScores.blunderRisk,
        pawnStorm = conceptScores.pawnStorm,
        fortress = conceptScores.fortress,
        colorComplex = conceptScores.colorComplex,
        badBishop = conceptScores.badBishop,
        goodKnight = conceptScores.goodKnight,
        rookActivity = conceptScores.rookActivity,
        kingSafety = conceptScores.kingSafety,
        dry = conceptScores.dry,
        comfortable = conceptScores.comfortable,
        unpleasant = conceptScores.unpleasant,
        engineLike = conceptScores.engineLike,
        conversionDifficulty = conceptScores.conversionDifficulty,
        sacrificeQuality = conceptScores.sacrificeQuality,
        alphaZeroStyle = conceptScores.alphaZeroStyle
      )
      val conceptsBefore = Concepts(
        dynamic = conceptScoresBefore.dynamic,
        drawish = conceptScoresBefore.drawish,
        imbalanced = conceptScoresBefore.imbalanced,
        tacticalDepth = conceptScoresBefore.tacticalDepth,
        blunderRisk = conceptScoresBefore.blunderRisk,
        pawnStorm = conceptScoresBefore.pawnStorm,
        fortress = conceptScoresBefore.fortress,
        colorComplex = conceptScoresBefore.colorComplex,
        badBishop = conceptScoresBefore.badBishop,
        goodKnight = conceptScoresBefore.goodKnight,
        rookActivity = conceptScoresBefore.rookActivity,
        kingSafety = conceptScoresBefore.kingSafety,
        dry = conceptScoresBefore.dry,
        comfortable = conceptScoresBefore.comfortable,
        unpleasant = conceptScoresBefore.unpleasant,
        engineLike = conceptScoresBefore.engineLike,
        conversionDifficulty = conceptScoresBefore.conversionDifficulty,
        sacrificeQuality = conceptScoresBefore.sacrificeQuality,
        alphaZeroStyle = conceptScoresBefore.alphaZeroStyle
      )
      val conceptDelta = Concepts(
        dynamic = concepts.dynamic - conceptsBefore.dynamic,
        drawish = concepts.drawish - conceptsBefore.drawish,
        imbalanced = concepts.imbalanced - conceptsBefore.imbalanced,
        tacticalDepth = concepts.tacticalDepth - conceptsBefore.tacticalDepth,
        blunderRisk = concepts.blunderRisk - conceptsBefore.blunderRisk,
        pawnStorm = concepts.pawnStorm - conceptsBefore.pawnStorm,
        fortress = concepts.fortress - conceptsBefore.fortress,
        colorComplex = concepts.colorComplex - conceptsBefore.colorComplex,
        badBishop = concepts.badBishop - conceptsBefore.badBishop,
        goodKnight = concepts.goodKnight - conceptsBefore.goodKnight,
        rookActivity = concepts.rookActivity - conceptsBefore.rookActivity,
        kingSafety = concepts.kingSafety - conceptsBefore.kingSafety,
        dry = concepts.dry - conceptsBefore.dry,
        comfortable = concepts.comfortable - conceptsBefore.comfortable,
        unpleasant = concepts.unpleasant - conceptsBefore.unpleasant,
        engineLike = concepts.engineLike - conceptsBefore.engineLike,
        conversionDifficulty = concepts.conversionDifficulty - conceptsBefore.conversionDifficulty,
        sacrificeQuality = concepts.sacrificeQuality - conceptsBefore.sacrificeQuality,
        alphaZeroStyle = concepts.alphaZeroStyle - conceptsBefore.alphaZeroStyle
      )
      val (phaseScore, phaseLabelRaw) = phaseTransition(conceptDelta, winBefore)
      val phaseLabel = phaseLabelRaw.filter(_ => phaseScore >= 8.0) // 노이즈 방지 임계값
      val bestVsSecondGap = (for
        top <- evalBeforeDeep.lines.headOption
        second <- evalBeforeDeep.lines.drop(1).headOption
      yield (top.winPct - second.winPct).abs)
      val bestVsPlayedGap = evalBeforeDeep.lines.headOption.map(_.winPct - winAfterForPlayer)

      val semanticTags =
        SemanticTagger.tags(
          position = nextGame.position,
          perspective = player,
          ply = nextGame.ply,
          self = moverFeatures,
          opp = oppFeatures
        )

      val mistakeCategory =
        MistakeClassifier.classify(
          MistakeClassifier.Input(
            ply = nextGame.ply,
            judgement = finalJudgement,
            deltaWinPct = delta,
            san = mod.toSanStr.value,
            player = player,
            inCheckBefore = game.position.check.yes,
            concepts = concepts,
            features = moverFeatures,
            oppFeatures = oppFeatures
          )
        )
      entries += PlyOutput(
        ply = nextGame.ply,
        turn = player, // 수를 둔 색
        san = mod.toSanStr.value,
        uci = mod.toUci.uci,
        fen = fenAfter,
        fenBefore = fenBefore,
        legalMoves = legalCount,
        features = moverFeatures,
        evalBeforeShallow = evalBeforeShallow,
        evalBeforeDeep = evalBeforeDeep,
        winPctBefore = winBefore,
        winPctAfterForPlayer = winAfterForPlayer,
        deltaWinPct = delta,
        epBefore = epBefore,
        epAfter = epAfter,
        epLoss = epLoss,
        judgement = finalJudgement,
        special = special,
        conceptsBefore = conceptsBefore,
        concepts = concepts,
        conceptDelta = conceptDelta,
        bestVsSecondGap = bestVsSecondGap,
        bestVsPlayedGap = bestVsPlayedGap,
        semanticTags = semanticTags,
        mistakeCategory = mistakeCategory,
        phaseLabel = phaseLabel
      )
      game = nextGame
      prevDeltaForOpp = delta // 다음 수에서 miss 판단용
    }
    entries.result() -> game

  private final case class EvalCacheKey(fen: String, depth: Int, multiPv: Int, timeMs: Int)

  private def detectPhaseLabel(delta: Concepts, winPctBefore: Double): Option[String] =
    if delta.dynamic <= -0.3 && delta.drawish >= 0.3 then Some("transition_to_endgame")
    else if delta.tacticalDepth <= -0.25 && delta.dry >= 0.25 then Some("tactical_to_positional")
    else if delta.fortress >= 0.4 then Some("fortress_formation")
    else if delta.comfortable <= -0.3 && delta.unpleasant >= 0.3 then Some("comfort_to_unpleasant")
    else if delta.alphaZeroStyle >= 0.35 then Some("positional_sacrifice")
    else if delta.kingSafety >= 0.4 then Some("king_safety_crisis")
    else if delta.conversionDifficulty >= 0.35 && winPctBefore >= 60 then Some("conversion_difficulty")
    else None

  private def phaseTransition(delta: Concepts, winPctBefore: Double): (Double, Option[String]) =
    val dynamicCollapse = if delta.dynamic <= -0.3 && delta.drawish >= 0.3 then 15.0 else 0.0
    val tacticalDry = if delta.tacticalDepth <= -0.25 && delta.dry >= 0.25 then 12.0 else 0.0
    val fortressJump = if delta.fortress >= 0.4 then 10.0 else 0.0
    val comfortLoss = if delta.comfortable <= -0.3 && delta.unpleasant >= 0.3 then 10.0 else 0.0
    val alphaZeroSpike = if delta.alphaZeroStyle >= 0.35 then 8.0 else 0.0
    val kingSafetyCollapse = if delta.kingSafety >= 0.4 then 12.0 else 0.0
    val conversionIssue = if delta.conversionDifficulty >= 0.35 && winPctBefore >= 60 then 8.0 else 0.0
    val phaseScore = List(dynamicCollapse, tacticalDry, fortressJump, comfortLoss, alphaZeroSpike, kingSafetyCollapse, conversionIssue).max
    (phaseScore, detectPhaseLabel(delta, winPctBefore))

  private def detectCritical(timeline: Vector[PlyOutput], client: StockfishClient, config: EngineConfig, llmRequestedPlys: Set[Int]): Vector[CriticalNode] =
    val evalCache = scala.collection.mutable.Map.empty[EvalCacheKey, EngineEval]
    val judgementBoost: Map[String, Double] = Map(
      "blunder" -> 15.0,
      "mistake" -> 8.0,
      "inaccuracy" -> 4.0,
      "good" -> 0.0,
      "best" -> 0.0,
      "book" -> 0.0
    )

    def criticalityScore(p: PlyOutput): (Double, Double, Option[String]) =
      val deltaScore = math.abs(p.deltaWinPct)
      val conceptJump = p.concepts.dynamic + p.concepts.tacticalDepth + p.concepts.blunderRisk
      val bestVsSecondGap = p.bestVsSecondGap.getOrElse {
        val top = p.evalBeforeDeep.lines.headOption
        val second = p.evalBeforeDeep.lines.drop(1).headOption
        (for t <- top; s <- second yield (t.winPct - s.winPct).abs).getOrElse(100.0)
      }
      val branchTension = math.max(0.0, 12.0 - bestVsSecondGap) // 가까울수록(갈림길) 점수 증가, 12% 이상은 0
      val (phaseShift, phaseLabel) = phaseTransition(p.conceptDelta, p.winPctBefore)
      val score =
        deltaScore * 0.6 +
          branchTension * 0.8 +
          judgementBoost.getOrElse(p.judgement, 0.0) +
          conceptJump * 20.0 +
          phaseShift * 1.5
      (score, bestVsSecondGap, phaseLabel)

    val scored = timeline.map { p =>
      val (s, gap, phaseLabel) = criticalityScore(p)
      val llmBoost = if llmRequestedPlys.contains(p.ply.value) then 50.0 else 0.0
      (p, (s + llmBoost, gap, phaseLabel))
    }
    val cap = math.min(8, math.max(3, 3 + timeline.size / 20)) // 게임 길이에 따라 3~8 사이 자동 조정

    def extraEval(fen: String, multiPv: Int): EngineEval =
      val depth = config.deepDepth + config.extraDepthDelta
      val time = config.deepTimeMs + config.extraTimeMs
      val key = EvalCacheKey(fen, depth, multiPv, time)
      evalCache.getOrElseUpdate(key, evalFen(client, fen, depth = depth, multiPv = multiPv, moveTimeMs = Some(time)))

    scored.sortBy { case (_, (s, _, _)) => -s }.take(cap).map { case (ply, (_, gap, phaseLabel)) =>
      val reason =
        if ply.judgement == "blunder" then "ΔWin% blunder spike"
        else if ply.judgement == "mistake" then "ΔWin% mistake"
        else if phaseLabel.nonEmpty then s"phase shift: ${phaseLabel.get}"
        else if gap <= 5.0 then "critical branching (lines close)"
        else if ply.deltaWinPct.abs >= 3 then "notable eval swing"
        else "concept shift"

      val baseLines = (ply.evalBeforeDeep.lines.headOption.toList ++ ply.evalBeforeDeep.lines.drop(1).take(2))
      val enriched =
        try
          val extra = extraEval(ply.fenBefore, math.min(3, config.maxMultiPv)).lines
          if extra.nonEmpty then extra.take(3) else baseLines
        catch
          case _: Throwable => baseLines

      val branches =
        enriched.zipWithIndex.map {
          case (l, idx) =>
            val label = idx match
              case 0 => "best (PV1)"
              case 1 => "alt (PV2)"
              case 2 => "alt (PV3)"
              case _ => "line"
            Branch(move = l.move, winPct = l.winPct, pv = l.pv, label = label)
        }

      val forcedMove = ply.legalMoves <= 1 || gap >= 20.0

      val tags = (phaseLabel.toList ++ ply.semanticTags).take(6)

      CriticalNode(
        ply = ply.ply,
        reason = reason,
        deltaWinPct = ply.deltaWinPct,
        branches = branches,
        bestVsSecondGap = Some(gap),
        bestVsPlayedGap = ply.bestVsPlayedGap,
        forced = forcedMove,
        legalMoves = Some(ply.legalMoves),
        mistakeCategory = ply.mistakeCategory,
        tags = tags
      )
    }

  private def buildTree(timeline: Vector[PlyOutput], critical: Vector[CriticalNode]): TreeNode =
    val criticalByPly = critical.map(c => c.ply.value -> c).toMap
    def glyphOf(j: String): String = j match
      case "blunder" => "??"
      case "mistake" => "?"
      case "inaccuracy" => "?!"
      case "good" | "best" => "!"
      case "book" => "="
      case _ => ""

    def tagsOf(p: PlyOutput): List[String] =
      (List(p.judgement) ++ p.special.toList ++ p.mistakeCategory.toList ++ p.semanticTags).filter(_.nonEmpty)

    def pvToSan(fen: String, pv: List[String], maxPlies: Int = 5): List[String] =
      val fullFen: chess.format.FullFen = chess.format.Fen.Full.clean(fen)
      val startGame: chess.Game = chess.Game(chess.variant.Standard, Some(fullFen))
      pv.take(maxPlies).foldLeft((List.empty[String], startGame)) {
        case ((sans, g), uciStr) =>
          chess.format.Uci(uciStr) match
            case Some(uci: chess.format.Uci.Move) =>
              g.apply(uci) match
                case Right((nextGame, _)) =>
                  val san = nextGame.sans.lastOption.map(_.value).getOrElse(uciStr)
                  (sans :+ san, nextGame)
                case _ => (sans, g)
            case _ => (sans, g)
      }._1
    def uciToSanSingle(fen: String, uciStr: String): String =
      pvToSan(fen, List(uciStr), maxPlies = 1).headOption.getOrElse(uciStr)

    val mainNodes = timeline.map { t =>
      val bestLine = t.evalBeforeDeep.lines.headOption
      val bestMove = bestLine.map(_.move)
      val bestEval = bestLine.map(_.winPct)
      val pvSan = bestLine.map(bl => pvToSan(t.fenBefore, bl.pv)).getOrElse(Nil)
      TreeNode(
        ply = t.ply.value,
        san = t.san,
        uci = t.uci,
        fen = t.fen,
        eval = t.winPctAfterForPlayer,
        evalType = "cp",
        judgement = t.judgement,
        glyph = glyphOf(t.judgement),
        tags = tagsOf(t),
        bestMove = bestMove,
        bestEval = bestEval,
        pv = pvSan,
        comment = t.shortComment,
        children = Nil
      )
    }

    def attachVariations(node: TreeNode, fenBefore: String): TreeNode =
      criticalByPly.get(node.ply) match
        case None => node
        case Some(c) =>
          val vars = c.branches.take(3).map { b =>
            val pvSan = pvToSan(fenBefore, b.pv)
            val sanMove = uciToSanSingle(fenBefore, b.move)
            TreeNode(
              ply = node.ply,
              san = sanMove,
              uci = b.move,
              fen = node.fen,
              eval = b.winPct,
              evalType = "cp",
              judgement = "variation",
              glyph = "",
              tags = List("variation", c.reason),
              bestMove = None,
              bestEval = None,
              pv = pvSan,
              comment = c.comment,
              children = Nil
            )
          }
          node.copy(children = vars)

    // build linear chain with attached variations
    val nodesWithVars = mainNodes.zip(timeline).map { case (n, t) => attachVariations(n, t.fenBefore) }
    val root = nodesWithVars.headOption.getOrElse(
      TreeNode(0, "start", "", "", 50.0, "cp", "book", "", Nil, None, None, Nil, None, Nil)
    )
    nodesWithVars.drop(1).foldLeft(root) { (acc, n) => acc.copy(children = acc.children :+ n) }

  private def evalFen(client: StockfishClient, fen: String, depth: Int, multiPv: Int, moveTimeMs: Option[Int]): EngineEval =
    client.evaluateFen(fen, depth = depth, multiPv = multiPv, moveTimeMs = moveTimeMs) match
      case Right(res) =>
        val lines = res.lines.map { l =>
          EngineLine(
            move = l.pv.headOption.getOrElse(""),
            winPct = l.winPercent,
            cp = l.cp,
            mate = l.mate,
            pv = l.pv
          )
        }
        EngineEval(depth, lines)
      case Left(err) =>
        System.err.println(s"[engine-error] $err")
        EngineEval(depth, Nil)

  def render(output: Output): String =
    val sb = new StringBuilder(256 + output.timeline.size * 128)
    sb.append('{')
    output.summaryText.foreach { s =>
      sb.append("\"summaryText\":\"").append(escape(s)).append("\",")
    }
    output.root.foreach { r =>
      sb.append("\"root\":")
      renderTree(sb, r)
      sb.append(',')
    }
    output.opening.foreach { op =>
      sb.append("\"opening\":{")
      sb.append("\"name\":\"").append(escape(op.opening.name.value)).append("\",")
      sb.append("\"eco\":\"").append(escape(op.opening.eco.value)).append("\",")
      sb.append("\"ply\":").append(op.ply.value)
      sb.append("},")
    }
    output.openingStats.foreach { os =>
      sb.append("\"openingStats\":")
      renderOpeningStats(sb, os)
      sb.append(',')
    }
    sb.append("\"oppositeColorBishops\":").append(output.oppositeColorBishops).append(',')
    output.openingSummary.foreach { s =>
      sb.append("\"openingSummary\":\"").append(escape(s)).append("\",")
    }
    output.bookExitComment.foreach { s =>
      sb.append("\"bookExitComment\":\"").append(escape(s)).append("\",")
    }
    output.openingTrend.foreach { s =>
      sb.append("\"openingTrend\":\"").append(escape(s)).append("\",")
    }
    sb.append("\"critical\":[")
    output.critical.zipWithIndex.foreach { case (c, idx) =>
      if idx > 0 then sb.append(',')
      renderCritical(sb, c)
    }
    sb.append("],")
    sb.append("\"timeline\":[")
    output.timeline.zipWithIndex.foreach { case (ply, idx) =>
      if idx > 0 then sb.append(',')
      sb.append('{')
      sb.append("\"ply\":").append(ply.ply.value).append(',')
      sb.append("\"turn\":\"").append(ply.turn.name).append("\",")
      sb.append("\"san\":\"").append(escape(ply.san)).append("\",")
      sb.append("\"uci\":\"").append(escape(ply.uci)).append("\",")
      sb.append("\"fen\":\"").append(escape(ply.fen)).append("\",")
      sb.append("\"fenBefore\":\"").append(escape(ply.fenBefore)).append("\",")
      sb.append("\"legalMoves\":").append(ply.legalMoves).append(',')
      renderFeatures(sb, ply.features)
      sb.append(',')
      renderEval(sb, "evalBeforeShallow", ply.evalBeforeShallow)
      sb.append(',')
      renderEval(sb, "evalBeforeDeep", ply.evalBeforeDeep)
      sb.append(',')
      sb.append("\"winPctBefore\":").append(fmt(ply.winPctBefore)).append(',')
      sb.append("\"winPctAfterForPlayer\":").append(fmt(ply.winPctAfterForPlayer)).append(',')
      sb.append("\"deltaWinPct\":").append(fmt(ply.deltaWinPct)).append(',')
      sb.append("\"epBefore\":").append(fmt(ply.epBefore)).append(',')
      sb.append("\"epAfter\":").append(fmt(ply.epAfter)).append(',')
      sb.append("\"epLoss\":").append(fmt(ply.epLoss)).append(',')
      sb.append("\"judgement\":\"").append(ply.judgement).append('"')
      ply.bestVsSecondGap.foreach { gap =>
        sb.append(",\"bestVsSecondGap\":").append(fmt(gap))
      }
      ply.bestVsPlayedGap.foreach { gap =>
        sb.append(",\"bestVsPlayedGap\":").append(fmt(gap))
      }
      ply.special.foreach { s =>
        sb.append(",\"special\":\"").append(escape(s)).append('"')
      }
      ply.mistakeCategory.foreach { cat =>
        sb.append(",\"mistakeCategory\":\"").append(escape(cat)).append('"')
      }
      ply.phaseLabel.foreach { phase =>
        sb.append(",\"phaseLabel\":\"").append(escape(phase)).append('"')
      }
      if ply.semanticTags.nonEmpty then
        sb.append(",\"semanticTags\":[")
        ply.semanticTags.zipWithIndex.foreach { case (t, i) =>
          if i > 0 then sb.append(',')
          sb.append("\"").append(escape(t)).append('"')
        }
        sb.append(']')
      ply.shortComment.foreach { txt =>
        sb.append(",\"shortComment\":\"").append(escape(txt)).append('"')
      }
      sb.append(',')
      renderConcepts(sb, "concepts", ply.concepts)
      sb.append(',')
      renderConcepts(sb, "conceptsBefore", ply.conceptsBefore)
      sb.append(',')
      renderConcepts(sb, "conceptDelta", ply.conceptDelta)
      sb.append('}')
    }
    sb.append("]}")
    sb.result()

  private def renderFeatures(sb: StringBuilder, f: FeatureExtractor.SideFeatures): Unit =
    sb.append("\"features\":{")
    sb.append("\"pawnIslands\":").append(f.pawnIslands).append(',')
    sb.append("\"isolatedPawns\":").append(f.isolatedPawns).append(',')
    sb.append("\"doubledPawns\":").append(f.doubledPawns).append(',')
    sb.append("\"passedPawns\":").append(f.passedPawns).append(',')
    sb.append("\"rookOpenFiles\":").append(f.rookOpenFiles).append(',')
    sb.append("\"rookSemiOpenFiles\":").append(f.rookSemiOpenFiles).append(',')
    sb.append("\"bishopPair\":").append(f.bishopPair).append(',')
    sb.append("\"kingRingPressure\":").append(f.kingRingPressure).append(',')
    sb.append("\"spaceControl\":").append(f.spaceControl)
    sb.append('}')

  private def fmt(d: Double): String = f"$d%.2f"
  private def pct(d: Double): Double = if d > 1.0 then d else d * 100.0

  private def renderConcepts(sb: StringBuilder, key: String, c: Concepts): Unit =
    sb.append('"').append(key).append("\":{")
    sb.append("\"dynamic\":").append(fmt(c.dynamic)).append(',')
    sb.append("\"drawish\":").append(fmt(c.drawish)).append(',')
    sb.append("\"imbalanced\":").append(fmt(c.imbalanced)).append(',')
    sb.append("\"tacticalDepth\":").append(fmt(c.tacticalDepth)).append(',')
    sb.append("\"blunderRisk\":").append(fmt(c.blunderRisk)).append(',')
    sb.append("\"pawnStorm\":").append(fmt(c.pawnStorm)).append(',')
    sb.append("\"fortress\":").append(fmt(c.fortress)).append(',')
    sb.append("\"colorComplex\":").append(fmt(c.colorComplex)).append(',')
    sb.append("\"badBishop\":").append(fmt(c.badBishop)).append(',')
    sb.append("\"goodKnight\":").append(fmt(c.goodKnight)).append(',')
    sb.append("\"rookActivity\":").append(fmt(c.rookActivity)).append(',')
    sb.append("\"kingSafety\":").append(fmt(c.kingSafety)).append(',')
    sb.append("\"dry\":").append(fmt(c.dry)).append(',')
    sb.append("\"comfortable\":").append(fmt(c.comfortable)).append(',')
    sb.append("\"unpleasant\":").append(fmt(c.unpleasant)).append(',')
    sb.append("\"engineLike\":").append(fmt(c.engineLike)).append(',')
    sb.append("\"conversionDifficulty\":").append(fmt(c.conversionDifficulty)).append(',')
    sb.append("\"sacrificeQuality\":").append(fmt(c.sacrificeQuality)).append(',')
    sb.append("\"alphaZeroStyle\":").append(fmt(c.alphaZeroStyle))
    sb.append('}')

  private def renderOpeningStats(sb: StringBuilder, os: OpeningExplorer.Stats): Unit =
    sb.append('{')
    sb.append("\"bookPly\":").append(os.bookPly).append(',')
    sb.append("\"noveltyPly\":").append(os.noveltyPly).append(',')
    os.games.foreach(g => sb.append("\"games\":").append(g).append(','))
    os.winWhite.foreach(w => sb.append("\"winWhite\":").append(fmt(pct(w))).append(','))
    os.winBlack.foreach(w => sb.append("\"winBlack\":").append(fmt(pct(w))).append(','))
    os.draw.foreach(d => sb.append("\"draw\":").append(fmt(pct(d))).append(','))
    os.minYear.foreach(y => sb.append("\"minYear\":").append(y).append(','))
    os.maxYear.foreach(y => sb.append("\"maxYear\":").append(y).append(','))
    if os.yearBuckets.nonEmpty then
      sb.append("\"yearBuckets\":{")
      os.yearBuckets.zipWithIndex.foreach { case ((k, v), idx) =>
        if idx > 0 then sb.append(',')
        sb.append("\"").append(escape(k)).append("\":").append(v)
      }
      sb.append("},")
    if os.topMoves.nonEmpty then
      sb.append("\"topMoves\":[")
      os.topMoves.zipWithIndex.foreach { case (m, idx) =>
        if idx > 0 then sb.append(',')
        sb.append('{')
        sb.append("\"san\":\"").append(escape(m.san)).append("\",")
        sb.append("\"uci\":\"").append(escape(m.uci)).append("\",")
        sb.append("\"games\":").append(m.games)
        m.winPct.foreach(w => sb.append(',').append("\"winPct\":").append(fmt(pct(w))))
        m.drawPct.foreach(d => sb.append(',').append("\"drawPct\":").append(fmt(pct(d))))
        sb.append('}')
      }
      sb.append("],")
    if os.topGames.nonEmpty then
      sb.append("\"topGames\":[")
      os.topGames.zipWithIndex.foreach { case (g, idx) =>
        if idx > 0 then sb.append(',')
        sb.append('{')
        sb.append("\"white\":\"").append(escape(g.white)).append("\",")
        sb.append("\"black\":\"").append(escape(g.black)).append("\",")
        g.whiteElo.foreach(e => sb.append("\"whiteElo\":").append(e).append(','))
        g.blackElo.foreach(e => sb.append("\"blackElo\":").append(e).append(','))
        sb.append("\"result\":\"").append(escape(g.result)).append("\",")
        g.date.foreach(d => sb.append("\"date\":\"").append(escape(d)).append("\","))
        g.event.foreach(e => sb.append("\"event\":\"").append(escape(e)).append("\","))
        if sb.length() > 0 && sb.charAt(sb.length - 1) == ',' then sb.setLength(sb.length - 1)
        sb.append('}')
      }
      sb.append("],")
    sb.append("\"source\":\"").append(escape(os.source)).append('"')
    sb.append('}')

  private def renderEval(sb: StringBuilder, key: String, eval: EngineEval): Unit =
    sb.append('"').append(key).append("\":{")
    sb.append("\"depth\":").append(eval.depth).append(',')
    sb.append("\"lines\":[")
    eval.lines.zipWithIndex.foreach { case (l, idx) =>
      if idx > 0 then sb.append(',')
      sb.append('{')
      sb.append("\"move\":\"").append(escape(l.move)).append("\",")
      sb.append("\"winPct\":").append(fmt(l.winPct)).append(',')
      l.cp.foreach(cp => sb.append("\"cp\":").append(cp).append(','))
      l.mate.foreach(m => sb.append("\"mate\":").append(m).append(','))
      sb.append("\"pv\":[")
      l.pv.zipWithIndex.foreach { case (m, i) =>
        if i > 0 then sb.append(',')
        sb.append("\"").append(escape(m)).append("\"")
      }
      sb.append("]}")
    }
    sb.append("]}")

  private def renderCritical(sb: StringBuilder, c: CriticalNode): Unit =
    sb.append('{')
    sb.append("\"ply\":").append(c.ply.value).append(',')
    sb.append("\"reason\":\"").append(escape(c.reason)).append("\",")
    sb.append("\"deltaWinPct\":").append(fmt(c.deltaWinPct)).append(',')
    c.bestVsSecondGap.foreach { g =>
      sb.append("\"bestVsSecondGap\":").append(fmt(g)).append(',')
    }
    c.bestVsPlayedGap.foreach { g =>
      sb.append("\"bestVsPlayedGap\":").append(fmt(g)).append(',')
    }
    c.legalMoves.foreach { lm =>
      sb.append("\"legalMoves\":").append(lm).append(',')
    }
    sb.append("\"forced\":").append(c.forced).append(',')
    c.mistakeCategory.foreach { cat =>
      sb.append("\"mistakeCategory\":\"").append(escape(cat)).append("\",")
    }
    if c.tags.nonEmpty then
      sb.append("\"tags\":[")
      c.tags.zipWithIndex.foreach { case (t, idx) =>
        if idx > 0 then sb.append(',')
        sb.append("\"").append(escape(t)).append('"')
      }
      sb.append("],")
    c.comment.foreach { txt =>
      sb.append("\"comment\":\"").append(escape(txt)).append("\",")
    }
    sb.append("\"branches\":[")
    c.branches.zipWithIndex.foreach { case (b, idx) =>
      if idx > 0 then sb.append(',')
      sb.append('{')
      sb.append("\"move\":\"").append(escape(b.move)).append("\",")
      sb.append("\"winPct\":").append(fmt(b.winPct)).append(',')
      sb.append("\"label\":\"").append(escape(b.label)).append("\",")
      sb.append("\"pv\":[")
      b.pv.zipWithIndex.foreach { case (m, i) =>
        if i > 0 then sb.append(',')
        sb.append("\"").append(escape(m)).append("\"")
      }
      sb.append("]}")
    }
    sb.append("]}")

  private def renderTree(sb: StringBuilder, n: TreeNode): Unit =
    sb.append('{')
    sb.append("\"ply\":").append(n.ply).append(',')
    sb.append("\"san\":\"").append(escape(n.san)).append("\",")
    sb.append("\"uci\":\"").append(escape(n.uci)).append("\",")
    sb.append("\"fen\":\"").append(escape(n.fen)).append("\",")
    sb.append("\"eval\":").append(fmt(n.eval)).append(',')
    sb.append("\"evalType\":\"").append(n.evalType).append("\",")
    sb.append("\"judgement\":\"").append(escape(n.judgement)).append("\",")
    sb.append("\"glyph\":\"").append(escape(n.glyph)).append("\",")
    sb.append("\"tags\":[")
    n.tags.zipWithIndex.foreach { case (t, idx) =>
      if idx > 0 then sb.append(',')
      sb.append("\"").append(escape(t)).append('"')
    }
    sb.append("],")
    n.bestMove.foreach(m => sb.append("\"bestMove\":\"").append(escape(m)).append("\","))
    n.bestEval.foreach(v => sb.append("\"bestEval\":").append(fmt(v)).append(','))
    if n.pv.nonEmpty then
      sb.append("\"pv\":[")
      n.pv.zipWithIndex.foreach { case (m, idx) =>
        if idx > 0 then sb.append(',')
        sb.append("\"").append(escape(m)).append('"')
      }
      sb.append("],")
    n.comment.foreach(c => sb.append("\"comment\":\"").append(escape(c)).append("\","))
    sb.append("\"children\":[")
    n.children.zipWithIndex.foreach { case (c, idx) =>
      if idx > 0 then sb.append(',')
      renderTree(sb, c)
    }
    sb.append("]}")

  def escape(in: String): String =
    val sb = new StringBuilder(in.length + 8)
    in.foreach {
      case '"' => sb.append("\\\"")
      case '\\' => sb.append("\\\\")
      case '\n' => sb.append("\\n")
      case '\r' => sb.append("\\r")
      case '\t' => sb.append("\\t")
      case c => sb.append(c)
    }
    sb.result()
