package lila.commentary.chess

class InterferenceRendererTest extends munit.FunSuite:

  test("selected uncapped Lead Interference lowers and renders blocks_defender_line"):
    val plan = interferencePlan
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.BlocksDefenderLine))
    assertEquals(ExplanationClaim.InterferenceAllowed.map(_.key), Vector("blocks_defender_line"))
    assertEquals(rendered.text, "Ba4 blocks the defender's line to a1.")
    assertEquals(rendered.claimKey, "blocks_defender_line")
    assertEquals(rendered.strength, ExplanationStrength.Bounded.key)
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, rendered).exists(_.contains("claimKey: blocks_defender_line")), true)
    forbiddenPhrases.foreach: phrase =>
      assertEquals(rendered.text.toLowerCase.contains(phrase), false, phrase)

  test("non-Lead capped refuted and blocked Interference rows produce no rendered text"):
    val story = interferenceStory
    val support =
      StoryTable
        .choose(Vector(story, story.copy(proof = supportProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected Interference Support row"))
    val context = StoryTable.choose(Vector(story.copy(proof = lowProof))).head
    val capped = StoryTable.choose(Vector(TacticInterference.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(TacticInterference.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Refutes)).get)).head
    val blocked = StoryTable.choose(Vector(story.copy(target = Some(Square('a', 2))))).head

    Vector(
      "support" -> support,
      "context" -> context,
      "capped" -> capped,
      "refuted" -> refuted,
      "blocked" -> blocked
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("renderer refuses malformed Interference plans"):
    val plan = interferencePlan

    Vector(
      "wrong claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.DeflectsDefender)),
      "missing target" -> plan.copy(target = None),
      "missing anchor" -> plan.copy(anchor = None),
      "missing SAN" -> plan.copy(routeSan = None),
      "missing route" -> plan.copy(route = None),
      "missing evidence line" -> plan.copy(evidenceLine = None),
      "wrong tactic" -> plan.copy(tactic = Some(Tactic.Deflect)),
      "wrong scene" -> plan.copy(scene = Scene.Material, tactic = None),
      "secondary target" -> plan.copy(secondaryTarget = Some(Square('a', 8))),
      "support role" -> plan.copy(role = Role.Support),
      "debug only" -> plan.copy(debugOnly = true)
    ).foreach: (label, badPlan) =>
      assertEquals(DeterministicRenderer.fromPlan(badPlan), None, label)

  private def interferencePlan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(interferenceStory)).head).get

  private def interferenceStory: Story =
    TacticInterference.write(facts, Some(interferenceMove), Some(defender), Some(blockingSquare), Some(target)).get

  private def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(if status == EngineCheckStatus.Refutes then EngineEval(-200) else EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Interference renderer FEN: $fen -> $error"), identity)

  private val forbiddenPhrases =
    Vector(
      "why it matters",
      "wins",
      "wins material",
      "wins a piece",
      "forced",
      "only",
      "best",
      "no defense",
      "no counterplay",
      "traps",
      "decoys",
      "deflects",
      "removes",
      "overloads",
      "pins",
      "skewers",
      "forks",
      "a8",
      "b3a4",
      "b3-a4",
      "a8a1",
      "a8-a1",
      "engine",
      "eval",
      "depth",
      "pv"
    )

  private val facts = board("r6k/8/8/8/8/1B6/8/n6K w - - 0 1")
  private val interferenceMove = Line(Square('b', 3), Square('a', 4))
  private val defender = Square('a', 8)
  private val blockingSquare = Square('a', 4)
  private val target = Square('a', 1)

  private val lowProof =
    Proof(
      boardProof = 70,
      lineProof = 70,
      ownerProof = 70,
      anchorProof = 70,
      routeProof = 70,
      persistence = 0,
      immediacy = 0,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 70,
      kingHeat = 0,
      pieceSupport = 0,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 0
    )

  private val supportProof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 95,
      immediacy = 85,
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
