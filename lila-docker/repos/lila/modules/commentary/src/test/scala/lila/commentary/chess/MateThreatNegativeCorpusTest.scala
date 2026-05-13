package lila.commentary.chess

class MateThreatNegativeCorpusTest extends munit.FunSuite:

  private val mateThreatFen = "7k/8/6K1/8/8/8/5Q2/8 w - - 0 1"
  private val threatMove = Line(Square('g', 6), Square('h', 6))
  private val immediateMateMove = Line(Square('f', 2), Square('f', 8))
  private val replyMove = Line(Square('h', 8), Square('g', 8))
  private val promotionThreatFen = "k7/8/4P3/8/8/8/8/4K3 w - - 0 1"
  private val promotionThreatMove = Line(Square('e', 6), Square('e', 7))

  private def facts: BoardFacts =
    BoardFacts.fromFen(mateThreatFen).toOption.get

  private def promotionThreatFacts: BoardFacts =
    BoardFacts.fromFen(promotionThreatFen).toOption.get

  private def mateThreatStory: Story =
    SceneMateThreat.write(facts, threatMove).get

  private def immediateCheckmateStory: Story =
    SceneCheckmate.write(facts, immediateMateMove).get

  private def promotionThreatStory: Story =
    ScenePromotionThreat.write(promotionThreatFacts, promotionThreatMove).get

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(threatMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def selected(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def assertSilent(label: String, verdict: Verdict): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan.flatMap(_.allowedClaim).map(_.key), None, label)
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertNoMateThreatClaim(label: String, verdict: Verdict): Unit =
    assertNotEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key),
      Some("threatens_mate_next"),
      label
    )

  test("MateThreatProof does not become sibling public meanings"):
    val story = mateThreatStory
    val forgedRows = Vector(
      "Checkmate" -> story.copy(scene = Scene.Checkmate, writer = Some(StoryWriter.SceneCheckmate)),
      "CheckGiven" -> story.copy(scene = Scene.CheckGiven, writer = Some(StoryWriter.SceneCheckGiven)),
      "CheckEscaped" -> story.copy(scene = Scene.CheckEscaped, writer = Some(StoryWriter.SceneCheckEscaped)),
      "Stalemate" -> story.copy(scene = Scene.Stalemate, writer = Some(StoryWriter.SceneStalemate)),
      "Defense" -> story.copy(scene = Scene.Defense, writer = Some(StoryWriter.SceneDefense)),
      "Pin" -> story.copy(scene = Scene.Tactic, tactic = Some(Tactic.Pin), writer = Some(StoryWriter.TacticPin)),
      "Skewer" -> story.copy(scene = Scene.Tactic, tactic = Some(Tactic.Skewer), writer = Some(StoryWriter.TacticSkewer)),
      "DiscoveredAttack" -> story.copy(
        scene = Scene.Tactic,
        tactic = Some(Tactic.DiscoveredAttack),
        writer = Some(StoryWriter.TacticDiscoveredAttack)
      ),
      "QueenHit" -> story.copy(scene = Scene.Tactic, tactic = Some(Tactic.QueenHit), writer = Some(StoryWriter.TacticQueenHit)),
      "RookHit" -> story.copy(scene = Scene.Tactic, tactic = Some(Tactic.RookHit), writer = Some(StoryWriter.TacticRookHit)),
      "Material" -> story.copy(scene = Scene.Material, writer = Some(StoryWriter.SceneMaterial)),
      "PromotionThreat" -> story.copy(scene = Scene.PromotionThreat, writer = Some(StoryWriter.ScenePromotionThreat))
    )

    forgedRows.foreach: (label, row) =>
      val verdict = selected(row)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertSilent(label, verdict)

  test("actual checkmate owns checkmate now and not MateThreat for the same route"):
    val checkmate = immediateCheckmateStory
    val checkmateVerdict = selected(checkmate)
    val checkmatePlan = ExplanationPlan.fromSelected(checkmateVerdict).get
    val forgedThreatNow =
      mateThreatStory.copy(
        route = Some(immediateMateMove),
        routeSan = Some("Qf8#"),
        mateThreatProof = Some(MateThreatProof.fromBoardFacts(facts, immediateMateMove))
      )
    val combined = StoryTable.choose(Vector(checkmate, forgedThreatNow))

    assertEquals(SceneMateThreat.write(facts, immediateMateMove), None)
    assertEquals(checkmate.scene, Scene.Checkmate)
    assertEquals(checkmate.checkmateProof.exists(_.complete), true)
    assertEquals(checkmate.mateThreatProof, None)
    assertEquals(checkmateVerdict.role, Role.Lead)
    assertEquals(checkmatePlan.allowedClaim.map(_.key), Some("checkmates"))
    assertNoMateThreatClaim("CheckmateProof must not lower to MateThreat", checkmateVerdict)
    assertEquals(combined.find(_.story == checkmate).map(_.role), Some(Role.Lead))
    assertSilent("same-route forged MateThreat now", combined.find(_.story == forgedThreatNow).get)

  test("MateThreatProof cannot lower to checkmates or gives_check"):
    val story = mateThreatStory
    val forgedCheckmate =
      story.copy(scene = Scene.Checkmate, writer = Some(StoryWriter.SceneCheckmate), checkmateProof = None)
    val forgedCheckGiven =
      story.copy(scene = Scene.CheckGiven, writer = Some(StoryWriter.SceneCheckGiven), checkGivenProof = None)

    assertSilent("MateThreatProof cannot claim checkmates", selected(forgedCheckmate))
    assertSilent("MateThreatProof cannot claim gives_check", selected(forgedCheckGiven))

  test("CheckmateProof cannot lower to threatens_mate_next"):
    val checkmate = immediateCheckmateStory
    val forgedMateThreat =
      checkmate.copy(scene = Scene.MateThreat, writer = Some(StoryWriter.SceneMateThreat), mateThreatProof = None)
    val verdict = selected(forgedMateThreat)

    assertEquals(verdict.role, Role.Blocked)
    assertSilent("CheckmateProof cannot claim threatens_mate_next", verdict)

  test("PromotionThreat remains only the promotion-next claim"):
    val promotionThreat = promotionThreatStory
    val verdict = selected(promotionThreat)
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forgedMateThreat =
      promotionThreat.copy(scene = Scene.MateThreat, writer = Some(StoryWriter.SceneMateThreat), mateThreatProof = None)
    val forgedPromotionThreat =
      mateThreatStory.copy(scene = Scene.PromotionThreat, writer = Some(StoryWriter.ScenePromotionThreat), promotionThreatProof = None)

    assertEquals(plan.allowedClaim.map(_.key), Some("threatens_promotion_next"))
    assertNoMateThreatClaim("PromotionThreat must not claim mate threat", verdict)
    assertSilent("PromotionThreatProof cannot become MateThreat", selected(forgedMateThreat))
    assertSilent("MateThreatProof cannot become PromotionThreat", selected(forgedPromotionThreat))

  test("EngineCheck and non-lead MateThreat rows stay silent"):
    val story = mateThreatStory
    val support = SceneMateThreat.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Supports)).get
    val capped = SceneMateThreat.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get
    val refute = engineCheck(story, EngineCheckStatus.Supports, before = 220, after = 20)
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(threatMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(400)),
      evalAfter = Some(EngineEval(400)),
      depth = Some(14),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val supportVerdict =
      StoryTable.choose(Vector(immediateCheckmateStory, support)).find(_.story == support).get
    val contextVerdict =
      Verdict(story = story, rank = 2, leadAllowed = false, strength = 0, role = Role.Context, selected = true)
    val blocked = story.copy(mateThreatProof = None)
    val refuted = story.copy(engineCheck = Some(refute))
    val engineOnlyStory =
      story.copy(writer = Some(StoryWriter.SceneMateThreat), storyProof = StoryProof.empty, mateThreatProof = None, engineCheck = Some(engineOnly))

    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
    assertEquals(supportVerdict.role, Role.Support)
    assertSilent("Support MateThreat", supportVerdict)
    assertSilent("Context MateThreat", contextVerdict)
    assertSilent("Blocked MateThreat", selected(blocked))
    assertEquals(selected(capped).engineStrengthLimited, true)
    assertSilent("capped MateThreat", selected(capped))
    assertEquals(refute.status, EngineCheckStatus.Refutes)
    assertEquals(SceneMateThreat.withEngineCheck(story, refute).flatMap(_.engineCheck).map(_.status), Some(EngineCheckStatus.Refutes))
    assertSilent("refuted MateThreat", selected(refuted))
    assertSilent("engine-only MateThreat", selected(engineOnlyStory))
