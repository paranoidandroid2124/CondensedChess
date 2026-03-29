package lila.llm.analysis

import lila.llm.model.authoring.{ LatentSeed, PlanHypothesis, SeedFamily }

object ThemeTaxonomy:

  enum ThemeL1(val id: String):
    case OpeningPrinciples extends ThemeL1("opening_principles")
    case RestrictionProphylaxis extends ThemeL1("restriction_prophylaxis")
    case PieceRedeployment extends ThemeL1("piece_redeployment")
    case SpaceClamp extends ThemeL1("space_clamp")
    case WeaknessFixation extends ThemeL1("weakness_fixation")
    case PawnBreakPreparation extends ThemeL1("pawn_break_preparation")
    case FavorableExchange extends ThemeL1("favorable_exchange")
    case FlankInfrastructure extends ThemeL1("flank_infrastructure")
    case AdvantageTransformation extends ThemeL1("advantage_transformation")
    case ImmediateTacticalGain extends ThemeL1("immediate_tactical_gain")
    case Unknown extends ThemeL1("unknown")

  object ThemeL1:
    val ranked: List[ThemeL1] = List(
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
    private val byId: Map[String, ThemeL1] =
      ThemeL1.values.toList.map(t => t.id -> t).toMap
    def fromId(raw: String): Option[ThemeL1] =
      byId.get(normalize(raw))

  enum SubplanId(val id: String, val theme: ThemeL1):
    case OpeningDevelopment extends SubplanId("opening_development", ThemeL1.OpeningPrinciples)
    case ProphylaxisRestraint extends SubplanId("prophylaxis_restraint", ThemeL1.RestrictionProphylaxis)
    case BreakPrevention extends SubplanId("break_prevention", ThemeL1.RestrictionProphylaxis)
    case KeySquareDenial extends SubplanId("key_square_denial", ThemeL1.RestrictionProphylaxis)

    case OutpostEntrenchment extends SubplanId("outpost_entrenchment", ThemeL1.PieceRedeployment)
    case WorstPieceImprovement extends SubplanId("worst_piece_improvement", ThemeL1.PieceRedeployment)
    case RookFileTransfer extends SubplanId("rook_file_transfer", ThemeL1.PieceRedeployment)
    case BishopReanchor extends SubplanId("bishop_reanchor", ThemeL1.PieceRedeployment)
    case OpenFilePressure extends SubplanId("open_file_pressure", ThemeL1.PieceRedeployment)

    case FlankClamp extends SubplanId("flank_clamp", ThemeL1.SpaceClamp)
    case CentralSpaceBind extends SubplanId("central_space_bind", ThemeL1.SpaceClamp)
    case MobilitySuppression extends SubplanId("mobility_suppression", ThemeL1.SpaceClamp)

    case StaticWeaknessFixation extends SubplanId("static_weakness_fixation", ThemeL1.WeaknessFixation)
    case MinorityAttackFixation extends SubplanId("minority_attack_fixation", ThemeL1.WeaknessFixation)
    case BackwardPawnTargeting extends SubplanId("backward_pawn_targeting", ThemeL1.WeaknessFixation)
    case IQPInducement extends SubplanId("iqp_inducement", ThemeL1.WeaknessFixation)

    case CentralBreakTiming extends SubplanId("central_break_timing", ThemeL1.PawnBreakPreparation)
    case WingBreakTiming extends SubplanId("wing_break_timing", ThemeL1.PawnBreakPreparation)
    case TensionMaintenance extends SubplanId("tension_maintenance", ThemeL1.PawnBreakPreparation)

    case SimplificationWindow extends SubplanId("simplification_window", ThemeL1.FavorableExchange)
    case DefenderTrade extends SubplanId("defender_trade", ThemeL1.FavorableExchange)
    case QueenTradeShield extends SubplanId("queen_trade_shield", ThemeL1.FavorableExchange)
    case BadPieceLiquidation extends SubplanId("bad_piece_liquidation", ThemeL1.FavorableExchange)

    case RookPawnMarch extends SubplanId("rook_pawn_march", ThemeL1.FlankInfrastructure)
    case HookCreation extends SubplanId("hook_creation", ThemeL1.FlankInfrastructure)
    case RookLiftScaffold extends SubplanId("rook_lift_scaffold", ThemeL1.FlankInfrastructure)

    case SimplificationConversion extends SubplanId("simplification_conversion", ThemeL1.AdvantageTransformation)
    case PasserConversion extends SubplanId("passer_conversion", ThemeL1.AdvantageTransformation)
    case PassedPawnManufacture extends SubplanId("passed_pawn_manufacture", ThemeL1.AdvantageTransformation)
    case InvasionTransition extends SubplanId("invasion_transition", ThemeL1.AdvantageTransformation)
    case OppositeBishopsConversion extends SubplanId("opposite_bishops_conversion", ThemeL1.AdvantageTransformation)

    case ForcingTacticalShot extends SubplanId("forcing_tactical_shot", ThemeL1.ImmediateTacticalGain)
    case DefenderOverload extends SubplanId("defender_overload", ThemeL1.ImmediateTacticalGain)
    case ClearanceBreak extends SubplanId("clearance_break", ThemeL1.ImmediateTacticalGain)
    case BatteryPressure extends SubplanId("battery_pressure", ThemeL1.ImmediateTacticalGain)

  object SubplanId:
    private val byId: Map[String, SubplanId] =
      SubplanId.values.toList.map(s => s.id -> s).toMap

    def fromId(raw: String): Option[SubplanId] =
      byId.get(normalize(raw))

  final case class SubplanSpec(
      requiredSignals: List[String],
      objective: String,
      horizon: String,
      aliases: List[String] = Nil
  )

  object SubplanCatalog:
    val specs: Map[SubplanId, SubplanSpec] = Map(
      SubplanId.OpeningDevelopment -> SubplanSpec(
        requiredSignals = List("keyMotifs", "futureSnapshot"),
        objective = "complete development while securing king and center",
        horizon = "short",
        aliases = List("opening development", "opening principles")
      ),
      SubplanId.ProphylaxisRestraint -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "restrict opponent counterplay before expansion",
        horizon = "long",
        aliases = List("prophylaxis", "restriction")
      ),
      SubplanId.BreakPrevention -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "deny opponent central break timing",
        horizon = "medium",
        aliases = List("break prevention", "prevent break")
      ),
      SubplanId.KeySquareDenial -> SubplanSpec(
        requiredSignals = List("keyMotifs"),
        objective = "deny key entry squares and outposts",
        horizon = "medium",
        aliases = List("key-square denial")
      ),
      SubplanId.OutpostEntrenchment -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "stabilize a piece on an outpost",
        horizon = "long",
        aliases = List("outpost", "entrenched piece")
      ),
      SubplanId.WorstPieceImprovement -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        objective = "improve the worst piece without tactical collapse",
        horizon = "medium",
        aliases = List("piece improvement", "redeployment")
      ),
      SubplanId.RookFileTransfer -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "reroute rook to active file or rank",
        horizon = "medium",
        aliases = List("rook activation", "file transfer")
      ),
      SubplanId.BishopReanchor -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "reroute a passive bishop onto an active diagonal",
        horizon = "medium",
        aliases = List("bishop reanchor", "bad bishop reroute")
      ),
      SubplanId.OpenFilePressure -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "occupy and reinforce an open or semi-open file with heavy pieces",
        horizon = "medium",
        aliases = List("open file pressure", "open file doubling", "file pressure")
      ),
      SubplanId.FlankClamp -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "gain flank space while suppressing mobility",
        horizon = "long",
        aliases = List("clamp", "bind")
      ),
      SubplanId.CentralSpaceBind -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        objective = "hold central squares and limit breaks",
        horizon = "medium",
        aliases = List("space bind", "center bind")
      ),
      SubplanId.MobilitySuppression -> SubplanSpec(
        requiredSignals = List("keyMotifs"),
        objective = "reduce piece mobility and escape squares",
        horizon = "medium",
        aliases = List("restriction net")
      ),
      SubplanId.StaticWeaknessFixation -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "create a persistent static weakness",
        horizon = "long",
        aliases = List("fixation", "static target")
      ),
      SubplanId.MinorityAttackFixation -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "induce and attack a fixed weakness via minority attack",
        horizon = "long",
        aliases = List("minority attack")
      ),
      SubplanId.BackwardPawnTargeting -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        objective = "fix and pressure a backward pawn",
        horizon = "long",
        aliases = List("backward pawn", "weak pawn")
      ),
      SubplanId.IQPInducement -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "induce an isolated pawn and turn it into a fixed target",
        horizon = "medium",
        aliases = List("iqp inducement", "create iqp", "isolated pawn inducement")
      ),
      SubplanId.CentralBreakTiming -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "prepare a central break at the right timing",
        horizon = "medium",
        aliases = List("central break", "d-break", "e-break")
      ),
      SubplanId.WingBreakTiming -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "prepare a wing break after coordination",
        horizon = "medium",
        aliases = List("wing break")
      ),
      SubplanId.TensionMaintenance -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        objective = "keep tension until conversion is favorable",
        horizon = "medium",
        aliases = List("maintain tension")
      ),
      SubplanId.SimplificationWindow -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "trade pieces only in favorable windows",
        horizon = "medium",
        aliases = List("favorable simplification")
      ),
      SubplanId.DefenderTrade -> SubplanSpec(
        requiredSignals = List("keyMotifs"),
        objective = "remove key defender to expose weaknesses",
        horizon = "short",
        aliases = List("remove defender")
      ),
      SubplanId.QueenTradeShield -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "neutralize attack via queen trade",
        horizon = "short",
        aliases = List("queen trade")
      ),
      SubplanId.BadPieceLiquidation -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "liquidate a bad piece while preserving the better structure",
        horizon = "short",
        aliases = List("bad piece liquidation", "trade bad bishop")
      ),
      SubplanId.RookPawnMarch -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "gain flank space with rook-pawn advance",
        horizon = "medium",
        aliases = List("rook-pawn march", "pawn storm")
      ),
      SubplanId.HookCreation -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "create a hook and open attacking files",
        horizon = "medium",
        aliases = List("hook")
      ),
      SubplanId.RookLiftScaffold -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "build rook-lift attack infrastructure",
        horizon = "medium",
        aliases = List("rook lift")
      ),
      SubplanId.SimplificationConversion -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "convert dynamic edge through simplification",
        horizon = "medium",
        aliases = List("conversion")
      ),
      SubplanId.PasserConversion -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "convert edge into passer dynamics",
        horizon = "long",
        aliases = List("passed pawn", "passer")
      ),
      SubplanId.PassedPawnManufacture -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot", "l1Delta"),
        objective = "manufacture a passed pawn by fixing structure and timing exchanges",
        horizon = "long",
        aliases = List("passed pawn manufacture", "create passed pawn")
      ),
      SubplanId.InvasionTransition -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "transform initiative into invasion/endgame edge",
        horizon = "medium",
        aliases = List("invasion", "transition")
      ),
      SubplanId.OppositeBishopsConversion -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot", "keyMotifs"),
        objective = "convert an opposite-colored bishops ending through color-complex penetration or passer transition",
        horizon = "long",
        aliases = List("opposite bishops conversion", "opposite-colored bishops conversion")
      ),
      SubplanId.ForcingTacticalShot -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta", "keyMotifs"),
        objective = "execute forcing tactical line with concrete gain",
        horizon = "short",
        aliases = List("forcing", "tactical shot")
      ),
      SubplanId.DefenderOverload -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "overload defender before breakthrough",
        horizon = "short",
        aliases = List("overload")
      ),
      SubplanId.ClearanceBreak -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "clear lines/squares for tactical execution",
        horizon = "short",
        aliases = List("clearance")
      ),
      SubplanId.BatteryPressure -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs", "futureSnapshot"),
        objective = "align major pieces on a critical line before the breakthrough",
        horizon = "short",
        aliases = List("battery pressure", "battery formation")
      )
    )

    def byTheme(theme: ThemeL1): List[(SubplanId, SubplanSpec)] =
      specs.toList.filter((sid, _) => sid.theme == theme).sortBy(_._1.id)

  object ThemeResolver:
    import ThemeL1.*

    private val planAliases: List[(String, ThemeL1)] = List(
      "openingprinciples" -> OpeningPrinciples,
      "openingdevelopment" -> OpeningPrinciples,
      "opening development" -> OpeningPrinciples,
      "restriction" -> RestrictionProphylaxis,
      "prophylaxis" -> RestrictionProphylaxis,
      "defensiveconsolidation" -> RestrictionProphylaxis,
      "deny" -> RestrictionProphylaxis,
      "consolidat" -> RestrictionProphylaxis,
      "prevent" -> RestrictionProphylaxis,
      "minorpiecemaneuver" -> PieceRedeployment,
      "pieceactivation" -> PieceRedeployment,
      "rookactivation" -> PieceRedeployment,
      "filecontrol" -> PieceRedeployment,
      "redeploy" -> PieceRedeployment,
      "outpost" -> PieceRedeployment,
      "knight" -> PieceRedeployment,
      "bishop" -> PieceRedeployment,
      "maneuver" -> PieceRedeployment,
      "regroup" -> PieceRedeployment,
      "improve" -> PieceRedeployment,
      "spaceadvantage" -> SpaceClamp,
      "space" -> SpaceClamp,
      "clamp" -> SpaceClamp,
      "bind" -> SpaceClamp,
      "mobility" -> SpaceClamp,
      "restrict" -> SpaceClamp,
      "weakpawnattack" -> WeaknessFixation,
      "weakness" -> WeaknessFixation,
      "fixation" -> WeaknessFixation,
      "minorityattack" -> WeaknessFixation,
      "blockade" -> WeaknessFixation,
      "target" -> WeaknessFixation,
      "pressure" -> WeaknessFixation,
      "isolat" -> WeaknessFixation,
      "backward" -> WeaknessFixation,
      "pawnbreakpreparation" -> PawnBreakPreparation,
      "centralcontrol" -> PawnBreakPreparation,
      "break" -> PawnBreakPreparation,
      "tension" -> PawnBreakPreparation,
      "exchange" -> FavorableExchange,
      "queentrade" -> FavorableExchange,
      "trade" -> FavorableExchange,
      "simplifydefense" -> FavorableExchange,
      "pawnstorm" -> FlankInfrastructure,
      "hook" -> FlankInfrastructure,
      "flank" -> FlankInfrastructure,
      "rooklift" -> FlankInfrastructure,
      "simplification" -> AdvantageTransformation,
      "conversion" -> AdvantageTransformation,
      "transform" -> AdvantageTransformation,
      "passedpawncreation" -> AdvantageTransformation,
      "passedpawnpush" -> AdvantageTransformation,
      "endgame" -> AdvantageTransformation,
      "technique" -> AdvantageTransformation,
      "decisive" -> AdvantageTransformation,
      "counterplay" -> ImmediateTacticalGain,
      "directmate" -> ImmediateTacticalGain,
      "perpetualcheck" -> ImmediateTacticalGain,
      "sacrifice" -> ImmediateTacticalGain,
      "tactic" -> ImmediateTacticalGain,
      "forcing" -> ImmediateTacticalGain,
      "kingsideattack" -> ImmediateTacticalGain,
      "queensideattack" -> ImmediateTacticalGain,
      "attack" -> ImmediateTacticalGain
    )

    private val structuralAliases: List[(String, ThemeL1)] = List(
      "entrenched" -> PieceRedeployment,
      "rook_pawn_march" -> FlankInfrastructure,
      "hook_creation" -> FlankInfrastructure,
      "color_complex_clamp" -> SpaceClamp,
      "generic_center_plan" -> PawnBreakPreparation
    )

    private val structuralSubplanAliases: List[(String, SubplanId)] = List(
      "entrenched" -> SubplanId.OutpostEntrenchment,
      "rook_pawn_march" -> SubplanId.RookPawnMarch,
      "hook_creation" -> SubplanId.HookCreation,
      "color_complex_clamp" -> SubplanId.FlankClamp,
      "generic_center_plan" -> SubplanId.CentralBreakTiming
    )

    private val seedAliases: List[(String, ThemeL1)] = List(
      "pawnstorm_kingside" -> FlankInfrastructure,
      "attack_the_hook" -> ImmediateTacticalGain,
      "minorityattack_queenside" -> WeaknessFixation,
      "canopener_h_pawn" -> ImmediateTacticalGain,
      "centralbreak_d" -> PawnBreakPreparation,
      "centralbreak_e" -> PawnBreakPreparation,
      "knightoutpost_route" -> PieceRedeployment,
      "rooklift_kingside" -> FlankInfrastructure,
      "badbishop_reroute" -> PieceRedeployment,
      "battery_formation" -> ImmediateTacticalGain,
      "createpassedpawn" -> AdvantageTransformation,
      "createiqp" -> WeaknessFixation,
      "fixbackwardpawn" -> WeaknessFixation,
      "prophylaxis_luft" -> RestrictionProphylaxis,
      "restrict_opponentpiece" -> RestrictionProphylaxis,
      "kingsafety_run" -> RestrictionProphylaxis,
      "trade_badbishop" -> FavorableExchange,
      "trade_queens_defensive" -> FavorableExchange,
      "simplify_to_endgame" -> AdvantageTransformation,
      "prepare_overload" -> ImmediateTacticalGain,
      "prepare_clearance" -> ImmediateTacticalGain,
      "openfile_doubling" -> PieceRedeployment,
      "space_bind" -> SpaceClamp,
      "central_bind" -> SpaceClamp,
      "squeeze" -> SpaceClamp
    )

    private val subplanAliases: List[(String, SubplanId)] =
      List(
        "opening_development" -> SubplanId.OpeningDevelopment,
        "openingdevelopment" -> SubplanId.OpeningDevelopment,
        "opening development" -> SubplanId.OpeningDevelopment,
        "prophylaxis_restraint" -> SubplanId.ProphylaxisRestraint,
        "break_prevention" -> SubplanId.BreakPrevention,
        "key_square_denial" -> SubplanId.KeySquareDenial,
        "outpost_entrenchment" -> SubplanId.OutpostEntrenchment,
        "worst_piece_improvement" -> SubplanId.WorstPieceImprovement,
        "rook_file_transfer" -> SubplanId.RookFileTransfer,
        "bishop_reanchor" -> SubplanId.BishopReanchor,
        "open_file_pressure" -> SubplanId.OpenFilePressure,
        "flank_clamp" -> SubplanId.FlankClamp,
        "central_space_bind" -> SubplanId.CentralSpaceBind,
        "mobility_suppression" -> SubplanId.MobilitySuppression,
        "static_weakness_fixation" -> SubplanId.StaticWeaknessFixation,
        "minority_attack_fixation" -> SubplanId.MinorityAttackFixation,
        "backward_pawn_targeting" -> SubplanId.BackwardPawnTargeting,
        "iqp_inducement" -> SubplanId.IQPInducement,
        "central_break_timing" -> SubplanId.CentralBreakTiming,
        "wing_break_timing" -> SubplanId.WingBreakTiming,
        "tension_maintenance" -> SubplanId.TensionMaintenance,
        "simplification_window" -> SubplanId.SimplificationWindow,
        "defender_trade" -> SubplanId.DefenderTrade,
        "queen_trade_shield" -> SubplanId.QueenTradeShield,
        "bad_piece_liquidation" -> SubplanId.BadPieceLiquidation,
        "rook_pawn_march" -> SubplanId.RookPawnMarch,
        "hook_creation" -> SubplanId.HookCreation,
        "rook_lift_scaffold" -> SubplanId.RookLiftScaffold,
        "simplification_conversion" -> SubplanId.SimplificationConversion,
        "passer_conversion" -> SubplanId.PasserConversion,
        "passed_pawn_manufacture" -> SubplanId.PassedPawnManufacture,
        "invasion_transition" -> SubplanId.InvasionTransition,
        "opposite_bishops_conversion" -> SubplanId.OppositeBishopsConversion,
        "forcing_tactical_shot" -> SubplanId.ForcingTacticalShot,
        "defender_overload" -> SubplanId.DefenderOverload,
        "clearance_break" -> SubplanId.ClearanceBreak,
        "battery_pressure" -> SubplanId.BatteryPressure,
        "badbishop_reroute" -> SubplanId.BishopReanchor,
        "openfile_doubling" -> SubplanId.OpenFilePressure,
        "battery_formation" -> SubplanId.BatteryPressure,
        "createpassedpawn" -> SubplanId.PassedPawnManufacture,
        "createiqp" -> SubplanId.IQPInducement,
        "trade_badbishop" -> SubplanId.BadPieceLiquidation,
        "trade_queens_defensive" -> SubplanId.QueenTradeShield,
        "outpost" -> SubplanId.OutpostEntrenchment,
        "entrenched" -> SubplanId.OutpostEntrenchment,
        "rookpawnmarch" -> SubplanId.RookPawnMarch,
        "rook_pawn" -> SubplanId.RookPawnMarch,
        "hook" -> SubplanId.HookCreation,
        "rooklift" -> SubplanId.RookLiftScaffold,
        "minorityattack" -> SubplanId.MinorityAttackFixation,
        "breakprep" -> SubplanId.CentralBreakTiming,
        "centralbreak" -> SubplanId.CentralBreakTiming,
        "wingbreak" -> SubplanId.WingBreakTiming,
        "simplify" -> SubplanId.SimplificationWindow,
        "conversion" -> SubplanId.SimplificationConversion,
        "passer" -> SubplanId.PasserConversion,
        "invasion" -> SubplanId.InvasionTransition,
        "opposite bishops" -> SubplanId.OppositeBishopsConversion,
        "opposite colored bishops" -> SubplanId.OppositeBishopsConversion,
        "forcing" -> SubplanId.ForcingTacticalShot,
        "overload" -> SubplanId.DefenderOverload,
        "clearance" -> SubplanId.ClearanceBreak
      ) ++ SubplanCatalog.specs.toList.flatMap { (sid, spec) =>
        spec.aliases.map(alias => normalize(alias) -> sid)
      }

    def sanitizeTheme(raw: String): Option[String] =
      ThemeL1.fromId(raw).filter(_ != Unknown).map(_.id)

    def fromPlanId(raw: String): ThemeL1 =
      byAlias(raw, planAliases).getOrElse(Unknown)

    def fromPlanName(raw: String): ThemeL1 =
      fromPlanId(raw)

    def fromStructuralState(raw: String): ThemeL1 =
      byAlias(raw, structuralAliases).getOrElse(Unknown)

    def subplanFromStructuralState(raw: String): Option[SubplanId] =
      byAlias(raw, structuralSubplanAliases)

    def fromSeedId(raw: String): ThemeL1 =
      byAlias(raw, seedAliases)
        .orElse(byAlias(raw, planAliases))
        .getOrElse(Unknown)

    def fromEvidenceSource(raw: String): ThemeL1 =
      val low = normalize(raw)
      if low.startsWith("theme:") then
        ThemeL1.fromId(low.stripPrefix("theme:")).getOrElse(Unknown)
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

    def fromSeed(seed: LatentSeed): ThemeL1 =
      val planTheme =
        seed.mapsToPlan
          .map(pid => fromPlanId(pid.toString))
          .filter(_ != Unknown)
      val familyTheme = seed.family match
        case SeedFamily.Pawn         => FlankInfrastructure
        case SeedFamily.Piece        => PieceRedeployment
        case SeedFamily.Structure    => WeaknessFixation
        case SeedFamily.Prophylaxis  => RestrictionProphylaxis
        case SeedFamily.Exchange     => FavorableExchange
        case SeedFamily.TacticalPrep => ImmediateTacticalGain
      val seedTheme = fromSeedId(seed.id)
      if seedTheme != Unknown then seedTheme
      else planTheme.getOrElse(familyTheme)

    def fromHypotheses(hypotheses: List[PlanHypothesis]): ThemeL1 =
      hypotheses.filter(_.viability.label != "refuted") match
        case Nil =>
          // Fallback: If everything is refuted (e.g. tactical blunder), use the top refuted theme to avoid excessive 'Unknown's
          hypotheses.headOption.map(fromHypothesis).filter(_ != ThemeL1.Unknown).getOrElse(ThemeL1.Unknown)
        case single :: Nil =>
          fromHypothesis(single)
        case multiple =>
          // Favor more structural/long-term themes over immediate tactical ones if viable
          val viableThemes = multiple.map(fromHypothesis).distinct
          ThemeL1.ranked.find(viableThemes.contains).getOrElse(ThemeL1.Unknown)

    def fromHypothesis(h: PlanHypothesis): ThemeL1 =
      ThemeL1
        .fromId(h.themeL1)
        .filter(_ != Unknown)
        .orElse {
          h.subplanId.flatMap(subplanFromId).map(_.theme)
        }
        .orElse {
          h.evidenceSources.iterator
            .map(fromEvidenceSource)
            .find(_ != Unknown)
        }
        .orElse {
          h.preconditions.iterator
            .flatMap(themeFromPrecondition)
            .map(fromEvidenceSource)
            .find(_ != Unknown)
        }
        .orElse {
          val fromId = fromPlanId(h.planId)
          Option.when(fromId != Unknown)(fromId)
        }
        .orElse {
          val fromName = fromPlanName(h.planName)
          Option.when(fromName != Unknown)(fromName)
        }
        .orElse {
          // Scan executionSteps and failureModes text for theme alias matches
          (h.executionSteps ++ h.failureModes).iterator
            .map(text => fromPlanId(text))
            .find(_ != Unknown)
        }
        .getOrElse(Unknown)

    def subplanFromId(raw: String): Option[SubplanId] =
      SubplanId.fromId(raw).orElse(byAlias(raw, subplanAliases))

    def subplanFromPlanId(raw: String): Option[SubplanId] =
      byAlias(raw, subplanAliases)
        .orElse(defaultSubplanForTheme(fromPlanId(raw)))

    def subplanFromPlanName(raw: String): Option[SubplanId] =
      subplanFromPlanId(raw)

    def subplanFromSeedId(raw: String): Option[SubplanId] =
      byAlias(raw, subplanAliases)
        .orElse(defaultSubplanForTheme(fromSeedId(raw)))

    def subplanFromSeed(seed: LatentSeed): Option[SubplanId] =
      subplanFromSeedId(seed.id)
        .orElse(seed.mapsToPlan.flatMap(pid => subplanFromPlanId(pid.toString)))
        .orElse(defaultSubplanForTheme(fromSeed(seed)))

    def subplanFromEvidenceSource(raw: String): Option[SubplanId] =
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

    def subplanFromHypothesis(h: PlanHypothesis): Option[SubplanId] =
      h.subplanId
        .flatMap(subplanFromId)
        .orElse(h.evidenceSources.iterator.flatMap(subplanFromEvidenceSource).toList.headOption)
        .orElse(subplanFromPlanId(h.planId))
        .orElse(subplanFromPlanName(h.planName))
        .orElse(defaultSubplanForTheme(fromHypothesis(h)))

    def defaultSubplanForTheme(theme: ThemeL1): Option[SubplanId] =
      theme match
        case OpeningPrinciples => Some(SubplanId.OpeningDevelopment)
        case RestrictionProphylaxis => Some(SubplanId.ProphylaxisRestraint)
        case PieceRedeployment => Some(SubplanId.WorstPieceImprovement)
        case SpaceClamp => Some(SubplanId.FlankClamp)
        case WeaknessFixation => Some(SubplanId.StaticWeaknessFixation)
        case PawnBreakPreparation => Some(SubplanId.CentralBreakTiming)
        case FavorableExchange => Some(SubplanId.SimplificationWindow)
        case FlankInfrastructure => Some(SubplanId.RookPawnMarch)
        case AdvantageTransformation => Some(SubplanId.SimplificationConversion)
        case ImmediateTacticalGain => Some(SubplanId.ForcingTacticalShot)
        case ThemeL1.Unknown => None

  private def themeFromPrecondition(raw: String): Option[String] =
    val low = normalize(raw)
    low.split("theme:").lift(1).map(_.trim).filter(_.nonEmpty).map(v => s"theme:$v")

  private def byAlias[T](raw: String, aliases: List[(String, T)]): Option[T] =
    val low = normalize(raw)
    aliases.collectFirst { case (needle, value) if low.contains(needle) => value }

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
