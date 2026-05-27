package lila.commentary.analysis.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.semantic.StrategicObservationIds.{ EvidenceSourceId, ProofFamilyId, ProofSourceId }
import chess.format.Uci
private[commentary] enum ProofContractStatus:
  case Releasable
  case BackendOnly
  case Deferred

private[commentary] enum ProofWitness:
  case OwnerSeed
  case Continuation
  case BranchProof
  case StablePersistence
  case StructureTransition
  case NoRivalRelease
  case NoTacticalVeto
  case ExactSlice
  case ClaimOnlySurface

private[commentary] final case class ProofContract(
    id: String,
    proofFamily: String,
    theme: Option[PlanTaxonomy.PlanTheme],
    subplan: Option[PlanTaxonomy.PlanKind],
    acceptedSources: Set[String],
    allowedScopes: Set[PlayerFacingPacketScope],
    requiredWitnesses: Set[ProofWitness],
    status: ProofContractStatus,
    certifiedEligible: Boolean,
    supportedLocalEligible: Boolean,
    defaultFailureTaxonomy: String
):
  def authorityEligible: Boolean =
    certifiedEligible || supportedLocalEligible

  def exactProofPath: Boolean =
    requiredWitnesses.contains(ProofWitness.BranchProof) ||
      requiredWitnesses.contains(ProofWitness.StablePersistence)

  def accepts(packet: PlayerFacingClaimPacket): Boolean =
    proofFamily == packet.proofFamily &&
      acceptedSources.contains(packet.proofSource) &&
      allowedScopes.contains(packet.scope)

