package lila.commentary.chess

class DeflectRendererStage5Test extends munit.FunSuite:

  test("Stage-5 selected uncapped Lead Deflect lowers and renders deflects_defender"):
    val plan = deflectPlan
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DeflectsDefender))
    assertEquals(rendered.text, "h3 deflects the defender from d5.")
    assertEquals(rendered.claimKey, "deflects_defender")
    assertEquals(rendered.strength, ExplanationStrength.Bounded.key)
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assertEquals(rendered.text.toLowerCase.contains(phrase), false, phrase)

  test("Stage-5 non-Lead capped refuted and blocked Deflect rows produce no rendered text"):
    val story = deflectStory
    val support =
      StoryTable
        .choose(Vector(story, story.copy(proof = weakerLeadProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected weaker Deflect Support row"))
    val capped = StoryTable.choose(Vector(TacticDeflect.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(TacticDeflect.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Refutes)).get)).head
    val blocked = StoryTable.choose(Vector(story.copy(anchor = Some(defenderBeforeReply)))).head

    Vector(
      "support" -> support,
      "capped" -> capped,
      "refuted" -> refuted,
      "blocked" -> blocked
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stage-5 renderer refuses malformed Deflect plans"):
    val plan = deflectPlan

    Vector(
      "wrong claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.RemovesDefender)),
      "missing target" -> plan.copy(target = None),
      "missing anchor" -> plan.copy(anchor = None),
      "missing SAN" -> plan.copy(routeSan = None),
      "missing route" -> plan.copy(route = None),
      "missing evidence line" -> plan.copy(evidenceLine = None),
      "wrong tactic" -> plan.copy(tactic = Some(Tactic.RemoveGuard)),
      "wrong scene" -> plan.copy(scene = Scene.Material, tactic = None),
      "secondary target" -> plan.copy(secondaryTarget = Some(Square('e', 6))),
      "debug only" -> plan.copy(debugOnly = true)
    ).foreach: (label, badPlan) =>
      assertEquals(DeterministicRenderer.fromPlan(badPlan), None, label)

  private def deflectPlan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(deflectStory)).head).get

  private def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = deflectFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(deflectMove))),
      replyLine = Some(EngineLine(Vector(deflectReply))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Deflect Stage-5 FEN: $fen -> $error"), identity)

  private def deflectStory: Story =
    TacticDeflect.write(deflectFacts, Some(deflectMove), Some(deflectReply), Some(defenderBeforeReply), Some(deflectTarget)).get

  private val forbiddenPhrases =
    Vector(
      "wins material",
      "wins piece",
      "forced",
      "only move",
      "best move",
      "no defense",
      "no counterplay",
      "decoy",
      "removes defender",
      "overload",
      "trap",
      "engine",
      "pv",
      "depth",
      "e6g4"
    )

  private val deflectFacts = board("7k/8/4b3/3n4/8/8/7P/7K w - - 0 1")
  private val deflectMove = Line(Square('h', 2), Square('h', 3))
  private val deflectReply = Line(Square('e', 6), Square('g', 4))
  private val defenderBeforeReply = Square('e', 6)
  private val deflectTarget = Square('d', 5)

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
