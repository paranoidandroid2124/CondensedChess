package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*

import munit.FunSuite

class ClaimAuthorityPolicyTest extends FunSuite:

  test("authority decision admits only non-suppressed strategic claim tiers") {
    assert(ClaimAuthorityDecision(ClaimAuthorityTier.CertifiedOwner).admitted)
    assert(ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal).admitted)
    assert(!ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed).admitted)
  }

  test("supported local surface strips strong strategic framing") {
    val surface =
      ClaimAuthorityPolicy.supportedLocalSurface(
        "The key strategic fact here is that this exchange moves the game into the queenless branch."
      )

    assertEquals(surface, "A local reading is that this exchange moves the game into the queenless branch.")
  }
