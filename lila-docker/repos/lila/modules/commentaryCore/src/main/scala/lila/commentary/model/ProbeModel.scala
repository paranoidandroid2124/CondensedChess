package lila.commentary.model

import _root_.chess.format.Fen
import _root_.chess.variant.Standard
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
  maxCpLoss: Option[Int] = None,           // optional fail-closed bound for viability probes
  candidateMove: Option[String] = None,    // explicit root move when the request is move-bound
  depthFloor: Option[Int] = None,          // minimum acceptable realized depth for certification
  variationHash: Option[String] = None,    // binds the request to a specific logical variation bundle
  engineConfigFingerprint: Option[String] = None // binds the request to engine/config generation
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
  seedId: Option[String] = None,
  requiredSignals: List[String] = Nil,
  generatedRequiredSignals: List[String] = Nil,
  motifInferenceMode: Option[String] = None,
  candidateMove: Option[String] = None,
  depthFloor: Option[Int] = None,
  variationHash: Option[String] = None,
  engineConfigFingerprint: Option[String] = None,
  generatedAtEpochMs: Option[Long] = None
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

  enum ProbeCertificateStatus:
    case Valid
    case WeaklyValid
    case Invalid
    case StaleOrMismatched

  case class ValidationResult(
      isValid: Boolean,
      missingSignals: List[String],
      reasonCodes: List[String],
      certificateStatus: ProbeCertificateStatus = ProbeCertificateStatus.Valid,
      hardReasonCodes: List[String] = Nil,
      softReasonCodes: List[String] = Nil
  )

  private val branchPurposes = Set(
    "reply_multipv",
    "defense_reply_multipv",
    "convert_reply_multipv",
    "recapture_branches",
    "keep_tension_branches",
    "free_tempo_branches"
  )

  private case class PurposeSignalProfile(
      strict: Set[String],
      relaxed: Set[String]
  )

  private val themePurposeSignals: Map[String, PurposeSignalProfile] = Map(
    "theme_plan_validation" ->
      PurposeSignalProfile(
        strict = Set("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"),
        relaxed = Set("replyPvs", "keyMotifs", "futureSnapshot")
      ),
    "route_denial_validation" ->
      PurposeSignalProfile(
        strict = Set("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"),
        relaxed = Set("replyPvs", "keyMotifs", "futureSnapshot")
      ),
    "color_complex_squeeze_validation" ->
      PurposeSignalProfile(
        strict = Set("replyPvs", "keyMotifs", "futureSnapshot"),
        relaxed = Set("replyPvs", "keyMotifs")
      ),
    "long_term_restraint_validation" ->
      PurposeSignalProfile(
        strict = Set("replyPvs", "keyMotifs", "futureSnapshot"),
        relaxed = Set("replyPvs", "keyMotifs")
      )
  )

  /** Strict mode: full signal requirements for fail-closed safety. */
  private val strictPurposeSignals: Map[String, Set[String]] =
    themePurposeSignals.view.mapValues(_.strict).toMap ++ Map(
    "latent_plan_refutation" -> Set("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"),
    "latent_plan_immediate" -> Set("replyPvs", "l1Delta"),
    "free_tempo_branches" -> Set("replyPvs", "futureSnapshot")
  )

  /** Relaxed mode: reduced requirements to improve probe hit rate. */
  private val relaxedPurposeSignals: Map[String, Set[String]] =
    themePurposeSignals.view.mapValues(_.relaxed).toMap ++ Map(
    "latent_plan_refutation" -> Set("replyPvs", "l1Delta"),
    "latent_plan_immediate" -> Set("replyPvs"),
    "free_tempo_branches" -> Set("replyPvs")
  )

  private val RelaxLatentSignals: Boolean =
    sys.env.get("AI_PROBE_RELAX_LATENT_SIGNALS")
      .map(_.trim.toLowerCase)
      .exists(v => v == "1" || v == "true" || v == "yes" || v == "on")

  private def activePurposeSignals: Map[String, Set[String]] =
    if RelaxLatentSignals then relaxedPurposeSignals else strictPurposeSignals

  def validate(result: ProbeResult): ValidationResult =
    val purpose = result.purpose.getOrElse("")
    val required = purposeRequiredSignals(purpose)
    val base = validateSignals(result, required)
    base.copy(
      certificateStatus =
        if base.isValid then ProbeCertificateStatus.Valid
        else ProbeCertificateStatus.Invalid
    )

  def validateAgainstRequest(
      request: ProbeRequest,
      result: ProbeResult
  ): ValidationResult =
    val fromRequest = request.requiredSignals.toSet
    val requestPurpose = request.purpose.map(_.trim).filter(_.nonEmpty)
    val requestPurposeSignals = requestPurpose.map(purposeRequiredSignals).getOrElse(Set.empty)
    val resultPurposeSignals =
      result.purpose.map(_.trim).filter(_.nonEmpty).map(purposeRequiredSignals).getOrElse(Set.empty)
    val required =
      if fromRequest.nonEmpty then fromRequest
      else if requestPurpose.nonEmpty then requestPurposeSignals
      else resultPurposeSignals
    val purposeContractMissing =
      requestPurpose.exists(_ => requestPurposeSignals.isEmpty) ||
        (fromRequest.isEmpty && requestPurpose.isEmpty && resultPurposeSignals.isEmpty)
    val base = validateSignals(result, required)
    val purposeMismatch =
      request.purpose.flatMap(rp => result.purpose.map(_ != rp)).contains(true)
    val idMismatch = request.id != result.id
    val resultFen = result.fen.map(_.trim).filter(_.nonEmpty)
    val requestFen = Option(request.fen).map(_.trim).filter(_.nonEmpty)
    val fenMissing =
      requestFen.nonEmpty && resultFen.isEmpty
    val fenMismatch =
      requestFen.exists(expected => resultFen.exists(_ != expected))
    val requestFenInvalid =
      requestFen.exists(fen => Fen.read(Standard, Fen.Full(fen)).isEmpty)
    val resultFenInvalid =
      resultFen.exists(fen => Fen.read(Standard, Fen.Full(fen)).isEmpty)
    val objectiveMismatch =
      request.objective.flatMap(expected => result.objective.map(_ != expected)).contains(true)
    val seedMismatch =
      request.seedId.flatMap(expected => result.seedId.map(_ != expected)).contains(true)
    val requestMoves =
      request.moves.map(_.trim).filter(_.nonEmpty)
    val allowedMoves =
      (request.candidateMove.map(_.trim).filter(_.nonEmpty).toList ++ requestMoves).distinct
    val resultMove =
      result.probedMove
        .orElse(result.candidateMove)
        .map(_.trim)
        .filter(_.nonEmpty)
    val moveMissing =
      allowedMoves.nonEmpty && resultMove.isEmpty
    val moveMismatch =
      resultMove.exists(move => allowedMoves.nonEmpty && !allowedMoves.contains(move))
    val requestMoveInvalid =
      allowedMoves.exists(move => !validUciMove(move))
    val resultMoveInvalid =
      resultMove.exists(move => !validUciMove(move))
    val variationHashMismatch =
      request.variationHash.flatMap(expected => result.variationHash.map(_ != expected)).contains(true)
    val engineConfigMismatch =
      request.engineConfigFingerprint.flatMap(expected => result.engineConfigFingerprint.map(_ != expected)).contains(true)
    val depthFloor =
      request.depthFloor
        .orElse(Option.when(request.depth > 0)(request.depth))
        .filter(_ > 0)
    val depthFloorUnmet =
      depthFloor.exists(floor => result.depth.exists(_ < floor))
    val hardReasons =
      List(
        Option.when(fenMissing)("FEN_UNVERIFIED"),
        Option.when(requestFenInvalid)("REQUEST_FEN_INVALID"),
        Option.when(resultFenInvalid)("RESULT_FEN_INVALID"),
        Option.when(fenMismatch)("FEN_MISMATCH"),
        Option.when(idMismatch)("ID_MISMATCH"),
        Option.when(moveMissing)("PROBED_MOVE_UNVERIFIED"),
        Option.when(requestMoveInvalid)("REQUEST_MOVE_INVALID"),
        Option.when(moveMismatch)("PROBED_MOVE_MISMATCH"),
        Option.when(resultMoveInvalid)("PROBED_MOVE_INVALID"),
        Option.when(purposeContractMissing)("PURPOSE_CONTRACT_MISSING"),
        Option.when(depthFloor.nonEmpty && result.depth.isEmpty)("DEPTH_FLOOR_UNVERIFIED"),
        Option.when(depthFloorUnmet)("DEPTH_FLOOR_UNMET")
      ).flatten
    val softReasons =
      List(
        Option.when(purposeMismatch)("PURPOSE_MISMATCH"),
        Option.when(objectiveMismatch)("OBJECTIVE_MISMATCH"),
        Option.when(seedMismatch)("SEED_MISMATCH"),
        Option.when(request.variationHash.exists(_.trim.nonEmpty) && result.variationHash.forall(_.trim.isEmpty))("VARIATION_HASH_MISSING"),
        Option.when(variationHashMismatch)("VARIATION_HASH_MISMATCH"),
        Option.when(
          request.engineConfigFingerprint.exists(_.trim.nonEmpty) &&
            result.engineConfigFingerprint.forall(_.trim.isEmpty)
        )("ENGINE_CONFIG_FINGERPRINT_MISSING"),
        Option.when(engineConfigMismatch)("ENGINE_CONFIG_MISMATCH")
      ).flatten
    val allHardReasons = (base.hardReasonCodes ++ hardReasons).distinct
    val certificateStatus =
      if allHardReasons.nonEmpty then ProbeCertificateStatus.Invalid
      else if softReasons.nonEmpty then ProbeCertificateStatus.WeaklyValid
      else ProbeCertificateStatus.Valid
    base.copy(
      isValid = allHardReasons.isEmpty,
      reasonCodes = (base.reasonCodes ++ allHardReasons ++ softReasons).distinct,
      certificateStatus = certificateStatus,
      hardReasonCodes = allHardReasons,
      softReasonCodes = softReasons.distinct
    )

  private def validUciMove(raw: String): Boolean =
    Option(raw).map(_.trim.toLowerCase).exists(_.matches("""[a-h][1-8][a-h][1-8][nbrq]?"""))

  private def validateSignals(
      result: ProbeResult,
      requiredSignals: Set[String]
  ): ValidationResult =
    if requiredSignals.isEmpty then
      ValidationResult(
        isValid = false,
        missingSignals = Nil,
        reasonCodes = List("NO_REQUIRED_SIGNALS"),
        hardReasonCodes = List("NO_REQUIRED_SIGNALS")
      )
    else
      val missing = requiredSignals.filterNot(sig => hasSignal(sig, result)).toList.sorted
      val hardReasons =
        if missing.isEmpty then Nil else List("MISSING_REQUIRED_SIGNALS") ++ missing
      ValidationResult(
        isValid = missing.isEmpty,
        missingSignals = missing,
        reasonCodes =
          if missing.isEmpty then List("REQUIRED_SIGNALS_PRESENT")
          else List("MISSING_REQUIRED_SIGNALS"),
        hardReasonCodes = hardReasons
      )

  private def purposeRequiredSignals(purpose: String): Set[String] =
    activePurposeSignals.getOrElse(
      purpose,
      if branchPurposes.contains(purpose) then Set("replyPvs") else Set.empty[String]
    )

  private def hasSignal(signal: String, result: ProbeResult): Boolean =
    signal match
      case "replyPvs" =>
        result.replyPvs.exists(_.exists(_.nonEmpty)) || result.bestReplyPv.nonEmpty
      case "keyMotifs" =>
        result.keyMotifs.nonEmpty && !clientGeneratedProbeSignal(signal, result)
      case "l1Delta" =>
        result.l1Delta.isDefined && !clientGeneratedProbeSignal(signal, result)
      case "futureSnapshot" =>
        result.futureSnapshot.isDefined && !clientGeneratedProbeSignal(signal, result)
      case "purpose" =>
        result.purpose.exists(_.nonEmpty)
      case "depth" =>
        result.depth.exists(_ > 0)
      case "variationHash" =>
        result.variationHash.exists(_.trim.nonEmpty)
      case "engineConfigFingerprint" =>
        result.engineConfigFingerprint.exists(_.trim.nonEmpty)
      case _ =>
        false

  private def clientGeneratedProbeSignal(signal: String, result: ProbeResult): Boolean =
    val normalizedSignal = signal.trim
    val generatedByRequiredSignals =
      result.generatedRequiredSignals.exists(_.trim == normalizedSignal)
    val generatedByPurposeOnlyInference =
      result.motifInferenceMode.exists { raw =>
        val mode = raw.trim.toLowerCase
        mode == "purpose_only" || mode == "purpose_plus_compat"
      } && Set("keyMotifs", "l1Delta", "futureSnapshot").contains(normalizedSignal)
    generatedByRequiredSignals || generatedByPurposeOnlyInference
