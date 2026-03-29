package lila.llm.tools

import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.{ Json, Writes }

import lila.llm.{ MoveEval, PgnAnalysisHelper }
import scala.util.control.NonFatal

object BookmakerPlannerSignoffRunner:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      manifestPath: Path = DefaultManifestDir.resolve("slice_manifest.jsonl"),
      outPath: Path = DefaultBookmakerRunDir.resolve("bookmaker_planner_signoff.jsonl"),
      depth: Int = 8,
      multiPv: Int = 2,
      perKindTarget: Int = 2,
      negativeTarget: Int = 4,
      maxScan: Int = 120,
      enginePath: Path
  )

  final case class SignoffEntry(
      sampleId: String,
      gameKey: String,
      sliceKind: String,
      targetPly: Int,
      playedSan: String,
      plannerPrimaryKind: Option[String],
      plannerPrimaryFallbackMode: Option[String],
      plannerSecondaryKind: Option[String],
      plannerSecondarySurfaced: Boolean,
      bookmakerFallbackMode: String,
      prose: String
  )
  object SignoffEntry:
    given Writes[SignoffEntry] = Json.writes[SignoffEntry]

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val entries =
      readJsonLines[SliceManifestEntry](config.manifestPath) match
        case Right(value) => value.filter(_.surface == "bookmaker")
        case Left(err) =>
          System.err.println(s"[bookmaker-planner-signoff] failed to read `${config.manifestPath}`: $err")
          sys.exit(1)

    if entries.isEmpty then
      System.err.println(s"[bookmaker-planner-signoff] no bookmaker entries in `${config.manifestPath}`")
      sys.exit(1)

    val engine = new LocalUciEngine(config.enginePath, timeoutMs = 30000L)
    try
      val selected = scala.collection.mutable.ListBuffer.empty[SignoffEntry]
      val counts = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
      val observed = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)

      entries.iterator.take(config.maxScan).foreach { entry =>
        if !targetsMet(counts.toMap, config) then
          try
            analyzeEntry(entry, engine, config).foreach { signoff =>
              val observedKey =
                s"${signoff.plannerPrimaryKind.getOrElse("none")}|${signoff.bookmakerFallbackMode}"
              observed.update(observedKey, observed(observedKey) + 1)
              categoryOf(signoff).foreach { category =>
                val limit =
                  if category == "negative" then config.negativeTarget
                  else config.perKindTarget
                if counts(category) < limit then
                  selected += signoff
                  counts.update(category, counts(category) + 1)
              }
            }
          catch
            case NonFatal(err) =>
              println(s"[bookmaker-planner-signoff] skipped `${entry.sampleId}`: ${err.getMessage}")
      }

      writeJsonLines(config.outPath, selected.toList)
      println(
        s"[bookmaker-planner-signoff] wrote `${config.outPath}` (samples=${selected.size}, counts=${counts.toMap.toList.sortBy(_._1).mkString(", ")})"
      )
      println(
        s"[bookmaker-planner-signoff] observed primary/fallback states: ${
            observed.toMap.toList.sortBy { case (_, count) => -count }.take(12).mkString(", ")
          }"
      )
      if !targetsMet(counts.toMap, config) then
        println(
          s"[bookmaker-planner-signoff] target incomplete after scanning ${config.maxScan} bookmaker entries: ${counts.toMap}"
        )
    finally engine.close()

  private def analyzeEntry(
      entry: SliceManifestEntry,
      engine: LocalUciEngine,
      config: Config
  ): Option[SignoffEntry] =
    val catalogEntry = loadCatalogEntry(entry)
    val pgn = Files.readString(Paths.get(entry.pgnPath))
    val plyData =
      PgnAnalysisHelper.extractPlyDataStrict(pgn) match
        case Right(value) =>
          value.find(_.ply == entry.targetPly).getOrElse {
            throw new IllegalArgumentException(s"${entry.sampleId}: missing ply ${entry.targetPly}")
          }
        case Left(err) =>
          throw new IllegalArgumentException(s"${entry.sampleId}: invalid PGN: $err")

    engine.newGame()
    val beforeVars = engine.analyze(entry.fen, config.depth, config.multiPv)
    val afterFen = lila.llm.analysis.NarrativeUtils.uciListToFen(entry.fen, List(entry.playedUci))
    engine.newGame()
    val afterVars = engine.analyze(afterFen, config.depth, config.multiPv)
    val afterBest = afterVars.headOption
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
    ).map { snapshot =>
      val runtime = bookmakerPlannerRuntime(snapshot)
      val plannerTrace = runtime.planner
      SignoffEntry(
        sampleId = entry.sampleId,
        gameKey = entry.gameKey,
        sliceKind = entry.sliceKind,
        targetPly = entry.targetPly,
        playedSan = entry.playedSan,
        plannerPrimaryKind = plannerTrace.primaryKind,
        plannerPrimaryFallbackMode = plannerTrace.primaryFallbackMode,
        plannerSecondaryKind = plannerTrace.secondaryKind,
        plannerSecondarySurfaced = plannerTrace.secondarySurfaced,
        bookmakerFallbackMode = plannerTrace.bookmakerFallbackMode,
        prose = runtime.prose.trim
      )
    }

  private def categoryOf(entry: SignoffEntry): Option[String] =
    val plannerOwned = entry.bookmakerFallbackMode == "planner_owned"
    entry.plannerPrimaryKind match
      case Some(kind) if plannerOwned && Set("WhyThis", "WhyNow", "WhatChanged", "WhatMustBeStopped", "WhosePlanIsFaster").contains(kind) =>
        Some(kind)
      case _ if entry.bookmakerFallbackMode == "exact_factual" || entry.plannerPrimaryFallbackMode.exists(_ != "PlannerOwned") =>
        Some("negative")
      case _ => None

  private def targetsMet(counts: Map[String, Int], config: Config): Boolean =
    val coreKinds = List("WhyThis", "WhyNow", "WhatChanged", "WhatMustBeStopped", "WhosePlanIsFaster")
    coreKinds.forall(kind => counts.getOrElse(kind, 0) >= config.perKindTarget) &&
    counts.getOrElse("negative", 0) >= config.negativeTarget

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
          System.err.println("[bookmaker-planner-signoff] missing engine path. Pass `--engine /path/to/uci-engine`.")
          sys.exit(1)
        }
    Config(
      manifestPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("slice_manifest.jsonl")),
      outPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultBookmakerRunDir.resolve("bookmaker_planner_signoff.jsonl")),
      depth = optionInt(args, "--depth").getOrElse(8).max(6),
      multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(2).max(1),
      perKindTarget = optionInt(args, "--per-kind").getOrElse(2).max(1),
      negativeTarget = optionInt(args, "--negative").getOrElse(4).max(1),
      maxScan = optionInt(args, "--max-scan").getOrElse(120).max(10),
      enginePath = enginePath
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue = Set("--engine", "--depth", "--multi-pv", "--multiPv", "--per-kind", "--negative", "--max-scan")
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
