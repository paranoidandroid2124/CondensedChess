package lila.commentary.validation

import chess.{ Bishop, Color, File, Pawn, Position, Queen, Rank, Rook, Square }
import chess.format.Fen
import chess.variant

import scala.collection.mutable

import lila.commentary.certification.{
  CertificationEvidence,
  CertificationEvidenceBundle,
  CertificationEvidencePurpose,
  CertificationEvidenceStrength,
  CertificationId,
  CertificationVerdict
}
import lila.commentary.delta.{ StrategicDeltaExtraction, StrategicDeltaExtractor }
import lila.commentary.projection.{
  StrategyProjectionAdmission,
  StrategyProjectionBandId,
  StrategyProjectionCoverageContract,
  StrategyProjectionEvidence,
  StrategyProjectionEvidenceKind,
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
import lila.commentary.witness.seed.{
  StrategySupportSeedExtraction,
  StrategySupportSeedExtractor,
  StrategySupportSeedScopeContract
}
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
      assertS19PreAdmissionFreeze(row)
      assertS22PerpetualHoldCycle(row)
      assertS22PreAdmissionFreeze(row)

  test("projection broad-ready gates track staged waves under wave-7 global closure"):
    assertEquals(
      ProjectionExpectationCorpus.closedLegacyBroadBlockerBands,
      Set("S06", "S10", "S12")
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
      val reusesAdmittedBoardForEvidenceDowngrade =
        row.band == "S22" && row.admissionPath == "perpetual_deferred"
      assert(
        reusesAdmittedBoardForEvidenceDowngrade ||
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
          "minor_edge_conversion_route" -> "bishop_pair_to_material",
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
          "shortcut_negative" -> "fortress_shell_only",
          "shortcut_negative" -> "checking_sequence_wording",
          "shortcut_negative" -> "perpetual_deferred"
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

  test("wave-3 initiative release coverage rows complete with S05 live admission kept narrow"):
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
        row.evidenceClaimsForRuntime.map(_.kind.value),
        if row.band == "S05" && row.expectation == "admitted" then Vector("center_release_route_certified")
        else if row.band == "S07" then Vector("initiative_conversion_route_certified")
        else if row.band == "S08" && row.expectation == "admitted" then Vector("counterplay_denial_route_certified")
        else if row.band == "S21" && row.expectation == "admitted" then Vector("counterplay_survival_route_certified")
        else Vector.empty,
        clues(s"${row.id} wave-3 broad-ready row must only declare live evidence for S05/S07/S08/S21 rows")
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
        if row.band == "S05" || row.band == "S07" || row.band == "S08" || row.band == "S21" then Right(false)
        else Left(s"Unsupported projection admission band: ${row.band}"),
        clues(s"${row.id} wave-3 broad-ready row must not admit without live projection evidence")
      )
    StrategyProjectionCoverageContract.initiativeReleaseCoverageFreezeBandIds
      .filterNot(bandId => Set("S05", "S07", "S08", "S21").contains(bandId.value))
      .foreach: bandId =>
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

  test("S21 runtime admission gate matches docs, code, corpus, and adjacent boundaries"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/Witnesses61.md",
      "modules/commentary/docs/StrategySupportSeedInventory.md"
    ).map(docText).mkString("\n")

    assertEquals(liveBandIds.contains("S21"), true)
    assertEquals(coverageOnlyIds.contains("S21"), false)
    assertEquals(liveBandIds.contains("S01"), false, clues("S01 must remain coverage-only after S21 admission"))
    assertEquals(coverageOnlyIds.contains("S01"), true, clues("S01 coverage rows stay countable but non-live"))
    assertEquals(liveBandIds.contains("S08"), true, clues("S08 is intentionally live from its separate branch"))
    assertEquals(coverageOnlyIds.contains("S08"), false, clues("S08 must not be reclassified as coverage-only"))
    assertEquals(liveBandIds.contains("S05"), true, clues("S05 is intentionally live from its separate branch"))
    assertEquals(coverageOnlyIds.contains("S05"), false, clues("S05 must not be reclassified as coverage-only"))
    assertEquals(
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S21").map(_.value),
      Vector("counterplay_survival_route_certified")
    )
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S21").map(_.value),
      Vector("counterplay_survival_route_certified")
    )
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S05").map(_.value),
      Vector("center_release_route_certified"),
      clues("S05 must remain a distinct live branch, not an S21 side effect")
    )
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand.keySet.contains("S01"),
      false,
      clues("S01 must not gain runtime evidence through S21")
    )
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S08").map(_.value),
      Vector("counterplay_denial_route_certified"),
      clues("S08 keeps its distinct denial evidence instead of gaining S21 evidence")
    )
    assert(docs.contains("counterplay_survival_route_certified"))
    assert(docs.contains("S21 live runtime admission"))
    assert(docs.contains("counterplay survival route is start-ready"))
    assert(docs.contains("certified `InitiativeWindow` alone is not S21 proof"))
    assert(
      docs.contains(
        "`S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`"
      ),
      clues("S21 must be present in documented live runtime start-ready enumerations")
    )
    Vector(
      "`S05`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`",
      "`S05`, `S07`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`",
      "`S05`, `S07`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S22`, `S23`, `S24`, and `S25`",
      "`S05`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S22`, `S23`, `S24`, and `S25`",
      "`S05`/`S11`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S22`/`S23`/`S24`/`S25`"
    ).foreach: staleLiveSet =>
      assert(!docs.contains(staleLiveSet), clues(s"stale live set omitted S21: $staleLiveSet"))

    rows.filter(_.band == "S21").foreach: row =>
      if row.expectation == "admitted" then
        assertEquals(row.evidenceClaimsForRuntime.map(_.kind.value), Vector("counterplay_survival_route_certified"))
        val claim =
          row.evidenceClaimsForRuntime.headOption.getOrElse(fail(s"${row.id} admitted S21 row must carry evidence"))
        assertEquals(
          claim.payload.get("counterplay_survival_route"),
          Some(WitnessValue.Token(row.admissionPath)),
          clues(s"${row.id} S21 evidence must mirror the route task")
        )
        assertEquals(
          claim.payload.get("certification_family"),
          Some(WitnessValue.Token("InitiativeWindow")),
          clues(s"${row.id} S21 evidence must mirror the support certification family")
        )
      else assertEquals(row.evidenceClaimsForRuntime, Vector.empty)

      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity)
      val evidence = StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime)
      val certificationEvidence = certificationEvidenceForRuntime(row, extraction)
      val admitted =
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          extraction,
          evidence,
          row.validatedOwner,
          certificationEvidence
        ).fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)
      assertEquals(
        admitted,
        row.expectation == "admitted",
        clues(s"${row.id} S21 runtime boundary")
      )

  test("S07 runtime admission gate matches docs while runtime admits only exact initiative-conversion evidence"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/Witnesses61.md",
      "modules/commentary/docs/StrategySupportSeedInventory.md"
    ).map(docText).mkString("\n")

    assertEquals(liveBandIds.contains("S07"), true)
    assertEquals(coverageOnlyIds.contains("S07"), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S07").map(_.value), Vector("initiative_conversion_route_certified"))
    assertEquals(
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S07").map(_.value),
      Vector("initiative_conversion_route_certified")
    )
    assertEquals(
      StrategyProjectionCoverageContract.rowSpecificAdmissionBurdenFreezeByBand("S07"),
      Vector(
        "initiative_conversion_route_requires_opening_development_regime_and_same_owner_development_comparison",
        "initiative_conversion_route_requires_same_owner_certified_initiative_window",
        "projection_evidence_must_bind_same_owner_board_and_initiative_conversion_route",
        "development_only_initiative_only_s08_denial_or_optional_strengthening_is_non_admitting"
      )
    )
    assert(docs.contains("S07 live runtime admission"))
    assert(docs.contains("initiative_conversion_route_certified"))
    assert(docs.contains("S08 live runtime admission"))
    assert(docs.contains("DevelopmentComparison` and certified `InitiativeWindow`"))
    assert(docs.contains("OpeningDevelopmentRegime` is support-only"))

    rows.filter(_.band == "S07").foreach: row =>
      assertEquals(row.evidenceClaimsForRuntime.map(_.kind.value), Vector("initiative_conversion_route_certified"))
      val claim =
        row.evidenceClaimsForRuntime.headOption.getOrElse(fail(s"${row.id} S07 row must carry route evidence"))
      val routeEvidence = claim.payload.get("initiative_conversion_route")
      if row.expectation == "admitted" then
        assertEquals(
          routeEvidence,
          Some(WitnessValue.Token(row.admissionPath)),
          clues(s"${row.id} S07 admitted evidence must mirror the route task")
        )
      else
        assert(
          Set(
            Some(WitnessValue.Token("development_led_window")),
            Some(WitnessValue.Token("move_right_window"))
          ).contains(routeEvidence),
          clues(s"${row.id} S07 rejected row must still use a valid route token for fail-closed testing")
        )
      assertEquals(
        claim.payload.get("certification_family"),
        Some(WitnessValue.Token("InitiativeWindow")),
        clues(s"${row.id} S07 evidence must mirror lower InitiativeWindow support")
      )
      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity)
      val evidence = StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime)
      val certificationEvidence = certificationEvidenceForRuntime(row, extraction)
      val admitted =
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          extraction,
          evidence,
          row.validatedOwner,
          certificationEvidence
        ).fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)
      assertEquals(
        admitted,
        row.expectation == "admitted",
        clues(s"${row.id} S07 runtime boundary")
      )
      assertEquals(
        StrategyProjectionAdmission.admits(
          StrategyProjectionBandId("S07"),
          extraction,
          StrategyProjectionEvidence.empty,
          row.validatedOwner,
          certificationEvidence
        ),
        Right(false)
      )

  test("S08 runtime admission gate matches docs while runtime admits only exact denial evidence"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val evidenceKind = StrategyProjectionEvidenceKind("counterplay_denial_route_certified")
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/Witnesses61.md"
    ).map(docText).mkString("\n")

    assertEquals(liveBandIds.contains("S08"), true)
    assertEquals(coverageOnlyIds.contains("S08"), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S08"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(StrategyProjectionBandId("S08"), evidenceKind), true)
    assertEquals(
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S08"),
      Vector(evidenceKind)
    )
    assert(docs.contains("S08 live runtime admission"))
    assert(docs.contains("counterplay_denial_route_certified"))
    assert(docs.contains("wrong-source/wrong-target/wrong-route/stale-evidence rejection"))
    assert(docs.contains("same rival source, target, and denial route"))
    assert(docs.contains("InitiativeWindow` remains lower Certification support, not projection truth"))

    rows.filter(_.band == "S08").foreach: row =>
      if row.expectation == "admitted" then
        assertEquals(row.evidenceClaimsForRuntime.map(_.kind.value), Vector("counterplay_denial_route_certified"))
        val claim =
          row.evidenceClaimsForRuntime.headOption.getOrElse(fail(s"${row.id} admitted S08 row must carry evidence"))
        assertEquals(
          claim.payload.get("counterplay_denial_route"),
          Some(WitnessValue.Token(row.admissionPath)),
          clues(s"${row.id} S08 evidence must mirror the denial route task")
        )
        assertEquals(
          claim.payload.get("certification_family"),
          Some(WitnessValue.Token("InitiativeWindow")),
          clues(s"${row.id} S08 evidence must mirror lower InitiativeWindow support")
        )
        assertEquals(
          claim.anchor,
          WitnessAnchor.PieceSquareAnchor(Square.D7),
          clues(s"${row.id} S08 evidence must stay anchored to the exact rival source")
        )
      else assertEquals(row.evidenceClaimsForRuntime, Vector.empty)

      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(message), identity)
      val evidence = StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime)
      val certificationEvidence = certificationEvidenceForRuntime(row, extraction)
      val admitted =
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          extraction,
          evidence,
          row.validatedOwner,
          certificationEvidence
        ).fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)
      assertEquals(
        admitted,
        row.expectation == "admitted",
        clues(s"${row.id} S08 runtime boundary")
      )
      assertEquals(
        StrategyProjectionAdmission.admits(
          StrategyProjectionBandId("S08"),
          extraction,
          StrategyProjectionEvidence.empty,
          row.validatedOwner,
          certificationEvidence
        ),
        Right(false),
        clues(s"${row.id} S08 must reject without exact projection evidence")
      )
      row.countableCoveragePair.foreach:
        case ("denial_route", _) =>
          assert(row.validatedRequiredCertificationFamilies.contains("InitiativeWindow"))
          assert(row.validatedSupportWitnessIds.contains("pawn_push_break_contact_source"))
        case _ => ()

  test("S05 runtime admission completion gate matches docs, code, corpus, and adjacent boundaries"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value)
    val requiredEvidenceKinds =
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand.view.mapValues(_.map(_.value)).toMap
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/Witnesses61.md"
    ).map(docText).mkString("\n")

    assertEquals(liveBandIds, Vector("S05", "S06", "S07", "S08", "S11", "S13", "S14", "S15", "S16", "S17", "S18", "S19", "S21", "S22", "S23", "S24", "S25"))
    assertEquals(requiredEvidenceKinds("S05"), Vector("center_release_route_certified"))
    assertEquals(
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S05").map(_.value),
      requiredEvidenceKinds("S05")
    )
    assert(docs.contains("`S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`"))
    assert(docs.contains("non-`S05`/`S06`/`S07`/`S08`/`S11`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S21`/`S22`/`S23`/`S24`/`S25` bands"))
    assert(docs.contains("center_release_route_certified"))
    assert(docs.contains("center-file source"))
    assert(docs.contains("center-file target"))
    assert(docs.contains("center-release route"))

    assertEquals(coverageOnlyIds.contains("S05"), false)
    assertEquals(coverageOnlyIds.contains("S06"), false)
    assertEquals(coverageOnlyIds.contains("S21"), false)
    assertEquals(liveBandIds.contains("S06"), true)
    assertEquals(liveBandIds.contains("S21"), true)
    assertEquals(requiredEvidenceKinds("S06"), Vector("space_bind_restriction_route_certified"))
    assertEquals(requiredEvidenceKinds("S06").intersect(requiredEvidenceKinds("S05")), Vector.empty[String])
    assertEquals(liveBandIds.contains("S14"), true, clues("S14 was already an intentional live row, not promoted by S05"))
    assertEquals(requiredEvidenceKinds("S14"), Vector("chain_base_contact_route_certified"))
    assertEquals(requiredEvidenceKinds("S14").intersect(requiredEvidenceKinds("S05")), Vector.empty[String])
    assertEquals(requiredEvidenceKinds("S21"), Vector("counterplay_survival_route_certified"))
    assertEquals(requiredEvidenceKinds("S21").intersect(requiredEvidenceKinds("S05")), Vector.empty[String])
    assertEquals(coverageOnlyIds.intersect(liveBandIds.toSet), Set.empty[String])

    assertEquals(
      ProjectionExpectationCorpus.coveragePairsByBand(rows).getOrElse("S05", Set.empty),
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S05")
    )
    assertEquals(
      ProjectionExpectationCorpus.missingRequiredCoveragePairsByBand(rows).contains("S05"),
      false
    )
    assertEquals(
      ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows).contains("S05"),
      true
    )

    val s05Rows = rows.filter(_.band == "S05")
    assertEquals(s05Rows.count(_.expectation == "admitted"), 2)
    s05Rows.foreach: row =>
      if row.expectation == "admitted" then
        assertEquals(row.evidenceClaimsForRuntime.map(_.kind.value), Vector("center_release_route_certified"))
        assert(row.evidenceClaimsForRuntime.forall(_.payload.get("center_release_route").nonEmpty))
      else
        assertEquals(row.evidenceClaimsForRuntime, Vector.empty)

      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity)
      val evidence = StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime)
      val admitted =
        StrategyProjectionAdmission
          .admits(row.validatedBand, extraction, evidence, row.validatedOwner)
          .fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)
      assertEquals(admitted, row.expectation == "admitted", clues(s"${row.id} S05 runtime boundary"))

    Set("S06").foreach: band =>
      rows.filter(_.band == band).foreach: row =>
        assertEquals(
          StrategyProjectionAdmission.admits(
            row.validatedBand,
            StrategySupportSeedExtractor
              .fromFen(row.normalizedFen)
              .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity),
            StrategyProjectionEvidence.empty,
            row.validatedOwner
          ),
          Right(false),
          clues(s"${row.id} adjacent live row must not admit through S05 empty evidence")
        )
    rows.filter(_.band == "S21").foreach: row =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          StrategySupportSeedExtractor
            .fromFen(row.normalizedFen)
            .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity),
          StrategyProjectionEvidence.empty,
          row.validatedOwner
        ),
        Right(false),
        clues(s"${row.id} adjacent live S21 row must not admit through S05 evidence or empty evidence")
      )

  test("S06 runtime admission gate matches docs while broad coverage remains countable"):
    val s06 = StrategyProjectionBandId("S06")
    val evidenceKind = StrategyProjectionEvidenceKind("space_bind_restriction_route_certified")
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value)
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/Witnesses61.md",
      "modules/commentary/docs/StrategySupportSeedInventory.md"
    ).map(docText).mkString("\n")

    assertEquals(liveBandIds.contains("S06"), true)
    assertEquals(coverageOnlyIds.contains("S06"), false)
    assertEquals(StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S06"), Vector(evidenceKind))
    assertEquals(StrategyProjectionScopeContract.isAllowedEvidenceKind(s06, evidenceKind), true)
    assertEquals(
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S06"),
      Vector(evidenceKind)
    )
    assertEquals(StrategyProjectionCoverageContract.preLiveRuntimeBoundaryFreezeByBand.contains("S06"), false)
    assert(docs.contains("S06 live runtime admission"))
    assert(docs.contains("space_bind_restriction_route_certified"))
    assert(docs.contains("SpaceBindRestrictionCertification"))
    assert(docs.contains("exact S05 center-release and exact S20 domination false-rival"))
    assert(docs.contains("S06 Live Support Freeze"))
    assert(docs.contains("support-only lower certification"))

    val exactS05FalseRival =
      rows
        .find(_.id == "proj-s06-exact-s05-center-release-false-rival")
        .getOrElse(fail("missing exact S05 false-rival row for S06"))
    assertEquals(exactS05FalseRival.validatedCaseType, "comparative_false_rival")
    assertEquals(exactS05FalseRival.validatedExpectation, "rejected")
    assertEquals(exactS05FalseRival.validatedCoveragePair, Some("same_cluster_near_miss" -> "vs_s05"))
    assertEquals(exactS05FalseRival.validatedRivalBands.toSet, Set("S05", "S20"))
    assertEquals(hasOwnerWitness(exactS05FalseRival, "available_lever_trigger"), true)
    assertEquals(hasOwnerWitness(exactS05FalseRival, "pawn_push_break_contact_source"), true)
    assert(exactS05FalseRival.validatedRequiredObjectFamilies.contains("CentralContactFront"))
    assertEquals(exactS05FalseRival.evidenceClaimsForRuntime, Vector.empty)

    val exactS20FalseRival =
      rows
        .find(_.id == "proj-s06-exact-s20-domination-false-rival")
        .getOrElse(fail("missing exact S20 false-rival row for S06"))
    assertEquals(exactS20FalseRival.validatedCaseType, "comparative_false_rival")
    assertEquals(exactS20FalseRival.validatedExpectation, "rejected")
    assertEquals(exactS20FalseRival.validatedCoveragePair, Some("same_cluster_near_miss" -> "vs_s20"))
    assertEquals(exactS20FalseRival.validatedRivalBands.toSet, Set("S05", "S20"))
    assertEquals(hasOwnerWitness(exactS20FalseRival, "short_run_slider_gate_restriction"), true)
    assertEquals(exactS20FalseRival.validatedRequiredCertificationFamilies, Vector("MobilityComparison"))
    assertEquals(exactS20FalseRival.evidenceClaimsForRuntime, Vector.empty)

    rows.filter(_.band == "S06").foreach: row =>
      if row.expectation == "admitted" then
        assertEquals(row.evidenceClaimsForRuntime.map(_.kind.value), Vector("space_bind_restriction_route_certified"))
        assertEquals(row.validatedRequiredCertificationFamilies, Vector("SpaceBindRestrictionCertification"))
      else assertEquals(row.evidenceClaimsForRuntime, Vector.empty, clues(s"${row.id} rejected S06 row has no live evidence"))
      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity)
      val evidence = StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime)
      val certificationEvidence = certificationEvidenceForRuntime(row, extraction)
      val actual =
        StrategyProjectionAdmission
          .admits(row.validatedBand, extraction, evidence, row.validatedOwner, certificationEvidence = certificationEvidence)
          .fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)
      assertEquals(
        actual,
        row.expectation == "admitted",
        clues(s"${row.id} S06 live runtime boundary")
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
        "S07" -> Set("exact", "near_miss", "comparative_false_rival", "nasty_negative"),
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

  test("conversion and holding wave 2 coverage rows complete with S19 and S22 live admission expansion"):
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
        "S09",
        "S10",
        "S12",
        "S20"
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

  test("S22 runtime admission completion gate matches docs, code, corpus, and adjacent boundaries"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value)
    val requiredEvidenceKinds =
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand.view.mapValues(_.map(_.value)).toMap
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val decisionFreezeLedger = docText("modules/commentary/docs/DecisionFreezeLedger.md")
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/Witnesses61.md"
    ).map(docText).appended(decisionFreezeLedger).mkString("\n")

    assertEquals(liveBandIds, Vector("S05", "S06", "S07", "S08", "S11", "S13", "S14", "S15", "S16", "S17", "S18", "S19", "S21", "S22", "S23", "S24", "S25"))
    assert(docs.contains("`S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`"))
    assert(docs.contains("`S05`/`S06`/`S07`/`S08`/`S11`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S21`/`S22`/`S23`/`S24`/`S25`"))
    assert(decisionFreezeLedger.contains("does **not** authorize broad runtime behavior"))
    assert(decisionFreezeLedger.contains("does not authorize broad deployment or runtime behavior outside"))
    assert(decisionFreezeLedger.contains("narrow admission slices"))
    assert(decisionFreezeLedger.contains("All non-`S05`/`S06`/`S07`/`S08`/`S11`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S21`/`S22`/`S23`/`S24`/`S25` S-bands remain outside"))
    assert(decisionFreezeLedger.contains("No S05/S06/S07/S08/S11/S13/S14/S15/S16/S17/S18/S19/S21/S22/S23/S24/S25 blocker remains"))
    assertEquals(
      decisionFreezeLedger.contains("It does **not** mean live projection runtime already exists"),
      false
    )
    assertEquals(decisionFreezeLedger.contains("live projection runtime exists"), false)
    assertEquals(
      decisionFreezeLedger.contains("All non-`S17`/`S23`/`S24`/`S25` S-bands remain outside"),
      false
    )
    assertEquals(
      decisionFreezeLedger.contains("`S17` / `S23` / `S24` / `S25` start-ready slices"),
      false
    )
    assertEquals(
      requiredEvidenceKinds,
        Map(
          "S05" -> Vector("center_release_route_certified"),
          "S06" -> Vector("space_bind_restriction_route_certified"),
          "S07" -> Vector("initiative_conversion_route_certified"),
          "S08" -> Vector("counterplay_denial_route_certified"),
          "S11" -> Vector("weak_pawn_target_pressure_persistence_certified"),
          "S17" -> Vector("liability_relief_certified"),
          "S18" -> Vector(
            "bishop_pair_initiative_conversion_certified",
            "bishop_pair_structure_conversion_certified",
            "bishop_pair_material_conversion_certified"
          ),
          "S19" -> Vector(
            "trade_invariant_material_simplification_certified",
            "trade_invariant_hold_simplification_certified"
        ),
        "S13" -> Vector("wing_damage_route_certified"),
        "S14" -> Vector("chain_base_contact_route_certified"),
        "S15" -> Vector("passer_creation_route_certified"),
        "S16" -> Vector("passer_suppression_route_certified"),
        "S21" -> Vector("counterplay_survival_route_certified"),
        "S22" -> Vector("fortress_hold_certified", "perpetual_hold_certified"),
        "S23" -> Vector("king_entry_conversion_certified", "king_opposition_certified"),
        "S24" -> Vector("same_target_forcing_realization", "same_target_conversion_certified"),
        "S25" -> Vector("rank_access_consequence_certified")
      )
    )
    requiredEvidenceKinds("S22").foreach(kind => assert(docs.contains(kind)))
    assertEquals(
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S22").map(_.value),
      requiredEvidenceKinds("S22")
    )

    assertEquals(coverageOnlyIds.contains("S22"), false)
    assertEquals(coverageOnlyIds.contains("S05"), false)
    assertEquals(coverageOnlyIds.contains("S19"), false)
    assertEquals(coverageOnlyIds.contains("S18"), false)
    assertEquals(coverageOnlyIds.intersect(liveBandIds.toSet), Set.empty[String])
    assertEquals(
      requiredEvidenceKinds("S23"),
      Vector("king_entry_conversion_certified", "king_opposition_certified"),
      clues("S23 must remain on its pre-existing king-activity runtime evidence kinds")
    )
    assertEquals(
      requiredEvidenceKinds("S19"),
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S19").map(_.value),
      clues("S19 must expose only its frozen simplification runtime evidence kinds")
    )

    assertEquals(ProjectionExpectationCorpus.broadReadyCoverageGateBands, allProjectionBandIds)
    assertEquals(
      ProjectionExpectationCorpus.missingRequiredCoveragePairsByBand(rows),
      Map.empty[String, Set[(String, String)]]
    )
    assertEquals(ProjectionExpectationCorpus.bandsWithCompleteBroadReadyCoverage(rows), allProjectionBandIds)
    assertEquals(
      ProjectionExpectationCorpus.coveragePairsByBand(rows).getOrElse("S22", Set.empty),
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S22")
    )

    val coverageOnlyRows =
      rows.filter(row => coverageOnlyIds.contains(row.band))
    coverageOnlyRows.foreach: row =>
      assertEquals(row.evidenceClaimsForRuntime, Vector.empty)
      assertEquals(row.validatedRequiredSupportSeedIds, Vector.empty)
      assertEquals(
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          StrategySupportSeedExtractor
            .fromFen(row.normalizedFen)
            .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity),
          StrategyProjectionEvidence.empty,
          row.validatedOwner
        ),
        Left(s"Unsupported projection admission band: ${row.band}"),
        clues(s"${row.id} coverage-only row must stay outside live runtime admission")
      )

    val s22Rows = rows.filter(_.band == "S22")
    assertEquals(s22Rows.nonEmpty, true)
    s22Rows.foreach: row =>
      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity)
      val evidence = StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime)
      val certificationEvidence = certificationEvidenceForRuntime(row, extraction)
      val admitted =
        StrategyProjectionAdmission
          .admits(row.validatedBand, extraction, evidence, row.validatedOwner, certificationEvidence)
          .fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)
      assertEquals(admitted, row.expectation == "admitted", clues(s"${row.id} S22 runtime boundary"))
      if row.expectation == "admitted" then
        assertEquals(
          row.evidenceClaimsForRuntime.map(_.kind.value).toSet.subsetOf(requiredEvidenceKinds("S22").toSet),
          true
        )
      else
        assertEquals(row.evidenceClaimsForRuntime, Vector.empty)

  test("S18 runtime admission gate matches docs, code, corpus, and adjacent boundaries"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/Witnesses61.md"
    ).map(docText).mkString("\n")
    val burdenFreeze = StrategyProjectionCoverageContract.rowSpecificAdmissionBurdenFreezeByBand("S18")
    val evidenceFreeze =
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S18").map(_.value)

    assertEquals(liveBandIds.contains("S18"), true)
    assertEquals(coverageOnlyIds.contains("S18"), false)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S18"),
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S18")
    )
    assertEquals(
      burdenFreeze,
      Vector(
        "initiative_route_requires_bishop_pair_state_and_same_board_initiative_window",
        "structure_route_requires_bishop_pair_state_and_same_board_mobility_comparison",
        "material_route_requires_bishop_pair_member_material_harvest",
        "projection_evidence_mirrors_conversion_family_targets_and_bishop_pair",
        "relation_only_optional_strengthening_or_adjacent_rival_is_non_admitting"
      )
    )
    assertEquals(
      evidenceFreeze,
      Vector(
        "bishop_pair_initiative_conversion_certified",
        "bishop_pair_structure_conversion_certified",
        "bishop_pair_material_conversion_certified"
      )
    )
    burdenFreeze.foreach: law =>
      assert(docs.contains(law), clues(s"S18 runtime burden freeze $law must be documented"))
    evidenceFreeze.foreach: kind =>
      assert(docs.contains(kind), clues(s"S18 runtime evidence kind $kind must be documented"))
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(
          StrategyProjectionBandId("S18"),
          StrategyProjectionEvidenceKind(kind)
        ),
        true,
        clues(s"S18 evidence kind $kind must be live after explicit runtime admission")
      )
    assertEquals(
      ProjectionExpectationCorpus.coveragePairsByBand(rows).getOrElse("S18", Set.empty),
      StrategyProjectionCoverageContract.requiredCoveragePairsFor("S18")
    )
    rows.filter(_.band == "S18").foreach: row =>
      assertEquals(row.validatedRequiredSupportSeedIds, Vector.empty)
      if row.expectation == "admitted" then
        assertEquals(
          row.evidenceClaimsForRuntime.map(_.kind.value),
          row.coverageBucket match
            case Some("bishop_pair_to_initiative") =>
              Vector("bishop_pair_initiative_conversion_certified")
            case Some("bishop_pair_to_structure") =>
              Vector("bishop_pair_structure_conversion_certified")
            case Some("bishop_pair_to_material") =>
              Vector("bishop_pair_material_conversion_certified")
            case other => fail(s"${row.id} admitted S18 row has unsupported bucket $other")
        )
      else
        assertEquals(
          row.evidenceClaimsForRuntime,
          Vector.empty,
          clues(s"${row.id} rejected S18 row must not carry shortcut projection evidence")
        )

      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity)
      val evidence = StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime)
      val certificationEvidence = certificationEvidenceForRuntime(row, extraction)
      val admitted =
        StrategyProjectionAdmission
          .admits(row.validatedBand, extraction, evidence, row.validatedOwner, certificationEvidence)
          .fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)
      assertEquals(admitted, row.expectation == "admitted", clues(s"${row.id} S18 runtime boundary"))

    assertEquals(liveBandIds.contains("S12"), false)
    assertEquals(liveBandIds.contains("S20"), false)
    Vector("S12", "S20").foreach: band =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          StrategyProjectionBandId(band),
          StrategySupportSeedExtractor
            .fromFen(rows.find(_.band == band).getOrElse(rows.head).normalizedFen)
            .fold(message => fail(s"FEN failed for $band boundary: $message"), identity),
          StrategyProjectionEvidence.empty,
          Color.White
        ),
        Left(s"Unsupported projection admission band: $band"),
        clues(s"$band adjacent coverage-only row must not become live with S18")
      )

  test("S19 runtime admission gate matches docs, code, corpus, and runtime boundary"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/StrategySupportSeedInventory.md",
      "modules/commentary/docs/Witnesses61.md"
    ).map(docText).mkString("\n")
    val burdenFreeze = StrategyProjectionCoverageContract.rowSpecificAdmissionBurdenFreezeByBand("S19")
    val evidenceFreeze =
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S19").map(_.value)
    val staleS19AdmissionFragments = Vector(
      "Only `S17`, `S22`, `S23`, `S24`, and `S25` are current live-runtime start-ready",
      "(`S17`, `S22`, `S23`, `S24`, and `S25`)",
      "For `S17`, `S22`, `S23`, `S24`, and `S25`,",
      "for `S17`, `S22`, `S23`, `S24`, and `S25` it does",
      "current `S17`/`S22`/`S23`/`S24`/`S25` start-ready",
      "`S17`, `S22`, `S23`, `S24`, and `S25` retain only their current narrow",
      "all non-`S17`/`S22`/`S23`/`S24`/`S25`",
      "`S17` is start-ready for projection implementation, not live projection runtime",
      "`S23` is start-ready for projection implementation, not live projection runtime",
      "`S24` is start-ready for projection implementation, not live projection runtime",
      "Any later `S17` projection admission",
      "Any later `S23` projection admission"
    )

    assertEquals(liveBandIds.contains("S19"), true)
    assertEquals(liveBandIds, Set("S05", "S06", "S07", "S08", "S11", "S13", "S14", "S15", "S16", "S17", "S18", "S19", "S21", "S22", "S23", "S24", "S25"))
    assertEquals(coverageOnlyIds.contains("S19"), false)
    assertEquals(coverageOnlyIds.contains("S22"), false)
    assertEquals(coverageOnlyIds.contains("S24"), false)
    assertEquals(
      liveBandIds.intersect(coverageOnlyIds),
      Set.empty[String],
      clues("live projection rows must not overlap coverage-only rows")
    )
    staleS19AdmissionFragments.foreach: fragment =>
      assert(!docs.contains(fragment), clues(s"stale S19 non-live doc fragment must not remain: $fragment"))
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S19"),
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S19")
    )
    val adjacentRequiredEvidenceKinds = Map(
      "S22" -> Vector("fortress_hold_certified", "perpetual_hold_certified"),
      "S24" -> Vector("same_target_forcing_realization", "same_target_conversion_certified")
    )
    adjacentRequiredEvidenceKinds.foreach: (band, expectedKinds) =>
      val actualKinds = StrategyProjectionScopeContract.requiredEvidenceKindsByBand(band).map(_.value)
      assertEquals(actualKinds, expectedKinds, clues(s"$band adjacent runtime evidence must stay unchanged"))
      assertEquals(
        actualKinds.toSet.intersect(evidenceFreeze.toSet),
        Set.empty[String],
        clues(s"$band must not reuse S19 simplification evidence kinds")
      )
    burdenFreeze.foreach: law =>
      assert(docs.contains(law), clues(s"S19 admission burden freeze $law must be documented"))
    evidenceFreeze.foreach: kind =>
      assert(docs.contains(kind), clues(s"S19 runtime evidence kind $kind must be documented"))
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(
          StrategyProjectionBandId("S19"),
          StrategyProjectionEvidenceKind(kind)
        ),
        true,
        clues(s"S19 evidence kind $kind must be live after explicit runtime admission")
      )
    rows.filter(_.band == "S19").foreach: row =>
      assertEquals(
        row.validatedRequiredSupportSeedIds,
        Vector.empty,
        clues(s"${row.id} S19 runtime admission must use delta/certification carriers, not support seeds")
      )
      if row.expectation == "admitted" then
        assertEquals(
          row.evidenceClaimsForRuntime.map(_.kind.value),
          row.coverageBucket match
            case Some("trade_invariant_to_material") =>
              Vector("trade_invariant_material_simplification_certified")
            case Some("trade_invariant_to_hold") =>
              Vector("trade_invariant_hold_simplification_certified")
            case other => fail(s"${row.id} admitted S19 row has unsupported bucket $other")
        )
      else
        assertEquals(
          row.evidenceClaimsForRuntime,
          Vector.empty,
          clues(s"${row.id} rejected S19 row must not carry shortcut projection evidence")
        )

      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity)
      val evidence = StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime)
      val certificationEvidence = certificationEvidenceForRuntime(row, extraction)
      val admitted =
        StrategyProjectionAdmission
          .admits(
            row.validatedBand,
            extraction,
            evidence,
            row.validatedOwner,
            certificationEvidence,
            deltaExtractionForRuntime(row)
          )
          .fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)

      assertEquals(admitted, row.expectation == "admitted", clues(s"${row.id} S19 runtime boundary"))

  test("wave-5 pawn-target structural damage coverage rows complete with S11/S13/S14 live admission kept separate"):
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
          "same_cluster_near_miss" -> "vs_s15",
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
        if band == "S11" || band == "S13" || band == "S14" then Right(false)
        else Left(s"Unsupported projection admission band: $band"),
        clues(s"$band broad-ready coverage alone must not auto-expand live projection admission")
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
      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(message), identity)
      if row.band == "S11" || row.band == "S13" || row.band == "S14" then
        if row.expectation == "admitted" then
          assertEquals(row.evidenceClaimsForRuntime.size, 1)
        else
          assertEquals(
            row.evidenceClaimsForRuntime,
            Vector.empty,
            clues(s"${row.id} rejected live wave-5 row must not carry shortcut projection evidence")
          )
        val admitted =
          StrategyProjectionAdmission
            .admits(
              row.validatedBand,
              extraction,
              StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime),
              row.validatedOwner
            )
            .fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)
        assertEquals(admitted, row.expectation == "admitted", clues(s"${row.id} live wave-5 runtime boundary"))
      else
        assertEquals(
          row.evidenceClaimsForRuntime,
          Vector.empty,
          clues(s"${row.id} wave-5 broad-ready row must not declare runtime projection evidence")
        )
        assertEquals(
          StrategyProjectionAdmission.admits(
            row.validatedBand,
            extraction,
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

    assertEquals(wave5CoverageRows.size, 21)
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

  test("S11 runtime admission gate matches docs, code, corpus, and adjacent boundaries"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/Witnesses61.md"
    ).map(docText).mkString("\n")
    val burdenFreeze = StrategyProjectionCoverageContract.rowSpecificAdmissionBurdenFreezeByBand("S11")
    val evidenceFreeze =
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S11").map(_.value)
    val staleS11NonLiveFragments = Vector(
      "S11 staged admission uses no support seed",
      "S11 now has a staged runtime-admission freeze only",
      "S11 has a staged runtime-admission freeze, but it remains non-live",
      "S11 stays in `coverageOnlyBandIds`",
      "`S11`, `S13`, and `S14` are broad-ready coverage-only bands, not live",
      "`S11` / `S13` / `S14` / `S15` / `S16` in the pawn-structure conversion cluster remain coverage-only"
    )

    assertEquals(liveBandIds.contains("S11"), true)
    assertEquals(coverageOnlyIds.contains("S11"), false)
    assertEquals(liveBandIds.contains("S13"), true)
    assertEquals(coverageOnlyIds.contains("S13"), false)
    assertEquals(liveBandIds.contains("S14"), true)
    assertEquals(coverageOnlyIds.contains("S14"), false)
    assertEquals(liveBandIds.intersect(coverageOnlyIds), Set.empty[String])
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S11"),
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S11")
    )
    assertEquals(
      burdenFreeze,
      Vector(
        "target_pressure_route_requires_same_square_weak_pawn_target_and_owner_pressure",
        "fixation_route_requires_current_fixed_pawn_persistence_on_same_target",
        "repeated_pressure_route_requires_two_owner_attackers_on_same_target",
        "projection_evidence_mirrors_weak_pawn_target_pressure_and_persistence",
        "weak_pawn_only_target_swap_adjacent_rival_or_optional_strengthening_is_non_admitting"
      )
    )
    assertEquals(evidenceFreeze, Vector("weak_pawn_target_pressure_persistence_certified"))
    staleS11NonLiveFragments.foreach: fragment =>
      assert(!docs.contains(fragment), clues(s"stale S11 non-live doc fragment must not remain: $fragment"))
    burdenFreeze.foreach: law =>
      assert(docs.contains(law), clues(s"S11 runtime admission burden freeze $law must be documented"))
    evidenceFreeze.foreach: kind =>
      assert(docs.contains(kind), clues(s"S11 runtime projection evidence kind $kind must be documented"))
      assertEquals(
        StrategyProjectionScopeContract.isAllowedEvidenceKind(
          StrategyProjectionBandId("S11"),
          StrategyProjectionEvidenceKind(kind)
        ),
        true,
        clues(s"S11 evidence kind $kind must be live after explicit runtime admission")
      )
    rows.filter(_.band == "S11").foreach: row =>
      assertEquals(row.validatedRequiredSupportSeedIds, Vector.empty)
      assertEquals(row.validatedRequiredObjectFamilies, Vector.empty)
      assertEquals(row.validatedRequiredDeltaFamilies, Vector.empty)
      assertEquals(row.validatedRequiredCertificationFamilies, Vector.empty)
      if row.expectation == "admitted" then
        val target = rowAnchorSquare(row)
        val attackers = ownerAttackersTo(row, target)
        val claim =
          row.evidenceClaimsForRuntime.headOption.getOrElse(
            fail(s"${row.id} admitted S11 row must carry exact projection evidence")
          )
        assertEquals(row.evidenceClaimsForRuntime.map(_.kind.value), evidenceFreeze)
        assertEquals(claim.anchor, WitnessAnchor.SquareAnchor(target))
        assertEquals(claim.payload.get("target_square"), Some(WitnessValue.SquareValue(target)))
        assertEquals(claim.payload.get("persistence_kind"), Some(WitnessValue.Token("fixed")))
        assertEquals(
          claim.payload.get("pressure_route"),
          row.coverageBucket.map(WitnessValue.Token.apply)
        )
        assertEquals(
          claim.payload.get("pressure_source_squares").collect { case WitnessValue.SquareListValue(values) =>
            values.toSet
          },
          Some(attackers.toSet),
          clues(s"${row.id} S11 projection evidence must mirror exact same-target attackers")
        )
        assert(
          hasSameTargetPersistenceProof(row, target),
          clues(s"${row.id} S11 live row must keep fixed-pawn persistence on the same target")
        )
      else
        assertEquals(row.evidenceClaimsForRuntime, Vector.empty)

      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(message), identity)
      val admitted =
        StrategyProjectionAdmission
          .admits(
            row.validatedBand,
            extraction,
            StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime),
            row.validatedOwner
          )
          .fold(message => fail(s"Row ${row.id} admission failed: $message"), identity)
      assertEquals(admitted, row.expectation == "admitted", clues(s"${row.id} S11 runtime boundary"))

    Vector("S14").foreach: band =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          StrategyProjectionBandId(band),
          StrategySupportSeedExtractor
            .fromFen(rows.find(_.band == band).getOrElse(rows.head).normalizedFen)
            .fold(message => fail(s"FEN failed for $band boundary: $message"), identity),
          StrategyProjectionEvidence.empty,
          Color.White
        ),
        Right(false),
        clues(s"$band adjacent pawn-target row must not auto-admit with empty evidence")
      )

  test("S13 live admission freeze matches docs while runtime admits only exact wing-damage evidence"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/Witnesses61.md"
    ).map(docText).mkString("\n")
    val burdenFreeze = StrategyProjectionCoverageContract.rowSpecificAdmissionBurdenFreezeByBand("S13")
    val evidenceFreeze =
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S13").map(_.value)

    assertEquals(liveBandIds.contains("S13"), true)
    assertEquals(coverageOnlyIds.contains("S13"), false)
    assertEquals(liveBandIds.contains("S11"), true)
    assertEquals(coverageOnlyIds.contains("S11"), false)
    assertEquals(liveBandIds.contains("S14"), true)
    assertEquals(coverageOnlyIds.contains("S14"), false)
    assertEquals(liveBandIds.contains("S15"), true)
    assertEquals(coverageOnlyIds.contains("S15"), false)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S13").map(_.value),
      evidenceFreeze
    )
    assertEquals(liveBandIds.intersect(coverageOnlyIds), Set.empty[String])
    assertEquals(
      burdenFreeze,
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
    assertEquals(evidenceFreeze, Vector("wing_damage_route_certified"))
    assertEquals(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S13"),
      Vector(
        "Witness:sector_asymmetry_state",
        "Witness:available_lever_trigger(same_wing_sector)",
        "Witness:pawn_push_break_contact_source(same_anchor_stronger_wing_mass_target)",
        "ProjectionValidation:wing_sector_non_chain_base_phalanx_edge_target|structurally_burdened_target",
        "SupportOnly:weak_pawn_target_state|file_lane_state"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.helperLawsByBand("S13"),
      Vector(
        "same_sector_asymmetry_plus_contact_source_law",
        "wing_damage_target_role_recomputed_from_exact_board_law",
        "phalanx_edge_target_excludes_chain_base_law",
        "center_sector_damage_is_not_wing_damage_law",
        "same_task_projection_evidence_must_mirror_s13_source_target_sector_and_route_law",
        "asymmetry_or_preexisting_weak_pawn_is_not_damage_law"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S13"),
      Vector(
        "sector_asymmetry_present",
        "same_sector_lever_and_break_contact_present",
        "wing_sector_damage_route_present",
        "contact_target_role_is_non_chain_base_phalanx_edge_or_structurally_burdened",
        "weak_pawn_or_asymmetry_only_not_counted",
        "stale_or_adjacent_runtime_evidence_not_counted_before_live_admission"
      )
    )
    (burdenFreeze ++ evidenceFreeze ++
      StrategyProjectionCoverageContract.helperLawsByBand("S13") ++
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S13")).foreach: token =>
      assert(docs.contains(token), clues(s"S13 live freeze token $token must be documented"))
    rows.filter(_.band == "S13").foreach: row =>
      assertEquals(row.validatedRequiredSupportSeedIds, Vector.empty)
      assertEquals(row.validatedRequiredObjectFamilies, Vector.empty)
      assertEquals(row.validatedRequiredDeltaFamilies, Vector.empty)
      assertEquals(row.validatedRequiredCertificationFamilies, Vector.empty)
      if row.expectation == "admitted" then
        assertEquals(row.validatedEvidenceKinds.map(_.value), Vector("wing_damage_route_certified"))
        row.evidenceClaimsForRuntime.foreach: claim =>
          assertEquals(claim.payload.get("target_square").nonEmpty, true, clues(s"${row.id} S13 target mirror"))
          assertEquals(claim.payload.get("contact_source_square").nonEmpty, true, clues(s"${row.id} S13 source mirror"))
          assertEquals(claim.payload.get("damage_route").nonEmpty, true, clues(s"${row.id} S13 route mirror"))
          assertEquals(claim.payload.get("damage_sector").nonEmpty, true, clues(s"${row.id} S13 sector mirror"))
      else assertEquals(row.evidenceClaimsForRuntime, Vector.empty)
      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(message), identity)
      assertEquals(
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          extraction,
          StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime),
          row.validatedOwner
        ),
        Right(row.expectation == "admitted"),
        clues(s"${row.id} S13 live runtime boundary")
      )
    Vector("S14", "S15").foreach: band =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          StrategyProjectionBandId(band),
          StrategySupportSeedExtractor
            .fromFen(Fen.Full.clean("4k3/8/8/8/1pp5/8/P7/4K3 w - - 0 1"))
            .fold(message => fail(message), identity),
          StrategyProjectionEvidence.empty,
          Color.White
        ),
        Right(false),
        clues(s"$band adjacent row must not auto-admit while S13 is live")
      )

  test("S14 live admission freeze matches docs while runtime admits only exact chain-base evidence"):
    val liveBandIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
    val coverageOnlyIds = StrategyProjectionCoverageContract.coverageOnlyBandIds.map(_.value).toSet
    val docs = Vector(
      "modules/commentary/docs/StrategyProjectionBoundaryMatrix.md",
      "modules/commentary/docs/CommentaryCoreSSOT.md",
      "modules/commentary/docs/ValidationMethodology.md",
      "modules/commentary/docs/DecisionFreezeLedger.md",
      "modules/commentary/docs/Witnesses61.md"
    ).map(docText).mkString("\n")
    val burdenFreeze = StrategyProjectionCoverageContract.rowSpecificAdmissionBurdenFreezeByBand("S14")
    val evidenceFreeze =
      StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S14").map(_.value)

    assertEquals(liveBandIds.contains("S14"), true)
    assertEquals(coverageOnlyIds.contains("S14"), false)
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand("S14").map(_.value),
      evidenceFreeze
    )
    assertEquals(
      burdenFreeze,
      Vector(
        "chain_base_route_requires_same_anchor_lever_and_contact_source",
        "chain_base_route_requires_recomputed_non_center_chain_base_target",
        "base_contact_continuation_requires_same_source_target_pair",
        "projection_evidence_mirrors_owner_source_target_and_chain_base_route",
        "fixed_chain_structural_shell_adjacent_rival_or_optional_strengthening_is_non_admitting"
      )
    )
    assertEquals(evidenceFreeze, Vector("chain_base_contact_route_certified"))
    assertEquals(
      StrategyProjectionCoverageContract.lowerCarrierOwnersByBand("S14"),
      Vector(
        "Witness:available_lever_trigger(base_contact)",
        "Witness:pawn_push_break_contact_source(same_anchor_chain_base_target)",
        "ProjectionValidation:non_center_chain_base_target_recomputed|base_contact_continuation",
        "SupportOnly:weak_pawn_target_state|structural_damage_shell|fixed_chain_context"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.helperLawsByBand("S14"),
      Vector(
        "same_anchor_chain_base_contact_law",
        "non_center_chain_base_role_recomputed_from_exact_board_law",
        "base_contact_must_bind_same_source_and_target_law",
        "same_task_projection_evidence_must_mirror_s14_source_target_and_route_law",
        "fixed_chain_or_structural_damage_shell_is_not_truth_owner_law"
      )
    )
    assertEquals(
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S14"),
      Vector(
        "base_contact_lever_and_break_contact_present",
        "contact_target_role_is_non_center_chain_base",
        "base_contact_continuation_same_anchor_present",
        "fixed_chain_or_structural_damage_shell_only_not_counted",
        "stale_or_adjacent_runtime_evidence_not_counted_before_live_admission"
      )
    )
    (burdenFreeze ++ evidenceFreeze ++
      StrategyProjectionCoverageContract.helperLawsByBand("S14") ++
      StrategyProjectionCoverageContract.exactValidationScaffoldByBand("S14")).foreach: token =>
      assert(docs.contains(token), clues(s"S14 live freeze token $token must be documented"))
    Vector(
      "S14 remains coverage-only",
      "`S14` remains a broad-ready coverage-only band",
      "`S14` is still a broad-ready coverage-only band",
      "`StrategyProjectionAdmission` must still reject S14",
      "outside `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` while S14"
    ).foreach: fragment =>
      assert(!docs.contains(fragment), clues(s"stale S14 non-live doc fragment must not remain: $fragment"))
    Vector(
      "`S11`, `S13`, `S17`, `S18`, `S19`, `S22`, `S23`, `S24`, and `S25`",
      "`S11`, `S13`, `S17`, `S18`, `S19`, `S22`, and `S24`",
      "`S11`/`S13`/`S17`/`S18`/`S19`/`S22`/`S23`/`S24`/`S25`",
      "current S11/S13/S17/S18/S19/S22/S23/S24/S25 blockers",
      "current `S11`/`S13`/`S17`/`S18`/`S19`/`S22`/`S23`/`S24`/`S25` start-ready",
      "- `S11`\n- `S13`\n- `S17`\n- `S18`\n- `S19`\n- `S22`\n- `S23`\n- `S24`\n- `S25`"
    ).foreach: fragment =>
      assert(!docs.contains(fragment), clues(s"stale S14-omitting live-set doc fragment must not remain: $fragment"))
    rows.filter(_.band == "S14").foreach: row =>
      assertEquals(row.validatedRequiredSupportSeedIds, Vector.empty)
      assertEquals(row.validatedRequiredObjectFamilies, Vector.empty)
      assertEquals(row.validatedRequiredDeltaFamilies, Vector.empty)
      assertEquals(row.validatedRequiredCertificationFamilies, Vector.empty)
      if row.expectation == "admitted" then
        assertEquals(row.validatedEvidenceKinds.map(_.value), Vector("chain_base_contact_route_certified"))
        val claim =
          row.evidenceClaimsForRuntime.headOption.getOrElse(
            fail(s"${row.id} admitted S14 row must carry exact projection evidence")
          )
        val target = rowAnchorSquare(row)
        assertEquals(claim.anchor, WitnessAnchor.SquareAnchor(target))
        assertEquals(claim.payload.get("target_square"), Some(WitnessValue.SquareValue(target)))
        assertEquals(claim.payload.get("contact_source_square").nonEmpty, true)
        assertEquals(
          claim.payload.get("chain_base_route"),
          row.coverageBucket.map(WitnessValue.Token.apply)
        )
        val forwardSquares =
          chainBaseForwardSupportSquares(exactUContext(row), !row.validatedOwner, target)
        assert(
          forwardSquares.nonEmpty,
          clues(s"${row.id} admitted S14 row must recompute exact chain-base forward support")
        )
        assertEquals(
          claim.payload.get("chain_base_forward_squares").collect { case WitnessValue.SquareListValue(values) =>
            values.toSet
          },
          Some(forwardSquares.toSet),
          clues(s"${row.id} S14 projection evidence must mirror exact chain-base forward support")
        )
      else
        assertEquals(row.evidenceClaimsForRuntime, Vector.empty)
      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(message), identity)
      assertEquals(
        StrategyProjectionAdmission.admits(
          row.validatedBand,
          extraction,
          StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime),
          row.validatedOwner
        ),
        Right(row.expectation == "admitted"),
        clues(s"${row.id} S14 live runtime boundary")
      )

  test("wave-6 passer creation suppression coverage rows complete with S15 and S16 live admission kept separate"):
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
          "shortcut_negative" -> "create_passer_shell_only",
          "shortcut_negative" -> "existing_passer_entity_only",
          "shortcut_negative" -> "split_anchor_creation_route"
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
      val emptyBoardExtraction =
        StrategySupportSeedExtractor
          .fromFen(Fen.Full.clean("7k/8/8/8/8/8/8/4K3 w - - 0 1"))
          .fold(message => fail(message), identity)
      if band == "S15" || band == "S16" then
        assertEquals(
          StrategyProjectionAdmission.admits(
            StrategyProjectionBandId(band),
            emptyBoardExtraction,
            StrategyProjectionEvidence.empty,
            chess.Color.White
          ),
          Right(false),
          clues(s"$band live runtime admission must require exact current-board evidence")
        )
      else
        assertEquals(
          StrategyProjectionAdmission.admits(
            StrategyProjectionBandId(band),
            emptyBoardExtraction,
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
      if row.band == "S15" && row.expectation == "admitted" then
        assertEquals(
          row.validatedEvidenceKinds.map(_.value),
          Vector("passer_creation_route_certified"),
          clues(s"${row.id} admitted S15 live row must declare exact projection evidence")
        )
      else if row.band == "S16" && row.expectation == "admitted" then
        assertEquals(
          row.validatedEvidenceKinds.map(_.value),
          Vector("passer_suppression_route_certified"),
          clues(s"${row.id} admitted S16 live row must declare exact projection evidence")
        )
      else
        assertEquals(
          row.evidenceClaimsForRuntime,
          Vector.empty,
          clues(s"${row.id} rejected wave-6 row must not declare runtime projection evidence")
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
      val extraction =
        StrategySupportSeedExtractor
          .fromFen(row.normalizedFen)
          .fold(message => fail(message), identity)
      if row.band == "S15" || row.band == "S16" then
        assertEquals(
          StrategyProjectionAdmission.admits(
            row.validatedBand,
            extraction,
            StrategyProjectionEvidence.forSeedExtraction(extraction, row.evidenceClaimsForRuntime),
            row.validatedOwner,
            certificationEvidenceForRuntime(row, extraction)
          ),
          Right(row.expectation == "admitted"),
          clues(s"${row.id} wave-6 live runtime admission must match exact row expectation")
        )
      else
        assertEquals(
          StrategyProjectionAdmission.admits(
            row.validatedBand,
            extraction,
            StrategyProjectionEvidence.empty,
            row.validatedOwner
          ),
          Left(s"Unsupported projection admission band: ${row.band}"),
          clues(s"${row.id} coverage-only wave-6 row must stay fail-closed at live admission")
        )

  test("wave-6 passer creation suppression rows keep exact-board lower carriers"):
    val wave6Bands =
      StrategyProjectionCoverageContract.passerCreationSuppressionCoverageFreezeBandIds.map(_.value).toSet
    val wave6CoverageRows =
      rows.filter(row => wave6Bands.contains(row.band) && row.countableCoveragePair.nonEmpty)

    assertEquals(wave6CoverageRows.size, 17)
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

  test("projection start-ready scaffold covers runtime exact and rejected boundary rows"):
    StrategyProjectionScopeContract.startReadyBandIds.map(_.value).foreach: band =>
      val actualCaseTypes = caseTypesByBand.getOrElse(band, Set.empty)
      val requiredCaseTypes =
        if band == "S11" || band == "S13" || band == "S14" || band == "S15" || band == "S16" then
          Set("exact", "comparative_false_rival", "nasty_negative")
        else if band == "S05" || band == "S06" || band == "S08" || band == "S18" || band == "S19" || band == "S21" || band == "S22" then
          Set("exact", "near_miss", "nasty_negative")
        else ProjectionExpectationCorpus.requiredStartReadyCaseTypes
      assertEquals(
        requiredCaseTypes.subsetOf(actualCaseTypes),
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
        val certificationEvidence =
          certificationEvidenceForRuntime(row, extraction)
        val admitted =
          StrategyProjectionAdmission
            .admits(
              row.validatedBand,
              extraction,
              evidence,
              row.validatedOwner,
              certificationEvidence,
              deltaExtractionForRuntime(row)
            )
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
      case ("center_release_route", bucket) =>
        assert(
          hasS05ExactCenterReleaseCarrier(row),
          clues(s"${row.id} S05 route must expose same-source lever and break-contact on a center target")
        )
        bucket match
          case "center_pawn_target" =>
            assert(
              row.validatedSupportWitnessIds.contains("available_lever_trigger"),
              clues(s"${row.id} S05 center-target route must mark available lever support")
            )
          case "central_axis_continuation" =>
            assert(
              row.validatedSupportWitnessIds.contains("pawn_push_break_contact_source"),
              clues(s"${row.id} S05 central-axis route must mark break-contact continuation support")
            )
          case other => fail(s"${row.id} unsupported S05 center-release bucket $other")
        assertEquals(
          StrategyProjectionCoverageContract.projectionEvidenceKindFreezeByBand("S05").map(_.value),
          Vector("center_release_route_certified"),
          clues(s"${row.id} S05 live evidence kind must match the frozen admission kind")
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
        assert(
          Set("near_miss", "comparative_false_rival").contains(row.caseType),
          clues(s"${row.id} wave-3 same-cluster row must be an explicit rejected rival boundary")
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
        val sameAnchorPairs =
          ownerBreakContactSourceTargetPairs(row).filter((source, _) =>
            ownerAvailableLeverSources(row).contains(source)
          )
        val nonCenterChainBasePairs =
          sameAnchorPairs.filter((_, target) =>
            isChainBaseTarget(context, defender, target) && !isCenterFile(target)
          )
        assert(
          nonCenterChainBasePairs.nonEmpty,
          clues(s"${row.id} S14 route must bind same-anchor lever/contact source to a non-center exact chain-base target")
        )
        assert(
          nonCenterChainBasePairs.exists((_, target) => target == rowAnchorSquare(row)),
          clues(s"${row.id} S14 route must keep the row anchor on the same non-center chain-base contact target")
        )
        row.coverageBucket.foreach:
          case "chain_base_target" =>
            assertEquals(
              chainBaseContinuationPresent(context, defender, rowAnchorSquare(row)),
              false,
              clues(s"${row.id} S14 chain-base-target route must not be mislabeled as a continuation")
            )
          case "base_contact_continuation" =>
            assert(
              nonCenterChainBasePairs.exists((_, target) =>
                target == rowAnchorSquare(row) &&
                  chainBaseContinuationPresent(context, defender, target)
              ),
              clues(s"${row.id} S14 base-contact continuation must expose exact same-anchor chain continuation")
            )
          case other => fail(s"${row.id} unsupported S14 route bucket $other")
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
            val targets = ownerBreakContactTargetSquares(row)
            assert(
              targets.exists(square => isChainBaseTarget(context, defender, square)),
              clues(s"${row.id} S13 vs-S14 false rival must expose an exact chain-base target")
            )
            assert(
              targets.forall(square =>
                !isPhalanxEdgeTarget(context, defender, square) &&
                  !isStructurallyBurdenedTarget(context, defender, square)
              ),
              clues(s"${row.id} S13 vs-S14 false rival must not also satisfy a live S13 target role")
            )
          case ("same_cluster_near_miss", "vs_s13") if row.band == "S14" =>
            assert(
              ownerBreakContactTargetSquares(row).forall(square => !isChainBaseTarget(context, defender, square)),
              clues(s"${row.id} S14 vs-S13 false rival must not also satisfy the chain-base route")
            )
          case ("same_cluster_near_miss", "vs_s15") if row.band == "S14" =>
            val candidate = rowAnchorPieceOrSquare(row)
            assert(
              context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, candidate),
              clues(s"${row.id} S14 vs-S15 false rival must expose exact candidate-passer support")
            )
            assert(
              ownerBreakContactTargetSquares(row).forall(square => !isChainBaseTarget(context, defender, square)),
              clues(s"${row.id} S14 vs-S15 false rival must not expose an exact S14 chain-base target")
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
        val passer = rowAnchorPieceOrSquare(row)
        assert(
          enemyPassedPawnSquares(row).contains(passer),
          clues(s"${row.id} S16 blockade route must expose the row-anchor enemy passed-pawn witness")
        )
        val blockers = enemyPassedForwardBlockers(row).filter(_._1 == passer)
        assert(
          blockers.nonEmpty,
          clues(s"${row.id} S16 blockade route must bind an owner blocker to the enemy passer front square")
        )
        assertCertifiedSupport(row, "FortressDrawCertification")
      case ("suppression_route", "restriction_hold") =>
        val passer = rowAnchorPieceOrSquare(row)
        assert(
          enemyPassedPawnSquares(row).contains(passer),
          clues(s"${row.id} S16 restriction route must expose the row-anchor enemy passed-pawn witness")
        )
        val blocker =
          enemyPassedForwardBlockers(row)
            .collectFirst { case (`passer`, blocker) => blocker }
            .getOrElse(fail(s"${row.id} S16 restriction-hold route must keep the blocker tied to the same enemy passer"))
        assert(
          allWitnesses.exists(witness =>
            witness.descriptorId == WitnessDescriptorId("short_run_slider_gate_restriction") &&
              witness.color.contains(row.validatedOwner) &&
              (
                witnessSquareList(witness, "beneficiary_occupied_gate_squares").contains(blocker) ||
                  witnessSquareList(witness, "beneficiary_controlled_gate_squares").contains(blocker)
              )
          ),
          clues(s"${row.id} S16 restriction route must expose owner-side restriction support on the same blocker square")
        )
        assertCertifiedSupport(row, "PerpetualCheckHolding")
      case ("suppression_route", "non_losing_race") =>
        val passers = enemyPassedPawnSquares(row).toSet
        assert(
          passers.nonEmpty,
          clues(s"${row.id} S16 race route must expose an enemy passed-pawn witness")
        )
        assertCertifiedSupport(row, "PromotionRace")
        assert(
          certifiedSupportTargetSquares(row, "PromotionRace").intersect(passers).nonEmpty,
          clues(s"${row.id} S16 race certification support must name at least one same-board enemy passer")
        )
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
          case "create_passer_shell_only" if row.band == "S15" =>
            val subject = rowAnchorPieceOrSquare(row)
            assert(
              row.validatedSupportShellIds.contains("create_passer"),
              clues(s"${row.id} create-passer shell shortcut must expose shell-only support metadata")
            )
            assert(
              !context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, subject),
              clues(s"${row.id} create-passer shell shortcut must not expose candidate-passer root truth")
            )
            assert(
              !ownerPassedPawnSquares(row).contains(subject),
              clues(s"${row.id} create-passer shell shortcut must not collapse to existing passed-pawn truth")
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
          case "split_anchor_creation_route" if row.band == "S15" =>
            val candidate = rowAnchorPieceOrSquare(row)
            assert(
              context.hasColorPawnSquare(SchemaId.CandidatePasser, row.validatedOwner, candidate),
              clues(s"${row.id} split-anchor shortcut must expose candidate-passer root truth on its anchor")
            )
            assert(
              hasAnyCreationRoute(row, defender),
              clues(s"${row.id} split-anchor shortcut must expose an S13/S14 creation route somewhere on the board")
            )
            assert(
              !hasAnyCreationRouteFrom(row, defender, candidate),
              clues(s"${row.id} split-anchor shortcut must not bind that route to the candidate anchor")
            )
            assertEquals(
              context
                .activeColorPawnSquares(SchemaId.CandidatePasser, row.validatedOwner)
                .filter(source => hasAnyCreationRouteFrom(row, defender, source)),
              Vector.empty,
              clues(s"${row.id} split-anchor shortcut must not contain any board-level candidate creation route")
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
      Set("InitiativeWindow", "MobilityComparison", "MaterialHarvest")
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
        row.validatedRequiredWitnessIds.toSet.contains("available_lever_trigger") &&
          row.validatedRequiredWitnessIds.toSet.contains("pawn_push_break_contact_source") &&
          hasS05ExactCenterReleaseCarrier(row)
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
          if isWingSector(sectorOf(target.file)) &&
            sectorOf(source.file) == sectorOf(target.file) &&
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
            isWingSector(sectorOf(target.file)) &&
            hasDefenderMajoritySectorAsymmetry(row, sectorOf(target.file), defenderColor) &&
            (isPhalanxEdgeTarget(context, defenderColor, target) ||
              isStructurallyBurdenedTarget(context, defenderColor, target))))
    )

  private def hasAnyCreationRoute(
      row: ProjectionExpectationCorpus.Row,
      defenderColor: chess.Color
  ): Boolean =
    ownerBreakContactSourceTargetPairs(row).exists((source, _) =>
      hasAnyCreationRouteFrom(row, defenderColor, source)
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

  private def certifiedSupportTargetSquares(row: ProjectionExpectationCorpus.Row, familyId: String): Set[Square] =
    certifiedClaimForRow(row, familyId).support.targetSquares.toSet

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

  private def ownerAvailableLeverSources(row: ProjectionExpectationCorpus.Row): Vector[Square] =
    currentWitnesses(row)
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("available_lever_trigger") &&
          witness.color.contains(row.validatedOwner)
      )
      .flatMap:
        _.anchor match
          case WitnessAnchor.PieceSquareAnchor(source) => Vector(source)
          case _                                      => Vector.empty
      .distinct
      .sortBy(_.value)

  private def ownerAvailableLeverSourceTargetPairs(row: ProjectionExpectationCorpus.Row): Vector[(Square, Square)] =
    currentWitnesses(row)
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("available_lever_trigger") &&
          witness.color.contains(row.validatedOwner)
      )
      .flatMap: witness =>
        witness.anchor match
          case WitnessAnchor.PieceSquareAnchor(source) =>
            witness.support.targetSquares.map(source -> _)
          case _ => Vector.empty
      .distinct
      .sortBy((source, target) => (source.value, target.value))

  private def hasS05ExactCenterReleaseCarrier(row: ProjectionExpectationCorpus.Row): Boolean =
    val leverPairs = ownerAvailableLeverSourceTargetPairs(row).toSet
    ownerBreakContactSourceTargetPairs(row).exists((source, target) =>
      isCenterFile(source) && isCenterFile(target) && leverPairs.contains(source -> target)
    )

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
      chainBaseForwardSupportSquares(context, defenderColor, square).nonEmpty &&
      !supportedFromRear(context, defenderColor, square)

  private def isPhalanxEdgeTarget(
      context: UExtractionContext,
      defenderColor: chess.Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      !isChainBaseTarget(context, defenderColor, square) &&
      sameRankAdjacentCount(context, defenderColor, square) == 1

  private def chainBaseForwardSupportSquares(
      context: UExtractionContext,
      defenderColor: chess.Color,
      square: Square
  ): Vector[Square] =
    if !context.hasPieceOn(defenderColor, Pawn, square) then Vector.empty
    else
      square.pawnAttacks(defenderColor).squares
        .filter(target => context.hasPieceOn(defenderColor, Pawn, target))
        .toVector
        .sortBy(_.value)

  private def chainBaseContinuationPresent(
      context: UExtractionContext,
      defenderColor: chess.Color,
      square: Square
  ): Boolean =
    chainBaseForwardSupportSquares(context, defenderColor, square).exists: forwardSquare =>
      chainBaseForwardSupportSquares(context, defenderColor, forwardSquare).nonEmpty

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
          if isWingSector(sectorOf(target.file)) &&
            sectorOf(source.file) == sectorOf(target.file) &&
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

  private def isWingSector(sector: WitnessSector): Boolean =
    sector != WitnessSector.Center

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

  private def docText(path: String): String =
    java.nio.file.Files.readString(java.nio.file.Paths.get(path))

  private def witnessSquareList(witness: lila.commentary.witness.Witness, field: String): Vector[Square] =
    witness.payload.get(field).collect { case WitnessValue.SquareListValue(values) => values }.getOrElse(Vector.empty)

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
        case "bishop_pair_to_material" =>
          val claim = certifiedClaimForRow(row, "MaterialHarvest")
          assertS18EvidenceMirrors(row, "MaterialHarvest", bishopSquares, homeRank, claim.support.targetSquares)
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
            assertS18EvidenceMirrors(row, familyId, bishopSquares, homeRank, claim.support.targetSquares)
        case other =>
          fail(s"${row.id} uses unsupported S18 conversion bucket $other")

  private def assertS18EvidenceMirrors(
      row: ProjectionExpectationCorpus.Row,
      certificationFamily: String,
      bishopSquares: Vector[Square],
      homeRank: Rank,
      targetSquares: Vector[Square]
  ): Unit =
    val claim =
      row.evidenceClaimsForRuntime.headOption
        .getOrElse(fail(s"${row.id} admitted S18 row must carry projection evidence"))
    assertEquals(
      claim.payload.get("certification_family"),
      Some(WitnessValue.Token(certificationFamily)),
      clues(s"${row.id} S18 projection evidence must name the same certification family")
    )
    val evidenceBishops =
      claim.payload
        .get("bishop_member_squares")
        .collect { case WitnessValue.SquareListValue(values) => values }
        .getOrElse(fail(s"${row.id} S18 projection evidence must mirror bishop_member_squares"))
    assertEquals(
      evidenceBishops.toSet,
      bishopSquares.toSet,
      clues(s"${row.id} S18 projection evidence must mirror exact bishop-pair members")
    )
    val activeBishop =
      claim.payload
        .get("active_bishop_square")
        .collect { case WitnessValue.SquareValue(square) => square }
        .getOrElse(fail(s"${row.id} S18 projection evidence must name an active bishop-pair member"))
    assert(
      bishopSquares.contains(activeBishop) && activeBishop.rank != homeRank,
      clues(s"${row.id} S18 active bishop evidence must be one of the current bishop-pair members")
    )
    val evidenceTargets =
      claim.payload
        .get("conversion_target_squares")
        .collect { case WitnessValue.SquareListValue(values) => values }
        .getOrElse(fail(s"${row.id} S18 projection evidence must mirror conversion target squares"))
    assert(
      evidenceTargets.nonEmpty,
      clues(s"${row.id} S18 projection evidence must name exact conversion targets")
    )
    assertEquals(
      evidenceTargets.toSet,
      targetSquares.toSet,
      clues(s"${row.id} S18 projection evidence must mirror lower certification target squares")
    )

  private def assertS19SameTaskCertification(row: ProjectionExpectationCorpus.Row): Unit =
    if row.countableCoveragePair.contains("simplification_route" -> row.coverageBucket.getOrElse("")) &&
      row.band == "S19" &&
      row.expectation == "admitted"
    then
      val (before, move, after) =
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
          val material = certifiedClaimForRow(row, "MaterialHarvest")
          assertEquals(
            material.payload.get("capture_from"),
            Some(WitnessValue.SquareValue(move.orig)),
            clues(s"${row.id} MaterialHarvest must bind to the exact TradeInvariant move source")
          )
          assertEquals(
            material.payload.get("capture_to"),
            Some(WitnessValue.SquareValue(move.dest)),
            clues(s"${row.id} MaterialHarvest must bind to the exact TradeInvariant move target")
          )
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

  private def assertS19PreAdmissionFreeze(row: ProjectionExpectationCorpus.Row): Unit =
    if row.band == "S19" then
      row.admissionPath match
        case "hold_rival_shortcut" =>
          assert(
            row.validatedOptionalStrengtheningFamilies.contains("FortressHoldingShell") &&
              row.validatedOptionalStrengtheningFamilies.contains("FortressDrawCertification"),
            clues(s"${row.id} S22 rival must declare hold carriers as non-admitting optional support")
          )
          assert(
            hasObjectFamily(row, "FortressHoldingShell"),
            clues(s"${row.id} S22 rival must expose exact FortressHoldingShell truth")
          )
          assertEquals(
            certifiedClaimForRow(row, "FortressDrawCertification").verdict,
            CertificationVerdict.Certified,
            clues(s"${row.id} S22 rival must expose certified hold truth without becoming S19")
          )
          assertEquals(
            hasS19Carrier(row),
            false,
            clues(s"${row.id} S22 rival must not satisfy S19 simplification carrier")
          )
        case "prepared_target_rival_shortcut" =>
          val extraction =
            StrategySupportSeedExtractor
              .fromFen(row.normalizedFen)
              .fold(message => fail(s"Row ${row.id} FEN failed: $message"), identity)
          val dependencyAnchors =
            extraction.seeds
              .forSeedId(StrategySupportSeedScopeContract.S24TargetResourceDependency)
              .filter(_.color.contains(row.validatedOwner))
              .map(_.anchor)
              .toSet
          val convergenceAnchors =
            extraction.seeds
              .forSeedId(StrategySupportSeedScopeContract.S24TargetAttackConvergence)
              .filter(_.color.contains(row.validatedOwner))
              .map(_.anchor)
              .toSet
          assert(
            row.validatedOptionalStrengtheningFamilies.contains("target_resource_dependency_seed") &&
              row.validatedOptionalStrengtheningFamilies.contains("target_attack_convergence_seed"),
            clues(s"${row.id} S24 rival must declare prepared-target carriers as non-admitting optional support")
          )
          assert(
            dependencyAnchors.contains(WitnessAnchor.PieceSquareAnchor(rowAnchorPieceOrSquare(row))) &&
              convergenceAnchors.contains(WitnessAnchor.PieceSquareAnchor(rowAnchorPieceOrSquare(row))),
            clues(s"${row.id} S24 rival must expose same-target dependency and convergence truth")
          )
          assertEquals(
            hasS19Carrier(row),
            false,
            clues(s"${row.id} S24 rival must not satisfy S19 simplification carrier")
          )
        case _ => ()

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

  private def assertS22PreAdmissionFreeze(row: ProjectionExpectationCorpus.Row): Unit =
    if row.band == "S22" then
      row.admissionPath match
        case "fortress_draw_hold" if row.expectation == "admitted" =>
          val holderKing = rowAnchorSquare(row)
          val shell =
            objectExtractionFromFen(row.fen).objects
              .forFamilyId("FortressHoldingShell")
              .find(obj =>
                obj.color.contains(row.validatedOwner) &&
                  obj.anchor == WitnessAnchor.SquareAnchor(holderKing)
              )
              .getOrElse(fail(s"${row.id} must bind FortressHoldingShell to row anchor ${row.anchor}"))
          assert(
            shell.support.targetSquares.nonEmpty,
            clues(s"${row.id} same-holder fortress shell must expose concrete support squares")
          )
          val certification = certifiedClaimForRow(row, "FortressDrawCertification")
          assertEquals(
            certification.payload.get("king_square"),
            Some(WitnessValue.SquareValue(holderKing)),
            clues(s"${row.id} FortressDrawCertification must name the same holder king square")
          )
        case "simplification_rival_shortcut" =>
          val (before, move, after) =
            row.validatedDeltaCompanion.getOrElse(fail(s"${row.id} must carry exact S19 delta rival fields"))
          assertEquals(
            row.normalizedFen,
            before,
            clues(s"${row.id} S19 false rival must certify MaterialHarvest on the exact move board")
          )
          val extraction =
            StrategicDeltaExtractor
              .fromFens(before, move, after)
              .fold(message => fail(s"${row.id} delta extraction failed: $message"), identity)
          assert(
            extraction.deltas.forFamilyId("TradeInvariant").exists(_.color.contains(row.validatedOwner)),
            clues(s"${row.id} S19 false rival must expose live exact TradeInvariant truth")
          )
          assert(
            row.validatedOptionalStrengtheningFamilies.contains("MaterialHarvest"),
            clues(s"${row.id} S19 false rival must declare MaterialHarvest as adjacent lower certification support")
          )
          val material = certifiedClaimForRow(row, "MaterialHarvest")
          assertEquals(
            material.verdict,
            CertificationVerdict.Certified,
            clues(s"${row.id} exact S19 rival must expose certified MaterialHarvest on the move board")
          )
          assertEquals(
            material.payload.get("capture_from"),
            Some(WitnessValue.SquareValue(move.orig)),
            clues(s"${row.id} MaterialHarvest must bind to the exact S19 move source")
          )
          assertEquals(
            material.payload.get("capture_to"),
            Some(WitnessValue.SquareValue(move.dest)),
            clues(s"${row.id} MaterialHarvest must bind to the exact S19 move target")
          )
        case "checking_sequence_wording" =>
          assertEquals(
            certificationVerdictForRow(
              row,
              "PerpetualCheckHolding",
              Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied)
            ),
            CertificationVerdict.Rejected,
            clues(s"${row.id} loose checking sequence must be rejected even with satisfied evidence metadata")
          )
          assert(
            !hasRenewableHeavyPieceCheckingCycle(row.normalizedFen),
            clues(s"${row.id} checking-sequence shortcut must not expose a renewable heavy-piece cycle")
          )
        case "perpetual_deferred" =>
          assert(
            hasRenewableHeavyPieceCheckingCycle(row.normalizedFen),
            clues(s"${row.id} deferred perpetual row must expose the exact cycle while evidence remains insufficient")
          )
          assertEquals(
            certificationVerdictForRow(
              row,
              "PerpetualCheckHolding",
              Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Insufficient)
            ),
            CertificationVerdict.Deferred,
            clues(s"${row.id} insufficient best-defense evidence must keep S22 fail-closed")
          )
        case _ => ()

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

  private def certificationEvidenceForRuntime(
      row: ProjectionExpectationCorpus.Row,
      extraction: StrategySupportSeedExtraction
  ): CertificationEvidenceBundle =
    if row.band == "S06" && row.expectation == "admitted" then
      val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
      evidenceBundleForFamily(
        current = current,
        familyId = "SpaceBindRestrictionCertification",
        color = row.validatedOwner,
        purposeStrengths = Map(
          CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
          CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
        )
      )
    else if row.band == "S07" then
      val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
      CertificationEvidenceBundle.forObjectExtraction(
        current,
        row.validatedRequiredCertificationFamilies.map:
          case "DevelopmentComparison" =>
            CertificationEvidence(
              familyId = CertificationId("DevelopmentComparison"),
              color = row.validatedOwner,
              purposeStrengths =
                Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
            )
          case "InitiativeWindow" =>
            CertificationEvidence(
              familyId = CertificationId("InitiativeWindow"),
              color = row.validatedOwner,
              purposeStrengths = Map(
                CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied,
                CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
              )
            )
          case other => fail(s"${row.id} unsupported S07 certification carrier $other")
      )
    else if row.band == "S18" && row.expectation == "admitted" then
      val familyId =
        row.validatedRequiredCertificationFamilies.headOption
          .getOrElse(fail(s"${row.id} admitted S18 row must name a certification carrier"))
      val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
      val purposes =
        familyId match
          case "InitiativeWindow" =>
            Map(
              CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied,
              CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
            )
          case "MobilityComparison" =>
            Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
          case "MaterialHarvest" =>
            Map(
              CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
              CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
            )
          case other => fail(s"${row.id} unsupported S18 certification carrier $other")
      evidenceBundleForFamily(
        current = current,
        familyId = familyId,
        color = row.validatedOwner,
        purposeStrengths = purposes
      )
    else if row.band == "S19" && row.expectation == "admitted" then
      val familyId =
        row.validatedRequiredCertificationFamilies.headOption
          .getOrElse(fail(s"${row.id} admitted S19 row must name a certification carrier"))
      val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
      val purposes =
        familyId match
          case "MaterialHarvest" =>
            Map(
              CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
              CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
            )
          case "FortressDrawCertification" =>
            Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied)
          case other => fail(s"${row.id} unsupported S19 certification carrier $other")
      evidenceBundleForFamily(
        current = current,
        familyId = familyId,
        color = row.validatedOwner,
        purposeStrengths = purposes
      )
    else if row.band == "S08" && row.expectation == "admitted" then
      val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
      evidenceBundleForFamily(
        current = current,
        familyId = "InitiativeWindow",
        color = row.validatedOwner,
        purposeStrengths = Map(
          CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied,
          CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
        )
      )
    else if row.band == "S21" && row.expectation == "admitted" then
      val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
      evidenceBundleForFamily(
        current = current,
        familyId = "InitiativeWindow",
        color = row.validatedOwner,
        purposeStrengths = Map(
          CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied,
          CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
        )
      )
    else if row.band == "S22" && row.expectation == "admitted" then
      val familyId =
        row.validatedRequiredCertificationFamilies.headOption
          .getOrElse(fail(s"${row.id} admitted S22 row must name a certification carrier"))
      val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
      evidenceBundleForFamily(
        current = current,
        familyId = familyId,
        color = row.validatedOwner,
        purposeStrengths = Map(
          CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
        )
      )
    else if row.band == "S16" && row.expectation == "admitted" then
      val familyId =
        row.evidenceClaims.headOption
          .flatMap(_.certificationFamily)
          .getOrElse(fail(s"${row.id} admitted S16 row must name a support certification family in evidence"))
      val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
      val purposes =
        familyId match
          case "PromotionRace" =>
            Map(
              CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
              CertificationEvidencePurpose.ConversionRouteSurvival -> CertificationEvidenceStrength.Satisfied
            )
          case "FortressDrawCertification" | "PerpetualCheckHolding" =>
            Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied)
          case other => fail(s"${row.id} unsupported S16 certification support $other")
      evidenceBundleForFamily(
        current = current,
        familyId = familyId,
        color = row.validatedOwner,
        purposeStrengths = purposes
      )
    else CertificationEvidenceBundle.empty

  private def deltaExtractionForRuntime(
      row: ProjectionExpectationCorpus.Row
  ): Option[StrategicDeltaExtraction] =
    if row.band == "S19" then
      row.validatedDeltaCompanion.map: (before, move, after) =>
        StrategicDeltaExtractor
          .fromFens(before, move, after)
          .fold(message => fail(s"${row.id} delta extraction failed: $message"), identity)
    else None

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
