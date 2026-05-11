package lila.commentary.chess

class TacticOverloadStage0Test extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-0 admits narrow proof-backed Tactic.Overload while sidecar stays non-public"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val relations = OverloadProof.observeRelations(
      facts,
      Some(overloadMove),
      Some(defender),
      Some(firstDutyTarget),
      Some(secondDutyTarget),
      Some(firstDutyTarget)
    )
    val proof = OverloadProof.fromRelations(facts, Some(overloadMove), relations)

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.noReplyPreservesBothDutyTargets, true)
    assertEquals(proof.replyMap.exists(_.preservesBothDutyTargets), false)

    val story = TacticOverload
      .write(
        facts,
        Some(overloadMove),
        Some(defender),
        Some(firstDutyTarget),
        Some(secondDutyTarget),
        Some(firstDutyTarget)
      )
      .get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Overload))
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.TacticOverload))
    assertEquals(story.overloadProof.exists(_.complete), true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim.map(_.key)), Some("overloads_defender"))

  test("Stage-0 blocks writerless or incomplete Overload-shaped rows"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val story = TacticOverload
      .write(
        facts,
        Some(overloadMove),
        Some(defender),
        Some(firstDutyTarget),
        Some(secondDutyTarget),
        Some(firstDutyTarget)
      )
      .get
    val writerless = story.copy(writer = None)
    val missingProof = story.copy(overloadProof = None)

    Vector(writerless, missingProof).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, row.toString)
      assertEquals(verdict.leadAllowed, false, row.toString)

    val removeGuard = story.copy(tactic = Some(Tactic.RemoveGuard))
    val deflect = story.copy(tactic = Some(Tactic.Deflect))
    val decoy = story.copy(tactic = Some(Tactic.Decoy))
    Vector(removeGuard, deflect, decoy).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, row.toString)
      assertEquals(verdict.leadAllowed, false, row.toString)
