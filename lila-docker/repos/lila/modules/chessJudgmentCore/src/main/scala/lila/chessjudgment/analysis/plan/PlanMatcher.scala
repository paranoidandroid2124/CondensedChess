package lila.chessjudgment.analysis.plan

import chess.*
import lila.chessjudgment.analysis.evaluation.{ JudgmentThresholds, PerspectiveMath }
import lila.chessjudgment.analysis.singlePosition.{ GamePhaseType, PawnPlayAnalysis, SinglePositionAssessment, ThreatAnalysis, TensionPolicy }
import lila.chessjudgment.model.*
import lila.chessjudgment.model.Motif.*
import lila.chessjudgment.model.structure.{ PlanAlignment, StructureId, StructureProfile }
import lila.chessjudgment.model.strategic.PlanTaxonomy.{ PlanKind, PlanSignal, PlanTheme, SubplanCatalog }
import lila.chessjudgment.analysis.position.{ PositionAnalyzer, PositionFeatures }

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
  def winPercentFor(color: Color): Double =
    PerspectiveMath.winPercentForMover(color, evalCp)
  def winPercentAdvantageFor(color: Color): Double =
    (winPercentFor(color) - 50.0).max(0.0)
  def phase: String = phaseEnumOpt match
    case Some(GamePhaseType.Opening)    => "opening"
    case Some(GamePhaseType.Middlegame) => "middlegame"
    case Some(GamePhaseType.Endgame)    => "endgame"
    case None                           => "unclassified"
  def phaseEnumOpt: Option[GamePhaseType] =
    positionAssessment.map(_.gamePhase.phaseType)
  private def materialThreat(threat: lila.chessjudgment.analysis.singlePosition.Threat): Boolean =
    threat.kind == lila.chessjudgment.analysis.singlePosition.ThreatKind.Mate ||
      threat.lossIfIgnoredWinPercent.exists(_ >= JudgmentThresholds.MATERIAL_THREAT_WP)
  private def significantThreat(threat: lila.chessjudgment.analysis.singlePosition.Threat): Boolean =
    threat.kind == lila.chessjudgment.analysis.singlePosition.ThreatKind.Mate ||
      threat.lossIfIgnoredWinPercent.exists(_ >= JudgmentThresholds.SIGNIFICANT_THREAT_WP)
  def tacticalThreatToUs: Boolean =
    threatsToUs.exists(_.threats.exists(t => t.turnsToImpact <= 2 && materialThreat(t)))
  def strategicThreatToUs: Boolean =
    threatsToUs.exists(_.threats.exists(t =>
      t.turnsToImpact <= 5 && significantThreat(t) && !tacticalThreatToUs
    ))
  def tacticalThreatToThem: Boolean =
    threatsToThem.exists(_.threats.exists(t => t.turnsToImpact <= 2 && materialThreat(t)))
  def strategicThreatToThem: Boolean =
    threatsToThem.exists(_.threats.exists(t =>
      t.turnsToImpact <= 5 && significantThreat(t) && !tacticalThreatToThem
    ))
  def maxThreatWinPercentLossToUs: Double =
    threatsToUs.flatMap(_.maxWinPercentLossIfIgnored).getOrElse(0.0)
  def maxThreatWinPercentLossToThem: Double =
    threatsToThem.flatMap(_.maxWinPercentLossIfIgnored).getOrElse(0.0)
  def underDefensivePressure: Boolean = tacticalThreatToUs || strategicThreatToUs
  def holdingAttackingThreats: Boolean = tacticalThreatToThem || strategicThreatToThem
  def simplificationReliefPossible: Boolean = underDefensivePressure
  def attackingOpportunityAtRisk: Boolean = holdingAttackingThreats

