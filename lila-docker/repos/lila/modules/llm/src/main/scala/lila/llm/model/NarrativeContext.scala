package lila.llm.model

import chess.Color
import lila.llm.model.authoring.{ AuthorQuestion, QuestionEvidence }
import play.api.libs.json.*

/** Aggregates all analysis layers into a hierarchical structure for LLM prompts. */
case class NarrativeContext(


  fen: String,


  header: ContextHeader,


  ply: Int,
  playedMove: Option[String] = None, // UCI
  playedSan: Option[String] = None,  // SAN (derived from `fen` used for analysis)
  counterfactual: Option[lila.llm.model.strategic.CounterfactualMatch] = None,
  

  summary: NarrativeSummary,
  

  threats: ThreatTable,
  pawnPlay: PawnPlayTable,
  plans: PlanTable,
  planContinuity: Option[lila.llm.model.strategic.PlanContinuity] = None,
  snapshots: List[L1Snapshot] = Nil,
  

  delta: Option[MoveDelta],
  phase: PhaseContext,
  
  // === CANDIDATES ===
  candidates: List[CandidateInfo],

  // === EVIDENCE AUGMENTATION ===
  probeRequests: List[ProbeRequest] = Nil,


  meta: Option[MetaSignals] = None,
  strategicFlow: Option[String] = None, // Transition justification
  
  // === SEMANTIC SECTION ===
  semantic: Option[SemanticSection] = None, // ExtendedAnalysisData semantic fields
  
  // === OPPONENT PLAN ===
  opponentPlan: Option[PlanRow] = None, // Top plan for opponent (side=!toMove)

  // === DECISION RATIONALE ===
  decision: Option[DecisionRationale] = None,

  // === OPENING EVENT ===
  openingEvent: Option[OpeningEvent] = None,
  
  // === OPENING DATA (for internal use, not rendered directly) ===
  openingData: Option[OpeningReference] = None,
  
  // === UPDATED BUDGET (for game-level accumulation) ===
  updatedBudget: OpeningEventBudget = OpeningEventBudget(),

  // === ENGINE EVIDENCE ===
  engineEvidence: Option[lila.llm.model.strategic.EngineEvidence] = None,

  // === AUTHOR QUESTIONS ===
  authorQuestions: List[AuthorQuestion] = Nil,
  authorEvidence: List[QuestionEvidence] = Nil,

  // === VERIFIED FACTS ===
  facts: List[Fact] = Nil,


  deltaAfterMove: Boolean = false
)


case class PhaseContext(
  current: String,            // "Opening", "Middlegame", "Endgame"
  reason: String,             // "Material: 45, Queens present"
  transitionTrigger: Option[String] = None // "Minor piece trade triggered Endgame"
)


case class ContextHeader(
  phase: String,           // "Opening" | "Middlegame" | "Endgame"
  criticality: String,     // "Normal" | "Critical" | "Forced"
  choiceType: String,      // "OnlyMove" | "NarrowChoice" | "StyleChoice"
  riskLevel: String,       // "Low" | "Medium" | "High"
  taskMode: String         // "ExplainPlan" | "ExplainTactics" | "ExplainDefense" | "ExplainConvert"
)


case class NarrativeSummary(
  primaryPlan: String,        // "Kingside Attack (0.85)"
  keyThreat: Option[String],  // "Fork on e5 (must defend)"
  choiceType: String,         // "Style Choice (multiple valid paths)"
  tensionPolicy: String,      // "Maintain (attack preparation)"
  evalDelta: String           // "+0.5 eval, RookLift appeared"
)


case class ThreatTable(
  toUs: List[ThreatRow],
  toThem: List[ThreatRow]
)

case class ThreatRow(
  kind: String,              // ThreatKind: "Mate" | "Material" | "Positional"
  side: String,              // TO US: "US" | TO THEM: "THEM"
  square: Option[String],    // Attack square
  lossIfIgnoredCp: Int,
  turnsToImpact: Int,
  bestDefense: Option[String],
  defenseCount: Int,
  insufficientData: Boolean,
  confidence: ConfidenceLevel = ConfidenceLevel.Engine,
  isTopCandidateDefense: Boolean = false
) {
  def urgencyLabel: String = 
    if (lossIfIgnoredCp >= 800 || kind == "Mate") "URGENT"
    else if (lossIfIgnoredCp >= 300) "IMPORTANT"
    else "LOW"
    
  def toNarrative: String = {
    val defenseNote = bestDefense.map(d => s" - Defend: $d").getOrElse("")
    s"$kind${square.map(s => s"/$s").getOrElse("")}/${lossIfIgnoredCp}cp/${turnsToImpact}moves$defenseNote"
  }
}


