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
      chainKey: Option[String]
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
    val visibleThreadMoments =
      rankedThreads
        .flatMap(selectRepresentatives)
        .distinctBy(_.ply)
        .take(MaxVisibleThreadMoments)
    val visibleThreadPlies = visibleThreadMoments.map(_.ply).toSet
    val visibleCompensationMoments =
      suppressInvestmentEchoes(
        selectCompensationPriorityMoments(moments, threadedPlies, truthContractsByPly),
        truthContractsByPly
      )
        .filterNot(moment => visibleThreadPlies.contains(moment.ply))
        .take((MaxVisibleMoments - visibleThreadMoments.size).max(0))
    val visibleCompensationPlies = visibleCompensationMoments.map(_.ply).toSet
    val visibleCoreEvents =
      selectCoreNonThreadEvents(moments, threadedPlies, truthContractsByPly)
        .filterNot(moment => visibleThreadPlies.contains(moment.ply) || visibleCompensationPlies.contains(moment.ply))
        .take((MaxVisibleMoments - visibleThreadMoments.size - visibleCompensationMoments.size).max(0))
    val visibleFallbackMoments =
      Option.when(visibleThreadMoments.isEmpty && visibleCoreEvents.isEmpty && visibleCompensationMoments.isEmpty) {
        selectStrategicFallbackMoments(moments, threadedPlies).take(MaxVisibleMoments)
      }.getOrElse(Nil)
    val visibleMoments =
      (visibleThreadMoments ++ visibleCompensationMoments ++ visibleCoreEvents ++ visibleFallbackMoments)
        .distinctBy(_.ply)
        .sortBy(_.ply)
        .take(MaxVisibleMoments)
    val activeNoteFallbackMoments =
      selectActiveNoteFallbackMoments(moments, visibleThreadPlies, truthContractsByPly).take(
        (MaxActiveNotes - visibleThreadMoments.size).max(0)
      )

    if rankedThreads.isEmpty then
      StrategicBranchSelection(
        selectedMoments = visibleMoments,
        activeNoteMoments = activeNoteFallbackMoments.take(MaxActiveNotes),
        threads = Nil,
        threadRefsByPly = Map.empty
      )
    else
      StrategicBranchSelection(
        selectedMoments = visibleMoments,
        activeNoteMoments =
          (visibleThreadMoments ++ activeNoteFallbackMoments).distinctBy(_.ply).take(MaxActiveNotes),
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
    if isBlunder(moment, truthView) then Some(1 -> moment.ply)
    else if isMissedWin(moment, truthView) then Some(2 -> moment.ply)
    else if isVerifiedExemplar(truthView) then Some(3 -> moment.ply)
    else if isPrimaryConversionOwner(truthView, moment) then Some(4 -> moment.ply)
    else if isProvisionalExemplar(truthView) then Some(5 -> moment.ply)
    else if isMaintenanceEcho(truthView) then Some(6 -> moment.ply)
    else if isMatePivot(moment) then Some(7 -> moment.ply)
    else if isOpeningBranchEvent(moment) then Some(8 -> moment.ply)
    else None

  private def isBlunder(moment: GameChronicleMoment, truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.BlunderOwner &&
      truthView.classificationKey == "blunder"

  private def isMissedWin(moment: GameChronicleMoment, truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.BlunderOwner &&
      truthView.classificationKey == "missedwin"

  private def isMatePivot(moment: GameChronicleMoment): Boolean =
    moment.momentType == "MatePivot"

  private def isExemplarPivot(truthView: TruthSelectionView): Boolean =
    truthView.exemplarRole == TruthExemplarRole.VerifiedExemplar ||
      truthView.exemplarRole == TruthExemplarRole.ProvisionalExemplar

  private def isConversionPivot(
      moment: GameChronicleMoment,
      truthView: TruthSelectionView
  ): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.ConversionOwner ||
      truthView.surfaceMode == TruthSurfaceMode.ConversionExplain ||
      moment.transitionType.exists { transition =>
      val lower = transition.trim.toLowerCase
      (lower.contains("promotion") || lower.contains("exchange") || lower.contains("convert") || lower.contains("simplif")) &&
        !isExemplarPivot(truthView)
      }

  private def isOpeningBranchEvent(moment: GameChronicleMoment): Boolean =
    OpeningEventMomentTypes.contains(moment.momentType)

  private def isVerifiedExemplar(truthView: TruthSelectionView): Boolean =
    truthView.exemplarRole == TruthExemplarRole.VerifiedExemplar &&
      truthView.visibilityRole == TruthVisibilityRole.PrimaryVisible

  private def isProvisionalExemplar(truthView: TruthSelectionView): Boolean =
    truthView.exemplarRole == TruthExemplarRole.ProvisionalExemplar &&
      truthView.visibilityRole != TruthVisibilityRole.Hidden

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
      chainKey = projection.chainKey
    )

  private def selectStrategicFallbackMoments(
      moments: List[GameChronicleMoment],
      threadedPlies: Set[Int]
  ): List[GameChronicleMoment] =
    moments
      .filterNot(moment =>
        threadedPlies.contains(moment.ply) ||
          moment.selectionKind == "thread_bridge" ||
          moment.momentType == "OpeningIntro"
      )
      .flatMap(moment => strategicFallbackScore(moment).map(score => score -> moment))
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

  private def strategicFallbackScore(moment: GameChronicleMoment): Option[(Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val hasStrategicCarrier =
      strategicCarrierPresent(
        moment,
        surface,
        truthSelectionView(moment, Map.empty[Int, DecisiveTruthContract])
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
    val visibleInvestment =
      truthView.visibilityRole == TruthVisibilityRole.PrimaryVisible ||
        truthView.visibilityRole == TruthVisibilityRole.SupportingVisible
    val exemplarTruth = isExemplarPivot(truthView)
    Option.when(surface.strictCompensationPosition || visibleInvestment || exemplarTruth) {
      val priority =
        if isVerifiedExemplar(truthView) then 0
        else if isPrimaryConversionOwner(truthView, moment) then 1
        else if isProvisionalExemplar(truthView) then 2
        else if isMaintenanceEcho(truthView) then 3
        else if surface.quietCompensationPosition then 4
        else if surface.durableCompensationPosition then 5
        else 6
      val evidence =
        if isVerifiedExemplar(truthView) then 0
        else if isProvisionalExemplar(truthView) then 1
        else if isMaintenanceEcho(truthView) then 2
        else if moment.probeRefinementRequests.nonEmpty then 0
        else if moment.signalDigest.exists(_.decisionComparison.isDefined) then 3
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
    Option.when(strategicCarrierPresent(moment, surface, truthView)) {
      val priority =
        if isVerifiedExemplar(truthView) then 0
        else if isPrimaryConversionOwner(truthView, moment) then 1
        else if isProvisionalExemplar(truthView) then 2
        else if isMaintenanceEcho(truthView) then 3
        else if surface.strictCompensationPosition && surface.quietCompensationPosition then 4
        else if surface.strictCompensationPosition && surface.durableCompensationPosition then 5
        else if surface.strictCompensationPosition then 6
        else if moment.selectionKind == "key" && surface.dominantIdeaText.nonEmpty then 7
        else if moment.selectionKind == "key" then 8
        else 9
      val secondary =
        if isVerifiedExemplar(truthView) then 0
        else if isProvisionalExemplar(truthView) then 1
        else if isMaintenanceEcho(truthView) then 2
        else if surface.strictCompensationPosition && surface.quietCompensationPosition then 0
        else if moment.signalDigest.exists(_.compensation.exists(_.trim.nonEmpty)) then 3
        else if surface.executionText.nonEmpty || surface.objectiveText.nonEmpty then 3
        else 4
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
              if supportCount >= 1 then (accepted, primarySeen, supportSeen)
              else (accepted :+ moment, primarySeen, supportSeen.updated(chainKey, supportCount + 1))
            case TruthVisibilityRole.Hidden =>
              (accepted :+ moment, primarySeen, supportSeen)
        case _ =>
          (accepted :+ moment, primarySeen, supportSeen)
    }._1

  private def strategicCarrierPresent(
      moment: GameChronicleMoment,
      surface: StrategyPackSurface.Snapshot,
      truthView: TruthSelectionView
  ): Boolean =
    val truthBackedStrategicVisibility =
      truthView.visibilityRole != TruthVisibilityRole.Hidden &&
        truthView.surfaceMode != TruthSurfaceMode.FailureExplain &&
        (
          truthView.ownershipRole != TruthOwnershipRole.NoneRole ||
            truthView.surfaceMode != TruthSurfaceMode.Neutral ||
            truthView.exemplarRole != TruthExemplarRole.NonExemplar
        )
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

  private def selectRepresentatives(thread: ActiveStrategicThreadBuilder.BuiltThread): List[GameChronicleMoment] =
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
    List(seed, build, finisher).flatten.distinctBy(_.ply).take(3)
