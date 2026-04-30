package lila.commentary.diagnostic

class AttackScaffoldImpactReportTest extends munit.FunSuite:

  test("attack scaffold impact exposes unique-fen ledgers without treating drops as false-positive proof"):
    val sourceDroppedA = impactRow("external_candidate:a", Some("AttackScaffold"), hasAttackScaffold = false, "8/8/8/8/8/8/8/4K2k b - - 0 7")
    val sourceDroppedB = sourceDroppedA.copy(rowId = "external_candidate:b", sourceKind = "external_candidate")
    val sourceStill =
      impactRow(
        "external_candidate:c",
        Some("AttackScaffold"),
        hasAttackScaffold = true,
        "8/8/8/8/8/8/4K3/4R2k b - - 0 8",
        supportFragmentIds = Vector("pinned_piece"),
        carrierFragmentIds = Vector("file_lane_state"),
        smellSupportIds = Vector("pinned_piece")
      )
    val report = AttackScaffoldImpactReport.fromImpactRows(Vector(sourceDroppedA, sourceDroppedB, sourceStill))

    assertEquals(report.summary.sourceSchemaAttackScaffoldUniqueFen, 2)
    assertEquals(report.summary.sourceSchemaAttackScaffoldDroppedUniqueFen, 1)
    assertEquals(report.summary.sourceSchemaAttackScaffoldStillCurrentUniqueFen, 1)

    val droppedLedger = AttackScaffoldImpactReport.sourceAttackScaffoldFenLedger(report.rows).find(_.currentStatus == "current_contract_dropped").getOrElse(fail("missing dropped ledger row"))
    assertEquals(droppedLedger.rowCount, 2)
    assertEquals(droppedLedger.sideToMove, Some("black"))
    assertEquals(droppedLedger.fullmoveNumber, Some(7))
    assertEquals(droppedLedger.auditConclusion, "current_contract_dropped_not_false_positive_proof")

    val residualLedger = AttackScaffoldImpactReport.currentAttackScaffoldFenLedger(report.rows).headOption.getOrElse(fail("missing residual ledger row"))
    assertEquals(residualLedger.currentStatus, "current_residual_smell_supported")
    assertEquals(residualLedger.smellSupportIds, Vector("pinned_piece"))
    assertEquals(residualLedger.auditConclusion, "current_residual_requires_exact_board_audit")

  private def impactRow(
      rowId: String,
      sourceSchema: Option[String],
      hasAttackScaffold: Boolean,
      fen: String,
      supportFragmentIds: Vector[String] = Vector.empty,
      carrierFragmentIds: Vector[String] = Vector.empty,
      smellSupportIds: Vector[String] = Vector.empty
  ): AttackScaffoldImpactRow =
    AttackScaffoldImpactRow(
      rowId = rowId,
      sourceFile = "synthetic.jsonl",
      sourceKind = "external_candidate",
      sourceSchema = sourceSchema,
      currentFen = fen,
      hasAttackScaffold = hasAttackScaffold,
      attackScaffoldCount = if hasAttackScaffold then 1 else 0,
      attackScaffoldColors = if hasAttackScaffold then Vector("black") else Vector.empty,
      supportFragmentIds = supportFragmentIds,
      carrierFragmentIds = carrierFragmentIds,
      smellSupportIds = smellSupportIds,
      extractionError = None
    )
