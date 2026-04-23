package lila.commentary.certification

import chess.Color
import chess.format.{ Fen, Uci }

import scala.util.control.NonFatal

import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.strategic.StrategicObjectExtraction
import lila.commentary.witness.WitnessAnchor

object CertificationEngineRuntimeIntake:

  enum Status:
    case Missing, Accepted, Rejected

  enum RuntimeScore:
    case Centipawns(cp: Int)
    case MateIn(plies: Int)

  enum RuntimeScorePerspective:
    case SideToMove, White, Black

  enum RuntimeScoreRequirement:
    case CentipawnAtLeast(cp: Int)
    case CentipawnAtMost(cp: Int)
    case CentipawnSwingAtLeast(cp: Int)
    case MateInAtMost(plies: Int)

  enum RuntimeAnchor:
    case Board

  final case class RuntimeTransitionBinding(
      beforeFen: String,
      playedMove: String,
      afterFen: String,
      beforeNodeId: Option[String] = None,
      beforePly: Option[Int] = None
  )

  final case class RuntimeSearchState(
      requestedDepth: Int,
      realizedDepth: Int,
      multiPv: Int,
      completed: Boolean,
      generatedAtEpochMs: Long,
      maxAgeMs: Long,
      engineConfigFingerprint: String
  )

  final case class RuntimeBaselinePacket(
      fen: String,
      nodeId: String,
      ply: Int,
      search: RuntimeSearchState,
      score: RuntimeScore,
      scorePerspective: RuntimeScorePerspective,
      pvLines: Vector[Vector[String]]
  )

  final case class RuntimeCertificationClaim(
      familyId: String,
      owner: String,
      purposes: Map[String, String],
      anchor: RuntimeAnchor = RuntimeAnchor.Board,
      minDepth: Int,
      minMultiPv: Int,
      minPvPlies: Int,
      requiredScore: Option[RuntimeScoreRequirement]
  )

  final case class RuntimeEnginePacket(
      fen: String,
      nodeId: String,
      ply: Int,
      requestedDepth: Int,
      realizedDepth: Int,
      multiPv: Int,
      completed: Boolean,
      generatedAtEpochMs: Long,
      maxAgeMs: Long,
      engineConfigFingerprint: String,
      score: RuntimeScore,
      scorePerspective: RuntimeScorePerspective,
      pvLines: Vector[Vector[String]],
      claims: Vector[RuntimeCertificationClaim],
      transition: Option[RuntimeTransitionBinding] = None,
      baseline: Option[RuntimeBaselinePacket] = None
  )

  final case class IntakeResult(
      status: Status,
      evidence: CertificationEngineEvidence,
      reason: Option[String] = None
  )

  def forObjectExtraction(
      current: StrategicObjectExtraction,
      expectedNode: EngineNodeIdentity,
      expectedFen: Fen.Full,
      packet: Option[RuntimeEnginePacket],
      nowEpochMs: Long
  ): IntakeResult =
    packet match
      case None => IntakeResult(Status.Missing, CertificationEngineEvidence.empty)
      case Some(raw) =>
        normalizePacket(raw).flatMap: typed =>
          CertificationEngineEvidenceContract.forObjectExtraction(
            current = current,
            expectedNode = expectedNode,
            expectedFen = expectedFen,
            packet = typed,
            nowEpochMs = nowEpochMs
          )
        match
          case Right(evidence) => IntakeResult(Status.Accepted, evidence)
          case Left(message) => IntakeResult(Status.Rejected, CertificationEngineEvidence.empty, Some(message))

  def forDeltaExtraction(
      delta: StrategicDeltaExtraction,
      expectedNode: EngineNodeIdentity,
      expectedBeforeFen: Fen.Full,
      expectedAfterFen: Fen.Full,
      packet: Option[RuntimeEnginePacket],
      nowEpochMs: Long
  ): IntakeResult =
    packet match
      case None => IntakeResult(Status.Missing, CertificationEngineEvidence.empty)
      case Some(raw) =>
        normalizePacket(raw).flatMap: typed =>
          CertificationEngineEvidenceContract.forDeltaExtraction(
            delta = delta,
            expectedNode = expectedNode,
            expectedBeforeFen = expectedBeforeFen,
            expectedAfterFen = expectedAfterFen,
            packet = typed,
            nowEpochMs = nowEpochMs
          )
        match
          case Right(evidence) => IntakeResult(Status.Accepted, evidence)
          case Left(message) => IntakeResult(Status.Rejected, CertificationEngineEvidence.empty, Some(message))

  def extractCertificationsForObject(
      current: StrategicObjectExtraction,
      baseEvidence: CertificationEvidenceBundle,
      expectedNode: EngineNodeIdentity,
      expectedFen: Fen.Full,
      packet: Option[RuntimeEnginePacket],
      nowEpochMs: Long
  ): Either[String, CertificationExtraction] =
    val intake = forObjectExtraction(current, expectedNode, expectedFen, packet, nowEpochMs)
    for
      combined <- combineForObject(current, baseEvidence, intake.evidence.asBundle)
      extraction <- CertificationExtractor.fromObjectExtractionFailClosed(current, combined)
    yield extraction

  def extractCertificationsForDelta(
      delta: StrategicDeltaExtraction,
      baseEvidence: CertificationEvidenceBundle,
      expectedNode: EngineNodeIdentity,
      expectedBeforeFen: Fen.Full,
      expectedAfterFen: Fen.Full,
      packet: Option[RuntimeEnginePacket],
      nowEpochMs: Long
  ): Either[String, CertificationExtraction] =
    val intake = forDeltaExtraction(delta, expectedNode, expectedBeforeFen, expectedAfterFen, packet, nowEpochMs)
    for
      combined <- combineForDelta(delta, baseEvidence, intake.evidence.asBundle)
      extraction <- CertificationExtractor.fromDeltaExtractionFailClosed(delta, combined)
    yield extraction

  private def normalizePacket(raw: RuntimeEnginePacket): Either[String, EngineEvidencePacket] =
    safe:
      EngineEvidencePacket(
        identity = EnginePacketIdentity(
          fen = raw.fen,
          node = EngineNodeIdentity(raw.nodeId, raw.ply),
          transition = raw.transition.map(normalizeTransition)
        ),
        search = EngineSearchState(
          requestedDepth = raw.requestedDepth,
          realizedDepth = raw.realizedDepth,
          multiPv = raw.multiPv,
          completed = raw.completed,
          generatedAtEpochMs = raw.generatedAtEpochMs,
          maxAgeMs = raw.maxAgeMs,
          engineConfigFingerprint = raw.engineConfigFingerprint
        ),
        score = normalizeScore(raw.score),
        scorePerspective = normalizePerspective(raw.scorePerspective),
        pvLines = raw.pvLines.map(normalizePvLine),
        claims = raw.claims.map(normalizeClaim),
        baseline = raw.baseline.map(normalizeBaseline)
      )

  private def normalizeTransition(raw: RuntimeTransitionBinding): EngineTransitionBinding =
    val beforeNode =
      (raw.beforeNodeId, raw.beforePly) match
        case (Some(nodeId), Some(ply)) => Some(EngineNodeIdentity(nodeId, ply))
        case (None, None) => None
        case _ =>
          throw IllegalArgumentException(
            "Engine runtime transition beforeNodeId and beforePly must be supplied together"
          )
    EngineTransitionBinding(raw.beforeFen, raw.playedMove, raw.afterFen, beforeNode)

  private def normalizeBaseline(raw: RuntimeBaselinePacket): EngineBaselinePacket =
    EngineBaselinePacket(
      identity = EnginePacketIdentity(raw.fen, EngineNodeIdentity(raw.nodeId, raw.ply)),
      search = EngineSearchState(
        requestedDepth = raw.search.requestedDepth,
        realizedDepth = raw.search.realizedDepth,
        multiPv = raw.search.multiPv,
        completed = raw.search.completed,
        generatedAtEpochMs = raw.search.generatedAtEpochMs,
        maxAgeMs = raw.search.maxAgeMs,
        engineConfigFingerprint = raw.search.engineConfigFingerprint
      ),
      score = normalizeScore(raw.score),
      scorePerspective = normalizePerspective(raw.scorePerspective),
      pvLines = raw.pvLines.map(normalizePvLine)
    )

  private def normalizeClaim(raw: RuntimeCertificationClaim): EngineCertificationClaim =
    val familyId = CertificationId(raw.familyId)
    require(
      CertificationScopeContract.activeCertificationFamilyIds.contains(familyId),
      s"Engine runtime intake rejects inactive certification family: ${raw.familyId}"
    )
    EngineCertificationClaim(
      familyId = familyId,
      owner = parseColor(raw.owner),
      purposes = raw.purposes.map: (purpose, strength) =>
        CertificationEvidencePurpose.fromKey(purpose).getOrElse:
          throw IllegalArgumentException(s"Unknown engine evidence purpose: $purpose")
        ->
          CertificationEvidenceStrength.fromKey(strength).getOrElse:
            throw IllegalArgumentException(s"Unknown engine evidence strength: $strength"),
      anchor = normalizeAnchor(raw.anchor),
      minDepth = raw.minDepth,
      minMultiPv = raw.minMultiPv,
      minPvPlies = raw.minPvPlies,
      requiredScore = raw.requiredScore.map(normalizeRequirement)
    )

  private def normalizeScore(score: RuntimeScore): EngineScore =
    score match
      case RuntimeScore.Centipawns(cp) => EngineScore.Centipawns(cp)
      case RuntimeScore.MateIn(plies) => EngineScore.MateIn(plies)

  private def normalizePerspective(perspective: RuntimeScorePerspective): EngineScorePerspective =
    perspective match
      case RuntimeScorePerspective.SideToMove => EngineScorePerspective.SideToMove
      case RuntimeScorePerspective.White => EngineScorePerspective.White
      case RuntimeScorePerspective.Black => EngineScorePerspective.Black

  private def normalizeRequirement(requirement: RuntimeScoreRequirement): EngineScoreRequirement =
    requirement match
      case RuntimeScoreRequirement.CentipawnAtLeast(cp) => EngineScoreRequirement.CentipawnAtLeast(cp)
      case RuntimeScoreRequirement.CentipawnAtMost(cp) => EngineScoreRequirement.CentipawnAtMost(cp)
      case RuntimeScoreRequirement.CentipawnSwingAtLeast(cp) => EngineScoreRequirement.CentipawnSwingAtLeast(cp)
      case RuntimeScoreRequirement.MateInAtMost(plies) => EngineScoreRequirement.MateInAtMost(plies)

  private def normalizeAnchor(anchor: RuntimeAnchor): WitnessAnchor =
    anchor match
      case RuntimeAnchor.Board => WitnessAnchor.BoardAnchor

  private def normalizePvLine(line: Vector[String]): Vector[Uci.Move] =
    line.map: uci =>
      Uci(uci) match
        case Some(move: Uci.Move) => move
        case Some(_) => throw IllegalArgumentException(s"Engine runtime intake rejects non-move UCI: $uci")
        case None => throw IllegalArgumentException(s"Engine runtime intake rejects invalid UCI: $uci")

  private def parseColor(value: String): Color =
    value.trim.toLowerCase match
      case "white" | "w" => Color.White
      case "black" | "b" => Color.Black
      case other => throw IllegalArgumentException(s"Engine runtime intake rejects unknown owner color: $other")

  private def combineForObject(
      current: StrategicObjectExtraction,
      baseEvidence: CertificationEvidenceBundle,
      engineEvidence: CertificationEvidenceBundle
  ): Either[String, CertificationEvidenceBundle] =
    if !baseEvidence.matches(current.rootState) then
      Left("Certification runtime intake rejected stale base certification evidence bundle")
    else if engineEvidence.isEmpty then Right(baseEvidence)
    else if !engineEvidence.matches(current.rootState) then
      Left("Certification runtime intake rejected stale Engine E evidence bundle")
    else
      safe(CertificationEvidenceBundle.forObjectExtraction(current, mergeClaims(baseEvidence, engineEvidence)))

  private def combineForDelta(
      delta: StrategicDeltaExtraction,
      baseEvidence: CertificationEvidenceBundle,
      engineEvidence: CertificationEvidenceBundle
  ): Either[String, CertificationEvidenceBundle] =
    if !baseEvidence.matches(delta.after.rootState) then
      Left("Certification runtime intake rejected stale base certification evidence bundle")
    else if engineEvidence.isEmpty then Right(baseEvidence)
    else if !engineEvidence.matches(delta.after.rootState) then
      Left("Certification runtime intake rejected stale Engine E evidence bundle")
    else
      safe(CertificationEvidenceBundle.forDeltaExtraction(delta, mergeClaims(baseEvidence, engineEvidence)))

  private def mergeClaims(
      baseEvidence: CertificationEvidenceBundle,
      engineEvidence: CertificationEvidenceBundle
  ): Vector[CertificationEvidenceClaim] =
    val baseKeys = baseEvidence.all.map(claimKey).toSet
    baseEvidence.all ++ engineEvidence.all.filterNot(claim => baseKeys.contains(claimKey(claim)))

  private def claimKey(
      claim: CertificationEvidenceClaim
  ): (CertificationId, Color, WitnessAnchor) =
    (claim.familyId, claim.owner, claim.anchor)

  private def safe[A](body: => A): Either[String, A] =
    try Right(body)
    catch
      case NonFatal(error) =>
        val message = Option(error.getMessage).filter(_.nonEmpty).getOrElse(error.getClass.getSimpleName)
        Left(message)
