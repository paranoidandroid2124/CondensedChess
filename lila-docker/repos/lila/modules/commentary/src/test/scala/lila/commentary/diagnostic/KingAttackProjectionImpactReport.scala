package lila.commentary.diagnostic

import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import chess.{ Color, Square }
import chess.format.Fen
import play.api.libs.json.*

import scala.collection.mutable

import lila.commentary.certification.{
  Certification,
  CertificationEvidence,
  CertificationEvidenceBundle,
  CertificationEvidencePurpose,
  CertificationEvidenceStrength,
  CertificationExtractor,
  CertificationId,
  CertificationVerdict
}
import lila.commentary.projection.{
  StrategyProjectionAdmission,
  StrategyProjectionBandId,
  StrategyProjectionEvidence,
  StrategyProjectionEvidenceClaim,
  StrategyProjectionScopeContract
}
import lila.commentary.strategic.{ StrategicObjectExtraction, StrategicObjectExtractor }
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessValue }
import lila.commentary.witness.seed.{ StrategySupportSeedExtraction, StrategySupportSeedExtractor }
import lila.commentary.witness.u.{ UExtractionContext, UWitnessExtraction, UWitnessExtractor }

final case class KingAttackProbeAdmission(
    band: String,
    owner: String,
    admitted: Boolean,
    carrierCount: Int,
    routeCount: Int,
    reason: String
)

final case class KingAttackProjectionImpactRow(
    rowId: String,
    sourceFile: String,
    sourceKind: String,
    sourceSchema: Option[String],
    currentFen: String,
    actualStatus: String,
    extractionError: Option[String],
    probeAdmissions: Vector[KingAttackProbeAdmission]
)

final case class KingAttackProjectionImpactSummary(
    total: Int,
    uniqueFenTotal: Int,
    extracted: Int,
    extractionErrors: Int,
    actualBlockedMissingProjectionEvidenceRows: Int,
    probeCarrierRowsByBand: Map[String, Int],
    probeCarrierUniqueFenByBand: Map[String, Int],
    probeAdmittedRowsByBand: Map[String, Int],
    probeAdmittedUniqueFenByBand: Map[String, Int],
    probeAdmissionCountsByBand: Map[String, Int],
    probeRowsByBandAndOwner: Map[String, Int]
)

final case class KingAttackProjectionProbeFenAuditRow(
    currentFen: String,
    rowCount: Int,
    rowIds: Vector[String],
    sourceKinds: Vector[String],
    sourceSchemas: Vector[String],
    sideToMove: Option[String],
    fullmoveNumber: Option[Int],
    actualStatuses: Vector[String],
    admittedBands: Vector[String],
    admittedOwners: Vector[String],
    admittedBandOwners: Vector[String],
    carrierBands: Vector[String],
    auditConclusion: String
)

final case class KingAttackProjectionImpactReport(
    summary: KingAttackProjectionImpactSummary,
    rows: Vector[KingAttackProjectionImpactRow]
)

