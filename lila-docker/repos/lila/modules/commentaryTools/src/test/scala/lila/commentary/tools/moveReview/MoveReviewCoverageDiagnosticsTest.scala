package lila.commentary.tools.moveReview

import lila.commentary.*
import lila.commentary.analysis.*
import lila.commentary.analysis.claim.ProofContractRules
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.*
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
          continuationTerms = List("a6")
        ),
        fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
      )
    val rejectedPacket =
      acceptedPacket.copy(
        sameBranchState = PlayerFacingSameBranchState.Missing,
        persistence = PlayerFacingClaimPersistence.Broken
      )

    val diagnostic = MoveReviewCoverageDiagnostics.supportedLocalFromPackets(List(acceptedPacket, rejectedPacket))
    val expectedFailures = ProofContractRules.failureCodes(rejectedPacket)

    assertEquals(diagnostic.candidateFamilies, List(acceptedFamily))
    assertEquals(diagnostic.admittedFamilies, List(acceptedFamily))
    expectedFailures.foreach { code =>
      assert(diagnostic.rejectReasons.contains(s"$acceptedFamily:$code"), clue(diagnostic.rejectReasons))
    }
  }
