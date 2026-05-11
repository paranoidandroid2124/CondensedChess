package lila.commentary.chess

class TacticOverloadStage5Test extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-5 one legal Overload row appears once and can Lead only when selected uncapped unrefuted proof-backed"):
    val row = overloadStory(board(overloadFen)).copy(proof = orderingProof)
    val verdicts = StoryTable.choose(Vector(row))
    val verdict = verdicts.head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdicts.size, 1)
    assertEquals(verdict.story.tactic, Some(Tactic.Overload))
    assertEquals(verdict.story.writer, Some(StoryWriter.TacticOverload))
    assertEquals(verdict.story.overloadProof.exists(_.complete), true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes), false)
    assertEquals(plan.allowedClaim.map(_.key), Some("overloads_defender"))

  test("Stage-5 duplicate Overload rows produce one speaking Lead under existing StoryTable convention"):
    val row = overloadStory(board(overloadFen)).copy(proof = orderingProof)
    val verdicts = StoryTable.choose(Vector(row, row)).filter(_.story.tactic.contains(Tactic.Overload))
    val claimKeys = verdicts.flatMap(verdict => ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim.map(_.key)))

    assertEquals(verdicts.size, 2)
    assertEquals(verdicts.count(_.role == Role.Lead), 1)
    assertEquals(verdicts.count(_.role == Role.Support), 1)
    assertEquals(claimKeys, Vector("overloads_defender"))

  test("Stage-5 sibling rows do not steal Overload identity or proof home"):
    val overload = overloadStory(board(overloadFen)).copy(proof = orderingProof)
    val siblings = siblingRows.map(_.copy(proof = orderingProof))
    val verdicts = StoryTable.choose(overload +: siblings)
    val overloadVerdict = verdicts.find(_.story.tactic.contains(Tactic.Overload)).get

    assertEquals(overloadVerdict.story.writer, Some(StoryWriter.TacticOverload))
    assertEquals(overloadVerdict.story.scene, Scene.Tactic)
    assertEquals(overloadVerdict.story.tactic, Some(Tactic.Overload))
    assert(overloadVerdict.story.overloadProof.nonEmpty)
    assertEquals(overloadVerdict.story.captureResult, None)
    assertEquals(overloadVerdict.story.removeGuardProof, None)
    assertEquals(overloadVerdict.story.queenHitProof, None)
    assertEquals(overloadVerdict.story.loosePieceProof, None)
    assert(!verdicts.exists(_.story.tactic.exists(tactic => tactic == Tactic.Deflect || tactic == Tactic.Decoy)))
    siblings.foreach: sibling =>
      val siblingVerdict = verdicts.find(_.story.writer == sibling.writer).get
      assertEquals(siblingVerdict.story.scene, sibling.scene, sibling.writer.toString)
      assertEquals(siblingVerdict.story.tactic, sibling.tactic, sibling.writer.toString)
    assert(verdicts.exists(v => v.story.writer.contains(StoryWriter.SceneMaterial) && v.story.captureResult.nonEmpty))
    assert(verdicts.exists(v => v.story.writer.contains(StoryWriter.TacticHanging) && v.story.captureResult.nonEmpty))
    assert(verdicts.exists(v => v.story.writer.contains(StoryWriter.TacticLoose) && v.story.loosePieceProof.nonEmpty))
    assert(verdicts.exists(v => v.story.writer.contains(StoryWriter.TacticQueenHit) && v.story.queenHitProof.nonEmpty))
    assert(verdicts.exists(v => v.story.writer.contains(StoryWriter.TacticRemoveGuard) && v.story.removeGuardProof.nonEmpty))

  test("Stage-5 Support Context Blocked capped refuted and non-Lead Overload rows do not speak"):
    val facts = board(overloadFen)
    val overload = overloadStory(facts).copy(proof = orderingProof)
    val support = StoryTable.choose(Vector(overload, overload)).find(_.role == Role.Support).get
    val context = StoryTable.choose(Vector(overload.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(overload.copy(overloadProof = None))).head
    val capped = StoryTable
      .choose(Vector(TacticOverload.withEngineCheck(overload, check(facts, overload, EngineCheckStatus.Caps)).get))
      .head
    val refuted = StoryTable
      .choose(Vector(TacticOverload.withEngineCheck(overload, check(facts, overload, EngineCheckStatus.Refutes)).get))
      .head

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    Vector(support, context, blocked, capped, refuted).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, verdict.toString)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

  test("Stage-5 Overload row does not create material wording"):
    val row = overloadStory(board(overloadFen)).copy(proof = orderingProof)
    val verdict = StoryTable.choose(Vector(row)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OverloadsDefender))
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Overload))
    assertEquals(verdict.story.scene == Scene.Material, false)
    assertEquals(verdict.story.tactic.exists(tactic => tactic == Tactic.Hanging || tactic == Tactic.RemoveGuard), false)
    assert(plan.forbiddenWording.contains(ForbiddenWording.WinsMaterial))
    assert(plan.forbiddenWording.contains(ForbiddenWording.WinsPiece))
    assert(plan.forbiddenWording.contains(ForbiddenWording.RemovesDefender))
    val rendered = DeterministicRenderer.fromPlan(plan).get
    assertEquals(rendered.claimKey, "overloads_defender")
    assert(!rendered.text.toLowerCase.contains("wins material"))
    assert(!rendered.text.toLowerCase.contains("wins a piece"))
    assert(!rendered.text.toLowerCase.contains("removes the defender"))

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

  private def siblingRows: Vector[Story] =
    val materialFacts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val hanging = TacticHanging.write(materialFacts, materialMove).get

    val looseFacts = board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
    val looseMove = Line(Square('d', 2), Square('h', 2))
    val loose = TacticLoose.write(looseFacts, Some(looseMove)).get

    val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(looseMove)).get

    val removeFacts = board("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1")
    val removeMove = Line(Square('d', 3), Square('e', 5))
    val remove = TacticRemoveGuard.write(removeFacts, Some(removeMove), Some(Square('g', 6)), Some(Square('e', 5))).get

    Vector(material, hanging, loose, queenHit, remove)

  private def check(facts: BoardFacts, story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(overloadMove))),
      replyLine = Some(EngineLine(Vector(overloadMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(if status == EngineCheckStatus.Supports then 300 else -50)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-5 FEN: $fen -> $error"), identity)

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
