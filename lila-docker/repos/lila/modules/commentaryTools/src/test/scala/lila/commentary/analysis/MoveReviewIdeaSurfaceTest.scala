package lila.commentary.analysis

import _root_.chess.{ Color, Knight, Pawn, Piece, Queen, Rook, Square }
import lila.commentary.*
import lila.commentary.model.*
import munit.FunSuite

final class MoveReviewIdeaSurfaceTest extends FunSuite:

  private def played(
      uci: String,
      san: String,
      from: Square,
      to: Square,
      piece: Piece,
      capturedRole: Option[_root_.chess.Role] = None
  ): MoveReviewExplanationBuilder.PlayedMove =
    MoveReviewExplanationBuilder.PlayedMove(
      uci = uci,
      san = san,
      from = from,
      to = to,
      piece = piece,
      afterFen = "after",
      capturedRole = capturedRole
    )

  private def evidence(
      facts: List[Fact] = Nil,
      motifs: List[Motif] = Nil,
      openingGoal: Option[OpeningGoals.Evaluation] = None,
      openingName: Option[String] = None
  ): MoveReviewExplanationBuilder.CanonicalEvidence =
    MoveReviewExplanationBuilder.CanonicalEvidence(
      facts = facts,
      motifs = motifs,
      openingGoal = openingGoal,
      openingName = openingName
    )

  private def moveRef(refId: String, san: String, uci: String, ply: Int): MoveReviewMoveRef =
    MoveReviewMoveRef(
      refId = refId,
      san = san,
      uci = uci,
      fenAfter = s"fen-$refId",
      ply = ply,
      moveNo = (ply + 1) / 2,
      marker = None
    )

  private def lineFacts(
      first: MoveReviewMoveRef,
      reply: Option[MoveReviewMoveRef],
      continuation: Option[MoveReviewMoveRef] = None
  ): MoveReviewPvFacts.LineFacts =
    val moves = List(Some(first), reply, continuation).flatten
    MoveReviewPvFacts.LineFacts(
      line = MoveReviewVariationRef(
        lineId = "line_01",
        scoreCp = 12,
        mate = None,
        depth = 16,
        moves = moves
      ),
      first = first,
      reply = reply,
      continuation = continuation
    )

  test("fork fact maps through one descriptor into tags title confirms purpose and learning point") {
    val fork =
      Fact.Fork(Square.F5, Knight, List(Square.E7 -> Rook, Square.H4 -> Queen), FactScope.CandidatePv)
    val descriptor =
      MoveReviewIdeaSurface
        .describe(
          played("d4f5", "Nf5", Square.D4, Square.F5, Piece(Color.White, Knight)),
          evidence(facts = List(fork)),
          Some(lineFacts(moveRef("m1", "Nf5", "d4f5", 1), Some(moveRef("m2", "Qg5", "h4g5", 2))))
        )
        .getOrElse(fail("expected fork descriptor"))

    assertEquals(descriptor.ideaKind, "fork", clue(descriptor))
    assertEquals(descriptor.source, "canonical_fact", clue(descriptor))
    assertEquals(descriptor.linePurpose, Some("create_tactical_threat"), clue(descriptor))
    assert(descriptor.reasonTags.contains("fork"), clue(descriptor.reasonTags))
    assert(descriptor.confirms.contains("fork"), clue(descriptor.confirms))
    assert(descriptor.title.contains("fork"), clue(descriptor.title))
    assert(descriptor.learningPoint.exists(_.toLowerCase.contains("tactical")), clue(descriptor.learningPoint))
  }

  test("target-piece descriptor requires a coupled PV reply before direct-threat surface") {
    val target =
      Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.CandidatePv)
    val current =
      played("d1h5", "Qh5", Square.D1, Square.H5, Piece(Color.White, Queen))
    val canonical = evidence(facts = List(target))

    assertEquals(MoveReviewIdeaSurface.describe(current, canonical, None), None)

    val descriptor =
      MoveReviewIdeaSurface
        .describe(
          current,
          canonical,
          Some(lineFacts(moveRef("m1", "Qh5", "d1h5", 3), Some(moveRef("m2", "g6", "g7g6", 4))))
        )
        .getOrElse(fail("expected PV-backed target descriptor"))

    assertEquals(descriptor.linePurpose, Some("answer_direct_threat"), clue(descriptor))
    assert(descriptor.confirms.contains("direct_threat"), clue(descriptor.confirms))
    assert(descriptor.learningPoint.exists(_.contains("g6")), clue(descriptor.learningPoint))
  }

  test("opening label alone is not a descriptor but grounded opening goal is") {
    val current =
      played("f1c4", "Bc4", Square.F1, Square.C4, Piece(Color.White, _root_.chess.Bishop))
    val labelOnly = evidence(openingName = Some("Italian Game"))
    val grounded =
      evidence(
        openingName = Some("Italian Game"),
        openingGoal = Some(
          OpeningGoals.Evaluation(
            goalName = "Development Logic",
            status = OpeningGoals.Status.Achieved,
            supportedEvidence = List("Minor piece developed"),
            missingEvidence = Nil,
            confidence = 0.86
          )
        )
      )

    assertEquals(MoveReviewIdeaSurface.describe(current, labelOnly, None), None)
    val descriptor = MoveReviewIdeaSurface.describe(current, grounded, None).getOrElse(fail("expected opening descriptor"))

    assertEquals(descriptor.source, "opening_goal", clue(descriptor))
    assertEquals(descriptor.requiresPvForAdmission, false, clue(descriptor))
    assert(descriptor.reasonTags.contains("opening_goal"), clue(descriptor.reasonTags))
    assert(descriptor.baseProse.contains("Italian Game"), clue(descriptor.baseProse))
  }

  test("endgame activity descriptor requires exact endgame fact not phase-only context") {
    val current = played("e1d2", "Kd2", Square.E1, Square.D2, Piece(Color.White, _root_.chess.King))
    val phaseOnly = evidence()
    val kingActivity = evidence(facts = List(Fact.KingActivity(Square.D2, mobility = 5, proximityToCenter = 1, FactScope.CandidatePv)))

    assertEquals(MoveReviewIdeaSurface.describe(current, phaseOnly, None), None)

    val descriptor =
      MoveReviewIdeaSurface
        .describe(
          current,
          kingActivity,
          Some(lineFacts(moveRef("m1", "Kd2", "e1d2", 60), Some(moveRef("m2", "Kg2", "h1g2", 61))))
        )
        .getOrElse(fail("expected endgame descriptor"))

    assertEquals(descriptor.source, "canonical_fact", clue(descriptor))
    assertEquals(descriptor.linePurpose, Some("improve_endgame_activity"), clue(descriptor))
    assert(descriptor.reasonTags.contains("king_activity"), clue(descriptor.reasonTags))
  }

  test("castling descriptor is local king-safety surface without requiring PV") {
    val descriptor =
      MoveReviewIdeaSurface
        .describe(
          played("e1g1", "O-O", Square.E1, Square.G1, Piece(Color.White, _root_.chess.King)),
          evidence(),
          None
        )
        .getOrElse(fail("expected castling descriptor"))

    assertEquals(descriptor.ideaKind, "king_safety", clue(descriptor))
    assertEquals(descriptor.requiresPvForAdmission, false, clue(descriptor))
    assertEquals(descriptor.linePurpose, Some("king_safety_first"), clue(descriptor))
    assert(descriptor.reasonTags.contains("king_safety"), clue(descriptor.reasonTags))
  }
