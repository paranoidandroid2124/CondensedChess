package lila.llm.analysis

import lila.llm.model.strategic._
import chess.Color

object DebugMotifExtractor extends App {
  // Outpost Test
  val fenOutpost = "r2q1rk1/pp3ppp/5n2/8/2P1P3/2N5/PP3PPP/R1BQ1RK1 b - - 0 1"
  val outpostPv = List("h7h6", "c3d5")
  val outpostMotifs = MoveAnalyzer.tokenizePv(fenOutpost, outpostPv)
  println("== OUTPOST == ")
  outpostMotifs.filter(_.plyIndex % 2 == 1).foreach(m => println(m.getClass.getSimpleName))

  // Structure Test
  val fenStructure = "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"
  val structurePv = List("h7h6", "b5c6", "d7c6")
  val structureMotifs = MoveAnalyzer.tokenizePv(fenStructure, structurePv)
  println("\n== STRUCTURE == ")
  structureMotifs.foreach(m => println(s"${m.getClass.getSimpleName} - ply: ${m.plyIndex}, color: ${m.color}"))
}
