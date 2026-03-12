package lila.llm.analysis

import lila.llm.GameNarrativeMoment
import ActiveStrategicThreadBuilder.{ BuiltThread, ThreadMoment, ThreadStage }

object ActiveBridgeMomentPlanner:

  private val MaxThreads = 3
  private val MaxCandidatePliesPerThread = 14
  private val MaxCandidatePliesTotal = 36
  private val MaxVisibleBridgesPerThread = 3
  private val ContinuityGapThreshold = 10

  final case class PlannedBridge(
      ply: Int,
      threadId: String,
      themeKey: String,
      themeLabel: String,
      stageKey: String,
      stageLabel: String,
      reason: String
  )

  def planCandidatePlies(
      rankedThreads: List[BuiltThread],
      anchorPlies: Set[Int],
      totalPlies: Int
  ): Map[String, List[Int]] =
    val seen = scala.collection.mutable.Set.empty[Int]
    rankedThreads
      .take(MaxThreads)
      .foldLeft(Map.empty[String, List[Int]]) { case (acc, thread) =>
        val remainingBudget = MaxCandidatePliesTotal - seen.size
        if remainingBudget <= 0 then acc
        else
          val selected =
            prioritizeCandidatePlies(thread, anchorPlies, totalPlies)
              .filterNot(seen.contains)
              .take(MaxCandidatePliesPerThread.min(remainingBudget))
          selected.foreach(seen += _)
          acc.updated(thread.thread.threadId, selected)
      }

  def selectBridges(
      rankedThreads: List[BuiltThread],
      enrichedMoments: List[GameNarrativeMoment],
      anchorPlies: Set[Int]
  ): List[PlannedBridge] =
    val enrichedThreads = ActiveStrategicThreadBuilder.build(enrichedMoments).builtThreads
    rankedThreads
      .take(MaxThreads)
      .flatMap { baseThread =>
        matchThread(baseThread, enrichedThreads).toList.flatMap { enrichedThread =>
          val candidates =
            enrichedThread.moments.filterNot(m => anchorPlies.contains(m.moment.ply))
          val existingStages = baseThread.moments.map(_.stage).toSet
          val chosen = scala.collection.mutable.ListBuffer.empty[PlannedBridge]
          val chosenPlies = scala.collection.mutable.Set.empty[Int]

          ThreadStage.values.foreach { stage =>
            if !existingStages.contains(stage) && chosen.size < MaxVisibleBridgesPerThread then
              pickStageBridge(stage, baseThread, candidates, chosenPlies.toSet).foreach { bridge =>
                chosen += bridge
                chosenPlies += bridge.ply
              }
          }

          val allStagesFilled =
            ThreadStage.values.forall(stage =>
              existingStages.contains(stage) || chosen.exists(_.stageKey == stage.key)
            )

          if allStagesFilled && chosen.size < MaxVisibleBridgesPerThread then
            pickContinuityBridge(baseThread, candidates, chosenPlies.toSet).foreach { bridge =>
              chosen += bridge
              chosenPlies += bridge.ply
            }

          chosen.toList.sortBy(_.ply)
        }
      }

  private def prioritizeCandidatePlies(
      thread: BuiltThread,
      anchorPlies: Set[Int],
      totalPlies: Int
  ): List[Int] =
    val lower = (thread.thread.seedPly - 4).max(1)
    val upperBound = if totalPlies > 0 then totalPlies else thread.thread.lastPly + 4
    val upper = (thread.thread.lastPly + 4).min(upperBound)
    val available = (lower to upper).filterNot(anchorPlies.contains).toList
    val existingStages = thread.moments.map(_.stage).toSet
    val stagePriority =
      ThreadStage.values.toList
        .filterNot(existingStages.contains)
        .flatMap(stage => stageCandidates(stage, thread, available))
    val gapPriority = largestGapCandidates(thread, available)
    val boundaryPriority = boundaryCandidates(thread, available)
    val fallback =
      available.sortBy(ply => (math.abs(ply - spanMidpoint(thread)), math.abs(ply - thread.thread.seedPly), ply))
    (stagePriority ++ gapPriority ++ boundaryPriority ++ fallback).distinct

  private def stageCandidates(
      stage: ThreadStage,
      thread: BuiltThread,
      available: List[Int]
  ): List[Int] =
    if available.isEmpty then Nil
    else
      val reps = thread.thread.representativePlies.sorted
      stage match
        case ThreadStage.Seed =>
          val before = available.filter(_ < thread.thread.seedPly).sortBy(ply => (thread.thread.seedPly - ply, ply))
          val after = available.filter(_ >= thread.thread.seedPly).sortBy(ply => (math.abs(ply - thread.thread.seedPly), ply))
          before ++ after
        case ThreadStage.Build =>
          val target = largestGapMidpoint(reps, thread.thread.seedPly, thread.thread.lastPly)
          val inside = available.filter(ply => ply > thread.thread.seedPly && ply < thread.thread.lastPly)
          val outside = available.diff(inside)
          inside.sortBy(ply => (math.abs(ply - target), ply)) ++ outside.sortBy(ply => (math.abs(ply - target), ply))
        case ThreadStage.Switch =>
          val target = reps.drop(1).lastOption.getOrElse(thread.thread.lastPly)
          val late = available.filter(_ >= target).sortBy(ply => (math.abs(ply - target), ply))
          val early = available.filter(_ < target).sortBy(ply => (math.abs(ply - target), ply))
          late ++ early
        case ThreadStage.Convert =>
          val target = thread.thread.lastPly
          val late = available.filter(_ >= target).sortBy(ply => (ply - target, ply))
          val inside = available.filter(ply => ply < target).sortBy(ply => (math.abs(ply - target), -ply))
          late ++ inside

  private def largestGapCandidates(thread: BuiltThread, available: List[Int]): List[Int] =
    val reps = thread.thread.representativePlies.sorted
    if reps.size < 2 then Nil
    else
      val gaps = reps.zip(reps.drop(1)).map { case (left, right) => (left, right, right - left) }
      gaps.sortBy { case (_, _, size) => -size }.headOption.toList.flatMap { case (left, right, _) =>
        val midpoint = left + ((right - left) / 2)
        available
          .filter(ply => ply > left && ply < right)
          .sortBy(ply => (math.abs(ply - midpoint), ply))
      }

  private def boundaryCandidates(thread: BuiltThread, available: List[Int]): List[Int] =
    val start = thread.thread.seedPly
    val end = thread.thread.lastPly
    available.sortBy(ply => (math.min(math.abs(ply - start), math.abs(ply - end)), ply))

  private def matchThread(
      baseThread: BuiltThread,
      enrichedThreads: List[BuiltThread]
  ): Option[BuiltThread] =
    val basePlies = baseThread.moments.map(_.moment.ply).toSet
    enrichedThreads
      .filter(thread =>
        thread.thread.side == baseThread.thread.side &&
          thread.thread.themeKey == baseThread.thread.themeKey
      )
      .sortBy { thread =>
        val overlap = thread.moments.map(_.moment.ply).toSet.intersect(basePlies).size
        (-overlap, math.abs(thread.thread.seedPly - baseThread.thread.seedPly), thread.thread.seedPly)
      }
      .headOption
      .filter(thread => thread.moments.exists(m => basePlies.contains(m.moment.ply)))

  private def pickStageBridge(
      stage: ThreadStage,
      baseThread: BuiltThread,
      candidates: List[ThreadMoment],
      chosenPlies: Set[Int]
  ): Option[PlannedBridge] =
    val filtered = candidates.filter(m => m.stage == stage && !chosenPlies.contains(m.moment.ply))
    val preferred =
      stage match
        case ThreadStage.Seed =>
          filtered.sortBy(m => (if m.moment.ply < baseThread.thread.seedPly then 0 else 1, m.moment.ply))
        case ThreadStage.Build =>
          filtered.sortBy(m => (math.abs(m.moment.ply - spanMidpoint(baseThread)), m.moment.ply))
        case ThreadStage.Switch =>
          filtered.sortBy(m => (m.moment.ply, m.moment.ply))
        case ThreadStage.Convert =>
          filtered.sortBy(m => (-m.moment.ply, m.moment.ply))
    preferred.headOption.map(toPlannedBridge(baseThread, _, stage, s"fills ${stage.label.toLowerCase} stage of ${baseThread.thread.themeLabel}"))

  private def pickContinuityBridge(
      baseThread: BuiltThread,
      candidates: List[ThreadMoment],
      chosenPlies: Set[Int]
  ): Option[PlannedBridge] =
    val reps = baseThread.thread.representativePlies.sorted
    if reps.size < 2 then None
    else
      reps
        .zip(reps.drop(1))
        .map { case (left, right) => (left, right, right - left) }
        .sortBy { case (_, _, gap) => -gap }
        .find { case (_, _, gap) => gap > ContinuityGapThreshold }
        .flatMap { case (left, right, _) =>
          val midpoint = left + ((right - left) / 2)
          candidates
            .filter(m =>
              !chosenPlies.contains(m.moment.ply) &&
                m.moment.ply > left &&
                m.moment.ply < right
            )
            .sortBy(m => (math.abs(m.moment.ply - midpoint), m.moment.ply))
            .headOption
            .map(toPlannedBridge(baseThread, _, ThreadStage.Build, s"bridges long gap in ${baseThread.thread.themeLabel}"))
        }

  private def toPlannedBridge(
      baseThread: BuiltThread,
      moment: ThreadMoment,
      stage: ThreadStage,
      reason: String
  ): PlannedBridge =
    PlannedBridge(
      ply = moment.moment.ply,
      threadId = baseThread.thread.threadId,
      themeKey = moment.theme.themeKey,
      themeLabel = moment.theme.themeLabel,
      stageKey = stage.key,
      stageLabel = stage.label,
      reason = reason
    )

  private def largestGapMidpoint(reps: List[Int], defaultStart: Int, defaultEnd: Int): Int =
    if reps.size < 2 then spanMidpoint(defaultStart, defaultEnd)
    else
      reps
        .zip(reps.drop(1))
        .maxByOption { case (left, right) => right - left }
        .map { case (left, right) => left + ((right - left) / 2) }
        .getOrElse(spanMidpoint(defaultStart, defaultEnd))

  private def spanMidpoint(thread: BuiltThread): Int =
    spanMidpoint(thread.thread.seedPly, thread.thread.lastPly)

  private def spanMidpoint(start: Int, end: Int): Int =
    start + ((end - start) / 2)
