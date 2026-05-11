package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class OverloadInteractionHardeningTest extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("OIH-7 fixes the Overload proof label writer and speech key chain"):
    val (story, plan, rendered) = admittedOverload
    val proof = story.overloadProof.get

    assertEquals(story.overloadProof.exists(_.complete), true)
    assertEquals(story.tactic, Some(Tactic.Overload))
    assertEquals(story.writer, Some(StoryWriter.TacticOverload))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(rendered.claimKey, "overloads_defender")
    assertEquals(rendered.text, "This move overloads the defender.")
    assertEquals(Set("OverloadProof", "Tactic.Overload", "TacticOverload", "overloads_defender").size, 4)

    assertEquals(proof.defenderDuty.exists(_.complete), true, "DefenderDuty")
    assertEquals(proof.dualDefenderDuty.exists(_.complete), true, "DualDefenderDuty")
    assertEquals(proof.overloadTest.exists(_.complete), true, "OverloadTest")
    assertEquals(proof.cannotSatisfyBoth.exists(_.complete), true, "CannotSatisfyBoth")
    assertEquals(proof.defenderDuty.exists(_.publicClaimAllowed), false, "DefenderDuty")
    assertEquals(proof.dualDefenderDuty.exists(_.publicClaimAllowed), false, "DualDefenderDuty")
    assertEquals(proof.overloadTest.exists(_.publicClaimAllowed), false, "OverloadTest")
    assertEquals(proof.cannotSatisfyBoth.exists(_.publicClaimAllowed), false, "CannotSatisfyBoth")

  test("OIH-7 Overload non Lead capped refuted and incomplete rows have no standalone text"):
    val facts = board(overloadFen)
    val (story, _, _) = admittedOverload
    val support = StoryTable.choose(Vector(story, story)).find(_.role == Role.Support).get
    val context = StoryTable.choose(Vector(story.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(story.copy(overloadProof = None))).head
    val capped = StoryTable.choose(Vector(TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable.choose(Vector(TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Refutes)).get)).head

    Vector(support, context, blocked, capped, refuted).foreach(assertNoStandaloneText(_, "OIH-7 silent row"))

  test("OIH-7 sibling tactic material and defense Stories keep their own meanings"):
    val (overload, _, _) = admittedOverload
    val overloadProof = overload.overloadProof.get

    siblingFixtures.foreach: fixture =>
      val verdicts = StoryTable.choose(Vector(overload, fixture.story))
      val siblingVerdict = verdicts.find(_.story.writer == fixture.writer).get
      val siblingPlan = ExplanationPlan.fromSelected(siblingVerdict)

      assertEquals(siblingVerdict.story.scene, fixture.scene, fixture.label)
      assertEquals(siblingVerdict.story.tactic, fixture.tactic, fixture.label)
      assertEquals(siblingVerdict.story.writer, fixture.writer, fixture.label)
      assertEquals(siblingVerdict.story.overloadProof, None, fixture.label)
      assertEquals(siblingPlan.flatMap(_.allowedClaim.map(_.key)).contains("overloads_defender"), false, fixture.label)
      assertEquals(
        siblingPlan.flatMap(DeterministicRenderer.fromPlan).exists(_.claimKey == "overloads_defender"),
        false,
        fixture.label
      )

      val contaminated = fixture.story.copy(overloadProof = Some(overloadProof))
      assertEquals(
        ExplanationPlan.fromSelected(StoryTable.choose(Vector(contaminated)).head)
          .flatMap(_.allowedClaim.map(_.key))
          .contains("overloads_defender"),
        false,
        fixture.label
      )

  test("OIH-7 downstream leak guard rejects forbidden wording and raw diagnostics"):
    val (_, plan, rendered) = admittedOverload
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val forbiddenOutputs =
      Vector(
        "This move wins material.",
        "This move wins a piece.",
        "This move wins the queen.",
        "This is forced.",
        "This is the only move.",
        "This is the best move.",
        "This is decisive.",
        "This is winning.",
        "There is no counterplay.",
        "This move deflects the defender.",
        "This move decoys the defender.",
        "This move removes the defender.",
        "The defender cannot defend everything.",
        "The defender must choose.",
        "The engine line proves the overload.",
        "The PV is winning after +1.2.",
        "proofFailures show the overload."
      )

    assertEquals(rendered.text, "This move overloads the defender.")
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("OverloadProof", "BoardFacts", "EngineCheck", "proofFailures", "raw PV", "evalBefore", "evalAfter").foreach: raw =>
      assert(!prompt.toLowerCase(java.util.Locale.ROOT).contains(raw.toLowerCase(java.util.Locale.ROOT)), raw)
    forbiddenOutputs.foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("OIH-7 public routes API and AGENTS remain closed"):
    val routes = Files.readString(Paths.get("conf/routes"))
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))
    val agents = Files.readString(Paths.get("../../../AGENTS.md"))

    assert(routes.contains("POST  /api/commentary/render"))
    assert(routes.contains("POST  /internal/commentary/render-local-probe"))
    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(controller.contains("\"status\" -> \"unavailable\""))
    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(!controller.contains("Ok("), "public route 200 remains closed")
    assert(!controller.contains("env.mode.isProd"), "production API remains closed")
    assert(!controller.contains("LlmNarrationSmoke"), "public/user-facing LLM narration remains closed")
    assert(!controller.contains("TacticOverload"), "public route must not expose Overload writer")
    assert(!controller.contains("OverloadProof"), "public route must not expose OverloadProof")
    assert(!agents.contains("### OIH-7 Closeout / Hard Cleanup"))

  private final case class SiblingFixture(
      label: String,
      story: Story,
      scene: Scene,
      tactic: Option[Tactic],
      writer: Option[StoryWriter],
      claimKey: Option[String]
  )

  private def siblingFixtures: Vector[SiblingFixture] =
    val materialFacts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialMove).get.copy(proof = orderingProof)
    val hanging = TacticHanging.write(materialFacts, materialMove).get.copy(proof = orderingProof)

    val looseFacts = board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
    val looseMove = Line(Square('d', 2), Square('h', 2))
    val loose = TacticLoose.write(looseFacts, Some(looseMove)).get.copy(proof = orderingProof)

    val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(looseMove)).get.copy(proof = orderingProof)

    val forkFacts = board("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get.copy(proof = orderingProof)

    val skewerFacts = board("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer
        .write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
        .copy(proof = orderingProof)

    val discoveredFacts = board("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = orderingProof)

    val pinFacts = board("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val pin =
      TacticPin
        .write(pinFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
        .get
        .copy(proof = orderingProof)

    val removeGuardFacts = board("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1")
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
        .get
        .copy(proof = orderingProof)

    val defenseFacts = board("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get.copy(proof = orderingProof)

    Vector(
      SiblingFixture("RemoveGuard", removeGuard, Scene.Tactic, Some(Tactic.RemoveGuard), Some(StoryWriter.TacticRemoveGuard), Some("removes_defender")),
      SiblingFixture("Defense", defense, Scene.Defense, None, Some(StoryWriter.SceneDefense), Some("defends_piece")),
      SiblingFixture("Material", material, Scene.Material, None, Some(StoryWriter.SceneMaterial), Some("material_balance_changes")),
      SiblingFixture("Hanging", hanging, Scene.Tactic, Some(Tactic.Hanging), Some(StoryWriter.TacticHanging), Some("can_win_piece")),
      SiblingFixture("Loose", loose, Scene.Tactic, Some(Tactic.Loose), Some(StoryWriter.TacticLoose), Some("attacks_loose_piece")),
      SiblingFixture("QueenHit", queenHit, Scene.Tactic, Some(Tactic.QueenHit), Some(StoryWriter.TacticQueenHit), Some("attacks_queen")),
      SiblingFixture("Fork", fork, Scene.Tactic, Some(Tactic.Fork), Some(StoryWriter.TacticFork), Some("forks_two_targets")),
      SiblingFixture("Skewer", skewer, Scene.Tactic, Some(Tactic.Skewer), Some(StoryWriter.TacticSkewer), Some("skewers_piece_to_piece")),
      SiblingFixture("Pin", pin, Scene.Tactic, Some(Tactic.Pin), Some(StoryWriter.TacticPin), Some("pins_piece")),
      SiblingFixture("DiscoveredAttack", discovered, Scene.Tactic, Some(Tactic.DiscoveredAttack), Some(StoryWriter.TacticDiscoveredAttack), Some("reveals_attack_on_piece"))
    )

  private def admittedOverload: (Story, ExplanationPlan, RenderedLine) =
    val story = overloadStory(board(overloadFen)).copy(proof = orderingProof)
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    (story, plan, rendered)

  private def overloadStory(facts: BoardFacts): Story =
    TacticOverload
      .write(
        facts,
        Some(overloadMove),
        Some(defender),
        Some(firstDutyTarget),
        Some(secondDutyTarget),
        Some(firstDutyTarget)
      )
      .get

  private def assertNoStandaloneText(verdict: Verdict, label: String): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def engineCheck(facts: BoardFacts, story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(overloadMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(if status == EngineCheckStatus.Refutes then 0 else 20)),
      depth = Some(20),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid OIH-7 FEN: $fen -> $error"), identity)

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

  private def lowProof: Proof =
    orderingProof.copy(boardProof = 60, lineProof = 60, ownerProof = 60, anchorProof = 60, routeProof = 60)
