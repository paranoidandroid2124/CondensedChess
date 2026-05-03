package lila.commentary.projection

import java.nio.file.{ Files, Paths }

import chess.{ Color, Square }
import chess.format.Fen

import lila.commentary.witness.WitnessAnchor
import lila.commentary.witness.seed.StrategySupportSeedExtractor

class StrategyProjectionCoverageContractTest extends munit.FunSuite:

  private val allProjectionBandIds: Vector[String] =
    (1 to 25).map(index => f"S$index%02d").toVector

  test("projection coverage contract names row groups without temporary stage labels"):
    val source =
      Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala"))
    Vector(
      "foundationBroadReadyCoverageBandIds",
      "broadCoverageCandidateBandIds",
      "initiativeReleaseCoverageFreezeBandIds",
      "kingAttackCoverageFreezeBandIds",
      "pawnTargetStructuralDamageCoverageFreezeBandIds",
      "passerCreationSuppressionCoverageFreezeBandIds",
      "globalClosureBroadReadyCoverageBandIds",
      "frozenCoverageBandIds",
      "rowSpecificAdmissionBurdenFreezeByBand",
      "projectionEvidenceKindFreezeByBand",
      "preLiveAdmissionBurdenFreezeByBand",
      "preLiveProjectionEvidenceKindFreezeByBand",
      "preLiveRuntimeBoundaryFreezeByBand"
    ).foreach: fragment =>
      assert(!source.contains(fragment), clues(s"runtime contract must not expose temporary stage name $fragment"))

  test("authority docs freeze Sxx projection terminology"):
    val core =
      Files.readString(Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/CommentaryCoreSSOT.md"))
    val boundary =
      Files.readString(Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/StrategyProjectionBoundaryMatrix.md"))
    val validation =
      Files.readString(Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/ValidationMethodology.md"))
    val witnesses =
      Files.readString(Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/Witnesses61.md"))

    Vector(
      "| `semantic boundary` | Sxx meaning, rival separation, and false-positive shape; not public admission |",
      "| `validation scaffold` | exact-board `src/test` coverage for legacy Sxx contracts, including raw matcher checks and corpus rows |",
      "| `LegacyValidationScaffold` | non-public authority attached to validation-scaffold admissions; never lowerable to public claims |",
      "| `CertifiedTruth(K)` | factory-minted same-root carrier bundle consumed by projection geometry |",
      "| `StrategyProjectionSliceDescriptor` | production owner of `A`, `mu`, descriptor-owned `d` algebra, carrier binding law, overlap policy, phrase capability, falsification burden, rival bands, and rollout flag |",
      "| `DescriptorCertifiedRuntime` | only projection admission authority that may lower into public claims |",
      "| `production public admission` | descriptor/K path from `CertifiedTruth(K)` through geometry and producer into `EvidenceClaimProducer` |",
      "For Sxx projection work, `start-ready`, `live admission`, `runtime admission`,"
    ).foreach: token =>
      assert(core.contains(token), clues(s"core terminology token must be frozen: $token"))

    Vector(boundary, validation, witnesses).foreach: docs =>
      assert(docs.contains("semantic boundary"))
      assert(docs.contains("validation scaffold"))
      assert(docs.contains("production public admission"))
    assertEquals(core.contains("The `24` strategy labels"), false)

  test("projection row clusters cover S01-S25 without deferred coverage leftovers"):
    assertEquals(
      StrategyProjectionCoverageContract.positionalAccessBandIds.map(_.value),
      Vector("S06", "S09", "S10", "S12", "S20", "S23", "S25")
    )
    assertEquals(
      StrategyProjectionCoverageContract.conversionHoldTargetBandIds.map(_.value),
      Vector("S17", "S18", "S19", "S22", "S24")
    )
    assertEquals(
      StrategyProjectionCoverageContract.initiativeReleaseCounterplayBandIds.map(_.value),
      Vector("S05", "S07", "S08", "S21")
    )
    assertEquals(
      StrategyProjectionCoverageContract.kingAttackBandIds.map(_.value),
      Vector("S01", "S02", "S03", "S04")
    )
    assertEquals(
      StrategyProjectionCoverageContract.pawnStructureDamageBandIds.map(_.value),
      Vector("S11", "S13", "S14")
    )
    assertEquals(
      StrategyProjectionCoverageContract.passerCreationSuppressionBandIds.map(_.value),
      Vector("S15", "S16")
    )
    assertEquals(
      StrategyProjectionCoverageContract.allProjectionBandIds.map(_.value),
      allProjectionBandIds
    )
    assertEquals(
      StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value),
      Vector.empty
    )
    assertEquals(
      StrategyProjectionScopeContract.startReadyBandIds.map(_.value),
      Vector("S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09", "S10", "S11", "S12", "S13", "S14", "S15", "S16", "S17", "S18", "S19", "S20", "S21", "S22", "S23", "S24", "S25")
    )

  test("finite-cover envelope covers every Sxx band with stable certified fact families"):
    assertEquals(
      StrategyGeometryFoundation.certifiedFactFamilies.map(_.key),
      Vector(
        "board_transition",
        "move_causal_tactic",
        "king_safety_attack",
        "line_access_activity",
        "pawn_structure_passer",
        "space_restriction_mobility",
        "initiative_counterplay",
        "piece_quality_material_trade",
        "endgame_conversion_holding"
      )
    )
    assertEquals(
      StrategyGeometryFoundation.certifiedFactFamiliesByBand.view.mapValues(_.map(_.key)).toMap,
      Map(
        "S01" -> Vector("king_safety_attack"),
        "S02" -> Vector("king_safety_attack", "line_access_activity"),
        "S03" -> Vector("king_safety_attack", "line_access_activity"),
        "S04" -> Vector("king_safety_attack"),
        "S05" -> Vector("pawn_structure_passer", "initiative_counterplay"),
        "S06" -> Vector("space_restriction_mobility"),
        "S07" -> Vector("initiative_counterplay"),
        "S08" -> Vector("initiative_counterplay"),
        "S09" -> Vector("line_access_activity"),
        "S10" -> Vector("space_restriction_mobility", "line_access_activity"),
        "S11" -> Vector("pawn_structure_passer"),
        "S12" -> Vector("line_access_activity", "space_restriction_mobility"),
        "S13" -> Vector("pawn_structure_passer"),
        "S14" -> Vector("pawn_structure_passer"),
        "S15" -> Vector("pawn_structure_passer"),
        "S16" -> Vector("pawn_structure_passer", "endgame_conversion_holding"),
        "S17" -> Vector("piece_quality_material_trade"),
        "S18" -> Vector("piece_quality_material_trade"),
        "S19" -> Vector("piece_quality_material_trade"),
        "S20" -> Vector("space_restriction_mobility"),
        "S21" -> Vector("initiative_counterplay", "pawn_structure_passer"),
        "S22" -> Vector("endgame_conversion_holding"),
        "S23" -> Vector("endgame_conversion_holding", "line_access_activity"),
        "S24" -> Vector("move_causal_tactic", "piece_quality_material_trade"),
        "S25" -> Vector("line_access_activity")
      )
    )
    assertEquals(
      StrategyGeometryFoundation.certifiedFactFamiliesByBand.keySet,
      allProjectionBandIds.toSet
    )
    assert(
      StrategyGeometryFoundation.certifiedFactFamiliesByBand.values.forall(_.nonEmpty),
      clues("each Sxx band must belong to at least one finite-cover family")
    )
    assertEquals(
      StrategyGeometryFoundation.certifiedFactFamiliesByBand("S23").map(_.key),
      Vector("endgame_conversion_holding", "line_access_activity")
    )

  test("public-path disposition algebra is closed and anti-case is terminal"):
    assertEquals(
      StrategyGeometryFoundation.publicPathDispositions.map(_.key),
      Vector("certified", "support_only", "context_only", "deferred", "rejected", "anti_case")
    )
    assertEquals(
      StrategyGeometryFoundation.publicPathDispositions.filter(_.entersSxxAdmission).map(_.key),
      Vector("certified")
    )
    assertEquals(
      StrategyGeometryFoundation.publicPathDispositions.filter(_.canOwnPublicClaim).map(_.key),
      Vector("certified")
    )
    assertEquals(
      StrategyGeometryFoundation.PublicPathDisposition.fromKey("anti_case"),
      Some(StrategyGeometryFoundation.PublicPathDisposition.AntiCase)
    )

  test("unsupported projection bands remain fail-closed outside S01-S25"):
    val extraction = seedExtraction("7k/8/8/8/8/8/8/4K3 w - - 0 1")

    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds, Vector.empty)
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S26"),
        extraction,
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Left("Unsupported projection admission band: S26")
    )

  test("S04 production public admission contract names exact burden, production-public boundary, and projection evidence kind"):
    val s04 = StrategyProjectionBandId("S04")
    val evidenceKind = StrategyProjectionEvidenceKind("king_shelter_breach_route_certified")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s04), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s04), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S04"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s04, evidenceKind), true)
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S04"),
      Vector(
        "king_shelter_breach_requires_same_defender_king_safety_shell",
        "king_shelter_breach_requires_shell_payload_or_support_break_route",
        "king_shelter_breach_requires_same_defender_certified_king_safety_edge",
        "same_task_projection_evidence_must_mirror_s04_owner_defending_king_shell_anchor_breach_squares_and_route",
        "s01_storm_s02_concentration_s03_diagonal_s24_tactic_or_optional_strengthening_is_non_admitting",
        "wrong_owner_wrong_defender_wrong_shell_wrong_route_stale_or_support_only_evidence_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S04"),
      Vector(evidenceKind)
    )
    val docs =
      Vector(
        "modules/commentary/docs/legacy-pre-semantic-reset/StrategyProjectionBoundaryMatrix.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/CommentaryCoreSSOT.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/Witnesses61.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/ValidationMethodology.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/DecisionFreezeLedger.md"
      ).map(path => Files.readString(Paths.get(path))).mkString("\n")
    Vector(
      "`S04` | `shelter_breach_route`",
      "shell_payload_breach",
      "support_break_breach",
      "king_shelter_breach_route_certified",
      "rowSpecificAdmissionBurdensByBand",
      "exactValidationScaffoldByBand",
      "declaredProjectionEvidenceKindsByBand"
    ).foreach: token =>
      assert(docs.contains(token), clues(s"S04 scaffold contract token $token must be documented"))

  test("S02 production public admission contract names exact king-ring concentration burden and projection evidence kind"):
    val s02 = StrategyProjectionBandId("S02")
    val evidenceKind = StrategyProjectionEvidenceKind("king_ring_concentration_route_certified")
    val extraction = seedExtraction("6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s02), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s02), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S02"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s02, evidenceKind), true)
    assertEquals(
      StrategyProjectionAdmission.admits(s02, extraction, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S02"),
      Vector(
        "king_ring_concentration_requires_same_defending_king_attack_scaffold",
        "king_ring_concentration_requires_certified_same_owner_king_safety_edge",
        "same_task_projection_evidence_must_mirror_s02_owner_defending_king_ring_targets_source_set_and_route",
        "s03_diagonal_s04_shell_s09_file_penetration_or_optional_strengthening_is_non_admitting",
        "wrong_owner_wrong_king_wrong_targets_wrong_sources_wrong_route_stale_or_support_only_evidence_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S02"),
      Vector(evidenceKind)
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S02")
        .contains("same_task_projection_evidence_must_mirror_s02_owner_defending_king_ring_targets_source_set_and_route_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S02")
        .contains("wrong_owner_wrong_king_wrong_targets_wrong_sources_wrong_route_stale_or_support_only_evidence_not_counted")
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S02")
        .contains("ProjectionEvidence:king_ring_concentration_route_certified")
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S02")
        .contains("ObjectSupportOnly:AttackScaffold(non_truth_owner)")
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S02")
        .contains("CertificationSupportOnly:CertifiedKingSafetyEdge(non_truth_owner)")
    )
    val docs =
      Vector(
        "modules/commentary/docs/legacy-pre-semantic-reset/StrategyProjectionBoundaryMatrix.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/CommentaryCoreSSOT.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/Witnesses61.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/ValidationMethodology.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/DecisionFreezeLedger.md"
      ).map(path => Files.readString(Paths.get(path))).mkString("\n")
    Vector(
      "`S02` | `attack_concentration_route`",
      "direct_piece_concentration",
      "lane_strengthened_concentration",
      "king_ring_concentration_route_certified",
      "rowSpecificAdmissionBurdensByBand",
      "exactValidationScaffoldByBand",
      "declaredProjectionEvidenceKindsByBand"
    ).foreach: token =>
      assert(docs.contains(token), clues(s"S02 scaffold contract token $token must be documented"))

  test("S03 production public admission contract names exact diagonal king-attack burden and projection evidence kind"):
    val s03 = StrategyProjectionBandId("S03")
    val evidenceKind = StrategyProjectionEvidenceKind("diagonal_king_attack_route_certified")
    val docs =
      Vector(
        "modules/commentary/docs/legacy-pre-semantic-reset/StrategyProjectionBoundaryMatrix.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/CommentaryCoreSSOT.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/Witnesses61.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/ValidationMethodology.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/DecisionFreezeLedger.md"
      ).map(path => Files.readString(Paths.get(path))).mkString("\n")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s03), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s03), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S03"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s03, evidenceKind), true)
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        seedExtraction("6k1/8/6p1/8/3B4/8/6R1/6K1 w - - 0 1"),
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S03"),
      Vector(
        "diagonal_king_attack_requires_king_theater_diagonal_lane",
        "diagonal_king_attack_requires_same_defending_king_attack_scaffold",
        "diagonal_king_attack_requires_comparative_fragility_and_certified_edge",
        "same_task_projection_evidence_must_mirror_s03_owner_defending_king_diagonal_source_entry_squares_and_route",
        "s02_concentration_s12_local_access_bishop_pair_or_non_king_diagonal_is_non_admitting",
        "attack_scaffold_certification_diagonal_only_stale_or_wrong_task_evidence_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S03"),
      Vector(evidenceKind)
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S03")
        .contains("ObjectSupportOnly:AttackScaffold(non_truth_owner)")
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S03")
        .contains("CertificationSupportOnly:ComparativeKingFragility|CertifiedKingSafetyEdge(non_truth_owner)")
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S03")
        .contains("ProjectionEvidence:diagonal_king_attack_route_certified")
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S03")
        .contains("same_task_projection_evidence_must_mirror_s03_owner_defending_king_diagonal_source_entry_squares_and_route_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S03")
        .contains("attack_scaffold_certification_diagonal_only_stale_or_wrong_task_evidence_not_counted")
    )
    StrategyProjectionCoverageContract.requiredCoveragePairsFor("S03").foreach: pair =>
      assert(
        docs.contains(pair._1) && docs.contains(pair._2),
        clues(s"docs must mention S03 coverage pair ${pair._1}/${pair._2}")
      )
    Vector(
      "`S03` | `diagonal_attack_route`",
      "king_facing_diagonal_entry",
      "fragility_linked_diagonal",
      "diagonal_king_attack_route_certified",
      "rowSpecificAdmissionBurdensByBand",
      "exactValidationScaffoldByBand",
      "declaredProjectionEvidenceKindsByBand"
    ).foreach: token =>
      assert(docs.contains(token), clues(s"docs must mention S03 scaffold contract token $token"))

  test("S01 production public admission contract names exact storm burden and projection evidence kind"):
    val s01 = StrategyProjectionBandId("S01")
    val evidenceKind = StrategyProjectionEvidenceKind("king_wing_storm_route_certified")
    val extraction = seedExtraction("6k1/6pp/8/6p1/3B1p2/8/Q3P1RP/6K1 w - - 0 1")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s01), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s01), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S01"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s01, evidenceKind), true)
    assertEquals(
      StrategyProjectionAdmission.admits(s01, extraction, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S01"),
      Vector(
        "king_wing_storm_route_requires_same_anchor_lever_and_contact_source",
        "king_wing_storm_route_requires_non_center_owner_wing_contact_source_and_target",
        "king_wing_storm_route_requires_same_defending_king_attack_scaffold_and_certified_edge",
        "same_task_projection_evidence_must_mirror_s01_source_target_defending_king_and_route",
        "s05_center_release_s21_counterplay_castling_shell_or_optional_strengthening_is_non_admitting",
        "wrong_source_wrong_target_wrong_king_wrong_route_stale_or_support_only_evidence_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S01"),
      Vector(evidenceKind)
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S01")
        .contains("ProjectionEvidence:king_wing_storm_route_certified")
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S01")
        .contains("ObjectSupportOnly:AttackScaffold(non_truth_owner)")
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S01")
        .contains("CertificationSupportOnly:CertifiedKingSafetyEdge(non_truth_owner)")
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S01")
        .contains("same_task_projection_evidence_must_mirror_s01_source_target_defending_king_and_route_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S01")
        .contains("wrong_source_wrong_target_wrong_king_wrong_route_stale_or_support_only_evidence_not_counted")
    )

  test("S09 production public admission contract names exact file-penetration burden and projection evidence kind"):
    val s09 = StrategyProjectionBandId("S09")
    val evidenceKind = StrategyProjectionEvidenceKind("file_penetration_route_certified")
    val extraction = seedExtraction("6k1/pppp1ppp/8/8/8/8/PPPPRPPP/6K1 w - - 0 1")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s09), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s09), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S09"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s09, evidenceKind), true)
    assertEquals(
      StrategyProjectionAdmission.admits(s09, extraction, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S09"),
      Vector(
        "file_penetration_route_requires_owner_file_lane_state",
        "file_penetration_route_requires_same_file_entry_or_penetration_consequence",
        "same_task_projection_evidence_must_mirror_s09_owner_file_source_entry_and_route",
        "s02_attack_s23_king_activity_s25_rank_access_or_optional_strengthening_is_non_admitting",
        "wrong_file_wrong_source_wrong_entry_wrong_route_stale_or_support_only_evidence_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S09"),
      Vector(evidenceKind)
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S09")
        .contains("ProjectionEvidence:file_penetration_route_certified")
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S09")
        .contains("CertificationSupportOnly:MaterialHarvest|WinningEndgame")
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S09")
        .contains("SupportOnly:AttackScaffold|CertifiedKingSafetyEdge")
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S09")
        .contains("same_task_projection_evidence_must_mirror_s09_owner_file_source_entry_and_route_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S09")
        .contains("wrong_file_wrong_source_wrong_entry_wrong_route_stale_or_support_only_evidence_not_counted")
    )

    val docs =
      Vector(
        "modules/commentary/docs/legacy-pre-semantic-reset/StrategyProjectionBoundaryMatrix.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/CommentaryCoreSSOT.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/Witnesses61.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/ValidationMethodology.md"
      ).map(path => Files.readString(Paths.get(path))).mkString("\n")
    Vector(
      "`S09` | `penetration_route`",
      "open_file_entry",
      "semi_open_file_entry",
      "same_file_penetration",
      "file_penetration_route_certified",
      "rowSpecificAdmissionBurdensByBand",
      "exactValidationScaffoldByBand",
      "declaredProjectionEvidenceKindsByBand"
    ).foreach: token =>
      assert(docs.contains(token), clues(s"S09 scaffold contract token $token must be documented"))

  test("S12 production public admission contract names exact access burden and projection evidence kind"):
    val s12 = StrategyProjectionBandId("S12")
    val evidenceKind = StrategyProjectionEvidenceKind("local_access_superiority_route_certified")
    val extraction = seedExtraction("6k1/8/3P4/3rB3/5P2/8/8/6K1 w - - 0 1")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s12), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s12), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S12"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s12, evidenceKind), true)
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S12"),
      Vector(
        "access_route_requires_same_anchor_weak_outpost_or_diagonal_lane",
        "access_route_requires_support_linked_short_run_restriction_reinforcement",
        "local_access_superiority_requires_same_owner_anchor_route_and_support",
        "same_task_projection_evidence_must_mirror_s12_owner_anchor_route_and_support",
        "s03_diagonal_attack_s10_outpost_s20_mobility_or_optional_strengthening_is_non_admitting",
        "wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S12"),
      Vector(evidenceKind)
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S12")
        .contains("CertificationSupportOnly:MobilityComparison(non_truth_owner)")
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S12")
        .contains("same_task_projection_evidence_must_mirror_s12_owner_anchor_route_and_support_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S12")
        .contains("wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_not_counted")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        extraction,
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Right(false)
    )

  test("S10 production public admission contract admits only exact same-anchor outpost occupation routes through runtime evidence"):
    val s10 = StrategyProjectionBandId("S10")
    val evidenceKind = StrategyProjectionEvidenceKind("outpost_occupation_route_certified")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s10), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s10), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S10"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s10, evidenceKind), true)
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S10"),
      Vector(evidenceKind)
    )
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S10"),
      Vector(
        "same_anchor_outpost_occupation_requires_weak_outpost_and_knight_occupancy",
        "same_anchor_outpost_route_requires_outpost_state_not_residual_weak_square",
        "same_task_projection_evidence_must_mirror_s10_anchor_outpost_square_and_route",
        "s12_access_good_piece_or_non_knight_occupancy_is_non_admitting",
        "wrong_anchor_wrong_route_stale_or_support_only_evidence_is_non_admitting"
      )
    )

  test("S20 production public admission contract admits only exact mobility domination routes through runtime evidence"):
    val s20 = StrategyProjectionBandId("S20")
    val evidenceKind = StrategyProjectionEvidenceKind("mobility_domination_route_certified")
    val extraction = seedExtraction("8/8/1nQ3n1/4p3/3b2K1/8/1k6/8 w - - 0 1")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s20), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s20), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S20"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s20, evidenceKind), true)
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S20"),
      Vector(evidenceKind)
    )
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S20"),
      Vector(
        "domination_route_requires_certified_same_board_mobility_comparison",
        "mobility_plus_restriction_requires_same_owner_short_run_slider_gate_restriction",
        "defender_starvation_requires_same_owner_duty_bound_defender",
        "same_task_projection_evidence_must_mirror_s20_route_owner_anchor_and_support",
        "space_access_liability_or_lower_certification_alone_is_non_admitting",
        "wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_is_non_admitting"
      )
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S20")
        .contains("Certification:MobilityComparison(lower_truth_owner_not_projection_truth)")
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S20")
        .contains("same_board_mobility_certification_and_support_witness_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S20")
        .contains("wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_not_counted")
    )
    val docs =
      Vector(
        "modules/commentary/docs/legacy-pre-semantic-reset/StrategyProjectionBoundaryMatrix.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/CommentaryCoreSSOT.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/Witnesses61.md",
        "modules/commentary/docs/legacy-pre-semantic-reset/ValidationMethodology.md"
      ).map(path => Files.readString(Paths.get(path))).mkString("\n")
    Vector(
      "`S20` | `domination_route`",
      "mobility_plus_restriction",
      "defender_starvation",
      "mobility_domination_route_certified",
      "rowSpecificAdmissionBurdensByBand",
      "exactValidationScaffoldByBand",
      "declaredProjectionEvidenceKindsByBand"
    ).foreach: token =>
      assert(docs.contains(token), clues(s"S20 scaffold contract token $token must be documented"))
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        extraction,
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Right(false)
    )

  test("S06 production public admission contract names exact space-bind burden and evidence kind"):
    val s06 = StrategyProjectionBandId("S06")
    val evidenceKind = StrategyProjectionEvidenceKind("space_bind_restriction_route_certified")
    val extraction = seedExtraction("4k3/8/8/3ppN2/3PP3/7B/8/4K3 w - - 0 1")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s06), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s06), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S06"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s06, evidenceKind), true)
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S06"),
      Vector(
        "space_bind_route_requires_structural_space_claim",
        "space_bind_route_requires_same_anchor_outpost_or_short_run_restriction",
        "space_bind_route_requires_space_bind_restriction_certification",
        "space_bind_certification_requires_same_route_host_link",
        "same_task_projection_evidence_must_mirror_s06_route_anchor_host_and_restriction",
        "s05_center_release_s20_domination_or_mobility_certification_alone_is_non_admitting",
        "wrong_anchor_wrong_sector_stale_or_shortcut_evidence_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S06"),
      Vector(evidenceKind)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        extraction,
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Right(false)
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S06")
        .contains("CertificationSupportOnly:SpaceBindRestrictionCertification(non_truth_owner)")
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S06")
        .contains("same_anchor_host_restriction_and_persistence_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S06")
        .contains("exact_s05_center_release_and_exact_s20_domination_false_rivals_not_counted")
    )

  test("S08 production public admission contract names exact denial burden and evidence kind"):
    val s08 = StrategyProjectionBandId("S08")
    val evidenceKind = StrategyProjectionEvidenceKind("counterplay_denial_route_certified")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s08), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s08), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S08"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s08, evidenceKind), true)
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S08"),
      Vector(
        "denial_route_requires_same_board_certified_initiative_window",
        "denial_route_requires_exact_rival_pawn_push_break_contact_source",
        "projection_evidence_must_bind_rival_source_target_and_denial_route",
        "initiative_window_mobility_restriction_s07_s21_s20_or_wording_only_is_non_admitting",
        "stale_evidence_wrong_source_wrong_target_or_wrong_route_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S08"),
      Vector(evidenceKind)
    )
    assert(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S08")
        .contains("WitnessSupportOnly:pawn_push_break_contact_source(rival_source_not_truth_owner)")
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S08")
        .contains("same_rival_source_target_and_denial_route_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S08")
        .contains("wrong_source_wrong_target_wrong_route_or_stale_evidence_not_counted")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        seedExtraction("r1bqkbnr/p1ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1"),
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Right(false)
    )

  test("S07 production public admission contract names initiative-conversion burden and exact evidence"):
    val s07 = StrategyProjectionBandId("S07")
    val evidenceKind = StrategyProjectionEvidenceKind("initiative_conversion_route_certified")
    val extraction = seedExtraction("r1bqkbnr/2ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s07), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s07), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S07"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s07, evidenceKind), true)
    assertEquals(
      StrategyProjectionAdmission.admits(s07, extraction, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S07"),
      Vector(
        "initiative_conversion_route_requires_opening_development_regime_and_same_owner_development_comparison",
        "initiative_conversion_route_requires_same_owner_certified_initiative_window",
        "projection_evidence_must_bind_same_owner_board_and_initiative_conversion_route",
        "development_only_initiative_only_s08_denial_or_optional_strengthening_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S07"),
      Vector(evidenceKind)
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S07")
        .contains("same_owner_development_comparison_plus_initiative_window_law")
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S07")
        .contains("initiative_window_is_lower_certification_not_projection_truth_owner_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S07")
        .contains("projection_evidence_same_board_route_present")
    )

  test("S05 production public admission contract names center-release burden and exact evidence"):
    val s05 = StrategyProjectionBandId("S05")
    val declaredEvidenceKinds =
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand
        .getOrElse("S05", Vector.empty)

    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand
        .getOrElse("S05", Vector.empty),
      Vector(
        "center_release_route_requires_same_anchor_lever_and_contact_source",
        "center_release_route_requires_recomputed_center_pawn_target",
        "central_axis_continuation_requires_same_source_target_pair",
        "projection_evidence_mirrors_owner_source_target_and_center_release_route",
        "open_center_wording_center_shell_support_object_optional_strengthening_or_adjacent_rival_is_non_admitting"
      )
    )
    assertEquals(
      declaredEvidenceKinds.map(_.value),
      Vector("center_release_route_certified")
    )
    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.map(_.value).contains("S05"), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).contains("S05"), false)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S05"),
      declaredEvidenceKinds
    )
    declaredEvidenceKinds.foreach: kind =>
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(s05, kind),
        true,
        clues(s"S05 evidence kind ${kind.value} must be production public evidence")
      )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        seedExtraction("6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1"),
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Right(false)
    )

  test("S18 production public admission contract names exact carrier burden and evidence without admitting shortcuts"):
    val s18 = StrategyProjectionBandId("S18")
    val extraction = seedExtraction("r1bqkbnr/p1ppp1pp/2n5/8/2P1P3/B1N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val declaredEvidenceKinds =
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand
        .getOrElse("S18", Vector.empty)

    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand
        .getOrElse("S18", Vector.empty),
      Vector(
        "initiative_route_requires_bishop_pair_state_and_same_board_initiative_window",
        "structure_route_requires_bishop_pair_state_and_same_board_mobility_comparison",
        "material_route_requires_bishop_pair_member_material_harvest",
        "projection_evidence_mirrors_conversion_family_targets_and_bishop_pair",
        "relation_only_optional_strengthening_or_adjacent_rival_is_non_admitting"
      )
    )
    assertEquals(
      declaredEvidenceKinds.map(_.value),
      Vector(
        "bishop_pair_initiative_conversion_certified",
        "bishop_pair_structure_conversion_certified",
        "bishop_pair_material_conversion_certified"
      )
    )
    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.map(_.value).contains("S18"), true)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S18"),
      declaredEvidenceKinds
    )
    declaredEvidenceKinds.foreach: kind =>
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(s18, kind),
        true,
        clues(s"S18 evidence kind ${kind.value} must be production-public after explicit admission")
      )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        extraction,
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Right(false)
    )

  test("S15 production public admission contract names same-candidate creation burden and exact evidence"):
    val s15 = StrategyProjectionBandId("S15")
    val extraction = seedExtraction("4k3/8/1p6/2p5/PP6/8/8/4K3 w - - 0 1")
    val declaredEvidenceKinds =
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand
        .getOrElse("S15", Vector.empty)

    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand
        .getOrElse("S15", Vector.empty),
      Vector(
        "creation_route_requires_same_owner_candidate_passer_anchor",
        "s13_route_requires_same_candidate_contact_source_and_recomputed_wing_damage_target",
        "s14_route_requires_same_candidate_contact_source_and_recomputed_chain_base_target",
        "projection_evidence_mirrors_candidate_source_target_and_creation_route",
        "candidate_shell_existing_passer_split_anchor_optional_strengthening_or_adjacent_rival_is_non_admitting"
      )
    )
    assertEquals(
      declaredEvidenceKinds.map(_.value),
      Vector("passer_creation_route_certified")
    )
    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.map(_.value).contains("S15"), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).contains("S15"), false)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S15"),
      declaredEvidenceKinds
    )
    declaredEvidenceKinds.foreach: kind =>
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(s15, kind),
        true,
        clues(s"S15 evidence kind ${kind.value} must be production public evidence")
      )
      StrategyProjectionEvidenceClaim(
        s15,
        kind,
        Color.White,
        WitnessAnchor.PieceSquareAnchor(Square.A4)
      )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        extraction,
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Right(false)
    )

  test("S16 production public admission contract names same-enemy-passer burden and exact evidence"):
    val s16 = StrategyProjectionBandId("S16")
    val extraction = seedExtraction("7k/n5pp/P7/8/8/4K3/3N4/8 w - - 0 1")
    val declaredEvidenceKinds =
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand
        .getOrElse("S16", Vector.empty)

    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand
        .getOrElse("S16", Vector.empty),
      Vector(
        "suppression_route_requires_same_enemy_passed_pawn_anchor",
        "blockade_route_requires_owner_blocker_on_enemy_passer_front_square",
        "restriction_hold_route_requires_same_enemy_passer_blocker_owner_restriction_and_certified_hold",
        "race_route_requires_enemy_passer_and_same_board_non_losing_race_certification",
        "projection_evidence_mirrors_enemy_passer_route_and_route_specific_proof",
        "enemy_passer_blocker_restriction_hold_race_or_adjacent_rival_alone_is_non_admitting"
      )
    )
    assertEquals(
      declaredEvidenceKinds.map(_.value),
      Vector("passer_suppression_route_certified")
    )
    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.map(_.value).contains("S16"), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).contains("S16"), false)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S16"),
      declaredEvidenceKinds
    )
    declaredEvidenceKinds.foreach: kind =>
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(s16, kind),
        true,
        clues(s"S16 evidence kind ${kind.value} must be production public evidence")
      )
      StrategyProjectionEvidenceClaim(
        s16,
        kind,
        Color.Black,
        WitnessAnchor.PieceSquareAnchor(Square.A6)
      )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        extraction,
        StrategyProjectionEvidence.empty,
        Color.Black
      ),
      Right(false)
    )

  test("S19 production public admission contract names exact task burden and evidence without admitting shortcuts"):
    val s19 = StrategyProjectionBandId("S19")
    val extraction = seedExtraction("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1")
    val declaredEvidenceKinds =
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand
        .getOrElse("S19", Vector.empty)

    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand
        .getOrElse("S19", Vector.empty),
      Vector(
        "material_route_requires_canonical_trade_invariant_and_same_move_material_harvest",
        "hold_route_requires_canonical_trade_invariant_and_post_trade_fortress_certification",
        "result_only_material_only_or_adjacent_rival_is_non_admitting"
      )
    )
    assertEquals(
      declaredEvidenceKinds.map(_.value),
      Vector(
        "trade_invariant_material_simplification_certified",
        "trade_invariant_hold_simplification_certified"
      )
    )
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S19"),
      declaredEvidenceKinds
    )
    declaredEvidenceKinds.foreach: kind =>
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(s19, kind),
        true,
        clues(s"S19 evidence kind ${kind.value} must be production public evidence")
      )
    assertEquals(
      StrategyProjectionAdmission.admits(s19, extraction, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )

  test("S22 production public admission contract exposes hold evidence while requiring proof"):
    val s22 = StrategyProjectionBandId("S22")
    val extraction = seedExtraction("7k/6pp/8/8/8/4K3/3N4/8 w - - 0 1")

    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S22"),
      Vector(
        "fortress_route_requires_same_holder_shell_and_certified_fortress_hold",
        "perpetual_route_requires_certified_perpetual_check_hold",
        "support_only_or_deferred_hold_verdict_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S22").map(_.value),
      Vector("fortress_hold_certified", "perpetual_hold_certified")
    )
    StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S22").foreach: kind =>
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(s22, kind),
        true,
        clues(s"S22 evidence kind ${kind.value} must be production public evidence")
      )
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S22"),
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S22")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s22, extraction, StrategyProjectionEvidence.empty, Color.Black),
      Right(false)
    )

  test("S11 production public admission contract names target pressure burden and exact evidence"):
    val s11 = StrategyProjectionBandId("S11")
    val extraction = seedExtraction("4k3/8/8/3p4/3P1N2/8/6B1/4K3 w - - 0 1")
    val declaredEvidenceKinds =
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand
        .getOrElse("S11", Vector.empty)

    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand
        .getOrElse("S11", Vector.empty),
      Vector(
        "target_pressure_route_requires_same_square_weak_pawn_target_and_owner_pressure",
        "fixation_route_requires_current_fixed_pawn_persistence_on_same_target",
        "repeated_pressure_route_requires_two_owner_attackers_on_same_target",
        "projection_evidence_mirrors_weak_pawn_target_pressure_and_persistence",
        "weak_pawn_only_target_swap_adjacent_rival_or_optional_strengthening_is_non_admitting"
      )
    )
    assertEquals(
      declaredEvidenceKinds.map(_.value),
      Vector("weak_pawn_target_pressure_persistence_certified")
    )
    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.map(_.value).contains("S11"), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).contains("S11"), false)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S11"),
      declaredEvidenceKinds
    )
    declaredEvidenceKinds.foreach: kind =>
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(s11, kind),
        true,
        clues(s"S11 evidence kind ${kind.value} must be production public evidence")
      )
      StrategyProjectionEvidenceClaim(
        s11,
        kind,
        Color.White,
        WitnessAnchor.SquareAnchor(Square.D5)
      )
    assertEquals(
      StrategyProjectionAdmission.admits(s11, extraction, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )

  test("S13 production public admission contract names wing-damage burden and exact evidence"):
    val s13 = StrategyProjectionBandId("S13")
    val extraction = seedExtraction("4k3/8/8/8/1pp5/8/P7/4K3 w - - 0 1")
    val declaredEvidenceKinds =
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand
        .getOrElse("S13", Vector.empty)

    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand
        .getOrElse("S13", Vector.empty),
      Vector(
        "wing_damage_route_requires_same_sector_asymmetry_and_same_anchor_contact_source",
        "wing_damage_route_excludes_center_sector_targets",
        "phalanx_edge_route_requires_recomputed_phalanx_edge_target",
        "phalanx_edge_route_excludes_chain_base_targets",
        "burdened_target_route_requires_recomputed_structurally_burdened_target",
        "projection_evidence_mirrors_owner_source_target_sector_and_damage_route",
        "asymmetry_weak_pawn_adjacent_rival_or_optional_strengthening_is_non_admitting"
      )
    )
    assertEquals(declaredEvidenceKinds.map(_.value), Vector("wing_damage_route_certified"))
    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.map(_.value).contains("S13"), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).contains("S13"), false)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S13"),
      declaredEvidenceKinds
    )
    declaredEvidenceKinds.foreach: kind =>
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(s13, kind),
        true,
        clues(s"S13 evidence kind ${kind.value} must be production public evidence")
      )
      StrategyProjectionEvidenceClaim(
        s13,
        kind,
        Color.White,
        WitnessAnchor.SquareAnchor(Square.B4)
      )
    assertEquals(
      StrategyProjectionAdmission.admits(s13, extraction, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )

  test("S14 production public admission contract names chain-base burden and exact evidence"):
    val s14 = StrategyProjectionBandId("S14")
    val extraction = seedExtraction("4k3/8/1p6/2p5/P7/8/8/4K3 w - - 0 1")
    val declaredEvidenceKinds =
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand
        .getOrElse("S14", Vector.empty)

    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand
        .getOrElse("S14", Vector.empty),
      Vector(
        "chain_base_route_requires_same_anchor_lever_and_contact_source",
        "chain_base_route_requires_recomputed_non_center_chain_base_target",
        "base_contact_continuation_requires_same_source_target_pair",
        "projection_evidence_mirrors_owner_source_target_and_chain_base_route",
        "fixed_chain_structural_shell_adjacent_rival_or_optional_strengthening_is_non_admitting"
      )
    )
    assertEquals(declaredEvidenceKinds.map(_.value), Vector("chain_base_contact_route_certified"))
    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.map(_.value).contains("S14"), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).contains("S14"), false)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S14"),
      declaredEvidenceKinds
    )
    declaredEvidenceKinds.foreach: kind =>
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(s14, kind),
        true,
        clues(s"S14 evidence kind ${kind.value} must be production-public after explicit admission")
      )
      StrategyProjectionEvidenceClaim(
        s14,
        kind,
        Color.White,
        WitnessAnchor.SquareAnchor(Square.B6)
      )
    assertEquals(
      StrategyProjectionAdmission.admits(s14, extraction, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )

  test("initiative release counterplay bands require exact runtime evidence after promotion"):
    val extraction = seedExtraction("7k/8/8/8/8/8/8/4K3 w - - 0 1")

    StrategyProjectionCoverageContract.initiativeReleaseCounterplayBandIds
      .foreach: bandId =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          bandId,
          extraction,
          StrategyProjectionEvidence.empty,
          Color.White
        ),
        Right(false)
      )

  test("S21 production public admission contract names exact survival burden and exact evidence"):
    val s21 = StrategyProjectionBandId("S21")
    val evidenceKind = StrategyProjectionEvidenceKind("counterplay_survival_route_certified")
    val extraction = seedExtraction("r1bqkbnr/p2pp1pp/2n5/2p5/4P2P/2N2N2/PP1P1PP1/R2QKB1R w KQkq - 0 1")

    assertEquals(StrategyProjectionScopeContract.startReadyBandIds.contains(s21), true)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S21"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s21, evidenceKind), true)
    assertEquals(StrategyProjectionCoverageContract.coverageOnlyBandIds.contains(s21), false)
    assertEquals(
      StrategyProjectionAdmission.admits(s21, extraction, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdensByBand("S21"),
      Vector(
        "counterplay_survival_route_requires_owner_break_contact_source",
        "counterplay_survival_route_requires_certified_same_board_initiative_window",
        "counterplay_survival_projection_evidence_must_mirror_source_target_route_and_certification",
        "source_only_initiative_only_deferred_or_adjacent_rival_is_non_admitting"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand("S21"),
      Vector(evidenceKind)
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S21")
        .contains("same_task_projection_evidence_must_mirror_s21_source_target_route_and_initiative_law")
    )
    assert(
      StrategyProjectionCoverageContract.helperLawsByBand("S21")
        .contains("initiative_window_is_not_counterplay_survival_truth_owner_law")
    )
    assert(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S21")
        .contains("projection_evidence_mirrors_owner_source_target_route_and_initiative")
    )

  test("row-specific coverage gates stay stable for authored corpus rows"):
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S01"),
      Set(
        "storm_route" -> "same_wing_contact",
        "storm_route" -> "attack_edge_same_king",
        "same_cluster_near_miss" -> "vs_s05",
        "same_cluster_near_miss" -> "vs_s21",
        "shortcut_negative" -> "castling_side_only",
        "shortcut_negative" -> "wing_shell_only",
        "shortcut_negative" -> "optional_strengthening_only",
        "shortcut_negative" -> "support_only_attack_certification",
        "shortcut_negative" -> "support_only_lane_evidence"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S02"),
      Set(
        "attack_concentration_route" -> "direct_piece_concentration",
        "attack_concentration_route" -> "lane_strengthened_concentration",
        "same_cluster_near_miss" -> "vs_s03",
        "same_cluster_near_miss" -> "vs_s04",
        "same_cluster_near_miss" -> "vs_s09",
        "shortcut_negative" -> "king_attack_label_only",
        "shortcut_negative" -> "battery_or_lift_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S03"),
      Set(
        "diagonal_attack_route" -> "king_facing_diagonal_entry",
        "diagonal_attack_route" -> "fragility_linked_diagonal",
        "same_cluster_near_miss" -> "vs_s02",
        "same_cluster_near_miss" -> "vs_s12",
        "shortcut_negative" -> "bishop_pair_only",
        "shortcut_negative" -> "non_king_diagonal_pressure",
        "shortcut_negative" -> "attack_scaffold_only",
        "shortcut_negative" -> "certification_support_only",
        "shortcut_negative" -> "diagonal_certification_without_scaffold",
        "shortcut_negative" -> "scaffold_certification_without_diagonal"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S04"),
      Set(
        "shelter_breach_route" -> "shell_payload_breach",
        "shelter_breach_route" -> "support_break_breach",
        "same_cluster_near_miss" -> "vs_s01",
        "same_cluster_near_miss" -> "vs_s02",
        "same_cluster_near_miss" -> "vs_s03",
        "same_cluster_near_miss" -> "vs_s24",
        "shortcut_negative" -> "king_shelter_wording_only",
        "shortcut_negative" -> "generic_attack_pressure",
        "shortcut_negative" -> "optional_strengthening_only",
        "shortcut_negative" -> "support_only_shell_or_certification",
        "shortcut_negative" -> "forged_wrong_shell_evidence",
        "shortcut_negative" -> "forged_wrong_route_evidence"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S05"),
      Set(
        "center_release_route" -> "center_pawn_target",
        "center_release_route" -> "central_axis_continuation",
        "same_cluster_near_miss" -> "vs_s06",
        "same_cluster_near_miss" -> "vs_s14",
        "same_cluster_near_miss" -> "vs_s21",
        "shortcut_negative" -> "open_center_wording_only",
        "shortcut_negative" -> "center_shell_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S06"),
      Set(
        "restriction_route" -> "outpost_anchor",
        "restriction_route" -> "non_outpost_space_bind",
        "same_cluster_near_miss" -> "vs_s05",
        "same_cluster_near_miss" -> "vs_s20",
        "shortcut_negative" -> "space_wording_only",
        "shortcut_negative" -> "mobility_comparison_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S07"),
      Set(
        "initiative_conversion_route" -> "development_led_window",
        "initiative_conversion_route" -> "move_right_window",
        "same_cluster_near_miss" -> "vs_s08",
        "shortcut_negative" -> "development_lead_only",
        "shortcut_negative" -> "initiative_wording_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S08"),
      Set(
        "denial_route" -> "rival_break_source_suppressed",
        "denial_route" -> "rival_counterplay_source_suppressed",
        "same_cluster_near_miss" -> "vs_s07",
        "same_cluster_near_miss" -> "vs_s20",
        "same_cluster_near_miss" -> "vs_s21",
        "shortcut_negative" -> "no_counterplay_wording_only",
        "shortcut_negative" -> "initiative_window_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S09"),
      Set(
        "penetration_route" -> "open_file_entry",
        "penetration_route" -> "semi_open_file_entry",
        "penetration_route" -> "same_file_penetration",
        "same_cluster_near_miss" -> "vs_s02",
        "same_cluster_near_miss" -> "vs_s23",
        "same_cluster_near_miss" -> "vs_s25",
        "shortcut_negative" -> "file_substrate_only",
        "shortcut_negative" -> "rook_on_file_only",
        "shortcut_negative" -> "support_only_attack_shell",
        "shortcut_negative" -> "certification_support_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S10"),
      Set(
        "occupancy_scope" -> "knight_only",
        "durability_route" -> "same_anchor_eviction_denial",
        "same_cluster_near_miss" -> "vs_s12",
        "shortcut_negative" -> "good_piece_wording_only",
        "shortcut_negative" -> "occupancy_without_durability",
        "shortcut_negative" -> "non_knight_occupancy_without_outpost_role"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S21"),
      Set(
        "counterplay_survival_route" -> "center_source_survives",
        "counterplay_survival_route" -> "far_wing_source_survives",
        "same_cluster_near_miss" -> "vs_s01",
        "same_cluster_near_miss" -> "vs_s05",
        "same_cluster_near_miss" -> "vs_s08",
        "shortcut_negative" -> "break_source_only",
        "shortcut_negative" -> "deferred_initiative_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S17"),
      Set(
        "liability_relief_route" -> "repair_route",
        "liability_relief_route" -> "exchange_relief",
        "same_cluster_near_miss" -> "vs_s18",
        "same_cluster_near_miss" -> "vs_s20",
        "shortcut_negative" -> "bad_piece_wording_only",
        "shortcut_negative" -> "generic_improving_move"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S18"),
      Set(
        "minor_edge_conversion_route" -> "bishop_pair_to_initiative",
        "minor_edge_conversion_route" -> "bishop_pair_to_structure",
        "minor_edge_conversion_route" -> "bishop_pair_to_material",
        "same_cluster_near_miss" -> "vs_s12",
        "same_cluster_near_miss" -> "vs_s17",
        "same_cluster_near_miss" -> "vs_s20",
        "shortcut_negative" -> "bishop_pair_state_only",
        "shortcut_negative" -> "minor_edge_label_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S19"),
      Set(
        "simplification_route" -> "trade_invariant_to_hold",
        "simplification_route" -> "trade_invariant_to_material",
        "same_cluster_near_miss" -> "vs_s22",
        "same_cluster_near_miss" -> "vs_s24",
        "shortcut_negative" -> "trade_wording_only",
        "shortcut_negative" -> "material_reduction_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S20"),
      Set(
        "domination_route" -> "mobility_plus_restriction",
        "domination_route" -> "defender_starvation",
        "same_cluster_near_miss" -> "vs_s06",
        "same_cluster_near_miss" -> "vs_s12",
        "same_cluster_near_miss" -> "vs_s17",
        "shortcut_negative" -> "mobility_edge_only",
        "shortcut_negative" -> "restriction_without_comparison"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S22"),
      Set(
        "hold_route" -> "fortress_draw_hold",
        "hold_route" -> "perpetual_hold",
        "same_cluster_near_miss" -> "vs_s19",
        "same_cluster_near_miss" -> "vs_s23",
        "shortcut_negative" -> "draw_wording_only",
        "shortcut_negative" -> "fortress_shell_only",
        "shortcut_negative" -> "checking_sequence_wording",
        "shortcut_negative" -> "perpetual_deferred"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S23"),
      Set(
        "king_activity_route" -> "same_entry_route",
        "king_activity_route" -> "same_contact_opposition",
        "same_cluster_near_miss" -> "vs_s09",
        "same_cluster_near_miss" -> "vs_s16",
        "same_cluster_near_miss" -> "vs_s22",
        "shortcut_negative" -> "centralization_only",
        "shortcut_negative" -> "king_proximity_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S24"),
      Set(
        "prepared_target_route" -> "same_target_forcing_conversion",
        "same_cluster_near_miss" -> "vs_s04",
        "same_cluster_near_miss" -> "vs_s19",
        "shortcut_negative" -> "raw_tactic_only",
        "shortcut_negative" -> "trade_or_result_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S11"),
      Set(
        "target_pressure_route" -> "same_target_fixation",
        "target_pressure_route" -> "same_target_repeated_pressure",
        "same_cluster_near_miss" -> "vs_s13",
        "same_cluster_near_miss" -> "vs_s14",
        "shortcut_negative" -> "weak_pawn_target_only",
        "shortcut_negative" -> "target_swap_by_prose"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S12"),
      Set(
        "access_route" -> "weak_square_route",
        "access_route" -> "diagonal_lane_route",
        "same_cluster_near_miss" -> "vs_s03",
        "same_cluster_near_miss" -> "vs_s10",
        "same_cluster_near_miss" -> "vs_s20",
        "shortcut_negative" -> "weak_square_wording_only",
        "shortcut_negative" -> "diagonal_wording_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S13"),
      Set(
        "wing_damage_route" -> "phalanx_edge_target",
        "wing_damage_route" -> "structurally_burdened_target",
        "same_cluster_near_miss" -> "vs_s11",
        "same_cluster_near_miss" -> "vs_s14",
        "same_cluster_near_miss" -> "vs_s15",
        "shortcut_negative" -> "sector_asymmetry_only",
        "shortcut_negative" -> "preexisting_weak_pawn_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S14"),
      Set(
        "chain_base_route" -> "chain_base_target",
        "chain_base_route" -> "base_contact_continuation",
        "same_cluster_near_miss" -> "vs_s05",
        "same_cluster_near_miss" -> "vs_s11",
        "same_cluster_near_miss" -> "vs_s13",
        "same_cluster_near_miss" -> "vs_s15",
        "shortcut_negative" -> "fixed_chain_only",
        "shortcut_negative" -> "generic_structural_damage"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S15"),
      Set(
        "creation_route" -> "s13_wing_damage",
        "creation_route" -> "s14_chain_base",
        "same_cluster_near_miss" -> "vs_s13",
        "same_cluster_near_miss" -> "vs_s14",
        "same_cluster_near_miss" -> "vs_s16",
        "shortcut_negative" -> "candidate_passer_only",
        "shortcut_negative" -> "create_passer_shell_only",
        "shortcut_negative" -> "existing_passer_entity_only",
        "shortcut_negative" -> "split_anchor_creation_route"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S16"),
      Set(
        "suppression_route" -> "blockade_hold",
        "suppression_route" -> "restriction_hold",
        "suppression_route" -> "non_losing_race",
        "same_cluster_near_miss" -> "vs_s15",
        "same_cluster_near_miss" -> "vs_s22",
        "same_cluster_near_miss" -> "vs_s23",
        "shortcut_negative" -> "enemy_passer_presence_only",
        "shortcut_negative" -> "blocker_picture_only"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S25"),
      Set(
        "rank_access_route" -> "cross_wing_rank_switch",
        "same_cluster_near_miss" -> "vs_s02",
        "same_cluster_near_miss" -> "vs_s03",
        "same_cluster_near_miss" -> "vs_s04",
        "same_cluster_near_miss" -> "vs_s09",
        "same_cluster_near_miss" -> "vs_s12",
        "same_cluster_near_miss" -> "vs_s23",
        "same_cluster_near_miss" -> "vs_s24",
        "shortcut_negative" -> "piece_on_rank_only",
        "shortcut_negative" -> "generic_rook_lift",
        "shortcut_negative" -> "file_substrate_only",
        "shortcut_negative" -> "back_rank_defense_only",
        "shortcut_negative" -> "king_activity_only",
        "shortcut_negative" -> "tactic_only"
      )
    )

  test("helper laws keep support carriers from becoming truth owners"):
    assertEquals(
      StrategyProjectionCoverageContract.helperLawsByBand.view.mapValues(_.toVector).toMap,
      Map(
        "S05" -> Vector(
          "same_anchor_contact_target_law:center_pawn_target",
          "central_axis_continuation_law",
          "same_task_projection_evidence_must_mirror_s05_source_target_and_center_release_route_law",
          "central_contact_front_initiative_or_file_support_is_not_truth_owner_law"
        ),
        "S06" -> Vector(
          "structural_space_plus_outpost_or_restriction_law",
          "same_anchor_host_restriction_and_persistence_law",
          "mobility_comparison_is_support_not_space_law",
          "space_wording_without_anchor_is_non_admitting_law"
        ),
        "S07" -> Vector(
          "same_owner_development_comparison_plus_initiative_window_law",
          "move_right_window_law",
          "same_task_projection_evidence_must_bind_s07_owner_board_and_route_law",
          "initiative_window_is_lower_certification_not_projection_truth_owner_law"
        ),
        "S08" -> Vector(
          "exact_rival_release_source_denial_law",
          "same_rival_source_target_and_denial_route_law",
          "initiative_window_is_support_not_denial_by_itself",
          "mobility_or_restriction_support_is_not_s08_denial_truth_law"
        ),
        "S09" -> Vector(
          "same_file_lane_penetration_law",
          "same_task_projection_evidence_must_mirror_s09_owner_file_source_entry_and_route_law",
          "rank_access_is_s25_not_s09_law",
          "file_substrate_or_rook_on_file_is_not_penetration_law",
          "attack_shell_or_certification_support_is_not_s09_truth_owner_law"
        ),
        "S10" -> Vector(
          "same_anchor_outpost_square_and_knight_occupancy_law",
          "same_anchor_durability_required_law",
          "same_task_projection_evidence_must_mirror_s10_anchor_outpost_square_and_route_law",
          "weak_outpost_square_state_is_lower_truth_owner_not_projection_truth_law",
          "non_knight_outpost_scope_deferred_law",
          "s12_access_or_good_piece_shortcut_is_non_admitting_law"
        ),
        "S01" -> Vector(
          "same_wing_contact_plus_attack_edge_law",
          "same_defending_king_safety_edge_law",
          "same_task_projection_evidence_must_mirror_s01_source_target_defending_king_and_route_law",
          "s05_center_release_or_s21_counterplay_survival_is_not_s01_storm_law",
          "castling_context_is_not_storm_proof_law"
        ),
        "S02" -> Vector(
          "direct_king_ring_concentration_law",
          "same_task_projection_evidence_must_mirror_s02_owner_defending_king_ring_targets_source_set_and_route_law",
          "lane_strengthens_but_does_not_admit_law",
          "attack_scaffold_is_not_projection_truth_owner_law",
          "certified_king_safety_edge_is_support_not_projection_truth_owner_law",
          "s03_diagonal_s04_shell_s09_file_penetration_are_not_s02_concentration_law"
        ),
        "S03" -> Vector(
          "king_facing_diagonal_entry_law",
          "fragility_linked_diagonal_same_king_law",
          "same_task_projection_evidence_must_mirror_s03_owner_defending_king_diagonal_source_entry_squares_and_route_law",
          "attack_scaffold_is_not_s03_projection_truth_owner_law",
          "comparative_fragility_and_certified_edge_are_support_not_s03_projection_truth_owner_law",
          "s02_concentration_or_s12_local_access_is_not_s03_diagonal_attack_law",
          "bishop_pair_is_not_diagonal_attack_law"
        ),
        "S04" -> Vector(
          "same_king_shell_breach_law",
          "shell_payload_or_support_break_breach_law",
          "king_safety_edge_same_defender_law",
          "same_task_projection_evidence_must_mirror_s04_owner_defending_king_shell_anchor_breach_squares_and_route_law",
          "s01_storm_s02_concentration_s03_diagonal_s24_tactic_is_not_s04_breach_law",
          "object_certification_support_is_not_s04_projection_truth_owner_law"
        ),
        "S11" -> Vector(
          "same_weak_pawn_target_identity_law",
          "pressure_bundle_is_not_weak_pawn_truth_law",
          "fixed_pawn_persistence_is_current_board_carrier_not_certification_truth_law",
          "same_task_projection_evidence_must_mirror_s11_target_pressure_and_persistence_law",
          "target_swap_by_prose_is_non_admitting_law"
        ),
        "S12" -> Vector(
          "local_weak_square_or_diagonal_access_law",
          "restriction_reinforcement_required_law",
          "same_task_projection_evidence_must_mirror_s12_owner_anchor_route_and_support_law",
          "mobility_comparison_is_support_not_s12_access_truth_law",
          "s03_s10_s20_rivals_are_not_s12_access_law",
          "weak_square_or_diagonal_wording_is_not_access_law"
        ),
        "S13" -> Vector(
          "same_sector_asymmetry_plus_contact_source_law",
          "wing_damage_target_role_recomputed_from_exact_board_law",
          "phalanx_edge_target_excludes_chain_base_law",
          "center_sector_damage_is_not_wing_damage_law",
          "same_task_projection_evidence_must_mirror_s13_source_target_sector_and_route_law",
          "asymmetry_or_preexisting_weak_pawn_is_not_damage_law"
        ),
        "S14" -> Vector(
          "same_anchor_chain_base_contact_law",
          "non_center_chain_base_role_recomputed_from_exact_board_law",
          "base_contact_must_bind_same_source_and_target_law",
          "same_task_projection_evidence_must_mirror_s14_source_target_and_route_law",
          "fixed_chain_or_structural_damage_shell_is_not_truth_owner_law"
        ),
        "S15" -> Vector(
          "same_candidate_passer_contact_source_route_law",
          "per_position_creation_route_disjunction_broad_coverage_conjunction_law",
          "candidate_create_shell_existing_passer_or_split_anchor_is_not_creation_law",
          "upper_passer_consequence_is_not_truth_owner_law"
        ),
        "S16" -> Vector(
          "same_enemy_passer_suppression_route_law",
          "certified_hold_or_race_required_law",
          "passer_presence_or_blocker_picture_is_not_suppression_law",
          "hold_or_king_activity_rival_is_not_suppression_law"
        ),
        "S17" -> Vector(
          "same_piece_liability_relief_law",
          "relief_seed_and_evidence_same_piece_law",
          "bad_piece_wording_is_not_relief_law"
        ),
        "S18" -> Vector(
          "same_current_board_bishop_pair_conversion_law",
          "s18_projection_evidence_same_task_same_target_law",
          "conversion_certification_is_lower_truth_owner_law",
          "minor_edge_relation_without_conversion_is_non_admitting_law",
          "weak_square_liability_or_mobility_rival_is_not_conversion_law"
        ),
        "S19" -> Vector(
          "exact_trade_invariant_same_task_certification_law",
          "result_or_material_reduction_only_is_non_admitting_law",
          "fortress_shell_is_support_not_simplification_truth_law"
        ),
        "S20" -> Vector(
          "mobility_comparison_plus_restriction_or_starvation_law",
          "same_board_mobility_certification_and_support_witness_law",
          "same_task_projection_evidence_must_mirror_s20_route_owner_anchor_and_support_law",
          "mobility_comparison_is_lower_certification_not_projection_truth_owner_law",
          "restriction_without_comparison_is_non_admitting_law",
          "space_access_or_liability_rival_is_not_domination_law"
        ),
        "S21" -> Vector(
          "non_center_owner_source_plus_same_board_initiative_law",
          "same_task_projection_evidence_must_mirror_s21_source_target_route_and_initiative_law",
          "initiative_window_is_not_counterplay_survival_truth_owner_law",
          "source_presence_is_not_survival_law",
          "pawn_storm_center_release_or_rival_denial_is_not_counterplay_survival_law"
        ),
        "S22" -> Vector(
          "fortress_or_perpetual_certified_hold_law",
          "same_holder_fortress_shell_and_certification_law",
          "perpetual_cycle_must_be_certified_law",
          "shell_or_draw_wording_is_not_hold_law",
          "support_only_or_deferred_hold_is_not_projection_admission_law",
          "trade_or_king_activity_rival_is_not_hold_law"
        ),
        "S23" -> Vector(
          "same_entry_or_same_contact_king_activity_law",
          "seed_presence_without_evidence_is_non_admitting_law",
          "file_passer_or_hold_rival_is_not_king_activity_law"
        ),
        "S24" -> Vector(
          "same_target_dependency_convergence_forcing_conversion_law",
          "tactic_or_result_only_is_not_prepared_target_law",
          "trade_or_shelter_breach_rival_is_not_prepared_target_law"
        ),
        "S25" -> Vector(
          "same_rank_cross_wing_corridor_law",
          "same_source_entry_and_corridor_kind_evidence_law",
          "rook_lift_file_king_tactic_or_piece_on_rank_is_non_admitting_law"
        )
      )
    )

  test("exact validation scaffold records board-local burdens before corpus rows exist"):
    assertEquals(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand.view.mapValues(_.toVector).toMap,
      Map(
        "S01" -> Vector(
          "non_center_same_wing_owner_contact_source_and_target_present",
          "attack_scaffold_same_defending_king_present",
          "certified_king_safety_edge_same_owner_present",
          "same_task_projection_evidence_must_mirror_s01_source_target_defending_king_and_route",
          "wrong_source_wrong_target_wrong_king_wrong_route_stale_or_support_only_evidence_not_counted",
          "castling_side_context_ignored"
        ),
        "S02" -> Vector(
          "attack_scaffold_same_defending_king_present",
          "certified_king_safety_edge_same_owner_present",
          "direct_piece_concentration_not_file_diagonal_shell_or_open_file_only",
          "same_task_projection_evidence_must_mirror_s02_owner_defending_king_ring_targets_source_set_and_route",
          "wrong_owner_wrong_king_wrong_targets_wrong_sources_wrong_route_stale_or_support_only_evidence_not_counted"
        ),
        "S03" -> Vector(
          "king_theater_diagonal_lane_only_present",
          "attack_scaffold_same_defending_king_present",
          "comparative_king_fragility_certified",
          "certified_king_safety_edge_same_owner_present",
          "same_task_projection_evidence_must_mirror_s03_owner_defending_king_diagonal_source_entry_squares_and_route",
          "attack_scaffold_certification_diagonal_only_stale_or_wrong_task_evidence_not_counted",
          "bishop_pair_state_not_counted"
        ),
        "S04" -> Vector(
          "king_safety_shell_same_defender_present",
          "shelter_breach_from_shell_payload_or_support_break",
          "certified_king_safety_edge_same_defender_present",
          "same_task_projection_evidence_must_mirror_s04_owner_defending_king_shell_anchor_breach_squares_and_route",
          "wrong_owner_wrong_defender_wrong_shell_wrong_route_stale_or_support_only_evidence_not_counted",
          "s01_s02_s03_s24_false_rivals_not_counted",
          "optional_strengthening_not_counted",
          "shell_only_not_counted",
          "shell_payload_and_support_break_use_distinct_declared_support_burdens"
        ),
        "S05" -> Vector(
          "available_lever_trigger_present",
          "pawn_push_break_contact_source_center_target_present",
          "central_axis_continuation_present",
          "projection_evidence_mirrors_same_source_target_and_center_release_route",
          "wrong_source_wrong_target_or_wrong_axis_not_counted",
          "stale_or_adjacent_runtime_evidence_not_counted_before_live_admission",
          "open_center_wording_or_center_shell_only_not_counted"
        ),
        "S06" -> Vector(
        "structural_space_claim_present",
        "outpost_or_short_run_restriction_present",
        "same_anchor_restriction_route_present",
        "same_route_host_link_present",
        "exact_s05_center_release_and_exact_s20_domination_false_rivals_not_counted",
          "structural_space_only_wrong_anchor_or_missing_persistence_not_counted",
          "stale_or_unmatched_s06_evidence_not_counted_in_live_admission",
          "space_or_mobility_wording_only_not_counted"
        ),
        "S07" -> Vector(
          "development_comparison_certified",
          "initiative_window_certified",
          "move_right_window_bound_to_exact_board",
          "projection_evidence_same_board_route_present",
          "development_or_initiative_wording_only_not_counted"
        ),
        "S08" -> Vector(
          "initiative_window_certified",
          "rival_release_source_suppressed_present",
          "denial_route_bound_to_exact_board",
          "projection_evidence_mirrors_rival_source_target_and_denial_route",
          "wrong_source_wrong_target_wrong_route_or_stale_evidence_not_counted",
          "no_counterplay_wording_or_initiative_window_only_not_counted"
        ),
        "S09" -> Vector(
        "file_lane_state_present",
        "open_or_semi_open_file_entry_or_same_file_penetration_present",
        "rank_access_rival_kept_separate",
        "file_substrate_or_rook_on_file_only_not_counted",
        "support_only_attack_shell_or_certification_only_not_counted",
        "wrong_file_wrong_source_wrong_entry_wrong_route_stale_or_support_only_evidence_not_counted"
      ),
        "S10" -> Vector(
          "weak_outpost_square_same_anchor_present",
          "knight_on_outpost_square_same_anchor_present",
          "same_anchor_eviction_denial_present",
          "projection_evidence_mirrors_same_anchor_outpost_square_and_route",
          "wrong_anchor_wrong_route_or_stale_evidence_not_counted",
          "good_piece_non_knight_or_non_durable_occupancy_not_counted"
        ),
        "S11" -> Vector(
          "weak_pawn_target_same_square_present",
          "same_target_pressure_or_repeated_pressure_present",
          "same_target_persistence_proof_present",
          "pressure_without_fixed_persistence_not_counted",
          "target_swap_by_prose_not_counted"
        ),
        "S12" -> Vector(
          "weak_square_or_diagonal_lane_anchor_present",
          "local_restriction_reinforcement_present",
          "local_access_superiority_present",
          "projection_evidence_mirrors_s12_owner_anchor_route_and_support",
          "wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_not_counted",
          "s03_s10_s20_false_rivals_not_counted",
          "weak_square_or_diagonal_wording_only_not_counted"
        ),
        "S13" -> Vector(
          "sector_asymmetry_present",
          "same_sector_lever_and_break_contact_present",
          "wing_sector_damage_route_present",
          "contact_target_role_is_non_chain_base_phalanx_edge_or_structurally_burdened",
          "weak_pawn_or_asymmetry_only_not_counted",
          "stale_or_adjacent_runtime_evidence_not_counted_before_live_admission"
        ),
        "S14" -> Vector(
          "base_contact_lever_and_break_contact_present",
          "contact_target_role_is_non_center_chain_base",
          "base_contact_continuation_same_anchor_present",
          "fixed_chain_or_structural_damage_shell_only_not_counted",
          "stale_or_adjacent_runtime_evidence_not_counted_before_live_admission"
        ),
        "S15" -> Vector(
          "candidate_passer_same_anchor_present",
          "same_candidate_s13_or_s14_creation_route_present",
          "broad_coverage_requires_both_creation_route_buckets",
          "candidate_create_shell_existing_passer_or_split_anchor_only_not_counted",
          "live_passer_creation_evidence_same_candidate_present"
        ),
        "S16" -> Vector(
          "enemy_passed_pawn_same_anchor_present",
          "blockade_or_restriction_support_bound_to_enemy_passer",
          "non_losing_race_or_holding_proof_present",
          "passer_presence_or_blocker_picture_only_not_counted"
        ),
        "S17" -> Vector(
          "same_piece_liability_anchor_seed_present",
          "same_piece_repair_or_exchange_relief_seed_present",
          "liability_relief_certified_evidence_same_piece_present",
          "liability_or_improving_move_only_not_counted"
        ),
        "S18" -> Vector(
          "bishop_pair_state_present",
          "active_bishop_pair_member_present",
          "same_current_board_conversion_certification_present",
          "projection_evidence_mirrors_conversion_family_targets_and_bishop_pair",
          "material_conversion_bishop_pair_member_capture_present",
          "initiative_or_structure_conversion_support_targets_present",
          "bishop_pair_minor_edge_or_optional_strengthening_only_not_counted"
        ),
        "S19" -> Vector(
          "trade_invariant_delta_companion_present",
          "same_task_material_or_hold_certification_present",
          "current_fen_is_trade_task_endpoint",
          "trade_wording_material_reduction_or_result_only_not_counted"
        ),
        "S20" -> Vector(
          "mobility_comparison_certified",
          "restriction_or_duty_bound_defender_present",
          "domination_route_bound_to_exact_board",
          "future_projection_evidence_mirrors_route_owner_anchor_and_support",
          "wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_not_counted",
          "mobility_or_restriction_alone_not_counted"
        ),
        "S21" -> Vector(
          "pawn_push_break_contact_source_present",
          "initiative_window_certified_same_board",
          "non_center_source_to_center_target_or_far_wing_source_survives",
          "projection_evidence_mirrors_owner_source_target_route_and_initiative",
          "break_source_initiative_only_deferred_initiative_or_adjacent_rival_not_counted"
        ),
        "S22" -> Vector(
          "fortress_holding_shell_or_perpetual_cycle_present",
          "certified_hold_verdict_present",
          "fortress_hold_bound_to_same_holder_king",
          "perpetual_hold_bound_to_current_board_cycle",
          "draw_wording_fortress_shell_checking_sequence_or_deferred_hold_not_counted"
        ),
        "S23" -> Vector(
          "king_entry_square_and_access_route_same_entry_present",
          "or_king_opposition_contact_same_square_present",
          "matching_projection_evidence_present",
          "centralization_or_proximity_only_not_counted"
        ),
        "S24" -> Vector(
          "target_resource_dependency_same_target_present",
          "target_attack_convergence_same_target_present",
          "same_target_forcing_and_conversion_evidence_present",
          "raw_tactic_trade_or_result_only_not_counted"
        ),
        "S25" -> Vector(
          "rank_corridor_state_seed_present",
          "same_source_entry_square_and_cross_wing_rank_switch_present",
          "rank_access_consequence_evidence_same_anchor_present",
          "piece_on_rank_rook_lift_file_king_or_tactic_only_not_counted"
        )
      )
    )

  test("lower carrier ownership keeps support, object, delta, and certification roles distinct"):
    assertEquals(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand.view.mapValues(_.toVector).toMap,
      Map(
        "S05" -> Vector(
          "Witness:available_lever_trigger",
          "Witness:pawn_push_break_contact_source(center_pawn_target)",
          "ProjectionValidation:central_axis_continuation",
          "ObjectSupportOnly:CentralContactFront(non_truth_owner)",
          "CertificationSupportOnly:InitiativeWindow(non_truth_owner)",
          "SupportOnly:file_lane_state"
        ),
        "S06" -> Vector(
          "Witness:structural_space_claim",
          "Witness:knight_on_outpost_square|short_run_slider_gate_restriction",
          "ProjectionValidation:restriction_route",
          "CertificationSupportOnly:MobilityComparison|InitiativeWindow",
          "CertificationSupportOnly:SpaceBindRestrictionCertification(non_truth_owner)",
          "SupportOnly:weak_outpost_square_state"
        ),
        "S07" -> Vector(
          "Certification:DevelopmentComparison",
          "Certification:InitiativeWindow",
          "ObjectSupportOnly:OpeningDevelopmentRegime"
        ),
        "S08" -> Vector(
          "CertificationSupportOnly:InitiativeWindow(non_truth_owner)",
          "WitnessSupportOnly:pawn_push_break_contact_source(rival_source_not_truth_owner)",
          "ProjectionValidation:counterplay_denial_route_certified(rival_source_target_route)",
          "SupportOnly:MobilityComparison|short_run_slider_gate_restriction"
        ),
        "S09" -> Vector(
          "Witness:file_lane_state",
          "Witness:rook_on_open_file_state",
          "ProjectionValidation:open_file_entry|semi_open_file_entry|same_file_penetration",
          "ProjectionEvidence:file_penetration_route_certified",
          "CertificationSupportOnly:MaterialHarvest|WinningEndgame",
          "SupportOnly:AttackScaffold|CertifiedKingSafetyEdge"
        ),
        "S10" -> Vector(
          "Witness:weak_outpost_square_state",
          "Witness:knight_on_outpost_square",
          "ProjectionValidation:knight_only_outpost_occupancy|same_anchor_eviction_denial",
          "CertificationSupportOnly:MobilityComparison",
          "SupportOnly:short_run_slider_gate_restriction"
        ),
        "S01" -> Vector(
          "Witness:available_lever_trigger(non_center_attacked_king_wing)",
          "Witness:pawn_push_break_contact_source(same_anchor_non_center_attacked_king_wing)",
          "ObjectSupportOnly:AttackScaffold(non_truth_owner)",
          "CertificationSupportOnly:CertifiedKingSafetyEdge(non_truth_owner)",
          "ProjectionEvidence:king_wing_storm_route_certified",
          "SupportOnly:ComparativeKingFragility|file_lane_state|diagonal_lane_only"
        ),
        "S02" -> Vector(
          "ObjectSupportOnly:AttackScaffold(non_truth_owner)",
          "CertificationSupportOnly:CertifiedKingSafetyEdge(non_truth_owner)",
          "ProjectionValidation:direct_piece_concentration|lane_strengthened_concentration",
          "ProjectionEvidence:king_ring_concentration_route_certified",
          "SupportOnly:file_lane_state|diagonal_lane_only|rook_on_open_file_state"
        ),
    "S03" -> Vector(
      "Witness:diagonal_lane_only(king_theater_linked)",
      "ObjectSupportOnly:AttackScaffold(non_truth_owner)",
      "CertificationSupportOnly:ComparativeKingFragility|CertifiedKingSafetyEdge(non_truth_owner)",
      "ProjectionEvidence:diagonal_king_attack_route_certified",
      "SupportOnly:bishop_pair_state"
    ),
        "S04" -> Vector(
          "ObjectSupportOnly:KingSafetyShell(non_truth_owner)",
          "ProjectionValidation:shell_payload_breach|support_break_breach(same_defender_king)",
          "CertificationSupportOnly:CertifiedKingSafetyEdge(non_truth_owner)",
          "ProjectionEvidence:king_shelter_breach_route_certified",
          "SupportOnly:AttackScaffold|ComparativeKingFragility|same_anchor_contact_target"
        ),
        "S11" -> Vector(
          "Witness:weak_pawn_target_state(same_target)",
          "ProjectionValidation:same_target_fixation|same_target_repeated_pressure",
          "ProjectionValidation:same_target_persistence_proof",
          "WitnessSeed:None",
          "Object:None",
          "Delta:None",
          "CertificationSupportOnly:MobilityComparison|InitiativeWindow(non_truth_owner)"
        ),
        "S12" -> Vector(
          "Witness:weak_outpost_square_state|diagonal_lane_only",
          "Witness:short_run_slider_gate_restriction",
          "ProjectionValidation:local_access_superiority",
          "CertificationSupportOnly:MobilityComparison(non_truth_owner)",
          "SupportOnly:knight_on_outpost_square"
        ),
        "S13" -> Vector(
          "Witness:sector_asymmetry_state",
          "Witness:available_lever_trigger(same_wing_sector)",
          "Witness:pawn_push_break_contact_source(same_anchor_stronger_wing_mass_target)",
          "ProjectionValidation:wing_sector_non_chain_base_phalanx_edge_target|structurally_burdened_target",
          "SupportOnly:weak_pawn_target_state|file_lane_state"
        ),
        "S14" -> Vector(
          "Witness:available_lever_trigger(base_contact)",
          "Witness:pawn_push_break_contact_source(same_anchor_chain_base_target)",
          "ProjectionValidation:non_center_chain_base_target_recomputed|base_contact_continuation",
          "SupportOnly:weak_pawn_target_state|structural_damage_shell|fixed_chain_context"
        ),
        "S15" -> Vector(
          "Root:candidate_passer(same_owner_pawn)",
          "ShellSupportOnly:create_passer(non_truth_owner)",
          "ProjectionValidation:same_candidate_s13_wing_damage|same_candidate_s14_chain_base",
          "ProjectionEvidence:passer_creation_route_certified",
          "CertificationSupportOnly:PromotionRace(non_truth_owner)",
          "SupportOnly:PasserComplex|ConversionFunnel"
        ),
        "S16" -> Vector(
          "Witness:passed_pawn_entity_state(enemy_passer)",
          "ProjectionValidation:blockade_hold|restriction_hold|non_losing_race",
          "WitnessSupportOnly:short_run_slider_gate_restriction",
          "CertificationSupportOnly:PromotionRace|FortressDrawCertification|PerpetualCheckHolding",
          "ObjectSupportOnly:FortressHoldingShell|EndgameRaceScaffold"
        ),
        "S17" -> Vector(
          "WitnessSeed:same_piece_liability_anchor_seed",
          "WitnessSeed:same_piece_repair_route_seed|same_piece_exchange_relief_seed",
          "ProjectionEvidence:liability_relief_certified"
        ),
        "S18" -> Vector(
          "Witness:bishop_pair_state(same_owner_current_board_substrate)",
          "WitnessSeed:None",
          "Object:None",
          "Delta:None",
          "Certification:InitiativeWindow|MobilityComparison|MaterialHarvest(same_current_board_lower_truth_owner)",
          "CertificationSupportOnly:WinningEndgame(future_non_live_s18_carrier)",
          "SupportOnly:favorable_minor_piece_relation|DevelopmentComparison|short_run_slider_gate_restriction"
        ),
        "S19" -> Vector(
          "Delta:TradeInvariant",
          "Certification:FortressDrawCertification|MaterialHarvest",
          "ObjectSupportOnly:FortressHoldingShell"
        ),
        "S20" -> Vector(
          "Witness:short_run_slider_gate_restriction|duty_bound_defender",
          "Certification:MobilityComparison(lower_truth_owner_not_projection_truth)",
          "ProjectionValidation:mobility_plus_restriction|defender_starvation",
          "SupportOnly:structural_space_claim|weak_outpost_square_state|same_piece_liability_anchor_seed"
        ),
        "S21" -> Vector(
          "Witness:pawn_push_break_contact_source",
          "Certification:InitiativeWindow",
          "CertificationSupportOnly:DevelopmentComparison",
          "SupportOnly:available_lever_trigger|center|sector_asymmetry_state|AttackScaffold|EndgameRaceScaffold"
        ),
        "S22" -> Vector(
          "Object:FortressHoldingShell(same_holder_support_only)",
          "Certification:FortressDrawCertification|PerpetualCheckHolding(certified_hold_only)",
          "DeltaSupportOnly:TradeInvariant"
        ),
        "S23" -> Vector(
          "WitnessSeed:king_entry_square_seed",
          "WitnessSeed:king_access_route_seed|king_opposition_contact_seed",
          "ProjectionEvidence:king_entry_conversion_certified|king_opposition_certified",
          "CertificationSupportOnly:WinningEndgame|PromotionRace"
        ),
        "S24" -> Vector(
          "WitnessSeed:target_resource_dependency_seed",
          "WitnessSeed:target_attack_convergence_seed",
          "ProjectionEvidence:same_target_forcing_realization|same_target_conversion_certified"
        ),
        "S25" -> Vector(
          "WitnessSeed:rank_corridor_state_seed",
          "ProjectionEvidence:rank_access_consequence_certified",
          "ProjectionValidation:cross_wing_rank_switch",
          "SupportOnly:AttackScaffold|CertifiedKingSafetyEdge|file_lane_state|diagonal_lane_only|weak_outpost_square_state"
        )
      )
    )

  private def seedExtraction(fen: String) =
    StrategySupportSeedExtractor
      .fromFen(Fen.Full.clean(fen))
      .fold(message => fail(message), identity)
