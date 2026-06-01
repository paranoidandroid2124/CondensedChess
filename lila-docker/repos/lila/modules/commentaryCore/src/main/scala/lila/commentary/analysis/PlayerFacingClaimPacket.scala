package lila.commentary.analysis


import chess.format.Uci
import lila.commentary.analysis.claim.*
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
private[commentary] enum PlayerFacingPacketScope:
  case MoveLocal
  case PositionLocal
  case LineScoped
  case BackendOnly

private[commentary] enum PlayerFacingSameBranchState:
  case Proven
  case Missing
  case Ambiguous

private[commentary] enum PlayerFacingClaimPersistence:
  case Stable
  case BestDefenseOnly
  case FutureOnly
  case Broken

private[commentary] enum PlayerFacingClaimFallbackMode:
  case Suppress
  case LineOnly
  case WeakMain
  case ExactFactual

private[commentary] object PlayerFacingClaimSuppressionReason:
  val AlternativeDominance = "alternative_dominance"
  val RivalStoryAlive = "rival_story_alive"
  val SameBranchMissing = "same_branch_missing"
  val SameBranchAmbiguous = "same_branch_ambiguous"
  val ScopeInflation = "scope_inflation"
  val SupportOnlyReinflation = "support_only_reinflation"
  val SameJobConversion = "same_job_conversion"
  val TradeKeyDefenderRelabel = "trade_key_defender_relabel"
  val RouteBindRelabel = "route_bind_relabel"
  val BetterEndgameInflation = "better_endgame_inflation"
  val B7Drift = "B7_drift"

private[commentary] object PlayerFacingClaimReleaseRisk:
  val MoveOrderFragility = "move_order_fragility"
  val HeavyPieceLeakage = "heavy_piece_leakage"
  val SurfaceReinflation = "surface_reinflation"
  val RouteMirage = "route_mirage"
  val RivalRelease = "rival_release"

private[commentary] sealed trait PlayerFacingExactSliceProof

private[commentary] object PlayerFacingExactSliceProof:
  final case class ExactTargetFixation(targetSquare: String) extends PlayerFacingExactSliceProof
  final case class CarlsbadFixedTarget(targetSquare: String, minoritySupport: Boolean) extends PlayerFacingExactSliceProof
  final case class TargetFocusedCoordination(
      targetSquare: String,
      supportFromSquares: List[String],
      targetPieces: List[String]
  ) extends PlayerFacingExactSliceProof
  final case class ColorComplexSqueeze(
      targetSquare: String,
      squareColor: String,
      minorPieceRole: String,
      minorPieceSquare: String
  ) extends PlayerFacingExactSliceProof
  final case class LocalFileEntryBind(file: String, entrySquare: String) extends PlayerFacingExactSliceProof
  final case class CounterplayAxisSuppression(breakToken: String) extends PlayerFacingExactSliceProof
  final case class ProphylacticRestraint(resourceToken: String) extends PlayerFacingExactSliceProof
  final case class QueenTradeShield(lineMoves: List[String]) extends PlayerFacingExactSliceProof
  final case class CentralBreakTiming(
      breakMove: String,
      breakSquare: String,
      breakToken: String
  ) extends PlayerFacingExactSliceProof

