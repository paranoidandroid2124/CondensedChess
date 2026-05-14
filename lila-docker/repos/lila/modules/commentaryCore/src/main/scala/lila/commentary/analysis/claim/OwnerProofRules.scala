package lila.commentary.analysis.claim

import lila.commentary.analysis.*
private[commentary] enum OwnerProofStatus:
  case Releasable
  case BackendOnly
  case Deferred

private[commentary] enum OwnerProofWitness:
  case OwnerSeed
  case Continuation
  case BranchProof
  case StablePersistence
  case StructureTransition
  case NoRivalRelease
  case NoTacticalVeto
  case ExactSlice
  case ClaimOnlySurface

private[commentary] final case class OwnerProofContract(
    id: String,
    ownerFamily: String,
    theme: Option[ThemeTaxonomy.ThemeL1],
    subplan: Option[ThemeTaxonomy.SubplanId],
    acceptedSources: Set[String],
    allowedScopes: Set[PlayerFacingPacketScope],
    requiredWitnesses: Set[OwnerProofWitness],
    status: OwnerProofStatus,
    certifiedEligible: Boolean,
    supportedLocalEligible: Boolean,
    defaultFailureTaxonomy: String
):
  def authorityEligible: Boolean =
    certifiedEligible || supportedLocalEligible

  def exactOwnerPath: Boolean =
    requiredWitnesses.contains(OwnerProofWitness.BranchProof) ||
      requiredWitnesses.contains(OwnerProofWitness.StablePersistence)

  def accepts(packet: PlayerFacingClaimPacket): Boolean =
    ownerFamily == packet.ownerFamily &&
      acceptedSources.contains(packet.ownerSource) &&
      allowedScopes.contains(packet.scope)

private[commentary] final case class OwnerProofTrace(
    contractId: Option[String] = None,
    contractStatus: Option[String] = None,
    failureCodes: List[String] = Nil,
    seedWitness: Boolean = false,
    continuationWitness: Boolean = false,
    branchState: Option[String] = None,
    persistence: Option[String] = None,
    rivalStory: Option[String] = None,
    suppressionReasons: List[String] = Nil,
    releaseRisks: List[String] = Nil
):
  def summary: String =
    List(
      s"contract=${contractId.getOrElse("-")}",
      s"status=${contractStatus.getOrElse("-")}",
      s"failures=${if failureCodes.isEmpty then "none" else failureCodes.distinct.mkString("+")}",
      s"seed=$seedWitness",
      s"continuation=$continuationWitness",
      s"branch=${branchState.getOrElse("-")}",
      s"persistence=${persistence.getOrElse("-")}",
      s"rival=${rivalStory.getOrElse("-")}",
      s"suppression=${if suppressionReasons.isEmpty then "none" else suppressionReasons.distinct.mkString("+")}",
      s"risks=${if releaseRisks.isEmpty then "none" else releaseRisks.distinct.mkString("+")}"
    ).mkString(";")

private[commentary] object OwnerProofTrace:
  val empty: OwnerProofTrace = OwnerProofTrace()

