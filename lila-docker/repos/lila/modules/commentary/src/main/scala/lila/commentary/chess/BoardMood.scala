package lila.commentary.chess

import lila.commentary.root.{ RootAtomRegistry, RootStateVector }

final class BoardMood private (val bits: Vector[Long], rawScalars: Vector[Int]):
  require(bits.size == BoardMood.Bits, s"BoardMood needs ${BoardMood.Bits} bit slots")
  val scalars: Vector[Int] = BoardMood.canonicalScalarInput(rawScalars)
  require(scalars.size == BoardMood.Scalars, s"BoardMood needs ${BoardMood.Scalars} scalars")
  BoardMood.requireCanonicalRootPadding(bits)

  def activeRootAtomIndices: Vector[Int] =
    bits
      .take(BoardMood.RootWordBits)
      .zipWithIndex
      .flatMap: (word, wordIndex) =>
        (0 until 64).collect:
          case bit if (word & (1L << bit)) != 0L =>
            wordIndex * 64 + bit
      .filter(_ < RootAtomRegistry.RootSize)

  override def equals(other: Any): Boolean =
    other match
      case that: BoardMood => bits == that.bits && scalars == that.scalars
      case _               => false

  override def hashCode: Int =
    31 * bits.hashCode + scalars.hashCode

  override def toString: String =
    s"BoardMood($bits,$scalars)"

