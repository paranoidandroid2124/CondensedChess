package lila.commentary.chess

class ProofDeficitDiagnosticsStage3Test extends munit.FunSuite:

  test("Stage-3 selected uncapped Lead behavior is unchanged and has no diagnostic attachment"):
    val story = materialStory
    val verdict = StoryTable.choose(Vector(story)).head
    val values = verdict.values
    val plan = ExplanationPlan.fromSelected(verdict)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.proofDeficitDiagnostic, None)
    assertEquals(verdict.values, values)
    assertEquals(plan.nonEmpty, true)

  test("Stage-3 attaches diagnostics to Support Context and Blocked rows without standalone text"):
    val lead = materialStory.copy(proof = orderingProof)
    val support = baseStory.copy(proof = orderingProof)
    val context = baseStory.copy(proof = contextProof)
    val blocked = baseStory.copy(writer = None)

    val supportVerdict = StoryTable.choose(Vector(lead, support)).find(_.story.tactic.contains(Tactic.Loose)).get
    val contextVerdict = StoryTable.choose(Vector(context)).head
    val blockedVerdict = StoryTable.choose(Vector(blocked)).head

    Vector(
      "support" -> supportVerdict,
      "context" -> contextVerdict,
      "blocked" -> blockedVerdict
    ).foreach: (label, verdict) =>
      assertEquals(verdict.proofDeficitDiagnostic.nonEmpty, true, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    assertEquals(supportVerdict.role, Role.Support)
    assertEquals(contextVerdict.role, Role.Context)
    assertEquals(blockedVerdict.role, Role.Blocked)

  test("Stage-3 attaches diagnostics to capped and refuted StoryTable rows without allowing speech"):
    val capped = SceneMaterial.withEngineCheck(materialStory, materialEngineCheck(EngineCheckStatus.Caps)).get
    val refuted = SceneMaterial.withEngineCheck(materialStory, materialEngineCheck(EngineCheckStatus.Refutes)).get
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    val refutedVerdict = StoryTable.choose(Vector(refuted)).head

    assertEquals(cappedVerdict.role, Role.Lead)
    assertEquals(cappedVerdict.leadAllowed, true)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(cappedVerdict.proofDeficitDiagnostic.nonEmpty, true)
    assertEquals(cappedVerdict.proofDeficitDiagnostic.get.blockedBy.contains("engine_strength_limited"), true)
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict)
    assertEquals(cappedPlan.flatMap(_.allowedClaim), None)
    assertEquals(cappedPlan.flatMap(DeterministicRenderer.fromPlan), None)

    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(refutedVerdict.proofDeficitDiagnostic.nonEmpty, true)
    assertEquals(refutedVerdict.proofDeficitDiagnostic.get.blockedBy.contains("engine_refutes"), true)
    val refutedPlan = ExplanationPlan.fromSelected(refutedVerdict)
    assertEquals(refutedPlan.flatMap(_.allowedClaim), None)
    assertEquals(refutedPlan.flatMap(DeterministicRenderer.fromPlan), None)

  test("Stage-3 diagnostics do not change StoryTable ordering"):
    val stories = Vector(materialStory.copy(proof = orderingProof), baseStory.copy(proof = orderingProof), baseStory.copy(writer = None))
    val first = StoryTable.choose(stories)
    val second = StoryTable.choose(stories)

    assertEquals(first.map(orderKey), second.map(orderKey))
    assertEquals(first.map(_.values), second.map(_.values))
    assertEquals(first.exists(_.proofDeficitDiagnostic.nonEmpty), true)

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val materialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"
  private val materialMove = Line(Square('d', 4), Square('e', 5))

  private def baseStory: Story =
    TacticLoose.write(board(looseFen), Some(looseMove)).get

  private def materialStory: Story =
    SceneMaterial.write(board(materialFen), materialMove).get

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

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def orderKey(verdict: Verdict): (Role, Int, Boolean, Double, Option[Tactic], Scene) =
    (verdict.role, verdict.rank, verdict.leadAllowed, verdict.strength, verdict.story.tactic, verdict.story.scene)

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

  private def contextProof: Proof =
    Proof(
      boardProof = 60,
      lineProof = 60,
      ownerProof = 60,
      anchorProof = 60,
      routeProof = 60,
      persistence = 50,
      immediacy = 50,
      forcing = 20,
      conversionPrize = 0,
      counterplayRisk = 45,
      kingHeat = 0,
      pieceSupport = 45,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 45
    )
