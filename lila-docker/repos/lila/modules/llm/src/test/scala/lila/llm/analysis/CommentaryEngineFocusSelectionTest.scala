package lila.llm.analysis

import munit.FunSuite
import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.VariationLine

class CommentaryEngineFocusSelectionTest extends FunSuite:

  private def minimalAnalysisData(
      ply: Int,
      plans: List[PlanMatch] = Nil
  ): ExtendedAnalysisData =
    ExtendedAnalysisData(
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      nature = PositionNature(NatureType.Dynamic, 0.5, 0.5, "Dynamic position"),
      motifs = Nil,
      plans = plans,
      preventedPlans = Nil,
      pieceActivity = Nil,
      structuralWeaknesses = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      prevMove = None,
      ply = ply,
      evalCp = 0,
      isWhiteToMove = true
    )

  private def chronicleMoment(
      ply: Int,
      momentType: String,
      moveClassification: Option[String] = None,
      cpBefore: Int = 0,
      cpAfter: Int = 0,
      narrative: String = "Pressure on b2 became the decisive shift.",
      transitionType: Option[String] = None,
      strategyPack: Option[StrategyPack] = None,
      signalDigest: Option[NarrativeSignalDigest] = None,
      truthPhase: Option[String] = None,
      surfacedMoveOwnsTruth: Boolean = false,
      verifiedPayoffAnchor: Option[String] = None,
      compensationProseAllowed: Boolean = false
  ): GameArcMoment =
    GameArcMoment(
      ply = ply,
      momentType = momentType,
      narrative = narrative,
      analysisData = minimalAnalysisData(ply),
      moveClassification = moveClassification,
      cpBefore = Some(cpBefore),
      cpAfter = Some(cpAfter),
      transitionType = transitionType,
      strategyPack = strategyPack,
      signalDigest = signalDigest,
      truthPhase = truthPhase,
      surfacedMoveOwnsTruth = surfacedMoveOwnsTruth,
      verifiedPayoffAnchor = verifiedPayoffAnchor,
      compensationProseAllowed = compensationProseAllowed
    )

  private def chronicleCtx(): NarrativeContext =
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
      renderMode = NarrativeRenderMode.FullGame
    )

  private def truthContract(
      ownershipRole: TruthOwnershipRole,
      visibilityRole: TruthVisibilityRole,
      surfaceMode: TruthSurfaceMode,
      exemplarRole: Option[TruthExemplarRole] = None,
      truthClass: DecisiveTruthClass = DecisiveTruthClass.Best,
      truthPhase: Option[InvestmentTruthPhase] = None,
      payoffAnchor: Option[String] = None,
      benchmarkProseAllowed: Boolean = false,
      reasonFamily: DecisiveReasonFamily = DecisiveReasonFamily.InvestmentSacrifice,
      failureMode: FailureInterpretationMode = FailureInterpretationMode.NoClearPlan,
      cpLoss: Int = 0,
      swingSeverity: Int = 0,
      benchmarkCriticalMove: Boolean = false
  ): DecisiveTruthContract =
    val resolvedExemplarRole =
      exemplarRole.getOrElse:
        if ownershipRole == TruthOwnershipRole.CommitmentOwner then TruthExemplarRole.VerifiedExemplar
        else TruthExemplarRole.NonExemplar
    DecisiveTruthContract(
      playedMove = Some("d1d5"),
      verifiedBestMove = Some("d1d5"),
      truthClass = truthClass,
      cpLoss = cpLoss,
      swingSeverity = swingSeverity,
      reasonFamily =
        if reasonFamily != DecisiveReasonFamily.InvestmentSacrifice then reasonFamily
        else if ownershipRole == TruthOwnershipRole.ConversionOwner then DecisiveReasonFamily.Conversion
        else if ownershipRole == TruthOwnershipRole.BlunderOwner then DecisiveReasonFamily.TacticalRefutation
        else DecisiveReasonFamily.InvestmentSacrifice,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = surfaceMode == TruthSurfaceMode.InvestmentExplain,
      truthPhase = truthPhase,
      ownershipRole = ownershipRole,
      visibilityRole = visibilityRole,
      surfaceMode = surfaceMode,
      exemplarRole = resolvedExemplarRole,
      surfacedMoveOwnsTruth =
        ownershipRole == TruthOwnershipRole.CommitmentOwner ||
          ownershipRole == TruthOwnershipRole.ConversionOwner ||
          ownershipRole == TruthOwnershipRole.BlunderOwner,
      verifiedPayoffAnchor = payoffAnchor,
      compensationProseAllowed = surfaceMode == TruthSurfaceMode.InvestmentExplain,
      benchmarkProseAllowed = benchmarkProseAllowed,
      investmentTruthChainKey = payoffAnchor.map(anchor => s"white:$anchor"),
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = benchmarkCriticalMove,
      failureMode = failureMode,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  private def truthBoundMoment(
      ply: Int,
      narrative: String,
      contract: DecisiveTruthContract
  ): CommentaryEngine.TruthBoundArcMoment =
    CommentaryEngine.TruthBoundArcMoment(
      moment = chronicleMoment(ply = ply, momentType = "SustainedPressure", narrative = narrative),
      truthFrame = null.asInstanceOf[MoveTruthFrame],
      truthContract = contract
    )

  test("focusMomentOutline keeps essential beats and highest-priority late beats") {
    val outline = NarrativeOutline(
      List(
        OutlineBeat(kind = OutlineBeatKind.MoveHeader, text = "12...Qe7"),
        OutlineBeat(kind = OutlineBeatKind.Context, text = "Context.", focusPriority = 100, fullGameEssential = true),
        OutlineBeat(kind = OutlineBeatKind.DecisionPoint, text = "Decision.", focusPriority = 96, fullGameEssential = true),
        OutlineBeat(kind = OutlineBeatKind.MainMove, text = "Main move.", focusPriority = 92, fullGameEssential = true),
        OutlineBeat(kind = OutlineBeatKind.Alternatives, text = "Alternatives.", focusPriority = 40),
        OutlineBeat(kind = OutlineBeatKind.WrapUp, text = "The position remains dynamically balanced.", focusPriority = 60),
        OutlineBeat(kind = OutlineBeatKind.OpeningTheory, text = "Opening theory.", focusPriority = 82)
      )
    )

    val focused = CommentaryEngine.focusMomentOutline(outline, hasCriticalBranch = true)

    assertEquals(
      focused.beats.map(_.kind),
      List(
        OutlineBeatKind.Context,
        OutlineBeatKind.DecisionPoint,
        OutlineBeatKind.MainMove,
        OutlineBeatKind.OpeningTheory
      )
    )
    assert(!focused.beats.exists(_.text.contains("dynamically balanced")))
  }

  test("applyVisibleCommentabilityPass reselects the next commentable move in the same thread") {
    val contract =
      truthContract(
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.SupportingVisible,
        surfaceMode = TruthSurfaceMode.Neutral
      )
    val bounds =
      List(
        truthBoundMoment(20, "", contract),
        truthBoundMoment(22, "A quiet but commentable step.", contract),
        truthBoundMoment(24, "Another thread stays commentable.", contract)
      )
    val threadRefs =
      Map(
        20 -> ActiveStrategicThreadRef("thread_a", "themeA", "Theme A", "build", "Build"),
        22 -> ActiveStrategicThreadRef("thread_a", "themeA", "Theme A", "build", "Build"),
        24 -> ActiveStrategicThreadRef("thread_b", "themeB", "Theme B", "build", "Build")
      )

    val accepted =
      CommentaryEngine.applyVisibleCommentabilityPass(
        selectedVisiblePlies = List(20, 24),
        bounds = bounds,
        threadRefsByPly = threadRefs
      )

    assertEquals(accepted, Set(22, 24))
  }

  test("focusMomentOutline retains essential and evidence-backed beats") {
    locally {
      val outline = NarrativeOutline(
        List(
          OutlineBeat(kind = OutlineBeatKind.Context, text = "Structure thesis.", focusPriority = 100, fullGameEssential = true),
          OutlineBeat(
            kind = OutlineBeatKind.DecisionPoint,
            text = "The practical alternative Qh5 stays secondary because Black can trade queens.",
            focusPriority = 96,
            fullGameEssential = true
          ),
          OutlineBeat(kind = OutlineBeatKind.MainMove, text = "This move starts the rook transfer.", focusPriority = 92, fullGameEssential = true),
          OutlineBeat(kind = OutlineBeatKind.WrapUp, text = "Practical coda.", focusPriority = 60)
        )
      )

      val focused = CommentaryEngine.focusMomentOutline(outline, hasCriticalBranch = false)
      val decision = focused.beats.find(_.kind == OutlineBeatKind.DecisionPoint).getOrElse(fail("missing decision beat"))
      assert(decision.text.contains("practical alternative Qh5"))
    }

    locally {
      val outline = NarrativeOutline(
        List(
          OutlineBeat(kind = OutlineBeatKind.Context, text = "The Carlsbad structure calls for the minority attack.", focusPriority = 100, fullGameEssential = true),
          OutlineBeat(kind = OutlineBeatKind.MainMove, text = "The rook belongs on the b-file, and this move starts that route immediately.", focusPriority = 92, fullGameEssential = true),
          OutlineBeat(kind = OutlineBeatKind.WrapUp, text = "The position remains dynamically balanced.", focusPriority = 40)
        )
      )

      val focused = CommentaryEngine.focusMomentOutline(outline, hasCriticalBranch = true)
      val mainMove = focused.beats.find(_.kind == OutlineBeatKind.MainMove).getOrElse(fail("missing main-move beat"))
      assert(mainMove.text.contains("rook belongs on the b-file"))
      assert(!focused.beats.exists(_.text == "The position remains dynamically balanced."))
    }

    locally {
      val outline = NarrativeOutline(
        List(
          OutlineBeat(kind = OutlineBeatKind.Context, text = "Context.", focusPriority = 100, fullGameEssential = true),
          OutlineBeat(
            kind = OutlineBeatKind.DecisionPoint,
            text = "After 12...Bf5 13.Nc3 Qa5, Black keeps the cleaner continuation.",
            focusPriority = 96,
            fullGameEssential = true,
            branchScoped = true,
            supportKinds = List(OutlineBeatKind.Evidence)
          ),
          OutlineBeat(
            kind = OutlineBeatKind.Evidence,
            text = "a) 12...Bf5 13.Nc3 Qa5 (+0.4)\nb) 12...Qa5 13.Nc3 Bf5 (+0.1)",
            focusPriority = 70,
            branchScoped = true
          ),
          OutlineBeat(kind = OutlineBeatKind.MainMove, text = "Main move.", focusPriority = 92, fullGameEssential = true),
          OutlineBeat(kind = OutlineBeatKind.OpeningTheory, text = "Opening.", focusPriority = 82),
          OutlineBeat(kind = OutlineBeatKind.WrapUp, text = "Wrap.", focusPriority = 40)
        )
      )

      val focused = CommentaryEngine.focusMomentOutline(outline, hasCriticalBranch = false)
      assert(focused.beats.exists(_.kind == OutlineBeatKind.DecisionPoint))
      assert(focused.beats.exists(_.kind == OutlineBeatKind.Evidence))
    }
  }

  test("whole-game conclusion support keeps one canonical anchor when shift and payoff restate the contest") {
    val strategicThreads =
      List(
        ActiveStrategicThread(
          threadId = "thread_b2",
          side = "white",
          themeKey = "pressure_b2",
          themeLabel = "Pressure on b2",
          summary = "pressure on b2",
          seedPly = 20,
          lastPly = 28,
          representativePlies = List(20, 24),
          opponentCounterplan = None,
          continuityScore = 0.92
        )
      )

    val decisiveMoment =
      chronicleMoment(
        ply = 24,
        momentType = "SustainedPressure",
        moveClassification = Some("Blunder"),
        cpBefore = 0,
        cpAfter = 280,
        narrative = "The turning point came through pressure on b2.",
        transitionType = Some("SustainedPressure")
      )

    val support =
      CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(decisiveMoment),
        strategicThreads = strategicThreads,
        themes = List("pressure on b2"),
        truthContractsByPly =
          Map(
            24 -> truthContract(
              ownershipRole = TruthOwnershipRole.BlunderOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Blunder
            )
          )
      )

    assert(support.mainContest.exists(_.toLowerCase.contains("pressure on b2")))
    assertEquals(support.decisiveShift, None)
    assertEquals(support.payoff, None)
  }

  test("focusMomentOutline prunes unsupported branch claims and meta wrap-up beats") {
    locally {
      val outline = NarrativeOutline(
        List(
          OutlineBeat(kind = OutlineBeatKind.Context, text = "Context.", focusPriority = 100, fullGameEssential = true),
          OutlineBeat(kind = OutlineBeatKind.MainMove, text = "Main move.", focusPriority = 99, fullGameEssential = true),
          OutlineBeat(kind = OutlineBeatKind.OpeningTheory, text = "Opening.", focusPriority = 98, fullGameEssential = true),
          OutlineBeat(
            kind = OutlineBeatKind.DecisionPoint,
            text = "Engine line says Black keeps the cleaner continuation.",
            focusPriority = 96,
            fullGameEssential = true,
            branchScoped = true,
            supportKinds = List(OutlineBeatKind.Evidence)
          ),
          OutlineBeat(
            kind = OutlineBeatKind.Evidence,
            text = "Engine line: cleaner continuation.",
            focusPriority = 70,
            branchScoped = true
          )
        )
      )

      val focused = CommentaryEngine.focusMomentOutline(outline, hasCriticalBranch = true)
      assert(!focused.beats.exists(_.kind == OutlineBeatKind.DecisionPoint))
      assert(!focused.beats.exists(_.kind == OutlineBeatKind.Evidence))
    }

    locally {
      val outline = NarrativeOutline(
        List(
          OutlineBeat(kind = OutlineBeatKind.Context, text = "Context.", focusPriority = 100, fullGameEssential = true),
          OutlineBeat(kind = OutlineBeatKind.MainMove, text = "Main move.", focusPriority = 95, fullGameEssential = true),
          OutlineBeat(
            kind = OutlineBeatKind.WrapUp,
            text = "Idea: Main strategic promotion is pending; latent stack is 1. Kingside Expansion (0.82). Evidence: Current support centers on probe branches.",
            focusPriority = 90,
            conceptIds = List("strategic_distribution_first", "plan_evidence_three_stage")
          ),
          OutlineBeat(kind = OutlineBeatKind.OpeningTheory, text = "Opening.", focusPriority = 82)
        )
      )

      val focused = CommentaryEngine.focusMomentOutline(outline, hasCriticalBranch = false)
      assert(!focused.beats.exists(_.conceptIds.contains("strategic_distribution_first")))
      assert(!focused.beats.exists(_.text.contains("Current support centers on")))
    }
  }

  test("canonical rescue keeps severe internal anchors ahead of softer bridge labels") {
    locally {
      val pgn =
        """[Event "Canonical Rescue"]
          |[Site "?"]
          |[Date "2026.03.25"]
          |[Round "1"]
          |[White "White"]
          |[Black "Black"]
          |[Result "*"]
          |
          |1. e4 e5 2. Nf3 Nc6 *
          |""".stripMargin
      val evals =
        Map(
          1 -> List(VariationLine(moves = List("e7e5"), scoreCp = 950)),
          2 -> List(VariationLine(moves = List("g1f3"), scoreCp = 500)),
          3 -> List(VariationLine(moves = List("b8c6"), scoreCp = 340)),
          4 -> List(VariationLine(moves = List("f1b5"), scoreCp = 320))
        )

      val diagnostic = CommentaryEngine.generateGameArcDiagnostic(pgn = pgn, evals = evals)

      assert(diagnostic.anchorPlies.contains(2), clue(diagnostic))
      assert(
        diagnostic.canonicalTraceMoments.exists(trace =>
          trace.ply == 2 &&
            trace.finalInternal
        ),
        clue(diagnostic.canonicalTraceMoments)
      )
    }

    locally {
      val catastrophicBridge =
        chronicleMoment(
          ply = 81,
          momentType = "AdvantageSwing",
          moveClassification = Some("Blunder"),
          cpBefore = 220,
          cpAfter = -340,
          transitionType = Some("StrategicBridge")
        ).copy(selectionKind = "thread_bridge", selectionLabel = Some("Campaign Bridge"))
      val severeOnlyMoveFailure =
        chronicleMoment(
          ply = 61,
          momentType = "StrategicBridge",
          moveClassification = Some("Mistake"),
          cpBefore = 90,
          cpAfter = -128,
          transitionType = Some("StrategicBridge")
        ).copy(selectionKind = "thread_bridge", selectionLabel = Some("Campaign Bridge"))

      val catastrophicContract =
        truthContract(
          ownershipRole = TruthOwnershipRole.BlunderOwner,
          visibilityRole = TruthVisibilityRole.PrimaryVisible,
          surfaceMode = TruthSurfaceMode.FailureExplain,
          truthClass = DecisiveTruthClass.Blunder,
          reasonFamily = DecisiveReasonFamily.OnlyMoveDefense,
          failureMode = FailureInterpretationMode.OnlyMoveFailure,
          cpLoss = 560,
          swingSeverity = 21,
          benchmarkCriticalMove = true
        )
      val onlyMoveFailureContract =
        truthContract(
          ownershipRole = TruthOwnershipRole.NoneRole,
          visibilityRole = TruthVisibilityRole.Hidden,
          surfaceMode = TruthSurfaceMode.Neutral,
          truthClass = DecisiveTruthClass.Mistake,
          reasonFamily = DecisiveReasonFamily.OnlyMoveDefense,
          failureMode = FailureInterpretationMode.OnlyMoveFailure,
          cpLoss = 218,
          swingSeverity = 48,
          benchmarkCriticalMove = true
        )

      assert(CommentaryEngine.canonicalRescueBridgeCandidate(catastrophicBridge, catastrophicContract))
      assert(CommentaryEngine.canonicalRescueBridgeCandidate(severeOnlyMoveFailure, onlyMoveFailureContract))
    }

    locally {
      val merged =
        CommentaryEngine.mergeCanonicalInternalMoments(
          List(
            KeyMoment(
              ply = 57,
              momentType = "TensionPeak",
              score = 0,
              description = "Soft label",
              cpBefore = 80,
              cpAfter = 80
            ),
            KeyMoment(
              ply = 57,
              momentType = "Blunder",
              score = -300,
              description = "Rescued decisive label",
              cpBefore = 180,
              cpAfter = -340,
              selectionKind = "thread_bridge",
              selectionLabel = Some("Campaign Bridge"),
              selectionReason = Some("Canonical bridge rescue")
            )
          )
        )

      assertEquals(merged.map(_.momentType), List("Blunder"))
      assertEquals(merged.map(_.selectionKind), List("thread_bridge"))
    }
  }

  test("hybrid moment rendering keeps compact structure and optional cited line") {
    locally {
      val ctx =
        chronicleCtx().copy(
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
        )
      val moment = KeyMoment(ply = ctx.ply, momentType = "StrategicBridge", score = 0, description = "Compression check")
      val strategyPack =
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
      val prepared =
        CommentaryEngine.HybridNarrativeParts(
          lead = "This middlegame block near ply 20 is defined by cumulative pressure and move-order accuracy.",
          defaultBridge = "The strategic stack still favors kingside expansion first.",
          criticalBranch = None,
          body = "The strategic stack still favors kingside expansion first. Current support centers on probe branches.",
          primaryPlan = None,
          focusedOutline = NarrativeOutline(
            List(
              OutlineBeat(kind = OutlineBeatKind.Context, text = "White has finished development and can start asking direct questions in the center.", focusPriority = 100, fullGameEssential = true),
              OutlineBeat(kind = OutlineBeatKind.MainMove, text = "Qe2 keeps the e4 push available while covering the c4 pawn.", focusPriority = 96, fullGameEssential = true),
              OutlineBeat(kind = OutlineBeatKind.DecisionPoint, text = "The move chooses coordination first and postpones queenside expansion, because the center still needs one more defender.", focusPriority = 92, fullGameEssential = true),
              OutlineBeat(
                kind = OutlineBeatKind.WrapUp,
                text = "Idea: Main strategic promotion is pending; latent stack is 1. Kingside Expansion (0.82). Evidence: Current support centers on probe branches.",
                focusPriority = 90,
                conceptIds = List("strategic_distribution_first", "plan_evidence_three_stage")
              )
            )
          ),
          phase = "Middlegame",
          tacticalPressure = false,
          cpWhite = Some(20),
          bead = 1
        )

      val (rendered, _) =
        CommentaryEngine.renderHybridMomentNarrative(ctx, moment, strategyPack = strategyPack, prepared = Some(prepared))
      val paragraphs = rendered.split("\n\n").toList.filter(_.trim.nonEmpty)

      assert(paragraphs.size <= 3, clue(rendered))
      assert(!rendered.contains("strategic stack"), clue(rendered))
      assert(!rendered.contains("Current support centers on"), clue(rendered))
      assert(!rendered.contains("This middlegame block near ply 20"), clue(rendered))
      assert(!rendered.contains("This makes exchanges around"), clue(rendered))
      assert(!rendered.contains("This removes a defensive resource"), clue(rendered))
    }

    locally {
      val ctx =
        chronicleCtx().copy(
          header = ContextHeader("Middlegame", "Critical", "NarrowChoice", "High", "ExplainPlan"),
          decision = Some(
            DecisionRationale(
              focalPoint = None,
              logicSummary = "Keeps the center stable -> improves coordination",
              delta = PVDelta(
                resolvedThreats = List("back-rank pressure"),
                newOpportunities = List("e4 push"),
                planAdvancements = List("queen and rook coordination"),
                concessions = Nil
              ),
              confidence = ConfidenceLevel.Probe
            )
          )
        )
      val moment = KeyMoment(ply = ctx.ply, momentType = "CriticalDecision", score = 0, description = "Cited line")
      val strategyPack =
        Some(
          StrategyPack(
            sideToMove = "white",
            pieceRoutes = List(
              StrategyPieceRoute(
                ownerSide = "white",
                piece = "Q",
                from = "d1",
                route = List("d1", "e2", "e4"),
                purpose = "keep the center stable",
                strategicFit = 0.83,
                tacticalSafety = 0.78,
                surfaceConfidence = 0.87,
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
                strategicReasons = List("keep the center stable"),
                evidence = List("probe")
              )
            ),
            signalDigest = Some(NarrativeSignalDigest(decision = Some("keep the center stable")))
          )
        )
      val prepared =
        CommentaryEngine.HybridNarrativeParts(
          lead = "Lead.",
          defaultBridge = "Bridge.",
          criticalBranch = Some("After 12...Bf5 13.Nc3 Qa5, Black keeps the cleaner continuation."),
          body = "Body.",
          primaryPlan = None,
          focusedOutline = NarrativeOutline(
            List(
              OutlineBeat(kind = OutlineBeatKind.MainMove, text = "Qe2 keeps the center stable before any pawn break.", focusPriority = 96, fullGameEssential = true),
              OutlineBeat(kind = OutlineBeatKind.DecisionPoint, text = "The move chooses king safety first and leaves queenside play for later.", focusPriority = 92, fullGameEssential = true),
              OutlineBeat(
                kind = OutlineBeatKind.Evidence,
                text = "a) 12...Bf5 13.Nc3 Qa5 (+0.4)\nb) 12...Qa5 13.Nc3 Bf5 (+0.1)",
                focusPriority = 80,
                branchScoped = true
              )
            )
          ),
          phase = "Middlegame",
          tacticalPressure = true,
          cpWhite = Some(20),
          bead = 2
        )

      val (rendered, _) =
        CommentaryEngine.renderHybridMomentNarrative(ctx, moment, strategyPack = strategyPack, prepared = Some(prepared))
      val sentenceCount = rendered.split("(?<=[.!?])\\s+").count(_.trim.nonEmpty)

      assert(rendered.isEmpty || sentenceCount >= 1, clue(rendered))
      if rendered.nonEmpty then
        assert(!rendered.contains("A concrete line is"), clue(rendered))
        assert(!rendered.contains("One concrete line that keeps the idea in play is"), clue(rendered))
        assert(!rendered.contains("Lead."), clue(rendered))
        assert(!rendered.contains("Bridge."), clue(rendered))
        assert(!rendered.contains("This makes exchanges around"), clue(rendered))
        assert(!rendered.contains("This removes a defensive resource"), clue(rendered))
    }
  }

  test("hybrid narrative parts keep a neutral default bridge without legacy thesis promotion") {
    locally {
      val ctx = BookmakerProseGoldenFixtures.exchangeSacrifice.ctx
      val moment =
        KeyMoment(
          ply = ctx.ply,
          momentType = "SustainedPressure",
          score = 0,
          description = "Compensation bridge"
        )
      val parts = CommentaryEngine.buildHybridNarrativeParts(ctx, moment)
      val bridge = parts.defaultBridge

      assert(bridge.nonEmpty, clue(parts))
      assert(
        bridge.toLowerCase.contains("material can wait") ||
          bridge.toLowerCase.contains("gives up material") ||
          bridge.toLowerCase.contains("winning it back") ||
          bridge.toLowerCase.contains("initiative") ||
          bridge.toLowerCase.contains("attack"),
        clue(bridge)
      )
    }

    locally {
      val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
      val moment =
        KeyMoment(
          ply = ctx.ply,
          momentType = "StrategicBridge",
          score = 0,
          description = "Quiet compensation bridge"
        )
      val parts = CommentaryEngine.buildHybridNarrativeParts(ctx, moment)
      val bridge = parts.defaultBridge

      assert(bridge.nonEmpty, clue(parts))
      assert(
        bridge.toLowerCase.contains("coherent plan") ||
          bridge.toLowerCase.contains("queenside") ||
          bridge.toLowerCase.contains("open file fight"),
        clue(bridge)
      )
      assert(!bridge.toLowerCase.contains("kingside clamp"), clue(bridge))
    }

    locally {
      val ctx =
        chronicleCtx().copy(
          decision = Some(
            DecisionRationale(
              focalPoint = Some(TargetSquare("g7")),
              logicSummary = "keep the kingside pressure coordinated",
              delta = PVDelta(
                resolvedThreats = List("trade into a worse ending"),
                newOpportunities = List("g7"),
                planAdvancements = Nil,
                concessions = Nil
              ),
              confidence = ConfidenceLevel.Probe
            )
          )
        )
      val moment =
        KeyMoment(
          ply = ctx.ply,
          momentType = "StrategicBridge",
          score = 0,
          description = "Weak compensation bridge"
        )
      val parts = CommentaryEngine.buildHybridNarrativeParts(ctx, moment)
      val bridge = parts.defaultBridge

      assert(bridge.nonEmpty, clue(parts))
      assert(!bridge.toLowerCase.contains("material can wait"), clue(bridge))
      assert(!bridge.toLowerCase.contains("winning the material back"), clue(bridge))
    }

    locally {
      val ctx =
        NarrativeContext(
          fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
          header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
          ply = 24,
          summary = NarrativeSummary("Kingside expansion", None, "NarrowChoice", "Maintain", "0.00"),
          threats = ThreatTable(Nil, Nil),
          pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
          plans = PlanTable(
            List(
              PlanRow(
                rank = 1,
                name = "Kingside Expansion",
                score = 0.82,
                evidence = List("space on the kingside"),
                confidence = ConfidenceLevel.Heuristic
              )
            ),
            Nil
          ),
          delta = None,
          phase = PhaseContext("Middlegame", "Balanced middlegame"),
          candidates = Nil,
          mainStrategicPlans = List(
            PlanHypothesis(
              planId = "kingside_expansion",
              planName = "Kingside Expansion",
              rank = 1,
              score = 0.82,
              preconditions = Nil,
              executionSteps = Nil,
              failureModes = Nil,
              viability = PlanViability(0.74, "medium", "slow"),
              themeL1 = "flank_infrastructure"
            )
          ),
          strategicPlanExperiments = List(
            StrategicPlanExperiment(
              planId = "kingside_expansion",
              evidenceTier = "pv_coupled",
              moveOrderSensitive = true
            )
          ),
          renderMode = NarrativeRenderMode.FullGame
        )
      val moment =
        KeyMoment(
          ply = ctx.ply,
          momentType = "StrategicBridge",
          score = 0,
          description = "Conditional bridge"
        )

      val parts = CommentaryEngine.buildHybridNarrativeParts(ctx, moment)
      val bridge = parts.defaultBridge
      assert(bridge.nonEmpty, clue(parts))
      assert(!parts.body.toLowerCase.contains("kingside expansion"), clue(parts.body))
      assert(!bridge.toLowerCase.contains("kingside expansion"), clue(bridge))
    }
  }

  test("selectWholeGamePromotionPly prioritizes decisive hidden turns over visible tension shells") {
    locally {
      val visibleTension =
        chronicleMoment(
          ply = 20,
          momentType = "TensionPeak",
          cpBefore = 10,
          cpAfter = 20,
          narrative = "This middlegame block near move 10 remains balanced but highly tension-sensitive."
        )
      val hiddenBlunder =
        chronicleMoment(
          ply = 36,
          momentType = "AdvantageSwing",
          moveClassification = Some("Blunder"),
          cpBefore = 20,
          cpAfter = 260,
          signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("pressure on b2"))),
          narrative = "Pressure on b2 became the decisive shift."
        )

      val promoted = CommentaryEngine.selectWholeGamePromotionPly(
        internalMoments = List(visibleTension, hiddenBlunder),
        visibleMomentPlies = Set(20)
      )

      assertEquals(promoted, Some(36))
    }

    locally {
      val visibleTension =
        chronicleMoment(
          ply = 24,
          momentType = "TensionPeak",
          cpBefore = 8,
          cpAfter = 18,
          narrative = "The game stayed tense around the center."
        )
      val hiddenInvestment =
        chronicleMoment(
          ply = 55,
          momentType = "InvestmentPivot",
          moveClassification = Some("WinningInvestment"),
          cpBefore = 12,
          cpAfter = 210,
          signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("open-file pressure"))),
          narrative = "Rxd5 changed the game by giving White open-file pressure."
        )

      val promoted = CommentaryEngine.selectWholeGamePromotionPly(
        internalMoments = List(visibleTension, hiddenInvestment),
        visibleMomentPlies = Set(24),
        truthContractsByPly =
          Map(
            55 -> truthContract(
              ownershipRole = TruthOwnershipRole.CommitmentOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.Neutral,
              truthClass = DecisiveTruthClass.WinningInvestment,
              truthPhase = Some(InvestmentTruthPhase.FirstInvestmentCommitment),
              payoffAnchor = Some("open-file pressure")
            )
          )
      )

      assertEquals(promoted, Some(55))
    }
  }

  test("thread-local quiet holds do not displace protected failure and commitment owners from active-note selection") {
    def focusMoment(
        ply: Int,
        momentType: String,
        moveClassification: Option[String] = None,
        strategyPack: Option[StrategyPack] = None
    ) =
      GameChronicleMoment(
        momentId = s"ply_$ply",
        ply = ply,
        moveNumber = (ply + 1) / 2,
        side = if ply % 2 == 1 then "white" else "black",
        moveClassification = moveClassification,
        momentType = momentType,
        fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        narrative = s"moment $ply",
        selectionKind = "key",
        selectionLabel = Some("Key Moment"),
        concepts = Nil,
        variations = Nil,
        cpBefore = 0,
        cpAfter = 0,
        mateBefore = None,
        mateAfter = None,
        wpaSwing = Some(8),
        strategicSalience = Some("High"),
        transitionType = None,
        transitionConfidence = None,
        activePlan = None,
        topEngineMove = None,
        collapse = None,
        strategyPack = strategyPack
      )

    val maintenanceMoments =
      (11 to 17).toList.map { ply =>
        focusMoment(
          ply = ply,
          momentType = "SustainedPressure",
          strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List(s"pressure at $ply")))
        )
      }
    val quietHold =
      focusMoment(
        ply = 19,
        momentType = "TensionPeak",
        strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List("quiet hold shell")))
      )
    val severeFailure = focusMoment(31, "AdvantageSwing", moveClassification = Some("Blunder"))
    val commitmentOwner =
      focusMoment(
        ply = 33,
        momentType = "InvestmentPivot",
        moveClassification = Some("WinningInvestment"),
        strategyPack = Some(StrategyPack(sideToMove = "white", longTermFocus = List("open-file pressure")))
      )

    val selection =
      StrategicBranchSelector.buildSelection(
        maintenanceMoments ++ List(quietHold, severeFailure, commitmentOwner),
        maintenanceMoments.map(_.ply).map(ply =>
          ply -> truthContract(
            ownershipRole = TruthOwnershipRole.MaintenanceEcho,
            visibilityRole = TruthVisibilityRole.SupportingVisible,
            surfaceMode = TruthSurfaceMode.MaintenancePreserve,
            truthPhase = Some(InvestmentTruthPhase.CompensationMaintenance),
            payoffAnchor = Some(s"pressure at $ply")
          )
        ).toMap ++ Map(
          19 -> truthContract(
            ownershipRole = TruthOwnershipRole.NoneRole,
            visibilityRole = TruthVisibilityRole.Hidden,
            surfaceMode = TruthSurfaceMode.Neutral,
            reasonFamily = DecisiveReasonFamily.QuietTechnicalMove,
            benchmarkCriticalMove = true
          ),
          31 -> truthContract(
            ownershipRole = TruthOwnershipRole.BlunderOwner,
            visibilityRole = TruthVisibilityRole.PrimaryVisible,
            surfaceMode = TruthSurfaceMode.FailureExplain,
            truthClass = DecisiveTruthClass.Blunder,
            failureMode = FailureInterpretationMode.TacticalRefutation
          ),
          33 -> truthContract(
            ownershipRole = TruthOwnershipRole.CommitmentOwner,
            visibilityRole = TruthVisibilityRole.PrimaryVisible,
            surfaceMode = TruthSurfaceMode.InvestmentExplain,
            truthClass = DecisiveTruthClass.WinningInvestment,
            truthPhase = Some(InvestmentTruthPhase.FirstInvestmentCommitment),
            payoffAnchor = Some("open-file pressure")
          )
        )
      )

    val notePlies = selection.activeNoteMoments.map(_.ply)
    assert(notePlies.contains(31), clue(selection.activeNoteMoments))
    assert(notePlies.contains(33), clue(selection.activeNoteMoments))
    assert(!notePlies.contains(19), clue(selection.activeNoteMoments))
  }

  test("protected active-note overflow can attach more than eight severe notes without changing visible selection") {
    def focusMoment(
        ply: Int,
        moveClassification: Option[String]
    ) =
      GameChronicleMoment(
        momentId = s"overflow_$ply",
        ply = ply,
        moveNumber = (ply + 1) / 2,
        side = if ply % 2 == 1 then "white" else "black",
        moveClassification = moveClassification,
        momentType = "AdvantageSwing",
        fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        narrative = s"moment $ply",
        selectionKind = "key",
        selectionLabel = Some("Key Moment"),
        concepts = Nil,
        variations = Nil,
        cpBefore = 0,
        cpAfter = -350,
        mateBefore = None,
        mateAfter = None,
        wpaSwing = Some(16),
        strategicSalience = Some("High"),
        transitionType = None,
        transitionConfidence = None,
        activePlan = None,
        topEngineMove = None,
        collapse = None,
        strategyPack = None
      )

    val blunderMoments =
      (11 to 35 by 2).toList.take(13).map(ply => focusMoment(ply, Some("Blunder")))

    val selection =
      StrategicBranchSelector.buildSelection(
        blunderMoments,
        blunderMoments.map(_.ply).map(ply =>
          ply -> truthContract(
            ownershipRole = TruthOwnershipRole.BlunderOwner,
            visibilityRole = TruthVisibilityRole.PrimaryVisible,
            surfaceMode = TruthSurfaceMode.FailureExplain,
            truthClass = DecisiveTruthClass.Blunder,
            failureMode = FailureInterpretationMode.TacticalRefutation
          )
        ).toMap
      )

    assertEquals(selection.selectedMoments.size, 12)
    assertEquals(selection.activeNoteMoments.size, 13)
    assertEquals(selection.activeNoteMoments.map(_.ply).sorted, blunderMoments.map(_.ply).sorted)
  }

  test("buildWholeGameConclusionSupport keeps owning payoff routes ahead of echoes") {
    locally {
      val commitment =
        chronicleMoment(
          ply = 55,
          momentType = "InvestmentPivot",
          moveClassification = Some("WinningInvestment"),
          cpBefore = 12,
          cpAfter = 210,
          narrative = "Rxd5 changed the game by creating open-file pressure.",
          signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("open-file pressure"))),
          truthPhase = Some("FirstInvestmentCommitment"),
          surfacedMoveOwnsTruth = true,
          verifiedPayoffAnchor = Some("open-file pressure"),
          compensationProseAllowed = true
        )
      val maintenance =
        chronicleMoment(
          ply = 59,
          momentType = "SustainedPressure",
          cpBefore = 210,
          cpAfter = 220,
          narrative = "White kept the pressure going.",
          signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("pressure on e6"))),
          truthPhase = Some("CompensationMaintenance"),
          surfacedMoveOwnsTruth = false,
          verifiedPayoffAnchor = Some("pressure on e6"),
          compensationProseAllowed = false
        )

      val support = CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(commitment, maintenance),
        strategicThreads = Nil,
        themes = List("Open-file pressure"),
        truthContractsByPly =
          Map(
            55 -> truthContract(
              ownershipRole = TruthOwnershipRole.CommitmentOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.InvestmentExplain,
              truthClass = DecisiveTruthClass.WinningInvestment,
              truthPhase = Some(InvestmentTruthPhase.FirstInvestmentCommitment),
              payoffAnchor = Some("open-file pressure")
            ),
            59 -> truthContract(
              ownershipRole = TruthOwnershipRole.MaintenanceEcho,
              visibilityRole = TruthVisibilityRole.SupportingVisible,
              surfaceMode = TruthSurfaceMode.MaintenancePreserve,
              truthPhase = Some(InvestmentTruthPhase.CompensationMaintenance),
              payoffAnchor = Some("pressure on e6")
            )
          )
      )

      assertEquals(support.decisiveShift, Some("The decisive shift came through open-file pressure."))
      assertEquals(support.payoff, Some("The conversion route ran through open-file pressure."))
    }

    locally {
      val conversion =
        chronicleMoment(
          ply = 71,
          momentType = "SustainedPressure",
          cpBefore = 120,
          cpAfter = 250,
          narrative = "The position turned into a promotion race.",
          transitionType = Some("PromotionConversion"),
          truthPhase = Some("ConversionFollowthrough"),
          surfacedMoveOwnsTruth = true,
          verifiedPayoffAnchor = Some("a promotion race")
        )

      val support = CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(conversion),
        strategicThreads = Nil,
        themes = List("Passed pawn play"),
        truthContractsByPly =
          Map(
            71 -> truthContract(
              ownershipRole = TruthOwnershipRole.ConversionOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.ConversionExplain,
              truthPhase = Some(InvestmentTruthPhase.ConversionFollowthrough),
              payoffAnchor = Some("a promotion race")
            )
          )
      )

      assertEquals(support.decisiveShift, Some("The position turned into a promotion race."))
      assertEquals(support.payoff, Some("The conversion route ran through a promotion race."))
    }
  }

  test("buildWholeGameConclusionSupport keeps deterministic player-language anchors") {
    locally {
      val decisiveMoment =
        chronicleMoment(
          ply = 36,
          momentType = "AdvantageSwing",
          moveClassification = Some("Blunder"),
          cpBefore = 15,
          cpAfter = 240,
          signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("pressure on b2"))),
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "white",
                longTermFocus = List("pressure on b2")
              )
            ),
          narrative = "Pressure on b2 became the decisive shift."
        )

      val support = CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(decisiveMoment),
        strategicThreads =
          List(
            ActiveStrategicThread(
              threadId = "w1",
              side = "white",
              themeKey = "target_fixing",
              themeLabel = "Target Fixing",
              summary = "White keeps improving squares before cashing in. Core plan: pressure on b2.",
              seedPly = 14,
              lastPly = 36,
              continuityScore = 0.92
            ),
            ActiveStrategicThread(
              threadId = "b1",
              side = "black",
              themeKey = "line_occupation",
              themeLabel = "Line Occupation",
              summary = "Black tries to stay active. Core plan: control of the d-file.",
              seedPly = 18,
              lastPly = 34,
              continuityScore = 0.78
            )
          ),
        themes = List("Queenside pressure"),
        truthContractsByPly =
          Map(
            36 ->
              truthContract(
                ownershipRole = TruthOwnershipRole.BlunderOwner,
                visibilityRole = TruthVisibilityRole.PrimaryVisible,
                surfaceMode = TruthSurfaceMode.FailureExplain,
                truthClass = DecisiveTruthClass.Blunder,
                payoffAnchor = Some("pressure on b2")
              )
          )
      )

      assertEquals(
        support.mainContest,
        Some("White was mainly playing for pressure on b2, while Black was mainly playing for control of the d-file.")
      )
      assertEquals(support.decisiveShift, Some("Pressure on b2 became the decisive shift."))
      assertEquals(support.payoff, Some("The punishment story ran through pressure on b2."))
    }

    locally {
      val decisiveMoment =
        chronicleMoment(
          ply = 44,
          momentType = "AdvantageSwing",
          moveClassification = Some("Blunder"),
          cpBefore = 10,
          cpAfter = 310,
          signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("pressure on e6"))),
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "white",
                directionalTargets =
                  List(
                    StrategyDirectionalTarget(
                      targetId = "target_e6",
                      ownerSide = "white",
                      piece = "Q",
                      from = "d1",
                      targetSquare = "e6",
                      readiness = DirectionalTargetReadiness.Build,
                      strategicReasons = List("pressure on e6"),
                      evidence = List("d1, e6")
                    )
                  ),
                longTermFocus = List("pressure on e6")
              )
            ),
          narrative = "The decisive shift came through d1, e6."
        )

      val support = CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(decisiveMoment),
        strategicThreads = Nil,
        themes = List("Central pressure"),
        truthContractsByPly =
          Map(
            44 ->
              truthContract(
                ownershipRole = TruthOwnershipRole.BlunderOwner,
                visibilityRole = TruthVisibilityRole.PrimaryVisible,
                surfaceMode = TruthSurfaceMode.FailureExplain,
                truthClass = DecisiveTruthClass.Blunder,
                payoffAnchor = Some("pressure on e6")
              )
          )
      )

      assertEquals(support.decisiveShift, Some("The decisive shift came through pressure on e6."))
      assertEquals(support.payoff, Some("The punishment story ran through pressure on e6."))
    }

    locally {
      val decisiveMoment =
        chronicleMoment(
          ply = 52,
          momentType = "AdvantageSwing",
          moveClassification = Some("Blunder"),
          cpBefore = 5,
          cpAfter = 280,
          signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("the kingside"))),
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "black",
                pieceRoutes =
                  List(
                    StrategyPieceRoute(
                      ownerSide = "black",
                      piece = "R",
                      from = "a8",
                      route = List("a8", "g8"),
                      purpose = "the kingside",
                      strategicFit = 0.74,
                      tacticalSafety = 0.71,
                      surfaceConfidence = 0.73,
                      surfaceMode = RouteSurfaceMode.Toward
                    )
                  )
              )
            ),
          narrative = "The punishment story ran through the kingside."
        )

      val support = CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(decisiveMoment),
        strategicThreads = Nil,
        themes = List("Kingside play")
      )

      assertEquals(support.decisiveShift, None)
      assertEquals(support.payoff, None)
    }

    locally {
      val decisiveMoment =
        chronicleMoment(
          ply = 58,
          momentType = "AdvantageSwing",
          moveClassification = Some("Blunder"),
          cpBefore = 18,
          cpAfter = 260,
          narrative = "Qb4 is a solid move that keeps the plan clear.",
          signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("pressure on e6"))),
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "white",
                longTermFocus = List("pressure on e6")
              )
            )
        )

      val support = CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(decisiveMoment),
        strategicThreads = Nil,
        themes = List("Central pressure"),
        truthContractsByPly =
          Map(
            58 ->
              truthContract(
                ownershipRole = TruthOwnershipRole.BlunderOwner,
                visibilityRole = TruthVisibilityRole.PrimaryVisible,
                surfaceMode = TruthSurfaceMode.FailureExplain,
                truthClass = DecisiveTruthClass.Blunder,
                payoffAnchor = Some("pressure on e6")
              )
          )
      )

      assertEquals(support.decisiveShift, Some("The decisive shift came through pressure on e6."))
      assertEquals(support.payoff, Some("The punishment story ran through pressure on e6."))
    }

    locally {
      val decisiveMoment =
        chronicleMoment(
          ply = 62,
          momentType = "AdvantageSwing",
          moveClassification = Some("MissedWin"),
          cpBefore = 30,
          cpAfter = -210,
          narrative = "This move is outside sampled principal lines, and Qf8 is the engine reference for safer conversion.",
          signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("pressure on e6"))),
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "black",
                longTermFocus = List("pressure on e6")
              )
            )
        )

      val support = CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(decisiveMoment),
        strategicThreads = Nil,
        themes = List("Central pressure"),
        truthContractsByPly =
          Map(
            62 ->
              truthContract(
                ownershipRole = TruthOwnershipRole.BlunderOwner,
                visibilityRole = TruthVisibilityRole.PrimaryVisible,
                surfaceMode = TruthSurfaceMode.FailureExplain,
                truthClass = DecisiveTruthClass.MissedWin,
                payoffAnchor = Some("pressure on e6")
              )
          )
      )

      assertEquals(support.decisiveShift, Some("The decisive shift came through pressure on e6."))
      assertEquals(support.payoff, Some("The winning route was pressure on e6."))
    }
  }

  test("buildWholeGameConclusionSupport does not promote support-only carriers into decisive wrappers") {
    val decisiveMoment =
      chronicleMoment(
        ply = 66,
        momentType = "AdvantageSwing",
        moveClassification = Some("Blunder"),
        cpBefore = 18,
        cpAfter = 260,
        narrative = "Qb4 is a solid move that keeps the plan clear.",
        signalDigest = Some(NarrativeSignalDigest(strategicFlow = Some("alignment intent: pressure on e6"))),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              longTermFocus = List("continuity: pressure on e6")
            )
          )
      )

    val support = CommentaryEngine.buildWholeGameConclusionSupport(
      moments = List(decisiveMoment),
      strategicThreads = Nil,
      themes = List("Central pressure"),
      truthContractsByPly =
        Map(
          66 ->
            truthContract(
              ownershipRole = TruthOwnershipRole.BlunderOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Blunder
            )
        )
    )

    assertEquals(support.decisiveShift, None)
    assertEquals(support.payoff, None)
  }

  test("buildWholeGameConclusionSupport keeps structured directional proof admissible without support-carrier promotion") {
    val decisiveMoment =
      chronicleMoment(
        ply = 68,
        momentType = "AdvantageSwing",
        moveClassification = Some("Blunder"),
        cpBefore = 12,
        cpAfter = 295,
        narrative = "Qb4 is a solid move that keeps the plan clear.",
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              directionalTargets =
                List(
                  StrategyDirectionalTarget(
                    targetId = "target_e6",
                    ownerSide = "white",
                    piece = "Q",
                    from = "d1",
                    targetSquare = "e6",
                    readiness = DirectionalTargetReadiness.Build,
                    strategicReasons = List("pressure on e6"),
                    evidence = List("d1", "e6")
                  )
                )
            )
          )
      )

    val support = CommentaryEngine.buildWholeGameConclusionSupport(
      moments = List(decisiveMoment),
      strategicThreads = Nil,
      themes = List("Central pressure"),
      truthContractsByPly =
        Map(
          68 ->
            truthContract(
              ownershipRole = TruthOwnershipRole.BlunderOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Blunder
            )
        )
    )

    assertEquals(support.decisiveShift, Some("The decisive shift came through pressure on e6."))
    assertEquals(support.payoff, Some("The punishment story ran through pressure on e6."))
  }

  test("buildWholeGameConclusionSupport drops helper-leaky whole-game wrappers when the anchor is not release-safe") {
    val decisiveMoment =
      chronicleMoment(
        ply = 36,
        momentType = "AdvantageSwing",
        moveClassification = Some("Blunder"),
        cpBefore = 15,
        cpAfter = 240,
        signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("Maneuver(knight, rerouting)"))),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              longTermFocus = List("Maneuver(knight, rerouting)")
            )
          ),
        narrative = "The turning point came through Maneuver(knight, rerouting)."
      )

    val support = CommentaryEngine.buildWholeGameConclusionSupport(
      moments = List(decisiveMoment),
      strategicThreads = Nil,
      themes = List("Central pressure")
    )

    assertEquals(support.decisiveShift, None)
    assertEquals(support.payoff, None)
  }

  test("buildWholeGameConclusionSupport keeps drawn games free of decisive and punishment wrappers") {
    val strategicThreads =
      List(
        ActiveStrategicThread(
          threadId = "thread_balance",
          side = "white",
          themeKey = "center_tension",
          themeLabel = "Center tension",
          summary = "center tension",
          seedPly = 18,
          lastPly = 42,
          representativePlies = List(18, 30),
          opponentCounterplan = None,
          continuityScore = 0.88
        )
      )

    val drawMoment =
      chronicleMoment(
        ply = 30,
        momentType = "AdvantageSwing",
        moveClassification = Some("Blunder"),
        cpBefore = 0,
        cpAfter = 210,
        narrative = "The turning point came through pressure on d5.",
        transitionType = Some("AdvantageSwing")
      )

    val support =
      CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(drawMoment),
        strategicThreads = strategicThreads,
        themes = List("center tension"),
        result = "1/2-1/2",
        truthContractsByPly =
          Map(
            30 -> truthContract(
              ownershipRole = TruthOwnershipRole.BlunderOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Blunder
            )
          )
      )

    assert(support.mainContest.nonEmpty)
    assertEquals(support.decisiveShift, None)
    assertEquals(support.payoff, None)
  }

  test("buildWholeGameConclusionSupport requires decisive proof before adding decisive wrappers") {
    val strategicThreads =
      List(
        ActiveStrategicThread(
          threadId = "thread_balance",
          side = "white",
          themeKey = "center_tension",
          themeLabel = "Center tension",
          summary = "center tension",
          seedPly = 18,
          lastPly = 46,
          representativePlies = List(18, 34),
          opponentCounterplan = None,
          continuityScore = 0.86
        )
      )

    val resultMoment =
      chronicleMoment(
        ply = 46,
        momentType = "AdvantageSwing",
        moveClassification = Some("Mistake"),
        cpBefore = 60,
        cpAfter = 180,
        narrative = "The decisive shift came through pressure on d5.",
        transitionType = Some("AdvantageSwing")
      )

    val support =
      CommentaryEngine.buildWholeGameConclusionSupport(
        moments = List(resultMoment),
        strategicThreads = strategicThreads,
        themes = List("center tension"),
        result = "1-0",
        truthContractsByPly = Map.empty
      )

    assert(support.mainContest.nonEmpty)
    assertEquals(support.decisiveShift, None)
    assertEquals(support.payoff, None)
  }

  test("selectWholeGamePromotionPly ignores raw conversion fallback when the truth contract is neutral") {
    val neutralConversionLikeMoment =
      chronicleMoment(
        ply = 44,
        momentType = "TensionPeak",
        transitionType = Some("ExchangeConversion"),
        narrative = "The move converted smoothly."
      )
    val commitmentMoment =
      chronicleMoment(
        ply = 52,
        momentType = "TensionPeak",
        narrative = "Pressure on e6 made the commitment concrete."
      )

    val promoted =
      CommentaryEngine.selectWholeGamePromotionPly(
        internalMoments = List(neutralConversionLikeMoment, commitmentMoment),
        visibleMomentPlies = Set.empty,
        truthContractsByPly =
          Map(
            44 ->
              truthContract(
                ownershipRole = TruthOwnershipRole.NoneRole,
                visibilityRole = TruthVisibilityRole.Hidden,
                surfaceMode = TruthSurfaceMode.Neutral
              ),
            52 ->
              truthContract(
                ownershipRole = TruthOwnershipRole.CommitmentOwner,
                visibilityRole = TruthVisibilityRole.PrimaryVisible,
                surfaceMode = TruthSurfaceMode.InvestmentExplain,
                truthClass = DecisiveTruthClass.CompensatedInvestment,
                truthPhase = Some(InvestmentTruthPhase.FirstInvestmentCommitment),
                payoffAnchor = Some("pressure on e6")
              )
          )
      )

    assertEquals(promoted, Some(52))
  }
