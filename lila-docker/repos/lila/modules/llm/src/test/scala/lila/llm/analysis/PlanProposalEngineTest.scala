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

  test("pawnstorm kingside seed does not surface in flexible non-locked center positions") {
    val fen = "r1bqr1k1/ppp1bppp/3p1n2/4n3/3NPB2/2N5/PPP2PPP/R2QRBK1 b - - 4 10"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = false,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 20)
    )

    val hypotheses = PlanProposalEngine.propose(
      fen = fen,
      ply = 20,
      ctx = ctx,
      maxItems = 5
    )

    val pawnStorm = hypotheses.find(h =>
      h.planName.equalsIgnoreCase("PawnStorm Kingside") ||
        h.evidenceSources.contains("latent_seed:PawnStorm_Kingside")
    )

    assert(pawnStorm.forall(_.score <= 0.38))
    assert(hypotheses.take(2).forall(h => pawnStorm.forall(_.planName != h.planName)))
  }
