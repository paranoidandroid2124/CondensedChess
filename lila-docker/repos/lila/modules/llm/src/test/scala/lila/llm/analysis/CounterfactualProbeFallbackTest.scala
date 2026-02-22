package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.strategic.*

class CounterfactualProbeFallbackTest extends FunSuite {

  private val prophylaxisAnalyzer = new lila.llm.analysis.strategic.ProphylaxisAnalyzerImpl()
  private val activityAnalyzer = new lila.llm.analysis.strategic.ActivityAnalyzerImpl()
  private val structureAnalyzer = new lila.llm.analysis.strategic.StructureAnalyzerImpl()
  private val endgameAnalyzer = new lila.llm.analysis.strategic.EndgameAnalyzerImpl()
  private val practicalityScorer = new lila.llm.analysis.strategic.PracticalityScorerImpl()

  private val extractor = new StrategicFeatureExtractorImpl(
    prophylaxisAnalyzer,
    activityAnalyzer,
    structureAnalyzer,
    endgameAnalyzer,
    practicalityScorer
  )

  test("counterfactual uses played-move probe line when played move is missing from MultiPV") {
    val fen = "r5k1/5ppp/8/8/8/8/5PPP/4R1K1 b - - 0 1"

    val vars = List(
      VariationLine(moves = List("h7h6", "h2h3"), scoreCp = 0, mate = None, depth = 14)
    )

    val playedProbe = ProbeResult(
      id = "played_probe",
      fen = Some(fen),
      evalCp = 900,
      bestReplyPv = List("e1e8"),
      deltaVsBaseline = 900,
      keyMotifs = Nil,
      purpose = Some("played_move_counterfactual"),
      probedMove = Some("a8a7"),
      depth = Some(18)
    )

    val result = extractor.extract(
      fen = fen,
      metadata = AnalysisMetadata(_root_.chess.Color.Black, 1, Some("a8a7")),
      baseData = BaseAnalysisData(
        nature = PositionNature(NatureType.Static, 0.0, 1.0, "Test"),
        motifs = Nil,
        plans = Nil
      ),
      vars = vars,
      playedMove = Some("a8a7"),
      probeResults = List(playedProbe)
    )

    assert(result.counterfactual.isDefined, "Counterfactual should be created from played probe")
    val cf = result.counterfactual.get
    assertEquals(cf.userLine.moves.take(2), List("a8a7", "e1e8"))
    assert(cf.cpLoss >= 200, s"cpLoss should reflect a serious miss, got ${cf.cpLoss}")
    assert(cf.causalThreat.exists(_.concept == "Checkmate"), s"Expected Checkmate threat, got ${cf.causalThreat}")
  }
}
