package lila.chessjudgment.analysis.qc

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import chess.format.pgn.PgnStr
import lila.core.game.Game
import lila.chessjudgment.analysis.assembly.{
  MoveReviewInputNormalizer,
  MoveReviewJudgmentOrchestrator,
  MoveReviewJudgmentResult,
  RawOpeningContext,
  RawMoveReviewInput
}
import lila.chessjudgment.model.strategic.VariationLine
import lila.tree.{ Branch, ExportOptions, ParseImport, Root, TreeBuilder }
import play.api.libs.json.*

private object RawOpeningContextJson:

  given Reads[RawOpeningContext] = Reads { json =>
    JsSuccess(
      RawOpeningContext(
        eco = (json \ "eco").asOpt[String].orElse((json \ "ecoCode").asOpt[String]),
        name = (json \ "name").asOpt[String].orElse((json \ "opening").asOpt[String]),
        family = (json \ "family").asOpt[String].orElse((json \ "ecoFamily").asOpt[String])
      )
    )
  }

  given Writes[RawOpeningContext] = Json.writes[RawOpeningContext]

  def from(json: JsValue): Option[RawOpeningContext] =
    merge(
      (json \ "openingContext").toOption.flatMap(_.validate[RawOpeningContext].asOpt),
      (json \ "openingIdentity").toOption.flatMap(_.validate[RawOpeningContext].asOpt),
      (json \ "opening").toOption.collect { case obj: JsObject => obj }.flatMap(_.validate[RawOpeningContext].asOpt),
      flatFields(json)
    )

  private def flatFields(json: JsValue): Option[RawOpeningContext] =
    merge(
      Some(
        RawOpeningContext(
          eco = (json \ "eco").asOpt[String].orElse((json \ "ecoCode").asOpt[String]),
          name = (json \ "opening").asOpt[String],
          family = (json \ "ecoFamily").asOpt[String].orElse((json \ "family").asOpt[String])
        )
      )
    )

  private def merge(contexts: Option[RawOpeningContext]*): Option[RawOpeningContext] =
    val available = contexts.flatten.toList
    val merged =
      RawOpeningContext(
        eco = firstText(available.flatMap(_.eco)),
        name = firstText(available.flatMap(_.name)),
        family = firstText(available.flatMap(_.family))
      )
    Option.when(merged.eco.nonEmpty || merged.name.nonEmpty || merged.family.nonEmpty)(merged)

  private def firstText(values: List[String]): Option[String] =
    values.map(_.trim).find(_.nonEmpty)

private object MoveReviewPgnInputSamples:
  private final case class ParsedPgnSample(
      sampleId: String,
      pgn: String,
      snapshots: List[PgnMoveEngineSnapshot],
      openingContext: Option[RawOpeningContext]
  )

  private final case class PgnMoveEngineSnapshot(
      sampleId: Option[String],
      beforePly: Int,
      variations: List[VariationLine],
      currentEvalCp: Option[Int],
      openingContext: Option[RawOpeningContext]
  )

  private final case class MainlineMove(
      beforePly: Int,
      beforeFen: String,
      playedMoveUci: String,
      movePrefixUci: List[String]
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
        currentEvalCp = (json \ "currentEvalCp").asOpt[Int],
        openingContext = RawOpeningContextJson.from(json)
      )
  }

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
      ParsedPgnSample(
        sampleId = sampleId,
        pgn = pgn,
        snapshots = snapshots,
        openingContext = RawOpeningContextJson.from(json)
      )
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
        ply = Some(snapshot.beforePly),
        openingContext = snapshot.openingContext.orElse(sample.openingContext),
        movePrefixUci = move.movePrefixUci
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
    def loop(
        beforeFen: String,
        beforePly: Int,
        branches: List[Branch],
        prefix: List[String],
        acc: List[MainlineMove]
    ): List[MainlineMove] =
      branches match
        case Nil => acc.reverse
        case branch :: rest =>
          val played = branch.move.uci.uci
          val move =
            MainlineMove(
              beforePly = beforePly,
              beforeFen = beforeFen,
              playedMoveUci = played,
              movePrefixUci = prefix
            )
          loop(branch.fen.value, branch.ply.value, rest, prefix :+ played, move :: acc)
    loop(root.fen.value, root.ply.value, root.mainline, Nil, Nil)

