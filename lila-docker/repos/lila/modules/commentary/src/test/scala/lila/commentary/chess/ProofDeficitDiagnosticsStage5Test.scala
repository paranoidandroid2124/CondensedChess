package lila.commentary.chess

class ProofDeficitDiagnosticsStage5Test extends munit.FunSuite:

  test("Stage-5 cross-family fixtures all use the same generic diagnostic shape"):
    val fixtures =
      Vector(
        DiagnosticFixture(
          "tactic missing sidecar",
          TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(loosePieceProof = None),
          "LoosePieceProof"
        ),
        DiagnosticFixture(
          "file missing same-board proof",
          fileOpenedStory.copy(storyProof = StoryProof.untrustedLegalLine(fileOpenedMove)),
          "StoryProof same-board proof"
        ),
        DiagnosticFixture(
          "king missing coordinate",
          checkGivenStory.copy(target = None),
          "Story identity target"
        ),
        DiagnosticFixture(
          "source blocked by board-backed proof rules",
          sourceStory,
          "Story writer"
        ),
        DiagnosticFixture(
          "engine refutes existing Story",
          SceneMaterial.withEngineCheck(materialStory, materialEngineCheck(EngineCheckStatus.Refutes)).get,
          "engine_refutes"
        ),
        DiagnosticFixture(
          "support row does not speak",
          supportVerdict.story,
          "non_lead_role",
          verdictOverride = Some(supportVerdict)
        )
      )

    val diagnostics = fixtures.map: fixture =>
      val verdict = fixture.verdictOverride.getOrElse(StoryTable.choose(Vector(fixture.story)).head)
      val diagnostic = verdict.proofDeficitDiagnostic.getOrElse(
        fail(s"${fixture.label} did not produce an internal diagnostic")
      )

      assertEquals(verdict.leadAllowed && verdict.role == Role.Lead && !verdict.engineStrengthLimited, false, fixture.label)
      assertNoStandaloneText(fixture.label, verdict)
      assertDiagnosticReports(fixture, diagnostic)
      assertNoNearClaimWording(fixture.label, diagnostic)
      diagnostic

    assertEquals(diagnostics.map(_.getClass), Vector.fill(fixtures.size)(diagnostics.head.getClass))
    assertEquals(
      diagnostics.map(_.proofCoordinates.getClass),
      Vector.fill(fixtures.size)(diagnostics.head.proofCoordinates.getClass)
    )

  test("Stage-5 fixtures keep existing Story labels and do not open public diagnostics"):
    val rows =
      Vector(
        TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(loosePieceProof = None),
        fileOpenedStory.copy(storyProof = StoryProof.untrustedLegalLine(fileOpenedMove)),
        checkGivenStory.copy(target = None),
        sourceStory,
        SceneMaterial.withEngineCheck(materialStory, materialEngineCheck(EngineCheckStatus.Refutes)).get,
        supportVerdict.story
      )
    val verdicts = rows.map(row => StoryTable.choose(Vector(row)).head)

    assert(rows.exists(_.tactic.contains(Tactic.Loose)))
    assert(rows.exists(_.scene == Scene.FileOpened))
    assert(rows.exists(_.scene == Scene.CheckGiven))
    assert(rows.exists(_.scene == Scene.Source))
    assert(verdicts.exists(_.engineCheckStatus.contains(EngineCheckStatus.Refutes)))
    assert(supportVerdict.role == Role.Support || supportVerdict.role == Role.Context)

    verdicts.foreach: verdict =>
      assertEquals(verdict.values.size, Verdict.Size)
      assertEquals(verdict.values, verdict.values)
      assertEquals(verdict.values.map(_.toString).mkString(" ").contains("ProofDeficitDiagnostic"), false)

  private final case class DiagnosticFixture(
      label: String,
      story: Story,
      expectedDeficit: String,
      verdictOverride: Option[Verdict] = None
  )

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val fileOpenedFen = "4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1"
  private val fileOpenedMove = Line(Square('e', 5), Square('d', 6))
  private val checkGivenFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkGivenMove = Line(Square('d', 2), Square('e', 2))
  private val materialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"
  private val materialMove = Line(Square('d', 4), Square('e', 5))

  private def fileOpenedStory: Story =
    SceneFileOpened.write(board(fileOpenedFen), fileOpenedMove).get

  private def checkGivenStory: Story =
    SceneCheckGiven.write(board(checkGivenFen), checkGivenMove).get

  private def materialStory: Story =
    SceneMaterial.write(board(materialFen), materialMove).get

  private def sourceStory: Story =
    val facts = board(materialFen)
    Story(
      scene = Scene.Source,
      proof = orderingProof,
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 5)),
      anchor = Some(Square('d', 4)),
      route = Some(materialMove),
      routeSan = BoardFacts.sanFor(facts, materialMove),
      storyProof = StoryProof.fromBoardFacts(facts, materialMove)
    )

  private def supportVerdict: Verdict =
    val lead = materialStory.copy(proof = orderingProof)
    val support = TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(proof = orderingProof)
    StoryTable.choose(Vector(lead, support)).find(_.story.tactic.contains(Tactic.Loose)).get

  private def materialEngineCheck(status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = board(materialFen),
      story = Some(materialStory),
      engineLine = Some(EngineLine(Vector(materialMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(300)),
      evalAfter = Some(EngineEval(260)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def assertDiagnosticReports(fixture: DiagnosticFixture, diagnostic: ProofDeficitDiagnostic): Unit =
    val allDiagnosticText =
      Vector(
        diagnostic.roleReason.toVector,
        diagnostic.blockedBy,
        diagnostic.missingSidecar,
        Vector(diagnostic.reason)
    ).flatten.mkString(" ")
    assert(allDiagnosticText.contains(fixture.expectedDeficit), fixture.label)
    assertEquals(diagnostic.reason.nonEmpty, true, fixture.label)
    assertEquals(
      diagnostic.boardFactsPresent.contains("story_identity"),
      !fixture.expectedDeficit.startsWith("Story identity"),
      fixture.label
    )

  private def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertNoNearClaimWording(label: String, diagnostic: ProofDeficitDiagnostic): Unit =
    val text =
      Vector(
        diagnostic.storyIdentityLabel,
        diagnostic.roleReason.getOrElse(""),
        diagnostic.blockedBy.mkString(" "),
        diagnostic.missingSidecar.mkString(" "),
        diagnostic.reason
      ).mkString(" ").toLowerCase(java.util.Locale.ROOT)
    Vector(
      "almost overload",
      "almost material",
      "probably checkmate",
      "strategy",
      "conversion",
      "pressure",
      "initiative",
      "best move",
      "only move",
      "forced",
      "winning",
      "decisive",
      "tell user"
    ).foreach: forbidden =>
      assertEquals(text.contains(forbidden), false, s"$label leaked $forbidden")

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def orderingProof: Proof =
    Proof(
      boardProof = 80,
      lineProof = 80,
      ownerProof = 80,
      anchorProof = 80,
      routeProof = 80,
      persistence = 80,
      immediacy = 80,
      forcing = 80,
      conversionPrize = 80,
      counterplayRisk = 20,
      kingHeat = 80,
      pieceSupport = 80,
      pawnSupport = 80,
      sourceFit = 80,
      novelty = 80,
      clarity = 80
    )
