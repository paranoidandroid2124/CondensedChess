package lila.commentary.chess

import chess.format.Fen
import chess.variant
import lila.commentary.root.{ RootAtomRegistry, RootStateVector }

class BoardFactsTest extends ChessTestSupport:

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
        Scene.PawnBreak,
        Scene.PawnBlock,
        Scene.Plan,
        Scene.Pieces,
        Scene.Space,
        Scene.Initiative,
        Scene.Convert,
        Scene.Endgame,
        Scene.Counterplay,
        Scene.Source,
        Scene.Quiet,
        Scene.PromotionThreat,
        Scene.Promotion,
        Scene.PawnCapture,
        Scene.PassedPawnCreated,
        Scene.FileOpened,
        Scene.CheckGiven,
        Scene.CheckEscaped,
        Scene.Checkmate,
        Scene.Stalemate
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
        Tactic.Interference,
        Tactic.Tempo
      )
    )

  test("Pawn Break runtime scope extends positive pawn promotion writers by one narrow slice"):
    val pawnPromotionWriters =
      StoryWriter.values.toVector.filter: writer =>
        val name = writer.toString
        name.contains("Pawn") || name.contains("Promotion")

    assertEquals(
      pawnPromotionWriters,
      Vector(
        StoryWriter.ScenePawnAdvance,
        StoryWriter.ScenePawnStop,
        StoryWriter.ScenePawnBreak,
        StoryWriter.ScenePawnBlock,
        StoryWriter.ScenePromotionThreat,
        StoryWriter.ScenePromotion,
        StoryWriter.ScenePawnCapture,
        StoryWriter.ScenePassedPawnCreated
      )
    )

    val closedPawnWriterFragments =
      Vector("Race", "Tablebase", "KingRoute", "Opposition", "Convert")
    StoryWriter.values.foreach: writer =>
      closedPawnWriterFragments.foreach: fragment =>
        assert(!writer.toString.contains(fragment), s"$fragment must not be a positive Story writer")

  test("Stage-2 Tempo QueenHit collision keeps queen-target text inside attacks_queen"):
    val facts = BoardFacts.fromFen("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1").toOption.get
    val move = Line(Square('d', 2), Square('h', 2))
    val queenHit =
      TacticQueenHit
        .write(facts, Some(move))
        .get
        .copy(
          proof = proof(
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
        )
    val verdict = StoryTable.choose(Vector(queenHit)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenQueenTargetText = Vector(
      "gains tempo",
      "wins tempo",
      "with tempo",
      "queen must move",
      "forces the queen",
      "drives the queen",
      "gains time by attacking the queen",
      "seizes initiative"
    )

    assertEquals(queenHit.tactic, Some(Tactic.QueenHit))
    assertEquals(queenHit.writer, Some(StoryWriter.TacticQueenHit))
    assertEquals(queenHit.queenHitProof.exists(_.complete), true)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.AttacksQueen))
    assertEquals(rendered.claimKey, "attacks_queen")
    assertEquals(rendered.text, "Rh2 attacks the queen on h5.")
    assertEquals(Tactic.values.toVector.contains(Tactic.Tempo), true)
    assertEquals(StoryWriter.values.toVector.exists(_.toString.contains("Tempo")), false)
    assertEquals(ExplanationClaim.values.toVector.exists(_.key.contains("tempo")), false)
    assert(plan.forbiddenWording.exists(_.key == "gains_tempo"))
    forbiddenQueenTargetText.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"QueenHit text escaped as forbidden queen-target wording: $phrase")

  test("PNC-2 runtime duplication audit keeps pawn promotion speech keys unique"):
    val pawnPromotionClaims =
      Vector(
        ExplanationClaim.PawnAdvanceAllowed,
        ExplanationClaim.PawnStopAllowed,
        ExplanationClaim.PromotionThreatAllowed,
        ExplanationClaim.PromotionAllowed
      )

    assertEquals(
      pawnPromotionClaims,
      Vector(
        Vector(ExplanationClaim.AdvancesPassedPawn),
        Vector(ExplanationClaim.StopsPassedPawnNextAdvance),
        Vector(ExplanationClaim.CreatesPromotionThreat),
        Vector(ExplanationClaim.PromotesPawn)
      )
    )

    val claimKeys = pawnPromotionClaims.flatten.map(_.key)
    assertEquals(
      claimKeys,
      Vector(
        "advances_passed_pawn",
        "stops_pawn_advance",
        "threatens_promotion_next",
        "promotes_pawn"
      )
    )
    assertEquals(claimKeys.distinct, claimKeys)

    val forbiddenPawnPromotionClaimKeys =
      Vector(
        "pawn_break",
        "pawn_race",
        "tablebase",
        "king_route",
        "opposition",
        "conversion",
        "material_gain",
        "winning"
      )
    forbiddenPawnPromotionClaimKeys.foreach: key =>
      assert(!claimKeys.contains(key), s"$key must not be an opened pawn/promotion speech key")

