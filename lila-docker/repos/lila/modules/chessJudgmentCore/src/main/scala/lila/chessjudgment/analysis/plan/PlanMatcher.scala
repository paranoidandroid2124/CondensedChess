package lila.chessjudgment.analysis.plan

import chess.*
import chess.Color.White
import lila.chessjudgment.analysis.evaluation.JudgmentThresholds
import lila.chessjudgment.analysis.singlePosition.{ GamePhaseType, PawnPlayAnalysis, SinglePositionAssessment, ThreatAnalysis, TensionPolicy }
import lila.chessjudgment.model.*
import lila.chessjudgment.model.Motif.*
import lila.chessjudgment.model.structure.{ PlanAlignment, StructureId, StructureProfile }
import lila.chessjudgment.model.strategic.PlanTaxonomy.{ PlanKind, PlanTheme }
import lila.chessjudgment.analysis.position.{ PositionAnalyzer, PositionFeatures, StrategicStateFeatures }

private val ThemeDiversityPenalty: Boolean =
  sys.env.get("AI_THEME_DIVERSITY_PENALTY")
    .map(_.trim.toLowerCase)
    .exists(v => v == "1" || v == "true" || v == "yes" || v == "on")

case class PlanInteractionContext(
    evalCp: Int,
    positionAssessment: Option[SinglePositionAssessment] = None,
    pawnAnalysis: Option[PawnPlayAnalysis] = None,
    opponentPawnAnalysis: Option[PawnPlayAnalysis] = None,
    threatsToUs: Option[ThreatAnalysis] = None,
    threatsToThem: Option[ThreatAnalysis] = None,
    openingName: Option[String] = None,
    isWhiteToMove: Boolean,
    positionKey: Option[String] = None,
    features: Option[PositionFeatures] = None,
    initialPos: Option[Position] = None,
    structureProfile: Option[StructureProfile] = None,
    planAlignment: Option[PlanAlignment] = None
):
  def evalFor(color: Color): Int = if color == White then evalCp else -evalCp
  def phase: String = phaseEnum match
    case GamePhaseType.Opening    => "opening"
    case GamePhaseType.Middlegame => "middlegame"
    case GamePhaseType.Endgame    => "endgame"
  def phaseEnum: GamePhaseType =
    positionAssessment.map(_.gamePhase.phaseType).getOrElse(GamePhaseType.Middlegame)
  def tacticalThreatToUs: Boolean =
    threatsToUs.exists(_.threats.exists(t => t.turnsToImpact <= 2 && t.lossIfIgnoredCp >= 200))
  def strategicThreatToUs: Boolean =
    threatsToUs.exists(_.threats.exists(t =>
      t.turnsToImpact <= 5 && t.lossIfIgnoredCp >= JudgmentThresholds.SIGNIFICANT_THREAT_CP && !tacticalThreatToUs
    ))
  def tacticalThreatToThem: Boolean =
    threatsToThem.exists(_.threats.exists(t => t.turnsToImpact <= 2 && t.lossIfIgnoredCp >= 200))
  def strategicThreatToThem: Boolean =
    threatsToThem.exists(_.threats.exists(t =>
      t.turnsToImpact <= 5 && t.lossIfIgnoredCp >= JudgmentThresholds.SIGNIFICANT_THREAT_CP && !tacticalThreatToThem
    ))
  def maxThreatLossToUs: Int =
    threatsToUs.map(_.threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)).getOrElse(0)
  def maxThreatLossToThem: Int =
    threatsToThem.map(_.threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)).getOrElse(0)
  def underDefensivePressure: Boolean = tacticalThreatToUs || strategicThreatToUs
  def holdingAttackingThreats: Boolean = tacticalThreatToThem || strategicThreatToThem
  def simplificationReliefPossible: Boolean = underDefensivePressure
  def attackingOpportunityAtRisk: Boolean = holdingAttackingThreats

