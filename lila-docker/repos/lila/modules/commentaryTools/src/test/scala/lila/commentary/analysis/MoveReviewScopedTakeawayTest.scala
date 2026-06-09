package lila.commentary.analysis

import chess.{ Bishop, Color, Piece, Square }
import lila.commentary.*
import munit.FunSuite

final class MoveReviewScopedTakeawayTest extends FunSuite:

  private def played(
      uci: String,
      san: String,
      from: Square,
      to: Square
  ): CommentaryIdeaSurface.PlayedMove =
    CommentaryIdeaSurface.PlayedMove(
      uci = uci,
      san = san,
      from = from,
      to = to,
      piece = Piece(Color.White, Bishop),
      afterFen = "after",
      capturedRole = None
    )

  private def evidence(openingGoal: Option[OpeningGoals.Evaluation]): CommentaryIdeaSurface.MoveReviewEvidence =
    CommentaryIdeaSurface.MoveReviewEvidence(
      facts = Nil,
      motifs = Nil,
      openingGoal = openingGoal,
      openingName = Some("Italian Game")
    )

  private val developmentGoal: OpeningGoals.Evaluation =
    OpeningGoals.Evaluation(
      goalName = "Development Logic",
      status = OpeningGoals.Status.Achieved,
      supportedEvidence = List("Minor piece developed"),
      missingEvidence = Nil,
      confidence = 0.86
    )

  private def moveRef(refId: String, san: String, uci: String, fenAfter: String, ply: Int): MoveReviewMoveRef =
    MoveReviewMoveRef(
      refId = refId,
      san = san,
      uci = uci,
      fenAfter = fenAfter,
      ply = ply,
      moveNo = (ply + 1) / 2,
      marker = None
    )

  private def lineFacts(lineId: String = "line_01"): MoveReviewPvLine.LineFacts =
    val first = moveRef("m1", "Bc4", "f1c4", "fen-after-bc4", 5)
    val reply = moveRef("m2", "Nf6", "g8f6", "fen-after-nf6", 6)
    val continuation = moveRef("m3", "d3", "d2d3", "fen-after-d3", 7)
    MoveReviewPvLine.LineFacts(
      line = MoveReviewVariationRef(
        lineId = lineId,
        scoreCp = 12,
        mate = None,
        depth = 16,
        moves = List(first, reply, continuation)
      ),
      first = first,
      reply = Some(reply),
      continuation = Some(continuation)
    )

  test("builds a scoped local takeaway with move branch and evidence metadata") {
    val takeaway =
      MoveReviewScopedTakeaway
        .build(
          purpose = "quiet_development",
          played = played("f1c4", "Bc4", Square.F1, Square.C4),
          evidence = evidence(openingGoal = Some(developmentGoal)),
          lineFacts = Some(lineFacts("italian_line"))
        )
        .getOrElse(fail("expected scoped takeaway"))

    assert(takeaway.text.contains("Bc4"), clue(takeaway))
    assert(takeaway.text.contains("Nf6"), clue(takeaway))
    assert(takeaway.text.contains("d3"), clue(takeaway))
    assertEquals(takeaway.fen, "fen-after-bc4", clue(takeaway))
    assertEquals(takeaway.playedUci, "f1c4", clue(takeaway))
    assertEquals(takeaway.lineId, Some("italian_line"), clue(takeaway))
    assertEquals(takeaway.evidenceTier, MoveReviewScopedTakeaway.EvidenceTier.PvCoupledLocal, clue(takeaway))
    assertEquals(takeaway.source, MoveReviewScopedTakeaway.Source.MoveReviewPvMeaning, clue(takeaway))
    assert(takeaway.guardrails.contains("scope:move_review_local"), clue(takeaway.guardrails))
    assert(takeaway.guardrails.contains("owner:pv_coupled_line"), clue(takeaway.guardrails))
  }

  test("does not emit a takeaway without PV-coupled local line facts") {
    val takeaway =
      MoveReviewScopedTakeaway.build(
        purpose = "quiet_development",
        played = played("f1c4", "Bc4", Square.F1, Square.C4),
        evidence = evidence(openingGoal = Some(developmentGoal)),
        lineFacts = None
      )

    assertEquals(takeaway, None)
  }

  test("does not emit a takeaway when the PV first move is not the reviewed move") {
    val mismatchedLine =
      lineFacts().copy(first = moveRef("m1", "Bb5", "f1b5", "fen-after-bb5", 5))
    val takeaway =
      MoveReviewScopedTakeaway.build(
        purpose = "quiet_development",
        played = played("f1c4", "Bc4", Square.F1, Square.C4),
        evidence = evidence(openingGoal = Some(developmentGoal)),
        lineFacts = Some(mismatchedLine)
      )

    assertEquals(takeaway, None)
  }

  test("keeps checked-line reply wording role-neutral") {
    val takeaway =
      MoveReviewScopedTakeaway
        .build(
          purpose = "create_tactical_threat",
          played = played("f1c4", "Bc4", Square.F1, Square.C4),
          evidence = evidence(openingGoal = None),
          lineFacts = Some(lineFacts("target_line"))
        )
        .getOrElse(fail("expected scoped takeaway"))

    assert(!takeaway.text.contains("first reply"), clue(takeaway.text))
    assert(!takeaway.text.contains("first answer"), clue(takeaway.text))
    assert(!takeaway.text.contains("asks for a response"), clue(takeaway.text))
    assert(!takeaway.text.contains("target evidence"), clue(takeaway.text))
  }

  test("rejects globalized lesson wording at the scoped boundary") {
    val text =
      "The lesson is that this rule generally works in every position."

    assert(!MoveReviewScopedTakeaway.isAllowedText(text), clue(text))
    assert(MoveReviewScopedTakeaway.isAllowedText("The line keeps the purpose local to Bc4 and d3."), clue(text))
  }

  test("CommentaryIdeaSurface projects the scoped takeaway into the compatibility learningPoint") {
    val descriptor =
      CommentaryIdeaSurface
        .describe(
          played("f1c4", "Bc4", Square.F1, Square.C4),
          evidence(openingGoal = Some(developmentGoal)),
          Some(lineFacts())
        )
        .getOrElse(fail("expected descriptor"))

    assert(descriptor.scopedTakeaway.exists(_.text == descriptor.learningPoint.getOrElse("")), clue(descriptor))
    assertEquals(descriptor.pvInterpretation(Some(lineFacts())).map(_.learningPoint), descriptor.learningPoint)
  }
