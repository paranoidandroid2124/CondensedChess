package lila.commentary.analysis

import lila.commentary.model.GameArcMoment

private[analysis] object MomentTruthSemantics:

  final case class WholeGameSemantics(
      projection: MomentTruthProjection,
      hasTruthContract: Boolean,
      canonicalPrioritySeed: Int,
      punishOrConversion: Boolean
  )

  def arc(
      moment: GameArcMoment,
      contractOpt: Option[DecisiveTruthContract]
  ): WholeGameSemantics =
    val projection = DecisiveTruth.momentProjection(moment, contractOpt)
    val hasTruthContract = contractOpt.nonEmpty
    val canonicalPrioritySeed =
      if hasTruthContract then truthFirstPriority(moment, projection)
      else fallbackPriority(moment, projection)
    val punishOrConversion =
      if hasTruthContract then
        projection.classificationKey == "blunder" ||
          projection.classificationKey == "missedwin" ||
          projection.ownershipRole == TruthOwnershipRole.CommitmentOwner ||
          projection.ownershipRole == TruthOwnershipRole.ConversionOwner ||
          projection.surfaceMode == TruthSurfaceMode.ConversionExplain
      else fallbackPunishOrConversion(moment, projection)
    WholeGameSemantics(
      projection = projection,
      hasTruthContract = hasTruthContract,
      canonicalPrioritySeed = canonicalPrioritySeed,
      punishOrConversion = punishOrConversion
    )

  private def truthFirstPriority(
      moment: GameArcMoment,
      projection: MomentTruthProjection
  ): Int =
    val momentType = normalize(moment.momentType)
    if projection.classificationKey == "blunder" then 120
    else if projection.classificationKey == "missedwin" then 110
    else if projection.benchmarkCriticalMove &&
        projection.visibilityRole == TruthVisibilityRole.PrimaryVisible then
      109
    else if projection.ownershipRole == TruthOwnershipRole.CommitmentOwner &&
        projection.visibilityRole == TruthVisibilityRole.PrimaryVisible then
      108
    else if projection.ownershipRole == TruthOwnershipRole.ConversionOwner &&
        projection.visibilityRole != TruthVisibilityRole.Hidden then
      95
    else if projection.ownershipRole == TruthOwnershipRole.MaintenanceEcho then 60
    else if momentType.contains("mate") then 105
    else if momentType.contains("equalization") then 65
    else if momentType.contains("tensionpeak") then 30
    else 40

  private def fallbackPriority(
      moment: GameArcMoment,
      projection: MomentTruthProjection
  ): Int =
    val momentType = normalize(moment.momentType)
    val transition = normalize(moment.transitionType.getOrElse(""))
    if projection.classificationKey == "blunder" then 120
    else if projection.classificationKey == "missedwin" then 110
    else if projection.benchmarkCriticalMove &&
        projection.visibilityRole == TruthVisibilityRole.PrimaryVisible then
      109
    else if projection.ownershipRole == TruthOwnershipRole.CommitmentOwner &&
        projection.visibilityRole == TruthVisibilityRole.PrimaryVisible then
      108
    else if projection.ownershipRole == TruthOwnershipRole.ConversionOwner &&
        projection.visibilityRole != TruthVisibilityRole.Hidden then
      95
    else if projection.ownershipRole == TruthOwnershipRole.MaintenanceEcho then 60
    else if momentType.contains("mate") then 105
    else if transition.contains("promotion") then 100
    else if transition.contains("exchange") || transition.contains("convert") || transition.contains("simplif") then 95
    else if momentType.contains("advantageswing") || momentType.contains("swing") then 90
    else if momentType.contains("sustainedpressure") then 80
    else if momentType.contains("equalization") then 65
    else if momentType.contains("tensionpeak") then 30
    else 40

  private def fallbackPunishOrConversion(
      moment: GameArcMoment,
      projection: MomentTruthProjection
  ): Boolean =
    val transition = normalize(moment.transitionType.getOrElse(""))
    projection.classificationKey == "blunder" ||
      projection.classificationKey == "missedwin" ||
      projection.ownershipRole == TruthOwnershipRole.CommitmentOwner ||
      projection.ownershipRole == TruthOwnershipRole.ConversionOwner ||
      transition.contains("promotion") ||
      transition.contains("exchange") ||
      transition.contains("convert") ||
      transition.contains("simplif")

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
