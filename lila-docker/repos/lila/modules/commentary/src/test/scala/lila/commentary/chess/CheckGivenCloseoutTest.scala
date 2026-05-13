package lila.commentary.chess

class CheckGivenCloseoutTest extends munit.FunSuite:

  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))
  private val replyMove = Line(Square('e', 8), Square('f', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(checkingFen).toOption.get

  private def story: Story =
    SceneCheckGiven.write(facts, checkingMove).get

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(checkingMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("CheckGiven closeout keeps proof Story writer and speech key separated"):
    val proof = CheckGivenProof.fromBoardFacts(facts, checkingMove)
    val row = story
    val verdict = StoryTable.choose(Vector(row)).head
    val checkPlan = ExplanationPlan.fromSelected(verdict).get
    val line = DeterministicRenderer.fromPlan(checkPlan).get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(row.scene, Scene.CheckGiven)
    assertEquals(row.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(row.checkGivenProof, Some(proof))
    assertEquals(row.side, proof.checkingSide)
    assertEquals(row.rival, proof.rivalSide)
    assertEquals(row.target, proof.rivalKingSquareAfter)
    assertEquals(row.anchor, proof.originSquare)
    assertEquals(row.route, proof.checkMove)
    assertEquals(row.routeSan, Some("Re2+"))

    assertEquals(checkPlan.scene, Scene.CheckGiven)
    assertEquals(checkPlan.tactic, None)
    assertEquals(checkPlan.allowedClaim, Some(ExplanationClaim.GivesCheck))
    assertEquals(checkPlan.allowedClaim.map(_.key), Some("gives_check"))
    assertEquals(line.claimKey, "gives_check")
    assertEquals(line.text, "Re2+ gives check.")
    assertEquals(LlmNarrationSmoke.mockNarrate(checkPlan, line), Some(line.text))

    assertEquals(ExplanationClaim.CheckGivenAllowed, Vector(ExplanationClaim.GivesCheck))
    Vector(
      ExplanationClaim.MaterialAllowed,
      ExplanationClaim.HangingAllowed,
      ExplanationClaim.ForkAllowed,
      ExplanationClaim.DefenseAllowed,
      ExplanationClaim.DiscoveredAttackAllowed,
      ExplanationClaim.PinAllowed,
      ExplanationClaim.RemoveGuardAllowed,
      ExplanationClaim.SkewerAllowed,
      ExplanationClaim.PawnAdvanceAllowed,
      ExplanationClaim.PawnStopAllowed,
      ExplanationClaim.PromotionThreatAllowed,
      ExplanationClaim.PromotionAllowed,
      ExplanationClaim.PawnBreakAllowed,
      ExplanationClaim.PawnCaptureAllowed,
      ExplanationClaim.PassedPawnCreatedAllowed,
      ExplanationClaim.FileOpenedAllowed,
      ExplanationClaim.PawnBlockAllowed
    ).foreach: siblingKeys =>
      assert(!siblingKeys.map(_.key).contains("gives_check"), s"gives_check leaked to sibling key home $siblingKeys")

    Vector(
      row.captureResult,
      row.threatProof,
      row.defenseProof,
      row.multiTargetProof,
      row.lineProof,
      row.pinProof,
      row.removeGuardProof,
      row.skewerProof,
      row.pawnAdvanceProof,
      row.pawnStopProof,
      row.promotionThreatProof,
      row.promotionProof,
      row.pawnBreakProof,
      row.pawnCaptureProof,
      row.passedPawnCreatedProof,
      row.fileOpenedProof,
      row.pawnBlockProof
    ).foreach: sidecar =>
      assertEquals(sidecar, None)

  test("CheckGiven closeout keeps closed meanings out of labels keys and public text"):
    val checkPlan = plan
    val line = rendered
    val publicClaimKeys = ExplanationClaim.values.map(_.key).toSet
    val checkGivenRow = story
    val writerNames = StoryWriter.values.map(_.toString).toSet

    Vector(
      "mate_threat",
      "checkmate",
      "king_safety",
      "king_unsafe",
      "attacks_king",
      "creates_attack",
      "creates_pressure",
      "takes_initiative",
      "forces_reply",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay"
    ).foreach: key =>
      assert(!publicClaimKeys.contains(key), s"closed CheckGiven-adjacent claim key exists: $key")

    Vector(
      "MateThreat",
      "Checkmate",
      "KingSafety",
      "UnsafeKing",
      "KingAttack",
      "Attack",
      "Pressure",
      "Initiative",
      "Forced",
      "BestMove",
      "OnlyMove",
      "Winning",
      "Decisive",
      "NoCounterplay"
    ).foreach: name =>
      assert(!checkGivenRow.scene.toString.contains(name), s"CheckGiven row used closed label $name")
      assert(!checkGivenRow.writer.exists(_.toString.contains(name)), s"CheckGiven writer used closed name $name")
      if !Set("Checkmate", "MateThreat").contains(name) then
        assert(!writerNames.contains(s"Scene$name"), s"closed writer Scene$name exists")

    val publicText = Vector(line.text, LlmNarrationSmoke.mockNarrate(checkPlan, line).get).mkString("\n").toLowerCase
    Vector(
      "mate",
      "checkmate",
      "unsafe",
      "king safety",
      "attack",
      "pressure",
      "initiative",
      "forces",
      "best move",
      "only move",
      "winning",
      "decisive",
      "no counterplay",
      "engine says"
    ).foreach: phrase =>
      assert(!publicText.contains(phrase), s"CheckGiven public text is stronger than gives_check: $phrase")

    Vector(
      "Re2+ threatens mate.",
      "Re2+ is checkmate.",
      "Re2+ shows the king is unsafe.",
      "Re2+ starts an attack.",
      "Re2+ creates pressure.",
      "Re2+ takes the initiative.",
      "Re2+ forces a reply.",
      "Re2+ is the best move.",
      "Re2+ is the only move.",
      "Re2+ is winning.",
      "Re2+ is decisive.",
      "Re2+ leaves no counterplay.",
      "The engine says Re2+ gives check."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(checkPlan, line, output).accepted, false, output)

  test("CheckGiven closeout keeps engine diagnostics and non-Lead rows silent"):
    val row = story
    val baseVerdict = StoryTable.choose(Vector(row)).head
    val supports =
      StoryTable.choose(Vector(SceneCheckGiven.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Supports, 31337, 31338)).get)).head
    val capped = StoryTable.choose(Vector(SceneCheckGiven.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Caps)).get)).head
    val unknown =
      StoryTable.choose(Vector(SceneCheckGiven.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Unknown, 31337, 31338)).get)).head
    val refuted =
      StoryTable.choose(Vector(SceneCheckGiven.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Supports, 250, 25)).get)).head

    assertEquals(supports.role, Role.Lead)
    assertEquals(supports.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supports.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supports).flatMap(_.allowedClaim).map(_.key), Some("gives_check"))
    assertEquals(ExplanationPlan.fromSelected(supports).flatMap(DeterministicRenderer.fromPlan).map(_.text), Some("Re2+ gives check."))
    assertEquals(supports.values.contains(31337.0), false)
    assertEquals(supports.values.contains(31338.0), false)

    assertEquals(unknown.role, Role.Lead)
    assertEquals(unknown.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknown.values, baseVerdict.values)
    assertEquals(ExplanationPlan.fromSelected(unknown), None)

    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    Vector(
      "support" -> baseVerdict.copy(role = Role.Support, leadAllowed = false),
      "context" -> baseVerdict.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> baseVerdict.copy(role = Role.Blocked, leadAllowed = false),
      "capped" -> capped,
      "refuted" -> refuted
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("CheckGiven closeout keeps raw proof engine and public API surfaces closed"):
    val checkPlan = plan
    val line = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(checkPlan, line).get

    Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
      assert(prompt.contains(field), s"LLM smoke prompt must include bounded field $field")
    Vector(
      "raw Story",
      "CheckGivenProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "proofFailures",
      "source row",
      "31337",
      "31338"
    ).foreach: raw =>
      assert(!prompt.contains(raw), s"LLM smoke prompt exposed raw runtime data: $raw")

    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val smokeMethods = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    Vector("fromStory", "fromVerdict", "fromBoardFacts", "fromCheckGivenProof", "fromEngineCheck", "fromPv").foreach:
      method =>
        assert(!rendererMethods.contains(method), s"Renderer exposed raw-input method $method")
    Vector("fromStory", "fromCheckGivenProof", "fromBoardFacts", "fromEngineCheck", "callApi", "productionApi").foreach:
      method =>
        assert(!smokeMethods.contains(method), s"LLM smoke exposed public/raw-input method $method")

    val renderedMembers =
      classOf[RenderedLine].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RenderedLine].getDeclaredFields.map(_.getName).toSet
    Vector("story", "checkGivenProof", "boardFacts", "engineCheck", "engineLine", "rawPv", "proofFailures", "sourceRows").foreach:
      member =>
        assert(!renderedMembers.exists(_.equalsIgnoreCase(member)), s"RenderedLine exposed $member")
