package lila.llm.analysis

import lila.llm.model.*
import lila.llm.model.strategic.*

class NullMoveProbeTest extends munit.FunSuite {

  private val prophylaxisAnalyzer = new lila.llm.analysis.strategic.ProphylaxisAnalyzerImpl()
  private val activityAnalyzer = new lila.llm.analysis.strategic.ActivityAnalyzerImpl()
  private val structureAnalyzer = new lila.llm.analysis.strategic.StructureAnalyzerImpl()
  private val endgameAnalyzer = new lila.llm.analysis.strategic.EndgameAnalyzerImpl()
  private val practicalityScorer = new lila.llm.analysis.strategic.PracticalityScorerImpl()

  val extractor = new StrategicFeatureExtractorImpl(
    prophylaxisAnalyzer,
    activityAnalyzer,
    structureAnalyzer,
    endgameAnalyzer,
    practicalityScorer
  )

  test("Extractor should prioritize NullMoveThreat probe over base heuristics") {
    // A simple FEN where White has just moved
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
    
    // Create a mock probe result representing what the engine saw after a null move
    // The engine (playing for white after black passes) decides to play Qh5
    val nullProbeResult = ProbeResult(
      id = "null_move_test",
      evalCp = 350,   // Engine evaluates passed position as +3.50 for the opponent (White)
      deltaVsBaseline = 0,
      keyMotifs = Nil,
      depth = Some(10),
      mate = None,
      bestReplyPv = List("d1h5", "g7g6"),
      purpose = Some("NullMoveThreat")
    )

    val meta = AnalysisMetadata(_root_.chess.Color.Black, 1, Some("e2e4"))
    val baseData = BaseAnalysisData(
      nature = PositionNature(NatureType.Static, 0.0, 1.0, "Test position"),
      motifs = Nil,
      plans = Nil,
      planSequence = None
    )
    val vars = List(VariationLine(moves = List("e7e5"), scoreCp = 0, mate = None, depth = 10))

    val result = extractor.extract(
      fen = fen,
      metadata = meta,
      baseData = baseData,
      vars = vars,
      playedMove = Some("e7e5"),
      probeResults = List(nullProbeResult)
    )

    // Verify Prophylaxis Results
    // Because the probe found a threat (Qh5), the extractor should have run prophylaxis against it
    // The first candidate move is "e7e5". Does it prevent "Qh5"? Probably not entirely, but that's what the analyzer checks.
    assertEquals(result.candidates.size, 1)
    val candidate = result.candidates.head
    
    // We mainly care if it even attempted Prophylaxis against the Null-Move plan
    assert(candidate.prophylaxisResults.nonEmpty, "Prophylaxis should be evaluated against the null move threat")
    
    // Verify the threat score is parsed from White-POV probe eval and converted to mover-relative drop.
    assertEquals(candidate.prophylaxisResults.head.counterplayScoreDrop, 350)
    assert(candidate.prophylaxisResults.head.planId.nonEmpty, "Null-move probe should provide a concrete threat plan id")
  }
}