object PlanMatcher:
  object Theme:
    val Opening = PlanTheme.OpeningPrinciples
    val Restriction = PlanTheme.RestrictionProphylaxis
    val Redeployment = PlanTheme.PieceRedeployment
    val SpaceClamp = PlanTheme.SpaceClamp
    val WeaknessFixation = PlanTheme.WeaknessFixation
    val PawnBreakPreparation = PlanTheme.PawnBreakPreparation
    val FavorableExchange = PlanTheme.FavorableExchange
    val FlankInfrastructure = PlanTheme.FlankInfrastructure
    val AdvantageTransformation = PlanTheme.AdvantageTransformation
    val ImmediateTacticalGain = PlanTheme.ImmediateTacticalGain

  object Subplan:
    val OpeningDevelopment = PlanKind.OpeningDevelopment
    val Restriction = PlanKind.ProphylaxisRestraint
    val Redeployment = PlanKind.WorstPieceImprovement
    val OutpostEntrenchment = PlanKind.OutpostEntrenchment
    val RookFileTransfer = PlanKind.RookFileTransfer
    val SpaceClamp = PlanKind.FlankClamp
    val WeaknessFixation = PlanKind.StaticWeaknessFixation
    val MinorityAttackFixation = PlanKind.MinorityAttackFixation
    val BackwardPawnTargeting = PlanKind.BackwardPawnTargeting
    val IQPInducement = PlanKind.IQPInducement
    val PawnBreakPreparation = PlanKind.CentralBreakTiming
    val WingBreakTiming = PlanKind.WingBreakTiming
    val TensionMaintenance = PlanKind.TensionMaintenance
    val FavorableExchange = PlanKind.SimplificationWindow
    val DefenderTrade = PlanKind.DefenderTrade
    val FlankInfrastructure = PlanKind.RookPawnMarch
    val HookCreation = PlanKind.HookCreation
    val RookLiftScaffold = PlanKind.RookLiftScaffold
    val AdvantageTransformation = PlanKind.SimplificationConversion
    val ImmediateTacticalGain = PlanKind.ForcingTacticalShot
    val DefenderOverload = PlanKind.DefenderOverload
    val ClearanceBreak = PlanKind.ClearanceBreak

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
    (for
      s <- snapshot(ctx, side)
      features <- ctx.features
      phase <- ctx.phaseEnumOpt
    yield
      val openingRaw =
        Option.when(phase == GamePhaseType.Opening)(
          openingDevelopment(motifs, ctx, side, features, s)
        ).toList
      val raw =
        openingRaw ++ List(
          restriction(motifs, ctx, side, s),
          redeployment(motifs, ctx, side, s),
          spaceClamp(motifs, side, s),
          weaknessFixation(motifs, ctx, side, s),
          breakPrep(motifs, ctx, side, features),
          favorableExchange(motifs, ctx, side),
          flankInfrastructure(motifs, ctx, side, s),
          advantageTransformation(motifs, ctx, side, s),
          immediateTacticalGain(motifs, ctx, side)
      )
      val (compatible, events) = applyCompatWithEvents(raw, ctx, side)
      val themePolicyScores = computePlanThemePolicyScores(compatible)
      val availableSignals = availablePlanSignals(ctx, motifs)
      val signalGated =
        compatible
          .map(pm => applySignalGate(pm, availableSignals))
          .filter(_.missingSignals.isEmpty)
      val annotated = signalGated.map(pm => annotateWithPlanThemeScore(pm, themePolicyScores.getOrElse(themeOf(pm), 0.0)))
      val top = annotated.sortBy(p => -p.score).filter(_.score >= 0.18).take(5)
      PlanScoringResult(top, top.headOption.map(_.score).getOrElse(0.0), ctx.phase, events)
    ).getOrElse(PlanScoringResult(Nil, 0.0, ctx.phase, Nil))

  def toActivePlans(sortedPlans: List[PlanMatch], events: List[CompatibilityEvent] = Nil): Option[ActivePlans] =
    sortedPlans.headOption.map { primary =>
      val secondary = sortedPlans.lift(1)
      val suppressed = sortedPlans.drop(2).filter(_.score < primary.score * 0.5)
      ActivePlans(primary, secondary, suppressed, sortedPlans, events)
    }

  def applyCompatWithEvents(
      plans: List[PlanMatch],
      ctx: PlanInteractionContext,
      side: Color
  ): (List[PlanMatch], List[CompatibilityEvent]) =
    import scala.collection.mutable.ListBuffer
    val events = ListBuffer.empty[CompatibilityEvent]

    def theme(pm: PlanMatch): PlanTheme = themeOf(pm)

    def adjust(
        list: List[PlanMatch],
        t: PlanTheme,
        factor: Double,
        adjustment: CompatibilityAdjustment
    ): List[PlanMatch] =
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
              adjustment = adjustment,
              adjustmentType =
                if after > before then CompatibilityAdjustmentType.Boost
                else CompatibilityAdjustmentType.Downweight
            )
          p.copy(score = after)
      }

    var out = plans
    val tactical = out.find(theme(_) == Theme.ImmediateTacticalGain).map(_.score).getOrElse(0.0)
    if tactical >= 0.72 then
      out.map(theme).filter(_ != Theme.ImmediateTacticalGain).distinct.foreach { planTheme =>
        out = adjust(out, planTheme, 0.48, CompatibilityAdjustment.TacticalOverride)
      }
    if ctx.underDefensivePressure then
      out = adjust(out, Theme.Restriction, 1.15, CompatibilityAdjustment.DefensivePressure)
      out = adjust(out, Theme.FlankInfrastructure, 0.74, CompatibilityAdjustment.DefensivePressure)
      out = adjust(out, Theme.PawnBreakPreparation, 0.82, CompatibilityAdjustment.DefensivePressure)
    if ctx.positionAssessment.exists(_.simplifyBias.shouldSimplify) &&
        ctx.winPercentAdvantageFor(side) >= JudgmentThresholds.CONVERSION_EDGE_WP
    then
      out = adjust(out, Theme.FavorableExchange, 1.15, CompatibilityAdjustment.ConversionWindow)
      out = adjust(out, Theme.AdvantageTransformation, 1.12, CompatibilityAdjustment.ConversionWindow)
    if ctx.features.exists(_.centralSpace.openCenter) && kingExposure(ctx.features, side) >= 2 then
      out = adjust(out, Theme.FlankInfrastructure, 0.72, CompatibilityAdjustment.OpenCenterFlankRisk)
    if ctx.phaseEnumOpt.contains(GamePhaseType.Opening) then
      out = adjust(out, Theme.FlankInfrastructure, 0.68, CompatibilityAdjustment.OpeningPhase)
      out = adjust(out, Theme.AdvantageTransformation, 0.78, CompatibilityAdjustment.OpeningPhase)
    (out, events.toList)

  private def openingDevelopment(
      m: List[Motif],
      ctx: PlanInteractionContext,
      side: Color,
      features: PositionFeatures,
      s: SideSnapshot
  ): PlanMatch =
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
    val centerControlDiff =
      if side.white then features.centralSpace.whiteCenterControl - features.centralSpace.blackCenterControl
      else features.centralSpace.blackCenterControl - features.centralSpace.whiteCenterControl
    val ourCenterPawns =
      if side.white then features.centralSpace.whiteCentralPawns else features.centralSpace.blackCentralPawns
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
      0.28 +
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

  private def breakPrep(m: List[Motif], ctx: PlanInteractionContext, side: Color, features: PositionFeatures): PlanMatch =
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
      0.26 +
        (if pa.exists(_.pawnBreakReady) then 0.24 else 0.0) +
        (if pa.exists(_.tensionPolicy == TensionPolicy.Maintain) then 0.06 else 0.0) +
        (if pa.exists(_.tensionPolicy == TensionPolicy.Release) then 0.10 else 0.0) +
        math.min(0.12, features.centralSpace.pawnTensionCount * 0.03) +
        math.min(0.16, ev.size * 0.05) -
        (if ctx.tacticalThreatToUs then 0.08 else 0.0)
    themed(Theme.PawnBreakPreparation, Plan.PawnBreakPreparation(side), score, ev, Some(subplanId))

  private def favorableExchange(m: List[Motif], ctx: PlanInteractionContext, side: Color): PlanMatch =
    val ev = evidence(m, 0.17) {
      case Capture(_, _, _, t, c, _, _, _) if c == side &&
          (t == CaptureType.Exchange || t == CaptureType.Recapture || t == CaptureType.Winning) => true
      case RemovingTheDefender(_, _, _, _, c, _, _) if c == side => true
    }
    val advantageEdge = ctx.winPercentAdvantageFor(side)
    val opponentAdvantageEdge = ctx.winPercentAdvantageFor(!side)
    val simplifyWindow = ctx.positionAssessment.exists(_.simplifyBias.shouldSimplify)
    val score =
      0.20 +
        (if simplifyWindow then 0.20 else 0.0) +
        (if advantageEdge >= JudgmentThresholds.CONVERSION_EDGE_WP then 0.10
         else if opponentAdvantageEdge >= JudgmentThresholds.CONVERSION_EDGE_WP then -0.08
         else 0.0) +
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
        (if s.rookPawnReady then 0.12 else 0.0) +
        (if s.hookChance then 0.08 else 0.0) +
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
    val advantageEdge = ctx.winPercentAdvantageFor(side)
    val score =
      0.22 +
        (if advantageEdge >= JudgmentThresholds.CRITICAL_CANDIDATE_GAP_WP then 0.14 else 0.0) +
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
        (if ctx.maxThreatWinPercentLossToThem >= JudgmentThresholds.URGENT_THREAT_WP then 0.10 else 0.0) +
        math.min(0.18, ev.size * 0.06) -
        (if ctx.tacticalThreatToUs && !ctx.tacticalThreatToThem then 0.12 else 0.0)
    themed(Theme.ImmediateTacticalGain, Plan.Counterplay(side), score, ev, Some(subplanId))

  private def themed(
      theme: PlanTheme,
      plan: Plan,
      score: Double,
      evidence: List[EvidenceAtom],
      subplan: Option[PlanKind]
  ): PlanMatch =
    PlanMatch(
      plan = plan,
      score = clamp(score),
      evidence = evidence.take(4),
      support = (List(PlanSupport.Theme(theme)) ++ subplan.map(PlanSupport.Subplan.apply)).distinct
    )

  private def themeOf(pm: PlanMatch): PlanTheme =
    pm.support.collectFirst { case PlanSupport.Theme(theme) => theme }.getOrElse(PlanTheme.Unknown)

  private def subplanOf(pm: PlanMatch): Option[PlanKind] =
    pm.support.collectFirst { case PlanSupport.Subplan(kind) => kind }

  private def availablePlanSignals(ctx: PlanInteractionContext, motifs: List[Motif]): Set[PlanSignal] =
    import PlanSignal.*
    Set(
      Option.when(motifs.nonEmpty)(KeyMotifs),
      Option.when(ctx.features.nonEmpty && ctx.positionKey.flatMap(PositionAnalyzer.extractStrategicState).nonEmpty)(FutureSnapshot),
      Option.when(ctx.positionAssessment.exists(_.candidateSet.bestLineEvalCp.nonEmpty))(ReplyPvs),
      Option.when(ctx.structureProfile.nonEmpty || ctx.pawnAnalysis.nonEmpty || ctx.planAlignment.nonEmpty)(BoardDelta)
    ).flatten

  private def applySignalGate(pm: PlanMatch, availableSignals: Set[PlanSignal]): PlanMatch =
    val missing =
      subplanOf(pm)
        .flatMap(SubplanCatalog.specs.get)
        .map(_.requiredSignals.filterNot(availableSignals.contains))
        .getOrElse(Nil)
    pm.copy(missingSignals = missing)

  private def computePlanThemePolicyScores(plans: List[PlanMatch]): Map[PlanTheme, Double] =
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
    val cleaned = pm.support.filterNot {
      case _: PlanSupport.ThemePolicyScore => true
      case _                               => false
    }
    pm.copy(support = (cleaned :+ PlanSupport.ThemePolicyScore(themePolicyScore.max(0.0).min(1.0))).distinct)

  private def evidence(
      motifs: List[Motif],
      weight: Double
  )(pf: PartialFunction[Motif, Boolean]): List[EvidenceAtom] =
    motifs.collect { case m if pf.isDefinedAt(m) && pf(m) => EvidenceAtom(m, weight) }.take(4)

  private def snapshot(ctx: PlanInteractionContext, side: Color): Option[SideSnapshot] =
    for
      f <- ctx.features
      st <- ctx.positionKey.flatMap(PositionAnalyzer.extractStrategicState)
    yield
      val p = f.pawns
      val a = f.activity
      val k = f.kingSafety
      val c = f.centralSpace
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
  ): PlanKind =
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
