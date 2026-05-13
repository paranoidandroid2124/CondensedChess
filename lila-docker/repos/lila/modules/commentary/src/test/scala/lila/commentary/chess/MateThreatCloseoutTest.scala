package lila.commentary.chess

import java.nio.file.Files
import java.nio.file.Path

class MateThreatCloseoutTest extends munit.FunSuite:

  private val mateThreatFen = "7k/8/6K1/8/8/8/5Q2/8 w - - 0 1"
  private val mateThreatMove = Line(Square('g', 6), Square('h', 6))
  private val replyMove = Line(Square('h', 8), Square('g', 8))
  private val mateNowFen = "7k/8/6K1/8/8/8/5Q2/8 w - - 0 1"
  private val mateNowMove = Line(Square('f', 2), Square('f', 8))
  private val checkGivenFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkGivenMove = Line(Square('d', 2), Square('e', 2))
  private val promotionThreatFen = "k7/8/4P3/8/8/8/8/4K3 w - - 0 1"
  private val promotionThreatMove = Line(Square('e', 6), Square('e', 7))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def mateThreatFacts: BoardFacts =
    facts(mateThreatFen)

  private def story: Story =
    SceneMateThreat.write(mateThreatFacts, mateThreatMove).get

  private def selected(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def plan(row: Story = story): ExplanationPlan =
    ExplanationPlan.fromSelected(selected(row)).get

  private def rendered(row: Story = story): RenderedLine =
    DeterministicRenderer.fromPlan(plan(row)).get

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = mateThreatFacts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(mateThreatMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("MateThreat closeout keeps one meaning proof Story writer and speech key"):
    val row = story
    val proof = row.mateThreatProof.get
    val threatPlan = plan(row)
    val line = rendered(row)

    assertEquals(row.scene, Scene.MateThreat)
    assertEquals(row.writer, Some(StoryWriter.SceneMateThreat))
    assertEquals(row.tactic, None)
    assertEquals(row.plan, None)
    assertEquals(row.side, Side.White)
    assertEquals(row.rival, Side.Black)
    assertEquals(row.target, Some(Square('h', 8)))
    assertEquals(row.anchor, Some(Square('h', 6)))
    assertEquals(row.route, Some(mateThreatMove))
    assertEquals(row.routeSan, Some("Kh6"))

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.threateningSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.threatMove, Some(mateThreatMove))
    assertEquals(proof.destinationSquare, Some(Square('h', 6)))
    assertEquals(proof.rivalKingSquareAfterThreatMove, Some(Square('h', 8)))
    assertEquals(proof.nextSidePlyLegalCheckmateAvailable, true)
    assertEquals(proof.nextMateSan, Some("Qf8#"))

    assertEquals(threatPlan.scene, Scene.MateThreat)
    assertEquals(threatPlan.allowedClaim, Some(ExplanationClaim.ThreatensMateNext))
    assertEquals(threatPlan.allowedClaim.map(_.key), Some("threatens_mate_next"))
    assertEquals(threatPlan.evidenceLine, Some(mateThreatMove))
    assertEquals(line.claimKey, "threatens_mate_next")
    assertEquals(line.text, "Kh6 threatens mate next move.")
    assertEquals(LlmNarrationSmoke.mockNarrate(threatPlan, line), Some(line.text))

    assert(!threatPlan.toString.contains("Qf8"), "mate move remains proof-only")
    assert(!line.text.contains("Qf8"), "mate move remains out of rendered text")

  test("MateThreat closeout keeps sibling claim homes separate"):
    val mateNowStory = SceneCheckmate.write(facts(mateNowFen), mateNowMove).get
    val checkGivenStory = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val promotionThreatStory = ScenePromotionThreat.write(facts(promotionThreatFen), promotionThreatMove).get
    val mateThreatPlan = plan()
    val mateNowPlan = ExplanationPlan.fromSelected(selected(mateNowStory)).get
    val checkGivenPlan = ExplanationPlan.fromSelected(selected(checkGivenStory)).get
    val promotionThreatPlan = ExplanationPlan.fromSelected(selected(promotionThreatStory)).get

    assertEquals(ExplanationClaim.CheckmateAllowed.map(_.key), Vector("checkmates"))
    assertEquals(ExplanationClaim.CheckGivenAllowed.map(_.key), Vector("gives_check"))
    assertEquals(ExplanationClaim.PromotionThreatAllowed.map(_.key), Vector("threatens_promotion_next"))

    assertEquals(mateNowStory.scene, Scene.Checkmate)
    assertEquals(mateNowStory.checkmateProof.exists(_.complete), true)
    assertEquals(mateNowStory.mateThreatProof, None)
    assertEquals(mateNowPlan.allowedClaim.map(_.key), Some("checkmates"))

    assertEquals(checkGivenStory.scene, Scene.CheckGiven)
    assertEquals(checkGivenStory.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGivenStory.mateThreatProof, None)
    assertEquals(checkGivenPlan.allowedClaim.map(_.key), Some("gives_check"))

    assertEquals(promotionThreatStory.scene, Scene.PromotionThreat)
    assertEquals(promotionThreatStory.promotionThreatProof.exists(_.complete), true)
    assertEquals(promotionThreatStory.mateThreatProof, None)
    assertEquals(promotionThreatPlan.allowedClaim.map(_.key), Some("threatens_promotion_next"))

    Vector(
      mateNowPlan.allowedClaim.map(_.key),
      checkGivenPlan.allowedClaim.map(_.key),
      promotionThreatPlan.allowedClaim.map(_.key),
      mateThreatPlan.allowedClaim.map(_.key)
    ).flatten.distinct.sorted.foreach: key =>
      assert(
        Vector("checkmates", "gives_check", "threatens_mate_next", "threatens_promotion_next").contains(key),
        s"unexpected public claim key opened: $key"
      )

  test("MateThreat closeout keeps stronger mate engine and king-safety meanings closed"):
    val threatPlan = plan()
    val line = rendered()
    val forbiddenKeys = threatPlan.forbiddenWording.map(_.key).toSet

    Vector(
      "mate_in_n",
      "forced_mate",
      "forced",
      "unavoidable",
      "only_defense",
      "no_defense",
      "winning",
      "decisive",
      "attack",
      "best_move",
      "only_move",
      "engine_says",
      "engine_says_mate",
      "raw_pv"
    ).foreach: key =>
      assert(forbiddenKeys.contains(key), s"MateThreat plan must forbid $key")

    assert(!forbiddenKeys.contains("mate_threat"), "exact threatens-mate wording remains phraseable")
    Vector(
      "Kh6 threatens forced mate.",
      "Kh6 starts mate in two.",
      "Kh6 starts mate in N.",
      "Kh6 creates unavoidable mate.",
      "Kh6 leaves only defense.",
      "Kh6 leaves no defense.",
      "Black cannot stop mate after Kh6.",
      "Kh6 is a winning attack.",
      "Kh6 is decisive.",
      "Kh6 is the best move.",
      "Kh6 improves king safety.",
      "The engine says mate after Kh6.",
      "Kh6 has a mate score.",
      "Raw PV: Kh6 Kg8 Qf8#.",
      "Kh6 threatens Qf8#."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(threatPlan, line, output).accepted, false, output)

  test("MateThreat closeout keeps EngineCheck support-only and non-Lead rows silent"):
    val row = story
    val supports = SceneMateThreat.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Supports)).get
    val capped = SceneMateThreat.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Caps)).get
    val refuted = SceneMateThreat.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Supports, before = 250, after = 25)).get
    val unknown = row.copy(engineCheck = Some(engineCheck(row, EngineCheckStatus.Unknown)))

    assertEquals(ExplanationPlan.fromSelected(selected(supports)).flatMap(_.allowedClaim).map(_.key), Some("threatens_mate_next"))
    assertEquals(DeterministicRenderer.fromPlan(ExplanationPlan.fromSelected(selected(supports)).get).map(_.text), Some("Kh6 threatens mate next move."))

    Vector(
      "support" -> selected(row).copy(role = Role.Support, leadAllowed = false),
      "context" -> selected(row).copy(role = Role.Context, leadAllowed = false),
      "blocked" -> selected(row).copy(role = Role.Blocked, leadAllowed = false),
      "capped" -> selected(capped),
      "refuted" -> selected(refuted),
      "unknown" -> selected(unknown)
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("MateThreat closeout exposes no raw downstream or public route surface"):
    val threatPlan = plan()
    val line = rendered()
    val prompt = LlmNarrationSmoke.codexCliPrompt(threatPlan, line).get

    Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
      assert(prompt.contains(field), s"LLM smoke prompt must include bounded field $field")
    Vector(
      "raw Story",
      "MateThreatProof",
      "CheckmateProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "proofFailures",
      "source row",
      "Qf8",
      "mate score"
    ).foreach: raw =>
      assert(!prompt.contains(raw), s"LLM smoke prompt exposed raw runtime data: $raw")

    val rendererMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val rendererParameterShapes =
      rendererMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector)
    val smokeMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val smokeParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assertEquals(rendererParameterShapes, Vector(Vector("ExplanationPlan")))
    Vector("fromStory", "fromMateThreatProof", "fromBoardFacts", "fromEngineCheck", "fromEngineEval", "fromEngineLine", "callApi", "productionApi").foreach:
      method =>
        assert(!smokeMethodNames.contains(method), s"LLM smoke exposed raw/public method $method")
    Vector("Story", "MateThreatProof", "CheckmateProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!smokeParameterNames.contains(parameter), s"LLM smoke accepts raw parameter $parameter")

    val routes = Files.readString(Path.of("conf", "routes"))
    val controller = Files.readString(Path.of("app", "controllers", "Commentary.scala"))
    assert(routes.contains("POST  /api/commentary/render"))
    assert(routes.contains("POST  /internal/commentary/render-local-probe"))
    assert(!controller.contains("Ok("), "public route 200 remains closed")
    assert(!controller.contains("SceneMateThreat"), "public route must not expose MateThreat writer")
    assert(!controller.contains("MateThreatProof"), "public route must not expose MateThreatProof")
    assert(!controller.contains("LlmNarrationSmoke"), "public route must not expose user-facing LLM narration")
    assert(!controller.contains("env.mode.isProd"), "production API switch remains closed")
