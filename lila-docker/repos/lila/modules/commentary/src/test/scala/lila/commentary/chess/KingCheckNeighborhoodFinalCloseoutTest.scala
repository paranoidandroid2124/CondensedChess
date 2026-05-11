package lila.commentary.chess

class KingCheckNeighborhoodFinalCloseoutTest extends munit.FunSuite:

  private case class Kcnfc3Row(label: String, story: Story)

  private val checkGivenFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkGivenMove = Line(Square('d', 2), Square('e', 2))
  private val checkEscapedFen = "k3r3/8/8/8/8/8/8/4K3 w - - 0 1"
  private val checkEscapedMove = Line(Square('e', 1), Square('f', 1))
  private val checkmateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val checkmateMove = Line(Square('c', 7), Square('b', 7))
  private val crossCheckFen = "4r3/8/8/7k/8/8/3Q4/4K3 w - - 0 1"
  private val crossCheckMove = Line(Square('d', 2), Square('e', 2))
  private val stalemateFen = "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
  private val noLegalNoCheckFen = "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"

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

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def selected(row: Story): Verdict =
    StoryTable.choose(Vector(row)).head

  private def plan(row: Story): Option[ExplanationPlan] =
    ExplanationPlan.fromSelected(selected(row))

  private def claimKey(row: Story): Option[String] =
    plan(row).flatMap(_.allowedClaim).map(_.key)

  private def rendered(row: Story): Option[RenderedLine] =
    plan(row).flatMap(DeterministicRenderer.fromPlan)

  private def assertNoStandaloneText(label: String, row: Story): Unit =
    assertEquals(ExplanationPlan.fromSelected(selected(row)), None, label)
    assertEquals(rendered(row), None, label)

  private def assertNoVerdictText(label: String, verdict: Verdict): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertNoLowering(label: String, row: Story): Unit =
    val verdict = selected(row)
    assertEquals(verdict.role, Role.Blocked, label)
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def kcnfc3Shape(verdicts: Vector[Verdict]) =
    verdicts.map: verdict =>
      (
        verdict.story.scene,
        verdict.story.tactic,
        verdict.story.plan,
        verdict.story.side,
        verdict.story.rival,
        verdict.story.route.map(line => (line.from.index, line.to.index)),
        verdict.story.target.map(_.index),
        verdict.story.anchor.map(_.index),
        verdict.role,
        verdict.leadAllowed,
        verdict.engineCheckStatus,
        verdict.engineStrengthLimited,
        ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
      )

  private def assertStableStoryTable(label: String, rows: Vector[Story]): Vector[Verdict] =
    val forward = StoryTable.choose(rows)
    val reverse = StoryTable.choose(rows.reverse)
    val sorted = StoryTable.choose(rows.sortBy(row => (row.scene.ordinal, row.side.ordinal, row.route.fold(0)(_.from.index))))
    val forwardShape = kcnfc3Shape(forward)

    assertEquals(kcnfc3Shape(reverse), forwardShape, s"$label reverse input order")
    assertEquals(kcnfc3Shape(sorted), forwardShape, s"$label sorted input order")
    forward.foreach: verdict =>
      assert(rows.contains(verdict.story), s"$label StoryTable returned a row it did not receive: ${verdict.story.scene}")
    forward

  private def kingCheckRows: Vector[Kcnfc3Row] =
    Vector(
      Kcnfc3Row("CheckGiven", strong(SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get)),
      Kcnfc3Row("CheckEscaped", strong(SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get)),
      Kcnfc3Row("Checkmate", strong(SceneCheckmate.write(facts(checkmateFen), checkmateMove).get))
    )

  private def kcnfc3CollisionTargets: Vector[Kcnfc3Row] =
    val materialFacts = facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val forkFacts = facts("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val defenseFacts = facts("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val discoveredFacts = facts("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val pinFacts = facts("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val removeGuardFacts = facts("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1")
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val skewerFacts = facts("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val stopFacts = facts("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1")
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val breakFacts = facts("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1")
    val breakMove = Line(Square('e', 4), Square('e', 5))
    val blockFacts = facts("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1")
    val blockMove = Line(Square('g', 5), Square('e', 6))
    val openedFacts = facts("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1")
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val advanceFacts = facts("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1")
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val threatFacts = facts("k7/8/4P3/8/8/8/8/4K3 w - - 0 1")
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionFacts = facts("k7/4P3/8/8/8/8/8/4K3 w - - 0 1")
    val promotionMove = Line(Square('e', 7), Square('e', 8))

    Vector(
      Kcnfc3Row("Material", strongMaterial(SceneMaterial.write(materialFacts, materialMove).get)),
      Kcnfc3Row("Hanging", strongMaterial(TacticHanging.write(materialFacts, materialMove).get)),
      Kcnfc3Row(
        "Fork",
        strong(TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get)
      ),
      Kcnfc3Row("Defense", strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get)),
      Kcnfc3Row(
        "DiscoveredAttack",
        strong(
          TacticDiscoveredAttack
            .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
            .get
        )
      ),
      Kcnfc3Row(
        "Pin",
        strong(
          TacticPin
            .write(pinFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
            .get
        )
      ),
      Kcnfc3Row(
        "RemoveGuard",
        strong(
          TacticRemoveGuard
            .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
            .get
        )
      ),
      Kcnfc3Row(
        "Skewer",
        strong(
          TacticSkewer
            .write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
            .get
        )
      ),
      Kcnfc3Row("PawnStop", strong(ScenePawnStop.write(stopFacts, stopMove).get)),
      Kcnfc3Row("PawnBreak", strong(ScenePawnBreak.write(breakFacts, breakMove).get)),
      Kcnfc3Row("PawnBlock", strong(ScenePawnBlock.write(blockFacts, blockMove).get)),
      Kcnfc3Row("PawnCapture", strong(ScenePawnCapture.write(openedFacts, openedMove).get)),
      Kcnfc3Row("PassedPawnCreated", strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get)),
      Kcnfc3Row("FileOpened", strong(SceneFileOpened.write(openedFacts, openedMove).get)),
      Kcnfc3Row("PawnAdvance", strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get)),
      Kcnfc3Row("PromotionThreat", strong(ScenePromotionThreat.write(threatFacts, threatMove).get)),
      Kcnfc3Row("Promotion", strong(ScenePromotion.write(promotionFacts, promotionMove).get))
    )

  private def kingCheckEngine(
      facts: BoardFacts,
      row: Story,
      move: Line,
      status: EngineCheckStatus,
      before: Int = 0,
      after: Int = 0
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(row),
      engineLine = Some(EngineLine(Vector(move))),
      replyLine = Some(EngineLine(Vector(move))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  test("KCNFC-0 keeps the three King Check chains separated and exhaustive"):
    val checkGiven = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val escaped = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get
    val mate = SceneCheckmate.write(facts(checkmateFen), checkmateMove).get

    assertEquals(checkGiven.scene, Scene.CheckGiven)
    assertEquals(checkGiven.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(checkGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGiven.checkEscapedProof, None)
    assertEquals(checkGiven.checkmateProof, None)
    assertEquals(claimKey(checkGiven), Some("gives_check"))

    assertEquals(escaped.scene, Scene.CheckEscaped)
    assertEquals(escaped.writer, Some(StoryWriter.SceneCheckEscaped))
    assertEquals(escaped.checkEscapedProof.exists(_.complete), true)
    assertEquals(escaped.checkGivenProof, None)
    assertEquals(escaped.checkmateProof, None)
    assertEquals(claimKey(escaped), Some("escapes_check"))

    assertEquals(mate.scene, Scene.Checkmate)
    assertEquals(mate.writer, Some(StoryWriter.SceneCheckmate))
    assertEquals(mate.checkmateProof.exists(_.complete), true)
    assertEquals(mate.checkGivenProof, None)
    assertEquals(mate.checkEscapedProof, None)
    assertEquals(claimKey(mate), Some("checkmates"))

    assertEquals(
      Vector(claimKey(checkGiven), claimKey(escaped), claimKey(mate)).flatten.distinct.sorted,
      Vector("checkmates", "escapes_check", "gives_check").sorted
    )

  test("KCNFC-0 allows checkmate and gives-check proof on one move without merging chains"):
    val mateFacts = facts(checkmateFen)
    val checkGivenProof = CheckGivenProof.fromBoardFacts(mateFacts, checkmateMove)
    val checkmateProof = CheckmateProof.fromBoardFacts(mateFacts, checkmateMove)
    val checkGiven = SceneCheckGiven.write(mateFacts, checkmateMove).get
    val mate = SceneCheckmate.write(mateFacts, checkmateMove).get

    assertEquals(checkGivenProof.complete, true)
    assertEquals(checkmateProof.complete, true)
    assertEquals(checkGiven.route, mate.route)
    assertEquals(checkGiven.scene, Scene.CheckGiven)
    assertEquals(mate.scene, Scene.Checkmate)
    assertEquals(checkGiven.writer, Some(StoryWriter.SceneCheckGiven))
    assertEquals(mate.writer, Some(StoryWriter.SceneCheckmate))
    assertEquals(checkGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGiven.checkmateProof, None)
    assertEquals(mate.checkmateProof.exists(_.complete), true)
    assertEquals(mate.checkGivenProof, None)
    assertEquals(claimKey(checkGiven), Some("gives_check"))
    assertEquals(claimKey(mate), Some("checkmates"))

  test("KCNFC-0 blocks contaminated rows and non-lead King Check text"):
    val checkGiven = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val escaped = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get
    val mate = SceneCheckmate.write(facts(checkmateFen), checkmateMove).get

    Vector(
      "given with escaped proof" -> checkGiven.copy(checkEscapedProof = escaped.checkEscapedProof),
      "given with mate proof" -> checkGiven.copy(checkmateProof = mate.checkmateProof),
      "escaped with given proof" -> escaped.copy(checkGivenProof = checkGiven.checkGivenProof),
      "escaped with mate proof" -> escaped.copy(checkmateProof = mate.checkmateProof),
      "mate with given proof" -> mate.copy(checkGivenProof = checkGiven.checkGivenProof),
      "mate with escaped proof" -> mate.copy(checkEscapedProof = escaped.checkEscapedProof),
      "given writerless" -> checkGiven.copy(writer = None),
      "escaped writerless" -> escaped.copy(writer = None),
      "mate writerless" -> mate.copy(writer = None)
    ).foreach: (label, row) =>
      assertEquals(selected(row).role, Role.Blocked, label)
      assertNoStandaloneText(label, row)

    Vector(checkGiven, escaped, mate).foreach: row =>
      val verdict = selected(row)
      Vector(Role.Support, Role.Context, Role.Blocked).foreach: role =>
        assertEquals(ExplanationPlan.fromSelected(verdict.copy(role = role, leadAllowed = false)), None)

  test("KCNFC-0 leaves adjacent forbidden meaning and public surfaces closed"):
    val claimKeys = ExplanationClaim.values.map(_.key).toSet
    val writerNames = StoryWriter.values.map(_.toString).toSet
    val sceneNames = Scene.values.map(_.toString).toSet

    Vector(
      "mate_threat",
      "mate_in_one",
      "mate_in_n",
      "forced_mate",
      "king_safety",
      "attacks_king",
      "creates_attack",
      "creates_pressure",
      "takes_initiative",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay"
    ).foreach: key =>
      assert(!claimKeys.contains(key), s"KCNFC-0 opened forbidden claim key: $key")

    Vector(
      "MateThreat",
      "MateInN",
      "ForcedMate",
      "KingSafety",
      "BestMove",
      "OnlyMove",
      "Winning",
      "Decisive",
      "NoCounterplay"
    ).foreach: name =>
      assert(!sceneNames.contains(name), s"KCNFC-0 opened forbidden Story label: $name")
      assert(!writerNames.contains(s"Scene$name"), s"KCNFC-0 opened forbidden Story writer: Scene$name")

    Vector("Attack", "Pressure", "Initiative").foreach: name =>
      assert(!writerNames.contains(s"Scene$name"), s"KCNFC-0 opened forbidden Story writer: Scene$name")

    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val llmMethods = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    Vector("fromStory", "fromBoardFacts", "fromEngineCheck", "fromPv", "productionApi", "callApi").foreach:
      method =>
        assert(!rendererMethods.contains(method), s"renderer opened forbidden method $method")
        assert(!llmMethods.contains(method), s"LLM smoke opened forbidden method $method")

  test("KCNFC-1 keeps each King Check chain to one proof Story writer and speech key"):
    val mateFacts = facts(checkmateFen)
    val checkGiven = SceneCheckGiven.write(mateFacts, checkmateMove).get
    val mate = SceneCheckmate.write(mateFacts, checkmateMove).get
    val escaped = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get

    assertEquals(checkGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGiven.checkmateProof, None)
    assertEquals(claimKey(checkGiven), Some("gives_check"))
    assertEquals(rendered(checkGiven).map(_.text), Some("Qb7# gives check."))
    assertEquals(rendered(checkGiven).exists(_.claimKey == "checkmates"), false)

    assertEquals(mate.checkmateProof.exists(_.complete), true)
    assertEquals(mate.checkGivenProof, None)
    assertEquals(claimKey(mate), Some("checkmates"))
    assertEquals(rendered(mate).map(_.text), Some("Qb7# is checkmate."))
    assertEquals(rendered(mate).exists(_.claimKey == "gives_check"), false)

    assertEquals(escaped.checkEscapedProof.exists(_.complete), true)
    assertEquals(escaped.checkmateProof, None)
    assertEquals(claimKey(escaped), Some("escapes_check"))
    assertEquals(ExplanationClaim.CheckEscapedAllowed.map(_.key).contains("avoids_mate"), false)
    assertEquals(rendered(escaped).exists(_.text.toLowerCase.contains("mate")), false)

  test("KCNFC-1 rejects wrong proof sidecars and wrong writers"):
    val checkGiven = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val escaped = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get
    val mate = SceneCheckmate.write(facts(checkmateFen), checkmateMove).get

    Vector(
      "CheckGiven row with CheckmateProof" -> checkGiven.copy(checkmateProof = mate.checkmateProof),
      "Checkmate row with CheckGivenProof only" -> mate.copy(checkmateProof = None, checkGivenProof = checkGiven.checkGivenProof),
      "CheckEscaped row with CheckmateProof" -> escaped.copy(checkmateProof = mate.checkmateProof),
      "CheckGiven writer with Checkmate scene" -> checkGiven.copy(scene = Scene.Checkmate),
      "CheckEscaped writer with Checkmate scene" -> escaped.copy(scene = Scene.Checkmate),
      "Checkmate writer with CheckGiven scene" -> mate.copy(scene = Scene.CheckGiven),
      "Checkmate writer with CheckEscaped scene" -> mate.copy(scene = Scene.CheckEscaped),
      "CheckGiven row with Checkmate writer" -> checkGiven.copy(writer = Some(StoryWriter.SceneCheckmate)),
      "Checkmate row with CheckGiven writer" -> mate.copy(writer = Some(StoryWriter.SceneCheckGiven)),
      "CheckEscaped row with Checkmate writer" -> escaped.copy(writer = Some(StoryWriter.SceneCheckmate))
    ).foreach: (label, row) =>
      assertNoLowering(label, row)

  test("KCNFC-1 rejects wrong-scene speech-key lowering"):
    val checkGiven = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val escaped = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get
    val mate = SceneCheckmate.write(facts(checkmateFen), checkmateMove).get

    val givenVerdict = selected(checkGiven)
    val escapedVerdict = selected(escaped)
    val mateVerdict = selected(mate)

    Vector(
      "CheckGiven row cannot lower as checkmates" -> givenVerdict.copy(story = checkGiven.copy(scene = Scene.Checkmate)),
      "CheckGiven row cannot lower as escapes_check" -> givenVerdict.copy(story = checkGiven.copy(scene = Scene.CheckEscaped)),
      "CheckEscaped row cannot lower as gives_check" -> escapedVerdict.copy(story = escaped.copy(scene = Scene.CheckGiven)),
      "CheckEscaped row cannot lower as checkmates" -> escapedVerdict.copy(story = escaped.copy(scene = Scene.Checkmate)),
      "Checkmate row cannot lower as gives_check" -> mateVerdict.copy(story = mate.copy(scene = Scene.CheckGiven)),
      "Checkmate row cannot lower as escapes_check" -> mateVerdict.copy(story = mate.copy(scene = Scene.CheckEscaped))
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("KCNFC-1 rejects SAN BoardFacts and engine-only Checkmate creation"):
    val mateFacts = facts(checkmateFen)
    val sanMate = mateFacts.sideLegal.sanFor(checkmateMove)
    val checkmateProof = CheckmateProof.fromBoardFacts(mateFacts, checkmateMove)
    val engineOnly =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(checkmateMove),
        engineLine = Some(EngineLine(Vector(checkmateMove))),
        replyLine = Some(EngineLine(Vector(checkmateMove))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(10000)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(sanMate.exists(_.contains("#")), true)
    assertEquals(checkmateProof.complete, true)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.publicClaimAllowed, false)

    val sanOnlyCheckmate =
      SceneCheckmate.write(mateFacts, checkmateMove).get.copy(checkmateProof = None)
    assertNoLowering("Checkmate row created from SAN #", sanOnlyCheckmate)

  test("KCNFC-2 collision fixtures keep King Check rows in separate proof homes"):
    val checkGiven = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val escaped = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get
    val mate = SceneCheckmate.write(facts(checkmateFen), checkmateMove).get
    val crossFacts = facts(crossCheckFen)
    val crossGiven = SceneCheckGiven.write(crossFacts, crossCheckMove).get
    val crossEscaped = SceneCheckEscaped.write(crossFacts, crossCheckMove).get
    val mateFacts = facts(checkmateFen)
    val mateGiven = SceneCheckGiven.write(mateFacts, checkmateMove).get
    val mateCheckmate = SceneCheckmate.write(mateFacts, checkmateMove).get

    assertEquals(checkGiven.scene, Scene.CheckGiven, "CheckGiven only fixture")
    assertEquals(checkGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGiven.checkEscapedProof, None)
    assertEquals(checkGiven.checkmateProof, None)
    assertEquals(claimKey(checkGiven), Some("gives_check"))
    assertEquals(rendered(checkGiven).map(_.text), Some("Re2+ gives check."))

    assertEquals(escaped.scene, Scene.CheckEscaped, "CheckEscaped only fixture")
    assertEquals(escaped.checkEscapedProof.exists(_.complete), true)
    assertEquals(escaped.checkGivenProof, None)
    assertEquals(escaped.checkmateProof, None)
    assertEquals(claimKey(escaped), Some("escapes_check"))
    assertEquals(rendered(escaped).map(_.text), Some("Kf1 gets out of check."))

    assertEquals(mate.scene, Scene.Checkmate, "Checkmate only fixture")
    assertEquals(mate.checkmateProof.exists(_.complete), true)
    assertEquals(mate.checkGivenProof, None)
    assertEquals(mate.checkEscapedProof, None)
    assertEquals(claimKey(mate), Some("checkmates"))
    assertEquals(rendered(mate).map(_.text), Some("Qb7# is checkmate."))

    assertEquals(CheckGivenProof.fromBoardFacts(crossFacts, crossCheckMove).complete, true)
    assertEquals(CheckEscapedProof.fromBoardFacts(crossFacts, crossCheckMove).complete, true)
    assertEquals(CheckmateProof.fromBoardFacts(crossFacts, crossCheckMove).complete, false)
    assertEquals(crossGiven.route, crossEscaped.route)
    assertEquals(crossGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(crossGiven.checkEscapedProof, None)
    assertEquals(crossGiven.checkmateProof, None)
    assertEquals(crossEscaped.checkEscapedProof.exists(_.complete), true)
    assertEquals(crossEscaped.checkGivenProof, None)
    assertEquals(crossEscaped.checkmateProof, None)
    assertEquals(claimKey(crossGiven), Some("gives_check"))
    assertEquals(claimKey(crossEscaped), Some("escapes_check"))
    assertEquals(rendered(crossGiven).map(_.text), Some("Qe2+ gives check."))
    assertEquals(rendered(crossEscaped).map(_.text), Some("Qe2+ gets out of check."))

    assertEquals(CheckGivenProof.fromBoardFacts(mateFacts, checkmateMove).complete, true)
    assertEquals(CheckmateProof.fromBoardFacts(mateFacts, checkmateMove).complete, true)
    assertEquals(mateGiven.route, mateCheckmate.route)
    assertEquals(mateGiven.checkGivenProof.exists(_.complete), true)
    assertEquals(mateGiven.checkmateProof, None)
    assertEquals(mateCheckmate.checkmateProof.exists(_.complete), true)
    assertEquals(mateCheckmate.checkGivenProof, None)
    assertEquals(claimKey(mateGiven), Some("gives_check"))
    assertEquals(claimKey(mateCheckmate), Some("checkmates"))

    val crossVerdicts = StoryTable.choose(Vector(crossGiven, crossEscaped))
    assertEquals(crossVerdicts.map(_.story.scene).toSet, Set(Scene.CheckGiven, Scene.CheckEscaped))
    assertEquals(crossVerdicts.exists(_.story.checkmateProof.nonEmpty), false)

    val mateVerdicts = StoryTable.choose(Vector(mateGiven, mateCheckmate))
    assertEquals(mateVerdicts.map(_.story.scene).toSet, Set(Scene.CheckGiven, Scene.Checkmate))
    assertEquals(mateVerdicts.exists(row => row.story.scene == Scene.CheckGiven && row.story.checkmateProof.nonEmpty), false)
    assertEquals(mateVerdicts.exists(row => row.story.scene == Scene.Checkmate && row.story.checkGivenProof.nonEmpty), false)

  test("KCNFC-2 no-legal SAN engine and contamination fixtures stay silent"):
    val mateFacts = facts(checkmateFen)
    val mate = SceneCheckmate.write(mateFacts, checkmateMove).get
    val matePlan = plan(mate).get
    val mateRendered = rendered(mate).get
    val stalemate = facts(stalemateFen)
    val noLegalNoCheck = facts(noLegalNoCheckFen)
    val missingMove = Line(Square('h', 8), Square('h', 7))
    val sanOnlyFacts =
      BoardFacts.untrusted(
        root = mateFacts.root,
        sideToMove = mateFacts.sideToMove,
        header = mateFacts.header,
        sideLegal = mateFacts.sideLegal,
        rivalLegal = mateFacts.rivalLegal,
        control = mateFacts.control,
        material = mateFacts.material,
        pawns = mateFacts.pawns,
        pieces = mateFacts.pieces
      )
    val engineMateLine =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(checkmateMove),
        engineLine = Some(EngineLine(Vector(checkmateMove, Line(Square('a', 8), Square('a', 7))))),
        replyLine = Some(EngineLine(Vector(checkmateMove))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(10000)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    Vector("stalemate" -> stalemate, "no legal move without check" -> noLegalNoCheck).foreach: (label, board) =>
      assertEquals(board.sideLegal.moveCount, 0, label)
      assertEquals(BoardFacts.sideInCheck(board, board.sideToMove), false, label)
      assertEquals(CheckmateProof.fromBoardFacts(board, missingMove).complete, false, label)
      assertEquals(SceneCheckmate.write(board, missingMove), None, label)

    assertEquals(mateFacts.sideLegal.sanFor(checkmateMove).exists(_.contains("#")), true)
    assertEquals(sanOnlyFacts.sideLegal.sanFor(checkmateMove).exists(_.contains("#")), true)
    assertEquals(CheckmateProof.fromBoardFacts(sanOnlyFacts, checkmateMove).complete, false)
    assertEquals(SceneCheckmate.write(sanOnlyFacts, checkmateMove), None)
    assertNoLowering("SAN # without proof forged Checkmate row", mate.copy(checkmateProof = None))

    assertEquals(engineMateLine.status, EngineCheckStatus.Supports)
    assertEquals(engineMateLine.storyBound, false)
    assertEquals(engineMateLine.publicClaimAllowed, false)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
    assertEquals(LlmNarrationSmoke.check(matePlan, mateRendered, "The engine says Qb7# is mate.").accepted, false)

    val checkGiven = SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get
    val escaped = SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get

    Vector(
      "CheckGiven contaminated with CheckEscapedProof" -> checkGiven.copy(checkEscapedProof = escaped.checkEscapedProof),
      "CheckGiven contaminated with CheckmateProof" -> checkGiven.copy(checkmateProof = mate.checkmateProof),
      "CheckEscaped contaminated with CheckGivenProof" -> escaped.copy(checkGivenProof = checkGiven.checkGivenProof),
      "CheckEscaped contaminated with CheckmateProof" -> escaped.copy(checkmateProof = mate.checkmateProof),
      "Checkmate contaminated with CheckGivenProof" -> mate.copy(checkGivenProof = checkGiven.checkGivenProof),
      "Checkmate contaminated with CheckEscapedProof" -> mate.copy(checkEscapedProof = escaped.checkEscapedProof)
    ).foreach: (label, row) =>
      assertNoLowering(label, row)

    assertEquals(LlmNarrationSmoke.mockNarrate(plan(checkGiven).get, mateRendered), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(matePlan, rendered(checkGiven).get), None)

  test("KCNFC-3 StoryTable orders existing King Check rows without creating or merging meanings"):
    val rows = kingCheckRows.map(_.story)
    val verdicts = assertStableStoryTable("KCNFC-3 King Check rows", rows)

    assertEquals(verdicts.map(_.story.scene).toSet, Set(Scene.CheckGiven, Scene.CheckEscaped, Scene.Checkmate))
    assertEquals(verdicts.forall(verdict => rows.contains(verdict.story)), true)
    assertEquals(verdicts.count(_.role == Role.Lead), 1)

    val checkGivenRow = verdicts.find(_.story.scene == Scene.CheckGiven).get.story
    val escaped = verdicts.find(_.story.scene == Scene.CheckEscaped).get.story
    val mate = verdicts.find(_.story.scene == Scene.Checkmate).get.story

    assertEquals(checkGivenRow.checkGivenProof.exists(_.complete), true)
    assertEquals(checkGivenRow.checkmateProof, None)
    assertEquals(escaped.checkEscapedProof.exists(_.complete), true)
    assertEquals(escaped.checkmateProof, None)
    assertEquals(mate.checkmateProof.exists(_.complete), true)
    assertEquals(mate.checkGivenProof, None)

    val targetOnlyVerdicts = assertStableStoryTable("KCNFC-3 non-King collision targets", kcnfc3CollisionTargets.map(_.story))
    targetOnlyVerdicts.foreach: verdict =>
      assert(
        !Set(Scene.CheckGiven, Scene.CheckEscaped, Scene.Checkmate).contains(verdict.story.scene),
        s"StoryTable created a King Check Story from ${verdict.story.scene}"
      )

  test("KCNFC-3 duplicate and same-board King Check collisions stay deterministic and owned"):
    val mateFacts = facts(checkmateFen)
    val mateGiven = strong(SceneCheckGiven.write(mateFacts, checkmateMove).get)
    val mateCheckmate = strong(SceneCheckmate.write(mateFacts, checkmateMove).get)
    val duplicateGivenRows =
      Vector(
        strong(SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get, 97),
        strong(SceneCheckGiven.write(mateFacts, checkmateMove).get, 95),
        strong(SceneCheckGiven.write(facts(crossCheckFen), crossCheckMove).get, 93)
      )
    val duplicateGivenVerdicts = assertStableStoryTable("KCNFC-3 duplicate CheckGiven rows", duplicateGivenRows)

    assertEquals(duplicateGivenVerdicts.size, duplicateGivenRows.size)
    assertEquals(duplicateGivenVerdicts.map(_.story.scene).toSet, Set(Scene.CheckGiven))
    duplicateGivenVerdicts.foreach: verdict =>
      assertEquals(verdict.story.checkGivenProof.exists(_.complete), true)
      assertEquals(verdict.story.checkmateProof, None)
      if verdict.role == Role.Lead then assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("gives_check"))
      else assertNoVerdictText("non-lead duplicate CheckGiven", verdict)

    val sameBoardVerdicts = assertStableStoryTable("KCNFC-3 same-board CheckGiven Checkmate", Vector(mateGiven, mateCheckmate))
    assertEquals(sameBoardVerdicts.map(_.story.scene).toSet, Set(Scene.CheckGiven, Scene.Checkmate))
    sameBoardVerdicts.foreach: verdict =>
      verdict.story.scene match
        case Scene.CheckGiven =>
          assertEquals(verdict.story.checkGivenProof.exists(_.complete), true)
          assertEquals(verdict.story.checkmateProof, None)
          assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).exists(_.key == "checkmates"), false)
        case Scene.Checkmate =>
          assertEquals(verdict.story.checkmateProof.exists(_.complete), true)
          assertEquals(verdict.story.checkGivenProof, None)
          assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).exists(_.key == "gives_check"), false)
        case other =>
          fail(s"unexpected same-board King Check scene: $other")

  test("KCNFC-3 sibling collisions keep Story family ownership outside King Check"):
    val kingRows = kingCheckRows
    val kingScenes = Set(Scene.CheckGiven, Scene.CheckEscaped, Scene.Checkmate)

    kingRows.foreach: king =>
      kcnfc3CollisionTargets.foreach: target =>
        val rows = Vector(king.story, target.story)
        val verdicts = assertStableStoryTable(s"KCNFC-3 ${king.label} vs ${target.label}", rows)
        val kingVerdict = verdicts.find(_.story == king.story).get
        val targetVerdict = verdicts.find(_.story == target.story).get

        assertEquals(kingVerdict.story.scene, king.story.scene, s"${king.label} scene")
        assertEquals(kingScenes.contains(targetVerdict.story.scene), false, s"${target.label} must stay outside King Check")
        assertEquals(targetVerdict.story.checkGivenProof, None, target.label)
        assertEquals(targetVerdict.story.checkEscapedProof, None, target.label)
        assertEquals(targetVerdict.story.checkmateProof, None, target.label)

        king.story.scene match
          case Scene.CheckGiven =>
            assertEquals(kingVerdict.story.checkGivenProof.exists(_.complete), true, target.label)
            assertEquals(kingVerdict.story.checkmateProof, None, target.label)
          case Scene.CheckEscaped =>
            assertEquals(kingVerdict.story.checkEscapedProof.exists(_.complete), true, target.label)
            assertEquals(kingVerdict.story.checkmateProof, None, target.label)
            assertEquals(ExplanationPlan.fromSelected(kingVerdict).flatMap(_.allowedClaim).exists(_.key == "avoids_mate"), false)
          case Scene.Checkmate =>
            assertEquals(kingVerdict.story.checkmateProof.exists(_.complete), true, target.label)
            assertEquals(kingVerdict.story.checkGivenProof, None, target.label)
          case other =>
            fail(s"unexpected King Check scene: $other")

  test("KCNFC-3 non-Lead capped and refuted King Check rows stay silent"):
    val materialLead = strongMaterial(
      SceneMaterial.write(facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"), Line(Square('d', 4), Square('e', 5))).get,
      100
    )
    val checkGivenRow = strong(SceneCheckGiven.write(facts(checkGivenFen), checkGivenMove).get, 90)
    val escaped = strong(SceneCheckEscaped.write(facts(checkEscapedFen), checkEscapedMove).get, 90)
    val mate = strong(SceneCheckmate.write(facts(checkmateFen), checkmateMove).get, 90)

    Vector("CheckGiven" -> checkGivenRow, "CheckEscaped" -> escaped, "Checkmate" -> mate).foreach: (label, row) =>
      val supportVerdict = StoryTable.choose(Vector(materialLead, row)).find(_.story == row).get
      assertEquals(supportVerdict.role, Role.Support, label)
      assertNoVerdictText(s"$label Support", supportVerdict)

      val contextVerdict = StoryTable.choose(Vector(row.copy(proof = eventProof(20)))).head
      assertEquals(contextVerdict.role, Role.Context, label)
      assertNoVerdictText(s"$label Context", contextVerdict)

      val blockedVerdict = StoryTable.choose(Vector(row.copy(writer = None))).head
      assertEquals(blockedVerdict.role, Role.Blocked, label)
      assertNoVerdictText(s"$label Blocked", blockedVerdict)

    val cappedGiven = checkGivenRow.copy(
      engineCheck = Some(kingCheckEngine(facts(checkGivenFen), checkGivenRow, checkGivenMove, EngineCheckStatus.Caps))
    )
    val cappedEscaped = escaped.copy(
      engineCheck = Some(kingCheckEngine(facts(checkEscapedFen), escaped, checkEscapedMove, EngineCheckStatus.Caps))
    )
    val cappedMate = mate.copy(
      engineCheck = Some(kingCheckEngine(facts(checkmateFen), mate, checkmateMove, EngineCheckStatus.Caps))
    )
    val refutedGiven = checkGivenRow.copy(
      engineCheck = Some(kingCheckEngine(facts(checkGivenFen), checkGivenRow, checkGivenMove, EngineCheckStatus.Supports, before = 220, after = 20))
    )
    val refutedEscaped = escaped.copy(
      engineCheck = Some(kingCheckEngine(facts(checkEscapedFen), escaped, checkEscapedMove, EngineCheckStatus.Supports, before = 220, after = 20))
    )
    val refutedMate = mate.copy(
      engineCheck = Some(kingCheckEngine(facts(checkmateFen), mate, checkmateMove, EngineCheckStatus.Supports, before = 220, after = 20))
    )

    Vector(cappedGiven, cappedEscaped, cappedMate).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.engineStrengthLimited, true, row.scene.toString)
      assertNoVerdictText(s"${row.scene} capped", verdict)

    Vector(refutedGiven, refutedEscaped, refutedMate).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, row.scene.toString)
      assertNoVerdictText(s"${row.scene} refuted", verdict)

  test("KCNFC-4 engine notation BoardFacts diagnostics and source rows cannot create King Check Stories"):
    val checkFacts = facts(checkGivenFen)
    val escapedFacts = facts(checkEscapedFen)
    val mateFacts = facts(checkmateFen)
    val checkSan = checkFacts.sideLegal.sanFor(checkGivenMove).get
    val mateSan = mateFacts.sideLegal.sanFor(checkmateMove).get
    val engineOnly =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(checkmateMove),
        engineLine = Some(EngineLine(Vector(checkmateMove, Line(Square('a', 8), Square('a', 7))))),
        replyLine = Some(EngineLine(Vector(checkmateMove))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(10000)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(checkSan.contains("+"), true, "SAN + must stay notation")
    assertEquals(mateSan.contains("#"), true, "SAN # must stay notation")
    assertEquals(BoardFacts.sideInCheckAfterLegalMove(checkFacts, checkGivenMove, Side.Black), Some(true))
    assertEquals(BoardFacts.sideInCheckAfterLegalMove(mateFacts, checkmateMove, Side.Black), Some(true))
    assertEquals(BoardFacts.sideLegalMoveCountAfterLegalMove(mateFacts, checkmateMove, Side.Black), Some(0))
    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.publicClaimAllowed, false)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)

    val forgedRows =
      Vector(
        "SAN + and engine-only CheckGiven" ->
          Story(
            scene = Scene.CheckGiven,
            proof = eventProof(100),
            side = Side.White,
            rival = Side.Black,
            target = Some(Square('e', 8)),
            anchor = Some(Square('d', 2)),
            route = Some(checkGivenMove),
            routeSan = Some(checkSan),
            engineCheck = Some(engineOnly)
          ),
        "BoardFacts-only CheckEscaped" ->
          Story(
            scene = Scene.CheckEscaped,
            proof = eventProof(100),
            side = Side.White,
            rival = Side.Black,
            target = Some(Square('f', 1)),
            anchor = Some(Square('e', 1)),
            route = Some(checkEscapedMove),
            routeSan = escapedFacts.sideLegal.sanFor(checkEscapedMove),
            engineCheck = Some(engineOnly)
          ),
        "SAN # and engine-only Checkmate" ->
          Story(
            scene = Scene.Checkmate,
            proof = eventProof(100),
            side = Side.White,
            rival = Side.Black,
            target = Some(Square('a', 8)),
            anchor = Some(Square('c', 7)),
            route = Some(checkmateMove),
            routeSan = Some(mateSan),
            engineCheck = Some(engineOnly)
          )
      )

    forgedRows.foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.story.scene, row.scene, label)
      assertEquals(verdict.role, Role.Blocked, label)
      assert(verdict.proofFailures.nonEmpty, s"$label must keep proof failures internal")
      assertNoVerdictText(label, verdict)

    val sourceRow =
      Story(
        scene = Scene.Source,
        proof = eventProof(100),
        side = Side.White,
        rival = Side.Black,
        target = Some(Square('a', 8)),
        anchor = Some(Square('c', 7)),
        route = Some(checkmateMove),
        routeSan = Some(mateSan),
        engineCheck = Some(engineOnly)
      )
    val sourceVerdict = StoryTable.choose(Vector(sourceRow)).head
    assertEquals(sourceVerdict.story.scene, Scene.Source)
    assertNoVerdictText("source rows cannot own King Check claims", sourceVerdict)

  test("KCNFC-4 EngineCheck supports caps or refutes only existing proof-backed King Check rows"):
    kingCheckRows.foreach: row =>
      val move = row.story.route.get
      val board =
        row.story.scene match
          case Scene.CheckGiven => facts(checkGivenFen)
          case Scene.CheckEscaped => facts(checkEscapedFen)
          case Scene.Checkmate => facts(checkmateFen)
          case other => fail(s"unexpected King Check row: $other")
      val supports = row.story.copy(
        engineCheck = Some(kingCheckEngine(board, row.story, move, EngineCheckStatus.Supports))
      )
      val caps = row.story.copy(
        engineCheck = Some(kingCheckEngine(board, row.story, move, EngineCheckStatus.Caps))
      )
      val refutes = row.story.copy(
        engineCheck = Some(kingCheckEngine(board, row.story, move, EngineCheckStatus.Supports, before = 220, after = 20))
      )
      val noStory =
        EngineCheck.fromStory(
          facts = board,
          story = None,
          engineLine = Some(EngineLine(Vector(move))),
          replyLine = Some(EngineLine(Vector(move))),
          evalBefore = Some(EngineEval(0)),
          evalAfter = Some(EngineEval(0)),
          depth = Some(18),
          freshnessPly = Some(0),
          requestedStatus = EngineCheckStatus.Supports
        )

      assertEquals(noStory.storyBound, false, row.label)
      assertEquals(noStory.status, EngineCheckStatus.Unknown, row.label)
      assertEquals(noStory.publicClaimAllowed, false, row.label)

      val supportVerdict = StoryTable.choose(Vector(supports)).head
      assertEquals(supportVerdict.role, Role.Lead, row.label)
      assertEquals(supportVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports), row.label)
      assertEquals(ExplanationPlan.fromSelected(supportVerdict).flatMap(_.allowedClaim).map(_.key), claimKey(row.story), row.label)

      val cappedVerdict = StoryTable.choose(Vector(caps)).head
      assertEquals(cappedVerdict.role, Role.Lead, row.label)
      assertEquals(cappedVerdict.engineStrengthLimited, true, row.label)
      assertNoVerdictText(s"${row.label} capped EngineCheck", cappedVerdict)

      val refutedVerdict = StoryTable.choose(Vector(refutes)).head
      assertEquals(refutedVerdict.role, Role.Blocked, row.label)
      assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), row.label)
      assertNoVerdictText(s"${row.label} refuted EngineCheck", refutedVerdict)

  test("KCNFC-4 raw engine diagnostics and forbidden wording never enter King Check public wording"):
    val forbiddenOutputs =
      Vector(
        "The engine says this is correct.",
        "The mate score is decisive.",
        "This is the best move.",
        "This is the only move.",
        "It is forced mate.",
        "This is winning.",
        "This is decisive.",
        "There is no counterplay.",
        "The king is safe.",
        "This leaves a safe king.",
        "This leaves an unsafe king.",
        "This starts an attack.",
        "This creates pressure.",
        "This takes the initiative.",
        "Raw PV: Qb7# Ka7.",
        "The eval is +3.5."
      )

    kingCheckRows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val renderedLower = rendered.text.toLowerCase(java.util.Locale.ROOT)

      Vector("engine says", "mate score", "best move", "only move", "forced mate", "no counterplay").foreach:
        phrase =>
          assertEquals(renderedLower.contains(phrase), false, s"${row.label} renderer leaked $phrase")

      forbiddenOutputs.foreach: output =>
        val checked = LlmNarrationSmoke.check(plan, rendered, s"${rendered.text} $output")
        assertEquals(checked.accepted, false, s"${row.label} accepted forbidden output: $output")

  test("KCNFC-5 ExplanationPlan lowers each King Check row only to its own claim key"):
    val expected =
      Map(
        Scene.CheckGiven -> ExplanationClaim.GivesCheck,
        Scene.CheckEscaped -> ExplanationClaim.EscapesCheck,
        Scene.Checkmate -> ExplanationClaim.Checkmates
      )

    kingCheckRows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val expectedClaim = expected(row.story.scene)

      assertEquals(plan.scene, row.story.scene, row.label)
      assertEquals(plan.tactic, None, row.label)
      assertEquals(plan.allowedClaim, Some(expectedClaim), row.label)
      assertEquals(rendered.claimKey, expectedClaim.key, row.label)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text), row.label)

      expected.values.filterNot(_ == expectedClaim).foreach: wrongClaim =>
        val wrongPlan = plan.copy(allowedClaim = Some(wrongClaim))
        val wrongRendered = rendered.copy(claimKey = wrongClaim.key)

        assertEquals(DeterministicRenderer.fromPlan(wrongPlan), None, s"${row.label} rendered ${wrongClaim.key}")
        assertEquals(LlmNarrationSmoke.mockNarrate(wrongPlan, rendered), None, s"${row.label} narrated ${wrongClaim.key}")
        assertEquals(LlmNarrationSmoke.mockNarrate(plan, wrongRendered), None, s"${row.label} accepted wrong rendered key")
        assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, wrongRendered), None, s"${row.label} prompted wrong key")

  test("KCNFC-5 renderer stays ExplanationPlan-only and no stronger than King Check claim keys"):
    val rendererSource =
      java.nio.file.Files.readString(
        java.nio.file.Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/DeterministicRenderer.scala")
      )

    assert(rendererSource.contains("def fromPlan(plan: ExplanationPlan): Option[RenderedLine]"))
    Vector("fromStory", "fromVerdict", "fromBoardFacts", "fromEngineCheck", "callApi", "productionApi").foreach:
      forbidden =>
        assert(!rendererSource.contains(forbidden), s"renderer must not expose $forbidden")

    kingCheckRows.foreach: row =>
      val rendered = DeterministicRenderer.fromPlan(ExplanationPlan.fromSelected(StoryTable.choose(Vector(row.story)).head).get).get
      val normalized = rendered.text.toLowerCase(java.util.Locale.ROOT)
      val allowedPhrase =
        row.story.scene match
          case Scene.CheckGiven => "gives check"
          case Scene.CheckEscaped => "gets out of check"
          case Scene.Checkmate => "checkmate"
          case other => fail(s"unexpected King Check scene: $other")

      assert(normalized.contains(allowedPhrase), row.label)
      Vector(
        "mate threat",
        "mate in",
        "forced mate",
        "engine",
        "king safety",
        "safe king",
        "unsafe king",
        "attack",
        "pressure",
        "initiative",
        "best move",
        "only move",
        "winning",
        "decisive",
        "no counterplay"
      ).foreach: phrase =>
        assertEquals(normalized.contains(phrase), false, s"${row.label} renderer leaked $phrase")

  test("KCNFC-5 LLM smoke receives only bounded rendered fields and rejects new chess facts"):
    val forbiddenAdditions =
      Vector(
        "Nf3 is also important.",
        "The line continues Qh5 Rh7.",
        "This creates a mate threat.",
        "This is mate in 2.",
        "This is forced mate.",
        "The engine explains the move as +3.0.",
        "This is about king safety.",
        "This leaves a safe king.",
        "This leaves an unsafe king.",
        "This starts an attack.",
        "This creates pressure.",
        "This takes the initiative.",
        "This is the best move.",
        "This is the only move.",
        "This is winning.",
        "This is decisive.",
        "There is no counterplay."
      )

    kingCheckRows.foreach: row =>
      val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(row.story)).head).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

      assert(prompt.contains(s"renderedText: ${rendered.text}"), row.label)
      assert(prompt.contains(s"claimKey: ${rendered.claimKey}"), row.label)
      assert(prompt.contains(s"strength: ${rendered.strength}"), row.label)
      assert(prompt.contains("forbiddenWording:"), row.label)
      assert(prompt.contains("Rephrase only. Do not add chess facts."), row.label)
      Vector(
        "BoardFacts",
        "EngineCheck",
        "EngineEval",
        "EngineLine",
        "CheckGivenProof",
        "CheckEscapedProof",
        "CheckmateProof",
        "proofFailures",
        "source rows",
        "StoryTable"
      ).foreach: forbidden =>
        assertEquals(prompt.contains(forbidden), false, s"${row.label} prompt leaked $forbidden")

      forbiddenAdditions.foreach: addition =>
        val checked = LlmNarrationSmoke.check(plan, rendered, s"${rendered.text} $addition")
        assertEquals(checked.accepted, false, s"${row.label} accepted addition: $addition")

  test("KCNFC-5 non-selected capped refuted Support Context and Blocked King Check rows stay downstream silent"):
    val materialLead = strongMaterial(
      SceneMaterial.write(facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"), Line(Square('d', 4), Square('e', 5))).get,
      100
    )

    kingCheckRows.foreach: row =>
      val leadVerdict = StoryTable.choose(Vector(row.story)).head
      assertNoVerdictText(s"${row.label} non-selected", leadVerdict.copy(selected = false))

      val supportVerdict = StoryTable.choose(Vector(materialLead, row.story)).find(_.story == row.story).get
      assertEquals(supportVerdict.role, Role.Support, row.label)
      assertNoVerdictText(s"${row.label} Support", supportVerdict)

      val contextVerdict = StoryTable.choose(Vector(row.story.copy(proof = eventProof(20)))).head
      assertEquals(contextVerdict.role, Role.Context, row.label)
      assertNoVerdictText(s"${row.label} Context", contextVerdict)

      val blockedVerdict = StoryTable.choose(Vector(row.story.copy(writer = None))).head
      assertEquals(blockedVerdict.role, Role.Blocked, row.label)
      assertNoVerdictText(s"${row.label} Blocked", blockedVerdict)

      val board =
        row.story.scene match
          case Scene.CheckGiven => facts(checkGivenFen)
          case Scene.CheckEscaped => facts(checkEscapedFen)
          case Scene.Checkmate => facts(checkmateFen)
          case other => fail(s"unexpected King Check scene: $other")
      val route = row.story.route.get
      val capped = row.story.copy(engineCheck = Some(kingCheckEngine(board, row.story, route, EngineCheckStatus.Caps)))
      val refuted =
        row.story.copy(
          engineCheck = Some(kingCheckEngine(board, row.story, route, EngineCheckStatus.Supports, before = 220, after = 20))
        )

      assertNoVerdictText(s"${row.label} capped", StoryTable.choose(Vector(capped)).head)
      assertNoVerdictText(s"${row.label} refuted", StoryTable.choose(Vector(refuted)).head)
