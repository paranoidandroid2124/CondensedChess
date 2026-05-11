package lila.commentary.chess

class LoosePieceStage7Test extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val replyMove = Line(Square('e', 8), Square('e', 7))

  test("Stage-7 DeterministicRenderer phrases only bounded Loose ExplanationPlan"):
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases = Vector(
      "wins the piece",
      "wins material",
      "hanging piece",
      "free piece",
      "en prise",
      "underdefended",
      "overload",
      "pressure",
      "initiative",
      "tempo",
      "best move",
      "only move",
      "forced",
      "decisive",
      "winning",
      "engine says"
    )

    assertEquals(rendered.text, "Rh2 attacks the undefended piece on h5.")
    assertEquals(rendered.claimKey, "attacks_loose_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Loose))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.AttacksLoosePiece))
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"Loose renderer used forbidden wording: $phrase")

  test("Stage-7 DeterministicRenderer rejects non Lead capped refuted malformed and sibling Loose plans"):
    Vector(
      "support" -> plan.copy(role = Role.Support, allowedClaim = None),
      "context" -> plan.copy(role = Role.Context, allowedClaim = None),
      "blocked" -> plan.copy(role = Role.Blocked, debugOnly = true),
      "missing claim" -> plan.copy(allowedClaim = None),
      "sibling queen claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.AttacksQueen)),
      "sibling remove guard claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.RemovesDefender)),
      "wrong scene" -> plan.copy(scene = Scene.Material),
      "wrong tactic" -> plan.copy(tactic = Some(Tactic.Hanging)),
      "secondary target contaminated" -> plan.copy(secondaryTarget = Some(Square('a', 1))),
      "target missing" -> plan.copy(target = None),
      "route missing" -> plan.copy(route = None),
      "route SAN missing" -> plan.copy(routeSan = None),
      "evidence mismatch" -> plan.copy(evidenceLine = Some(Line(Square('d', 2), Square('d', 8)))),
      "unbounded strength by missing forbidden wording" -> plan.copy(forbiddenWording = Vector.empty)
    ).foreach: (label, malformed) =>
      assertEquals(DeterministicRenderer.fromPlan(malformed), None, label)

    val capped = TacticLoose.withEngineCheck(leadStory, engineCheck(EngineCheckStatus.Caps, leadStory)).get
    val refuted =
      TacticLoose.withEngineCheck(leadStory, engineCheck(EngineCheckStatus.Supports, leadStory, before = 220, after = 20)).get

    Vector(
      "context" -> story.copy(proof = story.proof.copy(boardProof = 60, lineProof = 60)),
      "capped" -> capped,
      "refuted" -> refuted
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(row)).head).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Stage-7 Loose renderer surface remains ExplanationPlan only"):
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector

    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
    Vector("Story", "LoosePieceProof", "BoardFacts", "EngineCheck", "EngineLine", "Proof", "MissingEvidence")
      .foreach: forbiddenInput =>
        assert(!fromPlanParameterShapes.flatten.contains(forbiddenInput), s"renderer must not accept raw $forbiddenInput")

  private def facts: BoardFacts =
    BoardFacts.fromFen(looseFen).fold(error => fail(s"invalid Stage-7 FEN: $looseFen -> $error"), identity)

  private def story: Story =
    TacticLoose.write(facts, Some(looseMove)).get

  private def leadStory: Story =
    story.copy(proof = orderingProof)

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(leadStory)).head).get

  private def engineCheck(status: EngineCheckStatus, row: Story, before: Int = 20, after: Int = 20): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(looseMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def orderingProof: Proof =
    Proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      persistence = 99,
      immediacy = 99,
      forcing = 99,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 99,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 99
    )
