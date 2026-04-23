package lila.commentary.certification

import chess.Color
import chess.format.{ Fen, Uci }

import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.root.RootPositionSupport
import lila.commentary.strategic.{ StrategicObjectExtraction, StrategicObjectExtractor }
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload }

final case class EngineNodeIdentity(nodeId: String, ply: Int):
  require(nodeId.trim.nonEmpty, "Engine node identity requires a non-empty node id")
  require(ply >= 0, "Engine node identity requires a non-negative ply")

final case class EngineTransitionBinding(
    beforeFen: String,
    playedMove: String,
    afterFen: String,
    beforeNode: Option[EngineNodeIdentity] = None
):
  require(beforeFen.trim.nonEmpty, "Engine transition binding requires beforeFen")
  require(playedMove.trim.nonEmpty, "Engine transition binding requires playedMove")
  require(afterFen.trim.nonEmpty, "Engine transition binding requires afterFen")

final case class EnginePacketIdentity(
    fen: String,
    node: EngineNodeIdentity,
    transition: Option[EngineTransitionBinding] = None
):
  require(fen.trim.nonEmpty, "Engine packet identity requires exact FEN")

final case class EngineSearchState(
    requestedDepth: Int,
    realizedDepth: Int,
    multiPv: Int,
    completed: Boolean,
    generatedAtEpochMs: Long,
    maxAgeMs: Long,
    engineConfigFingerprint: String
):
  require(requestedDepth > 0, "Engine search requires positive requested depth")
  require(realizedDepth >= 0, "Engine search requires non-negative realized depth")
  require(multiPv > 0, "Engine search requires positive MultiPV")
  require(generatedAtEpochMs >= 0L, "Engine search requires non-negative generation time")
  require(maxAgeMs >= 0L, "Engine search requires non-negative max age")
  require(engineConfigFingerprint.trim.nonEmpty, "Engine search requires an engine config fingerprint")

enum EngineScore:
  case Centipawns(cp: Int)
  case MateIn(plies: Int)

enum EngineScorePerspective:
  case SideToMove
  case White
  case Black

enum EngineScoreRequirement:
  case CentipawnAtLeast(cp: Int)
  case CentipawnAtMost(cp: Int)
  case CentipawnSwingAtLeast(cp: Int)
  case MateInAtMost(plies: Int)

final case class EngineCertificationClaim(
    familyId: CertificationId,
    owner: Color,
    purposes: Map[CertificationEvidencePurpose, CertificationEvidenceStrength],
    anchor: WitnessAnchor = WitnessAnchor.BoardAnchor,
    minDepth: Int,
    minMultiPv: Int,
    minPvPlies: Int,
    requiredScore: Option[EngineScoreRequirement],
    payload: WitnessPayload = WitnessPayload.empty
):
  require(purposes.nonEmpty, "Engine certification claim requires at least one purpose")
  require(minDepth > 0, "Engine certification claim requires positive minDepth")
  require(minMultiPv > 0, "Engine certification claim requires positive minMultiPv")
  require(minPvPlies > 0, "Engine certification claim requires positive minPvPlies")

final case class EngineEvidencePacket(
    identity: EnginePacketIdentity,
    search: EngineSearchState,
    score: EngineScore,
    scorePerspective: EngineScorePerspective,
    pvLines: Vector[Vector[Uci.Move]],
    claims: Vector[EngineCertificationClaim],
    baseline: Option[EngineBaselinePacket] = None
):

  def normalizedCentipawnsFor(owner: Color): Option[Int] =
    EngineScoreNormalization.centipawnsFor(score, scorePerspective, identity.fen, owner)

  def normalizedMateFor(owner: Color): Option[Int] =
    EngineScoreNormalization.mateFor(score, scorePerspective, identity.fen, owner)

  def normalizedCentipawnSwingFor(owner: Color): Option[Int] =
    (normalizedCentipawnsFor(owner), baseline.flatMap(_.normalizedCentipawnsFor(owner))) match
      case (Some(actual), Some(base)) => Some(actual - base)
      case _ => None

final case class EngineBaselinePacket(
    identity: EnginePacketIdentity,
    search: EngineSearchState,
    score: EngineScore,
    scorePerspective: EngineScorePerspective,
    pvLines: Vector[Vector[Uci.Move]]
):

  def normalizedCentipawnsFor(owner: Color): Option[Int] =
    EngineScoreNormalization.centipawnsFor(score, scorePerspective, identity.fen, owner)

