package lila.chessjudgment.analysis.qc

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import java.security.MessageDigest

import chess.format.pgn.PgnStr
import lila.core.game.Game
import lila.chessjudgment.analysis.assembly.{
  MoveReviewInputNormalizer,
  MoveReviewJudgmentOrchestrator,
  MoveReviewJudgmentResult,
  RawOpeningContext,
  RawMoveReviewInput
}
import lila.chessjudgment.model.ProbeResult
import lila.chessjudgment.model.judgment.*
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

final case class MoveReviewPhase3PlayedBindingSummary(
    playedBoundIdeaFamilies: List[String],
    playedBoundClaimFamilies: List[String],
    playedBoundFamilies: List[String]
)

object MoveReviewPhase3PlayedBindingSummary:
  def from(
      playedMoveUci: String,
      ideas: List[ChessIdea],
      claims: List[ClaimSeed]
  ): MoveReviewPhase3PlayedBindingSummary =
    val playedMove = normalizeMove(playedMoveUci)
    val ideaFamilies =
      ideas
        .filter(idea => bindsToPlayedMove(playedMove, idea.moveUci, idea.primaryLine))
        .map(_.ref.family.toString)
        .distinct
        .sorted
    val claimFamilies =
      claims
        .filter(claim => bindsToPlayedMove(playedMove, claim.subjectMove, claim.primaryLine))
        .map(_.family.toString)
        .distinct
        .sorted
    MoveReviewPhase3PlayedBindingSummary(
      playedBoundIdeaFamilies = ideaFamilies,
      playedBoundClaimFamilies = claimFamilies,
      playedBoundFamilies = (ideaFamilies ++ claimFamilies).distinct.sorted
    )

  private def bindsToPlayedMove(
      playedMove: String,
      subjectMove: Option[String],
      primaryLine: Option[LineNodeRef]
  ): Boolean =
    playedMove.nonEmpty &&
      (
        subjectMove.exists(move => normalizeMove(move) == playedMove) ||
          primaryLine.exists(line => line.role == LineNodeRole.Played && normalizeMove(line.rootMove) == playedMove)
      )

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

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
        movePrefixUci = (json \ "movePrefixUci").asOpt[List[String]].getOrElse(Nil),
        probeResults = (json \ "probeResults").asOpt[List[ProbeResult]].getOrElse(Nil)
      )
  }

  def main(args: Array[String]): Unit =
    if args.isEmpty then
      throw IllegalArgumentException("usage: MoveReviewPhase3AuditRunner <input.json|jsonl> [output.jsonl]")
    val inputPath = Path.of(args(0))
    val outputPath = args.lift(1).map(Path.of(_))
    val samples =
      MoveReviewQualityInputFiles.parseJsonDocuments(inputPath).zipWithIndex.map { case (json, index) =>
        parseSample(json, index)
      }
    val fingerprints = samples.map(sample => inputFingerprint(sample.raw))
    val duplicateCounts = fingerprints.groupBy(identity).view.mapValues(_.size).toMap
    var seen = Map.empty[String, Int]
    val rows =
      samples.zip(fingerprints).map { case (sample, fingerprint) =>
        val ordinal = seen.getOrElse(fingerprint, 0) + 1
        seen = seen.updated(fingerprint, ordinal)
        auditSample(
          sample = sample,
          inputFingerprint = fingerprint,
          inputDuplicateOrdinal = ordinal,
          inputDuplicateCount = duplicateCounts.getOrElse(fingerprint, 1)
        )
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
      variations <- legacyVariationLines(rawResponse)
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

  private def legacyVariationLines(rawResponse: JsValue): Option[List[VariationLine]] =
    val topLevel = (rawResponse \ "variations").asOpt[List[VariationLine]].getOrElse(Nil)
    val refs =
      (rawResponse \ "refs" \ "variations")
        .asOpt[List[JsValue]]
        .getOrElse(Nil)
        .flatMap(refVariationLine)
    Option.when((topLevel ++ refs).nonEmpty)(topLevel ++ refs)

  private def refVariationLine(json: JsValue): Option[VariationLine] =
    val moves =
      (json \ "moves")
        .asOpt[List[JsValue]]
        .getOrElse(Nil)
        .flatMap(move => (move \ "uci").asOpt[String])
    Option.when(moves.nonEmpty)(
      VariationLine(
        moves = moves,
        scoreCp = (json \ "scoreCp").asOpt[Int].getOrElse(0),
        mate = (json \ "mate").asOpt[Int],
        depth = (json \ "depth").asOpt[Int].getOrElse(0)
      )
    )

  private def auditSample(
      sample: AuditInputSample,
      inputFingerprint: String,
      inputDuplicateOrdinal: Int,
      inputDuplicateCount: Int
  ): JsObject =
    val result = MoveReviewJudgmentOrchestrator.build(sample.raw)
    val base =
      Json.obj(
        "schemaVersion" -> "move_review_phase3_audit_row.v2",
        "sampleId" -> sample.sampleId,
        "opening" -> sample.opening,
        "sliceKind" -> sample.sliceKind,
        "targetPly" -> sample.targetPly,
        "fen" -> sample.raw.fen,
        "playedSan" -> sample.playedSan,
        "playedUci" -> sample.raw.playedMoveUci,
        "sourcePath" -> sample.sourcePath,
        "inputFingerprint" -> inputFingerprint,
        "inputDuplicateOrdinal" -> inputDuplicateOrdinal,
        "inputDuplicateCount" -> inputDuplicateCount,
        "inputDuplicate" -> (inputDuplicateCount > 1),
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
          "probeRequests" -> probeRequestSummary(built),
          "probeDiagnostics" -> probeDiagnosticsSummary(built),
          "claimSupportClusters" -> claimSupportClusters(built),
          "claimEventClusters" -> claimEventClusters(built),
          "topClaims" -> topClaims(built),
          "topPrimaryPlayedClaims" -> topPrimaryPlayedClaims(built),
          "topContextClaims" -> topContextClaims(built)
        )
      case None =>
        base ++ Json.obj(
          "buildStatus" -> "failed",
          "lineSummary" -> Json.obj(
            "inputVariationCount" -> sample.raw.variations.size,
            "inputRootMoves" -> sample.raw.variations.flatMap(_.moves.headOption)
          )
        )

  private def inputFingerprint(raw: RawMoveReviewInput): String =
    val key =
      List(
        raw.fen.trim,
        raw.playedMoveUci.trim,
        raw.ply.map(_.toString).getOrElse(""),
        raw.currentEvalCp.map(_.toString).getOrElse(""),
        raw.variations
          .map(line =>
            List(
              line.scoreCp.toString,
              line.mate.map(_.toString).getOrElse(""),
              line.depth.toString,
              line.moves.mkString(",")
            ).mkString(":")
          )
          .mkString("|"),
        raw.probeResults
          .map(probe =>
            List(
              probe.id,
              probe.purpose.map(_.key).getOrElse(""),
              probe.fen.getOrElse(""),
              probe.probedMove.orElse(probe.candidateMove).getOrElse(""),
              probe.replyLines
                .getOrElse(Nil)
                .map(line =>
                  List(
                    line.scoreCp.toString,
                    line.mate.map(_.toString).getOrElse(""),
                    line.depth.toString,
                    line.moves.mkString(",")
                  ).mkString(":")
                )
                .mkString("|")
            ).mkString(":")
          )
          .mkString("||")
      ).mkString("||")
    val digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8))
    digest.map(byte => f"${byte & 0xff}%02x").mkString

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

  private def probeRequestSummary(result: MoveReviewJudgmentResult): JsObject =
    val requests = result.packet.probeRequests
    Json.obj(
      "count" -> requests.size,
      "purposes" -> requests.flatMap(_.purpose.map(_.key)).distinct.sorted,
      "candidateMoves" -> requests.flatMap(_.candidateMove).distinct.sorted,
      "multiPv" -> requests.flatMap(_.multiPv).distinct.sorted,
      "depthFloors" -> requests.flatMap(_.depthFloor).distinct.sorted,
      "requiredSignals" -> requests.flatMap(_.requiredSignals).distinct.sorted,
      "requests" -> requests.take(5).map(request =>
        Json.obj(
          "id" -> request.id,
          "purpose" -> request.purpose.map(_.key),
          "candidateMove" -> request.candidateMove,
          "fen" -> request.fen,
          "multiPv" -> request.multiPv,
          "depth" -> request.depth,
          "depthFloor" -> request.depthFloor,
          "requiredSignals" -> request.requiredSignals,
          "variationHash" -> request.variationHash
        )
      )
    )

  private def probeDiagnosticsSummary(result: MoveReviewJudgmentResult): JsObject =
    val diagnostics = result.packet.probeDiagnostics
    Json.obj(
      "count" -> diagnostics.size,
      "statuses" -> diagnostics.groupMapReduce(_.status.toString)(_ => 1)(_ + _),
      "reasonCodes" -> diagnostics.flatMap(_.reasonCodes).groupMapReduce(identity)(_ => 1)(_ + _),
      "diagnostics" -> diagnostics.take(10).map(diagnostic =>
        Json.obj(
          "probeId" -> diagnostic.probeId,
          "status" -> diagnostic.status.toString,
          "reasonCodes" -> diagnostic.reasonCodes,
          "purpose" -> diagnostic.purpose.map(_.key),
          "candidateMove" -> diagnostic.candidateMove,
          "fen" -> diagnostic.fen,
          "admittedLineCount" -> diagnostic.admittedLineCount,
          "legalLineCount" -> diagnostic.legalLineCount,
          "scoredLineCount" -> diagnostic.scoredLineCount,
          "depthFloor" -> diagnostic.depthFloor,
          "variationHash" -> diagnostic.variationHash
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
    val playedBinding =
      MoveReviewPhase3PlayedBindingSummary.from(
        playedMoveUci = result.packet.playedTransition.map(_.moveUci).getOrElse(""),
        ideas = result.packet.ideas,
        claims = result.packet.claims
      )
    Json.obj(
      "tacticalIdeas" -> semantic.tacticalIdeas,
      "strategicIdeas" -> semantic.strategicIdeas,
      "pawnStructureIdeas" -> semantic.pawnStructureIdeas,
      "openingIdeas" -> semantic.openingIdeas,
      "defensiveIdeas" -> semantic.defensiveIdeas,
      "evaluationIdeas" -> semantic.evaluationIdeas,
      "conversionIdeas" -> semantic.conversionIdeas,
      "materialIdeas" -> semantic.materialIdeas,
      "claimFamilies" -> semantic.claimFamilies.map(_.toString).toList.sorted,
      "claimSupportClusters" -> semantic.claimSupportClusters,
      "clusteredAnchorClaims" -> semantic.clusteredAnchorClaims,
      "clusteredSupportingClaims" -> semantic.clusteredSupportingClaims,
      "clusteredConstrainingClaims" -> semantic.clusteredConstrainingClaims,
      "clusteredLongTermSupportIdeas" -> semantic.clusteredLongTermSupportIdeas,
      "claimSupportClusterFamilies" -> semantic.claimSupportClusterFamilies.map(_.toString).toList.sorted,
      "claimSupportClusterLayers" -> semantic.claimSupportClusterLayers.map(_.toString).toList.sorted,
      "claimEventClusters" -> semantic.claimEventClusters,
      "clusteredEventClaims" -> semantic.clusteredEventClaims,
      "clusteredEventCauseClaims" -> semantic.clusteredEventCauseClaims,
      "clusteredEventEvaluationClaims" -> semantic.clusteredEventEvaluationClaims,
      "clusteredEventWitnessClaims" -> semantic.clusteredEventWitnessClaims,
      "clusteredEventIdeas" -> semantic.clusteredEventIdeas,
      "relatedEventSupportClusters" -> semantic.relatedEventSupportClusters,
      "claimEventClusterFamilies" -> semantic.claimEventClusterFamilies.map(_.toString).toList.sorted,
      "claimEventClusterLayers" -> semantic.claimEventClusterLayers.map(_.toString).toList.sorted,
      "claimEventClusterKinds" -> semantic.claimEventClusterKinds.map(_.toString).toList.sorted,
      "claimEventClusterCauses" -> semantic.claimEventClusterCauses.map(_.toString).toList.sorted,
      "claimEventInteractionKinds" -> semantic.claimEventInteractionKinds.map(_.toString).toList.sorted,
      "unclusteredEventClaims" -> semantic.unclusteredEventClaims,
      "unclusteredEventClaimFamilies" -> semantic.unclusteredEventClaimFamilies.map(_.toString).toList.sorted,
      "verdictCarriersWithoutEventCauseOwner" -> semantic.verdictCarriersWithoutEventCauseOwner,
      "verdictOnlyEvaluationClaims" -> semantic.verdictOnlyEvaluationClaims,
      "localConcreteClaims" -> semantic.localConcreteClaims,
      "localConcreteClaimFamilies" -> semantic.localConcreteClaimFamilies.map(_.toString).toList.sorted,
      "localConcreteClaimDetails" -> localConcreteClaimDetails(semantic.localConcreteClaimDiagnostics),
      "playedBoundIdeaFamilies" -> playedBinding.playedBoundIdeaFamilies,
      "playedBoundClaimFamilies" -> playedBinding.playedBoundClaimFamilies,
      "playedBoundFamilies" -> playedBinding.playedBoundFamilies,
      "hasRelativeAssessment" -> semantic.hasRelativeAssessment,
      "candidateComparisonFacts" -> semantic.candidateComparisonFacts,
      "relativeCauseFacts" -> semantic.relativeCauseFacts,
      "moveVerdictCertifications" -> semantic.moveVerdictCertifications,
      "playedCandidateComparisonFacts" -> semantic.playedCandidateComparisonFacts,
      "playedRelativeCauseFacts" -> semantic.playedRelativeCauseFacts,
      "branchReplyProbeRequests" -> semantic.branchReplyProbeRequests,
      "branchReplyProbeMoves" -> semantic.branchReplyProbeMoves,
      "branchReplyThreatLines" -> semantic.branchReplyThreatLines,
      "branchReplyThreatPressureRecords" -> semantic.branchReplyThreatPressureRecords,
      "branchReplyProbeAdmittedResults" -> semantic.branchReplyProbeAdmittedResults,
      "branchReplyProbeRejectedResults" -> semantic.branchReplyProbeRejectedResults,
      "branchReplyProbeIgnoredResults" -> semantic.branchReplyProbeIgnoredResults,
      "branchReplyProbeRejectReasons" -> semantic.branchReplyProbeRejectReasons,
      "branchReplyProbeAdmittedMoves" -> semantic.branchReplyProbeAdmittedMoves,
      "branchReplyProbeRejectedMoves" -> semantic.branchReplyProbeRejectedMoves,
      "hasPendingBranchReplyDepth" -> semantic.hasPendingBranchReplyDepth,
      "hasUnexplainedEngineGap" -> semantic.hasUnexplainedEngineGap,
      "hasPlayedUnexplainedEngineGap" -> semantic.hasPlayedUnexplainedEngineGap,
      "hasContextUnexplainedEngineGap" -> semantic.hasContextUnexplainedEngineGap,
      "hasSecondaryContextEngineGap" -> semantic.hasSecondaryContextEngineGap,
      "secondaryContextWithPrimaryCoverageCount" -> semantic.secondaryContextWithPrimaryCoverageIds.size,
      "secondaryContextWithoutPrimaryCoverageCount" -> semantic.secondaryContextWithoutPrimaryCoverageIds.size,
      "secondaryContextComparisonIds" -> semantic.secondaryContextComparisonIds,
      "secondaryContextWithPrimaryCoverageIds" -> semantic.secondaryContextWithPrimaryCoverageIds,
      "secondaryContextWithoutPrimaryCoverageIds" -> semantic.secondaryContextWithoutPrimaryCoverageIds,
      "contextUnexplainedWithPrimaryCoverageCount" -> semantic.contextUnexplainedWithPrimaryCoverageIds.size,
      "contextUnexplainedWithoutPrimaryCoverageCount" -> semantic.contextUnexplainedWithoutPrimaryCoverageIds.size,
      "contextUnexplainedComparisonIds" -> semantic.contextUnexplainedComparisonIds,
      "contextUnexplainedWithPrimaryCoverageIds" -> semantic.contextUnexplainedWithPrimaryCoverageIds,
      "contextUnexplainedWithoutPrimaryCoverageIds" -> semantic.contextUnexplainedWithoutPrimaryCoverageIds,
      "openingApplicabilityDiagnostics" -> openingApplicabilityDiagnosticsSummary(semantic.openingApplicabilityDiagnostics),
      "comparisonDiagnostics" -> comparisonDiagnosticsSummary(semantic.comparisonDiagnostics),
      "hasVerdict" -> semantic.hasVerdict,
      "hasCandidateSetComparison" -> semantic.hasCandidateSetComparison,
      "hasOnlyMoveSignal" -> semantic.hasOnlyMoveSignal,
      "hasForcedLineTheme" -> semantic.hasForcedLineTheme
    )

  private def localConcreteClaimDetails(diagnostics: List[LocalConcreteClaimDiagnostic]): JsArray =
    JsArray(
      diagnostics.map(diagnostic =>
        Json.obj(
          "id" -> diagnostic.id,
          "family" -> diagnostic.family.toString,
          "subject" -> diagnostic.subject.toString,
          "primaryLine" -> diagnostic.primaryLine.map(lineRefSummary),
          "subjectMove" -> diagnostic.subjectMove,
          "scope" -> diagnostic.scope.toString,
          "localKind" -> diagnostic.localKind.toString,
          "evidenceLayers" -> diagnostic.evidenceLayers.map(_.toString).toList.sorted,
          "evidenceIds" -> diagnostic.evidenceIds,
          "engineVerdict" -> diagnostic.engineVerdict.map(_.toString),
          "engineWinPercentLossForMover" -> diagnostic.engineWinPercentLossForMover,
          "threatSeverity" -> diagnostic.threatSeverity.map(_.toString),
          "threatCount" -> diagnostic.threatCount,
          "threatDefenseRequired" -> diagnostic.threatDefenseRequired,
          "threatProphylaxisNeeded" -> diagnostic.threatProphylaxisNeeded,
          "threatOnlyDefense" -> diagnostic.threatOnlyDefense,
          "threatMaxWinPercentLossIfIgnored" -> diagnostic.threatMaxWinPercentLossIfIgnored,
          "threatPrimaryDriver" -> diagnostic.threatPrimaryDriver.map(_.toString),
          "threatInsufficientData" -> diagnostic.threatInsufficientData,
          "nearbyEventClusterIds" -> diagnostic.nearbyEventClusterIds
        )
      )
    )

  private def comparisonDiagnosticsSummary(diagnostics: List[CandidateComparisonDiagnostic]): JsArray =
    JsArray(diagnostics.map(comparisonDiagnosticJson))

  private def openingApplicabilityDiagnosticsSummary(
      diagnostics: List[OpeningApplicabilityDiagnostic]
  ): JsArray =
    JsArray(
      diagnostics.map(diagnostic =>
        Json.obj(
          "id" -> diagnostic.id,
          "applicability" -> diagnostic.applicability.toString,
          "status" -> diagnostic.status.toString,
          "observedThemes" -> diagnostic.observedThemes.map(_.toString),
          "supportedThemes" -> diagnostic.supportedThemes.map(_.toString),
          "unverifiedPriorThemes" -> diagnostic.unverifiedPriorThemes.map(_.toString),
          "observedOnlyThemes" -> diagnostic.observedOnlyThemes.map(_.toString),
          "anchorSourceLayers" -> diagnostic.anchorSourceLayers.map(_.toString),
          "anchorSignals" -> diagnostic.anchorSignals.map(_.toString),
          "supportedAnchorSourceLayers" -> diagnostic.supportedAnchorSourceLayers.map(_.toString),
          "supportedAnchorSignals" -> diagnostic.supportedAnchorSignals.map(_.toString),
          "priorAligned" -> diagnostic.priorAligned
        )
      )
    )

  private def comparisonDiagnosticJson(diagnostic: CandidateComparisonDiagnostic): JsObject =
    Json.obj(
      "id" -> diagnostic.id,
      "comparisonFingerprint" -> diagnostic.comparisonFingerprint,
      "dedupeKey" -> diagnostic.dedupeKey,
      "dedupeClass" -> dedupeClassId(diagnostic.dedupeClass),
      "comparisonKind" -> diagnostic.comparisonKind.toString,
      "referenceLine" -> lineDiagnosticJson(diagnostic.referenceLine),
      "candidateLine" -> lineDiagnosticJson(diagnostic.candidateLine),
      "verdict" -> diagnostic.verdict.toString,
      "mover" -> diagnostic.mover,
      "cpLossForMover" -> diagnostic.cpLossForMover,
      "candidateDeltaForMover" -> diagnostic.candidateDeltaForMover,
      "winPercentLossForMover" -> diagnostic.winPercentLossForMover,
      "candidateWinPercentDeltaForMover" -> diagnostic.candidateWinPercentDeltaForMover,
      "causeKinds" -> diagnostic.causeKinds.map(_.toString),
      "causeSupport" -> JsArray(
        diagnostic.causeSupport.map(support =>
          Json.obj(
            "id" -> support.id,
            "kind" -> support.kind.toString,
            "parentEvidenceIds" -> support.parentEvidenceIds,
            "parentLayers" -> support.parentLayers.map(_.toString),
            "parentLayerSignature" -> support.parentLayerSignature,
            "semanticSupportKinds" -> support.semanticSupportKinds,
            "semanticSupportSignature" -> support.semanticSupportSignature
          )
        )
      ),
      "failedCauseCandidates" -> diagnostic.failedCauseCandidates.map(_.toString),
      "significanceReasons" -> diagnostic.significanceReasons.map(significanceReasonId),
      "lowSignalReasons" -> diagnostic.lowSignalReasons.map(lowSignalReasonId),
      "comparisonConfidence" -> diagnostic.comparisonConfidence.toString,
      "causeConfidences" -> diagnostic.causeConfidences.map(_.toString),
      "hasLowDepthCause" -> diagnostic.hasLowDepthCause,
      "hasUnexplainedEngineGap" -> diagnostic.hasUnexplainedEngineGap,
      "hasSecondaryContextEngineGap" -> diagnostic.hasSecondaryContextEngineGap,
      "evidenceLayers" -> Json.obj(
        "reference" -> evidenceLayerNeighborhoodJson(diagnostic.evidenceLayers.reference),
        "candidate" -> evidenceLayerNeighborhoodJson(diagnostic.evidenceLayers.candidate)
      ),
      "decisionTrace" -> causeDecisionTraceJson(diagnostic.decisionTrace),
      "failureClass" -> failureClassId(diagnostic.failureClass),
      "failureReasons" -> diagnostic.failureReasons.map(failureReasonId)
    )

  private def causeDecisionTraceJson(trace: CandidateCauseDecisionTrace): JsObject =
    Json.obj(
      "badLoss" -> trace.badLoss,
      "tacticalLoss" -> trace.tacticalLoss,
      "majorLoss" -> trace.majorLoss,
      "candidateBetter" -> trace.candidateBetter,
      "requiresExplanatoryCause" -> trace.requiresExplanatoryCause,
      "positiveContextAlternative" -> trace.positiveContextAlternative,
      "referenceForcing" -> trace.referenceForcing,
      "candidateForcing" -> trace.candidateForcing,
      "referenceCauseEligibleTactical" -> trace.referenceCauseEligibleTactical,
      "candidateCauseEligibleTactical" -> trace.candidateCauseEligibleTactical,
      "referenceConcreteLine" -> trace.referenceConcreteLine,
      "candidateConcreteLine" -> trace.candidateConcreteLine,
      "candidateTacticalRefutationBridge" -> trace.candidateTacticalRefutationBridge,
      "referenceTacticalRisk" -> trace.referenceTacticalRisk,
      "hasOnlyDefense" -> trace.hasOnlyDefense,
      "hasThreatResource" -> trace.hasThreatResource,
      "referenceProphylacticResource" -> trace.referenceProphylacticResource,
      "referenceKingStepResource" -> trace.referenceKingStepResource,
      "candidateKingStepResource" -> trace.candidateKingStepResource,
      "referenceCastlingResource" -> trace.referenceCastlingResource,
      "candidateCastlingResource" -> trace.candidateCastlingResource,
      "referencePreventivePawnResource" -> trace.referencePreventivePawnResource,
      "candidatePreventivePawnResource" -> trace.candidatePreventivePawnResource,
      "hasConversionWindow" -> trace.hasConversionWindow,
      "referenceConversionWindow" -> trace.referenceConversionWindow,
      "candidateConversionWindow" -> trace.candidateConversionWindow,
      "referenceStructuralTargetRelease" -> trace.referenceStructuralTargetRelease,
      "referenceReleasedTargets" -> trace.referenceReleasedTargets,
      "candidateReleasedTargets" -> trace.candidateReleasedTargets,
      "referenceCreatedTargets" -> trace.referenceCreatedTargets,
      "candidateCreatedTargets" -> trace.candidateCreatedTargets,
      "referenceStructuralImprovement" -> trace.referenceStructuralImprovement,
      "candidateStructuralImprovement" -> trace.candidateStructuralImprovement,
      "candidatePawnStructureImprovement" -> trace.candidatePawnStructureImprovement,
      "candidateTargetPressureGain" -> trace.candidateTargetPressureGain,
      "candidateCenterControlGain" -> trace.candidateCenterControlGain,
      "candidateDevelopmentActivation" -> trace.candidateDevelopmentActivation,
      "candidatePieceActivityGain" -> trace.candidatePieceActivityGain,
      "sameDestinationCaptureChoice" -> trace.sameDestinationCaptureChoice,
      "referenceStructuralSignals" -> trace.referenceStructuralSignals,
      "candidateStructuralSignals" -> trace.candidateStructuralSignals,
      "candidatePawnStructureSignals" -> trace.candidatePawnStructureSignals,
      "referenceStrategicImprovementScore" -> trace.referenceStrategicImprovementScore,
      "candidateStrategicImprovementScore" -> trace.candidateStrategicImprovementScore,
      "referenceStrategicImprovementOverCandidate" -> trace.referenceStrategicImprovementOverCandidate,
      "candidatePlanEvidence" -> trace.candidatePlanEvidence,
      "candidateStrategicEvidence" -> trace.candidateStrategicEvidence,
      "candidateStrategicConcessionEvidence" -> trace.candidateStrategicConcessionEvidence,
      "candidateStrongStrategicConcessionEvidence" -> trace.candidateStrongStrategicConcessionEvidence,
      "candidateKingHomeStepConcession" -> trace.candidateKingHomeStepConcession,
      "materialSwingEvidence" -> trace.materialSwingEvidence,
      "referenceMaterialNetCp" -> trace.referenceMaterialNetCp,
      "candidateMaterialNetCp" -> trace.candidateMaterialNetCp,
      "referenceMaterialMaxGainCp" -> trace.referenceMaterialMaxGainCp,
      "candidateMaterialMaxGainCp" -> trace.candidateMaterialMaxGainCp,
      "referenceMaterialPromotionGainCp" -> trace.referenceMaterialPromotionGainCp,
      "candidateMaterialPromotionGainCp" -> trace.candidateMaterialPromotionGainCp,
      "referenceMaterialRecapture" -> trace.referenceMaterialRecapture,
      "candidateMaterialRecapture" -> trace.candidateMaterialRecapture,
      "referenceMaterialRecovery" -> trace.referenceMaterialRecovery,
      "candidateMaterialRecovery" -> trace.candidateMaterialRecovery,
      "referenceMaterialComplete" -> trace.referenceMaterialComplete,
      "candidateMaterialComplete" -> trace.candidateMaterialComplete,
      "referenceRelationKinds" -> trace.referenceRelationKinds.map(_.toString),
      "candidateRelationKinds" -> trace.candidateRelationKinds.map(_.toString),
      "relationKinds" -> trace.relationKinds.map(_.toString),
      "referenceMotifs" -> trace.referenceMotifs,
      "candidateMotifs" -> trace.candidateMotifs
    )

  private def lineDiagnosticJson(line: CandidateLineDiagnostic): JsObject =
    Json.obj(
      "id" -> line.id,
      "rootMove" -> line.rootMove,
      "role" -> line.role.toString,
      "rank" -> line.rank,
      "moves" -> line.moves,
      "evalCp" -> line.evalCp,
      "mate" -> line.mate,
      "depth" -> line.depth
    )

  private def evidenceLayerNeighborhoodJson(neighborhood: EvidenceLayerNeighborhood): JsObject =
    Json.obj(
      "directLayers" -> neighborhood.directLayers.map(_.toString).toList.sorted,
      "directLayerCounts" -> layerCountsJson(neighborhood.directLayerCounts),
      "parentLayerCounts" -> layerCountsJson(neighborhood.parentLayerCounts)
    )

  private def layerCountsJson(counts: Map[EvidenceLayer, Int]): JsObject =
    JsObject(
      counts.toList
        .sortBy(_._1.toString)
        .map((layer, count) => layer.toString -> JsNumber(count))
    )

  private def failureClassId(failureClass: CandidateComparisonFailureClass): String =
    failureClass match
      case CandidateComparisonFailureClass.NoFailure                 => "none"
      case CandidateComparisonFailureClass.LowSignalEnginePreference => "low_signal_engine_preference"
      case CandidateComparisonFailureClass.SecondaryContextGap       => "secondary_context_gap"
      case CandidateComparisonFailureClass.IncompleteCauseCoverage   => "incomplete_cause_coverage"
      case CandidateComparisonFailureClass.MissingEvidence           => "missing_evidence"
      case CandidateComparisonFailureClass.UnboundEvidence           => "unbound_evidence"
      case CandidateComparisonFailureClass.FailedCauseTemplate       => "failed_cause_template"
      case CandidateComparisonFailureClass.UnknownChessPattern       => "unknown_chess_pattern"

  private def failureReasonId(reason: CandidateComparisonFailureReason): String =
    reason match
      case CandidateComparisonFailureReason.LowSignalEnginePreference => "low_signal_engine_preference"
      case CandidateComparisonFailureReason.MissingLineEvidence       => "missing_line_evidence"
      case CandidateComparisonFailureReason.MissingEvalEvidence       => "missing_eval_evidence"
      case CandidateComparisonFailureReason.NoCauseGenerated          => "no_cause_generated"
      case CandidateComparisonFailureReason.GeneratedOnlyUnexplained  => "generated_only_unexplained"
      case CandidateComparisonFailureReason.TacticalEvidenceUnbound   => "tactical_evidence_unbound"
      case CandidateComparisonFailureReason.DefensiveEvidenceUnbound  => "defensive_evidence_unbound"
      case CandidateComparisonFailureReason.ConversionEvidenceUnbound => "conversion_evidence_unbound"
      case CandidateComparisonFailureReason.StrategicEvidenceUnbound  => "strategic_evidence_unbound"
      case CandidateComparisonFailureReason.LatentTacticalEvidence    => "latent_tactical_evidence"
      case CandidateComparisonFailureReason.LatentMaterialEvidence    => "latent_material_evidence"
      case CandidateComparisonFailureReason.LatentStrategicEvidence   => "latent_strategic_evidence"
      case CandidateComparisonFailureReason.TacticalEvidenceBelowThreshold => "tactical_evidence_below_threshold"
      case CandidateComparisonFailureReason.MaterialEvidenceBelowThreshold => "material_evidence_below_threshold"
      case CandidateComparisonFailureReason.StrategicEvidenceBelowThreshold => "strategic_evidence_below_threshold"
      case CandidateComparisonFailureReason.TacticalSignalNeedsWidthDepth => "tactical_signal_needs_width_depth"
      case CandidateComparisonFailureReason.StrategicContextBelowSignalFloor => "strategic_context_below_signal_floor"
      case CandidateComparisonFailureReason.StrategicEvidenceBelowCauseThreshold =>
        "strategic_evidence_below_cause_threshold"
      case CandidateComparisonFailureReason.PrimaryStrategicNearThresholdUnderbinding =>
        "primary_strategic_near_threshold_underbinding"
      case CandidateComparisonFailureReason.PrimaryStrategicSignalNeedsWidthDepth =>
        "primary_strategic_signal_needs_width_depth"
      case CandidateComparisonFailureReason.ContextAlternativeStrategicNearThreshold =>
        "context_alternative_strategic_near_threshold"
      case CandidateComparisonFailureReason.LowSignalTacticalContext  => "low_signal_tactical_context"
      case CandidateComparisonFailureReason.LowSignalMaterialContext  => "low_signal_material_context"
      case CandidateComparisonFailureReason.LowSignalStrategicContext => "low_signal_strategic_context"
      case CandidateComparisonFailureReason.LowDepthGeneratedCause    => "low_depth_generated_cause"
      case CandidateComparisonFailureReason.ContextAlternativeComparison => "context_alternative_comparison"
      case CandidateComparisonFailureReason.MissingExpectedCause      => "missing_expected_cause"
      case CandidateComparisonFailureReason.NoSpecificEvidence        => "no_specific_evidence"
      case CandidateComparisonFailureReason.UnknownPattern            => "unknown_pattern"

  private def significanceReasonId(reason: CandidateComparisonSignificanceReason): String =
    reason match
      case CandidateComparisonSignificanceReason.PlayedLoss        => "played_loss"
      case CandidateComparisonSignificanceReason.CandidateImproves => "candidate_improves"
      case CandidateComparisonSignificanceReason.OnlyMove          => "only_move"

  private def lowSignalReasonId(reason: CandidateComparisonLowSignalReason): String =
    reason match
      case CandidateComparisonLowSignalReason.QuietNoDirection              => "quiet_no_direction"
      case CandidateComparisonLowSignalReason.BelowSignalFloor              => "below_signal_floor"
      case CandidateComparisonLowSignalReason.BelowBindingThreshold         => "below_binding_threshold"
      case CandidateComparisonLowSignalReason.NearThresholdPrimaryPlayed    => "near_threshold_primary_played"
      case CandidateComparisonLowSignalReason.NearThresholdContextAlternative =>
        "near_threshold_context_alternative"
      case CandidateComparisonLowSignalReason.ContextAlternative            => "context_alternative"

  private def dedupeClassId(dedupeClass: CandidateComparisonDedupeClass): String =
    dedupeClass match
      case CandidateComparisonDedupeClass.Unique           => "unique"
      case CandidateComparisonDedupeClass.DuplicateEpisode => "duplicate_episode"

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
    val secondary =
      result.quality.evidenceLoss.filter(_.expectation == EvidenceLossExpectation.Secondary)
    val deferred =
      result.quality.evidenceLoss.filter(_.expectation == EvidenceLossExpectation.Deferred)
    val expected =
      result.quality.evidenceLoss.filter(_.expectation == EvidenceLossExpectation.Expected)
    Json.obj(
      "expectedCount" -> expected.size,
      "secondaryCount" -> secondary.size,
      "deferredCount" -> deferred.size,
      "unexpectedCount" -> unexpected.size,
      "expectedByLayer" -> countsBy(
        expected.flatMap(_.diagnostic.layer.map(_.toString))
      ),
      "expectedByReason" -> countsBy(
        expected.map(_.diagnostic.reason.toString)
      ),
      "deferredByLayer" -> countsBy(
        deferred.flatMap(_.diagnostic.layer.map(_.toString))
      ),
      "deferredByReason" -> countsBy(
        deferred.map(_.diagnostic.reason.toString)
      ),
      "secondaryByLayer" -> countsBy(
        secondary.flatMap(_.diagnostic.layer.map(_.toString))
      ),
      "secondaryByReason" -> countsBy(
        secondary.map(_.diagnostic.reason.toString)
      ),
      "unexpectedByLayer" -> countsBy(
        unexpected.flatMap(_.diagnostic.layer.map(_.toString))
      ),
      "unexpectedByReason" -> countsBy(
        unexpected.map(_.diagnostic.reason.toString)
      ),
      "unexpectedDiagnostics" -> JsArray(
        unexpected.take(20).map(loss => evidenceLossDiagnosticJson(result, loss))
      ),
      "deferredDiagnostics" -> JsArray(
        deferred.take(20).map(loss => evidenceLossDiagnosticJson(result, loss))
      ),
      "secondaryDiagnostics" -> JsArray(
        secondary.take(20).map(loss => evidenceLossDiagnosticJson(result, loss))
      )
    )

  private def evidenceLossDiagnosticJson(
      result: MoveReviewJudgmentResult,
      loss: EvidenceLossClassification
  ): JsObject =
    val diagnostic = loss.diagnostic
    Json.obj(
      "expectation" -> loss.expectation.toString,
      "stage" -> diagnostic.stage.toString,
      "reason" -> diagnostic.reason.toString,
      "subjectId" -> diagnostic.subjectId,
      "layer" -> diagnostic.layer.map(_.toString),
      "evidence" -> diagnostic.evidence.map(evidenceRefSummary),
      "evidencePayload" -> evidencePayloadSummary(result, diagnostic.subjectId),
      "idea" -> ideaSummary(result, diagnostic.subjectId),
      "claim" -> claimSummary(result, diagnostic.subjectId)
    )

  private def evidenceRefSummary(ref: EvidenceRef): JsObject =
    Json.obj(
      "id" -> ref.id,
      "producer" -> ref.producer.toString,
      "layer" -> ref.layer.toString,
      "scope" -> ref.scope.toString,
      "confidence" -> ref.confidence.toString,
      "line" -> ref.line.map(lineRefSummary)
    )

  private def lineRefSummary(ref: LineNodeRef): JsObject =
    Json.obj(
      "id" -> ref.id,
      "rootMove" -> ref.rootMove,
      "rank" -> ref.rank,
      "role" -> ref.role.toString
    )

  private def evidencePayloadSummary(result: MoveReviewJudgmentResult, subjectId: String): Option[JsObject] =
    result.packet.evidenceGraph.byId.get(subjectId).map {
      case EvidenceRecord(_, CandidateComparisonEvidence(fact), _) =>
        Json.obj(
          "payload" -> "CandidateComparison",
          "kind" -> fact.kind.toString,
          "referenceLine" -> lineRefSummary(fact.referenceLine),
          "candidateLine" -> lineRefSummary(fact.candidateLine),
          "verdict" -> fact.comparison.verdict.toString,
          "winPercentLossForMover" -> fact.comparison.winPercentLossForMover,
          "candidateWinPercentDeltaForMover" -> fact.comparison.candidateWinPercentDeltaForMover
        )
      case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
        Json.obj(
          "payload" -> "RelativeCause",
          "kind" -> cause.kind.toString,
          "comparisonKind" -> cause.comparisonKind.toString,
          "referenceLine" -> lineRefSummary(cause.referenceLine),
          "candidateLine" -> lineRefSummary(cause.candidateLine),
          "verdict" -> cause.verdict.toString,
          "winPercentLossForMover" -> cause.winPercentLossForMover
        )
      case EvidenceRecord(_, RelationFactEvidence(kind, _, targetSquare, lineMoves, participants), _) =>
        Json.obj(
          "payload" -> "Relation",
          "kind" -> kind.toString,
          "targetSquare" -> targetSquare.map(_.key),
          "lineMoves" -> lineMoves,
          "participants" -> participants.map(_.toString)
        )
      case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        Json.obj(
          "payload" -> "MoveMotif",
          "moveUci" -> moveUci,
          "motifs" -> motifs.map(_.getClass.getSimpleName.stripSuffix("$"))
        )
      case EvidenceRecord(_, ChessIdeaEvidence(idea), _) =>
        Json.obj(
          "payload" -> "ChessIdea",
          "id" -> idea.id,
          "family" -> idea.family.toString
        )
      case EvidenceRecord(ref, payload, _) =>
        Json.obj(
          "payload" -> payload.layer.toString,
          "evidenceId" -> ref.id
        )
    }

  private def ideaSummary(result: MoveReviewJudgmentResult, subjectId: String): Option[JsObject] =
    result.packet.ideas.find(_.ref.id == subjectId).map { idea =>
      Json.obj(
        "id" -> idea.ref.id,
        "family" -> idea.ref.family.toString,
        "subject" -> idea.subject.toString,
        "moveUci" -> idea.moveUci,
        "primaryLine" -> idea.primaryLine.map(lineRefSummary),
        "scope" -> idea.scope.toString,
        "confidence" -> idea.confidence.toString,
        "evidenceLayers" -> idea.evidence.map(_.layer.toString).distinct
      )
    }

  private def claimSummary(result: MoveReviewJudgmentResult, subjectId: String): Option[JsObject] =
    result.packet.claims.find(_.id == subjectId).map { claim =>
      Json.obj(
        "id" -> claim.id,
        "family" -> claim.family.toString,
        "subject" -> claim.subject.toString,
        "subjectMove" -> claim.subjectMove,
        "primaryLine" -> claim.primaryLine.map(lineRefSummary),
        "scope" -> claim.scope.toString,
        "confidence" -> claim.confidence.toString,
        "evidenceLayers" -> claim.evidence.map(_.layer.toString).distinct
      )
    }

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

  private def claimSupportClusters(result: MoveReviewJudgmentResult): JsArray =
    JsArray(
      result.packet.claimSupportClusters.map(cluster =>
        Json.obj(
          "id" -> cluster.id,
          "kind" -> cluster.kind.toString,
          "families" -> cluster.families.map(_.toString),
          "subject" -> cluster.subject.toString,
          "primaryPosition" -> Json.obj(
            "fen" -> cluster.primaryPosition.fen,
            "ply" -> cluster.primaryPosition.ply,
            "sideToMove" -> cluster.primaryPosition.sideToMove.map(_.name)
          ),
          "primaryLine" -> cluster.primaryLine.map(lineRefSummary),
          "subjectMove" -> cluster.subjectMove,
          "scope" -> cluster.scope.map(_.toString),
          "anchorClaimIds" -> cluster.anchorClaimIds,
          "supportingClaimIds" -> cluster.supportingClaimIds,
          "constrainingClaimIds" -> cluster.constrainingClaimIds,
          "ideas" -> cluster.ideas.map(idea => Json.obj("id" -> idea.id, "family" -> idea.family.toString)),
          "evidenceLayers" -> cluster.evidence.map(_.layer.toString).distinct,
          "presentLayers" -> cluster.presentLayers.map(_.toString).toList.sorted,
          "confidence" -> cluster.confidence.toString,
          "salienceDrivers" -> cluster.salienceDrivers.map(_.toString),
          "interactions" -> cluster.interactions.map(interaction =>
            Json.obj(
              "kind" -> interaction.kind.toString,
              "sourceClaimId" -> interaction.sourceClaimId,
              "targetClaimId" -> interaction.targetClaimId,
              "strength" -> interaction.strength,
              "interactionEvidence" -> interaction.evidence.map(_.id)
            )
          )
        )
      )
    )

  private def claimEventClusters(result: MoveReviewJudgmentResult): JsArray =
    JsArray(
      result.packet.claimEventClusters.map(cluster =>
        Json.obj(
          "id" -> cluster.id,
          "kind" -> cluster.kind.toString,
          "causeKind" -> cluster.causeKind.toString,
          "comparisonKind" -> cluster.comparisonKind.toString,
          "referenceLine" -> lineRefSummary(cluster.referenceLine),
          "candidateLine" -> lineRefSummary(cluster.candidateLine),
          "eventLine" -> lineRefSummary(cluster.eventLine),
          "eventRootMove" -> cluster.eventRootMove,
          "verdict" -> cluster.verdict.toString,
          "winPercentLossForMover" -> cluster.winPercentLossForMover,
          "candidateWinPercentDeltaForMover" -> cluster.candidateWinPercentDeltaForMover,
          "families" -> cluster.families.map(_.toString),
          "primaryPosition" -> Json.obj(
            "fen" -> cluster.primaryPosition.fen,
            "ply" -> cluster.primaryPosition.ply,
            "sideToMove" -> cluster.primaryPosition.sideToMove.map(_.name)
          ),
          "scope" -> cluster.scope.toString,
          "memberClaimIds" -> cluster.memberClaimIds,
          "causeClaimIds" -> cluster.causeClaimIds,
          "evaluationClaimIds" -> cluster.evaluationClaimIds,
          "witnessClaimIds" -> cluster.witnessClaimIds,
          "relatedSupportClusterIds" -> cluster.relatedSupportClusterIds,
          "ideas" -> cluster.ideas.map(idea => Json.obj("id" -> idea.id, "family" -> idea.family.toString)),
          "evidenceLayers" -> cluster.evidence.map(_.layer.toString).distinct,
          "presentLayers" -> cluster.presentLayers.map(_.toString).toList.sorted,
          "confidence" -> cluster.confidence.toString,
          "salienceDrivers" -> cluster.salienceDrivers.map(_.toString),
          "interactions" -> cluster.interactions.map(interaction =>
            Json.obj(
              "kind" -> interaction.kind.toString,
              "sourceClaimId" -> interaction.sourceClaimId,
              "targetClaimId" -> interaction.targetClaimId,
              "strength" -> interaction.strength,
              "interactionEvidence" -> interaction.evidence.map(_.id)
            )
          )
        )
      )
    )

  private def topClaims(result: MoveReviewJudgmentResult): JsArray =
    JsArray(
      result.packet.claims
        .take(5)
        .map(claim => claimJson(result.packet, claim))
    )

  private def topPrimaryPlayedClaims(result: MoveReviewJudgmentResult): JsArray =
    JsArray(
      result.packet.claims
        .filter(primaryPlayedClaim(result.packet, _))
        .take(5)
        .map(claim => claimJson(result.packet, claim))
    )

  private def topContextClaims(result: MoveReviewJudgmentResult): JsArray =
    JsArray(
      result.packet.claims
        .filterNot(primaryPlayedClaim(result.packet, _))
        .take(5)
        .map(claim => claimJson(result.packet, claim))
    )

  private def claimJson(packet: EvidenceBackedJudgmentPacket, claim: ClaimSeed): JsObject =
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
      "claimInteractions" -> claim.salience.map(_.interactions.map(interaction =>
        Json.obj(
          "kind" -> interaction.kind.toString,
          "relatedClaimId" -> interaction.relatedClaimId,
          "strength" -> interaction.strength,
          "interactionEvidence" -> interaction.interactionEvidence.map(_.id)
        )
      )),
      "engineVerdict" -> claim.engineComparison.map(_.verdict.toString),
      "engineWinPercentLossForMover" -> claim.engineComparison.map(_.winPercentLossForMover),
      "missingLayerGroups" -> claim.supportStatus.map(_.missingLayerGroups.map(_.map(_.toString).toList.sorted)),
      "evidenceLayers" -> claim.evidence.map(_.layer.toString),
      "eventBinding" -> claimEventBinding(packet, claim)
    )

  private def primaryPlayedClaim(packet: EvidenceBackedJudgmentPacket, claim: ClaimSeed): Boolean =
    val playedMoves = packetPlayedMoves(packet)
    directPlayedClaim(claim, playedMoves) ||
      claim.evidence
        .flatMap(ref => packet.evidenceGraph.byId.get(ref.id))
        .exists {
          case EvidenceRecord(_, RelativeAssessmentEvidence(_), _) =>
            true
          case EvidenceRecord(_, CandidateComparisonEvidence(fact), _) =>
            playedComparison(fact, playedMoves)
          case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
            playedEventCause(cause, playedMoves)
          case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
            playedMoves.contains(normalizeMove(certification.playedMove)) ||
              certification.causes.exists(cause => playedEventCause(cause, playedMoves))
          case _ =>
            false
        }

  private def directPlayedClaim(claim: ClaimSeed, playedMoves: Set[String]): Boolean =
    playedMoves.nonEmpty &&
      (
        claim.subjectMove.exists(move => playedMoves.contains(normalizeMove(move))) ||
          claim.primaryLine.exists(line =>
            line.role == LineNodeRole.Played && playedMoves.contains(normalizeMove(line.rootMove))
          )
      )

  private def packetPlayedMoves(packet: EvidenceBackedJudgmentPacket): Set[String] =
    (
      packet.playedTransition.map(_.moveUci).toList ++
        packet.relativeAssessments.map(_.played.moveUci)
    ).map(normalizeMove).filter(_.nonEmpty).toSet

  private def playedComparison(fact: CandidateComparisonFact, playedMoves: Set[String]): Boolean =
    playedMoves.contains(normalizeMove(fact.candidateLine.rootMove)) &&
      (fact.kind == CandidateComparisonKind.PlayedVsBest ||
        fact.kind == CandidateComparisonKind.PlayedVsAlternative)

  private def playedEventCause(cause: RelativeCauseFact, playedMoves: Set[String]): Boolean =
    playedMoves.contains(normalizeMove(cause.eventRootMove)) &&
      (cause.comparisonKind == CandidateComparisonKind.PlayedVsBest ||
        cause.comparisonKind == CandidateComparisonKind.PlayedVsAlternative ||
        cause.comparisonKind == CandidateComparisonKind.BestVsSecond)

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def claimEventBinding(packet: EvidenceBackedJudgmentPacket, claim: ClaimSeed): JsObject =
    val clusterRoles =
      packet.claimEventClusters.flatMap { cluster =>
        val role =
          if cluster.causeClaimIds.contains(claim.id) then Some("CauseOwner")
          else if cluster.evaluationClaimIds.contains(claim.id) then Some("VerdictCarrier")
          else if cluster.witnessClaimIds.contains(claim.id) then Some("Witness")
          else if cluster.memberClaimIds.contains(claim.id) then Some("Member")
          else None
        role.map(role =>
          Json.obj(
            "clusterId" -> cluster.id,
            "role" -> role,
            "kind" -> cluster.kind.toString,
            "causeKind" -> cluster.causeKind.toString,
            "comparisonKind" -> cluster.comparisonKind.toString,
            "eventRootMove" -> cluster.eventRootMove
          )
        )
      }
    Json.obj(
      "hasDirectEventEvidence" -> claim.evidence.exists(ref =>
        ref.layer == EvidenceLayer.RelativeCause || ref.layer == EvidenceLayer.MoveVerdictCertification
      ),
      "clusterRoles" -> clusterRoles
    )

private object MoveReviewQualityInputFiles:

  def parseJsonDocuments(path: Path): List[JsValue] =
    val raw = Files.readString(path, StandardCharsets.UTF_8).stripPrefix("\uFEFF").trim
    if raw.startsWith("[") then Json.parse(raw).as[List[JsValue]]
    else raw.linesIterator.toList.filter(_.trim.nonEmpty).map(line => Json.parse(line))
