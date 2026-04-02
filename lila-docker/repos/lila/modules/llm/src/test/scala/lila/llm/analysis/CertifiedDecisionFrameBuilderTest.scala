package lila.llm.analysis

import munit.FunSuite

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PlanContinuity

class CertifiedDecisionFrameBuilderTest extends FunSuite:

  private def plan(id: String, name: String) =
    PlanHypothesis(
      planId = id,
      planName = name,
      rank = 1,
      score = 0.82,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(score = 0.82, label = "high", risk = "stable"),
      evidenceSources = List("probe-backed"),
      themeL1 = "kingside_attack"
    )

  private def narrativeCtx =
    NarrativeContext(
      fen = "2r2rk1/pp3pp1/2pq1n1p/3p4/3P4/1QP1PNRP/P4PP1/2R3K1 w - - 0 22",
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 43,
      playedMove = Some("c1c3"),
      playedSan = Some("Rc3"),
      summary = NarrativeSummary("Kingside Pressure", None, "NarrowChoice", "Maintain", "+0.20"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "Kingside Pressure",
            score = 0.82,
            evidence = List("probe-backed")
          )
        ),
        suppressed = Nil
      ),
      delta = None,
      planContinuity = Some(
        PlanContinuity(
          planName = "Kingside Pressure",
          planId = Some("kingside_attack"),
          consecutivePlies = 2,
          startingPly = 41
        )
      ),
      phase = PhaseContext("Middlegame", "Representative middlegame"),
      candidates = List(
        CandidateInfo(
          move = "Rc3",
          annotation = "!",
          planAlignment = "Kingside Pressure",
          practicalDifficulty = "complex",
          tacticalAlert = None,
          whyNot = None
        )
      ),
      mainStrategicPlans = List(plan("kingside_attack", "Kingside Pressure")),
      strategicPlanExperiments = List(
        StrategicPlanExperiment(
          planId = "kingside_attack",
          evidenceTier = "evidence_backed",
          bestReplyStable = true,
          futureSnapshotAligned = true
        )
      ),
      renderMode = NarrativeRenderMode.Bookmaker
    )

  private def strategyPack(
      routePurpose: String = "kingside pressure",
      routeSquares: List[String] = List("c3", "g3"),
      targetSquare: String = "g7",
      focusZone: Option[String] = Some("kingside"),
      focusSquares: List[String] = List("g7", "h7"),
      moveIdea: String = "kingside pressure",
      targetReasons: List[String] = List("kingside pressure", "mating net"),
      longTermFocus: List[String] = List("kingside pressure around g7")
  ) =
    Some(
      StrategyPack(
        sideToMove = "white",
        strategicIdeas = List(
          StrategyIdeaSignal(
            ideaId = "idea_attack",
            ownerSide = "white",
            kind = StrategicIdeaKind.KingAttackBuildUp,
            group = StrategicIdeaGroup.InteractionAndTransformation,
            readiness = StrategicIdeaReadiness.Build,
            focusSquares = focusSquares,
            focusZone = focusZone,
            beneficiaryPieces = List("Q", "R"),
            confidence = 0.92
          )
        ),
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = routeSquares.headOption.getOrElse(""),
            route = routeSquares,
            purpose = routePurpose,
            strategicFit = 0.88,
            tacticalSafety = 0.8,
            surfaceConfidence = 0.84,
            surfaceMode = RouteSurfaceMode.Exact,
            evidence = List("probe-route")
          )
        ),
        pieceMoveRefs = List(
          StrategyPieceMoveRef(
            ownerSide = "white",
            piece = "Q",
            from = "d1",
            target = "h5",
            idea = moveIdea,
            evidence = List("probe-move")
          )
        ),
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "target_g7",
            ownerSide = "white",
            piece = "Q",
            from = "d1",
            targetSquare = targetSquare,
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = targetReasons,
            evidence = List("probe-target")
          )
        ),
        longTermFocus = longTermFocus,
        signalDigest = Some(
          NarrativeSignalDigest(
            dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
            dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
            dominantIdeaFocus = Some(focusSquares.mkString(", "))
          )
        )
      )
    )

  private def bookmakerBundle(
      claimText: String = "This advances the plan toward g3.",
      anchorTerms: List[String] = List("g7", "kingside pressure"),
      evidenceLines: List[String] = List("Line: a) Rg3.")
  ) =
    MainPathClaimBundle(
      mainClaim = Some(
        MainPathScopedClaim(
          scope = PlayerFacingClaimScope.MoveLocal,
          mode = PlayerFacingTruthMode.Strategic,
          deltaClass = Some(PlayerFacingMoveDeltaClass.PlanAdvance),
          claimText = claimText,
          anchorTerms = anchorTerms,
          evidenceLines = evidenceLines,
          sourceKind = "strategic_delta",
          tacticalOwnership = None
        )
      ),
      lineScopedClaim = None
    )

  private def moment(
      moveClassification: Option[String] = None,
      pack: Option[StrategyPack] = strategyPack(),
      experiments: List[StrategicPlanExperiment] = List(
        StrategicPlanExperiment(
          planId = "kingside_attack",
          evidenceTier = "evidence_backed",
          bestReplyStable = true,
          futureSnapshotAligned = true
        )
      )
  ) =
    GameChronicleMoment(
      momentId = "ply_43",
      ply = 43,
      moveNumber = 22,
      side = "white",
      moveClassification = moveClassification,
      momentType = "SustainedPressure",
      fen = narrativeCtx.fen,
      narrative = "Narrative",
      concepts = List("pressure"),
      variations = Nil,
      cpBefore = 0,
      cpAfter = 0,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = None,
      strategicSalience = Some("High"),
      transitionType = None,
      transitionConfidence = None,
      activePlan = Some(ActivePlanRef("Kingside Pressure", Some("rook_lift_scaffold"), Some("Build"), Some(0.82))),
      topEngineMove = None,
      collapse = None,
      strategyPack = pack,
      signalDigest = Some(
        NarrativeSignalDigest(
          decision = Some("Rook lift toward g3 and pressure on g7."),
          strategicFlow = Some("Keep the kingside pressure rolling."),
          counterplayScoreDrop = Some(70)
        )
      ),
      mainStrategicPlans = List(plan("kingside_attack", "Kingside Pressure")),
      strategicPlanExperiments = experiments
    )

  private def deltaBundle =
    PlayerFacingMoveDeltaBundle(
      claims =
        List(
          PlayerFacingMoveDeltaClaim(
            deltaClass = PlayerFacingMoveDeltaClass.PressureIncrease,
            anchorText = "g7",
            reasonText = Some("This move increases pressure on g7."),
            sourceKind = "delta"
          )
        ),
      visibleRouteRefs =
        List(
          ActiveStrategicRouteRef(
            routeId = "route_1",
            ownerSide = "white",
            piece = "R",
            route = List("c3", "g3"),
            purpose = "kingside pressure",
            strategicFit = 0.86,
            tacticalSafety = 0.8,
            surfaceConfidence = 0.83,
            surfaceMode = RouteSurfaceMode.Exact
          ),
          ActiveStrategicRouteRef(
            routeId = "route_2",
            ownerSide = "black",
            piece = "R",
            route = List("a8", "b8", "b4"),
            purpose = "queenside pressure",
            strategicFit = 0.8,
            tacticalSafety = 0.76,
            surfaceConfidence = 0.79,
            surfaceMode = RouteSurfaceMode.Toward
          )
        ),
      visibleMoveRefs =
        List(
          ActiveStrategicMoveRef(
            label = "Engine preference",
            source = "top_engine_move",
            uci = "c3g3",
            san = Some("Rg3")
          )
        ),
      visibleDirectionalTargets =
        List(
          StrategyDirectionalTarget(
            targetId = "target_g7",
            ownerSide = "white",
            piece = "Q",
            from = "d1",
            targetSquare = "g7",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("kingside pressure"),
            evidence = List("probe-target")
          ),
          StrategyDirectionalTarget(
            targetId = "target_b2",
            ownerSide = "black",
            piece = "Q",
            from = "d8",
            targetSquare = "b2",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("queenside pressure"),
            evidence = List("probe-target")
          )
        ),
      tacticalLead = None,
      tacticalEvidence = None
    )

  private val dossier =
    Some(
      ActiveBranchDossier(
        dominantLens = "pressureincrease",
        chosenBranchLabel = "pressure increase -> g7",
        whyChosen = Some("This move increases pressure on g7."),
        routeCue = Some(
          ActiveBranchRouteCue(
            routeId = "route_1",
            ownerSide = "white",
            piece = "R",
            route = List("c3", "g3"),
            purpose = "kingside pressure",
            strategicFit = 0.86,
            tacticalSafety = 0.8,
            surfaceConfidence = 0.83,
            surfaceMode = RouteSurfaceMode.Exact
          )
        ),
        evidenceCue = Some("Pressure on g7 is the point.")
      )
    )

  test("builder certifies intent and battlefront only from concrete probe-backed carriers") {
    val pack = strategyPack()
    val frame =
      CertifiedDecisionFrameBuilder.build(
        ctx = narrativeCtx,
        strategyPack = pack,
        truthContract = None,
        mainBundle = Some(bookmakerBundle())
      )

    assertEquals(frame.intent.map(_.sentence), Some("White is playing for pressure on g7."), clues(frame))
    assert(frame.battlefront.exists(_.sentence.contains("kingside")), clue(frame))
    assertEquals(
      frame.orderedSupports(2),
      List("White is playing for pressure on g7.", frame.battlefront.get.sentence),
      clues(frame)
    )
  }

  test("builder refuses intent and battlefront when support is only deferred") {
    val deferredCtx =
      narrativeCtx.copy(
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "kingside_attack",
            evidenceTier = "deferred"
          )
        )
      )

    val pack = strategyPack()
    val frame =
      CertifiedDecisionFrameBuilder.build(
        ctx = deferredCtx,
        strategyPack = pack,
        truthContract = None,
        mainBundle = Some(bookmakerBundle())
      )

    assertEquals(frame.intent, None, clues(frame))
    assertEquals(frame.battlefront, None, clues(frame))
  }

  test("builder refuses bookmaker support carriers that do not align with the admitted move claim") {
    val conflictingPack =
      strategyPack(
        routePurpose = "queenside pressure",
        targetSquare = "b2",
        focusZone = Some("queenside"),
        focusSquares = List("b2", "c2"),
        moveIdea = "queenside pressure",
        targetReasons = List("queenside pressure", "pawn target on b2"),
        longTermFocus = List("queenside pressure around b2")
      )
    val frame =
      CertifiedDecisionFrameBuilder.build(
        ctx = narrativeCtx,
        strategyPack = conflictingPack,
        truthContract = None,
        mainBundle =
          Some(
            bookmakerBundle(
              claimText = "This keeps the rook flexible.",
              anchorTerms = List("h-file"),
              evidenceLines = Nil
            )
          )
      )

    assertEquals(frame.intent, None, clues(frame))
    assertEquals(frame.battlefront, None, clues(frame))
  }

  test("builder certifies intent when the carrier aligns with a probe-backed main plan") {
    val frame =
      CertifiedDecisionFrameBuilder.build(
        ctx =
          narrativeCtx.copy(
            mainStrategicPlans =
              List(
                plan("kingside_attack", "Kingside Pressure").copy(
                  executionSteps = List("Keep the pressure on g7.")
                )
              )
          ),
        strategyPack = strategyPack(),
        truthContract = None,
        mainBundle =
          Some(
            bookmakerBundle(
              claimText = "This keeps the rook flexible.",
              anchorTerms = List("h-file"),
              evidenceLines = Nil
            )
          )
      )

    assertEquals(frame.intent.map(_.sentence), Some("White is playing for pressure on g7."), clues(frame))
  }

  test("builder refuses raw plan labels as a standalone decision-frame origin") {
    val frame =
      CertifiedDecisionFrameBuilder.build(
        ctx = narrativeCtx,
        strategyPack = strategyPack(),
        truthContract = None,
        mainBundle =
          Some(
            bookmakerBundle(
              claimText = "This keeps the rook flexible.",
              anchorTerms = List("h-file"),
              evidenceLines = Nil
            )
          )
      )

    assertEquals(frame.intent, None, clues(frame))
  }

  test("builder derives urgency tiers from truth mode and forcing or tension signals") {
    val immediate =
      CertifiedDecisionFrameBuilder.build(
        moment(moveClassification = Some("blunder"), pack = None, experiments = Nil),
        PlayerFacingMoveDeltaBundle(Nil, Nil, Nil, Nil, Some("This is a blunder, and the tactical point has to come first."), None),
        dossier = None
      )
    val pressing =
      CertifiedDecisionFrameBuilder.build(
        ctx = narrativeCtx.copy(header = narrativeCtx.header.copy(choiceType = "OnlyMove")),
        strategyPack = strategyPack(),
        truthContract = None,
        mainBundle = Some(bookmakerBundle())
      )
    val slowCtx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "kingside_attack",
              evidenceTier = "evidence_backed",
              bestReplyStable = true,
              futureSnapshotAligned = true
            )
          )
      )
    val slowBundle = MainPathMoveDeltaClaimBuilder.build(slowCtx, strategyPack(), truthContract = None)
    val slow =
      CertifiedDecisionFrameBuilder.build(
        ctx = slowCtx,
        strategyPack = strategyPack(),
        truthContract = None,
        mainBundle = slowBundle
      )

    assertEquals(
      immediate.urgency.map(_.sentence),
      Some("This is immediate: the tactical point matters right now."),
      clues(immediate)
    )
    assertEquals(
      pressing.urgency.map(_.sentence),
      Some("The timing matters now, and there is not much room to drift."),
      clues(pressing)
    )
    assertEquals(
      slow.urgency.map(_.sentence),
      Some("This is slower, so improving the setup matters more than forcing play right away."),
      clues(slow)
    )
  }

  test("battlefront refuses support clusters that lack a move-local or probe-backed anchor") {
    val zoneOnlyPack =
      strategyPack(
        routePurpose = "queenside pressure",
        routeSquares = List("a1", "a3"),
        targetSquare = "b2",
        focusZone = Some("kingside"),
        focusSquares = List("b2"),
        moveIdea = "quiet regrouping",
        targetReasons = List("kingside pressure"),
        longTermFocus = List("kingside pressure")
      )
    val supportOnlyDeltaBundle =
      deltaBundle.copy(
        claims = Nil,
        visibleDirectionalTargets = Nil
      )
    val genericMoment =
      moment(
        pack = zoneOnlyPack,
        experiments =
          List(
            StrategicPlanExperiment(
              planId = "initiative",
              evidenceTier = "evidence_backed",
              bestReplyStable = true,
              futureSnapshotAligned = true
            )
          )
      ).copy(
        mainStrategicPlans = List(plan("initiative", "Initiative"))
      )

    val frame = CertifiedDecisionFrameBuilder.build(genericMoment, supportOnlyDeltaBundle, dossier = None)

    assertEquals(frame.battlefront, None, clues(frame))
  }

  test("builder refuses generic shell support text as a certification source") {
    val shellPack =
      strategyPack(
        routePurpose = "A likely follow-up is kingside pressure.",
        routeSquares = List("a1", "a3"),
        targetSquare = "b2",
        focusZone = None,
        focusSquares = Nil,
        moveIdea = "The key idea is kingside pressure.",
        targetReasons = List("The key idea is kingside pressure."),
        longTermFocus = List("Further probe work still targets kingside pressure.")
      )
    val frame =
      CertifiedDecisionFrameBuilder.build(
        ctx =
          narrativeCtx.copy(
            mainStrategicPlans =
              List(
                plan("kingside_attack", "Kingside Pressure").copy(
                  executionSteps = List("Keep the pressure on g7.")
                )
              )
          ),
        strategyPack = shellPack,
        truthContract = None,
        mainBundle =
          Some(
            bookmakerBundle(
              claimText = "This keeps the rook flexible.",
              anchorTerms = List("kingside pressure"),
              evidenceLines = Nil
            )
          )
      )

    assertEquals(frame.intent, None, clues(frame))
    assertEquals(frame.battlefront, None, clues(frame))
  }

  test("battlefront accepts a probe-backed plan anchor when the theater is admitted and independently supported") {
    val zoneOnlyPack =
      strategyPack(
        routePurpose = "queenside pressure",
        routeSquares = List("a1", "a3"),
        targetSquare = "b2",
        focusZone = Some("kingside"),
        focusSquares = List("b2"),
        moveIdea = "quiet regrouping",
        targetReasons = List("kingside pressure"),
        longTermFocus = List("kingside pressure")
      )
    val supportOnlyDeltaBundle =
      deltaBundle.copy(
        claims = Nil,
        visibleDirectionalTargets = Nil
      )
    val anchoredMoment =
      moment(pack = zoneOnlyPack).copy(
        mainStrategicPlans =
          List(
            plan("kingside_attack", "Kingside Pressure").copy(
              executionSteps = List("Keep the kingside pressure growing.")
            )
          )
      )

    val frame = CertifiedDecisionFrameBuilder.build(anchoredMoment, supportOnlyDeltaBundle, dossier = None)

    assert(frame.battlefront.exists(_.sentence.contains("kingside")), clue(frame))
  }

  test("active alignment keeps only carriers that match the certified frame") {
    val frame = CertifiedDecisionFrameBuilder.build(moment(), deltaBundle, dossier)

    assertEquals(
      frame.alignedRouteRefs(deltaBundle.visibleRouteRefs).map(_.routeId),
      List("route_1"),
      clues(frame)
    )
    assertEquals(
      frame.alignedTargets(deltaBundle.visibleDirectionalTargets).map(_.targetId),
      List("target_g7"),
      clues(frame)
    )
    assert(frame.alignedDossier(dossier).nonEmpty, clue(frame))
  }

  test("certified frame exports active idea refs from intent and battlefront only") {
    val frame = CertifiedDecisionFrameBuilder.build(moment(), deltaBundle, dossier)

    val ideaRefs = frame.ideaRefs()

    assertEquals(ideaRefs.map(_.kind), List("intent", "battlefront"), clues(frame, ideaRefs))
    assertEquals(ideaRefs.map(_.ownerSide).distinct, List("white"), clues(frame, ideaRefs))
    assertEquals(ideaRefs.map(_.group).distinct, List("decision_frame"), clues(frame, ideaRefs))
    assertEquals(ideaRefs.map(_.readiness).distinct, List("certified"), clues(frame, ideaRefs))
    assert(ideaRefs.exists(_.focusSummary == "pressure on g7"), clues(frame, ideaRefs))
    assert(ideaRefs.exists(_.focusSummary == "around g7"), clues(frame, ideaRefs))
  }

  test("urgency-only frames do not export active idea refs") {
    val frame =
      CertifiedDecisionFrame(
        urgency =
          Some(
            CertifiedDecisionSupport(
              axis = CertifiedDecisionFrameAxis.Urgency,
              sentence = "The timing matters now, and there is not much room to drift.",
              priority = 70,
              sourceKind = "tension_signal"
            )
          ),
        ownerSide = Some("white")
      )

    assertEquals(frame.ideaRefs(), Nil, clue(frame))
  }

  test("active alignment omits carriers when the frame is not certified") {
    val uncertified =
      CertifiedDecisionFrameBuilder.build(
        moment(
          pack = strategyPack(),
          experiments = List(StrategicPlanExperiment(planId = "kingside_attack", evidenceTier = "pv_coupled"))
        ),
        deltaBundle,
        dossier
      )

    assertEquals(uncertified.alignedRouteRefs(deltaBundle.visibleRouteRefs), Nil, clues(uncertified))
    assertEquals(uncertified.alignedTargets(deltaBundle.visibleDirectionalTargets), Nil, clues(uncertified))
    assertEquals(uncertified.alignedDossier(dossier), None, clues(uncertified))
  }
