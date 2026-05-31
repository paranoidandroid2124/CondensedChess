package lila.commentary.analysis

import lila.commentary.analysis.semantic.RelationObservationCatalog
import munit.FunSuite

class PlayerProseBoundaryTest extends FunSuite:

  test("sanitize scrubs helper notation into player-facing prose") {
    val evaluation = PlayerProseBoundary.validate("Pin(rook, c7, queen) keeps the idea clear.")

    assertEquals(evaluation.reasons, Nil)
    assert(!evaluation.text.contains("Pin("), clue(evaluation))
    assert(evaluation.text.toLowerCase.contains("pin pressure"), clue(evaluation))
  }

  test("placeholder metadata is rejected by the hard gate") {
    val evaluation = PlayerProseBoundary.validateSanitized("This review covers Weekend Swiss (????.??.??).")

    assert(evaluation.reasons.contains("placeholder_leak_detected"), clue(evaluation))
  }

  test("broken fragments are rejected by the hard gate") {
    val evaluation = PlayerProseBoundary.validateSanitized("After 13.")

    assert(evaluation.reasons.contains("broken_fragment_detected"), clue(evaluation))
  }

  test("duplicate sentences are rejected by the hard gate") {
    val evaluation =
      PlayerProseBoundary.validateSanitized(
        "Pressure on b2 became the decisive shift. Pressure on b2 became the decisive shift."
      )

    assert(evaluation.reasons.contains("duplicate_sentence_detected"), clue(evaluation))
  }

  test("relation helper notation is denied from the player-facing prose boundary") {
    def pascalHelper(kind: String): String =
      kind
        .split("_")
        .toList
        .filter(_.nonEmpty)
        .map(part => part.head.toUpper.toString + part.drop(1))
        .mkString

    val relationKinds = RelationObservationCatalog.InventoryKinds
    val missingSnakeHits =
      relationKinds.filterNot(kind => PlayerProseBoundary.helperLeakHits(s"$kind(target)").contains(kind))
    val missingPascalHits =
      relationKinds.filterNot(kind =>
        PlayerProseBoundary.helperLeakHits(s"${pascalHelper(kind)}(target)").contains(kind)
      )
    val acceptedHelperRows =
      relationKinds.filterNot(kind =>
        PlayerProseBoundary
          .validateSanitized(s"${pascalHelper(kind)}(target) should not reach players as a helper call.")
          .reasons
          .contains("helper_symbol_leak_detected")
      )

    assertEquals(relationKinds.sorted, MoveReviewExchangeAnalyzer.RelationKind.All.sorted)
    assertEquals(missingSnakeHits, Nil)
    assertEquals(missingPascalHits, Nil)
    assertEquals(acceptedHelperRows, Nil)
  }
