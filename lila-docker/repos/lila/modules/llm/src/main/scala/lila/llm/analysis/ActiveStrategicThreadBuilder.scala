package lila.llm.analysis

import lila.llm.{ ActiveStrategicThread, ActiveStrategicThreadRef, GameNarrativeMoment }
import ActiveThemeSurfaceBuilder.ThemeSurface

object ActiveStrategicThreadBuilder:

  private val MaxGapPlies = 24

  enum ThreadStage(val key: String, val label: String):
    case Seed extends ThreadStage("seed", "Seed")
    case Build extends ThreadStage("build", "Build")
    case Switch extends ThreadStage("switch", "Switch")
    case Convert extends ThreadStage("convert", "Convert")

  final case class ThreadMoment(
      moment: GameNarrativeMoment,
      theme: ThemeSurface,
      stage: ThreadStage
  )

  final case class BuiltThread(
      thread: ActiveStrategicThread,
      moments: List[ThreadMoment]
  )

  final case class Result(
      threads: List[ActiveStrategicThread],
      builtThreads: List[BuiltThread],
      threadRefsByPly: Map[Int, ActiveStrategicThreadRef]
  )

  private case class Candidate(moment: GameNarrativeMoment, theme: ThemeSurface)
  private case class RawThread(id: Int, candidates: Vector[Candidate]):
    def side: String = candidates.head.moment.side
    def themeKey: String = candidates.head.theme.themeKey
    def last: Candidate = candidates.last
    def append(candidate: Candidate): RawThread = copy(candidates = candidates :+ candidate)

  def build(moments: List[GameNarrativeMoment]): Result =
    val candidates =
      moments.sortBy(_.ply).flatMap { moment =>
        ActiveThemeSurfaceBuilder
          .build(moment)
          .filter(ActiveThemeSurfaceBuilder.isCanonical)
          .map(Candidate(moment, _))
      }

    val rawThreads =
      candidates.foldLeft(Vector.empty[RawThread]) { (acc, candidate) =>
        val matchIdx = acc.lastIndexWhere(thread => canAppend(thread, candidate, candidates))
        if matchIdx >= 0 then acc.updated(matchIdx, acc(matchIdx).append(candidate))
        else acc :+ RawThread(acc.size + 1, Vector(candidate))
      }

    val built =
      rawThreads
        .filter(_.candidates.size >= 2)
        .map(buildThread)
        .sortBy(_.thread.seedPly)
        .toList

    val refsByPly =
      built.flatMap { builtThread =>
        builtThread.moments.map { staged =>
          val ref =
            ActiveStrategicThreadRef(
              threadId = builtThread.thread.threadId,
              themeKey = builtThread.thread.themeKey,
              themeLabel = builtThread.thread.themeLabel,
              stageKey = staged.stage.key,
              stageLabel = staged.stage.label
            )
          staged.moment.ply -> ref
        }
      }.toMap

    Result(
      threads = built.map(_.thread),
      builtThreads = built,
      threadRefsByPly = refsByPly
    )

  private def canAppend(
      thread: RawThread,
      candidate: Candidate,
      allCandidates: List[Candidate]
  ): Boolean =
    thread.side == candidate.moment.side &&
      thread.themeKey == candidate.theme.themeKey &&
      candidate.moment.ply - thread.last.moment.ply <= MaxGapPlies &&
      compatibleIdentity(thread.last.theme, candidate.theme) &&
      !structureChanged(thread.last.theme, candidate.theme) &&
      !strongerThemeInterposed(thread.last.moment.ply, candidate.moment.ply, candidate.moment.side, candidate.theme.priority, candidate.theme.themeKey, allCandidates)

  private def compatibleIdentity(left: ThemeSurface, right: ThemeSurface): Boolean =
    sameToken(left.planKey, right.planKey) ||
      sameToken(left.planLabel, right.planLabel) ||
      left.routeFamilies.intersect(right.routeFamilies).nonEmpty ||
      sameToken(left.structuralCue, right.structuralCue)

  private def structureChanged(left: ThemeSurface, right: ThemeSurface): Boolean =
    left.structuralCue.exists(l => right.structuralCue.exists(_ != l))

  private def strongerThemeInterposed(
      fromPly: Int,
      toPly: Int,
      side: String,
      priority: Int,
      themeKey: String,
      allCandidates: List[Candidate]
  ): Boolean =
    allCandidates.exists { other =>
      other.moment.side == side &&
      other.moment.ply > fromPly &&
      other.moment.ply < toPly &&
      other.theme.themeKey != themeKey &&
      other.theme.priority < priority
    }

  private def buildThread(raw: RawThread): BuiltThread =
    val staged = stageThread(raw.candidates.toList)
    val representativePlies = selectRepresentativePlies(staged)
    val opponentCounterplan = staged.flatMap(opponentCounterplanOf).headOption
    val theme = raw.candidates.head.theme
    val continuityScore = continuityScoreFor(staged)
    val thread =
      ActiveStrategicThread(
        threadId = s"thread_${raw.id}",
        side = raw.side,
        themeKey = theme.themeKey,
        themeLabel = theme.themeLabel,
        summary = summaryFor(raw.side, theme.themeKey, theme.planLabel),
        seedPly = staged.head.moment.ply,
        lastPly = staged.last.moment.ply,
        representativePlies = representativePlies,
        opponentCounterplan = opponentCounterplan,
        continuityScore = continuityScore
      )
    BuiltThread(thread = thread, moments = staged)

  private def stageThread(candidates: List[Candidate]): List[ThreadMoment] =
    candidates.zipWithIndex.map { case (candidate, idx) =>
      val previous = if idx > 0 then Some(candidates(idx - 1)) else None
      val stage =
        if idx == 0 then ThreadStage.Seed
        else if isConvertMoment(candidate, idx, candidates.size) then ThreadStage.Convert
        else if isSwitchMoment(previous, candidate) then ThreadStage.Switch
        else ThreadStage.Build
      ThreadMoment(candidate.moment, candidate.theme, stage)
    }

  private def isConvertMoment(candidate: Candidate, idx: Int, total: Int): Boolean =
    val digest = candidate.moment.signalDigest
    val corpus = buildStageCorpus(candidate.moment)
    val explicitRealization = containsAny(corpus, List("simplif", "passer", "endgame", "penetrat", "realiz"))
    explicitRealization ||
      ((idx == total - 1) && Set("opposite_bishops_conversion", "invasion_transition").contains(candidate.theme.themeKey)) ||
      digest.flatMap(_.decisionComparison).exists(_.cpLossVsChosen.exists(_ >= 50) && idx == total - 1)

  private def isSwitchMoment(previous: Option[Candidate], candidate: Candidate): Boolean =
    val corpus = buildStageCorpus(candidate.moment)
    val routeShift =
      previous.exists { prev =>
        prev.theme.routeFamilies.intersect(candidate.theme.routeFamilies).isEmpty &&
        prev.theme.routeFamilies.nonEmpty &&
        candidate.theme.routeFamilies.nonEmpty
      }
    val zoneShift =
      previous.exists(prev => candidate.theme.zones.diff(prev.theme.zones).nonEmpty && candidate.theme.zones.size >= 2)
    candidate.moment.transitionType.exists(t => t == "NaturalShift" || t == "Opportunistic") ||
      containsAny(corpus, List("switch", "transfer", "other flank", "central switch", "reroute")) ||
      routeShift ||
      zoneShift

  private def selectRepresentativePlies(staged: List[ThreadMoment]): List[Int] =
    val seed = staged.find(_.stage == ThreadStage.Seed)
    val build = staged.find(_.stage == ThreadStage.Build).orElse(staged.drop(1).headOption)
    val finisher =
      staged.reverse.find(m => m.stage == ThreadStage.Switch || m.stage == ThreadStage.Convert)
        .orElse(staged.lastOption)
    List(seed, build, finisher).flatten.map(_.moment.ply).distinct.take(3)

  private def continuityScoreFor(staged: List[ThreadMoment]): Double =
    val commitment = staged.flatMap(_.moment.activePlan.flatMap(_.commitmentScore))
    val avgCommitment =
      if commitment.nonEmpty then commitment.sum / commitment.size.toDouble
      else 0.45
    val routeBonus =
      staged.count(_.theme.routeFamilies.nonEmpty).toDouble / staged.size.toDouble
    val spanBonus =
      math.min(0.22, ((staged.last.moment.ply - staged.head.moment.ply).toDouble / MaxGapPlies.toDouble) * 0.22)
    (0.42 + (staged.size.min(4) - 1) * 0.12 + avgCommitment * 0.20 + routeBonus * 0.12 + spanBonus).min(0.99)

  private def opponentCounterplanOf(staged: ThreadMoment): Option[String] =
    staged.moment.signalDigest.flatMap(_.opponentPlan).flatMap(nonEmpty)
      .orElse(staged.moment.signalDigest.flatMap(_.prophylaxisThreat).flatMap(nonEmpty))

  private def summaryFor(
      side: String,
      themeKey: String,
      planLabel: Option[String]
  ): String =
    val sideLabel = side.capitalize
    val base =
      themeKey match
        case "whole_board_play" =>
          s"$sideLabel fixes one sector before switching pressure across the board."
        case "opposite_bishops_conversion" =>
          s"$sideLabel steers toward an opposite-coloured bishops conversion where color-complex penetration matters."
        case "active_passive_exchange" =>
          s"$sideLabel wants exchanges that strip away the opponent's only active resource."
        case "rook_lift_attack" =>
          s"$sideLabel builds the attack by lifting a rook across ranks before the final assault."
        case "minority_attack" =>
          s"$sideLabel keeps tightening a minority attack to create and target a fixed weakness."
        case "outpost_entrenchment" =>
          s"$sideLabel invests tempi in stabilizing an outpost for long-term domination."
        case "rook_pawn_march" =>
          s"$sideLabel gains flank space with the rook-pawn to open files for later pressure."
        case "invasion_transition" =>
          s"$sideLabel uses earlier pressure to cross into invasion or endgame conversion."
        case "prophylactic_restriction" =>
          s"$sideLabel first restricts counterplay so the main plan can proceed safely."
        case "compensation_attack" =>
          s"$sideLabel treats material as secondary and keeps the attack alive through activity and time."
        case _ =>
          s"$sideLabel keeps the same strategic campaign running across multiple moments."
    planLabel.flatMap(nonEmpty) match
      case Some(plan) if !normalize(base).contains(normalize(plan)) => s"$base Core plan: $plan."
      case _                                                        => base

  private def buildStageCorpus(moment: GameNarrativeMoment): String =
    normalize(
      List(
        Some(moment.momentType),
        moment.transitionType,
        moment.activePlan.map(_.themeL1),
        moment.activePlan.flatMap(_.subplanId),
        moment.signalDigest.flatMap(_.strategicFlow),
        moment.signalDigest.flatMap(_.decision),
        moment.signalDigest.flatMap(_.deploymentPurpose),
        moment.signalDigest.flatMap(_.decisionComparison).flatMap(_.deferredReason),
        Some(moment.strategyPack.toList.flatMap(_.longTermFocus).mkString(" ")),
        Some(moment.strategyPack.toList.flatMap(_.pieceRoutes.map(_.purpose)).mkString(" "))
      ).flatten.mkString(" ")
    )

  private def containsAny(corpus: String, terms: List[String]): Boolean =
    terms.exists(term => corpus.contains(normalize(term)))

  private def sameToken(left: Option[String], right: Option[String]): Boolean =
    left.exists(l => right.exists(_ == l))

  private def normalize(text: String): String =
    Option(text)
      .getOrElse("")
      .toLowerCase
      .replaceAll("""[^\p{L}\p{N}]+""", " ")
      .replaceAll("""\s+""", " ")
      .trim

  private def nonEmpty(text: String): Option[String] =
    Option(text).map(_.trim).filter(_.nonEmpty)
