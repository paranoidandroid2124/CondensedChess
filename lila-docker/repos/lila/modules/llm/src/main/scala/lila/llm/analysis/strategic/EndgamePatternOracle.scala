package lila.llm.analysis.strategic

import chess.*
import lila.llm.model.strategic.*

object EndgamePatternOracle:

  final case class PatternSignalOverrides(
      oppositionType: Option[EndgameOppositionType] = None,
      ruleOfSquare: Option[RuleOfSquareStatus] = None,
      triangulationAvailable: Option[Boolean] = None,
      kingActivityDelta: Option[Int] = None,
      rookEndgamePattern: Option[RookEndgamePattern] = None,
      zugzwangLikelihood: Option[Double] = None,
      theoreticalOutcomeHint: Option[TheoreticalOutcomeHint] = None
  )

  final case class PatternMatch(
      id: String,
      outcomeOverride: Option[TheoreticalOutcomeHint] = None,
      confidenceFloor: Double,
      signalOverrides: PatternSignalOverrides = PatternSignalOverrides(),
      ambiguityPenalty: Double = 0.0
  )

  private type Detector = (Board, Color, EndgameFeature) => Option[PatternMatch]

  private val PatternConfidenceThreshold = 0.70
  private val ZugzwangThreshold = 0.65

  private val orderedDetectors: List[Detector] = List(
    detectWrongRookPawnWrongBishopFortress,
    detectVancuraDefense,
    detectPhilidorDefense,
    detectLucena,
    detectTriangulationZugzwang,
    detectKeySquaresOppositionBreakthrough,
    detectConnectedPassers,
    detectOutsidePasserDecoy,
    detectBreakthroughSacrifice,
    detectShouldering,
    detectRetiManeuver,
    detectShortSideDefense,
    detectOppositeColoredBishopsDraw,
    detectGoodBishopRookPawnConversion,
    detectKnightBlockadeRookPawnDraw
  )

  def detect(board: Board, color: Color, coreFeature: EndgameFeature): Option[PatternMatch] =
    orderedDetectors.iterator.flatMap(det => det(board, color, coreFeature)).take(1).toList.headOption

  def applyPattern(core: EndgameFeature, pattern: PatternMatch): EndgameFeature =
    val overrides = pattern.signalOverrides
    val updatedZugLikelihood = overrides.zugzwangLikelihood.getOrElse(core.zugzwangLikelihood)
    val updatedOutcome = overrides.theoreticalOutcomeHint
      .orElse(pattern.outcomeOverride)
      .getOrElse(core.theoreticalOutcomeHint)
    val adjustedConfidence = clamp01(math.max(core.confidence, pattern.confidenceFloor) - pattern.ambiguityPenalty)
    if adjustedConfidence < PatternConfidenceThreshold then core
    else
      core.copy(
        oppositionType = overrides.oppositionType.getOrElse(core.oppositionType),
        ruleOfSquare = overrides.ruleOfSquare.getOrElse(core.ruleOfSquare),
        triangulationAvailable = overrides.triangulationAvailable.getOrElse(core.triangulationAvailable),
        kingActivityDelta = overrides.kingActivityDelta.getOrElse(core.kingActivityDelta),
        rookEndgamePattern = overrides.rookEndgamePattern.getOrElse(core.rookEndgamePattern),
        zugzwangLikelihood = updatedZugLikelihood,
        isZugzwang = updatedZugLikelihood >= ZugzwangThreshold || core.isZugzwang,
        theoreticalOutcomeHint = updatedOutcome,
        confidence = adjustedConfidence,
        primaryPattern = Some(pattern.id)
      )

  private def detectWrongRookPawnWrongBishopFortress(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    List(color, !color).collectFirst(Function.unlift { attacker =>
      val defender = !attacker
      val matA = material(board, attacker)
      val matD = material(board, defender)
      if matA.bishops != 1 || matA.rooks > 0 || matA.queens > 0 || matA.knights > 0 then None
      else if matD.rooks > 0 || matD.queens > 0 || matD.bishops > 0 || matD.knights > 0 then None
      else
        val bishopSq = board.byPiece(attacker, Bishop).squares.headOption
        val pawnOpt = board.byPiece(attacker, Pawn).squares
          .filter(isRookPawn)
          .sortBy(p => -relativeRank(p, attacker))
          .headOption
        for
          bishop <- bishopSq
          pawn <- pawnOpt
          promo <- promotionSquare(pawn, attacker)
          dKing <- board.kingPosOf(defender)
          if chebyshev(dKing, promo) <= 1
          if relativeRank(pawn, attacker) >= 7
          if
            val classicalWrongBishop = bishop.isLight != promo.isLight
            val distantFortress = bishop.isLight == promo.isLight &&
              chebyshev(bishop, pawn) >= 3 &&
              fileDistance(bishop.file, pawn.file) >= 3
            classicalWrongBishop || distantFortress
        yield
          val classicalWrongBishop = bishop.isLight != promo.isLight
          PatternMatch(
            id = "WrongRookPawnWrongBishopFortress",
            outcomeOverride = Some(TheoreticalOutcomeHint.Draw),
            confidenceFloor = if classicalWrongBishop then 0.90 else 0.82,
            ambiguityPenalty = if classicalWrongBishop && core.kingActivityDelta.abs > 3 then 0.06 else 0.0
          )
    })

  private def detectVancuraDefense(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    List((color, !color), (!color, color)).collectFirst(Function.unlift { (defender, attacker) =>
      val matA = material(board, attacker)
      val matD = material(board, defender)
      if matA.rooks != 1 || matD.rooks != 1 then None
      else if matA.queens > 0 || matD.queens > 0 || matA.knights + matA.bishops + matD.knights + matD.bishops > 0 then None
      else
        val rookPawn = passedPawns(board, attacker)
          .filter(p => isFlankPawn(p) && relativeRank(p, attacker) == 6)
          .sortBy(p => -relativeRank(p, attacker))
          .headOption
        for
          pawn <- rookPawn
          dRook <- board.byPiece(defender, Rook).squares.headOption
          if dRook.rank == pawn.rank
          if fileDistance(dRook.file, pawn.file) >= 1
          if !isRookBehindPawn(board, attacker, pawn)
        yield
          PatternMatch(
            id = "VancuraDefense",
            outcomeOverride = Some(TheoreticalOutcomeHint.Draw),
            confidenceFloor = 0.76,
            signalOverrides = PatternSignalOverrides(
              rookEndgamePattern = Some(RookEndgamePattern.KingCutOff)
            ),
            ambiguityPenalty = if core.kingActivityDelta > 3 then 0.06 else 0.01
          )
    })

  private def detectPhilidorDefense(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    List((color, !color), (!color, color)).collectFirst(Function.unlift { (defender, attacker) =>
      val matA = material(board, attacker)
      val matD = material(board, defender)
      if matA.rooks != 1 || matD.rooks != 1 then None
      else if matA.queens > 0 || matD.queens > 0 || matA.knights + matA.bishops + matD.knights + matD.bishops > 0 then None
      else
        val candidatePawn = board.byPiece(attacker, Pawn).squares.sortBy(p => -relativeRank(p, attacker)).headOption
        val barrierRank = if attacker.white then Rank.Sixth else Rank.Third
        for
          pawn <- candidatePawn
          dKing <- board.kingPosOf(defender)
          aKing <- board.kingPosOf(attacker)
          if relativeRank(pawn, attacker) <= 5
          if board.byPiece(defender, Rook).squares.exists(_.rank == barrierRank)
          if (if attacker.white then dKing.rank.value > pawn.rank.value else dKing.rank.value < pawn.rank.value)
          if relativeRank(aKing, attacker) <= 5
        yield
          PatternMatch(
            id = "PhilidorDefense",
            outcomeOverride = Some(TheoreticalOutcomeHint.Draw),
            confidenceFloor = 0.78,
            signalOverrides = PatternSignalOverrides(
              rookEndgamePattern = Some(RookEndgamePattern.KingCutOff)
            ),
            ambiguityPenalty = if core.kingActivityDelta > 2 then 0.06 else 0.01
          )
    })

  private def detectLucena(board: Board, color: Color, core: EndgameFeature): Option[PatternMatch] =
    val matUs = material(board, color)
    val matThem = material(board, !color)
    if matUs.rooks != 1 || matThem.rooks != 1 then None
    else if matUs.queens > 0 || matThem.queens > 0 || matUs.knights + matUs.bishops + matThem.knights + matThem.bishops > 0 then None
    else
      val passerOpt = passedPawns(board, color)
        .filter(p => !isRookPawn(p) && relativeRank(p, color) >= 6)
        .sortBy(p => -relativeRank(p, color))
        .headOption
      for
        pawn <- passerOpt
        promo <- promotionSquare(pawn, color)
        ourKing <- board.kingPosOf(color)
        theirKing <- board.kingPosOf(!color)
        ourRook <- board.byPiece(color, Rook).squares.headOption
        if chebyshev(ourKing, promo) <= 1
        if chebyshev(theirKing, promo) >= 2
        if ourRook.file != pawn.file
        if fileDistance(ourRook.file, pawn.file) >= 2
      yield
        PatternMatch(
          id = "Lucena",
          outcomeOverride = Some(TheoreticalOutcomeHint.Win),
          confidenceFloor = 0.82,
          signalOverrides = PatternSignalOverrides(
            rookEndgamePattern = Some(RookEndgamePattern.RookBehindPassedPawn)
          ),
          ambiguityPenalty = if core.ruleOfSquare == RuleOfSquareStatus.Fails then 0.08 else 0.0
        )

  private def detectTriangulationZugzwang(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    if !hasOnlyKingsAndPawns(board) then None
    else if !core.triangulationAvailable || core.oppositionType == EndgameOppositionType.None || core.oppositionType == EndgameOppositionType.Diagonal then None
    else if core.zugzwangLikelihood < 0.62 then None
    else
      val ourPawns = board.byPiece(color, Pawn).count
      val theirPawns = board.byPiece(!color, Pawn).count
      val hasPassed = passedPawns(board, color).nonEmpty || passedPawns(board, !color).nonEmpty
      if ourPawns != 2 || theirPawns != 2 || hasPassed then None
      else
        (board.kingPosOf(color), board.kingPosOf(!color)) match
          case (Some(ourKing), Some(theirKing)) =>
            val safeSquares = ourKing.kingAttacks.squares.count { sq =>
              !board.byColor(color).contains(sq) && chebyshev(sq, theirKing) > 1
            }
            if safeSquares < 2 || chebyshev(ourKing, theirKing) > 2 then None
            else
              Some(
                PatternMatch(
                  id = "TriangulationZugzwang",
                  confidenceFloor = 0.75,
                  signalOverrides = PatternSignalOverrides(
                    triangulationAvailable = Some(true),
                    zugzwangLikelihood = Some(math.max(core.zugzwangLikelihood, 0.75))
                  ),
                  ambiguityPenalty = 0.03
                )
              )
          case _ => None

  private def detectKeySquaresOppositionBreakthrough(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    if !hasOnlyKingsAndPawns(board) then None
    else if core.oppositionType == EndgameOppositionType.None then None
    else
      val passers = passedPawns(board, color).filterNot(isRookPawn)
      if passers.size != 1 then None
      else
        val pawn = passers.head
        (board.kingPosOf(color), board.kingPosOf(!color)) match
          case (Some(ourKing), Some(theirKing)) =>
            val kingNearPawn = chebyshev(ourKing, pawn) <= 1
            val kingContest = chebyshev(theirKing, pawn) <= 3
            if !kingNearPawn || !kingContest then None
            else
              Some(
                PatternMatch(
                  id = "KeySquaresOppositionBreakthrough",
                  outcomeOverride = Some(TheoreticalOutcomeHint.Win),
                  confidenceFloor = 0.82,
                  signalOverrides = PatternSignalOverrides(
                    zugzwangLikelihood = Some(math.max(core.zugzwangLikelihood, 0.65))
                  ),
                  ambiguityPenalty = if core.ruleOfSquare == RuleOfSquareStatus.Fails then 0.10 else 0.02
                )
              )
          case _ => None

  private def detectConnectedPassers(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    val pairs = connectedPassedPairs(board, color).filter { case (a, b) =>
      relativeRank(a, color) >= 5 && relativeRank(b, color) >= 5
    }
    if pairs.isEmpty then None
    else if board.queens.count > 0 || board.byPiece(!color, Rook).count > 0 then None
    else
      val blockedByKing = board.kingPosOf(!color).exists { enemyKing =>
        pairs.exists { case (a, b) => chebyshev(enemyKing, a) <= 1 && chebyshev(enemyKing, b) <= 1 }
      }
      if blockedByKing then None
      else
        Some(
          PatternMatch(
            id = "ConnectedPassers",
            outcomeOverride = Some(TheoreticalOutcomeHint.Win),
            confidenceFloor = 0.78,
            ambiguityPenalty = if core.kingActivityDelta <= 0 then 0.07 else 0.0
          )
        )

  private def detectOutsidePasserDecoy(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    if !hasOnlyKingsAndPawns(board) then None
    else
      val passers = passedPawns(board, color)
      val outside = passers.filter(p => p.file == File.A || p.file == File.B || p.file == File.G || p.file == File.H)
      val supportPawnExists = outside.exists { out =>
        board.byPiece(color, Pawn).squares.exists(p => fileDistance(p.file, out.file) >= 3)
      }
      val enemyFrontBlock = outside.exists { out =>
        board.kingPosOf(!color).exists { k =>
          k.file == out.file && (if color.white then k.rank.value > out.rank.value else k.rank.value < out.rank.value)
        }
      }
      if outside.isEmpty || !supportPawnExists || core.kingActivityDelta < 0 || enemyFrontBlock then None
      else
        Some(
          PatternMatch(
            id = "OutsidePasserDecoy",
            outcomeOverride = Some(TheoreticalOutcomeHint.Win),
            confidenceFloor = 0.76,
            ambiguityPenalty = 0.01
          )
        )

  private def detectBreakthroughSacrifice(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    if !hasOnlyKingsAndPawns(board) then None
    else
      val ourPawns = board.byPiece(color, Pawn).squares
      val targetRank = if color.white then Rank.Fifth else Rank.Fourth
      val frontRank = if color.white then Rank.Sixth else Rank.Third
      val onTarget = ourPawns.filter(_.rank == targetRank)
      val files = onTarget.map(_.file.value).sorted
      val hasTriplet = files.sliding(3).exists {
        case List(a, b, c) => b == a + 1 && c == b + 1
        case _ => false
      }
      val enemyFront = board.byPiece(!color, Pawn).squares.count(_.rank == frontRank)
      if !hasTriplet || enemyFront < 3 then None
      else
        Some(
          PatternMatch(
            id = "BreakthroughSacrifice",
            outcomeOverride = Some(TheoreticalOutcomeHint.Win),
            confidenceFloor = 0.74,
            signalOverrides = PatternSignalOverrides(
              zugzwangLikelihood = Some(math.max(core.zugzwangLikelihood, 0.70))
            ),
            ambiguityPenalty = 0.0
          )
        )

  private def detectShouldering(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    if !hasOnlyKingsAndPawns(board) then None
    else
      val pawnOpt = board.byPiece(color, Pawn).squares.sortBy(p => -relativeRank(p, color)).headOption
      (board.kingPosOf(color), board.kingPosOf(!color), pawnOpt) match
        case (Some(ourKing), Some(theirKing), Some(pawn)) =>
          val kingClose = chebyshev(ourKing, theirKing) <= 2
          val ourNearPawn = chebyshev(ourKing, pawn) <= 1
          val enemyNearPawn = chebyshev(theirKing, pawn) <= 2
          val oppositeFlanks =
            (ourKing.file.value - pawn.file.value) * (theirKing.file.value - pawn.file.value) < 0
          val progressShield =
            if color.white then ourKing.rank.value >= pawn.rank.value
            else ourKing.rank.value <= pawn.rank.value
          if !kingClose || !ourNearPawn || !enemyNearPawn || !oppositeFlanks || !progressShield then None
          else
            Some(
              PatternMatch(
                id = "Shouldering",
                confidenceFloor = 0.74,
                signalOverrides = PatternSignalOverrides(
                  zugzwangLikelihood = Some(math.max(core.zugzwangLikelihood, 0.65))
                ),
                ambiguityPenalty = 0.0
              )
            )
        case _ => None

  private def detectRetiManeuver(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    val _ = core
    if !hasOnlyKingsAndPawns(board) then None
    else
      val ourPassers = passedPawns(board, color).filter(isRookPawn).sortBy(p => -relativeRank(p, color))
      val enemyPassers = passedPawns(board, !color).filterNot(isRookPawn).sortBy(p => -relativeRank(p, !color))
      (board.kingPosOf(color), ourPassers.headOption, enemyPassers.headOption) match
        case (Some(ourKing), Some(ourPawn), Some(enemyPawn)) =>
          val flankSplit = fileDistance(ourPawn.file, enemyPawn.file) >= 5
          val cornerTarget = promotionSquare(ourPawn, color)
          val kingOnCorner = cornerTarget.contains(ourKing)
          if !flankSplit || !kingOnCorner then None
          else
            Some(
              PatternMatch(
                id = "RetiManeuver",
                confidenceFloor = 0.74,
                ambiguityPenalty = 0.0
              )
            )
        case _ => None

  private def detectShortSideDefense(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    List((color, !color), (!color, color)).collectFirst(Function.unlift { (defender, attacker) =>
      val matA = material(board, attacker)
      val matD = material(board, defender)
      if matA.rooks != 1 || matD.rooks != 1 then None
      else if matA.queens > 0 || matD.queens > 0 || matA.knights + matA.bishops + matD.knights + matD.bishops > 0 then None
      else
        val pawnOpt = board.byPiece(attacker, Pawn).squares
          .filter(p => isFlankPawn(p) && relativeRank(p, attacker) >= 5)
          .sortBy(p => -relativeRank(p, attacker))
          .headOption
        for
          pawn <- pawnOpt
          dKing <- board.kingPosOf(defender)
          dRook <- board.byPiece(defender, Rook).squares.headOption
          if isShortSideCorner(dKing, pawn, attacker)
          if dRook.rank == pawn.rank
          if fileDistance(dRook.file, pawn.file) >= 2
        yield
          PatternMatch(
            id = "ShortSideDefense",
            outcomeOverride = Some(TheoreticalOutcomeHint.Draw),
            confidenceFloor = 0.76,
            signalOverrides = PatternSignalOverrides(
              rookEndgamePattern = Some(RookEndgamePattern.KingCutOff)
            ),
            ambiguityPenalty = if core.kingActivityDelta > 2 then 0.05 else 0.01
          )
    })

  private def detectOppositeColoredBishopsDraw(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    val _ = color
    val wMat = material(board, Color.White)
    val bMat = material(board, Color.Black)
    if wMat.bishops != 1 || bMat.bishops != 1 then None
    else if wMat.rooks + wMat.queens + wMat.knights + bMat.rooks + bMat.queens + bMat.knights > 0 then None
    else
      val wBishop = board.byPiece(Color.White, Bishop).squares.headOption
      val bBishop = board.byPiece(Color.Black, Bishop).squares.headOption
      (wBishop, bBishop) match
        case (Some(wb), Some(bb)) =>
          val pawnDiff = (wMat.pawns - bMat.pawns).abs
          val totalPassers = passedPawns(board, Color.White).size + passedPawns(board, Color.Black).size
          val advancedPasserExists =
            passedPawns(board, Color.White).exists(relativeRank(_, Color.White) >= 5) ||
              passedPawns(board, Color.Black).exists(relativeRank(_, Color.Black) >= 5)
          if wb.isLight == bb.isLight || pawnDiff > 2 || totalPassers > 1 || advancedPasserExists then None
          else
            Some(
              PatternMatch(
                id = "OppositeColoredBishopsDraw",
                outcomeOverride = Some(TheoreticalOutcomeHint.Draw),
                confidenceFloor = 0.80,
                ambiguityPenalty = if core.kingActivityDelta.abs > 3 then 0.07 else 0.02
              )
            )
        case _ => None

  private def detectGoodBishopRookPawnConversion(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    val matUs = material(board, color)
    val matThem = material(board, !color)
    if matUs.bishops < 1 then None
    else if matUs.rooks + matUs.queens + matUs.knights + matThem.rooks + matThem.queens + matThem.knights + matThem.bishops > 0 then None
    else
      val bishopSq = board.byPiece(color, Bishop).squares.headOption
      val pawnOpt = passedPawns(board, color)
        .filter(p => isRookPawn(p) && relativeRank(p, color) >= 6)
        .sortBy(p => -relativeRank(p, color))
        .headOption
      for
        bishop <- bishopSq
        pawn <- pawnOpt
        promo <- promotionSquare(pawn, color)
        ourKing <- board.kingPosOf(color)
        if bishop.isLight == promo.isLight
        if chebyshev(bishop, pawn) <= 2
        if chebyshev(ourKing, pawn) <= 2
      yield
        PatternMatch(
          id = "GoodBishopRookPawnConversion",
          outcomeOverride = Some(TheoreticalOutcomeHint.Win),
          confidenceFloor = 0.80,
          ambiguityPenalty = if core.ruleOfSquare == RuleOfSquareStatus.Fails then 0.05 else 0.0
        )

  private def detectKnightBlockadeRookPawnDraw(
      board: Board,
      color: Color,
      core: EndgameFeature
  ): Option[PatternMatch] =
    List((color, !color), (!color, color)).collectFirst(Function.unlift { (defender, attacker) =>
      val matA = material(board, attacker)
      val matD = material(board, defender)
      if matD.knights < 1 then None
      else if matA.rooks + matA.queens + matA.knights + matA.bishops + matD.rooks + matD.queens + matD.bishops > 0 then None
      else
        val pawnOpt = passedPawns(board, attacker)
          .filter(p => isRookPawn(p) && relativeRank(p, attacker) >= 5)
          .sortBy(p => -relativeRank(p, attacker))
          .headOption
        for
          pawn <- pawnOpt
          promo <- promotionSquare(pawn, attacker)
          dKing <- board.kingPosOf(defender)
          if chebyshev(dKing, promo) <= 2
          if board.byPiece(defender, Knight).squares.exists { n =>
            n.knightAttacks.contains(promo) || chebyshev(n, promo) <= 1
          }
        yield
          PatternMatch(
            id = "KnightBlockadeRookPawnDraw",
            outcomeOverride = Some(TheoreticalOutcomeHint.Draw),
            confidenceFloor = 0.72,
            ambiguityPenalty = if core.kingActivityDelta > 2 then 0.08 else 0.02
          )
    })

  private final case class MaterialCounts(
      pawns: Int,
      knights: Int,
      bishops: Int,
      rooks: Int,
      queens: Int
  )

  private def material(board: Board, color: Color): MaterialCounts =
    MaterialCounts(
      pawns = board.byPiece(color, Pawn).count,
      knights = board.byPiece(color, Knight).count,
      bishops = board.byPiece(color, Bishop).count,
      rooks = board.byPiece(color, Rook).count,
      queens = board.byPiece(color, Queen).count
    )

  private def hasOnlyKingsAndPawns(board: Board): Boolean =
    board.queens.count == 0 && board.rooks.count == 0 && board.bishops.count == 0 && board.knights.count == 0

  private def isRookPawn(square: Square): Boolean =
    square.file == File.A || square.file == File.H

  private def isFlankPawn(square: Square): Boolean =
    square.file == File.A || square.file == File.B || square.file == File.G || square.file == File.H

  private def isShortSideCorner(defenderKing: Square, pawn: Square, attacker: Color): Boolean =
    val targetRank = if attacker.white then Rank.Eighth else Rank.First
    val targetFile =
      if pawn.file.value <= File.B.value then File.A
      else if pawn.file.value >= File.G.value then File.H
      else pawn.file
    defenderKing.rank == targetRank && defenderKing.file == targetFile

  private def connectedPassedPairs(board: Board, color: Color): List[(Square, Square)] =
    val passers = passedPawns(board, color).sortBy(_.file.value)
    passers
      .combinations(2)
      .collect {
        case List(a, b) if fileDistance(a.file, b.file) == 1 => (a, b)
      }
      .toList

  private def passedPawns(board: Board, color: Color): List[Square] =
    board.byPiece(color, Pawn).squares.filter(isPassedPawn(board, _, color))

  private def isPassedPawn(board: Board, pawnSq: Square, color: Color): Boolean =
    val oppPawnsByFile = board.byPiece(!color, Pawn).squares.groupBy(_.file)
    val fileValue = pawnSq.file.value
    val filesToCheck = List(fileValue - 1, fileValue, fileValue + 1).filter(idx => idx >= 0 && idx <= 7)
    filesToCheck.forall { idx =>
      File.all.lift(idx).forall { f =>
        oppPawnsByFile.get(f).forall { pawns =>
          pawns.forall { oppPawn =>
            if color.white then oppPawn.rank.value <= pawnSq.rank.value
            else oppPawn.rank.value >= pawnSq.rank.value
          }
        }
      }
    }

  private def relativeRank(square: Square, color: Color): Int =
    if color.white then square.rank.value + 1 else 8 - square.rank.value

  private def promotionSquare(pawnSq: Square, color: Color): Option[Square] =
    val promoRank = if color.white then 7 else 0
    Square.at(pawnSq.file.value, promoRank)

  private def isRookBehindPawn(board: Board, color: Color, pawnSq: Square): Boolean =
    board.byPiece(color, Rook).squares.exists { rookSq =>
      rookSq.file == pawnSq.file &&
      (if color.white then rookSq.rank.value < pawnSq.rank.value else rookSq.rank.value > pawnSq.rank.value)
    }

  private def chebyshev(a: Square, b: Square): Int =
    math.max((a.file.value - b.file.value).abs, (a.rank.value - b.rank.value).abs)

  private def fileDistance(a: File, b: File): Int =
    (a.value - b.value).abs

  private def clamp01(v: Double): Double =
    math.max(0.0, math.min(1.0, v))
