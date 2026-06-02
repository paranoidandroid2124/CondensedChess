package lila.commentary

import munit.FunSuite
import play.api.libs.json.{ JsObject, Json }

import lila.commentary.analysis.PlanStateTracker
import lila.commentary.analysis.PlayerFacingClaimProvenanceClass
import lila.commentary.analysis.MoveReviewExchangeAnalyzer
import lila.commentary.analysis.UserFacingSignalSanitizer
import lila.commentary.analysis.PlanEvidenceEvaluator.{ ClaimCertification, EvaluatedPlan, PlanEvidenceStatus, UserFacingPlanEligibility }
import lila.commentary.analysis.semantic.RelationObservationCatalog
import lila.commentary.model.{ FactFragment, StrategicPlanExperiment }
import lila.commentary.model.authoring.*
import lila.strategicPuzzle.StrategicPuzzle.*

class UserFacingPayloadSanitizerTest extends FunSuite:

  private val bannedPhrases = List(
    "PlayableByPV",
    "PlayedPV",
    "strict evidence mode",
    "probe evidence pending",
    "engine-coupled continuation",
    "theme:",
    "subplan:",
    "support:",
    "seed:",
    "{seed}",
    "{us}",
    "{them}",
    "candidate move",
    "candidate capture",
    "probe validation",
    "raw request",
    "return vector",
    "cash out"
  )

  private def assertNoLeaks(text: String): Unit =
    bannedPhrases.foreach { phrase =>
      assert(!text.toLowerCase.contains(phrase.toLowerCase), clue(text))
    }

  test("user-facing signal sanitizer routes legacy relation helper notation through fallback wording") {
    val text =
      UserFacingSignalSanitizer.sanitize(
        "Domination(Knight,e5) TrappedPiece(Queen,h4) Zwischenzug(Nf7) StalemateTrap(g8) PerpetualCheck(h5)."
      )
    val lower = text.toLowerCase

    assert(lower.contains("key-square restriction"), clue(text))
    assert(lower.contains("piece mobility"), clue(text))
    assert(lower.contains("move-order caution"), clue(text))
    assert(!lower.contains("domination"), clue(text))
    assert(!lower.contains("trapped piece"), clue(text))
    assert(!lower.contains("zwischenzug"), clue(text))
    assert(!lower.contains("stalemate"), clue(text))
    assert(!lower.contains("perpetual"), clue(text))
  }

  private def typedEvaluation(
      plan: PlanHypothesis,
      eligibility: UserFacingPlanEligibility,
      supportProbeIds: List[String] = Nil
  ): EvaluatedPlan =
    EvaluatedPlan(
      hypothesis = plan,
      status =
        eligibility match
          case UserFacingPlanEligibility.ProbeBacked   => PlanEvidenceStatus.PlayableEvidenceBacked
          case UserFacingPlanEligibility.TranspositionAligned => PlanEvidenceStatus.PlayableTranspositionAligned
          case UserFacingPlanEligibility.StructuralOnly => PlanEvidenceStatus.PlayableStructuralOnly
          case UserFacingPlanEligibility.PvCoupledOnly => PlanEvidenceStatus.PlayablePvCoupled
          case UserFacingPlanEligibility.Deferred      => PlanEvidenceStatus.Deferred
          case UserFacingPlanEligibility.Refuted       => PlanEvidenceStatus.Refuted,
      userFacingEligibility = eligibility,
      reason = "test typed evaluator admission",
      supportProbeIds = supportProbeIds,
      themeL1 = plan.themeL1,
      subplanId = plan.subplanId
    )

  private def probeBackedEvaluation(plan: PlanHypothesis): EvaluatedPlan =
    typedEvaluation(
      plan = plan,
      eligibility = UserFacingPlanEligibility.ProbeBacked,
      supportProbeIds = List("probe_1")
    )

  private def deferredEvaluation(plan: PlanHypothesis): EvaluatedPlan =
    typedEvaluation(plan, UserFacingPlanEligibility.Deferred)

  private def transpositionEvaluation(plan: PlanHypothesis): EvaluatedPlan =
    typedEvaluation(plan, UserFacingPlanEligibility.TranspositionAligned)
      .copy(
        transpositionProofIds = List("transposition:test:d5"),
        claimCertification =
          ClaimCertification(
            provenanceClass = PlayerFacingClaimProvenanceClass.TranspositionAligned
          )
      )

  test("sanitizes moveReview response across structured user-facing fields") {
    val response =
      CommentResponse(
        commentary =
          "Piece activation is deferred as PlayableByPV under strict evidence mode, and the return vector only holds if the line pressure cash out works.",
        concepts = List("PlayableByPV", "return vector"),
        authorQuestions = List(
          AuthorQuestionSummary(
            id = "q1",
            kind = "probe",
            priority = 1,
            question = "Does {seed} still work?",
            why = Some("supported by engine-coupled continuation"),
            anchors = List("theme:piece_redeployment"),
            confidence = "medium",
            latentPlanName = Some("PlayedPV follow-up")
          )
        ),
        authorEvidence = List(
          AuthorEvidenceSummary(
            questionId = "q1",
            questionKind = "probe",
            question = "Does the plan cash out?",
            why = Some("probe evidence pending"),
            status = "pending",
            purposes = List("theme:piece_redeployment"),
            probeObjectives = List("return vector"),
            linkedPlans = List("seed:pawnstorm_kingside")
          )
        ),
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "PieceActivation",
            planName = "PlayableByPV plan",
            rank = 1,
            score = 0.7,
            preconditions = List("theme:piece_redeployment", "{seed}"),
            executionSteps = List("cash out through c-file pressure"),
            failureModes = List("probe evidence pending"),
            viability = PlanViability(score = 0.7, label = "medium", risk = "return vector collapse"),
            refutation = Some("engine-coupled continuation fails"),
            subplanId = Some("rook_lift_scaffold"),
            evidenceSources = List("probe_backed:validated_support")
          )
        ),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "PieceActivation",
            themeL1 = "piece_redeployment",
            subplanId = Some("rook_lift_scaffold"),
            evidenceTier = "pv_coupled",
            supportProbeCount = 0,
            refuteProbeCount = 0,
            bestReplyStable = false,
            futureSnapshotAligned = false,
            counterBreakNeutralized = false,
            moveOrderSensitive = true,
            experimentConfidence = 0.52
          )
        ),
        strategyPack = Some(
          StrategyPack(
            sideToMove = "white",
            plans = List(
              StrategySidePlan(
                side = "white",
                horizon = "long",
                planName = "PlayableByPV plan",
                priorities = List("cash out"),
                riskTriggers = List("probe evidence pending")
              )
            ),
            longTermFocus = List("cash out only after return vector proves itself"),
            evidence = List("theme:piece_redeployment", "support:engine_hypothesis"),
            signalDigest = Some(
              NarrativeSignalDigest(
                strategicStack = List("PlayableByPV continuation"),
                latentPlan = Some("seed:pawnstorm_kingside"),
                latentReason = Some("probe evidence pending"),
                decisionComparison = Some(
                  DecisionComparisonDigest(
                    deferredMove = Some("Qe3"),
                    deferredReason = Some("accepted as PlayableByPV fallback"),
                    deferredSource = Some("engine-coupled continuation"),
                    evidence = Some("return vector through line pressure")
                  )
                ),
                authoringEvidence = Some("theme:piece_redeployment"),
                practicalVerdict = Some("cash out"),
                practicalFactors = List("return vector"),
                compensation = Some("return vector through line pressure and delayed recovery"),
                compensationVectors = List("Return Vector (0.5)", "Line Pressure (0.7)", "Delayed Recovery (0.4)")
              )
            )
          )
        ),
        moveReviewLedger = Some(
          MoveReviewStrategicLedger(
            motifKey = "attack",
            motifLabel = "PlayableByPV attack",
            stageKey = "build",
            stageLabel = "cash out",
            carryOver = false,
            stageReason = Some("probe evidence pending"),
            prerequisites = List("theme:piece_redeployment"),
            conversionTrigger = Some("return vector"),
            primaryLine = Some(
              MoveReviewLedgerLine(
                title = "PlayedPV line",
                note = Some("cash out through initiative"),
                source = "rule"
              )
            )
          )
        ),
        moveReviewExplanation = Some(
          MoveReviewExplanation(
            title = "PlayableByPV title",
            prose = "strict evidence mode prose with return vector",
            qualityLabel = Some("probe evidence pending"),
            reasonTags = List("theme:piece_redeployment", "cash out"),
            shortLine = Some(
              MoveReviewShortLine(
                san = List("PlayableByPV"),
                uci = List("seed:uci"),
                lineId = Some("support:line"),
                scoreCp = Some(12),
                mate = None,
                depth = Some(16),
                source = "engine-coupled continuation"
              )
            ),
            pvInterpretation = Some(
              MoveReviewPvInterpretation(
                linePurpose = "PlayableByPV",
                confirms = List("return vector"),
                tension = "strict evidence mode",
                opponentReplyMeaning = Some("probe evidence pending"),
                learningPoint = "cash out through {seed}",
                supportedByLineId = Some("support:line"),
                confidence = "engine-coupled continuation"
              )
            ),
            source = "support:basic",
            factFragments =
              Some(
                List(
                  FactFragment.StrategicSupportFragment(
                    san = "PlayableByPV",
                    proofFamily = "raw_proof_family",
                    proofSource = "raw_proof_source",
                    purpose = "support-only fact fragment"
                  )
                )
              )
          )
        ),
        moveReviewPlayerSurface = Some(
          MoveReviewPlayerSurface(
            title = Some("PlayableByPV support title"),
            summaryRows = List(
              MoveReviewPlayerSurfaceRow(
                label = "support:Main",
                text = "strict evidence mode with return vector",
                source = Some("support:engine_hypothesis"),
                refSans = List("Qe3")
              )
            ),
            decisionComparison = Some(
              MoveReviewPlayerDecisionComparison(
                kicker = "Decision compare",
                deferredSan = Some("Qe3"),
                secondaryText = Some("accepted as PlayableByPV fallback")
              )
            ),
            authorRows = List(
              MoveReviewPlayerAuthorRow(
                title = "theme:Authoring",
                status = "probe evidence pending",
                question = "Does {seed} still work?",
                why = Some("engine-coupled continuation"),
                meta = List("seed:pawnstorm_kingside"),
                branches = List(
                  MoveReviewPlayerSurfaceRow(
                    label = "line_1",
                    text = "return vector through line pressure",
                    source = Some("authoring")
                  )
                )
              )
            )
          )
        )
      )

    val sanitized =
      UserFacingPayloadSanitizer.sanitize(
        response,
        admittedPlans = response.mainStrategicPlans.map(probeBackedEvaluation)
      )
    val rendered =
      List(
        sanitized.commentary,
        sanitized.concepts.mkString(" "),
        sanitized.authorQuestions
          .flatMap(q => q.why.toList ++ List(q.question) ++ q.anchors ++ q.latentPlanName.toList)
          .mkString(" "),
        sanitized.authorEvidence
          .flatMap(e => e.why.toList ++ List(e.question) ++ e.purposes ++ e.probeObjectives ++ e.linkedPlans)
          .mkString(" "),
        sanitized.mainStrategicPlans
          .flatMap(p =>
            p.preconditions ++ p.executionSteps ++ p.failureModes ++ List(p.planName, p.viability.risk) ++ p.refutation.toList
          )
          .mkString(" "),
        sanitized.strategicPlanExperiments.map(_.themeL1).mkString(" "),
        sanitized.strategyPack.toList.flatMap(pack =>
          pack.longTermFocus ++
            pack.evidence ++
            pack.signalDigest.toList.flatMap(d =>
              d.strategicStack ++
                d.practicalFactors ++
                d.compensation.toList ++
                d.compensationVectors ++
                d.authoringEvidence.toList ++
                d.practicalVerdict.toList ++
                d.latentPlan.toList ++
                d.latentReason.toList ++
                d.decisionComparison.toList.flatMap(dc => dc.deferredReason.toList ++ dc.deferredSource.toList ++ dc.evidence.toList)
            )
        ).mkString(" "),
        sanitized.moveReviewLedger.toList.flatMap(ledger =>
          ledger.stageReason.toList ++
            ledger.prerequisites ++
            ledger.conversionTrigger.toList ++
            ledger.primaryLine.toList.flatMap(line => line.note.toList ++ List(line.title)) ++
            List(ledger.motifLabel, ledger.stageLabel)
        ).mkString(" "),
        sanitized.moveReviewExplanation.toList.flatMap(explanation =>
          List(explanation.title, explanation.prose, explanation.source) ++
            explanation.qualityLabel.toList ++
            explanation.reasonTags ++
            explanation.shortLine.toList.flatMap(line => line.san ++ line.uci ++ line.lineId.toList ++ List(line.source)) ++
            explanation.pvInterpretation.toList.flatMap(interpretation =>
              List(interpretation.linePurpose, interpretation.tension, interpretation.learningPoint, interpretation.confidence) ++
                interpretation.confirms ++
                interpretation.opponentReplyMeaning.toList ++
                interpretation.supportedByLineId.toList
            )
        ).mkString(" "),
        sanitized.moveReviewPlayerSurface.toList.flatMap(surface =>
          surface.title.toList ++
            surface.summaryRows.flatMap(row => List(row.label, row.text) ++ row.source.toList ++ row.refSans) ++
            surface.advancedRows.flatMap(row => List(row.label, row.text) ++ row.source.toList ++ row.refSans) ++
            surface.probeRows.flatMap(row => List(row.label, row.text) ++ row.source.toList ++ row.refSans) ++
            surface.decisionComparison.toList.flatMap(comparison =>
              List(comparison.kicker) ++
                comparison.gapLabel.toList ++
                comparison.chosenSan.toList ++
                comparison.engineSan.toList ++
                comparison.comparedSan.toList ++
                comparison.deferredSan.toList ++
                comparison.secondaryText.toList
            ) ++
            surface.authorRows.flatMap(row =>
              List(row.title, row.status, row.question) ++
                row.why.toList ++
                row.meta ++
                row.branches.flatMap(branch => List(branch.label, branch.text) ++ branch.source.toList ++ branch.refSans)
            )
        ).mkString(" ")
      ).mkString(" ")

    val json = Json.toJson(sanitized).as[JsObject]

    assertNoLeaks(rendered)
    assertEquals(sanitized.moveReviewExplanation.flatMap(_.factFragments), None, clue(sanitized.moveReviewExplanation))
    assert(!Json.stringify(json).contains("raw_proof_family"), clue(json))
    assertEquals(json.keys.contains("latentPlans"), false, clue(json))
    assertEquals(json.keys.contains("whyAbsentFromTopMultiPV"), false, clue(json))
    assertEquals(sanitized.signalDigest.flatMap(_.latentPlan), None, clue(sanitized.signalDigest))
    assertEquals(sanitized.signalDigest.flatMap(_.latentReason), None, clue(sanitized.signalDigest))
    assertEquals(sanitized.signalDigest.flatMap(_.opponentPlan), None, clue(sanitized.signalDigest))
    assertEquals(sanitized.signalDigest.flatMap(_.dominantIdeaKind), None, clue(sanitized.signalDigest))
    assertEquals(sanitized.signalDigest.flatMap(_.decision), None, clue(sanitized.signalDigest))
    assertEquals(
      sanitized.moveReviewPlayerSurface.flatMap(_.decisionComparison).flatMap(_.deferredSan),
      None,
      clue(sanitized.moveReviewPlayerSurface)
    )
    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.authorRows.flatMap(_.meta)),
      Nil,
      clue(sanitized.moveReviewPlayerSurface)
    )
    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(surface =>
        surface.summaryRows.flatMap(_.source) ++
          surface.advancedRows.flatMap(_.source) ++
          surface.probeRows.flatMap(_.source) ++
          surface.authorRows.flatMap(_.branches.flatMap(_.source))
      ),
      Nil,
      clue(sanitized.moveReviewPlayerSurface)
    )
    assertEquals(sanitized.mainStrategicPlans.size, 1, clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.strategicPlanExperiments.size, 1, clue(sanitized.strategicPlanExperiments))
    assert(rendered.toLowerCase.contains("pays off") || rendered.toLowerCase.contains("engine-backed"), clue(rendered))
  }

  test("does not admit strategic plans from probe-backed marker alone") {
    val markerOnlyPlan =
      PlanHypothesis(
        planId = "marker_only",
        planName = "Marker-only plan",
        rank = 1,
        score = 0.7,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.7, "medium", "marker-only risk"),
        evidenceSources = List("probe_backed:validated_support")
      )
    val response =
      CommentResponse(
        commentary = "Marker-only payload",
        concepts = Nil,
        mainStrategicPlans = List(markerOnlyPlan),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "marker_only",
            themeL1 = "piece_redeployment",
            subplanId = None,
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = true,
            moveOrderSensitive = false,
            experimentConfidence = 0.8
          )
        )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(sanitized.mainStrategicPlans, Nil, clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.strategicPlanExperiments, Nil, clue(sanitized.strategicPlanExperiments))
  }

  test("admits strategic plans only from typed evaluator decisions") {
    val admittedPlan =
      PlanHypothesis(
        planId = "admitted",
        planName = "Admitted plan",
        rank = 1,
        score = 0.7,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.7, "medium", "risk"),
        evidenceSources = List("probe_backed:validated_support")
      )
    val deferredPlan =
      PlanHypothesis(
        planId = "deferred",
        planName = "Deferred plan",
        rank = 2,
        score = 0.65,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.65, "medium", "risk"),
        evidenceSources = List("probe_backed:validated_support")
      )
    val response =
      CommentResponse(
        commentary = "Typed admission payload",
        concepts = Nil,
        mainStrategicPlans = List(admittedPlan, deferredPlan),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "admitted",
            themeL1 = "piece_redeployment",
            subplanId = None,
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = true,
            moveOrderSensitive = false,
            experimentConfidence = 0.8
          ),
          StrategicPlanExperiment(
            planId = "deferred",
            themeL1 = "piece_redeployment",
            subplanId = None,
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = true,
            moveOrderSensitive = false,
            experimentConfidence = 0.8
          )
        )
      )

    val sanitized =
      UserFacingPayloadSanitizer.sanitize(
        response,
        admittedPlans = List(probeBackedEvaluation(admittedPlan), deferredEvaluation(deferredPlan))
      )

    assertEquals(sanitized.mainStrategicPlans.map(_.planId), List("admitted"), clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.mainStrategicPlans.flatMap(_.evidenceSources), Nil, clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.strategicPlanExperiments.map(_.planId), List("admitted"), clue(sanitized.strategicPlanExperiments))
  }

  test("admits transposition-aligned strategic plans without probe ids") {
    val plan =
      PlanHypothesis(
        planId = "transposition",
        planName = "Transposition target plan",
        rank = 1,
        score = 0.76,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.76, "medium", "risk"),
        evidenceSources = List("weakness_target:d5")
      )
    val response =
      CommentResponse(
        commentary = "Transposition admission payload",
        concepts = Nil,
        mainStrategicPlans = List(plan),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "transposition",
            themeL1 = "weakness_fixation",
            subplanId = None,
            evidenceTier = "transposition_aligned",
            supportProbeCount = 0,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = false,
            moveOrderSensitive = false,
            experimentConfidence = 0.8
          )
        )
      )

    val sanitized =
      UserFacingPayloadSanitizer.sanitize(
        response,
        admittedPlans = List(transpositionEvaluation(plan))
      )

    assertEquals(sanitized.mainStrategicPlans.map(_.planId), List("transposition"))
    assertEquals(sanitized.mainStrategicPlans.flatMap(_.evidenceSources), Nil)
    assertEquals(sanitized.strategicPlanExperiments.map(_.planId), List("transposition"))
  }

  test("drops malformed top-level ledger lines without dropping valid ledger metadata") {
    val plan =
      PlanHypothesis(
        planId = "ledger_plan",
        planName = "Ledger plan",
        rank = 1,
        score = 0.76,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.76, "medium", "risk")
      )
    val response =
      CommentResponse(
        commentary = "Ledger payload",
        concepts = Nil,
        mainStrategicPlans = List(plan),
        moveReviewLedger =
          Some(
            MoveReviewStrategicLedger(
              motifKey = "piece_route",
              motifLabel = "Piece route",
              stageKey = "build",
              stageLabel = "Build",
              carryOver = false,
              primaryLine =
                Some(
                  MoveReviewLedgerLine(
                    title = "Raw request line",
                    sanMoves = List("Nf3"),
                    note = Some("raw request purpose"),
                    source = "probe_request"
                  )
                ),
              resourceLine =
                Some(
                  MoveReviewLedgerLine(
                    title = "Probe line",
                    sanMoves = List("Nf3", "Nc6"),
                    note = Some("12cp vs baseline"),
                    source = "probe"
                  )
                )
            )
          )
      )

    val sanitized =
      UserFacingPayloadSanitizer.sanitize(
        response,
        admittedPlans = List(probeBackedEvaluation(plan))
      )
    val ledger = sanitized.moveReviewLedger.getOrElse(fail("expected sanitized ledger"))

    assertEquals(ledger.primaryLine, None, clue(ledger))
    assertEquals(ledger.resourceLine.map(_.source), Some("probe"), clue(ledger))
    assertEquals(ledger.resourceLine.map(_.sanMoves), Some(List("Nf3", "Nc6")), clue(ledger))
  }

  test("rejects typed strategic admissions without probe-backed support ids") {
    val plans =
      List(
        "empty_support" -> UserFacingPlanEligibility.ProbeBacked,
        "structural" -> UserFacingPlanEligibility.StructuralOnly,
        "pv_coupled" -> UserFacingPlanEligibility.PvCoupledOnly,
        "refuted" -> UserFacingPlanEligibility.Refuted
      ).map { case (id, eligibility) =>
        PlanHypothesis(
          planId = id,
          planName = s"$id plan",
          rank = 1,
          score = 0.7,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.7, "medium", "risk"),
          evidenceSources = List("probe_backed:validated_support")
        ) -> eligibility
      }
    val response =
      CommentResponse(
        commentary = "Typed non-admission payload",
        concepts = Nil,
        mainStrategicPlans = plans.map(_._1),
        strategicPlanExperiments =
          plans.map { case (plan, _) =>
            StrategicPlanExperiment(
              planId = plan.planId,
              themeL1 = "piece_redeployment",
              subplanId = None,
              evidenceTier = "evidence_backed",
              supportProbeCount = 1,
              refuteProbeCount = 0,
              bestReplyStable = true,
              futureSnapshotAligned = true,
              counterBreakNeutralized = true,
              moveOrderSensitive = false,
              experimentConfidence = 0.8
            )
          }
      )
    val typedAdmissions =
      plans.map { case (plan, eligibility) =>
        typedEvaluation(
          plan = plan,
          eligibility = eligibility,
          supportProbeIds =
            if eligibility == UserFacingPlanEligibility.ProbeBacked then Nil
            else List("probe_1")
        )
      }

    val sanitized =
      UserFacingPayloadSanitizer.sanitize(
        response,
        admittedPlans = typedAdmissions
      )

    assertEquals(sanitized.mainStrategicPlans, Nil, clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.strategicPlanExperiments, Nil, clue(sanitized.strategicPlanExperiments))
  }

  test("does not admit plans from mismatched typed evaluator keys") {
    val payloadPlan =
      PlanHypothesis(
        planId = "payload_plan",
        planName = "Payload plan",
        rank = 1,
        score = 0.7,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.7, "medium", "risk"),
        subplanId = Some("payload_subplan"),
        evidenceSources = List("probe_backed:validated_support")
      )
    val unrelatedTypedPlan =
      payloadPlan.copy(planId = "other_plan", subplanId = Some("other_subplan"))
    val response =
      CommentResponse(
        commentary = "Mismatched typed payload",
        concepts = Nil,
        mainStrategicPlans = List(payloadPlan),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "payload_plan",
            themeL1 = "piece_redeployment",
            subplanId = Some("payload_subplan"),
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = true,
            moveOrderSensitive = false,
            experimentConfidence = 0.8
          )
        )
      )

    val sanitized =
      UserFacingPayloadSanitizer.sanitize(
        response,
        admittedPlans = List(probeBackedEvaluation(unrelatedTypedPlan))
      )

    assertEquals(sanitized.mainStrategicPlans, Nil, clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.strategicPlanExperiments, Nil, clue(sanitized.strategicPlanExperiments))
  }

  test("preserves already sanitized cached moveReview continuity and ledger fields") {
    val cachedPlan =
      PlanHypothesis(
        planId = "cached_plan",
        planName = "Cached plan",
        rank = 1,
        score = 0.7,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.7, "medium", "risk"),
        evidenceSources = Nil
      )
    val response =
      CommentResponse(
        commentary = "Cached payload",
        concepts = Nil,
        mainStrategicPlans = List(cachedPlan),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "cached_plan",
            themeL1 = "piece_redeployment",
            subplanId = None,
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = true,
            moveOrderSensitive = false,
            experimentConfidence = 0.8
          )
        ),
        planStateToken = Some(PlanStateTracker()),
        moveReviewLedger = Some(
          MoveReviewStrategicLedger(
            motifKey = "cached",
            motifLabel = "Cached motif",
            stageKey = "hold",
            stageLabel = "Hold",
            carryOver = false
          )
        ),
        moveReviewPlayerSurface = Some(
          MoveReviewPlayerSurface(
            summaryRows = List(MoveReviewPlayerSurfaceRow(label = "Plan", text = "Cached typed surface"))
          )
        )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitizeCachedMoveReview(response)

    assertEquals(sanitized.mainStrategicPlans.map(_.planId), List("cached_plan"), clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.strategicPlanExperiments.map(_.planId), List("cached_plan"), clue(sanitized.strategicPlanExperiments))
    assert(sanitized.planStateToken.nonEmpty, clue(sanitized.planStateToken))
    assert(sanitized.moveReviewLedger.nonEmpty, clue(sanitized.moveReviewLedger))
    assert(sanitized.moveReviewPlayerSurface.nonEmpty, clue(sanitized.moveReviewPlayerSurface))
  }

  test("preserves valid moveReview player surface authority") {
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface =
          Some(
            MoveReviewPlayerSurface(
              summaryRows =
                List(
                  MoveReviewPlayerSurfaceRow(
                    label = "Counterplay break",
                    text = "On the checked line, this stops the ...c5 break before it appears.",
                    authority =
                      Some(
                        MoveReviewSurfaceAuthority(
                          kind = MoveReviewSurfaceAuthority.CounterplayBreak,
                          token = Some("...c5")
                        )
                      )
                  )
                )
            )
          )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.summaryRows.flatMap(_.authority)),
      List(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5"))),
      clue(sanitized.moveReviewPlayerSurface)
    )
  }

  test("preserves valid practical moveReview player surface authority") {
    val rows =
      List(
        MoveReviewPlayerSurfaceRow(
          label = "Practical plan",
          text = "The structure points toward Carlsbad pressure as a practical plan.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Central liquidation",
          text = "The move releases central tension through d5-e4.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralLiquidation, token = Some("...d5-e4")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Central challenge",
          text = "The move challenges the center through d7-d6.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralChallenge, token = Some("...d7-d6")))
        )
      )
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface = Some(MoveReviewPlayerSurface(summaryRows = rows))
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.summaryRows.flatMap(_.authority).map(_.kind)),
      List(
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.CentralLiquidation,
        MoveReviewSurfaceAuthority.CentralChallenge
      ),
      clue(sanitized.moveReviewPlayerSurface)
    )
  }

  test("preserves bounded strategic relation authority on summary and advanced rows while dropping malformed relation tokens") {
    val summaryCueRow =
      MoveReviewPlayerSurfaceRow(
        label = "Line relation",
        text = "Why it works: the line runs from e4 through f5 toward g6. Next check: the line geometry through e4, f5, and g6.",
        authority =
          Some(
            MoveReviewSurfaceAuthority(
              kind = MoveReviewSurfaceAuthority.StrategicRelation,
              token = Some("xray"),
              target = Some("g6")
            )
          )
      )
    val rows =
      List(
        MoveReviewPlayerSurfaceRow(
          label = "Line relation",
          text = "The checked line gives x-ray evidence around e4, f5, g6.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.StrategicRelation,
                token = Some("xray"),
                target = Some("g6")
              )
            )
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Line relation",
          text = "This untargeted relation token should lose authority.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.StrategicRelation,
                token = Some("xray")
              )
            )
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Line relation",
          text = "This malformed relation token should lose authority.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.StrategicRelation,
                token = Some("source:xray_relation"),
                target = Some("g6")
              )
            )
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Line relation",
          text = "This uncataloged relation token should lose authority.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.StrategicRelation,
                token = Some("unsupported_relation"),
                target = Some("g6")
              )
            )
        )
      )
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface = Some(MoveReviewPlayerSurface(summaryRows = List(summaryCueRow), advancedRows = rows))
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.summaryRows.map(_.authority)),
      List(
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.StrategicRelation,
            token = Some("xray"),
            target = Some("g6")
          )
        )
      ),
      clue(sanitized.moveReviewPlayerSurface)
    )
    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.advancedRows.map(_.authority)),
      List(
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.StrategicRelation,
            token = Some("xray"),
            target = Some("g6")
          )
        ),
        None,
        None,
        None
      ),
      clue(sanitized.moveReviewPlayerSurface)
    )
  }

  test("strategic relation sanitizer accepts exactly the implemented relation catalog") {
    val catalogTokens = RelationObservationCatalog.Implemented.map(_.relationKind)
    val deferredTokens = RelationObservationCatalog.DeferredRelationKinds

    assertEquals(catalogTokens.distinct.size, catalogTokens.size, clue(catalogTokens))
    assertEquals(catalogTokens.toSet, RelationObservationCatalog.ImplementedKinds, clue(catalogTokens))
    assertEquals(RelationObservationCatalog.ImplementedKinds, MoveReviewExchangeAnalyzer.RelationKind.Implemented.toSet, clue(catalogTokens))
    assertEquals(deferredTokens.toSet.intersect(catalogTokens.toSet), Set.empty[String], clue(deferredTokens))

    val implementedRows =
      catalogTokens.map { token =>
        MoveReviewPlayerSurfaceRow(
          label = "Line relation",
          text = s"The checked line supports the $token relation.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.StrategicRelation,
                token = Some(token),
                target = Some("g6")
              )
            )
        )
      }
    val deferredRows =
      deferredTokens.map { token =>
        MoveReviewPlayerSurfaceRow(
          label = "Line relation",
          text = s"Deferred relation $token should not become public authority.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.StrategicRelation,
                token = Some(token),
                target = Some("g6")
              )
            )
        )
      }
    val rows =
      (implementedRows ++ deferredRows) :+
        MoveReviewPlayerSurfaceRow(
          label = "Line relation",
          text = "This uncataloged relation token should lose authority.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.StrategicRelation,
                token = Some("unsupported_relation"),
                target = Some("g6")
              )
            )
        )
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface = Some(MoveReviewPlayerSurface(advancedRows = rows))
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val authorities = sanitized.moveReviewPlayerSurface.toList.flatMap(_.advancedRows.map(_.authority))

    assertEquals(
      authorities,
      (
        catalogTokens.map(token =>
          Some(
            MoveReviewSurfaceAuthority(
              kind = MoveReviewSurfaceAuthority.StrategicRelation,
              token = Some(token),
              target = Some("g6")
            )
          )
        ) ++ deferredTokens.map(_ => None)
      ) :+ None,
      clue(deferredTokens)
    )
    assertEquals(
      authorities.slice(catalogTokens.size, catalogTokens.size + deferredTokens.size),
      deferredTokens.map(_ => None),
      clue(sanitized.moveReviewPlayerSurface)
    )
  }

  test("strips raw strategic idea carriers while preserving strategic relation surface authority") {
    val admittedPlan =
      PlanHypothesis(
        planId = "pressure_plan",
        planName = "Pressure Plan",
        rank = 1,
        score = 0.72,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.72, "medium", "risk")
      )
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = List("source:xray_relation", "xray_semantic", "raw_proof_family")
      )
    val relationRow =
      MoveReviewPlayerSurfaceRow(
        label = "Line relation",
        text = "The checked line gives x-ray evidence around e4, f5, g6.",
        authority =
          Some(
            MoveReviewSurfaceAuthority(
              kind = MoveReviewSurfaceAuthority.StrategicRelation,
              token = Some("xray"),
              target = Some("g6")
            )
          )
      )
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        mainStrategicPlans = List(admittedPlan),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              plans = List(StrategySidePlan("white", "long", "Line pressure")),
              strategicIdeas = List(relationIdea)
            )
          ),
        moveReviewPlayerSurface = Some(MoveReviewPlayerSurface(advancedRows = List(relationRow)))
      )

    val sanitized =
      UserFacingPayloadSanitizer.sanitize(response, admittedPlans = List(probeBackedEvaluation(admittedPlan)))

    assertEquals(sanitized.strategyPack.toList.flatMap(_.strategicIdeas), Nil, clue(sanitized.strategyPack))
    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.advancedRows.flatMap(_.authority)),
      List(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("xray"),
          target = Some("g6")
        )
      ),
      clue(sanitized.moveReviewPlayerSurface)
    )
  }

  test("strips legacy relation evidence terms from sanitized strategy pack support metadata") {
    val admittedPlan =
      PlanHypothesis(
        planId = "pressure_plan",
        planName = "Pressure Plan",
        rank = 1,
        score = 0.72,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.72, "medium", "risk")
      )
    val fallbackTerm = "piece_mobility"
    val pack =
      StrategyPack(
        sideToMove = "white",
        plans = List(StrategySidePlan("white", "long", "Pressure Plan")),
        pieceRoutes =
          List(
            StrategyPieceRoute(
              side = "white",
              piece = "N",
              from = "c2",
              route = List("e3"),
              purpose = "Improve the piece",
              confidence = 0.7,
              evidence = List("trapped_piece_signal", fallbackTerm, "low_mobility_signal")
            )
          ),
        pieceMoveRefs =
          List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "N",
              from = "c2",
              target = "e3",
              idea = "Improve the piece",
              evidence = List("domination", "piece_activity")
            )
          ),
        directionalTargets =
          List(
            StrategyDirectionalTarget(
              targetId = "target_white_n_c2_e4",
              ownerSide = "white",
              piece = "N",
              from = "c2",
              targetSquare = "e4",
              readiness = DirectionalTargetReadiness.Premature,
              evidence = List("zwischenzug_line", fallbackTerm, "directional_target")
            )
          ),
        evidence = List("perpetual_check", fallbackTerm, "idea:line_occupation:xray")
      )
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        mainStrategicPlans = List(admittedPlan),
        strategyPack = Some(pack),
        moveReviewPlayerSurface = Some(MoveReviewPlayerSurface())
      )

    val sanitized =
      UserFacingPayloadSanitizer.sanitizeCachedMoveReview(response)
    val sanitizedPack = sanitized.strategyPack.getOrElse(fail("sanitized strategy pack missing"))
    val publicEvidence =
      sanitizedPack.evidence ++
        sanitizedPack.pieceRoutes.flatMap(_.evidence) ++
        sanitizedPack.pieceMoveRefs.flatMap(_.evidence) ++
        sanitizedPack.directionalTargets.flatMap(_.evidence)

    assert(publicEvidence.contains(fallbackTerm), clue(publicEvidence))
    assert(publicEvidence.contains("low_mobility_signal"), clue(publicEvidence))
    assert(publicEvidence.contains("directional_target"), clue(publicEvidence))
    assert(!publicEvidence.contains("zwischenzug_line"), clue(publicEvidence))
    assert(!publicEvidence.contains("perpetual_check"), clue(publicEvidence))
    assert(!publicEvidence.exists(RelationObservationCatalog.deferredFallbackForMotifTag(_).nonEmpty), clue(publicEvidence))
  }

  test("drops invalid moveReview player surface authority") {
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface =
          Some(
            MoveReviewPlayerSurface(
              summaryRows =
                List(
                  MoveReviewPlayerSurfaceRow(
                    label = "Counterplay break",
                    text = "On the checked line, this stops the ...c5 break before it appears.",
                    authority =
                      Some(
                        MoveReviewSurfaceAuthority(
                          kind = MoveReviewSurfaceAuthority.CounterplayBreak,
                          token = Some("counterplay_axis_suppression")
                        )
                      )
                  )
                )
            )
          )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.summaryRows.map(_.authority)),
      List(None),
      clue(sanitized.moveReviewPlayerSurface)
    )
  }

  test("requires route-shaped central break authority while preserving square counterplay tokens") {
    val rows =
      List(
        MoveReviewPlayerSurfaceRow(
          label = "Central break",
          text = "On the checked line, this also plays the d4-d5 break at this moment.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralBreak, token = Some("d5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Counterplay break",
          text = "On the checked line, this stops the d5 break before it appears.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("d5")))
        )
      )
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface = Some(MoveReviewPlayerSurface(summaryRows = rows))
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.summaryRows.map(_.authority)),
      List(
        None,
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("d5")))
      ),
      clue(sanitized.moveReviewPlayerSurface)
    )
  }

  test("drops malformed practical authorities and preserves only allowlisted opening targets") {
    val rows =
      List(
        MoveReviewPlayerSurfaceRow(
          label = "Practical plan",
          text = "The structure points toward Carlsbad pressure as a practical plan.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, token = Some("d4-d5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Central liquidation",
          text = "The move releases central tension through d5-e4.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralLiquidation, token = Some("d5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Opening family",
          text = "The opening family is visible.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.OpeningFamily,
                openingFamily = Some("queens_gambit"),
                target = Some("d5")
              )
            )
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Opening family",
          text = "The opening family is visible without a trusted target.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.OpeningFamily,
                openingFamily = Some("queens_gambit"),
                target = Some("h4")
              )
            )
        )
      )
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface = Some(MoveReviewPlayerSurface(summaryRows = rows))
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.summaryRows.map(_.authority)),
      List(
        None,
        None,
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.OpeningFamily,
            openingFamily = Some("queens_gambit"),
            target = Some("d5")
          )
        ),
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.OpeningFamily, openingFamily = Some("queens_gambit")))
      ),
      clue(sanitized.moveReviewPlayerSurface)
    )
  }

  test("preserves opening targets from the family catalog instead of the legacy source allowlist") {
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface =
          Some(
            MoveReviewPlayerSurface(
              summaryRows =
                List(
                  MoveReviewPlayerSurfaceRow(
                    label = "Opening family",
                    text = "The Caro-Kann center leaves d5 as a target.",
                    authority =
                      Some(
                        MoveReviewSurfaceAuthority(
                          kind = MoveReviewSurfaceAuthority.OpeningFamily,
                          openingFamily = Some("caro_kann"),
                          target = Some("d5")
                        )
                      )
                  ),
                  MoveReviewPlayerSurfaceRow(
                    label = "Opening family",
                    text = "The Caro-Kann center should not trust h4 as a target.",
                    authority =
                      Some(
                        MoveReviewSurfaceAuthority(
                          kind = MoveReviewSurfaceAuthority.OpeningFamily,
                          openingFamily = Some("caro_kann"),
                          target = Some("h4")
                        )
                      )
                  )
                )
            )
          )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.summaryRows.map(_.authority)),
      List(
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.OpeningFamily,
            openingFamily = Some("caro_kann"),
            target = Some("d5")
          )
        ),
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.OpeningFamily, openingFamily = Some("caro_kann")))
      )
    )
  }

  test("sanitizes opening book metadata only on opening-family authority") {
    val rows =
      List(
        MoveReviewPlayerSurfaceRow(
          label = "Opening family",
          text = "The Queen's Gambit family carries public book metadata.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.OpeningFamily,
                openingFamily = Some("queens_gambit"),
                openingBook =
                  Some(
                    MoveReviewOpeningBookMetadata(
                      eco = Some("d06"),
                      totalGames = Some(12345),
                      topMoves = List("e6", "raw note", "Nf6")
                    )
                  )
              )
            )
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Counterplay break",
          text = "Non-opening authorities must not carry opening book metadata.",
          authority =
            Some(
              MoveReviewSurfaceAuthority(
                kind = MoveReviewSurfaceAuthority.CounterplayBreak,
                token = Some("d5"),
                openingBook = Some(MoveReviewOpeningBookMetadata(eco = Some("D06"), totalGames = Some(12345)))
              )
            )
        )
      )
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface = Some(MoveReviewPlayerSurface(summaryRows = rows))
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(
      sanitized.moveReviewPlayerSurface.toList.flatMap(_.summaryRows.map(_.authority)),
      List(
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.OpeningFamily,
            openingFamily = Some("queens_gambit"),
            openingBook =
              Some(MoveReviewOpeningBookMetadata(eco = Some("D06"), totalGames = Some(12345), topMoves = List("e6", "Nf6")))
          )
        ),
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("d5")))
      )
    )
  }

  test("sanitizes moveReview decision target comparison metadata") {
    val response =
      CommentResponse(
        commentary = "Public payload",
        concepts = Nil,
        moveReviewPlayerSurface =
          Some(
            MoveReviewPlayerSurface(
              decisionComparison =
                Some(
                  MoveReviewPlayerDecisionComparison(
                    kicker = "Decision point",
                    secondaryText = Some("The lines leave different targets."),
                    chosenMatchesBest = false,
                    targetComparison =
                      Some(
                        MoveReviewDecisionTargetComparison(
                          chosenTarget = "e5",
                          chosenTargetKind = "isolated_pawn",
                          bestTarget = "d5",
                          bestTargetKind = "backward_pawn"
                        )
                      )
                  )
                )
            )
          )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(
      sanitized.moveReviewPlayerSurface.flatMap(_.decisionComparison.flatMap(_.targetComparison)),
      Some(
        MoveReviewDecisionTargetComparison(
          chosenTarget = "e5",
          chosenTargetKind = "isolated_pawn",
          bestTarget = "d5",
          bestTargetKind = "backward_pawn"
        )
      )
    )
  }

  test("does not preserve marker-only cached moveReview strategic metadata") {
    val markerPlan =
      PlanHypothesis(
        planId = "cached_marker",
        planName = "Cached marker plan",
        rank = 1,
        score = 0.7,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.7, "medium", "risk"),
        evidenceSources = List("probe_backed:validated_support")
      )
    val response =
      CommentResponse(
        commentary = "Cached marker payload",
        concepts = Nil,
        mainStrategicPlans = List(markerPlan),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "cached_marker",
            themeL1 = "piece_redeployment",
            subplanId = None,
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = true,
            moveOrderSensitive = false,
            experimentConfidence = 0.8
          )
        ),
        planStateToken = Some(PlanStateTracker()),
        moveReviewPlayerSurface = Some(
          MoveReviewPlayerSurface(
            summaryRows = List(MoveReviewPlayerSurfaceRow(label = "Plan", text = "Cached typed surface"))
          )
        )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitizeCachedMoveReview(response)

    assertEquals(sanitized.mainStrategicPlans, Nil, clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.strategicPlanExperiments, Nil, clue(sanitized.strategicPlanExperiments))
    assertEquals(sanitized.planStateToken, None, clue(sanitized.planStateToken))
  }

  test("humanizes raw plan label families in user-facing payloads") {
    val response =
      CommentResponse(
        commentary = "Preparing e-break Break.",
        concepts = Nil,
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "break",
            planName = "Preparing e-break Break",
            rank = 1,
            score = 0.66,
            preconditions = Nil,
            executionSteps = List("Piece Activation"),
            failureModes = List("Opening Development and Center Control"),
            viability = PlanViability(0.66, "medium", "Exploiting Space Advantage"),
            refutation = Some("Simplification into Endgame"),
            evidenceSources = List("probe_backed:validated_support")
          )
        ),
        signalDigest = Some(
          NarrativeSignalDigest(
            latentPlan = Some("Preparing d-break Break"),
            latentReason = Some("Simplification into Endgame")
          )
        )
      )

    val sanitized =
      UserFacingPayloadSanitizer.sanitize(
        response,
        admittedPlans = response.mainStrategicPlans.map(probeBackedEvaluation)
      )

    assertEquals(sanitized.mainStrategicPlans.map(_.planName), List("Preparing the e-break"), clue(sanitized))
    assertEquals(
      sanitized.mainStrategicPlans.flatMap(_.executionSteps),
      List("Improving piece placement"),
      clue(sanitized)
    )
    assertEquals(
      sanitized.mainStrategicPlans.flatMap(_.failureModes),
      List("Development and central control"),
      clue(sanitized)
    )
    assertEquals(
      sanitized.mainStrategicPlans.map(_.viability.risk),
      List("Using the space advantage"),
      clue(sanitized)
    )
    assertEquals(
      sanitized.mainStrategicPlans.flatMap(_.refutation),
      List("Simplifying toward an endgame"),
      clue(sanitized)
    )
    assertEquals(sanitized.signalDigest.flatMap(_.latentPlan), None, clue(sanitized))
    assertEquals(sanitized.signalDigest.flatMap(_.latentReason), None, clue(sanitized))
  }

  test("drops internal plan diagnostic details while preserving useful strategic detail") {
    val plan =
      PlanHypothesis(
        planId = "central_break",
        planName = "Validated central break",
        rank = 1,
        score = 0.72,
        preconditions = List("candidate move e4e5", "The center can open"),
        executionSteps = List("test e4e5 as the concrete pawn break", "Improve the knight before opening the center"),
        failureModes = List("requires probe validation before being asserted", "The route must not hang a piece"),
        viability = PlanViability(0.72, "medium", "probe evidence pending"),
        refutation = Some("use probe replies to verify whether the opened structure favors this side"),
        evidenceSources = List("probe_backed:validated_support")
      )
    val response =
      CommentResponse(
        commentary = "Central break payload",
        concepts = Nil,
        mainStrategicPlans = List(plan),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              plans =
                List(
                  StrategySidePlan(
                    side = "white",
                    horizon = "long",
                    planName = "Validated central break",
                    priorities = List("candidate move e4e5", "Improve the knight before opening the center"),
                    riskTriggers = List("raw request purpose should not surface", "Keep the route safe")
                  )
                ),
              longTermFocus = List("white execution: candidate move e4e5", "white plan: improve the knight")
            )
          )
      )

    val sanitized =
      UserFacingPayloadSanitizer.sanitize(
        response,
        admittedPlans = List(probeBackedEvaluation(plan))
      )
    val sanitizedPlan =
      sanitized.mainStrategicPlans.headOption.getOrElse(fail("expected sanitized plan"))
    val sanitizedPack =
      sanitized.strategyPack.getOrElse(fail("expected sanitized strategy pack"))
    val rendered =
      (
        sanitizedPlan.preconditions ++
          sanitizedPlan.executionSteps ++
          sanitizedPlan.failureModes ++
          sanitizedPlan.refutation.toList ++
          List(sanitizedPlan.viability.risk) ++
          sanitizedPack.plans.flatMap(plan => plan.priorities ++ plan.riskTriggers) ++
          sanitizedPack.longTermFocus
      ).mkString(" ")

    assertEquals(sanitizedPlan.preconditions, List("The center can open"), clue(sanitizedPlan))
    assertEquals(sanitizedPlan.executionSteps, List("Improve the knight before opening the center"), clue(sanitizedPlan))
    assertEquals(sanitizedPlan.failureModes, List("The route must not hang a piece"), clue(sanitizedPlan))
    assertEquals(sanitizedPlan.refutation, None, clue(sanitizedPlan))
    assertEquals(sanitizedPlan.viability.risk, "", clue(sanitizedPlan))
    assertEquals(
      sanitizedPack.plans.flatMap(_.priorities),
      List("Improve the knight before opening the center"),
      clue(sanitizedPack)
    )
    assertEquals(sanitizedPack.plans.flatMap(_.riskTriggers), List("Keep the route safe"), clue(sanitizedPack))
    assertEquals(sanitizedPack.longTermFocus, List("white plan: improve the knight"), clue(sanitizedPack))
    assertNoLeaks(rendered)
  }

  test("sanitizes stored strategic puzzle runtime shell on read") {
    val shell =
      RuntimeShell(
        schema = RuntimeShellSchema,
        startFen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        sideToMove = "white",
        prompt = "PlayableByPV prompt with return vector and cash out",
        plans = List(
          PuzzlePlan(
            id = "plan_attack",
            familyKey = Some("king_attack_build_up|h7"),
            dominantIdeaKind = Some("king_attack_build_up"),
            anchor = Some("{them}"),
            task = "cash out the compensation before the return vector lands",
            feedback = "engine-coupled continuation and {seed}",
            allowedStarts =
              List(
                PlanStart(
                  uci = "e2e4",
                  san = "e4",
                  credit = "full",
                  label = Some("PlayedPV label"),
                  feedback = "engine-coupled continuation and {seed}",
                  afterFen = None,
                  terminalId = Some("t1")
                )
              ),
            featuredTerminalId = "t1",
            featuredStartUci = Some("e2e4")
          )
        ),
        proof = RuntimeProofLayer(
          rootChoices = List(
            ShellChoice(
              uci = "e2e4",
              san = "e4",
              credit = "full",
              nextNodeId = Some("n1"),
              terminalId = None,
              afterFen = None,
              familyKey = None,
              label = Some("PlayedPV label"),
              feedback = "engine-coupled continuation and {seed}"
            )
          ),
          nodes = List(
            PlayerNode(
              id = "n1",
              step = 1,
              fen = "4k3/8/8/8/8/8/8/4K3 b - - 0 1",
              prompt = "probe evidence pending",
              badMoveFeedback = "return vector through line pressure",
              choices = Nil
            )
          ),
          forcedReplies = Nil
        ),
        terminals = List(
          TerminalReveal(
            id = "t1",
            outcome = "full",
            title = "PlayableByPV title",
            summary = "cash out the compensation",
            commentary = "The return vector only holds if delayed recovery keeps the line pressure alive.",
            familyKey = None,
            dominantIdeaKind = None,
            anchor = Some("{us}"),
            lineSan = Nil,
            siblingMoves = Nil,
            opening = Some("theme:piece_redeployment"),
            eco = None,
            dominantFamilyKey = None,
            planId = Some("plan_attack"),
            planTask = Some("cash out the compensation before the return vector lands"),
            whyPlan = Some("engine-coupled continuation and {seed}"),
            whyMove = Some("The return vector only holds if delayed recovery keeps the line pressure alive."),
            acceptedStarts = List("e4", "Qe2"),
            featuredStart = Some("e4")
          )
        )
      )

    val payload =
      BootstrapPayload(
        puzzle =
          StrategicPuzzleDoc(
            id = "sp1",
            schema = "chesstory.strategicPuzzle.v1",
            source = SourcePayload(seedId = "seed", opening = None, eco = None),
            position = PositionPayload(fen = shell.startFen, sideToMove = shell.sideToMove),
            dominantFamily = Some(DominantFamilySummary(key = "attack", dominantIdeaKind = "king_attack_build_up", anchor = "cash out")),
            qualityScore = QualityScore(total = 90),
            generationMeta = GenerationMeta(selectionStatus = PublicSelectionStatus),
            runtimeShell = Some(shell)
          ),
        runtimeShell = shell,
        progress = ProgressPayload(authenticated = false, currentStreak = 0, recentAttempts = Nil)
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(payload)
    val rendered =
      List(
        sanitized.runtimeShell.prompt,
        sanitized.runtimeShell.plans.flatMap(plan => List(plan.task, plan.feedback) ++ plan.anchor.toList ++ plan.allowedStarts.flatMap(start => start.label.toList :+ start.feedback)).mkString(" "),
        sanitized.runtimeShell.proof.rootChoices.flatMap(choice => choice.label.toList :+ choice.feedback).mkString(" "),
        sanitized.runtimeShell.proof.nodes.flatMap(node => List(node.prompt, node.badMoveFeedback)).mkString(" "),
        sanitized.runtimeShell.terminals.flatMap(reveal => List(reveal.title, reveal.summary, reveal.commentary) ++ reveal.anchor.toList ++ reveal.opening.toList ++ reveal.planTask.toList ++ reveal.whyPlan.toList ++ reveal.whyMove.toList ++ reveal.acceptedStarts ++ reveal.featuredStart.toList).mkString(" "),
        sanitized.puzzle.dominantFamily.toList.map(_.anchor).mkString(" ")
      ).mkString(" ")

    assertNoLeaks(rendered)
    assert(rendered.toLowerCase.contains("pays off") || rendered.toLowerCase.contains("current engine line"), clue(rendered))
  }
