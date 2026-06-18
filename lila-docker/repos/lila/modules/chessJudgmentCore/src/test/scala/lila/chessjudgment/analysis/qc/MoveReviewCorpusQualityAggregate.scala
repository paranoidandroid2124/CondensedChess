package lila.chessjudgment.analysis.qc

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import chess.format.pgn.PgnStr
import lila.core.game.Game
import lila.chessjudgment.analysis.assembly.{ MoveReviewJudgmentOrchestrator, MoveReviewJudgmentResult, RawMoveReviewInput }
import lila.chessjudgment.model.strategic.VariationLine
import lila.tree.{ Branch, ExportOptions, ParseImport, Root, TreeBuilder }
import play.api.libs.json.*

final case class MoveReviewQualitySample(
    sampleId: String,
    result: Option[MoveReviewJudgmentResult]
)

final case class LayerGapAggregate(
    layer: JudgmentGraphLayer,
    sampleCount: Int,
    totalSlots: Int,
    missingSlots: Int,
    gapPercent: Double,
    missingBySlot: Map[JudgmentGraphSlot, Int],
    missingByOwner: Map[JudgmentGraphOwner, Int]
)

final case class CorpusIssueAggregate(
    kind: ChessQualityIssueKind,
    count: Int
)

final case class SemanticCoverageAggregate(
    sampleCount: Int,
    tacticalIdeaSamples: Int,
    strategicIdeaSamples: Int,
    pawnStructureIdeaSamples: Int,
    openingIdeaSamples: Int,
    defensiveIdeaSamples: Int,
    evaluationIdeaSamples: Int,
    conversionIdeaSamples: Int,
    relativeAssessmentSamples: Int,
    candidateSetComparisonSamples: Int,
    onlyMoveSignalSamples: Int,
    forcedLineThemeSamples: Int
)

final case class ClaimPromotionAggregate(
    sampleCount: Int,
    totalIdeas: Int,
    totalClaims: Int,
    totalClaimsWithEngineComparison: Int,
    claimPromotionRate: Double
)

final case class MoveReviewCorpusQualityReport(
    sampleCount: Int,
    builtCount: Int,
    failedBuildCount: Int,
    overallLayerGapPercent: Double,
    layerGaps: List[LayerGapAggregate],
    issues: List[CorpusIssueAggregate],
    semanticCoverage: SemanticCoverageAggregate,
    claimPromotion: ClaimPromotionAggregate
)

