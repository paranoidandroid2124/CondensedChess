package lila.commentary.chess

class TrapRowStateBoundaryTest extends munit.FunSuite:

  test("Stage-1 Trap row states lower only selected uncapped Lead to traps_piece"):
    val lead = StoryTable.choose(Vector(trapStory)).head
    val support =
      StoryTable.choose(Vector(strongerQueenHit, trapStory)).find(_.story.writer.contains(StoryWriter.TacticTrap)).get
    val context = StoryTable.choose(Vector(trapStory.copy(proof = lowProof))).head
    val incomplete = StoryTable.choose(Vector(incompleteTrap)).head
    val forged = StoryTable.choose(Vector(forgedTrap)).head
    val capped = StoryTable.choose(Vector(checkedTrap(EngineCheckStatus.Caps))).head
    val refuted = StoryTable.choose(Vector(checkedTrap(EngineCheckStatus.Refutes))).head

    assertEquals(lead.role, Role.Lead)
    assertEquals(lead.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(lead).flatMap(_.allowedClaim).map(_.key), Some("traps_piece"))
    assertEquals(ExplanationPlan.fromSelected(lead).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("traps_piece"))

    Vector(
      "Support" -> support,
      "Context" -> context,
      "incomplete Blocked" -> incomplete,
      "forged Blocked" -> forged,
      "capped" -> capped,
      "refuted" -> refuted
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(incomplete.role, Role.Blocked)
    assertEquals(forged.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)

  test("Stage-1 EngineCheck attaches only to existing proof-backed Trap"):
    val story = trapStory
    val supports = engineCheck(story, EngineCheckStatus.Supports)
    val caps = engineCheck(story, EngineCheckStatus.Caps)
    val refutes = engineCheck(story, EngineCheckStatus.Refutes)
    val engineOnly =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(trapMove),
        engineLine = Some(EngineLine(Vector(trapMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(0)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    Vector(supports, caps, refutes).foreach: check =>
      assertEquals(TacticTrap.withEngineCheck(story, check).flatMap(_.engineCheck).map(_.status), Some(check.status))

    assertEquals(TacticTrap.withEngineCheck(story.copy(trapProof = None), supports), None)
    assertEquals(TacticTrap.withEngineCheck(incompleteTrap, supports), None)
    assertEquals(TacticTrap.withEngineCheck(story, engineOnly), None)
    assertEquals(engineOnly.storyBound, false)

  test("Stage-1 Trap LLM smoke rejects stronger additions and raw diagnostics"):
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(trapStory)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    Vector(
      "wins piece" -> "Ra7 traps the piece on a8 and wins a piece.",
      "wins material" -> "Ra7 traps the piece on a8 and wins material.",
      "forced" -> "Ra7 traps the piece on a8 by force.",
      "only move" -> "Ra7 is the only move.",
      "best move" -> "Ra7 is the best move.",
      "no escape" -> "Ra7 traps the piece on a8 with no escape.",
      "no counterplay" -> "Ra7 traps the piece on a8 with no counterplay.",
      "proof failure" -> "TrapProof and proofFailures show Ra7 traps the piece.",
      "engine wording" -> "The engine confirms Ra7 traps the piece.",
      "raw PV" -> "The raw PV is Ra7 and the piece is trapped.",
      "eval wording" -> "Stockfish gives +2.1 after Ra7."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)

  private def checkedTrap(status: EngineCheckStatus): Story =
    TacticTrap.withEngineCheck(trapStory, engineCheck(trapStory, status)).get

  private def incompleteTrap: Story =
    trapStory.copy(
      trapProof = trapStory.trapProof.map(_.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("TrapProof", Vector("gap")))))
    )

  private def forgedTrap: Story =
    trapStory.copy(target = Some(Square('b', 6)))

  private def strongerQueenHit: Story =
    TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get.copy(proof = proofScore(96))

  private def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = trapFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(trapMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap row-state FEN: $fen -> $error"), identity)

  private def trapStory: Story =
    TacticTrap.write(trapFacts, Some(trapMove)).get

  private def proofScore(score: Int): Proof =
    Proof(
      boardProof = score,
      lineProof = score,
      ownerProof = score,
      anchorProof = score,
      routeProof = score,
      persistence = score,
      immediacy = score,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = score,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = score
    )

  private def lowProof: Proof =
    proofScore(60)

  private val trapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
