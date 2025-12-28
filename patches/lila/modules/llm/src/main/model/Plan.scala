package lila.llm.model

import chess.Color

case class PlanScoringResult(
    topPlans: List[PlanMatch],
    confidence: Double,
    phase: String // Opening, Middlegame, Endgame
)

case class PlanMatch(
    plan: Plan,
    score: Double,
    evidence: List[EvidenceAtom]
)

case class EvidenceAtom(
    motif: Motif,
    weight: Double,
    description: String
)

/**
 * High-level strategic plan inferred from motif patterns.
 * 
 * Categories:
 * - Attack Plans: Direct assault on opponent's king or weaknesses
 * - Positional Plans: Improving piece placement and control
 * - Structural Plans: Exploiting or creating pawn structure advantages
 * - Endgame Plans: Specific endgame techniques
 * - Defensive Plans: Consolidation and prophylaxis
 */
sealed trait Plan:
  def id: PlanId
  def name: String
  def category: PlanCategory

enum PlanCategory:
  case Attack, Positional, Structural, Endgame, Defensive

object Plan:

  // ============================================================
  // ATTACK PLANS
  // ============================================================

  case class KingsideAttack(color: Color) extends Plan:
    val id = PlanId.KingsideAttack
    val name = "Kingside Attack"
    val category = PlanCategory.Attack

  case class QueensideAttack(color: Color) extends Plan:
    val id = PlanId.QueensideAttack
    val name = "Queenside Attack"
    val category = PlanCategory.Attack

  case class CentralBreakthrough(color: Color) extends Plan:
    val id = PlanId.CentralBreakthrough
    val name = "Central Breakthrough"
    val category = PlanCategory.Attack

  case class PawnStorm(color: Color, side: String) extends Plan: // side: "kingside" or "queenside"
    val id = PlanId.PawnStorm
    val name = s"${side.capitalize} Pawn Storm"
    val category = PlanCategory.Attack

  case class PerpetualCheck(color: Color) extends Plan:
    val id = PlanId.PerpetualCheck
    val name = "Perpetual Check"
    val category = PlanCategory.Attack

  case class DirectMate(color: Color) extends Plan:
    val id = PlanId.DirectMate
    val name = "Mating Attack"
    val category = PlanCategory.Attack

  // ============================================================
  // POSITIONAL PLANS
  // ============================================================

  case class CentralControl(color: Color) extends Plan:
    val id = PlanId.CentralControl
    val name = "Central Control"
    val category = PlanCategory.Positional

  case class PieceActivation(color: Color) extends Plan:
    val id = PlanId.PieceActivation
    val name = "Piece Activation"
    val category = PlanCategory.Positional

  case class MinorPieceManeuver(color: Color, target: String) extends Plan:
    val id = PlanId.MinorPieceManeuver
    val name = s"Minor Piece Maneuver to $target"
    val category = PlanCategory.Positional

  case class RookActivation(color: Color) extends Plan:
    val id = PlanId.RookActivation
    val name = "Rook Activation"
    val category = PlanCategory.Positional

  case class FileControl(color: Color, fileType: String) extends Plan: // fileType: "open", "semi-open", "c-file" etc
    val id = PlanId.FileControl
    val name = s"$fileType File Control"
    val category = PlanCategory.Positional

  // ============================================================
  // STRUCTURAL PLANS
  // ============================================================

  case class PassedPawnCreation(color: Color) extends Plan:
    val id = PlanId.PassedPawnCreation
    val name = "Creating a Passed Pawn"
    val category = PlanCategory.Structural

  case class PassedPawnPush(color: Color) extends Plan:
    val id = PlanId.PassedPawnPush
    val name = "Passed Pawn Advance"
    val category = PlanCategory.Structural

  case class WeakPawnAttack(color: Color, pawnType: String) extends Plan: // pawnType: "isolated", "backward", "doubled"
    val id = PlanId.WeakPawnAttack
    val name = s"Attacking $pawnType Pawn"
    val category = PlanCategory.Structural

  case class PawnBreakPreparation(color: Color, breakMove: String) extends Plan:
    val id = PlanId.PawnBreakPreparation
    val name = s"Preparing $breakMove Break"
    val category = PlanCategory.Structural

  // ============================================================
  // ENDGAME PLANS
  // ============================================================

  case class KingActivation(color: Color) extends Plan:
    val id = PlanId.KingActivation
    val name = "King Activation"
    val category = PlanCategory.Endgame

  case class Opposition(color: Color) extends Plan:
    val id = PlanId.Opposition
    val name = "Gaining Opposition"
    val category = PlanCategory.Endgame

  case class Triangulation(color: Color) extends Plan:
    val id = PlanId.Triangulation
    val name = "Triangulation"
    val category = PlanCategory.Endgame

  case class Promotion(color: Color) extends Plan:
    val id = PlanId.Promotion
    val name = "Pawn Promotion"
    val category = PlanCategory.Endgame

  case class Fortress(color: Color) extends Plan:
    val id = PlanId.Fortress
    val name = "Building a Fortress"
    val category = PlanCategory.Endgame

  // ============================================================
  // DEFENSIVE PLANS
  // ============================================================

  case class DefensiveConsolidation(color: Color) extends Plan:
    val id = PlanId.DefensiveConsolidation
    val name = "Defensive Consolidation"
    val category = PlanCategory.Defensive

  case class Prophylaxis(color: Color, againstWhat: String) extends Plan:
    val id = PlanId.Prophylaxis
    val name = s"Prophylaxis against $againstWhat"
    val category = PlanCategory.Defensive

  case class Exchange(color: Color, purpose: String) extends Plan:
    val id = PlanId.Exchange
    val name = s"Exchanging for $purpose"
    val category = PlanCategory.Defensive

  case class Counterplay(color: Color, where: String) extends Plan:
    val id = PlanId.Counterplay
    val name = s"$where Counterplay"
    val category = PlanCategory.Defensive

enum PlanId:
  // Attack
  case KingsideAttack, QueensideAttack, CentralBreakthrough, PawnStorm, PerpetualCheck, DirectMate
  // Positional
  case CentralControl, PieceActivation, MinorPieceManeuver, RookActivation, FileControl
  // Structural
  case PassedPawnCreation, PassedPawnPush, WeakPawnAttack, PawnBreakPreparation
  // Endgame
  case KingActivation, Opposition, Triangulation, Promotion, Fortress
  // Defensive
  case DefensiveConsolidation, Prophylaxis, Exchange, Counterplay
