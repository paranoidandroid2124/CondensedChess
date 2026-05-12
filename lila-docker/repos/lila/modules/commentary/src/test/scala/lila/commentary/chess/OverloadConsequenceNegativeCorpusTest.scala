package lila.commentary.chess

class OverloadConsequenceNegativeCorpusTest extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-3 OCBA keeps complete Overload next to Material without borrowing material wording"):
    val overload = admittedOverloadStory
    val material = materialStory

    assertOwnClaim(material, "Material", "material_balance_changes")
    assertOverloadBesideSiblingHasOnlyOverloadWording(overload, material, "Material")

  test("Stage-3 OCBA keeps complete Overload next to Loose without borrowing loose-piece wording"):
    val overload = admittedOverloadStory
    val loose = looseStory

    assertOwnClaim(loose, "Loose", "attacks_loose_piece")
    assertOverloadBesideSiblingHasOnlyOverloadWording(overload, loose, "Loose")

  test("Stage-3 OCBA keeps complete Overload next to RemoveGuard without borrowing removed-guard wording"):
    val overload = admittedOverloadStory
    val removeGuard = removeGuardStory

    assertOwnClaim(removeGuard, "RemoveGuard", "removes_defender")
    assertOverloadBesideSiblingHasOnlyOverloadWording(overload, removeGuard, "RemoveGuard")

  test("Stage-3 OCBA keeps CannotSatisfyBoth readiness out of public cannot-save-both text"):
    val story = admittedOverloadStory
    val readinessOnly =
      story.overloadProof.get.copy(
        defenderDuty = None,
        dualDefenderDuty = None,
        overloadTest = None
      )
    val verdict = StoryTable.choose(Vector(story.copy(overloadProof = Some(readinessOnly)))).head

    assertEquals(readinessOnly.cannotSatisfyBoth.exists(_.complete), true)
    assertEquals(readinessOnly.complete, false)
    assertNoStandaloneText(verdict, "CannotSatisfyBoth readiness")

    val (_, plan, rendered) = admittedOverload
    Vector(
      "The defender cannot save both targets.",
      "The defender cannot satisfy both duties.",
      "The defender must choose which piece to save."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("Stage-3 OCBA EngineCheck Supports Overload adds no stronger consequence claim"):
    val facts = board(overloadFen)
    val overload = admittedOverloadStory
    val supported = TacticOverload.withEngineCheck(
      overload,
      engineCheck(facts, overload, EngineCheckStatus.Supports)
    ).get
    val verdict = StoryTable.choose(Vector(supported)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(rendered.claimKey, "overloads_defender")
    assertRenderedHasNoConsequenceText(rendered, "EngineCheck Supports")
    Vector(
      "This overload wins material.",
      "This overload removes the defender.",
      "The defender cannot save both targets.",
      "This is a forced win."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("Stage-3 OCBA EngineCheck Caps Overload produces no standalone consequence text"):
    val facts = board(overloadFen)
    val overload = admittedOverloadStory
    val capped = TacticOverload.withEngineCheck(
      overload,
      engineCheck(facts, overload, EngineCheckStatus.Caps)
    ).get
    val verdict = StoryTable.choose(Vector(capped)).head

    assertEquals(verdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(verdict.engineStrengthLimited, true)
    assertNoStandaloneText(verdict, "EngineCheck Caps")

  test("Stage-3 OCBA EngineCheck Refutes Overload produces no Overload text"):
    val facts = board(overloadFen)
    val overload = admittedOverloadStory
    val refuted = TacticOverload.withEngineCheck(
      overload,
      engineCheck(facts, overload, EngineCheckStatus.Refutes, before = 200, after = 0)
    ).get
    val verdict = StoryTable.choose(Vector(refuted)).head

    assertEquals(verdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(verdict.role, Role.Blocked)
    assertNoStandaloneText(verdict, "EngineCheck Refutes")

  test("Stage-4 OCBA downstream boundary keeps Overload speech bounded after selected Lead Verdict"):
    val (story, plan, rendered) = admittedOverload
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))
    assertEquals(rendered.claimKey, "overloads_defender")
    assertEquals(rendered.text, "This move overloads the defender.")
    assertRenderedHasNoConsequenceText(rendered, "Stage-4 renderer")

    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
      assert(prompt.contains(field), field)
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("BoardFacts", "OverloadProof", "EngineCheck", "evalBefore", "evalAfter", "raw PV").foreach: rawField =>
      assert(!prompt.toLowerCase(java.util.Locale.ROOT).contains(rawField.toLowerCase(java.util.Locale.ROOT)), rawField)

    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    forbiddenDownstreamOutputs.foreach: output =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, output)

  private def assertOverloadBesideSiblingHasOnlyOverloadWording(
      overload: Story,
      sibling: Story,
      label: String
  ): Unit =
    val verdicts = StoryTable.choose(Vector(overload, sibling))
    val overloadVerdict = verdicts.find(_.story.writer.contains(StoryWriter.TacticOverload)).get
    val siblingVerdict = verdicts.find(_.story.writer == sibling.writer).get

    assertEquals(siblingVerdict.story.writer, sibling.writer, label)
    assertEquals(siblingOwnsCompleteProof(siblingVerdict.story), true, label)
    ExplanationPlan.fromSelected(siblingVerdict).flatMap(DeterministicRenderer.fromPlan).foreach: rendered =>
      assert(!rendered.claimKey.contains("overload"), label)

    ExplanationPlan.fromSelected(overloadVerdict).foreach: plan =>
      assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"), label)
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(rendered.claimKey, "overloads_defender", label)
      assertRenderedHasNoConsequenceText(rendered, label)

  private def assertOwnClaim(story: Story, label: String, claimKey: String): Unit =
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.role, Role.Lead, label)
    assertEquals(plan.allowedClaim.map(_.key), Some(claimKey), label)
    assertEquals(rendered.claimKey, claimKey, label)
    assertEquals(siblingOwnsCompleteProof(story), true, label)

  private def assertNoStandaloneText(verdict: Verdict, label: String): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertRenderedHasNoConsequenceText(rendered: RenderedLine, label: String): Unit =
    val normalized = rendered.text.toLowerCase(java.util.Locale.ROOT)
    consequencePhrases.foreach: phrase =>
      assert(!normalized.contains(phrase), s"$label leaked '$phrase' in '${rendered.text}'")

  private def siblingOwnsCompleteProof(story: Story): Boolean =
    story.captureResult.exists(result => result.positiveMaterial && result.sameBoardProof && result.missingEvidence.isEmpty) ||
      story.loosePieceProof.exists(_.complete) ||
      story.removeGuardProof.exists(_.complete)

  private val consequencePhrases =
    Vector(
      "wins material",
      "win material",
      "gains material",
      "gain material",
      "loose piece",
      "hanging",
      "removes the defender",
      "removed guard",
      "cannot save both",
      "cannot satisfy both",
      "must choose",
      "forced",
      "only move",
      "best move",
      "decisive",
      "winning",
      "no counterplay"
    )

  private val forbiddenDownstreamOutputs =
    Vector(
      "This overload wins material.",
      "This overload wins the game.",
      "This overload is a forced choice.",
      "This is the best defensive move.",
      "This is the only defensive move.",
      "The defender has to choose.",
      "The defender must choose.",
      "The defender cannot save both targets.",
      "This overload is decisive.",
      "This overload is winning.",
      "There is no counterplay."
    )

  private def materialStory: Story =
    val facts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    val move = Line(Square('d', 4), Square('e', 5))
    SceneMaterial.write(facts, move).get.copy(proof = orderingProof)

  private def looseStory: Story =
    val facts = board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
    val move = Line(Square('d', 2), Square('h', 2))
    TacticLoose.write(facts, Some(move)).get.copy(proof = orderingProof)

  private def removeGuardStory: Story =
    val facts = board("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1")
    val move = Line(Square('d', 3), Square('e', 5))
    TacticRemoveGuard.write(facts, Some(move), Some(Square('g', 6)), Some(Square('e', 5))).get.copy(proof = orderingProof)

  private def admittedOverload: (Story, ExplanationPlan, RenderedLine) =
    val story = admittedOverloadStory
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    (story, plan, rendered)

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

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-3 FEN: $fen -> $error"), identity)

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
