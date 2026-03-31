package lila.llm.tools.active

import java.nio.file.{ Files, Path, Paths }
import java.time.Instant

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.*
import scala.util.Using
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import play.api.libs.json.Json
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.*
import lila.llm.analysis.{ CommentaryOpsBoard, OpeningExplorerClient }

object ActiveNarrativeCorpusRunner:

  import ActiveNarrativeCorpusSupport.*

  private val DefaultCorpusPath = Paths.get("modules/llm/docs/ActiveNarrativePlayerCorpus_20260311.json")
  private val DefaultEvalCachePath = Paths.get("modules/llm/docs/ActiveNarrativePlayerCorpus_20260311.evals.json")
  private val DefaultReportMarkdownPath = Paths.get("modules/llm/docs/ActiveNarrativeCorpusReport.latest.md")
  private val DefaultReportJsonPath = Paths.get("modules/llm/docs/ActiveNarrativeCorpusReport.latest.json")
  private val DefaultRawResponseDir = Paths.get("modules/llm/docs/active_narrative_runs/latest/responses")

  final case class Config(
      corpusPath: Path,
      evalCachePath: Path,
      markdownPath: Path,
      jsonPath: Path,
      rawResponseDir: Path,
      limit: Option[Int],
      style: String = "active",
      focusOn: List[String] = List("strategy", "long_plan", "piece_route"),
      allowLlmPolish: Boolean = true,
      asyncTier: Boolean = false,
      lang: String = "en",
      planTier: String = PlanTier.Pro,
      llmLevel: String = LlmLevel.Active,
      retryCount: Int = 1,
      activeNoteProvider: Option[String] = None,
      openAiActiveSyncModel: Option[String] = None,
      openAiActiveAsyncModel: Option[String] = None,
      openAiActiveFallbackModel: Option[String] = None,
      openAiActiveReasoningEffort: Option[String] = None,
      geminiActiveModel: Option[String] = None
  )

  final case class AnalysisArtifacts(
      response: GameChronicleResponse,
      latencyMs: Long,
      openingExplorerAdvisories: Int
  )

  trait NarrativeAnalyzer:
    def providerLabel: String
    def analyze(entry: CorpusEntry, evals: List[MoveEval]): Future[AnalysisArtifacts]
    def commentaryOpsSnapshot: Option[CommentaryOpsBoard.Snapshot] = None

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("active-narrative-corpus-runner")

    val config = parseConfig(args.toList)
    val providerConfig = LlmProviderConfig.fromEnv
    if providerConfig.isNone then
      System.err.println("[active-corpus-runner] `LLM_PROVIDER` resolved to `none`; cannot run Pro+Active benchmark.")
      sys.exit(1)

    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    val analyzer =
      LiveNarrativeAnalyzer(
        ws = ws,
        providerConfig = providerConfig,
        config = config
      )

    try
      val report = Await.result(execute(config, analyzer), Duration.Inf)
      println(
        s"[active-corpus-runner] wrote `${config.markdownPath}` and `${config.jsonPath}` (games=${report.games.size}, succeeded=${report.summary.succeeded}, failed=${report.summary.failed})"
      )
    finally
      ws.close()
      summon[ActorSystem].terminate()

  def execute(
      config: Config,
      analyzer: NarrativeAnalyzer
  )(using Executor): Future[RunReport] =
    val corpus =
      readCorpus(config.corpusPath) match
        case Right(value) => value
        case Left(err)    => throw new IllegalArgumentException(s"failed to read corpus `${config.corpusPath}`: $err")

    val evalCache =
      readEvalCache(config.evalCachePath) match
        case Right(value) => value
        case Left(err)    => throw new IllegalArgumentException(s"failed to read eval cache `${config.evalCachePath}`: $err")

    val entries = flattenCorpus(corpus).take(config.limit.getOrElse(Int.MaxValue))
    if entries.isEmpty then
      throw new IllegalArgumentException(s"no games selected from `${config.corpusPath}`")

    val selectedKeys = entries.map(_.gameKey).toSet
    val scopedEvalCache =
      if config.limit.isDefined then evalCache.copy(games = evalCache.games.filter(game => selectedKeys.contains(game.gameKey)))
      else evalCache

    val evalsByKey =
      alignEvalCache(entries, scopedEvalCache) match
        case Right(value) => value
        case Left(errors) =>
          throw new IllegalArgumentException(errors.mkString("; "))

    prepareOutputs(config.rawResponseDir)

    entries
      .foldLeft(Future.successful(List.empty[GameReport])) { (accFut, entry) =>
        accFut.flatMap { acc =>
          val evals = evalsByKey(entry.gameKey).evals
          runSingleGame(entry, evals, analyzer, config).map(report => acc :+ report)
        }
      }
      .map { reports =>
        val runReport =
          RunReport(
            generatedAt = Instant.now().toString,
            corpusTitle = corpus.title,
            corpusAsOfDate = corpus.asOfDate,
            corpusGeneratedAt = corpus.generatedAt,
            provider = analyzer.providerLabel,
            planTier = config.planTier,
            llmLevel = config.llmLevel,
            totalCorpusGames = entries.size,
            summary = buildSummary(reports),
            games = reports,
            opsSnapshot = analyzer.commentaryOpsSnapshot
          )
        writeJson(config.jsonPath, Json.toJson(runReport))
        writeText(config.markdownPath, renderMarkdown(runReport))
        runReport
      }

  private def runSingleGame(
      entry: CorpusEntry,
      evals: List[MoveEval],
      analyzer: NarrativeAnalyzer,
      config: Config
  )(using Executor): Future[GameReport] =
    def attempt(remainingRetries: Int, retried: Boolean): Future[GameReport] =
      analyzer
        .analyze(entry, evals)
        .flatMap { artifacts =>
          writeResponseSnapshot(entry, artifacts.response, config.rawResponseDir).map { responsePath =>
            buildSuccessReport(
              entry = entry,
              response = artifacts.response,
              latencyMs = artifacts.latencyMs,
              provider = analyzer.providerLabel,
              openingExplorerAdvisories = artifacts.openingExplorerAdvisories,
              retryUsed = retried,
              responsePath = responsePath
            )
          }
        }
        .recoverWith { case NonFatal(e) =>
          if remainingRetries > 0 then attempt(remainingRetries - 1, retried = true)
          else
            Future.successful(
              buildFailureReport(
                entry = entry,
                provider = analyzer.providerLabel,
                retryUsed = retried,
                error = Option(e.getMessage).getOrElse(e.getClass.getSimpleName)
              )
            )
        }

    attempt(config.retryCount, retried = false)

  private def buildSuccessReport(
      entry: CorpusEntry,
      response: GameChronicleResponse,
      latencyMs: Long,
      provider: String,
      openingExplorerAdvisories: Int,
      retryUsed: Boolean,
      responsePath: String
  ): GameReport =
    val activeNoteMoments = response.moments.filter(moment => nonEmpty(moment.activeStrategicNote))
    val activeNoteCount = activeNoteMoments.size
    val selectedStrategicMoments = response.moments.count(_.strategicBranch)
    val threadedMoments = response.moments.count(_.strategicThread.isDefined)
    val stageCoverage = orderedStages(response.moments.flatMap(_.strategicThread.map(_.stageKey)))
    val noteMetadataComplete = activeNoteMoments.forall(moment => dossierHasThreadMetadata(moment.activeBranchDossier))
    val review = response.review
    val warnings =
      List(
        Option.when(response.strategicThreads.isEmpty)("no thread"),
        Option.when(response.moments.forall(_.selectionKind != "thread_bridge"))("no bridge"),
        Option.when(activeNoteCount == 0)("no active note"),
        Option.when(openingExplorerAdvisories > 0)(s"opening fetch advisory x$openingExplorerAdvisories"),
        Option.when(retryUsed)("retry used")
      ).flatten

    GameReport(
      gameKey = entry.gameKey,
      player = entry.player,
      opponent = entry.game.opponent,
      event = entry.game.event,
      date = entry.game.date,
      result = entry.game.result,
      selectionRank = entry.game.selectionRank,
      totalPlies = entry.totalPlies,
      provider = provider,
      sourceMode = response.sourceMode,
      model = response.model,
      latencyMs = latencyMs,
      totalMoments = response.moments.size,
      selectedStrategicMoments = selectedStrategicMoments,
      threadedMoments = threadedMoments,
      threadCount = response.strategicThreads.size,
      threads =
        response.strategicThreads.map { thread =>
          ThreadSummary(
            themeKey = thread.themeKey,
            themeLabel = thread.themeLabel,
            representativePlies = thread.representativePlies,
            continuityScore = thread.continuityScore
          )
        },
      stageCoverage = stageCoverage,
      bridgeMomentCount = response.moments.count(_.selectionKind == "thread_bridge"),
      activeNoteCount = activeNoteCount,
      activeNoteAttachmentRatio =
        if selectedStrategicMoments == 0 then 0.0 else activeNoteCount.toDouble / selectedStrategicMoments.toDouble,
      allAttachedNotesHaveThreadMetadata = noteMetadataComplete,
      openingExplorerAdvisories = openingExplorerAdvisories,
      warnings = warnings,
      retryUsed = retryUsed,
      hardFailure = false,
      error = None,
      responsePath = Some(responsePath),
      threadSummaries = response.strategicThreads.map(_.summary),
      spotlightNotes =
        activeNoteMoments.map { moment =>
          SpotlightNoteSummary(
            themeLabel = moment.activeBranchDossier.flatMap(_.threadLabel),
            stageLabel = moment.activeBranchDossier.flatMap(_.threadStage),
            threadSummary = moment.activeBranchDossier.flatMap(_.threadSummary),
            noteExcerpt = excerpt(moment.activeStrategicNote.getOrElse(""))
          )
        },
      internalMomentCount = review.map(_.internalMomentCount),
      visibleMomentCount = review.map(_.visibleMomentCount),
      polishedMomentCount = review.map(_.polishedMomentCount),
      visibleStrategicMomentCount = review.map(_.visibleStrategicMomentCount),
      visibleBridgeMomentCount = review.map(_.visibleBridgeMomentCount)
    )

  private def buildFailureReport(
      entry: CorpusEntry,
      provider: String,
      retryUsed: Boolean,
      error: String
  ): GameReport =
    GameReport(
      gameKey = entry.gameKey,
      player = entry.player,
      opponent = entry.game.opponent,
      event = entry.game.event,
      date = entry.game.date,
      result = entry.game.result,
      selectionRank = entry.game.selectionRank,
      totalPlies = entry.totalPlies,
      provider = provider,
      sourceMode = "failed",
      model = None,
      latencyMs = -1L,
      totalMoments = 0,
      selectedStrategicMoments = 0,
      threadedMoments = 0,
      threadCount = 0,
      threads = Nil,
      stageCoverage = Nil,
      bridgeMomentCount = 0,
      activeNoteCount = 0,
      activeNoteAttachmentRatio = 0.0,
      allAttachedNotesHaveThreadMetadata = true,
      openingExplorerAdvisories = 0,
      warnings = List("hard failure") ++ Option.when(retryUsed)("retry used"),
      retryUsed = retryUsed,
      hardFailure = true,
      error = Some(error),
      responsePath = None
    )

  private def writeResponseSnapshot(
      entry: CorpusEntry,
      response: GameChronicleResponse,
      rawResponseDir: Path
  ): Future[String] =
    val out = rawResponseDir.resolve(s"${entry.gameKey}.json")
    val payload = Json.obj(
      "gameKey" -> entry.gameKey,
      "player" -> entry.player,
      "selectionRank" -> entry.game.selectionRank,
      "response" -> Json.toJson(response)
    )
    Future.successful {
      writeJson(out, payload)
      out.toAbsolutePath.normalize.toString
    }

  private def prepareOutputs(rawResponseDir: Path): Unit =
    ensureParent(rawResponseDir.resolve("placeholder"))
    if Files.exists(rawResponseDir) then
      Using.resource(Files.list(rawResponseDir)) { stream =>
        stream.forEach { path =>
          if Files.isRegularFile(path) && path.getFileName.toString.toLowerCase.endsWith(".json") then
            Files.deleteIfExists(path)
        }
      }

  private def dossierHasThreadMetadata(dossier: Option[ActiveBranchDossier]): Boolean =
    dossier.exists { d =>
      nonEmpty(d.threadLabel) &&
      nonEmpty(d.threadStage) &&
      nonEmpty(d.threadSummary)
    }

  private def nonEmpty(value: Option[String]): Boolean =
    value.exists(_.trim.nonEmpty)

  private def excerpt(text: String, maxChars: Int = 180): String =
    val cleaned =
      Option(text)
        .getOrElse("")
        .replaceAll("""\s+""", " ")
        .trim
    if cleaned.length <= maxChars then cleaned
    else cleaned.take(maxChars - 1).trim + "…"

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    Config(
      corpusPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultCorpusPath),
      evalCachePath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultEvalCachePath),
      markdownPath = optionString(args, "--markdown").map(Paths.get(_)).getOrElse(DefaultReportMarkdownPath),
      jsonPath = optionString(args, "--json").map(Paths.get(_)).getOrElse(DefaultReportJsonPath),
      rawResponseDir = optionString(args, "--raw-dir").map(Paths.get(_)).getOrElse(DefaultRawResponseDir),
      limit = optionInt(args, "--limit").filter(_ > 0),
      activeNoteProvider = optionString(args, "--active-provider").map(_.trim.toLowerCase).filter(_.nonEmpty),
      openAiActiveSyncModel = optionString(args, "--openai-active-sync"),
      openAiActiveAsyncModel = optionString(args, "--openai-active-async"),
      openAiActiveFallbackModel = optionString(args, "--openai-active-fallback"),
      openAiActiveReasoningEffort = optionString(args, "--openai-active-effort").map(_.trim.toLowerCase).filter(_.nonEmpty),
      geminiActiveModel = optionString(args, "--gemini-active-model")
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue =
      Set(
        "--markdown",
        "--json",
        "--raw-dir",
        "--limit",
        "--active-provider",
        "--openai-active-sync",
        "--openai-active-async",
        "--openai-active-fallback",
        "--openai-active-effort",
        "--gemini-active-model"
      )
    val out = scala.collection.mutable.ListBuffer.empty[String]
    var idx = 0
    while idx < args.length do
      val current = args(idx)
      if current.startsWith("--") then idx += (if optionsWithValue.contains(current) then 2 else 1)
      else
        out += current
        idx += 1
    out.toList

  private def optionString(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst {
      case List(flag, value) if flag == name => value
    }.map(_.trim).filter(_.nonEmpty)

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)

  private final class LiveNarrativeAnalyzer(
      ws: StandaloneAhcWSClient,
      providerConfig: LlmProviderConfig,
      config: Config
  )(using executor: Executor)
      extends NarrativeAnalyzer:

    override val providerLabel: String = providerConfig.provider

    private val advisoryProbe = OpeningExplorerAdvisoryProbe(ws)(using executor)
    private val resolvedProviderConfig =
      config.activeNoteProvider match
        case Some(provider) => providerConfig.copy(providerActiveNote = provider)
        case None           => providerConfig
    private val openAiConfig =
      val base = OpenAiConfig.fromEnv
      base.copy(
        modelActiveSync = config.openAiActiveSyncModel.getOrElse(base.modelActiveSync),
        modelActiveAsync = config.openAiActiveAsyncModel.getOrElse(base.modelActiveAsync),
        modelActiveFallback = config.openAiActiveFallbackModel.getOrElse(base.modelActiveFallback),
        reasoningEffortActive = config.openAiActiveReasoningEffort.getOrElse(base.reasoningEffortActive)
      )
    private val geminiConfig =
      val base = GeminiConfig.fromEnv
      base.copy(
        modelActive = config.geminiActiveModel.getOrElse(base.modelActive)
      )
    private val api =
      LlmApi(
        openingExplorer = OpeningExplorerClient(ws)(using executor),
        geminiClient = GeminiClient(ws, geminiConfig)(using executor),
        openAiClient = OpenAiClient(ws, openAiConfig)(using executor),
        commentaryCache = CommentaryCache(),
        llmConfig = LlmConfig.fromEnv,
        providerConfig = resolvedProviderConfig
      )(using executor)

    override def commentaryOpsSnapshot: Option[CommentaryOpsBoard.Snapshot] =
      Some(api.commentaryOpsSnapshot(limit = 50))

    override def analyze(entry: CorpusEntry, evals: List[MoveEval]): Future[AnalysisArtifacts] =
      for
        advisories <- advisoryProbe.countForPgn(entry.game.pgn)
        startedAt = System.nanoTime()
        responseOpt <- api.analyzeGameChronicleLocal(
          pgn = entry.game.pgn,
          evals = evals,
          style = config.style,
          focusOn = config.focusOn,
          allowLlmPolish = config.allowLlmPolish,
          asyncTier = config.asyncTier,
          lang = config.lang,
          planTier = config.planTier,
          llmLevel = config.llmLevel,
          disablePolishCircuit = true
        )
        latencyMs = (System.nanoTime() - startedAt) / 1000000L
        response <- Future.fromTry(
          scala.util.Try(responseOpt.getOrElse(throw new IllegalStateException("Narrative Analysis unavailable")))
        )
      yield AnalysisArtifacts(
        response = response,
        latencyMs = latencyMs,
        openingExplorerAdvisories = advisories
      )

  private object OpeningExplorerAdvisoryProbe:
    private val OpeningRefMinPly = 3
    private val OpeningRefMaxPly = 24

    def apply(ws: StandaloneAhcWSClient)(using Executor): OpeningExplorerAdvisoryProbe =
      new OpeningExplorerAdvisoryProbe(ws)

  private final class OpeningExplorerAdvisoryProbe(ws: StandaloneAhcWSClient)(using Executor):
    private val explorerBase =
      sys.env
        .get("EXPLORER_API_BASE")
        .map(_.trim)
        .filter(_.nonEmpty)
        .getOrElse("https://explorer.lichess.org")
        .stripSuffix("/")
    private val authHeaders =
      sys.env
        .get("LICHESS_EXPLORER_TOKEN")
        .orElse(sys.env.get("LICHESS_API_TOKEN"))
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(token => Seq("Authorization" -> s"Bearer $token"))
        .getOrElse(Seq.empty)
    private val cache = scala.collection.concurrent.TrieMap.empty[String, Future[Int]]

    def countForPgn(pgn: String): Future[Int] =
      val openingFens =
        PgnAnalysisHelper.extractPlyData(pgn).toOption.toList.flatMap { plyData =>
          plyData.collect {
            case pd
                if pd.ply >= OpeningExplorerAdvisoryProbe.OpeningRefMinPly &&
                  pd.ply <= OpeningExplorerAdvisoryProbe.OpeningRefMaxPly =>
              normalizeFen(pd.fen)
          }.distinct
        }
      Future.traverse(openingFens)(probeFen).map(_.sum)

    private def probeFen(fen: String): Future[Int] =
      cache.getOrElseUpdate(
        fen,
        ws.url(s"$explorerBase/masters")
          .addHttpHeaders(authHeaders*)
          .withQueryStringParameters("fen" -> fen)
          .withRequestTimeout(2.seconds)
          .get()
          .map { response =>
            if response.status == 200 || response.status == 404 then 0 else 1
          }
          .recover { case _ => 1 }
      )

    private def normalizeFen(fen: String): String =
      fen.trim.split("\\s+").toList match
        case a :: b :: c :: d :: _ => s"$a $b $c $d 0 1"
        case _                     => fen.trim