private[commentary] final case class ProofTrace(
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

private[commentary] object ProofTrace:
  val empty: ProofTrace = ProofTrace()

private[commentary] object ProofContractRules:

  private val ExactOwnerWitnesses =
    Set(
      ProofWitness.OwnerSeed,
      ProofWitness.Continuation,
      ProofWitness.BranchProof,
      ProofWitness.StablePersistence,
      ProofWitness.NoRivalRelease,
      ProofWitness.NoTacticalVeto
    )

  private val WeakOwnerWitnesses =
    Set(
      ProofWitness.OwnerSeed,
      ProofWitness.Continuation,
      ProofWitness.NoRivalRelease,
      ProofWitness.NoTacticalVeto,
      ProofWitness.ClaimOnlySurface
    )

  private val DeferredWitnesses =
    Set(ProofWitness.OwnerSeed, ProofWitness.NoTacticalVeto)

  private val TacticalWitnesses =
    Set(ProofWitness.OwnerSeed)

  private def proofFamily(theme: PlanTaxonomy.PlanTheme): ProofFamilyId =
    ProofFamilyId.fromPlanTheme(theme).getOrElse(sys.error(s"missing proof family registry for theme ${theme.id}"))

  private def proofFamily(subplan: PlanTaxonomy.PlanKind): ProofFamilyId =
    ProofFamilyId.fromPlanKind(subplan).getOrElse(sys.error(s"missing proof family registry for subplan ${subplan.id}"))

  private def themeContract(
      theme: PlanTaxonomy.PlanTheme,
      status: ProofContractStatus,
      defaultFailureTaxonomy: String
  ): ProofContract =
    val family = proofFamily(theme)
    ProofContract(
      id = s"theme:${theme.id}",
      proofFamily = family.wireKey,
      theme = Some(theme),
      subplan = None,
      acceptedSources = Set(family.wireKey),
      allowedScopes = Set(PlayerFacingPacketScope.BackendOnly),
      requiredWitnesses =
        if theme == PlanTaxonomy.PlanTheme.ImmediateTacticalGain then TacticalWitnesses else DeferredWitnesses,
      status = status,
      certifiedEligible = false,
      supportedLocalEligible = false,
      defaultFailureTaxonomy = defaultFailureTaxonomy
    )

  private def subplanContract(
      subplan: PlanTaxonomy.PlanKind,
      status: ProofContractStatus,
      acceptedSources: Set[ProofSourceId],
      acceptedSourceFamilies: Set[ProofFamilyId] = Set.empty,
      acceptedSelectorSources: Set[EvidenceSourceId] = Set.empty,
      allowedScopes: Set[PlayerFacingPacketScope],
      requiredWitnesses: Set[ProofWitness],
      certifiedEligible: Boolean,
      supportedLocalEligible: Boolean,
      defaultFailureTaxonomy: String,
      includeSubplanSource: Boolean = true
  ): ProofContract =
    val family = proofFamily(subplan)
    val acceptedSourceWires =
      acceptedSources.map(_.wireKey) ++
        acceptedSourceFamilies.map(_.wireKey) ++
        acceptedSelectorSources.map(_.wireKey) ++
        Option.when(includeSubplanSource)(family.wireKey)
    ProofContract(
      id = s"subplan:${subplan.id}",
      proofFamily = family.wireKey,
      theme = Some(subplan.theme),
      subplan = Some(subplan),
      acceptedSources = acceptedSourceWires,
      allowedScopes = allowedScopes,
      requiredWitnesses = requiredWitnesses,
      status = status,
      certifiedEligible = certifiedEligible,
      supportedLocalEligible = supportedLocalEligible,
      defaultFailureTaxonomy = defaultFailureTaxonomy
    )

  private def customContract(
      id: ProofFamilyId,
      acceptedSources: Set[ProofSourceId],
      acceptedSourceFamilies: Set[ProofFamilyId] = Set.empty,
      acceptedSelectorSources: Set[EvidenceSourceId] = Set.empty,
      allowedScopes: Set[PlayerFacingPacketScope],
      requiredWitnesses: Set[ProofWitness],
      certifiedEligible: Boolean,
      supportedLocalEligible: Boolean,
      defaultFailureTaxonomy: String
  ): ProofContract =
    val acceptedSourceWires =
      acceptedSources.map(_.wireKey) ++
        acceptedSourceFamilies.map(_.wireKey) ++
        acceptedSelectorSources.map(_.wireKey) +
        id.wireKey
    ProofContract(
      id = s"runtime:${id.wireKey}",
      proofFamily = id.wireKey,
      theme = None,
      subplan = None,
      acceptedSources = acceptedSourceWires,
      allowedScopes = allowedScopes,
      requiredWitnesses = requiredWitnesses,
      status = ProofContractStatus.Releasable,
      certifiedEligible = certifiedEligible,
      supportedLocalEligible = supportedLocalEligible,
      defaultFailureTaxonomy = defaultFailureTaxonomy
    )

  private val ThemeContracts =
    PlanTaxonomy.PlanTheme.ranked.map { theme =>
      if theme == PlanTaxonomy.PlanTheme.WeaknessFixation then
        ProofContract(
          id = s"theme:${theme.id}",
          proofFamily = proofFamily(theme).wireKey,
          theme = Some(theme),
          subplan = None,
          acceptedSources = Set(proofFamily(theme).wireKey),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = ExactOwnerWitnesses,
          status = ProofContractStatus.Releasable,
          certifiedEligible = true,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "certified_owner_path"
        )
      else
        val status =
          if theme == PlanTaxonomy.PlanTheme.ImmediateTacticalGain then ProofContractStatus.BackendOnly
          else ProofContractStatus.Deferred
        val taxonomy =
          if theme == PlanTaxonomy.PlanTheme.ImmediateTacticalGain then "tactical_truth_first"
          else "deferred_no_exact_owner"
        themeContract(theme, status, taxonomy)
    }

  private val SubplanContracts =
    PlanTaxonomy.PlanKind.values.toList.map {
      case subplan @ PlanTaxonomy.PlanKind.StaticWeaknessFixation =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Releasable,
          acceptedSources = Set(ProofSourceId.ExactTargetFixation),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = ExactOwnerWitnesses + ProofWitness.ExactSlice,
          certifiedEligible = true,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "position_probe_not_certified"
        )
      case subplan @ PlanTaxonomy.PlanKind.BackwardPawnTargeting =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Releasable,
          acceptedSources = Set(ProofSourceId.CarlsbadFixedTargetProbe),
          allowedScopes = Set(PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = ExactOwnerWitnesses + ProofWitness.ExactSlice,
          certifiedEligible = true,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "position_probe_not_certified"
        )
      case subplan @ PlanTaxonomy.PlanKind.MinorityAttackFixation =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Deferred,
          acceptedSources = Set.empty,
          allowedScopes = Set(PlayerFacingPacketScope.BackendOnly, PlayerFacingPacketScope.LineScoped),
          requiredWitnesses = DeferredWitnesses,
          certifiedEligible = false,
          supportedLocalEligible = false,
          defaultFailureTaxonomy = "deferred_no_exact_owner",
          includeSubplanSource = false
        )
      case subplan @ PlanTaxonomy.PlanKind.IQPInducement =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Releasable,
          acceptedSources = Set(ProofSourceId.IQPInducementProbe),
          allowedScopes = Set(PlayerFacingPacketScope.PositionLocal, PlayerFacingPacketScope.MoveLocal),
          requiredWitnesses =
            WeakOwnerWitnesses +
              ProofWitness.StructureTransition,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "iqp_inducement_probe_missing"
        )
      case subplan @ PlanTaxonomy.PlanKind.SimplificationWindow =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Releasable,
          acceptedSources = Set.empty,
          acceptedSelectorSources =
            Set(
              EvidenceSourceId.ClassificationTransformationWindow,
              EvidenceSourceId.ExchangeAvailabilityBridge,
              EvidenceSourceId.CaptureExchangeTransformation,
              EvidenceSourceId.IqpSimplificationProfile,
              EvidenceSourceId.PlanMatchTransformation
            ),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = ExactOwnerWitnesses + ProofWitness.StructureTransition,
          certifiedEligible = true,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "same_job_or_conversion_relabel_blocked"
        )
      case subplan @ PlanTaxonomy.PlanKind.DefenderTrade =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Releasable,
          acceptedSources = Set(ProofSourceId.ExchangeForcingDelta),
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = WeakOwnerWitnesses + ProofWitness.StructureTransition,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "attacking_piece_trade_unowned"
        )
      case subplan @ PlanTaxonomy.PlanKind.QueenTradeShield =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Releasable,
          acceptedSources = Set.empty,
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = WeakOwnerWitnesses + ProofWitness.ExactSlice,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "source_queen_trade_boundary"
        )
      case subplan @ PlanTaxonomy.PlanKind.CentralBreakTiming =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Releasable,
          acceptedSources = Set.empty,
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal),
          requiredWitnesses =
            WeakOwnerWitnesses +
              ProofWitness.ExactSlice,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "central_break_timing_witness_missing"
        )
      case subplan @ PlanTaxonomy.PlanKind.BadPieceLiquidation =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Releasable,
          acceptedSources = Set.empty,
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = WeakOwnerWitnesses,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "attacking_piece_trade_unowned"
        )
      case subplan @ PlanTaxonomy.PlanKind.OutpostEntrenchment =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Releasable,
          acceptedSources = Set.empty,
          allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
          requiredWitnesses = WeakOwnerWitnesses,
          certifiedEligible = false,
          supportedLocalEligible = true,
          defaultFailureTaxonomy = "outpost_entrenchment_witness_missing"
        )
      case subplan if subplan.theme == PlanTaxonomy.PlanTheme.ImmediateTacticalGain =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.BackendOnly,
          acceptedSources = Set.empty,
          allowedScopes = Set(PlayerFacingPacketScope.BackendOnly),
          requiredWitnesses = TacticalWitnesses,
          certifiedEligible = false,
          supportedLocalEligible = false,
          defaultFailureTaxonomy = "tactical_truth_first"
        )
      case subplan =>
        subplanContract(
          subplan = subplan,
          status = ProofContractStatus.Deferred,
          acceptedSources = Set.empty,
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
        id = ProofFamilyId.HalfOpenFilePressure,
        acceptedSources = Set(ProofSourceId.LocalFileEntryBind),
        acceptedSourceFamilies =
          Set(
            proofFamily(PlanTaxonomy.PlanKind.OpenFilePressure),
            proofFamily(PlanTaxonomy.PlanKind.RookFileTransfer)
          ),
        allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
        requiredWitnesses = ExactOwnerWitnesses + ProofWitness.ExactSlice,
        certifiedEligible = true,
        supportedLocalEligible = true,
        defaultFailureTaxonomy = "certified_owner_path"
      ),
      customContract(
        id = ProofFamilyId.NeutralizeKeyBreak,
        acceptedSources = Set(ProofSourceId.CounterplayAxisSuppression),
        acceptedSourceFamilies = Set(proofFamily(PlanTaxonomy.PlanKind.BreakPrevention)),
        allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
        requiredWitnesses = ExactOwnerWitnesses + ProofWitness.ExactSlice,
        certifiedEligible = true,
        supportedLocalEligible = true,
        defaultFailureTaxonomy = "certified_owner_path"
      ),
      customContract(
        id = ProofFamilyId.CounterplayRestraint,
        acceptedSources = Set(ProofSourceId.ProphylacticMove),
        acceptedSourceFamilies = Set(proofFamily(PlanTaxonomy.PlanKind.ProphylaxisRestraint)),
        allowedScopes = Set(PlayerFacingPacketScope.MoveLocal, PlayerFacingPacketScope.PositionLocal),
        requiredWitnesses = ExactOwnerWitnesses + ProofWitness.ExactSlice,
        certifiedEligible = true,
        supportedLocalEligible = true,
        defaultFailureTaxonomy = "certified_owner_path"
      ),
      customContract(
        id = ProofFamilyId.TradeKeyDefender,
        acceptedSources = Set(ProofSourceId.ExchangeForcingDelta),
        acceptedSourceFamilies = Set(proofFamily(PlanTaxonomy.PlanKind.DefenderTrade)),
        allowedScopes = Set(PlayerFacingPacketScope.MoveLocal),
        requiredWitnesses = ExactOwnerWitnesses + ProofWitness.StructureTransition,
        certifiedEligible = true,
        supportedLocalEligible = false,
        defaultFailureTaxonomy = "attacking_piece_trade_unowned"
      ),
      customContract(
        id = ProofFamilyId.TargetFocusedCoordination,
        acceptedSources = Set(ProofSourceId.TargetFocusedCoordinationProbe),
        allowedScopes = Set(PlayerFacingPacketScope.PositionLocal),
        requiredWitnesses = ExactOwnerWitnesses + ProofWitness.ExactSlice,
        certifiedEligible = true,
        supportedLocalEligible = true,
        defaultFailureTaxonomy = "certified_owner_path"
      ),
      ProofContract(
        id = s"runtime:${ProofFamilyId.ColorComplexSqueeze.wireKey}",
        proofFamily = ProofFamilyId.ColorComplexSqueeze.wireKey,
        theme = None,
        subplan = None,
        acceptedSources = Set(ProofSourceId.ColorComplexSqueezeProbe.wireKey),
        allowedScopes = Set(PlayerFacingPacketScope.PositionLocal),
        requiredWitnesses = ExactOwnerWitnesses + ProofWitness.ExactSlice,
        status = ProofContractStatus.Releasable,
        certifiedEligible = true,
        supportedLocalEligible = true,
        defaultFailureTaxonomy = "color_complex_authority_closed"
      )
    )

  val contracts: List[ProofContract] =
    (RuntimeContracts ++ SubplanContracts ++ ThemeContracts)

  private val byProofFamily: Map[String, ProofContract] =
    contracts
      .groupBy(contract => normalize(contract.proofFamily))
      .view
      .mapValues(_.head)
      .toMap

  def contractForProofFamily(proofFamily: String): Option[ProofContract] =
    byProofFamily.get(normalize(proofFamily))

  def contractForPacket(packet: PlayerFacingClaimPacket): Option[ProofContract] =
    contractForProofFamily(packet.proofFamily)

  def supportsPositionProbeProofFamily(proofFamily: String): Boolean =
    contractForProofFamily(proofFamily).exists(contract =>
      contract.authorityEligible && contract.allowedScopes.contains(PlayerFacingPacketScope.PositionLocal)
    )

  def supportsMoveDeltaProofFamily(proofFamily: String): Boolean =
    contractForProofFamily(proofFamily).exists(contract =>
      contract.authorityEligible && contract.allowedScopes.contains(PlayerFacingPacketScope.MoveLocal)
    )

  def exactProofFamily(proofFamily: String): Boolean =
    contractForProofFamily(proofFamily).exists(contract => contract.certifiedEligible && contract.exactProofPath)

  def certifiedEligible(proofFamily: String): Boolean =
    contractForProofFamily(proofFamily).exists(_.certifiedEligible)

  def supportedLocalEligible(proofFamily: String): Boolean =
    contractForProofFamily(proofFamily).exists(_.supportedLocalEligible)

  def certifiedOwnerAdmissible(packet: PlayerFacingClaimPacket): Boolean =
    contractForPacket(packet).exists(contract =>
      contract.certifiedEligible &&
        contract.accepts(packet) &&
        failureCodes(packet, Some(contract)).isEmpty
    )

  def supportedLocalAdmissible(packet: PlayerFacingClaimPacket): Boolean =
    contractForPacket(packet).exists(contract =>
      contract.supportedLocalEligible &&
        contract.accepts(packet) &&
        failureCodes(packet, Some(contract)).isEmpty
    )

  def traceFor(packet: PlayerFacingClaimPacket): ProofTrace =
    val contract = contractForPacket(packet)
    ProofTrace(
      contractId = contract.map(_.id),
      contractStatus = contract.map(_.status.toString),
      failureCodes = failureCodes(packet, contract),
      seedWitness = packet.proofPathWitness.hasOwnerSeed,
      continuationWitness = packet.proofPathWitness.hasContinuation,
      branchState = Some(packet.sameBranchState.toString),
      persistence = Some(packet.persistence.toString),
      rivalStory = packet.rivalKind,
      suppressionReasons = packet.suppressionReasons,
      releaseRisks = packet.releaseRisks
    )

  def attachTrace(packet: PlayerFacingClaimPacket): PlayerFacingClaimPacket =
    packet.copy(proofTrace = traceFor(packet))

  def failureCodes(
      packet: PlayerFacingClaimPacket,
      contract: Option[ProofContract] = None
  ): List[String] =
    val resolved = contract.orElse(contractForPacket(packet))
    val contractFailures =
      resolved match
        case None =>
          List("contract:missing")
        case Some(c) =>
          List(
            Option.when(c.status == ProofContractStatus.Deferred)("contract:deferred_no_exact_owner"),
            Option.when(c.status == ProofContractStatus.BackendOnly)("contract:backend_only"),
            Option.when(!c.allowedScopes.contains(packet.scope))("contract:scope_not_allowed"),
            Option.when(!c.acceptedSources.contains(packet.proofSource))("contract:source_not_accepted")
          ).flatten ++ verifyContractWitness(c, packet)
    (
      contractFailures ++
        packet.suppressionReasons.map(reason => s"suppression:$reason") ++
        packet.releaseRisks.map(risk => s"risk:$risk")
    ).distinct

  private def verifyContractWitness(
      contract: ProofContract,
      packet: PlayerFacingClaimPacket
  ): List[String] =
    val required = contract.requiredWitnesses
    List(
      Option.when(required.contains(ProofWitness.OwnerSeed) && !packet.proofPathWitness.hasOwnerSeed)(
        "witness:owner_seed_missing"
      ),
      Option.when(required.contains(ProofWitness.Continuation) && !packet.proofPathWitness.hasContinuation)(
        "witness:continuation_missing"
      ),
      Option.when(required.contains(ProofWitness.BranchProof) && packet.sameBranchState != PlayerFacingSameBranchState.Proven)(
        "witness:branch_not_proven"
      ),
      Option.when(required.contains(ProofWitness.StablePersistence) && packet.persistence != PlayerFacingClaimPersistence.Stable)(
        "witness:persistence_not_stable"
      ),
      Option.when(required.contains(ProofWitness.StructureTransition) && !packet.proofPathWitness.hasStructureTransition)(
        "witness:structure_transition_missing"
      ),
      Option.when(required.contains(ProofWitness.ExactSlice) && !exactSliceWitnessPresent(packet))(
        "witness:exact_slice_missing"
      ),
      Option.when(required.contains(ProofWitness.NoRivalRelease) && rivalReleasePresent(packet))(
        "rival:release_risk"
      ),
      Option.when(required.contains(ProofWitness.NoTacticalVeto) && tacticalVetoPresent(packet))(
        "witness:tactical_veto_present"
      ),
      Option.when(required.contains(ProofWitness.ClaimOnlySurface) && !claimOnlySurfacePresent(packet))(
        "witness:claim_only_surface_missing"
      )
    ).flatten

  private def rivalReleasePresent(packet: PlayerFacingClaimPacket): Boolean =
    packet.suppressionReasons.contains(PlayerFacingClaimSuppressionReason.RivalStoryAlive) ||
      packet.releaseRisks.contains(PlayerFacingClaimReleaseRisk.RivalRelease)

  private def tacticalVetoPresent(packet: PlayerFacingClaimPacket): Boolean =
    (packet.suppressionReasons ++ packet.releaseRisks).exists(isTacticalVetoCode)

  private def isTacticalVetoCode(raw: String): Boolean =
    val code = normalize(raw)
    code == "truth_contract_blunder" ||
      code == "truth_contract_missed_win" ||
      code == "truth_contract_tactical_refutation" ||
      code == "truth_contract_tactical_failure_mode" ||
      code == "planner_truth_mode_tactical" ||
      code == "main_claim_tactical" ||
      code == "context_severe_counterfactual" ||
      code == "tactical_context_missing" ||
      code == "truth_contract_missing" ||
      code.contains("tactical")

  private def claimOnlySurfacePresent(packet: PlayerFacingClaimPacket): Boolean =
    packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain

  private def exactSliceWitnessPresent(packet: PlayerFacingClaimPacket): Boolean =
    packet.proofPathWitness.exactSliceProof.exists(exactSliceProofMatchesPacket(packet, _))

  private def exactSliceProofMatchesPacket(
      packet: PlayerFacingClaimPacket,
      proof: PlayerFacingExactSliceProof
  ): Boolean =
    proof match
      case PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare) =>
        packetMatches(packet, ProofSourceId.ExactTargetFixation.wireKey, proofFamily(PlanTaxonomy.PlanKind.StaticWeaknessFixation).wireKey) &&
          squareKey(targetSquare)
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(targetSquare, minoritySupport) =>
        packetMatches(packet, ProofSourceId.CarlsbadFixedTargetProbe.wireKey, proofFamily(PlanTaxonomy.PlanKind.BackwardPawnTargeting).wireKey) &&
          Set("c6", "c3").contains(normalize(targetSquare)) &&
          minoritySupport
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(targetSquare, supportFromSquares, targetPieces) =>
        packetMatches(packet, ProofSourceId.TargetFocusedCoordinationProbe.wireKey, ProofFamilyId.TargetFocusedCoordination.wireKey) &&
          squareKey(targetSquare) &&
          supportFromSquares.map(normalize).filter(squareKey).distinct.size >= 2 &&
          targetPieces.exists(token => normalize(token).startsWith("target_"))
      case PlayerFacingExactSliceProof.ColorComplexSqueeze(targetSquare, squareColor, minorPieceRole, minorPieceSquare) =>
        packetMatches(packet, ProofSourceId.ColorComplexSqueezeProbe.wireKey, ProofFamilyId.ColorComplexSqueeze.wireKey) &&
          squareKey(targetSquare) &&
          Set("light", "dark").contains(normalize(squareColor)) &&
          Set("bishop", "knight").contains(normalize(minorPieceRole)) &&
          squareKey(minorPieceSquare)
      case PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare) =>
        packetMatches(packet, ProofSourceId.LocalFileEntryBind.wireKey, ProofFamilyId.HalfOpenFilePressure.wireKey) &&
          fileToken(file) &&
          squareKey(entrySquare)
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken) =>
        packetMatches(packet, ProofSourceId.CounterplayAxisSuppression.wireKey, ProofFamilyId.NeutralizeKeyBreak.wireKey) &&
          breakTokenShape(breakToken)
      case PlayerFacingExactSliceProof.ProphylacticRestraint(resourceTokenValue) =>
        packetMatches(packet, ProofSourceId.ProphylacticMove.wireKey, ProofFamilyId.CounterplayRestraint.wireKey) &&
          resourceToken(resourceTokenValue)
      case PlayerFacingExactSliceProof.QueenTradeShield(lineMoves) =>
        packetMatches(packet, proofFamily(PlanTaxonomy.PlanKind.QueenTradeShield).wireKey, proofFamily(PlanTaxonomy.PlanKind.QueenTradeShield).wireKey) &&
          lineMoves.size >= 2 &&
          lineMoves.forall(uciMove)
      case PlayerFacingExactSliceProof.CentralBreakTiming(breakMove, breakSquare, breakToken) =>
        packetMatches(packet, proofFamily(PlanTaxonomy.PlanKind.CentralBreakTiming).wireKey, proofFamily(PlanTaxonomy.PlanKind.CentralBreakTiming).wireKey) &&
          uciMove(breakMove) &&
          squareKey(breakSquare) &&
          uciDestination(breakMove).contains(normalize(breakSquare)) &&
          routeToken(breakToken)

  private def packetMatches(packet: PlayerFacingClaimPacket, proofSource: String, proofFamily: String): Boolean =
    normalize(packet.proofSource) == normalize(proofSource) &&
      normalize(packet.proofFamily) == normalize(proofFamily)

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
          token.matches("""denied_resource:(?:break|entry_square|forcing_threat|piece_activity|counterplay_route|route_node|reroute_square|pressure|color_complex_escape)""")
      )

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
