package lila.llm.tools.review

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.Json

import lila.llm.PgnAnalysisHelper

object CommentaryPlayerCorpusCatalogBuilder:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      rawPgnDir: Path = DefaultRawPgnDir,
      catalogPath: Path = DefaultCatalogDir.resolve("catalog.jsonl"),
      chronicleCorpusPath: Path = DefaultManifestDir.resolve("chronicle_corpus.json"),
      normalizedGamesDir: Path = DefaultCatalogDir.resolve("games")
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

    val selected = selectCorpus(candidates)
    if selected.isEmpty then
      System.err.println("[player-qc-catalog] no games survived validation and selection")
      sys.exit(1)

    writeJsonLines(config.catalogPath, selected)
    writeJson(config.chronicleCorpusPath, Json.toJson(chronicleCorpusFromCatalog(selected)))

    println(
      s"[player-qc-catalog] wrote `${config.catalogPath}` (selected=${selected.size}, candidates=${candidates.size}) and `${config.chronicleCorpusPath}`"
    )

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    Config(
      rawPgnDir = positional.headOption.map(Paths.get(_)).getOrElse(DefaultRawPgnDir),
      catalogPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultCatalogDir.resolve("catalog.jsonl")),
      chronicleCorpusPath =
        positional.lift(2).map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("chronicle_corpus.json")),
      normalizedGamesDir =
        positional.lift(3).map(Paths.get(_)).getOrElse(DefaultCatalogDir.resolve("games"))
    )

  private def positionalArgs(args: List[String]): List[String] =
    args.filterNot(_.startsWith("--"))
