package lila.llm

import munit.FunSuite

class FullGameSegmentEligibilityTest extends FunSuite:

  test("eligible segment keeps substantial prose even without locks") {
    val segment =
      "White improves the rook on the open file and keeps pressure on the backward pawn while the bishop covers key entry squares."

    assert(FullGameSegmentEligibility.isEligible(segment))
  }

  test("short segment is rejected before segment polish") {
    val segment = "White improves coordination and keeps pressure."

    assert(!FullGameSegmentEligibility.isEligible(segment))
  }

  test("lock-heavy segment with sparse prose is rejected") {
    val segment =
      "Critical branch: 18... Bxh7+ Kxh7 19.Qh5+ wins, while White still must watch the king."

    assert(!FullGameSegmentEligibility.isEligible(segment))
  }

  test("segment with one SAN token can still pass when prose remains substantial") {
    val segment =
      "After 18... Bxh7+, Black keeps the initiative because the rooks are ready to enter on the h-file and the queen supports the attack."

    assert(FullGameSegmentEligibility.isEligible(segment))
  }
end FullGameSegmentEligibilityTest

