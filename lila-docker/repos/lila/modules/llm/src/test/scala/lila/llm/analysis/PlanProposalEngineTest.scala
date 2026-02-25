package lila.llm.analysis

import munit.FunSuite

class PlanProposalEngineTest extends FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  test("plan-first proposal emits hypotheses without MultiPV input") {
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(initialFen),
      features = PositionAnalyzer.extractFeatures(initialFen, 1)
    )

    val hypotheses = PlanProposalEngine.propose(
      fen = initialFen,
      ply = 1,
      ctx = ctx,
      maxItems = 3
    )

    assert(hypotheses.nonEmpty)
    assert(
      hypotheses.exists(_.evidenceSources.exists(_.contains("proposal:plan_first")))
    )
  }

  test("assessExtended keeps plan hypotheses even when variations are empty") {
    val dataOpt = CommentaryEngine.assessExtended(
      fen = initialFen,
      variations = Nil,
      playedMove = None,
      opening = None,
      phase = Some("opening"),
      ply = 1,
      prevMove = None,
      probeResults = Nil
    )

    assert(dataOpt.nonEmpty)
    assert(dataOpt.get.planHypotheses.nonEmpty)
  }
