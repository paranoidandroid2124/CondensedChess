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
import lila.llm.analysis.OpeningExplorerClient
import lila.llm.model.strategic.VariationLine
import lila.llm.tools.strategicpuzzle.StrategicPuzzleCorpusSupport.*

object StrategicPuzzlePrePolishRunner:

  final case class Config(
      inputPath: Path,
      outDir: Path,
      limit: Option[Int],
      resume: Boolean,
      evalEndpoint: String
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[strategic-prepolish] $err")
        sys.exit(2)
      case Right(config) =>
        given Executor = ExecutionContext.global
        given ActorSystem = ActorSystem("strategic-puzzle-pre-polish-runner")
        val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
        try
          run(config, ws)
        finally
          ws.close()
          summon[ActorSystem].terminate()

  private def run(config: Config, ws: StandaloneAhcWSClient)(using Executor): Unit =
    Files.createDirectories(config.outDir)
    val stage03Path = config.outDir.resolve("stage03_rule_move_analyses.jsonl")
    val candidatePath = config.outDir.resolve("stage04_pre_polish_candidates.jsonl")
    val publishPath = config.outDir.resolve("stage04_pre_polish_auto_publish.jsonl")
    val reservePath = config.outDir.resolve("stage04_pre_polish_reserve.jsonl")
    val rejectPath = config.outDir.resolve("stage04_pre_polish_rejects.jsonl")
    val reportPath = config.outDir.resolve("report_pre_polish.json")

    val stage02Rows = loadStage02Rows(config)
    println(s"[strategic-prepolish] loaded ${stage02Rows.size} pre-LLM survivors from ${config.inputPath}")

    val rejectRows = scala.collection.mutable.ListBuffer.empty[RejectRow]
    val stage03Rows =
      loadOrBuildStage03Rule(
        config = config,
        ws = ws,
        stage02Rows = stage02Rows,
        stage03Path = stage03Path,
        rejectRows = rejectRows
      )
    println(s"[strategic-prepolish] stage03 rule-mode move analyses=${stage03Rows.size}")

    val stage03BySeed = stage03Rows.groupBy(_.seedId)
    val candidateDocs =
      stage02Rows.flatMap { row =>
        val moveRows =
          stage03BySeed
            .getOrElse(row.source.seedId, Nil)
            .filter(mr => row.rootAnalysis.candidateMoves.exists(_.uci == mr.move.uci))
        if moveRows.isEmpty then
          rejectRows += RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "prepolish_family", List("no_move_analyses"))
          None
        else
          chooseDominantFamily(moveRows) match
            case None =>
              rejectRows += RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "prepolish_family", List("no_robust_family"))
              None
            case Some(family) =>
              val (accepted, partial, alternate) = assignCredits(family.summary, moveRows)
              if accepted.isEmpty || accepted.size > MaxAcceptedMoves then
                rejectRows += RejectRow(
                  row.source.seedId,
                  row.position.fen,
                  row.position.fenKey,
                  "prepolish_credit_assignment",
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
                    generationMeta = GenerationMeta(isoNow(), s"${RunnerVersion}-prepolish", "pre_polish_candidate")
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
      selection.autoPublish.map(doc => doc.copy(generationMeta = doc.generationMeta.copy(selectionStatus = "pre_polish_top_1000")))
    val reserve =
      selection.reserve.map(doc => doc.copy(generationMeta = doc.generationMeta.copy(selectionStatus = "pre_polish_reserve")))

    writeJsonl(candidatePath, scoredDocs)
    writeJsonl(publishPath, publish)
    writeJsonl(reservePath, reserve)
    writeJsonl(rejectPath, rejectRows.distinctBy(rr => s"${rr.seedId}|${rr.stage}|${rr.reasons.mkString(",")}"))

    val report =
      Json.obj(
        "schema" -> "chesstory.strategicPuzzle.prePolishReport.v1",
        "generatedAt" -> isoNow(),
        "config" -> Json.obj(
          "inputPath" -> config.inputPath.toString,
          "outDir" -> config.outDir.toString,
          "limit" -> config.limit,
          "resume" -> config.resume,
          "evalEndpoint" -> config.evalEndpoint
        ),
        "counts" -> Json.obj(
          "stage02Input" -> stage02Rows.size,
          "stage03RuleMoveAnalyses" -> stage03Rows.size,
          "candidateDocs" -> scoredDocs.size,
          "prePolishTop1000" -> publish.size,
          "prePolishReserve" -> reserve.size,
          "rejectRows" -> rejectRows.size
        ),
        "acceptance" -> Json.obj(
          "duplicateFenCandidates" -> scoredDocs.groupBy(_.position.fenKey).count(_._2.size > 1),
          "duplicateIdCandidates" -> scoredDocs.groupBy(_.id).count(_._2.size > 1),
          "acceptedMoveExplanationFailures" -> scoredDocs.flatMap(_.acceptedMoves).count(!_.explanation.passed),
          "dominantFamilyMissing" -> scoredDocs.count(_.dominantFamily.key.trim.isEmpty)
        )
      )
    writeJson(reportPath, report)
    println(s"[strategic-prepolish] candidate docs=${scoredDocs.size}, top1000=${publish.size}, reserve=${reserve.size}")
    println(s"[strategic-prepolish] wrote artifacts under ${config.outDir}")

  private def loadStage02Rows(config: Config): List[Stage02RootAnalysisRow] =
    if !Files.exists(config.inputPath) then
      throw new IllegalArgumentException(s"input file does not exist: ${config.inputPath}")
    readJsonl[Stage02RootAnalysisRow](config.inputPath)
      .fold(err => throw new IllegalArgumentException(err), identity)
      .pipe(rows => config.limit.fold(rows)(limit => rows.take(limit)))

  private def loadOrBuildStage03Rule(
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
                  allowLlmPolish = false,
                  lang = "en"
                ),
                120.seconds
              )
            resultOpt match
              case None =>
                rejectRows += RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "prepolish_move", List(s"empty_bookmaker_response:${move.uci}"))
              case Some(result) =>
                val response = result.response
                val stage03Row =
                  Stage03MoveAnalysisRow(
                    seedId = row.source.seedId,
                    fenKey = row.position.fenKey,
                    move = move,
                    family = prePolishFamilySummary(response, move.cpLoss),
                    explanation = prePolishExplanationGate(response),
                    response = responseJson(response),
                    generatedAt = isoNow()
                  )
                appendJsonl(stage03Path, stage03Row)
                built += stage03Row
          catch
            case NonFatal(err) =>
              rejectRows += RejectRow(row.source.seedId, row.position.fen, row.position.fenKey, "prepolish_move", List(s"${move.uci}:${err.getMessage}"))
      }
    }
    built.toList

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
    val defaultInput = detectDefaultInput(workspaceRoot)
    val defaults =
      Config(
        inputPath = defaultInput,
        outDir = workspaceRoot.resolve(Path.of("tools", "strategic_puzzles", "runs", s"prepolish_${runDirName()}")),
        limit = None,
        resume = false,
        evalEndpoint = sys.env.get("ACCOUNTINTEL_EVAL_ENDPOINT").orElse(sys.env.get("EXTERNAL_ENGINE_ENDPOINT")).getOrElse("http://localhost:9666")
      )

    @annotation.tailrec
    def loop(rest: List[String], cfg: Config): Either[String, Config] =
      rest match
        case Nil => Right(cfg)
        case "--resume" :: tail => loop(tail, cfg.copy(resume = true))
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

  private def detectDefaultInput(workspaceRoot: Path): Path =
    val runsDir = workspaceRoot.resolve(Path.of("tools", "strategic_puzzles", "runs"))
    val default = runsDir.resolve(Path.of("prellm_full_20260318", "stage02_pre_llm_survivors.jsonl"))
    if Files.exists(default) then default
    else
      Option.when(Files.isDirectory(runsDir)) {
        Files.list(runsDir).toArray.map(_.asInstanceOf[Path]).toList
      }.getOrElse(Nil)
        .filter(path => Files.isDirectory(path) && path.getFileName.toString.startsWith("prellm_full_"))
        .sortBy(_.getFileName.toString)
        .reverse
        .map(_.resolve("stage02_pre_llm_survivors.jsonl"))
        .find(Files.exists(_))
        .getOrElse(default)

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
