package lila.commentary.chess

class DecoyRendererTest extends munit.FunSuite:

  test("selected uncapped Lead Decoy lowers and renders decoys_piece"):
    val plan = decoyPlan
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DecoysPiece))
    assertEquals(rendered.text, "Bf2 decoys the piece to a8.")
    assertEquals(rendered.claimKey, "decoys_piece")
    assertEquals(rendered.strength, ExplanationStrength.Bounded.key)
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assertEquals(rendered.text.toLowerCase.contains(phrase), false, phrase)

  test("non-Lead capped refuted and blocked Decoy rows produce no rendered text"):
    val story = decoyStory
    val support =
      StoryTable
        .choose(Vector(story, story.copy(proof = weakerLeadProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected weaker Decoy Support row"))
    val capped = StoryTable.choose(Vector(TacticDecoy.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(TacticDecoy.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Refutes)).get)).head
    val blocked = StoryTable.choose(Vector(story.copy(target = Some(Square('b', 8))))).head

    Vector(
      "support" -> support,
      "capped" -> capped,
      "refuted" -> refuted,
      "blocked" -> blocked
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("renderer refuses malformed Decoy plans"):
    val plan = decoyPlan

    Vector(
      "wrong claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.TrapsPiece)),
      "missing target" -> plan.copy(target = None),
      "missing anchor" -> plan.copy(anchor = None),
      "missing SAN" -> plan.copy(routeSan = None),
      "missing route" -> plan.copy(route = None),
      "missing evidence line" -> plan.copy(evidenceLine = None),
      "wrong tactic" -> plan.copy(tactic = Some(Tactic.Trap)),
      "wrong scene" -> plan.copy(scene = Scene.Material, tactic = None),
      "secondary target" -> plan.copy(secondaryTarget = Some(Square('b', 8))),
      "debug only" -> plan.copy(debugOnly = true)
    ).foreach: (label, badPlan) =>
      assertEquals(DeterministicRenderer.fromPlan(badPlan), None, label)

  private def decoyPlan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(decoyStory)).head).get

  private def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = decoyFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(decoyMove, Line(Square('e', 8), Square('e', 7))))),
      replyLine = Some(EngineLine(Vector(decoyReply))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def decoyStory: Story =
    TacticDecoy.write(
      decoyFacts,
      Some(decoyMove),
      Some(decoyReply),
      Some(decoyNamedPiece),
      Some(decoySquare),
      Some(completeTrapFollowUp)
    ).get

  private def completeTrapFollowUp: TrapProof =
    TrapProof.fromBoardFacts(afterReplyTrapFacts, trapMove)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Decoy renderer FEN: $fen -> $error"), identity)

  private val forbiddenPhrases =
    Vector(
      "why it matters",
      "traps",
      "wins material",
      "wins piece",
      "forced",
      "only move",
      "best move",
      "cannot refuse",
      "no escape",
      "no counterplay",
      "deflect",
      "removes defender",
      "overload",
      "engine",
      "pv",
      "depth",
      "b6a8"
    )

  private val weakerLeadProof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 95,
      immediacy = 80,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 95,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 95
    )

  private val decoyFacts = board("4k3/8/1nb5/8/8/8/8/R3B1K1 w - - 0 1")
  private val afterReplyTrapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val decoyMove = Line(Square('e', 1), Square('f', 2))
  private val decoyReply = Line(Square('b', 6), Square('a', 8))
  private val decoyNamedPiece = Square('b', 6)
  private val decoySquare = Square('a', 8)
  private val trapMove = Line(Square('a', 1), Square('a', 7))