case class PawnPlayTable(
  breakReady: Boolean,
  breakFile: Option[String],
  breakImpact: String,        // "High" | "Medium" | "Low"
  tensionPolicy: String,      // "Maintain" | "Release" | "Ignore"
  tensionReason: String,      // Why maintain/release
  passedPawnUrgency: String,  // "Critical" | "Important" | "Background" | "Blocked"
  passerBlockade: Option[String], // "d5 by Nd5" if blockaded
  counterBreak: Boolean,
  primaryDriver: String       // "break_ready" | "passed_pawn" | "defensive" | "quiet" | "tension_critical"
)


case class PlanTable(
  top5: List[PlanRow],
  suppressed: List[SuppressedPlan]
)

case class PlanRow(
  rank: Int,
  name: String,
  score: Double,
  evidence: List[String],      // Top 2 evidence items
  supports: List[String] = Nil,
  blockers: List[String] = Nil,
  missingPrereqs: List[String] = Nil,
  confidence: ConfidenceLevel = ConfidenceLevel.Heuristic,
  isEstablished: Boolean = false
)

case class SuppressedPlan(
  name: String,
  originalScore: Double,
  reason: String,
  isRemoved: Boolean
)


case class L1Snapshot(
  material: String,            // "+2" | "=" | "-1"
  imbalance: Option[String],   // "Bishop pair" | "Exchange down"
  kingSafetyUs: Option[String],    // "Exposed (2 attackers, 1 escape)"
  kingSafetyThem: Option[String],
  mobility: Option[String],    // "Advantage (+3 moves)"
  centerControl: Option[String], // "Dominant"
  openFiles: List[String]      // ["d", "e"]
)


case class MoveDelta(
  evalChange: Int,             // CP change (signed)
  newMotifs: List[String],     // Appeared motifs
  lostMotifs: List[String],    // Disappeared motifs
  structureChange: Option[String], // "d5 pawn became weak"
  openFileCreated: Option[String],
  phaseChange: Option[String]        // "middlegame → endgame"
)


case class CandidateInfo(
  move: String,                // SAN
  uci: Option[String] = None,  // NEW: UCI format for probe result matching
  annotation: String,          // "!" | "?" | ""
  planAlignment: String,       // Immediate intent (e.g., "Development")
  structureGuidance: Option[String] = None, // Internal structure-aware narrative guidance
  alignmentBand: Option[String] = None,     // OnBook | Playable | OffPlan | Unknown
  downstreamTactic: Option[String] = None,
  tacticalAlert: Option[String], // "allows Qb6 response"
  practicalDifficulty: String, // "clean" | "complex"
  whyNot: Option[String],       // Refutation reason if inferior
  tags: List[CandidateTag] = Nil, // Sharp, Solid, etc.
  tacticEvidence: List[String] = Nil,
  probeLines: List[String] = Nil,
  facts: List[Fact] = Nil,
  hypotheses: List[HypothesisCard] = Nil
)


enum HypothesisAxis:
  case Plan
  case Structure
  case Initiative
  case Conversion
  case KingSafety
  case PieceCoordination
  case PawnBreakTiming
  case EndgameTrajectory


enum HypothesisHorizon:
  case Short
  case Medium
  case Long


case class HypothesisCard(
  axis: HypothesisAxis,
  claim: String,
  supportSignals: List[String],
  conflictSignals: List[String],
  confidence: Double,
  horizon: HypothesisHorizon
)


enum ChoiceType:
  case OnlyMove      // PV1-PV2 > 200cp
  case StyleChoice   // PV1-PV2 < 50cp
  case NarrowChoice  // 50-200cp
  case Complex       // Many sharp lines


enum ConfidenceLevel:
  case Engine
  case Probe
  case Heuristic


case class ErrorClassification(
  isTactical: Boolean,         // turnsToImpact <= 2 && loss >= 200
  missedMotifs: List[String],
  errorSummary: String         // "전술(2수 내 300cp)" or "포지셔널"
)


case class DivergenceInfo(
  divergePly: Int,             // Actual ply where lines diverge
  punisherMove: Option[String], // Move that punishes user's choice
  branchPointFen: Option[String]
)


sealed trait TargetRef:
  def label: String

