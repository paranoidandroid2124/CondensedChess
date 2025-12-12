package chess
package analysis

import chess.format.Fen

object AnalyzeUtils:

  def round2(d: Double): Double = math.round(d * 100.0) / 100.0

  def escape(raw: String): String =
    raw.replace("\"", "\\\"").replace("\n", " ")

  /** Convert a single UCI move to SAN given a starting FEN. Falls back to UCI on parse failure. */
  def uciToSanSingle(fen: String, uci: String): String =
    val fullFen = Fen.Full.clean(fen)
    val game = chess.Game(chess.variant.Standard, Some(fullFen))
    chess.format.Uci(uci)
      .flatMap(game(_).toOption)
      .map(_._2.toSanStr.value)
      .getOrElse(uci)

  /** Convert a PV (UCIs) to SAN list from a starting FEN. Keeps original UCI if conversion fails mid-line. */
  def pvToSan(fen: String, pv: List[String]): List[String] =
    val fullFen = Fen.Full.clean(fen)
    val start = chess.Game(chess.variant.Standard, Some(fullFen))

    def loop(game: chess.Game, moves: List[String], acc: List[String]): List[String] =
      moves match
        case Nil => acc.reverse
        case uciStr :: rest =>
          chess.format.Uci(uciStr) match
            case Some(uci) =>
              game(uci) match
                case Right((next, moveOrDrop)) =>
                  val san = moveOrDrop.toSanStr.value
                  loop(next, rest, san :: acc)
                case _ =>
                  loop(game, rest, uciStr :: acc)
            case None =>
              loop(game, rest, uciStr :: acc)

    loop(start, pv, Nil)

  def pctInt(d: Double): Int = math.round(d).toInt

  def moveLabel(ply: chess.Ply, san: String, turn: chess.Color): String =
    val fullMove = (ply.value / 2) + 1
    if turn == chess.Color.White then s"$fullMove. $san" else s"$fullMove... $san"

  def clamp01(d: Double): Double = math.max(0.0, math.min(1.0, d))

  def materialScore(fen: String): Double = 0.0
