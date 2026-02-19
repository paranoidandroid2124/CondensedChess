package lila.llm.analysis.L3

import chess.{Square, Role}

/**
 * Main container for all Phase 1 classification results.
 * Each field contains both the classification AND its evidence.
 */
case class PositionClassification(
  nature: NatureResult,
  criticality: CriticalityResult,
  choiceTopology: ChoiceTopologyResult,
  gamePhase: GamePhaseResult,
  simplifyBias: SimplifyBiasResult,
  drawBias: DrawBiasResult,
  riskProfile: RiskProfileResult,
  taskMode: TaskModeResult
)
// 1. Nature (Static/Dynamic/Chaos)

enum NatureType:
  case Static, Dynamic, Chaos

case class NatureResult(
  natureType: NatureType,
  // Evidence
  tensionScore: Int,        // Number of pawn tensions
  openFilesCount: Int,      // Open files count
  mobilityDiff: Int,        // Mobility difference (white - black)
  lockedCenter: Boolean     // Center pawns locked
):
  def isStatic: Boolean = natureType == NatureType.Static
  def isDynamic: Boolean = natureType == NatureType.Dynamic
  def isChaos: Boolean = natureType == NatureType.Chaos
// 2. Criticality (Normal/Critical/Forced)

enum CriticalityType:
  case Normal, CriticalMoment, ForcedSequence

case class CriticalityResult(
  criticalityType: CriticalityType,
  // Evidence
  evalDeltaCp: Int,             // Eval delta from best to played
  mateDistance: Option[Int],    // Mate distance if applicable
  forcingMovesInPv: Int         // Count of checks/captures in PV
):
  def isNormal: Boolean = criticalityType == CriticalityType.Normal
  def isCritical: Boolean = criticalityType == CriticalityType.CriticalMoment
  def isForced: Boolean = criticalityType == CriticalityType.ForcedSequence
// 3. Choice Topology (OnlyMove/NarrowChoice/StyleChoice)

enum ChoiceTopologyType:
  case OnlyMove, NarrowChoice, StyleChoice

case class ChoiceTopologyResult(
  topologyType: ChoiceTopologyType,
  // Evidence
  pv1Eval: Int,
  pv2Eval: Int,
  pv3Eval: Option[Int],
  gapPv1ToPv2: Int,             // Gap between 1st and 2nd best
  spreadTop3: Int,              // Spread among top 3
  pv2FailureMode: Option[String] // Why PV2 fails (if OnlyMove)
):
  def isOnlyMove: Boolean = topologyType == ChoiceTopologyType.OnlyMove
  def isNarrowChoice: Boolean = topologyType == ChoiceTopologyType.NarrowChoice
  def isStyleChoice: Boolean = topologyType == ChoiceTopologyType.StyleChoice
// 4. Game Phase (Opening/Middlegame/Endgame)

enum GamePhaseType:
  case Opening, Middlegame, Endgame

case class GamePhaseResult(
  phaseType: GamePhaseType,
  // Evidence
  totalMaterial: Int,           // Total material on board
  queensOnBoard: Boolean,       // Whether queens are present
  minorPiecesCount: Int         // Total minor pieces
):
  def isOpening: Boolean = phaseType == GamePhaseType.Opening
  def isMiddlegame: Boolean = phaseType == GamePhaseType.Middlegame
  def isEndgame: Boolean = phaseType == GamePhaseType.Endgame
// 5. Simplify Bias (Window for simplification)

case class SimplifyBiasResult(
  isSimplificationWindow: Boolean,
  // Evidence
  evalAdvantage: Int,           // Current eval advantage
  isEndgameNear: Boolean,       // Phase is endgame or near
  exchangeAvailable: Boolean    // Queen/Rook exchange possible
):
  def shouldSimplify: Boolean = isSimplificationWindow
// 6. Draw Bias (Drawish tendency)

