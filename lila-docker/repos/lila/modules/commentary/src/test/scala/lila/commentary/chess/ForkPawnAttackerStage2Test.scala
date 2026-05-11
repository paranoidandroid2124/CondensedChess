package lila.commentary.chess

class ForkPawnAttackerStage2Test extends munit.FunSuite:

  test("Stage-2 TacticFork writes existing Fork identity for pawn attacker"):
    val facts = BoardFacts.fromFen("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1").toOption.get
    val route = Some(Line(Square('e', 4), Square('e', 5)))
    val targetA = Some(Square('d', 6))
    val targetB = Some(Square('f', 6))
    val story = TacticFork.write(facts, route, targetA, targetB).get
    val proof = story.multiTargetProof.get

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Fork))
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, targetA)
    assertEquals(story.secondaryTarget, targetB)
    assertEquals(story.anchor, Some(Square('e', 5)))
    assertEquals(story.route, route)
    assertEquals(story.writer, Some(StoryWriter.TacticFork))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(story.storyProof.failures(story), Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.attacker, Some(Piece(Side.White, Man.Pawn, Square('e', 4))))
    assertEquals(proof.attackerAfterMove, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.forkMove, route)
    assertEquals(proof.targetSquares, Vector(Square('d', 6), Square('f', 6)))
    assertEquals(proof.attackedTargetSquaresAfterMove, proof.targetSquares)
    assertEquals(proof.targets.map(_.side).distinct, Vector(Side.Black))
    assertEquals(proof.targets.exists(_.man == Man.King), false)

  test("Stage-2 TacticFork stays silent for pawn-family false surfaces"):
    val facts = BoardFacts.fromFen("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1").toOption.get
    val story =
      TacticFork.write(facts, Some(Line(Square('e', 4), Square('e', 5))), Some(Square('d', 6)), Some(Square('f', 6))).get
    val writerSurface =
      TacticFork.getClass.getDeclaredMethods.map(_.getName).toVector ++
        TacticFork.getClass.getDeclaredFields.map(_.getName).toVector

    assertEquals(story.tactic, Some(Tactic.Fork))
    assertEquals(story.tactic.contains(Tactic.PawnFork), false)
    assertEquals(story.writer, Some(StoryWriter.TacticFork))
    assert(!writerSurface.exists(_.contains("TacticPawnFork")))
    Vector("Material", "PromotionThreat", "PawnAdvance", "PawnCapture").foreach: closed =>
      assert(!writerSurface.exists(_.contains(closed)), closed)
