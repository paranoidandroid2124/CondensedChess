package lila.commentary.analysis.semantic

import lila.commentary.analysis.PlanTaxonomy

private[commentary] object StrategicObservationIds:

  enum SemanticObservationId(val wireKey: String):
    case MinorityAttackSemantic extends SemanticObservationId("minority_attack_semantic")
    case TargetPressureSemantic extends SemanticObservationId("target_pressure_semantic")

  object SemanticObservationId:
    private val byWireKey: Map[String, SemanticObservationId] =
      SemanticObservationId.values.map(id => normalize(id.wireKey) -> id).toMap

    def fromWireKey(raw: String): Option[SemanticObservationId] =
      byWireKey.get(normalize(raw))

  final case class EvidenceSourceId private (wireKey: String)

  object EvidenceSourceId:
    private def register(wireKey: String): EvidenceSourceId =
      EvidenceSourceId(wireKey)

    val MinorityAttackSemantic = register(SemanticObservationId.MinorityAttackSemantic.wireKey)

    // Pawn breaks and structural space.
    val PawnAnalysisBreakReady = register("pawn_analysis_break_ready")
    val PawnAnalysisTension = register("pawn_analysis_tension")
    val PawnAnalysisBreakRace = register("pawn_analysis_break_race")
    val PawnPlayBreakReady = register("pawn_play_break_ready")
    val CentralBreakTension = register("central_break_tension")
    val FileOpeningConsequence = register("file_opening_consequence")
    val PlanMatchBreakPreparation = register("plan_match_break_preparation")
    val FrenchCounterbreakProfile = register("french_counterbreak_profile")
    val FrenchF6BreakSeed = register("french_f6_break_seed")
    val SpaceAdvantageTag = register("space_advantage_tag")
    val ColorComplexClamp = register("color_complex_clamp")
    val CentralSpaceEdge = register("central_space_edge")
    val MobilityRestriction = register("mobility_restriction")
    val LockedCenterBind = register("locked_center_bind")
    val PlanAlignmentSpaceRace = register("plan_alignment_space_race")
    val PlanMatchSpaceAdvantage = register("plan_match_space_advantage")
    val MaroczyBindProfile = register("maroczy_bind_profile")
    val IqpSpaceBridge = register("iqp_space_bridge")
    val IqpCentralPresence = register("iqp_central_presence")

    // Target fixing and weakness pressure.
    val EnemyWeakSquare = register("enemy_weak_square")
    val ColorComplexWeakness = register("color_complex_weakness")
    val MinorityAttackSupport = register("minority_attack_support")
    val WeakComplexFixation = register("weak_complex_fixation")
    val PlanMatchTargetFixing = register("plan_match_target_fixing")
    val DirectionalTargetFixation = register("directional_target_fixation")
    val CompensationTargetFixation = register("compensation_target_fixation")
    val CarlsbadFixationProfile = register("carlsbad_fixation_profile")

    // Lines and access.
    val OpenFileControl = register("open_file_control")
    val DoubledRooks = register("doubled_rooks")
    val ConnectedRooks = register("connected_rooks")
    val RookOnSeventh = register("rook_on_seventh")
    val OccupiedLineControl = register("occupied_line_control")
    val RouteLineAccess = register("route_line_access")
    val DirectionalLineAccess = register("directional_line_access")
    val LineControlFeatures = register("line_control_features")
    val CompensationOpenLines = register("compensation_open_lines")
    val DelayedRecoveryWindow = register("delayed_recovery_window")
    val PlanMatchLineOccupation = register("plan_match_line_occupation")

    // Outposts and minor-piece imbalance.
    val OutpostTag = register("outpost_tag")
    val StrongKnight = register("strong_knight")
    val EntrenchedPieceState = register("entrenched_piece_state")
    val RouteOutpostAccess = register("route_outpost_access")
    val DirectionalOutpostAccess = register("directional_outpost_access")
    val BishopPairAdvantage = register("bishop_pair_advantage")
    val EnemyBadBishop = register("enemy_bad_bishop")
    val GoodBishop = register("good_bishop")
    val OppositeColorBishops = register("opposite_color_bishops")
    val StrongKnightVsBadBishop = register("strong_knight_vs_bad_bishop")
    val PieceActivityBadBishop = register("piece_activity_bad_bishop")
    val MinorPieceCountImbalance = register("minor_piece_count_imbalance")
    val FrenchMinorPieceProfile = register("french_minor_piece_profile")

    // Prophylaxis and attack.
    val PreventedPlan = register("prevented_plan")
    val ThreatAnalysisProphylaxis = register("threat_analysis_prophylaxis")
    val OpponentCounterbreakWatch = register("opponent_counterbreak_watch")
    val PlanMatchProphylaxis = register("plan_match_prophylaxis")
    val BishopPinWatch = register("bishop_pin_watch")
    val QueensideCounterbreakWatch = register("queenside_counterbreak_watch")
    val MateNet = register("mate_net")
    val EnemyKingStuckCenter = register("enemy_king_stuck_center")
    val EnemyWeakBackRank = register("enemy_weak_back_rank")
    val KingRingPressure = register("king_ring_pressure")
    val FlankPawnPressure = register("flank_pawn_pressure")
    val AttackingThreatAnalysis = register("attacking_threat_analysis")
    val MotifRookLift = register("motif_rook_lift")
    val MotifBattery = register("motif_battery")
    val MotifPieceLift = register("motif_piece_lift")
    val MotifCheckPressure = register("motif_check_pressure")
    val RouteAttackLane = register("route_attack_lane")
    val DirectionalAttackLane = register("directional_attack_lane")
    val CompensationDevelopmentLead = register("compensation_development_lead")
    val CompensationKingWindow = register("compensation_king_window")
    val CompensationDiagonalBattery = register("compensation_diagonal_battery")
    val PlanMatchKingAttack = register("plan_match_king_attack")
    val OppositeSideStorm = register("opposite_side_storm")
    val FianchettoAssaultProfile = register("fianchetto_assault_profile")

    // Transformation and counterplay.
    val RemovingTheDefender = register("removing_the_defender")
    val WinningEndgameTransition = register("winning_endgame_transition")
    val ClassificationTransformationWindow = register("classification_transformation_window")
    val ExchangeAvailabilityBridge = register("exchange_availability_bridge")
    val CaptureExchangeTransformation = register("capture_exchange_transformation")
    val PlanMatchTransformation = register("plan_match_transformation")
    val IqpSimplificationProfile = register("iqp_simplification_profile")
    val CounterplaySuppression = register("counterplay_suppression")
    val OpponentCounterbreakDenial = register("opponent_counterbreak_denial")
    val ThreatAnalysisCounterplay = register("threat_analysis_counterplay")
    val HedgehogContainmentProfile = register("hedgehog_containment_profile")
    val HedgehogBreakDenialGeometry = register("hedgehog_break_denial_geometry")
    val MaroczyCounterplaySuppression = register("maroczy_counterplay_suppression")
    val MaroczyBreakDenialGeometry = register("maroczy_break_denial_geometry")
    val PlanMatchCounterplaySuppression = register("plan_match_counterplay_suppression")
    val CompensationCounterplayDenial = register("compensation_counterplay_denial")

    val all: List[EvidenceSourceId] =
      List(
        MinorityAttackSemantic,
        PawnAnalysisBreakReady,
        PawnAnalysisTension,
        PawnAnalysisBreakRace,
        PawnPlayBreakReady,
        CentralBreakTension,
        FileOpeningConsequence,
        PlanMatchBreakPreparation,
        FrenchCounterbreakProfile,
        FrenchF6BreakSeed,
        SpaceAdvantageTag,
        ColorComplexClamp,
        CentralSpaceEdge,
        MobilityRestriction,
        LockedCenterBind,
        PlanAlignmentSpaceRace,
        PlanMatchSpaceAdvantage,
        MaroczyBindProfile,
        IqpSpaceBridge,
        IqpCentralPresence,
        EnemyWeakSquare,
        ColorComplexWeakness,
        MinorityAttackSupport,
        WeakComplexFixation,
        PlanMatchTargetFixing,
        DirectionalTargetFixation,
        CompensationTargetFixation,
        CarlsbadFixationProfile,
        OpenFileControl,
        DoubledRooks,
        ConnectedRooks,
        RookOnSeventh,
        OccupiedLineControl,
        RouteLineAccess,
        DirectionalLineAccess,
        LineControlFeatures,
        CompensationOpenLines,
        DelayedRecoveryWindow,
        PlanMatchLineOccupation,
        OutpostTag,
        StrongKnight,
        EntrenchedPieceState,
        RouteOutpostAccess,
        DirectionalOutpostAccess,
        BishopPairAdvantage,
        EnemyBadBishop,
        GoodBishop,
        OppositeColorBishops,
        StrongKnightVsBadBishop,
        PieceActivityBadBishop,
        MinorPieceCountImbalance,
        FrenchMinorPieceProfile,
        PreventedPlan,
        ThreatAnalysisProphylaxis,
        OpponentCounterbreakWatch,
        PlanMatchProphylaxis,
        BishopPinWatch,
        QueensideCounterbreakWatch,
        MateNet,
        EnemyKingStuckCenter,
        EnemyWeakBackRank,
        KingRingPressure,
        FlankPawnPressure,
        AttackingThreatAnalysis,
        MotifRookLift,
        MotifBattery,
        MotifPieceLift,
        MotifCheckPressure,
        RouteAttackLane,
        DirectionalAttackLane,
        CompensationDevelopmentLead,
        CompensationKingWindow,
        CompensationDiagonalBattery,
        PlanMatchKingAttack,
        OppositeSideStorm,
        FianchettoAssaultProfile,
        RemovingTheDefender,
        WinningEndgameTransition,
        ClassificationTransformationWindow,
        ExchangeAvailabilityBridge,
        CaptureExchangeTransformation,
        PlanMatchTransformation,
        IqpSimplificationProfile,
        CounterplaySuppression,
        OpponentCounterbreakDenial,
        ThreatAnalysisCounterplay,
        HedgehogContainmentProfile,
        HedgehogBreakDenialGeometry,
        MaroczyCounterplaySuppression,
        MaroczyBreakDenialGeometry,
        PlanMatchCounterplaySuppression,
        CompensationCounterplayDenial
      )

    private val byWireKey: Map[String, EvidenceSourceId] =
      all.map(id => normalize(id.wireKey) -> id).toMap

    def fromWireKey(raw: String): Option[EvidenceSourceId] =
      byWireKey.get(normalize(raw))

  final case class ProofSourceId private (wireKey: String)

  object ProofSourceId:
    private def register(wireKey: String): ProofSourceId =
      ProofSourceId(wireKey)

    val ExactTargetFixation = register("exact_target_fixation")
    val CarlsbadFixedTargetProbe = register("carlsbad_fixed_target_probe")
    val IQPInducementProbe = register("iqp_inducement_probe")
    val TargetFocusedCoordinationProbe = register("target_focused_coordination_probe")
    val ColorComplexSqueezeProbe = register("color_complex_squeeze_probe")

    val LocalFileEntryBind = register("local_file_entry_bind")
    val CounterplayAxisSuppression = register("counterplay_axis_suppression")
    val ProphylacticMove = register("prophylactic_move")
    val ExchangeForcingDelta = register("exchange_forcing_delta")
    val ActiveMoveDelta = register("active_move_delta")

    val NewAccessDelta = register("new_access_delta")
    val PressureIncreaseDelta = register("pressure_increase_delta")
    val CounterplayReductionDelta = register("counterplay_reduction_delta")
    val ResourceRemovalDelta = register("resource_removal_delta")
    val PlanAdvanceDelta = register("plan_advance_delta")

    val all: List[ProofSourceId] =
      List(
        ExactTargetFixation,
        CarlsbadFixedTargetProbe,
        IQPInducementProbe,
        TargetFocusedCoordinationProbe,
        ColorComplexSqueezeProbe,
        LocalFileEntryBind,
        CounterplayAxisSuppression,
        ProphylacticMove,
        ExchangeForcingDelta,
        ActiveMoveDelta,
        NewAccessDelta,
        PressureIncreaseDelta,
        CounterplayReductionDelta,
        ResourceRemovalDelta,
        PlanAdvanceDelta
      )

    private val byWireKey: Map[String, ProofSourceId] =
      all.map(id => normalize(id.wireKey) -> id).toMap

    def fromWireKey(raw: String): Option[ProofSourceId] =
      byWireKey.get(normalize(raw))

  final case class ProofFamilyId private (wireKey: String)

  object ProofFamilyId:
    private def register(wireKey: String): ProofFamilyId =
      ProofFamilyId(wireKey)

    private val themeFamilies: List[ProofFamilyId] =
      PlanTaxonomy.PlanTheme.ranked.map(theme => register(theme.id))

    private val planKindFamilies: List[ProofFamilyId] =
      PlanTaxonomy.PlanKind.values.toList.map(kind => register(kind.id))

    val HalfOpenFilePressure = register("half_open_file_pressure")
    val NeutralizeKeyBreak = register("neutralize_key_break")
    val CounterplayRestraint = register("counterplay_restraint")
    val TradeKeyDefender = register("trade_key_defender")
    val TargetFocusedCoordination = register("target_focused_coordination")
    val ColorComplexSqueeze = register("color_complex_squeeze")

    val NewAccess = register("new_access")
    val PressureIncrease = register("pressure_increase")
    val ExchangeForcing = register("exchange_forcing")
    val CounterplayReduction = register("counterplay_reduction")
    val ResourceRemoval = register("resource_removal")
    val PlanAdvance = register("plan_advance")
    val KingSafety = register("king_safety")
    val TechnicalConversion = register("technical_conversion")
    val PieceImprovement = register("piece_improvement")

    val all: List[ProofFamilyId] =
      (themeFamilies ++ planKindFamilies ++
        List(
          HalfOpenFilePressure,
          NeutralizeKeyBreak,
          CounterplayRestraint,
          TradeKeyDefender,
          TargetFocusedCoordination,
          ColorComplexSqueeze,
          NewAccess,
          PressureIncrease,
          ExchangeForcing,
          CounterplayReduction,
          ResourceRemoval,
          PlanAdvance,
          KingSafety,
          TechnicalConversion,
          PieceImprovement
        )).distinct

    private val byWireKey: Map[String, ProofFamilyId] =
      all.map(id => normalize(id.wireKey) -> id).toMap

    val BackwardPawnTargeting: ProofFamilyId =
      fromPlanKind(PlanTaxonomy.PlanKind.BackwardPawnTargeting).get

    def fromPlanKind(kind: PlanTaxonomy.PlanKind): Option[ProofFamilyId] =
      byWireKey.get(normalize(kind.id))

    def fromPlanTheme(theme: PlanTaxonomy.PlanTheme): Option[ProofFamilyId] =
      byWireKey.get(normalize(theme.id))

    def fromWireKey(raw: String): Option[ProofFamilyId] =
      byWireKey.get(normalize(raw))

  final case class FactId private (wireKey: String)

  object FactId:
    def semantic(id: SemanticObservationId): FactId =
      FactId(id.wireKey)

    def dynamic(raw: String): Option[FactId] =
      val key = raw.trim
      Option.when(
        key.nonEmpty &&
          !key.startsWith("source:") &&
          EvidenceSourceId.fromWireKey(key).isEmpty &&
          ProofSourceId.fromWireKey(key).isEmpty &&
          ProofFamilyId.fromWireKey(key).isEmpty
      )(FactId(key))

  enum EvidenceRef:
    case Source(id: EvidenceSourceId)
    case Fact(id: FactId)

    def wireKey: String =
      this match
        case Source(id) => s"source:${id.wireKey}"
        case Fact(id)   => id.wireKey

  private def normalize(raw: String): String =
    raw.trim.toLowerCase
