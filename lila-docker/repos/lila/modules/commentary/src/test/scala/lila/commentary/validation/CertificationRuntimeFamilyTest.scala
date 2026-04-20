package lila.commentary.validation

import chess.Color

import lila.commentary.certification.*
import lila.commentary.validation.CertificationRuntimeTestSupport.*
import lila.commentary.witness.{ WitnessAnchor, WitnessValue }

class CertificationRuntimeFamilyTest extends munit.FunSuite:

  private val rows = rowsForFamilies(
    Set(
      "InitiativeWindow",
      "ComparativeKingFragility",
      "CertifiedKingSafetyEdge",
      "MateNetCertification"
    )
  )

  rows.foreach: row =>
    test(s"certification runtime matches ${row.id}"):
      val extraction = extractionFor(row)
      val claims = familyClaims(extraction, row)
      val claim = ownerClaim(extraction, row)

      assertEquals(claims.map(_.anchor).distinct, Vector(WitnessAnchor.BoardAnchor))
      assertEquals(claims.size, 2, clues(s"${row.id} emitted ${claims.size} board certifications"))
      assertEquals(claim.verdict, expectedVerdict(row))
      assertEquals(claim.anchor, WitnessAnchor.BoardAnchor)
      assertEquals(claim.scope.key, row.scope)
      assertEquals(claim.burdenTag.value, row.burdenTag)
      assertEquals(claim.familyId.value, row.family)
      assertEquals(
        claim.payload.get("helper_tags"),
        Some(WitnessValue.TokenListValue(row.validatedHelpers))
      )
      assertEquals(
        claim.payload.get("evidence_purposes"),
        Some(WitnessValue.TokenListValue(row.validatedEnginePurposes))
      )
      assertEquals(
        claim.payload.get("supporting_families"),
        Option.when(row.validatedRequiredSupportFamilies.nonEmpty)(
          WitnessValue.TokenListValue(row.validatedRequiredSupportFamilies)
        )
      )
      assertEquals(rivalClaim(extraction, row).verdict, CertificationVerdict.Rejected)

      if claim.verdict != CertificationVerdict.Rejected then
        assertEquals(claim.payload.get("owner"), Some(WitnessValue.ColorValue(ownerColor(row))))
        assertEquals(claim.support.supportingTags, row.validatedHelpers.sorted)
        row.family match
          case "InitiativeWindow" =>
            assert(claim.payload.get("owner_advanced_pawns").nonEmpty)
            assert(claim.payload.get("owner_legal_moves").nonEmpty)
            assert(claim.payload.get("rival_legal_moves").nonEmpty)
            assert(claim.payload.get("rival_counterplay_source_count").nonEmpty)
            assert(claim.payload.get("development_gap").nonEmpty)
          case "ComparativeKingFragility" =>
            assert(claim.payload.get("defender_holes").nonEmpty)
            assert(claim.payload.get("owner_holes").nonEmpty)
            assert(claim.payload.get("defender_fragility_score").nonEmpty)
            assert(claim.payload.get("owner_fragility_score").nonEmpty)
          case "CertifiedKingSafetyEdge" =>
            assert(claim.payload.get("attacked_king_ring_squares").nonEmpty)
            assert(claim.payload.get("attacked_shelter_squares").nonEmpty)
            assert(claim.payload.get("major_piece_count").nonEmpty)
            assert(claim.payload.get("attack_strength").nonEmpty)
          case "MateNetCertification" =>
            assert(claim.payload.get("major_piece_count").nonEmpty)
            assert(claim.payload.get("attacked_king_ring_squares").nonEmpty)
          case other =>
            fail(s"Unexpected family $other")

  test("ComparativeKingFragility rejects central home-rank kings outside the frozen home-wing slice"):
    val fen = "4k3/8/6p1/4B3/8/8/7R/6K1 w - - 0 1"
    val current = objectExtractionFromFen(fen)
    val evidence =
      evidenceBundleForFamily(
        current = current,
        familyId = "ComparativeKingFragility",
        color = Color.White,
        purposeStrengths = Map(
          CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied
        )
      )
    val extraction = extractionFromFen(fen, evidence)

    assertEquals(
      ownerClaimForFamily(extraction, "ComparativeKingFragility", Color.White).verdict,
      CertificationVerdict.Rejected
    )

  test("CertifiedKingSafetyEdge stays rejected when fragility support comes from a central king only"):
    val fen = "4k3/8/6p1/4B3/8/8/7R/6K1 w - - 0 1"
    val current = objectExtractionFromFen(fen)
    val evidence =
      CertificationEvidenceBundle.forObjectExtraction(
        current,
        Vector(
          CertificationEvidence(
            familyId = CertificationId("ComparativeKingFragility"),
            color = Color.White,
            purposeStrengths = Map(
              CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied
            )
          ),
          CertificationEvidence(
            familyId = CertificationId("CertifiedKingSafetyEdge"),
            color = Color.White,
            purposeStrengths = Map(
              CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied,
              CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
            )
          )
        )
      )
    val extraction = extractionFromFen(fen, evidence)

    assertEquals(
      ownerClaimForFamily(extraction, "CertifiedKingSafetyEdge", Color.White).verdict,
      CertificationVerdict.Rejected
    )