object MoveReviewCorpusQualityAggregate:

  def fromSamples(samples: List[MoveReviewQualitySample]): MoveReviewCorpusQualityReport =
    val built = samples.flatMap(_.result)
    val layerGaps = aggregateLayerGaps(built)
    val totalSlots = layerGaps.map(_.totalSlots).sum
    val missingSlots = layerGaps.map(_.missingSlots).sum
    MoveReviewCorpusQualityReport(
      sampleCount = samples.size,
      builtCount = built.size,
      failedBuildCount = samples.size - built.size,
      overallLayerGapPercent = percent(missingSlots, totalSlots),
      layerGaps = layerGaps,
      issues = aggregateIssues(built),
      semanticCoverage = aggregateSemanticCoverage(built),
      claimPromotion = aggregateClaimPromotion(built)
    )

  private def aggregateLayerGaps(results: List[MoveReviewJudgmentResult]): List[LayerGapAggregate] =
    val byLayer =
      results.flatMap(_.quality.layerGaps.layers).groupBy(_.layer)
    JudgmentGraphLayer.values.toList.flatMap { layer =>
      byLayer.get(layer).map { metrics =>
        val totalSlots = metrics.map(_.totalSlots).sum
        val missing = metrics.flatMap(_.missingSlots)
        LayerGapAggregate(
          layer = layer,
          sampleCount = metrics.size,
          totalSlots = totalSlots,
          missingSlots = missing.size,
          gapPercent = percent(missing.size, totalSlots),
          missingBySlot = countBy(missing)(_.slot),
          missingByOwner = countBy(missing)(_.owner)
        )
      }
    }

  private def aggregateIssues(results: List[MoveReviewJudgmentResult]): List[CorpusIssueAggregate] =
    countBy(results.flatMap(_.quality.audit.issues))(_.kind)
      .toList
      .sortBy((kind, _) => kind.ordinal)
      .map(CorpusIssueAggregate.apply)

  private def aggregateSemanticCoverage(results: List[MoveReviewJudgmentResult]): SemanticCoverageAggregate =
    val coverage = results.map(_.quality.semanticCoverage)
    SemanticCoverageAggregate(
      sampleCount = results.size,
      tacticalIdeaSamples = coverage.count(_.tacticalIdeas > 0),
      strategicIdeaSamples = coverage.count(_.strategicIdeas > 0),
      pawnStructureIdeaSamples = coverage.count(_.pawnStructureIdeas > 0),
      openingIdeaSamples = coverage.count(_.openingIdeas > 0),
      defensiveIdeaSamples = coverage.count(_.defensiveIdeas > 0),
      evaluationIdeaSamples = coverage.count(_.evaluationIdeas > 0),
      conversionIdeaSamples = coverage.count(_.conversionIdeas > 0),
      relativeAssessmentSamples = coverage.count(_.hasRelativeAssessment),
      candidateSetComparisonSamples = coverage.count(_.hasCandidateSetComparison),
      onlyMoveSignalSamples = coverage.count(_.hasOnlyMoveSignal),
      forcedLineThemeSamples = coverage.count(_.hasForcedLineTheme)
    )

  private def aggregateClaimPromotion(results: List[MoveReviewJudgmentResult]): ClaimPromotionAggregate =
    val promotions = results.map(_.quality.claimPromotion)
    val totalIdeas = promotions.map(_.ideas).sum
    val totalClaims = promotions.map(_.claims).sum
    ClaimPromotionAggregate(
      sampleCount = results.size,
      totalIdeas = totalIdeas,
      totalClaims = totalClaims,
      totalClaimsWithEngineComparison = promotions.map(_.claimsWithEngineComparison).sum,
      claimPromotionRate = percent(totalClaims, totalIdeas) / 100d
    )

  private def countBy[A, K](items: List[A])(key: A => K): Map[K, Int] =
    items.groupMapReduce(key)(_ => 1)(_ + _)

  private def percent(numerator: Int, denominator: Int): Double =
    if denominator == 0 then 0d else numerator.toDouble / denominator.toDouble * 100d

object MoveReviewRawInputQualityRunner:

  private final case class ParsedSample(
      sampleId: String,
      raw: RawMoveReviewInput
  )

  private given Reads[RawMoveReviewInput] = Reads { json =>
    for
      fen <- (json \ "fen").validate[String]
      playedMoveUci <- (json \ "playedMoveUci").validate[String]
      variations <- (json \ "variations").validate[List[VariationLine]]
    yield
      RawMoveReviewInput(
        fen = fen,
        playedMoveUci = playedMoveUci,
        variations = variations,
        currentEvalCp = (json \ "currentEvalCp").asOpt[Int],
        ply = (json \ "ply").asOpt[Int]
      )
  }

  def main(args: Array[String]): Unit =
    if args.isEmpty then
      throw IllegalArgumentException("usage: MoveReviewRawInputQualityRunner <input.json|jsonl> [output.tsv]")
    val inputPath = Path.of(args(0))
    val outputPath = args.lift(1).map(Path.of(_))
    val parsed = parseSamples(inputPath)
    val samples =
      parsed.map(sample =>
        MoveReviewQualitySample(
          sampleId = sample.sampleId,
          result = MoveReviewJudgmentOrchestrator.build(sample.raw)
        )
      )
    val report = MoveReviewCorpusQualityAggregate.fromSamples(samples)
    val output = MoveReviewQualityReportFormatter.format(report)
    outputPath match
      case Some(path) => Files.writeString(path, output, StandardCharsets.UTF_8)
      case None       => println(output)

  private def parseSamples(path: Path): List[ParsedSample] =
    MoveReviewQualityInputFiles.parseJsonDocuments(path).zipWithIndex.map { case (json, index) =>
      parseSample(json, index)
    }

  private def parseSample(json: JsValue, index: Int): ParsedSample =
    val sampleId = (json \ "sampleId").asOpt[String].getOrElse((index + 1).toString)
    val body = (json \ "input").toOption.getOrElse(json)
    body.validate[RawMoveReviewInput] match
      case JsSuccess(raw, _) => ParsedSample(sampleId, raw)
      case JsError(errors)   => throw IllegalArgumentException(s"invalid sample $sampleId: $errors")

