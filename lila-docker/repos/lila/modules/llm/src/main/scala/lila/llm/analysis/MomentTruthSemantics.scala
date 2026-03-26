package lila.llm.analysis

import lila.llm.{ GameChronicleMoment, NarrativeSignalDigest }
import lila.llm.model.GameArcMoment

private[analysis] object MomentTruthSemantics:

  final case class ChronicleSemantics(
      projection: MomentTruthProjection,
      hasTruthContract: Boolean,
      canonicalLens: String,
      compensationSelectionEligible: Boolean,
      truthBackedStrategicVisibility: Boolean,
      collapseSensitive: Boolean
  )

  final case class WholeGameSemantics(
      projection: MomentTruthProjection,
      hasTruthContract: Boolean,
      canonicalPrioritySeed: Int,
      punishOrConversion: Boolean
  )

  def chronicle(
      moment: GameChronicleMoment,
      contractOpt: Option[DecisiveTruthContract]
  ): ChronicleSemantics =
    val projection = DecisiveTruth.momentProjection(moment, contractOpt)
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val digest = moment.signalDigest
    val hasTruthContract = contractOpt.nonEmpty
    val canonicalLens =
      contractOpt
        .flatMap(contractLens(_, projection))
        .getOrElse(fallbackChronicleLens(moment.momentType, moment.moveClassification, digest))
    val compensationSelectionEligible =
      if hasTruthContract then truthCompensationSelectionEligible(projection)
      else surface.strictCompensationPosition || surface.compensationPosition
    val truthBackedStrategicVisibility =
      projection.visibilityRole != TruthVisibilityRole.Hidden &&
        projection.surfaceMode != TruthSurfaceMode.FailureExplain &&
        (
          projection.ownershipRole != TruthOwnershipRole.NoneRole ||
            projection.surfaceMode != TruthSurfaceMode.Neutral ||
            projection.exemplarRole != TruthExemplarRole.NonExemplar ||
            projection.maintenanceExemplarCandidate
        )
    val collapseSensitive =
      contractOpt match
        case Some(contract) =>
          projection.classificationKey == "blunder" ||
            projection.classificationKey == "missedwin" ||
            projection.ownershipRole == TruthOwnershipRole.BlunderOwner ||
            projection.surfaceMode == TruthSurfaceMode.FailureExplain ||
            contract.failureInterpretationAllowed
        case None =>
          isFailureMomentType(moment.momentType)
    ChronicleSemantics(
      projection = projection,
      hasTruthContract = hasTruthContract,
      canonicalLens = canonicalLens,
      compensationSelectionEligible = compensationSelectionEligible,
      truthBackedStrategicVisibility = truthBackedStrategicVisibility,
      collapseSensitive = collapseSensitive
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

  private def truthCompensationSelectionEligible(projection: MomentTruthProjection): Boolean =
    projection.ownershipRole == TruthOwnershipRole.CommitmentOwner ||
      projection.ownershipRole == TruthOwnershipRole.MaintenanceEcho ||
      projection.ownershipRole == TruthOwnershipRole.ConversionOwner ||
      projection.exemplarRole != TruthExemplarRole.NonExemplar ||
      projection.maintenanceExemplarCandidate ||
      projection.surfaceMode == TruthSurfaceMode.InvestmentExplain ||
      projection.surfaceMode == TruthSurfaceMode.MaintenancePreserve ||
      projection.surfaceMode == TruthSurfaceMode.ConversionExplain

  private def contractLens(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Option[String] =
    if isTacticalTruth(contract, projection) then Some("tactical")
    else if truthCompensationSelectionEligible(projection) then Some("compensation")
    else if projection.ownershipRole == TruthOwnershipRole.ConversionOwner ||
        projection.surfaceMode == TruthSurfaceMode.ConversionExplain ||
        contract.reasonFamily == DecisiveReasonFamily.Conversion then
      Some("decision")
    else if contract.reasonFamily == DecisiveReasonFamily.QuietTechnicalMove then Some("decision")
    else None

  private def isTacticalTruth(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Boolean =
    projection.classificationKey == "blunder" ||
      projection.classificationKey == "missedwin" ||
      projection.ownershipRole == TruthOwnershipRole.BlunderOwner ||
      projection.surfaceMode == TruthSurfaceMode.FailureExplain ||
      contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation ||
      contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense ||
      contract.reasonFamily == DecisiveReasonFamily.MissedWin ||
      contract.failureMode == FailureInterpretationMode.TacticalRefutation ||
      contract.failureMode == FailureInterpretationMode.OnlyMoveFailure

  private def fallbackChronicleLens(
      momentType: String,
      moveClassification: Option[String],
      digest: Option[NarrativeSignalDigest]
  ): String =
    val normalizedMomentType = normalize(momentType)
    val normalizedClassification = normalize(moveClassification.getOrElse(""))
    if isTactical(normalizedMomentType) || isTactical(normalizedClassification) then "tactical"
    else if digest.exists(d => d.structureProfile.exists(_.trim.nonEmpty) && d.deploymentPiece.exists(_.trim.nonEmpty)) then
      "structure"
    else if digest.exists(_.opening.exists(_.trim.nonEmpty)) then "opening"
    else if digest.exists(d => d.compensation.exists(_.trim.nonEmpty) || d.investedMaterial.exists(_ > 0)) then
      "compensation"
    else if digest.exists(d => d.prophylaxisPlan.exists(_.trim.nonEmpty) || d.prophylaxisThreat.exists(_.trim.nonEmpty)) then
      "prophylaxis"
    else if digest.exists(d => d.decisionComparison.isDefined || d.decision.exists(_.trim.nonEmpty)) then "decision"
    else if digest.exists(_.practicalVerdict.exists(_.trim.nonEmpty)) then "practical"
    else "strategic"

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

  private def isFailureMomentType(raw: String): Boolean =
    val momentType = normalize(raw)
    momentType == "blunder" || momentType == "sustainedpressure"

  private def isTactical(raw: String): Boolean =
    raw.contains("blunder") ||
      raw.contains("missedwin") ||
      raw.contains("advantageswing") ||
      raw.contains("matepivot") ||
      raw.contains("tactical")

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
