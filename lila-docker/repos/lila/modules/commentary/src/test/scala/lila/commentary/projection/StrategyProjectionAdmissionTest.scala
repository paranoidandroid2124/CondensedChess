package lila.commentary.projection

import chess.{ Color, Square }
import chess.format.{ Fen, Uci }
import scala.util.Try

import lila.commentary.CommentaryCore
import lila.commentary.certification.{
  Certification,
  CertificationBurdenTag,
  CertificationEvidence,
  CertificationEvidenceBundle,
  CertificationEvidencePurpose,
  CertificationEvidenceStrength,
  CertificationId,
  CertificationExtractor,
  CertificationScope,
  CertificationVerdict
}
import lila.commentary.delta.{ StrategicDeltaExtraction, StrategicDeltaExtractor }
import lila.commentary.strategic.{
  StrategicObject,
  StrategicObjectExtraction,
  StrategicObjectExtractor,
  StrategicObjectId,
  StrategicObjectSet
}
import lila.commentary.witness.{ WitnessAnchor, WitnessDescriptorId, WitnessPayload, WitnessSupport, WitnessValue }
import lila.commentary.witness.seed.StrategySupportSeedExtractor

class StrategyProjectionAdmissionTest extends munit.FunSuite:

  test("S01/S02/S03/S04/S05/S06/S07/S08/S09/S10/S11/S12/S13/S14/S15/S16/S17/S18/S19/S20/S21/S22/S23/S24/S25 projection rows expose start-ready admission contracts"):
    assertEquals(
      StrategyProjectionScopeContract.startReadyBandIds.map(_.value),
      Vector("S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09", "S10", "S11", "S12", "S13", "S14", "S15", "S16", "S17", "S18", "S19", "S20", "S21", "S22", "S23", "S24", "S25")
    )
    assertEquals(
      CommentaryCore.strategyProjectionStartReadyBandIds,
      Vector("S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09", "S10", "S11", "S12", "S13", "S14", "S15", "S16", "S17", "S18", "S19", "S20", "S21", "S22", "S23", "S24", "S25")
    )
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand.view.mapValues(_.map(_.value)).toMap,
        Map(
          "S01" -> Vector("king_wing_storm_route_certified"),
          "S02" -> Vector("king_ring_concentration_route_certified"),
          "S03" -> Vector("diagonal_king_attack_route_certified"),
          "S04" -> Vector("king_shelter_breach_route_certified"),
          "S05" -> Vector("center_release_route_certified"),
          "S06" -> Vector("space_bind_restriction_route_certified"),
        "S07" -> Vector("initiative_conversion_route_certified"),
        "S08" -> Vector("counterplay_denial_route_certified"),
        "S09" -> Vector("file_penetration_route_certified"),
        "S10" -> Vector("outpost_occupation_route_certified"),
        "S11" -> Vector("weak_pawn_target_pressure_persistence_certified"),
        "S12" -> Vector("local_access_superiority_route_certified"),
          "S13" -> Vector("wing_damage_route_certified"),
          "S14" -> Vector("chain_base_contact_route_certified"),
          "S15" -> Vector("passer_creation_route_certified"),
          "S16" -> Vector("passer_suppression_route_certified"),
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
        "S20" -> Vector("mobility_domination_route_certified"),
        "S21" -> Vector("counterplay_survival_route_certified"),
        "S22" -> Vector("fortress_hold_certified", "perpetual_hold_certified"),
        "S23" -> Vector("king_entry_conversion_certified", "king_opposition_certified"),
        "S24" -> Vector("same_target_forcing_realization", "same_target_conversion_certified"),
        "S25" -> Vector("rank_access_consequence_certified")
      )
    )

  test("S01 admits only exact same-anchor king-wing storm route with certified same-king support"):
    val s01 = StrategyProjectionBandId("S01")
    val storm = seedExtraction("6k1/6pp/8/6p1/3B1p2/8/Q3P1RP/6K1 w - - 0 1")
    val certification = kingSafetyEdgeEvidenceFor(storm, Color.White)

    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        storm,
        s01Evidence(storm, "h2", "g5", "g8", "same_wing_contact"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        storm,
        s01Evidence(storm, "h2", "g5", "g8", "attack_edge_same_king"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s01, storm, StrategyProjectionEvidence.empty, Color.White, certification),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        storm,
        s01Evidence(storm, "h2", "g5", "g8", "same_wing_contact"),
        Color.White
      ),
      Right(false),
      clues("S01 evidence cannot prove admission without same-board CertifiedKingSafetyEdge support")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        storm,
        s01Evidence(storm, "h2", "f4", "g8", "same_wing_contact"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        storm,
        s01Evidence(storm, "g2", "g5", "g8", "same_wing_contact"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        storm,
        s01Evidence(storm, "h2", "g5", "h8", "same_wing_contact"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        storm,
        s01Evidence(storm, "h2", "g5", "g8", "castling_side_only"),
        Color.White,
        certification
      ),
      Right(false)
    )

  test("S01 rejects same-cluster rivals, shortcut negatives, optional strengthening, and stale evidence"):
    val s01 = StrategyProjectionBandId("S01")
    val storm = seedExtraction("6k1/6pp/8/6p1/3B1p2/8/Q3P1RP/6K1 w - - 0 1")
    val s05Rival = seedExtraction("6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1")
    val s21Rival = seedExtraction("r1bqkbnr/p1ppp3/2n5/6p1/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val centerEdgeRival = seedExtraction("6k1/6pp/8/8/3B1p2/8/4P1R1/6K1 w - - 0 1")
    val castlingSideOnly = seedExtraction("7k/8/8/7p/8/8/6P1/6K1 w - - 0 1")
    val wingShellOnly = seedExtraction("6k1/8/6p1/4B3/8/8/7R/6K1 w - - 0 1")
    val stale = seedExtraction("7k/8/8/7p/8/8/6P1/6K1 w - - 0 1")
    val certification = kingSafetyEdgeEvidenceFor(storm, Color.White)

    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        s05Rival,
        s01Evidence(s05Rival, "c2", "d5", "g8", "same_wing_contact"),
        Color.White,
        kingSafetyEdgeEvidenceFor(s05Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        centerEdgeRival,
        s01Evidence(centerEdgeRival, "e2", "f4", "g8", "same_wing_contact"),
        Color.White,
        kingSafetyEdgeEvidenceFor(centerEdgeRival, Color.White)
      ),
      Right(false),
      clues("S01 must not absorb c-f center-edge pawn contact that belongs to the S05 center-release boundary")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        s21Rival,
        s01Evidence(s21Rival, "h2", "g5", "e8", "same_wing_contact"),
        Color.White,
        initiativeWindowEvidenceFor(s21Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        castlingSideOnly,
        s01Evidence(castlingSideOnly, "g2", "h5", "h8", "same_wing_contact"),
        Color.White,
        kingSafetyEdgeEvidenceFor(castlingSideOnly, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        wingShellOnly,
        s01Evidence(wingShellOnly, "e2", "f4", "g8", "same_wing_contact"),
        Color.White,
        kingSafetyEdgeEvidenceFor(wingShellOnly, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        wingShellOnly,
        StrategyProjectionEvidence.empty,
        Color.White,
        kingSafetyEdgeEvidenceFor(wingShellOnly, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s01,
        storm,
        s01Evidence(stale, "g2", "h5", "h8", "same_wing_contact"),
        Color.White,
        certification
      ),
      Left("Strategy projection admission rejected stale evidence bundle")
    )
    assert(
      StrategyProjectionAdmission
        .admits(
          s01,
          storm,
          s01Evidence(storm, "h2", "g5", "g8", "same_wing_contact"),
          Color.White,
          kingSafetyEdgeEvidenceFor(stale, Color.White)
        )
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S02 admits only exact same-king king-ring concentration route with mirrored runtime evidence"):
    val s02 = StrategyProjectionBandId("S02")
    val exact = seedExtraction("6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1")
    val certification = kingSafetyEdgeEvidenceFor(exact, Color.White)

    assertEquals(
      StrategyProjectionAdmission.admits(
        s02,
        exact,
        s02Evidence(exact, "g8", Vector("d4"), Vector("g7"), "direct_piece_concentration"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s02,
        exact,
        s02Evidence(exact, "g8", Vector("d4"), Vector("g7"), "lane_strengthened_concentration"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s02, exact, StrategyProjectionEvidence.empty, Color.White, certification),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s02,
        exact,
        s02Evidence(exact, "g8", Vector("d4"), Vector("g7"), "direct_piece_concentration"),
        Color.White
      ),
      Right(false),
      clues("S02 projection evidence cannot admit without same-board CertifiedKingSafetyEdge support")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s02,
        exact,
        s02Evidence(exact, "h8", Vector("d4"), Vector("g7"), "direct_piece_concentration"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s02,
        exact,
        s02Evidence(exact, "g8", Vector("d4", "g2"), Vector("g7"), "direct_piece_concentration"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s02,
        exact,
        s02Evidence(exact, "g8", Vector("d4"), Vector("h8"), "direct_piece_concentration"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s02,
        exact,
        s02Evidence(exact, "g8", Vector("d4"), Vector("g7"), "diagonal_attack_rival_shortcut"),
        Color.White,
        certification
      ),
      Right(false)
    )

  test("S02 carrier targets exclude AttackScaffold support-only target squares"):
    val exact = seedExtraction("6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1")
    val current = StrategicObjectExtractor.fromRoot(exact.rootState)
    val defendingKing = squareFromKey("g8")
    val carrierSource = squareFromKey("d4")
    val carrierTarget = squareFromKey("g7")
    val supportOnlyTarget = squareFromKey("h7")
    val syntheticCurrent = StrategicObjectExtraction(
      rootState = current.rootState,
      primaryWitnesses = current.primaryWitnesses,
      attachedWitnesses = current.attachedWitnesses,
      objects = StrategicObjectSet(
        Vector(
          StrategicObject(
            familyId = StrategicObjectId("AttackScaffold"),
            anchor = WitnessAnchor.SquareAnchor(defendingKing),
            color = Some(Color.White),
            payload = WitnessPayload(
              "king_square" -> WitnessValue.SquareValue(defendingKing),
              "carrier_fragment_ids" -> WitnessValue.TokenListValue(Vector("diagonal_lane_only")),
              "carrier_source_squares" -> WitnessValue.SquareListValue(Vector(carrierSource)),
              "carrier_squares" -> WitnessValue.SquareListValue(Vector(carrierSource, carrierTarget)),
              "support_fragment_ids" -> WitnessValue.TokenListValue(Vector("loose_piece")),
              "support_squares" -> WitnessValue.SquareListValue(Vector(supportOnlyTarget))
            ),
            support = WitnessSupport.empty
              .addTargetSquare(carrierSource)
              .addTargetSquare(carrierTarget)
              .addTargetSquare(supportOnlyTarget)
          )
        )
      )
    )
    val syntheticCertification = Certification(
      familyId = CertificationId("CertifiedKingSafetyEdge"),
      scope = CertificationScope.Comparative,
      burdenTag = CertificationBurdenTag("king_safety_edge_certification"),
      verdict = CertificationVerdict.Certified,
      anchor = WitnessAnchor.BoardAnchor,
      color = Color.White,
      payload = WitnessPayload(
        "attacked_king_ring_squares" -> WitnessValue.SquareListValue(Vector(carrierTarget, supportOnlyTarget))
      ),
      support = WitnessSupport.empty
    )

    val carriers = invokeS02KingRingConcentrationCarriers(syntheticCurrent, syntheticCertification, Color.White)
    val carrierTargetSets =
      carriers.map(_.productElement(2).asInstanceOf[Set[Square]].map(_.key))

    assertEquals(
      carrierTargetSets.exists(_.contains(supportOnlyTarget.key)),
      false,
      clues("S02 must not promote AttackScaffold support-only squares into king-ring concentration targets")
    )
    assertEquals(carrierTargetSets.exists(_ == Set(carrierTarget.key)), true)

  test("S02 rejects same-cluster rivals, shortcut negatives, optional strengthening, and stale evidence"):
    val s02 = StrategyProjectionBandId("S02")
    val exact = seedExtraction("6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1")
    val s03Rival = seedExtraction("6k1/8/6p1/8/3B4/8/6R1/6K1 w - - 0 1")
    val s04Rival = seedExtraction("6k1/8/6p1/4B3/8/8/7R/6K1 w - - 0 1")
    val s09Rival = seedExtraction("6k1/8/8/8/8/8/4R3/6K1 w - - 0 1")
    val labelOnly = seedExtraction("6k1/8/8/8/8/8/4R3/6K1 w - - 0 1")
    val stale = seedExtraction("6k1/8/6p1/8/3B4/8/6R1/6K1 w - - 0 1")
    val certification = kingSafetyEdgeEvidenceFor(exact, Color.White)

    Vector(s03Rival, s04Rival, s09Rival, labelOnly).foreach: extraction =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          s02,
          extraction,
          s02Evidence(extraction, "g8", Vector("d4"), Vector("g7"), "direct_piece_concentration"),
          Color.White,
          kingSafetyEdgeEvidenceFor(extraction, Color.White)
        ),
        Right(false)
      )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s02,
        exact,
        s02Evidence(exact, "g8", Vector("d4"), Vector("g7"), "lane_strengthened_concentration"),
        Color.White,
        CertificationEvidenceBundle.empty
      ),
      Right(false),
      clues("S02 lane strengthening is not an admission proof without the exact lower certification carrier")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s02,
        exact,
        s02Evidence(stale, "g8", Vector("d4"), Vector("g7"), "direct_piece_concentration"),
        Color.White,
        certification
      ),
      Left("Strategy projection admission rejected stale evidence bundle")
    )
    assert(
      StrategyProjectionAdmission
        .admits(
          s02,
          exact,
          s02Evidence(exact, "g8", Vector("d4"), Vector("g7"), "direct_piece_concentration"),
          Color.White,
          kingSafetyEdgeEvidenceFor(stale, Color.White)
        )
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S03 admits only exact same-king diagonal attack routes with mirrored runtime evidence"):
    val s03 = StrategyProjectionBandId("S03")
    val exact = seedExtraction("6k1/8/6p1/8/3B4/8/6R1/6K1 w - - 0 1")
    val certification = kingSafetyEdgeEvidenceFor(exact, Color.White)

    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "d4", Vector("g7", "h8"), "fragility_linked_diagonal"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s03, exact, StrategyProjectionEvidence.empty, Color.White, certification),
      Right(false),
      clues("S03 lower carriers cannot admit without live projection evidence")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White
      ),
      Right(false),
      clues("S03 projection evidence cannot admit without same-board certified lower support")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "h8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "g2", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "d4", Vector("h8"), "king_facing_diagonal_entry"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "d4", Vector("g7", "h8"), "concentration_rival_shortcut"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry", owner = Color.Black),
        Color.White,
        certification
      ),
      Right(false)
    )

  test("S03 rejects same-cluster rivals, shortcut negatives, support-only proof, and stale evidence"):
    val s03 = StrategyProjectionBandId("S03")
    val exact = seedExtraction("6k1/8/6p1/8/3B4/8/6R1/6K1 w - - 0 1")
    val s02Rival = seedExtraction("6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1")
    val s12Rival = seedExtraction("6k1/8/7p/8/8/8/8/2B3K1 w - - 0 1")
    val bishopPairOnly = seedExtraction("4k3/8/8/8/8/8/6B1/2B1K3 w - - 0 1")
    val nonKingDiagonal = seedExtraction("4k3/8/7p/8/8/8/8/2B1K3 w - - 0 1")
    val scaffoldCertificationNoDiagonal =
      seedExtraction("6k1/6n1/8/8/8/8/6QR/6K1 w - - 0 1")
    val stale = seedExtraction("6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1")
    val certification = kingSafetyEdgeEvidenceFor(exact, Color.White)
    val noDiagonalCertification = kingSafetyEdgeEvidenceFor(scaffoldCertificationNoDiagonal, Color.White)

    Vector(s02Rival, s12Rival, bishopPairOnly, nonKingDiagonal).foreach: extraction =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          s03,
          extraction,
          s03Evidence(extraction, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
          Color.White,
          kingSafetyEdgeEvidenceFor(extraction, Color.White)
        ),
        Right(false)
      )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White,
        CertificationEvidenceBundle.empty
      ),
      Right(false),
      clues("S03 attack-scaffold-only proof attempt cannot admit without certified lower support")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        StrategyProjectionEvidence.empty,
        Color.White,
        certification
      ),
      Right(false),
      clues("S03 certification-support-only proof attempt cannot admit without live projection evidence")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        nonKingDiagonal,
        s03Evidence(nonKingDiagonal, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White,
        kingSafetyEdgeEvidenceFor(nonKingDiagonal, Color.White)
      ),
      Right(false),
      clues("S03 diagonal plus certification without same-defending-king scaffold cannot admit")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        scaffoldCertificationNoDiagonal,
        s03Evidence(scaffoldCertificationNoDiagonal, "g8", "g2", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White,
        noDiagonalCertification
      ),
      Right(false),
      clues("S03 scaffold plus certification without exact king-theater diagonal entry cannot admit")
    )
    assert(
      StrategicObjectExtractor
        .fromRoot(scaffoldCertificationNoDiagonal.rootState)
        .objects
        .forFamilyId("AttackScaffold")
        .exists(_.color.contains(Color.White)),
      clues("S03 nasty negative must contain real AttackScaffold support")
    )
    val noDiagonalCertifications =
      CertificationExtractor
        .fromObjectExtractionFailClosed(
          StrategicObjectExtractor.fromRoot(scaffoldCertificationNoDiagonal.rootState),
          noDiagonalCertification
        )
        .fold(message => fail(message), identity)
        .claims
    assert(
      noDiagonalCertifications
        .forFamilyId("ComparativeKingFragility")
        .exists(cert => cert.owner.contains(Color.White) && cert.verdict == CertificationVerdict.Certified) &&
        noDiagonalCertifications
          .forFamilyId("CertifiedKingSafetyEdge")
          .exists(cert => cert.owner.contains(Color.White) && cert.verdict == CertificationVerdict.Certified),
      clues("S03 nasty negative must contain certified lower support")
    )
    val blackKingRing =
      lila.commentary.witness.u.UExtractionContext(scaffoldCertificationNoDiagonal.rootState)
        .kingRingSquaresFor(Color.Black)
        .toSet
    val diagonalWitnesses =
      lila.commentary.witness.u.UWitnessExtractor
        .fromRoot(scaffoldCertificationNoDiagonal.rootState)
        .witnesses
        .forDescriptorId(WitnessDescriptorId("diagonal_lane_only"))
    assert(
      diagonalWitnesses.forall(witness => witness.support.targetSquares.toSet.intersect(blackKingRing).isEmpty),
      clues("S03 nasty negative must not contain a king-theater diagonal lane carrier")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White,
        certificationEvidenceFor(exact, "ComparativeKingFragility", Color.White)
      ),
      Right(false),
      clues("S03 ComparativeKingFragility support alone is not projection proof")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(exact, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White,
        certificationEvidenceFor(exact, "CertifiedKingSafetyEdge", Color.White)
      ),
      Right(false),
      clues("S03 CertifiedKingSafetyEdge support alone is not projection proof")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s03,
        exact,
        s03Evidence(stale, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
        Color.White,
        certification
      ),
      Left("Strategy projection admission rejected stale evidence bundle")
    )
    assert(
      StrategyProjectionAdmission
        .admits(
          s03,
          exact,
          s03Evidence(exact, "g8", "d4", Vector("g7", "h8"), "king_facing_diagonal_entry"),
          Color.White,
          kingSafetyEdgeEvidenceFor(stale, Color.White)
        )
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S09 admits only exact same-file penetration routes with mirrored runtime evidence"):
    val s09 = StrategyProjectionBandId("S09")
    val openEntry = seedExtraction("6k1/pppp1ppp/8/8/8/8/PPPPRPPP/6K1 w - - 0 1")
    val semiOpenEntry = seedExtraction("6k1/pppppppp/8/8/8/8/PPPPRPPP/6K1 w - - 0 1")
    val sameFilePenetration = seedExtraction("6k1/8/8/8/8/8/4R3/6K1 w - - 0 1")
    val fileAndRankAccess = seedExtraction("6k1/8/8/8/R7/8/8/6K1 w - - 0 1")
    val blackSameFilePenetration = seedExtraction("6k1/4r3/8/8/8/8/8/6K1 b - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        openEntry,
        s09Evidence(openEntry, "e2", "e7", "open_file_entry"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        semiOpenEntry,
        s09Evidence(semiOpenEntry, "e2", "e6", "semi_open_file_entry"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        sameFilePenetration,
        s09Evidence(sameFilePenetration, "e2", "e7", "same_file_penetration"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        fileAndRankAccess,
        s09Evidence(fileAndRankAccess, "a4", "a7", "same_file_penetration"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        blackSameFilePenetration,
        s09Evidence(blackSameFilePenetration, "e7", "e4", "same_file_penetration", owner = Color.Black),
        Color.Black
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S02"),
        fileAndRankAccess,
        s09Evidence(fileAndRankAccess, "a4", "a7", "same_file_penetration"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S23"),
        fileAndRankAccess,
        s09Evidence(fileAndRankAccess, "a4", "a7", "same_file_penetration"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        fileAndRankAccess,
        s09Evidence(fileAndRankAccess, "a4", "a7", "same_file_penetration"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        fileAndRankAccess,
        s25Evidence(fileAndRankAccess, "a4", "f4", "cross_wing_rank_switch"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s09, openEntry, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        openEntry,
        s09Evidence(openEntry, "e2", "e7", "semi_open_file_entry"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        openEntry,
        s09Evidence(openEntry, "e2", "d7", "open_file_entry"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        openEntry,
        s09Evidence(openEntry, "d2", "e7", "open_file_entry"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        openEntry,
        s09Evidence(openEntry, "e2", "e7", "open_file_entry", owner = Color.Black),
        Color.White
      ),
      Right(false)
    )

  test("S09 rejects adjacent rivals, shortcuts, support-only proof, and stale evidence"):
    val s09 = StrategyProjectionBandId("S09")
    val openEntry = seedExtraction("6k1/pppp1ppp/8/8/8/8/PPPPRPPP/6K1 w - - 0 1")
    val stale = seedExtraction("6k1/8/8/8/8/8/4R3/6K1 w - - 0 1")
    val kingAttackRival = seedExtraction("6k1/6pp/8/8/8/8/6Q1/6K1 w - - 0 1")
    val kingActivityRival = seedExtraction("6k1/8/8/3p4/5K2/8/8/8 w - - 0 1")
    val rankAccessRival = seedExtraction("6k1/8/8/n7/R7/8/8/6K1 w - - 0 1")
    val fileSubstrateOnly = seedExtraction("6k1/pppp1ppp/8/8/8/8/PPPP1PPP/6K1 w - - 0 1")
    val rookOnFileOnly = seedExtraction("6k1/pppppppp/8/8/8/8/PPPPPPPP/4R1K1 w - - 0 1")
    val attackSupportOnly = seedExtraction("6k1/6pp/8/8/8/8/5Q2/6K1 w - - 0 1")

    Vector(
      kingAttackRival,
      kingActivityRival,
      rankAccessRival,
      fileSubstrateOnly,
      rookOnFileOnly,
      attackSupportOnly
    ).foreach: extraction =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          s09,
          extraction,
          s09Evidence(extraction, "e2", "e7", "open_file_entry"),
          Color.White
        ),
        Right(false)
      )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        rankAccessRival,
        s09Evidence(rankAccessRival, "a4", "a7", "open_file_entry"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        openEntry,
        s09Evidence(stale, "e2", "e7", "same_file_penetration"),
        Color.White
      ),
      Left("Strategy projection admission rejected stale evidence bundle")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        openEntry,
        s09Evidence(
          openEntry,
          "e2",
          "e7",
          "open_file_entry",
          extraPayload = Vector("certification_family" -> WitnessValue.Token("MaterialHarvest"))
        ),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s09,
        openEntry,
        StrategyProjectionEvidence.empty,
        Color.White,
        certificationEvidenceFor(
          openEntry,
          "MaterialHarvest",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )

  test("S06 admits only exact structural-space bind routes with same-anchor evidence"):
    val s06 = StrategyProjectionBandId("S06")
    val outpost = seedExtraction("4k3/8/8/3ppN2/3PP3/7B/8/4K3 w - - 0 1")
    val nonOutpost = seedExtraction("6k1/8/1P6/1rBpp3/3PP3/8/8/6K1 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        outpost,
        s06Evidence(outpost, "f5", "outpost_anchor", "center", "closed_center", Some("f5"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(outpost, Color.White)
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        nonOutpost,
        s06Evidence(nonOutpost, "b5", "non_outpost_space_bind", "center", "closed_center", None, Some("b5")),
        Color.White,
        spaceBindCertificationEvidenceFor(nonOutpost, Color.White)
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s06, outpost, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        outpost,
        s06Evidence(outpost, "e5", "outpost_anchor", "center", "closed_center", Some("f5"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(outpost, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        outpost,
        s06Evidence(outpost, "f5", "non_outpost_space_bind", "center", "closed_center", Some("f5"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(outpost, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        outpost,
        s06Evidence(outpost, "f5", "outpost_anchor", "center", "fixed_chain", Some("f5"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(outpost, Color.White)
      ),
      Right(false)
    )

  test("S06 rejects adjacent false rivals, shortcuts, and stale evidence"):
    val s06 = StrategyProjectionBandId("S06")
    val outpost = seedExtraction("4k3/8/8/3ppN2/3PP3/7B/8/4K3 w - - 0 1")
    val centerReleaseRival = seedExtraction("4k3/8/8/3pP3/8/8/8/4K3 w - - 0 1")
    val exactCenterReleaseRival = seedExtraction("6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1")
    val dominationRival = seedExtraction("6kb/5Npp/8/8/8/8/8/4K3 b - - 0 1")
    val exactDominationRival = seedExtraction("8/8/1nQ3n1/4p3/3b2K1/8/1k6/8 w - - 0 1")
    val spaceWordingOnly = seedExtraction("4k3/8/8/3pP3/8/8/8/4K3 w - - 0 1")
    val mobilityOnly = seedExtraction("4k3/8/8/8/8/8/3Q4/4K3 w - - 0 1")
    val stale = seedExtraction("6k1/8/1P6/1rBpp3/3PP3/8/8/6K1 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        centerReleaseRival,
        s06Evidence(centerReleaseRival, "e5", "outpost_anchor", "center", "closed_center", Some("e5"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(centerReleaseRival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        exactCenterReleaseRival,
        s06Evidence(exactCenterReleaseRival, "d5", "outpost_anchor", "center", "closed_center", Some("d5"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(exactCenterReleaseRival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        dominationRival,
        s06Evidence(dominationRival, "f7", "outpost_anchor", "center", "closed_center", Some("f7"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(dominationRival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        exactDominationRival,
        s06Evidence(exactDominationRival, "d4", "non_outpost_space_bind", "center", "closed_center", None, Some("d4")),
        Color.White,
        certificationEvidenceFor(exactDominationRival, "MobilityComparison", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        spaceWordingOnly,
        s06Evidence(spaceWordingOnly, "e5", "outpost_anchor", "center", "closed_center", Some("e5"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(spaceWordingOnly, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        mobilityOnly,
        s06Evidence(mobilityOnly, "d2", "non_outpost_space_bind", "center", "closed_center", None, Some("d2")),
        Color.White,
        certificationEvidenceFor(mobilityOnly, "MobilityComparison", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        outpost,
        s06Evidence(stale, "f5", "outpost_anchor", "center", "closed_center", Some("f5"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(outpost, Color.White)
      ),
      Left("Strategy projection admission rejected stale evidence bundle")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        outpost,
        s06Evidence(outpost, "f5", "outpost_anchor", "queenside", "closed_center", Some("f5"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(outpost, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        outpost,
        s06Evidence(outpost, "f5", "outpost_anchor", "center", "closed_center", Some("e4"), None),
        Color.White,
        spaceBindCertificationEvidenceFor(outpost, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s06,
        outpost,
        s06Evidence(
          outpost,
          "f5",
          "outpost_anchor",
          "center",
          "closed_center",
          Some("f5"),
          None,
          certificationFamily = "MobilityComparison"
        ),
        Color.White,
        spaceBindCertificationEvidenceFor(outpost, Color.White)
      ),
      Right(false)
    )

  test("S08 admits only exact rival source denial with same-board InitiativeWindow evidence"):
    val s08 = StrategyProjectionBandId("S08")
    val denial = seedExtraction("r1bqkbnr/p1ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val certification = initiativeWindowEvidenceFor(denial, Color.White)

    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        denial,
        s08Evidence(denial, "d7", "c4", "rival_break_source_suppressed"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        denial,
        s08Evidence(denial, "d7", "e4", "rival_counterplay_source_suppressed"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s08, denial, StrategyProjectionEvidence.empty, Color.White, certification),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        denial,
        s08Evidence(denial, "d7", "c4", "rival_break_source_suppressed"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        denial,
        s08Evidence(denial, "e7", "c4", "rival_break_source_suppressed"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        denial,
        s08Evidence(denial, "d7", "d4", "rival_break_source_suppressed"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        denial,
        s08Evidence(denial, "d7", "c4", "initiative_conversion_rival_shortcut"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        denial,
        s08Evidence(denial, "d7", "c4", "rival_break_source_suppressed", "DevelopmentComparison"),
        Color.White,
        certification
      ),
      Right(false)
    )

  test("S08 rejects adjacent rivals, shortcuts, and optional-strengthening-only boards"):
    val s08 = StrategyProjectionBandId("S08")
    val s07Rival = seedExtraction("r1bqkbnr/2ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val s20Rival = seedExtraction("8/8/1nQ3n1/4p3/3b2K1/8/1k6/8 w - - 0 1")
    val s21Rival = seedExtraction("r1bqkbnr/p1ppp3/2n5/6p1/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val noCounterplayWording = seedExtraction("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3")
    val initiativeOnly = seedExtraction("r1bqkbnr/2ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        s07Rival,
        s08Evidence(s07Rival, "d7", "c4", "rival_break_source_suppressed"),
        Color.White,
        developmentInitiativeEvidenceFor(s07Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        s20Rival,
        s08Evidence(s20Rival, "d4", "e5", "rival_break_source_suppressed"),
        Color.White,
        certificationEvidenceFor(s20Rival, "MobilityComparison", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        s21Rival,
        s08Evidence(s21Rival, "h2", "g5", "rival_counterplay_source_suppressed"),
        Color.White,
        initiativeWindowEvidenceFor(s21Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        s21Rival,
        s08Evidence(s21Rival, "d7", "c4", "rival_break_source_suppressed"),
        Color.White,
        initiativeWindowEvidenceFor(s21Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        s21Rival,
        s08Evidence(s21Rival, "d7", "e4", "rival_counterplay_source_suppressed"),
        Color.White,
        initiativeWindowEvidenceFor(s21Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        noCounterplayWording,
        s08Evidence(noCounterplayWording, "d7", "e4", "rival_break_source_suppressed"),
        Color.White,
        developmentInitiativeEvidenceFor(noCounterplayWording, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s08,
        initiativeOnly,
        s08Evidence(initiativeOnly, "d7", "c4", "rival_break_source_suppressed"),
        Color.White,
        initiativeWindowEvidenceFor(initiativeOnly, Color.White)
      ),
      Right(false)
    )

  test("S08 projection admission rejects stale exact counterplay-denial evidence"):
    val s08 = StrategyProjectionBandId("S08")
    val current = seedExtraction("r1bqkbnr/p1ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val stale = seedExtraction("r1bqkbnr/2ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val currentCertification = initiativeWindowEvidenceFor(current, Color.White)
    val currentEvidence = s08Evidence(current, "d7", "c4", "rival_break_source_suppressed")
    val staleEvidence = s08Evidence(stale, "d7", "c4", "rival_break_source_suppressed")
    val staleCertification = initiativeWindowEvidenceFor(stale, Color.White)

    assertEquals(
      StrategyProjectionAdmission.admits(s08, current, currentEvidence, Color.White, currentCertification),
      Right(true)
    )
    assert(
      StrategyProjectionAdmission
        .admits(s08, current, staleEvidence, Color.White, currentCertification)
        .left
        .exists(_.contains("stale evidence bundle"))
    )
    assert(
      StrategyProjectionAdmission
        .admits(s08, current, currentEvidence, Color.White, staleCertification)
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S04 admits only exact same-defender shelter-breach routes with mirrored runtime evidence"):
    val s04 = StrategyProjectionBandId("S04")
    val exact = seedExtraction("6k1/8/6p1/4B3/8/8/7R/6K1 w - - 0 1")
    val certification = kingSafetyEdgeEvidenceFor(exact, Color.White)

    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        s04Evidence(exact, "g8", "g7", Vector("f6", "h6", "g7", "h7"), "shell_payload_breach"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        s04Evidence(exact, "g8", "g7", Vector("f6", "h6", "g7", "h7"), "support_break_breach"),
        Color.White,
        certification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s04, exact, StrategyProjectionEvidence.empty, Color.White, certification),
      Right(false),
      clues("S04 lower shell plus certification support cannot admit without live projection evidence")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        s04Evidence(exact, "g8", "g7", Vector("f6", "h6", "g7", "h7"), "shell_payload_breach"),
        Color.White
      ),
      Right(false),
      clues("S04 projection evidence cannot admit without same-board CertifiedKingSafetyEdge support")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        s04Evidence(exact, "h8", "g7", Vector("f6", "h6", "g7", "h7"), "shell_payload_breach"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        s04Evidence(exact, "g8", "h7", Vector("f6", "h6", "g7", "h7"), "shell_payload_breach"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        s04Evidence(exact, "g8", "g7", Vector("g7"), "shell_payload_breach"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        s04Evidence(exact, "g8", "g7", Vector("f6", "h6", "g7", "h7"), "generic_attack_pressure"),
        Color.White,
        certification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        s04Evidence(exact, "g8", "g7", Vector("f6", "h6", "g7", "h7"), "shell_payload_breach", owner = Color.Black),
        Color.White,
        certification
      ),
      Right(false),
      clues("S04 projection evidence with the wrong owner must not admit")
    )

  test("S04 rejects same-cluster rivals, shortcut negatives, optional strengthening, and stale evidence"):
    val s04 = StrategyProjectionBandId("S04")
    val exact = seedExtraction("6k1/8/6p1/4B3/8/8/7R/6K1 w - - 0 1")
    val s01Rival = seedExtraction("6k1/6pp/8/6p1/3B1p2/8/Q3P1RP/6K1 w - - 0 1")
    val s02Rival = seedExtraction("6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1")
    val s03Rival = seedExtraction("6k1/8/6p1/8/3B4/8/6R1/6K1 w - - 0 1")
    val s24Rival = seedExtraction("4k3/5ppp/8/3n4/3R4/8/5PPP/4K3 w - - 0 1")
    val wordingOnly = seedExtraction("6k1/8/6p1/4B1N1/8/8/8/6K1 w - - 0 1")
    val genericPressure = seedExtraction("6k1/8/8/8/8/8/4R3/6K1 w - - 0 1")
    val certification = kingSafetyEdgeEvidenceFor(exact, Color.White)

    Vector(s01Rival, s02Rival, s03Rival, s24Rival, wordingOnly, genericPressure).foreach: extraction =>
      assertEquals(
        StrategyProjectionAdmission.admits(
          s04,
          extraction,
          s04Evidence(extraction, "g8", "g7", Vector("f6", "h6", "g7", "h7"), "shell_payload_breach"),
          Color.White,
          kingSafetyEdgeEvidenceFor(extraction, Color.White)
        ),
        Right(false)
      )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        StrategyProjectionEvidence.empty,
        Color.White,
        certification
      ),
      Right(false),
      clues("S04 certification-only proof attempt cannot admit without live projection evidence")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        wordingOnly,
        s04Evidence(wordingOnly, "g8", "g7", Vector("f6", "h6", "g7", "h7"), "support_break_breach"),
        Color.White,
        CertificationEvidenceBundle.empty
      ),
      Right(false),
      clues("S04 optional strengthening or shell wording cannot replace certified same-board breach support")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s04,
        exact,
        s04Evidence(s01Rival, "g8", "g7", Vector("f6", "h6", "g7", "h7"), "shell_payload_breach"),
        Color.White,
        certification
      ),
      Left("Strategy projection admission rejected stale evidence bundle")
    )
    assert(
      StrategyProjectionAdmission
        .admits(
          s04,
          exact,
          s04Evidence(exact, "g8", "g7", Vector("f6", "h6", "g7", "h7"), "shell_payload_breach"),
          Color.White,
          kingSafetyEdgeEvidenceFor(s01Rival, Color.White)
        )
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S05 admits only exact same-anchor center-release carrier with route evidence"):
    val s05 = StrategyProjectionBandId("S05")
    val centerRelease = seedExtraction("6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        centerRelease,
        s05Evidence(centerRelease, "c2", "d5", "center_pawn_target"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        centerRelease,
        s05Evidence(centerRelease, "c2", "d5", "central_axis_continuation"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s05, centerRelease, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        centerRelease,
        s05Evidence(centerRelease, "d4", "d5", "center_pawn_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        centerRelease,
        s05Evidence(centerRelease, "c2", "e5", "center_pawn_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        centerRelease,
        s05Evidence(centerRelease, "c2", "d5", "open_center_wording_only"),
        Color.White
      ),
      Right(false)
    )

  test("S05 rejects same-cluster rivals, shortcut negatives, and optional-strengthening-only boards"):
    val s05 = StrategyProjectionBandId("S05")
    val s06Rival = seedExtraction("4k3/8/8/3ppN2/3PP3/7B/8/4K3 w - - 0 1")
    val s14Rival = seedExtraction("4k3/8/1p6/2p5/P7/8/8/4K3 w - - 0 1")
    val s21Rival = seedExtraction("r1bqkbnr/p1ppp3/2n5/6p1/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val s21CenterSourceRival = seedExtraction("r1bqkbnr/p2pp1pp/2n5/2p5/4P2P/2N2N2/PP1P1PP1/R2QKB1R w KQkq - 0 1")
    val centerShellOnly = seedExtraction("6k1/6pp/5n2/3pp3/3PP3/5N2/6PP/6K1 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        s06Rival,
        s05Evidence(s06Rival, "d4", "e5", "center_pawn_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        s14Rival,
        s05Evidence(s14Rival, "a4", "b6", "center_pawn_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        s21Rival,
        s05Evidence(s21Rival, "h2", "g5", "center_pawn_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        s21CenterSourceRival,
        s05Evidence(s21CenterSourceRival, "b2", "c5", "central_axis_continuation"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        centerShellOnly,
        s05Evidence(centerShellOnly, "c2", "d5", "center_pawn_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s05,
        centerShellOnly,
        StrategyProjectionEvidence.empty,
        Color.White,
        certificationEvidenceFor(
          centerShellOnly,
          "InitiativeWindow",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )

  test("S05 projection admission rejects stale exact center-release evidence"):
    val current = seedExtraction("6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1")
    val staleSource = seedExtraction("r1bqkbnr/p2pp1pp/2n5/2p5/4P2P/2N2N2/PP1P1PP1/R2QKB1R w KQkq - 0 1")
    val staleEvidence =
      s05Evidence(staleSource, "b2", "c5", "center_pawn_target")

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S05"), current, staleEvidence, Color.White)
        .left
        .exists(_.contains("stale evidence bundle"))
    )

  test("S07 admits only exact same-board initiative conversion with route evidence and certified lower carriers"):
    val s07 = StrategyProjectionBandId("S07")
    val conversion = seedExtraction("r1bqkbnr/2ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val certifiedLower = developmentInitiativeEvidenceFor(conversion, Color.White)

    assertEquals(
      StrategyProjectionAdmission.admits(
        s07,
        conversion,
        s07Evidence(conversion, "development_led_window"),
        Color.White,
        certifiedLower
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s07,
        conversion,
        s07Evidence(conversion, "move_right_window"),
        Color.White,
        certifiedLower
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s07, conversion, StrategyProjectionEvidence.empty, Color.White, certifiedLower),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s07,
        conversion,
        s07Evidence(conversion, "initiative_wording_only"),
        Color.White,
        certifiedLower
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s07,
        conversion,
        s07Evidence(conversion, "development_led_window", certificationFamily = "DevelopmentComparison"),
        Color.White,
        certifiedLower
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s07,
        conversion,
        s07Evidence(conversion, "development_led_window"),
        Color.White,
        certificationEvidenceFor(
          conversion,
          "DevelopmentComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s07,
        conversion,
        s07Evidence(conversion, "development_led_window"),
        Color.White,
        initiativeWindowEvidenceFor(conversion, Color.White)
      ),
      Right(false)
    )

  test("S07 rejects same-cluster rivals, shortcuts, and optional-strengthening-only boards"):
    val s07 = StrategyProjectionBandId("S07")
    val s08Rival = seedExtraction("r1bqkbnr/p1ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val developmentLeadOnly = seedExtraction("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s07,
        s08Rival,
        s07Evidence(s08Rival, "development_led_window"),
        Color.White,
        developmentInitiativeEvidenceFor(s08Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s07,
        developmentLeadOnly,
        s07Evidence(developmentLeadOnly, "development_led_window"),
        Color.White,
        certificationEvidenceFor(
          developmentLeadOnly,
          "DevelopmentComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s07,
        developmentLeadOnly,
        s07Evidence(developmentLeadOnly, "move_right_window"),
        Color.White,
        initiativeWindowEvidenceFor(developmentLeadOnly, Color.White)
      ),
      Right(false)
    )

  test("S07 projection admission rejects stale projection and certification evidence"):
    val s07 = StrategyProjectionBandId("S07")
    val current = seedExtraction("r1bqkbnr/2ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val staleSource = seedExtraction("r1bqkbnr/p1ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val currentEvidence = s07Evidence(current, "development_led_window")
    val staleEvidence = s07Evidence(staleSource, "development_led_window")
    val currentCertification = developmentInitiativeEvidenceFor(current, Color.White)
    val staleCertification = developmentInitiativeEvidenceFor(staleSource, Color.White)

    assert(
      StrategyProjectionAdmission
        .admits(s07, current, staleEvidence, Color.White, currentCertification)
        .left
        .exists(_.contains("stale evidence bundle"))
    )
    assert(
      StrategyProjectionAdmission
        .admits(s07, current, currentEvidence, Color.White, staleCertification)
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S21 admits only exact owner counterplay source survival with same-board InitiativeWindow evidence"):
    val s21 = StrategyProjectionBandId("S21")
    val centerSurvival = seedExtraction("r1bqkbnr/p2pp1pp/2n5/2p5/4P2P/2N2N2/PP1P1PP1/R2QKB1R w KQkq - 0 1")
    val farWingSurvival = seedExtraction("r1bqkbnr/p1ppp3/2n5/6p1/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val centerCertification = initiativeWindowEvidenceFor(centerSurvival, Color.White)
    val farWingCertification = initiativeWindowEvidenceFor(farWingSurvival, Color.White)

    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        centerSurvival,
        s21Evidence(centerSurvival, "b2", "c5", "center_source_survives"),
        Color.White,
        centerCertification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        centerSurvival,
        s21Evidence(centerSurvival, "d2", "c5", "center_source_survives"),
        Color.White,
        centerCertification
      ),
      Right(false),
      clues("S21 center survival must not admit the S05-shaped center-source/center-target release carrier")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        farWingSurvival,
        s21Evidence(farWingSurvival, "h2", "g5", "far_wing_source_survives"),
        Color.White,
        farWingCertification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        centerSurvival,
        StrategyProjectionEvidence.empty,
        Color.White,
        centerCertification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        centerSurvival,
        s21Evidence(centerSurvival, "b2", "c5", "center_source_survives"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        centerSurvival,
        s21Evidence(centerSurvival, "c2", "c5", "center_source_survives"),
        Color.White,
        centerCertification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        centerSurvival,
        s21Evidence(centerSurvival, "b2", "d5", "center_source_survives"),
        Color.White,
        centerCertification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        centerSurvival,
        s21Evidence(centerSurvival, "b2", "c5", "far_wing_source_survives"),
        Color.White,
        centerCertification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        centerSurvival,
        s21Evidence(centerSurvival, "b2", "c5", "center_source_survives", "DevelopmentComparison"),
        Color.White,
        centerCertification
      ),
      Right(false)
    )

  test("S21 rejects adjacent rivals, shortcuts, and non-certified initiative support"):
    val s21 = StrategyProjectionBandId("S21")
    val s01Rival = seedExtraction("6k1/6pp/8/8/3B1p2/8/4P1R1/6K1 w - - 0 1")
    val s05Rival = seedExtraction("6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1")
    val s08Rival = seedExtraction("r1bqkbnr/p1ppp1pp/2n5/8/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val breakSourceOnly = seedExtraction("4k3/8/8/8/3p4/8/2P5/4K3 w - - 0 1")
    val initiativeOnly = seedExtraction("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3")
    val farWingSurvival = seedExtraction("r1bqkbnr/p1ppp3/2n5/6p1/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        s01Rival,
        s21Evidence(s01Rival, "e2", "f4", "center_source_survives"),
        Color.White,
        initiativeWindowEvidenceFor(s01Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        s05Rival,
        s21Evidence(s05Rival, "c2", "d5", "center_source_survives"),
        Color.White,
        initiativeWindowEvidenceFor(s05Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        s08Rival,
        s21Evidence(s08Rival, "c4", "d5", "center_source_survives"),
        Color.White,
        initiativeWindowEvidenceFor(s08Rival, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        breakSourceOnly,
        s21Evidence(breakSourceOnly, "c2", "d4", "center_source_survives"),
        Color.White,
        initiativeWindowEvidenceFor(breakSourceOnly, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        initiativeOnly,
        StrategyProjectionEvidence.empty,
        Color.White,
        initiativeWindowEvidenceFor(initiativeOnly, Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s21,
        farWingSurvival,
        s21Evidence(farWingSurvival, "h2", "g5", "far_wing_source_survives"),
        Color.White,
        certificationEvidenceFor(
          farWingSurvival,
          "InitiativeWindow",
          Color.White,
          strength = CertificationEvidenceStrength.Insufficient,
          purposes = Map(
            CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Insufficient,
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
          )
        )
      ),
      Right(false)
    )

  test("S21 projection admission rejects stale projection and certification evidence"):
    val current = seedExtraction("r1bqkbnr/p2pp1pp/2n5/2p5/4P2P/2N2N2/PP1P1PP1/R2QKB1R w KQkq - 0 1")
    val staleSource = seedExtraction("r1bqkbnr/p1ppp3/2n5/6p1/2P1P3/2N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val currentEvidence = s21Evidence(current, "b2", "c5", "center_source_survives")
    val staleEvidence = s21Evidence(staleSource, "h2", "g5", "far_wing_source_survives")
    val currentCertification = initiativeWindowEvidenceFor(current, Color.White)
    val staleCertification = initiativeWindowEvidenceFor(staleSource, Color.White)

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S21"), current, staleEvidence, Color.White, currentCertification)
        .left
        .exists(_.contains("stale evidence bundle"))
    )
    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S21"), current, currentEvidence, Color.White, staleCertification)
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S11 admits only exact same-target weak-pawn pressure with fixed persistence evidence"):
    val s11 = StrategyProjectionBandId("S11")
    val fixation = seedExtraction("4k3/8/8/3p4/3P4/8/6B1/4K3 w - - 0 1")
    val repeated = seedExtraction("4k3/8/8/3p4/3P1N2/8/6B1/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        fixation,
        s11Evidence(fixation, "d5", "same_target_fixation", Vector("g2")),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        repeated,
        s11Evidence(repeated, "d5", "same_target_repeated_pressure", Vector("f4", "g2")),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s11, fixation, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        fixation,
        s11Evidence(fixation, "d5", "same_target_repeated_pressure", Vector("g2")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        fixation,
        s11Evidence(fixation, "e5", "same_target_fixation", Vector("g2")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        fixation,
        s11Evidence(fixation, "d5", "same_target_fixation", Vector("h3")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        repeated,
        s11Evidence(repeated, "d5", "same_target_repeated_pressure", Vector("g2")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        repeated,
        s11Evidence(repeated, "d5", "same_target_repeated_pressure", Vector("f4", "h3")),
        Color.White
      ),
      Right(false)
    )

  test("S11 rejects shortcut, same-cluster rival, and optional-strengthening-only boards"):
    val s11 = StrategyProjectionBandId("S11")
    val weakPawnOnly = seedExtraction("4k3/8/8/3p4/3P4/8/8/4K3 w - - 0 1")
    val pressureWithoutPersistence = seedExtraction("4k3/8/8/3p4/8/8/6B1/4K3 w - - 0 1")
    val targetSwap = seedExtraction("4k3/8/8/3pp3/4PN2/8/8/4K3 w - - 0 1")
    val s13Rival = seedExtraction("4k3/8/8/8/1pp5/8/P7/4K3 w - - 0 1")
    val s14Rival = seedExtraction("4k3/8/8/3p4/4p3/2P5/8/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        weakPawnOnly,
        s11Evidence(weakPawnOnly, "d5", "same_target_fixation", Vector("g2")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        pressureWithoutPersistence,
        s11Evidence(pressureWithoutPersistence, "d5", "same_target_fixation", Vector("g2")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        targetSwap,
        s11Evidence(targetSwap, "e5", "same_target_fixation", Vector("f4")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        s13Rival,
        s11Evidence(s13Rival, "b4", "same_target_fixation", Vector("a2")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        s14Rival,
        s11Evidence(s14Rival, "d5", "same_target_fixation", Vector("c3")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s11,
        weakPawnOnly,
        StrategyProjectionEvidence.empty,
        Color.White,
        certificationEvidenceFor(
          weakPawnOnly,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )

  test("S13 admits only exact same-wing-sector wing-damage contact with recomputed target-role evidence"):
    val s13 = StrategyProjectionBandId("S13")
    val phalanx = seedExtraction("4k3/8/8/8/1pp5/8/P7/4K3 w - - 0 1")
    val burdened = seedExtraction("4k3/2p5/2p5/1p6/1P6/P7/8/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        phalanx,
        s13Evidence(phalanx, "a2", "b4", "phalanx_edge_target"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        burdened,
        s13Evidence(burdened, "a3", "b5", "structurally_burdened_target"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s13, phalanx, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        phalanx,
        s13Evidence(phalanx, "a3", "b4", "phalanx_edge_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        phalanx,
        s13Evidence(phalanx, "a2", "c4", "phalanx_edge_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        phalanx,
        s13Evidence(phalanx, "a2", "b4", "structurally_burdened_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        phalanx,
        s13Evidence(phalanx, "a2", "b4", "phalanx_edge_target", "center"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        phalanx,
        StrategyProjectionEvidence.empty,
        Color.White,
        certificationEvidenceFor(
          phalanx,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )

  test("S13 rejects shortcut, same-cluster rival, and adjacent false-rival boards even with S13 evidence"):
    val s13 = StrategyProjectionBandId("S13")
    val s11Rival = seedExtraction("4k3/8/8/3p4/3P1N2/8/6B1/4K3 w - - 0 1")
    val s14Rival = seedExtraction("4k3/8/8/3p4/4p3/2P5/8/4K3 w - - 0 1")
    val s15Rival = seedExtraction("4k3/8/1p6/2p5/PP6/8/8/4K3 w - - 0 1")
    val chainBasePhalanxOverlap = seedExtraction("4k3/8/8/8/1pp5/2p5/P7/4K3 w - - 0 1")
    val centerSectorDamage = seedExtraction("4k3/8/3pp3/8/3P4/8/8/4K3 w - - 0 1")
    val sectorOnly = seedExtraction("4k3/pp6/8/8/8/8/PPP5/4K3 w - - 0 1")
    val weakPawnOnly = seedExtraction("4k3/8/8/3p4/3P4/8/8/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        s11Rival,
        s13Evidence(s11Rival, "d4", "d5", "structurally_burdened_target", "center"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        s14Rival,
        s13Evidence(s14Rival, "c3", "d5", "structurally_burdened_target", "center"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        s15Rival,
        s13Evidence(s15Rival, "a4", "b6", "structurally_burdened_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        chainBasePhalanxOverlap,
        s13Evidence(chainBasePhalanxOverlap, "a2", "b4", "phalanx_edge_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        centerSectorDamage,
        s13Evidence(centerSectorDamage, "d4", "e6", "phalanx_edge_target", "center"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        sectorOnly,
        s13Evidence(sectorOnly, "a2", "b7", "phalanx_edge_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s13,
        weakPawnOnly,
        s13Evidence(weakPawnOnly, "d4", "d5", "structurally_burdened_target", "center"),
        Color.White
      ),
      Right(false)
    )

  test("S14 admits only exact same-anchor non-center chain-base contact with route evidence"):
    val s14 = StrategyProjectionBandId("S14")
    val chainBaseTarget = seedExtraction("4k3/8/6p1/5p2/7P/8/8/4K3 w - - 0 1")
    val continuation = seedExtraction("4k3/8/1p6/2p5/P2p4/8/8/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        chainBaseTarget,
        s14Evidence(chainBaseTarget, "h4", "g6", "chain_base_target", Vector("f5")),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        continuation,
        s14Evidence(continuation, "a4", "b6", "base_contact_continuation", Vector("c5")),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s14, chainBaseTarget, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        chainBaseTarget,
        s14Evidence(chainBaseTarget, "a4", "g6", "chain_base_target", Vector("f5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        chainBaseTarget,
        s14Evidence(chainBaseTarget, "h4", "f5", "chain_base_target", Vector("f5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        chainBaseTarget,
        s14Evidence(chainBaseTarget, "h4", "g6", "generic_structural_damage", Vector("f5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        chainBaseTarget,
        s14Evidence(chainBaseTarget, "h4", "g6", "base_contact_continuation", Vector("f5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        continuation,
        s14Evidence(continuation, "a4", "b6", "chain_base_target", Vector("c5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        chainBaseTarget,
        s14Evidence(chainBaseTarget, "h4", "g6", "chain_base_target", Vector("h5")),
        Color.White
      ),
      Right(false)
    )

  test("S14 rejects same-cluster rivals, shortcut negatives, and optional-strengthening-only boards"):
    val s14 = StrategyProjectionBandId("S14")
    val s05Rival = seedExtraction("6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1")
    val s11Rival = seedExtraction("4k3/8/8/3p4/3P1N2/8/6B1/4K3 w - - 0 1")
    val s13Rival = seedExtraction("4k3/8/8/8/1pp5/8/P7/4K3 w - - 0 1")
    val s15Rival = seedExtraction("4k3/8/4p1p1/8/4PP2/8/8/4K3 w - - 0 1")
    val fixedChainOnly = seedExtraction("k7/6p1/5pP1/5P2/2BBN3/5N2/8/K7 w - - 0 1")
    val structuralOnly = seedExtraction("4k3/8/8/3p4/3P4/8/8/4K3 w - - 0 1")
    val optionalStrengtheningOnly = seedExtraction("4k3/8/8/3p4/3P1N2/8/6B1/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        s05Rival,
        s14Evidence(s05Rival, "d4", "e5", "chain_base_target", Vector("d4")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        s11Rival,
        s14Evidence(s11Rival, "d4", "d5", "chain_base_target", Vector("d4")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        s13Rival,
        s14Evidence(s13Rival, "a2", "b4", "chain_base_target", Vector("c3")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        s15Rival,
        s14Evidence(s15Rival, "e4", "e6", "chain_base_target", Vector("f5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        fixedChainOnly,
        s14Evidence(fixedChainOnly, "g6", "f7", "chain_base_target", Vector("g6")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        structuralOnly,
        s14Evidence(structuralOnly, "d4", "d5", "chain_base_target", Vector("d4")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s14,
        optionalStrengtheningOnly,
        StrategyProjectionEvidence.empty,
        Color.White,
        certificationEvidenceFor(
          optionalStrengtheningOnly,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )

  test("S15 admits only exact same-candidate passer creation through S13 or S14 evidence"):
    val s15 = StrategyProjectionBandId("S15")
    val s13Route = seedExtraction("4k3/2p5/1pp5/8/PP6/8/8/4K3 w - - 0 1")
    val s14Route = seedExtraction("4k3/8/1p6/2p5/PP6/8/8/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s13Route,
        s15WingDamageEvidence(s13Route, "a4", "b6", "phalanx_edge_target"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s14Route,
        s15ChainBaseEvidence(s14Route, "a4", "b6", "chain_base_target", Vector("c5")),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s15, s13Route, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s13Route,
        s15WingDamageEvidence(s13Route, "b4", "b6", "phalanx_edge_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s13Route,
        s15WingDamageEvidence(s13Route, "a4", "c6", "phalanx_edge_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s13Route,
        s15WingDamageEvidence(s13Route, "a4", "b6", "structurally_burdened_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s13Route,
        s15WingDamageEvidence(s13Route, "a4", "b6", "phalanx_edge_target", "center"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s14Route,
        s15ChainBaseEvidence(s14Route, "a4", "b6", "base_contact_continuation", Vector("c5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s14Route,
        s15ChainBaseEvidence(s14Route, "a4", "b6", "chain_base_target", Vector("a5")),
        Color.White
      ),
      Right(false)
    )

  test("S15 rejects adjacent false rivals, shortcut negatives, and optional-strengthening-only boards"):
    val s15 = StrategyProjectionBandId("S15")
    val s13Rival = seedExtraction("4k3/8/8/8/1pp5/8/P7/4K3 w - - 0 1")
    val s14Rival = seedExtraction("4k3/8/1p6/2p5/P7/8/8/4K3 w - - 0 1")
    val s16Rival = seedExtraction("8/2p5/3P4/k7/6p1/8/4K3/8 w - - 0 1")
    val candidateOnly = seedExtraction("4k3/8/4p1p1/8/4PP2/8/8/4K3 w - - 0 1")
    val shellOnly = seedExtraction("4k3/8/8/4p3/4P3/8/8/4K3 w - - 0 1")
    val existingPasserOnly = seedExtraction("4k3/8/8/8/3P4/8/8/4K3 w - - 0 1")
    val splitAnchor = seedExtraction("4k3/3p4/1p6/2pP4/P3P3/8/8/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s13Rival,
        s15WingDamageEvidence(s13Rival, "a2", "b4", "phalanx_edge_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s14Rival,
        s15ChainBaseEvidence(s14Rival, "a4", "b6", "chain_base_target", Vector("c5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        s16Rival,
        s15WingDamageEvidence(s16Rival, "d6", "c7", "phalanx_edge_target"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        candidateOnly,
        s15WingDamageEvidence(candidateOnly, "e4", "e6", "phalanx_edge_target", "kingside"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        shellOnly,
        s15ChainBaseEvidence(shellOnly, "e4", "e5", "chain_base_target", Vector("e4")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        existingPasserOnly,
        s15WingDamageEvidence(existingPasserOnly, "d4", "d6", "structurally_burdened_target", "center"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        splitAnchor,
        s15ChainBaseEvidence(splitAnchor, "e4", "b6", "base_contact_continuation", Vector("c5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        splitAnchor,
        s15ChainBaseEvidence(splitAnchor, "a4", "b6", "chain_base_target", Vector("c5")),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s15,
        candidateOnly,
        StrategyProjectionEvidence.empty,
        Color.White,
        certificationEvidenceFor(candidateOnly, "PromotionRace", Color.White)
      ),
      Right(false)
    )

  test("S17 admits only same-piece liability plus same-piece relief with certified relief evidence"):
    val fen = "4k3/8/8/3b4/5N2/8/8/4K3 b - - 0 1"
    val extraction = seedExtraction(fen)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S17"),
        kind = StrategyProjectionEvidenceKind("liability_relief_certified"),
        owner = Color.Black,
        anchor = pieceAnchor("d5"),
        payload = WitnessPayload("relief_kind" -> WitnessValue.Token("repair_route"))
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S17"),
        extraction,
        evidence,
        Color.Black
      ),
      Right(true)
    )

  test("S17 rejects liability-only boards even when a relief evidence claim is supplied"):
    val fen = "6kb/5Npp/8/8/8/8/8/4K3 b - - 0 1"
    val extraction = seedExtraction(fen)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S17"),
        kind = StrategyProjectionEvidenceKind("liability_relief_certified"),
        owner = Color.Black,
        anchor = pieceAnchor("h8"),
        payload = WitnessPayload("relief_kind" -> WitnessValue.Token("repair_route"))
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S17"),
        extraction,
        evidence,
        Color.Black
      ),
      Right(false)
    )

  test("S17 rejects adjacent bishop-pair and mobility rivals even with S17 evidence"):
    val s17 = StrategyProjectionBandId("S17")
    val bishopPairRival = seedExtraction("4k3/8/8/8/8/8/6B1/2B1K3 w - - 0 1")
    val mobilityRival = seedExtraction("8/8/1nQ3n1/4p3/3b2K1/8/1k6/8 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s17,
        bishopPairRival,
        evidenceFor(
          bishopPairRival,
          StrategyProjectionEvidenceClaim(
            bandId = s17,
            kind = StrategyProjectionEvidenceKind("liability_relief_certified"),
            owner = Color.White,
            anchor = pieceAnchor("c1"),
            payload = WitnessPayload("relief_kind" -> WitnessValue.Token("repair_route"))
          )
        ),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s17,
        mobilityRival,
        evidenceFor(
          mobilityRival,
          StrategyProjectionEvidenceClaim(
            bandId = s17,
            kind = StrategyProjectionEvidenceKind("liability_relief_certified"),
            owner = Color.White,
            anchor = pieceAnchor("d4"),
            payload = WitnessPayload("relief_kind" -> WitnessValue.Token("repair_route"))
          )
        ),
        Color.White
      ),
      Right(false)
    )

  test("S18 admits initiative, structure, and material conversion only with exact bishop-pair carriers"):
    val s18 = StrategyProjectionBandId("S18")
    val initiative = seedExtraction("r1bqkbnr/p1ppp1pp/2n5/8/2P1P3/B1N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1")
    val structure = seedExtraction("8/8/1nQ3n1/1B2p3/3b2K1/8/1k4B1/8 w - - 0 1")
    val material = seedExtraction("4k3/5ppp/8/8/8/7B/1B3Pn1/4K3 w - - 0 1")

    val initiativeEvidence = evidenceFor(
      initiative,
      StrategyProjectionEvidenceClaim(
        bandId = s18,
        kind = StrategyProjectionEvidenceKind("bishop_pair_initiative_conversion_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.BoardAnchor,
        payload = s18EvidencePayload(
          certificationFamily = "InitiativeWindow",
          bishopMemberSquares = Vector("f1", "a3"),
          activeBishopSquare = "a3",
          conversionTargetSquares = Vector("c4", "e4")
        )
      )
    )
    val structureEvidence = evidenceFor(
      structure,
      StrategyProjectionEvidenceClaim(
        bandId = s18,
        kind = StrategyProjectionEvidenceKind("bishop_pair_structure_conversion_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.BoardAnchor,
        payload = s18EvidencePayload(
          certificationFamily = "MobilityComparison",
          bishopMemberSquares = Vector("g2", "b5"),
          activeBishopSquare = "b5",
          conversionTargetSquares = Vector("c3", "c5")
        )
      )
    )
    val materialEvidence = evidenceFor(
      material,
      StrategyProjectionEvidenceClaim(
        bandId = s18,
        kind = StrategyProjectionEvidenceKind("bishop_pair_material_conversion_certified"),
        owner = Color.White,
        anchor = pieceAnchor("h3"),
        payload = s18EvidencePayload(
          certificationFamily = "MaterialHarvest",
          bishopMemberSquares = Vector("b2", "h3"),
          activeBishopSquare = "h3",
          conversionTargetSquares = Vector("g2")
        )
      )
    )
    val initiativeCertification =
      certificationEvidenceFor(
        initiative,
        "InitiativeWindow",
        Color.White,
        purposes = Map(
          CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied,
          CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
        )
      )

    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        initiative,
        initiativeEvidence,
        Color.White,
        initiativeCertification
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        structure,
        structureEvidence,
        Color.White,
        certificationEvidenceFor(
          structure,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        material,
        materialEvidence,
        Color.White,
        certificationEvidenceFor(
          material,
          "MaterialHarvest",
          Color.White,
          purposes = Map(
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
            CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
          )
        )
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        initiative,
        StrategyProjectionEvidence.empty,
        Color.White,
        certificationEvidenceFor(
          initiative,
          "InitiativeWindow",
          Color.White,
          purposes = Map(
            CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied,
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
          )
        )
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s18, initiative, initiativeEvidence, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        initiative,
        evidenceFor(
          initiative,
          StrategyProjectionEvidenceClaim(
            bandId = s18,
            kind = StrategyProjectionEvidenceKind("bishop_pair_initiative_conversion_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.White,
        initiativeCertification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        initiative,
        evidenceFor(
          initiative,
          StrategyProjectionEvidenceClaim(
            bandId = s18,
            kind = StrategyProjectionEvidenceKind("bishop_pair_initiative_conversion_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor,
            payload = s18EvidencePayload(
              certificationFamily = "MobilityComparison",
              bishopMemberSquares = Vector("f1", "a3"),
              activeBishopSquare = "a3",
              conversionTargetSquares = Vector("c4", "e4")
            )
          )
        ),
        Color.White,
        initiativeCertification
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        initiative,
        evidenceFor(
          initiative,
          StrategyProjectionEvidenceClaim(
            bandId = s18,
            kind = StrategyProjectionEvidenceKind("bishop_pair_initiative_conversion_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor,
            payload = s18EvidencePayload(
              certificationFamily = "InitiativeWindow",
              bishopMemberSquares = Vector("f1", "a3"),
              activeBishopSquare = "a3",
              conversionTargetSquares = Vector("c4")
            )
          )
        ),
        Color.White,
        initiativeCertification
      ),
      Right(false)
    )

  test("S20 admits only exact same-board mobility domination routes with matching evidence"):
    val s20 = StrategyProjectionBandId("S20")
    val mobilityRestriction = seedExtraction("8/8/1nQ3n1/4p3/3b2K1/8/1k6/8 w - - 0 1")
    val defenderStarvation = seedExtraction("3q2k1/7p/2Q2n2/6B1/8/3B4/8/4K3 w - - 0 1")
    val spaceClampRival = seedExtraction("4k3/8/8/3ppN2/3PP3/7B/8/4K3 w - - 0 1")
    val weakSquareRival = seedExtraction("6k1/8/3P4/3rB3/5P2/8/8/6K1 w - - 0 1")
    val liabilityRival = seedExtraction("6kb/5Npp/8/8/8/8/8/4K3 b - - 0 1")
    val mobilityOnly = seedExtraction("6k1/8/8/8/8/8/6N1/6K1 w - - 0 1")
    val restrictionOnly = seedExtraction("6k1/8/1P6/1rB5/3P4/8/8/6K1 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        mobilityRestriction,
        s20Evidence(mobilityRestriction, "d4", "mobility_plus_restriction"),
        Color.White,
        certificationEvidenceFor(
          mobilityRestriction,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        defenderStarvation,
        s20Evidence(defenderStarvation, "f6", "defender_starvation"),
        Color.White,
        certificationEvidenceFor(
          defenderStarvation,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        mobilityRestriction,
        s20Evidence(mobilityRestriction, "d4", "defender_starvation"),
        Color.White,
        certificationEvidenceFor(
          mobilityRestriction,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        defenderStarvation,
        s20Evidence(defenderStarvation, "f6", "defender_starvation", supportWitnessId = "short_run_slider_gate_restriction"),
        Color.White,
        certificationEvidenceFor(
          defenderStarvation,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        mobilityRestriction,
        StrategyProjectionEvidence.empty,
        Color.White,
        certificationEvidenceFor(
          mobilityRestriction,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        spaceClampRival,
        s20Evidence(spaceClampRival, "f5", "mobility_plus_restriction"),
        Color.White,
        CertificationEvidenceBundle.empty
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        weakSquareRival,
        s20Evidence(weakSquareRival, "e5", "mobility_plus_restriction"),
        Color.White,
        CertificationEvidenceBundle.empty
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        liabilityRival,
        s20Evidence(liabilityRival, "h8", "defender_starvation"),
        Color.White,
        CertificationEvidenceBundle.empty
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        mobilityOnly,
        s20Evidence(mobilityOnly, "g2", "mobility_plus_restriction"),
        Color.White,
        CertificationEvidenceBundle.empty
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s20,
        restrictionOnly,
        s20Evidence(restrictionOnly, "b5", "mobility_plus_restriction"),
        Color.White,
        CertificationEvidenceBundle.empty
      ),
      Right(false)
    )

  test("S10 admits only exact same-anchor knight outpost occupation routes with matching evidence"):
    val s10 = StrategyProjectionBandId("S10")
    val knightOnly = seedExtraction("4k3/8/8/3N4/8/2P5/8/4K3 w - - 0 1")
    val evictionDenied = seedExtraction("4k3/2p5/2P5/3N4/8/2P5/8/4K3 w - - 0 1")
    val s12WeakSquareRival = seedExtraction("6k1/8/3P4/3rB3/5P2/8/8/6K1 w - - 0 1")
    val goodPieceOnly = seedExtraction("4k3/8/8/3N4/8/8/8/4K3 w - - 0 1")
    val nonDurableOccupancy = seedExtraction("4k3/8/2p5/3N4/8/8/8/4K3 w - - 0 1")
    val nonKnightOccupancy = seedExtraction("4k3/8/8/3B4/8/2P5/8/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s10,
        knightOnly,
        s10Evidence(knightOnly, "d5", "knight_only_outpost_occupancy"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s10,
        evictionDenied,
        s10Evidence(evictionDenied, "d5", "same_anchor_eviction_denial"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s10,
        knightOnly,
        s10Evidence(knightOnly, "d5", "same_anchor_eviction_denial"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s10,
        knightOnly,
        StrategyProjectionEvidence.empty,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s10,
        s12WeakSquareRival,
        s10Evidence(s12WeakSquareRival, "e5", "knight_only_outpost_occupancy"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s10,
        goodPieceOnly,
        s10Evidence(goodPieceOnly, "d5", "knight_only_outpost_occupancy"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s10,
        nonDurableOccupancy,
        s10Evidence(nonDurableOccupancy, "d5", "knight_only_outpost_occupancy"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s10,
        nonKnightOccupancy,
        s10Evidence(nonKnightOccupancy, "d5", "knight_only_outpost_occupancy"),
        Color.White
      ),
      Right(false)
    )

  test("S12 admits only exact local weak-square or diagonal access routes with matching evidence"):
    val s12 = StrategyProjectionBandId("S12")
    val weakRoute = seedExtraction("6k1/8/3P4/3rB3/5P2/8/8/6K1 w - - 0 1")
    val diagonalRoute = seedExtraction("7k/8/1P6/1rB5/3P4/8/8/6K1 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        weakRoute,
        s12Evidence(weakRoute, "e5", "weak_square_route", "weak_outpost_square_state", "d5"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        diagonalRoute,
        s12Evidence(diagonalRoute, "c5", "diagonal_lane_route", "diagonal_lane_only", "b5"),
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s12, weakRoute, StrategyProjectionEvidence.empty, Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        weakRoute,
        s12Evidence(weakRoute, "e5", "diagonal_wording_only", "diagonal_lane_only", "d5"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        weakRoute,
        s12Evidence(weakRoute, "e5", "weak_square_route", "weak_outpost_square_state", "e5"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        diagonalRoute,
        s12Evidence(diagonalRoute, "c5", "diagonal_lane_route", "weak_outpost_square_state", "b5"),
        Color.White
      ),
      Right(false)
    )

  test("S12 rejects adjacent false rivals, shortcuts, optional strengthening, and stale evidence"):
    val s12 = StrategyProjectionBandId("S12")
    val weakRoute = seedExtraction("6k1/8/3P4/3rB3/5P2/8/8/6K1 w - - 0 1")
    val staleSource = seedExtraction("7k/8/1P6/1rB5/3P4/8/8/6K1 w - - 0 1")
    val s03Rival = seedExtraction("7k/8/8/8/8/2B5/8/6K1 b - - 0 1")
    val s03KingRingDiagonalRival = seedExtraction("6k1/8/1P6/1rB5/3P4/8/8/6K1 w - - 0 1")
    val s10Rival = seedExtraction("4k3/2p1p3/8/3N4/2P1P3/8/8/4K3 w - - 0 1")
    val s20Rival = seedExtraction("6kq/5Npp/8/8/8/8/8/4K3 b - - 0 1")
    val diagonalWordingOnly = seedExtraction("6k1/8/8/8/8/2B5/8/6K1 w - - 0 1")
    val weakWordingOnly = seedExtraction("4k3/8/8/3p4/8/3P4/8/4K3 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        s03Rival,
        s12Evidence(s03Rival, "c3", "diagonal_lane_route", "diagonal_lane_only", "c3"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        s03KingRingDiagonalRival,
        s12Evidence(s03KingRingDiagonalRival, "c5", "diagonal_lane_route", "diagonal_lane_only", "b5"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        s10Rival,
        s12Evidence(s10Rival, "d5", "weak_square_route", "weak_outpost_square_state", "d5"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        s20Rival,
        s12Evidence(s20Rival, "f7", "weak_square_route", "weak_outpost_square_state", "h8"),
        Color.White,
        certificationEvidenceFor(s20Rival, "MobilityComparison", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        diagonalWordingOnly,
        s12Evidence(diagonalWordingOnly, "c3", "diagonal_lane_route", "diagonal_lane_only", "c3"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        weakWordingOnly,
        s12Evidence(weakWordingOnly, "d5", "weak_square_route", "weak_outpost_square_state", "d5"),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        weakRoute,
        s12Evidence(staleSource, "c5", "diagonal_lane_route", "diagonal_lane_only", "b5"),
        Color.White
      ),
      Left("Strategy projection admission rejected stale evidence bundle")
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        weakRoute,
        s12Evidence(
          weakRoute,
          "e5",
          "weak_square_route",
          "weak_outpost_square_state",
          "d5",
          certificationFamily = Some("MobilityComparison")
        ),
        Color.White,
        certificationEvidenceFor(weakRoute, "MobilityComparison", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s12,
        weakRoute,
        s12Evidence(
          weakRoute,
          "e5",
          "weak_square_route",
          "weak_outpost_square_state",
          "d5",
          extraPayloadEntries = Vector("object_family" -> WitnessValue.Token("AttackScaffold"))
        ),
        Color.White
      ),
      Right(false)
    )

  test("S18 rejects same-cluster rivals, shortcut negatives, and wrong material anchors"):
    val s18 = StrategyProjectionBandId("S18")
    val s12Rival = seedExtraction("6k1/8/3P4/3rB3/5P2/8/8/6K1 w - - 0 1")
    val s17Rival = seedExtraction("4k3/8/8/3b4/5N2/8/6B1/7K b - - 0 1")
    val s20Rival = seedExtraction("8/8/1nQ3n1/4p3/3b2K1/8/1k6/8 w - - 0 1")
    val bishopPairOnly = seedExtraction("4k3/8/8/8/8/8/6B1/2B1K3 w - - 0 1")
    val minorEdgeLabelOnly = seedExtraction("4k3/2p1p3/8/3N4/2P1P3/8/8/4K3 w - - 0 1")
    val material = seedExtraction("4k3/5ppp/8/8/8/7B/1B3Pn1/4K3 w - - 0 1")

    def boardEvidence(extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction) =
      evidenceFor(
        extraction,
        StrategyProjectionEvidenceClaim(
          bandId = s18,
          kind = StrategyProjectionEvidenceKind("bishop_pair_structure_conversion_certified"),
          owner = Color.White,
          anchor = WitnessAnchor.BoardAnchor
        )
      )

    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        s12Rival,
        boardEvidence(s12Rival),
        Color.White,
        certificationEvidenceFor(
          s12Rival,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        s17Rival,
        evidenceFor(
          s17Rival,
          StrategyProjectionEvidenceClaim(
            bandId = s18,
            kind = StrategyProjectionEvidenceKind("bishop_pair_material_conversion_certified"),
            owner = Color.Black,
            anchor = pieceAnchor("d5")
          )
        ),
        Color.Black,
        certificationEvidenceFor(
          s17Rival,
          "MaterialHarvest",
          Color.Black,
          purposes = Map(
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
            CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
          )
        )
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        s20Rival,
        boardEvidence(s20Rival),
        Color.White,
        certificationEvidenceFor(
          s20Rival,
          "MobilityComparison",
          Color.White,
          purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        )
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s18, bishopPairOnly, boardEvidence(bishopPairOnly), Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(s18, minorEdgeLabelOnly, boardEvidence(minorEdgeLabelOnly), Color.White),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s18,
        material,
        evidenceFor(
          material,
          StrategyProjectionEvidenceClaim(
            bandId = s18,
            kind = StrategyProjectionEvidenceKind("bishop_pair_material_conversion_certified"),
            owner = Color.White,
            anchor = pieceAnchor("b2")
          )
        ),
        Color.White,
        certificationEvidenceFor(
          material,
          "MaterialHarvest",
          Color.White,
          purposes = Map(
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
            CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
          )
        )
      ),
      Right(false)
    )

  test("S16 admits only exact same-enemy-passer suppression with route evidence and certified support"):
    val s16 = StrategyProjectionBandId("S16")
    val blockadeHold = seedExtraction("6k1/5b1r/6n1/6P1/8/8/8/R5K1 w - - 0 1")
    val restrictionHold = seedExtraction("5r1k/6R1/3Pp1K1/3rB3/p7/N4N2/8/8 w - - 0 1")
    val unrelatedRestrictionHold = seedExtraction("5r1k/6R1/3P2K1/3rB3/p4P2/N7/8/8 w - - 0 1")
    val nonLosingRace = seedExtraction("8/2p5/3P4/k7/6p1/8/4K3/8 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        blockadeHold,
        s16Evidence(blockadeHold, Color.Black, "blockade_hold", "g5", Some("g6"), "FortressDrawCertification"),
        Color.Black,
        certificationEvidenceFor(blockadeHold, "FortressDrawCertification", Color.Black)
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        restrictionHold,
        s16Evidence(restrictionHold, Color.White, "restriction_hold", "e6", Some("e5"), "PerpetualCheckHolding"),
        Color.White,
        certificationEvidenceFor(restrictionHold, "PerpetualCheckHolding", Color.White)
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        nonLosingRace,
        s16Evidence(nonLosingRace, Color.White, "non_losing_race", "g4", None, "PromotionRace"),
        Color.White,
        certificationEvidenceFor(nonLosingRace, "PromotionRace", Color.White)
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        blockadeHold,
        StrategyProjectionEvidence.empty,
        Color.Black,
        certificationEvidenceFor(blockadeHold, "FortressDrawCertification", Color.Black)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        blockadeHold,
        s16Evidence(blockadeHold, Color.Black, "blockade_hold", "g5", Some("g6"), "FortressDrawCertification"),
        Color.Black,
        CertificationEvidenceBundle.empty
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        blockadeHold,
        s16Evidence(blockadeHold, Color.Black, "blockade_hold", "g5", Some("g8"), "FortressDrawCertification"),
        Color.Black,
        certificationEvidenceFor(blockadeHold, "FortressDrawCertification", Color.Black)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        restrictionHold,
        s16Evidence(restrictionHold, Color.White, "blockade_hold", "e6", Some("e5"), "PerpetualCheckHolding"),
        Color.White,
        certificationEvidenceFor(restrictionHold, "PerpetualCheckHolding", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        unrelatedRestrictionHold,
        s16Evidence(
          unrelatedRestrictionHold,
          Color.White,
          "restriction_hold",
          "a4",
          Some("a3"),
          "PerpetualCheckHolding"
        ),
        Color.White,
        certificationEvidenceFor(unrelatedRestrictionHold, "PerpetualCheckHolding", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        nonLosingRace,
        s16Evidence(nonLosingRace, Color.White, "non_losing_race", "c7", None, "PromotionRace"),
        Color.White,
        certificationEvidenceFor(nonLosingRace, "PromotionRace", Color.White)
      ),
      Right(false)
    )

  test("S16 rejects adjacent false rivals, shortcut negatives, and optional-strengthening-only boards"):
    val s16 = StrategyProjectionBandId("S16")
    val s15Rival = seedExtraction("4k3/8/1p6/2p5/PP6/8/8/4K3 w - - 0 1")
    val s22Rival = seedExtraction("7k/6pp/8/8/8/4K3/3N4/8 w - - 0 1")
    val s23Rival = seedExtraction("8/8/4k3/8/4K3/8/8/8 b - - 0 1")
    val enemyPasserOnly = seedExtraction("4k3/8/8/3p4/8/8/8/4K3 w - - 0 1")
    val blockerOnly = seedExtraction("4k3/8/8/3p4/3N4/8/8/4K3 w - - 0 1")
    val optionalHoldOnly = seedExtraction("7k/6pp/8/8/8/4K3/3N4/8 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        s15Rival,
        s16Evidence(s15Rival, Color.White, "non_losing_race", "a4", None, "PromotionRace"),
        Color.White,
        certificationEvidenceFor(s15Rival, "PromotionRace", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        s22Rival,
        s16Evidence(s22Rival, Color.Black, "blockade_hold", "g7", Some("g6"), "FortressDrawCertification"),
        Color.Black,
        certificationEvidenceFor(s22Rival, "FortressDrawCertification", Color.Black)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        s23Rival,
        s16Evidence(s23Rival, Color.White, "restriction_hold", "e6", Some("e5"), "PerpetualCheckHolding"),
        Color.White,
        certificationEvidenceFor(s23Rival, "PerpetualCheckHolding", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        enemyPasserOnly,
        s16Evidence(enemyPasserOnly, Color.White, "non_losing_race", "d5", None, "PromotionRace"),
        Color.White,
        certificationEvidenceFor(enemyPasserOnly, "PromotionRace", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        blockerOnly,
        s16Evidence(blockerOnly, Color.White, "blockade_hold", "d5", Some("d4"), "FortressDrawCertification"),
        Color.White,
        certificationEvidenceFor(blockerOnly, "FortressDrawCertification", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s16,
        optionalHoldOnly,
        StrategyProjectionEvidence.empty,
        Color.Black,
        certificationEvidenceFor(optionalHoldOnly, "FortressDrawCertification", Color.Black)
      ),
      Right(false)
    )

  test("S19 admits material simplification only with same-task TradeInvariant and MaterialHarvest"):
    val before = "4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"
    val after = "4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1"
    val extraction = seedExtraction(before)
    val delta = deltaExtraction(before, "d6c7", after)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S19"),
        kind = StrategyProjectionEvidenceKind("trade_invariant_material_simplification_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.BoardAnchor
      )
    )
    val certificationEvidence =
      certificationEvidenceFor(
        extraction,
        "MaterialHarvest",
        Color.White,
        purposes = Map(
          CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
          CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
        )
      )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S19"),
        extraction,
        evidence,
        Color.White,
        certificationEvidence,
        Some(delta)
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S19"),
        extraction,
        StrategyProjectionEvidence.empty,
        Color.White,
        certificationEvidence,
        Some(delta)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S19"),
        extraction,
        evidence,
        Color.White,
        CertificationEvidenceBundle.empty,
        Some(delta)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S19"),
        extraction,
        evidence,
        Color.White,
        certificationEvidence
      ),
      Right(false)
    )

  test("S19 admits hold simplification only with post-trade fortress certification"):
    val before = "4n2k/6Bp/3P4/8/8/4K1p1/3N4/8 b - - 0 1"
    val after = "7k/6np/3P4/8/8/4K1p1/3N4/8 w - - 0 2"
    val extraction = seedExtraction(after)
    val delta = deltaExtraction(before, "e8g7", after)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S19"),
        kind = StrategyProjectionEvidenceKind("trade_invariant_hold_simplification_certified"),
        owner = Color.Black,
        anchor = WitnessAnchor.BoardAnchor
      )
    )
    val certificationEvidence = certificationEvidenceFor(extraction, "FortressDrawCertification", Color.Black)

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S19"),
        extraction,
        evidence,
        Color.Black,
        certificationEvidence,
        Some(delta)
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S19"),
        extraction,
        evidence,
        Color.Black,
        CertificationEvidenceBundle.empty,
        Some(delta)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S19"),
        extraction,
        evidenceFor(
          extraction,
          StrategyProjectionEvidenceClaim(
            bandId = StrategyProjectionBandId("S19"),
            kind = StrategyProjectionEvidenceKind("trade_invariant_material_simplification_certified"),
            owner = Color.Black,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.Black,
        certificationEvidence,
        Some(delta)
      ),
      Right(false)
    )

  test("S19 rejects same-cluster rivals and shortcut negatives even with S19 evidence"):
    val s19 = StrategyProjectionBandId("S19")
    val holdRival = seedExtraction("7k/6pp/8/8/8/4K3/3N4/8 w - - 0 1")
    val preparedTargetRival = seedExtraction("6k1/8/4b3/3q4/8/8/6B1/3R2K1 w - - 0 1")
    val resultOnlyBefore = "k7/4n3/3P4/8/6p1/8/4K3/8 w - - 0 1"
    val resultOnlyAfter = "k7/4P3/8/8/6p1/8/4K3/8 b - - 0 1"
    val resultOnly = seedExtraction(resultOnlyAfter)
    val resultOnlyDelta = deltaExtraction(resultOnlyBefore, "d6e7", resultOnlyAfter)
    val tradeWordingBefore = "4k3/8/8/3r4/3R4/8/8/4K3 w - - 0 1"
    val tradeWordingAfter = "4k3/8/8/3R4/8/8/8/4K3 b - - 0 1"
    val tradeWording = seedExtraction(tradeWordingBefore)
    val tradeWordingDelta = deltaExtraction(tradeWordingBefore, "d4d5", tradeWordingAfter)
    val materialOnlyBefore = "4k3/2r5/3P4/8/8/8/4K3/8 w - - 0 1"
    val materialOnlyAfter = "4k3/2P5/8/8/8/8/4K3/8 b - - 0 1"
    val materialOnly = seedExtraction(materialOnlyBefore)
    val materialOnlyDelta = deltaExtraction(materialOnlyBefore, "d6c7", materialOnlyAfter)

    assertEquals(
      StrategyProjectionAdmission.admits(
        s19,
        holdRival,
        evidenceFor(
          holdRival,
          StrategyProjectionEvidenceClaim(
            bandId = s19,
            kind = StrategyProjectionEvidenceKind("trade_invariant_hold_simplification_certified"),
            owner = Color.Black,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.Black,
        certificationEvidenceFor(holdRival, "FortressDrawCertification", Color.Black)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s19,
        preparedTargetRival,
        evidenceFor(
          preparedTargetRival,
          StrategyProjectionEvidenceClaim(
            bandId = s19,
            kind = StrategyProjectionEvidenceKind("trade_invariant_material_simplification_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s19,
        resultOnly,
        evidenceFor(
          resultOnly,
          StrategyProjectionEvidenceClaim(
            bandId = s19,
            kind = StrategyProjectionEvidenceKind("trade_invariant_hold_simplification_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.White,
        certificationEvidenceFor(resultOnly, "WinningEndgame", Color.White),
        Some(resultOnlyDelta)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s19,
        tradeWording,
        evidenceFor(
          tradeWording,
          StrategyProjectionEvidenceClaim(
            bandId = s19,
            kind = StrategyProjectionEvidenceKind("trade_invariant_material_simplification_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.White,
        certificationEvidenceFor(
          tradeWording,
          "MaterialHarvest",
          Color.White,
          purposes = Map(
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
            CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
          )
        ),
        Some(tradeWordingDelta)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s19,
        materialOnly,
        evidenceFor(
          materialOnly,
          StrategyProjectionEvidenceClaim(
            bandId = s19,
            kind = StrategyProjectionEvidenceKind("trade_invariant_material_simplification_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.White,
        certificationEvidenceFor(
          materialOnly,
          "MaterialHarvest",
          Color.White,
          purposes = Map(
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
            CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
          )
        ),
        Some(materialOnlyDelta)
      ),
      Right(false)
    )

  test("S23 admits king entry only when route and entry-conversion evidence share the entry square"):
    val fen = "6k1/8/8/3p4/5K2/8/8/8 w - - 0 1"
    val extraction = seedExtraction(fen)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S23"),
        kind = StrategyProjectionEvidenceKind("king_entry_conversion_certified"),
        owner = Color.White,
        anchor = squareAnchor("e5"),
        payload = WitnessPayload(
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("e5"))
        )
      )
    )
    val wrongEntryEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S23"),
        kind = StrategyProjectionEvidenceKind("king_entry_conversion_certified"),
        owner = Color.White,
        anchor = squareAnchor("e5"),
        payload = WitnessPayload(
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("e4"))
        )
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S23"),
        extraction,
        evidence,
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S23"),
        extraction,
        wrongEntryEvidence,
        Color.White
      ),
      Right(false)
    )

  test("S23 admits direct opposition only with an opposition-specific evidence claim"):
    val fen = "8/8/4k3/8/4K3/8/8/8 b - - 0 1"
    val extraction = seedExtraction(fen)
    val noEvidence = StrategyProjectionEvidence.forSeedExtraction(extraction, Vector.empty)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S23"),
        kind = StrategyProjectionEvidenceKind("king_opposition_certified"),
        owner = Color.White,
        anchor = squareAnchor("e5"),
        payload = WitnessPayload(
          "contact_square" -> WitnessValue.SquareValue(squareFromKey("e5"))
        )
      )
    )
    val wrongContactEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S23"),
        kind = StrategyProjectionEvidenceKind("king_opposition_certified"),
        owner = Color.White,
        anchor = squareAnchor("e5"),
        payload = WitnessPayload(
          "contact_square" -> WitnessValue.SquareValue(squareFromKey("e4"))
        )
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S23"),
        extraction,
        noEvidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S23"),
        extraction,
        evidence,
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S23"),
        extraction,
        wrongContactEvidence,
        Color.White
      ),
      Right(false)
    )

  test("S24 admits only when dependency, convergence, forcing, and conversion all share one target"):
    val fen = "6k1/8/4b3/3q4/8/8/6B1/3R2K1 w - - 0 1"
    val extraction = seedExtraction(fen)
    val sameTargetEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S24"),
        kind = StrategyProjectionEvidenceKind("same_target_forcing_realization"),
        owner = Color.White,
        anchor = pieceAnchor("d5")
      ),
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S24"),
        kind = StrategyProjectionEvidenceKind("same_target_conversion_certified"),
        owner = Color.White,
        anchor = pieceAnchor("d5")
      )
    )
    val wrongTargetEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S24"),
        kind = StrategyProjectionEvidenceKind("same_target_forcing_realization"),
        owner = Color.White,
        anchor = pieceAnchor("d5")
      ),
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S24"),
        kind = StrategyProjectionEvidenceKind("same_target_conversion_certified"),
        owner = Color.White,
        anchor = pieceAnchor("e6")
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S24"),
        extraction,
        sameTargetEvidence,
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S24"),
        extraction,
        wrongTargetEvidence,
        Color.White
      ),
      Right(false)
    )

  test("S25 admits only when rank corridor evidence names the same source, entry, and corridor kind"):
    val fen = "6k1/8/8/n7/R7/8/8/6K1 w - - 0 1"
    val extraction = seedExtraction(fen)
    val sameCorridorEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = pieceAnchor("a4"),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token("cross_wing_rank_switch"),
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("f4"))
        )
      )
    )
    val noEvidence = StrategyProjectionEvidence.forSeedExtraction(extraction, Vector.empty)
    val wrongEntryEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = pieceAnchor("a4"),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token("cross_wing_rank_switch"),
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("g4"))
        )
      )
    )
    val wrongKindEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = pieceAnchor("a4"),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token("generic_rook_lift"),
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("f4"))
        )
      )
    )
    val wrongAnchorEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = pieceAnchor("b4"),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token("cross_wing_rank_switch"),
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("f4"))
        )
      )
    )
    val extraPayloadEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = pieceAnchor("a4"),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token("cross_wing_rank_switch"),
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("f4")),
          "certification_family" -> WitnessValue.Token("RankAccessConsequence")
        )
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        sameCorridorEvidence,
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        noEvidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        wrongEntryEvidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        wrongKindEvidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        wrongAnchorEvidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        extraPayloadEvidence,
        Color.White
      ),
      Right(false)
    )

  test("S22 admits fortress only with same-holder shell and certified fortress evidence"):
    val fen = "7k/6pp/8/8/8/4K3/3N4/8 w - - 0 1"
    val extraction = seedExtraction(fen)
    val sameHolderEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S22"),
        kind = StrategyProjectionEvidenceKind("fortress_hold_certified"),
        owner = Color.Black,
        anchor = squareAnchor("h8")
      )
    )
    val noEvidence = StrategyProjectionEvidence.forSeedExtraction(extraction, Vector.empty)
    val wrongAnchorEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S22"),
        kind = StrategyProjectionEvidenceKind("fortress_hold_certified"),
        owner = Color.Black,
        anchor = squareAnchor("g8")
      )
    )
    val certifiedFortress =
      certificationEvidenceFor(extraction, "FortressDrawCertification", Color.Black)
    val supportOnlyFortress =
      certificationEvidenceFor(
        extraction,
        "FortressDrawCertification",
        Color.Black,
        CertificationEvidenceStrength.Insufficient
      )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S22"),
        extraction,
        sameHolderEvidence,
        Color.Black,
        certifiedFortress
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S22"),
        extraction,
        noEvidence,
        Color.Black,
        certifiedFortress
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S22"),
        extraction,
        sameHolderEvidence,
        Color.Black
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S22"),
        extraction,
        wrongAnchorEvidence,
        Color.Black,
        certifiedFortress
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S22"),
        extraction,
        sameHolderEvidence,
        Color.Black,
        supportOnlyFortress
      ),
      Right(false)
    )

  test("S22 admits perpetual only with certified current-board perpetual evidence"):
    val fen = "5r1k/6R1/6K1/8/8/8/8/8 w - - 0 1"
    val extraction = seedExtraction(fen)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S22"),
        kind = StrategyProjectionEvidenceKind("perpetual_hold_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.BoardAnchor
      )
    )
    val noEvidence = StrategyProjectionEvidence.forSeedExtraction(extraction, Vector.empty)
    val certifiedPerpetual =
      certificationEvidenceFor(extraction, "PerpetualCheckHolding", Color.White)
    val deferredPerpetual =
      certificationEvidenceFor(
        extraction,
        "PerpetualCheckHolding",
        Color.White,
        CertificationEvidenceStrength.Insufficient
      )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S22"),
        extraction,
        evidence,
        Color.White,
        certifiedPerpetual
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S22"),
        extraction,
        noEvidence,
        Color.White,
        certifiedPerpetual
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S22"),
        extraction,
        evidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S22"),
        extraction,
        evidence,
        Color.White,
        deferredPerpetual
      ),
      Right(false)
    )

  test("S22 rejects shortcut and adjacent-rival boards even with S22 evidence"):
    val s22 = StrategyProjectionBandId("S22")
    val shortcutChecking = seedExtraction("5rk1/5pbp/8/8/8/8/5Q2/6K1 w - - 0 1")
    val s19Rival = seedExtraction("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1")
    val s23Rival = seedExtraction("6k1/8/8/3p4/5K2/8/8/8 w - - 0 1")

    assertEquals(
      StrategyProjectionAdmission.admits(
        s22,
        shortcutChecking,
        evidenceFor(
          shortcutChecking,
          StrategyProjectionEvidenceClaim(
            bandId = s22,
            kind = StrategyProjectionEvidenceKind("perpetual_hold_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.White,
        certificationEvidenceFor(shortcutChecking, "PerpetualCheckHolding", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s22,
        s19Rival,
        evidenceFor(
          s19Rival,
          StrategyProjectionEvidenceClaim(
            bandId = s22,
            kind = StrategyProjectionEvidenceKind("fortress_hold_certified"),
            owner = Color.White,
            anchor = squareAnchor("e1")
          ),
          StrategyProjectionEvidenceClaim(
            bandId = s22,
            kind = StrategyProjectionEvidenceKind("perpetual_hold_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.White,
        certificationEvidenceFor(s19Rival, "FortressDrawCertification", Color.White)
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        s22,
        s23Rival,
        evidenceFor(
          s23Rival,
          StrategyProjectionEvidenceClaim(
            bandId = s22,
            kind = StrategyProjectionEvidenceKind("fortress_hold_certified"),
            owner = Color.White,
            anchor = squareAnchor("e5")
          ),
          StrategyProjectionEvidenceClaim(
            bandId = s22,
            kind = StrategyProjectionEvidenceKind("perpetual_hold_certified"),
            owner = Color.White,
            anchor = WitnessAnchor.BoardAnchor
          )
        ),
        Color.White,
        certificationEvidenceFor(s23Rival, "PerpetualCheckHolding", Color.White)
      ),
      Right(false)
    )

  test("projection admission rejects stale non-empty evidence bundles"):
    val current = seedExtraction("4k3/8/8/3b4/5N2/8/8/4K3 b - - 0 1")
    val staleSource = seedExtraction("4k3/8/8/3b4/5N2/8/6B1/7K b - - 0 1")
    val staleEvidence = evidenceFor(
      staleSource,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S17"),
        kind = StrategyProjectionEvidenceKind("liability_relief_certified"),
        owner = Color.Black,
        anchor = pieceAnchor("d5"),
        payload = WitnessPayload("relief_kind" -> WitnessValue.Token("repair_route"))
      )
    )

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S17"), current, staleEvidence, Color.Black)
        .left
        .exists(_.contains("stale"))
    )

  test("S11 projection admission rejects stale exact weak-pawn evidence"):
    val current = seedExtraction("4k3/8/8/3p4/3P4/8/6B1/4K3 w - - 0 1")
    val staleSource = seedExtraction("4k3/8/8/3p4/3P1N2/8/6B1/4K3 w - - 0 1")
    val staleEvidence =
      s11Evidence(staleSource, "d5", "same_target_repeated_pressure", Vector("f4", "g2"))

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S11"), current, staleEvidence, Color.White)
        .left
        .exists(_.contains("stale evidence bundle"))
    )

  test("S13 projection admission rejects stale exact wing-damage evidence"):
    val current = seedExtraction("4k3/8/8/8/1pp5/8/P7/4K3 w - - 0 1")
    val staleSource = seedExtraction("4k3/2p5/2p5/1p6/1P6/P7/8/4K3 w - - 0 1")
    val staleEvidence =
      s13Evidence(staleSource, "a3", "b5", "structurally_burdened_target")

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S13"), current, staleEvidence, Color.White)
        .left
        .exists(_.contains("stale evidence bundle"))
    )

  test("S14 projection admission rejects stale exact chain-base evidence"):
    val current = seedExtraction("4k3/8/6p1/5p2/7P/8/8/4K3 w - - 0 1")
    val staleSource = seedExtraction("4k3/8/1p6/2p5/P2p4/8/8/4K3 w - - 0 1")
    val staleEvidence =
      s14Evidence(staleSource, "a4", "b6", "base_contact_continuation", Vector("c5"))

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S14"), current, staleEvidence, Color.White)
        .left
        .exists(_.contains("stale evidence bundle"))
    )

  test("S15 projection admission rejects stale same-candidate passer creation evidence"):
    val current = seedExtraction("4k3/2p5/1pp5/8/PP6/8/8/4K3 w - - 0 1")
    val staleSource = seedExtraction("4k3/8/1p6/2p5/PP6/8/8/4K3 w - - 0 1")
    val staleEvidence =
      s15ChainBaseEvidence(staleSource, "a4", "b6", "chain_base_target", Vector("c5"))

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S15"), current, staleEvidence, Color.White)
        .left
        .exists(_.contains("stale evidence bundle"))
    )

  test("S18 projection admission rejects stale projection and certification evidence"):
    val currentFen = "r1bqkbnr/p1ppp1pp/2n5/8/2P1P3/B1N2N2/PP1P1PPP/R2QKB1R w KQkq - 0 1"
    val staleFen = "8/8/1nQ3n1/1B2p3/3b2K1/8/1k4B1/8 w - - 0 1"
    val current = seedExtraction(currentFen)
    val staleSource = seedExtraction(staleFen)
    val currentEvidence = evidenceFor(
      current,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S18"),
        kind = StrategyProjectionEvidenceKind("bishop_pair_initiative_conversion_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.BoardAnchor
      )
    )
    val staleEvidence = evidenceFor(
      staleSource,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S18"),
        kind = StrategyProjectionEvidenceKind("bishop_pair_structure_conversion_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.BoardAnchor
      )
    )
    val currentCertification =
      certificationEvidenceFor(
        current,
        "InitiativeWindow",
        Color.White,
        purposes = Map(
          CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied,
          CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
        )
      )
    val staleCertification =
      certificationEvidenceFor(
        staleSource,
        "MobilityComparison",
        Color.White,
        purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
      )

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S18"), current, staleEvidence, Color.White, currentCertification)
        .left
        .exists(_.contains("stale evidence bundle"))
    )
    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S18"), current, currentEvidence, Color.White, staleCertification)
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S20 projection admission rejects stale projection and certification evidence"):
    val current = seedExtraction("8/8/1nQ3n1/4p3/3b2K1/8/1k6/8 w - - 0 1")
    val staleSource = seedExtraction("3q2k1/7p/2Q2n2/6B1/8/3B4/8/4K3 w - - 0 1")
    val currentEvidence = s20Evidence(current, "d4", "mobility_plus_restriction")
    val staleEvidence = s20Evidence(staleSource, "f6", "defender_starvation")
    val currentCertification =
      certificationEvidenceFor(
        current,
        "MobilityComparison",
        Color.White,
        purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
      )
    val staleCertification =
      certificationEvidenceFor(
        staleSource,
        "MobilityComparison",
        Color.White,
        purposes = Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
      )

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S20"), current, staleEvidence, Color.White, currentCertification)
        .left
        .exists(_.contains("stale evidence bundle"))
    )
    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S20"), current, currentEvidence, Color.White, staleCertification)
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S10 projection admission rejects stale projection evidence"):
    val current = seedExtraction("4k3/8/8/3N4/8/2P5/8/4K3 w - - 0 1")
    val staleSource = seedExtraction("4k3/2p5/2P5/3N4/8/2P5/8/4K3 w - - 0 1")

    assert(
      StrategyProjectionAdmission
        .admits(
          StrategyProjectionBandId("S10"),
          current,
          s10Evidence(staleSource, "d5", "same_anchor_eviction_denial"),
          Color.White
        )
        .left
        .exists(_.contains("stale evidence bundle"))
    )

  test("S22 projection admission rejects stale certified hold evidence"):
    val current = seedExtraction("7k/6pp/8/8/8/4K3/3N4/8 w - - 0 1")
    val staleSource = seedExtraction("7k/6pp/8/8/8/4K3/8/3N4 w - - 0 1")
    val staleEvidence = evidenceFor(
      staleSource,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S22"),
        kind = StrategyProjectionEvidenceKind("fortress_hold_certified"),
        owner = Color.Black,
        anchor = squareAnchor("h8")
      )
    )

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S22"), current, staleEvidence, Color.Black)
        .left
        .exists(_.contains("stale"))
    )

    val currentEvidence = evidenceFor(
      current,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S22"),
        kind = StrategyProjectionEvidenceKind("fortress_hold_certified"),
        owner = Color.Black,
        anchor = squareAnchor("h8")
      )
    )
    val staleCertificationEvidence =
      certificationEvidenceFor(staleSource, "FortressDrawCertification", Color.Black)

    assert(
      StrategyProjectionAdmission
        .admits(
          StrategyProjectionBandId("S22"),
          current,
          currentEvidence,
          Color.Black,
          staleCertificationEvidence
        )
        .left
        .exists(_.contains("stale certification evidence"))
    )

  test("S19 projection admission rejects stale projection, certification, and delta evidence"):
    val currentFen = "4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"
    val afterFen = "4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1"
    val staleFen = "4k3/2n5/3P4/8/8/8/4K3/8 w - - 0 1"
    val current = seedExtraction(currentFen)
    val staleSource = seedExtraction(staleFen)
    val currentDelta = deltaExtraction(currentFen, "d6c7", afterFen)
    val staleDelta = deltaExtraction(staleFen, "d6c7", "4k3/2P5/8/8/8/8/4K3/8 b - - 0 1")
    val currentEvidence = evidenceFor(
      current,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S19"),
        kind = StrategyProjectionEvidenceKind("trade_invariant_material_simplification_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.BoardAnchor
      )
    )
    val staleEvidence = evidenceFor(
      staleSource,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S19"),
        kind = StrategyProjectionEvidenceKind("trade_invariant_material_simplification_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.BoardAnchor
      )
    )
    val currentCertification =
      certificationEvidenceFor(
        current,
        "MaterialHarvest",
        Color.White,
        purposes = Map(
          CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
          CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
        )
      )
    val staleCertification =
      certificationEvidenceFor(
        staleSource,
        "MaterialHarvest",
        Color.White,
        purposes = Map(
          CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
          CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
        )
      )

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S19"), current, staleEvidence, Color.White, currentCertification, Some(currentDelta))
        .left
        .exists(_.contains("stale evidence bundle"))
    )
    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S19"), current, currentEvidence, Color.White, staleCertification, Some(currentDelta))
        .left
        .exists(_.contains("stale certification evidence"))
    )
    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S19"), current, currentEvidence, Color.White, currentCertification, Some(staleDelta))
        .left
        .exists(_.contains("stale strategic delta evidence"))
    )

  private def seedExtraction(fen: String) =
    StrategySupportSeedExtractor
      .fromFen(Fen.Full.clean(fen))
      .fold(message => fail(message), identity)

  private def deltaExtraction(before: String, move: String, after: String): StrategicDeltaExtraction =
    StrategicDeltaExtractor
      .fromFens(
        Fen.Full.clean(before),
        Uci(move).collect { case move: Uci.Move => move }.getOrElse(fail(s"bad move $move")),
        Fen.Full.clean(after)
      )
      .fold(message => fail(message), identity)

  private def evidenceFor(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      claims: StrategyProjectionEvidenceClaim*
  ): StrategyProjectionEvidence =
    StrategyProjectionEvidence.forSeedExtraction(extraction, claims)

  private def s18EvidencePayload(
      certificationFamily: String,
      bishopMemberSquares: Vector[String],
      activeBishopSquare: String,
      conversionTargetSquares: Vector[String]
  ): WitnessPayload =
    WitnessPayload(
      "certification_family" -> WitnessValue.Token(certificationFamily),
      "bishop_member_squares" -> WitnessValue.SquareListValue(bishopMemberSquares.map(squareFromKey)),
      "active_bishop_square" -> WitnessValue.SquareValue(squareFromKey(activeBishopSquare)),
      "conversion_target_squares" -> WitnessValue.SquareListValue(conversionTargetSquares.map(squareFromKey))
    )

  private def s11Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      targetSquare: String,
      pressureRoute: String,
      pressureSourceSquares: Vector[String]
  ): StrategyProjectionEvidence =
    val target = squareFromKey(targetSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S11"),
        kind = StrategyProjectionEvidenceKind("weak_pawn_target_pressure_persistence_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.SquareAnchor(target),
        payload = WitnessPayload(
          "target_square" -> WitnessValue.SquareValue(target),
          "pressure_route" -> WitnessValue.Token(pressureRoute),
          "persistence_kind" -> WitnessValue.Token("fixed"),
          "pressure_source_squares" -> WitnessValue.SquareListValue(pressureSourceSquares.map(squareFromKey))
        )
      )
    )

  private def s05Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      contactSourceSquare: String,
      targetSquare: String,
      centerReleaseRoute: String
  ): StrategyProjectionEvidence =
    val source = squareFromKey(contactSourceSquare)
    val target = squareFromKey(targetSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S05"),
        kind = StrategyProjectionEvidenceKind("center_release_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.SquareAnchor(target),
        payload = WitnessPayload(
          "contact_source_square" -> WitnessValue.SquareValue(source),
          "target_square" -> WitnessValue.SquareValue(target),
          "center_release_route" -> WitnessValue.Token(centerReleaseRoute)
        )
      )
    )

  private def s06Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      routeAnchorSquare: String,
      spaceBindRoute: String,
      structuralSector: String,
      structuralHostId: String,
      outpostSquare: Option[String],
      restrictionAnchorSquare: Option[String],
      certificationFamily: String = "SpaceBindRestrictionCertification"
  ): StrategyProjectionEvidence =
    val routeAnchor = squareFromKey(routeAnchorSquare)
    val payloadEntries =
      Vector(
        "route_anchor_square" -> WitnessValue.SquareValue(routeAnchor),
        "space_bind_route" -> WitnessValue.Token(spaceBindRoute),
        "structural_sector" -> WitnessValue.Token(structuralSector),
        "structural_host_id" -> WitnessValue.Token(structuralHostId),
        "certification_family" -> WitnessValue.Token(certificationFamily)
      ) ++ outpostSquare.toVector.map(square =>
        "outpost_square" -> WitnessValue.SquareValue(squareFromKey(square))
      ) ++ restrictionAnchorSquare.toVector.map(square =>
        "restriction_anchor_square" -> WitnessValue.SquareValue(squareFromKey(square))
      )
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S06"),
        kind = StrategyProjectionEvidenceKind("space_bind_restriction_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.PieceSquareAnchor(routeAnchor),
        payload = WitnessPayload.from(payloadEntries)
      )
    )

  private def s07Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      initiativeConversionRoute: String,
      certificationFamily: String = "InitiativeWindow"
  ): StrategyProjectionEvidence =
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S07"),
        kind = StrategyProjectionEvidenceKind("initiative_conversion_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.BoardAnchor,
        payload = WitnessPayload(
          "initiative_conversion_route" -> WitnessValue.Token(initiativeConversionRoute),
          "certification_family" -> WitnessValue.Token(certificationFamily)
        )
      )
    )

  private def s08Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      contactSourceSquare: String,
      targetSquare: String,
      counterplayDenialRoute: String,
      certificationFamily: String = "InitiativeWindow"
  ): StrategyProjectionEvidence =
    val source = squareFromKey(contactSourceSquare)
    val target = squareFromKey(targetSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S08"),
        kind = StrategyProjectionEvidenceKind("counterplay_denial_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.PieceSquareAnchor(source),
        payload = WitnessPayload(
          "contact_source_square" -> WitnessValue.SquareValue(source),
          "target_square" -> WitnessValue.SquareValue(target),
          "counterplay_denial_route" -> WitnessValue.Token(counterplayDenialRoute),
          "certification_family" -> WitnessValue.Token(certificationFamily)
        )
      )
    )

  private def s01Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      contactSourceSquare: String,
      targetSquare: String,
      defendingKingSquare: String,
      kingWingStormRoute: String,
      owner: Color = Color.White
  ): StrategyProjectionEvidence =
    val source = squareFromKey(contactSourceSquare)
    val target = squareFromKey(targetSquare)
    val defendingKing = squareFromKey(defendingKingSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S01"),
        kind = StrategyProjectionEvidenceKind("king_wing_storm_route_certified"),
        owner = owner,
        anchor = WitnessAnchor.PieceSquareAnchor(source),
        payload = WitnessPayload(
          "contact_source_square" -> WitnessValue.SquareValue(source),
          "target_square" -> WitnessValue.SquareValue(target),
          "defending_king_square" -> WitnessValue.SquareValue(defendingKing),
          "king_wing_storm_route" -> WitnessValue.Token(kingWingStormRoute),
          "certification_family" -> WitnessValue.Token("CertifiedKingSafetyEdge")
        )
      )
    )

  private def s02Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      defendingKingSquare: String,
      sourceSquares: Vector[String],
      kingRingTargetSquares: Vector[String],
      kingRingConcentrationRoute: String,
      owner: Color = Color.White
  ): StrategyProjectionEvidence =
    val defendingKing = squareFromKey(defendingKingSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionScopeContract.S02,
        kind = StrategyProjectionScopeContract.KingRingConcentrationRouteCertified,
        owner = owner,
        anchor = WitnessAnchor.SquareAnchor(defendingKing),
        payload = WitnessPayload(
          "defending_king_square" -> WitnessValue.SquareValue(defendingKing),
          "source_squares" -> WitnessValue.SquareListValue(sourceSquares.map(squareFromKey)),
          "king_ring_target_squares" -> WitnessValue.SquareListValue(kingRingTargetSquares.map(squareFromKey)),
          "king_ring_concentration_route" -> WitnessValue.Token(kingRingConcentrationRoute),
          "certification_family" -> WitnessValue.Token("CertifiedKingSafetyEdge")
        )
      )
    )

  private def s03Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      defendingKingSquare: String,
      diagonalSourceSquare: String,
      diagonalEndpointSquares: Vector[String],
      diagonalKingAttackRoute: String,
      owner: Color = Color.White
  ): StrategyProjectionEvidence =
    val defendingKing = squareFromKey(defendingKingSquare)
    val diagonalSource = squareFromKey(diagonalSourceSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S03"),
        kind = StrategyProjectionEvidenceKind("diagonal_king_attack_route_certified"),
        owner = owner,
        anchor = WitnessAnchor.SquareAnchor(defendingKing),
        payload = WitnessPayload(
          "defending_king_square" -> WitnessValue.SquareValue(defendingKing),
          "diagonal_source_square" -> WitnessValue.SquareValue(diagonalSource),
          "diagonal_endpoint_squares" -> WitnessValue.SquareListValue(diagonalEndpointSquares.map(squareFromKey)),
          "diagonal_king_attack_route" -> WitnessValue.Token(diagonalKingAttackRoute),
          "certification_family" -> WitnessValue.Token("CertifiedKingSafetyEdge")
        )
      )
    )

  private def s04Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      defendingKingSquare: String,
      shellAnchorSquare: String,
      breachSquares: Vector[String],
      kingShelterBreachRoute: String,
      owner: Color = Color.White
  ): StrategyProjectionEvidence =
    val defendingKing = squareFromKey(defendingKingSquare)
    val shellAnchor = squareFromKey(shellAnchorSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionScopeContract.S04,
        kind = StrategyProjectionScopeContract.KingShelterBreachRouteCertified,
        owner = owner,
        anchor = WitnessAnchor.SquareAnchor(defendingKing),
        payload = WitnessPayload(
          "defending_king_square" -> WitnessValue.SquareValue(defendingKing),
          "shell_anchor_square" -> WitnessValue.SquareValue(shellAnchor),
          "breach_squares" -> WitnessValue.SquareListValue(breachSquares.map(squareFromKey)),
          "king_shelter_breach_route" -> WitnessValue.Token(kingShelterBreachRoute),
          "certification_family" -> WitnessValue.Token("CertifiedKingSafetyEdge")
        )
      )
    )

  private def invokeS02KingRingConcentrationCarriers(
      current: StrategicObjectExtraction,
      certification: Certification,
      owner: Color
  ): Vector[Product] =
    val method =
      StrategyProjectionAdmission.getClass.getDeclaredMethods
        .find(method =>
          method.getName.contains("s02KingRingConcentrationCarriers") &&
            method.getParameterTypes.toVector == Vector(
              classOf[StrategicObjectExtraction],
              classOf[Certification],
              classOf[Color]
            )
        )
        .getOrElse(fail("missing S02 carrier builder"))
    method.setAccessible(true)
    method.invoke(StrategyProjectionAdmission, current, certification, owner).asInstanceOf[Vector[Product]]

  private def s09Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      sourceSquare: String,
      entrySquare: String,
      filePenetrationRoute: String,
      extraPayload: Vector[(String, WitnessValue)] = Vector.empty,
      owner: Color = Color.White
  ): StrategyProjectionEvidence =
    val source = squareFromKey(sourceSquare)
    val entry = squareFromKey(entrySquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionScopeContract.S09,
        kind = StrategyProjectionScopeContract.FilePenetrationRouteCertified,
        owner = owner,
        anchor = WitnessAnchor.PieceSquareAnchor(source),
        payload = WitnessPayload.from(
          Vector(
            "route_anchor_square" -> WitnessValue.SquareValue(source),
            "entry_square" -> WitnessValue.SquareValue(entry),
            "file_penetration_route" -> WitnessValue.Token(filePenetrationRoute)
          ) ++ extraPayload
        )
      )
    )

  private def s25Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      sourceSquare: String,
      entrySquare: String,
      corridorKind: String
  ): StrategyProjectionEvidence =
    val source = squareFromKey(sourceSquare)
    val entry = squareFromKey(entrySquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.PieceSquareAnchor(source),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token(corridorKind),
          "entry_square" -> WitnessValue.SquareValue(entry)
        )
      )
    )

  private def s21Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      contactSourceSquare: String,
      targetSquare: String,
      counterplaySurvivalRoute: String,
      certificationFamily: String = "InitiativeWindow"
  ): StrategyProjectionEvidence =
    val source = squareFromKey(contactSourceSquare)
    val target = squareFromKey(targetSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S21"),
        kind = StrategyProjectionEvidenceKind("counterplay_survival_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.PieceSquareAnchor(source),
        payload = WitnessPayload(
          "contact_source_square" -> WitnessValue.SquareValue(source),
          "target_square" -> WitnessValue.SquareValue(target),
          "counterplay_survival_route" -> WitnessValue.Token(counterplaySurvivalRoute),
          "certification_family" -> WitnessValue.Token(certificationFamily)
        )
      )
    )

  private def s20Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      routeAnchorSquare: String,
      dominationRoute: String,
      certificationFamily: String = "MobilityComparison",
      supportWitnessId: String = ""
  ): StrategyProjectionEvidence =
    val routeAnchor = squareFromKey(routeAnchorSquare)
    val witnessId =
      if supportWitnessId.nonEmpty then supportWitnessId
      else if dominationRoute == "defender_starvation" then "duty_bound_defender"
      else "short_run_slider_gate_restriction"
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S20"),
        kind = StrategyProjectionEvidenceKind("mobility_domination_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.PieceSquareAnchor(routeAnchor),
        payload = WitnessPayload(
          "route_anchor_square" -> WitnessValue.SquareValue(routeAnchor),
          "domination_route" -> WitnessValue.Token(dominationRoute),
          "support_witness_id" -> WitnessValue.Token(witnessId),
          "support_target_squares" -> WitnessValue.SquareListValue(
            s20SupportTargetSquares(extraction, witnessId, routeAnchor)
          ),
          "certification_family" -> WitnessValue.Token(certificationFamily)
        )
      )
    )

  private def s20SupportTargetSquares(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      supportWitnessId: String,
      routeAnchor: Square
  ): Vector[Square] =
    Try(StrategicObjectExtractor.fromRoot(extraction.rootState)).toOption.toVector
      .flatMap(_.primaryWitnesses.all)
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId(supportWitnessId) &&
          witness.anchor == WitnessAnchor.PieceSquareAnchor(routeAnchor)
      )
      .flatMap(_.support.targetSquares)
      .distinct
      .sortBy(_.value)

  private def s10Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      routeAnchorSquare: String,
      outpostOccupationRoute: String
  ): StrategyProjectionEvidence =
    val routeAnchor = squareFromKey(routeAnchorSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S10"),
        kind = StrategyProjectionEvidenceKind("outpost_occupation_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.PieceSquareAnchor(routeAnchor),
        payload = WitnessPayload(
          "route_anchor_square" -> WitnessValue.SquareValue(routeAnchor),
          "outpost_square" -> WitnessValue.SquareValue(routeAnchor),
          "outpost_occupation_route" -> WitnessValue.Token(outpostOccupationRoute)
        )
      )
    )

  private def s12Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      routeAnchorSquare: String,
      accessRoute: String,
      accessWitnessId: String,
      restrictionAnchorSquare: String,
      weakOutpostState: String = "outpost",
      certificationFamily: Option[String] = None,
      extraPayloadEntries: Vector[(String, WitnessValue)] = Vector.empty
  ): StrategyProjectionEvidence =
    val routeAnchor = squareFromKey(routeAnchorSquare)
    val restrictionAnchor = squareFromKey(restrictionAnchorSquare)
    val payloadEntries =
      Vector(
        "route_anchor_square" -> WitnessValue.SquareValue(routeAnchor),
        "access_route" -> WitnessValue.Token(accessRoute),
        "access_witness_id" -> WitnessValue.Token(accessWitnessId),
        "support_witness_id" -> WitnessValue.Token("short_run_slider_gate_restriction"),
        "support_target_squares" -> WitnessValue.SquareListValue(
          s12RestrictionSupportTargetSquares(extraction, restrictionAnchor)
        ),
        "restriction_anchor_square" -> WitnessValue.SquareValue(restrictionAnchor),
        "local_access_superiority" -> WitnessValue.Token("route_with_restriction")
      ) ++
        (if accessRoute == "weak_square_route" then
           Vector(
             "weak_outpost_square" -> WitnessValue.SquareValue(routeAnchor),
             "weak_outpost_state" -> WitnessValue.Token(weakOutpostState)
           )
         else
           Vector(
             "diagonal_source_square" -> WitnessValue.SquareValue(routeAnchor),
             "diagonal_endpoint_squares" -> WitnessValue.SquareListValue(s12DiagonalEndpointSquares(extraction, routeAnchor))
           )
        ) ++
        certificationFamily.toVector.map(family => "certification_family" -> WitnessValue.Token(family)) ++
        extraPayloadEntries
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S12"),
        kind = StrategyProjectionEvidenceKind("local_access_superiority_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.SquareAnchor(routeAnchor),
        payload = WitnessPayload.from(payloadEntries)
      )
    )

  private def s12RestrictionSupportTargetSquares(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      restrictionAnchor: Square
  ): Vector[Square] =
    Try(StrategicObjectExtractor.fromRoot(extraction.rootState)).toOption.toVector
      .flatMap(_.primaryWitnesses.all)
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("short_run_slider_gate_restriction") &&
          witness.anchor == WitnessAnchor.PieceSquareAnchor(restrictionAnchor)
      )
      .flatMap(_.support.targetSquares)
      .distinct
      .sortBy(_.value)

  private def s12DiagonalEndpointSquares(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      routeAnchor: Square
  ): Vector[Square] =
    Try(StrategicObjectExtractor.fromRoot(extraction.rootState)).toOption.toVector
      .flatMap(current => current.primaryWitnesses.all ++ current.attachedWitnesses.all)
      .filter(_.descriptorId == WitnessDescriptorId("diagonal_lane_only"))
      .filter(witness =>
        witness.payload
          .get("source_piece_squares")
          .collect { case WitnessValue.SquareListValue(squares) => squares.contains(routeAnchor) }
          .contains(true)
      )
      .flatMap(_.payload.get("endpoint_squares").collect { case WitnessValue.SquareListValue(squares) => squares }.toVector.flatten)
      .distinct
      .sortBy(_.value)

  private def s13Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      contactSourceSquare: String,
      targetSquare: String,
      damageRoute: String,
      damageSector: String = "queenside"
  ): StrategyProjectionEvidence =
    val source = squareFromKey(contactSourceSquare)
    val target = squareFromKey(targetSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S13"),
        kind = StrategyProjectionEvidenceKind("wing_damage_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.SquareAnchor(target),
        payload = WitnessPayload(
          "contact_source_square" -> WitnessValue.SquareValue(source),
          "target_square" -> WitnessValue.SquareValue(target),
          "damage_route" -> WitnessValue.Token(damageRoute),
          "damage_sector" -> WitnessValue.Token(damageSector)
        )
      )
    )

  private def s14Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      contactSourceSquare: String,
      targetSquare: String,
      chainBaseRoute: String,
      chainBaseForwardSquares: Vector[String]
  ): StrategyProjectionEvidence =
    val source = squareFromKey(contactSourceSquare)
    val target = squareFromKey(targetSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S14"),
        kind = StrategyProjectionEvidenceKind("chain_base_contact_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.SquareAnchor(target),
        payload = WitnessPayload(
          "contact_source_square" -> WitnessValue.SquareValue(source),
          "target_square" -> WitnessValue.SquareValue(target),
          "chain_base_route" -> WitnessValue.Token(chainBaseRoute),
          "chain_base_forward_squares" -> WitnessValue.SquareListValue(chainBaseForwardSquares.map(squareFromKey))
        )
      )
    )

  private def s15WingDamageEvidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      contactSourceSquare: String,
      targetSquare: String,
      damageRoute: String,
      damageSector: String = "queenside"
  ): StrategyProjectionEvidence =
    val source = squareFromKey(contactSourceSquare)
    val target = squareFromKey(targetSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S15"),
        kind = StrategyProjectionEvidenceKind("passer_creation_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.PieceSquareAnchor(source),
        payload = WitnessPayload(
          "creation_route" -> WitnessValue.Token("s13_wing_damage"),
          "contact_source_square" -> WitnessValue.SquareValue(source),
          "target_square" -> WitnessValue.SquareValue(target),
          "damage_route" -> WitnessValue.Token(damageRoute),
          "damage_sector" -> WitnessValue.Token(damageSector)
        )
      )
    )

  private def s15ChainBaseEvidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      contactSourceSquare: String,
      targetSquare: String,
      chainBaseRoute: String,
      chainBaseForwardSquares: Vector[String]
  ): StrategyProjectionEvidence =
    val source = squareFromKey(contactSourceSquare)
    val target = squareFromKey(targetSquare)
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S15"),
        kind = StrategyProjectionEvidenceKind("passer_creation_route_certified"),
        owner = Color.White,
        anchor = WitnessAnchor.PieceSquareAnchor(source),
        payload = WitnessPayload(
          "creation_route" -> WitnessValue.Token("s14_chain_base"),
          "contact_source_square" -> WitnessValue.SquareValue(source),
          "target_square" -> WitnessValue.SquareValue(target),
          "chain_base_route" -> WitnessValue.Token(chainBaseRoute),
          "chain_base_forward_squares" -> WitnessValue.SquareListValue(chainBaseForwardSquares.map(squareFromKey))
        )
      )
    )

  private def s16Evidence(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      owner: Color,
      suppressionRoute: String,
      passerSquare: String,
      blockerSquare: Option[String],
      certificationFamily: String
  ): StrategyProjectionEvidence =
    val passer = squareFromKey(passerSquare)
    val payloadEntries =
      Vector(
        "suppression_route" -> WitnessValue.Token(suppressionRoute),
        "passer_square" -> WitnessValue.SquareValue(passer),
        "certification_family" -> WitnessValue.Token(certificationFamily)
      ) ++ blockerSquare.toVector.map(square =>
        "blocker_square" -> WitnessValue.SquareValue(squareFromKey(square))
      )
    evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S16"),
        kind = StrategyProjectionEvidenceKind("passer_suppression_route_certified"),
        owner = owner,
        anchor = WitnessAnchor.PieceSquareAnchor(passer),
        payload = WitnessPayload.from(payloadEntries)
      )
    )

  private def initiativeWindowEvidenceFor(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      owner: Color
  ): CertificationEvidenceBundle =
    certificationEvidenceFor(
      extraction,
      "InitiativeWindow",
      owner,
      purposes = Map(
        CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
      )
    )

  private def developmentInitiativeEvidenceFor(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      owner: Color
  ): CertificationEvidenceBundle =
    val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
    CertificationEvidenceBundle.forObjectExtraction(
      current,
      Vector(
        CertificationEvidence(
          familyId = CertificationId("DevelopmentComparison"),
          color = owner,
          purposeStrengths =
            Map(CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied)
        ),
        CertificationEvidence(
          familyId = CertificationId("InitiativeWindow"),
          color = owner,
          purposeStrengths = Map(
            CertificationEvidencePurpose.CounterplayDenial -> CertificationEvidenceStrength.Satisfied,
            CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
          )
        )
      )
    )

  private def kingSafetyEdgeEvidenceFor(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      owner: Color
  ): CertificationEvidenceBundle =
    val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
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

  private def spaceBindCertificationEvidenceFor(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      owner: Color
  ): CertificationEvidenceBundle =
    certificationEvidenceFor(
      extraction,
      "SpaceBindRestrictionCertification",
      owner,
      purposes = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
      )
    )

  private def certificationEvidenceFor(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      familyId: String,
      owner: Color,
      strength: CertificationEvidenceStrength = CertificationEvidenceStrength.Satisfied,
      purposes: Map[CertificationEvidencePurpose, CertificationEvidenceStrength] =
        Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied)
  ): CertificationEvidenceBundle =
    val current = StrategicObjectExtractor.fromRoot(extraction.rootState)
    val resolvedPurposes =
      if familyId == "PromotionRace" &&
        purposes == Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied)
      then
        Map(
          CertificationEvidencePurpose.BestDefenseSurvival -> strength,
          CertificationEvidencePurpose.ConversionRouteSurvival -> strength
        )
      else if purposes == Map(CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied)
      then Map(CertificationEvidencePurpose.BestDefenseSurvival -> strength)
      else purposes
    CertificationEvidenceBundle.forObjectExtraction(
      current,
      Vector(
        CertificationEvidence(
          familyId = CertificationId(familyId),
          color = owner,
          purposeStrengths = resolvedPurposes
        )
      )
    )

  private def squareAnchor(square: String): WitnessAnchor =
    WitnessAnchor.SquareAnchor(squareFromKey(square))

  private def pieceAnchor(square: String): WitnessAnchor =
    WitnessAnchor.PieceSquareAnchor(squareFromKey(square))

  private def squareFromKey(square: String): Square =
    Square.fromKey(square).getOrElse(fail(s"bad square $square"))
