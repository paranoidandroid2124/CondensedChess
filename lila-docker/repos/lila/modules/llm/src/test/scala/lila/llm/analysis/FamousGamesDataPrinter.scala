package lila.llm.analysis

import munit.FunSuite
import chess.{ Board, Color }
import chess.format.Fen
import chess.variant.Standard
import lila.llm.analysis.strategic._

class FamousGamesDataPrinter extends FunSuite {

  val structure = new StructureAnalyzerImpl()
  val activity = new ActivityAnalyzerImpl()

  def boardFromFen(fen: String): Board = 
    Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(throw new RuntimeException("INvalid FEN"))

  def printRaw(title: String, fen: String, color: Color): Unit = {
    println(s"\n=== $title ===")
    println(s"FEN: $fen")
    println(s"Perspective: $color")
    
    val board = boardFromFen(fen)
    
    // 1. Structure & Position
    val struct = structure.analyze(board)
    val pos = structure.detectPositionalFeatures(board, color)
    
    println("\n[Structural & Positional Features]")
    if (struct.isEmpty && pos.isEmpty) println("  (None)")
    struct.foreach(s => println(s"  - WeakComplex: $s"))
    pos.foreach(p => println(s"  - PositionalTag: $p"))

    // 2. Activity
    val act = activity.analyze(board, color)
    println(s"\n[Piece Activity] (Avg Mobility: ${if(act.nonEmpty) act.map(_.mobilityScore).sum / act.length else 0})")
    act.filter(a => a.mobilityScore > 0.8 || a.isTrapped || a.isBadBishop).foreach { a =>
      println(s"  - $a")
    }

    // 3. Compensation
    val comp = structure.analyzeCompensation(board, color)
    println("\n[Compensation]")
    comp match {
      case Some(c) => println(s"  - $c")
      case None => println("  (None)")
    }
    println("==================================================\n")
  }

  test("Print Raw Data for Famous Games") {
    // 1. Morphy: Move 12 (Open File)
    printRaw("Morphy vs Duke (Move 12)", 
      "2kr1b1r/p1pn1ppp/2p1q3/1B4B1/4P3/8/PPP2PPP/2KR3R b - - 3 12", 
      Color.White)

    // 2. Fischer: Move 16 (Outpost)
    printRaw("Byrne vs Fischer (Move 16)", 
      "r3r1k1/pp3pbp/1qp3p1/2B5/2B1n3/2n2N2/PPP2PPP/3R1K1R w - - 0 16", 
      Color.Black)

    // 3. Kasparov: Move 24 (Compensation - Constructed with material deficit)
    printRaw("Kasparov vs Topalov (Move 24 - Compensation)", 
      "r6r/5p1p/k3pnp1/7q/1PQN4/5P2/6PP/1K3B2 w - - 0 1", 
      Color.White)
  }
}
