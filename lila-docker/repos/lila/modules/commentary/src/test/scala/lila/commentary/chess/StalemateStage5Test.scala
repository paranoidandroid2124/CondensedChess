package lila.commentary.chess

class StalemateStage5Test extends munit.FunSuite:

  private case class CollisionRow(label: String, story: Story)

  private val stalemateFen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1"
  private val stalemateMove = Line(Square('g', 5), Square('g', 6))
  private val mateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val mateMove = Line(Square('c', 7), Square('b', 7))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def eventProof(value: Int): Proof =
    Proof(
      boardProof = value,
      lineProof = value,
      ownerProof = value,
      anchorProof = value,
      routeProof = value,
      persistence = 0,
      immediacy = value,
      forcing = value,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = value,
      pieceSupport = value,
      pawnSupport = value,
      sourceFit = 0,
      novelty = 0,
      clarity = value
    )

  private def materialProof(value: Int): Proof =
    eventProof(value).copy(conversionPrize = value)

  private def strong(story: Story, value: Int = 99): Story =
    story.copy(proof = eventProof(value))

  private def strongMaterial(story: Story, value: Int = 99): Story =
    story.copy(proof = materialProof(value))

  private def stalemate(value: Int = 99): CollisionRow =
    CollisionRow(
      "Scene.Stalemate",
      SceneStalemate.write(facts(stalemateFen), stalemateMove).get.copy(proof = eventProof(value))
    )

  private def checkmate(value: Int = 100): CollisionRow =
    CollisionRow(
      "Scene.Checkmate",
      SceneCheckmate.write(facts(mateFen), mateMove).get.copy(proof = eventProof(value))
    )

  private def rowId(rows: Vector[CollisionRow], story: Story): String =
    rows.collectFirst { case row if row.story == story => row.label }.getOrElse(s"unknown:${story.scene}")

  private def shape(rows: Vector[CollisionRow], verdicts: Vector[Verdict]) =
    verdicts.map: verdict =>
      (
        rowId(rows, verdict.story),
        verdict.role,
        verdict.leadAllowed,
        verdict.engineCheckStatus,
        verdict.engineStrengthLimited,
        ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
      )

  private def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertStalemateRendererBoundary(label: String, verdict: Verdict): Unit =
    if verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited &&
      verdict.engineCheckStatus.forall(_ == EngineCheckStatus.Supports)
    then assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("stalemates"), label)
    else assertNoStandaloneText(label, verdict)

  private def assertStableRows(label: String, rows: Vector[CollisionRow]): Vector[Verdict] =
    val forward = StoryTable.choose(rows.map(_.story))
    val reverse = StoryTable.choose(rows.reverse.map(_.story))
    val sorted = StoryTable.choose(rows.sortBy(_.label).map(_.story))
    val forwardShape = shape(rows, forward)

    assertEquals(shape(rows, reverse), forwardShape, s"$label reverse input order")
    assertEquals(shape(rows, sorted), forwardShape, s"$label sorted input order")
    assertEquals(forward.count(_.role == Role.Lead), 1, label)
    forward

  private def stalemateEngine(row: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = facts(stalemateFen),
      story = Some(row),
      engineLine = Some(EngineLine(Vector(stalemateMove))),
      replyLine = Some(EngineLine(Vector(stalemateMove))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def collisionTargets: Vector[CollisionRow] =
    val mateFacts = facts(mateFen)
    val checkGiven =
      CollisionRow(
        "Scene.CheckGiven",
        strong(SceneCheckGiven.write(mateFacts, mateMove).get)
      )

    val escapeFacts = facts("k3r3/8/8/8/8/8/3B4/4K3 w - - 0 1")
    val escapeMove = Line(Square('d', 2), Square('e', 3))
    val checkEscaped =
      CollisionRow(
        "Scene.CheckEscaped",
        strong(SceneCheckEscaped.write(escapeFacts, escapeMove).get)
      )

    val materialFacts = facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material =
      CollisionRow(
        "Scene.Material",
        strongMaterial(SceneMaterial.write(materialFacts, materialMove).get)
      )
    val hanging =
      CollisionRow(
        "Tactic.Hanging",
        strongMaterial(TacticHanging.write(materialFacts, materialMove).get)
      )

    val forkFacts = facts("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      CollisionRow(
        "Tactic.Fork",
        strong(TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get)
      )

    val defenseFacts = facts("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense =
      CollisionRow(
        "Scene.Defense",
        strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get)
      )

    val discoveredFacts = facts("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      CollisionRow(
        "Tactic.DiscoveredAttack",
        strong(
          TacticDiscoveredAttack
            .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
            .get
        )
      )

    val pinFacts = facts("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val pin =
      CollisionRow(
        "Tactic.Pin",
        strong(
          TacticPin
            .write(
              pinFacts,
              Some(discoveredMove),
              Some(Square('b', 1)),
              Some(Square('g', 6)),
              Some(Square('h', 7))
            )
            .get
        )
      )

    val removeGuardFacts = facts("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1")
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      CollisionRow(
        "Tactic.RemoveGuard",
        strong(
          TacticRemoveGuard
            .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
            .get
        )
      )

    val skewerFacts = facts("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      CollisionRow(
        "Tactic.Skewer",
        strong(
          TacticSkewer
            .write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
            .get
        )
      )

    val stopFacts = facts("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1")
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = CollisionRow("Scene.PawnStop", strong(ScenePawnStop.write(stopFacts, stopMove).get))

    val breakFacts = facts("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1")
    val breakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = CollisionRow("Scene.PawnBreak", strong(ScenePawnBreak.write(breakFacts, breakMove).get))

    val blockFacts = facts("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1")
    val blockMove = Line(Square('g', 5), Square('e', 6))
    val pawnBlock = CollisionRow("Scene.PawnBlock", strong(ScenePawnBlock.write(blockFacts, blockMove).get))

    val openedFacts = facts("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1")
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture = CollisionRow("Scene.PawnCapture", strong(ScenePawnCapture.write(openedFacts, openedMove).get))
    val passedPawnCreated =
      CollisionRow("Scene.PassedPawnCreated", strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get))
    val fileOpened = CollisionRow("Scene.FileOpened", strong(SceneFileOpened.write(openedFacts, openedMove).get))

    val advanceFacts = facts("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1")
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = CollisionRow("Scene.PawnAdvance", strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get))

    val threatFacts = facts("k7/8/4P3/8/8/8/8/4K3 w - - 0 1")
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat =
      CollisionRow("Scene.PromotionThreat", strong(ScenePromotionThreat.write(threatFacts, threatMove).get))

    val promotionFacts = facts("k7/4P3/8/8/8/8/8/4K3 w - - 0 1")
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion = CollisionRow("Scene.Promotion", strong(ScenePromotion.write(promotionFacts, promotionMove).get))

    Vector(
      checkGiven,
      checkEscaped,
      material,
      hanging,
      fork,
      defense,
      discovered,
      pin,
      removeGuard,
      skewer,
      pawnStop,
      pawnBreak,
      pawnBlock,
      pawnCapture,
      passedPawnCreated,
      fileOpened,
      pawnAdvance,
      promotionThreat,
      promotion
    )

  test("Stalemate-5 StoryTable keeps Stalemate stable against opened collision targets"):
    val single = StoryTable.choose(Vector(stalemate().story)).head
    assertEquals(single.role, Role.Lead)
    assertEquals(single.story.scene, Scene.Stalemate)
    assertStalemateRendererBoundary("single Stalemate renderer boundary", single)

    collisionTargets.foreach: target =>
      val rows = Vector(stalemate(), target)
      val verdicts = assertStableRows(s"Stalemate vs ${target.label}", rows)
      val stalemateVerdict = verdicts.find(verdict => rowId(rows, verdict.story) == "Scene.Stalemate").get
      val targetVerdict = verdicts.find(verdict => rowId(rows, verdict.story) == target.label).get

      assertEquals(stalemateVerdict.story.scene, Scene.Stalemate, target.label)
      assertEquals(stalemateVerdict.story.writer, Some(StoryWriter.SceneStalemate), target.label)
      assertEquals(stalemateVerdict.story.stalemateProof.exists(_.complete), true, target.label)
      assertEquals(stalemateVerdict.story.checkmateProof, None, target.label)
      assertEquals(stalemateVerdict.story.checkGivenProof, None, target.label)
      assertEquals(stalemateVerdict.story.checkEscapedProof, None, target.label)
      assertStalemateRendererBoundary(s"Stalemate renderer boundary for ${target.label}", stalemateVerdict)

      assertEquals(rowId(rows, targetVerdict.story), target.label)
      assertEquals(targetVerdict.story.scene != Scene.Stalemate, true, target.label)
      assertEquals(targetVerdict.story.stalemateProof, None, target.label)

  test("Stalemate-5 keeps Checkmate and Stalemate terminal homes separate"):
    val stale = stalemate().story
    val mate = checkmate().story

    assertEquals(SceneStalemate.write(facts(stalemateFen), stalemateMove).nonEmpty, true)
    assertEquals(SceneCheckmate.write(facts(stalemateFen), stalemateMove), None)
    assertEquals(SceneCheckmate.write(facts(mateFen), mateMove).nonEmpty, true)
    assertEquals(SceneStalemate.write(facts(mateFen), mateMove), None)

    assertEquals(stale.scene, Scene.Stalemate)
    assertEquals(stale.stalemateProof.exists(_.complete), true)
    assertEquals(stale.checkmateProof, None)

    assertEquals(mate.scene, Scene.Checkmate)
    assertEquals(mate.checkmateProof.exists(_.complete), true)
    assertEquals(mate.stalemateProof, None)

    val rows = Vector(CollisionRow("Scene.Stalemate", stale), CollisionRow("Scene.Checkmate", mate))
    val verdicts = assertStableRows("Stalemate vs Checkmate", rows)
    assert(verdicts.exists(_.story.scene == Scene.Stalemate))
    assert(verdicts.exists(_.story.scene == Scene.Checkmate))
    verdicts.foreach: verdict =>
      if verdict.story.scene == Scene.Stalemate then assertStalemateRendererBoundary("Stalemate terminal row renderer boundary", verdict)

  test("Stalemate-5 no-legal-move ownership depends on exact check state proof"):
    val staleProof = StalemateProof.fromBoardFacts(facts(stalemateFen), stalemateMove)
    val mateProof = CheckmateProof.fromBoardFacts(facts(mateFen), mateMove)
    val notStalemate = StalemateProof.fromBoardFacts(facts(mateFen), mateMove)

    assertEquals(staleProof.complete, true)
    assertEquals(staleProof.afterBoardRivalSideNotInCheck, true)
    assertEquals(staleProof.afterBoardRivalSideHasNoLegalMoves, true)
    assertEquals(SceneStalemate.write(facts(stalemateFen), stalemateMove).exists(_.scene == Scene.Stalemate), true)

    assertEquals(mateProof.complete, true)
    assertEquals(mateProof.afterBoardRivalKingInCheck, true)
    assertEquals(mateProof.afterBoardRivalSideHasNoLegalEscape, true)
    assertEquals(notStalemate.afterBoardRivalSideHasNoLegalMoves, true)
    assertEquals(notStalemate.afterBoardRivalSideNotInCheck, false)
    assertEquals(notStalemate.complete, false)
    assertEquals(SceneCheckmate.write(facts(mateFen), mateMove).exists(_.scene == Scene.Checkmate), true)

  test("Stalemate-5 non-Lead capped refuted and blocked Stalemate rows have no standalone text"):
    val base = stalemate().story
    val mateLead = checkmate(100).story

    val supportVerdict =
      StoryTable.choose(Vector(mateLead, base.copy(proof = eventProof(90)))).find(_.story.scene == Scene.Stalemate).get
    assertEquals(supportVerdict.role, Role.Support)
    assertNoStandaloneText("support Stalemate", supportVerdict)

    val contextVerdict = StoryTable.choose(Vector(base.copy(proof = eventProof(20)))).head
    assertEquals(contextVerdict.role, Role.Context)
    assertNoStandaloneText("context Stalemate", contextVerdict)

    val capped = base.copy(engineCheck = Some(stalemateEngine(base, EngineCheckStatus.Caps)))
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertNoStandaloneText("capped Stalemate", cappedVerdict)

    val refuted = base.copy(engineCheck = Some(stalemateEngine(base, EngineCheckStatus.Refutes)))
    val refutedVerdict = StoryTable.choose(Vector(refuted)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.selected, false)
    assertNoStandaloneText("refuted Stalemate", refutedVerdict)

    val blocked = base.copy(writer = None)
    val blockedVerdict = StoryTable.choose(Vector(blocked)).head
    assertEquals(blockedVerdict.role, Role.Blocked)
    assertEquals(blockedVerdict.selected, false)
    assertNoStandaloneText("blocked Stalemate", blockedVerdict)

    val claimKeys = ExplanationClaim.values.map(_.key).toVector
    Vector(
      "draw",
      "draws_endgame",
      "tablebase_draw",
      "saves_game",
      "throws_win",
      "blunder",
      "best_move",
      "only_move",
      "forced",
      "winning",
      "losing",
      "decisive",
      "no_counterplay"
    ).foreach: closedKey =>
      assertEquals(claimKeys.contains(closedKey), false, closedKey)
