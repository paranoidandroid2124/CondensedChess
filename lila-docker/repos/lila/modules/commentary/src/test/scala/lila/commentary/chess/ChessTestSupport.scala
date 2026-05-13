package lila.commentary.chess

import chess.format.Fen
import chess.variant
import lila.commentary.root.RootStateVector

trait ChessTestSupport extends munit.FunSuite:
  protected final def proof(
      boardProof: Int = 80,
      lineProof: Int = 80,
      ownerProof: Int = 80,
      anchorProof: Int = 80,
      routeProof: Int = 80,
      persistence: Int = 80,
      immediacy: Int = 80,
      forcing: Int = 80,
      conversionPrize: Int = 80,
      counterplayRisk: Int = 20,
      kingHeat: Int = 80,
      pieceSupport: Int = 80,
      pawnSupport: Int = 80,
      sourceFit: Int = 80,
      novelty: Int = 80,
      clarity: Int = 80
  ) =
    Proof(
      boardProof = boardProof,
      lineProof = lineProof,
      ownerProof = ownerProof,
      anchorProof = anchorProof,
      routeProof = routeProof,
      persistence = persistence,
      immediacy = immediacy,
      forcing = forcing,
      conversionPrize = conversionPrize,
      counterplayRisk = counterplayRisk,
      kingHeat = kingHeat,
      pieceSupport = pieceSupport,
      pawnSupport = pawnSupport,
      sourceFit = sourceFit,
      novelty = novelty,
      clarity = clarity
    )

  protected final val safeRoute = Line(Square('a', 2), Square('a', 3))
  protected final val safeAnchor = Square('a', 2)
  protected final val safeRivalRoute = Line(Square('h', 7), Square('h', 6))

  protected final def storyProof(line: Line = safeRoute): StoryProof =
    StoryProof.fromBoardFacts(BoardFacts.fromFen(Fen.initial).toOption.get, legalLine = line)

  protected final def untrustedStoryProof(line: Line = safeRoute): StoryProof =
    StoryProof.untrustedLegalLine(legalLine = line)

  protected final def rivalOf(side: Side): Side =
    if side == Side.White then Side.Black else Side.White

  protected final def readyHeader: BoardHeader =
    BoardHeader(known = true, fullmoveNumber = 1)

  protected final def readyMoves(
      line: Line = safeRoute,
      moveCount: Int = 1,
      captureCount: Int = 0,
      checkCount: Int = 0
  ): Moves =
    Moves(
      known = true,
      lines = if moveCount > 0 then Vector(line) else Vector.empty,
      moveCount = moveCount,
      captureCount = captureCount,
      checkCount = checkCount
    )

  protected final def readyControl: Control =
    Control(
      known = true,
      white = ControlSide(space = 1, controlledSquares = 1),
      black = ControlSide(space = 1, controlledSquares = 1),
      contestedSquares = 1
    )

  protected final def readyMaterial: Material =
    Material(
      known = true,
      white = Pieces(kings = 1),
      black = Pieces(kings = 1)
    )

  protected final def readyPawns: Pawns =
    Pawns(known = true)

  protected final def minimalBoardFacts(
      root: RootStateVector = RootStateVector.fromIndices(Vector(0)),
      sideToMove: Side = Side.White,
      header: BoardHeader = readyHeader,
      sideLegal: Moves = readyMoves(),
      rivalLegal: Moves = readyMoves(line = safeRivalRoute),
      control: Control = readyControl,
      material: Material = readyMaterial,
      pawns: Pawns = readyPawns,
      pieces: Vector[Piece] = Vector.empty
  ): BoardFacts =
    BoardFacts.untrusted(
      root = root,
      sideToMove = sideToMove,
      header = header,
      sideLegal = sideLegal,
      rivalLegal = rivalLegal,
      control = control,
      material = material,
      pawns = pawns,
      pieces = pieces
    )

  protected final def positionReady(facts: BoardFacts): Int =
    BoardMood.fromFacts(facts).scalars(BoardMood.ScalarsByName("position_ready"))

  protected final def scalar(mood: BoardMood, name: String): Int =
    mood.scalars(BoardMood.ScalarsByName(name))

  protected final def canonicalScalars(values: Vector[Int]): Vector[Int] =
    values.zipWithIndex.map:
      case (_, index) if BoardMood.ClosedScalarIndices.contains(index) => 0
      case (value, _) => value

  protected final def legalReplayCounts(fen: Fen.Full): (Int, Int, Int) =
    val position = Fen.read(variant.Standard, fen).get
    val legal = position.legalMoves.toVector
    (
      legal.size,
      legal.count(move => move.capture.isDefined || move.enpassant),
      legal.count(move => move.after.check.yes)
    )

  protected final val mobilityScalarNames = Vector(
    "white_pawn_mobility",
    "white_knight_mobility",
    "white_bishop_mobility",
    "white_rook_mobility",
    "white_queen_mobility",
    "white_king_mobility",
    "black_pawn_mobility",
    "black_knight_mobility",
    "black_bishop_mobility",
    "black_rook_mobility",
    "black_queen_mobility",
    "black_king_mobility"
  )

  protected final val safeMobilityScalarNames = Vector(
    "white_pawn_safe_mobility",
    "white_knight_safe_mobility",
    "white_bishop_safe_mobility",
    "white_rook_safe_mobility",
    "white_queen_safe_mobility",
    "white_king_safe_mobility",
    "black_pawn_safe_mobility",
    "black_knight_safe_mobility",
    "black_bishop_safe_mobility",
    "black_rook_safe_mobility",
    "black_queen_safe_mobility",
    "black_king_safe_mobility"
  )
