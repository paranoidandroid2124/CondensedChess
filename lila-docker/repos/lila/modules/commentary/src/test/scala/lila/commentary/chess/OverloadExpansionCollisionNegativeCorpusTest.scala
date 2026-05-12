package lila.commentary.chess

class OverloadExpansionCollisionNegativeCorpusTest extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-3 duplicate collision corpus keeps neighbor meanings in their own owners"):
    val overload = admittedOverloadStory

    neighborFixtures.foreach: fixture =>
      assertOwnClaim(fixture)

      val verdicts = StoryTable.choose(Vector(overload, fixture.story))
      val neighborVerdict = verdicts.find(_.story.writer.contains(fixture.writer)).get
      val overloadVerdict = verdicts.find(_.story.writer.contains(StoryWriter.TacticOverload)).get

      assertEquals(neighborVerdict.story.scene, fixture.scene, fixture.label)
      assertEquals(neighborVerdict.story.tactic, fixture.tactic, fixture.label)
      assertEquals(neighborVerdict.story.writer, Some(fixture.writer), fixture.label)
      assertEquals(neighborVerdict.story.overloadProof, None, fixture.label)
      assertEquals(neighborProofComplete(neighborVerdict.story, fixture), true, fixture.label)
      assertNoOverloadText(neighborVerdict, fixture.label)

      assertEquals(overloadVerdict.story.tactic, Some(Tactic.Overload), fixture.label)
      assertEquals(overloadVerdict.story.writer, Some(StoryWriter.TacticOverload), fixture.label)
      assertEquals(overloadVerdict.story.overloadProof.exists(_.complete), true, fixture.label)
      assertEquals(overloadVerdict.story.captureResult, None, fixture.label)
      assertEquals(overloadVerdict.story.loosePieceProof, None, fixture.label)
      assertEquals(overloadVerdict.story.queenHitProof, None, fixture.label)
      assertEquals(overloadVerdict.story.removeGuardProof, None, fixture.label)

      ExplanationPlan.fromSelected(overloadVerdict).foreach: plan =>
        assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"), fixture.label)
        val rendered = DeterministicRenderer.fromPlan(plan).get
        assertEquals(rendered.claimKey, "overloads_defender", fixture.label)
        assertNoForbiddenText(rendered.text, fixture.label)

  test("Stage-3 selected uncapped Lead Overload produces only overloads_defender"):
    val verdict = StoryTable.choose(Vector(admittedOverloadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(rendered.claimKey, "overloads_defender")
    assertEquals(rendered.text, "This move overloads the defender.")
    assertNoForbiddenText(rendered.text, "selected Overload")

  test("Stage-3 CannotSatisfyBoth remains an internal ingredient only"):
    val story = admittedOverloadStory
    val proof = story.overloadProof.get
    val readinessOnly =
      proof.copy(
        defenderDuty = None,
        dualDefenderDuty = None,
        overloadTest = None
      )
    val verdict = StoryTable.choose(Vector(story.copy(overloadProof = Some(readinessOnly)))).head

    assertEquals(readinessOnly.cannotSatisfyBoth.exists(_.complete), true)
    assertEquals(readinessOnly.cannotSatisfyBoth.exists(_.publicClaimAllowed), false)
    assertEquals(readinessOnly.complete, false)
    assertNoStandaloneText(verdict, "CannotSatisfyBoth")

  test("Stage-3 EngineCheck statuses cannot create stronger Overload public text"):
    val facts = board(overloadFen)
    val overload = admittedOverloadStory
    val supported = TacticOverload.withEngineCheck(overload, engineCheck(facts, overload, EngineCheckStatus.Supports)).get
    val capped = TacticOverload.withEngineCheck(overload, engineCheck(facts, overload, EngineCheckStatus.Caps)).get
    val refuted = TacticOverload.withEngineCheck(overload, engineCheck(facts, overload, EngineCheckStatus.Refutes)).get

    val supportedVerdict = StoryTable.choose(Vector(supported)).head
    val supportedPlan = ExplanationPlan.fromSelected(supportedVerdict).get
    val supportedRendered = DeterministicRenderer.fromPlan(supportedPlan).get
    assertEquals(supportedVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportedPlan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(supportedRendered.claimKey, "overloads_defender")
    assertNoForbiddenText(supportedRendered.text, "EngineCheck Supports")

    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    assertEquals(cappedVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertNoStandaloneText(cappedVerdict, "EngineCheck Caps")

    val refutedVerdict = StoryTable.choose(Vector(refuted)).head
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertNoStandaloneText(refutedVerdict, "EngineCheck Refutes")

  test("Stage-3 non lead support context blocked capped and refuted Overload rows stay silent"):
    val facts = board(overloadFen)
    val overload = admittedOverloadStory
    val support = StoryTable.choose(Vector(overload, overload)).find(_.role == Role.Support).get
    val context = StoryTable.choose(Vector(overload.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(overload.copy(overloadProof = None))).head
    val capped =
      StoryTable.choose(Vector(TacticOverload.withEngineCheck(overload, engineCheck(facts, overload, EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable.choose(Vector(TacticOverload.withEngineCheck(overload, engineCheck(facts, overload, EngineCheckStatus.Refutes)).get)).head

    Vector(support, context, blocked, capped, refuted).foreach: verdict =>
      assertNoStandaloneText(verdict, verdict.toString)

  test("Stage-3 Overload LLM smoke rejects duplicate-collision wording"):
    val verdict = StoryTable.choose(Vector(admittedOverloadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    forbiddenOutputs.foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  private final case class NeighborFixture(
      label: String,
      story: Story,
      scene: Scene,
      tactic: Option[Tactic],
      writer: StoryWriter,
      claimKey: String
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

    val removeFacts = board("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1")
    val removeMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard
        .write(removeFacts, Some(removeMove), Some(Square('g', 6)), Some(Square('e', 5)))
        .get
        .copy(proof = orderingProof)

    val defenseFacts = board("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get.copy(proof = orderingProof)

    Vector(
      NeighborFixture("Material", material, Scene.Material, None, StoryWriter.SceneMaterial, "material_balance_changes"),
      NeighborFixture("Loose", loose, Scene.Tactic, Some(Tactic.Loose), StoryWriter.TacticLoose, "attacks_loose_piece"),
      NeighborFixture("Hanging", hanging, Scene.Tactic, Some(Tactic.Hanging), StoryWriter.TacticHanging, "can_win_piece"),
      NeighborFixture("QueenHit", queenHit, Scene.Tactic, Some(Tactic.QueenHit), StoryWriter.TacticQueenHit, "attacks_queen"),
      NeighborFixture("RemoveGuard", removeGuard, Scene.Tactic, Some(Tactic.RemoveGuard), StoryWriter.TacticRemoveGuard, "removes_defender"),
      NeighborFixture("Defense", defense, Scene.Defense, None, StoryWriter.SceneDefense, "defends_piece")
    )

  private def assertOwnClaim(fixture: NeighborFixture): Unit =
    val verdict = StoryTable.choose(Vector(fixture.story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.role, Role.Lead, fixture.label)
    assertEquals(verdict.story.writer, Some(fixture.writer), fixture.label)
    assertEquals(plan.allowedClaim.map(_.key), Some(fixture.claimKey), fixture.label)
    assertEquals(rendered.claimKey, fixture.claimKey, fixture.label)
    assertEquals(neighborProofComplete(verdict.story, fixture), true, fixture.label)

  private def neighborProofComplete(story: Story, fixture: NeighborFixture): Boolean =
    fixture.writer match
      case StoryWriter.SceneMaterial =>
        story.captureResult.exists(result => result.positiveMaterial && result.sameBoardProof && result.missingEvidence.isEmpty)
      case StoryWriter.TacticLoose       => story.loosePieceProof.exists(_.complete)
      case StoryWriter.TacticHanging     => story.captureResult.exists(result => result.sameBoardProof && result.missingEvidence.isEmpty)
      case StoryWriter.TacticQueenHit    => story.queenHitProof.exists(_.complete)
      case StoryWriter.TacticRemoveGuard => story.removeGuardProof.exists(_.complete)
      case StoryWriter.SceneDefense      => story.defenseProof.exists(_.complete)
      case _                             => false

  private def assertNoOverloadText(verdict: Verdict, label: String): Unit =
    ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim.map(_.key)).foreach: key =>
      assert(!key.contains("overload"), label)
    ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).foreach: rendered =>
      assert(!rendered.claimKey.contains("overload"), label)
      assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("overload"), label)

  private def assertNoStandaloneText(verdict: Verdict, label: String): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertNoForbiddenText(text: String, label: String): Unit =
    val normalized = text.toLowerCase(java.util.Locale.ROOT)
    forbiddenFragments.foreach: fragment =>
      assert(!normalized.contains(fragment), s"$label leaked '$fragment' in '$text'")

  private val forbiddenFragments =
    Vector(
      "material gain",
      "wins",
      "loose",
      "hanging",
      "queen attacked",
      "removes defender",
      "cannot satisfy both",
      "engine says",
      "best",
      "forced",
      "decisive",
      "no counterplay"
    )

  private val forbiddenOutputs =
    Vector(
      "This is a material gain.",
      "This wins.",
      "This makes the target loose.",
      "This leaves a hanging target.",
      "The queen attacked becomes the point.",
      "This removes defender coverage.",
      "The defender cannot satisfy both duties.",
      "The engine says this works.",
      "This is best.",
      "This is forced.",
      "This is decisive.",
      "There is no counterplay."
    )

  private def admittedOverloadStory: Story =
    TacticOverload
      .write(
        board(overloadFen),
        Some(overloadMove),
        Some(defender),
        Some(firstDutyTarget),
        Some(secondDutyTarget),
        Some(firstDutyTarget)
      )
      .get
      .copy(proof = orderingProof)

  private def engineCheck(
      facts: BoardFacts,
      story: Story,
      status: EngineCheckStatus
  ): EngineCheck =
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
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-3 collision FEN: $fen -> $error"), identity)

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