private object EngineScoreNormalization:

  def centipawnsFor(
      score: EngineScore,
      perspective: EngineScorePerspective,
      fen: String,
      owner: Color
  ): Option[Int] =
    score match
      case EngineScore.Centipawns(cp) => Some(normalizeSignedValue(cp, perspective, fen, owner))
      case EngineScore.MateIn(_) => None

  def mateFor(
      score: EngineScore,
      perspective: EngineScorePerspective,
      fen: String,
      owner: Color
  ): Option[Int] =
    score match
      case EngineScore.MateIn(plies) => Some(normalizeSignedValue(plies, perspective, fen, owner))
      case EngineScore.Centipawns(_) => None

  private def normalizeSignedValue(
      value: Int,
      perspective: EngineScorePerspective,
      fen: String,
      owner: Color
  ): Int =
    perspective match
      case EngineScorePerspective.SideToMove =>
        if sideToMoveInFen(fen).contains(owner) then value else -value
      case EngineScorePerspective.White =>
        if owner.white then value else -value
      case EngineScorePerspective.Black =>
        if owner.black then value else -value

  private def sideToMoveInFen(fen: String): Option[Color] =
    fen.trim.split("\\s+").lift(1).flatMap:
      case "w" => Some(Color.White)
      case "b" => Some(Color.Black)
      case _ => None