case class DrawBiasResult(
  isDrawish: Boolean,
  // Evidence
  materialSymmetry: Boolean,    // Material is roughly equal
  oppositeColorBishops: Boolean,// Opposite color bishops
  fortressLikely: Boolean,      // Structural fortress possible
  insufficientMaterial: Boolean // Insufficient mating material
):
  def tendsToDraw: Boolean = isDrawish
// 7. Risk Profile (Volatility proxy)

enum RiskLevel:
  case Low, Medium, High

case class RiskProfileResult(
  riskLevel: RiskLevel,
  // Evidence
  evalVolatility: Int,          // Eval swing across depths
  tacticalMotifsCount: Int,     // Number of tactical motifs
  kingExposureSum: Int          // Sum of both kings' exposure
):
  def isLowRisk: Boolean = riskLevel == RiskLevel.Low
  def isMediumRisk: Boolean = riskLevel == RiskLevel.Medium
  def isHighRisk: Boolean = riskLevel == RiskLevel.High
// 8. Task Mode (Narrative routing)

enum TaskModeType:
  case ExplainPlan      // Long-term strategic explanation
  case ExplainTactics   // Tactical sequence explanation
  case ExplainDefense   // Defensive necessity explanation
  case ExplainConvert   // Winning conversion explanation

case class TaskModeResult(
  taskMode: TaskModeType,
  // Evidence
  primaryDriver: String         // Main reason for this mode
):
  def isPlanMode: Boolean = taskMode == TaskModeType.ExplainPlan
  def isTacticsMode: Boolean = taskMode == TaskModeType.ExplainTactics
  def isDefenseMode: Boolean = taskMode == TaskModeType.ExplainDefense
  def isConvertMode: Boolean = taskMode == TaskModeType.ExplainConvert

/**
 * Represents a single PV line from engine analysis.
 * Used as input to PositionClassifier.
 */
case class PvLine(
  moves: List[String],  // UCI moves in the line
  evalCp: Int,          // Centipawn evaluation
  mate: Option[Int],    // Mate distance (positive = winning)
  depth: Int            // Search depth
):
  def score: Int = mate match
    case Some(m) if m > 0 => 10000 - m
    case Some(m) if m < 0 => -10000 - m
    case _ => evalCp

/**
 * Type of threat detected.
 */
enum ThreatKind:
  case Mate        // Checkmate threat
  case Material    // Material loss threat (>= 300cp)
  case Positional  // Positional disadvantage (< 300cp)

/**
 * Urgency level for defense.
 */
enum ThreatSeverity:
  case Urgent     // Must defend immediately (mate or >= 800cp)
  case Important  // Should defend (>= 300cp)  
  case Low        // Can consider ignoring (< 300cp)

/**
 * Single threat detected in the position.
 */
case class Threat(
  kind: ThreatKind,
  lossIfIgnoredCp: Int,         // Estimated loss if ignored (centipawns)
  turnsToImpact: Int,           // Moves until threat materializes (1 = immediate)
  motifs: List[String],         // Related L2 motif names
  attackSquares: List[String],  // Squares being attacked
  targetPieces: List[String],   // Pieces under threat
  bestDefense: Option[String],  // Best defensive move (SAN)
  defenseCount: Int             // Number of adequate defenses
):
  def isImmediate: Boolean = turnsToImpact <= 2
  def isStrategic: Boolean = turnsToImpact >= 3
  def severity: ThreatSeverity =
    if lossIfIgnoredCp >= 800 || kind == ThreatKind.Mate then ThreatSeverity.Urgent
    else if lossIfIgnoredCp >= 300 then ThreatSeverity.Important
    else ThreatSeverity.Low

/**
 * Assessment of defensive options.
 */
case class DefenseAssessment(
  necessity: ThreatSeverity,
  onlyDefense: Option[String],       // SAN if only one adequate defense exists
  alternatives: List[String],        // Alternative defensive moves
  counterIsBetter: Boolean,          // Counter-attack is superior to defense
  prophylaxisNeeded: Boolean,        // Prophylactic defense needed
  resourceCoverageScore: Int,        // Defensive resource score (0-100)
  notes: String                      // Explanation for L4
)

