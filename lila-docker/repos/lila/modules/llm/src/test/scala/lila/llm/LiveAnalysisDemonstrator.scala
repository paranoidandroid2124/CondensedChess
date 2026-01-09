package lila.llm

import lila.llm.analysis.*
import lila.llm.model.*
import lila.llm.model.strategic.*
import java.io.PrintWriter
import scala.sys.process.*
import play.api.libs.json.*

/**
 * Runs "REAL" analysis using Stockfish WASM.
 */
object LiveAnalysisDemonstrator {

  def main(args: Array[String]): Unit = {
    val fens = List(
      "r1bqk2r/pp2bppp/2nppn2/8/3NP3/2N1B3/PPP2PPP/R2QKB1R w KQkq - 2 8", // Richter-Rauzer
      "rn1qk2r/pp2bppp/2p1pn2/3p4/2PP4/2N1PN2/PP3PPP/R1BQKB1R w KQkq - 1 7", // QGD Slav
      "r1bqk2r/pp2ppbp/2np1np1/8/3NP3/2N1BP2/PPP3PP/R2QKB1R w KQkq - 1 8",  // Sicilian Dragon
      "r1bqkb1r/pp3ppp/2n5/1BppP3/3P4/2P2N2/P1P2PPP/R1BQK2R w KQkq - 0 8",  // Tarrasch Trap (Opening)
      "r1bq1rk1/1pp2ppp/p1np1n2/4p3/2B1P3/2PP1N2/PP3PPP/RN1QR1K1 w - - 0 10", // Marshall-ish
      "4k3/5ppp/8/8/8/6P1/5P1P/4K3 w - - 0 1",                             // Endgame Majority
      "r1b1k2r/ppqnbpp1/2ppbn1p/8/3NP2B/2N5/PPP1BPPP/R2QR1K1 w kq - 0 11", // Prophylaxis/Tactical
      "r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2N2N2/PP2BPPP/R1BQ1RK1 w - - 0 9",  // IQP
      "r4rk1/1b1q1ppp/p2p1n2/2p1pP2/2P1P3/2N1B3/PP1Q2PP/R4RK1 w - - 0 17", // Strategic Squeeze
      "rnbq1rk1/pp1p1pbp/5np1/2pP4/8/2N2N2/PP2PPPP/R1BQKB1R w KQ - 0 7",   // Benoni
      // PHASE 22.5: Critical/Tactical Additions
      "r1b2rk1/pp1nbppp/2p1pn2/q2p2B1/2PP4/2N1PN2/PPQ2PPP/2R1KB1R w K - 3 9", // Pin on Knight
      "r2q1rk1/1b2bppp/p3p3/1p1n4/3BN3/1P1B4/P1P1QPPP/R4RK1 w - - 1 15",    // Attacking Tension
      "rnbq1rk1/pp2ppbp/5np1/2pp4/2PP1B2/2N1PN2/PP3PPP/R2QKB1R w KQ - 0 8",  // Grunfeld Tension
      "4r3/1p1r1k1p/p1p2np1/P1Pp4/1R1Pp3/4P1P1/1P2BP1P/R5K1 w - - 0 26",     // Complex Rook Endgame
      "8/2p5/1p1p4/k2P4/2P5/2P5/2K5/8 w - - 0 1"                            // Zugzwang Struggle
    )

    val out = new PrintWriter("massive_analysis_log.txt")
    out.println("================================================================================")
    out.println("LIVE ANALYSIS DEMONSTRATOR: REAL STOCKFISH WASM + BOOKSTYLE RENDERER")
    out.println("================================================================================")

    fens.foreach { fen =>
      out.println(s"\n[ANALYZING FEN]: $fen")
      
      val variations = runStockfish(fen)
      
      // Phase 14: Build EngineEvidence from parsed variations
      val evidence = buildEvidence(fen, variations)
      
      CommentaryEngine.assessExtended(
        fen = fen,
        variations = variations,
        ply = 15,
        prevMove = None
      ) match {
        case Some(data) =>
          val baseCtx = NarrativeContextBuilder.build(data, data.toContext, None, Nil, None, None, OpeningEventBudget())
          // Phase 14: Inject EngineEvidence into NarrativeContext
          val ctx = baseCtx.copy(engineEvidence = Some(evidence))
          val prose = BookStyleRenderer.render(ctx)
          
          out.println("--- GENERATED PROSE ---")
          out.println(prose)
          out.println("-----------------------")
        case None => 
          out.println("Error: Analysis failed.")
      }
      out.flush()
    }
    
    out.println("\n================================================================================")
    out.close()
    println("Live analysis complete. Results written to massive_analysis_log.txt")
  }

  private def runStockfish(fen: String): List[VariationLine] = {
    println(s"Invoking Stockfish WASM for FEN...")
    val output = s"node run_stockfish_wasm.js \"$fen\"".!!
    
    val startMarker = "---JSON_START---"
    val endMarker = "---JSON_END---"
    
    val jsonPart = output.split(startMarker)
      .lastOption
      .flatMap(_.split(endMarker).headOption)
      .getOrElse {
        println(s"DEBUG: Full output was:\n$output")
        throw new Exception("Could not find JSON markers in Stockfish output")
      }

    val json = Json.parse(jsonPart.trim)
    val lines = (json \ fen).as[List[JsObject]]
    
    lines.map { obj =>
      val rawMoves = (obj \ "moves").as[List[String]]
      
      // Phase 14: Parse UCI moves to PvMove with SAN/piece/capture metadata
      val parsedMoves = MoveAnalyzer.parsePv(fen, rawMoves)
      
      VariationLine(
        moves = rawMoves,
        scoreCp = (obj \ "scoreCp").as[Int],
        mate = (obj \ "mate").asOpt[Int],
        resultingFen = None, 
        depth = (obj \ "depth").as[Int],
        tags = Nil,
        parsedMoves = parsedMoves // Phase 14: Include parsed PV
      )
    }
  }
  
  /** Phase 14: Create EngineEvidence from parsed VariationLines */
  private def buildEvidence(fen: String, variations: List[VariationLine]): EngineEvidence = {
    val depth = variations.headOption.map(_.depth).getOrElse(0)
    EngineEvidence(depth = depth, variations = variations)
  }
}
