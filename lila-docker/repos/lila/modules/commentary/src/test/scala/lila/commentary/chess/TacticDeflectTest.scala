package lila.commentary.chess

class TacticDeflectTest extends munit.FunSuite:

  test("Stage-2 TacticDeflect writer admits exactly one narrow Deflect Story"):
    val board = facts(positiveFen)
    val proof = DeflectProof.fromBoardFacts(board, Some(sideMove), Some(rivalReply), Some(defender), Some(target))
    val story = TacticDeflect.write(board, Some(sideMove), Some(rivalReply), Some(defender), Some(target)).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(proof.complete, true)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Deflect))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(target))
    assertEquals(story.anchor, Some(defenderAfterReply))
    assertEquals(story.route, Some(sideMove))
    assertEquals(story.writer, Some(StoryWriter.TacticDeflect))
    assertEquals(story.deflectProof.map(_.complete), Some(true))
    assertEquals(story.deflectProof.flatMap(_.rivalReply), Some(rivalReply))
    assertEquals(story.secondaryTarget, None)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(verdict).map(_.allowedClaim), Some(Some(ExplanationClaim.DeflectsDefender)))

  test("Stage-2 TacticDeflect writer rejects forged side rival target anchor and route"):
    val story = deflectStory

    Vector(
      "side" -> story.copy(side = Side.Black),
      "rival" -> story.copy(rival = Side.White),
      "target" -> story.copy(target = Some(Square('e', 6))),
      "anchor" -> story.copy(anchor = Some(defender)),
      "route" -> story.copy(route = Some(Line(Square('h', 2), Square('h', 4))))
    ).foreach: (label, forged) =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("Stage-2 TacticDeflect writer rejects incomplete DeflectProof and refuted EngineCheck"):
    val story = deflectStory
    val incompleteProof =
      story.deflectProof.get.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("DeflectProof", Vector("test gap"))))
    val incompleteDeflectProof = story.copy(deflectProof = Some(incompleteProof))
    val missingDeflectProof = story.copy(deflectProof = None)
    val refuting = refutingCheck(story).copy(storyBound = true)
    val checked = TacticDeflect.withEngineCheck(story, refuting).get

    assertEquals(checked.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))

    Vector(
      "incomplete DeflectProof" -> incompleteDeflectProof,
      "missing DeflectProof" -> missingDeflectProof,
      "refuted EngineCheck" -> checked
    ).foreach: (label, falsePositive) =>
      val verdict = StoryTable.choose(Vector(falsePositive)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("Stage-2 TacticDeflect writer rejects sibling proof sidecar contamination"):
    contaminationRows.foreach: (label, contaminated) =>
      val verdict = StoryTable.choose(Vector(contaminated)).head
      assertEquals(verdict.story.writer, Some(StoryWriter.TacticDeflect), label)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  private def contaminationRows: Vector[(String, Story)] =
    val story = deflectStory
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
    val trap = TacticTrap.write(trapFacts, Some(trapMove)).get
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get
    val skewer = TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val pin = TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get

    Vector(
      "RemoveGuard" -> story.copy(removeGuardProof = removeGuard.removeGuardProof),
      "Overload" -> story.copy(overloadProof = overload.overloadProof),
      "Trap" -> story.copy(trapProof = trap.trapProof),
      "QueenHit" -> story.copy(queenHitProof = queenHit.queenHitProof),
      "Material" -> story.copy(captureResult = material.captureResult),
      "Hanging" -> story.copy(captureResult = hanging.captureResult),
      "Fork" -> story.copy(multiTargetProof = fork.multiTargetProof),
      "Skewer" -> story.copy(skewerProof = skewer.skewerProof),
      "Pin" -> story.copy(pinProof = pin.pinProof)
    )

  private def deflectStory: Story =
    TacticDeflect.write(facts(positiveFen), Some(sideMove), Some(rivalReply), Some(defender), Some(target)).get

  private def refutingCheck(story: Story): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = story.route,
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(rivalReply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(-200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Deflect writer FEN: $fen -> $error"), identity)

  private val positiveFen = "7k/8/4b3/3n4/8/8/7P/7K w - - 0 1"
  private val sideMove = Line(Square('h', 2), Square('h', 3))
  private val rivalReply = Line(Square('e', 6), Square('g', 4))
  private val defender = Square('e', 6)
  private val defenderAfterReply = Square('g', 4)
  private val target = Square('d', 5)

  private val materialFacts = facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
  private val materialMove = Line(Square('d', 4), Square('e', 5))
  private val removeGuardFacts = facts("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1")
  private val removeGuardMove = Line(Square('g', 8), Square('c', 4))
  private val overloadFacts = facts("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val trapFacts = facts("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val queenHitFacts = facts("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val forkFacts = facts("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1")
  private val forkMove = Line(Square('e', 4), Square('e', 5))
  private val skewerFacts = facts("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
  private val skewerMove = Line(Square('a', 1), Square('e', 1))
  private val pinFacts = facts("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1")
  private val pinMove = Line(Square('a', 8), Square('e', 8))
