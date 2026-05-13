package lila.commentary.chess

class TacticTrapTest extends munit.FunSuite:

  test("TacticTrap writer admits exactly one narrow Trap Story"):
    val board = facts(positiveFen)
    val proof = TrapProof.fromBoardFacts(board, trapMove)
    val story = TacticTrap.write(board, Some(trapMove)).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(proof.complete, true)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Trap))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, proof.targetPieceSquareAfter)
    assertEquals(story.anchor, proof.anchorSquareAfter)
    assertEquals(story.route, Some(trapMove))
    assertEquals(story.writer, Some(StoryWriter.TacticTrap))
    assertEquals(story.trapProof.map(_.complete), Some(true))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("traps_piece"))

  test("TacticTrap writer rejects forged target anchor route side and rival"):
    val story = TacticTrap.write(facts(positiveFen), Some(trapMove)).get

    Vector(
      "target" -> story.copy(target = Some(Square('b', 6))),
      "anchor" -> story.copy(anchor = Some(Square('a', 1))),
      "route" -> story.copy(route = Some(Line(Square('a', 1), Square('a', 6)))),
      "side" -> story.copy(side = Side.Black),
      "rival" -> story.copy(rival = Side.White)
    ).foreach: (label, forged) =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("TacticTrap writer rejects incomplete TrapProof and blocks refuted EngineCheck"):
    val board = facts(positiveFen)
    val story = TacticTrap.write(board, Some(trapMove)).get
    val incompleteProof =
      story.trapProof.get.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("TrapProof", Vector("test gap"))))
    val incompleteTrapProof = story.copy(trapProof = Some(incompleteProof))
    val missingTrapProof = story.copy(trapProof = None)
    val refuting = refutingCheck(story).copy(storyBound = true)

    val checked = TacticTrap.withEngineCheck(story, refuting)
    assertEquals(checked.flatMap(_.engineCheck).map(_.status), Some(EngineCheckStatus.Refutes))

    Vector(
      "incomplete TrapProof" -> incompleteTrapProof,
      "missing TrapProof" -> missingTrapProof,
      "refuted EngineCheck" -> checked.get
    ).foreach: (label, falsePositive) =>
      val verdict = StoryTable.choose(Vector(falsePositive)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("TacticTrap writer rejects incomplete proof fixtures"):
    assertEquals(TacticTrap.write(facts(safeEscapeFen), Some(trapMove)), None)
    assertEquals(TacticTrap.write(facts(undefendedFen), Some(trapMove)), None)
    assertEquals(TacticTrap.write(untrustedFacts(positiveFen), Some(trapMove)), None)

  private def refutingCheck(story: Story): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = story.route,
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(-200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap writer FEN: $fen -> $error"), identity)

  private def untrustedFacts(fen: String): BoardFacts =
    val ready = facts(fen)
    BoardFacts.untrusted(
      root = ready.root,
      sideToMove = ready.sideToMove,
      header = ready.header,
      sideLegal = ready.sideLegal,
      rivalLegal = ready.rivalLegal,
      control = ready.control,
      material = ready.material,
      pawns = ready.pawns,
      pieces = ready.pieces
    )

  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val positiveFen = "n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1"
  private val safeEscapeFen = "n3k3/8/2b5/8/8/8/8/R5K1 w - - 0 1"
  private val undefendedFen = "n3k3/8/8/8/8/8/5B2/R5K1 w - - 0 1"
