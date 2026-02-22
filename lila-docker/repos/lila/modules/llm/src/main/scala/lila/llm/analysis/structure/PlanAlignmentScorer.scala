package lila.llm.analysis.structure

import chess.Color
import lila.llm.analysis.L3.PawnPlayAnalysis
import lila.llm.model.{ Motif, PlanMatch }
import lila.llm.model.structure.{ AlignmentBand, CenterState, PlanAlignment, StructureId, StructureProfile, StructuralPlaybookEntry }

object PlanAlignmentScorer:

  private val PlanMatchWeight = 55.0
  private val PreconditionWeight = 25.0
  private val AntiPlanPenalty = 20
  private val ConfidenceFloor = 0.72
  private val PrimaryPlanConfidenceFloor = 0.55

  def score(
      structureProfile: StructureProfile,
      playbookEntry: StructuralPlaybookEntry,
      topPlans: List[PlanMatch],
      motifs: List[Motif],
      pawnAnalysis: Option[PawnPlayAnalysis],
      sideToMove: Color
  ): PlanAlignment =
    if structureProfile.primary == StructureId.Unknown then return unknown(topPlans)

    val expected = StructuralPlaybook.expectedPlans(playbookEntry, sideToMove)
    val observed = topPlans.take(3).map(_.plan.id).distinct
    val matched = expected.filter(observed.contains)
    val missing = expected.filterNot(matched.contains)

    val planScore =
      if expected.isEmpty then 0
      else if observed.headOption.exists(expected.contains) then PlanMatchWeight.toInt
      else math.round(PlanMatchWeight * matched.size.toDouble / expected.size.toDouble).toInt

    val preconds = playbookEntry.preconditions
    val precondHits = preconds.count(p => isPreconditionSatisfied(p, structureProfile, pawnAnalysis, motifs))
    val precondScore =
      if preconds.isEmpty then PreconditionWeight.toInt
      else math.round(PreconditionWeight * precondHits.toDouble / preconds.size.toDouble).toInt

    val primaryObserved = observed.headOption
    val topPlanTrusted = topPlans.headOption.exists(_.score >= PrimaryPlanConfidenceFloor)
    val antiPlanByCounter = topPlanTrusted && primaryObserved.exists(id => playbookEntry.counterPlans.contains(id))
    val antiPlanByMiss = topPlanTrusted && primaryObserved.exists(id => !expected.contains(id) && matched.isEmpty)
    val antiPlan = antiPlanByCounter || antiPlanByMiss
    val antiPenalty = if antiPlan then AntiPlanPenalty else 0

    val raw = (planScore + precondScore - antiPenalty).max(0).min(100)
    val lowConf = structureProfile.confidence < ConfidenceFloor
    val band =
      if lowConf then AlignmentBand.Unknown
      else if raw >= 70 then AlignmentBand.OnBook
      else if raw >= 45 then AlignmentBand.Playable
      else AlignmentBand.OffPlan

    val reasons =
      (List.newBuilder[String]
        .addAll(if matched.nonEmpty then List("PA_MATCH") else Nil)
        .addAll(if preconds.nonEmpty && precondHits < preconds.size then List("PRECOND_MISS") else Nil)
        .addAll(if antiPlan then List("ANTI_PLAN") else Nil)
        .addAll(if lowConf then List("LOW_CONF") else Nil)
        .result()).distinct

    val reasonWeights = Map(
      "PA_MATCH" -> (if matched.nonEmpty then planScore.toDouble / 100.0 else 0.0),
      "PRECOND_MISS" -> (if preconds.nonEmpty && precondHits < preconds.size then (preconds.size - precondHits).toDouble / preconds.size.toDouble else 0.0),
      "ANTI_PLAN" -> (if antiPlan then antiPenalty.toDouble / 100.0 else 0.0),
      "LOW_CONF" -> (if lowConf then (ConfidenceFloor - structureProfile.confidence).max(0.0) else 0.0)
    ).filter(_._2 > 0.0)

    PlanAlignment(
      score = raw,
      band = band,
      matchedPlanIds = matched.map(_.toString),
      missingPlanIds = missing.map(_.toString),
      reasonCodes = reasons,
      narrativeIntent = Some(playbookEntry.narrativeIntent),
      narrativeRisk = Some(playbookEntry.narrativeRisk),
      reasonWeights = reasonWeights
    )

  def unknown(topPlans: List[PlanMatch]): PlanAlignment =
    PlanAlignment(
      score = 0,
      band = AlignmentBand.Unknown,
      matchedPlanIds = Nil,
      missingPlanIds = Nil,
      reasonCodes = List("LOW_CONF") ++ topPlans.headOption.map(p => s"TOP_${p.plan.id.toString}").toList,
      narrativeIntent = Some("keep flexibility and verify concrete lines before committing"),
      narrativeRisk = Some("uncertain"),
      reasonWeights = Map("LOW_CONF" -> 1.0)
    )

  private def isPreconditionSatisfied(
      precondition: String,
      structureProfile: StructureProfile,
      pawnAnalysis: Option[PawnPlayAnalysis],
      motifs: List[Motif]
  ): Boolean =
    precondition match
      case "tension_or_break" =>
        pawnAnalysis.exists(p => p.pawnBreakReady || p.advanceOrCapture || p.counterBreak)
      case "minority_ready" =>
        pawnAnalysis.exists(_.minorityAttack)
      case "piece_activity" =>
        motifs.exists {
          case _: Motif.Outpost | _: Motif.Centralization | _: Motif.Maneuver => true
          case _ => false
        }
      case "locked_center" =>
        structureProfile.centerState == CenterState.Locked
      case "open_center" =>
        structureProfile.centerState == CenterState.Open
      case "solid_center" =>
        structureProfile.centerState == CenterState.Locked || structureProfile.centerState == CenterState.Symmetric
      case "counter_break_watch" =>
        pawnAnalysis.exists(_.counterBreak)
      case "king_flank_focus" =>
        pawnAnalysis.exists(_.breakFile.exists(f => f == "f" || f == "g" || f == "h")) ||
          motifs.exists {
            case m: Motif.PawnAdvance => Set('f', 'g', 'h').contains(m.file.char)
            case _ => false
          }
      case _ =>
        true
