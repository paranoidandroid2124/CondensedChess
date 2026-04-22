package lila.commentary.validation

import chess.{ Bishop, File, Pawn, Position, Queen, Rank, Rook, Square }
import chess.format.Fen
import chess.variant

import scala.collection.mutable

import lila.commentary.certification.{
  CertificationEvidencePurpose,
  CertificationEvidenceStrength,
  CertificationVerdict
}
import lila.commentary.delta.StrategicDeltaExtractor
import lila.commentary.projection.{
  StrategyProjectionAdmission,
  StrategyProjectionBandId,
  StrategyProjectionCoverageContract,
  StrategyProjectionEvidence,
  StrategyProjectionScopeContract
}
import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.strategic.StrategicObjectExtractor
import lila.commentary.validation.CertificationRuntimeTestSupport.{
  evidenceBundleForFamily,
  extractionFromFen,
  objectExtractionFromFen,
  ownerClaimForFamily
}
import lila.commentary.witness.{ WitnessAnchor, WitnessDescriptorId, WitnessSector, WitnessValue }
import lila.commentary.witness.seed.{ StrategySupportSeedExtractor, StrategySupportSeedScopeContract }
import lila.commentary.witness.u.{ UAttachedExtractor, UExtractionContext, UWitnessExtractor }

class ProjectionExpectationCorpusTest extends munit.FunSuite:

  private val MaxRenewalStates = 512
  private val allProjectionBandIds: Set[String] =
    (1 to 25).map(index => f"S$index%02d").toSet
  private val rows = ProjectionExpectationCorpus.loadAll()
  private val caseTypesByBand = rows.groupMap(_.band)(_.caseType).view.mapValues(_.toSet).toMap

  test("projection rows expose the documented broad-ready row shape"):
    rows.foreach: row =>
      StrategySupportSeedExtractor
        .fromFen(row.normalizedFen)
        .fold(message => fail(s"Row ${row.id} FEN failed: $message"), _ => ())

      row.validatedAnchor
      row.validatedRequiredWitnessIds
      row.validatedRequiredObjectFamilies
      row.validatedRequiredDeltaFamilies
      row.validatedRequiredCertificationFamilies
      row.validatedSupportShellIds
      row.validatedOptionalStrengtheningFamilies
      row.validatedSupportWitnessIds
      row.validatedRivalBands
      row.validatedForbiddenShortcuts
      row.validatedCoveragePair
      row.validatedDeltaCompanion
      assertCoverageLiteralParity(row)
      assertRequiredCurrentWitnesses(row)
      assertRequiredObjectFamilies(row)
      assertRequiredDeltaFamilies(row)
      assertRequiredCertificationFamilies(row)
      assertS18ConversionLink(row)
      assertS19SameTaskCertification(row)
      assertS22PerpetualHoldCycle(row)

  test("projection broad-ready gates track staged waves under wave-7 global closure"):
    assertEquals(
      ProjectionExpectationCorpus.closedLegacyBroadBlockerBands,
      Set("S06", "S10", "S12", "S15")
    )
    assertEquals(
      ProjectionExpectationCorpus.laterBroadReadyCoverageGateBands,
      Set.empty[String]
    )
    assertEquals(
      ProjectionExpectationCorpus.broadReadyCoverageGateBands,
      Set(
        "S05",
        "S06",
        "S07",
        "S08",
        "S01",
        "S02",
        "S03",
        "S04",
        "S09",
        "S10",
        "S11",
        "S12",
        "S13",
        "S14",
        "S15",
        "S16",
        "S17",
        "S18",
        "S19",
        "S20",
        "S21",
        "S22",
        "S23",
        "S24",
        "S25"
      )
    )
    assertEquals(
      ProjectionExpectationCorpus.pendingCoverageFreezeGateBands,
      Set.empty[String]
    )
    assertEquals(
      ProjectionExpectationCorpus.broadReadyCoverageGateBands.intersect(
        ProjectionExpectationCorpus.pendingCoverageFreezeGateBands
      ),
      Set.empty[String]
    )
    assertEquals(
      ProjectionExpectationCorpus.coverageFreezeGateBands,
      ProjectionExpectationCorpus.broadReadyCoverageGateBands ++
        ProjectionExpectationCorpus.pendingCoverageFreezeGateBands
    )
    Set("S01", "S02", "S03", "S04").foreach: band =>
      assertEquals(
        ProjectionExpectationCorpus.requiredCoveragePairsFor(band),
        StrategyProjectionCoverageContract.requiredCoveragePairsFor(band),
        clues(s"$band wave-4 broad-ready gate must stay in parity with coverage contract")
      )
      assertEquals(
        docsCoveragePairsFor(band),
        ProjectionExpectationCorpus.requiredCoveragePairsFor(band),
        clues(s"$band wave-4 broad-ready gate must stay in literal parity with docs")
      )
    assertEquals(
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S05"),
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
      docsCoveragePairsFor("S05"),
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S05")
    )
    assertEquals(
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S07"),
      Set(
        "initiative_conversion_route" -> "development_led_window",
        "initiative_conversion_route" -> "move_right_window",
        "same_cluster_near_miss" -> "vs_s08",
        "shortcut_negative" -> "development_lead_only",
        "shortcut_negative" -> "initiative_wording_only"
      )
    )
    assertEquals(
      docsCoveragePairsFor("S07"),
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S07")
    )
    assertEquals(
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S08"),
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
      docsCoveragePairsFor("S08"),
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S08")
    )
    assertEquals(
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S21"),
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
      docsCoveragePairsFor("S21"),
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S21")
    )
    assertEquals(
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S06"),
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
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S10"),
      Set(
        "occupancy_scope" -> "knight_only",
        "durability_route" -> "same_anchor_eviction_denial",
        "same_cluster_near_miss" -> "vs_s12",
        "shortcut_negative" -> "good_piece_wording_only",
        "shortcut_negative" -> "occupancy_without_durability",
        "shortcut_negative" -> "non_knight_occupancy_without_freeze"
      )
    )
    assertEquals(
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S12"),
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
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S09"),
      Set(
        "penetration_route" -> "open_file_entry",
        "penetration_route" -> "semi_open_file_entry",
        "penetration_route" -> "same_file_penetration",
        "same_cluster_near_miss" -> "vs_s02",
        "same_cluster_near_miss" -> "vs_s23",
        "same_cluster_near_miss" -> "vs_s25",
        "shortcut_negative" -> "file_substrate_only",
        "shortcut_negative" -> "rook_on_file_only"
      )
    )
    assertEquals(
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S20"),
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
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S23"),
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
      ProjectionExpectationCorpus.requiredCoveragePairsFor("S25"),
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
    StrategyProjectionCoverageContract.broadCoverageCandidateBandIds.foreach: bandId =>
      assertEquals(
        ProjectionExpectationCorpus.requiredCoveragePairsFor(bandId.value),
        StrategyProjectionCoverageContract.requiredCoveragePairsFor(bandId.value),
        clues(s"${bandId.value} coverage gates must stay in parity with the wave-2 freeze")
      )
    StrategyProjectionCoverageContract.pawnTargetStructuralDamageCoverageFreezeBandIds.foreach: bandId =>
      assertEquals(
        ProjectionExpectationCorpus.requiredCoveragePairsFor(bandId.value),
        StrategyProjectionCoverageContract.requiredCoveragePairsFor(bandId.value),
        clues(s"${bandId.value} wave-5 broad-ready gate must stay in parity with coverage contract")
      )
      assertEquals(
        docsCoveragePairsFor(bandId.value),
        ProjectionExpectationCorpus.requiredCoveragePairsFor(bandId.value),
        clues(s"${bandId.value} wave-5 broad-ready gate must stay in literal parity with docs")
      )
    StrategyProjectionCoverageContract.passerCreationSuppressionCoverageFreezeBandIds.foreach: bandId =>
      assertEquals(
        ProjectionExpectationCorpus.requiredCoveragePairsFor(bandId.value),
        StrategyProjectionCoverageContract.requiredCoveragePairsFor(bandId.value),
        clues(s"${bandId.value} wave-6 broad-ready gate must stay in parity with coverage contract")
      )
      assertEquals(
        docsCoveragePairsFor(bandId.value),
        ProjectionExpectationCorpus.requiredCoveragePairsFor(bandId.value),
        clues(s"${bandId.value} wave-6 broad-ready gate must stay in literal parity with docs")
      )

  test("global projection coverage freeze is contract-owned for every S01-S25 band"):
    assertEquals(
      StrategyProjectionCoverageContract.coverageGatesByBand.keySet,
      ProjectionExpectationCorpus.broadReadyCoverageGateBands,
      clues("coverage gates must not be split between corpus-local tables and projection contract")
    )
    assertEquals(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand.keySet,
      ProjectionExpectationCorpus.broadReadyCoverageGateBands,
      clues("lower-carrier ownership must be frozen for every broad-ready projection band")
    )
    assertEquals(
      StrategyProjectionCoverageContract.helperLawsByBand.keySet,
      ProjectionExpectationCorpus.broadReadyCoverageGateBands,
      clues("helper/admission laws must be frozen for every broad-ready projection band")
    )
    assertEquals(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand.keySet,
      ProjectionExpectationCorpus.broadReadyCoverageGateBands,
      clues("exact-board validation scaffolds must be frozen for every broad-ready projection band")
    )
    ProjectionExpectationCorpus.broadReadyCoverageGateBands.foreach: band =>
      assertEquals(
        StrategyProjectionCoverageContract.requiredCoveragePairsFor(band),
        ProjectionExpectationCorpus.requiredCoveragePairsFor(band),
        clues(s"$band required coverage pairs must have a single executable owner")
      )
      assertEquals(
        docsCoveragePairsFor(band),
        StrategyProjectionCoverageContract.requiredCoveragePairsFor(band),
        clues(s"$band docs broad-ready row shape must stay in literal parity with the coverage contract")
      )

  test("wave-7 global projection closure rows complete every S01-S25 coverage pair"):
    val globalBands = ProjectionExpectationCorpus.globalClosureBroadReadyCoverageGateBands
    val actualPairsByBand = ProjectionExpectationCorpus.coveragePairsByBand(rows)

    assertEquals(globalBands, allProjectionBandIds)
    assertEquals(ProjectionExpectationCorpus.broadReadyCoverageGateBands, globalBands)
    assertEquals(actualPairsByBand.keySet, globalBands)
    assertEquals(
      ProjectionExpectationCorpus.missingRequiredCoveragePairsByBand(rows),
      Map.empty[String, Set[(String, String)]]
    )
    assertEquals(ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows), globalBands)

    globalBands.foreach: band =>
      assertEquals(
        actualPairsByBand.getOrElse(band, Set.empty),
        StrategyProjectionCoverageContract.requiredCoveragePairsFor(band),
        clues(s"$band global closure rows must exactly match required coverage pairs")
      )

    val coverageOnlyRows =
      rows.filter(row => StrategyProjectionCoverageContract.coverageOnlyBandIds.exists(_.value == row.band))
    coverageOnlyRows.foreach: row =>
      assertEquals(
        row.validatedRequiredSupportSeedIds,
        Vector.empty,
        clues(s"${row.id} coverage-only global closure row must not declare runtime support seeds")
      )
      assertEquals(
        row.evidenceClaimsForRuntime,
        Vector.empty,
        clues(s"${row.id} coverage-only global closure row must not declare live projection evidence")
      )

  test("global closure rejected rows have exact-board negative scaffolds"):
    val globalClosureNegativeBands = Set("S06", "S09", "S10", "S12", "S18", "S19", "S20", "S22", "S25")
    val rejectedRows =
      rows.filter(row => globalClosureNegativeBands.contains(row.band) && row.expectation == "rejected")

    assertEquals(rejectedRows.nonEmpty, true)
    rejectedRows.foreach(assertGlobalRejectedLowerCarrier)

  test("global closure rejected rows do not reuse same-band admitted current boards"):
    val admittedFensByBand =
      rows
        .filter(_.expectation == "admitted")
        .groupMap(_.band)(_.normalizedFen)
        .view
        .mapValues(_.toSet)
        .toMap
    val rejectedRows = rows.filter(_.expectation == "rejected")

    rejectedRows.foreach: row =>
      assert(
        !admittedFensByBand.getOrElse(row.band, Set.empty).contains(row.normalizedFen),
        clues(s"${row.id} rejected row must not reuse an admitted ${row.band} current board")
      )

  test("wave-2 coverage completion gate matches docs, JSONL, and runtime boundary"):
    val wave2Bands =
      StrategyProjectionCoverageContract.broadCoverageCandidateBandIds.map(_.value).toSet
    val expectedPairsByBand =
      Map(
        "S17" -> Set(
          "liability_relief_route" -> "repair_route",
          "liability_relief_route" -> "exchange_relief",
          "same_cluster_near_miss" -> "vs_s18",
          "same_cluster_near_miss" -> "vs_s20",
          "shortcut_negative" -> "bad_piece_wording_only",
          "shortcut_negative" -> "generic_improving_move"
        ),
        "S18" -> Set(
          "minor_edge_conversion_route" -> "bishop_pair_to_initiative",
          "minor_edge_conversion_route" -> "bishop_pair_to_structure",
          "minor_edge_conversion_route" -> "bishop_pair_to_material_or_endgame",
          "same_cluster_near_miss" -> "vs_s12",
          "same_cluster_near_miss" -> "vs_s17",
          "same_cluster_near_miss" -> "vs_s20",
          "shortcut_negative" -> "bishop_pair_state_only",
          "shortcut_negative" -> "minor_edge_label_only"
        ),
        "S19" -> Set(
          "simplification_route" -> "trade_invariant_to_hold",
          "simplification_route" -> "trade_invariant_to_material",
          "same_cluster_near_miss" -> "vs_s22",
          "same_cluster_near_miss" -> "vs_s24",
          "shortcut_negative" -> "trade_wording_only",
          "shortcut_negative" -> "material_reduction_only"
        ),
        "S22" -> Set(
          "hold_route" -> "fortress_draw_hold",
          "hold_route" -> "perpetual_hold",
          "same_cluster_near_miss" -> "vs_s19",
          "same_cluster_near_miss" -> "vs_s23",
          "shortcut_negative" -> "draw_wording_only",
          "shortcut_negative" -> "fortress_shell_only"
        ),
        "S24" -> Set(
          "prepared_target_route" -> "same_target_forcing_conversion",
          "same_cluster_near_miss" -> "vs_s04",
          "same_cluster_near_miss" -> "vs_s19",
          "shortcut_negative" -> "raw_tactic_only",
          "shortcut_negative" -> "trade_or_result_only"
        )
      )
    val actualPairsByBand = ProjectionExpectationCorpus.coveragePairsByBand(rows)

    assertEquals(wave2Bands, expectedPairsByBand.keySet)
    wave2Bands.foreach: band =>
      assertEquals(
        ProjectionExpectationCorpus.requiredCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band requiredCoveragePairsFor must match the frozen docs literal")
      )
      assertEquals(
        docsCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band docs table must match the executable coverage gate")
      )
      assertEquals(
        actualPairsByBand.getOrElse(band, Set.empty),
        expectedPairsByBand(band),
        clues(s"$band JSONL countable rows must fill every required coverage pair exactly")
      )

    assertEquals(
      ProjectionExpectationCorpus.missingRequiredCoveragePairsByBand(rows).filter((band, _) => wave2Bands.contains(band)),
      Map.empty[String, Set[(String, String)]]
    )
    assertEquals(
      ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows).intersect(wave2Bands),
      wave2Bands
    )
    assertEquals(
      StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet.intersect(
        StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
      ),
      Set.empty[String]
    )
    StrategyProjectionCoverageContract.coverageOnlyBandIds.foreach: bandId =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          bandId,
          StrategySupportSeedExtractor
            .fromFen(Fen.Full.clean("7k/8/8/8/8/8/8/4K3 w - - 0 1"))
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          chess.Color.White
        ),
        Left(s"Unsupported projection admission band: ${bandId.value}")
      )

  test("wave-3 initiative release coverage rows complete without live admission expansion"):
    val wave3Bands =
      StrategyProjectionCoverageContract.initiativeReleaseCoverageFreezeBandIds.map(_.value).toSet
    val expectedPairsByBand =
      Map(
        "S05" -> Set(
          "center_release_route" -> "center_pawn_target",
          "center_release_route" -> "central_axis_continuation",
          "same_cluster_near_miss" -> "vs_s06",
          "same_cluster_near_miss" -> "vs_s14",
          "same_cluster_near_miss" -> "vs_s21",
          "shortcut_negative" -> "open_center_wording_only",
          "shortcut_negative" -> "center_shell_only"
        ),
        "S07" -> Set(
          "initiative_conversion_route" -> "development_led_window",
          "initiative_conversion_route" -> "move_right_window",
          "same_cluster_near_miss" -> "vs_s08",
          "shortcut_negative" -> "development_lead_only",
          "shortcut_negative" -> "initiative_wording_only"
        ),
        "S08" -> Set(
          "denial_route" -> "rival_break_source_suppressed",
          "denial_route" -> "rival_counterplay_source_suppressed",
          "same_cluster_near_miss" -> "vs_s07",
          "same_cluster_near_miss" -> "vs_s20",
          "same_cluster_near_miss" -> "vs_s21",
          "shortcut_negative" -> "no_counterplay_wording_only",
          "shortcut_negative" -> "initiative_window_only"
        ),
        "S21" -> Set(
          "counterplay_survival_route" -> "center_source_survives",
          "counterplay_survival_route" -> "far_wing_source_survives",
          "same_cluster_near_miss" -> "vs_s01",
          "same_cluster_near_miss" -> "vs_s05",
          "same_cluster_near_miss" -> "vs_s08",
          "shortcut_negative" -> "break_source_only",
          "shortcut_negative" -> "deferred_initiative_only"
        )
      )
    val actualPairsByBand = ProjectionExpectationCorpus.coveragePairsByBand(rows)

    assertEquals(wave3Bands, expectedPairsByBand.keySet)
    assertEquals(
      ProjectionExpectationCorpus.broadReadyCoverageGateBands.intersect(wave3Bands),
      wave3Bands
    )
    assertEquals(
      ProjectionExpectationCorpus.pendingCoverageFreezeGateBands.intersect(wave3Bands),
      Set.empty[String]
    )
    wave3Bands.foreach: band =>
      assertEquals(
        ProjectionExpectationCorpus.requiredCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band requiredCoveragePairsFor must match the frozen docs literal")
      )
      assertEquals(
        docsCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band docs table must match the executable coverage gate")
      )
      assertEquals(
        actualPairsByBand.getOrElse(band, Set.empty),
        expectedPairsByBand(band),
        clues(s"$band JSONL countable rows must fill every required coverage pair exactly")
      )

    assertEquals(
      ProjectionExpectationCorpus.missingRequiredCoveragePairsByBand(rows).filter((band, _) => wave3Bands.contains(band)),
      Map.empty[String, Set[(String, String)]]
    )
    assertEquals(
      ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows).intersect(wave3Bands),
      wave3Bands
    )
    val wave3CoverageRows =
      rows.filter(row => wave3Bands.contains(row.band) && row.countableCoveragePair.nonEmpty)
    wave3CoverageRows.foreach: row =>
      assertEquals(
        row.validatedRequiredSupportSeedIds,
        Vector.empty,
        clues(s"${row.id} wave-3 broad-ready row must not declare runtime support seeds")
      )
      assertEquals(
        row.evidenceClaimsForRuntime,
        Vector.empty,
        clues(s"${row.id} wave-3 broad-ready row must not declare runtime projection evidence")
      )
      assertEquals(
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          StrategySupportSeedExtractor
            .fromFen(row.normalizedFen)
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          row.validatedOwner
        ),
        Left(s"Unsupported projection admission band: ${row.band}"),
        clues(s"${row.id} wave-3 broad-ready row must stay fail-closed at live admission")
      )
    StrategyProjectionCoverageContract.initiativeReleaseCoverageFreezeBandIds.foreach: bandId =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          bandId,
          StrategySupportSeedExtractor
            .fromFen(Fen.Full.clean("7k/8/8/8/8/8/8/4K3 w - - 0 1"))
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          chess.Color.White
        ),
        Left(s"Unsupported projection admission band: ${bandId.value}")
      )

  test("wave-4 king-attack coverage rows complete without live admission expansion"):
    val wave4Bands =
      StrategyProjectionCoverageContract.kingAttackCoverageFreezeBandIds.map(_.value).toSet
    val expectedPairsByBand =
      Map(
        "S01" -> Set(
          "storm_route" -> "same_wing_contact",
          "storm_route" -> "attack_edge_same_king",
          "same_cluster_near_miss" -> "vs_s05",
          "same_cluster_near_miss" -> "vs_s21",
          "shortcut_negative" -> "castling_side_only",
          "shortcut_negative" -> "wing_shell_only"
        ),
        "S02" -> Set(
          "attack_concentration_route" -> "direct_piece_concentration",
          "attack_concentration_route" -> "lane_strengthened_concentration",
          "same_cluster_near_miss" -> "vs_s03",
          "same_cluster_near_miss" -> "vs_s04",
          "same_cluster_near_miss" -> "vs_s09",
          "shortcut_negative" -> "king_attack_label_only",
          "shortcut_negative" -> "battery_or_lift_only"
        ),
        "S03" -> Set(
          "diagonal_attack_route" -> "king_facing_diagonal_entry",
          "diagonal_attack_route" -> "fragility_linked_diagonal",
          "same_cluster_near_miss" -> "vs_s02",
          "same_cluster_near_miss" -> "vs_s12",
          "shortcut_negative" -> "bishop_pair_only",
          "shortcut_negative" -> "non_king_diagonal_pressure"
        ),
        "S04" -> Set(
          "shelter_breach_route" -> "shell_payload_breach",
          "shelter_breach_route" -> "support_break_breach",
          "same_cluster_near_miss" -> "vs_s01",
          "same_cluster_near_miss" -> "vs_s02",
          "same_cluster_near_miss" -> "vs_s03",
          "same_cluster_near_miss" -> "vs_s24",
          "shortcut_negative" -> "king_shelter_wording_only",
          "shortcut_negative" -> "generic_attack_pressure"
        )
      )
    val actualPairsByBand = ProjectionExpectationCorpus.coveragePairsByBand(rows)

    assertEquals(wave4Bands, expectedPairsByBand.keySet)
    assertEquals(
      ProjectionExpectationCorpus.broadReadyCoverageGateBands.intersect(wave4Bands),
      wave4Bands
    )
    assertEquals(
      ProjectionExpectationCorpus.pendingCoverageFreezeGateBands.intersect(wave4Bands),
      Set.empty[String]
    )
    wave4Bands.foreach: band =>
      assertEquals(
        ProjectionExpectationCorpus.requiredCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band requiredCoveragePairsFor must match the frozen docs literal")
      )
      assertEquals(
        docsCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band docs table must match the executable coverage gate")
      )
      assertEquals(
        actualPairsByBand.getOrElse(band, Set.empty),
        expectedPairsByBand(band),
        clues(s"$band JSONL countable rows must fill every required coverage pair exactly")
      )

    assertEquals(
      ProjectionExpectationCorpus.missingRequiredCoveragePairsByBand(rows).filter((band, _) => wave4Bands.contains(band)),
      Map.empty[String, Set[(String, String)]]
    )
    assertEquals(
      ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows).intersect(wave4Bands),
      wave4Bands
    )
    val wave4CoverageRows =
      rows.filter(row => wave4Bands.contains(row.band) && row.countableCoveragePair.nonEmpty)
    wave4CoverageRows.foreach: row =>
      assertEquals(
        row.validatedRequiredSupportSeedIds,
        Vector.empty,
        clues(s"${row.id} wave-4 broad-ready row must not declare runtime support seeds")
      )
      assertEquals(
        row.evidenceClaimsForRuntime,
        Vector.empty,
        clues(s"${row.id} wave-4 broad-ready row must not declare runtime projection evidence")
      )
      assertEquals(
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          StrategySupportSeedExtractor
            .fromFen(row.normalizedFen)
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          row.validatedOwner
        ),
        Left(s"Unsupported projection admission band: ${row.band}"),
        clues(s"${row.id} wave-4 broad-ready row must stay fail-closed at live admission")
      )
    StrategyProjectionCoverageContract.kingAttackCoverageFreezeBandIds.foreach: bandId =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          bandId,
          StrategySupportSeedExtractor
            .fromFen(Fen.Full.clean("7k/8/8/8/8/8/8/4K3 w - - 0 1"))
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          chess.Color.White
        ),
        Left(s"Unsupported projection admission band: ${bandId.value}")
      )

  test("projection broad-ready coverage presence gate promotes only completed bands"):
    val initialCoveragePairs = Map(
      "S06" -> Set(
        "restriction_route" -> "outpost_anchor",
        "restriction_route" -> "non_outpost_space_bind",
        "same_cluster_near_miss" -> "vs_s05",
        "same_cluster_near_miss" -> "vs_s20",
        "shortcut_negative" -> "space_wording_only",
        "shortcut_negative" -> "mobility_comparison_only"
      ),
      "S12" -> Set(
        "access_route" -> "weak_square_route",
        "access_route" -> "diagonal_lane_route",
        "same_cluster_near_miss" -> "vs_s03",
        "same_cluster_near_miss" -> "vs_s10",
        "same_cluster_near_miss" -> "vs_s20",
        "shortcut_negative" -> "weak_square_wording_only",
        "shortcut_negative" -> "diagonal_wording_only"
      ),
      "S09" -> Set(
        "penetration_route" -> "open_file_entry",
        "penetration_route" -> "semi_open_file_entry",
        "penetration_route" -> "same_file_penetration",
        "same_cluster_near_miss" -> "vs_s02",
        "same_cluster_near_miss" -> "vs_s23",
        "same_cluster_near_miss" -> "vs_s25",
        "shortcut_negative" -> "file_substrate_only",
        "shortcut_negative" -> "rook_on_file_only"
      ),
      "S10" -> Set(
        "occupancy_scope" -> "knight_only",
        "durability_route" -> "same_anchor_eviction_denial",
        "same_cluster_near_miss" -> "vs_s12",
        "shortcut_negative" -> "good_piece_wording_only",
        "shortcut_negative" -> "occupancy_without_durability",
        "shortcut_negative" -> "non_knight_occupancy_without_freeze"
      ),
      "S20" -> Set(
        "domination_route" -> "mobility_plus_restriction",
        "domination_route" -> "defender_starvation",
        "same_cluster_near_miss" -> "vs_s06",
        "same_cluster_near_miss" -> "vs_s12",
        "same_cluster_near_miss" -> "vs_s17",
        "shortcut_negative" -> "mobility_edge_only",
        "shortcut_negative" -> "restriction_without_comparison"
      ),
      "S23" -> Set(
        "king_activity_route" -> "same_entry_route",
        "king_activity_route" -> "same_contact_opposition",
        "same_cluster_near_miss" -> "vs_s09",
        "same_cluster_near_miss" -> "vs_s16",
        "same_cluster_near_miss" -> "vs_s22",
        "shortcut_negative" -> "centralization_only",
        "shortcut_negative" -> "king_proximity_only"
      ),
      "S25" -> Set(
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
    val expectedCoveragePairs =
      initialCoveragePairs ++
        (StrategyProjectionCoverageContract.broadCoverageCandidateBandIds ++
          StrategyProjectionCoverageContract.initiativeReleaseCoverageFreezeBandIds ++
          StrategyProjectionCoverageContract.kingAttackCoverageFreezeBandIds ++
          StrategyProjectionCoverageContract.pawnTargetStructuralDamageCoverageFreezeBandIds ++
          StrategyProjectionCoverageContract.passerCreationSuppressionCoverageFreezeBandIds).map: bandId =>
        bandId.value -> StrategyProjectionCoverageContract.requiredCoveragePairsFor(bandId.value)
    assertEquals(
      ProjectionExpectationCorpus.coveragePairsByBand(rows),
      expectedCoveragePairs
    )
    assertEquals(
      ProjectionExpectationCorpus.missingRequiredCoveragePairsByBand(rows),
      ProjectionExpectationCorpus.broadReadyCoverageGateBands.map: band =>
        band ->
          (ProjectionExpectationCorpus.requiredCoveragePairsFor(band) --
            expectedCoveragePairs.getOrElse(band, Set.empty))
      .toMap
        .filter(_._2.nonEmpty)
    )
    assertEquals(
      ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows),
      ProjectionExpectationCorpus.broadReadyCoverageGateBands
    )
    assertEquals(
      ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows).intersect(
        ProjectionExpectationCorpus.pendingCoverageFreezeGateBands
      ),
      Set.empty[String]
    )
    assertEquals(
      ProjectionExpectationCorpus.missingCoverageFreezePairsByBand(rows).filter((band, _) =>
        ProjectionExpectationCorpus.pendingCoverageFreezeGateBands.contains(band)
      ),
      ProjectionExpectationCorpus.pendingCoverageFreezeGateBands.map: band =>
        band -> ProjectionExpectationCorpus.requiredCoveragePairsFor(band)
      .toMap
    )

  test("initiative release coverage rows are countable only under exact route, rejected near-miss, and rejected shortcut burdens"):
    val exactRouteRows = Vector(
      coverageRow(
        id = "test-s05-center-release-counts",
        band = "S05",
        caseType = "exact",
        expectation = "admitted",
        coverageAxis = "center_release_route",
        coverageBucket = "center_pawn_target"
      ),
      coverageRow(
        id = "test-s07-initiative-route-counts",
        band = "S07",
        caseType = "exact",
        expectation = "admitted",
        coverageAxis = "initiative_conversion_route",
        coverageBucket = "development_led_window"
      ),
      coverageRow(
        id = "test-s08-denial-route-counts",
        band = "S08",
        caseType = "exact",
        expectation = "admitted",
        coverageAxis = "denial_route",
        coverageBucket = "rival_break_source_suppressed"
      ),
      coverageRow(
        id = "test-s21-survival-route-counts",
        band = "S21",
        caseType = "exact",
        expectation = "admitted",
        coverageAxis = "counterplay_survival_route",
        coverageBucket = "center_source_survives"
      )
    )
    val rejectedRouteDoesNotCount =
      coverageRow(
        id = "test-s05-rejected-route-does-not-count",
        band = "S05",
        caseType = "near_miss",
        expectation = "rejected",
        coverageAxis = "center_release_route",
        coverageBucket = "center_pawn_target"
      )
    val nearMissCounts =
      coverageRow(
        id = "test-s21-vs-s08-near-miss-counts",
        band = "S21",
        caseType = "near_miss",
        expectation = "rejected",
        coverageAxis = "same_cluster_near_miss",
        coverageBucket = "vs_s08"
      )
    val shortcutCounts =
      coverageRow(
        id = "test-s08-initiative-only-shortcut-counts",
        band = "S08",
        caseType = "nasty_negative",
        expectation = "rejected",
        coverageAxis = "shortcut_negative",
        coverageBucket = "initiative_window_only"
      )

    assertEquals(
      ProjectionExpectationCorpus.coveragePairsByBand(exactRouteRows :+ rejectedRouteDoesNotCount :+ nearMissCounts :+ shortcutCounts),
      Map(
        "S05" -> Set("center_release_route" -> "center_pawn_target"),
        "S07" -> Set("initiative_conversion_route" -> "development_led_window"),
        "S08" -> Set(
          "denial_route" -> "rival_break_source_suppressed",
          "shortcut_negative" -> "initiative_window_only"
        ),
        "S21" -> Set(
          "counterplay_survival_route" -> "center_source_survives",
          "same_cluster_near_miss" -> "vs_s08"
        )
      )
    )

  test("wave-3 broad-ready coverage rows keep exact-board lower carriers"):
    val wave3Bands =
      StrategyProjectionCoverageContract.initiativeReleaseCoverageFreezeBandIds.map(_.value).toSet
    val wave3Rows = rows.filter(row => wave3Bands.contains(row.band) && row.countableCoveragePair.nonEmpty)

    assertEquals(
      wave3Rows.groupMap(_.band)(_.caseType).view.mapValues(_.toSet).toMap,
      Map(
        "S05" -> Set("exact", "near_miss", "nasty_negative"),
        "S07" -> Set("exact", "near_miss", "nasty_negative"),
        "S08" -> Set("exact", "near_miss", "nasty_negative"),
        "S21" -> Set("exact", "near_miss", "nasty_negative")
      )
    )

    wave3Rows.foreach: row =>
      assertWave3LowerCarrier(row)

  test("wave-4 broad-ready coverage rows keep exact-board lower carriers"):
    val wave4Bands =
      StrategyProjectionCoverageContract.kingAttackCoverageFreezeBandIds.map(_.value).toSet
    val wave4Rows = rows.filter(row => wave4Bands.contains(row.band) && row.countableCoveragePair.nonEmpty)

    assertEquals(wave4Rows.size, 27)
    assertEquals(
      wave4Rows.groupMap(_.band)(_.caseType).view.mapValues(_.toSet).toMap,
      Map(
        "S01" -> Set("exact", "comparative_false_rival", "nasty_negative"),
        "S02" -> Set("exact", "comparative_false_rival", "nasty_negative"),
        "S03" -> Set("exact", "comparative_false_rival", "nasty_negative"),
        "S04" -> Set("exact", "comparative_false_rival", "nasty_negative")
      )
    )

    wave4Rows.foreach: row =>
      assertWave4LowerCarrier(row)

  test("conversion and holding wave 2 coverage rows complete without live admission expansion"):
    val wave2Bands =
      StrategyProjectionCoverageContract.broadCoverageCandidateBandIds.map(_.value).toSet

    assertEquals(
      ProjectionExpectationCorpus.broadReadyCoverageGateBands.intersect(wave2Bands),
      wave2Bands
    )
    wave2Bands.foreach: band =>
      assertEquals(
        ProjectionExpectationCorpus.coveragePairsByBand(rows).getOrElse(band, Set.empty),
        StrategyProjectionCoverageContract.requiredCoveragePairsFor(band),
        clues(s"$band wave-2 coverage pairs")
      )
    assertEquals(
      StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet,
      Set(
        "S01",
        "S02",
        "S03",
        "S04",
        "S05",
        "S06",
        "S07",
        "S08",
        "S09",
        "S10",
        "S11",
        "S12",
        "S13",
        "S14",
        "S15",
        "S16",
        "S18",
        "S19",
        "S20",
        "S21",
        "S22"
      )
    )
    StrategyProjectionCoverageContract.coverageOnlyBandIds.foreach: bandId =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          bandId,
          StrategySupportSeedExtractor
            .fromFen(Fen.Full.clean("7k/8/8/8/8/8/8/4K3 w - - 0 1"))
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          chess.Color.White
        ),
        Left(s"Unsupported projection admission band: ${bandId.value}")
      )
    rows
      .filter(row =>
        row.band == "S19" &&
          row.coverageAxis.contains("simplification_route") &&
          row.expectation == "admitted"
      )
      .foreach: row =>
        assert(
          row.validatedRequiredDeltaFamilies.contains("TradeInvariant"),
          clues(s"${row.id} must prove exact TradeInvariant rather than simplification prose")
        )
        assert(
          row.validatedRequiredCertificationFamilies.toSet
            .intersect(Set("FortressDrawCertification", "MaterialHarvest"))
            .nonEmpty,
          clues(s"${row.id} must require a same-task lower certification rather than optional prose metadata")
        )

  test("wave-5 pawn-target structural damage coverage rows complete without live admission expansion"):
    val wave5Bands =
      StrategyProjectionCoverageContract.pawnTargetStructuralDamageCoverageFreezeBandIds.map(_.value).toSet
    val expectedPairsByBand =
      Map(
        "S11" -> Set(
          "target_pressure_route" -> "same_target_fixation",
          "target_pressure_route" -> "same_target_repeated_pressure",
          "same_cluster_near_miss" -> "vs_s13",
          "same_cluster_near_miss" -> "vs_s14",
          "shortcut_negative" -> "weak_pawn_target_only",
          "shortcut_negative" -> "target_swap_by_prose"
        ),
        "S13" -> Set(
          "wing_damage_route" -> "phalanx_edge_target",
          "wing_damage_route" -> "structurally_burdened_target",
          "same_cluster_near_miss" -> "vs_s11",
          "same_cluster_near_miss" -> "vs_s14",
          "same_cluster_near_miss" -> "vs_s15",
          "shortcut_negative" -> "sector_asymmetry_only",
          "shortcut_negative" -> "preexisting_weak_pawn_only"
        ),
        "S14" -> Set(
          "chain_base_route" -> "chain_base_target",
          "chain_base_route" -> "base_contact_continuation",
          "same_cluster_near_miss" -> "vs_s05",
          "same_cluster_near_miss" -> "vs_s11",
          "same_cluster_near_miss" -> "vs_s13",
          "shortcut_negative" -> "fixed_chain_only",
          "shortcut_negative" -> "generic_structural_damage"
        )
      )

    val actualPairsByBand = ProjectionExpectationCorpus.coveragePairsByBand(rows)

    assertEquals(wave5Bands, expectedPairsByBand.keySet)
    assertEquals(ProjectionExpectationCorpus.pendingCoverageFreezeGateBands.intersect(wave5Bands), Set.empty[String])
    assertEquals(ProjectionExpectationCorpus.broadReadyCoverageGateBands.intersect(wave5Bands), wave5Bands)
    wave5Bands.foreach: band =>
      assertEquals(
        ProjectionExpectationCorpus.requiredCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band requiredCoveragePairsFor must match the frozen docs literal")
      )
      assertEquals(
        docsCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band docs table must match the executable coverage gate")
      )
      assertEquals(
        actualPairsByBand.getOrElse(band, Set.empty),
        expectedPairsByBand(band),
        clues(s"$band JSONL countable rows must fill every required coverage pair exactly")
      )
      assertEquals(
        StrategyProjectionAdmission.admits(
          StrategyProjectionBandId(band),
          StrategySupportSeedExtractor
            .fromFen(Fen.Full.clean("7k/8/8/8/8/8/8/4K3 w - - 0 1"))
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          chess.Color.White
        ),
        Left(s"Unsupported projection admission band: $band"),
        clues(s"$band broad-ready coverage must not expand live projection admission")
      )

    assertEquals(
      ProjectionExpectationCorpus.missingCoverageFreezePairsByBand(rows).filter((band, _) => wave5Bands.contains(band)),
      Map.empty[String, Set[(String, String)]]
    )
    assertEquals(
      ProjectionExpectationCorpus.missingRequiredCoveragePairsByBand(rows).filter((band, _) => wave5Bands.contains(band)),
      Map.empty[String, Set[(String, String)]]
    )
    assertEquals(
      ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows).intersect(wave5Bands),
      wave5Bands
    )

    val wave5CoverageRows =
      rows.filter(row => wave5Bands.contains(row.band) && row.countableCoveragePair.nonEmpty)
    assertEquals(wave5CoverageRows.size, expectedPairsByBand.values.map(_.size).sum)
    wave5CoverageRows.foreach: row =>
      assertEquals(
        row.validatedRequiredSupportSeedIds,
        Vector.empty,
        clues(s"${row.id} wave-5 broad-ready row must not declare runtime support seeds")
      )
      assertEquals(
        row.evidenceClaimsForRuntime,
        Vector.empty,
        clues(s"${row.id} wave-5 broad-ready row must not declare runtime projection evidence")
      )
      assertEquals(
        row.validatedRequiredObjectFamilies,
        Vector.empty,
        clues(s"${row.id} wave-5 broad-ready row must not make Object families projection truth owners")
      )
      assertEquals(
        row.validatedRequiredDeltaFamilies,
        Vector.empty,
        clues(s"${row.id} wave-5 broad-ready row must not make Delta families projection truth owners")
      )
      assertEquals(
        row.validatedRequiredCertificationFamilies,
        Vector.empty,
        clues(s"${row.id} wave-5 broad-ready row must not make Certification families projection truth owners")
      )
      assertEquals(
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          StrategySupportSeedExtractor
            .fromFen(row.normalizedFen)
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          row.validatedOwner
        ),
        Left(s"Unsupported projection admission band: ${row.band}"),
        clues(s"${row.id} wave-5 broad-ready row must stay fail-closed at live admission")
      )

  test("wave-5 pawn-target structural damage rows keep exact-board lower carriers"):
    val wave5Bands =
      StrategyProjectionCoverageContract.pawnTargetStructuralDamageCoverageFreezeBandIds.map(_.value).toSet
    val wave5CoverageRows =
      rows.filter(row => wave5Bands.contains(row.band) && row.countableCoveragePair.nonEmpty)

    assertEquals(wave5CoverageRows.size, 20)
    wave5CoverageRows.foreach(assertWave5LowerCarrier)

    assertEquals(
      wave5CoverageRows.groupMap(_.band)(_.caseType).view.mapValues(_.toSet).toMap,
      Map(
        "S11" -> Set("exact", "comparative_false_rival", "nasty_negative"),
        "S13" -> Set("exact", "comparative_false_rival", "nasty_negative"),
        "S14" -> Set("exact", "comparative_false_rival", "nasty_negative")
      )
    )

  test("wave-5 S11 exact validation rejects same-target pressure without persistence proof"):
    val noPersistence =
      coverageRow(
        id = "test-s11-pressure-without-persistence-rejected",
        band = "S11",
        caseType = "exact",
        expectation = "admitted",
        coverageAxis = "target_pressure_route",
        coverageBucket = "same_target_fixation"
      ).copy(
        fen = "4k3/8/8/3p4/8/8/6B1/4K3 w - - 0 1",
        anchor = "square:d5",
        requiredWitnessIds = List("weak_pawn_target_state"),
        supportWitnessIds = List("weak_pawn_target_state"),
        rivalBands = List("S13", "S14"),
        forbiddenShortcuts = List("weak_pawn_target_only", "target_swap_by_prose"),
        admissionPath = "same_target_fixation",
        notes = Some("The target is isolated and attacked, but it is not fixed on the same square.")
      )

    intercept[munit.FailException](assertWave5LowerCarrier(noPersistence))

  test("wave-6 passer creation suppression coverage rows complete without live admission expansion"):
    val wave6Bands =
      StrategyProjectionCoverageContract.passerCreationSuppressionCoverageFreezeBandIds.map(_.value).toSet
    val expectedPairsByBand =
      Map(
        "S15" -> Set(
          "creation_route" -> "s13_wing_damage",
          "creation_route" -> "s14_chain_base",
          "same_cluster_near_miss" -> "vs_s13",
          "same_cluster_near_miss" -> "vs_s14",
          "same_cluster_near_miss" -> "vs_s16",
          "shortcut_negative" -> "candidate_passer_only",
          "shortcut_negative" -> "existing_passer_entity_only"
        ),
        "S16" -> Set(
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
    val actualPairsByBand = ProjectionExpectationCorpus.coveragePairsByBand(rows)

    assertEquals(wave6Bands, expectedPairsByBand.keySet)
    assertEquals(ProjectionExpectationCorpus.pendingCoverageFreezeGateBands.intersect(wave6Bands), Set.empty[String])
    assertEquals(ProjectionExpectationCorpus.broadReadyCoverageGateBands.intersect(wave6Bands), wave6Bands)
    wave6Bands.foreach: band =>
      assertEquals(
        ProjectionExpectationCorpus.requiredCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band requiredCoveragePairsFor must match the frozen docs literal")
      )
      assertEquals(
        docsCoveragePairsFor(band),
        expectedPairsByBand(band),
        clues(s"$band docs table must match the executable coverage gate")
      )
      assertEquals(
        actualPairsByBand.getOrElse(band, Set.empty),
        expectedPairsByBand(band),
        clues(s"$band JSONL countable rows must fill every required coverage pair exactly")
      )
      assertEquals(
        StrategyProjectionAdmission.admits(
          StrategyProjectionBandId(band),
          StrategySupportSeedExtractor
            .fromFen(Fen.Full.clean("7k/8/8/8/8/8/8/4K3 w - - 0 1"))
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          chess.Color.White
        ),
        Left(s"Unsupported projection admission band: $band"),
        clues(s"$band broad-ready coverage must not expand live projection admission")
      )

    assertEquals(
      ProjectionExpectationCorpus.missingCoverageFreezePairsByBand(rows).filter((band, _) => wave6Bands.contains(band)),
      Map.empty[String, Set[(String, String)]]
    )
    assertEquals(
      ProjectionExpectationCorpus.missingRequiredCoveragePairsByBand(rows).filter((band, _) => wave6Bands.contains(band)),
      Map.empty[String, Set[(String, String)]]
    )
    assertEquals(
      ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows).intersect(wave6Bands),
      wave6Bands
    )

    val wave6CoverageRows =
      rows.filter(row => wave6Bands.contains(row.band) && row.countableCoveragePair.nonEmpty)
    assertEquals(wave6CoverageRows.size, expectedPairsByBand.values.map(_.size).sum)
    wave6CoverageRows.foreach: row =>
      assertEquals(
        row.validatedRequiredSupportSeedIds,
        Vector.empty,
        clues(s"${row.id} wave-6 broad-ready row must not declare runtime support seeds")
      )
      assertEquals(
        row.evidenceClaimsForRuntime,
        Vector.empty,
        clues(s"${row.id} wave-6 broad-ready row must not declare runtime projection evidence")
      )
      val permittedLowerObjectSupport =
        row.band == "S16" &&
          row.coverageAxis.contains("same_cluster_near_miss") &&
          row.coverageBucket.contains("vs_s22") &&
          row.expectation == "rejected" &&
          row.validatedRequiredObjectFamilies == Vector("FortressHoldingShell")
      assert(
        row.validatedRequiredObjectFamilies.isEmpty || permittedLowerObjectSupport,
        clues(
          s"${row.id} wave-6 broad-ready row must keep Object families as bounded false-rival support only"
        )
      )
      assertEquals(
        row.validatedRequiredDeltaFamilies,
        Vector.empty,
        clues(s"${row.id} wave-6 broad-ready row must not make Delta families projection truth owners")
      )
      assertEquals(
        row.validatedRequiredCertificationFamilies,
        Vector.empty,
        clues(s"${row.id} wave-6 broad-ready row must not make Certification families projection truth owners")
      )
      assertEquals(
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          StrategySupportSeedExtractor
            .fromFen(row.normalizedFen)
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          row.validatedOwner
        ),
        Left(s"Unsupported projection admission band: ${row.band}"),
        clues(s"${row.id} wave-6 broad-ready row must stay fail-closed at live admission")
      )

  test("wave-6 passer creation suppression rows keep exact-board lower carriers"):
    val wave6Bands =
      StrategyProjectionCoverageContract.passerCreationSuppressionCoverageFreezeBandIds.map(_.value).toSet
    val wave6CoverageRows =
      rows.filter(row => wave6Bands.contains(row.band) && row.countableCoveragePair.nonEmpty)

    assertEquals(wave6CoverageRows.size, 15)
    wave6CoverageRows.foreach(assertWave6LowerCarrier)

    assertEquals(
      wave6CoverageRows.groupMap(_.band)(_.caseType).view.mapValues(_.toSet).toMap,
      Map(
        "S15" -> Set("exact", "comparative_false_rival", "nasty_negative"),
        "S16" -> Set("exact", "comparative_false_rival", "nasty_negative")
      )
    )

  test("projection broad-ready coverage presence counts only axis-appropriate row burdens"):
    val admittedRoute =
      coverageRow(
        id = "test-s10-knight-scope-counts",
        band = "S10",
        caseType = "exact",
        expectation = "admitted",
        coverageAxis = "occupancy_scope",
        coverageBucket = "knight_only"
      )
    val rejectedRoute =
      coverageRow(
        id = "test-s10-knight-scope-rejected-does-not-count",
        band = "S10",
        caseType = "near_miss",
        expectation = "rejected",
        coverageAxis = "occupancy_scope",
        coverageBucket = "knight_only"
      )
    val nearMiss =
      coverageRow(
        id = "test-s10-vs-s12-near-miss-counts",
        band = "S10",
        caseType = "near_miss",
        expectation = "rejected",
        coverageAxis = "same_cluster_near_miss",
        coverageBucket = "vs_s12"
      )
    val shortcut =
      coverageRow(
        id = "test-s10-shortcut-negative-counts",
        band = "S10",
        caseType = "nasty_negative",
        expectation = "rejected",
        coverageAxis = "shortcut_negative",
        coverageBucket = "good_piece_wording_only"
      )

    assertEquals(
      ProjectionExpectationCorpus.coveragePairsByBand(Vector(admittedRoute, rejectedRoute, nearMiss, shortcut)),
      Map(
        "S10" -> Set(
          "occupancy_scope" -> "knight_only",
          "same_cluster_near_miss" -> "vs_s12",
          "shortcut_negative" -> "good_piece_wording_only"
        )
      )
    )

  test("projection start-ready scaffold covers exact, near_miss, nasty_negative, and comparative_false_rival for S17/S23/S24/S25"):
    StrategyProjectionScopeContract.startReadyBandIds.map(_.value).foreach: band =>
      val actualCaseTypes = caseTypesByBand.getOrElse(band, Set.empty)
      assertEquals(
        ProjectionExpectationCorpus.requiredStartReadyCaseTypes.subsetOf(actualCaseTypes),
        true,
        clues(s"$band case types = ${actualCaseTypes.toVector.sorted.mkString(",")}")
      )

  rows.foreach: row =>
    test(s"projection start-ready row parses and matches admission scaffold for ${row.id}"):
      row.validatedCaseType
      row.validatedExpectation
      row.validatedBand
      row.validatedOwner
      row.validatedAdmissionPath
      row.validatedRequiredSupportSeedIds
      row.validatedEvidenceKinds
      row.validatedRivalBands

      if StrategyProjectionScopeContract.isStartReadyBandId(row.validatedBand) then
        val extraction =
          StrategySupportSeedExtractor
            .fromFen(Fen.Full.clean(row.fen))
            .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity)
        val evidence =
          StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime)
        val admitted =
          StrategyProjectionAdmission
            .admits(row.validatedBand, extraction, evidence, row.validatedOwner)
            .fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)

        assertEquals(
          admitted,
          row.expectation == "admitted",
          clues(s"${row.id} projection admission mismatch")
        )

        if row.expectation == "admitted" then
          row.validatedRequiredSupportSeedIds.foreach: seedId =>
            assert(
              extraction.seeds.forSeedId(seedId).exists(_.color.contains(row.validatedOwner)),
              clues(s"${row.id} missing required support seed ${seedId.value}")
            )

  private def coverageRow(
      id: String,
      band: String,
      caseType: String,
      expectation: String,
      coverageAxis: String,
      coverageBucket: String
  ): ProjectionExpectationCorpus.Row =
    ProjectionExpectationCorpus.Row(
      id = id,
      caseType = caseType,
      fen = "7k/8/8/8/8/8/8/4K3 w - - 0 1",
      fenBefore = None,
      playedMove = None,
      fenAfter = None,
      expectation = expectation,
      band = band,
      owner = "white",
      anchor = "board",
      requiredWitnessIds = Nil,
      requiredObjectFamilies = Nil,
      requiredDeltaFamilies = Nil,
      requiredCertificationFamilies = Nil,
      supportShellIds = Nil,
      optionalStrengtheningFamilies = Nil,
      supportWitnessIds = Nil,
      rivalBands = Nil,
      forbiddenShortcuts = Nil,
      coverageAxis = Some(coverageAxis),
      coverageBucket = Some(coverageBucket),
      admissionPath = "unspecified",
      requiredSupportSeedIds = Nil,
      evidenceClaims = Nil,
      notes = None
    )

  private def assertRequiredCurrentWitnesses(row: ProjectionExpectationCorpus.Row): Unit =
    if row.countableCoveragePair.nonEmpty &&
      row.expectation == "admitted" &&
      !StrategyProjectionScopeContract.isStartReadyBandId(row.validatedBand)
    then
      val requiredWitnessIds = row.validatedRequiredWitnessIds
      assert(
        requiredWitnessIds.nonEmpty ||
          row.validatedRequiredObjectFamilies.nonEmpty ||
          row.validatedRequiredDeltaFamilies.nonEmpty ||
          row.validatedRequiredCertificationFamilies.nonEmpty,
        clues(s"${row.id} broad exact/admitted coverage row must declare at least one lower carrier")
      )
      if requiredWitnessIds.nonEmpty then
        val primary =
          UWitnessExtractor
            .fromFen(row.normalizedFen)
            .fold(message => fail(s"Row ${row.id} U witness extraction failed: $message"), identity)
        val attached =
          UAttachedExtractor
            .fromFen(row.normalizedFen)
            .fold(message => fail(s"Row ${row.id} U-attached extraction failed: $message"), identity)
        val allWitnesses = primary.witnesses.all ++ attached.witnesses.all

        requiredWitnessIds.foreach: witnessId =>
          val descriptorId = WitnessDescriptorId(witnessId)
          assert(
            allWitnesses.exists(witness =>
              witness.descriptorId == descriptorId &&
                witnessColorMatchesRowRequirement(row, witnessId, witness.color)
            ),
            clues(s"${row.id} missing required current witness $witnessId for ${row.owner}")
          )

  private def witnessColorMatchesRowRequirement(
      row: ProjectionExpectationCorpus.Row,
      witnessId: String,
      color: Option[chess.Color]
  ): Boolean =
    if (row.band == "S16" || (row.band == "S15" && row.coverageBucket.contains("vs_s16"))) &&
      witnessId == "passed_pawn_entity_state"
    then
      color.contains(!row.validatedOwner)
    else color.forall(_ == row.validatedOwner)

  private def assertWave3LowerCarrier(row: ProjectionExpectationCorpus.Row): Unit =
    val hasDeclaredCarrier =
      row.validatedRequiredWitnessIds.nonEmpty ||
        row.validatedRequiredObjectFamilies.nonEmpty ||
        row.validatedRequiredCertificationFamilies.nonEmpty ||
        row.validatedSupportWitnessIds.nonEmpty
    assert(hasDeclaredCarrier, clues(s"${row.id} must declare at least one exact lower carrier"))

    val allWitnesses = currentWitnesses(row)
    row.validatedRequiredWitnessIds.foreach: witnessId =>
      val descriptorId = WitnessDescriptorId(witnessId)
      assert(
        allWitnesses.exists(witness =>
          witness.descriptorId == descriptorId &&
            witness.color.forall(_ == row.validatedOwner)
        ),
        clues(s"${row.id} missing required owner witness $witnessId for ${row.owner}")
      )
    row.validatedSupportWitnessIds.foreach: witnessId =>
      val descriptorId = WitnessDescriptorId(witnessId)
      assert(
        allWitnesses.exists(_.descriptorId == descriptorId),
        clues(s"${row.id} missing support witness $witnessId")
      )
    if row.validatedRequiredObjectFamilies.nonEmpty then
      val current = objectExtractionFromFen(row.fen)
      row.validatedRequiredObjectFamilies.foreach: familyId =>
        assert(
          current.objects.forFamilyId(familyId).exists(obj => obj.color.forall(_ == row.validatedOwner)),
          clues(s"${row.id} missing required current object family $familyId for ${row.owner}")
        )
    row.validatedRequiredCertificationFamilies.foreach: familyId =>
      assertEquals(
        certifiedClaimForRow(row, familyId).verdict,
        CertificationVerdict.Certified,
        clues(s"${row.id} lower certification $familyId must be certified on the exact board")
      )

    row.countableCoveragePair.foreach:
      case ("center_release_route", _) =>
        assert(
          ownerBreakContactTargetSquares(row).exists(isCenterFile),
          clues(s"${row.id} S05 route must expose a center-file pawn target")
        )
      case ("initiative_conversion_route", _) =>
        assertEquals(
          row.validatedRequiredCertificationFamilies.toSet,
          Set("DevelopmentComparison", "InitiativeWindow"),
          clues(s"${row.id} S07 route must require both DevelopmentComparison and InitiativeWindow")
        )
      case ("denial_route", _) =>
        assert(
          row.validatedRequiredCertificationFamilies.contains("InitiativeWindow"),
          clues(s"${row.id} S08 denial must keep InitiativeWindow as certified support")
        )
        assert(
          row.validatedSupportWitnessIds.contains("pawn_push_break_contact_source"),
          clues(s"${row.id} S08 denial must declare the exact rival break source as support")
        )
        assert(
          allWitnesses.exists(witness =>
            witness.descriptorId == WitnessDescriptorId("pawn_push_break_contact_source") &&
              witness.color.contains(!row.validatedOwner)
          ),
          clues(s"${row.id} S08 denial must be tied to an exact rival break source")
        )
      case ("counterplay_survival_route", "center_source_survives") =>
        assert(
          row.validatedRequiredWitnessIds.contains("pawn_push_break_contact_source") &&
            row.validatedRequiredCertificationFamilies.contains("InitiativeWindow"),
          clues(s"${row.id} S21 center survival must require source plus InitiativeWindow")
        )
        assert(
          ownerBreakContactTargetSquares(row).exists(isCenterFile),
          clues(s"${row.id} S21 center survival must expose a center-file source target")
        )
      case ("counterplay_survival_route", "far_wing_source_survives") =>
        assert(
          row.validatedRequiredWitnessIds.contains("pawn_push_break_contact_source") &&
            row.validatedRequiredCertificationFamilies.contains("InitiativeWindow"),
          clues(s"${row.id} S21 far-wing survival must require source plus InitiativeWindow")
        )
        assert(
          ownerBreakContactTargetSquares(row).exists(square => !isCenterFile(square)),
          clues(s"${row.id} S21 far-wing survival must expose a non-center source target")
        )
      case ("same_cluster_near_miss", _) =>
        assertEquals(
          row.caseType,
          "near_miss",
          clues(s"${row.id} wave-3 same-cluster row must be an explicit near-miss rejection")
        )
        assertEquals(
          hasWave3OwnCarrier(row),
          false,
          clues(s"${row.id} wave-3 false rival must not satisfy its own target-band carrier")
        )
      case ("shortcut_negative", _) =>
        assertEquals(
          hasWave3OwnCarrier(row),
          false,
          clues(s"${row.id} wave-3 shortcut negative must not satisfy its own target-band carrier")
        )
      case _ => ()

  private def assertWave4LowerCarrier(row: ProjectionExpectationCorpus.Row): Unit =
    val hasDeclaredCarrier =
      row.validatedRequiredWitnessIds.nonEmpty ||
        row.validatedRequiredObjectFamilies.nonEmpty ||
        row.validatedRequiredCertificationFamilies.nonEmpty ||
        row.validatedOptionalStrengtheningFamilies.nonEmpty ||
        row.validatedSupportWitnessIds.nonEmpty
    assert(hasDeclaredCarrier, clues(s"${row.id} must declare at least one exact lower carrier"))

    val allWitnesses = currentWitnesses(row)
    row.validatedRequiredWitnessIds.foreach: witnessId =>
      val descriptorId = WitnessDescriptorId(witnessId)
      assert(
        allWitnesses.exists(witness =>
          witness.descriptorId == descriptorId &&
            witness.color.forall(_ == row.validatedOwner)
        ),
        clues(s"${row.id} missing required owner witness $witnessId for ${row.owner}")
      )
    row.validatedSupportWitnessIds.foreach: witnessId =>
      val descriptorId = WitnessDescriptorId(witnessId)
      assert(
        allWitnesses.exists(_.descriptorId == descriptorId),
        clues(s"${row.id} missing support witness $witnessId")
      )

    val current = objectExtractionFromFen(row.fen)
    def hasOwnerObject(familyId: String): Boolean =
      current.objects.forFamilyId(familyId).exists(obj => obj.color.forall(_ == row.validatedOwner))
    def hasDefenderObject(familyId: String): Boolean =
      current.objects.forFamilyId(familyId).exists(obj => obj.color.contains(!row.validatedOwner))

    row.validatedRequiredObjectFamilies.foreach: familyId =>
      assert(
        hasOwnerObject(familyId),
        clues(s"${row.id} missing required current object family $familyId for ${row.owner}")
      )
    row.validatedOptionalStrengtheningFamilies.foreach: familyId =>
      val objectPresent =
        hasOwnerObject(familyId) ||
          (familyId == "KingSafetyShell" && hasDefenderObject(familyId))
      assert(
        objectPresent,
        clues(s"${row.id} missing optional exact strengthening object $familyId")
      )
    row.validatedRequiredCertificationFamilies.foreach: familyId =>
      assertEquals(
        certifiedClaimForRow(row, familyId).verdict,
        CertificationVerdict.Certified,
        clues(s"${row.id} lower certification $familyId must be certified on the exact board")
      )

    row.countableCoveragePair.foreach:
      case ("storm_route", _) =>
        assert(
          row.validatedRequiredWitnessIds.toSet.contains("available_lever_trigger") &&
            row.validatedRequiredWitnessIds.toSet.contains("pawn_push_break_contact_source"),
          clues(s"${row.id} S01 storm route must require both lever and break-contact witnesses")
        )
        assert(
          row.validatedRequiredObjectFamilies.contains("AttackScaffold") &&
            row.validatedRequiredCertificationFamilies.contains("CertifiedKingSafetyEdge"),
          clues(s"${row.id} S01 storm route must stay tied to same-king attack scaffold plus certified edge")
        )
        assert(
          ownerBreakContactTargetSquares(row).exists(isKingWing),
          clues(s"${row.id} S01 storm route must expose a same-wing pawn contact target")
        )
      case ("attack_concentration_route", _) =>
        assert(
          row.validatedRequiredObjectFamilies.contains("AttackScaffold") &&
            row.validatedRequiredCertificationFamilies.contains("CertifiedKingSafetyEdge"),
          clues(s"${row.id} S02 route must require attack scaffold plus certified king-safety edge")
        )
      case ("diagonal_attack_route", _) =>
        assert(
          row.validatedRequiredWitnessIds.contains("diagonal_lane_only") &&
            row.validatedRequiredObjectFamilies.contains("AttackScaffold") &&
            row.validatedRequiredCertificationFamilies.toSet
              .intersect(Set("ComparativeKingFragility", "CertifiedKingSafetyEdge"))
              .nonEmpty,
          clues(s"${row.id} S03 route must require exact diagonal lane, attack scaffold, and king-fragility certification support")
        )
      case ("shelter_breach_route", _) =>
        assert(
          row.validatedOptionalStrengtheningFamilies.contains("KingSafetyShell") &&
            hasDefenderObject("KingSafetyShell") &&
            row.validatedRequiredCertificationFamilies.contains("CertifiedKingSafetyEdge"),
          clues(s"${row.id} S04 route must tie breach burden to defender KingSafetyShell plus certified edge")
        )
      case ("same_cluster_near_miss", _) =>
        assertEquals(
          row.caseType,
          "comparative_false_rival",
          clues(s"${row.id} wave-4 same-cluster row must be an explicit false-rival rejection")
        )
        assertEquals(
          hasWave4OwnCarrier(row),
          false,
          clues(s"${row.id} wave-4 false rival must not satisfy its own target-band carrier")
        )
      case ("shortcut_negative", "king_shelter_wording_only") =>
        assert(
          hasDefenderObject("KingSafetyShell"),
          clues(s"${row.id} king-shelter wording negative must carry exact defender shell support")
        )
        assertEquals(
          hasCertifiedFamily(row, "CertifiedKingSafetyEdge"),
          false,
          clues(s"${row.id} king-shelter wording negative must not satisfy certified S04 breach burden")
        )
        assertEquals(
          hasWave4OwnCarrier(row),
          false,
          clues(s"${row.id} wave-4 shortcut negative must not satisfy its own target-band carrier")
        )
      case ("shortcut_negative", _) =>
        assertEquals(
          hasWave4OwnCarrier(row),
          false,
          clues(s"${row.id} wave-4 shortcut negative must not satisfy its own target-band carrier")
        )
      case _ => ()

  private def assertWave5LowerCarrier(row: ProjectionExpectationCorpus.Row): Unit =
    val hasDeclaredCarrier =
      row.validatedRequiredWitnessIds.nonEmpty ||
        row.validatedRequiredObjectFamilies.nonEmpty ||
        row.validatedRequiredCertificationFamilies.nonEmpty ||
        row.validatedOptionalStrengtheningFamilies.nonEmpty ||
        row.validatedSupportWitnessIds.nonEmpty
    assert(hasDeclaredCarrier, clues(s"${row.id} must declare at least one exact lower carrier"))

    val allWitnesses = currentWitnesses(row)
    row.validatedRequiredWitnessIds.foreach: witnessId =>
      val descriptorId = WitnessDescriptorId(witnessId)
      assert(
        allWitnesses.exists(witness =>
          witness.descriptorId == descriptorId &&
            witness.color.forall(_ == row.validatedOwner)
        ),
        clues(s"${row.id} missing required owner witness $witnessId for ${row.owner}")
      )
    row.validatedSupportWitnessIds.foreach: witnessId =>
      val descriptorId = WitnessDescriptorId(witnessId)
      assert(
        allWitnesses.exists(_.descriptorId == descriptorId),
        clues(s"${row.id} missing support witness $witnessId")
      )

    val context = exactUContext(row)
    val defender = !row.validatedOwner
    row.countableCoveragePair.foreach:
      case ("target_pressure_route", "same_target_fixation") =>
        val target = rowAnchorSquare(row)
        assert(
          row.validatedRequiredWitnessIds.contains("weak_pawn_target_state") &&
            weakPawnTargetSquares(row).contains(target),
          clues(s"${row.id} S11 fixation must keep the weak-pawn target on the same anchor square")
        )
        assert(
          ownerAttackersTo(row, target).nonEmpty,
          clues(s"${row.id} S11 fixation must expose exact same-target pressure")
        )
        assert(
          hasSameTargetPersistenceProof(row, target),
          clues(s"${row.id} S11 fixation must expose same-square fixed-pawn persistence proof")
        )
      case ("target_pressure_route", "same_target_repeated_pressure") =>
        val target = rowAnchorSquare(row)
        assert(
          row.validatedRequiredWitnessIds.contains("weak_pawn_target_state") &&
            weakPawnTargetSquares(row).contains(target),
          clues(s"${row.id} S11 repeated pressure must keep the weak-pawn target on the same anchor square")
        )
        assert(
          ownerAttackersTo(row, target).sizeCompare(2) >= 0,
          clues(s"${row.id} S11 repeated pressure must expose at least two exact same-target attackers")
        )
        assert(
          hasSameTargetPersistenceProof(row, target),
          clues(s"${row.id} S11 repeated pressure must expose same-square fixed-pawn persistence proof")
        )
      case ("wing_damage_route", "phalanx_edge_target") =>
        assertEquals(
          row.validatedRequiredWitnessIds.toSet,
          Set("sector_asymmetry_state", "available_lever_trigger", "pawn_push_break_contact_source"),
          clues(s"${row.id} S13 phalanx-edge route must require asymmetry plus same-sector lever/contact source")
        )
        assert(
          sameSectorDamageTargets(row, defender, square => isPhalanxEdgeTarget(context, defender, square)).nonEmpty,
          clues(s"${row.id} S13 phalanx-edge route must bind same-sector contact to an exact-board target role")
        )
      case ("wing_damage_route", "structurally_burdened_target") =>
        assertEquals(
          row.validatedRequiredWitnessIds.toSet,
          Set("sector_asymmetry_state", "available_lever_trigger", "pawn_push_break_contact_source"),
          clues(s"${row.id} S13 burdened-target route must require asymmetry plus same-sector lever/contact source")
        )
        assert(
          sameSectorDamageTargets(row, defender, square => isStructurallyBurdenedTarget(context, defender, square)).nonEmpty,
          clues(s"${row.id} S13 burdened-target route must bind same-sector contact to an exact-board target role")
        )
      case ("chain_base_route", _) =>
        assertEquals(
          row.validatedRequiredWitnessIds.toSet,
          Set("available_lever_trigger", "pawn_push_break_contact_source"),
          clues(s"${row.id} S14 route must require base-contact lever plus same-anchor contact source")
        )
        assert(
          ownerBreakContactTargetSquares(row).exists(square => isChainBaseTarget(context, defender, square)),
          clues(s"${row.id} S14 route must recompute the chain-base target from the exact board")
        )
        assert(
          ownerBreakContactTargetSquares(row).exists(square =>
            isChainBaseTarget(context, defender, square) && !isCenterFile(square)
          ),
          clues(s"${row.id} S14 route must include a non-center chain-base target so it cannot backfill S05")
        )
      case ("same_cluster_near_miss", _) =>
        assertEquals(
          row.caseType,
          "comparative_false_rival",
          clues(s"${row.id} wave-5 same-cluster row must be an explicit false-rival rejection")
        )
        assertEquals(
          hasWave5OwnCarrier(row),
          false,
          clues(s"${row.id} wave-5 false rival must not satisfy its own target-band carrier")
        )
        row.validatedCoveragePair.foreach:
          case ("same_cluster_near_miss", "vs_s14") if row.band == "S13" =>
            assert(
              ownerBreakContactTargetSquares(row).forall(square => !isStructurallyBurdenedTarget(context, defender, square)),
              clues(s"${row.id} S13 vs-S14 false rival must not also satisfy the burdened-target route")
            )
          case ("same_cluster_near_miss", "vs_s13") if row.band == "S14" =>
            assert(
              ownerBreakContactTargetSquares(row).forall(square => !isChainBaseTarget(context, defender, square)),
              clues(s"${row.id} S14 vs-S13 false rival must not also satisfy the chain-base route")
            )
          case ("same_cluster_near_miss", "vs_s15") if row.band == "S13" =>
            val candidate = rowAnchorPieceOrSquare(row)
            assert(
              context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, candidate),
              clues(s"${row.id} S13 vs-S15 false rival must expose exact candidate-passer support, not existing-passer-only evidence")
            )
            assert(
              ownerBreakContactSourceTargetPairs(row).exists((source, target) =>
                source == candidate && isChainBaseTarget(context, defender, target)
              ),
              clues(s"${row.id} S13 vs-S15 false rival must bind candidate-passer support to an exact creation route")
            )
          case _ => ()
      case ("shortcut_negative", _) =>
        assertEquals(
          row.caseType,
          "nasty_negative",
          clues(s"${row.id} wave-5 shortcut row must be a nasty-negative rejection")
        )
        assertEquals(
          hasWave5OwnCarrier(row),
          false,
          clues(s"${row.id} wave-5 shortcut negative must not satisfy its own target-band carrier")
        )
        row.validatedCoveragePair.foreach:
          case ("shortcut_negative", "target_swap_by_prose") if row.band == "S11" =>
            val target = rowAnchorSquare(row)
            assert(
              weakPawnTargetSquares(row).contains(target) &&
                ownerAttackersTo(row, target).isEmpty,
              clues(s"${row.id} S11 target-swap negative must not expose same-target pressure on its anchor")
            )
          case _ => ()
      case _ => ()

  private def assertWave6LowerCarrier(row: ProjectionExpectationCorpus.Row): Unit =
    val hasDeclaredCarrier =
      row.validatedRequiredWitnessIds.nonEmpty ||
        row.validatedRequiredObjectFamilies.nonEmpty ||
        row.validatedRequiredCertificationFamilies.nonEmpty ||
        row.validatedSupportShellIds.nonEmpty ||
        row.validatedOptionalStrengtheningFamilies.nonEmpty ||
        row.validatedSupportWitnessIds.nonEmpty
    assert(hasDeclaredCarrier, clues(s"${row.id} must declare at least one exact lower carrier"))

    val allWitnesses = currentWitnesses(row)
    row.validatedRequiredWitnessIds.foreach: witnessId =>
      val descriptorId = WitnessDescriptorId(witnessId)
      assert(
        allWitnesses.exists(witness =>
          witness.descriptorId == descriptorId &&
            witnessColorMatchesRowRequirement(row, witnessId, witness.color)
        ),
        clues(s"${row.id} missing required witness $witnessId at the row polarity")
      )
    row.validatedSupportWitnessIds.foreach: witnessId =>
      val descriptorId = WitnessDescriptorId(witnessId)
      assert(
        allWitnesses.exists(_.descriptorId == descriptorId),
        clues(s"${row.id} missing support witness $witnessId")
      )

    val context = exactUContext(row)
    val defender = !row.validatedOwner
    row.countableCoveragePair.foreach:
      case ("creation_route", "s13_wing_damage") =>
        val candidate = rowAnchorPieceOrSquare(row)
        assert(
          context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, candidate),
          clues(s"${row.id} S15 route must expose exact candidate-passer root truth on the anchor")
        )
        assert(
          row.validatedSupportShellIds.contains("create_passer"),
          clues(s"${row.id} S15 route must keep create_passer as support shell, not witness truth")
        )
        assert(
          sameSectorDamageTargetsFrom(row, defender, candidate, square =>
            isPhalanxEdgeTarget(context, defender, square) ||
              isStructurallyBurdenedTarget(context, defender, square)
          ).nonEmpty,
          clues(s"${row.id} S15 S13 route must bind the same candidate pawn to exact wing-damage contact")
        )
      case ("creation_route", "s14_chain_base") =>
        val candidate = rowAnchorPieceOrSquare(row)
        assert(
          context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, candidate),
          clues(s"${row.id} S15 route must expose exact candidate-passer root truth on the anchor")
        )
        assert(
          row.validatedSupportShellIds.contains("create_passer"),
          clues(s"${row.id} S15 route must keep create_passer as support shell, not witness truth")
        )
        assert(
          ownerBreakContactSourceTargetPairs(row).exists((source, target) =>
            source == candidate && isChainBaseTarget(context, defender, target)
          ),
          clues(s"${row.id} S15 S14 route must bind the same candidate pawn to an exact chain-base contact")
        )
      case ("suppression_route", "blockade_hold") =>
        assert(
          enemyPassedPawnSquares(row).nonEmpty,
          clues(s"${row.id} S16 blockade route must expose an enemy passed-pawn witness")
        )
        assert(
          enemyPassedForwardBlockers(row).nonEmpty,
          clues(s"${row.id} S16 blockade route must bind an owner blocker to the enemy passer front square")
        )
        assertCertifiedSupport(row, "FortressDrawCertification")
      case ("suppression_route", "restriction_hold") =>
        assert(
          enemyPassedPawnSquares(row).nonEmpty,
          clues(s"${row.id} S16 restriction route must expose an enemy passed-pawn witness")
        )
        assert(
          allWitnesses.exists(witness =>
            witness.descriptorId == WitnessDescriptorId("short_run_slider_gate_restriction") &&
              witness.color.contains(row.validatedOwner)
          ),
          clues(s"${row.id} S16 restriction route must expose owner-side restriction support")
        )
        assertCertifiedSupport(row, "PerpetualCheckHolding")
      case ("suppression_route", "non_losing_race") =>
        assert(
          enemyPassedPawnSquares(row).nonEmpty,
          clues(s"${row.id} S16 race route must expose an enemy passed-pawn witness")
        )
        assertCertifiedSupport(row, "PromotionRace")
      case ("same_cluster_near_miss", bucket) =>
        assertEquals(
          row.caseType,
          "comparative_false_rival",
          clues(s"${row.id} wave-6 same-cluster row must be an explicit false-rival rejection")
        )
        bucket match
          case "vs_s13" if row.band == "S15" =>
            assert(
              sameSectorDamageTargets(row, defender, _ => true).nonEmpty,
              clues(s"${row.id} S15 vs-S13 false rival must expose exact S13 wing-damage support")
            )
            assert(
              !context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, rowAnchorPieceOrSquare(row)),
              clues(s"${row.id} S15 vs-S13 false rival must miss same-anchor candidate-passer creation")
            )
          case "vs_s14" if row.band == "S15" =>
            assert(
              ownerBreakContactTargetSquares(row).exists(square => isChainBaseTarget(context, defender, square)),
              clues(s"${row.id} S15 vs-S14 false rival must expose exact chain-base contact support")
            )
            assert(
              !context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, rowAnchorPieceOrSquare(row)),
              clues(s"${row.id} S15 vs-S14 false rival must miss same-anchor candidate-passer creation")
            )
          case "vs_s16" if row.band == "S15" =>
            assert(
              enemyPassedPawnSquares(row).nonEmpty || row.validatedOptionalStrengtheningFamilies.exists(isHoldOrRaceFamily),
              clues(s"${row.id} S15 vs-S16 false rival must expose suppression/race support")
            )
            assert(
              context.activeColorPawnSquares(SchemaId.CandidatePasser, row.validatedOwner).isEmpty,
              clues(s"${row.id} S15 vs-S16 false rival must not expose owner candidate-passer creation truth")
            )
          case "vs_s15" if row.band == "S16" =>
            assert(
              context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, rowAnchorPieceOrSquare(row)),
              clues(s"${row.id} S16 vs-S15 false rival must expose exact candidate-passer creation support")
            )
            assert(
              enemyPassedPawnSquares(row).isEmpty,
              clues(s"${row.id} S16 vs-S15 false rival must not already expose an enemy passer")
            )
          case "vs_s22" if row.band == "S16" =>
            assert(
              row.validatedRequiredObjectFamilies.contains("FortressHoldingShell"),
              clues(s"${row.id} S16 vs-S22 false rival must declare fortress hold as lower object support")
            )
            assert(
              objectExtractionFromFen(row.fen).objects.forFamilyId("FortressHoldingShell").exists: obj =>
                obj.color.forall(_ == row.validatedOwner),
              clues(s"${row.id} S16 vs-S22 false rival must expose exact-board fortress hold object support")
            )
            assert(
              row.validatedOptionalStrengtheningFamilies.exists(isHoldOrRaceFamily),
              clues(s"${row.id} S16 vs-S22 false rival must expose hold support without enemy-passer suppression")
            )
            assert(
              enemyPassedPawnSquares(row).isEmpty,
              clues(s"${row.id} S16 vs-S22 false rival must not bind the hold to an enemy passer")
            )
          case "vs_s23" if row.band == "S16" =>
            assert(
              row.validatedForbiddenShortcuts.contains("king_activity_rival_shortcut"),
              clues(s"${row.id} S16 vs-S23 false rival must stay on king-activity rival wording")
            )
            assert(
              row.validatedOptionalStrengtheningFamilies.contains("king_opposition_certified"),
              clues(s"${row.id} S16 vs-S23 false rival must declare king-opposition support metadata")
            )
            assertKingOppositionContactSupport(row)
            assert(
              enemyPassedPawnSquares(row).isEmpty,
              clues(s"${row.id} S16 vs-S23 false rival must not bind king activity to enemy-passer suppression")
            )
          case _ => ()
      case ("shortcut_negative", bucket) =>
        assertEquals(
          row.caseType,
          "nasty_negative",
          clues(s"${row.id} wave-6 shortcut row must be a nasty-negative rejection")
        )
        bucket match
          case "candidate_passer_only" if row.band == "S15" =>
            val candidate = rowAnchorPieceOrSquare(row)
            assert(
              context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, candidate),
              clues(s"${row.id} candidate-only shortcut must expose exact candidate-passer root truth")
            )
            assert(
              !hasAnyCreationRouteFrom(row, defender, candidate),
              clues(s"${row.id} candidate-only shortcut must not expose an exact S13/S14 creation route")
            )
          case "existing_passer_entity_only" if row.band == "S15" =>
            val subject = rowAnchorPieceOrSquare(row)
            assert(
              ownerPassedPawnSquares(row).contains(subject),
              clues(s"${row.id} existing-passer shortcut must expose an existing passed pawn")
            )
            assert(
              !context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, subject),
              clues(s"${row.id} existing-passer shortcut must not be counted as candidate-passer creation")
            )
          case "enemy_passer_presence_only" if row.band == "S16" =>
            assert(
              enemyPassedPawnSquares(row).nonEmpty,
              clues(s"${row.id} enemy-passer shortcut must expose the enemy passed pawn exactly")
            )
            assert(
              enemyPassedForwardBlockers(row).isEmpty &&
                row.validatedOptionalStrengtheningFamilies.forall(family => !isHoldOrRaceFamily(family)),
              clues(s"${row.id} enemy-passer shortcut must not include blocker, hold, or race proof")
            )
          case "blocker_picture_only" if row.band == "S16" =>
            assert(
              enemyPassedPawnSquares(row).nonEmpty && enemyPassedForwardBlockers(row).nonEmpty,
              clues(s"${row.id} blocker-picture shortcut must expose a blocked enemy passer")
            )
            assert(
              row.validatedOptionalStrengtheningFamilies.forall(family => !isHoldOrRaceFamily(family)),
              clues(s"${row.id} blocker-picture shortcut must not smuggle certified hold/race support")
            )
          case _ => ()
      case _ => ()

  private def assertGlobalRejectedLowerCarrier(row: ProjectionExpectationCorpus.Row): Unit =
    assertEquals(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand.contains(row.band),
      true,
      clues(s"${row.band} must have a contract-owned exact-board scaffold before global closure")
    )
    if !StrategyProjectionScopeContract.isStartReadyBandId(row.validatedBand) then
      assertEquals(
        row.evidenceClaimsForRuntime,
        Vector.empty,
        clues(s"${row.id} coverage-only rejected row must not declare runtime projection evidence")
      )
      assertEquals(
        row.validatedRequiredSupportSeedIds,
        Vector.empty,
        clues(s"${row.id} coverage-only rejected row must not declare runtime support seeds")
      )
    assertOwnAdmittedCarrierAbsent(row)
    row.validatedCoveragePair.foreach:
      case ("shortcut_negative", bucket) =>
        assert(
          row.validatedForbiddenShortcuts.contains(bucket),
          clues(s"${row.id} shortcut negative must literally freeze $bucket")
        )
      case ("same_cluster_near_miss", bucket) =>
        assert(
          row.validatedRivalBands.contains(bucket.stripPrefix("vs_").toUpperCase),
          clues(s"${row.id} false-rival row must literally name ${bucket.stripPrefix("vs_").toUpperCase}")
        )
      case _ => ()
    if row.band == "S19" && row.admissionPath == "trade_invariant_result_without_same_task_certification" then
      val (before, move, after) =
        row.validatedDeltaCompanion.getOrElse(fail(s"${row.id} must carry exact delta fields"))
      val extraction =
        StrategicDeltaExtractor
          .fromFens(before, move, after)
          .fold(message => fail(s"${row.id} delta extraction failed: $message"), identity)
      assert(
        extraction.deltas.forFamilyId("TradeInvariant").exists(_.color.contains(row.validatedOwner)),
        clues(s"${row.id} result-only S19 negative must expose exact TradeInvariant support")
      )
      assert(
        row.validatedOptionalStrengtheningFamilies.contains("WinningEndgame"),
        clues(s"${row.id} result-only S19 negative must keep WinningEndgame as non-admitting support")
      )
      assertEquals(
        row.validatedRequiredCertificationFamilies,
        Vector.empty,
        clues(s"${row.id} result-only S19 negative must not promote result certification into S19 truth")
      )
      assertEquals(
        hasCertifiedFamily(row, "MaterialHarvest") || hasCertifiedFamily(row, "FortressDrawCertification"),
        false,
        clues(s"${row.id} result-only S19 negative must not share an admitted same-task certification")
      )
    if row.band == "S19" && row.admissionPath == "material_reduction_only" then
      val (before, move, after) =
        row.validatedDeltaCompanion.getOrElse(fail(s"${row.id} must carry exact delta fields"))
      val extraction =
        StrategicDeltaExtractor
          .fromFens(before, move, after)
          .fold(message => fail(s"${row.id} delta extraction failed: $message"), identity)
      assertEquals(
        extraction.deltas.forFamilyId("TradeInvariant").exists(_.color.contains(row.validatedOwner)),
        false,
        clues(s"${row.id} material-only S19 negative must not expose exact TradeInvariant support")
      )
      assertEquals(
        row.validatedRequiredCertificationFamilies,
        Vector.empty,
        clues(s"${row.id} material-only S19 negative must not promote MaterialHarvest into S19 truth")
      )
      assert(
        row.validatedOptionalStrengtheningFamilies.contains("MaterialHarvest"),
        clues(s"${row.id} material-only S19 negative must keep material support as non-admitting metadata")
      )
    if row.band == "S22" && row.admissionPath == "fortress_shell_only" then
      assert(
        hasObjectFamily(row, "FortressHoldingShell"),
        clues(s"${row.id} fortress-shell negative must carry an exact FortressHoldingShell object")
      )
      assertEquals(
        row.validatedRequiredCertificationFamilies,
        Vector.empty,
        clues(s"${row.id} fortress-shell negative must not declare certified hold evidence")
      )
      val admittedFortressHoldFens =
        rows.filter(other =>
          other.band == "S22" &&
            other.expectation == "admitted" &&
            other.coverageBucket.contains("fortress_draw_hold")
        ).map(_.normalizedFen).toSet
      assert(
        !admittedFortressHoldFens.contains(row.normalizedFen),
        clues(s"${row.id} fortress-shell negative must not reuse an admitted S22 hold board")
      )
      assertEquals(
        certificationVerdictForRow(
          row,
          "FortressDrawCertification",
          Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Insufficient)
        ),
        CertificationVerdict.SupportOnly,
        clues(s"${row.id} fortress-shell negative must stay support-only without satisfied best-defense evidence")
      )

  private def assertOwnAdmittedCarrierAbsent(row: ProjectionExpectationCorpus.Row): Unit =
    val admittedCarrierPresent =
      row.band match
        case "S06" => hasS06Carrier(row)
        case "S09" => hasS09Carrier(row)
        case "S10" => hasS10Carrier(row)
        case "S12" => hasS12Carrier(row)
        case "S18" => hasS18Carrier(row)
        case "S19" => hasS19Carrier(row)
        case "S20" => hasS20Carrier(row)
        case "S22" => hasS22Carrier(row)
        case "S25" => hasS25Carrier(row)
        case other => fail(s"${row.id} unsupported global negative freeze band $other")
    assertEquals(
      admittedCarrierPresent,
      false,
      clues(s"${row.id} rejected row must not satisfy its own admitted lower-carrier burden")
    )

  private def hasS06Carrier(row: ProjectionExpectationCorpus.Row): Boolean =
    row.validatedRequiredWitnessIds.contains("structural_space_claim") &&
    hasOwnerWitness(row, "structural_space_claim") &&
      ((row.validatedRequiredWitnessIds.contains("knight_on_outpost_square") &&
        hasOwnerWitness(row, "knight_on_outpost_square")) ||
        (row.validatedRequiredWitnessIds.contains("short_run_slider_gate_restriction") &&
          hasOwnerWitness(row, "short_run_slider_gate_restriction")))

  private def hasS09Carrier(row: ProjectionExpectationCorpus.Row): Boolean =
    row.validatedRequiredWitnessIds.contains("file_lane_state") &&
    hasOwnerWitness(row, "file_lane_state")

  private def hasS10Carrier(row: ProjectionExpectationCorpus.Row): Boolean =
    row.validatedRequiredWitnessIds.contains("weak_outpost_square_state") &&
    row.validatedRequiredWitnessIds.contains("knight_on_outpost_square") &&
    hasOwnerWitness(row, "weak_outpost_square_state") &&
      hasOwnerWitness(row, "knight_on_outpost_square")

  private def hasS12Carrier(row: ProjectionExpectationCorpus.Row): Boolean =
    ((row.validatedRequiredWitnessIds.contains("weak_outpost_square_state") &&
      hasOwnerWitness(row, "weak_outpost_square_state")) ||
      (row.validatedRequiredWitnessIds.contains("diagonal_lane_only") &&
        hasOwnerWitness(row, "diagonal_lane_only"))) &&
      row.validatedRequiredWitnessIds.contains("short_run_slider_gate_restriction") &&
      hasOwnerWitness(row, "short_run_slider_gate_restriction")

  private def hasS18Carrier(row: ProjectionExpectationCorpus.Row): Boolean =
    row.validatedRequiredWitnessIds.contains("bishop_pair_state") &&
    hasOwnerWitness(row, "bishop_pair_state") &&
      Set("InitiativeWindow", "MobilityComparison", "MaterialHarvest", "WinningEndgame")
        .exists(family => row.validatedRequiredCertificationFamilies.contains(family) && hasCertifiedFamily(row, family))

  private def hasS19Carrier(row: ProjectionExpectationCorpus.Row): Boolean =
    val materialCarrier =
      row.validatedRequiredCertificationFamilies == Vector("MaterialHarvest") &&
      row.normalizedFen == row.validatedDeltaCompanion.map(_._1).getOrElse(row.normalizedFen) &&
        hasCertifiedFamily(row, "MaterialHarvest")
    val holdCarrier =
      row.validatedRequiredCertificationFamilies == Vector("FortressDrawCertification") &&
      row.normalizedFen == row.validatedDeltaCompanion.map(_._3).getOrElse(row.normalizedFen) &&
        row.validatedRequiredObjectFamilies.contains("FortressHoldingShell") &&
        hasObjectFamily(row, "FortressHoldingShell") &&
        hasCertifiedFamily(row, "FortressDrawCertification") &&
        tradeInvariantTargetSquares(row).intersect(objectSupportTargetSquares(row, "FortressHoldingShell")).nonEmpty
    row.validatedDeltaCompanion.exists: (before, move, after) =>
      val extraction =
        StrategicDeltaExtractor
          .fromFens(before, move, after)
          .fold(message => fail(s"${row.id} delta extraction failed: $message"), identity)
      row.validatedRequiredDeltaFamilies == Vector("TradeInvariant") &&
        extraction.deltas.forFamilyId("TradeInvariant").exists(_.color.contains(row.validatedOwner)) &&
        (materialCarrier || holdCarrier)

  private def hasS20Carrier(row: ProjectionExpectationCorpus.Row): Boolean =
    row.validatedRequiredCertificationFamilies.contains("MobilityComparison") &&
      hasCertifiedFamily(row, "MobilityComparison") &&
      ((row.validatedRequiredWitnessIds.contains("short_run_slider_gate_restriction") &&
        hasOwnerWitness(row, "short_run_slider_gate_restriction")) ||
        (row.validatedRequiredWitnessIds.contains("duty_bound_defender") &&
          hasOwnerWitness(row, "duty_bound_defender")))

  private def hasS22Carrier(row: ProjectionExpectationCorpus.Row): Boolean =
    (row.validatedRequiredObjectFamilies.contains("FortressHoldingShell") &&
        row.validatedRequiredCertificationFamilies.contains("FortressDrawCertification") &&
        hasObjectFamily(row, "FortressHoldingShell") &&
        hasCertifiedFamily(row, "FortressDrawCertification")) ||
      (row.validatedRequiredCertificationFamilies.contains("PerpetualCheckHolding") &&
        hasCertifiedFamily(row, "PerpetualCheckHolding"))

  private def hasS25Carrier(row: ProjectionExpectationCorpus.Row): Boolean =
    val extraction =
      StrategySupportSeedExtractor
        .fromFen(row.normalizedFen)
        .fold(message => fail(s"Row ${row.id} support seed extraction failed: $message"), identity)
    extraction.seeds
      .forSeedId(StrategySupportSeedScopeContract.S25RankCorridorState)
      .exists(_.color.contains(row.validatedOwner))

  private def hasWave3OwnCarrier(row: ProjectionExpectationCorpus.Row): Boolean =
    row.band match
      case "S05" =>
        row.validatedRequiredWitnessIds.toSet.contains("pawn_push_break_contact_source") &&
          row.validatedRequiredObjectFamilies.contains("CentralContactFront") &&
          hasObjectFamily(row, "CentralContactFront") &&
          ownerBreakContactTargetSquares(row).exists(isCenterFile)
      case "S07" =>
        row.validatedRequiredObjectFamilies.contains("OpeningDevelopmentRegime") &&
          row.validatedRequiredCertificationFamilies.toSet == Set("DevelopmentComparison", "InitiativeWindow") &&
          hasObjectFamily(row, "OpeningDevelopmentRegime") &&
          hasCertifiedFamily(row, "DevelopmentComparison") &&
          hasCertifiedFamily(row, "InitiativeWindow")
      case "S08" =>
        row.validatedRequiredCertificationFamilies.contains("InitiativeWindow") &&
          row.validatedSupportWitnessIds.contains("pawn_push_break_contact_source") &&
          hasCertifiedFamily(row, "InitiativeWindow") &&
          currentWitnesses(row).exists(witness =>
            witness.descriptorId == WitnessDescriptorId("pawn_push_break_contact_source") &&
              witness.color.contains(!row.validatedOwner)
          )
      case "S21" =>
        row.validatedRequiredWitnessIds.contains("pawn_push_break_contact_source") &&
          row.validatedRequiredCertificationFamilies.contains("InitiativeWindow") &&
          hasCertifiedFamily(row, "InitiativeWindow") &&
          ownerBreakContactTargetSquares(row).nonEmpty
      case other => fail(s"${row.id} unsupported wave-3 carrier band $other")

  private def hasWave4OwnCarrier(row: ProjectionExpectationCorpus.Row): Boolean =
    row.band match
      case "S01" =>
        row.validatedRequiredWitnessIds.toSet.contains("available_lever_trigger") &&
          row.validatedRequiredWitnessIds.toSet.contains("pawn_push_break_contact_source") &&
          row.validatedRequiredObjectFamilies.contains("AttackScaffold") &&
          row.validatedRequiredCertificationFamilies.contains("CertifiedKingSafetyEdge") &&
          hasObjectFamily(row, "AttackScaffold") &&
          hasCertifiedFamily(row, "CertifiedKingSafetyEdge") &&
          ownerBreakContactTargetSquares(row).exists(isKingWing)
      case "S02" =>
        row.validatedRequiredObjectFamilies.contains("AttackScaffold") &&
          row.validatedRequiredCertificationFamilies.contains("CertifiedKingSafetyEdge") &&
          hasObjectFamily(row, "AttackScaffold") &&
          hasCertifiedFamily(row, "CertifiedKingSafetyEdge") &&
          !hasDefenderObjectFamily(row, "KingSafetyShell")
      case "S03" =>
        row.validatedRequiredWitnessIds.contains("diagonal_lane_only") &&
          row.validatedRequiredObjectFamilies.contains("AttackScaffold") &&
          row.validatedRequiredCertificationFamilies.toSet
            .intersect(Set("ComparativeKingFragility", "CertifiedKingSafetyEdge"))
            .nonEmpty &&
          hasOwnerWitness(row, "diagonal_lane_only") &&
          hasObjectFamily(row, "AttackScaffold")
      case "S04" =>
        row.validatedRequiredCertificationFamilies.contains("CertifiedKingSafetyEdge") &&
          hasCertifiedFamily(row, "CertifiedKingSafetyEdge") &&
          hasDefenderObjectFamily(row, "KingSafetyShell")
      case other => fail(s"${row.id} unsupported wave-4 carrier band $other")

  private def hasWave5OwnCarrier(row: ProjectionExpectationCorpus.Row): Boolean =
    val context = exactUContext(row)
    val defender = !row.validatedOwner
    row.band match
      case "S11" =>
        val target = rowAnchorSquare(row)
        row.validatedRequiredWitnessIds.contains("weak_pawn_target_state") &&
          weakPawnTargetSquares(row).contains(target) &&
          ownerAttackersTo(row, target).nonEmpty &&
          hasSameTargetPersistenceProof(row, target)
      case "S13" =>
        row.validatedRequiredWitnessIds.toSet == Set(
          "sector_asymmetry_state",
          "available_lever_trigger",
          "pawn_push_break_contact_source"
        ) &&
          sameSectorDamageTargets(row, defender, square =>
            isPhalanxEdgeTarget(context, defender, square) ||
              isStructurallyBurdenedTarget(context, defender, square)
          ).nonEmpty
      case "S14" =>
        row.validatedRequiredWitnessIds.toSet == Set("available_lever_trigger", "pawn_push_break_contact_source") &&
          ownerBreakContactTargetSquares(row).exists(square =>
            isChainBaseTarget(context, defender, square) && !isCenterFile(square)
          )
      case other => fail(s"${row.id} unsupported wave-5 carrier band $other")

  private def hasOwnerWitness(row: ProjectionExpectationCorpus.Row, witnessId: String): Boolean =
    currentWitnesses(row).exists(witness =>
      witness.descriptorId == WitnessDescriptorId(witnessId) &&
        witness.color.forall(_ == row.validatedOwner)
    )

  private def hasObjectFamily(row: ProjectionExpectationCorpus.Row, familyId: String): Boolean =
    objectExtractionFromFen(row.fen).objects.forFamilyId(familyId).exists(obj => obj.color.forall(_ == row.validatedOwner))

  private def hasDefenderObjectFamily(row: ProjectionExpectationCorpus.Row, familyId: String): Boolean =
    objectExtractionFromFen(row.fen).objects.forFamilyId(familyId).exists(obj => obj.color.contains(!row.validatedOwner))

  private def objectSupportTargetSquares(row: ProjectionExpectationCorpus.Row, familyId: String): Set[Square] =
    objectExtractionFromFen(row.fen).objects
      .forFamilyId(familyId)
      .filter(obj => obj.color.forall(_ == row.validatedOwner))
      .flatMap(_.support.targetSquares)
      .toSet

  private def tradeInvariantTargetSquares(row: ProjectionExpectationCorpus.Row): Set[Square] =
    row.validatedDeltaCompanion.toVector
      .flatMap: (before, move, after) =>
        StrategicDeltaExtractor
          .fromFens(before, move, after)
          .fold(message => fail(s"${row.id} delta extraction failed: $message"), identity)
          .deltas
          .forFamilyId("TradeInvariant")
          .filter(_.color.contains(row.validatedOwner))
          .flatMap(_.support.targetSquares)
      .toSet

  private def hasCertifiedFamily(row: ProjectionExpectationCorpus.Row, familyId: String): Boolean =
    try
      val claim = certifiedClaimForRow(row, familyId)
      claim.verdict == CertificationVerdict.Certified
    catch case _: munit.FailException => false

  private def sameSectorDamageTargetsFrom(
      row: ProjectionExpectationCorpus.Row,
      defenderColor: chess.Color,
      source: Square,
      targetRole: Square => Boolean
  ): Vector[Square] =
    ownerBreakContactSourceTargetPairs(row).collect:
      case (`source`, target)
          if sectorOf(source.file) == sectorOf(target.file) &&
            hasDefenderMajoritySectorAsymmetry(row, sectorOf(target.file), defenderColor) &&
            targetRole(target) =>
        target

  private def hasAnyCreationRouteFrom(
      row: ProjectionExpectationCorpus.Row,
      defenderColor: chess.Color,
      source: Square
  ): Boolean =
    val context = exactUContext(row)
    ownerBreakContactSourceTargetPairs(row).exists((contactSource, target) =>
      contactSource == source &&
        (isChainBaseTarget(context, defenderColor, target) ||
          (sectorOf(source.file) == sectorOf(target.file) &&
            hasDefenderMajoritySectorAsymmetry(row, sectorOf(target.file), defenderColor) &&
            (isPhalanxEdgeTarget(context, defenderColor, target) ||
              isStructurallyBurdenedTarget(context, defenderColor, target))))
    )

  private def ownerPassedPawnSquares(row: ProjectionExpectationCorpus.Row): Vector[Square] =
    passedPawnWitnessSquaresFor(row, row.validatedOwner)

  private def enemyPassedPawnSquares(row: ProjectionExpectationCorpus.Row): Vector[Square] =
    passedPawnWitnessSquaresFor(row, !row.validatedOwner)

  private def passedPawnWitnessSquaresFor(row: ProjectionExpectationCorpus.Row, color: chess.Color): Vector[Square] =
    currentWitnesses(row)
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("passed_pawn_entity_state") &&
          witness.color.contains(color)
      )
      .flatMap:
        case witness =>
          witness.anchor match
            case WitnessAnchor.PieceSquareAnchor(square) => Vector(square)
            case _ => Vector.empty
      .distinct
      .sortBy(_.value)

  private def enemyPassedForwardBlockers(row: ProjectionExpectationCorpus.Row): Vector[(Square, Square)] =
    val context = exactUContext(row)
    val enemy = !row.validatedOwner
    enemyPassedPawnSquares(row).flatMap: passer =>
      context.forwardSquare(enemy, passer).toVector.flatMap: blocker =>
        Option.when(context.pieceAt(blocker).exists(_.color == row.validatedOwner))(passer -> blocker)
    .sortBy((passer, blocker) => (passer.value, blocker.value))

  private def assertCertifiedSupport(row: ProjectionExpectationCorpus.Row, familyId: String): Unit =
    assert(
      row.validatedOptionalStrengtheningFamilies.contains(familyId) ||
        row.validatedRequiredCertificationFamilies.contains(familyId),
      clues(s"${row.id} must declare $familyId as support for this wave-6 route")
    )
    assertEquals(
      certifiedClaimForRow(row, familyId).verdict,
      CertificationVerdict.Certified,
      clues(s"${row.id} $familyId support must be certified on the exact board")
    )

  private def isHoldOrRaceFamily(familyId: String): Boolean =
    Set("FortressDrawCertification", "PerpetualCheckHolding", "PromotionRace").contains(familyId)

  private def currentWitnesses(row: ProjectionExpectationCorpus.Row) =
    val primary =
      UWitnessExtractor
        .fromFen(row.normalizedFen)
        .fold(message => fail(s"Row ${row.id} U witness extraction failed: $message"), identity)
    val attached =
      UAttachedExtractor
        .fromFen(row.normalizedFen)
        .fold(message => fail(s"Row ${row.id} U-attached extraction failed: $message"), identity)
    primary.witnesses.all ++ attached.witnesses.all

  private def ownerBreakContactTargetSquares(row: ProjectionExpectationCorpus.Row): Vector[Square] =
    ownerBreakContactSourceTargetPairs(row)
      .map(_._2)
      .distinct
      .sortBy(_.value)

  private def ownerBreakContactSourceTargetPairs(row: ProjectionExpectationCorpus.Row): Vector[(Square, Square)] =
    currentWitnesses(row)
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("pawn_push_break_contact_source") &&
          witness.color.contains(row.validatedOwner)
      )
      .flatMap: witness =>
        witness.anchor match
          case WitnessAnchor.PieceSquareAnchor(source) =>
            contactTargetSquares(witness).map(source -> _)
          case _ => Vector.empty
      .distinct
      .sortBy((source, target) => (source.value, target.value))

  private def weakPawnTargetSquares(row: ProjectionExpectationCorpus.Row): Vector[Square] =
    currentWitnesses(row)
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("weak_pawn_target_state") &&
          witness.color.contains(row.validatedOwner)
      )
      .flatMap(witness =>
        witness.payload.get("square").collect { case WitnessValue.SquareValue(square) => square }
      )
      .distinct
      .sortBy(_.value)

  private def weakPawnTargetTags(row: ProjectionExpectationCorpus.Row, target: Square): Set[String] =
    currentWitnesses(row)
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("weak_pawn_target_state") &&
          witness.color.contains(row.validatedOwner) &&
          witness.payload.get("square").contains(WitnessValue.SquareValue(target))
      )
      .flatMap(witness =>
        witness.payload.get("weakness_tags").collect { case WitnessValue.TokenListValue(values) => values }.getOrElse(Vector.empty)
      )
      .toSet

  private def hasSameTargetPersistenceProof(row: ProjectionExpectationCorpus.Row, target: Square): Boolean =
    weakPawnTargetTags(row, target).contains("fixed")

  private def rowAnchorSquare(row: ProjectionExpectationCorpus.Row): Square =
    val key = row.validatedAnchor.stripPrefix("square:")
    Square
      .fromKey(key)
      .getOrElse(fail(s"${row.id} must use square:<key> anchor, got ${row.anchor}"))

  private def rowAnchorPieceOrSquare(row: ProjectionExpectationCorpus.Row): Square =
    val normalized = row.validatedAnchor
    val key =
      if normalized.startsWith("piece:") then normalized.stripPrefix("piece:")
      else if normalized.startsWith("square:") then normalized.stripPrefix("square:")
      else fail(s"${row.id} must use piece:<key> or square:<key> anchor, got ${row.anchor}")
    Square
      .fromKey(key)
      .getOrElse(fail(s"${row.id} must use a valid square anchor, got ${row.anchor}"))

  private def ownerAttackersTo(row: ProjectionExpectationCorpus.Row, target: Square): Vector[Square] =
    val context = exactUContext(row)
    val position = Position(context.board.toBoard, variant.Standard, row.validatedOwner)
    Square.all
      .filter(square => position.pieceAt(square).exists(_.color == row.validatedOwner))
      .filter(square => position.generateMovesAt(square).exists(_.dest == target))
      .toVector
      .sortBy(_.value)

  private def exactUContext(row: ProjectionExpectationCorpus.Row): UExtractionContext =
    val extraction =
      StrategicObjectExtractor
        .fromFen(row.normalizedFen)
        .fold(message => fail(s"Row ${row.id} strategic extraction failed: $message"), identity)
    UExtractionContext(extraction.rootState)

  private def isStructurallyBurdenedTarget(
      context: UExtractionContext,
      defenderColor: chess.Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      !isChainBaseTarget(context, defenderColor, square) &&
      (context.hasColorPawnSquare(SchemaId.FixedPawn, defenderColor, square) ||
        context.hasColorPawnSquare(SchemaId.IsolatedPawn, defenderColor, square) ||
        context.hasColorPawnSquare(SchemaId.BackwardPawn, defenderColor, square))

  private def isChainBaseTarget(
      context: UExtractionContext,
      defenderColor: chess.Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      supportsForward(context, defenderColor, square) &&
      !supportedFromRear(context, defenderColor, square)

  private def isPhalanxEdgeTarget(
      context: UExtractionContext,
      defenderColor: chess.Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      sameRankAdjacentCount(context, defenderColor, square) == 1

  private def supportsForward(
      context: UExtractionContext,
      defenderColor: chess.Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      square.pawnAttacks(defenderColor).squares.exists(target => context.hasPieceOn(defenderColor, Pawn, target))

  private def supportedFromRear(
      context: UExtractionContext,
      defenderColor: chess.Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      square.pawnAttacks(!defenderColor).squares.exists(source => context.hasPieceOn(defenderColor, Pawn, source))

  private def sameRankAdjacentCount(
      context: UExtractionContext,
      defenderColor: chess.Color,
      square: Square
  ): Int =
    Vector(square.file.value - 1, square.file.value + 1)
      .flatMap(File(_))
      .count(file => context.hasPieceOn(defenderColor, Pawn, Square(file, square.rank)))

  private def sameSectorDamageTargets(
      row: ProjectionExpectationCorpus.Row,
      defenderColor: chess.Color,
      targetRole: Square => Boolean
  ): Vector[Square] =
    ownerBreakContactSourceTargetPairs(row).collect:
      case (source, target)
          if sectorOf(source.file) == sectorOf(target.file) &&
            hasDefenderMajoritySectorAsymmetry(row, sectorOf(target.file), defenderColor) &&
            targetRole(target) =>
        target

  private def hasDefenderMajoritySectorAsymmetry(
      row: ProjectionExpectationCorpus.Row,
      sector: WitnessSector,
      defenderColor: chess.Color
  ): Boolean =
    currentWitnesses(row).exists(witness =>
      witness.descriptorId == WitnessDescriptorId("sector_asymmetry_state") &&
        witness.anchor == WitnessAnchor.SectorAnchor(sector) &&
        witness.payload.get("majority_side").contains(WitnessValue.ColorValue(defenderColor))
    )

  private def sectorOf(file: File): WitnessSector =
    file.value match
      case 0 | 1 | 2 => WitnessSector.Queenside
      case 3 | 4 => WitnessSector.Center
      case _ => WitnessSector.Kingside

  private def contactTargetSquares(witness: lila.commentary.witness.Witness): Vector[Square] =
    witness.payload
      .get("contact_variants")
      .collect:
        case WitnessValue.ListValue(values) =>
          values.flatMap:
            case WitnessValue.ObjectValue(payload) =>
              payload
                .get("target_pawn_squares")
                .collect { case WitnessValue.SquareListValue(squares) => squares }
                .getOrElse(Vector.empty)
            case _ => Vector.empty
      .getOrElse(Vector.empty)

  private def isCenterFile(square: Square): Boolean =
    square.key.headOption.exists(file => Set('c', 'd', 'e', 'f').contains(file))

  private def isKingWing(square: Square): Boolean =
    square.key.headOption.exists(file => Set('f', 'g', 'h').contains(file))

  private def assertCoverageLiteralParity(row: ProjectionExpectationCorpus.Row): Unit =
    row.validatedCoveragePair.foreach:
      case ("shortcut_negative", bucket) =>
        assert(
          row.validatedForbiddenShortcuts.contains(bucket),
          clues(s"${row.id} shortcut-negative bucket $bucket must appear literally in forbiddenShortcuts")
        )
      case ("same_cluster_near_miss", bucket) if bucket.startsWith("vs_s") =>
        val rivalBand = bucket.stripPrefix("vs_").toUpperCase
        assert(
          row.validatedRivalBands.contains(rivalBand),
          clues(s"${row.id} same-cluster bucket $bucket must appear literally as rival band $rivalBand")
        )
      case _ => ()

  private def docsCoveragePairsFor(band: String): Set[(String, String)] =
    val docPath = java.nio.file.Paths.get("modules/commentary/docs/StrategyProjectionBoundaryMatrix.md")
    val lines = java.nio.file.Files.readAllLines(docPath).toArray.toVector.map(_.toString)
    val row =
      lines
        .find(line => line.startsWith(s"| `$band` |") && line.contains("shortcut_negative"))
        .getOrElse(fail(s"Missing StrategyProjectionBoundaryMatrix broad coverage row for $band"))
    val columns = row.split("\\|", -1).map(_.trim).toVector
    val coverageColumn =
      columns.lift(2).getOrElse(fail(s"Malformed StrategyProjectionBoundaryMatrix broad coverage row for $band"))
    "`([^`]+)`\\s*:\\s*([^;|]+)".r
      .findAllMatchIn(coverageColumn)
      .flatMap: axisMatch =>
        val axis = axisMatch.group(1)
        "`([^`]+)`".r.findAllMatchIn(axisMatch.group(2)).map(bucketMatch => axis -> bucketMatch.group(1))
      .toSet

  private def assertRequiredObjectFamilies(row: ProjectionExpectationCorpus.Row): Unit =
    if row.countableCoveragePair.nonEmpty &&
      row.expectation == "admitted" &&
      row.validatedRequiredObjectFamilies.nonEmpty
    then
      val current = objectExtractionFromFen(row.fen)
      row.validatedRequiredObjectFamilies.foreach: familyId =>
        assert(
          current.objects.forFamilyId(familyId).exists(obj => obj.color.forall(_ == row.validatedOwner)),
          clues(s"${row.id} missing required current object family $familyId for ${row.owner}")
        )

  private def assertRequiredDeltaFamilies(row: ProjectionExpectationCorpus.Row): Unit =
    if row.countableCoveragePair.nonEmpty &&
      row.expectation == "admitted" &&
      row.validatedRequiredDeltaFamilies.nonEmpty
    then
      val (before, move, after) =
        row.validatedDeltaCompanion.getOrElse(fail(s"${row.id} must declare exact delta transition fields"))
      val extraction =
        StrategicDeltaExtractor
          .fromFens(before, move, after)
          .fold(message => fail(s"Row ${row.id} delta extraction failed: $message"), identity)
      row.validatedRequiredDeltaFamilies.foreach: familyId =>
        assert(
          extraction.deltas.forFamilyId(familyId).exists(delta => delta.color.contains(row.validatedOwner)),
          clues(s"${row.id} missing required delta family $familyId for ${row.owner}")
        )

  private def assertRequiredCertificationFamilies(row: ProjectionExpectationCorpus.Row): Unit =
    if row.countableCoveragePair.nonEmpty &&
      row.expectation == "admitted" &&
      !StrategyProjectionScopeContract.isStartReadyBandId(row.validatedBand)
    then
      row.validatedRequiredCertificationFamilies.foreach: familyId =>
        val claim = certifiedClaimForRow(row, familyId)
        assertEquals(
          claim.verdict,
          CertificationVerdict.Certified,
          clues(s"${row.id} required certification $familyId is not certified for ${row.owner}")
        )

  private def assertKingOppositionContactSupport(row: ProjectionExpectationCorpus.Row): Unit =
    val contactSquare = rowAnchorSquare(row)
    val extraction =
      StrategySupportSeedExtractor
        .fromFen(row.normalizedFen)
        .fold(message => fail(s"Row ${row.id} support seed extraction failed: $message"), identity)
    assert(
      extraction.seeds
        .forSeedId(StrategySupportSeedScopeContract.S23KingOppositionContact)
        .exists: seed =>
          seed.anchor == WitnessAnchor.SquareAnchor(contactSquare) &&
            seed.color.contains(row.validatedOwner) &&
            seed.payload.get("relation").contains(WitnessValue.Token("direct_opposition")),
      clues(s"${row.id} missing exact-board S23 king-opposition support seed on ${contactSquare.key}")
    )

  private def assertS18ConversionLink(row: ProjectionExpectationCorpus.Row): Unit =
    if row.countableCoveragePair.contains("minor_edge_conversion_route" -> row.coverageBucket.getOrElse("")) &&
      row.band == "S18" &&
      row.expectation == "admitted"
    then
      val bishopSquares = bishopPairMemberSquares(row)
      val homeRank = if row.validatedOwner.white then Rank.First else Rank.Eighth
      assert(
        bishopSquares.exists(_.rank != homeRank),
        clues(s"${row.id} must use an active bishop-pair carrier, not only undeveloped home bishops")
      )
      row.coverageBucket.foreach:
        case "bishop_pair_to_material_or_endgame" =>
          val claim = certifiedClaimForRow(row, "MaterialHarvest")
          assertEquals(
            claim.payload.get("capturing_role"),
            Some(WitnessValue.RoleValue(Bishop)),
            clues(s"${row.id} material conversion must be carried by a bishop-pair member")
          )
          val captureFrom =
            claim.payload
              .get("capture_from")
              .collect { case WitnessValue.SquareValue(square) => square }
              .getOrElse(fail(s"${row.id} MaterialHarvest claim must expose capture_from"))
          assert(
            bishopSquares.contains(captureFrom),
            clues(s"${row.id} MaterialHarvest capture must start from a bishop-pair member square")
          )
        case "bishop_pair_to_initiative" | "bishop_pair_to_structure" =>
          row.validatedRequiredCertificationFamilies.foreach: familyId =>
            val claim = certifiedClaimForRow(row, familyId)
            assert(
              claim.support.targetSquares.nonEmpty,
              clues(s"${row.id} $familyId must expose exact support target squares")
            )
        case other =>
          fail(s"${row.id} uses unsupported S18 conversion bucket $other")

  private def assertS19SameTaskCertification(row: ProjectionExpectationCorpus.Row): Unit =
    if row.countableCoveragePair.contains("simplification_route" -> row.coverageBucket.getOrElse("")) &&
      row.band == "S19" &&
      row.expectation == "admitted"
    then
      val (before, _, after) =
        row.validatedDeltaCompanion.getOrElse(fail(s"${row.id} must declare exact S19 delta fields"))
      assert(
        row.normalizedFen == before || row.normalizedFen == after,
        clues(s"${row.id} current fen must be one endpoint of its TradeInvariant task")
      )
      assert(
        row.validatedRequiredCertificationFamilies.nonEmpty,
        clues(s"${row.id} must require same-task certification, not optional strengthening metadata")
      )
      row.coverageBucket.foreach:
        case "trade_invariant_to_material" =>
          assertEquals(row.validatedRequiredCertificationFamilies, Vector("MaterialHarvest"))
          assertEquals(row.normalizedFen, before, clues(s"${row.id} material certification must be on the move board"))
        case "trade_invariant_to_hold" =>
          assertEquals(row.validatedRequiredCertificationFamilies, Vector("FortressDrawCertification"))
          assertEquals(row.normalizedFen, after, clues(s"${row.id} hold certification must be on the post-trade board"))
          assert(
            tradeInvariantTargetSquares(row).intersect(objectSupportTargetSquares(row, "FortressHoldingShell")).nonEmpty,
            clues(s"${row.id} hold certification must share a support square with the exact TradeInvariant task")
          )
          assertFortressHoldDrawBudget(row, maxAbsCp = 250)
        case other =>
          fail(s"${row.id} uses unsupported S19 simplification bucket $other")

  private def assertFortressHoldDrawBudget(
      row: ProjectionExpectationCorpus.Row,
      maxAbsCp: Int
  ): Unit =
    val probe = StockfishProbe.probeFen(row.fen)
    assert(probe.bestMove.nonEmpty, clues(s"${row.id} fortress-hold probe returned no best move"))
    assert(
      probe.mate.forall(mate => math.abs(mate) > 2),
      clues(s"${row.id} fortress-hold board collapses into a too-short mate: ${probe.rawInfo.getOrElse("")}")
    )
    assert(
      probe.cp.exists(cp => math.abs(cp) <= maxAbsCp),
      clues(s"${row.id} fortress-hold board exceeds draw budget $maxAbsCp: ${probe.rawInfo.getOrElse("")}")
    )

  private def assertS22PerpetualHoldCycle(row: ProjectionExpectationCorpus.Row): Unit =
    if row.countableCoveragePair.contains("hold_route" -> "perpetual_hold") &&
      row.band == "S22" &&
      row.expectation == "admitted"
    then
      assertEquals(row.validatedRequiredCertificationFamilies, Vector("PerpetualCheckHolding"))
      assert(
        hasRenewableHeavyPieceCheckingCycle(row.normalizedFen),
        clues(s"${row.id} must expose a renewable heavy-piece checking cycle, not loose check narration")
      )

  private def certifiedClaimForRow(row: ProjectionExpectationCorpus.Row, familyId: String) =
    val purposes =
      CertificationExpectationCorpus.requiredEnginePurposesByFamily
        .getOrElse(familyId, fail(s"${row.id} uses unsupported required certification family $familyId"))
        .map: purposeKey =>
          CertificationEvidencePurpose
            .fromKey(purposeKey)
            .getOrElse(fail(s"${row.id} uses unsupported certification evidence purpose $purposeKey")) ->
            CertificationEvidenceStrength.Satisfied
        .toMap
    val current = objectExtractionFromFen(row.fen)
    val extraction =
      extractionFromFen(
        row.fen,
        evidenceBundleForFamily(
          current = current,
          familyId = familyId,
          color = row.validatedOwner,
          purposeStrengths = purposes
        )
      )
    ownerClaimForFamily(extraction, familyId, row.validatedOwner)

  private def certificationVerdictForRow(
      row: ProjectionExpectationCorpus.Row,
      familyId: String,
      purposeStrengths: Map[CertificationEvidencePurpose, CertificationEvidenceStrength]
  ): CertificationVerdict =
    val current = objectExtractionFromFen(row.fen)
    val extraction =
      extractionFromFen(
        row.fen,
        evidenceBundleForFamily(
          current = current,
          familyId = familyId,
          color = row.validatedOwner,
          purposeStrengths = purposeStrengths
        )
      )
    ownerClaimForFamily(extraction, familyId, row.validatedOwner).verdict

  private def bishopPairMemberSquares(row: ProjectionExpectationCorpus.Row): Vector[Square] =
    val primary =
      UWitnessExtractor
        .fromFen(row.normalizedFen)
        .fold(message => fail(s"Row ${row.id} U witness extraction failed: $message"), identity)
    val pair =
      primary.witnesses.all
        .find(witness =>
          witness.descriptorId == WitnessDescriptorId("bishop_pair_state") &&
            witness.color.contains(row.validatedOwner)
        )
        .getOrElse(fail(s"${row.id} must expose bishop_pair_state for ${row.owner}"))
    pair.payload
      .get("bishop_member_squares")
      .collect { case WitnessValue.SquareListValue(values) => values }
      .getOrElse(fail(s"${row.id} bishop_pair_state must expose member squares"))

  private def hasRenewableHeavyPieceCheckingCycle(fen: Fen.Full): Boolean =
    val extraction =
      StrategicObjectExtractor.fromFen(fen).fold(message => fail(message), identity)
    val lowLevel = UExtractionContext(extraction.rootState)
    val sideToMove =
      lowLevel.sideToMove.getOrElse(fail(s"Missing side-to-move for FEN $fen"))
    val position = Position(lowLevel.board.toBoard, variant.Standard, sideToMove)

    forcedRenewalFrom(position, Set.empty, mutable.Map.empty)

  private def legalMoves(position: Position): Vector[chess.Move] =
    Square.all
      .filter(square => position.pieceAt(square).exists(_.color == position.color))
      .flatMap(position.generateMovesAt)
      .sortBy(move => (move.orig.value, move.dest.value))
      .toVector

  private def checkingMoves(
      moves: Vector[chess.Move],
      pieceAt: Square => Option[chess.Piece]
  ): Vector[chess.Move] =
    moves.filter: move =>
      pieceAt(move.orig).exists(piece =>
        (piece.role == Queen || piece.role == Rook) &&
          move.after.check.yes &&
          legalMoves(move.after.position).nonEmpty
      )

  private def forcedRenewalFrom(
      position: Position,
      visiting: Set[String],
      memo: mutable.Map[String, Boolean]
  ): Boolean =
    val key = positionKey(position)
    if visiting.contains(key) then true
    else if memo.size > MaxRenewalStates then false
    else
      memo.get(key) match
        case Some(result) => result
        case None =>
          val nextVisiting = visiting + key
          val result =
            checkingMoves(legalMoves(position), position.board.pieceAt).exists: move =>
              val defenderReplies = legalMoves(move.after.position)
              defenderReplies.nonEmpty &&
                defenderReplies.forall(reply =>
                  forcedRenewalFrom(reply.after.position, nextVisiting, memo)
                )
          memo.update(key, result)
          result

  private def positionKey(position: Position): String =
    val pieces =
      Square.all
        .flatMap(square => position.pieceAt(square).map(piece => s"${square.key}:${piece.color}:${piece.role}"))
        .mkString(",")
    val enPassant =
      position.enPassantSquare.map(_.key).getOrElse("-")
    s"$pieces|${position.color}|${position.history.castles.value}|$enPassant"