private[commentary] object OwnerProofRules:

  private val ExactOwnerWitnesses =
    Set(
      OwnerProofWitness.OwnerSeed,
      OwnerProofWitness.Continuation,
      OwnerProofWitness.BranchProof,
      OwnerProofWitness.StablePersistence,
      OwnerProofWitness.NoRivalRelease,
      OwnerProofWitness.NoTacticalVeto
    )

  private val WeakOwnerWitnesses =
    Set(
      OwnerProofWitness.OwnerSeed,
      OwnerProofWitness.Continuation,
      OwnerProofWitness.NoRivalRelease,
      OwnerProofWitness.NoTacticalVeto,
      OwnerProofWitness.ClaimOnlySurface
    )

  private val DeferredWitnesses =
    Set(OwnerProofWitness.OwnerSeed, OwnerProofWitness.NoTacticalVeto)

  private val TacticalWitnesses =
    Set(OwnerProofWitness.OwnerSeed)

  private def themeContract(
      theme: ThemeTaxonomy.ThemeL1,
      status: OwnerProofStatus,
      defaultFailureTaxonomy: String
  ): OwnerProofContract =
    OwnerProofContract(
      id = s"theme:${theme.id}",
      ownerFamily = theme.id,
      theme = Some(theme),
      subplan = None,
      acceptedSources = Set(theme.id),
      allowedScopes = Set(PlayerFacingPacketScope.BackendOnly),
      requiredWitnesses =
        if theme == ThemeTaxonomy.ThemeL1.ImmediateTacticalGain then TacticalWitnesses else DeferredWitnesses,
      status = status,
      certifiedEligible = false,
      supportedLocalEligible = false,
      defaultFailureTaxonomy = defaultFailureTaxonomy
    )

  private def subplanContract(
      subplan: ThemeTaxonomy.SubplanId,
      status: OwnerProofStatus,
      acceptedSources: Set[String],
      allowedScopes: Set[PlayerFacingPacketScope],
      requiredWitnesses: Set[OwnerProofWitness],
      certifiedEligible: Boolean,
      supportedLocalEligible: Boolean,
      defaultFailureTaxonomy: String
  ): OwnerProofContract =
    OwnerProofContract(
      id = s"subplan:${subplan.id}",
      ownerFamily = subplan.id,
      theme = Some(subplan.theme),
      subplan = Some(subplan),
      acceptedSources = acceptedSources + subplan.id,
      allowedScopes = allowedScopes,
      requiredWitnesses = requiredWitnesses,
      status = status,
      certifiedEligible = certifiedEligible,
      supportedLocalEligible = supportedLocalEligible,
      defaultFailureTaxonomy = defaultFailureTaxonomy
    )

  private def customContract(
      id: String,
      ownerFamily: String,
      acceptedSources: Set[String],
      allowedScopes: Set[PlayerFacingPacketScope],
      requiredWitnesses: Set[OwnerProofWitness],
      certifiedEligible: Boolean,
      supportedLocalEligible: Boolean,
      defaultFailureTaxonomy: String
  ): OwnerProofContract =
    OwnerProofContract(
      id = s"runtime:$id",
      ownerFamily = ownerFamily,
      theme = None,
      subplan = None,
      acceptedSources = acceptedSources + ownerFamily,
      allowedScopes = allowedScopes,
      requiredWitnesses = requiredWitnesses,
      status = OwnerProofStatus.Releasable,
      certifiedEligible = certifiedEligible,
      supportedLocalEligible = supportedLocalEligible,
      defaultFailureTaxonomy = defaultFailureTaxonomy
    )

  private val ThemeContracts =
    ThemeTaxonomy.ThemeL1.ranked.map { theme =>
      if theme == ThemeTaxonomy.ThemeL1.WeaknessFixation then
        OwnerProofContract(
          id = s"theme:${theme.id}",
          ownerFamily = theme.id,
          theme = Some(theme),
          subplan = None,
          acceptedSources = Set(theme.id),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = ExactOwnerWitnesses,
          status = OwnerProofStatus.Releasable,
          certifiedEligible = true,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "certified_owner_path"
        )
      else
        val status =
          if theme == ThemeTaxonomy.ThemeL1.ImmediateTacticalGain then OwnerProofStatus.BackendOnly
          else OwnerProofStatus.Deferred
        val taxonomy =
          if theme == ThemeTaxonomy.ThemeL1.ImmediateTacticalGain then "tactical_truth_first"
          else "deferred_no_exact_owner"
        themeContract(theme, status, taxonomy)
    }

  private val SubplanContracts =
    ThemeTaxonomy.SubplanId.values.toList.map {
      case subplan @ ThemeTaxonomy.SubplanId.StaticWeaknessFixation =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.Releasable,
          acceptedSources = Set(PlayerFacingTruthModePolicy.ExactTargetFixationOwnerSource),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = ExactOwnerWitnesses + OwnerProofWitness.ExactSlice,
          certifiedEligible = true,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "position_probe_not_certified"
        )
      case subplan @ ThemeTaxonomy.SubplanId.BackwardPawnTargeting =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.Releasable,
          acceptedSources = Set(PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeOwnerSource),
          allowedScopes = Set(PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = ExactOwnerWitnesses + OwnerProofWitness.ExactSlice,
          certifiedEligible = true,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "position_probe_not_certified"
        )
      case subplan @ ThemeTaxonomy.SubplanId.MinorityAttackFixation =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.Releasable,
          acceptedSources = Set(PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeOwnerSource, "minority_attack_fixation"),
          allowedScopes = Set(PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = ExactOwnerWitnesses + OwnerProofWitness.ExactSlice,
          certifiedEligible = true,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "position_probe_not_certified"
        )
      case subplan @ ThemeTaxonomy.SubplanId.IQPInducement =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.Releasable,
          acceptedSources = Set(PlayerFacingTruthModePolicy.IQPInducementProbeOwnerSource),
          allowedScopes = Set(PlayerFacingPacketScope.PositionLocal, PlayerFacingPacketScope.MoveLocal),
          requiredWitnesses =
            WeakOwnerWitnesses +
              OwnerProofWitness.StructureTransition,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "iqp_inducement_probe_missing"
        )
      case subplan @ ThemeTaxonomy.SubplanId.SimplificationWindow =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.Releasable,
          acceptedSources = Set("classification_transformation_window", "exchange_availability_bridge", "capture_exchange_transformation", "iqp_simplification_profile", "plan_match_transformation"),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = ExactOwnerWitnesses + OwnerProofWitness.StructureTransition,
          certifiedEligible = true,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "same_job_or_conversion_relabel_blocked"
        )
      case subplan @ ThemeTaxonomy.SubplanId.DefenderTrade =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.Releasable,
          acceptedSources = Set(PlayerFacingTruthModePolicy.DefenderTradeOwnerSource, "exchange_forcing_delta"),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = WeakOwnerWitnesses + OwnerProofWitness.StructureTransition,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "attacking_piece_trade_unowned"
        )
      case subplan @ ThemeTaxonomy.SubplanId.QueenTradeShield =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.Releasable,
          acceptedSources = Set(PlayerFacingTruthModePolicy.QueenTradeShieldOwnerSource),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = WeakOwnerWitnesses + OwnerProofWitness.ExactSlice,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "source_queen_trade_boundary"
        )
      case subplan @ ThemeTaxonomy.SubplanId.BadPieceLiquidation =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.Releasable,
          acceptedSources = Set("bad_piece_liquidation"),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = WeakOwnerWitnesses,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "attacking_piece_trade_unowned"
        )
      case subplan if subplan.theme == ThemeTaxonomy.ThemeL1.ImmediateTacticalGain =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.BackendOnly,
          acceptedSources = Set(subplan.id),
          allowedScopes = Set(PlayerFacingPacketScope.BackendOnly),
          requiredWitnesses = TacticalWitnesses,
          certifiedEligible = false,
          supportedLocalEligible = false,
          defaultFailureTaxonomy = "tactical_truth_first"
        )
      case subplan =>
        subplanContract(
          subplan = subplan,
          status = OwnerProofStatus.Deferred,
          acceptedSources = Set(subplan.id),
          allowedScopes = Set(PlayerFacingPacketScope.BackendOnly, PlayerFacingPacketScope.LineScoped),
          requiredWitnesses = DeferredWitnesses,
          certifiedEligible = false,
          supportedLocalEligible = false,
          defaultFailureTaxonomy = "deferred_no_exact_owner"
        )
    }

  private val RuntimeContracts =
    List(
      customContract(
        id = "half_open_file_pressure",
        ownerFamily = "half_open_file_pressure",
        acceptedSources = Set("local_file_entry_bind", ThemeTaxonomy.SubplanId.OpenFilePressure.id, ThemeTaxonomy.SubplanId.RookFileTransfer.id),
        allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
        requiredWitnesses = ExactOwnerWitnesses + OwnerProofWitness.ExactSlice,
        certifiedEligible = true,
        supportedLocalEligible = true,
        defaultFailureTaxonomy = "certified_owner_path"
      ),
      customContract(
        id = "neutralize_key_break",
        ownerFamily = "neutralize_key_break",
        acceptedSources = Set("counterplay_axis_suppression", ThemeTaxonomy.SubplanId.BreakPrevention.id),
        allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
        requiredWitnesses = ExactOwnerWitnesses + OwnerProofWitness.ExactSlice,
        certifiedEligible = true,
        supportedLocalEligible = true,
        defaultFailureTaxonomy = "certified_owner_path"
      ),
      customContract(
        id = "counterplay_restraint",
        ownerFamily = "counterplay_restraint",
        acceptedSources = Set("prophylactic_move", ThemeTaxonomy.SubplanId.ProphylaxisRestraint.id),
        allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
        requiredWitnesses = ExactOwnerWitnesses + OwnerProofWitness.ExactSlice,
        certifiedEligible = true,
        supportedLocalEligible = true,
        defaultFailureTaxonomy = "certified_owner_path"
      ),
      customContract(
        id = "trade_key_defender",
        ownerFamily = "trade_key_defender",
        acceptedSources = Set("exchange_forcing_delta", ThemeTaxonomy.SubplanId.DefenderTrade.id),
        allowedScopes = Set(PlayerFacingPacketScope.MoveLocal),
        requiredWitnesses = ExactOwnerWitnesses + OwnerProofWitness.StructureTransition,
        certifiedEligible = true,
        supportedLocalEligible = false,
        defaultFailureTaxonomy = "attacking_piece_trade_unowned"
      ),
      customContract(
        id = PlayerFacingTruthModePolicy.TargetFocusedCoordinationOwnerFamily,
        ownerFamily = PlayerFacingTruthModePolicy.TargetFocusedCoordinationOwnerFamily,
        acceptedSources = Set(PlayerFacingTruthModePolicy.TargetFocusedCoordinationOwnerSource),
        allowedScopes = Set(PlayerFacingPacketScope.PositionLocal),
        requiredWitnesses = ExactOwnerWitnesses + OwnerProofWitness.ExactSlice,
        certifiedEligible = true,
        supportedLocalEligible = true,
        defaultFailureTaxonomy = "certified_owner_path"
      )
    )

  val contracts: List[OwnerProofContract] =
    (RuntimeContracts ++ SubplanContracts ++ ThemeContracts)

  private val byOwnerFamily: Map[String, OwnerProofContract] =
    contracts
      .groupBy(contract => normalize(contract.ownerFamily))
      .view
      .mapValues(_.head)
      .toMap

  def contractForFamily(ownerFamily: String): Option[OwnerProofContract] =
    byOwnerFamily.get(normalize(ownerFamily))

  def contractForPacket(packet: PlayerFacingClaimPacket): Option[OwnerProofContract] =
    contractForFamily(packet.ownerFamily)

  def supportsPositionProbeFamily(ownerFamily: String): Boolean =
    contractForFamily(ownerFamily).exists(contract =>
      contract.authorityEligible && contract.allowedScopes.contains(PlayerFacingPacketScope.PositionLocal)
    )

  def supportsMoveDeltaFamily(ownerFamily: String): Boolean =
    contractForFamily(ownerFamily).exists(contract =>
      contract.authorityEligible && contract.allowedScopes.contains(PlayerFacingPacketScope.MoveLocal)
    )

  def exactOwnerPathFamily(ownerFamily: String): Boolean =
    contractForFamily(ownerFamily).exists(contract => contract.certifiedEligible && contract.exactOwnerPath)

  def certifiedEligible(ownerFamily: String): Boolean =
    contractForFamily(ownerFamily).exists(_.certifiedEligible)

  def supportedLocalEligible(ownerFamily: String): Boolean =
    contractForFamily(ownerFamily).exists(_.supportedLocalEligible)

  def traceFor(packet: PlayerFacingClaimPacket): OwnerProofTrace =
    val contract = contractForPacket(packet)
    OwnerProofTrace(
      contractId = contract.map(_.id),
      contractStatus = contract.map(_.status.toString),
      failureCodes = failureCodes(packet, contract),
      seedWitness = packet.ownerPathWitness.hasOwnerSeed,
      continuationWitness = packet.ownerPathWitness.hasContinuation,
      branchState = Some(packet.sameBranchState.toString),
      persistence = Some(packet.persistence.toString),
      rivalStory = packet.rivalKind,
      suppressionReasons = packet.suppressionReasons,
      releaseRisks = packet.releaseRisks
    )

  def attachTrace(packet: PlayerFacingClaimPacket): PlayerFacingClaimPacket =
    packet.copy(ownerProofTrace = traceFor(packet))

  def failureCodes(
      packet: PlayerFacingClaimPacket,
      contract: Option[OwnerProofContract] = None
  ): List[String] =
    val resolved = contract.orElse(contractForPacket(packet))
    val contractFailures =
      resolved match
        case None =>
          List("contract:missing")
        case Some(c) =>
          List(
            Option.when(c.status == OwnerProofStatus.Deferred)("contract:deferred_no_exact_owner"),
            Option.when(c.status == OwnerProofStatus.BackendOnly)("contract:backend_only"),
            Option.when(!c.allowedScopes.contains(packet.scope))("contract:scope_not_allowed"),
            Option.when(!c.acceptedSources.contains(packet.ownerSource))("contract:source_not_accepted"),
            Option.when(c.requiredWitnesses.contains(OwnerProofWitness.OwnerSeed) && !packet.ownerPathWitness.hasOwnerSeed)(
              "witness:owner_seed_missing"
            ),
            Option.when(c.requiredWitnesses.contains(OwnerProofWitness.Continuation) && !packet.ownerPathWitness.hasContinuation)(
              "witness:continuation_missing"
            ),
            Option.when(c.requiredWitnesses.contains(OwnerProofWitness.BranchProof) && packet.sameBranchState != PlayerFacingSameBranchState.Proven)(
              "witness:branch_not_proven"
            ),
            Option.when(c.requiredWitnesses.contains(OwnerProofWitness.StablePersistence) && packet.persistence != PlayerFacingClaimPersistence.Stable)(
              "witness:persistence_not_stable"
            ),
            Option.when(c.requiredWitnesses.contains(OwnerProofWitness.StructureTransition) && !packet.ownerPathWitness.hasStructureTransition)(
              "witness:structure_transition_missing"
            ),
            Option.when(
              c.requiredWitnesses.contains(OwnerProofWitness.NoRivalRelease) &&
                (
                  packet.suppressionReasons.contains(PlayerFacingClaimSuppressionReason.RivalStoryAlive) ||
                    packet.releaseRisks.contains(PlayerFacingClaimReleaseRisk.RivalRelease)
                )
            )(
              "rival:release_risk"
            )
          ).flatten
    (
      contractFailures ++
        packet.suppressionReasons.map(reason => s"suppression:$reason") ++
        packet.releaseRisks.map(risk => s"risk:$risk")
    ).distinct

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
