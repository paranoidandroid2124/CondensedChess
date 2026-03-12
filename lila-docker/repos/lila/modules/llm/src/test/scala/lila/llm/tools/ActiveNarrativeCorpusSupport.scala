package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import play.api.libs.json.*

import lila.llm.MoveEval
import lila.llm.analysis.CommentaryOpsBoard

object ActiveNarrativeCorpusSupport:

  final case class SelectionPolicy(
      players: List[String],
      gamesPerPlayer: Int,
      sortOrder: String,
      variantFilter: String,
      notes: List[String]
  )

  object SelectionPolicy:
    given Reads[SelectionPolicy] = Json.reads[SelectionPolicy]
    given Writes[SelectionPolicy] = Json.writes[SelectionPolicy]

  final case class CorpusSource(
      sourceId: String,
      event: String,
      formatHint: String,
      pgnUrl: String
  )

  object CorpusSource:
    given Reads[CorpusSource] = Json.reads[CorpusSource]
    given Writes[CorpusSource] = Json.writes[CorpusSource]

  final case class CorpusGame(
      selectionRank: Int,
      date: String,
      round: String,
      event: String,
      site: Option[String],
      sourceId: String,
      sourceEvent: String,
      sourcePgnUrl: String,
      formatHint: String,
      white: String,
      black: String,
      color: String,
      opponent: String,
      result: String,
      playerResult: String,
      eco: Option[String],
      opening: Option[String],
      variation: Option[String],
      timeControl: Option[String],
      variant: String,
      pgn: String
  )

  object CorpusGame:
    given Reads[CorpusGame] = Json.reads[CorpusGame]
    given Writes[CorpusGame] = Json.writes[CorpusGame]

  final case class CorpusPlayer(
      player: String,
      selectionNote: String,
      games: List[CorpusGame]
  )

  object CorpusPlayer:
    given Reads[CorpusPlayer] = Json.reads[CorpusPlayer]
    given Writes[CorpusPlayer] = Json.writes[CorpusPlayer]

  final case class ActiveNarrativeCorpus(
      version: Int,
      generatedAt: String,
      asOfDate: String,
      title: String,
      description: String,
      selectionPolicy: SelectionPolicy,
      sources: List[CorpusSource],
      players: List[CorpusPlayer]
  )

  object ActiveNarrativeCorpus:
    given Reads[ActiveNarrativeCorpus] = Json.reads[ActiveNarrativeCorpus]
    given Writes[ActiveNarrativeCorpus] = Json.writes[ActiveNarrativeCorpus]

  final case class CorpusEntry(
      player: String,
      selectionNote: String,
      game: CorpusGame
  ):
    lazy val gameKey: String =
      gameKeyFor(player = player, date = game.date, round = game.round, white = game.white, black = game.black)

    lazy val totalPlies: Int =
      lila.llm.PgnAnalysisHelper.extractPlyData(game.pgn).map(_.size).getOrElse(0)

  final case class EvalEngineMeta(
      name: String,
      path: String,
      depth: Int,
      multiPv: Int,
      generatedAt: String
  )

  object EvalEngineMeta:
    given Reads[EvalEngineMeta] = Json.reads[EvalEngineMeta]
    given Writes[EvalEngineMeta] = Json.writes[EvalEngineMeta]

  final case class EvalCacheGame(
      gameKey: String,
      player: String,
      date: String,
      round: String,
      white: String,
      black: String,
      totalPlies: Int,
      evals: List[MoveEval]
  )

  object EvalCacheGame:
    given Reads[EvalCacheGame] = Json.reads[EvalCacheGame]
    given Writes[EvalCacheGame] = Json.writes[EvalCacheGame]

  final case class ActiveNarrativeEvalCache(
      version: Int = 1,
      corpusTitle: String,
      corpusAsOfDate: String,
      corpusGeneratedAt: String,
      gameCount: Int,
      engine: EvalEngineMeta,
      games: List[EvalCacheGame]
  )

  object ActiveNarrativeEvalCache:
    given Reads[ActiveNarrativeEvalCache] = Json.reads[ActiveNarrativeEvalCache]
    given Writes[ActiveNarrativeEvalCache] = Json.writes[ActiveNarrativeEvalCache]

  final case class ThreadSummary(
      themeKey: String,
      themeLabel: String,
      representativePlies: List[Int],
      continuityScore: Double
  )

  object ThreadSummary:
    given Writes[ThreadSummary] = Json.writes[ThreadSummary]

  final case class SpotlightNoteSummary(
      themeLabel: Option[String],
      stageLabel: Option[String],
      threadSummary: Option[String],
      noteExcerpt: String
  )

  object SpotlightNoteSummary:
    given Writes[SpotlightNoteSummary] = Json.writes[SpotlightNoteSummary]

  final case class GameReport(
      gameKey: String,
      player: String,
      opponent: String,
      event: String,
      date: String,
      result: String,
      selectionRank: Int,
      totalPlies: Int,
      provider: String,
      sourceMode: String,
      model: Option[String],
      latencyMs: Long,
      totalMoments: Int,
      selectedStrategicMoments: Int,
      threadedMoments: Int,
      threadCount: Int,
      threads: List[ThreadSummary],
      stageCoverage: List[String],
      bridgeMomentCount: Int,
      activeNoteCount: Int,
      activeNoteAttachmentRatio: Double,
      allAttachedNotesHaveThreadMetadata: Boolean,
      openingExplorerAdvisories: Int,
      warnings: List[String],
      retryUsed: Boolean,
      hardFailure: Boolean,
      error: Option[String],
      responsePath: Option[String],
      threadSummaries: List[String] = Nil,
      spotlightNotes: List[SpotlightNoteSummary] = Nil,
      internalMomentCount: Option[Int] = None,
      visibleMomentCount: Option[Int] = None,
      polishedMomentCount: Option[Int] = None,
      visibleStrategicMomentCount: Option[Int] = None,
      visibleBridgeMomentCount: Option[Int] = None
  )

  object GameReport:
    given Writes[GameReport] = Json.writes[GameReport]

  final case class RunSummary(
      totalGames: Int,
      succeeded: Int,
      failed: Int,
      providerDistribution: Map[String, Int],
      modelDistribution: Map[String, Int],
      sourceModeDistribution: Map[String, Int],
      avgLatencyMs: Double,
      p95LatencyMs: Double,
      avgNarrativeMoments: Double,
      avgInternalMoments: Double = 0.0,
      avgVisibleMoments: Double = 0.0,
      avgPolishedMoments: Double = 0.0,
      avgStrategicThreadsPerGame: Double,
      gamesWithThreadPct: Double,
      avgThreadedMomentsPerGame: Double,
      gamesWithBridgePct: Double,
      avgBridgeMomentsPerGame: Double,
      gamesWithActiveNotePct: Double,
      avgActiveNotesPerGame: Double,
      threadThemeDistribution: Map[String, Int],
      stageCoverageDistribution: Map[String, Int],
      dossierThreadMetadataCoverage: Double,
      openingExplorerAdvisoryCount: Int,
      retryCount: Int,
      hardFailureCount: Int
  )

  object RunSummary:
    given Writes[RunSummary] = Json.writes[RunSummary]

  final case class RunReport(
      version: Int = 1,
      generatedAt: String,
      corpusTitle: String,
      corpusAsOfDate: String,
      corpusGeneratedAt: String,
      provider: String,
      planTier: String,
      llmLevel: String,
      totalCorpusGames: Int,
      summary: RunSummary,
      games: List[GameReport],
      opsSnapshot: Option[CommentaryOpsBoard.Snapshot] = None
  )

  object RunReport:
    given Writes[RunReport] = Json.writes[RunReport]

  def readCorpus(path: Path): Either[String, ActiveNarrativeCorpus] =
    parseJsonFile(path).flatMap(_.validate[ActiveNarrativeCorpus].asEither.left.map(_.toString))

  def readEvalCache(path: Path): Either[String, ActiveNarrativeEvalCache] =
    parseJsonFile(path).flatMap(_.validate[ActiveNarrativeEvalCache].asEither.left.map(_.toString))

  def writeJson(path: Path, value: JsValue): Unit =
    ensureParent(path)
    Files.writeString(path, Json.prettyPrint(value) + "\n", StandardCharsets.UTF_8)

  def writeText(path: Path, text: String): Unit =
    ensureParent(path)
    Files.writeString(path, text, StandardCharsets.UTF_8)

  def flattenCorpus(corpus: ActiveNarrativeCorpus): List[CorpusEntry] =
    corpus.players.flatMap { player =>
      player.games.map(CorpusEntry(player.player, player.selectionNote, _))
    }

  def gameKeyFor(
      player: String,
      date: String,
      round: String,
      white: String,
      black: String
  ): String =
    slug(List(player, date, round, white, black).mkString("__"))

  def alignEvalCache(
      entries: List[CorpusEntry],
      cache: ActiveNarrativeEvalCache
  ): Either[List[String], Map[String, EvalCacheGame]] =
    val expected = entries.map(_.gameKey).toSet
    val actual = cache.games.map(_.gameKey).toSet
    val missing = (expected -- actual).toList.sorted.map(key => s"missing eval cache for `$key`")
    val unexpected = (actual -- expected).toList.sorted.map(key => s"unexpected eval cache entry `$key`")
    val mismatches =
      entries.flatMap { entry =>
        cache.games.find(_.gameKey == entry.gameKey).toList.flatMap { cached =>
          List(
            Option.when(cached.player != entry.player)(
              s"player mismatch for `${entry.gameKey}`: `${cached.player}` != `${entry.player}`"
            ),
            Option.when(cached.date != entry.game.date)(
              s"date mismatch for `${entry.gameKey}`: `${cached.date}` != `${entry.game.date}`"
            ),
            Option.when(cached.round != entry.game.round)(
              s"round mismatch for `${entry.gameKey}`: `${cached.round}` != `${entry.game.round}`"
            ),
            Option.when(cached.white != entry.game.white)(
              s"white mismatch for `${entry.gameKey}`: `${cached.white}` != `${entry.game.white}`"
            ),
            Option.when(cached.black != entry.game.black)(
              s"black mismatch for `${entry.gameKey}`: `${cached.black}` != `${entry.game.black}`"
            ),
            Option.when(cached.totalPlies != entry.totalPlies)(
              s"totalPlies mismatch for `${entry.gameKey}`: `${cached.totalPlies}` != `${entry.totalPlies}`"
            )
          ).flatten
        }
      }
    val errors = missing ++ unexpected ++ mismatches
    Either.cond(errors.isEmpty, cache.games.map(g => g.gameKey -> g).toMap, errors)

  def spotlightEntries(entries: List[CorpusEntry]): List[CorpusEntry] =
    val wantedRanks = Set(1, 5, 10)
    entries.filter(entry => wantedRanks.contains(entry.game.selectionRank))

  def spotlightReports(report: RunReport): List[GameReport] =
    val wantedRanks = Set(1, 5, 10)
    report.games.filter(game => wantedRanks.contains(game.selectionRank))

  def buildSummary(reports: List[GameReport]): RunSummary =
    val total = reports.size
    val successes = reports.count(!_.hardFailure)
    val failures = total - successes
    val succeededReports = reports.filter(!_.hardFailure)
    val activeNoteGames = reports.count(_.activeNoteCount > 0)
    val threadGames = reports.count(_.threadCount > 0)
    val bridgeGames = reports.count(_.bridgeMomentCount > 0)
    val latencyValues = reports.map(_.latencyMs).filter(_ >= 0)
    val noteMetadataDen = reports.map(_.activeNoteCount).sum
    val noteMetadataNum =
      reports.filter(_.allAttachedNotesHaveThreadMetadata).map(_.activeNoteCount).sum

    RunSummary(
      totalGames = total,
      succeeded = successes,
      failed = failures,
      providerDistribution = frequencyMap(reports.map(_.provider)),
      modelDistribution = frequencyMap(reports.flatMap(_.model)),
      sourceModeDistribution = frequencyMap(reports.map(_.sourceMode)),
      avgLatencyMs = averageLong(latencyValues),
      p95LatencyMs = percentileLong(latencyValues, 0.95),
      avgNarrativeMoments = averageInt(succeededReports.map(_.totalMoments)),
      avgInternalMoments = averageInt(succeededReports.flatMap(_.internalMomentCount)),
      avgVisibleMoments = averageInt(succeededReports.flatMap(_.visibleMomentCount)),
      avgPolishedMoments = averageInt(succeededReports.flatMap(_.polishedMomentCount)),
      avgStrategicThreadsPerGame = averageInt(succeededReports.map(_.threadCount)),
      gamesWithThreadPct = pct(threadGames, total),
      avgThreadedMomentsPerGame = averageInt(succeededReports.map(_.threadedMoments)),
      gamesWithBridgePct = pct(bridgeGames, total),
      avgBridgeMomentsPerGame = averageInt(succeededReports.map(_.bridgeMomentCount)),
      gamesWithActiveNotePct = pct(activeNoteGames, total),
      avgActiveNotesPerGame = averageInt(succeededReports.map(_.activeNoteCount)),
      threadThemeDistribution = frequencyMap(succeededReports.flatMap(_.threads.map(_.themeKey))),
      stageCoverageDistribution = frequencyMap(succeededReports.flatMap(_.stageCoverage)),
      dossierThreadMetadataCoverage =
        if noteMetadataDen == 0 then 1.0
        else noteMetadataNum.toDouble / noteMetadataDen.toDouble,
      openingExplorerAdvisoryCount = reports.map(_.openingExplorerAdvisories).sum,
      retryCount = reports.count(_.retryUsed),
      hardFailureCount = failures
    )

  def renderMarkdown(report: RunReport): String =
    val lines = scala.collection.mutable.ListBuffer.empty[String]
    lines += s"# ${report.corpusTitle} Benchmark"
    lines += ""
    lines += s"- Generated at: `${report.generatedAt}`"
    lines += s"- Corpus as of: `${report.corpusAsOfDate}`"
    lines += s"- Provider: `${report.provider}`"
    lines += s"- Mode: `${report.planTier}/${report.llmLevel}`"
    lines += s"- Games: `${report.summary.succeeded}/${report.totalCorpusGames}` succeeded"
    lines += ""
    lines += "## Run Summary"
    lines += ""
    lines += "| Metric | Value |"
    lines += "| --- | --- |"
    summaryRows(report.summary).foreach { case (label, value) =>
      lines += s"| $label | $value |"
    }
    report.opsSnapshot.foreach { snapshot =>
      lines += ""
      lines += "## LLM Ops Snapshot"
      lines += ""
      lines += "| Metric | Value |"
      lines += "| --- | --- |"
      opsSummaryRows(snapshot).foreach { case (label, value) =>
        lines += s"| $label | $value |"
      }
      lines += ""
      lines += "### Prompt Usage"
      lines += ""
      lines += "| Family | Attempts | Cache hits | Prompt tokens | Cached tokens | Completion tokens | Est. cost (USD) |"
      lines += "| --- | --- | --- | --- | --- | --- | --- |"
      val promptRows = snapshot.promptUsage.toList.sortBy(_._1)
      if promptRows.isEmpty then
        lines += "| - | 0 | 0 | 0 | 0 | 0 | 0.000000 |"
      else
        promptRows.foreach { case (family, usage) =>
          lines +=
            s"| ${escapePipe(family)} | ${usage.attempts} | ${usage.cacheHits} | ${usage.promptTokens} | ${usage.cachedTokens} | ${usage.completionTokens} | ${f"${usage.estimatedCostUsd}%.6f"} |"
        }
    }
    lines += ""
    lines += "## Per-Game Scorecard"
    lines += ""
    lines += "| Player | Rank | Date | Event | Threads | Bridges | Active notes | Source | Warnings |"
    lines += "| --- | --- | --- | --- | --- | --- | --- | --- | --- |"
    report.games.foreach { game =>
      val warnings =
        if game.warnings.isEmpty then "-"
        else game.warnings.mkString(", ")
      lines +=
        s"| ${game.player} | ${game.selectionRank} | ${game.date} | ${escapePipe(game.event)} | ${game.threadCount} | ${game.bridgeMomentCount} | ${game.activeNoteCount} | ${game.sourceMode} | ${escapePipe(warnings)} |"
    }
    lines += ""
    lines += "## Spotlight Review"
    lines += ""
    spotlightReports(report).foreach { game =>
      lines += s"### ${game.player} vs ${game.opponent} (${game.date}, rank ${game.selectionRank})"
      lines += ""
      lines += s"- Event: `${game.event}`"
      lines += s"- Result: `${game.result}`"
      lines += s"- Moments: `${game.totalMoments}`"
      game.internalMomentCount.foreach { value =>
        lines += s"- Internal coverage moments: `${value}`"
      }
      game.visibleMomentCount.foreach { value =>
        lines += s"- Visible moments: `${value}`"
      }
      game.polishedMomentCount.foreach { value =>
        lines += s"- Polish targets: `${value}`"
      }
      lines += s"- Threads: `${game.threadCount}`"
      lines += s"- Bridges: `${game.bridgeMomentCount}`"
      lines += s"- Active notes: `${game.activeNoteCount}`"
      lines +=
        s"- Stage coverage: `${if game.stageCoverage.nonEmpty then orderedStages(game.stageCoverage).mkString(", ") else "-"}`"
      if game.threadSummaries.nonEmpty then
        lines += "- Thread summaries:"
        game.threadSummaries.foreach(summary => lines += s"  - ${summary}")
      if game.spotlightNotes.nonEmpty then
        lines += "- Active note snapshots:"
        game.spotlightNotes.foreach { note =>
          val label = List(note.themeLabel, note.stageLabel).flatten.mkString(" / ")
          val headline = if label.nonEmpty then label else "unlabeled"
          lines += s"  - ${headline}: ${note.noteExcerpt}"
        }
      if game.responsePath.nonEmpty then
        lines += s"- Raw response: `${game.responsePath.get}`"
      if game.error.nonEmpty then
        lines += s"- Error: `${game.error.get}`"
      lines += ""
    }
    lines.mkString("\n") + "\n"

  def ensureParent(path: Path): Unit =
    Option(path.getParent).foreach(parent => Files.createDirectories(parent))

  def parseJsonFile(path: Path): Either[String, JsValue] =
    scala.util.Try(Json.parse(Files.readString(path, StandardCharsets.UTF_8))).toEither.left.map(_.getMessage)

  def slug(raw: String): String =
    Option(raw)
      .getOrElse("")
      .trim
      .toLowerCase
      .replaceAll("""[^a-z0-9]+""", "-")
      .replaceAll("(^-+|-+$)", "")

  def orderedStages(stages: Iterable[String]): List[String] =
    val order = List("seed", "build", "switch", "convert")
    val incoming = stages.toSet
    order.filter(incoming)

  private def summaryRows(summary: RunSummary): List[(String, String)] =
    List(
      "total games" -> summary.totalGames.toString,
      "succeeded" -> summary.succeeded.toString,
      "failed" -> summary.failed.toString,
      "avg latency (ms)" -> f"${summary.avgLatencyMs}%.1f",
      "p95 latency (ms)" -> f"${summary.p95LatencyMs}%.1f",
      "avg moments" -> f"${summary.avgNarrativeMoments}%.2f",
      "avg internal moments" -> f"${summary.avgInternalMoments}%.2f",
      "avg visible moments" -> f"${summary.avgVisibleMoments}%.2f",
      "avg polish targets" -> f"${summary.avgPolishedMoments}%.2f",
      "avg threads / game" -> f"${summary.avgStrategicThreadsPerGame}%.2f",
      "games with thread" -> f"${summary.gamesWithThreadPct * 100.0}%.1f%%",
      "avg threaded moments / game" -> f"${summary.avgThreadedMomentsPerGame}%.2f",
      "games with bridge" -> f"${summary.gamesWithBridgePct * 100.0}%.1f%%",
      "avg bridges / game" -> f"${summary.avgBridgeMomentsPerGame}%.2f",
      "games with active note" -> f"${summary.gamesWithActiveNotePct * 100.0}%.1f%%",
      "avg active notes / game" -> f"${summary.avgActiveNotesPerGame}%.2f",
      "dossier thread metadata coverage" -> f"${summary.dossierThreadMetadataCoverage * 100.0}%.1f%%",
      "opening explorer advisories" -> summary.openingExplorerAdvisoryCount.toString,
      "retries" -> summary.retryCount.toString,
      "hard failures" -> summary.hardFailureCount.toString,
      "source modes" -> renderMap(summary.sourceModeDistribution),
      "models" -> renderMap(summary.modelDistribution),
      "thread themes" -> renderMap(summary.threadThemeDistribution),
      "stage coverage" -> renderMap(summary.stageCoverageDistribution)
    )

  private def opsSummaryRows(snapshot: CommentaryOpsBoard.Snapshot): List[(String, String)] =
    List(
      "fullgame compare observed" -> snapshot.fullgame.compareObserved.toString,
      "fullgame compare consistency" -> f"${snapshot.fullgame.compareConsistencyRate * 100.0}%.1f%%",
      "active selected moments" -> snapshot.active.selectedMoments.toString,
      "active attempts" -> snapshot.active.attempts.toString,
      "active attached" -> snapshot.active.attached.toString,
      "active omitted" -> snapshot.active.omitted.toString,
      "active primary accepted" -> snapshot.active.primaryAccepted.toString,
      "active repair attempts" -> snapshot.active.repairAttempts.toString,
      "active repair recovered" -> snapshot.active.repairRecovered.toString,
      "active attach rate" -> f"${snapshot.active.attachRate * 100.0}%.1f%%",
      "active thesis agreement" -> f"${snapshot.active.thesisAgreementRate * 100.0}%.1f%%",
      "active provider" -> snapshot.active.provider.getOrElse("-"),
      "active model" -> snapshot.active.configuredModel.getOrElse("-"),
      "active fallback model" -> snapshot.active.fallbackModel.getOrElse("-"),
      "active reasoning effort" -> snapshot.active.reasoningEffort.getOrElse("-"),
      "active observed models" -> renderLongMap(snapshot.active.observedModelDistribution),
      "dossier attach rate" -> f"${snapshot.active.dossierAttachRate * 100.0}%.1f%%",
      "dossier compare rate" -> f"${snapshot.active.dossierCompareRate * 100.0}%.1f%%",
      "dossier route ref rate" -> f"${snapshot.active.dossierRouteRefRate * 100.0}%.1f%%",
      "dossier reference failure rate" -> f"${snapshot.active.dossierReferenceFailureRate * 100.0}%.1f%%",
      "omit reasons" -> renderLongMap(snapshot.active.omitReasons),
      "warning reasons" -> renderLongMap(snapshot.active.warningReasons),
      "prompt usage families" -> snapshot.promptUsage.size.toString,
      "recent sample count" -> snapshot.recentSamples.size.toString
    )

  private def renderMap(map: Map[String, Int]): String =
    if map.isEmpty then "-"
    else map.toList.sortBy { case (k, _) => k }.map { case (k, v) => s"$k=$v" }.mkString(", ")

  private def renderLongMap(map: Map[String, Long]): String =
    if map.isEmpty then "-"
    else map.toList.sortBy { case (k, _) => k }.map { case (k, v) => s"$k=$v" }.mkString(", ")

  private def frequencyMap(values: List[String]): Map[String, Int] =
    values.filter(_.trim.nonEmpty).groupBy(identity).view.mapValues(_.size).toMap

  private def averageInt(values: List[Int]): Double =
    if values.isEmpty then 0.0 else values.sum.toDouble / values.size.toDouble

  private def averageLong(values: List[Long]): Double =
    if values.isEmpty then 0.0 else values.sum.toDouble / values.size.toDouble

  private def percentileLong(values: List[Long], p: Double): Double =
    if values.isEmpty then 0.0
    else
      val sorted = values.sorted
      val idx = math.ceil(sorted.size.toDouble * p).toInt - 1
      sorted(idx.max(0).min(sorted.size - 1)).toDouble

  private def pct(numerator: Int, denominator: Int): Double =
    if denominator <= 0 then 0.0
    else numerator.toDouble / denominator.toDouble

  private def escapePipe(value: String): String =
    value.replace("|", "\\|")
