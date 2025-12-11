package chess
package analysis

/** 동적/전술/실전 느낌의 간단한 점수 계산기. */
object ConceptScorer:

  final case class Scores(
      dynamic: Double,
      drawish: Double,
      imbalanced: Double,
      tacticalDepth: Double,
      blunderRisk: Double,
      pawnStorm: Double,
      fortress: Double,
      colorComplex: Double,
      badBishop: Double,
      goodKnight: Double,
      rookActivity: Double,
      kingSafety: Double,
      dry: Double,
      comfortable: Double,
      unpleasant: Double,
      engineLike: Double,
      conversionDifficulty: Double,
      sacrificeQuality: Double,
      alphaZeroStyle: Double
  )

  def score(
      features: FeatureExtractor.SideFeatures,
      oppFeatures: FeatureExtractor.SideFeatures,
      evalShallowWin: Double,
      evalDeepWin: Double,
      multiPvWin: List[Double],
      position: Position,
      sideToMove: Color,
      san: String = ""
  ): Scores =
    val tacticalDepth = clamp(math.abs(evalDeepWin - evalShallowWin) / 20.0) // 20% 차이를 1.0으로
    val blunderRisk = multiPvRisk(multiPvWin, tacticalDepth)
    val pawnStorm = pawnStormScore(position, sideToMove)
    val kingPressure = clamp(features.kingRingPressure / 8.0)
    val space = clamp(features.spaceControl / 16.0)

    // Human Realized Chaos: Engine doesn't see 'force' as 'dynamic'. We correct this.
    val isViolence = san.exists(c => c == '+' || c == 'x' || c == '=')
    val humanChaosScore = if isViolence then 0.25 else 0.0

    val dynamic = clamp(0.3 * tacticalDepth + 0.2 * blunderRisk + 0.2 * kingPressure + 0.2 * space + 0.2 * pawnStorm + humanChaosScore)
    val drawish = clamp((1.0 - dynamic) * smallEval(evalDeepWin) * smallImbalance(features, oppFeatures))
    val imbalanced = clamp(
      0.35 * materialImbalance(features, oppFeatures) + 0.35 * pawnStructureImbalance(features, oppFeatures) + 0.3 * math.abs(space - clamp(oppFeatures.spaceControl / 16.0))
    )
    val fortress = fortressScore(evalDeepWin, evalShallowWin, features, dynamic)
    val colorComplex = colorComplexScore(position, sideToMove)
    val badBishop = badBishopScore(position, sideToMove)
    val goodKnight = goodKnightScore(position, sideToMove)
    val rookActivity = rookActivityScore(position, sideToMove, features)
    val kingSafety = kingSafetyScore(position, sideToMove, features)
    val dry = dryScore(position, evalShallowWin, evalDeepWin, dynamic, features)
    val comfortable = comfortableScore(multiPvWin)
    val unpleasant = unpleasantScore(multiPvWin)
    val engineLike = engineLikeScore(multiPvWin, tacticalDepth)
    val conversionDifficulty = conversionDifficultyScore(evalDeepWin, fortress, comfortable)
    val sacrificeQuality = sacrificeQualityScore(evalShallowWin, evalDeepWin)
    val alphaZeroStyle = alphaZeroScore(
      position, sideToMove, features, oppFeatures,
      sacrificeQuality, dynamic, kingSafety, rookActivity, goodKnight, colorComplex, imbalanced
    )
    Scores(
      dynamic,
      drawish,
      imbalanced,
      tacticalDepth,
      blunderRisk,
      pawnStorm,
      fortress,
      colorComplex,
      badBishop,
      goodKnight,
      rookActivity,
      kingSafety,
      dry,
      comfortable,
      unpleasant,
      engineLike,
      conversionDifficulty,
      sacrificeQuality,
      alphaZeroStyle
    )

  private def multiPvRisk(winPcts: List[Double], tacticalDepth: Double): Double =
    if winPcts.sizeIs < 2 then 0.0
    else
      val best = winPcts.head
      val worst = winPcts.last
      val spread = clamp((best - worst) / 30.0) // 30% spread -> 1.0
      clamp(0.6 * spread + 0.4 * tacticalDepth)

  private def pawnStormScore(position: Position, color: Color): Double =
    val oppKingSq = position.kingPosOf(!color)
    val hasQueen = (position.board.queens & position.board.byColor(color)).nonEmpty
    val isEndgame = position.board.pieces.size <= 12

    if !hasQueen || isEndgame then return 0.0

    // Pawn storm only makes sense when opponent has castled
    val oppCastled = oppKingSq.exists { ok =>
      ok == Square.G1 || ok == Square.G8 ||  // Kingside castle
      ok == Square.C1 || ok == Square.C8     // Queenside castle
    }
    
    if !oppCastled then return 0.0
    
    // Determine wing based on opponent king position (not our own)
    oppKingSq.fold(0.0) { ok =>
      val wingFiles: Set[Int] = if ok.file.value <= 3 then Set(0, 1, 2, 3) else Set(4, 5, 6, 7)
      val pawns = position.board.pawns & position.board.byColor(color)
      val advanced = pawns.squares.filter { sq =>
        wingFiles.contains(sq.file.value) && (if color == Color.White then sq.rank.value >= 3 else sq.rank.value <= 4)
      }.map(_.file.value).toList.sorted
      val connected = advanced.sliding(2).count {
        case a :: b :: Nil => (b - a) == 1
        case _ => false
      }
      val base =
        if advanced.size >= 3 then 0.8
        else if advanced.size >= 2 then 0.6
        else if advanced.nonEmpty then 0.3
        else 0.0
      val connBonus = if connected > 0 then 0.1 else 0.0
      val oppSideBonus = {
        val myKingSq = position.kingPosOf(color)
        myKingSq.fold(0.0) { ks =>
          val oppWingLeft = ok.file.value <= 3
          val myWingLeft = ks.file.value <= 3
          if oppWingLeft != myWingLeft then 0.1 else 0.0
        }
      }
      clamp(base + connBonus + oppSideBonus)
    }

  private def materialImbalance(f: FeatureExtractor.SideFeatures, opp: FeatureExtractor.SideFeatures): Double =
    val bpSelf = if f.bishopPair then 1 else 0
    val bpOpp = if opp.bishopPair then 1 else 0
    val bishopPairDiff = math.abs(bpSelf - bpOpp) * 0.2  // 한쪽만 가지고 있으면 0.2, 둘 다 있거나 둘 다 없으면 0
    val passedDiff = math.abs(f.passedPawns - opp.passedPawns) * 0.1
    clamp(bishopPairDiff + passedDiff)

  private def pawnStructureImbalance(f: FeatureExtractor.SideFeatures, opp: FeatureExtractor.SideFeatures): Double =
    clamp(math.abs(f.pawnIslands - opp.pawnIslands) * 0.15 + math.abs(f.isolatedPawns - opp.isolatedPawns) * 0.1 + math.abs(f.doubledPawns - opp.doubledPawns) * 0.05)

  private def smallEval(winPct: Double): Double =
    val diff = math.abs(winPct - 50.0)
    clamp(1.0 - diff / 10.0) // 0~10% 범위면 drawish↑

  private def smallImbalance(f: FeatureExtractor.SideFeatures, opp: FeatureExtractor.SideFeatures): Double =
    clamp(1.0 - (materialImbalance(f, opp) + pawnStructureImbalance(f, opp)))

  private def fortressScore(winDeep: Double, winShallow: Double, f: FeatureExtractor.SideFeatures, dynamic: Double): Double =
    val advantage = if winDeep > 60 then 0.3 else 0.0
    val stability = 1.0 - clamp(math.abs(winDeep - winShallow) / 5.0) // 5% 차이면 0
    val closed = clamp(1.0 - (f.rookOpenFiles + f.rookSemiOpenFiles).toDouble / 6.0)
    val breakFew = 1.0 - clamp(breakCandidatesCount / 6.0)
    val infiltrationLow = 1.0 - clamp(infiltrationCount / 3.0)
    clamp(advantage + 0.25 * stability + 0.2 * closed + 0.25 * breakFew + 0.1 * infiltrationLow + 0.2 * (1.0 - dynamic))

  private def colorComplexScore(position: Position, side: Color): Double =
    val attacks = attackedSquares(position.board, side)
    val darkControl = (attacks & Bitboard.darkSquares).count.toDouble
    val lightControl = (attacks & Bitboard.lightSquares).count.toDouble
    val total = darkControl + lightControl + 1.0
    val diff = math.abs(darkControl - lightControl) / total
    val hasBishop = (position.board.bishops & position.board.byColor(side)).nonEmpty
    val pawnSkew = {
      val pawns = position.board.pawns & position.board.byColor(side)
      val darkPawns = (pawns & Bitboard.darkSquares).count
      val lightPawns = (pawns & Bitboard.lightSquares).count
      math.abs(darkPawns - lightPawns).toDouble / (pawns.count + 1).toDouble
    }
    clamp(diff + (if !hasBishop then 0.1 else 0.0) + 0.2 * pawnSkew)

  private def badBishopScore(position: Position, side: Color): Double =
    val bishops = (position.board.bishops & position.board.byColor(side)).squares
    if bishops.isEmpty then 0.0
    else
      val pawns = position.board.pawns & position.board.byColor(side)
      val pawnCount = pawns.count.max(1)
      val scores = bishops.map { b =>
        val mobility = b.bishopAttacks(position.board.occupied).count.toDouble
        val mobilityNorm = clamp(mobility / 13.0) // max mobility ~13
        val sameColorPawns =
          val pawnOnSame = (pawns & (if Bitboard.darkSquares.contains(b) then Bitboard.darkSquares else Bitboard.lightSquares)).count.toDouble
          pawnOnSame / pawnCount
        clamp(0.5 * (1.0 - mobilityNorm) + 0.5 * sameColorPawns)
      }
      scores.sum / scores.size

  private def goodKnightScore(position: Position, side: Color): Double =
    val knights = (position.board.knights & position.board.byColor(side)).squares
    if knights.isEmpty then 0.0
    else
      val friendlyPawns = position.board.pawns & position.board.byColor(side)
      val oppPawns = position.board.pawns & position.board.byColor(!side)
      
      val scores = knights.map { n =>
        // Check if on advanced rank (4-6 for white, 1-3 for black)
        val advanced = side match
          case Color.White => n.rank.value >= 3 && n.rank.value <= 5
          case Color.Black => n.rank.value >= 2 && n.rank.value <= 4
        
        // Check if protected by pawn
        val protectedByPawn = friendlyPawns.squares.exists { p =>
          val attacks = p.pawnAttacks(side)
          attacks.contains(n)
        }
        
        // Check if cannot be attacked by enemy pawns (true outpost)
        val notAttackableByPawns = !oppPawns.squares.exists { p =>
          val attacks = p.pawnAttacks(!side)
          attacks.contains(n) || {
            // Check if pawn can advance to attack
            val fwd = if side == Color.White then p.rank.value - 1 else p.rank.value + 1
            fwd >= 0 && fwd <= 7 && math.abs(p.file.value - n.file.value) == 1
          }
        }
        
        val baseScore = if advanced && protectedByPawn && notAttackableByPawns then 0.8
          else if advanced && protectedByPawn then 0.5
          else if advanced then 0.2
          else 0.0
        
        baseScore
      }
      clamp(scores.max)

  private def rookActivityScore(position: Position, side: Color, f: FeatureExtractor.SideFeatures): Double =
    val rooks = (position.board.rooks & position.board.byColor(side)).squares
    if rooks.isEmpty then 0.0
    else
      val openScore = clamp((f.rookOpenFiles * 0.6 + f.rookSemiOpenFiles * 0.3) / 4.0)
      val seventh =
        rooks.count { r =>
          side match
            case Color.White => r.rank.value >= 6
            case Color.Black => r.rank.value <= 1
        } > 0
      clamp(openScore + (if seventh then 0.3 else 0.0) + 0.2 * (rooks.size.min(2).toDouble / 2.0))

  private def kingSafetyScore(position: Position, side: Color, f: FeatureExtractor.SideFeatures): Double =
    val kingSqOpt = position.kingPosOf(side)
    val oppColor = !side
    kingSqOpt.fold(0.0) { k =>
      // A. Base Exposure (20%) - Reduced weight
      val openAround =
        val files = List(k.file.value - 1, k.file.value, k.file.value + 1).filter(i => i >= 0 && i <= 7)
        val pawns = position.board.pawns & position.board.byColor(side)
        val gaps = files.count { fi => !(pawns & File(fi).get.bb).nonEmpty }
        clamp(gaps.toDouble / 3.0)
      
      // B. Active Threat (60%) - New Logic
      // f.kingRingPressure is essentially 'attackers count' on the king ring
      val attackersCount = f.kingRingPressure
      // Safe Check: If no attackers, the king is safe regardless of exposure
      if attackersCount == 0 then return 0.0

      val pressureScore = clamp(attackersCount.toDouble / 5.0)
      val enemyQueen = (position.board.queens & position.board.byColor(oppColor)).nonEmpty
      val queenThreat = if enemyQueen then 0.2 else 0.0
      
      // Check threat: is there a safe check available? (Basic heuristic using king attacks)
      // We don't have full legality check here easily without making moves, so we skip for now or use simple proxy
      val checkThreat = if position.check.yes then 0.1 else 0.0

      // C. Defender Density (Adjustment)
      // Simple proxy: pieces near king
      // omitted for simplicity to keep it lightweight, reliance on 'attackers' is strong enough

      // Final Calculation
      val rawScore = 0.2 * openAround + 0.5 * pressureScore + queenThreat + checkThreat
      
      // Dampen if no queen
      if !enemyQueen then clamp(rawScore * 0.5) else clamp(rawScore)
    }

  private def dryScore(position: Position, winShallow: Double, winDeep: Double, dynamic: Double, f: FeatureExtractor.SideFeatures): Double =
    val queens = position.board.queens.count
    val queenOff = if queens <= 1 then 0.3 else 0.0
    val lowTension = clamp(1.0 - (f.rookOpenFiles + f.rookSemiOpenFiles).toDouble / 6.0)
    val lowVol = clamp(1.0 - math.abs(winDeep - winShallow) / 10.0)
    val fewBreaks = 1.0 - clamp(breakCandidatesCount / 6.0)
    val lowInfiltration = 1.0 - clamp(infiltrationCount / 3.0)
    clamp(queenOff + 0.25 * lowTension + 0.2 * lowVol + 0.2 * fewBreaks + 0.1 * lowInfiltration + 0.25 * (1.0 - dynamic))

  private def comfortableScore(multiPvWin: List[Double]): Double =
    if multiPvWin.isEmpty then 0.0
    else
      val best = multiPvWin.head
      val min = multiPvWin.last
      val spread = best - min
      clamp(1.0 - spread / 20.0) // spread 작을수록 comfortable

  private def unpleasantScore(multiPvWin: List[Double]): Double =
    if multiPvWin.size < 2 then 0.0
    else
      val best = multiPvWin.head
      val second = multiPvWin(1)
      val spread = best - second
      clamp(spread / 20.0)

  private def engineLikeScore(multiPvWin: List[Double], tacticalDepth: Double): Double =
    if multiPvWin.size < 2 then 0.0
    else
      val best = multiPvWin.head
      val second = multiPvWin(1)
      val gap = best - second
      clamp(gap / 20.0 * 0.6 + tacticalDepth * 0.4)

  private def conversionDifficultyScore(winDeep: Double, fortress: Double, comfortable: Double): Double =
    if winDeep < 60 then 0.0
    else clamp(0.5 * fortress + 0.5 * (1.0 - comfortable))

  private def sacrificeQualityScore(winShallow: Double, winDeep: Double): Double =
    clamp((winDeep - winShallow) / 15.0)

  private def alphaZeroScore(
      position: Position,
      color: Color,
      features: FeatureExtractor.SideFeatures,
      oppFeatures: FeatureExtractor.SideFeatures,
      sacrificeQuality: Double,
      dynamic: Double,
      kingSafety: Double,
      rookActivity: Double,
      goodKnight: Double,
      colorComplex: Double,
      imbalanced: Double
  ): Double =
    // AlphaZero-like play: long-term compensation, piece activity, king attack,
    // complex imbalances, and positional dominance over material
    clamp(
      0.25 * sacrificeQuality +    // 희생 품질 (장기 보상)
      0.15 * kingSafety +          // 상대 킹 공격
      0.15 * rookActivity +        // 룩 활동성
      0.10 * goodKnight +          // 나이트 아웃포스트
      0.10 * colorComplex +        // 색 복합 지배
      0.15 * imbalanced +          // 재료 불균형
      0.10 * dynamic               // 동적 포지션
    )

  private def attackedSquares(board: Board, color: Color): Bitboard =
    Square.all.foldLeft(Bitboard.empty) { (acc, sq) =>
      if board.attackers(sq, color).nonEmpty then acc | sq.bb else acc
    }

  // ===== Helpers for break/infiltration (lightweight heuristics) =====
  private def breakCandidatesCount: Double = BreakCache.count.toDouble
  private def infiltrationCount: Double = BreakCache.infiltration.toDouble

  private object BreakCache:
    private val breakLocal = new ThreadLocal[Int]() { override def initialValue() = 0 }
    private val infiltrateLocal = new ThreadLocal[Int]() { override def initialValue() = 0 }
    def setCounts(breaks: Int, infiltrations: Int): Unit =
      breakLocal.set(breaks)
      infiltrateLocal.set(infiltrations)
    def count: Int = breakLocal.get()
    def infiltration: Int = infiltrateLocal.get()

  def computeBreakAndInfiltration(position: Position, side: Color): Unit =
    val breaks = countBreakCandidates(position, side)
    val infil = countInfiltration(position, side)
    BreakCache.setCounts(breaks, infil)

  private def countBreakCandidates(position: Position, side: Color): Int =
    val pawns = position.board.pawns & position.board.byColor(side)
    val oppPawns = position.board.pawns & position.board.byColor(!side)
    pawns.squares.count { p =>
      val forwardRankVal = p.rank.value + (if side == Color.White then 1 else -1)
      val forwardSq = Rank(forwardRankVal).flatMap(r => Square.at(p.file.value, r.value))
      val forwardEmpty = forwardSq.exists(sq => !position.board.isOccupied(sq))
      val captureSquares = p.pawnAttacks(side).squares
      val captureOpponent = captureSquares.exists(sq => oppPawns.contains(sq))
      forwardEmpty || captureOpponent
    }

  private def countInfiltration(position: Position, side: Color): Int =
    val rooksQueens = (position.board.rooks | position.board.queens) & position.board.byColor(side)
    rooksQueens.squares.count { sq =>
      val attacks = (sq.rookAttacks(position.board.occupied) | sq.bishopAttacks(position.board.occupied))
      attacks.squares.exists { target =>
        if side == Color.White then target.rank.value >= 4 else target.rank.value <= 3
      }
    }

  private def clamp(d: Double): Double =
    if d < 0 then 0.0 else if d > 1 then 1.0 else d
