package lila.commentary.analysis


import lila.commentary.analysis.claim.*
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

private[commentary] final case class PlayerFacingProofPathWitness(
    ownerSeedTerms: List[String] = Nil,
    continuationTerms: List[String] = Nil,
    rivalTerms: List[String] = Nil,
    structureTransitionTerms: List[String] = Nil
):
  def hasOwnerSeed: Boolean = ownerSeedTerms.nonEmpty
  def hasContinuation: Boolean = continuationTerms.nonEmpty
  def hasRivalContext: Boolean = rivalTerms.nonEmpty
  def hasStructureTransition: Boolean = structureTransitionTerms.nonEmpty

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
