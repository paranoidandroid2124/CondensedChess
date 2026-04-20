package lila.commentary.delta

import chess.format.{ Fen, Uci }

import lila.commentary.strategic.{ StrategicObjectExtraction, StrategicObjectExtractor }

object StrategicDeltaExtractor:

  def fromExtractions(
      beforeExtraction: StrategicObjectExtraction,
      afterExtraction: StrategicObjectExtraction,
      playedMove: Uci.Move
  ): Either[String, StrategicDeltaExtraction] =
    StrategicDeltaContext
      .build(beforeExtraction, afterExtraction, playedMove)
      .map: context =>
        StrategicDeltaExtraction(
          before = beforeExtraction,
          after = afterExtraction,
          playedMove = playedMove,
          deltas = StrategicInternalDeltaRuntime.extract(context)
        )

  def fromFens(
      fenBefore: Fen.Full,
      playedMove: Uci.Move,
      fenAfter: Fen.Full
  ): Either[String, StrategicDeltaExtraction] =
    for
      beforeExtraction <- StrategicObjectExtractor.fromFen(fenBefore)
      afterExtraction <- StrategicObjectExtractor.fromFen(fenAfter)
      extraction <- fromExtractions(beforeExtraction, afterExtraction, playedMove)
    yield extraction

  def fromFensFailClosed(
      fenBefore: Fen.Full,
      playedMove: Uci.Move,
      fenAfter: Fen.Full
  ): Either[String, StrategicDeltaExtraction] =
    for
      beforeExtraction <- StrategicObjectExtractor.fromFenFailClosed(fenBefore)
      afterExtraction <- StrategicObjectExtractor.fromFenFailClosed(fenAfter)
      extraction <- fromExtractions(beforeExtraction, afterExtraction, playedMove)
    yield extraction
