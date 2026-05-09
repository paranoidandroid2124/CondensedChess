package lila.commentary.chess

import java.nio.file.{ Files, Paths }

import chess.format.Fen
import chess.variant
import lila.commentary.root.{ RootAtomRegistry, RootStateVector }
import scala.jdk.CollectionConverters.*

class ChessFoundationTest extends munit.FunSuite:

  private def proof(
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

  private val safeRoute = Line(Square('a', 2), Square('a', 3))
  private val safeAnchor = Square('a', 2)
  private val safeRivalRoute = Line(Square('h', 7), Square('h', 6))

  private def storyProof(line: Line = safeRoute): StoryProof =
    StoryProof.fromBoardFacts(BoardFacts.fromFen(Fen.initial).toOption.get, legalLine = line)

  private def untrustedStoryProof(line: Line = safeRoute): StoryProof =
    StoryProof.untrustedLegalLine(legalLine = line)

  private def rivalOf(side: Side): Side =
    if side == Side.White then Side.Black else Side.White

  private def readyHeader: BoardHeader =
    BoardHeader(known = true, fullmoveNumber = 1)

  private def readyMoves(
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

  private def readyControl: Control =
    Control(
      known = true,
      white = ControlSide(space = 1, controlledSquares = 1),
      black = ControlSide(space = 1, controlledSquares = 1),
      contestedSquares = 1
    )

  private def readyMaterial: Material =
    Material(
      known = true,
      white = Pieces(kings = 1),
      black = Pieces(kings = 1)
    )

  private def readyPawns: Pawns =
    Pawns(known = true)

  private def minimalBoardFacts(
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

  private def positionReady(facts: BoardFacts): Int =
    BoardMood.fromFacts(facts).scalars(BoardMood.ScalarsByName("position_ready"))

  private def scalar(mood: BoardMood, name: String): Int =
    mood.scalars(BoardMood.ScalarsByName(name))

  private def canonicalScalars(values: Vector[Int]): Vector[Int] =
    values.zipWithIndex.map:
      case (_, index) if BoardMood.ClosedScalarIndices.contains(index) => 0
      case (value, _) => value

  private def legalReplayCounts(fen: Fen.Full): (Int, Int, Int) =
    val position = Fen.read(variant.Standard, fen).get
    val legal = position.legalMoves.toVector
    (
      legal.size,
      legal.count(move => move.capture.isDefined || move.enpassant),
      legal.count(move => move.after.check.yes)
    )

  private val mobilityScalarNames = Vector(
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

  private val safeMobilityScalarNames = Vector(
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

  test("BoardMood keeps the fixed dense shape"):
    assertEquals(BoardMood.Schema, 1)
    assertEquals(BoardMood.Bits, 48)
    assertEquals(BoardMood.Scalars, 256)
    assertEquals(BoardMood.Size, 3328)
    assertEquals(BoardMood.RootWordBits, 46)
    assertEquals(BoardMood.SideLegalDestinationsBit, 46)
    assertEquals(BoardMood.RivalLegalDestinationsBit, 47)
    assertEquals(BoardMood.RootWordBits + 2, BoardMood.Bits)
    assertEquals(BoardMood.empty.bits, Vector.fill(BoardMood.Bits)(0L))
    assertEquals(BoardMood.empty.scalars, Vector.fill(BoardMood.Scalars)(0))

    intercept[IllegalArgumentException]:
      BoardMood.fromPacked(Vector.fill(BoardMood.Bits - 1)(0L), Vector.fill(BoardMood.Scalars)(0))
    intercept[IllegalArgumentException]:
      BoardMood.fromPacked(Vector.fill(BoardMood.Bits)(0L), Vector.fill(BoardMood.Scalars + 1)(0))

  test("BoardMood names every bit slot and scalar slot with dense non-reserved metadata"):
    assertEquals(BoardMood.bitSlots.size, BoardMood.Bits)
    assertEquals(BoardMood.scalarSlots.size, BoardMood.Scalars)
    assertEquals(BoardMood.bitSlots.map(_.index), (0 until BoardMood.Bits).toVector)
    assertEquals(BoardMood.scalarSlots.map(_.index), (0 until BoardMood.Scalars).toVector)
    assertEquals(BoardMood.bitSlots.map(_.name).distinct.size, BoardMood.Bits)
    assertEquals(BoardMood.scalarSlots.map(_.name).distinct.size, BoardMood.Scalars)
    assert(BoardMood.bitSlots.forall(slot => slot.name.nonEmpty && slot.role.nonEmpty && slot.note.nonEmpty))
    assert(BoardMood.scalarSlots.forall: slot =>
      Vector(slot.name, slot.role, slot.zero, slot.scale, slot.source, slot.failClosed).forall(_.nonEmpty))

    val forbidden = "(?i).*(reserved|unknown|tbd).*".r
    assert(BoardMood.bitSlots.forall(slot => forbidden.findFirstIn(slot.name).isEmpty))
    assert(BoardMood.scalarSlots.forall(slot => forbidden.findFirstIn(slot.name).isEmpty))

    assertEquals(BoardMood.bitSlots(0).name, "root_atoms_0000_0063")
    assertEquals(BoardMood.bitSlots(0).role, "root-word transport")
    assert(BoardMood.bitSlots(0).note.contains("root atom index 64*0+k"))
    assertEquals(BoardMood.bitSlots(45).name, "root_atoms_2880_2890")
    assert(BoardMood.bitSlots(45).note.contains("valid only if < RootAtomRegistry.RootSize"))
    assertEquals(BoardMood.bitSlots(46).name, "side_legal_destination_union")
    assertEquals(BoardMood.bitSlots(47).name, "rival_legal_destination_union")

    assertEquals(BoardMood.scalarSlots(0).name, "side_to_move")
    assertEquals(BoardMood.scalarSlots(15).name, "position_ready")
    assertEquals(BoardMood.scalarSlots(32).name, "white_pawn_mobility")
    assertEquals(BoardMood.scalarSlots(64).name, "white_king_square")
    assertEquals(BoardMood.scalarSlots(96).name, "white_pawn_file_counts")
    assertEquals(BoardMood.scalarSlots(128).name, "white_minor_activity")
    assertEquals(BoardMood.scalarSlots(160).name, "white_forcing_move_count")
    assertEquals(BoardMood.scalarSlots(192).name, "plan_minority")
    assertEquals(BoardMood.scalarSlots(224).name, "exact_board_binding")
    assertEquals(BoardMood.scalarSlots(240).name, "ray_count")
    assertEquals(BoardMood.scalarSlots(255).name, "public_claim_pressure")
    assert(
      BoardMood.scalarSlots(BoardMood.ScalarsByName("white_pawn_file_counts")).scale.contains("packed 8-file")
    )
    assert(
      BoardMood.scalarSlots(BoardMood.ScalarsByName("black_pawn_file_counts")).scale.contains("packed 8-file")
    )
    assert(
      BoardMood.scalarSlots(BoardMood.ScalarsByName("white_king_square")).zero.contains("square index + 1")
    )
    assert(
      !BoardMood.scalarSlots(BoardMood.ScalarsByName("white_king_square")).zero.contains("A1 or no square")
    )
    assert(BoardMood.scalarSlots.forall(!_.source.contains("fromPieces fully populates")))

    val candidateNamedScalars = BoardMood.scalarNames.filter(_.contains("candidate"))
    assertEquals(
      candidateNamedScalars,
      Vector("white_candidate_passer_count", "black_candidate_passer_count")
    )

    val candidateNamedPawnFields =
      classOf[PawnSide].getDeclaredFields.map(_.getName).filter(_.toLowerCase.contains("candidate")).toVector
    assertEquals(candidateNamedPawnFields, Vector("candidatePassers"))

  test("BoardMood root words decode as root atom indices, not square masks"):
    assertEquals(BoardMood.rootAtomIndex(bitSlot = 0, bit = 0), Some(0))
    assertEquals(BoardMood.rootAtomIndex(bitSlot = 0, bit = 63), Some(63))
    assertEquals(BoardMood.rootAtomIndex(bitSlot = 45, bit = 10), Some(2890))
    assertEquals(BoardMood.rootAtomIndex(bitSlot = 45, bit = 11), None)
    assertEquals(BoardMood.rootAtomIndex(bitSlot = BoardMood.SideLegalDestinationsBit, bit = 0), None)
    assertEquals(BoardMood.rootWordSquareMask(0), None)

  test("BoardMood packs root atoms into B00 through B45 and legal masks into B46 and B47"):
    val active = Vector(0, 1, 63, 64, 65, 127, 128, 2879, 2880, RootAtomRegistry.RootSize - 1)
    val root = RootStateVector.fromIndices(active)
    val sideLegal = Square('a', 1).bit | Square('h', 8).bit
    val rivalLegal = Square('d', 4).bit | Square('e', 5).bit
    val scalars = Vector.tabulate(BoardMood.Scalars)(identity)

    val mood = BoardMood.fromRoot(
      root,
      sideLegalDestinations = sideLegal,
      rivalLegalDestinations = rivalLegal,
      scalars = Some(scalars)
    )

    assertEquals(mood.bits(0), (1L << 0) | (1L << 1) | (1L << 63))
    assertEquals(mood.bits(1), (1L << 0) | (1L << 1) | (1L << 63))
    assertEquals(mood.bits(2), 1L)
    assertEquals(mood.bits(44), 1L << 63)
    assertEquals(mood.bits(45), (1L << 0) | (1L << (RootAtomRegistry.RootSize - 1 - 45 * 64)))
    assertEquals(mood.bits(BoardMood.SideLegalDestinationsBit), sideLegal)
    assertEquals(mood.bits(BoardMood.RivalLegalDestinationsBit), rivalLegal)
    assertEquals(mood.scalars, canonicalScalars(scalars))
    assertEquals(mood.activeRootAtomIndices.sorted, active.sorted)

  test("BoardMood closes split, cut, proof, and pressure scalar slots to zero"):
    assertEquals(BoardMood.SplitScalarIndices.size, 69)
    assertEquals(BoardMood.CutScalarIndices.size, 30)
    assertEquals(BoardMood.ProofScalarIndices.size, 32)
    assertEquals(BoardMood.ClosedScalarIndices.size, 131)
    assert(BoardMood.SplitScalarIndices.forall(index => index >= 0 && index < 224))
    assert(BoardMood.CutScalarIndices.forall(index => index >= 0 && index < 224))
    assert(BoardMood.ProofScalarIndices.forall(index => index >= 224 && index < BoardMood.Scalars))
    assert((BoardMood.SplitScalarIndices & BoardMood.CutScalarIndices).isEmpty)

    Vector(
      "white_open_file_exposure",
      "black_open_file_exposure",
      "white_rook_open_file_count",
      "black_rook_open_file_count",
      "white_loose_piece_count",
      "black_loose_piece_count"
    ).foreach: name =>
      assert(
        BoardMood.ClosedScalarIndices.contains(BoardMood.ScalarsByName(name)),
        s"$name must remain a closed scalar"
      )

    val values = Vector.tabulate(BoardMood.Scalars)(index => index + 1)
    val mood = BoardMood.fromPacked(Vector.fill(BoardMood.Bits)(0L), values)

    BoardMood.ClosedScalarIndices.foreach: index =>
      assertEquals(mood.scalars(index), 0, s"${BoardMood.scalarSlots(index).name} must stay silent")

    (0 until BoardMood.Scalars)
      .filterNot(BoardMood.ClosedScalarIndices.contains)
      .foreach: index =>
        assertEquals(
          mood.scalars(index),
          values(index),
          s"${BoardMood.scalarSlots(index).name} must remain live"
        )

  test("BoardMood fromFacts is the runtime BoardMood input boundary"):
    val active = Vector(0, 70, RootAtomRegistry.RootSize - 1)
    val root = RootStateVector.fromIndices(active)
    val facts = BoardFacts.untrusted(
      root = root,
      sideToMove = Side.Black,
      header = BoardHeader(
        known = true,
        plyFromStart = 18,
        phaseTotal = 11,
        phaseNonPawn = 7,
        halfmoveClock = 2,
        fullmoveNumber = 10,
        castlingMask = 5,
        epSquare = Some(Square('e', 3)),
        inCheckMask = 2,
        snapshotPly = 19,
        hashLo = 0,
        hashHi = 0
      ),
      sideLegal = Moves(
        known = true,
        lines = Vector(Line(Square('e', 2), Square('e', 4)), Line(Square('g', 4), Square('g', 5))),
        destinationUnion = Square('h', 5).bit,
        moveCount = 37,
        captureCount = 8,
        checkCount = 3
      ),
      rivalLegal = Moves(
        known = true,
        lines = Vector(Line(Square('c', 7), Square('c', 5))),
        destinationUnion = Square('a', 6).bit,
        moveCount = 29,
        captureCount = 4,
        checkCount = 1
      ),
      control = Control(
        known = true,
        white = ControlSide(
          space = 14,
          controlledSquares = 33,
          attackedTwice = 4,
          controlledMask = Square('b', 8).bit
        ),
        black = ControlSide(
          space = 12,
          controlledSquares = 31,
          attackedTwice = 5,
          attackedSquares = Square('d', 8).bit
        ),
        contestedSquares = 17,
        spaceDiff = 2
      ),
      material = Material(
        known = true,
        white = Pieces(pawns = 7, knights = 2, bishops = 1, rooks = 2, queens = 1, kings = 1, value = 2920),
        black = Pieces(pawns = 6, knights = 1, bishops = 2, rooks = 1, queens = 1, kings = 1, value = 2650),
        diff = 270,
        imbalance = 0
      ),
      pawns = Pawns(
        known = true,
        white = PawnSide(
          fileCounts = 0x12101010,
          isolated = 1,
          backward = 2,
          doubledFiles = 1,
          passed = 2,
          candidatePassers = 3,
          protectedPassers = 1,
          fixed = 4,
          chainBases = 2,
          levers = 1,
          breakChances = 2,
          blockaded = 1,
          bestPromotionDistance = 3,
          support = 44,
          risk = 12,
          structure = 32
        ),
        black = PawnSide(
          fileCounts = 0x01010120,
          isolated = 2,
          backward = 1,
          doubledFiles = 0,
          passed = 1,
          candidatePassers = 0,
          protectedPassers = 1,
          fixed = 3,
          chainBases = 1,
          levers = 2,
          breakChances = 1,
          blockaded = 2,
          bestPromotionDistance = 4,
          support = 31,
          risk = 18,
          structure = 21
        )
      )
    )

    val mood = BoardMood.fromFacts(facts)

    assertEquals(mood.bits(0), 1L)
    assertEquals(mood.bits(1), 1L << 6)
    assertEquals(mood.bits(45), 1L << (RootAtomRegistry.RootSize - 1 - 45 * 64))
    assertEquals(mood.activeRootAtomIndices.sorted, active.sorted)
    assertEquals(
      mood.bits(BoardMood.SideLegalDestinationsBit),
      Square('e', 4).bit | Square('g', 5).bit | Square('h', 5).bit
    )
    assertEquals(
      mood.bits(BoardMood.RivalLegalDestinationsBit),
      Square('c', 5).bit | Square('a', 6).bit
    )

    def scalar(name: String) = mood.scalars(BoardMood.ScalarsByName(name))
    assertEquals(scalar("side_to_move"), Side.Black.ordinal)
    assertEquals(scalar("ply_from_start"), 18)
    assertEquals(scalar("phase_total"), 11)
    assertEquals(scalar("phase_non_pawn"), 7)
    assertEquals(scalar("halfmove_clock"), 2)
    assertEquals(scalar("fullmove_number"), 10)
    assertEquals(scalar("castling_mask"), 5)
    assertEquals(scalar("ep_square_plus_one"), Square('e', 3).index + 1)
    assertEquals(scalar("in_check_mask"), 2)
    assertEquals(scalar("legal_move_count"), 37)
    assertEquals(scalar("legal_capture_count"), 8)
    assertEquals(scalar("legal_check_count"), 3)
    assertEquals(scalar("snapshot_ply"), 19)
    assertEquals(scalar("board_hash_lo"), 0)
    assertEquals(scalar("board_hash_hi"), 0)
    assertEquals(scalar("position_ready"), 0)
    assertEquals(scalar("white_pawn_count"), 7)
    assertEquals(scalar("white_knight_count"), 2)
    assertEquals(scalar("white_bishop_count"), 1)
    assertEquals(scalar("white_rook_count"), 2)
    assertEquals(scalar("white_queen_count"), 1)
    assertEquals(scalar("white_king_count"), 1)
    assertEquals(scalar("black_pawn_count"), 6)
    assertEquals(scalar("black_knight_count"), 1)
    assertEquals(scalar("black_bishop_count"), 2)
    assertEquals(scalar("black_rook_count"), 1)
    assertEquals(scalar("black_queen_count"), 1)
    assertEquals(scalar("black_king_count"), 1)
    assertEquals(scalar("white_material"), 2920)
    assertEquals(scalar("black_material"), 2650)
    assertEquals(scalar("material_diff"), 270)
    assertEquals(scalar("material_imbalance"), 0)
    assertEquals(scalar("white_space"), 14)
    assertEquals(scalar("black_space"), 12)
    assertEquals(scalar("space_diff"), 2)
    assertEquals(scalar("white_controlled_squares"), 33)
    assertEquals(scalar("black_controlled_squares"), 31)
    assertEquals(scalar("contested_squares"), 17)
    assertEquals(scalar("white_attacked_twice"), 4)
    assertEquals(scalar("black_attacked_twice"), 5)
    assertEquals(scalar("white_pawn_file_counts"), 0x12101010)
    assertEquals(scalar("white_isolated_pawn_count"), 1)
    assertEquals(scalar("white_backward_pawn_count"), 2)
    assertEquals(scalar("white_doubled_file_count"), 1)
    assertEquals(scalar("white_passed_pawn_count"), 2)
    assertEquals(scalar("white_candidate_passer_count"), 3)
    assertEquals(scalar("white_protected_passer_count"), 0)
    assertEquals(scalar("white_fixed_pawn_count"), 4)
    assertEquals(scalar("white_chain_base_count"), 0)
    assertEquals(scalar("white_lever_count"), 1)
    assertEquals(scalar("white_break_chance_count"), 0)
    assertEquals(scalar("white_blockaded_pawn_count"), 0)
    assertEquals(scalar("white_promotion_distance_best"), 3)
    assertEquals(scalar("white_pawn_support"), 0)
    assertEquals(scalar("white_pawn_risk"), 0)
    assertEquals(scalar("white_pawn_structure_score"), 0)
    assertEquals(scalar("black_pawn_file_counts"), 0x01010120)
    assertEquals(scalar("black_isolated_pawn_count"), 2)
    assertEquals(scalar("black_backward_pawn_count"), 1)
    assertEquals(scalar("black_doubled_file_count"), 0)
    assertEquals(scalar("black_passed_pawn_count"), 1)
    assertEquals(scalar("black_candidate_passer_count"), 0)
    assertEquals(scalar("black_protected_passer_count"), 0)
    assertEquals(scalar("black_fixed_pawn_count"), 3)
    assertEquals(scalar("black_chain_base_count"), 0)
    assertEquals(scalar("black_lever_count"), 2)
    assertEquals(scalar("black_break_chance_count"), 0)
    assertEquals(scalar("black_blockaded_pawn_count"), 0)
    assertEquals(scalar("black_promotion_distance_best"), 4)
    assertEquals(scalar("black_pawn_support"), 0)
    assertEquals(scalar("black_pawn_risk"), 0)
    assertEquals(scalar("black_pawn_structure_score"), 0)
    assertEquals(scalar("white_pawn_mobility"), 0)
    assertEquals(scalar("exact_board_binding"), 0)
    assertEquals(scalar("public_claim_pressure"), 0)

  test("BoardFacts rejects non-playing sides at the runtime boundary"):
    intercept[IllegalArgumentException]:
      minimalBoardFacts(sideToMove = Side.None)
    intercept[IllegalArgumentException]:
      minimalBoardFacts(sideToMove = Side.Both)

  test("BoardMood fromFacts keeps complete manually assembled nested facts unready"):
    assertEquals(positionReady(minimalBoardFacts()), 0)
    assertEquals(positionReady(minimalBoardFacts(root = RootStateVector.empty)), 0)
    assertEquals(positionReady(minimalBoardFacts(header = readyHeader.copy(fullmoveNumber = -1))), 0)

  test("BoardMood fromFacts requires phase zero-baseline fields for readiness"):
    assertEquals(positionReady(minimalBoardFacts(header = readyHeader.copy(hashLo = 1))), 0)
    assertEquals(positionReady(minimalBoardFacts(header = readyHeader.copy(hashHi = -1))), 0)
    assertEquals(positionReady(minimalBoardFacts(material = readyMaterial.copy(imbalance = 1))), 0)

  test("BoardMood fromFacts does not mark manually assembled BoardFacts ready"):
    val manual = minimalBoardFacts()
    val runtime = BoardFacts.fromFen(Fen.initial).toOption.get

    assertEquals(positionReady(manual), 0)
    assertEquals(positionReady(runtime), 1)

  test("BoardMood fromFacts fails closed for default-ish nested facts"):
    assertEquals(positionReady(minimalBoardFacts(header = BoardHeader())), 0)
    assertEquals(positionReady(minimalBoardFacts(sideLegal = Moves())), 0)
    assertEquals(positionReady(minimalBoardFacts(rivalLegal = Moves())), 0)
    assertEquals(positionReady(minimalBoardFacts(control = Control())), 0)
    assertEquals(positionReady(minimalBoardFacts(material = Material())), 0)
    assertEquals(positionReady(minimalBoardFacts(pawns = Pawns())), 0)

  test("BoardMood fromFacts keeps known zero legal moves unready when manually assembled"):
    assertEquals(positionReady(minimalBoardFacts(sideLegal = readyMoves(moveCount = 0))), 0)
    assertEquals(positionReady(minimalBoardFacts(rivalLegal = readyMoves(moveCount = 0))), 0)

  test("BoardMood fromFacts rejects fake legal move summaries"):
    assertEquals(positionReady(minimalBoardFacts(sideLegal = Moves(known = true, moveCount = 1))), 0)
    assertEquals(
      positionReady(
        minimalBoardFacts(sideLegal =
          Moves(known = true, moveCount = 0, destinationUnion = Square('b', 3).bit)
        )
      ),
      0
    )

  test("BoardFacts fromFen builds ready exact-board facts for the initial position"):
    val facts = BoardFacts.fromFen(Fen.initial).toOption.get
    val mood = BoardMood.fromFacts(facts)

    assertEquals(scalar(mood, "position_ready"), 1)
    assertEquals(scalar(mood, "side_to_move"), Side.White.ordinal)
    assertEquals(scalar(mood, "ply_from_start"), 0)
    assertEquals(scalar(mood, "snapshot_ply"), 0)
    assertEquals(scalar(mood, "fullmove_number"), 1)
    assertEquals(scalar(mood, "castling_mask"), 15)
    assertEquals(scalar(mood, "ep_square_plus_one"), 0)
    assertEquals(scalar(mood, "in_check_mask"), 0)
    assertEquals(scalar(mood, "legal_move_count"), 20)
    assertEquals(facts.sideLegal.lines.size, 20)
    assertEquals(facts.sideLegal.san.size, 20)
    assertEquals(facts.sideLegal.sanFor(Line(Square('e', 2), Square('e', 4))), Some("e4"))
    assertEquals(facts.sideLegal.captureCount, 0)
    assertEquals(facts.sideLegal.checkCount, 0)
    assertEquals(facts.rivalLegal.moveCount, 20)
    assertEquals(scalar(mood, "phase_total"), 24)
    assertEquals(scalar(mood, "phase_non_pawn"), 14)
    assertEquals(scalar(mood, "white_material"), 4000)
    assertEquals(scalar(mood, "black_material"), 4000)
    assertEquals(scalar(mood, "material_diff"), 0)
    assertEquals(scalar(mood, "white_pawn_file_counts"), 0x11111111)
    assertEquals(scalar(mood, "black_pawn_file_counts"), 0x11111111)
    assertEquals(scalar(mood, "white_isolated_pawn_count"), 0)
    assertEquals(scalar(mood, "black_isolated_pawn_count"), 0)
    assertEquals(scalar(mood, "white_doubled_file_count"), 0)
    assertEquals(scalar(mood, "black_doubled_file_count"), 0)
    assertEquals(scalar(mood, "white_passed_pawn_count"), 0)
    assertEquals(scalar(mood, "black_passed_pawn_count"), 0)
    assertEquals(scalar(mood, "white_promotion_distance_best"), 6)
    assertEquals(scalar(mood, "black_promotion_distance_best"), 6)
    assertEquals(scalar(mood, "white_space"), 0)
    assertEquals(scalar(mood, "black_space"), 0)
    assertEquals(scalar(mood, "space_diff"), 0)
    assert(scalar(mood, "white_controlled_squares") > 0)
    assert(scalar(mood, "black_controlled_squares") > 0)
    assert(scalar(mood, "contested_squares") >= 0)
    assertEquals(scalar(mood, "board_hash_lo"), 0)
    assertEquals(scalar(mood, "board_hash_hi"), 0)
    assertEquals(scalar(mood, "exact_board_binding"), 0)
    assertEquals(scalar(mood, "public_claim_pressure"), 0)

  test("BoardFacts fromFen populates non-initial position header scalars"):
    val fen = Fen.Full("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 7 9")
    val facts = BoardFacts.fromFen(fen).toOption.get
    val mood = BoardMood.fromFacts(facts)

    assertEquals(scalar(mood, "position_ready"), 1)
    assertEquals(scalar(mood, "side_to_move"), Side.Black.ordinal)
    assertEquals(scalar(mood, "ply_from_start"), 17)
    assertEquals(scalar(mood, "snapshot_ply"), 17)
    assertEquals(scalar(mood, "halfmove_clock"), 7)
    assertEquals(scalar(mood, "fullmove_number"), 9)
    assertEquals(scalar(mood, "castling_mask"), 15)
    assertEquals(scalar(mood, "ep_square_plus_one"), Square('e', 3).index + 1)
    assertEquals(scalar(mood, "in_check_mask"), 0)
    assertEquals(scalar(mood, "phase_total"), 24)
    assertEquals(scalar(mood, "phase_non_pawn"), 14)

  test("BoardFacts fromFen derives capture and check counts from legal replay"):
    val captureFen = Fen.Full("4k3/8/8/8/8/8/4r3/4K3 w - - 0 1")
    val checkFen = Fen.Full("4k3/8/8/8/8/8/8/R3K3 w - - 0 1")

    val captureFacts = BoardFacts.fromFen(captureFen).toOption.get
    val checkFacts = BoardFacts.fromFen(checkFen).toOption.get
    val (_, expectedCaptures, _) = legalReplayCounts(captureFen)
    val (_, _, expectedChecks) = legalReplayCounts(checkFen)
    val captureMood = BoardMood.fromFacts(captureFacts)
    val checkMood = BoardMood.fromFacts(checkFacts)

    assert(expectedCaptures > 0)
    assert(expectedChecks > 0)
    assertEquals(captureFacts.sideLegal.captureCount, expectedCaptures)
    assertEquals(checkFacts.sideLegal.checkCount, expectedChecks)
    assertEquals(scalar(captureMood, "legal_capture_count"), expectedCaptures)
    assertEquals(scalar(checkMood, "legal_check_count"), expectedChecks)

  test("BoardFacts fromFen derives nonzero control diagnostics from the same board"):
    val fen = Fen.Full("4k3/8/5N2/8/5N2/8/8/4K3 b - - 0 1")
    val mood = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)

    assertEquals(scalar(mood, "position_ready"), 1)
    assert(scalar(mood, "white_controlled_squares") > 0)
    assert(scalar(mood, "white_attacked_twice") > 0)

  test("BoardFacts fromFen derives role mobility and safe mobility as diagnostics"):
    val fen = Fen.Full("4k3/7p/8/7r/3q4/2b1n3/3P3P/RNBQK3 w - - 0 1")
    val mood = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)

    assertEquals(scalar(mood, "position_ready"), 1)
    mobilityScalarNames.foreach: name =>
      assert(scalar(mood, name) > 0, s"$name should be nonzero from exact-board geometric mobility")
    safeMobilityScalarNames.foreach: name =>
      assert(scalar(mood, name) > 0, s"$name should be nonzero from opponent-control-filtered mobility")

    mobilityScalarNames
      .zip(safeMobilityScalarNames)
      .foreach: (mobilityName, safeName) =>
        assert(
          scalar(mood, safeName) <= scalar(mood, mobilityName),
          s"$safeName must not exceed $mobilityName"
        )

    assert(scalar(mood, "white_pawn_mobility") != scalar(mood, "white_knight_mobility"))
    assert(scalar(mood, "black_bishop_mobility") != scalar(mood, "black_queen_mobility"))

  test("BoardFacts white pawn mobility counts diagnostic one-step and diagonal destinations exactly"):
    val fen = Fen.Full("7k/8/8/5n2/8/2N1p3/3P3P/4K3 w - - 0 1")
    val mood = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)

    assertEquals(scalar(mood, "position_ready"), 1)
    assertEquals(
      scalar(mood, "white_pawn_mobility"),
      4,
      "single pawn steps and diagonal captures count diagnostically; diagonal non-captures and double pushes do not"
    )
    assertEquals(
      scalar(mood, "white_pawn_safe_mobility"),
      2,
      "black knight control filters e3 and g3, leaving d3 and h3 as safe diagnostic destinations"
    )
    assert(scalar(mood, "white_pawn_safe_mobility") <= scalar(mood, "white_pawn_mobility"))
    assertEquals(mood.scalars.slice(224, 256), Vector.fill(32)(0))

  test("BoardFacts black pawn mobility counts diagnostic one-step and diagonal destinations exactly"):
    val fen = Fen.Full("7k/4p2p/3n1P2/8/5N2/8/8/4K3 b - - 0 1")
    val mood = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)

    assertEquals(scalar(mood, "position_ready"), 1)
    assertEquals(
      scalar(mood, "black_pawn_mobility"),
      4,
      "single pawn steps and diagonal captures count diagnostically; diagonal non-captures and double pushes do not"
    )
    assertEquals(
      scalar(mood, "black_pawn_safe_mobility"),
      2,
      "white knight control filters e6 and g6, leaving f6 and h6 as safe diagnostic destinations"
    )
    assert(scalar(mood, "black_pawn_safe_mobility") <= scalar(mood, "black_pawn_mobility"))
    assertEquals(mood.scalars.slice(224, 256), Vector.fill(32)(0))

  test("BoardFacts pinned piece mobility and control are diagnostic non-proof semantics"):
    val fen = Fen.Full("4k3/4n3/8/8/8/8/8/K3R3 b - - 0 1")
    val facts = BoardFacts.fromFen(fen).toOption.get
    val mood = BoardMood.fromFacts(facts)
    val pinnedKnightDestinations =
      Square('c', 6).bit |
        Square('c', 8).bit |
        Square('d', 5).bit |
        Square('f', 5).bit |
        Square('g', 6).bit |
        Square('g', 8).bit

    assertEquals(scalar(mood, "position_ready"), 1)
    assert(scalar(mood, "black_knight_mobility") > 0)
    assert((facts.control.black.controlledMask & Square('c', 6).bit) != 0L)
    assertEquals(facts.sideLegal.legalDestinationUnion & pinnedKnightDestinations, 0L)
    assert(scalar(mood, "legal_move_count") > 0)

  test("BoardFacts fromFen derives deterministic same-board space diagnostics"):
    val fen = Fen.Full("4k3/8/8/8/4N3/8/8/4K3 w - - 0 1")
    val mood = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)

    assertEquals(scalar(mood, "position_ready"), 1)
    assert(scalar(mood, "white_space") > 0)
    assertEquals(scalar(mood, "black_space"), 0)
    assertEquals(
      scalar(mood, "space_diff"),
      scalar(mood, "white_space") - scalar(mood, "black_space")
    )

  test("BoardFacts fromFen keeps control diagnostics geometric and proof pressure slots zero"):
    val fen = Fen.Full("4k3/8/5N2/8/5N2/8/8/4K3 b - - 0 1")
    val facts = BoardFacts.fromFen(fen).toOption.get
    val mood = BoardMood.fromFacts(facts)

    assertEquals(scalar(mood, "position_ready"), 1)
    assertEquals(
      scalar(mood, "white_controlled_squares"),
      java.lang.Long.bitCount(facts.control.white.controlledMask)
    )
    assertEquals(
      scalar(mood, "black_controlled_squares"),
      java.lang.Long.bitCount(facts.control.black.controlledMask)
    )
    assertEquals(
      scalar(mood, "contested_squares"),
      java.lang.Long.bitCount(facts.control.white.controlledMask & facts.control.black.controlledMask)
    )
    assert(scalar(mood, "white_attacked_twice") > 0)
    assertEquals(mood.scalars.slice(224, 256), Vector.fill(32)(0))

  test("BoardMood fromFacts keeps manually assembled BoardFacts with pieces and control unready"):
    val facts = minimalBoardFacts(
      control = Control(
        known = true,
        white = ControlSide(space = 2, controlledSquares = 3, controlledMask = Square('d', 4).bit),
        black = ControlSide(space = 1, controlledSquares = 2, controlledMask = Square('e', 5).bit),
        contestedSquares = 0,
        spaceDiff = 1
      )
    )
    val mood = BoardMood.fromFacts(facts)

    assertEquals(scalar(mood, "white_space"), 2)
    assertEquals(scalar(mood, "black_space"), 1)
    assertEquals(scalar(mood, "position_ready"), 0)

  test("BoardFacts fromFen derives nonzero pawn diagnostics from the same board root"):
    val fen = Fen.Full("4k3/8/1pp5/5p2/1PPP1P1P/8/8/4K3 w - - 0 1")
    val mood = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)

    assertEquals(scalar(mood, "position_ready"), 1)
    assert(scalar(mood, "white_passed_pawn_count") > 0)
    assert(scalar(mood, "white_candidate_passer_count") > 0)
    assert(scalar(mood, "white_fixed_pawn_count") > 0)
    assert(scalar(mood, "white_lever_count") > 0)

  test("BoardFacts fromFen derives nonzero backward pawn diagnostics from the same board root"):
    val fen = Fen.Full("4k3/8/1p1p4/1P6/2P5/8/8/4K3 w - - 0 1")
    val mood = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)

    assertEquals(scalar(mood, "position_ready"), 1)
    assert(scalar(mood, "white_backward_pawn_count") > 0)

  test("BoardFacts fromFen populates imbalanced material counts with zero-baseline imbalance"):
    val fen = Fen.Full("4k3/8/8/8/8/8/8/RNBQK3 w - - 0 1")
    val facts = BoardFacts.fromFen(fen).toOption.get
    val mood = BoardMood.fromFacts(facts)

    assertEquals(scalar(mood, "position_ready"), 1)
    assertEquals(scalar(mood, "white_pawn_count"), 0)
    assertEquals(scalar(mood, "white_knight_count"), 1)
    assertEquals(scalar(mood, "white_bishop_count"), 1)
    assertEquals(scalar(mood, "white_rook_count"), 1)
    assertEquals(scalar(mood, "white_queen_count"), 1)
    assertEquals(scalar(mood, "white_king_count"), 1)
    assertEquals(scalar(mood, "black_pawn_count"), 0)
    assertEquals(scalar(mood, "black_knight_count"), 0)
    assertEquals(scalar(mood, "black_bishop_count"), 0)
    assertEquals(scalar(mood, "black_rook_count"), 0)
    assertEquals(scalar(mood, "black_queen_count"), 0)
    assertEquals(scalar(mood, "black_king_count"), 1)
    assertEquals(scalar(mood, "white_material"), 2050)
    assertEquals(scalar(mood, "black_material"), 0)
    assertEquals(scalar(mood, "material_diff"), 2050)
    assertEquals(scalar(mood, "material_imbalance"), 0)
    assertEquals(scalar(mood, "phase_total"), 8)
    assertEquals(scalar(mood, "phase_non_pawn"), 4)

  test("BoardFacts fromFen keeps hash and proof slots zero while ready"):
    val fen = Fen.Full("4k3/8/8/8/8/8/8/RNBQK3 w - - 0 1")
    val first = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)
    val second = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)

    assertEquals(scalar(first, "position_ready"), 1)
    assertEquals(scalar(first, "board_hash_lo"), 0)
    assertEquals(scalar(first, "board_hash_hi"), 0)
    assertEquals(scalar(second, "board_hash_lo"), 0)
    assertEquals(scalar(second, "board_hash_hi"), 0)
    assertEquals(first.scalars.slice(224, 256), Vector.fill(32)(0))
    assertEquals(second.scalars.slice(224, 256), Vector.fill(32)(0))

  test("BoardFacts fromFen accepts mate and stalemate as ready known zero-legal-move positions"):
    val mate = BoardFacts.fromFen(Fen.Full("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1")).toOption.get
    val stalemate = BoardFacts.fromFen(Fen.Full("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")).toOption.get

    assertEquals(mate.sideLegal.known, true)
    assertEquals(mate.sideLegal.moveCount, 0)
    assertEquals(scalar(BoardMood.fromFacts(mate), "position_ready"), 1)
    assertEquals(scalar(BoardMood.fromFacts(mate), "in_check_mask"), 2)

    assertEquals(stalemate.sideLegal.known, true)
    assertEquals(stalemate.sideLegal.moveCount, 0)
    assertEquals(scalar(BoardMood.fromFacts(stalemate), "position_ready"), 1)
    assertEquals(scalar(BoardMood.fromFacts(stalemate), "in_check_mask"), 0)

  test("BoardFacts fromFen fails closed for invalid FEN and strict castling or en-passant mismatches"):
    assert(BoardFacts.fromFen("not a fen").isLeft)
    assert(BoardFacts.fromFen(Fen.Full("8/8/8/8/8/8/8/8 w - - 0 1")).isLeft)
    assert(BoardFacts.fromFen(Fen.Full("8/8/8/8/8/8/4k3/4K3 w - - 0 1")).isLeft)
    assert(BoardFacts.fromFen(Fen.Full("7k/6Q1/6K1/8/8/8/8/8 w - - 0 1")).isLeft)
    assert(BoardFacts.fromFen(Fen.Full("8/8/8/8/8/8/8/4K3 w K - 0 1")).isLeft)
    assert(BoardFacts.fromFen(Fen.Full("8/8/8/8/8/8/4P3/4K2k w - e3 0 1")).isLeft)

  test("BoardFacts fromFen keeps root piece atoms and exact piece list in parity"):
    val facts = BoardFacts.fromFen(Fen.initial).toOption.get
    val whitePawnAtoms = facts.root
      .activeIndicesForSchema(RootAtomRegistry.SchemaId.PieceOn)
      .count: index =>
        (0 until 8).exists: file =>
          index == RootAtomRegistry.pieceOnIndex(
            chess.Color.White,
            chess.Pawn,
            chess.Square(chess.File(file).get, chess.Rank.Second)
          )
    val blackKingAtom =
      RootAtomRegistry.pieceOnIndex(
        chess.Color.Black,
        chess.King,
        chess.Square(chess.File.E, chess.Rank.Eighth)
      )

    assertEquals(facts.pieces.size, 32)
    assertEquals(facts.pieces.count(piece => piece.side == Side.White && piece.man == Man.Pawn), 8)
    assertEquals(whitePawnAtoms, 8)
    assert(facts.root.contains(blackKingAtom))

  test("BoardFacts fromFen keeps claim-shaped root schemas dark during Stage 1"):
    val pinFacts = BoardFacts.fromFen(Fen.Full("4r1k1/8/8/8/8/8/4N3/4K3 w - - 0 1")).toOption.get
    val xrayFacts = BoardFacts.fromFen(Fen.Full("7k/8/q7/2b5/8/B7/8/R6K w - - 0 1")).toOption.get
    val pawnSquareFacts =
      BoardFacts.fromFen(Fen.Full("7k/8/8/3pnp2/1P2P3/2P2N2/P7/4K3 w - - 0 1")).toOption.get
    val darkSchemas = Vector(
      RootAtomRegistry.SchemaId.WeakSquare,
      RootAtomRegistry.SchemaId.OutpostSquare,
      RootAtomRegistry.SchemaId.LoosePiece,
      RootAtomRegistry.SchemaId.PinnedPiece,
      RootAtomRegistry.SchemaId.OverloadedPiece,
      RootAtomRegistry.SchemaId.TrappedPiece,
      RootAtomRegistry.SchemaId.XrayTarget,
      RootAtomRegistry.SchemaId.KingShelterHole
    )

    Vector(pinFacts, xrayFacts, pawnSquareFacts).foreach: facts =>
      darkSchemas.foreach: schemaId =>
        assertEquals(facts.root.activeIndicesForSchema(schemaId), Vector.empty, s"$schemaId must stay dark")

  test("BoardFacts seen records legal moves with side piece square and line binding"):
    val facts = BoardFacts.fromFen(Fen.initial).toOption.get
    val seen = facts.seen
    val hasWhiteE4 =
      seen.legalMoves.contains(
        BoardFacts.LegalMove(
          side = Side.White,
          piece = Piece(Side.White, Man.Pawn, Square('e', 2)),
          line = Line(Square('e', 2), Square('e', 4))
        )
      )
    val hasBlackE5 =
      seen.legalMoves.contains(
        BoardFacts.LegalMove(
          side = Side.Black,
          piece = Piece(Side.Black, Man.Pawn, Square('e', 7)),
          line = Line(Square('e', 7), Square('e', 5))
        )
      )

    assertEquals(hasWhiteE4, true)
    assertEquals(hasBlackE5, true)
    assert(seen.failures.isEmpty)

  test("BoardFacts seen records attacked and guarded pieces as observations only"):
    val fen = Fen.Full("4k3/8/8/4n3/3P4/2N5/1P6/4K3 w - - 0 1")
    val seen = BoardFacts.fromFen(fen).toOption.get.seen
    val pawnAttacksKnight =
      seen.attacks.contains(
        BoardFacts.Attack(
          attacker = Piece(Side.White, Man.Pawn, Square('d', 4)),
          target = Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val pawnGuardsKnight =
      seen.guards.contains(
        BoardFacts.Guard(
          guard = Piece(Side.White, Man.Pawn, Square('b', 2)),
          target = Piece(Side.White, Man.Knight, Square('c', 3))
        )
      )

    assertEquals(pawnAttacksKnight, true)
    assertEquals(pawnGuardsKnight, true)

  test("BoardFacts seen consolidates piece contact without free hanging or material-win claims"):
    val fen = Fen.Full("4k3/8/8/4n3/3P4/2N5/1P6/4K3 w - - 0 1")
    val facts = BoardFacts.fromFen(fen).toOption.get
    val seen = facts.seen
    val blackKnight = Piece(Side.Black, Man.Knight, Square('e', 5))
    val whitePawn = Piece(Side.White, Man.Pawn, Square('d', 4))
    val whiteKnight = Piece(Side.White, Man.Knight, Square('c', 3))
    val whiteGuard = Piece(Side.White, Man.Pawn, Square('b', 2))

    assertEquals(
      seen.pieceContacts.filter(row => row.attacked || row.guarded || row.unguardedNonPawnNonKing),
      Vector(
        BoardFacts.PieceContact(
          piece = whiteKnight,
          attackers = Vector.empty,
          guards = Vector(whiteGuard)
        ),
        BoardFacts.PieceContact(
          piece = blackKnight,
          attackers = Vector(whitePawn),
          guards = Vector.empty
        )
      )
    )
    val blackKnightContact = seen.pieceContacts.find(_.piece == blackKnight).get
    assertEquals(blackKnightContact.attacked, true)
    assertEquals(blackKnightContact.guarded, false)
    assertEquals(blackKnightContact.attackedUnguarded, true)
    assertEquals(blackKnightContact.unguardedNonPawnNonKing, true)

    val mood = BoardMood.fromFacts(facts)
    assertEquals(scalar(mood, "white_loose_piece_count"), 0)
    assertEquals(scalar(mood, "black_loose_piece_count"), 0)
    assertEquals(scalar(mood, "white_hanging_piece_count"), 0)
    assertEquals(scalar(mood, "black_hanging_piece_count"), 0)
    assertEquals(scalar(mood, "exact_board_binding"), 0)
    assertEquals(scalar(mood, "legal_replay_binding"), 0)
    assertEquals(scalar(mood, "public_claim_pressure"), 0)

  test("BoardFacts seen consolidates pin lines without turning pins into public claims"):
    val fen = Fen.Full("4r1k1/8/8/8/8/8/4N3/4K3 w - - 0 1")
    val seen = BoardFacts.fromFen(fen).toOption.get.seen

    assert(
      seen.lineFacts.contains(
        BoardFacts.LineFact(
          kind = BoardFacts.LineKind.File,
          line = Line(Square('e', 8), Square('e', 1)),
          side = Some(Side.White),
          from = Some(Piece(Side.Black, Man.Rook, Square('e', 8))),
          to = Some(Piece(Side.White, Man.King, Square('e', 1))),
          blockers = Vector(Piece(Side.White, Man.Knight, Square('e', 2))),
          screen = None,
          target = None,
          king = Some(Piece(Side.White, Man.King, Square('e', 1))),
          pinned = Some(Piece(Side.White, Man.Knight, Square('e', 2))),
          attacker = Some(Piece(Side.Black, Man.Rook, Square('e', 8))),
          nearKingBlockers = Vector.empty,
          shapes = Set(BoardFacts.LineShape.PinToKing)
        )
      )
    )

    val mood = BoardMood.fromFacts(BoardFacts.fromFen(fen).toOption.get)
    assertEquals(scalar(mood, "exact_board_binding"), 0)
    assertEquals(scalar(mood, "legal_replay_binding"), 0)
    assertEquals(scalar(mood, "public_claim_pressure"), 0)

  test("BoardFacts seen consolidates line geometry without x-ray tactic or forced-tactic claims"):
    val fen = Fen.Full("7k/8/q7/2b5/8/B7/8/R6K w - - 0 1")
    val facts = BoardFacts.fromFen(fen).toOption.get
    val seen = facts.seen
    val whiteRook = Piece(Side.White, Man.Rook, Square('a', 1))
    val whiteKing = Piece(Side.White, Man.King, Square('h', 1))
    val whiteBishop = Piece(Side.White, Man.Bishop, Square('a', 3))
    val blackBishop = Piece(Side.Black, Man.Bishop, Square('c', 5))
    val blackQueen = Piece(Side.Black, Man.Queen, Square('a', 6))

    assert(
      seen.lineFacts.contains(
        BoardFacts.LineFact(
          kind = BoardFacts.LineKind.File,
          line = Line(Square('a', 1), Square('a', 3)),
          from = Some(whiteRook),
          to = Some(whiteBishop),
          shapes = Set(BoardFacts.LineShape.PieceLine)
        )
      )
    )
    assert(
      seen.lineFacts.contains(
        BoardFacts.LineFact(
          kind = BoardFacts.LineKind.Rank,
          line = Line(Square('a', 1), Square('h', 1)),
          from = Some(whiteRook),
          to = Some(whiteKing),
          shapes = Set(BoardFacts.LineShape.PieceLine)
        )
      )
    )
    assert(
      seen.lineFacts.contains(
        BoardFacts.LineFact(
          kind = BoardFacts.LineKind.Diagonal,
          line = Line(Square('a', 3), Square('c', 5)),
          from = Some(whiteBishop),
          to = Some(blackBishop),
          shapes = Set(BoardFacts.LineShape.PieceLine)
        )
      )
    )
    assert(
      seen.lineFacts.contains(
        BoardFacts.LineFact(
          kind = BoardFacts.LineKind.File,
          line = Line(Square('a', 1), Square('a', 8)),
          side = Some(Side.White),
          from = Some(whiteRook),
          blockers = Vector(whiteBishop, blackQueen),
          screen = Some(whiteBishop),
          target = Some(blackQueen),
          shapes = Set(BoardFacts.LineShape.Ray, BoardFacts.LineShape.Blocker, BoardFacts.LineShape.XRay)
        )
      )
    )
    assert(
      seen.lineFacts.contains(
        BoardFacts.LineFact(
          kind = BoardFacts.LineKind.File,
          line = Line(Square('a', 1), Square('a', 3)),
          side = Some(Side.White),
          from = Some(whiteRook),
          to = Some(whiteBishop),
          blockers = Vector(whiteBishop),
          shapes = Set(BoardFacts.LineShape.Blocker)
        )
      )
    )
    assertEquals(
      seen.lineFacts.exists(row =>
        row.shapes.contains(BoardFacts.LineShape.Blocker) && row.from.contains(
          whiteRook
        ) && row.blockers == Vector(blackQueen)
      ),
      false
    )

    val mood = BoardMood.fromFacts(facts)
    assertEquals(scalar(mood, "ray_count"), 0)
    assertEquals(scalar(mood, "line_proof_count"), 0)
    assertEquals(scalar(mood, "white_pin_motif_count"), 0)
    assertEquals(scalar(mood, "white_skewer_motif_count"), 0)
    assertEquals(scalar(mood, "white_xray_discovery_count"), 0)
    assertEquals(scalar(mood, "public_claim_pressure"), 0)

  test("BoardFacts seen consolidates file facts without file-control or invasion claims"):
    val fen = Fen.Full("4k3/8/3p4/8/8/8/R7/4K3 w - - 0 1")
    val facts = BoardFacts.fromFen(fen).toOption.get
    val seen = facts.seen
    val whiteRook = Piece(Side.White, Man.Rook, Square('a', 2))
    val blackPawn = Piece(Side.Black, Man.Pawn, Square('d', 6))

    val openCFile = seen.fileFacts.find(_.file == 2).get
    val semiOpenDFile = seen.fileFacts.find(_.file == 3).get
    assertEquals(openCFile.state, BoardFacts.FileState.Open)
    assertEquals(semiOpenDFile.state, BoardFacts.FileState.SemiOpen)
    assertEquals(semiOpenDFile.semiOpenFor, Vector(Side.White))
    assert(seen.fileFacts.exists(row => row.file == 0 && row.rooks.contains(whiteRook)))
    assert(
      semiOpenDFile.legalEntryMoves.contains(
        BoardFacts.LegalMove(
          side = Side.White,
          piece = whiteRook,
          line = Line(Square('a', 2), Square('d', 2))
        )
      )
    )
    assert(
      openCFile.rookOpenFileEntries.contains(
        BoardFacts.LegalMove(
          side = Side.White,
          piece = whiteRook,
          line = Line(Square('a', 2), Square('c', 2))
        )
      )
    )
    assert(semiOpenDFile.blockers.contains(blackPawn))
    assert(
      semiOpenDFile.targetSquares.contains((Side.White, Square('d', 2), Line(Square('a', 2), Square('d', 2))))
    )

    val mood = BoardMood.fromFacts(facts)
    assertEquals(scalar(mood, "white_open_file_exposure"), 0)
    assertEquals(scalar(mood, "black_open_file_exposure"), 0)
    assertEquals(scalar(mood, "white_rook_open_file_count"), 0)
    assertEquals(scalar(mood, "black_rook_open_file_count"), 0)
    assertEquals(scalar(mood, "plan_open_file"), 0)
    assertEquals(scalar(mood, "route_binding"), 0)
    assertEquals(scalar(mood, "line_proof_count"), 0)
    assertEquals(scalar(mood, "public_claim_pressure"), 0)

  test("BoardFacts seen records pawn and square facts without outpost or structure claims"):
    val fen = Fen.Full("7k/8/8/3pnp2/1P2P3/2P2N2/P7/4K3 w - - 0 1")
    val facts = BoardFacts.fromFen(fen).toOption.get
    val seen = facts.seen
    val whiteA2 = Piece(Side.White, Man.Pawn, Square('a', 2))
    val whiteC3 = Piece(Side.White, Man.Pawn, Square('c', 3))
    val whiteE4 = Piece(Side.White, Man.Pawn, Square('e', 4))
    val whiteKnight = Piece(Side.White, Man.Knight, Square('f', 3))
    val blackD5 = Piece(Side.Black, Man.Pawn, Square('d', 5))
    val blackKnight = Piece(Side.Black, Man.Knight, Square('e', 5))

    assert(
      seen.pawnChallenges.contains(
        BoardFacts.PawnChallenge(
          side = Side.White,
          pawn = whiteE4,
          square = Square('d', 5),
          line = Line(Square('e', 4), Square('d', 5))
        )
      )
    )
    assert(
      seen.pawnCannotChallengeSquares.contains(
        BoardFacts.PawnCannotChallengeSquare(
          side = Side.White,
          square = Square('f', 3),
          by = Side.Black
        )
      )
    )
    assert(
      seen.pawnSafeSquareObservations.contains(
        BoardFacts.PawnSafeSquareObservation(
          side = Side.White,
          square = Square('f', 3),
          by = Side.Black
        )
      )
    )
    assert(
      seen.noCurrentPawnChases.contains(
        BoardFacts.NoCurrentPawnChase(
          side = Side.White,
          square = Square('f', 3),
          by = Side.Black
        )
      )
    )
    assert(
      seen.frontBlockers.contains(
        BoardFacts.FrontBlocker(
          side = Side.White,
          pawn = whiteE4,
          blocker = blackKnight,
          square = Square('e', 5),
          line = Line(Square('e', 4), Square('e', 5))
        )
      )
    )
    assert(seen.passedPawnObservations.contains(BoardFacts.PassedPawnObservation(Side.White, whiteA2)))
    assert(seen.isolatedPawnObservations.contains(BoardFacts.IsolatedPawnObservation(Side.White, whiteE4)))
    assert(
      seen.backwardPawnFrontSquares.contains(
        BoardFacts.BackwardPawnFrontSquare(
          side = Side.White,
          pawn = whiteC3,
          square = Square('c', 4),
          line = Line(Square('c', 3), Square('c', 4))
        )
      )
    )
    assert(
      seen.pieceReachableSquares.contains(
        BoardFacts.PieceReachableSquare(
          side = Side.White,
          piece = whiteKnight,
          square = Square('g', 5),
          line = Line(Square('f', 3), Square('g', 5))
        )
      )
    )
    assert(
      seen.squareGuardMaps.contains(
        BoardFacts.SquareGuardMap(
          side = Side.White,
          square = Square('d', 5),
          guards = Vector(whiteE4)
        )
      )
    )
    assert(
      seen.pawnLevers.contains(
        BoardFacts.PawnLever(Side.White, whiteE4, blackD5, Line(Square('e', 4), Square('d', 5)))
      )
    )

    val mood = BoardMood.fromFacts(facts)
    assertEquals(scalar(mood, "white_outpost_count"), 0)
    assertEquals(scalar(mood, "black_outpost_count"), 0)
    assertEquals(scalar(mood, "plan_outpost"), 0)
    assertEquals(scalar(mood, "plan_weak_square"), 0)
    assertEquals(scalar(mood, "plan_minority"), 0)
    assertEquals(scalar(mood, "plan_center_break"), 0)
    assertEquals(scalar(mood, "plan_flank_break"), 0)
    assertEquals(scalar(mood, "route_binding"), 0)
    assertEquals(scalar(mood, "line_proof_count"), 0)
    assertEquals(scalar(mood, "public_claim_pressure"), 0)

  test("BoardFacts seen records king ring facts without unsafe king mate or no-escape claims"):
    val fen = Fen.Full("4r1k1/6br/8/8/8/8/4N1R1/4K3 b - - 0 1")
    val facts = BoardFacts.fromFen(fen).toOption.get
    val seen = facts.seen
    val whiteKing = Piece(Side.White, Man.King, Square('e', 1))
    val blackKing = Piece(Side.Black, Man.King, Square('g', 8))
    val whiteKnight = Piece(Side.White, Man.Knight, Square('e', 2))
    val whiteRook = Piece(Side.White, Man.Rook, Square('g', 2))
    val blackRookE8 = Piece(Side.Black, Man.Rook, Square('e', 8))
    val blackBishopG7 = Piece(Side.Black, Man.Bishop, Square('g', 7))
    val blackRookH7 = Piece(Side.Black, Man.Rook, Square('h', 7))

    assert(seen.kingSquares.contains(BoardFacts.KingSquare(Side.White, whiteKing)))
    assert(
      seen.kingRingSquares.contains(
        BoardFacts.KingRingSquare(
          side = Side.Black,
          king = blackKing,
          square = Square('g', 7)
        )
      )
    )
    assert(
      seen.kingRingAttacks.contains(
        BoardFacts.KingRingAttack(
          side = Side.Black,
          king = blackKing,
          square = Square('g', 7),
          attacker = whiteRook
        )
      )
    )
    assert(
      seen.kingRingDefenders.contains(
        BoardFacts.KingRingDefender(
          side = Side.Black,
          king = blackKing,
          square = Square('g', 7),
          defender = blackRookH7
        )
      )
    )
    assert(
      seen.legalEscapeSquares.contains(
        BoardFacts.LegalEscapeSquare(
          side = Side.Black,
          king = blackKing,
          square = Square('h', 8),
          line = Line(Square('g', 8), Square('h', 8))
        )
      )
    )
    assert(
      seen.contactCheckObservations.contains(
        BoardFacts.ContactCheckObservation(
          side = Side.Black,
          king = blackKing,
          attacker = Piece(Side.White, Man.Rook, Square('g', 7)),
          line = Line(Square('g', 2), Square('g', 7))
        )
      )
    )
    assert(
      seen.lineFacts.contains(
        BoardFacts.LineFact(
          kind = BoardFacts.LineKind.File,
          line = Line(Square('e', 8), Square('e', 1)),
          side = Some(Side.White),
          from = Some(blackRookE8),
          to = Some(whiteKing),
          blockers = Vector(whiteKnight),
          king = Some(whiteKing),
          nearKingBlockers = Vector(whiteKnight),
          shapes = Set(BoardFacts.LineShape.KingLine, BoardFacts.LineShape.BlockerNearKing)
        )
      )
    )
    assert(
      seen.lineFacts.contains(
        BoardFacts.LineFact(
          kind = BoardFacts.LineKind.File,
          line = Line(Square('g', 2), Square('g', 8)),
          side = Some(Side.Black),
          from = Some(whiteRook),
          to = Some(blackKing),
          blockers = Vector(blackBishopG7),
          king = Some(blackKing),
          nearKingBlockers = Vector(blackBishopG7),
          shapes = Set(BoardFacts.LineShape.KingLine, BoardFacts.LineShape.BlockerNearKing)
        )
      )
    )

    val mood = BoardMood.fromFacts(facts)
    assertEquals(scalar(mood, "white_king_square"), 0)
    assertEquals(scalar(mood, "black_king_square"), 0)
    assertEquals(scalar(mood, "white_king_ring_enemy_attacks"), 0)
    assertEquals(scalar(mood, "black_king_ring_enemy_attacks"), 0)
    assertEquals(scalar(mood, "white_contact_check_threats"), 0)
    assertEquals(scalar(mood, "black_contact_check_threats"), 0)
    assertEquals(scalar(mood, "white_mate_net_pressure"), 0)
    assertEquals(scalar(mood, "black_mate_net_pressure"), 0)
    assertEquals(scalar(mood, "white_king_heat"), 0)
    assertEquals(scalar(mood, "black_king_heat"), 0)
    assertEquals(scalar(mood, "line_proof_count"), 0)
    assertEquals(scalar(mood, "public_claim_pressure"), 0)

  test("BoardFacts seen records pawn levers open files rook entries and king ring attacks"):
    val levers = BoardFacts.fromFen(Fen.Full("7k/8/8/3p1p2/4P3/8/8/4K3 w - - 0 1")).toOption.get.seen
    val whitePawnLever =
      levers.pawnLevers.contains(
        BoardFacts.PawnLever(
          side = Side.White,
          pawn = Piece(Side.White, Man.Pawn, Square('e', 4)),
          target = Piece(Side.Black, Man.Pawn, Square('d', 5)),
          line = Line(Square('e', 4), Square('d', 5))
        )
      )
    assertEquals(whitePawnLever, true)

    val openFile = BoardFacts.fromFen(Fen.Full("4k3/8/8/8/8/8/R7/4K3 w - - 0 1")).toOption.get.seen
    val whiteRookEntry =
      openFile.fileFacts
        .find(_.file == 2)
        .exists:
          _.rookOpenFileEntries.contains(
            BoardFacts.LegalMove(
              side = Side.White,
              piece = Piece(Side.White, Man.Rook, Square('a', 2)),
              line = Line(Square('a', 2), Square('c', 2))
            )
          )
    assertEquals(whiteRookEntry, true)

    val kingRing = BoardFacts.fromFen(Fen.Full("6k1/8/8/8/8/8/6R1/4K3 b - - 0 1")).toOption.get.seen
    val blackKingRingG7 =
      kingRing.kingRingAttacks.contains(
        BoardFacts.KingRingAttack(
          side = Side.Black,
          king = Piece(Side.Black, Man.King, Square('g', 8)),
          square = Square('g', 7),
          attacker = Piece(Side.White, Man.Rook, Square('g', 2))
        )
      )
    assertEquals(blackKingRingG7, true)

  test("BoardFacts seen keeps untrusted or incomplete board facts silent with missing evidence logs"):
    val seen = minimalBoardFacts().seen

    assertEquals(seen.legalMoves, Vector.empty)
    assertEquals(seen.attacks, Vector.empty)
    assertEquals(seen.guards, Vector.empty)
    assertEquals(seen.pieceContacts, Vector.empty)
    assertEquals(seen.lineFacts, Vector.empty)
    assertEquals(seen.pawnChallenges, Vector.empty)
    assertEquals(seen.pawnCannotChallengeSquares, Vector.empty)
    assertEquals(seen.pawnSafeSquareObservations, Vector.empty)
    assertEquals(seen.noCurrentPawnChases, Vector.empty)
    assertEquals(seen.frontBlockers, Vector.empty)
    assertEquals(seen.passedPawnObservations, Vector.empty)
    assertEquals(seen.isolatedPawnObservations, Vector.empty)
    assertEquals(seen.backwardPawnFrontSquares, Vector.empty)
    assertEquals(seen.pieceReachableSquares, Vector.empty)
    assertEquals(seen.squareGuardMaps, Vector.empty)
    assertEquals(seen.fileFacts, Vector.empty)
    assertEquals(seen.kingSquares, Vector.empty)
    assertEquals(seen.kingRingSquares, Vector.empty)
    assertEquals(seen.kingRingAttacks, Vector.empty)
    assertEquals(seen.kingRingDefenders, Vector.empty)
    assertEquals(seen.legalEscapeSquares, Vector.empty)
    assertEquals(seen.contactCheckObservations, Vector.empty)
    val missingProducer = seen.failures.exists(_.missing.contains("same-board producer proof"))
    val missingPieces = seen.failures.exists(_.missing.contains("piece list"))
    assertEquals(missingProducer, true)
    assertEquals(missingPieces, true)

  test("BoardFacts fromPosition is an internal same-board diagnostic boundary"):
    val position = Fen.read(variant.Standard, Fen.initial).get
    val facts = BoardFacts.fromPosition(position, fullmoveNumber = 1).toOption.get
    val mood = BoardMood.fromFacts(facts)

    assertEquals(scalar(mood, "position_ready"), 1)
    assertEquals(facts.sideLegal.moveCount, 20)
    assertEquals(facts.rivalLegal.moveCount, 20)
    assertEquals(
      mood.bits(BoardMood.SideLegalDestinationsBit),
      facts.sideLegal.legalDestinationUnion
    )
    assertEquals(
      mood.bits(BoardMood.RivalLegalDestinationsBit),
      facts.rivalLegal.legalDestinationUnion
    )
    assertEquals(
      positionReady(
        minimalBoardFacts(
          sideLegal = Moves(known = true, moveCount = 0, lines = Vector(Line(Square('b', 2), Square('b', 3))))
        )
      ),
      0
    )
    assertEquals(
      positionReady(
        minimalBoardFacts(rivalLegal =
          Moves(known = true, moveCount = 0, destinationUnion = Square('g', 6).bit)
        )
      ),
      0
    )
    assertEquals(
      positionReady(
        minimalBoardFacts(
          rivalLegal =
            Moves(known = true, moveCount = 0, lines = Vector(Line(Square('g', 7), Square('g', 6))))
        )
      ),
      0
    )
    assertEquals(positionReady(minimalBoardFacts(sideLegal = readyMoves(moveCount = 1, captureCount = 2))), 0)
    assertEquals(positionReady(minimalBoardFacts(sideLegal = readyMoves(moveCount = 1, checkCount = 2))), 0)
    assertEquals(
      positionReady(minimalBoardFacts(rivalLegal = readyMoves(moveCount = 1, captureCount = 2))),
      0
    )
    assertEquals(positionReady(minimalBoardFacts(rivalLegal = readyMoves(moveCount = 1, checkCount = 2))), 0)

  test("BoardFacts fromPosition uses an explicit internal fullmove source"):
    val fen = Fen.Full("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 7 9")
    val position = Fen.read(variant.Standard, fen).get
    val mood = BoardMood.fromFacts(BoardFacts.fromPosition(position, fullmoveNumber = 9).toOption.get)

    assertEquals(scalar(mood, "side_to_move"), Side.Black.ordinal)
    assertEquals(scalar(mood, "fullmove_number"), 9)
    assertEquals(scalar(mood, "ply_from_start"), 17)
    assertEquals(scalar(mood, "snapshot_ply"), 17)
    assert(BoardFacts.fromPosition(position, fullmoveNumber = 0).isLeft)

  test("BoardMood legal destination bits ignore control masks"):
    val facts = minimalBoardFacts(
      sideLegal = Moves(
        lines = Vector(Line(Square('a', 2), Square('a', 4))),
        destinationUnion = Square('b', 4).bit,
        moveCount = 2
      ),
      rivalLegal = Moves(
        lines = Vector(Line(Square('h', 7), Square('h', 5))),
        destinationUnion = Square('g', 5).bit,
        moveCount = 2
      ),
      control = Control(
        white = ControlSide(controlledMask = Square('c', 6).bit),
        black = ControlSide(attackedSquares = Square('d', 6).bit)
      )
    )

    val mood = BoardMood.fromFacts(facts)

    assertEquals(mood.bits(BoardMood.SideLegalDestinationsBit), Square('a', 4).bit | Square('b', 4).bit)
    assertEquals(mood.bits(BoardMood.RivalLegalDestinationsBit), Square('h', 5).bit | Square('g', 5).bit)
    assertEquals(mood.bits(BoardMood.SideLegalDestinationsBit) & Square('c', 6).bit, 0L)
    assertEquals(mood.bits(BoardMood.RivalLegalDestinationsBit) & Square('d', 6).bit, 0L)

  test("BoardFacts constructor fields have no shortcut authority defaults"):
    val companionMethods = BoardFacts.getClass.getMethods.map(_.getName).toSet
    val instanceMethods = classOf[BoardFacts].getMethods.map(_.getName).toSet
    (1 to 9).foreach: index =>
      assert(
        !companionMethods.contains(s"$$lessinit$$greater$$default$$$index"),
        s"BoardFacts field $index must be explicit"
      )
    assert(classOf[BoardFacts].getDeclaredConstructors.forall(_.getParameterCount == 9))
    assert(!companionMethods.contains("fromProduct"), "BoardFacts must not expose product reconstruction")
    assert(!instanceMethods.contains("copy"), "BoardFacts must not expose case-class copy authority")
    assert(
      companionMethods.contains("untrusted"),
      "manual BoardFacts assembly must go through the untrusted fail-closed helper"
    )

  test("BoardFacts reflective construction cannot forge same-board readiness"):
    val constructor = classOf[BoardFacts].getDeclaredConstructors.maxBy(_.getParameterCount)
    constructor.setAccessible(true)
    val params =
      Vector(
        RootStateVector.fromIndices(Vector(0)),
        Side.White,
        readyHeader,
        readyMoves(),
        readyMoves(line = safeRivalRoute),
        readyControl,
        readyMaterial,
        readyPawns,
        Vector.empty[Piece]
      )
    assertEquals(constructor.getParameterCount, params.size)

    val forged = constructor.newInstance(params.map(_.asInstanceOf[AnyRef])*).asInstanceOf[BoardFacts]

    assertEquals(positionReady(forged), 0)
    assertEquals(positionReady(BoardFacts.fromFen(Fen.initial).toOption.get), 1)

  test("BoardMood fromParts and fromPacked reject wrong sizes"):
    val rootWords = Vector.fill(BoardMood.RootWordBits)(0L)
    val scalars = Vector.fill(BoardMood.Scalars)(0)
    assertEquals(
      BoardMood.fromParts(rootWords = Some(rootWords), scalars = Some(scalars)).bits.size,
      BoardMood.Bits
    )

    intercept[IllegalArgumentException]:
      BoardMood.fromParts(rootWords = Some(rootWords.dropRight(1)))
    intercept[IllegalArgumentException]:
      BoardMood.fromParts(scalars = Some(scalars.dropRight(1)))
    intercept[IllegalArgumentException]:
      BoardMood.fromPacked(Vector.fill(BoardMood.Bits - 1)(0L), scalars)
    intercept[IllegalArgumentException]:
      BoardMood.fromPacked(Vector.fill(BoardMood.Bits)(0L), scalars :+ 1)

  test("BoardMood low-level scalar constructors canonicalize closed slots to zero"):
    val bits = Vector.fill(BoardMood.Bits)(0L)
    val scalars = Vector.tabulate(BoardMood.Scalars)(index => index + 1)
    val root = RootStateVector.fromIndices(Vector(0))

    val packed = BoardMood.fromPacked(bits, scalars)
    val parts = BoardMood.fromParts(scalars = Some(scalars))
    val rooted = BoardMood.fromRoot(root, scalars = Some(scalars))

    Vector(packed, parts, rooted).foreach: mood =>
      assertEquals(mood.scalars, canonicalScalars(scalars))
      Vector(
        "white_open_file_exposure",
        "black_open_file_exposure",
        "white_rook_open_file_count",
        "black_rook_open_file_count",
        "white_loose_piece_count",
        "black_loose_piece_count"
      ).foreach: name =>
        assertEquals(mood.scalars(BoardMood.ScalarsByName(name)), 0)

  test("BoardMood exposes no case-class copy or product reconstruction shortcuts"):
    val companionMethods = BoardMood.getClass.getMethods.map(_.getName).toSet
    val instanceMethods = classOf[BoardMood].getMethods.map(_.getName).toSet

    assert(!companionMethods.contains("fromProduct"), "BoardMood must not expose product reconstruction")
    assert(!instanceMethods.contains("copy"), "BoardMood must not expose case-class copy authority")
    assert(!classOf[Product].isAssignableFrom(classOf[BoardMood]), "BoardMood must not be a Product")

  test("BoardMood raw constructor path canonicalizes closed slots to zero"):
    val bits = Vector.fill(BoardMood.Bits)(0L)
    val scalars = Vector.tabulate(BoardMood.Scalars)(index => index + 1)
    val constructor = classOf[BoardMood].getDeclaredConstructors.find(_.getParameterCount == 2).get
    constructor.setAccessible(true)

    val mood = constructor.newInstance(bits, scalars).asInstanceOf[BoardMood]

    assertEquals(mood.scalars, canonicalScalars(scalars))

  test("BoardMood rejects non-root padding bits in B45"):
    val scalars = Vector.fill(BoardMood.Scalars)(0)
    val badRootWords = Vector.fill(BoardMood.RootWordBits - 1)(0L) :+ (1L << 11)
    val badBits = badRootWords ++ Vector(0L, 0L)

    intercept[IllegalArgumentException]:
      BoardMood.fromPacked(badBits, scalars)
    intercept[IllegalArgumentException]:
      BoardMood.fromPacked(badBits, scalars)
    intercept[IllegalArgumentException]:
      BoardMood.fromParts(rootWords = Some(badRootWords), scalars = Some(scalars))

  test("BoardMood fromPieces is a local scaffold over the root-word contract"):
    val d4 = Square('d', 4)
    val pieces = Vector(
      Piece(Side.White, Man.King, Square('g', 1)),
      Piece(Side.White, Man.Queen, Square('a', 1)),
      Piece(Side.White, Man.Rook, d4),
      Piece(Side.White, Man.Bishop, Square('c', 1)),
      Piece(Side.White, Man.Knight, Square('f', 3)),
      Piece(Side.White, Man.Pawn, Square('e', 4)),
      Piece(Side.Black, Man.King, Square('g', 8)),
      Piece(Side.Black, Man.Queen, Square('a', 8)),
      Piece(Side.Black, Man.Rook, Square('d', 8)),
      Piece(Side.Black, Man.Bishop, Square('h', 6)),
      Piece(Side.Black, Man.Knight, Square('f', 4)),
      Piece(Side.Black, Man.Pawn, Square('d', 6))
    )

    val mood = BoardMood.fromPieces(pieces, side = Side.White)

    assertEquals(Man.values.toVector, Vector(Man.Pawn, Man.Knight, Man.Bishop, Man.Rook, Man.Queen, Man.King))
    assertEquals(Square.fromIndex(d4.index), d4)
    intercept[IllegalArgumentException]:
      Square.fromIndex(64)
    assert(mood.bits.take(BoardMood.RootWordBits).exists(_ != 0L))
    assertEquals(mood.bits(BoardMood.SideLegalDestinationsBit), 0L)
    assertEquals(mood.bits(BoardMood.RivalLegalDestinationsBit), 0L)
    assertEquals(mood.scalars(BoardMood.ScalarsByName("side_to_move")), Side.White.ordinal)
    assert(mood.scalars(BoardMood.ScalarsByName("phase_total")) > 0)
    assertEquals(mood.scalars(BoardMood.ScalarsByName("white_material")), 2150)
    assertEquals(mood.scalars(BoardMood.ScalarsByName("black_material")), 2150)
    assertEquals(mood.scalars(BoardMood.ScalarsByName("position_ready")), 0)

  test("Scene, Plan, and Tactic keep the contract order"):
    assertEquals(
      Scene.values.toVector,
      Vector(
        Scene.Tactic,
        Scene.Blunder,
        Scene.Material,
        Scene.King,
        Scene.Defense,
        Scene.Opening,
        Scene.Pawns,
        Scene.PawnAdvance,
        Scene.PawnStop,
        Scene.Plan,
        Scene.Pieces,
        Scene.Space,
        Scene.Initiative,
        Scene.Convert,
        Scene.Endgame,
        Scene.Counterplay,
        Scene.Source,
        Scene.Quiet,
        Scene.PromotionThreat
      )
    )
    assertEquals(
      Plan.values.toVector,
      Vector(
        Plan.Minority,
        Plan.Majority,
        Plan.CenterBreak,
        Plan.FlankBreak,
        Plan.Storm,
        Plan.Expansion,
        Plan.Cramp,
        Plan.Outpost,
        Plan.BadPiece,
        Plan.Reroute,
        Plan.Bishops,
        Plan.Blockade,
        Plan.OpenFile,
        Plan.Seventh,
        Plan.ColorBind,
        Plan.WeakSquare,
        Plan.Isolani,
        Plan.BackwardPawn,
        Plan.HangingPawns,
        Plan.ChainBase,
        Plan.PasserMake,
        Plan.PasserBlock,
        Plan.Race,
        Plan.Trade,
        Plan.Simplify,
        Plan.KeepPieces,
        Plan.Overload,
        Plan.Prophy,
        Plan.Counterplay,
        Plan.Initiative,
        Plan.KingConvert,
        Plan.Convert
      )
    )
    assertEquals(
      Tactic.values.toVector,
      Vector(
        Tactic.Loose,
        Tactic.Hanging,
        Tactic.AbsPin,
        Tactic.RelPin,
        Tactic.Pin,
        Tactic.Skewer,
        Tactic.Xray,
        Tactic.Fork,
        Tactic.DiscoveredAttack,
        Tactic.RemoveGuard,
        Tactic.Overload,
        Tactic.BackRank,
        Tactic.MateNet,
        Tactic.SafeCheck,
        Tactic.PawnFork,
        Tactic.PawnPush,
        Tactic.Trap,
        Tactic.QueenHit,
        Tactic.KingOpen,
        Tactic.Promote,
        Tactic.InBetween,
        Tactic.Clear,
        Tactic.Decoy,
        Tactic.Deflect,
        Tactic.Tempo
      )
    )

  test("Proof validates scores and computes the public strength formulas"):
    val strong = proof(
      boardProof = 75,
      ownerProof = 85,
      anchorProof = 90,
      routeProof = 80,
      forcing = 90,
      conversionPrize = 70,
      kingHeat = 80,
      lineProof = 66,
      immediacy = 60
    )

    assertEquals(strong.truth, 66)
    assertEqualsDouble(strong.tacticHeat, 76.4, 0.0001)
    assertEqualsDouble(strong.planHeat, 74.0, 0.0001)
    assertEqualsDouble(strong.publicStrength, 66.0, 0.0001)

    intercept[IllegalArgumentException]:
      proof(boardProof = -1)
    intercept[IllegalArgumentException]:
      proof(counterplayRisk = 101)

  test("Proof constructor uses contract field names"):
    val exact = Proof(
      boardProof = 70,
      lineProof = 71,
      ownerProof = 72,
      anchorProof = 73,
      routeProof = 74,
      persistence = 75,
      immediacy = 76,
      forcing = 77,
      conversionPrize = 78,
      counterplayRisk = 20,
      kingHeat = 79,
      pieceSupport = 80,
      pawnSupport = 81,
      sourceFit = 82,
      novelty = 83,
      clarity = 84
    )

    assertEquals(exact.values.size, Proof.Size)
    assertEquals(exact.values(Proof.Slots.BoardProof), 70)
    assertEquals(exact.values(Proof.Slots.LineProof), 71)
    assertEquals(exact.values(Proof.Slots.OwnerProof), 72)
    assertEquals(exact.values(Proof.Slots.RouteProof), 74)
    assertEquals(exact.values(Proof.Slots.ConversionPrize), 78)
    assertEquals(exact.values(Proof.Slots.CounterplayRisk), 20)
    assertEquals(exact.values(Proof.Slots.KingHeat), 79)
    assertEquals(exact.values(Proof.Slots.PieceSupport), 80)
    assertEquals(exact.values(Proof.Slots.PawnSupport), 81)
    assertEquals(exact.values(Proof.Slots.SourceFit), 82)

  test("Story constants preserve the fixed story shape"):
    assertEquals(Story.Size, 164)
    assertEquals(Story.SceneSlots, 19)
    assertEquals(Story.PlanSlots, 32)
    assertEquals(Story.TacticSlots, 25)
    assertEquals(Story.PawnSlots, 16)
    assertEquals(Story.PieceSlots, 16)
    assertEquals(Story.KingSlots, 16)
    assertEquals(Story.OpeningSlots, 8)
    assertEquals(Story.ProofSlots, 32)
    assertEquals(Story.Slots.Scene, 0)
    assertEquals(Story.Slots.Plan, 19)
    assertEquals(Story.Slots.Tactic, 51)
    assertEquals(Story.Slots.Pawn, 76)
    assertEquals(Story.Slots.Piece, 92)
    assertEquals(Story.Slots.King, 108)
    assertEquals(Story.Slots.Opening, 124)
    assertEquals(Story.Slots.Proof, 132)
    assertEquals(Story.Slots.End, Story.Size)

  test("Story values encode exact shape, public family, identity, and proof"):
    val e5 = Square('e', 5)
    val d4 = Square('d', 4)
    val route = Line(Square('c', 4), e5)
    val story = Story(
      Scene.Plan,
      side = Side.White,
      target = Some(e5),
      anchor = Some(d4),
      route = Some(route),
      rival = Side.Black,
      plan = Some(Plan.CenterBreak),
      proof = proof(boardProof = 91, lineProof = 62, routeProof = 88, conversionPrize = 84)
    )
    val values = story.values

    assertEquals(values.size, Story.Size)
    assertEquals(values(Story.Slots.Scene + Scene.Plan.ordinal), 1)
    assertEquals(values(Story.Slots.Plan + Plan.CenterBreak.ordinal), 1)
    assertEquals(values(Story.Slots.Tactic + Tactic.Fork.ordinal), 0)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.Side), Side.White.ordinal)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.Rival), Side.Black.ordinal)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.Target), e5.index + 1)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.Anchor), d4.index + 1)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.RouteFrom), route.from.index + 1)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.RouteTo), route.to.index + 1)
    assertEquals(values(Story.Slots.Proof + Proof.Slots.BoardProof), 91)
    assertEquals(values(Story.Slots.Proof + Proof.Slots.LineProof), 62)
    assertEquals(values(Story.Slots.Proof + Proof.Slots.RouteProof), 88)
    assertEquals(values(Story.Slots.Proof + Proof.Slots.ConversionPrize), 84)
    assert(values.exists(_ != 0))

  test("Story Proof lists the full missing evidence tuple before any Story can speak"):
    val story = Story(
      Scene.Material,
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99
      )
    )
    val verdict = StoryTable.choose(Vector(story)).head
    val expectedMissing =
      Vector("side", "target", "anchor", "route", "rival", "legal line", "same-board proof")

    assertEquals(story.proofFailures, Vector(BoardFacts.MissingEvidence("Story Proof", expectedMissing)))
    assertEquals(verdict.proofFailures, story.proofFailures)
    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("Story Proof does not own or duplicate Story identity"):
    val storyProofFields =
      classOf[StoryProof].getDeclaredFields
        .map(_.getName)
        .filterNot(name => name.startsWith("$") || name.contains("bitmap"))
        .toVector
        .sorted
    val storyProofMethods = classOf[StoryProof].getDeclaredMethods.map(_.getName).toVector
    val identityTerms = Vector("side", "target", "anchor", "route", "rival")

    assertEquals(storyProofFields, Vector("legalLine", "sameBoardProof"))
    identityTerms.foreach: term =>
      assert(!storyProofFields.exists(_.toLowerCase.contains(term)), s"StoryProof must not own $term")
    assert(!storyProofMethods.contains("copy"))
    assert(!classOf[Product].isAssignableFrom(classOf[StoryProof]))

  test("Story Proof legal line binding reports each missing evidence case"):
    def completeStory(
        route: Option[Line] = Some(safeRoute),
        storyProof: StoryProof = storyProof(),
        side: Side = Side.White,
        rival: Side = Side.Black,
        target: Option[Square] = Some(Square('a', 2)),
        anchor: Option[Square] = Some(safeAnchor)
    ) =
      Story(
        Scene.Material,
        side = side,
        target = target,
        anchor = anchor,
        route = route,
        rival = rival,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99
        ),
        storyProof = storyProof
      )

    val mismatchedRoute = Line(Square('b', 1), Square('b', 2))
    val cases = Vector(
      ("route missing", completeStory(route = None), Vector("route", "legal line")),
      ("legal line missing", completeStory(storyProof = StoryProof.empty), Vector(
        "legal line",
        "same-board proof"
      )),
      ("untrusted facts cannot forge same-board proof", completeStory(
        storyProof = StoryProof.fromBoardFacts(minimalBoardFacts(), safeRoute)
      ), Vector(
        "legal line",
        "same-board proof"
      )),
      ("legal line mismatch", completeStory(route = Some(mismatchedRoute)), Vector("legal line")),
      ("same-board proof missing", completeStory(storyProof = untrustedStoryProof()), Vector(
        "same-board proof"
      )),
      ("side missing", completeStory(side = Side.None), Vector("side", "rival")),
      ("side both", completeStory(side = Side.Both), Vector("side", "rival")),
      ("rival missing", completeStory(rival = Side.None), Vector("rival")),
      ("rival both", completeStory(rival = Side.Both), Vector("rival")),
      ("rival same as side", completeStory(rival = Side.White), Vector("rival")),
      ("target missing", completeStory(target = None), Vector("target")),
      ("anchor missing", completeStory(anchor = None), Vector("anchor"))
    )

    cases.foreach: (label, story, missing) =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(story.proofFailures, Vector(BoardFacts.MissingEvidence("Story Proof", missing)), label)
      assertEquals(verdict.proofFailures, story.proofFailures, label)
      assertEquals(verdict.leadAllowed, false, label)
      assert(verdict.role != Role.Lead, label)

  test("Complete Story Proof is necessary but not sufficient for public Lead in Stage 2"):
    def completeStory(
        scene: Scene,
        plan: Option[Plan] = None,
        tactic: Option[Tactic] = None,
        route: Line = safeRoute,
        target: Square = Square('a', 2),
        anchor: Square = safeAnchor,
        proofScore: Proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99,
          forcing = 99,
          kingHeat = 99,
          immediacy = 99
        )
    ) =
      Story(
        scene,
        side = Side.White,
        target = Some(target),
        anchor = Some(anchor),
        route = Some(route),
        rival = Side.Black,
        plan = plan,
        tactic = tactic,
        proof = proofScore,
        storyProof = storyProof(route)
      )

    val completeStories = Vector(
      "Material" -> completeStory(Scene.Material),
      "Tactic" -> completeStory(Scene.Tactic, tactic = Some(Tactic.Fork)),
      "Tactic.Hanging" -> completeStory(Scene.Tactic, tactic = Some(Tactic.Hanging)),
      "Plan" -> completeStory(Scene.Plan, plan = Some(Plan.CenterBreak)),
      "King" -> completeStory(Scene.King)
    )

    completeStories.foreach: (label, story) =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(story.proofFailures, Vector.empty, label)
      assertEquals(verdict.proofFailures, Vector.empty, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(verdict.role, Role.Blocked, label)

    val boardBacked = completeStory(Scene.Material, target = Square('b', 2))
    val source = completeStory(Scene.Source, target = Square('c', 2))
    val opening = completeStory(Scene.Opening, target = Square('d', 2))
    val sourceVerdicts = StoryTable.choose(Vector(source, boardBacked))
    val openingVerdicts = StoryTable.choose(Vector(opening, boardBacked))

    assertEquals(source.proofFailures, Vector.empty)
    assertEquals(opening.proofFailures, Vector.empty)
    assertEquals(sourceVerdicts.head.story, boardBacked)
    assertEquals(openingVerdicts.head.story, boardBacked)
    assert(sourceVerdicts.forall(_.role != Role.Lead))
    assert(openingVerdicts.forall(_.role != Role.Lead))
    assert(sourceVerdicts.find(_.story == source).exists(!_.leadAllowed))
    assert(openingVerdicts.find(_.story == opening).exists(!_.leadAllowed))

  test("CaptureResult records legal capture material evidence without public claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))

    val result = CaptureResult.fromBoardFacts(facts, capture)
    val publicSurfaceNames =
      classOf[CaptureResult].getDeclaredMethods.map(_.getName).toSet ++
        classOf[CaptureResult].getDeclaredFields.map(_.getName).toSet

    assertEquals(result.side, Side.White)
    assertEquals(result.capturingPiece, Some(Piece(Side.White, Man.Pawn, Square('d', 4))))
    assertEquals(result.targetPiece, Some(Piece(Side.Black, Man.Knight, Square('e', 5))))
    assertEquals(result.captureLine, capture)
    assertEquals(result.capturedValue, Some(320))
    assertEquals(result.recaptureCandidates, Vector.empty)
    assertEquals(result.materialResult, Some(320))
    assertEquals(result.sameBoardProof, true)
    assertEquals(result.missingEvidence, Vector.empty)
    assertEquals(result.publicClaimAllowed, false)
    Vector("leadAllowed", "publicText", "render", "llm", "verdict").foreach: publicName =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(publicName.toLowerCase)))

  test("CaptureResult bounded recapture check can cancel the material result"):
    val facts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('e', 4), Square('d', 5))

    val result = CaptureResult.fromBoardFacts(facts, capture)

    assertEquals(result.capturingPiece.map(_.man), Some(Man.Queen))
    assertEquals(result.targetPiece.map(_.man), Some(Man.Queen))
    assertEquals(result.capturedValue, Some(900))
    assertEquals(result.recaptureCandidates.map(_.piece.man), Vector(Man.Knight))
    assertEquals(result.materialResult, Some(0))
    assertEquals(result.positiveMaterial, false)
    assertEquals(result.missingEvidence, Vector.empty)

  test("Material-2 CaptureResult carries bounded material proof shape without Story or sentence"):
    val whiteFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val blackFacts = BoardFacts.fromFen("4k3/8/8/4p3/3N4/8/8/4K3 b - - 0 1").toOption.get
    val whiteCapture = Line(Square('d', 4), Square('e', 5))
    val blackCapture = Line(Square('e', 5), Square('d', 4))
    val whiteResult = CaptureResult.fromBoardFacts(whiteFacts, whiteCapture)
    val blackResult = CaptureResult.fromBoardFacts(blackFacts, blackCapture)
    val publicSurfaceNames =
      classOf[CaptureResult].getDeclaredMethods.map(_.getName).toSet ++
        classOf[CaptureResult].getDeclaredFields.map(_.getName).toSet ++
        classOf[CaptureResult.ExchangeStep].getDeclaredMethods.map(_.getName).toSet ++
        classOf[CaptureResult.ExchangeStep].getDeclaredFields.map(_.getName).toSet

    assertEquals(whiteResult.side, Side.White)
    assertEquals(whiteResult.captureLine, whiteCapture)
    assertEquals(whiteResult.capturedPieces, Vector(Piece(Side.Black, Man.Knight, Square('e', 5))))
    assertEquals(
      whiteResult.boundedExchangeSequence,
      Vector(CaptureResult.ExchangeStep(whiteCapture, Piece(Side.Black, Man.Knight, Square('e', 5)), 320))
    )
    assertEquals(whiteResult.materialResult, Some(320))
    assertEquals(whiteResult.sameBoardProof, true)
    assertEquals(whiteResult.missingEvidence, Vector.empty)

    assertEquals(blackResult.side, Side.Black)
    assertEquals(blackResult.captureLine, blackCapture)
    assertEquals(blackResult.capturedPieces, Vector(Piece(Side.White, Man.Knight, Square('d', 4))))
    assertEquals(
      blackResult.boundedExchangeSequence,
      Vector(CaptureResult.ExchangeStep(blackCapture, Piece(Side.White, Man.Knight, Square('d', 4)), 320))
    )
    assertEquals(blackResult.materialResult, Some(320))
    assertEquals(blackResult.sameBoardProof, true)
    assertEquals(blackResult.missingEvidence, Vector.empty)

    Vector(
      "Story",
      "Sentence",
      "RenderedLine",
      "ExplanationPlan",
      "Verdict",
      "publicText",
      "render",
      "llm"
    ).foreach: forbidden =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    assertEquals(whiteResult.publicClaimAllowed, false)
    assertEquals(blackResult.publicClaimAllowed, false)

  test("Material-2 bounded exchange sequence records recapture candidates and final material result"):
    val facts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('e', 4), Square('d', 5))
    val recapture = Line(Square('f', 4), Square('d', 5))
    val result = CaptureResult.fromBoardFacts(facts, capture)

    assertEquals(result.side, Side.White)
    assertEquals(result.captureLine, capture)
    assertEquals(result.recaptureCandidates, Vector(CaptureResult.Recapture(Piece(Side.Black, Man.Knight, Square('f', 4)), recapture)))
    assertEquals(
      result.capturedPieces,
      Vector(
        Piece(Side.Black, Man.Queen, Square('d', 5)),
        Piece(Side.White, Man.Queen, Square('d', 5))
      )
    )
    assertEquals(
      result.boundedExchangeSequence,
      Vector(
        CaptureResult.ExchangeStep(capture, Piece(Side.Black, Man.Queen, Square('d', 5)), 900),
        CaptureResult.ExchangeStep(recapture, Piece(Side.White, Man.Queen, Square('d', 5)), 0)
      )
    )
    assertEquals(result.materialResult, Some(0))
    assertEquals(result.positiveMaterial, false)
    assertEquals(result.sameBoardProof, true)
    assertEquals(result.missingEvidence, Vector.empty)

  test("Defense-1 ThreatProof proves immediate legal material threat without Story or public claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val proof = ThreatProof.fromBoardFacts(facts, threatLine)
    val publicSurfaceNames =
      classOf[ThreatProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ThreatProof].getDeclaredFields.map(_.getName).toSet
    val threatMethods = ThreatProof.getClass.getDeclaredMethods.toVector

    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.threatenedTarget, Some(Piece(Side.White, Man.Queen, Square('d', 4))))
    assertEquals(proof.attackingPiece, Some(Piece(Side.Black, Man.Knight, Square('f', 5))))
    assertEquals(proof.legalThreatLine, Some(threatLine))
    assertEquals(proof.targetValue, Some(900))
    assertEquals(proof.materialLossIfUnanswered, Some(900))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.missingEvidence, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    Vector(
      "Story",
      "Sentence",
      "RenderedLine",
      "ExplanationPlan",
      "Verdict",
      "publicText",
      "render",
      "llm",
      "engine"
    ).foreach: forbidden =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    assertEquals(threatMethods.filter(_.getName == "fromBoardFacts").map(_.getReturnType.getSimpleName), Vector("ThreatProof"))
    assertEquals(threatMethods.exists(_.getReturnType.getSimpleName.contains("Story")), false)

  test("Defense-1 ThreatProof keeps non-immediate and non-material threats incomplete"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Q3/8/5N2/4K3 w - - 0 1").toOption.get
    val recaptureCancelledThreat = Line(Square('d', 5), Square('e', 4))
    val untrusted =
      minimalBoardFacts(
        rivalLegal = readyMoves(line = threatLine, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Queen, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('f', 5))
        )
      )
    val kingTarget =
      minimalBoardFacts(
        rivalLegal = readyMoves(line = Line(Square('f', 5), Square('e', 3)), captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 3)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.Black, Man.Knight, Square('f', 5))
        )
      )
    val attackOnly =
      ThreatProof.fromBoardFacts(positiveFacts, Line(Square('f', 5), Square('h', 4)))
    val illegalThreat =
      ThreatProof.fromBoardFacts(positiveFacts, Line(Square('f', 5), Square('f', 4)))
    val untrustedThreat = ThreatProof.fromBoardFacts(untrusted, threatLine)
    val kingThreat = ThreatProof.fromBoardFacts(kingTarget, Line(Square('f', 5), Square('e', 3)))
    val cancelledThreat = ThreatProof.fromBoardFacts(defendedFacts, recaptureCancelledThreat)
    val highProofOnly =
      Story(
        Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('e', 2)),
        route = Some(safeRoute),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99,
          forcing = 99,
          immediacy = 99
        ),
        storyProof = storyProof()
      )

    Vector(attackOnly, illegalThreat, untrustedThreat, kingThreat, cancelledThreat).foreach: proof =>
      assertEquals(proof.complete, false)
      assert(proof.missingEvidence.nonEmpty)
      assertEquals(proof.publicClaimAllowed, false)
    assertEquals(attackOnly.missingEvidence.exists(_.missing.contains("legal threat line")), true)
    assertEquals(illegalThreat.missingEvidence.exists(_.missing.contains("legal threat line")), true)
    assertEquals(untrustedThreat.missingEvidence.exists(_.missing.contains("same-board proof")), true)
    assertEquals(kingThreat.missingEvidence.exists(_.missing.contains("threatened non-king target")), true)
    assertEquals(cancelledThreat.materialLossIfUnanswered, Some(0))
    assertEquals(cancelledThreat.missingEvidence.exists(_.missing.contains("material loss if unanswered")), true)
    assertEquals(StoryTable.choose(Vector(highProofOnly)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(highProofOnly)).head.leadAllowed, false)

  test("Defense-2 DefenseProof proves target moves away without Story or public claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val threat = ThreatProof.fromBoardFacts(facts, threatLine)
    val proof = DefenseProof.fromBoardFacts(facts, threat, defenseMove)
    val publicSurfaceNames =
      classOf[DefenseProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[DefenseProof].getDeclaredFields.map(_.getName).toSet
    val defenseMethods = DefenseProof.getClass.getDeclaredMethods.toVector

    assertEquals(proof.defendingSide, Side.White)
    assertEquals(proof.defenseMove, Some(defenseMove))
    assertEquals(proof.defendedTarget, Some(Piece(Side.White, Man.Queen, Square('d', 4))))
    assertEquals(proof.originalThreat, threat)
    assertEquals(proof.afterDefenseTargetStatus, Some(DefenseTargetStatus.TargetMovedAway))
    assertEquals(proof.materialLossPrevented, Some(900))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.missingEvidence, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    Vector(
      "Story",
      "Sentence",
      "RenderedLine",
      "ExplanationPlan",
      "Verdict",
      "publicText",
      "render",
      "llm",
      "engine"
    ).foreach: forbidden =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    assertEquals(defenseMethods.filter(_.getName == "fromBoardFacts").map(_.getReturnType.getSimpleName), Vector("DefenseProof"))
    assertEquals(defenseMethods.exists(_.getReturnType.getSimpleName.contains("Story")), false)

  test("Defense-2 DefenseProof proves guarding blocking and attacker capture only"):
    val guardedFacts = BoardFacts.fromFen("4k3/8/8/3q4/3R4/8/7B/4K3 w - - 0 1").toOption.get
    val guardedThreatLine = Line(Square('d', 5), Square('d', 4))
    val guardedMove = Line(Square('h', 2), Square('e', 5))
    val guardedThreat = ThreatProof.fromBoardFacts(guardedFacts, guardedThreatLine)
    val guardedProof = DefenseProof.fromBoardFacts(guardedFacts, guardedThreat, guardedMove)
    val blockedFacts = BoardFacts.fromFen("3qk3/8/8/8/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val blockedThreatLine = Line(Square('d', 8), Square('d', 4))
    val blockedMove = Line(Square('e', 4), Square('d', 5))
    val blockedThreat = ThreatProof.fromBoardFacts(blockedFacts, blockedThreatLine)
    val blockedProof = DefenseProof.fromBoardFacts(blockedFacts, blockedThreat, blockedMove)
    val capturedFacts = BoardFacts.fromFen("4k3/8/8/5n2/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val capturedThreatLine = Line(Square('f', 5), Square('d', 4))
    val capturedMove = Line(Square('e', 4), Square('f', 5))
    val capturedThreat = ThreatProof.fromBoardFacts(capturedFacts, capturedThreatLine)
    val capturedProof = DefenseProof.fromBoardFacts(capturedFacts, capturedThreat, capturedMove)

    assertEquals(guardedThreat.materialLossIfUnanswered, Some(500))
    assertEquals(guardedProof.afterDefenseTargetStatus, Some(DefenseTargetStatus.TargetGuarded))
    assertEquals(guardedProof.materialLossPrevented, Some(500))
    assertEquals(guardedProof.complete, true)
    assertEquals(blockedThreat.materialLossIfUnanswered, Some(500))
    assertEquals(blockedProof.afterDefenseTargetStatus, Some(DefenseTargetStatus.AttackerLineBlocked))
    assertEquals(blockedProof.materialLossPrevented, Some(500))
    assertEquals(blockedProof.complete, true)
    assertEquals(capturedThreat.materialLossIfUnanswered, Some(500))
    assertEquals(capturedProof.afterDefenseTargetStatus, Some(DefenseTargetStatus.AttackerCaptured))
    assertEquals(capturedProof.materialLossPrevented, Some(500))
    assertEquals(capturedProof.complete, true)
    Vector(guardedProof, blockedProof, capturedProof).foreach: proof =>
      assertEquals(proof.missingEvidence, Vector.empty)
      assertEquals(proof.publicClaimAllowed, false)

  test("Defense-2 DefenseProof keeps unsupported defense-looking moves incomplete"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val threat = ThreatProof.fromBoardFacts(facts, threatLine)
    val illegalMove = DefenseProof.fromBoardFacts(facts, threat, Line(Square('a', 1), Square('a', 2)))
    val quietMove = DefenseProof.fromBoardFacts(facts, threat, Line(Square('e', 1), Square('e', 2)))
    val stillCapturableTargetMove =
      DefenseProof.fromBoardFacts(facts, threat, Line(Square('d', 4), Square('h', 4)))
    val untrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = Line(Square('d', 4), Square('e', 4))),
        rivalLegal = readyMoves(line = threatLine, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Queen, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('f', 5))
        )
      )
    val untrustedThreat = ThreatProof.fromBoardFacts(untrusted, threatLine)
    val untrustedProof =
      DefenseProof.fromBoardFacts(untrusted, untrustedThreat, Line(Square('d', 4), Square('e', 4)))
    val incompleteThreat =
      ThreatProof.fromBoardFacts(facts, Line(Square('f', 5), Square('f', 4)))
    val incompleteThreatProof =
      DefenseProof.fromBoardFacts(facts, incompleteThreat, Line(Square('d', 4), Square('e', 4)))
    val highProofOnly =
      Story(
        Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(Line(Square('d', 4), Square('e', 4))),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99,
          forcing = 99,
          immediacy = 99
        ),
        storyProof = storyProof()
      )

    Vector(illegalMove, quietMove, stillCapturableTargetMove, untrustedProof, incompleteThreatProof).foreach: proof =>
      assertEquals(proof.complete, false)
      assert(proof.missingEvidence.nonEmpty)
      assertEquals(proof.publicClaimAllowed, false)
    assertEquals(illegalMove.missingEvidence.exists(_.missing.contains("legal defense move")), true)
    assertEquals(quietMove.missingEvidence.exists(_.missing.contains("after-defense target status")), true)
    assertEquals(stillCapturableTargetMove.missingEvidence.exists(_.missing.contains("after-defense target status")), true)
    assertEquals(untrustedProof.missingEvidence.exists(_.missing.contains("same-board proof")), true)
    assertEquals(incompleteThreatProof.missingEvidence.exists(_.missing.contains("complete ThreatProof")), true)
    assertEquals(StoryTable.choose(Vector(highProofOnly)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(highProofOnly)).head.leadAllowed, false)

  test("Defense-3 SceneDefense writer admits one proof-backed Defense Story"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val threat = ThreatProof.fromBoardFacts(facts, threatLine)
    val defense = DefenseProof.fromBoardFacts(facts, threat, defenseLine)
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(threat.complete, true)
    assertEquals(defense.complete, true)
    assertEquals(story.scene, Scene.Defense)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('d', 4)))
    assertEquals(story.anchor, Some(Square('d', 4)))
    assertEquals(story.route, Some(defenseLine))
    assertEquals(story.writer, Some(StoryWriter.SceneDefense))
    assertEquals(story.threatProof, Some(threat))
    assertEquals(story.defenseProof, Some(defense))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(story.engineCheck.exists(_.status == EngineCheckStatus.Refutes), false)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.story.scene, Scene.Defense)

  test("Defense-3 SceneDefense writer blocks refuted and unsupported Defense rows"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val quietDefense = Line(Square('e', 1), Square('e', 2))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val refute = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(-100)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val refutedStory = SceneDefense.withEngineCheck(story, refute).get
    val forged =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99,
          forcing = 99,
          immediacy = 99
        ),
        storyProof = StoryProof.fromBoardFacts(facts, defenseLine),
        writer = Some(StoryWriter.SceneDefense)
      )

    assertEquals(SceneDefense.write(facts, threatLine, quietDefense), None)
    assertEquals(refute.status, EngineCheckStatus.Refutes)
    assertEquals(StoryTable.choose(Vector(refutedStory)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(refutedStory)).head.leadAllowed, false)
    assertEquals(StoryTable.choose(Vector(forged)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(forged)).head.leadAllowed, false)

  test("Defense-4 negative corpus keeps defense-looking false positives silent"):
    val baselineFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val quietDefense = Line(Square('e', 1), Square('e', 2))
    val wrongGuardFacts = BoardFacts.fromFen("4k3/8/8/3q4/3R4/8/B6B/4K3 w - - 0 1").toOption.get
    val alreadyDefendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Q3/8/5N2/4K3 w - - 0 1").toOption.get
    val stillLosesFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val recaptureFacts = BoardFacts.fromFen("4k3/8/8/5n2/3RBq2/8/8/4K3 w - - 0 1").toOption.get
    val tacticFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val noThreat = SceneDefense.write(baselineFacts, Line(Square('f', 5), Square('h', 4)), defenseLine)
    val illegalThreat = SceneDefense.write(baselineFacts, Line(Square('f', 5), Square('f', 4)), defenseLine)
    val alreadyDefended =
      SceneDefense.write(alreadyDefendedFacts, Line(Square('d', 5), Square('e', 4)), Line(Square('e', 4), Square('e', 3)))
    val doesNotAffectTarget = SceneDefense.write(baselineFacts, threatLine, quietDefense)
    val guardsWrongPiece =
      SceneDefense.write(wrongGuardFacts, Line(Square('d', 5), Square('d', 4)), Line(Square('a', 2), Square('b', 3)))
    val stillLoses = SceneDefense.write(stillLosesFacts, threatLine, Line(Square('d', 4), Square('h', 4)))
    val allowsEquivalentRecapture =
      SceneDefense.write(recaptureFacts, Line(Square('f', 5), Square('d', 4)), Line(Square('e', 4), Square('f', 5)))
    val prophylaxis =
      Story(
        scene = Scene.Defense,
        plan = Some(Plan.Prophy),
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('e', 2)),
        route = Some(quietDefense),
        routeSan = Some("Ke2"),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99,
          forcing = 99,
          immediacy = 99
        ),
        storyProof = StoryProof.fromBoardFacts(baselineFacts, quietDefense),
        writer = Some(StoryWriter.SceneDefense)
      )
    val tacticMaterialGain = SceneDefense.write(tacticFacts, Line(Square('e', 5), Square('d', 4)), Line(Square('d', 4), Square('e', 5)))
    val kingSafetyClaim =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('e', 1)),
        anchor = Some(Square('g', 2)),
        route = Some(Line(Square('g', 2), Square('g', 3))),
        routeSan = Some("g3"),
        rival = Side.Black,
        proof = proof(boardProof = 99, lineProof = 99, ownerProof = 99, anchorProof = 99, routeProof = 99, kingHeat = 99),
        storyProof = StoryProof.empty,
        writer = Some(StoryWriter.SceneDefense)
      )
    val mateDefenseClaim =
      Story(
        scene = Scene.Defense,
        tactic = Some(Tactic.MateNet),
        side = Side.White,
        target = Some(Square('e', 1)),
        anchor = Some(Square('g', 2)),
        route = Some(Line(Square('g', 2), Square('g', 3))),
        routeSan = Some("g3"),
        rival = Side.Black,
        proof = proof(boardProof = 99, lineProof = 99, ownerProof = 99, anchorProof = 99, routeProof = 99, kingHeat = 99),
        storyProof = StoryProof.empty,
        writer = Some(StoryWriter.SceneDefense)
      )
    val onlyMoveClaim =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          forcing = 100,
          conversionPrize = 100,
          clarity = 100
        ),
        storyProof = StoryProof.fromBoardFacts(baselineFacts, defenseLine),
        writer = Some(StoryWriter.SceneDefense)
      )
    val storyProofIncomplete =
      SceneDefense.write(minimalBoardFacts(sideLegal = readyMoves(line = defenseLine), rivalLegal = readyMoves(line = threatLine)), threatLine, defenseLine)
    val threatProofIncomplete = SceneDefense.write(baselineFacts, Line(Square('f', 5), Square('f', 4)), defenseLine)
    val defenseProofIncomplete = SceneDefense.write(baselineFacts, threatLine, quietDefense)
    val refuted = SceneDefense.write(baselineFacts, threatLine, defenseLine).flatMap: story =>
      val check = EngineCheck.fromStory(
        facts = baselineFacts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(defenseLine))),
        replyLine = Some(EngineLine(Vector(threatLine))),
        evalBefore = Some(EngineEval(120)),
        evalAfter = Some(EngineEval(-120)),
        depth = Some(12),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      SceneDefense.withEngineCheck(story, check)
    val highProofOnly =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = proof(
          boardProof = 100,
          lineProof = 100,
          ownerProof = 100,
          anchorProof = 100,
          routeProof = 100,
          forcing = 100,
          conversionPrize = 100,
          clarity = 100
        ),
        storyProof = StoryProof.fromBoardFacts(baselineFacts, defenseLine)
      )

    Vector(
      "no actual threat" -> noThreat,
      "threat is illegal" -> illegalThreat,
      "attacked piece is already adequately defended" -> alreadyDefended,
      "defense move does not affect the target" -> doesNotAffectTarget,
      "defense move guards wrong piece" -> guardsWrongPiece,
      "defense move still loses material" -> stillLoses,
      "defense move allows equivalent recapture" -> allowsEquivalentRecapture,
      "defense is actually a tactic / material gain" -> tacticMaterialGain,
      "StoryProof incomplete" -> storyProofIncomplete,
      "ThreatProof incomplete" -> threatProofIncomplete,
      "DefenseProof incomplete" -> defenseProofIncomplete
    ).foreach: (label, defenseStory) =>
      assertEquals(defenseStory, None, label)

    Vector(
      "defense only looks like prophylaxis" -> prophylaxis,
      "king safety claim tries to enter" -> kingSafetyClaim,
      "mate defense tries to enter" -> mateDefenseClaim,
      "only-move claim tries to enter" -> onlyMoveClaim,
      "EngineCheck Refutes" -> refuted.get,
      "high Proof score only" -> highProofOnly
    ).foreach: (label, story) =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(verdict.role, Role.Blocked, label)

  test("Defense-5 reuses EngineCheck statuses for existing SceneDefense Stories only"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(defenseLine))),
        replyLine = Some(EngineLine(Vector(threatLine))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(12),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val unknown = SceneDefense.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = SceneDefense.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = SceneDefense.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes = SceneDefense.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 100, after = -100)).get
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)

  test("Defense-5 EngineCheck cannot create or explain Defense without same-board Story binding"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val otherFacts = BoardFacts.fromFen("4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val wrongRoute = Line(Square('d', 4), Square('h', 4))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val staleOrDepthMissing = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = None,
      freshnessPly = Some(2),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatched = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(wrongRoute),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val differentBoard = EngineCheck.fromStory(
      facts = otherFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(SceneDefense.withEngineCheck(story, engineOnly), None)
    assertEquals(SceneDefense.withEngineCheck(story, staleOrDepthMissing), None)
    assertEquals(SceneDefense.withEngineCheck(story, routeMismatched), None)
    assertEquals(SceneDefense.withEngineCheck(story, differentBoard), None)
    assertEquals(StoryTable.choose(Vector(story)).head.role, Role.Lead)

  test("Defense-6 StoryTable orders Hanging Fork Material and Defense deterministically"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val material = SceneMaterial.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get

    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val high = score(99)
    val tied = Vector(
      hanging.copy(proof = high),
      fork.copy(proof = high),
      material.copy(proof = high),
      defense.copy(proof = high)
    )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(tied(3), tied(1), tied(2), tied(0)))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.map(_.story.scene).toSet, Set(Scene.Tactic, Scene.Material, Scene.Defense))
    assertEquals(forward.map(_.story.tactic).toSet, Set(Some(Tactic.Hanging), Some(Tactic.Fork), None))
    assertEquals(forward.map(_.role), Vector(Role.Lead, Role.Support, Role.Support, Role.Support))
    forward.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("Defense-6 StoryTable blocks invalid Defense rows without creating meaning"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val refute = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(-100)),
      depth = Some(14),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val refuted = SceneDefense.withEngineCheck(story, refute).get
    val writerless = story.copy(writer = None)
    val withoutThreatProof = story.copy(threatProof = None)
    val withoutDefenseProof = story.copy(defenseProof = None)
    val incompleteDefense =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = story.proof,
        storyProof = StoryProof.empty,
        writer = Some(StoryWriter.SceneDefense),
        threatProof = story.threatProof,
        defenseProof = story.defenseProof
      )
    val highProofOnly =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = proof(
          boardProof = 100,
          lineProof = 100,
          ownerProof = 100,
          anchorProof = 100,
          routeProof = 100,
          forcing = 100,
          conversionPrize = 100,
          clarity = 100
        ),
        storyProof = StoryProof.fromBoardFacts(facts, defenseLine)
      )
    val alternateDefenseLine = Line(Square('d', 4), Square('c', 4))
    val alternateStory = SceneDefense.write(facts, threatLine, alternateDefenseLine).get
    val rawEvalFavoredLowProof = story
      .copy(
        proof = proof(boardProof = 72, lineProof = 72, ownerProof = 72, anchorProof = 72, routeProof = 72),
        engineCheck = Some(
          EngineCheck.fromEvidence(
            sameBoardProof = true,
            checkedMove = Some(defenseLine),
            engineLine = Some(EngineLine(Vector(defenseLine))),
            replyLine = Some(EngineLine(Vector(threatLine))),
            evalBefore = Some(EngineEval(-900)),
            evalAfter = Some(EngineEval(900)),
            depth = Some(20),
            freshnessPly = Some(0),
            requestedStatus = EngineCheckStatus.Supports
          )
        )
      )
    val proofFavored = alternateStory.copy(
      proof = proof(boardProof = 95, lineProof = 95, ownerProof = 95, anchorProof = 95, routeProof = 95)
    )

    Vector(
      "Refuted Defense" -> refuted,
      "incomplete Defense" -> incompleteDefense,
      "writerless Defense" -> writerless,
      "Defense without ThreatProof" -> withoutThreatProof,
      "Defense without DefenseProof" -> withoutDefenseProof,
      "high Proof score only" -> highProofOnly
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)

    val evalOrder = StoryTable.choose(Vector(rawEvalFavoredLowProof, proofFavored))
    assertEquals(evalOrder.head.story, proofFavored)
    assertEquals(evalOrder.exists(_.story == rawEvalFavoredLowProof), true)
    assertEquals(StoryTable.choose(Vector(story)).head.role, Role.Lead)
    assertEquals(StoryTable.choose(Vector(story)).exists(_.story.scene == Scene.Defense), true)

  test("Defense-7 ExplanationPlan lowers selected Defense Verdict without raw proof input"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(defense)).head
    val maybePlan = ExplanationPlan.fromSelected(verdict)
    val fromSelectedMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    val fromSelectedParameterNames =
      fromSelectedMethods
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getName)
        .mkString(" ")
    val planSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)), Vector(Vector("Verdict")))
    Vector(
      "ThreatProof",
      "DefenseProof",
      "EngineCheck",
      "BoardFacts",
      "EngineLine",
      "Source",
      "String"
    ).foreach: forbiddenType =>
      assert(!fromSelectedParameterNames.contains(forbiddenType), s"ExplanationPlan must not accept $forbiddenType")

    val plan = maybePlan.get
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Defense)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('d', 4)))
    assertEquals(plan.anchor, Some(Square('d', 4)))
    assertEquals(plan.route, Some(defenseLine))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    assertEquals(plan.allowedClaim.map(_.key), Some("defends_piece"))
    assertEquals(
      ExplanationClaim.DefenseAllowed.map(_.key),
      Vector("defends_piece", "prevents_material_loss", "protects_target")
    )
    assertEquals(
      ExplanationClaim.DefenseForbiddenKeys,
      Vector(
        "only_move",
        "best_defense",
        "refutes_attack",
        "stops_counterplay",
        "solves_position",
        "king_safe",
        "mate_defense",
        "no_counterplay"
      )
    )
    assertEquals(plan.evidenceLine, Some(defenseLine))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    Vector(
      "only_move",
      "best_move",
      "best_defense",
      "refutes_attack",
      "stops_counterplay",
      "solves_position",
      "king_safe",
      "mate_defense",
      "no_counterplay"
    ).foreach: forbidden =>
      assert(plan.forbiddenWording.map(_.key).contains(forbidden), forbidden)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    Vector(
      "threatProof",
      "defenseProof",
      "engineCheck",
      "boardFacts",
      "rawPv",
      "proofFailures",
      "sourceRow"
    ).foreach: forbiddenName =>
      assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Defense-7 Defense non Lead capped and refuted plans create no stronger claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val leftMove = Line(Square('d', 4), Square('e', 4))
    val rightMove = Line(Square('d', 4), Square('c', 4))
    val leftDefense = SceneDefense.write(facts, threatLine, leftMove).get
    val rightDefense = SceneDefense.write(facts, threatLine, rightMove).get.copy(proof = leftDefense.proof)
    val verdicts = StoryTable.choose(Vector(leftDefense, rightDefense))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false)
    val cappedCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(leftDefense),
      engineLine = Some(EngineLine(Vector(leftMove))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val refuteCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(leftDefense),
      engineLine = Some(EngineLine(Vector(leftMove))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(-100)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val cappedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(SceneDefense.withEngineCheck(leftDefense, cappedCheck).get)).head).get
    val refutedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(SceneDefense.withEngineCheck(leftDefense, refuteCheck).get)).head).get
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val supportPlan = ExplanationPlan.fromSelected(supportVerdict).get
    val contextPlan = ExplanationPlan.fromSelected(contextVerdict).get
    val blockedPlan = ExplanationPlan.fromSelected(blockedVerdict).get

    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    Vector(supportPlan, contextPlan, blockedPlan, cappedPlan, refutedPlan).foreach: plan =>
      assertEquals(plan.allowedClaim, None)
    assertEquals(supportPlan.relations, Vector(ExplanationRelation.SameFamilyLowerRank))
    assertEquals(contextPlan.relations, Vector.empty)
    assertEquals(blockedPlan.debugOnly, true)
    assertEquals(cappedPlan.forbiddenWording.contains(ForbiddenWording.StrongWording), true)
    assertEquals(refutedPlan.relations, Vector(ExplanationRelation.BlockedByEngineRefute))
    assertEquals(refutedPlan.debugOnly, true)

  test("Defense-8 DeterministicRenderer phrases Defense ExplanationPlan without stronger meaning"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(defense)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val materialLossPlan = plan.copy(allowedClaim = Some(ExplanationClaim.PreventsMaterialLoss))
    val materialLossRendered = DeterministicRenderer.fromPlan(materialLossPlan).get
    val rendererMethods =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .map(_.getParameterTypes.toVector.map(_.getSimpleName))
        .toVector

    assertEquals(rendererMethods, Vector(Vector("ExplanationPlan")))
    assertEquals(rendered.text, "Qe4+ defends the piece on d4.")
    assertEquals(rendered.claimKey, "defends_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(materialLossRendered.text, "Qe4+ prevents the piece on d4 from being lost immediately.")
    assertEquals(materialLossRendered.claimKey, "prevents_material_loss")
    Vector(
      "only move",
      "best move",
      "refutes the attack",
      "stops all counterplay",
      "solves the position",
      "king is safe",
      "mate is stopped",
      "winning",
      "decisive",
      "forced"
    ).foreach: forbidden =>
      assert(!rendered.text.toLowerCase.contains(forbidden))
      assert(!materialLossRendered.text.toLowerCase.contains(forbidden))

  test("Defense-8 Renderer refuses Defense text without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val alternateDefenseLine = Line(Square('d', 4), Square('c', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val alternateDefense = SceneDefense.write(facts, threatLine, alternateDefenseLine).get.copy(proof = defense.proof)
    val verdicts = StoryTable.choose(Vector(defense, alternateDefense))
    val supportPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Support).get).get
    val leadPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get).get
    val blockedPlan = leadPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val cappedPlan = leadPlan.copy(allowedClaim = None, forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.StrongWording)
    val wrongScenePlan = leadPlan.copy(scene = Scene.Material)
    val wrongClaimPlan = leadPlan.copy(allowedClaim = Some(ExplanationClaim.MaterialBalanceChanges))
    val strongerRoutePlan = leadPlan.copy(routeSan = Some("Qe4 wins material"))

    Vector(supportPlan, blockedPlan, cappedPlan, wrongScenePlan, wrongClaimPlan, strongerRoutePlan).foreach: plan =>
      assertEquals(DeterministicRenderer.fromPlan(plan), None)

  test("Defense-9 LLM smoke accepts Defense RenderedLine without raw proof input"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(defense)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val smokeMethods =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
    val smokeParameterNames =
      smokeMethods.flatMap(_.getParameterTypes.toVector).map(_.getName).mkString(" ")

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text), NarrationSmokeCheck(true, Vector.empty))
    assert(prompt.contains("renderedText: Qe4+ defends the piece on d4."))
    assert(prompt.contains("claimKey: defends_piece"))
    assert(prompt.contains("strength: bounded"))
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector(
      "ThreatProof",
      "DefenseProof",
      "EngineCheck",
      "BoardFacts",
      "EngineEval",
      "EngineLine",
      "Story",
      "Verdict"
    ).foreach: forbiddenType =>
      assert(!smokeParameterNames.contains(forbiddenType), s"LLM smoke must not accept $forbiddenType")
    Vector(
      "raw Verdict",
      "Story",
      "ThreatProof",
      "DefenseProof",
      "EngineCheck",
      "BoardFacts",
      "engine eval",
      "raw PV",
      "proofFailures"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), forbidden)

  test("Defense-9 LLM smoke rejects stronger Defense rephrases"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(defense)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val cases = Vector(
      "Qe4+ is the only move to defend the piece on d4." -> "forbidden_wording",
      "Qe4+ is the best move to defend the piece on d4." -> "forbidden_wording",
      "Qe4+ gives White no counterplay problems." -> "forbidden_wording",
      "Qe4+ keeps the king safe while defending d4." -> "forbidden_wording",
      "Qe4+ stops mate and defends d4." -> "forbidden_wording",
      "Qe4+ refutes the attack on d4." -> "forbidden_wording",
      "The engine says Qe4+ defends d4." -> "engine_mention",
      "Qe4+ defends d4 and Nc6 is next." -> "new_move_or_line",
      "Qe4+ defends d4 with a fork." -> "new_tactic_or_plan",
      "Qe4+ starts a plan to defend d4." -> "new_tactic_or_plan",
      "Qe4+ wins material by defending d4." -> "stronger_claim"
    )

    cases.foreach: (output, violation) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, output)
      assert(check.violations.contains(violation), s"$output should include $violation, got ${check.violations}")

  test("Defense slice closeout keeps proof homes separate and unopened meanings silent"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val threat = ThreatProof.fromBoardFacts(facts, threatLine)
    val defenseProof = DefenseProof.fromBoardFacts(facts, threat, defenseLine)
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(threat.publicClaimAllowed, false)
    assertEquals(defenseProof.publicClaimAllowed, false)
    assertEquals(threat.complete, true)
    assertEquals(defenseProof.complete, true)
    assertEquals(story.scene, Scene.Defense)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof.exists(_.complete), true)
    assertEquals(story.defenseProof.exists(_.complete), true)
    assertEquals(story.proof.kingHeat, 0)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    Vector(
      "only move",
      "best move",
      "no counterplay",
      "king safety",
      "mate defense",
      "refutes",
      "strategy",
      "prophylaxis",
      "winning",
      "decisive",
      "forced"
    ).foreach: forbidden =>
      assert(!rendered.text.toLowerCase.contains(forbidden), forbidden)

  test("Defense slice closeout real game smoke accepts attacked bishop defense"):
    // Fischer-Spassky, World Championship 1972 game 6 after 1.c4 e6 2.Nf3 d5 3.d4 Nf6 4.Nc3 Be7 5.Bg5 O-O 6.e3 h6.
    val facts = BoardFacts.fromFen("rnbq1rk1/ppp1bpp1/4pn1p/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R w KQ - 0 7").toOption.get
    val threatLine = Line(Square('h', 6), Square('g', 5))
    val defenseLine = Line(Square('g', 5), Square('h', 4))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.scene, Scene.Defense)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.threatProof.exists(_.complete), true)
    assertEquals(story.defenseProof.exists(_.complete), true)
    assertEquals(story.defenseProof.flatMap(_.afterDefenseTargetStatus), Some(DefenseTargetStatus.TargetMovedAway))
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    assertEquals(rendered.text, "Bh4 defends the piece on g5.")
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text), NarrationSmokeCheck(true, Vector.empty))

  test("Middlegame interaction hardening lets same-board Material outrank Defense when the move changes material now"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val captureDefense = Line(Square('e', 4), Square('f', 5))
    val defense = SceneDefense.write(facts, threatLine, captureDefense).get
    val material = SceneMaterial.write(facts, captureDefense).get
    val forward = StoryTable.choose(Vector(defense, material))
    val reverse = StoryTable.choose(Vector(material, defense))

    assertEquals(defense.defenseProof.flatMap(_.afterDefenseTargetStatus), Some(DefenseTargetStatus.AttackerCaptured))
    assertEquals(material.captureResult.exists(_.positiveMaterial), true)
    assertEquals(forward.map(v => (v.story.scene, v.role)), reverse.map(v => (v.story.scene, v.role)))
    assertEquals(forward.head.story.scene, Scene.Material)
    assertEquals(forward.head.role, Role.Lead)
    assertEquals(forward(1).story.scene, Scene.Defense)
    assertEquals(forward(1).role, Role.Support)

  test("Middlegame interaction hardening keeps immediate Defense lead separate from speculative future loss"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val speculativeFutureLoss =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = proof(
          boardProof = 100,
          lineProof = 100,
          ownerProof = 100,
          anchorProof = 100,
          routeProof = 100,
          forcing = 100,
          conversionPrize = 100,
          counterplayRisk = 0,
          kingHeat = 0,
          pieceSupport = 100,
          clarity = 100
        ),
        storyProof = StoryProof.fromBoardFacts(facts, defenseLine),
        writer = Some(StoryWriter.SceneDefense)
      )
    val verdicts = StoryTable.choose(Vector(speculativeFutureLoss, defense))

    assertEquals(verdicts.head.story, defense)
    assertEquals(verdicts.head.role, Role.Lead)
    assertEquals(verdicts(1).story, speculativeFutureLoss)
    assertEquals(verdicts(1).role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(verdicts(1)).flatMap(DeterministicRenderer.fromPlan), None)

  test("Middlegame interaction hardening covers DefenseProof move away guard block and capture cases"):
    val movedFacts = BoardFacts.fromFen("rnbq1rk1/ppp1bpp1/4pn1p/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R w KQ - 0 7").toOption.get
    val moved = SceneDefense.write(
      movedFacts,
      Line(Square('h', 6), Square('g', 5)),
      Line(Square('g', 5), Square('h', 4))
    ).get
    val guardedFacts = BoardFacts.fromFen("4k3/8/8/3q4/3R4/8/7B/4K3 w - - 0 1").toOption.get
    val guarded = SceneDefense.write(
      guardedFacts,
      Line(Square('d', 5), Square('d', 4)),
      Line(Square('h', 2), Square('e', 5))
    ).get
    val blockedFacts = BoardFacts.fromFen("3qk3/8/8/8/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val blocked = SceneDefense.write(
      blockedFacts,
      Line(Square('d', 8), Square('d', 4)),
      Line(Square('e', 4), Square('d', 5))
    ).get
    val capturedFacts = BoardFacts.fromFen("4k3/8/8/5n2/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val captured = SceneDefense.write(
      capturedFacts,
      Line(Square('f', 5), Square('d', 4)),
      Line(Square('e', 4), Square('f', 5))
    ).get
    val cases = Vector(
      DefenseTargetStatus.TargetMovedAway -> moved,
      DefenseTargetStatus.TargetGuarded -> guarded,
      DefenseTargetStatus.AttackerLineBlocked -> blocked,
      DefenseTargetStatus.AttackerCaptured -> captured
    )

    cases.foreach: (status, story) =>
      assertEquals(story.defenseProof.flatMap(_.afterDefenseTargetStatus), Some(status), status.toString)
      assertEquals(StoryTable.choose(Vector(story)).head.role, Role.Lead, status.toString)

  test("Middlegame interaction hardening keeps open Story families deterministically ordered"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val material = SceneMaterial.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defense = SceneDefense.write(
      defenseFacts,
      Line(Square('f', 5), Square('d', 4)),
      Line(Square('d', 4), Square('e', 4))
    ).get
    val matrix = Vector(
      "Material vs Defense" -> Vector(material, defense),
      "Hanging vs Defense" -> Vector(hanging, defense),
      "Fork vs Defense" -> Vector(fork, defense),
      "Hanging vs Material" -> Vector(hanging, material),
      "Hanging vs Fork" -> Vector(hanging, fork)
    )

    matrix.foreach: (label, rows) =>
      val forward = StoryTable.choose(rows)
      val reverse = StoryTable.choose(rows.reverse)
      assertEquals(forward.map(v => (v.story.scene, v.story.tactic, v.story.route, v.role)), reverse.map(v => (v.story.scene, v.story.tactic, v.story.route, v.role)), label)
      assertEquals(forward.count(_.role == Role.Lead), 1, label)
      assert(forward.forall(v => Set(Role.Lead, Role.Support, Role.Context, Role.Blocked).contains(v.role)), label)
      forward.filter(_.role != Role.Lead).foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Middlegame interaction hardening applies EngineCheck Supports Caps and Refutes across open rows"):
    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val hanging = TacticHanging.write(materialFacts, materialMove).get

    def check(facts: BoardFacts, story: Story, reply: Line, status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = story.route.map(route => EngineLine(Vector(route))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(12),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val rows = Vector(
      (materialFacts, material, materialMove, SceneMaterial.withEngineCheck),
      (materialFacts, hanging, materialMove, TacticHanging.withEngineCheck),
      (forkFacts, fork, forkMove, TacticFork.withEngineCheck),
      (defenseFacts, defense, defenseThreat, SceneDefense.withEngineCheck)
    )

    rows.foreach: (facts, story, reply, attach) =>
      val supports = attach(story, check(facts, story, reply, EngineCheckStatus.Supports)).get
      val caps = attach(story, check(facts, story, reply, EngineCheckStatus.Caps)).get
      val refutes = attach(story, check(facts, story, reply, EngineCheckStatus.Supports, before = 200, after = 0)).get
      val supportsVerdict = StoryTable.choose(Vector(supports)).head
      val capsVerdict = StoryTable.choose(Vector(caps)).head
      val refutesVerdict = StoryTable.choose(Vector(refutes)).head

      assertEquals(supportsVerdict.role, Role.Lead, story.toString)
      assertEquals(supportsVerdict.engineStrengthLimited, false, story.toString)
      assertEquals(capsVerdict.role, Role.Lead, story.toString)
      assertEquals(capsVerdict.engineStrengthLimited, true, story.toString)
      assertEquals(refutesVerdict.role, Role.Blocked, story.toString)

  test("MIH-1 fixture map covers every allowed middlegame hardening category"):
    final case class MihFixture(
        category: String,
        fen: String,
        sideToMove: Side,
        legalCandidateLines: Vector[Line],
        expectedOpenRows: Vector[String],
        expectedBlockedRows: Vector[String],
        expectedRoles: Vector[(String, Role)],
        expectedSelectedVerdict: String,
        forbiddenClaims: Vector[String],
        rows: BoardFacts => Vector[(String, Story)]
    )

    val hanging = "Tactic.Hanging"
    val fork = "Tactic.Fork"
    val material = "Scene.Material"
    val defense = "Scene.Defense"
    val materialSupports = "Scene.Material#EngineCheck.Supports"
    val materialCaps = "Scene.Material#EngineCheck.Caps"
    val materialRefutes = "Scene.Material#EngineCheck.Refutes"

    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    def checkedMaterial(facts: BoardFacts, story: Story, status: EngineCheckStatus, before: Int, after: Int): Story =
      val route = story.route.get
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(route))),
        replyLine = Some(EngineLine(Vector(Line(Square('g', 8), Square('f', 8))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      SceneMaterial.withEngineCheck(story, check).get

    val queenTakesH6 = Line(Square('d', 2), Square('h', 6))
    val knightFork = Line(Square('f', 3), Square('d', 4))
    val materialQueenTakesE5 = Line(Square('d', 4), Square('e', 5))
    val pawnThreat = Line(Square('h', 6), Square('g', 5))
    val bishopStepsAway = Line(Square('g', 5), Square('h', 4))
    val bishopTakesAttacker = Line(Square('e', 4), Square('f', 5))
    val knightThreatensRook = Line(Square('f', 5), Square('d', 4))

    val sharedHangingForkFen = "6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1"
    val materialDefenseFen = "6k1/8/7p/4n1B1/3Q4/8/8/6K1 w - - 0 1"
    val forkDefenseFen = "7k/8/7p/1q3rB1/8/5N2/8/7K w - - 0 1"
    val sameBoardMaterialDefenseFen = "4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1"

    val defaultForbiddenClaims =
      Vector(
        "winning",
        "decisive",
        "forced",
        "conversion",
        "new_story_family",
        "new_proof_home",
        "new_renderer_wording",
        "public_route_200",
        "production_api",
        "public_llm_narration"
      )

    val fixtureMap = Vector(
      MihFixture(
        category = "Hanging vs Material",
        fen = sharedHangingForkFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(queenTakesH6),
        expectedOpenRows = Vector(hanging, material),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(hanging -> Role.Lead, material -> Role.Support),
        expectedSelectedVerdict = hanging,
        forbiddenClaims = defaultForbiddenClaims,
        rows = facts =>
          Vector(
            hanging -> TacticHanging.write(facts, queenTakesH6).get,
            material -> SceneMaterial.write(facts, queenTakesH6).get.copy(proof = score(99))
          )
      ),
      MihFixture(
        category = "Hanging vs Fork",
        fen = sharedHangingForkFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(queenTakesH6, knightFork),
        expectedOpenRows = Vector(hanging, fork),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(hanging -> Role.Lead, fork -> Role.Support),
        expectedSelectedVerdict = hanging,
        forbiddenClaims = defaultForbiddenClaims :+ "wins_queen",
        rows = facts =>
          Vector(
            hanging -> TacticHanging.write(facts, queenTakesH6).get.copy(proof = score(99)),
            fork -> TacticFork.write(facts, Some(knightFork), Some(Square('b', 5)), Some(Square('f', 5))).get
          )
      ),
      MihFixture(
        category = "Material vs Defense",
        fen = materialDefenseFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(materialQueenTakesE5, pawnThreat, bishopStepsAway),
        expectedOpenRows = Vector(defense, material),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(material -> Role.Lead, defense -> Role.Support),
        expectedSelectedVerdict = material,
        forbiddenClaims = defaultForbiddenClaims,
        rows = facts =>
          Vector(
            material -> SceneMaterial.write(facts, materialQueenTakesE5).get,
            defense -> SceneDefense.write(facts, pawnThreat, bishopStepsAway).get
          )
      ),
      MihFixture(
        category = "Fork vs Defense",
        fen = forkDefenseFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(knightFork, pawnThreat, bishopStepsAway),
        expectedOpenRows = Vector(defense, fork),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(defense -> Role.Lead, fork -> Role.Support),
        expectedSelectedVerdict = defense,
        forbiddenClaims = defaultForbiddenClaims :+ "wins_queen",
        rows = facts =>
          Vector(
            fork -> TacticFork.write(facts, Some(knightFork), Some(Square('b', 5)), Some(Square('f', 5))).get,
            defense -> SceneDefense.write(facts, pawnThreat, bishopStepsAway).get
          )
      ),
      MihFixture(
        category = "Material vs Defense on same board",
        fen = sameBoardMaterialDefenseFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(knightThreatensRook, bishopTakesAttacker),
        expectedOpenRows = Vector(material, defense),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(material -> Role.Lead, defense -> Role.Support),
        expectedSelectedVerdict = material,
        forbiddenClaims = defaultForbiddenClaims,
        rows = facts =>
          Vector(
            defense -> SceneDefense.write(facts, knightThreatensRook, bishopTakesAttacker).get,
            material -> SceneMaterial.write(facts, bishopTakesAttacker).get
          )
      ),
      MihFixture(
        category = "EngineCheck Supports/Caps/Refutes over existing rows",
        fen = materialDefenseFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(materialQueenTakesE5),
        expectedOpenRows = Vector(materialSupports, materialCaps),
        expectedBlockedRows = Vector(materialRefutes),
        expectedRoles = Vector(
          materialSupports -> Role.Lead,
          materialCaps -> Role.Support,
          materialRefutes -> Role.Blocked
        ),
        expectedSelectedVerdict = materialSupports,
        forbiddenClaims = defaultForbiddenClaims,
        rows = facts =>
          val base = SceneMaterial.write(facts, materialQueenTakesE5).get
          Vector(
            materialSupports ->
              checkedMaterial(facts, base, EngineCheckStatus.Supports, before = 20, after = 80).copy(proof = score(99)),
            materialCaps ->
              checkedMaterial(facts, base, EngineCheckStatus.Caps, before = 20, after = 80).copy(proof = score(98)),
            materialRefutes ->
              checkedMaterial(facts, base, EngineCheckStatus.Refutes, before = 20, after = 80).copy(proof = score(100))
          )
      )
    )

    assertEquals(
      fixtureMap.map(_.category),
      Vector(
        "Hanging vs Material",
        "Hanging vs Fork",
        "Material vs Defense",
        "Fork vs Defense",
        "Material vs Defense on same board",
        "EngineCheck Supports/Caps/Refutes over existing rows"
      )
    )

    val allowedRows: Set[(Scene, Option[Tactic])] =
      Set(
        (Scene.Tactic, Some(Tactic.Hanging)),
        (Scene.Tactic, Some(Tactic.Fork)),
        (Scene.Material, None),
        (Scene.Defense, None)
      )
    val forbiddenExpectationTerms = Vector("pressure", "initiative", "best move", "only move", "proofFailures")

    fixtureMap.foreach: fixture =>
      val facts = BoardFacts.fromFen(fixture.fen).toOption.get
      val rows = fixture.rows(facts)
      val verdicts = StoryTable.choose(rows.map(_._2))
      val rowIdByStory = rows.map((id, story) => story -> id).toMap
      val roleByRow = verdicts.map(verdict => rowIdByStory(verdict.story) -> verdict.role)
      val openRows = roleByRow.collect { case (id, role) if role != Role.Blocked => id }
      val blockedRows = roleByRow.collect { case (id, Role.Blocked) => id }
      val fixtureText =
        (Vector(fixture.category) ++ fixture.expectedOpenRows ++ fixture.expectedBlockedRows ++
          fixture.expectedRoles.map(_._1) ++ Vector(fixture.expectedSelectedVerdict) ++ fixture.forbiddenClaims)
          .mkString(" ")
          .toLowerCase

      assertEquals(facts.sideToMove, fixture.sideToMove, fixture.category)
      fixture.legalCandidateLines.foreach: line =>
        assert(
          facts.sideLegal.lines.contains(line) || facts.rivalLegal.lines.contains(line),
          s"${fixture.category} candidate line must be legal: $line"
        )
      rows.foreach: (_, story) =>
        assert(
          allowedRows.contains((story.scene, story.tactic)),
          s"${fixture.category} must not imply unopened Story family: ${story.scene}/${story.tactic}"
        )
        assertEquals(story.proofFailures, Vector.empty, fixture.category)
      assertEquals(openRows.toSet, fixture.expectedOpenRows.toSet, fixture.category)
      assertEquals(blockedRows.toSet, fixture.expectedBlockedRows.toSet, fixture.category)
      assertEquals(roleByRow, fixture.expectedRoles, fixture.category)
      assertEquals(rowIdByStory(verdicts.head.story), fixture.expectedSelectedVerdict, fixture.category)
      assertEquals(verdicts.head.role, Role.Lead, fixture.category)
      assert(fixture.forbiddenClaims.nonEmpty, fixture.category)
      forbiddenExpectationTerms.foreach: term =>
        assert(!fixtureText.contains(term), s"${fixture.category} must not expect forbidden term: $term")

      val selectedPlan = ExplanationPlan.fromSelected(verdicts.head).get
      val rendered = DeterministicRenderer.fromPlan(selectedPlan).get
      val mockText = LlmNarrationSmoke.mockNarrate(selectedPlan, rendered).get
      assert(LlmNarrationSmoke.codexCliPrompt(selectedPlan, rendered).nonEmpty, fixture.category)
      val publicSmokeText = Vector(rendered.text, mockText).mkString(" ").toLowerCase
      fixture.forbiddenClaims.foreach: claim =>
        val normalizedClaim = claim.replace('_', ' ').toLowerCase
        assert(
          !publicSmokeText.contains(normalizedClaim),
          s"${fixture.category} output must not contain forbidden claim: $claim"
        )
      verdicts.filterNot(_ == verdicts.head).foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, fixture.category)

  test("MIH-2 Role Stability keeps existing rows deterministic without duplicate Lead"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    def checkedMaterial(facts: BoardFacts, story: Story, status: EngineCheckStatus): Story =
      val route = story.route.get
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(route))),
        replyLine = Some(EngineLine(Vector(Line(Square('g', 8), Square('f', 8))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      SceneMaterial.withEngineCheck(story, check).get

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def roleById(rows: Vector[(String, Story)], verdicts: Vector[Verdict]): Map[String, Role] =
      verdicts.map(verdict => rowId(rows, verdict.story) -> verdict.role).toMap

    def roleShape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim)
        )

    val hangingMaterialFacts =
      BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get
    val queenTakesH6 = Line(Square('d', 2), Square('h', 6))
    val knightFork = Line(Square('f', 3), Square('d', 4))
    val hanging = TacticHanging.write(hangingMaterialFacts, queenTakesH6).get.copy(proof = strongProof(96))
    val materialSameRoute = SceneMaterial.write(hangingMaterialFacts, queenTakesH6).get.copy(proof = strongProof(99))
    val fork = TacticFork
      .write(hangingMaterialFacts, Some(knightFork), Some(Square('b', 5)), Some(Square('f', 5)))
      .get
      .copy(proof = strongProof(94))

    val refutedMaterial =
      checkedMaterial(hangingMaterialFacts, materialSameRoute, EngineCheckStatus.Refutes)
        .copy(proof = strongProof(100))
    val incompleteMaterial = materialSameRoute.copy(storyProof = StoryProof.empty, proof = strongProof(100))
    val cappedMaterial =
      checkedMaterial(hangingMaterialFacts, materialSameRoute, EngineCheckStatus.Caps).copy(proof = strongProof(98))

    val defenseFacts =
      BoardFacts.fromFen("4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1").toOption.get
    val knightThreatensRook = Line(Square('f', 5), Square('d', 4))
    val bishopTakesAttacker = Line(Square('e', 4), Square('f', 5))
    val validDefense = SceneDefense.write(defenseFacts, knightThreatensRook, bishopTakesAttacker).get
    val defenseWithoutThreat = validDefense.copy(threatProof = None, proof = strongProof(100))
    val forkWithoutTwoTargetProof = fork.copy(multiTargetProof = None, proof = strongProof(100))

    val roleStabilityRows =
      Vector(
        "Tactic.Hanging" -> hanging,
        "Scene.Material.sameRoute" -> materialSameRoute,
        "Tactic.Fork" -> fork,
        "Scene.Material.refuted" -> refutedMaterial,
        "Scene.Material.incomplete" -> incompleteMaterial,
        "Scene.Defense.noThreat" -> defenseWithoutThreat
      )
    val forward = StoryTable.choose(roleStabilityRows.map(_._2))
    val reverse = StoryTable.choose(roleStabilityRows.reverse.map(_._2))
    val shuffled =
      StoryTable.choose(
        Vector(
          roleStabilityRows(3),
          roleStabilityRows(1),
          roleStabilityRows(5),
          roleStabilityRows(0),
          roleStabilityRows(2),
          roleStabilityRows(4)
        ).map(_._2)
      )

    assertEquals(roleShape(roleStabilityRows, forward), roleShape(roleStabilityRows, reverse))
    assertEquals(roleShape(roleStabilityRows, forward), roleShape(roleStabilityRows, shuffled))
    assertEquals(rowId(roleStabilityRows, forward.head.story), "Tactic.Hanging")
    assertEquals(rowId(roleStabilityRows, reverse.head.story), "Tactic.Hanging")
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(roleById(roleStabilityRows, forward)("Scene.Material.sameRoute"), Role.Support)
    assertEquals(roleById(roleStabilityRows, forward)("Scene.Material.incomplete"), Role.Blocked)
    assertEquals(roleById(roleStabilityRows, forward)("Scene.Material.refuted"), Role.Blocked)
    assertEquals(roleById(roleStabilityRows, forward)("Scene.Defense.noThreat"), Role.Blocked)

    val sameRouteVerdicts = StoryTable.choose(Vector(hanging, materialSameRoute))
    assertEquals(sameRouteVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(sameRouteVerdicts.find(_.story == hanging).map(_.role), Some(Role.Lead))
    assertEquals(sameRouteVerdicts.find(_.story == materialSameRoute).map(_.role), Some(Role.Support))

    val defenseWithoutThreatVerdict = StoryTable.choose(Vector(defenseWithoutThreat)).head
    assertEquals(defenseWithoutThreatVerdict.role, Role.Blocked)
    assertEquals(defenseWithoutThreatVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(defenseWithoutThreatVerdict).flatMap(_.allowedClaim), None)
    assertEquals(ExplanationPlan.fromSelected(defenseWithoutThreatVerdict).flatMap(DeterministicRenderer.fromPlan), None)

    val forkWithoutTwoTargetVerdict = StoryTable.choose(Vector(forkWithoutTwoTargetProof)).head
    assertEquals(forkWithoutTwoTargetVerdict.role, Role.Blocked)
    assertEquals(forkWithoutTwoTargetVerdict.leadAllowed, false)
    val forkWithoutTwoTargetPlan = ExplanationPlan.fromSelected(forkWithoutTwoTargetVerdict).get
    assertEquals(forkWithoutTwoTargetPlan.allowedClaim, None)
    assert(forkWithoutTwoTargetPlan.allowedClaim.forall(_ != ExplanationClaim.MaterialBalanceChanges))
    assertEquals(DeterministicRenderer.fromPlan(forkWithoutTwoTargetPlan), None)

    val cappedVerdict = StoryTable.choose(Vector(cappedMaterial)).head
    assertEquals(cappedVerdict.role, Role.Lead)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get
    assertEquals(cappedPlan.allowedClaim, None)
    assertEquals(DeterministicRenderer.fromPlan(cappedPlan), None)

  test("MIH-3 Material vs Defense collision separates material change now from prevented loss"):
    def publicSmokeText(plan: ExplanationPlan, rendered: RenderedLine): String =
      Vector(rendered.text, LlmNarrationSmoke.mockNarrate(plan, rendered).get)
        .mkString(" ")
        .toLowerCase

    def assertNoForbidden(text: String, forbidden: Vector[String], label: String): Unit =
      forbidden.foreach: phrase =>
        assert(!text.contains(phrase), s"$label must not contain forbidden phrase: $phrase")

    val collisionFacts =
      BoardFacts.fromFen("6k1/8/7p/4n1B1/3Q4/8/8/6K1 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val threatLine = Line(Square('h', 6), Square('g', 5))
    val defenseMove = Line(Square('g', 5), Square('h', 4))
    val material = SceneMaterial.write(collisionFacts, materialMove).get
    val defense = SceneDefense.write(collisionFacts, threatLine, defenseMove).get
    val forward = StoryTable.choose(Vector(defense, material))
    val reverse = StoryTable.choose(Vector(material, defense))

    assertEquals(material.captureResult.exists(_.positiveMaterial), true)
    assertEquals(defense.threatProof.exists(_.complete), true)
    assertEquals(defense.defenseProof.exists(_.complete), true)
    assertEquals(defense.defenseProof.flatMap(_.afterDefenseTargetStatus), Some(DefenseTargetStatus.TargetMovedAway))
    assertEquals(
      forward.map(verdict => (verdict.story.scene, verdict.story.route, verdict.role)),
      reverse.map(verdict => (verdict.story.scene, verdict.story.route, verdict.role))
    )
    assertEquals(forward.head.story, material)
    assertEquals(forward.head.role, Role.Lead)
    assertEquals(forward.find(_.story == defense).map(_.role), Some(Role.Support))
    assertEquals(ExplanationPlan.fromSelected(forward.find(_.story == defense).get).flatMap(DeterministicRenderer.fromPlan), None)

    val materialPlan = ExplanationPlan.fromSelected(forward.head).get
    val materialRendered = DeterministicRenderer.fromPlan(materialPlan).get
    assertEquals(materialPlan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    assertNoForbidden(publicSmokeText(materialPlan, materialRendered), Vector("winning", "conversion", "decisive"), "Material")
    assertEquals(
      LlmNarrationSmoke.check(materialPlan, materialRendered, "Qxe5 is winning and decisive conversion.").accepted,
      false
    )

    val defenseOnlyVerdict = StoryTable.choose(Vector(defense)).head
    val defensePlan = ExplanationPlan.fromSelected(defenseOnlyVerdict).get
    val defenseRendered = DeterministicRenderer.fromPlan(defensePlan).get
    assertEquals(defenseOnlyVerdict.role, Role.Lead)
    assertEquals(defensePlan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    assertNoForbidden(publicSmokeText(defensePlan, defenseRendered), Vector("best defense", "only move", "refutes attack"), "Defense")
    assertEquals(
      LlmNarrationSmoke.check(defensePlan, defenseRendered, "Bh4 is the only move and refutes the attack.").accepted,
      false
    )

    val speculativeFutureLoss = defense.copy(threatProof = None, defenseProof = None)
    val speculativeVerdict = StoryTable.choose(Vector(speculativeFutureLoss)).head
    assertEquals(speculativeVerdict.role, Role.Blocked)
    assertEquals(speculativeVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(speculativeVerdict).flatMap(_.allowedClaim), None)
    assertEquals(ExplanationPlan.fromSelected(speculativeVerdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("MIH-4 EngineCheck interaction reuses status boundaries without public engine meaning"):
    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    def publicSmokeText(plan: ExplanationPlan, rendered: RenderedLine): String =
      Vector(rendered.text, LlmNarrationSmoke.mockNarrate(plan, rendered).get)
        .mkString(" ")
        .toLowerCase

    def assertNoEngineExpression(text: String, label: String): Unit =
      Vector("engine", "eval", "pv", "engine says", "best move", "1234", "1478", "d7").foreach: phrase =>
        assert(!text.contains(phrase), s"$label must not expose engine phrase or raw evidence: $phrase")

    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, materialMove).get

    def check(story: Story, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get, Line(Square('e', 8), Square('d', 7))))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1478)),
        depth = Some(19),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val baseVerdict = StoryTable.choose(Vector(material)).head
    val supports = SceneMaterial.withEngineCheck(material, check(material, EngineCheckStatus.Supports)).get
    val caps = SceneMaterial.withEngineCheck(material, check(material, EngineCheckStatus.Caps)).get
    val refutes = SceneMaterial.withEngineCheck(material, check(material, EngineCheckStatus.Refutes)).get
    val unknown = SceneMaterial.withEngineCheck(material, check(material, EngineCheckStatus.Unknown)).get

    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head

    val basePlan = ExplanationPlan.fromSelected(baseVerdict).get
    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    val capsPlan = ExplanationPlan.fromSelected(capsVerdict).get
    val refutesPlan = ExplanationPlan.fromSelected(refutesVerdict).get
    val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get

    Vector(supportsVerdict, capsVerdict, refutesVerdict, unknownVerdict).foreach: verdict =>
      assert(!verdict.values.contains(1234.0), "evalBefore must stay out of public Verdict values")
      assert(!verdict.values.contains(1478.0), "evalAfter must stay out of public Verdict values")

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(supportsPlan.allowedClaim, basePlan.allowedClaim)
    assertEquals(supportsPlan.relations.exists(_.key.contains("engine")), false)
    val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
    assertNoEngineExpression(publicSmokeText(supportsPlan, supportsRendered), "Supports")
    val supportsPrompt = LlmNarrationSmoke.codexCliPrompt(supportsPlan, supportsRendered).get
    Vector("1234", "1478", "d7").foreach: rawEvidence =>
      assert(!supportsPrompt.toLowerCase.contains(rawEvidence), s"Supports prompt must not expose raw evidence: $rawEvidence")
    assertEquals(
      LlmNarrationSmoke.check(supportsPlan, supportsRendered, "The engine says Qxe5 is the best move at +14.78.").accepted,
      false
    )

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(capsPlan.allowedClaim, None)
    assert(capsPlan.forbiddenWording.contains(ForbiddenWording.StrongWording))
    assertEquals(DeterministicRenderer.fromPlan(capsPlan), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesPlan.allowedClaim, None)
    assertEquals(refutesPlan.debugOnly, true)
    assert(refutesPlan.relations.contains(ExplanationRelation.BlockedByEngineRefute))
    assertEquals(DeterministicRenderer.fromPlan(refutesPlan), None)

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(unknownPlan.allowedClaim, basePlan.allowedClaim)
    val unknownRendered = DeterministicRenderer.fromPlan(unknownPlan).get
    assertNoEngineExpression(publicSmokeText(unknownPlan, unknownRendered), "Unknown")
    assertEquals(
      LlmNarrationSmoke.check(unknownPlan, unknownRendered, "The engine says Qxe5 is the best move at +14.78.").accepted,
      false
    )

    val orderingFacts = facts
    val highProofLowEval = material.copy(proof = score(96))
    val lowProofHighEval = material.copy(proof = score(72))

    def orderingCheck(story: Story, after: Int): EngineCheck =
      EngineCheck.fromStory(
        facts = orderingFacts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(19),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    val lowEvalRow = SceneMaterial.withEngineCheck(highProofLowEval, orderingCheck(highProofLowEval, 1)).get
    val highEvalRow = SceneMaterial.withEngineCheck(lowProofHighEval, orderingCheck(lowProofHighEval, 3000)).get
    val ordered = StoryTable.choose(Vector(highEvalRow, lowEvalRow))
    assertEquals(ordered.head.story, lowEvalRow)
    assertEquals(ordered.head.role, Role.Lead)

  test("MIH-5 Negative Corpus keeps close false positives silent on complex boards"):
    def assertNoPublicOutput(label: String, story: Story): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assert(verdict.role != Role.Lead, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    val attackedButRecapturedFacts =
      BoardFacts.fromFen("6k1/ppp2ppp/8/3q4/4Qn2/2N2B2/PPP2PPP/6K1 w - - 0 1").toOption.get
    val defendedQueenCapture = Line(Square('e', 4), Square('d', 5))
    val attackedButRecaptured = TacticHanging.write(attackedButRecapturedFacts, defendedQueenCapture)
    val equalMaterialResult = CaptureResult.fromBoardFacts(attackedButRecapturedFacts, defendedQueenCapture)

    val materialLostAfterReplyFacts =
      BoardFacts.fromFen("6k1/ppp2ppp/8/3p4/4Qn2/2N2B2/PPP2PPP/6K1 w - - 0 1").toOption.get
    val queenTakesDefendedPawn = Line(Square('e', 4), Square('d', 5))
    val lostAfterReply = SceneMaterial.write(materialLostAfterReplyFacts, queenTakesDefendedPawn)
    val lostMaterialResult = CaptureResult.fromBoardFacts(materialLostAfterReplyFacts, queenTakesDefendedPawn)

    val forkLookingFacts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get
    val forkLookingMove = Line(Square('f', 3), Square('d', 4))
    val oneRealTarget = TacticFork.write(forkLookingFacts, Some(forkLookingMove), Some(Square('b', 5)), None)

    val defenseFacts =
      BoardFacts.fromFen("4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1").toOption.get
    val knightThreatensRook = Line(Square('f', 5), Square('d', 4))
    val bishopTakesAttacker = Line(Square('e', 4), Square('f', 5))
    val noCompleteThreat = SceneDefense.write(defenseFacts, Line(Square('f', 5), Square('h', 4)), bishopTakesAttacker)
    val stillLeavesLoss = SceneDefense.write(defenseFacts, knightThreatensRook, Line(Square('d', 4), Square('h', 4)))

    val wrongTargetFacts = BoardFacts.fromFen("4k3/8/8/3q4/3R4/8/B6B/4K3 w - - 0 1").toOption.get
    val wrongTargetGuard =
      SceneDefense.write(wrongTargetFacts, Line(Square('d', 5), Square('d', 4)), Line(Square('a', 2), Square('b', 3)))

    Vector(
      "attacked-looking piece but adequate recapture exists" -> attackedButRecaptured,
      "fork-looking move but only one real target" -> oneRealTarget,
      "material-looking capture but equal/lost after immediate reply" -> lostAfterReply,
      "defense-looking move but no complete ThreatProof" -> noCompleteThreat,
      "defense move guards wrong target" -> wrongTargetGuard,
      "defense move still leaves material loss" -> stillLeavesLoss
    ).foreach: (label, maybeStory) =>
      assertEquals(maybeStory, None, label)

    assertEquals(equalMaterialResult.materialResult, Some(0))
    assertEquals(lostMaterialResult.materialResult.exists(_ < 0), true)

    val plausibleMaterial = SceneMaterial.write(defenseFacts, bishopTakesAttacker).get
    val refutingCheck = EngineCheck.fromStory(
      facts = defenseFacts,
      story = Some(plausibleMaterial),
      engineLine = Some(EngineLine(Vector(bishopTakesAttacker))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
      evalBefore = Some(EngineEval(200)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val refutedPlausible = SceneMaterial.withEngineCheck(plausibleMaterial, refutingCheck).get
    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    assertNoPublicOutput("engine refutes otherwise plausible Story", refutedPlausible)

    val sameBoardProofMissing =
      minimalBoardFacts(
        sideLegal = readyMoves(line = bishopTakesAttacker, captureCount = 1),
        rivalLegal = readyMoves(line = knightThreatensRook, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Bishop, Square('e', 4)),
          Piece(Side.Black, Man.Knight, Square('f', 5))
        )
      )
    assertEquals(SceneMaterial.write(sameBoardProofMissing, bishopTakesAttacker), None, "same-board proof missing")

    val routeMismatch = plausibleMaterial.copy(route = Some(Line(Square('e', 4), Square('h', 7))))
    assertNoPublicOutput("route mismatch", routeMismatch)

    val wrongEngineLine = EngineCheck.fromStory(
      facts = defenseFacts,
      story = Some(plausibleMaterial),
      engineLine = Some(EngineLine(Vector(Line(Square('e', 4), Square('h', 7))))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val staleEngineLine = EngineCheck.fromStory(
      facts = defenseFacts,
      story = Some(plausibleMaterial),
      engineLine = Some(EngineLine(Vector(bishopTakesAttacker))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(3),
      requestedStatus = EngineCheckStatus.Supports
    )

    Vector("wrong engine line" -> wrongEngineLine, "stale engine line" -> staleEngineLine).foreach: (label, check) =>
      assertEquals(check.status, EngineCheckStatus.Unknown, label)
      assertEquals(SceneMaterial.withEngineCheck(plausibleMaterial, check), None, label)
      assertEquals(StoryTable.choose(Vector(plausibleMaterial)).exists(_.story.engineCheck.nonEmpty), false, label)

  test("MIH-6 Downstream Boundary Smoke passes only selected Lead Verdict through existing downstream stages"):
    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val materialFacts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get

    def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = materialFacts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val hangingMove = Line(Square('d', 2), Square('h', 6))
    val hanging = TacticHanging.write(materialFacts, hangingMove).get.copy(proof = score(99))
    val overlappingMaterial = SceneMaterial.write(materialFacts, hangingMove).get.copy(proof = score(98))
    val lowStrengthMaterial = overlappingMaterial.copy(proof = score(60))
    val cappedMaterial =
      SceneMaterial.withEngineCheck(overlappingMaterial, engineCheck(overlappingMaterial, EngineCheckStatus.Caps)).get
    val refutedMaterial =
      SceneMaterial.withEngineCheck(overlappingMaterial, engineCheck(overlappingMaterial, EngineCheckStatus.Refutes)).get
    val incompleteMaterial = overlappingMaterial.copy(storyProof = StoryProof.empty)

    val leadVerdict = StoryTable.choose(Vector(hanging)).head
    val supportVerdict = StoryTable.choose(Vector(hanging, overlappingMaterial)).find(_.story == overlappingMaterial).get
    val contextVerdict = StoryTable.choose(Vector(lowStrengthMaterial)).head
    val blockedVerdict = StoryTable.choose(Vector(incompleteMaterial)).head
    val cappedVerdict = StoryTable.choose(Vector(cappedMaterial)).head
    val refutedVerdict = StoryTable.choose(Vector(refutedMaterial)).head
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)
      plan.foreach: relationPlan =>
        assertEquals(LlmNarrationSmoke.mockNarrate(relationPlan, Some(rendered)), None, label)
        assertEquals(LlmNarrationSmoke.codexCliPrompt(relationPlan, rendered), None, label)

    assertEquals(leadVerdict.selected, true)
    assertEquals(leadVerdict.role, Role.Lead)
    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(LlmNarrationSmoke.mockNarrate(leadPlan, rendered), Some(rendered.text))
    assertEquals(LlmNarrationSmoke.check(leadPlan, rendered, rendered.text), NarrationSmokeCheck(true, Vector.empty))
    assertEquals(ExplanationPlan.fromSelected(leadVerdict.copy(selected = false)), None)

    Vector(
      "renderedText:",
      "claimKey:",
      "strength:",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Codex CLI prompt must include allowed field: $required")

    Vector(
      "ExplanationPlan",
      "Verdict",
      "Story",
      "Proof",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "source row",
      "route:",
      "target:",
      "anchor:",
      "role:",
      "scene:",
      "tactic:"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"Codex CLI prompt must not include raw input: $forbidden")

    val explanationMethods = ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    assertEquals(explanationMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)), Vector(Vector("Verdict")))
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    assertEquals(rendererMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)), Vector(Vector("ExplanationPlan")))
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val llmMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    Vector("fromStory", "fromProof", "fromEngineCheck", "fromVerdict", "fromBoardFacts").foreach: forbiddenMethod =>
      assert(!rendererMethodNames.contains(forbiddenMethod), s"renderer must not expose $forbiddenMethod")
      assert(!llmMethodNames.contains(forbiddenMethod), s"LLM smoke must not expose $forbiddenMethod")

    assertEquals(supportVerdict.role, Role.Support)
    assertEquals(contextVerdict.role, Role.Context)
    assertEquals(blockedVerdict.role, Role.Blocked)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(refutedVerdict.role, Role.Blocked)

    Vector(
      "Support row" -> supportVerdict,
      "Context row" -> contextVerdict,
      "Blocked row" -> blockedVerdict,
      "Capped row" -> cappedVerdict,
      "Refuted row" -> refutedVerdict
    ).foreach: (label, verdict) =>
      assertNoStandaloneText(label, verdict)

  test("MIH-7 Diagnostics Boundary keeps diagnostics out of public meaning"):
    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val facts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get

    def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val route = Line(Square('d', 2), Square('h', 6))
    val hanging = TacticHanging.write(facts, route).get.copy(proof = score(99))
    val material = SceneMaterial.write(facts, route).get.copy(proof = score(98))
    val cappedMaterial =
      SceneMaterial.withEngineCheck(material, engineCheck(material, EngineCheckStatus.Caps)).get
    val refutedMaterial =
      SceneMaterial.withEngineCheck(material, engineCheck(material, EngineCheckStatus.Refutes)).get

    val leadVerdict = StoryTable.choose(Vector(hanging)).head
    val supportVerdict = StoryTable.choose(Vector(hanging, material)).find(_.story == material).get
    val cappedVerdict = StoryTable.choose(Vector(cappedMaterial)).head
    val refutedVerdict = StoryTable.choose(Vector(refutedMaterial)).head
    val diagnosticTerms =
      Vector(
        "diagnostic proof failure should never render",
        "raw engine text should never render",
        "source row payload should never render",
        "proofFailures",
        "EngineCheck"
      )
    val diagnosticVerdict = supportVerdict.copy(
      proofFailures = Vector(BoardFacts.MissingEvidence("MIH-7 diagnostic", diagnosticTerms)),
      engineCheckStatus = Some(EngineCheckStatus.Refutes),
      engineStrengthLimited = true
    )
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get
    val supportPlan = ExplanationPlan.fromSelected(supportVerdict).get
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get
    val refutedPlan = ExplanationPlan.fromSelected(refutedVerdict).get
    val diagnosticPlan = ExplanationPlan.fromSelected(diagnosticVerdict).get

    assert(diagnosticVerdict.proofFailures.nonEmpty)
    assertEquals(diagnosticVerdict.values, supportVerdict.values)
    diagnosticTerms.foreach: term =>
      assert(!diagnosticVerdict.values.mkString(" ").contains(term), s"Verdict.values leaked diagnostic term: $term")
      assert(!diagnosticPlan.toString.contains(term), s"ExplanationPlan leaked diagnostic term: $term")
      assert(!rendered.text.contains(term), s"RenderedLine leaked diagnostic term: $term")
      assert(!prompt.contains(term), s"LLM smoke prompt leaked diagnostic term: $term")

    val explanationSurface =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet ++
        ExplanationPlan.getClass.getDeclaredMethods.map(_.getName).toSet
    Vector("sourceRow", "source_row", "proofFailures", "engineCheck", "fromStory", "fromProof", "fromEngineCheck").foreach:
      forbiddenSurface =>
        assert(
          !explanationSurface.exists(_.toLowerCase.contains(forbiddenSurface.toLowerCase)),
          s"ExplanationPlan must not expose diagnostic/source-row surface: $forbiddenSurface"
        )

    assertEquals(diagnosticPlan.allowedClaim, None)
    assertEquals(diagnosticPlan.supportContextLinks, Vector.empty)
    assert(diagnosticPlan.relations.contains(ExplanationRelation.CappedSameStory))
    assert(refutedPlan.relations.contains(ExplanationRelation.BlockedByEngineRefute))

    Vector(
      "Support relation" -> supportPlan,
      "Capped relation" -> cappedPlan,
      "Refuted relation" -> refutedPlan,
      "Injected diagnostic relation" -> diagnosticPlan
    ).foreach: (label, plan) =>
      assert(plan.relations.nonEmpty, label)
      assertEquals(DeterministicRenderer.fromPlan(plan), None, label)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, Some(rendered)), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, rendered), None, label)
      plan.relations.map(_.key).foreach: relationKey =>
        assert(!rendered.text.contains(relationKey), s"$label relation key leaked to renderer wording")
        assert(!prompt.contains(relationKey), s"$label relation key leaked to LLM prompt")

  test("MIH Closeout Hard Cleanup keeps ownership separated without new runtime authority"):
    val hangingMaterialFacts =
      BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingMaterialFacts, capture).get
    val material = SceneMaterial.write(hangingMaterialFacts, capture).get

    val defenseFacts =
      BoardFacts.fromFen("4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1").toOption.get
    val threat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('e', 4), Square('f', 5))
    val defense = SceneDefense.write(defenseFacts, threat, defenseMove).get

    def selectedPlan(story: Story): ExplanationPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    val hangingPlan = selectedPlan(hanging)
    val materialPlan = selectedPlan(material)
    val defensePlan = selectedPlan(defense)

    assertEquals(
      StoryWriter.values.toVector,
      Vector(
        StoryWriter.TacticHanging,
        StoryWriter.TacticFork,
        StoryWriter.SceneMaterial,
        StoryWriter.SceneDefense,
        StoryWriter.TacticDiscoveredAttack,
        StoryWriter.TacticPin,
        StoryWriter.TacticRemoveGuard,
        StoryWriter.TacticSkewer,
        StoryWriter.ScenePawnAdvance,
        StoryWriter.ScenePawnStop,
        StoryWriter.ScenePromotionThreat
      )
    )
    assertEquals(ExplanationStrength.values.toVector, Vector(ExplanationStrength.Bounded))
    assertEquals(
      ExplanationRelation.values.toVector,
      Vector(
        ExplanationRelation.SameFamilyLowerRank,
        ExplanationRelation.AlternativeHangingCandidate,
        ExplanationRelation.AlternativeForkCandidate,
        ExplanationRelation.CappedSameStory,
        ExplanationRelation.BlockedByEngineRefute
      )
    )

    assertEquals(hanging.writer, Some(StoryWriter.TacticHanging))
    assertEquals(hanging.scene, Scene.Tactic)
    assertEquals(hanging.tactic, Some(Tactic.Hanging))
    assert(hanging.captureResult.exists(_.positiveMaterial))
    assertEquals(hanging.threatProof, None)
    assertEquals(hanging.defenseProof, None)
    assertEquals(hangingPlan.allowedClaim, Some(ExplanationClaim.CanWinPiece))

    assertEquals(material.writer, Some(StoryWriter.SceneMaterial))
    assertEquals(material.scene, Scene.Material)
    assertEquals(material.tactic, None)
    assert(material.captureResult.exists(_.positiveMaterial))
    assertEquals(material.threatProof, None)
    assertEquals(material.defenseProof, None)
    assertEquals(materialPlan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))

    assertEquals(defense.writer, Some(StoryWriter.SceneDefense))
    assertEquals(defense.scene, Scene.Defense)
    assertEquals(defense.tactic, None)
    assertEquals(defense.captureResult, None)
    assert(defense.threatProof.exists(_.complete))
    assert(defense.defenseProof.exists(_.complete))
    assertEquals(defensePlan.allowedClaim, Some(ExplanationClaim.DefendsPiece))

    val overlapVerdicts = StoryTable.choose(Vector(material, hanging))
    assertEquals(overlapVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(overlapVerdicts.find(_.story == hanging).map(_.role), Some(Role.Lead))
    assertEquals(overlapVerdicts.find(_.story == material).map(_.role), Some(Role.Support))

    val speechKeysByOwner =
      Map(
        "Tactic.Hanging" -> ExplanationClaim.HangingAllowed.map(_.key),
        "Scene.Material" -> ExplanationClaim.MaterialAllowed.map(_.key),
        "Scene.Defense" -> ExplanationClaim.DefenseAllowed.map(_.key)
      )
    assertEquals(speechKeysByOwner.values.flatten.toVector.distinct.size, speechKeysByOwner.values.flatten.size)
    speechKeysByOwner.values.flatten.foreach: key =>
      Vector("pressure", "initiative", "strategy", "plan", "winning", "decisive", "conversion").foreach: broadTerm =>
        assert(!key.contains(broadTerm), s"speech key must not promote broad term authority: $key")

    val runtimeAuthorityNames =
      StoryWriter.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.key).toVector ++
        ExplanationRelation.values.map(_.key).toVector
    Vector("mih", "hardening", "closeout", "diagnostic_boundary", "public_route", "production_api").foreach: forbidden =>
      assert(!runtimeAuthorityNames.exists(_.toLowerCase.contains(forbidden)), s"MIH helper became runtime authority: $forbidden")

  test("Material-3 Scene.Material writer admits one narrow proof-backed Story"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val writerSurfaceNames =
      SceneMaterial.getClass.getDeclaredMethods.map(_.getName).toSet ++
        SceneMaterial.getClass.getDeclaredFields.map(_.getName).toSet

    assertEquals(SceneMaterial.WriterOpen, true)
    assertEquals(story.scene, Scene.Material)
    assertEquals(story.tactic, None)
    assertEquals(story.writer, Some(StoryWriter.SceneMaterial))
    assertEquals(story.captureResult.exists(_.positiveMaterial), true)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.anchor, Some(Square('d', 4)))
    assertEquals(story.target, Some(Square('e', 5)))
    assertEquals(story.route, Some(capture))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(verdict.proofFailures, Vector.empty)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    Vector(
      "winning",
      "decisive",
      "blunder",
      "conversion",
      "bestMove",
      "forced",
      "noCounterplay",
      "engineSays",
      "render",
      "llm"
    ).foreach: forbidden =>
      assert(!writerSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Material-3 writer keeps false positives and aliases silent"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val defendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val defendedCapture = Line(Square('e', 4), Square('d', 5))
    val untrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val unclearMaterial =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        material = readyMaterial.copy(known = false),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val kingTarget =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 5)),
          Piece(Side.White, Man.Queen, Square('d', 4))
        )
      )

    assertEquals(SceneMaterial.write(defendedFacts, defendedCapture), None, "equal recapture result is closed")
    assertEquals(SceneMaterial.write(positiveFacts, Line(Square('d', 4), Square('d', 5))), None, "illegal capture")
    assertEquals(SceneMaterial.write(untrusted, capture), None, "same-board proof missing")
    assertEquals(SceneMaterial.write(unclearMaterial, capture), None, "material result unknown")
    assertEquals(SceneMaterial.write(kingTarget, capture), None, "king target is closed")

    val material = SceneMaterial.write(positiveFacts, capture).get
    val noWriter = material.copy(writer = None)
    val missingCapture = material.copy(captureResult = None)
    val mismatchedTarget = material.copy(target = Some(Square('d', 4)))
    val tacticAlias = material.copy(scene = Scene.Tactic, tactic = Some(Tactic.Hanging))
    val hangingAlias = material.copy(writer = Some(StoryWriter.TacticHanging))

    Vector(noWriter, missingCapture, mismatchedTarget, tacticAlias, hangingAlias).foreach: story =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false)
      assert(verdict.role != Role.Lead)

  test("Material-3 EngineCheck Refutes blocks Scene.Material writer"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = SceneMaterial.write(facts, capture).get

    def check(status: EngineCheckStatus) =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supports = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(supports.engineCheck.map(_.status), Option(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Option(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Option(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Option(EngineCheckStatus.Refutes))
    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)

  test("Material-4 negative corpus keeps material-looking false positives silent"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val defendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val defendedCapture = Line(Square('e', 4), Square('d', 5))
    val material = SceneMaterial.write(positiveFacts, capture).get
    val zeroResult = CaptureResult.fromBoardFacts(defendedFacts, defendedCapture)
    val untrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val unclearMaterial =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        material = readyMaterial.copy(known = false),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val kingTarget =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 5)),
          Piece(Side.White, Man.Queen, Square('d', 4))
        )
      )

    def noLeadOrBlocked(label: String, story: Story): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(verdict.role, Role.Blocked, label)

    def check(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = positiveFacts,
        story = Some(material),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    assertEquals(SceneMaterial.write(positiveFacts, Line(Square('d', 4), Square('d', 5))), None, "no legal line")
    assertEquals(SceneMaterial.write(untrusted, capture), None, "same-board proof missing")
    assertEquals(SceneMaterial.write(defendedFacts, defendedCapture), None, "recapture erases material result")
    assertEquals(SceneMaterial.write(unclearMaterial, capture), None, "exchange result unclear")
    assertEquals(SceneMaterial.write(kingTarget, capture), None, "king target")
    assertEquals(zeroResult.materialResult, Some(0))

    val zeroMaterialStory =
      material.copy(
        target = zeroResult.targetPiece.map(_.square),
        anchor = zeroResult.capturingPiece.map(_.square),
        route = Some(defendedCapture),
        storyProof = StoryProof.fromBoardFacts(defendedFacts, defendedCapture),
        captureResult = Some(zeroResult)
      )
    val noLegalLine = material.copy(route = None)
    val storyProofIncomplete = material.copy(storyProof = StoryProof.empty)
    val proofWithoutMaterialResult =
      material.copy(captureResult = material.captureResult.map(_.copy(materialResult = None)))
    val proofWithoutExchange =
      material.copy(captureResult = material.captureResult.map(_.copy(boundedExchangeSequence = Vector.empty)))
    val proofMissing = material.copy(captureResult = None)
    val tacticWriterAsMaterial = material.copy(writer = Some(StoryWriter.TacticHanging))
    val forkWriterAsMaterial = material.copy(writer = Some(StoryWriter.TacticFork))
    val highProofOnly =
      Story(
        Scene.Material,
        side = Side.White,
        target = Some(Square('a', 3)),
        anchor = Some(Square('a', 2)),
        route = Some(safeRoute),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99,
          forcing = 99,
          kingHeat = 99,
          immediacy = 99
        ),
        storyProof = storyProof()
      )
    val refuted = SceneMaterial.withEngineCheck(material, check(EngineCheckStatus.Refutes)).get

    Vector(
      "legal line missing" -> noLegalLine,
      "material result zero" -> zeroMaterialStory,
      "engine refutes" -> refuted,
      "StoryProof incomplete" -> storyProofIncomplete,
      "material proof lacks result" -> proofWithoutMaterialResult,
      "material proof lacks bounded exchange" -> proofWithoutExchange,
      "material proof missing" -> proofMissing,
      "tactic writer tries Material" -> tacticWriterAsMaterial,
      "Fork writer tries Material" -> forkWriterAsMaterial,
      "high Proof score only" -> highProofOnly
    ).foreach: (label, story) =>
      noLeadOrBlocked(label, story)

  test("Material-4 prevents automatic duplicate Material speech from tactic Stories"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingCapture = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingCapture).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

    def noMaterialRow(label: String, story: Story): Unit =
      val verdicts = StoryTable.choose(Vector(story))
      assertEquals(verdicts.exists(_.story.scene == Scene.Material), false, label)

    def blockedMaterialAlias(label: String, story: Story): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(verdict.role, Role.Blocked, label)

    noMaterialRow("Hanging alone does not create Material", hanging)
    noMaterialRow("Fork alone does not create Material", fork)
    blockedMaterialAlias("Hanging copied as Material is blocked", hanging.copy(scene = Scene.Material, tactic = None))
    blockedMaterialAlias("Fork copied as Material is blocked", fork.copy(scene = Scene.Material, tactic = None))

  test("Material-5 reuses EngineCheck only for existing same-board Material Stories"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val otherFacts = BoardFacts.fromFen(Fen.initial).toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val wrongRoute = Line(Square('d', 4), Square('d', 5))
    val reply = EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))
    val story = SceneMaterial.write(facts, capture).get

    def check(
        status: EngineCheckStatus,
        engineLine: Option[EngineLine] = Some(EngineLine(Vector(capture))),
        checkedFacts: BoardFacts = facts,
        storyInput: Option[Story] = Some(story),
        depth: Option[Int] = Some(18),
        freshnessPly: Option[Int] = Some(0)
    ) =
      EngineCheck.fromStory(
        facts = checkedFacts,
        story = storyInput,
        engineLine = engineLine,
        replyLine = Some(reply),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = depth,
        freshnessPly = freshnessPly,
        requestedStatus = status
      )

    val supports = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get

    assertEquals(supports.engineCheck.exists(_.storyBound), true)
    assertEquals(StoryTable.choose(Vector(supports)).head.role, Role.Lead)
    assertEquals(StoryTable.choose(Vector(caps)).head.engineStrengthLimited, true)
    assertEquals(StoryTable.choose(Vector(unknown)).head.role, Role.Lead)
    assertEquals(StoryTable.choose(Vector(refutes)).head.role, Role.Blocked)

    val engineOnly = check(EngineCheckStatus.Supports, storyInput = None)
    val differentFen = check(EngineCheckStatus.Supports, checkedFacts = otherFacts)
    val routeMismatch = check(EngineCheckStatus.Supports, engineLine = Some(EngineLine(Vector(wrongRoute))))
    val missingDepth = check(EngineCheckStatus.Supports, depth = None, freshnessPly = None)
    val stale = check(EngineCheckStatus.Supports, freshnessPly = Some(2))
    val rawEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(capture),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    Vector(engineOnly, differentFen, routeMismatch, missingDepth, stale).foreach: engineCheck =>
      assertEquals(engineCheck.status, EngineCheckStatus.Unknown)
      assertEquals(SceneMaterial.withEngineCheck(story, engineCheck), None)
    assertEquals(rawEvidence.evidenceReady, true)
    assertEquals(rawEvidence.storyBound, false)
    assertEquals(SceneMaterial.withEngineCheck(story, rawEvidence), None)
    assertEquals(StoryTable.choose(Vector(story)).exists(_.story.engineCheck.nonEmpty), false)
    assertEquals(intercept[ClassNotFoundException](Class.forName("lila.commentary.chess.MaterialEngineCheck")).getClass, classOf[ClassNotFoundException])

  test("Material-6 StoryTable lets Hanging Fork and Material compete deterministically"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val material = SceneMaterial.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val low = score(72)
    val high = score(99)
    val hangingHigh = hanging.copy(proof = high)
    val forkHigh = fork.copy(proof = high)
    val materialHigh = material.copy(proof = high)
    val hangingLow = hanging.copy(proof = low)
    val forkLow = fork.copy(proof = low)
    val materialLow = material.copy(proof = low)

    assertEquals(StoryTable.choose(Vector(hangingHigh, forkLow, materialLow)).head.story, hangingHigh)
    assertEquals(StoryTable.choose(Vector(hangingLow, forkHigh, materialLow)).head.story, forkHigh)
    assertEquals(StoryTable.choose(Vector(forkLow, materialHigh)).head.story, materialHigh)

    val overlapping = StoryTable.choose(Vector(materialHigh, hanging))
    val overlappingHanging = overlapping.find(_.story == hanging).get
    val overlappingMaterial = overlapping.find(_.story == materialHigh).get

    assertEquals(materialHigh.route, hanging.route)
    assertEquals(materialHigh.target, hanging.target)
    assertEquals(materialHigh.captureResult.flatMap(_.materialResult), hanging.captureResult.flatMap(_.materialResult))
    assertEquals(overlappingHanging.role, Role.Lead)
    assertEquals(overlappingMaterial.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(overlappingMaterial).flatMap(DeterministicRenderer.fromPlan), None)

    val tied = Vector(hangingHigh, forkHigh, materialHigh)
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(materialHigh, hangingHigh, forkHigh))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.map(_.role), Vector(Role.Lead, Role.Support, Role.Support))
    forward.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)
    assertEquals(StoryTable.choose(Vector(hangingHigh, forkHigh)).exists(_.story.scene == Scene.Material), false)

  test("Material-6 blocks invalid Material rows and ignores raw engine eval ordering"):
    val materialFacts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val leftCapture = Line(Square('d', 4), Square('c', 5))
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val leftMaterial = SceneMaterial.write(materialFacts, leftCapture).get
    val rightMaterial = SceneMaterial.write(materialFacts, rightCapture).get.copy(proof = leftMaterial.proof)
    val reply = EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))

    def checked(story: Story, before: Int, after: Int): Story =
      val route = story.route.get
      val check = EngineCheck.fromStory(
        facts = materialFacts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(route))),
        replyLine = Some(reply),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      SceneMaterial.withEngineCheck(story, check).get

    val lowEvalLeft = checked(leftMaterial, before = 20, after = 80)
    val highEvalRight = checked(rightMaterial, before = 400, after = 460)
    val highEvalLeft = checked(leftMaterial, before = 400, after = 460)
    val lowEvalRight = checked(rightMaterial, before = 20, after = 80)

    assertEquals(
      StoryTable.choose(Vector(highEvalRight, lowEvalLeft)).map(_.story.route),
      StoryTable.choose(Vector(lowEvalRight, highEvalLeft)).map(_.story.route)
    )

    val refutedCheck = EngineCheck.fromStory(
      facts = materialFacts,
      story = Some(leftMaterial),
      engineLine = Some(EngineLine(Vector(leftCapture))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refutedMaterial = SceneMaterial.withEngineCheck(leftMaterial, refutedCheck).get
    val incompleteMaterial = leftMaterial.copy(storyProof = StoryProof.empty)
    val writerlessMaterial = leftMaterial.copy(writer = None)
    val noProofMaterial = leftMaterial.copy(captureResult = None)
    val highProofConversionLike = leftMaterial.copy(
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99,
        forcing = 99,
        immediacy = 99,
        kingHeat = 99
      )
    )

    val verdicts = StoryTable.choose(
      Vector(highProofConversionLike, refutedMaterial, incompleteMaterial, writerlessMaterial, noProofMaterial)
    )

    assertEquals(verdicts.find(_.story == highProofConversionLike).map(_.role), Some(Role.Lead))
    assertEquals(verdicts.find(_.story == refutedMaterial).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == incompleteMaterial).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == writerlessMaterial).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == noProofMaterial).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.exists(_.story.scene == Scene.Convert), false)
    verdicts.foreach: verdict =>
      assertEquals(verdict.values.size, Verdict.Size)
    assertEquals(
      verdicts.find(_.story == highProofConversionLike).flatMap(ExplanationPlan.fromSelected).flatMap(_.allowedClaim),
      Some(ExplanationClaim.MaterialBalanceChanges)
    )
    Vector(refutedMaterial, incompleteMaterial, writerlessMaterial, noProofMaterial).foreach: blockedStory =>
      val blockedPlan = verdicts.find(_.story == blockedStory).flatMap(ExplanationPlan.fromSelected)
      assertEquals(blockedPlan.flatMap(_.allowedClaim), None)
      assertEquals(blockedPlan.flatMap(DeterministicRenderer.fromPlan), None)

  test("Material-7 ExplanationPlan lowers selected Material Verdict without raw proof input"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val maybePlan = ExplanationPlan.fromSelected(verdict)
    val fromSelectedMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    val fromSelectedParameterNames =
      fromSelectedMethods
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getName)
        .mkString(" ")
    val planSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)), Vector(Vector("Verdict")))
    Vector(
      "BoardFacts",
      "CaptureResult",
      "ExchangeResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "String",
      "Source"
    ).foreach: forbiddenType =>
      assert(!fromSelectedParameterNames.contains(forbiddenType), s"ExplanationPlan must not accept $forbiddenType")

    val plan = maybePlan.get
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Material)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 5)))
    assertEquals(plan.anchor, Some(Square('d', 4)))
    assertEquals(plan.route, Some(capture))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(plan.allowedClaim.map(_.key), Some("material_balance_changes"))
    assertEquals(
      ExplanationClaim.MaterialAllowed.map(_.key),
      Vector("material_balance_changes", "line_leaves_material_gain", "exchange_leaves_side_ahead")
    )
    assertEquals(
      ExplanationClaim.MaterialForbiddenKeys,
      Vector(
        "winning_position",
        "decisive_advantage",
        "conversion",
        "blunder",
        "best_move",
        "forced_win",
        "no_counterplay",
        "line_tactic_identity"
      )
    )
    assertEquals(plan.evidenceLine, Some(capture))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    Vector("winning", "decisive", "conversion", "blunder", "best_move", "forced_win", "no_counterplay")
      .foreach: forbidden =>
        assert(plan.forbiddenWording.map(_.key).contains(forbidden), forbidden)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    Vector("sentence", "prose", "rendered").foreach: forbiddenName =>
      assert(!plan.productElementNames.exists(name => name.toLowerCase.contains(forbiddenName)))
    Vector(
      "materialProof",
      "captureResult",
      "exchangeResult",
      "engineCheck",
      "boardFacts",
      "rawPv",
      "proofFailures",
      "sourceRow"
    ).foreach: forbiddenName =>
      assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Material-7 Material non Lead capped and refuted plans create no stronger claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val leftCapture = Line(Square('d', 4), Square('c', 5))
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val leftMaterial = SceneMaterial.write(facts, leftCapture).get
    val rightMaterial = SceneMaterial.write(facts, rightCapture).get.copy(proof = leftMaterial.proof)
    val verdicts = StoryTable.choose(Vector(leftMaterial, rightMaterial))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)

    def checked(status: EngineCheckStatus): Verdict =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(leftMaterial),
        engineLine = Some(EngineLine(Vector(leftCapture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      StoryTable.choose(Vector(SceneMaterial.withEngineCheck(leftMaterial, check).get)).head

    val cappedVerdict = checked(EngineCheckStatus.Caps)
    val refutedVerdict = checked(EngineCheckStatus.Refutes)
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val supportPlan = ExplanationPlan.fromSelected(supportVerdict).get
    val contextPlan = ExplanationPlan.fromSelected(contextVerdict).get
    val blockedPlan = ExplanationPlan.fromSelected(blockedVerdict).get
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get
    val refutedPlan = ExplanationPlan.fromSelected(refutedVerdict).get

    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    Vector(supportPlan, contextPlan, blockedPlan, cappedPlan, refutedPlan).foreach: plan =>
      assertEquals(plan.allowedClaim, None)
      assertEquals(DeterministicRenderer.fromPlan(plan), None)
    assertEquals(supportPlan.relations.map(_.key), Vector("same_family_lower_rank"))
    assertEquals(contextPlan.relations, Vector.empty)
    assertEquals(blockedPlan.debugOnly, true)
    assertEquals(cappedPlan.relations.map(_.key), Vector("capped_same_story"))
    assert(cappedPlan.forbiddenWording.map(_.key).contains("strong_wording"))
    assertEquals(refutedPlan.relations.map(_.key), Vector("blocked_by_engine_refute"))
    assertEquals(refutedPlan.debugOnly, true)

  test("Material-8 DeterministicRenderer phrases Material ExplanationPlan without stronger meaning"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val maybeRendered = DeterministicRenderer.fromPlan(plan)
    val forbiddenPhrases =
      Vector(
        "winning",
        "technically winning",
        "decisive",
        "blunder",
        "forced",
        "best move",
        "no counterplay",
        "engine says",
        "conversion"
      )
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.toVector

    assertEquals(plan.scene, Scene.Material)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(facts.sideLegal.sanFor(capture), Some("dxe5"))
    assertEquals(plan.routeSan, Some("dxe5"))
    assertEquals(maybeRendered.map(_.text), Some("After dxe5, White comes out ahead in material."))
    val rendered = maybeRendered.get
    assertEquals(rendered.text, "After dxe5, White comes out ahead in material.")
    assertEquals(rendered.claimKey, "material_balance_changes")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assertEquals(
      rendererMethods
        .filter(_.getName == "fromPlan")
        .map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("ExplanationPlan"))
    )

  test("Material-8 refuses Material renderer text without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val leftCapture = Line(Square('d', 4), Square('c', 5))
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val leftMaterial = SceneMaterial.write(facts, leftCapture).get
    val rightMaterial = SceneMaterial.write(facts, rightCapture).get.copy(proof = leftMaterial.proof)
    val verdicts = StoryTable.choose(Vector(leftMaterial, rightMaterial))
    val leadPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get).get
    val supportPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Support).get).get
    val blockedPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get.copy(role = Role.Blocked)).get
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(leftMaterial),
      engineLine = Some(EngineLine(Vector(leftCapture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val cappedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(SceneMaterial.withEngineCheck(leftMaterial, check).get)).head).get

    assert(DeterministicRenderer.fromPlan(leadPlan).nonEmpty)
    Vector(
      supportPlan,
      blockedPlan,
      cappedPlan,
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.LineLeavesMaterialGain)),
      leadPlan.copy(strength = ExplanationStrength.Bounded, forbiddenWording = Vector.empty),
      leadPlan.copy(scene = Scene.Tactic),
      leadPlan.copy(debugOnly = true)
    ).foreach: plan =>
      assertEquals(DeterministicRenderer.fromPlan(plan), None)

  test("Material-9 LLM smoke accepts Material RenderedLine without raw proof input"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val mockText = LlmNarrationSmoke.mockNarrate(plan, rendered)
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered)
    val checked = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White is ahead in material.")

    assertEquals(mockText, Some(rendered.text))
    assertEquals(checked.accepted, true)
    assertEquals(checked.violations, Vector.empty)
    Vector(
      "renderedText: After dxe5, White comes out ahead in material.",
      "claimKey: material_balance_changes",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.exists(_.contains(required)), s"Material prompt must include allowed input: $required")
    Vector(
      "Verdict",
      "Story",
      "material proof",
      "CaptureResult",
      "ExchangeResult",
      "EngineCheck",
      "BoardFacts",
      "engine eval",
      "raw PV",
      "proofFailures",
      "source row",
      "materialResult:",
      "capturedPieces:",
      "recaptureCandidates:"
    ).foreach: forbidden =>
      assert(!prompt.exists(_.contains(forbidden)), s"Material prompt must not include forbidden raw input label: $forbidden")

  test("Material-9 LLM smoke rejects stronger Material rephrases"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    val safe = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White is ahead in material.")
    val inventedMove = LlmNarrationSmoke.check(plan, rendered, "After dxe5 Ke7, White is ahead in material.")
    val inventedLine = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White is ahead, and Ke7 follows.")
    val inventedTactic = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White starts a fork.")
    val inventedPlan = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White starts a plan.")
    val engineBestWinning =
      LlmNarrationSmoke.check(plan, rendered, "The engine says dxe5 is the best move and technically winning.")
    val forcedDecisiveBlunder =
      LlmNarrationSmoke.check(plan, rendered, "dxe5 is a forced decisive result after a blunder.")
    val conversion = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White converts the material edge.")
    val strongerMaterialWin = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White wins material.")
    val noCounterplay = LlmNarrationSmoke.check(plan, rendered, "After dxe5, Black has no counterplay.")
    val coordinateRoute = LlmNarrationSmoke.check(plan, rendered, "After the move from d4 to e5, White is ahead in material.")
    val compactCoordinateRoute = LlmNarrationSmoke.check(plan, rendered, "After d4e5, White is ahead in material.")

    assertEquals(safe.accepted, true)
    assertEquals(safe.violations, Vector.empty)
    assertEquals(inventedMove.accepted, false)
    assertEquals(inventedMove.violations.contains("new_move_or_line"), true)
    assertEquals(inventedLine.accepted, false)
    assertEquals(inventedLine.violations.contains("new_move_or_line"), true)
    assertEquals(inventedTactic.accepted, false)
    assertEquals(inventedTactic.violations.contains("new_tactic_or_plan"), true)
    assertEquals(inventedPlan.accepted, false)
    assertEquals(inventedPlan.violations.contains("new_tactic_or_plan"), true)
    assertEquals(engineBestWinning.accepted, false)
    assertEquals(engineBestWinning.violations.contains("engine_mention"), true)
    assertEquals(engineBestWinning.violations.contains("forbidden_wording"), true)
    assertEquals(engineBestWinning.violations.contains("stronger_claim"), true)
    assertEquals(forcedDecisiveBlunder.accepted, false)
    assertEquals(forcedDecisiveBlunder.violations.contains("forbidden_wording"), true)
    assertEquals(forcedDecisiveBlunder.violations.contains("stronger_claim"), true)
    assertEquals(conversion.accepted, false)
    assertEquals(conversion.violations.contains("forbidden_wording"), true)
    assertEquals(strongerMaterialWin.accepted, false)
    assertEquals(strongerMaterialWin.violations.contains("stronger_claim"), true)
    assertEquals(noCounterplay.accepted, false)
    assertEquals(noCounterplay.violations.contains("forbidden_wording"), true)
    assertEquals(coordinateRoute.accepted, false)
    assertEquals(coordinateRoute.violations.contains("non_san_move_text"), true)
    assertEquals(compactCoordinateRoute.accepted, false)
    assertEquals(compactCoordinateRoute.violations.contains("non_san_move_text"), true)

  test("Material slice closeout keeps sibling scenes and proof homes closed"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.story.scene, Scene.Material)
    assertEquals(material.tactic, None)
    assertEquals(material.plan, None)
    assertEquals(material.captureResult.exists(_.publicClaimAllowed), false)
    assertEquals(material.storyProof.failures(material), Vector.empty)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(rendered.text.contains("winning"), false)
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "After dxe5, White has a winning position.").accepted, false)
    assertEquals(intercept[ClassNotFoundException](Class.forName("lila.commentary.chess.ExchangeResult")).getClass, classOf[ClassNotFoundException])
    assertEquals(intercept[ClassNotFoundException](Class.forName("lila.commentary.chess.MaterialEngineCheck")).getClass, classOf[ClassNotFoundException])

    val engineCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(material),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val checkedMaterial = SceneMaterial.withEngineCheck(material, engineCheck).get
    val checkedVerdict = StoryTable.choose(Vector(checkedMaterial)).head
    val checkedPlan = ExplanationPlan.fromSelected(checkedVerdict).get
    val checkedRendered = DeterministicRenderer.fromPlan(checkedPlan).get

    assertEquals(checkedMaterial.engineCheck.exists(_.storyBound), true)
    assertEquals(checkedVerdict.role, Role.Lead)
    assertEquals(checkedPlan.scene, Scene.Material)
    assertEquals(checkedRendered.claimKey, "material_balance_changes")
    assertEquals(LlmNarrationSmoke.mockNarrate(checkedPlan, checkedRendered), Some(checkedRendered.text))

    val siblingScenes = Vector(
      "Defense" -> material.copy(scene = Scene.Defense),
      "Plan" -> material.copy(scene = Scene.Plan, plan = Some(Plan.CenterBreak)),
      "Convert" -> material.copy(scene = Scene.Convert),
      "Blunder" -> material.copy(scene = Scene.Blunder)
    )

    siblingScenes.foreach: (label, story) =>
      val siblingVerdict = StoryTable.choose(Vector(story)).head
      assertEquals(siblingVerdict.role, Role.Blocked, label)
      assertEquals(siblingVerdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(siblingVerdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("CaptureResult leaves missing evidence for capture false positives"):
    val legalButUntrustedLine = Line(Square('d', 4), Square('e', 5))
    val legalButUntrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = legalButUntrustedLine, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val illegalCapture = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val ownTarget = BoardFacts.fromFen("4k3/8/8/4N3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val kingTarget =
      minimalBoardFacts(
        sideLegal = readyMoves(line = Line(Square('d', 4), Square('e', 5)), captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 5)),
          Piece(Side.White, Man.Queen, Square('d', 4))
        )
      )
    val unclearMaterial =
      minimalBoardFacts(
        sideLegal = readyMoves(line = legalButUntrustedLine, captureCount = 1),
        material = readyMaterial.copy(known = false),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )

    val cases = Vector(
      "attacked but illegal capture" ->
        CaptureResult.fromBoardFacts(illegalCapture, Line(Square('d', 4), Square('d', 5))) ->
        Vector("legal capture", "target piece"),
      "target is own piece" ->
        CaptureResult.fromBoardFacts(ownTarget, legalButUntrustedLine) ->
        Vector("legal capture", "target enemy piece"),
      "target is king" ->
        CaptureResult.fromBoardFacts(kingTarget, Line(Square('d', 4), Square('e', 5))) ->
        Vector("legal capture", "target non-king"),
      "same-board proof missing" ->
        CaptureResult.fromBoardFacts(legalButUntrusted, legalButUntrustedLine) ->
        Vector("same-board proof"),
      "capture material result unclear" ->
        CaptureResult.fromBoardFacts(unclearMaterial, legalButUntrustedLine) ->
        Vector("same-board proof", "material result")
    )

    cases.foreach:
      case ((label, result), expectedMissing) =>
        assertEquals(result.publicClaimAllowed, false, label)
        assertEquals(result.positiveMaterial, false, label)
        assert(result.missingEvidence.nonEmpty, label)
        val missing = result.missingEvidence.flatMap(_.missing)
        expectedMissing.foreach: expected =>
          assert(missing.contains(expected), s"$label must report missing $expected, got $missing")

    val highProofHanging = Story(
      Scene.Tactic,
      tactic = Some(Tactic.Hanging),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 5)),
      anchor = Some(Square('d', 4)),
      route = Some(legalButUntrustedLine),
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99,
        forcing = 99,
        immediacy = 99
      ),
      storyProof = StoryProof.fromBoardFacts(BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get, legalButUntrustedLine)
    )
    val verdict = StoryTable.choose(Vector(highProofHanging)).head

    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("EngineCheck records internal engine evidence without public claim authority"):
    val checkedMove = Line(Square('d', 4), Square('e', 5))
    val replyMove = Line(Square('e', 8), Square('e', 7))
    val engineLine = EngineLine(Vector(checkedMove, replyMove))
    val replyLine = EngineLine(Vector(replyMove))

    val check = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(checkedMove),
      engineLine = Some(engineLine),
      replyLine = Some(replyLine),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(85)),
      depth = Some(18),
      freshnessPly = Some(0)
    )
    val engineSurfaceNames =
      classOf[EngineCheck].getDeclaredMethods.map(_.getName).toSet ++
        classOf[EngineCheck].getDeclaredFields.map(_.getName).toSet ++
        classOf[EngineLine].getDeclaredMethods.map(_.getName).toSet ++
        classOf[EngineLine].getDeclaredFields.map(_.getName).toSet ++
        classOf[EngineEval].getDeclaredMethods.map(_.getName).toSet ++
        classOf[EngineEval].getDeclaredFields.map(_.getName).toSet

    assertEquals(engineLine.moves, Vector(checkedMove, replyMove))
    assertEquals(replyLine.moves, Vector(replyMove))
    assertEquals(check.sameBoardProof, true)
    assertEquals(check.checkedMove, Some(checkedMove))
    assertEquals(check.engineLine, Some(engineLine))
    assertEquals(check.replyLine, Some(replyLine))
    assertEquals(check.evalBefore, Some(EngineEval(20)))
    assertEquals(check.evalAfter, Some(EngineEval(85)))
    assertEquals(check.depth, Some(18))
    assertEquals(check.freshnessPly, Some(0))
    assertEquals(check.missingEvidence, Vector.empty)
    assertEquals(check.publicClaimAllowed, false)
    assertEquals(check.evidenceReady, true)
    Vector("best", "strategy", "commentary", "render", "llm", "publicText", "verdict").foreach: forbidden =>
      assert(!engineSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("EngineCheck reports missing same-board stale and move-binding evidence"):
    val checkedMove = Line(Square('d', 4), Square('e', 5))
    val otherMove = Line(Square('d', 4), Square('d', 5))
    val stale = EngineCheck.fromEvidence(
      sameBoardProof = false,
      checkedMove = Some(checkedMove),
      engineLine = Some(EngineLine(Vector(otherMove))),
      replyLine = None,
      evalBefore = None,
      evalAfter = None,
      depth = None,
      freshnessPly = Some(2)
    )

    assertEquals(stale.publicClaimAllowed, false)
    assertEquals(stale.evidenceReady, false)
    assertEquals(
      stale.missingEvidence,
      Vector(
        BoardFacts.MissingEvidence(
          "EngineCheck",
          Vector(
            "same-board proof",
            "checked move in engine line",
            "reply line",
            "eval before",
            "eval after",
            "depth or freshness",
            "fresh engine evidence"
          )
        )
      )
    )

  test("EngineLine rejects empty PV-shaped evidence"):
    intercept[IllegalArgumentException]:
      EngineLine(Vector.empty)

  test("EngineCheck rejects engine evidence from a different FEN"):
    val storyFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val otherFacts = BoardFacts.fromFen(Fen.initial).toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(storyFacts, capture).get

    val check = EngineCheck.fromStory(
      facts = otherFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0)
    )

    assertEquals(check.publicClaimAllowed, false)
    assertEquals(check.evidenceReady, false)
    assertEquals(check.sameBoardProof, false)
    assert(check.missingEvidence.flatMap(_.missing).contains("same-board proof"))
    assert(check.missingEvidence.flatMap(_.missing).contains("same legal line"))

  test("EngineCheck rejects engine lines that do not start with the Story route"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val wrongRoute = Line(Square('d', 4), Square('d', 5))
    val story = TacticHanging.write(facts, capture).get

    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0)
    )

    assertEquals(check.publicClaimAllowed, false)
    assertEquals(check.evidenceReady, false)
    assertEquals(check.sameBoardProof, true)
    assert(check.missingEvidence.flatMap(_.missing).contains("same Story route"))
    assert(check.missingEvidence.flatMap(_.missing).contains("checked move in engine line"))

  test("EngineCheck keeps stale or depth-missing engine data diagnostic only"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get

    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = None,
      freshnessPly = Some(2)
    )

    assertEquals(check.publicClaimAllowed, false)
    assertEquals(check.evidenceReady, false)
    assertEquals(check.missingEvidence.flatMap(_.missing), Vector("depth or freshness", "fresh engine evidence"))

  test("EngineCheck cannot speak from eval or PV without a Story"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val evalOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = None,
      replyLine = None,
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0)
    )
    val pvOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = None,
      evalAfter = None,
      depth = Some(18),
      freshnessPly = Some(0)
    )

    Vector(evalOnly, pvOnly).foreach: check =>
      assertEquals(check.publicClaimAllowed, false)
      assertEquals(check.evidenceReady, false)
      assertEquals(check.checkedMove, None)
      assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
      assert(check.missingEvidence.flatMap(_.missing).contains("Story"))

  test("EngineCheck status stays Unknown unless same-board Story guard passes"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val unknown = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val supports = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(unknown.status, EngineCheckStatus.Unknown)
    assertEquals(supports.status, EngineCheckStatus.Supports)
    assertEquals(supports.missingEvidence, Vector.empty)

  test("Tactic.Hanging attaches EngineCheck statuses and Refutes blocks lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val baseVerdict = StoryTable.choose(Vector(story)).head

    def check(status: EngineCheckStatus) =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supports = TacticHanging.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticHanging.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes = TacticHanging.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(supportsVerdict.leadAllowed, true)
    assertEquals(capsVerdict.leadAllowed, true)
    assertEquals(refutesVerdict.leadAllowed, false)
    assert(refutesVerdict.role != Role.Lead)
    assertEquals(supportsVerdict.values, baseVerdict.values)
    assertEquals(capsVerdict.values, baseVerdict.values)

  test("StoryTable integrates EngineCheck conservatively without creating public engine claims"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val baseVerdict = StoryTable.choose(Vector(story)).head

    def checked(status: EngineCheckStatus) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      TacticHanging.withEngineCheck(story, check).get

    val unknownVerdict = StoryTable.choose(Vector(checked(EngineCheckStatus.Unknown))).head
    val supportsVerdict = StoryTable.choose(Vector(checked(EngineCheckStatus.Supports))).head
    val capsVerdict = StoryTable.choose(Vector(checked(EngineCheckStatus.Caps))).head
    val refutesVerdict = StoryTable.choose(Vector(checked(EngineCheckStatus.Refutes))).head

    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
    Vector(unknownVerdict, supportsVerdict, capsVerdict).foreach: verdict =>
      assertEquals(verdict.leadAllowed, true)
      assertEquals(verdict.role, Role.Lead)
      assertEquals(verdict.strength, baseVerdict.strength)
      assertEquals(verdict.values, baseVerdict.values)

    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(refutesVerdict.engineStrengthLimited, false)
    assertEquals(refutesVerdict.leadAllowed, false)
    assert(refutesVerdict.role != Role.Lead)

  test("Stage 5-1 verified Hanging alone becomes Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(verdict.story, story)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.proofFailures, Vector.empty)

  test("Stage 5-1 chooses deterministic Lead from two verified Hanging rows"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val leftCapture = Line(Square('d', 4), Square('c', 5))
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val left = TacticHanging.write(facts, leftCapture).get
    val right = TacticHanging.write(facts, rightCapture).get
    val forward = StoryTable.choose(Vector(right, left))
    val reverse = StoryTable.choose(Vector(left, right))

    assertEquals(forward.map(_.story), reverse.map(_.story))
    assertEquals(forward.head.story, left)
    assertEquals(forward.head.role, Role.Lead)
    assertEquals(forward.head.leadAllowed, true)
    assertEquals(forward(1).story, right)
    assertEquals(forward(1).role, Role.Support)
    assertEquals(forward(1).leadAllowed, false)

  test("Stage 5-1 Refuted Hanging is Blocked while supported Hanging may Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val supportedCapture = Line(Square('d', 4), Square('c', 5))
    val refutedCapture = Line(Square('d', 4), Square('e', 5))
    val supported = TacticHanging.write(facts, supportedCapture).get
    val refuted = TacticHanging.write(facts, refutedCapture).get

    def check(story: Story, status: EngineCheckStatus) =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val checkedSupported = TacticHanging.withEngineCheck(supported, check(supported, EngineCheckStatus.Supports)).get
    val checkedRefuted = TacticHanging.withEngineCheck(refuted, check(refuted, EngineCheckStatus.Refutes)).get
    val verdicts = StoryTable.choose(Vector(checkedRefuted, checkedSupported))

    assertEquals(verdicts.head.story, checkedSupported)
    assertEquals(verdicts.head.role, Role.Lead)
    assertEquals(verdicts.head.leadAllowed, true)
    assertEquals(verdicts.find(_.story == checkedRefuted).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == checkedRefuted).map(_.leadAllowed), Some(false))
    assertEquals(verdicts.find(_.story == checkedRefuted).flatMap(_.engineCheckStatus), Some(EngineCheckStatus.Refutes))

  test("Stage 5-1 Capped Hanging keeps strength-limited diagnostic"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val capped = TacticHanging.withEngineCheck(story, check).get
    val verdict = StoryTable.choose(Vector(capped)).head

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(verdict.engineStrengthLimited, true)

  test("Stage 5-1 Unknown EngineCheck creates no engine wording"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val baseVerdict = StoryTable.choose(Vector(story)).head
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Unknown
    )
    val unknown = TacticHanging.withEngineCheck(story, check).get
    val verdict = StoryTable.choose(Vector(unknown)).head

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.values, baseVerdict.values)

  test("Stage 5-1 incomplete or unsupported Hanging rows are not Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val lowStrength = story.copy(
      proof = proof(
        boardProof = 60,
        lineProof = 60,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        conversionPrize = 60
      )
    )
    val incompleteProof = story.copy(storyProof = StoryProof.empty)
    val missingCapture = story.copy(captureResult = None)
    val noWriter = story.copy(writer = None)

    val verdicts = StoryTable.choose(Vector(lowStrength, incompleteProof, missingCapture, noWriter))

    assertEquals(verdicts.find(_.story == lowStrength).map(_.role), Some(Role.Context))
    assertEquals(verdicts.find(_.story == incompleteProof).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == missingCapture).map(_.role), Some(Role.Blocked))
    assert(verdicts.find(_.story == noWriter).exists(verdict => verdict.role == Role.Context || verdict.role == Role.Blocked))
    assert(verdicts.forall(_.role != Role.Lead))
    assert(verdicts.forall(!_.leadAllowed))

  test("Stage 5-2 keeps deterministic Lead independent of input order"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val forward = StoryTable.choose(Vector(right, left))
    val reverse = StoryTable.choose(Vector(left, right))

    assertEquals(forward.map(_.story), reverse.map(_.story))
    assertEquals(forward.head.role, Role.Lead)
    assertEquals(forward.head.story, left)

  test("Stage 5-2 uses target anchor and route as deterministic equal-strength tie-breaks"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val c5 = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val e5 = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val routeA = c5.copy(
      target = Some(Square('c', 5)),
      anchor = Some(Square('c', 4)),
      route = Some(Line(Square('c', 4), Square('c', 5))),
      storyProof = StoryProof.untrustedLegalLine(Line(Square('c', 4), Square('c', 5)))
    )
    val routeB = c5.copy(
      target = Some(Square('c', 5)),
      anchor = Some(Square('c', 4)),
      route = Some(Line(Square('d', 4), Square('c', 5))),
      storyProof = StoryProof.untrustedLegalLine(Line(Square('d', 4), Square('c', 5)))
    )

    assertEquals(StoryTable.choose(Vector(e5, c5)).head.story, c5)
    assertEquals(StoryTable.choose(Vector(routeB, routeA)).head.story, routeA)

  test("Stage 5-2 uses blocked status without proofFailures text as public ordering"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val context = story.copy(
      target = Some(Square('a', 1)),
      proof = proof(
        boardProof = 60,
        lineProof = 60,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        conversionPrize = 60
      )
    )
    val blocked = story.copy(
      target = Some(Square('h', 8)),
      storyProof = StoryProof.empty,
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99,
        forcing = 99,
        immediacy = 99
      )
    )
    val verdicts = StoryTable.choose(Vector(blocked, context))

    assertEquals(context.proofFailures, Vector.empty)
    assert(blocked.proofFailures.nonEmpty)
    assertEquals(verdicts.head.story, context)
    assertEquals(verdicts.head.role, Role.Context)
    assertEquals(verdicts(1).story, blocked)
    assertEquals(verdicts(1).role, Role.Blocked)

  test("Stage 5-2 raw engine eval does not reorder Hanging rows"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get

    def checked(story: Story, before: Int, after: Int) =
      val route = story.route.get
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(route))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      TacticHanging.withEngineCheck(story, check).get

    val lowEvalLeft = checked(left, before = 20, after = 80)
    val highEvalRight = checked(right, before = 400, after = 460)
    val highEvalLeft = checked(left, before = 400, after = 460)
    val lowEvalRight = checked(right, before = 20, after = 80)

    assertEquals(
      StoryTable.choose(Vector(highEvalRight, lowEvalLeft)).map(_.story.route),
      StoryTable.choose(Vector(lowEvalRight, highEvalLeft)).map(_.story.route)
    )

  test("Stage 5-3 Refuted Hanging remains Blocked"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(80)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refuted = TacticHanging.withEngineCheck(story, check).get
    val verdict = StoryTable.choose(Vector(refuted)).head

    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(verdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))

  test("Stage 5-3 Quiet cannot Lead when positive Hanging exists"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hanging = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val quiet = Story(
      Scene.Quiet,
      side = Side.White,
      target = Some(Square('a', 2)),
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      rival = Side.Black,
      proof = proof(boardProof = 90, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 90),
      storyProof = storyProof()
    )
    val verdicts = StoryTable.choose(Vector(quiet, hanging))

    assertEquals(verdicts.head.story, hanging)
    assertEquals(verdicts.head.role, Role.Lead)
    assertEquals(verdicts.find(_.story == quiet).map(_.leadAllowed), Some(false))
    assert(verdicts.find(_.story == quiet).exists(_.role != Role.Lead))

  test("Stage 5-3 Source and Opening cannot outrank board-backed Hanging"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hanging = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    def context(scene: Scene, target: Square) =
      Story(
        scene,
        side = Side.White,
        target = Some(target),
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99,
          forcing = 99,
          immediacy = 99
        ),
        storyProof = storyProof()
      )

    val sourceVerdicts = StoryTable.choose(Vector(context(Scene.Source, Square('b', 2)), hanging))
    val openingVerdicts = StoryTable.choose(Vector(context(Scene.Opening, Square('c', 2)), hanging))

    assertEquals(sourceVerdicts.head.story, hanging)
    assertEquals(openingVerdicts.head.story, hanging)
    assert(sourceVerdicts.forall(verdict => verdict.story.scene != Scene.Source || !verdict.leadAllowed))
    assert(openingVerdicts.forall(verdict => verdict.story.scene != Scene.Opening || !verdict.leadAllowed))

  test("Stage 5-3 no-writer Story cannot behave like Hanging"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hanging = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val noWriter = hanging.copy(
      writer = None,
      proof = proof(
        boardProof = 60,
        lineProof = 60,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        forcing = 60,
        immediacy = 60,
        conversionPrize = 60,
        kingHeat = 60
      )
    )
    val verdict = StoryTable.choose(Vector(noWriter)).head

    assertEquals(noWriter.proofFailures, Vector.empty)
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)

  test("EngineCheck negative corpus blocks or weakens false positive Hanging evidence"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val otherFacts = BoardFacts.fromFen(Fen.initial).toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val wrongRoute = Line(Square('d', 4), Square('d', 5))
    val reply = Line(Square('e', 8), Square('e', 7))
    val story = TacticHanging.write(facts, capture).get
    val positiveCapture = CaptureResult.fromBoardFacts(facts, capture)
    val noCaptureResult = story.copy(captureResult = None)
    val incompleteStoryProof = story.copy(storyProof = StoryProof.empty)
    val noNamedWriter = story.copy(writer = None)

    def check(
        storyInput: Option[Story] = Some(story),
        factsInput: BoardFacts = facts,
        engineMoves: Vector[Line] = Vector(capture, reply),
        replyMoves: Vector[Line] = Vector(reply),
        before: EngineEval = EngineEval(80),
        after: EngineEval = EngineEval(120),
        depthInput: Option[Int] = Some(18),
        freshnessInput: Option[Int] = Some(0),
        requested: EngineCheckStatus = EngineCheckStatus.Supports
    ) =
      EngineCheck.fromStory(
        facts = factsInput,
        story = storyInput,
        engineLine = Some(EngineLine(engineMoves)),
        replyLine = Some(EngineLine(replyMoves)),
        evalBefore = Some(before),
        evalAfter = Some(after),
        depth = depthInput,
        freshnessPly = freshnessInput,
        requestedStatus = requested
      )

    val localGainButLargerTactic = check(after = EngineEval(-420))
    val replyRefutes = check(after = EngineEval(-260), requested = EngineCheckStatus.Refutes)
    val evalCollapse = check(before = EngineEval(120), after = EngineEval(-500))
    val wrongBoard = check(factsInput = otherFacts)
    val stale = check(freshnessInput = Some(3))
    val routeMismatch = check(engineMoves = Vector(wrongRoute, reply))
    val engineOnlyNoCapture = check(storyInput = Some(noCaptureResult))
    val engineOnlyIncompleteProof = check(storyInput = Some(incompleteStoryProof))
    val engineOnlyNoNamedWriter = check(storyInput = Some(noNamedWriter))

    Vector(localGainButLargerTactic, replyRefutes, evalCollapse).foreach: refutingCheck =>
      assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
      val checkedStory = TacticHanging.withEngineCheck(story, refutingCheck).get
      val verdict = StoryTable.choose(Vector(checkedStory)).head
      assertEquals(verdict.leadAllowed, false)
      assert(verdict.role != Role.Lead)

    Vector(
      wrongBoard -> "same-board proof",
      stale -> "fresh engine evidence",
      routeMismatch -> "same Story route",
      engineOnlyNoCapture -> "same-board proof",
      engineOnlyIncompleteProof -> "same-board proof",
      engineOnlyNoNamedWriter -> "same-board proof"
    ).foreach: (check, missing) =>
      assertEquals(check.status, EngineCheckStatus.Unknown)
      assertEquals(check.publicClaimAllowed, false)
      assertEquals(check.evidenceReady, false)
      assert(check.missingEvidence.flatMap(_.missing).contains(missing), missing)

    assertEquals(noCaptureResult.copy(engineCheck = Some(engineOnlyNoCapture)).captureResult, None)
    assertEquals(incompleteStoryProof.proofFailures.nonEmpty, true)
    assertEquals(noNamedWriter.writer, None)
    assertEquals(positiveCapture.missingEvidence, Vector.empty)

  test("EngineCheck attaches only to named tactic writers"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(facts, capture).get
    val hangingCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(hanging),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val forkCheck = EngineCheck.fromStory(
      facts = forkFacts,
      story = Some(fork),
      engineLine = Some(EngineLine(Vector(forkMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val material = hanging.copy(scene = Scene.Material, tactic = None)
    val noWriter = hanging.copy(writer = None)

    assertEquals(TacticHanging.withEngineCheck(hanging, hangingCheck).map(_.engineCheck), Some(Some(hangingCheck)))
    assertEquals(TacticFork.withEngineCheck(fork, forkCheck).map(_.engineCheck), Some(Some(forkCheck)))
    assertEquals(TacticHanging.withEngineCheck(material, hangingCheck), None)
    assertEquals(TacticHanging.withEngineCheck(noWriter, hangingCheck), None)
    assertEquals(TacticFork.withEngineCheck(hanging, hangingCheck), None)

  test("Fork-5 EngineCheck reuses existing sidecar without creating Fork"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val reply = EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

    def check(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(fork),
        engineLine = Some(EngineLine(Vector(forkMove))),
        replyLine = Some(reply),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supports = check(EngineCheckStatus.Supports)
    val caps = check(EngineCheckStatus.Caps)
    val unknown = check(EngineCheckStatus.Unknown)
    val refutes = check(EngineCheckStatus.Refutes)

    Vector(supports, caps, unknown, refutes).foreach: engineCheck =>
      assertEquals(engineCheck.evidenceReady, true)
      assertEquals(engineCheck.sameBoardProof, true)
      assertEquals(engineCheck.checkedMove, Some(forkMove))
      assertEquals(TacticFork.withEngineCheck(fork, engineCheck).map(_.engineCheck), Some(Some(engineCheck)))

    assertEquals(StoryTable.choose(Vector(TacticFork.withEngineCheck(fork, supports).get)).head.role, Role.Lead)
    val cappedVerdict = StoryTable.choose(Vector(TacticFork.withEngineCheck(fork, caps).get)).head
    assertEquals(cappedVerdict.role, Role.Lead)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(StoryTable.choose(Vector(TacticFork.withEngineCheck(fork, unknown).get)).head.role, Role.Lead)
    assertEquals(StoryTable.choose(Vector(TacticFork.withEngineCheck(fork, refutes).get)).head.role, Role.Blocked)

    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(forkMove))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val missingDepth = EngineCheck.fromStory(
      facts = facts,
      story = Some(fork),
      engineLine = Some(EngineLine(Vector(forkMove))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = None,
      freshnessPly = None,
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(Line(Square('h', 1), Square('h', 2))),
      engineLine = Some(EngineLine(Vector(Line(Square('h', 1), Square('h', 2))))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.evidenceReady, false)
    assertEquals(engineOnly.checkedMove, None)
    assertEquals(TacticFork.withEngineCheck(fork, engineOnly), None)
    assertEquals(missingDepth.status, EngineCheckStatus.Unknown)
    assertEquals(missingDepth.evidenceReady, false)
    assertEquals(TacticFork.withEngineCheck(fork, missingDepth), None)
    assertEquals(routeMismatch.evidenceReady, true)
    assertEquals(TacticFork.withEngineCheck(fork, routeMismatch), None)
    assertEquals(intercept[ClassNotFoundException](Class.forName("lila.commentary.chess.ForkEngineCheck")).getClass, classOf[ClassNotFoundException])

    val engineCheckSurfaces =
      EngineCheck.getClass.getDeclaredMethods.toVector ++ classOf[EngineCheck].getDeclaredMethods.toVector
    assertEquals(engineCheckSurfaces.exists(_.getReturnType.getSimpleName.contains("Story")), false)

  test("Fork-6 StoryTable orders Hanging and Fork without creating meaning"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val forkTied = fork.copy(proof = hanging.proof)

    assertEquals(StoryTable.choose(Vector(hanging)).head.role, Role.Lead)
    assertEquals(StoryTable.choose(Vector(fork)).head.role, Role.Lead)

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    val hangingFork = StoryTable.choose(Vector(hanging, forkTied))
    val forkHanging = StoryTable.choose(Vector(forkTied, hanging))
    assertEquals(orderShape(hangingFork), orderShape(forkHanging))
    assertEquals(hangingFork.head.story, hanging)
    assertEquals(hangingFork.head.role, Role.Lead)
    assertEquals(hangingFork.find(_.story == forkTied).map(_.role), Some(Role.Support))
    val forkSupportPlan = ExplanationPlan.fromSelected(hangingFork.find(_.story == forkTied).get).get
    assertEquals(forkSupportPlan.allowedClaim, None)
    assertEquals(DeterministicRenderer.fromPlan(forkSupportPlan), None)

    val lowProof = proof(
      boardProof = 60,
      lineProof = 60,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      conversionPrize = 60,
      forcing = 60,
      immediacy = 60
    )
    val writerlessFork = fork.copy(writer = None, proof = lowProof)
    val forkWithoutMultiTargetProof = fork.copy(multiTargetProof = None, proof = lowProof)
    val incompleteFork = fork.copy(storyProof = StoryProof.empty, proof = lowProof)
    val refuted = EngineCheck.fromStory(
      facts = forkFacts,
      story = Some(fork),
      engineLine = Some(EngineLine(Vector(forkMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refutedFork = TacticFork.withEngineCheck(fork, refuted).get
    val blocked = StoryTable.choose(Vector(hanging, writerlessFork, forkWithoutMultiTargetProof, incompleteFork, refutedFork))

    assertEquals(blocked.find(_.story == hanging).map(_.role), Some(Role.Lead))
    assertEquals(blocked.find(_.story == writerlessFork).map(_.role), Some(Role.Blocked))
    assertEquals(blocked.find(_.story == forkWithoutMultiTargetProof).map(_.role), Some(Role.Blocked))
    assertEquals(blocked.find(_.story == incompleteFork).map(_.role), Some(Role.Blocked))
    assertEquals(blocked.find(_.story == refutedFork).map(_.role), Some(Role.Blocked))
    assertEquals(blocked.find(_.story == forkWithoutMultiTargetProof).get.proofFailures, Vector.empty)

    def checkedFork(evalBefore: Int, evalAfter: Int, pvTail: Line): Story =
      val check = EngineCheck.fromStory(
        facts = forkFacts,
        story = Some(forkTied),
        engineLine = Some(EngineLine(Vector(forkMove, pvTail))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(evalBefore)),
        evalAfter = Some(EngineEval(evalAfter)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      TacticFork.withEngineCheck(forkTied, check).get

    val lowEvalFork = checkedFork(20, 80, Line(Square('h', 8), Square('h', 7)))
    val highEvalFork = checkedFork(400, 460, Line(Square('h', 8), Square('h', 6)))
    assertEquals(
      orderShape(StoryTable.choose(Vector(hanging, lowEvalFork))),
      orderShape(StoryTable.choose(Vector(hanging, highEvalFork)))
    )

  test("Fork-7 ExplanationPlan lowers selected Fork Verdict without raw proof input"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val maybePlan = ExplanationPlan.fromSelected(verdict)
    val planSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(maybePlan.flatMap(_.allowedClaim.map(_.key)), Some("forks_two_targets"))
    val plan = maybePlan.get
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Fork))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('b', 5)))
    assertEquals(plan.secondaryTarget, Some(Square('f', 5)))
    assertEquals(plan.anchor, Some(Square('f', 3)))
    assertEquals(plan.route, Some(forkMove))
    assertEquals(plan.evidenceLine, Some(forkMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assert(planSurfaceNames.contains("secondaryTarget"))
    assertEquals(ExplanationClaim.ForkAllowed.map(_.key), Vector("forks_two_targets", "attacks_two_targets"))
    assertEquals(
      ExplanationClaim.ForkForbiddenKeys,
      Vector(
        "wins_material_by_fork",
        "wins_queen",
        "decisive_fork",
        "forced_win",
        "best_move",
        "no_counterplay"
      )
    )
    Vector(
      "wins_material_by_fork",
      "wins_queen",
      "decisive_fork",
      "forced_win",
      "best_move",
      "no_counterplay"
    ).foreach: forbidden =>
      assert(plan.forbiddenWording.map(_.key).contains(forbidden), forbidden)
    Vector("MultiTargetProof", "EngineCheck", "CaptureResult", "BoardFacts", "rawPv", "proofFailures", "sourceRow")
      .foreach: forbiddenName =>
        assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Fork-7 Support Context and Blocked Fork plans are relation only"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val leadVerdict = StoryTable.choose(Vector(fork)).head
    val supportVerdict = leadVerdict.copy(role = Role.Support, leadAllowed = false, rank = 2)
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)
    val engineBlockedVerdict =
      blockedVerdict.copy(engineCheckStatus = Some(EngineCheckStatus.Refutes))

    val supportPlan = ExplanationPlan.fromSelected(supportVerdict).get
    val contextPlan = ExplanationPlan.fromSelected(contextVerdict).get
    val blockedPlan = ExplanationPlan.fromSelected(blockedVerdict).get
    val engineBlockedPlan = ExplanationPlan.fromSelected(engineBlockedVerdict).get

    Vector(supportPlan, contextPlan, blockedPlan, engineBlockedPlan).foreach: plan =>
      assertEquals(plan.allowedClaim, None)
      assertEquals(DeterministicRenderer.fromPlan(plan), None)

    assertEquals(supportPlan.relations.map(_.key), Vector("same_family_lower_rank"))
    assertEquals(contextPlan.relations.map(_.key), Vector("alternative_fork_candidate"))
    assertEquals(blockedPlan.relations, Vector.empty)
    assertEquals(blockedPlan.debugOnly, true)
    assertEquals(engineBlockedPlan.relations.map(_.key), Vector("blocked_by_engine_refute"))
    assertEquals(engineBlockedPlan.debugOnly, true)

  test("Fork-8 DeterministicRenderer phrases Fork ExplanationPlan without stronger meaning"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases =
      Vector(
        "wins queen",
        "wins material",
        "decisive",
        "forced",
        "best move",
        "engine says",
        "no counterplay",
        "blunder"
      )

    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.tactic, Some(Tactic.Fork))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.ForksTwoTargets))
    assertEquals(plan.target, Some(Square('b', 5)))
    assertEquals(plan.secondaryTarget, Some(Square('f', 5)))
    assertEquals(plan.route, Some(forkMove))
    assertEquals(facts.sideLegal.sanFor(forkMove), Some("Nd4"))
    assertEquals(plan.routeSan, Some("Nd4"))
    assertEquals(plan.evidenceLine, Some(forkMove))
    assertEquals(rendered.text, "Nd4 forks the pieces on b5 and f5.")
    assertEquals(rendered.claimKey, "forks_two_targets")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"Fork renderer must not contain forbidden phrase: $phrase")

  test("Fork-9 LLM smoke accepts Fork RenderedLine without raw proof input"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val mockText = LlmNarrationSmoke.mockNarrate(plan, rendered).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val checked = LlmNarrationSmoke.check(plan, rendered, mockText)

    assertEquals(mockText, rendered.text)
    assertEquals(checked.accepted, true)
    assertEquals(checked.violations, Vector.empty)
    Vector(
      "renderedText: Nd4 forks the pieces on b5 and f5.",
      "claimKey: forks_two_targets",
      "strength: bounded",
      "forbiddenWording:",
      "Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Fork prompt must include allowed input: $required")
    Vector(
      "Verdict",
      "Story",
      "MultiTargetProof",
      "EngineCheck",
      "BoardFacts",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "source row",
      "target:",
      "secondaryTarget:",
      "reply map:"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"Fork prompt must not include forbidden raw input label: $forbidden")

  test("Fork-9 LLM smoke rejects stronger Fork rephrases"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    val safe = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces on b5 and f5.")
    val inventedMove = LlmNarrationSmoke.check(plan, rendered, "After Nd4 Kh7, the fork stays.")
    val inventedTactic = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces and starts a skewer.")
    val inventedPlan = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces and starts a plan.")
    val engineBestWinning =
      LlmNarrationSmoke.check(plan, rendered, "The engine says Nd4 is the best move and a winning fork.")
    val forcedDecisiveBlunder =
      LlmNarrationSmoke.check(plan, rendered, "Nd4 is a forced decisive fork after a blunder.")
    val winsQueen = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces and wins the queen.")
    val winsMaterial = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces and wins material.")
    val namesTargets =
      LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the queen on b5 and rook on f5.")
    val coordinateRoute = LlmNarrationSmoke.check(plan, rendered, "After the move from f3 to d4, the fork stays.")
    val compactCoordinateRoute = LlmNarrationSmoke.check(plan, rendered, "After f3d4, the fork stays.")

    assertEquals(safe.accepted, true)
    assertEquals(safe.violations, Vector.empty)
    assertEquals(inventedMove.accepted, false)
    assertEquals(inventedMove.violations.contains("new_move_or_line"), true)
    assertEquals(inventedTactic.accepted, false)
    assertEquals(inventedTactic.violations.contains("new_tactic_or_plan"), true)
    assertEquals(inventedPlan.accepted, false)
    assertEquals(inventedPlan.violations.contains("new_tactic_or_plan"), true)
    assertEquals(engineBestWinning.accepted, false)
    assertEquals(engineBestWinning.violations.contains("engine_mention"), true)
    assertEquals(engineBestWinning.violations.contains("forbidden_wording"), true)
    assertEquals(engineBestWinning.violations.contains("stronger_claim"), true)
    assertEquals(forcedDecisiveBlunder.accepted, false)
    assertEquals(forcedDecisiveBlunder.violations.contains("forbidden_wording"), true)
    assertEquals(forcedDecisiveBlunder.violations.contains("stronger_claim"), true)
    Vector(winsQueen, winsMaterial, namesTargets).foreach: checked =>
      assertEquals(checked.accepted, false)
      assert(checked.violations.contains("forbidden_wording") || checked.violations.contains("stronger_claim"))
    assertEquals(coordinateRoute.accepted, false)
    assertEquals(coordinateRoute.violations.contains("non_san_move_text"), true)
    assertEquals(compactCoordinateRoute.accepted, false)
    assertEquals(compactCoordinateRoute.violations.contains("non_san_move_text"), true)

  test("Fork-8 refuses Fork renderer text without structural Fork plan permission"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val invalidPlans =
      Vector(
        plan.copy(role = Role.Support),
        plan.copy(allowedClaim = Some(ExplanationClaim.AttacksTwoTargets)),
        plan.copy(allowedClaim = None),
        plan.copy(debugOnly = true),
        plan.copy(target = None),
        plan.copy(secondaryTarget = None),
        plan.copy(route = None),
        plan.copy(evidenceLine = None),
        plan.copy(forbiddenWording = Vector.empty)
      )

    invalidPlans.foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None)

  test("Tactic.Hanging writer opens the first narrow positive Story"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))

    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(TacticHanging.WriterOpen, true)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Hanging))
    assertEquals(story.writer, Some(StoryWriter.TacticHanging))
    assertEquals(story.captureResult.exists(_.positiveMaterial), true)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.anchor, Some(Square('d', 4)))
    assertEquals(story.target, Some(Square('e', 5)))
    assertEquals(story.route, Some(capture))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(verdict.proofFailures, Vector.empty)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.role, Role.Lead)

  test("Tactic.Hanging writer keeps the negative corpus silent"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val defendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val pawnTargetFacts = BoardFacts.fromFen("4k3/8/8/4p3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val defendedCapture = Line(Square('e', 4), Square('d', 5))
    val untrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val kingTarget =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 5)),
          Piece(Side.White, Man.Queen, Square('d', 4))
        )
      )

    assertEquals(TacticHanging.write(defendedFacts, defendedCapture), None, "attacked and recaptured equally")
    assertEquals(TacticHanging.write(positiveFacts, Line(Square('d', 4), Square('d', 5))), None, "capture illegal")
    assertEquals(TacticHanging.write(pawnTargetFacts, capture), None, "pawn target scope is closed")
    assertEquals(TacticHanging.write(kingTarget, capture), None, "king target is closed")
    assertEquals(TacticHanging.write(untrusted, capture), None, "same-board proof missing")

    val positiveCapture = CaptureResult.fromBoardFacts(positiveFacts, capture)
    val highProof = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      conversionPrize = 99,
      forcing = 99,
      immediacy = 99
    )
    val noWriter = Story(
      Scene.Tactic,
      tactic = Some(Tactic.Hanging),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 5)),
      anchor = Some(Square('d', 4)),
      route = Some(capture),
      proof = highProof,
      storyProof = StoryProof.fromBoardFacts(positiveFacts, capture),
      captureResult = Some(positiveCapture)
    )
    val writerWithoutCapture = noWriter.copy(writer = Some(StoryWriter.TacticHanging), captureResult = None)
    val captureWithoutStoryProof =
      noWriter.copy(writer = Some(StoryWriter.TacticHanging), storyProof = StoryProof.empty)
    val boardFactOnly = noWriter.copy(writer = None, captureResult = None, storyProof = StoryProof.empty)
    val mismatchedCaptureIdentity =
      noWriter.copy(
        writer = Some(StoryWriter.TacticHanging),
        target = Some(Square('d', 4))
      )

    Vector(noWriter, writerWithoutCapture, captureWithoutStoryProof, boardFactOnly, mismatchedCaptureIdentity).foreach: story =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false)
      assert(verdict.role != Role.Lead)

    val fork = noWriter.copy(tactic = Some(Tactic.Fork), writer = Some(StoryWriter.TacticHanging))
    val material = noWriter.copy(scene = Scene.Material, tactic = None, writer = Some(StoryWriter.TacticHanging))
    val defense = noWriter.copy(scene = Scene.Defense, tactic = None, writer = Some(StoryWriter.TacticHanging))
    val planStory =
      noWriter.copy(scene = Scene.Plan, tactic = None, plan = Some(Plan.CenterBreak), writer = Some(StoryWriter.TacticHanging))

    Vector(fork, material, defense, planStory).foreach: story =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false)
      assert(verdict.role != Role.Lead)

  test("Fork geometry readiness stays inside MultiTargetProof"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Some(Line(Square('f', 3), Square('d', 4)))
    val targetA = Some(Square('b', 5))
    val targetB = Some(Square('f', 5))
    val proof = MultiTargetProof.fromBoardFacts(facts, forkMove, targetA, targetB)

    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.forkMove, forkMove)
    assertEquals(proof.attacker, Some(Piece(Side.White, Man.Knight, Square('f', 3))))
    assertEquals(proof.attackerAfterMove, Some(Piece(Side.White, Man.Knight, Square('d', 4))))
    assertEquals(proof.targetSquares, Vector(Square('b', 5), Square('f', 5)))
    assertEquals(
      proof.targets,
      Vector(Piece(Side.Black, Man.Queen, Square('b', 5)), Piece(Side.Black, Man.Rook, Square('f', 5)))
    )
    assertEquals(proof.targetValues, Vector(900, 500))
    assertEquals(proof.attackedTargetSquaresAfterMove, Vector(Square('b', 5), Square('f', 5)))
    assertEquals(proof.publicClaimAllowed, false)

    val proofSurfaceNames =
      classOf[MultiTargetProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[MultiTargetProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "forkWorks",
      "forkSucceeds",
      "winsMaterial",
      "winsQueen",
      "decisive",
      "forced",
      "bestMove",
      "story",
      "verdict",
      "render",
      "llm"
    ).foreach: forbidden =>
      assert(!proofSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

    val boardFactsSurfaceNames =
      classOf[BoardFacts].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts].getDeclaredFields.map(_.getName).toSet
    assert(!boardFactsSurfaceNames.exists(_.toLowerCase.contains("fork")))

  test("Fork-2 MultiTargetProof owns Fork proof evidence without Story construction"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Some(Line(Square('f', 3), Square('d', 4)))
    val proof =
      MultiTargetProof.fromBoardFacts(facts, forkMove, Some(Square('b', 5)), Some(Square('f', 5)))

    assertEquals(proof.complete, true)
    assertEquals(proof.attacker, Some(Piece(Side.White, Man.Knight, Square('f', 3))))
    assertEquals(proof.forkMove, forkMove)
    assertEquals(proof.forkSquare, Some(Square('d', 4)))
    assertEquals(proof.targetA, Some(Piece(Side.Black, Man.Queen, Square('b', 5))))
    assertEquals(proof.targetB, Some(Piece(Side.Black, Man.Rook, Square('f', 5))))
    assertEquals(proof.targetValues, Vector(900, 500))
    assertEquals(proof.replyMap.map(_.target), proof.targets)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      MultiTargetProof.getClass.getDeclaredMethods.toVector ++ classOf[MultiTargetProof].getDeclaredMethods.toVector
    val storyReturningMethods = proofHomeMethods.filter(_.getReturnType.getSimpleName.contains("Story")).map(_.getName)
    val storyConstructingMethods =
      proofHomeMethods.map(_.getName).filter(name => name == "write" || name == "toStory" || name == "fromStory")
    assertEquals(storyReturningMethods, Vector.empty)
    assertEquals(storyConstructingMethods, Vector.empty)

  test("Fork-3 TacticFork writer admits only proven two-target Stories"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Some(Line(Square('f', 3), Square('d', 4)))
    val targetA = Some(Square('b', 5))
    val targetB = Some(Square('f', 5))
    val story = TacticFork.write(facts, forkMove, targetA, targetB).get
    val proof = story.multiTargetProof.get

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Fork))
    assertEquals(story.writer, Some(StoryWriter.TacticFork))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(story.route, forkMove)
    assertEquals(story.anchor, Some(Square('f', 3)))
    assertEquals(story.target, targetA)
    assertEquals(story.secondaryTarget, targetB)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.forkSquare, Some(Square('d', 4)))
    assertEquals(proof.targetA.map(_.square), Some(Square('b', 5)))
    assertEquals(proof.targetB.map(_.square), Some(Square('f', 5)))
    assertEquals(proof.attackedTargetSquaresAfterMove, proof.targetSquares)

    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)

    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(forkMove.get))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refutedVerdict = StoryTable.choose(Vector(TacticFork.withEngineCheck(story, refutes).get)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)

    val forgedUnprovenRelation =
      story.copy(multiTargetProof = Some(proof.copy(attackedTargetSquaresAfterMove = Vector.empty)))
    val forgedVerdict = StoryTable.choose(Vector(forgedUnprovenRelation)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)

    val writerSurfaceNames =
      TacticFork.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticFork.getClass.getDeclaredFields.map(_.getName).toSet
    Vector(
      "winsQueen",
      "winsMaterial",
      "decisive",
      "forced",
      "bestMove",
      "onlyMove",
      "noCounterplay",
      "blunder",
      "engineSays"
    ).foreach: forbidden =>
      assert(!writerSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    val plan = ExplanationPlan.fromSelected(verdict).get
    assertEquals(plan.allowedClaim.map(_.key), Some("forks_two_targets"))
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)

  test("Fork negative corpus keeps fork-looking false positives silent"):
    val forkMove = Some(Line(Square('f', 3), Square('d', 4)))
    val targetA = Some(Square('b', 5))
    val targetB = Some(Square('f', 5))
    val positiveFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val positive = TacticFork.write(positiveFacts, forkMove, targetA, targetB).get

    assertEquals(positive.scene, Scene.Tactic)
    assertEquals(positive.tactic, Some(Tactic.Fork))
    assertEquals(positive.writer, Some(StoryWriter.TacticFork))
    assertEquals(positive.anchor, Some(Square('f', 3)))
    assertEquals(positive.route, forkMove)
    assertEquals(positive.target, targetA)
    assertEquals(positive.secondaryTarget, targetB)
    assertEquals(positive.multiTargetProof.exists(_.complete), true)
    assertEquals(StoryTable.choose(Vector(positive)).head.role, Role.Lead)

    val noLegalMove = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val noAttacker = BoardFacts.fromFen("7k/8/8/1q3r2/8/8/8/7K w - - 0 1").toOption.get
    val ownTarget = BoardFacts.fromFen("7k/8/8/1q3R2/8/5N2/8/7K w - - 0 1").toOption.get
    val targetNotAttacked = BoardFacts.fromFen("7k/8/8/1q3rb1/8/5N2/8/7K w - - 0 1").toOption.get
    val unsafeForkSquare = BoardFacts.fromFen("7k/6b1/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val oneReplySavesBoth = BoardFacts.fromFen("3r3k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val pawnFork = BoardFacts.fromFen("7k/8/3q1r2/8/4P3/8/8/7K w - - 0 1").toOption.get
    val skewer = BoardFacts.fromFen("r6k/8/q7/8/8/8/8/R6K w - - 0 1").toOption.get
    val queenHitOnly = BoardFacts.fromFen("7k/8/8/1q6/8/5N2/8/7K w - - 0 1").toOption.get

    Vector(
      "legal move 없음" ->
        TacticFork.write(noLegalMove, Some(Line(Square('f', 3), Square('d', 5))), targetA, targetB),
      "attacker 없음" ->
        TacticFork.write(noAttacker, forkMove, targetA, targetB),
      "fork square 없음" ->
        TacticFork.write(positiveFacts, None, targetA, targetB),
      "two targets 없음" ->
        TacticFork.write(positiveFacts, forkMove, targetA, None),
      "duplicated target" ->
        TacticFork.write(positiveFacts, forkMove, targetA, targetA),
      "own piece target" ->
        TacticFork.write(ownTarget, forkMove, targetA, targetB),
      "target not attacked after move" ->
        TacticFork.write(targetNotAttacked, forkMove, targetA, Some(Square('g', 5))),
      "fork square unsafe with no compensation" ->
        TacticFork.write(unsafeForkSquare, forkMove, targetA, targetB),
      "both targets saved by one reply" ->
        TacticFork.write(oneReplySavesBoth, forkMove, targetA, targetB),
      "pawn fork tries to enter Tactic.Fork" ->
        TacticFork.write(
          pawnFork,
          Some(Line(Square('e', 4), Square('e', 5))),
          Some(Square('d', 6)),
          Some(Square('f', 6))
        ),
      "skewer tries to enter Tactic.Fork" ->
        TacticFork.write(
          skewer,
          Some(Line(Square('a', 1), Square('a', 4))),
          Some(Square('a', 6)),
          Some(Square('a', 8))
        ),
      "queen-hit-only tries to enter Tactic.Fork" ->
        TacticFork.write(queenHitOnly, forkMove, targetA, targetB)
    ).foreach: (label, maybeStory) =>
      assertEquals(maybeStory, None, label)

    val highProof = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      conversionPrize = 99,
      forcing = 99,
      immediacy = 99
    )
    val storyProofIncomplete = positive.copy(storyProof = StoryProof.empty)
    val multiTargetProofIncomplete = positive.copy(multiTargetProof = None)
    val bothTargetsSavedByOneReply =
      positive.copy(
        multiTargetProof =
          positive.multiTargetProof.map: proof =>
            proof.copy(replyMap = proof.replyMap.map(reply => reply.copy(savedByOneReply = true)))
      )
    val highProofOnly = Story(
      Scene.Tactic,
      tactic = Some(Tactic.Fork),
      side = Side.White,
      rival = Side.Black,
      target = targetA,
      secondaryTarget = targetB,
      anchor = Some(Square('f', 3)),
      route = forkMove,
      proof = highProof,
      storyProof = StoryProof.fromBoardFacts(positiveFacts, forkMove.get)
    )
    val engineRefutes = EngineCheck.fromStory(
      facts = positiveFacts,
      story = Some(positive),
      engineLine = Some(EngineLine(Vector(forkMove.get))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(120)),
      evalAfter = Some(EngineEval(-400)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val requestedRefute = EngineCheck.fromStory(
      facts = positiveFacts,
      story = Some(positive),
      engineLine = Some(EngineLine(Vector(forkMove.get))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )

    Vector(
      "StoryProof incomplete" -> storyProofIncomplete,
      "MultiTargetProof incomplete" -> multiTargetProofIncomplete,
      "both targets saved by one reply" -> bothTargetsSavedByOneReply,
      "high Proof score only" -> highProofOnly
    ).foreach: (label, story) =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assert(verdict.role != Role.Lead, label)

    Vector(engineRefutes, requestedRefute).foreach: check =>
      assertEquals(check.status, EngineCheckStatus.Refutes)
      val verdict = StoryTable.choose(Vector(TacticFork.withEngineCheck(positive, check).get)).head
      assertEquals(verdict.leadAllowed, false)
      assertEquals(verdict.role, Role.Blocked)

  test("Line-1 LineProof proves one legal discovered slider attack without creating Story"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Some(Line(Square('d', 3), Square('f', 4)))
    val proof = LineProof.fromBoardFacts(
      facts = facts,
      revealingMove = revealMove,
      sliderSquare = Some(Square('b', 1)),
      targetSquare = Some(Square('g', 6))
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.White)
    assertEquals(proof.slider, Some(Piece(Side.White, Man.Bishop, Square('b', 1))))
    assertEquals(proof.blocker, Some(Piece(Side.White, Man.Knight, Square('d', 3))))
    assertEquals(proof.movedPiece, Some(Piece(Side.White, Man.Knight, Square('d', 3))))
    assertEquals(proof.revealedTarget, Some(Piece(Side.Black, Man.Rook, Square('g', 6))))
    assertEquals(proof.revealingMove, revealMove)
    assertEquals(proof.lineKind, Some(BoardFacts.LineKind.Diagonal))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.beforeLineBlockedOrInactive, true)
    assertEquals(proof.afterSliderAttacksTarget, true)
    assertEquals(proof.targetNonKingMaterial, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      LineProof.getClass.getDeclaredMethods.toVector ++ classOf[LineProof].getDeclaredMethods.toVector
    val publicOutputReturningMethods = proofHomeMethods
      .filter(method =>
        Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine").exists(name =>
          method.getReturnType.getSimpleName.contains(name)
        )
      )
      .map(_.getName)
    assertEquals(publicOutputReturningMethods, Vector.empty)

    val surfaceNames =
      proofHomeMethods.map(_.getName).toSet ++
        LineProof.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "pin",
      "pressure",
      "winsMaterial",
      "winning",
      "decisive",
      "blunder",
      "bestMove",
      "forcedLine"
    ).foreach: forbidden =>
      assert(!surfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Line-1 LineProof keeps false positives and diagnostics out of public output"):
    val kingTargetFacts = BoardFacts.fromFen("8/8/6k1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Some(Line(Square('d', 3), Square('f', 4)))
    val kingTargetProof = LineProof.fromBoardFacts(
      facts = kingTargetFacts,
      revealingMove = revealMove,
      sliderSquare = Some(Square('b', 1)),
      targetSquare = Some(Square('g', 6))
    )

    assertEquals(kingTargetProof.complete, false)
    assertEquals(kingTargetProof.publicClaimAllowed, false)
    assertEquals(
      kingTargetProof.missingEvidence.exists(_.missing.contains("target non-king material piece")),
      true
    )

    val legalMoveMissing = LineProof.fromBoardFacts(
      facts = kingTargetFacts,
      revealingMove = Some(Line(Square('d', 3), Square('d', 4))),
      sliderSquare = Some(Square('b', 1)),
      targetSquare = Some(Square('g', 6))
    )
    assertEquals(legalMoveMissing.complete, false)
    assertEquals(
      legalMoveMissing.missingEvidence.exists(_.missing.contains("legal revealing move")),
      true
    )

    val highProof = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      conversionPrize = 99,
      forcing = 99,
      immediacy = 99
    )
    val forgedStory = Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.DiscoveredAttack),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('g', 6)),
      anchor = Some(Square('b', 1)),
      route = revealMove,
      routeSan = BoardFacts.sanFor(kingTargetFacts, revealMove.get),
      proof = highProof,
      storyProof = StoryProof.fromBoardFacts(kingTargetFacts, revealMove.get)
    )
    val forgedVerdict = StoryTable.choose(Vector(forgedStory)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None)

  test("Line-2 TacticDiscoveredAttack writer admits one proof-backed revealed slider Story"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(
        facts = facts,
        revealingMove = Some(revealMove),
        sliderSquare = Some(Square('b', 1)),
        targetSquare = Some(Square('g', 6))
      )
      .get
    val proof = story.lineProof.get

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.DiscoveredAttack))
    assertEquals(story.writer, Some(StoryWriter.TacticDiscoveredAttack))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('g', 6)))
    assertEquals(story.anchor, Some(Square('d', 3)))
    assertEquals(story.route, Some(revealMove))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.revealingMove, Some(revealMove))
    assertEquals(proof.movedPiece.map(_.square), Some(Square('d', 3)))
    assertEquals(proof.slider.map(_.square), Some(Square('b', 1)))
    assertEquals(proof.revealedTarget.map(_.square), Some(Square('g', 6)))
    assertEquals(proof.afterSliderAttacksTarget, true)

    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    val plan = ExplanationPlan.fromSelected(verdict).get
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(DeterministicRenderer.fromPlan(plan).map(_.text), Some("Nf4 reveals an attack on the piece on g6."))

    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(120)),
      evalAfter = Some(EngineEval(-120)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    val refutedVerdict = StoryTable.choose(Vector(TacticDiscoveredAttack.withEngineCheck(story, refutes).get)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)

  test("Line-2 TacticDiscoveredAttack writer keeps sibling line tactics and king targets silent"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val positive = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get

    val kingTargetFacts = BoardFacts.fromFen("8/8/6k1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    assertEquals(
      TacticDiscoveredAttack.write(
        kingTargetFacts,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6))
      ),
      None
    )
    assertEquals(
      TacticDiscoveredAttack.write(
        facts,
        Some(Line(Square('d', 3), Square('d', 4))),
        Some(Square('b', 1)),
        Some(Square('g', 6))
      ),
      None
    )

    val incompleteStoryProof = positive.copy(storyProof = StoryProof.empty)
    assertEquals(StoryTable.choose(Vector(incompleteStoryProof)).head.role, Role.Blocked)

    Vector(Tactic.AbsPin, Tactic.RelPin, Tactic.Skewer, Tactic.Xray, Tactic.RemoveGuard).foreach: tactic =>
      val forged = positive.copy(tactic = Some(tactic))
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, tactic.toString)
      assertEquals(verdict.leadAllowed, false, tactic.toString)

    val writerSurfaceNames =
      TacticDiscoveredAttack.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticDiscoveredAttack.getClass.getDeclaredFields.map(_.getName).toSet
    Vector("pin", "skewer", "xray", "removeGuard", "mate", "kingSafety", "winning", "decisive", "blunder")
      .foreach: forbidden =>
        assert(!writerSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Line-3 negative corpus keeps revealed attack false positives silent"):
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val positiveFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val sameBoardMissingFacts = minimalBoardFacts(
      sideToMove = Side.White,
      sideLegal = Moves(known = true, lines = Vector(revealMove), san = Vector("Nf4"), moveCount = 1),
      rivalLegal = Moves(known = true, lines = Vector(Line(Square('h', 8), Square('h', 7))), san = Vector("Kh7"), moveCount = 1),
      pieces = Vector(
        Piece(Side.White, Man.King, Square('h', 1)),
        Piece(Side.White, Man.Bishop, Square('b', 1)),
        Piece(Side.White, Man.Knight, Square('d', 3)),
        Piece(Side.Black, Man.King, Square('h', 8)),
        Piece(Side.Black, Man.Rook, Square('g', 6))
      )
    )
    val corpus = Vector(
      (
        "legal move missing",
        positiveFacts,
        Some(Line(Square('d', 3), Square('d', 4))),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "legal revealing move"
      ),
      (
        "same-board proof missing",
        sameBoardMissingFacts,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "same-board proof"
      ),
      (
        "blocker moves but line remains closed",
        BoardFacts.fromFen("7k/8/6r1/8/8/3Q4/8/1B5K w - - 0 1").toOption.get,
        Some(Line(Square('d', 3), Square('e', 4))),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "before-move blocked or inactive line"
      ),
      (
        "target not attacked after move",
        BoardFacts.fromFen("7k/7r/8/8/8/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('h', 6)),
        "after-move slider attack"
      ),
      (
        "slider is not a slider",
        BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1N5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "slider piece"
      ),
      (
        "target is king",
        BoardFacts.fromFen("8/8/6k1/8/8/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "target non-king material piece"
      ),
      (
        "another piece still blocks after the blocker moves",
        BoardFacts.fromFen("7k/8/6r1/8/4P3/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "after-move slider attack"
      ),
      (
        "discovered-looking move has no target",
        BoardFacts.fromFen("7k/8/8/8/8/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "revealed target"
      )
    )

    corpus.foreach: (label, facts, move, slider, target, expectedMissing) =>
      val lineProof = LineProof.fromBoardFacts(facts, move, slider, target)
      assertEquals(lineProof.complete, false, label)
      assertEquals(
        lineProof.missingEvidence.exists(_.missing.contains(expectedMissing)),
        true,
        label
      )
      assertEquals(TacticDiscoveredAttack.write(facts, move, slider, target), None, label)

  test("Line-3 negative corpus blocks wording drift and sibling line classifications"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val positive = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val lineProofSurface =
      positive.lineProof.get.missingEvidence.flatMap(_.missing).mkString(" ").toLowerCase
    val writerSurfaceNames =
      (TacticDiscoveredAttack.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticDiscoveredAttack.getClass.getDeclaredFields.map(_.getName).toSet)
        .map(_.toLowerCase)
    val closedWording = Vector("pressure", "initiative", "mate")

    closedWording.foreach: forbidden =>
      assert(!lineProofSurface.contains(forbidden), forbidden)
      assert(!writerSurfaceNames.exists(_.contains(forbidden)), forbidden)

    val initiativeForgery = positive.copy(scene = Scene.Initiative, plan = Some(Plan.Initiative))
    val initiativeVerdict = StoryTable.choose(Vector(initiativeForgery)).head
    assertEquals(initiativeVerdict.role, Role.Blocked)
    assertEquals(initiativeVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(initiativeVerdict), None)

    Vector(Tactic.AbsPin, Tactic.RelPin, Tactic.Skewer, Tactic.Xray, Tactic.RemoveGuard).foreach: tactic =>
      val forged = positive.copy(tactic = Some(tactic))
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, tactic.toString)
      assertEquals(verdict.leadAllowed, false, tactic.toString)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, tactic.toString)

  test("Line-4 DiscoveredAttack reuses EngineCheck statuses without creating claims"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val reply = Line(Square('h', 8), Square('h', 7))

    def check(
        status: EngineCheckStatus,
        before: Int = 20,
        after: Int = 20,
        storyInput: Option[Story] = Some(story),
        engineLine: Option[EngineLine] = Some(EngineLine(Vector(revealMove))),
        replyLine: Option[EngineLine] = Some(EngineLine(Vector(reply))),
        depth: Option[Int] = Some(18),
        freshnessPly: Option[Int] = Some(0)
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = storyInput,
        engineLine = engineLine,
        replyLine = replyLine,
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = depth,
        freshnessPly = freshnessPly,
        requestedStatus = status
      )

    val unknown = TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes =
      TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    Vector(unknown, supports, caps, refutes).foreach: checkedStory =>
      assertEquals(checkedStory.engineCheck.exists(_.publicClaimAllowed), false, checkedStory.toString)

    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get
    assertEquals(unknownPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(DeterministicRenderer.fromPlan(unknownPlan).map(_.text), Some("Nf4 reveals an attack on the piece on g6."))

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    assertEquals(supportsPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(DeterministicRenderer.fromPlan(supportsPlan).map(_.text), Some("Nf4 reveals an attack on the piece on g6."))

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

  test("Line-4 EngineCheck cannot create or rank DiscoveredAttack from raw engine evidence"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val wrongRoute = Line(Square('h', 1), Square('g', 1))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val reply = Line(Square('h', 8), Square('h', 7))

    val standaloneEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(revealMove),
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(standaloneEvidence.evidenceReady, true)
    assertEquals(standaloneEvidence.storyBound, false)
    assertEquals(standaloneEvidence.status, EngineCheckStatus.Supports)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(story, standaloneEvidence), None)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(story, engineOnly), None)
    assertEquals(routeMismatch.status, EngineCheckStatus.Unknown)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(story, routeMismatch), None)

    val lowEvalCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(-850)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val highEvalCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(850)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val lowEvalVerdict =
      StoryTable.choose(Vector(TacticDiscoveredAttack.withEngineCheck(story, lowEvalCheck).get)).head
    val highEvalVerdict =
      StoryTable.choose(Vector(TacticDiscoveredAttack.withEngineCheck(story, highEvalCheck).get)).head

    assertEquals(lowEvalVerdict.role, highEvalVerdict.role)
    assertEquals(lowEvalVerdict.strength, highEvalVerdict.strength)
    assertEquals(lowEvalVerdict.engineCheckStatus, highEvalVerdict.engineCheckStatus)
    assertEquals(
      ExplanationPlan.fromSelected(lowEvalVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RevealsAttackOnPiece)
    )
    assertEquals(
      ExplanationPlan.fromSelected(highEvalVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RevealsAttackOnPiece)
    )

  test("Line-5 StoryTable keeps DiscoveredAttack stable against existing rows"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get.copy(proof = strongProof(96))
    val material = SceneMaterial.write(hangingFacts, hangingMove).get.copy(proof = strongProof(99))
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get.copy(proof = strongProof(94))
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get.copy(proof = strongProof(93))
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(95))

    val rows =
      Vector(
        "Tactic.Hanging" -> hanging,
        "Tactic.Fork" -> fork,
        "Scene.Material" -> material,
        "Scene.Defense" -> defense,
        "Tactic.DiscoveredAttack" -> discovered
      )

    def rowId(story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim.map(_.key))
        )

    val forward = StoryTable.choose(rows.map(_._2))
    val reverse = StoryTable.choose(rows.reverse.map(_._2))
    val shuffled = StoryTable.choose(Vector(rows(3), rows(1), rows(4), rows(2), rows(0)).map(_._2))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(forward.map(verdict => rowId(verdict.story)).toSet, rows.map(_._1).toSet)
    assertEquals(forward.find(_.story == discovered).map(_.role), Some(Role.Support))
    assertEquals(ExplanationPlan.fromSelected(forward.find(_.story == discovered).get), None)

    val materialVerdict = forward.find(_.story == material).get
    assertEquals(material.target, hanging.target)
    assertEquals(material.route, hanging.route)
    assertEquals(materialVerdict.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(materialVerdict).flatMap(_.allowedClaim), None)
    assertEquals(DeterministicRenderer.fromPlan(ExplanationPlan.fromSelected(materialVerdict).get), None)

  test("Line-5 invalid Defense and incomplete Fork cannot block or absorb DiscoveredAttack"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(95))
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val validDefense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val defenseWithoutThreat = validDefense.copy(threatProof = None, proof = strongProof(100))
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val validFork =
      TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val forkWithoutTwoTargetProof = validFork.copy(multiTargetProof = None, proof = strongProof(100))

    val verdicts = StoryTable.choose(Vector(defenseWithoutThreat, forkWithoutTwoTargetProof, discovered))
    val discoveredVerdict = verdicts.find(_.story == discovered).get
    val defenseVerdict = verdicts.find(_.story == defenseWithoutThreat).get
    val forkVerdict = verdicts.find(_.story == forkWithoutTwoTargetProof).get
    val defensePlan = ExplanationPlan.fromSelected(defenseVerdict).get
    val forkPlan = ExplanationPlan.fromSelected(forkVerdict).get

    assertEquals(discoveredVerdict.role, Role.Lead)
    assertEquals(discoveredVerdict.leadAllowed, true)
    val discoveredPlan = ExplanationPlan.fromSelected(discoveredVerdict).get
    assertEquals(discoveredPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(DeterministicRenderer.fromPlan(discoveredPlan).map(_.text), Some("Nf4 reveals an attack on the piece on g6."))
    assertEquals(defenseVerdict.role, Role.Blocked)
    assertEquals(defensePlan.allowedClaim, None)
    assertEquals(defensePlan.debugOnly, true)
    assertEquals(DeterministicRenderer.fromPlan(defensePlan), None)
    assertEquals(forkVerdict.role, Role.Blocked)
    assertEquals(forkPlan.allowedClaim, None)
    assertEquals(forkPlan.debugOnly, true)
    assertEquals(DeterministicRenderer.fromPlan(forkPlan), None)
    assertEquals(verdicts.count(_.role == Role.Lead), 1)

  test("Line-6 ExplanationPlan lowers selected DiscoveredAttack Lead only"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.DiscoveredAttack))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('g', 6)))
    assertEquals(plan.anchor, Some(Square('d', 3)))
    assertEquals(plan.route, Some(revealMove))
    assertEquals(plan.routeSan, Some("Nf4"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(plan.evidenceLine, Some(revealMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(ExplanationClaim.DiscoveredAttackAllowed.map(_.key), Vector("reveals_attack_on_piece"))
    assertEquals(
      ExplanationClaim.DiscoveredAttackForbiddenKeys,
      Vector(
        "wins_material",
        "pins_piece",
        "skewers_piece",
        "creates_pressure",
        "takes_initiative",
        "mate_threat",
        "best_move",
        "forced",
        "decisive"
      )
    )
    val forbiddenWordingKeys = plan.forbiddenWording.map(_.key)
    ExplanationClaim.DiscoveredAttackForbiddenKeys.foreach: forbiddenKey =>
      assert(forbiddenWordingKeys.contains(forbiddenKey), forbiddenKey)
    assert(forbiddenWordingKeys.contains("winning"))
    assertEquals(
      ExplanationClaim.DiscoveredAttackAllowed.map(_.key).toSet
        .intersect(ExplanationClaim.DiscoveredAttackForbiddenKeys.toSet)
        .isEmpty,
      true
    )
    assertEquals(
      LlmNarrationSmoke.mockNarrate(plan, DeterministicRenderer.fromPlan(plan)),
      Some("Nf4 reveals an attack on the piece on g6.")
    )

  test("Line-6 DiscoveredAttack ExplanationPlan gives no standalone claim to non Lead capped or refuted rows"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(revealMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val leadVerdict = StoryTable.choose(Vector(story)).head
    val supportVerdict = leadVerdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false)
    val unselectedVerdict = leadVerdict.copy(selected = false)
    val capped = TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refuted =
      TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    val refutedVerdict = StoryTable.choose(Vector(refuted)).head

    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get.copy(proof = strongProof(99))
    val supportStory = story.copy(proof = strongProof(90))
    val mixedVerdicts = StoryTable.choose(Vector(supportStory, hanging))
    val realSupportVerdict = mixedVerdicts.find(_.story == supportStory).get

    assertEquals(ExplanationPlan.fromSelected(supportVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(contextVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)
    assertEquals(realSupportVerdict.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(realSupportVerdict), None)

  test("Line-7 DeterministicRenderer phrases selected DiscoveredAttack ExplanationPlan only"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val forbiddenPhrases =
      Vector(
        "wins material",
        "winning",
        "decisive",
        "best move",
        "forces",
        "pins",
        "skewers",
        "puts pressure",
        "creates a mating threat"
      )

    assertEquals(rendered.text, "Nf4 reveals an attack on the piece on g6.")
    assertEquals(rendered.claimKey, "reveals_attack_on_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assert(rendererMethods.contains("fromPlan"))
    assert(!rendererMethods.contains("fromStory"))
    assert(!rendererMethods.contains("fromVerdict"))
    assert(!rendererMethods.contains("fromBoardFacts"))
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

  test("Line-7 DeterministicRenderer refuses DiscoveredAttack without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val leadPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    Vector(
      leadPlan.copy(role = Role.Support),
      leadPlan.copy(role = Role.Context),
      leadPlan.copy(role = Role.Blocked, debugOnly = true),
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.CanWinPiece)),
      leadPlan.copy(tactic = Some(Tactic.Hanging)),
      leadPlan.copy(scene = Scene.Material, tactic = None),
      leadPlan.copy(strength = ExplanationStrength.Bounded, evidenceLine = None),
      leadPlan.copy(route = None),
      leadPlan.copy(routeSan = None),
      leadPlan.copy(target = None)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None, invalidPlan.toString)

  test("Line-8 LLM smoke reuses 8B prompt contract for DiscoveredAttack RenderedLine only"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text), NarrationSmokeCheck(true, Vector.empty))
    assert(prompt.contains("renderedText: Nf4 reveals an attack on the piece on g6."))
    assert(prompt.contains("claimKey: reveals_attack_on_piece"))
    assert(prompt.contains("strength: bounded"))
    assert(prompt.contains("forbiddenWording: wins material, winning, decisive, best move, only move, forced, cannot move, no defense, front piece must move, wins rear piece, creates pressure, takes initiative, mate threat, king unsafe, pins piece, skewers piece"))
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("raw Story", "LineProof", "LineFact", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), forbiddenInput)

  test("Line-8 LLM smoke rejects DiscoveredAttack additions and stronger claims"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val invalidOutputs =
      Vector(
        "Nf4 wins material on g6." -> "forbidden_wording",
        "Nf4 creates pressure on the piece on g6." -> "forbidden_wording",
        "Nf4 takes initiative against g6." -> "forbidden_wording",
        "Nf4 creates a mating threat after attacking g6." -> "forbidden_wording",
        "Nf4 forces a new line after attacking g6." -> "forbidden_wording",
        "Nf4 pins the piece on g6." -> "forbidden_wording",
        "Nf4 reveals an attack on g6, then Bg2 adds another line." -> "new_move_or_line",
        "Nf4 reveals an attack on g6 because Black is winning." -> "forbidden_wording"
      )

    invalidOutputs.foreach: (output, expectedViolation) =>
      val checked = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(checked.accepted, false, output)
      assert(checked.violations.contains(expectedViolation), output)

    val mismatchedRendered = rendered.copy(claimKey = "wins_material")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(
      LlmNarrationSmoke.check(plan, mismatchedRendered, rendered.text).violations.contains("input_mismatch"),
      true
    )

  test("Line Closeout hard cleanup keeps authority homes separated and public surfaces closed"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val lineProof = story.lineProof.get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val lineFactSurface =
      classOf[BoardFacts.LineFact].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.LineFact].getDeclaredFields.map(_.getName).toSet
    val lineProofSurface =
      classOf[LineProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    val writerNames = StoryWriter.values.map(_.toString).toVector
    val rendererMethodInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(!lineFactSurface.exists(_.toLowerCase.contains(forbidden)), forbidden)
    Vector("CaptureResult", "MultiTargetProof", "ThreatProof", "DefenseProof", "EngineCheck").foreach: forbidden =>
      assert(!lineProofSurface.exists(_.contains(forbidden)), forbidden)
    assertEquals(lineProof.publicClaimAllowed, false)
    assertEquals(lineProof.complete, true)
    assertEquals(story.lineProof.nonEmpty, true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.writer, Some(StoryWriter.TacticDiscoveredAttack))
    assert(!writerNames.exists(name => Vector("Line", "Ray", "XRay").exists(name.contains)))

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("reveals_attack_on_piece"))
    assertEquals(ExplanationClaim.DiscoveredAttackAllowed.map(_.key), Vector("reveals_attack_on_piece"))
    assert(!ExplanationClaim.DiscoveredAttackAllowed.map(_.key).contains("wins_material"))
    assertEquals(rendered.claimKey, "reveals_attack_on_piece")
    assertEquals(rendered.text, "Nf4 reveals an attack on the piece on g6.")
    assertEquals(rendererMethodInputs, Vector("ExplanationPlan"))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    Vector(
      "Nf4 wins material on g6.",
      "Nf4 pins the piece on g6.",
      "Nf4 skewers the piece on g6.",
      "Nf4 creates pressure on g6.",
      "Nf4 creates a mating threat on g6.",
      "Nf4 is the best move and forces a line."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector("LineProof", "LineFact", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach: forbidden =>
      assert(!prompt.contains(forbidden), forbidden)
    Vector(Tactic.AbsPin, Tactic.RelPin, Tactic.Skewer, Tactic.Xray, Tactic.RemoveGuard).foreach: tactic =>
      val forged = story.copy(tactic = Some(tactic))
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked, tactic.toString)
      assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None, tactic.toString)

  test("Pin-1 PinProof proves one legal pinned-to-king relation without creating Story"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Some(Line(Square('a', 8), Square('e', 8)))
    val proof = PinProof.fromBoardFacts(
      facts = facts,
      pinningMove = pinningMove,
      sliderSquare = Some(Square('e', 8)),
      targetSquare = Some(Square('e', 2)),
      kingSquare = Some(Square('e', 1))
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.Black)
    assertEquals(proof.pinnedTarget, Some(Piece(Side.White, Man.Knight, Square('e', 2))))
    assertEquals(proof.pinningSlider, Some(Piece(Side.Black, Man.Rook, Square('e', 8))))
    assertEquals(proof.kingBehindTarget, Some(Piece(Side.White, Man.King, Square('e', 1))))
    assertEquals(proof.pinningMove, pinningMove)
    assertEquals(proof.lineKind, Some(BoardFacts.LineKind.File))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.beforePinRelation, false)
    assertEquals(proof.afterPinRelation, true)
    assertEquals(proof.targetNonKing, true)
    assertEquals(proof.targetAndKingSameSide, true)
    assertEquals(proof.sliderAttacksThroughTargetTowardKingAfterMove, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      PinProof.getClass.getDeclaredMethods.toVector ++ classOf[PinProof].getDeclaredMethods.toVector
    val publicOutputReturningMethods = proofHomeMethods
      .filter(method =>
        Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine").exists(name =>
          method.getReturnType.getSimpleName.contains(name)
        )
      )
      .map(_.getName)
    assertEquals(publicOutputReturningMethods, Vector.empty)

    val surfaceNames =
      proofHomeMethods.map(_.getName).toSet ++
        PinProof.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[PinProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "material",
      "kingUnsafe",
      "mate",
      "pressure",
      "initiative",
      "winning",
      "decisive",
      "bestMove",
      "forcedMove",
      "cannotMove"
    ).foreach: forbidden =>
      assert(!surfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Pin-1 PinProof keeps false positives and diagnostics out of public output"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Some(Line(Square('a', 8), Square('e', 8)))
    val illegalMove = PinProof.fromBoardFacts(
      facts = facts,
      pinningMove = Some(Line(Square('a', 8), Square('e', 7))),
      sliderSquare = Some(Square('e', 7)),
      targetSquare = Some(Square('e', 2)),
      kingSquare = Some(Square('e', 1))
    )
    val targetIsKing = PinProof.fromBoardFacts(
      facts = facts,
      pinningMove = pinningMove,
      sliderSquare = Some(Square('e', 8)),
      targetSquare = Some(Square('e', 1)),
      kingSquare = Some(Square('e', 1))
    )
    val lineNotCreated = PinProof.fromBoardFacts(
      facts = facts,
      pinningMove = Some(Line(Square('a', 8), Square('a', 7))),
      sliderSquare = Some(Square('a', 7)),
      targetSquare = Some(Square('e', 2)),
      kingSquare = Some(Square('e', 1))
    )

    assertEquals(illegalMove.complete, false)
    assertEquals(illegalMove.publicClaimAllowed, false)
    assertEquals(
      illegalMove.missingEvidence.exists(_.missing.contains("legal pinning or revealing move")),
      true
    )
    assertEquals(targetIsKing.complete, false)
    assertEquals(targetIsKing.missingEvidence.exists(_.missing.contains("target non-king")), true)
    assertEquals(lineNotCreated.complete, false)
    assertEquals(
      lineNotCreated.missingEvidence.exists(_.missing.contains("before/after pin relation")),
      true
    )

    val highProof = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      conversionPrize = 99,
      forcing = 99,
      immediacy = 99
    )
    val forgedStory = Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.AbsPin),
      side = Side.Black,
      rival = Side.White,
      target = Some(Square('e', 2)),
      anchor = Some(Square('e', 8)),
      route = pinningMove,
      routeSan = BoardFacts.sanFor(facts, pinningMove.get),
      proof = highProof,
      storyProof = StoryProof.fromBoardFacts(facts, pinningMove.get)
    )
    val forgedVerdict = StoryTable.choose(Vector(forgedStory)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None)

  test("Pin-2 TacticPin writer admits one proof-backed Pin Story"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story = TacticPin
      .write(
        facts = facts,
        pinningMove = Some(pinningMove),
        sliderSquare = Some(Square('e', 8)),
        targetSquare = Some(Square('e', 2)),
        kingSquare = Some(Square('e', 1))
      )
      .get
    val proof = story.pinProof.get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Pin))
    assertEquals(story.writer, Some(StoryWriter.TacticPin))
    assertEquals(story.side, Side.Black)
    assertEquals(story.rival, Side.White)
    assertEquals(story.target, Some(Square('e', 2)))
    assertEquals(story.anchor, Some(Square('e', 8)))
    assertEquals(story.route, Some(pinningMove))
    assertEquals(story.routeSan, BoardFacts.sanFor(facts, pinningMove))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.afterPinRelation, true)
    assertEquals(proof.pinnedTarget, Some(Piece(Side.White, Man.Knight, Square('e', 2))))
    assertEquals(proof.pinningSlider, Some(Piece(Side.Black, Man.Rook, Square('e', 8))))
    assertEquals(proof.kingBehindTarget, Some(Piece(Side.White, Man.King, Square('e', 1))))
    assertEquals(story.captureResult, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), Some(ExplanationClaim.PinsPiece))
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("pins_piece"))

  test("Pin-2 TacticPin writer blocks refuted unsupported and sibling-meaning rows"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story = TacticPin
      .write(
        facts = facts,
        pinningMove = Some(pinningMove),
        sliderSquare = Some(Square('e', 8)),
        targetSquare = Some(Square('e', 2)),
        kingSquare = Some(Square('e', 1))
      )
      .get
    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(pinningMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 1), Square('f', 1))))),
      evalBefore = Some(EngineEval(80)),
      evalAfter = Some(EngineEval(90)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refuted = TacticPin.withEngineCheck(story, refutes).get
    val targetKing =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 1)), Some(Square('e', 1)))
    val noPin =
      TacticPin.write(
        facts,
        Some(Line(Square('a', 8), Square('a', 7))),
        Some(Square('a', 7)),
        Some(Square('e', 2)),
        Some(Square('e', 1))
      )
    val forgedMaterial = story.copy(scene = Scene.Material, tactic = None)
    val forgedDefense = story.copy(scene = Scene.Defense, tactic = None)
    val forgedRemoveGuard = story.copy(tactic = Some(Tactic.RemoveGuard))
    val forgedAbsPin = story.copy(tactic = Some(Tactic.AbsPin))

    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    assertEquals(StoryTable.choose(Vector(refuted)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(refuted)).head.leadAllowed, false)
    assertEquals(targetKing, None)
    assertEquals(noPin, None)
    Vector(forgedMaterial, forgedDefense).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, forged.toString)
      assertEquals(verdict.leadAllowed, false, forged.toString)
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, forged.toString)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, forged.toString)
    Vector(forgedRemoveGuard, forgedAbsPin).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, forged.toString)
      assertEquals(verdict.leadAllowed, false, forged.toString)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, forged.toString)

  test("Pin-3 negative corpus keeps incomplete pin relations silent"):
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val untrustedFacts =
      minimalBoardFacts(
        sideToMove = Side.Black,
        sideLegal = readyMoves(line = pinningMove),
        pieces = Vector(
          Piece(Side.Black, Man.Rook, Square('a', 8)),
          Piece(Side.Black, Man.King, Square('g', 8)),
          Piece(Side.White, Man.Knight, Square('e', 2)),
          Piece(Side.White, Man.King, Square('e', 1))
        )
      )
    val nonSliderFacts = BoardFacts.fromFen("n5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val targetWithoutKingFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/3K4 b - - 0 1").toOption.get
    val oppositeKingFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4n3/4K3 b - - 0 1").toOption.get
    val blockerFacts = BoardFacts.fromFen("r5k1/8/8/8/4B3/8/4N3/4K3 b - - 0 1").toOption.get
    val alreadyPinnedFacts = BoardFacts.fromFen("4r1k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val alreadyPinnedMove = Line(Square('g', 8), Square('h', 8))

    def write(
        facts: BoardFacts,
        move: Line,
        slider: Square,
        target: Square,
        king: Square
    ): Option[Story] =
      TacticPin.write(facts, Some(move), Some(slider), Some(target), Some(king))

    val sameBoardProof =
      PinProof.fromBoardFacts(
        untrustedFacts,
        Some(pinningMove),
        Some(Square('e', 8)),
        Some(Square('e', 2)),
        Some(Square('e', 1))
      )

    Vector(
      "illegal move" ->
        write(pinFacts, Line(Square('a', 8), Square('a', 7)), Square('a', 7), Square('e', 2), Square('e', 1)),
      "missing same-board proof" ->
        write(untrustedFacts, pinningMove, Square('e', 8), Square('e', 2), Square('e', 1)),
      "slider is not a slider" ->
        write(nonSliderFacts, Line(Square('a', 8), Square('b', 6)), Square('b', 6), Square('e', 2), Square('e', 1)),
      "no king behind target" ->
        write(pinFacts, pinningMove, Square('e', 8), Square('e', 2), Square('d', 1)),
      "target and king are not same side" ->
        write(oppositeKingFacts, pinningMove, Square('e', 8), Square('e', 2), Square('e', 1)),
      "line does not run through target to king" ->
        write(targetWithoutKingFacts, pinningMove, Square('e', 8), Square('e', 2), Square('d', 1)),
      "target is king" ->
        write(pinFacts, pinningMove, Square('e', 8), Square('e', 1), Square('e', 1)),
      "another blocker sits between slider and king" ->
        write(blockerFacts, pinningMove, Square('e', 8), Square('e', 2), Square('e', 1)),
      "pin-looking geometry already existed before move" ->
        write(alreadyPinnedFacts, alreadyPinnedMove, Square('e', 8), Square('e', 2), Square('e', 1))
    ).foreach: (label, story) =>
      assertEquals(story, None, label)

    assertEquals(sameBoardProof.sameBoardProof, false)
    assertEquals(sameBoardProof.complete, false)
    assert(sameBoardProof.missingEvidence.exists(_.missing.contains("same-board proof")))

  test("Pin-3 negative corpus keeps sibling line tactics and pin wording silent"):
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val skewerFacts = BoardFacts.fromFen("4q3/4k3/8/8/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pinStory =
      TacticPin.write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val pinVerdict = StoryTable.choose(Vector(pinStory)).head

    assertEquals(
      TacticPin.write(
        discoveredFacts,
        Some(discoveredMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 8))
      ),
      None,
      "discovered attack without king-behind-target relation is not Pin"
    )
    assertEquals(
      TacticPin.write(
        skewerFacts,
        Some(skewerMove),
        Some(Square('e', 1)),
        Some(Square('e', 8)),
        Some(Square('e', 7))
      ),
      None,
      "skewer-looking front-king line is not Pin"
    )
    assertEquals(pinVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(pinVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.PinsPiece))
    assertEquals(ExplanationPlan.fromSelected(pinVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey), Some("pins_piece"))

  test("Pin-4 EngineCheck reuses existing statuses for TacticPin only"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val reply = Line(Square('e', 1), Square('f', 1))
    val story =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(pinningMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val unknown = TacticPin.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = TacticPin.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticPin.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes = TacticPin.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    Vector(unknown, supports, caps, refutes).foreach: checkedStory =>
      assertEquals(checkedStory.engineCheck.exists(_.publicClaimAllowed), false, checkedStory.toString)

    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(unknownVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.PinsPiece))

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.PinsPiece))

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

  test("Pin-4 EngineCheck cannot create Pin or leak engine wording"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val wrongRoute = Line(Square('g', 8), Square('h', 8))
    val reply = Line(Square('e', 1), Square('f', 1))
    val story =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get

    val standaloneEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(pinningMove),
      engineLine = Some(EngineLine(Vector(pinningMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(pinningMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    def lowOrHighEval(before: Int, after: Int) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(pinningMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      StoryTable.choose(Vector(TacticPin.withEngineCheck(story, check).get)).head

    val lowEvalVerdict = lowOrHighEval(-900, -850)
    val highEvalVerdict = lowOrHighEval(850, 900)
    val methodNames =
      TacticPin.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticPin.getClass.getDeclaredFields.map(_.getName).toSet

    assertEquals(standaloneEvidence.evidenceReady, true)
    assertEquals(standaloneEvidence.storyBound, false)
    assertEquals(standaloneEvidence.status, EngineCheckStatus.Supports)
    assertEquals(TacticPin.withEngineCheck(story, standaloneEvidence), None)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(TacticPin.withEngineCheck(story, engineOnly), None)
    assertEquals(routeMismatch.status, EngineCheckStatus.Unknown)
    assertEquals(TacticPin.withEngineCheck(story, routeMismatch), None)
    assertEquals(lowEvalVerdict.role, highEvalVerdict.role)
    assertEquals(lowEvalVerdict.strength, highEvalVerdict.strength)
    assertEquals(lowEvalVerdict.values, highEvalVerdict.values)
    assertEquals(ExplanationPlan.fromSelected(lowEvalVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.PinsPiece))
    assertEquals(ExplanationPlan.fromSelected(highEvalVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.PinsPiece))
    assertEquals(ExplanationPlan.fromSelected(lowEvalVerdict).map(_.forbiddenWording), ExplanationPlan.fromSelected(highEvalVerdict).map(_.forbiddenWording))
    assert(!methodNames.contains("fromEngineCheck"))
    assert(!methodNames.contains("fromEngineEvidence"))

  test("Pin-5 StoryTable keeps Pin stable against existing open rows"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val material = SceneMaterial.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).get
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin.write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get

    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val high = score(99)
    val tied = Vector(
      hanging.copy(proof = high),
      fork.copy(proof = high),
      material.copy(proof = high),
      defense.copy(proof = high),
      discovered.copy(proof = high),
      pin.copy(proof = high)
    )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(tied(5), tied(2), tied(4), tied(0), tied(3), tied(1)))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(forward.map(_.story.tactic).toSet, Set(Some(Tactic.Hanging), Some(Tactic.Fork), Some(Tactic.DiscoveredAttack), Some(Tactic.Pin), None))
    assert(forward.exists(verdict => verdict.story.scene == Scene.Material))
    assert(forward.exists(verdict => verdict.story.scene == Scene.Defense))
    forward.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

  test("Pin-5 StoryTable keeps Pin out of Material king safety and Defense claim homes"):
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin.write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialMove).get.copy(proof = proof(boardProof = 99, lineProof = 99, ownerProof = 99, anchorProof = 99, routeProof = 99, persistence = 99, immediacy = 99, forcing = 99, conversionPrize = 99, counterplayRisk = 20, clarity = 99))
    val forgedDefense = pin.copy(scene = Scene.Defense, tactic = None, writer = None, threatProof = None, defenseProof = None)
    val forgedKingSafety = pin.copy(scene = Scene.King, tactic = None, writer = None)

    val materialPinVerdicts = StoryTable.choose(Vector(pin, material))
    val materialVerdict = materialPinVerdicts.find(_.story.scene == Scene.Material).get
    val pinVerdict = materialPinVerdicts.find(_.story.tactic.contains(Tactic.Pin)).get
    val forgedDefenseVerdict = StoryTable.choose(Vector(pin, forgedDefense)).find(_.story == forgedDefense).get
    val forgedKingSafetyVerdict = StoryTable.choose(Vector(pin, forgedKingSafety)).find(_.story == forgedKingSafety).get

    assertEquals(materialVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(materialVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(ExplanationPlan.fromSelected(pinVerdict), None)
    assertEquals(forgedDefenseVerdict.role, Role.Blocked)
    assertEquals(forgedDefenseVerdict.leadAllowed, false)
    val forgedDefensePlan = ExplanationPlan.fromSelected(forgedDefenseVerdict)
    assertEquals(forgedDefensePlan.flatMap(_.allowedClaim), None)
    assertEquals(forgedDefensePlan.flatMap(DeterministicRenderer.fromPlan), None)
    assertEquals(forgedKingSafetyVerdict.role, Role.Blocked)
    assertEquals(forgedKingSafetyVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedKingSafetyVerdict), None)

  test("Pin-5 StoryTable prevents duplicate Lead for same-line DiscoveredAttack and Pin"):
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6))).get
    val pin = TacticPin.write(pinFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get
    val forward = StoryTable.choose(Vector(discovered, pin))
    val reverse = StoryTable.choose(Vector(pin, discovered))

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(shape(forward), shape(reverse))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(forward.map(_.story.route).distinct, Vector(Some(revealMove)))
    assertEquals(forward.map(_.story.tactic).toSet, Set[Option[Tactic]](Some(Tactic.DiscoveredAttack), Some(Tactic.Pin)))
    forward.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("Pin-6 ExplanationPlan lowers selected uncapped Pin Lead only"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Pin))
    assertEquals(plan.side, Side.Black)
    assertEquals(plan.target, Some(Square('e', 2)))
    assertEquals(plan.anchor, Some(Square('e', 8)))
    assertEquals(plan.route, Some(pinningMove))
    assertEquals(plan.routeSan, BoardFacts.sanFor(facts, pinningMove))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.PinsPiece))
    assertEquals(plan.evidenceLine, Some(pinningMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(ExplanationClaim.PinAllowed.map(_.key), Vector("pins_piece"))
    assertEquals(
      ExplanationClaim.PinForbiddenKeys,
      Vector(
        "wins_material",
        "king_unsafe",
        "mate_threat",
        "best_move",
        "only_move",
        "forced",
        "decisive",
        "creates_pressure",
        "takes_initiative",
        "cannot_move"
      )
    )
    ExplanationClaim.PinForbiddenKeys.foreach: forbiddenKey =>
      assert(!ExplanationClaim.PinAllowed.map(_.key).contains(forbiddenKey), forbiddenKey)
      assert(plan.forbiddenWording.map(_.key).contains(forbiddenKey), forbiddenKey)
    assertEquals(ExplanationClaim.PinAllowed.map(_.key).toSet.intersect(ExplanationClaim.PinForbiddenKeys.toSet), Set.empty)

  test("Pin-6 ExplanationPlan gives no standalone claim to non Lead capped refuted or unselected Pin rows"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val reply = Line(Square('e', 1), Square('f', 1))
    val story =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val verdict = StoryTable.choose(Vector(story)).head

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(pinningMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supportVerdict = verdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = verdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = verdict.copy(role = Role.Blocked, leadAllowed = false)
    val unselectedVerdict = verdict.copy(selected = false)
    val cappedVerdict = StoryTable.choose(Vector(TacticPin.withEngineCheck(story, check(EngineCheckStatus.Caps)).get)).head
    val refutedVerdict =
      StoryTable.choose(Vector(TacticPin.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get)).head

    assertEquals(ExplanationPlan.fromSelected(supportVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(contextVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

  test("Pin-7 DeterministicRenderer phrases selected Pin ExplanationPlan only"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val forbiddenPhrases =
      Vector(
        "cannot move",
        "the king is unsafe",
        "wins material",
        "winning",
        "decisive",
        "best move",
        "only move",
        "forces",
        "creates pressure",
        "threatens mate"
      )

    assertEquals(rendered.text, "Re8 pins the piece on e2.")
    assertEquals(rendered.claimKey, "pins_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assert(rendererMethods.contains("fromPlan"))
    assert(!rendererMethods.contains("fromStory"))
    assert(!rendererMethods.contains("fromVerdict"))
    assert(!rendererMethods.contains("fromBoardFacts"))
    assert(!rendererMethods.contains("fromPinProof"))

  test("Pin-7 DeterministicRenderer refuses Pin without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val leadPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    Vector(
      leadPlan.copy(role = Role.Support),
      leadPlan.copy(role = Role.Context),
      leadPlan.copy(role = Role.Blocked, debugOnly = true),
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.RevealsAttackOnPiece)),
      leadPlan.copy(tactic = Some(Tactic.DiscoveredAttack)),
      leadPlan.copy(scene = Scene.Material, tactic = None),
      leadPlan.copy(strength = ExplanationStrength.Bounded, evidenceLine = None),
      leadPlan.copy(route = None),
      leadPlan.copy(routeSan = None),
      leadPlan.copy(target = None),
      leadPlan.copy(secondaryTarget = Some(Square('e', 1))),
      leadPlan.copy(forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.PinsPiece)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None, invalidPlan.toString)

  test("Pin-8 LLM smoke reuses 8B prompt contract for Pin RenderedLine only"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text), NarrationSmokeCheck(true, Vector.empty))
    assert(prompt.contains("renderedText: Re8 pins the piece on e2."))
    assert(prompt.contains("claimKey: pins_piece"))
    assert(prompt.contains("strength: bounded"))
    assert(prompt.contains("forbiddenWording: wins material, winning, decisive, best move, only move, forced, cannot move, no defense, front piece must move, wins rear piece, creates pressure, takes initiative, mate threat, king unsafe"))
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("raw Story", "PinProof", "LineProof", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), forbiddenInput)

  test("Pin-8 LLM smoke rejects Pin additions and stronger claims"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val invalidOutputs =
      Vector(
        "Re8 wins material by pinning the piece on e2." -> "forbidden_wording",
        "Re8 creates pressure on the piece on e2." -> "forbidden_wording",
        "Re8 takes the initiative by pinning e2." -> "forbidden_wording",
        "Re8 threatens mate after pinning e2." -> "forbidden_wording",
        "Re8 means the piece on e2 cannot move." -> "forbidden_wording",
        "Re8 pins the piece on e2, then Re1 adds a new line." -> "new_move_or_line",
        "Re8 pins the piece on e2 and Bg2 adds another line." -> "new_move_or_line",
        "Re8 pins the piece on e2 because Black is winning." -> "forbidden_wording",
        "Engine says Re8 pins the piece on e2." -> "engine_mention"
      )

    invalidOutputs.foreach: (output, expectedViolation) =>
      val checked = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(checked.accepted, false, output)
      assert(checked.violations.contains(expectedViolation), output)

    Vector(
      plan.copy(allowedClaim = Some(ExplanationClaim.RevealsAttackOnPiece)),
      plan.copy(tactic = Some(Tactic.DiscoveredAttack)),
      plan.copy(role = Role.Support),
      plan.copy(debugOnly = true)
    ).foreach: mismatchedPlan =>
      assertEquals(LlmNarrationSmoke.mockNarrate(mismatchedPlan, rendered), None, mismatchedPlan.toString)

    val mismatchedRendered = rendered.copy(claimKey = "cannot_move")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(
      LlmNarrationSmoke.check(plan, mismatchedRendered, rendered.text).violations.contains("input_mismatch"),
      true
    )

  test("Pin Closeout hard cleanup keeps authority homes separated and public surfaces closed"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin.write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val pinProof = story.pinProof.get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val lineFactSurface =
      classOf[BoardFacts.LineFact].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.LineFact].getDeclaredFields.map(_.getName).toSet
    val lineProofSurface =
      classOf[LineProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    val pinProofSurface =
      classOf[PinProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[PinProof].getDeclaredFields.map(_.getName).toSet
    val tacticPinSurface =
      TacticPin.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticPin.getClass.getDeclaredFields.map(_.getName).toSet
    val writerNames = StoryWriter.values.map(_.toString).toVector
    val rendererMethodInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(!lineFactSurface.exists(_.toLowerCase.contains(forbidden)), forbidden)
    Vector("PinProof", "TacticPin", "Story", "Verdict", "ExplanationPlan", "RenderedLine").foreach: forbidden =>
      assert(!lineProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine", "Material", "Defense", "RemoveGuard", "Skewer").foreach:
      forbidden =>
        assert(!pinProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector("test", "fixture", "helper", "mock", "fromEngineEvidence", "fromBoardFacts").foreach: forbidden =>
      assert(!tacticPinSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

    assertEquals(pinProof.publicClaimAllowed, false)
    assertEquals(pinProof.complete, true)
    assertEquals(story.writer, Some(StoryWriter.TacticPin))
    assertEquals(story.tactic, Some(Tactic.Pin))
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.pinProof.nonEmpty, true)
    assertEquals(story.lineProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assert(!writerNames.exists(name => Vector("LineTactic", "Ray", "XRay").exists(name.contains)))

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("pins_piece"))
    assertEquals(ExplanationClaim.PinAllowed.map(_.key), Vector("pins_piece"))
    assert(!ExplanationClaim.PinAllowed.map(_.key).exists(key => key.contains("material") || key.contains("defense")))
    assertEquals(ExplanationClaim.PinAllowed.map(_.key).toSet.intersect(ExplanationClaim.MaterialAllowed.map(_.key).toSet), Set.empty)
    assertEquals(ExplanationClaim.PinAllowed.map(_.key).toSet.intersect(ExplanationClaim.DefenseAllowed.map(_.key).toSet), Set.empty)
    assertEquals(ExplanationClaim.PinAllowed.map(_.key).toSet.intersect(ExplanationClaim.DiscoveredAttackAllowed.map(_.key).toSet), Set.empty)
    assertEquals(rendered.claimKey, "pins_piece")
    assertEquals(rendered.text, "Re8 pins the piece on e2.")
    assertEquals(rendererMethodInputs, Vector("ExplanationPlan"))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    Vector(
      "Re8 wins material by pinning e2.",
      "Re8 creates pressure on e2.",
      "Re8 threatens mate after pinning e2.",
      "Re8 is winning and decisive.",
      "Re8 means the piece on e2 cannot move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector("PinProof", "LineProof", "LineFact", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach:
      forbidden =>
        assert(!prompt.contains(forbidden), forbidden)
    Vector(Tactic.AbsPin, Tactic.RelPin, Tactic.Skewer, Tactic.Xray, Tactic.RemoveGuard, Tactic.DiscoveredAttack).foreach:
      tactic =>
        val forged = story.copy(tactic = Some(tactic))
        val forgedVerdict = StoryTable.choose(Vector(forged)).head
        assertEquals(forgedVerdict.role, Role.Blocked, tactic.toString)
        assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None, tactic.toString)

  test("RemoveGuard-1 RemoveGuardProof proves one legal defender capture without creating Story"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Some(Line(Square('g', 8), Square('c', 4)))
    val proof = RemoveGuardProof.fromBoardFacts(
      facts = facts,
      removeGuardMove = removeGuardMove,
      targetSquare = Some(Square('e', 5)),
      defenderSquare = Some(Square('c', 4))
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.guardedTarget, Some(Piece(Side.Black, Man.Rook, Square('e', 5))))
    assertEquals(proof.removedDefender, Some(Piece(Side.Black, Man.Knight, Square('c', 4))))
    assertEquals(proof.removeGuardMove, removeGuardMove)
    assertEquals(proof.removalKind, Some(RemoveGuardRemovalKind.DefenderCaptured))
    assertEquals(proof.targetNonKingMaterial, true)
    assertEquals(proof.defenderGuardedTargetBeforeMove, true)
    assertEquals(proof.afterMoveDefenderNoLongerGuardsTarget, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactBoardAfterMoveRelation, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      RemoveGuardProof.getClass.getDeclaredMethods.toVector ++ classOf[RemoveGuardProof].getDeclaredMethods.toVector
    val publicOutputReturningMethods = proofHomeMethods
      .filter(method =>
        Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine").exists(name =>
          method.getReturnType.getSimpleName.contains(name)
        )
      )
      .map(_.getName)
    assertEquals(publicOutputReturningMethods, Vector.empty)

    val surfaceNames =
      proofHomeMethods.map(_.getName).toSet ++
        RemoveGuardProof.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[RemoveGuardProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "materialResult",
      "materialGain",
      "winsMaterial",
      "winning",
      "decisive",
      "bestMove",
      "onlyMove",
      "pressure",
      "initiative",
      "overloaded",
      "sacrificeLure",
      "deflection"
    ).foreach: forbidden =>
      assert(!surfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("RemoveGuard-1 RemoveGuardProof admits exact guard-line block and rejects closed removal theories"):
    val captureFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val blockFacts = BoardFacts.fromFen("7k/8/8/r6q/5N2/8/8/4K3 w - - 0 1").toOption.get
    val blockMove = Some(Line(Square('f', 4), Square('d', 5)))
    val blockedGuard = RemoveGuardProof.fromBoardFacts(
      facts = blockFacts,
      removeGuardMove = blockMove,
      targetSquare = Some(Square('h', 5)),
      defenderSquare = Some(Square('a', 5))
    )
    val illegalMove = RemoveGuardProof.fromBoardFacts(
      facts = blockFacts,
      removeGuardMove = Some(Line(Square('f', 4), Square('f', 5))),
      targetSquare = Some(Square('h', 5)),
      defenderSquare = Some(Square('a', 5))
    )
    val defenderStillGuards = RemoveGuardProof.fromBoardFacts(
      facts = captureFacts,
      removeGuardMove = Some(Line(Square('g', 8), Square('f', 7))),
      targetSquare = Some(Square('e', 5)),
      defenderSquare = Some(Square('c', 4))
    )
    val targetIsKing = RemoveGuardProof.fromBoardFacts(
      facts = captureFacts,
      removeGuardMove = Some(Line(Square('g', 8), Square('c', 4))),
      targetSquare = Some(Square('h', 8)),
      defenderSquare = Some(Square('c', 4))
    )

    assertEquals(blockedGuard.complete, true)
    assertEquals(blockedGuard.removalKind, Some(RemoveGuardRemovalKind.GuardLineBlocked))
    assertEquals(blockedGuard.guardedTarget, Some(Piece(Side.Black, Man.Queen, Square('h', 5))))
    assertEquals(blockedGuard.removedDefender, Some(Piece(Side.Black, Man.Rook, Square('a', 5))))
    assertEquals(blockedGuard.defenderGuardedTargetBeforeMove, true)
    assertEquals(blockedGuard.afterMoveDefenderNoLongerGuardsTarget, true)
    assertEquals(blockedGuard.exactBoardAfterMoveRelation, true)

    assertEquals(illegalMove.complete, false)
    assertEquals(illegalMove.missingEvidence.exists(_.missing.contains("legal remove-guard move")), true)
    assertEquals(defenderStillGuards.complete, false)
    assertEquals(
      defenderStillGuards.missingEvidence.exists(_.missing.contains("after move defender no longer guards target")),
      true
    )
    assertEquals(targetIsKing.complete, false)
    assertEquals(targetIsKing.missingEvidence.exists(_.missing.contains("target non-king material piece")), true)

    val forgedStory = Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.RemoveGuard),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 5)),
      anchor = Some(Square('c', 4)),
      route = Some(Line(Square('g', 8), Square('c', 4))),
      routeSan = BoardFacts.sanFor(captureFacts, Line(Square('g', 8), Square('c', 4))),
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99,
        forcing = 99,
        immediacy = 99
      ),
      storyProof = StoryProof.fromBoardFacts(captureFacts, Line(Square('g', 8), Square('c', 4)))
    )
    val forgedVerdict = StoryTable.choose(Vector(forgedStory)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None)

  test("Skewer-1 SkewerProof proves one front and rear target relation without creating Story"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Some(Line(Square('a', 1), Square('e', 1)))
    val proof = SkewerProof.fromBoardFacts(
      facts = facts,
      skewerMove = skewerMove,
      sliderSquare = Some(Square('e', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.skewerSlider, Some(Piece(Side.White, Man.Rook, Square('e', 1))))
    assertEquals(proof.frontTarget, Some(Piece(Side.Black, Man.Queen, Square('e', 5))))
    assertEquals(proof.rearTarget, Some(Piece(Side.Black, Man.Rook, Square('e', 8))))
    assertEquals(proof.skewerMove, skewerMove)
    assertEquals(proof.lineKind, Some(BoardFacts.LineKind.File))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.frontTargetNonKingMaterial, true)
    assertEquals(proof.rearTargetNonKingMaterial, true)
    assertEquals(proof.frontAndRearSameRivalSide, true)
    assertEquals(proof.afterMoveSliderAttacksFrontTarget, true)
    assertEquals(proof.rearTargetBehindFrontTargetOnSameRay, true)
    assertEquals(proof.noExtraBlockerBreaksFrontToRearRelation, true)
    assertEquals(proof.beforeSkewerRelationAbsentOrBlocked, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      SkewerProof.getClass.getDeclaredMethods.toVector ++ classOf[SkewerProof].getDeclaredMethods.toVector
    val publicOutputReturningMethods = proofHomeMethods
      .filter(method =>
        Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine").exists(name =>
          method.getReturnType.getSimpleName.contains(name)
        )
      )
      .map(_.getName)
    assertEquals(publicOutputReturningMethods, Vector.empty)

    val surfaceNames =
      proofHomeMethods.map(_.getName).toSet ++
        SkewerProof.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[SkewerProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "materialGain",
      "materialResult",
      "winsMaterial",
      "mustMove",
      "winsRear",
      "frontMustMove",
      "rearPieceWon",
      "winning",
      "decisive",
      "bestMove",
      "onlyMove",
      "pressure",
      "initiative"
    ).foreach: forbidden =>
      assert(!surfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Skewer-1 SkewerProof keeps false positives and diagnostics out of public output"):
    val positiveFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val extraBlockerFacts = BoardFacts.fromFen("4r2k/8/4b3/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val kingFrontFacts = BoardFacts.fromFen("4r3/8/8/4k3/8/8/8/R6K w - - 0 1").toOption.get
    val relationAlreadyPresentFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/4R2K w - - 0 1").toOption.get
    val move = Some(Line(Square('a', 1), Square('e', 1)))

    val illegalMove = SkewerProof.fromBoardFacts(
      facts = positiveFacts,
      skewerMove = Some(Line(Square('a', 1), Square('d', 1))),
      sliderSquare = Some(Square('d', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )
    val targetIsKing = SkewerProof.fromBoardFacts(
      facts = kingFrontFacts,
      skewerMove = move,
      sliderSquare = Some(Square('e', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )
    val extraBlocker = SkewerProof.fromBoardFacts(
      facts = extraBlockerFacts,
      skewerMove = move,
      sliderSquare = Some(Square('e', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )
    val alreadyPresent = SkewerProof.fromBoardFacts(
      facts = relationAlreadyPresentFacts,
      skewerMove = Some(Line(Square('h', 1), Square('h', 2))),
      sliderSquare = Some(Square('e', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )

    assertEquals(illegalMove.complete, false)
    assertEquals(illegalMove.publicClaimAllowed, false)
    assertEquals(illegalMove.missingEvidence.exists(_.missing.contains("legal skewer or revealing move")), true)
    assertEquals(targetIsKing.complete, false)
    assertEquals(targetIsKing.missingEvidence.exists(_.missing.contains("front target non-king material piece")), true)
    assertEquals(extraBlocker.complete, false)
    assertEquals(
      extraBlocker.missingEvidence.exists(_.missing.contains("no extra blocker breaks front-to-rear relation")),
      true
    )
    assertEquals(alreadyPresent.complete, false)
    assertEquals(
      alreadyPresent.missingEvidence.exists(_.missing.contains("before-move skewer relation absent or blocked")),
      true
    )

    val highProof = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99
    )
    val forgedStory = Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.Skewer),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 5)),
      anchor = Some(Square('e', 1)),
      route = Some(Line(Square('a', 1), Square('e', 1))),
      proof = highProof
    )
    val forgedVerdict = StoryTable.choose(Vector(forgedStory)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None)

  test("Skewer-2 TacticSkewer writer admits one proof-backed Skewer Story without downstream speech"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story = TacticSkewer
      .write(
        facts = facts,
        skewerMove = Some(skewerMove),
        sliderSquare = Some(Square('e', 1)),
        frontTargetSquare = Some(Square('e', 5)),
        rearTargetSquare = Some(Square('e', 8))
      )
      .get
    val proof = story.skewerProof.get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Skewer))
    assertEquals(story.writer, Some(StoryWriter.TacticSkewer))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 5)))
    assertEquals(story.secondaryTarget, Some(Square('e', 8)))
    assertEquals(story.anchor, Some(Square('e', 1)))
    assertEquals(story.route, Some(skewerMove))
    assertEquals(story.routeSan, BoardFacts.sanFor(facts, skewerMove))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.skewerMove, Some(skewerMove))
    assertEquals(proof.skewerSlider.map(_.square), Some(Square('e', 1)))
    assertEquals(proof.frontTarget.map(_.square), Some(Square('e', 5)))
    assertEquals(proof.rearTarget.map(_.square), Some(Square('e', 8)))
    assertEquals(proof.afterMoveSliderAttacksFrontTarget, true)
    assertEquals(proof.rearTargetBehindFrontTargetOnSameRay, true)
    assertEquals(proof.noExtraBlockerBreaksFrontToRearRelation, true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(verdict.role, Role.Context)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("Skewer-2 TacticSkewer writer blocks incomplete refuted material pin and rear king paths"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story = TacticSkewer
      .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
      .get
    val rearKingFacts = BoardFacts.fromFen("4k3/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val pinLookingFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get

    assertEquals(
      TacticSkewer.write(
        facts,
        Some(Line(Square('a', 1), Square('d', 1))),
        Some(Square('d', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8))
      ),
      None
    )
    assertEquals(
      TacticSkewer.write(
        rearKingFacts,
        Some(skewerMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8))
      ),
      None
    )
    assertEquals(
      TacticSkewer.write(
        pinLookingFacts,
        Some(Line(Square('a', 8), Square('e', 8))),
        Some(Square('e', 8)),
        Some(Square('e', 2)),
        Some(Square('e', 1))
      ),
      None
    )

    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(skewerMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(220)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    val refutedStory = TacticSkewer.withEngineCheck(story, refutes).get
    val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(refutedStory.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    val forgedMaterial = story.copy(scene = Scene.Material, tactic = None)
    val forgedPin = story.copy(tactic = Some(Tactic.Pin), secondaryTarget = None)
    Vector(forgedMaterial, forgedPin).foreach: forged =>
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked)
      assertEquals(forgedVerdict.leadAllowed, false)
      assertEquals(ExplanationPlan.fromSelected(forgedVerdict).flatMap(_.allowedClaim), None)

  test("Skewer-3 negative corpus keeps incomplete skewer relations silent"):
    val legalMove = Line(Square('a', 1), Square('e', 1))
    val positiveFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val sameBoardMissingFacts = minimalBoardFacts(
      sideToMove = Side.White,
      sideLegal = readyMoves(line = legalMove),
      rivalLegal = readyMoves(line = Line(Square('h', 8), Square('h', 7))),
      pieces = Vector(
        Piece(Side.White, Man.King, Square('h', 1)),
        Piece(Side.White, Man.Rook, Square('a', 1)),
        Piece(Side.Black, Man.King, Square('h', 8)),
        Piece(Side.Black, Man.Queen, Square('e', 5)),
        Piece(Side.Black, Man.Rook, Square('e', 8))
      )
    )
    val corpus = Vector(
      (
        "legal move missing",
        positiveFacts,
        Some(Line(Square('a', 1), Square('d', 1))),
        Some(Square('d', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "legal skewer or revealing move"
      ),
      (
        "same-board proof missing",
        sameBoardMissingFacts,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "same-board proof"
      ),
      (
        "slider is not a slider",
        BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/N6K w - - 0 1").toOption.get,
        Some(Line(Square('a', 1), Square('e', 2))),
        Some(Square('e', 2)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "skewer slider"
      ),
      (
        "front target missing",
        positiveFacts,
        Some(legalMove),
        Some(Square('e', 1)),
        None,
        Some(Square('e', 8)),
        "front target"
      ),
      (
        "rear target missing",
        positiveFacts,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        None,
        "rear target"
      ),
      (
        "front target is king",
        BoardFacts.fromFen("4r3/8/8/4k3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "front target non-king material piece"
      ),
      (
        "rear target is king",
        BoardFacts.fromFen("4k3/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "rear target non-king material piece"
      ),
      (
        "front and rear targets are not the same rival side",
        BoardFacts.fromFen("4B2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "front and rear target same rival side"
      ),
      (
        "rear target is not behind the front target on the same line",
        BoardFacts.fromFen("3r3k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('d', 8)),
        "rear target behind front target on same ray"
      ),
      (
        "middle blocker breaks the front-to-rear relation",
        BoardFacts.fromFen("4r2k/8/4b3/4q3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "no extra blocker breaks front-to-rear relation"
      ),
      (
        "discovered attack only is not a skewer",
        BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(Line(Square('d', 3), Square('f', 4))),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        None,
        "rear target"
      ),
      (
        "pin-looking position is not a skewer",
        BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get,
        Some(Line(Square('a', 8), Square('e', 8))),
        Some(Square('e', 8)),
        Some(Square('e', 2)),
        Some(Square('e', 1)),
        "rear target non-king material piece"
      ),
      (
        "already-present relation needs front-piece-must-move assumption",
        BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/4R1NK w - - 0 1").toOption.get,
        Some(Line(Square('g', 1), Square('f', 3))),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "before-move skewer relation absent or blocked"
      )
    )

    corpus.foreach: (label, facts, move, slider, front, rear, expectedMissing) =>
      val proof = SkewerProof.fromBoardFacts(facts, move, slider, front, rear)
      assertEquals(proof.complete, false, label)
      assertEquals(proof.publicClaimAllowed, false, label)
      assertEquals(proof.missingEvidence.exists(_.missing.contains(expectedMissing)), true, label)
      assertEquals(TacticSkewer.write(facts, move, slider, front, rear), None, label)

  test("Skewer-3 negative corpus blocks material forced best-move and front-must-move wording"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val verdict = StoryTable.choose(Vector(story)).head
    val writerSurface =
      TacticSkewer.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticSkewer.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[SkewerProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[SkewerProof].getDeclaredFields.map(_.getName).toSet
    val proofText = story.skewerProof.toString.toLowerCase

    assertEquals(verdict.role, Role.Context)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)
    Vector(
      "materialWin",
      "materialGain",
      "winsMaterial",
      "forced",
      "bestMove",
      "frontMustMove",
      "mustMove",
      "winsRear",
      "rearPieceWon"
    ).foreach: forbidden =>
      assert(!writerSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
      assert(!proofText.contains(forbidden.toLowerCase), forbidden)

  test("Skewer-4 EngineCheck reuses existing statuses for TacticSkewer only"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(skewerMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val unknown = TacticSkewer.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = TacticSkewer.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticSkewer.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes = TacticSkewer.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    Vector(unknown, supports, caps, refutes).foreach: checkedStory =>
      assertEquals(checkedStory.engineCheck.exists(_.publicClaimAllowed), false, checkedStory.toString)

    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknownVerdict.role, Role.Context)
    assertEquals(unknownVerdict.leadAllowed, false)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(unknownVerdict), None)

    assertEquals(supportsVerdict.role, Role.Context)
    assertEquals(supportsVerdict.leadAllowed, false)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict), None)

    assertEquals(capsVerdict.role, Role.Context)
    assertEquals(capsVerdict.leadAllowed, false)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

  test("Skewer-4 EngineCheck cannot create Skewer or leak engine wording"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val wrongRoute = Line(Square('h', 1), Square('g', 1))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get

    val standaloneEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(skewerMove),
      engineLine = Some(EngineLine(Vector(skewerMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(skewerMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    def lowOrHighEval(before: Int, after: Int) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(skewerMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      StoryTable.choose(Vector(TacticSkewer.withEngineCheck(story, check).get)).head

    val lowEvalVerdict = lowOrHighEval(-900, -850)
    val highEvalVerdict = lowOrHighEval(850, 900)
    val forgedFromEngine = story.copy(writer = None, skewerProof = None)
    val forgedVerdict = StoryTable.choose(Vector(forgedFromEngine)).head
    val methodNames =
      TacticSkewer.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticSkewer.getClass.getDeclaredFields.map(_.getName).toSet

    assertEquals(standaloneEvidence.evidenceReady, true)
    assertEquals(standaloneEvidence.storyBound, false)
    assertEquals(standaloneEvidence.status, EngineCheckStatus.Supports)
    assertEquals(TacticSkewer.withEngineCheck(story, standaloneEvidence), None)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(TacticSkewer.withEngineCheck(story, engineOnly), None)
    assertEquals(routeMismatch.status, EngineCheckStatus.Unknown)
    assertEquals(TacticSkewer.withEngineCheck(story, routeMismatch), None)
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(lowEvalVerdict.role, highEvalVerdict.role)
    assertEquals(lowEvalVerdict.strength, highEvalVerdict.strength)
    assertEquals(lowEvalVerdict.values, highEvalVerdict.values)
    assertEquals(ExplanationPlan.fromSelected(lowEvalVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(highEvalVerdict), None)
    assert(!methodNames.contains("fromEngineCheck"))
    assert(!methodNames.contains("fromEngineEvidence"))
    Vector("engineSays", "bestMove", "onlyMove", "forcedWin", "winningTactic", "rawPv", "evalNumber").foreach:
      forbidden =>
        assert(!methodNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Skewer-5 StoryTable keeps Skewer stable against existing open rows"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val material = SceneMaterial.write(materialFacts, hangingMove).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).get
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinMove = Line(Square('a', 8), Square('e', 8))
    val pin = TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewerBase =
      TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val skewerHighProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val skewer = skewerBase.copy(proof = skewerHighProof)

    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val high = score(99)
    val tied = Vector(
      hanging.copy(proof = high),
      fork.copy(proof = high),
      material.copy(proof = high),
      defense.copy(proof = high),
      discovered.copy(proof = high),
      pin.copy(proof = high),
      removeGuard.copy(proof = high),
      skewer.copy(proof = high)
    )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(tied(7), tied(2), tied(5), tied(0), tied(6), tied(3), tied(1), tied(4)))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(
      forward.map(verdict => verdict.story.scene -> verdict.story.tactic).toSet,
      Set[(Scene, Option[Tactic])](
        Scene.Tactic -> Some(Tactic.Hanging),
        Scene.Tactic -> Some(Tactic.Fork),
        Scene.Material -> None,
        Scene.Defense -> None,
        Scene.Tactic -> Some(Tactic.DiscoveredAttack),
        Scene.Tactic -> Some(Tactic.Pin),
        Scene.Tactic -> Some(Tactic.RemoveGuard),
        Scene.Tactic -> Some(Tactic.Skewer)
      )
    )
    val skewerVerdict = forward.find(_.story.tactic.contains(Tactic.Skewer)).get
    assert(skewerVerdict.role != Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(skewerVerdict), None)
    forward.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

  test("Skewer-5 keeps Material Pin and RemoveGuard claim homes separate"):
    val materialSkewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val materialSkewerMove = Line(Square('a', 1), Square('e', 1))
    val materialSkewer =
      TacticSkewer.write(
        materialSkewerFacts,
        Some(materialSkewerMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8))
      ).get
    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinMove = Line(Square('d', 3), Square('f', 4))
    val pin = TacticPin.write(pinFacts, Some(pinMove), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get
    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get

    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val lowerSkewerForMaterial = materialSkewer.copy(proof = score(80))
    val highMaterial = material.copy(proof = score(99))
    val materialCollision = StoryTable.choose(Vector(lowerSkewerForMaterial, highMaterial))
    val materialVerdict = materialCollision.find(_.story == highMaterial).get
    val skewerMaterialVerdict = materialCollision.find(_.story == lowerSkewerForMaterial).get

    assertEquals(materialCollision.count(_.role == Role.Lead), 1)
    assertEquals(materialVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(materialVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(ExplanationPlan.fromSelected(skewerMaterialVerdict), None)

    val highPin = pin.copy(proof = score(99))
    val lowerSkewerForPin = materialSkewer.copy(proof = score(80))
    val pinCollision = StoryTable.choose(Vector(lowerSkewerForPin, highPin))
    val pinVerdict = pinCollision.find(_.story == highPin).get
    val skewerPinVerdict = pinCollision.find(_.story == lowerSkewerForPin).get

    assertEquals(pinCollision.count(_.role == Role.Lead), 1)
    assertEquals(pinVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(pinVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.PinsPiece))
    assertEquals(ExplanationPlan.fromSelected(skewerPinVerdict), None)

    val highRemoveGuard = removeGuard.copy(proof = score(99))
    val lowerSkewerForRemoveGuard = materialSkewer.copy(proof = score(80))
    val removeGuardCollision = StoryTable.choose(Vector(lowerSkewerForRemoveGuard, highRemoveGuard))
    val removeGuardVerdict = removeGuardCollision.find(_.story == highRemoveGuard).get
    val skewerRemoveGuardVerdict = removeGuardCollision.find(_.story == lowerSkewerForRemoveGuard).get

    assertEquals(removeGuardCollision.count(_.role == Role.Lead), 1)
    assertEquals(removeGuardVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(removeGuardVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.RemovesDefender))
    assertEquals(ExplanationPlan.fromSelected(skewerRemoveGuardVerdict), None)

  test("Skewer-5 separates DiscoveredAttack collision and keeps incomplete Skewer silent"):
    val skewerDiscoveredFacts = BoardFacts.fromFen("7k/7q/6r1/8/8/3N4/8/KB6 w - - 0 1").toOption.get
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/KB6 w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6))).get
    val skewer =
      TacticSkewer.write(
        skewerDiscoveredFacts,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 7))
      ).get
    val forward = StoryTable.choose(Vector(discovered, skewer))
    val reverse = StoryTable.choose(Vector(skewer, discovered))

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(shape(forward), shape(reverse))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(forward.map(_.story.route).distinct, Vector(Some(revealMove)))
    assertEquals(forward.map(_.story.tactic).toSet, Set[Option[Tactic]](Some(Tactic.DiscoveredAttack), Some(Tactic.Skewer)))
    forward.filter(_.story.tactic.contains(Tactic.Skewer)).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

    val discoveredOnlyFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/KB6 w - - 0 1").toOption.get
    val discoveredOnly =
      TacticDiscoveredAttack.write(discoveredOnlyFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6))).get
    val incompleteSkewer =
      TacticSkewer.write(
        discoveredOnlyFacts,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 7))
      )
    val discoveredOnlyVerdict = StoryTable.choose(Vector(discoveredOnly)).head

    assertEquals(incompleteSkewer, None)
    assertEquals(discoveredOnlyVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(discoveredOnlyVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.RevealsAttackOnPiece))

  test("Skewer-6 ExplanationPlan lowers selected uncapped Skewer Lead only"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val leadStory = story.copy(proof = highProof)
    val verdict = StoryTable.choose(Vector(leadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forbiddenKeys =
      Vector(
        "wins_material",
        "wins_rear_piece",
        "front_piece_must_move",
        "best_move",
        "only_move",
        "forced",
        "decisive",
        "king_unsafe",
        "mate_threat",
        "creates_pressure",
        "takes_initiative"
      )

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Skewer))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 5)))
    assertEquals(plan.secondaryTarget, Some(Square('e', 8)))
    assertEquals(plan.anchor, Some(Square('e', 1)))
    assertEquals(plan.route, Some(skewerMove))
    assertEquals(plan.routeSan, BoardFacts.sanFor(facts, skewerMove))
    assertEquals(plan.allowedClaim.map(_.key), Some("skewers_piece_to_piece"))
    assertEquals(plan.evidenceLine, Some(skewerMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(ExplanationClaim.values.map(_.key).filter(_.contains("skewer")).toVector, Vector("skewers_piece_to_piece"))
    forbiddenKeys.foreach: forbiddenKey =>
      assert(!ExplanationClaim.values.map(_.key).contains(forbiddenKey), forbiddenKey)
      assert(plan.forbiddenWording.map(_.key).contains(forbiddenKey), forbiddenKey)

  test("Skewer-6 ExplanationPlan gives no standalone claim to non Lead capped refuted or unselected Skewer rows"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val leadStory = story.copy(proof = highProof)
    val verdict = StoryTable.choose(Vector(leadStory)).head

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(leadStory),
        engineLine = Some(EngineLine(Vector(skewerMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supportVerdict = verdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = verdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = verdict.copy(role = Role.Blocked, leadAllowed = false)
    val unselectedVerdict = verdict.copy(selected = false)
    val cappedVerdict =
      StoryTable.choose(Vector(TacticSkewer.withEngineCheck(leadStory, check(EngineCheckStatus.Caps)).get)).head
    val refutedVerdict =
      StoryTable.choose(Vector(TacticSkewer.withEngineCheck(leadStory, check(EngineCheckStatus.Supports, before = 220, after = 20)).get)).head

    assertEquals(ExplanationPlan.fromSelected(supportVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(contextVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

  test("Skewer-7 DeterministicRenderer phrases selected Skewer ExplanationPlan only"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story.copy(proof = highProof))).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val forbiddenPhrases =
      Vector(
        "wins material",
        "wins the piece behind it",
        "front piece must move",
        "best move",
        "only move",
        "forces",
        "decisive",
        "king is unsafe",
        "threatens mate",
        "creates pressure"
      )

    assertEquals(rendered.text, s"${plan.routeSan.get} skewers the piece on e5 to the piece on e8.")
    assertEquals(rendered.claimKey, "skewers_piece_to_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assert(rendererMethods.contains("fromPlan"))
    assert(!rendererMethods.contains("fromStory"))
    assert(!rendererMethods.contains("fromVerdict"))
    assert(!rendererMethods.contains("fromBoardFacts"))
    assert(!rendererMethods.contains("fromSkewerProof"))

  test("Skewer-7 DeterministicRenderer refuses Skewer without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val leadPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story.copy(proof = highProof))).head).get

    Vector(
      leadPlan.copy(role = Role.Support),
      leadPlan.copy(role = Role.Context),
      leadPlan.copy(role = Role.Blocked, debugOnly = true),
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.RevealsAttackOnPiece)),
      leadPlan.copy(tactic = Some(Tactic.DiscoveredAttack)),
      leadPlan.copy(scene = Scene.Material, tactic = None),
      leadPlan.copy(strength = ExplanationStrength.Bounded, evidenceLine = None),
      leadPlan.copy(route = None),
      leadPlan.copy(routeSan = None),
      leadPlan.copy(target = None),
      leadPlan.copy(secondaryTarget = None),
      leadPlan.copy(forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.SkewersPiece)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None, invalidPlan.toString)

  test("Skewer-8 LLM smoke reuses 8B prompt contract for Skewer RenderedLine only"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story.copy(proof = highProof))).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text), NarrationSmokeCheck(true, Vector.empty))
    assert(prompt.contains(s"renderedText: ${rendered.text}"))
    assert(prompt.contains("claimKey: skewers_piece_to_piece"))
    assert(prompt.contains("strength: bounded"))
    assert(prompt.contains("forbiddenWording: wins material, winning, decisive, best move, only move, forced, cannot move, no defense, front piece must move, wins rear piece, creates pressure, takes initiative, mate threat, king unsafe"))
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("raw Story", "SkewerProof", "LineProof", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach:
      forbiddenInput =>
        assert(!prompt.contains(forbiddenInput), forbiddenInput)

  test("Skewer-8 LLM smoke rejects Skewer additions and stronger claims"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story.copy(proof = highProof))).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val invalidOutputs =
      Vector(
        "Re1 wins material with a skewer on e5 and e8." -> "forbidden_wording",
        "Re1 wins the piece behind it." -> "forbidden_wording",
        "Re1 means the front piece must move." -> "forbidden_wording",
        "Re1 forces the skewer on e5 and e8." -> "forbidden_wording",
        "Re1 creates pressure with the skewer." -> "forbidden_wording",
        "Re1 takes the initiative with the skewer." -> "forbidden_wording",
        "Re1 threatens mate after the skewer." -> "forbidden_wording",
        "Re1 skewers the piece on e5 to e8, then Qxe5 adds a new line." -> "new_move_or_line",
        "Re1 skewers the piece on e5 to e8 and Bg2 adds another line." -> "new_move_or_line",
        "Engine says Re1 skewers e5 to e8." -> "engine_mention",
        "The raw PV confirms Re1 skewers e5 to e8." -> "engine_mention"
      )

    invalidOutputs.foreach: (output, expectedViolation) =>
      val checked = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(checked.accepted, false, output)
      assert(checked.violations.contains(expectedViolation), output)

    Vector(
      plan.copy(allowedClaim = Some(ExplanationClaim.RevealsAttackOnPiece)),
      plan.copy(tactic = Some(Tactic.DiscoveredAttack)),
      plan.copy(role = Role.Support),
      plan.copy(debugOnly = true)
    ).foreach: mismatchedPlan =>
      assertEquals(LlmNarrationSmoke.mockNarrate(mismatchedPlan, rendered), None, mismatchedPlan.toString)

    val mismatchedRendered = rendered.copy(claimKey = "wins_material")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(
      LlmNarrationSmoke.check(plan, mismatchedRendered, rendered.text).violations.contains("input_mismatch"),
      true
    )

  test("Skewer Closeout hard cleanup keeps authority homes separated and public surfaces closed"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer.write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val leadStory = story.copy(proof = highProof)
    val skewerProof = leadStory.skewerProof.get
    val verdict = StoryTable.choose(Vector(leadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val lineFactSurface =
      classOf[BoardFacts.LineFact].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.LineFact].getDeclaredFields.map(_.getName).toSet
    val lineProofSurface =
      classOf[LineProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    val skewerProofSurface =
      classOf[SkewerProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[SkewerProof].getDeclaredFields.map(_.getName).toSet
    val tacticSkewerSurface =
      TacticSkewer.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticSkewer.getClass.getDeclaredFields.map(_.getName).toSet
    val rendererMethodInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(!lineFactSurface.exists(_.toLowerCase.contains(forbidden)), forbidden)
    Vector("SkewerProof", "TacticSkewer", "Story", "Verdict", "ExplanationPlan", "RenderedLine").foreach: forbidden =>
      assert(!lineProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine", "Hanging", "Defense", "Pin", "DiscoveredAttack", "RemoveGuard").foreach:
      forbidden =>
        assert(!skewerProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector("test", "fixture", "helper", "mock", "fromEngineEvidence", "fromLineProof").foreach: forbidden =>
      assert(!tacticSkewerSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    Vector("materialGain", "winsMaterial", "winsRearPiece", "frontPieceMustMove", "forcedSkewer", "pressure", "initiative", "mateThreat").foreach:
      forbidden =>
        assert(!skewerProofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

    assertEquals(skewerProof.publicClaimAllowed, false)
    assertEquals(skewerProof.complete, true)
    assertEquals(leadStory.writer, Some(StoryWriter.TacticSkewer))
    assertEquals(leadStory.tactic, Some(Tactic.Skewer))
    assertEquals(leadStory.scene, Scene.Tactic)
    assertEquals(leadStory.skewerProof.nonEmpty, true)
    assertEquals(leadStory.captureResult, None)
    assertEquals(leadStory.multiTargetProof, None)
    assertEquals(leadStory.threatProof, None)
    assertEquals(leadStory.defenseProof, None)
    assertEquals(leadStory.lineProof, None)
    assertEquals(leadStory.pinProof, None)
    assertEquals(leadStory.removeGuardProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("skewers_piece_to_piece"))
    assertEquals(ExplanationClaim.SkewerAllowed.map(_.key), Vector("skewers_piece_to_piece"))
    Vector(
      ExplanationClaim.MaterialAllowed,
      ExplanationClaim.HangingAllowed,
      ExplanationClaim.PinAllowed,
      ExplanationClaim.DiscoveredAttackAllowed,
      ExplanationClaim.RemoveGuardAllowed,
      ExplanationClaim.DefenseAllowed
    ).foreach: siblingAllowed =>
      assertEquals(ExplanationClaim.SkewerAllowed.map(_.key).toSet.intersect(siblingAllowed.map(_.key).toSet), Set.empty)
    Vector("wins_material", "wins_rear_piece", "front_piece_must_move", "forced").foreach: forbiddenKey =>
      assert(!ExplanationClaim.SkewerAllowed.map(_.key).contains(forbiddenKey), forbiddenKey)
      assert(ExplanationClaim.SkewerForbiddenKeys.contains(forbiddenKey), forbiddenKey)
      assert(plan.forbiddenWording.map(_.key).contains(forbiddenKey), forbiddenKey)

    assertEquals(rendered.claimKey, "skewers_piece_to_piece")
    assertEquals(rendered.text, "Re1 skewers the piece on e5 to the piece on e8.")
    assertEquals(rendererMethodInputs, Vector("ExplanationPlan"))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    Vector(
      "Re1 wins rear piece.",
      "Re1 wins the piece behind it.",
      "Re1 wins material with a skewer.",
      "Re1 is a forced skewer.",
      "Re1 means the front piece must move.",
      "Re1 creates pressure with the skewer.",
      "Re1 takes the initiative with the skewer.",
      "Re1 threatens mate after the skewer.",
      "Re1 starts a pin."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector("raw Story", "SkewerProof", "LineProof", "LineFact", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach:
      forbidden =>
        assert(!prompt.contains(forbidden), forbidden)

    Vector(
      leadStory.copy(scene = Scene.Material, tactic = None),
      leadStory.copy(scene = Scene.Defense, tactic = None),
      leadStory.copy(tactic = Some(Tactic.Hanging)),
      leadStory.copy(tactic = Some(Tactic.Pin), secondaryTarget = None),
      leadStory.copy(tactic = Some(Tactic.DiscoveredAttack), secondaryTarget = None),
      leadStory.copy(tactic = Some(Tactic.RemoveGuard), secondaryTarget = None)
    ).foreach: forged =>
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked, forged.toString)
      assertEquals(ExplanationPlan.fromSelected(forgedVerdict).flatMap(_.allowedClaim), None, forged.toString)

  test("RemoveGuard-2 TacticRemoveGuard writer admits one proof-backed RemoveGuard Story"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story = TacticRemoveGuard
      .write(
        facts = facts,
        removeGuardMove = Some(removeGuardMove),
        targetSquare = Some(Square('e', 5)),
        defenderSquare = Some(Square('c', 4))
      )
      .get
    val proof = story.removeGuardProof.get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.RemoveGuard))
    assertEquals(story.writer, Some(StoryWriter.TacticRemoveGuard))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 5)))
    assertEquals(story.anchor, Some(Square('c', 4)))
    assertEquals(story.route, Some(removeGuardMove))
    assertEquals(story.routeSan, BoardFacts.sanFor(facts, removeGuardMove))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactBoardAfterMoveRelation, true)
    assertEquals(proof.removalKind, Some(RemoveGuardRemovalKind.DefenderCaptured))
    assertEquals(proof.guardedTarget, Some(Piece(Side.Black, Man.Rook, Square('e', 5))))
    assertEquals(proof.removedDefender, Some(Piece(Side.Black, Man.Knight, Square('c', 4))))
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), Some(ExplanationClaim.RemovesDefender))
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("removes_defender")
    )

  test("RemoveGuard-2 TacticRemoveGuard writer blocks refuted unsupported and sibling-meaning rows"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story = TacticRemoveGuard
      .write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
      .get
    val refuted = story.copy(
      engineCheck = Some(
        EngineCheck(
          sameBoardProof = true,
          checkedMove = Some(removeGuardMove),
          engineLine = Some(EngineLine(Vector(removeGuardMove))),
          replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
          evalBefore = Some(EngineEval(0)),
          evalAfter = Some(EngineEval(0)),
          depth = Some(18),
          freshnessPly = Some(0),
          status = EngineCheckStatus.Refutes,
          missingEvidence = Vector.empty,
          storyBound = true
        )
      )
    )
    val illegal =
      TacticRemoveGuard.write(facts, Some(Line(Square('g', 8), Square('f', 7))), Some(Square('e', 5)), Some(Square('c', 4)))
    val targetKing =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('h', 8)), Some(Square('c', 4)))
    val forgedMaterial = story.copy(scene = Scene.Material, tactic = None)
    val forgedDefense = story.copy(scene = Scene.Defense, tactic = None)
    val forgedHanging = story.copy(tactic = Some(Tactic.Hanging))

    assertEquals(StoryTable.choose(Vector(refuted)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(refuted)).head.leadAllowed, false)
    assertEquals(illegal, None)
    assertEquals(targetKing, None)

    Vector(forgedMaterial, forgedDefense, forgedHanging).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, forged.toString)
      assertEquals(verdict.leadAllowed, false, forged.toString)
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, forged.toString)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, forged.toString)

    val writerSurfaceNames =
      TacticRemoveGuard.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticRemoveGuard.getClass.getDeclaredFields.map(_.getName).toSet
    Vector("materialClaim", "winsMaterial", "hanging", "refutesDefense", "noDefense", "pressure", "initiative")
      .foreach: forbidden =>
        assert(!writerSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("RemoveGuard-3 negative corpus keeps incomplete guard-removal false positives silent"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val untrustedFacts =
      minimalBoardFacts(
        sideToMove = Side.White,
        sideLegal = readyMoves(line = removeGuardMove, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('h', 1)),
          Piece(Side.White, Man.Bishop, Square('g', 8)),
          Piece(Side.Black, Man.King, Square('h', 8)),
          Piece(Side.Black, Man.Rook, Square('e', 5)),
          Piece(Side.Black, Man.Knight, Square('c', 4))
        )
      )
    val corpus = Vector(
      (
        "legal move missing",
        facts,
        Some(Line(Square('g', 8), Square('g', 7))),
        Some(Square('e', 5)),
        Some(Square('c', 4)),
        "legal remove-guard move"
      ),
      (
        "same-board proof missing",
        untrustedFacts,
        Some(removeGuardMove),
        Some(Square('e', 5)),
        Some(Square('c', 4)),
        "same-board proof"
      ),
      (
        "target is king",
        facts,
        Some(removeGuardMove),
        Some(Square('h', 8)),
        Some(Square('c', 4)),
        "target non-king material piece"
      ),
      (
        "defender did not guard target",
        BoardFacts.fromFen("6Bk/8/7b/4r3/2n5/8/8/7K w - - 0 1").toOption.get,
        Some(removeGuardMove),
        Some(Square('h', 6)),
        Some(Square('c', 4)),
        "defender guarded target before move"
      ),
      (
        "defender still guards after move",
        facts,
        Some(Line(Square('g', 8), Square('f', 7))),
        Some(Square('e', 5)),
        Some(Square('c', 4)),
        "after move defender no longer guards target"
      )
    )

    corpus.foreach: (label, board, move, target, defender, expectedMissing) =>
      val proof = RemoveGuardProof.fromBoardFacts(board, move, target, defender)
      assertEquals(proof.complete, false, label)
      assertEquals(proof.missingEvidence.exists(_.missing.contains(expectedMissing)), true, label)
      assertEquals(TacticRemoveGuard.write(board, move, target, defender), None, label)

  test("RemoveGuard-3 negative corpus blocks broad claims and sibling tactic misclassification"):
    val defendedFacts = BoardFacts.fromFen("6Bk/6b1/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story = TacticRemoveGuard
      .write(defendedFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
      .get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.removeGuardProof.exists(_.complete), true)
    assertEquals(defendedFacts.seen.guards.exists(_.target.square == Square('e', 5)), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.RemoveGuard))
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), Some(ExplanationClaim.RemovesDefender))
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("removes_defender")
    )

    val forgedMaterial = story.copy(scene = Scene.Material, tactic = None)
    val forgedHanging = story.copy(tactic = Some(Tactic.Hanging))
    val forgedDefense = story.copy(scene = Scene.Defense, tactic = None)
    val forgedOverload = story.copy(tactic = Some(Tactic.Overload))
    val forgedDeflection = story.copy(tactic = Some(Tactic.Deflect))
    val forgedPin = story.copy(tactic = Some(Tactic.Pin))
    val forgedDiscoveredAttack = story.copy(tactic = Some(Tactic.DiscoveredAttack))
    val forgedSkewer = story.copy(tactic = Some(Tactic.Skewer))

    Vector(
      forgedMaterial,
      forgedHanging,
      forgedDefense,
      forgedOverload,
      forgedDeflection,
      forgedPin,
      forgedDiscoveredAttack,
      forgedSkewer
    ).foreach: forged =>
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked, forged.toString)
      assertEquals(forgedVerdict.leadAllowed, false, forged.toString)
      val plan = ExplanationPlan.fromSelected(forgedVerdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, forged.toString)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, forged.toString)

    val surfaceText =
      (TacticRemoveGuard.getClass.getDeclaredMethods.map(_.getName).toVector ++
        TacticRemoveGuard.getClass.getDeclaredFields.map(_.getName).toVector)
        .mkString(" ")
        .toLowerCase
    Vector("noDefense", "winsMaterial", "bestMove", "overloaded", "deflection", "refutesDefense").foreach:
      forbidden =>
        assert(!surfaceText.contains(forbidden.toLowerCase), forbidden)

  test("RemoveGuard-4 EngineCheck reuses existing statuses for TacticRemoveGuard only"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story = TacticRemoveGuard
      .write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
      .get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(removeGuardMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val unknown = TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes =
      TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    Vector(unknown, supports, caps, refutes).foreach: checkedStory =>
      assertEquals(checkedStory.engineCheck.exists(_.publicClaimAllowed), false, checkedStory.toString)

    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(unknownVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.RemovesDefender))

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.RemovesDefender))

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

  test("RemoveGuard-4 EngineCheck cannot create RemoveGuard or leak engine wording"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val wrongRoute = Line(Square('g', 8), Square('f', 7))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story = TacticRemoveGuard
      .write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
      .get

    val standaloneEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(removeGuardMove),
      engineLine = Some(EngineLine(Vector(removeGuardMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(removeGuardMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    def lowOrHighEval(before: Int, after: Int) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(removeGuardMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      StoryTable.choose(Vector(TacticRemoveGuard.withEngineCheck(story, check).get)).head

    val lowEvalVerdict = lowOrHighEval(-900, -850)
    val highEvalVerdict = lowOrHighEval(850, 900)
    val forgedFromEngine = story.copy(writer = None, removeGuardProof = None)
    val forgedVerdict = StoryTable.choose(Vector(forgedFromEngine)).head
    val methodNames =
      TacticRemoveGuard.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticRemoveGuard.getClass.getDeclaredFields.map(_.getName).toSet

    assertEquals(standaloneEvidence.evidenceReady, true)
    assertEquals(standaloneEvidence.storyBound, false)
    assertEquals(standaloneEvidence.status, EngineCheckStatus.Supports)
    assertEquals(TacticRemoveGuard.withEngineCheck(story, standaloneEvidence), None)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(TacticRemoveGuard.withEngineCheck(story, engineOnly), None)
    assertEquals(routeMismatch.status, EngineCheckStatus.Unknown)
    assertEquals(TacticRemoveGuard.withEngineCheck(story, routeMismatch), None)
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(lowEvalVerdict.role, highEvalVerdict.role)
    assertEquals(lowEvalVerdict.strength, highEvalVerdict.strength)
    assertEquals(lowEvalVerdict.values, highEvalVerdict.values)
    assertEquals(ExplanationPlan.fromSelected(lowEvalVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.RemovesDefender))
    assertEquals(ExplanationPlan.fromSelected(highEvalVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.RemovesDefender))
    assert(!methodNames.contains("fromEngineCheck"))
    assert(!methodNames.contains("fromEngineEvidence"))
    Vector("engineSays", "bestMove", "onlyMove", "winningTactic", "rawPv", "evalNumber").foreach: forbidden =>
      assert(!methodNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("RemoveGuard-5 StoryTable keeps RemoveGuard stable against existing open rows"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val material = SceneMaterial.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).get
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin.write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get

    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    val high = score(99)
    val tied = Vector(
      hanging.copy(proof = high),
      fork.copy(proof = high),
      material.copy(proof = high),
      defense.copy(proof = high),
      discovered.copy(proof = high),
      pin.copy(proof = high),
      removeGuard.copy(proof = high)
    )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(tied(6), tied(3), tied(1), tied(5), tied(0), tied(4), tied(2)))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(
      forward.map(verdict => verdict.story.scene -> verdict.story.tactic).toSet,
      Set[(Scene, Option[Tactic])](
        Scene.Tactic -> Some(Tactic.Hanging),
        Scene.Tactic -> Some(Tactic.Fork),
        Scene.Material -> None,
        Scene.Defense -> None,
        Scene.Tactic -> Some(Tactic.DiscoveredAttack),
        Scene.Tactic -> Some(Tactic.Pin),
        Scene.Tactic -> Some(Tactic.RemoveGuard)
      )
    )
    forward.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

  test("RemoveGuard-5 keeps material and hanging claim homes over same-route guard capture"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val material = SceneMaterial.write(facts, removeGuardMove).get
    val hanging = TacticHanging.write(facts, removeGuardMove).get

    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        clarity = value
      )

    val highRemoveGuard = removeGuard.copy(proof = score(99))
    val sameRouteMaterial = material.copy(proof = score(90))
    val sameRouteHanging = hanging.copy(proof = score(80))
    val materialCollision = StoryTable.choose(Vector(highRemoveGuard, sameRouteMaterial))
    val hangingCollision = StoryTable.choose(Vector(highRemoveGuard, sameRouteHanging))
    val allCollision = StoryTable.choose(Vector(highRemoveGuard, sameRouteMaterial, sameRouteHanging))

    assertEquals(materialCollision.count(_.role == Role.Lead), 1)
    assertEquals(materialCollision.find(_.story == sameRouteMaterial).map(_.role), Some(Role.Lead))
    assertEquals(materialCollision.find(_.story == highRemoveGuard).exists(_.role == Role.Lead), false)
    assertEquals(
      ExplanationPlan.fromSelected(materialCollision.find(_.story == sameRouteMaterial).get).flatMap(_.allowedClaim),
      Some(ExplanationClaim.MaterialBalanceChanges)
    )
    assertEquals(ExplanationPlan.fromSelected(materialCollision.find(_.story == highRemoveGuard).get), None)

    assertEquals(hangingCollision.count(_.role == Role.Lead), 1)
    assertEquals(hangingCollision.find(_.story == sameRouteHanging).map(_.role), Some(Role.Lead))
    assertEquals(hangingCollision.find(_.story == highRemoveGuard).exists(_.role == Role.Lead), false)
    assertEquals(
      ExplanationPlan.fromSelected(hangingCollision.find(_.story == sameRouteHanging).get).flatMap(_.allowedClaim),
      Some(ExplanationClaim.CanWinPiece)
    )

    assertEquals(allCollision.count(_.role == Role.Lead), 1)
    assertEquals(allCollision.find(_.story == sameRouteHanging).map(_.role), Some(Role.Lead))
    assertEquals(allCollision.find(_.story == sameRouteMaterial).map(_.role), Some(Role.Support))
    assertEquals(allCollision.find(_.story == highRemoveGuard).exists(_.role == Role.Lead), false)

  test("RemoveGuard-5 blocks incomplete Defense reaction and duplicate Pin RemoveGuard lead"):
    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val forgedDefense =
      removeGuard.copy(scene = Scene.Defense, tactic = None, writer = Some(StoryWriter.SceneDefense), threatProof = None, defenseProof = None)
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin.write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val pinRemoveGuard = StoryTable.choose(Vector(pin, removeGuard))
    val reverse = StoryTable.choose(Vector(removeGuard, pin))

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.tactic, verdict.story.route, verdict.role))

    val forgedDefenseVerdict = StoryTable.choose(Vector(removeGuard, forgedDefense)).find(_.story == forgedDefense).get
    assertEquals(forgedDefenseVerdict.role, Role.Blocked)
    assertEquals(forgedDefenseVerdict.leadAllowed, false)
    val forgedDefensePlan = ExplanationPlan.fromSelected(forgedDefenseVerdict)
    assertEquals(forgedDefensePlan.flatMap(_.allowedClaim), None)
    assertEquals(forgedDefensePlan.flatMap(DeterministicRenderer.fromPlan), None)
    assertEquals(shape(pinRemoveGuard), shape(reverse))
    assertEquals(pinRemoveGuard.count(_.role == Role.Lead), 1)
    assertEquals(pinRemoveGuard.map(_.story.tactic).toSet, Set[Option[Tactic]](Some(Tactic.Pin), Some(Tactic.RemoveGuard)))
    pinRemoveGuard.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("RemoveGuard-6 ExplanationPlan lowers selected uncapped RemoveGuard Lead only"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.RemoveGuard))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 5)))
    assertEquals(plan.anchor, Some(Square('c', 4)))
    assertEquals(plan.route, Some(removeGuardMove))
    assertEquals(plan.routeSan, BoardFacts.sanFor(facts, removeGuardMove))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.RemovesDefender))
    assertEquals(plan.evidenceLine, Some(removeGuardMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(ExplanationClaim.RemoveGuardAllowed.map(_.key), Vector("removes_defender"))
    assertEquals(
      ExplanationClaim.RemoveGuardForbiddenKeys,
      Vector(
        "wins_material",
        "target_is_hanging",
        "no_defense",
        "refutes_defense",
        "best_move",
        "only_move",
        "forced",
        "decisive",
        "creates_pressure",
        "takes_initiative"
      )
    )
    ExplanationClaim.RemoveGuardForbiddenKeys.foreach: forbiddenKey =>
      assert(!ExplanationClaim.RemoveGuardAllowed.map(_.key).contains(forbiddenKey), forbiddenKey)
      assert(plan.forbiddenWording.map(_.key).contains(forbiddenKey), forbiddenKey)
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed.map(_.key).toSet.intersect(ExplanationClaim.RemoveGuardForbiddenKeys.toSet),
      Set.empty
    )

  test("RemoveGuard-6 ExplanationPlan gives no standalone claim to non Lead capped refuted or unselected RemoveGuard rows"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val verdict = StoryTable.choose(Vector(story)).head

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(removeGuardMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supportVerdict = verdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = verdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = verdict.copy(role = Role.Blocked, leadAllowed = false)
    val unselectedVerdict = verdict.copy(selected = false)
    val cappedVerdict = StoryTable.choose(Vector(TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Caps)).get)).head
    val refutedVerdict =
      StoryTable.choose(Vector(TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get)).head

    assertEquals(ExplanationPlan.fromSelected(supportVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(contextVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

  test("RemoveGuard-7 DeterministicRenderer phrases selected RemoveGuard ExplanationPlan only"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val forbiddenPhrases =
      Vector(
        "wins material",
        "leaves it undefended",
        "no defender remains",
        "best move",
        "only move",
        "forces",
        "decisive",
        "refutes the defense",
        "creates pressure"
      )

    assertEquals(rendered.text, "Bxc4 removes the defender of the piece on e5.")
    assertEquals(rendered.claimKey, "removes_defender")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assert(rendererMethods.contains("fromPlan"))
    assert(!rendererMethods.contains("fromStory"))
    assert(!rendererMethods.contains("fromVerdict"))
    assert(!rendererMethods.contains("fromBoardFacts"))
    assert(!rendererMethods.contains("fromRemoveGuardProof"))

  test("RemoveGuard-7 DeterministicRenderer refuses RemoveGuard without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val leadPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    Vector(
      leadPlan.copy(role = Role.Support),
      leadPlan.copy(role = Role.Context),
      leadPlan.copy(role = Role.Blocked, debugOnly = true),
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.PinsPiece)),
      leadPlan.copy(tactic = Some(Tactic.Pin)),
      leadPlan.copy(scene = Scene.Material, tactic = None),
      leadPlan.copy(strength = ExplanationStrength.Bounded, evidenceLine = None),
      leadPlan.copy(route = None),
      leadPlan.copy(routeSan = None),
      leadPlan.copy(target = None),
      leadPlan.copy(secondaryTarget = Some(Square('e', 1))),
      leadPlan.copy(forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.RemovesDefender)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None, invalidPlan.toString)

  test("RemoveGuard-8 LLM smoke reuses 8B prompt contract for RemoveGuard RenderedLine only"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text), NarrationSmokeCheck(true, Vector.empty))
    assert(prompt.contains("renderedText: Bxc4 removes the defender of the piece on e5."))
    assert(prompt.contains("claimKey: removes_defender"))
    assert(prompt.contains("strength: bounded"))
    assert(prompt.contains("forbiddenWording: wins material, winning, decisive, best move, only move, forced, cannot move, no defense, front piece must move, wins rear piece, creates pressure, takes initiative, mate threat, king unsafe, target is hanging, leaves it undefended, no defender remains, refutes defense"))
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("raw Story", "RemoveGuardProof", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), forbiddenInput)

  test("RemoveGuard-8 LLM smoke rejects RemoveGuard additions and stronger claims"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val invalidOutputs =
      Vector(
        "Bxc4 wins material by removing the defender of e5." -> "forbidden_wording",
        "Bxc4 leaves it undefended." -> "forbidden_wording",
        "Bxc4 means no defense remains for e5." -> "forbidden_wording",
        "Bxc4 creates pressure on the piece on e5." -> "forbidden_wording",
        "Bxc4 takes the initiative by removing the defender." -> "forbidden_wording",
        "Bxc4 removes the defender of e5, then Qxe5 adds a new line." -> "new_move_or_line",
        "Bxc4 removes the defender and Bg2 adds another line." -> "new_move_or_line",
        "Engine says Bxc4 removes the defender." -> "engine_mention"
      )

    invalidOutputs.foreach: (output, expectedViolation) =>
      val checked = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(checked.accepted, false, output)
      assert(checked.violations.contains(expectedViolation), output)

    Vector(
      plan.copy(allowedClaim = Some(ExplanationClaim.PinsPiece)),
      plan.copy(tactic = Some(Tactic.Pin)),
      plan.copy(role = Role.Support),
      plan.copy(debugOnly = true)
    ).foreach: mismatchedPlan =>
      assertEquals(LlmNarrationSmoke.mockNarrate(mismatchedPlan, rendered), None, mismatchedPlan.toString)

    val mismatchedRendered = rendered.copy(claimKey = "wins_material")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(
      LlmNarrationSmoke.check(plan, mismatchedRendered, rendered.text).violations.contains("input_mismatch"),
      true
    )

  test("RemoveGuard Closeout hard cleanup keeps authority homes separated and public surfaces closed"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val removeGuardProof = story.removeGuardProof.get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val guardSurface =
      classOf[BoardFacts.Guard].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.Guard].getDeclaredFields.map(_.getName).toSet
    val removeGuardProofSurface =
      classOf[RemoveGuardProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RemoveGuardProof].getDeclaredFields.map(_.getName).toSet
    val tacticRemoveGuardSurface =
      TacticRemoveGuard.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticRemoveGuard.getClass.getDeclaredFields.map(_.getName).toSet
    val writerNames = StoryWriter.values.map(_.toString).toVector
    val rendererMethodInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(!guardSurface.exists(_.toLowerCase.contains(forbidden)), forbidden)
    Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine", "Hanging", "Defense", "Pin", "DiscoveredAttack").foreach:
      forbidden =>
        assert(!removeGuardProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector("test", "fixture", "helper", "mock", "fromEngineEvidence", "fromBoardFacts").foreach: forbidden =>
      assert(!tacticRemoveGuardSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    Vector("deflection", "overload", "overloaded", "noDefender", "winsMaterial", "materialGain").foreach: forbidden =>
      assert(!removeGuardProofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

    assertEquals(removeGuardProof.publicClaimAllowed, false)
    assertEquals(removeGuardProof.complete, true)
    assertEquals(story.writer, Some(StoryWriter.TacticRemoveGuard))
    assertEquals(story.tactic, Some(Tactic.RemoveGuard))
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.removeGuardProof.nonEmpty, true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assert(!writerNames.exists(name => Vector("Deflect", "Overload").exists(name.contains)))

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("removes_defender"))
    assertEquals(ExplanationClaim.RemoveGuardAllowed.map(_.key), Vector("removes_defender"))
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed.map(_.key).toSet.intersect(ExplanationClaim.MaterialAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed.map(_.key).toSet.intersect(ExplanationClaim.HangingAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed.map(_.key).toSet.intersect(ExplanationClaim.DefenseAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed.map(_.key).toSet.intersect(ExplanationClaim.PinAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed.map(_.key).toSet.intersect(ExplanationClaim.DiscoveredAttackAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(rendered.claimKey, "removes_defender")
    assertEquals(rendered.text, "Bxc4 removes the defender of the piece on e5.")
    assertEquals(rendererMethodInputs, Vector("ExplanationPlan"))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    Vector(
      "Bxc4 wins material by removing the defender.",
      "Bxc4 leaves it undefended.",
      "Bxc4 means no defender remains.",
      "Bxc4 is a deflection tactic.",
      "Bxc4 overloads the defender.",
      "Bxc4 creates pressure on e5.",
      "Bxc4 takes the initiative."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector("RemoveGuardProof", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach: forbidden =>
      assert(!prompt.contains(forbidden), forbidden)
    Vector(
      Tactic.Hanging,
      Tactic.Fork,
      Tactic.DiscoveredAttack,
      Tactic.Pin,
      Tactic.Skewer,
      Tactic.Overload,
      Tactic.Deflect
    ).foreach: tactic =>
      val forged = story.copy(tactic = Some(tactic))
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked, tactic.toString)
      ExplanationPlan.fromSelected(forgedVerdict).foreach: forgedPlan =>
        assertEquals(forgedPlan.allowedClaim, None, tactic.toString)
        assertEquals(forgedPlan.debugOnly, true, tactic.toString)

  test("LDH-0 keeps same-board RemoveGuard from being stolen by DiscoveredAttack"):
    val facts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val move = Line(Square('d', 3), Square('e', 5))
    val discovered =
      TacticDiscoveredAttack.write(facts, Some(move), Some(Square('b', 1)), Some(Square('g', 6))).get
    val removeGuard =
      TacticRemoveGuard.write(facts, Some(move), Some(Square('g', 6)), Some(Square('e', 5))).get
    val high =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val tied = Vector(discovered.copy(proof = high), removeGuard.copy(proof = high))
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.tactic, verdict.story.target, verdict.story.route, verdict.role))

    val removeGuardVerdict = forward.find(_.story.tactic.contains(Tactic.RemoveGuard)).get
    val discoveredVerdict = forward.find(_.story.tactic.contains(Tactic.DiscoveredAttack)).get

    assertEquals(shape(forward), shape(reverse))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(forward.map(_.story.route).distinct, Vector(Some(move)))
    assertEquals(removeGuardVerdict.role, Role.Lead)
    assertEquals(discoveredVerdict.role == Role.Lead, false)
    assertEquals(
      ExplanationPlan.fromSelected(removeGuardVerdict).flatMap(_.allowedClaim).map(_.key),
      Some("removes_defender")
    )
    assertEquals(
      ExplanationPlan.fromSelected(removeGuardVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("removes_defender")
    )
    assertEquals(ExplanationPlan.fromSelected(discoveredVerdict), None)

  test("LDH-0 complex same-board fixture keeps row roles stable and downstream smoke bounded"):
    val facts = BoardFacts.fromFen("7k/8/6r1/4n3/2q5/3N4/8/1B5K w - - 0 1").toOption.get
    val move = Line(Square('d', 3), Square('e', 5))
    val hanging = TacticHanging.write(facts, move).get
    val material = SceneMaterial.write(facts, move).get
    val fork = TacticFork.write(facts, Some(move), Some(Square('c', 4)), Some(Square('g', 6))).get
    val discovered =
      TacticDiscoveredAttack.write(facts, Some(move), Some(Square('b', 1)), Some(Square('g', 6))).get
    val removeGuard =
      TacticRemoveGuard.write(facts, Some(move), Some(Square('g', 6)), Some(Square('e', 5))).get
    val high =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val tied =
      Vector(
        hanging.copy(proof = high),
        material.copy(proof = high),
        fork.copy(proof = high),
        discovered.copy(proof = high),
        removeGuard.copy(proof = high)
      )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(tied(3), tied(1), tied(4), tied(0), tied(2)))

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.target, verdict.role))

    val lead = forward.find(_.role == Role.Lead).get
    val leadPlan = ExplanationPlan.fromSelected(lead).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get

    assertEquals(shape(forward), shape(reverse))
    assertEquals(shape(forward), shape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(lead.story.tactic, Some(Tactic.Hanging))
    assertEquals(leadPlan.allowedClaim.map(_.key), Some("can_win_piece"))
    assertEquals(rendered.claimKey, "can_win_piece")
    forward.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

    Vector(
      "Nxe5 reveals an attack.",
      "Nxe5 pins the piece.",
      "Nxe5 removes the defender.",
      "Nxe5 creates pressure.",
      "Nxe5 threatens mate.",
      "Nxe5 is the best move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(leadPlan, rendered, output).accepted, false, output)

  test("LDH-1 fixture map covers complex same-board line defender interactions"):
    final case class LdhFixture(
        category: String,
        fen: String,
        sideToMove: Side,
        candidateLegalLines: Vector[Line],
        rows: Vector[(String, Story)],
        expectedOpenRows: Set[String],
        expectedBlockedRows: Set[String],
        expectedRoles: Map[String, Role],
        expectedSelectedVerdict: String,
        expectedSelectedRole: Role,
        forbiddenClaims: Vector[String]
    )

    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    def strong(story: Story): Story = story.copy(proof = score(99))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def forbiddenOutput(claim: String, plan: ExplanationPlan): String =
      val san = plan.routeSan.getOrElse("Move")
      claim match
        case "wins_material"              => s"$san wins material."
        case "best_move"                  => s"$san is the best move."
        case "creates_pressure"           => s"$san creates pressure."
        case "takes_initiative"           => s"$san takes the initiative."
        case "mate_threat"                => s"$san threatens mate."
        case "king_unsafe"                => s"$san makes the king unsafe."
        case "skewers_piece"              => s"$san skewers the piece."
        case "xray"                       => s"$san is an XRay tactic."
        case "reveals_attack_on_piece"    => s"$san reveals an attack."
        case "pins_piece"                 => s"$san pins the piece."
        case "removes_defender"           => s"$san removes the defender."
        case "refutes_defense"            => s"$san refutes the defense."
        case "no_defense"                 => s"$san leaves no defense."
        case other                        => s"$san adds $other."

    def assertFixture(fixture: LdhFixture): Unit =
      val facts = BoardFacts.fromFen(fixture.fen).toOption.get
      assertEquals(facts.sideToMove, fixture.sideToMove, fixture.category)
      fixture.candidateLegalLines.foreach: line =>
        assert(facts.sideLegal.lines.contains(line), s"${fixture.category} legal line missing: $line")
      assertEquals(fixture.rows.map(_._1).toSet, fixture.expectedOpenRows ++ fixture.expectedBlockedRows, fixture.category)

      val forward = StoryTable.choose(fixture.rows.map(_._2))
      val reverse = StoryTable.choose(fixture.rows.reverse.map(_._2))

      def shape(verdicts: Vector[Verdict]) =
        verdicts.map(verdict => (rowId(fixture.rows, verdict.story), verdict.role, verdict.engineCheckStatus))

      val roleMap = forward.map(verdict => rowId(fixture.rows, verdict.story) -> verdict.role).toMap
      val selected = forward.head
      val selectedId = rowId(fixture.rows, selected.story)

      assertEquals(shape(forward), shape(reverse), fixture.category)
      assertEquals(roleMap, fixture.expectedRoles, fixture.category)
      fixture.expectedOpenRows.foreach: id =>
        assert(roleMap.get(id).exists(_ != Role.Blocked), s"${fixture.category} open row blocked: $id")
      fixture.expectedBlockedRows.foreach: id =>
        assertEquals(roleMap.get(id), Some(Role.Blocked), s"${fixture.category} blocked row role: $id")
      assertEquals(selectedId, fixture.expectedSelectedVerdict, fixture.category)
      assertEquals(selected.role, fixture.expectedSelectedRole, fixture.category)
      assertEquals(forward.count(_.role == Role.Lead), fixture.expectedRoles.values.count(_ == Role.Lead), fixture.category)

      forward.filter(_.role != Role.Lead).foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

      if selected.role == Role.Lead && !selected.engineStrengthLimited then
        val plan = ExplanationPlan.fromSelected(selected).get
        val rendered = DeterministicRenderer.fromPlan(plan).get
        fixture.forbiddenClaims.foreach: claim =>
          val output = forbiddenOutput(claim, plan)
          assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"${fixture.category}: $claim")
      else
        assertEquals(ExplanationPlan.fromSelected(selected), None, fixture.category)

    val discoveredPinFen = "6nk/8/6r1/r6q/5N2/3N4/8/1BR1K3 w - - 0 1"
    val discoveredPinMove = Line(Square('d', 3), Square('e', 5))
    val discoveredPinPinMove = Line(Square('c', 1), Square('c', 8))
    val discoveredPinFacts = BoardFacts.fromFen(discoveredPinFen).toOption.get
    val discoveredPinDiscovered =
      strong(TacticDiscoveredAttack.write(discoveredPinFacts, Some(discoveredPinMove), Some(Square('b', 1)), Some(Square('g', 6))).get)
    val discoveredPinPin =
      strong(TacticPin.write(discoveredPinFacts, Some(discoveredPinPinMove), Some(Square('c', 8)), Some(Square('g', 8)), Some(Square('h', 8))).get)

    val discoveredRemoveFen = "7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1"
    val discoveredRemoveMove = Line(Square('d', 3), Square('e', 5))
    val discoveredRemoveFacts = BoardFacts.fromFen(discoveredRemoveFen).toOption.get
    val discoveredRemoveDiscovered =
      strong(TacticDiscoveredAttack.write(discoveredRemoveFacts, Some(discoveredRemoveMove), Some(Square('b', 1)), Some(Square('g', 6))).get)
    val discoveredRemoveRemove =
      strong(TacticRemoveGuard.write(discoveredRemoveFacts, Some(discoveredRemoveMove), Some(Square('g', 6)), Some(Square('e', 5))).get)
    val discoveredRemoveMaterial = strong(SceneMaterial.write(discoveredRemoveFacts, discoveredRemoveMove).get)
    val discoveredRemoveHanging = strong(TacticHanging.write(discoveredRemoveFacts, discoveredRemoveMove).get)

    val pinRemoveFen = "8/7k/6r1/4n3/8/3N4/8/1B5K w - - 0 1"
    val pinRemoveMove = Line(Square('d', 3), Square('e', 5))
    val pinRemoveFacts = BoardFacts.fromFen(pinRemoveFen).toOption.get
    val pinRemovePin =
      strong(TacticPin.write(pinRemoveFacts, Some(pinRemoveMove), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get)
    val pinRemoveRemove =
      strong(TacticRemoveGuard.write(pinRemoveFacts, Some(pinRemoveMove), Some(Square('g', 6)), Some(Square('e', 5))).get)

    val allThreeFen = "6nk/8/6r1/r6q/5N2/3N4/8/1BR1K3 w - - 0 1"
    val allThreeFacts = BoardFacts.fromFen(allThreeFen).toOption.get
    val allThreeDiscoveredMove = Line(Square('d', 3), Square('e', 5))
    val allThreePinMove = Line(Square('c', 1), Square('c', 8))
    val allThreeRemoveMove = Line(Square('f', 4), Square('d', 5))
    val allThreeDiscovered =
      strong(TacticDiscoveredAttack.write(allThreeFacts, Some(allThreeDiscoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).get)
    val allThreePin =
      strong(TacticPin.write(allThreeFacts, Some(allThreePinMove), Some(Square('c', 8)), Some(Square('g', 8)), Some(Square('h', 8))).get)
    val allThreeRemove =
      strong(TacticRemoveGuard.write(allThreeFacts, Some(allThreeRemoveMove), Some(Square('h', 5)), Some(Square('a', 5))).get)
    val allThreeSkewerBlocked = allThreePin.copy(tactic = Some(Tactic.Skewer), writer = None)

    val defenseFen = "4k1B1/8/8/4rn2/2nQ4/8/8/7K w - - 0 1"
    val defenseFacts = BoardFacts.fromFen(defenseFen).toOption.get
    val defenseRemoveMove = Line(Square('g', 8), Square('c', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseRemove =
      strong(TacticRemoveGuard.write(defenseFacts, Some(defenseRemoveMove), Some(Square('e', 5)), Some(Square('c', 4))).get)
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get)

    Vector(
      LdhFixture(
        category = "DiscoveredAttack vs Pin",
        fen = discoveredPinFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredPinMove, discoveredPinPinMove),
        rows = Vector(
          "Tactic.DiscoveredAttack" -> discoveredPinDiscovered,
          "Tactic.Pin" -> discoveredPinPin
        ),
        expectedOpenRows = Set("Tactic.DiscoveredAttack", "Tactic.Pin"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.Pin" -> Role.Lead, "Tactic.DiscoveredAttack" -> Role.Support),
        expectedSelectedVerdict = "Tactic.Pin",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("wins_material", "reveals_attack_on_piece", "removes_defender", "best_move", "creates_pressure", "takes_initiative", "mate_threat", "skewers_piece")
      ),
      LdhFixture(
        category = "DiscoveredAttack vs RemoveGuard",
        fen = discoveredRemoveFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredRemoveMove),
        rows = Vector(
          "Tactic.DiscoveredAttack" -> discoveredRemoveDiscovered,
          "Tactic.RemoveGuard" -> discoveredRemoveRemove
        ),
        expectedOpenRows = Set("Tactic.DiscoveredAttack", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.RemoveGuard" -> Role.Lead, "Tactic.DiscoveredAttack" -> Role.Support),
        expectedSelectedVerdict = "Tactic.RemoveGuard",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("wins_material", "reveals_attack_on_piece", "pins_piece", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      ),
      LdhFixture(
        category = "Pin vs RemoveGuard",
        fen = pinRemoveFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(pinRemoveMove),
        rows = Vector(
          "Tactic.Pin" -> pinRemovePin,
          "Tactic.RemoveGuard" -> pinRemoveRemove
        ),
        expectedOpenRows = Set("Tactic.Pin", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.Pin" -> Role.Lead, "Tactic.RemoveGuard" -> Role.Support),
        expectedSelectedVerdict = "Tactic.Pin",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("wins_material", "reveals_attack_on_piece", "removes_defender", "king_unsafe", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      ),
      LdhFixture(
        category = "DiscoveredAttack + Pin + RemoveGuard same-board",
        fen = allThreeFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(allThreeDiscoveredMove, allThreePinMove, allThreeRemoveMove),
        rows = Vector(
          "Tactic.DiscoveredAttack" -> allThreeDiscovered,
          "Tactic.Pin" -> allThreePin,
          "Tactic.RemoveGuard" -> allThreeRemove,
          "Tactic.Skewer/blocked" -> allThreeSkewerBlocked
        ),
        expectedOpenRows = Set("Tactic.DiscoveredAttack", "Tactic.Pin", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set("Tactic.Skewer/blocked"),
        expectedRoles = Map(
          "Tactic.Pin" -> Role.Lead,
          "Tactic.DiscoveredAttack" -> Role.Support,
          "Tactic.RemoveGuard" -> Role.Support,
          "Tactic.Skewer/blocked" -> Role.Blocked
        ),
        expectedSelectedVerdict = "Tactic.Pin",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("wins_material", "reveals_attack_on_piece", "removes_defender", "skewers_piece", "xray", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      ),
      LdhFixture(
        category = "Line/Defender row vs Material",
        fen = discoveredRemoveFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredRemoveMove),
        rows = Vector(
          "Scene.Material" -> discoveredRemoveMaterial,
          "Tactic.RemoveGuard" -> discoveredRemoveRemove
        ),
        expectedOpenRows = Set("Scene.Material", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Material" -> Role.Lead, "Tactic.RemoveGuard" -> Role.Support),
        expectedSelectedVerdict = "Scene.Material",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("removes_defender", "pins_piece", "reveals_attack_on_piece", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      ),
      LdhFixture(
        category = "Line/Defender row vs Hanging",
        fen = discoveredRemoveFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredRemoveMove),
        rows = Vector(
          "Tactic.Hanging" -> discoveredRemoveHanging,
          "Tactic.RemoveGuard" -> discoveredRemoveRemove
        ),
        expectedOpenRows = Set("Tactic.Hanging", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.Hanging" -> Role.Lead, "Tactic.RemoveGuard" -> Role.Support),
        expectedSelectedVerdict = "Tactic.Hanging",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("removes_defender", "pins_piece", "reveals_attack_on_piece", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      ),
      LdhFixture(
        category = "Line/Defender row vs Defense",
        fen = defenseFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(defenseRemoveMove, defenseMove),
        rows = Vector(
          "Scene.Defense" -> defense,
          "Tactic.RemoveGuard" -> defenseRemove
        ),
        expectedOpenRows = Set("Scene.Defense", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Defense" -> Role.Lead, "Tactic.RemoveGuard" -> Role.Support),
        expectedSelectedVerdict = "Scene.Defense",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("wins_material", "refutes_defense", "no_defense", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      )
    ).foreach(assertFixture)

  test("LDH-1 fixture map covers EngineCheck statuses over existing line defender rows"):
    final case class EngineLdhFixture(
        category: String,
        fen: String,
        sideToMove: Side,
        candidateLegalLines: Vector[Line],
        baseRowId: String,
        baseStory: Story,
        attach: (Story, EngineCheck) => Option[Story],
        expectedSupportsRole: Role,
        expectedCapsRole: Role,
        expectedClaim: Option[ExplanationClaim],
        forbiddenClaims: Vector[String]
    )

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def forbiddenOutput(claim: String, plan: ExplanationPlan): String =
      val san = plan.routeSan.getOrElse("Move")
      claim match
        case "wins_material"              => s"$san wins material."
        case "best_move"                  => s"$san is the best move."
        case "creates_pressure"           => s"$san creates pressure."
        case "takes_initiative"           => s"$san takes the initiative."
        case "mate_threat"                => s"$san threatens mate."
        case "reveals_attack_on_piece"    => s"$san reveals an attack."
        case "pins_piece"                 => s"$san pins the piece."
        case "removes_defender"           => s"$san removes the defender."
        case "wins_rear_piece"            => s"$san wins the rear piece."
        case "front_piece_must_move"      => s"$san forces the front piece to move."
        case other                        => s"$san adds $other."

    val discoveredFen = "7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1"
    val discoveredFacts = BoardFacts.fromFen(discoveredFen).toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).get
    val pinFen = "r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1"
    val pinFacts = BoardFacts.fromFen(pinFen).toOption.get
    val pinMove = Line(Square('a', 8), Square('e', 8))
    val pin = TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val removeFen = "6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1"
    val removeFacts = BoardFacts.fromFen(removeFen).toOption.get
    val removeMove = Line(Square('g', 8), Square('c', 4))
    val remove =
      TacticRemoveGuard.write(removeFacts, Some(removeMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val skewerFen = "4r2k/8/8/4q3/8/8/8/R6K w - - 0 1"
    val skewerFacts = BoardFacts.fromFen(skewerFen).toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get

    val fixtures = Vector(
      EngineLdhFixture(
        category = "EngineCheck over Tactic.DiscoveredAttack",
        fen = discoveredFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredMove),
        baseRowId = "Tactic.DiscoveredAttack",
        baseStory = discovered,
        attach = TacticDiscoveredAttack.withEngineCheck,
        expectedSupportsRole = Role.Lead,
        expectedCapsRole = Role.Lead,
        expectedClaim = Some(ExplanationClaim.RevealsAttackOnPiece),
        forbiddenClaims = Vector("wins_material", "pins_piece", "removes_defender", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      ),
      EngineLdhFixture(
        category = "EngineCheck over Tactic.Pin",
        fen = pinFen,
        sideToMove = Side.Black,
        candidateLegalLines = Vector(pinMove),
        baseRowId = "Tactic.Pin",
        baseStory = pin,
        attach = TacticPin.withEngineCheck,
        expectedSupportsRole = Role.Lead,
        expectedCapsRole = Role.Lead,
        expectedClaim = Some(ExplanationClaim.PinsPiece),
        forbiddenClaims = Vector("wins_material", "reveals_attack_on_piece", "removes_defender", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      ),
      EngineLdhFixture(
        category = "EngineCheck over Tactic.RemoveGuard",
        fen = removeFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(removeMove),
        baseRowId = "Tactic.RemoveGuard",
        baseStory = remove,
        attach = TacticRemoveGuard.withEngineCheck,
        expectedSupportsRole = Role.Lead,
        expectedCapsRole = Role.Lead,
        expectedClaim = Some(ExplanationClaim.RemovesDefender),
        forbiddenClaims = Vector("wins_material", "reveals_attack_on_piece", "pins_piece", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      ),
      EngineLdhFixture(
        category = "EngineCheck over Tactic.Skewer",
        fen = skewerFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(skewerMove),
        baseRowId = "Tactic.Skewer",
        baseStory = skewer,
        attach = TacticSkewer.withEngineCheck,
        expectedSupportsRole = Role.Context,
        expectedCapsRole = Role.Context,
        expectedClaim = None,
        forbiddenClaims = Vector("wins_material", "wins_rear_piece", "front_piece_must_move", "best_move", "creates_pressure", "takes_initiative", "mate_threat")
      )
    )

    fixtures.foreach: fixture =>
      val facts = BoardFacts.fromFen(fixture.fen).toOption.get
      assertEquals(facts.sideToMove, fixture.sideToMove, fixture.category)
      fixture.candidateLegalLines.foreach: line =>
        assert(facts.sideLegal.lines.contains(line), s"${fixture.category} legal line missing: $line")
      val line = fixture.candidateLegalLines.head
      val supports =
        fixture.attach(fixture.baseStory, engineCheck(facts, fixture.baseStory, line, EngineCheckStatus.Supports)).get
      val caps =
        fixture.attach(fixture.baseStory, engineCheck(facts, fixture.baseStory, line, EngineCheckStatus.Caps)).get
      val refutes =
        fixture
          .attach(fixture.baseStory, engineCheck(facts, fixture.baseStory, line, EngineCheckStatus.Supports, before = 220, after = 20))
          .get

      val supportsVerdict = StoryTable.choose(Vector(supports)).head
      val capsVerdict = StoryTable.choose(Vector(caps)).head
      val refutesVerdict = StoryTable.choose(Vector(refutes)).head

      assertEquals(supportsVerdict.role, fixture.expectedSupportsRole, fixture.category)
      assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports), fixture.category)
      fixture.expectedClaim match
        case Some(expectedClaim) =>
          val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
          val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
          assertEquals(supportsPlan.allowedClaim, Some(expectedClaim), fixture.category)
          fixture.forbiddenClaims.foreach: claim =>
            assertEquals(LlmNarrationSmoke.check(supportsPlan, supportsRendered, forbiddenOutput(claim, supportsPlan)).accepted, false, s"${fixture.category}: $claim")
        case None =>
          assertEquals(ExplanationPlan.fromSelected(supportsVerdict), None, fixture.category)

      assertEquals(capsVerdict.role, fixture.expectedCapsRole, fixture.category)
      assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps), fixture.category)
      assertEquals(capsVerdict.engineStrengthLimited, true, fixture.category)
      assertEquals(ExplanationPlan.fromSelected(capsVerdict), None, fixture.category)

      assertEquals(refutesVerdict.role, Role.Blocked, fixture.category)
      assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), fixture.category)
      assertEquals(refutesVerdict.leadAllowed, false, fixture.category)
      assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None, fixture.category)

  test("LDH-2 Role Stability keeps selected Verdict stable across input order"):
    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    def strong(story: Story): Story = story.copy(proof = score(99))

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val discoveredRemoveFen = "7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1"
    val discoveredRemoveFacts = BoardFacts.fromFen(discoveredRemoveFen).toOption.get
    val discoveredRemoveMove = Line(Square('d', 3), Square('e', 5))
    val discovered =
      strong(TacticDiscoveredAttack.write(discoveredRemoveFacts, Some(discoveredRemoveMove), Some(Square('b', 1)), Some(Square('g', 6))).get)
    val removeGuard =
      strong(TacticRemoveGuard.write(discoveredRemoveFacts, Some(discoveredRemoveMove), Some(Square('g', 6)), Some(Square('e', 5))).get)

    val pinFen = "8/7k/6r1/4n3/8/3N4/8/1B5K w - - 0 1"
    val pinFacts = BoardFacts.fromFen(pinFen).toOption.get
    val pinMove = Line(Square('d', 3), Square('e', 5))
    val pin =
      strong(TacticPin.write(pinFacts, Some(pinMove), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get)
    val incompletePin = pin.copy(pinProof = None)
    val cappedPin =
      TacticPin.withEngineCheck(pin, engineCheck(pinFacts, pin, pinMove, EngineCheckStatus.Caps)).get
    val refutedRemoveGuard =
      TacticRemoveGuard
        .withEngineCheck(removeGuard, engineCheck(discoveredRemoveFacts, removeGuard, discoveredRemoveMove, EngineCheckStatus.Supports, before = 220, after = 20))
        .get

    val rows = Vector(
      "Tactic.DiscoveredAttack" -> discovered,
      "Tactic.Pin" -> pin,
      "Tactic.RemoveGuard" -> removeGuard,
      "Tactic.Pin/incomplete" -> incompletePin,
      "Tactic.Pin/capped" -> cappedPin,
      "Tactic.RemoveGuard/refuted" -> refutedRemoveGuard
    )
    val permutations =
      Vector(
        rows,
        rows.reverse,
        Vector(rows(3), rows(0), rows(5), rows(2), rows(4), rows(1))
      )

    def rowId(story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (rowId(verdict.story), verdict.role, verdict.engineCheckStatus, verdict.engineStrengthLimited))

    val selectedShapes = permutations.map(input => shape(StoryTable.choose(input.map(_._2))))
    val baseline = selectedShapes.head
    val baselineVerdicts = StoryTable.choose(rows.map(_._2))
    val selected = baselineVerdicts.head

    selectedShapes.foreach: current =>
      assertEquals(current, baseline)
    assertEquals(rowId(selected.story), "Tactic.Pin")
    assertEquals(selected.role, Role.Lead)
    assertEquals(baselineVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(baselineVerdicts.find(_.story == incompletePin).map(_.role), Some(Role.Blocked))
    assertEquals(baselineVerdicts.find(_.story == refutedRemoveGuard).map(_.role), Some(Role.Blocked))
    assertEquals(baselineVerdicts.find(_.story == cappedPin).map(_.role), Some(Role.Support))
    assertEquals(ExplanationPlan.fromSelected(baselineVerdicts.find(_.story == cappedPin).get), None)

  test("LDH-2 Role Stability keeps line and defender meanings from duplicate Lead ownership"):
    def score(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = 0,
        clarity = value
      )

    def strong(story: Story): Story = story.copy(proof = score(99))

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val sharedLine = Line(Square('d', 3), Square('f', 4))
    val discoveredSameLine =
      strong(TacticDiscoveredAttack.write(discoveredFacts, Some(sharedLine), Some(Square('b', 1)), Some(Square('g', 6))).get)
    val pinSameLine =
      strong(TacticPin.write(pinFacts, Some(sharedLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get)
    val sameLine = StoryTable.choose(Vector(discoveredSameLine, pinSameLine))

    assertEquals(sameLine.count(_.role == Role.Lead), 1)
    assertEquals(sameLine.map(_.story.route).distinct, Vector(Some(sharedLine)))
    assertEquals(sameLine.find(_.story.tactic.contains(Tactic.Pin)).map(_.role), Some(Role.Lead))
    assertEquals(sameLine.find(_.story.tactic.contains(Tactic.DiscoveredAttack)).exists(_.role == Role.Lead), false)
    sameLine.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, verdict.toString)

    val pinRemoveFacts = BoardFacts.fromFen("8/7k/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinRemoveLine = Line(Square('d', 3), Square('e', 5))
    val pin =
      strong(TacticPin.write(pinRemoveFacts, Some(pinRemoveLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get)
    val removeGuardOnPinLine =
      strong(TacticRemoveGuard.write(pinRemoveFacts, Some(pinRemoveLine), Some(Square('g', 6)), Some(Square('e', 5))).get)
    val pinRemove = StoryTable.choose(Vector(removeGuardOnPinLine, pin))
    val pinLead = pinRemove.find(_.story.tactic.contains(Tactic.Pin)).get
    val removeGuardSupport = pinRemove.find(_.story.tactic.contains(Tactic.RemoveGuard)).get

    assertEquals(pinRemove.count(_.role == Role.Lead), 1)
    assertEquals(pinLead.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(pinLead).flatMap(_.allowedClaim), Some(ExplanationClaim.PinsPiece))
    assertEquals(removeGuardSupport.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(removeGuardSupport), None)

    val discoveredRemoveFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredRemoveLine = Line(Square('d', 3), Square('e', 5))
    val discovered =
      strong(TacticDiscoveredAttack.write(discoveredRemoveFacts, Some(discoveredRemoveLine), Some(Square('b', 1)), Some(Square('g', 6))).get)
    val removeGuard =
      strong(TacticRemoveGuard.write(discoveredRemoveFacts, Some(discoveredRemoveLine), Some(Square('g', 6)), Some(Square('e', 5))).get)
    val discoveredRemove = StoryTable.choose(Vector(discovered, removeGuard))
    val discoveredSupport = discoveredRemove.find(_.story.tactic.contains(Tactic.DiscoveredAttack)).get
    val removeGuardLead = discoveredRemove.find(_.story.tactic.contains(Tactic.RemoveGuard)).get

    assertEquals(discoveredRemove.count(_.role == Role.Lead), 1)
    assertEquals(removeGuardLead.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(removeGuardLead).flatMap(_.allowedClaim), Some(ExplanationClaim.RemovesDefender))
    assertEquals(discoveredSupport.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(discoveredSupport), None)

    val duplicatePin = StoryTable.choose(Vector(pin, pin.copy(anchor = pin.anchor)))
    assertEquals(duplicatePin.count(_.role == Role.Lead), 1)

  test("LDH-3 Meaning Ownership Boundary keeps each open row on its own claim key"):
    def leadPlan(story: Story): (Verdict, ExplanationPlan, RenderedLine) =
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      (verdict, plan, rendered)

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val (_, discoveredPlan, discoveredRendered) =
      leadPlan(TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6))).get)

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinLine = Line(Square('d', 3), Square('f', 4))
    val (_, pinPlan, pinRendered) =
      leadPlan(TacticPin.write(pinFacts, Some(pinLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get)

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val (_, removeGuardPlan, removeGuardRendered) =
      leadPlan(TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5))).get)

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialLine = Line(Square('d', 4), Square('e', 5))
    val (_, materialPlan, materialRendered) =
      leadPlan(SceneMaterial.write(materialFacts, materialLine).get)
    val (_, hangingPlan, hangingRendered) =
      leadPlan(TacticHanging.write(materialFacts, materialLine).get)

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val (_, defensePlan, defenseRendered) =
      leadPlan(SceneDefense.write(defenseFacts, threatLine, defenseLine).get)

    assertEquals(discoveredPlan.allowedClaim.map(_.key), Some("reveals_attack_on_piece"))
    assertEquals(pinPlan.allowedClaim.map(_.key), Some("pins_piece"))
    assertEquals(removeGuardPlan.allowedClaim.map(_.key), Some("removes_defender"))
    assertEquals(materialPlan.allowedClaim.map(_.key), Some("material_balance_changes"))
    assertEquals(hangingPlan.allowedClaim.map(_.key), Some("can_win_piece"))
    assertEquals(defensePlan.allowedClaim.map(_.key), Some("defends_piece"))

    assertEquals(discoveredPlan.scene, Scene.Tactic)
    assertEquals(discoveredPlan.tactic, Some(Tactic.DiscoveredAttack))
    assertEquals(pinPlan.scene, Scene.Tactic)
    assertEquals(pinPlan.tactic, Some(Tactic.Pin))
    assertEquals(removeGuardPlan.scene, Scene.Tactic)
    assertEquals(removeGuardPlan.tactic, Some(Tactic.RemoveGuard))
    assertEquals(materialPlan.scene, Scene.Material)
    assertEquals(materialPlan.tactic, None)
    assertEquals(hangingPlan.scene, Scene.Tactic)
    assertEquals(hangingPlan.tactic, Some(Tactic.Hanging))
    assertEquals(defensePlan.scene, Scene.Defense)
    assertEquals(defensePlan.tactic, None)

    assert(hangingRendered.text.toLowerCase.contains("wins material"))
    assert(!discoveredRendered.text.toLowerCase.contains("wins material"))
    assert(!pinRendered.text.toLowerCase.contains("cannot move"))
    assert(!pinRendered.text.toLowerCase.contains("king unsafe"))
    assert(!removeGuardRendered.text.toLowerCase.contains("material"))
    assert(!materialRendered.text.toLowerCase.contains("pin"))
    assert(!materialRendered.text.toLowerCase.contains("discovered"))
    assert(!defenseRendered.text.toLowerCase.contains("best"))

  test("LDH-3 Meaning Ownership Boundary rejects cross-meaning public wording"):
    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard = TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5))).get
    val removeGuardPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(removeGuard)).head).get
    val removeGuardRendered = DeterministicRenderer.fromPlan(removeGuardPlan).get
    val removeGuardMaterialGain =
      LlmNarrationSmoke.check(removeGuardPlan, removeGuardRendered, s"${removeGuardPlan.routeSan.get} gains material.")

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinLine = Line(Square('d', 3), Square('f', 4))
    val pin = TacticPin.write(pinFacts, Some(pinLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get
    val pinPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(pin)).head).get
    val pinRendered = DeterministicRenderer.fromPlan(pinPlan).get
    val pinKingUnsafe =
      LlmNarrationSmoke.check(pinPlan, pinRendered, s"${pinPlan.routeSan.get} pins the piece on g6 and the king is unsafe.")
    val pinCannotMove =
      LlmNarrationSmoke.check(pinPlan, pinRendered, s"${pinPlan.routeSan.get} means the piece on g6 cannot move.")

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6))).get
    val discoveredPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(discovered)).head).get
    val discoveredRendered = DeterministicRenderer.fromPlan(discoveredPlan).get
    val discoveredMaterialGain =
      LlmNarrationSmoke.check(discoveredPlan, discoveredRendered, s"${discoveredPlan.routeSan.get} gains material.")

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialLine = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialLine).get
    val materialPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(material)).head).get
    val materialRendered = DeterministicRenderer.fromPlan(materialPlan).get
    val materialLineTactic =
      LlmNarrationSmoke.check(materialPlan, materialRendered, s"After ${materialPlan.routeSan.get}, this line tactic leaves White ahead in material.")

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, threatLine, defenseLine).get
    val incompleteDefense = defense.copy(threatProof = None)
    val incompleteDefenseVerdict = StoryTable.choose(Vector(incompleteDefense)).head

    assertEquals(removeGuardPlan.allowedClaim, Some(ExplanationClaim.RemovesDefender))
    assertEquals(removeGuardMaterialGain.accepted, false)
    assert(removeGuardMaterialGain.violations.contains("forbidden_wording"), removeGuardMaterialGain.toString)
    assertEquals(pinKingUnsafe.accepted, false)
    assert(pinKingUnsafe.violations.contains("forbidden_wording"), pinKingUnsafe.toString)
    assertEquals(pinCannotMove.accepted, false)
    assert(pinCannotMove.violations.contains("forbidden_wording"), pinCannotMove.toString)
    assertEquals(discoveredMaterialGain.accepted, false)
    assert(discoveredMaterialGain.violations.contains("forbidden_wording"), discoveredMaterialGain.toString)
    assert(materialPlan.forbiddenWording.map(_.key).contains("line_tactic_identity"))
    assertEquals(materialLineTactic.accepted, false)
    assert(materialLineTactic.violations.contains("forbidden_wording"), materialLineTactic.toString)
    assertEquals(incompleteDefenseVerdict.role, Role.Blocked)
    assertEquals(incompleteDefenseVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(incompleteDefenseVerdict).flatMap(_.allowedClaim), None)
    assertEquals(ExplanationPlan.fromSelected(incompleteDefenseVerdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("LDH-4 EngineCheck Interaction reuses existing statuses without engine-owned public claims"):
    final case class EngineOwnershipFixture(
        label: String,
        facts: BoardFacts,
        story: Story,
        line: Line,
        attach: (Story, EngineCheck) => Option[Story],
        expectedClaim: ExplanationClaim
    )

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6))).get

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinLine = Line(Square('d', 3), Square('f', 4))
    val pin =
      TacticPin.write(pinFacts, Some(pinLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5))).get

    Vector(
      EngineOwnershipFixture(
        "Tactic.DiscoveredAttack",
        discoveredFacts,
        discovered,
        discoveredLine,
        TacticDiscoveredAttack.withEngineCheck,
        ExplanationClaim.RevealsAttackOnPiece
      ),
      EngineOwnershipFixture(
        "Tactic.Pin",
        pinFacts,
        pin,
        pinLine,
        TacticPin.withEngineCheck,
        ExplanationClaim.PinsPiece
      ),
      EngineOwnershipFixture(
        "Tactic.RemoveGuard",
        removeGuardFacts,
        removeGuard,
        removeGuardLine,
        TacticRemoveGuard.withEngineCheck,
        ExplanationClaim.RemovesDefender
      )
    ).foreach: fixture =>
      val baseVerdict = StoryTable.choose(Vector(fixture.story)).head
      val basePlan = ExplanationPlan.fromSelected(baseVerdict).get
      val baseRendered = DeterministicRenderer.fromPlan(basePlan).get
      val supports = fixture.attach(fixture.story, engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Supports)).get
      val caps = fixture.attach(fixture.story, engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Caps)).get
      val refutes =
        fixture.attach(fixture.story, engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Supports, before = 220, after = 20)).get
      val unknown = fixture.attach(fixture.story, engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Unknown)).get
      val supportsVerdict = StoryTable.choose(Vector(supports)).head
      val capsVerdict = StoryTable.choose(Vector(caps)).head
      val refutesVerdict = StoryTable.choose(Vector(refutes)).head
      val unknownVerdict = StoryTable.choose(Vector(unknown)).head
      val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
      val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
      val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get
      val unknownRendered = DeterministicRenderer.fromPlan(unknownPlan).get

      assertEquals(basePlan.allowedClaim, Some(fixture.expectedClaim), fixture.label)
      assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports), fixture.label)
      assertEquals(supportsPlan.allowedClaim, basePlan.allowedClaim, fixture.label)
      assertEquals(supportsRendered.claimKey, baseRendered.claimKey, fixture.label)
      assertEquals(supportsRendered.text, baseRendered.text, fixture.label)

      assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps), fixture.label)
      assertEquals(capsVerdict.engineStrengthLimited, true, fixture.label)
      assertEquals(ExplanationPlan.fromSelected(capsVerdict), None, fixture.label)
      assertEquals(ExplanationPlan.fromSelected(capsVerdict).flatMap(DeterministicRenderer.fromPlan), None, fixture.label)

      assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), fixture.label)
      assertEquals(refutesVerdict.role, Role.Blocked, fixture.label)
      assertEquals(refutesVerdict.leadAllowed, false, fixture.label)
      assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None, fixture.label)

      assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown), fixture.label)
      assertEquals(unknownPlan.allowedClaim, basePlan.allowedClaim, fixture.label)
      assertEquals(unknownRendered.text, baseRendered.text, fixture.label)

      Vector(supportsPlan -> supportsRendered, unknownPlan -> unknownRendered).foreach: (plan, rendered) =>
        Vector(
          s"${plan.routeSan.get} is supported because the engine says so." -> "engine_mention",
          s"${plan.routeSan.get} follows the principal variation." -> "engine_mention",
          s"${plan.routeSan.get} is +0.80." -> "engine_mention",
          s"${plan.routeSan.get} is the best move." -> "forbidden_wording",
          s"${plan.routeSan.get} is the only move." -> "forbidden_wording",
          s"${plan.routeSan.get} starts a forced line." -> "forbidden_wording"
        ).foreach: (output, violation) =>
          val checked = LlmNarrationSmoke.check(plan, rendered, output)
          assertEquals(checked.accepted, false, s"${fixture.label}: $output")
          assert(checked.violations.contains(violation), s"${fixture.label}: $output should include $violation, got ${checked.violations}")

      Vector(
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} engine says")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} principal variation")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} +0.80")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} best move")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} only move")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} forced line"))
      ).foreach: plan =>
        assertEquals(DeterministicRenderer.fromPlan(plan), None, s"${fixture.label}: ${plan.routeSan}")

  test("LDH-5 Negative Corpus keeps close line defender false positives silent"):
    def assertNoPublicOutput(story: Story, label: String): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus = EngineCheckStatus.Supports,
        before: Int = 20,
        after: Int = 20,
        freshness: Option[Int] = Some(0)
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = freshness,
        requestedStatus = status
      )

    val revealLine = Line(Square('d', 3), Square('f', 4))
    val captureDefenderLine = Line(Square('d', 3), Square('e', 5))

    val lineOpensNoAttackFacts = BoardFacts.fromFen("7k/8/7r/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val lineOpensNoAttack =
      LineProof.fromBoardFacts(lineOpensNoAttackFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('h', 6)))
    assertEquals(lineOpensNoAttack.complete, false)
    assertEquals(lineOpensNoAttack.afterSliderAttacksTarget, false)
    assertEquals(
      TacticDiscoveredAttack.write(lineOpensNoAttackFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('h', 6))),
      None
    )

    val kingTargetFacts = BoardFacts.fromFen("8/8/6k1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val kingTarget =
      LineProof.fromBoardFacts(kingTargetFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)))
    assertEquals(kingTarget.complete, false)
    assertEquals(kingTarget.afterSliderAttacksTarget, true)
    assertEquals(kingTarget.targetNonKingMaterial, false)
    assertEquals(TacticDiscoveredAttack.write(kingTargetFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6))), None)

    val pinLookingNoKingFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinLookingNoKing =
      PinProof.fromBoardFacts(
        pinLookingNoKingFacts,
        Some(revealLine),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 7))
      )
    assertEquals(pinLookingNoKing.complete, false)
    assertEquals(pinLookingNoKing.kingBehindTarget, None)
    assertEquals(
      TacticPin.write(
        pinLookingNoKingFacts,
        Some(revealLine),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 7))
      ),
      None
    )

    val stillGuardedFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val stillGuarded =
      RemoveGuardProof.fromBoardFacts(stillGuardedFacts, Some(revealLine), Some(Square('g', 6)), Some(Square('e', 5)))
    assertEquals(stillGuarded.complete, false)
    assertEquals(stillGuarded.defenderGuardedTargetBeforeMove, true)
    assertEquals(stillGuarded.afterMoveDefenderNoLongerGuardsTarget, false)
    assertEquals(TacticRemoveGuard.write(stillGuardedFacts, Some(revealLine), Some(Square('g', 6)), Some(Square('e', 5))), None)

    val defenderRemovedNoMaterialFacts = BoardFacts.fromFen("7k/4q3/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardOnly =
      TacticRemoveGuard.write(
        defenderRemovedNoMaterialFacts,
        Some(captureDefenderLine),
        Some(Square('g', 6)),
        Some(Square('e', 5))
      ).get
    val defenderCaptureResult = CaptureResult.fromBoardFacts(defenderRemovedNoMaterialFacts, captureDefenderLine)
    assertEquals(defenderCaptureResult.positiveMaterial, false)
    assertEquals(SceneMaterial.write(defenderRemovedNoMaterialFacts, captureDefenderLine), None)
    assertEquals(TacticHanging.write(defenderRemovedNoMaterialFacts, captureDefenderLine), None)
    val removeGuardOnlyVerdict = StoryTable.choose(Vector(removeGuardOnly)).head
    val removeGuardOnlyPlan = ExplanationPlan.fromSelected(removeGuardOnlyVerdict).get
    assertEquals(removeGuardOnlyPlan.allowedClaim, Some(ExplanationClaim.RemovesDefender))
    assertEquals(DeterministicRenderer.fromPlan(removeGuardOnlyPlan).exists(_.claimKey == "removes_defender"), true)

    val discoveredOnly =
      TacticDiscoveredAttack.write(pinLookingNoKingFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6))).get
    val discoveredOnlyVerdicts = StoryTable.choose(Vector(discoveredOnly))
    assertEquals(discoveredOnlyVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(discoveredOnlyVerdicts.head.story.tactic, Some(Tactic.DiscoveredAttack))
    assertEquals(ExplanationPlan.fromSelected(discoveredOnlyVerdicts.head).flatMap(_.allowedClaim), Some(ExplanationClaim.RevealsAttackOnPiece))

    val positiveDiscoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val positiveDiscovered =
      TacticDiscoveredAttack.write(positiveDiscoveredFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6))).get
    val wrongBoardCheck =
      engineCheck(BoardFacts.fromFen("8/7k/7r/8/8/3N4/8/1B5K w - - 0 1").toOption.get, positiveDiscovered, revealLine)
    val staleCheck = engineCheck(positiveDiscoveredFacts, positiveDiscovered, revealLine, freshness = Some(2))
    val routeMismatchCheck =
      engineCheck(positiveDiscoveredFacts, positiveDiscovered, captureDefenderLine)
    assertEquals(wrongBoardCheck.evidenceReady, false)
    assertEquals(wrongBoardCheck.status, EngineCheckStatus.Unknown)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(positiveDiscovered, wrongBoardCheck), None)
    assertEquals(staleCheck.evidenceReady, false)
    assertEquals(staleCheck.status, EngineCheckStatus.Unknown)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(positiveDiscovered, staleCheck), None)
    assertEquals(routeMismatchCheck.evidenceReady, false)
    assertEquals(routeMismatchCheck.status, EngineCheckStatus.Unknown)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(positiveDiscovered, routeMismatchCheck), None)

    val routeMismatchStory = positiveDiscovered.copy(route = Some(captureDefenderLine), routeSan = Some("Ne5"))
    assertNoPublicOutput(routeMismatchStory, "route mismatch")

    val refuteCheck =
      engineCheck(positiveDiscoveredFacts, positiveDiscovered, revealLine, EngineCheckStatus.Supports, before = 220, after = 20)
    val refuted = TacticDiscoveredAttack.withEngineCheck(positiveDiscovered, refuteCheck).get
    assertEquals(refuteCheck.status, EngineCheckStatus.Refutes)
    assertNoPublicOutput(refuted, "engine refutes plausible row")

    val positivePinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val positivePin =
      TacticPin.write(positivePinFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get
    val skewerLooking = positivePin.copy(tactic = Some(Tactic.Skewer))
    assertNoPublicOutput(skewerLooking, "Skewer-looking relation before Skewer opens")

  test("LDH-6 Downstream Boundary Smoke sends only selected Lead Verdicts to text stages"):
    final case class DownstreamFixture(
        label: String,
        facts: BoardFacts,
        story: Story,
        line: Line,
        attach: (Story, EngineCheck) => Option[Story],
        expectedClaim: ExplanationClaim
    )

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus,
        before: Int = 20,
        after: Int = 20
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def lowContextProof =
      proof(
        boardProof = 20,
        lineProof = 0,
        ownerProof = 0,
        anchorProof = 0,
        routeProof = 0,
        persistence = 0,
        immediacy = 0,
        forcing = 0,
        conversionPrize = 0,
        kingHeat = 0,
        pieceSupport = 0,
        pawnSupport = 0,
        sourceFit = 0,
        novelty = 0,
        clarity = 0
      )

    def assertNoStandaloneText(verdict: Verdict, renderedFromLead: RenderedLine, label: String): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)
      plan.foreach: relationPlan =>
        assertEquals(LlmNarrationSmoke.mockNarrate(relationPlan, Some(renderedFromLead)), None, label)
        assertEquals(LlmNarrationSmoke.codexCliPrompt(relationPlan, renderedFromLead), None, label)

    val revealLine = Line(Square('d', 3), Square('f', 4))
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6))).get

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      TacticPin.write(pinFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5))).get

    val fixtures = Vector(
      DownstreamFixture(
        "Tactic.DiscoveredAttack",
        discoveredFacts,
        discovered,
        revealLine,
        TacticDiscoveredAttack.withEngineCheck,
        ExplanationClaim.RevealsAttackOnPiece
      ),
      DownstreamFixture("Tactic.Pin", pinFacts, pin, revealLine, TacticPin.withEngineCheck, ExplanationClaim.PinsPiece),
      DownstreamFixture(
        "Tactic.RemoveGuard",
        removeGuardFacts,
        removeGuard,
        removeGuardLine,
        TacticRemoveGuard.withEngineCheck,
        ExplanationClaim.RemovesDefender
      )
    )

    val leadOutputs = fixtures.map: fixture =>
      val verdict = StoryTable.choose(Vector(fixture.story)).head
      val unselected = verdict.copy(selected = false)
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

      assertEquals(verdict.role, Role.Lead, fixture.label)
      assertEquals(plan.role, Role.Lead, fixture.label)
      assertEquals(plan.allowedClaim, Some(fixture.expectedClaim), fixture.label)
      assertEquals(ExplanationPlan.fromSelected(unselected), None, fixture.label)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text), fixture.label)
      assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text), NarrationSmokeCheck(true, Vector.empty), fixture.label)

      Vector(
        s"renderedText: ${rendered.text}",
        s"claimKey: ${rendered.claimKey}",
        s"strength: ${rendered.strength}",
        "forbiddenWording:",
        "instruction: Rephrase only. Do not add chess facts."
      ).foreach: allowedInput =>
        assert(prompt.contains(allowedInput), s"${fixture.label}: prompt must include $allowedInput")

      Vector(
        "ExplanationPlan",
        "Verdict",
        "Story",
        "StoryProof",
        "LineProof",
        "PinProof",
        "RemoveGuardProof",
        "EngineCheck",
        "EngineEval",
        "EngineLine",
        "BoardFacts",
        "BoardMood",
        "proofFailures",
        "source row",
        "role:",
        "scene:",
        "tactic:",
        "side:",
        "target:",
        "anchor:",
        "route:",
        "raw PV"
      ).foreach: forbiddenInput =>
        assert(!prompt.contains(forbiddenInput), s"${fixture.label}: prompt must not include $forbiddenInput")

      val capped =
        fixture.attach(fixture.story, engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Caps)).get
      val refuted = fixture
        .attach(fixture.story, engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Supports, 220, 20))
        .get
      assertNoStandaloneText(StoryTable.choose(Vector(capped)).head, rendered, s"${fixture.label}: capped")
      assertNoStandaloneText(StoryTable.choose(Vector(refuted)).head, rendered, s"${fixture.label}: refuted")

      rendered

    val sameLineVerdicts = StoryTable.choose(Vector(discovered, pin))
    val supportVerdict = sameLineVerdicts.find(_.role == Role.Support).get
    val contextStory = Story(
      Scene.Quiet,
      proof = lowContextProof,
      side = Side.White,
      target = Some(Square('g', 6)),
      anchor = Some(Square('b', 1)),
      route = Some(revealLine),
      routeSan = Some("Nf4"),
      rival = Side.Black,
      storyProof = StoryProof.fromBoardFacts(discoveredFacts, revealLine)
    )
    val contextVerdict = StoryTable.choose(Vector(discovered, contextStory)).find(_.story == contextStory).get
    val blockedVerdict = StoryTable.choose(Vector(discovered.copy(route = Some(removeGuardLine), routeSan = Some("Ne5")))).head

    assertEquals(sameLineVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(sameLineVerdicts.count(_.role == Role.Support), 1)
    assertEquals(contextVerdict.role, Role.Context)
    assertEquals(blockedVerdict.role, Role.Blocked)
    assertNoStandaloneText(supportVerdict, leadOutputs.head, "Support row")
    assertNoStandaloneText(contextVerdict, leadOutputs.head, "Context row")
    assertNoStandaloneText(blockedVerdict, leadOutputs.head, "Blocked row")

    val fromSelectedMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    val rendererFromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
    val smokeParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assertEquals(fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)), Vector(Vector("Verdict")))
    assertEquals(rendererFromPlanMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)), Vector(Vector("ExplanationPlan")))
    Vector("fromStory", "fromProof", "fromEngineCheck", "fromVerdict").foreach: forbiddenMethod =>
      assert(!rendererMethodNames.contains(forbiddenMethod), s"Renderer must not expose $forbiddenMethod")
    Vector("canPhraseXRay", "canPhraseLineTactic", "canPhrasePressure", "canPhraseInitiative").foreach:
      forbiddenTemplate =>
        assert(!rendererMethodNames.contains(forbiddenTemplate), s"Renderer must not add $forbiddenTemplate")
    Vector(
      "Story",
      "Proof",
      "StoryProof",
      "LineProof",
      "PinProof",
      "RemoveGuardProof",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "MultiTargetProof",
      "ThreatProof",
      "DefenseProof"
    ).foreach: forbiddenType =>
      assert(!rendererParameterNames.contains(forbiddenType), s"Renderer must not accept $forbiddenType")
      assert(!smokeParameterNames.contains(forbiddenType), s"LLM smoke must not accept $forbiddenType")

  test("LDH-7 Diagnostics Boundary keeps diagnostics out of public meaning"):
    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val revealLine = Line(Square('d', 3), Square('f', 4))
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val story =
      TacticDiscoveredAttack.write(facts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6))).get
    val supported =
      TacticDiscoveredAttack.withEngineCheck(story, engineCheck(facts, story, revealLine, EngineCheckStatus.Supports)).get
    val leadVerdict = StoryTable.choose(Vector(supported)).head
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get
    val diagnosticMarkers =
      Vector(
        "ldh7 proof failure marker",
        "ldh7 raw proof text marker",
        "ldh7 enginecheck payload marker",
        "ldh7 storytable debug relation marker"
      )
    val diagnosticVerdict = leadVerdict.copy(
      proofFailures = Vector(BoardFacts.MissingEvidence("LDH-7 diagnostic", diagnosticMarkers)),
      engineCheckStatus = Some(EngineCheckStatus.Refutes),
      engineStrengthLimited = true
    )
    val diagnosticPlan = leadPlan.copy(
      allowedClaim = None,
      relations = Vector(ExplanationRelation.CappedSameStory, ExplanationRelation.BlockedByEngineRefute),
      debugOnly = true
    )

    assert(diagnosticVerdict.proofFailures.nonEmpty)
    assertEquals(diagnosticVerdict.values, leadVerdict.values)
    assertEquals(leadVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(rendered.claimKey, "reveals_attack_on_piece")
    assertEquals(LlmNarrationSmoke.check(leadPlan, rendered, rendered.text), NarrationSmokeCheck(true, Vector.empty))

    diagnosticMarkers.foreach: marker =>
      assert(!diagnosticVerdict.values.mkString(" ").contains(marker), s"Verdict.values leaked diagnostic marker: $marker")
      assert(!leadPlan.toString.contains(marker), s"ExplanationPlan leaked diagnostic marker: $marker")
      assert(!rendered.text.contains(marker), s"Renderer leaked diagnostic marker: $marker")
      assert(!prompt.contains(marker), s"LLM smoke prompt leaked diagnostic marker: $marker")

    Vector("EngineCheck", "EngineEval", "EngineLine", "Supports", "Refutes", "Caps", "h8", "h7").foreach: engineDiagnostic =>
      assert(!leadPlan.toString.contains(engineDiagnostic), s"EngineCheck diagnostic reached ExplanationPlan: $engineDiagnostic")
      assert(!prompt.contains(engineDiagnostic), s"EngineCheck diagnostic reached LLM smoke prompt: $engineDiagnostic")

    diagnosticPlan.relations.foreach: relation =>
      assert(!rendered.text.contains(relation.key), s"StoryTable relation leaked to renderer wording: ${relation.key}")
      assert(!prompt.contains(relation.key), s"StoryTable relation leaked to LLM smoke prompt: ${relation.key}")
    assertEquals(DeterministicRenderer.fromPlan(diagnosticPlan), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(diagnosticPlan, Some(rendered)), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(diagnosticPlan, rendered), None)

    val explanationSurface =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet ++
        ExplanationPlan.getClass.getDeclaredMethods.map(_.getName).toSet
    val rendererSurface =
      DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet ++
        DeterministicRenderer.getClass.getDeclaredFields.map(_.getName).toSet
    val smokeSurface =
      LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet ++
        LlmNarrationSmoke.getClass.getDeclaredFields.map(_.getName).toSet
    Vector("proofFailures", "missingEvidence", "engineCheck", "sourceRow", "fromStory", "fromProof", "fromEngineCheck").foreach:
      forbiddenSurface =>
        assert(!explanationSurface.exists(_.toLowerCase.contains(forbiddenSurface.toLowerCase)))
        assert(!rendererSurface.exists(_.toLowerCase.contains(forbiddenSurface.toLowerCase)))
        assert(!smokeSurface.exists(_.toLowerCase.contains(forbiddenSurface.toLowerCase)))

    val runtimeAuthorityNames =
      StoryWriter.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.key).toVector ++
        ExplanationRelation.values.map(_.key).toVector ++
        Tactic.values.map(_.toString).toVector
    Vector("ldh", "diagnostics_boundary", "diagnostic_boundary", "fixture", "helper", "test_helper").foreach: forbidden =>
      assert(!runtimeAuthorityNames.exists(_.toLowerCase.contains(forbidden)), s"test helper became runtime authority: $forbidden")

  test("LDH Closeout Hard Cleanup keeps Line Defender authority separated and public surfaces closed"):
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get

    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get

    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard.write(
        removeGuardFacts,
        Some(removeGuardMove),
        Some(Square('e', 5)),
        Some(Square('c', 4))
      ).get

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(materialFacts, materialMove).get
    val material = SceneMaterial.write(materialFacts, materialMove).get

    val defenseFacts =
      BoardFacts.fromFen("4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1").toOption.get
    val defense = SceneDefense
      .write(defenseFacts, Line(Square('f', 5), Square('d', 4)), Line(Square('e', 4), Square('f', 5)))
      .get

    def selectedPlan(story: Story): ExplanationPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    val discoveredPlan = selectedPlan(discovered)
    val pinPlan = selectedPlan(pin)
    val removeGuardPlan = selectedPlan(removeGuard)
    val discoveredRendered = DeterministicRenderer.fromPlan(discoveredPlan).get
    val pinRendered = DeterministicRenderer.fromPlan(pinPlan).get
    val removeGuardRendered = DeterministicRenderer.fromPlan(removeGuardPlan).get
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val rendererInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector
    val storyFieldNames = classOf[Story].getDeclaredFields.map(_.getName).toSet
    val lineFactSurface =
      classOf[BoardFacts.LineFact].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.LineFact].getDeclaredFields.map(_.getName).toSet
    val lineProofSurface =
      classOf[LineProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    val pinProofSurface =
      classOf[PinProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[PinProof].getDeclaredFields.map(_.getName).toSet
    val removeGuardProofSurface =
      classOf[RemoveGuardProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RemoveGuardProof].getDeclaredFields.map(_.getName).toSet

    assertEquals(
      StoryWriter.values.toVector,
      Vector(
        StoryWriter.TacticHanging,
        StoryWriter.TacticFork,
        StoryWriter.SceneMaterial,
        StoryWriter.SceneDefense,
        StoryWriter.TacticDiscoveredAttack,
        StoryWriter.TacticPin,
        StoryWriter.TacticRemoveGuard,
        StoryWriter.TacticSkewer,
        StoryWriter.ScenePawnAdvance,
        StoryWriter.ScenePawnStop,
        StoryWriter.ScenePromotionThreat
      )
    )
    Vector(
      "xrayProof",
      "xRayProof",
      "deflectionProof",
      "overloadProof",
      "pressureProof",
      "initiativeProof",
      "lineTacticProof",
      "rayProof",
      "kingSafetyProof",
      "mateThreatProof"
    ).foreach: forbiddenProofHome =>
      assert(!storyFieldNames.exists(_.equalsIgnoreCase(forbiddenProofHome)), s"new proof home opened: $forbiddenProofHome")

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(!lineFactSurface.exists(_.toLowerCase.contains(forbidden)), s"LineFact became authority surface: $forbidden")
    Vector("PinProof", "RemoveGuardProof", "Hanging", "Defense", "Skewer", "XRay").foreach: forbidden =>
      assert(!lineProofSurface.exists(_.contains(forbidden)), s"LineProof mixed authority: $forbidden")
    Vector("winsMaterial", "materialGain", "materialBalance", "SceneMaterial").foreach: forbidden =>
      assert(!lineProofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), s"LineProof owns material claim: $forbidden")
    Vector("LineProof", "RemoveGuardProof", "Material", "Hanging", "Defense", "Skewer", "XRay").foreach: forbidden =>
      assert(!pinProofSurface.exists(_.contains(forbidden)), s"PinProof mixed authority: $forbidden")
    Vector("LineProof", "PinProof", "Hanging", "Defense", "Skewer", "XRay").foreach: forbidden =>
      assert(!removeGuardProofSurface.exists(_.contains(forbidden)), s"RemoveGuardProof mixed authority: $forbidden")
    Vector("winsMaterial", "materialGain", "materialBalance", "SceneMaterial").foreach: forbidden =>
      assert(
        !removeGuardProofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)),
        s"RemoveGuardProof owns material claim: $forbidden"
      )

    assert(discovered.lineProof.exists(_.complete))
    assertEquals(discovered.lineProof.exists(_.publicClaimAllowed), false)
    assertEquals(discovered.pinProof, None)
    assertEquals(discovered.removeGuardProof, None)
    assertEquals(discovered.captureResult, None)
    assertEquals(discovered.threatProof, None)
    assertEquals(discovered.defenseProof, None)
    assertEquals(discovered.writer, Some(StoryWriter.TacticDiscoveredAttack))
    assertEquals(discovered.tactic, Some(Tactic.DiscoveredAttack))

    assert(pin.pinProof.exists(_.complete))
    assertEquals(pin.pinProof.exists(_.publicClaimAllowed), false)
    assertEquals(pin.lineProof, None)
    assertEquals(pin.removeGuardProof, None)
    assertEquals(pin.captureResult, None)
    assertEquals(pin.threatProof, None)
    assertEquals(pin.defenseProof, None)
    assertEquals(pin.writer, Some(StoryWriter.TacticPin))
    assertEquals(pin.tactic, Some(Tactic.Pin))

    assert(removeGuard.removeGuardProof.exists(_.complete))
    assertEquals(removeGuard.removeGuardProof.exists(_.publicClaimAllowed), false)
    assertEquals(removeGuard.lineProof, None)
    assertEquals(removeGuard.pinProof, None)
    assertEquals(removeGuard.captureResult, None)
    assertEquals(removeGuard.threatProof, None)
    assertEquals(removeGuard.defenseProof, None)
    assertEquals(removeGuard.writer, Some(StoryWriter.TacticRemoveGuard))
    assertEquals(removeGuard.tactic, Some(Tactic.RemoveGuard))

    assert(hanging.captureResult.exists(_.positiveMaterial))
    assertEquals(hanging.lineProof, None)
    assertEquals(hanging.pinProof, None)
    assertEquals(hanging.removeGuardProof, None)
    assert(material.captureResult.exists(_.positiveMaterial))
    assertEquals(material.lineProof, None)
    assertEquals(material.pinProof, None)
    assertEquals(material.removeGuardProof, None)
    assert(defense.threatProof.exists(_.complete))
    assert(defense.defenseProof.exists(_.complete))
    assertEquals(defense.lineProof, None)
    assertEquals(defense.pinProof, None)
    assertEquals(defense.removeGuardProof, None)

    val lineDefenderClaims =
      Map(
        "DiscoveredAttack" -> ExplanationClaim.DiscoveredAttackAllowed.map(_.key).toSet,
        "Pin" -> ExplanationClaim.PinAllowed.map(_.key).toSet,
        "RemoveGuard" -> ExplanationClaim.RemoveGuardAllowed.map(_.key).toSet
      )
    val openedClaimHomes =
      Map(
        "Hanging" -> ExplanationClaim.HangingAllowed.map(_.key).toSet,
        "Material" -> ExplanationClaim.MaterialAllowed.map(_.key).toSet,
        "Defense" -> ExplanationClaim.DefenseAllowed.map(_.key).toSet
      )
    assertEquals(lineDefenderClaims("DiscoveredAttack"), Set("reveals_attack_on_piece"))
    assertEquals(lineDefenderClaims("Pin"), Set("pins_piece"))
    assertEquals(lineDefenderClaims("RemoveGuard"), Set("removes_defender"))
    lineDefenderClaims.toVector.combinations(2).foreach: pair =>
      val first = pair(0)
      val second = pair(1)
      assertEquals(first._2.intersect(second._2), Set.empty[String], s"${first._1} invaded ${second._1}")
    lineDefenderClaims.foreach: (owner, keys) =>
      openedClaimHomes.foreach: (claimHome, homeKeys) =>
        assertEquals(keys.intersect(homeKeys), Set.empty[String], s"$owner invaded $claimHome claim home")

    assertEquals(discoveredPlan.allowedClaim.map(_.key), Some("reveals_attack_on_piece"))
    assertEquals(pinPlan.allowedClaim.map(_.key), Some("pins_piece"))
    assertEquals(removeGuardPlan.allowedClaim.map(_.key), Some("removes_defender"))
    assertEquals(discoveredRendered.text, "Nf4 reveals an attack on the piece on g6.")
    assertEquals(pinRendered.text, "Re8 pins the piece on e2.")
    assertEquals(removeGuardRendered.text, "Bxc4 removes the defender of the piece on e5.")
    assertEquals(rendererInputs, Vector("ExplanationPlan"))
    Vector("canPhraseXRay", "canPhraseLineTactic", "canPhraseDeflection", "canPhraseOverload", "canPhrasePressure", "canPhraseInitiative").foreach:
      forbiddenRenderer =>
        assert(!rendererMethodNames.contains(forbiddenRenderer), s"broad renderer authority opened: $forbiddenRenderer")

    val liveLineDefenderAuthorityNames =
      StoryWriter.values
        .filter(writer =>
          writer == StoryWriter.TacticDiscoveredAttack ||
            writer == StoryWriter.TacticPin ||
            writer == StoryWriter.TacticRemoveGuard
        )
        .map(_.toString.toLowerCase)
        .toVector ++ lineDefenderClaims.values.flatten.toVector
    Vector("line_tactic", "ray", "xray", "x_ray", "skewer", "deflect", "deflection", "overload", "pressure", "initiative").foreach:
      forbidden =>
        assert(!liveLineDefenderAuthorityNames.exists(_.contains(forbidden)), s"broad term became LDH authority: $forbidden")

    Vector(
      discoveredPlan -> discoveredRendered,
      pinPlan -> pinRendered,
      removeGuardPlan -> removeGuardRendered
    ).foreach: (plan, rendered) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
      assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
      assert(LlmNarrationSmoke.codexCliPrompt(plan, rendered).exists(_.contains("Rephrase only. Do not add chess facts.")))

    Vector(
      discovered -> Vector(Tactic.Pin, Tactic.RemoveGuard, Tactic.Hanging, Tactic.Skewer, Tactic.Xray, Tactic.Deflect, Tactic.Overload),
      pin -> Vector(Tactic.DiscoveredAttack, Tactic.RemoveGuard, Tactic.Hanging, Tactic.Skewer, Tactic.Xray, Tactic.Deflect, Tactic.Overload),
      removeGuard -> Vector(Tactic.DiscoveredAttack, Tactic.Pin, Tactic.Hanging, Tactic.Skewer, Tactic.Xray, Tactic.Deflect, Tactic.Overload)
    ).foreach: (story, tactics) =>
      tactics.foreach: tactic =>
        val forged = story.copy(tactic = Some(tactic))
        val verdict = StoryTable.choose(Vector(forged)).head
        assertEquals(verdict.role, Role.Blocked, s"${story.writer} accepted forged $tactic")
        ExplanationPlan.fromSelected(verdict).foreach: forgedPlan =>
          assertEquals(forgedPlan.allowedClaim, None, s"${story.writer} claimed forged $tactic")
          assertEquals(forgedPlan.debugOnly, true, s"${story.writer} made forged $tactic public")
          assertEquals(DeterministicRenderer.fromPlan(forgedPlan), None, s"${story.writer} rendered forged $tactic")

    Vector(
      "Nf4 wins material on g6.",
      "Nf4 is a skewer on g6.",
      "Nf4 creates pressure on g6.",
      "Re8 means the piece on e2 cannot move.",
      "Re8 creates a mate threat.",
      "Bxc4 is a deflection tactic.",
      "Bxc4 overloads the defender.",
      "Bxc4 takes the initiative."
    ).foreach: output =>
      val relevant =
        if output.startsWith("Nf4") then discoveredPlan -> discoveredRendered
        else if output.startsWith("Re8") then pinPlan -> pinRendered
        else removeGuardPlan -> removeGuardRendered
      assertEquals(LlmNarrationSmoke.check(relevant._1, relevant._2, output).accepted, false, output)

    val runtimeAuthorityNames =
      StoryWriter.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.key).toVector ++
        ExplanationRelation.values.map(_.key).toVector
    Vector("ldh_closeout", "public_route", "production_api", "public_llm", "user_facing_llm").foreach: forbidden =>
      assert(!runtimeAuthorityNames.exists(_.toLowerCase.contains(forbidden)), s"closed surface became runtime authority: $forbidden")

  test("LNC-5 Downstream Boundary Audit keeps Line Defender speech bounded"):
    final case class DownstreamFixture(
        label: String,
        facts: BoardFacts,
        story: Story,
        line: Line,
        attach: (Story, EngineCheck) => Option[Story],
        expectedClaim: ExplanationClaim
    )

    def highProof: Proof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6))).get
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinLine = Line(Square('a', 8), Square('e', 8))
    val pin = TacticPin.write(pinFacts, Some(pinLine), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
    val removeFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeLine = Line(Square('g', 8), Square('c', 4))
    val remove = TacticRemoveGuard.write(removeFacts, Some(removeLine), Some(Square('e', 5)), Some(Square('c', 4))).get
    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerLine = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer
        .write(skewerFacts, Some(skewerLine), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
        .copy(
          proof = proof(
            boardProof = 99,
            lineProof = 99,
            ownerProof = 99,
            anchorProof = 99,
            routeProof = 99,
            pieceSupport = 99,
            clarity = 99
          )
        )
        .copy(proof = highProof)

    val requiredForbiddenKeys =
      Vector(
        "wins_material",
        "winning",
        "decisive",
        "best_move",
        "only_move",
        "forced",
        "cannot_move",
        "no_defense",
        "front_piece_must_move",
        "wins_rear_piece",
        "creates_pressure",
        "takes_initiative",
        "mate_threat",
        "king_unsafe"
      )
    val forbiddenOutputs =
      Vector(
        "wins material" -> ((san: String) => s"$san wins material."),
        "winning" -> ((san: String) => s"$san is winning."),
        "decisive" -> ((san: String) => s"$san is decisive."),
        "best move" -> ((san: String) => s"$san is the best move."),
        "only move" -> ((san: String) => s"$san is the only move."),
        "forced" -> ((san: String) => s"$san starts a forced line."),
        "cannot move" -> ((san: String) => s"$san means the piece cannot move."),
        "no defense" -> ((san: String) => s"$san leaves no defense."),
        "front piece must move" -> ((san: String) => s"$san means the front piece must move."),
        "wins rear piece" -> ((san: String) => s"$san wins the rear piece."),
        "pressure" -> ((san: String) => s"$san creates pressure."),
        "initiative" -> ((san: String) => s"$san takes the initiative."),
        "mate threat" -> ((san: String) => s"$san creates a mate threat."),
        "king unsafe" -> ((san: String) => s"$san makes the king unsafe.")
      )

    val fixtures = Vector(
      DownstreamFixture("Tactic.DiscoveredAttack", discoveredFacts, discovered, discoveredLine, TacticDiscoveredAttack.withEngineCheck, ExplanationClaim.RevealsAttackOnPiece),
      DownstreamFixture("Tactic.Pin", pinFacts, pin, pinLine, TacticPin.withEngineCheck, ExplanationClaim.PinsPiece),
      DownstreamFixture("Tactic.RemoveGuard", removeFacts, remove, removeLine, TacticRemoveGuard.withEngineCheck, ExplanationClaim.RemovesDefender),
      DownstreamFixture("Tactic.Skewer", skewerFacts, skewer, skewerLine, TacticSkewer.withEngineCheck, ExplanationClaim.SkewersPieceToPiece)
    )

    fixtures.foreach: fixture =>
      val leadVerdict = StoryTable.choose(Vector(fixture.story)).head
      assertEquals(leadVerdict.role, Role.Lead, fixture.label)
      val plan = ExplanationPlan.fromSelected(leadVerdict).get
      assertEquals(plan.allowedClaim, Some(fixture.expectedClaim), fixture.label)
      val forbiddenKeys = plan.forbiddenWording.map(_.key)
      requiredForbiddenKeys.foreach: key =>
        assert(forbiddenKeys.contains(key), s"${fixture.label} missing forbidden wording key: $key")
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
      Vector(
        s"renderedText: ${rendered.text}",
        s"claimKey: ${rendered.claimKey}",
        s"strength: ${rendered.strength}",
        "forbiddenWording:",
        "instruction: Rephrase only. Do not add chess facts."
      ).foreach: expectedInput =>
        assert(prompt.contains(expectedInput), s"${fixture.label} prompt missing $expectedInput")
      forbiddenOutputs.foreach: (label, outputFor) =>
        val result = LlmNarrationSmoke.check(plan, rendered, outputFor(plan.routeSan.get))
        assertEquals(result.accepted, false, s"${fixture.label}: $label")
        assert(result.violations.contains("forbidden_wording"), s"${fixture.label}: $label -> $result")

      val supportVerdict = leadVerdict.copy(role = Role.Support, leadAllowed = false)
      val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false)
      val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false)
      val unselectedVerdict = leadVerdict.copy(selected = false)
      val cappedVerdict =
        StoryTable.choose(Vector(fixture.attach(fixture.story, engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Caps)).get)).head
      val refutedVerdict =
        StoryTable
          .choose(Vector(fixture.attach(fixture.story, engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Supports, before = 220, after = 20)).get))
          .head
      Vector(supportVerdict, contextVerdict, blockedVerdict, unselectedVerdict, cappedVerdict, refutedVerdict).foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict), None, s"${fixture.label}: $verdict")

  test("LNC-7 Test Helper Runtime Boundary Audit keeps helpers out of runtime authority"):
    val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val runtimeSourceStream = Files.walk(runtimeRoot)
    val runtimeText =
      try
        runtimeSourceStream
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
        .map(Files.readString)
        .mkString("\n")
      finally runtimeSourceStream.close()

    Vector(
      "LNC-",
      "LDH-",
      "Closeout",
      "closeout",
      "fixture map",
      "negative corpus",
      "Line Defender Neighborhood",
      "Contact Neighborhood"
    ).foreach: closeoutOnlyTerm =>
      assert(!runtimeText.contains(closeoutOnlyTerm), s"runtime source must not contain closeout-only term: $closeoutOnlyTerm")

    val runtimeAuthorityNames =
      StoryWriter.values.map(_.toString).toVector ++
        Scene.values.map(_.toString).toVector ++
        Tactic.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.key).toVector ++
        ExplanationRelation.values.map(_.key).toVector
    Vector("Fixture", "Corpus", "Closeout", "Neighborhood", "Audit").foreach: helperName =>
      assert(!runtimeAuthorityNames.exists(_.contains(helperName)), s"test helper name became runtime authority: $helperName")

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerLine = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer
        .write(skewerFacts, Some(skewerLine), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
        .copy(
          proof = proof(
            boardProof = 99,
            lineProof = 99,
            ownerProof = 99,
            anchorProof = 99,
            routeProof = 99,
            pieceSupport = 99,
            clarity = 99
          )
        )
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(skewer)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    Vector("front_piece_must_move", "wins_rear_piece", "mate_threat", "king_unsafe", "creates_pressure").foreach: internalKey =>
      val result = LlmNarrationSmoke.check(plan, rendered, s"${rendered.text} $internalKey")
      assert(!result.violations.contains("forbidden_wording"), s"internal key must not be treated as public prose: $internalKey -> $result")

  test("PawnAdvance-0 opens only bounded passed pawn advance"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))

    val story = ScenePawnAdvance.write(facts, advance).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.scene, Scene.PawnAdvance)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 6)))
    assertEquals(story.anchor, Some(Square('e', 5)))
    assertEquals(story.route, Some(advance))
    assertEquals(story.routeSan, Some("e6"))
    assertEquals(story.writer, Some(StoryWriter.ScenePawnAdvance))
    assert(story.pawnAdvanceProof.exists(_.complete))
    assertEquals(story.pawnAdvanceProof.exists(_.publicClaimAllowed), false)
    assertEquals(story.pawnAdvanceProof.exists(_.alreadyPassedBefore), true)
    assertEquals(story.pawnAdvanceProof.exists(_.afterBoardPassedPawn), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("advances_passed_pawn"))
    assertEquals(rendered.text, "e6 advances the passed pawn.")
    assertEquals(rendered.claimKey, "advances_passed_pawn")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      "e6 threatens promotion.",
      "e6 is a winning passed-pawn strategy.",
      "e6 is the best move.",
      "e6 is forced.",
      "e6 starts conversion.",
      "e6 creates a pawn race.",
      "e6 is unstoppable.",
      "e6 wins.",
      "e6 queens.",
      "e6 promotes next.",
      "e6 has a clear path.",
      "e6 cannot be stopped."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnAdvance-1 PawnAdvanceProof binds exact legal passed-pawn advance without public claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val proof = PawnAdvanceProof.fromBoardFacts(facts, advance)

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.pawnAfter, Some(Piece(Side.White, Man.Pawn, Square('e', 6))))
    assertEquals(proof.fromSquare, Some(Square('e', 5)))
    assertEquals(proof.toSquare, Some(Square('e', 6)))
    assertEquals(proof.advanceMove, Some(advance))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnAdvance, true)
    assertEquals(proof.nonCapture, true)
    assertEquals(proof.nonPromotion, true)
    assertEquals(proof.legalOneStepNonCaptureNonPromotion, true)
    assertEquals(proof.alreadyPassedBefore, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.afterBoardPassedPawn, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.missingEvidence, Vector.empty)
    assert(facts.seen.passedPawnObservations.exists(row => row.side == Side.White && row.pawn == proof.pawnBefore.get))

  test("PawnAdvance-1 PawnAdvanceProof stays diagnostic for forged or converting moves"):
    val fakeAdvance = Line(Square('e', 5), Square('e', 6))
    val sameBoardProofMissing =
      minimalBoardFacts(
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5))
        ),
        sideLegal = Moves(
          known = true,
          lines = Vector(fakeAdvance),
          san = Vector("e6"),
          moveCount = 1
        )
      )
    val fakeProof = PawnAdvanceProof.fromBoardFacts(sameBoardProofMissing, fakeAdvance)

    assertEquals(fakeProof.sameBoardProof, false)
    assertEquals(fakeProof.exactAfterBoardReplay, false)
    assertEquals(fakeProof.afterBoardPassedPawn, false)
    assertEquals(fakeProof.publicClaimAllowed, false)
    assert(fakeProof.missingEvidence.exists(_.missing.contains("same-board proof")))
    assertEquals(ScenePawnAdvance.write(sameBoardProofMissing, fakeAdvance), None)

    val captureFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureProof = PawnAdvanceProof.fromBoardFacts(captureFacts, Line(Square('e', 5), Square('d', 6)))
    assertEquals(captureProof.nonCapture, false)
    assertEquals(captureProof.legalPawnAdvance, false)
    assertEquals(captureProof.exactAfterBoardReplay, false)
    assertEquals(captureProof.publicClaimAllowed, false)

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionProof = PawnAdvanceProof.fromBoardFacts(promotionFacts, Line(Square('e', 7), Square('e', 8)))
    assertEquals(promotionProof.nonPromotion, false)
    assertEquals(promotionProof.exactAfterBoardReplay, false)
    assertEquals(promotionProof.publicClaimAllowed, false)

  test("PawnAdvance-2 ScenePawnAdvance writer pins bounded Story identity"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val proof = story.pawnAdvanceProof.get

    assertEquals(story.writer, Some(StoryWriter.ScenePawnAdvance))
    assertEquals(story.scene, Scene.PawnAdvance)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.toSquare)
    assertEquals(story.anchor, proof.fromSquare)
    assertEquals(story.route, proof.advanceMove)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalOneStepNonCaptureNonPromotion, true)
    assertEquals(proof.alreadyPassedBefore, true)
    assertEquals(proof.afterBoardPassedPawn, true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.pieceSupport, 0)

    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("advances_passed_pawn"))

  test("PawnAdvance-2 ScenePawnAdvance writer rejects refuted or stronger forged rows"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val refutingCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(advance))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(50)),
        evalAfter = Some(EngineEval(-200)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Refutes
      )
    val refuted = story.copy(engineCheck = Some(refutingCheck))
    val forgedPromotionThreat = story.copy(tactic = Some(Tactic.PawnFork))
    val forgedWinningConversion = story.copy(scene = Scene.Convert, proof = story.proof.copy(conversionPrize = 100))
    val forgedPlan = story.copy(plan = Some(Plan.Convert))

    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    assertEquals(StoryTable.choose(Vector(refuted)).head.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(refuted)).head), None)
    Vector(forgedPromotionThreat, forgedWinningConversion, forgedPlan).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked)
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("PawnAdvance-0 rejects near passed pawn advance false positives"):
    val notPassedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val doubleAdvanceFacts = BoardFacts.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1").toOption.get
    val sameBoardProofMissing =
      minimalBoardFacts(
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5))
        ),
        sideLegal = Moves(
          known = true,
          lines = Vector(Line(Square('e', 5), Square('e', 6))),
          san = Vector("e6"),
          moveCount = 1
        )
      )

    assertEquals(ScenePawnAdvance.write(notPassedFacts, Line(Square('e', 5), Square('e', 6))), None, "not already passed")
    assertEquals(ScenePawnAdvance.write(captureFacts, Line(Square('e', 5), Square('d', 6))), None, "capture")
    assertEquals(ScenePawnAdvance.write(promotionFacts, Line(Square('e', 7), Square('e', 8))), None, "promotion")
    assertEquals(ScenePawnAdvance.write(doubleAdvanceFacts, Line(Square('e', 2), Square('e', 4))), None, "double advance")
    assertEquals(ScenePawnAdvance.write(sameBoardProofMissing, Line(Square('e', 5), Square('e', 6))), None, "same-board proof")

  test("PawnAdvance-3 negative corpus requires complete PawnAdvanceProof or silence"):
    val legalAdvanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val legalAdvance = Line(Square('e', 5), Square('e', 6))
    val illegalLine = Line(Square('e', 5), Square('e', 7))
    val rookMoveFacts = BoardFacts.fromFen("k7/8/8/4R3/8/8/8/7K w - - 0 1").toOption.get
    val rookMove = Line(Square('e', 5), Square('e', 6))
    val enPassantFacts = BoardFacts.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").toOption.get
    val enPassantCapture = Line(Square('e', 5), Square('d', 6))

    val illegalProof = PawnAdvanceProof.fromBoardFacts(legalAdvanceFacts, illegalLine)
    assertEquals(illegalProof.legalPawnAdvance, false)
    assertEquals(illegalProof.complete, false)
    assertEquals(ScenePawnAdvance.write(legalAdvanceFacts, illegalLine), None, "not a legal move")

    val rookProof = PawnAdvanceProof.fromBoardFacts(rookMoveFacts, rookMove)
    assertEquals(rookProof.pawnBefore, None)
    assertEquals(rookProof.complete, false)
    assertEquals(ScenePawnAdvance.write(rookMoveFacts, rookMove), None, "not a pawn")

    val enPassantProof = PawnAdvanceProof.fromBoardFacts(enPassantFacts, enPassantCapture)
    assertEquals(enPassantProof.nonCapture, false)
    assertEquals(enPassantProof.legalOneStepNonCaptureNonPromotion, false)
    assertEquals(ScenePawnAdvance.write(enPassantFacts, enPassantCapture), None, "en passant is capture complexity")

    val story = ScenePawnAdvance.write(legalAdvanceFacts, legalAdvance).get
    val afterNotPassed =
      story.copy(pawnAdvanceProof =
        story.pawnAdvanceProof.map(_.copy(afterBoardPassedPawn = false, missingEvidence = Vector.empty))
      )
    val promotionThreatForgery = story.copy(tactic = Some(Tactic.PawnPush))
    Vector(afterNotPassed, promotionThreatForgery).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked)
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("PawnAdvance-3 closes immediate promotion and stronger wording expansion"):
    val nearPromotionFacts = BoardFacts.fromFen("4k3/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 6), Square('e', 7))
    val story = ScenePawnAdvance.write(nearPromotionFacts, advance).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(plan.allowedClaim.map(_.key), Some("advances_passed_pawn"))
    Vector(
      "e7 promotes next.",
      "e7 is unstoppable.",
      "e7 is winning.",
      "e7 starts conversion.",
      "e7 creates a promotion threat."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnAdvance-4 reuses EngineCheck without creating engine-owned claims"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val reply = Line(Square('e', 8), Square('e', 7))

    def check(status: EngineCheckStatus, boundStory: Option[Story] = Some(story)): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = boundStory,
        engineLine = Some(EngineLine(Vector(advance))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1240)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def selected(attached: Story): Verdict =
      StoryTable.choose(Vector(attached)).head

    def noStandaloneEngineText(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("engine says")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("best move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("only move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("winning endgame")), false, label)
      assertEquals(rendered.exists(_.text.contains("+3.2")), false, label)
      assertEquals(rendered.exists(_.text.contains("40")), false, label)
      assertEquals(rendered.exists(_.text.contains("45")), false, label)

    val supports = ScenePawnAdvance.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePawnAdvance.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePawnAdvance.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = ScenePawnAdvance.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val unbound = check(EngineCheckStatus.Supports, boundStory = None)
    val wrongRoute =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(Line(Square('e', 5), Square('e', 7))))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1240)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    val supportsVerdict = selected(supports)
    val capsVerdict = selected(caps)
    val unknownVerdict = selected(unknown)
    val refutesVerdict = selected(refutes)
    val capsPlan = ExplanationPlan.fromSelected(capsVerdict)
    val refutesPlan = ExplanationPlan.fromSelected(refutesVerdict)

    assertEquals(
      ScenePawnAdvance.withEngineCheck(story, unbound),
      Option.empty[Story],
      "EngineCheck cannot attach without Story binding"
    )
    assertEquals(
      ScenePawnAdvance.withEngineCheck(story, wrongRoute),
      Option.empty[Story],
      "EngineCheck cannot attach a different route"
    )
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty, "EngineCheck alone cannot create PawnAdvance")

    assertEquals(supports.engineCheck.map(_.status), Option(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Option(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Option(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Option(EngineCheckStatus.Refutes))

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.leadAllowed, true)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.leadAllowed, true)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(capsPlan, Option.empty[ExplanationPlan])
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesPlan, Option.empty[ExplanationPlan])

    Vector(
      "supports" -> supportsVerdict,
      "caps" -> capsVerdict,
      "unknown" -> unknownVerdict,
      "refutes" -> refutesVerdict
    ).foreach: (label, verdict) =>
      noStandaloneEngineText(label, verdict)

    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
    Vector(
      "engine says e6 is best",
      "+3.2 and winning endgame",
      "only move for a tablebase-like win",
      "best move",
      "only move",
      "winning endgame"
    ).foreach: phrase =>
      assertEquals(LlmNarrationSmoke.mockNarrate(supportsPlan, supportsRendered.copy(text = phrase)), None, phrase)

  test("PawnAdvance-5 StoryTable keeps existing tactical material and defense homes ahead of PawnAdvance"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim),
          verdict.engineStrengthLimited
        )

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    val pawnFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = ScenePawnAdvance.write(pawnFacts, pawnMove).get.copy(proof = strongProof(100))

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialLine = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(materialFacts, materialLine).get.copy(proof = strongProof(90))
    val material = SceneMaterial.write(materialFacts, materialLine).get.copy(proof = strongProof(90))

    val forkFacts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get.copy(proof = strongProof(90))

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, threatLine, defenseLine).get.copy(proof = strongProof(90))

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(90))

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      TacticPin
        .write(pinFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
        .get
        .copy(proof = strongProof(90))

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5)))
        .get
        .copy(proof = strongProof(90))

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerLine = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer
        .write(skewerFacts, Some(skewerLine), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
        .copy(proof = strongProof(90))

    val existingRows =
      Vector(
        "Tactic.Hanging" -> hanging,
        "Tactic.Fork" -> fork,
        "Scene.Material" -> material,
        "Scene.Defense" -> defense,
        "Tactic.DiscoveredAttack" -> discovered,
        "Tactic.Pin" -> pin,
        "Tactic.RemoveGuard" -> removeGuard,
        "Tactic.Skewer" -> skewer
      )
    val mixedRows = existingRows :+ ("Scene.PawnAdvance" -> pawnAdvance)
    val forward = StoryTable.choose(mixedRows.map(_._2))
    val reverse = StoryTable.choose(mixedRows.reverse.map(_._2))
    val shuffled =
      StoryTable.choose(
        Vector(
          mixedRows(8),
          mixedRows(2),
          mixedRows(5),
          mixedRows(0),
          mixedRows(7),
          mixedRows(3),
          mixedRows(1),
          mixedRows(6),
          mixedRows(4)
        ).map(_._2)
      )

    assertEquals(shape(mixedRows, forward), shape(mixedRows, reverse))
    assertEquals(shape(mixedRows, forward), shape(mixedRows, shuffled))
    assert(rowId(mixedRows, forward.head.story) != "Scene.PawnAdvance")
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assert(!forward.exists(verdict => verdict.story == pawnAdvance && verdict.role == Role.Lead))

    existingRows.foreach: (label, row) =>
      val verdicts = StoryTable.choose(Vector(pawnAdvance, row))
      val existingVerdict = verdicts.find(_.story == row).get
      val pawnVerdict = verdicts.find(_.story == pawnAdvance).get

      assertEquals(existingVerdict.role, Role.Lead, label)
      assertEquals(existingVerdict.leadAllowed, true, label)
      assertEquals(pawnVerdict.role, Role.Support, label)
      assertEquals(pawnVerdict.leadAllowed, false, label)
      val existingPlan = ExplanationPlan.fromSelected(existingVerdict).get
      val existingRendered = DeterministicRenderer.fromPlan(existingPlan).get
      assertEquals(ExplanationPlan.fromSelected(pawnVerdict), None, label)
      assertEquals(existingPlan.allowedClaim.exists(_ == ExplanationClaim.AdvancesPassedPawn), false, label)
      assert(!existingRendered.text.toLowerCase.contains("passed pawn"), label)

    val materialCollision = StoryTable.choose(Vector(pawnAdvance, material))
    val materialLead = materialCollision.find(_.story == material).get
    val pawnWithMaterial = materialCollision.find(_.story == pawnAdvance).get
    assertEquals(materialLead.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(materialLead).flatMap(_.allowedClaim), Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(pawnWithMaterial.role, Role.Support)

    val immediateTacticRows = Vector(hanging, fork, discovered, pin, removeGuard, skewer)
    immediateTacticRows.foreach: tacticRow =>
      val verdicts = StoryTable.choose(Vector(pawnAdvance, tacticRow))
      val tacticVerdict = verdicts.find(_.story == tacticRow).get
      val pawnVerdict = verdicts.find(_.story == pawnAdvance).get
      assertEquals(tacticVerdict.role, Role.Lead, tacticRow.toString)
      assertEquals(tacticVerdict.story.scene, Scene.Tactic, tacticRow.toString)
      assertEquals(pawnVerdict.role, Role.Support, tacticRow.toString)
      assertNoStandaloneText("PawnAdvance support under immediate tactic", pawnVerdict)

    def pawnCheck(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = pawnFacts,
        story = Some(pawnAdvance),
        engineLine = Some(EngineLine(Vector(pawnMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val cappedPawn = ScenePawnAdvance.withEngineCheck(pawnAdvance, pawnCheck(EngineCheckStatus.Caps)).get
    val refutedPawn = ScenePawnAdvance.withEngineCheck(pawnAdvance, pawnCheck(EngineCheckStatus.Refutes)).get
    Vector("capped" -> cappedPawn, "refuted" -> refutedPawn).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PawnAdvance-6 ExplanationPlan accepts only selected uncapped PawnAdvance Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val leadVerdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(leadVerdict).get

    assertEquals(leadVerdict.selected, true)
    assertEquals(leadVerdict.role, Role.Lead)
    assertEquals(leadVerdict.leadAllowed, true)
    assertEquals(leadVerdict.engineStrengthLimited, false)
    assertEquals(plan.scene, Scene.PawnAdvance)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("advances_passed_pawn"))
    assertEquals(ExplanationClaim.PawnAdvanceAllowed.map(_.key), Vector("advances_passed_pawn"))

    val forbiddenClaimKeys =
      Vector(
        "promotion_threat",
        "unstoppable_pawn",
        "wins_endgame",
        "converts_advantage",
        "best_move",
        "only_move",
        "forced",
        "decisive",
        "creates_pressure",
        "takes_initiative"
      )
    forbiddenClaimKeys.foreach: key =>
      assert(ExplanationClaim.PawnAdvanceForbiddenKeys.contains(key), s"PawnAdvance missing forbidden claim key: $key")
      assert(plan.forbiddenWording.map(_.key).contains(key), s"PawnAdvance plan missing forbidden wording key: $key")
    assertEquals(
      ExplanationClaim.PawnAdvanceAllowed.map(_.key).toSet.intersect(ExplanationClaim.PawnAdvanceForbiddenKeys.toSet),
      Set.empty[String]
    )

    def pawnCheck(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(advance))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supportVerdict = leadVerdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict =
      leadVerdict.copy(role = Role.Blocked, leadAllowed = false, engineCheckStatus = Some(EngineCheckStatus.Refutes))
    val unselectedVerdict = leadVerdict.copy(selected = false)
    val cappedVerdict =
      StoryTable.choose(Vector(ScenePawnAdvance.withEngineCheck(story, pawnCheck(EngineCheckStatus.Caps)).get)).head
    val refutedVerdict =
      StoryTable.choose(Vector(ScenePawnAdvance.withEngineCheck(story, pawnCheck(EngineCheckStatus.Refutes)).get)).head

    Vector(
      "Support" -> supportVerdict,
      "Context" -> contextVerdict,
      "Blocked" -> blockedVerdict,
      "unselected" -> unselectedVerdict,
      "capped" -> cappedVerdict,
      "refuted" -> refutedVerdict
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("PawnAdvance-7 DeterministicRenderer phrases only bounded passed-pawn advance"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    val fromPlanMethods = DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    assertEquals(fromPlanMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)), Vector(Vector("ExplanationPlan")))
    assertEquals(rendered.text, "e6 advances the passed pawn.")
    assertEquals(rendered.claimKey, "advances_passed_pawn")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

    Vector(
      "cannot be stopped" -> "e6 cannot be stopped",
      "will promote" -> "e6 will promote",
      "wins" -> "e6 wins",
      "winning endgame" -> "e6 is a winning endgame",
      "converts" -> "e6 converts",
      "best move" -> "e6 is the best move",
      "only move" -> "e6 is the only move",
      "forces" -> "e6 forces",
      "decisive" -> "e6 is decisive",
      "creates pressure" -> "e6 creates pressure"
    ).foreach: (label, forgedRouteSan) =>
      assertEquals(DeterministicRenderer.fromPlan(plan.copy(routeSan = Some(forgedRouteSan))), None, label)

  test("PawnAdvance-8 LLM smoke reuses 8B prompt boundary without new chess facts"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector(
      "renderedText: e6 advances the passed pawn.",
      "claimKey: advances_passed_pawn",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"PawnAdvance prompt must include allowed field: $required")
    Vector(
      "Story",
      "PawnAdvanceProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "FEN",
      "route:",
      "evidence line:"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"PawnAdvance prompt must not include raw input label: $forbidden")

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("e6 advances the passed pawn."))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "e6 advances the passed pawn.").accepted, true)

    Vector(
      "raw Story" -> "The raw Story says e6 advances the passed pawn.",
      "raw PawnAdvanceProof" -> "PawnAdvanceProof says e6 advances the passed pawn.",
      "BoardFacts" -> "BoardFacts show e6 advances the passed pawn.",
      "EngineCheck" -> "EngineCheck supports e6.",
      "raw PV" -> "raw PV e6 Ke7 proves it.",
      "proofFailures" -> "proofFailures are empty, so e6 is right."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)
      assert(result.violations.contains("raw_input"), s"$label must be rejected as raw input leak: $result")

    Vector(
      "new move" -> "e6 and then e7 advances again.",
      "new line" -> "After e6 Ke7, the pawn keeps going.",
      "promotion" -> "e6 will promote.",
      "unstoppable" -> "e6 cannot be stopped.",
      "winning" -> "e6 wins the endgame.",
      "conversion" -> "e6 converts the advantage."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v => v == "new_move_or_line" || v == "forbidden_wording" || v == "new_tactic_or_plan" || v == "stronger_claim"),
        s"$label must be rejected as new chess fact or stronger claim: $result"
      )

  test("PawnAdvance Closeout hard cleanup keeps ownership layers separated and surfaces closed"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val observation =
      facts.seen.passedPawnObservations.find(row => row.side == Side.White && row.pawn.square == Square('e', 5)).get
    val proof = PawnAdvanceProof.fromBoardFacts(facts, advance)
    val story = ScenePawnAdvance.write(facts, advance).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(observation.pawn.man, Man.Pawn)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.alreadyPassedBefore, true)
    assertEquals(proof.afterBoardPassedPawn, true)
    assertEquals(story.pawnAdvanceProof.contains(proof), true)
    assertEquals(story.scene, Scene.PawnAdvance)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnAdvance))
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.anchor, proof.fromSquare)
    assertEquals(story.target, proof.toSquare)
    assertEquals(story.route, proof.advanceMove)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.forcing, 0)
    assertEquals(story.proof.kingHeat, 0)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.AdvancesPassedPawn))
    assertEquals(ExplanationClaim.PawnAdvanceAllowed.map(_.key), Vector("advances_passed_pawn"))
    assertEquals(rendered.text, "e6 advances the passed pawn.")
    assertEquals(rendered.claimKey, "advances_passed_pawn")
    assert(!rendered.text.toLowerCase.contains("promot"))
    assert(!rendered.text.toLowerCase.contains("unstoppable"))
    assert(!rendered.text.toLowerCase.contains("conversion"))
    assert(!rendered.text.toLowerCase.contains("clear path"))
    assert(!rendered.text.toLowerCase.contains("strategy"))
    assert(!rendered.text.toLowerCase.contains("wins"))

    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet
    Vector(
      "promotion_threat",
      "unstoppable_pawn",
      "wins_endgame",
      "converts_advantage",
      "pawn_race",
      "passed_pawn_strategy",
      "best_move",
      "only_move",
      "forced",
      "decisive",
      "creates_pressure",
      "takes_initiative"
    ).foreach: key =>
      assert(forbiddenKeys.contains(key), s"PawnAdvance closeout must forbid $key")

    Vector(
      "e6 will promote.",
      "e6 cannot be stopped.",
      "e6 converts the advantage.",
      "e6 has a clear path.",
      "e6 is the passed pawn strategy.",
      "e6 creates pressure.",
      "e6 is the best move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnStop-0 opens only bounded immediate passed pawn next-square stop"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))

    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.scene, Scene.PawnStop)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.Black)
    assertEquals(story.rival, Side.White)
    assertEquals(story.target, Some(Square('e', 6)))
    assertEquals(story.anchor, Some(Square('g', 7)))
    assertEquals(story.route, Some(stop))
    assertEquals(story.routeSan, Some("Ne6"))
    assertEquals(story.writer, Some(StoryWriter.ScenePawnStop))
    assert(story.pawnStopProof.exists(_.complete))
    assertEquals(story.pawnStopProof.exists(_.publicClaimAllowed), false)
    assertEquals(story.pawnStopProof.exists(_.targetPawnAlreadyPassed), true)
    assertEquals(story.pawnStopProof.exists(_.nextAdvanceSquareStoppedAfter), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("stops_pawn_advance"))
    assertEquals(rendered.text, "Ne6 stops the passed pawn from advancing next.")
    assertEquals(rendered.claimKey, "stops_pawn_advance")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      "Ne6 stops promotion.",
      "Ne6 permanently stops the pawn.",
      "Ne6 draws the tablebase.",
      "Ne6 is the best defense.",
      "Ne6 is the only move.",
      "Ne6 wins the endgame.",
      "Ne6 stops conversion.",
      "Ne6 wins the pawn race.",
      "Ne6 uses the king route.",
      "Ne6 has the opposition.",
      "Ne6 is passed pawn strategy."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnStop-1 PawnStopProof binds exact legal move to already-passed pawn next square"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val proof = PawnStopProof.fromBoardFacts(facts, stop)

    assertEquals(proof.side, Side.Black)
    assertEquals(proof.rivalSide, Side.White)
    assertEquals(proof.targetPawn, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(proof.stoppingPieceBefore, Some(Piece(Side.Black, Man.Knight, Square('g', 7))))
    assertEquals(proof.stoppingPieceAfter, Some(Piece(Side.Black, Man.Knight, Square('e', 6))))
    assertEquals(proof.stopMove, Some(stop))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalStopMove, true)
    assertEquals(proof.targetPawnAlreadyPassed, true)
    assertEquals(proof.nextAdvanceSquareNonPromotion, true)
    assertEquals(proof.nextAdvanceSquareEmptyBefore, true)
    assertEquals(proof.stopKind, Some(PawnStopKind.NextSquareOccupied))
    assertEquals(proof.nextAdvanceSquareOccupiedAfter, true)
    assertEquals(proof.nextAdvanceSquareAttackedAfter, false)
    assertEquals(proof.nextAdvanceSquareControlledByPawnAfter, false)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.targetPawnStillPresentAfter, true)
    assertEquals(proof.nextAdvanceSquareStoppedAfter, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.missingEvidence, Vector.empty)
    assert(facts.seen.passedPawnObservations.exists(row => row.side == Side.White && row.pawn == proof.targetPawn.get))
    assertEquals(
      PawnStopKind.values.toVector,
      Vector(
        PawnStopKind.NextSquareOccupied,
        PawnStopKind.NextSquareAttacked,
        PawnStopKind.NextSquareControlledByPawn
      )
    )

  test("PawnStop-1 PawnStopProof admits direct next-square attack but not pre-existing blockade"):
    val attackedFacts = BoardFacts.fromFen("k3r3/8/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val attackStop = Line(Square('e', 8), Square('e', 7))
    val attackedProof = PawnStopProof.fromBoardFacts(attackedFacts, attackStop)

    assertEquals(attackedProof.targetPawn, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(attackedProof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(attackedProof.stoppingPieceAfter, Some(Piece(Side.Black, Man.Rook, Square('e', 7))))
    assertEquals(attackedProof.stopKind, Some(PawnStopKind.NextSquareAttacked))
    assertEquals(attackedProof.nextAdvanceSquareOccupiedAfter, false)
    assertEquals(attackedProof.nextAdvanceSquareAttackedAfter, true)
    assertEquals(attackedProof.nextAdvanceSquareControlledByPawnAfter, false)
    assertEquals(attackedProof.nextAdvanceSquareStoppedAfter, true)
    assertEquals(attackedProof.complete, true)
    assert(ScenePawnStop.write(attackedFacts, attackStop).nonEmpty)

    val preBlockedFacts = BoardFacts.fromFen("4k3/8/4n3/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val unrelatedMove = Line(Square('e', 8), Square('d', 8))
    val preBlockedProof = PawnStopProof.fromBoardFacts(preBlockedFacts, unrelatedMove)
    assertEquals(preBlockedProof.nextAdvanceSquareEmptyBefore, false)
    assertEquals(preBlockedProof.stopKind, None)
    assertEquals(preBlockedProof.nextAdvanceSquareStoppedAfter, false)
    assertEquals(ScenePawnStop.write(preBlockedFacts, unrelatedMove), None)

  test("PawnStop-2 ScenePawnStop writer binds only bounded pawn-stop Story identity"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val proof = story.pawnStopProof.get

    assertEquals(story.scene, Scene.PawnStop)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.Black)
    assertEquals(story.rival, Side.White)
    assertEquals(story.target, Some(Square('e', 6)))
    assertEquals(story.anchor, Some(Square('g', 7)))
    assertEquals(story.route, Some(stop))
    assertEquals(story.writer, Some(StoryWriter.ScenePawnStop))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.legalStopMove, true)
    assertEquals(proof.targetPawn.nonEmpty, true)
    assertEquals(proof.nextAdvanceSquareStoppedAfter, true)
    assertEquals(
      proof.nextAdvanceSquareOccupiedAfter ||
        proof.nextAdvanceSquareAttackedAfter ||
        proof.nextAdvanceSquareControlledByPawnAfter,
      true
    )

    val supportedCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(stop))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 1), Square('e', 2))))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0)
    )
    assertEquals(ScenePawnStop.withEngineCheck(story, supportedCheck).nonEmpty, true)

    val refutingCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(stop))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 1), Square('e', 2))))),
      evalBefore = Some(EngineEval(200)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0)
    )
    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    val refutedStory = ScenePawnStop.withEngineCheck(story, refutingCheck).get
    val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    val forgedAnchor = story.copy(anchor = proof.targetPawn.map(_.square))
    val forgedDefense = story.copy(scene = Scene.Defense)
    val forgedPromotion = story.copy(tactic = Some(Tactic.Promote))
    val forgedEndgame = story.copy(scene = Scene.Endgame)

    Vector(forgedAnchor, forgedDefense, forgedPromotion, forgedEndgame).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked)
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None)

  test("PawnStop-3 negative corpus requires complete PawnStopProof or silence"):
    val positiveFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val notPassedFacts = BoardFacts.fromFen("4k3/6n1/3p4/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val promotionStopFacts = BoardFacts.fromFen("3k4/4P3/8/8/8/8/8/4K3 b - - 0 1").toOption.get
    val illegalMove = Line(Square('g', 7), Square('g', 8))
    val safeNextAdvanceMove = Line(Square('g', 7), Square('f', 5))
    val sameBoardProofMissing =
      minimalBoardFacts(
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5)),
          Piece(Side.Black, Man.Knight, Square('g', 7))
        ),
        sideLegal = Moves(
          known = true,
          lines = Vector(Line(Square('g', 7), Square('e', 6))),
          san = Vector("Ne6"),
          moveCount = 1
        )
      )

    val illegalProof = PawnStopProof.fromBoardFacts(positiveFacts, illegalMove)
    assertEquals(illegalProof.legalStopMove, false)
    assertEquals(illegalProof.complete, false)
    assertEquals(ScenePawnStop.write(positiveFacts, illegalMove), None, "legal move absent")

    assertEquals(ScenePawnStop.write(notPassedFacts, Line(Square('g', 7), Square('e', 6))), None, "not already passed")
    assertEquals(ScenePawnStop.write(promotionStopFacts, Line(Square('d', 8), Square('e', 8))), None, "promotion stop")
    assertEquals(ScenePawnStop.write(sameBoardProofMissing, Line(Square('g', 7), Square('e', 6))), None, "same-board proof")

    val safeNextAdvanceProof = PawnStopProof.fromBoardFacts(positiveFacts, safeNextAdvanceMove)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareOccupiedAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareAttackedAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareControlledByPawnAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareStoppedAfter, false)
    assertEquals(ScenePawnStop.write(positiveFacts, safeNextAdvanceMove), None, "next advance square remains empty and safe")

    val story = ScenePawnStop.write(positiveFacts, Line(Square('g', 7), Square('e', 6))).get
    val forgedEndgameDefense = story.copy(scene = Scene.Endgame)
    val forgedPlan = story.copy(plan = Some(Plan.PasserBlock))
    val missingNextSquare =
      story.copy(pawnStopProof =
        story.pawnStopProof.map(_.copy(nextAdvanceSquare = None, missingEvidence = Vector.empty))
      )
    val incompleteStop =
      story.copy(pawnStopProof =
        story.pawnStopProof.map(_.copy(nextAdvanceSquareStoppedAfter = false, missingEvidence = Vector.empty))
      )
    val missingStopKind =
      story.copy(pawnStopProof =
        story.pawnStopProof.map(_.copy(stopKind = None, missingEvidence = Vector.empty))
      )
    Vector(forgedEndgameDefense, forgedPlan, missingNextSquare, incompleteStop, missingStopKind).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked)
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("PawnStop-4 reuses EngineCheck without creating engine-owned claims"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val reply = Line(Square('e', 1), Square('e', 2))

    def check(status: EngineCheckStatus, boundStory: Option[Story] = Some(story)): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = boundStory,
        engineLine = Some(EngineLine(Vector(stop))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def selected(attached: Story): Verdict =
      StoryTable.choose(Vector(attached)).head

    def noStandaloneEngineText(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("engine says")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("best defense")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("only move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("tablebase")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("winning endgame")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("losing endgame")), false, label)
      assertEquals(rendered.exists(_.text.contains("+3.2")), false, label)
      assertEquals(rendered.exists(_.text.contains("40")), false, label)
      assertEquals(rendered.exists(_.text.contains("45")), false, label)

    val supports = ScenePawnStop.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePawnStop.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePawnStop.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = ScenePawnStop.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val unbound = check(EngineCheckStatus.Supports, boundStory = None)
    val wrongRoute =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(Line(Square('g', 7), Square('f', 5))))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    val supportsVerdict = selected(supports)
    val capsVerdict = selected(caps)
    val unknownVerdict = selected(unknown)
    val refutesVerdict = selected(refutes)
    val capsPlan = ExplanationPlan.fromSelected(capsVerdict)
    val refutesPlan = ExplanationPlan.fromSelected(refutesVerdict)

    assertEquals(
      ScenePawnStop.withEngineCheck(story, unbound),
      Option.empty[Story],
      "EngineCheck cannot attach without Story binding"
    )
    assertEquals(
      ScenePawnStop.withEngineCheck(story, wrongRoute),
      Option.empty[Story],
      "EngineCheck cannot attach a different route"
    )
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty, "EngineCheck alone cannot create PawnStop")

    assertEquals(supports.engineCheck.map(_.status), Option(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Option(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Option(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Option(EngineCheckStatus.Refutes))

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.leadAllowed, true)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.leadAllowed, true)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(capsPlan, Option.empty[ExplanationPlan])
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesPlan, Option.empty[ExplanationPlan])

    Vector(
      "supports" -> supportsVerdict,
      "caps" -> capsVerdict,
      "unknown" -> unknownVerdict,
      "refutes" -> refutesVerdict
    ).foreach: (label, verdict) =>
      noStandaloneEngineText(label, verdict)

    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
    Vector(
      "engine says Ne6 is best defense",
      "+3.2 and winning endgame",
      "only move for a tablebase draw",
      "best defense",
      "only move",
      "tablebase draw",
      "winning endgame",
      "losing endgame"
    ).foreach: phrase =>
      assertEquals(LlmNarrationSmoke.mockNarrate(supportsPlan, supportsRendered.copy(text = phrase)), None, phrase)

  test("PawnStop-5 StoryTable keeps existing claim homes and same-pawn PawnAdvance ahead of PawnStop"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim),
          verdict.engineStrengthLimited
        )

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    val pawnStopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val pawnStopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = ScenePawnStop.write(pawnStopFacts, pawnStopMove).get.copy(proof = strongProof(100))

    val pawnAdvanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnAdvanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = ScenePawnAdvance.write(pawnAdvanceFacts, pawnAdvanceMove).get.copy(proof = strongProof(90))

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialLine = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(materialFacts, materialLine).get.copy(proof = strongProof(90))
    val material = SceneMaterial.write(materialFacts, materialLine).get.copy(proof = strongProof(90))

    val forkFacts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get.copy(proof = strongProof(90))

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, threatLine, defenseLine).get.copy(proof = strongProof(90))

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(90))

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      TacticPin
        .write(pinFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
        .get
        .copy(proof = strongProof(90))

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5)))
        .get
        .copy(proof = strongProof(90))

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerLine = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer
        .write(skewerFacts, Some(skewerLine), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
        .copy(proof = strongProof(90))

    val existingRows =
      Vector(
        "Tactic.Hanging" -> hanging,
        "Tactic.Fork" -> fork,
        "Scene.Material" -> material,
        "Scene.Defense" -> defense,
        "Tactic.DiscoveredAttack" -> discovered,
        "Tactic.Pin" -> pin,
        "Tactic.RemoveGuard" -> removeGuard,
        "Tactic.Skewer" -> skewer
      )
    val mixedRows = existingRows :+ ("Scene.PawnAdvance" -> pawnAdvance) :+ ("Scene.PawnStop" -> pawnStop)
    val forward = StoryTable.choose(mixedRows.map(_._2))
    val reverse = StoryTable.choose(mixedRows.reverse.map(_._2))
    val shuffled =
      StoryTable.choose(
        Vector(
          mixedRows(9),
          mixedRows(2),
          mixedRows(8),
          mixedRows(5),
          mixedRows(0),
          mixedRows(7),
          mixedRows(3),
          mixedRows(1),
          mixedRows(6),
          mixedRows(4)
        ).map(_._2)
      )

    assertEquals(shape(mixedRows, forward), shape(mixedRows, reverse))
    assertEquals(shape(mixedRows, forward), shape(mixedRows, shuffled))
    assert(rowId(mixedRows, forward.head.story) != "Scene.PawnStop")
    assertEquals(forward.count(_.role == Role.Lead), 1)

    existingRows.foreach: (label, row) =>
      val verdicts = StoryTable.choose(Vector(pawnStop, row))
      val existingVerdict = verdicts.find(_.story == row).get
      val pawnStopVerdict = verdicts.find(_.story == pawnStop).get

      assertEquals(existingVerdict.role, Role.Lead, label)
      assertEquals(existingVerdict.leadAllowed, true, label)
      assertEquals(pawnStopVerdict.role, Role.Support, label)
      assertEquals(pawnStopVerdict.leadAllowed, false, label)
      val existingPlan = ExplanationPlan.fromSelected(existingVerdict).get
      val existingRendered = DeterministicRenderer.fromPlan(existingPlan).get
      assertEquals(ExplanationPlan.fromSelected(pawnStopVerdict), None, label)
      assertEquals(existingPlan.allowedClaim.exists(_ == ExplanationClaim.StopsPassedPawnNextAdvance), false, label)
      assertEquals(existingPlan.allowedClaim.map(_.key).contains("threatens_promotion_next"), false, label)
      assert(!existingRendered.text.toLowerCase.contains("passed pawn"), label)

    val defenseCollision = StoryTable.choose(Vector(pawnStop, defense))
    val defenseLead = defenseCollision.find(_.story == defense).get
    val pawnStopWithDefense = defenseCollision.find(_.story == pawnStop).get
    assertEquals(defenseLead.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(defenseLead).flatMap(_.allowedClaim), Some(ExplanationClaim.DefendsPiece))
    assertEquals(pawnStopWithDefense.role, Role.Support)

    val materialCollision = StoryTable.choose(Vector(pawnStop, material))
    val materialLead = materialCollision.find(_.story == material).get
    val pawnStopWithMaterial = materialCollision.find(_.story == pawnStop).get
    assertEquals(materialLead.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(materialLead).flatMap(_.allowedClaim), Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(pawnStopWithMaterial.role, Role.Support)

    val samePawnRows = Vector("Scene.PawnAdvance" -> pawnAdvance, "Scene.PawnStop" -> pawnStop)
    val samePawnForward = StoryTable.choose(samePawnRows.map(_._2))
    val samePawnReverse = StoryTable.choose(samePawnRows.reverse.map(_._2))
    val advanceVerdict = samePawnForward.find(_.story == pawnAdvance).get
    val stopVerdict = samePawnForward.find(_.story == pawnStop).get
    assertEquals(shape(samePawnRows, samePawnForward), shape(samePawnRows, samePawnReverse))
    assertEquals(advanceVerdict.role, Role.Lead)
    assertEquals(advanceVerdict.leadAllowed, true)
    assertEquals(stopVerdict.role, Role.Support)
    assertEquals(stopVerdict.leadAllowed, false)
    assertNoStandaloneText("PawnStop support under same-pawn PawnAdvance", stopVerdict)

    def pawnStopCheck(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = pawnStopFacts,
        story = Some(pawnStop),
        engineLine = Some(EngineLine(Vector(pawnStopMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 1), Square('e', 2))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val cappedPawnStop = ScenePawnStop.withEngineCheck(pawnStop, pawnStopCheck(EngineCheckStatus.Caps)).get
    val refutedPawnStop = ScenePawnStop.withEngineCheck(pawnStop, pawnStopCheck(EngineCheckStatus.Refutes)).get
    Vector("capped" -> cappedPawnStop, "refuted" -> refutedPawnStop).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PawnStop-6 ExplanationPlan accepts only selected uncapped PawnStop Lead"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(plan.scene, Scene.PawnStop)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.StopsPassedPawnNextAdvance))
    assertEquals(plan.allowedClaim.map(_.key), Some("stops_pawn_advance"))
    assertEquals(ExplanationClaim.PawnStopAllowed.map(_.key), Vector("stops_pawn_advance"))
    assertEquals(ExplanationClaim.PawnStopForbiddenKeys.toSet.contains("stops_pawn_advance"), false)
    assertEquals(rendered.text, "Ne6 stops the passed pawn from advancing next.")
    assert(!rendered.text.toLowerCase.contains("promotion"))
    assert(!rendered.text.toLowerCase.contains("permanent"))
    assert(!rendered.text.toLowerCase.contains("endgame"))
    assert(!rendered.text.toLowerCase.contains("best"))
    assert(!rendered.text.toLowerCase.contains("only"))
    assert(!rendered.text.toLowerCase.contains("draw"))
    assert(!rendered.text.toLowerCase.contains("strategy"))

    Vector(
      "stops_promotion",
      "permanently_stops_pawn",
      "draws_endgame",
      "best_defense",
      "only_move",
      "tablebase_draw",
      "wins_endgame",
      "converts_advantage",
      "forced"
    ).foreach: key =>
      assert(ExplanationClaim.PawnStopForbiddenKeys.contains(key), s"PawnStop must forbid $key")
      assert(plan.forbiddenWording.map(_.key).contains(key), s"PawnStop plan must forbid $key")

    Vector(
      "stops_passed_pawn_next_advance",
      "promotion_stop",
      "permanent_stop",
      "conversion_stopped"
    ).foreach: key =>
      assertEquals(ExplanationClaim.PawnStopAllowed.map(_.key).contains(key), false, key)

    Vector(
      "support" -> verdict.copy(role = Role.Support, leadAllowed = false),
      "context" -> verdict.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> verdict.copy(role = Role.Blocked, leadAllowed = false),
      "unselected" -> verdict.copy(selected = false),
      "capped" -> verdict.copy(engineStrengthLimited = true, engineCheckStatus = Some(EngineCheckStatus.Caps)),
      "refuted" -> verdict.copy(role = Role.Blocked, leadAllowed = false, engineCheckStatus = Some(EngineCheckStatus.Refutes))
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(row), None, label)

    Vector(
      "promotion_stop",
      "permanent_stop",
      "conversion_stopped",
      "pawn_race",
      "king_route",
      "opposition",
      "passed_pawn_strategy"
    ).foreach: key =>
      assertEquals(ExplanationClaim.PawnStopForbiddenKeys.contains(key), false, key)

    Vector(
      "Ne6 stops promotion.",
      "Ne6 stops the promotion threat.",
      "Ne6 creates a tablebase draw.",
      "Ne6 draws the position.",
      "Ne6 reaches king opposition.",
      "Ne6 permanently stops the pawn.",
      "Ne6 means the pawn cannot advance.",
      "Ne6 is the only move.",
      "Engine says Ne6 stops the passed pawn's next advance."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnStop-7 DeterministicRenderer phrases only bounded next-advance stop"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(rendered.text, "Ne6 stops the passed pawn from advancing next.")
    assertEquals(rendered.claimKey, "stops_pawn_advance")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

    Vector(
      "stops promotion",
      "stops the pawn for good",
      "draws",
      "holds the endgame",
      "best defense",
      "only move",
      "forces",
      "wins",
      "tablebase"
    ).foreach: forbidden =>
      val forged = plan.copy(routeSan = Some(forbidden))
      assertEquals(DeterministicRenderer.fromPlan(forged), None, forbidden)

    val methodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assert(methodNames.contains("fromPlan"))
    Vector("fromVerdict", "fromStory", "fromBoardFacts", "fromPawnStopProof", "fromEngineCheck").foreach: method =>
      assert(!methodNames.contains(method), s"renderer must not expose $method")
    Vector("Verdict", "Story", "BoardFacts", "PawnStopProof", "EngineCheck").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"renderer must not accept $parameter")

  test("PawnStop-8 LLM smoke reuses 8B prompt boundary without new chess facts"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector(
      "renderedText: Ne6 stops the passed pawn from advancing next.",
      "claimKey: stops_pawn_advance",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"PawnStop prompt must include allowed field: $required")
    Vector(
      "Story",
      "PawnStopProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "proofFailures",
      "FEN",
      "PGN",
      "BoardMood",
      "Verdict",
      "raw Story",
      "source row",
      "role:",
      "scene:",
      "route:",
      "evidence line:"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"PawnStop prompt must not include raw input label: $forbidden")

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("Ne6 stops the passed pawn from advancing next."))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "Ne6 stops the passed pawn from advancing next.").accepted, true)

    Vector(
      "raw Story" -> "Raw Story says Ne6 stops the pawn.",
      "raw PawnStopProof" -> "PawnStopProof says Ne6 stops the passed pawn.",
      "BoardFacts" -> "BoardFacts show e6 is stopped.",
      "EngineCheck" -> "EngineCheck supports Ne6.",
      "raw PV" -> "Raw PV: Ne6 e2.",
      "proofFailures" -> "proofFailures are empty, so Ne6 works."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)
      assert(result.violations.contains("raw_input"), s"$label must be rejected as raw input leak: $result")

    Vector(
      "new move" -> "Nf5 also stops the passed pawn.",
      "new line" -> "After Ne6 Kd7 the pawn is stopped.",
      "promotion" -> "Ne6 stops promotion.",
      "permanent stop" -> "Ne6 stops the pawn for good.",
      "draw" -> "Ne6 draws the endgame.",
      "tablebase" -> "Ne6 is a tablebase draw.",
      "winning" -> "Ne6 wins the endgame."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v => v == "new_move_or_line" || v == "forbidden_wording" || v == "new_tactic_or_plan" || v == "stronger_claim"),
        s"$label must be rejected as new chess fact or stronger claim: $result"
      )

    val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
    val mismatchedRendered = rendered.copy(claimKey = "stops_promotion")
    assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector("fromStory", "fromPawnStopProof", "fromBoardFacts", "fromEngineCheck", "fromEngineLine", "callApi", "productionApi").foreach:
      method =>
        assert(!methodNames.contains(method), s"LLM smoke must not expose $method")
    Vector("Story", "PawnStopProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"LLM smoke must not accept $parameter")

  test("PromotionThreat-0 opens only immediate next-move promotion threat after a legal pawn move"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val promotionRoute = Line(Square('e', 7), Square('e', 8))
    val proof = PromotionThreatProof.fromBoardFacts(facts, creatingMove)
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 6))))
    assertEquals(proof.pawnAfter, Some(Piece(Side.White, Man.Pawn, Square('e', 7))))
    assertEquals(proof.creatingMove, Some(creatingMove))
    assertEquals(proof.nextPromotionMove, Some(promotionRoute))
    assertEquals(proof.promotionSquare, Some(Square('e', 8)))
    assertEquals(proof.promotionRoute, Some(promotionRoute))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.nonPromotionCreatingMove, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.pawnOnPenultimateRankAfter, true)
    assertEquals(proof.nextMovePromotionLegal, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)

    assertEquals(story.scene, Scene.PromotionThreat)
    assertEquals(story.writer, Some(StoryWriter.ScenePromotionThreat))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.anchor, Some(Square('e', 6)))
    assertEquals(story.target, Some(Square('e', 8)))
    assertEquals(story.route, Some(creatingMove))
    assertEquals(story.promotionThreatProof, Some(proof))
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.scene, Scene.PromotionThreat)
    assertEquals(plan.allowedClaim.map(_.key), Some("threatens_promotion_next"))
    assertEquals(ExplanationClaim.PromotionThreatAllowed.map(_.key), Vector("threatens_promotion_next"))
    assertEquals(rendered.text, "e7 threatens to promote next.")
    assertEquals(rendered.claimKey, "threatens_promotion_next")
    assertEquals(rendered.strength, "bounded")

    Vector(
      ForbiddenWording.ActualPromotion,
      ForbiddenWording.UnstoppablePawn,
      ForbiddenWording.WinningEndgame,
      ForbiddenWording.ConvertsAdvantage,
      ForbiddenWording.PawnRace,
      ForbiddenWording.BestMove,
      ForbiddenWording.OnlyMove,
      ForbiddenWording.Forced
    ).foreach: forbidden =>
      assert(plan.forbiddenWording.contains(forbidden), s"PromotionThreat must forbid ${forbidden.key}")

    Vector(
      "e7 will promote.",
      "e7 will queen.",
      "e7 cannot be stopped.",
      "e7 wins the endgame.",
      "e7 is the best move.",
      "e7 starts conversion.",
      "e7 wins the pawn race."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PromotionThreat-0 stays silent for actual promotion and non-immediate promotion-looking moves"):
    val notImmediateMove = Line(Square('e', 5), Square('e', 6))
    val notImmediateFacts = BoardFacts.fromFen("k7/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val actualPromotionMove = Line(Square('e', 7), Square('e', 8))
    val actualPromotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val blockedPromotionMove = Line(Square('e', 6), Square('e', 7))
    val blockedPromotionFacts = BoardFacts.fromFen("4r1k1/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get

    val notImmediateProof = PromotionThreatProof.fromBoardFacts(notImmediateFacts, notImmediateMove)
    val actualPromotionProof = PromotionThreatProof.fromBoardFacts(actualPromotionFacts, actualPromotionMove)
    val blockedPromotionProof = PromotionThreatProof.fromBoardFacts(blockedPromotionFacts, blockedPromotionMove)

    assertEquals(notImmediateProof.pawnOnPenultimateRankAfter, false)
    assertEquals(notImmediateProof.nextMovePromotionLegal, false)
    assertEquals(notImmediateProof.complete, false)
    assertEquals(ScenePromotionThreat.write(notImmediateFacts, notImmediateMove), None)

    assertEquals(actualPromotionProof.nonPromotionCreatingMove, false)
    assertEquals(actualPromotionProof.exactAfterBoardReplay, false)
    assertEquals(actualPromotionProof.complete, false)
    assertEquals(ScenePromotionThreat.write(actualPromotionFacts, actualPromotionMove), None)

    assertEquals(blockedPromotionProof.pawnOnPenultimateRankAfter, true)
    assertEquals(blockedPromotionProof.nextMovePromotionLegal, false)
    assertEquals(blockedPromotionProof.complete, false)
    assertEquals(ScenePromotionThreat.write(blockedPromotionFacts, blockedPromotionMove), None)

    val advanceStory = ScenePawnAdvance.write(BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get, blockedPromotionMove).get
    val forgedPromotionThreat = advanceStory.copy(scene = Scene.PromotionThreat, promotionThreatProof = None)
    val verdict = StoryTable.choose(Vector(forgedPromotionThreat)).head
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("PromotionThreat-1 PromotionThreatProof owns only diagnostic next-move promotion evidence"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val nextPromotionMove = Line(Square('e', 7), Square('e', 8))
    val proof = PromotionThreatProof.fromBoardFacts(facts, creatingMove)
    val advanceProof = PawnAdvanceProof.fromBoardFacts(facts, creatingMove)
    val stopProof = PawnStopProof.fromBoardFacts(facts, creatingMove)
    val promotionStory = ScenePromotionThreat.write(facts, creatingMove).get
    val advanceStory = ScenePawnAdvance.write(facts, creatingMove).get
    val verdicts = StoryTable.choose(Vector(advanceStory, promotionStory))

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore.exists(piece => piece.man == Man.Pawn && piece.side == Side.White), true)
    assertEquals(proof.nonPromotionCreatingMove, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.nextPromotionMove, Some(nextPromotionMove))
    assertEquals(proof.promotionRoute, Some(nextPromotionMove))
    assertEquals(proof.nextMovePromotionLegal, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)

    assertEquals(advanceProof.publicClaimAllowed, false)
    assertEquals(stopProof.complete, false)
    assertEquals(ScenePawnStop.write(facts, creatingMove), None)
    assertEquals(advanceStory.promotionThreatProof, None)
    assertEquals(advanceStory.scene, Scene.PawnAdvance)
    assertEquals(promotionStory.promotionThreatProof, Some(proof))
    assertEquals(verdicts.find(_.story.scene == Scene.PromotionThreat).map(_.role), Some(Role.Lead))
    assertEquals(verdicts.find(_.story.scene == Scene.PawnAdvance).exists(_.role == Role.Lead), false)

  test("PromotionThreat-2 ScenePromotionThreat writer binds identity and blocks EngineCheck Refutes"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val nextPromotionMove = Line(Square('e', 7), Square('e', 8))
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val proof = story.promotionThreatProof.get

    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(story.writer, Some(StoryWriter.ScenePromotionThreat))
    assertEquals(story.scene, Scene.PromotionThreat)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.promotionSquare)
    assertEquals(story.anchor, proof.pawnBefore.map(_.square))
    assertEquals(story.route, Some(creatingMove))
    assertEquals(story.secondaryTarget, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(proof.nextPromotionMove, Some(nextPromotionMove))

    val refutingCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(creatingMove))),
      replyLine = Some(EngineLine(Vector(nextPromotionMove))),
      evalBefore = Some(EngineEval(120)),
      evalAfter = Some(EngineEval(-80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(refutingCheck.storyBound, true)
    assertEquals(refutingCheck.evidenceReady, true)
    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)

    val refutedStory = ScenePromotionThreat.withEngineCheck(story, refutingCheck).get
    val verdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

    val actualPromotionStory = story.copy(
      route = Some(nextPromotionMove),
      anchor = Some(Square('e', 7)),
      target = Some(Square('e', 8))
    )
    assertEquals(StoryTable.choose(Vector(actualPromotionStory)).head.role, Role.Blocked)

  test("PromotionThreat-3 negative corpus requires legal next-move promotion proof or silence"):
    val legalFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val legalCreatingMove = Line(Square('e', 6), Square('e', 7))
    val legalStory = ScenePromotionThreat.write(legalFacts, legalCreatingMove).get
    val legalVerdict = StoryTable.choose(Vector(legalStory)).head
    val legalPlan = ExplanationPlan.fromSelected(legalVerdict).get
    val legalRendered = DeterministicRenderer.fromPlan(legalPlan).get

    val illegalCreatingMove = Line(Square('e', 6), Square('e', 8))
    val nonPawnMove = Line(Square('e', 1), Square('e', 2))
    val actualPromotionMove = Line(Square('e', 7), Square('e', 8))
    val twoMovesNeeded = Line(Square('e', 5), Square('e', 6))
    val blockedPromotionMove = Line(Square('e', 6), Square('e', 7))
    val manualFacts = minimalBoardFacts(
      sideLegal = readyMoves(line = legalCreatingMove),
      pieces = Vector(Piece(Side.White, Man.Pawn, Square('e', 6)), Piece(Side.White, Man.King, Square('e', 1)), Piece(Side.Black, Man.King, Square('a', 8)))
    )
    val nonPawnFacts = BoardFacts.fromFen("k7/8/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val actualPromotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val twoMovesNeededFacts = BoardFacts.fromFen("k7/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val blockedPromotionFacts = BoardFacts.fromFen("4r1k1/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get

    val negativeCases: Vector[(String, BoardFacts, Line, String)] = Vector(
      "illegal creating move" -> (legalFacts, illegalCreatingMove, "legal pawn move"),
      "no same-board proof" -> (manualFacts, legalCreatingMove, "same-board proof"),
      "not a pawn move" -> (nonPawnFacts, nonPawnMove, "pawn identity"),
      "creating move itself promotes" -> (actualPromotionFacts, actualPromotionMove, "creating move is non-promotion"),
      "next promotion move illegal on after-board" -> (blockedPromotionFacts, blockedPromotionMove, "legal next-move promotion"),
      "promotion square cannot be computed" -> (nonPawnFacts, nonPawnMove, "promotion square"),
      "two or more moves still needed" -> (twoMovesNeededFacts, twoMovesNeeded, "pawn on penultimate rank after move")
    ).map((label, data) => (label, data._1, data._2, data._3))

    negativeCases.foreach:
      case (label, facts, move, expectedMissing) =>
      val proof = PromotionThreatProof.fromBoardFacts(facts, move)
      assertEquals(proof.complete, false, label)
      assertEquals(ScenePromotionThreat.write(facts, move), None, label)
      assert(
        proof.missingEvidence.exists(_.missing.contains(expectedMissing)),
        s"$label must report $expectedMissing"
      )

    Vector(
      "e7 is unstoppable.",
      "e7 cannot be stopped.",
      "e7 wins.",
      "e7 wins by tablebase.",
      "e7 converts the endgame.",
      "e7 will queen."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(legalPlan, legalRendered, output).accepted, false, output)

  test("PromotionThreat-4 reuses EngineCheck without creating engine-owned claims"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val nextPromotionMove = Line(Square('e', 7), Square('e', 8))
    val story = ScenePromotionThreat.write(facts, creatingMove).get

    def check(status: EngineCheckStatus, boundStory: Option[Story] = Some(story)): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = boundStory,
        engineLine = Some(EngineLine(Vector(creatingMove))),
        replyLine = Some(EngineLine(Vector(nextPromotionMove))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def selected(attached: Story): Verdict =
      StoryTable.choose(Vector(attached)).head

    def noEngineText(label: String, verdict: Verdict): Unit =
      val rendered = ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("engine says")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("best move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("only move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("tablebase")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("winning endgame")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("forced win")), false, label)
      assertEquals(rendered.exists(_.text.contains("+3.2")), false, label)
      assertEquals(rendered.exists(_.text.contains("40")), false, label)
      assertEquals(rendered.exists(_.text.contains("45")), false, label)

    val supports = ScenePromotionThreat.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePromotionThreat.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePromotionThreat.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = ScenePromotionThreat.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val unbound = check(EngineCheckStatus.Supports, boundStory = None)
    val wrongRoute =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(Line(Square('e', 6), Square('e', 8))))),
        replyLine = Some(EngineLine(Vector(nextPromotionMove))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    val forgedWithoutProof = story.copy(promotionThreatProof = None)

    assertEquals(
      ScenePromotionThreat.withEngineCheck(story, unbound),
      Option.empty[Story],
      "EngineCheck cannot attach without Story binding"
    )
    assertEquals(
      ScenePromotionThreat.withEngineCheck(story, wrongRoute),
      Option.empty[Story],
      "EngineCheck cannot attach a different route"
    )
    assertEquals(
      ScenePromotionThreat.withEngineCheck(forgedWithoutProof, supports.engineCheck.get),
      Option.empty[Story],
      "EngineCheck cannot repair or create PromotionThreat without proof"
    )
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty, "EngineCheck alone cannot create PromotionThreat")

    assertEquals(supports.engineCheck.map(_.status), Option(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Option(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Option(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Option(EngineCheckStatus.Refutes))

    val supportsVerdict = selected(supports)
    val capsVerdict = selected(caps)
    val unknownVerdict = selected(unknown)
    val refutesVerdict = selected(refutes)

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), Option.empty[ExplanationPlan])
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), Option.empty[ExplanationPlan])

    Vector(
      "supports" -> supportsVerdict,
      "caps" -> capsVerdict,
      "unknown" -> unknownVerdict,
      "refutes" -> refutesVerdict
    ).foreach: (label, verdict) =>
      noEngineText(label, verdict)

    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
    Vector(
      "engine says e7 is best move",
      "+3.2 and winning endgame",
      "only move by tablebase",
      "forced win",
      "best move",
      "only move",
      "tablebase result",
      "winning endgame"
    ).foreach: phrase =>
      assertEquals(LlmNarrationSmoke.mockNarrate(supportsPlan, supportsRendered.copy(text = phrase)), None, phrase)

  test("PromotionThreat-5 StoryTable keeps existing rows stable and claim homes separate"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = 0,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story, value: Int): Story =
      story.copy(proof = strongProof(value))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    def assertStable(rows: Vector[(String, Story)], expectedLead: String): Vector[Verdict] =
      val forward = StoryTable.choose(rows.map(_._2))
      val reverse = StoryTable.choose(rows.reverse.map(_._2))
      val shuffled = StoryTable.choose(rows.sortBy(_._1).map(_._2))
      val forwardShape = shape(rows, forward)
      assertEquals(shape(rows, reverse), forwardShape)
      assertEquals(shape(rows, shuffled), forwardShape)
      assertEquals(rowId(rows, forward.head.story), expectedLead)
      assertEquals(forward.head.role, Role.Lead)
      forward

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertLeadClaim(verdict: Verdict, expected: ExplanationClaim): (ExplanationPlan, RenderedLine) =
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(plan.allowedClaim, Some(expected))
      (plan, rendered)

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 7), Square('e', 8))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val advanceThreatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceThreatMove = Line(Square('e', 6), Square('e', 7))
    val advance = strong(ScenePawnAdvance.write(advanceThreatFacts, advanceThreatMove).get, 90)
    val promotionThreat = strong(ScenePromotionThreat.write(advanceThreatFacts, advanceThreatMove).get, 99)
    val advanceRows = Vector("Scene.PawnAdvance" -> advance, "Scene.PromotionThreat" -> promotionThreat)
    val advanceVerdicts = assertStable(advanceRows, "Scene.PromotionThreat")
    val advanceSupport = advanceVerdicts.find(_.story == advance).get
    val promotionLead = advanceVerdicts.find(_.story == promotionThreat).get
    val (promotionPlan, promotionRendered) = assertLeadClaim(promotionLead, ExplanationClaim.CreatesPromotionThreat)
    assertNoStandaloneText("PawnAdvance support must not speak under PromotionThreat", advanceSupport)
    assertEquals(LlmNarrationSmoke.check(promotionPlan, promotionRendered, "e7 advances the passed pawn.").accepted, false)

    val stopThreatFacts = BoardFacts.fromFen("r5k1/5n2/8/7P/8/4p3/8/3K4 b - - 0 1").toOption.get
    val stopMove = Line(Square('f', 7), Square('h', 6))
    val blackThreatMove = Line(Square('e', 3), Square('e', 2))
    val stop = strong(ScenePawnStop.write(stopThreatFacts, stopMove).get, 99)
    val blackThreat = strong(ScenePromotionThreat.write(stopThreatFacts, blackThreatMove).get, 90)
    val stopRows = Vector("Scene.PawnStop" -> stop, "Scene.PromotionThreat" -> blackThreat)
    val stopVerdicts = assertStable(stopRows, "Scene.PawnStop")
    val (stopPlan, stopRendered) = assertLeadClaim(stopVerdicts.head, ExplanationClaim.StopsPassedPawnNextAdvance)
    val stopThreatSupport = stopVerdicts.find(_.story == blackThreat).get
    assertNoStandaloneText("PromotionThreat support must not own PawnStop meaning", stopThreatSupport)
    Vector("Nh6 stops promotion.", "Nh6 prevents the pawn from queening.").foreach: output =>
      assertEquals(LlmNarrationSmoke.check(stopPlan, stopRendered, output).accepted, false, output)

    val materialThreatFacts = BoardFacts.fromFen("k7/8/4P3/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val materialThreatMove = Line(Square('e', 6), Square('e', 7))
    val material = strong(SceneMaterial.write(materialThreatFacts, materialMove).get, 90)
    val hanging = strong(TacticHanging.write(materialThreatFacts, materialMove).get, 90)
    val materialThreat = strong(ScenePromotionThreat.write(materialThreatFacts, materialThreatMove).get, 99)

    val defenseThreatFacts = BoardFacts.fromFen("k7/8/4P3/5n1P/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defensePromotionMove = Line(Square('e', 6), Square('e', 7))
    val defense = strong(SceneDefense.write(defenseThreatFacts, defenseThreat, defenseMove).get, 90)
    val defensePromotionThreat = strong(ScenePromotionThreat.write(defenseThreatFacts, defensePromotionMove).get, 99)

    val lineThreatFacts = BoardFacts.fromFen("7k/8/4P1r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val lineMove = Line(Square('d', 3), Square('f', 4))
    val linePromotionMove = Line(Square('e', 6), Square('e', 7))
    val line = strong(
      TacticDiscoveredAttack.write(lineThreatFacts, Some(lineMove), Some(Square('b', 1)), Some(Square('g', 6))).get,
      90
    )
    val linePromotionThreat = strong(ScenePromotionThreat.write(lineThreatFacts, linePromotionMove).get, 99)

    Vector(
      ("Scene.Material", material, materialThreat, ExplanationClaim.MaterialBalanceChanges),
      ("Tactic.Hanging", hanging, materialThreat, ExplanationClaim.CanWinPiece),
      ("Scene.Defense", defense, defensePromotionThreat, ExplanationClaim.DefendsPiece),
      ("Tactic.DiscoveredAttack", line, linePromotionThreat, ExplanationClaim.RevealsAttackOnPiece)
    ).foreach: (label, existing, threat, expectedClaim) =>
      val rows = Vector(label -> existing, "Scene.PromotionThreat" -> threat)
      val verdicts = assertStable(rows, label)
      val existingLead = verdicts.find(_.story == existing).get
      val threatSupport = verdicts.find(_.story == threat).get
      val (plan, rendered) = assertLeadClaim(existingLead, expectedClaim)
      assertNoStandaloneText(s"$label keeps PromotionThreat support silent", threatSupport)
      assertEquals(LlmNarrationSmoke.check(plan, rendered, s"${plan.routeSan.getOrElse("Move")} creates a next-move promotion threat.").accepted, false)

    val cappedThreat =
      ScenePromotionThreat.withEngineCheck(
        promotionThreat,
        engineCheck(advanceThreatFacts, promotionThreat, advanceThreatMove, EngineCheckStatus.Caps)
      ).get
    val refutedThreat =
      ScenePromotionThreat.withEngineCheck(
        promotionThreat,
        engineCheck(advanceThreatFacts, promotionThreat, advanceThreatMove, EngineCheckStatus.Refutes)
      ).get
    val cappedVerdict = StoryTable.choose(Vector(cappedThreat)).head
    val refutedVerdict = StoryTable.choose(Vector(refutedThreat)).head
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertNoStandaloneText("capped PromotionThreat has no standalone text", cappedVerdict)
    assertNoStandaloneText("refuted PromotionThreat has no standalone text", refutedVerdict)

  test("PromotionThreat-6 ExplanationPlan admits only selected uncapped Lead threat claim"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forbiddenClaimKeys =
      Vector(
        "unstoppable_pawn",
        "will_promote",
        "cannot_be_stopped",
        "wins_endgame",
        "converts_advantage",
        "best_move",
        "only_move",
        "forced",
        "tablebase_win",
        "no_counterplay"
      )

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes), false)
    assertEquals(plan.allowedClaim.map(_.key), Some("threatens_promotion_next"))
    assertEquals(ExplanationClaim.PromotionThreatAllowed.map(_.key), Vector("threatens_promotion_next"))
    assertEquals(ExplanationClaim.PromotionThreatForbiddenKeys, forbiddenClaimKeys)
    forbiddenClaimKeys.foreach: forbiddenKey =>
      assertEquals(ExplanationClaim.PromotionThreatAllowed.map(_.key).contains(forbiddenKey), false, forbiddenKey)

    Vector(
      verdict.copy(selected = false),
      verdict.copy(role = Role.Support, leadAllowed = false, rank = 2),
      verdict.copy(role = Role.Context, leadAllowed = false, rank = 3),
      verdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4, engineCheckStatus = Some(EngineCheckStatus.Refutes))
    ).foreach: nonStandalone =>
      assertEquals(ExplanationPlan.fromSelected(nonStandalone), None)

    def checked(status: EngineCheckStatus): Verdict =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(creatingMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 7), Square('e', 8))))),
        evalBefore = Some(EngineEval(30)),
        evalAfter = Some(EngineEval(35)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      StoryTable.choose(Vector(ScenePromotionThreat.withEngineCheck(story, check).get)).head

    val capped = checked(EngineCheckStatus.Caps)
    val refuted = checked(EngineCheckStatus.Refutes)

    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(capped), None)
    assertEquals(ExplanationPlan.fromSelected(refuted), None)

  test("PromotionThreat-7 DeterministicRenderer phrases only bounded next promotion threat"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenOutputs =
      Vector(
        "e7 will promote.",
        "e7 cannot be stopped.",
        "e7 is unstoppable.",
        "e7 wins.",
        "e7 wins the winning endgame.",
        "e7 converts the advantage.",
        "e7 is the best move.",
        "e7 is the only move.",
        "e7 forces promotion.",
        "e7 is a tablebase win.",
        "e7 gives no counterplay."
      )

    assertEquals(rendered.text, "e7 threatens to promote next.")
    assertEquals(rendered.claimKey, "threatens_promotion_next")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    forbiddenOutputs.foreach: output =>
      val candidate = rendered.copy(text = output)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, candidate), None, output)
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector(
      plan.copy(role = Role.Support),
      plan.copy(role = Role.Context),
      plan.copy(role = Role.Blocked, debugOnly = true),
      plan.copy(allowedClaim = None),
      plan.copy(allowedClaim = Some(ExplanationClaim.AdvancesPassedPawn)),
      plan.copy(route = None),
      plan.copy(routeSan = None),
      plan.copy(evidenceLine = None),
      plan.copy(target = None),
      plan.copy(anchor = None),
      plan.copy(forbiddenWording = Vector.empty)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None)

    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assert(!rendererMethodNames.contains("fromVerdict"))
    assert(!rendererMethodNames.contains("fromStory"))
    assert(!rendererMethodNames.contains("fromPromotionThreatProof"))
    Vector("Verdict", "Story", "PromotionThreatProof", "BoardFacts", "EngineCheck").foreach: forbiddenInput =>
      assert(!rendererParameterNames.contains(forbiddenInput), s"renderer must not accept $forbiddenInput")

  test("PromotionThreat-8 LLM smoke reuses 8B boundary without new chess facts"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("e7 threatens to promote next."))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "e7 threatens to promote next.").accepted, true)

    Vector(
      "renderedText: e7 threatens to promote next.",
      "claimKey: threatens_promotion_next",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"LLM smoke prompt must include $required")

    Vector(
      "ExplanationPlan",
      "FEN",
      "PGN",
      "raw Story",
      "Story:",
      "PromotionThreatProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "proofFailures",
      "role:",
      "scene:",
      "target:",
      "route:",
      "evidence line:"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"LLM smoke prompt must not expose $forbiddenInput")

    Vector(
      "raw Story" -> "Raw Story says e7 threatens to promote next.",
      "raw PromotionThreatProof" -> "PromotionThreatProof proves e7.",
      "BoardFacts" -> "BoardFacts show the route.",
      "EngineCheck" -> "EngineCheck supports e7.",
      "raw PV" -> "Raw PV: e7 e8=Q.",
      "proofFailures" -> "proofFailures are empty."
    ).foreach: (label, output) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, label)
      assert(check.violations.contains("raw_input"), label)

    Vector(
      "new move" -> "e7 and Kd2 threaten promotion.",
      "new line" -> "After e7 Kd7, White threatens to promote next.",
      "actual promotion" -> "e7 is an actual promotion.",
      "will promote" -> "e7 will promote.",
      "unstoppable" -> "e7 cannot be stopped.",
      "winning" -> "e7 wins the endgame.",
      "conversion" -> "e7 converts the advantage.",
      "tablebase" -> "e7 is a tablebase win."
    ).foreach: (label, output) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, label)

    val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
    val mismatchedRendered = rendered.copy(claimKey = "will_promote")
    assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, mismatchedRendered), None)

    val llmMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val llmParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromPromotionThreatProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: forbiddenMethod =>
      assert(!llmMethodNames.contains(forbiddenMethod), s"LLM smoke must not expose $forbiddenMethod")

    Vector("Story", "PromotionThreatProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: forbiddenInput =>
      assert(!llmParameterNames.contains(forbiddenInput), s"LLM smoke must not accept $forbiddenInput")

  test("PromotionThreat Closeout hard cleanup keeps ownership layers separated and surfaces closed"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val proof = PromotionThreatProof.fromBoardFacts(facts, creatingMove)
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.nonPromotionCreatingMove, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.pawnOnPenultimateRankAfter, true)
    assertEquals(proof.nextMovePromotionLegal, true)
    assertEquals(proof.creatingMove, Some(creatingMove))
    assertEquals(proof.nextPromotionMove, Some(Line(Square('e', 7), Square('e', 8))))
    assertEquals(proof.promotionSquare, Some(Square('e', 8)))
    assertEquals(proof.promotionRoute, proof.nextPromotionMove)

    assertEquals(story.scene, Scene.PromotionThreat)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePromotionThreat))
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.promotionSquare)
    assertEquals(story.anchor, proof.pawnBefore.map(_.square))
    assertEquals(story.route, proof.creatingMove)
    assertEquals(story.promotionThreatProof, Some(proof))
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.forcing, 0)
    assertEquals(story.proof.kingHeat, 0)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CreatesPromotionThreat))
    assertEquals(ExplanationClaim.PromotionThreatAllowed.map(_.key), Vector("threatens_promotion_next"))
    assertEquals(rendered.text, "e7 threatens to promote next.")
    assertEquals(rendered.claimKey, "threatens_promotion_next")
    assertEquals(rendered.strength, "bounded")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      ExplanationClaim.PawnAdvanceAllowed,
      ExplanationClaim.PawnStopAllowed,
      ExplanationClaim.MaterialAllowed,
      ExplanationClaim.DefenseAllowed,
      ExplanationClaim.HangingAllowed,
      ExplanationClaim.DiscoveredAttackAllowed,
      ExplanationClaim.PinAllowed,
      ExplanationClaim.RemoveGuardAllowed,
      ExplanationClaim.SkewerAllowed
    ).foreach: openedHomeClaims =>
      assertEquals(openedHomeClaims.contains(ExplanationClaim.CreatesPromotionThreat), false)

    val livePositiveClaimKeys = ExplanationClaim.values.map(_.key).toVector
    Vector(
      "will_promote",
      "cannot_be_stopped",
      "unstoppable_pawn",
      "wins_endgame",
      "converts_advantage",
      "tablebase_win",
      "actual_promotion",
      "pawn_break",
      "promotion_story",
      "pawn_race"
    ).foreach: closedClaim =>
      assert(!livePositiveClaimKeys.contains(closedClaim), s"closed PromotionThreat meaning became a live claim key: $closedClaim")

    val storyFieldNames = classOf[Story].getDeclaredFields.map(_.getName).toSet
    Vector(
      "promotionProof",
      "actualPromotionProof",
      "pawnBreakProof",
      "tablebaseProof",
      "conversionProof",
      "winningEndgameProof",
      "unstoppablePawnProof",
      "pawnRaceProof"
    ).foreach: closedProofHome =>
      assert(!storyFieldNames.contains(closedProofHome), s"closed proof home reached Story: $closedProofHome")

    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet
    Vector(
      "actual_promotion",
      "unstoppable_pawn",
      "wins_endgame",
      "converts_advantage",
      "tablebase_draw",
      "stops_promotion",
      "permanently_stops_pawn",
      "pawn_race",
      "king_route",
      "opposition",
      "passed_pawn_strategy"
    ).foreach: forbiddenKey =>
      assert(forbiddenKeys.contains(forbiddenKey), s"closed PromotionThreat wording must remain forbidden only: $forbiddenKey")

    Vector(
      "e7 will promote.",
      "e7 cannot be stopped.",
      "e7 is unstoppable.",
      "e7 converts the advantage.",
      "e7 is a tablebase win.",
      "e7 wins material.",
      "e7 is a pawn break.",
      "e7 wins the endgame.",
      "e7 forces promotion.",
      "e7 creates no counterplay."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    val renderedLower = rendered.text.toLowerCase
    Vector(
      "will promote",
      "cannot be stopped",
      "unstoppable",
      "conversion",
      "tablebase",
      "wins",
      "winning",
      "pawn break",
      "best move",
      "only move",
      "forced",
      "no counterplay"
    ).foreach: forbidden =>
      assert(!renderedLower.contains(forbidden), s"closed PromotionThreat wording leaked to renderer: $forbidden")

    val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val runtimeSourceStream = Files.walk(runtimeRoot)
    val runtimeText =
      try
        runtimeSourceStream
          .iterator()
          .asScala
          .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
          .map(Files.readString)
          .mkString("\n")
      finally runtimeSourceStream.close()
    Vector(
      "PromotionThreat Closeout",
      "Hard Cleanup",
      "one live authority document",
      "public route 200",
      "production API",
      "user-facing LLM"
    ).foreach: closeoutOnlyTerm =>
      assert(!runtimeText.contains(closeoutOnlyTerm), s"closeout-only term became runtime authority: $closeoutOnlyTerm")

  test("PawnStop Closeout hard cleanup keeps ownership layers separated and surfaces closed"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val observation =
      facts.seen.passedPawnObservations.find(row => row.side == Side.White && row.pawn.square == Square('e', 5)).get
    val proof = PawnStopProof.fromBoardFacts(facts, stop)
    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(observation.pawn.man, Man.Pawn)
    assertEquals(observation.side, Side.White)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.targetPawn, Some(observation.pawn))
    assertEquals(proof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(proof.stopMove, Some(stop))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalStopMove, true)
    assertEquals(proof.targetPawnAlreadyPassed, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.nextAdvanceSquareStoppedAfter, true)
    assertEquals(
      proof.nextAdvanceSquareOccupiedAfter || proof.nextAdvanceSquareAttackedAfter || proof.nextAdvanceSquareControlledByPawnAfter,
      true
    )

    assertEquals(story.scene, Scene.PawnStop)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnStop))
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.nextAdvanceSquare)
    assertEquals(story.anchor, Some(stop.from))
    assertEquals(story.route, proof.stopMove)
    assertEquals(story.pawnStopProof, Some(proof))
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.forcing, 0)
    assertEquals(story.proof.kingHeat, 0)

    assertEquals(ExplanationClaim.PawnStopAllowed.map(_.key), Vector("stops_pawn_advance"))
    assertEquals(ExplanationClaim.PawnAdvanceAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.DefenseAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.MaterialAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.HangingAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.DiscoveredAttackAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.PinAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.RemoveGuardAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.SkewerAllowed.map(_.key).contains("stops_pawn_advance"), false)
    Vector(
      "stops_promotion",
      "permanently_stops_pawn",
      "draws_endgame",
      "best_defense",
      "only_move",
      "tablebase_draw",
      "wins_endgame",
      "converts_advantage",
      "forced"
    ).foreach: key =>
      assert(!ExplanationClaim.PawnStopAllowed.map(_.key).contains(key), s"$key must not be a PawnStop live claim")

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.StopsPassedPawnNextAdvance))
    assertEquals(rendered.text, "Ne6 stops the passed pawn from advancing next.")
    assertEquals(rendered.claimKey, "stops_pawn_advance")
    assertEquals(rendered.strength, "bounded")
    val renderedLower = rendered.text.toLowerCase
    Vector("promotion", "promotes", "permanent", "for good", "draw", "tablebase", "best defense", "only move", "wins").foreach:
      forbidden =>
        assert(!renderedLower.contains(forbidden), s"PawnStop rendered text must not contain $forbidden")

    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet
    Vector(
      "stops_promotion",
      "permanently_stops_pawn",
      "draws_endgame",
      "best_defense",
      "only_move",
      "tablebase_draw",
      "wins_endgame",
      "converts_advantage",
      "forced"
    ).foreach: key =>
      assert(forbiddenKeys.contains(key), s"PawnStop closeout must keep $key forbidden")
    Vector(
      "Ne6 stops promotion.",
      "Ne6 stops the pawn for good.",
      "Ne6 draws the endgame.",
      "Ne6 is a tablebase draw.",
      "Ne6 is the best defense.",
      "Ne6 is the only move.",
      "Ne6 wins the endgame."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    val methodNames =
      (DeterministicRenderer.getClass.getDeclaredMethods ++ LlmNarrationSmoke.getClass.getDeclaredMethods).map(_.getName).toSet
    Vector("callApi", "productionApi", "fromPawnStopProof", "fromBoardFacts", "fromEngineCheck").foreach: method =>
      assert(!methodNames.contains(method), s"PawnStop closeout must not expose downstream method $method")

  test("PIH-0 Pawn Interaction Hardening keeps pawn rows below same-board tactic and separated proof homes"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    val sameBoardFacts = BoardFacts.fromFen("7k/8/6r1/4P3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pawnAdvanceMove = Line(Square('e', 5), Square('e', 6))
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val pawnAdvance = ScenePawnAdvance.write(sameBoardFacts, pawnAdvanceMove).get.copy(proof = strongProof(99))
    val discovered =
      TacticDiscoveredAttack
        .write(sameBoardFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(99))

    val sameBoardRows = Vector("Scene.PawnAdvance" -> pawnAdvance, "Tactic.DiscoveredAttack" -> discovered)
    val forward = StoryTable.choose(sameBoardRows.map(_._2))
    val reverse = StoryTable.choose(sameBoardRows.reverse.map(_._2))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict =>
        (
          rowId(sameBoardRows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim)
        )
      )

    assertEquals(shape(forward), shape(reverse))
    assertEquals(forward.find(_.story == discovered).map(_.role), Some(Role.Lead))
    assertEquals(forward.find(_.story == pawnAdvance).map(_.role), Some(Role.Support))
    assertEquals(ExplanationPlan.fromSelected(forward.find(_.story == pawnAdvance).get), None)
    val discoveredPlan = ExplanationPlan.fromSelected(forward.find(_.story == discovered).get).get
    val discoveredRendered = DeterministicRenderer.fromPlan(discoveredPlan).get
    assertEquals(discoveredPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    Vector(
      "Nf4 will promote the pawn.",
      "Nf4 converts the endgame.",
      "Nf4 is the best move.",
      "Nf4 advances the passed pawn."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(discoveredPlan, discoveredRendered, output).accepted, false, output)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val contaminatedAdvance =
      pawnAdvance.copy(pawnStopProof = Some(PawnStopProof.fromBoardFacts(stopFacts, stopMove)))
    val contaminatedStop =
      ScenePawnStop.write(stopFacts, stopMove).get.copy(pawnAdvanceProof = pawnAdvance.pawnAdvanceProof)

    Vector("PawnAdvance with PawnStopProof" -> contaminatedAdvance, "PawnStop with PawnAdvanceProof" -> contaminatedStop).foreach:
      (label, row) =>
        val verdict = StoryTable.choose(Vector(row)).head
        assertEquals(verdict.role, Role.Blocked, label)
        assertEquals(verdict.leadAllowed, false, label)
        assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("PIH-1 fixture map covers pawn interaction hardening categories"):
    final case class PihFixture(
        category: String,
        fen: String,
        sideToMove: Side,
        candidateLegalLines: Vector[Line],
        rows: Vector[(String, Story)],
        expectedOpenRows: Set[String],
        expectedBlockedRows: Set[String],
        expectedRoles: Map[String, Role],
        expectedSelectedVerdict: String,
        expectedSelectedRole: Role,
        forbiddenClaims: Vector[String]
    )

    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story): Story =
      story.copy(proof = strongProof(99))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus,
        before: Int = 20,
        after: Int = 20
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def forbiddenOutput(claim: String, plan: ExplanationPlan): String =
      val san = plan.routeSan.getOrElse("Move")
      claim match
        case "promotion_threat"          => s"$san will promote next."
        case "threatens_promotion_next"  => s"$san will promote next."
        case "stops_promotion"          => s"$san stops promotion."
        case "converts_advantage"       => s"$san converts the advantage."
        case "wins_endgame"             => s"$san wins the endgame."
        case "draws_endgame"            => s"$san draws the endgame."
        case "tablebase_draw"           => s"$san is a tablebase draw."
        case "best_move"                => s"$san is the best move."
        case "only_move"                => s"$san is the only move."
        case "forced"                   => s"$san is forced."
        case "defends_piece"            => s"$san defends the piece."
        case "reveals_attack_on_piece"  => s"$san reveals an attack."
        case "pins_piece"               => s"$san pins the piece."
        case "removes_defender"         => s"$san removes the defender."
        case "skewers_piece_to_piece"   => s"$san skewers the piece."
        case "material_balance_changes" => s"$san changes the material balance."
        case "advances_passed_pawn"     => s"$san advances the passed pawn."
        case "stops_pawn_advance"       => s"$san stops the passed pawn from advancing next."
        case other                      => s"$san adds $other."

    def assertFixture(fixture: PihFixture): Unit =
      val facts = BoardFacts.fromFen(fixture.fen).toOption.get
      assertEquals(facts.sideToMove, fixture.sideToMove, fixture.category)
      fixture.candidateLegalLines.foreach: line =>
        assert(facts.sideLegal.lines.contains(line), s"${fixture.category} legal line missing: $line")
      assertEquals(fixture.rows.map(_._1).toSet, fixture.expectedOpenRows ++ fixture.expectedBlockedRows, fixture.category)

      val forward = StoryTable.choose(fixture.rows.map(_._2))
      val reverse = StoryTable.choose(fixture.rows.reverse.map(_._2))

      def shape(verdicts: Vector[Verdict]) =
        verdicts.map(verdict =>
          (
            rowId(fixture.rows, verdict.story),
            verdict.role,
            verdict.leadAllowed,
            verdict.engineCheckStatus,
            verdict.engineStrengthLimited
          )
        )

      val roleMap = forward.map(verdict => rowId(fixture.rows, verdict.story) -> verdict.role).toMap
      val selected = forward.head
      val selectedId = rowId(fixture.rows, selected.story)

      assertEquals(shape(forward), shape(reverse), fixture.category)
      assertEquals(roleMap, fixture.expectedRoles, fixture.category)
      fixture.expectedOpenRows.foreach: id =>
        assert(roleMap.get(id).exists(_ != Role.Blocked), s"${fixture.category} open row blocked: $id")
      fixture.expectedBlockedRows.foreach: id =>
        assertEquals(roleMap.get(id), Some(Role.Blocked), s"${fixture.category} blocked row role: $id")
      assertEquals(selectedId, fixture.expectedSelectedVerdict, fixture.category)
      assertEquals(selected.role, fixture.expectedSelectedRole, fixture.category)
      assertEquals(forward.count(_.role == Role.Lead), fixture.expectedRoles.values.count(_ == Role.Lead), fixture.category)

      forward.filter(_.role != Role.Lead).foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

      if selected.role == Role.Lead && !selected.engineStrengthLimited then
        val plan = ExplanationPlan.fromSelected(selected).get
        val rendered = DeterministicRenderer.fromPlan(plan).get
        fixture.forbiddenClaims.foreach: claim =>
          val output = forbiddenOutput(claim, plan)
          assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"${fixture.category}: $claim")
      else
        assertEquals(ExplanationPlan.fromSelected(selected), None, fixture.category)

    val pawnAdvanceStopFen = "4k3/8/8/4P3/8/8/8/4K3 w - - 0 1"
    val pawnAdvanceStopFacts = BoardFacts.fromFen(pawnAdvanceStopFen).toOption.get
    val pawnAdvanceStopMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvanceForStopFixture = strong(ScenePawnAdvance.write(pawnAdvanceStopFacts, pawnAdvanceStopMove).get)
    val blockedPawnStopSameBoard =
      pawnAdvanceForStopFixture.copy(scene = Scene.PawnStop, writer = None, pawnAdvanceProof = None)

    val advanceMaterialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"
    val advanceMaterialFacts = BoardFacts.fromFen(advanceMaterialFen).toOption.get
    val advanceMaterialPawnMove = Line(Square('d', 4), Square('d', 5))
    val advanceMaterialCapture = Line(Square('d', 4), Square('e', 5))
    val advanceMaterialPawn = strong(ScenePawnAdvance.write(advanceMaterialFacts, advanceMaterialPawnMove).get)
    val advanceMaterial = strong(SceneMaterial.write(advanceMaterialFacts, advanceMaterialCapture).get)

    val advanceDefenseFen = "4k3/8/8/5n1P/3Q4/8/8/4K3 w - - 0 1"
    val advanceDefenseFacts = BoardFacts.fromFen(advanceDefenseFen).toOption.get
    val advanceDefensePawnMove = Line(Square('h', 5), Square('h', 6))
    val advanceDefenseThreat = Line(Square('f', 5), Square('d', 4))
    val advanceDefenseMove = Line(Square('d', 4), Square('e', 4))
    val advanceDefensePawn = strong(ScenePawnAdvance.write(advanceDefenseFacts, advanceDefensePawnMove).get)
    val advanceDefense = strong(SceneDefense.write(advanceDefenseFacts, advanceDefenseThreat, advanceDefenseMove).get)

    val stopDefenseFen = "4k3/6n1/8/3qP3/5N2/8/8/4K3 b - - 0 1"
    val stopDefenseFacts = BoardFacts.fromFen(stopDefenseFen).toOption.get
    val stopDefenseStopMove = Line(Square('g', 7), Square('e', 6))
    val stopDefenseThreat = Line(Square('f', 4), Square('d', 5))
    val stopDefenseMove = Line(Square('d', 5), Square('c', 6))
    val stopDefensePawnStop = strong(ScenePawnStop.write(stopDefenseFacts, stopDefenseStopMove).get)
    val stopDefense = strong(SceneDefense.write(stopDefenseFacts, stopDefenseThreat, stopDefenseMove).get)

    val stopLineFen = "r5k1/5n2/8/7P/8/8/4N3/4K3 b - - 0 1"
    val stopLineFacts = BoardFacts.fromFen(stopLineFen).toOption.get
    val stopLineStopMove = Line(Square('f', 7), Square('h', 6))
    val stopLinePinMove = Line(Square('a', 8), Square('e', 8))
    val stopLinePawnStop = strong(ScenePawnStop.write(stopLineFacts, stopLineStopMove).get)
    val stopLinePin =
      strong(TacticPin.write(stopLineFacts, Some(stopLinePinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get)

    val engineFen = pawnAdvanceStopFen
    val engineFacts = pawnAdvanceStopFacts
    val engineMove = pawnAdvanceStopMove
    val enginePawnBase = strong(ScenePawnAdvance.write(engineFacts, engineMove).get)
    val engineSupports =
      strong(ScenePawnAdvance.withEngineCheck(enginePawnBase, engineCheck(engineFacts, enginePawnBase, engineMove, EngineCheckStatus.Supports)).get)
    val engineCaps =
      strong(ScenePawnAdvance.withEngineCheck(enginePawnBase, engineCheck(engineFacts, enginePawnBase, engineMove, EngineCheckStatus.Caps)).get)
    val engineRefutes =
      strong(ScenePawnAdvance.withEngineCheck(enginePawnBase, engineCheck(engineFacts, enginePawnBase, engineMove, EngineCheckStatus.Refutes)).get)

    val promotionLookingFen = "4k3/8/4P3/8/8/8/8/4K3 w - - 0 1"
    val promotionLookingFacts = BoardFacts.fromFen(promotionLookingFen).toOption.get
    val promotionLookingMove = Line(Square('e', 6), Square('e', 7))
    val promotionLookingAdvance = strong(ScenePawnAdvance.write(promotionLookingFacts, promotionLookingMove).get)
    val blockedPromotionLooking =
      promotionLookingAdvance.copy(scene = Scene.Pawns, writer = None, pawnAdvanceProof = None)

    val tablebaseLookingFen = "4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1"
    val tablebaseLookingFacts = BoardFacts.fromFen(tablebaseLookingFen).toOption.get
    val tablebaseLookingStopMove = Line(Square('g', 7), Square('e', 6))
    val tablebaseLookingStop = strong(ScenePawnStop.write(tablebaseLookingFacts, tablebaseLookingStopMove).get)
    val blockedTablebase =
      tablebaseLookingStop.copy(scene = Scene.Endgame, writer = None, pawnStopProof = None)

    Vector(
      PihFixture(
        category = "PawnAdvance vs PawnStop",
        fen = pawnAdvanceStopFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(pawnAdvanceStopMove),
        rows = Vector(
          "Scene.PawnAdvance" -> pawnAdvanceForStopFixture,
          "Scene.PawnStop/blocked" -> blockedPawnStopSameBoard
        ),
        expectedOpenRows = Set("Scene.PawnAdvance"),
        expectedBlockedRows = Set("Scene.PawnStop/blocked"),
        expectedRoles = Map("Scene.PawnAdvance" -> Role.Lead, "Scene.PawnStop/blocked" -> Role.Blocked),
        expectedSelectedVerdict = "Scene.PawnAdvance",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("stops_pawn_advance", "promotion_threat", "converts_advantage", "best_move", "only_move")
      ),
      PihFixture(
        category = "PawnAdvance vs Material",
        fen = advanceMaterialFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(advanceMaterialPawnMove, advanceMaterialCapture),
        rows = Vector(
          "Scene.PawnAdvance" -> advanceMaterialPawn,
          "Scene.Material" -> advanceMaterial
        ),
        expectedOpenRows = Set("Scene.PawnAdvance", "Scene.Material"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Material" -> Role.Lead, "Scene.PawnAdvance" -> Role.Support),
        expectedSelectedVerdict = "Scene.Material",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("advances_passed_pawn", "promotion_threat", "converts_advantage", "best_move")
      ),
      PihFixture(
        category = "PawnAdvance vs Defense",
        fen = advanceDefenseFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(advanceDefensePawnMove, advanceDefenseMove),
        rows = Vector(
          "Scene.PawnAdvance" -> advanceDefensePawn,
          "Scene.Defense" -> advanceDefense
        ),
        expectedOpenRows = Set("Scene.PawnAdvance", "Scene.Defense"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Defense" -> Role.Lead, "Scene.PawnAdvance" -> Role.Support),
        expectedSelectedVerdict = "Scene.Defense",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("advances_passed_pawn", "promotion_threat", "converts_advantage", "best_move", "only_move")
      ),
      PihFixture(
        category = "PawnStop vs Defense",
        fen = stopDefenseFen,
        sideToMove = Side.Black,
        candidateLegalLines = Vector(stopDefenseStopMove, stopDefenseMove),
        rows = Vector(
          "Scene.PawnStop" -> stopDefensePawnStop,
          "Scene.Defense" -> stopDefense
        ),
        expectedOpenRows = Set("Scene.PawnStop", "Scene.Defense"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Defense" -> Role.Lead, "Scene.PawnStop" -> Role.Support),
        expectedSelectedVerdict = "Scene.Defense",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("stops_pawn_advance", "stops_promotion", "draws_endgame", "tablebase_draw", "best_move", "only_move")
      ),
      PihFixture(
        category = "PawnStop vs Line/Defender tactic",
        fen = stopLineFen,
        sideToMove = Side.Black,
        candidateLegalLines = Vector(stopLineStopMove, stopLinePinMove),
        rows = Vector(
          "Scene.PawnStop" -> stopLinePawnStop,
          "Tactic.Pin" -> stopLinePin
        ),
        expectedOpenRows = Set("Scene.PawnStop", "Tactic.Pin"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.Pin" -> Role.Lead, "Scene.PawnStop" -> Role.Support),
        expectedSelectedVerdict = "Tactic.Pin",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("stops_pawn_advance", "stops_promotion", "draws_endgame", "tablebase_draw", "best_move", "only_move")
      ),
      PihFixture(
        category = "Pawn row vs EngineCheck Supports/Caps/Refutes",
        fen = engineFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(engineMove),
        rows = Vector(
          "Scene.PawnAdvance#Supports" -> engineSupports,
          "Scene.PawnAdvance#Caps" -> engineCaps,
          "Scene.PawnAdvance#Refutes" -> engineRefutes
        ),
        expectedOpenRows = Set("Scene.PawnAdvance#Supports", "Scene.PawnAdvance#Caps"),
        expectedBlockedRows = Set("Scene.PawnAdvance#Refutes"),
        expectedRoles = Map(
          "Scene.PawnAdvance#Supports" -> Role.Lead,
          "Scene.PawnAdvance#Caps" -> Role.Support,
          "Scene.PawnAdvance#Refutes" -> Role.Blocked
        ),
        expectedSelectedVerdict = "Scene.PawnAdvance#Supports",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("promotion_threat", "converts_advantage", "wins_endgame", "best_move", "only_move")
      ),
      PihFixture(
        category = "Promotion-looking but no PromotionThreat yet",
        fen = promotionLookingFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(promotionLookingMove),
        rows = Vector(
          "Scene.PawnAdvance" -> promotionLookingAdvance,
          "promotion-looking/blocked" -> blockedPromotionLooking
        ),
        expectedOpenRows = Set("Scene.PawnAdvance"),
        expectedBlockedRows = Set("promotion-looking/blocked"),
        expectedRoles = Map("Scene.PawnAdvance" -> Role.Lead, "promotion-looking/blocked" -> Role.Blocked),
        expectedSelectedVerdict = "Scene.PawnAdvance",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("promotion_threat", "converts_advantage", "wins_endgame", "best_move", "only_move")
      ),
      PihFixture(
        category = "tablebase-looking but no tablebase authority",
        fen = tablebaseLookingFen,
        sideToMove = Side.Black,
        candidateLegalLines = Vector(tablebaseLookingStopMove),
        rows = Vector(
          "Scene.PawnStop" -> tablebaseLookingStop,
          "endgame-result/blocked" -> blockedTablebase
        ),
        expectedOpenRows = Set("Scene.PawnStop"),
        expectedBlockedRows = Set("endgame-result/blocked"),
        expectedRoles = Map("Scene.PawnStop" -> Role.Lead, "endgame-result/blocked" -> Role.Blocked),
        expectedSelectedVerdict = "Scene.PawnStop",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector("draws_endgame", "tablebase_draw", "wins_endgame", "best_move", "only_move", "forced")
      )
    ).foreach(assertFixture)

  test("PIH-2 Role Stability keeps pawn rows deterministic without duplicate pawn claims"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story): Story =
      story.copy(proof = strongProof(99))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    def assertNoStandaloneClaim(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def leadPlan(story: Story): (Verdict, ExplanationPlan, RenderedLine) =
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      (verdict, plan, rendered)

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get)

    val defenseFacts = BoardFacts.fromFen("4k3/6n1/8/3qP3/5N2/8/8/4K3 b - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 4), Square('d', 5))
    val defenseMove = Line(Square('d', 5), Square('c', 6))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get)

    val incompleteAdvance = pawnAdvance.copy(pawnAdvanceProof = None)
    val incompleteStop = pawnStop.copy(pawnStopProof = None)
    val refutedAdvance =
      ScenePawnAdvance.withEngineCheck(pawnAdvance, engineCheck(advanceFacts, pawnAdvance, advanceMove, EngineCheckStatus.Refutes)).get
    val refutedStop =
      ScenePawnStop.withEngineCheck(pawnStop, engineCheck(stopFacts, pawnStop, stopMove, EngineCheckStatus.Refutes)).get
    val cappedAdvance =
      ScenePawnAdvance.withEngineCheck(pawnAdvance, engineCheck(advanceFacts, pawnAdvance, advanceMove, EngineCheckStatus.Caps)).get

    val rows = Vector(
      "Scene.Defense" -> defense,
      "Scene.PawnAdvance" -> pawnAdvance,
      "Scene.PawnStop" -> pawnStop,
      "Scene.PawnAdvance/incomplete" -> incompleteAdvance,
      "Scene.PawnStop/incomplete" -> incompleteStop,
      "Scene.PawnAdvance/refuted" -> refutedAdvance,
      "Scene.PawnStop/refuted" -> refutedStop,
      "Scene.PawnAdvance/capped" -> cappedAdvance
    )
    val permutations =
      Vector(
        rows,
        rows.reverse,
        Vector(rows(5), rows(2), rows(7), rows(0), rows(4), rows(1), rows(6), rows(3))
      )
    val baseline = StoryTable.choose(rows.map(_._2))
    val baselineShape = shape(rows, baseline)

    permutations.foreach: ordered =>
      val verdicts = StoryTable.choose(ordered.map(_._2))
      assertEquals(rowId(rows, verdicts.head.story), "Scene.Defense")
      assertEquals(shape(rows, verdicts), baselineShape)

    assertEquals(baseline.count(_.role == Role.Lead), 1)
    assertEquals(baseline.find(_.story == incompleteAdvance).map(_.role), Some(Role.Blocked))
    assertEquals(baseline.find(_.story == incompleteStop).map(_.role), Some(Role.Blocked))
    assertEquals(baseline.find(_.story == refutedAdvance).map(_.role), Some(Role.Blocked))
    assertEquals(baseline.find(_.story == refutedStop).map(_.role), Some(Role.Blocked))
    assertEquals(baseline.find(_.story == cappedAdvance).map(_.role), Some(Role.Support))
    Vector(incompleteAdvance, incompleteStop, refutedAdvance, refutedStop, cappedAdvance).foreach: row =>
      assertNoStandaloneClaim(row.toString, StoryTable.choose(Vector(row)).head)

    val samePawnRows = Vector("Scene.PawnAdvance" -> pawnAdvance, "Scene.PawnStop" -> pawnStop)
    val samePawnForward = StoryTable.choose(samePawnRows.map(_._2))
    val samePawnReverse = StoryTable.choose(samePawnRows.reverse.map(_._2))
    val samePawnAdvanceVerdict = samePawnForward.find(_.story == pawnAdvance).get
    val samePawnStopVerdict = samePawnForward.find(_.story == pawnStop).get

    assertEquals(shape(samePawnRows, samePawnForward), shape(samePawnRows, samePawnReverse))
    assertEquals(samePawnForward.count(_.role == Role.Lead), 1)
    assertEquals(samePawnAdvanceVerdict.role, Role.Lead)
    assertEquals(samePawnStopVerdict.role, Role.Support)
    assertNoStandaloneClaim("same-pawn PawnStop support", samePawnStopVerdict)

    assertEquals(StoryTable.choose(Vector(pawnAdvance, pawnAdvance.copy(anchor = pawnAdvance.anchor))).count(_.role == Role.Lead), 1)
    assertEquals(StoryTable.choose(Vector(pawnStop, pawnStop.copy(anchor = pawnStop.anchor))).count(_.role == Role.Lead), 1)

    val defenseCollision = StoryTable.choose(Vector(pawnStop, defense))
    val defenseLead = defenseCollision.find(_.story == defense).get
    val pawnStopSupport = defenseCollision.find(_.story == pawnStop).get
    assertEquals(defenseLead.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(defenseLead).flatMap(_.allowedClaim), Some(ExplanationClaim.DefendsPiece))
    assertEquals(pawnStopSupport.role, Role.Support)
    assertNoStandaloneClaim("PawnStop must not own Defense claim", pawnStopSupport)

    val (_, advancePlan, advanceRendered) = leadPlan(pawnAdvance)
    assertEquals(advancePlan.allowedClaim, Some(ExplanationClaim.AdvancesPassedPawn))
    Vector("e6 will promote next.", "e6 converts the advantage.", "e6 wins the endgame.").foreach: output =>
      assertEquals(LlmNarrationSmoke.check(advancePlan, advanceRendered, output).accepted, false, output)

    val (_, stopPlan, stopRendered) = leadPlan(pawnStop)
    assertEquals(stopPlan.allowedClaim, Some(ExplanationClaim.StopsPassedPawnNextAdvance))
    assertEquals(stopPlan.allowedClaim.exists(_ == ExplanationClaim.DefendsPiece), false)
    assertEquals(LlmNarrationSmoke.check(stopPlan, stopRendered, "Ne6 defends the piece.").accepted, false)

  test("PIH-3 Meaning Ownership Boundary keeps pawn collision rows on their own claims"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story): Story =
      story.copy(proof = strongProof(99))

    def leadPlan(story: Story): (Verdict, ExplanationPlan, RenderedLine) =
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      (verdict, plan, rendered)

    def assertOwnsOnly(
        label: String,
        story: Story,
        expectedClaim: ExplanationClaim,
        rejectedOutputs: Vector[String]
    ): ExplanationPlan =
      val (verdict, plan, rendered) = leadPlan(story)
      assertEquals(verdict.role, Role.Lead, label)
      assertEquals(plan.allowedClaim, Some(expectedClaim), label)
      rejectedOutputs.foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"$label: $output")
      plan

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get)
    assert(pawnAdvance.pawnAdvanceProof.exists(proof =>
      proof.alreadyPassedBefore &&
        proof.legalOneStepNonCaptureNonPromotion &&
        proof.afterBoardPassedPawn
    ))
    assertOwnsOnly(
      "Scene.PawnAdvance",
      pawnAdvance,
      ExplanationClaim.AdvancesPassedPawn,
      Vector(
        "e6 will promote.",
        "e6 is unstoppable.",
        "e6 converts the advantage.",
        "e6 wins the endgame.",
        "e6 is the only move."
      )
    )

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get)
    assert(pawnStop.pawnStopProof.exists(proof =>
      proof.legalStopMove &&
        proof.targetPawnAlreadyPassed &&
        proof.nextAdvanceSquareNonPromotion &&
        proof.nextAdvanceSquareStoppedAfter
    ))
    assertOwnsOnly(
      "Scene.PawnStop",
      pawnStop,
      ExplanationClaim.StopsPassedPawnNextAdvance,
      Vector(
        "Ne6 stops promotion.",
        "Ne6 draws the endgame.",
        "Ne6 holds the endgame.",
        "Ne6 defends the piece.",
        "Ne6 is the only move."
      )
    )

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialCapture = Line(Square('d', 4), Square('e', 5))
    val material = strong(SceneMaterial.write(materialFacts, materialCapture).get)
    assert(material.captureResult.exists(_.positiveMaterial))
    assertOwnsOnly(
      "Scene.Material",
      material,
      ExplanationClaim.MaterialBalanceChanges,
      Vector(
        "dxe5 advances the passed pawn.",
        "dxe5 stops the passed pawn from advancing next.",
        "dxe5 will promote.",
        "dxe5 converts the advantage."
      )
    )

    val hanging = strong(TacticHanging.write(materialFacts, materialCapture).get)
    assert(hanging.captureResult.exists(result => result.positiveMaterial && result.targetPiece.exists(_.man != Man.Pawn)))
    assertOwnsOnly(
      "Tactic.Hanging",
      hanging,
      ExplanationClaim.CanWinPiece,
      Vector(
        "dxe5 advances the passed pawn.",
        "dxe5 stops the passed pawn from advancing next.",
        "dxe5 will promote.",
        "dxe5 converts the advantage."
      )
    )

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get)
    assert(defense.threatProof.exists(_.complete))
    assert(defense.defenseProof.exists(_.complete))
    assertOwnsOnly(
      "Scene.Defense",
      defense,
      ExplanationClaim.DefendsPiece,
      Vector(
        "Qe4+ advances the passed pawn.",
        "Qe4+ stops promotion.",
        "Qe4+ converts the advantage.",
        "Qe4+ is the only move."
      )
    )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      strong(TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6))).get)
    assertOwnsOnly(
      "Tactic.DiscoveredAttack",
      discovered,
      ExplanationClaim.RevealsAttackOnPiece,
      Vector(
        "Nf4 pins the piece.",
        "Nf4 removes the defender.",
        "Nf4 advances the passed pawn.",
        "Nf4 wins material."
      )
    )

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      strong(TacticPin.write(pinFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7))).get)
    assertOwnsOnly(
      "Tactic.Pin",
      pin,
      ExplanationClaim.PinsPiece,
      Vector(
        "Nf4 reveals an attack.",
        "Nf4 removes the defender.",
        "Nf4 advances the passed pawn.",
        "Nf4 wins material."
      )
    )

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      strong(TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5))).get)
    assertOwnsOnly(
      "Tactic.RemoveGuard",
      removeGuard,
      ExplanationClaim.RemovesDefender,
      Vector(
        "Nxe5 pins the piece.",
        "Nxe5 reveals an attack.",
        "Nxe5 advances the passed pawn.",
        "Nxe5 wins material."
      )
    )

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerLine = Line(Square('a', 1), Square('e', 1))
    val skewer =
      strong(TacticSkewer.write(skewerFacts, Some(skewerLine), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get)
    assertOwnsOnly(
      "Tactic.Skewer",
      skewer,
      ExplanationClaim.SkewersPieceToPiece,
      Vector(
        "Re1 pins the piece.",
        "Re1 removes the defender.",
        "Re1 advances the passed pawn.",
        "Re1 wins material."
      )
    )

    val defenseCollision = StoryTable.choose(Vector(pawnStop, defense))
    val defenseVerdict = defenseCollision.find(_.story == defense).get
    val pawnStopVerdict = defenseCollision.find(_.story == pawnStop).get
    assertEquals(defenseVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(defenseVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.DefendsPiece))
    assertEquals(pawnStopVerdict.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(pawnStopVerdict), None)

  test("PIH-4 EngineCheck Interaction reuses existing pawn statuses without engine-owned claims"):
    final case class PawnEngineFixture(
        label: String,
        facts: BoardFacts,
        story: Story,
        line: Line,
        reply: Line,
        expectedClaim: ExplanationClaim,
        attach: (Story, EngineCheck) => Option[Story]
    )

    def engineCheck(fixture: PawnEngineFixture, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = fixture.facts,
        story = Some(fixture.story),
        engineLine = Some(EngineLine(Vector(fixture.line))),
        replyLine = Some(EngineLine(Vector(fixture.reply))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def selected(story: Story): Verdict =
      StoryTable.choose(Vector(story)).head

    def assertNoEnginePublicValues(label: String, verdict: Verdict): Unit =
      assertEquals(verdict.values.contains(1234.0), false, label)
      assertEquals(verdict.values.contains(1240.0), false, label)

    def assertRejectsEngineSpeech(label: String, plan: ExplanationPlan, rendered: RenderedLine): Unit =
      Vector(
        "The engine says this is best.",
        "Raw PV: e6 Ke7 proves it.",
        "+12.34 is the public eval.",
        "1234 centipawns proves the move.",
        "This is the best move.",
        "This is the only move.",
        "This is a tablebase-like claim.",
        "This is a tablebase draw."
      ).foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"$label: $output")

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceStory = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val stopStory = ScenePawnStop.write(stopFacts, stopLine).get

    Vector(
      PawnEngineFixture(
        label = "Scene.PawnAdvance",
        facts = advanceFacts,
        story = advanceStory,
        line = advanceLine,
        reply = Line(Square('e', 8), Square('e', 7)),
        expectedClaim = ExplanationClaim.AdvancesPassedPawn,
        attach = ScenePawnAdvance.withEngineCheck
      ),
      PawnEngineFixture(
        label = "Scene.PawnStop",
        facts = stopFacts,
        story = stopStory,
        line = stopLine,
        reply = Line(Square('e', 1), Square('e', 2)),
        expectedClaim = ExplanationClaim.StopsPassedPawnNextAdvance,
        attach = ScenePawnStop.withEngineCheck
      )
    ).foreach: fixture =>
      val baseVerdict = selected(fixture.story)
      val basePlan = ExplanationPlan.fromSelected(baseVerdict).get
      val baseRendered = DeterministicRenderer.fromPlan(basePlan).get
      val supports = fixture.attach(fixture.story, engineCheck(fixture, EngineCheckStatus.Supports)).get
      val caps = fixture.attach(fixture.story, engineCheck(fixture, EngineCheckStatus.Caps)).get
      val refutes = fixture.attach(fixture.story, engineCheck(fixture, EngineCheckStatus.Refutes)).get
      val unknown = fixture.attach(fixture.story, engineCheck(fixture, EngineCheckStatus.Unknown)).get

      val supportsVerdict = selected(supports)
      val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
      val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
      val capsVerdict = selected(caps)
      val refutesVerdict = selected(refutes)
      val unknownVerdict = selected(unknown)
      val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get
      val unknownRendered = DeterministicRenderer.fromPlan(unknownPlan).get

      assertEquals(basePlan.allowedClaim, Some(fixture.expectedClaim), fixture.label)
      assertEquals(supportsVerdict.role, Role.Lead, fixture.label)
      assertEquals(supportsPlan.allowedClaim, basePlan.allowedClaim, fixture.label)
      assertEquals(supportsRendered.text, baseRendered.text, fixture.label)
      assertRejectsEngineSpeech(s"${fixture.label} Supports", supportsPlan, supportsRendered)

      assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps), fixture.label)
      assertEquals(capsVerdict.engineStrengthLimited, true, fixture.label)
      assertEquals(ExplanationPlan.fromSelected(capsVerdict), None, fixture.label)
      assertEquals(DeterministicRenderer.fromPlan(basePlan.copy(allowedClaim = None)), None, fixture.label)

      assertEquals(refutesVerdict.role, Role.Blocked, fixture.label)
      assertEquals(refutesVerdict.leadAllowed, false, fixture.label)
      assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), fixture.label)
      assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None, fixture.label)

      assertEquals(unknownVerdict.role, Role.Lead, fixture.label)
      assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown), fixture.label)
      assertEquals(unknownPlan.allowedClaim, basePlan.allowedClaim, fixture.label)
      assertEquals(unknownRendered.text, baseRendered.text, fixture.label)
      assertRejectsEngineSpeech(s"${fixture.label} Unknown", unknownPlan, unknownRendered)

      Vector(baseVerdict, supportsVerdict, capsVerdict, refutesVerdict, unknownVerdict).foreach: verdict =>
        assertNoEnginePublicValues(fixture.label, verdict)

  test("PIH-5 Negative Corpus keeps close pawn false positives silent"):
    def selected(story: Story): Verdict =
      StoryTable.choose(Vector(story)).head

    def assertSilent(label: String, story: Story): Unit =
      val verdict = selected(story)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus,
        before: Int = 20,
        after: Int = 20,
        freshness: Option[Int] = Some(0)
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = freshness,
        requestedStatus = status
      )

    val notPassedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val notPassedAdvance = Line(Square('e', 5), Square('e', 6))
    val notPassedProof = PawnAdvanceProof.fromBoardFacts(notPassedFacts, notPassedAdvance)
    assertEquals(notPassedProof.legalOneStepNonCaptureNonPromotion, true)
    assertEquals(notPassedProof.alreadyPassedBefore, false)
    assertEquals(notPassedProof.complete, false)
    assertEquals(ScenePawnAdvance.write(notPassedFacts, notPassedAdvance), None, "pawn advances but was not passed")

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceStory = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val noAfterReplay =
      advanceStory.copy(pawnAdvanceProof =
        advanceStory.pawnAdvanceProof.map(_.copy(exactAfterBoardReplay = false, missingEvidence = Vector.empty))
      )
    assertSilent("passed pawn advances but no exact after-board proof", noAfterReplay)

    val routeMismatch =
      advanceStory.copy(route = Some(Line(Square('e', 5), Square('e', 7))))
    assertSilent("route mismatch", routeMismatch)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val safeNextAdvanceMove = Line(Square('g', 7), Square('f', 5))
    val safeNextAdvanceProof = PawnStopProof.fromBoardFacts(stopFacts, safeNextAdvanceMove)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareOccupiedAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareAttackedAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareControlledByPawnAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareStoppedAfter, false)
    assertEquals(ScenePawnStop.write(stopFacts, safeNextAdvanceMove), None, "next square still available")

    val stopStory = ScenePawnStop.write(stopFacts, stopLine).get
    assertSilent("long-term blockade claim", stopStory.copy(plan = Some(Plan.PasserBlock)))

    val promotionLookingFacts = BoardFacts.fromFen("4k3/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionLookingLine = Line(Square('e', 6), Square('e', 7))
    val promotionLookingAdvance = ScenePawnAdvance.write(promotionLookingFacts, promotionLookingLine).get
    val promotionPlan = ExplanationPlan.fromSelected(selected(promotionLookingAdvance)).get
    val promotionRendered = DeterministicRenderer.fromPlan(promotionPlan).get
    Vector("e7 will promote next.", "e7 is unstoppable.", "e7 converts the advantage.").foreach: output =>
      assertEquals(LlmNarrationSmoke.check(promotionPlan, promotionRendered, output).accepted, false, output)

    assertSilent("tablebase-looking position", stopStory.copy(scene = Scene.Endgame, pawnStopProof = None))
    assertSilent("king opposition-looking position", stopStory.copy(scene = Scene.Endgame, plan = Some(Plan.KingConvert), pawnStopProof = None))
    assertSilent("pawn race-looking position", promotionLookingAdvance.copy(scene = Scene.Pawns, plan = Some(Plan.Race), pawnAdvanceProof = None))

    val wrongBoard = BoardFacts.fromFen("4k3/8/8/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val wrongBoardCheck = engineCheck(wrongBoard, advanceStory, advanceLine, EngineCheckStatus.Supports)
    val staleCheck = engineCheck(advanceFacts, advanceStory, advanceLine, EngineCheckStatus.Supports, freshness = Some(2))
    val routeMismatchCheck =
      EngineCheck.fromStory(
        facts = advanceFacts,
        story = Some(advanceStory),
        engineLine = Some(EngineLine(Vector(Line(Square('e', 5), Square('e', 7))))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(20)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    Vector("wrong board" -> wrongBoardCheck, "stale board" -> staleCheck, "route mismatch check" -> routeMismatchCheck).foreach:
      (label, check) =>
        assertEquals(check.status, EngineCheckStatus.Unknown, label)
        assertEquals(ScenePawnAdvance.withEngineCheck(advanceStory, check), None, label)

    val refutingCheck =
      engineCheck(advanceFacts, advanceStory, advanceLine, EngineCheckStatus.Supports, before = 220, after = 20)
    val refutedStory = ScenePawnAdvance.withEngineCheck(advanceStory, refutingCheck).get
    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    assertSilent("engine refutes plausible pawn row", refutedStory)

  test("PIH-6 Downstream Boundary Smoke sends only selected Lead pawn Verdicts to text stages"):
    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1240)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def assertDownstreamBoundary(
        label: String,
        leadStory: Story,
        supportStory: Story,
        contextStory: Story,
        blockedStory: Story,
        cappedStory: Story,
        refutedStory: Story,
        expectedClaim: ExplanationClaim
    ): Unit =
      val rows =
        Vector(
          "Lead" -> leadStory,
          "Support" -> supportStory,
          "Context" -> contextStory,
          "Blocked" -> blockedStory,
          "Capped" -> cappedStory,
          "Refuted" -> refutedStory
        )
      val verdicts = StoryTable.choose(rows.map(_._2))
      val byId = verdicts.map(verdict => rows.collectFirst { case (id, story) if story == verdict.story => id }.get -> verdict).toMap
      assertEquals(byId("Lead").role, Role.Lead, label)
      assertEquals(byId("Support").role, Role.Support, label)
      assertEquals(byId("Context").role, Role.Context, label)
      assertEquals(byId("Blocked").role, Role.Blocked, label)
      assertEquals(byId("Capped").engineStrengthLimited, true, label)
      assertEquals(byId("Refuted").engineCheckStatus, Some(EngineCheckStatus.Refutes), label)

      val leadPlan = ExplanationPlan.fromSelected(byId("Lead")).get
      val rendered = DeterministicRenderer.fromPlan(leadPlan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get
      assertEquals(leadPlan.allowedClaim, Some(expectedClaim), label)
      assertEquals(rendered.claimKey, expectedClaim.key, label)
      assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."), label)
      Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
        assert(prompt.contains(field), s"$label prompt missing $field")
      Vector(
        "BoardFacts",
        "Story(",
        "Verdict(",
        "EngineCheck",
        "EngineEval",
        "EngineLine",
        "proofFailures",
        "sourceRow",
        "FEN",
        "raw PV",
        "routeSan"
      ).foreach: forbiddenInput =>
        assertEquals(prompt.contains(forbiddenInput), false, s"$label prompt leaked $forbiddenInput")

      Vector(
        ForbiddenWording.PromotionThreat,
        ForbiddenWording.UnstoppablePawn,
        ForbiddenWording.WinningEndgame,
        ForbiddenWording.ConvertsAdvantage,
        ForbiddenWording.DrawsEndgame,
        ForbiddenWording.TablebaseDraw,
        ForbiddenWording.BestMove,
        ForbiddenWording.OnlyMove,
        ForbiddenWording.Forced,
        ForbiddenWording.CreatesPressure,
        ForbiddenWording.TakesInitiative
      ).foreach: forbidden =>
        assert(leadPlan.forbiddenWording.contains(forbidden), s"$label missing forbidden wording ${forbidden.key}")

      Vector("Support", "Context", "Blocked", "Capped", "Refuted").foreach: id =>
        assertEquals(ExplanationPlan.fromSelected(byId(id)), None, s"$label $id plan")
        assertEquals(ExplanationPlan.fromSelected(byId(id)).flatMap(DeterministicRenderer.fromPlan), None, s"$label $id render")

      Vector(
        "will promote",
        "is unstoppable",
        "wins the endgame",
        "is winning",
        "conversion",
        "draws the endgame",
        "holds the endgame",
        "tablebase",
        "is the best move",
        "is the only move",
        "is forced",
        "creates pressure",
        "takes the initiative"
      ).foreach: phrase =>
        val output = s"${rendered.text} It $phrase."
        assertEquals(LlmNarrationSmoke.check(leadPlan, rendered, output).accepted, false, s"$label: $phrase")

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceLead = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val advanceSupport = advanceLead.copy(proof = advanceLead.proof.copy(clarity = 99))
    val advanceContext = advanceLead.copy(proof = advanceLead.proof.copy(boardProof = 20))
    val advanceBlocked =
      advanceLead.copy(pawnAdvanceProof =
        advanceLead.pawnAdvanceProof.map(_.copy(exactAfterBoardReplay = false, missingEvidence = Vector.empty))
      )
    val advanceCapped =
      ScenePawnAdvance.withEngineCheck(advanceLead, engineCheck(advanceFacts, advanceLead, advanceLine, EngineCheckStatus.Caps)).get
    val advanceRefuted =
      ScenePawnAdvance.withEngineCheck(advanceLead, engineCheck(advanceFacts, advanceLead, advanceLine, EngineCheckStatus.Refutes)).get
    assertDownstreamBoundary(
      "Scene.PawnAdvance",
      advanceLead,
      advanceSupport,
      advanceContext,
      advanceBlocked,
      advanceCapped,
      advanceRefuted,
      ExplanationClaim.AdvancesPassedPawn
    )

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val stopLead = ScenePawnStop.write(stopFacts, stopLine).get
    val stopSupport = stopLead.copy(proof = stopLead.proof.copy(clarity = 99))
    val stopContext = stopLead.copy(proof = stopLead.proof.copy(boardProof = 20))
    val stopBlocked =
      stopLead.copy(pawnStopProof =
        stopLead.pawnStopProof.map(_.copy(nextAdvanceSquareStoppedAfter = false, missingEvidence = Vector.empty))
      )
    val stopCapped =
      ScenePawnStop.withEngineCheck(stopLead, engineCheck(stopFacts, stopLead, stopLine, EngineCheckStatus.Caps)).get
    val stopRefuted =
      ScenePawnStop.withEngineCheck(stopLead, engineCheck(stopFacts, stopLead, stopLine, EngineCheckStatus.Refutes)).get
    assertDownstreamBoundary(
      "Scene.PawnStop",
      stopLead,
      stopSupport,
      stopContext,
      stopBlocked,
      stopCapped,
      stopRefuted,
      ExplanationClaim.StopsPassedPawnNextAdvance
    )

  test("PIH-7 Diagnostics Boundary keeps pawn diagnostics out of public meaning"):
    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1240)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def assertDiagnosticBoundary(
        label: String,
        story: Story,
        blockedStory: Story,
        cappedStory: Story,
        refutedStory: Story,
        expectedClaim: ExplanationClaim
    ): Unit =
      val blockedVerdict = StoryTable.choose(Vector(blockedStory)).head
      assertEquals(blockedVerdict.role, Role.Blocked, label)
      assertEquals(blockedVerdict.proofFailures.nonEmpty, true, label)
      assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None, label)
      assertEquals(DeterministicRenderer.fromPlan(ExplanationPlan(
        role = Role.Blocked,
        scene = story.scene,
        tactic = None,
        side = story.side,
        target = story.target,
        anchor = story.anchor,
        route = story.route,
        routeSan = story.routeSan,
        secondaryTarget = None,
        allowedClaim = Some(expectedClaim),
        evidenceLine = story.route,
        strength = ExplanationStrength.Bounded,
        forbiddenWording = Vector(ForbiddenWording.EngineSays),
        relations = Vector(ExplanationRelation.BlockedByEngineRefute),
        debugOnly = true,
        supportContextLinks = Vector.empty
      )), None, label)

      Vector(
        "PawnAdvanceProof",
        "PawnStopProof",
        "exact after-board replay",
        "missing evidence",
        "same-board proof",
        "EngineCheck",
        "EngineLine",
        "EngineEval",
        "1234",
        "1240"
      ).foreach: diagnostic =>
        assertEquals(blockedVerdict.values.mkString(" ").contains(diagnostic), false, s"$label Verdict.values leaked $diagnostic")

      val cappedVerdict = StoryTable.choose(Vector(cappedStory)).head
      assertEquals(cappedVerdict.engineStrengthLimited, true, label)
      assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None, label)

      val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
      assertEquals(refutedVerdict.role, Role.Blocked, label)
      assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), label)
      assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None, label)

      val leadPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
      val rendered = DeterministicRenderer.fromPlan(leadPlan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get
      Vector(
        "proofFailures",
        "missing evidence",
        "exact after-board replay",
        "same-board proof",
        "EngineCheck",
        "EngineLine",
        "EngineEval",
        "StoryTable",
        "debug relation",
        "blocked_by_engine_refute",
        "capped_same_story"
      ).foreach: diagnostic =>
        assertEquals(prompt.contains(diagnostic), false, s"$label prompt leaked $diagnostic")

      val relationInjectedPlan =
        leadPlan.copy(relations =
          Vector(
            ExplanationRelation.BlockedByEngineRefute,
            ExplanationRelation.CappedSameStory,
            ExplanationRelation.SameFamilyLowerRank
          )
        )
      val relationRendered = DeterministicRenderer.fromPlan(relationInjectedPlan).get
      Vector("blocked_by_engine_refute", "capped_same_story", "same_family_lower_rank", "debug relation", "StoryTable").foreach:
        diagnostic =>
          assertEquals(relationRendered.text.contains(diagnostic), false, s"$label renderer leaked $diagnostic")

      Vector(
        s"${rendered.text} Missing evidence: exact after-board replay.",
        s"${rendered.text} StoryTable debug relation: blocked_by_engine_refute.",
        s"${rendered.text} EngineCheck text says +12.34.",
        s"${rendered.text} capped_same_story proves the row.",
        s"${rendered.text} proofFailures show same-board proof failed."
      ).foreach: output =>
        assertEquals(LlmNarrationSmoke.check(leadPlan, rendered, output).accepted, false, s"$label accepted diagnostic output: $output")

      val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
      val runtimeSourceStream = Files.walk(runtimeRoot)
      val runtimeText =
        try
          runtimeSourceStream
            .iterator()
            .asScala
            .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
            .map(Files.readString)
            .mkString("\n")
        finally runtimeSourceStream.close()
      Vector(
        "PIH-",
        "fixture map",
        "negative corpus",
        "Pawn Interaction Hardening"
      ).foreach: helperOnlyTerm =>
        assert(!runtimeText.contains(helperOnlyTerm), s"$label test helper term became runtime authority: $helperOnlyTerm")

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceStory = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val advanceBlocked =
      advanceStory.copy(
        pawnAdvanceProof =
          advanceStory.pawnAdvanceProof.map(_.copy(exactAfterBoardReplay = false, missingEvidence = Vector.empty)),
        storyProof = StoryProof.empty
      )
    assertDiagnosticBoundary(
      "Scene.PawnAdvance",
      advanceStory,
      advanceBlocked,
      ScenePawnAdvance.withEngineCheck(advanceStory, engineCheck(advanceFacts, advanceStory, advanceLine, EngineCheckStatus.Caps)).get,
      ScenePawnAdvance.withEngineCheck(advanceStory, engineCheck(advanceFacts, advanceStory, advanceLine, EngineCheckStatus.Refutes)).get,
      ExplanationClaim.AdvancesPassedPawn
    )

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val stopStory = ScenePawnStop.write(stopFacts, stopLine).get
    val stopBlocked =
      stopStory.copy(
        pawnStopProof =
          stopStory.pawnStopProof.map(_.copy(nextAdvanceSquareStoppedAfter = false, missingEvidence = Vector.empty)),
        storyProof = StoryProof.empty
      )
    assertDiagnosticBoundary(
      "Scene.PawnStop",
      stopStory,
      stopBlocked,
      ScenePawnStop.withEngineCheck(stopStory, engineCheck(stopFacts, stopStory, stopLine, EngineCheckStatus.Caps)).get,
      ScenePawnStop.withEngineCheck(stopStory, engineCheck(stopFacts, stopStory, stopLine, EngineCheckStatus.Refutes)).get,
      ExplanationClaim.StopsPassedPawnNextAdvance
    )

  test("PIH Closeout Hard Cleanup keeps pawn interaction authority separated and surfaces closed"):
    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceObservation =
      advanceFacts.seen.passedPawnObservations.find(row => row.side == Side.White && row.pawn.square == Square('e', 5)).get
    val advanceProof = PawnAdvanceProof.fromBoardFacts(advanceFacts, advanceLine)
    val advanceStory = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val advancePlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(advanceStory)).head).get
    val advanceRendered = DeterministicRenderer.fromPlan(advancePlan).get

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val stopObservation =
      stopFacts.seen.passedPawnObservations.find(row => row.side == Side.White && row.pawn.square == Square('e', 5)).get
    val stopProof = PawnStopProof.fromBoardFacts(stopFacts, stopLine)
    val stopStory = ScenePawnStop.write(stopFacts, stopLine).get
    val stopPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(stopStory)).head).get
    val stopRendered = DeterministicRenderer.fromPlan(stopPlan).get

    assertEquals(advanceObservation.pawn, advanceProof.pawnBefore.get)
    assertEquals(advanceObservation.pawn, stopObservation.pawn)
    assertEquals(stopProof.targetPawn, Some(stopObservation.pawn))
    assertEquals(advanceProof.publicClaimAllowed, false)
    assertEquals(stopProof.publicClaimAllowed, false)

    assertEquals(advanceStory.scene, Scene.PawnAdvance)
    assertEquals(advanceStory.writer, Some(StoryWriter.ScenePawnAdvance))
    assertEquals(advanceStory.pawnAdvanceProof, Some(advanceProof))
    assertEquals(advanceStory.pawnStopProof, None)
    assertEquals(advanceStory.threatProof, None)
    assertEquals(advanceStory.defenseProof, None)
    assertEquals(advanceStory.captureResult, None)
    assertEquals(advanceStory.lineProof, None)
    assertEquals(advanceStory.pinProof, None)
    assertEquals(advanceStory.removeGuardProof, None)
    assertEquals(advanceStory.skewerProof, None)
    assertEquals(advancePlan.allowedClaim, Some(ExplanationClaim.AdvancesPassedPawn))
    assertEquals(advanceRendered.claimKey, "advances_passed_pawn")

    assertEquals(stopStory.scene, Scene.PawnStop)
    assertEquals(stopStory.writer, Some(StoryWriter.ScenePawnStop))
    assertEquals(stopStory.pawnStopProof, Some(stopProof))
    assertEquals(stopStory.pawnAdvanceProof, None)
    assertEquals(stopStory.threatProof, None)
    assertEquals(stopStory.defenseProof, None)
    assertEquals(stopStory.captureResult, None)
    assertEquals(stopStory.lineProof, None)
    assertEquals(stopStory.pinProof, None)
    assertEquals(stopStory.removeGuardProof, None)
    assertEquals(stopStory.skewerProof, None)
    assertEquals(stopPlan.allowedClaim, Some(ExplanationClaim.StopsPassedPawnNextAdvance))
    assertEquals(stopRendered.claimKey, "stops_pawn_advance")

    assertEquals(ExplanationClaim.PawnAdvanceAllowed.map(_.key), Vector("advances_passed_pawn"))
    assertEquals(ExplanationClaim.PawnStopAllowed.map(_.key), Vector("stops_pawn_advance"))
    assertEquals(ExplanationClaim.PawnAdvanceAllowed.intersect(ExplanationClaim.PawnStopAllowed), Vector.empty)
    Vector(
      ExplanationClaim.MaterialAllowed,
      ExplanationClaim.DefenseAllowed,
      ExplanationClaim.HangingAllowed,
      ExplanationClaim.DiscoveredAttackAllowed,
      ExplanationClaim.PinAllowed,
      ExplanationClaim.RemoveGuardAllowed,
      ExplanationClaim.SkewerAllowed
    ).foreach: openedHomeClaims =>
      assertEquals(openedHomeClaims.contains(ExplanationClaim.AdvancesPassedPawn), false)
      assertEquals(openedHomeClaims.contains(ExplanationClaim.StopsPassedPawnNextAdvance), false)

    assertEquals(
      StoryWriter.values.toVector,
      Vector(
        StoryWriter.TacticHanging,
        StoryWriter.TacticFork,
        StoryWriter.SceneMaterial,
        StoryWriter.SceneDefense,
        StoryWriter.TacticDiscoveredAttack,
        StoryWriter.TacticPin,
        StoryWriter.TacticRemoveGuard,
        StoryWriter.TacticSkewer,
        StoryWriter.ScenePawnAdvance,
        StoryWriter.ScenePawnStop,
        StoryWriter.ScenePromotionThreat
      )
    )
    val livePositiveClaimKeys = ExplanationClaim.values.map(_.key).toVector
    Vector(
      "promotion",
      "pawn_break",
      "tablebase",
      "tablebase_draw",
      "pawn_race",
      "king_route",
      "opposition",
      "unstoppable",
      "conversion",
      "winning_endgame",
      "draws_endgame"
    ).foreach: closedClaim =>
      assert(!livePositiveClaimKeys.contains(closedClaim), s"closed pawn meaning became a live claim key: $closedClaim")

    val storyFieldNames = classOf[Story].getDeclaredFields.map(_.getName).toSet
    Vector(
      "promotionProof",
      "pawnBreakProof",
      "tablebaseProof",
      "pawnRaceProof",
      "kingRouteProof",
      "oppositionProof",
      "conversionProof",
      "winningEndgameProof"
    ).foreach: closedProofHome =>
      assert(!storyFieldNames.contains(closedProofHome), s"closed proof home reached Story: $closedProofHome")

    val pawnForbiddenKeys = (advancePlan.forbiddenWording ++ stopPlan.forbiddenWording).map(_.key).toSet
    Vector("promotion_threat", "tablebase_draw", "pawn_race", "king_route", "opposition").foreach: forbiddenKey =>
      assert(pawnForbiddenKeys.contains(forbiddenKey), s"closed meaning must remain forbidden wording only: $forbiddenKey")

    Vector(advanceRendered.text, stopRendered.text).foreach: text =>
      val lowered = text.toLowerCase
      Vector(
        "will promote",
        "promotion",
        "pawn break",
        "tablebase",
        "pawn race",
        "king route",
        "opposition",
        "unstoppable",
        "conversion",
        "winning",
        "draw"
      ).foreach: forbidden =>
        assert(!lowered.contains(forbidden), s"closed pawn wording leaked to renderer: $forbidden")

    val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val runtimeSourceStream = Files.walk(runtimeRoot)
    val runtimeText =
      try
        runtimeSourceStream
          .iterator()
          .asScala
          .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
          .map(Files.readString)
          .mkString("\n")
      finally runtimeSourceStream.close()
    Vector(
      "PIH Closeout",
      "Pawn Interaction Hardening",
      "fixture map",
      "negative corpus",
      "tablebase authority",
      "public route 200",
      "production API",
      "user-facing LLM"
    ).foreach: closeoutOnlyTerm =>
      assert(!runtimeText.contains(closeoutOnlyTerm), s"closeout-only term became runtime authority: $closeoutOnlyTerm")

  test("Board Facts legal rows cannot become public claims without Story Proof"):
    val facts = BoardFacts.fromFen(Fen.initial).toOption.get
    val legalRow = facts.seen.legalMoves.head
    val story = Story(
      Scene.Material,
      side = legalRow.side,
      target = Some(legalRow.line.to),
      anchor = Some(legalRow.piece.square),
      route = Some(legalRow.line),
      rival = rivalOf(legalRow.side),
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99
      )
    )
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(
      story.proofFailures,
      Vector(BoardFacts.MissingEvidence("Story Proof", Vector("legal line", "same-board proof")))
    )
    assertEquals(verdict.proofFailures, story.proofFailures)
    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("Board Facts observations cannot speak without Story Proof"):
    def highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99,
        forcing = 99,
        kingHeat = 99,
        immediacy = 99
      )

    def claimLikeStory(side: Side, target: Square, anchor: Square, route: Line, rival: Side): Story =
      Story(
        Scene.Material,
        side = side,
        target = Some(target),
        anchor = Some(anchor),
        route = Some(route),
        rival = rival,
        proof = highProof
      )

    def cannotSpeak(label: String, boardFactExists: Boolean, story: Story): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(boardFactExists, true, s"$label Board Fact must exist")
      assert(story.proof.publicStrength >= 65, s"$label must use a high Proof score")
      assertEquals(
        story.proofFailures,
        Vector(BoardFacts.MissingEvidence("Story Proof", Vector("legal line", "same-board proof"))),
        label
      )
      assertEquals(verdict.proofFailures, story.proofFailures, label)
      assertEquals(verdict.leadAllowed, false, label)
      assert(verdict.role != Role.Lead, label)

    val contactFacts =
      BoardFacts.fromFen(Fen.Full("4k3/8/8/4n3/3P4/2N5/1P6/4K3 w - - 0 1")).toOption.get
    val contactSeen = contactFacts.seen
    val whitePawn = Piece(Side.White, Man.Pawn, Square('d', 4))
    val whiteKnight = Piece(Side.White, Man.Knight, Square('c', 3))
    val whiteGuard = Piece(Side.White, Man.Pawn, Square('b', 2))
    val blackKnight = Piece(Side.Black, Man.Knight, Square('e', 5))
    val attacked = contactSeen.attacks.find(row => row.attacker == whitePawn && row.target == blackKnight).get
    val guarded = contactSeen.guards.find(row => row.guard == whiteGuard && row.target == whiteKnight).get
    val loose = contactSeen.pieceContacts.find(row => row.piece == blackKnight && row.attackedUnguarded).get

    cannotSpeak(
      "attacked piece",
      boardFactExists = contactSeen.attacks.contains(attacked),
      claimLikeStory(
        attacked.attacker.side,
        attacked.target.square,
        attacked.attacker.square,
        Line(attacked.attacker.square, attacked.target.square),
        attacked.target.side
      )
    )
    cannotSpeak(
      "guarded piece",
      boardFactExists = contactSeen.guards.contains(guarded),
      claimLikeStory(
        guarded.guard.side,
        guarded.target.square,
        guarded.guard.square,
        Line(guarded.guard.square, guarded.target.square),
        rivalOf(guarded.guard.side)
      )
    )
    cannotSpeak(
      "loose unguarded piece",
      boardFactExists = contactSeen.pieceContacts.contains(loose),
      claimLikeStory(
        loose.attackers.head.side,
        loose.piece.square,
        loose.attackers.head.square,
        Line(loose.attackers.head.square, loose.piece.square),
        loose.piece.side
      )
    )

    val pinSeen = BoardFacts.fromFen(Fen.Full("4r1k1/8/8/8/8/8/4N3/4K3 w - - 0 1")).toOption.get.seen
    val pinLine = pinSeen.lineFacts.find(_.shapes.contains(BoardFacts.LineShape.PinToKing)).get
    cannotSpeak(
      "pin-to-king line",
      boardFactExists = pinSeen.lineFacts.contains(pinLine),
      claimLikeStory(
        pinLine.attacker.get.side,
        pinLine.pinned.get.square,
        pinLine.attacker.get.square,
        pinLine.line,
        pinLine.pinned.get.side
      )
    )

    val xraySeen =
      BoardFacts.fromFen(Fen.Full("7k/8/q7/2b5/8/B7/8/R6K w - - 0 1")).toOption.get.seen
    val xrayLine = xraySeen.lineFacts.find(_.shapes.contains(BoardFacts.LineShape.XRay)).get
    cannotSpeak(
      "x-ray shape",
      boardFactExists = xraySeen.lineFacts.contains(xrayLine),
      claimLikeStory(
        xrayLine.from.get.side,
        xrayLine.target.get.square,
        xrayLine.from.get.square,
        xrayLine.line,
        xrayLine.target.get.side
      )
    )

    val openFileSeen = BoardFacts.fromFen(Fen.Full("4k3/8/8/8/8/8/R7/4K3 w - - 0 1")).toOption.get.seen
    val openFileEntry = openFileSeen.fileFacts.find(_.file == 2).get.rookOpenFileEntries.head
    cannotSpeak(
      "open file entry",
      boardFactExists = openFileSeen.fileFacts.exists(_.rookOpenFileEntries.contains(openFileEntry)),
      claimLikeStory(
        openFileEntry.side,
        openFileEntry.line.to,
        openFileEntry.piece.square,
        openFileEntry.line,
        rivalOf(openFileEntry.side)
      )
    )

    val pawnSeen =
      BoardFacts.fromFen(Fen.Full("7k/8/8/3pnp2/1P2P3/2P2N2/P7/4K3 w - - 0 1")).toOption.get.seen
    val pawnLever = pawnSeen.pawnLevers.find(_.pawn.square == Square('e', 4)).get
    val pawnSafe = pawnSeen.pawnSafeSquareObservations.find(_.square == Square('f', 3)).get
    cannotSpeak(
      "pawn lever",
      boardFactExists = pawnSeen.pawnLevers.contains(pawnLever),
      claimLikeStory(
        pawnLever.side,
        pawnLever.target.square,
        pawnLever.pawn.square,
        pawnLever.line,
        pawnLever.target.side
      )
    )
    cannotSpeak(
      "pawn-safe square",
      boardFactExists = pawnSeen.pawnSafeSquareObservations.contains(pawnSafe),
      claimLikeStory(
        pawnSafe.side,
        pawnSafe.square,
        pawnSafe.square,
        Line(pawnSafe.square, pawnSafe.square),
        pawnSafe.by
      )
    )

    val kingSeen =
      BoardFacts.fromFen(Fen.Full("4r1k1/6br/8/8/8/8/4N1R1/4K3 b - - 0 1")).toOption.get.seen
    val kingRingAttack = kingSeen.kingRingAttacks.find(_.square == Square('g', 7)).get
    val legalEscape = kingSeen.legalEscapeSquares.find(_.square == Square('h', 8)).get
    val contactCheck = kingSeen.contactCheckObservations.head
    cannotSpeak(
      "king-ring attack",
      boardFactExists = kingSeen.kingRingAttacks.contains(kingRingAttack),
      claimLikeStory(
        kingRingAttack.attacker.side,
        kingRingAttack.square,
        kingRingAttack.attacker.square,
        Line(kingRingAttack.attacker.square, kingRingAttack.square),
        kingRingAttack.side
      )
    )
    cannotSpeak(
      "legal escape square",
      boardFactExists = kingSeen.legalEscapeSquares.contains(legalEscape),
      claimLikeStory(
        legalEscape.side,
        legalEscape.square,
        legalEscape.king.square,
        legalEscape.line,
        rivalOf(legalEscape.side)
      )
    )
    cannotSpeak(
      "contact check observation",
      boardFactExists = kingSeen.contactCheckObservations.contains(contactCheck),
      claimLikeStory(
        contactCheck.attacker.side,
        contactCheck.king.square,
        contactCheck.attacker.square,
        contactCheck.line,
        contactCheck.side
      )
    )

  test("Story route endpoints encode differently and order deterministically"):
    val forward = Story(
      Scene.Material,
      side = Side.White,
      target = Some(Square('d', 4)),
      anchor = Some(Square('e', 5)),
      route = Some(Line(Square('a', 2), Square('a', 4))),
      rival = Side.Black,
      proof = proof(boardProof = 82, ownerProof = 82, anchorProof = 82, routeProof = 82, conversionPrize = 82),
      storyProof = storyProof(Line(Square('a', 2), Square('a', 4)))
    )
    val reverse = forward.copy(
      route = Some(Line(Square('a', 4), Square('a', 2))),
      storyProof = StoryProof.empty
    )

    assertEquals(
      forward.values(Story.Slots.Pawn + Story.Identity.RouteFrom),
      Square('a', 2).index + 1
    )
    assertEquals(
      reverse.values(Story.Slots.Pawn + Story.Identity.RouteFrom),
      Square('a', 4).index + 1
    )
    assert(forward.values != reverse.values)
    assertEquals(StoryTable.choose(Vector(reverse, forward)).head.story, forward)
    assertEquals(StoryTable.choose(Vector(forward, reverse)).head.story, forward)

  test("Verdict values encode the fixed dense layout and all proof values"):
    val target = Square('f', 7)
    val anchor = Square('e', 6)
    val exactProof = Proof(
      boardProof = 61,
      lineProof = 62,
      ownerProof = 63,
      anchorProof = 64,
      routeProof = 65,
      persistence = 66,
      immediacy = 67,
      forcing = 68,
      conversionPrize = 69,
      counterplayRisk = 10,
      kingHeat = 71,
      pieceSupport = 72,
      pawnSupport = 73,
      sourceFit = 74,
      novelty = 75,
      clarity = 76
    )
    val story = Story(
      Scene.Tactic,
      side = Side.Black,
      target = Some(target),
      anchor = Some(anchor),
      rival = Side.White,
      tactic = Some(Tactic.Fork),
      proof = exactProof
    )
    val verdict = Verdict(story, rank = 2, leadAllowed = false, strength = 87.5, role = Role.Blocked)
    val values = verdict.values

    assertEquals(Verdict.Size, 100)
    assertEquals(values.size, Verdict.Size)
    assertEquals(Verdict.Slots.Role, 0)
    assertEquals(Verdict.Slots.Rank, 1)
    assertEquals(Verdict.Slots.LeadAllowed, 2)
    assertEquals(Verdict.Slots.Strength, 3)
    assertEquals(Verdict.Slots.Side, 4)
    assertEquals(Verdict.Slots.Rival, 5)
    assertEquals(Verdict.Slots.Target, 6)
    assertEquals(Verdict.Slots.Anchor, 7)
    assertEquals(Verdict.Slots.Scene, 8)
    assertEquals(Verdict.Slots.Plan, 27)
    assertEquals(Verdict.Slots.Tactic, 59)
    assertEquals(Verdict.Slots.Proof, 84)
    assertEquals(Verdict.Slots.End, Verdict.Size)
    assertEqualsDouble(values(Verdict.Slots.Role), Role.Blocked.ordinal.toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Rank), 2.0, 0.0)
    assertEqualsDouble(values(Verdict.Slots.LeadAllowed), 0.0, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Strength), 87.5, 0.0001)
    assertEqualsDouble(values(Verdict.Slots.Side), Side.Black.ordinal.toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Rival), Side.White.ordinal.toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Target), (target.index + 1).toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Anchor), (anchor.index + 1).toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Scene + Scene.Tactic.ordinal), 1.0, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Tactic + Tactic.Fork.ordinal), 1.0, 0.0)
    assertEquals(values.slice(Verdict.Slots.Proof, Verdict.Slots.End).map(_.toInt), exactProof.values)
    assert(values.exists(_ != 0.0))

  test("Verdict proofFailures are internal diagnostics, not public payload"):
    val story = Story(
      Scene.Material,
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99
      )
    )
    val verdict = StoryTable.choose(Vector(story)).head
    val cleared = verdict.copy(proofFailures = Vector.empty)
    val verdictSlotNames =
      Verdict.Slots.getClass.getDeclaredMethods
        .map(_.getName)
        .filterNot(name => name.startsWith("$") || name.contains("bitmap"))
        .toVector
    val debugTerms = Vector("proofFailures", "MissingEvidence", "missing evidence", "renderer", "prompt")

    assert(verdict.proofFailures.nonEmpty)
    assertEquals(verdict.values, cleared.values)
    debugTerms.foreach: term =>
      assert(
        !verdictSlotNames.exists(_.toLowerCase.contains(term.toLowerCase)),
        s"Verdict public slots must not expose $term"
      )

  test("Stage 5-4 Verdict diagnostics do not enter public values"):
    val story = Story(
      Scene.Tactic,
      side = Side.White,
      target = Some(Square('e', 5)),
      anchor = Some(Square('d', 4)),
      route = Some(Line(Square('d', 4), Square('e', 5))),
      rival = Side.Black,
      tactic = Some(Tactic.Hanging),
      proof = proof(),
      storyProof = storyProof()
    )
    val base =
      Verdict(story, rank = 1, leadAllowed = true, strength = 80.0, role = Role.Lead)
    val withProofFailure =
      base.copy(proofFailures = Vector(BoardFacts.MissingEvidence("Story Proof", Vector("same-board proof"))))
    val withUnknownEngine =
      base.copy(engineCheckStatus = Some(EngineCheckStatus.Unknown))
    val withRefutingEngine =
      base.copy(engineCheckStatus = Some(EngineCheckStatus.Refutes))
    val withStrengthLimit =
      base.copy(engineCheckStatus = Some(EngineCheckStatus.Caps), engineStrengthLimited = true)

    assertEquals(base.values.size, Verdict.Size)
    assertEquals(withProofFailure.values, base.values)
    assertEquals(withUnknownEngine.values, base.values)
    assertEquals(withRefutingEngine.values, base.values)
    assertEquals(withStrengthLimit.values, base.values)

  test("Stage 6-1 builds structured ExplanationPlan for selected Hanging Lead Verdict"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head

    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Hanging))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 5)))
    assertEquals(plan.anchor, Some(Square('d', 4)))
    assertEquals(plan.route, Some(capture))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(plan.allowedClaim.map(_.key), Some("can_win_piece"))
    assert(!plan.allowedClaim.exists(_.key.contains(" ")))
    assertEquals(plan.evidenceLine, Some(capture))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(
      plan.forbiddenWording.map(_.key),
      Vector(
        "free_piece",
        "blunder",
        "winning",
        "decisive",
        "forced",
        "best_move",
        "only_move",
        "engine_says",
        "no_counterplay",
        "king_unsafe",
        "file_control",
        "outpost",
        "strategic_key",
        "conversion",
        "mate_net"
      )
    )
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)

  test("Stage 6-1 ExplanationPlan rejects unselected or unsupported Verdicts"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val supportVerdict = StoryTable.choose(Vector(left, right))(1)
    val forgedForkLead = Verdict(
      story = right.copy(tactic = Some(Tactic.Fork)),
      rank = 1,
      leadAllowed = true,
      strength = 80.0,
      role = Role.Lead
    )
    val publicSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(supportVerdict.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(forgedForkLead), None)
    Vector("sentence", "prose", "engineCheck", "EngineEval", "proofFailures").foreach: forbiddenName =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 6-2 maps Hanging Lead to safe allowed claim keys"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val allowedClaimKeys = ExplanationClaim.HangingAllowed.map(_.key)
    val forbiddenClaimKeys = ExplanationClaim.HangingForbiddenKeys

    assertEquals(
      allowedClaimKeys,
      Vector("can_win_piece", "piece_can_be_taken_with_gain", "capture_leaves_material_gain")
    )
    assertEquals(
      forbiddenClaimKeys,
      Vector(
        "free_piece",
        "blunder",
        "winning_tactic",
        "decisive_tactic",
        "forced_win",
        "best_move",
        "no_counterplay",
        "engine_approved"
      )
    )
    assert(plan.allowedClaim.exists(claim => allowedClaimKeys.contains(claim.key)))
    assert(!plan.allowedClaim.exists(claim => forbiddenClaimKeys.contains(claim.key)))

  test("Stage 6-2 only Lead Verdict produces an allowed claim plan"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)

    assert(ExplanationPlan.fromSelected(leadVerdict).exists(plan => plan.allowedClaim.contains(ExplanationClaim.CanWinPiece)))
    Vector(supportVerdict, contextVerdict, blockedVerdict).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None)

  test("Stage 6-2 engineStrengthLimited suppresses claim and strengthens forbidden wording"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val plainVerdict = StoryTable.choose(Vector(story)).head
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val capped = TacticHanging.withEngineCheck(story, check).get
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    val plainPlan = ExplanationPlan.fromSelected(plainVerdict).get
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get

    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(plainPlan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(cappedPlan.allowedClaim, None)
    assert(!plainPlan.forbiddenWording.map(_.key).contains("strong_wording"))
    assert(cappedPlan.forbiddenWording.map(_.key).contains("strong_wording"))

  test("Stage 6-3 gives ExplanationPlan a stronger forbidden wording boundary"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forbiddenKeys = plan.forbiddenWording.map(_.key)

    assertEquals(
      ForbiddenWording.Basic.map(_.key),
      Vector(
        "free_piece",
        "blunder",
        "winning",
        "decisive",
        "forced",
        "best_move",
        "only_move",
        "engine_says",
        "no_counterplay",
        "king_unsafe",
        "file_control",
        "outpost",
        "strategic_key",
        "conversion",
        "mate_net"
      )
    )
    assertEquals(forbiddenKeys, ForbiddenWording.Basic.map(_.key))
    assert(forbiddenKeys.size > ExplanationClaim.HangingAllowed.size)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(plan.strength, ExplanationStrength.Bounded)

  test("Stage 6-3 engineStrengthLimited adds strong wording prohibition without a claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val capped = TacticHanging.withEngineCheck(story, check).get
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get

    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(cappedPlan.allowedClaim, None)
    assertEquals(
      cappedPlan.forbiddenWording.map(_.key),
      ForbiddenWording.Basic.map(_.key) :+ "strong_wording"
    )

  test("Stage 6-4 keeps Support and Context as relation-only plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val supportPlan = ExplanationPlan.fromSelected(supportVerdict).get
    val contextPlan = ExplanationPlan.fromSelected(contextVerdict).get

    assertEquals(ExplanationPlan.fromSelected(leadVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.CanWinPiece))
    assertEquals(supportPlan.allowedClaim, None)
    assertEquals(supportPlan.relations.map(_.key), Vector("same_family_lower_rank"))
    assertEquals(supportPlan.debugOnly, false)
    assertEquals(contextPlan.allowedClaim, None)
    assertEquals(contextPlan.relations.map(_.key), Vector("alternative_hanging_candidate"))
    assertEquals(contextPlan.debugOnly, false)

  test("Stage 6-4 records capped and engine-refuted relations without proofFailure wording"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get

    def checked(status: EngineCheckStatus) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      TacticHanging.withEngineCheck(story, check).get

    val cappedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(checked(EngineCheckStatus.Caps))).head).get
    val blockedVerdict = StoryTable.choose(Vector(checked(EngineCheckStatus.Refutes))).head.copy(
      proofFailures = Vector(BoardFacts.MissingEvidence("Story Proof", Vector("must never become wording")))
    )
    val blockedPlan = ExplanationPlan.fromSelected(blockedVerdict).get
    val publicSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(cappedPlan.allowedClaim, None)
    assertEquals(cappedPlan.relations.map(_.key), Vector("capped_same_story"))
    assertEquals(blockedPlan.allowedClaim, None)
    assertEquals(blockedPlan.relations.map(_.key), Vector("blocked_by_engine_refute"))
    assertEquals(blockedPlan.debugOnly, true)
    Vector("proofFailures", "must never become wording", "debugText").foreach: forbiddenName =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 6-5 ExplanationPlan accepts selected Verdict only"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val selectedVerdict = StoryTable.choose(Vector(story)).head
    val unselectedVerdict = selectedVerdict.copy(selected = false)

    assertEquals(selectedVerdict.selected, true)
    assert(ExplanationPlan.fromSelected(selectedVerdict).exists(_.allowedClaim.contains(ExplanationClaim.CanWinPiece)))
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(selectedVerdict.values, unselectedVerdict.values)

  test("Stage 6-5 ExplanationPlan exposes no raw proof material input"):
    val fromSelectedMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    val fromSelectedParameterNames =
      fromSelectedMethods
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getName)
        .mkString(" ")
    val planSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)), Vector(Vector("Verdict")))
    Vector(
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "MultiTargetProof",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "Story",
      "String",
      "Source"
    ).foreach: forbiddenType =>
      assert(!fromSelectedParameterNames.contains(forbiddenType), s"ExplanationPlan must not accept $forbiddenType")
    Vector(
      "boardFacts",
      "boardMood",
      "rootAtoms",
      "captureResult",
      "engineCheck",
      "engineEval",
      "engineLine",
      "rawPv",
      "proofFailures",
      "sourceRow"
    ).foreach: forbiddenName =>
      assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 6 closeout negative corpus creates no public claim outside uncapped Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)

    def checked(status: EngineCheckStatus) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(right),
        engineLine = Some(EngineLine(Vector(Line(Square('d', 4), Square('e', 5))))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      StoryTable.choose(Vector(TacticHanging.withEngineCheck(right, check).get)).head

    val cappedVerdict = checked(EngineCheckStatus.Caps)
    val refutedVerdict = checked(EngineCheckStatus.Refutes)
    val negativePlans =
      Vector(supportVerdict, contextVerdict, blockedVerdict, cappedVerdict, refutedVerdict)
        .flatMap(ExplanationPlan.fromSelected)

    assertEquals(ExplanationPlan.fromSelected(leadVerdict).flatMap(_.allowedClaim), Some(ExplanationClaim.CanWinPiece))
    assertEquals(negativePlans.map(_.allowedClaim), Vector.fill(negativePlans.size)(None))
    assert(negativePlans.exists(_.relations.contains(ExplanationRelation.CappedSameStory)))
    assert(negativePlans.exists(_.relations.contains(ExplanationRelation.BlockedByEngineRefute)))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict).map(_.debugOnly), Some(true))

  test("Stage 6 closeout runtime surface stays pre-render"):
    val fromSelectedMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    val planSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)), Vector(Vector("Verdict")))
    Vector(
      "render",
      "renderer",
      "llm",
      "sentence",
      "prose",
      "publicRoute",
      "pedagogy",
      "engineExplanation",
      "bestMove",
      "engineEval",
      "captureResult",
      "boardFacts"
    ).foreach: forbiddenName =>
      assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 7-1 DeterministicRenderer accepts ExplanationPlan only"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector
    val fromPlanParameterNames =
      fromPlanMethods
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet

    assert(rendered.text.nonEmpty)
    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
    assertEquals(fromPlanParameterNames, Vector("ExplanationPlan"))
    Vector(
      "fromVerdict",
      "fromStory",
      "fromBoardFacts",
      "fromBoardMood",
      "fromCaptureResult",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine"
    ).foreach: forbiddenMethod =>
      assert(!rendererMethodNames.contains(forbiddenMethod), s"DeterministicRenderer must not expose $forbiddenMethod")

  test("Stage 7-1 DeterministicRenderer cannot create text without an ExplanationPlan"):
    val rendererNoArgTextMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter: method =>
        method.getParameterTypes.isEmpty && method.getReturnType.getSimpleName.contains("RenderedLine")
    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
        .mkString(" ")
    val textSurfaceNames =
      classOf[RenderedLine].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RenderedLine].getDeclaredFields.map(_.getName).toSet

    assertEquals(rendererNoArgTextMethods, Vector.empty)
    Vector(
      "Verdict",
      "Story",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine"
    ).foreach: forbiddenType =>
      assert(!rendererParameterNames.contains(forbiddenType), s"DeterministicRenderer must not accept $forbiddenType")
    Vector(
      "verdict",
      "story",
      "boardFacts",
      "boardMood",
      "captureResult",
      "engineCheck",
      "engineEval",
      "engineLine",
      "rawPv",
      "proofFailures",
      "sourceRow"
    ).foreach: forbiddenName =>
      assert(!textSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 7-2 renders only the minimal CanWinPiece Hanging template"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases =
      Vector(
        "free piece",
        "blunder",
        "winning",
        "decisive",
        "forced",
        "best move",
        "only move",
        "engine says",
        "no counterplay",
        "king unsafe",
        "file control",
        "outpost"
      )

    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.route, Some(capture))
    assertEquals(plan.routeSan, Some("dxe5"))
    assertEquals(plan.evidenceLine, Some(capture))
    assertEquals(plan.target, Some(Square('e', 5)))
    assert(plan.forbiddenWording.nonEmpty)
    assertEquals(rendered.text, "dxe5 wins material against the piece on e5.")
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"template must not contain forbidden phrase: $phrase")

  test("Stage 7-2 refuses missing or non CanWinPiece template prerequisites"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val invalidPlans =
      Vector(
        plan.copy(role = Role.Support),
        plan.copy(allowedClaim = Some(ExplanationClaim.PieceCanBeTakenWithGain)),
        plan.copy(allowedClaim = None),
        plan.copy(debugOnly = true),
        plan.copy(route = None),
        plan.copy(routeSan = None),
        plan.copy(evidenceLine = None),
        plan.copy(target = None),
        plan.copy(forbiddenWording = Vector.empty)
      )

    invalidPlans.foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None)

  test("Stage 7-3 enforces forbidden wording before renderer output"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val lowerText = rendered.text.toLowerCase
    val forbiddenMeanings =
      Vector(
        "free piece",
        "blunder",
        "winning position",
        "decisive",
        "forced",
        "best move",
        "only move",
        "engine says",
        "no counterplay",
        "king unsafe",
        "file control",
        "outpost",
        "strategic key",
        "conversion",
        "mate net"
      )
    val strongBlockedPlan =
      plan.copy(forbiddenWording = plan.forbiddenWording :+ ForbiddenWording.StrongWording)

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assert(lowerText.contains("wins material"))
    forbiddenMeanings.foreach: phrase =>
      assert(!lowerText.contains(phrase), s"renderer output must not contain forbidden phrase: $phrase")
    assertEquals(DeterministicRenderer.fromPlan(strongBlockedPlan), None)

  test("Stage 7-3 refuses engine-limited no-claim and debug-only plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val cappedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(TacticHanging.withEngineCheck(story, check).get)).head).get

    assertEquals(cappedPlan.allowedClaim, None)
    assert(cappedPlan.forbiddenWording.contains(ForbiddenWording.StrongWording))
    assertEquals(DeterministicRenderer.fromPlan(cappedPlan), None)
    assertEquals(DeterministicRenderer.fromPlan(plan.copy(allowedClaim = None)), None)
    assertEquals(DeterministicRenderer.fromPlan(plan.copy(debugOnly = true)), None)

  test("Stage 7-4 renders no standalone text for Support Context or Blocked plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get).get
    val supportPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Support).get).get
    val contextPlan =
      leadPlan.copy(role = Role.Context, allowedClaim = None, relations = Vector(ExplanationRelation.AlternativeHangingCandidate))
    val blockedPlan =
      leadPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)

    assert(leadPlan.allowedClaim.nonEmpty)
    assert(DeterministicRenderer.fromPlan(leadPlan).nonEmpty)
    assertEquals(DeterministicRenderer.fromPlan(supportPlan), None)
    assertEquals(DeterministicRenderer.fromPlan(contextPlan), None)
    assertEquals(DeterministicRenderer.fromPlan(blockedPlan), None)
    assertEquals(DeterministicRenderer.fromPlan(leadPlan.copy(role = Role.Lead, allowedClaim = None)), None)

  test("Stage 7-4 renders no text for capped or engine-refuted relation plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get

    def checked(status: EngineCheckStatus) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      TacticHanging.withEngineCheck(story, check).get

    val cappedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(checked(EngineCheckStatus.Caps))).head).get
    val refutedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(checked(EngineCheckStatus.Refutes))).head).get

    assertEquals(cappedPlan.allowedClaim, None)
    assert(cappedPlan.relations.contains(ExplanationRelation.CappedSameStory))
    assertEquals(DeterministicRenderer.fromPlan(cappedPlan), None)
    assertEquals(refutedPlan.allowedClaim, None)
    assert(refutedPlan.relations.contains(ExplanationRelation.BlockedByEngineRefute))
    assertEquals(DeterministicRenderer.fromPlan(refutedPlan), None)

  test("Stage 7-5 RenderedLine carries only text claim strength and forbidden check"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val fieldNames =
      rendered.getClass.getDeclaredFields.map(_.getName).filterNot(_.startsWith("$")).toVector
    def fieldValue(name: String) =
      val field = rendered.getClass.getDeclaredField(name)
      field.setAccessible(true)
      field.get(rendered)

    assertEquals(rendered.getClass.getSimpleName, "RenderedLine")
    assertEquals(fieldNames, Vector("text", "claimKey", "strength", "forbiddenCheckPassed"))
    assertEquals(fieldValue("text"), "dxe5 wins material against the piece on e5.")
    assertEquals(fieldValue("claimKey"), "can_win_piece")
    assertEquals(fieldValue("strength"), "bounded")
    assertEquals(fieldValue("forbiddenCheckPassed").asInstanceOf[Boolean], true)

  test("Stage 7-5 RenderedLine owns no proof engine board source or route analysis fields"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val renderedLineSurfaceNames =
      (rendered.getClass.getDeclaredFields.map(_.getName).toSet ++
        rendered.getClass.getDeclaredMethods.map(_.getName).toSet)
        .map(_.toLowerCase)

    Vector(
      "captureResult",
      "engineCheck",
      "boardFacts",
      "proofFailures",
      "rawRouteAnalysis",
      "sourceRow",
      "engineEval",
      "engineLine",
      "rawPv",
      "route",
      "proof"
    ).foreach: forbiddenName =>
      assert(
        !renderedLineSurfaceNames.exists(_.contains(forbiddenName.toLowerCase)),
        s"RenderedLine must not expose $forbiddenName"
      )

  test("Stage 7-6 baseline renders Lead CanWinPiece bounded text no stronger than plan"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases =
      Vector(
        "engine",
        "best move",
        "blunder",
        "free piece",
        "decisive",
        "forced",
        "winning position"
      )

    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(rendered.text, "dxe5 wins material against the piece on e5.")
    assertEquals(rendered.claimKey, "can_win_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"baseline renderer must not mention: $phrase")

  test("Stage 7-6 baseline rejects non Lead no-claim engine-limited and forbidden plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val right = TacticHanging.write(facts, rightCapture).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get).get
    val supportPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Support).get).get
    val contextPlan =
      leadPlan.copy(role = Role.Context, allowedClaim = None, relations = Vector(ExplanationRelation.AlternativeHangingCandidate))
    val blockedPlan =
      leadPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val debugOnlyPlan = leadPlan.copy(debugOnly = true)
    val noClaimPlan = leadPlan.copy(allowedClaim = None)
    val forbiddenPlan = leadPlan.copy(forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.StrongWording)
    val cappedCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(right),
      engineLine = Some(EngineLine(Vector(rightCapture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val cappedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(TacticHanging.withEngineCheck(right, cappedCheck).get)).head).get

    Vector(
      supportPlan,
      contextPlan,
      blockedPlan,
      debugOnlyPlan,
      noClaimPlan,
      cappedPlan,
      forbiddenPlan
    ).foreach: plan =>
      assertEquals(DeterministicRenderer.fromPlan(plan), None)

  test("Stage 7-6 baseline renderer exposes no Verdict or EngineCheck input"):
    val methodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assert(!methodNames.contains("fromVerdict"))
    assert(!methodNames.contains("fromEngineCheck"))
    assert(!parameterNames.contains("Verdict"))
    assert(!parameterNames.contains("EngineCheck"))

  test("Stage 8A mock narrator echoes safe RenderedLine text only"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val mockText = LlmNarrationSmoke.mockNarrate(plan, rendered).get
    val checked = LlmNarrationSmoke.check(plan, rendered, mockText)
    val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = plan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = plan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)

    assertEquals(mockText, rendered.text)
    assertEquals(checked.accepted, true)
    assertEquals(checked.violations, Vector.empty)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, Option.empty[RenderedLine]), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, Some(rendered)), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(contextPlan, Some(rendered)), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(blockedPlan, Some(rendered)), None)

  test("Stage 8B Codex CLI prompt smoke uses only rendered text contract"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector(
      "renderedText: dxe5 wins material against the piece on e5.",
      "claimKey: can_win_piece",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"prompt must include allowed input: $required")
    Vector(
      "ExplanationPlan",
      "FEN",
      "PGN",
      "Verdict",
      "Story",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "source row",
      "role:",
      "scene:",
      "tactic:",
      "side:",
      "target:",
      "route:",
      "evidence line:"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"prompt must not include forbidden raw input label: $forbidden")

  test("Stage 8 smoke checker rejects forbidden wording stronger claims and invented lines"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val engineBestWinning =
      LlmNarrationSmoke.check(plan, rendered, "The engine says dxe5 is the best move and a winning position.")
    val inventedLine =
      LlmNarrationSmoke.check(plan, rendered, "After dxe5 Ke7, White wins material.")
    val inventedTactic =
      LlmNarrationSmoke.check(plan, rendered, "dxe5 starts a fork and a strategic plan.")
    val inventedCauseAndEval =
      LlmNarrationSmoke.check(plan, rendered, "dxe5 works because White is better afterward.")
    val freePiece =
      LlmNarrationSmoke.check(plan, rendered, "White wins a free piece on e5.")

    assertEquals(engineBestWinning.accepted, false)
    assertEquals(engineBestWinning.violations.contains("forbidden_wording"), true)
    assertEquals(engineBestWinning.violations.contains("stronger_claim"), true)
    assertEquals(inventedLine.accepted, false)
    assertEquals(inventedLine.violations.contains("new_move_or_line"), true)
    assertEquals(inventedTactic.accepted, false)
    assertEquals(inventedTactic.violations.contains("new_tactic_or_plan"), true)
    assertEquals(inventedCauseAndEval.accepted, false)
    assertEquals(inventedCauseAndEval.violations.contains("new_cause_or_evaluation"), true)
    assertEquals(freePiece.accepted, false)
    assertEquals(freePiece.violations.contains("forbidden_wording"), true)
    assertEquals(freePiece.violations.contains("stronger_claim"), true)

  test("Stage 8 narration smoke exposes no raw proof or production API input"):
    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
    val resultSurfaceNames =
      classOf[NarrationSmokeCheck].getDeclaredFields.map(_.getName).toSet ++
        classOf[NarrationSmokeCheck].getDeclaredMethods.map(_.getName).toSet

    assertEquals(parameterNames.contains("ExplanationPlan"), true)
    assertEquals(parameterNames.contains("RenderedLine"), true)
    Vector(
      "fromVerdict",
      "fromStory",
      "fromBoardFacts",
      "fromBoardMood",
      "fromCaptureResult",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: forbiddenMethod =>
      assert(!methodNames.contains(forbiddenMethod), s"Stage 8 smoke must not expose $forbiddenMethod")
    Vector(
      "Verdict",
      "Story",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine"
    ).foreach: forbiddenType =>
      assert(!parameterNames.contains(forbiddenType), s"Stage 8 smoke must not accept $forbiddenType")
    Vector(
      "verdict",
      "story",
      "boardFacts",
      "boardMood",
      "captureResult",
      "engineCheck",
      "engineEval",
      "engineLine",
      "rawPv",
      "proofFailures",
      "sourceRow",
      "model",
      "temperature",
      "responseFormat",
      "retry",
      "timeout"
    ).foreach: forbiddenName =>
      assert(!resultSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 2 ordering does not use proofFailures as public sort input"):
    val proofScore = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      conversionPrize = 99
    )
    val incomplete = Story(
      Scene.Material,
      side = Side.White,
      target = Some(Square('a', 1)),
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      rival = Side.Black,
      proof = proofScore
    )
    val complete = incomplete.copy(
      target = Some(Square('b', 1)),
      storyProof = storyProof()
    )
    val verdicts = StoryTable.choose(Vector(complete, incomplete))

    assert(incomplete.proofFailures.nonEmpty)
    assertEquals(complete.proofFailures, Vector.empty)
    assertEquals(verdicts.head.story, incomplete)
    assert(verdicts.forall(!_.leadAllowed))
    assert(verdicts.forall(_.role != Role.Lead))

  test("StoryTable chooses at most eight deterministic verdicts"):
    val stories =
      Vector.tabulate(10): i =>
        Story(
          Scene.Material,
          side = Side.White,
          route = Some(safeRoute),
          proof = proof(
            boardProof = 90 - i,
            ownerProof = 90,
            anchorProof = 90,
            routeProof = 90,
            conversionPrize = 90 - i
          )
        )

    val verdicts = StoryTable.choose(stories)

    assertEquals(StoryTable.TopK, 8)
    assertEquals(verdicts.size, StoryTable.TopK)
    assert(verdicts.map(_.strength).sliding(2).forall(pair => pair.size == 1 || pair(0) >= pair(1)))
    assertEquals(verdicts.map(_.rank), (1 to 8).toVector)

  test("StoryTable requires named positive Story writers for public leads"):
    val low =
      Story(
        Scene.Material,
        side = Side.White,
        proof =
          proof(boardProof = 64, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 90)
      )
    val risky =
      Story(
        Scene.King,
        side = Side.White,
        route = Some(safeRoute),
        proof = proof(
          boardProof = 80,
          ownerProof = 90,
          anchorProof = 90,
          routeProof = 90,
          conversionPrize = 90,
          counterplayRisk = 71
        )
      )
    val solid =
      Story(
        Scene.Material,
        side = Side.White,
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        proof =
          proof(boardProof = 80, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 90)
      )

    val verdicts = StoryTable.choose(Vector(low, risky, solid))

    assertEquals(StoryTable.PublicStoryLeadsRequireNamedProofWriters, true)
    assertEquals(verdicts.find(_.story == solid).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == solid).map(_.leadAllowed), Some(false))
    assertEquals(verdicts.find(_.story == low).map(_.leadAllowed), Some(false))
    assertEquals(verdicts.find(_.story == risky).map(_.leadAllowed), Some(false))
    assert(verdicts.forall(_.role != Role.Lead))

  test("Quiet remains blocked without same-root proof sidecars"):
    val quiet =
      Story(
        Scene.Quiet,
        side = Side.White,
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        proof =
          proof(boardProof = 80, ownerProof = 80, anchorProof = 80, routeProof = 80, conversionPrize = 90)
      )
    val material =
      Story(
        Scene.Material,
        side = Side.White,
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        proof =
          proof(boardProof = 80, ownerProof = 80, anchorProof = 80, routeProof = 80, conversionPrize = 90)
      )

    assertEquals(StoryTable.choose(Vector(quiet)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(quiet)).head.leadAllowed, false)
    assertEquals(
      StoryTable.choose(Vector(quiet, material)).find(_.story == quiet).map(_.leadAllowed),
      Some(false)
    )

  test("Tactic heat does not open Stage 3 priority without named Hanging writer"):
    val plan = Story(
      Scene.Plan,
      side = Side.White,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      plan = Some(Plan.Minority),
      rival = Side.Black,
      target = Some(Square('a', 2)),
      proof = proof(boardProof = 80, ownerProof = 80, anchorProof = 80, routeProof = 80, conversionPrize = 90),
      storyProof = storyProof()
    )
    val tactic = Story(
      Scene.Tactic,
      side = Side.Black,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      rival = Side.White,
      target = Some(Square('a', 2)),
      tactic = Some(Tactic.Fork),
      proof = proof(
        boardProof = 80,
        ownerProof = 80,
        anchorProof = 80,
        routeProof = 80,
        lineProof = 70,
        forcing = 95,
        conversionPrize = 95,
        kingHeat = 95,
        immediacy = 95
      ),
      storyProof = storyProof()
    )

    val verdicts = StoryTable.choose(Vector(plan, tactic))

    assertEquals(verdicts.head.story, plan)
    assert(verdicts.forall(!_.leadAllowed))
    assert(verdicts.forall(_.role != Role.Lead))

  test("Plan ordering remains deterministic while only Hanging writer can lead"):
    val plan = Story(
      Scene.Plan,
      side = Side.White,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      target = Some(Square('a', 2)),
      rival = Side.Black,
      plan = Some(Plan.CenterBreak),
      proof = proof(boardProof = 85, ownerProof = 85, anchorProof = 85, routeProof = 85, conversionPrize = 95),
      storyProof = storyProof()
    )
    val sameSideTactic = Story(
      Scene.Tactic,
      side = Side.White,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      target = Some(Square('a', 2)),
      rival = Side.Black,
      tactic = Some(Tactic.Fork),
      proof = proof(boardProof = 75, ownerProof = 75, anchorProof = 75, routeProof = 75, conversionPrize = 95),
      storyProof = storyProof()
    )
    val opposingTactic = sameSideTactic.copy(side = Side.Black, rival = Side.White)

    assertEquals(
      StoryTable.choose(Vector(plan, sameSideTactic)).find(_.story == plan).map(_.leadAllowed),
      Some(false)
    )

    val verdicts = StoryTable.choose(Vector(plan, opposingTactic))

    assertEquals(verdicts.find(_.story == plan).map(_.leadAllowed), Some(false))
    assertEquals(verdicts.head.story, plan)
    assert(verdicts.forall(_.role != Role.Lead))

  test("Source remains non-lead while only Hanging writer can lead"):
    val source =
      Story(
        Scene.Source,
        side = Side.White,
        route = Some(safeRoute),
        proof =
          proof(boardProof = 90, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 95)
      )
    val boardBacked =
      Story(
        Scene.Material,
        side = Side.White,
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        proof =
          proof(boardProof = 70, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 95)
      )

    val verdicts = StoryTable.choose(Vector(source, boardBacked))

    assertEquals(verdicts.find(_.story == source).map(_.leadAllowed), Some(false))
    assertEquals(verdicts.find(_.story == boardBacked).map(_.leadAllowed), Some(false))
    assert(verdicts.forall(_.role != Role.Lead))

  test("Broad pin stories and writerless Pin remain blocked"):
    val absPin = Story(
      Scene.Tactic,
      side = Side.White,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      tactic = Some(Tactic.AbsPin),
      proof = proof(
        boardProof = 80,
        ownerProof = 80,
        anchorProof = 80,
        routeProof = 80,
        lineProof = 70,
        forcing = 90
      )
    )
    val relPin = absPin.copy(tactic = Some(Tactic.RelPin))
    val writerlessPin = absPin.copy(tactic = Some(Tactic.Pin))

    assertEquals(StoryTable.choose(Vector(absPin)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(relPin)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(writerlessPin)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(absPin)).head.leadAllowed, false)
    assertEquals(StoryTable.choose(Vector(relPin)).head.leadAllowed, false)
    assertEquals(StoryTable.choose(Vector(writerlessPin)).head.leadAllowed, false)
    assertEquals(absPin.tactic.exists(t => t == Tactic.AbsPin || t == Tactic.RelPin), true)
    assertEquals(relPin.tactic.exists(t => t == Tactic.AbsPin || t == Tactic.RelPin), true)
    assertEquals(writerlessPin.tactic.contains(Tactic.Pin), true)

  test("Pin tactic tags on non-Tactic scenes cannot lead"):
    val falsePin = Story(
      Scene.Plan,
      side = Side.White,
      route = Some(safeRoute),
      plan = Some(Plan.Reroute),
      tactic = Some(Tactic.AbsPin),
      proof = proof(
        boardProof = 85,
        ownerProof = 85,
        anchorProof = 85,
        routeProof = 85,
        lineProof = 75,
        forcing = 90
      )
    )

    val verdict = StoryTable.choose(Vector(falsePin)).head

    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("Non-Tactic scenes with tactic tags do not get tactical lead treatment"):
    val falseFork = Story(
      Scene.Material,
      side = Side.White,
      route = Some(safeRoute),
      tactic = Some(Tactic.Fork),
      proof = proof(
        boardProof = 85,
        ownerProof = 85,
        anchorProof = 85,
        routeProof = 85,
        lineProof = 75,
        forcing = 90
      )
    )

    val verdict = StoryTable.choose(Vector(falseFork)).head

    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("StoryTable tie break includes route identity"):
    val bRoute = Story(
      Scene.Material,
      side = Side.White,
      target = Some(Square('d', 4)),
      anchor = Some(Square('e', 5)),
      route = Some(Line(Square('b', 1), Square('b', 8))),
      rival = Side.Black,
      proof = proof(boardProof = 82, ownerProof = 82, anchorProof = 82, routeProof = 82, conversionPrize = 82)
    )
    val aRoute = bRoute.copy(
      route = Some(Line(Square('a', 1), Square('a', 8)))
    )

    assertEquals(StoryTable.choose(Vector(bRoute, aRoute)).head.story, aRoute)
    assertEquals(StoryTable.choose(Vector(aRoute, bRoute)).head.story, aRoute)

  test("StoryTable fail-closes forged owner, anchor, route, line, and tactic claims"):
    val ownerless = Story(
      Scene.Material,
      side = Side.None,
      route = Some(safeRoute),
      proof = proof(
        boardProof = 90,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        lineProof = 90,
        conversionPrize = 95
      )
    )
    val anchorless = ownerless.copy(side = Side.White, anchor = None, route = Some(safeRoute))
    val routeless = ownerless.copy(side = Side.White, route = None)
    val lineMissing = Story(
      Scene.Tactic,
      side = Side.White,
      route = Some(safeRoute),
      tactic = Some(Tactic.Fork),
      proof = proof(
        boardProof = 90,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        lineProof = 0,
        conversionPrize = 95
      )
    )
    val motifMissing = lineMissing.copy(
      tactic = None,
      proof = proof(
        boardProof = 90,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        lineProof = 90,
        conversionPrize = 95
      )
    )

    val verdicts = StoryTable.choose(Vector(ownerless, anchorless, routeless, lineMissing, motifMissing))

    assert(verdicts.forall(!_.leadAllowed))
    assert(verdicts.forall(_.role != Role.Lead))
