package lila.commentary.chess

class CheckmateStage5Test extends munit.FunSuite:

  private case class CollisionRow(label: String, story: Story, claim: Option[ExplanationClaim])

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

  private def checkmate(value: Int = 100): CollisionRow =
    CollisionRow(
      "Scene.Checkmate",
      SceneCheckmate.write(facts(mateFen), mateMove).get.copy(proof = eventProof(value)),
      Some(ExplanationClaim.Checkmates)
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

  private def assertCheckmateText(label: String, verdict: Verdict): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("checkmates"), label)

  private def assertStableCollision(label: String, rows: Vector[CollisionRow], expectedLead: String): Vector[Verdict] =
    val forward = StoryTable.choose(rows.map(_.story))
    val reverse = StoryTable.choose(rows.reverse.map(_.story))
    val sorted = StoryTable.choose(rows.sortBy(_.label).map(_.story))
    val forwardShape = shape(rows, forward)

    assertEquals(shape(rows, reverse), forwardShape, s"$label reverse input order")
    assertEquals(shape(rows, sorted), forwardShape, s"$label sorted input order")
    assertEquals(forward.count(_.role == Role.Lead), 1, label)
    assertEquals(rowId(rows, forward.head.story), expectedLead, label)
    forward

  private def assertStableRows(label: String, rows: Vector[CollisionRow]): Vector[Verdict] =
    val forward = StoryTable.choose(rows.map(_.story))
    val reverse = StoryTable.choose(rows.reverse.map(_.story))
    val sorted = StoryTable.choose(rows.sortBy(_.label).map(_.story))
    val forwardShape = shape(rows, forward)

    assertEquals(shape(rows, reverse), forwardShape, s"$label reverse input order")
    assertEquals(shape(rows, sorted), forwardShape, s"$label sorted input order")
    assertEquals(forward.count(_.role == Role.Lead), 1, label)
    forward

  private def checkmateEngine(row: Story, status: EngineCheckStatus, before: Int = 0, after: Int = 0): EngineCheck =
    EngineCheck.fromStory(
      facts = facts(mateFen),
      story = Some(row),
      engineLine = Some(EngineLine(Vector(mateMove))),
      replyLine = Some(EngineLine(Vector(mateMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def collisionTargets: Vector[CollisionRow] =
    val mateFacts = facts(mateFen)
    val checkGiven =
      CollisionRow(
        "Scene.CheckGiven",
        strong(SceneCheckGiven.write(mateFacts, mateMove).get),
        Some(ExplanationClaim.GivesCheck)
      )

    val escapeFacts = facts("k3r3/8/8/8/8/8/3B4/4K3 w - - 0 1")
    val escapeMove = Line(Square('d', 2), Square('e', 3))
    val checkEscaped =
      CollisionRow(
        "Scene.CheckEscaped",
        strong(SceneCheckEscaped.write(escapeFacts, escapeMove).get),
        Some(ExplanationClaim.EscapesCheck)
      )

    val materialFacts = facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material =
      CollisionRow(
        "Scene.Material",
        strongMaterial(SceneMaterial.write(materialFacts, materialMove).get),
        Some(ExplanationClaim.MaterialBalanceChanges)
      )
    val hanging =
      CollisionRow(
        "Tactic.Hanging",
        strongMaterial(TacticHanging.write(materialFacts, materialMove).get),
        Some(ExplanationClaim.CanWinPiece)
      )

    val forkFacts = facts("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      CollisionRow(
        "Tactic.Fork",
        strong(TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get),
        Some(ExplanationClaim.ForksTwoTargets)
      )

    val defenseFacts = facts("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense =
      CollisionRow(
        "Scene.Defense",
        strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get),
        Some(ExplanationClaim.DefendsPiece)
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
        ),
        Some(ExplanationClaim.RevealsAttackOnPiece)
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
        ),
        Some(ExplanationClaim.PinsPiece)
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
        ),
        Some(ExplanationClaim.RemovesDefender)
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
        ),
        Some(ExplanationClaim.SkewersPieceToPiece)
      )

    val stopFacts = facts("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1")
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop =
      CollisionRow(
        "Scene.PawnStop",
        strong(ScenePawnStop.write(stopFacts, stopMove).get),
        Some(ExplanationClaim.StopsPassedPawnNextAdvance)
      )

    val breakFacts = facts("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1")
    val breakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak =
      CollisionRow(
        "Scene.PawnBreak",
        strong(ScenePawnBreak.write(breakFacts, breakMove).get),
        Some(ExplanationClaim.ChallengesPawnDirectly)
      )

    val blockFacts = facts("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1")
    val blockMove = Line(Square('g', 5), Square('e', 6))
    val pawnBlock = CollisionRow("Scene.PawnBlock", strong(ScenePawnBlock.write(blockFacts, blockMove).get), Some(ExplanationClaim.BlocksPawn))

    val openedFacts = facts("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1")
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture =
      CollisionRow(
        "Scene.PawnCapture",
        strong(ScenePawnCapture.write(openedFacts, openedMove).get),
        Some(ExplanationClaim.CapturesPawn)
      )
    val passedPawnCreated =
      CollisionRow(
        "Scene.PassedPawnCreated",
        strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get),
        Some(ExplanationClaim.CreatesPassedPawn)
      )
    val fileOpened =
      CollisionRow(
        "Scene.FileOpened",
        strong(SceneFileOpened.write(openedFacts, openedMove).get),
        Some(ExplanationClaim.OpensFile)
      )

    val advanceFacts = facts("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1")
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance =
      CollisionRow(
        "Scene.PawnAdvance",
        strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get),
        Some(ExplanationClaim.AdvancesPassedPawn)
      )

    val threatFacts = facts("k7/8/4P3/8/8/8/8/4K3 w - - 0 1")
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat =
      CollisionRow(
        "Scene.PromotionThreat",
        strong(ScenePromotionThreat.write(threatFacts, threatMove).get),
        Some(ExplanationClaim.CreatesPromotionThreat)
      )

    val promotionFacts = facts("k7/4P3/8/8/8/8/8/4K3 w - - 0 1")
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion =
      CollisionRow(
        "Scene.Promotion",
        strong(ScenePromotion.write(promotionFacts, promotionMove).get),
        Some(ExplanationClaim.PromotesPawn)
      )

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

  test("Stage-5 StoryTable keeps Checkmate stable against every opened collision target"):
    val single = StoryTable.choose(Vector(checkmate().story)).head
    assertEquals(single.role, Role.Lead)
    assertEquals(single.story.scene, Scene.Checkmate)
    assertCheckmateText("single Checkmate bounded renderer text", single)

    collisionTargets.foreach: target =>
      val rows = Vector(checkmate(), target)
      val verdicts = assertStableRows(s"Checkmate vs ${target.label}", rows)
      val mateVerdict = verdicts.find(verdict => rowId(rows, verdict.story) == "Scene.Checkmate").get
      val targetVerdict = verdicts.find(verdict => rowId(rows, verdict.story) == target.label).get

      assertEquals(mateVerdict.story.scene, Scene.Checkmate, target.label)
      assertEquals(mateVerdict.story.writer, Some(StoryWriter.SceneCheckmate), target.label)
      assertEquals(mateVerdict.story.checkmateProof.exists(_.complete), true, target.label)
      assertEquals(mateVerdict.story.checkGivenProof, None, target.label)
      assertEquals(mateVerdict.story.checkEscapedProof, None, target.label)
      if mateVerdict.role == Role.Lead then assertCheckmateText(s"Checkmate renderer for ${target.label}", mateVerdict)
      else assertNoStandaloneText(s"non-lead Checkmate remains silent for ${target.label}", mateVerdict)

      assertEquals(rowId(rows, targetVerdict.story), target.label)
      assertEquals(targetVerdict.story.scene != Scene.Checkmate, true, target.label)
      assertEquals(targetVerdict.story.checkmateProof, None, target.label)

  test("Stage-5 Checkmate CheckGiven and CheckEscaped keep separate homes"):
    val mateFacts = facts(mateFen)
    val mate = checkmate().story
    val checkGiven = strong(SceneCheckGiven.write(mateFacts, mateMove).get)
    val escapeFacts = facts("k3r3/8/8/8/8/8/3B4/4K3 w - - 0 1")
    val escapeMove = Line(Square('d', 2), Square('e', 3))
    val checkEscaped = strong(SceneCheckEscaped.write(escapeFacts, escapeMove).get)

    assertEquals(mate.scene, Scene.Checkmate)
    assertEquals(mate.checkmateProof.exists(_.complete), true)
    assertEquals(mate.checkGivenProof, None)
    assertEquals(mate.checkEscapedProof, None)

    assertEquals(checkGiven.scene, Scene.CheckGiven)
    assertEquals(checkGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGiven.checkmateProof, None)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(checkGiven)).head).flatMap(_.allowedClaim).map(_.key), Some("gives_check"))

    assertEquals(checkEscaped.scene, Scene.CheckEscaped)
    assertEquals(checkEscaped.checkEscapedProof.exists(_.complete), true)
    assertEquals(checkEscaped.checkmateProof, None)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(checkEscaped)).head).flatMap(_.allowedClaim).map(_.key), Some("escapes_check"))

    val rows = Vector(
      CollisionRow("Scene.Checkmate", mate, Some(ExplanationClaim.Checkmates)),
      CollisionRow("Scene.CheckGiven", checkGiven, Some(ExplanationClaim.GivesCheck)),
      CollisionRow("Scene.CheckEscaped", checkEscaped, Some(ExplanationClaim.EscapesCheck))
    )
    val verdicts = assertStableCollision("Checkmate check homes", rows, "Scene.Checkmate")
    verdicts.foreach: verdict =>
      if verdict.story.scene == Scene.Checkmate then assertCheckmateText("Checkmate has bounded renderer text after Stage-7", verdict)

  test("Stage-5 StoryTable cannot create Checkmate and non-lead Checkmate rows produce no text"):
    val noMateRows = collisionTargets.take(4)
    StoryTable.choose(noMateRows.map(_.story)).foreach: verdict =>
      assertEquals(verdict.story.scene == Scene.Checkmate, false)

    val mate = checkmate().story
    val materialLead = collisionTargets.find(_.label == "Scene.Material").get.copy(story =
      collisionTargets.find(_.label == "Scene.Material").get.story.copy(proof = eventProof(100))
    )
    val supportMate = mate.copy(proof = eventProof(90))
    val supportVerdict =
      StoryTable.choose(Vector(materialLead.story, supportMate)).find(_.story.scene == Scene.Checkmate).get
    assertEquals(supportVerdict.role, Role.Support)
    assertNoStandaloneText("support Checkmate", supportVerdict)

    val contextMate = mate.copy(proof = eventProof(20))
    val contextVerdict = StoryTable.choose(Vector(contextMate)).head
    assertEquals(contextVerdict.role, Role.Context)
    assertNoStandaloneText("context Checkmate", contextVerdict)

    val capped = mate.copy(engineCheck = Some(checkmateEngine(mate, EngineCheckStatus.Caps)))
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertNoStandaloneText("capped Checkmate", cappedVerdict)

    val refuted = mate.copy(engineCheck = Some(checkmateEngine(mate, EngineCheckStatus.Supports, before = 220, after = 20)))
    val refutedVerdict = StoryTable.choose(Vector(refuted)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertNoStandaloneText("refuted Checkmate", refutedVerdict)

    val blocked = mate.copy(writer = None)
    val blockedVerdict = StoryTable.choose(Vector(blocked)).head
    assertEquals(blockedVerdict.role, Role.Blocked)
    assertNoStandaloneText("blocked Checkmate", blockedVerdict)
