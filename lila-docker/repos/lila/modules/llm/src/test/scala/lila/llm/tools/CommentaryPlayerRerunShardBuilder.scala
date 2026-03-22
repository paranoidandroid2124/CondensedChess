package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant

import play.api.libs.json.*

object CommentaryPlayerRerunShardBuilder:

  import CommentaryPlayerQcSupport.*
  import RealPgnNarrativeEvalRunner.{ Corpus, CorpusGame }

  final case class Config(
      corpusPath: Path = DefaultManifestDir.resolve("chronicle_corpus.json"),
      manifestPath: Path = DefaultManifestDir.resolve("slice_manifest.jsonl"),
      outDir: Path = DefaultManifestDir.resolve("full360_shards"),
      gamesPerShard: Int = 20,
      groupByTier: Boolean = true,
      writeScripts: Boolean = true
  )

  final case class ShardMeta(
      shardId: String,
      tier: String,
      offset: Int,
      gameCount: Int,
      sliceEntryCount: Int,
      pairedSliceCount: Int,
      corpusPath: String,
      manifestPath: String,
      runDir: String,
      gameIds: List[String]
  )
  object ShardMeta:
    given OFormat[ShardMeta] = Json.format[ShardMeta]

  final case class ShardPlan(
      version: Int = 1,
      generatedAt: String,
      sourceCorpusPath: String,
      sourceManifestPath: String,
      gamesPerShard: Int,
      shardCount: Int,
      totalGames: Int,
      totalSliceEntries: Int,
      shards: List[ShardMeta]
  )
  object ShardPlan:
    given OFormat[ShardPlan] = Json.format[ShardPlan]

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[player-qc-rerun-shards] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(config: Config): Unit =
    val corpus = readCorpus(config.corpusPath)
    val manifest =
      readJsonLines[SliceManifestEntry](config.manifestPath) match
        case Right(value) => value
        case Left(err)    => throw new IllegalArgumentException(s"failed to read manifest `${config.manifestPath}`: $err")
    ensureDir(config.outDir)
    val shardGroups = buildShardGroups(corpus.games, config)
    val shardMetas =
      shardGroups.map { case (tier, offset, games) =>
        writeShard(config, corpus, manifest, tier, offset, games)
      }
    val plan =
      ShardPlan(
        generatedAt = Instant.now().toString,
        sourceCorpusPath = config.corpusPath.toAbsolutePath.normalize.toString,
        sourceManifestPath = config.manifestPath.toAbsolutePath.normalize.toString,
        gamesPerShard = config.gamesPerShard,
        shardCount = shardMetas.size,
        totalGames = shardMetas.map(_.gameCount).sum,
        totalSliceEntries = shardMetas.map(_.sliceEntryCount).sum,
        shards = shardMetas
      )
    writeText(config.outDir.resolve("shard_plan.json"), Json.prettyPrint(Json.toJson(plan)) + "\n")
    if config.writeScripts then
      writeText(config.outDir.resolve("run_real_pgn_shards.ps1"), renderRealPgnScript(shardMetas))
      writeText(config.outDir.resolve("run_bookmaker_shards.ps1"), renderBookmakerScript(shardMetas))
      writeText(config.outDir.resolve("run_review_queue_shards.ps1"), renderReviewQueueScript(shardMetas))
      writeText(config.outDir.resolve("merge_real_pgn_reports.ps1"), renderMergeScript(config.outDir))
    println(
      s"[player-qc-rerun-shards] wrote `${config.outDir}` (shards=${shardMetas.size}, games=${plan.totalGames}, sliceEntries=${plan.totalSliceEntries})"
    )

  private def writeShard(
      config: Config,
      corpus: Corpus,
      manifest: List[SliceManifestEntry],
      tier: String,
      offset: Int,
      games: List[CorpusGame]
  ): ShardMeta =
    val shardId = f"${normalizeTier(tier)}_${offset}%03d"
    val gameIds = games.map(_.id).toSet
    val shardManifest = manifest.filter(entry => gameIds.contains(entry.gameKey))
    val shardCorpus =
      corpus.copy(
        generatedAt = Instant.now().toString,
        title = s"${corpus.title} [$shardId]",
        description = s"${corpus.description} (tier=$tier, offset=$offset, games=${games.size})",
        games = games
      )
    val corpusPath = config.outDir.resolve(s"chronicle_corpus_$shardId.json")
    val manifestPath = config.outDir.resolve(s"slice_manifest_$shardId.jsonl")
    val runDir = config.outDir.resolve("runs").resolve(shardId)
    ensureDir(runDir)
    writeText(corpusPath, Json.prettyPrint(Json.toJson(shardCorpus)) + "\n")
    writeJsonLines(manifestPath, shardManifest)
    ShardMeta(
      shardId = shardId,
      tier = tier,
      offset = offset,
      gameCount = games.size,
      sliceEntryCount = shardManifest.size,
      pairedSliceCount = shardManifest.size / 2,
      corpusPath = corpusPath.toAbsolutePath.normalize.toString,
      manifestPath = manifestPath.toAbsolutePath.normalize.toString,
      runDir = runDir.toAbsolutePath.normalize.toString,
      gameIds = games.map(_.id)
    )

  private def buildShardGroups(games: List[CorpusGame], config: Config): List[(String, Int, List[CorpusGame])] =
    val grouped =
      if config.groupByTier then games.groupBy(_.tier).toList.sortBy(_._1)
      else List("all" -> games)
    grouped.flatMap { case (tier, tierGames) =>
      tierGames
        .sortBy(_.id)
        .grouped(config.gamesPerShard)
        .zipWithIndex
        .map { case (chunk, idx) =>
          (tier, idx * config.gamesPerShard, chunk)
        }
        .toList
    }

  private def renderRealPgnScript(shards: List[ShardMeta]): String =
    renderScript(shards, "run shard real-PGN signoff") { shard =>
      val reportMd = s"${shard.runDir}\\report.md"
      val reportJson = s"${shard.runDir}\\report.json"
      val rawDir = s"${shard.runDir}\\raw"
      s"""sbt 'llm/test:runMain lila.llm.tools.RealPgnNarrativeEvalRunner ${quoteSbtArg(shard.corpusPath)} ${quoteSbtArg(reportMd)} ${quoteSbtArg(reportJson)} ${quoteSbtArg(rawDir)} --depth 10 --multi-pv 3'"""
    }

  private def renderBookmakerScript(shards: List[ShardMeta]): String =
    renderScript(shards, "run shard bookmaker outputs") { shard =>
      val outPath = s"${shard.runDir}\\bookmaker_outputs.jsonl"
      val rawDir = s"${shard.runDir}\\bookmaker_raw"
      s"""sbt 'llm/test:runMain lila.llm.tools.BookmakerCorpusRunner ${quoteSbtArg(shard.manifestPath)} ${quoteSbtArg(outPath)} ${quoteSbtArg(rawDir)} --depth 10 --multi-pv 3'"""
    }

  private def renderReviewQueueScript(shards: List[ShardMeta]): String =
    renderScript(shards, "build shard review queue") { shard =>
      val bookmakerPath = s"${shard.runDir}\\bookmaker_outputs.jsonl"
      val chronicleReport = s"${shard.runDir}\\report.json"
      val queuePath = s"${shard.runDir}\\review_queue.jsonl"
      val summaryPath = s"${shard.runDir}\\review_queue_summary.json"
      s"""sbt 'llm/test:runMain lila.llm.tools.CommentaryPlayerReviewQueueBuilder ${quoteSbtArg(shard.manifestPath)} ${quoteSbtArg(bookmakerPath)} ${quoteSbtArg(chronicleReport)} ${quoteSbtArg(queuePath)} ${quoteSbtArg(summaryPath)}'"""
    }

  private def renderMergeScript(outDir: Path): String =
    val reportRoot = outDir.resolve("runs").toAbsolutePath.normalize.toString
    val mergedMd = outDir.resolve("merged_report.md").toAbsolutePath.normalize.toString
    val mergedJson = outDir.resolve("merged_report.json").toAbsolutePath.normalize.toString
    s"""Set-StrictMode -Version Latest
Push-Location ${quoteLiteral(repoRoot().toString)}
try {
  sbt 'llm/test:runMain lila.llm.tools.RealPgnNarrativeEvalReportMerge ${quoteSbtArg(reportRoot)} ${quoteSbtArg(mergedMd)} ${quoteSbtArg(mergedJson)}'
} finally {
  Pop-Location
}
"""

  private def renderScript(shards: List[ShardMeta], header: String)(commandFor: ShardMeta => String): String =
    val body =
      shards
        .map(commandFor)
        .mkString("", "\n", if shards.nonEmpty then "\n" else "")
    s"""Set-StrictMode -Version Latest
# $header
if (-not $$env:STOCKFISH_BIN -and -not $$env:LLM_ACTIVE_CORPUS_ENGINE_PATH) {
  throw "Set STOCKFISH_BIN or LLM_ACTIVE_CORPUS_ENGINE_PATH before running shard jobs."
}
Push-Location ${quoteLiteral(repoRoot().toString)}
try {
$body} finally {
  Pop-Location
}
"""

  private def readCorpus(path: Path): Corpus =
    val raw = Files.readString(path, StandardCharsets.UTF_8)
    Json.parse(raw).as[Corpus]

  private def parseArgs(args: List[String]): Either[String, Config] =
    val defaults = Config()

    @annotation.tailrec
    def loop(rest: List[String], cfg: Config): Either[String, Config] =
      rest match
        case Nil => Right(cfg)
        case head :: tail if head.startsWith("--corpus=") =>
          loop(tail, cfg.copy(corpusPath = Paths.get(head.stripPrefix("--corpus=")).toAbsolutePath.normalize))
        case "--corpus" :: value :: tail =>
          loop(tail, cfg.copy(corpusPath = Paths.get(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--manifest=") =>
          loop(tail, cfg.copy(manifestPath = Paths.get(head.stripPrefix("--manifest=")).toAbsolutePath.normalize))
        case "--manifest" :: value :: tail =>
          loop(tail, cfg.copy(manifestPath = Paths.get(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--out-dir=") =>
          loop(tail, cfg.copy(outDir = Paths.get(head.stripPrefix("--out-dir=")).toAbsolutePath.normalize))
        case "--out-dir" :: value :: tail =>
          loop(tail, cfg.copy(outDir = Paths.get(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--games-per-shard=") =>
          head.stripPrefix("--games-per-shard=").toIntOption match
            case Some(value) if value > 0 => loop(tail, cfg.copy(gamesPerShard = value))
            case _                        => Left(s"invalid --games-per-shard: $head")
        case "--games-per-shard" :: value :: tail =>
          value.toIntOption match
            case Some(parsed) if parsed > 0 => loop(tail, cfg.copy(gamesPerShard = parsed))
            case _                          => Left(s"invalid --games-per-shard: $value")
        case "--no-tier-group" :: tail =>
          loop(tail, cfg.copy(groupByTier = false))
        case "--no-scripts" :: tail =>
          loop(tail, cfg.copy(writeScripts = false))
        case unknown :: _ => Left(s"unknown argument: $unknown")

    loop(args, defaults)

  private def quoteSbtArg(raw: String): String =
    "\"" + raw.replace("\\", "/") + "\""

  private def quoteLiteral(raw: String): String =
    "'" + raw.replace("'", "''") + "'"

  private def normalizeTier(raw: String): String =
    Option(raw).getOrElse("all").trim.toLowerCase.replaceAll("[^a-z0-9]+", "_")

  private def repoRoot(): Path =
    Path.of(".").toAbsolutePath.normalize

  private def writeText(path: Path, value: String): Unit =
    ensureParent(path)
    Files.writeString(path, value, StandardCharsets.UTF_8)
