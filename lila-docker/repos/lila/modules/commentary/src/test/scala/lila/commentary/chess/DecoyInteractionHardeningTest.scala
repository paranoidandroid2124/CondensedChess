package lila.commentary.chess

class DecoyInteractionHardeningTest extends munit.FunSuite:

  private final case class NeighborRow(
      label: String,
      facts: BoardFacts,
      route: Line,
      story: Story,
      tactic: Option[Tactic],
      writer: Option[StoryWriter]
  )

  test("Decoy keeps neighbor proof homes in their own Story owners"):
    neighborRows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head

      assertEquals(verdict.story.scene, if row.tactic.isDefined then Scene.Tactic else Scene.Material, row.label)
      assertEquals(verdict.story.tactic, row.tactic, row.label)
      assertEquals(verdict.story.writer, row.writer, row.label)
      assertEquals(verdict.story.tactic.contains(Tactic.Decoy), false, row.label)
      assertEquals(
        TacticDecoy.write(row.facts, Some(row.route), None, None, None, None),
        None,
        s"${row.label} must not create Decoy from route evidence"
      )
      assertNoDecoySpeech(verdict, row.label)

  test("Decoy blocks borrowed sibling proof sidecars"):
    siblingProofRows.foreach: (label, contaminated) =>
      val verdict = StoryTable.choose(Vector(contaminated)).head

      assertEquals(verdict.story.writer, Some(StoryWriter.TacticDecoy), label)
      assertEquals(verdict.story.tactic, Some(Tactic.Decoy), label)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("Trap follow-up proof does not become Trap ownership or Trap speech"):
    val story = decoyStory
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).getOrElse(fail("expected selected Decoy plan"))
    val rendered = DeterministicRenderer.fromPlan(plan).getOrElse(fail("expected Decoy rendered line"))

    assertEquals(story.decoyProof.flatMap(_.trapFollowUpProof).exists(_.complete), true)
    assertEquals(story.trapProof, None)
    assertEquals(story.tactic, Some(Tactic.Decoy))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DecoysPiece))
    assertEquals(rendered.claimKey, "decoys_piece")
    assertEquals(rendered.text, "Bf2 decoys the piece to a8.")
    assertForbiddenWordingAbsent(rendered.text, "Lead Decoy")

    val trapVerdict = StoryTable.choose(Vector(TacticTrap.write(afterReplyTrapFacts, Some(trapMove)).get)).head
    assertEquals(trapVerdict.story.tactic, Some(Tactic.Trap))
    assertNoDecoySpeech(trapVerdict, "Trap owner")

  test("Trap sidecar reply line engine evidence and diagnostics alone cannot create Decoy"):
    assertEquals(TacticDecoy.write(decoyFacts, Some(decoyMove), None, Some(decoyNamedPiece), Some(decoySquare), Some(completeTrapFollowUp)), None)
    assertEquals(TacticDecoy.write(decoyFacts, None, Some(decoyReply), Some(decoyNamedPiece), Some(decoySquare), Some(completeTrapFollowUp)), None)
    assertEquals(TacticDecoy.write(decoyFacts, Some(decoyMove), Some(decoyReply), None, Some(decoySquare), Some(completeTrapFollowUp)), None)
    assertEquals(TacticDecoy.write(decoyFacts, Some(decoyMove), Some(decoyReply), Some(decoyNamedPiece), Some(decoySquare), None), None)

    val trapSidecarOnly =
      decoyStory.copy(writer = None, tactic = Some(Tactic.Trap), decoyProof = None, trapProof = Some(completeTrapFollowUp))
    val replyLineOnly =
      decoyStory.copy(writer = None, decoyProof = None, route = Some(decoyReply), routeSan = None)
    val engineOnly =
      decoyStory.copy(writer = None, decoyProof = None, engineCheck = Some(engineCheckFromStory(decoyStory, EngineCheckStatus.Supports)))
    val diagnosticsOnly =
      decoyStory.copy(writer = None, decoyProof = None, target = None, route = None, routeSan = None)

    Vector(
      "Trap sidecar" -> trapSidecarOnly,
      "reply line" -> replyLineOnly,
      "engine evidence" -> engineOnly,
      "diagnostics" -> diagnosticsOnly
    ).foreach: (label, row) =>
      assertEquals(StoryTable.choose(Vector(row)).exists(_.story.tactic.contains(Tactic.Decoy)), false, label)

  test("only selected uncapped Lead Decoy lowers to public text"):
    val lead = StoryTable.choose(Vector(decoyStory)).head
    val support =
      StoryTable
        .choose(Vector(decoyStory, decoyStory.copy(proof = supportProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected Decoy Support"))
    val context = StoryTable.choose(Vector(decoyStory.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(decoyStory.copy(target = Some(Square('b', 8))))).head
    val capped = StoryTable.choose(Vector(TacticDecoy.withEngineCheck(decoyStory, engineCheckFromStory(decoyStory, EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(TacticDecoy.withEngineCheck(decoyStory, engineCheckFromStory(decoyStory, EngineCheckStatus.Refutes)).get)).head

    assertEquals(lead.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(lead).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("decoys_piece"))

    Vector(
      "support" -> support,
      "context" -> context,
      "blocked" -> blocked,
      "capped" -> capped,
      "refuted" -> refuted
    ).foreach: (label, verdict) =>
      assertNoDecoySpeech(verdict, label)

  test("EngineCheck only supports caps or refutes existing proof-backed Decoy"):
    val story = decoyStory
    val supports = engineCheckFromStory(story, EngineCheckStatus.Supports)
    val caps = engineCheckFromStory(story, EngineCheckStatus.Caps)
    val refutes = engineCheckFromStory(story, EngineCheckStatus.Refutes)
    val engineOnly = supports.copy(storyBound = false)
    val wrongRoute =
      supports.copy(checkedMove = Some(Line(Square('e', 1), Square('e', 2))))
    val noProof = story.copy(decoyProof = None)

    assertEquals(TacticDecoy.withEngineCheck(story, supports).flatMap(_.engineCheck).map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(TacticDecoy.withEngineCheck(story, caps).flatMap(_.engineCheck).map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(TacticDecoy.withEngineCheck(story, refutes).flatMap(_.engineCheck).map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(TacticDecoy.withEngineCheck(story, engineOnly), None)
    assertEquals(TacticDecoy.withEngineCheck(story, wrongRoute), None)
    assertEquals(TacticDecoy.withEngineCheck(noProof, supports), None)

  private def neighborRows: Vector[NeighborRow] =
    Vector(
      NeighborRow("Trap", afterReplyTrapFacts, trapMove, TacticTrap.write(afterReplyTrapFacts, Some(trapMove)).get, Some(Tactic.Trap), Some(StoryWriter.TacticTrap)),
      NeighborRow(
        "Deflect",
        deflectFacts,
        deflectMove,
        TacticDeflect.write(deflectFacts, Some(deflectMove), Some(deflectReply), Some(Square('e', 6)), Some(Square('d', 5))).get,
        Some(Tactic.Deflect),
        Some(StoryWriter.TacticDeflect)
      ),
      NeighborRow(
        "RemoveGuard",
        removeGuardFacts,
        removeGuardMove,
        TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get,
        Some(Tactic.RemoveGuard),
        Some(StoryWriter.TacticRemoveGuard)
      ),
      NeighborRow("Overload", overloadFacts, overloadMove, overloadStory, Some(Tactic.Overload), Some(StoryWriter.TacticOverload)),
      NeighborRow("Loose", looseFacts, looseMove, TacticLoose.write(looseFacts, Some(looseMove)).get, Some(Tactic.Loose), Some(StoryWriter.TacticLoose)),
      NeighborRow("Hanging", materialFacts, materialMove, TacticHanging.write(materialFacts, materialMove).get, Some(Tactic.Hanging), Some(StoryWriter.TacticHanging)),
      NeighborRow("Material", materialFacts, materialMove, SceneMaterial.write(materialFacts, materialMove).get, None, Some(StoryWriter.SceneMaterial)),
      NeighborRow("QueenHit", queenHitFacts, queenHitMove, TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get, Some(Tactic.QueenHit), Some(StoryWriter.TacticQueenHit)),
      NeighborRow("Pin", pinFacts, pinMove, TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get, Some(Tactic.Pin), Some(StoryWriter.TacticPin)),
      NeighborRow("Fork", forkFacts, forkMove, TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get, Some(Tactic.Fork), Some(StoryWriter.TacticFork)),
      NeighborRow(
        "Skewer",
        skewerFacts,
        skewerMove,
        TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get,
        Some(Tactic.Skewer),
        Some(StoryWriter.TacticSkewer)
      )
    )

  private def siblingProofRows: Vector[(String, Story)] =
    val story = decoyStory
    val trap = TacticTrap.write(afterReplyTrapFacts, Some(trapMove)).get
    val deflect = TacticDeflect.write(deflectFacts, Some(deflectMove), Some(deflectReply), Some(Square('e', 6)), Some(Square('d', 5))).get
    val removeGuard = TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val loose = TacticLoose.write(looseFacts, Some(looseMove)).get
    val hanging = TacticHanging.write(materialFacts, materialMove).get
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get
    val pin = TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get
    val skewer = TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get

    Vector(
      "Trap" -> story.copy(trapProof = trap.trapProof),
      "Deflect" -> story.copy(deflectProof = deflect.deflectProof),
      "RemoveGuard" -> story.copy(removeGuardProof = removeGuard.removeGuardProof),
      "Overload" -> story.copy(overloadProof = overloadStory.overloadProof),
      "Loose" -> story.copy(loosePieceProof = loose.loosePieceProof),
      "Hanging" -> story.copy(captureResult = hanging.captureResult),
      "Material" -> story.copy(captureResult = material.captureResult),
      "QueenHit" -> story.copy(queenHitProof = queenHit.queenHitProof),
      "Pin" -> story.copy(pinProof = pin.pinProof),
      "Fork" -> story.copy(multiTargetProof = fork.multiTargetProof),
      "Skewer" -> story.copy(skewerProof = skewer.skewerProof)
    )

  private def assertNoDecoySpeech(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
    val prompt = rendered.flatMap(line => plan.flatMap(LlmNarrationSmoke.codexCliPrompt(_, line)))
    assertEquals(plan.exists(_.allowedClaim.contains(ExplanationClaim.DecoysPiece)), false, label)
    assertEquals(rendered.exists(_.claimKey == "decoys_piece"), false, label)
    assertEquals(prompt.exists(_.contains("decoys_piece")), false, label)

  private def assertForbiddenWordingAbsent(text: String, label: String): Unit =
    val lower = text.toLowerCase
    forbiddenWording.foreach: phrase =>
      assertEquals(lower.contains(phrase), false, s"$label must not say '$phrase'")

  private def decoyStory: Story =
    TacticDecoy.write(decoyFacts, Some(decoyMove), Some(decoyReply), Some(decoyNamedPiece), Some(decoySquare), Some(completeTrapFollowUp)).get

  private def overloadStory: Story =
    TacticOverload.write(overloadFacts, Some(overloadMove), Some(Square('e', 6)), Some(Square('e', 7)), Some(Square('a', 6)), Some(Square('e', 7))).get

  private def completeTrapFollowUp: TrapProof =
    TrapProof.fromBoardFacts(afterReplyTrapFacts, trapMove)

  private def engineCheckFromStory(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = decoyFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(decoyMove, Line(Square('e', 8), Square('e', 7))))),
      replyLine = Some(EngineLine(Vector(decoyReply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(if status == EngineCheckStatus.Refutes then EngineEval(-200) else EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Decoy interaction FEN: $fen -> $error"), identity)

  private val forbiddenWording =
    Vector(
      "forced",
      "only move",
      "best move",
      "cannot refuse",
      "wins material",
      "wins a piece",
      "traps the piece",
      "no escape",
      "no counterplay",
      "defender removed",
      "overloaded",
      "deflected from defense"
    )

  private val decoyFacts = board("4k3/8/1nb5/8/8/8/8/R3B1K1 w - - 0 1")
  private val afterReplyTrapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val decoyMove = Line(Square('e', 1), Square('f', 2))
  private val decoyReply = Line(Square('b', 6), Square('a', 8))
  private val decoyNamedPiece = Square('b', 6)
  private val decoySquare = Square('a', 8)
  private val trapMove = Line(Square('a', 1), Square('a', 7))

  private val deflectFacts = board("7k/8/4b3/3n4/8/8/7P/7K w - - 0 1")
  private val deflectMove = Line(Square('h', 2), Square('h', 3))
  private val deflectReply = Line(Square('e', 6), Square('g', 4))
  private val removeGuardFacts = board("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1")
  private val removeGuardMove = Line(Square('g', 8), Square('c', 4))
  private val overloadFacts = board("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val looseFacts = board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val materialFacts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
  private val materialMove = Line(Square('d', 4), Square('e', 5))
  private val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val pinFacts = board("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1")
  private val pinMove = Line(Square('a', 8), Square('e', 8))
  private val forkFacts = board("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1")
  private val forkMove = Line(Square('e', 4), Square('e', 5))
  private val skewerFacts = board("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
  private val skewerMove = Line(Square('a', 1), Square('e', 1))

  private val lowProof =
    Proof(70, 70, 70, 70, 70, 0, 0, 0, 0, 70, 0, 0, 0, 0, 0, 0)

  private val supportProof =
    Proof(90, 90, 90, 90, 90, 95, 85, 0, 0, 0, 0, 95, 0, 0, 0, 95)
