package lila.commentary.chess

class CheckEscapedStage0Test extends munit.FunSuite:

  private val escapeFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val quietFen = "k7/8/8/8/8/8/8/4K3 w - - 0 1"
  private val escapeMove = Line(Square('e', 1), Square('f', 1))
  private val illegalMove = Line(Square('e', 1), Square('e', 2))

  test("Stage-0 CheckEscapedProof proves only a legal same-board move that gets out of check"):
    val facts = BoardFacts.fromFen(escapeFen).toOption.get
    val proof = CheckEscapedProof.fromBoardFacts(facts, escapeMove)

    assertEquals(proof.complete, true)
    assertEquals(proof.escapingSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.escapeMove, Some(escapeMove))
    assertEquals(proof.beforeKingSquare, Some(Square('e', 1)))
    assertEquals(proof.afterKingSquare, Some(Square('f', 1)))
    assertEquals(proof.legalMove, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.beforeBoardSideKingInCheck, true)
    assertEquals(proof.afterBoardSideKingNotInCheck, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val story = SceneCheckEscaped.write(facts, escapeMove).get
    assertEquals(story.scene, Scene.CheckEscaped)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('f', 1)))
    assertEquals(story.anchor, Some(Square('e', 1)))
    assertEquals(story.route, Some(escapeMove))
    assertEquals(story.routeSan, Some("Kf1"))
    assertEquals(story.writer, Some(StoryWriter.SceneCheckEscaped))
    assertEquals(story.checkEscapedProof.exists(_.complete), true)
    assertEquals(story.proofFailures, Vector.empty)

    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.story, story)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.selected, true)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("escapes_check"))
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.text),
      Some("Kf1 gets out of check.")
    )

  test("Stage-0 CheckEscaped stays silent for illegal non-check untrusted and contaminated rows"):
    val escapeFacts = BoardFacts.fromFen(escapeFen).toOption.get
    val quietFacts = BoardFacts.fromFen(quietFen).toOption.get

    val quietProof = CheckEscapedProof.fromBoardFacts(quietFacts, escapeMove)
    assertEquals(quietProof.complete, false)
    assertEquals(quietProof.beforeBoardSideKingInCheck, false)
    assertEquals(SceneCheckEscaped.write(quietFacts, escapeMove), None)

    val illegalProof = CheckEscapedProof.fromBoardFacts(escapeFacts, illegalMove)
    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalMove, false)
    assertEquals(SceneCheckEscaped.write(escapeFacts, illegalMove), None)

    val untrustedFacts = BoardFacts.untrusted(
      root = escapeFacts.root,
      sideToMove = escapeFacts.sideToMove,
      header = escapeFacts.header,
      sideLegal = escapeFacts.sideLegal,
      rivalLegal = escapeFacts.rivalLegal,
      control = escapeFacts.control,
      material = escapeFacts.material,
      pawns = escapeFacts.pawns,
      pieces = escapeFacts.pieces
    )
    val untrustedProof = CheckEscapedProof.fromBoardFacts(untrustedFacts, escapeMove)
    assertEquals(untrustedProof.complete, false)
    assertEquals(untrustedProof.sameBoardProof, false)
    assertEquals(SceneCheckEscaped.write(untrustedFacts, escapeMove), None)

    val story = SceneCheckEscaped.write(escapeFacts, escapeMove).get
    val writerless = story.copy(writer = None)
    val contaminated = story.copy(tactic = Some(Tactic.SafeCheck))
    val incomplete = story.copy(
      checkEscapedProof = story.checkEscapedProof.map(
        _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("CheckEscapedProof", Vector("after-board side king not in check"))))
      )
    )

    Vector(writerless, contaminated, incomplete).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked)
      assertEquals(verdict.leadAllowed, false)
      assertEquals(ExplanationPlan.fromSelected(verdict), None)
