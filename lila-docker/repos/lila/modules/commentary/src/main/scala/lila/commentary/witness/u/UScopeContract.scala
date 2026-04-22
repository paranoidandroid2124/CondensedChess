package lila.commentary.witness.u

import lila.commentary.witness.WitnessDescriptorId

object UScopeContract:

  val activePrimaryDescriptorIds: Vector[WitnessDescriptorId] = Vector(
    WitnessDescriptorId("file_lane_state"),
    WitnessDescriptorId("diagonal_lane_only"),
    WitnessDescriptorId("weak_pawn_target_state"),
    WitnessDescriptorId("passed_pawn_entity_state"),
    WitnessDescriptorId("weak_outpost_square_state"),
    WitnessDescriptorId("loose_piece_target_state"),
    WitnessDescriptorId("pawn_push_break_contact_source"),
    WitnessDescriptorId("sector_asymmetry_state"),
    WitnessDescriptorId("available_lever_trigger"),
    WitnessDescriptorId("rook_on_open_file_state"),
    WitnessDescriptorId("bishop_pair_state"),
    WitnessDescriptorId("knight_on_outpost_square"),
    WitnessDescriptorId("duty_bound_defender"),
    WitnessDescriptorId("short_run_slider_gate_restriction"),
    WitnessDescriptorId("pin"),
    WitnessDescriptorId("fork"),
    WitnessDescriptorId("skewer"),
    WitnessDescriptorId("overload")
  )

  val activePrimaryDescriptorIdSet: Set[WitnessDescriptorId] = activePrimaryDescriptorIds.toSet
  private val activePrimaryDescriptorIdValues: Set[String] = activePrimaryDescriptorIds.map(_.value).toSet

  val bannedRehomeRows: Set[String] = Set(
    "opening-tempo",
    "middlegame-positional",
    "transition-liquidation",
    "endgame-race",
    "central tension"
  )

  val hostShellOnlyRows: Set[String] = Set(
    "closed center",
    "fixed chain"
  )

  val blockedStrategySeedFamilies: Set[String] = Set(
    "same_piece_liability_anchor_seed",
    "same_piece_repair_route_seed",
    "same_piece_exchange_relief_seed",
    "king_entry_square_seed",
    "king_access_route_seed",
    "king_opposition_contact_seed",
    "target_resource_dependency_seed",
    "target_attack_convergence_seed",
    "rank_corridor_state_seed"
  )

  require(
    activePrimaryDescriptorIds.size == 18,
    s"Expected exactly 18 active U-primary descriptor ids, found ${activePrimaryDescriptorIds.size}"
  )
  require(
    activePrimaryDescriptorIds.distinct.size == activePrimaryDescriptorIds.size,
    "Duplicate active U-primary descriptor ids are not allowed"
  )

  def isActivePrimaryDescriptorId(id: WitnessDescriptorId): Boolean =
    activePrimaryDescriptorIdSet.contains(id)

  private[witness] def requireActivePrimaryDescriptorIds(
      descriptorIds: IterableOnce[WitnessDescriptorId]
  ): Unit =
    val outOfScope =
      descriptorIds.iterator
        .map(_.value)
        .filterNot(activePrimaryDescriptorIdValues)
        .toSet
        .toVector
        .sorted

    require(
      outOfScope.isEmpty,
      s"Out-of-scope U-primary descriptor ids: ${outOfScope.mkString(", ")}"
    )
