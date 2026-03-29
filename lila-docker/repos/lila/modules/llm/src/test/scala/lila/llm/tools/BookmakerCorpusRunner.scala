package lila.llm.tools

import java.nio.file.{ Files, Path, Paths }

import scala.concurrent.{ Await, ExecutionContext }
import akka.actor.ActorSystem
import play.api.libs.json.Json
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.*
import lila.llm.analysis.OpeningExplorerClient

object BookmakerCorpusRunner:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      manifestPath: Path = DefaultManifestDir.resolve("slice_manifest.jsonl"),
      outPath: Path = DefaultBookmakerRunDir.resolve("bookmaker_outputs.jsonl"),
      rawDir: Path = DefaultBookmakerRunDir.resolve("raw"),
      depth: Int = 10,
      multiPv: Int = 3,
      enginePath: Path
  )

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("bookmaker-corpus-runner")

    val config = parseConfig(args.toList)
    val entries =
      readJsonLines[SliceManifestEntry](config.manifestPath) match
        case Right(value) => value.filter(_.surface == "bookmaker")
        case Left(err) =>
          System.err.println(s"[bookmaker-corpus] failed to read manifest `${config.manifestPath}`: $err")
          sys.exit(1)

    if entries.isEmpty then
      System.err.println(s"[bookmaker-corpus] no bookmaker entries in `${config.manifestPath}`")
      sys.exit(1)

    ensureDir(config.rawDir)
    val engine = new LocalUciEngine(config.enginePath, timeoutMs = 30000L)
    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    val api =
      LlmApi(
        openingExplorer = OpeningExplorerClient(ws),
        geminiClient = GeminiClient(ws, GeminiConfig.fromEnv),
        openAiClient = OpenAiClient(ws, OpenAiConfig.fromEnv),
        commentaryCache = CommentaryCache(),
        llmConfig = LlmConfig.fromEnv,
        providerConfig = LlmProviderConfig.fromEnv
      )

    try
      val outputs =
        entries.map { entry =>
          val catalogEntry = loadCatalogEntry(entry)
          val pgn = Files.readString(Paths.get(entry.pgnPath))
          val plyData = PgnAnalysisHelper.extractPlyDataStrict(pgn) match
            case Right(value) =>
              value.find(_.ply == entry.targetPly).getOrElse {
                throw new IllegalArgumentException(s"${entry.sampleId}: missing ply ${entry.targetPly}")
              }
            case Left(err) =>
              throw new IllegalArgumentException(s"${entry.sampleId}: invalid PGN: $err")

          val beforeVars = engine.analyze(entry.fen, config.depth, config.multiPv)
          val afterFen = lila.llm.analysis.NarrativeUtils.uciListToFen(entry.fen, List(entry.playedUci))
          val afterVars = engine.analyze(afterFen, config.depth, config.multiPv)
          val afterBest = afterVars.headOption
          val plannerTrace =
            analyzePly(
              catalogEntry,
              plyData,
              beforeVars,
              afterBest.map(best =>
                MoveEval(
                  ply = entry.targetPly,
                  cp = best.scoreCp,
                  mate = best.mate,
                  pv = best.moves,
                  variations = afterVars
                )
              )
            ).map(bookmakerPlannerTrace).getOrElse(BookmakerPlannerTrace())
          val result =
            Await.result(
              api.bookmakerCommentPosition(
                fen = entry.fen,
                lastMove = Some(entry.playedUci),
                eval = beforeVars.headOption.map(v => EvalData(cp = v.scoreCp, mate = v.mate, pv = Some(v.moves))),
                variations = Some(beforeVars),
                probeResults = None,
                openingData = minimalOpeningReference(catalogEntry),
                afterFen = Some(afterFen),
                afterEval = afterBest.map(v => EvalData(cp = v.scoreCp, mate = v.mate, pv = Some(v.moves))),
                afterVariations = Some(afterVars),
                opening = entry.opening.orElse(catalogEntry.opening),
                phase = guessPhase(entry.fen, entry.targetPly),
                ply = entry.targetPly,
                variant = Some(entry.variant),
                prevStateToken = None,
                prevEndgameStateToken = None,
                allowLlmPolish = true,
                lang = "en",
                planTier = PlanTier.Pro,
                llmLevel = LlmLevel.Polish
              ),
              180.seconds
            ).getOrElse(throw new IllegalStateException(s"${entry.sampleId}: empty bookmaker response"))

          val rawPath = config.rawDir.resolve(s"${entry.sampleId.replace(':', '_')}.json")
          writeJson(rawPath, Json.toJson(result.response))
          val (supportRows, advancedRows) = buildBookmakerRows(result.response)

          BookmakerOutputEntry(
            sampleId = entry.sampleId,
            gameKey = entry.gameKey,
            sliceKind = entry.sliceKind,
            targetPly = entry.targetPly,
            fen = entry.fen,
            playedSan = entry.playedSan,
            playedUci = entry.playedUci,
            opening = entry.opening.orElse(catalogEntry.opening),
            commentary = result.response.commentary,
            supportRows = supportRows,
            advancedRows = advancedRows,
            sourceMode = result.response.sourceMode,
            model = result.response.model,
            rawResponsePath = rawPath.toAbsolutePath.normalize.toString,
            variationCount = result.response.variations.size,
            cacheHit = result.cacheHit,
            plannerPrimaryKind = plannerTrace.primaryKind,
            plannerPrimaryFallbackMode = plannerTrace.primaryFallbackMode,
            plannerSecondaryKind = plannerTrace.secondaryKind,
            plannerSecondarySurfaced = plannerTrace.secondarySurfaced,
            bookmakerFallbackMode = plannerTrace.bookmakerFallbackMode,
            plannerSceneType = plannerTrace.sceneType,
            plannerOwnerCandidates = plannerTrace.ownerCandidates,
            plannerAdmittedFamilies = plannerTrace.admittedFamilies,
            plannerDroppedFamilies = plannerTrace.droppedFamilies,
            plannerSupportMaterialSeparation = plannerTrace.supportMaterialSeparation,
            plannerProposedFamilyMappings = plannerTrace.proposedFamilyMappings,
            plannerDemotionReasons = plannerTrace.demotionReasons,
            plannerSelectedQuestion = plannerTrace.selectedQuestion,
            plannerSelectedOwnerFamily = plannerTrace.selectedOwnerFamily,
            plannerSelectedOwnerSource = plannerTrace.selectedOwnerSource,
            surfaceReplayOutcome = plannerTrace.surfaceReplayOutcome
          )
        }

      writeJsonLines(config.outPath, outputs)
      println(s"[bookmaker-corpus] wrote `${config.outPath}` (samples=${outputs.size})")
    finally
      engine.close()
      ws.close()
      summon[ActorSystem].terminate()

  private def loadCatalogEntry(entry: SliceManifestEntry): CatalogEntry =
    val pgn = Files.readString(Paths.get(entry.pgnPath))
    val headers = parseHeaders(pgn)
    CatalogEntry(
      gameKey = entry.gameKey,
      source = entry.pgnPath,
      sourceId = entry.gameKey,
      pgnPath = entry.pgnPath,
      mixBucket = entry.mixBucket.getOrElse(MixBucket.Club),
      familyTags = entry.tags,
      ratingBucket = ratingBucketOf(headers),
      timeControlBucket = timeControlBucketOf(headers),
      openingMacroFamily = openingMacroFamily(headers),
      opening = openingLabel(headers),
      eco = headers.get("ECO"),
      variation = headers.get("Variation"),
      white = headers.get("White"),
      black = headers.get("Black"),
      event = headers.get("Event"),
      date = headers.get("Date"),
      round = headers.get("Round"),
      result = headers.get("Result"),
      timeControl = headers.get("TimeControl"),
      variant = headers.getOrElse("Variant", entry.variant)
    )

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    val enginePath =
      optionString(args, "--engine")
        .orElse(sys.env.get("LLM_ACTIVE_CORPUS_ENGINE_PATH").map(_.trim).filter(_.nonEmpty))
        .orElse(sys.env.get("STOCKFISH_BIN").map(_.trim).filter(_.nonEmpty))
        .map(Paths.get(_))
        .getOrElse {
          System.err.println("[bookmaker-corpus] missing engine path. Pass `--engine /path/to/uci-engine`.")
          sys.exit(1)
        }
    Config(
      manifestPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("slice_manifest.jsonl")),
      outPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultBookmakerRunDir.resolve("bookmaker_outputs.jsonl")),
      rawDir = positional.lift(2).map(Paths.get(_)).getOrElse(DefaultBookmakerRunDir.resolve("raw")),
      depth = optionInt(args, "--depth").getOrElse(10).max(8),
      multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(3).max(1),
      enginePath = enginePath
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue = Set("--engine", "--depth", "--multi-pv", "--multiPv")
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
    args.sliding(2).collectFirst { case List(flag, value) if flag == name => value.trim }.filter(_.nonEmpty)

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)
