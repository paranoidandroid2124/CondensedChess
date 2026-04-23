package lila.commentary.projection

object StrategyProjectionCoverageContract:

  final case class CoverageGate(axis: String, buckets: Vector[String]):
    require(axis.matches("^[a-z][a-z0-9_]*$"), s"Invalid projection coverage axis $axis")
    require(buckets.nonEmpty, s"Projection coverage gate $axis must declare buckets")
    require(
      buckets.forall(_.matches("^[a-z][a-z0-9_]*$")),
      s"Invalid projection coverage buckets for $axis: ${buckets.mkString(", ")}"
    )

    def pairs: Set[(String, String)] =
      buckets.map(axis -> _).toSet

  val foundationBroadReadyCoverageBandIds: Vector[StrategyProjectionBandId] =
    Vector("S06", "S09", "S10", "S12", "S20", "S23", "S25").map(StrategyProjectionBandId.apply)

  val broadCoverageCandidateBandIds: Vector[StrategyProjectionBandId] =
    Vector("S17", "S18", "S19", "S22", "S24").map(StrategyProjectionBandId.apply)

  val initiativeReleaseCoverageFreezeBandIds: Vector[StrategyProjectionBandId] =
    Vector("S05", "S07", "S08", "S21").map(StrategyProjectionBandId.apply)

  val kingAttackCoverageFreezeBandIds: Vector[StrategyProjectionBandId] =
    Vector("S01", "S02", "S03", "S04").map(StrategyProjectionBandId.apply)

  val pawnTargetStructuralDamageCoverageFreezeBandIds: Vector[StrategyProjectionBandId] =
    Vector("S11", "S13", "S14").map(StrategyProjectionBandId.apply)

  val passerCreationSuppressionCoverageFreezeBandIds: Vector[StrategyProjectionBandId] =
    Vector("S15", "S16").map(StrategyProjectionBandId.apply)

  val globalClosureBroadReadyCoverageBandIds: Vector[StrategyProjectionBandId] =
    (1 to 25).map(index => StrategyProjectionBandId(f"S$index%02d")).toVector

  val frozenCoverageBandIds: Vector[StrategyProjectionBandId] =
    foundationBroadReadyCoverageBandIds ++
      broadCoverageCandidateBandIds ++
      initiativeReleaseCoverageFreezeBandIds ++
      kingAttackCoverageFreezeBandIds ++
      pawnTargetStructuralDamageCoverageFreezeBandIds ++
      passerCreationSuppressionCoverageFreezeBandIds

  val coverageGatesByBand: Map[String, Vector[CoverageGate]] = Map(
    "S01" -> Vector(
      CoverageGate("storm_route", Vector("same_wing_contact", "attack_edge_same_king")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s05", "vs_s21")),
      CoverageGate("shortcut_negative", Vector("castling_side_only", "wing_shell_only"))
    ),
    "S02" -> Vector(
      CoverageGate(
        "attack_concentration_route",
        Vector("direct_piece_concentration", "lane_strengthened_concentration")
      ),
      CoverageGate("same_cluster_near_miss", Vector("vs_s03", "vs_s04", "vs_s09")),
      CoverageGate("shortcut_negative", Vector("king_attack_label_only", "battery_or_lift_only"))
    ),
    "S03" -> Vector(
      CoverageGate("diagonal_attack_route", Vector("king_facing_diagonal_entry", "fragility_linked_diagonal")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s02", "vs_s12")),
      CoverageGate("shortcut_negative", Vector("bishop_pair_only", "non_king_diagonal_pressure"))
    ),
    "S04" -> Vector(
      CoverageGate("shelter_breach_route", Vector("shell_payload_breach", "support_break_breach")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s01", "vs_s02", "vs_s03", "vs_s24")),
      CoverageGate("shortcut_negative", Vector("king_shelter_wording_only", "generic_attack_pressure"))
    ),
    "S05" -> Vector(
      CoverageGate("center_release_route", Vector("center_pawn_target", "central_axis_continuation")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s06", "vs_s14", "vs_s21")),
      CoverageGate("shortcut_negative", Vector("open_center_wording_only", "center_shell_only"))
    ),
    "S06" -> Vector(
      CoverageGate("restriction_route", Vector("outpost_anchor", "non_outpost_space_bind")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s05", "vs_s20")),
      CoverageGate("shortcut_negative", Vector("space_wording_only", "mobility_comparison_only"))
    ),
    "S07" -> Vector(
      CoverageGate("initiative_conversion_route", Vector("development_led_window", "move_right_window")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s08")),
      CoverageGate("shortcut_negative", Vector("development_lead_only", "initiative_wording_only"))
    ),
    "S08" -> Vector(
      CoverageGate(
        "denial_route",
        Vector("rival_break_source_suppressed", "rival_counterplay_source_suppressed")
      ),
      CoverageGate("same_cluster_near_miss", Vector("vs_s07", "vs_s20", "vs_s21")),
      CoverageGate("shortcut_negative", Vector("no_counterplay_wording_only", "initiative_window_only"))
    ),
    "S09" -> Vector(
      CoverageGate(
        "penetration_route",
        Vector("open_file_entry", "semi_open_file_entry", "same_file_penetration")
      ),
      CoverageGate("same_cluster_near_miss", Vector("vs_s02", "vs_s23", "vs_s25")),
      CoverageGate("shortcut_negative", Vector("file_substrate_only", "rook_on_file_only"))
    ),
    "S10" -> Vector(
      CoverageGate("occupancy_scope", Vector("knight_only")),
      CoverageGate("durability_route", Vector("same_anchor_eviction_denial")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s12")),
      CoverageGate(
        "shortcut_negative",
        Vector("good_piece_wording_only", "occupancy_without_durability", "non_knight_occupancy_without_freeze")
      )
    ),
    "S21" -> Vector(
      CoverageGate("counterplay_survival_route", Vector("center_source_survives", "far_wing_source_survives")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s01", "vs_s05", "vs_s08")),
      CoverageGate("shortcut_negative", Vector("break_source_only", "deferred_initiative_only"))
    ),
    "S11" -> Vector(
      CoverageGate("target_pressure_route", Vector("same_target_fixation", "same_target_repeated_pressure")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s13", "vs_s14")),
      CoverageGate("shortcut_negative", Vector("weak_pawn_target_only", "target_swap_by_prose"))
    ),
    "S12" -> Vector(
      CoverageGate("access_route", Vector("weak_square_route", "diagonal_lane_route")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s03", "vs_s10", "vs_s20")),
      CoverageGate("shortcut_negative", Vector("weak_square_wording_only", "diagonal_wording_only"))
    ),
    "S13" -> Vector(
      CoverageGate("wing_damage_route", Vector("phalanx_edge_target", "structurally_burdened_target")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s11", "vs_s14", "vs_s15")),
      CoverageGate("shortcut_negative", Vector("sector_asymmetry_only", "preexisting_weak_pawn_only"))
    ),
    "S14" -> Vector(
      CoverageGate("chain_base_route", Vector("chain_base_target", "base_contact_continuation")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s05", "vs_s11", "vs_s13", "vs_s15")),
      CoverageGate("shortcut_negative", Vector("fixed_chain_only", "generic_structural_damage"))
    ),
    "S15" -> Vector(
      CoverageGate("creation_route", Vector("s13_wing_damage", "s14_chain_base")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s13", "vs_s14", "vs_s16")),
      CoverageGate(
        "shortcut_negative",
        Vector(
          "candidate_passer_only",
          "create_passer_shell_only",
          "existing_passer_entity_only",
          "split_anchor_creation_route"
        )
      )
    ),
    "S16" -> Vector(
      CoverageGate("suppression_route", Vector("blockade_hold", "restriction_hold", "non_losing_race")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s15", "vs_s22", "vs_s23")),
      CoverageGate("shortcut_negative", Vector("enemy_passer_presence_only", "blocker_picture_only"))
    ),
    "S17" -> Vector(
      CoverageGate("liability_relief_route", Vector("repair_route", "exchange_relief")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s18", "vs_s20")),
      CoverageGate("shortcut_negative", Vector("bad_piece_wording_only", "generic_improving_move"))
    ),
    "S18" -> Vector(
      CoverageGate(
        "minor_edge_conversion_route",
        Vector("bishop_pair_to_initiative", "bishop_pair_to_structure", "bishop_pair_to_material")
      ),
      CoverageGate("same_cluster_near_miss", Vector("vs_s12", "vs_s17", "vs_s20")),
      CoverageGate("shortcut_negative", Vector("bishop_pair_state_only", "minor_edge_label_only"))
    ),
    "S19" -> Vector(
      CoverageGate(
        "simplification_route",
        Vector("trade_invariant_to_hold", "trade_invariant_to_material")
      ),
      CoverageGate("same_cluster_near_miss", Vector("vs_s22", "vs_s24")),
      CoverageGate("shortcut_negative", Vector("trade_wording_only", "material_reduction_only"))
    ),
    "S20" -> Vector(
      CoverageGate("domination_route", Vector("mobility_plus_restriction", "defender_starvation")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s06", "vs_s12", "vs_s17")),
      CoverageGate("shortcut_negative", Vector("mobility_edge_only", "restriction_without_comparison"))
    ),
    "S22" -> Vector(
      CoverageGate("hold_route", Vector("fortress_draw_hold", "perpetual_hold")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s19", "vs_s23")),
      CoverageGate(
        "shortcut_negative",
        Vector("draw_wording_only", "fortress_shell_only", "checking_sequence_wording", "perpetual_deferred")
      )
    ),
    "S23" -> Vector(
      CoverageGate("king_activity_route", Vector("same_entry_route", "same_contact_opposition")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s09", "vs_s16", "vs_s22")),
      CoverageGate("shortcut_negative", Vector("centralization_only", "king_proximity_only"))
    ),
    "S24" -> Vector(
      CoverageGate("prepared_target_route", Vector("same_target_forcing_conversion")),
      CoverageGate("same_cluster_near_miss", Vector("vs_s04", "vs_s19")),
      CoverageGate("shortcut_negative", Vector("raw_tactic_only", "trade_or_result_only"))
    ),
    "S25" -> Vector(
      CoverageGate("rank_access_route", Vector("cross_wing_rank_switch")),
      CoverageGate(
        "same_cluster_near_miss",
        Vector("vs_s02", "vs_s03", "vs_s04", "vs_s09", "vs_s12", "vs_s23", "vs_s24")
      ),
      CoverageGate(
        "shortcut_negative",
        Vector(
          "piece_on_rank_only",
          "generic_rook_lift",
          "file_substrate_only",
          "back_rank_defense_only",
          "king_activity_only",
          "tactic_only"
        )
      )
    )
  )

  val lowerCarrierOwnersByBand: Map[String, Vector[String]] = Map(
    "S01" -> Vector(
      "Witness:available_lever_trigger(attacked_king_wing)",
      "Witness:pawn_push_break_contact_source(same_anchor_attacked_king_wing)",
      "ObjectSupportOnly:AttackScaffold",
      "CertificationSupportOnly:CertifiedKingSafetyEdge",
      "SupportOnly:ComparativeKingFragility|file_lane_state|diagonal_lane_only"
    ),
    "S02" -> Vector(
      "ObjectSupportOnly:AttackScaffold",
      "CertificationSupportOnly:CertifiedKingSafetyEdge",
      "ProjectionValidation:direct_piece_concentration|lane_strengthened_concentration",
      "SupportOnly:file_lane_state|diagonal_lane_only|rook_on_open_file_state"
    ),
    "S03" -> Vector(
      "Witness:diagonal_lane_only(king_theater_linked)",
      "ObjectSupportOnly:AttackScaffold",
      "CertificationSupportOnly:ComparativeKingFragility|CertifiedKingSafetyEdge",
      "SupportOnly:bishop_pair_state"
    ),
    "S04" -> Vector(
      "ObjectSupportOnly:KingSafetyShell",
      "ProjectionValidation:shell_payload_breach|support_break_breach",
      "CertificationSupportOnly:CertifiedKingSafetyEdge",
      "SupportOnly:AttackScaffold|ComparativeKingFragility|same_anchor_contact_target"
    ),
    "S05" -> Vector(
      "Witness:available_lever_trigger",
      "Witness:pawn_push_break_contact_source(center_pawn_target)",
      "ProjectionValidation:central_axis_continuation",
      "ObjectSupportOnly:CentralContactFront(non_truth_owner)",
      "CertificationSupportOnly:InitiativeWindow(non_truth_owner)",
      "SupportOnly:file_lane_state"
    ),
    "S06" -> Vector(
      "Witness:structural_space_claim",
      "Witness:knight_on_outpost_square|short_run_slider_gate_restriction",
      "ProjectionValidation:restriction_route",
      "CertificationSupportOnly:MobilityComparison|InitiativeWindow",
      "CertificationSupportOnly:SpaceBindRestrictionCertification(non_truth_owner)",
      "SupportOnly:weak_outpost_square_state"
    ),
    "S07" -> Vector(
      "Certification:DevelopmentComparison",
      "Certification:InitiativeWindow",
      "ObjectSupportOnly:OpeningDevelopmentRegime"
    ),
    "S08" -> Vector(
      "CertificationSupportOnly:InitiativeWindow(non_truth_owner)",
      "WitnessSupportOnly:pawn_push_break_contact_source(rival_source_not_truth_owner)",
      "ProjectionValidation:counterplay_denial_route_certified(rival_source_target_route)",
      "SupportOnly:MobilityComparison|short_run_slider_gate_restriction"
    ),
    "S09" -> Vector(
      "Witness:file_lane_state",
      "Witness:rook_on_open_file_state",
      "ProjectionValidation:open_file_entry|semi_open_file_entry|same_file_penetration",
      "CertificationSupportOnly:MaterialHarvest|WinningEndgame",
      "SupportOnly:AttackScaffold|CertifiedKingSafetyEdge"
    ),
    "S10" -> Vector(
      "Witness:weak_outpost_square_state",
      "Witness:knight_on_outpost_square",
      "ProjectionValidation:same_anchor_eviction_denial",
      "CertificationSupportOnly:MobilityComparison",
      "SupportOnly:short_run_slider_gate_restriction"
    ),
    "S21" -> Vector(
      "Witness:pawn_push_break_contact_source",
      "Certification:InitiativeWindow",
      "CertificationSupportOnly:DevelopmentComparison",
      "SupportOnly:available_lever_trigger|center|sector_asymmetry_state|AttackScaffold|EndgameRaceScaffold"
    ),
    "S11" -> Vector(
      "Witness:weak_pawn_target_state(same_target)",
      "ProjectionValidation:same_target_fixation|same_target_repeated_pressure",
      "ProjectionValidation:same_target_persistence_proof",
      "WitnessSeed:None",
      "Object:None",
      "Delta:None",
      "CertificationSupportOnly:MobilityComparison|InitiativeWindow(non_truth_owner)"
    ),
    "S12" -> Vector(
      "Witness:weak_outpost_square_state|diagonal_lane_only",
      "Witness:short_run_slider_gate_restriction",
      "ProjectionValidation:local_access_superiority",
      "CertificationSupportOnly:MobilityComparison",
      "SupportOnly:knight_on_outpost_square"
    ),
    "S13" -> Vector(
      "Witness:sector_asymmetry_state",
      "Witness:available_lever_trigger(same_wing_sector)",
      "Witness:pawn_push_break_contact_source(same_anchor_stronger_wing_mass_target)",
      "ProjectionValidation:wing_sector_non_chain_base_phalanx_edge_target|structurally_burdened_target",
      "SupportOnly:weak_pawn_target_state|file_lane_state"
    ),
    "S14" -> Vector(
      "Witness:available_lever_trigger(base_contact)",
      "Witness:pawn_push_break_contact_source(same_anchor_chain_base_target)",
      "ProjectionValidation:non_center_chain_base_target_recomputed|base_contact_continuation",
      "SupportOnly:weak_pawn_target_state|structural_damage_shell|fixed_chain_context"
    ),
    "S15" -> Vector(
      "Root:candidate_passer(same_owner_pawn)",
      "ShellSupportOnly:create_passer(non_truth_owner)",
      "ProjectionValidation:same_candidate_s13_wing_damage|same_candidate_s14_chain_base",
      "ProjectionEvidence:passer_creation_route_certified",
      "CertificationSupportOnly:PromotionRace(non_truth_owner)",
      "SupportOnly:PasserComplex|ConversionFunnel"
    ),
    "S16" -> Vector(
      "Witness:passed_pawn_entity_state(enemy_passer)",
      "ProjectionValidation:blockade_hold|restriction_hold|non_losing_race",
      "WitnessSupportOnly:short_run_slider_gate_restriction",
      "CertificationSupportOnly:PromotionRace|FortressDrawCertification|PerpetualCheckHolding",
      "ObjectSupportOnly:FortressHoldingShell|EndgameRaceScaffold"
    ),
    "S17" -> Vector(
      "WitnessSeed:same_piece_liability_anchor_seed",
      "WitnessSeed:same_piece_repair_route_seed|same_piece_exchange_relief_seed",
      "ProjectionEvidence:liability_relief_certified"
    ),
    "S18" -> Vector(
      "Witness:bishop_pair_state(same_owner_current_board_substrate)",
      "WitnessSeed:None",
      "Object:None",
      "Delta:None",
      "Certification:InitiativeWindow|MobilityComparison|MaterialHarvest(same_current_board_lower_truth_owner)",
      "CertificationSupportOnly:WinningEndgame(future_non_live_s18_carrier)",
      "SupportOnly:favorable_minor_piece_relation|DevelopmentComparison|short_run_slider_gate_restriction"
    ),
    "S19" -> Vector(
      "Delta:TradeInvariant",
      "Certification:FortressDrawCertification|MaterialHarvest",
      "ObjectSupportOnly:FortressHoldingShell"
    ),
    "S20" -> Vector(
      "Witness:short_run_slider_gate_restriction|duty_bound_defender",
      "Certification:MobilityComparison",
      "ProjectionValidation:mobility_plus_restriction|defender_starvation",
      "SupportOnly:structural_space_claim|weak_outpost_square_state|same_piece_liability_anchor_seed"
    ),
    "S22" -> Vector(
      "Object:FortressHoldingShell(same_holder_support_only)",
      "Certification:FortressDrawCertification|PerpetualCheckHolding(certified_hold_only)",
      "DeltaSupportOnly:TradeInvariant"
    ),
    "S23" -> Vector(
      "WitnessSeed:king_entry_square_seed",
      "WitnessSeed:king_access_route_seed|king_opposition_contact_seed",
      "ProjectionEvidence:king_entry_conversion_certified|king_opposition_certified",
      "CertificationSupportOnly:WinningEndgame|PromotionRace"
    ),
    "S24" -> Vector(
      "WitnessSeed:target_resource_dependency_seed",
      "WitnessSeed:target_attack_convergence_seed",
      "ProjectionEvidence:same_target_forcing_realization|same_target_conversion_certified"
    ),
    "S25" -> Vector(
      "WitnessSeed:rank_corridor_state_seed",
      "ProjectionEvidence:rank_access_consequence_certified",
      "ProjectionValidation:cross_wing_rank_switch",
      "SupportOnly:AttackScaffold|CertifiedKingSafetyEdge|file_lane_state|diagonal_lane_only|weak_outpost_square_state"
    )
  )

  val helperLawsByBand: Map[String, Vector[String]] = Map(
    "S05" -> Vector(
      "same_anchor_contact_target_law:center_pawn_target",
      "central_axis_continuation_law",
      "same_task_projection_evidence_must_mirror_s05_source_target_and_center_release_route_law",
      "central_contact_front_initiative_or_file_support_is_not_truth_owner_law"
    ),
    "S06" -> Vector(
      "structural_space_plus_outpost_or_restriction_law",
      "same_anchor_host_restriction_and_persistence_law",
      "mobility_comparison_is_support_not_space_law",
      "space_wording_without_anchor_is_non_admitting_law"
    ),
    "S07" -> Vector(
      "same_owner_development_comparison_plus_initiative_window_law",
      "move_right_window_law",
      "same_task_projection_evidence_must_bind_s07_owner_board_and_route_law",
      "initiative_window_is_lower_certification_not_projection_truth_owner_law"
    ),
    "S08" -> Vector(
      "exact_rival_release_source_denial_law",
      "same_rival_source_target_and_denial_route_law",
      "initiative_window_is_support_not_denial_by_itself",
      "mobility_or_restriction_support_is_not_s08_denial_truth_law"
    ),
    "S09" -> Vector(
      "same_file_lane_penetration_law",
      "rank_access_is_s25_not_s09_law",
      "file_substrate_or_rook_on_file_is_not_penetration_law"
    ),
    "S10" -> Vector(
      "knight_only_outpost_occupancy_law",
      "same_anchor_durability_required_law",
      "non_knight_outpost_scope_deferred_law"
    ),
    "S21" -> Vector(
      "non_center_owner_source_plus_same_board_initiative_law",
      "same_task_projection_evidence_must_mirror_s21_source_target_route_and_initiative_law",
      "initiative_window_is_not_counterplay_survival_truth_owner_law",
      "source_presence_is_not_survival_law",
      "pawn_storm_center_release_or_rival_denial_is_not_counterplay_survival_law"
    ),
    "S11" -> Vector(
      "same_weak_pawn_target_identity_law",
      "pressure_bundle_is_not_weak_pawn_truth_law",
      "fixed_pawn_persistence_is_current_board_carrier_not_certification_truth_law",
      "same_task_projection_evidence_must_mirror_s11_target_pressure_and_persistence_law",
      "target_swap_by_prose_is_non_admitting_law"
    ),
    "S12" -> Vector(
      "local_weak_square_or_diagonal_access_law",
      "restriction_reinforcement_required_law",
      "weak_square_or_diagonal_wording_is_not_access_law"
    ),
    "S13" -> Vector(
      "same_sector_asymmetry_plus_contact_source_law",
      "wing_damage_target_role_recomputed_from_exact_board_law",
      "phalanx_edge_target_excludes_chain_base_law",
      "center_sector_damage_is_not_wing_damage_law",
      "same_task_projection_evidence_must_mirror_s13_source_target_sector_and_route_law",
      "asymmetry_or_preexisting_weak_pawn_is_not_damage_law"
    ),
    "S14" -> Vector(
      "same_anchor_chain_base_contact_law",
      "non_center_chain_base_role_recomputed_from_exact_board_law",
      "base_contact_must_bind_same_source_and_target_law",
      "same_task_projection_evidence_must_mirror_s14_source_target_and_route_law",
      "fixed_chain_or_structural_damage_shell_is_not_truth_owner_law"
    ),
    "S15" -> Vector(
      "same_candidate_passer_contact_source_route_law",
      "per_position_creation_route_disjunction_broad_coverage_conjunction_law",
      "candidate_create_shell_existing_passer_or_split_anchor_is_not_creation_law",
      "upper_passer_consequence_is_not_truth_owner_law"
    ),
    "S16" -> Vector(
      "same_enemy_passer_suppression_route_law",
      "certified_hold_or_race_required_law",
      "passer_presence_or_blocker_picture_is_not_suppression_law",
      "hold_or_king_activity_rival_is_not_suppression_law"
    ),
    "S17" -> Vector(
      "same_piece_liability_relief_law",
      "relief_seed_and_evidence_same_piece_law",
      "bad_piece_wording_is_not_relief_law"
    ),
    "S18" -> Vector(
      "same_current_board_bishop_pair_conversion_law",
      "s18_projection_evidence_same_task_same_target_law",
      "conversion_certification_is_lower_truth_owner_law",
      "minor_edge_relation_without_conversion_is_non_admitting_law",
      "weak_square_liability_or_mobility_rival_is_not_conversion_law"
    ),
    "S19" -> Vector(
      "exact_trade_invariant_same_task_certification_law",
      "result_or_material_reduction_only_is_non_admitting_law",
      "fortress_shell_is_support_not_simplification_truth_law"
    ),
    "S20" -> Vector(
      "mobility_comparison_plus_restriction_or_starvation_law",
      "restriction_without_comparison_is_non_admitting_law",
      "space_access_or_liability_rival_is_not_domination_law"
    ),
    "S22" -> Vector(
      "fortress_or_perpetual_certified_hold_law",
      "same_holder_fortress_shell_and_certification_law",
      "perpetual_cycle_must_be_certified_law",
      "shell_or_draw_wording_is_not_hold_law",
      "support_only_or_deferred_hold_is_not_projection_admission_law",
      "trade_or_king_activity_rival_is_not_hold_law"
    ),
    "S23" -> Vector(
      "same_entry_or_same_contact_king_activity_law",
      "seed_presence_without_evidence_is_non_admitting_law",
      "file_passer_or_hold_rival_is_not_king_activity_law"
    ),
    "S24" -> Vector(
      "same_target_dependency_convergence_forcing_conversion_law",
      "tactic_or_result_only_is_not_prepared_target_law",
      "trade_or_shelter_breach_rival_is_not_prepared_target_law"
    ),
    "S25" -> Vector(
      "same_rank_cross_wing_corridor_law",
      "same_source_entry_and_corridor_kind_evidence_law",
      "rook_lift_file_king_tactic_or_piece_on_rank_is_non_admitting_law"
    ),
    "S01" -> Vector(
      "same_wing_contact_plus_attack_edge_law",
      "same_defending_king_safety_edge_law",
      "castling_context_is_not_storm_proof_law"
    ),
    "S02" -> Vector(
      "direct_king_ring_concentration_law",
      "lane_strengthens_but_does_not_admit_law",
      "attack_scaffold_is_not_projection_truth_owner_law"
    ),
    "S03" -> Vector(
      "king_facing_diagonal_entry_law",
      "fragility_linked_diagonal_same_king_law",
      "bishop_pair_is_not_diagonal_attack_law"
    ),
    "S04" -> Vector(
      "same_king_shell_breach_law",
      "shell_payload_or_support_break_breach_law",
      "king_safety_edge_same_defender_law",
      "shell_object_is_not_demolition_truth_owner_law"
    )
  )

  val exactValidationScaffoldByBand: Map[String, Vector[String]] = Map(
    "S01" -> Vector(
      "same_wing_owner_contact_target_present",
      "attack_scaffold_same_defending_king_present",
      "certified_king_safety_edge_same_owner_present",
      "castling_side_context_ignored"
    ),
    "S02" -> Vector(
      "attack_scaffold_same_defending_king_present",
      "certified_king_safety_edge_same_owner_present",
      "direct_piece_concentration_not_file_or_diagonal_only"
    ),
    "S03" -> Vector(
      "king_theater_diagonal_lane_only_present",
      "comparative_king_fragility_certified",
      "certified_king_safety_edge_same_owner_present",
      "bishop_pair_state_not_counted"
    ),
    "S04" -> Vector(
      "king_safety_shell_same_defender_present",
      "shelter_breach_from_shell_payload_or_support_break",
      "certified_king_safety_edge_same_defender_present",
      "shell_only_not_counted"
    ),
    "S05" -> Vector(
      "available_lever_trigger_present",
      "pawn_push_break_contact_source_center_target_present",
      "central_axis_continuation_present",
      "projection_evidence_mirrors_same_source_target_and_center_release_route",
      "wrong_source_wrong_target_or_wrong_axis_not_counted",
      "stale_or_adjacent_runtime_evidence_not_counted_before_live_admission",
      "open_center_wording_or_center_shell_only_not_counted"
    ),
    "S06" -> Vector(
      "structural_space_claim_present",
      "outpost_or_short_run_restriction_present",
      "same_anchor_restriction_route_present",
      "same_route_host_link_present",
      "exact_s05_center_release_and_exact_s20_domination_false_rivals_not_counted",
      "structural_space_only_wrong_anchor_or_missing_persistence_not_counted",
      "stale_or_unmatched_s06_evidence_not_counted_in_live_admission",
      "space_or_mobility_wording_only_not_counted"
    ),
    "S07" -> Vector(
      "development_comparison_certified",
      "initiative_window_certified",
      "move_right_window_bound_to_exact_board",
      "projection_evidence_same_board_route_present",
      "development_or_initiative_wording_only_not_counted"
    ),
    "S08" -> Vector(
      "initiative_window_certified",
      "rival_release_source_suppressed_present",
      "denial_route_bound_to_exact_board",
      "projection_evidence_mirrors_rival_source_target_and_denial_route",
      "wrong_source_wrong_target_wrong_route_or_stale_evidence_not_counted",
      "no_counterplay_wording_or_initiative_window_only_not_counted"
    ),
    "S09" -> Vector(
      "file_lane_state_present",
      "open_or_semi_open_file_entry_or_same_file_penetration_present",
      "rank_access_rival_kept_separate",
      "file_substrate_or_rook_on_file_only_not_counted"
    ),
    "S10" -> Vector(
      "weak_outpost_square_same_anchor_present",
      "knight_on_outpost_square_same_anchor_present",
      "same_anchor_eviction_denial_present",
      "good_piece_non_knight_or_non_durable_occupancy_not_counted"
    ),
    "S11" -> Vector(
      "weak_pawn_target_same_square_present",
      "same_target_pressure_or_repeated_pressure_present",
      "same_target_persistence_proof_present",
      "pressure_without_fixed_persistence_not_counted",
      "target_swap_by_prose_not_counted"
    ),
    "S12" -> Vector(
      "weak_square_or_diagonal_lane_anchor_present",
      "local_restriction_reinforcement_present",
      "local_access_superiority_present",
      "weak_square_or_diagonal_wording_only_not_counted"
    ),
    "S13" -> Vector(
      "sector_asymmetry_present",
      "same_sector_lever_and_break_contact_present",
      "wing_sector_damage_route_present",
      "contact_target_role_is_non_chain_base_phalanx_edge_or_structurally_burdened",
      "weak_pawn_or_asymmetry_only_not_counted",
      "stale_or_adjacent_runtime_evidence_not_counted_before_live_admission"
    ),
    "S14" -> Vector(
      "base_contact_lever_and_break_contact_present",
      "contact_target_role_is_non_center_chain_base",
      "base_contact_continuation_same_anchor_present",
      "fixed_chain_or_structural_damage_shell_only_not_counted",
      "stale_or_adjacent_runtime_evidence_not_counted_before_live_admission"
    ),
    "S15" -> Vector(
      "candidate_passer_same_anchor_present",
      "same_candidate_s13_or_s14_creation_route_present",
      "broad_coverage_requires_both_creation_route_buckets",
      "candidate_create_shell_existing_passer_or_split_anchor_only_not_counted",
      "live_passer_creation_evidence_same_candidate_present"
    ),
    "S16" -> Vector(
      "enemy_passed_pawn_same_anchor_present",
      "blockade_or_restriction_support_bound_to_enemy_passer",
      "non_losing_race_or_holding_proof_present",
      "passer_presence_or_blocker_picture_only_not_counted"
    ),
    "S17" -> Vector(
      "same_piece_liability_anchor_seed_present",
      "same_piece_repair_or_exchange_relief_seed_present",
      "liability_relief_certified_evidence_same_piece_present",
      "liability_or_improving_move_only_not_counted"
    ),
    "S18" -> Vector(
      "bishop_pair_state_present",
      "active_bishop_pair_member_present",
      "same_current_board_conversion_certification_present",
      "projection_evidence_mirrors_conversion_family_targets_and_bishop_pair",
      "material_conversion_bishop_pair_member_capture_present",
      "initiative_or_structure_conversion_support_targets_present",
      "bishop_pair_minor_edge_or_optional_strengthening_only_not_counted"
    ),
    "S19" -> Vector(
      "trade_invariant_delta_companion_present",
      "same_task_material_or_hold_certification_present",
      "current_fen_is_trade_task_endpoint",
      "trade_wording_material_reduction_or_result_only_not_counted"
    ),
    "S20" -> Vector(
      "mobility_comparison_certified",
      "restriction_or_duty_bound_defender_present",
      "domination_route_bound_to_exact_board",
      "mobility_or_restriction_alone_not_counted"
    ),
    "S21" -> Vector(
      "pawn_push_break_contact_source_present",
      "initiative_window_certified_same_board",
      "non_center_source_to_center_target_or_far_wing_source_survives",
      "projection_evidence_mirrors_owner_source_target_route_and_initiative",
      "break_source_initiative_only_deferred_initiative_or_adjacent_rival_not_counted"
    ),
    "S22" -> Vector(
      "fortress_holding_shell_or_perpetual_cycle_present",
      "certified_hold_verdict_present",
      "fortress_hold_bound_to_same_holder_king",
      "perpetual_hold_bound_to_current_board_cycle",
      "draw_wording_fortress_shell_checking_sequence_or_deferred_hold_not_counted"
    ),
    "S23" -> Vector(
      "king_entry_square_and_access_route_same_entry_present",
      "or_king_opposition_contact_same_square_present",
      "matching_projection_evidence_present",
      "centralization_or_proximity_only_not_counted"
    ),
    "S24" -> Vector(
      "target_resource_dependency_same_target_present",
      "target_attack_convergence_same_target_present",
      "same_target_forcing_and_conversion_evidence_present",
      "raw_tactic_trade_or_result_only_not_counted"
    ),
    "S25" -> Vector(
      "rank_corridor_state_seed_present",
      "same_source_entry_square_and_cross_wing_rank_switch_present",
      "rank_access_consequence_evidence_same_anchor_present",
      "piece_on_rank_rook_lift_file_king_or_tactic_only_not_counted"
    )
  )

  val coverageOnlyBandIds: Vector[StrategyProjectionBandId] =
    frozenCoverageBandIds.filterNot(StrategyProjectionScopeContract.isStartReadyBandId)

  val rowSpecificAdmissionBurdenFreezeByBand: Map[String, Vector[String]] = Map(
    "S05" -> Vector(
      "center_release_route_requires_same_anchor_lever_and_contact_source",
      "center_release_route_requires_recomputed_center_pawn_target",
      "central_axis_continuation_requires_same_source_target_pair",
      "projection_evidence_mirrors_owner_source_target_and_center_release_route",
      "open_center_wording_center_shell_support_object_optional_strengthening_or_adjacent_rival_is_non_admitting"
    ),
    "S06" -> Vector(
      "space_bind_route_requires_structural_space_claim",
      "space_bind_route_requires_same_anchor_outpost_or_short_run_restriction",
      "space_bind_route_requires_space_bind_restriction_certification",
      "space_bind_certification_requires_same_route_host_link",
      "same_task_projection_evidence_must_mirror_s06_route_anchor_host_and_restriction",
      "s05_center_release_s20_domination_or_mobility_certification_alone_is_non_admitting",
      "wrong_anchor_wrong_sector_stale_or_shortcut_evidence_is_non_admitting"
    ),
    "S07" -> Vector(
      "initiative_conversion_route_requires_opening_development_regime_and_same_owner_development_comparison",
      "initiative_conversion_route_requires_same_owner_certified_initiative_window",
      "projection_evidence_must_bind_same_owner_board_and_initiative_conversion_route",
      "development_only_initiative_only_s08_denial_or_optional_strengthening_is_non_admitting"
    ),
    "S08" -> Vector(
      "denial_route_requires_same_board_certified_initiative_window",
      "denial_route_requires_exact_rival_pawn_push_break_contact_source",
      "projection_evidence_must_bind_rival_source_target_and_denial_route",
      "initiative_window_mobility_restriction_s07_s21_s20_or_wording_only_is_non_admitting",
      "stale_evidence_wrong_source_wrong_target_or_wrong_route_is_non_admitting"
    ),
    "S11" -> Vector(
      "target_pressure_route_requires_same_square_weak_pawn_target_and_owner_pressure",
      "fixation_route_requires_current_fixed_pawn_persistence_on_same_target",
      "repeated_pressure_route_requires_two_owner_attackers_on_same_target",
      "projection_evidence_mirrors_weak_pawn_target_pressure_and_persistence",
      "weak_pawn_only_target_swap_adjacent_rival_or_optional_strengthening_is_non_admitting"
    ),
    "S13" -> Vector(
      "wing_damage_route_requires_same_sector_asymmetry_and_same_anchor_contact_source",
      "wing_damage_route_excludes_center_sector_targets",
      "phalanx_edge_route_requires_recomputed_phalanx_edge_target",
      "phalanx_edge_route_excludes_chain_base_targets",
      "burdened_target_route_requires_recomputed_structurally_burdened_target",
      "projection_evidence_mirrors_owner_source_target_sector_and_damage_route",
      "asymmetry_weak_pawn_adjacent_rival_or_optional_strengthening_is_non_admitting"
    ),
    "S14" -> Vector(
      "chain_base_route_requires_same_anchor_lever_and_contact_source",
      "chain_base_route_requires_recomputed_non_center_chain_base_target",
      "base_contact_continuation_requires_same_source_target_pair",
      "projection_evidence_mirrors_owner_source_target_and_chain_base_route",
      "fixed_chain_structural_shell_adjacent_rival_or_optional_strengthening_is_non_admitting"
    ),
    "S15" -> Vector(
      "creation_route_requires_same_owner_candidate_passer_anchor",
      "s13_route_requires_same_candidate_contact_source_and_recomputed_wing_damage_target",
      "s14_route_requires_same_candidate_contact_source_and_recomputed_chain_base_target",
      "projection_evidence_mirrors_candidate_source_target_and_creation_route",
      "candidate_shell_existing_passer_split_anchor_optional_strengthening_or_adjacent_rival_is_non_admitting"
    ),
    "S16" -> Vector(
      "suppression_route_requires_same_enemy_passed_pawn_anchor",
      "blockade_route_requires_owner_blocker_on_enemy_passer_front_square",
      "restriction_hold_route_requires_same_enemy_passer_blocker_owner_restriction_and_certified_hold",
      "race_route_requires_enemy_passer_and_same_board_non_losing_race_certification",
      "projection_evidence_mirrors_enemy_passer_route_and_route_specific_proof",
      "enemy_passer_blocker_restriction_hold_race_or_adjacent_rival_alone_is_non_admitting"
    ),
    "S18" -> Vector(
      "initiative_route_requires_bishop_pair_state_and_same_board_initiative_window",
      "structure_route_requires_bishop_pair_state_and_same_board_mobility_comparison",
      "material_route_requires_bishop_pair_member_material_harvest",
      "projection_evidence_mirrors_conversion_family_targets_and_bishop_pair",
      "relation_only_optional_strengthening_or_adjacent_rival_is_non_admitting"
    ),
    "S19" -> Vector(
      "material_route_requires_canonical_trade_invariant_and_same_move_material_harvest",
      "hold_route_requires_canonical_trade_invariant_and_post_trade_fortress_certification",
      "result_only_material_only_or_adjacent_rival_is_non_admitting"
    ),
    "S21" -> Vector(
      "counterplay_survival_route_requires_owner_break_contact_source",
      "counterplay_survival_route_requires_certified_same_board_initiative_window",
      "counterplay_survival_projection_evidence_must_mirror_source_target_route_and_certification",
      "source_only_initiative_only_deferred_or_adjacent_rival_is_non_admitting"
    ),
    "S22" -> Vector(
      "fortress_route_requires_same_holder_shell_and_certified_fortress_hold",
      "perpetual_route_requires_certified_perpetual_check_hold",
      "support_only_or_deferred_hold_verdict_is_non_admitting"
    )
  )

  val preLiveAdmissionBurdenFreezeByBand: Map[String, Vector[String]] = Map.empty

  val preLiveProjectionEvidenceKindFreezeByBand: Map[String, Vector[StrategyProjectionEvidenceKind]] = Map.empty

  val preLiveRuntimeBoundaryFreezeByBand: Map[String, Vector[String]] = Map.empty

  val projectionEvidenceKindFreezeByBand: Map[String, Vector[StrategyProjectionEvidenceKind]] = Map(
    "S05" -> Vector(
      StrategyProjectionEvidenceKind("center_release_route_certified")
    ),
    "S06" -> Vector(
      StrategyProjectionEvidenceKind("space_bind_restriction_route_certified")
    ),
    "S07" -> Vector(
      StrategyProjectionEvidenceKind("initiative_conversion_route_certified")
    ),
    "S08" -> Vector(
      StrategyProjectionEvidenceKind("counterplay_denial_route_certified")
    ),
    "S11" -> Vector(
      StrategyProjectionEvidenceKind("weak_pawn_target_pressure_persistence_certified")
    ),
    "S13" -> Vector(
      StrategyProjectionEvidenceKind("wing_damage_route_certified")
    ),
    "S14" -> Vector(
      StrategyProjectionEvidenceKind("chain_base_contact_route_certified")
    ),
    "S15" -> Vector(
      StrategyProjectionEvidenceKind("passer_creation_route_certified")
    ),
    "S16" -> Vector(
      StrategyProjectionEvidenceKind("passer_suppression_route_certified")
    ),
    "S18" -> Vector(
      StrategyProjectionEvidenceKind("bishop_pair_initiative_conversion_certified"),
      StrategyProjectionEvidenceKind("bishop_pair_structure_conversion_certified"),
      StrategyProjectionEvidenceKind("bishop_pair_material_conversion_certified")
    ),
    "S19" -> Vector(
      StrategyProjectionEvidenceKind("trade_invariant_material_simplification_certified"),
      StrategyProjectionEvidenceKind("trade_invariant_hold_simplification_certified")
    ),
    "S21" -> Vector(
      StrategyProjectionEvidenceKind("counterplay_survival_route_certified")
    ),
    "S22" -> Vector(
      StrategyProjectionEvidenceKind("fortress_hold_certified"),
      StrategyProjectionEvidenceKind("perpetual_hold_certified")
    )
  )

  def requiredCoveragePairsFor(band: String): Set[(String, String)] =
    coverageGatesByBand.getOrElse(band, Vector.empty).flatMap(_.pairs).toSet

  require(
    coverageGatesByBand.keySet == frozenCoverageBandIds.map(_.value).toSet,
    "Every frozen coverage band must declare coverage gates"
  )
  require(
    frozenCoverageBandIds.map(_.value).toSet ==
      globalClosureBroadReadyCoverageBandIds.map(_.value).toSet,
    "Projection broad-ready global closure must cover exactly S01-S25"
  )
  require(
    lowerCarrierOwnersByBand.keySet == frozenCoverageBandIds.map(_.value).toSet,
    "Every frozen coverage band must declare lower carrier ownership"
  )
  require(
    helperLawsByBand.keySet == frozenCoverageBandIds.map(_.value).toSet,
    "Every frozen coverage band must declare helper laws"
  )
  require(
    exactValidationScaffoldByBand.keySet == frozenCoverageBandIds.map(_.value).toSet,
    "Every frozen coverage band must declare an exact validation scaffold"
  )
  require(
    rowSpecificAdmissionBurdenFreezeByBand.keySet == Set("S05", "S06", "S07", "S08", "S11", "S13", "S14", "S15", "S16", "S18", "S19", "S21", "S22"),
    "S05/S06/S07/S08/S11/S13/S14/S15/S16/S18/S19/S21/S22 row-specific admission burden freezes are current"
  )
  require(
    projectionEvidenceKindFreezeByBand.keySet == rowSpecificAdmissionBurdenFreezeByBand.keySet,
    "Projection evidence freezes must match row-specific admission burden freezes"
  )
  require(
    projectionEvidenceKindFreezeByBand.forall: (band, kinds) =>
      val bandId = StrategyProjectionBandId(band)
      if StrategyProjectionScopeContract.isStartReadyBandId(bandId) then
        StrategyProjectionScopeContract.requiredEvidenceKindsByBand.getOrElse(band, Vector.empty) == kinds
      else !StrategyProjectionScopeContract.requiredEvidenceKindsByBand.contains(band),
    "Projection evidence freezes must match live admission only for start-ready bands"
  )
  require(
    preLiveAdmissionBurdenFreezeByBand.isEmpty,
    "No pre-live projection admission freezes remain in this scaffold"
  )
  require(
    preLiveProjectionEvidenceKindFreezeByBand.keySet == preLiveAdmissionBurdenFreezeByBand.keySet,
    "Pre-live projection evidence kind freezes must match pre-live admission burden freezes"
  )
  require(
    preLiveRuntimeBoundaryFreezeByBand.keySet == preLiveAdmissionBurdenFreezeByBand.keySet,
    "Pre-live runtime boundary freezes must match pre-live admission burden freezes"
  )
  require(
    preLiveAdmissionBurdenFreezeByBand.keySet.forall: band =>
      coverageOnlyBandIds.exists(_.value == band) &&
        !StrategyProjectionScopeContract.startReadyBandIds.exists(_.value == band) &&
        !StrategyProjectionScopeContract.requiredEvidenceKindsByBand.contains(band),
    "Pre-live admission freezes must stay coverage-only and outside live required evidence"
  )
  require(
    preLiveProjectionEvidenceKindFreezeByBand.forall: (band, kinds) =>
      val bandId = StrategyProjectionBandId(band)
      kinds.nonEmpty && kinds.forall(kind => !StrategyProjectionScopeContract.isAllowedEvidenceKind(bandId, kind)),
    "Pre-live evidence kinds are frozen names only and must not be live-allowed before start-ready promotion"
  )
  require(
    coverageOnlyBandIds.map(_.value) ==
      Vector(
        "S09",
        "S10",
        "S12",
        "S20",
        "S01",
        "S02",
        "S03",
        "S04"
      ),
    "All non-runtime broad-ready projection bands are coverage-only here and must not enter live admission"
  )
