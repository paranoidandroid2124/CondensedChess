package lila.llm

import lila.llm.model.*

import _root_.chess.*
import _root_.chess.format.{ Fen, Uci }

/** Engine evaluation data (from frontend Stockfish WASM) */
case class EngineEval(
    cp: Option[Int],
    mate: Option[Int],
    pv: List[String] = Nil
):
  def score: Int = 
    cp.getOrElse(mate.map(m => if m > 0 then 10000 - m else -10000 + m).getOrElse(0))

/** Candidate move type */
enum CandidateType(val name: String):
  case EngineBest extends CandidateType("engine_best")
  case EngineSecond extends CandidateType("engine_second")
  case UserMove extends CandidateType("user_move")
  case Fork extends CandidateType("fork")
  case Pin extends CandidateType("pin")
  case Capture extends CandidateType("capture")
  case Check extends CandidateType("check")
  case CentralBreak extends CandidateType("central_break")
  case PieceImprovement extends CandidateType("piece_improvement")

/** Candidate move for analysis */
case class CandidateMove(
    uci: String,
    candidateType: CandidateType,
    priority: Double,
    reason: String = ""
)

/** Counterfactual comparison result */
case class CounterfactualMatch(
    userMove: String,
    bestMove: String,
    cpLoss: Int,
    missedMotifs: List[Motif],
    userMoveMotifs: List[Motif],
    severity: String // "blunder" | "mistake" | "inaccuracy" | "ok"
)

/** Hypothesis - human-like candidate move */
case class Hypothesis(
    move: String,
    candidateType: CandidateType,
    rationale: String,
    featuresDelta: Map[String, Int],
    eval: Option[EngineEval]
)

/**
 * Unified Move Analyzer
 * 
 * Combines:
 * - MotifTokenizer: Detects tactical/structural motifs from PV lines
 * - ExperimentExecutor: Runs counterfactual experiments comparing moves
 * 
 * This is the single source of truth for move sequence analysis.
 */
