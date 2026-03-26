package lila.llm.tools

import chess.*
import chess.format.Fen

import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant
import java.util.concurrent.{ LinkedBlockingQueue, TimeUnit }

import scala.concurrent.{ Await, ExecutionContext }
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import play.api.libs.json.*
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.*
import lila.llm.analysis.{ CommentaryEngine, CompensationContractMatcher, CompensationRecaptureGate, EarlyOpeningNarrationPolicy, NarrativeUtils, OpeningExplorerClient, StrategyPackSurface }
import lila.llm.model.{ FutureSnapshot, L1DeltaSnapshot, ProbeRequest, ProbeResult, TargetsDelta }
import lila.llm.model.strategic.VariationLine

object RealPgnNarrativeEvalRunner:

  private val DefaultCorpusPath = Paths.get("modules/llm/docs/RealPgnNarrativeEvalCorpus_20260316.json")
  private val DefaultMarkdownPath = Paths.get("modules/llm/docs/RealPgnNarrativeEvalReport.latest.md")
  private val DefaultJsonPath = Paths.get("modules/llm/docs/RealPgnNarrativeEvalReport.latest.json")
  private val DefaultRawDir = Paths.get("modules/llm/docs/real_pgn_eval/latest")
  private val DefaultDepth = 10
  private val DefaultMultiPv = 3
  private val EngineEnvVars = List("STOCKFISH_BIN", "LLM_ACTIVE_CORPUS_ENGINE_PATH")
  private val MaxProbeMoments = 3
  private val MaxFocusMoments = 3
  private[tools] val PositiveCompensationExemplars = Set(
    "2023_07_30_1_25_daneshvar_bardiya_iri_muradli_mahammad_aze_fide_endgame_heavy_35:24",
    "2024_03_01_1_12_veres_alex_novomeska_karin_lichess_broadcast_club_classical_64:31",
    "2024_03_01_1_12_veres_alex_novomeska_karin_lichess_broadcast_club_classical_64:35",
    "2024_10_23_4_1_khotenashvili_b_badelka_olga_twic_master_classical_30:31",
    "2024_10_23_4_1_khotenashvili_b_badelka_olga_twic_master_classical_30:37",
    "2024_10_23_4_1_khotenashvili_b_badelka_olga_twic_master_classical_30:51"
  )
  private val BorderlineExpectations = Map(
    "EVA01:17" -> true,
    "EVA02:73" -> false,
    "EVA02:75" -> false,
    "QID02:38" -> false
  )
  private val CompensationLexicon =
    List("compensation", "initiative", "line pressure", "delayed recovery", "return vector", "cash out")

  private[tools] final case class NegativeGuardSpec(
      id: String,
      label: String,
      family: String,
      targetPly: Int,
      pgn: String
  )

  private[tools] val NegativeGuards = List(
    NegativeGuardSpec(
      id = "TAT06",
      label = "Abdusattorov vs Gukesh, Tata Steel 2026 Round 6",
      family = "ruy_lopez_exchange",
      targetPly = 60,
      pgn =
        """[Event "Tata Steel Chess 2026 | Masters"]
          |[Site "Chess.com"]
          |[Date "2026.01.23"]
          |[Round "06"]
          |[White "Abdusattorov, Nodirbek"]
          |[Black "Gukesh D"]
          |[Result "1-0"]
          |[WhiteElo "2751"]
          |[BlackElo "2754"]
          |[TimeControl "40/7200:1800+30"]
          |[Link "https://www.chess.com/events/2026-tata-steel-chess-masters/06/Abdusattorov_Nodirbek-Gukesh_D"]
          |
          |1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 6. Bxc6 dxc6 7. d3 Nd7 8.
          |Nc3 O-O 9. Be3 Re8 10. a4 a5 11. Kh1 Bd6 12. Ne2 Nf8 13. Ng3 c5 14. Nd2 Ne6 15.
          |Nf5 Bf8 16. Nc4 f6 17. Qg4 Kh8 18. Qg3 b6 19. h4 g6 20. Nh6 Qd7 21. Ng4 Bg7 22.
          |f3 Ba6 23. b3 Rad8 24. h5 gxh5 25. Nh6 Bxc4 26. bxc4 Bxh6 27. Bxh6 Rg8 28. Qh2
          |Rg6 29. Qxh5 Nf4 30. Bxf4 exf4 31. g4 fxg3 32. Kg2 Qd4 33. Rae1 Kg7 34. e5 Rh6
          |35. Qg4+ Rg6 36. Qf5 Rg5 37. Qxf6+ 1-0""".stripMargin
    )
  )

  final case class Config(
      corpusPath: Path,
      markdownPath: Path,
      jsonPath: Path,
      rawDir: Path,
      depth: Int,
      multiPv: Int,
      enginePath: Path
  )

  final case class Corpus(
      version: Int,
      generatedAt: String,
      asOfDate: String,
      title: String,
      description: String,
      games: List[CorpusGame]
  )

  object Corpus:
    given OFormat[Corpus] = Json.format[Corpus]

  final case class CorpusGame(
      id: String,
      tier: String,
      family: String,
      label: String,
      notes: List[String],
      expectedThemes: List[String],
      pgn: String
  )

  object CorpusGame:
    given OFormat[CorpusGame] = Json.format[CorpusGame]

  final case class FocusMomentReport(
      ply: Int,
      moveNumber: Int,
      side: String,
      momentType: String,
      selectionKind: String,
      dominantIdea: Option[String],
      secondaryIdea: Option[String],
      campaignOwner: Option[String],
      ownerMismatch: Boolean,
      gameArcCompensationPosition: Boolean,
      bookmakerCompensationPosition: Boolean,
      compensationPosition: Boolean,
      exemplarVisible: Boolean,
      gameArcCompensationSubtype: Option[String],
      bookmakerCompensationSubtype: Option[String],
      compensationSubtype: Option[String],
      gameArcPreparationCompensationSubtype: Option[String],
      bookmakerPreparationCompensationSubtype: Option[String],
      gameArcPayoffCompensationSubtype: Option[String],
      bookmakerPayoffCompensationSubtype: Option[String],
      gameArcDisplaySubtypeSource: String,
      bookmakerDisplaySubtypeSource: String,
      activeCompensationMention: Boolean,
      bookmakerCompensationMention: Boolean,
      execution: Option[String],
      objective: Option[String],
      focus: Option[String],
      gameArcNarrative: String,
      bookmakerCommentary: String,
      bookmakerSourceMode: String,
      activeNoteStatus: String,
      activeNote: Option[String],
      probeRequestCount: Int,
      probeRefinementRequestCount: Int,
      maintenanceExemplarCandidate: Boolean = false,
      failureMode: Option[String] = None,
      failureIntentConfidence: Double = 0.0,
      failureIntentAnchor: Option[String] = None,
      failureInterpretationAllowed: Boolean = false
  )

  object FocusMomentReport:
    given OFormat[FocusMomentReport] = Json.format[FocusMomentReport]

  final case class GameReport(
      id: String,
      tier: String,
      family: String,
      label: String,
      event: Option[String],
      date: Option[String],
      opening: Option[String],
      result: Option[String],
      totalPlies: Int,
      initialMomentCount: Int,
      refinedMomentCount: Int,
      strategicMomentCount: Int,
      threadCount: Int,
      activeNoteCount: Int,
      probeCandidateMoments: Int,
      probeCandidateRequests: Int,
      probeExecutedRequests: Int,
      probeUnsupportedRequests: Int,
      usedProbeRefinement: Boolean,
      overallThemes: List[String],
      visibleMomentPlies: List[Int],
      focusMoments: List[FocusMomentReport]
  )

  object GameReport:
    given OFormat[GameReport] = Json.format[GameReport]

  final case class Summary(
      totalGames: Int,
      totalFocusMoments: Int,
      totalActiveNotes: Int,
      gamesUsingProbeRefinement: Int,
      totalProbeCandidateRequests: Int,
      totalProbeExecutedRequests: Int,
      totalProbeUnsupportedRequests: Int,
      familyCounts: Map[String, Int],
      compensationSubtypeCounts: Map[String, Int]
  )

  object Summary:
    given OFormat[Summary] = Json.format[Summary]

  final case class NegativeGuardResult(
      id: String,
      label: String,
      targetPly: Int,
      compensationPosition: Boolean,
      bookmakerCompensationMention: Boolean,
      passed: Boolean
  )

  object NegativeGuardResult:
    given OFormat[NegativeGuardResult] = Json.format[NegativeGuardResult]

  final case class AuditFailure(
      category: String,
      severity: String,
      key: String,
      gameId: Option[String],
      ply: Option[Int],
      detail: String
  )

  object AuditFailure:
    given OFormat[AuditFailure] = Json.format[AuditFailure]

  final case class SignoffSummary(
      falsePositiveCount: Int,
      falseNegativeCount: Int,
      positiveExemplarExpectedCount: Int,
      positiveExemplarEvaluatedCount: Int,
      crossSurfaceAgreementRate: Double,
      subtypeAgreementRate: Double,
      payoffTheaterAgreementRate: Double,
      pathVsPayoffDivergenceCount: Int,
      displaySubtypeSourceDistribution: Map[String, Int],
      mustFixFailureCount: Int,
      mustFixFailureCounts: Map[String, Int],
      negativeGuardPassCount: Int,
      negativeGuardFailCount: Int,
      negativeGuards: List[NegativeGuardResult],
      releaseGatePassed: Boolean,
      mustFixFailures: List[AuditFailure]
  )

  object SignoffSummary:
    given Reads[SignoffSummary] = Reads { js =>
      for
        falsePositiveCount <- (js \ "falsePositiveCount").validate[Int]
        falseNegativeCount <- (js \ "falseNegativeCount").validate[Int]
        positiveExemplarExpectedCount <- (js \ "positiveExemplarExpectedCount").validateOpt[Int]
        positiveExemplarEvaluatedCount <- (js \ "positiveExemplarEvaluatedCount").validateOpt[Int]
        crossSurfaceAgreementRate <- (js \ "crossSurfaceAgreementRate").validate[Double]
        subtypeAgreementRate <- (js \ "subtypeAgreementRate").validate[Double]
        payoffTheaterAgreementRate <- (js \ "payoffTheaterAgreementRate").validate[Double]
        pathVsPayoffDivergenceCount <- (js \ "pathVsPayoffDivergenceCount").validate[Int]
        displaySubtypeSourceDistribution <- (js \ "displaySubtypeSourceDistribution").validateOpt[Map[String, Int]]
        mustFixFailureCount <- (js \ "mustFixFailureCount").validateOpt[Int]
        mustFixFailureCounts <- (js \ "mustFixFailureCounts").validateOpt[Map[String, Int]]
        negativeGuardPassCount <- (js \ "negativeGuardPassCount").validate[Int]
        negativeGuardFailCount <- (js \ "negativeGuardFailCount").validate[Int]
        negativeGuards <- (js \ "negativeGuards").validate[List[NegativeGuardResult]]
        releaseGatePassed <- (js \ "releaseGatePassed").validateOpt[Boolean]
        mustFixFailures <- (js \ "mustFixFailures").validateOpt[List[AuditFailure]]
      yield SignoffSummary(
        falsePositiveCount = falsePositiveCount,
        falseNegativeCount = falseNegativeCount,
        positiveExemplarExpectedCount = positiveExemplarExpectedCount.getOrElse(PositiveCompensationExemplars.size),
        positiveExemplarEvaluatedCount = positiveExemplarEvaluatedCount.getOrElse(0),
        crossSurfaceAgreementRate = crossSurfaceAgreementRate,
        subtypeAgreementRate = subtypeAgreementRate,
        payoffTheaterAgreementRate = payoffTheaterAgreementRate,
        pathVsPayoffDivergenceCount = pathVsPayoffDivergenceCount,
        displaySubtypeSourceDistribution = displaySubtypeSourceDistribution.getOrElse(Map.empty),
        mustFixFailureCount = mustFixFailureCount.getOrElse(0),
        mustFixFailureCounts = mustFixFailureCounts.getOrElse(Map.empty),
        negativeGuardPassCount = negativeGuardPassCount,
        negativeGuardFailCount = negativeGuardFailCount,
        negativeGuards = negativeGuards,
        releaseGatePassed = releaseGatePassed.getOrElse(false),
        mustFixFailures = mustFixFailures.getOrElse(Nil)
      )
    }
    given OWrites[SignoffSummary] = Json.writes[SignoffSummary]
    given OFormat[SignoffSummary] = OFormat(summon[Reads[SignoffSummary]], summon[OWrites[SignoffSummary]])

  final case class RunReport(
      version: Int = 1,
      generatedAt: String,
      corpusTitle: String,
      corpusAsOfDate: String,
      depth: Int,
      multiPv: Int,
      enginePath: String,
      summary: Summary,
      signoff: SignoffSummary,
      games: List[GameReport]
  )

  object RunReport:
    given OFormat[RunReport] = Json.format[RunReport]

  private final case class HeaderMeta(
      event: Option[String],
      site: Option[String],
      date: Option[String],
      round: Option[String],
      white: Option[String],
      black: Option[String],
      result: Option[String],
      eco: Option[String],
      opening: Option[String],
      variation: Option[String]
  )

  private final case class ProbeMomentBundle(
      ply: Int,
      requests: List[lila.llm.model.ProbeRequest]
  )

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("real-pgn-narrative-eval-runner")

    val config = parseConfig(args.toList)
    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    val api =
      LlmApi(
        openingExplorer = OpeningExplorerClient(ws),
        geminiClient = GeminiClient(ws, GeminiConfig.fromEnv),
        openAiClient = OpenAiClient(ws, OpenAiConfig.fromEnv),
        commentaryCache = CommentaryCache(),
        llmConfig = LlmConfig.fromEnv,
        providerConfig = LlmProviderConfig.fromEnv.copy(provider = "none")
      )

    val corpus =
      readCorpus(config.corpusPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[real-pgn-eval] failed to read corpus `${config.corpusPath}`: $err")
          sys.exit(1)
    val positiveExemplarCorpus =
      findTruthInventory()
        .flatMap(path =>
          readTruthInventory(path) match
            case Right(value) => Some(buildPositiveExemplarCorpus(value))
            case Left(err) =>
              System.err.println(s"[real-pgn-eval] failed to read truth inventory `${path}`: $err")
              None
        )

    val engine = new LocalUciEngine(config.enginePath, timeoutMs = 30000L)
    try
      val reports = corpus.games.map(runSingleGame(_, api, engine, config))
      val positiveExemplarReports = positiveExemplarCorpus.toList.flatMap(_.games.map(runSingleGame(_, api, engine, config)))
      val negativeGuards = NegativeGuards.flatMap(runNegativeGuard(_, api, engine, config))
      val report =
        RunReport(
          generatedAt = Instant.now().toString,
          corpusTitle = corpus.title,
          corpusAsOfDate = corpus.asOfDate,
          depth = config.depth,
          multiPv = config.multiPv,
          enginePath = config.enginePath.toAbsolutePath.normalize.toString,
          summary = buildSummary(reports),
          signoff = buildSignoff(reports, negativeGuards, positiveExemplarReports),
          games = reports
        )
      writeJson(config.jsonPath, Json.toJson(report))
      writeText(config.markdownPath, renderMarkdown(report))
      println(
        s"[real-pgn-eval] wrote `${config.markdownPath}` and `${config.jsonPath}` (games=${reports.size}, focusMoments=${report.summary.totalFocusMoments})"
      )
    finally
      engine.close()
      ws.close()
      summon[ActorSystem].terminate()

  private def runSingleGame(
      game: CorpusGame,
      api: LlmApi,
      engine: LocalUciEngine,
      config: Config
  ): GameReport =
    val header = parseHeaders(game.pgn)
    val plyData =
      PgnAnalysisHelper.extractPlyDataStrict(game.pgn) match
        case Right(value) => value
        case Left(err)    => throw new IllegalArgumentException(s"${game.id}: PGN validation failed: $err")

    val afterMoveEvals = buildAfterMoveEvals(plyData, engine, config.depth, config.multiPv)
    val initialResponse =
      Await.result(
        api.analyzeGameChronicleLocal(
          pgn = game.pgn,
          evals = afterMoveEvals,
          style = "active",
          focusOn = List("strategy", "long_plan", "piece_route"),
          allowLlmPolish = false,
          asyncTier = false,
          lang = "en",
          planTier = PlanTier.Pro,
          llmLevel = LlmLevel.Active
        ),
        180.seconds
      ).getOrElse(throw new IllegalStateException(s"${game.id}: empty Game Chronicle response"))

    val probeBundles = collectProbeMomentBundles(initialResponse, MaxProbeMoments)
    val (probeResultsByPly, unsupportedProbeCount, executedProbeCount) =
      executeSupportedProbeRequests(probeBundles, engine)

    val refinedResponse =
      if probeResultsByPly.isEmpty then initialResponse
      else
        Await.result(
          api.analyzeGameChronicleLocal(
            pgn = game.pgn,
            evals = afterMoveEvals,
            style = "active",
            focusOn = List("strategy", "long_plan", "piece_route"),
            allowLlmPolish = false,
            asyncTier = false,
            lang = "en",
            planTier = PlanTier.Pro,
            llmLevel = LlmLevel.Active,
            probeResultsByPly = probeResultsByPly
          ),
          180.seconds
        ).getOrElse(initialResponse)

    val internalTruthByPly =
      buildInternalTruthByPly(
        pgn = game.pgn,
        afterMoveEvals = afterMoveEvals,
        probeResultsByPly = probeResultsByPly
      )

    val focusMoments = pickFocusMoments(refinedResponse).flatMap { moment =>
      buildFocusMomentReport(
        game = game,
        header = header,
        moment = moment,
        plyData = plyData,
        afterMoveEvals = afterMoveEvals,
        api = api,
        engine = engine,
        config = config,
        internalTruthByPly = internalTruthByPly
      )
    }

    val rawGamePath = config.rawDir.resolve(s"${game.id}.game_arc.json")
    writeJson(rawGamePath, Json.toJson(refinedResponse))

    GameReport(
      id = game.id,
      tier = game.tier,
      family = game.family,
      label = game.label,
      event = header.event,
      date = header.date,
      opening = combineOpening(header),
      result = header.result,
      totalPlies = plyData.size,
      initialMomentCount = initialResponse.moments.size,
      refinedMomentCount = refinedResponse.moments.size,
      strategicMomentCount = refinedResponse.moments.count(_.strategyPack.exists(_.strategicIdeas.nonEmpty)),
      threadCount = refinedResponse.strategicThreads.size,
      activeNoteCount = refinedResponse.moments.count(_.activeStrategicNote.exists(_.trim.nonEmpty)),
      probeCandidateMoments = probeBundles.size,
      probeCandidateRequests = probeBundles.map(_.requests.size).sum,
      probeExecutedRequests = executedProbeCount,
      probeUnsupportedRequests = unsupportedProbeCount,
      usedProbeRefinement = probeResultsByPly.nonEmpty,
      overallThemes = refinedResponse.themes,
      visibleMomentPlies = refinedResponse.moments.map(_.ply),
      focusMoments = focusMoments
    )

  private def buildFocusMomentReport(
      game: CorpusGame,
      header: HeaderMeta,
      moment: GameChronicleMoment,
      plyData: List[PgnAnalysisHelper.PlyData],
      afterMoveEvals: List[MoveEval],
      api: LlmApi,
      engine: LocalUciEngine,
      config: Config,
      internalTruthByPly: Map[Int, CommentaryEngine.TruthTraceMoment]
  ): Option[FocusMomentReport] =
    plyData.find(_.ply == moment.ply).flatMap { pd =>
      val beforeVars = engine.analyze(pd.fen, config.depth, config.multiPv)
      val afterEval = afterMoveEvals.find(_.ply == pd.ply)
      val afterFen = NarrativeUtils.uciListToFen(pd.fen, List(pd.playedUci))
      val bookmakerResultOpt =
        Await.result(
          api.bookmakerCommentPosition(
            fen = pd.fen,
            lastMove = Some(pd.playedUci),
            eval = beforeVars.headOption.map(v => EvalData(cp = v.scoreCp, mate = v.mate, pv = Some(v.moves))),
            variations = Some(beforeVars),
            probeResults = None,
            openingData = None,
            afterFen = Some(afterFen),
            afterEval = afterEval.map(e => EvalData(cp = e.cp, mate = e.mate, pv = Option.when(e.pv.nonEmpty)(e.pv))),
            afterVariations = afterEval.map(_.getVariations),
            opening = combineOpening(header),
            phase = phaseFromPly(pd.ply, plyData.size),
            ply = pd.ply,
            prevStateToken = None,
            prevEndgameStateToken = None,
            allowLlmPolish = false,
            lang = "en",
            planTier = PlanTier.Pro,
            llmLevel = LlmLevel.Active
          ),
          180.seconds
        )
      bookmakerResultOpt.map { bookmakerResult =>
        val momentSurface = StrategyPackSurface.from(moment.strategyPack)
        val bookmakerSurface = StrategyPackSurface.from(bookmakerResult.response.strategyPack)
        val internalTruth = internalTruthByPly.get(moment.ply)
        val activeNoteText = moment.activeStrategicNote.map(oneLine)
        val bookmakerCommentary = oneLine(bookmakerResult.response.commentary)
        val gameArcCompensationPosition = momentSurface.compensationPosition
        val bookmakerCompensationPosition = bookmakerSurface.compensationPosition
        val compensationPosition =
          RealPgnNarrativeEvalCalibration.compensationEvalPosition(
            moment,
            momentSurface,
            bookmakerSurface,
            Some(pd.playedUci)
          )
        val exemplarVisible =
          RealPgnNarrativeEvalCalibration.exemplarEvalPosition(moment, momentSurface, bookmakerSurface, internalTruth)
        val rawBookmakerPath = config.rawDir.resolve(s"${game.id}.ply_${moment.ply}.bookmaker.json")
        writeJson(rawBookmakerPath, Json.toJson(bookmakerResult.response))
        FocusMomentReport(
          ply = moment.ply,
          moveNumber = moment.moveNumber,
          side = moment.side,
          momentType = moment.momentType,
          selectionKind = moment.selectionKind,
          dominantIdea = momentSurface.dominantIdeaText,
          secondaryIdea = momentSurface.secondaryIdeaText,
          campaignOwner = momentSurface.campaignOwnerText,
          ownerMismatch = momentSurface.ownerMismatch,
          gameArcCompensationPosition = gameArcCompensationPosition,
          bookmakerCompensationPosition = bookmakerCompensationPosition,
          compensationPosition = compensationPosition,
          exemplarVisible = exemplarVisible,
          gameArcCompensationSubtype =
            StrategyPackSurface.strictCompensationSubtypeLabel(momentSurface)
              .orElse(StrategyPackSurface.compensationSubtypeLabel(momentSurface)),
          bookmakerCompensationSubtype =
            StrategyPackSurface.strictCompensationSubtypeLabel(bookmakerSurface)
              .orElse(StrategyPackSurface.compensationSubtypeLabel(bookmakerSurface)),
          compensationSubtype =
            Option.when(compensationPosition) {
              StrategyPackSurface.strictCompensationSubtypeLabel(momentSurface)
                .orElse(StrategyPackSurface.strictCompensationSubtypeLabel(bookmakerSurface))
                .orElse(StrategyPackSurface.compensationSubtypeLabel(momentSurface))
                .orElse(StrategyPackSurface.compensationSubtypeLabel(bookmakerSurface))
            }.flatten,
          gameArcPreparationCompensationSubtype =
            StrategyPackSurface.preparationCompensationSubtypeLabel(momentSurface),
          bookmakerPreparationCompensationSubtype =
            StrategyPackSurface.preparationCompensationSubtypeLabel(bookmakerSurface),
          gameArcPayoffCompensationSubtype =
            StrategyPackSurface.payoffCompensationSubtypeLabel(momentSurface),
          bookmakerPayoffCompensationSubtype =
            StrategyPackSurface.payoffCompensationSubtypeLabel(bookmakerSurface),
          gameArcDisplaySubtypeSource = momentSurface.displaySubtypeSource,
          bookmakerDisplaySubtypeSource = bookmakerSurface.displaySubtypeSource,
          activeCompensationMention =
            activeNoteText.exists(text => mentionsCompensationContract(text, momentSurface)),
          bookmakerCompensationMention = mentionsCompensationContract(bookmakerCommentary, bookmakerSurface),
          execution = momentSurface.executionText,
          objective = momentSurface.objectiveText,
          focus = momentSurface.focusText,
          gameArcNarrative = oneLine(moment.narrative),
          bookmakerCommentary = bookmakerCommentary,
          bookmakerSourceMode = bookmakerResult.response.sourceMode,
          activeNoteStatus = moment.activeStrategicSourceMode.getOrElse("missing"),
          activeNote = activeNoteText,
          probeRequestCount = moment.probeRequests.size,
          probeRefinementRequestCount = moment.probeRefinementRequests.size,
          maintenanceExemplarCandidate = internalTruth.exists(_.maintenanceExemplarCandidate),
          failureMode = internalTruth.map(_.failureMode),
          failureIntentConfidence = internalTruth.map(_.failureIntentConfidence).getOrElse(0.0),
          failureIntentAnchor = internalTruth.flatMap(_.failureIntentAnchor),
          failureInterpretationAllowed = internalTruth.exists(_.failureInterpretationAllowed)
        )
      }
    }

  private def runNegativeGuard(
      guard: NegativeGuardSpec,
      api: LlmApi,
      engine: LocalUciEngine,
      config: Config
  ): Option[NegativeGuardResult] =
    val game =
      CorpusGame(
        id = guard.id,
        tier = "guard",
        family = guard.family,
        label = guard.label,
        notes = List("negative guard"),
        expectedThemes = Nil,
        pgn = guard.pgn
      )
    val header = parseHeaders(game.pgn)
    val plyData =
      PgnAnalysisHelper.extractPlyDataStrict(game.pgn) match
        case Right(value) => value
        case Left(err)    => throw new IllegalArgumentException(s"${guard.id}: PGN validation failed: $err")

    val afterMoveEvals = buildAfterMoveEvals(plyData, engine, config.depth, config.multiPv)
    val initialResponse =
      Await.result(
        api.analyzeGameChronicleLocal(
          pgn = game.pgn,
          evals = afterMoveEvals,
          style = "active",
          focusOn = List("strategy", "long_plan", "piece_route"),
          allowLlmPolish = false,
          asyncTier = false,
          lang = "en",
          planTier = PlanTier.Pro,
          llmLevel = LlmLevel.Active
        ),
        180.seconds
      ).getOrElse(throw new IllegalStateException(s"${guard.id}: empty Game Chronicle response"))

    val probeBundles = collectProbeMomentBundles(initialResponse, MaxProbeMoments)
    val (probeResultsByPly, _, _) = executeSupportedProbeRequests(probeBundles, engine)
    val refinedResponse =
      if probeResultsByPly.isEmpty then initialResponse
      else
        Await.result(
          api.analyzeGameChronicleLocal(
            pgn = game.pgn,
            evals = afterMoveEvals,
            style = "active",
            focusOn = List("strategy", "long_plan", "piece_route"),
            allowLlmPolish = false,
            asyncTier = false,
            lang = "en",
            planTier = PlanTier.Pro,
            llmLevel = LlmLevel.Active,
            probeResultsByPly = probeResultsByPly
          ),
          180.seconds
        ).getOrElse(initialResponse)

    val internalTruthByPly =
      buildInternalTruthByPly(
        pgn = game.pgn,
        afterMoveEvals = afterMoveEvals,
        probeResultsByPly = probeResultsByPly
      )

    refinedResponse.moments.find(_.ply == guard.targetPly).flatMap { moment =>
      buildFocusMomentReport(
        game = game,
        header = header,
        moment = moment,
        plyData = plyData,
        afterMoveEvals = afterMoveEvals,
        api = api,
        engine = engine,
        config = config,
        internalTruthByPly = internalTruthByPly
      ).map { report =>
        NegativeGuardResult(
          id = guard.id,
          label = guard.label,
          targetPly = guard.targetPly,
          compensationPosition = report.compensationPosition,
          bookmakerCompensationMention = report.bookmakerCompensationMention,
          passed = !report.compensationPosition && !report.bookmakerCompensationMention
        )
      }
    }

  private def buildInternalTruthByPly(
      pgn: String,
      afterMoveEvals: List[MoveEval],
      probeResultsByPly: Map[Int, List[lila.llm.model.ProbeResult]]
  ): Map[Int, CommentaryEngine.TruthTraceMoment] =
    val evalMap = afterMoveEvals.map(eval => eval.ply -> eval.getVariations).toMap
    CommentaryEngine
      .generateGameArcDiagnostic(
        pgn = pgn,
        evals = evalMap,
        probeResultsByPly = probeResultsByPly,
        variantKey = EarlyOpeningNarrationPolicy.StandardVariant
      )
      .canonicalTraceMoments
      .map(trace => trace.ply -> trace)
      .toMap

  private def findTruthInventory(): Option[Path] =
    CommentaryPlayerQcSupport.TruthInventoryLookupPaths
      .map(_.toAbsolutePath.normalize)
      .find(Files.isRegularFile(_))

  private[tools] def buildPositiveExemplarCorpus(
      inventory: RealPgnNarrativeEvalTruthInventoryBuilder.TruthInventory,
      generatedAt: Instant = Instant.now()
  ): Corpus =
    val selectedGameIds =
      PositiveCompensationExemplars.toList.sorted.map(_.takeWhile(_ != ':')).distinct
    val sourceById = inventory.games.map(game => game.id -> game).toMap
    val missingGameIds = selectedGameIds.filterNot(sourceById.contains)
    require(
      missingGameIds.isEmpty,
      s"truth inventory is missing canonical positive exemplar game ids: ${missingGameIds.mkString(", ")}"
    )
    Corpus(
      version = 1,
      generatedAt = generatedAt.toString,
      asOfDate = generatedAt.toString.take(10),
      title = "Positive Compensation Exemplars",
      description = "Canonical positive-exemplar rerun corpus derived from the truth inventory.",
      games = selectedGameIds.flatMap(sourceById.get)
    )

  private def pickFocusMoments(response: GameChronicleResponse): List[GameChronicleMoment] =
    def rank(moment: GameChronicleMoment): (Int, Int, Int, Int, Int) =
      val surface = StrategyPackSurface.from(moment.strategyPack)
      (
        if surface.strictCompensationPosition && surface.quietCompensationPosition then 0
        else if surface.strictCompensationPosition && surface.durableCompensationPosition then 1
        else if surface.strictCompensationPosition then 2
        else 3,
        if moment.strategyPack.exists(_.strategicIdeas.nonEmpty) then 0 else 1,
        if moment.strategicThread.isDefined then 0 else 1,
        if moment.probeRefinementRequests.nonEmpty then 0 else 1,
        moment.ply
      )

    val candidates =
      response.moments
        .filter(m => m.strategyPack.isDefined || m.signalDigest.isDefined || m.strategicThread.isDefined)
        .sortBy(rank)
        .distinctBy(_.ply)

    (if candidates.nonEmpty then candidates else response.moments.distinctBy(_.ply)).take(MaxFocusMoments)

  private def collectProbeMomentBundles(
      response: GameChronicleResponse,
      maxMoments: Int
  ): List[ProbeMomentBundle] =
    val seen = scala.collection.mutable.Set.empty[String]
    response.moments
      .sortBy(probeMomentRank)
      .flatMap { moment =>
        val requests =
          if moment.probeRefinementRequests.nonEmpty then moment.probeRefinementRequests
          else moment.probeRequests
        requests
          .find { request =>
            val key = probeDedupKey(request)
            if seen.contains(key) then false
            else
              seen += key
              true
          }
          .map(req => ProbeMomentBundle(moment.ply, List(req)))
      }
      .take(maxMoments)

  private def probeMomentRank(moment: GameChronicleMoment): (Int, Int, Int, Int, Int) =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val quietCompensation = surface.strictCompensationPosition && surface.quietCompensationPosition
    val durableCompensation = surface.strictCompensationPosition && surface.durableCompensationPosition
    val compensation =
      surface.strictCompensationPosition ||
        moment.signalDigest.exists(_.compensation.exists(_.trim.nonEmpty))
    val strategicCarrier =
      compensation ||
        surface.dominantIdeaText.nonEmpty ||
        surface.executionText.nonEmpty ||
        surface.objectiveText.nonEmpty ||
        moment.signalDigest.exists(_.dominantIdeaKind.isDefined) ||
        moment.strategyPack.exists(pack =>
          pack.strategicIdeas.nonEmpty || pack.longTermFocus.nonEmpty || pack.pieceRoutes.nonEmpty
        )
    val priority =
      if quietCompensation && moment.strategicBranch then 0
      else if durableCompensation && moment.strategicBranch then 1
      else if compensation && moment.strategicBranch then 2
      else if moment.strategicBranch && strategicCarrier then 3
      else if quietCompensation then 4
      else if moment.selectionKind == "key" && strategicCarrier then 5
      else if compensation then 6
      else if strategicCarrier then 7
      else 8
    val salience =
      moment.strategicSalience match
        case Some(level) if level.equalsIgnoreCase("High")   => 0
        case Some(level) if level.equalsIgnoreCase("Medium") => 1
        case _                                               => 2
    val selectionKind =
      if moment.selectionKind == "key" then 0
      else if moment.selectionKind == "thread_bridge" then 1
      else 2
    val refinementBias = if moment.probeRefinementRequests.nonEmpty then 0 else 1
    (priority, salience, selectionKind, refinementBias, moment.ply)

  private def executeSupportedProbeRequests(
      bundles: List[ProbeMomentBundle],
      engine: LocalUciEngine
  ): (Map[Int, List[ProbeResult]], Int, Int) =
    val supportedSignals = Set("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot")
    var unsupported = 0
    val byPly = scala.collection.mutable.Map.empty[Int, List[ProbeResult]].withDefaultValue(Nil)

    bundles.foreach { bundle =>
      bundle.requests.foreach { request =>
        val required = request.requiredSignals.toSet
        if required.subsetOf(supportedSignals) then
          val afterFen = NarrativeUtils.uciListToFen(request.fen, request.moves)
          val multiPv = request.multiPv.getOrElse(2).max(1)
          val vars = engine.analyze(afterFen, request.depth.max(8), multiPv)
          vars.headOption.foreach { best =>
            val deltaVsBaseline = best.scoreCp - request.baselineEvalCp.getOrElse(best.scoreCp)
            val moverColor = PgnAnalysisHelper.sideToMoveFromFen(request.fen).getOrElse(Color.White)
            val moverLoss = moverLossCp(moverColor, deltaVsBaseline)
            val keyMotifs =
              if required.contains("keyMotifs") then synthesizeKeyMotifs(request, best, moverLoss) else Nil
            val l1Delta =
              if required.contains("l1Delta") then
                synthesizeL1Delta(request.fen, afterFen, moverLoss, best)
              else None
            val futureSnapshot =
              if required.contains("futureSnapshot") then
                synthesizeFutureSnapshot(request, afterFen, best, moverLoss, keyMotifs)
              else None
            val result =
              ProbeResult(
                id = request.id,
                fen = Some(request.fen),
                evalCp = best.scoreCp,
                bestReplyPv = best.moves,
                replyPvs = Some(vars.map(_.moves)),
                deltaVsBaseline = deltaVsBaseline,
                keyMotifs = keyMotifs,
                purpose = request.purpose,
                questionId = request.questionId,
                questionKind = request.questionKind,
                probedMove = request.moves.headOption,
                mate = best.mate,
                depth = Some(best.depth),
                l1Delta = l1Delta,
                futureSnapshot = futureSnapshot,
                objective = request.objective,
                seedId = request.seedId
              )
            byPly.update(bundle.ply, byPly(bundle.ply) :+ result)
          }
        else unsupported += 1
      }
    }

    (byPly.toMap.view.mapValues(_.take(1)).toMap, unsupported, byPly.values.map(_.size).sum)

  private def moverLossCp(moverColor: Color, deltaVsBaseline: Int): Int =
    if moverColor.white then -deltaVsBaseline else deltaVsBaseline

  private def synthesizeKeyMotifs(
      request: ProbeRequest,
      best: VariationLine,
      moverLoss: Int
  ): List[String] =
    val raw =
      List(
        request.id,
        request.planId.getOrElse(""),
        request.planName.getOrElse(""),
        request.objective.getOrElse(""),
        request.purpose.getOrElse("")
      ).mkString(" ").toLowerCase
    val motifs = scala.collection.mutable.ListBuffer.empty[String]
    if raw.contains("counterplay") then motifs += "counterplay"
    if raw.contains("break") then motifs += "counter_break"
    if raw.contains("pawnstorm") || raw.contains("rook_pawn_march") || raw.contains("hook") then motifs += "pawnstorm"
    if raw.contains("simplification") || raw.contains("convert") || raw.contains("endgame") then motifs += "conversion"
    if raw.contains("weakpawnattack") || raw.contains("fixed_pawn") || raw.contains("weakness") then motifs += "weakness_fixation"
    if raw.contains("pieceactivation") || raw.contains("piece_activation") || raw.contains("coordination") then motifs += "coordination"
    if raw.contains("prophylaxis") then motifs += "prophylaxis"
    if raw.contains("line") || raw.contains("file") || raw.contains("occupation") then motifs += "line_pressure"
    if raw.contains("kingattack") || raw.contains("king_attack") then motifs += "king_attack"
    if raw.contains("outpost") then motifs += "outpost"
    if best.mate.exists(_ < 0) then motifs += "Mate"
    else if moverLoss >= 150 then motifs += "Material"
    motifs.toList.distinct.take(4)

  private def synthesizeL1Delta(
      beforeFen: String,
      afterFen: String,
      moverLoss: Int,
      best: VariationLine
  ): Option[L1DeltaSnapshot] =
    val materialDelta = whiteMaterialScore(afterFen) - whiteMaterialScore(beforeFen)
    val mobilityDelta = legalMoveCount(afterFen) - legalMoveCount(beforeFen)
    val kingSafetyDelta =
      if best.mate.exists(_ < 0) then -80
      else if positionInCheck(afterFen) then -35
      else 0
    val collapseReason =
      if best.mate.exists(_ < 0) then Some("King exposed")
      else if moverLoss >= 180 then Some("Lost initiative")
      else if materialDelta <= -100 then Some("Material deficit deepens")
      else if mobilityDelta <= -10 then Some("Piece activity stalls")
      else None
    Some(
      L1DeltaSnapshot(
        materialDelta = materialDelta,
        kingSafetyDelta = kingSafetyDelta,
        centerControlDelta = 0,
        openFilesDelta = 0,
        mobilityDelta = mobilityDelta,
        collapseReason = collapseReason
      )
    )

  private def synthesizeFutureSnapshot(
      request: ProbeRequest,
      afterFen: String,
      best: VariationLine,
      moverLoss: Int,
      keyMotifs: List[String]
  ): Option[FutureSnapshot] =
    val supportive = moverLoss <= 80 && !best.mate.exists(_ < 0)
    val planHints = planProgressHints(request, keyMotifs)
    val blockersRemoved =
      if supportive && keyMotifs.contains("counterplay") then List("counterplay")
      else if supportive && keyMotifs.contains("line_pressure") then List("line access")
      else Nil
    val prereqsMet =
      if supportive then planHints.take(2)
      else Nil
    val strategicAdded =
      if supportive then
        keyMotifs
          .filter(m => Set("line_pressure", "weakness_fixation", "pawnstorm", "conversion", "coordination").contains(m))
          .map(_.replace('_', ' '))
          .take(2)
      else Nil
    val tacticalAdded =
      if supportive then request.moves.takeRight(1).map(NarrativeUtils.uciToSanOrFormat(afterFen, _))
      else Nil
    val newThreatKinds =
      if best.mate.exists(_ < 0) then List("Mate")
      else if moverLoss >= 150 then List("Material")
      else Nil
    val resolvedThreatKinds =
      if supportive && blockersRemoved.nonEmpty then List("Counterplay")
      else Nil
    val positive =
      prereqsMet.nonEmpty || blockersRemoved.nonEmpty || strategicAdded.nonEmpty || resolvedThreatKinds.nonEmpty || newThreatKinds.nonEmpty
    Option.when(positive) {
      FutureSnapshot(
        resolvedThreatKinds = resolvedThreatKinds,
        newThreatKinds = newThreatKinds,
        targetsDelta =
          TargetsDelta(
            tacticalAdded = tacticalAdded,
            tacticalRemoved = Nil,
            strategicAdded = strategicAdded,
            strategicRemoved = if !supportive && moverLoss >= 150 then List("initiative") else Nil
          ),
        planBlockersRemoved = blockersRemoved,
        planPrereqsMet = prereqsMet
      )
    }

  private def planProgressHints(request: ProbeRequest, keyMotifs: List[String]): List[String] =
    val raw =
      List(request.planName, request.objective, request.purpose, request.planId).flatten.mkString(" ").toLowerCase
    val hints = scala.collection.mutable.ListBuffer.empty[String]
    if keyMotifs.contains("line_pressure") || raw.contains("line") then hints += "line pressure lane secured"
    if keyMotifs.contains("weakness_fixation") || raw.contains("weak") then hints += "target fixation is maintained"
    if keyMotifs.contains("pawnstorm") then hints += "attack lane remains available"
    if keyMotifs.contains("conversion") || raw.contains("convert") then hints += "conversion window remains open"
    if keyMotifs.contains("coordination") then hints += "piece coordination is improved"
    if hints.isEmpty && raw.nonEmpty then hints += "strategic trajectory remains intact"
    hints.toList.distinct

  private def whiteMaterialScore(fen: String): Int =
    fen
      .takeWhile(_ != ' ')
      .foldLeft(0) {
        case (acc, ch) =>
          acc + (ch match
            case 'P' => 100
            case 'N' => 320
            case 'B' => 330
            case 'R' => 500
            case 'Q' => 900
            case 'p' => -100
            case 'n' => -320
            case 'b' => -330
            case 'r' => -500
            case 'q' => -900
            case _   => 0)
      }

  private def legalMoveCount(fen: String): Int =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map(_.legalMoves.toList.size).getOrElse(0)

  private def positionInCheck(fen: String): Boolean =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).exists(_.check.yes)

  private def buildAfterMoveEvals(
      plyData: List[PgnAnalysisHelper.PlyData],
      engine: LocalUciEngine,
      depth: Int,
      multiPv: Int
  ): List[MoveEval] =
    engine.newGame()
    plyData.map { pd =>
      val afterFen = NarrativeUtils.uciListToFen(pd.fen, List(pd.playedUci))
      val variations = engine.analyze(afterFen, depth, multiPv)
      val best = variations.headOption
      MoveEval(
        ply = pd.ply,
        cp = best.map(_.scoreCp).getOrElse(0),
        mate = best.flatMap(_.mate),
        pv = best.map(_.moves).getOrElse(Nil),
        variations = variations
      )
    }

  private[tools] def buildSummary(games: List[GameReport]): Summary =
    val compensationSubtypeCounts =
      games
        .flatMap(_.focusMoments)
        .flatMap(moment => moment.compensationSubtype.toList)
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toMap
    Summary(
      totalGames = games.size,
      totalFocusMoments = games.map(_.focusMoments.size).sum,
      totalActiveNotes = games.map(_.activeNoteCount).sum,
      gamesUsingProbeRefinement = games.count(_.usedProbeRefinement),
      totalProbeCandidateRequests = games.map(_.probeCandidateRequests).sum,
      totalProbeExecutedRequests = games.map(_.probeExecutedRequests).sum,
      totalProbeUnsupportedRequests = games.map(_.probeUnsupportedRequests).sum,
      familyCounts = games.groupBy(_.family).view.mapValues(_.size).toMap,
      compensationSubtypeCounts = compensationSubtypeCounts
    )

  private[tools] def buildSignoff(
      games: List[GameReport],
      negativeGuards: List[NegativeGuardResult],
      positiveExemplarReports: List[GameReport] = Nil
  ): SignoffSummary =
    val focusMoments = games.flatMap(_.focusMoments)
    val exemplarFocusMoments = positiveExemplarReports.flatMap(_.focusMoments)
    val compensationMoments = focusMoments.filter(_.compensationPosition)
    val auditFailures = scala.collection.mutable.ListBuffer.empty[AuditFailure]
    val evaluatedPositiveExemplarKeys =
      (focusMoments.map(moment => momentKey(gameIdForMoment(games, moment), moment)) ++
        exemplarFocusMoments.map(moment => momentKey(gameIdForMoment(positiveExemplarReports, moment), moment)))
        .toSet
        .intersect(PositiveCompensationExemplars)
    val positiveFalseNegatives =
      evaluatedPositiveExemplarKeys.toList.sorted.count { key =>
        val focusHit =
          focusMoments.exists(moment => momentKey(gameIdForMoment(games, moment), moment) == key && moment.exemplarVisible)
        val exemplarHit =
          exemplarFocusMoments.exists(moment =>
            momentKey(gameIdForMoment(positiveExemplarReports, moment), moment) == key && moment.exemplarVisible
          )
        !focusHit && !exemplarHit
      }
    evaluatedPositiveExemplarKeys.toList.sorted.foreach { key =>
      val focusHit =
        focusMoments.exists(moment => momentKey(gameIdForMoment(games, moment), moment) == key && moment.exemplarVisible)
      val exemplarHit =
        exemplarFocusMoments.exists(moment =>
          momentKey(gameIdForMoment(positiveExemplarReports, moment), moment) == key && moment.exemplarVisible
        )
      if !focusHit && !exemplarHit then
        val (gameId, ply) = parseMomentKey(key)
        auditFailures += AuditFailure(
          category = "positive_exemplar_missed",
          severity = "P0",
          key = key,
          gameId = gameId,
          ply = ply,
          detail = "Expected positive exemplar is missing from the current focus-moment exemplar set."
        )
    }
    val borderlineFalsePositives =
      BorderlineExpectations.count { case (key, shouldKeep) =>
        !shouldKeep &&
          focusMoments.exists(moment => momentKey(gameIdForMoment(games, moment), moment) == key && moment.compensationPosition)
      }
    negativeGuards.filterNot(_.passed).foreach { guard =>
      auditFailures += AuditFailure(
        category = "negative_guard_failed",
        severity = "P0",
        key = guard.id,
        gameId = Some(guard.id),
        ply = Some(guard.targetPly),
        detail = "Negative guard still leaks compensation in report tagging or raw Bookmaker prose."
      )
    }
    val crossSurfaceAgreementRate =
      ratio(
        compensationMoments.count(moment =>
          moment.gameArcCompensationPosition &&
            moment.bookmakerCompensationPosition &&
            moment.activeCompensationMention
        ),
        compensationMoments.size
      )
    val subtypeAgreementRate =
      ratio(
        compensationMoments.count(moment =>
          moment.gameArcCompensationSubtype.nonEmpty &&
            moment.gameArcCompensationSubtype == moment.bookmakerCompensationSubtype
        ),
        compensationMoments.size
      )
    val payoffTheaterAgreementRate =
      ratio(
        compensationMoments.count(moment =>
          extractTheater(moment.gameArcPayoffCompensationSubtype)
            .zip(extractTheater(moment.bookmakerPayoffCompensationSubtype))
            .exists { case (gameArcTheater, bookmakerTheater) => gameArcTheater == bookmakerTheater }
        ),
        compensationMoments.size
      )
    val pathVsPayoffDivergenceCount =
      compensationMoments.count(moment =>
        (moment.gameArcPreparationCompensationSubtype, moment.gameArcPayoffCompensationSubtype) match
          case (Some(path), Some(payoff))
              if unresolvedPathPayoffDivergence(
                path,
                payoff,
                moment.gameArcCompensationSubtype,
                moment.gameArcDisplaySubtypeSource
              ) =>
            true
          case _ => false
      ) +
        compensationMoments.count(moment =>
          (moment.bookmakerPreparationCompensationSubtype, moment.bookmakerPayoffCompensationSubtype) match
            case (Some(path), Some(payoff))
                if unresolvedPathPayoffDivergence(
                  path,
                  payoff,
                  moment.bookmakerCompensationSubtype,
                  moment.bookmakerDisplaySubtypeSource
                ) =>
              true
            case _ => false
        )
    val displaySubtypeSourceDistribution =
      compensationMoments
        .flatMap(moment => List(moment.gameArcDisplaySubtypeSource, moment.bookmakerDisplaySubtypeSource))
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toMap
    compensationMoments.foreach { moment =>
      val gameId = gameIdForMoment(games, moment)
      val key = momentKey(gameId, moment)
      val missingSurfaces =
        List(
          "chronicle" -> moment.gameArcCompensationPosition,
          "bookmaker" -> moment.bookmakerCompensationPosition,
          "active" -> moment.activeCompensationMention
        ).collect { case (surface, false) => surface }
      if missingSurfaces.nonEmpty then
        auditFailures += AuditFailure(
          category = "cross_surface_parity",
          severity = "P1",
          key = key,
          gameId = Some(gameId),
          ply = Some(moment.ply),
          detail = s"Compensation contract is missing on ${missingSurfaces.mkString(", ")}."
        )
      (moment.gameArcCompensationSubtype, moment.bookmakerCompensationSubtype) match
        case (Some(gameArcSubtype), Some(bookmakerSubtype)) if gameArcSubtype != bookmakerSubtype =>
          auditFailures += AuditFailure(
            category = "subtype_mismatch",
            severity = "P1",
            key = key,
            gameId = Some(gameId),
            ply = Some(moment.ply),
            detail = s"Chronicle subtype `$gameArcSubtype` and Bookmaker subtype `$bookmakerSubtype` disagree."
          )
        case _ => ()
      List(
        "chronicle" -> (
          moment.gameArcPreparationCompensationSubtype,
          moment.gameArcPayoffCompensationSubtype,
          moment.gameArcCompensationSubtype,
          moment.gameArcDisplaySubtypeSource
        ),
        "bookmaker" -> (
          moment.bookmakerPreparationCompensationSubtype,
          moment.bookmakerPayoffCompensationSubtype,
          moment.bookmakerCompensationSubtype,
          moment.bookmakerDisplaySubtypeSource
        )
      ).foreach { case (surface, pair) =>
        pair match
          case (Some(path), Some(payoff), selected, source) if unresolvedPathPayoffDivergence(path, payoff, selected, source) =>
            auditFailures += AuditFailure(
              category = "path_payoff_divergence",
              severity = "P1",
              key = key,
              gameId = Some(gameId),
              ply = Some(moment.ply),
              detail = s"$surface preparation subtype `$path` diverges from payoff subtype `$payoff`."
            )
          case _ => ()
      }
    }
    val mustFixFailures =
      auditFailures.toList.distinctBy(failure => s"${failure.category}|${failure.key}|${failure.detail}")
    val mustFixFailureCounts = mustFixFailures.groupBy(_.category).view.mapValues(_.size).toMap
    val falsePositiveCount = borderlineFalsePositives + negativeGuards.count(!_.passed)
    val releaseGatePassed = falsePositiveCount == 0 && mustFixFailures.isEmpty

    SignoffSummary(
      falsePositiveCount = falsePositiveCount,
      falseNegativeCount = positiveFalseNegatives,
      positiveExemplarExpectedCount = PositiveCompensationExemplars.size,
      positiveExemplarEvaluatedCount = evaluatedPositiveExemplarKeys.size,
      crossSurfaceAgreementRate = crossSurfaceAgreementRate,
      subtypeAgreementRate = subtypeAgreementRate,
      payoffTheaterAgreementRate = payoffTheaterAgreementRate,
      pathVsPayoffDivergenceCount = pathVsPayoffDivergenceCount,
      displaySubtypeSourceDistribution = displaySubtypeSourceDistribution,
      mustFixFailureCount = mustFixFailures.size,
      mustFixFailureCounts = mustFixFailureCounts,
      negativeGuardPassCount = negativeGuards.count(_.passed),
      negativeGuardFailCount = negativeGuards.count(!_.passed),
      negativeGuards = negativeGuards,
      releaseGatePassed = releaseGatePassed,
      mustFixFailures = mustFixFailures
    )

  private[tools] def renderMarkdown(report: RunReport): String =
    val sb = new StringBuilder()
    sb.append(s"# ${report.corpusTitle}\n\n")
    sb.append(s"- Generated: `${report.generatedAt}`\n")
    sb.append(s"- Depth / MultiPV: `${report.depth}` / `${report.multiPv}`\n")
    sb.append(s"- Engine: `${report.enginePath}`\n")
    sb.append(s"- Games: `${report.summary.totalGames}`\n")
    sb.append(s"- Focus moments reviewed: `${report.summary.totalFocusMoments}`\n")
    sb.append(s"- Probe refinement used in games: `${report.summary.gamesUsingProbeRefinement}`\n")
    sb.append(s"- Probe candidate / executed / unsupported: `${report.summary.totalProbeCandidateRequests}` / `${report.summary.totalProbeExecutedRequests}` / `${report.summary.totalProbeUnsupportedRequests}`\n")
    sb.append(s"- Active notes attached: `${report.summary.totalActiveNotes}`\n\n")
    sb.append(s"- Compensation subtype counts: ${renderMap(report.summary.compensationSubtypeCounts)}\n\n")
    sb.append("## Signoff\n\n")
    sb.append(s"- False positives: `${report.signoff.falsePositiveCount}`\n")
    sb.append(s"- False negatives on positive exemplars: `${report.signoff.falseNegativeCount}`\n")
    sb.append(
      s"- Positive exemplar coverage: `${report.signoff.positiveExemplarEvaluatedCount}` / `${report.signoff.positiveExemplarExpectedCount}`\n"
    )
    sb.append(f"- Cross-surface agreement rate: `${report.signoff.crossSurfaceAgreementRate * 100}%.1f%%`\n")
    sb.append(f"- Subtype agreement rate: `${report.signoff.subtypeAgreementRate * 100}%.1f%%`\n")
    sb.append(f"- Payoff-theater agreement rate: `${report.signoff.payoffTheaterAgreementRate * 100}%.1f%%`\n")
    sb.append(s"- Path vs payoff divergence count: `${report.signoff.pathVsPayoffDivergenceCount}`\n")
    sb.append(s"- Display subtype sources: ${renderMap(report.signoff.displaySubtypeSourceDistribution)}\n")
    sb.append(s"- Must-fix audit failures: `${report.signoff.mustFixFailureCount}`\n")
    sb.append(s"- Must-fix families: ${renderMap(report.signoff.mustFixFailureCounts)}\n")
    sb.append(s"- Negative guard pass / fail: `${report.signoff.negativeGuardPassCount}` / `${report.signoff.negativeGuardFailCount}`\n\n")
    sb.append("### Exit Criteria\n\n")
    sb.append(s"- `false positives = 0`: `${report.signoff.falsePositiveCount == 0}`\n")
    sb.append(s"- `positive exemplar misses = 0`: `${report.signoff.falseNegativeCount == 0}`\n")
    sb.append(
      s"- `positive exemplar coverage = ${report.signoff.positiveExemplarExpectedCount}`: `${report.signoff.positiveExemplarEvaluatedCount == report.signoff.positiveExemplarExpectedCount}`\n"
    )
    sb.append(s"- `cross-surface agreement = 100%`: `${report.signoff.crossSurfaceAgreementRate == 1.0}`\n")
    sb.append(s"- `subtype agreement = 100%`: `${report.signoff.subtypeAgreementRate == 1.0}`\n")
    sb.append(s"- `path/payoff divergence = 0`: `${report.signoff.pathVsPayoffDivergenceCount == 0}`\n")
    sb.append(s"- `negative guards all pass`: `${report.signoff.negativeGuardFailCount == 0}`\n")
    sb.append(s"- `release gate passed`: `${report.signoff.releaseGatePassed}`\n\n")
    if report.signoff.mustFixFailures.nonEmpty then
      sb.append("### Must-Fix Audit Failures\n\n")
      report.signoff.mustFixFailures.foreach { failure =>
        sb.append(s"- `${failure.severity}` `${failure.category}` `${failure.key}`")
        failure.gameId.foreach(gameId => sb.append(s" game=`$gameId`"))
        failure.ply.foreach(ply => sb.append(s" ply=`$ply`"))
        sb.append(s": ${failure.detail}\n")
      }
      sb.append("\n")
    if report.signoff.negativeGuards.nonEmpty then
      sb.append("### Negative Guards\n\n")
      report.signoff.negativeGuards.foreach { guard =>
        sb.append(
          s"- `${guard.id}` ply `${guard.targetPly}` passed=`${guard.passed}` compensation=`${guard.compensationPosition}` rawBookmakerMention=`${guard.bookmakerCompensationMention}`\n"
        )
      }
      sb.append("\n")
    sb.append("## Games\n\n")
    report.games.foreach { game =>
      val eventText = game.event.getOrElse("-")
      val dateText = game.date.getOrElse("-")
      val openingText = game.opening.getOrElse("-")
      val resultText = game.result.getOrElse("-")
      val visibleMomentPliesText = game.visibleMomentPlies.mkString(", ")
      sb.append(s"### ${game.id} ${game.label}\n\n")
      sb.append(s"- Family / tier: `${game.family}` / `${game.tier}`\n")
      sb.append(s"- Event / date: `${eventText}` / `${dateText}`\n")
      sb.append(s"- Opening / result: `${openingText}` / `${resultText}`\n")
      sb.append(s"- Moments initial/refined/strategic: `${game.initialMomentCount}` / `${game.refinedMomentCount}` / `${game.strategicMomentCount}`\n")
      sb.append(s"- Threads / active notes: `${game.threadCount}` / `${game.activeNoteCount}`\n")
      sb.append(s"- Probe candidate moments/requests/executed/unsupported: `${game.probeCandidateMoments}` / `${game.probeCandidateRequests}` / `${game.probeExecutedRequests}` / `${game.probeUnsupportedRequests}`\n")
      sb.append(s"- Themes: ${renderList(game.overallThemes)}\n")
      sb.append(s"- Visible moment plies: ${visibleMomentPliesText}\n\n")
      game.focusMoments.foreach { moment =>
        val dominantIdeaText = moment.dominantIdea.getOrElse("-")
        val campaignOwnerText = moment.campaignOwner.getOrElse("-")
        val compensationSubtypeText = moment.compensationSubtype.getOrElse("-")
        val executionText = moment.execution.getOrElse("-")
        val objectiveText = moment.objective.getOrElse("-")
        val focusText = moment.focus.getOrElse("-")
        val activeNoteText = moment.activeNote.getOrElse("<omitted>")
        sb.append(s"- Ply `${moment.ply}` `${moment.selectionKind}` `${moment.momentType}`\n")
        sb.append(s"  - Surface: dominant=`${dominantIdeaText}` owner=`${campaignOwnerText}` mismatch=`${moment.ownerMismatch}` compensation=`${moment.compensationPosition}` exemplar=`${moment.exemplarVisible}` subtype=`${compensationSubtypeText}`\n")
        sb.append(s"  - Execution / objective / focus: `${executionText}` / `${objectiveText}` / `${focusText}`\n")
        sb.append(s"  - Game Arc: ${moment.gameArcNarrative}\n")
        sb.append(s"  - Bookmaker (`${moment.bookmakerSourceMode}`): ${moment.bookmakerCommentary}\n")
        sb.append(s"  - Active (`${moment.activeNoteStatus}`): ${activeNoteText}\n")
      }
      sb.append("\n")
    }
    sb.toString()

  private def extractTheater(label: Option[String]): Option[String] =
    label.flatMap(_.split("/").headOption).map(_.trim).filter(_.nonEmpty)

  private def renderList(values: List[String]): String =
    if values.isEmpty then "-"
    else values.map(_.trim).filter(_.nonEmpty).mkString(", ")

  private def renderMap(values: Map[String, Int]): String =
    if values.isEmpty then "-"
    else values.toList.sortBy { case (key, _) => key }.map { case (key, value) => s"`$key=$value`" }.mkString(", ")

  private def mentionsCompensationContract(
      text: String,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    CompensationContractMatcher.mentionsCompensationContract(text, surface) ||
      Option(text).map(_.toLowerCase).exists { lower =>
        CompensationLexicon.exists(lower.contains)
      }

  private def unresolvedPathPayoffDivergence(
      path: String,
      payoff: String,
      selected: Option[String],
      displaySubtypeSource: String
  ): Boolean =
    path != payoff &&
      (displaySubtypeSource == "raw_fallback" || selected.forall(label => label != path && label != payoff))

  private def gameIdForMoment(
      games: List[GameReport],
      moment: FocusMomentReport
  ): String =
    games.collectFirst { case game if game.focusMoments.contains(moment) => game.id }.getOrElse("unknown")

  private def momentKey(gameId: String, moment: FocusMomentReport): String =
    s"$gameId:${moment.ply}"

  private def parseMomentKey(key: String): (Option[String], Option[Int]) =
    key.split(":").toList match
      case gameId :: ply :: Nil => (Some(gameId), ply.toIntOption)
      case _                    => (None, None)

  private def ratio(numerator: Int, denominator: Int): Double =
    if denominator <= 0 then 1.0 else numerator.toDouble / denominator.toDouble

  private def readCorpus(path: Path): Either[String, Corpus] =
    try
      val raw = Files.readString(path, StandardCharsets.UTF_8)
      Json.parse(raw).validate[Corpus].asEither.left.map(_.toString)
    catch case NonFatal(e) => Left(e.getMessage)

  private def readTruthInventory(
      path: Path
  ): Either[String, RealPgnNarrativeEvalTruthInventoryBuilder.TruthInventory] =
    try
      val raw = Files.readString(path, StandardCharsets.UTF_8)
      Json
        .parse(raw)
        .validate[RealPgnNarrativeEvalTruthInventoryBuilder.TruthInventory]
        .asEither
        .left
        .map(_.toString)
    catch case NonFatal(e) => Left(e.getMessage)

  private def writeJson(path: Path, value: JsValue): Unit =
    ensureParent(path)
    Files.writeString(path, Json.prettyPrint(value) + "\n", StandardCharsets.UTF_8)

  private def writeText(path: Path, text: String): Unit =
    ensureParent(path)
    Files.writeString(path, text, StandardCharsets.UTF_8)

  private def ensureParent(path: Path): Unit =
    Option(path.getParent).foreach(parent => Files.createDirectories(parent))

  private def parseHeaders(pgn: String): HeaderMeta =
    def tag(name: String): Option[String] =
      val pattern = ("\\[" + java.util.regex.Pattern.quote(name) + " \"([^\"]*)\"\\]").r
      pattern.findFirstMatchIn(pgn).map(_.group(1).trim).filter(_.nonEmpty)

    HeaderMeta(
      event = tag("Event"),
      site = tag("Site"),
      date = tag("Date"),
      round = tag("Round"),
      white = tag("White"),
      black = tag("Black"),
      result = tag("Result"),
      eco = tag("ECO"),
      opening = tag("Opening"),
      variation = tag("Variation")
    )

  private def combineOpening(header: HeaderMeta): Option[String] =
    List(header.opening, header.variation).flatten.filter(_.trim.nonEmpty) match
      case Nil          => header.eco
      case one :: Nil   => Some(one)
      case first :: second :: _ => Some(s"$first, $second")

  private def phaseFromPly(ply: Int, totalPlies: Int): String =
    if ply <= 20 then "opening"
    else if totalPlies > 0 && (totalPlies - ply) <= 12 then "endgame"
    else "middlegame"

  private def oneLine(text: String): String =
    Option(text).getOrElse("").replaceAll("""\s+""", " ").trim

  private def probeDedupKey(request: lila.llm.model.ProbeRequest): String =
    List(
      request.fen,
      request.moves.mkString(","),
      request.depth.toString,
      request.purpose.getOrElse(""),
      request.questionId.getOrElse(""),
      request.planId.getOrElse("")
    ).mkString("|")

  private def parseConfig(args: List[String]): Config =
    val positional = args.filterNot(_.startsWith("--"))
    val corpusPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultCorpusPath)
    val markdownPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultMarkdownPath)
    val jsonPath = positional.lift(2).map(Paths.get(_)).getOrElse(DefaultJsonPath)
    val rawDir = positional.lift(3).map(Paths.get(_)).getOrElse(DefaultRawDir)
    val depth = optionInt(args, "--depth").getOrElse(DefaultDepth).max(8)
    val multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(DefaultMultiPv).max(2)
    val enginePath =
      optionString(args, "--engine")
        .orElse(EngineEnvVars.iterator.flatMap(name => sys.env.get(name).map(_.trim).filter(_.nonEmpty)).toSeq.headOption)
        .map(Paths.get(_))
        .getOrElse {
          System.err.println(
            s"[real-pgn-eval] missing engine path. Set one of ${EngineEnvVars.mkString(", ")} or pass --engine /path/to/uci-engine."
          )
          sys.exit(1)
        }
    Config(
      corpusPath = corpusPath,
      markdownPath = markdownPath,
      jsonPath = jsonPath,
      rawDir = rawDir,
      depth = depth,
      multiPv = multiPv,
      enginePath = enginePath
    )

  private def optionString(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst { case List(flag, value) if flag == name => value }.map(_.trim).filter(_.nonEmpty)

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)

  private final case class ParsedInfo(
      depth: Int,
      multiPv: Int,
      scoreType: String,
      scoreValue: Int,
      moves: List[String]
  )

  private final class LocalUciEngine(enginePath: Path, timeoutMs: Long):
    private val process =
      new ProcessBuilder(enginePath.toAbsolutePath.normalize.toString)
        .redirectErrorStream(true)
        .start()
    private val lines = LinkedBlockingQueue[String]()
    private val writer =
      new BufferedWriter(new OutputStreamWriter(process.getOutputStream, StandardCharsets.UTF_8))
    private val reader = new Thread(() => pumpOutput(), "real-pgn-eval-engine")
    @volatile private var closed = false
    private var resolvedEngineName = enginePath.getFileName.toString

    reader.setDaemon(true)
    reader.start()
    initialize()

    def engineName: String = resolvedEngineName

    def newGame(): Unit =
      send("ucinewgame")
      ready()

    def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] =
      drainPending()
      send(s"setoption name MultiPV value $multiPv")
      send(s"position fen $fen")
      send(s"go depth $depth")

      val perspectiveSign = whitePerspectiveSign(fen)
      val byPv = scala.collection.mutable.Map.empty[Int, ParsedInfo]
      var bestMove: Option[String] = None
      val deadline = System.nanoTime() + timeoutMs * 1000000L
      var done = false

      while !done do
        val line = awaitLine(deadline)
        if line.startsWith("info ") then
          parseInfoLine(line).foreach { info =>
            val prev = byPv.get(info.multiPv)
            if prev.forall(p => info.depth > p.depth || (info.depth == p.depth && info.moves.size >= p.moves.size)) then
              byPv.update(info.multiPv, info)
          }
        else if line.startsWith("bestmove") then
          bestMove =
            line.split("\\s+").lift(1).map(_.trim).filter(move => move.nonEmpty && move != "(none)")
          done = true

      val normalized =
        byPv.toList.sortBy(_._1).map { case (_, info) => normalizeLine(info, perspectiveSign) }
      if normalized.nonEmpty then normalized
      else bestMove.toList.map(move => VariationLine(moves = List(move), scoreCp = 0, mate = None, depth = 0))

    def close(): Unit =
      if !closed then
        closed = true
        try send("quit")
        catch case _: Throwable => ()
        writer.close()
        if process.isAlive then process.destroy()

    private def initialize(): Unit =
      send("uci")
      val deadline = System.nanoTime() + timeoutMs * 1000000L
      var uciOk = false
      while !uciOk do
        val line = awaitLine(deadline)
        if line.startsWith("id name ") then resolvedEngineName = line.stripPrefix("id name ").trim
        else if line == "uciok" then uciOk = true
      send("setoption name Threads value 1")
      send("setoption name Hash value 64")
      ready()

    private def ready(): Unit =
      send("isready")
      val deadline = System.nanoTime() + timeoutMs * 1000000L
      var isReady = false
      while !isReady do
        val line = awaitLine(deadline)
        if line == "readyok" then isReady = true

    private def pumpOutput(): Unit =
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream, StandardCharsets.UTF_8))
      try
        Iterator.continually(reader.readLine()).takeWhile(_ != null).foreach(lines.put)
      finally reader.close()

    private def send(command: String): Unit =
      writer.write(command)
      writer.newLine()
      writer.flush()

    private def awaitLine(deadlineNs: Long): String =
      val remainingMs = ((deadlineNs - System.nanoTime()) / 1000000L).max(1L)
      val line = lines.poll(remainingMs, TimeUnit.MILLISECONDS)
      if line == null then throw new RuntimeException("engine timeout")
      line.trim

    private def drainPending(): Unit =
      while lines.poll() != null do ()

    private def parseInfoLine(line: String): Option[ParsedInfo] =
      val tokens = line.split("\\s+").toList
      if !tokens.contains("score") then None
      else
        def valueAfter(name: String): Option[String] =
          tokens.sliding(2).collectFirst { case List(flag, value) if flag == name => value }

        val pvIdx = tokens.indexOf("pv")
        val moves =
          if pvIdx >= 0 then tokens.drop(pvIdx + 1).map(_.trim).filter(_.nonEmpty)
          else Nil

        for
          depth <- valueAfter("depth").flatMap(_.toIntOption)
          multiPv = valueAfter("multipv").flatMap(_.toIntOption).getOrElse(1)
          scoreIdx = tokens.indexOf("score")
          scoreType <- tokens.lift(scoreIdx + 1)
          scoreValue <- tokens.lift(scoreIdx + 2).flatMap(_.toIntOption)
        yield ParsedInfo(depth, multiPv, scoreType, scoreValue, moves)

    private def normalizeLine(info: ParsedInfo, perspectiveSign: Int): VariationLine =
      val scoreCp =
        if info.scoreType == "cp" then info.scoreValue * perspectiveSign
        else 0
      val mate =
        if info.scoreType == "mate" then Some(info.scoreValue * perspectiveSign)
        else None
      VariationLine(moves = info.moves, scoreCp = scoreCp, mate = mate, depth = info.depth)

    private def whitePerspectiveSign(fen: String): Int =
      fen.split("\\s+").lift(1).map(_.trim) match
        case Some("w") => 1
        case Some("b") => -1
        case _         => 1

