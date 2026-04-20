package lila.commentary.delta

import chess.Color
import chess.format.{ Fen, Uci }

import lila.commentary.strategic.StrategicObjectExtractor
import lila.commentary.witness.{ WitnessAnchor, WitnessValue }

import play.api.libs.json.Json

class TradeInvariantRuleTest extends munit.FunSuite:

  private val rows = loadTradeInvariantRows()

  test("trade invariant matches the frozen exact corpus row"):
    val deltas = extract(rows("delta-trade-invariant-exact"))

    assertEquals(deltas.size, 1)

    val delta = deltas.head
    assertEquals(delta.familyId.value, "TradeInvariant")
    assertEquals(delta.scope, StrategicDeltaScope.MoveLocal)
    assertEquals(delta.deltaTag.value, "bounded_favorable_simplification")
    assertEquals(delta.anchor, WitnessAnchor.BoardAnchor)
    assertEquals(delta.color, Some(Color.White))
    assertEquals(
      delta.payload.get("persistent_carrier_family"),
      Some(WitnessValue.Token("EndgameRaceScaffold"))
    )
    assertEquals(
      delta.payload.get("persistent_carrier_anchor"),
      Some(WitnessValue.Token("board"))
    )
    assertEquals(
      delta.payload.get("material_reduction"),
      Some(WitnessValue.Number(1))
    )
    assertEquals(delta.support.targetSquares.map(_.key), Vector("c7"))
    assert(delta.support.rootIndices.nonEmpty)
    assertEquals(
      delta.support.supportingTags,
      Vector(
        "bounded_material_reduction",
        "persistent_object_carrier",
        "trade_invariant_transition"
      )
    )

  test("trade invariant stays absent when the favorable trade does not preserve the carrier after the move"):
    assertEquals(extract(rows("delta-trade-invariant-near-miss")), Vector.empty[StrategicDelta])

  test("trade invariant stays absent when the trade only creates the carrier after the move"):
    assertEquals(
      extract(rows("delta-trade-invariant-after-only-carrier")),
      Vector.empty[StrategicDelta]
    )

  test("trade invariant stays absent for a winning-looking capture that does not preserve any pre-existing carrier"):
    assertEquals(extract(rows("delta-trade-invariant-nasty-negative")), Vector.empty[StrategicDelta])

  test("trade invariant stays absent when the same task survives a quiet move without bounded material reduction"):
    assertEquals(extract(rows("delta-trade-invariant-false-witness")), Vector.empty[StrategicDelta])

  test("trade invariant stays absent when a pawn capture preserves the same carrier but does not reduce non-king non-pawn material"):
    assertEquals(
      extract(rows("delta-trade-invariant-pawn-capture-false-witness")),
      Vector.empty[StrategicDelta]
    )

  test("trade invariant stays absent when the board-level race scaffold survives but the mover's clear-run carrier switches to a different pawn"):
    assertEquals(extract(rows("delta-trade-invariant-carrier-switch")), Vector.empty[StrategicDelta])

  test("trade invariant rejects the forbidden TradeCompressionCorridor rival on the same board anchor"):
    val exact = rows("delta-trade-invariant-exact")
    val rival = StrategicDelta(
      familyId = StrategicDeltaId("TradeCompressionCorridor"),
      scope = StrategicDeltaScope.MoveLocal,
      deltaTag = StrategicDeltaTag("transition_compression"),
      anchor = WitnessAnchor.BoardAnchor,
      color = Some(Color.White)
    )

    val deltas = TradeInvariantRule.extract(context(exact), StrategicDeltaSet(Vector(rival)))

    assert(deltas.isEmpty)

  private def extract(row: TradeInvariantRow): Vector[StrategicDelta] =
    TradeInvariantRule.extract(context(row), StrategicDeltaSet.empty)

  private def context(row: TradeInvariantRow): StrategicDeltaContext =
    val beforeExtraction = extraction(row.fenBefore)
    val afterExtraction = extraction(row.fenAfter)
    val move = moveUci(row.playedMove)
    StrategicDeltaContext
      .build(beforeExtraction, afterExtraction, move)
      .fold(message => fail(message), identity)

  private def extraction(fen: Fen.Full) =
    StrategicObjectExtractor.fromFen(fen).fold(message => fail(message), identity)

  private def moveUci(uci: String): Uci.Move =
    Uci(uci) match
      case Some(move: Uci.Move) => move
      case Some(_) => fail(s"Expected move UCI, got $uci")
      case None => fail(s"Invalid move UCI $uci")

  private def loadTradeInvariantRows(): Map[String, TradeInvariantRow] =
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
          Option.when((json \ "family").as[String] == "TradeInvariant"):
            TradeInvariantRow(
              id = (json \ "id").as[String],
              fenBefore = Fen.Full.clean((json \ "fenBefore").as[String]),
              playedMove = (json \ "playedMove").as[String],
              fenAfter = Fen.Full.clean((json \ "fenAfter").as[String])
            )
        .map(row => row.id -> row)
        .toMap
    finally source.close()

  private final case class TradeInvariantRow(
      id: String,
      fenBefore: Fen.Full,
      playedMove: String,
      fenAfter: Fen.Full
  )
