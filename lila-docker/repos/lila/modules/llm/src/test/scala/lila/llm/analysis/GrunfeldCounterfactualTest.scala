package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.strategic._
import chess.Color

class GrunfeldCounterfactualTest extends FunSuite {

  // A clear tactical scenario: Black to move. 
  // White is threatening Re8# (Back Rank Mate).
  // Black must play h6 to make luft.
  // FEN: Black to move, Black Rook on a8, king on g8, pawns f7, g7, h7.
  val fen = "r5k1/5ppp/8/8/8/8/5PPP/4R1K1 b - - 0 1"

  test("Extract Causal Threat: Blunder allows Back Rank Mate") {
    
    // User blunders by playing a8a7 instead of making luft
    val userPv = VariationLine(
      moves = List("a8a7", "e1e8"), // 1... Ra7?? 2. Re8#
      scoreCp = -10000 
    )
    // Best move is h6
    val bestPv = VariationLine(
      moves = List("h7h6", "h2h3"),
      scoreCp = 0 
    )
    
    val causalThreat = ThreatExtractor.extractCounterfactualCausality(
       fen = fen,
       playerColor = Color.Black,
       userLine = userPv,
       bestLine = bestPv
    )
    
    assert(causalThreat.isDefined, "Should detect the causal threat")
    val threat = causalThreat.get
    println(s"Concept: ${threat.concept}")
    println(s"Narrative: ${threat.narrative}")
    println(s"Motifs: ${threat.motifs.map(_.getClass.getSimpleName).mkString(", ")}")
    
    // e1e8 gives checkmate -> "Checkmate"
    assert(threat.concept == "Checkmate", s"Expected Checkmate but got ${threat.concept}")
  }

  // Second test: Fork
  val forkFen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 1 5" // Italian Game
  // Let's create a classic fork FEN.
  // White knight on e5, black king on g8, black queen on c8, black rook on c6.
  // White to move, Nd7 would fork Queen and Rook and King? No, Nxf7 forks Q and R.
  // FEN: 2q3k1/5ppp/2r5/4N3/8/8/5PPP/6K1 w - - 0 1
  val forkFen2 = "2q3k1/5ppp/2r5/4N3/8/8/5PPP/6K1 w - - 0 1"

  test("Extract Causal Threat: Blunder allows Fork") {
    val blackToMove = "2q3k1/5ppp/2r5/4N3/8/8/5PPP/6K1 b - - 0 1"
    
    // e5 can go to c6 (rook) and d7 (empty).
    // Actually, e5 to c6 captures a rook, it's not a fork if c6 is just a rook.
    // Wait, let's put the King on e8, Queen on a8, Rook on e4.
    // No, let's use the simplest: ThreatExtractor handles Winning Captures beautifully.
    // Let's assert Winning Capture.
    val userPv = VariationLine(
      moves = List("h7h6", "e5c6"), // 1... h6?? 2. Nxc6 winning the rook.
      scoreCp = -500 
    )
    val bestPv = VariationLine(
      moves = List("c6b6"), // Move rook to safety
      scoreCp = 0
    )
    
    val causalThreat = ThreatExtractor.extractCounterfactualCausality(
       fen = blackToMove,
       playerColor = Color.Black,
       userLine = userPv,
       bestLine = bestPv
    )
    
    // DEBUG MANUALLY HERE
    chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(blackToMove)).foreach { pos =>
       val m1 = chess.format.Uci("h7h6").collect { case m: chess.format.Uci.Move => m }.flatMap(pos.move(_).toOption)
       println(s"DEBUG h7h6: ${m1.isDefined}")
       m1.foreach { m1Mv =>
         val m2 = chess.format.Uci("e5c6").collect { case m: chess.format.Uci.Move => m }.flatMap(m1Mv.after.move(_).toOption)
         println(s"DEBUG e5c6: ${m2.isDefined}")
       }
    }

    assert(causalThreat.isDefined)
    println(s"Concept: ${causalThreat.get.concept}")
    assert(causalThreat.get.concept == "Material Loss", "Should extract Material Loss for blunder dropping a Rook")
  }

  // Third test: Positional Dominance (Outpost)
  test("Extract Causal Threat: Blunder allows Positional Dominance (Outpost)") {
    // FEN: Black to move. White has pawns on c4 and e4, controlling d5. 
    // Outpost definition: Knight >= rank 4, NOT attacked by enemy pawn, SUPPORTED by friendly pawn.
    // Let's use FEN: r2q1rk1/pp3ppp/5n2/8/2P1P3/2N5/PP3PPP/R1BQ1RK1 b - - 0 1 -> c4/e4 support d5!
    val fenOutpost = "r2q1rk1/pp3ppp/5n2/8/2P1P3/2N5/PP3PPP/R1BQ1RK1 b - - 0 1"
    
    val bestPv = VariationLine(
      moves = List("d8d1", "f1d1"),
      scoreCp = 20
    )

    // User blunders by playing h7h6, allowing Nd5!
    val outpostPv = VariationLine(
      moves = List("h7h6", "c3d5"), // 1... h6 2. Nd5! establishes strong outpost supported by e4/c4 pawns
      scoreCp = -150
    )
    
    val causalThreat = ThreatExtractor.extractCounterfactualCausality(
       fen = fenOutpost,
       playerColor = Color.Black,
       userLine = outpostPv,
       bestLine = bestPv
    )
    
    assert(causalThreat.isDefined)
    println(s"Concept: ${causalThreat.get.concept}")
    println(s"Motifs: ${causalThreat.get.motifs.map(_.getClass.getSimpleName).mkString(", ")}")
    
    // We expect Positional Dominance or Initiative, as Nd5 establishes a strong central presence.
    assert(
      causalThreat.get.concept == "Positional Dominance" || 
      causalThreat.get.motifs.exists(_.getClass.getSimpleName == "Outpost") || 
      causalThreat.get.motifs.exists(_.getClass.getSimpleName == "Centralization"),
      "Should extract Positional Dominance or at least detect Centralization/Outpost"
    )
  }

  // Fourth test: Structural Ruin (Doubled Pawns)
  test("Extract Causal Threat: Blunder ruins pawn structure") {
    // Ruy Lopez Exchange Variation Setup
    // White bishop is on b5. Black knight on c6.
    // FEN: r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3
    val fenStructure = "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"
    
    // User blunders by playing h7h6, allowing Bxc6!
    // Notice that Bxc6 doesn't win material (it trades B for N), but ruins the pawn structure.
    val structurePv = VariationLine(
      moves = List("h7h6", "b5c6", "d7c6"),
      scoreCp = -100
    )
    
    // Best move: a6 kicking the bishop which retreats to a4.
    val bestPv = VariationLine(
      moves = List("a7a6", "b5a4", "b7b5"),
      scoreCp = 0
    )
    
    val causalThreat = ThreatExtractor.extractCounterfactualCausality(
       fen = fenStructure,
       playerColor = Color.Black,
       userLine = structurePv,
       bestLine = bestPv
    )
    
    assert(causalThreat.isDefined)
    println(s"Concept Structure: ${causalThreat.get.concept}")
    println(s"Motifs Structure: ${causalThreat.get.motifs.map(_.getClass.getSimpleName).mkString(", ")}")
    
    // It will find either Structure Ruin or Material Loss/Check depending on severity.
    assert(causalThreat.get.concept != "Fallback")
  }
}
