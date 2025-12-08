package chess
package analysis

import scala.collection.mutable

/** Rule-based semantic tags to turn board state into chess facts (not raw floats).
  * Tags are relevance-filtered to avoid noise; only conditions that matter for the
  * current side are emitted.
  */
object SemanticTagger:

  def tags(
      position: Position,
      perspective: Color,
      ply: Ply,
      self: FeatureExtractor.SideFeatures,
      opp: FeatureExtractor.SideFeatures,
      concepts: Option[ConceptScorer.Scores] = None,
      winPct: Double = 50.0 // Added winPct for context-aware tagging
  ): List[String] =
    val board = position.board
    val tags = mutable.ListBuffer.empty[String]
    val oppColor = !perspective
    val plyNumber = ply.value

    val oppKing = board.kingPosOf(oppColor)
    val p = perspective.toString.toLowerCase
    val o = oppColor.toString.toLowerCase

    if hasOpenFileThreat(board, oppKing, File.H, perspective) then tags += s"${p}_open_h_file"
    if hasOpenFileThreat(board, oppKing, File.G, perspective) then tags += s"${p}_open_g_file"

    weakFHomeTag(board, oppColor, File.F, Rank.Seventh, s"${o}_weak_f7").foreach(tags += _)
    weakFHomeTag(board, oppColor, File.F, Rank.Second, s"${o}_weak_f2").foreach(tags += _)

    strongOutpost(board, perspective).foreach(s => tags += s"${p}_$s") // strongOutpost returns "outpost_..."

    if backRankWeak(board, perspective) then tags += s"${p}_weak_back_rank"
    looseMinor(board, perspective).foreach(s => tags += s"${p}_$s") // looseMinor returns "loose_piece_..."

    if kingStuckCenter(position, perspective, plyNumber) then tags += s"${p}_king_stuck_center"

    if hasIsolatedDPawn(board, perspective, plyNumber) then tags += s"${p}_isolated_d_pawn"

    if rookOnSeventh(board, perspective) then tags += s"${p}_rook_on_seventh"

    val boardPawnStorm = pawnStormAgainstCastledKing(board, perspective, oppKing)
    if boardPawnStorm then tags += s"${p}_pawn_storm_against_castled_king"

    if opp.bishopPair && self.bishopPair == false && FeatureExtractor.hasOppositeColorBishops(board) then
      tags += "opposite_color_bishops"

    if self.kingRingPressure >= 4 && opp.kingRingPressure <= 1 then tags += s"${p}_king_attack_ready"

    // Concept-based tags from ConceptScorer
    concepts.foreach { c =>
      // Strategic imbalances
      if c.badBishop >= 0.6 then tags += s"${p}_restricted_bishop"
      if c.goodKnight >= 0.6 then
        if hasCentralKnightOutpost(board, perspective) then tags += s"${p}_knight_outpost_central"
        else tags += s"${p}_strong_knight"
      if c.colorComplex >= 0.5 then tags += s"${p}_color_complex_weakness"
      
      // Positional character
      // Fortress Logic Split
      val endgame = isEndgame(board)
      if c.fortress >= 0.6 && !endgame && plyNumber > 20 then tags += "locked_position" // Neutral
      
      if c.fortress >= 0.6 && endgame then
        val myMat = materialScore(board, perspective)
        val oppMat = materialScore(board, oppColor)
        if myMat <= oppMat - 1.0 && c.drawish >= 0.5 then
           tags += s"${p}_fortress_defense"
 
      val dynamicTagged =
        if c.dynamic >= 0.7 && c.dry < 0.5 then
          tags += "dynamic_position"
          true
        else false
      if !dynamicTagged && c.dry >= 0.6 && (plyNumber > 16 || isEndgame(board)) then tags += "dry_position"
      if c.drawish >= 0.7 && winPct >= 35.0 && winPct <= 65.0 then tags += "drawish_position"
      if c.pawnStorm >= 0.6 && boardPawnStorm then tags += s"${p}_pawn_storm"
      
      // Space and activity
      val spaceAdvantage = self.spaceControl.toDouble / (self.spaceControl + opp.spaceControl + 1.0)
      if spaceAdvantage >= 0.65 then tags += s"${p}_space_advantage"
      if c.rookActivity >= 0.6 then tags += s"${p}_active_rooks"
      
      // Crisis and opportunity
      val crisis = c.kingSafety >= 0.6 && (c.pawnStorm >= 0.5 || c.rookActivity >= 0.6)
      if crisis then tags += s"${p}_king_safety_crisis"
      else if c.kingSafety >= 0.5 then tags += s"${p}_king_exposed"
      
      // Conversion Difficulty: Only if winning
      if c.conversionDifficulty >= 0.5 && winPct >= 60.0 then tags += s"${p}_conversion_difficulty"
      
      if c.blunderRisk >= 0.6 then tags += "high_blunder_risk"
      if c.tacticalDepth >= 0.6 then tags += "tactical_complexity"
      
      // Dynamic vs static
      if c.imbalanced >= 0.6 then tags += "material_imbalance"
      
      // Advanced concepts
      if c.sacrificeQuality >= 0.5 then tags += s"${p}_positional_sacrifice"
      if c.alphaZeroStyle >= 0.6 then tags += s"${p}_long_term_compensation"
      if c.engineLike >= 0.6 then tags += s"${p}_engine_only_move"
      if c.comfortable >= 0.7 then tags += s"${p}_comfortable_position"
      
      // Unpleasant: Only if slightly worse or equal
      if c.unpleasant >= 0.6 && winPct <= 45.0 then tags += s"${p}_unpleasant_position"
    }

    // Rich Concept Taxonomy (White Space #2) - Refined
    concepts.foreach { c =>
      // 1. Conversion Difficulty Context
      if c.conversionDifficulty >= 0.5 && winPct >= 60.0 then
        if isEndgame(board) then tags += s"${p}_conversion_difficulty_endgame"
        if tags.contains("opposite_color_bishops") then tags += s"${p}_conversion_difficulty_opposite_bishops"

      // 2. Fortress Potential (Material down but holding) - Merged into fortress_defense logic above

      // 3. Bishop Pair Advantage in Open Position
      if self.bishopPair && !opp.bishopPair && c.dry <= 0.4 then 
        tags += s"${p}_bishop_pair_advantage"
    }

    tags.distinct.toList

  private def hasOpenFileThreat(board: Board, kingOpt: Option[Square], file: File, perspective: Color): Boolean =
    kingOpt.exists { kingSq =>
      val kingNearFile = (kingSq.file.value - file.value).abs <= 1
      val myMajorOnFile = hasMajorOnFile(board, perspective, file)
      val openOrSemiOpen =
        val friendly = board.pawns & board.byColor(perspective)
        val enemy = board.pawns & board.byColor(!perspective)
        val mask = file.bb
        val friendlyOnFile = friendly.intersects(mask)
        val enemyOnFile = enemy.intersects(mask)
        (!friendlyOnFile && !enemyOnFile) || (!friendlyOnFile && enemyOnFile)
      kingNearFile && myMajorOnFile && openOrSemiOpen
    }

  private def hasMajorOnFile(board: Board, color: Color, file: File): Boolean =
    val majors = (board.rooks | board.queens) & board.byColor(color)
    majors.squares.exists(_.file == file)

  private def weakFHomeTag(board: Board, color: Color, file: File, rank: Rank, tag: String): Option[String] =
    if isEndgame(board) then None
    else if !kingInFHomeSector(board, color) then None
    else
      val homeSq = Square(file, rank)
      val opp = !color
      val attackers = board.attackers(homeSq, opp)
      val defenders = board.attackers(homeSq, color)
      val attackersCount = attackers.count
      val defendersCount = defenders.count
      val strongAttackerExists =
        attackers.exists { sq =>
          board.roleAt(sq).exists(r => r == Queen || r == Rook)
        }
      val oppMajorsExist = hasMajors(board, opp)

      board.pieceAt(homeSq) match
        case Some(Piece(Pawn, `color`)) =>
          if attackersCount >= 2 && attackersCount > defendersCount && strongAttackerExists then Some(tag) else None
        case _ =>
          if oppMajorsExist && attackersCount >= 1 && strongAttackerExists then Some(tag) else None

  private def kingInFHomeSector(board: Board, color: Color): Boolean =
    board.kingPosOf(color).exists { k =>
      color match
        case Color.White =>
          k.rank.value <= Rank.Second.value && k.file.value >= File.E.value && k.file.value <= File.G.value
        case Color.Black =>
          k.rank.value >= Rank.Seventh.value && k.file.value >= File.E.value && k.file.value <= File.G.value
    }

  private def hasMajors(board: Board, color: Color): Boolean =
    ((board.queens & board.byColor(color)).nonEmpty) || ((board.rooks & board.byColor(color)).nonEmpty)

  private def hasCentralKnightOutpost(board: Board, color: Color): Boolean =
    val knights = board.knights & board.byColor(color)
    val centralFiles = Set(File.D, File.E)
    val centralRanks =
      if color == Color.White then Set(Rank.Fourth, Rank.Fifth, Rank.Sixth)
      else Set(Rank.Fifth, Rank.Fourth, Rank.Third)
    knights.squares.exists(k => centralFiles.contains(k.file) && centralRanks.contains(k.rank))

  private def strongOutpost(board: Board, color: Color): Option[String] =
    val squares =
      if color == Color.White then List(Square.D4, Square.E4, Square.F4, Square.D5, Square.E5, Square.F5)
      else List(Square.D5, Square.E5, Square.F5, Square.D4, Square.E4, Square.F4)
    squares.collectFirst {
      case sq if isOutpost(board, color, sq) =>
        val label = s"outpost_${sq.key.toLowerCase}"
        label
    }

  private def isOutpost(board: Board, color: Color, sq: Square): Boolean =
    val knights = board.knights & board.byColor(color)
    val pawnSupport = (board.pawns & board.byColor(color)).squares.exists { p =>
      p.pawnAttacks(color).contains(sq)
    }
    val enemyPawns = board.pawns & board.byColor(!color)
    val enemyChasers = enemyPawns.squares.exists { p =>
      p.pawnAttacks(!color).contains(sq)
    }
    knights.contains(sq) && pawnSupport && !enemyChasers

  private def backRankWeak(board: Board, color: Color): Boolean =
    board.kingPosOf(color).exists { k =>
      val backRank = if color == Color.White then Rank.First else Rank.Eighth
      if k.rank != backRank then false
      else if isEndgame(board) then false
      else
        val oppMajors = (board.rooks | board.queens) & board.byColor(!color)
        if oppMajors.isEmpty then false
        else
          val shieldRank = if color == Color.White then Rank.Second else Rank.Seventh
          val shieldFiles =
            if k.file.value <= File.D.value then List(File.B, File.C, File.D)
            else List(File.F, File.G, File.H)
          val pawns = board.pawns & board.byColor(color)
          val shieldCount = shieldFiles.count(f => pawns.contains(Square(f, shieldRank)))
          shieldCount < 2
    }

  private def looseMinor(board: Board, color: Color): Option[String] =
    val defenders = board.attackers(_: Square, color)
    val attackers = board.attackers(_: Square, !color)
    val minors = (board.bishops | board.knights) & board.byColor(color)
    minors.squares.collectFirst {
      case sq if attackers(sq).nonEmpty && defenders(sq).isEmpty => s"loose_piece_${sq.key.toLowerCase}"
    }

  private def kingStuckCenter(position: Position, color: Color, ply: Int): Boolean =
    val board = position.board
    val endgame = isEndgame(board)
    val anyQueen = board.queens.nonEmpty
    val canCastle = position.castles.can(color)
    board.kingPosOf(color).exists { k =>
      val centerFiles = Set(File.D, File.E)
      val centerRankOk =
        if color == Color.White then k.rank.value <= Rank.Third.value
        else k.rank.value >= Rank.Sixth.value
      ply > 12 &&
      !endgame &&
      anyQueen &&
      canCastle &&
      centerFiles.contains(k.file) &&
      centerRankOk
    }

  private def hasIsolatedDPawn(board: Board, color: Color, ply: Int): Boolean =
    if ply < 16 then false
    else
      val pawns = board.pawns & board.byColor(color)
      val dPawn = pawns.squares.find(_.file == File.D)
      dPawn.exists { pawn =>
        val files = List(File.C, File.E)
        val hasNeighbor = files.exists { f => pawns.squares.exists(_.file == f) }
        !hasNeighbor
      }

  private def rookOnSeventh(board: Board, color: Color): Boolean =
    val rooks = board.rooks & board.byColor(color)
    rooks.squares.exists { r =>
      if color == Color.White then r.rank == Rank.Seventh else r.rank == Rank.Second
    }

  private def pawnStormAgainstCastledKing(board: Board, color: Color, oppKingOpt: Option[Square]): Boolean =
    if isEndgame(board) || board.queens.isEmpty then false
    else
      oppKingOpt.exists { king =>
        val isShortCastle = (king == Square.G1 && color == Color.Black) || (king == Square.G8 && color == Color.White)
        val isLongCastle = (king == Square.C1 && color == Color.Black) || (king == Square.C8 && color == Color.White)
        val myPawns = board.pawns & board.byColor(color)
        val advancedThreshold = if color == Color.White then Rank.Fourth.value else Rank.Fifth.value

        val candidatePawns = myPawns.squares.filter { s =>
          val kingside = s.file.value >= File.F.value
          val queenside = s.file.value <= File.C.value
          val advanced = if color == Color.White then s.rank.value >= advancedThreshold else s.rank.value <= advancedThreshold
          (isShortCastle && kingside && advanced) || (isLongCastle && queenside && advanced)
        }

        val closeToKing = candidatePawns.exists { s =>
          val fileDist = (s.file.value - king.file.value).abs
          val rankDist = (s.rank.value - king.rank.value).abs
          fileDist + rankDist <= 3
        }

        (isShortCastle || isLongCastle) && candidatePawns.size >= 2 && closeToKing
      }

  private def isEndgame(board: Board): Boolean =
    val whitePieces = board.byColor(Color.White).count
    val blackPieces = board.byColor(Color.Black).count
    whitePieces <= 6 || blackPieces <= 6 || (board.queens.isEmpty && whitePieces <= 8 && blackPieces <= 8)

  private def materialScore(board: Board, color: Color): Double =
    val pawns = (board.pawns & board.byColor(color)).count * 1.0
    val knights = (board.knights & board.byColor(color)).count * 3.0
    val bishops = (board.bishops & board.byColor(color)).count * 3.0
    val rooks = (board.rooks & board.byColor(color)).count * 5.0
    val queens = (board.queens & board.byColor(color)).count * 9.0
    pawns + knights + bishops + rooks + queens
