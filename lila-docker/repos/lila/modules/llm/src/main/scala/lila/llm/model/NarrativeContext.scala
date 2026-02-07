package lila.llm.model

import chess.Color
import lila.llm.model.authoring.{ AuthorQuestion, QuestionEvidence }
import play.api.libs.json.*

/**
 * Phase 6: Narrative Context
 * 
 * Aggregates all analysis layers into a hierarchical structure for LLM prompts.
 * Format: Header → Summary → Evidence Tables → Delta
 */
case class NarrativeContext(
  // === POSITION CONTEXT ===
  // FEN used for analysis (position before `playedMove` in move-annotation mode).
  fen: String,

  // === HEADER (Context) ===
  header: ContextHeader,

  // === MOVE ANNOTATION CONTEXT ===
  // When present, BookStyleRenderer should frame the prose as an annotation of `playedMove`.
  ply: Int,
  playedMove: Option[String] = None, // UCI
  playedSan: Option[String] = None,  // SAN (derived from `fen` used for analysis)
  counterfactual: Option[lila.llm.model.strategic.CounterfactualMatch] = None,
  
  // === SUMMARY (5 lines) ===
  summary: NarrativeSummary,
  
  // === EVIDENCE TABLES ===
  threats: ThreatTable,
  pawnPlay: PawnPlayTable,
  plans: PlanTable,
  snapshots: List[L1Snapshot] = Nil,
  
  // === DELTA (changes from prev move) ===
  delta: Option[MoveDelta],
  
  // === PHASE CONTEXT (A8) ===
  phase: PhaseContext,
  
  // === CANDIDATES (enhanced) ===
  candidates: List[CandidateInfo],

  // === EVIDENCE AUGMENTATION (Phase 6.5) ===
  probeRequests: List[ProbeRequest] = Nil,

  // === META SIGNALS (B-axis) ===
  meta: Option[MetaSignals] = None,
  strategicFlow: Option[String] = None, // Transition justification
  
  // === SEMANTIC SECTION (Phase A) ===
  semantic: Option[SemanticSection] = None, // ExtendedAnalysisData semantic fields
  
  // === OPPONENT PLAN (Phase B) ===
  opponentPlan: Option[PlanRow] = None, // Top plan for opponent (side=!toMove)

  // === DECISION RATIONALE (Phase F) ===
  decision: Option[DecisionRationale] = None,

  // === OPENING EVENT (Phase A9) ===
  openingEvent: Option[OpeningEvent] = None,
  
  // === OPENING DATA (for internal use, not rendered directly) ===
  openingData: Option[OpeningReference] = None,
  
  // === UPDATED BUDGET (for game-level accumulation) ===
  updatedBudget: OpeningEventBudget = OpeningEventBudget(),

  // === PHASE 14: ENGINE EVIDENCE (raw PV for book-style rendering) ===
  engineEvidence: Option[lila.llm.model.strategic.EngineEvidence] = None,

  // === PHASE 1: AUTHOR QUESTIONS (book-style "decision points") ===
  authorQuestions: List[AuthorQuestion] = Nil,
  authorEvidence: List[QuestionEvidence] = Nil,

  // === PHASE 23: VERIFIED FACTS (Truth Maintenance) ===
  facts: List[Fact] = Nil,

  // === DELTA MODE ===
  // True when `delta` represents the immediate before/after of a played move.
  // (As opposed to a cross-moment delta when narrating key moments of a game.)
  deltaAfterMove: Boolean = false
)


/**
 * A8: Phase context with evidence and transition triggers.
 */
case class PhaseContext(
  current: String,            // "Opening", "Middlegame", "Endgame"
  reason: String,             // "Material: 45, Queens present"
  transitionTrigger: Option[String] = None // "Minor piece trade triggered Endgame"
)

/**
 * Context header for narrative routing.
 */
case class ContextHeader(
  phase: String,           // "Opening" | "Middlegame" | "Endgame"
  criticality: String,     // "Normal" | "Critical" | "Forced"
  choiceType: String,      // "OnlyMove" | "NarrowChoice" | "StyleChoice"
  riskLevel: String,       // "Low" | "Medium" | "High"
  taskMode: String         // "ExplainPlan" | "ExplainTactics" | "ExplainDefense" | "ExplainConvert"
)

/**
 * 5-line narrative summary.
 */
case class NarrativeSummary(
  primaryPlan: String,        // "Kingside Attack (0.85)"
  keyThreat: Option[String],  // "Fork on e5 (must defend)"
  choiceType: String,         // "Style Choice (multiple valid paths)"
  tensionPolicy: String,      // "Maintain (attack preparation)"
  evalDelta: String           // "+0.5 eval, RookLift appeared"
)

// ============================================================
// EVIDENCE TABLES
// ============================================================

/**
 * Threats table (bidirectional).
 */
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
  confidence: ConfidenceLevel = ConfidenceLevel.Engine, // Default for threats from engine
  isTopCandidateDefense: Boolean = false // Item 3: true if bestDefense matches top candidate
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

