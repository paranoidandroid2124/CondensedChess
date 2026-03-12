package lila.llm.analysis

import lila.llm.{ ActiveStrategicThread, ActiveStrategicThreadRef, GameNarrativeMoment }

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
      selectedMoments: List[GameNarrativeMoment],
      activeNoteMoments: List[GameNarrativeMoment],
      threads: List[ActiveStrategicThread],
      threadRefsByPly: Map[Int, ActiveStrategicThreadRef]
  )

  def select(moments: List[GameNarrativeMoment]): List[GameNarrativeMoment] =
    buildSelection(moments).selectedMoments

  def rankThreads(moments: List[GameNarrativeMoment]): List[ActiveStrategicThreadBuilder.BuiltThread] =
    sortThreads(ActiveStrategicThreadBuilder.build(moments).builtThreads)

  def buildSelection(moments: List[GameNarrativeMoment]): StrategicBranchSelection =
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
    val visibleCoreEvents =
      selectCoreNonThreadEvents(moments, threadedPlies)
        .filterNot(moment => visibleThreadPlies.contains(moment.ply))
        .take((MaxVisibleMoments - visibleThreadMoments.size).max(0))
    val visibleMoments =
      (visibleThreadMoments ++ visibleCoreEvents)
        .distinctBy(_.ply)
        .sortBy(_.ply)
        .take(MaxVisibleMoments)

    if rankedThreads.isEmpty then
      StrategicBranchSelection(
        selectedMoments = visibleMoments,
        activeNoteMoments = Nil,
        threads = Nil,
        threadRefsByPly = Map.empty
      )
    else
      StrategicBranchSelection(
        selectedMoments = visibleMoments,
        activeNoteMoments = visibleThreadMoments.take(MaxActiveNotes),
        threads = rankedThreads.map(_.thread),
        threadRefsByPly = threadRefsByPly
      )

  private def selectCoreNonThreadEvents(
      moments: List[GameNarrativeMoment],
      threadedPlies: Set[Int]
  ): List[GameNarrativeMoment] =
    moments
      .filterNot(moment =>
        threadedPlies.contains(moment.ply) ||
          moment.selectionKind == "thread_bridge" ||
          moment.momentType == "OpeningIntro"
      )
      .flatMap(moment => coreEventPriority(moment).map(priority => priority -> moment))
      .sortBy { case ((priority, tiebreak), moment) => (priority, tiebreak, moment.ply) }
      .map(_._2)

  private def coreEventPriority(moment: GameNarrativeMoment): Option[(Int, Int)] =
    if isBlunder(moment) then Some(1 -> moment.ply)
    else if isMissedWin(moment) then Some(2 -> moment.ply)
    else if isMatePivot(moment) then Some(3 -> moment.ply)
    else if isOpeningBranchEvent(moment) then Some(4 -> moment.ply)
    else None

  private def isBlunder(moment: GameNarrativeMoment): Boolean =
    moment.momentType == "AdvantageSwing" && moment.moveClassification.exists(_.equalsIgnoreCase("Blunder"))

  private def isMissedWin(moment: GameNarrativeMoment): Boolean =
    moment.momentType == "AdvantageSwing" && moment.moveClassification.exists(_.equalsIgnoreCase("MissedWin"))

  private def isMatePivot(moment: GameNarrativeMoment): Boolean =
    moment.momentType == "MatePivot"

  private def isOpeningBranchEvent(moment: GameNarrativeMoment): Boolean =
    OpeningEventMomentTypes.contains(moment.momentType)

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

  private def selectRepresentatives(thread: ActiveStrategicThreadBuilder.BuiltThread): List[GameNarrativeMoment] =
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
