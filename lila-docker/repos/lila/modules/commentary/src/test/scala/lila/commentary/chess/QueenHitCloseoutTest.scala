package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class QueenHitCloseoutTest extends munit.FunSuite:

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))

  test("Stage-9 QueenHit authority owners remain separated"):
    val row = story
    val proof = row.queenHitProof.get
    val verdict = StoryTable.choose(Vector(leadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(row.tactic, Some(Tactic.QueenHit))
    assertEquals(row.writer, Some(StoryWriter.TacticQueenHit))
    assertEquals(plan.allowedClaim.map(_.key), Some("attacks_queen"))
    assertEquals(rendered.claimKey, "attacks_queen")

    assertEquals(row.captureResult, None)
    assertEquals(row.multiTargetProof, None)
    assertEquals(row.lineProof, None)
    assertEquals(row.pinProof, None)
    assertEquals(row.removeGuardProof, None)
    assertEquals(row.skewerProof, None)
    assertEquals(row.plan, None)

  test("Stage-9 QueenHit does not become sibling tactic or material meaning"):
    val row = leadStory
    val siblingRows =
      Vector(
        "Hanging" -> row.copy(tactic = Some(Tactic.Hanging)),
        "Fork" -> row.copy(tactic = Some(Tactic.Fork), secondaryTarget = Some(Square('a', 1))),
        "Skewer" -> row.copy(tactic = Some(Tactic.Skewer), secondaryTarget = Some(Square('a', 1))),
        "Pin" -> row.copy(tactic = Some(Tactic.Pin)),
        "RemoveGuard" -> row.copy(tactic = Some(Tactic.RemoveGuard)),
        "Material" -> row.copy(scene = Scene.Material, tactic = None),
        "Tempo" -> row.copy(tactic = Some(Tactic.Tempo))
      )

    siblingRows.foreach: (label, contaminated) =>
      val selected = StoryTable.choose(Vector(contaminated)).head
      assertEquals(ExplanationPlan.fromSelected(selected), None, label)

  test("Stage-9 QueenHit public text stays attacks queen only"):
    val queenHitPlan = plan
    val queenHitRendered = rendered
    val publicText =
      Vector(
        queenHitRendered.text,
        LlmNarrationSmoke.mockNarrate(queenHitPlan, queenHitRendered).get
      ).mkString("\n").toLowerCase(java.util.Locale.ROOT)

    assertEquals(queenHitRendered.text, "Rh2 attacks the queen on h5.")
    Vector(
      "wins queen",
      "wins the queen",
      "traps queen",
      "traps the queen",
      "queen is lost",
      "tempo",
      "initiative",
      "pressure",
      "material gain",
      "wins material",
      "best move",
      "only move",
      "forced",
      "decisive",
      "winning",
      "engine"
    ).foreach: phrase =>
      assert(!publicText.contains(phrase), s"QueenHit closeout text must not say: $phrase")

  test("Stage-9 QueenHit LLM verifier keeps overclaims closed"):
    val queenHitPlan = plan
    val queenHitRendered = rendered

    Vector(
      "Rh2 wins the queen.",
      "Rh2 traps the queen.",
      "The queen is lost after Rh2.",
      "Rh2 gains tempo.",
      "Rh2 wins material.",
      "Rh2 is the best move.",
      "Rh2 is the only move.",
      "Rh2 is forced.",
      "Rh2 is decisive.",
      "Rh2 is winning.",
      "The engine line is Rh2 Kf8."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(queenHitPlan, queenHitRendered, output).accepted, false, output)

  test("Stage-9 QueenHit public route production API and public narration remain closed"):
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))
    assert(controller.contains("def renderCommentary"))
    assert(controller.contains("def renderLocalProbeCommentary"))
    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(!controller.contains("Ok("), "commentary public surface must not return 200")
    Vector("QueenHitProof", "TacticQueenHit", "LlmNarrationSmoke", "RenderedLine").foreach: raw =>
      assert(!controller.contains(raw), s"Commentary controller must not expose $raw")

  private def facts: BoardFacts =
    BoardFacts.fromFen(queenHitFen).fold(error => fail(s"invalid QueenHit closeout FEN: $queenHitFen -> $error"), identity)

  private def story: Story =
    TacticQueenHit.write(facts, Some(queenHitMove)).get

  private def leadStory: Story =
    story.copy(proof = orderingProof)

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(leadStory)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

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
