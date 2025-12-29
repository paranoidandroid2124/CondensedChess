package lila.llm.analysis

import chess.*
import lila.llm.model.*
import lila.llm.model.Motif.*

case class IntegratedContext(
    eval: Double,
    phase: String, // "opening", "middlegame", "endgame"
    openingName: Option[String] = None
)

/**
 * Matches detected Motifs to high-level strategic Plans.
 * Uses scoring with phase gating and motif category weights.
 */
object PlanMatcher:

  def matchPlans(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): PlanScoringResult =
    val allScorers = List(
      // Attack plans
      scoreKingsideAttack(motifs, ctx, side),
      scoreQueensideAttack(motifs, ctx, side),
      scorePawnStorm(motifs, ctx, side),
      scorePerpetualCheck(motifs, ctx, side),
      scoreDirectMate(motifs, ctx, side),
      // Positional plans
      scoreCentralControl(motifs, ctx, side),
      scorePieceActivation(motifs, ctx, side),
      scoreRookActivation(motifs, ctx, side),
      // Structural plans
      scorePassedPawnPush(motifs, ctx, side),
      // Endgame plans
      scoreKingActivation(motifs, ctx, side),
      scorePromotion(motifs, ctx, side),
      // Defensive plans
      scoreDefensiveConsolidation(motifs, ctx, side)
    )

    val plans = allScorers.flatten
    val sortedPlans = plans.sortBy(-_.score)
    val confidence = sortedPlans.headOption.map(_.score).getOrElse(0.0)

    PlanScoringResult(
      topPlans = sortedPlans.take(2),
      confidence = confidence,
      phase = ctx.phase
    )

  // ============================================================
  // ATTACK PLAN SCORING
  // ============================================================

  private def scoreKingsideAttack(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val relevantMotifs = motifs.filter:
      case m: PawnAdvance   => m.file.isKingside
      case m: RookLift      => m.file.isKingside
      case m: Motif.Check   => m.targetSquare.file.isKingside
      case m: PieceLift     => true
      case m: Fork          => m.targets.contains(King)
      case _                => false

    if (relevantMotifs.isEmpty) None
    else
      val baseScore = relevantMotifs.size * 0.2
      val phaseMultiplier = ctx.phase match
        case "opening"    => 0.7
        case "middlegame" => 1.4
        case "endgame"    => 0.5
        case _            => 1.0

      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.2, s"Kingside pressure: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.KingsideAttack(side), baseScore * phaseMultiplier, evidence))

  private def scoreQueensideAttack(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val relevantMotifs = motifs.filter:
      case m: PawnAdvance => m.file.isQueenside
      case m: PawnBreak   => m.targetFile.isQueenside
      case m: RookLift    => m.file.isQueenside
      case _              => false

    if (relevantMotifs.isEmpty) None
    else
      val baseScore = relevantMotifs.size * 0.25
      val phaseMultiplier = ctx.phase match
        case "middlegame" => 1.2
        case "endgame"    => 0.8
        case _            => 1.0

      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.25, s"Queenside expansion: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.QueensideAttack(side), baseScore * phaseMultiplier, evidence))

  private def scorePawnStorm(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val pawnAdvances = motifs.collect { case m: PawnAdvance => m }
    val kingsidePawns = pawnAdvances.filter(_.file.isKingside)
    val queensidePawns = pawnAdvances.filter(_.file.isQueenside)

    val (stormSide, stormPawns) = 
      if (kingsidePawns.size >= 2) ("kingside", kingsidePawns)
      else if (queensidePawns.size >= 2) ("queenside", queensidePawns)
      else return None

    // Only in middlegame/endgame
    if (ctx.phase == "opening") return None

    val evidence = stormPawns.take(3).map: m =>
      EvidenceAtom(m, 0.3, s"Pawn storm: ${m.move.getOrElse("")}")

    Some(PlanMatch(Plan.PawnStorm(side, stormSide), stormPawns.size * 0.3, evidence))

  private def scorePerpetualCheck(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val checks = motifs.collect { case m: Motif.Check => m }
    // Need 3+ checks and eval near 0 to suggest perpetual
    if (checks.size >= 3 && ctx.eval.abs < 0.5)
      val evidence = checks.take(3).map: m =>
        EvidenceAtom(m, 1.0, s"Repetitive check: ${m.move.getOrElse("")}")
      Some(PlanMatch(Plan.PerpetualCheck(side), 1.0, evidence))
    else None

  private def scoreDirectMate(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val checks = motifs.collect { case m: Motif.Check => m }
    // If eval is very high and multiple checks, likely mating sequence
    if (checks.nonEmpty && ctx.eval > 5.0)
      val evidence = checks.take(3).map: m =>
        EvidenceAtom(m, 1.0, s"Mating attack: ${m.move.getOrElse("")}")
      Some(PlanMatch(Plan.DirectMate(side), 1.5, evidence))
    else None

  // ============================================================
  // POSITIONAL PLAN SCORING
  // ============================================================

  private def scoreCentralControl(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val relevantMotifs = motifs.filter:
      case m: PawnAdvance    => m.file.isCentral
      case m: Centralization => true
      case m: Outpost        => true
      case _                 => false

    if (relevantMotifs.isEmpty) None
    else
      val baseScore = relevantMotifs.size * 0.3
      val phaseMultiplier = ctx.phase match
        case "opening" => 1.5
        case _         => 0.8

      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.3, s"Central influence: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.CentralControl(side), baseScore * phaseMultiplier, evidence))

  private def scorePieceActivation(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val relevantMotifs = motifs.filter:
      case m: Fianchetto     => true
      case m: Outpost        => true
      case m: Centralization => true
      case m: PieceLift      => true
      case _                 => false

    if (relevantMotifs.size < 2) None
    else
      val baseScore = relevantMotifs.size * 0.25
      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.25, s"Piece development: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.PieceActivation(side), baseScore, evidence))

  private def scoreRookActivation(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val rookMotifs = motifs.collect { 
      case m: RookLift => m 
      case m: DoubledPieces if m.role == Rook => m
    }

    if (rookMotifs.isEmpty) None
    else
      val evidence = rookMotifs.take(2).map: m =>
        EvidenceAtom(m, 0.4, s"Rook activation: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.RookActivation(side), rookMotifs.size * 0.4, evidence))

  // ============================================================
  // STRUCTURAL PLAN SCORING
  // ============================================================

  private def scorePassedPawnPush(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val pawnMotifs = motifs.collect { 
      case m: PawnAdvance if m.relativeTo >= 5 => m
      case m: PassedPawnPush => m
      case m: PawnPromotion => m
    }

    if (ctx.phase != "endgame" || pawnMotifs.isEmpty) None
    else
      val evidence = pawnMotifs.take(2).map: m =>
        EvidenceAtom(m, 0.5, s"Passed pawn advance: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.PassedPawnPush(side), 0.8 + pawnMotifs.size * 0.1, evidence))

  // ============================================================
  // ENDGAME PLAN SCORING
  // ============================================================

  private def scoreKingActivation(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val kingMoves = motifs.collect:
      case m: KingStep if m.stepType == KingStepType.Activation => m

    if (ctx.phase == "endgame" && kingMoves.nonEmpty)
      val evidence = kingMoves.take(2).map: m =>
        EvidenceAtom(m, 0.4, s"King march: ${m.move.getOrElse("")}")
      Some(PlanMatch(Plan.KingActivation(side), 0.7 + kingMoves.size * 0.1, evidence))
    else None

  private def scorePromotion(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val promoMotifs = motifs.collect { case m: PawnPromotion => m }

    if (promoMotifs.nonEmpty)
      val evidence = promoMotifs.map: m =>
        EvidenceAtom(m, 1.0, s"Promotion: ${m.move.getOrElse("")}")
      Some(PlanMatch(Plan.Promotion(side), 1.2, evidence))
    else None

  // ============================================================
  // DEFENSIVE PLAN SCORING
  // ============================================================

  private def scoreDefensiveConsolidation(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val defensiveMotifs = motifs.filter:
      case m: KingStep if m.stepType == KingStepType.ToCorner => true
      case m: KingStep if m.stepType == KingStepType.Evasion => true
      case m: Castling => true
      case m: Capture if m.captureType == CaptureType.Exchange => true
      case _ => false

    if (defensiveMotifs.size < 2) None
    else
      val evidence = defensiveMotifs.take(2).map: m =>
        EvidenceAtom(m, 0.3, s"Defensive move: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.DefensiveConsolidation(side), defensiveMotifs.size * 0.3, evidence))

  // ============================================================
  // FILE CLASSIFICATION HELPERS
  // ============================================================

  extension (f: File)
    def isKingside: Boolean = f == File.F || f == File.G || f == File.H
    def isQueenside: Boolean = f == File.A || f == File.B || f == File.C
    def isCentral: Boolean = f == File.D || f == File.E
