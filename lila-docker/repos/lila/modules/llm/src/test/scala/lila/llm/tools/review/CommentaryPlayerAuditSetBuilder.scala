package lila.llm.tools.review

import java.nio.file.{ Path, Paths }
import java.time.Instant

import play.api.libs.json.Json

import scala.collection.mutable

object CommentaryPlayerAuditSetBuilder:

  import CommentaryPlayerQcSupport.*
  import lila.llm.tools.realpgn.RealPgnNarrativeEvalRunner.*

  private val DefaultShardRunDirs = List(
    ExternalRoot.resolve("manifests").resolve("active_parity_closure_runs_v2").resolve("edge_case_000"),
    ExternalRoot.resolve("manifests").resolve("active_parity_closure_runs_v2").resolve("master_classical_000"),
    ExternalRoot.resolve("manifests").resolve("active_parity_closure_runs_v2").resolve("master_classical_080"),
    ExternalRoot.resolve("manifests").resolve("active_parity_closure_runs_v2").resolve("titled_practical_000"),
    ExternalRoot.resolve("manifests").resolve("active_parity_closure_runs_v2").resolve("titled_practical_040")
  )

  private val DefaultSingleCorpora = List(
    DefaultManifestDir.resolve("single_actorxu_77.json"),
    DefaultManifestDir.resolve("single_infernal_59.json")
  )

  final case class Config(
      runDirs: List[Path] = DefaultShardRunDirs,
      singleCorpora: List[Path] = DefaultSingleCorpora,
      singleRunsRoot: Path = DefaultManifestDir.resolve("audit_202_singles"),
      outPath: Path = DefaultManifestDir.resolve("audit_202_set.json")
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val auditSet = buildAuditSet(config, Instant.now())
    writeJson(config.outPath, Json.toJson(auditSet))
    println(s"[player-qc-audit-set] wrote `${config.outPath}` (games=${auditSet.games.size})")

  private[tools] def buildAuditSet(config: Config, generatedAt: Instant): AuditSetManifest =
    val entries = mutable.ListBuffer.empty[AuditSetEntry]

    config.runDirs.foreach { runDir =>
      val reportPath = runDir.resolve("report.json")
      val rawDir = runDir.resolve("raw")
      val report = readRunReport(reportPath)
      report.games.foreach { game =>
        val entry =
          AuditSetEntry(
            auditId = s"${runDir.getFileName.toString}:${game.id}",
            gameId = game.id,
            tier = game.tier,
            openingFamily = game.family,
            label = game.label,
            reportPath = reportPath.toString,
            rawDir = rawDir.toString,
            sourceTag = runDir.getFileName.toString,
            event = game.event,
            date = game.date,
            result = game.result
          )
        entries += entry
      }
    }

    config.singleCorpora.foreach { corpusPath =>
      val corpus = readCorpusOrExit(corpusPath)
      val stem = stripJsonSuffix(corpusPath.getFileName.toString)
      val singleRunDir = config.singleRunsRoot.resolve(stem)
      corpus.games.foreach { game =>
        val entry =
          AuditSetEntry(
            auditId = s"$stem:${game.id}",
            gameId = game.id,
            tier = game.tier,
            openingFamily = game.family,
            label = game.label,
            reportPath = singleRunDir.resolve("report.json").toString,
            rawDir = singleRunDir.resolve("raw").toString,
            sourceTag = stem,
            corpusPath = Some(corpusPath.toString)
          )
        entries += entry
      }
    }

    AuditSetManifest(
      generatedAt = generatedAt.toString,
      title = "Commentary Player QC 202-game audit set",
      description = "Deterministic 202-game audit set built from active parity shard reports plus singled-out residual exemplars.",
      games = entries.toList.sortBy(entry => (entry.sourceTag, entry.tier, entry.openingFamily, entry.label, entry.gameId))
    )

  private def readRunReport(path: Path): RunReport =
    play.api.libs.json.Json.parse(java.nio.file.Files.readString(path)).validate[RunReport].asEither match
      case Right(value) => value
      case Left(err) =>
        throw new IllegalArgumentException(s"failed to parse run report `${path}`: $err")

  private def readCorpusOrExit(path: Path): Corpus =
    Json.parse(java.nio.file.Files.readString(path)).validate[Corpus].asEither match
      case Right(value) => value
      case Left(err)    => throw new IllegalArgumentException(s"failed to parse corpus `${path}`: $err")

  private def stripJsonSuffix(name: String): String =
    if name.toLowerCase.endsWith(".json") then name.dropRight(5) else name

  private def parseConfig(args: List[String]): Config =
    @annotation.tailrec
    def loop(
        rest: List[String],
        runDirs: List[Path],
        singleCorpora: List[Path],
        singleRunsRoot: Path,
        outPath: Option[Path]
    ): Config =
      rest match
        case "--run-dir" :: value :: tail =>
          loop(tail, runDirs :+ Paths.get(value), singleCorpora, singleRunsRoot, outPath)
        case "--single-corpus" :: value :: tail =>
          loop(tail, runDirs, singleCorpora :+ Paths.get(value), singleRunsRoot, outPath)
        case "--single-runs-root" :: value :: tail =>
          loop(tail, runDirs, singleCorpora, Paths.get(value), outPath)
        case value :: tail if !value.startsWith("--") && outPath.isEmpty =>
          loop(tail, runDirs, singleCorpora, singleRunsRoot, Some(Paths.get(value)))
        case _ :: tail =>
          loop(tail, runDirs, singleCorpora, singleRunsRoot, outPath)
        case Nil =>
          Config(
            runDirs = if runDirs.nonEmpty then runDirs else DefaultShardRunDirs,
            singleCorpora = if singleCorpora.nonEmpty then singleCorpora else DefaultSingleCorpora,
            singleRunsRoot = singleRunsRoot,
            outPath = outPath.getOrElse(DefaultManifestDir.resolve("audit_202_set.json"))
          )

    loop(args, Nil, Nil, DefaultManifestDir.resolve("audit_202_singles"), None)
