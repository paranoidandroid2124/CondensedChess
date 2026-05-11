package lila.commentary.chess

class ForkPawnAttackerStage1Test extends munit.FunSuite:

  test("Stage-1 MultiTargetProof admits only legal bounded pawn attackers"):
    val legalFacts = BoardFacts.fromFen("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1").toOption.get
    val legalMove = Some(Line(Square('e', 4), Square('e', 5)))
    val targetA = Some(Square('d', 6))
    val targetB = Some(Square('f', 6))
    val legalProof = MultiTargetProof.fromBoardFacts(legalFacts, legalMove, targetA, targetB)

    assertEquals(legalProof.complete, true)
    assertEquals(legalProof.publicClaimAllowed, false)
    assertEquals(legalProof.attacker, Some(Piece(Side.White, Man.Pawn, Square('e', 4))))
    assertEquals(legalProof.attackerAfterMove, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(legalProof.forkMove, legalMove)
    assertEquals(legalProof.targetSquares, Vector(Square('d', 6), Square('f', 6)))
    assertEquals(legalProof.attackedTargetSquaresAfterMove, legalProof.targetSquares)
    assertEquals(legalProof.replyMap.map(_.target), legalProof.targets)
    assertEquals(legalProof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      MultiTargetProof.getClass.getDeclaredMethods.toVector ++ classOf[
        MultiTargetProof
      ].getDeclaredMethods.toVector
    assertEquals(proofHomeMethods.filter(_.getReturnType.getSimpleName.contains("Story")).map(_.getName), Vector.empty)
    assertEquals(
      proofHomeMethods.map(_.getName).filter(name => name == "write" || name == "toStory" || name == "fromStory"),
      Vector.empty
    )

  test("Stage-1 pawn attacker negative corpus remains incomplete or silent"):
    val enPassantFacts = BoardFacts.fromFen("7k/2n1n3/8/3pP3/8/8/8/7K w - d6 0 1").toOption.get
    val pinnedPseudoFacts = BoardFacts.fromFen("7k/8/8/8/1b1n1n2/4b3/3P4/4K3 w - - 0 1").toOption.get
    val promotionFacts = BoardFacts.fromFen("3n1n1k/4P3/8/8/8/8/8/7K w - - 0 1").toOption.get
    val pawnAdvanceOnlyFacts = BoardFacts.fromFen("7k/8/8/8/4P3/8/8/7K w - - 0 1").toOption.get

    Vector(
      "en passant" -> (
        enPassantFacts,
        Some(Line(Square('e', 5), Square('d', 6))),
        Some(Square('c', 7)),
        Some(Square('e', 7)),
        "not en passant"
      ),
        "pinned pawn pseudo-capture" -> (
          pinnedPseudoFacts,
          Some(Line(Square('d', 2), Square('e', 3))),
          Some(Square('d', 4)),
          Some(Square('f', 4)),
          "legal move"
        ),
      "promotion move" -> (
        promotionFacts,
        Some(Line(Square('e', 7), Square('e', 8))),
        Some(Square('d', 8)),
        Some(Square('f', 8)),
        "pawn after route destination"
      ),
      "pawn advance story only" -> (
        pawnAdvanceOnlyFacts,
        Some(Line(Square('e', 4), Square('e', 5))),
        Some(Square('d', 6)),
        Some(Square('f', 6)),
        "target pieces"
      )
    ).foreach: (label, row) =>
      val (facts, move, first, second, missing) = row
      val proof = MultiTargetProof.fromBoardFacts(facts, move, first, second)
      assertEquals(proof.complete, false, label)
      assertEquals(proof.publicClaimAllowed, false, label)
      assert(proof.missingEvidence.exists(_.missing.contains(missing)), label)
      assertEquals(TacticFork.write(facts, move, first, second), None, label)
