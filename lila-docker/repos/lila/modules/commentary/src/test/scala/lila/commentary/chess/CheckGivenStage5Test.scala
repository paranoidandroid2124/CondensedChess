package lila.commentary.chess

class CheckGivenStage5Test extends munit.FunSuite:

  private final case class CollisionRow(label: String, story: Story, claim: Option[ExplanationClaim])

  private val checkingFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkingMove = Line(Square('d', 2), Square('e', 2))
  private val replyMove = Line(Square('e', 8), Square('f', 8))

  private def facts: BoardFacts =
    BoardFacts.fromFen(checkingFen).toOption.get

  private def checkGiven: Story =
    strong(SceneCheckGiven.write(facts, checkingMove).get)

  private def eventProof(value: Int): Proof =
    Proof(
      boardProof = value,
      lineProof = value,
      ownerProof = value,
      anchorProof = value,
      routeProof = value,
      persistence = value,
      immediacy = value,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 20,
      kingHeat = 0,
      pieceSupport = value,
      pawnSupport = value,
      sourceFit = 0,
      novelty = value,
      clarity = value
    )

  private def materialProof(value: Int): Proof =
    eventProof(value).copy(forcing = value, conversionPrize = value)

  private def strong(story: Story, value: Int = 99): Story =
    story.copy(proof = eventProof(value))

  private def strongMaterial(story: Story, value: Int = 99): Story =
    story.copy(proof = materialProof(value))

  private def rowId(rows: Vector[CollisionRow], story: Story): String =
    rows.collectFirst { case row if row.story == story => row.label }.getOrElse(s"unknown:$story")

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
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertCheckGivenStage6Boundary(label: String, verdict: Verdict): Unit =
    if verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited && verdict.engineCheckStatus.forall(
        _ == EngineCheckStatus.Supports
      )
    then
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("gives_check"), label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("gives_check"), label)
    else assertNoStandaloneText(label, verdict)

  private def assertStable(label: String, rows: Vector[CollisionRow]): Vector[Verdict] =
    val forward = StoryTable.choose(rows.map(_.story))
    val reverse = StoryTable.choose(rows.reverse.map(_.story))
    val sorted = StoryTable.choose(rows.sortBy(_.label).map(_.story))
    val forwardShape = shape(rows, forward)

    assertEquals(shape(rows, reverse), forwardShape, s"$label reverse input order")
    assertEquals(shape(rows, sorted), forwardShape, s"$label sorted input order")
    assertEquals(forward.count(_.role == Role.Lead), 1, label)
    forward.foreach: verdict =>
      val id = rowId(rows, verdict.story)
      if id == "Scene.CheckGiven" then assertCheckGivenStage6Boundary(s"$label CheckGiven boundary", verdict)
      else if verdict.role == Role.Lead then
        val expectedClaim = rows.find(_.story == verdict.story).flatMap(_.claim)
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), expectedClaim, s"$label $id")
    forward

  private def engineCheck(row: Story, status: EngineCheckStatus, before: Int = 100, after: Int = 100): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(checkingMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def openClaimRows: Vector[CollisionRow] =
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      CollisionRow(
        "Tactic.Fork",
        strong(TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get),
        Some(ExplanationClaim.ForksTwoTargets)
      )

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
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

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense =
      CollisionRow(
        "Scene.Defense",
        strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get),
        Some(ExplanationClaim.DefendsPiece)
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
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

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
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

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
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

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      CollisionRow(
        "Tactic.Skewer",
        strong(
          TacticSkewer
            .write(
              skewerFacts,
              Some(skewerMove),
              Some(Square('e', 1)),
              Some(Square('e', 5)),
              Some(Square('e', 8))
            )
            .get
        ),
        Some(ExplanationClaim.SkewersPieceToPiece)
      )

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop =
      CollisionRow(
        "Scene.PawnStop",
        strong(ScenePawnStop.write(stopFacts, stopMove).get),
        Some(ExplanationClaim.StopsPassedPawnNextAdvance)
      )

    val breakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val breakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak =
      CollisionRow(
        "Scene.PawnBreak",
        strong(ScenePawnBreak.write(breakFacts, breakMove).get),
        Some(ExplanationClaim.ChallengesPawnDirectly)
      )

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
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

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance =
      CollisionRow(
        "Scene.PawnAdvance",
        strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get),
        Some(ExplanationClaim.AdvancesPassedPawn)
      )

    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat =
      CollisionRow(
        "Scene.PromotionThreat",
        strong(ScenePromotionThreat.write(threatFacts, threatMove).get),
        Some(ExplanationClaim.CreatesPromotionThreat)
      )

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion =
      CollisionRow(
        "Scene.Promotion",
        strong(ScenePromotion.write(promotionFacts, promotionMove).get),
        Some(ExplanationClaim.PromotesPawn)
      )

    val blockFacts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val blockMove = Line(Square('g', 5), Square('e', 6))
    val pawnBlock =
      CollisionRow(
        "Scene.PawnBlock",
        strong(ScenePawnBlock.write(blockFacts, blockMove).get),
        Some(ExplanationClaim.BlocksPawn)
      )

    Vector(
      fork,
      material,
      hanging,
      defense,
      discovered,
      pin,
      removeGuard,
      skewer,
      pawnAdvance,
      pawnStop,
      promotionThreat,
      promotion,
      pawnBreak,
      pawnCapture,
      passedPawnCreated,
      fileOpened,
      pawnBlock
    )

  test("Stage-5 StoryTable selects existing Scene.CheckGiven rows with stable ordering only"):
    val check = CollisionRow("Scene.CheckGiven", checkGiven, None)

    openClaimRows.foreach: existing =>
      val verdicts = assertStable(s"CheckGiven vs ${existing.label}", Vector(check, existing))
      assert(verdicts.exists(_.story == check.story), s"${existing.label} collision must keep CheckGiven as input row only")

    val fullRows = check +: openClaimRows
    val fullShape = shape(fullRows, StoryTable.choose(fullRows.map(_.story)))
    val reverseShape = shape(fullRows, StoryTable.choose(fullRows.reverse.map(_.story)))
    val sortedShape = shape(fullRows, StoryTable.choose(fullRows.sortBy(_.label).map(_.story)))
    assertEquals(reverseShape, fullShape, "full collision reverse input order")
    assertEquals(sortedShape, fullShape, "full collision sorted input order")
    assert(StoryTable.choose(openClaimRows.map(_.story)).forall(_.story.scene != Scene.CheckGiven))

  test("Stage-5 existing claim homes keep proof and speech ownership outside CheckGiven"):
    openClaimRows.foreach: row =>
      row.claim.foreach: claim =>
        val verdict = StoryTable.choose(Vector(row.story)).head
        assertEquals(verdict.role, Role.Lead, row.label)
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), Some(claim), row.label)

    val nonCheckFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val nonCheckMove = Line(Square('d', 4), Square('e', 5))
    assertEquals(SceneMaterial.write(nonCheckFacts, nonCheckMove).nonEmpty, true)
    assertEquals(SceneCheckGiven.write(nonCheckFacts, nonCheckMove), None)

  test("Stage-5 CheckGiven Support Context Blocked capped and refuted rows produce no standalone text"):
    val high = checkGiven
    val support = checkGiven.copy(proof = eventProof(98))
    val context = checkGiven.copy(proof = eventProof(20))
    val blocked = checkGiven.copy(writer = None)
    val capped = SceneCheckGiven.withEngineCheck(high, engineCheck(high, EngineCheckStatus.Caps)).get
    val refuted =
      SceneCheckGiven.withEngineCheck(high, engineCheck(high, EngineCheckStatus.Supports, before = 250, after = 25)).get

    val supportVerdicts = StoryTable.choose(Vector(high, support))
    assertEquals(supportVerdicts.map(_.role).toSet, Set(Role.Lead, Role.Support))
    supportVerdicts.foreach: verdict =>
      if verdict.story.scene == Scene.CheckGiven then assertCheckGivenStage6Boundary("support collision", verdict)

    Vector(context, blocked, capped, refuted).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertNoStandaloneText(s"${verdict.role} CheckGiven", verdict)
      if row == context then assertEquals(verdict.role, Role.Context)
      if row == blocked then assertEquals(verdict.role, Role.Blocked)
      if row == capped then assertEquals(verdict.engineStrengthLimited, true)
      if row == refuted then assertEquals(verdict.role, Role.Blocked)

    Vector(
      "mate_threat",
      "checkmate",
      "king_safety",
      "attack",
      "creates_pressure",
      "takes_initiative",
      "forced",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay"
    ).foreach: forbidden =>
      assert(!ExplanationClaim.CheckGivenAllowed.map(_.key).contains(forbidden))
