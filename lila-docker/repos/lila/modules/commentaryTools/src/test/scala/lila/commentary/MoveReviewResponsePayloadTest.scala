package lila.commentary

import munit.FunSuite
import play.api.libs.json.{ JsObject }

import lila.commentary.model.StrategicPlanExperiment
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }

class MoveReviewResponsePayloadTest extends FunSuite:

  private def polishMeta: MoveReviewPolishMeta =
    MoveReviewPolishMeta(
      provider = "openai",
      model = Some("gpt-test"),
      sourceMode = "fallback_rule_invalid",
      validationPhase = "middlegame",
      validationReasons = List("contract_violation", "internal_prompt_marker"),
      cacheHit = false,
      promptTokens = Some(123),
      cachedTokens = Some(45),
      completionTokens = Some(67),
      estimatedCostUsd = Some(0.0123),
      strategyCoverage = Some(
        MoveReviewStrategyCoverageMeta(
          mode = "strict",
          enforced = true,
          threshold = 0.7,
          availableCategories = 4,
          coveredCategories = 3,
          requiredCategories = 2,
          coverageScore = 0.75,
          passesThreshold = true,
          planSignals = 2,
          planHits = 1,
          routeSignals = 1,
          routeHits = 1,
          focusSignals = 1,
          focusHits = 1
        )
      )
    )

  test("moveReview wire payload omits raw strategy carriers and internal polish diagnostics") {
    val response =
      CommentResponse(
        commentary = "This move keeps the position stable while Black finishes development.",
        concepts = List("internal concept"),
        variations = Nil,
        authorQuestions = List(
          AuthorQuestionSummary(
            id = "q1",
            kind = "probe",
            priority = 1,
            question = "Internal probe question",
            confidence = "medium"
          )
        ),
        authorEvidence = List(
          AuthorEvidenceSummary(
            questionId = "q1",
            questionKind = "probe",
            question = "Internal probe question",
            status = "pending"
          )
        ),
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "p1",
            planName = "Internal plan carrier",
            rank = 1,
            score = 0.7,
            preconditions = List("internal precondition"),
            executionSteps = List("internal execution"),
            failureModes = Nil,
            viability = PlanViability(score = 0.7, label = "medium", risk = "internal risk")
          )
        ),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "p1",
            themeL1 = "internal_theme",
            subplanId = None,
            evidenceTier = "probe_backed",
            supportProbeCount = 1,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = true,
            moveOrderSensitive = false,
            experimentConfidence = 0.8
          )
        ),
        sourceMode = "fallback_rule_invalid",
        model = Some("gpt-test"),
        polishMeta = Some(polishMeta),
        strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List("internal focus"))),
        signalDigest = Some(NarrativeSignalDigest(authoringEvidence = Some("internal evidence"))),
        moveReviewPlayerSurface = Some(
          MoveReviewPlayerSurface(
            summaryRows = List(
              MoveReviewPlayerSurfaceRow(
                label = "Plan",
                text = "Stable public support",
                authority =
                  Some(
                    MoveReviewSurfaceAuthority(
                      kind = MoveReviewSurfaceAuthority.CounterplayBreak,
                      token = Some("...c5")
                    )
                  )
              )
            ),
            decisionComparison =
              Some(
                MoveReviewPlayerDecisionComparison(
                  kicker = "Decision point",
                  gapLabel = Some("220cp"),
                  chosenSan = Some("h4"),
                  engineSan = Some("g4"),
                  secondaryText = Some("The checked line reaches an exchange sequence after Bxc6.")
                )
              )
          )
        )
      )

    val payload =
      MoveReviewResponsePayload.json(
        response = response,
        html = "<section>public html</section>",
        cacheHit = false
      )

    assertEquals(payload.keys.contains("strategyPack"), false, clue(payload))
    assertEquals(payload.keys.contains("signalDigest"), false, clue(payload))
    assertEquals(payload.keys.contains("authorQuestions"), false, clue(payload))
    assertEquals(payload.keys.contains("authorEvidence"), false, clue(payload))
    assertEquals(payload.keys.contains("mainStrategicPlans"), false, clue(payload))
    assertEquals(payload.keys.contains("strategicPlanExperiments"), false, clue(payload))
    assertEquals(payload.keys.contains("concepts"), false, clue(payload))
    assertEquals(payload.keys.contains("planTier"), false, clue(payload))
    assertEquals(payload.keys.contains("commentaryMode"), false, clue(payload))
    assertEquals(payload.keys.exists(_.toLowerCase.contains("opaque")), false, clue(payload))
    assertEquals(payload.keys.exists(_.toLowerCase.contains("strategictoken")), false, clue(payload))

    assertEquals((payload \ "schema").as[String], "chesstory.move_review.v2", clue(payload))
    assertEquals((payload \ "html").as[String], "<section>public html</section>", clue(payload))
    assertEquals(
      (payload \ "moveReviewPlayerSurface" \ "schema").as[String],
      "chesstory.move_review.player_surface.v2",
      clue(payload)
    )
    assertEquals(
      (payload \ "moveReviewPlayerSurface" \ "summaryRows" \ 0 \ "authority" \ "kind").as[String],
      "counterplay_break",
      clue(payload)
    )
    assertEquals(
      (payload \ "moveReviewPlayerSurface" \ "summaryRows" \ 0 \ "authority" \ "token").as[String],
      "...c5",
      clue(payload)
    )
    assertEquals(
      (payload \ "moveReviewPlayerSurface" \ "summaryRows" \ 0 \ "authority").as[JsObject].keys.contains("proofSource"),
      false,
      clue(payload)
    )
    assertEquals((payload \ "moveReviewPlayerSurface" \ "decisionComparison" \ "kicker").as[String], "Decision point", clue(payload))
    assertEquals((payload \ "probeRequests").as[List[JsObject]], Nil, clue(payload))
    assertEquals((payload \ "mainStrategicPlanCount").as[Int], 1, clue(payload))
    assertEquals((payload \ "diagnostics" \ "status").as[String], "fallback_available", clue(payload))
    assertEquals((payload \ "diagnostics" \ "sourceModeReason").as[String], "contract_violation", clue(payload))

    val publicMeta = (payload \ "polishMeta").as[JsObject]
    assertEquals(publicMeta.keys.contains("validationReasons"), false, clue(publicMeta))
    assertEquals(publicMeta.keys.contains("promptTokens"), false, clue(publicMeta))
    assertEquals(publicMeta.keys.contains("cachedTokens"), false, clue(publicMeta))
    assertEquals(publicMeta.keys.contains("completionTokens"), false, clue(publicMeta))
    assertEquals(publicMeta.keys.contains("estimatedCostUsd"), false, clue(publicMeta))
    assertEquals(publicMeta.keys.contains("strategyCoverage"), false, clue(publicMeta))
  }
