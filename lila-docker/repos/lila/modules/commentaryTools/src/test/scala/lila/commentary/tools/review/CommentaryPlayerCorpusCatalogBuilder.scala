package lila.commentary.tools.review

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import lila.commentary.PgnAnalysisHelper

object CommentaryPlayerCorpusCatalogBuilder:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      rawPgnDir: Path = DefaultRawPgnDir,
      catalogPath: Path = DefaultCatalogDir.resolve("catalog.jsonl"),
      normalizedGamesDir: Path = DefaultCatalogDir.resolve("games"),
      minElo: Option[Int] = None,
      excludedTimeBuckets: Set[String] = Set.empty,
      selection: String = "balanced",
      limit: Option[Int] = None
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    ensureDir(config.normalizedGamesDir)

    val rawFiles = readPgnFiles(config.rawPgnDir)
    if rawFiles.isEmpty then
      System.err.println(s"[player-qc-catalog] no PGN files under `${config.rawPgnDir}`")
      sys.exit(1)

    val candidates =
      rawFiles.flatMap { file =>
        val raw = Files.readString(file, StandardCharsets.UTF_8)
        splitPgnGames(raw).zipWithIndex.flatMap { case (pgn, idx) =>
          val headers = parseHeaders(pgn)
          val variant = headers.getOrElse("Variant", "standard").trim.toLowerCase
          val supportedVariant = variant.isEmpty || variant == "standard" || variant == "chess960"
          if !supportedVariant then Nil
          else if !headerResultMatchesPgn(headers, pgn) then Nil
          else if !matchesRatingFloor(headers, config.minElo) then Nil
          else if config.excludedTimeBuckets.contains(timeControlBucketOf(headers)) then Nil
          else
            PgnAnalysisHelper.extractPlyDataStrict(pgn).toOption.toList.flatMap { plyData =>
              val sourceId = slug(file.getFileName.toString.stripSuffix(".pgn"))
              val gameKey = gameKeyFor(headers, sourceId, idx + 1)
              val normalizedPath = config.normalizedGamesDir.resolve(s"$gameKey.pgn")
              Files.writeString(normalizedPath, pgn.trim + "\n", StandardCharsets.UTF_8)
              val mixBucket = inferMixBucket(file, headers)
              val entry =
                CatalogEntry(
                  gameKey = gameKey,
                  source = file.toAbsolutePath.normalize.toString,
                  sourceId = sourceId,
                  pgnPath = normalizedPath.toAbsolutePath.normalize.toString,
                  mixBucket = mixBucket,
                  familyTags = familyTagsFor(file, headers, plyData.size),
                  ratingBucket = ratingBucketOf(headers),
                  timeControlBucket = timeControlBucketOf(headers),
                  notes = Nil,
                  openingMacroFamily = openingMacroFamily(headers),
                  opening = openingLabel(headers),
                  eco = headers.get("ECO").map(_.trim).filter(_.nonEmpty),
                  variation = headers.get("Variation").map(_.trim).filter(_.nonEmpty),
                  white = headers.get("White").map(_.trim).filter(_.nonEmpty),
                  black = headers.get("Black").map(_.trim).filter(_.nonEmpty),
                  event = headers.get("Event").map(_.trim).filter(_.nonEmpty),
                  date = headers.get("Date").map(_.trim).filter(_.nonEmpty),
                  round = headers.get("Round").map(_.trim).filter(_.nonEmpty),
                  result = headers.get("Result").map(_.trim).filter(_.nonEmpty),
                  timeControl = headers.get("TimeControl").map(_.trim).filter(_.nonEmpty),
                  variant = if variant.nonEmpty then variant else "standard",
                  whiteElo = parseIntHeader(headers, "WhiteElo"),
                  blackElo = parseIntHeader(headers, "BlackElo"),
                  totalPlies = plyData.size
                )
              List(entry)
            }
        }
      }

    val selected =
      val base =
        config.selection match
          case "all"      => candidates
          case "balanced" => selectCorpus(candidates)
          case other =>
            System.err.println(s"[player-qc-catalog] unknown selection mode `$other`; use `balanced` or `all`.")
            sys.exit(1)
      config.limit.fold(base)(base.take)
    if selected.isEmpty then
      System.err.println("[player-qc-catalog] no games survived validation and selection")
      sys.exit(1)

    writeJsonLines(config.catalogPath, selected)

    println(
      s"[player-qc-catalog] wrote `${config.catalogPath}` (selected=${selected.size}, candidates=${candidates.size})"
    )

  private def matchesRatingFloor(headers: Map[String, String], minElo: Option[Int]): Boolean =
    minElo.forall { floor =>
      List(parseIntHeader(headers, "WhiteElo"), parseIntHeader(headers, "BlackElo")) match
        case Some(white) :: Some(black) :: Nil => white >= floor && black >= floor
        case _                                => false
    }

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    Config(
      rawPgnDir = positional.headOption.map(Paths.get(_)).getOrElse(DefaultRawPgnDir),
      catalogPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultCatalogDir.resolve("catalog.jsonl")),
      normalizedGamesDir =
        positional.lift(2).map(Paths.get(_)).getOrElse(DefaultCatalogDir.resolve("games")),
      minElo = optionInt(args, "--min-elo").filter(_ > 0),
      excludedTimeBuckets = optionStrings(args, "--exclude-time-bucket").map(_.trim).filter(_.nonEmpty).toSet,
      selection = optionString(args, "--selection").map(_.trim.toLowerCase).filter(_.nonEmpty).getOrElse("balanced"),
      limit = optionInt(args, "--limit").filter(_ > 0)
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue = Set("--min-elo", "--exclude-time-bucket", "--selection", "--limit")
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
    optionStrings(args, name).headOption

  private def optionStrings(args: List[String], name: String): List[String] =
    args.sliding(2).collect { case List(flag, value) if flag == name => value.trim }.filter(_.nonEmpty).toList

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)