object KingAttackProjectionImpactReport:

  private val Bands = Vector("S01", "S02", "S03")

  def fromRows(rows: Vector[LowerDiagnosticLargeCorpus.Row]): KingAttackProjectionImpactReport =
    val fenCache = mutable.Map.empty[String, FenProbe]
    val impactRows = rows.map(row => rowFromCorpusRow(row, fenCache))
    KingAttackProjectionImpactReport(summary(impactRows), impactRows)

  def probeAdmittedFenLedger(rows: Vector[KingAttackProjectionImpactRow]): Vector[KingAttackProjectionProbeFenAuditRow] =
    rows
      .filter(_.probeAdmissions.exists(_.admitted))
      .groupBy(_.currentFen)
      .toVector
      .sortBy(_._1)
      .map { case (fen, fenRows) =>
        val admissions = fenRows.flatMap(_.probeAdmissions)
        val admitted = admissions.filter(_.admitted)
        KingAttackProjectionProbeFenAuditRow(
          currentFen = fen,
          rowCount = fenRows.size,
          rowIds = fenRows.map(_.rowId).distinct.sorted,
          sourceKinds = fenRows.map(_.sourceKind).distinct.sorted,
          sourceSchemas = fenRows.flatMap(_.sourceSchema).distinct.sorted,
          sideToMove = sideToMove(fen),
          fullmoveNumber = fullmoveNumber(fen),
          actualStatuses = fenRows.map(_.actualStatus).distinct.sorted,
          admittedBands = admitted.map(_.band).distinct.sorted,
          admittedOwners = admitted.map(_.owner).distinct.sorted,
          admittedBandOwners = admitted.map(admission => s"${admission.band}:${admission.owner}").distinct.sorted,
          carrierBands = admissions.filter(_.carrierCount > 0).map(_.band).distinct.sorted,
          auditConclusion = "diagnostic_probe_admitted_not_actual_public_admission"
        )
      }

  private final case class FenProbe(
      extractionError: Option[String],
      probeAdmissions: Vector[KingAttackProbeAdmission]
  )

  private def rowFromCorpusRow(
      row: LowerDiagnosticLargeCorpus.Row,
      fenCache: mutable.Map[String, FenProbe]
  ): KingAttackProjectionImpactRow =
    val probe =
      fenCache.getOrElseUpdate(row.input.currentFen, probeFen(row.input.currentFen))
    KingAttackProjectionImpactRow(
      rowId = row.id,
      sourceFile = row.sourceFile,
      sourceKind = row.sourceKind,
      sourceSchema = row.sourceSchema,
      currentFen = row.input.currentFen,
      actualStatus = if probe.extractionError.nonEmpty then "not_run_input_invalid" else actualStatus(row),
      extractionError = probe.extractionError,
      probeAdmissions = probe.probeAdmissions
    )

  private def probeFen(fen: String): FenProbe =
    StrategySupportSeedExtractor.fromFenFailClosed(Fen.Full.clean(fen)) match
      case Left(reason) =>
        FenProbe(Some(reason), Vector.empty)
      case Right(seedExtraction) =>
        val current = StrategicObjectExtractor.fromRoot(seedExtraction.rootState)
        FenProbe(
          extractionError = None,
          probeAdmissions = Vector(Color.White, Color.Black).flatMap(owner => probeForOwner(seedExtraction, current, owner))
        )

  private def actualStatus(row: LowerDiagnosticLargeCorpus.Row): String =
    if row.metadata.contains("projectionEvidenceClaims") || row.metadata.contains("projectionEvidence") then
      "actual_projection_evidence_present_not_parsed"
    else "blocked_missing_projection_evidence"

  private def probeForOwner(
      seedExtraction: StrategySupportSeedExtraction,
      current: StrategicObjectExtraction,
      owner: Color
  ): Vector[KingAttackProbeAdmission] =
    val certificationEvidence = kingAttackCertificationEvidence(current, owner)
    val certificationExtraction =
      CertificationExtractor.fromObjectExtractionFailClosed(current, certificationEvidence).toOption
    val certifiedEdge =
      certificationExtraction.flatMap(_.claims.forFamilyId("CertifiedKingSafetyEdge").find(_.owner.contains(owner)))
        .filter(_.verdict == CertificationVerdict.Certified)
    val comparativeFragility =
      certificationExtraction.flatMap(_.claims.forFamilyId("ComparativeKingFragility").find(_.owner.contains(owner)))
        .filter(_.verdict == CertificationVerdict.Certified)

    val s01Claims = s01EvidenceClaims(seedExtraction, current, owner)
    val s02Claims = certifiedEdge.toVector.flatMap(cert => s02EvidenceClaims(current, cert, owner))
    val s03Claims = s03EvidenceClaims(seedExtraction, current, owner)
    Vector(
      probeAdmission(
        band = "S01",
        owner = owner,
        seedExtraction = seedExtraction,
        claims = s01Claims,
        certificationEvidence = certificationEvidence,
        certificationReady = certifiedEdge.nonEmpty
      ),
      probeAdmission(
        band = "S02",
        owner = owner,
        seedExtraction = seedExtraction,
        claims = s02Claims,
        certificationEvidence = certificationEvidence,
        certificationReady = certifiedEdge.nonEmpty
      ),
      probeAdmission(
        band = "S03",
        owner = owner,
        seedExtraction = seedExtraction,
        claims = s03Claims,
        certificationEvidence = certificationEvidence,
        certificationReady = certifiedEdge.nonEmpty && comparativeFragility.nonEmpty
      )
    )

  private def probeAdmission(
      band: String,
      owner: Color,
      seedExtraction: StrategySupportSeedExtraction,
      claims: Vector[StrategyProjectionEvidenceClaim],
      certificationEvidence: CertificationEvidenceBundle,
      certificationReady: Boolean
  ): KingAttackProbeAdmission =
    val admitted =
      claims.exists: claim =>
        StrategyProjectionAdmission
          .admits(
            StrategyProjectionBandId(band),
            seedExtraction,
            StrategyProjectionEvidence.forSeedExtraction(seedExtraction, Vector(claim)),
            owner,
            certificationEvidence
          )
          .contains(true)
    val representativeDecision =
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId(band),
        seedExtraction,
        StrategyProjectionEvidence.empty,
        owner,
        certificationEvidence
      )
    KingAttackProbeAdmission(
      band = band,
      owner = colorKey(owner),
      admitted = admitted,
      carrierCount = claims.size,
      routeCount = claims.size,
      reason =
        if claims.isEmpty then "no_runtime_carrier"
        else if !certificationReady then "certification_not_ready"
        else if admitted then "diagnostic_certified_probe_admitted"
        else representativeDecision.left.getOrElse("projection_admission_rejected")
    )

  private def kingAttackCertificationEvidence(
      current: StrategicObjectExtraction,
      owner: Color
  ): CertificationEvidenceBundle =
    CertificationEvidenceBundle.forObjectExtraction(
      current,
      Vector(
        CertificationEvidence(
          familyId = CertificationId("ComparativeKingFragility"),
          color = owner,
          purposeStrengths =
            Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        ),
        CertificationEvidence(
          familyId = CertificationId("CertifiedKingSafetyEdge"),
          color = owner,
          purposeStrengths = Map(
            CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied,
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
          )
        )
      )
    )

  private def s01EvidenceClaims(
      seedExtraction: StrategySupportSeedExtraction,
      current: StrategicObjectExtraction,
      owner: Color
  ): Vector[StrategyProjectionEvidenceClaim] =
    val witnesses = UWitnessExtractor.fromRoot(seedExtraction.rootState)
    val context = UExtractionContext(seedExtraction.rootState)
    invokeVector(
      methodName = "s01KingWingStormCarriers",
      parameterTypes = Vector(classOf[StrategicObjectExtraction], classOf[UWitnessExtraction], classOf[UExtractionContext], classOf[Color]),
      args = Vector(current, witnesses, context, owner)
    ).map: carrier =>
      val source = carrier.productElement(0).asInstanceOf[Square]
      val target = carrier.productElement(1).asInstanceOf[Square]
      val defendingKing = carrier.productElement(2).asInstanceOf[Square]
      val route = carrier.productElement(3).asInstanceOf[String]
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionScopeContract.S01,
        kind = StrategyProjectionScopeContract.KingWingStormRouteCertified,
        owner = owner,
        anchor = WitnessAnchor.PieceSquareAnchor(source),
        payload = WitnessPayload(
          "contact_source_square" -> WitnessValue.SquareValue(source),
          "target_square" -> WitnessValue.SquareValue(target),
          "defending_king_square" -> WitnessValue.SquareValue(defendingKing),
          "king_wing_storm_route" -> WitnessValue.Token(route),
          "certification_family" -> WitnessValue.Token("CertifiedKingSafetyEdge")
        )
      )

  private def s02EvidenceClaims(
      current: StrategicObjectExtraction,
      certification: Certification,
      owner: Color
  ): Vector[StrategyProjectionEvidenceClaim] =
    invokeVector(
      methodName = "s02KingRingConcentrationCarriers",
      parameterTypes = Vector(classOf[StrategicObjectExtraction], classOf[Certification], classOf[Color]),
      args = Vector(current, certification, owner)
    ).map: carrier =>
      val defendingKing = carrier.productElement(0).asInstanceOf[Square]
      val sourceSquares = carrier.productElement(1).asInstanceOf[Set[Square]]
      val targetSquares = carrier.productElement(2).asInstanceOf[Set[Square]]
      val route = carrier.productElement(3).asInstanceOf[String]
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionScopeContract.S02,
        kind = StrategyProjectionScopeContract.KingRingConcentrationRouteCertified,
        owner = owner,
        anchor = WitnessAnchor.SquareAnchor(defendingKing),
        payload = WitnessPayload(
          "defending_king_square" -> WitnessValue.SquareValue(defendingKing),
          "source_squares" -> WitnessValue.SquareListValue(sourceSquares.toVector.sortBy(_.value)),
          "king_ring_target_squares" -> WitnessValue.SquareListValue(targetSquares.toVector.sortBy(_.value)),
          "king_ring_concentration_route" -> WitnessValue.Token(route),
          "certification_family" -> WitnessValue.Token("CertifiedKingSafetyEdge")
        )
      )

  private def s03EvidenceClaims(
      seedExtraction: StrategySupportSeedExtraction,
      current: StrategicObjectExtraction,
      owner: Color
  ): Vector[StrategyProjectionEvidenceClaim] =
    val context = UExtractionContext(seedExtraction.rootState)
    invokeVector(
      methodName = "s03DiagonalKingAttackCarriers",
      parameterTypes = Vector(classOf[StrategicObjectExtraction], classOf[UExtractionContext], classOf[Color]),
      args = Vector(current, context, owner)
    ).map: carrier =>
      val defendingKing = carrier.productElement(0).asInstanceOf[Square]
      val diagonalSource = carrier.productElement(1).asInstanceOf[Square]
      val endpointSquares = carrier.productElement(2).asInstanceOf[Set[Square]]
      val route = carrier.productElement(3).asInstanceOf[String]
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionScopeContract.S03,
        kind = StrategyProjectionScopeContract.DiagonalKingAttackRouteCertified,
        owner = owner,
        anchor = WitnessAnchor.SquareAnchor(defendingKing),
        payload = WitnessPayload(
          "defending_king_square" -> WitnessValue.SquareValue(defendingKing),
          "diagonal_source_square" -> WitnessValue.SquareValue(diagonalSource),
          "diagonal_endpoint_squares" -> WitnessValue.SquareListValue(endpointSquares.toVector.sortBy(_.value)),
          "diagonal_king_attack_route" -> WitnessValue.Token(route),
          "certification_family" -> WitnessValue.Token("CertifiedKingSafetyEdge")
        )
      )

  private def invokeVector(
      methodName: String,
      parameterTypes: Vector[Class[?]],
      args: Vector[AnyRef]
  ): Vector[Product] =
    val method = privateMethod(methodName, parameterTypes)
    method.invoke(StrategyProjectionAdmission, args*).asInstanceOf[Vector[Product]]

  private def privateMethod(methodName: String, parameterTypes: Vector[Class[?]]): Method =
    val method =
      StrategyProjectionAdmission.getClass.getDeclaredMethods
        .find(method => method.getName.contains(methodName) && method.getParameterTypes.toVector == parameterTypes)
        .getOrElse(throw IllegalStateException(s"missing projection carrier builder $methodName"))
    method.setAccessible(true)
    method

  private def summary(rows: Vector[KingAttackProjectionImpactRow]): KingAttackProjectionImpactSummary =
    val probes = rows.flatMap(_.probeAdmissions)
    val admitted = probes.filter(_.admitted)
    val carrierRowsByBand =
      Bands.map(band => band -> rows.count(_.probeAdmissions.exists(probe => probe.band == band && probe.carrierCount > 0))).toMap
    val admittedRowsByBand =
      Bands.map(band => band -> rows.count(_.probeAdmissions.exists(probe => probe.band == band && probe.admitted))).toMap
    val carrierUniqueFenByBand =
      Bands.map(band => band -> uniqueFenCount(rows.filter(_.probeAdmissions.exists(probe => probe.band == band && probe.carrierCount > 0)))).toMap
    val admittedUniqueFenByBand =
      Bands.map(band => band -> uniqueFenCount(rows.filter(_.probeAdmissions.exists(probe => probe.band == band && probe.admitted)))).toMap
    KingAttackProjectionImpactSummary(
      total = rows.size,
      uniqueFenTotal = uniqueFenCount(rows),
      extracted = rows.count(_.extractionError.isEmpty),
      extractionErrors = rows.count(_.extractionError.nonEmpty),
      actualBlockedMissingProjectionEvidenceRows = rows.count(_.actualStatus == "blocked_missing_projection_evidence"),
      probeCarrierRowsByBand = carrierRowsByBand,
      probeCarrierUniqueFenByBand = carrierUniqueFenByBand,
      probeAdmittedRowsByBand = admittedRowsByBand,
      probeAdmittedUniqueFenByBand = admittedUniqueFenByBand,
      probeAdmissionCountsByBand = countBy(admitted.map(_.band)),
      probeRowsByBandAndOwner = countBy(admitted.map(probe => s"${probe.band}:${probe.owner}"))
    )

  private def uniqueFenCount(rows: Vector[KingAttackProjectionImpactRow]): Int =
    rows.map(_.currentFen).distinct.size

  private def countBy(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"

  private def sideToMove(fen: String): Option[String] =
    fen.split("\\s+").lift(1).collect:
      case "w" => "white"
      case "b" => "black"

  private def fullmoveNumber(fen: String): Option[Int] =
    fen.split("\\s+").lift(5).flatMap(_.toIntOption)

object KingAttackProjectionImpactReportJson:

  def summaryJson(summary: KingAttackProjectionImpactSummary): JsObject =
    Json.obj(
      "total" -> summary.total,
      "uniqueFenTotal" -> summary.uniqueFenTotal,
      "extracted" -> summary.extracted,
      "extractionErrors" -> summary.extractionErrors,
      "actualBlockedMissingProjectionEvidenceRows" -> summary.actualBlockedMissingProjectionEvidenceRows,
      "probeCarrierRowsByBand" -> summary.probeCarrierRowsByBand,
      "probeCarrierUniqueFenByBand" -> summary.probeCarrierUniqueFenByBand,
      "probeAdmittedRowsByBand" -> summary.probeAdmittedRowsByBand,
      "probeAdmittedUniqueFenByBand" -> summary.probeAdmittedUniqueFenByBand,
      "probeAdmissionCountsByBand" -> summary.probeAdmissionCountsByBand,
      "probeRowsByBandAndOwner" -> summary.probeRowsByBandAndOwner,
      "probeMode" -> "diagnostic_certified_route_evidence_generated_from_runtime_carriers",
      "actualMode" -> "blocked unless source row carries projection evidence claims"
    )

  def rowJson(row: KingAttackProjectionImpactRow): JsObject =
    Json.obj(
      "rowId" -> row.rowId,
      "sourceFile" -> row.sourceFile,
      "sourceKind" -> row.sourceKind,
      "sourceSchema" -> row.sourceSchema,
      "currentFen" -> row.currentFen,
      "actualStatus" -> row.actualStatus,
      "extractionError" -> row.extractionError,
      "probeAdmissions" -> row.probeAdmissions.map(probeJson)
    )

  private def probeJson(probe: KingAttackProbeAdmission): JsObject =
    Json.obj(
      "band" -> probe.band,
      "owner" -> probe.owner,
      "admitted" -> probe.admitted,
      "carrierCount" -> probe.carrierCount,
      "routeCount" -> probe.routeCount,
      "reason" -> probe.reason
    )

  def probeFenAuditRowJson(row: KingAttackProjectionProbeFenAuditRow): JsObject =
    Json.obj(
      "currentFen" -> row.currentFen,
      "rowCount" -> row.rowCount,
      "rowIds" -> row.rowIds,
      "sourceKinds" -> row.sourceKinds,
      "sourceSchemas" -> row.sourceSchemas,
      "sideToMove" -> row.sideToMove,
      "fullmoveNumber" -> row.fullmoveNumber,
      "actualStatuses" -> row.actualStatuses,
      "admittedBands" -> row.admittedBands,
      "admittedOwners" -> row.admittedOwners,
      "admittedBandOwners" -> row.admittedBandOwners,
      "carrierBands" -> row.carrierBands,
      "auditConclusion" -> row.auditConclusion
    )

object KingAttackProjectionImpactReportRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/king-attack-projection-impact")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    write(KingAttackProjectionImpactReport.fromRows(rows), options.outputDir)

  def write(report: KingAttackProjectionImpactReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("king-attack-projection-impact-summary.json"),
      Json.prettyPrint(KingAttackProjectionImpactReportJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("king-attack-projection-impact-rows.jsonl"),
      report.rows.map(KingAttackProjectionImpactReportJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("king-attack-projection-probe-admitted-fen-ledger.jsonl"),
      KingAttackProjectionImpactReport
        .probeAdmittedFenLedger(report.rows)
        .map(KingAttackProjectionImpactReportJson.probeFenAuditRowJson)
        .map(Json.stringify)
        .mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    val admittedRows =
      report.rows.filter(_.probeAdmissions.exists(_.admitted)).take(100)
    Files.writeString(
      outputDir.resolve("king-attack-projection-probe-admitted-sample.jsonl"),
      admittedRows.map(KingAttackProjectionImpactReportJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )

  private final case class RunnerOptions(
      outputDir: Path,
      inputPath: Option[Path],
      limit: Option[Int]
  )

  private object RunnerOptions:
    def parse(args: Vector[String]): RunnerOptions =
      def valueAfter(flag: String): Option[String] =
        args.sliding(2).collectFirst { case Vector(`flag`, value) => value }
      RunnerOptions(
        outputDir = valueAfter("--out").map(Paths.get(_)).getOrElse(defaultOutputDir),
        inputPath = valueAfter("--input").map(Paths.get(_)),
        limit = valueAfter("--limit").flatMap(_.toIntOption)
      )
