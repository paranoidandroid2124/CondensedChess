package lila.llm.model

import play.api.libs.json._

/**
 * Request for client-side engine probing.
 * Sent when the server detects a "Ghost Plan" that needs verification.
 */
case class ProbeRequest(
  id: String,
  fen: String,
  moves: List[String], // UCI format moves to probe (e.g. "e2e4")
  depth: Int,          // Target depth for the WASM engine
  // Optional metadata for UI/debugging and downstream prompt shaping
  planId: Option[String] = None,
  planName: Option[String] = None,
  planScore: Option[Double] = None,
  // Optional baseline context (usually PV1) so the probe can be self-contained
  baselineMove: Option[String] = None,
  baselineEvalCp: Option[Int] = None,
  baselineMate: Option[Int] = None,
  baselineDepth: Option[Int] = None
)

object ProbeRequest:
  given Reads[ProbeRequest] = Json.reads[ProbeRequest]
  given Writes[ProbeRequest] = Json.writes[ProbeRequest]

/**
 * Raw engine evidence returned by the client.
 */
case class ProbeResult(
  id: String,
  evalCp: Int,               // White POV centipawns (same convention as IntegratedContext.evalCp)
  bestReplyPv: List[String], // UCI moves of the refutation/support line after the probed move
  deltaVsBaseline: Int,      // evalCp - baselineEvalCp (same POV). Negative = worse than baseline.
  keyMotifs: List[String],   // Motifs detected in the probe line
  // Optional metadata to make ProbeResult self-describing (critical for B-axis "Why-not")
  probedMove: Option[String] = None, // The probed candidate move (UCI)
  mate: Option[Int] = None,          // Mate distance if applicable
  depth: Option[Int] = None          // Depth reached by the client engine
)

object ProbeResult:
  given Reads[ProbeResult] = Json.reads[ProbeResult]
  given Writes[ProbeResult] = Json.writes[ProbeResult]

/**
 * A container for a plan and its representative UCI moves.
 * Used by the ProbeDetector to identify which moves to probe for a specific plan.
 */
case class PlanMoveMapping(
  planId: String,
  suggestedMoves: List[String] // List of UCI moves that characterize this plan
)
