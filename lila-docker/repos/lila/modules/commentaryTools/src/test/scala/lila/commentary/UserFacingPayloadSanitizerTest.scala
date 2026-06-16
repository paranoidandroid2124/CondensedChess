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
    "return vector",
    "cash out"
  )

  private def assertNoLeaks(text: String): Unit =
    bannedPhrases.foreach { phrase =>
      assert(!text.toLowerCase.contains(phrase.toLowerCase), clue(text))
    }

  private def assertNoRelationSupportLeaks(values: Seq[String]): Unit =
    assert(!values.exists(_.trim.toLowerCase.startsWith("deferred_")), clue(values))
    assert(!values.exists(RelationObservationCatalog.deferredFallbackForMotifTag(_).nonEmpty), clue(values))
    assert(!values.exists(RelationObservationCatalog.relationWitnessOnlyMotifTag), clue(values))
    assert(!values.exists(RelationObservationCatalog.pvDrawResourceOnlyMotifTag), clue(values))

  test("user-facing signal sanitizer routes witness-only helpers and suppresses PV-only draw-resource helpers") {
    val text =
      UserFacingSignalSanitizer.sanitize(
        "Domination(Knight,e5) TrappedPiece(Queen,h4) Zwischenzug(Nf7) StalemateTrap(g8) PerpetualCheck(h5) SmotheredMate(f7)."
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
    assert(!lower.contains("smothered"), clue(text))
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

  private def strategicPlan(
      planId: String,
      planName: String,
      rank: Int = 1,
      score: Double = 0.7,
      risk: String = "risk",
      refutation: Option[String] = None,
      subplanId: Option[String] = None,
      preconditions: List[String] = Nil,
      executionSteps: List[String] = Nil,
      failureModes: List[String] = Nil,
      evidenceSources: List[String] = List("probe_backed:validated_support")
  ): PlanHypothesis =
    PlanHypothesis(
      planId = planId,
      planName = planName,
      rank = rank,
      score = score,
      preconditions = preconditions,
      executionSteps = executionSteps,
      failureModes = failureModes,
      viability = PlanViability(score, "medium", risk),
      refutation = refutation,
      subplanId = subplanId,
      evidenceSources = evidenceSources
    )

  private def strategicExperiment(
      planId: String,
      themeL1: String = "piece_redeployment",
      subplanId: Option[String] = None,
      evidenceTier: String = "evidence_backed",
      supportProbeCount: Int = 1,
      refuteProbeCount: Int = 0,
      bestReplyStable: Boolean = true,
      futureSnapshotAligned: Boolean = true,
      counterBreakNeutralized: Boolean = true,
      moveOrderSensitive: Boolean = false,
      experimentConfidence: Double = 0.8
  ): StrategicPlanExperiment =
    StrategicPlanExperiment(
      planId = planId,
      themeL1 = themeL1,
      subplanId = subplanId,
      evidenceTier = evidenceTier,
      supportProbeCount = supportProbeCount,
      refuteProbeCount = refuteProbeCount,
      bestReplyStable = bestReplyStable,
      futureSnapshotAligned = futureSnapshotAligned,
      counterBreakNeutralized = counterBreakNeutralized,
      moveOrderSensitive = moveOrderSensitive,
      experimentConfidence = experimentConfidence
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

  private def responseWithSurface(surface: MoveReviewPlayerSurface): CommentResponse =
    CommentResponse(
      commentary = "Public payload",
      concepts = Nil,
      moveReviewPlayerSurface = Some(surface)
    )

  private def strategicResponse(
      commentary: String,
      plans: List[PlanHypothesis],
      experiments: List[StrategicPlanExperiment] = Nil
  ): CommentResponse =
    CommentResponse(
      commentary = commentary,
      concepts = Nil,
      mainStrategicPlans = plans,
      strategicPlanExperiments = experiments
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
          strategicPlan(
            planId = "PieceActivation",
            planName = "PlayableByPV plan",
            preconditions = List("theme:piece_redeployment", "{seed}"),
            executionSteps = List("cash out through c-file pressure"),
            failureModes = List("probe evidence pending"),
            risk = "return vector collapse",
            refutation = Some("engine-coupled continuation fails"),
            subplanId = Some("rook_lift_scaffold"),
            evidenceSources = List("probe_backed:validated_support")
          )
        ),
        strategicPlanExperiments = List(
          strategicExperiment(
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
            prerequisites = List("theme:piece_redeployment", "perpetual_check", "deferred_move_order_caution"),
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
    val ledgerPrerequisites = sanitized.moveReviewLedger.toList.flatMap(_.prerequisites)
    assertNoRelationSupportLeaks(ledgerPrerequisites)
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
      strategicPlan(
        planId = "marker_only",
        planName = "Marker-only plan",
        risk = "marker-only risk",
        evidenceSources = List("probe_backed:validated_support")
      )
    val response =
      strategicResponse(
        commentary = "Marker-only payload",
        plans = List(markerOnlyPlan),
        experiments = List(
          strategicExperiment(planId = "marker_only")
        )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)

    assertEquals(sanitized.mainStrategicPlans, Nil, clue(sanitized.mainStrategicPlans))
    assertEquals(sanitized.strategicPlanExperiments, Nil, clue(sanitized.strategicPlanExperiments))
  }

  test("admits strategic plans only from typed evaluator decisions") {
    val admittedPlan =
      strategicPlan(
        planId = "admitted",
        planName = "Admitted plan"
      )
    val deferredPlan =
      strategicPlan(
        planId = "deferred",
        planName = "Deferred plan",
        rank = 2,
        score = 0.65
      )
    val response =
      strategicResponse(
        commentary = "Typed admission payload",
        plans = List(admittedPlan, deferredPlan),
        experiments = List(
          strategicExperiment(planId = "admitted"),
          strategicExperiment(planId = "deferred")
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
      strategicPlan(
        planId = "transposition",
        planName = "Transposition target plan",
        score = 0.76,
        evidenceSources = List("weakness_target:d5")
      )
    val response =
      strategicResponse(
        commentary = "Transposition admission payload",
        plans = List(plan),
        experiments = List(
          strategicExperiment(
            planId = "transposition",
            themeL1 = "weakness_fixation",
            evidenceTier = "transposition_aligned",
            supportProbeCount = 0,
            counterBreakNeutralized = false
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
      strategicPlan(
        planId = "ledger_plan",
        planName = "Ledger plan",
        score = 0.76,
        evidenceSources = Nil
      )
    val response =
      strategicResponse(
        commentary = "Ledger payload",
        plans = List(plan)
      ).copy(
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
        strategicPlan(
          planId = id,
          planName = s"$id plan"
        ) -> eligibility
      }
    val response =
      strategicResponse(
        commentary = "Typed non-admission payload",
        plans = plans.map(_._1),
        experiments =
          plans.map { case (plan, _) =>
            strategicExperiment(planId = plan.planId)
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
      strategicPlan(
        planId = "payload_plan",
        planName = "Payload plan",
        subplanId = Some("payload_subplan")
      )
    val unrelatedTypedPlan =
      payloadPlan.copy(planId = "other_plan", subplanId = Some("other_subplan"))
    val response =
      strategicResponse(
        commentary = "Mismatched typed payload",
        plans = List(payloadPlan),
        experiments = List(
          strategicExperiment(
            planId = "payload_plan",
            subplanId = Some("payload_subplan")
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
      strategicPlan(
        planId = "cached_plan",
        planName = "Cached plan",
        evidenceSources = Nil
      )
    val response =
      strategicResponse(
        commentary = "Cached payload",
        plans = List(cachedPlan),
        experiments = List(
          strategicExperiment(planId = "cached_plan")
        )
      ).copy(
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
      responseWithSurface(
        MoveReviewPlayerSurface(
          summaryRows =
            List(
              MoveReviewPlayerSurfaceRow(
                label = "Counterplay break",
                text = "This stops the ...c5 break before it appears.",
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

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.summaryRows.flatMap(_.authority),
      List(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5"))),
      clue(surface)
    )
  }

  test("preserves structured moveReview player surface schema identifier") {
    val response =
      responseWithSurface(
        MoveReviewPlayerSurface(
          schema = "chesstory.move_review.player_surface.v2",
          summaryRows = List(MoveReviewPlayerSurfaceRow(label = "Checked line", text = "Short line: Bc4 Nf6 d3."))
        )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.schema,
      "chesstory.move_review.player_surface.v2",
      clue(surface)
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
          label = "Fixed target",
          text = "The checked line keeps d6 fixed as the target.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Minority attack",
          text = "The checked line keeps c6 as the minority-attack fixed target.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "IQP target",
          text = "The checked line leaves d5 as an isolated pawn target.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Simplification",
          text = "The checked line keeps the same local edge after the exchange on e6.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Knight outpost",
          text = "The checked line puts the knight on the e5 outpost.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "File entry",
          text = "The checked line keeps pressure on c6 through the c-file.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Target coordination",
          text = "The checked line coordinates pressure on c6 from c1 and e3.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Color complex",
          text = "The checked line keeps the knight on c4 attacking e5 in the dark-square complex.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Color complex",
          text = "The checked line keeps the knight on c4 attacking e5 in the red-square complex.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Color complex",
          text = "The checked line keeps the queen on c4 attacking e5 in the dark-square complex.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Color complex",
          text = "The checked line keeps the bishop on c4 attacking e5 in the dark-square complex.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
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
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Target coordination",
          text = "The checked line coordinates pressure on c6 from c1 and c1.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
        )
      )
    val response =
      responseWithSurface(MoveReviewPlayerSurface(summaryRows = rows))

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.summaryRows.flatMap(_.authority).map(_.kind),
      List(
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.PracticalPlan,
        MoveReviewSurfaceAuthority.CentralLiquidation,
        MoveReviewSurfaceAuthority.CentralChallenge,
        MoveReviewSurfaceAuthority.PracticalPlan
      ),
      clue(surface)
    )
    assertEquals(
      surface.summaryRows.flatMap(_.authority.flatMap(_.target)),
      List("d6", "c6", "d5", "e6", "e5", "c6", "c6", "e5"),
      clue(surface)
    )
  }

  test("preserves bounded strategic relation authority and drops malformed relation tokens") {
    val validXrayAuthority =
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("xray"),
          target = Some("g6")
        )
      )
    val staleSummaryRow =
      MoveReviewPlayerSurfaceRow(
        label = "Line relation",
        text = "A stale summary relation row should lose authority.",
        authority = validXrayAuthority
      )
    val rows =
      List(
        MoveReviewPlayerSurfaceRow(
          label = "Line relation",
          text = "The checked line uses x-ray geometry around e4, f5, g6.",
          authority = validXrayAuthority
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
      responseWithSurface(MoveReviewPlayerSurface(summaryRows = List(staleSummaryRow), advancedRows = rows))

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.summaryRows.map(_.authority),
      List(None),
      clue(surface)
    )
    assertEquals(
      surface.advancedRows.map(_.authority),
      List(validXrayAuthority, None, None, None),
      clue(surface)
    )
  }

  test("cached player surface suppresses strategic relation rows duplicated by authoritative summary rows") {
    val practicalAuthority =
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    val defenderTradeAuthority =
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("defender_trade"),
          target = Some("e5")
        )
      )
    val mateNetAuthority =
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("mate_net"),
          target = Some("h8")
        )
      )
    val xrayAuthority =
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("xray"),
          target = Some("g6")
        )
      )
    val defenderSummary =
      MoveReviewPlayerSurfaceRow(
        label = "Defender trade",
        text = "The checked line trades on d4 to remove the defender from c5, loosening e5.",
        authority = defenderTradeAuthority
      )
    val mateSummary =
      MoveReviewPlayerSurfaceRow(
        label = "Smothered mate",
        text = "The checked line ends in smothered mate on h8 after h6f7.",
        authority = practicalAuthority
      )
    val duplicateAdvanced =
      MoveReviewPlayerSurfaceRow(
        label = "Line relation",
        text = "The checked line creates a defender trade motif around c5, d4, e5.",
        authority = defenderTradeAuthority
      )
    val mateAdvanced =
      MoveReviewPlayerSurfaceRow(
        label = "Line relation",
        text = "The checked line creates a mate net motif around h8, f7.",
        authority = mateNetAuthority
      )
    val otherAdvanced =
      MoveReviewPlayerSurfaceRow(
        label = "Line relation",
        text = "The checked line uses x-ray geometry around e4, f5, g6.",
        authority = xrayAuthority
      )
    val response =
      responseWithSurface(
        MoveReviewPlayerSurface(
          summaryRows = List(defenderSummary, mateSummary),
          advancedRows = List(duplicateAdvanced, mateAdvanced, otherAdvanced)
        )
      )

    val sanitized = UserFacingPayloadSanitizer.sanitizeCachedMoveReview(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(surface.summaryRows.map(_.label), List("Defender trade", "Smothered mate"), clue(surface))
    assertEquals(
      surface.summaryRows.flatMap(_.authority.flatMap(_.token)),
      List("defender_trade"),
      clue(surface.summaryRows)
    )
    assertEquals(surface.advancedRows.map(_.text), List(otherAdvanced.text), clue(surface.advancedRows))
    assertEquals(surface.advancedRows.flatMap(_.authority.flatMap(_.token)), List("xray"), clue(surface.advancedRows))
  }

  test("cached player surface does not suppress strategic relation rows from unauthoritative summary labels") {
    val staleSummary =
      MoveReviewPlayerSurfaceRow(
        label = "Defender trade",
        text = "A stale label without practical authority should not suppress advanced relation authority."
      )
    val advanced =
      MoveReviewPlayerSurfaceRow(
        label = "Line relation",
        text = "The checked line creates a defender trade motif around c5, d4, e5.",
        authority =
          Some(
            MoveReviewSurfaceAuthority(
              kind = MoveReviewSurfaceAuthority.StrategicRelation,
              token = Some("defender_trade"),
              target = Some("e5")
            )
          )
      )
    val response =
      responseWithSurface(MoveReviewPlayerSurface(summaryRows = List(staleSummary), advancedRows = List(advanced)))

    val sanitized = UserFacingPayloadSanitizer.sanitizeCachedMoveReview(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(surface.advancedRows.flatMap(_.authority.flatMap(_.token)), List("defender_trade"), clue(surface))
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
          text = s"The checked line uses $token geometry around g6.",
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
      responseWithSurface(MoveReviewPlayerSurface(advancedRows = rows))

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
      strategicPlan(
        planId = "pressure_plan",
        planName = "Pressure Plan",
        score = 0.72,
        evidenceSources = Nil
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
        text = "The checked line uses x-ray geometry around e4, f5, g6.",
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
      strategicResponse(
        commentary = "Public payload",
        plans = List(admittedPlan)
      ).copy(
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
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(sanitized.strategyPack.toList.flatMap(_.strategicIdeas), Nil, clue(sanitized.strategyPack))
    assertEquals(
      surface.advancedRows.flatMap(_.authority),
      List(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("xray"),
          target = Some("g6")
        )
      ),
      clue(surface)
    )
  }

  test("strips deferred and witness-only relation evidence from sanitized strategy pack support metadata") {
    val admittedPlan =
      strategicPlan(
        planId = "pressure_plan",
        planName = "Pressure Plan",
        score = 0.72,
        evidenceSources = Nil
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        plans =
          List(
            StrategySidePlan(
              side = "white",
              horizon = "long",
              planName = "Pressure Plan",
              priorities = List("trapped_piece", "Domination(Knight,e5)", "hold e4"),
              riskTriggers = List("deferred_move_order_caution", "clock pressure")
            )
          ),
        pieceRoutes =
          List(
            StrategyPieceRoute(
              side = "white",
              piece = "N",
              from = "c2",
              route = List("e3"),
              purpose = "Improve the piece",
              confidence = 0.7,
              evidence = List("TrappedPiece(Queen,h4)", "trapped_piece_signal", "low_mobility_signal")
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
              strategicReasons = List("domination", "directional target"),
              prerequisites = List("perpetual_check", "route ready"),
              evidence = List("zwischenzug_line", "Zwischenzug(Nf7)", "deferred_move_order_caution", "directional_target")
            )
          ),
        longTermFocus = List("stalemate_trap", "cash out"),
        evidence = List("perpetual_check", "deferred_move_order_caution", "idea:line_occupation:xray")
      )
    val response =
      strategicResponse(
        commentary = "Public payload",
        plans = List(admittedPlan)
      ).copy(
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
    val publicSupport =
      publicEvidence ++
        sanitizedPack.longTermFocus ++
        sanitizedPack.plans.flatMap(plan => plan.priorities ++ plan.riskTriggers) ++
        sanitizedPack.directionalTargets.flatMap(target => target.strategicReasons ++ target.prerequisites)

    assert(publicSupport.contains("low_mobility_signal"), clue(publicSupport))
    assert(publicSupport.contains("directional_target"), clue(publicSupport))
    assert(publicSupport.contains("hold e4"), clue(publicSupport))
    assert(publicSupport.contains("clock pressure"), clue(publicSupport))
    assert(publicSupport.contains("route ready"), clue(publicSupport))
    assert(!publicSupport.contains("key-square restriction"), clue(publicSupport))
    assert(!publicSupport.contains("piece mobility"), clue(publicSupport))
    assert(!publicSupport.contains("move-order caution"), clue(publicSupport))
    assertNoRelationSupportLeaks(publicSupport)
  }

  test("drops invalid moveReview player surface authority") {
    val response =
      responseWithSurface(
        MoveReviewPlayerSurface(
          summaryRows =
            List(
              MoveReviewPlayerSurfaceRow(
                label = "Counterplay break",
                text = "This stops the ...c5 break before it appears.",
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

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.summaryRows.map(_.authority),
      List(None),
      clue(surface)
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
          text = "This stops the d5 break before it appears.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("d5")))
        )
      )
    val response =
      responseWithSurface(MoveReviewPlayerSurface(summaryRows = rows))

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.summaryRows.map(_.authority),
      List(
        None,
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("d5")))
      ),
      clue(surface)
    )
  }

  test("drops malformed practical authorities and treats opening targets as context-only") {
    val rows =
      List(
        MoveReviewPlayerSurfaceRow(
          label = "Practical plan",
          text = "The structure points toward Carlsbad pressure as a practical plan.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, token = Some("d4-d5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Practical plan",
          text = "A generic practical plan target should not become public authority.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Simplification window",
          text = "An approximate simplification label should not carry target authority.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Knight outpost plan",
          text = "An approximate outpost label should not carry target authority.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Knight outpost",
          text = "The checked line puts the queen on the e5 outpost.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "File entry plan",
          text = "An approximate file-entry label should not carry target authority.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Target coordination plan",
          text = "An approximate coordination label should not carry target authority.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Color complex plan",
          text = "An approximate color-complex label should not carry target authority.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
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
      responseWithSurface(MoveReviewPlayerSurface(summaryRows = rows))

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.summaryRows.map(_.authority),
      List(
        None,
        None,
        None,
        None,
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
        None,
        None,
        None,
        None,
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.OpeningFamily,
            openingFamily = Some("queens_gambit")
          )
        ),
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.OpeningFamily, openingFamily = Some("queens_gambit")))
      ),
      clue(surface)
    )
  }

  test("strips stale practical-plan target metadata from exact labels without exact row wording") {
    val rows =
      List(
        MoveReviewPlayerSurfaceRow(
          label = "File entry",
          text = "The rook already has a practical c-file post.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "Minority attack",
          text = "The Carlsbad-type pawn shape makes c6 a natural queenside target for White's minority-attack ideas.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "File entry",
          text = "The checked line keeps pressure on c6 through the c-file.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
        ),
        MoveReviewPlayerSurfaceRow(
          label = "File entry",
          text = "The checked line keeps pressure on e6 through the c-file.",
          authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e6")))
        )
      )
    val response =
      responseWithSurface(MoveReviewPlayerSurface(summaryRows = rows))

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.summaryRows.map(_.authority),
      List(
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6"))),
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
      )
    )
  }

  test("strips opening targets even when the family catalog allows the square") {
    val response =
      responseWithSurface(
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

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.summaryRows.map(_.authority),
      List(
        Some(
          MoveReviewSurfaceAuthority(
            kind = MoveReviewSurfaceAuthority.OpeningFamily,
            openingFamily = Some("caro_kann")
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
      responseWithSurface(MoveReviewPlayerSurface(summaryRows = rows))

    val sanitized = UserFacingPayloadSanitizer.sanitize(response)
    val surface = sanitized.moveReviewPlayerSurface.getOrElse(fail("missing sanitized player surface"))

    assertEquals(
      surface.summaryRows.map(_.authority),
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
      responseWithSurface(
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
      strategicPlan(
        planId = "cached_marker",
        planName = "Cached marker plan"
      )
    val response =
      strategicResponse(
        commentary = "Cached marker payload",
        plans = List(markerPlan),
        experiments = List(
          strategicExperiment(planId = "cached_marker")
        )
      ).copy(
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
      strategicResponse(
        commentary = "Preparing e-break Break.",
        plans = List(
          strategicPlan(
            planId = "break",
            planName = "Preparing e-break Break",
            score = 0.66,
            executionSteps = List("Piece Activation"),
            failureModes = List("Opening Development and Center Control"),
            risk = "Exploiting Space Advantage",
            refutation = Some("Simplification into Endgame"),
            evidenceSources = List("probe_backed:validated_support")
          )
        )
      ).copy(
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
