package lila.commentary.chess

class TacticRookHitWriterTest extends munit.FunSuite:

  private val rookHitFen = "4k3/8/8/7r/8/8/3R4/4K3 w - - 0 1"
  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val emptyTargetFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val rookHitMove = Line(Square('d', 2), Square('h', 2))
  private val wrongRoute = Line(Square('d', 2), Square('d', 5))
  private val targetSquare = Square('h', 5)
  private val anchorSquare = Square('h', 2)

  test("TacticRookHit writes only a proof-backed RookHit Story identity"):
    val facts = board(rookHitFen)
    val story = TacticRookHit.write(facts, Some(rookHitMove), Some(targetSquare)).get

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.RookHit))
    assertEquals(story.writer, Some(StoryWriter.TacticRookHit))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(targetSquare))
    assertEquals(story.anchor, Some(anchorSquare))
    assertEquals(story.route, Some(rookHitMove))
    assertEquals(story.routeSan, Some("Rh2"))
    assertEquals(story.rookHitProof.exists(_.complete), true)
    assertEquals(story.rookHitProof.flatMap(_.targetSquare), Some(targetSquare))
    assertEquals(story.rookHitProof.flatMap(_.attackingPieceSquareAfter), Some(anchorSquare))
    assertEquals(story.queenHitProof, None)
    assertEquals(story.loosePieceProof, None)
    assertEquals(story.trapProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.overloadProof, None)
    assertEquals(story.deflectProof, None)
    assertEquals(story.decoyProof, None)
    assertEquals(story.interferenceProof, None)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(StoryTable.choose(Vector(story)).head.leadAllowed, true)

  test("TacticRookHit refuses incomplete RookHitProof and incomplete StoryProof"):
    val facts = board(rookHitFen)

    assertEquals(TacticRookHit.write(facts, None, Some(targetSquare)), None)
    assertEquals(TacticRookHit.write(facts, Some(rookHitMove), None), None)
    assertEquals(TacticRookHit.write(board(queenHitFen), Some(rookHitMove), Some(targetSquare)), None)
    assertEquals(TacticRookHit.write(board(emptyTargetFen), Some(rookHitMove), Some(targetSquare)), None)

    val incompleteProof = RookHitProof.fromBoardFacts(
      facts,
      rookHitMove,
      Some(targetSquare),
      storyProof = StoryProof.empty
    )
    assertEquals(incompleteProof.complete, false)
    assertEquals(TacticRookHit.fromProof(facts, rookHitMove, incompleteProof), None)

    val borrowedProof = RookHitProof.fromBoardFacts(facts, rookHitMove, Some(targetSquare))
    assertEquals(borrowedProof.complete, true)
    assertEquals(TacticRookHit.fromProof(board(queenHitFen), rookHitMove, borrowedProof), None)

  test("TacticRookHit rejects forged side rival target anchor and route bindings"):
    val facts = board(rookHitFen)
    val story = TacticRookHit.write(facts, Some(rookHitMove), Some(targetSquare)).get

    val forgedRows = Vector(
      "side" -> story.copy(side = Side.Black),
      "rival" -> story.copy(rival = Side.White),
      "target" -> story.copy(target = Some(Square('h', 4))),
      "anchor" -> story.copy(anchor = Some(Square('d', 2))),
      "route" -> story.copy(route = Some(wrongRoute))
    )

    forgedRows.foreach: (label, forged) =>
      assertEquals(TacticRookHit.validStory(forged), false, label)
      assertEquals(StoryTable.choose(Vector(forged)).head.leadAllowed, false, label)

  test("TacticRookHit attaches only non-refuting EngineCheck bound to the same route"):
    val facts = board(rookHitFen)
    val story = TacticRookHit.write(facts, Some(rookHitMove), Some(targetSquare)).get
    val supports = engineCheck(facts, story, EngineCheckStatus.Supports)
    val caps = engineCheck(facts, story, EngineCheckStatus.Caps)
    val refutes = engineCheck(facts, story, EngineCheckStatus.Refutes)
    val unbound = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(rookHitMove),
      engineLine = Some(EngineLine(Vector(rookHitMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(TacticRookHit.withEngineCheck(story, supports).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Supports)))
    assertEquals(TacticRookHit.withEngineCheck(story, caps).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Caps)))
    assertEquals(TacticRookHit.withEngineCheck(story, refutes), None)
    assertEquals(TacticRookHit.withEngineCheck(story, unbound), None)
    assertEquals(TacticRookHit.write(facts, Some(rookHitMove), Some(targetSquare), Some(refutes)), None)

  private def engineCheck(facts: BoardFacts, story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(rookHitMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid TacticRookHit FEN: $fen -> $error"), identity)
