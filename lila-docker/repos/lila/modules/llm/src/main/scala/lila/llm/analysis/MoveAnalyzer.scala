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
      val (_, motifs, _) = pv.zipWithIndex.foldLeft((initialPos, List.empty[Motif], Option.empty[Move])) {
        case ((pos, acc, lastMv), (uciStr, index)) =>
          Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(pos.move(_).toOption) match
            case Some(mv) =>
              val moveMotifs = detectMoveMotifs(mv, pos, index, lastMv)
              // State motifs are detected only at the final position to avoid massive redundancy
              val stateMotifs = if (index == pv.length - 1) detectStateMotifs(mv.after, index) else Nil
              (mv.after, acc ++ moveMotifs ++ stateMotifs, Some(mv))
            case None => 
              (pos, acc, lastMv)
      }
      motifs
    }.getOrElse(Nil)

  /**
   * Phase 14: Parse UCI moves into PvMove with SAN, piece, capture, check metadata.
   * This preserves coordinate-level information for book-style rendering.
   */
  def parsePv(initialFen: String, pv: List[String]): List[lila.llm.model.strategic.PvMove] =
    Fen.read(chess.variant.Standard, Fen.Full(initialFen)).map { initialPos =>
      val (_, pvMoves) = pv.foldLeft((initialPos, List.empty[lila.llm.model.strategic.PvMove])) {
        case ((pos, acc), uciStr) =>
          val uciMoveOpt = Uci(uciStr).collect { case m: Uci.Move => m }
          uciMoveOpt.flatMap(uciMove => pos.move(uciMove).toOption) match
            case Some(mv) =>
              val san = mv.toSanStr.toString
              val role = mv.piece.role
              val piece = if (role == chess.Pawn) "P" else role.toString.take(1).toUpperCase()
              
              val pvMove = lila.llm.model.strategic.PvMove(
                uci = uciStr,
                san = san,
                from = mv.orig.key,
                to = mv.dest.key,
                piece = piece,
                isCapture = mv.captures,
                capturedPiece = mv.capture.map(_.toString.take(1).toUpperCase()),
                givesCheck = mv.after.check.yes
              )
              (mv.after, acc :+ pvMove)
            case None => 
              (pos, acc) // Stop or skip invalid moves
      }
      pvMoves
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
      
      val severity = Thresholds.classifySeverity(cpLoss)
      
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

  def detectMoveMotifs(mv: Move, pos: Position, plyIndex: Int, prevMove: Option[Move] = None): List[Motif] =
    val color = pos.color
    val san = mv.toSanStr.toString
    val nextPos = mv.after

    List.concat(
      detectPawnMotifs(mv, pos, color, san, plyIndex),
      detectPieceMotifs(mv, color, san, plyIndex),
      detectKingMotifs(mv, color, san, plyIndex),
      detectTacticalMotifs(mv, pos, nextPos, color, san, plyIndex, prevMove),
      detectPositionalMotifs(mv, pos, nextPos, color, san, plyIndex) 
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

  private def detectPieceMotifs(mv: Move, color: Color, san: String, plyIndex: Int): List[Motif] =
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

  private def detectKingMotifs(mv: Move, color: Color, san: String, plyIndex: Int): List[Motif] =
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
      plyIndex: Int,
      prevMove: Option[Move] = None
  ): List[Motif] = {
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
    
    // Capture (enhanced with Exchange Sacrifice detection and Recapture logic)
    if mv.captures then
      val capturedRole = mv.capture.flatMap(pos.board.roleAt).getOrElse(pos.board.roleAt(mv.dest).getOrElse(Pawn))
      
      // Phase 22.5: Recapture detection
      val isRecapture = prevMove.exists(pm => pm.captures && pm.dest == mv.dest)
      val captureType = if (isRecapture) CaptureType.Recapture else determineCaptureType(mv.piece.role, capturedRole)
      
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
    val forkTargets = detectForkTargets(mv, nextPos, color)
    // Phase 22.5: Harden fork detection to avoid false positives on Pawn + 1 Piece or simple exchanges.
    // A real fork should target at least two high-value pieces (non-pawn) or involve the King.
    // Also, if the attacker is immediately recaptured by an equal or lesser piece, it's just an exchange.
    val forkRoles = forkTargets.map(_._2)
    val highValueTargets = forkRoles.filter(_ != Pawn)
    val attackerIsSafe = nextPos.board.attackers(mv.dest, !color).isEmpty
    
    // If we target the king, it's always interesting (check + threat)
    val involvesKing = forkRoles.contains(King)
    val isRealFork = (involvesKing && forkRoles.size >= 2) || (highValueTargets.size >= 2 && attackerIsSafe)
    
    if isRealFork then
      motifs = motifs :+ Motif.Fork(mv.piece.role, forkRoles, mv.dest, forkTargets.map(_._1), color, plyIndex, Some(san))

    // Phase 29: Removing the Defender
    detectRemovingTheDefender(mv, pos, nextPos, color, plyIndex, Some(san)).foreach { m =>
      motifs = motifs :+ m
    }

    // Phase 29: Mate Net
    detectMateNet(nextPos, color, plyIndex, Some(san)).foreach { m =>
      motifs = motifs :+ m
    }
      
    // Pin detection
    detectPin(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }
    
    // Skewer detection  
    detectSkewer(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }
    
    // XRay detection
    detectXRay(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }
    
    // Battery detection (Tactical formation by move)
    // If the move creates a battery, we should flag it here too?
    // Currently Battery is detected as a Structural/Positional motif in detectStateMotifs.
    // We'll leave it there for now to avoid duplication, or check for "Battery Formation".
    
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

    // Trapped Piece detection (Phase 24)
    detectTrappedPiece(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }
    
    // Blockade detection (Phase 26)
    detectBlockade(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }

    // Smothered Mate (Phase 26)
    if (nextPos.check.yes) { 
      detectSmotheredMate(mv, nextPos, color, plyIndex, Some(san)).foreach { m => motifs = motifs :+ m }
    }

    motifs
  }

  // ============================================================
  // POSITIONAL MOTIF DETECTION
  // ============================================================

  private def detectPositionalMotifs(
      mv: Move,
      pos: Position,
      nextPos: Position,
      color: Color,
      san: String,
      plyIndex: Int
  ): List[Motif] = {
    var motifs = List.empty[Motif]
    val role = mv.piece.role

    // 1. Maneuver (Piece Improvement)
    if (!mv.captures && !nextPos.check.yes && role != Pawn && role != King) {
      val mobilityBefore = calculateMobility(pos.board, mv.orig, role)
      val mobilityAfter = calculateMobility(nextPos.board, mv.dest, role)
      
      if (mobilityAfter > mobilityBefore + 2) {
        motifs = motifs :+ Motif.Maneuver(role, "improving_scope", color, plyIndex, Some(san))
      } else {
        val isBackRank = if (color.white) mv.orig.rank == chess.Rank.First else mv.orig.rank == chess.Rank.Eighth
        if (role == Knight && !isBackRank && mv.dest.rank != mv.orig.rank && mv.dest.file != mv.orig.file) {
          motifs = motifs :+ Motif.Maneuver(role, "rerouting", color, plyIndex, Some(san))
        }
      }
    }

    // 2. Domination (Restricting enemy pieces)
    val board = nextPos.board
    val attacks = role match {
      case Knight => mv.dest.knightAttacks
      case Bishop => mv.dest.bishopAttacks(board.occupied)
      case Rook => mv.dest.rookAttacks(board.occupied)
      case Queen => mv.dest.queenAttacks(board.occupied)
      case _ => Bitboard.empty
    }

    val enemyPieces = board.byColor(!color)
    val attackedEnemies = attacks & enemyPieces
    
    attackedEnemies.squares.foreach { targetSq =>
      val targetRole = board.roleAt(targetSq).getOrElse(Pawn)
      if (targetRole != Pawn && targetRole != King) {
        val targetMobility = calculateMobility(board, targetSq, targetRole)
        if (targetMobility <= 1) {
          motifs = motifs :+ Motif.Domination(role, targetRole, targetSq, color, plyIndex, Some(san))
        }
      }
    }

    // 3. Rook Behind Passed Pawn (Tarrasch Rule)
    if (role == Rook) {
      val ourPawnsBitboard = nextPos.board.byPiece(color, Pawn)
      val oppPawnsByFile = nextPos.board.byPiece(!color, Pawn).squares.groupBy(_.file)
       
      ourPawnsBitboard.squares.filter(pSq => pSq.file == mv.dest.file).foreach { pSq =>
        if (isPassed(pSq, color, oppPawnsByFile)) {
          val isBehind = if (color.white) mv.dest.rank.value < pSq.rank.value else mv.dest.rank.value > pSq.rank.value
          if (isBehind) {
            motifs = motifs :+ Motif.RookBehindPassedPawn(mv.dest.file, color, plyIndex, Some(san))
          }
        }
      }
    }

    // 4. King Cut-Off (Endgame)
    if (nextPos.board.occupied.count <= 12 && (role == Rook || role == Queen)) {
      nextPos.board.kingPosOf(!color).foreach { kSq =>
        val isCutOffRank = (
          (color.white && mv.dest.rank.value > kSq.rank.value && mv.dest.rank.value <= 4) ||
          (color.black && mv.dest.rank.value < kSq.rank.value && mv.dest.rank.value >= 3)
        )
        val isCutOffFile = (
          (mv.dest.file.value < kSq.file.value && mv.dest.file.value >= 2) ||
          (mv.dest.file.value > kSq.file.value && mv.dest.file.value <= 5)
        )
        
        if (isCutOffRank) {
          motifs = motifs :+ Motif.KingCutOff("Rank", mv.dest.rank.value + 1, color, plyIndex, Some(san))
        } else if (isCutOffFile) {
          motifs = motifs :+ Motif.KingCutOff("File", mv.dest.file.value, color, plyIndex, Some(san))
        }
      }
    }

    // 5. Seventh Rank Invasion
    if (role == Rook) {
      val seventhRank = if (color.white) Rank.Seventh else Rank.Second
      if (mv.dest.rank == seventhRank && mv.orig.rank != seventhRank) {
        motifs = motifs :+ SeventhRankInvasion(color, plyIndex, Some(san))
      }
    }

    // 6. Semi-open File Control
    if (role == Rook || role == Queen) {
      val file = mv.dest.file
      val ourPawnsOnFile = nextPos.board.pawns & nextPos.board.byColor(color) & Bitboard.file(file)
      val enemyPawnsOnFile = nextPos.board.pawns & nextPos.board.byColor(!color) & Bitboard.file(file)
      
      if (ourPawnsOnFile.isEmpty && enemyPawnsOnFile.nonEmpty) {
        motifs = motifs :+ SemiOpenFileControl(file, color, plyIndex, Some(san))
      }
    }

    // Phase 29: Initiative (Heuristic evalLoss = 0 as we don't have engine context here)
    detectInitiative(mv, pos, nextPos, color, 0, plyIndex, Some(san)).foreach { m =>
      motifs = motifs :+ m
    }

    motifs
  }

  // Helper for mobility calculation (simple count of pseudo-legal moves)
  private def calculateMobility(board: Board, square: Square, role: Role): Int = {
    val occupied = board.occupied
    val targets = role match {
      case Knight => square.knightAttacks
      case Bishop => square.bishopAttacks(occupied)
      case Rook => square.rookAttacks(occupied)
      case Queen => square.queenAttacks(occupied)
      case _ => Bitboard.empty
    }
    // Exclude friendly pieces (cannot capture own)
    val friends = board.byColor(board.colorAt(square).getOrElse(White))
    (targets & ~friends).count
  }

  private def determineCaptureType(attacker: Role, victim: Role): CaptureType =
    val val1 = pieceValue(attacker)
    val val2 = pieceValue(victim)
    if val2 > val1 then CaptureType.Winning
    else if val2 == val1 then CaptureType.Exchange
    else CaptureType.Normal // Default to Normal for BxP, Sacrifice requires ROI/context

  private def detectForkTargets(mv: Move, nextPos: Position, color: Color): List[(Square, Role)] =
    val sq = mv.dest
    val role = mv.piece.role
    val board = nextPos.board
    val attacks: Bitboard = role match
      case Pawn   => sq.pawnAttacks(color)
      case Knight => sq.knightAttacks
      case Bishop => sq.bishopAttacks(board.occupied)
      case Rook   => sq.rookAttacks(board.occupied)
      case Queen  => sq.queenAttacks(board.occupied)
      case King   => sq.kingAttacks
    
    val enemyPieces = board.byColor(!color)
    val targets = attacks & enemyPieces
    
    // Phase 22.5: Filter targets to only those that are meaningful tactical threats.
    // A target is meaningful if it is the King, higher value than attacker, or undefended.
    targets.squares.flatMap { targetSq =>
      val targetRole = board.roleAt(targetSq).getOrElse(Pawn)
      val isKing = targetRole == King
      
      // Simple value check
      val attackerVal = if (role == Queen) 9 else if (role == Rook) 5 else if (role == Bishop || role == Knight) 3 else 1
      val targetVal = if (targetRole == Queen) 9 else if (targetRole == Rook) 5 else if (targetRole == Bishop || targetRole == Knight) 3 else 1
      val isHigherValue = targetVal > attackerVal
      
      // Undefended check: Does the opponent (!color) have any pieces defending targetSq?
      val isUndefended = board.attackers(targetSq, !color).isEmpty
      
      if (isKing || isHigherValue || isUndefended) Some((targetSq, targetRole)) else None
    }

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
      
      // Phase 22.5: Only flag effective pins/skewers where attacker is safe
      val attackerIsSafe = nextPos.board.attackers(sq, !color).isEmpty
      
      if (attackerIsSafe) {
        occupiedBehind.squares.filter(s => Bitboard.between(sq, s).contains(targetSq))
          .minByOption(s => chebyshevDist(sq, s))
          .flatMap { behindSq =>
            board.roleAt(behindSq).flatMap { behindRole =>
              if (board.colorAt(behindSq).contains(!color)) {
                // Phase 22.5: Harden Pin/Skewer thresholds
                // Trivial pins (pawn to knight etc) shouldn't be reported as tactical 'shots'
                val isSignificantPin = (pieceValue(targetRole) < pieceValue(behindRole)) && {
                  if (targetRole == Pawn) List(Rook, Queen, King).contains(behindRole)
                  else (pieceValue(behindRole) - pieceValue(targetRole) >= 2) || behindRole == King
                }
                
                val isSignificantSkewer = (pieceValue(targetRole) > pieceValue(behindRole)) && {
                  behindRole != Pawn // Skewering a piece to a pawn is rarely a 'skewer' worth mentioning
                }

                if (isSignificantPin)
                  Some(Motif.Pin(role, targetRole, behindRole, color, plyIndex, san, Some(sq), Some(targetSq), Some(behindSq)))
                else if (isSignificantSkewer)
                  Some(Motif.Skewer(role, targetRole, behindRole, color, plyIndex, san, Some(sq), Some(targetSq), Some(behindSq)))
                else None
              } else None
            }
          }
      } else None
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
        nextPos.board.roleAt(targetSq).flatMap { targetRole =>
          // Phase 22.5: Filter discovered targets to meaningful ones
          val isKing = targetRole == King
          val attackerVal = pieceValue(role)
          val targetVal = pieceValue(targetRole)
          val isHigherValue = targetVal > attackerVal
          val isUndefended = nextPos.board.attackers(targetSq, !color).isEmpty
          val attackerIsSafe = nextPos.board.attackers(pieceSq, !color).isEmpty

          if (isKing || ((isHigherValue || isUndefended) && attackerIsSafe)) {
            Some(Motif.DiscoveredAttack(movedPiece, role, targetRole, color, plyIndex, san, Some(mv.dest), Some(pieceSq), Some(targetSq)))
          } else None
        }
      }
    }

  private def detectOverloading(
      board: Board, 
      color: Color, 
      plyIndex: Int, 
      san: Option[String]
  ): List[Motif] =
    // Phase 22.5: Only consider squares to be 'critically attacked' if the attacker is safe and target is higher value or undefended
    val criticalSquares = (board.byColor(color) & ~board.kings).squares.filter { sq =>
      val targetRole = board.roleAt(sq).getOrElse(Pawn)
      board.attackers(sq, !color).squares.exists { attackerSq =>
        val attackerRole = board.roleAt(attackerSq).getOrElse(Pawn)
        val isUndefended = board.attackers(sq, color).isEmpty
        val isHigherValue = pieceValue(targetRole) > pieceValue(attackerRole)
        val attackerIsSafe = board.attackers(attackerSq, color).isEmpty
        
        isHigherValue || (isUndefended && attackerIsSafe)
      }
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
      
      // Phase 22.5: Only effective interference where attacker is safe
      val attackerIsSafe = nextPos.board.attackers(blockerSq, !color).isEmpty
      
      if (attackerIsSafe) {
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
    }
    
    blockedDefenses.result()

  private def detectTrappedPiece(
      mv: Move,
      nextPos: Position,
      color: Color,
      plyIndex: Int,
      san: Option[String]
  ): List[Motif] =
    val board = nextPos.board
    val enemyPieces = board.byColor(!color)
    val trapped = List.newBuilder[Motif]
    
    // Check valuable pieces: Queen, Rook, Bishop, Knight
    val valuableRoles = List(Queen, Rook, Bishop, Knight)
    
    val targetSquares = (enemyPieces & (board.queens | board.rooks | board.bishops | board.knights)).squares
    
    targetSquares.foreach { sq =>
      val role = board.roleAt(sq).getOrElse(Pawn)
      // Must be attacked by us
      if (board.attackers(sq, color).nonEmpty) {
        val mobility = calculateMobility(board, sq, role)
        if (mobility == 0) {
           // Trapped!
           trapped += Motif.TrappedPiece(role, sq, !color, plyIndex, san)
        }
      }
    }
    trapped.result()

  private def detectBlockade(mv: Move, nextPos: Position, color: Color, plyIndex: Int, san: Option[String]): Option[Motif] =
    val board = nextPos.board
    // 1. Must be a piece blocking an ENEMY pawn
    val rankVal = mv.dest.rank.value
    // If white, pawn is down (rank - 1); if black, pawn is up (rank + 1)
    val enemyPawnRankVal = if (color.white) rankVal - 1 else rankVal + 1
    
    if (enemyPawnRankVal < 0 || enemyPawnRankVal > 7) return None

    val enemyPawnSqOpt = Square.at(mv.dest.file.value, enemyPawnRankVal)
    
    enemyPawnSqOpt.flatMap { pSq =>
       val pieceBehind = board.roleAt(pSq)
       val colorBehind = board.colorAt(pSq)
       
       if (pieceBehind.contains(Pawn) && colorBehind.contains(!color)) {
         val rank = pSq.rank
         val isAdvanced = if (color.white) rank.value >= 3 else rank.value <= 4 
         
         if (isAdvanced && mv.piece.role == Knight) { 
            Some(Motif.Blockade(Knight, mv.dest, pSq, color, plyIndex, san))
         } else None
       } else None
    }

  private def detectSmotheredMate(mv: Move, nextPos: Position, color: Color, plyIndex: Int, san: Option[String]): Option[Motif] =
    // 1. Must be a Knight delivering mate
    if (mv.piece.role == Knight) {
      val board = nextPos.board
      val enemyKing = board.kingPosOf(!color)
      
      enemyKing.flatMap { kSq =>
        // 2. Check King's valid moves (0, because it's mate)
        // 3. Check if squares around King are occupied by FRIENDS
        val neighbors = kSq.kingAttacks
        val occupiedByFriend = neighbors & board.byColor(!color)
        
        // Smothered if majority of escape squares are blocked by friends
        // Heuristic: If King has no moves (mate) and > 0 neighbors are blocked by friends, and knight checks...
        // Strictly: Smothered means ALL flight squares are blocked by own pieces.
        // But in corner it might be fewer.
        // Let's check if all accessible neighbors are occupied by own pieces.
        val onBoardNeighbors = neighbors
        val allBlockedByFriends = (onBoardNeighbors & ~board.byColor(!color)).isEmpty 
        // Also check if Knight is safe from capture (simplified)
        val isKnightSafe = board.attackers(mv.dest, !color).isEmpty

        if (allBlockedByFriends && isKnightSafe) Some(Motif.SmotheredMate(color, kSq, plyIndex, san))
        else None
      }
    } else None

  def detectStateMotifs(pos: Position, plyIndex: Int): List[Motif] =
    val board = pos.board
    List.concat(
      detectPawnStructure(board, plyIndex),
      detectPins(board, plyIndex),
      detectOpposition(board, pos.color, plyIndex),
      detectOpenFiles(board, plyIndex),
      detectWeakBackRank(pos, plyIndex),
      detectSpaceAdvantage(board, plyIndex),
      detectBattery(board, plyIndex),  // L2 Completion
      detectImbalance(board, plyIndex) // Phase 25: Knight vs Bishop
    )

  private def detectPins(board: Board, plyIndex: Int): List[Motif] =
    // Static absolute pins (to king) for bishops, rooks, and queens.
    // Keep conservative to avoid noise: only detect pins to the king.
    val motifs = List.newBuilder[Motif]

    def addRayPins(from: Square, role: Role, attacker: Color, deltas: List[(Int, Int)]): Unit =
      deltas.foreach { (df, dr) =>
        var f = from.file.value + df
        var r = from.rank.value + dr
        var pinned: Option[(Square, Role)] = None

        def stopRay(): Unit =
          f = 99
          r = 99

        while (f >= 0 && f <= 7 && r >= 0 && r <= 7) do
          Square.at(f, r) match
            case None => stopRay()
            case Some(sq) =>
              (board.colorAt(sq), board.roleAt(sq)) match
                case (None, _) =>
                  f += df; r += dr
                case (Some(hitColor), Some(hitRole)) if hitColor == attacker =>
                  stopRay()
                case (Some(_), Some(hitRole)) =>
                  pinned match
                    case None =>
                      pinned = Some((sq, hitRole))
                      f += df; r += dr
                    case Some((pinnedSq, pinnedRole)) =>
                      if (hitRole == King) then
                        motifs += Motif.Pin(
                          pinningPiece = role,
                          pinnedPiece = pinnedRole,
                          targetBehind = King,
                          color = attacker,
                          plyIndex = plyIndex,
                          move = None,
                          pinningSq = Some(from),
                          pinnedSq = Some(pinnedSq),
                          behindSq = Some(sq)
                        )
                      stopRay()
                case _ => stopRay()
      }

    for attacker <- List(White, Black) do
      (board.bishops & board.byColor(attacker)).squares.foreach { from =>
        addRayPins(from, Bishop, attacker, List((1, 1), (1, -1), (-1, 1), (-1, -1)))
      }
      (board.rooks & board.byColor(attacker)).squares.foreach { from =>
        addRayPins(from, Rook, attacker, List((1, 0), (-1, 0), (0, 1), (0, -1)))
      }
      (board.queens & board.byColor(attacker)).squares.foreach { from =>
        addRayPins(from, Queen, attacker, List((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)))
      }

    motifs.result().distinct

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

  private def detectImbalance(board: Board, plyIndex: Int): List[Motif] =
    var motifs = List.empty[Motif]
    
    // Check for Knight vs Bishop specific imbalance (e.g. White has Knight, Black has Bishop)
    // We only care if roles differ significantly
    val wKnights = board.byPiece(White, Knight).count
    val wBishops = board.byPiece(White, Bishop).count
    val bKnights = board.byPiece(Black, Knight).count
    val bBishops = board.byPiece(Black, Bishop).count
    
    // Scenario 1: White has Knight, Black has Bishop (and no Knight vs no Bishop or similar)
    // Simplified: Look for cases where one side has a Knight and NO Bishop, and other has Bishop and NO Knight (1 vs 1 minor piece endgame/middlegame)
    val whiteHasKnightOnly = wKnights > 0 && wBishops == 0
    val blackHasBishopOnly = bBishops > 0 && bKnights == 0
    
    val blackHasKnightOnly = bKnights > 0 && bBishops == 0
    val whiteHasBishopOnly = wBishops > 0 && wKnights == 0
    
    if ((whiteHasKnightOnly && blackHasBishopOnly) || (blackHasKnightOnly && whiteHasBishopOnly)) {
       // Determine openness
       val pawnCount = board.pawns.count
       val isClosed = pawnCount >= 10 // Heuristic
       // Knights prefer closed, Bishops prefer open
       
       if (whiteHasKnightOnly) {
         val isKnightBetter = isClosed
         motifs = motifs :+ Motif.KnightVsBishop(White, isKnightBetter, plyIndex)
       } else {
         val isKnightBetter = isClosed
         motifs = motifs :+ Motif.KnightVsBishop(Black, isKnightBetter, plyIndex)
       }
    }
    
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
        if isBackward(pawnSq, color, pawnsByFile, oppPawnsByFile) then
          motifs = motifs :+ BackwardPawn(pawnSq.file, pawnSq.rank.value + 1, color, plyIndex)
      }

      pawnsByFile.foreach { (file, sqs) =>
        if sqs.size >= 2 then motifs = motifs :+ Motif.DoubledPawns(file, color, plyIndex)
      }

      val chainPairs = scala.collection.mutable.Set[(File, File)]()
      pawns.squares.foreach { baseSq =>
        val supported = baseSq.pawnAttacks(color) & pawns
        supported.squares.foreach { tipSq =>
          val (base, tip) =
            if (color.white && baseSq.rank.value <= tipSq.rank.value) (baseSq, tipSq)
            else if (!color.white && baseSq.rank.value >= tipSq.rank.value) (baseSq, tipSq)
            else (tipSq, baseSq)
          chainPairs += ((base.file, tip.file))
        }
      }
      chainPairs.foreach { case (baseFile, tipFile) =>
        motifs = motifs :+ Motif.PawnChain(baseFile, tipFile, color, plyIndex)
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
            if color.white then oppPawn.rank.value <= pawnSq.rank.value
            else oppPawn.rank.value >= pawnSq.rank.value
          }
        }
      }
    }

  private def isBackward(
      pawnSq: Square,
      color: Color,
      pawnsByFile: Map[File, List[Square]],
      oppPawnsByFile: Map[File, List[Square]]
  ): Boolean =
    // Minimal heuristic:
    // - no adjacent friendly pawn can support later
    // - and the pawn is blocked from advancing by enemy pawn control on its advance square
    val rank = pawnSq.rank.value
    val fileValue = pawnSq.file.value
    val adjacentFiles = List(fileValue - 1, fileValue + 1).filter(f => f >= 0 && f <= 7)

    val canBeSupportedLater = adjacentFiles.exists { adjFileIdx =>
      File.all.lift(adjFileIdx).exists { adjFile =>
        pawnsByFile.get(adjFile).exists(_.exists { supportSq =>
          if (color.white) supportSq.rank.value <= rank else supportSq.rank.value >= rank
        })
      }
    }

    if (canBeSupportedLater) false
    else
      val advanceSqOpt = Square.at(pawnSq.file.value, if (color.white) pawnSq.rank.value + 1 else pawnSq.rank.value - 1)
      advanceSqOpt.exists { advanceSq =>
        // If enemy pawns control the advance square, it's a more realistic backward pawn.
        val enemyPawnAttacks = oppPawnsByFile.values.flatten.foldLeft(Bitboard.empty) { (bb, oppPawnSq) =>
          bb | oppPawnSq.pawnAttacks(!color)
        }
        enemyPawnAttacks.contains(advanceSq)
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
    
    // 3. Piece activity gain (bishop pair, dominant piece, battery)?
    val ownBishops = (board.bishops & board.byColor(color)).count
    if (ownBishops >= 2) {
      reasons = reasons :+ "bishop_pair"
      totalValue += 50
    }
    
    val hasBattery = detectBattery(board, 0).exists(b => b.color == color && (b.frontSq.exists(_ == mv.dest) || b.backSq.exists(_ == mv.dest)))
    if (hasBattery) {
      reasons = reasons :+ "piece_activity"
      totalValue += 100
    }

    // 4. Passed pawn created or supported?
    val ourPawns = board.byPiece(color, Pawn).squares
    val enemyPawnsByFile = board.byPiece(!color, Pawn).squares.groupBy(_.file)
    val hasPassedPawn = ourPawns.exists(p => isPassed(p, color, enemyPawnsByFile))
    if (hasPassedPawn) {
      reasons = reasons :+ "passed_pawn"
      totalValue += 150
    }
    
    // Only return ROI if there's meaningful compensation (tightened threshold)
    if totalValue >= 200 || (reasons.size >= 2 && totalValue >= 100) then 
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
          // Phase 22.5: Only flag deflection if we safely attack the defender
          val attackerIsSafe = nextPos.board.attackers(mv.dest, color).nonEmpty || board.attackers(defenderSq, !color).isEmpty
          // Wait, the deflection attack comes from mv.dest. We want to know if the piece at mv.dest is safe.
          val destIsSafe = nextPos.board.attackers(mv.dest, !color).isEmpty

          if (destIsSafe) {
            Some(Motif.Deflection(defenderRole, defenderSq, opponent, plyIndex, san))
          } else None
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
        
        // Phase 22.5: Check if we now have a REALLY winning move (mate, winning capture, or fork)
        val hasExecution = afterPos.legalMoves.exists { ourResponse =>
          val givesMate = ourResponse.after.checkMate
          val isWinningCapture = ourResponse.captures && {
             val victim = afterPos.board.roleAt(ourResponse.dest).getOrElse(Pawn)
             val attacker = ourResponse.piece.role
             pieceValue(victim) > pieceValue(attacker)
          }
          val forkTargets = detectForkTargets(ourResponse, ourResponse.after, color)
          val hasRealFork = (forkTargets.contains(King) && forkTargets.size >= 2) || (forkTargets.filter(_ != Pawn).size >= 2)

          givesMate || isWinningCapture || hasRealFork
        }
        
        if (hasExecution) {
          Some(Motif.Decoy(oppCapture.piece.role, mv.dest, !color, plyIndex, san))
        } else None
      }
    }.headOption
  }

  // ============================================================
  // PHASE 27: BISHOP TACTICS (PIN, SKEWER, X-RAY)
  // ============================================================

  private type Dir = Square => Option[Square]
  private def getSquare(f: Int, r: Int): Option[Square] = 
    if (f < 0 || f > 7 || r < 0 || r > 7) None
    else Square.fromKey(s"${('a' + f).toChar}${('1' + r).toChar}")

  private val bishopDirs: List[Dir] = List(
    s => getSquare(s.file.value - 1, s.rank.value + 1),
    s => getSquare(s.file.value + 1, s.rank.value + 1),
    s => getSquare(s.file.value - 1, s.rank.value - 1),
    s => getSquare(s.file.value + 1, s.rank.value - 1)
  )
  private val rookDirs: List[Dir] = List(
    s => getSquare(s.file.value, s.rank.value + 1),
    s => getSquare(s.file.value, s.rank.value - 1),
    s => getSquare(s.file.value - 1, s.rank.value),
    s => getSquare(s.file.value + 1, s.rank.value)
  )
  private val queenDirs: List[Dir] = bishopDirs ++ rookDirs

  private def detectPin(mv: Move, nextPos: Position, color: Color, plyIndex: Int, san: Option[String]): List[Motif.Pin] = {
    val board = nextPos.board
    val moverRole = mv.piece.role
    if (!List(Bishop, Rook, Queen).contains(moverRole)) return Nil

    val directions = moverRole match {
      case Bishop => bishopDirs
      case Rook => rookDirs
      case Queen => queenDirs
      case _ => Nil
    }

    directions.flatMap { dir =>
      // Ray casting: Find first piece, then second piece
      // 1. First piece must be Enemy
      // 2. Second piece must be Enemy (and valuable)
      
      def trace(sq: Square): Option[(Square, Role, Square, Role)] = {
        var current = sq
        var first: Option[(Square, Role)] = None
        
        while (true) {
          dir(current) match {
            case Some(next) =>
              current = next
              board.roleAt(current) match {
                case Some(role) =>
                  val roleColor = board.colorAt(current).get
                  if (first.isEmpty) {
                    if (roleColor != color) first = Some((current, role))
                    else return None // Blocked by friendly
                  } else {
                    // We found the second piece
                    if (roleColor != color) {
                      return Some((first.get._1, first.get._2, current, role))
                    } else return None // Blocked by friendly behind
                  }
                case None => // Empty square, continue
              }
            case None => return None // End of board
          }
        }
        None
      }
      
      trace(mv.dest).flatMap { case (pinnedSq, pinnedRole, behindSq, behindRole) =>
        if (pieceValue(behindRole) > pieceValue(pinnedRole) || behindRole == King) {
          Some(Motif.Pin(
            pinningPiece = moverRole,
            pinnedPiece = pinnedRole,
            targetBehind = behindRole,
            color = color,
            plyIndex = plyIndex,
            move = san,
            pinningSq = Some(mv.dest),
            pinnedSq = Some(pinnedSq),
            behindSq = Some(behindSq)
          ))
        } else None
      }
    }
  }

  private def detectSkewer(mv: Move, nextPos: Position, color: Color, plyIndex: Int, san: Option[String]): List[Motif.Skewer] = {
    val board = nextPos.board
    val moverRole = mv.piece.role
    if (!List(Bishop, Rook, Queen).contains(moverRole)) return Nil

    // Same ray casting logic as Pin, but value check is inverted
    val directions = moverRole match {
      case Bishop => bishopDirs
      case Rook => rookDirs
      case Queen => queenDirs
      case _ => Nil
    }
    
    directions.flatMap { dir =>
      def trace(sq: Square): Option[(Square, Role, Square, Role)] = {
        var current = sq
        var first: Option[(Square, Role)] = None
        while(true) {
          dir(current) match {
            case Some(next) =>
              current = next
              board.roleAt(current) match {
                case Some(role) =>
                  val roleColor = board.colorAt(current).get
                  if (first.isEmpty) {
                    if (roleColor != color) first = Some((current, role))
                    else return None 
                  } else {
                    if (roleColor != color) return Some((first.get._1, first.get._2, current, role))
                    else return None
                  }
                case None => 
              }
            case None => return None
          }
        }
        None
      }
      
      trace(mv.dest).flatMap { case (frontSq, frontRole, backSq, backRole) =>
        // Skewer: Front piece is MORE valuable than Back piece (or is King)
        // And Front piece must be able to move (escape)
        if (pieceValue(frontRole) > pieceValue(backRole) || frontRole == King) {
           Some(Motif.Skewer(
             attackingPiece = moverRole,
             frontPiece = frontRole,
             backPiece = backRole,
             color = color,
             plyIndex = plyIndex,
             move = san,
             attackingSq = Some(mv.dest),
             frontSq = Some(frontSq),
             backSq = Some(backSq)
           ))
        } else None
      }
    }
  }

  private def detectXRay(mv: Move, nextPos: Position, color: Color, plyIndex: Int, san: Option[String]): List[Motif.XRay] = {
    // X-Ray Attack: Attacking a square/piece THROUGH another piece.
    // Common case: Rooks supporting each other, or attacking enemy through enemy.
    // For narrative, X-Ray defense (supporting friendly through friendly) or attack (aiming at target through blocker).
    
    // We'll focus on X-Ray ATTACK: Aiming at a piece behind a blocker on the same line.
    // This is similar to Pin/Skewer but implies the "presence" of the attack on the rear piece.
    // Usually distinguished when the blockage is not absolute or is about to be cleared.
    
    // Implementation: Same ray cast. If aligned, it's an X-Ray.
    // Difference from Pin: Pin focuses on the immobility of the middle piece.
    // Difference from Skewer: Skewer forces the front piece to move.
    // X-Ray is the generic "looking through".
    // We'll detect it if we see two pieces on a line, regardless of values.
    // But to avoid noise, let's limit to:
    // 1. Attacking through FRIENDLY piece (Support)
    // 2. Attacking through ENEMY piece (Pressure)
    
    val board = nextPos.board
    val moverRole = mv.piece.role
    if (!List(Bishop, Rook, Queen).contains(moverRole)) return Nil

    val directions = moverRole match {
      case Bishop => bishopDirs
      case Rook => rookDirs
      case Queen => queenDirs
      case _ => Nil
    }
    
    directions.flatMap { dir =>
      def trace(sq: Square): Option[(Square, Role, Square, Role, Color)] = {
        var current = sq
        var first: Option[(Square, Role, Color)] = None
        while(true) {
          dir(current) match {
            case Some(next) =>
              current = next
              board.roleAt(current) match {
                case Some(role) =>
                  val roleColor = board.colorAt(current).get
                  if (first.isEmpty) {
                    first = Some((current, role, roleColor))
                  } else {
                    // Second piece found
                    return Some((first.get._1, first.get._2, current, role, first.get._3))
                  }
                case None =>
              }
            case None => return None
          }
        }
        None
      }
      
      trace(mv.dest).flatMap { case (midSq, midRole, rearSq, rearRole, midColor) =>
        // Only interesting if targeting enemy or supporting friend
        val rearColor = board.colorAt(rearSq).get
        
        // Case A: Attacking enemy through enemy (Pressure/Pin/Skewer latent)
        // Case B: Attacking enemy through friend (Discovery prep)
        // Case C: Defending friend through friend (Battery/Support)
        // Case D: Defending friend through enemy (X-Ray Defense)
        
        // Let's filter for "X-Ray Attack" -> Rear is Enemy. Mid is Enemy
        if (rearColor != color && midColor != color) {
           Some(Motif.XRay(moverRole, rearRole, rearSq, color, plyIndex, san))
        } else None
      }
    }
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
      
      // Phase 22.5: Was the original square of the moved piece on the ray, AND is the new attack meaningful?
      val ray = Bitboard.ray(friendlySq, origSq)
      val targets = newAttacks & nextBoard.byColor(!color)
      val meaningfulTarget = targets.squares.exists { sq =>
        val targetRole = nextBoard.roleAt(sq).getOrElse(Pawn)
        val attackerRole = friendlyRole
        targetRole == King || pieceValue(targetRole) > pieceValue(attackerRole) || nextBoard.attackers(sq, !color).isEmpty
      }
      val clearedRay = ray.nonEmpty && meaningfulTarget
      
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
              batteries += Motif.Battery(Queen, Rook, Motif.BatteryAxis.File, color, plyIndex, None, Some(queenSq), Some(rookSq))
            }
          } else if (queenSq.rank == rookSq.rank && queenSq != rookSq) {
            if (hasTargetOnLine(queenSq, rookSq, Motif.BatteryAxis.Rank)) {
              batteries += Motif.Battery(Queen, Rook, Motif.BatteryAxis.Rank, color, plyIndex, None, Some(queenSq), Some(rookSq))
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
                batteries += Motif.Battery(Queen, Bishop, Motif.BatteryAxis.Diagonal, color, plyIndex, None, Some(queenSq), Some(bishopSq))
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
            batteries += Motif.Battery(Rook, Rook, Motif.BatteryAxis.File, color, plyIndex, None, Some(r1), Some(r2))
          }
        } else if (r1.rank == r2.rank) {
          if (hasTargetOnLine(r1, r2, Motif.BatteryAxis.Rank)) {
            batteries += Motif.Battery(Rook, Rook, Motif.BatteryAxis.Rank, color, plyIndex, None, Some(r1), Some(r2))
          }
        }
      }
    }
    
    batteries.result()
  }

  // ============================================================
  // PHASE 29: QUEEN MOTIFS & TACTICAL HARDENING
  // ============================================================

  /**
   * Detect Removing the Defender - capturing a piece that protected another.
   */
  private def detectRemovingTheDefender(
      mv: Move,
      pos: Position,
      nextPos: Position,
      color: Color,
      plyIndex: Int,
      san: Option[String]
  ): Option[Motif.RemovingTheDefender] = {
    if (!mv.captures) return None
    
    val board = pos.board
    val nextBoard = nextPos.board
    val capturedSq = mv.dest
    val capturedRole = board.roleAt(capturedSq).getOrElse(Pawn)
    val oppColor = !color

    // 1. What did the captured piece defend in the BEFORE position?
    val defenderAttacks = capturedRole match {
      case Pawn   => capturedSq.pawnAttacks(oppColor)
      case Knight => capturedSq.knightAttacks
      case Bishop => capturedSq.bishopAttacks(board.occupied)
      case Rook   => capturedSq.rookAttacks(board.occupied)
      case Queen  => capturedSq.queenAttacks(board.occupied)
      case King   => capturedSq.kingAttacks
    }
    val protectedSqs = (defenderAttacks & board.byColor(oppColor)).squares

    // 2. For each protected piece, was it the SOLE defender?
    protectedSqs.flatMap { targetSq =>
      val otherDefenders = board.attackers(targetSq, oppColor) & ~Bitboard(capturedSq)
      if (otherDefenders.isEmpty) {
        // It was the sole defender. Is the target now under attack by US in nextPos?
        val currentAttackers = nextBoard.attackers(targetSq, color)
        if (currentAttackers.nonEmpty) {
          val targetRole = nextBoard.roleAt(targetSq).getOrElse(Pawn)
          if (pieceValue(targetRole) > 1 || targetRole == King) { // Only interesting if not just a pawn
             Some(Motif.RemovingTheDefender(mv.piece.role, capturedRole, targetRole, capturedSq, color, plyIndex, san))
          } else None
        } else None
      } else None
    }.headOption
  }

  /**
   * Detect Mate Net - King's mobility restricted by multiple attackers.
   */
  private def detectMateNet(
      nextPos: Position,
      color: Color,
      plyIndex: Int,
      san: Option[String]
  ): Option[Motif.MateNet] = {
    val board = nextPos.board
    val oppColor = !color
    val kingSqOpt = board.kingPosOf(oppColor)
    
    kingSqOpt.flatMap { kingSq =>
      val adjacent = kingSq.kingAttacks
      val safeSquares = (adjacent & ~board.occupied).squares.filter { sq =>
        board.attackers(sq, color).isEmpty
      }
      
      // Heuristic: King has 0 or 1 safe escape squares
      if (safeSquares.size <= 1) {
        val attackers = board.attackers(kingSq, color).squares.flatMap(board.roleAt).toList
        val nearbyAttackers = (kingSq.kingAttacks & board.byColor(color)).count
        
        // At least two pieces coordinating or one very close/powerful (Queen)
        if (nearbyAttackers >= 2 || attackers.contains(Queen)) {
          Some(Motif.MateNet(kingSq, attackers, color, plyIndex, san))
        } else None
      } else None
    }
  }

  /**
   * Detect Initiative - maintaining pressure and active flow.
   */
  private def detectInitiative(
      mv: Move,
      pos: Position,
      nextPos: Position,
      color: Color,
      evalLoss: Int,
      plyIndex: Int,
      san: Option[String]
  ): Option[Motif.Initiative] = {
    // Basic Initiative heuristics:
    // 1. Move is accurate (very low eval loss)
    // 2. Move generates a significant new threat or activity
    val board = nextPos.board
    val isAccurate = evalLoss <= 15
    
    if (isAccurate) {
      val hasNewThreat = nextPos.check.yes || detectForkTargets(mv, nextPos, color).nonEmpty
      val mobilityBefore = calculateMobility(pos.board, mv.orig, mv.piece.role)
      val mobilityAfter = calculateMobility(board, mv.dest, mv.piece.role)
      val activityGain = mobilityAfter > mobilityBefore + 3
      
      if (hasNewThreat || activityGain) {
        val score = (if (hasNewThreat) 10 else 0) + (if (activityGain) 10 else 0) - evalLoss
        if (score > 0) {
          Some(Motif.Initiative(color, score, plyIndex, san))
        } else None
      } else None
    } else None
  }
