package lila.commentary.chess

import chess.format.Fen
import chess.variant
import lila.commentary.root.{ RootAtomRegistry, RootStateVector }

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
      "d2-d3 and h2-h3 count; d2xe3 and h2-g3 count diagnostically; d2-c3 and double pushes do not"
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
      "e7-e6 and h7-h6 count; e7xf6 and h7-g6 count diagnostically; e7-d6 and double pushes do not"
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
        Scene.Plan,
        Scene.Pieces,
        Scene.Space,
        Scene.Initiative,
        Scene.Convert,
        Scene.Endgame,
        Scene.Counterplay,
        Scene.Source,
        Scene.Quiet
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
        Tactic.Skewer,
        Tactic.Xray,
        Tactic.Fork,
        Tactic.Discover,
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
    assertEquals(Story.Size, 160)
    assertEquals(Story.SceneSlots, 16)
    assertEquals(Story.PlanSlots, 32)
    assertEquals(Story.TacticSlots, 24)
    assertEquals(Story.PawnSlots, 16)
    assertEquals(Story.PieceSlots, 16)
    assertEquals(Story.KingSlots, 16)
    assertEquals(Story.OpeningSlots, 8)
    assertEquals(Story.ProofSlots, 32)
    assertEquals(Story.Slots.Scene, 0)
    assertEquals(Story.Slots.Plan, 16)
    assertEquals(Story.Slots.Tactic, 48)
    assertEquals(Story.Slots.Pawn, 72)
    assertEquals(Story.Slots.Piece, 88)
    assertEquals(Story.Slots.King, 104)
    assertEquals(Story.Slots.Opening, 120)
    assertEquals(Story.Slots.Proof, 128)
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
    assertEquals(plan.evidenceLine, Some(forkMove))
    assertEquals(rendered.text, "f3-d4 forks the pieces on b5 and f5.")
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
      "renderedText: f3-d4 forks the pieces on b5 and f5.",
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

    val safe = LlmNarrationSmoke.check(plan, rendered, "f3-d4 forks the pieces on b5 and f5.")
    val inventedMove = LlmNarrationSmoke.check(plan, rendered, "After f3-d4 h8h7, the fork stays.")
    val inventedTactic = LlmNarrationSmoke.check(plan, rendered, "f3-d4 forks the pieces and starts a skewer.")
    val inventedPlan = LlmNarrationSmoke.check(plan, rendered, "f3-d4 forks the pieces and starts a plan.")
    val engineBestWinning =
      LlmNarrationSmoke.check(plan, rendered, "The engine says f3-d4 is the best move and a winning fork.")
    val forcedDecisiveBlunder =
      LlmNarrationSmoke.check(plan, rendered, "f3-d4 is a forced decisive fork after a blunder.")
    val winsQueen = LlmNarrationSmoke.check(plan, rendered, "f3-d4 forks the pieces and wins the queen.")
    val winsMaterial = LlmNarrationSmoke.check(plan, rendered, "f3-d4 forks the pieces and wins material.")
    val namesTargets =
      LlmNarrationSmoke.check(plan, rendered, "f3-d4 forks the queen on b5 and rook on f5.")

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

    assertEquals(Verdict.Size, 96)
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
    assertEquals(Verdict.Slots.Plan, 24)
    assertEquals(Verdict.Slots.Tactic, 56)
    assertEquals(Verdict.Slots.Proof, 80)
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
    assertEquals(plan.evidenceLine, Some(capture))
    assertEquals(plan.target, Some(Square('e', 5)))
    assert(plan.forbiddenWording.nonEmpty)
    assertEquals(rendered.text, "d4xe5 wins material against the piece on e5.")
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
    assertEquals(fieldValue("text"), "d4xe5 wins material against the piece on e5.")
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
    assertEquals(rendered.text, "d4xe5 wins material against the piece on e5.")
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
      "renderedText: d4xe5 wins material against the piece on e5.",
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
      LlmNarrationSmoke.check(plan, rendered, "The engine says d4xe5 is the best move and a winning position.")
    val inventedLine =
      LlmNarrationSmoke.check(plan, rendered, "After d4xe5 e8e7, White wins material.")
    val inventedTactic =
      LlmNarrationSmoke.check(plan, rendered, "d4xe5 starts a fork and a strategic plan.")
    val inventedCauseAndEval =
      LlmNarrationSmoke.check(plan, rendered, "d4xe5 works because White is better afterward.")
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

  test("Pin stories remain blocked until proof sidecar writers exist"):
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

    assertEquals(StoryTable.choose(Vector(absPin)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(relPin)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(absPin)).head.leadAllowed, false)
    assertEquals(StoryTable.choose(Vector(relPin)).head.leadAllowed, false)
    assertEquals(absPin.tactic.exists(t => t == Tactic.AbsPin || t == Tactic.RelPin), true)
    assertEquals(relPin.tactic.exists(t => t == Tactic.AbsPin || t == Tactic.RelPin), true)

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
