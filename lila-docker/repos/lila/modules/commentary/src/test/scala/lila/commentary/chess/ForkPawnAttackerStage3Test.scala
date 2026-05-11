package lila.commentary.chess

class ForkPawnAttackerStage3Test extends munit.FunSuite:

  private val forkFacts = facts("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1")
  private val forkRoute = Line(Square('e', 4), Square('e', 5))
  private val targetA = Square('d', 6)
  private val targetB = Square('f', 6)

  test("Stage-3 pawn fork geometry false positives stay silent"):
    val illegalMove = (forkFacts, Line(Square('e', 4), Square('e', 6)), targetA, targetB)
    val pinnedPseudo =
      (facts("7k/8/8/8/1b1n1n2/4b3/3P4/4K3 w - - 0 1"), Line(Square('d', 2), Square('e', 3)), Square('d', 4), Square('f', 4))
    val targetANotAttacked =
      (facts("7k/8/2n2n2/8/4P3/8/8/7K w - - 0 1"), forkRoute, Square('c', 6), targetB)
    val targetBNotAttacked =
      (facts("7k/8/3n2n1/8/4P3/8/8/7K w - - 0 1"), forkRoute, targetA, Square('g', 6))
    val duplicatedTargets = (forkFacts, forkRoute, targetA, targetA)
    val ownPieceTarget =
      (facts("7k/8/3N1n2/8/4P3/8/8/7K w - - 0 1"), forkRoute, targetA, targetB)
    val kingTarget =
      (facts("8/8/3k1n2/8/4P3/8/8/7K w - - 0 1"), forkRoute, targetA, targetB)

    Vector(
      "illegal pawn move" -> illegalMove,
      "pseudo-legal pinned pawn move" -> pinnedPseudo,
      "pawn does not attack target A after move" -> targetANotAttacked,
      "pawn does not attack target B after move" -> targetBNotAttacked,
      "duplicated targets" -> duplicatedTargets,
      "own-piece target" -> ownPieceTarget,
      "king target" -> kingTarget
    ).foreach: (label, row) =>
      val (board, route, first, second) = row
      assertNoFork(board, Some(route), Some(first), Some(second), label)

    assertNoFork(forkFacts, Some(forkRoute), Some(targetA), None, "one target only")

  test("Stage-3 pawn family and material contaminants stay out of Fork"):
    val promotionFacts = facts("3n1n1k/4P3/8/8/8/8/8/7K w - - 0 1")
    val promotionRoute = Line(Square('e', 7), Square('e', 8))
    val promotionThreatFacts = facts("7k/8/4P3/8/8/8/8/7K w - - 0 1")
    val promotionThreatRoute = Line(Square('e', 6), Square('e', 7))
    val materialFacts = facts("7k/8/8/3n4/4P3/8/8/7K w - - 0 1")
    val materialRoute = Line(Square('e', 4), Square('d', 5))
    val pawnAdvanceFacts = facts("7k/8/8/8/4P3/8/8/7K w - - 0 1")
    val pawnCaptureFacts = facts("7k/8/8/3p4/4P3/8/8/7K w - - 0 1")
    val pawnCaptureRoute = Line(Square('e', 4), Square('d', 5))

    assertNoFork(promotionFacts, Some(promotionRoute), Some(Square('d', 8)), Some(Square('f', 8)), "promotion move contamination")
    assertEquals(ScenePromotionThreat.write(promotionThreatFacts, promotionThreatRoute).nonEmpty, true)
    assertNoFork(
      promotionThreatFacts,
      Some(promotionThreatRoute),
      Some(Square('d', 8)),
      Some(Square('f', 8)),
      "promotion-threat contamination"
    )
    assertEquals(TacticHanging.write(materialFacts, materialRoute).nonEmpty, true)
    assertNoFork(materialFacts, Some(materialRoute), Some(Square('d', 5)), Some(Square('f', 6)), "pawn capture / material-gain contamination")
    assertEquals(ScenePawnAdvance.write(pawnAdvanceFacts, forkRoute).nonEmpty, true)
    assertNoFork(pawnAdvanceFacts, Some(forkRoute), Some(targetA), Some(targetB), "PawnAdvance-only move")
    assertEquals(ScenePawnCapture.write(pawnCaptureFacts, pawnCaptureRoute).nonEmpty, true)
    assertNoFork(pawnCaptureFacts, Some(pawnCaptureRoute), Some(Square('d', 5)), Some(Square('f', 6)), "PawnCapture-only move")

  test("Stage-3 sibling tactic attack shapes do not enter Fork"):
    val queenHitFacts = facts("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
    val queenHitRoute = Line(Square('d', 2), Square('h', 2))
    val looseFacts = facts("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
    val skewerFacts = facts("r6k/8/q7/8/8/8/8/R6K w - - 0 1")
    val skewerRoute = Line(Square('a', 1), Square('a', 4))

    assertEquals(TacticQueenHit.write(queenHitFacts, Some(queenHitRoute)).nonEmpty, true)
    assertNoFork(queenHitFacts, Some(queenHitRoute), Some(Square('h', 5)), Some(Square('a', 5)), "QueenHit-only attack")
    assertEquals(TacticLoose.write(looseFacts, Some(queenHitRoute)).nonEmpty, true)
    assertNoFork(looseFacts, Some(queenHitRoute), Some(Square('h', 5)), Some(Square('a', 5)), "Loose-only attack")
    assertNoFork(skewerFacts, Some(skewerRoute), Some(Square('a', 6)), Some(Square('a', 8)), "Skewer-looking line")

  test("Stage-3 reply engine source and diagnostics cannot create pawn fork"):
    val story = TacticFork.write(forkFacts, Some(forkRoute), Some(targetA), Some(targetB)).get
    val oneReplySavesBoth =
      story.copy(
        multiTargetProof = story.multiTargetProof.map: proof =>
          proof.copy(replyMap = proof.replyMap.map(_.copy(savedByOneReply = true)))
      )
    val refutes = EngineCheck.fromStory(
      facts = forkFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(forkRoute))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(80)),
      evalAfter = Some(EngineEval(-120)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnly = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(forkRoute),
      engineLine = Some(EngineLine(Vector(forkRoute))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(10)),
      evalAfter = Some(EngineEval(120)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnlyStory = story.copy(writer = None, multiTargetProof = None, engineCheck = Some(engineOnly))
    val sourceRow = Story(
      scene = Scene.Source,
      tactic = Some(Tactic.PawnFork),
      proof = highProof,
      side = Side.White,
      rival = Side.Black,
      target = Some(targetA),
      secondaryTarget = Some(targetB),
      anchor = Some(Square('e', 5)),
      route = Some(forkRoute),
      routeSan = BoardFacts.sanFor(forkFacts, forkRoute),
      storyProof = StoryProof.fromBoardFacts(forkFacts, forkRoute)
    )
    val proofFailureVerdict =
      StoryTable.choose(Vector(story.copy(storyProof = StoryProof.empty))).head.copy(
        proofFailures = Vector(BoardFacts.MissingEvidence("MultiTargetProof", Vector("pawn fork")))
      )

    assertBlocked(oneReplySavesBoth, "one reply saves both targets")
    assertBlocked(TacticFork.withEngineCheck(story, refutes).get, "stronger rival reply refutes fork")
    assertBlocked(engineOnlyStory, "EngineCheck-only evidence")
    assertSilentOrBlocked(sourceRow, "source row saying pawn fork")
    val proofFailurePlan = ExplanationPlan.fromSelected(proofFailureVerdict)
    assertEquals(proofFailurePlan.flatMap(_.allowedClaim), None, "proofFailures text saying pawn fork")

  test("Stage-3 keeps PawnFork names and stronger wording closed"):
    val story = TacticFork.write(forkFacts, Some(forkRoute), Some(targetA), Some(targetB)).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val runtimeNames =
      TacticFork.getClass.getDeclaredMethods.map(_.getName).toVector ++
        TacticFork.getClass.getDeclaredFields.map(_.getName).toVector

    assertEquals(story.tactic, Some(Tactic.Fork))
    assertEquals(story.tactic.contains(Tactic.PawnFork), false)
    assertEquals(plan.allowedClaim.map(_.key), Some("forks_two_targets"))
    Vector("TacticPawnFork", "PawnForkProof", "pawn_forks_two_targets").foreach: closed =>
      assert(!runtimeNames.exists(_.contains(closed)), closed)
    val allowedKey = plan.allowedClaim.map(_.key).mkString
    Vector("wins_material", "wins_piece", "tempo", "best", "only", "forced").foreach: closed =>
      assert(!allowedKey.contains(closed), closed)
    Vector("wins_material", "best_move", "only_move", "forced").foreach: closed =>
      assert(plan.forbiddenWording.exists(_.key.contains(closed)), closed)

  private def assertNoFork(
      board: BoardFacts,
      route: Option[Line],
      first: Option[Square],
      second: Option[Square],
      label: String
  ): Unit =
    assertEquals(TacticFork.write(board, route, first, second), None, label)
    assertEquals(MultiTargetProof.fromBoardFacts(board, route, first, second).complete, false, label)

  private def assertBlocked(story: Story, label: String): Unit =
    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.role, Role.Blocked, label)
    assertEquals(verdict.leadAllowed, false, label)
    ExplanationPlan.fromSelected(verdict).foreach: plan =>
      assertEquals(plan.debugOnly, true, label)
      assertEquals(plan.allowedClaim, None, label)

  private def assertSilentOrBlocked(story: Story, label: String): Unit =
    StoryTable.choose(Vector(story)).headOption.foreach: verdict =>
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      ExplanationPlan.fromSelected(verdict).foreach: plan =>
        assertEquals(plan.debugOnly, true, label)
        assertEquals(plan.allowedClaim, None, label)

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-3 FEN: $fen -> $error"), identity)

  private val highProof: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 80,
      immediacy = 80,
      forcing = 80,
      conversionPrize = 80,
      counterplayRisk = 20,
      kingHeat = 0,
      pieceSupport = 80,
      pawnSupport = 0,
      sourceFit = 80,
      novelty = 0,
      clarity = 80
    )
