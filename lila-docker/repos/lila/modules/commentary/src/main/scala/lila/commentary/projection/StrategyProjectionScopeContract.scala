package lila.commentary.projection

object StrategyProjectionScopeContract:

  val S05: StrategyProjectionBandId = StrategyProjectionBandId("S05")
  val S06: StrategyProjectionBandId = StrategyProjectionBandId("S06")
  val S07: StrategyProjectionBandId = StrategyProjectionBandId("S07")
  val S08: StrategyProjectionBandId = StrategyProjectionBandId("S08")
  val S11: StrategyProjectionBandId = StrategyProjectionBandId("S11")
  val S13: StrategyProjectionBandId = StrategyProjectionBandId("S13")
  val S14: StrategyProjectionBandId = StrategyProjectionBandId("S14")
  val S15: StrategyProjectionBandId = StrategyProjectionBandId("S15")
  val S16: StrategyProjectionBandId = StrategyProjectionBandId("S16")
  val S17: StrategyProjectionBandId = StrategyProjectionBandId("S17")
  val S18: StrategyProjectionBandId = StrategyProjectionBandId("S18")
  val S19: StrategyProjectionBandId = StrategyProjectionBandId("S19")
  val S21: StrategyProjectionBandId = StrategyProjectionBandId("S21")
  val S22: StrategyProjectionBandId = StrategyProjectionBandId("S22")
  val S23: StrategyProjectionBandId = StrategyProjectionBandId("S23")
  val S24: StrategyProjectionBandId = StrategyProjectionBandId("S24")
  val S25: StrategyProjectionBandId = StrategyProjectionBandId("S25")

  val WeakPawnTargetPressurePersistenceCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("weak_pawn_target_pressure_persistence_certified")
  val CenterReleaseRouteCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("center_release_route_certified")
  val SpaceBindRestrictionRouteCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("space_bind_restriction_route_certified")
  val InitiativeConversionRouteCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("initiative_conversion_route_certified")
  val CounterplayDenialRouteCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("counterplay_denial_route_certified")
  val WingDamageRouteCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("wing_damage_route_certified")
  val ChainBaseContactRouteCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("chain_base_contact_route_certified")
  val PasserCreationRouteCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("passer_creation_route_certified")
  val PasserSuppressionRouteCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("passer_suppression_route_certified")
  val LiabilityReliefCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("liability_relief_certified")
  val BishopPairInitiativeConversionCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("bishop_pair_initiative_conversion_certified")
  val BishopPairStructureConversionCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("bishop_pair_structure_conversion_certified")
  val BishopPairMaterialConversionCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("bishop_pair_material_conversion_certified")
  val TradeInvariantMaterialSimplificationCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("trade_invariant_material_simplification_certified")
  val TradeInvariantHoldSimplificationCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("trade_invariant_hold_simplification_certified")
  val CounterplaySurvivalRouteCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("counterplay_survival_route_certified")
  val FortressHoldCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("fortress_hold_certified")
  val PerpetualHoldCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("perpetual_hold_certified")
  val KingEntryConversionCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("king_entry_conversion_certified")
  val KingOppositionCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("king_opposition_certified")
  val SameTargetForcingRealization: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("same_target_forcing_realization")
  val SameTargetConversionCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("same_target_conversion_certified")
  val RankAccessConsequenceCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("rank_access_consequence_certified")

  val startReadyBandIds: Vector[StrategyProjectionBandId] =
    Vector(S05, S06, S07, S08, S11, S13, S14, S15, S16, S17, S18, S19, S21, S22, S23, S24, S25)

  val requiredEvidenceKindsByBand: Map[String, Vector[StrategyProjectionEvidenceKind]] =
    Map(
      S05.value -> Vector(CenterReleaseRouteCertified),
      S06.value -> Vector(SpaceBindRestrictionRouteCertified),
      S07.value -> Vector(InitiativeConversionRouteCertified),
      S08.value -> Vector(CounterplayDenialRouteCertified),
      S11.value -> Vector(WeakPawnTargetPressurePersistenceCertified),
      S13.value -> Vector(WingDamageRouteCertified),
      S14.value -> Vector(ChainBaseContactRouteCertified),
      S15.value -> Vector(PasserCreationRouteCertified),
      S16.value -> Vector(PasserSuppressionRouteCertified),
      S17.value -> Vector(LiabilityReliefCertified),
      S18.value -> Vector(
        BishopPairInitiativeConversionCertified,
        BishopPairStructureConversionCertified,
        BishopPairMaterialConversionCertified
      ),
      S19.value -> Vector(
        TradeInvariantMaterialSimplificationCertified,
        TradeInvariantHoldSimplificationCertified
      ),
      S21.value -> Vector(CounterplaySurvivalRouteCertified),
      S22.value -> Vector(FortressHoldCertified, PerpetualHoldCertified),
      S23.value -> Vector(KingEntryConversionCertified, KingOppositionCertified),
      S24.value -> Vector(SameTargetForcingRealization, SameTargetConversionCertified),
      S25.value -> Vector(RankAccessConsequenceCertified)
    )

  private val startReadyBandIdSet: Set[StrategyProjectionBandId] =
    startReadyBandIds.toSet

  def isStartReadyBandId(bandId: StrategyProjectionBandId): Boolean =
    startReadyBandIdSet.contains(bandId)

  def isAllowedEvidenceKind(
      bandId: StrategyProjectionBandId,
      kind: StrategyProjectionEvidenceKind
  ): Boolean =
    requiredEvidenceKindsByBand
      .getOrElse(bandId.value, Vector.empty)
      .contains(kind)

  require(
    startReadyBandIds.map(_.value) == Vector("S05", "S06", "S07", "S08", "S11", "S13", "S14", "S15", "S16", "S17", "S18", "S19", "S21", "S22", "S23", "S24", "S25"),
    "S05/S06/S07/S08/S11/S13/S14/S15/S16/S17/S18/S19/S21/S22/S23/S24/S25 are the only start-ready projection bands in this scaffold"
  )
