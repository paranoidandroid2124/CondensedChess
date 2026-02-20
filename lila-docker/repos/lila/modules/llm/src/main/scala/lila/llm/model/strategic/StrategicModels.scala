package lila.llm.model.strategic

import chess.{ Color, Role, Square }

case class PreventedPlan(
    planId: String, // e.g. "StopCheck", "PreventFork", "DenyBreak"
    deniedSquares: List[Square], // Squares opponent stopped controlling/occupying
    breakNeutralized: Option[String], // e.g. "f5"
    mobilityDelta: Int, // Change in opponent's safe mobility
    counterplayScoreDrop: Int, // Drop in opponent's positional eval
    preventedThreatType: Option[String] = None // NEW: "Check", "Fork", "Mate", "Material"
)

case class PieceActivity(
    piece: Role,
    square: Square,
    mobilityScore: Double, // 0.0 (Trapped) to 1.0 (Optimal)
    isTrapped: Boolean, // No safe moves
    isBadBishop: Boolean, // Blocked by own pawns
    keyRoutes: List[Square], // Path to relevant area (e.g. Qh5 -> h7)
    coordinationLinks: List[Square] // Squares defended/attacked in tandem with friendly pieces
)

case class WeakComplex(
    color: Color, // White/Black squares
    squares: List[Square], // The weak squares e.g. f3, g2, h3
    isOutpost: Boolean, // Can an enemy piece settle here safely?
    cause: String // "Missing fianchetto bishop", "Over-pushed pawns"
)

case class Compensation(
    investedMaterial: Int, // CP value sacrificed
    returnVector: Map[String, Double], // { "Time" -> 0.8, "Space" -> 0.5, "Attack" -> 0.9 }
    expiryPly: Option[Int], // How long does this compensation last?
    conversionPlan: String // "Mating attack" or "Perpetual"
)

// Endgame Features
case class EndgameFeature(
    hasOpposition: Boolean,
    isZugzwang: Boolean,
    keySquaresControlled: List[Square]
)

// Output of PracticalityScorer
case class PracticalAssessment(
    engineScore: Int, // Standard CP
    practicalScore: Double, // Adjusted CP
    biasFactors: List[BiasFactor],
    verdict: String // "Unpleasant Draw", "White is Fighting", etc.
)

case class BiasFactor(
    factor: String, // "Mobility Diff", "Forgiveness Index"
    description: String,
    weight: Double
)

enum GamePhase:
  case Opening, Middlegame, Endgame

// (VariationLine and VariationTag moved to Variation.scala)

// StructureTag and PlanTag have been removed as they were obsolete dead code.

enum PositionalTag:
  case Outpost(square: Square, color: Color)
  case OpenFile(file: chess.File, color: Color)
  case WeakSquare(square: Square, color: Color)
  case LoosePiece(square: Square, role: Role, color: Color)
  case WeakBackRank(color: Color)
  case BishopPairAdvantage(color: Color)
  case BadBishop(color: Color)
  case GoodBishop(color: Color)
  // New additions
  case RookOnSeventh(color: Color)
  case StrongKnight(square: Square, color: Color)
  case SpaceAdvantage(color: Color)
  case OppositeColorBishops
  case KingStuckCenter(color: Color)
  case ConnectedRooks(color: Color)
  case DoubledRooks(file: chess.File, color: Color)
  case ColorComplexWeakness(color: Color, squareColor: String, squares: List[Square])  // "light" or "dark"
  case PawnMajority(color: Color, flank: String, count: Int)  // "queenside" or "kingside"
  case MinorityAttack(color: Color, flank: String)
  // case QueenActivity(color: Color)
  // case QueenManeuver(color: Color)
  case MateNet(color: Color)
  // case PerpetualCheck(color: Color)
  case RemovingTheDefender(target: Role, color: Color)
  case Initiative(color: Color)

case class Hypothesis(move: String, candidateType: String, rationale: String)

case class PlanContinuity(
  planName: String,
  consecutivePlies: Int,
  startingPly: Int
)

case class CounterfactualMatch(
    userMove: String,
    bestMove: String,
    cpLoss: Int,
    missedMotifs: List[lila.llm.model.Motif],
    userMoveMotifs: List[lila.llm.model.Motif],
    severity: String,
    userLine: lila.llm.model.strategic.VariationLine 
)
case class MoveIntent(
    immediate: String,           // What the move itself does (e.g., "Development")
    downstream: Option[String]   // Tactics that emerge later in PV (e.g., "Tactical shot (fork)")
)

case class AnalyzedCandidate(
    move: String,
    score: Int, // Normalized CP
    motifs: List[lila.llm.model.Motif],
    prophylaxisResults: List[PreventedPlan],
    futureContext: String, // Kept for backward compatibility
    moveIntent: MoveIntent = MoveIntent("Positional maneuvering", None), // Default for backward compat
    facts: List[lila.llm.model.Fact] = Nil, // Verified Facts
    line: VariationLine
)
