package lila.commentary.witness.seed

import chess.format.Fen

import lila.commentary.CommentaryCore

class StrategySupportSeedScopeContractTest extends munit.FunSuite:

  test("strategy support seeds use a distinct live contract outside U-primary descriptors"):
    assertEquals(
      StrategySupportSeedScopeContract.allowedSeedIds.map(_.value),
      Vector(
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
    )

    assertEquals(
      StrategySupportSeedScopeContract.seedIdsByBand.view.mapValues(_.map(_.value)).toMap,
      Map(
        "S17" -> Vector(
          "same_piece_liability_anchor_seed",
          "same_piece_repair_route_seed",
          "same_piece_exchange_relief_seed"
        ),
        "S23" -> Vector(
          "king_entry_square_seed",
          "king_access_route_seed",
          "king_opposition_contact_seed"
        ),
        "S24" -> Vector(
          "target_resource_dependency_seed",
          "target_attack_convergence_seed"
        ),
        "S25" -> Vector(
          "rank_corridor_state_seed"
        )
      )
    )

    assertEquals(CommentaryCore.activeUPrimaryDescriptorIds.size, 18)
    assertEquals(
      CommentaryCore.activeUPrimaryDescriptorIds.intersect(
        StrategySupportSeedScopeContract.allowedSeedIds.map(_.value)
      ),
      Vector.empty
    )
    assertEquals(
      CommentaryCore.strategySupportSeedContractIds,
      StrategySupportSeedScopeContract.allowedSeedIds.map(_.value)
    )
    assertEquals(
      CommentaryCore.liveStrategySupportSeedIds,
      Vector(
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
    )

  test("strategy support seed ids validate independently from witness descriptor ids"):
    val id = StrategySupportSeedId("king_entry_square_seed")

    assertEquals(id.value, "king_entry_square_seed")
    intercept[IllegalArgumentException]:
      StrategySupportSeedId("King Entry Square")

  test("support seed extractor exposes root, Fen, String, and fail-closed facades"):
    val fenText = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
    val fen = Fen.Full.clean(fenText)
    val fromFen = StrategySupportSeedExtractor.fromFen(fen).fold(message => fail(message), identity)
    val fromString =
      CommentaryCore.extractStrategySupportSeedsFromFen(fenText).fold(message => fail(message), identity)
    val fromRoot = CommentaryCore.extractStrategySupportSeeds(fromFen.rootState)

    assertEquals(fromRoot.seeds, fromFen.seeds)
    assertEquals(fromFen.seeds, fromString.seeds)
    assert(fromFen.seeds.isEmpty)

    val illegalFenText = "8/8/8/8/8/8/4k3/4K3 w - - 0 1"
    val illegalFen = Fen.Full.clean(illegalFenText)
    assert(StrategySupportSeedExtractor.fromFenFailClosed(illegalFen).isLeft)
    assert(CommentaryCore.extractStrategySupportSeedsFromFenFailClosed(illegalFenText).isLeft)