case class TargetSquare(key: String) extends TargetRef:
  val label = key

case class TargetFile(file: String) extends TargetRef:
  val label = s"$file-file"

case class TargetPiece(role: String, square: String) extends TargetRef:
  val label = s"$role on $square"

case class TargetEntry(
  ref: TargetRef,
  reason: String,
  confidence: ConfidenceLevel,
  priority: Int // 1: Urgent (Threat), 2: High (Probe), 3: Normal (Heuristic)
)


case class Targets(
  tactical: List[TargetEntry],  // Immediate attack/defend
  strategic: List[TargetEntry]  // Outposts, open files, blockades, weaknesses
)


case class PlanConcurrency(
  primary: String,
  secondary: Option[String],
  relationship: String         // "↔ synergy" | "⟂ conflict" | "independent"
)

/** Unified meta-signals container. */
case class MetaSignals(
  choiceType: ChoiceType, // from classification.choiceTopology
  targets: Targets, // from threatsToUs/Them
  planConcurrency: PlanConcurrency, // from compatibilityEvents
  divergence: Option[DivergenceInfo] = None, // counterfactual comparison
  errorClass: Option[ErrorClassification] = None, // B2/B6: tactical vs positional
  whyNot: Option[String] = None // ProbeResult-based refutation
)


case class WeakComplexInfo(
  owner: String,                // "White" | "Black" (side that owns the weakness)
  squareColor: String,          // "light" | "dark"
  squares: List[String],        // ["f3", "g2", "h3"]
  isOutpost: Boolean,
  cause: String                 // "Missing fianchetto bishop"
)


case class PieceActivityInfo(
  piece: String,                // "Knight"
  square: String,               // "c3"
  mobilityScore: Double,        // 0.0 - 1.0
  isTrapped: Boolean,
  isBadBishop: Boolean,
  keyRoutes: List[String],      // ["e4", "d5"]
  coordinationLinks: List[String]
)


case class PositionalTagInfo(
  tagType: String,              // "Outpost", "OpenFile", "WeakSquare", etc.
  square: Option[String],       // For square-based tags
  file: Option[String],         // For file-based tags
  color: String,                // "White" | "Black"
  detail: Option[String] = None // Additional context
)


case class CompensationInfo(
  investedMaterial: Int,        // CP sacrificed
  returnVector: Map[String, Double], // "Time" -> 0.8, "Attack" -> 0.9
  expiryPly: Option[Int],
  conversionPlan: String        // "Mating attack"
)


case class EndgameInfo(
  hasOpposition: Boolean,
  isZugzwang: Boolean,
  keySquaresControlled: List[String],
  oppositionType: String = "None",
  zugzwangLikelihood: Double = 0.0,
  ruleOfSquare: String = "NA",
  triangulationAvailable: Boolean = false,
  kingActivityDelta: Int = 0,
  rookEndgamePattern: String = "None",
  theoreticalOutcomeHint: String = "Unclear",
  confidence: Double = 0.0,
  primaryPattern: Option[String] = None
)


case class PracticalInfo(
  engineScore: Int,
  practicalScore: Double,
  verdict: String               // "Unpleasant Draw", "White is Fighting"
)


case class PreventedPlanInfo(
  planId: String,               // "StopCheck", "PreventFork"
  deniedSquares: List[String],
  breakNeutralized: Option[String],
  mobilityDelta: Int,
  preventedThreatType: Option[String]
)

case class StructureProfileInfo(
  primary: String,
  confidence: Double,
  alternatives: List[String],
  centerState: String,
  evidenceCodes: List[String]
)

case class PlanAlignmentInfo(
  score: Int,
  band: String,
  matchedPlanIds: List[String],
  missingPlanIds: List[String],
  reasonCodes: List[String],
  narrativeIntent: Option[String] = None,
  narrativeRisk: Option[String] = None,
  reasonWeights: Map[String, Double] = Map.empty
)

/** Exposes ExtendedAnalysisData semantic fields to LLM. */
case class SemanticSection(
  structuralWeaknesses: List[WeakComplexInfo],
  pieceActivity: List[PieceActivityInfo],
  positionalFeatures: List[PositionalTagInfo],
  compensation: Option[CompensationInfo],
  endgameFeatures: Option[EndgameInfo],
  practicalAssessment: Option[PracticalInfo],
  preventedPlans: List[PreventedPlanInfo],
  conceptSummary: List[String],  // High-level concepts from ConceptLabeler
  structureProfile: Option[StructureProfileInfo] = None,
  planAlignment: Option[PlanAlignmentInfo] = None
)


