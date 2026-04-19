package lila.commentary.witness.u

import chess.format.Fen
import chess.{ Bishop, Color, File, Pawn, Piece, Queen, Square }

import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.root.RootAtomRegistry
import lila.commentary.root.{ RootExtractor, RootStateVector }
import lila.commentary.witness.WitnessDirection
import lila.commentary.witness.u.UWitnessTestSupport.*

class UWitnessExtractorTest extends munit.FunSuite:

  private val fen = Fen.Full.clean("4k3/8/8/3p4/3B4/8/4RQ2/4K3 w - - 0 1")

  test("u extraction context rebuilds board occupancy and root-backed helpers from root state"):
    val rootState = RootExtractor.fromFen(fen).fold(message => fail(message), identity)
    val context = UExtractionContext(rootState)

    assertEquals(context.sideToMove, Some(Color.White))
    assertEquals(context.board.pieceAt(Square.fromKey("d4").get), Some(Piece(Color.White, Bishop)))
    assertEquals(context.board.pieceAt(Square.fromKey("d5").get), Some(Piece(Color.Black, Pawn)))
    assert(context.hasPieceOn(Color.Black, Pawn, Square.fromKey("d5").get))
    assert(context.hasNeutralFile(SchemaId.OpenFile, File.E))

  test("u board helpers keep exact ray order and expose blocker geometry"):
    val rootState = RootExtractor.fromFen(fen).fold(message => fail(message), identity)
    val context = UExtractionContext(rootState)
    val rookSquare = Square.fromKey("e2").get
    val queenSquare = Square.fromKey("f2").get
    val leftEdge = Square.fromKey("a2").get

    assertEquals(
      context.board.rayUntilBlocked(rookSquare, WitnessDirection.North).map(_.key),
      Vector("e3", "e4", "e5", "e6", "e7", "e8")
    )
    assertEquals(
      context.board.firstBlocker(rookSquare, WitnessDirection.North).map(_.key),
      Some("e8")
    )
    assertEquals(
      context.board.occupiedBetween(queenSquare, leftEdge).map(_.key),
      Vector("e2")
    )
    assertEquals(
      context.board.singleBlockerBetween(queenSquare, leftEdge).map(_.key),
      Some("e2")
    )

  test("u board helpers support mobility and removal probes needed by later rows"):
    val rootState = RootExtractor.fromFen(fen).fold(message => fail(message), identity)
    val context = UExtractionContext(rootState)
    val bishopSquare = Square.fromKey("d4").get
    val queenSquare = Square.fromKey("f2").get
    val leftEdge = Square.fromKey("a2").get

    assertEquals(
      context.board.testableDirectionsFrom(bishopSquare, Bishop),
      Vector(
        WitnessDirection.NorthEast,
        WitnessDirection.SouthEast,
        WitnessDirection.SouthWest,
        WitnessDirection.NorthWest
      )
    )
    assertEquals(
      context.board.openTestableDirectionsFrom(queenSquare, Queen),
      Vector(
        WitnessDirection.North,
        WitnessDirection.East,
        WitnessDirection.South,
        WitnessDirection.NorthEast,
        WitnessDirection.SouthEast,
        WitnessDirection.NorthWest
      )
    )
    assertEquals(context.board.without(queenSquare).pieceAt(queenSquare), None)
    assertEquals(
      context.board.singleBlockerBetween(queenSquare, leftEdge),
      Some(Square.fromKey("e2").get)
    )

  test("u extraction context exposes exact pawn push geometry without legal-move inflation"):
    val fen = Fen.Full.clean("4k3/8/8/4p3/8/8/3P4/4K3 w - - 0 1")
    val rootState = RootExtractor.fromFen(fen).fold(message => fail(message), identity)
    val context = UExtractionContext(rootState)
    val pawnSquare = Square.fromKey("d2").get

    assertEquals(
      context.pawnAdvancesFrom(Color.White, pawnSquare).map(advance => advance.to.key -> advance.steps),
      Vector("d3" -> 1, "d4" -> 2)
    )
    assertEquals(context.forwardSquare(Color.White, pawnSquare), Some(Square.fromKey("d3").get))
    assertEquals(context.backwardSquare(Color.White, pawnSquare), Some(Square.fromKey("d1").get))

  test("u extractor entry points emit live witnesses after registry wiring"):
    val extraction = UWitnessExtractor.fromFen(fen).fold(message => fail(message), identity)

    assertEquals(extraction.rootState, RootExtractor.fromFen(fen).fold(message => fail(message), identity))
    val seededFen = "4k3/8/8/3p4/3B4/8/4RQ2/4K3 w - - 0 1"

    assert(findFile(seededFen, "file_lane_state", File.E, Some("open_file_state")).nonEmpty)
    assert(findPieceSquare(seededFen, "rook_on_open_file_state", "e2", Some(Color.White)).nonEmpty)
    assert(findRay(seededFen, "diagonal_lane_only", "d4", WitnessDirection.NorthEast).nonEmpty)

  test("u extractor fail-closed entry point preserves exact-board input discipline"):
    val illegalFen = Fen.Full.clean("8/8/8/8/8/8/4k3/4K3 w - - 0 1")

    assert(UWitnessExtractor.fromFenFailClosed(illegalFen).isLeft)

  test("u board reconstruction rejects duplicate piece_on occupancy on one square"):
    val duplicatedSquare = Square.fromKey("d4").get
    val malformedRoot = RootStateVector.fromIndices(
      Vector(
        RootAtomRegistry.pieceOnIndex(Color.White, Bishop, duplicatedSquare),
        RootAtomRegistry.pieceOnIndex(Color.White, Pawn, duplicatedSquare)
      )
    )

    intercept[IllegalArgumentException]:
      UExtractionContext(malformedRoot)

  test("u extraction context rejects malformed one-hot auxiliary roots"):
    val malformedRoot = RootStateVector.fromIndices(
      Vector(
        RootAtomRegistry.sideToMoveIndex(Color.White),
        RootAtomRegistry.sideToMoveIndex(Color.Black)
      )
    )

    intercept[IllegalArgumentException]:
      UExtractionContext(malformedRoot).sideToMove
