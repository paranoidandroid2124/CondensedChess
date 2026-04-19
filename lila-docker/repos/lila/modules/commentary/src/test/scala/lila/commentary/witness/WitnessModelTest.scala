package lila.commentary.witness

import chess.{ Color, File, Square }

class WitnessModelTest extends munit.FunSuite:

  test("witness payload preserves insertion order and rejects duplicate fields"):
    val payload = WitnessPayload(
      "state" -> WitnessValue.Token("open"),
      "file" -> WitnessValue.FileValue(File.E)
    )

    assertEquals(payload.entries.map(_._1), Vector("state", "file"))
    assertEquals(payload.get("file"), Some(WitnessValue.FileValue(File.E)))

    intercept[IllegalArgumentException]:
      WitnessPayload(
        "state" -> WitnessValue.Token("open"),
        "state" -> WitnessValue.Token("semi_open")
      )

  test("witness payload merges compatible list and mask fields"):
    val left = WitnessPayload(
      "targetSquares" -> WitnessValue.SquareListValue(
        Vector(Square.fromKey("e5").get, Square.fromKey("f6").get)
      ),
      "rayMask" -> WitnessValue.LongMaskValue(0x04L)
    )
    val right = WitnessPayload(
      "targetSquares" -> WitnessValue.SquareListValue(
        Vector(Square.fromKey("f6").get, Square.fromKey("g7").get)
      ),
      "rayMask" -> WitnessValue.LongMaskValue(0x10L)
    )

    val merged = left.merge(right)

    assertEquals(
      merged.get("targetSquares"),
      Some(
        WitnessValue.SquareListValue(
          Vector(
            Square.fromKey("e5").get,
            Square.fromKey("f6").get,
            Square.fromKey("g7").get
          )
        )
      )
    )
    assertEquals(merged.get("rayMask"), Some(WitnessValue.LongMaskValue(0x14L)))

  test("witness constructors enforce color discipline"):
    val square = WitnessAnchor.SquareAnchor(Square.fromKey("d5").get)

    assertEquals(
      Witness.neutral(WitnessDescriptorId("diagonal_lane_only"), square).color,
      None
    )
    assertEquals(
      Witness.owner(WitnessDescriptorId("bishop_pair_state"), Color.White, square).color,
      Some(Color.White)
    )

    intercept[IllegalArgumentException]:
      Witness(
        descriptorId = WitnessDescriptorId("pin"),
        anchor = square,
        polarity = WitnessPolarity.Beneficiary,
        color = None
      )

  test("witness set merges duplicate identity instances and unions support payload"):
    val anchor = WitnessAnchor.PieceSquareAnchor(Square.fromKey("e4").get)
    val descriptorId = WitnessDescriptorId("fork")

    val first = Witness.beneficiary(
      descriptorId = descriptorId,
      color = Color.White,
      anchor = anchor,
      payload = WitnessPayload(
        "targetSquares" -> WitnessValue.SquareListValue(Vector(Square.fromKey("d6").get))
      ),
      support = WitnessSupport
        .roots(12, 44)
        .addTargetSquare(Square.fromKey("d6").get)
        .addTag("attackedTargets")
    )

    val second = Witness.beneficiary(
      descriptorId = descriptorId,
      color = Color.White,
      anchor = anchor,
      payload = WitnessPayload(
        "targetSquares" -> WitnessValue.SquareListValue(
          Vector(Square.fromKey("f6").get, Square.fromKey("d6").get)
        )
      ),
      support = WitnessSupport
        .roots(12, 90)
        .addTargetSquare(Square.fromKey("f6").get)
        .addGeometryMask("squareMask", 0x20L)
    )

    val merged = WitnessSet(Vector(first, second))

    assertEquals(merged.all.size, 1)
    assertEquals(merged.all.head.support.rootIndices, Vector(12, 44, 90))
    assertEquals(
      merged.all.head.support.targetSquares.map(_.key),
      Vector("d6", "f6")
    )
    assertEquals(merged.all.head.support.geometryMask("squareMask"), Some(0x20L))
    assertEquals(merged.all.head.support.supportingTags, Vector("attackedTargets"))
    assertEquals(
      merged.all.head.payload.get("targetSquares"),
      Some(
        WitnessValue.SquareListValue(
          Vector(Square.fromKey("d6").get, Square.fromKey("f6").get)
        )
      )
    )

  test("witness set presence checks can discriminate polarity"):
    val anchor = WitnessAnchor.PieceSquareAnchor(Square.fromKey("e4").get)
    val descriptorId = WitnessDescriptorId("fork")
    val witness = Witness.beneficiary(descriptorId, Color.White, anchor)
    val set = WitnessSet(Vector(witness))

    assert(set.contains(descriptorId, anchor, polarity = Some(WitnessPolarity.Beneficiary), color = Some(Color.White)))
    assert(!set.contains(descriptorId, anchor, polarity = Some(WitnessPolarity.Owner), color = Some(Color.White)))

  test("ray helpers preserve exact source and direction geometry"):
    val source = Square.fromKey("d4").get
    val target = Square.fromKey("h8").get
    val ray = WitnessRay.between(source, target).getOrElse(fail("expected a diagonal ray"))

    assertEquals(ray.direction, WitnessDirection.NorthEast)
    assertEquals(ray.key, "d4:north_east")
    assertEquals(ray.squares.map(_.key), Vector("e5", "f6", "g7", "h8"))
    assert(ray.contains(target))
