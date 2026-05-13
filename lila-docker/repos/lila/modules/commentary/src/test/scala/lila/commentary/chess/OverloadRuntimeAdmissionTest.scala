package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class OverloadRuntimeAdmissionTest extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-9 authority chain keeps proof Story writer and speech key in separate homes"):
    val (story, plan, rendered) = admittedOverload
    val proof = story.overloadProof.get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Overload))
    assertEquals(story.writer, Some(StoryWriter.TacticOverload))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(rendered.claimKey, "overloads_defender")

    assert(proof.defenderDuty.exists(_.complete))
    assert(proof.dualDefenderDuty.exists(_.complete))
    assert(proof.overloadTest.exists(_.complete))
    assert(proof.cannotSatisfyBoth.exists(_.complete))
    assertEquals(proof.defenderDuty.exists(_.publicClaimAllowed), false)
    assertEquals(proof.dualDefenderDuty.exists(_.publicClaimAllowed), false)
    assertEquals(proof.overloadTest.exists(_.publicClaimAllowed), false)
    assertEquals(proof.cannotSatisfyBoth.exists(_.publicClaimAllowed), false)

    assertEquals(Set("OverloadProof", "Overload", "TacticOverload", "overloads_defender").size, 4)

  test("Stage-9 OverloadProof requires DefenderDuty DualDefenderDuty OverloadTest and CannotSatisfyBoth"):
    val (story, _, _) = admittedOverload
    val proof = story.overloadProof.get
    val missingRelationProofs =
      Vector(
        "DefenderDuty" -> proof.copy(defenderDuty = None),
        "DualDefenderDuty" -> proof.copy(dualDefenderDuty = None),
        "OverloadTest" -> proof.copy(overloadTest = None),
        "CannotSatisfyBoth" -> proof.copy(cannotSatisfyBoth = None)
      )

    missingRelationProofs.foreach: (label, forgedProof) =>
      val forged = story.copy(overloadProof = Some(forgedProof))
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(
        ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan),
        None,
        label
      )

  test("OIH-1 missing proof ingredient makes OverloadProof incomplete and blocks Tactic.Overload"):
    val (story, _, _) = admittedOverload
    val proof = story.overloadProof.get
    val missingIngredients =
      Vector(
        "DefenderDuty" -> proof.copy(defenderDuty = None),
        "DualDefenderDuty" -> proof.copy(dualDefenderDuty = None),
        "OverloadTest" -> proof.copy(overloadTest = None),
        "CannotSatisfyBoth" -> proof.copy(cannotSatisfyBoth = None)
      )

    missingIngredients.foreach: (label, forgedProof) =>
      assertEquals(forgedProof.complete, false, label)
      assertNoOverloadSpeech(story.copy(overloadProof = Some(forgedProof)), label)

  test("OIH-1 proof ingredients alone are internal readiness and create no public Story"):
    val (story, _, _) = admittedOverload
    val proof = story.overloadProof.get
    val ingredientOnlyProofs =
      Vector(
        "DefenderDuty" -> proof.copy(dualDefenderDuty = None, overloadTest = None, cannotSatisfyBoth = None),
        "DualDefenderDuty" -> proof.copy(defenderDuty = None, overloadTest = None, cannotSatisfyBoth = None),
        "OverloadTest" -> proof.copy(defenderDuty = None, dualDefenderDuty = None, cannotSatisfyBoth = None),
        "CannotSatisfyBoth" -> proof.copy(defenderDuty = None, dualDefenderDuty = None, overloadTest = None)
      )

    ingredientOnlyProofs.foreach: (label, ingredientOnlyProof) =>
      assertEquals(ingredientOnlyProof.complete, false, label)
      assertNoOverloadSpeech(
        story.copy(writer = None, overloadProof = Some(ingredientOnlyProof)),
        label
      )

  test("OIH-1 BoardFacts EngineCheck raw PV eval and proofFailures cannot create proof ingredients"):
    val facts = board(overloadFen)
    val (story, _, _) = admittedOverload
    val proof = story.overloadProof.get
    val missingDefenderDuty = proof.copy(defenderDuty = None)
    val missingIngredientStory = story.copy(overloadProof = Some(missingDefenderDuty))
    val rawEngineCheck =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(overloadMove),
        engineLine = Some(EngineLine(Vector(overloadMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
        evalBefore = Some(EngineEval(100)),
        evalAfter = Some(EngineEval(120)),
        depth = Some(20),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(
      TacticOverload.write(facts, None, None, None, None, None),
      None,
      "BoardFacts alone must not create Overload ingredients"
    )
    assertEquals(TacticOverload.withEngineCheck(missingIngredientStory, rawEngineCheck), None)
    assertNoOverloadSpeech(missingIngredientStory.copy(engineCheck = Some(rawEngineCheck)), "raw engine")
    val proofFailureStory = missingIngredientStory.copy(storyProof = StoryProof.empty)
    assert(proofFailureStory.proofFailures.nonEmpty)
    assertNoOverloadSpeech(
      proofFailureStory.copy(
        proof = orderingProof.copy(boardProof = 99, lineProof = 99, clarity = 99),
        engineCheck = Some(rawEngineCheck)
      ),
      "proofFailures"
    )

  test("Stage-9 Tactic.Overload cannot replace sibling Story meanings"):
    val (story, _, _) = admittedOverload
    val siblingTactics =
      Vector(
        Tactic.RemoveGuard,
        Tactic.Hanging,
        Tactic.Loose,
        Tactic.QueenHit,
        Tactic.Fork,
        Tactic.Skewer,
        Tactic.Pin,
        Tactic.DiscoveredAttack,
        Tactic.Deflect,
        Tactic.Decoy
      )
    val siblingScenes =
      Vector(Scene.Defense, Scene.Material)

    siblingTactics.foreach: tactic =>
      assertNoOverloadSpeech(story.copy(tactic = Some(tactic)), tactic.toString)
    siblingScenes.foreach: scene =>
      assertNoOverloadSpeech(story.copy(scene = scene, tactic = None), scene.toString)
    assertNoOverloadSpeech(story.copy(plan = Some(Plan.Overload)), "Plan.Overload")

  test("OIH-2 sibling fixtures keep their own Story labels and speech keys"):
    neighborFixtures.foreach: fixture =>
      val maybeVerdict = StoryTable.choose(Vector(fixture.story)).headOption
      if fixture.writer.nonEmpty then
        val verdict = maybeVerdict.getOrElse(fail(s"${fixture.label} must keep its opened owner row"))
        assertEquals(verdict.story.scene, fixture.scene, fixture.label)
        assertEquals(verdict.story.tactic, fixture.tactic, fixture.label)
        assertEquals(verdict.story.writer, fixture.writer, fixture.label)
        assertEquals(verdict.role, Role.Lead, fixture.label)
        assertEquals(
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim.map(_.key)),
          fixture.claimKey,
          fixture.label
        )
      else
        maybeVerdict.foreach: verdict =>
          assertEquals(verdict.story.scene, fixture.scene, fixture.label)
          assertEquals(verdict.story.tactic, fixture.tactic, fixture.label)
          assertEquals(verdict.story.writer, fixture.writer, fixture.label)
          assertEquals(verdict.role, Role.Blocked, fixture.label)
          assertEquals(ExplanationPlan.fromSelected(verdict), None, fixture.label)

  test("OIH-2 Overload fixture emits no sibling Story unless separate complete proof exists"):
    val facts = board(overloadFen)
    val siblingAttempts =
      Vector(
        "RemoveGuard" ->
          TacticRemoveGuard.write(facts, Some(overloadMove), Some(defender), Some(firstDutyTarget)),
        "Defense" ->
          SceneDefense.write(facts, overloadMove, overloadMove),
        "Material" ->
          SceneMaterial.write(facts, overloadMove),
        "Hanging" ->
          TacticHanging.write(facts, overloadMove),
        "Loose" ->
          TacticLoose.write(facts, Some(overloadMove)),
        "QueenHit" ->
          TacticQueenHit.write(facts, Some(overloadMove)),
        "Fork" ->
          TacticFork.write(facts, Some(overloadMove), Some(firstDutyTarget), Some(secondDutyTarget)),
        "Skewer" ->
          TacticSkewer.write(facts, Some(overloadMove), Some(overloadMove.to), Some(firstDutyTarget), Some(secondDutyTarget)),
        "Pin" ->
          TacticPin.write(facts, Some(overloadMove), Some(overloadMove.to), Some(firstDutyTarget), Some(Square('h', 8))),
        "DiscoveredAttack" ->
          TacticDiscoveredAttack.write(facts, Some(overloadMove), Some(overloadMove.from), Some(firstDutyTarget)),
        "Deflect" ->
          None,
        "Decoy" ->
          None
      )

    siblingAttempts.foreach: (label, maybeStory) =>
      maybeStory.foreach: story =>
        assertEquals(story.writer.contains(StoryWriter.TacticOverload), false, label)
        assertEquals(story.tactic.contains(Tactic.Overload), false, label)
        assertEquals(story.overloadProof, None, label)
        assert(siblingCompleteProof(story), s"$label must speak only from its own complete proof")
        assertNoOverloadClaim(story.copy(proof = orderingProof), label)

  test("OIH-2 sibling fixtures cannot lower to overloads_defender"):
    val (overload, plan, rendered) = admittedOverload
    val overloadProof = overload.overloadProof.get

    neighborFixtures.foreach: fixture =>
      val contaminated = fixture.story.copy(proof = orderingProof, overloadProof = Some(overloadProof))
      assertNoOverloadClaim(contaminated, fixture.label)

    Vector(
      "wins material" -> "This move wins material.",
      "wins piece" -> "This move wins a piece.",
      "removes defender" -> "This move removes the defender.",
      "deflects" -> "This move deflects the defender.",
      "decoys" -> "This move decoys the defender."
    ).foreach: (label, output) =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, label)

  test("OIH-3 EngineCheck support cannot create or repair OverloadProof"):
    val facts = board(overloadFen)
    val (story, _, _) = admittedOverload
    val proofless = story.copy(overloadProof = None)
    val incomplete = story.copy(overloadProof = story.overloadProof.map(_.copy(cannotSatisfyBoth = None)))
    val supportForProofless = engineCheck(facts, proofless, EngineCheckStatus.Supports)
    val supportForIncomplete = engineCheck(facts, incomplete, EngineCheckStatus.Supports)

    assertEquals(TacticOverload.withEngineCheck(proofless, supportForProofless), None)
    assertEquals(TacticOverload.withEngineCheck(incomplete, supportForIncomplete), None)
    assertEquals(proofless.copy(engineCheck = Some(supportForProofless)).overloadProof, None)
    assertEquals(incomplete.copy(engineCheck = Some(supportForIncomplete)).overloadProof.exists(_.complete), false)
    assertNoOverloadSpeech(proofless.copy(engineCheck = Some(supportForProofless)), "proof absent + Engine support")
    assertNoOverloadSpeech(incomplete.copy(engineCheck = Some(supportForIncomplete)), "incomplete proof + Engine support")

  test("OIH-3 capped and refuted proof-backed Overload rows have no standalone text"):
    val facts = board(overloadFen)
    val (story, _, _) = admittedOverload
    val capped =
      TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Caps)).get
    val refuted =
      TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Refutes, before = 300, after = 0)).get

    assertEquals(capped.overloadProof.exists(_.complete), true)
    assertEquals(refuted.overloadProof.exists(_.complete), true)
    assertEquals(StoryTable.choose(Vector(capped)).head.engineStrengthLimited, true)
    assertNoStandaloneText(StoryTable.choose(Vector(capped)).head, "capped")
    assertEquals(StoryTable.choose(Vector(refuted)).head.role, Role.Blocked)
    assertNoStandaloneText(StoryTable.choose(Vector(refuted)).head, "refuted")

  test("OIH-3 support context and blocked Overload rows do not render independently"):
    val (story, _, _) = admittedOverload
    val duplicateVerdicts = StoryTable.choose(Vector(story, story))
    val support = duplicateVerdicts.find(_.role == Role.Support).get
    val context = StoryTable.choose(Vector(story.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(story.copy(overloadProof = None))).head

    assertNoStandaloneText(support, "support")
    assertNoStandaloneText(context, "context")
    assertNoStandaloneText(blocked, "blocked")

  test("OIH-3 raw eval and PV stay out of Overload renderer and LLM smoke inputs"):
    val facts = board(overloadFen)
    val (story, _, _) = admittedOverload
    val checked = TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Supports)).get
    val verdict = StoryTable.choose(Vector(checked)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val publicSurface = Vector(rendered.text, prompt).mkString("\n").toLowerCase(java.util.Locale.ROOT)

    assertEquals(rendered.claimKey, "overloads_defender")
    Vector("engineline", "engine line", "replyline", "reply line", "evalbefore", "evalafter", "pv", "centipawn", "+1.2", "120").foreach: token =>
      assert(!publicSurface.contains(token), s"raw eval/PV token leaked: $token")
    Vector(
      "The engine line is Bd6 and the eval rises to +1.2.",
      "The PV proves Bd6 overloads the defender.",
      "Stockfish supports this overload."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("OIH-4 one proof-backed Overload creates one selected uncapped Lead row"):
    val (story, _, _) = admittedOverload
    val verdicts = StoryTable.choose(Vector(story))
    val overloadVerdicts = verdicts.filter(_.story.tactic.contains(Tactic.Overload))
    val verdict = overloadVerdicts.head
    val plan = ExplanationPlan.fromSelected(verdict)

    assertEquals(overloadVerdicts.size, 1)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes), false)
    assertEquals(verdict.story.writer, Some(StoryWriter.TacticOverload))
    assertEquals(verdict.story.overloadProof.exists(_.complete), true)
    assertEquals(plan.flatMap(_.allowedClaim.map(_.key)), Some("overloads_defender"))
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("overloads_defender"))

  test("OIH-4 duplicate Overload source rows create no duplicate public narration"):
    val (story, _, _) = admittedOverload
    val verdicts = StoryTable.choose(Vector(story, story)).filter(_.story.tactic.contains(Tactic.Overload))
    val rendered =
      verdicts.flatMap(verdict => ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan))

    assertEquals(verdicts.size, 2)
    assertEquals(verdicts.count(_.role == Role.Lead), 1)
    assertEquals(verdicts.count(_.role == Role.Support), 1)
    assertEquals(rendered.map(_.claimKey), Vector("overloads_defender"))
    verdicts.filter(_.role != Role.Lead).foreach(assertNoStandaloneText(_, "duplicate non-Lead Overload"))

  test("OIH-4 StoryTable does not turn Overload into result wording"):
    val (story, _, _) = admittedOverload
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.story.scene, Scene.Tactic)
    assertEquals(verdict.story.tactic, Some(Tactic.Overload))
    assertEquals(verdict.story.writer, Some(StoryWriter.TacticOverload))
    assertEquals(verdict.story.captureResult, None)
    assertEquals(verdict.story.removeGuardProof, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assertEquals(plan.allowedClaim.exists(ExplanationClaim.MaterialAllowed.contains), false)
    assertEquals(plan.allowedClaim.exists(ExplanationClaim.HangingAllowed.contains), false)
    assertEquals(plan.allowedClaim.exists(ExplanationClaim.RemoveGuardAllowed.contains), false)
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("wins material"))
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("wins a piece"))
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("removes the defender"))
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("deflect"))
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("decoy"))

  test("OIH-4 result Stories keep their proof homes and do not become Overload wording"):
    val (overload, _, _) = admittedOverload
    neighborFixtures.filter(_.writer.nonEmpty).foreach: fixture =>
      val verdicts = StoryTable.choose(Vector(overload, fixture.story))
      val resultVerdict = verdicts.find(_.story.writer == fixture.writer).get
      val resultPlan = ExplanationPlan.fromSelected(resultVerdict)

      assertEquals(resultVerdict.story.scene, fixture.scene, fixture.label)
      assertEquals(resultVerdict.story.tactic, fixture.tactic, fixture.label)
      assertEquals(resultVerdict.story.writer, fixture.writer, fixture.label)
      assertEquals(resultVerdict.story.overloadProof, None, fixture.label)
      assertEquals(resultPlan.flatMap(_.allowedClaim.map(_.key)).contains("overloads_defender"), false, fixture.label)
      assertEquals(
        resultPlan.flatMap(DeterministicRenderer.fromPlan).exists(_.claimKey == "overloads_defender"),
        false,
        fixture.label
      )
      val overloadVerdict = verdicts.find(_.story.writer.contains(StoryWriter.TacticOverload)).get
      assertEquals(overloadVerdict.story.tactic, Some(Tactic.Overload), fixture.label)
      assertEquals(overloadVerdict.story.overloadProof.exists(_.complete), true, fixture.label)

  test("OIH-5 ExplanationPlan lowers only selected uncapped Lead Overload to overloads_defender"):
    val facts = board(overloadFen)
    val (story, _, _) = admittedOverload
    val selected = StoryTable.choose(Vector(story)).head
    val support = StoryTable.choose(Vector(story, story)).find(_.role == Role.Support).get
    val context = StoryTable.choose(Vector(story.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(story.copy(overloadProof = None))).head
    val capped = StoryTable.choose(Vector(TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable.choose(Vector(TacticOverload.withEngineCheck(story, engineCheck(facts, story, EngineCheckStatus.Refutes)).get)).head
    val plan = ExplanationPlan.fromSelected(selected).get

    assertEquals(selected.role, Role.Lead)
    assertEquals(selected.engineStrengthLimited, false)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Overload))
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(plan.evidenceLine, story.route)
    assertEquals(plan.supportContextLinks.isEmpty, true)
    Vector(support, context, blocked, capped, refuted).foreach(assertNoStandaloneText(_, "OIH-5 non-selected Overload"))
    neighborFixtures.filter(_.writer.nonEmpty).foreach: fixture =>
      val resultPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(fixture.story)).head)
      assertEquals(resultPlan.flatMap(_.allowedClaim.map(_.key)).contains("overloads_defender"), false, fixture.label)

  test("OIH-5 renderer emits only the bounded Overload sentence from ExplanationPlan"):
    val (_, plan, rendered) = admittedOverload
    val malformedPlans =
      Vector(
        "wrong claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.RemovesDefender)),
        "support role" -> plan.copy(role = Role.Support),
        "debug only" -> plan.copy(debugOnly = true),
        "missing forbidden wording" -> plan.copy(forbiddenWording = Vector.empty),
        "wrong tactic" -> plan.copy(tactic = Some(Tactic.RemoveGuard)),
        "missing evidence line" -> plan.copy(evidenceLine = None)
      )

    assertEquals(rendered.text, "This move overloads the defender.")
    assertEquals(rendered.claimKey, "overloads_defender")
    malformedPlans.foreach: (label, malformed) =>
      assertEquals(DeterministicRenderer.fromPlan(malformed), None, label)

  test("OIH-5 LLM smoke input is bounded and rejects forbidden Overload expansions"):
    val (_, plan, rendered) = admittedOverload
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
      assert(prompt.contains(field), field)
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("OverloadProof", "BoardFacts", "EngineCheck", "raw PV", "evalBefore", "evalAfter").foreach: rawField =>
      assert(!prompt.toLowerCase(java.util.Locale.ROOT).contains(rawField.toLowerCase(java.util.Locale.ROOT)), rawField)
    Vector(
      "wins material",
      "wins a piece",
      "wins the queen",
      "forced",
      "only move",
      "best move",
      "decisive",
      "winning",
      "no counterplay",
      "deflects",
      "decoys",
      "removes the defender",
      "cannot defend everything",
      "must choose"
    ).foreach: forbidden =>
      assert(prompt.toLowerCase(java.util.Locale.ROOT).contains(forbidden), s"prompt must forbid $forbidden")

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
      "The defender must choose."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("Stage-9 Overload does not open public routes production API or public LLM narration"):
    val routes = Files.readString(Paths.get("conf/routes"))
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))

    assert(routes.contains("POST  /api/commentary/render"))
    assert(routes.contains("POST  /internal/commentary/render-local-probe"))
    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(!controller.contains("Ok("), "commentary route tombstones must not return 200")
    assert(!controller.contains("TacticOverload"), "public route must not expose Overload writer")
    assert(!controller.contains("OverloadProof"), "public route must not expose OverloadProof")
    assert(!controller.contains("LlmNarrationSmoke"), "public route must not expose public LLM narration")
    assert(!controller.contains("env.mode.isProd"), "production API switch must remain closed")

  private def assertNoOverloadSpeech(story: Story, label: String): Unit =
    StoryTable.choose(Vector(story)).headOption.foreach: verdict =>
      assertEquals(verdict.role == Role.Lead, false, label)
      val plan = ExplanationPlan.fromSelected(verdict)
      plan.foreach: debugPlan =>
        assertEquals(debugPlan.allowedClaim, None, label)
        assertEquals(debugPlan.debugOnly, true, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertNoOverloadClaim(story: Story, label: String): Unit =
    StoryTable.choose(Vector(story)).headOption.foreach: verdict =>
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim.map(_.key)).contains("overloads_defender"), false, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan).exists(_.claimKey == "overloads_defender"), false, label)

  private def assertNoStandaloneText(verdict: Verdict, label: String): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def engineCheck(
      facts: BoardFacts,
      story: Story,
      status: EngineCheckStatus,
      before: Int = 20,
      after: Int = 20
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(overloadMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(20),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private final case class NeighborFixture(
      label: String,
      story: Story,
      scene: Scene,
      tactic: Option[Tactic],
      writer: Option[StoryWriter],
      claimKey: Option[String]
  )

  private def neighborFixtures: Vector[NeighborFixture] =
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

    val closedFacts = board(overloadFen)
    val closedStoryProof = StoryProof.fromBoardFacts(closedFacts, overloadMove)
    def closed(tactic: Tactic): Story =
      Story(
        scene = Scene.Tactic,
        tactic = Some(tactic),
        side = Side.White,
        rival = Side.Black,
        target = Some(firstDutyTarget),
        anchor = Some(defender),
        route = Some(overloadMove),
        routeSan = BoardFacts.sanFor(closedFacts, overloadMove),
        proof = orderingProof,
        storyProof = closedStoryProof
      )

    Vector(
      NeighborFixture("RemoveGuard", removeGuard, Scene.Tactic, Some(Tactic.RemoveGuard), Some(StoryWriter.TacticRemoveGuard), Some("removes_defender")),
      NeighborFixture("Defense", defense, Scene.Defense, None, Some(StoryWriter.SceneDefense), Some("defends_piece")),
      NeighborFixture("Material", material, Scene.Material, None, Some(StoryWriter.SceneMaterial), Some("material_balance_changes")),
      NeighborFixture("Hanging", hanging, Scene.Tactic, Some(Tactic.Hanging), Some(StoryWriter.TacticHanging), Some("can_win_piece")),
      NeighborFixture("Loose", loose, Scene.Tactic, Some(Tactic.Loose), Some(StoryWriter.TacticLoose), Some("attacks_loose_piece")),
      NeighborFixture("QueenHit", queenHit, Scene.Tactic, Some(Tactic.QueenHit), Some(StoryWriter.TacticQueenHit), Some("attacks_queen")),
      NeighborFixture("Fork", fork, Scene.Tactic, Some(Tactic.Fork), Some(StoryWriter.TacticFork), Some("forks_two_targets")),
      NeighborFixture("Skewer", skewer, Scene.Tactic, Some(Tactic.Skewer), Some(StoryWriter.TacticSkewer), Some("skewers_piece_to_piece")),
      NeighborFixture("Pin", pin, Scene.Tactic, Some(Tactic.Pin), Some(StoryWriter.TacticPin), Some("pins_piece")),
      NeighborFixture("DiscoveredAttack", discovered, Scene.Tactic, Some(Tactic.DiscoveredAttack), Some(StoryWriter.TacticDiscoveredAttack), Some("reveals_attack_on_piece")),
      NeighborFixture("Deflect", closed(Tactic.Deflect), Scene.Tactic, Some(Tactic.Deflect), None, None),
      NeighborFixture("Decoy", closed(Tactic.Decoy), Scene.Tactic, Some(Tactic.Decoy), None, None)
    )

  private def siblingCompleteProof(story: Story): Boolean =
    story.captureResult.exists(result => result.positiveMaterial && result.sameBoardProof && result.missingEvidence.isEmpty) ||
      story.defenseProof.exists(_.complete) ||
      story.multiTargetProof.exists(_.complete) ||
      story.lineProof.exists(_.complete) ||
      story.pinProof.exists(_.complete) ||
      story.removeGuardProof.exists(_.complete) ||
      story.skewerProof.exists(_.complete) ||
      story.queenHitProof.exists(_.complete) ||
      story.loosePieceProof.exists(_.complete)

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

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-9 FEN: $fen -> $error"), identity)

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
