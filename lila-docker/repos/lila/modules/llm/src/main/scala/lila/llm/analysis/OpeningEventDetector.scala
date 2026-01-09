package lila.llm.analysis

import lila.llm.model._
import chess.format.{ Fen, Uci }
import chess.variant.Standard

/**
 * A9 Event Detector: Detects opening-related events for narrative generation.
 * 
 * Events:
 * - Intro: ECO/name confirmed at ply 1-3
 * - BranchPoint: Theory diverges
 * - OutOfBook: Played move not in top moves
 * - TheoryEnds: Sample size drops below threshold
 * - Novelty: OutOfBook + good move + constructive evidence
 */
object OpeningEventDetector:

  private val TheoryEndsThresholdEarly = 100  // ply < 10
  private val TheoryEndsThresholdMid = 50     // ply 10-20
  private val TheoryEndsThresholdLate = 20    // ply > 20
  private val OutOfBookPercentage = 0.01      // <1% = out of book
  private val NoveltyCpLossThreshold = 30     // Max cpLoss for novelty

  private def sortedMoves(ref: OpeningReference): List[ExplorerMove] =
    ref.topMoves.sortBy(m => -m.total)

  private def moveShare(move: ExplorerMove, ref: OpeningReference): Double =
    move.total.toDouble / ref.totalGames.max(1)

  private def isOutOfBookMove(
      move: ExplorerMove,
      ref: OpeningReference,
      movesSorted: List[ExplorerMove],
      includeTop5: Boolean
  ): Boolean =
    moveShare(move, ref) < OutOfBookPercentage ||
      (includeTop5 && !movesSorted.take(5).exists(_.uci == move.uci))

  /**
   * Main detection entry point.
   * Returns an event if one should fire, None otherwise.
   */
  def detect(
      ply: Int,
      playedMove: Option[String],
      fen: String,  // For UCI→SAN conversion
      ref: Option[OpeningReference],
      budget: OpeningEventBudget,
      cpLoss: Option[Int],
      hasConstructiveEvidence: Boolean,
      prevRef: Option[OpeningReference] = None
  ): Option[OpeningEvent] = {
    ref match {
      case None => None
      case Some(r) if r.totalGames == 0 => None
      case Some(r) =>
        // Priority: TheoryEnds > Novelty > OutOfBook > BranchPoint > Intro
        detectTheoryEnds(ply, r, prevRef, budget)
          .orElse(detectNovelty(ply, playedMove, fen, r, cpLoss, hasConstructiveEvidence, budget))
          .orElse(detectOutOfBook(ply, playedMove, fen, r, budget))
          .orElse(detectBranchPoint(r, prevRef, budget))
          .orElse(detectIntro(ply, r, budget))
    }
  }

  /**
   * Intro: Fire at ply 1-3 when ECO is confirmed and budget allows.
   */
  def detectIntro(ply: Int, ref: OpeningReference, budget: OpeningEventBudget): Option[OpeningEvent] = {
    if (ply > 6 || !budget.canFireIntro) return None
    
    (ref.eco, ref.name) match {
      case (Some(eco), Some(name)) if ref.totalGames >= 100 =>
        val theme = inferTheme(name)
        val topMoves = sortedMoves(ref).take(3).map(m => s"${m.san} (${m.total}g)")
        Some(OpeningEvent.Intro(eco, name, theme, topMoves))
      case _ => None
    }
  }

  /**
   * BranchPoint: Fire when top move distribution shifts significantly.
   */
  def detectBranchPoint(
      ref: OpeningReference,
      prevRef: Option[OpeningReference],
      budget: OpeningEventBudget
  ): Option[OpeningEvent] = {
    if (!budget.canFireEvent) return None
    
    prevRef match {
      case Some(prev) =>
        val prevTop = sortedMoves(prev).headOption.map(_.san)
        val currTop = sortedMoves(ref).headOption.map(_.san)
        
        // Detect significant shift: top move changed OR top move share dropped significantly
        val topChanged = prevTop.isDefined && currTop.isDefined && prevTop != currTop
        val shareDropped = (sortedMoves(prev).headOption, sortedMoves(ref).headOption) match {
          case (Some(p), Some(c)) =>
            val prevShare = moveShare(p, prev)
            val currShare = moveShare(c, ref)
            prevShare > 0.5 && currShare < 0.3  // Was dominant, now not
          case _ => false
        }
        
        if (topChanged || shareDropped) {
          val diverging = sortedMoves(ref).take(3).map(_.san)
          val reason = if (topChanged) "Main line shifts" else "Theory fragments"
          val game = ref.sampleGames.headOption.map(g => s"lichess.org/${g.id}")
          Some(OpeningEvent.BranchPoint(diverging, reason, game))
        } else None
      case None => None
    }
  }

  /**
   * OutOfBook (Move-Level): Fire when played move is not in top 5 or <1%.
   */
  def detectOutOfBook(
      ply: Int,
      playedMove: Option[String],
      fen: String,
      ref: OpeningReference,
      budget: OpeningEventBudget
  ): Option[OpeningEvent] = {
    if (!budget.canFireEvent) return None
    
    playedMove.flatMap { move =>
      val movesSorted = sortedMoves(ref)
      val moveStats = movesSorted.find(m => m.uci == move || m.san == move)
      
      val isOutOfBook = moveStats match {
        case None => true  // Not in DB at all
        case Some(m) => isOutOfBookMove(m, ref, movesSorted, includeTop5 = true)
      }
      
      if (isOutOfBook) {
        val topAvailable = movesSorted.take(3).map(_.san)
        val moveSan = uciToSan(move, fen, ref.topMoves)
        Some(OpeningEvent.OutOfBook(moveSan, topAvailable, ply))
      } else None
    }
  }

  /**
   * TheoryEnds: Fire when sample size drops below threshold.
   */
  def detectTheoryEnds(
      ply: Int,
      ref: OpeningReference,
      prevRef: Option[OpeningReference],
      budget: OpeningEventBudget
  ): Option[OpeningEvent] = {
    if (budget.theoryEnded) return None  // Already fired
    
    val threshold = 
      if (ply < 10) TheoryEndsThresholdEarly
      else if (ply < 20) TheoryEndsThresholdMid
      else TheoryEndsThresholdLate
    
    val wasAbove = prevRef.exists(_.totalGames >= threshold)
    val nowBelow = ref.totalGames < threshold
    
    if (wasAbove && nowBelow) {
      Some(OpeningEvent.TheoryEnds(ply, ref.totalGames))
    } else None
  }

  /**
   * Novelty: OutOfBook + low cpLoss + constructive evidence.
   */
  def detectNovelty(
      ply: Int,
      playedMove: Option[String],
      fen: String,
      ref: OpeningReference,
      cpLoss: Option[Int],
      hasConstructiveEvidence: Boolean,
      budget: OpeningEventBudget
  ): Option[OpeningEvent] = {
    if (!budget.canFireEvent) return None
    
    // First check if it's out of book
    val isOutOfBook = playedMove.exists { move =>
      val movesSorted = sortedMoves(ref)
      movesSorted.find(m => m.uci == move || m.san == move) match {
        case None => true
        case Some(m) => isOutOfBookMove(m, ref, movesSorted, includeTop5 = false)
      }
    }
    
    if (!isOutOfBook) return None
    
    // Check cpLoss and evidence
    val loss = cpLoss.getOrElse(999)
    if (loss <= NoveltyCpLossThreshold && hasConstructiveEvidence) {
      val move = playedMove.getOrElse("?")
      val moveSan = uciToSan(move, fen, ref.topMoves)
      val evidence = "Stable evaluation with constructive plans"
      Some(OpeningEvent.Novelty(moveSan, loss, evidence, ply))
    } else None
  }

  /**
   * Convert UCI move to SAN.
   * 1. Try topMoves lookup first (fast path)
   * 2. Try converting using FEN context (accurate SAN)
   * 3. Fall back to formatted UCI (e.g., e2e4 → e4)
   */
  private def uciToSan(uci: String, fen: String, topMoves: List[ExplorerMove]): String = {
    // Fast path: lookup in topMoves
    topMoves.find(_.uci == uci).map(_.san).getOrElse {
      // Convert using board context when possible
      uciToSanFromFen(fen, uci).getOrElse(formatUciAsSan(uci))
    }
  }

  /**
   * Convert UCI to SAN using board context from FEN.
   */
  private def uciToSanFromFen(fen: String, uciMove: String): Option[String] =
    for
      pos <- Fen.read(Standard, Fen.Full(fen))
      uci <- Uci(uciMove)
      move <- uci match
        case m: Uci.Move => pos.move(m).toOption
        case _: Uci.Drop => None
    yield move.toSanStr.toString

  /**
   * Best-effort UCI to SAN-like format without board context.
   * e2e4 → e4, e7e8q → e8=Q
   */
  private def formatUciAsSan(uci: String): String = {
    if (uci.length < 4) return uci
    val dest = uci.substring(2, 4)
    val promotion = if (uci.length > 4) s"=${uci(4).toUpper}" else ""
    s"$dest$promotion"
  }

  /**
   * Infer opening theme from name.
   */
  private def inferTheme(name: String): String = {
    val lower = name.toLowerCase
    if (lower.contains("gambit")) "Sacrificial initiative"
    else if (lower.contains("attack")) "Aggressive kingside play"
    else if (lower.contains("defense") || lower.contains("defence")) "Solid defensive setup"
    else if (lower.contains("indian")) "Hypermodern fianchetto"
    else if (lower.contains("sicilian")) "Asymmetrical pawn structure"
    else if (lower.contains("french")) "Central tension and counterplay"
    else if (lower.contains("caro") || lower.contains("slav")) "Solid pawn structure"
    else if (lower.contains("italian") || lower.contains("spanish")) "Classical central control"
    else "Flexible development"
  }
