package lila.commentary.chess

class TacticDecoyTest extends munit.FunSuite:

  test("TacticDecoy writer admits exactly one narrow Decoy Story"):
    val board = facts(positiveFen)
    val proof =
      DecoyProof.fromBoardFacts(board, Some(sideMove), Some(decoyReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(completeTrapFollowUp))
    val story =
      TacticDecoy.write(board, Some(sideMove), Some(decoyReply), Some(namedPieceBeforeReply), Some(decoySquare), Some(completeTrapFollowUp)).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(proof.complete, true)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Decoy))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(decoySquare))
    assertEquals(story.anchor, Some(sideMovedPieceAfter))
    assertEquals(story.route, Some(sideMove))
    assertEquals(story.writer, Some(StoryWriter.TacticDecoy))
    assertEquals(story.decoyProof.map(_.complete), Some(true))
    assertEquals(story.decoyProof.flatMap(_.rivalReply), Some(decoyReply))
    assertEquals(story.trapProof, None)
    assertEquals(story.secondaryTarget, None)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(verdict).map(_.allowedClaim), Some(Some(ExplanationClaim.DecoysPiece)))

  test("TacticDecoy writer rejects forged side rival target anchor and route"):
    val story = decoyStory

    Vector(
      "side" -> story.copy(side = Side.Black),
      "rival" -> story.copy(rival = Side.White),
      "target" -> story.copy(target = Some(Square('b', 8))),
      "anchor" -> story.copy(anchor = Some(Square('e', 1))),
      "route" -> story.copy(route = Some(Line(Square('e', 1), Square('e', 2))))
    ).foreach: (label, forged) =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("TacticDecoy writer rejects incomplete DecoyProof and refuted EngineCheck"):
    val story = decoyStory
    val incompleteProof =
      story.decoyProof.get.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("DecoyProof", Vector("test gap"))))
    val incompleteDecoyProof = story.copy(decoyProof = Some(incompleteProof))
    val missingDecoyProof = story.copy(decoyProof = None)
    val refuting = refutingCheck(story).copy(storyBound = true)
    val checked = TacticDecoy.withEngineCheck(story, refuting).get

    assertEquals(checked.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))

    Vector(
      "incomplete DecoyProof" -> incompleteDecoyProof,
      "missing DecoyProof" -> missingDecoyProof,
      "refuted EngineCheck" -> checked
    ).foreach: (label, falsePositive) =>
      val verdict = StoryTable.choose(Vector(falsePositive)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("TacticDecoy writer rejects sibling proof sidecar contamination"):
    contaminationRows.foreach: (label, contaminated) =>
      val verdict = StoryTable.choose(Vector(contaminated)).head
      assertEquals(verdict.story.writer, Some(StoryWriter.TacticDecoy), label)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  private def contaminationRows: Vector[(String, Story)] =
    val story = decoyStory
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val hanging = TacticHanging.write(materialFacts, materialMove).get
    val removeGuard =
      TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val overload = TacticOverload
      .write(
        overloadFacts,
        Some(overloadMove),
        Some(Square('e', 6)),
        Some(Square('e', 7)),
        Some(Square('a', 6)),
        Some(Square('e', 7))
      )
      .get
    val deflect = TacticDeflect.write(deflectFacts, Some(deflectMove), Some(deflectReply), Some(Square('e', 6)), Some(Square('d', 5))).get
    val trap = TacticTrap.write(trapFacts, Some(trapMove)).get
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get
    val loose = TacticLoose.write(looseFacts, Some(looseMove)).get
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get
    val skewer = TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val pin = TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get

    Vector(
      "Trap" -> story.copy(trapProof = trap.trapProof),
      "Deflect" -> story.copy(deflectProof = deflect.deflectProof),
      "RemoveGuard" -> story.copy(removeGuardProof = removeGuard.removeGuardProof),
      "Overload" -> story.copy(overloadProof = overload.overloadProof),
      "QueenHit" -> story.copy(queenHitProof = queenHit.queenHitProof),
      "Loose" -> story.copy(loosePieceProof = loose.loosePieceProof),
      "Material" -> story.copy(captureResult = material.captureResult),
      "Hanging" -> story.copy(captureResult = hanging.captureResult),
      "Fork" -> story.copy(multiTargetProof = fork.multiTargetProof),
      "Skewer" -> story.copy(skewerProof = skewer.skewerProof),
      "Pin" -> story.copy(pinProof = pin.pinProof)
    )

  private def decoyStory: Story =
    TacticDecoy.write(
      facts(positiveFen),
      Some(sideMove),
      Some(decoyReply),
      Some(namedPieceBeforeReply),
      Some(decoySquare),
      Some(completeTrapFollowUp)
    ).get

  private def refutingCheck(story: Story): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = story.route,
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(decoyReply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(-200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Decoy writer FEN: $fen -> $error"), identity)

  private val positiveFen = "4k3/8/1nb5/8/8/8/8/R3B1K1 w - - 0 1"
  private val afterReplyTrapFen = "n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1"
  private val sideMove = Line(Square('e', 1), Square('f', 2))
  private val decoyReply = Line(Square('b', 6), Square('a', 8))
  private val namedPieceBeforeReply = Square('b', 6)
  private val decoySquare = Square('a', 8)
  private val sideMovedPieceAfter = Square('f', 2)
  private val trapMove = Line(Square('a', 1), Square('a', 7))

  private def completeTrapFollowUp: TrapProof =
    TrapProof.fromBoardFacts(facts(afterReplyTrapFen), trapMove)

  private val materialFacts = facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
  private val materialMove = Line(Square('d', 4), Square('e', 5))
  private val removeGuardFacts = facts("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1")
  private val removeGuardMove = Line(Square('g', 8), Square('c', 4))
  private val overloadFacts = facts("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val deflectFacts = facts("7k/8/4b3/3n4/8/8/7P/7K w - - 0 1")
  private val deflectMove = Line(Square('h', 2), Square('h', 3))
  private val deflectReply = Line(Square('e', 6), Square('g', 4))
  private val trapFacts = facts(afterReplyTrapFen)
  private val queenHitFacts = facts("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val looseFacts = facts("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val forkFacts = facts("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1")
  private val forkMove = Line(Square('e', 4), Square('e', 5))
  private val skewerFacts = facts("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
  private val skewerMove = Line(Square('a', 1), Square('e', 1))
  private val pinFacts = facts("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1")
  private val pinMove = Line(Square('a', 8), Square('e', 8))
