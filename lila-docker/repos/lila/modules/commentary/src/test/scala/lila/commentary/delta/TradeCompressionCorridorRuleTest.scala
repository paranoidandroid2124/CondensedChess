package lila.commentary.delta

import chess.{ Color, Square }
import chess.format.{ Fen, Uci }

import lila.commentary.strategic.{ StrategicObjectExtraction, StrategicObjectExtractor }
import lila.commentary.witness.{ WitnessAnchor, WitnessValue }

import play.api.libs.json.Json

class TradeCompressionCorridorRuleTest extends munit.FunSuite:

  private val rows = loadTradeCompressionRows()

  test("trade compression corridor matches the frozen exact corpus row"):
    val deltas = extract(rows("delta-trade-compression-exact"))

    assertEquals(deltas.size, 1)

    val delta = deltas.head
    assertEquals(delta.familyId.value, "TradeCompressionCorridor")
    assertEquals(delta.scope, StrategicDeltaScope.MoveLocal)
    assertEquals(delta.deltaTag.value, "transition_compression")
    assertEquals(delta.anchor, WitnessAnchor.BoardAnchor)
    assertEquals(delta.color, Some(Color.White))
    assertEquals(
      delta.payload.get("corridorPairSquares"),
      Some(WitnessValue.SquareListValue(Vector(square("d5"), square("d7"))))
    )
    assertEquals(
      delta.payload.get("corridorKind"),
      Some(WitnessValue.Token("file"))
    )
    assertEquals(
      delta.payload.get("captureSquare"),
      Some(WitnessValue.SquareValue(square("d5")))
    )
    assertEquals(
      delta.payload.get("capturedRole"),
      Some(WitnessValue.RoleValue(chess.Knight))
    )
    assertEquals(delta.support.targetSquares, Vector(square("d5"), square("d7")))
    assertEquals(
      delta.support.supportingTags,
      Vector(
        "compressed_trade_window",
        "reciprocal_exchange_corridor",
        "trade_compression_transition"
      )
    )

  test("trade compression corridor also emits on the frozen diagonal exact row"):
    val deltas = extract(rows("delta-trade-compression-diagonal-exact"))

    assertEquals(deltas.size, 1)

    val delta = deltas.head
    assertEquals(delta.familyId.value, "TradeCompressionCorridor")
    assertEquals(delta.scope, StrategicDeltaScope.MoveLocal)
    assertEquals(delta.deltaTag.value, "transition_compression")
    assertEquals(delta.anchor, WitnessAnchor.BoardAnchor)
    assertEquals(delta.color, Some(Color.White))
    assertEquals(
      delta.payload.get("corridorPairSquares"),
      Some(WitnessValue.SquareListValue(Vector(square("c4"), square("e6"))))
    )
    assertEquals(
      delta.payload.get("corridorKind"),
      Some(WitnessValue.Token("diagonal"))
    )
    assertEquals(
      delta.payload.get("captureSquare"),
      Some(WitnessValue.SquareValue(square("c4")))
    )
    assertEquals(
      delta.payload.get("capturedRole"),
      Some(WitnessValue.RoleValue(chess.Knight))
    )
    assertEquals(delta.support.targetSquares, Vector(square("c4"), square("e6")))

  test("trade compression corridor rejects the frozen near-miss with no surviving shared corridor"):
    assertEquals(extract(rows("delta-trade-compression-near-miss")), Vector.empty[StrategicDelta])

  test("trade compression corridor rejects the frozen nasty negative with queens still on the board"):
    assertEquals(extract(rows("delta-trade-compression-nasty-negative")), Vector.empty[StrategicDelta])

  test("trade compression corridor rejects the frozen nasty negative when the corridor was already true before the trade"):
    assertEquals(
      extract(rows("delta-trade-compression-already-true-before")),
      Vector.empty[StrategicDelta]
    )

  test("trade compression corridor rejects the frozen move-local false witness with no trade reduction"):
    assertEquals(extract(rows("delta-trade-compression-false-witness")), Vector.empty[StrategicDelta])

  test("trade compression corridor rejects a queenless corridor that still leaves too many major or minor pieces on the board"):
    assertEquals(
      extract(rows("delta-trade-compression-overcrowded-window")),
      Vector.empty[StrategicDelta]
    )

  test("trade compression corridor rejects a capture that leaves more than one reciprocal corridor pair after the trade"):
    assertEquals(
      extract(rows("delta-trade-compression-multi-corridor")),
      Vector.empty[StrategicDelta]
    )

  test("trade compression corridor rejects the forbidden TradeInvariant rival even when the corridor shape is present"):
    val beforeFen = Fen.Full.clean("4k3/3r4/P7/3n4/3R3p/8/8/4K3 w - - 0 1")
    val afterFen = Fen.Full.clean("4k3/3r4/P7/3R4/7p/8/8/4K3 b - - 0 1")
    val move = moveUci("d4d5")

    val beforeExtraction = extraction(beforeFen)
    val afterExtraction = extraction(afterFen)

    assert(beforeExtraction.objects.forFamilyId("EndgameRaceScaffold").nonEmpty)
    assert(afterExtraction.objects.forFamilyId("EndgameRaceScaffold").nonEmpty)

    val context =
      StrategicDeltaContext
        .build(beforeExtraction, afterExtraction, move)
        .fold(message => fail(message), identity)

    assertEquals(
      TradeCompressionCorridorRule.extract(context, StrategicDeltaSet.empty),
      Vector.empty[StrategicDelta]
    )

  test("trade compression corridor stays live when EndgameRaceScaffold persists but the actual TradeInvariant carrier continuity fails"):
    val beforeFen = Fen.Full.clean("3bk3/r2n4/4P3/4P3/R6p/8/6N1/4K3 w - - 0 1")
    val afterFen = Fen.Full.clean("3bk3/r2P4/8/4P3/R6p/8/6N1/4K3 b - - 0 1")
    val move = moveUci("e6d7")

    val beforeExtraction = extraction(beforeFen)
    val afterExtraction = extraction(afterFen)

    assert(beforeExtraction.objects.forFamilyId("EndgameRaceScaffold").nonEmpty)
    assert(afterExtraction.objects.forFamilyId("EndgameRaceScaffold").nonEmpty)

    val context =
      StrategicDeltaContext
        .build(beforeExtraction, afterExtraction, move)
        .fold(message => fail(message), identity)

    assertEquals(TradeInvariantRule.extract(context, StrategicDeltaSet.empty), Vector.empty[StrategicDelta])

    val deltas = TradeCompressionCorridorRule.extract(context, StrategicDeltaSet.empty)
    assertEquals(deltas.size, 1)
    assertEquals(deltas.head.familyId.value, "TradeCompressionCorridor")
    assertEquals(
      deltas.head.payload.get("corridorPairSquares"),
      Some(WitnessValue.SquareListValue(Vector(square("a4"), square("a7"))))
    )

  private def extract(row: TradeCompressionRow): Vector[StrategicDelta] =
    val context =
      StrategicDeltaContext
        .build(extraction(row.fenBefore), extraction(row.fenAfter), moveUci(row.playedMove))
        .fold(message => fail(message), identity)
    TradeCompressionCorridorRule.extract(context, StrategicDeltaSet.empty)

  private def extraction(fen: Fen.Full): StrategicObjectExtraction =
    StrategicObjectExtractor.fromFen(fen).fold(message => fail(message), identity)

  private def moveUci(uci: String): Uci.Move =
    Uci(uci) match
      case Some(move: Uci.Move) => move
      case Some(_) => fail(s"Unsupported non-move UCI: $uci")
      case None => fail(s"Invalid UCI: $uci")

  private def square(key: String): Square =
    Square.fromKey(key).getOrElse(fail(s"Invalid square key: $key"))

  private def loadTradeCompressionRows(): Map[String, TradeCompressionRow] =
    val stream =
      Option(getClass.getResourceAsStream("/commentary-corpus/delta-expectations.jsonl"))
        .getOrElse(fail("Missing /commentary-corpus/delta-expectations.jsonl"))
    val source = scala.io.Source.fromInputStream(stream)
    try
      source
        .getLines()
        .filter(_.nonEmpty)
        .map(Json.parse)
        .flatMap: json =>
          Option.when((json \ "family").as[String] == "TradeCompressionCorridor"):
            TradeCompressionRow(
              id = (json \ "id").as[String],
              fenBefore = Fen.Full.clean((json \ "fenBefore").as[String]),
              playedMove = (json \ "playedMove").as[String],
              fenAfter = Fen.Full.clean((json \ "fenAfter").as[String])
            )
        .map(row => row.id -> row)
        .toMap
    finally source.close()

  private final case class TradeCompressionRow(
      id: String,
      fenBefore: Fen.Full,
      playedMove: String,
      fenAfter: Fen.Full
  )
