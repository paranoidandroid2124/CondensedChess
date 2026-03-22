package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*

class BranchProvenanceRegressionTest extends FunSuite:

  test("outline keeps same-head evidence branches distinct by SAN prefix") {
    val question =
      AuthorQuestion(
        id = "q_branch",
        kind = AuthorQuestionKind.TensionDecision,
        priority = 1,
        question = "How should Black meet the position — choose the clean branch?"
      )
    val ctx =
      BookmakerProseGoldenFixtures.exchangeSacrifice.ctx.copy(
        renderMode = NarrativeRenderMode.FullGame,
        authorQuestions = List(question),
        authorEvidence = List(
          QuestionEvidence(
            questionId = "q_branch",
            purpose = "decision_branch",
            branches = List(
              EvidenceBranch(
                keyMove = "12...Bf5",
                line = "12...Bf5 13.Nc3 Qa5 14.Bd2",
                evalCp = Some(40)
              ),
              EvidenceBranch(
                keyMove = "12...Bf5",
                line = "12...Bf5 13.Nc3 Rc8 14.Bd2",
                evalCp = Some(18)
              )
            )
          )
        )
      )

    val rec = new TraceRecorder()
    val (outline, _) = NarrativeOutlineBuilder.build(ctx, rec)
    val decision = outline.beats.find(_.kind == OutlineBeatKind.DecisionPoint).getOrElse(fail(s"missing decision beat: ${outline.beats.map(_.kind)}"))
    val evidence = outline.beats.find(_.kind == OutlineBeatKind.Evidence).getOrElse(fail(s"missing evidence beat: ${outline.beats.map(_.kind)}"))

    assert(decision.text.contains("Qa5"), clue(decision.text))
    assert(decision.text.contains("Rc8"), clue(decision.text))
    assert(evidence.text.contains("Qa5"), clue(evidence.text))
    assert(evidence.text.contains("Rc8"), clue(evidence.text))
  }

  test("line-scoped prophylaxis stays out of signal digest sidecars") {
    val base = BookmakerProseGoldenFixtures.exchangeSacrifice.ctx
    val semantic = base.semantic.getOrElse(fail("fixture needs semantic signals"))
    val ctx =
      base.copy(
        semantic = Some(
          semantic.copy(
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_break",
                deniedSquares = List("d5"),
                breakNeutralized = Some("d"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                sourceScope = FactScope.ThreatLine,
                citationLine = Some("12...Bf5 13.Nc3 Qa5")
              )
            )
          )
        )
      )

    val digest = NarrativeSignalDigestBuilder.build(ctx).getOrElse(fail("missing signal digest"))

    assertEquals(digest.prophylaxisPlan, None)
    assertEquals(digest.prophylaxisThreat, None)
    assertEquals(digest.counterplayScoreDrop, None)
  }
