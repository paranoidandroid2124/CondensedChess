package lila.llm.tools

import java.nio.file.{ Files, Path, Paths }

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.DurationInt

import munit.FunSuite
import play.api.libs.json.Json

import lila.llm.*
import lila.llm.analysis.CommentaryOpsBoard

class ActiveNarrativeCorpusToolsTest extends FunSuite:

  import ActiveNarrativeCorpusSupport.*

  given Executor = ExecutionContext.global

  test("eval cache schema aligns with fixture corpus") {
    val corpus = readCorpus(resourcePath("active_narrative_fixture_corpus.json")).fold(fail(_), identity)
    val evalCache = readEvalCache(resourcePath("active_narrative_fixture_evals.json")).fold(fail(_), identity)

    val entries = flattenCorpus(corpus)
    assertEquals(entries.size, 2)
    val aligned = alignEvalCache(entries, evalCache).fold(errs => fail(errs.mkString("; ")), identity)
    assertEquals(aligned.keySet, entries.map(_.gameKey).toSet)

    val tmp = Files.createTempFile("active-narrative-evals-roundtrip-", ".json")
    writeJson(tmp, Json.toJson(evalCache))
    val reparsed = readEvalCache(tmp).fold(fail(_), identity)
    assertEquals(reparsed.gameCount, 2)
    assertEquals(reparsed.engine.name, "fixture-engine")
  }

  test("spotlight selection is fixed to ranks 1, 5, 10 per player") {
    val players = List("Abdusattorov, Nodirbek", "Carlsen, Magnus", "Caruana, Fabiano")
    val entries =
      players.flatMap { player =>
        (1 to 10).toList.map(rank => syntheticEntry(player, rank))
      }

    val selected = spotlightEntries(entries)
    assertEquals(selected.size, 9)
    assertEquals(selected.groupBy(_.player).view.mapValues(_.map(_.game.selectionRank).sorted).toMap,
      Map(
        "Abdusattorov, Nodirbek" -> List(1, 5, 10),
        "Carlsen, Magnus" -> List(1, 5, 10),
        "Caruana, Fabiano" -> List(1, 5, 10)
      )
    )
  }

  test("renderMarkdown remains stable when thread and note coverage are empty") {
    val report =
      RunReport(
        generatedAt = "2026-03-11T00:00:00Z",
        corpusTitle = "Fixture",
        corpusAsOfDate = "2026-03-11",
        corpusGeneratedAt = "2026-03-11T00:00:00Z",
        provider = "fixture",
        planTier = PlanTier.Pro,
        llmLevel = LlmLevel.Active,
        totalCorpusGames = 1,
        summary =
          RunSummary(
            totalGames = 1,
            succeeded = 1,
            failed = 0,
            providerDistribution = Map("fixture" -> 1),
            modelDistribution = Map.empty,
            sourceModeDistribution = Map("rule" -> 1),
            avgLatencyMs = 12.0,
            p95LatencyMs = 12.0,
            avgNarrativeMoments = 1.0,
            avgStrategicThreadsPerGame = 0.0,
            gamesWithThreadPct = 0.0,
            avgThreadedMomentsPerGame = 0.0,
            gamesWithBridgePct = 0.0,
            avgBridgeMomentsPerGame = 0.0,
            gamesWithActiveNotePct = 0.0,
            avgActiveNotesPerGame = 0.0,
            threadThemeDistribution = Map.empty,
            stageCoverageDistribution = Map.empty,
            dossierThreadMetadataCoverage = 1.0,
            openingExplorerAdvisoryCount = 0,
            retryCount = 0,
            hardFailureCount = 0
          ),
        games =
          List(
            GameReport(
              gameKey = "fixture-game",
              player = "Carlsen, Magnus",
              opponent = "Opponent, One",
              event = "Fixture Event",
              date = "2026-01-01",
              result = "1-0",
              selectionRank = 1,
              totalPlies = 4,
              provider = "fixture",
              sourceMode = "rule",
              model = None,
              latencyMs = 12,
              totalMoments = 1,
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
              warnings = List("no thread", "no bridge", "no active note"),
              retryUsed = false,
              hardFailure = false,
              error = None,
              responsePath = Some("fixture-game.json")
            )
          ),
        opsSnapshot = FixtureAnalyzer.commentaryOpsSnapshot
      )

    val markdown = renderMarkdown(report)
    assert(markdown.contains("## Run Summary"))
    assert(markdown.contains("## LLM Ops Snapshot"))
    assert(markdown.contains("### Prompt Usage"))
    assert(markdown.contains("## Per-Game Scorecard"))
    assert(markdown.contains("no thread, no bridge, no active note"))
    assert(markdown.contains("Spotlight Review"))
    assert(markdown.contains("active model"))
    assert(markdown.contains("gpt-5.2"))
  }

  test("runner writes raw responses, json report, and markdown report for fixture corpus") {
    val tempDir = Files.createTempDirectory("active-narrative-runner-fixture-")
    val markdownPath = tempDir.resolve("report.md")
    val jsonPath = tempDir.resolve("report.json")
    val rawDir = tempDir.resolve("responses")

    val report = Await.result(
      ActiveNarrativeCorpusRunner.execute(
        config =
          ActiveNarrativeCorpusRunner.Config(
            corpusPath = resourcePath("active_narrative_fixture_corpus.json"),
            evalCachePath = resourcePath("active_narrative_fixture_evals.json"),
            markdownPath = markdownPath,
            jsonPath = jsonPath,
            rawResponseDir = rawDir,
            limit = None
          ),
        analyzer = FixtureAnalyzer
      ),
      30.seconds
    )

    assertEquals(report.summary.totalGames, 2)
    assertEquals(report.summary.succeeded, 2)
    assert(Files.exists(markdownPath))
    assert(Files.exists(jsonPath))
    assert(Files.isDirectory(rawDir))
    assertEquals(countJsonFiles(rawDir), 2)
    assert(report.opsSnapshot.nonEmpty)

    val reportJson = Files.readString(jsonPath)
    val reportMarkdown = Files.readString(markdownPath)

    assert(reportJson.contains("carlsen-magnus-2026-01-01-1-carlsen-magnus-opponent-one"))
    assert(reportJson.contains("caruana-fabiano-2026-01-02-2-opponent-two-caruana-fabiano"))
    assert(reportJson.contains("\"promptUsage\""))
    assert(reportJson.contains("\"warningReasons\""))
    assert(reportJson.contains("\"configuredModel\""))
    assert(reportJson.contains("gpt-5.2"))
    assert(reportMarkdown.contains("Spotlight Review"))
    assert(reportMarkdown.contains("Rook Lift Attack"))
    assert(reportMarkdown.contains("fullgame_moment_polish"))
    assert(reportMarkdown.contains("warning reasons"))
    assert(reportMarkdown.contains("active observed models"))
    assertEquals(report.opsSnapshot.flatMap(_.active.configuredModel), Some("gpt-5.2"))

    val threadGame = report.games.find(_.player == "Carlsen, Magnus").getOrElse(fail("missing Carlsen fixture game"))
    assertEquals(threadGame.threadCount, 1)
    assertEquals(threadGame.bridgeMomentCount, 1)
    assertEquals(threadGame.activeNoteCount, 3)
    assert(threadGame.allAttachedNotesHaveThreadMetadata)

    val plainGame = report.games.find(_.player == "Caruana, Fabiano").getOrElse(fail("missing Caruana fixture game"))
    assertEquals(plainGame.threadCount, 0)
    assertEquals(plainGame.bridgeMomentCount, 0)
    assertEquals(plainGame.activeNoteCount, 0)
    assert(plainGame.warnings.exists(_.contains("opening fetch advisory")))
  }

  test("runner limit=1 restricts execution to a single game") {
    val tempDir = Files.createTempDirectory("active-narrative-runner-limit-")
    val report = Await.result(
      ActiveNarrativeCorpusRunner.execute(
        config =
          ActiveNarrativeCorpusRunner.Config(
            corpusPath = resourcePath("active_narrative_fixture_corpus.json"),
            evalCachePath = resourcePath("active_narrative_fixture_evals.json"),
            markdownPath = tempDir.resolve("report.md"),
            jsonPath = tempDir.resolve("report.json"),
            rawResponseDir = tempDir.resolve("responses"),
            limit = Some(1)
          ),
        analyzer = FixtureAnalyzer
      ),
      30.seconds
    )

    assertEquals(report.games.size, 1)
    assertEquals(report.summary.totalGames, 1)
    assertEquals(report.games.head.player, "Carlsen, Magnus")
  }

  private object FixtureAnalyzer extends ActiveNarrativeCorpusRunner.NarrativeAnalyzer:
    override val providerLabel: String = "fixture"

    override val commentaryOpsSnapshot: Option[CommentaryOpsBoard.Snapshot] =
      Some(
        CommentaryOpsBoard.Snapshot(
          generatedAtMs = 0L,
          bookmaker =
            CommentaryOpsBoard.BookmakerMetrics(
              requests = 0L,
              polishAttempts = 0L,
              polishAccepted = 0L,
              polishFallbackRate = 0.0,
              softRepairAnyRate = 0.0,
              softRepairMaterialRate = 0.0,
              compareObserved = 0L,
              compareConsistencyRate = 1.0,
              avgCostUsd = 0.0
            ),
          fullgame =
            CommentaryOpsBoard.FullGameMetrics(
              compareObserved = 2L,
              compareConsistencyRate = 1.0,
              repairAttempts = 4L,
              repairBypassed = 1L,
              softRepairApplied = 2L,
              mergedRetrySkipped = 1L,
              invalidReasonCounts = Map("segment_primary:anchor_missing" -> 2L)
            ),
          active =
            CommentaryOpsBoard.ActiveMetrics(
              selectedMoments = 3L,
              attempts = 3L,
              attached = 3L,
              omitted = 0L,
              primaryAccepted = 2L,
              repairAttempts = 1L,
              repairRecovered = 1L,
              attachRate = 1.0,
              thesisAgreementRate = 1.0,
              dossierAttachRate = 1.0,
              dossierCompareRate = 1.0,
              dossierRouteRefRate = 1.0,
              dossierReferenceFailureRate = 0.0,
              provider = Some("openai"),
              configuredModel = Some("gpt-5.2"),
              fallbackModel = Some("gpt-5-mini"),
              reasoningEffort = Some("none"),
              observedModelDistribution = Map("gpt-5.2-2025-12-11" -> 3L),
              omitReasons = Map.empty,
              warningReasons = Map("active_move_label_missing" -> 2L),
              routeRedeployCount = 3L,
              routeMoveRefCount = 1L,
              routeHiddenSafetyCount = 1L,
              routeTowardOnlyCount = 2L,
              routeExactSurfaceCount = 0L,
              routeOpponentHiddenCount = 1L
            ),
          promptUsage =
            Map(
              "fullgame_moment_polish" ->
                CommentaryOpsBoard.PromptUsageMetrics(
                  attempts = 5L,
                  cacheHits = 2L,
                  promptTokens = 1200L,
                  cachedTokens = 400L,
                  completionTokens = 260L,
                  estimatedCostUsd = 0.018
                ),
              "active_note_polish" ->
                CommentaryOpsBoard.PromptUsageMetrics(
                  attempts = 3L,
                  cacheHits = 1L,
                  promptTokens = 900L,
                  cachedTokens = 250L,
                  completionTokens = 180L,
                  estimatedCostUsd = 0.014
                )
            ),
          recentSamples = Nil
        )
      )

    override def analyze(entry: CorpusEntry, evals: List[MoveEval]): Future[ActiveNarrativeCorpusRunner.AnalysisArtifacts] =
      val response =
        if entry.player == "Carlsen, Magnus" then threadedResponse
        else plainResponse
      val advisories = if entry.player == "Caruana, Fabiano" then 1 else 0
      Future.successful(
        ActiveNarrativeCorpusRunner.AnalysisArtifacts(
          response = response,
          latencyMs = 25L,
          openingExplorerAdvisories = advisories
        )
      )

    private val rookLiftThread =
      ActiveStrategicThread(
        threadId = "thread_1",
        side = "white",
        themeKey = "rook_lift_attack",
        themeLabel = "Rook Lift Attack",
        summary = "White lifts the rook and keeps the kingside attack coherent across the middlegame.",
        seedPly = 1,
        lastPly = 4,
        representativePlies = List(1, 3, 4),
        opponentCounterplan = Some("Black wants to trade the attacking rook before the files open."),
        continuityScore = 0.84
      )

    private def dossier(stage: String): ActiveBranchDossier =
      ActiveBranchDossier(
        dominantLens = "strategic",
        chosenBranchLabel = "rook lift attack",
        threadLabel = Some("Rook Lift Attack"),
        threadStage = Some(stage),
        threadSummary = Some(rookLiftThread.summary),
        threadOpponentCounterplan = rookLiftThread.opponentCounterplan
      )

    private def threadRef(stageKey: String, stageLabel: String): ActiveStrategicThreadRef =
      ActiveStrategicThreadRef(
        threadId = rookLiftThread.threadId,
        themeKey = rookLiftThread.themeKey,
        themeLabel = rookLiftThread.themeLabel,
        stageKey = stageKey,
        stageLabel = stageLabel
      )

    private def moment(
        ply: Int,
        momentType: String,
        narrative: String,
        selectionKind: String,
        selectionLabel: Option[String],
        selectionReason: Option[String],
        strategicThread: Option[ActiveStrategicThreadRef],
        strategicBranch: Boolean,
        activeNote: Option[String],
        dossier: Option[ActiveBranchDossier]
    ): GameChronicleMoment =
      GameChronicleMoment(
        momentId = s"ply_$ply",
        ply = ply,
        moveNumber = (ply + 1) / 2,
        side = if ply % 2 == 1 then "white" else "black",
        moveClassification = None,
        momentType = momentType,
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        narrative = narrative,
        selectionKind = selectionKind,
        selectionLabel = selectionLabel,
        selectionReason = selectionReason,
        concepts = Nil,
        variations = Nil,
        cpBefore = 0,
        cpAfter = 0,
        mateBefore = None,
        mateAfter = None,
        wpaSwing = None,
        strategicSalience = Some("High"),
        transitionType = None,
        transitionConfidence = None,
        activePlan = None,
        topEngineMove = None,
        collapse = None,
        strategyPack = None,
        signalDigest = None,
        probeRequests = Nil,
        authorQuestions = Nil,
        authorEvidence = Nil,
        mainStrategicPlans = Nil,
        latentPlans = Nil,
        whyAbsentFromTopMultiPV = Nil,
        strategicBranch = strategicBranch,
        activeStrategicNote = activeNote,
        activeStrategicSourceMode = activeNote.map(_ => "llm_polished"),
        activeStrategicRoutes = Nil,
        activeStrategicMoves = Nil,
        activeBranchDossier = dossier,
        strategicThread = strategicThread
      )

    private val threadedResponse =
      GameChronicleResponse(
        schema = GameChronicleResponse.schemaV6,
        intro = "Fixture threaded intro.",
        moments = List(
          moment(
            ply = 1,
            momentType = "OpeningTheoryEnds",
            narrative = "White signals the rook-lift idea early.",
            selectionKind = "key",
            selectionLabel = Some("Key Moment"),
            selectionReason = None,
            strategicThread = Some(threadRef("seed", "Seed")),
            strategicBranch = true,
            activeNote = Some("White starts the rook-lift campaign before Black has organized any kingside cover."),
            dossier = Some(dossier("Seed"))
          ),
          moment(
            ply = 3,
            momentType = "StrategicBridge",
            narrative = "The rook transfer becomes concrete and keeps the attack coordinated.",
            selectionKind = "thread_bridge",
            selectionLabel = Some("Campaign Bridge"),
            selectionReason = Some("fills build stage of Rook Lift Attack"),
            strategicThread = Some(threadRef("build", "Build")),
            strategicBranch = true,
            activeNote = Some("The bridge move matters because it turns the idea into a usable attacking file transfer."),
            dossier = Some(dossier("Build"))
          ),
          moment(
            ply = 4,
            momentType = "SustainedPressure",
            narrative = "The converted attack now points directly at the king.",
            selectionKind = "key",
            selectionLabel = Some("Key Moment"),
            selectionReason = None,
            strategicThread = Some(threadRef("convert", "Convert")),
            strategicBranch = true,
            activeNote = Some("The final phase is conversion: White has already lifted the rook and now cashes in with direct pressure."),
            dossier = Some(dossier("Convert"))
          )
        ),
        conclusion = "Fixture threaded conclusion.",
        themes = List("rook lift", "kingside attack"),
        review = None,
        sourceMode = "llm_polished",
        model = Some("fixture-model"),
        planTier = PlanTier.Pro,
        llmLevel = LlmLevel.Active,
        strategicThreads = List(rookLiftThread)
      )

    private val plainResponse =
      GameChronicleResponse(
        schema = GameChronicleResponse.schemaV6,
        intro = "Fixture plain intro.",
        moments =
          List(
            moment(
              ply = 2,
              momentType = "TensionPeak",
              narrative = "The position is balanced and no long campaign forms.",
              selectionKind = "key",
              selectionLabel = Some("Key Moment"),
              selectionReason = None,
              strategicThread = None,
              strategicBranch = false,
              activeNote = None,
              dossier = None
            )
          ),
        conclusion = "Fixture plain conclusion.",
        themes = List("balance"),
        review = None,
        sourceMode = "rule",
        model = Some("fixture-model"),
        planTier = PlanTier.Pro,
        llmLevel = LlmLevel.Active,
        strategicThreads = Nil
      )

  private def resourcePath(name: String): Path =
    Paths.get(getClass.getResource(s"/$name").toURI)

  private def countJsonFiles(dir: Path): Int =
    val stream = Files.list(dir)
    try stream.filter(path => path.getFileName.toString.endsWith(".json")).count().toInt
    finally stream.close()

  private def syntheticEntry(player: String, rank: Int): CorpusEntry =
    CorpusEntry(
      player = player,
      selectionNote = "synthetic",
      game =
        CorpusGame(
          selectionRank = rank,
          date = f"2026-01-${rank}%02d",
          round = rank.toString,
          event = "Synthetic Event",
          site = Some("Test"),
          sourceId = "synthetic",
          sourceEvent = "Synthetic Event",
          sourcePgnUrl = "https://example.invalid/synthetic.pgn",
          formatHint = "classical",
          white = player,
          black = "Synthetic Opponent",
          color = "white",
          opponent = "Synthetic Opponent",
          result = "1-0",
          playerResult = "win",
          eco = Some("A00"),
          opening = Some("Synthetic Opening"),
          variation = None,
          timeControl = None,
          variant = "Standard",
          pgn = "[Event \"Synthetic Event\"]\n[Site \"Test\"]\n[Date \"2026.01.01\"]\n[Round \"1\"]\n[White \"Synthetic\"]\n[Black \"Opponent\"]\n[Result \"1-0\"]\n\n1. e4 e5 1-0"
        )
    )
