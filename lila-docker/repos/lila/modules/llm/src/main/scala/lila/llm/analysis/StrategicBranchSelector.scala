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

  def select(moments: List[GameChronicleMoment]): List[GameChronicleMoment] =
    buildSelection(moments).selectedMoments

  def rankThreads(moments: List[GameChronicleMoment]): List[ActiveStrategicThreadBuilder.BuiltThread] =
    sortThreads(ActiveStrategicThreadBuilder.build(moments).builtThreads)

  def buildSelection(moments: List[GameChronicleMoment]): StrategicBranchSelection =
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
      selectCompensationPriorityMoments(moments, threadedPlies)
        .filterNot(moment => visibleThreadPlies.contains(moment.ply))
        .take((MaxVisibleMoments - visibleThreadMoments.size).max(0))
    val visibleCompensationPlies = visibleCompensationMoments.map(_.ply).toSet
    val visibleCoreEvents =
      selectCoreNonThreadEvents(moments, threadedPlies)
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
      selectActiveNoteFallbackMoments(moments, visibleThreadPlies).take(
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
      threadedPlies: Set[Int]
  ): List[GameChronicleMoment] =
    moments
      .filterNot(moment =>
        threadedPlies.contains(moment.ply) ||
          moment.selectionKind == "thread_bridge" ||
          moment.momentType == "OpeningIntro"
      )
      .flatMap(moment => coreEventPriority(moment).map(priority => priority -> moment))
      .sortBy { case ((priority, tiebreak), moment) => (priority, tiebreak, moment.ply) }
      .map(_._2)

  private def coreEventPriority(moment: GameChronicleMoment): Option[(Int, Int)] =
    if isBlunder(moment) then Some(1 -> moment.ply)
    else if isMissedWin(moment) then Some(2 -> moment.ply)
    else if isMatePivot(moment) then Some(3 -> moment.ply)
    else if isOpeningBranchEvent(moment) then Some(4 -> moment.ply)
    else None

  private def isBlunder(moment: GameChronicleMoment): Boolean =
    moment.momentType == "AdvantageSwing" && moment.moveClassification.exists(_.equalsIgnoreCase("Blunder"))

  private def isMissedWin(moment: GameChronicleMoment): Boolean =
    moment.momentType == "AdvantageSwing" && moment.moveClassification.exists(_.equalsIgnoreCase("MissedWin"))

  private def isMatePivot(moment: GameChronicleMoment): Boolean =
    moment.momentType == "MatePivot"

  private def isOpeningBranchEvent(moment: GameChronicleMoment): Boolean =
    OpeningEventMomentTypes.contains(moment.momentType)

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
      threadedPlies: Set[Int]
  ): List[GameChronicleMoment] =
    moments
      .filterNot(moment =>
        threadedPlies.contains(moment.ply) ||
          moment.selectionKind == "thread_bridge" ||
          moment.momentType == "OpeningIntro"
      )
      .flatMap(moment => compensationPriorityScore(moment).map(score => score -> moment))
      .sortBy { case ((priority, evidence, salience, tiebreak), moment) =>
        (priority, evidence, salience, tiebreak, moment.ply)
      }
      .map(_._2)

  private def strategicFallbackScore(moment: GameChronicleMoment): Option[(Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val hasStrategicCarrier = strategicCarrierPresent(moment, surface)

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
      visibleThreadPlies: Set[Int]
  ): List[GameChronicleMoment] =
    moments
      .filterNot(moment => visibleThreadPlies.contains(moment.ply))
      .filterNot(moment => moment.selectionKind == "thread_bridge" || moment.momentType == "OpeningIntro")
      .flatMap(moment => activeNoteFallbackScore(moment).map(score => score -> moment))
      .sortBy { case ((priority, secondary, salience, tiebreak), moment) =>
        (priority, secondary, salience, tiebreak, moment.ply)
      }
      .map(_._2)

  private def compensationPriorityScore(moment: GameChronicleMoment): Option[(Int, Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    Option.when(surface.strictCompensationPosition) {
      val priority =
        if surface.quietCompensationPosition then 0
        else if surface.durableCompensationPosition then 1
        else 2
      val evidence =
        if moment.probeRefinementRequests.nonEmpty then 0
        else if moment.signalDigest.exists(_.decisionComparison.isDefined) then 1
        else if moment.selectionKind == "key" then 2
        else 3
      val salience =
        moment.strategicSalience match
          case Some(level) if level.equalsIgnoreCase("High")   => 0
          case Some(level) if level.equalsIgnoreCase("Medium") => 1
          case _                                               => 2
      val tiebreak = if moment.selectionKind == "key" then 0 else 1
      (priority, evidence, salience, tiebreak)
    }

  private def activeNoteFallbackScore(moment: GameChronicleMoment): Option[(Int, Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    Option.when(strategicCarrierPresent(moment, surface)) {
      val priority =
        if surface.strictCompensationPosition && surface.quietCompensationPosition then 0
        else if surface.strictCompensationPosition && surface.durableCompensationPosition then 1
        else if surface.strictCompensationPosition then 2
        else if moment.selectionKind == "key" && surface.dominantIdeaText.nonEmpty then 3
        else if moment.selectionKind == "key" then 4
        else 5
      val secondary =
        if surface.strictCompensationPosition && surface.quietCompensationPosition then 0
        else if moment.signalDigest.exists(_.compensation.exists(_.trim.nonEmpty)) then 1
        else if surface.executionText.nonEmpty || surface.objectiveText.nonEmpty then 1
        else 2
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

  private def strategicCarrierPresent(
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
