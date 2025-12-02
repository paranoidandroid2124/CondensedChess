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
      sideToMove: Color
  ): Scores =
    val tacticalDepth = clamp(math.abs(evalDeepWin - evalShallowWin) / 20.0) // 20% 차이를 1.0으로
    val blunderRisk = multiPvRisk(multiPvWin, tacticalDepth)
    val pawnStorm = pawnStormScore(position, sideToMove)
    val kingPressure = clamp(features.kingRingPressure / 8.0)
    val space = clamp(features.spaceControl / 16.0)
    val dynamic = clamp(0.25 * tacticalDepth + 0.2 * blunderRisk + 0.2 * kingPressure + 0.2 * space + 0.15 * pawnStorm)
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
    val alphaZeroStyle = alphaZeroScore(sacrificeQuality, pawnStorm, dynamic)
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
    val kingSq = position.kingPosOf(color)
    val oppKingSq = position.kingPosOf(!color)
    kingSq.fold(0.0) { ks =>
      val wingFiles: Set[Int] = if ks.file.value <= 3 then Set(0, 1, 2, 3) else Set(4, 5, 6, 7)
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
      val oppSideBonus = oppKingSq.fold(0.0) { ok =>
        val oppWingLeft = ok.file.value <= 3
        if oppWingLeft != (wingFiles.headOption.exists(_ <= 3)) then 0.1 else 0.0
      }
      clamp(base + connBonus + oppSideBonus)
    }

  private def materialImbalance(f: FeatureExtractor.SideFeatures, opp: FeatureExtractor.SideFeatures): Double =
    val bishopPairDiff = if f.bishopPair && !opp.bishopPair then 0.2 else if !f.bishopPair && opp.bishopPair then 0.0 else 0.1
    clamp(bishopPairDiff + math.abs(f.passedPawns - opp.passedPawns) * 0.1)

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
      val enemyPawns = position.board.pawns & position.board.byColor(!side)
      val enemyPawnAttacks = enemyPawns.fold(Bitboard.empty)((acc, sq) => acc | sq.pawnAttacks(!side))
      val ourPawns = position.board.pawns & position.board.byColor(side)
      val ourPawnAttacks = ourPawns.fold(Bitboard.empty)((acc, sq) => acc | sq.pawnAttacks(side))
      val scores = knights.map { n =>
        val outpost = !enemyPawnAttacks.contains(n) && ourPawnAttacks.contains(n)
        val centrality = 1.0 - (math.abs(n.file.value - 3.5) + math.abs(n.rank.value - 3.5)) / 7.0
        val attacks = (n.knightAttacks & position.board.byColor(!side)).count.toDouble / 5.0
        clamp((if outpost then 0.4 else 0.0) + 0.4 * clamp(centrality) + 0.2 * clamp(attacks))
      }
      scores.sum / scores.size

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
    kingSqOpt.fold(0.0) { k =>
      val ringPressure = clamp(f.kingRingPressure / 8.0)
      val openAround =
        val files = List(k.file.value - 1, k.file.value, k.file.value + 1).filter(i => i >= 0 && i <= 7)
        val pawns = position.board.pawns & position.board.byColor(side)
        val gaps = files.count { fi => !(pawns & File(fi).get.bb).nonEmpty }
        clamp(gaps.toDouble / 3.0)
      clamp(0.6 * ringPressure + 0.4 * openAround)
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

  private def alphaZeroScore(sacrificeQuality: Double, pawnStorm: Double, dynamic: Double): Double =
    clamp(0.5 * sacrificeQuality + 0.25 * pawnStorm + 0.25 * dynamic)

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
