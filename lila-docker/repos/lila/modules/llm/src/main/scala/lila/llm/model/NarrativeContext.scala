package lila.llm.model

import chess.{Color, Square}
import lila.llm.analysis.L3._
import lila.llm.analysis.PlanMatcher.ActivePlans

/**
 * Phase 6: Narrative Context
 * 
 * Aggregates all analysis layers into a hierarchical structure for LLM prompts.
 * Format: Header → Summary → Evidence Tables → Delta
 */
case class NarrativeContext(
  // === HEADER (Context) ===
  header: ContextHeader,
  
  // === SUMMARY (5 lines) ===
  summary: NarrativeSummary,
  
  // === EVIDENCE TABLES ===
  threats: ThreatTable,
  pawnPlay: PawnPlayTable,
  plans: PlanTable,
  l1Snapshot: L1Snapshot,
  
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
  strategicFlow: Option[String] = None // NEW: Transition justification
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
  square: Option[String],    // Attack square
  lossIfIgnoredCp: Int,
  turnsToImpact: Int,
  bestDefense: Option[String],
  defenseCount: Int,
  insufficientData: Boolean
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
  evidence: List[String]      // Top 2 evidence items
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
  planAlignment: String,       // "attack aligned" | "defensive"
  tacticalAlert: Option[String], // "allows Qb6 response"
  practicalDifficulty: String, // "clean" | "complex"
  whyNot: Option[String]       // Refutation reason if inferior
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
 * Attack/Defend targets.
 */
case class Targets(
  attackTargets: List[(String, String)],  // (square, reason)
  defendTargets: List[(String, String)]
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
