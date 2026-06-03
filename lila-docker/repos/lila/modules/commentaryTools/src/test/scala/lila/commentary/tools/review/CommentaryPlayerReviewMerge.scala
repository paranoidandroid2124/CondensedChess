package lila.commentary.tools.review

import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.Json

import scala.jdk.CollectionConverters.*

object CommentaryPlayerReviewMerge:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      judgmentsDir: Path = DefaultReviewDir,
      reviewQueuePath: Path = DefaultReviewDir.resolve("review_queue.jsonl"),
      blockersPath: Path = DefaultReportDir.resolve("blockers.md"),
      phraseClustersPath: Path = DefaultReportDir.resolve("phrase-family-clusters.md"),
      fixPriorityPath: Path = DefaultReportDir.resolve("fix-priority.json"),
      auditReportPath: Path = DefaultReportDir.resolve("audit-report.md")
  )

  private val RootCauseHints = Map(
    FixFamily.GenericFillerMainProse -> "MoveReview main prose is collapsing into safe filler instead of leading with the anchored idea.",
    FixFamily.AnchoredSupportMissingFromProse -> "Specific support evidence exists, but it is staying in support rows instead of the body.",
    FixFamily.ConditionalityBlur -> "Candidate/provisional ideas are not keeping their provenance and conditional framing in the main prose.",
    FixFamily.MisanchoredConcreteClaim -> "Specific strategic claims are being stated without a stable concrete carrier.",
    FixFamily.StrategicFlattening -> "Strategic richness is being compressed down to generic safe language."
  )

  private val PatchTargets = Map(
    FixFamily.GenericFillerMainProse -> List("MoveReview main-thesis selection", "MoveReview compression"),
    FixFamily.AnchoredSupportMissingFromProse -> List("MoveReview support-to-body promotion", "move-delta carrier"),
    FixFamily.ConditionalityBlur -> List("MoveReview provenance phrasing", "conditionality carryover"),
    FixFamily.MisanchoredConcreteClaim -> List("shared truth/provenance guard", "MoveReview concrete-claim gating"),
    FixFamily.StrategicFlattening -> List("MoveReview compression", "shared player-facing rewrite")
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val judgmentFiles =
      if !Files.exists(config.judgmentsDir) then Nil
      else
        Files
          .list(config.judgmentsDir)
          .iterator()
          .asScala
          .filter(path => Files.isRegularFile(path) && path.getFileName.toString.startsWith("judgments-") && path.getFileName.toString.endsWith(".jsonl"))
          .toList
          .sortBy(_.getFileName.toString)

    val judgments =
      judgmentFiles.flatMap { path =>
        readJsonLines[JudgmentEntry](path) match
          case Right(value) => value
          case Left(err)    => throw new IllegalArgumentException(s"failed to read `${path}`: $err")
      }

    val queueMetadata =
      if Files.exists(config.reviewQueuePath) then
        readJsonLines[ReviewQueueEntry](config.reviewQueuePath) match
          case Right(entries) => entries.map(entry => entry.sampleId -> entry).toMap
          case Left(err)      => throw new IllegalArgumentException(s"failed to read `${config.reviewQueuePath}`: $err")
      else Map.empty[String, ReviewQueueEntry]

    val blockers = judgments.filter(_.severity == ReviewSeverity.Blocker)
    val phraseClusters =
      judgments
        .flatMap(_.fixFamily)
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toList
        .sortBy { case (_, count) => -count }

    val priority =
      judgments
        .groupBy(_.blockerType.getOrElse("none"))
        .view
        .mapValues(entries =>
          Json.obj(
            "count" -> entries.size,
            "samples" -> entries.take(10).map(_.sampleId),
            "fixFamilies" -> entries.flatMap(_.fixFamily).distinct.sorted
          )
        )
        .toMap

    val enriched =
      judgments.map { judgment =>
        val metadata = queueMetadata.get(judgment.sampleId)
        val surface = judgment.surface.orElse(metadata.map(_.surface)).getOrElse("unknown")
        val reviewKind = judgment.reviewKind.orElse(metadata.map(_.reviewKind)).getOrElse(ReviewKind.MoveReviewFocus)
        val tier = judgment.tier.orElse(metadata.flatMap(_.tier)).getOrElse("unknown")
        val openingFamily = judgment.openingFamily.orElse(metadata.flatMap(_.openingFamily)).getOrElse("unknown")
        (judgment, surface, reviewKind, tier, openingFamily)
      }

    val familyCounts =
      enriched
        .flatMap { case (judgment, _, _, _, _) => judgment.fixFamily }
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toMap

    val surfaceFamilyCounts =
      nestedFamilyCounts(enriched, _._2)
    val tierFamilyCounts =
      nestedFamilyCounts(enriched, _._4)
    val openingFamilyCounts =
      nestedFamilyCounts(enriched, _._5)

    val exemplarBundle =
      enriched
        .flatMap { case (judgment, surface, reviewKind, tier, openingFamily) =>
          judgment.fixFamily.map(family =>
            family -> Json.obj(
              "sampleId" -> judgment.sampleId,
              "surface" -> surface,
              "reviewKind" -> reviewKind,
              "tier" -> tier,
              "openingFamily" -> openingFamily,
              "severity" -> judgment.severity,
              "notes" -> judgment.notes
            )
          )
        }
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2).take(5))
        .toMap

    ensureParent(config.blockersPath)
    ensureParent(config.phraseClustersPath)
    ensureParent(config.fixPriorityPath)
    ensureParent(config.auditReportPath)

    writeText(
      config.blockersPath,
      blockers
        .map { judgment =>
          val metadata = queueMetadata.get(judgment.sampleId)
          val surface = judgment.surface.orElse(metadata.map(_.surface)).getOrElse("unknown")
          val tier = judgment.tier.orElse(metadata.flatMap(_.tier)).getOrElse("unknown")
          s"- `${judgment.sampleId}` ($surface/$tier, ${judgment.blockerType.getOrElse("unspecified")}): ${judgment.notes}"
        }
        .mkString("# Blockers\n\n", "\n", if blockers.nonEmpty then "\n" else "")
    )
    writeText(
      config.phraseClustersPath,
      phraseClusters
        .map { case (family, count) => s"- `$family`: $count" }
        .mkString("# Phrase Family Clusters\n\n", "\n", if phraseClusters.nonEmpty then "\n" else "")
    )
    writeJson(
      config.fixPriorityPath,
      Json.obj(
        "generatedAt" -> java.time.Instant.now().toString,
        "priorities" -> priority,
        "familyCounts" -> familyCounts,
        "surfaceFamilyCounts" -> surfaceFamilyCounts,
        "tierFamilyCounts" -> tierFamilyCounts,
        "openingFamilyCounts" -> openingFamilyCounts,
        "rootCauseHints" -> RootCauseHints,
        "patchTargets" -> PatchTargets,
        "exemplarBundle" -> exemplarBundle
      )
    )
    writeText(config.auditReportPath, renderAuditReport(familyCounts, surfaceFamilyCounts, tierFamilyCounts, openingFamilyCounts, exemplarBundle))

    println(
      s"[player-qc-merge] wrote `${config.blockersPath}`, `${config.phraseClustersPath}`, `${config.fixPriorityPath}`, `${config.auditReportPath}`"
    )

  private def nestedFamilyCounts(
      enriched: List[(JudgmentEntry, String, String, String, String)],
      keyOf: ((JudgmentEntry, String, String, String, String)) => String
  ): Map[String, Map[String, Int]] =
    enriched
      .flatMap { enrichedJudgment =>
        val bucket = keyOf(enrichedJudgment)
        enrichedJudgment._1.fixFamily.map(family => (bucket, family))
      }
      .groupBy(_._1)
      .view
      .mapValues(entries =>
        entries
          .map(_._2)
          .groupBy(identity)
          .view
          .mapValues(_.size)
          .toMap
      )
      .toMap

  private def renderAuditReport(
      familyCounts: Map[String, Int],
      surfaceFamilyCounts: Map[String, Map[String, Int]],
      tierFamilyCounts: Map[String, Map[String, Int]],
      openingFamilyCounts: Map[String, Map[String, Int]],
      exemplarBundle: Map[String, Seq[play.api.libs.json.JsObject]]
  ): String =
    val sb = new StringBuilder()
    sb.append("# 202 Audit Report\n\n")
    familyCounts.toList.sortBy { case (_, count) => -count }.foreach { case (family, count) =>
      sb.append(s"## `$family`\n\n")
      sb.append(s"- Count: `$count`\n")
      sb.append(s"- Root cause: ${RootCauseHints.getOrElse(family, "manual review required")}\n")
      sb.append(s"- Patch targets: ${PatchTargets.getOrElse(family, Nil).mkString(", ")}\n")
      sb.append(s"- Surfaces: ${renderBreakdown(surfaceFamilyCounts, family)}\n")
      sb.append(s"- Tiers: ${renderBreakdown(tierFamilyCounts, family)}\n")
      sb.append(s"- Opening families: ${renderBreakdown(openingFamilyCounts, family)}\n")
      val exemplars =
        exemplarBundle
          .getOrElse(family, Nil)
          .map(js =>
            s"`${(js \ "sampleId").as[String]}` (${(js \ "surface").as[String]}/${(js \ "tier").as[String]}): ${(js \ "notes").as[String]}"
          )
      if exemplars.nonEmpty then
        sb.append("- Exemplars:\n")
        exemplars.foreach(line => sb.append(s"  - $line\n"))
      sb.append('\n')
    }
    sb.toString

  private def renderBreakdown(buckets: Map[String, Map[String, Int]], family: String): String =
    buckets.toList
      .flatMap { case (bucket, counts) => counts.get(family).map(count => bucket -> count) }
      .sortBy { case (_, count) => -count }
      .map { case (bucket, count) => s"$bucket=$count" }
      .mkString(", ")

  private def parseConfig(args: List[String]): Config =
    val positional = args.filterNot(_.startsWith("--"))
    Config(
      judgmentsDir = positional.headOption.map(Paths.get(_)).getOrElse(DefaultReviewDir),
      reviewQueuePath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultReviewDir.resolve("review_queue.jsonl")),
      blockersPath = positional.lift(2).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("blockers.md")),
      phraseClustersPath = positional.lift(3).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("phrase-family-clusters.md")),
      fixPriorityPath = positional.lift(4).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("fix-priority.json")),
      auditReportPath = positional.lift(5).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("audit-report.md"))
    )