object MoveAnalyzer:

  // ============================================================
  // MOTIF TOKENIZATION (from MotifTokenizer)
  // ============================================================

  /**
   * Translates a PV (list of UCI moves) into a list of Motifs.
   */
  def tokenizePv(initialFen: String, pv: List[String]): List[Motif] =
    Fen.read(chess.variant.Standard, Fen.Full(initialFen)).map { initialPos =>
      val (_, motifs) = pv.zipWithIndex.foldLeft((initialPos, List.empty[Motif])) {
        case ((pos, acc), (uciStr, index)) =>
          Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(pos.move(_).toOption) match
            case Some(mv) =>
              val moveMotifs = detectMoveMotifs(mv, pos, index)
              // State motifs are detected only at the final position to avoid massive redundancy
              val stateMotifs = if (index == pv.length - 1) detectStateMotifs(mv.after, index) else Nil
              (mv.after, acc ++ moveMotifs ++ stateMotifs)
            case None => 
              (pos, acc)
      }
      motifs
    }.getOrElse(Nil)

  // ============================================================
  // COUNTERFACTUAL ANALYSIS
  // ============================================================

  /**
   * Compare user move against engine's best move.
   * Returns counterfactual match with missed opportunities.
   */
  def compareMove(
      fen: String,
      userMove: String,
      bestMove: String,
      userEval: Int,
      bestEval: Int
  ): Option[CounterfactualMatch] =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map { pos =>
      val cpLoss = bestEval - userEval
      
      // Tokenize both lines (just the first move for now)
      val bestMotifs = tokenizePv(fen, List(bestMove))
      val userMotifs = tokenizePv(fen, List(userMove))
      
      // Missed motifs = in best but not in user
      val missedMotifs = bestMotifs.filterNot(bm => 
        userMotifs.exists(um => um.getClass == bm.getClass)
      )
      
      val severity = 
        if cpLoss >= 300 then "blunder"
        else if cpLoss >= 100 then "mistake"
        else if cpLoss >= 50 then "inaccuracy"
        else "ok"
      
      CounterfactualMatch(
        userMove = userMove,
        bestMove = bestMove,
        cpLoss = cpLoss,
        missedMotifs = missedMotifs,
        userMoveMotifs = userMotifs,
        severity = severity
      )
    }


  // ============================================================
  // HYPOTHESIS GENERATION (Human-like Candidates)
  // ============================================================

  /**
   * Generates "human-like" candidate moves that might be mistakes.
   * Used for counterfactual analysis (e.g., "Why not capture here?").
   */
  def generateHypotheses(
      fen: String,
      engineBestMove: String,
      currentEval: Int // CP
  ): List[Hypothesis] =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map { pos =>
      val legalMoves = pos.legalMoves
      val candidates = List.newBuilder[Hypothesis]

      // 1. "The Trap" / Natural Captures
      // Moves that capture material but might be bad
      legalMoves.filter(_.captures).foreach { mv =>
         val uci = mv.toUci.uci
         if uci != engineBestMove then
           val role = mv.piece.role
           val victim = pos.board.roleAt(mv.dest).getOrElse(Pawn)
           // If it looks like a free piece or favorable trade
           if pieceValue(victim) >= pieceValue(role) then
             candidates += Hypothesis(
               move = uci,
               candidateType = CandidateType.Capture,
               rationale = s"Captures the ${victim.name}",
               featuresDelta = Map.empty, // To be filled if analyzed deeper
               eval = None
             )
      }

      // 2. "Checks"
      // Checks are always tempting
      legalMoves.filter(m => m.after.check.yes).foreach { mv =>
        val uci = mv.toUci.uci
        if uci != engineBestMove then
          candidates += Hypothesis(
            move = uci,
            candidateType = CandidateType.Check,
            rationale = "Forcing check",
            featuresDelta = Map.empty,
            eval = None
          )
      }

      // 3. "Recaptures" (Retaking)
      // If the last move was a capture by opponent, we usually want to recapture
      // (This requires knowing the last move, which we don't have in pure FEN. 
      //  We can infer 'to' square from previous position if available, but for now skip.)

      // 4. "Tension Release" (Pawn Breaks)
      // If there are pawn tension, resolving it is natural
      // (Simplified logic: Pawn captures are already covered above)

      candidates.result().take(3) // Limit to top 3 most "obvious" candidates
    }.getOrElse(Nil)

    // ============================================================
  // MOVE MOTIF DETECTION (from MotifTokenizer)
  // ============================================================

  private def detectMoveMotifs(mv: Move, pos: Position, plyIndex: Int): List[Motif] =
    val color = pos.color
    val san = mv.toSanStr.toString
    val nextPos = mv.after

    List.concat(
      detectPawnMotifs(mv, pos, color, san, plyIndex),
      detectPieceMotifs(mv, pos, color, san, plyIndex),
      detectKingMotifs(mv, pos, color, san, plyIndex),
      detectTacticalMotifs(mv, pos, nextPos, color, san, plyIndex),
      detectThreatMotifs(mv, pos, nextPos, color, san, plyIndex)
    )

  // ============================================================
  // MULTI-PV VARIATION ANALYSIS
  // ============================================================

  /**
   * Analyze a list of variation lines, adding motifs and tags.
   */
  def analyzeVariations(
      fen: String, 
      lines: List[VariationLine],
      threat: Option[VariationLine] = None
  ): List[VariationLine] =
    lines.map { line =>
      val motifs = tokenizePv(fen, line.moves)
      val newTags = detectVariationTags(line, motifs, threat)
      line.copy(tags = (line.tags ++ newTags).distinct)
    }

  private def detectVariationTags(
      line: VariationLine, 
      motifs: List[Motif], 
      threat: Option[VariationLine]
  ): List[VariationTag] =
    val builder = List.newBuilder[VariationTag]
    
    // Check for tactical complexity
    val tacticsCount = motifs.count {
      case _: CheckMotif | _: ForkMotif | _: PinMotif => true
      case m: CaptureMotif if m.captureType != CaptureType.Normal => true
      case _ => false
    }

    if tacticsCount >= 2 then builder += VariationTag.Sharp
    else if line.moves.length > 5 && tacticsCount == 0 then builder += VariationTag.Solid

    // Prophylaxis: Significantly improves score compared to the threat (Null Move score)
    // Heuristic: If threat score is bad < -100 (from our perspective) and this line is > -50
    // Or simply if score delta is large (> 150cp)
    threat.foreach { t =>
      // threat.scoreCp is from our perspective (e.g. if we skipped move, opponent kills us => low score)
      // Actually, standard null move returns score from opponent perspective?
      // Let's assume AnalysisModels normalized it. If threat is -500 (we are losing)
      // and line is 0 (equal), then we prevented it.
      if t.scoreCp < -100 && line.scoreCp > t.scoreCp + 100 then
        builder += VariationTag.Prophylaxis
    }

    // Simplification: Winning score (> 200) + Excanges
    if line.scoreCp > 200 then
      val captures = motifs.count(_.isInstanceOf[CaptureMotif])
      val exchanges = motifs.count {
        case m: CaptureMotif => m.captureType == CaptureType.Exchange
        case _ => false
      }
      if captures > 0 || exchanges > 0 then
        builder += VariationTag.Simplification

    builder.result()

  private def detectPawnMotifs(mv: Move, pos: Position, color: Color, san: String, plyIndex: Int): List[Motif] =
    if mv.piece.role != Pawn then return Nil
    
    var motifs = List.empty[Motif]

    // Pawn Advance (non-capture)
    if !mv.captures then
      motifs = motifs :+ PawnAdvance(
        file = mv.dest.file,
        fromRank = mv.orig.rank.value + 1,
        toRank = mv.dest.rank.value + 1,
        color = color,
        plyIndex = plyIndex,
        move = Some(san)
      )

    // Pawn Break (capture)
    if mv.captures then
      motifs = motifs :+ PawnBreak(
        file = mv.orig.file,
        targetFile = mv.dest.file,
        color = color,
        plyIndex = plyIndex,
        move = Some(san)
      )

    // Pawn Promotion
    mv.promotion.foreach { promotedTo =>
      motifs = motifs :+ PawnPromotion(
        file = mv.dest.file,
        promotedTo = promotedTo,
        color = color,
        plyIndex = plyIndex,
        move = Some(san)
      )
    }

    motifs

  private def detectPieceMotifs(mv: Move, pos: Position, color: Color, san: String, plyIndex: Int): List[Motif] =
    var motifs = List.empty[Motif]
    val role = mv.piece.role

    // Rook Lift
    if role == Rook then
      val backRankValue = if color.white then 0 else 7
      if mv.orig.rank.value == backRankValue && mv.dest.rank.value != backRankValue then
        motifs = motifs :+ RookLift(
          file = mv.dest.file,
          fromRank = mv.orig.rank.value + 1,
          toRank = mv.dest.rank.value + 1,
          color = color,
          plyIndex = plyIndex,
          move = Some(san)
        )

    // Fianchetto
    if role == Bishop then
      val fianchettoSquares = if color.white then List(Square.G2, Square.B2) else List(Square.G7, Square.B7)
      if fianchettoSquares.contains(mv.dest) then
        val side = if mv.dest.file == File.G then FianchettoSide.Kingside else FianchettoSide.Queenside
        motifs = motifs :+ Fianchetto(side, color, plyIndex, Some(san))

    // Centralization
    val centralSquares = List(Square.D4, Square.D5, Square.E4, Square.E5)
    if (role == Knight || role == Bishop) && centralSquares.contains(mv.dest) then
      motifs = motifs :+ Centralization(role, mv.dest, color, plyIndex, Some(san))

    // Outpost
    if role == Knight then
      val isInEnemyTerritory = if color.white then mv.dest.rank.value >= 4 else mv.dest.rank.value <= 3
      if isInEnemyTerritory then
        motifs = motifs :+ Outpost(role, mv.dest, color, plyIndex, Some(san))

    motifs

  private def detectKingMotifs(mv: Move, pos: Position, color: Color, san: String, plyIndex: Int): List[Motif] =
    if mv.piece.role != King then return Nil
    var motifs = List.empty[Motif]
    if mv.castle.isDefined then
      val side = if mv.dest.file == File.G then CastlingSide.Kingside else CastlingSide.Queenside
      motifs = motifs :+ Castling(side, color, plyIndex, Some(san))
    else
      val stepType = determineKingStepType(mv, color)
      motifs = motifs :+ KingStep(stepType, color, plyIndex, Some(san))
    motifs

  private def determineKingStepType(mv: Move, color: Color): KingStepType =
    val relFrom = Motif.relativeRank(mv.orig.rank.value + 1, color)
    val relTo = Motif.relativeRank(mv.dest.rank.value + 1, color)
    if relFrom == 1 && relTo > 1 then KingStepType.OffBackRank
    else if relTo >= 4 then KingStepType.Activation
    else if mv.dest.file == File.A || mv.dest.file == File.H then KingStepType.ToCorner
    else KingStepType.Other

  private def detectTacticalMotifs(
      mv: Move, 
      pos: Position, 
      nextPos: Position,
      color: Color, 
      san: String, 
      plyIndex: Int
  ): List[Motif] =
    var motifs = List.empty[Motif]
    
    // Check
    if nextPos.check.yes then
      val checkers = nextPos.checkers
      val isMate = nextPos.checkMate
      val isDouble = checkers.count > 1
      val isDiscovered = checkers.exists(sq => sq != mv.dest)
      
      val checkType = if (isMate) {
        if (isSmotheredPattern(nextPos, color)) CheckType.Smothered 
        else CheckType.Mate
      } else if (isDouble) CheckType.Double
      else if (isDiscovered) CheckType.Discovered
      else CheckType.Normal
      
      val kingSq = nextPos.board.kingPosOf(!color).getOrElse(mv.dest) // Fallback to dest if king not found (unlikely)
      motifs = motifs :+ CheckMotif(mv.piece.role, kingSq, checkType, plyIndex, Some(san))
    
    // Capture
    if mv.captures then
      // mv.capture returns Option[Square] (the captured square), not the role
      // For en passant, captured pawn is on mv.capture, not mv.dest
      val capturedRole = mv.capture.flatMap(pos.board.roleAt).getOrElse(pos.board.roleAt(mv.dest).getOrElse(Pawn))
      val captureType = determineCaptureType(mv.piece.role, capturedRole)
      motifs = motifs :+ CaptureMotif(mv.piece.role, capturedRole, mv.dest, captureType, plyIndex, Some(san))
    
    // Fork detection
    val forkTargetsList = detectForkTargets(mv, nextPos, color)
    if forkTargetsList.size >= 2 then
      motifs = motifs :+ ForkMotif(mv.piece.role, forkTargetsList, mv.dest, color, plyIndex, Some(san))
    
    // Pin / Skewer detection
    detectLineTactics(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Discovered Attack detection
    detectDiscoveredTactics(mv, pos, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Overloading detection
    detectOverloading(nextPos.board, !color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Interference detection
    detectInterference(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    motifs

  private def determineCaptureType(attacker: Role, victim: Role): CaptureType =
    val val1 = pieceValue(attacker)
    val val2 = pieceValue(victim)
    if val2 < val1 then CaptureType.Sacrifice
    else if val2 == val1 then CaptureType.Exchange
    else CaptureType.Normal

  private def detectForkTargets(mv: Move, nextPos: Position, color: Color): List[Role] =
    val sq = mv.dest
    val role = mv.piece.role
    val targets: Bitboard = role match
      case Pawn   => sq.pawnAttacks(color)
      case Knight => sq.knightAttacks
      case Bishop => sq.bishopAttacks(nextPos.board.occupied)
      case Rook   => sq.rookAttacks(nextPos.board.occupied)
      case Queen  => sq.queenAttacks(nextPos.board.occupied)
      case King   => sq.kingAttacks
    (targets & nextPos.board.byColor(!color)).squares.flatMap(nextPos.board.roleAt)

  private def detectLineTactics(
      mv: Move,
      nextPos: Position,
      color: Color,
      plyIndex: Int,
      san: Option[String]
  ): List[Motif] =
    val sq = mv.dest
    val role = mv.piece.role
    if (!List(Bishop, Rook, Queen).contains(role)) return Nil

    val board = nextPos.board
    val occupied = board.occupied
    val enemyPieces = board.byColor(!color)
    val targets = role match
      case Bishop => sq.bishopAttacks(occupied) & enemyPieces
      case Rook   => sq.rookAttacks(occupied) & enemyPieces
      case Queen  => sq.queenAttacks(occupied) & enemyPieces
      case _      => Bitboard.empty

    targets.squares.flatMap { targetSq =>
      val targetRole = board.roleAt(targetSq).getOrElse(Pawn)
      val ray = Bitboard.ray(sq, targetSq)
      val occupiedBehind = occupied & ray & ~Bitboard(targetSq) & ~Bitboard(sq)
      
      // Select the nearest square such that the targetSq is between us and it
      // Manual Chebyshev distance: max(|file diff|, |rank diff|)
      def chebyshevDist(a: Square, b: Square): Int =
        Math.max((a.file.value - b.file.value).abs, (a.rank.value - b.rank.value).abs)
      
      occupiedBehind.squares.filter(s => Bitboard.between(sq, s).contains(targetSq))
        .minByOption(s => chebyshevDist(sq, s))
        .flatMap { behindSq =>
          board.roleAt(behindSq).flatMap { behindRole =>
            if (board.colorAt(behindSq).contains(!color)) {
              if (pieceValue(targetRole) < pieceValue(behindRole))
                Some(PinMotif(role, targetRole, behindRole, color, plyIndex, san))
              else if (pieceValue(targetRole) > pieceValue(behindRole))
                Some(Skewer(role, targetRole, behindRole, color, plyIndex, san))
              else None
            } else None
          }
        }
    }

  private def detectDiscoveredTactics(
      mv: Move,
      pos: Position,
      nextPos: Position,
      color: Color,
      plyIndex: Int,
      san: Option[String]
  ): List[Motif] =
    val movedPiece = mv.piece.role
    // Note: orig and dest are available via mv.orig/mv.dest if needed
    
    // A discovery happens if a line was opened from 'orig'
    // Find our long-range pieces that might be attacking through 'orig'
    val longRangePieces = pos.board.byColor(color) & (pos.board.bishops | pos.board.rooks | pos.board.queens)
    
    longRangePieces.squares.flatMap { pieceSq =>
      val role = pos.board.roleAt(pieceSq).getOrElse(Queen)
      val attacksBefore = role match
        case Bishop => pieceSq.bishopAttacks(pos.board.occupied)
        case Rook   => pieceSq.rookAttacks(pos.board.occupied)
        case Queen  => pieceSq.queenAttacks(pos.board.occupied)
        case _      => Bitboard.empty
        
      val attacksAfter = role match
        case Bishop => pieceSq.bishopAttacks(nextPos.board.occupied)
        case Rook   => pieceSq.rookAttacks(nextPos.board.occupied)
        case Queen  => pieceSq.queenAttacks(nextPos.board.occupied)
        case _      => Bitboard.empty
      
      // New targets after moving the blocking piece
      val newTargets = (attacksAfter & ~attacksBefore) & nextPos.board.byColor(!color)
      
      newTargets.squares.flatMap { targetSq =>
        nextPos.board.roleAt(targetSq).map { targetRole =>
          DiscoveredAttack(movedPiece, role, targetRole, color, plyIndex, san)
        }
      }
    }

  private def detectOverloading(
      board: Board, 
      color: Color, 
      plyIndex: Int, 
      san: Option[String]
  ): List[Motif] =
    val criticalSquares = (board.byColor(color) & ~board.kings).squares.filter { sq =>
      board.attackers(sq, !color).nonEmpty
    }

    val duties = scala.collection.mutable.Map[Square, List[Square]]()

    criticalSquares.foreach { defendedSq =>
      val defenders = board.attackers(defendedSq, color)
      defenders.squares.foreach { defenderSq =>
        if defenderSq != defendedSq then
          duties.update(defenderSq, defendedSq :: duties.getOrElse(defenderSq, Nil))
      }
    }

    duties.collect { 
      case (defenderSq, tasks) if tasks.size >= 2 =>
        val role = board.roleAt(defenderSq).getOrElse(Pawn)
        Overloading(role, defenderSq, tasks, color, plyIndex, san)
    }.toList

  private def detectInterference(
      mv: Move, 
      nextPos: Position, 
      color: Color, 
      plyIndex: Int, 
      san: Option[String]
  ): List[Motif] =
    val blockerSq = mv.dest
    val board = nextPos.board
    val oppColor = !color
    
    val oppDefenders = board.byColor(oppColor) & (board.rooks | board.bishops | board.queens)
    val blockedDefenses = List.newBuilder[Motif]
    
    oppDefenders.squares.foreach { defenderSq =>
      val role = board.roleAt(defenderSq).getOrElse(Queen)
      
      // Interference: blocker is between defenderSq and some other piece it was defending
      val opponentPieces = board.byColor(oppColor) & ~Bitboard(defenderSq)
      opponentPieces.squares.foreach { targetSq =>
        // If blockerSq is strictly between defenderSq and targetSq, coordination is broken
        if (Bitboard.between(defenderSq, targetSq).contains(blockerSq)) {
          val targetRole = board.roleAt(targetSq).getOrElse(Pawn)
          blockedDefenses += Interference(
             interferingPiece = mv.piece.role,
             interferingSquare = blockerSq,
             blockedPiece1 = role,
             blockedPiece2 = targetRole,
             color = color,
             plyIndex = plyIndex,
             move = san
          )
        }
      }
    }
    
    blockedDefenses.result()

  // ============================================================
  // STATE MOTIF DETECTION
  // ============================================================

  private def detectStateMotifs(pos: Position, plyIndex: Int): List[Motif] =
    val board = pos.board
    List.concat(
      detectPawnStructure(board, plyIndex),
      detectOpposition(board, plyIndex)
    )

  private def detectPawnStructure(board: Board, plyIndex: Int): List[Motif] =
    var motifs = List.empty[Motif]
    for color <- List(White, Black) do
      val pawns = board.byPiece(color, Pawn)
      val oppPawns = board.byPiece(!color, Pawn)
      val pawnsByFile = pawns.squares.groupBy(_.file)
      val oppPawnsByFile = oppPawns.squares.groupBy(_.file)
      pawns.foreach { pawnSq =>
        if isIsolated(pawnSq, pawnsByFile) then 
          motifs = motifs :+ IsolatedPawn(pawnSq.file, pawnSq.rank.value + 1, color, plyIndex)
        if isPassed(pawnSq, color, oppPawnsByFile) then 
          motifs = motifs :+ PassedPawn(pawnSq.file, pawnSq.rank.value + 1, color, isProtected = false, plyIndex)
      }
    motifs

  private def isIsolated(pawnSq: Square, pawnsByFile: Map[File, List[Square]]): Boolean =
    val fileValue = pawnSq.file.value
    val adjacentFiles = List(fileValue - 1, fileValue + 1).filter(f => f >= 0 && f <= 7)
    !adjacentFiles.exists { adjFileIdx => 
      File.all.lift(adjFileIdx).exists(pawnsByFile.contains)
    }

  private def isPassed(pawnSq: Square, color: Color, oppPawnsByFile: Map[File, List[Square]]): Boolean =
    val fileValue = pawnSq.file.value
    val filesToCheck = List(fileValue - 1, fileValue, fileValue + 1).filter(f => f >= 0 && f <= 7)
    filesToCheck.forall { fIdx =>
      File.all.lift(fIdx).forall { f =>
        oppPawnsByFile.get(f).forall { oppPawns =>
          oppPawns.forall { oppPawn =>
            // Opponent pawn must be STRICTLY behind our pawn (not on same rank)
            if color.white then oppPawn.rank.value < pawnSq.rank.value
            else oppPawn.rank.value > pawnSq.rank.value
          }
        }
      }
    }

  private def detectOpposition(board: Board, plyIndex: Int): List[Motif] =
    (board.kingPosOf(White), board.kingPosOf(Black)) match
      case (Some(wk), Some(bk)) =>
        val fDiff = (wk.file.value - bk.file.value).abs
        val rDiff = (wk.rank.value - bk.rank.value).abs
        // Opposition: The side NOT to move has the opposition
        // In endgame context, we don't have side-to-move here, so we report both kings' squares
        // The consumer of this motif should determine context from plyIndex (even=White, odd=Black to move)
        val sideWithOpposition = if plyIndex % 2 == 0 then Black else White // Opposite of side to move
        if fDiff == 0 && rDiff == 2 then List(Opposition(bk, wk, OppositionType.Direct, sideWithOpposition, plyIndex))
        else if rDiff == 0 && fDiff == 2 then List(Opposition(bk, wk, OppositionType.Direct, sideWithOpposition, plyIndex))
        else Nil
      case _ => Nil

  private def detectThreatMotifs(
      mv: Move, 
      pos: Position, 
      nextPos: Position,
      color: Color, 
      san: String, 
      plyIndex: Int
  ): List[Motif] =
    // If this move creates a threat that must be addressed immediately
    // Heuristic: Does the opponent have a piece that can be captured next?
    // This is partly covered by Fork/Pin, but 'Threat' can be simpler (e.g. attacking a piece)
    Nil // Placeholder: complex threat analysis usually requires Engine input

  /**
   * Null Move Analysis: What is the threat if we skip our turn?
   * Scores and ranks threats by tactical severity, returning only the most dangerous ones.
   */
  /**
   * Null Move Analysis: What is the threat if we skip our turn?
   * Scores and ranks threats by tactical severity, returning interesting motifs from the most dangerous moves.
   */
  def detectThreats(fen: String, color: Color): List[Motif] =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map { pos =>
      val oppColor = !color
      val oppPos = if pos.color == oppColor then pos else pos.withColor(oppColor)
      
      // Group motifs by move so we can detect combinations (e.g. Check AND Fork)
      val threateningMoves = oppPos.legalMoves.flatMap { mv =>
        val san = mv.toSanStr.toString
        val motifs = detectTacticalMotifs(mv, oppPos, mv.after, oppColor, san, 0)
        
        // Static Exchange guard: Filter out "Winning Captures" that are actually defended
        val filteredMotifs = motifs.filter {
          case c: CaptureMotif if c.captureType == CaptureType.Winning =>
            // Check if the target square (where capture happens) is defended by the other side
            // If it IS defended, this "winning" capture might just be a trade or even a loss.
            val targetSquare = mv.dest
            val defender = color // The side we're analyzing threats FOR (opponent's perspective)
            val isDefended = oppPos.board.attackers(targetSquare, defender).nonEmpty
            
            // If the target is defended, we need the value difference to be significant
            // to still consider it a "real" winning threat.
            // Simple heuristic: If defended, only keep if attacker is minor and target is major (or similar high-value trade)
            if (isDefended) {
              // Get the value of attacker and target (in pawn units: P=1, N/B=3, R=5, Q=9)
              val attackerValue = oppPos.board.roleAt(mv.orig).map(pieceValue).getOrElse(0)
              val targetValue = oppPos.board.roleAt(targetSquare).map(pieceValue).getOrElse(0)
              // Only keep if we win significantly (e.g., knight takes rook on defended square)
              // Net gain must be at least 2 pawn units (e.g., minor takes rook)
              targetValue - attackerValue >= 2
            } else {
              true // Undefended = free win, keep it
            }
          case _ => true
        }

        if (filteredMotifs.nonEmpty) {
          val score = scoreMoveThreat(filteredMotifs)
          Some((filteredMotifs, score))
        } else None
      }

      // Filter noise and select top threats
      threateningMoves
        .filter(_._2 > 0)     // Only actual threats
        .sortBy(-_._2)        // Highest impact first
        .take(2)              // Focus on the top 2 most critical threats
        .flatMap(_._1)        // Flatten to list of motifs for the prompt
    }.getOrElse(Nil).distinct

  /**
   * Scores a move based on the combination of motifs it generates.
   * Detecting combinations (Check + Fork) is critical for high-quality alerts.
   */
  private def scoreMoveThreat(motifs: List[Motif]): Int =
    val givesCheck = motifs.exists(_.isInstanceOf[CheckMotif])
    val forks = motifs.exists(_.isInstanceOf[ForkMotif])
    val skewers = motifs.exists(_.isInstanceOf[Skewer])
    val discovered = motifs.exists(_.isInstanceOf[DiscoveredAttack])
    val winningCapture = motifs.collectFirst { 
      case c: CaptureMotif if c.captureType == CaptureType.Winning => true 
    }.getOrElse(false)

    var score = 0
    
    // Base scores for tactical themes
    if (givesCheck) score += 50
    if (forks) score += 80
    if (skewers) score += 70
    if (discovered) score += 60
    if (winningCapture) score += 60

    // Bonus for combinations (The "Guard" logic fix)
    if (givesCheck && forks) score += 50      // Check-Fork is deadly
    if (givesCheck && winningCapture) score += 40 // Check-WinningCapture is forcing
    if (discovered && givesCheck) score += 40 // Discovered check is often mating

    score

  // ============================================================
  // HELPERS
  // ============================================================

  private def isSmotheredPattern(pos: Position, color: Color): Boolean =
    pos.board.kingPosOf(!color).exists { kingSq =>
      val attackers = pos.board.attackers(kingSq, color)
      val isKnightCheck = attackers.exists(sq => pos.board.roleAt(sq).contains(Knight))
      val adjacent = kingSq.kingAttacks
      val blocked = adjacent & pos.board.byColor(!color)
      isKnightCheck && (blocked == adjacent)
    }

  private def pieceValue(role: Role): Int = role match
    case Pawn   => 1
    case Knight => 3
    case Bishop => 3
    case Rook   => 5
    case Queen  => 9
    case King   => 100
