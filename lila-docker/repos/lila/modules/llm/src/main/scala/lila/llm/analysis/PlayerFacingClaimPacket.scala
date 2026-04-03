package lila.llm.analysis

private[llm] enum PlayerFacingPacketScope:
  case MoveLocal
  case LineScoped
  case BackendOnly

private[llm] enum PlayerFacingSameBranchState:
  case Proven
  case Missing
  case Ambiguous

private[llm] enum PlayerFacingClaimPersistence:
  case Stable
  case BestDefenseOnly
  case FutureOnly
  case Broken

private[llm] enum PlayerFacingClaimFallbackMode:
  case Suppress
  case LineOnly
  case WeakMain
  case ExactFactual

private[llm] object PlayerFacingClaimSuppressionReason:
  val AlternativeDominance = "alternative_dominance"
  val RivalStoryAlive = "rival_story_alive"
  val SameBranchMissing = "same_branch_missing"
  val SameBranchAmbiguous = "same_branch_ambiguous"
  val ScopeInflation = "scope_inflation"
  val SupportOnlyReinflation = "support_only_reinflation"

private[llm] object PlayerFacingClaimReleaseRisk:
  val MoveOrderFragility = "move_order_fragility"
  val HeavyPieceLeakage = "heavy_piece_leakage"
  val SurfaceReinflation = "surface_reinflation"
  val RouteMirage = "route_mirage"
  val RivalRelease = "rival_release"

private[llm] final case class PlayerFacingClaimPacket(
    claimGate: PlanEvidenceEvaluator.ClaimCertification = PlanEvidenceEvaluator.ClaimCertification(),
    ownerSource: String = "unowned",
    ownerFamily: String = "unknown",
    scope: PlayerFacingPacketScope = PlayerFacingPacketScope.BackendOnly,
    triggerKind: String = "unknown",
    anchorTerms: List[String] = Nil,
    bestDefenseMove: Option[String] = None,
    bestDefenseBranchKey: Option[String] = None,
    sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Missing,
    persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Broken,
    rivalKind: Option[String] = None,
    suppressionReasons: List[String] = Nil,
    releaseRisks: List[String] = Nil,
    fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.Suppress
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

private[llm] object PlayerFacingClaimPacket:
  private val lineOnlyPilotOwners =
    Set("local_file_entry_bind", "counterplay_axis_suppression")
  private val lineOnlyPilotFamilies =
    Set("half_open_file_pressure", "neutralize_key_break")

  def isLineOnlyPilot(ownerSource: String, ownerFamily: String): Boolean =
    lineOnlyPilotOwners.contains(ownerSource) || lineOnlyPilotFamilies.contains(ownerFamily)

  val empty: PlayerFacingClaimPacket = PlayerFacingClaimPacket()
