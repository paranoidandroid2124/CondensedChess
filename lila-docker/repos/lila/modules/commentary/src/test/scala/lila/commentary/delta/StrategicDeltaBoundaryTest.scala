package lila.commentary.delta

import chess.Color
import chess.format.{ Fen, Uci }

class StrategicDeltaBoundaryTest extends munit.FunSuite:

  test("StrategicDeltaScopeContract freezes both delta families together"):
    assertEquals(
      StrategicDeltaScopeContract.activeDeltaFamilyIds.map(_.value),
      Vector("TradeCompressionCorridor", "TradeInvariant")
    )

  test("StrategicDeltaExtractor emits TradeCompressionCorridor through both live entrypoints"):
    val move = moveUci("d4d5")
    val fromFens =
      StrategicDeltaExtractor
        .fromFens(
          Fen.Full.clean("4k3/3r4/8/3n4/3R4/8/8/4K3 w - - 0 1"),
          move,
          Fen.Full.clean("4k3/3r4/8/3R4/8/8/8/4K3 b - - 0 1")
        )
        .fold(message => fail(message), identity)
    val fromExtractions =
      StrategicDeltaExtractor
        .fromExtractions(fromFens.before, fromFens.after, move)
        .fold(message => fail(message), identity)

    assertEquals(fromExtractions, fromFens)
    assert(
      fromFens.deltas.contains(
        "TradeCompressionCorridor",
        lila.commentary.witness.WitnessAnchor.BoardAnchor,
        color = Some(Color.White),
        scope = Some(StrategicDeltaScope.MoveLocal),
        deltaTag = Some(StrategicDeltaTag("transition_compression"))
      )
    )

  test("StrategicDeltaExtractor emits TradeInvariant through both live entrypoints"):
    val move = moveUci("d6c7")
    val fromFens =
      StrategicDeltaExtractor
        .fromFens(
          Fen.Full.clean("4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1"),
          move,
          Fen.Full.clean("4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1")
        )
        .fold(message => fail(message), identity)
    val fromExtractions =
      StrategicDeltaExtractor
        .fromExtractions(fromFens.before, fromFens.after, move)
        .fold(message => fail(message), identity)

    assertEquals(fromExtractions, fromFens)
    assert(
      fromFens.deltas.contains(
        "TradeInvariant",
        lila.commentary.witness.WitnessAnchor.BoardAnchor,
        color = Some(Color.White),
        scope = Some(StrategicDeltaScope.MoveLocal),
        deltaTag = Some(StrategicDeltaTag("bounded_favorable_simplification"))
      )
    )

  private def moveUci(uci: String): Uci.Move =
    Uci(uci) match
      case Some(move: Uci.Move) => move
      case Some(_) => fail(s"Unsupported non-move UCI: $uci")
      case None => fail(s"Invalid UCI: $uci")
