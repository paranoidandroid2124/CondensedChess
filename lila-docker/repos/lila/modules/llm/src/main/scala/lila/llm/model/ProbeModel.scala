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
  purpose: Option[String] = None, // e.g. "recapture_branches", "reply_multipv"
  questionId: Option[String] = None,
  questionKind: Option[String] = None,
  multiPv: Option[Int] = None,
  planId: Option[String] = None,
  planName: Option[String] = None,
  planScore: Option[Double] = None,
  // Optional baseline context (usually PV1) so the probe can be self-contained
  baselineMove: Option[String] = None,
  baselineEvalCp: Option[Int] = None,
  baselineMate: Option[Int] = None,
  baselineDepth: Option[Int] = None,
  // v2: objective-driven probing contract
  objective: Option[String] = None,        // e.g. "validate_latent_plan", "refute_plan", "compare_branches"
  seedId: Option[String] = None,           // Latent seed identifier when relevant
  requiredSignals: List[String] = Nil,     // e.g. "replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"
  horizon: Option[String] = None,          // "short" | "medium" | "long"
  maxCpLoss: Option[Int] = None            // optional fail-closed bound for viability probes
)

object ProbeRequest:
  given Reads[ProbeRequest] = Json.reads[ProbeRequest]
  given Writes[ProbeRequest] = Json.writes[ProbeRequest]

/**
 * Raw engine evidence returned by the client.
 */
case class ProbeResult(
  id: String,
  fen: Option[String] = None, // Base FEN the probe was run from (critical when probing non-root branches)
  evalCp: Int,               // White POV centipawns (same convention as IntegratedContext.evalCp)
  bestReplyPv: List[String], // UCI moves of the refutation/support line after the probed move
  // Optional: MultiPV reply lines (first element should correspond to bestReplyPv)
  replyPvs: Option[List[List[String]]] = None,
  deltaVsBaseline: Int,      // evalCp - baselineEvalCp (same POV). Negative = worse than baseline.
  keyMotifs: List[String],   // Motifs detected in the probe line
  // Optional metadata to make ProbeResult self-describing (critical for B-axis "Why-not")
  purpose: Option[String] = None,
  questionId: Option[String] = None,
  questionKind: Option[String] = None,
  probedMove: Option[String] = None, // The probed candidate move (UCI)
  mate: Option[Int] = None,          // Mate distance if applicable
  depth: Option[Int] = None,         // Depth reached by the client engine
  // Phase C: L1 delta for stronger counterfactual explanations
  l1Delta: Option[L1DeltaSnapshot] = None,
  // P1: Structured future state for accurate delta comparison
  futureSnapshot: Option[FutureSnapshot] = None,
  // v2: optional contract diagnostics
  objective: Option[String] = None,
  seedId: Option[String] = None
)

object ProbeResult:
  given Reads[ProbeResult] = Json.reads[ProbeResult]
  given Writes[ProbeResult] = Json.writes[ProbeResult]

/**
 * L1 positional delta after applying a candidate move.
 * "What changed structurally?" - for explaining why a move is bad/good
 * beyond just the eval delta.
 */
case class L1DeltaSnapshot(
  materialDelta: Int,           // Material change in centipawns (White POV)
  kingSafetyDelta: Int,         // King attackers/escapes change (+ = safer, - = more exposed)
  centerControlDelta: Int,      // Center control change
  openFilesDelta: Int,          // Change in open file control
  mobilityDelta: Int,           // Mobility change
  // Human-readable summary of what collapsed/improved
  collapseReason: Option[String] = None  // e.g. "King exposed", "Lost center control"
)

object L1DeltaSnapshot:
  given Reads[L1DeltaSnapshot] = Json.reads[L1DeltaSnapshot]
  given Writes[L1DeltaSnapshot] = Json.writes[L1DeltaSnapshot]

/**
 * P1: Structured future state snapshot for accurate PVDelta comparison.
 * Populated by WASM client after applying the probed move.
 */
case class FutureSnapshot(
  resolvedThreatKinds: List[String],   // ThreatKinds present before but gone after (e.g., "Mate", "Material")
  newThreatKinds: List[String],        // ThreatKinds that newly appear after the move
  targetsDelta: TargetsDelta,          // Targets added/removed
  planBlockersRemoved: List[String],   // Plan blockers that were neutralized
  planPrereqsMet: List[String]         // Plan prerequisites that are now satisfied
)

