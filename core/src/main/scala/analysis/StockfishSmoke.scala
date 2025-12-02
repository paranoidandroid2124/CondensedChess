package chess
package analysis

/** Stockfish 래퍼 스모크 테스트용 CLI.
  *
  * 사용법: sbt "scalachess/runMain chess.analysis.StockfishSmoke [fen] [depth] [multipv]"
  */
object StockfishSmoke:

  def main(args: Array[String]): Unit =
    val fen = args.lift(0).getOrElse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    val depth = args.lift(1).flatMap(_.toIntOption).getOrElse(8)
    val multiPv = args.lift(2).flatMap(_.toIntOption).getOrElse(3)
    val client = new StockfishClient()
    client.evaluateFen(fen, depth = depth, multiPv = multiPv) match
      case Left(err) =>
        System.err.println(s"engine error: $err")
        sys.exit(1)
      case Right(res) =>
        println(s"bestmove=${res.bestmove.getOrElse("")}")
        res.lines.foreach { l =>
          val score = l.cp.map(cp => s"cp=$cp").orElse(l.mate.map(m => s"mate=$m")).getOrElse("n/a")
          println(s"pv${l.multiPv}@d${l.depth} $score win=${"%.1f".format(l.winPercent)} pv=${l.pv.mkString(" ")}")
        }