/**
 * Pawn play table.
 */
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

/**
 * Plans table (Top 5 + suppressed).
 */
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
  confidence: ConfidenceLevel = ConfidenceLevel.Heuristic, // Default for plans from matcher
  isEstablished: Boolean = false // P10 Issue 2: true if this is a current fact, not anticipated
)

case class SuppressedPlan(
  name: String,
  originalScore: Double,
  reason: String,
  isRemoved: Boolean
)

/**
 * L1 snapshot (key numbers only).
 */
case class L1Snapshot(
  material: String,            // "+2" | "=" | "-1"
  imbalance: Option[String],   // "Bishop pair" | "Exchange down"
  kingSafetyUs: Option[String],    // "Exposed (2 attackers, 1 escape)"
  kingSafetyThem: Option[String],
  mobility: Option[String],    // "Advantage (+3 moves)"
  centerControl: Option[String], // "Dominant"
  openFiles: List[String]      // ["d", "e"]
)

/**
 * Delta (changes from previous move).
 */
case class MoveDelta(
  evalChange: Int,             // CP change (signed)
  newMotifs: List[String],     // Appeared motifs
  lostMotifs: List[String],    // Disappeared motifs
  structureChange: Option[String], // "d5 pawn became weak"
  openFileCreated: Option[String],
  phaseChange: Option[String]        // "middlegame → endgame"
)

// ============================================================
// CANDIDATES
// ============================================================

/**
 * Enhanced candidate move info.
 */
case class CandidateInfo(
  move: String,                // SAN
  uci: Option[String] = None,  // NEW: UCI format for probe result matching
  annotation: String,          // "!" | "?" | ""
  planAlignment: String,       // Immediate intent (e.g., "Development")
  downstreamTactic: Option[String] = None, // Phase 22.5: Downstream consequence (e.g., "fork threat")
  tacticalAlert: Option[String], // "allows Qb6 response"
  practicalDifficulty: String, // "clean" | "complex"
  whyNot: Option[String],       // Refutation reason if inferior
  tags: List[CandidateTag] = Nil, // Sharp, Solid, etc.
  tacticEvidence: List[String] = Nil, // P12: Evidence for tactical labels (motifs, victims)
  probeLines: List[String] = Nil, // Bookmaker: optional a1/a2 reply samples from probes (SAN)
  facts: List[Fact] = Nil // Phase 23: Verified facts for this move
)

// ============================================================
// META SIGNALS (B-axis)
// ============================================================

/**
 * Choice type classification.
 */
enum ChoiceType:
  case OnlyMove      // PV1-PV2 > 200cp
  case StyleChoice   // PV1-PV2 < 50cp
  case NarrowChoice  // 50-200cp
  case Complex       // Many sharp lines

// ============================================================
// CONFIDENCE LEVELS (Phase B)
// ============================================================

/**
 * Confidence level for signals - helps LLM adjust tone.
 * Engine: Definitive (from engine eval)
 * Probe: Verified (from WASM probe)
 * Heuristic: Suggestive (from board heuristics)
 */
enum ConfidenceLevel:
  case Engine     // Definitive - "확실히", backed by engine
  case Probe      // Verified - "검증됨", backed by probe result
  case Heuristic  // Suggestive - "그림상", board pattern heuristic

/**
 * Error classification for counterfactuals.
 */
case class ErrorClassification(
  isTactical: Boolean,         // turnsToImpact <= 2 && loss >= 200
  missedMotifs: List[String],
  errorSummary: String         // "전술(2수 내 300cp)" or "포지셔널"
)

/**
 * Divergence info for user vs best line.
 */
case class DivergenceInfo(
  divergePly: Int,             // Actual ply where lines diverge
  punisherMove: Option[String], // Move that punishes user's choice
  branchPointFen: Option[String]
)

/**
 * Targeting reference - Square, File, or Piece.
 */
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

/**
 * Tactical vs Strategic targets.
 */
case class Targets(
  tactical: List[TargetEntry],  // Immediate attack/defend
  strategic: List[TargetEntry]  // Outposts, open files, blockades, weaknesses
)

/**
 * Plan concurrency hint.
 */
case class PlanConcurrency(
  primary: String,
  secondary: Option[String],
  relationship: String         // "↔ synergy" | "⟂ conflict" | "independent"
)

/**
 * B-axis: Unified meta-signals container.
 * Populated by NarrativeContextBuilder from L3/L4 analysis.
 */
case class MetaSignals(
  choiceType: ChoiceType,                     // B1: from classification.choiceTopology
  targets: Targets,                           // B5: from threatsToUs/Them
  planConcurrency: PlanConcurrency,           // B8: from compatibilityEvents
  divergence: Option[DivergenceInfo] = None,  // B3: counterfactual comparison
  errorClass: Option[ErrorClassification] = None, // B2/B6: tactical vs positional
  whyNot: Option[String] = None               // B7: ProbeResult-based refutation
)

