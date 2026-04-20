package lila.commentary.certification

import chess.Color

import lila.commentary.validation.CertificationRuntimeTestSupport.*
import lila.commentary.witness.{ WitnessAnchor, WitnessValue }

class ResultDrawRaceCertificationRuleTest extends munit.FunSuite:

  test("material harvest keeps only realizable non-king captures live"):
    assertFamilyVerdict("MaterialHarvest", "cert-material-harvest-exact", CertificationVerdict.Certified)
    assertFenVerdict(
      family = "MaterialHarvest",
      fen = "4k3/5ppp/8/3n4/3R4/8/5PPP/4K3 w - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Certified
    )
    assertFamilyVerdict("MaterialHarvest", "cert-material-harvest-near-miss", CertificationVerdict.Rejected)
    assertFamilyVerdict("MaterialHarvest", "cert-material-harvest-nasty-negative", CertificationVerdict.Rejected)
    assertFamilyVerdict(
      "MaterialHarvest",
      "cert-material-harvest-best-defense",
      CertificationVerdict.SupportOnly
    )
    assertFenVerdict(
      family = "MaterialHarvest",
      fen = "4k3/5ppp/8/3n4/3R4/8/5PPP/4K3 w - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Insufficient,
        CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.SupportOnly
    )

  test("winning endgame rows map to the frozen verdict lattice"):
    assertFamilyVerdict("WinningEndgame", "cert-winning-endgame-exact", CertificationVerdict.Certified)
    assertFamilyVerdict("WinningEndgame", "cert-winning-endgame-near-miss", CertificationVerdict.Rejected)
    assertFamilyVerdict("WinningEndgame", "cert-winning-endgame-nasty-negative", CertificationVerdict.Rejected)
    assertFamilyVerdict("WinningEndgame", "cert-winning-endgame-best-defense", CertificationVerdict.Deferred)

  test("fortress draw certification rows map to the frozen verdict lattice"):
    assertFamilyVerdict(
      "FortressDrawCertification",
      "cert-fortress-draw-certification-exact",
      CertificationVerdict.Certified
    )
    assertFamilyVerdict(
      "FortressDrawCertification",
      "cert-fortress-draw-certification-near-miss",
      CertificationVerdict.Rejected
    )
    assertFamilyVerdict(
      "FortressDrawCertification",
      "cert-fortress-draw-certification-nasty-negative",
      CertificationVerdict.Rejected
    )
    assertFamilyVerdict(
      "FortressDrawCertification",
      "cert-fortress-draw-certification-best-defense",
      CertificationVerdict.SupportOnly
    )

  test("FortressDrawCertification exact rows still fall back to support-only when best-defense evidence is insufficient"):
    assertFenVerdict(
      family = "FortressDrawCertification",
      fen = "7k/6pp/8/8/8/4K3/3N4/8 w - - 0 1",
      owner = Color.Black,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Insufficient
      ),
      expectedVerdict = CertificationVerdict.SupportOnly
    )

  test("perpetual check holding rows map to the frozen verdict lattice"):
    assertFamilyVerdict(
      "PerpetualCheckHolding",
      "cert-perpetual-check-holding-exact",
      CertificationVerdict.Certified
    )
    assertFamilyVerdict(
      "PerpetualCheckHolding",
      "cert-perpetual-check-holding-near-miss",
      CertificationVerdict.Rejected
    )
    assertFamilyVerdict(
      "PerpetualCheckHolding",
      "cert-perpetual-check-holding-nasty-negative",
      CertificationVerdict.Rejected
    )
    assertFamilyVerdict(
      "PerpetualCheckHolding",
      "cert-perpetual-check-holding-best-defense",
      CertificationVerdict.Deferred
    )

  test("promotion race rows map to the frozen verdict lattice"):
    assertFamilyVerdict("PromotionRace", "cert-promotion-race-exact", CertificationVerdict.Certified)
    assertFamilyVerdict("PromotionRace", "cert-promotion-race-near-miss", CertificationVerdict.Rejected)
    assertFamilyVerdict("PromotionRace", "cert-promotion-race-nasty-negative", CertificationVerdict.Rejected)
    assertFamilyVerdict("PromotionRace", "cert-promotion-race-best-defense", CertificationVerdict.Deferred)

  test("MaterialHarvest only certifies captures that exist on the current turn"):
    assertFenVerdict(
      family = "MaterialHarvest",
      fen = "4k3/5ppp/8/3n4/3R4/8/5PPP/4K3 b - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Rejected
    )

  test("MaterialHarvest rejects captures that fail the immediate recapture screen"):
    assertFenVerdict(
      family = "MaterialHarvest",
      fen = "8/5ppp/4k3/3b4/3R4/8/5PPP/4K3 w - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Rejected
    )

  test("WinningEndgame stays side-to-move sensitive"):
    assertFenVerdict(
      family = "WinningEndgame",
      fen = "8/2k5/2P5/4K3/8/8/8/8 b - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.ConversionRouteSurvival -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Rejected
    )

  test("WinningEndgame rejects rook-pawn corner draws"):
    assertFenVerdict(
      family = "WinningEndgame",
      fen = "1k6/8/PK6/8/8/8/8/8 w - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.ConversionRouteSurvival -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Rejected
    )

  test("WinningEndgame rejects rival pawn counterplay in the live runner slice"):
    assertFenVerdict(
      family = "WinningEndgame",
      fen = "4k3/8/2P5/2K5/8/8/6p1/8 w - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.ConversionRouteSurvival -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Rejected
    )

  test("PerpetualCheckHolding rejects loose checks that do not survive the defender reply set"):
    assertFenVerdict(
      family = "PerpetualCheckHolding",
      fen = "8/6pk/8/8/8/8/2Q5/6K1 b - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Rejected
    )

  test("PromotionRace rejects non-king interceptors that stop the route"):
    assertFenVerdict(
      family = "PromotionRace",
      fen = "8/8/3P1n2/k7/6p1/8/4K3/8 w - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.ConversionRouteSurvival -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Rejected
    )

  test("PromotionRace rejects equal-tempo races that fail route-survival geometry"):
    assertFenVerdict(
      family = "PromotionRace",
      fen = "4k3/2p5/3P4/8/6p1/8/4K3/8 w - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.ConversionRouteSurvival -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Rejected
    )

  private def assertFamilyVerdict(
      family: String,
      rowId: String,
      expectedVerdict: CertificationVerdict
  ): Unit =
    assertEquals(familyForRowId(rowId), family)

    val extraction = extractionForRowId(rowId)
    val familyCerts = familyClaimsForRowId(extraction, rowId)
    val cert = ownerClaimForRowId(extraction, rowId)

    assertEquals(
      familyCerts.map(_.anchor).distinct,
      Vector(WitnessAnchor.BoardAnchor)
    )
    assertEquals(
      familyCerts.size,
      2,
      clues(s"$rowId emitted unexpected live cert count: ${familyCerts.size}")
    )
    assertEquals(cert.verdict, expectedVerdict, clues(s"$rowId verdict drift"))
    assertEquals(cert.anchor, WitnessAnchor.BoardAnchor)
    assertEquals(cert.scope.key, scopeForRowId(rowId))
    assertEquals(cert.burdenTag.value, burdenTagForRowId(rowId))
    assertEquals(
      cert.payload.get("helper_tags"),
      Some(WitnessValue.TokenListValue(helperTagsForRowId(rowId)))
    )
    assertEquals(
      cert.payload.get("evidence_purposes"),
      Some(WitnessValue.TokenListValue(evidencePurposesForRowId(rowId)))
    )
    assertEquals(
      cert.payload.get("supporting_families"),
      Option.when(supportFamiliesForRowId(rowId).nonEmpty)(
        WitnessValue.TokenListValue(supportFamiliesForRowId(rowId))
      )
    )
    assertEquals(rivalClaimForRowId(extraction, rowId).verdict, CertificationVerdict.Rejected)

    if cert.verdict != CertificationVerdict.Rejected then
      assertEquals(cert.support.supportingTags, helperTagsForRowId(rowId).sorted)

      family match
        case "MaterialHarvest" =>
          assertEquals(cert.color, Some(Color.White))
          assertEquals(cert.payload.get("owner"), Some(WitnessValue.ColorValue(Color.White)))
          assert(cert.payload.get("capture_from").nonEmpty)
          assert(cert.payload.get("capture_to").nonEmpty)
          assert(cert.payload.get("captured_role").nonEmpty)
          assert(cert.payload.get("capturing_role").nonEmpty)
          assert(cert.payload.get("material_swing").nonEmpty)
        case "WinningEndgame" =>
          assertEquals(cert.color, Some(Color.White))
          assertEquals(cert.payload.get("owner"), Some(WitnessValue.ColorValue(Color.White)))
          assert(cert.payload.get("runner_square").nonEmpty)
          assert(cert.payload.get("owner_king_square").nonEmpty)
        case "FortressDrawCertification" =>
          assertEquals(cert.color, Some(Color.Black))
          assertEquals(
            cert.payload.get("supporting_families"),
            Some(WitnessValue.TokenListValue(Vector("FortressHoldingShell")))
          )
          assertEquals(cert.payload.get("holder"), Some(WitnessValue.ColorValue(Color.Black)))
          assert(cert.payload.get("king_square").nonEmpty)
        case "PerpetualCheckHolding" =>
          assertEquals(cert.color, Some(Color.White))
          assertEquals(cert.payload.get("owner"), Some(WitnessValue.ColorValue(Color.White)))
          assert(cert.payload.get("checking_piece_squares").nonEmpty)
        case "PromotionRace" =>
          assertEquals(cert.color, Some(Color.White))
          assertEquals(
            cert.payload.get("supporting_families"),
            Some(WitnessValue.TokenListValue(Vector("EndgameRaceScaffold")))
          )
          assertEquals(cert.payload.get("owner"), Some(WitnessValue.ColorValue(Color.White)))
          assert(cert.payload.get("owner_clear_run_squares").nonEmpty)
          assert(cert.payload.get("rival_clear_run_squares").nonEmpty)
          assert(cert.payload.get("owner_promotion_distance").nonEmpty)
          assert(cert.payload.get("rival_promotion_distance").nonEmpty)
        case other =>
          fail(s"Unexpected certification family $other")

  private def assertFenVerdict(
      family: String,
      fen: String,
      owner: Color,
      purposeStrengths: Map[CertificationEvidencePurpose, CertificationEvidenceStrength],
      expectedVerdict: CertificationVerdict
  ): Unit =
    val current = objectExtractionFromFen(fen)
    val extraction =
      extractionFromFen(
        fen,
        evidenceBundleForFamily(
          current = current,
          familyId = family,
          color = owner,
          purposeStrengths = purposeStrengths
        )
      )

    assertEquals(
      ownerClaimForFamily(extraction, family, owner).verdict,
      expectedVerdict,
      clues(s"$family verdict drift for $fen")
    )
