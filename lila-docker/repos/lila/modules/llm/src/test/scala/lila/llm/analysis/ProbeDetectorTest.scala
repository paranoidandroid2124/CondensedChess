package lila.llm.analysis

import chess.Color
import lila.llm.analysis.L3.PvLine
import lila.llm.model.{ Plan, PlanMatch, PlanScoringResult }
import lila.llm.model.authoring.*
import munit.FunSuite

class ProbeDetectorTest extends FunSuite:

  private val StartFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  private val latentPlanInfo = LatentPlanInfo(
    seedId = "kingside_rook_pawn_march",
    seedFamily = SeedFamily.Pawn,
    candidateMoves = List(MovePattern.PawnAdvance(chess.File.A)),
    typicalCounters = List(CounterPattern.CentralStrike),
    narrative = NarrativeTemplate(template = "If the opponent is slow, flank expansion can follow.")
  )

  private val latentQuestion = AuthorQuestion(
    id = "latent_q",
    kind = AuthorQuestionKind.LatentPlan,
    priority = 1,
    question = "Can a latent flank plan become viable here?",
    latentPlan = Some(latentPlanInfo)
  )

  private def emptyScoring: PlanScoringResult =
    PlanScoringResult(
      topPlans = Nil,
      confidence = 0.0,
      phase = "opening"
    )

  private def matchPlan(plan: Plan, score: Double): PlanMatch =
    PlanMatch(plan = plan, score = score, evidence = Nil)

  test("detect emits latent probes with purpose-aware contracts") {
    val ctx = IntegratedContext(evalCp = 30, isWhiteToMove = true)
    val multiPv = List(
      PvLine(List("e2e4", "e7e5"), evalCp = 30, mate = None, depth = 20),
      PvLine(List("d2d4", "d7d5"), evalCp = 15, mate = None, depth = 20)
    )

    val requests = ProbeDetector.detect(
      ctx = ctx,
      planScoring = emptyScoring,
      multiPv = multiPv,
      fen = StartFen,
      playedMove = Some("e2e4"),
      authorQuestions = List(latentQuestion)
    )

    val byPurpose = requests.groupBy(_.purpose.getOrElse(""))
    val freeTempo = byPurpose.getOrElse("free_tempo_branches", Nil)
    val immediate = byPurpose.getOrElse("latent_plan_immediate", Nil)
    val refute = byPurpose.getOrElse("latent_plan_refutation", Nil)

    assert(freeTempo.nonEmpty, clue(requests))
    assert(immediate.nonEmpty, clue(requests))
    assert(refute.nonEmpty, clue(requests))

    assert(freeTempo.head.requiredSignals.contains("futureSnapshot"))
    assert(immediate.head.requiredSignals.contains("l1Delta"))
    assert(refute.head.requiredSignals.contains("futureSnapshot"))
    assert(refute.head.requiredSignals.contains("keyMotifs"))
    assertEquals(refute.head.objective, Some("refute_plan"))
  }

  test("latent evidence probes remain present under mixed probe pressure") {
    val ctx = IntegratedContext(evalCp = 20, isWhiteToMove = true)
    val multiPv = List(
      PvLine(List("g1h3", "d7d5"), evalCp = 20, mate = None, depth = 20),
      PvLine(List("b1a3", "d7d5"), evalCp = 8, mate = None, depth = 20),
      PvLine(List("c2c3", "d7d5"), evalCp = 5, mate = None, depth = 20)
    )
    val crowdedScoring = PlanScoringResult(
      topPlans = List(
        matchPlan(Plan.CentralControl(Color.White), 0.91),
        matchPlan(Plan.PieceActivation(Color.White), 0.89),
        matchPlan(Plan.KingsideAttack(Color.White), 0.87),
        matchPlan(Plan.QueensideAttack(Color.White), 0.85),
        matchPlan(Plan.RookActivation(Color.White), 0.83)
      ),
      confidence = 0.9,
      phase = "opening"
    )

    val requests = ProbeDetector.detect(
      ctx = ctx,
      planScoring = crowdedScoring,
      multiPv = multiPv,
      fen = StartFen,
      playedMove = Some("e2e4"),
      authorQuestions = List(latentQuestion)
    )

    val purposes = requests.flatMap(_.purpose).toSet
    assert(purposes.contains("free_tempo_branches"), clue(requests))
    assert(purposes.contains("latent_plan_immediate"), clue(requests))
    assert(purposes.contains("latent_plan_refutation"), clue(requests))
    assert(purposes.contains("played_move_counterfactual"), clue(requests))
    assert(requests.size <= 8, clue(requests.map(_.id)))
  }

  test("competitive and defensive probes are shadow-only by default") {
    val quietCtx = IntegratedContext(evalCp = 10, isWhiteToMove = true)
    val competitivePv = List(
      PvLine(List("e2e4", "e7e5"), evalCp = 10, mate = None, depth = 20),
      PvLine(List("d2d4", "d7d5"), evalCp = -5, mate = None, depth = 20)
    )

    val competitiveRequests = ProbeDetector.detect(
      ctx = quietCtx,
      planScoring = emptyScoring,
      multiPv = competitivePv,
      fen = StartFen
    )
    assert(!competitiveRequests.exists(_.id.startsWith("competitive_")), clue(competitiveRequests))

    val tacticalFen = "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1"
    val defensivePv = List(
      PvLine(List("e4e5"), evalCp = 20, mate = None, depth = 20),
      PvLine(List("e4d5"), evalCp = -60, mate = None, depth = 20)
    )
    val defensiveRequests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 20, isWhiteToMove = true),
      planScoring = emptyScoring,
      multiPv = defensivePv,
      fen = tacticalFen
    )
    assert(!defensiveRequests.exists(_.id.startsWith("aggressive_why_not_")), clue(defensiveRequests))
  }

  test("low-confidence ghost probes are shadow-only while high-confidence ghosts remain emitted") {
    val lowConfidencePlan = matchPlan(
      Plan.KingsideAttack(Color.White),
      score = 0.79
    ).copy(
      evidence = List(
        lila.llm.model.EvidenceAtom(
          motif = lila.llm.model.Motif.PawnAdvance(
            file = chess.File.G,
            fromRank = 2,
            toRank = 4,
            color = Color.White,
            plyIndex = 0,
            move = Some("g4")
          ),
          weight = 1.0,
          description = "g-pawn push"
        )
      )
    )
    val lowScoring = PlanScoringResult(
      topPlans = List(lowConfidencePlan),
      confidence = 0.79,
      phase = "opening"
    )
    val lowRequests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 20, isWhiteToMove = true),
      planScoring = lowScoring,
      multiPv = List(PvLine(List("e2e4"), evalCp = 20, mate = None, depth = 20)),
      fen = StartFen
    )
    assert(!lowRequests.exists(_.objective.contains("validate_plan_presence")), clue(lowRequests))

    val highConfidencePlan = lowConfidencePlan.copy(score = 0.85)
    val highScoring = PlanScoringResult(
      topPlans = List(highConfidencePlan),
      confidence = 0.85,
      phase = "opening"
    )
    val highRequests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 20, isWhiteToMove = true),
      planScoring = highScoring,
      multiPv = List(PvLine(List("e2e4"), evalCp = 20, mate = None, depth = 20)),
      fen = StartFen
    )
    val ghost = highRequests.find(_.objective.contains("validate_plan_presence"))
    assert(ghost.nonEmpty, clue(highRequests))
    assert(ghost.exists(_.moves.contains("g2g4")), clue(highRequests))
  }
