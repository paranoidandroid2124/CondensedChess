package lila.llm.analysis

import chess.Color
import lila.llm.analysis.L3.PvLine
import lila.llm.model.{ Plan, PlanMatch, PlanScoringResult }
import lila.llm.model.authoring.{ AuthorQuestion, AuthorQuestionKind, PlanHypothesis, PlanViability }
import munit.FunSuite

class ProbeDetectorTest extends FunSuite:

  private val StartFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  private val whyThisQuestion = AuthorQuestion(
    id = "why_this_q",
    kind = AuthorQuestionKind.WhyThis,
    priority = 1,
    question = "Why choose the central advance here?",
    evidencePurposes = List("reply_multipv")
  )

  private def emptyScoring: PlanScoringResult =
    PlanScoringResult(
      topPlans = Nil,
      confidence = 0.0,
      phase = "opening"
    )

  private def matchPlan(plan: Plan, score: Double): PlanMatch =
    PlanMatch(plan = plan, score = score, evidence = Nil)

  private def hypothesis(
      id: String,
      name: String,
      score: Double,
      sources: List[String],
      subplanId: Option[String] = None
  ): PlanHypothesis =
    PlanHypothesis(
      planId = id,
      planName = name,
      rank = 0,
      score = score,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(score = score, label = "medium", risk = "test"),
      refutation = None,
      evidenceSources = sources,
      subplanId = subplanId
    )

  test("detect emits question-first probes with purpose-aware contracts") {
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
      authorQuestions = List(whyThisQuestion)
    )

    val byPurpose = requests.groupBy(_.purpose.getOrElse(""))
    val reply = byPurpose.getOrElse("reply_multipv", Nil)

    assert(reply.nonEmpty, clue(requests))
    assert(reply.head.requiredSignals.contains("replyPvs"))
    assertEquals(reply.head.questionKind, Some("WhyThis"))
    assertEquals(reply.head.objective, Some("compare_reply_branches"))
  }

  test("question-first evidence probes remain present under mixed probe pressure") {
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
      authorQuestions = List(whyThisQuestion)
    )

    val purposes = requests.flatMap(_.purpose).toSet
    assert(purposes.contains("reply_multipv"), clue(requests))
    assert(requests.size >= 2, clue(requests))
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
    val lowGhostRequests = lowRequests.filter(_.planId.nonEmpty)
    assert(
      lowGhostRequests.forall(req => req.purpose.exists(ThemePlanProbePurpose.isThemeValidationPurpose)),
      clue(lowRequests)
    )

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
    assert(
      highRequests.exists(req =>
        req.planId.nonEmpty &&
          req.purpose.exists(ThemePlanProbePurpose.isThemeValidationPurpose)
      ),
      clue(highRequests)
    )
  }

  test("restriction subplans use family-specific probe purposes instead of generic theme validation") {
    val keySquareDenial =
      hypothesis(
        id = "KeySquareClamp",
        name = "Key-square denial around e5",
        score = 0.81,
        sources = List("theme:restriction_prophylaxis"),
        subplanId = Some(ThemeTaxonomy.SubplanId.KeySquareDenial.id)
      )

    val requests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
      planScoring = emptyScoring,
      planHypotheses = List(keySquareDenial),
      multiPv = List(PvLine(List("e2e4"), evalCp = 18, mate = None, depth = 20)),
      fen = StartFen,
      playedMove = Some("e2e4")
    )

    val routeDenial = requests.find(_.planId.contains("KeySquareClamp")).getOrElse(fail(requests.toString))
    assertEquals(routeDenial.purpose, Some("route_denial_validation"))
    assertEquals(routeDenial.objective, Some("validate_route_denial"))
    assert(routeDenial.requiredSignals.contains("futureSnapshot"), clue(routeDenial))
    assert(routeDenial.requiredSignals.contains("l1Delta"), clue(routeDenial))
  }

  test("detect handles planner-era subplans without MatchError and keeps validation probes live") {
    val fen = "4k3/8/8/2pp4/3BP3/5Q2/4R3/4K3 w - - 0 1"
    val multiPv = List(
      PvLine(List("e1d2"), evalCp = 18, mate = None, depth = 20)
    )
    val problematicSubplans =
      List(
        ThemeTaxonomy.SubplanId.BishopReanchor,
        ThemeTaxonomy.SubplanId.OpenFilePressure,
        ThemeTaxonomy.SubplanId.IQPInducement,
        ThemeTaxonomy.SubplanId.BadPieceLiquidation,
        ThemeTaxonomy.SubplanId.PassedPawnManufacture,
        ThemeTaxonomy.SubplanId.BatteryPressure
      )
    problematicSubplans.foreach { subplan =>
      val requests =
        ProbeDetector.detect(
          ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
          planScoring = emptyScoring,
          planHypotheses =
            List(
              hypothesis(
                id = s"plan_${subplan.id}",
                name = subplan.id,
                score = 0.84,
                sources = List(s"subplan:${subplan.id}"),
                subplanId = Some(subplan.id)
              )
            ),
          multiPv = multiPv,
          fen = fen
        )
      assert(
        requests.exists(_.planId.contains(s"plan_${subplan.id}")),
        clue(subplan, requests)
      )
    }
  }
