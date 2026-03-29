package lila.llm.tools

import java.nio.file.{ Path, Paths }

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import play.api.libs.json.Json
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.*
import lila.llm.analysis.OpeningExplorerClient

object CommentaryPlayerSliceManifestBuilder:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      catalogPath: Path = DefaultCatalogDir.resolve("catalog.jsonl"),
      manifestPath: Path = DefaultManifestDir.resolve("slice_manifest.jsonl"),
      chronicleCorpusPath: Path = DefaultManifestDir.resolve("chronicle_corpus.json"),
      depth: Int = 10,
      multiPv: Int = 3,
      mixBucket: Option[String] = None,
      offset: Int = 0,
      limit: Option[Int] = None,
      enginePath: Path
  )

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("commentary-player-slice-manifest-builder")

    val config = parseConfig(args.toList)
    val catalog =
      readJsonLines[CatalogEntry](config.catalogPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[player-qc-slices] failed to read catalog `${config.catalogPath}`: $err")
          sys.exit(1)

    if catalog.isEmpty then
      System.err.println(s"[player-qc-slices] catalog `${config.catalogPath}` is empty")
      sys.exit(1)

    val filteredCatalog = applyShardFilter(catalog, config)
    if filteredCatalog.isEmpty then
      System.err.println(
        s"[player-qc-slices] shard filter produced an empty catalog (mixBucket=${config.mixBucket.getOrElse("all")}, offset=${config.offset}, limit=${config.limit.getOrElse("all")})"
      )
      sys.exit(1)

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
    val engine = new LocalUciEngine(config.enginePath, timeoutMs = 30000L)
    val chronicleEngine = new RealPgnNarrativeEvalRunner.LocalUciEngine(config.enginePath, timeoutMs = 30000L)
    try
      val manifest =
        filteredCatalog.zipWithIndex.flatMap { case (entry, idx) =>
          if idx % 5 == 0 then
            println(
              s"[player-qc-slices] analyzing ${idx + 1}/${filteredCatalog.size}: ${entry.gameKey} (${entry.mixBucket})"
            )
          val pgn = java.nio.file.Files.readString(Paths.get(entry.pgnPath))
          val plyData =
            lila.llm.PgnAnalysisHelper.extractPlyDataStrict(pgn) match
              case Right(value) => value
              case Left(err) =>
                throw new IllegalArgumentException(s"${entry.gameKey}: invalid PGN in catalog: $err")

          val afterMoveEvals = buildAfterMoveEvals(plyData, engine, config.depth, config.multiPv)
          val evalByPly = afterMoveEvals.map(eval => eval.ply -> eval).toMap
          val analyzed =
            plyData.flatMap { pd =>
              val beforeVars =
                evalByPly.get(pd.ply - 1).map(_.getVariations).filter(_.nonEmpty).getOrElse {
                  engine.analyze(pd.fen, config.depth, config.multiPv)
                }
              analyzePly(entry, pd, beforeVars, evalByPly.get(pd.ply))
            }
          val rawSlices = selectSlices(analyzed)
          val questionWhyNow =
            Option.when(rawSlices.exists(_._1 == SliceKind.QuestionWhyNow)) {
              selectVisibleQuestionWhyNowSnapshot(entry, pgn, analyzed, api, chronicleEngine, config)
            }.flatten
          val selectedSlices =
            rawSlices.filterNot(_._1 == SliceKind.QuestionWhyNow) ++
              questionWhyNow.toList.map(SliceKind.QuestionWhyNow -> _)
          selectedSlices.flatMap { case (sliceKind, snapshot) =>
            manifestEntriesFor(sliceKind, snapshot)
          }
        }

      writeJsonLines(config.manifestPath, manifest)
      writeJson(config.chronicleCorpusPath, Json.toJson(chronicleCorpusFromCatalog(filteredCatalog)))

      val counts =
        manifest.groupBy(_.sliceKind).view.mapValues(_.size / 2).toMap
      println(
        s"[player-qc-slices] wrote `${config.manifestPath}` (entries=${manifest.size}, pairedSlices=${manifest.size / 2}, catalogGames=${filteredCatalog.size}, counts=${counts.toSeq.sortBy(_._1).mkString(", ")})"
      )
    finally
      engine.close()
      chronicleEngine.close()
      ws.close()
      summon[ActorSystem].terminate()

  private def selectVisibleQuestionWhyNowSnapshot(
      entry: CatalogEntry,
      pgn: String,
      analyzed: List[SliceSnapshot],
      api: LlmApi,
      engine: RealPgnNarrativeEvalRunner.LocalUciEngine,
      config: Config
  ): Option[SliceSnapshot] =
    val artifacts =
      RealPgnNarrativeEvalRunner.analyzeChronicleGame(
        pgn = pgn,
        api = api,
        engine = engine,
        depth = config.depth,
        multiPv = config.multiPv,
        gameId = Some(entry.gameKey)
      )
    val visibleMomentsByPly = artifacts.response.moments.map(moment => moment.ply -> moment).toMap
    val threadsById = artifacts.internalResponse.strategicThreads.map(thread => thread.threadId -> thread).toMap
    val carriedVisibleWhyNowPlies =
      artifacts.internalResponse.moments.flatMap { internalMoment =>
        visibleMomentsByPly.get(internalMoment.ply).flatMap { visibleMoment =>
          val momentState =
            ChronicleActivePlannerSliceRunner.analyzeMoment(
              gameKey = entry.gameKey,
              mixBucket = entry.mixBucket,
              internalMoment = internalMoment,
              visibleMoment = Some(visibleMoment),
              threadsById = threadsById,
              api = api
            )
          val carriesWhyNow =
            momentState.authorQuestionKinds.contains("WhyNow") || momentState.authorEvidenceKinds.contains("WhyNow")
          val surfacedWhyNow =
            momentState.chroniclePrimaryKind.contains("WhyNow") || momentState.activePrimaryKind.contains("WhyNow")
          Option.when(carriesWhyNow && surfacedWhyNow)(internalMoment.ply)
        }
      }.toSet
    selectQuestionWhyNowSnapshot(analyzed, allowedPlies = Some(carriedVisibleWhyNowPlies))

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    val enginePath =
      optionString(args, "--engine")
        .orElse(sys.env.get("LLM_ACTIVE_CORPUS_ENGINE_PATH").map(_.trim).filter(_.nonEmpty))
        .orElse(sys.env.get("STOCKFISH_BIN").map(_.trim).filter(_.nonEmpty))
        .map(Paths.get(_))
        .getOrElse {
          System.err.println("[player-qc-slices] missing engine path. Pass `--engine /path/to/uci-engine`.")
          sys.exit(1)
        }
    Config(
      catalogPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultCatalogDir.resolve("catalog.jsonl")),
      manifestPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("slice_manifest.jsonl")),
      chronicleCorpusPath =
        positional.lift(2).map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("chronicle_corpus.json")),
      depth = optionInt(args, "--depth").getOrElse(10).max(8),
      multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(3).max(1),
      mixBucket = optionString(args, "--mix-bucket").map(_.trim).filter(_.nonEmpty),
      offset = optionInt(args, "--offset").getOrElse(0).max(0),
      limit = optionInt(args, "--limit").filter(_ > 0),
      enginePath = enginePath
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue = Set("--engine", "--depth", "--multi-pv", "--multiPv", "--mix-bucket", "--offset", "--limit")
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

  private def applyShardFilter(catalog: List[CatalogEntry], config: Config): List[CatalogEntry] =
    val byBucket =
      config.mixBucket
        .map(bucket => catalog.filter(_.mixBucket == bucket))
        .getOrElse(catalog)
        .sortBy(_.gameKey)
    config.limit match
      case Some(limit) => byBucket.drop(config.offset).take(limit)
      case None        => byBucket.drop(config.offset)
