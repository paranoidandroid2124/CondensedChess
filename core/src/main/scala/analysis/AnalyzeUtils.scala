package chess
package analysis

import chess.format.Uci

object AnalyzeUtils:
  private val pieceValues: Map[Role, Double] = Map(
    Pawn   -> 1.0,
    Knight -> 3.0,
    Bishop -> 3.0,
    Rook   -> 5.0,
    Queen  -> 9.0,
    King   -> 0.0
  )

  def clamp01(d: Double): Double = math.max(0.0, math.min(1.0, d))
  def pctInt(d: Double): Int = math.round(if d > 1.0 then d else d * 100.0).toInt

  def moveLabel(ply: Ply, san: String, turn: Color): String =
    val moveNumber = (ply.value + 1) / 2
    val sep = if turn == Color.White then "." else "..."
    s"$moveNumber$sep $san"

  def material(board: Board, color: Color): Double =
    board.piecesOf(color).toList.map { case (_, piece) => pieceValues.getOrElse(piece.role, 0.0) }.sum

  def pvToSan(fen: String, pv: List[String], maxPlies: Int = 5): List[String] =
    val fullFen: chess.format.FullFen = chess.format.Fen.Full.clean(fen)
    val startGame: chess.Game = chess.Game(chess.variant.Standard, Some(fullFen))
    pv.take(maxPlies).foldLeft((List.empty[String], startGame)) {
      case ((sans, g), uciStr) =>
        Uci(uciStr) match
          case Some(uci: Uci.Move) =>
            g.apply(uci) match
              case Right((nextGame, _)) =>
                val san = nextGame.sans.lastOption.map(_.value).getOrElse(uciStr)
                (sans :+ san, nextGame)
              case _ => (sans, g)
          case _ => (sans, g)
    }._1

  def uciToSanSingle(fen: String, uciStr: String): String =
    pvToSan(fen, List(uciStr), maxPlies = 1).headOption.getOrElse(uciStr)

  def escape(in: String): String =
    val sb = new StringBuilder(in.length + 8)
    in.foreach {
      case '"' => sb.append("\\\"")
      case '\\' => sb.append("\\\\")
      case '\n' => sb.append("\\n")
      case '\r' => sb.append("\\r")
      case '\t' => sb.append("\\t")
      case c => sb.append(c)
    }
    sb.result()
