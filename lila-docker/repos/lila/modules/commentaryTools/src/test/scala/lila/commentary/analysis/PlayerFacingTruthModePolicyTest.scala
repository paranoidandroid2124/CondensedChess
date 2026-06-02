package lila.commentary.analysis

import java.nio.file.{ Files, Paths }

import chess.format.Fen
import munit.FunSuite
import lila.commentary.{ DirectionalTargetReadiness, GameChronicleMoment, NarrativeSignalDigest, RouteSurfaceMode, StrategicIdeaGroup, StrategicIdeaKind, StrategicIdeaReadiness, StrategyDirectionalTarget, StrategyIdeaSignal, StrategyPack, StrategyPieceMoveRef, StrategyPieceRoute, StrategyRelationSupport }
import lila.commentary.model.*
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind, PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.{ EngineEvidence, PvMove, VariationLine }
import lila.commentary.analysis.claim.PlayerFacingClaimPrefixKind
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }

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

  private def position(fen: String): _root_.chess.Position =
    Fen.read(_root_.chess.variant.Standard, Fen.Full(fen)).getOrElse(fail(s"invalid FEN: $fen"))

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
        fen = "4k3/3n4/8/1B6/8/8/3P4/3QK3 w - - 0 1",
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
    val exchangeFen = "r2q1rk1/pp2bppp/2np1n2/2p1p3/2P5/2NP2P1/PP2QPBP/R1B2RK1 w - - 0 10"
    val ctx =
      baseCtx().copy(
        fen = exchangeFen,
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("c6")),
            logicSummary = "The exchange theme is in the air.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("exchange on c6"),
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
              target = "c6",
              idea = "exchange on c6",
              tacticalTheme = Some("exchange"),
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c6",
              ownerSide = "white",
              piece = "B",
              from = "g2",
              targetSquare = "c6",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("exchange on c6"),
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
    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, pack, None),
      PlayerFacingTruthMode.Minimal
    )

    val exchangeCtx =
      ctx.copy(
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("g2c6", "b7c6"),
                scoreCp = 34,
                depth = 18,
                parsedMoves = List(
                  PvMove("g2c6", "Bxc6", "g2", "c6", "B", isCapture = true, capturedPiece = Some("n"), givesCheck = false),
                  PvMove("b7c6", "bxc6", "b7", "c6", "P", isCapture = true, capturedPiece = Some("b"), givesCheck = false)
                )
              )
            )
          )
        )
      )

    val provingOnlyCtx =
      baseCtx().copy(
        fen = exchangeFen,
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("g2c6", "b7c6"),
                scoreCp = 34,
                depth = 18,
                parsedMoves = List(
                  PvMove("g2c6", "Bxc6", "g2", "c6", "B", isCapture = true, capturedPiece = Some("n"), givesCheck = false),
                  PvMove("b7c6", "bxc6", "b7", "c6", "P", isCapture = true, capturedPiece = Some("b"), givesCheck = false)
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
        fen = exchangeFen,
        playedMove = Some("g2c6"),
        playedSan = Some("Bxc6"),
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
        deniedSquares = List("e3"),
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
              targetId = "target_e3",
              ownerSide = "white",
              piece = "N",
              from = "c2",
              targetSquare = "e3",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("control e3"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("control e3")))
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
              citationLine = Some("...Qe3 can no longer enter on e3.")
            )
          )
        )),
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.NarrowChoice,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Attack", None, "independent"),
            whyNot = Some("Black can no longer use e3 as an entry square.")
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        specificCtx,
        StrategyPackSurface.from(pack),
        None
      ).map(_.deltaClass),
      Some(PlayerFacingMoveDeltaClass.CounterplayReduction)
    )

    val replayBackedCtx =
      specificCtx.copy(
        engineEvidence = Some(
          EngineEvidence(
            depth = 16,
            variations = List(VariationLine(List("e2e3", "f6e4"), 15, depth = 16))
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        replayBackedCtx,
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
                moves = List("f3e5", "c6e5", "f4e5"),
                scoreCp = 90,
                depth = 18,
                parsedMoves = List(
                  PvMove("f3e5", "Ne5", "f3", "e5", "N", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("c6e5", "Nxe5", "c6", "e5", "N", isCapture = true, capturedPiece = Some("n"), givesCheck = false),
                  PvMove("f4e5", "fxe5", "f4", "e5", "P", isCapture = true, capturedPiece = Some("n"), givesCheck = false)
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
    assertEquals(delta.packet.bestDefenseBranchKey, Some("f3e5|c6e5"))
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
                moves = List("f3e5", "c6e5", "f4e5"),
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
    assertEquals(delta.packet.bestDefenseBranchKey, Some("f3e5|c6e5"))
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
    assert(primary.evidence.exists(_.branchScoped), clues(primary))
    assert(primary.evidence.exists(_.purposes.contains("planner_line_proof")), clues(primary))
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
                moves = List("f3e5", "c6e5", "f4e5"),
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

  test("Benoni d6 target fixation admits from board and replayed knight-route evidence") {
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
    assertEquals(delta.packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assert(PlayerFacingClaimProof.allowsWeakMainClaim(delta.packet))
    assertEquals(
      delta.packet.proofPathWitness.exactSliceProof,
      Some(PlayerFacingExactSliceProof.ExactTargetFixation("d6"))
    )

    val bundle = MainPathMoveDeltaClaimBuilder.build(ctx, pack, None)
    assertEquals(bundle.flatMap(_.mainClaim).map(_.claimText), Some("This keeps the pressure fixed on d6."))
  }

  test("catalog route target fixation admits reversed Benoni and King's Indian descriptors without source changes") {
    def ctxFor(
        fen: String,
        playedMove: String,
        target: String,
        pvMoves: List[String],
        planName: String
    ): NarrativeContext =
      baseCtx().copy(
        fen = fen,
        playedMove = Some(playedMove),
        playedSan = Some("Nd7"),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = s"route_target_$target",
              planName = planName,
              subplanId = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
              executionSteps = List(s"Use the knight route to keep $target fixed."),
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = s"route_target_$target",
              subplanId = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare(target)),
            logicSummary = s"The route keeps $target fixed.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List(s"pressure on $target"),
              planAdvancements = List(s"route pressure on $target"),
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
                moves = pvMoves,
                scoreCp = 40,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy

    def pack(target: String) =
      Some(
        StrategyPack(
          sideToMove = "black",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = s"target_$target",
              ownerSide = "black",
              piece = "N",
              from = "f6",
              targetSquare = target,
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List(s"route pressure on $target"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some(s"route pressure on $target")))
        )
      )

    val reversedBenoni =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(
          ctxFor(
            fen = "4k3/8/5n2/8/2pp4/3P4/8/4K3 b - - 0 1",
            playedMove = "f6d7",
            target = "d3",
            pvMoves = List("f6d7", "e1e2", "d7c5"),
            planName = "Keep the d3 weakness fixed"
          ),
          StrategyPackSurface.from(pack("d3")),
          None
        )
        .getOrElse(fail("reversed Benoni route should admit a target-fixation witness"))

    val kingsIndian =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(
          ctxFor(
            fen = "4k3/8/5n2/8/8/8/8/4K3 b - - 0 1",
            playedMove = "f6d7",
            target = "c5",
            pvMoves = List("f6d7", "e1e2", "d7c5"),
            planName = "Occupy c5 with the knight route"
          ),
          StrategyPackSurface.from(pack("c5")),
          None
        )
        .getOrElse(fail("King's Indian route should admit a target-fixation witness"))

    assertEquals(
      reversedBenoni.packet.proofPathWitness.exactSliceProof,
      Some(PlayerFacingExactSliceProof.ExactTargetFixation("d3"))
    )
    assert(reversedBenoni.packet.proofPathWitness.structureTransitionTerms.exists(_.contains("reversed_benoni_d3_knight_route_f6")))
    assertEquals(
      kingsIndian.packet.proofPathWitness.exactSliceProof,
      Some(PlayerFacingExactSliceProof.ExactTargetFixation("c5"))
    )
    assert(kingsIndian.packet.proofPathWitness.structureTransitionTerms.exists(_.contains("kings_indian_c5_knight_route")))
  }

  test("dynamic target fixation binds generic weakness targets but not carlsbad-labelled non-c6-c3 targets") {
    val ctx =
      baseCtx().copy(
        fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1",
        playedMove = Some("e1e2"),
        playedSan = Some("Ke2"),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "static_weakness_e5",
              planName = "Fix the isolated e5 pawn",
              subplanId = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
              executionSteps = List("Keep pressure on the isolated e5 pawn."),
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "static_weakness_e5",
              subplanId = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
              themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("e5")),
            logicSummary = "The move keeps the isolated e5 pawn fixed.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("pressure on e5"),
              planAdvancements = List("keep the e5 pawn fixed"),
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
                moves = List("e1e2", "e8e7"),
                scoreCp = 40,
                depth = 18
              )
            )
          )
        )
      ).withTypedEvidenceFromLegacy

    def pack(evidenceRefs: List[String]) =
      Some(
        StrategyPack(
          sideToMove = "white",
          strategicIdeas =
            List(
              StrategyIdeaSignal(
                ideaId = "target_e5",
                ownerSide = "white",
                kind = StrategicIdeaKind.TargetFixing,
                group = StrategicIdeaGroup.StructuralChange,
                readiness = StrategicIdeaReadiness.Ready,
                focusSquares = List("e5"),
                confidence = 0.91,
                evidenceRefs = evidenceRefs
              )
            )
        )
      )

    val genericDelta =
      PlayerFacingTruthModePolicy
        .mainPathMoveDeltaEvidence(
          ctx,
          StrategyPackSurface.from(pack(List("source:plan_match_target_fixing", "source:weak_complex_fixation"))),
          None
        )
        .getOrElse(fail("generic dynamic target should admit exact target fixation"))

    assertEquals(genericDelta.packet.proofSource, PlayerFacingTruthModePolicy.ExactTargetFixationProofSource)
    assertEquals(
      genericDelta.packet.proofPathWitness.exactSliceProof,
      Some(PlayerFacingExactSliceProof.ExactTargetFixation("e5"))
    )

    val carlsbadLabelledDelta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(
          pack(List("source:plan_match_target_fixing", "source:weak_complex_fixation", "source:carlsbad_fixation_profile"))
        ),
        None
      )

    assert(
      carlsbadLabelledDelta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.ExactTargetFixationProofSource),
      clue(carlsbadLabelledDelta)
    )
  }

  test("target-fixation route evidence is catalog-driven rather than Benoni-specific") {
    val source =
      Files.readString(
        Paths.get(
          "modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlayerFacingTruthModePolicy.scala"
        )
      )

    assert(source.contains("OpeningRouteCatalog"), clue(source))
    assert(!source.contains("benoniD6KnightRouteEvidence"), clue(source))
    assert(source.contains("findRouteWitness"), clue(source))
    assert(source.contains("checkRouteEvidence"), clue(source))
    assert(!source.contains("benoniD6SurfaceEvidenceWitness"), clue(source))
    assert(!source.contains("benoniD6TargetFixationWitness"), clue(source))
    assert(!source.contains("\"benoni_d6_knight_route\""), clue(source))
  }

  test("Carlsbad fixed-target board proof accepts advanced minority pawns on b4 and b5") {
    val whiteB2 = position("6k1/8/2p5/3p4/3P4/8/1P6/6K1 w - - 0 1")
    val whiteB4 = position("6k1/8/2p5/3p4/1P1P4/8/8/6K1 w - - 0 1")
    val whiteB5 = position("6k1/8/2p5/1P1p4/3P4/8/8/6K1 w - - 0 1")
    val blackB7 = position("6k1/1p6/8/3p4/3P4/2P5/8/6K1 b - - 0 1")
    val blackB5 = position("6k1/8/8/1p1p4/3P4/2P5/8/6K1 b - - 0 1")
    val blackB4 = position("6k1/8/8/3p4/1p1P4/2P5/8/6K1 b - - 0 1")

    assertEquals(PlayerFacingTruthModePolicy.carlsbadTargetForBoard(whiteB2.board, whiteB2.color), Some("c6"))
    assertEquals(PlayerFacingTruthModePolicy.carlsbadTargetForBoard(whiteB4.board, whiteB4.color), Some("c6"))
    assertEquals(PlayerFacingTruthModePolicy.carlsbadTargetForBoard(whiteB5.board, whiteB5.color), Some("c6"))
    assertEquals(PlayerFacingTruthModePolicy.carlsbadTargetForBoard(blackB7.board, blackB7.color), Some("c3"))
    assertEquals(PlayerFacingTruthModePolicy.carlsbadTargetForBoard(blackB5.board, blackB5.color), Some("c3"))
    assertEquals(PlayerFacingTruthModePolicy.carlsbadTargetForBoard(blackB4.board, blackB4.color), Some("c3"))
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

  test("unproven trade-key-defender text degrades to a replayed exchange-sequence owner") {
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

    assertEquals(delta.packet.proofFamily, PlanTaxonomy.PlanKind.SimplificationWindow.id)
    assertEquals(delta.packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assert(delta.packet.proofPathWitness.ownerSeedTerms.contains("e6"))
    assert(delta.packet.proofPathWitness.structureTransitionTerms.nonEmpty)
    assert(!delta.packet.suppressionReasons.contains(PlayerFacingClaimSuppressionReason.SupportOnlyReinflation))
    assertNotEquals(delta.packet.proofFamily, "trade_key_defender")
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
              planName = "Exchange the defender of c5",
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
            focalPoint = Some(TargetSquare("c5")),
            logicSummary = "The exchange removes the bishop defender from the c5 branch.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("exchange the defender guarding c5"),
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
              idea = "exchange the defender guarding c5",
              tacticalTheme = Some("exchange"),
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c5",
              ownerSide = "white",
              piece = "B",
              from = "c1",
              targetSquare = "c5",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("defender"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("exchange the defender guarding c5")))
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
    assertEquals(
      packet.proofPathWitness.exactSliceProof,
      Some(PlayerFacingExactSliceProof.DefenderTrade("f8", "a3", "c5"))
    )

    val carrierTextOnlyDelta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx.copy(
          mainStrategicPlans = Nil,
          strategicPlanExperiments = Nil
        ),
        surface,
        None
      )

    assert(
      carrierTextOnlyDelta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.DefenderTradeProofSource),
      clues(carrierTextOnlyDelta)
    )
    assert(
      carrierTextOnlyDelta.forall(_.packet.fallbackMode != PlayerFacingClaimFallbackMode.WeakMain),
      clues(carrierTextOnlyDelta)
    )

    val missingPlayedMoveDelta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx.copy(playedMove = None), surface, None)

    assert(
      missingPlayedMoveDelta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.DefenderTradeProofSource),
      clues(missingPlayedMoveDelta)
    )
    assert(
      missingPlayedMoveDelta.forall(_.packet.fallbackMode != PlayerFacingClaimFallbackMode.WeakMain),
      clues(missingPlayedMoveDelta)
    )

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

  test("replacement defender geometry does not re-promote broad defender plans to exact owner") {
    val ctx =
      baseCtx().copy(
        fen = "k7/8/5b2/4b3/8/8/3Q4/1R4K1 w - - 0 1",
        ply = 1,
        playedMove = Some("b1b2"),
        playedSan = Some("Rb2"),
        mainStrategicPlans =
          List(
            evidenceBackedPlan(
              planId = "defender_trade_replacement_line",
              planName = "Trade the defender of c3",
              subplanId = PlanTaxonomy.PlanKind.DefenderTrade.id,
              executionSteps = List("Invite the bishop trade on b2."),
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        strategicPlanExperiments =
          List(
            evidenceBackedExperiment(
              planId = "defender_trade_replacement_line",
              subplanId = PlanTaxonomy.PlanKind.DefenderTrade.id,
              themeL1 = PlanTaxonomy.PlanTheme.FavorableExchange.id
            )
          ),
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("c3")),
            logicSummary = "The line trades one bishop defender but leaves another defender on c3.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("exchange the defender around c3"),
              planAdvancements = List("trade the defender"),
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
                  moves = List("b1b2", "e5b2", "d2b2"),
                  scoreCp = 16,
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
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "R",
              from = "b1",
              target = "b2",
              idea = "trade the defender of c3",
              tacticalTheme = Some("exchange"),
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c3",
              ownerSide = "white",
              piece = "R",
              from = "b1",
              targetSquare = "c3",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("trade the defender"),
              evidence = List("probe")
            )
          )
        )
      )

    val delta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx, StrategyPackSurface.from(pack), None)

    assert(
      delta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.DefenderTradeProofSource),
      clues(delta)
    )
    assert(
      delta.forall(_.packet.proofFamily != PlanTaxonomy.PlanKind.DefenderTrade.id),
      clues(delta)
    )
    assert(
      delta.forall(_.packet.fallbackMode != PlayerFacingClaimFallbackMode.WeakMain),
      clues(delta)
    )
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
    assertEquals(
      packet.proofPathWitness.exactSliceProof,
      Some(PlayerFacingExactSliceProof.BadPieceLiquidation("c1", "f8"))
    )

    val missingPlayedMoveDelta =
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx.copy(playedMove = None), surface, None)

    assert(
      missingPlayedMoveDelta.forall(_.packet.proofSource != PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource),
      clues(missingPlayedMoveDelta)
    )
    assert(
      missingPlayedMoveDelta.forall(_.packet.fallbackMode != PlayerFacingClaimFallbackMode.WeakMain),
      clues(missingPlayedMoveDelta)
    )

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

  test("board/PV relation transformation witnesses materialize typed supported-local relation proof") {
    final case class Scenario(
        relationKind: String,
        fen: String,
        playedMove: String,
        line: List[String],
        focus: List[String],
        target: String,
        support: StrategyRelationSupport,
        proof: PlayerFacingExactSliceProof
    )

    val scenarios =
      List(
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Overload,
          fen = "k7/7p/5n2/3p4/8/8/8/3Q2K1 w - - 0 1",
          playedMove = "d1d3",
          line = List("d1d3", "a8a7"),
          focus = List("f6", "d5", "h7"),
          target = "f6",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Overload,
              focusSquares = List("f6", "d5", "h7"),
              defenderSquare = Some("f6"),
              targetSquares = List("d5", "h7"),
              attackerSquare = Some("d3"),
              targetSquare = Some("f6")
            ),
          proof = PlayerFacingExactSliceProof.Overload("f6", List("d5", "h7"), "d3")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Deflection,
          fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
          playedMove = "c1a3",
          line = List("c1a3", "f8a3"),
          focus = List("g7", "f8", "a3"),
          target = "g7",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Deflection,
              focusSquares = List("g7", "f8", "a3"),
              defenderSquare = Some("f8"),
              targetSquare = Some("g7"),
              attackerSquare = Some("a3")
            ),
          proof = PlayerFacingExactSliceProof.Deflection("f8", "g7", "a3")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
          fen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1",
          playedMove = "d3f4",
          line = List("d3f4", "a8a7"),
          focus = List("b1", "d3", "h7"),
          target = "h7",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
              focusSquares = List("b1", "d3", "h7"),
              attackerSquare = Some("b1"),
              clearedSquare = Some("d3"),
              targetSquare = Some("h7"),
              attackerRole = Some("bishop")
          ),
          proof = PlayerFacingExactSliceProof.DiscoveredAttack("b1", "d3", "h7", "bishop")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
          fen = "4k3/8/8/8/4N3/8/8/4R1K1 w - - 0 1",
          playedMove = "e4f6",
          line = List("e4f6"),
          focus = List("e8", "e1", "f6"),
          target = "e8",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
              focusSquares = List("e8", "e1", "f6"),
              targetSquare = Some("e8"),
              kingSquare = Some("e8"),
              checkerSquares = List("e1", "f6"),
              moverSquare = Some("f6"),
              moverRole = Some("knight")
            ),
          proof = PlayerFacingExactSliceProof.DoubleCheck("e8", List("e1", "f6"), "f6", "knight")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
          fen = "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1",
          playedMove = "e1e8",
          line = List("e1e8"),
          focus = List("g8", "e8"),
          target = "g8",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
              focusSquares = List("g8", "e8"),
              targetSquare = Some("g8"),
              kingSquare = Some("g8"),
              checkerSquares = List("e8"),
              matingMove = Some("e1e8"),
              patternId = Some("back_rank_mate")
            ),
          proof = PlayerFacingExactSliceProof.BackRankMate("g8", List("e8"), "e1e8")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.MateNet,
          fen = "6rk/6pp/7N/8/8/8/8/6K1 w - - 0 1",
          playedMove = "h6f7",
          line = List("h6f7"),
          focus = List("h8", "f7"),
          target = "h8",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.MateNet,
              focusSquares = List("h8", "f7"),
              targetSquare = Some("h8"),
              kingSquare = Some("h8"),
              checkerSquares = List("f7"),
              matingMove = Some("h6f7"),
              patternId = Some("smothered_mate")
            ),
          proof = PlayerFacingExactSliceProof.MateNet("h8", List("f7"), "h6f7", Some("smothered_mate"))
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
          fen = "6k1/6pp/8/6NQ/8/3B4/8/4K3 w - - 0 1",
          playedMove = "d3h7",
          line = List("d3h7"),
          focus = List("h7"),
          target = "h7",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
              focusSquares = List("h7"),
              targetSquare = Some("h7"),
              bishopSquare = Some("h7"),
              entryMove = Some("d3h7"),
              patternId = Some("greek_gift")
            ),
          proof = PlayerFacingExactSliceProof.GreekGift("h7", "h7", "d3h7", "greek_gift")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
          fen = "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1",
          playedMove = "g5g6",
          line = List("g5g6"),
          focus = List("h8"),
          target = "h8",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
              focusSquares = List("h8"),
              targetSquare = Some("h8"),
              kingSquare = Some("h8"),
              trappingMove = Some("g5g6")
            ),
          proof = PlayerFacingExactSliceProof.StalemateTrap("h8", "g5g6")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
          fen = "7k/8/8/8/8/8/8/4Q1K1 w - - 0 1",
          playedMove = "e1e8",
          line = List("e1e8", "h8h7", "e8h5", "h7g8", "h5e8", "g8h7", "e8h5"),
          focus = List("h7", "e8", "h5"),
          target = "h7",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
              focusSquares = List("h7", "e8", "h5"),
              targetSquare = Some("h7"),
              kingSquare = Some("h7"),
              checkingMoves = List("e1e8", "e8h5", "h5e8", "e8h5"),
              cycleMoves = List("e8h5", "h7g8", "h5e8", "g8h7", "e8h5"),
              repeatedPositionPly = Some(7)
            ),
          proof =
            PlayerFacingExactSliceProof.PerpetualCheck(
              kingSquare = "h7",
              checkingMoves = List("e1e8", "e8h5", "h5e8", "e8h5"),
              cycleMoves = List("e8h5", "h7g8", "h5e8", "g8h7", "e8h5"),
              repeatedPositionPly = 7
            )
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Fork,
          fen = "k7/4r3/8/8/3N3q/8/8/6K1 w - - 0 1",
          playedMove = "d4f5",
          line = List("d4f5", "a8b8"),
          focus = List("f5", "h4", "e7"),
          target = "h4",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Fork,
              focusSquares = List("f5", "h4", "e7"),
              targetSquare = Some("h4"),
              targetSquares = List("h4", "e7"),
              targetRoles = List("queen", "rook"),
              attackerSquare = Some("f5"),
              attackerRole = Some("knight")
            ),
          proof =
            PlayerFacingExactSliceProof.Fork(
              attackerSquare = "f5",
              attackerRole = "knight",
              targets =
                List(
                  PlayerFacingExactSliceProof.TargetPiece("h4", "queen"),
                  PlayerFacingExactSliceProof.TargetPiece("e7", "rook")
                )
            )
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.HangingPiece,
          fen = "k7/8/8/5b2/2B5/8/8/6K1 w - - 0 1",
          playedMove = "c4d3",
          line = List("c4d3", "a8b8"),
          focus = List("d3", "f5"),
          target = "f5",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.HangingPiece,
              focusSquares = List("d3", "f5"),
              targetSquare = Some("f5"),
              targetRole = Some("bishop"),
              attackerSquare = Some("d3"),
              attackerRole = Some("bishop")
            ),
          proof = PlayerFacingExactSliceProof.HangingPiece("d3", "f5", "bishop", "bishop")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
          fen = "n3k3/2p5/1p6/8/8/8/4K3/7R w - - 0 1",
          playedMove = "h1a1",
          line = List("h1a1", "e8d8"),
          focus = List("a8", "a1"),
          target = "a8",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
              focusSquares = List("a8", "a1"),
              targetSquare = Some("a8"),
              targetRole = Some("knight"),
              attackerSquares = List("a1"),
              legalEscapeCount = Some(0)
            ),
          proof = PlayerFacingExactSliceProof.TrappedPiece("a8", "knight", List("a1"), 0)
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Domination,
          fen = "n3k3/2p5/8/8/8/8/4K3/7R w - - 0 1",
          playedMove = "h1a1",
          line = List("h1a1", "e8d8"),
          focus = List("a8", "a1"),
          target = "a8",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Domination,
              focusSquares = List("a8", "a1"),
              targetSquare = Some("a8"),
              targetRole = Some("knight"),
              controllerSquare = Some("a1"),
              controllerRole = Some("rook"),
              legalMoveCount = Some(1)
            ),
          proof = PlayerFacingExactSliceProof.Domination("a1", "a8", "rook", "knight", 1)
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
          fen = "6k1/8/8/7r/8/8/8/4Q1K1 w - - 0 1",
          playedMove = "e1e8",
          line = List("e1e8", "g8h7", "e8h5"),
          focus = List("h5", "e8"),
          target = "h5",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
              focusSquares = List("h5", "e8"),
              targetSquare = Some("h5"),
              intermediateMove = Some("e1e8"),
              threatType = Some("check"),
              responseMove = Some("g8h7"),
              payoffMove = Some("e8h5")
            ),
          proof = PlayerFacingExactSliceProof.Zwischenzug("e1e8", "check", "g8h7", "e8h5", "h5")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Decoy,
          fen = "k7/8/8/3q4/5N2/8/4B3/3Q2K1 w - - 0 1",
          playedMove = "f4d3",
          line = List("f4d3", "d5d3", "e2d3"),
          focus = List("f4", "d3", "d5"),
          target = "d3",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Decoy,
              focusSquares = List("f4", "d3", "d5"),
              targetSquare = Some("d3"),
              baitFromSquare = Some("f4"),
              baitSquare = Some("d3"),
              baitRole = Some("knight"),
              luredFromSquare = Some("d5"),
              luredRole = Some("queen"),
              executionFromSquare = Some("e2"),
              executionToSquare = Some("d3")
            ),
          proof = PlayerFacingExactSliceProof.Decoy("f4", "d3", "d5", "e2", "d3", "knight", "queen")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
          fen = "k7/8/6q1/5n2/8/8/8/1B5K w - - 0 1",
          playedMove = "b1e4",
          line = List("b1e4", "a8b8"),
          focus = List("e4", "f5", "g6"),
          target = "g6",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
              focusSquares = List("e4", "f5", "g6"),
              targetSquare = Some("g6"),
              attackerSquare = Some("e4"),
              blockerSquare = Some("f5"),
              attackerRole = Some("bishop"),
              blockerRole = Some("knight"),
              targetRole = Some("queen")
            ),
          proof = PlayerFacingExactSliceProof.XRay("e4", "f5", "g6", "bishop", "knight", "queen")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Clearance,
          fen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1",
          playedMove = "d3f4",
          line = List("d3f4", "a8a7"),
          focus = List("b1", "d3", "h7"),
          target = "h7",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Clearance,
              focusSquares = List("b1", "d3", "h7"),
              targetSquare = Some("h7"),
              beneficiarySquare = Some("b1"),
              beneficiaryRole = Some("bishop"),
              clearedSquare = Some("d3"),
              clearingTo = Some("f4")
            ),
          proof = PlayerFacingExactSliceProof.Clearance("b1", "d3", "h7", "bishop", "f4")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Battery,
          fen = "k7/7p/8/8/8/8/8/1B1Q2K1 w - - 0 1",
          playedMove = "d1d3",
          line = List("d1d3", "a8a7"),
          focus = List("d3", "b1", "h7"),
          target = "h7",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Battery,
              focusSquares = List("d3", "b1", "h7"),
              targetSquare = Some("h7"),
              frontSquare = Some("d3"),
              frontRole = Some("queen"),
              backSquare = Some("b1"),
              backRole = Some("bishop"),
              axis = Some("diagonal")
            ),
          proof = PlayerFacingExactSliceProof.Battery("d3", "b1", "h7", "queen", "bishop", "diagonal")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
          fen = "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1",
          playedMove = "f8b4",
          line = List("f8b4", "e1d1"),
          focus = List("b4", "c3", "e1"),
          target = "c3",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
              focusSquares = List("b4", "c3", "e1"),
              targetSquare = Some("c3"),
              attackerSquare = Some("b4"),
              attackerRole = Some("bishop"),
              pinnedSquare = Some("c3"),
              pinnedRole = Some("knight"),
              behindSquare = Some("e1"),
              behindRole = Some("king"),
              absolutePin = Some(true)
            ),
          proof = PlayerFacingExactSliceProof.Pin("b4", "c3", "e1", "c3", "bishop", "knight", "king", true)
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Skewer,
          fen = "r6k/8/8/8/8/8/7K/4Q2R b - - 0 1",
          playedMove = "a8a1",
          line = List("a8a1", "h2g1"),
          focus = List("a1", "e1", "h1"),
          target = "e1",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Skewer,
              focusSquares = List("a1", "e1", "h1"),
              targetSquare = Some("e1"),
              attackerSquare = Some("a1"),
              attackerRole = Some("rook"),
              frontSquare = Some("e1"),
              frontRole = Some("queen"),
              backSquare = Some("h1"),
              backRole = Some("rook")
            ),
          proof = PlayerFacingExactSliceProof.Skewer("a1", "e1", "h1", "e1", "rook", "queen", "rook")
        ),
        Scenario(
          relationKind = MoveReviewExchangeAnalyzer.RelationKind.Interference,
          fen = "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1",
          playedMove = "f5d6",
          line = List("f5d6", "a8b8"),
          focus = List("d6", "d8", "d5"),
          target = "d5",
          support =
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Interference,
              focusSquares = List("d6", "d8", "d5"),
              targetSquare = Some("d5"),
              blockerSquare = Some("d6"),
              blockerRole = Some("knight"),
              defenderSquare = Some("d8"),
              defenderRole = Some("rook"),
              targetRole = Some("queen")
            ),
          proof = PlayerFacingExactSliceProof.Interference("d6", "d8", "d5", "knight", "rook", "queen")
        )
      )

    scenarios.foreach { scenario =>
      val ctx =
        baseCtx().copy(
          fen = scenario.fen,
          ply = 1,
          playedMove = Some(scenario.playedMove),
          playedSan = Some(scenario.playedMove),
          authorQuestions = defaultQuestions,
          semantic =
            Some(
              SemanticSection(
                structuralWeaknesses =
                  List(
                    WeakComplexInfo(
                      owner = "Black",
                      squareColor = "dark",
                      squares = scenario.focus.filter(_.matches("[a-h][1-8]")),
                      isOutpost = false,
                      cause = s"${scenario.relationKind} target"
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
          engineEvidence =
            Some(
              EngineEvidence(
                depth = 18,
                variations = List(VariationLine(moves = scenario.line, scoreCp = 70, depth = 18))
              )
            )
        )
      val pack =
        Some(
          StrategyPack(
            sideToMove = "white",
            strategicIdeas =
              List(
                StrategyIdeaSignal(
                  ideaId = s"idea_${scenario.relationKind}",
                  ownerSide = "white",
                  kind = StrategicIdeaKind.FavorableTradeOrTransformation,
                  group = StrategicIdeaGroup.InteractionAndTransformation,
                  readiness = StrategicIdeaReadiness.Build,
                  focusSquares = scenario.focus,
                  confidence = 0.72,
                  targetSquare = Some(scenario.target),
                  relationKind = Some(scenario.relationKind),
                  relationFocusSquares = scenario.focus,
                  relationSupport = Some(scenario.support)
                )
              )
          )
        )
      val surface = StrategyPackSurface.from(pack)
      val delta =
        PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx, surface, None)
          .getOrElse(fail(s"${scenario.relationKind} relation proof should create move-delta evidence"))
      val packet = delta.packet

      assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.ExchangeForcing)
      assertEquals(packet.proofSource, ProofSourceId.RelationTransformation.wireKey)
      assertEquals(packet.proofFamily, ProofFamilyId.RelationTransformation.wireKey)
      assertEquals(packet.scope, PlayerFacingPacketScope.MoveLocal)
      assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
      assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
      assertEquals(packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
      assertEquals(packet.proofPathWitness.exactSliceProof, Some(scenario.proof))

      val inputs = QuestionPlannerInputsBuilder.build(ctx, pack, truthContract = None)
      val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
      val primary =
        ranked.primary.getOrElse(fail(s"${scenario.relationKind} relation proof should admit WhyThis"))

      assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.MoveDelta)
      assertEquals(primary.plannerSource, ProofSourceId.RelationTransformation.wireKey)
      assertEquals(primary.claim, "This move creates a concrete tactical relation on the checked line.")
      assertEquals(primary.prefixKind, PlayerFacingClaimPrefixKind.SupportedLocal)
      assertEquals(primary.contrast, None)
      assertEquals(primary.consequence, None)
      assert(primary.admissibilityReasons.contains("strategic_claim_supported_local"), clues(primary))

      val missingTypedCarrier =
        PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
          ctx,
          StrategyPackSurface.from(
            pack.map(pack => pack.copy(strategicIdeas = pack.strategicIdeas.map(_.copy(relationSupport = None))))
          ),
          None
        )

      assert(
        missingTypedCarrier.forall(_.packet.proofSource != ProofSourceId.RelationTransformation.wireKey),
        clues(missingTypedCarrier)
      )
    }
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
    assertEquals(
      delta.packet.proofPathWitness.exactSliceProof,
      Some(PlayerFacingExactSliceProof.BadPieceLiquidation("c1", "f8"))
    )
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

    assertEquals(delta.deltaClass, PlayerFacingMoveDeltaClass.CounterplayReduction)
    assertEquals(delta.packet.triggerKind, "entry_square_denial")
    assertEquals(delta.packet.proofSource, "counterplay_reduction_delta")
    assertEquals(delta.packet.proofFamily, "counterplay_reduction")
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
