package lila.commentary.chess

class TrapRendererStage8Test extends munit.FunSuite:

  test("Stage-8 renderer phrases only Trap traps_piece ExplanationPlan"):
    val plan = trapPlan
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(rendered.text, "Ra7 traps the piece on a8.")
    assertEquals(rendered.claimKey, "traps_piece")
    assertEquals(rendered.strength, ExplanationStrength.Bounded.key)
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assertEquals(rendered.text.toLowerCase.contains(phrase), false, phrase)

  test("Stage-8 renderer refuses missing target missing SAN wrong claim and non-Trap traps_piece plans"):
    val plan = trapPlan

    Vector(
      "missing target" -> plan.copy(target = None),
      "missing SAN" -> plan.copy(routeSan = None),
      "wrong claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.AttacksLoosePiece)),
      "non-Lead" -> plan.copy(role = Role.Support, debugOnly = false),
      "wrong tactic" -> plan.copy(tactic = Some(Tactic.Loose)),
      "wrong scene" -> plan.copy(scene = Scene.Material, tactic = None),
      "secondary target" -> plan.copy(secondaryTarget = Some(Square('b', 8))),
      "debug only" -> plan.copy(debugOnly = true)
    ).foreach: (label, badPlan) =>
      assertEquals(DeterministicRenderer.fromPlan(badPlan), None, label)

  test("Stage-8 renderer refuses capped and refuted Trap rows before rendering"):
    val story = trapStory
    val capped = TacticTrap.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get
    val refuted = TacticTrap.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Refutes)).get

    Vector(
      "capped" -> StoryTable.choose(Vector(capped)).head,
      "refuted" -> StoryTable.choose(Vector(refuted)).head
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def trapPlan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(trapStory)).head).get

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
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap Stage-8 FEN: $fen -> $error"), identity)

  private def trapStory: Story =
    TacticTrap.write(trapFacts, Some(trapMove)).get

  private val forbiddenPhrases =
    Vector(
      "wins piece",
      "wins material",
      "forced",
      "only move",
      "best move",
      "no escape",
      "cannot be saved",
      "no counterplay",
      "queen trap",
      "free piece",
      "engine",
      "proof"
    )

  private val trapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
