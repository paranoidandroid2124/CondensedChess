package lila.commentary.strategic

import StrategicObjectTestSupport.*
import lila.commentary.witness.WitnessAnchor
import lila.commentary.witness.WitnessSector

class CentralContactFrontRuleTest extends munit.FunSuite:

  test("central contact front selects one canonical component instead of merging disconnected fronts"):
    val fen = "4k3/8/2pp4/1pPP3n/4p3/5N2/1N2N3/4K3 w - - 0 1"

    val objectWitnesses = family(fen, "CentralContactFront")
    val centralFront = findSector(fen, "CentralContactFront", WitnessSector.Center)
    val componentSquares = centralFront.toVector.flatMap(obj => squareList(obj.payload, "component_squares")).map(_.key)

    assertEquals(objectWitnesses.size, 1)
    assert(centralFront.nonEmpty)
    assert(componentSquares.contains("c4"))
    assert(componentSquares.contains("c5"))
    assert(componentSquares.contains("d5"))
    assert(componentSquares.contains("c6"))
    assert(componentSquares.contains("d6"))
    assert(!componentSquares.contains("f3"))
    assert(!componentSquares.contains("f4"))

  test("central contact front admits one connected central contact component with both colors present"):
    val fen = "6k1/6pp/5n2/3pp3/3PP3/5N2/6PP/6K1 w - - 0 1"

    val objectWitnesses = family(fen, "CentralContactFront")
    val centralFront = findSector(fen, "CentralContactFront", WitnessSector.Center)

    assertEquals(objectWitnesses.size, 1)
    assert(centralFront.nonEmpty)
    assertEquals(centralFront.get.anchor, WitnessAnchor.SectorAnchor(WitnessSector.Center))
    assertEquals(centralFront.get.color, None)
    assertEquals(
      squareList(centralFront.get.payload, "component_squares").map(_.key),
      Vector("d4", "e4", "d5", "e5")
    )
    assert(squareList(centralFront.get.payload, "contested_squares").map(_.key).contains("d4"))
    assertEquals(
      squareList(centralFront.get.payload, "occupied_contact_squares").map(_.key),
      Vector("d4", "e4", "d5", "e5")
    )

  test("central contact front rejects a single-square central touch"):
    val fen = "4k3/8/8/8/8/8/3N1n2/4K3 w - - 0 1"

    assertEquals(family(fen, "CentralContactFront"), Vector.empty)

  test("central contact front rejects an empty central board"):
    val fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"

    assertEquals(family(fen, "CentralContactFront"), Vector.empty)
