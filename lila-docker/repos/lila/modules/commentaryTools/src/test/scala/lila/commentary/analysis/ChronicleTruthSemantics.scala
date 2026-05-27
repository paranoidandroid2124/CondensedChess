package lila.commentary.analysis

import lila.commentary.{ GameChronicleMoment, NarrativeSignalDigest }

private[analysis] object ChronicleTruthSemantics:

  final case class Semantics(
      projection: MomentTruthProjection,
      hasTruthContract: Boolean,
      canonicalLens: String,
      compensationSelectionEligible: Boolean,
      truthBackedStrategicVisibility: Boolean,
      threadLocalReplacementEligible: Boolean,
      globalVisibleEligible: Boolean,
      globalActiveNoteEligible: Boolean,
      collapseSensitive: Boolean
  )

  def projection(
      moment: GameChronicleMoment,
      contractOpt: Option[DecisiveTruthContract]
  ): MomentTruthProjection =
    contractOpt.map(contractProjection).getOrElse(
      fallbackProjection(
        classificationKey = normalized(moment.moveClassification.getOrElse("")).getOrElse(""),
        verifiedPayoffAnchor = None
      )
    )

  def chronicle(
      moment: GameChronicleMoment,
      contractOpt: Option[DecisiveTruthContract]
  ): Semantics =
    val projected = projection(moment, contractOpt)
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val digest = moment.signalDigest
    val hasTruthContract = contractOpt.nonEmpty
    val canonicalLens =
      contractOpt
        .flatMap(contractLens(_, projected))
        .getOrElse(fallbackChronicleLens(moment.momentType, moment.moveClassification, digest))
    val compensationSelectionEligible =
      if hasTruthContract then truthCompensationSelectionEligible(projected)
      else surface.strictCompensationPosition || surface.compensationPosition
    val truthBackedStrategicVisibility =
      projected.visibilityRole != TruthVisibilityRole.Hidden &&
        projected.surfaceMode != TruthSurfaceMode.FailureExplain &&
        (
          projected.ownershipRole != TruthOwnershipRole.NoneRole ||
            projected.surfaceMode != TruthSurfaceMode.Neutral ||
            projected.exemplarRole != TruthExemplarRole.NonExemplar ||
            projected.maintenanceExemplarCandidate
        )
    val threadLocalReplacementEligible =
      contractOpt match
        case Some(contract) => truthThreadLocalReplacementEligible(contract, projected)
        case None           => fallbackStrategicCarrierPresent(moment, surface)
    val globalVisibleEligible =
      contractOpt match
        case Some(contract) => truthGlobalVisibleEligible(contract, projected)
        case None           => fallbackStrategicCarrierPresent(moment, surface)
    val globalActiveNoteEligible =
      contractOpt match
        case Some(contract) => truthGlobalVisibleEligible(contract, projected)
        case None           => fallbackStrategicCarrierPresent(moment, surface)
    val collapseSensitive =
      contractOpt match
        case Some(contract) =>
          projected.classificationKey == "blunder" ||
            projected.classificationKey == "missedwin" ||
            projected.ownershipRole == TruthOwnershipRole.BlunderOwner ||
            projected.surfaceMode == TruthSurfaceMode.FailureExplain ||
            contract.failureInterpretationAllowed
        case None =>
          isFailureMomentType(moment.momentType)
    Semantics(
      projection = projected,
      hasTruthContract = hasTruthContract,
      canonicalLens = canonicalLens,
      compensationSelectionEligible = compensationSelectionEligible,
      truthBackedStrategicVisibility = truthBackedStrategicVisibility,
      threadLocalReplacementEligible = threadLocalReplacementEligible,
      globalVisibleEligible = globalVisibleEligible,
      globalActiveNoteEligible = globalActiveNoteEligible,
      collapseSensitive = collapseSensitive
    )

  private def contractProjection(contract: DecisiveTruthContract): MomentTruthProjection =
    MomentTruthProjection(
      classificationKey = contract.truthClassKey.replace("_", ""),
      ownershipRole = contract.ownershipRole,
      visibilityRole = contract.visibilityRole,
      surfaceMode = contract.surfaceMode,
      exemplarRole = contract.exemplarRole,
      surfacedMoveOwnsTruth = contract.surfacedMoveOwnsTruth,
      verifiedPayoffAnchor = contract.verifiedPayoffAnchor,
      benchmarkProseAllowed = contract.benchmarkProseAllowed,
      chainKey = contract.investmentTruthChainKey,
      maintenanceExemplarCandidate = contract.maintenanceExemplarCandidate,
      benchmarkCriticalMove = contract.benchmarkCriticalMove
    )

  private def fallbackProjection(
      classificationKey: String,
      verifiedPayoffAnchor: Option[String]
  ): MomentTruthProjection =
    val ownershipRole =
      classificationKey match
        case "blunder" | "missedwin" => TruthOwnershipRole.BlunderOwner
        case _                       => TruthOwnershipRole.NoneRole
    val surfaceMode =
      ownershipRole match
        case TruthOwnershipRole.BlunderOwner => TruthSurfaceMode.FailureExplain
        case _                               => TruthSurfaceMode.Neutral
    val ownsTruth = ownershipRole == TruthOwnershipRole.BlunderOwner
    MomentTruthProjection(
      classificationKey = classificationKey,
      ownershipRole = ownershipRole,
      visibilityRole = if ownsTruth then TruthVisibilityRole.PrimaryVisible else TruthVisibilityRole.Hidden,
      surfaceMode = surfaceMode,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = ownsTruth,
      verifiedPayoffAnchor = Option.when(ownsTruth)(verifiedPayoffAnchor).flatten,
      benchmarkProseAllowed = false,
      chainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = false
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

  private def truthThreadLocalReplacementEligible(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Boolean =
    truthGlobalVisibleEligible(contract, projection) ||
      truthFailureSignificantThreadLocal(contract, projection) ||
      truthCriticalBestTacticalOrTechnical(contract, projection)

  private def truthGlobalVisibleEligible(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Boolean =
    truthProtectedFamily(contract, projection) ||
      (
        projection.visibilityRole != TruthVisibilityRole.Hidden &&
          (
            projection.ownershipRole == TruthOwnershipRole.CommitmentOwner ||
              projection.ownershipRole == TruthOwnershipRole.ConversionOwner ||
              projection.ownershipRole == TruthOwnershipRole.MaintenanceEcho ||
              projection.exemplarRole != TruthExemplarRole.NonExemplar ||
              projection.maintenanceExemplarCandidate ||
              projection.surfaceMode == TruthSurfaceMode.InvestmentExplain ||
              projection.surfaceMode == TruthSurfaceMode.MaintenancePreserve ||
              projection.surfaceMode == TruthSurfaceMode.ConversionExplain
          )
      )

  private def truthProtectedFamily(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Boolean =
    projection.classificationKey == "blunder" ||
      projection.classificationKey == "missedwin" ||
      truthPromotedBestHold(contract, projection) ||
      projection.exemplarRole == TruthExemplarRole.VerifiedExemplar ||
      projection.exemplarRole == TruthExemplarRole.ProvisionalExemplar ||
      projection.ownershipRole == TruthOwnershipRole.CommitmentOwner ||
      projection.ownershipRole == TruthOwnershipRole.ConversionOwner

  private def truthPromotedBestHold(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Boolean =
    projection.classificationKey == "best" &&
      contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense &&
      contract.benchmarkCriticalMove

  private def truthFailureSignificantThreadLocal(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Boolean =
    projection.classificationKey != "best" &&
      contract.failureMode != FailureInterpretationMode.NoClearPlan &&
      (
        contract.reasonFamily == DecisiveReasonKind.TacticalRefutation ||
          contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense ||
          contract.reasonFamily == DecisiveReasonKind.QuietTechnicalMove
      )

  private def truthCriticalBestTacticalOrTechnical(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Boolean =
    projection.classificationKey == "best" &&
      (
        contract.reasonFamily == DecisiveReasonKind.TacticalRefutation ||
          (
            contract.reasonFamily == DecisiveReasonKind.QuietTechnicalMove &&
              contract.benchmarkCriticalMove
          )
      )

  private def contractLens(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Option[String] =
    if isTacticalTruth(contract, projection) then Some("tactical")
    else if truthCompensationSelectionEligible(projection) then Some("compensation")
    else if projection.ownershipRole == TruthOwnershipRole.ConversionOwner ||
        projection.surfaceMode == TruthSurfaceMode.ConversionExplain ||
        contract.reasonFamily == DecisiveReasonKind.Conversion then
      Some("decision")
    else if contract.reasonFamily == DecisiveReasonKind.QuietTechnicalMove then Some("decision")
    else None

  private def isTacticalTruth(
      contract: DecisiveTruthContract,
      projection: MomentTruthProjection
  ): Boolean =
    projection.classificationKey == "blunder" ||
      projection.classificationKey == "missedwin" ||
      projection.ownershipRole == TruthOwnershipRole.BlunderOwner ||
      projection.surfaceMode == TruthSurfaceMode.FailureExplain ||
      contract.reasonFamily == DecisiveReasonKind.TacticalRefutation ||
      contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense ||
      contract.reasonFamily == DecisiveReasonKind.MissedWin ||
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

  private def fallbackStrategicCarrierPresent(
      moment: GameChronicleMoment,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    surface.dominantIdeaText.nonEmpty ||
      surface.executionText.nonEmpty ||
      surface.objectiveText.nonEmpty ||
      surface.focusText.nonEmpty ||
      surface.compensationPosition ||
      moment.activePlan.isDefined ||
      moment.strategyPack.exists(pack =>
        pack.strategicIdeas.nonEmpty || pack.longTermFocus.nonEmpty || pack.pieceRoutes.nonEmpty
      ) ||
      moment.signalDigest.exists(digest =>
        digest.dominantIdeaKind.isDefined ||
          digest.compensation.exists(_.trim.nonEmpty) ||
          digest.compensationVectors.exists(_.trim.nonEmpty) ||
          digest.opponentPlan.exists(_.trim.nonEmpty)
      )

  private def isFailureMomentType(raw: String): Boolean =
    val momentType = normalize(raw)
    momentType == "blunder" || momentType == "sustainedpressure"

  private def isTactical(raw: String): Boolean =
    raw.contains("blunder") ||
      raw.contains("missedwin") ||
      raw.contains("advantageswing") ||
      raw.contains("matepivot") ||
      raw.contains("tactical")

  private def normalized(raw: String): Option[String] =
    Option(raw).map(_.trim.toLowerCase).filter(_.nonEmpty)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
