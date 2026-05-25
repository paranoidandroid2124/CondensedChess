package lila.commentary.analysis.claim

import munit.FunSuite

class ClaimAuthorityPolicyTest extends FunSuite:

  test("authority decision has one public admission ladder with no extra roles") {
    assertEquals(
      ClaimAuthorityTier.values.toList,
      List(
        ClaimAuthorityTier.CertifiedOwner,
        ClaimAuthorityTier.SupportedLocal,
        ClaimAuthorityTier.DiagnosticOnly,
        ClaimAuthorityTier.Suppressed
      )
    )
  }

  test("authority decision admits only non-suppressed strategic claim tiers") {
    assert(ClaimAuthorityDecision(ClaimAuthorityTier.CertifiedOwner).admitted)
    assert(ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal).admitted)
    assert(!ClaimAuthorityDecision(ClaimAuthorityTier.DiagnosticOnly).admitted)
    assert(!ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed).admitted)
  }

  test("authority decision exposes failure codes while preserving tactical veto compatibility") {
    val decision =
      ClaimAuthorityDecision(
        tier = ClaimAuthorityTier.Suppressed,
        failureCodes = List("contract:deferred", "truth_contract_tactical_refutation")
      )

    assertEquals(decision.failureCodes, List("contract:deferred", "truth_contract_tactical_refutation"))
    assertEquals(decision.vetoReasons, List("truth_contract_tactical_refutation"))
  }

  test("supported local surface strips strong strategic framing") {
    val surface =
      ClaimAuthorityPolicy.supportedLocalSurface(
        "The key strategic fact here is that this exchange moves the game into the queenless branch."
      )

    assertEquals(surface, "A key idea is that this exchange moves the game into the queenless branch.")
  }
