package lila.chessjudgment.analysis

object PlanTaxonomy:

  enum PlanTheme(val id: String):
    case OpeningPrinciples extends PlanTheme("opening_principles")
    case RestrictionProphylaxis extends PlanTheme("restriction_prophylaxis")
    case PieceRedeployment extends PlanTheme("piece_redeployment")
    case SpaceClamp extends PlanTheme("space_clamp")
    case WeaknessFixation extends PlanTheme("weakness_fixation")
    case PawnBreakPreparation extends PlanTheme("pawn_break_preparation")
    case FavorableExchange extends PlanTheme("favorable_exchange")
    case FlankInfrastructure extends PlanTheme("flank_infrastructure")
    case AdvantageTransformation extends PlanTheme("advantage_transformation")
    case ImmediateTacticalGain extends PlanTheme("immediate_tactical_gain")
    case Unknown extends PlanTheme("unknown")

  object PlanTheme:
    val ranked: List[PlanTheme] = List(
      OpeningPrinciples,
      RestrictionProphylaxis,
      PieceRedeployment,
      SpaceClamp,
      WeaknessFixation,
      PawnBreakPreparation,
      FavorableExchange,
      FlankInfrastructure,
      AdvantageTransformation,
      ImmediateTacticalGain
    )
    private val byId: Map[String, PlanTheme] =
      PlanTheme.values.toList.map(t => t.id -> t).toMap
    def fromId(raw: String): Option[PlanTheme] =
      byId.get(normalize(raw))

  enum PlanKind(val id: String, val theme: PlanTheme):
    case OpeningDevelopment extends PlanKind("opening_development", PlanTheme.OpeningPrinciples)
    case ProphylaxisRestraint extends PlanKind("prophylaxis_restraint", PlanTheme.RestrictionProphylaxis)
    case BreakPrevention extends PlanKind("break_prevention", PlanTheme.RestrictionProphylaxis)
    case KeySquareDenial extends PlanKind("key_square_denial", PlanTheme.RestrictionProphylaxis)

    case OutpostEntrenchment extends PlanKind("outpost_entrenchment", PlanTheme.PieceRedeployment)
    case WorstPieceImprovement extends PlanKind("worst_piece_improvement", PlanTheme.PieceRedeployment)
    case RookFileTransfer extends PlanKind("rook_file_transfer", PlanTheme.PieceRedeployment)
    case BishopReanchor extends PlanKind("bishop_reanchor", PlanTheme.PieceRedeployment)
    case OpenFilePressure extends PlanKind("open_file_pressure", PlanTheme.PieceRedeployment)

    case FlankClamp extends PlanKind("flank_clamp", PlanTheme.SpaceClamp)
    case CentralSpaceBind extends PlanKind("central_space_bind", PlanTheme.SpaceClamp)
    case MobilitySuppression extends PlanKind("mobility_suppression", PlanTheme.SpaceClamp)

    case StaticWeaknessFixation extends PlanKind("static_weakness_fixation", PlanTheme.WeaknessFixation)
    case MinorityAttackFixation extends PlanKind("minority_attack_fixation", PlanTheme.WeaknessFixation)
    case BackwardPawnTargeting extends PlanKind("backward_pawn_targeting", PlanTheme.WeaknessFixation)
    case IQPInducement extends PlanKind("iqp_inducement", PlanTheme.WeaknessFixation)

    case CentralBreakTiming extends PlanKind("central_break_timing", PlanTheme.PawnBreakPreparation)
    case WingBreakTiming extends PlanKind("wing_break_timing", PlanTheme.PawnBreakPreparation)
    case TensionMaintenance extends PlanKind("tension_maintenance", PlanTheme.PawnBreakPreparation)

    case SimplificationWindow extends PlanKind("simplification_window", PlanTheme.FavorableExchange)
    case DefenderTrade extends PlanKind("defender_trade", PlanTheme.FavorableExchange)
    case QueenTradeShield extends PlanKind("queen_trade_shield", PlanTheme.FavorableExchange)
    case BadPieceLiquidation extends PlanKind("bad_piece_liquidation", PlanTheme.FavorableExchange)

    case RookPawnMarch extends PlanKind("rook_pawn_march", PlanTheme.FlankInfrastructure)
    case HookCreation extends PlanKind("hook_creation", PlanTheme.FlankInfrastructure)
    case RookLiftScaffold extends PlanKind("rook_lift_scaffold", PlanTheme.FlankInfrastructure)

    case SimplificationConversion extends PlanKind("simplification_conversion", PlanTheme.AdvantageTransformation)
    case PasserConversion extends PlanKind("passer_conversion", PlanTheme.AdvantageTransformation)
    case PassedPawnManufacture extends PlanKind("passed_pawn_manufacture", PlanTheme.AdvantageTransformation)
    case InvasionTransition extends PlanKind("invasion_transition", PlanTheme.AdvantageTransformation)
    case OppositeBishopsConversion extends PlanKind("opposite_bishops_conversion", PlanTheme.AdvantageTransformation)

    case ForcingTacticalShot extends PlanKind("forcing_tactical_shot", PlanTheme.ImmediateTacticalGain)
    case DefenderOverload extends PlanKind("defender_overload", PlanTheme.ImmediateTacticalGain)
    case ClearanceBreak extends PlanKind("clearance_break", PlanTheme.ImmediateTacticalGain)
    case BatteryPressure extends PlanKind("battery_pressure", PlanTheme.ImmediateTacticalGain)

  object PlanKind:
    private val byId: Map[String, PlanKind] =
      PlanKind.values.toList.map(s => s.id -> s).toMap

    def fromId(raw: String): Option[PlanKind] =
      byId.get(normalize(raw))

  final case class SubplanSpec(
      requiredSignals: List[String],
      horizon: String
  )

  object SubplanCatalog:
    val specs: Map[PlanKind, SubplanSpec] = Map(
      PlanKind.OpeningDevelopment -> SubplanSpec(
        requiredSignals = List("keyMotifs", "futureSnapshot"),
        horizon = "short"
      ),
      PlanKind.ProphylaxisRestraint -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        horizon = "long"
      ),
      PlanKind.BreakPrevention -> SubplanSpec(
        requiredSignals = List("replyPvs", "boardDelta"),
        horizon = "medium"
      ),
      PlanKind.KeySquareDenial -> SubplanSpec(
        requiredSignals = List("keyMotifs"),
        horizon = "medium"
      ),
      PlanKind.OutpostEntrenchment -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        horizon = "long"
      ),
      PlanKind.WorstPieceImprovement -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        horizon = "medium"
      ),
      PlanKind.RookFileTransfer -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "medium"
      ),
      PlanKind.BishopReanchor -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "medium"
      ),
      PlanKind.OpenFilePressure -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "medium"
      ),
      PlanKind.FlankClamp -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "long"
      ),
      PlanKind.CentralSpaceBind -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        horizon = "medium"
      ),
      PlanKind.MobilitySuppression -> SubplanSpec(
        requiredSignals = List("keyMotifs"),
        horizon = "medium"
      ),
      PlanKind.StaticWeaknessFixation -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "long"
      ),
      PlanKind.MinorityAttackFixation -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        horizon = "long"
      ),
      PlanKind.BackwardPawnTargeting -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        horizon = "long"
      ),
      PlanKind.IQPInducement -> SubplanSpec(
        requiredSignals = List("replyPvs", "boardDelta"),
        horizon = "medium"
      ),
      PlanKind.CentralBreakTiming -> SubplanSpec(
        requiredSignals = List("replyPvs", "boardDelta"),
        horizon = "medium"
      ),
      PlanKind.WingBreakTiming -> SubplanSpec(
        requiredSignals = List("replyPvs", "boardDelta"),
        horizon = "medium"
      ),
      PlanKind.TensionMaintenance -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        horizon = "medium"
      ),
      PlanKind.SimplificationWindow -> SubplanSpec(
        requiredSignals = List("replyPvs", "boardDelta"),
        horizon = "medium"
      ),
      PlanKind.DefenderTrade -> SubplanSpec(
        requiredSignals = List("keyMotifs"),
        horizon = "short"
      ),
      PlanKind.QueenTradeShield -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "short"
      ),
      PlanKind.BadPieceLiquidation -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        horizon = "short"
      ),
      PlanKind.RookPawnMarch -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        horizon = "medium"
      ),
      PlanKind.HookCreation -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "medium"
      ),
      PlanKind.RookLiftScaffold -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "medium"
      ),
      PlanKind.SimplificationConversion -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "medium"
      ),
      PlanKind.PasserConversion -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        horizon = "long"
      ),
      PlanKind.PassedPawnManufacture -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot", "boardDelta"),
        horizon = "long"
      ),
      PlanKind.InvasionTransition -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "medium"
      ),
      PlanKind.OppositeBishopsConversion -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot", "keyMotifs"),
        horizon = "long"
      ),
      PlanKind.ForcingTacticalShot -> SubplanSpec(
        requiredSignals = List("replyPvs", "boardDelta", "keyMotifs"),
        horizon = "short"
      ),
      PlanKind.DefenderOverload -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        horizon = "short"
      ),
      PlanKind.ClearanceBreak -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        horizon = "short"
      ),
      PlanKind.BatteryPressure -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs", "futureSnapshot"),
        horizon = "short"
      )
    )

    def byTheme(theme: PlanTheme): List[(PlanKind, SubplanSpec)] =
      specs.toList.filter((sid, _) => sid.theme == theme).sortBy(_._1.id)

  object ThemeResolver:
    import PlanTheme.*

    def sanitizeTheme(raw: String): Option[String] =
      PlanTheme.fromId(raw).filter(_ != Unknown).map(_.id)

    def fromPlanId(raw: String): PlanTheme =
      PlanTheme.fromId(raw).getOrElse(Unknown)

    def fromStructuralState(raw: String): PlanTheme =
      PlanTheme.fromId(raw).getOrElse(Unknown)

    def subplanFromStructuralState(raw: String): Option[PlanKind] =
      PlanKind.fromId(raw)

    def fromSeedId(raw: String): PlanTheme =
      PlanTheme.fromId(raw).getOrElse(Unknown)

    def fromEvidenceSource(raw: String): PlanTheme =
      val low = normalize(raw)
      if low.startsWith("theme:") then
        PlanTheme.fromId(low.stripPrefix("theme:")).getOrElse(Unknown)
      else if low.startsWith("subplan:") then
        subplanFromId(low.stripPrefix("subplan:")).map(_.theme).getOrElse(Unknown)
      else if low.startsWith("structural_state:") then
        fromStructuralState(low.stripPrefix("structural_state:"))
      else if low.startsWith("latent_seed:") then
        fromSeedId(low.stripPrefix("latent_seed:"))
      else if low.startsWith("seed:") then
        fromSeedId(low.stripPrefix("seed:"))
      else
        fromPlanId(low)

    def subplanFromId(raw: String): Option[PlanKind] =
      PlanKind.fromId(raw)

    def subplanFromPlanId(raw: String): Option[PlanKind] =
      PlanKind.fromId(raw)

    def subplanFromSeedId(raw: String): Option[PlanKind] =
      PlanKind.fromId(raw)

    def subplanFromEvidenceSource(raw: String): Option[PlanKind] =
      val low = normalize(raw)
      if low.startsWith("subplan:") then
        subplanFromId(low.stripPrefix("subplan:"))
      else if low.startsWith("structural_state:") then
        subplanFromStructuralState(low.stripPrefix("structural_state:"))
      else if low.startsWith("latent_seed:") then
        subplanFromSeedId(low.stripPrefix("latent_seed:"))
      else if low.startsWith("seed:") then
        subplanFromSeedId(low.stripPrefix("seed:"))
      else subplanFromPlanId(low)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