private[commentary] object PlayerFacingExactSliceProofFacts:
  final case class Path(proofSource: String, proofFamily: String):
    def matches(packet: PlayerFacingClaimPacket): Boolean =
      matches(packet.proofSource, packet.proofFamily)

    def matches(source: String, family: String): Boolean =
      normalize(source) == normalize(proofSource) &&
        normalize(family) == normalize(proofFamily)

  def expectedPath(proof: PlayerFacingExactSliceProof): Path =
    proof match
      case PlayerFacingExactSliceProof.ExactTargetFixation(_) =>
        Path(ProofSourceId.ExactTargetFixation.wireKey, proofFamily(PlanTaxonomy.PlanKind.StaticWeaknessFixation))
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(_, _) =>
        Path(ProofSourceId.CarlsbadFixedTargetProbe.wireKey, ProofFamilyId.BackwardPawnTargeting.wireKey)
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(_, _, _) =>
        Path(ProofSourceId.TargetFocusedCoordinationProbe.wireKey, ProofFamilyId.TargetFocusedCoordination.wireKey)
      case PlayerFacingExactSliceProof.ColorComplexSqueeze(_, _, _, _) =>
        Path(ProofSourceId.ColorComplexSqueezeProbe.wireKey, ProofFamilyId.ColorComplexSqueeze.wireKey)
      case PlayerFacingExactSliceProof.LocalFileEntryBind(_, _) =>
        Path(ProofSourceId.LocalFileEntryBind.wireKey, ProofFamilyId.HalfOpenFilePressure.wireKey)
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(_) =>
        Path(ProofSourceId.CounterplayAxisSuppression.wireKey, ProofFamilyId.NeutralizeKeyBreak.wireKey)
      case PlayerFacingExactSliceProof.ProphylacticRestraint(_) =>
        Path(ProofSourceId.ProphylacticMove.wireKey, ProofFamilyId.CounterplayRestraint.wireKey)
      case PlayerFacingExactSliceProof.QueenTradeShield(_) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.QueenTradeShield)
        Path(family, family)
      case PlayerFacingExactSliceProof.CentralBreakTiming(_, _, _) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.CentralBreakTiming)
        Path(family, family)

  def matchesPacket(
      packet: PlayerFacingClaimPacket,
      proof: PlayerFacingExactSliceProof
  ): Boolean =
    validShape(proof) && expectedPath(proof).matches(packet)

  def matchesPath(
      proof: PlayerFacingExactSliceProof,
      proofSource: String,
      proofFamily: String
  ): Boolean =
    validShape(proof) && expectedPath(proof).matches(proofSource, proofFamily)

  def validShape(proof: PlayerFacingExactSliceProof): Boolean =
    proof match
      case PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare) =>
        squareKey(targetSquare)
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(targetSquare, minoritySupport) =>
        Set("c6", "c3").contains(normalize(targetSquare)) &&
          minoritySupport
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(targetSquare, supportFromSquares, targetPieces) =>
        squareKey(targetSquare) &&
          supportFromSquares.map(normalize).filter(squareKey).distinct.size >= 2 &&
          targetPieces.exists(token => normalize(token).startsWith("target_"))
      case PlayerFacingExactSliceProof.ColorComplexSqueeze(targetSquare, squareColor, minorPieceRole, minorPieceSquare) =>
        squareKey(targetSquare) &&
          Set("light", "dark").contains(normalize(squareColor)) &&
          Set("bishop", "knight").contains(normalize(minorPieceRole)) &&
          squareKey(minorPieceSquare)
      case PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare) =>
        fileToken(file) &&
          squareKey(entrySquare)
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken) =>
        breakTokenShape(breakToken)
      case PlayerFacingExactSliceProof.ProphylacticRestraint(resourceTokenValue) =>
        resourceToken(resourceTokenValue)
      case PlayerFacingExactSliceProof.QueenTradeShield(lineMoves) =>
        lineMoves.size >= 2 &&
          lineMoves.forall(uciMove)
      case PlayerFacingExactSliceProof.CentralBreakTiming(breakMove, breakSquare, breakToken) =>
        uciMove(breakMove) &&
          squareKey(breakSquare) &&
          uciDestination(breakMove).contains(normalize(breakSquare)) &&
          routeToken(breakToken)

  def targetSquare(proof: PlayerFacingExactSliceProof): Option[String] =
    proof match
      case PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(targetSquare, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(targetSquare, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.ColorComplexSqueeze(targetSquare, _, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case _ => None

  def fixedTargetTerm(square: String): String =
    exactTargetTerm("fixed_target", square)

  def weakSquareTerm(square: String): String =
    exactTargetTerm("weak_square", square)

  def coordinatedTargetTerm(square: String): String =
    exactTargetTerm("coordinated_target", square)

  def targetWitnessTerm(proof: PlayerFacingExactSliceProof): Option[String] =
    targetSquare(proof).map { square =>
      proof match
        case _: PlayerFacingExactSliceProof.ColorComplexSqueeze =>
          weakSquareTerm(square)
        case _: PlayerFacingExactSliceProof.TargetFocusedCoordination =>
          coordinatedTargetTerm(square)
        case _ =>
          fixedTargetTerm(square)
    }

  def targetWitnessTermForPath(proofSource: String, square: String): String =
    normalize(proofSource) match
      case source if source == ProofSourceId.ColorComplexSqueezeProbe.wireKey =>
        weakSquareTerm(square)
      case source if source == ProofSourceId.TargetFocusedCoordinationProbe.wireKey =>
        coordinatedTargetTerm(square)
      case _ =>
        fixedTargetTerm(square)

  def localFileEntryTerm(file: String, entrySquare: String): Option[String] =
    Option.when(fileToken(file) && squareKey(entrySquare))(
      s"file-entry:${normalize(file)}:${normalize(entrySquare)}"
    )

  def localFileEntryTerms(file: String, entrySquare: String): List[String] =
    localFileEntryTerm(file, entrySquare).toList

  def coordinationSupportTerms(fromSquare: String, targetPiece: String): List[String] =
    List(
      cleanTerm("support_from", fromSquare),
      cleanTerm("target_piece", targetPiece),
      Some("coordinated_piece_pressure")
    ).flatten

  def colorComplexTerm(squareColor: String): Option[String] =
    val color = normalize(squareColor)
    Option.when(Set("light", "dark").contains(color))(s"color_complex:$color")

  def minorPieceTerm(roleName: String, square: String): Option[String] =
    val role = normalize(roleName)
    Option.when(Set("bishop", "knight").contains(role) && squareKey(square))(
      s"minor_piece:${role}_${normalize(square)}"
    )

  def attacksTerm(square: String): Option[String] =
    squareTerm("attacks", square)

  def minorPieceAttackTerm(fromSquare: String, targetSquare: String): Option[String] =
    Option.when(squareKey(fromSquare) && squareKey(targetSquare))(
      s"minor_piece_attack:${normalize(fromSquare)}-${normalize(targetSquare)}"
    )

  private def proofFamily(kind: PlanTaxonomy.PlanKind): String =
    ProofFamilyId
      .fromPlanKind(kind)
      .map(_.wireKey)
      .getOrElse(kind.id)

  private def squareKey(raw: String): Boolean =
    normalize(raw).matches("[a-h][1-8]")

  private def uciMove(raw: String): Boolean =
    normalize(raw).matches("[a-h][1-8][a-h][1-8][nbrq]?")

  private def uciDestination(raw: String): Option[String] =
    Uci(normalize(raw)).collect { case move: Uci.Move => move.dest.key }

  private def fileToken(raw: String): Boolean =
    normalize(raw).matches("[a-h](?:-file)?")

  private def routeToken(raw: String): Boolean =
    normalize(raw).matches("""(?:\.\.\.)?[a-h][1-8]-[a-h][1-8]""")

  private def breakTokenShape(raw: String): Boolean =
    normalize(raw).matches("""(?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?""")

  private def resourceToken(raw: String): Boolean =
    val token = normalize(raw)
    token.nonEmpty &&
      !token.contains("|") &&
      (
        token.matches("""(?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?""") ||
          PlanEvidenceEvaluator.isProphylacticDeniedResourceTerm(token)
      )

  private def exactTargetTerm(prefix: String, square: String): String =
    s"$prefix:${normalize(square)}"

  private def squareTerm(prefix: String, square: String): Option[String] =
    Option.when(squareKey(square))(s"$prefix:${normalize(square)}")

  private def cleanTerm(prefix: String, value: String): Option[String] =
    val token = normalize(value)
    Option.when(token.nonEmpty)(s"$prefix:$token")

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

private[commentary] final case class PlayerFacingProofPathWitness(
    ownerSeedTerms: List[String] = Nil,
    continuationTerms: List[String] = Nil,
    rivalTerms: List[String] = Nil,
    structureTransitionTerms: List[String] = Nil,
    exactSliceProof: Option[PlayerFacingExactSliceProof] = None
):
  def hasOwnerSeed: Boolean = ownerSeedTerms.nonEmpty
  def hasContinuation: Boolean = continuationTerms.nonEmpty
  def hasRivalContext: Boolean = rivalTerms.nonEmpty
  def hasStructureTransition: Boolean = structureTransitionTerms.nonEmpty
  def hasExactSlice: Boolean = exactSliceProof.nonEmpty

private[commentary] object PlayerFacingProofPathWitness:
  val empty: PlayerFacingProofPathWitness = PlayerFacingProofPathWitness()

private[commentary] final case class PlayerFacingClaimPacket(
    claimGate: PlanEvidenceEvaluator.ClaimCertification = PlanEvidenceEvaluator.ClaimCertification(),
    proofSource: String = "unowned",
    proofFamily: String = "unknown",
    scope: PlayerFacingPacketScope = PlayerFacingPacketScope.BackendOnly,
    triggerKind: String = "unknown",
    anchorTerms: List[String] = Nil,
    bestDefenseMove: Option[String] = None,
    bestDefenseBranchKey: Option[String] = None,
    sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Missing,
    persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Broken,
    rivalKind: Option[String] = None,
    proofPathWitness: PlayerFacingProofPathWitness = PlayerFacingProofPathWitness.empty,
    suppressionReasons: List[String] = Nil,
    releaseRisks: List[String] = Nil,
    fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.Suppress,
    proofTrace: ProofTrace = ProofTrace.empty
):
  def admitsStrategicTruthMode: Boolean =
    scope != PlayerFacingPacketScope.BackendOnly &&
      fallbackMode != PlayerFacingClaimFallbackMode.Suppress

  def allowsLineEvidence: Boolean =
    fallbackMode == PlayerFacingClaimFallbackMode.WeakMain ||
      fallbackMode == PlayerFacingClaimFallbackMode.LineOnly

  def allowsMoveLocalClaim: Boolean =
    scope == PlayerFacingPacketScope.MoveLocal &&
      fallbackMode == PlayerFacingClaimFallbackMode.WeakMain

private[commentary] object PlayerFacingClaimPacket:
  private val lineOnlyPilotOwners =
    Set.empty[String]
  private val lineOnlyPilotFamilies =
    Set.empty[String]

  def isLineOnlyPilot(proofSource: String, proofFamily: String): Boolean =
    lineOnlyPilotOwners.contains(proofSource) || lineOnlyPilotFamilies.contains(proofFamily)

  val empty: PlayerFacingClaimPacket = PlayerFacingClaimPacket()