object PlanMatcher:
  object Theme:
    val Opening = PlanTheme.OpeningPrinciples.id
    val Restriction = PlanTheme.RestrictionProphylaxis.id
    val Redeployment = PlanTheme.PieceRedeployment.id
    val SpaceClamp = PlanTheme.SpaceClamp.id
    val WeaknessFixation = PlanTheme.WeaknessFixation.id
    val PawnBreakPreparation = PlanTheme.PawnBreakPreparation.id
    val FavorableExchange = PlanTheme.FavorableExchange.id
    val FlankInfrastructure = PlanTheme.FlankInfrastructure.id
    val AdvantageTransformation = PlanTheme.AdvantageTransformation.id
    val ImmediateTacticalGain = PlanTheme.ImmediateTacticalGain.id

  object Subplan:
    val OpeningDevelopment = PlanKind.OpeningDevelopment.id
    val Restriction = PlanKind.ProphylaxisRestraint.id
    val Redeployment = PlanKind.WorstPieceImprovement.id
    val OutpostEntrenchment = PlanKind.OutpostEntrenchment.id
    val RookFileTransfer = PlanKind.RookFileTransfer.id
    val SpaceClamp = PlanKind.FlankClamp.id
    val WeaknessFixation = PlanKind.StaticWeaknessFixation.id
    val MinorityAttackFixation = PlanKind.MinorityAttackFixation.id
    val BackwardPawnTargeting = PlanKind.BackwardPawnTargeting.id
    val IQPInducement = PlanKind.IQPInducement.id
    val PawnBreakPreparation = PlanKind.CentralBreakTiming.id
    val WingBreakTiming = PlanKind.WingBreakTiming.id
    val TensionMaintenance = PlanKind.TensionMaintenance.id
    val FavorableExchange = PlanKind.SimplificationWindow.id
    val DefenderTrade = PlanKind.DefenderTrade.id
    val FlankInfrastructure = PlanKind.RookPawnMarch.id
    val HookCreation = PlanKind.HookCreation.id
    val RookLiftScaffold = PlanKind.RookLiftScaffold.id
    val AdvantageTransformation = PlanKind.SimplificationConversion.id
    val ImmediateTacticalGain = PlanKind.ForcingTacticalShot.id
    val DefenderOverload = PlanKind.DefenderOverload.id
    val ClearanceBreak = PlanKind.ClearanceBreak.id

  def triggerKind(themeId: String, subplanId: Option[String]): String =
    PlanKind.fromId(subplanId.getOrElse(""))
      .map(_.id)
      .orElse(PlanTheme.fromId(themeId).map(_.id))
      .getOrElse(PlanTheme.PieceRedeployment.id)

  def proofFamily(themeId: String, subplanId: Option[String]): String =
    triggerKind(themeId, subplanId)

  private case class SideSnapshot(
      lockedCenter: Boolean,
      openCenter: Boolean,
      space: Int,
      devLag: Int,
      lowMobility: Int,
      kingExposure: Int,
      oppWeakness: Int,
      ourPassers: Int,
      oppPassers: Int,
      entrenched: Int,
      rookPawnReady: Boolean,
      hookChance: Boolean,
      clamp: Boolean
  )

  def matchPlans(motifs: List[Motif], ctx: PlanInteractionContext, side: Color): PlanScoringResult =
    val s = snapshot(ctx, side)
    val openingRaw =
      Option.when(ctx.phaseEnum == GamePhaseType.Opening)(
        openingDevelopment(motifs, ctx, side, s)
      ).toList
    val raw =
      openingRaw ++ List(
        restriction(motifs, ctx, side, s),
        redeployment(motifs, ctx, side, s),
        spaceClamp(motifs, side, s),
        weaknessFixation(motifs, ctx, side, s),
        breakPrep(motifs, ctx, side),
        favorableExchange(motifs, ctx, side),
        flankInfrastructure(motifs, ctx, side, s),
        advantageTransformation(motifs, ctx, side, s),
        immediateTacticalGain(motifs, ctx, side)
      )
    val (compatible, events) = applyCompatWithEvents(raw, ctx, side)
    val themePolicyScores = computePlanThemePolicyScores(compatible)
    val annotated = compatible.map(pm => annotateWithPlanThemeScore(pm, themePolicyScores.getOrElse(themeOf(pm), 0.0)))
    val top = annotated.sortBy(p => -p.score).filter(_.score >= 0.18).take(5)
    val plans =
      if top.nonEmpty then top
      else List(themed(Theme.Redeployment, Plan.PieceActivation(side), 0.3, Nil, Some(Subplan.Redeployment)))
    PlanScoringResult(plans, plans.head.score, ctx.phase, events)

  def toActivePlans(sortedPlans: List[PlanMatch], events: List[CompatibilityEvent] = Nil): ActivePlans =
    val primary = sortedPlans.headOption.getOrElse(PlanMatch(Plan.CentralControl(Color.White), 0.0, Nil))
    val secondary = sortedPlans.lift(1)
    val suppressed = sortedPlans.drop(2).filter(_.score < primary.score * 0.5)
    ActivePlans(primary, secondary, suppressed, sortedPlans, events)

  def applyCompatWithEvents(
      plans: List[PlanMatch],
      ctx: PlanInteractionContext,
      side: Color
  ): (List[PlanMatch], List[CompatibilityEvent]) =
    import scala.collection.mutable.ListBuffer
    val events = ListBuffer.empty[CompatibilityEvent]

    def theme(pm: PlanMatch): String = themeOf(pm)

    def adjust(list: List[PlanMatch], t: String, factor: Double, adjustmentId: String): List[PlanMatch] =
      list.map { p =>
        if theme(p) != t then p
        else
          val before = p.score
          val after = clamp(before * factor)
          if math.abs(after - before) > 1e-6 then
            events += CompatibilityEvent(
              originalScore = before,
              finalScore = after,
              delta = after - before,
              adjustmentId = adjustmentId,
              adjustmentType = if after > before then "boost" else "downweight"
            )
          p.copy(score = after)
      }

    var out = plans
    val tactical = out.find(theme(_) == Theme.ImmediateTacticalGain).map(_.score).getOrElse(0.0)
    if tactical >= 0.72 then
      out.map(theme).filter(_ != Theme.ImmediateTacticalGain).distinct.foreach { planTheme =>
        out = adjust(out, planTheme, 0.48, "tactical_override")
      }
    if ctx.underDefensivePressure then
      out = adjust(out, Theme.Restriction, 1.15, "defensive_pressure")
      out = adjust(out, Theme.FlankInfrastructure, 0.74, "defensive_pressure")
      out = adjust(out, Theme.PawnBreakPreparation, 0.82, "defensive_pressure")
    if ctx.positionAssessment.exists(_.simplifyBias.shouldSimplify) && ctx.evalFor(side) >= 80 then
      out = adjust(out, Theme.FavorableExchange, 1.15, "conversion_window")
      out = adjust(out, Theme.AdvantageTransformation, 1.12, "conversion_window")
    if ctx.features.exists(_.centralSpace.openCenter) && kingExposure(ctx.features, side) >= 2 then
      out = adjust(out, Theme.FlankInfrastructure, 0.72, "open_center_flank_risk")
    if ctx.phaseEnum == GamePhaseType.Opening then
      out = adjust(out, Theme.FlankInfrastructure, 0.68, "opening_phase")
      out = adjust(out, Theme.AdvantageTransformation, 0.78, "opening_phase")
    (out, events.toList)

  private def openingDevelopment(m: List[Motif], ctx: PlanInteractionContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.18) {
      case Centralization(piece, _, c, _, _) if c == side && (piece == Knight || piece == Bishop) => true
      case PawnAdvance(file, _, _, c, _, _) if c == side && centralFile(file) => true
      case PawnBreak(file, targetFile, c, _, _) if c == side && (centralFile(file) || centralFile(targetFile)) => true
      case Castling(_, c, _, _) if c == side => true
      case Fianchetto(_, c, _, _) if c == side => true
      case OpenFileControl(file, c, _, _) if c == side && (file == File.D || file == File.E) => true
      case SpaceAdvantage(c, pawnDelta, _, _) if c == side && pawnDelta > 0 => true
      case Maneuver(_, ManeuverPurpose.ImprovingScope, c, _, _) if c == side => true
    }
    val f = ctx.features.getOrElse(PositionFeatures.empty)
    val centerControlDiff =
      if side.white then f.centralSpace.whiteCenterControl - f.centralSpace.blackCenterControl
      else f.centralSpace.blackCenterControl - f.centralSpace.whiteCenterControl
    val ourCenterPawns =
      if side.white then f.centralSpace.whiteCentralPawns else f.centralSpace.blackCentralPawns
    val hasCastled = m.exists { case Castling(_, c, _, _) if c == side => true; case _ => false }
    val developmentNeed = s.devLag.max(0)
    val score =
      0.26 +
        math.min(0.12, developmentNeed * 0.04) +
        math.min(0.08, centerControlDiff.max(0) * 0.015) +
        math.min(0.06, ourCenterPawns.max(0) * 0.03) +
        (if hasCastled then 0.05 else 0.0) +
        math.min(0.18, ev.size * 0.05) -
        (if developmentNeed == 0 && !hasCastled && ev.isEmpty then 0.08 else 0.0) -
        (if ctx.tacticalThreatToUs then 0.12 else 0.0) -
        (if ctx.tacticalThreatToThem then 0.08 else 0.0)
    themed(Theme.Opening, Plan.OpeningDevelopment(side), score, ev, Some(Subplan.OpeningDevelopment))

  private def restriction(m: List[Motif], ctx: PlanInteractionContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.18) {
      case Domination(_, _, _, c, _, _) if c == side => true
      case Blockade(_, _, _, c, _, _) if c == side => true
      case OpenFileControl(_, c, _, _) if c == side => true
    }
    val score =
      0.28 +
        (if ctx.tacticalThreatToUs then 0.22 else 0.0) +
        (if ctx.strategicThreatToUs then 0.12 else 0.0) +
        (if s.clamp then 0.14 else 0.0) +
        (if s.lockedCenter then 0.06 else 0.0) +
        math.min(0.16, ev.size * 0.05) -
        (if ctx.tacticalThreatToThem && !ctx.tacticalThreatToUs then 0.08 else 0.0)
    themed(Theme.Restriction, Plan.Prophylaxis(side), score, ev, Some(Subplan.Restriction))

  private def redeployment(m: List[Motif], ctx: PlanInteractionContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.17) {
      case Outpost(_, _, c, _, _) if c == side => true
      case Centralization(_, _, c, _, _) if c == side => true
      case Maneuver(_, _, c, _, _) if c == side => true
      case RookLift(_, _, _, c, _, _) if c == side => true
    }
    val prefersOutpost = s.entrenched > 0 || m.exists { case Outpost(_, _, c, _, _) if c == side => true; case _ => false }
    val prefersRookFileTransfer =
      !prefersOutpost && m.exists {
        case RookLift(_, _, _, c, _, _) if c == side => true
        case OpenFileControl(_, c, _, _) if c == side => true
        case SemiOpenFileControl(_, c, _, _) if c == side => true
        case _ => false
      }
    val subplanId =
      if prefersOutpost then Subplan.OutpostEntrenchment
      else if prefersRookFileTransfer then Subplan.RookFileTransfer
      else Subplan.Redeployment
    val score =
      (if ThemeDiversityPenalty then 0.32 else 0.28) +
        math.min(0.18, s.devLag * 0.05) +
        math.min(0.14, s.lowMobility * 0.04) +
        math.min(0.14, s.entrenched * 0.06) +
        math.min(0.14, ev.size * 0.04) -
        (if ctx.tacticalThreatToUs then 0.07 else 0.0)
    themed(Theme.Redeployment, Plan.PieceActivation(side), score, ev, Some(subplanId))

  private def spaceClamp(m: List[Motif], side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.16) {
      case SpaceAdvantage(c, _, _, _) if c == side => true
      case PawnAdvance(file, _, _, c, _, _) if c == side && isFlank(file) => true
    }
    val score =
      0.24 +
        (if s.space > 0 then math.min(0.20, s.space * 0.05) else -math.min(0.10, s.space.abs * 0.03)) +
        (if s.clamp then 0.16 else 0.0) +
        (if s.lockedCenter then 0.06 else 0.0) +
        math.min(0.14, ev.size * 0.04) -
        (if s.openCenter && s.kingExposure >= 2 then 0.10 else 0.0)
    themed(Theme.SpaceClamp, Plan.SpaceAdvantage(side), score, ev, Some(Subplan.SpaceClamp))

  private def weaknessFixation(m: List[Motif], ctx: PlanInteractionContext, side: Color, s: SideSnapshot): PlanMatch =
    val opp = !side
    val ev = evidence(m, 0.17) {
      case IsolatedPawn(_, _, c, _, _) if c == opp => true
      case BackwardPawn(_, _, c, _, _) if c == opp => true
      case DoubledPawns(_, c, _, _) if c == opp => true
      case Blockade(_, _, _, c, _, _) if c == side => true
    }
    val score =
      0.23 +
        math.min(0.24, s.oppWeakness * 0.05) +
        (if s.hookChance then 0.10 else 0.0) +
        math.min(0.16, ev.size * 0.05) -
        (if s.oppWeakness == 0 && ev.isEmpty then 0.05 else 0.0)
    themed(Theme.WeaknessFixation, Plan.WeakPawnAttack(side), score, ev, Some(weaknessFixationSubplan(m, ctx, side, s)))

  private def breakPrep(m: List[Motif], ctx: PlanInteractionContext, side: Color): PlanMatch =
    val ev = evidence(m, 0.18) {
      case PawnBreak(_, _, c, _, _) if c == side => true
      case PawnAdvance(file, _, _, c, _, _) if c == side && centralFile(file) => true
    }
    val pa = ctx.pawnAnalysis
    val breakFile = pa.flatMap(_.breakFile)
    val subplanId =
      if pa.exists(_.tensionPolicy == TensionPolicy.Maintain) then Subplan.TensionMaintenance
      else if breakFile.exists(isWingBreakFile) then Subplan.WingBreakTiming
      else Subplan.PawnBreakPreparation
    val score =
      (if ThemeDiversityPenalty then 0.30 else 0.26) +
        (if pa.exists(_.pawnBreakReady) then 0.24 else 0.0) +
        (if pa.exists(_.tensionPolicy == TensionPolicy.Maintain) then 0.06 else 0.0) +
        (if pa.exists(_.tensionPolicy == TensionPolicy.Release) then 0.10 else 0.0) +
        math.min(0.12, ctx.features.map(_.centralSpace.pawnTensionCount).getOrElse(0) * 0.03) +
        math.min(0.16, ev.size * 0.05) -
        (if ctx.tacticalThreatToUs then 0.08 else 0.0)
    themed(Theme.PawnBreakPreparation, Plan.PawnBreakPreparation(side), score, ev, Some(subplanId))

  private def favorableExchange(m: List[Motif], ctx: PlanInteractionContext, side: Color): PlanMatch =
    val ev = evidence(m, 0.17) {
      case Capture(_, _, _, t, c, _, _, _) if c == side &&
          (t == CaptureType.Exchange || t == CaptureType.Recapture || t == CaptureType.Winning) => true
      case RemovingTheDefender(_, _, _, _, c, _, _) if c == side => true
    }
    val evalEdge = ctx.evalFor(side)
    val simplifyWindow = ctx.positionAssessment.exists(_.simplifyBias.shouldSimplify)
    val score =
      0.20 +
        (if simplifyWindow then 0.20 else 0.0) +
        (if evalEdge >= 80 then 0.10 else if evalEdge <= -80 then -0.08 else 0.0) +
        math.min(0.15, ev.size * 0.05)
    themed(Theme.FavorableExchange, Plan.Exchange(side), score, ev, Some(Subplan.FavorableExchange))

  private def flankInfrastructure(m: List[Motif], ctx: PlanInteractionContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.19) {
      case PawnAdvance(file, _, _, c, _, _) if c == side && (file == File.A || file == File.H) => true
      case RookLift(_, _, _, c, _, _) if c == side => true
      case PawnChain(_, _, c, _, _) if c == side => true
    }
    val hasRookLiftSignal = m.exists { case RookLift(_, _, _, c, _, _) if c == side => true; case _ => false }
    val hasHookSignal =
      s.hookChance ||
        m.exists {
          case PawnChain(_, _, c, _, _) if c == side => true
          case PawnAdvance(file, _, _, c, _, _) if c == side && (file == File.B || file == File.G) => true
          case _ => false
        }
    val subplanId =
      if hasRookLiftSignal then Subplan.RookLiftScaffold
      else if hasHookSignal && (!s.rookPawnReady || ev.size <= 1) then Subplan.HookCreation
      else Subplan.FlankInfrastructure
    val score =
      0.16 +
        (if s.rookPawnReady then (if ThemeDiversityPenalty then 0.08 else 0.12) else 0.0) +
        (if s.hookChance then (if ThemeDiversityPenalty then 0.04 else 0.08) else 0.0) +
        math.min(0.25, ev.size * 0.10) -
        (if s.openCenter && s.kingExposure >= 2 then 0.12 else 0.0) -
        (if ctx.tacticalThreatToUs then 0.08 else 0.0)
    themed(Theme.FlankInfrastructure, Plan.PawnStorm(side), score, ev, Some(subplanId))

  private def advantageTransformation(m: List[Motif], ctx: PlanInteractionContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.17) {
      case PassedPawnPush(_, _, c, _, _) if c == side => true
      case RookBehindPassedPawn(_, c, _, _) if c == side => true
      case SeventhRankInvasion(c, _, _) if c == side => true
    }
    val evalEdge = ctx.evalFor(side)
    val score =
      0.22 +
        (if evalEdge.abs >= 70 then 0.14 else 0.0) +
        (if s.ourPassers > s.oppPassers then 0.10 else 0.0) +
        (if ctx.positionAssessment.exists(_.simplifyBias.shouldSimplify) then 0.10 else 0.0) +
        math.min(0.16, ev.size * 0.05)
    themed(Theme.AdvantageTransformation, Plan.Simplification(side), score, ev, Some(Subplan.AdvantageTransformation))

  private def immediateTacticalGain(m: List[Motif], ctx: PlanInteractionContext, side: Color): PlanMatch =
    val ev = evidence(m, 0.20) {
      case Motif.Check(_, _, _, c, _, _) if c == side => true
      case Fork(_, _, _, _, c, _, _) if c == side => true
      case Pin(_, _, _, c, _, _, _, _, _) if c == side => true
      case Skewer(_, _, _, c, _, _, _, _, _) if c == side => true
      case DiscoveredAttack(_, _, _, c, _, _, _, _, _) if c == side => true
      case Capture(_, _, _, t, c, _, _, _) if c == side &&
          (t == CaptureType.Winning || t == CaptureType.Sacrifice || t == CaptureType.ExchangeSacrifice) => true
    }
    val overloadSignal =
      m.exists {
        case Overloading(_, _, _, c, _, _) if c == side => true
        case RemovingTheDefender(_, _, _, _, c, _, _) if c == side => true
        case Deflection(_, _, c, _, _) if c == side => true
        case _ => false
      }
    val clearanceSignal =
      !overloadSignal &&
        m.exists {
          case Clearance(_, _, _, _, c, _, _) if c == side => true
          case Interference(_, _, _, _, c, _, _) if c == side => true
          case _ => false
        }
    val subplanId =
      if overloadSignal then Subplan.DefenderOverload
      else if clearanceSignal then Subplan.ClearanceBreak
      else Subplan.ImmediateTacticalGain
    val tacticalCount = m.count(mm => mm.category == MotifCategory.Tactical && mm.color == side)
    val score =
      0.18 +
        math.min(0.28, tacticalCount * 0.06) +
        (if ctx.tacticalThreatToThem then 0.22 else 0.0) +
        (if ctx.maxThreatLossToThem >= JudgmentThresholds.URGENT_THREAT_CP then 0.10 else 0.0) +
        math.min(0.18, ev.size * 0.06) -
        (if ctx.tacticalThreatToUs && !ctx.tacticalThreatToThem then 0.12 else 0.0)
    themed(Theme.ImmediateTacticalGain, Plan.Counterplay(side), score, ev, Some(subplanId))

  private def themed(
      themeId: String,
      plan: Plan,
      score: Double,
      evidence: List[EvidenceAtom],
      subplanId: Option[String]
  ): PlanMatch =
    PlanMatch(
      plan = plan,
      score = clamp(score),
      evidence = evidence.take(4),
      supportIds = (List(s"theme:$themeId") ++ subplanId.map(id => s"subplan:$id")).distinct
    )

  private def themeOf(pm: PlanMatch): String =
    pm.supportIds.collectFirst { case s if s.startsWith("theme:") => s.stripPrefix("theme:") }.getOrElse("")

  private def computePlanThemePolicyScores(plans: List[PlanMatch]): Map[String, Double] =
    val nonNegative = plans.map(p => p -> p.score.max(0.0))
    val total = nonNegative.map(_._2).sum
    if total <= 1e-9 then Map.empty
    else
      nonNegative
        .groupBy((p, _) => themeOf(p))
        .view
        .mapValues(v => v.map(_._2).sum / total)
        .toMap

  private def annotateWithPlanThemeScore(pm: PlanMatch, themePolicyScore: Double): PlanMatch =
    val cleaned = pm.supportIds.filterNot(_.startsWith("theme_policy_score:"))
    pm.copy(supportIds = (cleaned :+ f"theme_policy_score:${themePolicyScore.max(0.0).min(1.0)}%.3f").distinct)

  private def evidence(
      motifs: List[Motif],
      weight: Double
  )(pf: PartialFunction[Motif, Boolean]): List[EvidenceAtom] =
    motifs.collect { case m if pf.isDefinedAt(m) && pf(m) => EvidenceAtom(m, weight) }.take(4)

  private def snapshot(ctx: PlanInteractionContext, side: Color): SideSnapshot =
    val f = ctx.features.getOrElse(PositionFeatures.empty)
    val p = f.pawns
    val a = f.activity
    val k = f.kingSafety
    val c = f.centralSpace
    val st = ctx.positionKey.flatMap(PositionAnalyzer.extractStrategicState).getOrElse(StrategicStateFeatures.empty)
    val w = side.white
    val oppWeakness =
      if w then p.blackIsolatedPawns + p.blackBackwardPawns + p.blackDoubledPawns
      else p.whiteIsolatedPawns + p.whiteBackwardPawns + p.whiteDoubledPawns
    SideSnapshot(
      lockedCenter = c.lockedCenter,
      openCenter = c.openCenter,
      space = if w then c.spaceDiff else -c.spaceDiff,
      devLag = if w then a.whiteDevelopmentLag else a.blackDevelopmentLag,
      lowMobility = if w then a.whiteLowMobilityPieces else a.blackLowMobilityPieces,
      kingExposure = if w then k.whiteKingExposedFiles else k.blackKingExposedFiles,
      oppWeakness = oppWeakness,
      ourPassers = if w then p.whitePassedPawns else p.blackPassedPawns,
      oppPassers = if w then p.blackPassedPawns else p.whitePassedPawns,
      entrenched = if w then st.whiteEntrenchedPieces else st.blackEntrenchedPieces,
      rookPawnReady = if w then st.whiteRookPawnMarchReady else st.blackRookPawnMarchReady,
      hookChance = if w then st.whiteHookCreationChance else st.blackHookCreationChance,
      clamp = if w then st.whiteColorComplexClamp else st.blackColorComplexClamp
    )

  private def weaknessFixationSubplan(
      m: List[Motif],
      ctx: PlanInteractionContext,
      side: Color,
      s: SideSnapshot
  ): String =
    val opp = !side
    val backwardPawnTarget = m.exists { case BackwardPawn(_, _, c, _, _) if c == opp => true; case _ => false }
    val isolatedPawnTarget = m.exists { case IsolatedPawn(_, _, c, _, _) if c == opp => true; case _ => false }
    val minorityAttackStructure = structureMatches(ctx, StructureId.Carlsbad)
    val iqpTargetStructure = if side.white then structureMatches(ctx, StructureId.IQPBlack) else structureMatches(ctx, StructureId.IQPWhite)
    if minorityAttackStructure && (s.hookChance || s.oppWeakness > 0) then Subplan.MinorityAttackFixation
    else if backwardPawnTarget then Subplan.BackwardPawnTargeting
    else if iqpTargetStructure && isolatedPawnTarget then Subplan.IQPInducement
    else Subplan.WeaknessFixation

  private def structureMatches(ctx: PlanInteractionContext, id: StructureId): Boolean =
    ctx.structureProfile.exists(_.primary == id)

  private def kingExposure(features: Option[PositionFeatures], side: Color): Int =
    features.map { f =>
      if side.white then f.kingSafety.whiteKingExposedFiles else f.kingSafety.blackKingExposedFiles
    }.getOrElse(0)

  private def centralFile(file: File): Boolean =
    file == File.C || file == File.D || file == File.E || file == File.F

  private def isFlank(file: File): Boolean =
    file == File.A || file == File.B || file == File.G || file == File.H

  private def isWingBreakFile(file: String): Boolean =
    val low = Option(file).getOrElse("").trim.toLowerCase
    low == "a" || low == "b" || low == "g" || low == "h"

  private def clamp(score: Double): Double =
    math.max(0.0, math.min(1.0, score))