object CertificationEngineEvidenceContract:

  private val BestDefenseMinimumMultiPv = 3

  def forObjectExtraction(
      current: StrategicObjectExtraction,
      expectedNode: EngineNodeIdentity,
      expectedFen: Fen.Full,
      packet: EngineEvidencePacket,
      nowEpochMs: Long
  ): Either[String, CertificationEngineEvidence] =
    for
      canonicalCurrent <- canonicalCurrent(current)
      _ <- validateExactFenString(packet.identity.fen, expectedFen, "Engine evidence exact FEN must match requested FEN")
      _ <- Either.cond(
        packet.identity.transition.isEmpty,
        (),
        "Object-position engine evidence must not use a transition-bound packet"
      )
      _ <- validateCurrentBinding(canonicalCurrent, packet)
      _ <- validateCommon(expectedNode, packet, nowEpochMs, canonicalCurrent)
    yield buildEvidence(canonicalCurrent, packet)

  def forDeltaExtraction(
      delta: StrategicDeltaExtraction,
      expectedNode: EngineNodeIdentity,
      expectedBeforeFen: Fen.Full,
      expectedAfterFen: Fen.Full,
      packet: EngineEvidencePacket,
      nowEpochMs: Long
  ): Either[String, CertificationEngineEvidence] =
    for
      canonicalDelta <- lila.commentary.delta.StrategicDeltaExtractor.validateCanonical(delta)
      transition <- packet.identity.transition.toRight(
        "Delta engine evidence requires beforeFen, playedMove, and afterFen binding"
      )
      _ <- validateExactFenString(
        transition.beforeFen,
        expectedBeforeFen,
        "Engine transition beforeFen must exactly match requested beforeFen"
      )
      _ <- validateExactFenString(
        transition.afterFen,
        expectedAfterFen,
        "Engine transition afterFen must exactly match requested afterFen"
      )
      _ <- validateExactFenString(
        packet.identity.fen,
        expectedAfterFen,
        "Engine evidence exact FEN must match requested afterFen"
      )
      _ <- validateTransitionBinding(canonicalDelta, transition)
      _ <- validateCurrentBinding(canonicalDelta.after, packet)
      _ <- validateCommon(expectedNode, packet, nowEpochMs, canonicalDelta.after)
    yield buildEvidence(canonicalDelta.after, packet)

  private def canonicalCurrent(
      current: StrategicObjectExtraction
  ): Either[String, StrategicObjectExtraction] =
    StrategicObjectExtractor
      .validateCanonical(current)
      .left
      .map(message => s"Engine evidence current extraction canonicalization failed: $message")

  private def validateCurrentBinding(
      current: StrategicObjectExtraction,
      packet: EngineEvidencePacket
  ): Either[String, Unit] =
    StrategicObjectExtractor
      .fromFenFailClosed(Fen.Full.clean(packet.identity.fen))
      .left
      .map(message => s"Engine evidence exact FEN failed to parse: $message")
      .flatMap: fromFen =>
        Either.cond(
          fromFen.rootState == current.rootState,
          (),
          "Engine evidence must be bound to the same exact FEN as the current extraction"
        )

  private def validateTransitionBinding(
      delta: StrategicDeltaExtraction,
      transition: EngineTransitionBinding
  ): Either[String, Unit] =
    for
      before <- StrategicObjectExtractor.fromFenFailClosed(Fen.Full.clean(transition.beforeFen))
        .left.map(message => s"Engine transition beforeFen failed to parse: $message")
      after <- StrategicObjectExtractor.fromFenFailClosed(Fen.Full.clean(transition.afterFen))
        .left.map(message => s"Engine transition afterFen failed to parse: $message")
      played <- parseMove(transition.playedMove)
      _ <- Either.cond(
        before.rootState == delta.before.rootState,
        (),
        "Engine transition beforeFen must match delta.before"
      )
      _ <- Either.cond(
        after.rootState == delta.after.rootState,
        (),
        "Engine transition afterFen must match delta.after"
      )
      _ <- Either.cond(
        played == delta.playedMove,
        (),
        "Engine transition playedMove must match the canonical delta playedMove"
      )
    yield ()

  private def validateCommon(
      expectedNode: EngineNodeIdentity,
      packet: EngineEvidencePacket,
      nowEpochMs: Long,
      current: StrategicObjectExtraction
  ): Either[String, Unit] =
    for
      _ <- Either.cond(
        packet.identity.node == expectedNode,
        (),
        "Engine evidence node identity does not match the requested node identity"
      )
      _ <- Either.cond(
        packet.search.completed,
        (),
        "Engine evidence search state is incomplete"
      )
      _ <- Either.cond(
        nowEpochMs >= packet.search.generatedAtEpochMs &&
          nowEpochMs - packet.search.generatedAtEpochMs <= packet.search.maxAgeMs,
        (),
        "Engine evidence is stale"
      )
      _ <- validateClaimSearch(packet)
      _ <- validatePvLines(current, packet)
      _ <- validateDistinctClaims(packet)
      _ <- validateScoreRequirements(packet, nowEpochMs)
    yield ()

  private def validateClaimSearch(packet: EngineEvidencePacket): Either[String, Unit] =
    packet.claims.foldLeft[Either[String, Unit]](Right(())):
      case (acc, claim) =>
        acc.flatMap: _ =>
          for
            _ <- Either.cond(
               packet.search.realizedDepth >= claim.minDepth,
               (),
               s"Engine evidence realized depth ${packet.search.realizedDepth} is below required depth ${claim.minDepth}"
             )
            requiredMultiPv = requiredMultiPvFor(claim)
            _ <- Either.cond(
              packet.search.multiPv >= requiredMultiPv && packet.pvLines.size >= requiredMultiPv,
               (),
              s"Engine evidence MultiPV ${packet.search.multiPv} is below required MultiPV ${requiredMultiPv}"
             )
            _ <- Either.cond(
              packet.pvLines.take(requiredMultiPv).forall(_.size >= claim.minPvPlies),
               (),
               s"Engine evidence has a truncated PV below required length ${claim.minPvPlies}"
             )
            _ <- validatePvBranchDiversity(packet.pvLines.take(requiredMultiPv), requiredMultiPv)
          yield ()

  private def validatePvLines(
      current: StrategicObjectExtraction,
      packet: EngineEvidencePacket
  ): Either[String, Unit] =
    RootPositionSupport
      .exactPosition(current.rootState)
      .left
      .map(message => s"Engine evidence exact position reconstruction failed: $message")
      .flatMap: position =>
        packet.pvLines.zipWithIndex.foldLeft[Either[String, Unit]](Right(())):
          case (acc, (line, index)) =>
            acc.flatMap(_ => validatePvLine(position, line, index))

  private def validatePvLine(
      start: chess.Position,
      line: Vector[Uci.Move],
      lineIndex: Int
  ): Either[String, Unit] =
    line.zipWithIndex.foldLeft[Either[String, chess.Position]](Right(start)):
      case (acc, (move, ply)) =>
        acc.flatMap: position =>
          position
            .move(move)
            .map(_.after.position)
            .left
            .map(error => s"Engine evidence illegal PV at line ${lineIndex + 1}, ply ${ply + 1}: $error")
    .map(_ => ())

  private def validatePvLinesFromFen(
      fen: String,
      pvLines: Vector[Vector[Uci.Move]]
  ): Either[String, Unit] =
    StrategicObjectExtractor
      .fromFenFailClosed(Fen.Full.clean(fen))
      .left
      .map(message => s"Engine evidence PV baseline FEN failed to parse: $message")
      .flatMap: extraction =>
        RootPositionSupport
          .exactPosition(extraction.rootState)
          .left
          .map(message => s"Engine evidence PV baseline reconstruction failed: $message")
      .flatMap: position =>
        pvLines.zipWithIndex.foldLeft[Either[String, Unit]](Right(())):
          case (acc, (line, index)) =>
            acc.flatMap(_ => validatePvLine(position, line, index))

  private def validatePvBranchDiversity(
      lines: Vector[Vector[Uci.Move]],
      requiredMultiPv: Int
  ): Either[String, Unit] =
    if requiredMultiPv <= 1 then Right(())
    else
      val firstMoves = lines.flatMap(_.headOption)
      Either.cond(
        firstMoves.distinct.size >= requiredMultiPv,
        (),
        s"Engine evidence MultiPV requires ${requiredMultiPv} distinct first moves"
      )

  private def requiredMultiPvFor(claim: EngineCertificationClaim): Int =
    if claim.purposes.contains(CertificationEvidencePurpose.BestDefenseSurvival) then
      math.max(claim.minMultiPv, BestDefenseMinimumMultiPv)
    else claim.minMultiPv

  private def validateDistinctClaims(packet: EngineEvidencePacket): Either[String, Unit] =
    val duplicates =
      packet.claims
        .groupBy(claim => (claim.familyId, claim.owner, claim.anchor))
        .collect { case ((familyId, owner, anchor), grouped) if grouped.size > 1 =>
          s"${familyId.value}:${if owner.white then "white" else "black"}:${anchor.key}"
        }
        .toVector
        .sorted
    Either.cond(
      duplicates.isEmpty,
      (),
      s"Engine evidence contains duplicate bounded claims: ${duplicates.mkString(", ")}"
    )

  private def validateScoreRequirements(
      packet: EngineEvidencePacket,
      nowEpochMs: Long
  ): Either[String, Unit] =
    packet.claims.foldLeft[Either[String, Unit]](Right(())):
      case (acc, claim) =>
        acc.flatMap: _ =>
          claim.requiredScore match
            case None => Left("Engine evidence claim requires a typed score requirement")
            case Some(requirement) => validateScoreRequirement(packet, claim, requirement, nowEpochMs)

  private def validateScoreRequirement(
      packet: EngineEvidencePacket,
      claim: EngineCertificationClaim,
      requirement: EngineScoreRequirement,
      nowEpochMs: Long
  ): Either[String, Unit] =
    requirement match
      case EngineScoreRequirement.CentipawnAtLeast(cp) =>
        packet.normalizedCentipawnsFor(claim.owner).toRight("Engine evidence requires a centipawn score").flatMap: actual =>
          Either.cond(actual >= cp, (), s"Engine evidence centipawn score $actual is below required threshold $cp")
      case EngineScoreRequirement.CentipawnAtMost(cp) =>
        packet.normalizedCentipawnsFor(claim.owner).toRight("Engine evidence requires a centipawn score").flatMap: actual =>
          Either.cond(actual <= cp, (), s"Engine evidence centipawn score $actual is above required threshold $cp")
      case EngineScoreRequirement.CentipawnSwingAtLeast(cp) =>
        for
          _ <- validateBoundBaseline(packet, claim, nowEpochMs)
          actual <- packet.normalizedCentipawnSwingFor(claim.owner).toRight(
            "Engine evidence requires a bound centipawn baseline"
          )
          _ <- Either.cond(actual >= cp, (), s"Engine evidence centipawn swing $actual is below required threshold $cp")
        yield ()
      case EngineScoreRequirement.MateInAtMost(plies) =>
        packet.normalizedMateFor(claim.owner).toRight("Engine evidence requires a mate score").flatMap: actual =>
          Either.cond(actual > 0 && actual <= plies, (), s"Engine evidence mate score $actual is outside required mate threshold $plies")

  private def validateBoundBaseline(
      packet: EngineEvidencePacket,
      claim: EngineCertificationClaim,
      nowEpochMs: Long
  ): Either[String, Unit] =
    for
      baseline <- packet.baseline.toRight("Engine evidence centipawn swing requires a bound baseline")
      transition <- packet.identity.transition.toRight(
        "Engine evidence centipawn swing requires transition-bound beforeFen, playedMove, and afterFen"
      )
      _ <- Either.cond(
        baseline.identity.transition.isEmpty,
        (),
        "Engine evidence baseline must be a before-position packet, not a transition packet"
      )
      _ <- validateBaselineNodeBinding(packet, baseline, transition)
      _ <- validateFenRootStateMatches(
        baseline.identity.fen,
        transition.beforeFen,
        "Engine evidence baseline FEN must match transition beforeFen"
      )
      _ <- validateExactFenString(
        baseline.identity.fen,
        Fen.Full.clean(transition.beforeFen),
        "Engine evidence baseline FEN must exactly match transition beforeFen"
      )
      _ <- Either.cond(
        baseline.search.completed,
        (),
        "Engine evidence baseline search state is incomplete"
      )
      _ <- Either.cond(
        nowEpochMs >= baseline.search.generatedAtEpochMs &&
          nowEpochMs - baseline.search.generatedAtEpochMs <= baseline.search.maxAgeMs,
        (),
        "Engine evidence baseline is stale"
      )
      _ <- Either.cond(
        baseline.search.engineConfigFingerprint == packet.search.engineConfigFingerprint,
        (),
        "Engine evidence baseline engine config fingerprint must match the current packet"
      )
      requiredMultiPv = requiredMultiPvFor(claim)
      _ <- Either.cond(
        baseline.search.realizedDepth >= claim.minDepth,
        (),
        s"Engine evidence baseline realized depth ${baseline.search.realizedDepth} is below required depth ${claim.minDepth}"
      )
      _ <- Either.cond(
        baseline.search.multiPv >= requiredMultiPv && baseline.pvLines.size >= requiredMultiPv,
        (),
        s"Engine evidence baseline MultiPV ${baseline.search.multiPv} is below required MultiPV ${requiredMultiPv}"
      )
      _ <- Either.cond(
        baseline.pvLines.take(requiredMultiPv).forall(_.size >= claim.minPvPlies),
        (),
        s"Engine evidence baseline has a truncated PV below required length ${claim.minPvPlies}"
      )
      _ <- validatePvBranchDiversity(baseline.pvLines.take(requiredMultiPv), requiredMultiPv)
      _ <- validatePvLinesFromFen(baseline.identity.fen, baseline.pvLines)
    yield ()

  private def validateBaselineNodeBinding(
      packet: EngineEvidencePacket,
      baseline: EngineBaselinePacket,
      transition: EngineTransitionBinding
  ): Either[String, Unit] =
    transition.beforeNode match
      case Some(expectedBeforeNode) =>
        Either.cond(
          baseline.identity.node == expectedBeforeNode,
          (),
          "Engine evidence baseline node identity must match transition beforeNode"
        )
      case None =>
        Either.cond(
          baseline.identity.node.ply + 1 == packet.identity.node.ply,
          (),
          "Engine evidence baseline ply must immediately precede the transition packet ply"
        )

  private def validateFenRootStateMatches(
      leftFen: String,
      rightFen: String,
      mismatchMessage: String
  ): Either[String, Unit] =
    for
      left <- StrategicObjectExtractor.fromFenFailClosed(Fen.Full.clean(leftFen))
        .left.map(message => s"Engine evidence baseline FEN failed to parse: $message")
      right <- StrategicObjectExtractor.fromFenFailClosed(Fen.Full.clean(rightFen))
        .left.map(message => s"Engine evidence transition FEN failed to parse: $message")
      _ <- Either.cond(left.rootState == right.rootState, (), mismatchMessage)
    yield ()

  private def validateExactFenString(
      actual: String,
      expected: Fen.Full,
      mismatchMessage: String
  ): Either[String, Unit] =
    val actualFen = Fen.Full.clean(actual)
    val expectedFen = Fen.Full.clean(expected.value)
    Either.cond(actualFen == expectedFen, (), mismatchMessage)

  private def buildEvidence(
      current: StrategicObjectExtraction,
      packet: EngineEvidencePacket
  ): CertificationEngineEvidence =
    if packet.claims.isEmpty then CertificationEngineEvidence.empty
    else
      CertificationEngineEvidence.fromBoundedContract(
        CertificationEvidenceBundle.forObjectExtraction(
          current,
          packet.claims.map: claim =>
            CertificationEvidence(
              familyId = claim.familyId,
              color = claim.owner,
              anchor = claim.anchor,
              purposeStrengths = claim.purposes,
              payload = claim.payload
            )
        )
      )

  private def parseMove(uci: String): Either[String, Uci.Move] =
    Uci(uci) match
      case Some(move: Uci.Move) => Right(move)
      case Some(_) => Left(s"Engine transition playedMove is not a UCI move: $uci")
      case None => Left(s"Engine transition playedMove is invalid: $uci")
