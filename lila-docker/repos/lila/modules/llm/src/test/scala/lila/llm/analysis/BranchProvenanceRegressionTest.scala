package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*

class BranchProvenanceRegressionTest extends FunSuite:

  private def tacticalTruthContract: DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("c3g3"),
      verifiedBestMove = Some("c3g3"),
      truthClass = DecisiveTruthClass.Blunder,
      cpLoss = 280,
      swingSeverity = 280,
      reasonFamily = DecisiveReasonFamily.TacticalRefutation,
      allowConcreteBenchmark = false,
      chosenMatchesBest = false,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.BlunderOwner,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = true,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.TacticalRefutation,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  test("outline keeps same-head evidence branches distinct by SAN prefix") {
    val question =
      AuthorQuestion(
        id = "q_branch",
        kind = AuthorQuestionKind.WhyThis,
        priority = 1,
        question = "Why does Black choose the cleaner branch here?",
        evidencePurposes = List("reply_multipv")
      )
    val ctx =
      BookmakerProseGoldenFixtures.exchangeSacrifice.ctx.copy(
        renderMode = NarrativeRenderMode.FullGame,
        authorQuestions = List(question),
        authorEvidence = List(
          QuestionEvidence(
            questionId = "q_branch",
            purpose = "reply_multipv",
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
    val (outline, _) = NarrativeOutlineBuilder.build(ctx, rec, Some(tacticalTruthContract))
    val decision = outline.beats.find(_.kind == OutlineBeatKind.DecisionPoint).getOrElse(fail(s"missing decision beat: ${outline.beats.map(_.kind)}"))
    val evidence = outline.beats.find(_.kind == OutlineBeatKind.Evidence).getOrElse(fail(s"missing evidence beat: ${outline.beats.map(_.kind)}"))

    assert(!decision.text.contains("Why does Black choose the cleaner branch here?"), clue(decision.text))
    assert(decision.text.toLowerCase.contains("tactical point"), clue(decision.text))
    assertEquals(decision.questionKinds, List(AuthorQuestionKind.WhyThis))
    assertEquals(decision.expectedEvidencePurposes, List("reply_multipv"))
    assert(evidence.text.contains("Qa5"), clue(evidence.text))
    assert(evidence.text.contains("Rc8"), clue(evidence.text))
    assertEquals(evidence.evidencePurposes, List("reply_multipv"))
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