object MoveReviewPgnQualityRunner:

  private final case class ParsedPgnSample(
      sampleId: String,
      pgn: String,
      snapshots: List[PgnMoveEngineSnapshot]
  )

  private final case class PgnMoveEngineSnapshot(
      sampleId: Option[String],
      beforePly: Int,
      variations: List[VariationLine],
      currentEvalCp: Option[Int]
  )

  private final case class MainlineMove(
      beforePly: Int,
      beforeFen: String,
      playedMoveUci: String
  )

  private given Reads[PgnMoveEngineSnapshot] = Reads { json =>
    for
      beforePly <- (json \ "beforePly").validateOpt[Int].flatMap:
        case Some(ply) => JsSuccess(ply)
        case None      => (json \ "ply").validate[Int]
      variations <- (json \ "variations").validate[List[VariationLine]]
    yield
      PgnMoveEngineSnapshot(
        sampleId = (json \ "sampleId").asOpt[String],
        beforePly = beforePly,
        variations = variations,
        currentEvalCp = (json \ "currentEvalCp").asOpt[Int]
      )
  }

  def main(args: Array[String]): Unit =
    if args.isEmpty then
      throw IllegalArgumentException("usage: MoveReviewPgnQualityRunner <input.json|jsonl> [output.tsv]")
    val inputPath = Path.of(args(0))
    val outputPath = args.lift(1).map(Path.of(_))
    val samples =
      rawSamples(inputPath).map { case (sampleId, raw) =>
        MoveReviewQualitySample(
          sampleId = sampleId,
          result = MoveReviewJudgmentOrchestrator.build(raw)
        )
      }
    val report = MoveReviewCorpusQualityAggregate.fromSamples(samples)
    val output = MoveReviewQualityReportFormatter.format(report)
    outputPath match
      case Some(path) => Files.writeString(path, output, StandardCharsets.UTF_8)
      case None       => println(output)

  private[qc] def rawSamples(path: Path): List[(String, RawMoveReviewInput)] =
    parseSamples(path).flatMap(expandRawSample)

  private def parseSamples(path: Path): List[ParsedPgnSample] =
    MoveReviewQualityInputFiles.parseJsonDocuments(path).zipWithIndex.map { case (json, index) =>
      val sampleId = (json \ "sampleId").asOpt[String].getOrElse((index + 1).toString)
      val snapshotsJson =
        (json \ "positions").toOption
          .orElse((json \ "moves").toOption)
          .orElse((json \ "snapshots").toOption)
      val snapshots = snapshotsJson match
        case Some(value) =>
          value.validate[List[PgnMoveEngineSnapshot]] match
            case JsSuccess(value, _) => value
            case JsError(errors)     => throw IllegalArgumentException(s"invalid PGN sample $sampleId snapshots: $errors")
        case None => throw IllegalArgumentException(s"invalid PGN sample $sampleId: missing positions, moves, or snapshots")
      val pgn = (json \ "pgn").asOpt[String].getOrElse:
        throw IllegalArgumentException(s"invalid PGN sample $sampleId: missing pgn")
      ParsedPgnSample(sampleId = sampleId, pgn = pgn, snapshots = snapshots)
    }

  private def expandRawSample(sample: ParsedPgnSample): List[(String, RawMoveReviewInput)] =
    val movesByPly = parseMainline(sample)
    sample.snapshots.map { snapshot =>
      val move = movesByPly.getOrElse(
        snapshot.beforePly,
        throw IllegalArgumentException(s"PGN sample ${sample.sampleId}: no played move at beforePly ${snapshot.beforePly}")
      )
      val raw = RawMoveReviewInput(
        fen = move.beforeFen,
        playedMoveUci = move.playedMoveUci,
        variations = snapshot.variations,
        currentEvalCp = snapshot.currentEvalCp,
        ply = Some(snapshot.beforePly)
      )
      snapshot.sampleId.getOrElse(s"${sample.sampleId}:${snapshot.beforePly}") -> raw
    }

  private def parseMainline(sample: ParsedPgnSample): Map[Int, MainlineMove] =
    val imported =
      ParseImport.full(PgnStr(sample.pgn)) match
        case Right(result) => result
        case Left(error)   => throw IllegalArgumentException(s"invalid PGN sample ${sample.sampleId}: ${error.value}")
    imported.replayError.foreach: error =>
      throw IllegalArgumentException(s"invalid PGN sample ${sample.sampleId} replay: ${error.value}")
    val initialFen = imported.initialFen.getOrElse(imported.game.variant.initialFen)
    val game = Game.make(imported.game.variant, imported.initialFen).copy(chess = imported.game)
    val root =
      TreeBuilder(
        game = game,
        analysis = None,
        initialFen = initialFen,
        withFlags = ExportOptions.default,
        logChessError = error => throw IllegalArgumentException(s"invalid PGN sample ${sample.sampleId} tree: $error")
      )
    mainlineMoves(root).map(move => move.beforePly -> move).toMap

  private def mainlineMoves(root: Root): List[MainlineMove] =
    def loop(beforeFen: String, beforePly: Int, branches: List[Branch], acc: List[MainlineMove]): List[MainlineMove] =
      branches match
        case Nil => acc.reverse
        case branch :: rest =>
          val move =
            MainlineMove(
              beforePly = beforePly,
              beforeFen = beforeFen,
              playedMoveUci = branch.move.uci.uci
            )
          loop(branch.fen.value, branch.ply.value, rest, move :: acc)
    loop(root.fen.value, root.ply.value, root.mainline, Nil)

