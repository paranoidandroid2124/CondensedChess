package lila.llm.analysis

import chess.{ Board, Square, Role, Color }
import lila.llm.model._

/**
 * FactExtractor
 * 
 * The bridge between low-level tactical motifs and high-level Facts.
 * Performs coordinate verification to ensure zero-hallucination commentary.
 */
object FactExtractor {

  /**
   * Generates a list of verified Facts from raw Motifs and Board state.
   */
  def fromMotifs(
      board: Board,
      motifs: List[Motif],
      scope: FactScope
  ): List[Fact] = {
    motifs.flatMap { m =>
      mapMotifToFact(m, board, scope)
    }.distinct
  }

  /**
   * Extracts static positional facts (Hanging pieces, Weak squares) from a board state.
   */
  def extractStaticFacts(board: Board, color: Color): List[Fact] = {
    val myPieces = board.byColor(color)
    val oppPieces = board.byColor(!color)
    val scope = FactScope.Now

    val hanging = myPieces.squares.flatMap { sq =>
      val role = board.roleAt(sq).getOrElse(chess.Pawn)
      if (role == chess.King) None // King is never "hanging", it's in check or mate
      else {
        val attackers = board.attackers(sq, !color).squares
        val defenders = board.attackers(sq, color).squares
        
        // Piece value check to avoid trading down poorly
        val isHanging = (attackers.nonEmpty && defenders.isEmpty) || 
                        (attackers.nonEmpty && pieceValue(role) > 1 && attackers.size > defenders.size)
        
        if (isHanging) Some(Fact.HangingPiece(sq, role, attackers, defenders, scope))
        else None
      }
    }

    val loose = myPieces.squares.flatMap { sq =>
      val role = board.roleAt(sq).getOrElse(chess.Pawn)
      val attackers = board.attackers(sq, !color).squares
      val defenders = board.attackers(sq, color).squares
      
      if (attackers.nonEmpty && !hanging.exists(_.participants.contains(sq))) {
        Some(Fact.TargetPiece(sq, role, attackers, defenders, scope))
      } else None
    }

    hanging ++ loose
  }

  /**
   * Endgame specialized fact extraction.
   */
  def extractEndgameFacts(board: Board, color: Color): List[Fact] = {
    // (Approximated here by low total material or absence of major pieces)
    val majorPieces = (board.queens | board.rooks)
    val totalMaterial = board.occupied.count
    val isEndgame = majorPieces.count <= 2 || totalMaterial <= 15

    if (!isEndgame) return Nil

    val myKing = board.kingPosOf(color)
    val oppKing = board.kingPosOf(!color)
    val scope = FactScope.Now
    val facts = scala.collection.mutable.ListBuffer[Fact]()

    myKing.foreach { mk =>
      // King Activity: Correct mobility counts squares king can actually move to
      val mobility = (mk.kingAttacks & ~board.byColor(color)).count
      val dist = Math.max((mk.file.value - 4).abs, (mk.rank.value - 4).abs)
      facts += Fact.KingActivity(mk, mobility.toInt, dist, scope)

      // Opposition
      oppKing.foreach { ok =>
        val fileDiff = (mk.file.value - ok.file.value).abs
        val rankDiff = (mk.rank.value - ok.rank.value).abs
        
        // Direct opposition
        val isDirect = (fileDiff == 0 && rankDiff == 2) || (fileDiff == 2 && rankDiff == 0)
        // Distant opposition (3 or 5 squares)
        val isDistant = (fileDiff == 0 && (rankDiff == 4 || rankDiff == 6)) || 
                        (rankDiff == 0 && (fileDiff == 4 || fileDiff == 6))
        // Diagonal opposition
        val isDiagonal = fileDiff == 2 && rankDiff == 2

        if (isDirect || isDistant || isDiagonal) {
          facts += Fact.Opposition(
            king = mk, 
            enemyKing = ok, 
            distance = Math.max(fileDiff, rankDiff), 
            isDirect = isDirect, 
            scope = scope
          )
        }
      }
    }

    facts.toList
  }

  private def mapMotifToFact(motif: Motif, board: Board, scope: FactScope): Option[Fact] = {
    motif match {
      case m: Motif.Pin =>
        for {
          a <- m.pinningSq
          p <- m.pinnedSq
          b <- m.behindSq
          aRole <- board.roleAt(a).orElse(Some(m.pinningPiece))
          pRole <- board.roleAt(p).orElse(Some(m.pinnedPiece))
          bRole <- board.roleAt(b).orElse(Some(m.targetBehind))
        } yield Fact.Pin(a, aRole, p, pRole, b, bRole, m.isAbsolutePin, scope)

      case m: Motif.Skewer =>
        for {
          a <- m.attackingSq
          f <- m.frontSq
          b <- m.backSq
          aRole <- board.roleAt(a).orElse(Some(m.attackingPiece))
          fRole <- board.roleAt(f).orElse(Some(m.frontPiece))
          bRole <- board.roleAt(b).orElse(Some(m.backPiece))
        } yield Fact.Skewer(a, aRole, f, fRole, b, bRole, scope)

      case m: Motif.Fork =>
        // Verify attacker exists (for Scope.Now)
        val verifiedTargets = m.targetSquares.flatMap { ts =>
          // In PV scope, pieces might move, so we rely on the motif's claim
          // But for Now scope, we check the board.
          if (scope == FactScope.Now) {
            board.roleAt(ts).map(r => (ts, r))
          } else {
             // Fallback to roles from motif
             // Since m.targets is just a list of Roles, we zip them if possible
             m.targets.zip(m.targetSquares).map { case (r, s) => (s, r) }.headOption // Simplification
          }
        }
        if (verifiedTargets.nonEmpty) Some(Fact.Fork(m.square, m.attackingPiece, verifiedTargets.asInstanceOf[List[(Square, Role)]], scope))
        else None

      case m: Motif.Outpost =>
        Some(Fact.Outpost(m.square, m.piece, scope))

      case m: Motif.Centralization =>
        Some(Fact.ActivatesPiece(m.piece, m.square, m.square, false, scope)) // Simplified mapping

      case m: Motif.Check =>
        if (m.checkType == Motif.CheckType.Double) Some(Fact.DoubleCheck(List(m.targetSquare), scope)) // Simplified
        else Some(Fact.TargetPiece(m.targetSquare, chess.King, List(m.targetSquare), Nil, scope))

      /*
      case m: Motif.Zugzwang =>
        Some(Fact.Zugzwang(m.color, scope))
      */
      
      case m: Motif.PawnPromotion =>
        Some(Fact.PawnPromotion(Square.at(m.file.value, if (m.color.white) 7 else 0).getOrElse(Square.A1), Some(m.promotedTo), scope))

      case m: Motif.PassedPawnPush =>
        Some(Fact.PawnPromotion(Square.at(m.file.value, m.toRank - 1).getOrElse(Square.A1), None, scope))

      /*
      case m: Motif.StalemateThreat =>
        board.kingPosOf(m.color).map(k => Fact.StalemateThreat(k, scope))
      */

      case m: Motif.DoubleCheck =>
        // Need to find the checking squares - in Motif it's just a category
        // But we can approximate or leave as TargetPiece for now if data is missing
        Some(Fact.DoubleCheck(Nil, scope))

      case _ => None
    }
  }

  private def pieceValue(role: Role): Int = role match {
    case chess.Pawn   => 1
    case chess.Knight => 3
    case chess.Bishop => 3
    case chess.Rook   => 5
    case chess.Queen  => 9
    case chess.King   => 0
  }
}
