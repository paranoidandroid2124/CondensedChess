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
      concepts: Option[ConceptScorer.Scores] = None
  ): List[String] =
    val board = position.board
    val tags = mutable.ListBuffer.empty[String]
    val oppColor = !perspective
    val plyNumber = ply.value

    val myKing = board.kingPosOf(perspective)
    val oppKing = board.kingPosOf(oppColor)

    if hasOpenFileThreat(board, oppKing, File.H, perspective) then tags += "open_h_file"
    if hasOpenFileThreat(board, oppKing, File.G, perspective) then tags += "open_g_file"

    if weakHomePawn(board, oppColor, File.F, Rank.Seventh) then tags += "weak_f7"
    if weakHomePawn(board, oppColor, File.F, Rank.Second) then tags += "weak_f2"

    strongOutpost(board, perspective).foreach(tags += _)

    if backRankWeak(board, perspective) then tags += TagName.WeakBackRank
    looseMinor(board, perspective).foreach(tags += _)

    if kingStuckCenter(myKing, plyNumber) then tags += "king_stuck_center"

    if hasIsolatedDPawn(board, perspective, plyNumber) then tags += "isolated_d_pawn"

    if rookOnSeventh(board, perspective) then tags += "rook_on_seventh"

    if pawnStormAgainstCastledKing(board, perspective, oppKing) then tags += "pawn_storm_against_castled_king"

    if opp.bishopPair && self.bishopPair == false && FeatureExtractor.hasOppositeColorBishops(board) then
      tags += "opposite_color_bishops"

    if self.kingRingPressure >= 4 && opp.kingRingPressure <= 1 then tags += "king_attack_ready"

    // Concept-based tags from ConceptScorer
    concepts.foreach { c =>
      // Strategic imbalances
      if c.badBishop >= 0.6 then tags += "bad_bishop"
      if c.goodKnight >= 0.6 then tags += "good_knight_outpost"
      if c.colorComplex >= 0.5 then tags += "color_complex_weakness"
      
      // Positional character
      if c.fortress >= 0.6 then tags += TagName.FortressBuilding
      if c.dry >= 0.6 then tags += "dry_position"
      if c.pawnStorm >= 0.6 then tags += "pawn_storm"
      
      // Space and activity
      val spaceAdvantage = self.spaceControl.toDouble / (self.spaceControl + opp.spaceControl + 1.0)
      if spaceAdvantage >= 0.65 then tags += "space_advantage"
      if c.rookActivity >= 0.6 then tags += "active_rooks"
      
      // Crisis and opportunity
      if c.kingSafety >= 0.5 then tags += TagName.KingExposed
      if c.conversionDifficulty >= 0.5 then tags += TagName.ConversionDifficulty
      if c.blunderRisk >= 0.6 then tags += "high_blunder_risk"
      if c.tacticalDepth >= 0.6 then tags += "tactical_complexity"
      
      // Dynamic vs static
      if c.dynamic >= 0.7 then tags += "dynamic_position"
      if c.drawish >= 0.7 then tags += "drawish_position"
      if c.imbalanced >= 0.6 then tags += "material_imbalance"
      
      // Advanced concepts
      if c.sacrificeQuality >= 0.5 then tags += TagName.PositionalSacrifice
      if c.alphaZeroStyle >= 0.6 then tags += "long_term_compensation"
      if c.engineLike >= 0.6 then tags += "engine_only_move"
      if c.comfortable >= 0.7 then tags += "comfortable_position"
      if c.unpleasant >= 0.6 then tags += "unpleasant_position"
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

  private def weakHomePawn(board: Board, color: Color, file: File, rank: Rank): Boolean =
    val pawns = board.pawns & board.byColor(color)
    !pawns.contains(Square(file, rank))

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
      val kingOnBackRank = k.rank == backRank
      val pawnsShield =
        val files = List(File.F, File.G, File.H)
        val pawns = board.pawns & board.byColor(color)
        val shieldRank = if color == Color.White then Rank.Second else Rank.Seventh
        files.count(f => pawns.contains(Square(f, shieldRank))) >= 2
      kingOnBackRank && !pawnsShield
    }

  private def looseMinor(board: Board, color: Color): Option[String] =
    val defenders = board.attackers(_: Square, color)
    val attackers = board.attackers(_: Square, !color)
    val minors = (board.bishops | board.knights) & board.byColor(color)
    minors.squares.collectFirst {
      case sq if attackers(sq).nonEmpty && defenders(sq).isEmpty => s"loose_piece_${sq.key.toLowerCase}"
    }

  private def kingStuckCenter(kingOpt: Option[Square], ply: Int): Boolean =
    kingOpt.exists { k =>
      val centerFiles = Set(File.D, File.E)
      ply > 12 && centerFiles.contains(k.file)
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
    oppKingOpt.exists { king =>
      val isShortCastle = (king == Square.G1 && color == Color.Black) || (king == Square.G8 && color == Color.White)
      val isLongCastle = (king == Square.C1 && color == Color.Black) || (king == Square.C8 && color == Color.White)
      val myPawns = board.pawns & board.byColor(color)
      val advancedOnWing =
        if isShortCastle then
          myPawns.squares.exists { s =>
            s.file.value >= File.F.value && (if color == Color.White then s.rank.value >= Rank.Fourth.value else s.rank.value <= Rank.Fifth.value)
          }
        else if isLongCastle then
          myPawns.squares.exists { s =>
            s.file.value <= File.C.value && (if color == Color.White then s.rank.value >= Rank.Fourth.value else s.rank.value <= Rank.Fifth.value)
          }
        else false
      (isShortCastle || isLongCastle) && advancedOnWing
    }
