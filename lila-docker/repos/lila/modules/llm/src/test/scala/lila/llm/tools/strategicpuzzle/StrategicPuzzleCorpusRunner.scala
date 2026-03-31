package lila.llm.tools.strategicpuzzle

import java.nio.file.{ Files, Path }

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.*
import lila.llm.analysis.{ CommentaryEngine, NarrativeContextBuilder, OpeningExplorerClient, StrategyPackBuilder }
import lila.llm.model.NarrativeRenderMode
import lila.llm.model.strategic.VariationLine
import lila.llm.tools.strategicpuzzle.StrategicPuzzleCorpusSupport.*

object StrategicPuzzleCorpusRunner:

  final case class Config(
      inputPath: Path,
      outDir: Path,
      limit: Option[Int],
      resume: Boolean,
      evalEndpoint: String,
      stopBeforeLlm: Boolean,
      rootParallelism: Int
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[strategic-puzzle] $err")
        sys.exit(2)
      case Right(config) =>
        given Executor = ExecutionContext.global
        given ActorSystem = ActorSystem("strategic-puzzle-corpus-runner")
        val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
        try
          run(config, ws)
        finally
          ws.close()
          summon[ActorSystem].terminate()

  private def run(config: Config, ws: StandaloneAhcWSClient)(using Executor): Unit =
    Files.createDirectories(config.outDir)
    val stage01Path = config.outDir.resolve("stage01_structural_pass.jsonl")
    val stage02Path = config.outDir.resolve("stage02_root_analysis.jsonl")
    val preLlmPath = config.outDir.resolve("stage02_pre_llm_survivors.jsonl")
    val stage03Path = config.outDir.resolve("stage03_move_analyses.jsonl")
    val publishPath = config.outDir.resolve("stage04_publish.jsonl")
    val reservePath = config.outDir.resolve("stage04_reserve.jsonl")
    val rejectPath = config.outDir.resolve("stage04_rejects.jsonl")
    val reportPath = config.outDir.resolve("report.json")

    val seeds = loadSeeds(config)
    println(s"[strategic-puzzle] loaded ${seeds.size} CET seeds from ${config.inputPath}")

    val rejectRows = scala.collection.mutable.ListBuffer.empty[RejectRow]

    val stage01Rows =
      seeds.flatMap { seed =>
        structuralComputation(seed) match
          case Left(err) =>
            rejectRows += RejectRow(seed.id, seed.fen, seed.fenKey, "stage01_structural", List(err))
            None
          case Right(result) if result.screening.passed =>
            Some(
              Stage01StructuralPassRow(
                source = sourcePayload(seed),
                position = result.position,
                screening = result.screening
              )
            )
          case Right(result) =>
            rejectRows += RejectRow(seed.id, seed.fen, seed.fenKey, "stage01_structural", result.screening.reasons)
            None
      }
    writeJsonl(stage01Path, stage01Rows)
    println(s"[strategic-puzzle] stage01 pass count=${stage01Rows.size}")

    val stage02Rows =
      loadOrBuildStage02(
        config = config,
        ws = ws,
        stage01Rows = stage01Rows,
        stage02Path = stage02Path,
        rejectRows = rejectRows
      )
    writeJsonl(preLlmPath, stage02Rows)
    println(s"[strategic-puzzle] stage02 pass count=${stage02Rows.size}")

    if config.stopBeforeLlm then
      val report =
        Json.obj(
          "schema" -> "chesstory.strategicPuzzle.report.v1",
          "generatedAt" -> isoNow(),
          "mode" -> "stop_before_llm",
          "config" -> Json.obj(
            "inputPath" -> config.inputPath.toString,
            "outDir" -> config.outDir.toString,
            "limit" -> config.limit,
            "resume" -> config.resume,
            "evalEndpoint" -> config.evalEndpoint,
            "stopBeforeLlm" -> true,
            "rootParallelism" -> config.rootParallelism
          ),
          "counts" -> Json.obj(
            "inputSeeds" -> seeds.size,
            "stage01Pass" -> stage01Rows.size,
            "stage02Pass" -> stage02Rows.size,
            "preLlmSurvivors" -> stage02Rows.size,
            "rejectRows" -> rejectRows.size
          )
        )
      writeJsonl(rejectPath, rejectRows.distinctBy(rr => s"${rr.seedId}|${rr.stage}|${rr.reasons.mkString(",")}"))
      writeJson(reportPath, report)
      println(s"[strategic-puzzle] stop-before-llm mode wrote ${stage02Rows.size} survivors to $preLlmPath")
      return

    val stage03Rows =
      loadOrBuildStage03(
        config = config,
        ws = ws,
        stage02Rows = stage02Rows,
        stage03Path = stage03Path,
        rejectRows = rejectRows
      )
    println(s"[strategic-puzzle] stage03 move analyses=${stage03Rows.size}")

    val stage03BySeed = stage03Rows.groupBy(_.seedId)
    val candidateDocs =
      stage02Rows.flatMap { row =>
        val moveRows =
          stage03BySeed
            .getOrElse(row.source.seedId, Nil)
            .filter(mr => row.rootAnalysis.candidateMoves.exists(_.uci == mr.move.uci))
        if moveRows.isEmpty then
          rejectRows += RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "stage04_family", List("no_move_analyses"))
          None
        else
          chooseDominantFamily(moveRows) match
            case None =>
              rejectRows += RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "stage04_family", List("no_robust_family"))
              None
            case Some(family) =>
              val (accepted, partial, alternate) = assignCredits(family.summary, moveRows)
              if accepted.isEmpty || accepted.size > MaxAcceptedMoves then
                rejectRows += RejectRow(
                  row.source.seedId,
                  row.position.fen,
                  row.position.fenKey,
                  "stage04_credit_assignment",
                  List(s"invalid_full_credit_count:${accepted.size}")
                )
                None
              else
                Some(
                  StrategicPuzzleDoc(
                    id = stablePuzzleId(row.position.fen, family.summary.key, accepted),
                    schema = Schema,
                    source = row.source,
                    position = row.position,
                    screening = row.screening,
                    rootAnalysis = row.rootAnalysis,
                    dominantFamily = family.summary,
                    acceptedMoves = accepted,
                    partialMoves = partial,
                    alternateMoves = alternate,
                    qualityScore = QualityScore(0, 0, 0, 0, 0),
                    generationMeta = GenerationMeta(isoNow(), RunnerVersion, "candidate")
                  )
                )
      }

    val openingFreq = candidateDocs.groupBy(_.source.opening.getOrElse("unknown")).view.mapValues(_.size).toMap
    val familyFreq = candidateDocs.groupBy(_.dominantFamily.dominantIdeaKind).view.mapValues(_.size).toMap
    val sideFreq = candidateDocs.groupBy(_.position.sideToMove).view.mapValues(_.size).toMap
    val scoredDocs =
      candidateDocs.map { doc =>
        scorePuzzleDoc(
          doc = doc,
          openingFrequency = openingFreq.getOrElse(doc.source.opening.getOrElse("unknown"), 0),
          familyFrequency = familyFreq.getOrElse(doc.dominantFamily.dominantIdeaKind, 0),
          sideFrequency = sideFreq.getOrElse(doc.position.sideToMove, 0),
          poolSize = candidateDocs.size
        )
      }

    val selection = selectAutoPublish(scoredDocs, PublishTarget)
    val publish =
      selection.autoPublish.map(doc => doc.copy(generationMeta = doc.generationMeta.copy(selectionStatus = "auto_publish")))
    val reserve =
      selection.reserve.map(doc => doc.copy(generationMeta = doc.generationMeta.copy(selectionStatus = "reserve")))

    writeJsonl(publishPath, publish)
    writeJsonl(reservePath, reserve)
    writeJsonl(rejectPath, rejectRows.distinctBy(rr => s"${rr.seedId}|${rr.stage}|${rr.reasons.mkString(",")}"))

    val report =
      Json.obj(
        "schema" -> "chesstory.strategicPuzzle.report.v1",
        "generatedAt" -> isoNow(),
        "config" -> Json.obj(
          "inputPath" -> config.inputPath.toString,
          "outDir" -> config.outDir.toString,
          "limit" -> config.limit,
          "resume" -> config.resume,
          "evalEndpoint" -> config.evalEndpoint,
          "rootParallelism" -> config.rootParallelism
        ),
        "counts" -> Json.obj(
          "inputSeeds" -> seeds.size,
          "stage01Pass" -> stage01Rows.size,
          "stage02Pass" -> stage02Rows.size,
          "stage03MoveAnalyses" -> stage03Rows.size,
          "candidateDocs" -> scoredDocs.size,
          "autoPublish" -> publish.size,
          "reserve" -> reserve.size,
          "rejectRows" -> rejectRows.size
        ),
        "acceptance" -> Json.obj(
          "duplicateFenPublish" -> publish.groupBy(_.position.fenKey).count(_._2.size > 1),
          "duplicateIdPublish" -> publish.groupBy(_.id).count(_._2.size > 1),
          "fullCreditExplanationMissing" -> publish.flatMap(_.acceptedMoves).count(!_.explanation.passed),
          "publishFamilyMissing" -> publish.count(_.dominantFamily.key.trim.isEmpty)
        ),
        "distributions" -> Json.obj(
          "publishBySide" -> publish.groupBy(_.position.sideToMove).view.mapValues(_.size).toMap,
          "publishByIdea" -> publish.groupBy(_.dominantFamily.dominantIdeaKind).view.mapValues(_.size).toMap,
          "publishByOpening" -> publish.groupBy(_.source.opening.getOrElse("unknown")).view.mapValues(_.size).toMap
        )
      )
    writeJson(reportPath, report)
    println(s"[strategic-puzzle] wrote artifacts under ${config.outDir}")

  private def loadSeeds(config: Config): List[CetSeed] =
    if !Files.exists(config.inputPath) then
      throw new IllegalArgumentException(s"input file does not exist: ${config.inputPath}")
    val raw = readJsonl[CetSeed](config.inputPath).fold(err => throw new IllegalArgumentException(err), identity)
    raw
      .groupBy(_.fenKey)
      .values
      .flatMap(_.sortBy(_.id).headOption)
      .toList
      .sortBy(_.id)
      .pipe(seeds => config.limit.fold(seeds)(limit => seeds.take(limit)))

  private def loadOrBuildStage02(
      config: Config,
      ws: StandaloneAhcWSClient,
      stage01Rows: List[Stage01StructuralPassRow],
      stage02Path: Path,
      rejectRows: scala.collection.mutable.ListBuffer[RejectRow]
  )(using Executor): List[Stage02RootAnalysisRow] =
    val allowedSeedIds = stage01Rows.map(_.source.seedId).toSet
    if config.resume && Files.exists(stage02Path) then
      readJsonl[Stage02RootAnalysisRow](stage02Path)
        .fold(err => throw new IllegalArgumentException(err), identity)
        .filter(row => allowedSeedIds.contains(row.source.seedId))
    else
      val evalClient = EvalClient(ws, config.evalEndpoint)
      val built = scala.collection.mutable.ListBuffer.empty[Stage02RootAnalysisRow]
      stage01Rows.grouped(config.rootParallelism.max(1)).foreach { batch =>
        val batchResults =
          Await.result(
            Future.traverse(batch)(row => buildStage02Row(row, evalClient)),
            (90.seconds.toMillis * batch.size.max(1)).millis
          )
        batchResults.foreach {
          case Right(row)   => built += row
          case Left(reject) => rejectRows += reject
        }
      }
      val rows = built.toList
      writeJsonl(stage02Path, rows)
      rows

  private def loadOrBuildStage03(
      config: Config,
      ws: StandaloneAhcWSClient,
      stage02Rows: List[Stage02RootAnalysisRow],
      stage03Path: Path,
      rejectRows: scala.collection.mutable.ListBuffer[RejectRow]
  )(using Executor): List[Stage03MoveAnalysisRow] =
    val allowedSeedIds = stage02Rows.map(_.source.seedId).toSet
    val existing =
      if config.resume && Files.exists(stage03Path) then
        readJsonl[Stage03MoveAnalysisRow](stage03Path)
          .fold(err => throw new IllegalArgumentException(err), identity)
          .filter(row => allowedSeedIds.contains(row.seedId))
      else
        Files.deleteIfExists(stage03Path)
        Nil

    val existingKeys = existing.map(moveAnalysisKey).toSet
    val llmApi = mkApi(ws)
    val evalClient = EvalClient(ws, config.evalEndpoint)
    val built = scala.collection.mutable.ListBuffer.from(existing)

    stage02Rows.foreach { row =>
      row.rootAnalysis.candidateMoves.foreach { move =>
        val key = s"${row.source.seedId}:${move.uci}"
        if !existingKeys.contains(key) then
          try
            val afterLines = Await.result(evalClient.analyze(move.afterFen, AfterMultiPv, AfterDepth), 60.seconds)
            val afterVars = afterLines.map(_.toVariationLine(move.afterFen))
            val rootVars = reorderRootVariations(row.rootAnalysis.variations, move.uci)
            val resultOpt =
              Await.result(
                llmApi.bookmakerCommentPosition(
                  fen = row.position.fen,
                  lastMove = Some(move.uci),
                  eval = bestEvalData(rootVars),
                  variations = Some(rootVars),
                  probeResults = None,
                  openingData = None,
                  afterFen = Some(move.afterFen),
                  afterEval = bestEvalData(afterVars),
                  afterVariations = Option.when(afterVars.nonEmpty)(afterVars),
                  opening = row.source.opening,
                  phase = row.position.phase,
                  ply = row.position.ply,
                  prevStateToken = None,
                  allowLlmPolish = true,
                  lang = "en"
                ),
                180.seconds
              )
            resultOpt match
              case None =>
                rejectRows += RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "stage03_move", List(s"empty_bookmaker_response:${move.uci}"))
              case Some(result) =>
                val response = result.response
                val stage03Row =
                  Stage03MoveAnalysisRow(
                    seedId = row.source.seedId,
                    fenKey = row.position.fenKey,
                    move = move,
                    family = familySummary(response, move.cpLoss),
                    explanation = explanationGate(response),
                    response = responseJson(response),
                    generatedAt = isoNow()
                  )
                appendJsonl(stage03Path, stage03Row)
                built += stage03Row
          catch
            case NonFatal(err) =>
              rejectRows += RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "stage03_move", List(s"${move.uci}:${err.getMessage}"))
      }
    }
    built.toList

  private def buildPreScan(row: Stage01StructuralPassRow, rootScreen: RootScreenResult): RootStrategicPreScan =
    val dataOpt =
      CommentaryEngine.assessExtended(
        fen = row.position.fen,
        variations = rootScreen.rootAnalysis.variations,
        playedMove = rootScreen.rootAnalysis.candidateMoves.headOption.map(_.uci),
        opening = row.source.opening,
        phase = Some(row.position.phase),
        ply = row.position.ply,
        prevMove = rootScreen.rootAnalysis.candidateMoves.headOption.map(_.uci)
      )
    dataOpt match
      case None => RootStrategicPreScan(false, List("commentary_engine_assessment_missing"), 0, 0, 0, None)
      case Some(data) =>
        val ctx =
          NarrativeContextBuilder.build(
            data = data,
            ctx = data.toContext,
            renderMode = NarrativeRenderMode.Bookmaker
          )
        val strategyPack = StrategyPackBuilder.build(data, ctx)
        summarizeRootPreScan(strategyPack, strategyPack.flatMap(_.signalDigest))

  private def buildStage02Row(
      row: Stage01StructuralPassRow,
      evalClient: EvalClient
  )(using Executor): Future[Either[RejectRow, Stage02RootAnalysisRow]] =
    evalClient
      .analyze(row.position.fen, RootMultiPv, RootDepth)
      .map { rootLines =>
        screenRootPosition(row.position.fen, rootLines) match
          case Left(reasons) =>
            Left(RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "stage02_root", reasons))
          case Right(rootScreen) =>
            buildPreScan(row, rootScreen) match
              case preScan if preScan.passed =>
                Right(
                  Stage02RootAnalysisRow(
                    source = row.source,
                    position = row.position,
                    screening = row.screening,
                    rootAnalysis = rootScreen.rootAnalysis,
                    preScan = preScan
                  )
                )
              case preScan =>
                Left(RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "stage02_prescan", preScan.reasons))
      }
      .recover { case NonFatal(err) =>
        Left(RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "stage02_root", List(err.getMessage)))
      }

  private def reorderRootVariations(variations: List[VariationLine], moveUci: String): List[VariationLine] =
    variations.partition(_.moves.headOption.contains(moveUci)) match
      case (Nil, others)      => others
      case (matching, others) => matching ::: others

  private def mkApi(ws: StandaloneAhcWSClient)(using Executor): LlmApi =
    LlmApi(
      openingExplorer = OpeningExplorerClient(ws),
      geminiClient = GeminiClient(ws, GeminiConfig.fromEnv),
      openAiClient = OpenAiClient(ws, OpenAiConfig.fromEnv),
      commentaryCache = CommentaryCache(),
      llmConfig = LlmConfig.fromEnv,
      providerConfig = LlmProviderConfig.fromEnv.copy(polishGateThreshold = 2.0, premiumOnly = false)
    )

  private def parseArgs(args: List[String]): Either[String, Config] =
    val workspaceRoot = detectWorkspaceRoot()
    val defaults =
      Config(
        inputPath = workspaceRoot.resolve(Path.of("tools", "cet", "data", "chessevaluationtraining_positions.jsonl")),
        outDir = workspaceRoot.resolve(Path.of("tools", "strategic_puzzles", "runs", runDirName())),
        limit = None,
        resume = false,
        evalEndpoint = sys.env.get("ACCOUNTINTEL_EVAL_ENDPOINT").orElse(sys.env.get("EXTERNAL_ENGINE_ENDPOINT")).getOrElse("http://localhost:9666"),
        stopBeforeLlm = false,
        rootParallelism = sys.env.get("STRATEGIC_PUZZLE_ROOT_PARALLELISM").flatMap(_.toIntOption).filter(_ > 0).getOrElse(4)
      )

    @annotation.tailrec
    def loop(rest: List[String], cfg: Config): Either[String, Config] =
      rest match
        case Nil => Right(cfg)
        case "--resume" :: tail => loop(tail, cfg.copy(resume = true))
        case "--stop-before-llm" :: tail => loop(tail, cfg.copy(stopBeforeLlm = true))
        case head :: tail if head.startsWith("--root-parallelism=") =>
          head.stripPrefix("--root-parallelism=").toIntOption.filter(_ > 0) match
            case Some(value) => loop(tail, cfg.copy(rootParallelism = value))
            case None        => Left(s"invalid --root-parallelism: $head")
        case "--root-parallelism" :: value :: tail =>
          value.toIntOption.filter(_ > 0) match
            case Some(v) => loop(tail, cfg.copy(rootParallelism = v))
            case None    => Left(s"invalid --root-parallelism: $value")
        case head :: tail if head.startsWith("--input=") =>
          loop(tail, cfg.copy(inputPath = Path.of(head.stripPrefix("--input=")).toAbsolutePath.normalize))
        case "--input" :: value :: tail =>
          loop(tail, cfg.copy(inputPath = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--out-dir=") =>
          loop(tail, cfg.copy(outDir = Path.of(head.stripPrefix("--out-dir=")).toAbsolutePath.normalize))
        case "--out-dir" :: value :: tail =>
          loop(tail, cfg.copy(outDir = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--limit=") =>
          head.stripPrefix("--limit=").toIntOption.filter(_ > 0) match
            case Some(limit) => loop(tail, cfg.copy(limit = Some(limit)))
            case None        => Left(s"invalid --limit: $head")
        case "--limit" :: value :: tail =>
          value.toIntOption.filter(_ > 0) match
            case Some(limit) => loop(tail, cfg.copy(limit = Some(limit)))
            case None        => Left(s"invalid --limit: $value")
        case head :: tail if head.startsWith("--eval-endpoint=") =>
          loop(tail, cfg.copy(evalEndpoint = head.stripPrefix("--eval-endpoint=")))
        case "--eval-endpoint" :: value :: tail =>
          loop(tail, cfg.copy(evalEndpoint = value))
        case unknown :: _ => Left(s"unknown argument: $unknown")

    loop(args, defaults)

  private def detectWorkspaceRoot(): Path =
    val cwd = Path.of(".").toAbsolutePath.normalize
    if Files.isDirectory(cwd.resolve("tools")) && Files.isDirectory(cwd.resolve("lila-docker")) then cwd
    else
      Option(cwd.getParent)
        .flatMap(parent => Option(parent.getParent))
        .flatMap(grandParent => Option(grandParent.getParent))
        .filter(root => Files.isDirectory(root.resolve("tools")))
        .getOrElse(cwd)

  private final case class EvalClient(ws: StandaloneAhcWSClient, endpoint: String)(using Executor):
    def analyze(fen: String, multiPv: Int, depth: Int): Future[List[EnginePv]] =
      ws.url(endpoint)
        .addQueryStringParameters(
          "fen" -> fen,
          "multiPv" -> multiPv.toString,
          "depth" -> depth.toString,
          "variant" -> "standard"
        )
        .withRequestTimeout(20.seconds)
        .get()
        .map { response =>
          if response.status / 100 != 2 then
            throw new IllegalStateException(s"eval endpoint returned status=${response.status}")
          parseEvalResponse(Json.parse(response.body[String]))
        }

    private def parseEvalResponse(js: JsValue): List[EnginePv] =
      (js \ "pvs").asOpt[List[JsObject]].getOrElse(Nil).zipWithIndex.flatMap { case (row, idx) =>
        val moves = (row \ "moves").asOpt[String].map(parseMoveString).getOrElse(Nil)
        Option.when(moves.nonEmpty) {
          EnginePv(
            rank = idx + 1,
            cp = (row \ "cp").asOpt[Int],
            mate = (row \ "mate").asOpt[Int],
            depth = (row \ "depth").asOpt[Int].getOrElse(0),
            moves = moves
          )
        }
      }

  extension [A](a: A)
    private def pipe[B](f: A => B): B = f(a)
