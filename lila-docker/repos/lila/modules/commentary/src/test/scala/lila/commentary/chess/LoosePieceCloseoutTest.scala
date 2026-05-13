package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class LoosePieceCloseoutTest extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))

  test("Stage-9 Loose authority owners remain separated"):
    val row = story
    val proof = row.loosePieceProof.get
    val verdict = StoryTable.choose(Vector(leadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(row.tactic, Some(Tactic.Loose))
    assertEquals(row.writer, Some(StoryWriter.TacticLoose))
    assertEquals(plan.allowedClaim.map(_.key), Some("attacks_loose_piece"))
    assertEquals(rendered.claimKey, "attacks_loose_piece")

    assertEquals(row.captureResult, None)
    assertEquals(row.multiTargetProof, None)
    assertEquals(row.lineProof, None)
    assertEquals(row.pinProof, None)
    assertEquals(row.removeGuardProof, None)
    assertEquals(row.skewerProof, None)
    assertEquals(row.queenHitProof, None)
    assertEquals(row.plan, None)

  test("Stage-9 Loose does not become sibling tactic material tempo pressure meaning"):
    val row = leadStory
    val siblingRows =
      Vector(
        "Hanging" -> row.copy(tactic = Some(Tactic.Hanging)),
        "Material" -> row.copy(scene = Scene.Material, tactic = None),
        "QueenHit" -> row.copy(tactic = Some(Tactic.QueenHit)),
        "Fork" -> row.copy(tactic = Some(Tactic.Fork), secondaryTarget = Some(Square('a', 1))),
        "Skewer" -> row.copy(tactic = Some(Tactic.Skewer), secondaryTarget = Some(Square('a', 1))),
        "Pin" -> row.copy(tactic = Some(Tactic.Pin)),
        "RemoveGuard" -> row.copy(tactic = Some(Tactic.RemoveGuard)),
        "Tempo" -> row.copy(tactic = Some(Tactic.Tempo)),
        "Pressure" -> row.copy(scene = Scene.Initiative, tactic = None)
      )

    siblingRows.foreach: (label, contaminated) =>
      val selected = StoryTable.choose(Vector(contaminated)).head
      assertEquals(ExplanationPlan.fromSelected(selected), None, label)

  test("Stage-9 Loose public text stays attacks loose piece only"):
    val loosePlan = plan
    val looseRendered = rendered
    val publicText =
      Vector(
        looseRendered.text,
        LlmNarrationSmoke.mockNarrate(loosePlan, looseRendered).get
      ).mkString("\n").toLowerCase(java.util.Locale.ROOT)

    assertEquals(looseRendered.text, "Rh2 attacks the undefended piece on h5.")
    Vector(
      "hanging",
      "wins piece",
      "wins the piece",
      "wins material",
      "free piece",
      "en prise",
      "underdefended",
      "overloaded defender",
      "pressure",
      "initiative",
      "tempo",
      "best move",
      "only move",
      "forced",
      "decisive",
      "winning",
      "engine"
    ).foreach: phrase =>
      assert(!publicText.contains(phrase), s"Loose closeout text must not say: $phrase")

  test("Stage-9 Loose LLM verifier keeps overclaims closed"):
    val loosePlan = plan
    val looseRendered = rendered

    Vector(
      "Rh2 wins the piece.",
      "Rh2 wins material.",
      "Rh2 attacks a hanging piece.",
      "Rh2 attacks a free piece.",
      "The piece is en prise after Rh2.",
      "Rh2 attacks an underdefended piece.",
      "Rh2 attacks the overloaded defender.",
      "Rh2 creates pressure.",
      "Rh2 takes the initiative.",
      "Rh2 wins tempo.",
      "Rh2 is the best move.",
      "Rh2 is the only move.",
      "Rh2 is forced.",
      "Rh2 is decisive.",
      "Rh2 is winning.",
      "The engine line is Rh2 Kf8."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(loosePlan, looseRendered, output).accepted, false, output)

  test("Stage-9 Loose public route production API and public narration remain closed"):
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))
    assert(controller.contains("def renderCommentary"))
    assert(controller.contains("def renderLocalProbeCommentary"))
    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(!controller.contains("Ok("), "commentary public surface must not return 200")
    Vector("LoosePieceProof", "TacticLoose", "LlmNarrationSmoke", "RenderedLine").foreach: raw =>
      assert(!controller.contains(raw), s"Commentary controller must not expose $raw")

  private def facts: BoardFacts =
    BoardFacts.fromFen(looseFen).fold(error => fail(s"invalid Loose closeout FEN: $looseFen -> $error"), identity)

  private def story: Story =
    TacticLoose.write(facts, Some(looseMove)).get

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
