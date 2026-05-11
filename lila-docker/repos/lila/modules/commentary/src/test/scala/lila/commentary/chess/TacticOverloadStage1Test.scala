package lila.commentary.chess

class TacticOverloadStage1Test extends munit.FunSuite:

  private val overloadFen = "7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1"
  private val boardMismatchFen = "7k/4q3/b3r3/8/8/6B1/8/6K1 w - - 0 1"
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val illegalMove = Line(Square('g', 3), Square('g', 8))
  private val defender = Square('e', 6)
  private val firstDutyTarget = Square('e', 7)
  private val secondDutyTarget = Square('a', 6)

  test("Stage-1 OverloadProof succeeds only with complete relation proof shape"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val relations = completeRelations(facts)
    val proof = proofFrom(facts, relations)

    assertEquals(proof.complete, true)
    assertEquals(proof.proofComplete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.beforeBoard, facts.pieces)
    assertEquals(proof.afterBoard, BoardFacts.piecesAfterLegalMove(facts, overloadMove).get)
    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.route, Some(overloadMove))
    assertEquals(proof.overloadMove, Some(overloadMove))
    assertEquals(proof.defender.map(_.square), Some(defender))
    assertEquals(proof.anchor.map(_.square), Some(defender))
    assertEquals(proof.target.map(_.square), Some(firstDutyTarget))
    assertEquals(proof.testedDutyTarget.map(_.square), Some(firstDutyTarget))
    assertEquals(proof.secondaryTarget.map(_.square), Some(secondDutyTarget))
    assertEquals(proof.otherDutyTarget.map(_.square), Some(secondDutyTarget))
    assertEquals(proof.defenderDuty.exists(_.complete), true)
    assertEquals(proof.dualDefenderDuty.exists(_.complete), true)
    assertEquals(proof.overloadTest.exists(_.complete), true)
    assertEquals(proof.cannotSatisfyBoth.exists(_.complete), true)
    assertEquals(proof.sameBoardLegalReplay, true)
    assertEquals(proof.noReplyPreservesBothDutyTargets, true)
    assertEquals(proof.replyMap.exists(_.preservesBothDutyTargets), false)
    assertEquals(proof.missingEvidence, Vector.empty)

  test("Stage-1 OverloadProof fails when a required relation is incomplete"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val relations = completeRelations(facts)

    val incompleteDefenderDuty =
      relations.copy(defenderDuty =
        relations.defenderDuty.copy(missingEvidence =
          Vector(BoardFacts.MissingEvidence("DefenderDuty", Vector("test gap")))
        )
      )
    val incompleteDualDefenderDuty =
      relations.copy(dualDefenderDuty =
        relations.dualDefenderDuty.copy(missingEvidence =
          Vector(BoardFacts.MissingEvidence("DualDefenderDuty", Vector("test gap")))
        )
      )
    val incompleteOverloadTest =
      relations.copy(overloadTest =
        relations.overloadTest.copy(missingEvidence =
          Vector(BoardFacts.MissingEvidence("OverloadTest", Vector("test gap")))
        )
      )
    val incompleteCannotSatisfyBoth =
      relations.copy(cannotSatisfyBoth =
        relations.cannotSatisfyBoth.copy(missingEvidence =
          Vector(BoardFacts.MissingEvidence("CannotSatisfyBoth", Vector("test gap")))
        )
      )

    Vector(
      proofFrom(facts, incompleteDefenderDuty) -> "complete DefenderDuty relation",
      proofFrom(facts, incompleteDualDefenderDuty) -> "complete DualDefenderDuty relation",
      proofFrom(facts, incompleteOverloadTest) -> "complete OverloadTest relation",
      proofFrom(facts, incompleteCannotSatisfyBoth) -> "complete CannotSatisfyBoth relation"
    ).foreach: (proof, missing) =>
      assertEquals(proof.complete, false, missing)
      assertEquals(proof.proofComplete, false, missing)
      assert(proof.missingEvidence.exists(_.missing.contains(missing)), missing)

  test("Stage-1 OverloadProof fails illegal replay and board mismatch"):
    val facts = BoardFacts.fromFen(overloadFen).toOption.get
    val mismatchFacts = BoardFacts.fromFen(boardMismatchFen).toOption.get
    val illegalRelations = OverloadProof.observeRelations(
      facts,
      Some(illegalMove),
      Some(defender),
      Some(firstDutyTarget),
      Some(secondDutyTarget),
      Some(firstDutyTarget)
    )
    val illegalProof = OverloadProof.fromRelations(facts, Some(illegalMove), illegalRelations)

    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.sameBoardLegalReplay, false)
    assert(
      illegalProof.missingEvidence.exists(_.missing.contains("same-board legal replay")),
      "illegal replay must be missing same-board legal replay"
    )

    val proofWithBoardMismatch = proofFrom(mismatchFacts, completeRelations(facts))
    assertEquals(proofWithBoardMismatch.complete, false)
    assertEquals(proofWithBoardMismatch.proofComplete, false)
    assert(
      proofWithBoardMismatch.missingEvidence.exists(_.missing.contains("board mismatch")),
      "mismatched proof board must be rejected"
    )

  private def completeRelations(facts: BoardFacts): OverloadProof.RelationBundle =
    OverloadProof.observeRelations(
      facts,
      Some(overloadMove),
      Some(defender),
      Some(firstDutyTarget),
      Some(secondDutyTarget),
      Some(firstDutyTarget)
    )

  private def proofFrom(facts: BoardFacts, relations: OverloadProof.RelationBundle): OverloadProof =
    OverloadProof.fromRelations(facts, Some(overloadMove), relations)
