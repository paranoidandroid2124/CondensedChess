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

  private def admittedFact(
      family: MoveReviewLocalFact.Family,
      source: MoveReviewLocalFact.Source = MoveReviewLocalFact.Source.CanonicalFact,
      producer: MoveReviewLocalFact.Producer = MoveReviewLocalFact.Producer.TacticalMotif,
      subject: MoveReviewLocalFact.Subject,
      strictFallbackCandidate: Boolean = true
  ): MoveReviewLocalFact.Admission =
    MoveReviewLocalFact.admitted(MoveReviewLocalFact.Candidate(
      family = family,
      source = source,
      producer = producer,
      subject = subject,
      strictFallbackCandidate = strictFallbackCandidate,
      lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
      guardrails = List("test_pv_coupled")
    ))

  private def openingGoalFact: MoveReviewLocalFact.Admission =
    admittedFact(
      MoveReviewLocalFact.Family.OpeningGoal,
      source = MoveReviewLocalFact.Source.OpeningGoalEvidence,
      producer = MoveReviewLocalFact.Producer.OpeningGoal,
      subject = MoveReviewLocalFact.Subject.OpeningGoal,
      strictFallbackCandidate = false
    )

  private def lineConsequenceFact: MoveReviewLocalFact.Admission =
    admittedFact(
      MoveReviewLocalFact.Family.LineConsequence,
      source = MoveReviewLocalFact.Source.PvCoupledLine,
      producer = MoveReviewLocalFact.Producer.LineConsequence,
      subject = MoveReviewLocalFact.Subject.PlayedMove,
      strictFallbackCandidate = false
    )

  test("builds a scoped local takeaway with move branch and evidence metadata") {
    val takeaway =
      MoveReviewScopedTakeaway
        .build(
          purpose = "quiet_development",
          played = played("f1c4", "Bc4", Square.F1, Square.C4),
          evidence = evidence(openingGoal = Some(developmentGoal)),
          lineFacts = Some(lineFacts("italian_line")),
          localFact = openingGoalFact
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
        lineFacts = None,
        localFact = openingGoalFact
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
        lineFacts = Some(mismatchedLine),
        localFact = openingGoalFact
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
          lineFacts = Some(lineFacts("target_line")),
          localFact = admittedFact(
            MoveReviewLocalFact.Family.Threat,
            subject = MoveReviewLocalFact.Subject.Target
          )
        )
        .getOrElse(fail("expected scoped takeaway"))

    assert(!takeaway.text.contains("first reply"), clue(takeaway.text))
    assert(!takeaway.text.contains("first answer"), clue(takeaway.text))
    assert(!takeaway.text.contains("asks for a response"), clue(takeaway.text))
    assert(!takeaway.text.contains("target evidence"), clue(takeaway.text))
  }

  test("target-pressure takeaway names the immediate target move when the reply leaves the target square") {
    val first = moveRef("m1", "c4", "b5c4", "fen-after-c4", 26)
    val reply = moveRef("m2", "Bc2", "d3c2", "fen-after-bc2", 27)
    val continuation = moveRef("m3", "Bc5", "f8c5", "fen-after-bc5", 28)
    val line =
      MoveReviewPvLine.LineFacts(
        line = MoveReviewVariationRef(
          lineId = "target_pressure_reply",
          scoreCp = 31,
          mate = None,
          depth = 14,
          moves = List(first, reply, continuation)
        ),
        first = first,
        reply = Some(reply),
        continuation = Some(continuation)
      )
    val localFact =
      MoveReviewLocalFact.admitted(MoveReviewLocalFact.Candidate(
        family = MoveReviewLocalFact.Family.Pressure,
        source = MoveReviewLocalFact.Source.CanonicalFact,
        producer = MoveReviewLocalFact.Producer.TargetPressure,
        subject = MoveReviewLocalFact.Subject.Target,
        strictFallbackCandidate = true,
        anchors = List(
          MoveReviewLocalFact.Anchor("target_square", "d3"),
          MoveReviewLocalFact.Anchor("target_role", "bishop")
        ),
        lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
        guardrails = List("target_fact_attacked_by_played_move", "pv_coupled")
      ))

    val takeaway =
      MoveReviewScopedTakeaway
        .build(
          purpose = "create_tactical_threat",
          played = played("b5c4", "c4", Square.B5, Square.C4),
          evidence = evidence(openingGoal = None),
          lineFacts = Some(line),
          localFact = localFact
        )
        .getOrElse(fail("expected target-pressure takeaway"))

    assertEquals(
      takeaway.text,
      "The PV keeps the pressure on the d3 bishop local to c4: Bc2 moves that bishop from d3."
    )
  }

  test("actual Qd5 file-entry takeaway does not expose internal authority wording") {
    val first = moveRef("m1", "Qd5", "c5d5", "fen-after-qd5", 50)
    val reply = moveRef("m2", "Qe7", "e4e7", "fen-after-qe7", 51)
    val continuation = moveRef("m3", "Qd4", "d5d4", "fen-after-qd4", 52)
    val qd5Line =
      MoveReviewPvLine.LineFacts(
        line = MoveReviewVariationRef(
          lineId = "actual_qd5_file_entry",
          scoreCp = 70,
          mate = None,
          depth = 10,
          moves = List(first, reply, continuation)
        ),
        first = first,
        reply = Some(reply),
        continuation = Some(continuation)
      )
    val localFact =
      MoveReviewLocalFact.admitted(MoveReviewLocalFact.Candidate(
        family = MoveReviewLocalFact.Family.Pressure,
        source = MoveReviewLocalFact.Source.CertifiedStrategy,
        producer = MoveReviewLocalFact.Producer.CertifiedStrategyDelta,
        subject = MoveReviewLocalFact.Subject.PlayedMove,
        strictFallbackCandidate = true,
        anchors = List(
          MoveReviewLocalFact.Anchor("strategic_idea_kind", "line_occupation"),
          MoveReviewLocalFact.Anchor("line_file", "d"),
          MoveReviewLocalFact.Anchor("line_file_status", "open")
        ),
        lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
        evidenceRefs = List(
          "strategic_idea_kind:line_occupation",
          "line_occupation_file:d",
          "line_occupation_status:open"
        ),
        guardrails = List("actual_qd5_file_entry")
      ))

    val takeaway =
      MoveReviewScopedTakeaway
        .build(
          purpose = "quiet_improvement",
          played = played("c5d5", "Qd5", Square.C5, Square.D5),
          evidence = evidence(openingGoal = None),
          lineFacts = Some(qd5Line),
          localFact = localFact
        )
        .getOrElse(fail("expected Qd5 file-entry scoped takeaway"))

    assertEquals(
      takeaway.text,
      "The PV keeps the open d-file occupation local to Qd5: the checked continuation begins with Qe7."
    )
  }

  test("does not render tactical or defensive scoped prose from line-only admission") {
    val current = played("f1c4", "Bc4", Square.F1, Square.C4)
    val line = Some(lineFacts("line_only"))

    assertEquals(
      MoveReviewScopedTakeaway.build(
        purpose = "create_tactical_threat",
        played = current,
        evidence = evidence(openingGoal = None),
        lineFacts = line,
        localFact = lineConsequenceFact
      ),
      None
    )
    assertEquals(
      MoveReviewScopedTakeaway.build(
        purpose = "answer_direct_threat",
        played = current,
        evidence = evidence(openingGoal = None),
        lineFacts = line,
        localFact = lineConsequenceFact
      ),
      None
    )
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