object FutureSnapshot:
  given Reads[FutureSnapshot] = Json.reads[FutureSnapshot]
  given Writes[FutureSnapshot] = Json.writes[FutureSnapshot]

/**
 * P1: Delta in tactical and strategic targets.
 */
case class TargetsDelta(
  tacticalAdded: List[String],    // New tactical targets (squares) created
  tacticalRemoved: List[String],  // Tactical targets that are no longer relevant
  strategicAdded: List[String],   // New strategic targets (outposts, files, etc.)
  strategicRemoved: List[String]  // Strategic targets that are neutralized
)

object TargetsDelta:
  given Reads[TargetsDelta] = Json.reads[TargetsDelta]
  given Writes[TargetsDelta] = Json.writes[TargetsDelta]

/**
 * Purpose-aware probe contract validator.
 * Fail-closed: if required signals are missing for a purpose, the probe should
 * not be used to support strong commentary claims.
 */
object ProbeContractValidator:

  case class ValidationResult(
      isValid: Boolean,
      missingSignals: List[String],
      reasonCodes: List[String]
  )

  private val branchPurposes = Set(
    "reply_multipv",
    "defense_reply_multipv",
    "convert_reply_multipv",
    "recapture_branches",
    "keep_tension_branches",
    "free_tempo_branches"
  )

  private val strongPurposeSignals: Map[String, Set[String]] = Map(
    "latent_plan_refutation" -> Set("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"),
    "latent_plan_immediate" -> Set("replyPvs", "l1Delta"),
    "free_tempo_branches" -> Set("replyPvs", "futureSnapshot")
  )

  def validate(result: ProbeResult): ValidationResult =
    val purpose = result.purpose.getOrElse("")
    val required =
      strongPurposeSignals.getOrElse(
        purpose,
        if branchPurposes.contains(purpose) then Set("replyPvs") else Set.empty[String]
      )
    validateSignals(result, required)

  def validateAgainstRequest(
      request: ProbeRequest,
      result: ProbeResult
  ): ValidationResult =
    val fromRequest = request.requiredSignals.toSet
    val fromPurpose = validate(result)
    val required =
      if fromRequest.nonEmpty then fromRequest
      else purposeRequiredSignals(result.purpose.getOrElse(""))
    val base = validateSignals(result, required)
    val purposeMismatch =
      request.purpose.flatMap(rp => result.purpose.map(_ != rp)).contains(true)
    val idMismatch = request.id != result.id
    val extraReasons =
      List(
        Option.when(purposeMismatch)("PURPOSE_MISMATCH"),
        Option.when(idMismatch)("ID_MISMATCH"),
        Option.when(fromPurpose.missingSignals.nonEmpty && fromRequest.isEmpty)("PURPOSE_CONTRACT_MISSING")
      ).flatten
    base.copy(
      isValid = base.isValid && !purposeMismatch && !idMismatch,
      reasonCodes = (base.reasonCodes ++ extraReasons).distinct
    )

  private def validateSignals(
      result: ProbeResult,
      requiredSignals: Set[String]
  ): ValidationResult =
    if requiredSignals.isEmpty then
      ValidationResult(
        isValid = true,
        missingSignals = Nil,
        reasonCodes = List("NO_REQUIRED_SIGNALS")
      )
    else
      val missing = requiredSignals.filterNot(sig => hasSignal(sig, result)).toList.sorted
      ValidationResult(
        isValid = missing.isEmpty,
        missingSignals = missing,
        reasonCodes =
          if missing.isEmpty then List("REQUIRED_SIGNALS_PRESENT")
          else List("MISSING_REQUIRED_SIGNALS")
      )

  private def purposeRequiredSignals(purpose: String): Set[String] =
    strongPurposeSignals.getOrElse(
      purpose,
      if branchPurposes.contains(purpose) then Set("replyPvs") else Set.empty[String]
    )

  private def hasSignal(signal: String, result: ProbeResult): Boolean =
    signal match
      case "replyPvs" =>
        result.replyPvs.exists(_.exists(_.nonEmpty)) || result.bestReplyPv.nonEmpty
      case "keyMotifs" =>
        result.keyMotifs.nonEmpty
      case "l1Delta" =>
        result.l1Delta.isDefined
      case "futureSnapshot" =>
        result.futureSnapshot.isDefined
      case "purpose" =>
        result.purpose.exists(_.nonEmpty)
      case "depth" =>
        result.depth.exists(_ > 0)
      case _ =>
        true
