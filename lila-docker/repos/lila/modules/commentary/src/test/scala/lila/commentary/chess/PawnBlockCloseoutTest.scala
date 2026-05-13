package lila.commentary.chess

class PawnBlockCloseoutTest extends munit.FunSuite:

  private def proof(value: Int): Proof =
    proofWith(value, forcing = value, conversionPrize = value, kingHeat = value)

  private def proofWith(value: Int, forcing: Int, conversionPrize: Int, kingHeat: Int): Proof =
    Proof(
      boardProof = value,
      lineProof = value,
      ownerProof = value,
      anchorProof = value,
      routeProof = value,
      persistence = value,
      immediacy = value,
      forcing = forcing,
      conversionPrize = conversionPrize,
      counterplayRisk = 20,
      kingHeat = kingHeat,
      pieceSupport = value,
      pawnSupport = value,
      sourceFit = value,
      novelty = value,
      clarity = value
    )

  test("PBFNC-4 StoryTable stability audit keeps PawnBlock deterministic and engine bounded"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val pawnBlock = ScenePawnBlock.write(facts, move).get.copy(proof = proof(90))

    def eventProof(value: Int): Proof =
      proofWith(value, forcing = 0, conversionPrize = 0, kingHeat = 0)

    def materialProof(value: Int): Proof =
      eventProof(value).copy(forcing = value, conversionPrize = value)

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialMove).get.copy(proof = materialProof(99))
    val hanging = TacticHanging.write(materialFacts, materialMove).get.copy(proof = materialProof(99))

    def shape(rows: Vector[Story]): Vector[(Scene, Option[Tactic], Role, Boolean, Option[String])] =
      StoryTable.choose(rows).map: verdict =>
        (
          verdict.story.scene,
          verdict.story.tactic,
          verdict.role,
          verdict.leadAllowed,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    val rows = Vector(pawnBlock, material, hanging)
    val forward = StoryTable.choose(rows)
    assertEquals(shape(rows.reverse), shape(rows), "input order remains stable")
    assertEquals(shape(rows.sortBy(story => (story.scene.ordinal, story.tactic.map(_.ordinal).getOrElse(-1)))), shape(rows))
    assertEquals(forward.head.story.tactic, Some(Tactic.Hanging))
    assertEquals(ExplanationPlan.fromSelected(forward.head).flatMap(_.allowedClaim).map(_.key), Some("can_win_piece"))

    val pawnBlockRoles =
      Vector(rows, rows.reverse, rows.sortBy(_.hashCode())).map: ordered =>
        StoryTable.choose(ordered).find(_.story == pawnBlock).map(_.role)
    assertEquals(pawnBlockRoles.distinct, Vector(Some(Role.Support)))

    val materialLead = StoryTable.choose(Vector(pawnBlock, material)).head
    assertEquals(materialLead.story.scene, Scene.Material)
    assertEquals(ExplanationPlan.fromSelected(materialLead).flatMap(_.allowedClaim).map(_.key), Some("material_balance_changes"))

    val hangingLead = StoryTable.choose(Vector(pawnBlock, hanging)).head
    assertEquals(hangingLead.story.tactic, Some(Tactic.Hanging))
    assertEquals(ExplanationPlan.fromSelected(hangingLead).flatMap(_.allowedClaim).map(_.key), Some("can_win_piece"))

    val invalid = pawnBlock.copy(pawnBlockProof = None)
    val invalidVerdict = StoryTable.choose(Vector(invalid)).head
    assertEquals(invalidVerdict.role, Role.Blocked)
    assertEquals(invalidVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(invalidVerdict), None)

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(pawnBlock),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val baseVerdict = StoryTable.choose(Vector(pawnBlock)).head
    val supports = ScenePawnBlock.withEngineCheck(pawnBlock, check(EngineCheckStatus.Supports, 31337, 31338)).get
    val caps = ScenePawnBlock.withEngineCheck(pawnBlock, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePawnBlock.withEngineCheck(pawnBlock, check(EngineCheckStatus.Unknown, 31337, 31338)).get
    val refuted = ScenePawnBlock.withEngineCheck(pawnBlock, check(EngineCheckStatus.Supports, 220, 20)).get

    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val refutedVerdict = StoryTable.choose(Vector(refuted)).head

    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key), Some("blocks_pawn"))
    assertEquals(
      ExplanationPlan.fromSelected(supportsVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.text),
      Some("Ne6 blocks the pawn from advancing.")
    )
    assertEquals(supportsVerdict.values.contains(31337.0), false)
    assertEquals(supportsVerdict.values.contains(31338.0), false)

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict).flatMap(DeterministicRenderer.fromPlan), None)

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(unknownVerdict.values, baseVerdict.values)
    val unknownRendered = ExplanationPlan.fromSelected(unknownVerdict).flatMap(DeterministicRenderer.fromPlan).get
    assertEquals(unknownRendered.claimKey, "blocks_pawn")
    assertEquals(unknownRendered.text, "Ne6 blocks the pawn from advancing.")

    Vector(
      "support" -> baseVerdict.copy(role = Role.Support, leadAllowed = false),
      "context" -> baseVerdict.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> baseVerdict.copy(role = Role.Blocked, leadAllowed = false),
      "capped" -> capsVerdict,
      "refuted" -> refutedVerdict
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    val publicText =
      Vector(
        ExplanationPlan.fromSelected(supportsVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.text),
        Some(unknownRendered.text)
      ).flatten.mkString("\n").toLowerCase(java.util.Locale.ROOT)
    Vector("engine says", "eval", "best move", "only move", "restrict", "pressure", "initiative").foreach: phrase =>
      assert(!publicText.contains(phrase), s"PBFNC-4 forbidden wording reached public text: $phrase")

  test("PBFNC-5 downstream boundary audit keeps PawnBlock expression bounded"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get.copy(proof = proof(90))
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.scene, Scene.PawnBlock)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("blocks_pawn"))
    assertEquals(rendered.claimKey, "blocks_pawn")
    assertEquals(
      Set(
        s"${plan.routeSan.get} blocks the pawn on ${plan.target.get.file}${plan.target.get.rank}.",
        s"${plan.routeSan.get} blocks the pawn from advancing."
      ).contains(rendered.text),
      true
    )

    Vector(
      "unselected" -> verdict.copy(selected = false),
      "support" -> verdict.copy(role = Role.Support, leadAllowed = false),
      "context" -> verdict.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> verdict.copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, invalidVerdict) =>
      assertEquals(ExplanationPlan.fromSelected(invalidVerdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(invalidVerdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val capped = StoryTable.choose(Vector(ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable.choose(Vector(ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Supports, 220, 20)).get)).head
    assertEquals(ExplanationPlan.fromSelected(capped), None, "capped")
    assertEquals(ExplanationPlan.fromSelected(refuted), None, "refuted")

    val allClaimKeys = ExplanationClaim.values.map(_.key).toSet
    val forbiddenClaimKeys =
      Vector(
        "fixed_pawn",
        "weak_pawn",
        "creates_blockade",
        "restricts_opponent",
        "creates_pressure",
        "takes_initiative",
        "stops_passed_pawn",
        "challenges_pawn",
        "captures_rival_pawn",
        "creates_passed_pawn",
        "opens_file",
        "wins_material",
        "best_move",
        "only_move",
        "forced"
      )
    forbiddenClaimKeys.foreach: key =>
      assert(plan.allowedClaim.forall(_.key != key), s"PawnBlock plan allowed forbidden key $key")
    Vector("fixed_pawn", "weak_pawn", "creates_blockade", "restricts_opponent", "creates_pressure", "takes_initiative")
      .foreach: key =>
        assert(!allClaimKeys.contains(key), s"closed broad claim key exists: $key")

    val forbiddenWordingKeys = plan.forbiddenWording.map(_.key).toSet
    Vector(
      "stops_pawn_advance",
      "challenges_pawn",
      "captures_rival_pawn",
      "creates_passed_pawn",
      "opens_file",
      "wins_material",
      "best_move",
      "only_move",
      "forced",
      "creates_blockade",
      "restricts_opponent",
      "creates_pressure",
      "takes_initiative"
    ).foreach: key =>
      assert(forbiddenWordingKeys.contains(key), s"PawnBlock plan must forbid wording key $key")

    val publicText = Vector(rendered.text, LlmNarrationSmoke.mockNarrate(plan, rendered).get).mkString("\n").toLowerCase
    Vector(
      "fixed pawn",
      "weak pawn",
      "blockade",
      "restriction",
      "pressure",
      "initiative",
      "strategy",
      "best move",
      "only move",
      "forces",
      "wins tempo"
    ).foreach: phrase =>
      assert(!publicText.contains(phrase), s"PawnBlock public text contains forbidden phrase $phrase")

    Vector(
      "Ne6 creates a fixed pawn.",
      "Ne6 creates a weak pawn.",
      "Ne6 creates a blockade.",
      "Ne6 creates a restriction.",
      "Ne6 creates pressure.",
      "Ne6 takes the initiative.",
      "Ne6 is a strategy.",
      "Ne6 is the best move.",
      "Ne6 is the only move.",
      "Ne6 forces Black back.",
      "Ne6 wins tempo."
    ).foreach: output =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, output)

  test("PBFNC-8 runtime boundary audit keeps PawnBlock raw data out of downstream surfaces"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get.copy(proof = proof(90))

    def check(status: EngineCheckStatus, before: Int, after: Int): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val verdict = StoryTable.choose(Vector(ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Supports, 31337, 31338)).get)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.values.size, Verdict.Size)
    assertEquals(Verdict.Size, 111)
    assertEquals(Verdict.Slots.End, Verdict.Size)
    assertEquals(verdict.values.contains(31337.0), false)
    assertEquals(verdict.values.contains(31338.0), false)
    assertEquals(verdict.values.exists(_.isNaN), false)

    val clearedDiagnostics =
      verdict.copy(
        proofFailures = Vector(BoardFacts.MissingEvidence("Story Proof", Vector("target"))),
        engineCheckStatus = Some(EngineCheckStatus.Refutes),
        engineStrengthLimited = true
      )
    assertEquals(verdict.values, clearedDiagnostics.values)

    assertEquals(plan.allowedClaim.map(_.key), Some("blocks_pawn"))
    assertEquals(rendered.claimKey, "blocks_pawn")
    assertEquals(rendered.strength, plan.strength.key)
    assertEquals(rendered.forbiddenCheckPassed, true)

    val rendererMethods = classOf[DeterministicRenderer.type].getDeclaredMethods.map(_.getName).toSet
    assert(rendererMethods.contains("fromPlan"))
    Vector("fromStory", "fromVerdict", "fromBoardFacts", "fromPawnBlockProof", "fromEngineCheck", "fromPv").foreach:
      method =>
        assert(!rendererMethods.contains(method), s"Renderer exposed raw-input method $method")

    val renderedMembers =
      classOf[RenderedLine].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RenderedLine].getDeclaredFields.map(_.getName).toSet
    Vector(
      "boardFacts",
      "story",
      "pawnBlockProof",
      "engineCheck",
      "engineLine",
      "rawPv",
      "proofFailures",
      "values"
    ).foreach: forbidden =>
      assert(!renderedMembers.exists(_.equalsIgnoreCase(forbidden)), s"RenderedLine exposed $forbidden")

    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
      assert(prompt.contains(field), s"LLM smoke prompt must include bounded field $field")
    Vector(
      "BoardFacts",
      "raw Story",
      "PawnBlockProof",
      "EngineCheck",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "31337",
      "31338"
    ).foreach: raw =>
      assert(!prompt.contains(raw), s"LLM smoke prompt exposed raw runtime data: $raw")

    Vector(
      "BoardFacts show the block.",
      "The raw Story proves it.",
      "PawnBlockProof is complete.",
      "EngineCheck supports Ne6.",
      "Raw PV: Ne6 Kg8.",
      "proofFailures are empty."
    ).foreach: output =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, output)

  test("PBFNC-6 forbidden wording authority audit keeps broad wording out of PawnBlock identity"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get.copy(proof = proof(90))
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.scene, Scene.PawnBlock)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnBlock))
    assertEquals(story.pawnBlockProof.exists(_.complete), true)
    assertEquals(plan.allowedClaim.map(_.key), Some("blocks_pawn"))
    assertEquals(rendered.claimKey, "blocks_pawn")

    val forbiddenClaimKeys =
      Set(
        "fixed_pawn",
        "weak_pawn",
        "blockade",
        "restriction",
        "restricts",
        "pressure",
        "initiative",
        "space_advantage",
        "strategic_clamp",
        "permanent_weakness",
        "best_move",
        "only_move",
        "forced"
      )
    val publicClaimKeys = ExplanationClaim.values.map(_.key).toSet
    forbiddenClaimKeys.foreach: key =>
      assert(!publicClaimKeys.contains(key), s"forbidden wording became public claim key: $key")

    val forbiddenAuthorityNames =
      Set(
        "FixedPawn",
        "WeakPawn",
        "Blockade",
        "Restriction",
        "Restricts",
        "Pressure",
        "Initiative",
        "SpaceAdvantage",
        "StrategicClamp",
        "PermanentWeakness",
        "BestMove",
        "OnlyMove",
        "Forced"
      )
    val writerNames = StoryWriter.values.map(_.toString).toSet
    assert(!forbiddenAuthorityNames.contains(story.scene.toString), s"PawnBlock story used forbidden label: ${story.scene}")
    assert(
      !forbiddenAuthorityNames.exists(name => story.writer.exists(_.toString.contains(name))),
      s"PawnBlock writer used forbidden wording: ${story.writer}"
    )
    forbiddenAuthorityNames.foreach: name =>
      assert(!writerNames.contains(s"Scene$name"), s"forbidden wording became writer name: Scene$name")
      assert(!writerNames.contains(s"Story$name"), s"forbidden wording became writer name: Story$name")
      assert(!writerNames.contains(s"${name}Writer"), s"forbidden wording became writer name: ${name}Writer")

    val publicText = Vector(rendered.text, LlmNarrationSmoke.mockNarrate(plan, rendered).get).mkString("\n").toLowerCase
    Vector(
      "fixed pawn",
      "weak pawn",
      "blockade",
      "restriction",
      "restricts",
      "pressure",
      "initiative",
      "space advantage",
      "strategic clamp",
      "permanent weakness",
      "best move",
      "only move",
      "forced"
    ).foreach: phrase =>
      assert(!publicText.contains(phrase), s"forbidden wording reached public PawnBlock output: $phrase")

    Vector(
      "Ne6 creates a fixed pawn.",
      "Ne6 creates a weak pawn.",
      "Ne6 creates a blockade.",
      "Ne6 creates a restriction.",
      "Ne6 restricts Black.",
      "Ne6 creates pressure.",
      "Ne6 takes the initiative.",
      "Ne6 gains a space advantage.",
      "Ne6 creates a strategic clamp.",
      "Ne6 creates a permanent weakness.",
      "Ne6 is the best move.",
      "Ne6 is the only move.",
      "Ne6 is forced."
    ).foreach: output =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, output)
