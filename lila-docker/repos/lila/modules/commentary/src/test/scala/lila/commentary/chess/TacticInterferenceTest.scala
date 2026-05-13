package lila.commentary.chess

class TacticInterferenceTest extends munit.FunSuite:

  test("TacticInterference writer admits one proof-backed Interference Story"):
    val board = facts(positiveFen)
    val proof = InterferenceProof.fromBoardFacts(board, Some(sideMove), Some(defender), Some(blockingSquare), Some(target))
    val story = TacticInterference.write(board, Some(sideMove), Some(defender), Some(blockingSquare), Some(target)).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(proof.complete, true)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Interference))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(target))
    assertEquals(story.anchor, Some(blockingSquare))
    assertEquals(story.route, Some(sideMove))
    assertEquals(story.writer, Some(StoryWriter.TacticInterference))
    assertEquals(story.interferenceProof.map(_.complete), Some(true))
    assertEquals(story.secondaryTarget, None)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), Some(ExplanationClaim.BlocksDefenderLine))

  test("TacticInterference writer binds target to an explicitly defended square"):
    val story =
      TacticInterference.write(facts(positiveFen), Some(sideMove), Some(defender), Some(blockingSquare), Some(emptyTarget)).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.tactic, Some(Tactic.Interference))
    assertEquals(story.target, Some(emptyTarget))
    assertEquals(story.anchor, Some(blockingSquare))
    assertEquals(story.interferenceProof.flatMap(_.targetPieceBefore), None)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.target), Some(emptyTarget))

  test("TacticInterference writer rejects forged side rival target anchor and route"):
    val story = interferenceStory

    Vector(
      "side" -> story.copy(side = Side.Black),
      "rival" -> story.copy(rival = Side.White),
      "target" -> story.copy(target = Some(Square('a', 2))),
      "anchor" -> story.copy(anchor = Some(Square('b', 3))),
      "route" -> story.copy(route = Some(Line(Square('b', 3), Square('c', 2))))
    ).foreach: (label, forged) =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("TacticInterference writer rejects incomplete proof and refuted EngineCheck"):
    val story = interferenceStory
    val incompleteProof =
      story.interferenceProof.get.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("InterferenceProof", Vector("test gap"))))
    val incompleteInterferenceProof = story.copy(interferenceProof = Some(incompleteProof))
    val missingInterferenceProof = story.copy(interferenceProof = None)
    val refuting = refutingCheck(story).copy(storyBound = true)
    val checked = TacticInterference.withEngineCheck(story, refuting).get

    assertEquals(checked.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))

    Vector(
      "incomplete InterferenceProof" -> incompleteInterferenceProof,
      "missing InterferenceProof" -> missingInterferenceProof,
      "refuted EngineCheck" -> checked
    ).foreach: (label, falsePositive) =>
      val verdict = StoryTable.choose(Vector(falsePositive)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

    assertEquals(TacticInterference.write(untrusted(positiveFen), Some(sideMove), Some(defender), Some(blockingSquare), Some(target)), None)
    assertEquals(TacticInterference.write(facts(captureFen), Some(captureMove), Some(defender), Some(blockingSquare), Some(target)), None)

  test("TacticInterference writer rejects borrowed proof homes"):
    contaminationRows.foreach: (label, contaminated) =>
      val verdict = StoryTable.choose(Vector(contaminated)).head
      assertEquals(verdict.story.writer, Some(StoryWriter.TacticInterference), label)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  private def contaminationRows: Vector[(String, Story)] =
    val story = interferenceStory
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val hanging = TacticHanging.write(materialFacts, materialMove).get
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).get
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
    val decoy =
      TacticDecoy.write(decoyFacts, Some(decoyMove), Some(decoyReply), Some(Square('b', 6)), Some(Square('a', 8)), Some(decoyTrapProof)).get
    val trap = TacticTrap.write(trapFacts, Some(trapMove)).get
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get
    val loose = TacticLoose.write(looseFacts, Some(looseMove)).get
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get
    val skewer = TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val pin = TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get

    Vector(
      "Decoy" -> story.copy(decoyProof = decoy.decoyProof),
      "Deflect" -> story.copy(deflectProof = deflect.deflectProof),
      "RemoveGuard" -> story.copy(removeGuardProof = removeGuard.removeGuardProof),
      "Overload" -> story.copy(overloadProof = overload.overloadProof),
      "Trap" -> story.copy(trapProof = trap.trapProof),
      "Pin" -> story.copy(pinProof = pin.pinProof),
      "Skewer" -> story.copy(skewerProof = skewer.skewerProof),
      "Fork" -> story.copy(multiTargetProof = fork.multiTargetProof),
      "DiscoveredAttack" -> story.copy(lineProof = discovered.lineProof),
      "QueenHit" -> story.copy(queenHitProof = queenHit.queenHitProof),
      "Loose" -> story.copy(loosePieceProof = loose.loosePieceProof),
      "Hanging" -> story.copy(captureResult = hanging.captureResult),
      "Material" -> story.copy(captureResult = material.captureResult),
      "Defense" -> story.copy(threatProof = defense.threatProof, defenseProof = defense.defenseProof)
    )

  private def interferenceStory: Story =
    TacticInterference.write(facts(positiveFen), Some(sideMove), Some(defender), Some(blockingSquare), Some(target)).get

  private def refutingCheck(story: Story): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = story.route,
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(-200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Interference writer FEN: $fen -> $error"), identity)

  private def untrusted(fen: String): BoardFacts =
    val board = facts(fen)
    BoardFacts.untrusted(
      root = board.root,
      sideToMove = board.sideToMove,
      header = board.header,
      sideLegal = board.sideLegal,
      rivalLegal = board.rivalLegal,
      control = board.control,
      material = board.material,
      pawns = board.pawns,
      pieces = board.pieces
    )

  private val positiveFen = "r6k/8/8/8/8/1B6/8/n6K w - - 0 1"
  private val captureFen = "r6k/8/8/8/p7/1B6/8/n6K w - - 0 1"
  private val sideMove = Line(Square('b', 3), Square('a', 4))
  private val captureMove = Line(Square('b', 3), Square('a', 4))
  private val defender = Square('a', 8)
  private val blockingSquare = Square('a', 4)
  private val target = Square('a', 1)
  private val emptyTarget = Square('a', 2)

  private val materialFacts = facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
  private val materialMove = Line(Square('d', 4), Square('e', 5))
  private val defenseFacts = facts("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
  private val defenseThreat = Line(Square('f', 5), Square('d', 4))
  private val defenseMove = Line(Square('d', 4), Square('e', 4))
  private val discoveredFacts = facts("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1")
  private val discoveredMove = Line(Square('d', 3), Square('f', 4))
  private val removeGuardFacts = facts("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1")
  private val removeGuardMove = Line(Square('g', 8), Square('c', 4))
  private val overloadFacts = facts("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val deflectFacts = facts("7k/8/4b3/3n4/8/8/7P/7K w - - 0 1")
  private val deflectMove = Line(Square('h', 2), Square('h', 3))
  private val deflectReply = Line(Square('e', 6), Square('g', 4))
  private val decoyFacts = facts("4k3/8/1nb5/8/8/8/8/R3B1K1 w - - 0 1")
  private val decoyMove = Line(Square('e', 1), Square('f', 2))
  private val decoyReply = Line(Square('b', 6), Square('a', 8))
  private val trapFacts = facts("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private def decoyTrapProof: TrapProof = TrapProof.fromBoardFacts(trapFacts, trapMove)
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