enum CandidateTag:
  case Sharp        // High risk, high reward (RiskLevel.High)
  case Solid        // Positional, structural improvement
  case Prophylactic // Neutralizes threat/opponent intent
  case Converting   // Simplification or promotion push
  case Competitive  // PV1 vs PV2 score diff < 30cp
  case TacticalGamble // Refuted by a specific tactical probe


case class PVDelta(
  resolvedThreats: List[String],    // Threats present in current but gone in future
  newOpportunities: List[String],   // New tactical/strategic targets appeared
  planAdvancements: List[String],   // Blockers removed / Prereqs met
  concessions: List[String]         // New threats or lost advantages
)


case class DecisionRationale(
  focalPoint: Option[TargetRef],    // The "star" of the narrative (e.g. "Knight on d5")
  logicSummary: String,             // "Solve X -> Gain Y -> Watch out for Z"
  delta: PVDelta,
  confidence: ConfidenceLevel
)


enum OpeningEvent:
  /** First 1-3 ply: ECO/name confirmed with theme and top moves */
  case Intro(eco: String, name: String, theme: String, topMoves: List[String])
  /** Theory diverges: top move distribution shifts or strategic flow changes */
  case BranchPoint(divergingMoves: List[String], reason: String, sampleGame: Option[String])
  /** Played move not in top 5 or <1% of games */
  case OutOfBook(playedMove: String, topMovesAvailable: List[String], ply: Int)
  /** Sample size drops below threshold - theory ends here */
  case TheoryEnds(lastKnownPly: Int, sampleCount: Int)
  /** OutOfBook + low cpLoss + constructive evidence */
  case Novelty(playedMove: String, cpLoss: Int, evidence: String, ply: Int)


case class OpeningEventBudget(
  introUsed: Boolean = false,
  eventsUsed: Int = 0,
  maxEvents: Int = 2,
  theoryEnded: Boolean = false,
  lastPlyWithData: Int = 0
) {
  def canFireIntro: Boolean = !introUsed
  def canFireEvent: Boolean = !theoryEnded && eventsUsed < maxEvents
  def afterIntro: OpeningEventBudget = copy(introUsed = true)
  def afterEvent: OpeningEventBudget = copy(eventsUsed = eventsUsed + 1)
  def afterTheoryEnds: OpeningEventBudget = copy(theoryEnded = true)
  def updatePly(ply: Int): OpeningEventBudget = copy(lastPlyWithData = ply)
}


case class OpeningReference(
  eco: Option[String],
  name: Option[String],
  totalGames: Int,
  topMoves: List[ExplorerMove],
  sampleGames: List[ExplorerGame],
  description: Option[String] = None
)

case class ExplorerMove(
  uci: String,
  san: String,
  total: Int,
  white: Int,
  draws: Int,
  black: Int,
  performance: Int
)

case class ExplorerGame(
  id: String,
  winner: Option[Color],
  white: ExplorerPlayer,
  black: ExplorerPlayer,
  year: Int,
  month: Int,
  event: Option[String] = None,
  pgn: Option[String] = None  // Optional: first 8-12 ply for snippet
)

case class ExplorerPlayer(name: String, rating: Int)

object ExplorerPlayer:
  given Reads[ExplorerPlayer] = Json.reads[ExplorerPlayer]
  given Writes[ExplorerPlayer] = Json.writes[ExplorerPlayer]

object ExplorerMove:
  given Reads[ExplorerMove] = Json.reads[ExplorerMove]
  given Writes[ExplorerMove] = Json.writes[ExplorerMove]

object ExplorerGame:
  private given Reads[Color] = Reads {
    case JsString(s) if s.equalsIgnoreCase("white") => JsSuccess(chess.White)
    case JsString(s) if s.equalsIgnoreCase("black") => JsSuccess(chess.Black)
    case other                                       => JsError(s"Invalid Color: $other")
  }
  private given Writes[Color] = Writes { c =>
    JsString(if c == chess.White then "white" else "black")
  }

  given Reads[ExplorerGame] = Json.reads[ExplorerGame]
  given Writes[ExplorerGame] = Json.writes[ExplorerGame]

object OpeningReference:
  given Reads[OpeningReference] = Json.reads[OpeningReference]
  given Writes[OpeningReference] = Json.writes[OpeningReference]
