package lila.commentary.chess

class CheckGivenStage0Test extends munit.FunSuite:

  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val quietFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val illegalMove = Line(Square('d', 2), Square('e', 8))

  test("Stage-0 CheckGivenProof proves only a legal same-board move that gives check"):
    val facts = BoardFacts.fromFen(checkingFen).toOption.get
    val proof = CheckGivenProof.fromBoardFacts(facts, checkingMove)

    assertEquals(proof.complete, true)
    assertEquals(proof.checkingSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.checkMove, Some(checkingMove))
    assertEquals(proof.rivalKingSquareAfter, Some(Square('e', 8)))
    assertEquals(proof.legalMove, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.afterBoardRivalKingInCheck, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val story = SceneCheckGiven.write(facts, checkingMove).get
    assertEquals(story.scene, Scene.CheckGiven)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 8)))
    assertEquals(story.anchor, Some(Square('d', 2)))
    assertEquals(story.route, Some(checkingMove))
    assertEquals(story.routeSan, Some("Re2+"))
    assertEquals(story.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(story.checkGivenProof.exists(_.complete), true)
    assertEquals(story.proofFailures, Vector.empty)

    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.story, story)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.selected, true)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("gives_check"))
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.text),
      Some("Re2+ gives check.")
    )

  test("Stage-0 CheckGiven stays silent for illegal quiet untrusted and contaminated rows"):
    val facts = BoardFacts.fromFen(quietFen).toOption.get

    val quietProof = CheckGivenProof.fromBoardFacts(facts, quietMove)
    assertEquals(quietProof.complete, false)
    assertEquals(quietProof.afterBoardRivalKingInCheck, false)
    assertEquals(SceneCheckGiven.write(facts, quietMove), None)

    val illegalProof = CheckGivenProof.fromBoardFacts(facts, illegalMove)
    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalMove, false)
    assertEquals(SceneCheckGiven.write(facts, illegalMove), None)

    val untrustedFacts = BoardFacts.untrusted(
      root = facts.root,
      sideToMove = facts.sideToMove,
      header = facts.header,
      sideLegal = facts.sideLegal,
      rivalLegal = facts.rivalLegal,
      control = facts.control,
      material = facts.material,
      pawns = facts.pawns,
      pieces = facts.pieces
    )
    val untrustedProof = CheckGivenProof.fromBoardFacts(untrustedFacts, checkingMove)
    assertEquals(untrustedProof.complete, false)
    assertEquals(untrustedProof.sameBoardProof, false)
    assertEquals(SceneCheckGiven.write(untrustedFacts, checkingMove), None)

    val story = SceneCheckGiven.write(facts, checkingMove).get
    val writerless = story.copy(writer = None)
    val contaminated = story.copy(tactic = Some(Tactic.SafeCheck))

    Vector(writerless, contaminated).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked)
      assertEquals(verdict.leadAllowed, false)
      assertEquals(ExplanationPlan.fromSelected(verdict), None)
