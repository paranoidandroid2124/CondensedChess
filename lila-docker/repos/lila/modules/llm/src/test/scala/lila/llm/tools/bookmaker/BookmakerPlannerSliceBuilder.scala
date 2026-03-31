package lila.llm.tools.bookmaker

import java.nio.file.{ Files, Path, Paths }

import lila.llm.{ MoveEval, PgnAnalysisHelper }
import lila.llm.analysis.NarrativeUtils
import lila.llm.tools.review.CommentaryPlayerQcSupport

import scala.util.control.NonFatal

object BookmakerPlannerSliceBuilder:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      catalogPath: Path = DefaultCatalogDir.resolve("catalog.jsonl"),
      outPath: Path = DefaultManifestDir.resolve("bookmaker_planner_signoff_slice.jsonl"),
      depth: Int = 8,
      multiPv: Int = 2,
      perKindTarget: Int = 2,
      negativeTarget: Int = 4,
      minPly: Int = 6,
      maxGames: Int = Int.MaxValue,
      enginePath: Path
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val catalog =
      readJsonLines[CatalogEntry](config.catalogPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[bookmaker-planner-slice] failed to read `${config.catalogPath}`: $err")
          sys.exit(1)

    if catalog.isEmpty then
      System.err.println(s"[bookmaker-planner-slice] no catalog entries in `${config.catalogPath}`")
      sys.exit(1)

    val engine = new LocalUciEngine(config.enginePath, timeoutMs = 30000L)
    try
      val selected = scala.collection.mutable.ListBuffer.empty[SliceManifestEntry]
      val counts = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
      val seenSampleIds = scala.collection.mutable.Set.empty[String]

      catalog.iterator.take(config.maxGames).foreach { entry =>
        if !targetsMet(counts.toMap, config) then
          try
            selectFromGame(entry, engine, config).foreach { case (slice, category) =>
              val limit = if category == "negative" then config.negativeTarget else config.perKindTarget
              if counts(category) < limit && !seenSampleIds(slice.sampleId) then
                selected += slice
                seenSampleIds += slice.sampleId
                counts.update(category, counts(category) + 1)
            }
          catch
            case NonFatal(err) =>
              println(s"[bookmaker-planner-slice] skipped game `${entry.gameKey}`: ${err.getMessage}")
      }

      writeJsonLines(config.outPath, selected.toList)
      println(
        s"[bookmaker-planner-slice] wrote `${config.outPath}` (samples=${selected.size}, counts=${counts.toMap.toList.sortBy(_._1).mkString(", ")})"
      )
      if !targetsMet(counts.toMap, config) then
        println(
          s"[bookmaker-planner-slice] target incomplete after scanning ${config.maxGames.min(catalog.size)} games: ${counts.toMap}"
        )
    finally engine.close()

  private def selectFromGame(
      entry: CatalogEntry,
      engine: LocalUciEngine,
      config: Config
  ): List[(SliceManifestEntry, String)] =
    val pgn = Files.readString(Paths.get(entry.pgnPath))
    val plies =
      PgnAnalysisHelper.extractPlyDataStrict(pgn) match
        case Right(value) => value.filter(_.ply >= config.minPly)
        case Left(err) =>
          throw new IllegalArgumentException(s"${entry.gameKey}: invalid PGN: $err")

    plies.flatMap { plyData =>
      engine.newGame()
      val beforeVars = engine.analyze(plyData.fen, config.depth, config.multiPv)
      val afterFen = NarrativeUtils.uciListToFen(plyData.fen, List(plyData.playedUci))
      engine.newGame()
      val afterVars = engine.analyze(afterFen, config.depth, config.multiPv)
      val afterBest = afterVars.headOption
      analyzePly(
        entry,
        plyData,
        beforeVars,
        afterBest.map(best =>
          MoveEval(
            ply = plyData.ply,
            cp = best.scoreCp,
            mate = best.mate,
            pv = best.moves,
            variations = afterVars
          )
        )
      ).flatMap { snapshot =>
        val runtime = bookmakerPlannerRuntime(snapshot)
        categoryOf(runtime.planner).map { category =>
          buildSliceEntry(entry, plyData, category) -> category
        }
      }
    }

  private def buildSliceEntry(
      entry: CatalogEntry,
      plyData: PgnAnalysisHelper.PlyData,
      category: String
  ): SliceManifestEntry =
    val normalizedCategory =
      category
        .replaceAll("([a-z])([A-Z])", "$1_$2")
        .toLowerCase
    SliceManifestEntry(
      sampleId = s"${entry.gameKey}:signoff_${normalizedCategory}:${plyData.ply}:bookmaker",
      gameKey = entry.gameKey,
      surface = "bookmaker",
      sliceKind = s"signoff_${normalizedCategory}",
      targetPly = plyData.ply,
      fen = plyData.fen,
      playedSan = plyData.playedMove,
      opening = entry.opening,
      tags = (entry.familyTags :+ "bookmaker_signoff" :+ normalizedCategory).distinct,
      pgnPath = entry.pgnPath,
      playedUci = plyData.playedUci,
      variant = entry.variant,
      mixBucket = Some(entry.mixBucket)
    )

  private def categoryOf(trace: BookmakerPlannerTrace): Option[String] =
    val plannerOwned = trace.bookmakerFallbackMode == "planner_owned"
    trace.primaryKind match
      case Some(kind) if plannerOwned && coreKinds.contains(kind) => Some(kind)
      case _ if trace.bookmakerFallbackMode == "exact_factual" ||
          trace.primaryFallbackMode.exists(_ != "PlannerOwned")   => Some("negative")
      case _                                                     => None

  private val coreKinds =
    Set("WhyThis", "WhyNow", "WhatChanged", "WhatMustBeStopped", "WhosePlanIsFaster")

  private def targetsMet(counts: Map[String, Int], config: Config): Boolean =
    coreKinds.forall(kind => counts.getOrElse(kind, 0) >= config.perKindTarget) &&
    counts.getOrElse("negative", 0) >= config.negativeTarget

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    val enginePath =
      optionString(args, "--engine")
        .orElse(sys.env.get("LLM_ACTIVE_CORPUS_ENGINE_PATH").map(_.trim).filter(_.nonEmpty))
        .orElse(sys.env.get("STOCKFISH_BIN").map(_.trim).filter(_.nonEmpty))
        .map(Paths.get(_))
        .getOrElse {
          System.err.println("[bookmaker-planner-slice] missing engine path. Pass `--engine /path/to/uci-engine`.")
          sys.exit(1)
        }
    Config(
      catalogPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultCatalogDir.resolve("catalog.jsonl")),
      outPath =
        positional.lift(1).map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("bookmaker_planner_signoff_slice.jsonl")),
      depth = optionInt(args, "--depth").getOrElse(8).max(6),
      multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(2).max(1),
      perKindTarget = optionInt(args, "--per-kind").getOrElse(2).max(1),
      negativeTarget = optionInt(args, "--negative").getOrElse(4).max(1),
      minPly = optionInt(args, "--min-ply").getOrElse(6).max(1),
      maxGames = optionInt(args, "--max-games").getOrElse(Int.MaxValue).max(1),
      enginePath = enginePath
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue =
      Set("--engine", "--depth", "--multi-pv", "--multiPv", "--per-kind", "--negative", "--min-ply", "--max-games")
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
