package lila.commentary.chess

class KingCheckInteractionHardeningTest extends munit.FunSuite:

  private final case class CollisionRow(label: String, story: Story)

  private val checkGivenOnlyFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkGivenOnlyMove = Line(Square('d', 2), Square('e', 2))
  private val checkEscapedOnlyFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val checkEscapedOnlyMove = Line(Square('e', 1), Square('f', 1))
  private val crossCheckFen = "k3r3/3P4/8/8/8/8/8/4K3 w - - 0 1"
  private val crossCheckMove = Line(Square('d', 7), Square('e', 8))
  private val replyMove = Line(Square('a', 8), Square('b', 8))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

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

  private def strong(story: Story): Story =
    story.copy(proof = eventProof(99))

  private def materialProof(value: Int): Proof =
    eventProof(value).copy(forcing = value, conversionPrize = value)

  private def strongMaterial(story: Story): Story =
    story.copy(proof = materialProof(99))

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

  private def assertStable(label: String, rows: Vector[CollisionRow]): Vector[Verdict] =
    val forward = StoryTable.choose(rows.map(_.story))
    val reverse = StoryTable.choose(rows.reverse.map(_.story))
    val sorted = StoryTable.choose(rows.sortBy(_.label).map(_.story))
    val expected = shape(rows, forward)

    assertEquals(shape(rows, reverse), expected, s"$label reverse input order")
    assertEquals(shape(rows, sorted), expected, s"$label sorted input order")
    forward

  private def assertNoStandaloneClaim(label: String, verdict: Verdict): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertNoRenderedText(label: String, verdict: Verdict): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertClaim(label: String, verdict: Verdict, key: String): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some(key), label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some(key), label)

  private def engineCheck(
      facts: BoardFacts,
      row: Story,
      status: EngineCheckStatus,
      before: Int = 100,
      after: Int = 100
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = row.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def rawEngineEvidence(move: Line, status: EngineCheckStatus = EngineCheckStatus.Supports): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(move),
      engineLine = Some(EngineLine(Vector(move))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(100)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def routeMissingEngineCheck(row: Story): EngineCheck =
    EngineCheck.fromStory(
      facts = facts(crossCheckFen),
      story = Some(row),
      engineLine = Some(EngineLine(Vector(replyMove))),
      replyLine = Some(EngineLine(Vector(replyMove))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(100)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

  private val forbiddenEngineWording: Vector[String] =
    Vector(
      "engine says",
      "eval",
      "raw pv",
      "best move",
      "only move",
      "forced move",
      "winning",
      "decisive",
      "no counterplay",
      "mate threat",
      "checkmate",
      "safe king",
      "refutes attack"
    )

  private def assertNoForbiddenEngineWording(label: String, verdict: Verdict): Unit =
    ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).foreach: rendered =>
      val text = rendered.text.toLowerCase
      forbiddenEngineWording.foreach: phrase =>
        assert(!text.contains(phrase), s"$label rendered forbidden engine wording: $phrase in $text")

  private def leadPlanAndRendered(row: Story): (Verdict, ExplanationPlan, RenderedLine) =
    val verdict = StoryTable.choose(Vector(row)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    (verdict, plan, rendered)

  private def attachKingCheckEngine(row: Story, check: EngineCheck): Option[Story] =
    row.writer match
      case Some(StoryWriter.SceneCheckGiven)   => SceneCheckGiven.withEngineCheck(row, check)
      case Some(StoryWriter.SceneCheckEscaped) => SceneCheckEscaped.withEngineCheck(row, check)
      case _                                   => None

  private def crossCheckRows: (BoardFacts, Story, Story) =
    val crossFacts = facts(crossCheckFen)
    val checkGiven = strong(SceneCheckGiven.write(crossFacts, crossCheckMove).get)
    val checkEscaped = strong(SceneCheckEscaped.write(crossFacts, crossCheckMove).get)
    (crossFacts, checkGiven, checkEscaped)

  private def openedClaimRows: Vector[(CollisionRow, ExplanationClaim)] =
    val forkFacts = facts("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      CollisionRow(
        "Tactic.Fork",
        strong(TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get)
      ) -> ExplanationClaim.ForksTwoTargets

    val materialFacts = facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material =
      CollisionRow("Scene.Material", strongMaterial(SceneMaterial.write(materialFacts, materialMove).get)) ->
        ExplanationClaim.MaterialBalanceChanges
    val hanging =
      CollisionRow("Tactic.Hanging", strongMaterial(TacticHanging.write(materialFacts, materialMove).get)) ->
        ExplanationClaim.CanWinPiece

    val defenseFacts = facts("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense =
      CollisionRow("Scene.Defense", strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get)) ->
        ExplanationClaim.DefendsPiece

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
      ) -> ExplanationClaim.RevealsAttackOnPiece

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
      ) -> ExplanationClaim.PinsPiece

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
      ) -> ExplanationClaim.RemovesDefender

    val skewerFacts = facts("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
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
        )
      ) -> ExplanationClaim.SkewersPieceToPiece

    val advanceFacts = facts("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1")
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance =
      CollisionRow("Scene.PawnAdvance", strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get)) ->
        ExplanationClaim.AdvancesPassedPawn

    val stopFacts = facts("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1")
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop =
      CollisionRow("Scene.PawnStop", strong(ScenePawnStop.write(stopFacts, stopMove).get)) ->
        ExplanationClaim.StopsPassedPawnNextAdvance

    val threatFacts = facts("k7/8/4P3/8/8/8/8/4K3 w - - 0 1")
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat =
      CollisionRow("Scene.PromotionThreat", strong(ScenePromotionThreat.write(threatFacts, threatMove).get)) ->
        ExplanationClaim.CreatesPromotionThreat

    val promotionFacts = facts("k7/4P3/8/8/8/8/8/4K3 w - - 0 1")
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion =
      CollisionRow("Scene.Promotion", strong(ScenePromotion.write(promotionFacts, promotionMove).get)) ->
        ExplanationClaim.PromotesPawn

    val breakFacts = facts("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1")
    val breakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak =
      CollisionRow("Scene.PawnBreak", strong(ScenePawnBreak.write(breakFacts, breakMove).get)) ->
        ExplanationClaim.ChallengesPawnDirectly

    val openedFacts = facts("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1")
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture =
      CollisionRow("Scene.PawnCapture", strong(ScenePawnCapture.write(openedFacts, openedMove).get)) ->
        ExplanationClaim.CapturesPawn
    val passedPawnCreated =
      CollisionRow("Scene.PassedPawnCreated", strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get)) ->
        ExplanationClaim.CreatesPassedPawn
    val fileOpened =
      CollisionRow("Scene.FileOpened", strong(SceneFileOpened.write(openedFacts, openedMove).get)) ->
        ExplanationClaim.OpensFile

    val blockFacts = facts("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1")
    val blockMove = Line(Square('g', 5), Square('e', 6))
    val pawnBlock =
      CollisionRow("Scene.PawnBlock", strong(ScenePawnBlock.write(blockFacts, blockMove).get)) ->
        ExplanationClaim.BlocksPawn

    Vector(
      material,
      hanging,
      fork,
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

  test("KCIH-2 fixture categories stay limited to existing CheckGiven and CheckEscaped rows"):
    val checkGivenFacts = facts(checkGivenOnlyFen)
    val checkGivenOnly = SceneCheckGiven.write(checkGivenFacts, checkGivenOnlyMove).get
    assertEquals(checkGivenOnly.scene, Scene.CheckGiven)
    assertEquals(checkGivenOnly.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(checkGivenOnly.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGivenOnly.checkEscapedProof, None)
    assertEquals(SceneCheckEscaped.write(checkGivenFacts, checkGivenOnlyMove), None)
    assertClaim("CheckGiven only", StoryTable.choose(Vector(strong(checkGivenOnly))).head, "gives_check")

    val checkEscapedFacts = facts(checkEscapedOnlyFen)
    val checkEscapedOnly = SceneCheckEscaped.write(checkEscapedFacts, checkEscapedOnlyMove).get
    assertEquals(checkEscapedOnly.scene, Scene.CheckEscaped)
    assertEquals(checkEscapedOnly.writer, Some(StoryWriter.SceneCheckEscaped))
    assertEquals(checkEscapedOnly.checkEscapedProof.exists(_.complete), true)
    assertEquals(checkEscapedOnly.checkGivenProof, None)
    assertEquals(SceneCheckGiven.write(checkEscapedFacts, checkEscapedOnlyMove), None)
    assertClaim("CheckEscaped only", StoryTable.choose(Vector(strong(checkEscapedOnly))).head, "escapes_check")

  test("KCIH-2 cross-check fixtures keep proof writer target route and speech ownership separate"):
    val (_, checkGiven, checkEscaped) = crossCheckRows

    assertEquals(checkGiven.scene, Scene.CheckGiven)
    assertEquals(checkGiven.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(checkGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGiven.checkEscapedProof, None)
    assertEquals(checkGiven.target, checkGiven.checkGivenProof.flatMap(_.rivalKingSquareAfter))
    assertEquals(checkGiven.anchor, checkGiven.checkGivenProof.flatMap(_.originSquare))
    assertEquals(checkGiven.route, checkGiven.checkGivenProof.flatMap(_.checkMove))

    assertEquals(checkEscaped.scene, Scene.CheckEscaped)
    assertEquals(checkEscaped.writer, Some(StoryWriter.SceneCheckEscaped))
    assertEquals(checkEscaped.checkEscapedProof.exists(_.complete), true)
    assertEquals(checkEscaped.checkGivenProof, None)
    assertEquals(checkEscaped.target, checkEscaped.checkEscapedProof.flatMap(_.afterKingSquare))
    assertEquals(checkEscaped.anchor, checkEscaped.checkEscapedProof.flatMap(_.originSquare))
    assertEquals(checkEscaped.route, checkEscaped.checkEscapedProof.flatMap(_.escapeMove))
    assert(checkGiven.target != checkEscaped.target, "cross-check rows must not borrow each other's target")

    val rows = Vector(CollisionRow("Scene.CheckGiven", checkGiven), CollisionRow("Scene.CheckEscaped", checkEscaped))
    val verdicts = assertStable("cross-check CheckGiven and CheckEscaped", rows)
    assertEquals(verdicts.count(_.role == Role.Lead), 1)
    verdicts.foreach: verdict =>
      rowId(rows, verdict.story) match
        case "Scene.CheckGiven" if verdict.role == Role.Lead => assertClaim("lead CheckGiven", verdict, "gives_check")
        case "Scene.CheckEscaped" if verdict.role == Role.Lead => assertClaim("lead CheckEscaped", verdict, "escapes_check")
        case label => assertNoStandaloneClaim(s"$label non-lead", verdict)

    assertClaim("single CheckGiven", StoryTable.choose(Vector(checkGiven)).head, "gives_check")
    assertClaim("single CheckEscaped", StoryTable.choose(Vector(checkEscaped)).head, "escapes_check")

  test("KCIH-2 contaminated sidecars and mixed writers are blocked or silent"):
    val (_, checkGiven, checkEscaped) = crossCheckRows
    val contaminatedGiven = checkGiven.copy(checkEscapedProof = checkEscaped.checkEscapedProof)
    val contaminatedEscaped = checkEscaped.copy(checkGivenProof = checkGiven.checkGivenProof)
    val mixedGivenWriter = checkGiven.copy(writer = Some(StoryWriter.SceneCheckEscaped))
    val mixedEscapedWriter = checkEscaped.copy(writer = Some(StoryWriter.SceneCheckGiven))

    Vector(
      "CheckGiven row with contaminated CheckEscaped sidecar" -> contaminatedGiven,
      "CheckEscaped row with contaminated CheckGiven sidecar" -> contaminatedEscaped,
      "CheckGiven row with CheckEscaped writer" -> mixedGivenWriter,
      "CheckEscaped row with CheckGiven writer" -> mixedEscapedWriter
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertNoStandaloneClaim(label, verdict)

    val rows = Vector(
      CollisionRow("contaminated CheckGiven", contaminatedGiven),
      CollisionRow("contaminated CheckEscaped", contaminatedEscaped),
      CollisionRow("valid CheckGiven", checkGiven),
      CollisionRow("valid CheckEscaped", checkEscaped)
    )
    val verdicts = assertStable("contaminated rows beside valid rows", rows)
    verdicts.foreach: verdict =>
      if rowId(rows, verdict.story).startsWith("contaminated") then assertNoStandaloneClaim(rowId(rows, verdict.story), verdict)

  test("KCIH-2 capped and refuted cross-check fixtures leave the uncapped valid sibling bounded"):
    val (crossFacts, checkGiven, checkEscaped) = crossCheckRows
    val cappedGiven = SceneCheckGiven.withEngineCheck(checkGiven, engineCheck(crossFacts, checkGiven, EngineCheckStatus.Caps)).get
    val cappedEscaped =
      SceneCheckEscaped.withEngineCheck(checkEscaped, engineCheck(crossFacts, checkEscaped, EngineCheckStatus.Caps)).get
    val refutedGiven =
      SceneCheckGiven.withEngineCheck(
        checkGiven,
        engineCheck(crossFacts, checkGiven, EngineCheckStatus.Supports, before = 250, after = 25)
      ).get
    val refutedEscaped =
      SceneCheckEscaped.withEngineCheck(
        checkEscaped,
        engineCheck(crossFacts, checkEscaped, EngineCheckStatus.Supports, before = 250, after = 25)
      ).get

    Vector(
      ("capped CheckGiven next to uncapped CheckEscaped", cappedGiven, checkEscaped, "Scene.CheckEscaped", "escapes_check"),
      ("capped CheckEscaped next to uncapped CheckGiven", cappedEscaped, checkGiven, "Scene.CheckGiven", "gives_check"),
      ("refuted CheckGiven next to valid CheckEscaped", refutedGiven, checkEscaped, "Scene.CheckEscaped", "escapes_check"),
      ("refuted CheckEscaped next to valid CheckGiven", refutedEscaped, checkGiven, "Scene.CheckGiven", "gives_check")
    ).foreach: (label, limited, valid, expectedLead, expectedKey) =>
      val rows = Vector(CollisionRow("limited", limited), CollisionRow(expectedLead, valid))
      val verdicts = assertStable(label, rows)
      val lead = verdicts.find(_.role == Role.Lead).get

      assertEquals(rowId(rows, lead.story), expectedLead, label)
      assertClaim(label, lead, expectedKey)
      verdicts.filter(_.story == limited).foreach(assertNoStandaloneClaim(s"$label limited", _))

  test("KCIH-3 StoryTable orders existing King Check rows without creating them"):
    val (_, checkGiven, checkEscaped) = crossCheckRows
    val nonKingRows = openedClaimRows.map(_._1)

    assert(StoryTable.choose(nonKingRows.map(_.story)).forall(verdict => verdict.story.scene != Scene.CheckGiven))
    assert(StoryTable.choose(nonKingRows.map(_.story)).forall(verdict => verdict.story.scene != Scene.CheckEscaped))

    val checkGivenVerdict = StoryTable.choose(Vector(checkGiven)).head
    assertEquals(checkGivenVerdict.story.scene, Scene.CheckGiven)
    assertClaim("ordered CheckGiven", checkGivenVerdict, "gives_check")

    val checkEscapedVerdict = StoryTable.choose(Vector(checkEscaped)).head
    assertEquals(checkEscapedVerdict.story.scene, Scene.CheckEscaped)
    assertClaim("ordered CheckEscaped", checkEscapedVerdict, "escapes_check")

  test("KCIH-3 duplicate King Check meanings order deterministically"):
    val (_, checkGiven, checkEscaped) = crossCheckRows
    val weakerGiven = checkGiven.copy(proof = eventProof(98))
    val weakerEscaped = checkEscaped.copy(proof = eventProof(98))

    Vector(
      "duplicate CheckGiven" -> Vector(CollisionRow("strong CheckGiven", checkGiven), CollisionRow("weaker CheckGiven", weakerGiven)),
      "duplicate CheckEscaped" -> Vector(CollisionRow("strong CheckEscaped", checkEscaped), CollisionRow("weaker CheckEscaped", weakerEscaped))
    ).foreach: (label, rows) =>
      val verdicts = assertStable(label, rows)
      assertEquals(verdicts.map(_.role).toSet, Set(Role.Lead, Role.Support), label)
      assertEquals(verdicts.count(_.role == Role.Lead), 1, label)
      verdicts.filter(_.role != Role.Lead).foreach(assertNoStandaloneClaim(s"$label non-lead", _))

  test("KCIH-3 King Check collisions keep sibling proof and speech homes"):
    val (_, checkGiven, checkEscaped) = crossCheckRows
    val kingRows =
      Vector(
        CollisionRow("Scene.CheckGiven", checkGiven) -> "gives_check",
        CollisionRow("Scene.CheckEscaped", checkEscaped) -> "escapes_check"
      )

    openedClaimRows.foreach: (opened, openedClaim) =>
      assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(opened.story)).head).flatMap(_.allowedClaim), Some(openedClaim), opened.label)

      kingRows.foreach: (king, kingClaim) =>
        val rows = Vector(king, opened)
        val verdicts = assertStable(s"${king.label} vs ${opened.label}", rows)

        verdicts.foreach: verdict =>
          val id = rowId(rows, verdict.story)
          if id == opened.label && verdict.role == Role.Lead then
            assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), Some(openedClaim), id)
          else if id == king.label && verdict.role == Role.Lead then assertClaim(id, verdict, kingClaim)
          else if id == king.label then assertNoStandaloneClaim(s"$id non-lead", verdict)
          else assertNoRenderedText(s"$id non-lead", verdict)

  test("KCIH-3 Support Context Blocked capped and refuted King Check rows have no standalone text"):
    val (crossFacts, checkGiven, checkEscaped) = crossCheckRows

    val scenarios = Vector(
      "CheckGiven" -> checkGiven,
      "CheckEscaped" -> checkEscaped
    )

    scenarios.foreach: (label, row) =>
      val support = row.copy(proof = eventProof(98))
      val context = row.copy(proof = eventProof(20))
      val blocked = row.copy(writer = None)
      val capped =
        row.writer match
          case Some(StoryWriter.SceneCheckGiven) =>
            SceneCheckGiven.withEngineCheck(row, engineCheck(crossFacts, row, EngineCheckStatus.Caps)).get
          case Some(StoryWriter.SceneCheckEscaped) =>
            SceneCheckEscaped.withEngineCheck(row, engineCheck(crossFacts, row, EngineCheckStatus.Caps)).get
          case _ => fail(s"unexpected King Check writer for $label")
      val refuted =
        row.writer match
          case Some(StoryWriter.SceneCheckGiven) =>
            SceneCheckGiven
              .withEngineCheck(row, engineCheck(crossFacts, row, EngineCheckStatus.Supports, before = 250, after = 25))
              .get
          case Some(StoryWriter.SceneCheckEscaped) =>
            SceneCheckEscaped
              .withEngineCheck(row, engineCheck(crossFacts, row, EngineCheckStatus.Supports, before = 250, after = 25))
              .get
          case _ => fail(s"unexpected King Check writer for $label")

      val supportVerdicts = StoryTable.choose(Vector(row, support))
      assertEquals(supportVerdicts.map(_.role).toSet, Set(Role.Lead, Role.Support), label)
      supportVerdicts.filter(_.role == Role.Support).foreach(assertNoStandaloneClaim(s"$label Support", _))

      Vector(
        "Context" -> context,
        "Blocked" -> blocked,
        "capped" -> capped,
        "refuted" -> refuted
      ).foreach: (roleLabel, story) =>
        val verdict = StoryTable.choose(Vector(story)).head
        assertNoStandaloneClaim(s"$label $roleLabel", verdict)
        if roleLabel == "Context" then assertEquals(verdict.role, Role.Context, label)
        if roleLabel == "Blocked" then assertEquals(verdict.role, Role.Blocked, label)
        if roleLabel == "capped" then assertEquals(verdict.engineStrengthLimited, true, label)
        if roleLabel == "refuted" then assertEquals(verdict.role, Role.Blocked, label)

  test("KCIH-4 EngineCheck cannot create or attach to incomplete or unbound King Check rows"):
    val (crossFacts, checkGiven, checkEscaped) = crossCheckRows
    val rawEvidence = rawEngineEvidence(crossCheckMove)

    assertEquals(rawEvidence.storyBound, false)
    assertEquals(rawEvidence.publicClaimAllowed, false)
    assertEquals(SceneCheckGiven.withEngineCheck(checkGiven, rawEvidence), None)
    assertEquals(SceneCheckEscaped.withEngineCheck(checkEscaped, rawEvidence), None)

    assertEquals(
      SceneCheckGiven.withEngineCheck(
        checkGiven.copy(checkGivenProof = None),
        engineCheck(crossFacts, checkGiven, EngineCheckStatus.Supports)
      ),
      None
    )
    assertEquals(
      SceneCheckEscaped.withEngineCheck(
        checkEscaped.copy(checkEscapedProof = None),
        engineCheck(crossFacts, checkEscaped, EngineCheckStatus.Supports)
      ),
      None
    )

    assertEquals(SceneCheckGiven.withEngineCheck(checkGiven, routeMissingEngineCheck(checkGiven)), None)
    assertEquals(SceneCheckEscaped.withEngineCheck(checkEscaped, routeMissingEngineCheck(checkEscaped)), None)

    val staleFacts = facts(checkGivenOnlyFen)
    assertEquals(
      SceneCheckGiven.withEngineCheck(
        checkGiven,
        engineCheck(staleFacts, checkGiven, EngineCheckStatus.Supports)
      ),
      None
    )
    assertEquals(
      SceneCheckEscaped.withEngineCheck(
        checkEscaped,
        engineCheck(staleFacts, checkEscaped, EngineCheckStatus.Supports)
      ),
      None
    )

  test("KCIH-4 EngineCheck support cap refute and unknown stay bounded for King Check rows"):
    val (crossFacts, checkGiven, checkEscaped) = crossCheckRows

    Vector(
      ("KCIH-4 CheckGiven", checkGiven, "gives_check"),
      ("KCIH-4 CheckEscaped", checkEscaped, "escapes_check")
    ).foreach: (label, row, claimKey) =>
      val supported = attachKingCheckEngine(row, engineCheck(crossFacts, row, EngineCheckStatus.Supports)).get
      val supportedVerdict = StoryTable.choose(Vector(supported)).head
      assertEquals(supported.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
      assertEquals(supported.engineCheck.exists(_.publicClaimAllowed), false)
      assertClaim(label, supportedVerdict, claimKey)
      assertNoForbiddenEngineWording(s"$label supported", supportedVerdict)

      val capped = attachKingCheckEngine(row, engineCheck(crossFacts, row, EngineCheckStatus.Caps)).get
      val cappedVerdict = StoryTable.choose(Vector(capped)).head
      assertEquals(capped.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
      assertEquals(capped.engineCheck.exists(_.publicClaimAllowed), false)
      assertEquals(cappedVerdict.engineStrengthLimited, true)
      assertNoStandaloneClaim(s"$label capped", cappedVerdict)
      assertNoForbiddenEngineWording(s"$label capped", cappedVerdict)

      val refuted =
        attachKingCheckEngine(
          row,
          engineCheck(crossFacts, row, EngineCheckStatus.Supports, before = 250, after = 25)
        ).get
      val refutedVerdict = StoryTable.choose(Vector(refuted)).head
      assertEquals(refuted.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
      assertEquals(refuted.engineCheck.exists(_.publicClaimAllowed), false)
      assertEquals(refutedVerdict.role, Role.Blocked)
      assertNoStandaloneClaim(s"$label refuted", refutedVerdict)
      assertNoForbiddenEngineWording(s"$label refuted", refutedVerdict)

      val unknown = attachKingCheckEngine(row, engineCheck(crossFacts, row, EngineCheckStatus.Unknown)).get
      val unknownVerdict = StoryTable.choose(Vector(unknown)).head
      assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
      assertEquals(unknown.engineCheck.exists(_.publicClaimAllowed), false)
      assertNoForbiddenEngineWording(s"$label unknown", unknownVerdict)

  test("KCIH-5 ExplanationPlan lowers only selected uncapped King Check Leads to their own claim keys"):
    val (crossFacts, checkGiven, checkEscaped) = crossCheckRows

    val (_, givenPlan, givenRendered) = leadPlanAndRendered(checkGiven)
    assertEquals(givenPlan.scene, Scene.CheckGiven)
    assertEquals(givenPlan.allowedClaim.map(_.key), Some("gives_check"))
    assertEquals(givenRendered.claimKey, "gives_check")
    assertEquals(givenRendered.text, s"${givenPlan.routeSan.get} gives check.")
    assertEquals(givenPlan.allowedClaim.exists(_.key == "escapes_check"), false)

    val (_, escapedPlan, escapedRendered) = leadPlanAndRendered(checkEscaped)
    assertEquals(escapedPlan.scene, Scene.CheckEscaped)
    assertEquals(escapedPlan.allowedClaim.map(_.key), Some("escapes_check"))
    assertEquals(escapedRendered.claimKey, "escapes_check")
    assertEquals(escapedRendered.text, s"${escapedPlan.routeSan.get} gets out of check.")
    assertEquals(escapedPlan.allowedClaim.exists(_.key == "gives_check"), false)

    Vector(
      "CheckGiven" -> checkGiven,
      "CheckEscaped" -> checkEscaped
    ).foreach: (label, row) =>
      val support = row.copy(proof = eventProof(98))
      StoryTable.choose(Vector(row, support)).filter(_.role == Role.Support).foreach: verdict =>
        assertNoStandaloneClaim(s"KCIH-5 $label Support", verdict)

      val context = row.copy(proof = eventProof(20))
      val blocked = row.copy(writer = None)
      val capped = attachKingCheckEngine(row, engineCheck(crossFacts, row, EngineCheckStatus.Caps)).get
      val refuted =
        attachKingCheckEngine(
          row,
          engineCheck(crossFacts, row, EngineCheckStatus.Supports, before = 250, after = 25)
        ).get

      Vector(
        "Context" -> context,
        "Blocked" -> blocked,
        "capped" -> capped,
        "refuted" -> refuted
      ).foreach: (roleLabel, story) =>
        assertNoStandaloneClaim(s"KCIH-5 $label $roleLabel", StoryTable.choose(Vector(story)).head)

  test("KCIH-5 renderer takes ExplanationPlan only and keeps King Check text bounded"):
    val (_, checkGiven, checkEscaped) = crossCheckRows

    val (_, givenPlan, givenRendered) = leadPlanAndRendered(checkGiven)
    val (_, escapedPlan, escapedRendered) = leadPlanAndRendered(checkEscaped)
    assertEquals(givenRendered.text, s"${givenPlan.routeSan.get} gives check.")
    assertEquals(escapedRendered.text, s"${escapedPlan.routeSan.get} gets out of check.")
    assertEquals(givenRendered.forbiddenCheckPassed, true)
    assertEquals(escapedRendered.forbiddenCheckPassed, true)

    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector
    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))

    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.flatMap(_.getParameterTypes.map(_.getSimpleName)).toSet
    Vector(
      "Story",
      "Proof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "CheckGivenProof",
      "CheckEscapedProof",
      "ProofFailure"
    ).foreach: forbidden =>
      assert(!rendererParameterNames.contains(forbidden), s"renderer accepted raw downstream input: $forbidden")

    val renderedSurfaceNames =
      classOf[RenderedLine].getDeclaredMethods.map(_.getName).toSet ++ classOf[RenderedLine].getDeclaredFields.map(_.getName).toSet
    Vector("story", "proof", "boardFacts", "engineCheck", "proofFailures", "sourceRows", "rawPv").foreach: forbidden =>
      assert(!renderedSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), s"RenderedLine exposed $forbidden")

  test("KCIH-5 LLM smoke prompt and checker stay rephrase-only for King Check rows"):
    val (_, checkGiven, checkEscaped) = crossCheckRows

    Vector(
      "CheckGiven" -> leadPlanAndRendered(checkGiven),
      "CheckEscaped" -> leadPlanAndRendered(checkEscaped)
    ).foreach: (label, downstream) =>
      val (_, plan, rendered) = downstream
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
      Vector(
        "renderedText:",
        s"claimKey: ${rendered.claimKey}",
        s"strength: ${rendered.strength}",
        "forbiddenWording:",
        "instruction: Rephrase only. Do not add chess facts."
      ).foreach: field =>
        assert(prompt.contains(field), s"KCIH-5 $label prompt omitted bounded field $field")

      Vector(
        "raw Story",
        "CheckGivenProof",
        "CheckEscapedProof",
        "BoardFacts",
        "EngineCheck",
        "EngineLine",
        "EngineEval",
        "raw PV",
        "proofFailures",
        "source row",
        "source rows"
      ).foreach: forbiddenInput =>
        assert(!prompt.toLowerCase.contains(forbiddenInput.toLowerCase), s"KCIH-5 $label prompt exposed $forbiddenInput")

      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

      Vector(
        "new move" -> s"${rendered.text} Qh5 is next.",
        "new line" -> s"${rendered.text} The line continues with Qh5.",
        "mate" -> s"${rendered.text} It threatens mate.",
        "checkmate" -> s"${rendered.text} It is checkmate.",
        "king safety" -> s"${rendered.text} The king is safe.",
        "attack" -> s"${rendered.text} It creates an attack.",
        "pressure" -> s"${rendered.text} It creates pressure.",
        "initiative" -> s"${rendered.text} It takes initiative.",
        "force" -> s"${rendered.text} It forces a reply.",
        "best" -> s"${rendered.text} It is the best move.",
        "only" -> s"${rendered.text} It is the only move.",
        "winning" -> s"${rendered.text} It is winning.",
        "decisive" -> s"${rendered.text} It is decisive.",
        "no-counterplay" -> s"${rendered.text} There is no counterplay.",
        "engine explanation" -> s"${rendered.text} The engine says +1.2.",
        "escape method detail" -> s"${rendered.text} The escape method is a king move."
      ).foreach: (violation, output) =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"KCIH-5 $label accepted $violation")

  test("KCIH-7 closeout keeps King Check ownership expression and no-go surfaces closed"):
    val (crossFacts, checkGiven, checkEscaped) = crossCheckRows

    assertEquals(checkGiven.scene, Scene.CheckGiven)
    assertEquals(checkGiven.writer, Some(StoryWriter.SceneCheckGiven))
    assert(checkGiven.checkGivenProof.exists(_.complete))
    assertEquals(checkGiven.checkEscapedProof, None)
    val (_, givenPlan, givenRendered) = leadPlanAndRendered(checkGiven)
    assertEquals(givenPlan.allowedClaim.map(_.key), Some("gives_check"))
    assertEquals(givenRendered.claimKey, "gives_check")
    assertEquals(givenRendered.text, s"${givenPlan.routeSan.get} gives check.")

    assertEquals(checkEscaped.scene, Scene.CheckEscaped)
    assertEquals(checkEscaped.writer, Some(StoryWriter.SceneCheckEscaped))
    assert(checkEscaped.checkEscapedProof.exists(_.complete))
    assertEquals(checkEscaped.checkGivenProof, None)
    val (_, escapedPlan, escapedRendered) = leadPlanAndRendered(checkEscaped)
    assertEquals(escapedPlan.allowedClaim.map(_.key), Some("escapes_check"))
    assertEquals(escapedRendered.claimKey, "escapes_check")
    assertEquals(escapedRendered.text, s"${escapedPlan.routeSan.get} gets out of check.")

    assertEquals(givenPlan.allowedClaim.exists(_.key == "escapes_check"), false)
    assertEquals(escapedPlan.allowedClaim.exists(_.key == "gives_check"), false)
    assertEquals(checkGiven.route, checkEscaped.route)
    assert(checkGiven.checkGivenProof.exists(proof => proof.checkMove == checkGiven.route && proof.sameBoardProof))
    assert(checkEscaped.checkEscapedProof.exists(proof => proof.escapeMove == checkEscaped.route && proof.sameBoardProof))

    Vector("king_escapes_check", "blocks_check", "captures_checker").foreach: key =>
      assert(ExplanationClaim.CheckEscapedForbiddenKeys.contains(key), s"escape method detail must stay forbidden: $key")
      assertEquals(ExplanationClaim.CheckEscapedAllowed.map(_.key).contains(key), false)

    val closedOutputs = Vector(
      "mate threat" -> "This threatens mate.",
      "checkmate" -> "This is checkmate.",
      "king safety" -> "This fixes king safety.",
      "attack" -> "This creates an attack.",
      "pressure" -> "This creates pressure.",
      "initiative" -> "This takes initiative.",
      "forced" -> "This is forced.",
      "best" -> "This is the best move.",
      "only" -> "This is the only move.",
      "winning" -> "This is winning.",
      "decisive" -> "This is decisive.",
      "no-counterplay" -> "There is no counterplay."
    )
    Vector("CheckGiven" -> (givenPlan, givenRendered), "CheckEscaped" -> (escapedPlan, escapedRendered)).foreach:
      (label, downstream) =>
        val (plan, rendered) = downstream
        closedOutputs.foreach: (meaning, suffix) =>
          assertEquals(
            LlmNarrationSmoke.check(plan, rendered, s"${rendered.text} $suffix").accepted,
            false,
            s"KCIH-7 $label opened $meaning"
          )

    Vector(
      "CheckGiven" -> checkGiven,
      "CheckEscaped" -> checkEscaped
    ).foreach: (label, row) =>
      val support = row.copy(proof = eventProof(98))
      StoryTable.choose(Vector(row, support)).filter(_.role == Role.Support).foreach: verdict =>
        assertNoStandaloneClaim(s"KCIH-7 $label Support", verdict)

      val context = row.copy(proof = eventProof(20))
      val blocked = row.copy(writer = None)
      val capped = attachKingCheckEngine(row, engineCheck(crossFacts, row, EngineCheckStatus.Caps)).get
      val refuted =
        attachKingCheckEngine(row, engineCheck(crossFacts, row, EngineCheckStatus.Supports, before = 250, after = 25)).get

      Vector(
        "Context" -> context,
        "Blocked" -> blocked,
        "capped" -> capped,
        "refuted" -> refuted
      ).foreach: (roleLabel, story) =>
        assertNoStandaloneClaim(s"KCIH-7 $label $roleLabel", StoryTable.choose(Vector(story)).head)
