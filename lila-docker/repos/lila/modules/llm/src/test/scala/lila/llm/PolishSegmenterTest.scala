package lila.llm

import munit.FunSuite

class PolishSegmenterTest extends FunSuite:

  test("locks SAN and eval bearing chunks while keeping plain prose editable") {
    val text =
      """8. Bxh7+ keeps to the strongest continuation.
        |
        |a) Bxh7+ Kxh7 Qh5+ (+1.5)
        |b) O-O b6 Re1 (+0.3)
        |
        |The practical burden is mostly technical and strategic.""".stripMargin

    val seg = PolishSegmenter.segment(text)
    assert(seg.editableSegments.nonEmpty)
    val locked = seg.segments.filterNot(_.editable).map(_.text).mkString("\n")
    assert(locked.contains("Bxh7+"))
    assert(locked.contains("(+1.5)"))
    assert(locked.contains("b) O-O"))
  }

  test("merge should only replace editable segments") {
    val text = "Quiet technical phase. 8...Nf6 keeps balance."
    val seg = PolishSegmenter.segment(text)
    val rewriteMap = seg.editableSegments.map(s => s.id -> s.text.replace("Quiet", "Calmer")).toMap
    val merged = seg.merge(rewriteMap)

    assert(merged.contains("Calmer technical phase"))
    assert(merged.contains("8...Nf6"))
  }

  test("mask/unmask keeps SAN eval and branch markers intact") {
    val text =
      """8. Bxh7+ keeps to the strongest continuation.
        |a) Bxh7+ Kxh7 Qh5+ (+1.5)
        |b) O-O b6 Re1 (+0.3)""".stripMargin

    val masked = PolishSegmenter.maskStructuralTokens(text)
    assert(masked.hasLocks)
    assert(!masked.maskedText.contains("Bxh7+"))
    assertEquals(PolishSegmenter.validateLocks(masked.maskedText, masked.expectedOrder), Nil)
    assertEquals(PolishSegmenter.unmask(masked.maskedText, masked.tokenById), text)
  }

  test("validateLocks detects missing and reordered lock anchors") {
    val masked = PolishSegmenter.maskStructuralTokens("8... Bxh7+ Kxh7 Qh5+ (+1.5)")
    val anchors = """\[\[LK_[0-9]{3}\]\]""".r.findAllIn(masked.maskedText).toList
    assert(anchors.size >= 2)

    val swapped =
      masked.maskedText
        .replace(anchors.head, "__TMP__")
        .replace(anchors(1), anchors.head)
        .replace("__TMP__", anchors(1))
    val removed = masked.maskedText.replaceFirst("""\[\[LK_[0-9]{3}\]\]""", "")

    val swappedReasons = PolishSegmenter.validateLocks(swapped, masked.expectedOrder)
    val removedReasons = PolishSegmenter.validateLocks(removed, masked.expectedOrder)
    assert(swappedReasons.contains("anchor_order_violation"))
    assert(removedReasons.contains("anchor_missing"))
  }
