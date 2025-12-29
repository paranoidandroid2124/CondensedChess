package lila.llm

import lila.llm.analysis.*
import lila.llm.model.*

class CommentaryEngineTest extends munit.FunSuite:

  test("assess a simple position"):
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3" // Ruy Lopez starting position
    val pv = List("f1b5") // Bb5 move
    
    val assessmentOpt = CommentaryEngine.assess(
      fen = fen,
      pv = pv,
      opening = Some("Ruy Lopez"),
      phase = Some("opening")
    )

    assert(assessmentOpt.isDefined)
    val assessment = assessmentOpt.get
    assert(assessment.motifs.nonEmpty)
    assert(assessment.plans.topPlans.nonEmpty)
    assert(assessment.nature.description.nonEmpty)

  test("detect basic motifs"):
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val pv = List("e2e4", "e7e5", "g1f3", "b8c6", "f1b5")
    
    val assessmentOpt = CommentaryEngine.assess(fen, pv)
    
    assert(assessmentOpt.isDefined)
    val assessment = assessmentOpt.get
    val motifClassNames = assessment.motifs.map(_.getClass.getSimpleName)
    assert(motifClassNames.exists(_.contains("PawnAdvance")))
    // Centralization or PieceLift depends on exactly how it's detected
    assert(motifClassNames.nonEmpty)
