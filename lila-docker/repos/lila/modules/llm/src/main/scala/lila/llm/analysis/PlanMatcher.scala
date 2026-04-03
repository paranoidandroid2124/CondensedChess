package lila.llm.analysis

import chess.*
import lila.llm.model.*
import lila.llm.model.Motif.*
import lila.llm.analysis.L3.{ PawnPlayAnalysis, PositionClassification, ThreatAnalysis, TensionPolicy }
import lila.llm.model.structure.{ PlanAlignment, StructureProfile }
import chess.Color.White
import lila.llm.analysis.ThemeTaxonomy.{ ThemeL1, SubplanId }

private val ThemeDiversityPenalty: Boolean =
  sys.env.get("LLM_THEME_DIVERSITY_PENALTY")
    .map(_.trim.toLowerCase)
    .exists(v => v == "1" || v == "true" || v == "yes" || v == "on")

case class IntegratedContext(
    evalCp: Int,
    classification: Option[PositionClassification] = None,
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
    case lila.llm.analysis.L3.GamePhaseType.Opening    => "opening"
    case lila.llm.analysis.L3.GamePhaseType.Middlegame => "middlegame"
    case lila.llm.analysis.L3.GamePhaseType.Endgame    => "endgame"
  def phaseEnum: lila.llm.analysis.L3.GamePhaseType =
    classification.map(_.gamePhase.phaseType).getOrElse(lila.llm.analysis.L3.GamePhaseType.Middlegame)
  def tacticalThreatToUs: Boolean =
    threatsToUs.exists(_.threats.exists(t => t.turnsToImpact <= 2 && t.lossIfIgnoredCp >= 200))
  def strategicThreatToUs: Boolean =
    threatsToUs.exists(_.threats.exists(t =>
      t.turnsToImpact <= 5 && t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP && !tacticalThreatToUs
    ))
  def tacticalThreatToThem: Boolean =
    threatsToThem.exists(_.threats.exists(t => t.turnsToImpact <= 2 && t.lossIfIgnoredCp >= 200))
  def strategicThreatToThem: Boolean =
    threatsToThem.exists(_.threats.exists(t =>
      t.turnsToImpact <= 5 && t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP && !tacticalThreatToThem
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
    val Opening = ThemeL1.OpeningPrinciples.id
    val Restriction = ThemeL1.RestrictionProphylaxis.id
    val Redeployment = ThemeL1.PieceRedeployment.id
    val SpaceClamp = ThemeL1.SpaceClamp.id
    val WeaknessFixation = ThemeL1.WeaknessFixation.id
    val PawnBreakPreparation = ThemeL1.PawnBreakPreparation.id
    val FavorableExchange = ThemeL1.FavorableExchange.id
    val FlankInfrastructure = ThemeL1.FlankInfrastructure.id
    val AdvantageTransformation = ThemeL1.AdvantageTransformation.id
    val ImmediateTacticalGain = ThemeL1.ImmediateTacticalGain.id

  object Subplan:
    val OpeningDevelopment = SubplanId.OpeningDevelopment.id
    val Restriction = SubplanId.ProphylaxisRestraint.id
    val Redeployment = SubplanId.WorstPieceImprovement.id
    val OutpostEntrenchment = SubplanId.OutpostEntrenchment.id
    val RookFileTransfer = SubplanId.RookFileTransfer.id
    val SpaceClamp = SubplanId.FlankClamp.id
    val WeaknessFixation = SubplanId.StaticWeaknessFixation.id
    val PawnBreakPreparation = SubplanId.CentralBreakTiming.id
    val WingBreakTiming = SubplanId.WingBreakTiming.id
    val TensionMaintenance = SubplanId.TensionMaintenance.id
    val FavorableExchange = SubplanId.SimplificationWindow.id
    val FlankInfrastructure = SubplanId.RookPawnMarch.id
    val HookCreation = SubplanId.HookCreation.id
    val RookLiftScaffold = SubplanId.RookLiftScaffold.id
    val AdvantageTransformation = SubplanId.SimplificationConversion.id
    val ImmediateTacticalGain = SubplanId.ForcingTacticalShot.id
    val DefenderOverload = SubplanId.DefenderOverload.id
    val ClearanceBreak = SubplanId.ClearanceBreak.id

  def triggerKind(themeL1: String, subplanId: Option[String]): String =
    ThemeTaxonomy.SubplanId.fromId(subplanId.getOrElse("")).map {
      case ThemeTaxonomy.SubplanId.BreakPrevention     => "break_neutralization"
      case ThemeTaxonomy.SubplanId.KeySquareDenial     => "entry_square_denial"
      case ThemeTaxonomy.SubplanId.OpenFilePressure    => "bounded_file_pressure"
      case ThemeTaxonomy.SubplanId.RookFileTransfer    => "bounded_file_pressure"
      case ThemeTaxonomy.SubplanId.DefenderTrade       => "trade_key_defender"
      case ThemeTaxonomy.SubplanId.SimplificationWindow => "trade_key_defender"
      case ThemeTaxonomy.SubplanId.ProphylaxisRestraint => "counterplay_restraint"
      case other                                       => other.id
    }.orElse(ThemeTaxonomy.ThemeL1.fromId(themeL1).map(_.id)).getOrElse("strategic_claim")

  def ownerFamily(themeL1: String, subplanId: Option[String]): String =
    ThemeTaxonomy.SubplanId.fromId(subplanId.getOrElse("")).map {
      case ThemeTaxonomy.SubplanId.BreakPrevention     => "neutralize_key_break"
      case ThemeTaxonomy.SubplanId.KeySquareDenial     => "half_open_file_pressure"
      case ThemeTaxonomy.SubplanId.OpenFilePressure    => "half_open_file_pressure"
      case ThemeTaxonomy.SubplanId.RookFileTransfer    => "half_open_file_pressure"
      case ThemeTaxonomy.SubplanId.DefenderTrade       => "trade_key_defender"
      case ThemeTaxonomy.SubplanId.SimplificationWindow => "trade_key_defender"
      case other                                       => other.id
    }.orElse(ThemeTaxonomy.ThemeL1.fromId(themeL1).map(_.id)).getOrElse("strategic_claim")

  case class ActivePlans(
      primary: PlanMatch,
      secondary: Option[PlanMatch],
      suppressed: List[PlanMatch],
      allPlans: List[PlanMatch],
      compatibilityEvents: List[CompatibilityEvent] = Nil
  )

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

  def matchPlans(motifs: List[Motif], ctx: IntegratedContext, side: Color): PlanScoringResult =
    val s = snapshot(ctx, side)
    val openingRaw =
      Option
        .when(ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Opening)(
          openingDevelopment(motifs, ctx, side, s)
        )
        .toList
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
    val l1Scores = computeL1PolicyScores(compatible)
    val annotated = compatible.map(pm => annotateWithL1Score(pm, l1Scores.getOrElse(themeOf(pm), 0.0)))
    val top = annotated.sortBy(p => -p.score).filter(_.score >= 0.18).take(5)
    val plans =
      if top.nonEmpty then top
      else
        List(
          themed(
            Theme.Redeployment,
            Plan.PieceActivation(side),
            0.3,
            Nil,
            Nil,
            Nil,
            List("need stronger evidence"),
            subplanId = Some(Subplan.Redeployment)
          )
        )
    PlanScoringResult(plans, plans.head.score, ctx.phase, events)

  def toActivePlans(sortedPlans: List[PlanMatch], events: List[CompatibilityEvent] = Nil): ActivePlans =
    val primary = sortedPlans.headOption.getOrElse(PlanMatch(Plan.CentralControl(Color.White), 0.0, Nil))
    val secondary = sortedPlans.lift(1)
    val suppressed = sortedPlans.drop(2).filter(_.score < primary.score * 0.5)
    ActivePlans(primary, secondary, suppressed, sortedPlans, events)

  def applyCompatWithEvents(
      plans: List[PlanMatch],
      ctx: IntegratedContext,
      side: Color
  ): (List[PlanMatch], List[CompatibilityEvent]) =
    import scala.collection.mutable.ListBuffer
    val events = ListBuffer.empty[CompatibilityEvent]

    def theme(pm: PlanMatch): String =
      pm.supports.collectFirst { case s if s.startsWith("theme:") => s.stripPrefix("theme:") }.getOrElse("")

    def adjust(list: List[PlanMatch], t: String, factor: Double, reason: String): List[PlanMatch] =
      list.map { p =>
        if theme(p) != t then p
        else
          val before = p.score
          val after = clamp(before * factor)
          if math.abs(after - before) > 1e-6 then
            events += CompatibilityEvent(
              planName = p.plan.name,
              originalScore = before,
              finalScore = after,
              delta = after - before,
              reason = reason,
              eventType = if after > before then "boosted" else "downweight"
            )
          p.copy(score = after)
      }

    var out = plans
    val tactical = out.find(theme(_) == Theme.ImmediateTacticalGain).map(_.score).getOrElse(0.0)
    if tactical >= 0.72 then
      out.filter(p => theme(p) != Theme.ImmediateTacticalGain).foreach { p =>
        out = adjust(out, theme(p), 0.48, "override: immediate tactical gain")
      }
    if ctx.underDefensivePressure then
      out = adjust(out, Theme.Restriction, 1.15, "defensive pressure boost")
      out = adjust(out, Theme.FlankInfrastructure, 0.74, "defensive pressure penalty")
      out = adjust(out, Theme.PawnBreakPreparation, 0.82, "defensive pressure penalty")
    if ctx.classification.exists(_.simplifyBias.shouldSimplify) && ctx.evalFor(side) >= 80 then
      out = adjust(out, Theme.FavorableExchange, 1.15, "conversion boost")
      out = adjust(out, Theme.AdvantageTransformation, 1.12, "conversion boost")
    if ctx.features.exists(_.centralSpace.openCenter) && kingExposure(ctx.features, side) >= 2 then
      out = adjust(out, Theme.FlankInfrastructure, 0.72, "open-center flank penalty")
    if ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Opening then
      out = adjust(out, Theme.FlankInfrastructure, 0.68, "opening principle: finish development before flank commitment")
      out = adjust(out, Theme.AdvantageTransformation, 0.78, "opening phase: conversion plan is premature")
    (out, events.toList)

  private def openingDevelopment(m: List[Motif], ctx: IntegratedContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.18) {
      case Centralization(piece, _, c, _, _) if c == side && (piece == Knight || piece == Bishop) =>
        "minor piece development supports opening control"
      case PawnAdvance(file, _, _, c, _, _) if c == side &&
          (file == File.C || file == File.D || file == File.E || file == File.F) =>
        "central pawn claim supports opening space"
      case PawnBreak(file, targetFile, c, _, _) if c == side &&
          (file == File.C || file == File.D || file == File.E || file == File.F ||
            targetFile == File.C || targetFile == File.D || targetFile == File.E || targetFile == File.F) =>
        "central break timing is part of opening development"
      case Castling(_, c, _, _) if c == side =>
        "castling secures king safety while completing development"
      case Fianchetto(_, c, _, _) if c == side =>
        "fianchetto development reinforces long-diagonal control"
      case OpenFileControl(file, c, _, _) if c == side && (file == File.D || file == File.E) =>
        "central file pressure supports opening initiative"
      case SpaceAdvantage(c, pawnDelta, _, _) if c == side && pawnDelta > 0 =>
        "space advantage reflects successful opening coordination"
      case Maneuver(_, purpose, c, _, _) if c == side && purpose.toLowerCase.contains("improv") =>
        "piece improvement keeps opening tempo healthy"
    }
    val f = ctx.features.getOrElse(PositionFeatures.empty)
    val centerControlDiff =
      if side.white then f.centralSpace.whiteCenterControl - f.centralSpace.blackCenterControl
      else f.centralSpace.blackCenterControl - f.centralSpace.whiteCenterControl
    val ourCenterPawns =
      if side.white then f.centralSpace.whiteCentralPawns
      else f.centralSpace.blackCentralPawns
    val hasCastled =
      m.exists {
        case Castling(_, c, _, _) if c == side => true
        case _                                  => false
      }
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
    themed(
      Theme.Opening,
      Plan.OpeningDevelopment(side),
      score,
      ev,
      List("develop first, then commit to structural or flank operations"),
      List(
        Option.when(ctx.tacticalThreatToUs)("tactical defense can delay opening development goals"),
        Option.when(ctx.tacticalThreatToThem)("forcing tactical chance may outweigh pure development moves")
      ).flatten,
      List(
        Option.when(!hasCastled)("king safety remains unresolved"),
        Option.when(ourCenterPawns <= 0)("center presence is still underdeveloped"),
        Option.when(developmentNeed <= 0 && ev.isEmpty)("development edge is not clearly visible")
      ).flatten,
      subplanId = Some(Subplan.OpeningDevelopment)
    )

  private def restriction(m: List[Motif], ctx: IntegratedContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.18) {
      case Domination(_, _, _, c, _, _) if c == side => "domination restricts enemy mobility"
      case Blockade(_, _, _, c, _, _) if c == side   => "blockade limits counterplay"
      case OpenFileControl(_, c, _, _) if c == side  => "file control supports prophylaxis"
    }
    val score =
      0.28 +
        (if ctx.tacticalThreatToUs then 0.22 else 0.0) +
        (if ctx.strategicThreatToUs then 0.12 else 0.0) +
        (if s.clamp then 0.14 else 0.0) +
        (if s.lockedCenter then 0.06 else 0.0) +
        math.min(0.16, ev.size * 0.05) -
        (if ctx.tacticalThreatToThem && !ctx.tacticalThreatToUs then 0.08 else 0.0)
    themed(
      Theme.Restriction,
      Plan.Prophylaxis(side, "counterplay"),
      score,
      ev,
      List("prevent opponent plan first"),
      Option.when(ctx.tacticalThreatToThem && !ctx.tacticalThreatToUs)("forcing tactical line may be stronger").toList,
      Option.when(!s.clamp && ev.isEmpty)("need stable restraint geometry").toList,
      subplanId = Some(Subplan.Restriction)
    )

  private def redeployment(m: List[Motif], ctx: IntegratedContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.17) {
      case Outpost(_, _, c, _, _) if c == side        => "outpost route exists"
      case Centralization(_, _, c, _, _) if c == side => "centralization improves coordination"
      case Maneuver(_, _, c, _, _) if c == side       => "quiet maneuver improves worst piece"
      case RookLift(_, _, _, c, _, _) if c == side    => "rook lift supports redeployment"
    }
    val prefersOutpost =
      s.entrenched > 0 ||
        m.exists {
          case Outpost(_, _, c, _, _) if c == side => true
          case _                                    => false
        }
    val prefersRookFileTransfer =
      !prefersOutpost &&
        m.exists {
          case RookLift(_, _, _, c, _, _) if c == side      => true
          case OpenFileControl(_, c, _, _) if c == side     => true
          case SemiOpenFileControl(_, c, _, _) if c == side => true
          case _                                             => false
        }
    val subplanId =
      if prefersOutpost then Subplan.OutpostEntrenchment
      else if prefersRookFileTransfer then Subplan.RookFileTransfer
      else Subplan.Redeployment
    val score =
      (if ThemeDiversityPenalty then 0.32 else 0.28) + // Increased base score (was 0.30/0.26)
        math.min(0.18, s.devLag * 0.05) +
        math.min(0.14, s.lowMobility * 0.04) +
        math.min(0.14, s.entrenched * 0.06) +
        math.min(0.14, ev.size * 0.04) -
        (if ctx.tacticalThreatToUs then 0.07 else 0.0)
    themed(
      Theme.Redeployment,
      Plan.PieceActivation(side),
      score,
      ev,
      List("improve worst-placed piece"),
      Option.when(ctx.tacticalThreatToUs)("defensive urgency may delay reroutes").toList,
      Option.when(ev.isEmpty && s.devLag <= 1 && s.lowMobility <= 1)("few clear improvement targets").toList,
      subplanId = Some(subplanId)
    )

  private def spaceClamp(m: List[Motif], side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.16) {
      case SpaceAdvantage(c, _, _, _) if c == side                             => "space edge supports clamp"
      case PawnAdvance(file, _, _, c, _, _) if c == side && isFlank(file)      => "flank pawn advance builds clamp"
    }
    val score =
      0.24 +
        (if s.space > 0 then math.min(0.20, s.space * 0.05) else -math.min(0.10, s.space.abs * 0.03)) +
        (if s.clamp then 0.16 else 0.0) +
        (if s.lockedCenter then 0.06 else 0.0) +
        math.min(0.14, ev.size * 0.04) -
        (if s.openCenter && s.kingExposure >= 2 then 0.10 else 0.0)
    themed(
      Theme.SpaceClamp,
      Plan.SpaceAdvantage(side),
      score,
      ev,
      List("space limits opponent freedom"),
      Option.when(s.openCenter && s.kingExposure >= 2)("open center punishes slow clamp moves").toList,
      Option.when(!s.clamp && s.space <= 0)("need more pawn-front control first").toList,
      subplanId = Some(Subplan.SpaceClamp)
    )

  private def weaknessFixation(m: List[Motif], ctx: IntegratedContext, side: Color, s: SideSnapshot): PlanMatch =
    val opp = !side
    val ev = evidence(m, 0.17) {
      case IsolatedPawn(_, _, c, _, _) if c == opp  => "isolated pawn can be fixed"
      case BackwardPawn(_, _, c, _, _) if c == opp  => "backward pawn can be fixed"
      case DoubledPawns(_, c, _, _) if c == opp     => "doubled pawns provide a static target"
      case Blockade(_, _, _, c, _, _) if c == side  => "blockade supports fixation"
    }
    val score =
      0.23 +
        math.min(0.24, s.oppWeakness * 0.05) +
        (if s.hookChance then 0.10 else 0.0) +
        math.min(0.16, ev.size * 0.05) -
        (if s.oppWeakness == 0 && ev.isEmpty then 0.05 else 0.0)
    themed(
      Theme.WeaknessFixation,
      Plan.WeakPawnAttack(side, "fixed"),
      score,
      ev,
      List("create and fix long-term targets"),
      Option.when(ctx.tacticalThreatToUs && !ctx.tacticalThreatToThem)("immediate defense can postpone fixation").toList,
      Option.when(s.oppWeakness == 0 && ev.isEmpty)("need to induce weakness before attacking it").toList,
      subplanId = Some(Subplan.WeaknessFixation)
    )

  private def breakPrep(m: List[Motif], ctx: IntegratedContext, side: Color): PlanMatch =
    val ev = evidence(m, 0.18) {
      case PawnBreak(_, _, c, _, _) if c == side => "break route is visible"
      case PawnAdvance(file, _, _, c, _, _) if c == side && (file == File.C || file == File.D || file == File.E || file == File.F) =>
        "central pawn move supports break timing"
    }
    val pa = ctx.pawnAnalysis
    val breakFile = pa.flatMap(_.breakFile)
    val breakLabel = breakFile.map(f => s"$f-break").getOrElse("central")
    val subplanId =
      if pa.exists(_.tensionPolicy == TensionPolicy.Maintain) then Subplan.TensionMaintenance
      else if breakFile.exists(isWingBreakFile) then Subplan.WingBreakTiming
      else Subplan.PawnBreakPreparation
    val score =
      (if ThemeDiversityPenalty then 0.30 else 0.26) + // Increased base score (was 0.28/0.24)
        (if pa.exists(_.pawnBreakReady) then 0.24 else 0.0) +
        (if pa.exists(_.tensionPolicy == TensionPolicy.Maintain) then 0.06 else 0.0) +
        (if pa.exists(_.tensionPolicy == TensionPolicy.Release) then 0.10 else 0.0) +
        math.min(0.12, ctx.features.map(_.centralSpace.pawnTensionCount).getOrElse(0) * 0.03) +
        math.min(0.16, ev.size * 0.05) -
        (if ctx.tacticalThreatToUs then 0.08 else 0.0)
    themed(
      Theme.PawnBreakPreparation,
      Plan.PawnBreakPreparation(side, breakLabel),
      score,
      ev,
      List("prepare structure change before committing"),
      Option.when(ctx.tacticalThreatToUs)("defensive tasks delay break prep").toList,
      Option.when(!pa.exists(_.pawnBreakReady))("break preconditions not fully met").toList,
      subplanId = Some(subplanId)
    )

  private def favorableExchange(m: List[Motif], ctx: IntegratedContext, side: Color): PlanMatch =
    val ev = evidence(m, 0.17) {
      case Capture(_, _, _, t, c, _, _, _) if c == side &&
          (t == CaptureType.Exchange || t == CaptureType.Recapture || t == CaptureType.Winning) =>
        "capture pattern supports favorable exchange"
      case RemovingTheDefender(_, _, _, _, c, _, _) if c == side => "defender removal supports exchange design"
    }
    val evalEdge = ctx.evalFor(side)
    val simplifyWindow = ctx.classification.exists(_.simplifyBias.shouldSimplify)
    val score =
      0.20 +
        (if simplifyWindow then 0.20 else 0.0) +
        (if evalEdge >= 80 then 0.10 else if evalEdge <= -80 then -0.08 else 0.0) +
        math.min(0.15, ev.size * 0.05)
    themed(
      Theme.FavorableExchange,
      Plan.Exchange(side, "favorable simplification"),
      score,
      ev,
      List("exchange only when structure improves"),
      Option.when(evalEdge <= -80)("behind on eval; simplification may help opponent").toList,
      Option.when(!simplifyWindow && ev.isEmpty)("need clear exchange asymmetry first").toList,
      subplanId = Some(Subplan.FavorableExchange)
    )

  private def flankInfrastructure(m: List[Motif], ctx: IntegratedContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.19) {
      case PawnAdvance(file, _, _, c, _, _) if c == side && (file == File.A || file == File.H) =>
        "rook pawn advance builds flank infrastructure"
      case RookLift(_, _, _, c, _, _) if c == side => "rook lift supports flank pressure"
      case PawnChain(_, _, c, _, _) if c == side   => "pawn chain anchors flank expansion"
    }
    val hasRookLiftSignal =
      m.exists {
        case RookLift(_, _, _, c, _, _) if c == side => true
        case _                                        => false
      }
    val hasHookSignal =
      s.hookChance ||
        m.exists {
          case PawnChain(_, _, c, _, _) if c == side => true
          case PawnAdvance(file, _, _, c, _, _) if c == side && (file == File.B || file == File.G) =>
            true
          case _ => false
        }
    val subplanId =
      if hasRookLiftSignal then Subplan.RookLiftScaffold
      else if hasHookSignal && (!s.rookPawnReady || ev.size <= 1) then Subplan.HookCreation
      else Subplan.FlankInfrastructure
    val flank = if s.rookPawnReady || s.hookChance then "kingside" else "queenside"
    val score =
      0.16 + // Reduce base score (was 0.22)
        (if s.rookPawnReady then (if ThemeDiversityPenalty then 0.08 else 0.12) else 0.0) +
        (if s.hookChance then (if ThemeDiversityPenalty then 0.04 else 0.08) else 0.0) +
        math.min(0.25, ev.size * 0.10) - // Increase motif evidence cap from 0.18->0.25 and scaling from 0.06->0.10
        (if s.openCenter && s.kingExposure >= 2 then 0.12 else 0.0) -
        (if ctx.tacticalThreatToUs then 0.08 else 0.0)
    themed(
      Theme.FlankInfrastructure,
      Plan.PawnStorm(side, flank),
      score,
      ev,
      List("build attack infrastructure before direct assault"),
      List(
        Option.when(s.openCenter && s.kingExposure >= 2)("open center + king exposure can refute flank race"),
        Option.when(ctx.tacticalThreatToUs)("urgent defense may override flank plan")
      ).flatten,
      Option.when(!s.rookPawnReady && !s.hookChance)("need hook/lever before commitment").toList,
      subplanId = Some(subplanId)
    )

  private def advantageTransformation(m: List[Motif], ctx: IntegratedContext, side: Color, s: SideSnapshot): PlanMatch =
    val ev = evidence(m, 0.17) {
      case PassedPawnPush(_, _, c, _, _) if c == side    => "passed pawn can convert dynamic edge"
      case RookBehindPassedPawn(_, c, _, _) if c == side => "rook behind passer supports conversion"
      case SeventhRankInvasion(c, _, _) if c == side     => "seventh-rank activity helps conversion"
    }
    val evalEdge = ctx.evalFor(side)
    val score =
      0.22 +
        (if evalEdge.abs >= 70 then 0.14 else 0.0) +
        (if s.ourPassers > s.oppPassers then 0.10 else 0.0) +
        (if ctx.classification.exists(_.simplifyBias.shouldSimplify) then 0.10 else 0.0) +
        math.min(0.16, ev.size * 0.05)
    themed(
      Theme.AdvantageTransformation,
      Plan.Simplification(side),
      score,
      ev,
      List("convert one advantage form into another"),
      Nil,
      Option.when(evalEdge.abs < 40 && ev.isEmpty)("need clearer imbalance before transformation").toList,
      subplanId = Some(Subplan.AdvantageTransformation)
    )

  private def immediateTacticalGain(m: List[Motif], ctx: IntegratedContext, side: Color): PlanMatch =
    val ev = evidence(m, 0.20) {
      case Motif.Check(_, _, _, c, _, _) if c == side                   => "forcing check sequence exists"
      case Fork(_, _, _, _, c, _, _) if c == side                        => "fork gives immediate tactical gains"
      case Pin(_, _, _, c, _, _, _, _, _) if c == side                   => "pin creates forcing pressure"
      case Skewer(_, _, _, c, _, _, _, _, _) if c == side                => "skewer creates forcing pressure"
      case DiscoveredAttack(_, _, _, c, _, _, _, _, _) if c == side     => "discovered attack increases forcing value"
      case Capture(_, _, _, t, c, _, _, _) if c == side &&
          (t == CaptureType.Winning || t == CaptureType.Sacrifice || t == CaptureType.ExchangeSacrifice) =>
        "forcing capture sequence is available"
    }
    val overloadSignal =
      m.exists {
        case Overloading(_, _, _, c, _, _) if c == side           => true
        case RemovingTheDefender(_, _, _, _, c, _, _) if c == side => true
        case Deflection(_, _, c, _, _) if c == side               => true
        case _                                                    => false
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
        (if ctx.maxThreatLossToThem >= Thresholds.URGENT_THREAT_CP then 0.10 else 0.0) +
        math.min(0.18, ev.size * 0.06) -
        (if ctx.tacticalThreatToUs && !ctx.tacticalThreatToThem then 0.12 else 0.0)
    themed(
      Theme.ImmediateTacticalGain,
      Plan.Counterplay(side, "immediate tactical gain"),
      score,
      ev,
      List("forcing line takes priority when available"),
      Option.when(ctx.tacticalThreatToUs && !ctx.tacticalThreatToThem)("opponent threat can override our tactic").toList,
      Option.when(tacticalCount == 0 && !ctx.tacticalThreatToThem)("no forcing tactical edge detected").toList,
      subplanId = Some(subplanId)
    )

  private def themed(
      themeId: String,
      plan: Plan,
      score: Double,
      evidence: List[EvidenceAtom],
      supports: List[String],
      blockers: List[String],
      missing: List[String],
      subplanId: Option[String]
  ): PlanMatch =
    PlanMatch(
      plan = plan,
      score = clamp(score),
      evidence = evidence.take(4),
      supports = (List(s"theme:$themeId") ++ subplanId.map(id => s"subplan:$id") ++ supports).distinct.take(8),
      blockers = blockers.distinct.take(4),
      missingPrereqs = missing.distinct.take(3)
    )

  private def themeOf(pm: PlanMatch): String =
    pm.supports.collectFirst { case s if s.startsWith("theme:") => s.stripPrefix("theme:") }.getOrElse("")

  private def computeL1PolicyScores(plans: List[PlanMatch]): Map[String, Double] =
    val nonNegative = plans.map(p => p -> p.score.max(0.0))
    val total = nonNegative.map(_._2).sum
    if total <= 1e-9 then Map.empty
    else
      nonNegative
        .groupBy((p, _) => themeOf(p))
        .view
        .mapValues(v => v.map(_._2).sum / total)
        .toMap

  private def annotateWithL1Score(pm: PlanMatch, l1Score: Double): PlanMatch =
    val cleaned = pm.supports.filterNot(_.startsWith("l1_score:"))
    pm.copy(supports = (cleaned :+ f"l1_score:${l1Score.max(0.0).min(1.0)}%.3f").distinct)

  private def evidence(
      motifs: List[Motif],
      weight: Double
  )(pf: PartialFunction[Motif, String]): List[EvidenceAtom] =
    motifs.collect { case m if pf.isDefinedAt(m) => EvidenceAtom(m, weight, pf(m)) }.take(4)

  private def snapshot(ctx: IntegratedContext, side: Color): SideSnapshot =
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

  private def kingExposure(features: Option[PositionFeatures], side: Color): Int =
    features match
      case Some(f) => if side.white then f.kingSafety.whiteKingExposedFiles else f.kingSafety.blackKingExposedFiles
      case None    => 0

  private def isFlank(file: File): Boolean =
    file == File.A || file == File.B || file == File.C || file == File.F || file == File.G || file == File.H

  private def isWingBreakFile(raw: String): Boolean =
    val low = Option(raw).getOrElse("").trim.toLowerCase
    low == "a" || low == "b" || low == "g" || low == "h"

  private def clamp(v: Double): Double = math.max(0.0, math.min(1.0, v))
