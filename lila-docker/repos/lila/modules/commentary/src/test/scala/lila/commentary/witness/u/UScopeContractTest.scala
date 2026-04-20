package lila.commentary.witness.u

import lila.commentary.witness.{ Witness, WitnessDescriptorId }

class UScopeContractTest extends munit.FunSuite:

  test("u scope contract is frozen to the active U-primary 18 only"):
    assertEquals(
      UScopeContract.activePrimaryDescriptorIds.map(_.value),
      Vector(
        "file_lane_state",
        "diagonal_lane_only",
        "weak_pawn_target_state",
        "passed_pawn_entity_state",
        "weak_outpost_square_state",
        "loose_piece_target_state",
        "pawn_push_break_contact_source",
        "sector_asymmetry_state",
        "available_lever_trigger",
        "rook_on_open_file_state",
        "bishop_pair_state",
        "knight_on_outpost_square",
        "duty_bound_defender",
        "short_run_slider_gate_restriction",
        "pin",
        "fork",
        "skewer",
        "overload"
      )
    )

    assertEquals(UScopeContract.bannedRehomeRows, Set(
      "opening-tempo",
      "middlegame-positional",
      "transition-liquidation",
      "endgame-race",
      "central tension"
    ))
    assertEquals(UScopeContract.hostShellOnlyRows, Set("closed center", "fixed chain"))
    assertEquals(
      UScopeContract.blockedStrategySeedFamilies,
      Set(
        "same_piece_liability_anchor_seed",
        "same_piece_repair_route_seed",
        "same_piece_exchange_relief_seed",
        "king_entry_square_seed",
        "king_access_route_seed",
        "king_opposition_contact_seed",
        "target_resource_dependency_seed",
        "target_attack_convergence_seed"
      )
    )

  test("u scope contract rejects out-of-scope descriptor registration"):
    intercept[IllegalArgumentException]:
      UScopeContract.requireActivePrimaryDescriptorIds(
        Vector(
          WitnessDescriptorId("pin"),
          WitnessDescriptorId("closed_center"),
          WitnessDescriptorId("king_entry_square_seed")
        )
      )

  test("u runtime registry rejects duplicate or out-of-scope rules before extraction"):
    final case class TestRule(descriptorId: WitnessDescriptorId) extends UScopedWitnessRule:
      def extract(context: UExtractionContext): Vector[Witness] = Vector.empty

    val pin = TestRule(WitnessDescriptorId("pin"))

    intercept[IllegalArgumentException]:
      UInternalRuntime.validateRegisteredRules(Vector(pin, pin))

    intercept[IllegalArgumentException]:
      UInternalRuntime.validateRegisteredRules(
        Vector(TestRule(WitnessDescriptorId("closed_center")))
      )

    val validated = UInternalRuntime.validateRegisteredRules(
      Vector(pin, TestRule(WitnessDescriptorId("fork")))
    )

    assertEquals(validated.map(_.descriptorId.value), Vector("pin", "fork"))
