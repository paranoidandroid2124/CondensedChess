package lila.commentary.projection

object StrategyProjectionScopeContract:

  val S17: StrategyProjectionBandId = StrategyProjectionBandId("S17")
  val S23: StrategyProjectionBandId = StrategyProjectionBandId("S23")
  val S24: StrategyProjectionBandId = StrategyProjectionBandId("S24")
  val S25: StrategyProjectionBandId = StrategyProjectionBandId("S25")

  val LiabilityReliefCertified: StrategyProjectionEvidenceKind =
    StrategyProjectionEvidenceKind("liability_relief_certified")
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
    Vector(S17, S23, S24, S25)

  val requiredEvidenceKindsByBand: Map[String, Vector[StrategyProjectionEvidenceKind]] =
    Map(
      S17.value -> Vector(LiabilityReliefCertified),
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
    startReadyBandIds.map(_.value) == Vector("S17", "S23", "S24", "S25"),
    "S17/S23/S24/S25 are the only start-ready projection bands in this scaffold"
  )
