package lila.llm.tools.strategicpuzzle

import munit.FunSuite

import lila.strategicPuzzle.StrategicPuzzle.*

class StrategicPuzzleRevealAuditSupportTest extends FunSuite:

  private def doc =
    StrategicPuzzleDoc(
      id = "spz_test",
      schema = "chesstory.strategicPuzzle.v1",
      source = SourcePayload(seedId = "seed", opening = Some("French Defense"), eco = Some("C11")),
      position = PositionPayload(fen = "8/8/8/8/8/8/8/8 w - - 0 1", sideToMove = "white"),
      dominantFamily = Some(DominantFamilySummary(key = "pawn_break|e5", dominantIdeaKind = "pawn_break", anchor = "e5")),
      qualityScore = QualityScore(total = 90),
      generationMeta = GenerationMeta(selectionStatus = "audit"),
      runtimeShell = None
    )

  test("auditTerminal flags provenance blur and missing anchor risks") {
    val terminal =
      TerminalReveal(
        id = "terminal_1",
        outcome = StatusFull,
        title = "e4 finishes a route.",
        summary = "The move keeps initiative more stable.",
        commentary =
          "The engine preference still leans here under strict evidence mode.\n\nIt opens the position.",
        familyKey = Some("pawn_break|e5"),
        dominantIdeaKind = Some("pawn_break"),
        anchor = Some("e5"),
        lineSan = List("e4"),
        siblingMoves = Nil,
        opening = Some("French Defense"),
        eco = Some("C11"),
        dominantFamilyKey = Some("pawn_break|e5")
      )

    val row = StrategicPuzzleRevealAuditSupport.auditTerminal(doc, terminal)

    assert(row.issueCodes.contains("provenance_blur"), clues(row.issues))
    assert(row.issueCodes.contains("false_claim_risk"), clues(row.issues))
    assert(row.issueCodes.contains("missing_anchor"), clues(row.issues))
  }

  test("auditTerminal accepts anchored plan-first reveal copy") {
    val terminal =
      TerminalReveal(
        id = "terminal_2",
        outcome = StatusFull,
        title = "Nd5 finishes the route.",
        summary = "The point is to plant a knight on d5 and support it with c4.",
        commentary =
          "Nd5 fixes the knight on d5 and prepares c4, so the c-file and the e6 square stay under pressure.\n\nThat gives White a stable outpost to play around.",
        familyKey = Some("outpost|d5"),
        dominantIdeaKind = Some("piece_activation"),
        anchor = Some("d5"),
        lineSan = List("Nd5"),
        siblingMoves = Nil,
        opening = Some("French Defense"),
        eco = Some("C11"),
        dominantFamilyKey = Some("outpost|d5")
      )

    val row = StrategicPuzzleRevealAuditSupport.auditTerminal(doc, terminal)

    assertEquals(row.issueCodes, Nil, clues(row.issues))
  }
