package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.{ DirectionalTargetReadiness, GameChronicleMoment, NarrativeSignalDigest, RouteSurfaceMode, StrategicIdeaGroup, StrategicIdeaKind, StrategicIdeaReadiness, StrategyDirectionalTarget, StrategyIdeaSignal, StrategyPack, StrategyPieceMoveRef, StrategyPieceRoute }
import lila.commentary.model.*
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind, PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.{ EngineEvidence, PvMove, VariationLine }
import lila.commentary.analysis.claim.PlayerFacingClaimPrefixKind

class PlayerFacingTruthModePolicyTest extends FunSuite:

  private def baseCtx(): NarrativeContext =
    NarrativeContext(
      fen = "r2q1rk1/pp2bppp/2np1n2/2p1p3/2P1P3/2NP1NP1/PP2QPBP/R1B2RK1 w - - 0 10",
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 20,
      playedMove = Some("e2e3"),
      playedSan = Some("Qe2"),
      summary = NarrativeSummary("Central restraint", None, "StyleChoice", "Maintain", "0.20"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Normal middlegame"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def truthContract(
      truthClass: DecisiveTruthClass,
      reasonFamily: DecisiveReasonKind
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("d1d5"),
      verifiedBestMove = Some("d1d5"),
      truthClass = truthClass,
      cpLoss = if truthClass == DecisiveTruthClass.Blunder then 280 else 0,
      swingSeverity = if truthClass == DecisiveTruthClass.Blunder then 280 else 0,
      reasonFamily = reasonFamily,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode =
        if truthClass == DecisiveTruthClass.Blunder then TruthSurfaceMode.FailureExplain
        else TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode =
        if truthClass == DecisiveTruthClass.Blunder then FailureInterpretationMode.TacticalRefutation
        else FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  private def evidenceBackedPlan(
      planId: String,
      planName: String,
      subplanId: String,
      executionSteps: List[String],
      themeL1: String = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id
  ): PlanHypothesis =
    PlanHypothesis(
      planId = planId,
      planName = planName,
      rank = 1,
      score = 0.82,
      preconditions = Nil,
      executionSteps = executionSteps,
      failureModes = Nil,
      viability = PlanViability(score = 0.8, label = "high", risk = "test"),
      evidenceSources = List(s"theme:$themeL1"),
      themeL1 = themeL1,
      subplanId = Some(subplanId)
    )

  private def evidenceBackedExperiment(
      planId: String,
      subplanId: String,
      themeL1: String = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id
  ): StrategicPlanExperiment =
    StrategicPlanExperiment(
      planId = planId,
      themeL1 = themeL1,
      subplanId = Some(subplanId),
      evidenceTier = "evidence_backed",
      supportProbeCount = 1,
      refuteProbeCount = 0,
      bestReplyStable = true,
      futureSnapshotAligned = true,
      counterBreakNeutralized = true,
      moveOrderSensitive = false,
      experimentConfidence = 0.86
    )

  private def colorComplexCtx(fen: String): NarrativeContext =
    baseCtx().copy(
      fen = fen,
      engineEvidence = Some(
        EngineEvidence(
          depth = 16,
          variations = List(VariationLine(List("c4e5", "e8f8", "e5g6", "f8g8"), 20, depth = 16))
        )
      ),
      semantic = Some(
        SemanticSection(
          structuralWeaknesses =
            List(
              WeakComplexInfo(
                owner = "Black",
                squareColor = "light",
                squares = List("e5"),
                isOutpost = false,
                cause = "light-square holes after the fianchetto bishop disappeared"
              )
            ),
          pieceActivity =
            List(
              PieceActivityInfo(
                piece = "Knight",
                square = "c4",
                mobilityScore = 0.8,
                isTrapped = false,
                isBadBishop = false,
                keyRoutes = List("e5"),
                coordinationLinks = Nil,
                directionalTargets = List("e5")
              )
            ),
          positionalFeatures =
            List(PositionalTagInfo("ColorComplexWeakness", None, None, "Black", Some("light squares: e5"))),
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans =
            List(
              PreventedPlanInfo(
                planId = "color_complex_squeeze",
                deniedSquares = List("e5"),
                breakNeutralized = None,
                mobilityDelta = -1,
                counterplayScoreDrop = 90,
                preventedThreatType = Some("counterplay"),
                sourceScope = FactScope.Now
              )
            ),
          conceptSummary = List("color complex pressure")
        )
      )
    )

  private def colorComplexPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      pieceMoveRefs =
        List(
          StrategyPieceMoveRef(
            ownerSide = "white",
            piece = "Knight",
            from = "c4",
            target = "e5",
            idea = "color-complex pressure",
            evidence = List("color_complex_weakness")
          )
        ),
      strategicIdeas =
        List(
          StrategyIdeaSignal(
            ideaId = "color_complex_squeeze",
            ownerSide = "white",
            kind = "color_complex_squeeze",
            group = StrategicIdeaGroup.StructuralChange,
            readiness = StrategicIdeaReadiness.Ready,
            focusSquares = List("e5"),
            confidence = 0.92,
            evidenceRefs = List("color_complex_weakness")
          )
        ),
      longTermFocus = List("pressure on e5"),
      evidence = List("color_complex_weakness:e5")
    )

  extension (ctx: NarrativeContext)
    private def withTypedEvidenceFromLegacy: NarrativeContext =
      val evaluated =
        ctx.mainStrategicPlans.map { plan =>
          val experiment =
            ctx.strategicPlanExperiments
              .find(exp =>
                exp.planId == plan.planId &&
                  exp.subplanId == plan.subplanId
              )
              .orElse(ctx.strategicPlanExperiments.find(_.planId == plan.planId))
          val quantifier =
            experiment match
              case Some(exp) if exp.bestReplyStable && exp.futureSnapshotAligned && !exp.moveOrderSensitive =>
                PlayerFacingClaimQuantifier.Universal
              case Some(exp) if exp.bestReplyStable || exp.futureSnapshotAligned =>
                PlayerFacingClaimQuantifier.BestResponse
              case _ =>
                PlayerFacingClaimQuantifier.Existential
          val stability =
            experiment match
              case Some(exp) if (exp.bestReplyStable || exp.futureSnapshotAligned) && !exp.moveOrderSensitive =>
                PlayerFacingClaimStabilityGrade.Stable
              case Some(_) =>
                PlayerFacingClaimStabilityGrade.Unstable
              case None =>
                PlayerFacingClaimStabilityGrade.Unknown
          PlanEvidenceEvaluator.EvaluatedPlan(
            hypothesis = plan,
            status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
            userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
            reason = "test typed evidence",
            supportProbeIds =
              experiment
                .map(exp => List.fill(math.max(1, exp.supportProbeCount))("legacy_support_probe"))
                .getOrElse(List("legacy_support_probe")),
            refuteProbeIds =
              experiment
                .map(exp => List.fill(exp.refuteProbeCount)("legacy_refute_probe"))
                .getOrElse(Nil),
            themeL1 = plan.themeL1,
            subplanId = plan.subplanId,
            claimCertification =
              PlanEvidenceEvaluator.ClaimCertification(
                certificateStatus =
                  if stability == PlayerFacingClaimStabilityGrade.Stable then PlayerFacingCertificateStatus.Valid
                  else PlayerFacingCertificateStatus.WeaklyValid,
                quantifier = quantifier,
                modalityTier = PlayerFacingClaimModalityTier.Supports,
                attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
                stabilityGrade = stability,
                provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
                ontologyFamily = PlayerFacingClaimOntologyKind.PlanAdvance,
                alternativeDominance = false
              )
          )
        }
      ctx.copy(
        strategicPlanEvidence =
          PlanEvidenceEvaluator.StrategicPlanEvidenceView(
            selectedPlans = evaluated,
            evaluatedPlans = evaluated
          )
      )

  test("color-complex exact witness releases only when a minor piece attacks the semantic weak square") {
    val ctx = colorComplexCtx("4k3/8/8/8/2N5/8/8/4K3 w - - 0 1")
    val surface = StrategyPackSurface.from(Some(colorComplexPack))
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(ctx, surface, truthContract = None)
        .getOrElse(fail("missing color-complex delta"))

    assertEquals(delta.packet.proofSource, PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource)
    assertEquals(delta.packet.proofFamily, PlayerFacingTruthModePolicy.ColorComplexSqueezeProofFamily)
    assertEquals(delta.packet.scope, PlayerFacingPacketScope.PositionLocal)
    assert(delta.packet.proofPathWitness.ownerSeedTerms.contains("weak_square:e5"), clues(delta.packet))
    assert(delta.packet.proofPathWitness.ownerSeedTerms.contains("minor_piece_attack:c4-e5"), clues(delta.packet))
    assert(
      delta.packet.proofPathWitness.exactSliceProof.exists {
        case PlayerFacingExactSliceProof.ColorComplexSqueeze("e5", "dark", "knight", "c4") => true
        case _                                                                             => false
      },
      clues(delta.packet.proofPathWitness.exactSliceProof)
    )
    assert(PlayerFacingTruthModePolicy.certifiedPositionProbePacket(delta.packet), clues(delta.packet))
    assert(
      !PlayerFacingTruthModePolicy.certifiedPositionProbePacket(
        delta.packet.copy(proofPathWitness = delta.packet.proofPathWitness.copy(exactSliceProof = None))
      )
    )
  }

  test("color-complex text and semantic square do not release when the minor piece does not attack it") {
    val ctx = colorComplexCtx("4k3/8/8/8/8/2N5/8/4K3 w - - 0 1")
    val surface = StrategyPackSurface.from(Some(colorComplexPack))
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(ctx, surface, truthContract = None)
        .getOrElse(fail("missing generic counterplay delta"))

    assertNotEquals(delta.packet.proofSource, PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource)
    assert(!PlayerFacingTruthModePolicy.certifiedPositionProbePacket(delta.packet), clues(delta.packet))
  }

  test("experiment confidence alone does not create color-complex authority") {
    val ctx =
      colorComplexCtx("4k3/8/8/8/8/2N5/8/4K3 w - - 0 1").copy(
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "color_complex_plan",
              subplanId = PlanTaxonomy.PlanKind.OppositeBishopsConversion.id
            ).copy(experimentConfidence = 0.90)
          )
      )
    val surface = StrategyPackSurface.from(Some(colorComplexPack))
    val delta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(ctx, surface, truthContract = None)
        .getOrElse(fail("missing generic counterplay delta"))

    assertNotEquals(delta.packet.proofSource, PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource)
    assert(!PlayerFacingTruthModePolicy.certifiedPositionProbePacket(delta.packet), clues(delta.packet))
  }

  private val defaultQuestions =
    List(
      AuthorQuestion("why_this", AuthorQuestionKind.WhyThis, 100, "Why this move?"),
      AuthorQuestion("what_matters_here", AuthorQuestionKind.WhatMattersHere, 90, "What matters here?"),
      AuthorQuestion("what_changed", AuthorQuestionKind.WhatChanged, 80, "What changed?"),
      AuthorQuestion("why_now", AuthorQuestionKind.WhyNow, 60, "Why now?")
    )

  private def exactReviewScene(id: String) =
    val fixture =
      TaskShiftProvingFixtures.reviewFixtures
        .find(_.id == id)
        .getOrElse(fail(s"missing review fixture: $id"))
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fixture.fen,
          variations = List(VariationLine(fixture.pvMoves, fixture.scoreCp, depth = 16)),
          phase = Some(fixture.phase),
          ply = fixture.ply
        )
        .getOrElse(fail(s"analysis missing for ${fixture.id}"))
    val ctx =
      NarrativeContextBuilder
        .build(data, data.toContext, None)
        .copy(authorQuestions = defaultQuestions)
    val pack =
      StrategyPackBuilder
        .build(data, ctx)
        .getOrElse(fail(s"strategy pack missing for ${fixture.id}"))
    val reviewTruthContract = Some(truthContract(DecisiveTruthClass.Acceptable, DecisiveReasonKind.QuietTechnicalMove))
    val inputs =
      QuestionPlannerInputsBuilder.build(ctx, Some(pack), truthContract = reviewTruthContract)
    val ranked =
      QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = reviewTruthContract)
    (fixture, ctx, pack, inputs, ranked)

  private def exactReviewSnapshot(id: String) =
    val (fixture, _, pack, inputs, ranked) = exactReviewScene(id)
    (fixture, pack, inputs, ranked)

  private def standaloneEntrySquareDenialSnapshot() =
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        ply = 46,
        playedMove = Some("a2a3"),
        playedSan = Some("a3"),
        authorQuestions = defaultQuestions,
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "entry_route_denial",
              planName = "Take away the b4 entry square",
              subplanId = PlanTaxonomy.PlanKind.KeySquareDenial.id,
              executionSteps = List("Keep Black out of b4 on the defended branch.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "entry_route_denial",
              subplanId = PlanTaxonomy.PlanKind.KeySquareDenial.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_entry",
                deniedSquares = List("b4"),
                breakNeutralized = None,
                mobilityDelta = -2,
                counterplayScoreDrop = 138,
                preventedThreatType = Some("counterplay"),
                citationLine = Some("Black can no longer use b4 as an entry square."),
                deniedResourceClass = Some("entry_square"),
                deniedEntryScope = Some("single_square")
              )
            ),
            conceptSummary = Nil
          )
        ),
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.NarrowChoice,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Attack", None, "independent"),
            whyNot = Some("Black can no longer use b4 as an entry square.")
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations =
              List(
                VariationLine(
                  moves = List("a2a3", "b7b5", "a3a4"),
                  scoreCp = 82,
                  depth = 18
                )
              )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets =
            List(
              StrategyDirectionalTarget(
                targetId = "target_b4",
                ownerSide = "white",
                piece = "P",
                from = "a2",
                targetSquare = "b4",
                readiness = DirectionalTargetReadiness.Build,
                strategicReasons = List("keep Black out of b4"),
                evidence = List("probe")
              )
            ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("keep Black out of b4")))
        )
      )
    val inputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    (ctx, pack, inputs, ranked)

  test("quiet shell-only support resolves to Minimal") {
    val ctx = baseCtx().copy(strategicSalience = lila.commentary.model.strategic.StrategicSalience.Low)
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          strategicIdeas = List(
            lila.commentary.StrategyIdeaSignal(
              ideaId = "shell_only",
              ownerSide = "white",
              kind = StrategicIdeaKind.TargetFixing,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Build,
              confidence = 0.62
            )
          ),
          longTermFocus = List("Carlsbad pressure")
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, pack, None),
      PlayerFacingTruthMode.Minimal
    )
  }

  test("forcing truth contracts resolve to Tactical") {
    val ctx = baseCtx()
    assertEquals(
      PlayerFacingTruthModePolicy.classify(
        ctx,
        None,
        Some(truthContract(DecisiveTruthClass.Blunder, DecisiveReasonKind.TacticalRefutation))
      ),
      PlayerFacingTruthMode.Tactical
    )
  }

  test("criticality and cp swing alone do not resolve to Tactical") {
    val ctx =
      baseCtx().copy(
        header = ContextHeader("Middlegame", "Critical", "OnlyMove", "High", "ExplainTactics"),
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.OnlyMove,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Attack", None, "independent"),
            errorClass = Some(
              ErrorClassification(
                isTactical = true,
                missedMotifs = List("fork"),
                errorSummary = "generic tactical pressure"
              )
            )
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, None, None),
      PlayerFacingTruthMode.Minimal
    )

    val moment =
      GameChronicleMoment(
        momentId = "ply_22_advantageswing",
        ply = 22,
        moveNumber = 11,
        side = "white",
        moveClassification = Some("mistake"),
        momentType = "AdvantageSwing",
        fen = baseCtx().fen,
        narrative = "The move keeps some pressure.",
        concepts = Nil,
        variations = Nil,
        cpBefore = -20,
        cpAfter = 220,
        mateBefore = None,
        mateAfter = None,
        wpaSwing = None,
        strategicSalience = Some("High"),
        transitionType = None,
        transitionConfidence = None,
        activePlan = None,
        topEngineMove = None,
        collapse = None,
        strategyPack = None,
        signalDigest = None
      )

    assertEquals(PlayerFacingMoveDeltaBuilder.classify(moment), PlayerFacingTruthMode.Minimal)
  }

  test("concrete route and target evidence resolves to Strategic") {
    val ctx =
      baseCtx().copy(
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("e5")),
            logicSummary = "Pressure against e5 is the concrete point.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("e5 pressure"),
              planAdvancements = List("queen and rook coordination"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              route = List("d1", "f3", "f5"),
              purpose = "kingside pressure",
              strategicFit = 0.86,
              tacticalSafety = 0.8,
              surfaceConfidence = 0.85,
              surfaceMode = RouteSurfaceMode.Exact,
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_e5",
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              targetSquare = "e5",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("pressure on e5"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("pressure on e5")))
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, pack, None),
      PlayerFacingTruthMode.Strategic
    )
  }

  test("generic plan-advance delta claim stays closed without an explicit owner path") {
    val ctx =
      baseCtx().copy(
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "central_space_bind",
              planName = "Prepare the central break",
              subplanId = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
              executionSteps = List("Support the e4 break."),
              themeL1 = PlanTaxonomy.PlanTheme.PawnBreakPreparation.id
            )
          ),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "central_space_bind",
            themeL1 = "pawn_break_preparation",
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = false,
            moveOrderSensitive = false,
            experimentConfidence = 0.86
          )
        ),
        decision = Some(
          DecisionRationale(
            focalPoint = None,
            logicSummary = "Keeps the center stable -> improves coordination",
            delta = PVDelta(
              resolvedThreats = List("central tension"),
              newOpportunities = List("e4 push"),
              planAdvancements = List("queen and rook coordination"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              route = List("d1", "e2", "e4"),
              purpose = "support the e4 break",
              strategicFit = 0.84,
              tacticalSafety = 0.78,
              surfaceConfidence = 0.88,
              surfaceMode = RouteSurfaceMode.Exact,
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_e4",
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              targetSquare = "e4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("support the e4 break"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("support the e4 break")))
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, pack, None),
      PlayerFacingTruthMode.Strategic
    )
    assert(
      !PlayerFacingTruthModePolicy.allowsStrategicClaimText(
        "Qe2 keeps the e4 push available while covering the c4 pawn.",
        ctx,
        pack,
        None
      )
    )
  }

  test("draw results disable whole-game decisive narration") {
    assert(!PlayerFacingTruthModePolicy.allowWholeGameDecisiveNarrative("1/2-1/2"))
    assert(PlayerFacingTruthModePolicy.allowWholeGameDecisiveNarrative("1-0"))
  }

  test("quiet chronicle moments suppress active notes") {
    val moment =
      GameChronicleMoment(
        momentId = "ply_20_quiet",
        ply = 20,
        moveNumber = 10,
        side = "white",
        moveClassification = None,
        momentType = "StrategicBridge",
        fen = baseCtx().fen,
        narrative = "A quiet move.",
        concepts = Nil,
        variations = List(VariationLine(List("Qe2", "...Re8"), 20, None, 18)),
        cpBefore = 20,
        cpAfter = 20,
        mateBefore = None,
        mateAfter = None,
        wpaSwing = None,
        strategicSalience = Some("Low"),
        transitionType = None,
        transitionConfidence = None,
        activePlan = None,
        topEngineMove = None,
        collapse = None,
        strategyPack = None,
        signalDigest = None
      )

    assertEquals(PlayerFacingMoveDeltaBuilder.classify(moment), PlayerFacingTruthMode.Minimal)
  }

  test("tactical sacrifice with immediate recoup resolves to Tactical") {
    val ctx =
      baseCtx().copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = Some(
              CompensationInfo(
                investedMaterial = 300,
                returnVector = Map("attack" -> 0.9),
                expiryPly = Some(4),
                conversionPlan = "recover material by force"
              )
            ),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("b5d7", "e8d7", "d1g4"),
                scoreCp = 75,
                depth = 18,
                parsedMoves = List(
                  PvMove("b5d7", "Bxd7+", "b5", "d7", "B", isCapture = true, capturedPiece = Some("n"), givesCheck = true),
                  PvMove("e8d7", "Kxd7", "e8", "d7", "K", isCapture = true, capturedPiece = Some("b"), givesCheck = false),
                  PvMove("d1g4", "Qg4+", "d1", "g4", "Q", isCapture = false, capturedPiece = None, givesCheck = true)
                )
              )
            )
          )
        )
      )
    val contract =
      truthContract(DecisiveTruthClass.CompensatedInvestment, DecisiveReasonKind.InvestmentSacrifice).copy(
        surfaceMode = TruthSurfaceMode.InvestmentExplain,
        compensationProseAllowed = true
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, None, Some(contract)),
      PlayerFacingTruthMode.Tactical
    )
  }

  test("strategic sacrifice without immediate recoup resolves to Strategic") {
    val ctx =
      baseCtx().copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = Some(
              CompensationInfo(
                investedMaterial = 300,
                returnVector = Map("initiative" -> 0.8),
                expiryPly = Some(8),
                conversionPlan = "long-term pressure"
              )
            ),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("g2g4", "h7h6", "a2a3", "a7a6"),
                scoreCp = 20,
                depth = 18,
                parsedMoves = List(
                  PvMove("g2g4", "g4", "g2", "g4", "P", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("h7h6", "h6", "h7", "h6", "P", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("a2a3", "a3", "a2", "a3", "P", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("a7a6", "a6", "a7", "a6", "P", isCapture = false, capturedPiece = None, givesCheck = false)
                )
              )
            )
          )
        )
      )
    val contract =
      truthContract(DecisiveTruthClass.CompensatedInvestment, DecisiveReasonKind.InvestmentSacrifice).copy(
        surfaceMode = TruthSurfaceMode.InvestmentExplain,
        verifiedPayoffAnchor = Some("pressure on g7"),
        compensationProseAllowed = true
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              route = List("d1", "g4", "h5"),
              purpose = "pressure on g7",
              strategicFit = 0.84,
              tacticalSafety = 0.72,
              surfaceConfidence = 0.88,
              surfaceMode = RouteSurfaceMode.Exact,
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_g7",
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              targetSquare = "g7",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("pressure on g7"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(compensation = Some("pressure on g7")))
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, pack, Some(contract)),
      PlayerFacingTruthMode.Strategic
    )
  }

  test("unsupported sacrifice romance resolves to Minimal") {
    val ctx =
      baseCtx().copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = Some(
              CompensationInfo(
                investedMaterial = 300,
                returnVector = Map.empty,
                expiryPly = None,
                conversionPlan = ""
              )
            ),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 16,
            variations = List(VariationLine(List("g2g4", "h7h6"), scoreCp = 0, depth = 16))
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, None, None),
      PlayerFacingTruthMode.Minimal
    )
  }

  test("main-path exchange forcing requires both an anchored line and move-linked exchange evidence") {
    val ctx =
      baseCtx().copy(
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("c4")),
            logicSummary = "The exchange theme is in the air.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("exchange on c4"),
              planAdvancements = Nil,
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "B",
              from = "g2",
              target = "c4",
              idea = "exchange on c4",
              tacticalTheme = Some("exchange"),
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c4",
              ownerSide = "white",
              piece = "B",
              from = "g2",
              targetSquare = "c4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("exchange on c4"),
              evidence = List("probe")
            )
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ),
      None
    )

    val exchangeCtx =
      ctx.copy(
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("g2c4", "d5c4", "d1d8"),
                scoreCp = 34,
                depth = 18,
                parsedMoves = List(
                  PvMove("g2c4", "Bxc4", "g2", "c4", "B", isCapture = true, capturedPiece = Some("p"), givesCheck = false),
                  PvMove("d5c4", "dxc4", "d5", "c4", "P", isCapture = true, capturedPiece = Some("b"), givesCheck = false),
                  PvMove("d1d8", "Qd8+", "d1", "d8", "Q", isCapture = false, capturedPiece = None, givesCheck = true)
                )
              )
            )
          )
        )
      )

    val provingOnlyCtx =
      baseCtx().copy(
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("g2c4", "d5c4", "d1d8"),
                scoreCp = 34,
                depth = 18,
                parsedMoves = List(
                  PvMove("g2c4", "Bxc4", "g2", "c4", "B", isCapture = true, capturedPiece = Some("p"), givesCheck = false),
                  PvMove("d5c4", "dxc4", "d5", "c4", "P", isCapture = true, capturedPiece = Some("b"), givesCheck = false),
                  PvMove("d1d8", "Qd8+", "d1", "d8", "Q", isCapture = false, capturedPiece = None, givesCheck = true)
                )
              )
            )
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        provingOnlyCtx,
        StrategyPackSurface.from(pack),
        None
      ),
      None
    )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        exchangeCtx,
        StrategyPackSurface.from(pack),
        None
      ).map(_.deltaClass),
      Some(PlayerFacingMoveDeltaClass.ExchangeForcing)
    )

    val directCaptureCtx =
      baseCtx().copy(
        playedMove = Some("g2c4"),
        playedSan = Some("Bxc4"),
        engineEvidence = exchangeCtx.engineEvidence
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        directCaptureCtx,
        StrategyPackSurface.from(pack),
        None
      ),
      None
    )
  }

  test("main-path resource removal requires a specific prevented resource") {
    val basePrevented =
      PreventedPlanInfo(
        planId = "Queenside counterplay",
        deniedSquares = List("c4"),
        breakNeutralized = None,
        mobilityDelta = -1,
        counterplayScoreDrop = 80,
        preventedThreatType = None
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c4",
              ownerSide = "white",
              piece = "N",
              from = "e3",
              targetSquare = "c4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("control c4"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("control c4")))
        )
      )

    val genericCtx =
      baseCtx().copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(basePrevented),
            conceptSummary = Nil
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        genericCtx,
        StrategyPackSurface.from(pack),
        None
      ).map(_.deltaClass),
      Some(PlayerFacingMoveDeltaClass.CounterplayReduction)
    )

    val specificCtx =
      genericCtx.copy(
        semantic = genericCtx.semantic.map(_.copy(
          preventedPlans = List(
            basePrevented.copy(
              preventedThreatType = Some("entry_square"),
              citationLine = Some("...Qa5 can no longer enter on c4.")
            )
          )
        )),
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.NarrowChoice,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Attack", None, "independent"),
            whyNot = Some("Black can no longer use c4 as an entry square.")
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        specificCtx,
        StrategyPackSurface.from(pack),
        None
      ).map(_.deltaClass),
      Some(PlayerFacingMoveDeltaClass.ResourceRemoval)
    )
  }

  test("exact local file-entry bind packet is admitted as a bounded move-local half-open-file release") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "true_local_file_entry_bind",
              planName = "Take the c-file away and keep b4 closed",
              subplanId = PlanTaxonomy.PlanKind.KeySquareDenial.id,
              executionSteps = List("Take the c-file away first and keep b4 closed.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "true_local_file_entry_bind",
              subplanId = PlanTaxonomy.PlanKind.KeySquareDenial.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_c_file",
                deniedSquares = List("c5"),
                breakNeutralized = Some("c-file"),
                mobilityDelta = -2,
                counterplayScoreDrop = 145,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                deniedEntryScope = Some("file")
              ),
              PreventedPlanInfo(
                planId = "deny_entry",
                deniedSquares = List("b4"),
                breakNeutralized = None,
                mobilityDelta = -2,
                counterplayScoreDrop = 130,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("entry_square"),
                deniedEntryScope = Some("single_square")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8", "f8e8", "c8e8"),
                scoreCp = 90,
                depth = 18,
                parsedMoves = List(
                  PvMove("c1c8", "Rc8", "c1", "c8", "R", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("f8e8", "Rfe8", "f8", "e8", "R", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("c8e8", "Rxe8+", "c8", "e8", "R", isCapture = true, capturedPiece = Some("r"), givesCheck = true)
                )
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_b4",
              ownerSide = "white",
              piece = "R",
              from = "c1",
              targetSquare = "b4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("keep b4 closed while controlling the c-file"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("keep b4 closed while controlling the c-file")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).get

    assertEquals(delta.packet.proofSource, "local_file_entry_bind")
    assertEquals(delta.packet.proofFamily, "half_open_file_pressure")
    assertEquals(delta.packet.bestDefenseBranchKey, Some("c1c8|f8e8"))
    assertEquals(delta.packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(delta.packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(delta.packet.releaseRisks, Nil)
    assertEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assert(PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet))
  }

  test("file-entry promotion fails closed when the best-defense branch key is missing") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "local_file_entry_bind_missing_branch",
              planName = "Take the c-file away and keep b4 closed",
              subplanId = PlanTaxonomy.PlanKind.KeySquareDenial.id,
              executionSteps = List("Take the c-file away first and keep b4 closed.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "local_file_entry_bind_missing_branch",
              subplanId = PlanTaxonomy.PlanKind.KeySquareDenial.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_c_file",
                deniedSquares = List("c5"),
                breakNeutralized = Some("c-file"),
                mobilityDelta = -2,
                counterplayScoreDrop = 145,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                deniedEntryScope = Some("file")
              ),
              PreventedPlanInfo(
                planId = "deny_entry",
                deniedSquares = List("b4"),
                breakNeutralized = None,
                mobilityDelta = -2,
                counterplayScoreDrop = 130,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("entry_square"),
                deniedEntryScope = Some("single_square")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8"),
                scoreCp = 90,
                depth = 18,
                parsedMoves = List(
                  PvMove("c1c8", "Rc8", "c1", "c8", "R", isCapture = false, capturedPiece = None, givesCheck = false)
                )
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_b4",
              ownerSide = "white",
              piece = "R",
              from = "c1",
              targetSquare = "b4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("keep b4 closed while controlling the c-file"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("keep b4 closed while controlling the c-file")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).get

    assertEquals(delta.packet.bestDefenseBranchKey, None)
    assertNotEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assert(!PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet))
    Option.when(delta.packet.proofFamily == "half_open_file_pressure")(delta.packet.sameBranchState).foreach { state =>
      assertEquals(state, PlayerFacingSameBranchState.Missing)
    }
  }

  test("exact named-break suppression packet is admitted as a bounded move-local neutralize-key-break release") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "named_break_suppression",
              planName = "Clamp the ...c5 break",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id,
              executionSteps = List("Keep the opponent's main counterplay route closed first.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "named_break_suppression",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_counterplay",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                citationLine = Some("The ...c5 break never becomes available on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8", "f8e8", "c8e8"),
                scoreCp = 88,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).get

    assertEquals(delta.packet.proofSource, "counterplay_axis_suppression")
    assertEquals(delta.packet.proofFamily, "neutralize_key_break")
    assertEquals(delta.packet.bestDefenseBranchKey, Some("c1c8|f8e8"))
    assertEquals(delta.packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(delta.packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(delta.packet.releaseRisks, Nil)
    assertEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assert(PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet))

    val malformedCtx =
      ctx
        .copy(
          semantic = ctx.semantic.map(section =>
            section.copy(
              preventedPlans = section.preventedPlans.map(plan =>
                plan.copy(
                  deniedSquares = List("counterplay window"),
                  breakNeutralized = None,
                  deniedResourceClass = None
                )
              )
            )
          )
        )
        .withTypedEvidenceFromLegacy
    val malformedDelta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        malformedCtx,
        StrategyPackSurface.from(pack),
        None
      )

    assert(
      malformedDelta.forall(delta =>
        delta.packet.fallbackMode != PlayerFacingClaimFallbackMode.WeakMain &&
          !PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet)
      ),
      clues(malformedDelta)
    )
  }

  test("forcing-defense threat timing remains planner-owned when no named-break witness matches packet proof") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        authorQuestions = List(AuthorQuestion("why_now", AuthorQuestionKind.WhyNow, 100, "Why now?")),
        threats = ThreatTable(
          toUs = List(
            ThreatRow(
              kind = "Material",
              side = "US",
              square = Some("c8"),
              lossIfIgnoredCp = 320,
              turnsToImpact = 1,
              bestDefense = Some("c1c8"),
              defenseCount = 1,
              insufficientData = false
            )
          ),
          toThem = Nil
        ),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "named_break_timing_release",
              planName = "Clamp the ...c5 break",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id,
              executionSteps = List("Keep the opponent's main counterplay route closed first.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "named_break_timing_release",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_counterplay",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                citationLine = Some("The ...c5 break never becomes available on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8", "f8e8", "c8e8"),
                scoreCp = 88,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
        )
      )

    val inputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val primary =
      ranked.primary.getOrElse(fail(s"stable named-break timing should produce a plan: ${ranked.ownerTrace} ${ranked.rejected}"))

    assertEquals(primary.questionKind, AuthorQuestionKind.WhyNow)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.ForcingDefense)
    assertEquals(primary.fallbackMode, QuestionPlanFallbackMode.PlannerOwned)
    assertEquals(primary.strengthTier, QuestionPlanStrengthTier.Strong)
    assert(primary.admissibilityReasons.contains("timing_owner"), clues(primary))
    assert(!primary.admissibilityReasons.contains("strategic_claim_supported_local"), clues(primary))
    assertEquals(
      primary.claim,
      "The move has to happen now because otherwise c1c8 is demanded immediately."
    )
    assertEquals(primary.prefixKind, PlayerFacingClaimPrefixKind.None)
    assertEquals(primary.evidence, None)
    assert(primary.contrast.nonEmpty, clues(primary))
    assert(primary.consequence.nonEmpty, clues(primary))
  }

  test("forcing-defense WhatMustBeStopped threat timing remains planner-owned without a named-break witness") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        authorQuestions = List(AuthorQuestion("what_stop", AuthorQuestionKind.WhatMustBeStopped, 100, "What must be stopped?")),
        threats = ThreatTable(
          toUs = List(
            ThreatRow(
              kind = "Material",
              side = "US",
              square = Some("c8"),
              lossIfIgnoredCp = 320,
              turnsToImpact = 1,
              bestDefense = Some("c1c8"),
              defenseCount = 1,
              insufficientData = false
            )
          ),
          toThem = Nil
        ),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "named_break_stop_release",
              planName = "Clamp the ...c5 break",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id,
              executionSteps = List("Keep the opponent's main counterplay route closed first.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "named_break_stop_release",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_counterplay",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                citationLine = Some("The ...c5 break never becomes available on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8", "f8e8", "c8e8"),
                scoreCp = 88,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
        )
      )

    val inputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val primary =
      ranked.primary.getOrElse(fail(s"stable WhatMustBeStopped timing should produce a plan: ${ranked.ownerTrace} ${ranked.rejected}"))

    assertEquals(primary.questionKind, AuthorQuestionKind.WhatMustBeStopped)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.ForcingDefense)
    assertEquals(primary.plannerSource, "threat")
    assertEquals(primary.timingWitness.map(_.proofFamily), Some("neutralize_key_break"))
    assert(primary.timingWitness.exists(_.witnessTokens.contains("c1c8")), clues(primary))
    assertEquals(primary.fallbackMode, QuestionPlanFallbackMode.PlannerOwned)
    assertEquals(primary.strengthTier, QuestionPlanStrengthTier.Strong)
    assert(primary.admissibilityReasons.contains("defensive_owner"), clues(primary))
    assert(!primary.admissibilityReasons.contains("strategic_claim_supported_local"), clues(primary))
    assertEquals(
      primary.claim,
      "This has to stop the opponent's material threat before it lands."
    )
    assertEquals(primary.prefixKind, PlayerFacingClaimPrefixKind.None)
    assertEquals(primary.evidence, None)
    assert(primary.contrast.nonEmpty, clues(primary))
    assertEquals(primary.consequence, None)
  }

  test("forcing-defense named-break timing stays unreleased when packet proof lacks a branch") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        authorQuestions = List(AuthorQuestion("why_now", AuthorQuestionKind.WhyNow, 100, "Why now?")),
        threats = ThreatTable(
          toUs = List(
            ThreatRow(
              kind = "Material",
              side = "US",
              square = Some("c8"),
              lossIfIgnoredCp = 320,
              turnsToImpact = 1,
              bestDefense = Some("c1c8"),
              defenseCount = 1,
              insufficientData = false
            )
          ),
          toThem = Nil
        ),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "named_break_timing_missing_branch",
              planName = "Clamp the ...c5 break",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id,
              executionSteps = List("Keep the opponent's main counterplay route closed first.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "named_break_timing_missing_branch",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_counterplay",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                citationLine = Some("The ...c5 break never becomes available on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8"),
                scoreCp = 88,
                depth = 18
              )
            )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
        )
      )

    val inputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val primary = ranked.primary.getOrElse(fail("forcing timing shell should still produce a plan"))

    assertEquals(primary.questionKind, AuthorQuestionKind.WhyNow)
    assert(!primary.admissibilityReasons.contains("strategic_claim_supported_local"), clues(primary))
    assertNotEquals(primary.fallbackMode, QuestionPlanFallbackMode.FactualFallback)
  }

  test("forcing-defense named-break timing stays unreleased when timing text is uncoupled from packet witness") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        authorQuestions = List(AuthorQuestion("why_now", AuthorQuestionKind.WhyNow, 100, "Why now?")),
        threats = ThreatTable(
          toUs = List(
            ThreatRow(
              kind = "Material",
              side = "US",
              square = Some("h3"),
              lossIfIgnoredCp = 320,
              turnsToImpact = 1,
              bestDefense = Some("h2h3"),
              defenseCount = 1,
              insufficientData = false
            )
          ),
          toThem = Nil
        ),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "named_break_timing_uncoupled",
              planName = "Clamp the ...c5 break",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id,
              executionSteps = List("Keep the opponent's main counterplay route closed first.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "named_break_timing_uncoupled",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_counterplay",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                citationLine = Some("The ...c5 break never becomes available on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8", "f8e8", "c8e8"),
                scoreCp = 88,
                depth = 18
              )
            )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
        )
      )

    val inputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val primary =
      ranked.primary.getOrElse(fail(s"uncoupled forcing timing shell should still produce a plan: ${ranked.ownerTrace} ${ranked.rejected}"))

    assertEquals(primary.questionKind, AuthorQuestionKind.WhyNow)
    assert(!primary.admissibilityReasons.contains("strategic_claim_supported_local"), clues(primary))
    assertNotEquals(primary.fallbackMode, QuestionPlanFallbackMode.FactualFallback)
  }

  test("forcing-defense threat timing is not treated as named-break SupportedLocal under tactical truth mode") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        authorQuestions = List(AuthorQuestion("why_now", AuthorQuestionKind.WhyNow, 100, "Why now?")),
        threats = ThreatTable(
          toUs = List(
            ThreatRow(
              kind = "Material",
              side = "US",
              square = Some("c8"),
              lossIfIgnoredCp = 320,
              turnsToImpact = 1,
              bestDefense = Some("c1c8"),
              defenseCount = 1,
              insufficientData = false
            )
          ),
          toThem = Nil
        ),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "named_break_timing_tactical_veto",
              planName = "Clamp the ...c5 break",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id,
              executionSteps = List("Keep the opponent's main counterplay route closed first.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "named_break_timing_tactical_veto",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_counterplay",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                citationLine = Some("The ...c5 break never becomes available on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8", "f8e8", "c8e8"),
                scoreCp = 88,
                depth = 18
              )
            )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
        )
      )

    val stableInputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
    assert(
      stableInputs.mainBundle.flatMap(_.mainClaim).flatMap(_.packet).exists(packet =>
        packet.proofFamily == "neutralize_key_break" &&
          packet.sameBranchState == PlayerFacingSameBranchState.Proven &&
          packet.persistence == PlayerFacingClaimPersistence.Stable
      )
    )
    val tacticalInputs = stableInputs.copy(truthMode = PlayerFacingTruthMode.Tactical)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, tacticalInputs, truthContract = None)

    assert(ranked.primary.forall(!_.admissibilityReasons.contains("strategic_claim_supported_local")), clues(ranked.primary))
    assertEquals(ranked.rejected.filter(_.reasons.contains("strategic_claim_tactical_veto")), Nil)
  }

  test("named-break promotion fails closed when the best-defense branch key is missing") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "named_break_suppression_missing_branch",
              planName = "Clamp the ...c5 break",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id,
              executionSteps = List("Keep the opponent's main counterplay route closed first.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "named_break_suppression_missing_branch",
              subplanId = PlanTaxonomy.PlanKind.BreakPrevention.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_counterplay",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                citationLine = Some("The ...c5 break never becomes available on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8"),
                scoreCp = 88,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c5",
              ownerSide = "white",
              piece = "R",
              from = "c1",
              targetSquare = "c5",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("deny the ...c5 break"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).get

    assertEquals(delta.packet.proofFamily, "neutralize_key_break")
    assertEquals(delta.packet.bestDefenseBranchKey, None)
    assertEquals(delta.packet.sameBranchState, PlayerFacingSameBranchState.Missing)
    assert(delta.packet.suppressionReasons.contains(PlayerFacingClaimSuppressionReason.SameBranchMissing))
    assertEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.LineOnly)
    assert(!PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet))
  }

  test("named-break shell does not materialize counterplay-axis authority under a rival family") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "iqp_rival_shell",
              planName = "Induce the isolated pawn",
              subplanId = PlanTaxonomy.PlanKind.IQPInducement.id,
              executionSteps = List("Use the central capture sequence.")
            )
          ),
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "iqp_rival_shell",
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
              subplanId = Some(PlanTaxonomy.PlanKind.IQPInducement.id),
              evidenceTier = "evidence_backed",
              supportProbeCount = 1,
              refuteProbeCount = 0,
              bestReplyStable = true,
              futureSnapshotAligned = true,
              counterBreakNeutralized = false,
              moveOrderSensitive = false,
              experimentConfidence = 0.86
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_counterplay",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                citationLine = Some("The ...c5 break never becomes available on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8", "f8e8", "c8c6"),
                scoreCp = 88,
                depth = 18
              )
            )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c5",
              ownerSide = "white",
              piece = "R",
              from = "c1",
              targetSquare = "c5",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("deny the ...c5 break"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).get

    assertNotEquals(delta.packet.proofSource, "counterplay_axis_suppression")
    assert(!PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet), clues(delta.packet))
  }

  test("exact prophylactic-move packet is admitted as a bounded move-local counterplay-restraint release") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "prophylactic_move_named_resource",
              planName = "Slow queenside counterplay before expanding",
              subplanId = PlanTaxonomy.PlanKind.ProphylaxisRestraint.id,
              executionSteps = List("Slow queenside counterplay before expanding.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "prophylactic_move_named_resource",
              subplanId = PlanTaxonomy.PlanKind.ProphylaxisRestraint.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "queenside counterplay",
                deniedSquares = Nil,
                breakNeutralized = None,
                mobilityDelta = -2,
                counterplayScoreDrop = 138,
                preventedThreatType = None,
                deniedResourceClass = Some("counterplay_route"),
                citationLine = Some("Queenside counterplay never gets going on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("a2a3", "b7b5", "a3a4"),
                scoreCp = 82,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_b4",
              ownerSide = "white",
              piece = "P",
              from = "a2",
              targetSquare = "b4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("queenside counterplay"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("queenside counterplay")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).get

    assertEquals(delta.packet.proofSource, "prophylactic_move")
    assertEquals(delta.packet.proofFamily, "counterplay_restraint")
    assertEquals(delta.packet.bestDefenseBranchKey, Some("a2a3|b7b5"))
    assertEquals(delta.packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(delta.packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(delta.packet.releaseRisks, Nil)
    assertEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assert(PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet))
  }

  test("prophylactic-move promotion fails closed when the best-defense branch key is missing") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "prophylactic_move_named_resource_missing_branch",
              planName = "Slow queenside counterplay before expanding",
              subplanId = PlanTaxonomy.PlanKind.ProphylaxisRestraint.id,
              executionSteps = List("Slow queenside counterplay before expanding.")
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "prophylactic_move_named_resource_missing_branch",
              subplanId = PlanTaxonomy.PlanKind.ProphylaxisRestraint.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "queenside counterplay",
                deniedSquares = Nil,
                breakNeutralized = None,
                mobilityDelta = -2,
                counterplayScoreDrop = 138,
                preventedThreatType = None,
                deniedResourceClass = Some("counterplay_route"),
                citationLine = Some("Queenside counterplay never gets going on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("a2a3"),
                scoreCp = 82,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_b4",
              ownerSide = "white",
              piece = "P",
              from = "a2",
              targetSquare = "b4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("queenside counterplay"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("queenside counterplay")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).get

    assertEquals(delta.packet.proofSource, "prophylactic_move")
    assertEquals(delta.packet.proofFamily, "counterplay_restraint")
    assertEquals(delta.packet.bestDefenseBranchKey, None)
    assertEquals(delta.packet.sameBranchState, PlayerFacingSameBranchState.Missing)
    assert(delta.packet.suppressionReasons.contains(PlayerFacingClaimSuppressionReason.SameBranchMissing))
    assertEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.LineOnly)
    assert(!PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet))
  }

  test("quiet neutralize-key-break remains non-user-facing after main-path promotion") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_counterplay",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                citationLine = Some("The ...c5 break never becomes available on the defended branch.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8", "f8e8", "c8e8"),
                scoreCp = 88,
                depth = 18
              )
            )
          )
        )
      )

    assertEquals(QuietMoveIntentBuilder.build(ctx), None)
  }

  test("defensive-regrouping stays absorbed into prophylactic_move rather than opening a distinct owner path") {
    val ctx =
      baseCtx().copy(
        fen = "r1bq1rk1/pp3ppp/2n1pn2/2bp4/3P4/2N1PN2/PPQ2PPP/R1B2RK1 w - - 0 12",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "defensive_regrouping_shell",
              planName = "Re-anchor the bishop to blunt queenside counterplay",
              subplanId = PlanTaxonomy.PlanKind.WorstPieceImprovement.id,
              executionSteps = List("Re-anchor the bishop and cover the queenside route."),
              themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "defensive_regrouping_shell",
              subplanId = PlanTaxonomy.PlanKind.WorstPieceImprovement.id,
              themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "queenside counterplay",
                deniedSquares = Nil,
                breakNeutralized = None,
                mobilityDelta = -1,
                counterplayScoreDrop = 96,
                preventedThreatType = None,
                deniedResourceClass = Some("counterplay_route"),
                citationLine = Some("The regrouping move slows queenside counterplay.")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 16,
            variations = List(
              VariationLine(
                moves = List("c1e3", "b7b5", "e3d4"),
                scoreCp = 44,
                depth = 16
              )
            )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_d4",
              ownerSide = "white",
              piece = "B",
              from = "c1",
              targetSquare = "d4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("cover queenside counterplay"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("cover queenside counterplay")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      )

    delta.foreach { evidence =>
      assertNotEquals(evidence.packet.proofSource, "prophylactic_move")
      assertNotEquals(evidence.packet.proofFamily, "counterplay_restraint")
    }
  }

  test("open-file-control stays absorbed into half-open-file pressure without an independent file-entry proof") {
    val ctx =
      baseCtx().copy(
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "open_file_control_shell",
              planName = "Occupy the c-file with heavy pieces",
              subplanId = PlanTaxonomy.PlanKind.OpenFilePressure.id,
              executionSteps = List("Occupy the c-file and pressure it."),
              themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "open_file_control_shell",
              subplanId = PlanTaxonomy.PlanKind.OpenFilePressure.id,
              themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
            )
          ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(
              PreventedPlanInfo(
                planId = "deny_c_file",
                deniedSquares = List("c5"),
                breakNeutralized = Some("c-file"),
                mobilityDelta = -2,
                counterplayScoreDrop = 145,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                deniedEntryScope = Some("file")
              )
            ),
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("c1c8", "f8e8", "c8e8"),
                scoreCp = 90,
                depth = 18
              )
            )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c5",
              ownerSide = "white",
              piece = "R",
              from = "c1",
              targetSquare = "c5",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("control the c-file"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("control the c-file")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      )

    delta.foreach { evidence =>
      assertNotEquals(evidence.packet.proofFamily, "half_open_file_pressure")
    }
  }

  test("weakness-fixation stays absorbed even when the weak complex is exact and branch-visible") {
    val ctx =
      baseCtx().copy(
        fen = "rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 0 9",
        playedMove = Some("f3d2"),
        playedSan = Some("Nd2"),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "weakness_fixation_shell",
              planName = "Keep the d6 weakness fixed",
              subplanId = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
              executionSteps = List("Fix the d6 target and keep building pressure."),
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "weakness_fixation_shell",
              subplanId = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("d6")),
            logicSummary = "The move keeps pressure fixed on d6.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("pressure on d6"),
              planAdvancements = List("keep the d6 weakness fixed"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = List(
              WeakComplexInfo(
                owner = "Black",
                squareColor = "dark",
                squares = List("d6"),
                isOutpost = false,
                cause = "Backward pawn on d6"
              )
            ),
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("f3d2", "b8a6", "e1g1"),
                scoreCp = 71,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_d6",
              ownerSide = "white",
              piece = "N",
              from = "f3",
              targetSquare = "d6",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("keep pressure on d6"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("pressure on d6")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).get

    assertEquals(delta.packet.proofFamily, PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
    assertEquals(delta.packet.bestDefenseBranchKey, Some("f3d2|b8a6"))
    assertEquals(delta.packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(delta.packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(delta.packet.proofPathWitness.ownerSeedTerms.contains("d6"), true)
    assert(delta.packet.proofPathWitness.continuationTerms.nonEmpty)
    assertNotEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assert(!PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet))

    val bundle = MainPathMoveDeltaClaimBuilder.build(ctx, pack, None)
    assertEquals(bundle.flatMap(_.mainClaim), None)
  }

  test("exact weakness positive controls promote planner-owned WhatChanged state delta on the admitted target-fixation lane") {
    List("B21", "B21A").foreach { id =>
      val (_, pack, inputs, ranked) = exactReviewSnapshot(id)

      if !pack.strategicIdeas.exists(_.kind == StrategicIdeaKind.TargetFixing) then
        fail(s"$id should still expose target-fixing evidence")

      val mainClaim =
        inputs.mainBundle.flatMap(_.mainClaim).getOrElse(fail(s"$id should admit a main claim"))
      val packet = mainClaim.packet.getOrElse(fail(s"$id should carry the owner packet"))

      assertEquals(mainClaim.claimText, "This keeps the pressure fixed on d6.")
      assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.ExactTargetFixationProofSource)
      assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
      assertEquals(packet.bestDefenseBranchKey, Some("f3d2|b8a6"))
      assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
      assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
      assertEquals(packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
      assert(packet.proofPathWitness.ownerSeedTerms.contains("d6"))
      assertEquals(
        ranked.primary.map(_.questionKind),
        Some(AuthorQuestionKind.WhatChanged)
      )
      assertEquals(
        ranked.primary.map(_.claim),
        Some("This changes the position by fixing d6 as the target.")
      )
      assertEquals(
        ranked.primary.flatMap(_.contrast),
        Some("Before the move, d6 was not yet fixed as the target on that defended branch.")
      )
      assertEquals(
        ranked.primary.flatMap(_.consequence.map(_.text)),
        Some("That same defended branch keeps the pressure fixed on d6.")
      )
      assertEquals(ranked.primary.map(_.plannerOwnerKind), Some(PlannerOwnerKind.MoveDelta))
      assert(ranked.primary.exists(_.admissibilityReasons.contains("exact_target_state_delta")))
      assertEquals(ranked.secondary, None)
    }
  }

  test("carlsbad fixed-target positive controls promote planner-owned WhatMattersHere position probes") {
    val expectedBranchKeys =
      Map(
        "B15A" -> "h4f2|b7b5",
        "B16B" -> "f1e1|c8d7"
      )

    List("B15A", "B16B").foreach { id =>
      val (_, pack, inputs, ranked) = exactReviewSnapshot(id)

      assert(!pack.strategicIdeas.exists(_.kind == StrategicIdeaKind.TargetFixing), clues(id, pack.strategicIdeas))

      val mainClaim =
        inputs.mainBundle.flatMap(_.mainClaim).getOrElse(fail(s"$id should now admit a position probe"))
      val packet = mainClaim.packet.getOrElse(fail(s"$id should carry the probe packet"))

      assertEquals(mainClaim.scope, PlayerFacingClaimScope.PositionLocal)
      assertEquals(mainClaim.claimText, "c6 is the fixed target.")
      assertEquals(mainClaim.prefixKind, PlayerFacingClaimPrefixKind.KeyStrategicFact)
      assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource)
      assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.BackwardPawnTargeting.id)
      assertEquals(packet.scope, PlayerFacingPacketScope.PositionLocal)
      assertEquals(packet.bestDefenseBranchKey, expectedBranchKeys.get(id))
      assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
      assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
      assertEquals(packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
      assert(packet.proofPathWitness.ownerSeedTerms.contains("c6"))
      assert(packet.proofPathWitness.ownerSeedTerms.contains("fixed_target:c6"))
      assert(packet.proofPathWitness.ownerSeedTerms.contains(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id))
      assertEquals(
        packet.proofPathWitness.exactSliceProof,
        Some(PlayerFacingExactSliceProof.CarlsbadFixedTarget("c6", minoritySupport = true))
      )
      assert(
        !PlayerFacingTruthModePolicy.certifiedPositionProbePacket(
          packet.copy(proofPathWitness = packet.proofPathWitness.copy(exactSliceProof = None))
        )
      )
      assert(
        PlayerFacingTruthModePolicy.certifiedPositionProbePacket(
          packet.copy(
            proofPathWitness = packet.proofPathWitness.copy(
              exactSliceProof = Some(PlayerFacingExactSliceProof.CarlsbadFixedTarget("c3", minoritySupport = true))
            )
          )
        )
      )
      assertEquals(
        ranked.primary.map(_.questionKind),
        Some(AuthorQuestionKind.WhatMattersHere)
      )
      assertEquals(
        ranked.primary.map(_.claim),
        Some("c6 is the fixed target.")
      )
      assertEquals(
        ranked.primary.flatMap(_.consequence.map(_.text)),
        Some("So the task is to keep the queenside pressure trained on c6 instead of rushing a conversion.")
      )
      assertEquals(ranked.primary.map(_.plannerOwnerKind), Some(PlannerOwnerKind.PositionProbe))
      assert(ranked.primary.exists(_.admissibilityReasons.contains("position_probe_owner")))
      assertEquals(ranked.secondary, None)
    }
  }

  test("target-focused coordination positive controls promote planner-owned WhatMattersHere position probes") {
    val expectedBranchKeys =
      Map(
        "K09A" -> "d1b3|d8d7",
        "K09D" -> "h2h3|g4f3"
      )

    List("K09A", "K09D").foreach { id =>
      val (_, pack, inputs, ranked) = exactReviewSnapshot(id)

      assert(pack.strategicIdeas.exists(_.kind == StrategicIdeaKind.FavorableTradeOrTransformation), clues(id, pack.strategicIdeas))

      val mainClaim =
        inputs.mainBundle.flatMap(_.mainClaim).getOrElse(fail(s"$id should now admit a coordination probe"))
      val packet = mainClaim.packet.getOrElse(fail(s"$id should carry the probe packet"))

      assertEquals(mainClaim.scope, PlayerFacingClaimScope.PositionLocal)
      assertEquals(mainClaim.claimText, "the pressure is coordinated on c6.")
      assertEquals(mainClaim.prefixKind, PlayerFacingClaimPrefixKind.KeyStrategicFact)
      assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource)
      assertEquals(packet.proofFamily, PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily)
      assertEquals(packet.scope, PlayerFacingPacketScope.PositionLocal)
      assertEquals(packet.bestDefenseBranchKey, expectedBranchKeys.get(id))
      assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
      assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
      assertEquals(packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
      assert(packet.bestDefenseMove.nonEmpty)
      assert(packet.proofPathWitness.ownerSeedTerms.contains("c6"))
      assert(packet.proofPathWitness.ownerSeedTerms.contains("coordinated_target:c6"))
      assert(packet.proofPathWitness.ownerSeedTerms.contains("rook_on_c1"))
      assert(packet.proofPathWitness.continuationTerms.contains(PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource))
      assert(packet.proofPathWitness.continuationTerms.contains("coordinated_target:c6"))
      assert(packet.proofPathWitness.structureTransitionTerms.contains("coordinated_piece_pressure"))
      assert(
        packet.proofPathWitness.exactSliceProof.exists {
          case PlayerFacingExactSliceProof.TargetFocusedCoordination("c6", supportFromSquares, targetPieces) =>
            supportFromSquares.distinct.size >= 2 && targetPieces.contains("target_knight")
          case _ => false
        },
        clues(packet.proofPathWitness.exactSliceProof)
      )
      assert(
        !PlayerFacingTruthModePolicy.certifiedPositionProbePacket(
          packet.copy(proofPathWitness = packet.proofPathWitness.copy(exactSliceProof = None))
        )
      )
      assertEquals(ranked.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatMattersHere))
      assertEquals(
        ranked.primary.map(_.claim),
        Some("the pressure is coordinated on c6.")
      )
      assertEquals(
        ranked.primary.flatMap(_.consequence.map(_.text)),
        Some("So the task is to keep the pressure coordinated on c6 until the target has to give way.")
      )
      assertEquals(ranked.primary.map(_.plannerOwnerKind), Some(PlannerOwnerKind.PositionProbe))
      assertEquals(ranked.primary.map(_.plannerSource), Some(PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource))
      assert(ranked.primary.exists(_.admissibilityReasons.contains("certified_position_probe")))
      assertEquals(ranked.secondary, None)
    }
  }

  test("reviewed sibling rows and blocked controls keep the owner path closed") {
    List("K03A").foreach { id =>
      val (_, _, inputs, ranked) = exactReviewSnapshot(id)
      assertEquals(inputs.mainBundle.flatMap(_.mainClaim), None)
      assert(ranked.primary.isEmpty)
      assert(ranked.rejected.exists(_.reasons.contains("position_probe_missing")))
    }

    List("K09E", "D01A").foreach { id =>
      val (_, pack, inputs, ranked) = exactReviewSnapshot(id)

      if pack.strategicIdeas.headOption.exists(_.kind == StrategicIdeaKind.TargetFixing) then
        fail(s"$id should stay rival-dominated rather than weakness-owned")

      assertEquals(inputs.mainBundle.flatMap(_.mainClaim), None)
      assert(ranked.primary.isEmpty)
      assert(ranked.rejected.exists(_.reasons.contains("missing_move_owner")))
    }
  }

  test("target-focused coordination deterministic surfaces keep WhatMattersHere as planner-owned primary") {
    List("K09A", "K09D").foreach { id =>
      val (_, ctx, pack, inputs, ranked) = exactReviewScene(id)

      val moveReviewSelection =
        MoveReviewCompressionPolicy.renderSelection(inputs, ranked, truthContract = None)
          .getOrElse(fail(s"$id should select a planner-owned moveReview surface"))
      val moveReviewSlots =
        MoveReviewCompressionPolicy
          .buildSlotsOrFallbackFromPlannerRuntime(
            ctx = ctx,
            inputs = inputs,
            rankedPlans = ranked,
            strategyPack = Some(pack),
            truthContract = None
          )
      val moveReviewFallbackAware =
        MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
          ctx = ctx,
          inputs = inputs,
          rankedPlans = ranked,
          strategyPack = Some(pack),
          truthContract = None
        )

      assertEquals(moveReviewSelection.primary.questionKind, AuthorQuestionKind.WhatMattersHere)
      assertEquals(moveReviewSelection.primary.plannerOwnerKind, PlannerOwnerKind.PositionProbe)
      assertEquals(
        MoveReviewProseContract.stripMoveHeader(moveReviewSlots.claim),
        "The key strategic fact here is that the pressure is coordinated on c6."
      )
      assertEquals(
        moveReviewSlots.coda,
        Some("So the task is to keep the pressure coordinated on c6 until the target has to give way.")
      )
      assertEquals(moveReviewFallbackAware, moveReviewSlots)

      assertEquals(ranked.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatMattersHere))
      assertEquals(ranked.primary.map(_.plannerOwnerKind), Some(PlannerOwnerKind.PositionProbe))
    }
  }

  test("carlsbad probe deterministic surfaces keep WhatMattersHere as planner-owned primary") {
    List("B15A", "B16B").foreach { id =>
      val (_, ctx, pack, inputs, ranked) = exactReviewScene(id)

      val moveReviewSelection =
        MoveReviewCompressionPolicy.renderSelection(inputs, ranked, truthContract = None)
          .getOrElse(fail(s"$id should select a planner-owned moveReview surface"))
      val moveReviewSlots =
        MoveReviewCompressionPolicy
          .buildSlotsOrFallbackFromPlannerRuntime(
            ctx = ctx,
            inputs = inputs,
            rankedPlans = ranked,
            strategyPack = Some(pack),
            truthContract = None
          )
      val moveReviewFallbackAware =
        MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
          ctx = ctx,
          inputs = inputs,
          rankedPlans = ranked,
          strategyPack = Some(pack),
          truthContract = None
        )

      assertEquals(moveReviewSelection.primary.questionKind, AuthorQuestionKind.WhatMattersHere)
      assertEquals(moveReviewSelection.primary.plannerOwnerKind, PlannerOwnerKind.PositionProbe)
      assertEquals(
        MoveReviewProseContract.stripMoveHeader(moveReviewSlots.claim),
        "The key strategic fact here is that c6 is the fixed target."
      )
      assertEquals(
        moveReviewSlots.coda,
        Some("So the task is to keep the queenside pressure trained on c6 instead of rushing a conversion.")
      )
      assertEquals(moveReviewFallbackAware, moveReviewSlots)

      assertEquals(ranked.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatMattersHere))
      assertEquals(ranked.primary.map(_.plannerOwnerKind), Some(PlannerOwnerKind.PositionProbe))
    }
  }

  test("cluster B deterministic surfaces keep the exact target-fixation WhatChanged as planner-owned primary") {
    List("B21", "B21A").foreach { id =>
      val (_, ctx, pack, inputs, ranked) = exactReviewScene(id)
      val outline =
        BookStyleRenderer.validatedOutline(ctx, strategyPack = Some(pack), truthContract = None)

      val moveReviewSelection =
        MoveReviewCompressionPolicy.renderSelection(inputs, ranked, truthContract = None)
          .getOrElse(fail(s"$id should select a planner-owned moveReview surface"))
      val moveReviewSlots =
        MoveReviewCompressionPolicy
          .buildSlots(ctx, outline, refs = None, strategyPack = Some(pack))
          .getOrElse(fail(s"$id should build planner-owned moveReview slots"))
      val moveReviewFallbackAware =
        MoveReviewCompressionPolicy.buildSlotsOrFallback(
          ctx = ctx,
          outline = outline,
          refs = None,
          strategyPack = Some(pack),
          truthContract = None
        )

      assertEquals(moveReviewSelection.primary.questionKind, AuthorQuestionKind.WhatChanged)
      assertEquals(moveReviewSelection.primary.plannerOwnerKind, PlannerOwnerKind.MoveDelta)
      assertEquals(
        MoveReviewProseContract.stripMoveHeader(moveReviewSlots.claim),
        "This changes the position by fixing d6 as the target."
      )
      assertEquals(moveReviewFallbackAware, moveReviewSlots)

      assertEquals(ranked.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatChanged))
      assertEquals(ranked.primary.map(_.plannerOwnerKind), Some(PlannerOwnerKind.MoveDelta))
    }
  }

  test("bounded favorable simplification exact controls materialize a same-task owner path") {
    List("K09B", "K09F").foreach { id =>
      val (_, pack, inputs, ranked) = exactReviewSnapshot(id)

      assert(pack.strategicIdeas.exists(_.kind == StrategicIdeaKind.FavorableTradeOrTransformation))

      val mainClaim =
        inputs.mainBundle.flatMap(_.mainClaim).getOrElse(fail(s"$id should now admit a main claim"))
      val packet = mainClaim.packet.getOrElse(fail(s"$id should carry the owner packet"))

      assert(mainClaim.claimText.toLowerCase.contains("same local edge"))
      assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.SimplificationWindow.id)
      assertEquals(packet.bestDefenseBranchKey, Some("d4e6|f7e6"))
      assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
      assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
      assertEquals(packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
      assertEquals(ranked.primary.map(_.claim), Some(mainClaim.claimText))
      assertEquals(ranked.primary.map(_.questionKind), Some(AuthorQuestionKind.WhyThis))
      assert(!ranked.primary.exists(_.questionKind == AuthorQuestionKind.WhatChanged))
    }
  }

  test("attacking-piece trade review rows stay owner-closed when the charter collapses into rival shells") {
    List("K08A", "K08D").foreach { id =>
      val (_, _, inputs, ranked) = exactReviewSnapshot(id)

      assertEquals(inputs.mainBundle.flatMap(_.mainClaim), None)
      assert(ranked.primary.isEmpty)
      assert(
        ranked.rejected.exists(_.reasons.contains("missing_move_owner"))
      )
    }

    val (_, pack, inputs, ranked) = exactReviewSnapshot("MI5")
    assert(pack.strategicIdeas.exists(_.kind == StrategicIdeaKind.FavorableTradeOrTransformation))
    assertEquals(inputs.mainBundle.flatMap(_.mainClaim), None)
    assert(ranked.primary.isEmpty)
    assert(
      ranked.rejected.exists(_.reasons.contains("missing_move_owner"))
    )
  }

  test("remove-key-defender trigger-only review rows stay bounded to simplification or fail closed") {
    List("K09B", "K09F").foreach { id =>
      val (_, _, inputs, ranked) = exactReviewSnapshot(id)
      val mainClaim =
        inputs.mainBundle.flatMap(_.mainClaim).getOrElse(fail(s"$id should stay owned by simplification instead"))
      val packet = mainClaim.packet.getOrElse(fail(s"$id should carry the owner packet"))

      assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.SimplificationWindow.id)
      assertNotEquals(packet.proofFamily, "trade_key_defender")
      assert(mainClaim.claimText.toLowerCase.contains("same local edge"))
      assert(!mainClaim.claimText.toLowerCase.contains("defender"))
      assertEquals(ranked.primary.map(_.claim), Some(mainClaim.claimText))
    }

    List("K09A", "K09D").foreach { id =>
      val (_, _, inputs, ranked) = exactReviewSnapshot(id)
      val mainClaim =
        inputs.mainBundle.flatMap(_.mainClaim).getOrElse(fail(s"$id should stay on the coordination probe lane"))
      val packet = mainClaim.packet.getOrElse(fail(s"$id should carry the probe packet"))

      assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource)
      assertEquals(packet.proofFamily, PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily)
      assertNotEquals(packet.proofFamily, "trade_key_defender")
      assertEquals(ranked.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatMattersHere))
    }

    List("K09E", "MI5").foreach { id =>
      val (_, _, inputs, ranked) = exactReviewSnapshot(id)

      assertEquals(inputs.mainBundle.flatMap(_.mainClaim), None)
      assert(ranked.primary.isEmpty)
      assert(
        ranked.rejected.exists(_.reasons.contains("missing_move_owner"))
      )
    }
  }

  test("minority-attack-fixation shell does not create a packet without exact Carlsbad witness") {
    val ctx =
      baseCtx().copy(
        fen = "r1bq1rk1/pp2ppbp/2np2p1/8/2PP4/1P1BPN2/P4PPP/R1BQ1RK1 w - - 0 11",
        playedMove = Some("b3b4"),
        playedSan = Some("b4"),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "minority_attack_fixation_shell",
              planName = "Fix b7 as the minority-attack target",
              subplanId = PlanTaxonomy.PlanKind.MinorityAttackFixation.id,
              executionSteps = List("Advance on the queenside and keep b7 under pressure."),
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "minority_attack_fixation_shell",
              subplanId = PlanTaxonomy.PlanKind.MinorityAttackFixation.id,
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("b7")),
            logicSummary = "The move fixes the queenside target on b7.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("pressure on b7"),
              planAdvancements = List("minority-attack fixation against b7"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = List(
              WeakComplexInfo(
                owner = "Black",
                squareColor = "dark",
                squares = List("b7"),
                isOutpost = false,
                cause = "Minority-attack target on b7"
              )
            ),
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("b3b4", "a7a5", "b4b5"),
                scoreCp = 58,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_b7",
              ownerSide = "white",
              piece = "P",
              from = "b3",
              targetSquare = "b7",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("keep pressure on b7"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("pressure on b7")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      )

    assertEquals(delta, None)

    val bundle = MainPathMoveDeltaClaimBuilder.build(ctx, pack, None)
    assertEquals(bundle.flatMap(_.mainClaim), None)
  }

  test("iqp-inducement stays fail-closed under the reviewed weakness absorb gate") {
    val ctx =
      baseCtx().copy(
        fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13",
        playedMove = Some("d4e6"),
        playedSan = Some("Nxe6"),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "iqp_inducement_shell",
              planName = "Force the structure toward an isolated d-pawn target",
              subplanId = PlanTaxonomy.PlanKind.IQPInducement.id,
              executionSteps = List("Clarify the center and leave Black with a fixed d-pawn target."),
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "iqp_inducement_shell",
              subplanId = PlanTaxonomy.PlanKind.IQPInducement.id,
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("d5")),
            logicSummary = "The move steers the branch toward an isolani target on d5.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("pressure on the d-pawn"),
              planAdvancements = List("induce and attack the isolated d-pawn"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        ),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = List(
              WeakComplexInfo(
                owner = "Black",
                squareColor = "dark",
                squares = List("d5"),
                isOutpost = false,
                cause = "Potential isolated d-pawn target"
              )
            ),
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("d4e6", "f7e6", "a1d1"),
                scoreCp = 60,
                depth = 18
              )
            )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_d5",
              ownerSide = "white",
              piece = "N",
              from = "d4",
              targetSquare = "d5",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("steer the structure toward a fixed d-pawn"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("pressure on the d-pawn")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      )

    assertEquals(delta, None)

    val bundle = MainPathMoveDeltaClaimBuilder.build(ctx, pack, None)
    assertEquals(bundle.flatMap(_.mainClaim), None)
  }

  test("trade-key-defender packet stays blocked without an exact cert owner path") {
    val ctx =
      baseCtx().copy(
        fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13",
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "trade_key_defender_shell",
              planName = "Trade the key defender",
              subplanId = PlanTaxonomy.PlanKind.DefenderTrade.id,
              executionSteps = List("Trade the defender on e6 when it is favorable."),
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "trade_key_defender_shell",
              subplanId = PlanTaxonomy.PlanKind.DefenderTrade.id,
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("e6")),
            logicSummary = "The defender on e6 is overloaded.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("trade the defender on e6"),
              planAdvancements = List("simplify after removing the key defender"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("d4e6", "f7e6", "c2g6"),
                scoreCp = 54,
                depth = 18,
                parsedMoves = List(
                  PvMove("d4e6", "Nxe6", "d4", "e6", "N", isCapture = true, capturedPiece = Some("b"), givesCheck = false),
                  PvMove("f7e6", "fxe6", "f7", "e6", "P", isCapture = true, capturedPiece = Some("n"), givesCheck = false),
                  PvMove("c2g6", "Qg6", "c2", "g6", "Q", isCapture = false, capturedPiece = None, givesCheck = false)
                )
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "N",
              from = "d4",
              target = "e6",
              idea = "trade the key defender on e6",
              tacticalTheme = Some("exchange"),
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_e6",
              ownerSide = "white",
              piece = "N",
              from = "d4",
              targetSquare = "e6",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("trade the key defender on e6"),
              evidence = List("probe")
            )
          )
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).get

    assertEquals(delta.packet.proofFamily, "trade_key_defender")
    assertEquals(delta.packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assert(delta.packet.proofPathWitness.ownerSeedTerms.contains("e6"))
    assert(delta.packet.proofPathWitness.structureTransitionTerms.nonEmpty)
    assert(delta.packet.suppressionReasons.contains(PlayerFacingClaimSuppressionReason.SupportOnlyReinflation))
    assertNotEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
  }

  test("board/PV defender-trade branch materializes a supported-local move owner") {
    val ctx =
      baseCtx().copy(
        fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
        ply = 33,
        playedMove = Some("c1a3"),
        playedSan = Some("Ba3"),
        authorQuestions = defaultQuestions,
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "defender_trade_board_pv_branch",
              planName = "Exchange the defender of a7",
              subplanId = PlanTaxonomy.PlanKind.DefenderTrade.id,
              executionSteps = List("Put the bishop on a3 so Black's bishop must be exchanged."),
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "defender_trade_board_pv_branch",
              subplanId = PlanTaxonomy.PlanKind.DefenderTrade.id,
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("a7")),
            logicSummary = "The exchange removes the bishop defender from the a7 branch.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("exchange the defender guarding a7"),
              planAdvancements = List("remove the defender on a3"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations =
              List(
                VariationLine(
                  moves = List("c1a3", "f8a3", "b3a3", "d7b5"),
                  scoreCp = 44,
                  depth = 18,
                  parsedMoves = List(
                    PvMove("c1a3", "Ba3", "c1", "a3", "B", isCapture = false, capturedPiece = None, givesCheck = false),
                    PvMove("f8a3", "Bxa3", "f8", "a3", "B", isCapture = true, capturedPiece = Some("B"), givesCheck = false),
                    PvMove("b3a3", "Rxa3", "b3", "a3", "R", isCapture = true, capturedPiece = Some("B"), givesCheck = false),
                    PvMove("d7b5", "Bb5", "d7", "b5", "B", isCapture = false, capturedPiece = None, givesCheck = false)
                  )
                )
              )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "B",
              from = "c1",
              target = "a3",
              idea = "exchange the defender guarding a7",
              tacticalTheme = Some("exchange"),
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_a7",
              ownerSide = "white",
              piece = "B",
              from = "c1",
              targetSquare = "a7",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("defender"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("exchange the defender guarding a7")))
        )
      )
    val surface = StrategyPackSurface.from(pack)

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx, surface, None)
        .getOrElse(fail("board/PV defender-trade branch should create move-delta evidence"))
    val packet = delta.packet

    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing)
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.DefenderTradeProofSource)
    assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.DefenderTrade.id)
    assertEquals(packet.scope, PlayerFacingPacketScope.MoveLocal)
    assertEquals(packet.bestDefenseBranchKey, Some("c1a3|f8a3"))
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
    assertEquals(packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assertEquals(packet.suppressionReasons, Nil)
    assertEquals(packet.releaseRisks, Nil)
    assert(packet.proofPathWitness.ownerSeedTerms.contains("defender:f8"), clues(packet))
    assert(packet.proofPathWitness.structureTransitionTerms.contains("defender_removed:f8-a3"), clues(packet))

    val inputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val primary = ranked.primary.getOrElse(fail("board/PV defender trade should admit WhyThis"))

    assert(
      Set(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged).contains(primary.questionKind),
      clues(primary)
    )
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.MoveDelta)
    assertEquals(primary.plannerSource, PlayerFacingTruthModePolicy.DefenderTradeProofSource)
    assertEquals(primary.claim, "This exchange removes a defender on the local branch.")
    assertEquals(primary.prefixKind, PlayerFacingClaimPrefixKind.SupportedLocal)
    assertEquals(primary.contrast, None)
    assertEquals(primary.consequence, None)
    assert(primary.admissibilityReasons.contains("strategic_claim_supported_local"), clues(primary))
  }

  test("board/PV bad-piece liquidation branch materializes a supported-local move owner") {
    val ctx =
      baseCtx().copy(
        fen = "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1",
        ply = 1,
        playedMove = Some("c1a3"),
        playedSan = Some("Ba3"),
        authorQuestions = defaultQuestions,
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "bad_piece_liquidation_board_pv_branch",
              planName = "Trade the bad bishop",
              subplanId = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
              executionSteps = List("Put the bishop on a3 so the blocked bishop is exchanged."),
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "bad_piece_liquidation_board_pv_branch",
              subplanId = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("f8")),
            logicSummary = "The branch trades away White's blocked bishop.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("trade the bad bishop"),
              planAdvancements = List("clear the bad bishop from the local branch"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations =
              List(
                VariationLine(
                  moves = List("c1a3", "e7f7", "a3f8", "f7f8"),
                  scoreCp = 38,
                  depth = 18,
                  parsedMoves = List(
                    PvMove("c1a3", "Ba3", "c1", "a3", "B", isCapture = false, capturedPiece = None, givesCheck = false),
                    PvMove("e7f7", "Kf7", "e7", "f7", "K", isCapture = false, capturedPiece = None, givesCheck = false),
                    PvMove("a3f8", "Bxf8", "a3", "f8", "B", isCapture = true, capturedPiece = Some("B"), givesCheck = false),
                    PvMove("f7f8", "Kxf8", "f7", "f8", "K", isCapture = true, capturedPiece = Some("B"), givesCheck = false)
                  )
                )
              )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "B",
              from = "c1",
              target = "a3",
              idea = "trade the bad bishop",
              tacticalTheme = Some("exchange"),
              evidence = List("bad_piece_liquidation")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("trade the bad bishop")))
        )
      )
    val surface = StrategyPackSurface.from(pack)

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx, surface, None)
        .getOrElse(fail("board/PV bad-piece liquidation branch should create move-delta evidence"))
    val packet = delta.packet

    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing)
    assertEquals(packet.proofSource, PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource)
    assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.BadPieceLiquidation.id)
    assertEquals(packet.scope, PlayerFacingPacketScope.MoveLocal)
    assertEquals(packet.bestDefenseBranchKey, Some("c1a3|e7f7"))
    assertEquals(packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assert(packet.proofPathWitness.ownerSeedTerms.contains("bad_piece:c1"), clues(packet))
    assert(packet.proofPathWitness.structureTransitionTerms.contains("bad_piece_removed:c1-f8"), clues(packet))

    val inputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val primary = ranked.primary.getOrElse(fail("board/PV bad-piece liquidation should admit WhyThis"))

    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.MoveDelta)
    assertEquals(primary.plannerSource, PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource)
    assertEquals(primary.claim, "This trade clears the bad piece from the local branch.")
    assertEquals(primary.prefixKind, PlayerFacingClaimPrefixKind.SupportedLocal)
    assertEquals(primary.contrast, None)
    assertEquals(primary.consequence, None)
    assert(primary.admissibilityReasons.contains("strategic_claim_supported_local"), clues(primary))
  }

  test("bad-piece liquidation is admitted by board/PV facts beyond the original source row") {
    val ctx =
      baseCtx().copy(
        fen = "5b2/p3k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1",
        ply = 1,
        playedMove = Some("c1a3"),
        playedSan = Some("Ba3"),
        authorQuestions = defaultQuestions,
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "bad_piece_liquidation_general_branch",
              planName = "Trade the bad bishop",
              subplanId = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
              executionSteps = List("Put the bishop on a3 so the blocked bishop is exchanged."),
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "bad_piece_liquidation_general_branch",
              subplanId = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("f8")),
            logicSummary = "The branch trades away White's blocked bishop.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("trade the bad bishop"),
              planAdvancements = List("clear the bad bishop from the local branch"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations =
              List(
                VariationLine(
                  moves = List("c1a3", "e7f7", "a3f8", "f7f8"),
                  scoreCp = 38,
                  depth = 18,
                  parsedMoves = List(
                    PvMove("c1a3", "Ba3", "c1", "a3", "B", isCapture = false, capturedPiece = None, givesCheck = false),
                    PvMove("e7f7", "Kf7", "e7", "f7", "K", isCapture = false, capturedPiece = None, givesCheck = false),
                    PvMove("a3f8", "Bxf8", "a3", "f8", "B", isCapture = true, capturedPiece = Some("B"), givesCheck = false),
                    PvMove("f7f8", "Kxf8", "f7", "f8", "K", isCapture = true, capturedPiece = Some("B"), givesCheck = false)
                  )
                )
              )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "B",
              from = "c1",
              target = "a3",
              idea = "trade the bad bishop",
              tacticalTheme = Some("exchange"),
              evidence = List("bad_piece_liquidation")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("trade the bad bishop")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx, StrategyPackSurface.from(pack), None)
        .getOrElse(fail("general bad-piece liquidation branch should create move-delta evidence"))

    assertEquals(delta.packet.proofSource, PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource)
    assertEquals(delta.packet.proofFamily, PlanTaxonomy.PlanKind.BadPieceLiquidation.id)
    assertEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assert(delta.packet.proofPathWitness.ownerSeedTerms.contains("bad_piece:c1"), clues(delta.packet))
    assert(delta.packet.proofPathWitness.structureTransitionTerms.contains("bad_piece_removed:c1-f8"), clues(delta.packet))
  }

  test("bad-piece liquidation rejects the same trade when the piece is not constrained") {
    val ctx =
      baseCtx().copy(
        fen = "5b2/p3k1pp/8/8/8/1R6/P3PPPP/2B3K1 w - - 0 1",
        ply = 1,
        playedMove = Some("c1a3"),
        playedSan = Some("Ba3"),
        authorQuestions = defaultQuestions,
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "bad_piece_liquidation_not_bad",
              planName = "Trade the bishop",
              subplanId = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
              executionSteps = List("Trade the bishop on f8."),
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "bad_piece_liquidation_not_bad",
              subplanId = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("f8")),
            logicSummary = "The branch trades a bishop.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("trade the bishop"),
              planAdvancements = List("exchange the bishop on f8"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations =
              List(
                VariationLine(
                  moves = List("c1a3", "e7f7", "a3f8", "f7f8"),
                  scoreCp = 18,
                  depth = 18,
                  parsedMoves = List(
                    PvMove("c1a3", "Ba3", "c1", "a3", "B", isCapture = false, capturedPiece = None, givesCheck = false),
                    PvMove("e7f7", "Kf7", "e7", "f7", "K", isCapture = false, capturedPiece = None, givesCheck = false),
                    PvMove("a3f8", "Bxf8", "a3", "f8", "B", isCapture = true, capturedPiece = Some("B"), givesCheck = false),
                    PvMove("f7f8", "Kxf8", "f7", "f8", "K", isCapture = true, capturedPiece = Some("B"), givesCheck = false)
                  )
                )
              )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "B",
              from = "c1",
              target = "a3",
              idea = "trade the bishop",
              tacticalTheme = Some("exchange"),
              evidence = List("bad_piece_liquidation")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("trade the bishop")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx, StrategyPackSurface.from(pack), None)

    assert(delta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource), clues(delta))
    assert(delta.forall(_.packet.fallbackMode != PlayerFacingClaimFallbackMode.WeakMain), clues(delta))
  }

  test("bad-piece labels stay owner-closed without actual liquidation branch") {
    val ctx =
      baseCtx().copy(
        fen = "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1",
        ply = 1,
        playedMove = Some("c1d2"),
        playedSan = Some("Bd2"),
        authorQuestions = defaultQuestions,
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "bad_piece_reposition_only",
              planName = "Improve the bad bishop",
              subplanId = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
              executionSteps = List("Move the bishop without trading it."),
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations =
              List(
                VariationLine(
                  moves = List("c1d2", "e7f7"),
                  scoreCp = 20,
                  depth = 18,
                  parsedMoves = List(
                    PvMove("c1d2", "Bd2", "c1", "d2", "B", isCapture = false, capturedPiece = None, givesCheck = false),
                    PvMove("e7f7", "Kf7", "e7", "f7", "K", isCapture = false, capturedPiece = None, givesCheck = false)
                  )
                )
              )
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "B",
              from = "c1",
              target = "d2",
              idea = "improve the bad bishop",
              tacticalTheme = Some("improvement"),
              evidence = List("bad_piece_liquidation")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("improve the bad bishop")))
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx, StrategyPackSurface.from(pack), None)

    assert(delta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource), clues(delta))
    assert(delta.forall(_.packet.fallbackMode != PlayerFacingClaimFallbackMode.WeakMain), clues(delta))
  }

  test("standalone entry-square denial stays owner-closed without a promoted family") {
    val (ctx, pack, inputs, ranked) = standaloneEntrySquareDenialSnapshot()
    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ).getOrElse(fail("standalone entry-square denial should still build a packet"))

    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ResourceRemoval)
    assertEquals(delta.packet.triggerKind, "entry_square_denial")
    assertEquals(delta.packet.proofSource, "resource_removal_delta")
    assertEquals(delta.packet.proofFamily, "resource_removal")
    assertEquals(delta.packet.bestDefenseBranchKey, Some("a2a3|b7b5"))
    assertEquals(delta.packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(delta.packet.persistence, PlayerFacingClaimPersistence.Stable)
    assert(delta.packet.suppressionReasons.contains(PlayerFacingClaimSuppressionReason.ScopeInflation))
    assertEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.Suppress)
    assert(!PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet))

    assertEquals(inputs.mainBundle.flatMap(_.mainClaim), None)
    assertEquals(inputs.mainBundle.flatMap(_.lineScopedClaim), None)
    assert(ranked.primary.isEmpty)
  }