object MoveReviewPgnRawInputRunner:

  private given Writes[RawMoveReviewInput] = Json.writes[RawMoveReviewInput]

  def main(args: Array[String]): Unit =
    if args.isEmpty then
      throw IllegalArgumentException("usage: MoveReviewPgnRawInputRunner <input.json|jsonl> [output.jsonl]")
    val inputPath = Path.of(args(0))
    val outputPath = args.lift(1).map(Path.of(_))
    val lines =
      MoveReviewPgnQualityRunner.rawSamples(inputPath).map { case (sampleId, raw) =>
        Json.stringify(Json.obj("sampleId" -> sampleId, "input" -> Json.toJson(raw)))
      }
    val output = lines.mkString(System.lineSeparator())
    outputPath match
      case Some(path) => Files.writeString(path, output, StandardCharsets.UTF_8)
      case None       => println(output)

private object MoveReviewQualityInputFiles:

  def parseJsonDocuments(path: Path): List[JsValue] =
    val raw = Files.readString(path, StandardCharsets.UTF_8).stripPrefix("\uFEFF").trim
    if raw.startsWith("[") then Json.parse(raw).as[List[JsValue]]
    else raw.linesIterator.toList.filter(_.trim.nonEmpty).map(line => Json.parse(line))

private object MoveReviewQualityReportFormatter:

  def format(report: MoveReviewCorpusQualityReport): String =
    val header =
      List(
        s"sample_count\t${report.sampleCount}",
        s"built_count\t${report.builtCount}",
        s"failed_build_count\t${report.failedBuildCount}",
        f"overall_layer_gap_percent\t${report.overallLayerGapPercent}%.2f",
        f"claim_promotion_rate\t${report.claimPromotion.claimPromotionRate}%.4f"
      )
    val layerRows =
      report.layerGaps.map { layer =>
        val missingSlots = layer.missingBySlot.toList.sortBy(_._1.ordinal).map((slot, count) => s"$slot:$count").mkString(",")
        val missingOwners = layer.missingByOwner.toList.sortBy(_._1.ordinal).map((owner, count) => s"$owner:$count").mkString(",")
        f"layer\t${layer.layer}\t${layer.sampleCount}\t${layer.totalSlots}\t${layer.missingSlots}\t${layer.gapPercent}%.2f\t$missingSlots\t$missingOwners"
      }
    val issueRows =
      report.issues.map(issue => s"issue\t${issue.kind}\t${issue.count}")
    val semantic = report.semanticCoverage
    val semanticRows =
      List(
        s"semantic\ttactical\t${semantic.tacticalIdeaSamples}",
        s"semantic\tstrategic\t${semantic.strategicIdeaSamples}",
        s"semantic\tpawn_structure\t${semantic.pawnStructureIdeaSamples}",
        s"semantic\topening\t${semantic.openingIdeaSamples}",
        s"semantic\tdefensive\t${semantic.defensiveIdeaSamples}",
        s"semantic\tevaluation\t${semantic.evaluationIdeaSamples}",
        s"semantic\tconversion\t${semantic.conversionIdeaSamples}",
        s"semantic\trelative_assessment\t${semantic.relativeAssessmentSamples}",
        s"semantic\tcandidate_set_comparison\t${semantic.candidateSetComparisonSamples}",
        s"semantic\tonly_move_signal\t${semantic.onlyMoveSignalSamples}",
        s"semantic\tforced_line_theme\t${semantic.forcedLineThemeSamples}"
      )
    (header ++ layerRows ++ issueRows ++ semanticRows).mkString(System.lineSeparator())
