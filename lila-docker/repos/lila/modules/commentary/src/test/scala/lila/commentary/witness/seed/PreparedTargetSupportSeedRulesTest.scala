package lila.commentary.witness.seed

import chess.{ Color, Queen }

import lila.commentary.witness.seed.StrategySupportSeedTestSupport.*

class PreparedTargetSupportSeedRulesTest extends munit.FunSuite:

  test("target dependency and attack convergence stay tied to one named target"):
    val fen = "6k1/8/4b3/3q4/8/8/6B1/3R2K1 w - - 0 1"

    val dependency =
      findPieceSquare(fen, "target_resource_dependency_seed", "d5", Some(Color.White))
        .getOrElse(fail("missing target dependency"))
    val convergence =
      findPieceSquare(fen, "target_attack_convergence_seed", "d5", Some(Color.White))
        .getOrElse(fail("missing target convergence"))

    assertEquals(squareValue(dependency.payload, "target_square").map(_.key), Some("d5"))
    assertEquals(roleValue(dependency.payload, "target_role"), Some(Queen))
    assertEquals(squareValue(dependency.payload, "dependency_defender_square").map(_.key), Some("e6"))
    assertEquals(squareValue(convergence.payload, "target_square").map(_.key), Some("d5"))
    assertEquals(squareValue(convergence.payload, "dependency_defender_square").map(_.key), Some("e6"))
    assertEquals(squareList(convergence.payload, "attack_source_squares").map(_.key), Vector("d1", "g2"))
    assertEquals(token(convergence.payload, "tactic_family"), None)
    assertEquals(token(convergence.payload, "conversion_result"), None)

  test("target dependency alone does not imply attack convergence"):
    val fen = "6k1/8/4b3/3q4/8/8/8/3R2K1 w - - 0 1"

    assert(findPieceSquare(fen, "target_resource_dependency_seed", "d5", Some(Color.White)).nonEmpty)
    assert(findPieceSquare(fen, "target_attack_convergence_seed", "d5", Some(Color.White)).isEmpty)

  test("prepared-target seeds reject generic pressure with no named dependency"):
    val fen = "6k1/8/8/3q4/8/8/6B1/3R2K1 w - - 0 1"

    assertEquals(descriptor(fen, "target_resource_dependency_seed"), Vector.empty)
    assertEquals(descriptor(fen, "target_attack_convergence_seed"), Vector.empty)

  test("prepared-target seeds reject exact tactic alone"):
    val forkOnlyFen = "4k3/8/3r1r2/8/4N3/8/8/4K3 w - - 0 1"

    assertEquals(descriptor(forkOnlyFen, "target_resource_dependency_seed"), Vector.empty)
    assertEquals(descriptor(forkOnlyFen, "target_attack_convergence_seed"), Vector.empty)

  test("prepared-target seeds reject result or trade-only shortcuts"):
    val rookTradeOnlyFen = "4k3/8/8/3r4/3R4/8/8/4K3 w - - 0 1"

    assertEquals(descriptor(rookTradeOnlyFen, "target_resource_dependency_seed"), Vector.empty)
    assertEquals(descriptor(rookTradeOnlyFen, "target_attack_convergence_seed"), Vector.empty)
