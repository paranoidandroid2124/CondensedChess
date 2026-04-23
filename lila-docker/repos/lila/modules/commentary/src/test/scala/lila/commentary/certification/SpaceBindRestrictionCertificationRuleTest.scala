package lila.commentary.certification

import chess.Color

import lila.commentary.validation.CertificationRuntimeTestSupport.*
import lila.commentary.witness.WitnessValue

class SpaceBindRestrictionCertificationRuleTest extends munit.FunSuite:

  test("space-bind restriction certification rows map to exact carrier and best-defense evidence"):
    assertFamilyVerdict(
      "SpaceBindRestrictionCertification",
      "cert-space-bind-restriction-exact",
      CertificationVerdict.Certified
    )
    assertFamilyVerdict(
      "SpaceBindRestrictionCertification",
      "cert-space-bind-restriction-center-release-near-miss",
      CertificationVerdict.Rejected
    )
    assertFamilyVerdict(
      "SpaceBindRestrictionCertification",
      "cert-space-bind-restriction-mobility-only-nasty-negative",
      CertificationVerdict.Rejected
    )
    assertFamilyVerdict(
      "SpaceBindRestrictionCertification",
      "cert-space-bind-restriction-best-defense",
      CertificationVerdict.SupportOnly
    )
    val exact = ownerClaimForRowId(
      extractionForRowId("cert-space-bind-restriction-exact"),
      "cert-space-bind-restriction-exact"
    )
    assertEquals(
      exact.payload.get("route_host_links"),
      Some(WitnessValue.TokenListValue(Vector("f5|outpost_anchor|center|closed_center")))
    )

  test("space-bind restriction certification requires both structural host and route support"):
    assertFenVerdict(
      family = "SpaceBindRestrictionCertification",
      fen = "4k3/8/8/3ppN2/3PP3/7B/8/4K3 w - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
      ),
      expectedVerdict = CertificationVerdict.Certified
    )
    assertFenVerdict(
      family = "SpaceBindRestrictionCertification",
      fen = "6kb/5Npp/8/8/8/8/8/4K3 b - - 0 1",
      owner = Color.White,
      purposeStrengths = Map(
        CertificationEvidencePurpose.BestDefenseSurvival -> CertificationEvidenceStrength.Satisfied,
        CertificationEvidencePurpose.TacticalReleaseDetection -> CertificationEvidenceStrength.Satisfied
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

    assertEquals(familyCerts.size, 2, clues(s"$rowId emitted unexpected live cert count"))
    assertEquals(cert.scope.key, scopeForRowId(rowId))
    assertEquals(cert.burdenTag.value, burdenTagForRowId(rowId))
    assertEquals(
      cert.payload.get("helper_tags"),
      Some(WitnessValue.TokenListValue(helperTagsForRowId(rowId)))
    )
    if cert.verdict != CertificationVerdict.Rejected then
      assertEquals(cert.support.supportingTags.toSet, helperTagsForRowId(rowId).toSet)
    assertEquals(cert.verdict, expectedVerdict)

  private def assertFenVerdict(
      family: String,
      fen: String,
      owner: Color,
      purposeStrengths: Map[CertificationEvidencePurpose, CertificationEvidenceStrength],
      expectedVerdict: CertificationVerdict
  ): Unit =
    val current = objectExtractionFromFen(fen)
    val evidence = evidenceBundleForFamily(current, family, owner, purposeStrengths)
    val extraction = extractionFromFen(fen, evidence)
    val cert = ownerClaimForFamily(extraction, family, owner)
    assertEquals(cert.verdict, expectedVerdict)
