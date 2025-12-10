package chess
package analysis

import chess.format.Uci
import chess.{ Color, Position, Square }
import chess.analysis.FeatureExtractor.PositionFeatures
import chess.{ Pawn, Knight, Bishop, Rook } // Import Roles directly

object MoveGenerator:

  enum CandidateType(val name: String):
    case EngineBest extends CandidateType("engine_best")
    case EngineSecond extends CandidateType("engine_second")
    case EngineThird extends CandidateType("engine_third")
    case CentralBreak extends CandidateType("central_break")
    case QueensideMajority extends CandidateType("queenside_majority")
    case KingsidePawnStorm extends CandidateType("kingside_pawn_storm")
    case RookLift extends CandidateType("rook_lift")
    case PieceImprovement extends CandidateType("piece_improvement")
    case Recapture extends CandidateType("recapture")
    case SacrificeProbe extends CandidateType("sacrifice_probe")
    case TacticalCheck extends CandidateType("tactical_check")
    case Fork extends CandidateType("Fork")
    case Pin extends CandidateType("Pin")
    case Skewer extends CandidateType("Skewer")
    case DiscoveredAttack extends CandidateType("DiscoveredAttack")
    case Unknown extends CandidateType("unknown")

  case class CandidateMove(
      uci: String,
      san: String,
      candidateType: CandidateType,
      priority: Double,
      explanation: String = "" // Optional reason
  )

  // Main Entry Point
  def generateCandidates(
      position: Position,
      features: PositionFeatures,
      engineMoves: List[(Uci, Double)] = Nil // Optional engine suggestions (UCI, Score)
  ): List[CandidateMove] =
    
    val candidates = List.newBuilder[CandidateMove]

    // 1. Engine Candidates (Top 3)
    engineMoves.zipWithIndex.foreach { case ((uci, score), idx) =>
      val (cType, priority) = idx match
        case 0 => (CandidateType.EngineBest, 1.0)
        case 1 => (CandidateType.EngineSecond, 0.9)
        case _ => (CandidateType.EngineThird, 0.8)
      candidates += CandidateMove(uci.uci, "", cType, priority, "Engine suggestion")
    }

    // 2. Rule-based Candidates
    candidates ++= computeStructureCandidates(position, features)
    candidates ++= computeActivityCandidates(position, features)
    candidates ++= computeTacticalCandidates(position, features)

    // Deduplicate: Keep highest priority for same UCI
    // Sort by priority desc
    candidates.result()
      .groupBy(_.uci)
      .map { case (_, moves) => moves.maxBy(_.priority) }
      .toList
      .sortBy(-_.priority)
      .take(10) // Limit to top N

  // --- Internal Generators ---

  private def computeStructureCandidates(position: Position, features: PositionFeatures): List[CandidateMove] =
    val moves = List.newBuilder[CandidateMove]
    val us = position.color
    val pawns = features.pawns

    // A. Central Breaks (d4-d5, e4-e5 etc.)
    // Check if we have central pawns and if pushing them is legal
    // Central files: d(3), e(4)
    // "Break" usually means pawn interaction or advancing into enemy territory (rank 4->5 or 5->6?)
    // Simplified: Any central pawn push.
    val breaks = position.legalMoves.filter { m => 
      m.piece.role == Pawn && (m.orig.file.value == 3 || m.orig.file.value == 4)
    }
    breaks.foreach { m =>
      // Refined Break Logic: Pawn capture or advance to Rank 4/5 (for White)
      val isCapture = m.captures
      val toRank = m.dest.rank.value
      val isDeepAdvance = if us == Color.White then toRank >= 4 else toRank <= 3
      
      if isCapture || isDeepAdvance then
         moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.CentralBreak, 0.9, "Central Break") 
    }

    // B. Minority Attack (Queenside)
    // If features say MinorityAttackReady, look for a/b pawn pushes
    val minorityReady = if us == Color.White then pawns.whiteMinorityAttackReady else pawns.blackMinorityAttackReady
    if minorityReady then
       val qPushes = position.legalMoves.filter { m =>
         val isQueenside = m.orig.file.value <= 2 // a,b,c
         m.piece.role == Pawn && isQueenside
       }
       qPushes.foreach { m =>
         moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.QueensideMajority, 0.85, "Minority Attack Push")
       }

    // C. Pawn Storm (Kingside)
    // Basic heuristic: if opposite castling data available or just general Kingside expansion
    // For now, if kingside pawns advance
    val kPushes = position.legalMoves.filter { m =>
         val isKingside = m.orig.file.value >= 5 // f,g,h
         m.piece.role == Pawn && isKingside
    }
    // Only verify if this makes sense (e.g. Activity says "Attacking")
    // For now add with lower priority
    // moves += ...

    moves.result()

  private def computeActivityCandidates(position: Position, features: PositionFeatures): List[CandidateMove] =
    val moves = List.newBuilder[CandidateMove]
    val us = position.color

    // A. Rook Lifts
    // Rook moves to 3rd/4th/5th rank (intended for attack)
    val rooks = position.legalMoves.filter(_.piece.role == Rook)
    rooks.foreach { m =>
      val r = m.dest.rank.value // 0-7
      val rankTarget = if us == Color.White then (r == 2 || r == 3 || r == 4) else (r == 5 || r == 4 || r == 3)
      // Allow any move to lift ranks
      if rankTarget then
        moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.RookLift, 0.75, "Rook Lift")
    }

    // B. Piece Improvement (Minor Pieces)
    // Move towards center or outpost
    // Center: d4/e4/d5/e5
    // Outpost: defined in features (but we need to know WHICH square). 
    // Heuristic: Move to Central 4 squares
    val minors = position.legalMoves.filter(m => m.piece.role == Knight || m.piece.role == Bishop)
    minors.foreach { m =>
      val isCenter = m.dest.file.value >= 2 && m.dest.file.value <= 5 && m.dest.rank.value >= 2 && m.dest.rank.value <= 5
      // Improving: moving from non-center to center?
      val wasCenter = m.orig.file.value >= 2 && m.orig.file.value <= 5 && m.orig.rank.value >= 2 && m.orig.rank.value <= 5
      
      if isCenter && !wasCenter then
        moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.PieceImprovement, 0.65, "Centralizing")
    }

    moves.result()

  private def computeTacticalCandidates(position: Position, features: PositionFeatures): List[CandidateMove] =
    val moves = List.newBuilder[CandidateMove]
    val us = position.color
    val board = position.board
    val enemyKingOpt = board.kingPosOf(!us)
    val enemyQueens = board.queens & board.byColor(!us)
    val enemies = board.byColor(!us)
    val occupied = board.occupied
    
    // A. Fork Detection (Knights attacking 2+ valuable pieces)
    val knightMoves = position.legalMoves.filter(_.piece.role == Knight)
    knightMoves.foreach { m =>
      val destAttacks = m.dest.knightAttacks & enemies
      // Count valuable targets: King, Queen, Rook, or undefended minor
      val valuableTargets = destAttacks.squares.count { sq =>
        board.roleAt(sq).exists { role =>
          role == King || role == Queen || role == Rook ||
          (role == Knight || role == Bishop) && board.attackers(sq, us).nonEmpty // Undefended minors
        }
      }
      if valuableTargets >= 2 then
        moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.Fork, 0.95, "Knight Fork")
    }
    
    // B. Pin Detection (Bishop/Rook/Queen pinning piece to King)
    val sliders = position.legalMoves.filter(m => m.piece.role == Bishop || m.piece.role == Rook || m.piece.role == Queen)
    sliders.foreach { m =>
      enemyKingOpt.foreach { enemyKing =>
        // Check if dest is on line between an enemy piece and enemy King
        val onLine = m.dest.onSameFile(enemyKing) || m.dest.onSameRank(enemyKing) || m.dest.onSameDiagonal(enemyKing)
        if onLine then
          // Get all squares between dest and enemyKing
          val between = Bitboard.between(m.dest, enemyKing)
          val piecesInBetween = between & enemies
          // If exactly one piece is in between, it's a potential pin
          if piecesInBetween.count == 1 then
            // Verify our piece can actually attack along this line
            val canAttack = m.piece.role match
              case Bishop => m.dest.onSameDiagonal(enemyKing)
              case Rook => m.dest.onSameFile(enemyKing) || m.dest.onSameRank(enemyKing)
              case Queen => true
              case _ => false
            if canAttack then
              moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.Pin, 0.9, "Pin")
      }
    }
    
    // B.2 Skewer Detection (Reversed Pin: attack high-value piece, lower-value behind)
    sliders.foreach { m =>
      val destOccupant = board.roleAt(m.dest)
      val isHighValueTarget = destOccupant.exists(r => r == King || r == Queen || r == Rook)
      if isHighValueTarget && m.capture.isDefined then
        val sliderRole = m.piece.role
        val rayAttacks = sliderRole match
          case Bishop => m.dest.bishopAttacks(occupied ^ m.dest.bb) & enemies
          case Rook => m.dest.rookAttacks(occupied ^ m.dest.bb) & enemies
          case Queen => m.dest.queenAttacks(occupied ^ m.dest.bb) & enemies
          case _ => Bitboard.empty
        if rayAttacks.nonEmpty then
          moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.Skewer, 0.88, "Skewer")
    }
    
    // C. Discovered Attack (Moving a piece unblocks an attack from behind)
    // Logic: For each move, check if a friendly slider (B/R/Q) on same line as orig
    // would gain attack on valuable enemy target after the moving piece clears the way.
    val friendlySliders = (board.bishops | board.rooks | board.queens) & board.byColor(us)
    position.legalMoves.foreach { m =>
      val orig = m.orig
      friendlySliders.squares.foreach { sliderSq =>
        if sliderSq != orig then // Don't check the moving piece itself if it's a slider
          val onSameLine = orig.onSameFile(sliderSq) || orig.onSameRank(sliderSq) || orig.onSameDiagonal(sliderSq)
          if onSameLine then
            // Check if orig was blocking slider's attack
            val between = Bitboard.between(sliderSq, orig)
            val blockers = between & occupied
            if blockers.isEmpty then // orig was the only blocker
              // Check what slider would attack beyond orig
              val sliderRole = board.roleAt(sliderSq)
              val beyondAttacks = sliderRole match
                case Some(Bishop) => orig.bishopAttacks(occupied ^ orig.bb) & enemies // Attacks with orig removed
                case Some(Rook) => orig.rookAttacks(occupied ^ orig.bb) & enemies
                case Some(Queen) => orig.queenAttacks(occupied ^ orig.bb) & enemies
                case _ => Bitboard.empty
              // Check for valuable targets
              val valuableHit = beyondAttacks.squares.exists { sq =>
                board.roleAt(sq).exists(r => r == Queen || r == Rook || r == King)
              }
              if valuableHit then
                moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.DiscoveredAttack, 0.92, "Discovered Attack")
      }
    }
    
    // D. Captures (Forcing moves) - Original logic
    val tacticals = position.legalMoves.filter { m => 
      m.capture.isDefined && !knightMoves.contains(m) // Avoid double-tagging forks
    }
    tacticals.foreach { m =>
      val prio = 0.6
      moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.SacrificeProbe, prio, "Tactical (Capture)")
    }
    
    // E. Recaptures - Original logic
    position.history.lastMove.foreach { lastUci => 
       val targetOpt = lastUci match
         case Uci.Move(_, dest, _) => Some(dest)
         case Uci.Drop(_, dest)    => Some(dest)
         case _ => None
       
       targetOpt.foreach { target =>
         val recaptures = position.legalMoves.filter(_.dest == target)
         recaptures.foreach { m =>
           moves += CandidateMove(m.toUci.uci, m.toUci.uci, CandidateType.Recapture, 0.88, "Recapture")
         }
       }
    }

    moves.result()
