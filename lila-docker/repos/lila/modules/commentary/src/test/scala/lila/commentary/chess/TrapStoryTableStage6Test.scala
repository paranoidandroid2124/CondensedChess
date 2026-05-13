package lila.commentary.chess

class TrapStoryTableStage6Test extends munit.FunSuite:

  test("Stage-6 proof-backed Trap alone can become Lead"):
    val story = trapStory
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(verdict.story.tactic, Some(Tactic.Trap))
    assertEquals(verdict.story.writer, Some(StoryWriter.TacticTrap))
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)

  test("Stage-6 Trap with stronger existing owner orders deterministically and stays silent when non Lead"):
    val trap = trapStory
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get.copy(proof = proofScore(96))

    Vector(Vector(trap, queenHit), Vector(queenHit, trap)).foreach: rows =>
      val verdicts = StoryTable.choose(rows)
      val lead = verdicts.find(_.role == Role.Lead).get
      val trapVerdict = verdicts.find(_.story.tactic.contains(Tactic.Trap)).get

      assertEquals(lead.story.tactic, Some(Tactic.QueenHit))
      assertEquals(lead.story.writer, Some(StoryWriter.TacticQueenHit))
      assertEquals(trapVerdict.role, Role.Support)
      assertEquals(ExplanationPlan.fromSelected(trapVerdict), None)
      assertEquals(ExplanationPlan.fromSelected(trapVerdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("Stage-6 incomplete forged refuted and closed Trap rows cannot Lead or speak"):
    val story = trapStory
    val incomplete = story.copy(
      trapProof = story.trapProof.map(_.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("TrapProof", Vector("gap")))))
    )
    val forged = story.copy(target = Some(Square('b', 6)))
    val refuted = TacticTrap.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Refutes)).get
    val closed = forgedClosedTrapRow

    Vector(
      "incomplete" -> incomplete,
      "forged" -> forged,
      "refuted" -> refuted
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    assertEquals(StoryTable.choose(Vector(closed)), Vector.empty)

  test("Stage-6 ordering ignores raw eval proof failure text and source rows"):
    val supportedLowEval =
      TacticTrap.withEngineCheck(trapStory, engineCheck(trapStory, EngineCheckStatus.Supports)).get
    val supportedHighEval =
      TacticTrap
        .withEngineCheck(
          trapStory,
          engineCheck(trapStory, EngineCheckStatus.Supports, before = -800, after = 800)
        )
        .get
    val noisyRows = Vector(sourceRow, storyProofFailure("target missing"), storyProofFailure("route missing"))

    val lowEvalLead = StoryTable.choose(supportedLowEval +: noisyRows).find(_.role == Role.Lead).map(verdictSignature)
    val highEvalLead = StoryTable.choose(supportedHighEval +: noisyRows.reverse).find(_.role == Role.Lead).map(verdictSignature)

    assertEquals(lowEvalLead, Some(("Tactic.Trap", Some(StoryWriter.TacticTrap), Role.Lead)))
    assertEquals(highEvalLead, lowEvalLead)

  private def verdictSignature(verdict: Verdict): (String, Option[StoryWriter], Role) =
    (
      verdict.story.tactic.map(tactic => s"Tactic.$tactic").getOrElse(verdict.story.scene.toString),
      verdict.story.writer,
      verdict.role
    )

  private def storyProofFailure(label: String): Story =
    val base = trapStory.copy(proof = proofScore(99))
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
      target = Some(Square('a', 1)),
      anchor = Some(Square('a', 1)),
      route = Some(trapMove),
      routeSan = Some("Ra7"),
      storyProof = StoryProof.fromBoardFacts(trapFacts, trapMove)
    )

  private def forgedClosedTrapRow: Story =
    Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.Trap),
      proof = proofScore(100),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('a', 8)),
      anchor = Some(Square('a', 7)),
      route = Some(trapMove),
      routeSan = Some("Ra7"),
      storyProof = StoryProof.fromBoardFacts(trapFacts, trapMove)
    )

  private def engineCheck(
      story: Story,
      status: EngineCheckStatus,
      before: Int = 0,
      after: Int = 0
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = trapFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(trapMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap Stage-6 FEN: $fen -> $error"), identity)

  private def trapStory: Story =
    TacticTrap.write(trapFacts, Some(trapMove)).get

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

  private val trapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
