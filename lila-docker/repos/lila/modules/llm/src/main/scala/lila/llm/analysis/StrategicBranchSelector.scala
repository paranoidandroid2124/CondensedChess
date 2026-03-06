package lila.llm.analysis

import lila.llm.GameNarrativeMoment

object StrategicBranchSelector:

  private val CandidateMomentTypes = Set(
    "OpeningBranchPoint",
    "OpeningOutOfBook",
    "OpeningNovelty",
    "OpeningTheoryEnds",
    "TensionPeak",
    "Equalization",
    "SustainedPressure",
    "AdvantageSwing",
    "MatePivot"
  )

  private val CandidateTransitionTypes = Set(
    "ForcedPivot",
    "NaturalShift",
    "Opportunistic"
  )

  private val MomentTypeWeight = Map(
    "OpeningBranchPoint" -> 3.7,
    "OpeningOutOfBook" -> 3.3,
    "OpeningNovelty" -> 3.2,
    "OpeningTheoryEnds" -> 2.9,
    "TensionPeak" -> 2.8,
    "Equalization" -> 2.3,
    "SustainedPressure" -> 2.5,
    "AdvantageSwing" -> 3.8,
    "MatePivot" -> 4.2
  )

  private val TransitionWeight = Map(
    "ForcedPivot" -> 2.5,
    "NaturalShift" -> 1.5,
    "Opportunistic" -> 1.8
  )

  private val MaxSelected = 8

  private case class ScoredMoment(moment: GameNarrativeMoment, score: Double)

  def select(moments: List[GameNarrativeMoment]): List[GameNarrativeMoment] =
    moments
      .flatMap(scoreCandidate)
      .sortBy(sm => (-sm.score, sm.moment.ply))
      .take(MaxSelected)
      .map(_.moment)

  private def scoreCandidate(moment: GameNarrativeMoment): Option[ScoredMoment] =
    val momentWeight = MomentTypeWeight.getOrElse(moment.momentType, 0.0)
    val transitionWeight = moment.transitionType.flatMap(TransitionWeight.get).getOrElse(0.0)
    val isCandidate =
      CandidateMomentTypes.contains(moment.momentType) ||
        moment.transitionType.exists(CandidateTransitionTypes.contains)
    if !isCandidate then None
    else
      val strategyRichness = strategyRichnessScore(moment)
      val wpaBoost = moment.wpaSwing.map(v => math.min(math.abs(v), 35.0) * 0.04).getOrElse(0.0)
      Some(ScoredMoment(moment, momentWeight + transitionWeight + strategyRichness + wpaBoost))

  private def strategyRichnessScore(moment: GameNarrativeMoment): Double =
    moment.strategyPack match
      case None => 0.0
      case Some(pack) =>
        val planSignal = if pack.plans.nonEmpty then 1.2 else 0.0
        val routeSignal = if pack.pieceRoutes.nonEmpty then 1.3 else 0.0
        val focusSignal = if pack.longTermFocus.nonEmpty then 1.0 else 0.0
        val volume =
          (pack.plans.size.min(2) * 0.25) +
            (pack.pieceRoutes.size.min(2) * 0.3) +
            (pack.longTermFocus.size.min(2) * 0.2)
        planSignal + routeSignal + focusSignal + volume
