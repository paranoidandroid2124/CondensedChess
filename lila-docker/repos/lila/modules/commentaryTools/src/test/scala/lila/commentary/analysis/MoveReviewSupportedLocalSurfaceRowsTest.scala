package lila.commentary.analysis

import lila.commentary.MoveReviewSurfaceAuthority
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.NarrativeContext
import lila.commentary.model.authoring.AuthorQuestionKind
import lila.commentary.model.strategic.VariationLine
import munit.FunSuite

final class MoveReviewSupportedLocalSurfaceRowsTest extends FunSuite:

  private val ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx
  private val neutralizeCtx =
    ctx.copy(
      fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
      playedMove = Some("e2e3"),
      playedSan = Some("e3")
    )
  private val collisionCtx =
    ctx.copy(
      fen = "2b1k3/8/8/8/8/8/8/4K3 b - - 0 1",
      playedMove = Some("c8g4"),
      playedSan = Some("Bg4")
    )
  private val sanOnlyCollisionCtx =
    collisionCtx.copy(playedMove = None)
  private val playedDestinationRouteCtx =
    ctx.copy(
      playedMove = Some("e7e5"),
      playedSan = Some("e5")
    )
  private val MadernaExactFen =
    "nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17"
  private val MadernaExactLines =
    List(
      VariationLine(List("e4e5", "d6e5", "f4e5", "d7e5", "c4e5"), scoreCp = 82, depth = 18),
      VariationLine(List("c1e3", "b7b5", "a5b6"), scoreCp = 36, depth = 18)
    )
  private val DirectBreakFen =
    "rnbqk2r/pp2bppp/4pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1 w kq - 0 8"

  private def supportedNeutralizePacket(
      proofFamily: String = ProofFamilyId.NeutralizeKeyBreak.wireKey,
      proofSource: String = ProofSourceId.CounterplayAxisSuppression.wireKey,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      scope: PlayerFacingPacketScope = PlayerFacingPacketScope.MoveLocal,
      anchorTerms: List[String] = List("...c5"),
      ownerSeedTerms: List[String] = List("neutralize_key_break", "...c5"),
      structureTransitionTerms: List[String] = List("...c5"),
      bestDefenseMove: Option[String] = Some("c5"),
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...c5"))
  ): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = scope,
      triggerKind = PlanTaxonomy.PlanKind.BreakPrevention.id,
      anchorTerms = anchorTerms,
      bestDefenseMove = bestDefenseMove,
      bestDefenseBranchKey = Some("e4d5|c6d5"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = ownerSeedTerms,
          continuationTerms = List("counterplay_axis_suppression", "e4d5", "c6d5"),
          structureTransitionTerms = structureTransitionTerms,
          exactSliceProof = exactSliceProof
        ),
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def mainClaim(
      packet: PlayerFacingClaimPacket,
      claimText: String
  ): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.MoveLocal,
      mode = PlayerFacingTruthMode.Strategic,
      deltaClass = Some(PlayerFacingMoveDeltaClass.CounterplayReduction),
      claimText = claimText,
      anchorTerms = List("...c5"),
      evidenceLines = List("14.e4 c5 15.d5"),
      sourceKind = "prevented_plan",
      tacticalOwnership = None,
      packet = Some(packet)
    )

  private def inputs(
      packet: PlayerFacingClaimPacket = supportedNeutralizePacket(),
      truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic,
      claimText: String = "This move stops the ...c5 break before Black gets counterplay."
  ): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle = Some(MainPathClaimBundle(Some(mainClaim(packet, claimText)), None)),
      quietIntent = None,
      decisionFrame = CertifiedDecisionFrame(),
      decisionComparison = None,
      alternativeNarrative = None,
      truthMode = truthMode,
      preventedPlansNow = Nil,
      pvDelta = None,
      counterfactual = None,
      practicalAssessment = None,
      opponentThreats = Nil,
      forcingThreats = Nil,
      evidenceByQuestionId = Map.empty,
      candidateEvidenceLines = Nil,
      evidenceBackedPlans = Nil,
      opponentPlan = None,
      factualFallback = None
    )

  private def safeTruthContract(playedMove: String = "c3g3"): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some(playedMove),
      verifiedBestMove = Some(playedMove),
      truthClass = DecisiveTruthClass.Best,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.SupportingVisible,
      surfaceMode = TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  private def neutralizePlan(
      claim: String = "This move stops the ...c5 break before Black gets counterplay.",
      proofFamily: String = ProofFamilyId.NeutralizeKeyBreak.wireKey,
      plannerSource: String = "prevented_plan",
      namedBreak: Option[String] = Some("...c5")
  ): QuestionPlan =
    QuestionPlan(
      questionId = "q_now_counterplay_break",
      questionKind = AuthorQuestionKind.WhyNow,
      priority = 100,
      claim = claim,
      evidence = None,
      contrast = None,
      consequence = None,
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = List(plannerSource),
      admissibilityReasons = Nil,
      plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
      plannerSource = plannerSource,
      timingWitness =
        Some(
          QuestionPlanTimingWitness(
            proofFamily = proofFamily,
            source = plannerSource,
            namedBreak = namedBreak,
            continuationMove = Some("e4d5"),
            branchKey = Some("e4d5|c6d5"),
            witnessTokens = List("counterplay_axis_suppression")
          )
        )
    )

  private def ranked(plan: QuestionPlan): RankedQuestionPlans =
    RankedQuestionPlans(primary = Some(plan), secondary = None, rejected = Nil)

  private def centralScene(
      fen: String = MadernaExactFen,
      ply: Int = 33,
      playedMove: String = "e4e5",
      lines: List[VariationLine] = MadernaExactLines,
      truthContract: Option[DecisiveTruthContract] = None
  ): (NarrativeContext, QuestionPlannerInputs, RankedQuestionPlans) =
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fen,
          variations = lines,
          playedMove = Some(playedMove),
          phase = Some("middlegame"),
          ply = ply,
          prevMove = Some(playedMove)
        )
        .getOrElse(fail(s"analysis missing for $fen"))
    val centralCtx = NarrativeContextBuilder.build(data, data.toContext, None)
    val pack = StrategyPackBuilder.build(data, centralCtx).getOrElse(fail(s"strategy pack missing for $fen"))
    val effectiveTruthContract = truthContract.orElse(Some(safeTruthContract(playedMove)))
    val centralInputs = QuestionPlannerInputsBuilder.build(centralCtx, Some(pack), effectiveTruthContract)
    val centralRanked = QuestionFirstCommentaryPlanner.plan(centralCtx, centralInputs, effectiveTruthContract)
    (centralCtx, centralInputs, centralRanked)

  test("projects a SupportedLocal neutralize_key_break timing plan into a named counterplay break row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = inputs(),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this stops the ...c5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5")))
    )
    assertEquals(rows.head.source, None)
  }

  test("projects exact central_break_timing packet into a central break row") {
    val (centralCtx, centralInputs, centralRanked) = centralScene()

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the e4-e5 break at this moment."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralBreak, token = Some("e4-e5")))
    )
    assertEquals(rows.head.source, None)
    assert(!rows.exists(_.text.contains("central_break_timing")), clue(rows))
  }

  test("suppresses central_break_timing row when tactical truth mode vetoes SupportedLocal") {
    val truth = tacticalTruthContract(playedMove = "e4e5")
    val (centralCtx, centralInputs, centralRanked) = centralScene(truthContract = Some(truth))

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(truth)
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
  }

  test("does not project plan-only central_break_timing review evidence") {
    val lines =
      List(
        VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 72, depth = 18),
        VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 28, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "b1c3", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
  }

  test("projects central_break_timing when the board-backed break is not best-line-exclusive") {
    val lines =
      List(
        VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 68, depth = 18),
        VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 31, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the d4-d5 break at this moment."
    )
  }

  test("projects central_break_timing without requiring a two-move branch key") {
    val lines =
      List(
        VariationLine(List("d4d5"), scoreCp = 0, depth = 18),
        VariationLine(List("b1c3"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the d4-d5 break at this moment."
    )
  }

  test("projects played direct central_break_timing when top PV omits that break") {
    val lines =
      List(
        VariationLine(List("b1c3", "e8g8", "h2h3"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the d4-d5 break at this moment."
    )
  }

  test("projects diagonal central captures as central liquidation rows, not central break") {
    val diagonalCaptureFen =
      "r2q1rk1/pp2npb1/2p1b1pp/3pB3/3PN3/1BP2N2/PP3PPP/R2Q1RK1 b - - 0 15"
    val lines =
      List(
        VariationLine(List("d5e4"), scoreCp = 0, depth = 18),
        VariationLine(List("g7e5"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = diagonalCaptureFen, ply = 30, playedMove = "d5e4", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
    assertEquals(rows.map(_.label), List("Central liquidation"))
    assertEquals(rows.head.text, "The move releases central tension through d5-e4.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralLiquidation, token = Some("...d5-e4")))
    )
  }

  test("projects prep or challenge pawn moves as central challenge rows, not central break") {
    val prepFen =
      "rnbqkbnr/pp1ppppp/8/2p1P3/2B5/5Q2/PPPP1PPP/RNB1K1NR b KQkq - 2 4"
    val lines =
      List(
        VariationLine(List("d7d6"), scoreCp = 0, depth = 18),
        VariationLine(List("b8c6"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = prepFen, ply = 8, playedMove = "d7d6", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
    assertEquals(rows.map(_.label), List("Central challenge"))
    assertEquals(rows.head.text, "The move challenges the center through d7-d6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralChallenge, token = Some("...d7-d6")))
    )
  }

  test("suppresses practical central rows when tactical truth mode vetoes support") {
    val diagonalCaptureFen =
      "r2q1rk1/pp2npb1/2p1b1pp/3pB3/3PN3/1BP2N2/PP3PPP/R2Q1RK1 b - - 0 15"
    val lines =
      List(
        VariationLine(List("d5e4"), scoreCp = 0, depth = 18),
        VariationLine(List("g7e5"), scoreCp = 0, depth = 18)
      )
    val truth = tacticalTruthContract(playedMove = "d5e4")
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = diagonalCaptureFen, ply = 30, playedMove = "d5e4", lines = lines, truthContract = Some(truth))

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(truth)
      )

    assertEquals(rows, Nil)
  }

  test("suppresses neutralize_key_break row when tactical truth mode vetoes SupportedLocal") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects an admitted neutralize_key_break packet claim when no timing plan is selected") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = inputs(),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this stops the ...c5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5")))
    )
    assertEquals(rows.head.source, None)
  }

  test("does not project tokenless neutralize_key_break packets") {
    val tokenless =
      supportedNeutralizePacket(
        anchorTerms = Nil,
        ownerSeedTerms = List("neutralize_key_break", "counterplay_axis_suppression"),
        structureTransitionTerms = Nil,
        bestDefenseMove = None,
        exactSliceProof = None
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = tokenless),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not recover fake break tokens from packet terms without typed proof") {
    val termOnly =
      supportedNeutralizePacket(
        anchorTerms = List("...c5"),
        ownerSeedTerms = List("neutralize_key_break", "...c5"),
        structureTransitionTerms = List("...c5"),
        exactSliceProof = None
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = termOnly),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project single-square break tokens that collide with the played move") {
    val collisionPacket =
      supportedNeutralizePacket(
        anchorTerms = List("g4"),
        ownerSeedTerms = List("neutralize_key_break", "g4"),
        structureTransitionTerms = List("g4"),
        bestDefenseMove = Some("g4"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = collisionCtx,
        inputs = inputs(packet = collisionPacket),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not use SAN-only played move text as collision authority") {
    val collisionPacket =
      supportedNeutralizePacket(
        anchorTerms = List("g4"),
        ownerSeedTerms = List("neutralize_key_break", "g4"),
        structureTransitionTerms = List("g4"),
        bestDefenseMove = Some("g4"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = sanOnlyCollisionCtx,
        inputs = inputs(packet = collisionPacket),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not replace a played-move collision with a later anchor square") {
    val collisionPacket =
      supportedNeutralizePacket(
        anchorTerms = List("c6"),
        ownerSeedTerms = List("neutralize_key_break", "g4", "c6"),
        structureTransitionTerms = List("g4", "c6"),
        bestDefenseMove = Some("g4"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = collisionCtx,
        inputs = inputs(packet = collisionPacket),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects route-shaped break tokens even when the played move occupies the destination square") {
    val routePacket =
      supportedNeutralizePacket(
        anchorTerms = List("e4-e5"),
        ownerSeedTerms = List("neutralize_key_break", "e4-e5"),
        structureTransitionTerms = List("e4-e5"),
        bestDefenseMove = Some("e4e5"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("e4-e5"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = playedDestinationRouteCtx,
        inputs = inputs(packet = routePacket),
        rankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this stops the e4-e5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("e4-e5")))
    )
  }

  test("does not project plan plus packet neutralize rows when typed proof token differs from plan witness") {
    val mismatched =
      supportedNeutralizePacket(
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...e5"))
      )

    val decision =
      NeutralizeKeyBreakSurfaceGate.decideForPlanPacket(
        plan = neutralizePlan(namedBreak = Some("...c5")),
        packet = mismatched,
        ctx = ctx
      )

    assertEquals(decision.token, None)
    assertEquals(decision.rejectReason, Some(NeutralizeKeyBreakSurfaceGate.MissingNamedBreak))
  }

  test("ignores source mismatch release risk and other proof families") {
    val sourceMismatch =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedNeutralizePacket(proofSource = "wrong_source")),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val releaseRisk =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedNeutralizePacket(releaseRisks = List(PlayerFacingClaimReleaseRisk.RivalRelease))),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val otherFamily =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedNeutralizePacket(proofFamily = ProofFamilyId.CounterplayRestraint.wireKey)),
        rankedPlans = ranked(neutralizePlan(proofFamily = ProofFamilyId.CounterplayRestraint.wireKey)),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(sourceMismatch, Nil)
    assertEquals(releaseRisk, Nil)
    assertEquals(otherFamily, Nil)
  }

  test("released text comes from the named witness token rather than raw claim prose") {
    val raw =
      "The strategic point is that neutralize_key_break uses counterplay_axis_suppression on e4d5|c6d5."
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = inputs(claimText = raw),
        rankedPlans = ranked(neutralizePlan(claim = raw)),
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(
      rows.headOption.map(_.text),
      Some("On the checked line, this stops the ...c5 break before it appears.")
    )
    assert(!rows.exists(_.text.contains("neutralize_key_break")), clue(rows))
    assert(!rows.exists(_.text.contains("counterplay_axis_suppression")), clue(rows))
    assert(!rows.exists(_.text.contains("e4d5|c6d5")), clue(rows))
  }

  private def tacticalTruthContract(playedMove: String): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some(playedMove),
      verifiedBestMove = Some(playedMove),
      truthClass = DecisiveTruthClass.Blunder,
      cpLoss = 280,
      swingSeverity = 280,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
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
