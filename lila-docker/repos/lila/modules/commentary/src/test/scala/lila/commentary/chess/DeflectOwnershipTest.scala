package lila.commentary.chess

class DeflectOwnershipTest extends munit.FunSuite:

  test("Deflect has one proof writer Story label and speech key"):
    val story = deflectStory
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.tactic, Some(Tactic.Deflect))
    assertEquals(story.writer, Some(StoryWriter.TacticDeflect))
    assertEquals(story.deflectProof.exists(_.complete), true)
    assertEquals(ExplanationClaim.DeflectAllowed, Vector(ExplanationClaim.DeflectsDefender))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DeflectsDefender))
    assertEquals(rendered.claimKey, "deflects_defender")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

  test("Deflect does not borrow neighboring proof homes"):
    val story = deflectStory

    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.overloadProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.queenHitProof, None)
    assertEquals(story.loosePieceProof, None)
    assertEquals(story.trapProof, None)
    assertEquals(story.fileOpenedProof, None)
    assertEquals(story.engineCheck, None)

  test("Deflect runtime surface has no process concept names"):
    val methodNames =
      Vector(
        DeflectProof.getClass,
        TacticDeflect.getClass,
        ExplanationPlan.getClass,
        DeterministicRenderer.getClass,
        LlmNarrationSmoke.getClass,
        StoryTable.getClass
      ).flatMap(_.getDeclaredMethods.toVector.map(_.getName))

    Vector(
      "DeflectAudit",
      "DeflectGate",
      "DeflectChecklist",
      "Stage7Deflect",
      "DeflectWorkflow"
    ).foreach: forbidden =>
      assert(!methodNames.exists(_.contains(forbidden)), s"runtime method name must not contain $forbidden")

  private def deflectStory: Story =
    TacticDeflect.write(facts, Some(route), Some(reply), Some(defenderBeforeReply), Some(target)).get

  private def facts: BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Deflect ownership FEN: $fen -> $error"), identity)

  private val fen = "7k/8/4b3/3n4/8/8/7P/7K w - - 0 1"
  private val route = Line(Square('h', 2), Square('h', 3))
  private val reply = Line(Square('e', 6), Square('g', 4))
  private val defenderBeforeReply = Square('e', 6)
  private val target = Square('d', 5)
