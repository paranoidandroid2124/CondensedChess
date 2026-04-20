package lila.commentary.validation

import chess.Color

import lila.commentary.certification.*
import lila.commentary.validation.CertificationRuntimeTestSupport.*
import lila.commentary.witness.{ WitnessAnchor, WitnessValue }

class CertificationComparativeRuntimeTest extends munit.FunSuite:

  private val rows = rowsForFamilies(Set("DevelopmentComparison", "MobilityComparison"))

  test("certification scope keeps the frozen comparative families in runtime order"):
    val comparativeFamilies =
      CertificationScopeContract.activeCertificationFamilyIds
        .map(_.value)
        .filter(family =>
          CertificationExpectationCorpus.requiredScopesByFamily
            .getOrElse(family, Set.empty)
            .contains("comparative")
        )

    assertEquals(
      comparativeFamilies,
      Vector(
        "DevelopmentComparison",
        "InitiativeWindow",
        "MobilityComparison",
        "ComparativeKingFragility",
        "CertifiedKingSafetyEdge"
      )
    )

  rows.foreach: row =>
    test(s"${row.family} row ${row.id} preserves the frozen verdict and metadata"):
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
        assertEquals(claim.support.supportingTags, row.validatedHelpers.sorted)
        assertEquals(claim.payload.get("owner"), Some(WitnessValue.ColorValue(ownerColor(row))))
        row.family match
          case "DevelopmentComparison" =>
            assert(claim.payload.get("owner_development_count").nonEmpty)
            assert(claim.payload.get("rival_development_count").nonEmpty)
            assert(claim.payload.get("development_gap").nonEmpty)
          case "MobilityComparison" =>
            assert(claim.payload.get("owner_legal_moves").nonEmpty)
            assert(claim.payload.get("rival_legal_moves").nonEmpty)
            assert(claim.payload.get("mobility_gap").nonEmpty)
          case other =>
            fail(s"Unexpected comparative family $other")

  test("DevelopmentComparison rejects heuristic opening shapes without OpeningDevelopmentRegime support"):
    val fen = "r3kbnr/p1p2ppp/8/1p1pp3/1P1PP1b1/3B1N2/P1P2PPP/RNB1K2R w - - 0 1"
    val current = objectExtractionFromFen(fen)
    assertEquals(current.objects.forFamilyId("OpeningDevelopmentRegime"), Vector.empty)

    val extraction =
      extractionFromFen(
        fen,
        evidenceBundleForFamily(
          current = current,
          familyId = "DevelopmentComparison",
          color = Color.White,
          purposeStrengths = Map(
            CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied
          )
        )
      )

    val claim = ownerClaimForFamily(extraction, "DevelopmentComparison", Color.White)
    assertEquals(claim.verdict, CertificationVerdict.Rejected)
    assertEquals(
      claim.payload.get("missing_support_families"),
      Some(WitnessValue.TokenListValue(Vector("OpeningDevelopmentRegime")))
    )

  test("MobilityComparison rejects activity-only boards without restriction support"):
    val fen = "r6k/6pp/8/8/4K3/8/8/1R6 w - - 0 1"
    val current = objectExtractionFromFen(fen)
    val extraction =
      extractionFromFen(
        fen,
        evidenceBundleForFamily(
          current = current,
          familyId = "MobilityComparison",
          color = Color.White,
          purposeStrengths = Map(
            CertificationEvidencePurpose.ComparativeSuperiority -> CertificationEvidenceStrength.Satisfied
          )
        )
      )

    val claim = ownerClaimForFamily(extraction, "MobilityComparison", Color.White)
    assertEquals(claim.verdict, CertificationVerdict.Rejected)
