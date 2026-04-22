package lila.commentary.witness.u

class UBroadAuditContractTest extends munit.FunSuite:

  private def normalizeSnapshot(value: String): String =
    value.replace("\r\n", "\n").stripSuffix("\n")

  test("u coverage matrix snapshot stays synchronized with the renderer"):
    val rendered = normalizeSnapshot(UBroadCoverageMatrix.render())
    val artifact = normalizeSnapshot(UBroadCoverageMatrix.loadArtifact())

    assertEquals(rendered, artifact)

  test("u broad matrix covers the frozen active U-primary and U-attached inventories exactly"):
    assertEquals(
      UBroadCoverageMatrix.activePrimaryDescriptorIds,
      UScopeContract.activePrimaryDescriptorIds.map(_.value)
    )
    assertEquals(
      UBroadCoverageMatrix.activeAttachedDescriptorIds,
      UAttachedScopeContract.activeAttachedDescriptorIds.map(_.value)
    )

  test("current formal witness corpus row count matches the U broad audit matrix"):
    assertEquals(
      UBroadCoverageMatrix.formalCorpusRows.map(_.descriptorId).toSet,
      Set(
        "file_lane_state",
        "bishop_pair_state",
        "sector_asymmetry_state",
        "rook_on_open_file_state",
        "passed_pawn_entity_state",
        "available_lever_trigger",
        "diagonal_lane_only",
        "weak_pawn_target_state",
        "weak_outpost_square_state",
        "knight_on_outpost_square",
        "loose_piece_target_state",
        "duty_bound_defender",
        "pin",
        "skewer",
        "fork",
        "overload",
        "short_run_slider_gate_restriction",
        "pawn_push_break_contact_source",
        "structural_space_claim"
      )
    )
    assertEquals(UBroadCoverageMatrix.formalCorpusRowCount(), 218)
    assertEquals(
      UBroadCoverageMatrix.formalCorpusDescriptorRowCounts,
      UBroadCoverageMatrix.expectedFormalCorpusRowsByDescriptor
    )
    assertEquals(
      UBroadCoverageMatrix.formalCorpusRowCount(),
      UBroadCoverageMatrix.expectedFormalCorpusRows
    )

  test("u broad-confidence-green is claimed only for descriptor-local closed corpus"):
    val greenDescriptorIds =
      UBroadCoverageMatrix.allPolicies.collect:
        case policy if policy.status == "broad-confidence-green" => policy.descriptorId

    assertEquals(
      greenDescriptorIds,
      Vector(
        "file_lane_state",
        "diagonal_lane_only",
        "weak_pawn_target_state",
        "passed_pawn_entity_state",
        "weak_outpost_square_state",
        "loose_piece_target_state",
        "pawn_push_break_contact_source",
        "sector_asymmetry_state",
        "available_lever_trigger",
        "rook_on_open_file_state",
        "bishop_pair_state",
        "knight_on_outpost_square",
        "duty_bound_defender",
        "short_run_slider_gate_restriction",
        "pin",
        "fork",
        "skewer",
        "overload",
        "structural_space_claim"
      )
    )
    assertEquals(UBroadCoverageMatrix.statuses.toSet, Set("broad-confidence-green"))
    assertEquals(
      UBroadCoverageMatrix.rootDependencyGates.toSet,
      Set("closed: required root dependencies are root broad-confidence-green")
    )
    assertEquals(UBroadCoverageMatrix.unresolvedRootDependencies, Map.empty)
    assertEquals(UBroadCoverageMatrix.rootDependencyStatuses.values.toSet, Set("broad-confidence-green"))
    assertEquals(UBroadCoverageMatrix.broadGreenPromotionViolations, Map.empty)
