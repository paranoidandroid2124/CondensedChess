package lila.commentary.chess

class DecoyEngineCheckStoryTableTest extends munit.FunSuite:

  test("EngineCheck attaches only to existing proof-backed Decoy"):
    val story = decoyStory
    val supporting = engineCheckFromStory(story, EngineCheckStatus.Supports)
    val capped = engineCheckFromStory(story, EngineCheckStatus.Caps)
    val missingProof = story.copy(decoyProof = None)
    val incompleteProof =
      story.copy(
        decoyProof = story.decoyProof.map(
          _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("DecoyProof", Vector("reply landing unproven"))))
        )
      )

    assertEquals(supporting.storyBound, true)
    assertEquals(supporting.evidenceReady, true)
    assertEquals(TacticDecoy.withEngineCheck(story, supporting).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Supports)))
    assertEquals(TacticDecoy.withEngineCheck(story, capped).map(_.engineCheck.map(_.status)), Some(Some(EngineCheckStatus.Caps)))
    assertEquals(TacticDecoy.withEngineCheck(missingProof, supporting), None)
    assertEquals(TacticDecoy.withEngineCheck(incompleteProof, supporting), None)
    assertEquals(TacticDecoy.withEngineCheck(story, engineOnlyCheck), None)

  test("StoryTable orders proof-backed Decoy rows without creating Decoy"):
    val lead = StoryTable.choose(Vector(decoyStory)).head
    val support =
      StoryTable
        .choose(Vector(decoyStory, decoyStory.copy(proof = weakerProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected weaker proof-backed Decoy to become Support"))
    val incomplete =
      StoryTable
        .choose(
          Vector(
            decoyStory.copy(
              decoyProof = decoyStory.decoyProof.map(
                _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("DecoyProof", Vector("Trap follow-up mismatch"))))
              )
            )
          )
        )
        .head
    val forged = StoryTable.choose(Vector(decoyStory.copy(target = Some(Square('b', 8))))).head
    val diagnosticOnly =
      StoryTable.choose(Vector(forgedClosedDecoyRow)).headOption
    val trapProofOnly =
      StoryTable
        .choose(
          Vector(
            decoyStory.copy(
              writer = None,
              decoyProof = None,
              trapProof = Some(completeTrapFollowUp)
            )
          )
        )

    assertEquals(lead.role, Role.Lead)
    assertEquals(support.role, Role.Support)
    assertEquals(incomplete.role, Role.Blocked)
    assertEquals(forged.role, Role.Blocked)
    assertEquals(diagnosticOnly, None)
    assertEquals(trapProofOnly, Vector.empty)

    Vector(support, incomplete, forged).foreach: verdict =>
      assertNoDecoySpeech(verdict, verdict.toString)

  test("supported capped refuted and raw engine evidence stay bounded for Decoy"):
    val supported =
      StoryTable
        .choose(Vector(TacticDecoy.withEngineCheck(decoyStory, engineCheckFromStory(decoyStory, EngineCheckStatus.Supports)).get))
        .head
    val capped =
      StoryTable
        .choose(Vector(TacticDecoy.withEngineCheck(decoyStory, engineCheckFromStory(decoyStory, EngineCheckStatus.Caps)).get))
        .head
    val refuted =
      StoryTable
        .choose(Vector(TacticDecoy.withEngineCheck(decoyStory, engineCheckFromStory(decoyStory, EngineCheckStatus.Refutes)).get))
        .head
    val noisyRows = Vector(sourceRow, storyProofFailure("target missing"), storyProofFailure("route missing"))
    val lowEvalLead =
      StoryTable
        .choose(TacticDecoy.withEngineCheck(decoyStory, engineCheckFromStory(decoyStory, EngineCheckStatus.Supports)).get +: noisyRows)
        .find(_.role == Role.Lead)
        .map(verdictSignature)
    val highEvalLead =
      StoryTable
        .choose(
          TacticDecoy.withEngineCheck(
            decoyStory,
            engineCheckFromStory(decoyStory, EngineCheckStatus.Supports, before = -800, after = 800, depth = 31)
          ).get +: noisyRows.reverse
        )
        .find(_.role == Role.Lead)
        .map(verdictSignature)

    assertEquals(supported.role, Role.Lead)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(lowEvalLead, Some(("Tactic.Decoy", Some(StoryWriter.TacticDecoy), Role.Lead)))
    assertEquals(highEvalLead, lowEvalLead)

    assertDecoyPlanNoRawEngine(supported, "supported")
    Vector(capped, refuted).foreach: verdict =>
      assertNoDecoySpeech(verdict, verdict.toString)

  private def assertDecoyPlanNoRawEngine(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
    val prompt = rendered.flatMap(line => plan.flatMap(LlmNarrationSmoke.codexCliPrompt(_, line)))
    val text = rendered.map(_.text.toLowerCase).getOrElse("")
    val promptText = prompt.getOrElse("").toLowerCase

    assertEquals(plan.flatMap(_.allowedClaim).map(_.key), Some("decoys_piece"), label)
    assertEquals(rendered.map(_.claimKey), Some("decoys_piece"), label)
    assertEquals(prompt.nonEmpty, true, label)
    Vector("engine", "pv", "eval", "depth", "supports", "caps", "refutes").foreach: phrase =>
      assertEquals(text.contains(phrase), false, phrase)
    Vector("enginecheck", "engineeval", "engineline", "raw pv", "principal variation", "eval", "depth", "supports", "caps", "refutes").foreach: phrase =>
      assertEquals(promptText.contains(phrase), false, phrase)

  private def assertNoDecoySpeech(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
    val prompt = rendered.flatMap(line => plan.flatMap(LlmNarrationSmoke.codexCliPrompt(_, line)))

    assertEquals(plan, None, label)
    assertEquals(rendered, None, label)
    assertEquals(prompt, None, label)

  private def verdictSignature(verdict: Verdict): (String, Option[StoryWriter], Role) =
    (
      verdict.story.tactic.map(tactic => s"Tactic.$tactic").getOrElse(verdict.story.scene.toString),
      verdict.story.writer,
      verdict.role
    )

  private def storyProofFailure(label: String): Story =
    val base = decoyStory.copy(proof = proofScore(99))
    label match
      case "target missing" => base.copy(target = None)
      case "route missing" => base.copy(route = None)
      case _ => base.copy(anchor = None)

  private def sourceRow: Story =
    Story(
      scene = Scene.Source,
      proof = proofScore(100),
      side = Side.White,
      rival = Side.Black,
      target = Some(decoySquare),
      anchor = Some(Square('f', 2)),
      route = Some(decoyMove),
      routeSan = Some("Bf2"),
      storyProof = StoryProof.fromBoardFacts(decoyFacts, decoyMove)
    )

  private def forgedClosedDecoyRow: Story =
    Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.Decoy),
      proof = proofScore(100),
      side = Side.White,
      rival = Side.Black,
      target = Some(decoySquare),
      anchor = Some(Square('f', 2)),
      route = Some(decoyMove),
      routeSan = Some("Bf2"),
      storyProof = StoryProof.fromBoardFacts(decoyFacts, decoyMove)
    )

  private def engineCheckFromStory(
      story: Story,
      status: EngineCheckStatus,
      before: Int = 0,
      after: Int = 0,
      depth: Int = 18
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = decoyFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(decoyMove, Line(Square('e', 8), Square('e', 7))))),
      replyLine = Some(EngineLine(Vector(decoyReply))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(if status == EngineCheckStatus.Refutes then EngineEval(-200) else EngineEval(after)),
      depth = Some(depth),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def engineOnlyCheck: EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(decoyMove),
      engineLine = Some(EngineLine(Vector(decoyMove))),
      replyLine = Some(EngineLine(Vector(decoyReply))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

  private def decoyStory: Story =
    TacticDecoy.write(
      decoyFacts,
      Some(decoyMove),
      Some(decoyReply),
      Some(decoyNamedPiece),
      Some(decoySquare),
      Some(completeTrapFollowUp)
    ).get

  private def completeTrapFollowUp: TrapProof =
    TrapProof.fromBoardFacts(afterReplyTrapFacts, trapMove)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Decoy EngineCheck FEN: $fen -> $error"), identity)

  private def proofScore(score: Int): Proof =
    Proof(
      boardProof = score,
      lineProof = score,
      ownerProof = score,
      anchorProof = score,
      routeProof = score,
      persistence = score,
      immediacy = score,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = score,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = score
    )

  private val weakerProof =
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

  private val decoyFacts = board("4k3/8/1nb5/8/8/8/8/R3B1K1 w - - 0 1")
  private val afterReplyTrapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val decoyMove = Line(Square('e', 1), Square('f', 2))
  private val decoyReply = Line(Square('b', 6), Square('a', 8))
  private val decoyNamedPiece = Square('b', 6)
  private val decoySquare = Square('a', 8)
  private val trapMove = Line(Square('a', 1), Square('a', 7))
