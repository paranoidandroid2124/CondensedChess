package lila.llm.analysis

import lila.llm.model._

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
        val theme = inferTheme(name, sortedMoves(ref).take(4).map(_.san))
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
          val reason = branchPointReason(ref, topChanged)
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
      NarrativeUtils.uciToSanOrFormat(fen, uci)
    }
  }

  /**
   * Infer opening theme from name.
   */
  private def inferTheme(name: String, topMoves: List[String] = Nil): String = {
    val lower = name.toLowerCase
    inferMoveTheme(topMoves)
      .orElse {
        if lower.contains("scandinavian") || lower.contains("center game") then Some("early queen exposure")
        else if lower.contains("catalan") || lower.contains("english") || lower.contains("reti") then Some("flank fianchetto support")
        else if
          lower.contains("queen's gambit") || lower.contains("queens gambit") ||
          lower.contains("slav") || lower.contains("grunfeld") || lower.contains("benoni")
        then Some("center reaction")
        else if lower.contains("dragon") || lower.contains("yugoslav") then Some("castle race")
        else if
          lower.contains("king's indian") || lower.contains("kings indian") ||
          lower.contains("pirc") || lower.contains("modern")
        then Some("thematic break preparation")
        else if
          lower.contains("italian") || lower.contains("spanish") || lower.contains("ruy") ||
          lower.contains("vienna") || lower.contains("scotch") || lower.contains("four knights")
        then Some("development logic")
        else None
      }
      .orElse {
        if (lower.contains("gambit")) Some("Sacrificial initiative")
        else if (lower.contains("attack")) Some("Aggressive kingside play")
        else if (lower.contains("defense") || lower.contains("defence")) Some("Solid defensive setup")
        else if (lower.contains("indian")) Some("Hypermodern fianchetto")
        else if (lower.contains("sicilian")) Some("Asymmetrical pawn structure")
        else if (lower.contains("french")) Some("Central tension and counterplay")
        else if (lower.contains("caro") || lower.contains("slav")) Some("Solid pawn structure")
        else if (lower.contains("italian") || lower.contains("spanish")) Some("Classical central control")
        else None
      }
      .getOrElse("Flexible development")
  }

  private def branchPointReason(ref: OpeningReference, topChanged: Boolean): String =
    inferMoveTheme(sortedMoves(ref).take(4).map(_.san)) match
      case Some(theme @ ("development logic" | "center reaction" | "flank fianchetto support" |
          "early queen exposure" | "castle race" | "thematic break preparation")) =>
        if topChanged then s"Main line shifts toward $theme"
        else s"Theory fragments around $theme"
      case _ =>
        if topChanged then "Main line shifts" else "Theory fragments"

  private def inferMoveTheme(moves: List[String]): Option[String] = {
    val normalized = moves.map(normalizeSan).filter(_.nonEmpty)
    val scores = List(
      "development logic" -> scoreDevelopmentLogic(normalized),
      "center reaction" -> scoreCenterReaction(normalized),
      "flank fianchetto support" -> scoreFianchettoSupport(normalized),
      "early queen exposure" -> scoreQueenExposure(normalized),
      "castle race" -> scoreCastleRace(normalized),
      "thematic break preparation" -> scoreBreakPreparation(normalized)
    )
    scores.maxByOption(_._2).collect { case (label, score) if score >= 2 => label }
  }

  private def normalizeSan(raw: String): String =
    Option(raw).getOrElse("").trim.replaceAll("""[+#?!]+$""", "")

  private def isMinorDevelopmentMove(move: String): Boolean =
    move.matches("""^[NB](?!x).*[a-h][1-8]$""")

  private def isCenterReactionMove(move: String): Boolean =
    move.matches("""^(c|d|e|f)[3-6]$""") ||
      List("cxd4", "cxd5", "dxc4", "dxe4", "exd4", "exd5", "fxe4", "fxe5").contains(move)

  private def isFianchettoMove(move: String): Boolean =
    Set("g3", "b3", "g6", "b6", "Bg2", "Bb2", "Bg7", "Bb7").contains(move)

  private def isQueenMove(move: String): Boolean =
    move.startsWith("Q")

  private def isCastleMove(move: String): Boolean =
    move == "O-O" || move == "O-O-O"

  private def isBreakPreparationMove(move: String): Boolean =
    Set(
      "c3", "d3", "f3", "a3", "h3", "Qc2", "Qe2", "Be2", "Bd3", "Re1",
      "c6", "d6", "f6", "a6", "h6", "Qc7", "Qe7", "Be7", "Bd6", "Re8",
      "b4", "b5", "g4", "g5", "f4", "f5"
    ).contains(move)

  private def scoreDevelopmentLogic(moves: List[String]): Int =
    val minorMoves = moves.count(isMinorDevelopmentMove)
    minorMoves + Option.when(moves.exists(isCastleMove))(1).getOrElse(0) - Option.when(moves.exists(isQueenMove))(1).getOrElse(0)

  private def scoreCenterReaction(moves: List[String]): Int =
    val centerMoves = moves.count(isCenterReactionMove)
    centerMoves + Option.when(moves.exists(_.contains("x")))(1).getOrElse(0)

  private def scoreFianchettoSupport(moves: List[String]): Int =
    val fianchettoMoves = moves.count(isFianchettoMove)
    fianchettoMoves * 2 + Option.when(moves.exists(isMinorDevelopmentMove))(1).getOrElse(0)

  private def scoreQueenExposure(moves: List[String]): Int =
    val queenMoves = moves.count(isQueenMove)
    queenMoves * 2 + Option.when(moves.exists(m => m.startsWith("Q") && m.contains("x")))(1).getOrElse(0)

  private def scoreCastleRace(moves: List[String]): Int =
    val castles = moves.count(isCastleMove)
    val flankCommitments = moves.count(m => Set("g4", "g5", "h4", "h5", "a4", "a5", "b4", "b5").contains(m))
    castles * 2 + Option.when(castles >= 1 && flankCommitments >= 1)(1).getOrElse(0)

  private def scoreBreakPreparation(moves: List[String]): Int =
    val prepMoves = moves.count(isBreakPreparationMove)
    prepMoves * 2 + Option.when(moves.exists(isCenterReactionMove))(1).getOrElse(0)
