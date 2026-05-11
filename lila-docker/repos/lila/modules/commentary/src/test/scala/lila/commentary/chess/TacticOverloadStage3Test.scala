package lila.commentary.chess

class TacticOverloadStage3Test extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val defenderProtectsOneFen = "7k/4q3/b3r3/7n/8/6B1/8/7K w - - 0 1"
  private val twoDefendersFen = "7k/4q3/b1n1r3/1b6/8/6B1/8/7K w - - 0 1"
  private val boardMismatchFen = "7k/4q3/b3r3/8/8/6B1/8/6K1 w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val quietMove = Line(Square('g', 3), Square('h', 4))
  private val illegalMove = Line(Square('g', 3), Square('g', 8))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-3 writer negative corpus produces no public Tactic.Overload text"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val defenderProtectsOne = BoardFacts.fromFen(defenderProtectsOneFen).toOption.get
    val twoDefenders = BoardFacts.fromFen(twoDefendersFen).toOption.get

    val writerNegatives = Vector(
      "defender protects only one target" ->
        TacticOverload.write(
          defenderProtectsOne,
          Some(overloadMove),
          Some(defender),
          Some(firstDutyTarget),
          Some(Square('h', 5)),
          Some(firstDutyTarget)
        ),
      "two different defenders protect two different targets" ->
        TacticOverload.write(
          twoDefenders,
          Some(overloadMove),
          Some(defender),
          Some(firstDutyTarget),
          Some(secondDutyTarget),
          Some(firstDutyTarget)
        ),
      "defender has two duties but current move does not test either duty target" ->
        TacticOverload.write(
          facts,
          Some(quietMove),
          Some(defender),
          Some(firstDutyTarget),
          Some(secondDutyTarget),
          Some(firstDutyTarget)
        ),
      "target is king" ->
        TacticOverload.write(
          facts,
          Some(overloadMove),
          Some(defender),
          Some(Square('h', 8)),
          Some(secondDutyTarget),
          Some(Square('h', 8))
        ),
      "route is illegal" ->
        TacticOverload.write(
          facts,
          Some(illegalMove),
          Some(defender),
          Some(firstDutyTarget),
          Some(secondDutyTarget),
          Some(firstDutyTarget)
        )
    )

    writerNegatives.foreach: (label, maybeStory) =>
      assertEquals(maybeStory, None, label)

  test("Stage-3 forged proof negatives do not lower to overloads_defender"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val mismatchFacts = BoardFacts.fromFen(boardMismatchFen).toOption.get
    val base = overloadStory(facts)
    val proof = base.overloadProof.get
    val mismatchProof =
      OverloadProof.fromRelations(mismatchFacts, Some(overloadMove), overloadRelations(facts))
    val incompleteCannotSatisfyBoth =
      proof.copy(
        cannotSatisfyBoth = proof.cannotSatisfyBoth.map(_.copy(missingEvidence =
          Vector(BoardFacts.MissingEvidence("CannotSatisfyBoth", Vector("absent")))
        )),
        proofComplete = false,
        missingEvidence = Vector(BoardFacts.MissingEvidence("OverloadProof", Vector("complete CannotSatisfyBoth relation")))
      )
    val replyPreservesBoth =
      proof.copy(
        cannotSatisfyBoth = proof.cannotSatisfyBoth.map(_.copy(noReplyPreservesBothDutyTargets = false)),
        replyMap = proof.replyMap :+ OverloadProof.ReplyPreservation(overloadMove, preservesBothDutyTargets = true),
        noReplyPreservesBothDutyTargets = false,
        proofComplete = false,
        missingEvidence = Vector(BoardFacts.MissingEvidence("OverloadProof", Vector("no legal rival reply preserves both duty targets")))
      )

    val forgedRows = Vector(
      "OverloadTest exists but CannotSatisfyBoth is absent" ->
        base.copy(overloadProof = Some(incompleteCannotSatisfyBoth)),
      "at least one legal rival reply preserves both duty targets" ->
        base.copy(overloadProof = Some(replyPreservesBoth)),
      "proof board and Story board differ" ->
        base.copy(overloadProof = Some(mismatchProof)),
      "attack-only row tries to become Overload" ->
        base.copy(overloadProof = None),
      "engine eval/PV says advantage but proof is absent" ->
        base.copy(
          overloadProof = None,
          engineCheck = Some(
            EngineCheck(
              sameBoardProof = true,
              checkedMove = Some(overloadMove),
              engineLine = Some(EngineLine(Vector(overloadMove))),
              replyLine = Some(EngineLine(Vector(overloadMove))),
              evalBefore = Some(EngineEval(0)),
              evalAfter = Some(EngineEval(300)),
              depth = Some(18),
              freshnessPly = Some(0),
              status = EngineCheckStatus.Supports,
              missingEvidence = Vector.empty,
              storyBound = true
            )
          )
        ),
      "SAN/check/checkmate annotation tries to create Overload" ->
        base.copy(overloadProof = None, routeSan = Some("Bd6#"))
    )

    forgedRows.foreach: (label, row) =>
      assertNoOverloadPublicText(label, row)

  test("Stage-3 sibling rows cannot become public Overload"):
    val siblingRows = Vector(
      "Loose tries to become Overload" -> looseAsOverload(),
      "QueenHit tries to become Overload" -> queenHitAsOverload(),
      "Hanging tries to become Overload" -> hangingAsOverload(),
      "Material tries to become Overload" -> materialAsOverload(),
      "RemoveGuard tries to become Overload" -> removeGuardAsOverload(),
      "Defense tries to become Overload" -> defenseAsOverload()
    )

    siblingRows.foreach: (label, row) =>
      assertNoOverloadPublicText(label, row)

  test("Stage-3 negative corpus forbids stronger public overload wording"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val base = overloadStory(facts)
    val negativeRows =
      Vector(
        base.copy(overloadProof = None),
        base.copy(overloadProof = Some(base.overloadProof.get.copy(noReplyPreservesBothDutyTargets = false, proofComplete = false))),
        looseAsOverload(),
        queenHitAsOverload(),
        hangingAsOverload(),
        materialAsOverload(),
        removeGuardAsOverload(),
        defenseAsOverload()
      )

    val plans = negativeRows.flatMap(overloadPlan)
    assertEquals(plans, Vector.empty)
    plans.foreach: plan =>
      assertEquals(plan.allowedClaim.map(_.key), None)

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

  private def overloadRelations(facts: BoardFacts): OverloadProof.RelationBundle =
    OverloadProof.observeRelations(
      facts,
      Some(overloadMove),
      Some(defender),
      Some(firstDutyTarget),
      Some(secondDutyTarget),
      Some(firstDutyTarget)
    )

  private def assertNoOverloadPublicText(label: String, row: Story): Unit =
    val verdict = StoryTable.choose(Vector(row)).head
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim.map(_.key)),
      None,
      label
    )

  private def overloadPlan(row: Story): Option[ExplanationPlan] =
    StoryTable
      .choose(Vector(row))
      .headOption
      .flatMap(ExplanationPlan.fromSelected)
      .filter(_.allowedClaim.exists(_.key == "overloads_defender"))

  private def looseAsOverload(): Story =
    val facts = BoardFacts.fromFen("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1").toOption.get
    TacticLoose
      .write(facts, Some(Line(Square('d', 2), Square('h', 2))))
      .get
      .copy(tactic = Some(Tactic.Overload), writer = Some(StoryWriter.TacticOverload))

  private def queenHitAsOverload(): Story =
    val facts = BoardFacts.fromFen("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1").toOption.get
    TacticQueenHit
      .write(facts, Some(Line(Square('d', 2), Square('h', 2))))
      .get
      .copy(tactic = Some(Tactic.Overload), writer = Some(StoryWriter.TacticOverload))

  private def hangingAsOverload(): Story =
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    TacticHanging
      .write(facts, Line(Square('d', 4), Square('e', 5)))
      .get
      .copy(tactic = Some(Tactic.Overload), writer = Some(StoryWriter.TacticOverload))

  private def materialAsOverload(): Story =
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    SceneMaterial
      .write(facts, Line(Square('d', 4), Square('e', 5)))
      .get
      .copy(scene = Scene.Tactic, tactic = Some(Tactic.Overload), writer = Some(StoryWriter.TacticOverload))

  private def removeGuardAsOverload(): Story =
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    TacticRemoveGuard
      .write(facts, Some(Line(Square('g', 8), Square('c', 4))), Some(Square('e', 5)), Some(Square('c', 4)))
      .get
      .copy(tactic = Some(Tactic.Overload), writer = Some(StoryWriter.TacticOverload))

  private def defenseAsOverload(): Story =
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    SceneDefense
      .write(facts, Line(Square('f', 5), Square('d', 4)), Line(Square('d', 4), Square('e', 4)))
      .get
      .copy(scene = Scene.Tactic, tactic = Some(Tactic.Overload), writer = Some(StoryWriter.TacticOverload))
