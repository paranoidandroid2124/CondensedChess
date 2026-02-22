package lila.llm.analysis

import chess.{ Board, Square, Role, Color }
import lila.llm.model._
import lila.llm.model.strategic.{ EndgameFeature, RuleOfSquareStatus, EndgameOppositionType, TheoreticalOutcomeHint, RookEndgamePattern as OracleRookPattern }

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
    val ourPieces = board.byColor(color)
    val scope = FactScope.Now

    val hanging = ourPieces.squares.flatMap { sq =>
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

    val loose = ourPieces.squares.flatMap { sq =>
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
  def extractEndgameFacts(board: Board, color: Color): List[Fact] =
    extractEndgameFacts(board, color, None)

  /**
   * Endgame specialized fact extraction.
   * If oracle output is present, it is treated as the single source of truth.
   */
  def extractEndgameFacts(
      board: Board,
      color: Color,
      oracle: Option[EndgameFeature]
  ): List[Fact] = {
    oracle match {
      case Some(endgame) => extractEndgameFactsFromOracle(board, color, endgame)
      case None          => extractEndgameFactsHeuristic(board, color)
    }
  }

  private def extractEndgameFactsHeuristic(board: Board, color: Color): List[Fact] = {
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
            oppositionType =
              if (isDirect) EndgameOppositionType.Direct.toString
              else if (isDistant) EndgameOppositionType.Distant.toString
              else EndgameOppositionType.Diagonal.toString,
            scope = scope
          )
        }
      }
    }

    facts.toList
  }

  private def extractEndgameFactsFromOracle(
      board: Board,
      color: Color,
      endgame: EndgameFeature
  ): List[Fact] = {
    val myKing = board.kingPosOf(color)
    val oppKing = board.kingPosOf(!color)
    val scope = FactScope.Now
    val facts = scala.collection.mutable.ListBuffer[Fact]()

    myKing.foreach { mk =>
      val mobility = (mk.kingAttacks & ~board.byColor(color)).count
      val dist = Math.max((mk.file.value - 4).abs, (mk.rank.value - 4).abs)
      facts += Fact.KingActivity(mk, mobility.toInt, dist, scope)
      if (endgame.triangulationAvailable) facts += Fact.TriangulationOpportunity(mk, scope)
    }

    if (endgame.hasOpposition) {
      (myKing, oppKing) match {
        case (Some(mk), Some(ok)) =>
          val fileDiff = (mk.file.value - ok.file.value).abs
          val rankDiff = (mk.rank.value - ok.rank.value).abs
          val isDirect = endgame.oppositionType == EndgameOppositionType.Direct
          facts += Fact.Opposition(
            king = mk,
            enemyKing = ok,
            distance = Math.max(fileDiff, rankDiff),
            isDirect = isDirect,
            oppositionType = endgame.oppositionType.toString,
            scope = scope
          )
        case _ => ()
      }
    }

    if (endgame.isZugzwang || endgame.zugzwangLikelihood >= 0.65) {
      facts += Fact.Zugzwang(color, scope)
    }

    if (endgame.rookEndgamePattern != OracleRookPattern.None) {
      facts += Fact.RookEndgamePattern(endgame.rookEndgamePattern.toString, scope)
    }

    if (endgame.ruleOfSquare != RuleOfSquareStatus.NA) {
      val enemyPassers = board.byPiece(!color, chess.Pawn).squares.filter(isPassedPawn(board, _, !color))
      val targetPawnOpt = enemyPassers.sortBy(p => if ((!color).white) -p.rank.value else p.rank.value).headOption
      targetPawnOpt.foreach { pawnSq =>
        val promoRank = if ((!color).white) 7 else 0
        val promoSqOpt = Square.at(pawnSq.file.value, promoRank)
        for
          mk <- myKing
          promoSq <- promoSqOpt
        do
          facts += Fact.RuleOfSquare(
            defenderKing = mk,
            targetPawn = pawnSq,
            promotionSquare = promoSq,
            status = endgame.ruleOfSquare.toString,
            scope = scope
          )
      }
    }

    if (endgame.theoreticalOutcomeHint != TheoreticalOutcomeHint.Unclear || endgame.confidence > 0.0) {
      facts += Fact.EndgameOutcome(
        outcome = endgame.theoreticalOutcomeHint.toString,
        confidence = endgame.confidence,
        scope = scope
      )
    }

    facts.toList.distinct
  }

  private def isPassedPawn(board: Board, pawnSq: Square, color: Color): Boolean = {
    val oppPawnsByFile = board.byPiece(!color, chess.Pawn).squares.groupBy(_.file)
    val fileValue = pawnSq.file.value
    val filesToCheck = List(fileValue - 1, fileValue, fileValue + 1).filter(f => f >= 0 && f <= 7)
    filesToCheck.forall { idx =>
      chess.File.all.lift(idx).forall { f =>
        oppPawnsByFile.get(f).forall { pawns =>
          pawns.forall { oppPawn =>
            if (color.white) oppPawn.rank.value <= pawnSq.rank.value
            else oppPawn.rank.value >= pawnSq.rank.value
          }
        }
      }
    }
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

      case m: Motif.Opposition =>
        val fileDiff = (m.ownKingSquare.file.value - m.opponentKingSquare.file.value).abs
        val rankDiff = (m.ownKingSquare.rank.value - m.opponentKingSquare.rank.value).abs
        Some(
          Fact.Opposition(
            king = m.ownKingSquare,
            enemyKing = m.opponentKingSquare,
            distance = Math.max(fileDiff, rankDiff),
            isDirect = m.oppType == Motif.OppositionType.Direct,
            oppositionType = m.oppType.toString,
            scope = scope
          )
        )

      case m: Motif.Zugzwang =>
        Some(Fact.Zugzwang(m.color, scope))

      case m: Motif.Check =>
        if (m.checkType == Motif.CheckType.Double) Some(Fact.DoubleCheck(List(m.targetSquare), scope)) // Simplified
        else Some(Fact.TargetPiece(m.targetSquare, chess.King, List(m.targetSquare), Nil, scope))

      case m: Motif.PawnPromotion =>
        Some(Fact.PawnPromotion(Square.at(m.file.value, if (m.color.white) 7 else 0).getOrElse(Square.A1), Some(m.promotedTo), scope))

      case m: Motif.PassedPawnPush =>
        Some(Fact.PawnPromotion(Square.at(m.file.value, m.toRank - 1).getOrElse(Square.A1), None, scope))

      /*
      case m: Motif.StalemateThreat =>
        board.kingPosOf(m.color).map(k => Fact.StalemateThreat(k, scope))
      */

      case _: Motif.DoubleCheck =>
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
