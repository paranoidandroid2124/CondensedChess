package chess.analysis

import munit.FunSuite
import chess.analysis.FeatureExtractor
import chess.analysis.ConceptLabeler
import chess.analysis.AnalysisTypes.ExperimentResult

class GoldenBaselineSuite extends FunSuite {

  case class BaselineCase(
    name: String,
    fenBefore: String,
    moveUci: String,
    fenAfter: String, // Helper for tests without engine move logic
    evalBefore: Int,
    evalAfter: Int,
    bestEval: Int, // What the engine saw as best (if different from played)
    experiments: List[ExperimentResult] = Nil
  )
  
  import chess.analysis.AnalysisModel.EngineEval
  import chess.analysis.AnalysisModel.EngineLine

  // 1. Greek Gift (Classic)
  val greekGift = BaselineCase(
    name = "Greek Gift",
    fenBefore = "r1bq1rk1/pppn1ppp/3bp3/3p4/3P4/2PB1N2/PP3PPP/R1BQK2R w KQ - 3 9",
    moveUci = "d3h7",
    fenAfter = "r1bq1rk1/pppn1pBp/3bp3/3p4/3P4/2P2N2/PP3PPP/R1BQK2R b KQ - 0 9",
    evalBefore = 50,
    evalAfter = 400,
    bestEval = 400,
    experiments = List(
      ExperimentResult(
        expType = AnalysisTypes.ExperimentType.TacticalCheck,
        fen = "r1bq1rk1/pppn1ppp/3bp3/3p4/3P4/2PB1N2/PP3PPP/R1BQK2R w KQ - 3 9",
        move = Some("d3h7"),
        eval = EngineEval(18, List(EngineLine("d3h7", 0.75, Some(400), None, List("Bxh7+", "Kxh7", "Ng5+", "Kg8", "Qh5")))),
        metadata = Map("candidateType" -> "SacrificeProbe")
      )
    )
  )

  // 2. Isolated Queen's Pawn (Structure)
  val iqpPosition = BaselineCase(
    name = "White IQP",
    fenBefore = "r1bq1rk1/pp3ppp/2n1pn2/8/2BP4/2N2N2/PP3PPP/R2Q1RK1 w - - 0 10",
    moveUci = "d4d5",
    fenAfter = "r1bq1rk1/pp3ppp/2n1pn2/3P4/2B5/2N2N2/PP3PPP/R2Q1RK1 b - - 0 10",
    evalBefore = 10,
    evalAfter = 10,
    bestEval = 10
  )

  // 3. Ignored Threat (Blunder)
  val threatCase = BaselineCase(
    name = "Ignored Threat",
    fenBefore = "r1bq1rk1/pp3p1p/2npp1p1/8/2B1P3/2N1QN2/PPP2PPP/R4RK1 w - - 0 12",
    moveUci = "c4d3", // Passive move
    fenAfter = "r1bq1rk1/pp3p1p/2npp1p1/8/4P3/2NBQN2/PPP2PPP/R4RK1 b - - 1 12",
    evalBefore = 0,
    evalAfter = -200,
    bestEval = 50,
    experiments = List(
      ExperimentResult(
        expType = AnalysisTypes.ExperimentType.GeneralEval,
        fen = "r1bq1rk1/pp3p1p/2npp1p1/8/2B1P3/2N1QN2/PPP2PPP/R4RK1 w - - 0 12",
        move = None, // Null move result
        eval = EngineEval(18, List(EngineLine("Qh6", 0.20, Some(-300), None, List("Qh6", "...", "Ng5", "...", "Qxh7#")))),
        metadata = Map("candidateType" -> "Threat")
      )
    )
  )

  test("Capture Baseline Tags") {
     val cases = List(greekGift, iqpPosition, threatCase)
     
     cases.foreach { c =>
       val f1 = FeatureExtractor.extractPositionFeatures(c.fenBefore, 10)
       // We might need a real fenAfter for some checks, but for FeatureExtractor mainly f1/f2 matters
       val f2 = FeatureExtractor.extractPositionFeatures(c.fenAfter, 11)
       
       val labels = ConceptLabeler.labelAll(
         featuresBefore = f1,
         featuresAfter = f2,
         movePlayedUci = c.moveUci,
         experiments = c.experiments,
         baselineEval = c.evalBefore,
         evalAfterPlayed = c.evalAfter,
         bestEval = c.bestEval
       )
       
       println(s"=== Case: ${c.name} ===")
       println(s"Structure: ${labels.structureTags}")
       println(s"Tactics: ${labels.tacticTags}")
       println(s"Plans: ${labels.planTags}")
       println(s"Positional: ${labels.positionalTags}")
       println(s"Rich Tags: ${labels.richTags}")
       println(s"Evidence: ${labels.evidence}")
       println("========================\n")
     }
  }
}
