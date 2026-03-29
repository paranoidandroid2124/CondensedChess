package lila.llm.analysis

import lila.llm.*

private[llm] object StrategyPackSurface:

  final case class CompensationSubtype(
      pressureTheater: String,
      pressureMode: String,
      recoveryPolicy: String,
      stabilityClass: String
  ):
    def durablePressure: Boolean = stabilityClass == "durable_pressure"
    def quietPressure: Boolean =
      durablePressure && Set("line_occupation", "target_fixing", "counterplay_denial").contains(pressureMode)
    def transitionOnly: Boolean = stabilityClass == "transition_only" || pressureMode == "conversion_window"

  final case class DisplayNormalization(
      normalizedDominantIdeaText: Option[String],
      normalizedExecutionText: Option[String],
      normalizedObjectiveText: Option[String],
      normalizedLongTermFocusText: Option[String],
      normalizedCompensationLead: Option[String],
      normalizedCompensationSubtype: Option[CompensationSubtype],
      normalizationActive: Boolean,
      normalizationConfidence: Int,
      preparationSubtype: Option[CompensationSubtype],
      payoffSubtype: Option[CompensationSubtype],
      selectedDisplaySubtype: Option[CompensationSubtype],
      displaySubtypeSource: String,
      payoffConfidence: Int,
      pathConfidence: Int
  )

  final case class Snapshot(
      sideToMove: Option[String],
      dominantIdea: Option[StrategyIdeaSignal],
      secondaryIdea: Option[StrategyIdeaSignal],
      campaignOwner: Option[String],
      ownerMismatch: Boolean,
      allRoutes: List[StrategyPieceRoute],
      topRoute: Option[StrategyPieceRoute],
      allMoveRefs: List[StrategyPieceMoveRef],
      topMoveRef: Option[StrategyPieceMoveRef],
      allDirectionalTargets: List[StrategyDirectionalTarget],
      topDirectionalTarget: Option[StrategyDirectionalTarget],
      longTermFocus: Option[String],
      evidenceHints: List[String],
      compensationSummary: Option[String],
      compensationVectors: List[String],
      investedMaterial: Option[Int],
      compensationSubtype: Option[CompensationSubtype],
      displayNormalization: Option[DisplayNormalization] = None
  ):
    def rawDominantIdeaText: Option[String] = dominantIdea.flatMap(StrategyPackSurface.ideaText)
    def rawSecondaryIdeaText: Option[String] = secondaryIdea.flatMap(StrategyPackSurface.ideaText)
    def rawExecutionText: Option[String] =
      topRoute.flatMap(StrategyPackSurface.routeText).orElse(topMoveRef.flatMap(StrategyPackSurface.moveRefText))
    def rawObjectiveText: Option[String] =
      topDirectionalTarget.flatMap(StrategyPackSurface.targetText).orElse(longTermFocus)
    def rawFocusText: Option[String] =
      longTermFocus.orElse(
        dominantIdea.map(StrategicIdeaSelector.focusSummary).map(StrategyPackSurface.normalizeText).filter(_.nonEmpty)
      )
    def preferRawAttackDisplay: Boolean = StrategyPackSurface.preferRawAttackDisplay(this)
    def normalizedDominantIdeaText: Option[String] =
      Option.when(!preferRawAttackDisplay)(displayNormalization.filter(_.normalizationActive).flatMap(_.normalizedDominantIdeaText)).flatten
    def normalizedExecutionText: Option[String] =
      Option.when(!preferRawAttackDisplay)(displayNormalization.filter(_.normalizationActive).flatMap(_.normalizedExecutionText)).flatten
    def normalizedObjectiveText: Option[String] =
      Option.when(!preferRawAttackDisplay)(displayNormalization.filter(_.normalizationActive).flatMap(_.normalizedObjectiveText)).flatten
    def normalizedLongTermFocusText: Option[String] =
      Option.when(!preferRawAttackDisplay)(displayNormalization.filter(_.normalizationActive).flatMap(_.normalizedLongTermFocusText)).flatten
    def normalizedCompensationLead: Option[String] =
      Option.when(!preferRawAttackDisplay)(displayNormalization.filter(_.normalizationActive).flatMap(_.normalizedCompensationLead)).flatten
    def displayCompensationSubtype: Option[CompensationSubtype] =
      Option.when(!preferRawAttackDisplay)(displayNormalization.filter(_.normalizationActive).flatMap(_.selectedDisplaySubtype)).flatten
    def preparationCompensationSubtype: Option[CompensationSubtype] =
      Option.when(!preferRawAttackDisplay)(displayNormalization.flatMap(_.preparationSubtype)).flatten
    def payoffCompensationSubtype: Option[CompensationSubtype] =
      Option.when(!preferRawAttackDisplay)(displayNormalization.flatMap(_.payoffSubtype)).flatten
    def displaySubtypeSource: String =
      Option.when(!preferRawAttackDisplay)(displayNormalization.map(_.displaySubtypeSource)).flatten.getOrElse("raw_fallback")
    def payoffConfidence: Int = Option.when(!preferRawAttackDisplay)(displayNormalization.map(_.payoffConfidence)).flatten.getOrElse(0)
    def pathConfidence: Int = Option.when(!preferRawAttackDisplay)(displayNormalization.map(_.pathConfidence)).flatten.getOrElse(0)
    def effectiveCompensationSubtype: Option[CompensationSubtype] =
      displayCompensationSubtype.orElse(compensationSubtype)
    def strictCompensationSubtype: Option[CompensationSubtype] =
      displayNormalization
        .filter(resolvedCompensationContract)
        .flatMap(_.selectedDisplaySubtype)
    def compensationContractResolved: Boolean =
      displayNormalization.exists(resolvedCompensationContract)
    def strictCompensationPosition: Boolean =
      compensationPosition &&
        strictCompensationSubtype.exists(subtype => !subtype.transitionOnly)
    def dominantIdeaText: Option[String] = normalizedDominantIdeaText.orElse(rawDominantIdeaText)
    def secondaryIdeaText: Option[String] = rawSecondaryIdeaText
    def executionText: Option[String] = normalizedExecutionText.orElse(rawExecutionText)
    def objectiveText: Option[String] = normalizedObjectiveText.orElse(rawObjectiveText)
    def focusText: Option[String] = normalizedLongTermFocusText.orElse(rawFocusText)
    def normalizationActive: Boolean = displayNormalization.exists(_.normalizationActive) && !preferRawAttackDisplay
    def normalizationConfidence: Int = displayNormalization.map(_.normalizationConfidence).getOrElse(0)
    def compensationPosition: Boolean =
      compensationSummary.exists(_.nonEmpty) || investedMaterial.exists(_ > 0)
    def durableCompensationPosition: Boolean = effectiveCompensationSubtype.exists(_.durablePressure)
    def quietCompensationPosition: Boolean = effectiveCompensationSubtype.exists(_.quietPressure)
    def campaignOwnerText: Option[String] = campaignOwner.map(StrategyPackSurface.sideLabel)

  def from(packOpt: Option[StrategyPack]): Snapshot =
    val pack = packOpt
    val dominantIdea = pack.toList.flatMap(_.strategicIdeas).headOption
    val secondaryIdea = pack.toList.flatMap(_.strategicIdeas).lift(1)
    val preferredOwner = dominantIdea.map(_.ownerSide).orElse(pack.map(_.sideToMove))
    val sideToMove = pack.map(_.sideToMove).map(normalizeSide)
    val campaignOwner = preferredOwner.map(normalizeSide)
    val ownerMismatch = campaignOwner.exists(owner => sideToMove.exists(_ != owner))
    val signalDigest = pack.flatMap(_.signalDigest)
    val allRoutes =
      prioritizeByOwner(pack.toList.flatMap(_.pieceRoutes).filterNot(isHiddenRoute), preferredOwner.map(normalizeSide), _.ownerSide)
    val allMoveRefs =
      prioritizeByOwner(pack.toList.flatMap(_.pieceMoveRefs), preferredOwner.map(normalizeSide), _.ownerSide)
    val allDirectionalTargets =
      prioritizeByOwner(pack.toList.flatMap(_.directionalTargets), preferredOwner.map(normalizeSide), _.ownerSide)
    val topRoute = allRoutes.headOption
    val topMoveRef = allMoveRefs.headOption
    val topDirectionalTarget = allDirectionalTargets.headOption
    val longTermFocus = pack.toList.flatMap(_.longTermFocus.map(normalizeText).filter(_.nonEmpty)).headOption
    val evidenceHints =
      pack.toList.flatMap(_.evidence.map(normalizeText).filter(_.nonEmpty)).distinct
    val compensationSummary = signalDigest.flatMap(_.compensation).map(normalizeText).filter(_.nonEmpty)
    val compensationVectors =
      signalDigest.toList.flatMap(_.compensationVectors.map(normalizeText).filter(_.nonEmpty)).distinct
    val investedMaterial = signalDigest.flatMap(_.investedMaterial).filter(_ > 0)
    val compensationSubtype =
      deriveCompensationSubtype(
        dominantIdea = dominantIdea,
        secondaryIdea = secondaryIdea,
        allRoutes = allRoutes,
        topRoute = topRoute,
        allMoveRefs = allMoveRefs,
        topMoveRef = topMoveRef,
        allDirectionalTargets = allDirectionalTargets,
        topDirectionalTarget = topDirectionalTarget,
        longTermFocus = longTermFocus,
        compensationSummary = compensationSummary,
        compensationVectors = compensationVectors,
        investedMaterial = investedMaterial
      )

    val rawSnapshot =
      Snapshot(
      sideToMove = sideToMove,
      dominantIdea = dominantIdea,
      secondaryIdea = secondaryIdea,
      campaignOwner = campaignOwner,
      ownerMismatch = ownerMismatch,
      allRoutes = allRoutes,
      topRoute = topRoute,
      allMoveRefs = allMoveRefs,
        topMoveRef = topMoveRef,
        allDirectionalTargets = allDirectionalTargets,
        topDirectionalTarget = topDirectionalTarget,
        longTermFocus = longTermFocus,
        evidenceHints = evidenceHints,
        compensationSummary = compensationSummary,
        compensationVectors = compensationVectors,
        investedMaterial = investedMaterial,
        compensationSubtype = compensationSubtype
    )
    rawSnapshot.copy(displayNormalization = deriveDisplayNormalization(rawSnapshot))

  def sideLabel(side: String): String =
    if normalizeSide(side) == "black" then "Black" else "White"

  def compensationSubtypeLabel(surface: Snapshot): Option[String] =
    surface.effectiveCompensationSubtype.map(compensationSubtypeLabel)

  def preparationCompensationSubtypeLabel(surface: Snapshot): Option[String] =
    surface.preparationCompensationSubtype.map(compensationSubtypeLabel)

  def payoffCompensationSubtypeLabel(surface: Snapshot): Option[String] =
    surface.payoffCompensationSubtype.map(compensationSubtypeLabel)

  def strictCompensationSubtypeLabel(surface: Snapshot): Option[String] =
    surface.strictCompensationSubtype.map(compensationSubtypeLabel)

  def resolvedDisplaySubtypeSource(surface: Snapshot): String =
    surface.displayNormalization.map(_.displaySubtypeSource).getOrElse("raw_fallback")

  def resolvedNormalizedCompensationLead(surface: Snapshot): Option[String] =
    surface.displayNormalization.filter(_.normalizationActive).flatMap(_.normalizedCompensationLead)

  def resolvedNormalizedExecutionText(surface: Snapshot): Option[String] =
    surface.displayNormalization.filter(_.normalizationActive).flatMap(_.normalizedExecutionText)

  def resolvedNormalizedObjectiveText(surface: Snapshot): Option[String] =
    surface.displayNormalization.filter(_.normalizationActive).flatMap(_.normalizedObjectiveText)

  def resolvedNormalizedLongTermFocusText(surface: Snapshot): Option[String] =
    surface.displayNormalization.filter(_.normalizationActive).flatMap(_.normalizedLongTermFocusText)

  private def compensationSubtypeLabel(subtype: CompensationSubtype): String =
    s"${subtype.pressureTheater}/${subtype.pressureMode}/${subtype.recoveryPolicy}/${subtype.stabilityClass}"

  private def resolvedCompensationContract(normalization: DisplayNormalization): Boolean =
    normalization.normalizationActive &&
      normalization.displaySubtypeSource != "raw_fallback" &&
      normalization.selectedDisplaySubtype.nonEmpty &&
      !unresolvedPathPayoffConflict(normalization)

  private def unresolvedPathPayoffConflict(normalization: DisplayNormalization): Boolean =
    (normalization.preparationSubtype, normalization.payoffSubtype, normalization.selectedDisplaySubtype) match
      case (Some(path), Some(payoff), Some(selected)) if path != payoff =>
        selected != path && selected != payoff
      case _ => false

  private def preferRawAttackDisplay(surface: Snapshot): Boolean =
    val strongDisplayOverride =
      surface.displayNormalization.exists(norm =>
        norm.normalizationActive &&
          norm.normalizationConfidence >= 8 &&
          norm.normalizedCompensationSubtype.exists(normalized =>
            surface.compensationSubtype.exists(raw =>
              normalized != raw &&
                (
                  normalized.recoveryPolicy != raw.recoveryPolicy ||
                    normalized.pressureMode != raw.pressureMode ||
                    normalized.pressureTheater != raw.pressureTheater
                )
            )
          )
      )
    !strongDisplayOverride &&
      surface.dominantIdea.exists(_.kind == StrategicIdeaKind.KingAttackBuildUp) &&
      surface.dominantIdea.flatMap(_.focusZone).exists(zone => normalizeText(zone).equalsIgnoreCase("kingside")) &&
      surface.compensationSubtype.exists(subtype =>
        subtype.pressureTheater == "kingside" &&
          subtype.pressureMode == "line_occupation" &&
          subtype.recoveryPolicy == "immediate"
      ) &&
      (
        surface.compensationSummary.exists(_.toLowerCase.contains("initiative")) ||
          surface.compensationVectors.exists(_.toLowerCase.contains("initiative"))
      )

  private def deriveDisplayNormalization(surface: Snapshot): Option[DisplayNormalization] =
    surface.compensationSubtype.map(subtype =>
      CompensationDisplayPhrasing.buildDisplayNormalization(
        surface = surface,
        rawSubtype = subtype,
        resolution = CompensationDisplaySubtypeResolver.resolve(surface, subtype)
      )
    )

  private[analysis] object CompensationDisplaySubtypeResolver:

    final case class DisplaySubtypeResolution(
        preparationSubtype: CompensationSubtype,
        payoffSubtype: CompensationSubtype,
        selectedDisplaySubtype: Option[CompensationSubtype],
        displaySubtypeSource: String,
        payoffConfidence: Int,
        pathConfidence: Int,
        normalizationConfidence: Int,
        normalizationActive: Boolean
    )

    def resolve(
        surface: Snapshot,
        rawSubtype: CompensationSubtype
    ): DisplaySubtypeResolution =
      val preparationSubtype = derivePreparationCompensationSubtype(surface, rawSubtype).getOrElse(rawSubtype)
      val derivedPayoffSubtype =
        derivePayoffCompensationSubtype(surface, rawSubtype, preparationSubtype).getOrElse(preparationSubtype)
      val pathCarrierStrength = subtypeCarrierStrength(surface, preparationSubtype)
      val rawPayoffCarrierStrength = subtypeCarrierStrength(surface, derivedPayoffSubtype)
      val pathConfidence = normalizationConfidence(surface, preparationSubtype)
      val rawPayoffConfidence = payoffNormalizationConfidence(surface, derivedPayoffSubtype)
      val fixedTargetingDisplayLock =
        preparationSubtype.pressureMode == "target_fixing" &&
          derivedPayoffSubtype.pressureMode == "line_occupation" &&
          (
            explicitTargetFixingFocus(surface) ||
              fixedTargetPlanCarrier(surface) ||
              repeatedTargetPawnCount(surface) >= 1 ||
              sameTheaterTargetFixingEvidence(surface, preparationSubtype.pressureTheater) >= 2
          )
      val fixedTargetingTheaterLock =
        preparationSubtype.pressureMode == "target_fixing" &&
          derivedPayoffSubtype.pressureMode == "target_fixing" &&
          preparationSubtype.pressureTheater != derivedPayoffSubtype.pressureTheater &&
          (
            explicitTargetFixingFocus(surface) ||
              fixedTargetPlanCarrier(surface) ||
              repeatedTargetPawnCount(surface) >= 1 ||
              targetFixingIdeaAnchorTheaters(surface).nonEmpty
          )
      val durablePathAnchorLock =
        preparationSubtype.stabilityClass == "durable_pressure" &&
          preparationSubtype.recoveryPolicy != "immediate" &&
          derivedPayoffSubtype != preparationSubtype &&
          pathCarrierStrength >= 3 &&
          (
            pathCarrierStrength >= rawPayoffCarrierStrength + 1 ||
              pathConfidence >= rawPayoffConfidence
          )
      val payoffSubtype =
        if fixedTargetingDisplayLock || fixedTargetingTheaterLock || durablePathAnchorLock then preparationSubtype
        else derivedPayoffSubtype
      val payoffCarrierStrength = subtypeCarrierStrength(surface, payoffSubtype)
      val payoffConfidence =
        if payoffSubtype == preparationSubtype then pathConfidence
        else rawPayoffConfidence
      val pathSelectedAtLowerConfidence =
        preparationSubtype != rawSubtype &&
          pathCarrierStrength >= 3 &&
          (
            fixedTargetingDisplayLock ||
              fixedTargetingTheaterLock ||
              durablePathAnchorLock
          ) &&
          pathConfidence >= 4
      val preferPayoffSource =
        !fixedTargetingDisplayLock &&
          !fixedTargetingTheaterLock &&
          !durablePathAnchorLock &&
          payoffCarrierStrength > 0 &&
          payoffConfidence >= 6 &&
          (
            (
              preparationSubtype != rawSubtype &&
                payoffSubtype != preparationSubtype &&
                payoffConfidence >= pathConfidence + 2 &&
                payoffCarrierStrength >= pathCarrierStrength + 1
            ) ||
              (
                preparationSubtype == rawSubtype &&
                  (
                    payoffSubtype != preparationSubtype ||
                      (
                        payoffSubtype.pressureMode == "target_fixing" &&
                          (fixedTargetPlanCarrier(surface) || explicitTargetFixingFocus(surface))
                      )
                  )
              )
          )
      val selectedSource =
        if preferPayoffSource then "payoff"
        else if preparationSubtype != rawSubtype && (pathConfidence >= 6 || pathSelectedAtLowerConfidence) then "path"
        else "raw_fallback"
      val selectedSubtype =
        selectedSource match
          case "payoff" => payoffSubtype
          case "path"   => preparationSubtype
          case _        => rawSubtype
      val confidence =
        selectedSource match
          case "payoff" => payoffConfidence
          case "path"   => pathConfidence
          case _        => 0
      val quietTargetingRescue =
        selectedSource != "raw_fallback" &&
          selectedSubtype != rawSubtype &&
          selectedSubtype.pressureMode == "target_fixing" &&
          selectedSubtype.recoveryPolicy == "intentionally_deferred" &&
          selectedSubtype.stabilityClass == "durable_pressure" &&
          (
            explicitTargetFixingFocus(surface) ||
              repeatedCenterTargetShell(surface) ||
              sameTheaterTargetFixingEvidence(surface, selectedSubtype.pressureTheater) >= 3
          )
      val active =
        surface.compensationPosition &&
          !selectedSubtype.transitionOnly &&
          (confidence >= 6 || (quietTargetingRescue && confidence >= 4))
      DisplaySubtypeResolution(
        preparationSubtype = preparationSubtype,
        payoffSubtype = payoffSubtype,
        selectedDisplaySubtype = Option.when(active && selectedSource != "raw_fallback")(selectedSubtype),
        displaySubtypeSource = selectedSource,
        payoffConfidence = payoffConfidence,
        pathConfidence = pathConfidence,
        normalizationConfidence = confidence,
        normalizationActive = active
      )

    private def subtypeCarrierStrength(
        surface: Snapshot,
        subtype: CompensationSubtype
    ): Int =
      val alignedRouteWeight = Option.when(alignedRoute(surface, subtype).nonEmpty)(2).getOrElse(0)
      val alignedMoveRefWeight = Option.when(alignedMoveRef(surface, subtype).nonEmpty)(2).getOrElse(0)
      val alignedTargetWeight = Option.when(alignedDirectionalTarget(surface, subtype).nonEmpty)(1).getOrElse(0)
      val modeAnchorWeight =
        subtype.pressureMode match
          case "target_fixing" =>
            sameTheaterTargetFixingEvidence(surface, subtype.pressureTheater).min(3) +
              repeatedTargetPawnCount(surface).min(2) +
              Option.when(explicitTargetFixingFocus(surface) || fixedTargetPlanCarrier(surface))(2).getOrElse(0)
          case "line_occupation" =>
            sameTheaterOpenFileRouteCount(surface, subtype.pressureTheater).min(3)
          case "counterplay_denial" =>
            Option.when(counterplayAnchorStrength(displaySignalTexts(surface)) > 0)(2).getOrElse(0)
          case _ => 0
      val stabilityWeight =
        Option.when(subtype.stabilityClass == "durable_pressure")(1).getOrElse(0) +
          Option.when(subtype.recoveryPolicy == "intentionally_deferred")(1).getOrElse(0)
      alignedRouteWeight + alignedMoveRefWeight + alignedTargetWeight + modeAnchorWeight + stabilityWeight
  private def derivePreparationCompensationSubtype(
      surface: Snapshot,
      rawSubtype: CompensationSubtype
  ): Option[CompensationSubtype] =
    val signalTexts = displaySignalTexts(surface)
    val targetAnchors = targetFixingAnchorStrength(surface, signalTexts)
    val lineAnchors = lineOccupationAnchorStrength(surface, signalTexts)
    val counterplayAnchors = counterplayAnchorStrength(signalTexts)

    val normalizedTheater =
      normalizedPressureTheater(surface, signalTexts).filter(_ != "mixed").getOrElse(rawSubtype.pressureTheater)
    val sameTheaterTargetEvidence =
      sameTheaterTargetFixingEvidence(surface, normalizedTheater)
    val repeatedTargetPawnRefs = repeatedTargetPawnCount(surface)
    val lateTargetEvidence = lateTargetFixingEvidenceCount(surface)
    val sameTheaterLineRoutes = sameTheaterOpenFileRouteCount(surface, normalizedTheater)
    val normalizedMode =
      normalizedPressureMode(
        surface = surface,
        rawSubtype = rawSubtype,
        targetAnchors = targetAnchors,
        lineAnchors = lineAnchors,
        counterplayAnchors = counterplayAnchors,
        sameTheaterTargetEvidence = sameTheaterTargetEvidence,
        repeatedTargetPawnRefs = repeatedTargetPawnRefs,
        lateTargetEvidence = lateTargetEvidence,
        sameTheaterLineRoutes = sameTheaterLineRoutes,
        hasOpenFileOccupationRoute = surface.allRoutes.exists(route =>
          containsAny(normalizeText(route.purpose).toLowerCase, List("open-file occupation", "file occupation", "file pressure", "open file"))
        ),
        focusTargetFixingHint = explicitTargetFixingFocus(surface),
        repeatedCenterTargetShell = repeatedCenterTargetShell(surface)
      )
    val displayTheater =
      if normalizedMode == "target_fixing" then
        targetFixingTheaterOverride(surface).filter(_ != "mixed").getOrElse(normalizedTheater)
      else normalizedTheater
    val normalizedRecovery =
      normalizedRecoveryPolicy(surface, rawSubtype, normalizedMode, signalTexts)
    val normalizedStability =
      normalizedStabilityClass(
        rawSubtype = rawSubtype,
        normalizedMode = normalizedMode,
        normalizedRecoveryPolicy = normalizedRecovery,
        targetAnchors = targetAnchors,
        lineAnchors = lineAnchors,
        counterplayAnchors = counterplayAnchors
      )

    val normalizedSubtype =
      rawSubtype.copy(
        pressureTheater = displayTheater,
        pressureMode = normalizedMode,
        recoveryPolicy = normalizedRecovery,
        stabilityClass = normalizedStability
      )

    Option.when(normalizedSubtype != rawSubtype)(normalizedSubtype)

  private def derivePayoffCompensationSubtype(
      surface: Snapshot,
      rawSubtype: CompensationSubtype,
      preparationSubtype: CompensationSubtype
  ): Option[CompensationSubtype] =
    val signalTexts = payoffSignalTexts(surface)
    val targetAnchors = payoffTargetFixingAnchorStrength(surface, signalTexts)
    val lineAnchors = payoffLineOccupationAnchorStrength(surface, signalTexts)
    val counterplayAnchors = counterplayAnchorStrength(signalTexts)
    val normalizedMode =
      payoffPressureMode(
        surface = surface,
        rawSubtype = rawSubtype,
        preparationSubtype = preparationSubtype,
        targetAnchors = targetAnchors,
        lineAnchors = lineAnchors,
        counterplayAnchors = counterplayAnchors
      )
    val normalizedTheater =
      payoffPressureTheater(surface, normalizedMode, signalTexts)
        .filter(_ != "mixed")
        .getOrElse(preparationSubtype.pressureTheater)
    val normalizedRecovery =
      normalizedRecoveryPolicy(surface, rawSubtype, normalizedMode, signalTexts)
    val normalizedStability =
      normalizedStabilityClass(
        rawSubtype = rawSubtype,
        normalizedMode = normalizedMode,
        normalizedRecoveryPolicy = normalizedRecovery,
        targetAnchors = targetAnchors,
        lineAnchors = lineAnchors,
        counterplayAnchors = counterplayAnchors
      )
    val normalizedSubtype =
      rawSubtype.copy(
        pressureTheater = normalizedTheater,
        pressureMode = normalizedMode,
        recoveryPolicy = normalizedRecovery,
        stabilityClass = normalizedStability
      )
    Option.when(normalizedSubtype != rawSubtype)(normalizedSubtype)

  private def prioritizeByOwner[A](items: List[A], owner: Option[String], ownerOf: A => String): List[A] =
    val normalizedOwner = owner.map(normalizeSide)
    items.sortBy(item => if normalizedOwner.contains(normalizeSide(ownerOf(item))) then 0 else 1)

  private def normalizeSide(side: String): String =
    Option(side).map(_.trim.toLowerCase).filter(_.nonEmpty).getOrElse("white")

  private def isHiddenRoute(route: StrategyPieceRoute): Boolean =
    normalizeText(route.surfaceMode).equalsIgnoreCase(RouteSurfaceMode.Hidden)

  private def ideaText(idea: StrategyIdeaSignal): Option[String] =
    val label = normalizeText(StrategicIdeaSelector.playerFacingIdeaText(idea))
    Option.when(label.nonEmpty)(label)

  private def routeText(route: StrategyPieceRoute): Option[String] =
    val routeSquares = route.route.map(normalizeText).filter(_.nonEmpty)
    val destination = routeSquares.lastOption
    val purpose = playerFacingRoutePurpose(route.purpose)
    destination.map { square =>
      val piece = pieceName(route.piece)
      if normalizeText(route.surfaceMode).equalsIgnoreCase(RouteSurfaceMode.Exact) && routeSquares.size >= 2 then
        val path = routeSquares.mkString("-")
        (List(s"$piece via $path") ++ purpose.map(p => s"for $p")).mkString(" ")
      else (List(s"$piece toward $square") ++ purpose.map(p => s"for $p")).mkString(" ")
    }

  private def moveRefText(moveRef: StrategyPieceMoveRef): Option[String] =
    val piece = pieceName(moveRef.piece)
    val target = normalizeText(moveRef.target)
    val idea = normalizeText(moveRef.idea)
    Option.when(target.nonEmpty) {
      (List(s"$piece toward $target") ++ Option.when(idea.nonEmpty)(s"for $idea").toList).mkString(" ")
    }

  private def targetText(target: StrategyDirectionalTarget): Option[String] =
    val piece = pieceName(target.piece)
    val square = normalizeText(target.targetSquare)
    Option.when(square.nonEmpty) {
      s"the $piece can head for $square"
    }

  private def playerFacingRoutePurpose(raw: String): Option[String] =
    val rewritten = normalizeText(LiveNarrativeCompressionCore.rewritePlayerLanguage(raw))
    Option.when(
      rewritten.nonEmpty &&
        !rewritten.equalsIgnoreCase("the main plan") &&
        !rewritten.equalsIgnoreCase("better piece coordination") &&
        !rewritten.equalsIgnoreCase("the current structure")
    )(rewritten)

  private[analysis] def pieceName(code: String): String =
    normalizeText(code).toUpperCase match
      case "P" | "PAWN"   => "pawn"
      case "N" | "KNIGHT" => "knight"
      case "B" | "BISHOP" => "bishop"
      case "R" | "ROOK"   => "rook"
      case "Q" | "QUEEN"  => "queen"
      case "K" | "KING"   => "king"
      case other          => normalizeText(other).toLowerCase

  private[analysis] def normalizeText(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""[_\-]+""", " ").replaceAll("\\s+", " ").trim

  private def payoffSignalTexts(surface: Snapshot): List[String] =
    (
      surface.longTermFocus.toList ++
        surface.evidenceHints ++
        surface.compensationSummary.toList ++
        surface.compensationVectors
    ).map(normalizeText).filter(_.nonEmpty).map(_.toLowerCase).distinct

  private def displaySignalTexts(surface: Snapshot): List[String] =
    (
      List(
        surface.rawDominantIdeaText,
        surface.rawSecondaryIdeaText,
        surface.rawExecutionText,
        surface.compensationSummary
      ).flatten ++
        surface.compensationVectors ++
        surface.allRoutes.flatMap(routeText) ++
        surface.allMoveRefs.flatMap(moveRefText) ++
        surface.allDirectionalTargets.flatMap(targetText)
    ).map(normalizeText).filter(_.nonEmpty).map(_.toLowerCase).distinct

  private def payoffTargetFixingAnchorStrength(
      surface: Snapshot,
      signalTexts: List[String]
  ): Int =
    val moveRefScore =
      surface.allMoveRefs.map { moveRef =>
        val idea = normalizeText(moveRef.idea).toLowerCase
        val evidence = moveRef.evidence.map(normalizeText).map(_.toLowerCase)
        if evidence.contains("target_pawn") then 4
        else if containsAny(idea, List("contest the pawn", "fixed target", "fixed pawn", "weak pawn", "backward pawn", "fix the"))
        then 3
        else 0
      }.sum
    val fixedTargetTextScore =
      signalTexts.count(text =>
        containsAny(
          text,
          List(
            "fixed target",
            "fixed targets",
            "fixed central targets",
            "fixed queenside targets",
            "fix the",
            "contest the pawn",
            "weak pawn",
            "backward pawn",
            "attacking fixed pawn"
          )
        )
      ) * 3
    val planCarrierScore =
      (if fixedTargetPlanCarrier(surface) then 4 else 0) +
        (if explicitTargetFixingFocus(surface) then 4 else 0) +
        (if repeatedTargetPawnCount(surface) >= 2 then 3 else 0)
    val directionalScore =
      surface.allDirectionalTargets.count { target =>
        target.strategicReasons.map(normalizeText).map(_.toLowerCase).exists(reason =>
          containsAny(
            reason,
            List(
              "weak pawn",
              "backward pawn",
              "fixed target",
              "fixed pawn",
              "attacking fixed pawn",
              "static weakness fixation"
            )
          )
        )
      } * 2
    moveRefScore + fixedTargetTextScore + planCarrierScore + directionalScore

  private def payoffLineOccupationAnchorStrength(
      surface: Snapshot,
      signalTexts: List[String]
  ): Int =
    val focusScore =
      signalTexts.count(text =>
        containsAny(
          text,
          List(
            "queenside file pressure",
            "central file pressure",
            "line pressure",
            "file pressure",
            "open line",
            "open-file control",
            "open-line pressure",
            "central file control",
            "files under pressure"
          )
        )
      ) * 3
    val vectorScore =
      surface.compensationVectors.count(vector =>
        containsAny(normalizeText(vector).toLowerCase, List("line pressure", "open file", "file pressure"))
      ) * 2
    val dominantScore =
      (if surface.dominantIdea.exists(_.kind == StrategicIdeaKind.LineOccupation) then 2 else 0) +
        (if surface.secondaryIdea.exists(_.kind == StrategicIdeaKind.LineOccupation) then 1 else 0)
    focusScore + vectorScore + dominantScore

  private def payoffPressureMode(
      surface: Snapshot,
      rawSubtype: CompensationSubtype,
      preparationSubtype: CompensationSubtype,
      targetAnchors: Int,
      lineAnchors: Int,
      counterplayAnchors: Int
  ): String =
    val fixedPlan = fixedTargetPlanCarrier(surface)
    val explicitTargetFocus = explicitTargetFixingFocus(surface)
    val repeatedTargets = repeatedTargetPawnCount(surface)
    val targetFixingIdeaPresent =
      surface.dominantIdea.exists(_.kind == StrategicIdeaKind.TargetFixing) ||
        surface.secondaryIdea.exists(_.kind == StrategicIdeaKind.TargetFixing)
    val lineOccupationLock =
      preparationSubtype.pressureMode == "line_occupation" &&
        !fixedPlan &&
        !explicitTargetFocus &&
        !targetFixingIdeaPresent &&
        targetAnchors < lineAnchors + 2 &&
        (
          surface.dominantIdea.exists(_.kind == StrategicIdeaKind.LineOccupation) ||
            surface.secondaryIdea.exists(_.kind == StrategicIdeaKind.LineOccupation)
        )
    if counterplayAnchors > math.max(targetAnchors, lineAnchors) then "counterplay_denial"
    else if lineOccupationLock then "line_occupation"
    else if targetAnchors >= lineAnchors + 2 && targetAnchors >= 5 then "target_fixing"
    else if fixedPlan && (explicitTargetFocus || repeatedTargets >= 1) && targetAnchors + 1 >= lineAnchors
    then "target_fixing"
    else if explicitTargetFocus && targetAnchors + 3 >= lineAnchors then "target_fixing"
    else if lineAnchors >= 4 then "line_occupation"
    else if preparationSubtype.pressureMode != rawSubtype.pressureMode then preparationSubtype.pressureMode
    else rawSubtype.pressureMode

  private def payoffPressureTheater(
      surface: Snapshot,
      normalizedMode: String,
      signalTexts: List[String]
  ): Option[String] =
    normalizedMode match
      case "target_fixing"   => targetFixingPayoffTheater(surface, signalTexts)
      case "line_occupation" => lineOccupationPayoffTheater(surface, signalTexts)
      case _                 => explicitPayoffTheater(signalTexts)

  private def explicitPayoffTheater(signalTexts: List[String]): Option[String] =
    val hits = scala.collection.mutable.Map("queenside" -> 0, "kingside" -> 0, "center" -> 0)

    def add(theater: String, score: Int): Unit =
      if hits.contains(theater) && score > 0 then hits.update(theater, hits(theater) + score)

    signalTexts.foreach { text =>
      if containsAny(text, List("fixed queenside targets", "queenside targets", "queenside file pressure", "queenside files under pressure")) then
        add("queenside", 5)
      if containsAny(text, List("fixed central targets", "central targets", "central file pressure", "central files under control", "central files under pressure")) then
        add("center", 5)
      if containsAny(text, List("fixed kingside targets", "kingside targets", "kingside file pressure")) then
        add("kingside", 5)
      if containsAny(text, List("queenside", "queen side", "b-file", "c-file")) then add("queenside", 2)
      if containsAny(text, List("central", "center", "d-file", "e-file")) then add("center", 2)
      if containsAny(text, List("kingside", "king side", "f-file", "g-file", "h-file")) then add("kingside", 2)
    }

    choosePayoffTheater(hits.toMap, Nil)

  private def targetFixingPayoffTheater(
      surface: Snapshot,
      signalTexts: List[String]
  ): Option[String] =
    val hits = scala.collection.mutable.Map("queenside" -> 0, "kingside" -> 0, "center" -> 0)

    def add(theater: String, score: Int): Unit =
      if hits.contains(theater) && score > 0 then hits.update(theater, hits(theater) + score)

    explicitPayoffTheater(signalTexts).foreach(add(_, 4))
    targetFixingIdeaAnchorTheaters(surface).foreach(add(_, 4))
    objectiveAnchorTheaters(surface).foreach(add(_, 3))
    val orderedAnchors = payoffTargetAnchorTheaters(surface)
    orderedAnchors.foreach(add(_, 3))

    choosePayoffTheater(hits.toMap, orderedAnchors)

  private def lineOccupationPayoffTheater(
      surface: Snapshot,
      signalTexts: List[String]
  ): Option[String] =
    explicitPayoffTheater(signalTexts).orElse {
      val hits = scala.collection.mutable.Map("queenside" -> 0, "kingside" -> 0, "center" -> 0)

      def add(theater: String, score: Int): Unit =
        if hits.contains(theater) && score > 0 then hits.update(theater, hits(theater) + score)

      signalTexts.foreach { text =>
        if containsAny(text, List("queenside file pressure", "queenside files under pressure")) then add("queenside", 4)
        if containsAny(text, List("central file pressure", "central files under control", "central files under pressure")) then add("center", 4)
      }
      surface.allDirectionalTargets.foreach { target =>
        val reasons = target.strategicReasons.map(normalizeText).map(_.toLowerCase)
        if reasons.exists(reason => containsAny(reason, List("line access", "file access", "open line"))) then
          theaterFromSquare(target.targetSquare).foreach(add(_, 1))
      }
      choosePayoffTheater(hits.toMap, Nil)
    }

  private def payoffTargetAnchorTheaters(surface: Snapshot): List[String] =
    val allowFixedPlanWeakAnchors = fixedTargetPlanCarrier(surface)
    val ideaAnchors = targetFixingIdeaAnchorTheaters(surface)
    val objectiveAnchors = objectiveAnchorTheaters(surface)
    val moveRefAnchors =
      surface.allMoveRefs.flatMap { moveRef =>
        val idea = normalizeText(moveRef.idea).toLowerCase
        val evidence = moveRef.evidence.map(normalizeText).map(_.toLowerCase)
        val strongTarget =
          evidence.contains("target_pawn") ||
            containsAny(
              idea,
              List("contest the pawn", "fixed target", "fixed pawn", "weak pawn", "backward pawn", "attacking fixed pawn", "fix the")
            )
        val weakFixedPlanTarget =
          allowFixedPlanWeakAnchors &&
            moveRef.piece != "K" &&
            evidence.contains("enemy_occupied_endpoint")
        Option.when(strongTarget || weakFixedPlanTarget)(moveRef.target).flatMap(theaterFromSquare)
      }
    val directionalAnchors =
      surface.allDirectionalTargets.flatMap { target =>
        val reasons = target.strategicReasons.map(normalizeText).map(_.toLowerCase)
        val strongTarget =
          reasons.exists(reason =>
            containsAny(
              reason,
              List(
                "weak pawn",
                "backward pawn",
                "fixed target",
                "fixed pawn",
                "attacking fixed pawn",
                "static weakness fixation"
              )
            )
          )
        val weakFixedPlanTarget =
          allowFixedPlanWeakAnchors &&
            reasons.exists(reason => containsAny(reason, List("supports attacking fixed pawn", "fixed target", "weak pawn", "backward pawn")))
        Option.when(strongTarget || weakFixedPlanTarget)(target.targetSquare).flatMap(theaterFromSquare)
      }
    ideaAnchors ++ objectiveAnchors ++ moveRefAnchors ++ directionalAnchors

  private def targetFixingIdeaAnchorTheaters(surface: Snapshot): List[String] =
    List(surface.dominantIdea, surface.secondaryIdea).flatten.flatMap { idea =>
      Option.when(idea.kind == StrategicIdeaKind.TargetFixing) {
        idea.focusSquares.flatMap(theaterFromSquare) ++
          idea.focusFiles.flatMap(theaterFromFile) ++
          idea.focusZone.toList.flatMap(zone => normalizeTheaterToken(normalizeText(zone).toLowerCase))
      }.getOrElse(Nil)
    }

  private def objectiveAnchorTheaters(surface: Snapshot): List[String] =
    surface.rawObjectiveText.toList.flatMap(theatersFromText)

  private def theatersFromText(text: String): List[String] =
    val lowered = normalizeText(text).toLowerCase
    val squareAnchors =
      """\b[a-h][1-8]\b""".r.findAllIn(lowered).toList.flatMap(theaterFromSquare)
    val tokenAnchors =
      List(
        Option.when(containsAny(lowered, List("queenside", "queen side", "b-file", "c-file")))("queenside"),
        Option.when(containsAny(lowered, List("central", "center", "d-file", "e-file")))("center"),
        Option.when(containsAny(lowered, List("kingside", "king side", "f-file", "g-file", "h-file")))("kingside")
      ).flatten
    squareAnchors ++ tokenAnchors

  private def choosePayoffTheater(
      hits: Map[String, Int],
      orderedAnchors: List[String]
  ): Option[String] =
    val ranked = hits.toList.sortBy { case (_, score) => -score }
    ranked match
      case (topTheater, topScore) :: (_, secondScore) :: _ if topScore >= 3 && topScore >= secondScore + 1 =>
        Some(topTheater)
      case (topTheater, topScore) :: _ if topScore >= 4 && orderedAnchors.headOption.contains(topTheater) =>
        Some(topTheater)
      case (topTheater, topScore) :: _ if topScore >= 4 && orderedAnchors.nonEmpty =>
        orderedAnchors.find(theater => hits.getOrElse(theater, 0) == topScore).orElse(Some(topTheater))
      case (topTheater, topScore) :: Nil if topScore >= 3 =>
        Some(topTheater)
      case _ =>
        orderedAnchors.headOption

  private def payoffNormalizationConfidence(
      surface: Snapshot,
      subtype: CompensationSubtype
  ): Int =
    val signalTexts = payoffSignalTexts(surface)
    val targetAnchors = payoffTargetFixingAnchorStrength(surface, signalTexts)
    val lineAnchors = payoffLineOccupationAnchorStrength(surface, signalTexts)
    val counterplayAnchors = counterplayAnchorStrength(signalTexts)
    val structuralAnchors =
      subtype.pressureMode match
        case "target_fixing"     => targetAnchors
        case "line_occupation"   => lineAnchors
        case "counterplay_denial" => counterplayAnchors
        case _                   => math.max(targetAnchors, lineAnchors)
    val base =
      (if surface.compensationPosition then 1 else 0) +
        (if subtype.pressureTheater != "mixed" then 3 else 0) +
        (if subtype.durablePressure then 2 else 0) +
        (if subtype.recoveryPolicy == "intentionally_deferred" then 2
         else if subtype.recoveryPolicy == "delayed" then 1
         else 0)
    val anchorBoost =
      math.min(4, structuralAnchors / 2) +
        (if fixedTargetPlanCarrier(surface) && subtype.pressureMode == "target_fixing" then 2 else 0)
    val transitionPenalty =
      (if subtype.transitionOnly then 4 else 0) +
        (if subtype.stabilityClass == "tactical_window" then 2 else 0)
    base + anchorBoost - transitionPenalty

  private def normalizedPressureTheater(
      surface: Snapshot,
      signalTexts: List[String]
  ): Option[String] =
    directionalTargetTheaterOverride(surface).orElse {
    val theaterHits = scala.collection.mutable.Map("queenside" -> 0, "kingside" -> 0, "center" -> 0)

    def addHit(theater: String, weight: Int): Unit =
      if theaterHits.contains(theater) && weight > 0 then theaterHits.update(theater, theaterHits(theater) + weight)

    def addSquares(squares: List[String], weight: Int): Unit =
      squares.flatMap(theaterFromSquare).foreach(addHit(_, weight))

    def addFiles(files: List[String], weight: Int): Unit =
      files.flatMap(theaterFromFile).foreach(addHit(_, weight))

    addSquares(surface.allDirectionalTargets.map(_.targetSquare), 4)
    addSquares(surface.allMoveRefs.map(_.target), 3)
    addSquares(surface.allRoutes.flatMap(_.route.lastOption), 3)
    addFiles(surface.dominantIdea.toList.flatMap(_.focusFiles), 1)
    addFiles(surface.secondaryIdea.toList.flatMap(_.focusFiles), 1)
    addSquares(surface.dominantIdea.toList.flatMap(_.focusSquares), 1)
    addSquares(surface.secondaryIdea.toList.flatMap(_.focusSquares), 1)
    surface.dominantIdea.flatMap(_.focusZone).map(normalizeText).map(_.toLowerCase).flatMap(normalizeTheaterToken).foreach(addHit(_, 2))
    surface.secondaryIdea.flatMap(_.focusZone).map(normalizeText).map(_.toLowerCase).flatMap(normalizeTheaterToken).foreach(addHit(_, 1))

    signalTexts.foreach { text =>
      if containsAny(text, List("queenside", "queen side", "queenside files", "queenside targets", "b-file", "c-file")) then
        addHit("queenside", 2)
      if containsAny(text, List("kingside", "king side", "g-file", "h-file", "f-file")) then
        addHit("kingside", 2)
      if containsAny(text, List("central", "center", "d-file", "e-file")) then
        addHit("center", 2)
    }

      Option(chooseTheater(theaterHits.toMap)).filter(_ != "mixed")
    }

  private def directionalTargetTheaterOverride(surface: Snapshot): Option[String] =
    Option.when(surface.allRoutes.isEmpty && surface.allMoveRefs.isEmpty && surface.allDirectionalTargets.size >= 3) {
      val counts =
        surface.allDirectionalTargets
          .flatMap(target => theaterFromSquare(target.targetSquare))
          .groupMapReduce(identity)(_ => 1)(_ + _)
      val ranked = counts.toList.sortBy { case (_, score) => -score }
      ranked match
        case (topTheater, topScore) :: (_, secondScore) :: _ if topScore >= 2 && topScore >= secondScore + 1 =>
          topTheater
        case (topTheater, topScore) :: Nil if topScore >= 2 =>
          topTheater
        case _ =>
          "mixed"
    }.filter(_ != "mixed")

  private def sameTheaterTargetFixingEvidence(
      surface: Snapshot,
      theater: String
  ): Int =
    if theater == "mixed" then 0
    else
      val moveRefHits =
        surface.allMoveRefs.count { moveRef =>
          theaterFromSquare(moveRef.target).contains(theater) &&
            (
              moveRef.evidence.map(normalizeText).map(_.toLowerCase).contains("target_pawn") ||
                containsAny(
                  normalizeText(moveRef.idea).toLowerCase,
                  List("contest the pawn", "fixed target", "fixed pawn", "weak pawn", "backward pawn", "attacking fixed pawn")
                )
            )
        }
      val targetHits =
        surface.allDirectionalTargets.count { target =>
          theaterFromSquare(target.targetSquare).contains(theater) &&
            target.strategicReasons.map(normalizeText).map(_.toLowerCase).exists(reason =>
              containsAny(
                reason,
                List("weak pawn", "backward pawn", "fixed target", "fixed pawn", "attacking fixed pawn", "static weakness fixation")
              )
            )
        }
      moveRefHits + (targetHits * 2)

  private def explicitTargetFixingFocus(surface: Snapshot): Boolean =
    surface.longTermFocus.toList
      .map(normalizeText)
      .map(_.toLowerCase)
      .exists(text =>
        containsAny(
          text,
          List(
            "fixed target",
            "fixed targets",
            "targets fixed",
            "fix the",
            "contest the pawn",
            "weak pawn",
            "backward pawn",
            "attacking fixed pawn"
          )
        )
      )

  private def repeatedCenterTargetShell(surface: Snapshot): Boolean =
    val repeatedTargetPawnRefs =
      surface.allMoveRefs.count(moveRef =>
        moveRef.evidence.map(normalizeText).map(_.toLowerCase).contains("target_pawn")
      ) >= 2
    val centeredOpenFileRoutes =
      surface.allRoutes.count { route =>
        containsAny(normalizeText(route.purpose).toLowerCase, List("open-file occupation", "file occupation", "file pressure", "open file")) &&
          route.route.lastOption.flatMap(theaterFromSquare).contains("center")
      } >= 2
    repeatedTargetPawnRefs && centeredOpenFileRoutes

  private def targetFixingAnchorStrength(
      surface: Snapshot,
      signalTexts: List[String]
  ): Int =
    val moveRefScore =
      surface.allMoveRefs.map { moveRef =>
        val idea = normalizeText(moveRef.idea).toLowerCase
        val evidence = moveRef.evidence.map(normalizeText).map(_.toLowerCase)
        if
          evidence.contains("target_pawn") ||
            containsAny(
              idea,
              List(
                "contest the pawn",
                "fixed target",
                "fixed pawn",
                "weak pawn",
                "backward pawn",
                "fix the",
                "attacking fixed pawn"
              )
            )
        then 4
        else 0
      }.sum
    val targetReasonScore =
      surface.allDirectionalTargets.map { target =>
        val reasons = target.strategicReasons.map(normalizeText).map(_.toLowerCase)
        if
          reasons.exists(reason =>
            containsAny(
              reason,
              List(
                "backward pawn",
                "weak pawn",
                "fixed target",
                "fixed pawn",
                "attacking fixed pawn",
                "static weakness fixation"
              )
            )
          )
        then 3
        else 0
      }.sum
    val dominantScore =
      (if surface.dominantIdea.exists(_.kind == StrategicIdeaKind.TargetFixing) then 3 else 0) +
        (if surface.secondaryIdea.exists(_.kind == StrategicIdeaKind.TargetFixing) then 2 else 0)
    val textScore =
      signalTexts.count(text =>
        containsAny(
          text,
          List(
            "fixed target",
            "fixed targets",
            "contest the pawn",
            "weak pawn",
            "backward pawn",
            "target pressure",
            "keep the targets",
            "attacking fixed pawn",
            "static weakness fixation"
          )
        )
      ) * 2
    val lateStructuralScore =
      surface.evidenceHints.count(text =>
        containsAny(
          normalizeText(text).toLowerCase,
          List(
            "attacking fixed pawn",
            "fixed pawn",
            "fixed target",
            "fixed targets",
            "backward pawn",
            "weakness_fixation",
            "weakness fixation",
            "static weakness fixation"
          )
        )
      ) * 3
    val focusScore =
      surface.longTermFocus.toList.count(text =>
        containsAny(
          normalizeText(text).toLowerCase,
          List(
            "fixed target",
            "fixed targets",
            "targets fixed",
            "contest the pawn",
            "weak pawn",
            "backward pawn",
            "attacking fixed pawn"
          )
        )
      ) * 4
    moveRefScore + targetReasonScore + dominantScore + textScore + lateStructuralScore + focusScore

  private def repeatedTargetPawnCount(surface: Snapshot): Int =
    surface.allMoveRefs.count(moveRef =>
      moveRef.evidence.map(normalizeText).map(_.toLowerCase).contains("target_pawn")
    )

  private def lateTargetFixingEvidenceCount(surface: Snapshot): Int =
    surface.evidenceHints.count(text =>
      containsAny(
        normalizeText(text).toLowerCase,
        List(
          "attacking fixed pawn",
          "fixed pawn",
          "fixed target",
          "fixed targets",
          "backward pawn",
          "weakness_fixation",
          "weakness fixation",
          "static weakness fixation"
        )
      )
    )

  private def fixedTargetPlanCarrier(surface: Snapshot): Boolean =
    false

  private def sameTheaterOpenFileRouteCount(
      surface: Snapshot,
      theater: String
  ): Int =
    if theater == "mixed" then 0
    else
      surface.allRoutes.count { route =>
        containsAny(
          normalizeText(route.purpose).toLowerCase,
          List("open-file occupation", "file occupation", "file pressure", "open file")
        ) &&
          route.route.lastOption.flatMap(theaterFromSquare).contains(theater)
      }

  private def lineOccupationAnchorStrength(
      surface: Snapshot,
      signalTexts: List[String]
  ): Int =
    val routeScore =
      surface.allRoutes.map { route =>
        val purpose = normalizeText(route.purpose).toLowerCase
        if containsAny(purpose, List("open-file occupation", "file occupation", "file pressure", "open file", "line pressure")) then 4
        else if route.route.nonEmpty then 1
        else 0
      }.sum
    val targetReasonScore =
      surface.allDirectionalTargets.map { target =>
        val reasons = target.strategicReasons.map(normalizeText).map(_.toLowerCase)
        if reasons.exists(reason => containsAny(reason, List("line access", "file access", "open line"))) then 2
        else 0
      }.sum
    val dominantScore =
      (if surface.dominantIdea.exists(_.kind == StrategicIdeaKind.LineOccupation) then 3 else 0) +
        (if surface.secondaryIdea.exists(_.kind == StrategicIdeaKind.LineOccupation) then 2 else 0)
    val textScore =
      signalTexts.count(text =>
        containsAny(
          text,
          List(
            "line pressure",
            "file pressure",
            "open file",
            "file control",
            "line control",
            "open-line pressure",
            "files under pressure",
            "line access",
            "central file control"
          )
        )
      ) * 2
    routeScore + targetReasonScore + dominantScore + textScore

  private def counterplayAnchorStrength(signalTexts: List[String]): Int =
    signalTexts.count(text =>
      containsAny(text, List("counterplay", "deny counterplay", "cannot breathe", "tied down", "passive defense"))
    ) * 2

  private def normalizedPressureMode(
      surface: Snapshot,
      rawSubtype: CompensationSubtype,
      targetAnchors: Int,
      lineAnchors: Int,
      counterplayAnchors: Int,
      sameTheaterTargetEvidence: Int,
      repeatedTargetPawnRefs: Int,
      lateTargetEvidence: Int,
      sameTheaterLineRoutes: Int,
      hasOpenFileOccupationRoute: Boolean,
      focusTargetFixingHint: Boolean,
      repeatedCenterTargetShell: Boolean
  ): String =
      val rawLineOccupationBias =
        surface.dominantIdea.exists(_.kind == StrategicIdeaKind.LineOccupation) &&
          !surface.dominantIdea.exists(_.kind == StrategicIdeaKind.TargetFixing)
      val fixedTargetPlan = fixedTargetPlanCarrier(surface)
      val dominantTargetFixing =
        surface.dominantIdea.exists(_.kind == StrategicIdeaKind.TargetFixing)
      val targetFixingIdeaPresent =
        dominantTargetFixing || surface.secondaryIdea.exists(_.kind == StrategicIdeaKind.TargetFixing)
      val lateTargetFixingRescue =
        !rawLineOccupationBias &&
          fixedTargetPlan &&
          sameTheaterLineRoutes == 0
      val lineOccupationLock =
        rawLineOccupationBias &&
          !targetFixingIdeaPresent &&
          !repeatedCenterTargetShell &&
          lateTargetEvidence >= 1 &&
          !fixedTargetPlan &&
          !hasOpenFileOccupationRoute
      if counterplayAnchors > math.max(targetAnchors, lineAnchors) then "counterplay_denial"
      else if lineOccupationLock then "line_occupation"
      else if !rawLineOccupationBias && lateTargetFixingRescue then "target_fixing"
      else if dominantTargetFixing && targetAnchors >= 2 then "target_fixing"
      else if targetFixingIdeaPresent && targetAnchors + 1 >= lineAnchors && targetAnchors >= 4 then
        "target_fixing"
      else if repeatedTargetPawnRefs >= 2 && targetAnchors + 1 >= lineAnchors then
        "target_fixing"
      else if sameTheaterTargetEvidence >= 3 && targetAnchors + 3 >= lineAnchors then "target_fixing"
      else if repeatedCenterTargetShell then "target_fixing"
      else if focusTargetFixingHint then "target_fixing"
      else if
        hasOpenFileOccupationRoute && lineAnchors >= targetAnchors + 4 && sameTheaterTargetEvidence <= 1 &&
          !focusTargetFixingHint
      then "line_occupation"
      else if targetAnchors >= 6 && targetAnchors >= lineAnchors + 2 then "target_fixing"
      else if targetAnchors >= lineAnchors && targetAnchors >= 4 then "target_fixing"
      else if lineAnchors > 0 then "line_occupation"
      else rawSubtype.pressureMode

  private def targetFixingTheaterOverride(surface: Snapshot): Option[String] =
    val hits = scala.collection.mutable.Map("queenside" -> 0, "kingside" -> 0, "center" -> 0)
    val fixedPlanCarrier = fixedTargetPlanCarrier(surface)

    def add(theater: String, score: Int): Unit =
      if hits.contains(theater) && score > 0 then hits.update(theater, hits(theater) + score)

    surface.allMoveRefs.foreach { moveRef =>
      val idea = normalizeText(moveRef.idea).toLowerCase
      val evidence = moveRef.evidence.map(normalizeText).map(_.toLowerCase)
      val isTargeting =
        evidence.contains("target_pawn") ||
          containsAny(
            idea,
            List("contest the pawn", "fixed target", "fixed pawn", "weak pawn", "backward pawn", "attacking fixed pawn")
          )
      val weakFixedPlanTarget =
        fixedPlanCarrier &&
          moveRef.piece != "K" &&
          evidence.contains("enemy_occupied_endpoint")
      if isTargeting then
          theaterFromSquare(moveRef.target).foreach(add(_, 3))
      else if weakFixedPlanTarget then
        theaterFromSquare(moveRef.target).foreach(add(_, 2))
    }
    surface.allDirectionalTargets.foreach { target =>
      val isTargeting =
        target.strategicReasons.map(normalizeText).map(_.toLowerCase).exists(reason =>
          containsAny(
            reason,
            List("weak pawn", "backward pawn", "fixed target", "fixed pawn", "attacking fixed pawn", "static weakness fixation")
          )
        )
      val weakFixedPlanTarget =
        fixedPlanCarrier &&
          target.strategicReasons.map(normalizeText).map(_.toLowerCase).exists(reason =>
            containsAny(reason, List("supports attacking fixed pawn"))
          )
      if isTargeting then
        theaterFromSquare(target.targetSquare).foreach(add(_, 2))
      else if weakFixedPlanTarget then
        theaterFromSquare(target.targetSquare).foreach(add(_, 2))
    }
    surface.longTermFocus.toList.foreach { focus =>
      val lowered = normalizeText(focus).toLowerCase
      if containsAny(lowered, List("fixed queenside targets", "queenside targets")) then add("queenside", 5)
      if containsAny(lowered, List("fixed central targets", "central targets")) then add("center", 5)
      if containsAny(lowered, List("fixed kingside targets", "kingside targets")) then add("kingside", 5)
      if containsAny(lowered, List("queenside", "queen side", "b-file", "c-file")) then add("queenside", 2)
      if containsAny(lowered, List("central", "center", "d-file", "e-file")) then add("center", 2)
      if containsAny(lowered, List("kingside", "king side", "f-file", "g-file", "h-file")) then add("kingside", 2)
    }
    targetFixingIdeaAnchorTheaters(surface).foreach(add(_, 3))
    objectiveAnchorTheaters(surface).foreach(add(_, 2))

    val explicitFocusTheaterOverride =
      Option.when(explicitTargetFixingFocus(surface)) {
        val focusAnchors = surface.longTermFocus.toList.flatMap(theatersFromText)
        val counts = focusAnchors.groupMapReduce(identity)(_ => 1)(_ + _)
        counts.toList.sortBy { case (_, score) => -score } match
          case (topTheater, topScore) :: (_, secondScore) :: _ if topScore >= secondScore + 1 => Some(topTheater)
          case (topTheater, topScore) :: Nil if topScore >= 1                                  => Some(topTheater)
          case _                                                                               => None
      }.flatten

    val ranked = hits.toList.sortBy { case (_, score) => -score }
    explicitFocusTheaterOverride.orElse {
      ranked match
        case (theater, top) :: (_, second) :: _ if top >= 3 && top >= second + 1 => Some(theater)
        case (theater, top) :: Nil if top >= 3                                     => Some(theater)
        case _                                                                     => None
    }

  private def normalizedRecoveryPolicy(
      surface: Snapshot,
      rawSubtype: CompensationSubtype,
      normalizedMode: String,
      signalTexts: List[String]
  ): String =
    val recoveryTexts =
      signalTexts ++ surface.longTermFocus.toList.map(normalizeText).filter(_.nonEmpty).map(_.toLowerCase)
    val quietDurableMode =
      Set("line_occupation", "target_fixing", "counterplay_denial", "defender_tied_down").contains(normalizedMode)
    val explicitDeferred =
      recoveryTexts.exists(text =>
        containsAny(
          text,
          List(
            "before recovering",
            "delayed recovery",
            "delay recovery",
            "delaying recovery",
            "keep the material invested",
            "stay invested",
            "cash out",
            "before cashing out",
            "recovering material"
          )
        )
      )
    if explicitDeferred && quietDurableMode then "intentionally_deferred"
    else if quietDurableMode && surface.investedMaterial.exists(_ > 0) && rawSubtype.stabilityClass == "durable_pressure"
    then "intentionally_deferred"
    else rawSubtype.recoveryPolicy

  private def normalizedStabilityClass(
      rawSubtype: CompensationSubtype,
      normalizedMode: String,
      normalizedRecoveryPolicy: String,
      targetAnchors: Int,
      lineAnchors: Int,
      counterplayAnchors: Int
  ): String =
    val structuralAnchors = targetAnchors + lineAnchors + counterplayAnchors
    if
      rawSubtype.transitionOnly &&
        Set("line_occupation", "target_fixing", "counterplay_denial").contains(normalizedMode) &&
        normalizedRecoveryPolicy != "immediate" &&
        structuralAnchors >= 4
    then "durable_pressure"
    else if rawSubtype.transitionOnly then rawSubtype.stabilityClass
    else if
      Set("line_occupation", "target_fixing", "counterplay_denial").contains(normalizedMode) ||
        normalizedRecoveryPolicy == "intentionally_deferred"
    then "durable_pressure"
    else rawSubtype.stabilityClass

  private def normalizationConfidence(
      surface: Snapshot,
      subtype: CompensationSubtype
  ): Int =
    val alignmentBoost =
      (if alignedRoute(surface, subtype).nonEmpty then 1 else 0) +
        (if alignedMoveRef(surface, subtype).nonEmpty then 1 else 0) +
        (if alignedDirectionalTarget(surface, subtype).nonEmpty then 1 else 0)
    val base =
      (if surface.compensationPosition then 1 else 0) +
        (if subtype.pressureTheater != "mixed" then 2 else 0) +
        (if subtype.durablePressure then 2 else 0) +
        (if Set("line_occupation", "target_fixing", "counterplay_denial", "defender_tied_down").contains(subtype.pressureMode)
         then 2
         else 0) +
        (if subtype.recoveryPolicy == "intentionally_deferred" then 2
         else if subtype.recoveryPolicy == "delayed" then 1
         else 0)
    val anchorPenalty =
      executionAnchorTheater(surface)
        .filter(anchor => anchor != subtype.pressureTheater && subtype.pressureTheater != "mixed")
        .map(_ => 3)
        .getOrElse(0)
    val attackBiasPenalty =
      Option.when(
        surface.dominantIdea.exists(_.kind == StrategicIdeaKind.KingAttackBuildUp) &&
          subtype.pressureTheater != "kingside" &&
          (subtype.recoveryPolicy == "immediate" || subtype.stabilityClass == "tactical_window")
      )(2).getOrElse(0)
    val attackSurfacePenalty =
      Option.when(
        surface.dominantIdea.exists(_.kind == StrategicIdeaKind.KingAttackBuildUp) &&
          surface.dominantIdea.flatMap(_.focusZone).exists(zone => normalizeText(zone).equalsIgnoreCase("kingside")) &&
          subtype.pressureMode == "line_occupation" &&
          subtype.recoveryPolicy == "immediate"
      )(5).getOrElse(0)
    val transitionPenalty =
      (if subtype.transitionOnly then 4 else 0) +
        (if subtype.stabilityClass == "tactical_window" then 2 else 0)
    base + alignmentBoost - anchorPenalty - attackBiasPenalty - attackSurfacePenalty - transitionPenalty

  private def executionAnchorTheater(surface: Snapshot): Option[String] =
    val routeAnchor = surface.topRoute.flatMap(_.route.lastOption).flatMap(theaterFromSquare)
    val moveRefAnchor = surface.topMoveRef.map(_.target).flatMap(theaterFromSquare)
    val targetAnchor = surface.topDirectionalTarget.map(_.targetSquare).flatMap(theaterFromSquare)
    routeAnchor.orElse(moveRefAnchor).orElse(targetAnchor)

  private[analysis] def alignedRoute(surface: Snapshot, subtype: CompensationSubtype): Option[StrategyPieceRoute] =
    surface.allRoutes.find { route =>
      val destinationTheater = route.route.lastOption.flatMap(theaterFromSquare)
      val purpose = normalizeText(route.purpose).toLowerCase
      val theaterOk =
        subtype.pressureTheater == "mixed" || destinationTheater.contains(subtype.pressureTheater)
      val modeOk =
        subtype.pressureMode match
          case "line_occupation" =>
            containsAny(purpose, List("open-file occupation", "file occupation", "file pressure", "open file"))
          case "target_fixing" =>
            containsAny(purpose, List("open-file occupation", "file occupation", "pressure"))
          case "counterplay_denial" =>
            containsAny(purpose, List("contain", "deny", "restrict", "occupation", "pressure"))
          case _ => true
      theaterOk && modeOk
    }

  private[analysis] def alignedMoveRef(surface: Snapshot, subtype: CompensationSubtype): Option[StrategyPieceMoveRef] =
    surface.allMoveRefs.find { moveRef =>
      val targetTheater = theaterFromSquare(moveRef.target)
      val idea = normalizeText(moveRef.idea).toLowerCase
      val evidence = moveRef.evidence.map(normalizeText).map(_.toLowerCase)
      val theaterOk =
        subtype.pressureTheater == "mixed" || targetTheater.contains(subtype.pressureTheater)
      val modeOk =
        subtype.pressureMode match
          case "target_fixing" =>
            moveRef.piece != "K" &&
              (evidence.contains("target_pawn") ||
                containsAny(idea, List("contest the pawn", "fixed target", "fixed pawn", "weak pawn")))
          case "counterplay_denial" =>
            containsAny(idea, List("restrict", "deny", "tie down", "contain"))
          case _ => moveRef.piece != "K"
      theaterOk && modeOk
    }

  private[analysis] def alignedDirectionalTarget(surface: Snapshot, subtype: CompensationSubtype): Option[StrategyDirectionalTarget] =
    surface.allDirectionalTargets.find { target =>
      val targetTheater = theaterFromSquare(target.targetSquare)
      subtype.pressureTheater == "mixed" || targetTheater.contains(subtype.pressureTheater)
    }

  private def deriveCompensationSubtype(
      dominantIdea: Option[StrategyIdeaSignal],
      secondaryIdea: Option[StrategyIdeaSignal],
      allRoutes: List[StrategyPieceRoute],
      topRoute: Option[StrategyPieceRoute],
      allMoveRefs: List[StrategyPieceMoveRef],
      topMoveRef: Option[StrategyPieceMoveRef],
      allDirectionalTargets: List[StrategyDirectionalTarget],
      topDirectionalTarget: Option[StrategyDirectionalTarget],
      longTermFocus: Option[String],
      compensationSummary: Option[String],
      compensationVectors: List[String],
      investedMaterial: Option[Int]
  ): Option[CompensationSubtype] =
    Option.when(
      compensationSummary.exists(_.nonEmpty) || compensationVectors.nonEmpty || investedMaterial.exists(_ > 0)
    ) {
      val texts =
        List(
          dominantIdea.flatMap(ideaText),
          secondaryIdea.flatMap(ideaText),
          topRoute.flatMap(routeText),
          topMoveRef.flatMap(moveRefText),
          topDirectionalTarget.flatMap(targetText),
          longTermFocus,
          compensationSummary
        ).flatten.map(normalizeText).filter(_.nonEmpty) ++
          allRoutes.flatMap(routeText).map(normalizeText).filter(_.nonEmpty) ++
          allMoveRefs.flatMap(moveRefText).map(normalizeText).filter(_.nonEmpty) ++
          allDirectionalTargets.flatMap(targetText).map(normalizeText).filter(_.nonEmpty)
      val lowered = (texts ++ compensationVectors.map(normalizeText)).map(_.toLowerCase)
      val dominantKind = dominantIdea.map(_.kind)
      val secondaryKind = secondaryIdea.map(_.kind)
      val basePressureTheater =
        derivePressureTheater(dominantIdea, secondaryIdea, allRoutes, allMoveRefs, allDirectionalTargets, lowered)
      val lineAnchor =
        dominantKind.contains(StrategicIdeaKind.LineOccupation) ||
          secondaryKind.contains(StrategicIdeaKind.LineOccupation) ||
          allRoutes.exists(route =>
            containsAny(normalizeText(route.purpose).toLowerCase, List("open-file occupation", "file occupation", "file pressure", "open file"))
          ) ||
          lowered.exists(text =>
            containsAny(
              text,
              List("line pressure", "file pressure", "open file", "semi open", "file control", "line control", "open line", "occupy the open file")
            )
          )
      val targetAnchor =
        dominantKind.contains(StrategicIdeaKind.TargetFixing) ||
          secondaryKind.contains(StrategicIdeaKind.TargetFixing) ||
          allMoveRefs.exists(moveRef =>
            moveRef.evidence.map(normalizeText).map(_.toLowerCase).contains("target_pawn") ||
              containsAny(normalizeText(moveRef.idea).toLowerCase, List("contest the pawn", "fixed target", "fixed pawn", "weak pawn"))
          ) ||
          lowered.exists(text =>
            containsAny(
              text,
              List("target fixing", "fixed target", "fixed targets", "minority attack", "weak target", "backward pawn", "attacking fixed pawn", "fixed pawn", "fix the")
            )
          )
      val breakAnchor =
        dominantKind.contains(StrategicIdeaKind.PawnBreak) ||
          secondaryKind.contains(StrategicIdeaKind.PawnBreak) ||
          lowered.exists(text => containsAny(text, List("break", "hook", "pawn storm", "scaffold")))
      val counterplayAnchor =
        dominantKind.contains(StrategicIdeaKind.CounterplaySuppression) ||
          secondaryKind.contains(StrategicIdeaKind.CounterplaySuppression) ||
          lowered.exists(text =>
            containsAny(text, List("counterplay", "deny counterplay", "denying counterplay", "cannot breathe", "clamp"))
          )
      val conversionAnchor =
        dominantKind.contains(StrategicIdeaKind.FavorableTradeOrTransformation) ||
          secondaryKind.contains(StrategicIdeaKind.FavorableTradeOrTransformation) ||
          lowered.exists(text => containsAny(text, List("cash out", "transition", "transform", "trade down", "conversion")))
      val defenderAnchor =
        dominantKind.contains(StrategicIdeaKind.KingAttackBuildUp) ||
          secondaryKind.contains(StrategicIdeaKind.KingAttackBuildUp) ||
          lowered.exists(text => containsAny(text, List("tied down", "tied to", "passive defense", "passive shell", "defender")))
      val delayedSignal =
        lowered.exists(text => containsAny(text, List("delayed recovery", "delay recovery", "delay material recovery")))
      val structuralTargetPressure =
        allMoveRefs.exists(moveRef =>
          moveRef.evidence.map(normalizeText).map(_.toLowerCase).contains("target_pawn") ||
            containsAny(normalizeText(moveRef.idea).toLowerCase, List("contest the pawn", "fixed pawn", "weak pawn"))
        ) ||
          allRoutes.exists(route =>
            containsAny(normalizeText(route.purpose).toLowerCase, List("open-file occupation", "file occupation", "file pressure", "open file"))
          )
      val structuralPressureTheater =
        deriveStructuralPressureTheater(allRoutes, allMoveRefs, allDirectionalTargets, lowered)
      val quietStructuralCompensation =
        investedMaterial.exists(_ > 0) &&
          structuralTargetPressure &&
          (delayedSignal || targetAnchor || compensationVectors.exists(vector =>
            containsAny(vector.toLowerCase, List("line pressure", "delayed recovery", "return vector"))
          ))
      val pressureTheater =
        if dominantKind.contains(StrategicIdeaKind.KingAttackBuildUp) &&
            quietStructuralCompensation &&
            structuralPressureTheater.exists(theater => theater != "mixed" && theater != "kingside")
        then structuralPressureTheater.get
        else basePressureTheater

      val attackLedKingside =
        dominantKind.contains(StrategicIdeaKind.KingAttackBuildUp) &&
          pressureTheater == "kingside" &&
          !targetAnchor &&
          !counterplayAnchor &&
          !conversionAnchor

      val lineScore =
        scoreModeAnchor(dominantKind, secondaryKind, StrategicIdeaKind.LineOccupation, lineAnchor, lowered, pressureTheater)
      val targetScore =
        scoreModeAnchor(dominantKind, secondaryKind, StrategicIdeaKind.TargetFixing, targetAnchor, lowered, pressureTheater) +
          Option.when(
            pressureTheater == "queenside" &&
              lowered.exists(text =>
                containsAny(text, List("minority attack", "weak target", "backward pawn", "attacking fixed pawn", "fixed queenside", "queenside targets"))
              )
          )(1).getOrElse(0) +
          Option.when(
            quietStructuralCompensation &&
              structuralPressureTheater.contains("queenside") &&
              allMoveRefs.exists(moveRef =>
                moveRef.evidence.map(normalizeText).map(_.toLowerCase).contains("target_pawn") ||
                  containsAny(normalizeText(moveRef.idea).toLowerCase, List("contest the pawn", "fixed pawn", "weak pawn"))
              )
          )(2).getOrElse(0)
      val counterplayScore =
        scoreModeAnchor(dominantKind, secondaryKind, StrategicIdeaKind.CounterplaySuppression, counterplayAnchor, lowered, pressureTheater)
      val breakScore =
        scoreModeAnchor(dominantKind, secondaryKind, StrategicIdeaKind.PawnBreak, breakAnchor, lowered, pressureTheater)
      val conversionScore =
        scoreModeAnchor(dominantKind, secondaryKind, StrategicIdeaKind.FavorableTradeOrTransformation, conversionAnchor, lowered, pressureTheater)
      val defenderScore =
        scoreModeAnchor(dominantKind, secondaryKind, StrategicIdeaKind.KingAttackBuildUp, defenderAnchor, lowered, pressureTheater)

      val pressureMode =
        if attackLedKingside && breakAnchor then "break_preparation"
        else if attackLedKingside then "defender_tied_down"
        else if pressureTheater == "queenside" && targetScore > 0 && targetScore >= lineScore then "target_fixing"
        else if lineScore > 0 && lineScore >= targetScore && lineScore >= counterplayScore then "line_occupation"
        else if targetScore > 0 && targetScore >= counterplayScore then "target_fixing"
        else if counterplayScore > 0 then "counterplay_denial"
        else if breakScore > 0 then "break_preparation"
        else if conversionScore > 0 then "conversion_window"
        else if defenderScore > 0 then "defender_tied_down"
        else if compensationVectors.exists(_.toLowerCase.contains("line pressure")) then "line_occupation"
        else "defender_tied_down"

      val recoveryPolicy =
        if delayedSignal && Set("line_occupation", "target_fixing", "counterplay_denial", "defender_tied_down").contains(pressureMode)
        then "intentionally_deferred"
        else if delayedSignal || (investedMaterial.exists(_ > 0) && pressureTheater == "queenside" && Set("line_occupation", "target_fixing", "counterplay_denial").contains(pressureMode))
        then "delayed"
        else "immediate"

      val stabilityClass =
        if pressureMode == "conversion_window" || lowered.exists(text => containsAny(text, List("cash out", "transition window")))
        then "transition_only"
        else if
          Set("line_occupation", "target_fixing", "counterplay_denial").contains(pressureMode) ||
            recoveryPolicy == "intentionally_deferred" ||
            (pressureTheater == "queenside" && (lineAnchor || targetAnchor))
        then "durable_pressure"
        else "tactical_window"

      CompensationSubtype(
        pressureTheater = pressureTheater,
        pressureMode = pressureMode,
        recoveryPolicy = recoveryPolicy,
        stabilityClass = stabilityClass
      )
    }

  private def derivePressureTheater(
      dominantIdea: Option[StrategyIdeaSignal],
      secondaryIdea: Option[StrategyIdeaSignal],
      allRoutes: List[StrategyPieceRoute],
      allMoveRefs: List[StrategyPieceMoveRef],
      allDirectionalTargets: List[StrategyDirectionalTarget],
      loweredTexts: List[String]
  ): String =
    val theaterHits = scala.collection.mutable.Map("queenside" -> 0, "kingside" -> 0, "center" -> 0)

    def addHit(theater: String, weight: Int): Unit =
      if theaterHits.contains(theater) && weight > 0 then theaterHits.update(theater, theaterHits(theater) + weight)

    def addZone(zone: Option[String], weight: Int): Unit =
      zone.map(normalizeText).map(_.toLowerCase).flatMap(normalizeTheaterToken).foreach(addHit(_, weight))

    def addFiles(files: List[String], weight: Int): Unit =
      files.flatMap(theaterFromFile).foreach(addHit(_, weight))

    def addSquares(squares: List[String], weight: Int): Unit =
      squares.flatMap(theaterFromSquare).foreach(addHit(_, weight))

    addZone(dominantIdea.flatMap(_.focusZone), 5)
    addZone(secondaryIdea.flatMap(_.focusZone), 3)
    addFiles(dominantIdea.toList.flatMap(_.focusFiles), 4)
    addFiles(secondaryIdea.toList.flatMap(_.focusFiles), 2)
    addSquares(dominantIdea.toList.flatMap(_.focusSquares), 3)
    addSquares(secondaryIdea.toList.flatMap(_.focusSquares), 2)
    addSquares(allDirectionalTargets.map(_.targetSquare), 2)
    addSquares(allMoveRefs.map(_.target), 1)
    addSquares(allRoutes.flatMap(_.route), 1)

    val explicitTheaterHits =
      List(
        Option.when(loweredTexts.exists(containsAny(_, List("queenside", "queen side", "minority attack", "queenside files", "queenside targets"))))("queenside"),
        Option.when(loweredTexts.exists(containsAny(_, List("kingside", "king side", "mating net", "attack on king"))))("kingside"),
        Option.when(loweredTexts.exists(containsAny(_, List("center", "central", "central files", "central break"))))("center")
      ).flatten
    explicitTheaterHits.foreach(addHit(_, 2))

    chooseTheater(theaterHits.toMap)

  private def deriveStructuralPressureTheater(
      allRoutes: List[StrategyPieceRoute],
      allMoveRefs: List[StrategyPieceMoveRef],
      allDirectionalTargets: List[StrategyDirectionalTarget],
      loweredTexts: List[String]
  ): Option[String] =
    val theaterHits = scala.collection.mutable.Map("queenside" -> 0, "kingside" -> 0, "center" -> 0)

    def addHit(theater: String, weight: Int): Unit =
      if theaterHits.contains(theater) && weight > 0 then theaterHits.update(theater, theaterHits(theater) + weight)

    def addSquares(squares: List[String], weight: Int): Unit =
      squares.flatMap(theaterFromSquare).foreach(addHit(_, weight))

    addSquares(allDirectionalTargets.map(_.targetSquare), 2)
    addSquares(allRoutes.flatMap(_.route.lastOption), 2)
    allRoutes.foreach { route =>
      val purpose = normalizeText(route.purpose).toLowerCase
      val routeWeight =
        if containsAny(purpose, List("open-file occupation", "file occupation", "file pressure", "open file")) then 3
        else 1
      addSquares(route.route.lastOption.toList, routeWeight)
    }
    allMoveRefs.foreach { moveRef =>
      val idea = normalizeText(moveRef.idea).toLowerCase
      val evidence = moveRef.evidence.map(normalizeText).map(_.toLowerCase)
      val moveRefWeight =
        if evidence.contains("target_pawn") || containsAny(idea, List("contest the pawn", "fixed pawn", "weak pawn")) then 4
        else 2
      addSquares(List(moveRef.target), moveRefWeight)
    }
    List(
      Option.when(loweredTexts.exists(containsAny(_, List("queenside", "queen side", "queenside files", "queenside targets"))))("queenside"),
      Option.when(loweredTexts.exists(containsAny(_, List("central files", "center", "central"))))("center")
    ).flatten.foreach(addHit(_, 1))

    Option(chooseTheater(theaterHits.toMap)).filter(_ != "mixed")

  private def chooseTheater(theaterHits: Map[String, Int]): String =
    val ranked =
      List("queenside", "kingside", "center")
        .map(theater => theater -> theaterHits.getOrElse(theater, 0))
        .sortBy { case (_, score) => -score }
    ranked match
      case (topTheater, topScore) :: (secondTheater, secondScore) :: _ if topScore > 0 =>
        val topIsFlank = topTheater == "queenside" || topTheater == "kingside"
        val secondIsFlank = secondTheater == "queenside" || secondTheater == "kingside"
        if secondScore <= 0 then topTheater
        else if topIsFlank && secondTheater == "center" && topScore >= secondScore then topTheater
        else if topTheater == "center" && secondIsFlank && secondScore >= topScore - 1 then secondTheater
        else if topIsFlank && secondIsFlank && topScore <= secondScore + 1 then "mixed"
        else if topScore >= secondScore + 2 then topTheater
        else "mixed"
      case (topTheater, topScore) :: _ if topScore > 0 => topTheater
      case _                                           => "mixed"

  private def theaterFromSquare(square: String): Option[String] =
    val normalized = normalizeText(square).toLowerCase
    Option.when(normalized.matches("^[a-h][1-8]$")) {
      normalized.head match
        case 'a' | 'b' | 'c' => "queenside"
        case 'f' | 'g' | 'h' => "kingside"
        case 'd' | 'e'       => "center"
        case _               => "mixed"
    }

  private def theaterFromFile(file: String): Option[String] =
    normalizeText(file).toLowerCase.headOption.flatMap {
      case 'a' | 'b' | 'c' => Some("queenside")
      case 'f' | 'g' | 'h' => Some("kingside")
      case 'd' | 'e'       => Some("center")
      case _               => None
    }

  private def normalizeTheaterToken(raw: String): Option[String] =
    normalizeText(raw).toLowerCase match
      case value if value.contains("queen side") || value.contains("queenside") => Some("queenside")
      case value if value.contains("king side") || value.contains("kingside")   => Some("kingside")
      case value if value.contains("center") || value.contains("central")        => Some("center")
      case _                                                                      => None

  private def scoreModeAnchor(
      dominantKind: Option[String],
      secondaryKind: Option[String],
      targetKind: String,
      textAnchor: Boolean,
      loweredTexts: List[String],
      pressureTheater: String
  ): Int =
    val dominantWeight = Option.when(dominantKind.contains(targetKind))(3).getOrElse(0)
    val secondaryWeight = Option.when(secondaryKind.contains(targetKind))(2).getOrElse(0)
    val textWeight = Option.when(textAnchor)(1).getOrElse(0)
    val theaterWeight =
      Option.when(
        pressureTheater == "queenside" &&
          targetKind == StrategicIdeaKind.TargetFixing &&
          loweredTexts.exists(text => containsAny(text, List("backward pawn", "minority attack", "fixed target", "fixed pawn")))
      )(1).getOrElse(0)
    dominantWeight + secondaryWeight + textWeight + theaterWeight

  private def containsAny(text: String, needles: List[String]): Boolean =
    needles.exists(text.contains)

  private[analysis] def theaterDisplay(theater: String): String =
    theater match
      case "kingside"  => "kingside"
      case "queenside" => "queenside"
      case "center"    => "central"
      case _           => "mixed"
