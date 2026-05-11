package lila.commentary.chess

class CheckEscapedCloseoutTest extends munit.FunSuite:

  private val escapeFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val escapeMove = Line(Square('e', 1), Square('f', 1))
  private val replyMove = Line(Square('a', 8), Square('b', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(escapeFen).toOption.get

  private def story: Story =
    SceneCheckEscaped.write(facts, escapeMove).get

  private def plan: ExplanationPlan =
    ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

  private def rendered: RenderedLine =
    DeterministicRenderer.fromPlan(plan).get

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(escapeMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("CheckEscaped closeout keeps proof Story writer and speech key separated"):
    val proof = CheckEscapedProof.fromBoardFacts(facts, escapeMove)
    val row = story
    val verdict = StoryTable.choose(Vector(row)).head
    val checkPlan = ExplanationPlan.fromSelected(verdict).get
    val line = DeterministicRenderer.fromPlan(checkPlan).get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(row.scene, Scene.CheckEscaped)
    assertEquals(row.writer, Some(StoryWriter.SceneCheckEscaped))
    assertEquals(row.checkEscapedProof, Some(proof))
    assertEquals(row.side, proof.escapingSide)
    assertEquals(row.rival, proof.rivalSide)
    assertEquals(row.target, proof.afterKingSquare)
    assertEquals(row.anchor, proof.beforeKingSquare)
    assertEquals(row.route, proof.escapeMove)
    assertEquals(row.routeSan, Some("Kf1"))

    assertEquals(checkPlan.scene, Scene.CheckEscaped)
    assertEquals(checkPlan.tactic, None)
    assertEquals(checkPlan.allowedClaim, Some(ExplanationClaim.EscapesCheck))
    assertEquals(checkPlan.allowedClaim.map(_.key), Some("escapes_check"))
    assertEquals(line.claimKey, "escapes_check")
    assertEquals(line.text, "Kf1 gets out of check.")
    assertEquals(LlmNarrationSmoke.mockNarrate(checkPlan, line), Some(line.text))

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
      ExplanationClaim.PawnBlockAllowed,
      ExplanationClaim.CheckGivenAllowed
    ).foreach: siblingKeys =>
      assert(!siblingKeys.map(_.key).contains("escapes_check"), s"escapes_check leaked to sibling key home $siblingKeys")

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
      row.pawnBlockProof,
      row.checkGivenProof
    ).foreach: sidecar =>
      assertEquals(sidecar, None)

  test("CheckEscaped closeout keeps closed meanings out of labels keys and public text"):
    val checkPlan = plan
    val line = rendered
    val publicClaimKeys = ExplanationClaim.values.map(_.key).toSet
    val checkEscapedRow = story
    val writerNames = StoryWriter.values.map(_.toString).toSet

    Vector(
      "king_escapes_check",
      "blocks_check",
      "captures_checker",
      "avoids_mate",
      "safe_king",
      "defense_success",
      "refutes_attack",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay"
    ).foreach: key =>
      assert(!publicClaimKeys.contains(key), s"closed CheckEscaped-adjacent claim key exists: $key")

    Vector(
      "KingMovedOutOfCheck",
      "CheckBlocked",
      "CheckingPieceCaptured",
      "Checkmate",
      "AvoidsMate",
      "KingSafety",
      "SafeKing",
      "DefenseSuccess",
      "RefutesAttack",
      "Forced",
      "BestMove",
      "OnlyMove",
      "Winning",
      "Decisive",
      "NoCounterplay"
    ).foreach: name =>
      assert(!checkEscapedRow.scene.toString.contains(name), s"CheckEscaped row used closed label $name")
      assert(!checkEscapedRow.writer.exists(_.toString.contains(name)), s"CheckEscaped writer used closed name $name")
      if name != "Checkmate" then assert(!writerNames.contains(s"Scene$name"), s"closed writer Scene$name exists")

    val publicText = Vector(line.text, LlmNarrationSmoke.mockNarrate(checkPlan, line).get).mkString("\n").toLowerCase
    Vector(
      "moves the king",
      "blocks check",
      "captures the checker",
      "checkmate",
      "avoids mate",
      "king is safe",
      "king safety",
      "defends",
      "refutes",
      "forced",
      "best move",
      "only move",
      "winning",
      "decisive",
      "no counterplay",
      "engine says"
    ).foreach: phrase =>
      assert(!publicText.contains(phrase), s"CheckEscaped public text is stronger than escapes_check: $phrase")

    Vector(
      "Kf1 moves the king out of check.",
      "Kf1 blocks the check.",
      "Kf1 captures the checker.",
      "Kf1 avoids mate.",
      "Kf1 makes the king safe.",
      "Kf1 is the best defense.",
      "Kf1 is forced.",
      "Kf1 is the only move.",
      "Kf1 is winning.",
      "Kf1 is decisive.",
      "Kf1 leaves no counterplay.",
      "The engine says Kf1 gets out of check."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(checkPlan, line, output).accepted, false, output)

  test("CheckEscaped closeout keeps engine diagnostics and non-Lead rows silent"):
    val row = story
    val baseVerdict = StoryTable.choose(Vector(row)).head
    val supports =
      StoryTable.choose(Vector(SceneCheckEscaped.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Supports, 31337, 31338)).get)).head
    val capped = StoryTable.choose(Vector(SceneCheckEscaped.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Caps)).get)).head
    val unknown =
      StoryTable.choose(Vector(SceneCheckEscaped.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Unknown, 31337, 31338)).get)).head
    val refuted =
      StoryTable.choose(Vector(SceneCheckEscaped.withEngineCheck(row, engineCheck(row, EngineCheckStatus.Supports, 250, 25)).get)).head

    assertEquals(supports.role, Role.Lead)
    assertEquals(supports.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supports.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supports).flatMap(_.allowedClaim).map(_.key), Some("escapes_check"))
    assertEquals(ExplanationPlan.fromSelected(supports).flatMap(DeterministicRenderer.fromPlan).map(_.text), Some("Kf1 gets out of check."))
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

  test("CheckEscaped closeout keeps raw proof engine and public API surfaces closed"):
    val checkPlan = plan
    val line = rendered
    val prompt = LlmNarrationSmoke.codexCliPrompt(checkPlan, line).get

    Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
      assert(prompt.contains(field), s"LLM smoke prompt must include bounded field $field")
    Vector(
      "raw Story",
      "CheckEscapedProof",
      "CheckGivenProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "proofFailures",
      "source row",
      "escape method diagnostics",
      "31337",
      "31338"
    ).foreach: raw =>
      assert(!prompt.contains(raw), s"LLM smoke prompt exposed raw runtime data: $raw")

    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val smokeMethods = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    Vector("fromStory", "fromVerdict", "fromBoardFacts", "fromCheckEscapedProof", "fromEngineCheck", "fromPv").foreach:
      method =>
        assert(!rendererMethods.contains(method), s"Renderer exposed raw-input method $method")
    Vector("fromStory", "fromCheckEscapedProof", "fromBoardFacts", "fromEngineCheck", "callApi", "productionApi").foreach:
      method =>
        assert(!smokeMethods.contains(method), s"LLM smoke exposed public/raw-input method $method")

    val renderedMembers =
      classOf[RenderedLine].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RenderedLine].getDeclaredFields.map(_.getName).toSet
    Vector(
      "story",
      "checkEscapedProof",
      "checkGivenProof",
      "boardFacts",
      "engineCheck",
      "engineLine",
      "rawPv",
      "proofFailures",
      "sourceRows",
      "escapeMethodDiagnostics"
    ).foreach: member =>
      assert(!renderedMembers.exists(_.equalsIgnoreCase(member)), s"RenderedLine exposed $member")
