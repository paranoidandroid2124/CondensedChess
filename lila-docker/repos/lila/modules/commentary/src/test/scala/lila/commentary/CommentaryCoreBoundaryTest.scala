package lila.commentary

import chess.{ Color, File, Square }
import chess.format.{ Fen, Uci }

import lila.commentary.delta.{ StrategicDeltaScope, StrategicDeltaTag }
import lila.commentary.root.RootExtractor
import lila.commentary.witness.{
  WitnessAnchor,
  WitnessDescriptorId,
  WitnessDirection,
  WitnessRay,
  WitnessSector,
  WitnessVariantId
}
import lila.commentary.witness.u.UWitnessTestSupport.squareList

class CommentaryCoreBoundaryTest extends munit.FunSuite:

  test("CommentaryCore publishes the frozen live U-primary 18 ids"):
    assertEquals(
      CommentaryCore.activeUPrimaryDescriptorIds,
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

  test("CommentaryCore exposes seeded U witnesses through the public rootState, Fen, and String facades"):
    val seededFenText = "4k3/8/8/3p4/3B4/8/4RQ2/4K3 w - - 0 1"
    val seededFen = Fen.Full.clean(seededFenText)
    val rootState = RootExtractor.fromFen(seededFen).fold(message => fail(message), identity)
    val fromRoot = CommentaryCore.extractUWitnesses(rootState)
    val fromFen = CommentaryCore.extractUWitnesses(seededFen).fold(message => fail(message), identity)
    val fromString = CommentaryCore.extractUWitnessesFromFen(seededFenText).fold(message => fail(message), identity)

    assertEquals(fromRoot.witnesses, fromFen.witnesses)
    assertEquals(fromFen.witnesses, fromString.witnesses)

    assert(
      fromRoot.witnesses.contains(
        WitnessDescriptorId("file_lane_state"),
        WitnessAnchor.FileAnchor(File.E),
        variant = Some(WitnessVariantId("open_file_state"))
      )
    )
    assert(
      fromRoot.witnesses.contains(
        WitnessDescriptorId("rook_on_open_file_state"),
        WitnessAnchor.PieceSquareAnchor(Square.fromKey("e2").get),
        color = Some(Color.White)
      )
    )
    assert(
      fromRoot.witnesses.contains(
        WitnessDescriptorId("diagonal_lane_only"),
        WitnessAnchor.RayAnchor(WitnessRay(Square.fromKey("d4").get, WitnessDirection.NorthEast))
      )
    )

  test("CommentaryCore preserves fail-closed exact-board input discipline"):
    val illegalFenText = "8/8/8/8/8/8/4k3/4K3 w - - 0 1"
    val illegalFen = Fen.Full.clean(illegalFenText)

    assert(CommentaryCore.extractUWitnessesFailClosed(illegalFen).isLeft)
    assert(CommentaryCore.extractUWitnessesFromFenFailClosed(illegalFenText).isLeft)

  test("CommentaryCore exposes the one live U-attached descriptor and keeps shell-only rows out of standalone public registration"):
    assertEquals(CommentaryCore.activeUAttachedDescriptorIds, Vector("structural_space_claim"))

  test("CommentaryCore publishes the frozen Object 7 family ids"):
    assertEquals(
      CommentaryCore.activeObjectFamilyIds,
      Vector(
        "OpeningDevelopmentRegime",
        "DistributedContactRegime",
        "EndgameRaceScaffold",
        "AttackScaffold",
        "FortressHoldingShell",
        "KingSafetyShell",
        "CentralContactFront"
      )
    )

  test("CommentaryCore publishes the frozen Delta 2 family ids"):
    assertEquals(
      CommentaryCore.activeDeltaFamilyIds,
      Vector(
        "TradeCompressionCorridor",
        "TradeInvariant"
      )
    )

  test("CommentaryCore exposes a closed-center structural_space_claim through the public rootState, Fen, and String attached facades"):
    val attachedFenText = "4k3/8/8/3ppN2/3PP3/7B/8/4K3 w - - 0 1"
    val attachedFen = Fen.Full.clean(attachedFenText)
    val rootState = RootExtractor.fromFen(attachedFen).fold(message => fail(message), identity)
    val fromRoot = CommentaryCore.extractUAttachedWitnesses(rootState)
    val fromFen = CommentaryCore.extractUAttachedWitnesses(attachedFen).fold(message => fail(message), identity)
    val fromString =
      CommentaryCore.extractUAttachedWitnessesFromFen(attachedFenText).fold(message => fail(message), identity)

    assertEquals(fromRoot.witnesses, fromFen.witnesses)
    assertEquals(fromFen.witnesses, fromString.witnesses)
    assertEquals(
      fromRoot.witnesses.all.map(_.descriptorId).distinct,
      Vector(WitnessDescriptorId("structural_space_claim"))
    )

    assertEquals(
      fromRoot.witnesses.forDescriptorId(WitnessDescriptorId("structural_space_claim")).size,
      1
    )
    val whiteClaim =
      fromRoot.witnesses.forDescriptorId(WitnessDescriptorId("structural_space_claim")).find: witness =>
        witness.anchor == WitnessAnchor.SectorAnchor(WitnessSector.Center) &&
          witness.color.contains(Color.White) &&
          witness.variant.contains(WitnessVariantId("closed_center_host"))

    assert(whiteClaim.nonEmpty)
    assertEquals(
      squareList(whiteClaim.get.payload, "claimed_squares").map(_.key),
      Vector("d6", "e7")
    )
    assertEquals(
      fromRoot.witnesses.contains(
        WitnessDescriptorId("structural_space_claim"),
        WitnessAnchor.SectorAnchor(WitnessSector.Center),
        color = Some(Color.White),
        variant = Some(WitnessVariantId("closed_center_host"))
      ),
      true
    )

  test("CommentaryCore exposes a fixed-chain structural_space_claim through the public attached facade"):
    val attachedFenText = "k7/6p1/5pP1/5P1N/2B3p1/5pP1/5P2/K7 w - - 0 1"
    val attachedFen = Fen.Full.clean(attachedFenText)
    val rootState = RootExtractor.fromFen(attachedFen).fold(message => fail(message), identity)
    val fromRoot = CommentaryCore.extractUAttachedWitnesses(rootState)
    val fromFen = CommentaryCore.extractUAttachedWitnesses(attachedFen).fold(message => fail(message), identity)
    val fromString =
      CommentaryCore.extractUAttachedWitnessesFromFen(attachedFenText).fold(message => fail(message), identity)

    assertEquals(fromRoot.witnesses, fromFen.witnesses)
    assertEquals(fromFen.witnesses, fromString.witnesses)
    assertEquals(
      fromRoot.witnesses.all.map(_.descriptorId).distinct,
      Vector(WitnessDescriptorId("structural_space_claim"))
    )

    val whiteClaim =
      fromRoot.witnesses.forDescriptorId(WitnessDescriptorId("structural_space_claim")).find: witness =>
        witness.anchor == WitnessAnchor.SectorAnchor(WitnessSector.Kingside) &&
          witness.color.contains(Color.White) &&
          witness.variant.contains(WitnessVariantId("fixed_chain_host_white_segment"))

    assert(whiteClaim.nonEmpty)
    assertEquals(
      squareList(whiteClaim.get.payload, "boundary_pawn_squares").map(_.key),
      Vector("f5", "g6")
    )
    assert(squareList(whiteClaim.get.payload, "claimed_squares").map(_.key).contains("f7"))
    assert(squareList(whiteClaim.get.payload, "claimed_squares").map(_.key).contains("g8"))

  test("CommentaryCore preserves fail-closed exact-board input discipline for attached witnesses"):
    val illegalFenText = "8/8/8/8/8/8/4k3/4K3 w - - 0 1"
    val illegalFen = Fen.Full.clean(illegalFenText)

    assert(CommentaryCore.extractUAttachedWitnessesFailClosed(illegalFen).isLeft)
    assert(CommentaryCore.extractUAttachedWitnessesFromFenFailClosed(illegalFenText).isLeft)

  test("CommentaryCore exposes strategic objects through the public rootState, Fen, and String facades"):
    val objectFenText = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 2 3"
    val objectFen = Fen.Full.clean(objectFenText)
    val rootState = RootExtractor.fromFen(objectFen).fold(message => fail(message), identity)
    val fromRoot = CommentaryCore.extractStrategicObjects(rootState)
    val fromFen = CommentaryCore.extractStrategicObjects(objectFen).fold(message => fail(message), identity)
    val fromString =
      CommentaryCore.extractStrategicObjectsFromFen(objectFenText).fold(message => fail(message), identity)

    assertEquals(fromRoot.objects, fromFen.objects)
    assertEquals(fromFen.objects, fromString.objects)
    assert(
      fromRoot.objects.contains(
        "OpeningDevelopmentRegime",
        WitnessAnchor.BoardAnchor,
        None
      )
    )

  test("CommentaryCore preserves fail-closed exact-board input discipline for strategic objects"):
    val illegalFenText = "8/8/8/8/8/8/4k3/4K3 w - - 0 1"
    val illegalFen = Fen.Full.clean(illegalFenText)

    assert(CommentaryCore.extractStrategicObjectsFailClosed(illegalFen).isLeft)
    assert(CommentaryCore.extractStrategicObjectsFromFenFailClosed(illegalFenText).isLeft)

  test("CommentaryCore exposes strategic deltas through the public extraction facades"):
    val beforeFenText = "4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"
    val afterFenText = "4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1"
    val beforeFen = Fen.Full.clean(beforeFenText)
    val afterFen = Fen.Full.clean(afterFenText)
    val move = Uci("d6c7").get.asInstanceOf[Uci.Move]
    val beforeExtraction =
      CommentaryCore.extractStrategicObjects(beforeFen).fold(message => fail(message), identity)
    val afterExtraction =
      CommentaryCore.extractStrategicObjects(afterFen).fold(message => fail(message), identity)
    val fromObjects =
      CommentaryCore
        .extractStrategicDeltas(beforeExtraction, afterExtraction, move)
        .fold(message => fail(message), identity)
    val fromFens =
      CommentaryCore.extractStrategicDeltas(beforeFen, move, afterFen).fold(message => fail(message), identity)
    val fromStrings =
      CommentaryCore
        .extractStrategicDeltasFromFens(beforeFenText, "d6c7", afterFenText)
        .fold(message => fail(message), identity)

    assertEquals(fromObjects.deltas, fromFens.deltas)
    assertEquals(fromFens.deltas, fromStrings.deltas)
    assert(
      fromObjects.deltas.contains(
        "TradeInvariant",
        WitnessAnchor.BoardAnchor,
        color = Some(Color.White),
        scope = Some(StrategicDeltaScope.MoveLocal),
        deltaTag = Some(StrategicDeltaTag("bounded_favorable_simplification"))
      )
    )
    assertEquals(
      fromObjects.deltas.forFamilyId("TradeCompressionCorridor"),
      Vector.empty
    )

    val corridorBeforeFenText = "4k3/3r4/8/3n4/3R4/8/8/4K3 w - - 0 1"
    val corridorAfterFenText = "4k3/3r4/8/3R4/8/8/8/4K3 b - - 0 1"
    val corridorBeforeFen = Fen.Full.clean(corridorBeforeFenText)
    val corridorAfterFen = Fen.Full.clean(corridorAfterFenText)
    val corridorMove = Uci("d4d5").get.asInstanceOf[Uci.Move]
    val corridorBeforeExtraction =
      CommentaryCore.extractStrategicObjects(corridorBeforeFen).fold(message => fail(message), identity)
    val corridorAfterExtraction =
      CommentaryCore.extractStrategicObjects(corridorAfterFen).fold(message => fail(message), identity)
    val corridorFromObjects =
      CommentaryCore
        .extractStrategicDeltas(corridorBeforeExtraction, corridorAfterExtraction, corridorMove)
        .fold(message => fail(message), identity)
    val corridorFromFens =
      CommentaryCore
        .extractStrategicDeltas(corridorBeforeFen, corridorMove, corridorAfterFen)
        .fold(message => fail(message), identity)
    val corridorFromStrings =
      CommentaryCore
        .extractStrategicDeltasFromFens(corridorBeforeFenText, "d4d5", corridorAfterFenText)
        .fold(message => fail(message), identity)

    assertEquals(corridorFromObjects.deltas, corridorFromFens.deltas)
    assertEquals(corridorFromFens.deltas, corridorFromStrings.deltas)
    assert(
      corridorFromObjects.deltas.contains(
        "TradeCompressionCorridor",
        WitnessAnchor.BoardAnchor,
        color = Some(Color.White),
        scope = Some(StrategicDeltaScope.MoveLocal),
        deltaTag = Some(StrategicDeltaTag("transition_compression"))
      )
    )

  test("CommentaryCore public delta facade keeps corridor and invariant mutually exclusive on the overlapping board"):
    val beforeFenText = "4k3/3r4/P7/3n4/3R3p/8/8/4K3 w - - 0 1"
    val afterFenText = "4k3/3r4/P7/3R4/7p/8/8/4K3 b - - 0 1"
    val beforeFen = Fen.Full.clean(beforeFenText)
    val afterFen = Fen.Full.clean(afterFenText)
    val move = Uci("d4d5").get.asInstanceOf[Uci.Move]
    val beforeExtraction =
      CommentaryCore.extractStrategicObjects(beforeFen).fold(message => fail(message), identity)
    val afterExtraction =
      CommentaryCore.extractStrategicObjects(afterFen).fold(message => fail(message), identity)
    val fromObjects =
      CommentaryCore
        .extractStrategicDeltas(beforeExtraction, afterExtraction, move)
        .fold(message => fail(message), identity)
    val fromFens =
      CommentaryCore.extractStrategicDeltas(beforeFen, move, afterFen).fold(message => fail(message), identity)
    val fromStrings =
      CommentaryCore
        .extractStrategicDeltasFromFens(beforeFenText, "d4d5", afterFenText)
        .fold(message => fail(message), identity)

    assertEquals(fromObjects.deltas, fromFens.deltas)
    assertEquals(fromFens.deltas, fromStrings.deltas)
    assertEquals(fromObjects.deltas.all.map(_.familyId.value), Vector("TradeInvariant"))
    assert(
      fromObjects.deltas.contains(
        "TradeInvariant",
        WitnessAnchor.BoardAnchor,
        color = Some(Color.White),
        scope = Some(StrategicDeltaScope.MoveLocal),
        deltaTag = Some(StrategicDeltaTag("bounded_favorable_simplification"))
      )
    )

  test("CommentaryCore keeps TradeCompressionCorridor live when only the board-level race scaffold persists but TradeInvariant carrier continuity fails"):
    val beforeFenText = "3bk3/r2n4/4P3/4P3/R6p/8/6N1/4K3 w - - 0 1"
    val afterFenText = "3bk3/r2P4/8/4P3/R6p/8/6N1/4K3 b - - 0 1"

    val extraction =
      CommentaryCore
        .extractStrategicDeltasFromFens(beforeFenText, "e6d7", afterFenText)
        .fold(message => fail(message), identity)

    assert(
      extraction.deltas.contains(
        "TradeCompressionCorridor",
        WitnessAnchor.BoardAnchor,
        color = Some(Color.White),
        scope = Some(StrategicDeltaScope.MoveLocal),
        deltaTag = Some(StrategicDeltaTag("transition_compression"))
      )
    )
    assertEquals(extraction.deltas.forFamilyId("TradeInvariant"), Vector.empty)
    assertEquals(extraction.deltas.all.map(_.familyId.value), Vector("TradeCompressionCorridor"))

  test("CommentaryCore preserves fail-closed exact-board input discipline for strategic deltas"):
    val beforeFenText = "4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"
    val afterFenText = "4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1"

    assert(
      CommentaryCore
        .extractStrategicDeltasFromFensFailClosed(beforeFenText, "not-a-move", afterFenText)
        .isLeft
    )
    assert(
      CommentaryCore
        .extractStrategicDeltasFromFensFailClosed(beforeFenText, "d6c7", beforeFenText)
        .isLeft
    )
