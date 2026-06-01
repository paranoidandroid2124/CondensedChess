package lila.commentary.analysis

import lila.commentary.model.authoring.{ LatentSeed, PlanHypothesis, SeedKind }

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
      objective: String,
      horizon: String,
      aliases: List[String] = Nil
  )

  object SubplanCatalog:
    val specs: Map[PlanKind, SubplanSpec] = Map(
      PlanKind.OpeningDevelopment -> SubplanSpec(
        requiredSignals = List("keyMotifs", "futureSnapshot"),
        objective = "complete development while securing king and center",
        horizon = "short",
        aliases = List("opening development", "opening principles")
      ),
      PlanKind.ProphylaxisRestraint -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "restrict opponent counterplay before expansion",
        horizon = "long",
        aliases = List("prophylaxis", "restriction")
      ),
      PlanKind.BreakPrevention -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "deny opponent central break timing",
        horizon = "medium",
        aliases = List("break prevention", "prevent break")
      ),
      PlanKind.KeySquareDenial -> SubplanSpec(
        requiredSignals = List("keyMotifs"),
        objective = "deny key entry squares and outposts",
        horizon = "medium",
        aliases = List("key-square denial")
      ),
      PlanKind.OutpostEntrenchment -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "stabilize a piece on an outpost",
        horizon = "long",
        aliases = List("outpost", "entrenched piece")
      ),
      PlanKind.WorstPieceImprovement -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        objective = "improve the worst piece without tactical collapse",
        horizon = "medium",
        aliases = List("piece improvement", "redeployment")
      ),
      PlanKind.RookFileTransfer -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "reroute rook to active file or rank",
        horizon = "medium",
        aliases = List("rook activation", "file transfer")
      ),
      PlanKind.BishopReanchor -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "reroute a passive bishop onto an active diagonal",
        horizon = "medium",
        aliases = List("bishop reanchor", "bad bishop reroute")
      ),
      PlanKind.OpenFilePressure -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "occupy and reinforce an open or semi-open file with heavy pieces",
        horizon = "medium",
        aliases = List("open file pressure", "open file doubling", "file pressure")
      ),
      PlanKind.FlankClamp -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "gain flank space while suppressing mobility",
        horizon = "long",
        aliases = List("clamp", "bind")
      ),
      PlanKind.CentralSpaceBind -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        objective = "hold central squares and limit breaks",
        horizon = "medium",
        aliases = List("space bind", "center bind")
      ),
      PlanKind.MobilitySuppression -> SubplanSpec(
        requiredSignals = List("keyMotifs"),
        objective = "reduce piece mobility and escape squares",
        horizon = "medium",
        aliases = List("restriction net")
      ),
      PlanKind.StaticWeaknessFixation -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "create a persistent static weakness",
        horizon = "long",
        aliases = List("fixation", "static target")
      ),
      PlanKind.MinorityAttackFixation -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "induce and attack a fixed weakness via minority attack",
        horizon = "long",
        aliases = List("minority attack")
      ),
      PlanKind.BackwardPawnTargeting -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        objective = "fix and pressure a backward pawn",
        horizon = "long",
        aliases = List("backward pawn", "weak pawn")
      ),
      PlanKind.IQPInducement -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "induce an isolated pawn and turn it into a fixed target",
        horizon = "medium",
        aliases = List("iqp inducement", "create iqp", "isolated pawn inducement")
      ),
      PlanKind.CentralBreakTiming -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "prepare a central break at the right timing",
        horizon = "medium",
        aliases = List("central break", "d-break", "e-break")
      ),
      PlanKind.WingBreakTiming -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "prepare a wing break after coordination",
        horizon = "medium",
        aliases = List("wing break")
      ),
      PlanKind.TensionMaintenance -> SubplanSpec(
        requiredSignals = List("replyPvs"),
        objective = "keep tension until conversion is favorable",
        horizon = "medium",
        aliases = List("maintain tension")
      ),
      PlanKind.SimplificationWindow -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta"),
        objective = "trade pieces only in favorable windows",
        horizon = "medium",
        aliases = List("favorable simplification")
      ),
      PlanKind.DefenderTrade -> SubplanSpec(
        requiredSignals = List("keyMotifs"),
        objective = "remove key defender to expose weaknesses",
        horizon = "short",
        aliases = List("remove defender")
      ),
      PlanKind.QueenTradeShield -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "neutralize attack via queen trade",
        horizon = "short",
        aliases = List("queen trade")
      ),
      PlanKind.BadPieceLiquidation -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "liquidate a bad piece while preserving the better structure",
        horizon = "short",
        aliases = List("bad piece liquidation", "trade bad bishop")
      ),
      PlanKind.RookPawnMarch -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "gain flank space with rook-pawn advance",
        horizon = "medium",
        aliases = List("rook-pawn march", "pawn storm")
      ),
      PlanKind.HookCreation -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "create a hook and open attacking files",
        horizon = "medium",
        aliases = List("hook")
      ),
      PlanKind.RookLiftScaffold -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "build rook-lift attack infrastructure",
        horizon = "medium",
        aliases = List("rook lift")
      ),
      PlanKind.SimplificationConversion -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "convert dynamic edge through simplification",
        horizon = "medium",
        aliases = List("conversion")
      ),
      PlanKind.PasserConversion -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "convert edge into passer dynamics",
        horizon = "long",
        aliases = List("passed pawn", "passer")
      ),
      PlanKind.PassedPawnManufacture -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot", "l1Delta"),
        objective = "manufacture a passed pawn by fixing structure and timing exchanges",
        horizon = "long",
        aliases = List("passed pawn manufacture", "create passed pawn")
      ),
      PlanKind.InvasionTransition -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot"),
        objective = "transform initiative into invasion/endgame edge",
        horizon = "medium",
        aliases = List("invasion", "transition")
      ),
      PlanKind.OppositeBishopsConversion -> SubplanSpec(
        requiredSignals = List("replyPvs", "futureSnapshot", "keyMotifs"),
        objective = "convert an opposite-colored bishops ending through color-complex penetration or passer transition",
        horizon = "long",
        aliases = List("opposite bishops conversion", "opposite-colored bishops conversion")
      ),
      PlanKind.ForcingTacticalShot -> SubplanSpec(
        requiredSignals = List("replyPvs", "l1Delta", "keyMotifs"),
        objective = "execute forcing tactical line with concrete gain",
        horizon = "short",
        aliases = List("forcing", "tactical shot")
      ),
      PlanKind.DefenderOverload -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "overload defender before breakthrough",
        horizon = "short",
        aliases = List("overload")
      ),
      PlanKind.ClearanceBreak -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs"),
        objective = "clear lines/squares for tactical execution",
        horizon = "short",
        aliases = List("clearance")
      ),
      PlanKind.BatteryPressure -> SubplanSpec(
        requiredSignals = List("replyPvs", "keyMotifs", "futureSnapshot"),
        objective = "align major pieces on a critical line before the breakthrough",
        horizon = "short",
        aliases = List("battery pressure", "battery formation")
      )
    )

    def byTheme(theme: PlanTheme): List[(PlanKind, SubplanSpec)] =
      specs.toList.filter((sid, _) => sid.theme == theme).sortBy(_._1.id)

  object ThemeResolver:
    import PlanTheme.*

    private val ThemeTagPrefix = "theme:"
    private val SubplanTagPrefix = "subplan:"
    private val StructuralStateTagPrefix = "structural_state:"
    private val LatentSeedTagPrefix = "latent_seed:"
    private val SeedTagPrefix = "seed:"
    private val SubplanAnnotationRegex = """(?i)\s*\[subplan:[^\]]+\]""".r

    def themeTag(theme: PlanTheme): String =
      themeTag(theme.id)

    def themeTag(themeId: String): String =
      s"$ThemeTagPrefix${canonicalTagId(themeId)}"

    def subplanTag(subplan: PlanKind): String =
      subplanTag(subplan.id)

    def subplanTag(subplanId: String): String =
      s"$SubplanTagPrefix${canonicalTagId(subplanId)}"

    def structuralStateTag(stateId: String): String =
      s"$StructuralStateTagPrefix${canonicalTagId(stateId)}"

    def latentSeedTag(seedId: String): String =
      s"$LatentSeedTagPrefix${canonicalTagId(seedId)}"

    def seedTag(seedId: String): String =
      s"$SeedTagPrefix${canonicalTagId(seedId)}"

    def themeIdFromSupport(raw: String): Option[String] =
      tagPayload(raw, ThemeTagPrefix)

    def subplanIdFromSupport(raw: String): Option[String] =
      tagPayload(raw, SubplanTagPrefix)

    def subplanFromSupport(raw: String): Option[PlanKind] =
      subplanIdFromSupport(raw).flatMap(subplanFromId)

    def structuralStateIdFromEvidenceSource(raw: String): Option[String] =
      tagPayload(raw, StructuralStateTagPrefix)

    def latentSeedIdFromEvidenceSource(raw: String): Option[String] =
      tagPayload(raw, LatentSeedTagPrefix)

    def seedIdFromEvidenceSource(raw: String): Option[String] =
      latentSeedIdFromEvidenceSource(raw).orElse(tagPayload(raw, SeedTagPrefix))

    def themeTagFromEmbeddedText(raw: String): Option[String] =
      val low = normalize(raw)
      val idx = low.indexOf(ThemeTagPrefix)
      if idx < 0 then None
      else
        Option(low.substring(idx + ThemeTagPrefix.length).trim)
          .filter(_.nonEmpty)
          .map(themeTag)

    def hasStructuralStateEvidence(sources: Iterable[String]): Boolean =
      sources.exists(structuralStateIdFromEvidenceSource(_).nonEmpty)

    def subplanAnnotation(label: String, subplan: PlanKind): String =
      s"${Option(label).getOrElse("").trim} [${subplanTag(subplan)}]".trim

    def subplanFromAnnotatedText(raw: String): Option[PlanKind] =
      val low = normalize(raw)
      val idx = low.indexOf(SubplanTagPrefix)
      if idx < 0 then None
      else
        val token =
          low
            .substring(idx + SubplanTagPrefix.length)
            .takeWhile(ch => ch.isLetterOrDigit || ch == '_' || ch == '-')
            .trim
        subplanFromId(token)

    def stripSubplanAnnotations(raw: String): String =
      SubplanAnnotationRegex.replaceAllIn(Option(raw).getOrElse(""), "")

    def hasSubplanAnnotation(raw: String): Boolean =
      SubplanAnnotationRegex.findFirstIn(Option(raw).getOrElse("")).nonEmpty

    def canonicalSupportTags(supports: Iterable[String]): List[String] =
      (
        supports.flatMap(themeIdFromSupport).map(themeTag) ++
          supports.flatMap(subplanIdFromSupport).map(subplanTag)
      ).toList.distinct

    private val planAliases: List[(String, PlanTheme)] = List(
      "openfile" -> PieceRedeployment,
      "oppositebishopsconversion" -> AdvantageTransformation,
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

    private val structuralAliases: List[(String, PlanTheme)] = List(
      "entrenched" -> PieceRedeployment,
      "rook_pawn_march" -> FlankInfrastructure,
      "hook_creation" -> FlankInfrastructure,
      "color_complex_clamp" -> SpaceClamp,
      "generic_center_plan" -> PawnBreakPreparation
    )

    private val structuralSubplanAliases: List[(String, PlanKind)] = List(
      "entrenched" -> PlanKind.OutpostEntrenchment,
      "rook_pawn_march" -> PlanKind.RookPawnMarch,
      "hook_creation" -> PlanKind.HookCreation,
      "color_complex_clamp" -> PlanKind.FlankClamp,
      "generic_center_plan" -> PlanKind.CentralBreakTiming
    )

    private val seedAliases: List[(String, PlanTheme)] = List(
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

    private val subplanAliases: List[(String, PlanKind)] =
      List(
        "opening_development" -> PlanKind.OpeningDevelopment,
        "openingdevelopment" -> PlanKind.OpeningDevelopment,
        "opening development" -> PlanKind.OpeningDevelopment,
        "prophylaxis_restraint" -> PlanKind.ProphylaxisRestraint,
        "break_prevention" -> PlanKind.BreakPrevention,
        "key_square_denial" -> PlanKind.KeySquareDenial,
        "outpost_entrenchment" -> PlanKind.OutpostEntrenchment,
        "worst_piece_improvement" -> PlanKind.WorstPieceImprovement,
        "rook_file_transfer" -> PlanKind.RookFileTransfer,
        "bishop_reanchor" -> PlanKind.BishopReanchor,
        "open_file_pressure" -> PlanKind.OpenFilePressure,
        "flank_clamp" -> PlanKind.FlankClamp,
        "central_space_bind" -> PlanKind.CentralSpaceBind,
        "mobility_suppression" -> PlanKind.MobilitySuppression,
        "static_weakness_fixation" -> PlanKind.StaticWeaknessFixation,
        "minority_attack_fixation" -> PlanKind.MinorityAttackFixation,
        "backward_pawn_targeting" -> PlanKind.BackwardPawnTargeting,
        "iqp_inducement" -> PlanKind.IQPInducement,
        "central_break_timing" -> PlanKind.CentralBreakTiming,
        "wing_break_timing" -> PlanKind.WingBreakTiming,
        "tension_maintenance" -> PlanKind.TensionMaintenance,
        "simplification_window" -> PlanKind.SimplificationWindow,
        "defender_trade" -> PlanKind.DefenderTrade,
        "queen_trade_shield" -> PlanKind.QueenTradeShield,
        "bad_piece_liquidation" -> PlanKind.BadPieceLiquidation,
        "rook_pawn_march" -> PlanKind.RookPawnMarch,
        "hook_creation" -> PlanKind.HookCreation,
        "rook_lift_scaffold" -> PlanKind.RookLiftScaffold,
        "simplification_conversion" -> PlanKind.SimplificationConversion,
        "passer_conversion" -> PlanKind.PasserConversion,
        "passed_pawn_manufacture" -> PlanKind.PassedPawnManufacture,
        "invasion_transition" -> PlanKind.InvasionTransition,
        "opposite_bishops_conversion" -> PlanKind.OppositeBishopsConversion,
        "forcing_tactical_shot" -> PlanKind.ForcingTacticalShot,
        "defender_overload" -> PlanKind.DefenderOverload,
        "clearance_break" -> PlanKind.ClearanceBreak,
        "battery_pressure" -> PlanKind.BatteryPressure,
        "badbishop_reroute" -> PlanKind.BishopReanchor,
        "openfile_doubling" -> PlanKind.OpenFilePressure,
        "battery_formation" -> PlanKind.BatteryPressure,
        "createpassedpawn" -> PlanKind.PassedPawnManufacture,
        "createiqp" -> PlanKind.IQPInducement,
        "trade_badbishop" -> PlanKind.BadPieceLiquidation,
        "trade_queens_defensive" -> PlanKind.QueenTradeShield,
        "outpost" -> PlanKind.OutpostEntrenchment,
        "entrenched" -> PlanKind.OutpostEntrenchment,
        "rookpawnmarch" -> PlanKind.RookPawnMarch,
        "rook_pawn" -> PlanKind.RookPawnMarch,
        "hook" -> PlanKind.HookCreation,
        "rooklift" -> PlanKind.RookLiftScaffold,
        "minorityattack" -> PlanKind.MinorityAttackFixation,
        "breakprep" -> PlanKind.CentralBreakTiming,
        "centralbreak" -> PlanKind.CentralBreakTiming,
        "wingbreak" -> PlanKind.WingBreakTiming,
        "simplify" -> PlanKind.SimplificationWindow,
        "conversion" -> PlanKind.SimplificationConversion,
        "passer" -> PlanKind.PasserConversion,
        "invasion" -> PlanKind.InvasionTransition,
        "opposite bishops" -> PlanKind.OppositeBishopsConversion,
        "opposite colored bishops" -> PlanKind.OppositeBishopsConversion,
        "forcing" -> PlanKind.ForcingTacticalShot,
        "overload" -> PlanKind.DefenderOverload,
        "clearance" -> PlanKind.ClearanceBreak
      ) ++ SubplanCatalog.specs.toList.flatMap { (sid, spec) =>
        spec.aliases.map(alias => normalize(alias) -> sid)
      }

    def sanitizeTheme(raw: String): Option[String] =
      PlanTheme.fromId(raw).filter(_ != Unknown).map(_.id)

    def fromPlanId(raw: String): PlanTheme =
      byAlias(raw, planAliases).getOrElse(Unknown)

    def fromPlanName(raw: String): PlanTheme =
      fromPlanId(raw)

    def fromStructuralState(raw: String): PlanTheme =
      byAlias(raw, structuralAliases).getOrElse(Unknown)

    def subplanFromStructuralState(raw: String): Option[PlanKind] =
      byAlias(raw, structuralSubplanAliases)

    def fromSeedId(raw: String): PlanTheme =
      byAlias(raw, seedAliases)
        .orElse(byAlias(raw, planAliases))
        .getOrElse(Unknown)

    def fromEvidenceSource(raw: String): PlanTheme =
      val low = normalize(raw)
      if low.startsWith(ThemeTagPrefix) then
        themeIdFromSupport(low).flatMap(PlanTheme.fromId).getOrElse(Unknown)
      else if low.startsWith(SubplanTagPrefix) then
        subplanFromSupport(low).map(_.theme).getOrElse(Unknown)
      else if low.startsWith(StructuralStateTagPrefix) then
        structuralStateIdFromEvidenceSource(low).map(fromStructuralState).getOrElse(Unknown)
      else if low.startsWith(LatentSeedTagPrefix) || low.startsWith(SeedTagPrefix) then
        seedIdFromEvidenceSource(low).map(fromSeedId).getOrElse(Unknown)
      else
        fromPlanId(low)

    def fromSeed(seed: LatentSeed): PlanTheme =
      val planTheme =
        seed.mapsToPlan
          .map(pid => fromPlanId(pid.toString))
          .filter(_ != Unknown)
      val seedKindTheme = seed.seedKind match
        case SeedKind.Pawn         => FlankInfrastructure
        case SeedKind.Piece        => PieceRedeployment
        case SeedKind.Structure    => WeaknessFixation
        case SeedKind.Prophylaxis  => RestrictionProphylaxis
        case SeedKind.Exchange     => FavorableExchange
        case SeedKind.TacticalPrep => ImmediateTacticalGain
      val seedIdTheme = fromSeedId(seed.id)
      if seedIdTheme != Unknown then seedIdTheme
      else planTheme.getOrElse(seedKindTheme)

    def fromHypotheses(hypotheses: List[PlanHypothesis]): PlanTheme =
      hypotheses.filter(_.viability.label != "refuted") match
        case Nil =>
          // Fallback: If everything is refuted (e.g. tactical blunder), use the top refuted theme to avoid excessive 'Unknown's
          hypotheses.headOption.map(fromHypothesis).filter(_ != PlanTheme.Unknown).getOrElse(PlanTheme.Unknown)
        case single :: Nil =>
          fromHypothesis(single)
        case multiple =>
          // Favor more structural/long-term themes over immediate tactical ones if viable
          val viableThemes = multiple.map(fromHypothesis).distinct
          PlanTheme.ranked.find(viableThemes.contains).getOrElse(PlanTheme.Unknown)

    def fromHypothesis(h: PlanHypothesis): PlanTheme =
      PlanTheme
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

    def subplanFromId(raw: String): Option[PlanKind] =
      PlanKind.fromId(raw).orElse(byAlias(raw, subplanAliases))

    def subplanFromPlanId(raw: String): Option[PlanKind] =
      byAlias(raw, subplanAliases)
        .orElse(defaultSubplanForTheme(fromPlanId(raw)))

    def subplanFromPlanName(raw: String): Option[PlanKind] =
      subplanFromPlanId(raw)

    def subplanFromSeedId(raw: String): Option[PlanKind] =
      byAlias(raw, subplanAliases)
        .orElse(defaultSubplanForTheme(fromSeedId(raw)))

    def subplanFromSeed(seed: LatentSeed): Option[PlanKind] =
      subplanFromSeedId(seed.id)
        .orElse(seed.mapsToPlan.flatMap(pid => subplanFromPlanId(pid.toString)))
        .orElse(defaultSubplanForTheme(fromSeed(seed)))

    def subplanFromEvidenceSource(raw: String): Option[PlanKind] =
      val low = normalize(raw)
      if low.startsWith(SubplanTagPrefix) then
        subplanFromSupport(low)
      else if low.startsWith(StructuralStateTagPrefix) then
        structuralStateIdFromEvidenceSource(low).flatMap(subplanFromStructuralState)
      else if low.startsWith(LatentSeedTagPrefix) || low.startsWith(SeedTagPrefix) then
        seedIdFromEvidenceSource(low).flatMap(subplanFromSeedId)
      else subplanFromPlanId(low)

    private def tagPayload(raw: String, prefix: String): Option[String] =
      val low = normalize(raw)
      Option.when(low.startsWith(prefix))(low.stripPrefix(prefix).trim).filter(_.nonEmpty)

    private def canonicalTagId(raw: String): String =
      normalize(raw).replace('-', '_').replaceAll("\\s+", "_")

    def subplanFromHypothesis(h: PlanHypothesis): Option[PlanKind] =
      h.subplanId
        .flatMap(subplanFromId)
        .orElse(h.evidenceSources.iterator.flatMap(subplanFromEvidenceSource).toList.headOption)
        .orElse(subplanFromPlanId(h.planId))
        .orElse(subplanFromPlanName(h.planName))
        .orElse(defaultSubplanForTheme(fromHypothesis(h)))

    def defaultSubplanForTheme(theme: PlanTheme): Option[PlanKind] =
      theme match
        case OpeningPrinciples => Some(PlanKind.OpeningDevelopment)
        case RestrictionProphylaxis => Some(PlanKind.ProphylaxisRestraint)
        case PieceRedeployment => Some(PlanKind.WorstPieceImprovement)
        case SpaceClamp => Some(PlanKind.FlankClamp)
        case WeaknessFixation => Some(PlanKind.StaticWeaknessFixation)
        case PawnBreakPreparation => Some(PlanKind.CentralBreakTiming)
        case FavorableExchange => Some(PlanKind.SimplificationWindow)
        case FlankInfrastructure => Some(PlanKind.RookPawnMarch)
        case AdvantageTransformation => Some(PlanKind.SimplificationConversion)
        case ImmediateTacticalGain => Some(PlanKind.ForcingTacticalShot)
        case PlanTheme.Unknown => None

  private def themeFromPrecondition(raw: String): Option[String] =
    ThemeResolver.themeTagFromEmbeddedText(raw)

  private def strip_text_noise(s: String): String =
    Option(s).getOrElse("").trim.toLowerCase.replace("_", "").replace(" ", "").replace("-", "")

  private def byAlias[T](raw: String, aliases: List[(String, T)]): Option[T] =
    val simplifiedRaw = strip_text_noise(raw)
    aliases.collectFirst { case (needle, value) if simplifiedRaw.contains(strip_text_noise(needle)) => value }

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
