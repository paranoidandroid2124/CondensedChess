package lila.llm.analysis

import lila.llm.*
import lila.llm.model.*

private[analysis] enum StrategicLens:
  case Compensation
  case Prophylaxis
  case Structure
  case Decision
  case Practical
  case Opening

private[analysis] case class StrategicThesis(
    lens: StrategicLens,
    claim: String,
    support: List[String],
    tension: Option[String],
    evidenceHook: Option[String]
)

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
      strategicStack: List[String],
      latentPlan: Option[String],
      decisionEvidence: Option[String],
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
    val strategicStack =
      signalDigest.toList.flatMap(_.strategicStack.map(normalizeText).filter(_.nonEmpty)).distinct
    val latentPlan =
      signalDigest.flatMap(_.latentPlan).map(normalizeText).filter(_.nonEmpty)
    val decisionEvidence =
      signalDigest.flatMap(_.decisionComparison.flatMap(_.evidence)).map(normalizeText).filter(_.nonEmpty)
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
        strategicStack = strategicStack,
        latentPlan = latentPlan,
        decisionEvidence = decisionEvidence,
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

  private def compensationSubtypeLabel(subtype: CompensationSubtype): String =
    s"${subtype.pressureTheater}/${subtype.pressureMode}/${subtype.recoveryPolicy}/${subtype.stabilityClass}"

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

  def compensationPayoffText(surface: Snapshot): Option[String] =
    CompensationDisplayPhrasing.compensationPayoffText(surface)

  def compensationPersistenceText(surface: Snapshot): Option[String] =
    CompensationDisplayPhrasing.compensationPersistenceText(surface)

  def compensationWhyNowText(surface: Snapshot): Option[String] =
    CompensationDisplayPhrasing.compensationWhyNowText(surface)

  def compensationObjectiveText(surface: Snapshot): Option[String] =
    CompensationDisplayPhrasing.compensationObjectiveText(surface)

  def compensationExecutionTail(surface: Snapshot): Option[String] =
    CompensationDisplayPhrasing.compensationExecutionTail(surface)

  def compensationSupportText(surface: Snapshot): List[String] =
    CompensationDisplayPhrasing.compensationSupportText(surface)

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
      val payoffSubtype =
        derivePayoffCompensationSubtype(surface, rawSubtype, preparationSubtype).getOrElse(preparationSubtype)
      val pathConfidence = normalizationConfidence(surface, preparationSubtype)
      val payoffConfidence = payoffNormalizationConfidence(surface, payoffSubtype)
      val fixedTargetingDisplayLock =
        preparationSubtype.pressureMode == "target_fixing" &&
          payoffSubtype.pressureMode == "line_occupation" &&
          (
            explicitTargetFixingFocus(surface) ||
              fixedTargetPlanCarrier(surface) ||
              repeatedTargetPawnCount(surface) >= 1 ||
              sameTheaterTargetFixingEvidence(surface, preparationSubtype.pressureTheater) >= 2
          )
      val fixedTargetingTheaterLock =
        preparationSubtype.pressureMode == "target_fixing" &&
          payoffSubtype.pressureMode == "target_fixing" &&
          preparationSubtype.pressureTheater != payoffSubtype.pressureTheater &&
          (
            explicitTargetFixingFocus(surface) ||
              fixedTargetPlanCarrier(surface) ||
              repeatedTargetPawnCount(surface) >= 1 ||
              targetFixingIdeaAnchorTheaters(surface).nonEmpty
          )
      val preferPayoffSource =
        !fixedTargetingDisplayLock &&
          !fixedTargetingTheaterLock &&
          payoffConfidence >= 6 &&
          (
            (
              preparationSubtype != rawSubtype &&
                payoffSubtype != preparationSubtype &&
                payoffConfidence >= pathConfidence + 2
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
        else if preparationSubtype != rawSubtype && pathConfidence >= 6 then "path"
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

  private[analysis] object CompensationDisplayPhrasing:

    def buildDisplayNormalization(
        surface: Snapshot,
        rawSubtype: CompensationSubtype,
        resolution: CompensationDisplaySubtypeResolver.DisplaySubtypeResolution
    ): DisplayNormalization =
      val normalizedSubtype = resolution.selectedDisplaySubtype.getOrElse(rawSubtype)
      val normalizedDominant =
        Option.when(resolution.normalizationActive)(normalizedDominantIdeaText(normalizedSubtype)).flatten
      val normalizedExecution =
        Option.when(resolution.normalizationActive)(normalizedExecutionText(surface, normalizedSubtype)).flatten
      val normalizedObjective =
        Option.when(resolution.normalizationActive)(normalizedObjectiveText(normalizedSubtype)).flatten
      val normalizedFocus =
        Option.when(resolution.normalizationActive)(normalizedLongTermFocusText(normalizedSubtype)).flatten
      val normalizedLead =
        Option.when(resolution.normalizationActive)(normalizedCompensationLead(surface, normalizedSubtype)).flatten
      DisplayNormalization(
        normalizedDominantIdeaText = normalizedDominant,
        normalizedExecutionText = normalizedExecution,
        normalizedObjectiveText = normalizedObjective,
        normalizedLongTermFocusText = normalizedFocus,
        normalizedCompensationLead = normalizedLead,
        normalizedCompensationSubtype = resolution.selectedDisplaySubtype,
        normalizationActive = resolution.normalizationActive,
        normalizationConfidence = resolution.normalizationConfidence,
        preparationSubtype = Option.when(resolution.preparationSubtype != rawSubtype)(resolution.preparationSubtype),
        payoffSubtype =
          Option.when(
            resolution.payoffSubtype != resolution.preparationSubtype || resolution.displaySubtypeSource == "payoff"
          )(resolution.payoffSubtype),
        selectedDisplaySubtype = resolution.selectedDisplaySubtype,
        displaySubtypeSource = resolution.displaySubtypeSource,
        payoffConfidence = resolution.payoffConfidence,
        pathConfidence = resolution.pathConfidence
      )

    def compensationPayoffText(surface: Snapshot): Option[String] =
      surface.effectiveCompensationSubtype.map {
        case CompensationSubtype("queenside", "target_fixing", _, _) =>
          "fixed queenside targets and long file pressure"
        case CompensationSubtype("queenside", "line_occupation", _, "durable_pressure") =>
          "durable queenside file pressure"
        case CompensationSubtype(_, "line_occupation", _, "durable_pressure") =>
          "open-file control and durable line pressure"
        case CompensationSubtype(_, "target_fixing", _, _) =>
          "fixed targets and durable pressure"
        case CompensationSubtype(_, "counterplay_denial", _, _) =>
          "keeping the extra pawn tied down and denying counterplay"
        case CompensationSubtype("kingside", "break_preparation", _, _) =>
          "a break-driven initiative against the king"
        case CompensationSubtype("kingside", "defender_tied_down", _, _) =>
          "initiative with the defenders tied to the king"
        case CompensationSubtype(_, "defender_tied_down", _, _) =>
          "tying the defenders to a passive shell"
        case CompensationSubtype(_, "conversion_window", _, _) =>
          "a transition window before the investment has to cash out"
        case CompensationSubtype(theater, _, _, _) =>
          s"${theaterDisplay(theater)} pressure bought by the investment".trim
      }

    def compensationPersistenceText(surface: Snapshot): Option[String] =
      surface.effectiveCompensationSubtype.map {
        case CompensationSubtype("queenside", "target_fixing", _, _) =>
          "the fixed queenside targets stay under pressure"
        case CompensationSubtype("queenside", "line_occupation", _, "durable_pressure") =>
          "the queenside files stay under pressure"
        case CompensationSubtype("center", "line_occupation", _, "durable_pressure") =>
          "the central files stay under pressure"
        case CompensationSubtype(_, "line_occupation", _, "durable_pressure") =>
          "the open-line pressure remains durable"
        case CompensationSubtype(_, "target_fixing", _, _) =>
          "the fixed targets stay under pressure"
        case CompensationSubtype(_, "counterplay_denial", _, _) =>
          "the extra pawn never gets to breathe"
        case CompensationSubtype("kingside", "break_preparation", _, _) =>
          "the break threats still have to land with force"
        case CompensationSubtype("kingside", "defender_tied_down", _, _) =>
          "the defenders keep getting dragged back to the king"
        case CompensationSubtype(_, "defender_tied_down", _, _) =>
          "the defender stays tied to passive defense"
        case CompensationSubtype(_, "conversion_window", _, _) =>
          "the transition window stays under control"
        case CompensationSubtype(_, _, "intentionally_deferred", _) =>
          "the recovery keeps being deferred for the pressure"
        case _ =>
          "the compensation remains durable"
      }

    def compensationWhyNowText(surface: Snapshot): Option[String] =
      Option.when(surface.compensationPosition) {
        val execution = compensationExecutionCue(surface).map(ex => s" through $ex").getOrElse("")
        if !surface.normalizationActive then
          val attackLed =
            surface.preferRawAttackDisplay ||
              surface.rawDominantIdeaText.exists(_.toLowerCase.contains("king attack")) ||
              surface.compensationSummary.exists(_.toLowerCase.contains("initiative")) ||
              surface.compensationVectors.exists(_.toLowerCase.contains("initiative"))
          if attackLed then
            s"Do not rush to recover the material; the point is to keep the initiative alive$execution."
          else
            s"Do not rush to recover the material; the point is to keep the compensation structure alive$execution."
        else
          surface.effectiveCompensationSubtype match
            case Some(CompensationSubtype("queenside", "target_fixing", _, _)) =>
              s"Do not rush to recover the material; the compensation investment bought fixed queenside targets and long line pressure on the queenside files$execution."
            case Some(CompensationSubtype("center", "target_fixing", _, _)) =>
              s"Do not rush to recover the material; the compensation investment bought fixed central targets and durable pressure$execution."
            case Some(CompensationSubtype("queenside", "line_occupation", _, "durable_pressure")) =>
              s"Do not rush to recover the material; the compensation investment bought long queenside line pressure on the files$execution."
            case Some(CompensationSubtype("center", "line_occupation", _, "durable_pressure")) =>
              s"Do not rush to recover the material; the compensation investment bought central line pressure that has to stay durable$execution."
            case Some(CompensationSubtype(_, "line_occupation", _, "durable_pressure")) =>
              s"Do not rush to recover the material; the compensation only pays if the open-line pressure stays durable$execution."
            case Some(CompensationSubtype(_, "counterplay_denial", _, _)) =>
              s"Do not rush to recover the material; the compensation only works because the extra pawn cannot breathe while the counterplay stays tied down$execution."
            case Some(CompensationSubtype("kingside", "break_preparation", _, _)) =>
              s"Do not rush to recover the material; the point is to keep the break-driven initiative alive$execution."
            case Some(CompensationSubtype("kingside", "defender_tied_down", _, _)) =>
              s"Do not rush to recover the material; the point is to keep the initiative alive while the defenders stay tied to the king$execution."
            case Some(CompensationSubtype(_, "conversion_window", _, _)) =>
              s"Do not force the recovery yet; the compensation still has to cash out cleanly$execution."
            case Some(_) =>
              s"Do not rush to recover the material; the point is to keep the compensation structure alive$execution."
            case None =>
              s"Do not rush to recover the material; the point is to keep the initiative alive$execution."
      }

    def compensationObjectiveText(surface: Snapshot): Option[String] =
      Option.when(surface.compensationPosition) {
        val objective = surface.rawObjectiveText.map(obj => s" while working toward $obj").getOrElse("")
        surface.effectiveCompensationSubtype match
          case Some(CompensationSubtype("queenside", "target_fixing", _, _)) =>
            s"keep the fixed queenside targets under pressure before recovering the material$objective"
          case Some(CompensationSubtype(_, "line_occupation", _, "durable_pressure")) =>
            s"keep the open-line pressure durable before recovering the material$objective"
          case Some(CompensationSubtype(_, "counterplay_denial", _, _)) =>
            s"keep counterplay tied down before recovering the material$objective"
          case Some(CompensationSubtype("kingside", "break_preparation", _, _)) =>
            s"keep the break threats alive before cashing out the compensation$objective"
          case Some(CompensationSubtype(_, "conversion_window", _, _)) =>
            s"cash out the compensation into a clean transition$objective"
          case Some(_) =>
            s"keep the compensation alive before recovering the material$objective"
          case None =>
            surface.objectiveText.map(obj => s"keep the initiative alive while working toward $obj")
              .getOrElse("keep the initiative alive while delaying material recovery")
      }

    def compensationExecutionTail(surface: Snapshot): Option[String] =
      compensationExecutionCue(surface).map { execution =>
        surface.effectiveCompensationSubtype match
          case Some(CompensationSubtype("queenside", "target_fixing", _, _)) =>
            s", with the queenside targets pressured through $execution"
          case Some(CompensationSubtype(_, "target_fixing", _, _)) =>
            s", with the pressure routed through $execution"
          case Some(CompensationSubtype("queenside", "line_occupation", _, _)) =>
            s", with the queenside files controlled through $execution"
          case Some(CompensationSubtype("center", "line_occupation", _, _)) =>
            s", with the central files controlled through $execution"
          case Some(CompensationSubtype(_, "counterplay_denial", _, _)) =>
            s", with the clamp reinforced through $execution"
          case Some(CompensationSubtype(_, "conversion_window", _, _)) =>
            s", with the transition still running through $execution"
          case _ =>
            s", with the line pressure running through $execution"
      }

    def compensationSupportText(surface: Snapshot): List[String] =
      surface.effectiveCompensationSubtype.toList.flatMap {
        case CompensationSubtype("queenside", "target_fixing", "intentionally_deferred", _) =>
          List(
            "The material should stay invested until the fixed queenside targets are ready to crack.",
            "The compensation only lasts if the file pressure keeps those targets under fire."
          )
        case CompensationSubtype("queenside", "line_occupation", "intentionally_deferred", "durable_pressure") =>
          List(
            "The material should stay invested until the queenside files are fully under control.",
            "The compensation only lasts if the queenside file pressure stays durable."
          )
        case CompensationSubtype("center", "line_occupation", "intentionally_deferred", "durable_pressure") =>
          List(
            "The material should stay invested until the central files are fully under control.",
            "The compensation only lasts if the central file pressure stays durable."
          )
        case CompensationSubtype(_, "line_occupation", "intentionally_deferred", "durable_pressure") =>
          List(
            "The material should stay invested until the open lines are fully under control.",
            "The compensation only lasts if the line pressure stays durable."
          )
        case CompensationSubtype(_, "counterplay_denial", _, _) =>
          List(
            "The extra pawn has no real value if it never gets counterplay.",
            "The compensation only lasts if the defender stays tied down."
          )
        case CompensationSubtype("kingside", "break_preparation", _, _) =>
          List(
            "The investment is justified only if the break threats keep landing with tempo."
          )
        case CompensationSubtype(_, "conversion_window", _, _) =>
          List(
            "The compensation is already shading toward a transition, so the window has to cash out cleanly."
          )
        case CompensationSubtype(_, "defender_tied_down", _, _) =>
          List(
            "The compensation only works if the defenders keep reacting instead of untangling."
          )
        case _ =>
          Nil
      }.distinct

    private def normalizedDominantIdeaText(subtype: CompensationSubtype): Option[String] =
      subtype match
        case CompensationSubtype("queenside", "target_fixing", _, _) => Some("fixed queenside targets")
        case CompensationSubtype("queenside", "line_occupation", _, _) => Some("queenside file pressure")
        case CompensationSubtype("center", "target_fixing", _, _)     => Some("fixed central targets")
        case CompensationSubtype("center", "line_occupation", _, _)   => Some("central file pressure")
        case CompensationSubtype(_, "target_fixing", _, _)            => Some("fixed targets")
        case CompensationSubtype(_, "counterplay_denial", _, _)       => Some("counterplay denial")
        case CompensationSubtype(_, "defender_tied_down", _, _)       => Some("tying the defenders down")
        case CompensationSubtype(_, "line_occupation", _, _)          => Some("durable line pressure")
        case _                                                        => None

    private def normalizedExecutionText(
        surface: Snapshot,
        subtype: CompensationSubtype
    ): Option[String] =
      val subtypeRoute = alignedRoute(surface, subtype)
      val subtypeMoveRef = alignedMoveRef(surface, subtype)
      val subtypeTarget = alignedDirectionalTarget(surface, subtype)
      val fallback = compensationExecutionCue(surface)
      subtype match
        case CompensationSubtype("queenside", "target_fixing", _, _) =>
          subtypeMoveRef.map(ref => s"${pieceName(ref.piece)} toward ${normalizeText(ref.target).toLowerCase} to keep the fixed queenside targets under pressure")
            .orElse(subtypeRoute.map(route => s"${pieceName(route.piece)} toward ${normalizeText(route.route.lastOption.getOrElse("c4")).toLowerCase} to keep the fixed queenside targets under pressure"))
            .orElse(subtypeTarget.map(target => s"keep the fixed queenside targets under pressure via ${normalizeText(target.targetSquare).toLowerCase}"))
            .orElse(fallback.map(execution => s"$execution to keep the fixed queenside targets under pressure"))
        case CompensationSubtype("queenside", "line_occupation", _, _) =>
          subtypeRoute.map(route => s"${pieceName(route.piece)} toward ${normalizeText(route.route.lastOption.getOrElse("a1")).toLowerCase} to keep the queenside files under pressure")
            .orElse(fallback.map(execution => s"$execution to keep the queenside files under pressure"))
        case CompensationSubtype("center", "target_fixing", _, _) =>
          subtypeMoveRef.map(ref => s"${pieceName(ref.piece)} toward ${normalizeText(ref.target).toLowerCase} to keep the fixed central targets under pressure")
            .orElse(subtypeTarget.map(target => s"keep the fixed central targets under pressure via ${normalizeText(target.targetSquare).toLowerCase}"))
            .orElse(fallback.map(execution => s"$execution to keep the central targets fixed"))
        case CompensationSubtype("center", "line_occupation", _, _) =>
          subtypeRoute.map(route => s"${pieceName(route.piece)} toward ${normalizeText(route.route.lastOption.getOrElse("d4")).toLowerCase} to keep the central files under pressure")
            .orElse(fallback.map(execution => s"$execution to keep the central files under pressure"))
        case CompensationSubtype(_, "counterplay_denial", _, _) =>
          fallback.map(execution => s"$execution to keep counterplay tied down")
        case CompensationSubtype(_, "defender_tied_down", _, _) =>
          fallback.map(execution => s"$execution to keep the defenders tied down")
        case CompensationSubtype(_, "line_occupation", _, _) =>
          fallback.map(execution => s"$execution to keep the line pressure durable")
        case CompensationSubtype(_, "target_fixing", _, _) =>
          fallback.map(execution => s"$execution to keep the targets fixed")
        case _ =>
          fallback

    private def normalizedObjectiveText(subtype: CompensationSubtype): Option[String] =
      subtype match
        case CompensationSubtype("queenside", "target_fixing", _, _) =>
          Some("keeping the fixed queenside targets under pressure before recovering material")
        case CompensationSubtype("queenside", "line_occupation", _, _) =>
          Some("keeping the queenside files under pressure before cashing out")
        case CompensationSubtype("center", "target_fixing", _, _) =>
          Some("keeping the central targets fixed before recovering material")
        case CompensationSubtype("center", "line_occupation", _, _) =>
          Some("keeping the central files under control while delaying recovery")
        case CompensationSubtype(_, "counterplay_denial", _, _) =>
          Some("denying counterplay before converting the compensation")
        case CompensationSubtype(_, "defender_tied_down", _, _) =>
          Some("keeping the defenders tied down until the compensation can cash out")
        case CompensationSubtype(_, "line_occupation", _, _) =>
          Some("keeping the line pressure durable before recovering material")
        case CompensationSubtype(_, "target_fixing", _, _) =>
          Some("keeping the fixed targets under pressure before recovering material")
        case _ =>
          None

    private def normalizedLongTermFocusText(subtype: CompensationSubtype): Option[String] =
      normalizedObjectiveText(subtype)

    private def normalizedCompensationLead(
        surface: Snapshot,
        subtype: CompensationSubtype
    ): Option[String] =
      subtype match
        case CompensationSubtype("queenside", "target_fixing", _, _) =>
          Some("fixed queenside targets and long queenside pressure")
        case CompensationSubtype("queenside", "line_occupation", _, _) =>
          Some("durable queenside file pressure")
        case CompensationSubtype("center", "target_fixing", _, _) =>
          Some("fixed central targets and durable pressure")
        case CompensationSubtype("center", "line_occupation", _, _) =>
          Some("central file control and delayed recovery")
        case CompensationSubtype(_, "counterplay_denial", _, _) =>
          Some("counterplay denial and durable pressure")
        case CompensationSubtype(_, "defender_tied_down", _, _) =>
          Some("keeping the defenders tied down")
        case CompensationSubtype(_, "line_occupation", _, _) =>
          Some("durable line pressure bought by the investment")
        case CompensationSubtype(_, "target_fixing", _, _) =>
          Some("fixed targets and durable pressure")
        case _ =>
          compensationPayoffText(surface)

    private[analysis] def compensationExecutionCue(surface: Snapshot): Option[String] =
      val conciseRoute =
        surface.topRoute.flatMap { route =>
          route.route.lastOption.map(normalizeText).filter(_.nonEmpty).map { square =>
            s"${pieceName(route.piece)} toward ${square.toLowerCase}"
          }
        }
      val conciseMoveRef =
        surface.topMoveRef.map(ref => s"${pieceName(ref.piece)} toward ${normalizeText(ref.target).toLowerCase}")
          .filterNot(_.endsWith(" toward "))
      val conciseTarget =
        surface.topDirectionalTarget.map(target =>
          s"${pieceName(target.piece)} toward ${normalizeText(target.targetSquare).toLowerCase}"
        ).filterNot(_.endsWith(" toward "))
      val genericExecution =
        surface.rawExecutionText
          .map(_.toLowerCase)
          .map(text => text.replaceFirst("\\s+for\\s+.*$", "").trim)
          .filter(_.nonEmpty)
      surface.effectiveCompensationSubtype match
        case Some(CompensationSubtype(theater, mode, _, _))
            if Set("queenside", "center").contains(theater) || Set("line_occupation", "target_fixing", "counterplay_denial").contains(mode) =>
          conciseRoute.orElse(conciseMoveRef).orElse(conciseTarget).orElse(genericExecution)
        case _ =>
          surface.rawExecutionText.map(_.toLowerCase).filter(_.nonEmpty)

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
    val label = StrategicIdeaSelector.humanizedKind(idea.kind)
    val focus = normalizeText(StrategicIdeaSelector.focusSummary(idea))
    Option.when(label.nonEmpty)((List(label) ++ Option.when(focus.nonEmpty)(focus).toList).mkString(" around "))

  private def routeText(route: StrategyPieceRoute): Option[String] =
    val routeSquares = route.route.map(normalizeText).filter(_.nonEmpty)
    val destination = routeSquares.lastOption
    val purpose = normalizeText(route.purpose)
    destination.map { square =>
      val piece = pieceName(route.piece)
      if normalizeText(route.surfaceMode).equalsIgnoreCase(RouteSurfaceMode.Exact) && routeSquares.size >= 2 then
        val path = routeSquares.mkString("-")
        (List(s"$piece via $path") ++ Option.when(purpose.nonEmpty)(s"for $purpose").toList).mkString(" ")
      else (List(s"$piece toward $square") ++ Option.when(purpose.nonEmpty)(s"for $purpose").toList).mkString(" ")
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
      (
        List(s"making $square available for the $piece") ++
          Option.when(normalizeText(target.readiness).nonEmpty)(s"(${normalizeText(target.readiness).toLowerCase})").toList
      ).mkString(" ")
    }

  private def pieceName(code: String): String =
    normalizeText(code).toUpperCase match
      case "P" | "PAWN"   => "pawn"
      case "N" | "KNIGHT" => "knight"
      case "B" | "BISHOP" => "bishop"
      case "R" | "ROOK"   => "rook"
      case "Q" | "QUEEN"  => "queen"
      case "K" | "KING"   => "king"
      case other          => normalizeText(other).toLowerCase

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""[_\-]+""", " ").replaceAll("\\s+", " ").trim

  private def payoffSignalTexts(surface: Snapshot): List[String] =
    (
      surface.longTermFocus.toList ++
        surface.evidenceHints ++
        surface.strategicStack ++
        surface.latentPlan.toList ++
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
    val decisionAnchors =
      surface.decisionEvidence.toList.flatMap { evidence =>
        val lowered = normalizeText(evidence).toLowerCase
        Option.when(
          containsAny(lowered, List("attacking fixed pawn", "fixed target", "fixed pawn", "contest the pawn"))
        ) {
          """\b[a-h][1-8]\b""".r.findAllIn(lowered).toList.flatMap(theaterFromSquare)
        }.getOrElse(Nil)
      }
    ideaAnchors ++ objectiveAnchors ++ moveRefAnchors ++ directionalAnchors ++ decisionAnchors

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
      (
        surface.evidenceHints ++
          surface.strategicStack ++
          surface.latentPlan.toList ++
          surface.decisionEvidence.toList
      ).count(text =>
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
    (
      surface.evidenceHints ++
        surface.strategicStack ++
        surface.latentPlan.toList ++
        surface.decisionEvidence.toList
    ).count(text =>
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
    surface.latentPlan.exists(plan =>
      containsAny(normalizeText(plan).toLowerCase, List("attacking fixed pawn", "fixed target", "fixed pawn"))
    ) ||
      surface.decisionEvidence.exists(evidence =>
        containsAny(normalizeText(evidence).toLowerCase, List("attacking fixed pawn", "fixed target", "fixed pawn"))
      )

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
    surface.decisionEvidence.foreach { evidence =>
      val lowered = normalizeText(evidence).toLowerCase
      if containsAny(lowered, List("attacking fixed pawn", "fixed target", "fixed pawn")) then
        """\b[a-h][1-8]\b""".r.findAllIn(lowered).foreach { square =>
          theaterFromSquare(square).foreach(add(_, 2))
        }
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
        val evidenceAnchors = surface.decisionEvidence.toList.flatMap(theatersFromText)
        val counts = (focusAnchors ++ evidenceAnchors).groupMapReduce(identity)(_ => 1)(_ + _)
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

  private def alignedRoute(surface: Snapshot, subtype: CompensationSubtype): Option[StrategyPieceRoute] =
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

  private def alignedMoveRef(surface: Snapshot, subtype: CompensationSubtype): Option[StrategyPieceMoveRef] =
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

  private def alignedDirectionalTarget(surface: Snapshot, subtype: CompensationSubtype): Option[StrategyDirectionalTarget] =
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

  private def theaterDisplay(theater: String): String =
    theater match
      case "kingside"  => "kingside"
      case "queenside" => "queenside"
      case "center"    => "central"
      case _           => "mixed"

private[analysis] object StrategicThesisBuilder:

  def build(ctx: NarrativeContext, strategyPack: Option[StrategyPack] = None): Option[StrategicThesis] =
    val surface = StrategyPackSurface.from(strategyPack)
    buildCompensation(ctx, surface)
      .orElse(buildProphylaxis(ctx, surface))
      .orElse(buildStructure(ctx, surface))
      .orElse(buildDecision(ctx, surface))
      .orElse(buildPractical(ctx))
      .orElse(buildOpening(ctx))

  private def buildCompensation(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
    val compensationDecision = CompensationInterpretation.effectiveConsumerDecision(ctx, surface)
    val vectors = compensationDecision.map(_.signal.vectors).filter(_.nonEmpty).getOrElse(Nil)
    val plan =
      compensationDecision.flatMap(_.signal.summary).map(normalizeText).filter(_.nonEmpty)
    val investedMaterial = compensationDecision.flatMap(_.signal.investedMaterial).filter(_ > 0)
    val hasCompensationSignal = compensationDecision.isDefined
    if !hasCompensationSignal then None
    else
      val vectorText = joinNatural(vectors)
      val subtypePayoff =
        Option.when(surface.normalizationActive) {
          surface.normalizedCompensationLead.orElse(StrategyPackSurface.compensationPayoffText(surface))
        }.flatten
      val payoff =
        subtypePayoff.getOrElse(
          if vectorText.nonEmpty then vectorText
          else
            surface.compensationSummary.map(_.toLowerCase).filter(_.nonEmpty)
              .orElse(plan.map(_.toLowerCase))
              .orElse(surface.dominantIdeaText.map(_.toLowerCase))
              .getOrElse("long-term compensation")
        )
      val surfaceAnchor =
        surface.dominantIdeaText
          .map(idea => s" while keeping ${idea.toLowerCase} as the dominant thesis")
          .orElse(surface.executionText.map(execution => s" through ${execution.toLowerCase}"))
          .orElse(surface.objectiveText.map(objective => s" with the initiative aimed at ${objective.toLowerCase}"))
          .getOrElse("")
      val executionTail =
        surface.executionText
          .filter(execution => surface.dominantIdeaText.forall(idea => !execution.equalsIgnoreCase(idea)))
          .flatMap(_ => StrategyPackSurface.compensationExecutionTail(surface))
          .getOrElse("")
      val claim =
        investedMaterial match
          case Some(investment) =>
            surface.campaignOwnerText.filter(_ => surface.ownerMismatch)
              .map(side =>
                s"$side is not trying to win the material back immediately; the ${investment}cp compensation investment is for $payoff$surfaceAnchor$executionTail."
              )
              .getOrElse(
                s"The point of the move is not an immediate score, but to accept a ${investment}cp compensation investment for $payoff$surfaceAnchor$executionTail."
              )
          case None =>
            surface.campaignOwnerText.filter(_ => surface.ownerMismatch)
              .map(side => s"$side is still playing for compensation through $payoff$surfaceAnchor$executionTail, not immediate recovery.")
              .getOrElse(s"The move keeps the compensation alive through $payoff$surfaceAnchor$executionTail rather than immediate recovery.")
      val compensationSupportBase = List(
          plan
            .filterNot(value =>
              surface.executionText.exists(_.equalsIgnoreCase(value)) ||
                surface.objectiveText.exists(_.equalsIgnoreCase(value))
            )
            .map(value => s"The compensation has to cash out through $value."),
          Option.when(surface.normalizationActive)(surface.effectiveCompensationSubtype).flatten.flatMap { subtype =>
            Option.when(subtype.recoveryPolicy == "intentionally_deferred" && plan.isEmpty) {
              StrategyPackSurface.compensationObjectiveText(surface).getOrElse("The material should stay invested until the compensation is ready to cash out.")
            }
          },
          Option.when(surface.normalizationActive)(surface.effectiveCompensationSubtype).flatten.flatMap { _ =>
            StrategyPackSurface.compensationPersistenceText(surface).map(reason => s"The compensation only works if $reason.")
          },
          Option.when(vectorText.nonEmpty)(s"The return vector only holds if the initiative keeps generating $vectorText."),
          Option.when(plan.isEmpty && vectorText.isEmpty) {
            surface.executionText
              .map(execution => s"The delayed recovery only works if the line pressure keeps building through $execution.")
              .orElse(surface.objectiveText.map(objective => s"The initiative only holds if the compensation stays aimed at $objective."))
          }.flatten
        ).flatten
      val subtypeSupport = Option.when(surface.normalizationActive)(StrategyPackSurface.compensationSupportText(surface)).getOrElse(Nil)
      val orderedCompensationSupport =
        if surface.quietCompensationPosition then subtypeSupport ++ compensationSupportBase
        else compensationSupportBase ++ subtypeSupport
      val support = (orderedCompensationSupport ++ strategySupport(surface)).distinct
      Some(
        StrategicThesis(
          lens = StrategicLens.Compensation,
          claim = claim,
          support = support.take(2),
          tension = opponentOrAbsenceTension(ctx),
          evidenceHook = NarrativeEvidenceHooks.build(ctx)
        )
      )

  private def buildProphylaxis(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
    ctx.semantic.flatMap(_.preventedPlans.headOption).flatMap { prevented =>
      val target =
        prevented.preventedThreatType.map(normalizeText).filter(_.nonEmpty)
          .orElse(prevented.breakNeutralized.map(file => s"$file-break"))
          .orElse(Option.when(normalizeText(prevented.planId).nonEmpty)(normalizeText(prevented.planId)))
      val hasSignal = target.nonEmpty || prevented.counterplayScoreDrop > 0
      if !hasSignal then None
      else
        val targetText = target.getOrElse("the opponent's easiest counterplay")
        val claim =
          surface.dominantIdeaText.map(idea =>
            s"The move matters less for a direct gain than for cutting out $targetText and keeping $idea alive."
          ).getOrElse(s"The move matters less for a direct gain than for cutting out $targetText.")
        val support = strategySupport(surface) ++ List(
          Option.when(prevented.counterplayScoreDrop > 0)(
            s"That strips away roughly ${prevented.counterplayScoreDrop}cp of counterplay."
          ),
          leadingPlanName(ctx).map(plan => s"It also gives $plan more time to take hold.")
        ).flatten
        Some(
          StrategicThesis(
            lens = StrategicLens.Prophylaxis,
            claim = claim,
            support = support.take(2),
            tension = opponentPlanTension(ctx),
            evidenceHook = NarrativeEvidenceHooks.build(ctx)
          )
        )
    }

  private def buildStructure(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
    val semantic = ctx.semantic
    val profileOpt = semantic.flatMap(_.structureProfile)
    val alignmentOpt = semantic.flatMap(_.planAlignment)
    val hasStructure = profileOpt.isDefined || alignmentOpt.exists(pa => pa.reasonCodes.nonEmpty || normalizeText(pa.band).nonEmpty)
    if !hasStructure then None
    else
      val arcOpt = StructurePlanArcBuilder.build(ctx)
      val structureName = profileOpt.map(_.primary).map(normalizeText).filter(_.nonEmpty)
      val centerState = profileOpt.map(_.centerState).map(normalizeText).filter(_.nonEmpty)
      val leadPlan =
        leadingPlanName(ctx)
          .orElse(alignmentOpt.flatMap(_.narrativeIntent).map(normalizeText).filter(_.nonEmpty))
      val claim =
        arcOpt.filter(StructurePlanArcBuilder.proseEligible).map(StructurePlanArcBuilder.claimText)
          .getOrElse {
            val structureLead = structureName.orElse(centerState).getOrElse("the structure")
            val planLead = leadPlan.getOrElse("the natural plan")
            val centerText =
              centerState.filterNot(s => structureName.contains(s)).map(s => s" and its ${s.toLowerCase} center").getOrElse("")
            val ideaTail =
              surface.dominantIdeaText.filterNot(_.equalsIgnoreCase(planLead)).map(idea => s" and keeping $idea in view").getOrElse("")
            s"The move reorganizes the pieces around $structureLead$centerText, aiming at $planLead$ideaTail."
          }
      val support =
        val structuralSupport =
          arcOpt match
          case Some(arc) if StructurePlanArcBuilder.proseEligible(arc) =>
            List(
              Some(StructurePlanArcBuilder.supportPrimaryText(arc)),
              Some(StructurePlanArcBuilder.supportSecondaryText(arc))
            ).flatten
          case Some(arc) =>
            List(
              alignmentOpt.flatMap(_.narrativeIntent).map(intent => s"It follows the structure's logic of ${normalizeSentenceFragment(intent)}."),
              Some(StructurePlanArcBuilder.cautionSupportText(arc)),
              alignmentOpt.flatMap(_.narrativeRisk).map(risk => s"That makes move order matter because ${normalizeSentenceFragment(risk)}.")
            ).flatten
          case None =>
            List(
              alignmentOpt.flatMap(_.narrativeIntent).map(intent => s"It follows the structure's logic of ${normalizeSentenceFragment(intent)}."),
              alignmentOpt.flatMap(_.narrativeRisk).map(risk => s"That makes move order matter because ${normalizeSentenceFragment(risk)}."),
              Option.when(alignmentOpt.exists(_.reasonCodes.nonEmpty)) {
                val reasons = alignmentOpt.toList.flatMap(_.reasonCodes).map(humanizeCode).filter(_.nonEmpty).take(2)
                Option.when(reasons.nonEmpty)(s"The plan fit is shaped by ${joinNatural(reasons)}.").getOrElse("")
              }.filter(_.nonEmpty)
            ).flatten
        strategySupport(surface) ++ structuralSupport
      val tension =
        arcOpt.flatMap(_.prophylaxisSupport)
          .orElse(alignmentOpt.flatMap(_.narrativeRisk).map(risk => s"That still leaves ${normalizeSentenceFragment(risk)}."))
          .orElse(opponentOrAbsenceTension(ctx))
      Some(
        StrategicThesis(
          lens = StrategicLens.Structure,
          claim = claim,
          support = support.take(2),
          tension = tension,
          evidenceHook = NarrativeEvidenceHooks.build(ctx)
        )
      )

  private def strategySupport(surface: StrategyPackSurface.Snapshot): List[String] =
    List(
      surface.executionText.map(execution => s"The execution still runs through $execution."),
      surface.objectiveText
        .filter(objective => surface.executionText.forall(execution => !objective.equalsIgnoreCase(execution)))
        .map(objective => s"The long-term objective is $objective."),
      surface.campaignOwnerText.filter(_ => surface.ownerMismatch).map(side => s"$side still own the campaign here.")
    ).flatten.filter(_.nonEmpty)

  private def hasStrategicClaimSurface(surface: StrategyPackSurface.Snapshot): Boolean =
    surface.dominantIdeaText.nonEmpty ||
      surface.executionText.nonEmpty ||
      surface.objectiveText.nonEmpty ||
      surface.compensationPosition ||
      surface.investedMaterial.exists(_ > 0)

  private def decisionClaimFromSurface(
      surface: StrategyPackSurface.Snapshot,
      deferred: String
  ): Option[String] =
    Option.when(hasStrategicClaimSurface(surface)) {
      val lead =
        if surface.compensationPosition then
          surface.dominantIdeaText
            .map(idea => s"The move keeps the compensation alive around $idea")
            .orElse(surface.executionText.map(execution => s"The move keeps the compensation alive through $execution"))
            .orElse(surface.objectiveText.map(objective => s"The move keeps the initiative tied to $objective"))
        else
          surface.dominantIdeaText
            .map(idea => s"The move keeps $idea as the dominant thesis")
            .orElse(surface.executionText.map(execution => s"The move keeps the plan grounded in $execution"))
            .orElse(surface.objectiveText.map(objective => s"The move keeps the plan pointed at $objective"))
      lead.map { base =>
        val executionTail =
          surface.executionText
            .filter(execution => surface.dominantIdeaText.forall(idea => !execution.equalsIgnoreCase(idea)))
            .map { execution =>
              if surface.compensationPosition then s", with the line pressure running through $execution"
              else s", with the execution running through $execution"
            }
            .getOrElse("")
        val deferredTail =
          Option.when(normalizeText(deferred).nonEmpty && deferred != "the most direct alternative") {
            s" rather than drifting into $deferred"
          }.getOrElse("")
        s"$base$executionTail$deferredTail."
      }
    }.flatten

  private def renderDecisionFocalSupport(
      target: TargetRef,
      surface: StrategyPackSurface.Snapshot
  ): String =
    val rendered = renderTargetRef(target)
    if surface.compensationPosition then s"$rendered is the square where the compensation has to stay alive."
    else
      surface.dominantIdeaText.map(idea => s"$rendered is the square that keeps $idea grounded.")
        .orElse(surface.objectiveText.map(_ => s"$rendered is the square the move is really playing for."))
        .orElse(Option.when(hasStrategicClaimSurface(surface))(s"The move is really playing for $rendered."))
        .getOrElse(s"The whole decision turns on $rendered.")

  private def buildDecision(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
    ctx.decision.flatMap { decision =>
      val hasSignal =
        ctx.whyAbsentFromTopMultiPV.nonEmpty || ctx.authorEvidence.nonEmpty || ctx.opponentPlan.isDefined
      if !hasSignal then None
      else
        val chosen =
          leadingPlanName(ctx)
            .orElse(normalizedDecisionSummary(decision.logicSummary))
            .getOrElse("the main continuation")
        val deferred =
          AlternativeNarrativeSupport.moveLabel(ctx)
            .orElse(ctx.meta.flatMap(_.whyNot).flatMap(extractQuotedMove))
            .getOrElse("the most direct alternative")
        val reason =
          normalizedDecisionSummary(decision.logicSummary)
            .orElse(ctx.whyAbsentFromTopMultiPV.headOption.map(normalizeSentenceFragment))
            .getOrElse("it keeps the move order coherent")
        val surfaceClaim = decisionClaimFromSurface(surface, deferred)
        val claim =
          surfaceClaim.getOrElse(s"The key decision is to choose $chosen and postpone $deferred, because $reason.")
        val focalSupport = ctx.decision.flatMap(_.focalPoint.map(target => renderDecisionFocalSupport(target, surface)))
        val support =
          (
            if surfaceClaim.isDefined || hasStrategicClaimSurface(surface) then
              List(
                surface.objectiveText
                  .filter(objective => surface.executionText.forall(execution => !objective.equalsIgnoreCase(execution)))
                  .map(objective =>
                    if surface.compensationPosition then s"The compensation still has to cash out toward $objective."
                    else s"The objective is $objective."
                  ),
                Some(s"$deferred stays secondary because $reason."),
                buildDecisionDeltaSupport(decision.delta),
                focalSupport
              )
            else
              List(
                buildDecisionDeltaSupport(decision.delta),
                focalSupport
              )
          ).flatten.distinct
        Some(
          StrategicThesis(
            lens = StrategicLens.Decision,
            claim = claim,
            support = support.take(2),
            tension = opponentOrAbsenceTension(ctx),
            evidenceHook = NarrativeEvidenceHooks.build(ctx)
          )
        )
    }

  private def buildPractical(ctx: NarrativeContext): Option[StrategicThesis] =
    ctx.semantic.flatMap(_.practicalAssessment).flatMap { practical =>
      val verdict = normalizeText(practical.verdict)
      val drivers = practical.biasFactors.sortBy(b => -Math.abs(b.weight)).take(2).map(renderBiasFactor)
      if verdict.isEmpty && drivers.isEmpty then None
      else
        val driverText = joinNatural(drivers)
        val claim =
          if driverText.nonEmpty then
            s"More important than the nominal evaluation is that the move creates an easier practical task through $driverText."
          else
            s"More important than the nominal evaluation is that the move creates a ${verdict.toLowerCase} practical task."
        val support = List(
          Option.when(verdict.nonEmpty)(s"The resulting task is ${verdict.toLowerCase.stripSuffix(".")}."),
          Option.when(driverText.nonEmpty)(s"That matters because $driverText shape the workload.")
        ).flatten
        Some(
          StrategicThesis(
            lens = StrategicLens.Practical,
            claim = claim,
            support = support.take(2),
            tension = opponentOrAbsenceTension(ctx),
            evidenceHook = NarrativeEvidenceHooks.build(ctx)
          )
        )
    }

  private def buildOpening(ctx: NarrativeContext): Option[StrategicThesis] =
    openingLabel(ctx).flatMap { openingName =>
      val planClue =
        leadingPlanName(ctx)
          .orElse(ctx.semantic.flatMap(_.structureProfile).map(_.primary).map(normalizeText).filter(_.nonEmpty))
      if planClue.isEmpty then None
      else
        val claim = s"The move extends $openingName ideas toward ${planClue.get.toLowerCase}."
        val precedentBranch = OpeningPrecedentBranching.representative(ctx, ctx.openingData, requireFocus = true)
        val support = List(
          precedentBranch.map(_.representativeSentence),
          OpeningPrecedentBranching.relationSentence(ctx, ctx.openingData, requireFocus = true),
          precedentBranch.map(_.summarySentence),
          ctx.openingEvent.map(renderOpeningEventSupport),
          ctx.semantic.flatMap(_.structureProfile).map { profile =>
            val center = normalizeText(profile.centerState)
            if center.nonEmpty then s"The position already points to a ${center.toLowerCase} center and long-term maneuvering."
            else s"The structure already matches the long-term themes of $openingName."
          }
        ).flatten
        Some(
          StrategicThesis(
            lens = StrategicLens.Opening,
            claim = claim,
            support = support.take(2),
            tension = opponentOrAbsenceTension(ctx),
            evidenceHook = NarrativeEvidenceHooks.build(ctx)
          )
        )
    }

  private def openingLabel(ctx: NarrativeContext): Option[String] =
    ctx.openingData.flatMap(_.name).map(normalizeText).filter(_.nonEmpty)
      .orElse {
        ctx.openingEvent.collect {
          case OpeningEvent.Intro(_, name, _, _) => normalizeText(name)
        }.find(_.nonEmpty)
      }

  private def opponentOrAbsenceTension(ctx: NarrativeContext): Option[String] =
    AlternativeNarrativeSupport.sentence(ctx).orElse(opponentPlanTension(ctx))

  private def leadingPlanName(ctx: NarrativeContext): Option[String] =
    ctx.mainStrategicPlans.headOption.map(_.planName).map(normalizeText).filter(_.nonEmpty)
      .orElse(ctx.plans.top5.headOption.map(_.name).map(normalizeText).filter(_.nonEmpty))

  private def opponentPlanTension(ctx: NarrativeContext): Option[String] =
    ctx.opponentPlan.map(_.name).map(normalizeText).filter(_.nonEmpty).map { plan =>
      s"The main counterplay still revolves around $plan."
    }

  private def buildDecisionDeltaSupport(delta: PVDelta): Option[String] =
    val actions = List(
      delta.resolvedThreats.headOption.map(threat => s"resolving $threat"),
      delta.newOpportunities.headOption.map(target => s"creating pressure on ${normalizeSentenceFragment(target)}"),
      delta.planAdvancements.headOption.map(step => s"advancing ${normalizeSentenceFragment(step.replace("Met:", "").replace("Removed:", ""))}")
    ).flatten
    actions match
      case a :: b :: _ => Some(s"It does that by $a and then $b.")
      case a :: Nil    => Some(s"It does that by $a.")
      case Nil         => None

  private def renderOpeningEventSupport(event: OpeningEvent): String =
    event match
      case OpeningEvent.Intro(_, name, theme, _) if normalizeText(theme).nonEmpty =>
        s"The game is still in ${normalizeText(name)} territory, with $theme as the long-term guide."
      case OpeningEvent.BranchPoint(divergingMoves, _, _) if divergingMoves.nonEmpty =>
        s"The opening now branches around ${joinNatural(divergingMoves.take(3))}."
      case OpeningEvent.OutOfBook(playedMove, _, _) =>
        s"This already functions as an out-of-book decision after $playedMove."
      case OpeningEvent.Novelty(playedMove, cpLoss, _, _) =>
        val cost = if cpLoss > 0 then s" for only about ${cpLoss}cp" else ""
        s"This is effectively a novelty with $playedMove$cost."
      case OpeningEvent.TheoryEnds(_, sampleCount) =>
        s"Theory is already thinning out here, with only about $sampleCount games left in sample."
      case _ =>
        "The opening reference is already giving way to independent strategic play."

  private def renderBiasFactor(bias: PracticalBiasInfo): String =
    val factor = normalizeText(bias.factor).toLowerCase
    val description = normalizeText(bias.description)
    if factor.nonEmpty && description.nonEmpty && !description.equalsIgnoreCase(factor) then s"$factor ($description)"
    else if factor.nonEmpty then factor
    else description.toLowerCase

  private def extractQuotedMove(raw: String): Option[String] =
    "\"([^\"]+)\"".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).trim).filter(_.nonEmpty)
      .orElse("'([^']+)'".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).trim).filter(_.nonEmpty))

  private def humanizeCode(code: String): String =
    normalizeText(code).replace("pa ", "").replace("req ", "").replace("sup ", "")

  private def renderTargetRef(target: TargetRef): String =
    target match
      case TargetSquare(key)        => key
      case TargetFile(file)         => s"$file-file"
      case TargetPiece(role, square) => s"${NarrativeUtils.humanize(role)} on $square"

  private def joinNatural(items: List[String]): String =
    items.map(normalizeText).filter(_.nonEmpty).distinct match
      case Nil => ""
      case one :: Nil => one
      case a :: b :: Nil => s"$a and $b"
      case many => s"${many.dropRight(1).mkString(", ")}, and ${many.last}"

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""[_\-]+""", " ").replaceAll("\\s+", " ").trim

  private def normalizeSentenceFragment(raw: String): String =
    normalizeText(raw).stripSuffix(".")

  private def normalizedDecisionSummary(raw: String): Option[String] =
    Option(raw)
      .map(normalizeSentenceFragment)
      .map(_.replace(" -> ", "; then ").replace("->", "; then "))
      .map(_.stripPrefix("The idea is ").stripPrefix("the idea is ").trim)
      .filter(_.nonEmpty)
