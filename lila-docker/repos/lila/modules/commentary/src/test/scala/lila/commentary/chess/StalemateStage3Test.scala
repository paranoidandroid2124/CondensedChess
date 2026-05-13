package lila.commentary.chess

class StalemateStage3Test extends munit.FunSuite:

  private val stalemateFen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1"
  private val stalemateMove = Line(Square('g', 5), Square('g', 6))
  private val checkmateFen = "k7/2Q5/2K5/8/8/8/8/8 w - - 0 1"
  private val checkmateMove = Line(Square('c', 7), Square('b', 7))
  private val legalWithRepliesFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val legalWithRepliesMove = Line(Square('d', 2), Square('a', 2))

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def supportingEngine(move: Line): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(move),
      engineLine = Some(EngineLine(Vector(move))),
      replyLine = Some(EngineLine(Vector(move))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

  private def assertBlocked(row: Story, label: String): Unit =
    val verdict = StoryTable.choose(Vector(row)).head
    assertEquals(verdict.role, Role.Blocked, label)
    assertEquals(verdict.leadAllowed, false, label)
    assertEquals(verdict.selected, false, label)
    assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("Stalemate-3 false-positive boards stay silent unless not-in-check and no-legal-move both hold"):
    val illegalMove = Line(Square('g', 5), Square('h', 8))
    val sideToMoveConfusionMove = Line(Square('e', 7), Square('e', 5))
    val sideToMoveConfusionFacts =
      facts("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

    val checkmateProof = StalemateProof.fromBoardFacts(facts(checkmateFen), checkmateMove)
    assertEquals(checkmateProof.legalMove, true)
    assertEquals(checkmateProof.afterBoardRivalSideHasNoLegalMoves, true)
    assertEquals(checkmateProof.afterBoardRivalSideNotInCheck, false)
    assertEquals(checkmateProof.complete, false)
    assertEquals(checkmateProof.stalemateProducedByLegalMove, false)

    val withReplies = StalemateProof.fromBoardFacts(facts(legalWithRepliesFen), legalWithRepliesMove)
    assertEquals(withReplies.legalMove, true)
    assertEquals(withReplies.afterBoardRivalSideNotInCheck, true)
    assertEquals(withReplies.afterBoardRivalSideHasNoLegalMoves, false)
    assertEquals(withReplies.complete, false)

    val illegal = StalemateProof.fromBoardFacts(facts(stalemateFen), illegalMove)
    assertEquals(illegal.legalMove, false)
    assertEquals(illegal.exactAfterBoardReplay, false)
    assertEquals(illegal.complete, false)

    val sideConfused = StalemateProof.fromBoardFacts(sideToMoveConfusionFacts, sideToMoveConfusionMove)
    assertEquals(sideConfused.legalMove, true)
    assertEquals(sideConfused.exactAfterBoardReplay, false)
    assertEquals(sideConfused.complete, false)

    assertEquals(SceneStalemate.write(facts(checkmateFen), checkmateMove), None)
    assertEquals(SceneStalemate.write(facts(legalWithRepliesFen), legalWithRepliesMove), None)
    assertEquals(SceneStalemate.write(facts(stalemateFen), illegalMove), None)
    assertEquals(SceneStalemate.write(sideToMoveConfusionFacts, sideToMoveConfusionMove), None)

  test("Stalemate-3 forged rows with missing proof identity or contaminated sidecars are blocked"):
    val story = SceneStalemate.write(facts(stalemateFen), stalemateMove).get
    val missing = Vector(BoardFacts.MissingEvidence("StalemateProof", Vector("negative corpus")))

    val missingExactReplay = story.copy(
      stalemateProof = story.stalemateProof.map(
        _.copy(exactAfterBoardReplay = false, stalemateProducedByLegalMove = false, missingEvidence = missing)
      )
    )
    val rivalInCheck = story.copy(
      stalemateProof = story.stalemateProof.map(
        _.copy(afterBoardRivalSideNotInCheck = false, stalemateProducedByLegalMove = false, missingEvidence = missing)
      )
    )
    val rivalHasMove = story.copy(
      stalemateProof = story.stalemateProof.map(
        _.copy(afterBoardRivalSideHasNoLegalMoves = false, stalemateProducedByLegalMove = false, missingEvidence = missing)
      )
    )
    val missingRivalKing = story.copy(
      target = None,
      stalemateProof = story.stalemateProof.map(
        _.copy(rivalKingSquareAfter = None, stalemateProducedByLegalMove = false, missingEvidence = missing)
      )
    )
    val incompleteStoryProof = story.copy(storyProof = StoryProof.empty)
    val incompleteStalemateProof = story.copy(
      stalemateProof = story.stalemateProof.map(_.copy(missingEvidence = missing))
    )
    val sideConfused = story.copy(side = Side.Black)
    val writerless = story.copy(writer = None)
    val contaminatedSidecar = story.copy(
      scene = Scene.Checkmate,
      writer = Some(StoryWriter.SceneCheckmate),
      checkmateProof = None
    )
    val sanOrResultOnly = story.copy(
      storyProof = StoryProof.empty,
      stalemateProof = None,
      routeSan = Some("Qg6 1/2-1/2")
    )
    val engineOnlyDrawClaim = story.copy(
      stalemateProof = None,
      engineCheck = Some(supportingEngine(stalemateMove)),
      routeSan = Some("Qg6 tablebase draw")
    )

    Vector(
      "missing exact after-board replay" -> missingExactReplay,
      "after-board rival side in check" -> rivalInCheck,
      "after-board rival side has legal move" -> rivalHasMove,
      "missing rival king square" -> missingRivalKing,
      "incomplete StoryProof" -> incompleteStoryProof,
      "incomplete StalemateProof" -> incompleteStalemateProof,
      "side-to-move confusion" -> sideConfused,
      "writerless row" -> writerless,
      "contaminated sidecar row" -> contaminatedSidecar,
      "SAN or result notation without proof" -> sanOrResultOnly,
      "engine/tablebase-only draw claim" -> engineOnlyDrawClaim
    ).foreach: (label, row) =>
      assertBlocked(row, label)

  test("Stalemate-3 opens no draw result or public stalemate wording downstream"):
    val story = SceneStalemate.write(facts(stalemateFen), stalemateMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("stalemates"))

    val claimKeys = ExplanationClaim.values.map(_.key).toVector
    Vector(
      "draw",
      "draws_endgame",
      "tablebase_draw",
      "saves_game",
      "throws_win",
      "blunder",
      "best_move",
      "only_move",
      "forced",
      "winning",
      "losing",
      "decisive",
      "no_counterplay",
      "repetition",
      "fifty_move",
      "insufficient_material",
      "resignation",
      "timeout"
    ).foreach: closedKey =>
      assertEquals(claimKeys.contains(closedKey), false, closedKey)
