package chess
package analysis

import AnalysisModel.*
import chess.format.pgn.PgnStr
import chess.opening.OpeningDb
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

/** 간단한 PGN→타임라인 CLI.
  *
  * 사용법: sbt "core/runMain chess.analysis.AnalyzePgn path/to/game.pgn"
  * 출력: JSON 한 줄 (timeline + opening + per-ply feature 스냅샷)
  */
object AnalyzePgn:
  export AnalysisModel.{ EngineConfig, EngineLine, EngineEval, Concepts, PlyOutput, Output, Branch, CriticalNode, TreeNode, StudyLine, StudyChapter }
  export AnalyzeUtils.escape

  def main(args: Array[String]): Unit =
    if args.length != 1 then
      System.err.println("usage: AnalyzePgn <path/to/game.pgn>")
      sys.exit(1)
    val path = args(0)
    val pgn = Source.fromFile(path).mkString
    analyzeAndRender(pgn, EngineConfig.fromEnv()) match
      case Left(err) =>
        System.err.println(err)
        sys.exit(1)
      case Right(json) =>
        println(json)
        EnginePool.shutdown()

  /** PGN 문자열을 분석하여 JSON 문자열로 반환 */
  def analyzeAndRender(pgn: String, config: EngineConfig = EngineConfig.fromEnv(), llmRequestedPlys: Set[Int] = Set.empty): Either[String, String] =
    analyze(pgn, config, llmRequestedPlys).map(AnalysisSerializer.render)

  /** PGN 문자열을 분석하여 Output 객체 반환 */
  def analyze(pgn: String, config: EngineConfig = EngineConfig.fromEnv(), llmRequestedPlys: Set[Int] = Set.empty, jobId: Option[String] = None): Either[String, Output] =
    Replay.mainline(PgnStr(pgn)).flatMap(_.valid) match
      case Left(err) => Left(s"PGN 파싱 실패: ${err.value}")
      case Right(replay) =>
        // Stage 1: Engine Evaluation (40% weight)
        jobId.foreach(id => AnalysisProgressTracker.update(id, AnalysisStage.ENGINE_EVALUATION, 0.0))
        
        val playerContext = extractPlayerContext(pgn)
        val sans = replay.chronoMoves.map(_.toSanStr)
        val opening = OpeningDb.searchWithTransposition(sans)
        val openingStats = OpeningExplorer.explore(opening, replay.chronoMoves.map(_.toSanStr).toList)
        val engineService = new EngineService() 
        val experimentRunner = new ExperimentRunner(engineService)

          jobId.foreach(id => AnalysisProgressTracker.update(id, AnalysisStage.ENGINE_EVALUATION, 0.05)) // Setup done
          
          val futureTimeline = TimelineBuilder.buildTimeline(replay, engineService, experimentRunner, config, opening, playerContext, jobId)
          
          // Await result (CLI mode)
          val (timelineRaw, finalGame) = Await.result(futureTimeline, Duration.Inf)
          
          val timeline = StudySignals.withStudySignals(timelineRaw, opening)
          
          jobId.foreach(id => AnalysisProgressTracker.update(id, AnalysisStage.ENGINE_EVALUATION, 1.0))
          
          // Stage 2: Critical Detection (10% weight)
          jobId.foreach(id => AnalysisProgressTracker.update(id, AnalysisStage.CRITICAL_DETECTION, 0.0))
          
          // Use EnginePool for blocking operations (CriticalDetector, ReviewTreeBuilder)
          // We assume sequential execution for now to keep logic simple
          
          val critical = Await.result(
            EnginePool.withEngine { client =>
              Future {
                CriticalDetector.detectCritical(timeline, client, config, llmRequestedPlys, jobId)
              }
            }, Duration.Inf)
            
          val oppositeColorBishops = FeatureExtractor.hasOppositeColorBishops(finalGame.position.board)
          val root = TreeBuilder.buildTree(timeline, critical)
          val studyChapters = StudyChapterBuilder.buildStudyChapters(timeline)
          val (openingSummary, bookExitComment, openingTrend) = OpeningNotes.buildOpeningNotes(opening, openingStats, timeline)
          val (accWhite, accBlack) = AccuracyScore.calculateBothSides(timeline)
          
          // Phase 4.6: Build Book JSON
          val gameMeta = BookModel.GameMeta(
            white = findTag(pgn, "White").getOrElse("?"),
            black = findTag(pgn, "Black").getOrElse("?"),
            result = findTag(pgn, "Result").getOrElse("*"),
            openingName = opening.map(_.opening.name.value)
          )
          val book = Some(BookBuilder.buildBook(timeline, gameMeta))
          
          jobId.foreach(id => AnalysisProgressTracker.update(id, AnalysisStage.CRITICAL_DETECTION, 1.0))
          
          // Stage 3: LLM will be tracked in ApiServer
          // Stage 4: Finalization
          jobId.foreach(id => AnalysisProgressTracker.update(id, AnalysisStage.FINALIZATION, 0.0))
          
          Right(
            Output(
              opening,
              openingStats,
              timeline,
              oppositeColorBishops,
              critical,
              openingSummary,
              bookExitComment,
              openingTrend,
              None, // summaryText
              root = root,
              studyChapters = studyChapters,
              pgn = pgn,
              accuracyWhite = Some(accWhite),
              accuracyBlack = Some(accBlack),
              book = book
            )
          )

  def render(output: Output): String = AnalysisSerializer.render(output)

  private def extractPlayerContext(pgn: String): Option[PlayerContext] =
    val whiteElo = findTag(pgn, "WhiteElo").flatMap(_.toIntOption)
    val blackElo = findTag(pgn, "BlackElo").flatMap(_.toIntOption)
    val timeControl = findTag(pgn, "TimeControl").flatMap(parseTimeControl)
    
    if whiteElo.isEmpty && blackElo.isEmpty && timeControl.isEmpty then None
    else Some(PlayerContext(whiteElo, blackElo, timeControl))

  private def findTag(pgn: String, tagName: String): Option[String] =
    val regex = s"""\\[$tagName\\s+"([^"]+)"\\]""".r
    regex.findFirstMatchIn(pgn).map(_.group(1))

  private def parseTimeControl(tc: String): Option[TimeControl] =
    // Formats: "180+2", "600", "300+0"
    val parts = tc.split("\\+")
    if parts.length == 2 then
      for
        initial <- parts(0).toIntOption
        increment <- parts(1).toIntOption
      yield TimeControl(initial, increment)
    else if parts.length == 1 then
      parts(0).toIntOption.map(initial => TimeControl(initial, 0))
    else
      None
