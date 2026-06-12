package lila.commentary.tools.moveReview

import lila.commentary.*
import lila.commentary.analysis.*
import lila.commentary.analysis.claim.ProofContractRules
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.*
import lila.commentary.model.strategic.VariationLine
import munit.FunSuite

final class MoveReviewCoverageDiagnosticsTest extends FunSuite:

  private val italianBeforeBc4 =
    "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"

  private val italianOpening =
    OpeningReference(
      eco = Some("C50"),
      name = Some("Italian Game"),
      totalGames = 420000,
      topMoves = List(ExplorerMove("f1c4", "Bc4", 210000, 93000, 52000, 65000, 2460)),
      sampleGames = Nil
    )
  private val MadernaExactFen =
    "nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17"
  private val MadernaExactLines =
    List(
      VariationLine(List("e4e5", "d6e5", "f4e5", "d7e5", "c4e5"), scoreCp = 82, depth = 18),
      VariationLine(List("c1e3", "b7b5", "a5b6"), scoreCp = 36, depth = 18)
    )

  private def developmentGoal: OpeningGoals.Evaluation =
    OpeningGoals.Evaluation(
      goalName = "Development Logic",
      status = OpeningGoals.Status.Achieved,
      supportedEvidence = List("Minor piece developed"),
      missingEvidence = Nil,
      confidence = 0.86
    )

  private def ctx(
      fen: String,
      playedMove: String,
      playedSan: String,
      phase: String = "Opening",
      ply: Int = 5,
      phaseReason: String,
      opening: Option[OpeningReference],
      openingGoalEvaluation: Option[OpeningGoals.Evaluation] = None
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader(phase, "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = ply,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary(phaseReason, None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext(phase, phaseReason),
      candidates = List(
        CandidateInfo(
          move = playedSan,
          uci = Some(playedMove),
          annotation = "",
          planAlignment = phaseReason,
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None
        )
      ),
      facts = Nil,
      openingEvent = opening.map(ref => OpeningEvent.Intro(ref.eco.getOrElse(""), ref.name.getOrElse("Opening"), phaseReason, List(playedSan))),
      openingData = opening,
      openingGoalEvaluation =
        openingGoalEvaluation.orElse(Option.when(opening.exists(_.name.contains("Italian Game")) && playedMove == "f1c4")(developmentGoal)),
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def italianCtx: NarrativeContext =
    ctx(
      fen = italianBeforeBc4,
      playedMove = "f1c4",
      playedSan = "Bc4",
      phaseReason = "Italian Game development",
      opening = Some(italianOpening)
    )

  private def neutralizeCtx: NarrativeContext =
    ctx(
      fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
      playedMove = "e2e3",
      playedSan = "e3",
      phase = "Middlegame",
      ply = 20,
      phaseReason = "legal non-colliding move",
      opening = None
    )

  private def refsForLine(startFen: String, ucis: List[String], sans: List[String], lineId: String = "line_01"): MoveReviewRefs =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewRefs(
      startFen = startFen,
      startPly = NarrativeUtils.plyFromFen(startFen).map(_ + 1).getOrElse(1),
      variations = List(
        MoveReviewVariationRef(
          lineId = lineId,
          scoreCp = 16,
          mate = None,
          depth = 16,
          moves =
            ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
              val ply = NarrativeUtils.plyFromFen(startFen).map(_ + 1 + idx).getOrElse(idx + 1)
              MoveReviewMoveRef(
                refId = s"${lineId}_m${idx + 1}",
                san = san,
                uci = uci,
                fenAfter = fens(idx),
                ply = ply,
                moveNo = (ply + 1) / 2,
                marker = Some(if ply % 2 == 1 then s"${(ply + 1) / 2}." else s"${(ply + 1) / 2}...")
              )
            }
        )
      )
    )

  private def plannerInputs(ctx: NarrativeContext): QuestionPlannerInputs =
    QuestionPlannerInputsBuilder.build(ctx, None, truthContract = None, candidateEvidenceLines = Nil)

  private def centralScene: (NarrativeContext, QuestionPlannerInputs, PlayerFacingClaimPacket) =
    val data =
      CommentaryEngine
        .assessExtended(
          fen = MadernaExactFen,
          variations = MadernaExactLines,
          playedMove = Some("e4e5"),
          phase = Some("middlegame"),
          ply = 33,
          prevMove = Some("e4e5")
        )
        .getOrElse(fail(s"analysis missing for $MadernaExactFen"))
    val centralCtx = NarrativeContextBuilder.build(data, data.toContext, None)
    val pack = StrategyPackBuilder.build(data, centralCtx).getOrElse(fail("strategy pack missing for central scene"))
    val inputs = QuestionPlannerInputsBuilder.build(centralCtx, Some(pack), truthContract = None)
    val packet =
      inputs.mainBundle
        .flatMap(_.mainClaim)
        .flatMap(_.packet)
        .filter(_.proofFamily == CentralBreakTimingWitness.ProofFamily)
        .getOrElse(fail(s"central packet missing: ${inputs.mainBundle}"))
    (centralCtx, inputs, packet)

  test("records basic source kind when basic move explanation wins") {
    val refs = refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))
    val outline = BookStyleRenderer.validatedOutline(italianCtx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(italianCtx, outline, refs = Some(refs), strategyPack = None, truthContract = None)

    val diagnostics =
      MoveReviewCoverageDiagnostics.build(italianCtx, Some(refs), None, None, slots, plannerInputs(italianCtx))

    assertEquals(diagnostics.moveReviewSourceKind, Some("basic_move_explanation"))
    assertEquals(diagnostics.basicEvidenceStatus, Some("emitted"))
    assertEquals(diagnostics.basicEvidenceRejectReasons, Nil)
    assertEquals(diagnostics.moveReviewLocalFactStatus, Some("emitted"))
    assertEquals(diagnostics.moveReviewLocalFactFamilies, List("opening_goal"))
    assertEquals(diagnostics.moveReviewLocalFactAuthorities, List("opening_goal_evidence"))
    assertEquals(diagnostics.moveReviewLocalFactStrictFallbackEligible, Some(true))

    val scrubbedSlots =
      slots.copy(moveReviewExplanation = slots.moveReviewExplanation.map(_.copy(reasonTags = Nil)))
    val scrubbedDiagnostics =
      MoveReviewCoverageDiagnostics.build(italianCtx, Some(refs), None, None, scrubbedSlots, plannerInputs(italianCtx))
    assertEquals(scrubbedDiagnostics.moveReviewLocalFactStatus, Some("emitted"))
    assertEquals(scrubbedDiagnostics.moveReviewLocalFactFamilies, List("opening_goal"))
    assertEquals(scrubbedDiagnostics.moveReviewLocalFactAuthorities, List("opening_goal_evidence"))
  }

  test("records basic evidence blocker when exact factual fallback has no coupled PV") {
    val outline = BookStyleRenderer.validatedOutline(italianCtx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(italianCtx, outline, refs = None, strategyPack = None, truthContract = None)

    val diagnostics =
      MoveReviewCoverageDiagnostics.build(italianCtx, None, None, None, slots, plannerInputs(italianCtx))

    assertEquals(diagnostics.moveReviewSourceKind, Some("exact_factual_fallback"))
    assertEquals(diagnostics.basicEvidenceStatus, Some("blocked"))
    assert(diagnostics.basicEvidenceRejectReasons.contains("missing_coupled_pv_line"), clue(diagnostics))
  }

  test("records replay failure when refs contain only a one-ply played move") {
    val onePlyRefs = refsForLine(italianBeforeBc4, List("f1c4"), List("Bc4"))
    val outline = BookStyleRenderer.validatedOutline(italianCtx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(italianCtx, outline, refs = Some(onePlyRefs), strategyPack = None, truthContract = None)

    val diagnostics =
      MoveReviewCoverageDiagnostics.build(
        italianCtx,
        refs = Some(onePlyRefs),
        strategyPack = None,
        truthContract = None,
        slots = slots,
        plannerInputs = plannerInputs(italianCtx)
      )

    assert(diagnostics.basicEvidenceRejectReasons.contains("coupled_pv_replay_failed"), clue(diagnostics))
    assert(!diagnostics.basicEvidenceRejectReasons.contains("missing_coupled_pv_line"), clue(diagnostics))
  }

  test("records rendered planner local fact instead of basic preempted candidate") {
    val outline = BookStyleRenderer.validatedOutline(italianCtx)
    val baseSlots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        italianCtx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )
    val slots =
      baseSlots.copy(
        claim = "3. Bc4: The immediate Qd8 threat gives this move a concrete timing window.",
        supportPrimary = None,
        supportSecondary = None,
        tension = None,
        evidenceHook = None,
        coda = None,
        factGuardrails = Nil,
        paragraphPlan = List("p1=claim"),
        sourceKind = MoveReviewPolishSlots.Source.Planner
      )
    val causalTrace =
      Some(
        MoveReviewCompressionPolicy.CausalClaimTrace(
          status = "accepted",
          questionKind = "WhyNow",
          subjectRole = Some("played_move"),
          evidenceKinds = List("timing_witness"),
          evidenceSources = List("timing_witness"),
          evidenceSubjects = List("line_or_reply"),
          evidenceLineBindings = List("replayed"),
          relationKinds = List("timing_constraint"),
          frameIntent = Some("why_now"),
          frameRoles = List("played_move", "line_or_reply"),
          frameSurfaceContract = List("surface_checked_line=false", "surface_forced=false", "surface_alternative=false", "surface_timing=true", "surface_line=replayed"),
          rejectReasons = Nil,
          supportRenderedInClaim = Some(false),
          guardrail = Some("MoveReview causal claim: local_fact=timing/only_move_defense"),
          localFactFamily = Some("timing"),
          localFactAuthority = Some("only_move_defense"),
          localFactProducer = Some("planner_causal_claim"),
          localFactStrictFallbackEligible = Some(true),
          localFactEvidenceRefs = Nil,
          localFactGuardrails = Nil,
          localFactRejectReasons = Nil
        )
      )

    val diagnostics =
      MoveReviewCoverageDiagnostics.build(
        italianCtx,
        refs = None,
        strategyPack = None,
        truthContract = None,
        slots = slots,
        plannerInputs = plannerInputs(italianCtx),
        causalTrace = causalTrace
      )

    assertEquals(diagnostics.basicEvidenceStatus, Some("planner_preempted"))
    assertEquals(diagnostics.moveReviewLocalFactStatus, Some("emitted"))
    assertEquals(diagnostics.moveReviewLocalFactFamilies, List("timing"))
    assertEquals(diagnostics.moveReviewLocalFactAuthorities, List("only_move_defense"))
    assertEquals(diagnostics.moveReviewLocalFactStrictFallbackEligible, Some(true))
  }

  test("SupportedLocal diagnostics separate admitted and rejected proof families") {
    val acceptedFamily = ProofFamilyId.NeutralizeKeyBreak.wireKey
    val acceptedPacket =
      PlayerFacingClaimPacket(
        claimGate = PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
        proofSource = ProofSourceId.CounterplayAxisSuppression.wireKey,
        proofFamily = acceptedFamily,
        scope = PlayerFacingPacketScope.MoveLocal,
        anchorTerms = List("b5"),
        sameBranchState = PlayerFacingSameBranchState.Proven,
        persistence = PlayerFacingClaimPersistence.Stable,
        proofPathWitness = PlayerFacingProofPathWitness(
          ownerSeedTerms = List("b5"),
          continuationTerms = List("a6"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("b5"))
        ),
        fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
      )
    val rejectedPacket =
      acceptedPacket.copy(
        sameBranchState = PlayerFacingSameBranchState.Missing,
        persistence = PlayerFacingClaimPersistence.Broken
      )

    val diagnostic =
      MoveReviewCoverageDiagnostics.supportedLocalFromPackets(
        packets = List(acceptedPacket, rejectedPacket),
        ctx = Some(neutralizeCtx)
      )
    val expectedFailures = ProofContractRules.failureCodes(rejectedPacket)

    assertEquals(diagnostic.candidateFamilies, List(acceptedFamily))
    assertEquals(diagnostic.admittedFamilies, List(acceptedFamily))
    expectedFailures.foreach { code =>
      assert(diagnostic.rejectReasons.contains(s"$acceptedFamily:$code"), clue(diagnostic.rejectReasons))
    }
  }

  test("SupportedLocal diagnostics keep tactical-veto neutralize rows out of admitted families") {
    val family = ProofFamilyId.NeutralizeKeyBreak.wireKey
    val packet =
      PlayerFacingClaimPacket(
        claimGate = PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
        proofSource = ProofSourceId.CounterplayAxisSuppression.wireKey,
        proofFamily = family,
        scope = PlayerFacingPacketScope.MoveLocal,
        anchorTerms = List("d5"),
        bestDefenseBranchKey = Some("e4d5|c6d5"),
        sameBranchState = PlayerFacingSameBranchState.Proven,
        persistence = PlayerFacingClaimPersistence.Stable,
        proofPathWitness = PlayerFacingProofPathWitness(
          ownerSeedTerms = List("neutralize_key_break", "d5"),
          continuationTerms = List("e4d5", "c6d5")
        ),
        fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
      )

    val diagnostic =
      MoveReviewCoverageDiagnostics.supportedLocalFromPackets(
        packets = List(packet),
        tacticalVetoReasons = List("planner_truth_mode_tactical")
      )

    assertEquals(diagnostic.candidateFamilies, List(family))
    assertEquals(diagnostic.admittedFamilies, Nil)
    assert(diagnostic.rejectReasons.contains(s"$family:tactical_veto:planner_truth_mode_tactical"), clue(diagnostic.rejectReasons))
  }

  test("SupportedLocal diagnostics require a named neutralize break token") {
    val family = ProofFamilyId.NeutralizeKeyBreak.wireKey
    val packet =
      PlayerFacingClaimPacket(
        claimGate = PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
        proofSource = ProofSourceId.CounterplayAxisSuppression.wireKey,
        proofFamily = family,
        scope = PlayerFacingPacketScope.MoveLocal,
        anchorTerms = Nil,
        bestDefenseBranchKey = Some("e4d5|c6d5"),
        sameBranchState = PlayerFacingSameBranchState.Proven,
        persistence = PlayerFacingClaimPersistence.Stable,
        proofPathWitness = PlayerFacingProofPathWitness(
          ownerSeedTerms = List("neutralize_key_break", "counterplay_axis_suppression"),
          continuationTerms = List("e4d5", "c6d5")
        ),
        fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
      )

    val diagnostic = MoveReviewCoverageDiagnostics.supportedLocalFromPackets(List(packet))

    assertEquals(diagnostic.candidateFamilies, List(family))
    assertEquals(diagnostic.admittedFamilies, Nil)
    assert(diagnostic.rejectReasons.contains(s"$family:surface:named_break_missing"), clue(diagnostic.rejectReasons))
  }

  test("SupportedLocal diagnostics reject neutralize break tokens that collide with the played move") {
    val family = ProofFamilyId.NeutralizeKeyBreak.wireKey
    val packet =
      PlayerFacingClaimPacket(
        claimGate = PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
        proofSource = ProofSourceId.CounterplayAxisSuppression.wireKey,
        proofFamily = family,
        scope = PlayerFacingPacketScope.MoveLocal,
        anchorTerms = List("g4"),
        bestDefenseBranchKey = Some("c8g4|d2g5"),
        sameBranchState = PlayerFacingSameBranchState.Proven,
        persistence = PlayerFacingClaimPersistence.Stable,
        proofPathWitness = PlayerFacingProofPathWitness(
          ownerSeedTerms = List("neutralize_key_break", "g4"),
          structureTransitionTerms = List("g4"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
        ),
        fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
      )
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "2b1k3/8/8/8/8/8/8/4K3 b - - 0 1",
        playedMove = Some("c8g4"),
        playedSan = Some("Bg4")
      )

    val diagnostic =
      MoveReviewCoverageDiagnostics.supportedLocalFromPackets(
        packets = List(packet),
        ctx = Some(ctx)
      )

    assertEquals(diagnostic.candidateFamilies, List(family))
    assertEquals(diagnostic.admittedFamilies, Nil)
    assert(diagnostic.rejectReasons.contains(s"$family:surface:played_move_collision"), clue(diagnostic.rejectReasons))
  }

  test("SupportedLocal diagnostics admit central_break_timing only with exact surface witness context") {
    val (centralCtx, _, packet) = centralScene
    val family = CentralBreakTimingWitness.ProofFamily

    val admitted =
      MoveReviewCoverageDiagnostics.supportedLocalFromPackets(
        packets = List(packet),
        ctx = Some(centralCtx)
      )
    val missingContext =
      MoveReviewCoverageDiagnostics.supportedLocalFromPackets(
        packets = List(packet),
        ctx = None
      )

    assertEquals(admitted.candidateFamilies, List(family))
    assertEquals(admitted.admittedFamilies, List(family))
    assertEquals(missingContext.candidateFamilies, List(family))
    assertEquals(missingContext.admittedFamilies, Nil)
    assert(
      missingContext.rejectReasons.contains(s"$family:${CentralBreakTimingSurfaceGate.MissingExactWitness}"),
      clue(missingContext.rejectReasons)
    )
  }