// ============================================================
// SEMANTIC SECTION (Phase A Enhancement)
// ============================================================

/**
 * Wrapper for structural weakness with narrative-friendly format.
 */
case class WeakComplexInfo(
  owner: String,                // "White" | "Black" (side that owns the weakness)
  squareColor: String,          // "light" | "dark"
  squares: List[String],        // ["f3", "g2", "h3"]
  isOutpost: Boolean,
  cause: String                 // "Missing fianchetto bishop"
)

/**
 * Wrapper for piece activity with narrative-friendly format.
 */
case class PieceActivityInfo(
  piece: String,                // "Knight"
  square: String,               // "c3"
  mobilityScore: Double,        // 0.0 - 1.0
  isTrapped: Boolean,
  isBadBishop: Boolean,
  keyRoutes: List[String],      // ["e4", "d5"]
  coordinationLinks: List[String]
)

/**
 * Wrapper for positional tag with flattened structure.
 */
case class PositionalTagInfo(
  tagType: String,              // "Outpost", "OpenFile", "WeakSquare", etc.
  square: Option[String],       // For square-based tags
  file: Option[String],         // For file-based tags
  color: String,                // "White" | "Black"
  detail: Option[String] = None // Additional context
)

/**
 * Wrapper for compensation assessment.
 */
case class CompensationInfo(
  investedMaterial: Int,        // CP sacrificed
  returnVector: Map[String, Double], // "Time" -> 0.8, "Attack" -> 0.9
  expiryPly: Option[Int],
  conversionPlan: String        // "Mating attack"
)

/**
 * Wrapper for endgame features.
 */
case class EndgameInfo(
  hasOpposition: Boolean,
  isZugzwang: Boolean,
  keySquaresControlled: List[String]
)

/**
 * Wrapper for practical assessment.
 */
case class PracticalInfo(
  engineScore: Int,
  practicalScore: Double,
  verdict: String               // "Unpleasant Draw", "White is Fighting"
)

/**
 * Wrapper for prevented plan.
 */
case class PreventedPlanInfo(
  planId: String,               // "StopCheck", "PreventFork"
  deniedSquares: List[String],
  breakNeutralized: Option[String],
  mobilityDelta: Int,
  preventedThreatType: Option[String]
)

/**
 * Semantic section: Exposes ExtendedAnalysisData semantic fields to LLM.
 * Provides 2-3x information increase without new analyzers.
 */
case class SemanticSection(
  structuralWeaknesses: List[WeakComplexInfo],
  pieceActivity: List[PieceActivityInfo],
  positionalFeatures: List[PositionalTagInfo],
  compensation: Option[CompensationInfo],
  endgameFeatures: Option[EndgameInfo],
  practicalAssessment: Option[PracticalInfo],
  preventedPlans: List[PreventedPlanInfo],
  conceptSummary: List[String]  // High-level concepts from ConceptLabeler
)

// ============================================================
// PHASE F: DECISION RATIONALE MODELS
// ============================================================

/**
 * High-level category for candidate moves.
 */
enum CandidateTag:
  case Sharp        // High risk, high reward (RiskLevel.High)
  case Solid        // Positional, structural improvement
  case Prophylactic // Neutralizes threat/opponent intent
  case Converting   // Simplification or promotion push
  case Competitive  // PV1 vs PV2 score diff < 30cp
  case TacticalGamble // Refuted by a specific tactical probe

/**
 * Delta analysis for a specific PV (usually PV1).
 * Shows what's solved vs what's created.
 */
case class PVDelta(
  resolvedThreats: List[String],    // Threats present in current but gone in future
  newOpportunities: List[String],   // New tactical/strategic targets appeared
  planAdvancements: List[String],   // Blockers removed / Prereqs met
  concessions: List[String]         // New threats or lost advantages
)

/**
 * Synthesis of the entire position's logic.
 */
case class DecisionRationale(
  focalPoint: Option[TargetRef],    // The "star" of the narrative (e.g. "Knight on d5")
  logicSummary: String,             // "Solve X -> Gain Y -> Watch out for Z"
  delta: PVDelta,
  confidence: ConfidenceLevel
)

// ============================================================
// PHASE A9: OPENING EVENT LAYER
// ============================================================

/**
 * A9 Event: Triggered opening-related events (not per-move references).
 * Budget: Max 2-3 per game (Intro + 1-2 events)
 */
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

/**
 * Budget tracker to limit A9 mentions per game.
 */
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

/**
 * Explorer-driven reference data (for event detection).
 * Kept for data fetching, but events are derived from this.
 */
case class OpeningReference(
  eco: Option[String],
  name: Option[String],
  totalGames: Int,
  topMoves: List[ExplorerMove],
  sampleGames: List[ExplorerGame],
  isNoveltyCandidate: Boolean = false,
  outOfBook: Boolean = false,
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
