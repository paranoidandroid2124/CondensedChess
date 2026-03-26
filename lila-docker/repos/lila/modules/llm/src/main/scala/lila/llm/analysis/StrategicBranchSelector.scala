package lila.llm.analysis

import lila.llm.{ ActiveStrategicThread, ActiveStrategicThreadRef, GameChronicleMoment }

object StrategicBranchSelector:

  private val OpeningEventMomentTypes = Set(
    "OpeningBranchPoint",
    "OpeningOutOfBook",
    "OpeningNovelty",
    "OpeningTheoryEnds"
  )
  private val MaxThreads = 3
  private val MaxVisibleMoments = 12
  private val MaxVisibleThreadMoments = MaxThreads * 3
  private val MaxActiveNotes = 8

  final case class StrategicBranchSelection(
      selectedMoments: List[GameChronicleMoment],
      activeNoteMoments: List[GameChronicleMoment],
      threads: List[ActiveStrategicThread],
      threadRefsByPly: Map[Int, ActiveStrategicThreadRef]
  )

  private case class TruthSelectionView(
      classificationKey: String,
      ownershipRole: TruthOwnershipRole,
      visibilityRole: TruthVisibilityRole,
      surfaceMode: TruthSurfaceMode,
      exemplarRole: TruthExemplarRole,
      chainKey: Option[String],
      maintenanceExemplarCandidate: Boolean,
      reasonFamily: DecisiveReasonFamily,
      failureMode: FailureInterpretationMode,
      benchmarkCriticalMove: Boolean
  )

  def select(moments: List[GameChronicleMoment]): List[GameChronicleMoment] =
    buildSelection(moments).selectedMoments

  def rankThreads(moments: List[GameChronicleMoment]): List[ActiveStrategicThreadBuilder.BuiltThread] =
    sortThreads(ActiveStrategicThreadBuilder.build(moments).builtThreads)

  def buildSelection(moments: List[GameChronicleMoment]): StrategicBranchSelection =
    buildSelection(moments, Map.empty)

  def buildSelection(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): StrategicBranchSelection =
    val threadResult = ActiveStrategicThreadBuilder.build(moments)
    val rankedThreads = sortThreads(threadResult.builtThreads).take(MaxThreads)
    val rankedThreadIds = rankedThreads.map(_.thread.threadId).toSet
    val threadRefsByPly =
      threadResult.threadRefsByPly.filter { case (_, ref) => rankedThreadIds.contains(ref.threadId) }
    val threadedPlies = threadResult.threadRefsByPly.keySet
    val protectedVisibleMoments =
      selectProtectedVisibleMoments(moments, truthContractsByPly)
    val protectedVisiblePlies = protectedVisibleMoments.map(_.ply).toSet
    val remainingVisibleSlotsAfterProtected = (MaxVisibleMoments - protectedVisibleMoments.size).max(0)
    val visibleThreadMoments =
      rankedThreads
        .flatMap(thread => selectRepresentatives(thread, truthContractsByPly))
        .distinctBy(_.ply)
        .filterNot(moment => protectedVisiblePlies.contains(moment.ply))
        .take(MaxVisibleThreadMoments.min(remainingVisibleSlotsAfterProtected))
    val visibleThreadPlies = visibleThreadMoments.map(_.ply).toSet
    val visibleCompensationMoments =
      suppressInvestmentEchoes(
        selectCompensationPriorityMoments(moments, threadedPlies, truthContractsByPly),
        truthContractsByPly
      )
        .filterNot(moment => protectedVisiblePlies.contains(moment.ply) || visibleThreadPlies.contains(moment.ply))
        .take((remainingVisibleSlotsAfterProtected - visibleThreadMoments.size).max(0))
    val visibleCompensationPlies = visibleCompensationMoments.map(_.ply).toSet
    val visibleCoreEvents =
      suppressInvestmentEchoes(
        visibleCompensationMoments ++ selectCoreNonThreadEvents(moments, threadedPlies, truthContractsByPly),
        truthContractsByPly
      )
        .filterNot(moment =>
          protectedVisiblePlies.contains(moment.ply) ||
            visibleThreadPlies.contains(moment.ply) ||
            visibleCompensationPlies.contains(moment.ply)
        )
        .take((remainingVisibleSlotsAfterProtected - visibleThreadMoments.size - visibleCompensationMoments.size).max(0))
    val visibleCoreEventPlies = visibleCoreEvents.map(_.ply).toSet
    val visibleFallbackCandidates =
      selectStrategicFallbackMoments(moments, threadedPlies, truthContractsByPly)
        .filterNot(moment =>
          protectedVisiblePlies.contains(moment.ply) ||
            visibleThreadPlies.contains(moment.ply) ||
            visibleCompensationPlies.contains(moment.ply) ||
            visibleCoreEventPlies.contains(moment.ply)
        )
    val visibleFallbackMoments =
      suppressInvestmentEchoes(
        visibleCompensationMoments ++ visibleCoreEvents ++ visibleFallbackCandidates,
        truthContractsByPly
      )
        .filterNot(moment =>
          protectedVisiblePlies.contains(moment.ply) ||
            visibleThreadPlies.contains(moment.ply) ||
            visibleCompensationPlies.contains(moment.ply) ||
            visibleCoreEventPlies.contains(moment.ply)
        )
        .take(
          (
            remainingVisibleSlotsAfterProtected -
              visibleThreadMoments.size -
              visibleCompensationMoments.size -
              visibleCoreEvents.size
          ).max(0)
        )
    val prioritizedVisibleMoments =
      (
        protectedVisibleMoments ++
          visibleThreadMoments ++
          visibleCompensationMoments ++
          visibleCoreEvents ++
          visibleFallbackMoments
      ).distinctBy(_.ply).take(MaxVisibleMoments)
    val visibleMoments =
      prioritizedVisibleMoments
        .sortBy(_.ply)
    val protectedActiveNoteMoments =
      selectProtectedActiveNoteMoments(moments, truthContractsByPly)
    val protectedActiveNotePlies = protectedActiveNoteMoments.map(_.ply).toSet
    val remainingActiveNoteSlotsAfterProtected = (MaxActiveNotes - protectedActiveNoteMoments.size).max(0)
    val activeNoteFallbackMoments =
      selectActiveNoteFallbackMoments(moments, visibleThreadPlies, truthContractsByPly)
        .filterNot(moment => protectedActiveNotePlies.contains(moment.ply))
        .take(
          (
            remainingActiveNoteSlotsAfterProtected -
              visibleThreadMoments.count(moment => !protectedActiveNotePlies.contains(moment.ply))
          ).max(0)
        )
    val prioritizedActiveNoteMoments =
      if rankedThreads.isEmpty then
        (
          protectedActiveNoteMoments ++
            activeNoteFallbackMoments
        ).distinctBy(_.ply).take(MaxActiveNotes)
      else
        (
          protectedActiveNoteMoments ++
            (visibleThreadMoments ++ activeNoteFallbackMoments)
              .filterNot(moment => protectedActiveNotePlies.contains(moment.ply))
        ).distinctBy(_.ply).take(MaxActiveNotes)

    if rankedThreads.isEmpty then
      StrategicBranchSelection(
        selectedMoments = visibleMoments,
        activeNoteMoments = prioritizedActiveNoteMoments,
        threads = Nil,
        threadRefsByPly = Map.empty
      )
    else
      StrategicBranchSelection(
        selectedMoments = visibleMoments,
        activeNoteMoments = prioritizedActiveNoteMoments,
        threads = rankedThreads.map(_.thread),
        threadRefsByPly = threadRefsByPly
      )

  private def selectCoreNonThreadEvents(
      moments: List[GameChronicleMoment],
      threadedPlies: Set[Int],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    moments
      .filterNot(moment =>
        threadedPlies.contains(moment.ply) ||
          moment.selectionKind == "thread_bridge" ||
          moment.momentType == "OpeningIntro"
      )
      .flatMap(moment => coreEventPriority(moment, truthContractsByPly).map(priority => priority -> moment))
      .sortBy { case ((priority, tiebreak), moment) => (priority, tiebreak, moment.ply) }
      .map(_._2)

  private def coreEventPriority(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int)] =
    val truthView = truthSelectionView(moment, truthContractsByPly)
    if isSevereFailure(truthView) then Some(1 -> moment.ply)
    else if isPromotedBestHold(truthView) then Some(2 -> moment.ply)
    else if isVerifiedExemplar(truthView) then Some(3 -> moment.ply)
    else if isProvisionalExemplar(truthView) || isPrimaryConversionOwner(truthView, moment) then Some(4 -> moment.ply)
    else if isSelectionExemplar(truthView) then Some(5 -> moment.ply)
    else if isMaintenanceEcho(truthView) then Some(6 -> moment.ply)
    else if isMatePivot(moment) then Some(7 -> moment.ply)
    else if isOpeningBranchEvent(moment) then Some(8 -> moment.ply)
    else None

  private def isBlunder(truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.BlunderOwner &&
      truthView.classificationKey == "blunder"

  private def isMissedWin(truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.BlunderOwner &&
      truthView.classificationKey == "missedwin"

  private def isSevereFailure(truthView: TruthSelectionView): Boolean =
    isBlunder(truthView) || isMissedWin(truthView)

  private def isPromotedBestHold(truthView: TruthSelectionView): Boolean =
    truthView.classificationKey == "best" &&
      truthView.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense &&
      truthView.benchmarkCriticalMove

  private def isCriticalBestTacticalOrTechnical(truthView: TruthSelectionView): Boolean =
    truthView.classificationKey == "best" &&
      (
        truthView.reasonFamily == DecisiveReasonFamily.TacticalRefutation ||
          (
            truthView.reasonFamily == DecisiveReasonFamily.QuietTechnicalMove &&
              truthView.benchmarkCriticalMove
          )
      )

  private def isFailureSignificantThreadLocal(truthView: TruthSelectionView): Boolean =
    truthView.classificationKey != "best" &&
      truthView.failureMode != FailureInterpretationMode.NoClearPlan &&
      (
        truthView.reasonFamily == DecisiveReasonFamily.TacticalRefutation ||
          truthView.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense ||
          truthView.reasonFamily == DecisiveReasonFamily.QuietTechnicalMove
      )

  private def isReplacementOnlyThreadLocal(truthView: TruthSelectionView): Boolean =
    isCriticalBestTacticalOrTechnical(truthView)

  private def isMatePivot(moment: GameChronicleMoment): Boolean =
    moment.momentType == "MatePivot"

  private def isExemplarPivot(truthView: TruthSelectionView): Boolean =
    truthView.exemplarRole == TruthExemplarRole.VerifiedExemplar ||
      truthView.exemplarRole == TruthExemplarRole.ProvisionalExemplar ||
      truthView.maintenanceExemplarCandidate

  private def isOpeningBranchEvent(moment: GameChronicleMoment): Boolean =
    OpeningEventMomentTypes.contains(moment.momentType)

  private def isVerifiedExemplar(truthView: TruthSelectionView): Boolean =
    truthView.exemplarRole == TruthExemplarRole.VerifiedExemplar &&
      truthView.visibilityRole == TruthVisibilityRole.PrimaryVisible

  private def isProvisionalExemplar(truthView: TruthSelectionView): Boolean =
    truthView.exemplarRole == TruthExemplarRole.ProvisionalExemplar &&
      truthView.visibilityRole != TruthVisibilityRole.Hidden

  private def isSelectionExemplar(truthView: TruthSelectionView): Boolean =
    isProvisionalExemplar(truthView) ||
      (
        truthView.maintenanceExemplarCandidate &&
          truthView.visibilityRole != TruthVisibilityRole.Hidden
      )

  private def isMaintenanceEcho(truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.MaintenanceEcho &&
      truthView.visibilityRole != TruthVisibilityRole.Hidden

  private def isPrimaryConversionOwner(
      truthView: TruthSelectionView,
      moment: GameChronicleMoment
  ): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.ConversionOwner &&
      truthView.visibilityRole != TruthVisibilityRole.Hidden &&
      (
        truthView.surfaceMode == TruthSurfaceMode.ConversionExplain ||
          moment.transitionType.exists(_.toLowerCase.contains("convert")) ||
          moment.transitionType.exists(_.toLowerCase.contains("promotion")) ||
          moment.transitionType.exists(_.toLowerCase.contains("exchange")) ||
          moment.transitionType.exists(_.toLowerCase.contains("simplif"))
      )

  private def truthSelectionView(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): TruthSelectionView =
    val projection = DecisiveTruth.momentProjection(moment, truthContractsByPly.get(moment.ply))
    TruthSelectionView(
      classificationKey = projection.classificationKey,
      ownershipRole = projection.ownershipRole,
      visibilityRole = projection.visibilityRole,
      surfaceMode = projection.surfaceMode,
      exemplarRole = projection.exemplarRole,
      chainKey = projection.chainKey,
      maintenanceExemplarCandidate = projection.maintenanceExemplarCandidate,
      reasonFamily =
        truthContractsByPly.get(moment.ply).map(_.reasonFamily).getOrElse(DecisiveReasonFamily.QuietTechnicalMove),
      failureMode =
        truthContractsByPly.get(moment.ply).map(_.failureMode).getOrElse(FailureInterpretationMode.NoClearPlan),
      benchmarkCriticalMove = truthContractsByPly.get(moment.ply).exists(_.benchmarkCriticalMove)
    )

  private def selectStrategicFallbackMoments(
      moments: List[GameChronicleMoment],
      threadedPlies: Set[Int],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    moments
      .filterNot(moment =>
        threadedPlies.contains(moment.ply) ||
          moment.selectionKind == "thread_bridge" ||
          moment.momentType == "OpeningIntro"
      )
      .flatMap(moment => strategicFallbackScore(moment, truthContractsByPly).map(score => score -> moment))
      .sortBy { case ((priority, secondary, tiebreak), moment) =>
        (priority, secondary, tiebreak, moment.ply)
      }
      .map(_._2)

  private def selectCompensationPriorityMoments(
      moments: List[GameChronicleMoment],
      threadedPlies: Set[Int],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    moments
      .filterNot(moment =>
        threadedPlies.contains(moment.ply) ||
          moment.selectionKind == "thread_bridge" ||
          moment.momentType == "OpeningIntro"
      )
      .flatMap(moment => compensationPriorityScore(moment, truthContractsByPly).map(score => score -> moment))
      .sortBy { case ((priority, evidence, salience, tiebreak), moment) =>
        (priority, evidence, salience, tiebreak, moment.ply)
      }
      .map(_._2)

  private def strategicFallbackScore(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val semantics = MomentTruthSemantics.chronicle(moment, truthContractsByPly.get(moment.ply))
    val hasStrategicCarrier =
      strategicCarrierPresent(
        moment,
        surface,
        truthSelectionView(moment, truthContractsByPly),
        semantics
      )

    Option.when(hasStrategicCarrier) {
      val priority =
        if surface.strictCompensationPosition && surface.quietCompensationPosition then 0
        else if surface.strictCompensationPosition && surface.durableCompensationPosition then 1
        else if surface.strictCompensationPosition then 2
        else if surface.dominantIdeaText.nonEmpty || surface.executionText.nonEmpty then 3
        else if surface.focusText.nonEmpty then 4
        else 5
      val secondary =
        if moment.selectionKind == "key" then 0
        else if moment.selectionKind == "opening" then 1
        else 2
      val tiebreak =
        moment.strategicSalience match
          case Some(level) if level.equalsIgnoreCase("High")   => 0
          case Some(level) if level.equalsIgnoreCase("Medium") => 1
          case _                                               => 2
      (priority, secondary, tiebreak)
    }

  private def selectActiveNoteFallbackMoments(
      moments: List[GameChronicleMoment],
      visibleThreadPlies: Set[Int],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    moments
      .filterNot(moment => visibleThreadPlies.contains(moment.ply))
      .filterNot(moment => moment.selectionKind == "thread_bridge" || moment.momentType == "OpeningIntro")
      .flatMap(moment => activeNoteFallbackScore(moment, truthContractsByPly).map(score => score -> moment))
      .sortBy { case ((priority, secondary, salience, tiebreak), moment) =>
        (priority, secondary, salience, tiebreak, moment.ply)
      }
      .map(_._2)

  private def compensationPriorityScore(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val truthView = truthSelectionView(moment, truthContractsByPly)
    val semantics = MomentTruthSemantics.chronicle(moment, truthContractsByPly.get(moment.ply))
    val visibleInvestment =
      truthView.visibilityRole == TruthVisibilityRole.PrimaryVisible ||
        truthView.visibilityRole == TruthVisibilityRole.SupportingVisible
    val exemplarTruth = isExemplarPivot(truthView)
    val compensationEligible =
      if semantics.hasTruthContract then semantics.compensationSelectionEligible
      else surface.strictCompensationPosition || visibleInvestment || exemplarTruth
    Option.when(compensationEligible) {
      val priority =
        if isVerifiedExemplar(truthView) then 0
        else if isPrimaryConversionOwner(truthView, moment) then 1
        else if isSelectionExemplar(truthView) then 2
        else if isMaintenanceEcho(truthView) then 3
        else if !semantics.hasTruthContract && surface.quietCompensationPosition then 4
        else if !semantics.hasTruthContract && surface.durableCompensationPosition then 5
        else 6
      val evidence =
        if isVerifiedExemplar(truthView) then 0
        else if isSelectionExemplar(truthView) then 1
        else if isMaintenanceEcho(truthView) then 2
        else if moment.probeRefinementRequests.nonEmpty then 0
        else if !semantics.hasTruthContract && moment.signalDigest.exists(_.decisionComparison.isDefined) then 3
        else if moment.selectionKind == "key" then 4
        else 5
      val salience =
        moment.strategicSalience match
          case Some(level) if level.equalsIgnoreCase("High")   => 0
          case Some(level) if level.equalsIgnoreCase("Medium") => 1
          case _                                               => 2
      val tiebreak = if moment.selectionKind == "key" then 0 else 1
      (priority, evidence, salience, tiebreak)
    }

  private def activeNoteFallbackScore(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val truthView = truthSelectionView(moment, truthContractsByPly)
    val semantics = MomentTruthSemantics.chronicle(moment, truthContractsByPly.get(moment.ply))
    Option.when(strategicCarrierPresent(moment, surface, truthView, semantics)) {
      val priority =
        if isSevereFailure(truthView) then 0
        else if isPromotedBestHold(truthView) then 1
        else if isVerifiedExemplar(truthView) then 2
        else if isPrimaryConversionOwner(truthView, moment) then 3
        else if isSelectionExemplar(truthView) then 4
        else if isMaintenanceEcho(truthView) then 5
        else if !semantics.hasTruthContract && surface.strictCompensationPosition && surface.quietCompensationPosition then 6
        else if !semantics.hasTruthContract && surface.strictCompensationPosition && surface.durableCompensationPosition then 7
        else if !semantics.hasTruthContract && surface.strictCompensationPosition then 8
        else if moment.selectionKind == "key" && surface.dominantIdeaText.nonEmpty then 9
        else if moment.selectionKind == "key" then 10
        else 11
      val secondary =
        if isSevereFailure(truthView) then 0
        else if isPromotedBestHold(truthView) then 1
        else if isVerifiedExemplar(truthView) then 2
        else if isSelectionExemplar(truthView) then 3
        else if isMaintenanceEcho(truthView) then 4
        else if !semantics.hasTruthContract && surface.strictCompensationPosition && surface.quietCompensationPosition then 0
        else if !semantics.hasTruthContract && moment.signalDigest.exists(_.compensation.exists(_.trim.nonEmpty)) then 5
        else if surface.executionText.nonEmpty || surface.objectiveText.nonEmpty then 5
        else 6
      val salience =
        moment.strategicSalience match
          case Some(level) if level.equalsIgnoreCase("High")   => 0
          case Some(level) if level.equalsIgnoreCase("Medium") => 1
          case _                                               => 2
      val tiebreak =
        if moment.strategicThread.isDefined then 0
        else if moment.selectionKind == "opening" then 1
        else 2
      (priority, secondary, salience, tiebreak)
    }

  private def suppressInvestmentEchoes(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    moments.foldLeft(
      (
        List.empty[GameChronicleMoment],
        Set.empty[String],
        Map.empty[String, Int]
      )
    ) { case ((accepted, primarySeen, supportSeen), moment) =>
      val truthView = truthSelectionView(moment, truthContractsByPly)
      truthView.chainKey match
        case Some(chainKey) if chainKey.nonEmpty =>
          truthView.visibilityRole match
            case TruthVisibilityRole.PrimaryVisible =>
              if primarySeen.contains(chainKey) then (accepted, primarySeen, supportSeen)
              else (accepted :+ moment, primarySeen + chainKey, supportSeen)
            case TruthVisibilityRole.SupportingVisible =>
              val supportCount = supportSeen.getOrElse(chainKey, 0)
              val maxSupports =
                if qualifiesForSecondSupport(truthView) then 2
                else 1
              if supportCount >= maxSupports then (accepted, primarySeen, supportSeen)
              else (accepted :+ moment, primarySeen, supportSeen.updated(chainKey, supportCount + 1))
            case TruthVisibilityRole.Hidden =>
              (accepted :+ moment, primarySeen, supportSeen)
        case _ =>
          (accepted :+ moment, primarySeen, supportSeen)
    }._1

  private def strategicCarrierPresent(
      moment: GameChronicleMoment,
      surface: StrategyPackSurface.Snapshot,
      truthView: TruthSelectionView,
      semantics: MomentTruthSemantics.ChronicleSemantics
  ): Boolean =
    val truthBackedStrategicVisibility =
      truthView.visibilityRole != TruthVisibilityRole.Hidden &&
        truthView.surfaceMode != TruthSurfaceMode.FailureExplain &&
        (
          truthView.ownershipRole != TruthOwnershipRole.NoneRole ||
            truthView.surfaceMode != TruthSurfaceMode.Neutral ||
            truthView.exemplarRole != TruthExemplarRole.NonExemplar
        )
    val truthCriticalSupport =
      isPromotedBestHold(truthView) ||
        truthView.failureMode != FailureInterpretationMode.NoClearPlan
    if semantics.hasTruthContract then
      truthBackedStrategicVisibility ||
        semantics.truthBackedStrategicVisibility ||
        isExemplarPivot(truthView) ||
        truthCriticalSupport
    else
      truthBackedStrategicVisibility ||
        isExemplarPivot(truthView) ||
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

  private def threadScore(thread: ActiveStrategicThreadBuilder.BuiltThread): Double =
    val moments = thread.moments
    val routeRichness = moments.count(_.moment.strategyPack.exists(_.pieceRoutes.nonEmpty)) * 0.45
    val comparisonRichness = moments.count(_.moment.signalDigest.flatMap(_.decisionComparison).isDefined) * 0.40
    val salience = moments.count(_.moment.strategicSalience.exists(_.equalsIgnoreCase("High"))) * 0.35
    val opponentVisibility =
      moments.count(m =>
        m.moment.signalDigest.flatMap(_.opponentPlan).exists(_.trim.nonEmpty) ||
          m.moment.signalDigest.flatMap(_.prophylaxisThreat).exists(_.trim.nonEmpty)
      ) * 0.30
    val specificity = moments.headOption.map(_.theme.specificityScore * 2.4).getOrElse(0.0)
    thread.thread.continuityScore * 3.2 + specificity + routeRichness + comparisonRichness + salience + opponentVisibility

  private def sortThreads(
      threads: List[ActiveStrategicThreadBuilder.BuiltThread]
  ): List[ActiveStrategicThreadBuilder.BuiltThread] =
    threads.sortBy(thread => (-threadScore(thread), thread.thread.seedPly))

  private def selectRepresentatives(
      thread: ActiveStrategicThreadBuilder.BuiltThread,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    val seed = thread.moments.find(_.stage == ActiveStrategicThreadBuilder.ThreadStage.Seed).map(_.moment)
    val build =
      thread.moments.find(_.stage == ActiveStrategicThreadBuilder.ThreadStage.Build).map(_.moment)
        .orElse(thread.moments.drop(1).headOption.map(_.moment))
    val finisher =
      thread.moments.reverse.find(m =>
        m.stage == ActiveStrategicThreadBuilder.ThreadStage.Switch ||
          m.stage == ActiveStrategicThreadBuilder.ThreadStage.Convert
      ).map(_.moment)
        .orElse(thread.moments.lastOption.map(_.moment))
    val stagePriorityByPly =
      List(seed.map(_.ply -> 0), build.map(_.ply -> 1), finisher.map(_.ply -> 2)).flatten.toMap
    val threadMoments = thread.moments.map(_.moment).distinctBy(_.ply)
    val rankedStrong =
      threadMoments
        .filter(moment => isStrongRepresentativeQualified(truthSelectionView(moment, truthContractsByPly)))
        .sortBy(moment =>
          representativeMomentScore(moment, truthContractsByPly, Set.empty, stagePriorityByPly)
        )
    val rankedSecondary =
      threadMoments
        .filter(moment => isSecondaryRepresentativeQualified(truthSelectionView(moment, truthContractsByPly)))
        .sortBy(moment =>
          representativeMomentScore(moment, truthContractsByPly, Set.empty, stagePriorityByPly)
        )
    val baseRepresentatives =
      selectStageAwareRepresentatives(
        stageMoments = List(seed, build, finisher),
        rankedStrong = rankedStrong,
        rankedSecondary = rankedSecondary,
        truthContractsByPly = truthContractsByPly,
        stagePriorityByPly = stagePriorityByPly
      )
    val baseHasStrongOrFailureRepresentative =
      baseRepresentatives.exists { moment =>
        val truthView = truthSelectionView(moment, truthContractsByPly)
        isStrongRepresentativeQualified(truthView) || isFailureSignificantThreadLocal(truthView)
      }
    val supplementalRepresentatives =
      (
        rankedStrong ++
          rankedSecondary.filter { moment =>
            !isReplacementOnlyThreadLocal(truthSelectionView(moment, truthContractsByPly)) ||
              !baseHasStrongOrFailureRepresentative
          }
      )
        .filterNot(moment => baseRepresentatives.exists(_.ply == moment.ply))
        .sortBy(moment =>
          representativeMomentScore(moment, truthContractsByPly, baseRepresentatives.map(_.ply).toSet, stagePriorityByPly)
        )
        .take((3 - baseRepresentatives.size).max(0))
    (baseRepresentatives ++ supplementalRepresentatives).distinctBy(_.ply).take(3).sortBy(_.ply)

  private def selectStageAwareRepresentatives(
      stageMoments: List[Option[GameChronicleMoment]],
      rankedStrong: List[GameChronicleMoment],
      rankedSecondary: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract],
      stagePriorityByPly: Map[Int, Int]
  ): List[GameChronicleMoment] =
    stageMoments.foldLeft((List.empty[GameChronicleMoment], Set.empty[Int])) {
      case ((selected, usedPlies), stageMomentOpt) =>
        val stageTruthView = stageMomentOpt.map(moment => truthSelectionView(moment, truthContractsByPly))
        val selectedHasStrongOrFailureRepresentative =
          selected.exists { moment =>
            val truthView = truthSelectionView(moment, truthContractsByPly)
            isStrongRepresentativeQualified(truthView) || isFailureSignificantThreadLocal(truthView)
          }
        def scoreCandidate(moment: GameChronicleMoment): (Int, Int, Int, Int, Int) =
          val (priority, _, stagePenalty, tiebreak) =
            representativeMomentScore(moment, truthContractsByPly, Set.empty, stagePriorityByPly)
          val distancePenalty = stageMomentOpt.map(stage => math.abs(stage.ply - moment.ply)).getOrElse(0)
          (priority, distancePenalty, stagePenalty, tiebreak, moment.ply)
        def pickUnused(
            candidates: List[GameChronicleMoment],
            excludedPly: Option[Int] = None
        ): Option[GameChronicleMoment] =
          candidates
            .filterNot(moment => usedPlies.contains(moment.ply) || excludedPly.contains(moment.ply))
            .sortBy(scoreCandidate)
            .headOption
        def pickSecondary(excludedPly: Option[Int] = None): Option[GameChronicleMoment] =
          val reusableFailureSecondary =
            rankedSecondary
              .filter(moment => !isReplacementOnlyThreadLocal(truthSelectionView(moment, truthContractsByPly)))
          val replacementOnlySecondary =
            rankedSecondary
              .filter(moment => isReplacementOnlyThreadLocal(truthSelectionView(moment, truthContractsByPly)))
          pickUnused(reusableFailureSecondary, excludedPly)
            .orElse(
              Option.when(!selectedHasStrongOrFailureRepresentative) {
                pickUnused(replacementOnlySecondary, excludedPly)
              }.flatten
            )
        val chosen =
          stageMomentOpt match
            case Some(moment)
                if !usedPlies.contains(moment.ply) &&
                  isStrongRepresentativeQualified(truthSelectionView(moment, truthContractsByPly)) =>
              Some(moment)
            case Some(moment)
                if !usedPlies.contains(moment.ply) &&
                  isFailureSignificantThreadLocal(truthSelectionView(moment, truthContractsByPly)) =>
              Some(moment)
            case Some(moment) =>
              pickUnused(rankedStrong)
                .orElse(pickSecondary(stageTruthView.collect {
                  case truthView if isReplacementOnlyThreadLocal(truthView) => moment.ply
                }))
                .orElse(
                  Option.when(
                    !usedPlies.contains(moment.ply) &&
                      !stageTruthView.exists(isReplacementOnlyThreadLocal)
                  )(moment)
                )
            case None =>
              pickUnused(rankedStrong).orElse(pickSecondary())
        chosen match
          case Some(moment) if !usedPlies.contains(moment.ply) => (selected :+ moment, usedPlies + moment.ply)
          case _                                               => (selected, usedPlies)
    }._1

  private def representativeMomentScore(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract],
      baseRepresentativePlies: Set[Int],
      stagePriorityByPly: Map[Int, Int]
  ): (Int, Int, Int, Int) =
    val truthView = truthSelectionView(moment, truthContractsByPly)
    val priority =
      if isSevereFailure(truthView) then 0
      else if isPromotedBestHold(truthView) then 1
      else if isVerifiedExemplar(truthView) then 2
      else if isProvisionalExemplar(truthView) || isPrimaryConversionOwner(truthView, moment) then 3
      else if isFailureSignificantThreadLocal(truthView) then 4
      else if isCriticalBestTacticalOrTechnical(truthView) then 5
      else if isSelectionExemplar(truthView) then 6
      else if isMaintenanceEcho(truthView) then 7
      else if truthView.visibilityRole != TruthVisibilityRole.Hidden then 8
      else 9
    val basePenalty = if baseRepresentativePlies.contains(moment.ply) then 0 else 1
    val stagePenalty = stagePriorityByPly.getOrElse(moment.ply, 3)
    (priority, basePenalty, stagePenalty, moment.ply)

  private def qualifiesForSecondSupport(truthView: TruthSelectionView): Boolean =
    truthView.maintenanceExemplarCandidate ||
      truthView.classificationKey != "best" ||
      truthView.failureMode != FailureInterpretationMode.NoClearPlan

  private def isStrongRepresentativeQualified(truthView: TruthSelectionView): Boolean =
    truthView.visibilityRole != TruthVisibilityRole.Hidden ||
      truthView.maintenanceExemplarCandidate ||
      truthView.exemplarRole != TruthExemplarRole.NonExemplar ||
      truthView.classificationKey == "winninginvestment" ||
      truthView.classificationKey == "compensatedinvestment"

  private def isSecondaryRepresentativeQualified(truthView: TruthSelectionView): Boolean =
    isCriticalBestTacticalOrTechnical(truthView) ||
      isFailureSignificantThreadLocal(truthView)

  private def selectProtectedVisibleMoments(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    moments
      .flatMap(moment => protectedMomentPriority(moment, truthContractsByPly).map(priority => priority -> moment))
      .sortBy { case ((priority, severity, secondary, tiebreak), moment) =>
        (priority, severity, secondary, tiebreak, moment.ply)
      }
      .map(_._2)
      .distinctBy(_.ply)
      .take(MaxVisibleMoments)

  private def selectProtectedActiveNoteMoments(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    moments
      .flatMap(moment => protectedMomentPriority(moment, truthContractsByPly).map(priority => priority -> moment))
      .sortBy { case ((priority, severity, secondary, tiebreak), moment) =>
        (priority, severity, secondary, tiebreak, moment.ply)
      }
      .map(_._2)
      .distinctBy(_.ply)
      .take(MaxActiveNotes)

  private def protectedMomentPriority(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int, Int, Int)] =
    val truthView = truthSelectionView(moment, truthContractsByPly)
    if isBlunder(truthView) then Some((0, protectedSeverityScore(moment), 0, moment.ply))
    else if isMissedWin(truthView) then Some((1, protectedSeverityScore(moment), 0, moment.ply))
    else if isPromotedBestHold(truthView) then Some((2, protectedSeverityScore(moment), 1, moment.ply))
    else None

  private def protectedSeverityScore(
      moment: GameChronicleMoment
  ): Int =
    -math.abs(moment.cpAfter - moment.cpBefore)