/**
 * Complete threat analysis result for a position.
 * Contains both individual threats and aggregate flags.
 */
case class ThreatAnalysis(
  // Individual threats
  threats: List[Threat],
  
  // Defense assessment
  defense: DefenseAssessment,
  
  // Aggregate flags (for quick routing)
  threatSeverity: ThreatSeverity,
  immediateThreat: Boolean,          // Any threat with turnsToImpact <= 2
  strategicThreat: Boolean,          // Any threat with turnsToImpact >= 3  
  threatIgnorable: Boolean,          // Safe to ignore all threats
  defenseRequired: Boolean,          // Must defend
  counterThreatBetter: Boolean,      // Counter-attack preferred
  prophylaxisNeeded: Boolean,        // Prophylaxis needed
  resourceAvailable: Boolean,        // Adequate defensive resources
  
  // Evidence for L4 narrative
  maxLossIfIgnored: Int,             // Highest lossIfIgnoredCp among threats
  primaryDriver: String,             // "mate_threat" | "material_threat" | "positional_threat" | "no_threat"
  insufficientData: Boolean          // True if MultiPV was inadequate
):
  def hasThreat: Boolean = threats.nonEmpty
  def threatCount: Int = threats.size

/**
 * Tension resolution policy recommendation.
 * Concept 10: tensionPolicy
 */
enum TensionPolicy:
  case Maintain   // Keep tension, don't resolve yet
  case Release    // Resolve tension now (capture/advance)
  case Ignore     // No significant tension to manage

/**
 * Passed pawn advancement urgency.
 * Concept 5: passedPawnUrgency
 */
enum PassedPawnUrgency:
  case Critical   // Must push NOW (rank 6-7, near promotion)
  case Important  // Should prioritize pushing
  case Background // Strategic asset, can wait
  case Blocked    // Cannot advance without support

/**
 * Complete Phase 3 analysis result.
 * Contains all 10 concepts for Break & Pawn Play.
 */
case class PawnPlayAnalysis(
  // === Concept 1-3: Break Analysis ===
  pawnBreakReady: Boolean,              // Concept 1: A pawn break is immediately executable
  breakFile: Option[String],            // Concept 2: File where primary break exists
  breakImpact: Int,                     // Concept 3: Estimated cp impact of break (0-500)
  
  // === Concept 4: Tension Resolution ===
  advanceOrCapture: Boolean,            // Concept 4: Pawn tension MUST be resolved this move
  
  // === Concept 5-7: Passed Pawn Handling ===
  passedPawnUrgency: PassedPawnUrgency, // Concept 5: How urgent is passed pawn push
  passerBlockade: Boolean,              // Concept 6: Passed pawn is blockaded
  blockadeSquare: Option[Square],       // NEW: Specific square of blockade
  blockadeRole: Option[Role],           // NEW: Role of blockading piece
  pusherSupport: Boolean,               // Concept 7: Rook/King support for promotion
  
  // === Concept 8-9: Strategic Posture ===
  minorityAttack: Boolean,              // Concept 8: Queenside minority attack available
  counterBreak: Boolean,                // Concept 9: Opponent has ready counter-break
  
  // === Concept 10: Tension Policy ===
  tensionPolicy: TensionPolicy,         // Concept 10: Maintain/Release/Ignore recommendation
  
  // === Evidence for L4 Narrative ===
  tensionSquares: List[String],         // Squares with pawn tension (e.g., "d4", "e5")
  primaryDriver: String,                // "break_ready" | "passed_pawn" | "defensive" | "quiet"
  notes: String                         // Additional context for L4
):
  def hasBreakOpportunity: Boolean = pawnBreakReady
  def needsPassedPawnAction: Boolean = passedPawnUrgency == PassedPawnUrgency.Critical
  def hasTensionToResolve: Boolean = tensionPolicy == TensionPolicy.Release

