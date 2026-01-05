package lila.llm.analysis

import lila.llm.model.*
import lila.llm.model.strategic.{ VariationLine, VariationTag, CounterfactualMatch, Hypothesis }
import lila.llm.model.Motif.{ *, given }

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
      
      Some(CounterfactualMatch(
        userMove = userMove,
        bestMove = bestMove,
        cpLoss = cpLoss,
        missedMotifs = missedMotifs,
        userMoveMotifs = userMotifs,
        severity = severity,
        userLine = lila.llm.model.strategic.VariationLine(
          moves = List(userMove),
          scoreCp = 0,
          mate = None,
          tags = Nil
        )
      ))


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
               candidateType = CandidateType.Capture.name,
               rationale = s"Captures the ${victim.name}"
             )
      }

      // 2. "Checks"
      // Checks are always tempting
      legalMoves.filter(m => m.after.check.yes).foreach { mv =>
        val uci = mv.toUci.uci
        if uci != engineBestMove then
          candidates += Hypothesis(
            move = uci,
            candidateType = CandidateType.Check.name,
            rationale = "Forcing check"
          )
      }

      candidates.result().take(3) // Limit to top 3 most "obvious" candidates
    }.getOrElse(Nil)

    // ============================================================
  // MOVE MOTIF DETECTION (from MotifTokenizer)
  // ============================================================

  def detectMoveMotifs(mv: Move, pos: Position, plyIndex: Int): List[Motif] =
    val color = pos.color
    val san = mv.toSanStr.toString
    val nextPos = mv.after

    List.concat(
      detectPawnMotifs(mv, pos, color, san, plyIndex),
      detectPieceMotifs(mv, pos, color, san, plyIndex),
      detectKingMotifs(mv, pos, color, san, plyIndex),
      detectTacticalMotifs(mv, pos, nextPos, color, san, plyIndex)
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
      case _: Motif.Check | _: Motif.Fork | _: Motif.Pin => true
      case m: Motif.Capture if m.captureType != Motif.CaptureType.Normal => true
      case _ => false
    }

    if tacticsCount >= 2 then builder += VariationTag.Sharp
    else if line.moves.length > 5 && tacticsCount == 0 then builder += VariationTag.Solid

    threat.foreach { t =>
      if t.scoreCp < -100 && line.scoreCp > t.scoreCp + 100 then
        builder += VariationTag.Prophylaxis
    }

    // Simplification: Winning score (> 200) + Excanges
    if line.scoreCp > 200 then
      val captures = motifs.count(_.isInstanceOf[Motif.Capture])
      val exchanges = motifs.count {
        case m: Motif.Capture => m.captureType == Motif.CaptureType.Exchange
        case _ => false
      }
      if captures > 0 || exchanges > 0 then
        builder += VariationTag.Simplification

    builder.result()

  private def detectPawnMotifs(mv: Move, _pos: Position, color: Color, san: String, plyIndex: Int): List[Motif] =
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

    val beforeBoard = _pos.board
    val wPassed = PositionAnalyzer.passedPawns(Color.White, beforeBoard.pawns & beforeBoard.white, beforeBoard.pawns & beforeBoard.black)
    val bPassed = PositionAnalyzer.passedPawns(Color.Black, beforeBoard.pawns & beforeBoard.black, beforeBoard.pawns & beforeBoard.white)
    val passedSquares = if color == Color.White then wPassed else bPassed

    if (passedSquares.contains(mv.orig)) {
       motifs = motifs :+ PassedPawnPush(
         file = mv.dest.file,
         toRank = mv.dest.rank.value + 1,
         color = color,
         plyIndex = plyIndex,
         move = Some(san)
       )
    }

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

  private def detectPieceMotifs(mv: Move, _pos: Position, color: Color, san: String, plyIndex: Int): List[Motif] =
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

  private def detectKingMotifs(mv: Move, _pos: Position, color: Color, san: String, plyIndex: Int): List[Motif] =
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

  def detectTacticalMotifs(
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
      motifs = motifs :+ Motif.Check(mv.piece.role, kingSq, checkType, color, plyIndex, Some(san))
    
    // Capture (enhanced with Exchange Sacrifice detection)
    if mv.captures then
      val capturedRole = mv.capture.flatMap(pos.board.roleAt).getOrElse(pos.board.roleAt(mv.dest).getOrElse(Pawn))
      val captureType = determineCaptureType(mv.piece.role, capturedRole)
      
      // Detect Positional Sacrifice with ROI (any meaningful material sacrifice)
      // Cases: Rook for minor piece, Minor piece for pawn, Queen sacrifice
      val isMaterialSacrifice = (
        // Rook for minor piece (classic exchange sacrifice)
        (mv.piece.role == Rook && (capturedRole == Knight || capturedRole == Bishop)) ||
        // Minor piece for pawn (positional sacrifice)
        ((mv.piece.role == Knight || mv.piece.role == Bishop) && capturedRole == Pawn) ||
        // Queen sacrifice for compensation
        (mv.piece.role == Queen && capturedRole != Queen)
      )
      
      val sacrificeROI = if (isMaterialSacrifice) {
        detectExchangeSacrificeROI(mv, nextPos, color)
      } else None
      
      val finalCaptureType = if (sacrificeROI.isDefined) CaptureType.ExchangeSacrifice else captureType
      motifs = motifs :+ Motif.Capture(mv.piece.role, capturedRole, mv.dest, finalCaptureType, color, plyIndex, Some(san), sacrificeROI)
    
    // Fork detection
    val forkTargetsList = detectForkTargets(mv, nextPos, color)
    if forkTargetsList.size >= 2 then
      motifs = motifs :+ Motif.Fork(mv.piece.role, forkTargetsList, mv.dest, color, plyIndex, Some(san))
    
    // Pin / Skewer detection
    detectLineTactics(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Discovered Attack detection
    detectDiscoveredTactics(mv, pos, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Overloading detection
    detectOverloading(nextPos.board, !color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Interference detection
    detectInterference(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Deflection detection (NEW - Phase 11)
    detectDeflection(mv, pos, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Decoy detection (NEW - Phase 11)  
    detectDecoy(mv, pos, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Clearance detection (L2 Completion)
    detectClearance(mv, pos, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    motifs

  private def determineCaptureType(attacker: Role, victim: Role): CaptureType =
    val val1 = pieceValue(attacker)
    val val2 = pieceValue(victim)
    if val2 > val1 then CaptureType.Winning
    else if val2 == val1 then CaptureType.Exchange
    else CaptureType.Normal // Default to Normal for BxP, Sacrifice requires ROI/context

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
      
      def chebyshevDist(a: Square, b: Square): Int =
        Math.max((a.file.value - b.file.value).abs, (a.rank.value - b.rank.value).abs)
      
      occupiedBehind.squares.filter(s => Bitboard.between(sq, s).contains(targetSq))
        .minByOption(s => chebyshevDist(sq, s))
        .flatMap { behindSq =>
          board.roleAt(behindSq).flatMap { behindRole =>
            if (board.colorAt(behindSq).contains(!color)) {
              if (pieceValue(targetRole) < pieceValue(behindRole))
                Some(Motif.Pin(role, targetRole, behindRole, color, plyIndex, san))
              else if (pieceValue(targetRole) > pieceValue(behindRole))
                Some(Motif.Skewer(role, targetRole, behindRole, color, plyIndex, san))
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
      
      val newTargets = (attacksAfter & ~attacksBefore) & nextPos.board.byColor(!color)
      
      newTargets.squares.flatMap { targetSq =>
        nextPos.board.roleAt(targetSq).map { targetRole =>
          Motif.DiscoveredAttack(movedPiece, role, targetRole, color, plyIndex, san)
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
        Motif.Overloading(role, defenderSq, tasks, color, plyIndex, san)
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
      val opponentPieces = board.byColor(oppColor) & ~Bitboard(defenderSq)
      opponentPieces.squares.foreach { targetSq =>
        if (Bitboard.between(defenderSq, targetSq).contains(blockerSq)) {
          val targetRole = board.roleAt(targetSq).getOrElse(Pawn)
          blockedDefenses += Motif.Interference(
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

  def detectStateMotifs(pos: Position, plyIndex: Int): List[Motif] =
    val board = pos.board
    List.concat(
      detectPawnStructure(board, plyIndex),
      detectOpposition(board, pos.color, plyIndex),
      detectOpenFiles(board, plyIndex),
      detectWeakBackRank(pos, plyIndex),
      detectSpaceAdvantage(board, plyIndex),
      detectBattery(board, plyIndex)  // L2 Completion
    )

  // ===== FIX 6: New Motif Detectors =====
  private def detectOpenFiles(board: Board, plyIndex: Int): List[Motif] =
    var motifs = List.empty[Motif]
    for file <- File.all do
      val whitePawns = board.byPiece(White, Pawn) & Bitboard.file(file)
      val blackPawns = board.byPiece(Black, Pawn) & Bitboard.file(file)
      
      if whitePawns.isEmpty && blackPawns.isEmpty then
        // Open file - check who controls it
        val whiteRooks = (board.byPiece(White, Rook) | board.byPiece(White, Queen)) & Bitboard.file(file)
        val blackRooks = (board.byPiece(Black, Rook) | board.byPiece(Black, Queen)) & Bitboard.file(file)
        
        if whiteRooks.nonEmpty && blackRooks.isEmpty then
          motifs = motifs :+ OpenFileControl(file, White, plyIndex)
        else if blackRooks.nonEmpty && whiteRooks.isEmpty then
          motifs = motifs :+ OpenFileControl(file, Black, plyIndex)
    motifs

  private def detectWeakBackRank(pos: Position, plyIndex: Int): List[Motif] =
    var motifs = List.empty[Motif]
    val board = pos.board
    
    for color <- List(White, Black) do
      val backRank = if color.white then Rank.First else Rank.Eighth
      board.kingPosOf(color).foreach { kingSq =>
        if kingSq.rank == backRank then
          // Check if king is trapped on back rank (no escape squares)
          val escapeRank = if color.white then Rank.Second else Rank.Seventh
          val escapeSquares = kingSq.kingAttacks & Bitboard.rank(escapeRank)
          val blockedByOwn = escapeSquares & board.byColor(color)
          
          if escapeSquares.count > 0 && blockedByOwn.count == escapeSquares.count then
            motifs = motifs :+ WeakBackRank(color, plyIndex)
      }
    motifs

  private def detectSpaceAdvantage(board: Board, plyIndex: Int): List[Motif] =
    // Count pawns on rank 4/5 for White, rank 5/4 for Black
    val whiteCentralPawns = (board.byPiece(White, Pawn) & (Bitboard.rank(Rank.Fourth) | Bitboard.rank(Rank.Fifth))).count
    val blackCentralPawns = (board.byPiece(Black, Pawn) & (Bitboard.rank(Rank.Fourth) | Bitboard.rank(Rank.Fifth))).count
    
    val diff = whiteCentralPawns - blackCentralPawns
    if diff >= 2 then List(SpaceAdvantage(White, diff, plyIndex))
    else if diff <= -2 then List(SpaceAdvantage(Black, -diff, plyIndex))
    else Nil

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
            if color.white then oppPawn.rank.value < pawnSq.rank.value
            else oppPawn.rank.value > pawnSq.rank.value
          }
        }
      }
    }

  private def detectOpposition(board: Board, color: Color, plyIndex: Int): List[Motif] =
    (board.kingPosOf(White), board.kingPosOf(Black)) match
      case (Some(wk), Some(bk)) =>
        val fDiff = (wk.file.value - bk.file.value).abs
        val rDiff = (wk.rank.value - bk.rank.value).abs
        // Side with opposition is the one NOT whose turn it is.
        // If it's White's turn (color == White) and opposition exists, Black has the opposition.
        val sideWithOpposition = !color
        if fDiff == 0 && rDiff == 2 then List(Opposition(bk, wk, OppositionType.Direct, sideWithOpposition, plyIndex))
        else if rDiff == 0 && fDiff == 2 then List(Opposition(bk, wk, OppositionType.Direct, sideWithOpposition, plyIndex))
        else Nil
      case _ => Nil

  def detectThreats(fen: String, color: Color): List[Motif] =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map { pos =>
      val oppColor = !color
      val oppPos = if pos.color == oppColor then pos else pos.withColor(oppColor)
      
      val threateningMoves = oppPos.legalMoves.flatMap { mv =>
        val san = mv.toSanStr.toString
        val motifs = detectTacticalMotifs(mv, oppPos, mv.after, oppColor, san, 0)
        
        val filteredMotifs = motifs.filter {
          case c: Motif.Capture if c.captureType == CaptureType.Winning =>
            val targetSquare = mv.dest
            val defender = color
            val isDefended = oppPos.board.attackers(targetSquare, defender).nonEmpty
            
            if (isDefended) {
              val attackerValue = oppPos.board.roleAt(mv.orig).map(pieceValue).getOrElse(0)
              val targetValue = oppPos.board.roleAt(targetSquare).map(pieceValue).getOrElse(0)
              targetValue - attackerValue >= 2
            } else {
              true
            }
          case _ => true
        }

        if (filteredMotifs.nonEmpty) {
          val score = scoreMoveThreat(filteredMotifs)
          Some((filteredMotifs, score))
        } else None
      }

      threateningMoves
        .filter(_._2 > 0)
        .sortBy(-_._2)
        .take(2)
        .flatMap(_._1)
    }.getOrElse(Nil).distinct

  private def scoreMoveThreat(motifs: List[Motif]): Int =
    val givesCheck = motifs.exists(_.isInstanceOf[Motif.Check])
    val forks = motifs.exists(_.isInstanceOf[Motif.Fork])
    val skewers = motifs.exists(_.isInstanceOf[Motif.Skewer])
    val discovered = motifs.exists(_.isInstanceOf[Motif.DiscoveredAttack])
    val winningCapture = motifs.collectFirst { 
      case c: Motif.Capture if c.captureType == CaptureType.Winning => true 
    }.getOrElse(false)

    var score = 0
    
    if (givesCheck) score += 50
    if (forks) score += 80
    if (skewers) score += 70
    if (discovered) score += 60
    if (winningCapture) score += 60

    if (givesCheck && forks) score += 50
    if (givesCheck && winningCapture) score += 40
    if (discovered && givesCheck) score += 40

    score

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

  // ============================================================
  // PHASE 11: EXCHANGE SACRIFICE DETECTION
  // ============================================================

  /**
   * Detect compensation for Exchange Sacrifice (Rook for minor piece).
   * Returns SacrificeROI if positional compensation exists.
   */
  private def detectExchangeSacrificeROI(mv: Move, nextPos: Position, color: Color): Option[SacrificeROI] = {
    val board = nextPos.board
    var reasons = List.empty[String]
    var totalValue = 0
    
    // 1. Open file created for remaining rook?
    val capturedFile = mv.dest.file
    val ownPawnsOnFile = (board.pawns & board.byColor(color)).squares.count(_.file == capturedFile)
    val enemyPawnsOnFile = (board.pawns & board.byColor(!color)).squares.count(_.file == capturedFile)
    if (ownPawnsOnFile == 0) {
      val hasRookOnFile = (board.rooks & board.byColor(color)).squares.exists(_.file == capturedFile)
      if (hasRookOnFile || enemyPawnsOnFile == 0) {
        reasons = reasons :+ "open_file"
        totalValue += 150
      }
    }
    
    // 2. King exposed (weakened shelter)?
    val enemyKingPos = board.kingPosOf(!color)
    enemyKingPos.foreach { kingSq =>
      // Check if king is on back rank with reduced pawn cover
      val kingFile = kingSq.file
      val kingFileIdx = kingFile.value
      val adjacentFileIndices = List(kingFileIdx - 1, kingFileIdx, kingFileIdx + 1).filter(i => i >= 0 && i <= 7)
      val adjacentFiles = adjacentFileIndices.flatMap(i => File.all.lift(i))
      val pawnShieldCount = adjacentFiles.count { f =>
        (board.pawns & board.byColor(!color)).squares.exists(sq => 
          sq.file == f && (if (!color).white then sq.rank.value >= kingSq.rank.value else sq.rank.value <= kingSq.rank.value)
        )
      }
      if (pawnShieldCount <= 1) {
        reasons = reasons :+ "king_exposed"
        totalValue += 200
      }
    }
    
    // 3. Piece activity gain (bishop pair, dominant piece)?
    val ownBishops = (board.bishops & board.byColor(color)).count
    if (ownBishops >= 2) {
      reasons = reasons :+ "bishop_pair"
      totalValue += 50
    }
    
    // Only return ROI if there's meaningful compensation (tightened threshold)
    if totalValue >= 250 || (reasons.size >= 2 && totalValue >= 150) then 
      Some(SacrificeROI(reasons.mkString(","), totalValue))
    else None
  }

  // ============================================================
  // PHASE 11: ZWISCHENZUG DETECTION
  // ============================================================

  /**
   * Detect Zwischenzug (intermediate move) in a PV line.
   * Called when analyzing consecutive moves where:
   * 1. Previous move was a capture
   * 2. Current move is NOT a recapture on that square
   * 3. Current move creates a strong threat (check, fork, winning capture)
   */
  def detectZwischenzug(
      prevMove: Uci.Move,
      prevPos: Position,
      currentMove: Uci.Move, 
      currentPos: Position,
      plyIndex: Int
  ): Option[Motif.Zwischenzug] = {
    // 1. Check if previous move was a capture
    val prevMoveObj = prevPos.move(prevMove).toOption
    val wasCapture = prevMoveObj.exists(_.captures)
    
    if (!wasCapture) return None
    
    // 2. Expected recapture square = destination of previous move
    val expectedRecaptureSquare = prevMove.dest
    
    // 3. Is current move a recapture? If so, not a Zwischenzug
    if (currentMove.dest == expectedRecaptureSquare) return None
    
    // 4. Does current move create a strong threat?
    val color = currentPos.color
    currentPos.move(currentMove).toOption.flatMap { mv =>
      val afterPos = mv.after
      
      // Check threat types
      val givesCheck = afterPos.check.yes
      val forkTargets = detectForkTargets(mv, afterPos, color)
      val hasFork = forkTargets.size >= 2
      
      // Winning capture threat on next move
      val hasWinningCaptureThreat = afterPos.legalMoves.exists { nextMv =>
        nextMv.captures && {
          val victim = afterPos.board.roleAt(nextMv.dest)
          val attacker = nextMv.piece.role
          victim.exists(v => pieceValue(v) > pieceValue(attacker))
        }
      }
      
      val threatType = 
        if (givesCheck && afterPos.checkMate) "MateThreat"
        else if (givesCheck) "Check"
        else if (hasFork) "Fork"
        else if (hasWinningCaptureThreat) "WinningCapture"
        else ""
      
      if (threatType.nonEmpty) {
        Some(Motif.Zwischenzug(
          intermediateMove = currentMove.uci,
          threatType = threatType,
          expectedRecaptureSquare = expectedRecaptureSquare,
          color = color,
          plyIndex = plyIndex,
          move = Some(mv.toSanStr.toString)
        ))
      } else None
    }
  }

  // ============================================================
  // PHASE 11: DEFLECTION DETECTION
  // ============================================================

  /**
   * Detect Deflection - forcing a defending piece away from its duties.
   * Example: Attacking a piece that defends the king or a valuable target.
   */
  private def detectDeflection(
      mv: Move,
      pos: Position,
      nextPos: Position,
      color: Color,
      plyIndex: Int,
      san: Option[String]
  ): Option[Motif.Deflection] = {
    val opponent = !color
    val board = pos.board
    
    // The moved piece attacks enemy pieces in next position
    val attackedSquares = mv.dest match {
      case dest =>
        val role = mv.piece.role
        role match {
          case Pawn   => dest.pawnAttacks(color)
          case Knight => dest.knightAttacks
          case Bishop => dest.bishopAttacks(nextPos.board.occupied)
          case Rook   => dest.rookAttacks(nextPos.board.occupied)
          case Queen  => dest.queenAttacks(nextPos.board.occupied)
          case King   => dest.kingAttacks
        }
    }
    val attackedEnemies = attackedSquares & board.byColor(opponent)
    
    // For each attacked enemy piece, check if it's defending something important
    attackedEnemies.squares.flatMap { defenderSq =>
      board.roleAt(defenderSq).flatMap { defenderRole =>
        // What does this defender protect?
        val defenderAttacks = defenderRole match {
          case Pawn   => defenderSq.pawnAttacks(opponent)
          case Knight => defenderSq.knightAttacks
          case Bishop => defenderSq.bishopAttacks(board.occupied)
          case Rook   => defenderSq.rookAttacks(board.occupied)
          case Queen  => defenderSq.queenAttacks(board.occupied)
          case King   => defenderSq.kingAttacks
        }
        val protectedFriendlies = (defenderAttacks & board.byColor(opponent)).squares
        
        // Is it protecting something more valuable than itself?
        val protectsValuable = protectedFriendlies.exists { sq =>
          board.roleAt(sq).exists(r => pieceValue(r) >= pieceValue(defenderRole))
        }
        
        if (protectsValuable) {
          Some(Motif.Deflection(defenderRole, defenderSq, opponent, plyIndex, san))
        } else None
      }
    }.headOption
  }

  // ============================================================
  // PHASE 11: DECOY DETECTION  
  // ============================================================

  /**
   * Detect Decoy - luring a piece to a vulnerable square.
   * Example: Sacrifice to force king/piece to a fork or check square.
   */
  private def detectDecoy(
      mv: Move,
      pos: Position,
      nextPos: Position,
      color: Color,
      plyIndex: Int,
      san: Option[String]
  ): Option[Motif.Decoy] = {
    // For decoy, we typically sacrifice to lure a piece
    // Check if we're offering our piece (capture available for opponent)
    val isOffering = nextPos.legalMoves.exists { oppMv =>
      oppMv.captures && oppMv.dest == mv.dest
    }
    
    if (!isOffering) return None
    
    // Simulate opponent capture on our piece
    val captureScenarios = nextPos.legalMoves.filter(m => m.captures && m.dest == mv.dest)
    
    captureScenarios.flatMap { oppCapture =>
      val afterCaptureOpt = nextPos.move(oppCapture.toUci.asInstanceOf[Uci.Move]).toOption
      
      afterCaptureOpt.flatMap { afterCapture =>
        val afterPos = afterCapture.after
        
        // Check if we now have a winning move (fork, check, etc.)
        val hasWinningResponse = afterPos.legalMoves.exists { ourResponse =>
          ourResponse.after.check.yes || {
            val forkTargets = detectForkTargets(ourResponse, ourResponse.after, color)
            forkTargets.size >= 2
          }
        }
        
        if (hasWinningResponse) {
          Some(Motif.Decoy(oppCapture.piece.role, mv.dest, !color, plyIndex, san))
        } else None
      }
    }.headOption
  }

  // ============================================================
  // L2 COMPLETION: CLEARANCE DETECTION
  // ============================================================

  /**
   * Detect Clearance - moving a piece to clear a line/square for another piece.
   * Example: Moving a rook so that the queen can use the file.
   */
  private def detectClearance(
      mv: Move,
      pos: Position,
      nextPos: Position,
      color: Color,
      plyIndex: Int,
      san: Option[String]
  ): Option[Motif.Clearance] = {
    val board = pos.board
    val nextBoard = nextPos.board
    val origSq = mv.orig
    val destSq = mv.dest
    val movedRole = mv.piece.role
    
    // Only detect for long-range pieces clearing lines
    if (!List(Rook, Queen, Bishop).contains(movedRole)) return None
    
    // Check if a friendly piece now has access to a line that was blocked
    val friendlyLongRange = (nextBoard.byColor(color) & 
      (nextBoard.rooks | nextBoard.bishops | nextBoard.queens)) &
      ~Bitboard(destSq)
    
    friendlyLongRange.squares.flatMap { friendlySq =>
      val friendlyRole = nextBoard.roleAt(friendlySq).getOrElse(Queen)
      
      // Check if this piece can now attack through where the moved piece was
      val attacksAfter = friendlyRole match {
        case Bishop => friendlySq.bishopAttacks(nextBoard.occupied)
        case Rook   => friendlySq.rookAttacks(nextBoard.occupied)
        case Queen  => friendlySq.queenAttacks(nextBoard.occupied)
        case _      => Bitboard.empty
      }
      
      val attacksBefore = friendlyRole match {
        case Bishop => friendlySq.bishopAttacks(board.occupied)
        case Rook   => friendlySq.rookAttacks(board.occupied)
        case Queen  => friendlySq.queenAttacks(board.occupied)
        case _      => Bitboard.empty
      }
      
      // New attack squares = squares accessible after but not before
      val newAttacks = attacksAfter & ~attacksBefore
      
      // Was the original square of the moved piece on the ray?
      val ray = Bitboard.ray(friendlySq, origSq)
      val clearedRay = ray.nonEmpty && (newAttacks & nextBoard.byColor(!color)).nonEmpty
      
      if (clearedRay) {
        val clearanceType = 
          if (friendlySq.file == origSq.file) Motif.ClearanceType.File
          else if (friendlySq.rank == origSq.rank) Motif.ClearanceType.Rank
          else Motif.ClearanceType.Diagonal
          
        Some(Motif.Clearance(
          clearingPiece = movedRole,
          clearingFrom = origSq,
          clearedLine = clearanceType,
          beneficiary = friendlyRole,
          color = color,
          plyIndex = plyIndex,
          move = san
        ))
      } else None
    }.headOption
  }

  // ============================================================
  // L2 COMPLETION: BATTERY DETECTION
  // ============================================================

  /**
   * Detect Battery - two pieces aligned on the same line (Q+B on diagonal, R+R on file).
   * Used for detectStateMotifs at end of PV.
   */
  private def detectBattery(board: Board, plyIndex: Int): List[Motif.Battery] = {
    val batteries = List.newBuilder[Motif.Battery]
    
    for (color <- List(White, Black)) {
      val queens = board.byPiece(color, Queen)
      val rooks = board.byPiece(color, Rook)
      val bishops = board.byPiece(color, Bishop)
      val enemyPieces = board.byColor(!color)
      
      // Helper: Check if there's an enemy piece between or beyond two squares on a line
      def hasTargetOnLine(sq1: Square, sq2: Square, axis: Motif.BatteryAxis): Boolean = {
        val ray = axis match {
          case Motif.BatteryAxis.File => Bitboard.file(sq1.file)
          case Motif.BatteryAxis.Rank => Bitboard.rank(sq1.rank)
          case Motif.BatteryAxis.Diagonal => Bitboard.ray(sq1, sq2)
        }
        // Check if any enemy piece is on this line
        (ray & enemyPieces).nonEmpty
      }
      
      // Queen + Rook on same file/rank - ONLY if enemy target exists on line
      queens.foreach { queenSq =>
        rooks.foreach { rookSq =>
          if (queenSq.file == rookSq.file && queenSq != rookSq) {
            if (hasTargetOnLine(queenSq, rookSq, Motif.BatteryAxis.File)) {
              batteries += Motif.Battery(Queen, Rook, Motif.BatteryAxis.File, color, plyIndex, None)
            }
          } else if (queenSq.rank == rookSq.rank && queenSq != rookSq) {
            if (hasTargetOnLine(queenSq, rookSq, Motif.BatteryAxis.Rank)) {
              batteries += Motif.Battery(Queen, Rook, Motif.BatteryAxis.Rank, color, plyIndex, None)
            }
          }
        }
      }
      
      // Queen + Bishop on same diagonal - ONLY if enemy target exists
      queens.foreach { queenSq =>
        bishops.foreach { bishopSq =>
          if (queenSq != bishopSq) {
            val fileDiff = (queenSq.file.value - bishopSq.file.value).abs
            val rankDiff = (queenSq.rank.value - bishopSq.rank.value).abs
            if (fileDiff == rankDiff && fileDiff > 0) {
              if (hasTargetOnLine(queenSq, bishopSq, Motif.BatteryAxis.Diagonal)) {
                batteries += Motif.Battery(Queen, Bishop, Motif.BatteryAxis.Diagonal, color, plyIndex, None)
              }
            }
          }
        }
      }
      
      // Rook + Rook on same file/rank - ONLY if enemy target exists
      val rookList = rooks.squares.toList
      if (rookList.size >= 2) {
        val r1 = rookList(0)
        val r2 = rookList(1)
        if (r1.file == r2.file) {
          if (hasTargetOnLine(r1, r2, Motif.BatteryAxis.File)) {
            batteries += Motif.Battery(Rook, Rook, Motif.BatteryAxis.File, color, plyIndex, None)
          }
        } else if (r1.rank == r2.rank) {
          if (hasTargetOnLine(r1, r2, Motif.BatteryAxis.Rank)) {
            batteries += Motif.Battery(Rook, Rook, Motif.BatteryAxis.Rank, color, plyIndex, None)
          }
        }
      }
    }
    
    batteries.result()
  }
