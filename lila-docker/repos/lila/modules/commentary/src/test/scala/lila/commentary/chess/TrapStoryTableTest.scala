package lila.commentary.chess

class TrapStoryTableTest extends munit.FunSuite:

  test("StoryTable filters closed Tactic.Trap rows before ranking"):
    val plainClosed = forgedClosedTrapRow(writer = None)
    val forgedWriter = forgedClosedTrapRow(writer = Some(StoryWriter.TacticQueenHit))

    assertEquals(StoryTable.choose(Vector(plainClosed)), Vector.empty)
    assertEquals(StoryTable.choose(Vector(forgedWriter)), Vector.empty)

  test("StoryTable keeps opened writer rows unchanged when closed Trap rows are present"):
    val queenHit = TacticQueenHit.write(board(queenHitFen), Some(queenHitMove)).get.copy(proof = orderingProof)
    val verdicts = StoryTable.choose(Vector(forgedClosedTrapRow(writer = Some(StoryWriter.TacticQueenHit)), queenHit))

    assertEquals(verdicts.size, 1)
    val verdict = verdicts.head
    assertEquals(verdict.story.tactic, Some(Tactic.QueenHit))
    assertEquals(verdict.story.writer, Some(StoryWriter.TacticQueenHit))
    assertEquals(verdict.role, Role.Lead)

  private def forgedClosedTrapRow(writer: Option[StoryWriter]): Story =
    Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.Trap),
      proof = orderingProof,
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('h', 5)),
      anchor = Some(Square('d', 2)),
      route = Some(queenHitMove),
      routeSan = Some("Rh2"),
      storyProof = StoryProof.fromBoardFacts(board(queenHitFen), queenHitMove),
      writer = writer
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap admission FEN: $fen -> $error"), identity)

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))

  private val orderingProof: Proof =
    Proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      persistence = 99,
      immediacy = 99,
      forcing = 99,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 99,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 99
    )
