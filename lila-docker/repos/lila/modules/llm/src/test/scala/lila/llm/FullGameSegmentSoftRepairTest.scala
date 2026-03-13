package lila.llm

import munit.FunSuite

class FullGameSegmentSoftRepairTest extends FunSuite:

  test("repairMerged reverts only the rewrite that introduced invalid SAN") {
    val original =
      "White centralizes the rook.\nBlack must watch the c-file."
    val segmentation = PolishSegmenter.segment(original)

    val repaired =
      FullGameSegmentSoftRepair.repairMerged(
        segmentation = segmentation,
        rewrites = List(
          FullGameSegmentSoftRepair.Rewrite(
            id = "s0001",
            rewritten = "White improves the rook.",
            original = "White centralizes the rook.\n"
          ),
          FullGameSegmentSoftRepair.Rewrite(
            id = "s0002",
            rewritten = "Black must watch the c-file and Qh5.",
            original = "Black must watch the c-file."
          )
        ),
        originalProse = original,
        allowedSans = Nil
      )

    assert(repaired.applied)
    assertEquals(repaired.reasons, Nil)
    assert(repaired.actions.contains("revert_s0002"))
    assert(repaired.text.contains("White improves the rook."))
    assert(repaired.text.contains("Black must watch the c-file."))
    assert(!repaired.text.contains("Qh5"))
  }
end FullGameSegmentSoftRepairTest