object MoveReviewPgnRawInputRunner:
  import RawOpeningContextJson.given

  private given Writes[RawMoveReviewInput] = Json.writes[RawMoveReviewInput]

  def main(args: Array[String]): Unit =
    if args.isEmpty then
      throw IllegalArgumentException("usage: MoveReviewPgnRawInputRunner <input.json|jsonl> [output.jsonl]")
    val inputPath = Path.of(args(0))
    val outputPath = args.lift(1).map(Path.of(_))
    val lines =
      MoveReviewPgnInputSamples.rawSamples(inputPath).map { case (sampleId, raw) =>
        Json.stringify(Json.obj("sampleId" -> sampleId, "input" -> Json.toJson(raw)))
      }
    val output = lines.mkString(System.lineSeparator())
    outputPath match
      case Some(path) => Files.writeString(path, output, StandardCharsets.UTF_8)
      case None       => println(output)

object MoveReviewPhase3AuditRunner:
  private final case class AuditInputSample(
      sampleId: String,
      raw: RawMoveReviewInput,
      opening: Option[String],
      sliceKind: Option[String],
      targetPly: Option[Int],
      playedSan: Option[String],
      sourcePath: Option[String]
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
        ply = (json \ "ply").asOpt[Int],
        openingContext = RawOpeningContextJson.from(json),
        movePrefixUci = (json \ "movePrefixUci").asOpt[List[String]].getOrElse(Nil)
      )
  }

  def main(args: Array[String]): Unit =
    if args.isEmpty then
      throw IllegalArgumentException("usage: MoveReviewPhase3AuditRunner <input.json|jsonl> [output.jsonl]")
    val inputPath = Path.of(args(0))
    val outputPath = args.lift(1).map(Path.of(_))
    val rows =
      MoveReviewQualityInputFiles.parseJsonDocuments(inputPath).zipWithIndex.map { case (json, index) =>
        auditSample(parseSample(json, index))
      }
    val output = rows.map(row => Json.stringify(row)).mkString(System.lineSeparator())
    outputPath match
      case Some(path) => Files.writeString(path, output, StandardCharsets.UTF_8)
      case None       => println(output)

  private def parseSample(json: JsValue, index: Int): AuditInputSample =
    parseDirectRaw(json, index).orElse(parseLegacyMoveReviewOutput(json, index)) match
      case Some(sample) => sample
      case None =>
        val sampleId = (json \ "sampleId").asOpt[String].getOrElse((index + 1).toString)
        throw IllegalArgumentException(
          s"invalid phase3 sample $sampleId: expected RawMoveReviewInput or legacy move_review_outputs row with rawResponsePath"
        )

  private def parseDirectRaw(json: JsValue, index: Int): Option[AuditInputSample] =
    val sampleId = (json \ "sampleId").asOpt[String].getOrElse((index + 1).toString)
    val body = (json \ "input").toOption.getOrElse(json)
    body.validate[RawMoveReviewInput] match
      case JsSuccess(raw, _) =>
        val enrichedRaw =
          raw.copy(openingContext = raw.openingContext.orElse(RawOpeningContextJson.from(json)))
        Some(
          AuditInputSample(
            sampleId = sampleId,
            raw = enrichedRaw,
            opening = (json \ "opening").asOpt[String].orElse(enrichedRaw.openingContext.flatMap(_.name)),
            sliceKind = (json \ "sliceKind").asOpt[String],
            targetPly = (json \ "targetPly").asOpt[Int].orElse(enrichedRaw.ply),
            playedSan = (json \ "playedSan").asOpt[String],
            sourcePath = None
          )
        )
      case JsError(_) => None

  private def parseLegacyMoveReviewOutput(json: JsValue, index: Int): Option[AuditInputSample] =
    for
      fen <- (json \ "fen").asOpt[String]
      playedUci <- (json \ "playedUci").asOpt[String]
      rawResponsePath <- (json \ "rawResponsePath").asOpt[String]
      rawResponse = Json.parse(Files.readString(Path.of(rawResponsePath), StandardCharsets.UTF_8))
      variations <- (rawResponse \ "variations").asOpt[List[VariationLine]]
      if variations.nonEmpty
    yield
      AuditInputSample(
        sampleId = (json \ "sampleId").asOpt[String].getOrElse((index + 1).toString),
        raw = RawMoveReviewInput(
          fen = fen,
          playedMoveUci = playedUci,
          variations = variations,
          currentEvalCp = (json \ "currentEvalCp").asOpt[Int].orElse((rawResponse \ "currentEvalCp").asOpt[Int]),
          ply = (json \ "targetPly").asOpt[Int],
          openingContext = RawOpeningContextJson.from(json),
          movePrefixUci = (json \ "movePrefixUci").asOpt[List[String]].getOrElse(Nil)
        ),
        opening = (json \ "opening").asOpt[String],
        sliceKind = (json \ "sliceKind").asOpt[String],
        targetPly = (json \ "targetPly").asOpt[Int],
        playedSan = (json \ "playedSan").asOpt[String],
        sourcePath = Some(rawResponsePath)
      )

  private def auditSample(sample: AuditInputSample): JsObject =
    val result = MoveReviewJudgmentOrchestrator.build(sample.raw)
    val base =
      Json.obj(
        "schemaVersion" -> "move_review_phase3_audit_row.v1",
        "sampleId" -> sample.sampleId,
        "opening" -> sample.opening,
        "sliceKind" -> sample.sliceKind,
        "targetPly" -> sample.targetPly,
        "fen" -> sample.raw.fen,
        "playedSan" -> sample.playedSan,
        "playedUci" -> sample.raw.playedMoveUci,
        "sourcePath" -> sample.sourcePath,
        "inputDiagnostics" -> inputDiagnostics(sample.raw),
        "manualReview" -> Json.obj(
          "status" -> "required",
          "openingRead" -> JsNull,
          "positionRead" -> JsNull,
          "expectedPrimaryTheme" -> JsNull,
          "expectedVerdict" -> JsNull,
          "graphGapKind" -> JsNull,
          "notes" -> JsNull
        )
      )
    result match
      case Some(built) =>
        base ++ Json.obj(
          "buildStatus" -> "built",
          "lineSummary" -> lineSummary(built),
          "relativeAssessment" -> relativeSummary(built),
          "semanticCoverage" -> semanticSummary(built),
          "layerGaps" -> layerGapSummary(built),
          "issues" -> Json.toJson(built.quality.audit.issues.map(_.kind.toString)),
          "evidenceLoss" -> evidenceLossSummary(built),
          "evidenceLayerCounts" -> evidenceLayerCounts(built),
          "relationKinds" -> relationKinds(built),
          "strategicKinds" -> strategicKinds(built),
          "ideaFamilies" -> Json.toJson(built.packet.ideas.map(_.ref.family.toString)),
          "claimFamilies" -> Json.toJson(built.packet.claims.map(_.family.toString)),
          "topClaims" -> topClaims(built)
        )
      case None =>
        base ++ Json.obj(
          "buildStatus" -> "failed",
          "lineSummary" -> Json.obj(
            "inputVariationCount" -> sample.raw.variations.size,
            "inputRootMoves" -> sample.raw.variations.flatMap(_.moves.headOption)
          )
        )

  private def lineSummary(result: MoveReviewJudgmentResult): JsObject =
    def line(role: lila.chessjudgment.model.judgment.LineNodeRole): Option[JsObject] =
      result.packet.candidateLines.find(_.role == role).map(node =>
        Json.obj(
          "role" -> node.role.toString,
          "rootMove" -> node.ref.rootMove,
          "rank" -> node.ref.rank,
          "evalCp" -> node.evalCp,
          "mate" -> node.mate,
          "depth" -> node.depth,
          "moves" -> node.line.moves
        )
      )
    Json.obj(
      "candidateCount" -> result.packet.candidateLines.size,
      "reference" -> line(lila.chessjudgment.model.judgment.LineNodeRole.BestReference),
      "played" -> line(lila.chessjudgment.model.judgment.LineNodeRole.Played),
      "alternatives" -> JsArray(
        result.packet.candidateLines
          .filter(_.role == lila.chessjudgment.model.judgment.LineNodeRole.Alternative)
          .map(node =>
            Json.obj(
              "rootMove" -> node.ref.rootMove,
              "rank" -> node.ref.rank,
              "evalCp" -> node.evalCp,
              "mate" -> node.mate,
              "depth" -> node.depth
            )
          )
      )
    )

  private def relativeSummary(result: MoveReviewJudgmentResult): JsValue =
    result.packet.relativeAssessments.headOption match
      case Some(assessment) =>
        val comparison = assessment.comparison
        Json.obj(
          "verdict" -> comparison.verdict.toString,
          "mover" -> comparison.mover.name,
          "cpLossForMover" -> comparison.cpLossForMover,
          "winPercentLossForMover" -> comparison.winPercentLossForMover,
          "candidateDeltaForMover" -> comparison.candidateDeltaForMover,
          "candidateWinPercentDeltaForMover" -> comparison.candidateWinPercentDeltaForMover,
          "bestToSecondGapForMover" -> comparison.candidateSet.flatMap(_.bestToSecondGapForMover),
          "bestToSecondWinPercentGapForMover" -> comparison.candidateSet.flatMap(_.bestToSecondWinPercentGapForMover),
          "candidateCount" -> comparison.candidateSet.map(_.candidateCount),
          "onlyMove" -> comparison.candidateSet.exists(_.onlyMove)
        )
      case None => JsNull

  private def semanticSummary(result: MoveReviewJudgmentResult): JsObject =
    val semantic = result.quality.semanticCoverage
    Json.obj(
      "tacticalIdeas" -> semantic.tacticalIdeas,
      "strategicIdeas" -> semantic.strategicIdeas,
      "pawnStructureIdeas" -> semantic.pawnStructureIdeas,
      "openingIdeas" -> semantic.openingIdeas,
      "defensiveIdeas" -> semantic.defensiveIdeas,
      "evaluationIdeas" -> semantic.evaluationIdeas,
      "conversionIdeas" -> semantic.conversionIdeas,
      "claimFamilies" -> semantic.claimFamilies.map(_.toString).toList.sorted,
      "hasRelativeAssessment" -> semantic.hasRelativeAssessment,
      "hasVerdict" -> semantic.hasVerdict,
      "hasCandidateSetComparison" -> semantic.hasCandidateSetComparison,
      "hasOnlyMoveSignal" -> semantic.hasOnlyMoveSignal,
      "hasForcedLineTheme" -> semantic.hasForcedLineTheme
    )

  private def layerGapSummary(result: MoveReviewJudgmentResult): JsArray =
    JsArray(
      result.quality.layerGaps.layers.map(layer =>
        Json.obj(
          "layer" -> layer.layer.toString,
          "totalSlots" -> layer.totalSlots,
          "presentSlots" -> layer.presentSlots,
          "missingSlots" -> layer.missingSlots.map(_.slot.toString),
          "gapPercent" -> layer.gapPercent
        )
      )
    )

  private def inputDiagnostics(raw: RawMoveReviewInput): JsObject =
    val playedMove = MoveReviewInputNormalizer.normalizeUci(raw.playedMoveUci)
    val rootMoves = raw.variations.flatMap(_.moves.headOption).map(MoveReviewInputNormalizer.normalizeUci)
    Json.obj(
      "inputVariationCount" -> raw.variations.size,
      "variationRootMoves" -> rootMoves,
      "playedMoveInVariations" -> rootMoves.contains(playedMove),
      "missingPlayedLineEval" -> !rootMoves.contains(playedMove)
    )

  private def evidenceLossSummary(result: MoveReviewJudgmentResult): JsObject =
    val unexpected =
      result.quality.evidenceLoss.filter(_.expectation == EvidenceLossExpectation.Unexpected)
    Json.obj(
      "unexpectedCount" -> unexpected.size,
      "unexpectedByLayer" -> countsBy(
        unexpected.flatMap(_.diagnostic.layer.map(_.toString))
      ),
      "unexpectedByReason" -> countsBy(
        unexpected.map(_.diagnostic.reason.toString)
      ),
      "unexpectedDiagnostics" -> JsArray(
        unexpected.take(20).map(loss =>
          Json.obj(
            "stage" -> loss.diagnostic.stage.toString,
            "reason" -> loss.diagnostic.reason.toString,
            "subjectId" -> loss.diagnostic.subjectId,
            "layer" -> loss.diagnostic.layer.map(_.toString)
          )
        )
      )
    )

  private def countsBy(values: List[String]): JsObject =
    JsObject(
      values
        .groupMapReduce(identity)(_ => 1)(_ + _)
        .toList
        .sortBy(_._1)
        .map((key, count) => key -> JsNumber(count))
    )

  private def evidenceLayerCounts(result: MoveReviewJudgmentResult): JsObject =
    val counts =
      result.packet.evidenceGraph.records
        .groupMapReduce(_.ref.layer.toString)(_ => 1)(_ + _)
        .toList
        .sortBy(_._1)
    JsObject(counts.map((layer, count) => layer -> JsNumber(count)))

  private def relationKinds(result: MoveReviewJudgmentResult): JsArray =
    JsArray(
      result.packet.evidenceGraph.records.collect {
        case lila.chessjudgment.model.judgment.EvidenceRecord(
              _,
              payload: lila.chessjudgment.model.judgment.RelationFactEvidence,
              _
            ) =>
          JsString(payload.kind.toString)
      }
    )

  private def strategicKinds(result: MoveReviewJudgmentResult): JsArray =
    JsArray(
      result.packet.evidenceGraph.records.collect {
        case lila.chessjudgment.model.judgment.EvidenceRecord(
              _,
              payload: lila.chessjudgment.model.judgment.StrategicFactEvidence,
              _
            ) =>
          JsString(payload.kind.toString)
      }
    )

  private def topClaims(result: MoveReviewJudgmentResult): JsArray =
    JsArray(
      result.packet.claims
        .sortBy(claim => -claim.salience.map(_.score).getOrElse(0))
        .take(5)
        .map(claim =>
          Json.obj(
            "id" -> claim.id,
            "family" -> claim.family.toString,
            "subject" -> claim.subject.toString,
            "subjectMove" -> claim.subjectMove,
            "scope" -> claim.scope.toString,
            "confidence" -> claim.confidence.toString,
            "supportStatus" -> claim.supportStatus.map(_.status.toString),
            "salienceScore" -> claim.salience.map(_.score),
            "salienceDrivers" -> claim.salience.map(_.drivers.map(_.toString)),
            "engineVerdict" -> claim.engineComparison.map(_.verdict.toString),
            "engineWinPercentLossForMover" -> claim.engineComparison.map(_.winPercentLossForMover),
            "missingLayerGroups" -> claim.supportStatus.map(_.missingLayerGroups.map(_.map(_.toString).toList.sorted)),
            "evidenceLayers" -> claim.evidence.map(_.layer.toString)
          )
        )
    )

private object MoveReviewQualityInputFiles:

  def parseJsonDocuments(path: Path): List[JsValue] =
    val raw = Files.readString(path, StandardCharsets.UTF_8).stripPrefix("\uFEFF").trim
    if raw.startsWith("[") then Json.parse(raw).as[List[JsValue]]
    else raw.linesIterator.toList.filter(_.trim.nonEmpty).map(line => Json.parse(line))
