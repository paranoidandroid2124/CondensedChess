package lila.llm.analysis

import chess.*
import chess.Bitboard
import chess.Bitboard.*
import lila.llm.model.authoring.*

/**
 * Generates concrete candidates for Latent Seeds based on the current position.
 * This centralizes logic that was previously hardcoded or missing in LatentPlanSeeder.
 */
object SeedMoveGenerator:

  def generate(seed: LatentSeed, pos: Position, us: Color): List[MovePattern] =
    seed.family match
      case SeedFamily.Pawn         => genPawnMoves(seed, pos, us)
      case SeedFamily.Piece        => genPieceManeuvers(seed, pos, us)
      case SeedFamily.Structure    => genStructureMoves(seed, pos, us)
      case SeedFamily.Exchange     => genExchanges(seed, pos, us)
      case SeedFamily.Prophylaxis  => genProphylaxis(seed, pos, us)
      case SeedFamily.TacticalPrep => genTacticalPrep(seed, pos, us)
  // 1. Pawn Logic (Levers, Storms, Hook Attacks)
  private def genPawnMoves(seed: LatentSeed, pos: Position, us: Color): List[MovePattern] =
    seed.id match
      case "Attack_The_Hook" =>
        // 1. Identify the hook (g3/g6 pawn)
        val hookRank = if us.white then Rank.Sixth else Rank.Third
        val hookFile = File.G
        val hookSq = Square(hookFile, hookRank)
        
        // Check if opponent has pawn on hook square
        val hasHook = pos.board.pieceAt(hookSq).contains(Piece(!us, Pawn))
        
        // 2. Identify our h-pawn pusher
        if hasHook then
          val hFile = File.H
          // Check if we have h-pawn
          val pawns = (pos.board.pawns & pos.board.byColor(us)) & Bitboard.file(hFile)
          if pawns.nonEmpty then List(MovePattern.PawnAdvance(hFile)) else Nil
        else Nil

      case _ =>
        // Default: use the pattern defined in the seed
        seed.candidateMoves.collect {
          case mp @ MovePattern.PawnAdvance(file) if hasPawnOnFile(pos, us, file) => mp
          case mp @ MovePattern.PawnLever(from, _) if hasPawnOnFile(pos, us, from) => mp
        }

  private def hasPawnOnFile(pos: Position, color: Color, file: File): Boolean =
    ((pos.board.pawns & pos.board.byColor(color)) & Bitboard.file(file)).nonEmpty
  // 2. Piece Logic (Maneuvers, Outposts, Batteries)
  private def genPieceManeuvers(seed: LatentSeed, pos: Position, us: Color): List[MovePattern] =
    seed.id match
      case "KnightOutpost_Route" =>
        val enemyPawns = pos.board.pawns & pos.board.byColor(!us)
        val ourPawns = pos.board.pawns & pos.board.byColor(us)
        val outposts = getOutpostSquares(pos, us, ourPawns, enemyPawns)
        val knights = (pos.board.knights & pos.board.byColor(us)).squares
        
        outposts.flatMap { target =>
          if knights.exists(k => routeExists(Knight, k, target, pos.board)) then
            List(MovePattern.PieceTo(Knight, target))
          else Nil
        }

      case "Battery_Formation" =>
         val pieces = pos.board.piecesOf(us)
         val hasQ = pieces.exists(_._2.role == Queen)
         val hasB = pieces.exists(_._2.role == Bishop)
         val hasR = pieces.exists(_._2.role == Rook)
         
         if (hasQ && hasB) || (hasQ && hasR) || (pieces.count(_._2.role == Rook) >= 2) then
           List(MovePattern.BatteryFormation(Queen, Bishop, LineType.DiagonalLine)) // Generic placeholder
         else Nil

      case "RookLift_Kingside" =>
        val rooks = (pos.board.rooks & pos.board.byColor(us)).squares
        val targetRank = if us.white then Rank.Third else Rank.Sixth
        val candidates = Bitboard.rank(targetRank).squares.filter(sq => pos.board.pieceAt(sq).isEmpty)
        
        rooks.flatMap { r =>
          candidates.flatMap { t =>
            if routeExists(Rook, r, t, pos.board) then List(MovePattern.PieceTo(Rook, t)) else Nil
          }
        }.take(2)

      case "BadBishop_Reroute" =>
        val bishops = (pos.board.bishops & pos.board.byColor(us)).squares
        val occ = pos.board.occupied
        
        bishops.flatMap { bSq =>
           // Current mobility
           val curMobility = bSq.bishopAttacks(occ).count
           
           // Heuristic: Try to get to center or diagonals.
           val candidates = chess.Square.all.filter { sq => 
              sq != bSq && pos.board.pieceAt(sq).isEmpty && 
              sq.bishopAttacks(occ).count > curMobility + 2
           }
           
           candidates.take(2).flatMap { tSq =>
              if routeExists(Bishop, bSq, tSq, pos.board) then List(MovePattern.PieceTo(Bishop, tSq)) else Nil
           }
        }.take(1)

      case _ => Nil
  // 3. Exchange Logic
  private def genExchanges(seed: LatentSeed, pos: Position, us: Color): List[MovePattern] =
    seed.id match
      case "Trade_BadBishop" =>
        val bishops = (pos.board.bishops & pos.board.byColor(us)).squares
        val targets = (pos.board.knights | pos.board.bishops) & pos.board.byColor(!us)
        
        bishops.flatMap { bSq =>
          targets.squares.flatMap { tSq =>
             if pos.board.attackers(tSq, us).contains(bSq) then 
               val role = pos.board.pieceAt(tSq).map(_.role).getOrElse(Bishop)
               List(MovePattern.Exchange(role, tSq))
             else Nil
          }
        }

      case "Trade_Queens_Defensive" | "Simplify_To_Endgame" =>
        val ourQ = (pos.board.queens & pos.board.byColor(us)).squares.headOption
        val theirQ = (pos.board.queens & pos.board.byColor(!us)).squares.headOption
        
        (ourQ, theirQ) match
          case (Some(usQ), Some(themQ)) =>
             if pos.board.attackers(themQ, us).contains(usQ) then
               List(MovePattern.Exchange(Queen, themQ))
             else Nil
          case _ => Nil

      case _ => Nil
  // 4. Tactical Preparation
  private def genTacticalPrep(seed: LatentSeed, pos: Position, us: Color): List[MovePattern] =
    seed.id match
      case "Prepare_Overload" =>
        val enemyPiecesArr = pos.board.byColor(!us).squares
        val overloaded = enemyPiecesArr.filter { defenderSq =>
           val protects = enemyPiecesArr.filter { targetSq =>
              targetSq != defenderSq && pos.board.attackers(targetSq, !us).contains(defenderSq)
           }
           protects.size >= 2
        }
        
        overloaded.headOption.flatMap { defenderSq =>
           chess.Square.all.find(sq => pos.board.pieceAt(sq).exists(_.color == us) && pos.board.attackers(defenderSq, us).isEmpty).map { _ =>
              MovePattern.PieceTo(Rook, defenderSq)
           }
        }.toList

      case "Prepare_Clearance" =>
         val piecesList = pos.board.piecesOf(us).toList
         val linePieces = piecesList.filter(p => p._2.role == Rook || p._2.role == Bishop)
         
         val results = linePieces.flatMap { case (lpSq, lp) =>
            val targetsArr = pos.board.byColor(!us).squares
            targetsArr.flatMap { tSq =>
               val path = Bitboard.between(lpSq, tSq)
               val blockers = path & pos.board.occupied
               if blockers.count == 1 then
                  val blockerSq = blockers.squares.head
                  if pos.board.pieceAt(blockerSq).exists(_.color == us) then
                     Some(MovePattern.PieceTo(pos.board.pieceAt(blockerSq).get.role, blockerSq))
                  else None
               else None
            }
         }
         results.take(1)

      case _ => Nil
  // 5. Structure & Prophylaxis
  private def genStructureMoves(seed: LatentSeed, pos: Position, us: Color): List[MovePattern] = 
    seed.id match
      case "CreatePassedPawn" =>
        val flanks = List(
           (List(File.A, File.B, File.C), "Queenside"),
           (List(File.F, File.G, File.H), "Kingside")
        )
        
        flanks.flatMap { case (files, _) =>
           val fileMask = files.foldLeft(Bitboard.empty)((acc, f) => acc | Bitboard.file(f))
           val ourCount = (pos.board.pawns & pos.board.byColor(us) & fileMask).count
           val theirCount = (pos.board.pawns & pos.board.byColor(!us) & fileMask).count
           
           if ourCount > theirCount then
             val ourPawnsOnFlank = (pos.board.pawns & pos.board.byColor(us) & fileMask).squares.sortBy(s => if us.white then -s.rank.value else s.rank.value)
             ourPawnsOnFlank.headOption.map(s => MovePattern.PawnAdvance(s.file)).toList
           else Nil
        }

      case "FixBackwardPawn" =>
        val theirPawns = pos.board.pawns & pos.board.byColor(!us)
        val theirPawnSquares = theirPawns.squares
        theirPawnSquares.flatMap { pSq =>
           val stopSquareOpt = if us.white then Square.at(pSq.file.value, pSq.rank.value - 1)
                               else Square.at(pSq.file.value, pSq.rank.value + 1)

           stopSquareOpt.toList.flatMap { stopSq =>
              if pos.board.pieceAt(stopSq).isEmpty then List(MovePattern.PieceTo(Knight, stopSq)) else Nil
           }
        }.take(1)

      case "CreateIQP" => 
        val capturesList = pos.board.piecesOf(us).toList.flatMap { case (usSq, piece) =>
           val l_attacks = piece.role match
             case Knight => usSq.knightAttacks
             case Bishop => usSq.bishopAttacks(pos.board.occupied)
             case Rook   => usSq.rookAttacks(pos.board.occupied)
             case Queen  => usSq.queenAttacks(pos.board.occupied)
             case King   => usSq.kingAttacks
             case Pawn   => usSq.pawnAttacks(us)
           
           l_attacks.squares.filter(tSq => pos.board.pieceAt(tSq).exists(_.color == !us))
        }
        
        capturesList.flatMap { tSq =>
           if pos.board.pieceAt(tSq).exists(_.role == Pawn) then
              val file = tSq.file
              val neighbors = List(File.all.lift(file.value - 1), File.all.lift(file.value + 1)).flatten
              val hasNeighborPawns = neighbors.exists(f => (pos.board.pawns & pos.board.byColor(!us) & Bitboard.file(f)).nonEmpty)
              if !hasNeighborPawns then List(MovePattern.Exchange(Pawn, tSq)) else Nil
           else Nil
        }.take(1)

      case _ => Nil

  private def genProphylaxis(seed: LatentSeed, pos: Position, us: Color): List[MovePattern] = 
    seed.id match
      case "Prophylaxis_Luft" =>
        val pushes = 
           if us.white then List(MovePattern.PawnAdvance(File.H), MovePattern.PawnAdvance(File.G))
           else List(MovePattern.PawnAdvance(File.H), MovePattern.PawnAdvance(File.G))
        pushes.filter { 
           case MovePattern.PawnAdvance(f) => hasPawnOnFile(pos, us, f) && pos.board.pieceAt(Square(f, if us.white then Rank.Third else Rank.Sixth)).isEmpty
           case _ => false
        }
        
      case "Restrict_OpponentPiece" =>
         val activeEnemyRank = if us.white then Bitboard.rank(Rank.Fourth) | Bitboard.rank(Rank.Fifth)
                               else Bitboard.rank(Rank.Fifth) | Bitboard.rank(Rank.Fourth)
         val enemyPiecesBb = (pos.board.knights | pos.board.bishops | pos.board.queens) & pos.board.byColor(!us) & activeEnemyRank
         
         enemyPiecesBb.squares.flatMap { eSq =>
            val attackSqs = eSq.pawnAttacks(!us).squares
            attackSqs.flatMap { aSq =>
               val pawnFile = aSq.file
               val ourPawn = (pos.board.pawns & pos.board.byColor(us) & Bitboard.file(pawnFile)).squares.headOption
               ourPawn.flatMap { pSq =>
                  val isBelow = if us.white then pSq.rank.value < aSq.rank.value else pSq.rank.value > aSq.rank.value
                  if isBelow && pos.board.pieceAt(aSq).isEmpty then
                     Some(MovePattern.PawnAdvance(pawnFile))
                  else None
               }
            }
         }.take(1)
         
      case "KingSafety_Run" =>
         val kingSqOpt = (pos.board.kings & pos.board.byColor(us)).squares.headOption
         kingSqOpt.flatMap { kSq =>
             if us.white then
                if kSq == Square.G1 then Some(MovePattern.PieceTo(King, Square.H1))
                else if kSq == Square.B1 then Some(MovePattern.PieceTo(King, Square.A1))
                else None
             else
                if kSq == Square.G8 then Some(MovePattern.PieceTo(King, Square.H8))
                else if kSq == Square.B8 then Some(MovePattern.PieceTo(King, Square.A8))
                else None
         }.toList

      case _ => Nil

  // --- Helpers ---
  
  private def getOutpostSquares(pos: Position, us: Color, ourPawns: Bitboard, enemyPawns: Bitboard): List[Square] =
    val relevantRanks = if us.white then Bitboard.rank(Rank.Fourth) | Bitboard.rank(Rank.Fifth) | Bitboard.rank(Rank.Sixth)
                        else Bitboard.rank(Rank.Fifth) | Bitboard.rank(Rank.Fourth) | Bitboard.rank(Rank.Third)
    
    var ourPawnAttacksBb = Bitboard.empty
    (pos.board.pawns & pos.board.byColor(us)).foreach { sq =>
      ourPawnAttacksBb = ourPawnAttacksBb | sq.pawnAttacks(us)
    }
    
    relevantRanks.squares.filter { sq =>
      val supported = pos.board.attackers(sq, us).exists(s => pos.board.roleAt(s).contains(Pawn))
      val enemyPawnControl = pos.board.attackers(sq, !us).exists(s => pos.board.roleAt(s).contains(Pawn))
      supported && !enemyPawnControl && pos.board.pieceAt(sq).forall(_.color == us)
    }

  private def routeExists(role: Role, from: Square, to: Square, board: Board): Boolean =
    val maxDepth = 4
    val queue = scala.collection.mutable.Queue((from, 0))
    val visited = scala.collection.mutable.Set(from)
    val ourColor = board.pieceAt(from).map(_.color).getOrElse(Color.White)
    val occ = board.occupied
    
    while queue.nonEmpty do
      val (curr, depth) = queue.dequeue()
      if curr == to then return true
      if depth < maxDepth then
        val l_attacks: Bitboard = role match
          case Knight => curr.knightAttacks
          case King   => curr.kingAttacks
          case Bishop => curr.bishopAttacks(occ)
          case Rook   => curr.rookAttacks(occ)
          case Queen  => curr.queenAttacks(occ)
          case Pawn   => Bitboard.empty 
        
        l_attacks.foreach { dest =>
          if !visited.contains(dest) then
             val isFriendly = board.pieceAt(dest).exists(_.color == ourColor)
             if !isFriendly || dest == to then  
               if !isFriendly then
                 visited += dest
                 queue.enqueue((dest, depth + 1))
        }
    false
