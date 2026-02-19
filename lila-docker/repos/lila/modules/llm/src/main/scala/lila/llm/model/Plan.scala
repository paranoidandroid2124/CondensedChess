package lila.llm.model

import chess.Color

case class PlanScoringResult(
    topPlans: List[PlanMatch],
    confidence: Double,
    phase: String, // Opening, Middlegame, Endgame
    compatibilityEvents: List[CompatibilityEvent] = Nil // A4 Trace
)

/**
 * A4: Compatibility event tracking for debugging suppressed/adjusted plans.
 */
case class CompatibilityEvent(
  planName: String,
  originalScore: Double,
  finalScore: Double,
  delta: Double,
  reason: String,          // e.g. "conflict: attack ⟂ simplification"
  eventType: String        // "downweight" | "removed" | "boosted"
)

case class PlanMatch(
    plan: Plan,
    score: Double,
    evidence: List[EvidenceAtom],
    supports: List[String] = Nil,       // Positive factors favoring the plan
    blockers: List[String] = Nil,       // Negative factors or obstacles
    missingPrereqs: List[String] = Nil  // Things needed to make it viable
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
  def color: Color

enum PlanCategory:
  case Attack, Positional, Structural, Endgame, Defensive, Transition

object Plan:

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

  case class Simplification(color: Color) extends Plan:
    val id = PlanId.Simplification
    val name = "Simplification into Endgame"
    val category = PlanCategory.Transition

  case class QueenTrade(color: Color) extends Plan:
    val id = PlanId.QueenTrade
    val name = "Trading Queens"
    val category = PlanCategory.Transition

  case class Blockade(color: Color, square: String) extends Plan:
    val id = PlanId.Blockade
    val name = s"Blockade on $square"
    val category = PlanCategory.Structural

  case class PawnChain(color: Color) extends Plan:
    val id = PlanId.PawnChain
    val name = "Pawn Chain Maintenance"
    val category = PlanCategory.Structural

  case class MinorityAttack(color: Color) extends Plan:
    val id = PlanId.MinorityAttack
    val name = "Minority Attack"
    val category = PlanCategory.Structural

  case class SpaceAdvantage(color: Color) extends Plan:
    val id = PlanId.SpaceAdvantage
    val name = "Exploiting Space Advantage"
    val category = PlanCategory.Positional

  case class Zugzwang(color: Color) extends Plan:
    val id = PlanId.Zugzwang
    val name = "Inducing Zugzwang"
    val category = PlanCategory.Endgame

  case class Sacrifice(color: Color, piece: String) extends Plan:
    val id = PlanId.Sacrifice
    val name = s"$piece Sacrifice"
    val category = PlanCategory.Attack

enum PlanId:
  // Attack
  case KingsideAttack, QueensideAttack, CentralBreakthrough, PawnStorm, PerpetualCheck, DirectMate, Sacrifice
  // Positional
  case CentralControl, PieceActivation, MinorPieceManeuver, RookActivation, FileControl, SpaceAdvantage
  // Structural
  case PassedPawnCreation, PassedPawnPush, WeakPawnAttack, PawnBreakPreparation, Blockade, PawnChain, MinorityAttack
  // Endgame
  case KingActivation, Opposition, Triangulation, Promotion, Fortress, Zugzwang
  // Defensive
  case DefensiveConsolidation, Prophylaxis, Exchange, Counterplay
  // Transition
  case Simplification, QueenTrade
