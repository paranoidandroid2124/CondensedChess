package lila.llm.analysis

import munit.FunSuite
import chess.{ Board, Color }
import chess.format.Fen
import chess.variant.Standard
import lila.llm.model._
import lila.llm.analysis.strategic._
import lila.llm.model.strategic.{ VariationLine, VariationTag, PreventedPlan }

class MultiPvDemonstration extends FunSuite {

  // Factories - Correct verification of constructor order:
  // Prophylaxis, Activity, Structure, Endgame, Practicality
  val extractor = new StrategicFeatureExtractorImpl(
    new ProphylaxisAnalyzerImpl(),
    new ActivityAnalyzerImpl(),
    new StructureAnalyzerImpl(),
    new EndgameAnalyzerImpl(),
    new PracticalityScorerImpl()
  )

  val narrativeGen = lila.llm.NarrativeGenerator

  test("Demonstrate Multi-PV Candidate Comparison (Byrne vs Fischer)") {
    // 1. Setup: Game of the Century, Move 16 (Black to move)
    val currentFen = "r3r1k1/pp3pbp/1qp3p1/2B5/2B1n3/2n2N2/PPP2PPP/3R1K1R b - - 6 16"
    val prevMove = "Bc5"
    val ply = 32 // Move 16 black

    println(s"\n=== Multi-PV Simulation: Byrne vs Fischer (Move 16) ===")
    println(s"FEN: $currentFen")
    println(s"To Move: Black")

    // 2. Dummy Multi-PV Lines (What Stockfish WASM would provide)
    val candidates: List[VariationLine] = List(
      // Candidate 1 (Best): Rfe8+ (Forces King move, sets up Be6)
      VariationLine(
        moves = List("Rfe8+", "Kf1", "Be6"), 
        scoreCp = -250, // Black winning
        mate = None
      ),
      // Candidate 2 (Mistake): Qc7?
      VariationLine(
        moves = List("Qc7", "Bd4"), 
        scoreCp = 50, // White is slightly better/equal
        mate = None
      ),
      // Candidate 3 (Blunder): ...h6? (Loses)
      VariationLine(
        moves = List("h6", "Bxb6", "axb6"), 
        scoreCp = 800, // White winning
        mate = None
      )
    )

    // 3. Create Processing Data
    val baseData = BaseAnalysisData(
      nature = PositionNature(
          natureType = lila.llm.analysis.NatureType.Static, 
          tension = 0.0, 
          stability = 1.0, 
          description = "Demo Position"
      ),
      motifs = Nil,
      plans = Nil
    )
    
    val metadata = AnalysisMetadata(
      color = Color.Black,
      ply = ply, 
      prevMove = Some(prevMove)
    )

    // 4. Run Pipeline
    // extract(fen, metadata, baseData, vars, playedMove)
    val extendedData = extractor.extract(
        currentFen, 
        metadata, 
        baseData, 
        candidates, 
        playedMove = Some("Rfe8+")
    )

    // 5. Generate Narrative
    println("\n=== Generated Narrative Section: CANDIDATES ===")
    
    // We only want the CANDIDATE part of the narrative for this demo
    val narrativeText = narrativeGen.describeExtended(extendedData, lila.llm.NarrativeTemplates.Style.Coach)
    
    // Filter for candidate related text or just print everything if short
    // Actually describeExtended produces sections.
    val lines = narrativeText.split("\n")
    lines.foreach(println)

    
    // Also print the raw comparison data for the user
    println("\n=== Raw Comparison Data (For Debug/Proof) ===")
    extendedData.candidates.foreach { c =>
      println(s"Move: ${c.move} | Score: ${c.score} | Future: ${c.futureContext}")
    }
  }
}
