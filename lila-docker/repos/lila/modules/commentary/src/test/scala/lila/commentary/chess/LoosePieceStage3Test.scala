package lila.commentary.chess

class LoosePieceStage3Test extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val guardedFen = "4k3/8/8/7b/5n2/8/3R4/4K3 w - - 0 1"
  private val kingTargetFen = "8/8/8/7k/8/8/3R4/4K3 w - - 0 1"
  private val ownTargetFen = "6k1/8/8/7B/8/8/3R4/4K3 w - - 0 1"
  private val beforeOnlyFen = "4k3/8/8/3R3b/8/8/8/4K3 w - - 0 1"
  private val blockedAfterFen = "4k3/8/8/7b/8/7N/3R4/4K3 w - - 0 1"

  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val quietMove = Line(Square('d', 2), Square('a', 2))
  private val illegalMove = Line(Square('d', 2), Square('h', 8))
  private val beforeOnlyMove = Line(Square('d', 5), Square('d', 2))

  test("Stage-3 TacticLoose stays silent for board and proof false positives"):
    val facts = board(looseFen)
    val untrusted = BoardFacts.untrusted(
      root = facts.root,
      sideToMove = facts.sideToMove,
      header = facts.header,
      sideLegal = facts.sideLegal,
      rivalLegal = facts.rivalLegal,
      control = facts.control,
      material = facts.material,
      pawns = facts.pawns,
      pieces = facts.pieces
    )

    val falsePositives = Vector(
      "illegal move" -> TacticLoose.write(facts, Some(illegalMove)),
      "missing same-board proof" -> TacticLoose.write(untrusted, Some(looseMove)),
      "missing target piece" -> TacticLoose.write(facts, Some(quietMove)),
      "target is king" -> TacticLoose.write(board(kingTargetFen), Some(looseMove)),
      "target belongs to moving side" -> TacticLoose.write(board(ownTargetFen), Some(looseMove)),
      "target was attacked only on before-board" -> TacticLoose.write(board(beforeOnlyFen), Some(beforeOnlyMove)),
      "attack line is blocked on after-board" -> TacticLoose.write(board(blockedAfterFen), Some(looseMove)),
      "target has at least one legal rival defender" -> TacticLoose.write(board(guardedFen), Some(looseMove)),
      "missing loose move" -> TacticLoose.write(facts, None)
    )

    falsePositives.foreach: (label, maybeStory) =>
      assertEquals(maybeStory, None, s"$label must not create Tactic.Loose")

  test("Stage-3 StoryTable blocks raw notation engine source writerless and contaminated loose rows"):
    val facts = board(looseFen)
    val clean = TacticLoose.write(facts, Some(looseMove)).get.copy(proof = strongProof)
    val rawNotationOnly = Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.Loose),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('h', 5)),
      anchor = Some(Square('h', 2)),
      route = Some(looseMove),
      routeSan = Some("Rh2 attacks a loose piece"),
      proof = strongProof,
      storyProof = StoryProof.fromBoardFacts(facts, looseMove)
    )
    val sourceTextOnly = rawNotationOnly.copy(
      scene = Scene.Source,
      tactic = None,
      routeSan = Some("source row says loose piece")
    )
    val enginePressureOnly = rawNotationOnly.copy(
      scene = Scene.Initiative,
      tactic = None,
      routeSan = Some("engine pressure on a loose piece"),
      engineCheck = Some(engineEvidence(looseMove))
    )
    val boardFactsOnly = clean.copy(loosePieceProof = None)
    val incompleteStoryProof = clean.copy(storyProof = StoryProof.empty)
    val incompleteLooseProof = clean.copy(
      loosePieceProof = clean.loosePieceProof.map(
        _.copy(missingEvidence = Vector(BoardFacts.MissingEvidence("LoosePieceProof", Vector("target piece identity"))))
      )
    )
    val writerless = clean.copy(writer = None)
    val contaminatedSidecar = clean.copy(scene = Scene.Material, tactic = None, writer = Some(StoryWriter.SceneMaterial))
    val hangingContamination = clean.copy(tactic = Some(Tactic.Hanging))

    Vector(
      "only raw notation says loose" -> rawNotationOnly,
      "source row text saying loose piece" -> sourceTextOnly,
      "EngineCheck-only pressure" -> enginePressureOnly,
      "BoardFacts-only loose-looking row without LoosePieceProof" -> boardFactsOnly,
      "incomplete StoryProof" -> incompleteStoryProof,
      "incomplete LoosePieceProof" -> incompleteLooseProof,
      "writerless row" -> writerless,
      "contaminated sidecar row" -> contaminatedSidecar,
      "Loose is not Hanging" -> hangingContamination
    ).foreach: (label, row) =>
      assertBlockedNoSpeech(label, row)

  test("Stage-3 defender map must be present complete and consistent"):
    val facts = board(looseFen)
    val clean = TacticLoose.write(facts, Some(looseMove)).get.copy(proof = strongProof)
    val impossibleDefenderMap = clean.copy(
      loosePieceProof = clean.loosePieceProof.map(
        _.copy(rivalLegalDefendersAfter = Vector(Piece(Side.Black, Man.Knight, Square('f', 4))))
      )
    )

    assertBlockedNoSpeech("defender map is incomplete or inconsistent", impossibleDefenderMap)

  test("Stage-3 forbidden wording has no downstream speech path"):
    val facts = board(looseFen)
    val base = TacticLoose.write(facts, Some(looseMove)).get.copy(proof = strongProof)

    val forbiddenRows =
      Vector(
        "hanging",
        "wins piece",
        "wins material",
        "free piece",
        "en prise",
        "underdefended",
        "overloaded",
        "pressure",
        "initiative",
        "tempo",
        "best move",
        "only move",
        "forced move",
        "decisive",
        "winning"
      ).map(word => base.copy(routeSan = Some(word), storyProof = StoryProof.empty))

    forbiddenRows.foreach: row =>
      assertBlockedNoSpeech(row.routeSan.get, row)

  private def assertBlockedNoSpeech(label: String, row: Story): Unit =
    val verdict = StoryTable.choose(Vector(row)).head
    assertEquals(verdict.role, Role.Blocked, s"$label must be blocked")
    assertEquals(verdict.leadAllowed, false, s"$label must not lead")
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan.flatMap(_.allowedClaim), None, s"$label must not create an allowed claim")
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, s"$label must not render public text")

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-3 FEN: $fen -> $error"), identity)

  private def engineEvidence(move: Line): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(move),
      engineLine = Some(EngineLine(Vector(move))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(30)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

  private val strongProof: Proof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 80,
      immediacy = 80,
      forcing = 80,
      conversionPrize = 80,
      counterplayRisk = 10,
      kingHeat = 0,
      pieceSupport = 80,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 80
    )
