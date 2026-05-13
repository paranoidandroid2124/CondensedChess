package lila.commentary.chess

class MateThreatRendererTest extends munit.FunSuite:

  private val mateThreatFen = "7k/8/6K1/8/8/8/5Q2/8 w - - 0 1"
  private val threatMove = Line(Square('g', 6), Square('h', 6))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateThreatFen).toOption.get

  private def plan: ExplanationPlan =
    val story = SceneMateThreat.write(facts, threatMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    ExplanationPlan.fromSelected(verdict).get

  test("MateThreat renderer phrases only the bounded ExplanationPlan"):
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(rendered.text, "Kh6 threatens mate next move.")
    assertEquals(rendered.claimKey, "threatens_mate_next")
    assertEquals(rendered.strength, "bounded")
    assert(!rendered.text.contains("Qf8"), "public text must not include the mate move")
    assert(!rendered.text.contains("#"), "public text must not include mate notation from the proof move")

  test("MateThreat renderer rejects stronger or engine-shaped wording"):
    val base = plan
    Vector(
      "mate in two" -> "Kh6 mate in two",
      "forced mate" -> "Kh6 forced mate",
      "unavoidable" -> "Kh6 unavoidable",
      "only defense" -> "Kh6 only defense",
      "no defense" -> "Kh6 no defense",
      "winning attack" -> "Kh6 winning attack",
      "engine wording" -> "Kh6 engine says",
      "mate score" -> "Kh6 mate score",
      "mate move line" -> "Kh6 Qf8#"
    ).foreach: (label, routeSan) =>
      assertEquals(DeterministicRenderer.fromPlan(base.copy(routeSan = Some(routeSan))), None, label)

  test("MateThreat renderer stays ExplanationPlan-only and refuses non-lead or wrong-claim plans"):
    val base = plan
    assertEquals(DeterministicRenderer.fromPlan(base.copy(role = Role.Support)), None)
    assertEquals(DeterministicRenderer.fromPlan(base.copy(allowedClaim = None)), None)
    assertEquals(DeterministicRenderer.fromPlan(base.copy(allowedClaim = Some(ExplanationClaim.Checkmates))), None)
    assertEquals(DeterministicRenderer.fromPlan(base.copy(scene = Scene.Checkmate)), None)
    assertEquals(DeterministicRenderer.fromPlan(base.copy(evidenceLine = None)), None)