private[tools] object RealPgnNarrativeEvalCalibration:

  private val TechnicalTailMomentTypes = Set("MatePivot", "TensionPeak", "AdvantageSwing")
  private val DynamicCompensationTerms = List("initiative", "line pressure", "delayed recovery", "return vector")
  private val TruthBoundInvestmentLabels = Set("WinningInvestment", "CompensatedInvestment")

  def compensationEvalPosition(
      moment: GameChronicleMoment,
      gameArcSurface: StrategyPackSurface.Snapshot,
      bookmakerSurface: StrategyPackSurface.Snapshot,
      playedMove: Option[String] = None
  ): Boolean =
    if truthBoundInvestmentCompensation(moment, gameArcSurface, bookmakerSurface) then true
    else if !gameArcSurface.compensationPosition then false
    else if !bookmakerSurface.compensationPosition then false
    else if !surfaceSupportsCompensationContract(gameArcSurface) then false
    else if !surfaceSupportsCompensationContract(bookmakerSurface) then false
    else if !sharedCompensationSubtype(gameArcSurface, bookmakerSurface) then false
    else if recaptureNeutralizedCompensation(moment, gameArcSurface, bookmakerSurface, playedMove) then false
    else if lateTechnicalSpaceOnlyCompensation(moment, gameArcSurface, bookmakerSurface) then false
    else if lateTechnicalStaticTailCompensation(moment, gameArcSurface, bookmakerSurface) then false
    else if lateTransitionOnlyCompensation(moment, gameArcSurface, bookmakerSurface) then false
    else true

  def exemplarEvalPosition(
      moment: GameChronicleMoment,
      gameArcSurface: StrategyPackSurface.Snapshot,
      bookmakerSurface: StrategyPackSurface.Snapshot,
      internalTruth: Option[CommentaryEngine.TruthTraceMoment] = None
  ): Boolean =
    if truthBoundInvestmentCompensation(moment, gameArcSurface, bookmakerSurface) then true
    else if internalTruth.exists(_.maintenanceExemplarCandidate) then true
    else
      moment.moveClassification.exists(TruthBoundInvestmentLabels.contains) ||
        moment.momentType == "InvestmentPivot"

  def lateTechnicalSpaceOnlyCompensation(
      moment: GameChronicleMoment,
      gameArcSurface: StrategyPackSurface.Snapshot,
      bookmakerSurface: StrategyPackSurface.Snapshot
  ): Boolean =
    val gameArcSpaceOnly = compensationVectorsAreSpaceOnly(gameArcSurface)
    val bookmakerSpaceOnly = compensationVectorsAreSpaceOnly(bookmakerSurface)
    val heavyInvestment =
      gameArcSurface.investedMaterial.exists(_ >= 500) ||
        bookmakerSurface.investedMaterial.exists(_ >= 500)
    technicalTail(moment) && heavyInvestment && gameArcSpaceOnly && bookmakerSpaceOnly

  def compensationVectorsAreSpaceOnly(surface: StrategyPackSurface.Snapshot): Boolean =
    surface.compensationVectors.nonEmpty &&
      surface.compensationVectors.forall(_.toLowerCase.contains("space advantage"))

  def lateTechnicalStaticTailCompensation(
      moment: GameChronicleMoment,
      gameArcSurface: StrategyPackSurface.Snapshot,
      bookmakerSurface: StrategyPackSurface.Snapshot
  ): Boolean =
    technicalTail(moment) &&
      !surfaceHasDynamicCompensation(gameArcSurface) &&
      !surfaceHasDynamicCompensation(bookmakerSurface)

  def lateTransitionOnlyCompensation(
      moment: GameChronicleMoment,
      gameArcSurface: StrategyPackSurface.Snapshot,
      bookmakerSurface: StrategyPackSurface.Snapshot
  ): Boolean =
    technicalTail(moment) &&
      gameArcSurface.compensationSubtype.exists(_.transitionOnly) &&
      bookmakerSurface.compensationSubtype.exists(_.transitionOnly)

  def recaptureNeutralizedCompensation(
      moment: GameChronicleMoment,
      gameArcSurface: StrategyPackSurface.Snapshot,
      bookmakerSurface: StrategyPackSurface.Snapshot,
      playedMove: Option[String]
  ): Boolean =
    val investedMaterial =
      (gameArcSurface.investedMaterial.toList ++ bookmakerSurface.investedMaterial.toList).maxOption.getOrElse(0)
    playedMove.exists(move =>
      CompensationRecaptureGate.suppressAfterCompensation(moment.fen, move, investedMaterial)
    )

  private def surfaceHasDynamicCompensation(surface: StrategyPackSurface.Snapshot): Boolean =
    (surface.compensationSummary.toList ++ surface.compensationVectors)
      .map(_.toLowerCase)
      .exists(text => DynamicCompensationTerms.exists(text.contains))

  private def truthBoundInvestmentCompensation(
      moment: GameChronicleMoment,
      gameArcSurface: StrategyPackSurface.Snapshot,
      bookmakerSurface: StrategyPackSurface.Snapshot
  ): Boolean =
    (moment.moveClassification.exists(TruthBoundInvestmentLabels.contains) || moment.momentType == "InvestmentPivot") &&
      gameArcSurface.compensationPosition &&
      bookmakerSurface.compensationPosition &&
      sharedCompensationSubtype(gameArcSurface, bookmakerSurface)

  private def technicalTail(moment: GameChronicleMoment): Boolean =
    moment.moveNumber >= 35 && TechnicalTailMomentTypes.contains(moment.momentType)

  private def surfaceSupportsCompensationContract(
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    surface.strictCompensationPosition && surface.compensationContractResolved

  private def sharedCompensationSubtype(
      gameArcSurface: StrategyPackSurface.Snapshot,
      bookmakerSurface: StrategyPackSurface.Snapshot
  ): Boolean =
    StrategyPackSurface
      .strictCompensationSubtypeLabel(gameArcSurface)
      .zip(StrategyPackSurface.strictCompensationSubtypeLabel(bookmakerSurface))
      .exists { case (gameArcSubtype, bookmakerSubtype) => gameArcSubtype == bookmakerSubtype }