object BoardMood:
  val Schema = 1
  val Bits = 48
  val Scalars = 256
  val Size = Bits * 64 + Scalars

  val RootWordBits = 46
  val SideLegalDestinationsBit = 46
  val RivalLegalDestinationsBit = 47
  private val LastRootWord = RootWordBits - 1
  private val LastRootValidBits = RootAtomRegistry.RootSize - LastRootWord * 64
  private val LastRootPaddingMask = ~((1L << LastRootValidBits) - 1L)
  val SplitScalarIndices: Set[Int] = Set(
    31, 76, 77, 79, 92, 93, 95, 102, 106, 107, 109, 110, 118, 122, 123, 125, 126, 128, 129, 130,
    133, 134, 136, 137, 143, 144, 145, 146, 149, 150, 152, 153, 159, 163, 168, 169, 170, 171, 172,
    173, 179, 184, 185, 186, 187, 188, 189, 192, 193, 194, 195, 196, 197, 198, 199, 200, 202, 203,
    204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 218
  )
  val CutScalarIndices: Set[Int] = Set(
    13, 14, 72, 78, 88, 94, 104, 111, 120, 127, 131, 142, 147, 158, 164, 174, 175, 180, 190, 191,
    201, 214, 215, 216, 217, 219, 220, 221, 222, 223
  )
  val ProofScalarIndices: Set[Int] = (224 until Scalars).toSet
  val ClosedScalarIndices: Set[Int] = SplitScalarIndices ++ CutScalarIndices ++ ProofScalarIndices

  final case class BitSlot(index: Int, name: String, role: String, note: String)

  final case class ScalarSlot(
      index: Int,
      name: String,
      role: String,
      zero: String,
      scale: String,
      source: String,
      failClosed: String
  )

  val bitSlots: Vector[BitSlot] =
    val rootSlots =
      (0 until RootWordBits).toVector.map: index =>
        val start = index * 64
        val end = math.min(start + 63, RootAtomRegistry.RootSize - 1)
        BitSlot(
          index = index,
          name = f"root_atoms_${start}%04d_${end}%04d",
          role = "root-word transport",
          note =
            s"Packed RootStateVector word: bit k of B$index decodes to root atom index 64*$index+k; valid only if < RootAtomRegistry.RootSize. This is not a 64-square board mask."
        )
    rootSlots ++ Vector(
      BitSlot(
        SideLegalDestinationsBit,
        "side_legal_destination_union",
        "legal destination summary",
        "Union of legal destination squares for the story side; it does not prove origin, route, forcing line, or public claim legality."
      ),
      BitSlot(
        RivalLegalDestinationsBit,
        "rival_legal_destination_union",
        "legal destination summary",
        "Union of legal destination squares for the rival side; it does not prove origin, route, forcing line, or public claim legality."
      )
    )

  val scalarNames: Vector[String] = Vector(
    "side_to_move",
    "ply_from_start",
    "phase_total",
    "phase_non_pawn",
    "halfmove_clock",
    "fullmove_number",
    "castling_mask",
    "ep_square_plus_one",
    "in_check_mask",
    "legal_move_count",
    "legal_capture_count",
    "legal_check_count",
    "snapshot_ply",
    "board_hash_lo",
    "board_hash_hi",
    "position_ready",
    "white_pawn_count",
    "white_knight_count",
    "white_bishop_count",
    "white_rook_count",
    "white_queen_count",
    "white_king_count",
    "black_pawn_count",
    "black_knight_count",
    "black_bishop_count",
    "black_rook_count",
    "black_queen_count",
    "black_king_count",
    "white_material",
    "black_material",
    "material_diff",
    "material_imbalance",
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
    "black_king_mobility",
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
    "black_king_safe_mobility",
    "white_space",
    "black_space",
    "space_diff",
    "white_controlled_squares",
    "black_controlled_squares",
    "contested_squares",
    "white_attacked_twice",
    "black_attacked_twice",
    "white_king_square",
    "white_king_ring_squares",
    "white_king_ring_enemy_attacks",
    "white_king_ring_friendly_defenders",
    "white_safe_escape_count",
    "white_legal_check_count_against",
    "white_shelter_holes",
    "white_storm_pawns_against",
    "white_open_file_exposure",
    "white_diagonal_exposure",
    "white_rook_queen_line_exposure",
    "white_pinned_defender_count",
    "white_overloaded_defender_count",
    "white_contact_check_threats",
    "white_mate_net_pressure",
    "white_king_heat",
    "black_king_square",
    "black_king_ring_squares",
    "black_king_ring_enemy_attacks",
    "black_king_ring_friendly_defenders",
    "black_safe_escape_count",
    "black_legal_check_count_against",
    "black_shelter_holes",
    "black_storm_pawns_against",
    "black_open_file_exposure",
    "black_diagonal_exposure",
    "black_rook_queen_line_exposure",
    "black_pinned_defender_count",
    "black_overloaded_defender_count",
    "black_contact_check_threats",
    "black_mate_net_pressure",
    "black_king_heat",
    "white_pawn_file_counts",
    "white_isolated_pawn_count",
    "white_backward_pawn_count",
    "white_doubled_file_count",
    "white_passed_pawn_count",
    "white_candidate_passer_count",
    "white_protected_passer_count",
    "white_fixed_pawn_count",
    "white_chain_base_count",
    "white_lever_count",
    "white_break_chance_count",
    "white_blockaded_pawn_count",
    "white_promotion_distance_best",
    "white_pawn_support",
    "white_pawn_risk",
    "white_pawn_structure_score",
    "black_pawn_file_counts",
    "black_isolated_pawn_count",
    "black_backward_pawn_count",
    "black_doubled_file_count",
    "black_passed_pawn_count",
    "black_candidate_passer_count",
    "black_protected_passer_count",
    "black_fixed_pawn_count",
    "black_chain_base_count",
    "black_lever_count",
    "black_break_chance_count",
    "black_blockaded_pawn_count",
    "black_promotion_distance_best",
    "black_pawn_support",
    "black_pawn_risk",
    "black_pawn_structure_score",
    "white_minor_activity",
    "white_rook_activity",
    "white_queen_activity",
    "white_piece_mobility_score",
    "white_outpost_count",
    "white_bad_bishop_count",
    "white_rook_open_file_count",
    "white_rook_seventh_count",
    "white_loose_piece_count",
    "white_hanging_piece_count",
    "white_pinned_piece_count",
    "white_overloaded_piece_count",
    "white_trapped_piece_count",
    "white_xray_target_count",
    "white_route_clarity",
    "white_piece_coordination",
    "black_minor_activity",
    "black_rook_activity",
    "black_queen_activity",
    "black_piece_mobility_score",
    "black_outpost_count",
    "black_bad_bishop_count",
    "black_rook_open_file_count",
    "black_rook_seventh_count",
    "black_loose_piece_count",
    "black_hanging_piece_count",
    "black_pinned_piece_count",
    "black_overloaded_piece_count",
    "black_trapped_piece_count",
    "black_xray_target_count",
    "black_route_clarity",
    "black_piece_coordination",
    "white_forcing_move_count",
    "white_check_threat_count",
    "white_capture_threat_count",
    "white_see_best_gain",
    "white_conversion_prize",
    "white_fork_motif_count",
    "white_pin_motif_count",
    "white_skewer_motif_count",
    "white_xray_discovery_count",
    "white_overload_deflect_count",
    "white_remove_guard_count",
    "white_back_rank_pressure",
    "white_promotion_threat",
    "white_defender_shortage",
    "white_counterplay",
    "white_tactical_score",
    "black_forcing_move_count",
    "black_check_threat_count",
    "black_capture_threat_count",
    "black_see_best_gain",
    "black_conversion_prize",
    "black_fork_motif_count",
    "black_pin_motif_count",
    "black_skewer_motif_count",
    "black_xray_discovery_count",
    "black_overload_deflect_count",
    "black_remove_guard_count",
    "black_back_rank_pressure",
    "black_promotion_threat",
    "black_defender_shortage",
    "black_counterplay",
    "black_tactical_score",
    "plan_minority",
    "plan_majority",
    "plan_center_break",
    "plan_flank_break",
    "plan_storm",
    "plan_expansion",
    "plan_cramp",
    "plan_outpost",
    "plan_bad_piece",
    "plan_reroute",
    "plan_bishops",
    "plan_blockade",
    "plan_open_file",
    "plan_seventh",
    "plan_color_bind",
    "plan_weak_square",
    "plan_isolani",
    "plan_backward_pawn",
    "plan_hanging_pawns",
    "plan_chain_base",
    "plan_passer_make",
    "plan_passer_block",
    "plan_race",
    "plan_trade",
    "plan_simplify",
    "plan_keep_pieces",
    "plan_overload",
    "plan_prophy",
    "plan_counterplay",
    "plan_initiative",
    "plan_king_convert",
    "plan_convert",
    "exact_board_binding",
    "legal_replay_binding",
    "owner_binding",
    "anchor_binding",
    "route_binding",
    "same_root_certificate",
    "engine_depth",
    "engine_freshness_ply",
    "engine_eval_stability",
    "eval_delta_cp",
    "source_fit",
    "source_public_safety",
    "opening_line_fit",
    "novelty_signal",
    "stale_or_forbidden",
    "render_safe",
    "ray_count",
    "line_proof_count",
    "source_count",
    "evidence_count",
    "side_piece_pressure",
    "rival_piece_pressure",
    "side_pawn_pressure",
    "rival_pawn_pressure",
    "side_king_pressure",
    "rival_king_pressure",
    "side_plan_pressure",
    "rival_plan_pressure",
    "side_tactic_pressure",
    "rival_tactic_pressure",
    "board_story_pressure",
    "public_claim_pressure"
  )

  require(scalarNames.size == Scalars, s"BoardMood scalar name table must have $Scalars entries")
  require(scalarNames.distinct.size == Scalars, "BoardMood scalar names must be unique")

  val scalarSlots: Vector[ScalarSlot] =
    scalarNames.zipWithIndex.map: (name, index) =>
      ScalarSlot(
        index = index,
        name = name,
        role = scalarRole(index),
        zero = scalarZero(name),
        scale = scalarScale(name, index),
        source = scalarSource(index),
        failClosed = scalarFailClosed(index)
      )

  val ScalarsByName: Map[String, Int] = scalarSlots.map(slot => slot.name -> slot.index).toMap

  val empty = new BoardMood(Vector.fill(Bits)(0L), Vector.fill(Scalars)(0))

  def fromPacked(bits: Vector[Long], scalars: Vector[Int]): BoardMood =
    new BoardMood(bits, scalars)

  def fromParts(
      rootWords: Option[Vector[Long]] = None,
      sideLegalDestinations: Long = 0L,
      rivalLegalDestinations: Long = 0L,
      scalars: Option[Vector[Int]] = None
  ): BoardMood =
    val words = rootWords match
      case Some(values) =>
        require(values.size == RootWordBits, s"rootWords needs $RootWordBits slots")
        values
      case None => Vector.fill(RootWordBits)(0L)
    val scalarValues = scalars match
      case Some(values) =>
        require(values.size == Scalars, s"scalars needs $Scalars slots")
        canonicalScalarInput(values)
      case None => Vector.fill(Scalars)(0)
    new BoardMood(words ++ Vector(sideLegalDestinations, rivalLegalDestinations), scalarValues)

  def fromRoot(
      rootState: RootStateVector,
      sideLegalDestinations: Long = 0L,
      rivalLegalDestinations: Long = 0L,
      scalars: Option[Vector[Int]] = None
  ): BoardMood =
    val words = Array.fill[Long](RootWordBits)(0L)
    rootState.activeIndices.foreach: index =>
      words(index >>> 6) |= 1L << (index & 63)
    fromParts(
      rootWords = Some(words.toVector),
      sideLegalDestinations = sideLegalDestinations,
      rivalLegalDestinations = rivalLegalDestinations,
      scalars = scalars
    )

  def fromFacts(facts: BoardFacts): BoardMood =
    require(
      facts.sideToMove == Side.White || facts.sideToMove == Side.Black,
      "BoardFacts sideToMove must be White or Black"
    )
    val scalars = Array.fill[Int](Scalars)(0)

    def put(name: String, value: Int): Unit =
      scalars(ScalarsByName(name)) = value

    val header = facts.header
    put("side_to_move", facts.sideToMove.ordinal)
    put("ply_from_start", header.plyFromStart)
    put("phase_total", header.phaseTotal)
    put("phase_non_pawn", header.phaseNonPawn)
    put("halfmove_clock", header.halfmoveClock)
    put("fullmove_number", header.fullmoveNumber)
    put("castling_mask", header.castlingMask)
    put("ep_square_plus_one", header.epSquare.fold(0)(_.index + 1))
    put("in_check_mask", header.inCheckMask)
    put("legal_move_count", facts.sideLegal.moveCount)
    put("legal_capture_count", facts.sideLegal.captureCount)
    put("legal_check_count", facts.sideLegal.checkCount)
    put("snapshot_ply", header.snapshotPly)
    put("board_hash_lo", header.hashLo)
    put("board_hash_hi", header.hashHi)
    put("position_ready", if positionReady(facts) then 1 else 0)

    putPieces("white", facts.material.white, put)
    putPieces("black", facts.material.black, put)
    put("white_material", facts.material.white.value)
    put("black_material", facts.material.black.value)
    put("material_diff", facts.material.diff)
    put("material_imbalance", facts.material.imbalance)

    val mobility = mobilityFromPieces(facts.pieces, facts.control)
    putMobility("white", mobility.white.mobility, safe = false, put)
    putMobility("black", mobility.black.mobility, safe = false, put)
    putMobility("white", mobility.white.safeMobility, safe = true, put)
    putMobility("black", mobility.black.safeMobility, safe = true, put)

    put("white_space", facts.control.white.space)
    put("black_space", facts.control.black.space)
    put("space_diff", facts.control.spaceDiff)
    put("white_controlled_squares", facts.control.white.controlledSquares)
    put("black_controlled_squares", facts.control.black.controlledSquares)
    put("contested_squares", facts.control.contestedSquares)
    put("white_attacked_twice", facts.control.white.attackedTwice)
    put("black_attacked_twice", facts.control.black.attackedTwice)

    putPawns("white", facts.pawns.white, put)
    putPawns("black", facts.pawns.black, put)

    fromRoot(
      facts.root,
      sideLegalDestinations = facts.sideLegal.legalDestinationUnion,
      rivalLegalDestinations = facts.rivalLegal.legalDestinationUnion,
      scalars = Some(scalars.toVector)
    )

  def fromPieces(pieces: Vector[Piece], side: Side = Side.None): BoardMood =
    val indices = pieces.map: piece =>
      RootAtomRegistry.pieceOnIndex(rootColor(piece.side), rootRole(piece.man), rootSquare(piece.square))
    val scalars = Array.fill[Int](Scalars)(0)
    scalars(ScalarsByName("side_to_move")) = side.ordinal
    scalars(ScalarsByName("phase_total")) = pieces.map(piece => phaseValue(piece.man)).sum
    scalars(ScalarsByName("phase_non_pawn")) = pieces.count(piece => piece.man != Man.Pawn && piece.man != Man.King)
    scalars(ScalarsByName("position_ready")) = 0

    pieces.foreach: piece =>
      val countName = s"${sideName(piece.side)}_${manName(piece.man)}_count"
      scalars(ScalarsByName(countName)) += 1
      val materialName = s"${sideName(piece.side)}_material"
      scalars(ScalarsByName(materialName)) += materialValue(piece.man)

    scalars(ScalarsByName("material_diff")) =
      scalars(ScalarsByName("white_material")) - scalars(ScalarsByName("black_material"))

    fromRoot(RootStateVector.fromIndices(indices), scalars = Some(scalars.toVector))

  def rootAtomIndex(bitSlot: Int, bit: Int): Option[Int] =
    if bitSlot < 0 || bitSlot >= RootWordBits || bit < 0 || bit >= 64 then None
    else
      val index = bitSlot * 64 + bit
      Option.when(index < RootAtomRegistry.RootSize)(index)

  def rootWordSquareMask(bitSlot: Int): Option[Long] = None

  private def requireCanonicalRootPadding(bits: Vector[Long]): Unit =
    if bits.size == Bits then
      require(
        (bits(LastRootWord) & LastRootPaddingMask) == 0L,
        s"B45 padding bits above root index ${RootAtomRegistry.RootSize - 1} must be zero"
      )

  private def scalarRole(index: Int): String =
    index match
      case i if i < 16   => "position snapshot"
      case i if i < 32   => "material identity"
      case i if i < 64   => "mobility and space"
      case i if i < 96   => "king safety"
      case i if i < 128  => "pawn structure"
      case i if i < 160  => "piece activity"
      case i if i < 192  => "tactical affordance"
      case i if i < 224  => "plan affordance"
      case i if i < 240  => "binding and source diagnostic"
      case _             => "proof and pressure summary"

  private def scalarZero(name: String): String =
    if name.endsWith("_plus_one") then "0 means absent; otherwise stored value is square index + 1."
    else if name.endsWith("_square") then "0 means absent; populated values store square index + 1."
    else if name.contains("binding") || name == "render_safe" || name == "position_ready" then
      "0 means not proven, not ready, or fail-closed."
    else "0 means absent, none, false, or no measured pressure/count."

  private def scalarScale(name: String, index: Int): String =
    if name == "white_pawn_file_counts" || name == "black_pawn_file_counts" then
      "packed 8-file pawn counts: one file count per nibble from a-file through h-file."
    else if name.contains("hash") then "32-bit signed limb of a board hash."
    else if name.contains("eval") || name.contains("material") || name.contains("see") then
      "Integer centipawn/material units; producers must saturate before writing."
    else if index >= 224 then "0..100 diagnostic score unless the slot name is a count/depth/ply/delta."
    else "Integer count, ordinal, mask, score, or centipawn value as named; producers own saturation."

  private def scalarSource(index: Int): String =
    if SplitScalarIndices.contains(index) then
      "Closed broad slot: it may re-enter only as smaller exact chess facts with their own law."
    else if CutScalarIndices.contains(index) then
      "Closed cut slot: BoardMood does not carry this as live chess fact."
    else
      index match
        case i if i < 16 =>
          "Runtime population requires RootStateVector plus legal replay position header; fromPieces is scaffold-only for basic slots."
        case i if i < 32 =>
          "Runtime population requires exact-board material extraction; fromPieces is scaffold-only for counts/material."
        case i if i < 64 =>
          "Runtime population requires legal move, attack, and control sidecars; fromPieces does not populate these."
        case i if i < 96 =>
          "Runtime population requires king safety plus legal escape and attack sidecars; fromPieces does not populate these."
        case i if i < 128 =>
          "Runtime population requires pawn structure sidecars over exact board root atoms; fromPieces does not populate these."
        case i if i < 160 =>
          "Runtime population requires piece activity, route, and ray sidecars; fromPieces does not populate these."
        case i if i < 192 =>
          "Runtime population requires tactical affordance plus SEE/legal replay sidecars; fromPieces does not populate these."
        case i if i < 224 =>
          "Runtime population requires plan affordance extractors; these are inputs, not priors."
        case i if i < 240 =>
          "Runtime population requires owner, anchor, route, source, engine, and replay proof sidecars."
        case _ =>
          "Runtime population requires ray, line proof, source, evidence, and public pressure sidecars."

  private def scalarFailClosed(index: Int): String =
    if SplitScalarIndices.contains(index) then
      "Always writes 0 until replaced by smaller exact chess facts; broad values must stay silent."
    else if CutScalarIndices.contains(index) then
      "Always writes 0; this slot has no live BoardMood meaning."
    else
      index match
        case i if i < 224 =>
          "Missing or stale source writes 0 and must not become public proof by itself."
        case _ =>
          "Missing sidecar proof writes 0 and must block or down-rank public claims."

  private def rootColor(side: Side) =
    side match
      case Side.White => RootAtomRegistry.canonicalColors(0)
      case Side.Black => RootAtomRegistry.canonicalColors(1)
      case _          => throw IllegalArgumentException("Root piece atoms require White or Black")

  private def rootRole(man: Man) = RootAtomRegistry.canonicalRoles(man.ordinal)

  private def rootSquare(square: Square) = RootAtomRegistry.canonicalSquares(square.index)

  private def sideName(side: Side): String =
    side match
      case Side.White => "white"
      case Side.Black => "black"
      case _          => throw IllegalArgumentException("Piece scalars require White or Black")

  private def manName(man: Man): String =
    man match
      case Man.Pawn   => "pawn"
      case Man.Knight => "knight"
      case Man.Bishop => "bishop"
      case Man.Rook   => "rook"
      case Man.Queen  => "queen"
      case Man.King   => "king"

  private def materialValue(man: Man): Int =
    man match
      case Man.Pawn   => 100
      case Man.Knight => 320
      case Man.Bishop => 330
      case Man.Rook   => 500
      case Man.Queen  => 900
      case Man.King   => 0

  private def positionReady(facts: BoardFacts): Boolean =
    // Stage 0 readiness means the boundary received explicit playable facts;
    // it is not proof authority and does not validate move replay.
    (facts.sideToMove == Side.White || facts.sideToMove == Side.Black) &&
      facts.root.activeIndices.nonEmpty &&
      facts.sameBoardReady &&
      facts.header.sane &&
      facts.sideLegal.sane &&
      facts.rivalLegal.sane &&
      facts.control.sane &&
      facts.material.sane &&
      facts.pawns.sane

  private def canonicalScalarInput(values: Vector[Int]): Vector[Int] =
    require(values.size == Scalars, s"scalars needs $Scalars slots")
    values.zipWithIndex.map:
      case (_, index) if ClosedScalarIndices.contains(index) => 0
      case (value, _)                                       => value

  private def putPieces(prefix: String, pieces: Pieces, put: (String, Int) => Unit): Unit =
    put(s"${prefix}_pawn_count", pieces.pawns)
    put(s"${prefix}_knight_count", pieces.knights)
    put(s"${prefix}_bishop_count", pieces.bishops)
    put(s"${prefix}_rook_count", pieces.rooks)
    put(s"${prefix}_queen_count", pieces.queens)
    put(s"${prefix}_king_count", pieces.kings)

  private final case class RoleMobility(
      pawns: Int = 0,
      knights: Int = 0,
      bishops: Int = 0,
      rooks: Int = 0,
      queens: Int = 0,
      kings: Int = 0
  ):
    def +(piece: Piece, destinations: Long): RoleMobility =
      val count = java.lang.Long.bitCount(destinations)
      piece.man match
        case Man.Pawn   => copy(pawns = pawns + count)
        case Man.Knight => copy(knights = knights + count)
        case Man.Bishop => copy(bishops = bishops + count)
        case Man.Rook   => copy(rooks = rooks + count)
        case Man.Queen  => copy(queens = queens + count)
        case Man.King   => copy(kings = kings + count)

    def scalars: Vector[Int] = Vector(pawns, knights, bishops, rooks, queens, kings)

  private final case class SideMobility(mobility: RoleMobility, safeMobility: RoleMobility)
  private final case class MobilityBySide(white: SideMobility, black: SideMobility)

  private def mobilityFromPieces(pieces: Vector[Piece], control: Control): MobilityBySide =
    val occupied = pieces.foldLeft(0L): (mask, piece) =>
      mask | piece.square.bit
    val whiteOccupied = occupiedBy(pieces, Side.White)
    val blackOccupied = occupiedBy(pieces, Side.Black)

    def sideMobility(side: Side, ownOccupied: Long, opponentControl: Long): SideMobility =
      pieces
        .filter(_.side == side)
        .foldLeft(SideMobility(RoleMobility(), RoleMobility())): (summary, piece) =>
          val destinations = mobilityMask(piece, occupied, ownOccupied)
          val safeDestinations = destinations & ~opponentControl
          SideMobility(
            mobility = summary.mobility.+(piece, destinations),
            safeMobility = summary.safeMobility.+(piece, safeDestinations)
          )

    MobilityBySide(
      white = sideMobility(Side.White, whiteOccupied, control.black.controlledMask),
      black = sideMobility(Side.Black, blackOccupied, control.white.controlledMask)
    )

  private def occupiedBy(pieces: Vector[Piece], side: Side): Long =
    pieces.foldLeft(0L): (mask, piece) =>
      if piece.side == side then mask | piece.square.bit else mask

  private def mobilityMask(piece: Piece, occupied: Long, ownOccupied: Long): Long =
    piece.man match
      case Man.Pawn => pawnMobilityMask(piece, occupied, ownOccupied)
      case Man.Knight =>
        leaperMask(piece.square, Vector((1, 2), (2, 1), (2, -1), (1, -2), (-1, -2), (-2, -1), (-2, 1), (-1, 2))) &
          ~ownOccupied
      case Man.Bishop => sliderMask(piece.square, occupied, Vector((1, 1), (1, -1), (-1, 1), (-1, -1))) & ~ownOccupied
      case Man.Rook   => sliderMask(piece.square, occupied, Vector((1, 0), (-1, 0), (0, 1), (0, -1))) & ~ownOccupied
      case Man.Queen =>
        sliderMask(
          piece.square,
          occupied,
          Vector((1, 1), (1, -1), (-1, 1), (-1, -1), (1, 0), (-1, 0), (0, 1), (0, -1))
        ) & ~ownOccupied
      case Man.King =>
        leaperMask(piece.square, Vector((1, 1), (1, 0), (1, -1), (0, 1), (0, -1), (-1, 1), (-1, 0), (-1, -1))) &
          ~ownOccupied

  private def pawnMobilityMask(piece: Piece, occupied: Long, ownOccupied: Long): Long =
    val direction = if piece.side == Side.White then 1 else -1
    val forward =
      target(piece.square, 0, direction)
        .filter(square => (square.bit & occupied) == 0L)
        .fold(0L)(_.bit)
    val diagonals =
      Vector(-1, 1).foldLeft(0L): (mask, fileDelta) =>
        target(piece.square, fileDelta, direction)
          .filter(square => (square.bit & ownOccupied) == 0L)
          .fold(mask)(square => mask | square.bit)
    forward | diagonals

  private def leaperMask(square: Square, deltas: Vector[(Int, Int)]): Long =
    deltas.foldLeft(0L): (mask, delta) =>
      target(square, delta._1, delta._2).fold(mask)(targetSquare => mask | targetSquare.bit)

  private def sliderMask(square: Square, occupied: Long, directions: Vector[(Int, Int)]): Long =
    directions.foldLeft(0L): (mask, direction) =>
      mask | rayMask(square, occupied, direction._1, direction._2)

  private def rayMask(square: Square, occupied: Long, fileDelta: Int, rankDelta: Int): Long =
    def loop(file: Int, rank: Int, mask: Long): Long =
      if file < 0 || file >= 8 || rank < 0 || rank >= 8 then mask
      else
        val targetSquare = Square.fromIndex(rank * 8 + file)
        val nextMask = mask | targetSquare.bit
        if (targetSquare.bit & occupied) != 0L then nextMask
        else loop(file + fileDelta, rank + rankDelta, nextMask)
    loop(square.file + fileDelta, square.rank + rankDelta, 0L)

  private def target(square: Square, fileDelta: Int, rankDelta: Int): Option[Square] =
    val file = square.file + fileDelta
    val rank = square.rank + rankDelta
    Option.when(file >= 0 && file < 8 && rank >= 0 && rank < 8)(Square.fromIndex(rank * 8 + file))

  private def putMobility(
      prefix: String,
      mobility: RoleMobility,
      safe: Boolean,
      put: (String, Int) => Unit
  ): Unit =
    val suffix = if safe then "safe_mobility" else "mobility"
    Vector("pawn", "knight", "bishop", "rook", "queen", "king")
      .zip(mobility.scalars)
      .foreach: (role, value) =>
        put(s"${prefix}_${role}_$suffix", value)

  private def putPawns(prefix: String, pawns: PawnSide, put: (String, Int) => Unit): Unit =
    val names = Vector(
      "pawn_file_counts",
      "isolated_pawn_count",
      "backward_pawn_count",
      "doubled_file_count",
      "passed_pawn_count",
      "candidate_passer_count",
      "protected_passer_count",
      "fixed_pawn_count",
      "chain_base_count",
      "lever_count",
      "break_chance_count",
      "blockaded_pawn_count",
      "promotion_distance_best",
      "pawn_support",
      "pawn_risk",
      "pawn_structure_score"
    )
    names.zip(pawns.scalars).foreach: (name, value) =>
      put(s"${prefix}_$name", value)

  private def phaseValue(man: Man): Int =
    man match
      case Man.Pawn | Man.King => 0
      case Man.Knight          => 1
      case Man.Bishop          => 1
      case Man.Rook            => 2
      case Man.Queen           => 4
