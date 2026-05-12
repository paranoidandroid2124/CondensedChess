package lila.commentary.chess

class ProofInteractionLawReconciliationTest extends munit.FunSuite:

  test("PILFR quiet fallback remains non-speaking without opened Quiet writer"):
    val facts = board(materialFen)
    val quiet =
      Story(
        scene = Scene.Quiet,
        proof = strongProof,
        side = Side.White,
        rival = Side.Black,
        target = Some(Square('e', 5)),
        anchor = Some(Square('d', 4)),
        route = Some(materialMove),
        routeSan = BoardFacts.sanFor(facts, materialMove),
        storyProof = StoryProof.fromBoardFacts(facts, materialMove)
      )

    val verdict = StoryTable.choose(Vector(quiet)).head

    assert(verdict.role != Role.Lead)
    assertEquals(verdict.leadAllowed, false)
    assertNoStandaloneText(verdict, "Quiet")
    assert(verdict.proofDeficitDiagnostic.exists(_.missingSidecar.contains("Story writer")))

  test("PILFR strategic Plan with no route stays blocked and has no public plan speech"):
    val plan =
      Story(
        scene = Scene.Plan,
        plan = Some(Plan.OpenFile),
        proof = strongProof,
        side = Side.White,
        rival = Side.Black,
        target = Some(Square('d', 5)),
        anchor = Some(Square('d', 1)),
        route = None,
        routeSan = None,
        storyProof = StoryProof.empty
      )

    val verdict = StoryTable.choose(Vector(plan)).head
    val diagnostic = verdict.proofDeficitDiagnostic.getOrElse(fail("Plan row should produce internal deficit"))

    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)
    assertNoStandaloneText(verdict, "Plan")
    assert(diagnostic.missingSidecar.exists(_.contains("route")))
    assert(diagnostic.missingSidecar.exists(_.contains("StoryProof legal line")))

  test("PILFR source and opening rows stay non-speaking next to board-backed proof"):
    val facts = board(materialFen)
    val material = SceneMaterial.write(facts, materialMove).get
    val sourceRows =
      Vector(Scene.Source, Scene.Opening).map: scene =>
        Story(
          scene = scene,
          proof = strongProof,
          side = Side.White,
          rival = Side.Black,
          target = Some(Square('e', 5)),
          anchor = Some(Square('d', 4)),
          route = Some(materialMove),
          routeSan = BoardFacts.sanFor(facts, materialMove),
          storyProof = StoryProof.fromBoardFacts(facts, materialMove)
        )

    sourceRows.foreach: row =>
      val verdicts = StoryTable.choose(Vector(row, material))
      val contextVerdict = verdicts.find(_.story == row).get
      val materialVerdict = verdicts.find(_.story == material).get

      assertEquals(materialVerdict.role, Role.Lead, row.scene.toString)
      assertEquals(contextVerdict.leadAllowed, false, row.scene.toString)
      assertNoStandaloneText(contextVerdict, row.scene.toString)
      assert(contextVerdict.proofDeficitDiagnostic.exists(_.missingSidecar.contains("Story writer")))

  test("PILFR high counterplay risk blocks standalone material or conversion speech"):
    val material =
      SceneMaterial
        .write(board(materialFen), materialMove)
        .get
        .copy(proof = strongProof.copy(counterplayRisk = 71))
    val verdict = StoryTable.choose(Vector(material)).head
    val diagnostic = verdict.proofDeficitDiagnostic.getOrElse(fail("Risk-capped Material should diagnose non-lead row"))

    assertEquals(verdict.role, Role.Context)
    assertEquals(verdict.leadAllowed, false)
    assertNoStandaloneText(verdict, "Material counterplay")
    assert(diagnostic.blockedBy.contains("lead_not_allowed"))

  test("PILFR legal-escape king pressure stays below mate-net wording"):
    val facts = board(checkButEscapeFen)
    val checkMove = Line(Square('d', 2), Square('e', 2))

    assertEquals(SceneCheckmate.write(facts, checkMove), None)

    val check = SceneCheckGiven.write(facts, checkMove).get
    val verdict = StoryTable.choose(Vector(check)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("gives_check"))
    assertEquals(rendered.claimKey, "gives_check")

    Vector(
      "This creates a mate net.",
      "The king has no legal escape.",
      "This is forced mate.",
      "There is no counterplay."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  private val materialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"
  private val materialMove = Line(Square('d', 4), Square('e', 5))
  private val checkButEscapeFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"

  private def assertNoStandaloneText(verdict: Verdict, label: String): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid PILFR FEN: $fen -> $error"), identity)

  private def strongProof: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 90,
      immediacy = 90,
      forcing = 90,
      conversionPrize = 90,
      counterplayRisk = 20,
      kingHeat = 90,
      pieceSupport = 90,
      pawnSupport = 90,
      sourceFit = 90,
      novelty = 90,
      clarity = 90
    )
