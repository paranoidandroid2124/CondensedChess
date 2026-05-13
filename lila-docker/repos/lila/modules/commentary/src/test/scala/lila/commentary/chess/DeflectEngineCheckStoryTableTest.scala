package lila.commentary.chess

class DeflectEngineCheckStoryTableTest extends munit.FunSuite:

  test("EngineCheck attaches only to existing proof-backed Deflect"):
    val story = deflectStory
    val supporting = engineCheckFromStory(story, EngineCheckStatus.Supports)
    val capped = engineCheckFromStory(story, EngineCheckStatus.Caps)
    val missingProof = story.copy(deflectProof = None)
    val incompleteProof =
      story.copy(
        deflectProof = story.deflectProof.map(
          _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("DeflectProof", Vector("reply left unproven"))))
        )
      )

    assertEquals(supporting.storyBound, true)
    assertEquals(supporting.evidenceReady, true)
    assertEquals(TacticDeflect.withEngineCheck(story, supporting).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Supports)))
    assertEquals(TacticDeflect.withEngineCheck(story, capped).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Caps)))
    assertEquals(TacticDeflect.withEngineCheck(missingProof, supporting), None)
    assertEquals(TacticDeflect.withEngineCheck(incompleteProof, supporting), None)
    assertEquals(TacticDeflect.withEngineCheck(story, engineOnlyCheck), None)

  test("StoryTable orders proof-backed Deflect rows without creating Deflect"):
    val lead = StoryTable.choose(Vector(deflectStory)).head
    val support =
      StoryTable
        .choose(Vector(deflectStory, deflectStory.copy(proof = weakerLeadProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected weaker proof-backed Deflect to become Support"))
    val incomplete =
      StoryTable
        .choose(
          Vector(
            deflectStory.copy(
              deflectProof = deflectStory.deflectProof.map(
                _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("DeflectProof", Vector("target still guarded"))))
              )
            )
          )
        )
        .head
    val forged =
      StoryTable.choose(Vector(deflectStory.copy(anchor = Some(defenderBeforeReply)))).head
    val diagnosticOnly =
      StoryTable
        .choose(
          Vector(
            deflectStory.copy(
              writer = None,
              deflectProof = None
            )
          )
        )
        .head

    assertEquals(lead.role, Role.Lead)
    assertEquals(support.role, Role.Support)
    assertEquals(incomplete.role, Role.Blocked)
    assertEquals(forged.role, Role.Blocked)
    assertEquals(diagnosticOnly.role, Role.Blocked)
    assertEquals(diagnosticOnly.story.tactic, Some(Tactic.Deflect))

    Vector(support, incomplete, forged, diagnosticOnly).foreach: verdict =>
      assertNoPublicDeflectText(verdict, verdict.toString)

  test("supported capped refuted and raw engine evidence stay bounded for Deflect"):
    val supported = StoryTable.choose(Vector(TacticDeflect.withEngineCheck(deflectStory, engineCheckFromStory(deflectStory, EngineCheckStatus.Supports)).get)).head
    val capped = StoryTable.choose(Vector(TacticDeflect.withEngineCheck(deflectStory, engineCheckFromStory(deflectStory, EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(TacticDeflect.withEngineCheck(deflectStory, engineCheckFromStory(deflectStory, EngineCheckStatus.Refutes)).get)).head

    assertEquals(supported.role, Role.Lead)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(supported).map(_.allowedClaim), Some(Some(ExplanationClaim.DeflectsDefender)))

    Vector(capped, refuted).foreach: verdict =>
      assertNoPublicDeflectText(verdict, verdict.toString)

  private def assertNoPublicDeflectText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan, None, label)
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)
    assertEquals(plan.flatMap(plan => DeterministicRenderer.fromPlan(plan).flatMap(LlmNarrationSmoke.codexCliPrompt(plan, _))), None, label)

  private def engineCheckFromStory(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      deflectFacts,
      Some(story),
      engineLine = Some(EngineLine(Vector(deflectMove, Line(Square('h', 8), Square('h', 7))))),
      replyLine = Some(EngineLine(Vector(deflectReply))),
      evalBefore = Some(EngineEval(1234)),
      evalAfter = Some(if status == EngineCheckStatus.Refutes then EngineEval(-432) else EngineEval(1200)),
      depth = Some(31),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def engineOnlyCheck: EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(deflectMove),
      engineLine = Some(EngineLine(Vector(deflectMove))),
      replyLine = Some(EngineLine(Vector(deflectReply))),
      evalBefore = Some(EngineEval(1234)),
      evalAfter = Some(EngineEval(1200)),
      depth = Some(31),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

  private def deflectStory: Story =
    TacticDeflect.write(deflectFacts, Some(deflectMove), Some(deflectReply), Some(defenderBeforeReply), Some(deflectTarget)).get

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Deflect EngineCheck FEN: $fen -> $error"), identity)

  private val deflectFacts = facts("7k/8/4b3/3n4/8/8/7P/7K w - - 0 1")
  private val deflectMove = Line(Square('h', 2), Square('h', 3))
  private val deflectReply = Line(Square('e', 6), Square('g', 4))
  private val defenderBeforeReply = Square('e', 6)
  private val deflectTarget = Square('d', 5)

  private val weakerLeadProof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 95,
      immediacy = 80,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 95,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 95
    )
