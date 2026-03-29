package lila.llm.analysis

import munit.FunSuite

import lila.llm.*
import lila.llm.model.StrategicPlanExperiment
import lila.llm.model.authoring.{ PlanHypothesis, PlanViability }

class ActiveStrategicCoachingBriefBuilderTest extends FunSuite:

  private val strategyPack = Some(
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_attack_g7",
          ownerSide = "white",
          kind = StrategicIdeaKind.KingAttackBuildUp,
          group = StrategicIdeaGroup.InteractionAndTransformation,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("g7", "h7"),
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.91
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "R",
          from = "c3",
          route = List("c3", "g3"),
          purpose = "kingside pressure",
          strategicFit = 0.88,
          tacticalSafety = 0.81,
          surfaceConfidence = 0.84,
          surfaceMode = RouteSurfaceMode.Exact,
          evidence = List("probe-route", "pressure on g7")
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "white",
          piece = "Q",
          from = "d1",
          target = "h5",
          idea = "pressure on g7",
          evidence = List("probe-move")
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
          strategicReasons = List("pressure on g7", "mating net"),
          evidence = List("probe-target")
        )
      ),
      longTermFocus = List("keep pressure on g7"),
      signalDigest = Some(
        NarrativeSignalDigest(
          dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
          dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("g7, h7")
        )
      )
    )
  )

  private val routeRef =
    ActiveStrategicRouteRef(
      routeId = "route_1",
      ownerSide = "white",
      piece = "R",
      route = List("c3", "g3"),
      purpose = "kingside pressure",
      strategicFit = 0.88,
      tacticalSafety = 0.81,
      surfaceConfidence = 0.84,
      surfaceMode = RouteSurfaceMode.Exact
    )

  private val moveRef =
    ActiveStrategicMoveRef(
      label = "Engine preference",
      source = "top_engine_move",
      uci = "c3g3",
      san = Some("Rg3")
    )

  private val target =
    StrategyDirectionalTarget(
      targetId = "target_g7",
      ownerSide = "white",
      piece = "Q",
      from = "d1",
      targetSquare = "g7",
      readiness = DirectionalTargetReadiness.Build,
      strategicReasons = List("pressure on g7"),
      evidence = List("probe-target")
    )

  private val deltaBundle =
    PlayerFacingMoveDeltaBundle(
      claims =
        List(
          PlayerFacingMoveDeltaClaim(
            deltaClass = PlayerFacingMoveDeltaClass.PressureIncrease,
            anchorText = "g7",
            reasonText = Some("This move increases pressure on g7."),
            routeCue = None,
            moveCue = None,
            directionalTargets = List(target),
            evidenceLines = List("14...Rc8 15.Re1 Qc7"),
            sourceKind = "strategic_delta"
          )
        ),
      visibleRouteRefs = List(routeRef),
      visibleMoveRefs = List(moveRef),
      visibleDirectionalTargets = List(target),
      tacticalLead = None,
      tacticalEvidence = None
    )

  private val dossier =
    Some(
      ActiveBranchDossier(
        dominantLens = "pressureincrease",
        chosenBranchLabel = "pressure increase -> g7",
        whyChosen = Some("This move increases pressure on g7."),
        whyDeferred = Some("If White drifts, Black can untangle and challenge the g-file."),
        opponentResource = Some("Black is trying to untangle and hit the g-file first."),
        practicalRisk = Some("If White drifts, Black can untangle and challenge the g-file."),
        routeCue = Some(
          ActiveBranchRouteCue(
            routeId = routeRef.routeId,
            ownerSide = routeRef.ownerSide,
            piece = routeRef.piece,
            route = routeRef.route,
            purpose = routeRef.purpose,
            strategicFit = routeRef.strategicFit,
            tacticalSafety = routeRef.tacticalSafety,
            surfaceConfidence = routeRef.surfaceConfidence,
            surfaceMode = routeRef.surfaceMode
          )
        ),
        evidenceCue = Some("Pressure on g7 is the point.")
      )
    )

  private def plan(name: String = "Kingside Pressure"): PlanHypothesis =
    PlanHypothesis(
      planId = "kingside_attack",
      planName = name,
      rank = 1,
      score = 0.84,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(score = 0.84, label = "high", risk = "stable"),
      evidenceSources = List("probe"),
      themeL1 = "king_attack",
      subplanId = None
    )

  private def authorQuestion(
      id: String,
      kind: String,
      priority: Int = 100,
      why: Option[String] = None
  ) =
    AuthorQuestionSummary(
      id = id,
      kind = kind,
      priority = priority,
      question = s"placeholder-$id",
      why = why,
      anchors = List("g7"),
      confidence = "Heuristic"
    )

  private def authorEvidence(
      questionId: String,
      kind: String,
      purpose: String,
      line: String
  ) =
    AuthorEvidenceSummary(
      questionId = questionId,
      questionKind = kind,
      question = s"placeholder-$questionId",
      status = "ready",
      purposes = List(purpose),
      branchCount = 1,
      branches = List(EvidenceBranchSummary(keyMove = "line_1", line = line, evalCp = Some(36))),
      pendingProbeIds = Nil,
      pendingProbeCount = 0,
      probeObjectives = Nil,
      linkedPlans = List("kingside_attack")
    )

  private def moment(
      authorQuestions: List[AuthorQuestionSummary],
      authorEvidence: List[AuthorEvidenceSummary] = Nil,
      topEngineMove: Option[EngineAlternative] = None,
      signalDigest: NarrativeSignalDigest =
        NarrativeSignalDigest(
          decisionComparison =
            Some(
              DecisionComparisonDigest(
                chosenMove = Some("Rg3"),
                engineBestMove = Some("Qe2"),
                engineBestScoreCp = Some(22),
                cpLossVsChosen = Some(8),
                deferredMove = Some("Qe2"),
                deferredReason = Some("it stays quieter and gives Black time to regroup"),
                deferredSource = Some("verified_best"),
                evidence = Some("14...Rc8 15.Re1 Qc7"),
                practicalAlternative = false,
                chosenMatchesBest = false
              )
            ),
          deploymentOwnerSide = Some("white"),
          deploymentPiece = Some("R"),
          deploymentRoute = List("c3", "g3"),
          deploymentPurpose = Some("kingside pressure"),
          deploymentContribution = Some("Pressure on g7 is the point."),
          deploymentSurfaceMode = Some("exact"),
          counterplayScoreDrop = Some(90),
          strategicFlow = Some("Keep the kingside pressure rolling."),
          opponentPlan = Some("queenside counterplay")
        )
  ): GameChronicleMoment =
    GameChronicleMoment(
      momentId = "ply_43_active",
      ply = 43,
      moveNumber = 22,
      side = "white",
      moveClassification = None,
      momentType = "SustainedPressure",
      fen = "r2q1rk1/pp3pp1/2n1pn1p/2pp4/3P3P/2P1PR2/PPQ2PP1/2KR4 w - - 0 22",
      narrative = "Narrative",
      concepts = List("pressure"),
      variations = Nil,
      cpBefore = 20,
      cpAfter = 34,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = None,
      strategicSalience = Some("High"),
      transitionType = None,
      transitionConfidence = None,
      activePlan = Some(ActivePlanRef("Kingside Pressure", Some("kingside_attack"), Some("Build"), Some(0.84))),
      topEngineMove = topEngineMove,
      collapse = None,
      strategyPack = strategyPack,
      signalDigest = Some(signalDigest),
      authorQuestions = authorQuestions,
      authorEvidence = authorEvidence,
      mainStrategicPlans = List(plan()),
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

  private def decisionFrame(moment: GameChronicleMoment): CertifiedDecisionFrame =
    CertifiedDecisionFrameBuilder.build(moment, deltaBundle, dossier)

  test("planner-first active note uses the primary claim and keeps side surfaces outside the note body") {
    val activeMoment =
      moment(
        authorQuestions = List(authorQuestion("q_why_this", "WhyThis")),
        authorEvidence = List(authorEvidence("q_why_this", "WhyThis", "reply_multipv", "14...Rc8 15.Re1 Qc7"))
      )
    val frame = decisionFrame(activeMoment)
    val selection =
      ActiveStrategicCoachingBriefBuilder
        .selectPlannerSurface(activeMoment, deltaBundle, dossier, frame)
        .getOrElse(fail("expected planner selection"))

    assertEquals(selection.primary.questionKind, lila.llm.model.authoring.AuthorQuestionKind.WhyThis)

    val note =
      ActiveStrategicCoachingBriefBuilder
        .buildDeterministicNote(selection, activeMoment)
        .getOrElse(fail("expected deterministic note"))

    assert(note.contains("This move increases pressure on g7."), clue(note))
    assert(!note.contains("The real fight is on the kingside"), clue(note))

    val visible =
      ActiveStrategicCoachingBriefBuilder.visibleSideSurfaces(selection, frame, deltaBundle, dossier)

    assert(visible.ideaRefs.nonEmpty, clue(visible))
    assert(visible.routeRefs.nonEmpty, clue(visible))
    assert(visible.moveRefs.nonEmpty, clue(visible))
    assert(visible.directionalTargets.nonEmpty, clue(visible))
  }

  test("WhatMustBeStopped note stays short and keeps only defensive dossier support") {
    val activeMoment =
      moment(
        authorQuestions =
          List(
            authorQuestion(
              "q_stop",
              "WhatMustBeStopped",
              why = Some("Black is threatening to untangle and hit the g-file.")
            )
          ),
        signalDigest =
          NarrativeSignalDigest(
            prophylaxisThreat = Some("g-file counterplay"),
            prophylaxisPlan = Some("...g5 break"),
            counterplayScoreDrop = Some(140),
            opponentPlan = Some("g-file counterplay"),
            deploymentContribution = Some("Pressure on g7 is the point."),
            deploymentSurfaceMode = Some("exact")
          )
      )
    val frame = decisionFrame(activeMoment)
    val selection =
      ActiveStrategicCoachingBriefBuilder
        .selectPlannerSurface(activeMoment, deltaBundle, dossier, frame)
        .getOrElse(fail("expected defensive planner selection"))

    assertEquals(selection.primary.questionKind, lila.llm.model.authoring.AuthorQuestionKind.WhatMustBeStopped)

    val note =
      ActiveStrategicCoachingBriefBuilder
        .buildDeterministicNote(selection, activeMoment)
        .getOrElse(fail("expected defensive note"))

    assert(note.toLowerCase.contains("stop") || note.toLowerCase.contains("prevent"), clue(note))

    val visible =
      ActiveStrategicCoachingBriefBuilder.visibleSideSurfaces(selection, frame, deltaBundle, dossier)

    assertEquals(visible.routeRefs, Nil)
    assertEquals(visible.moveRefs, Nil)
    assert(visible.dossier.nonEmpty, clue(visible))
  }

  test("Active replay can recover WhyNow from top-engine timing loss without generic urgency") {
    val activeMoment =
      moment(
        authorQuestions = List(authorQuestion("q_now", "WhyNow")),
        authorEvidence = List(authorEvidence("q_now", "WhyNow", "reply_multipv", "14...Rc8 15.Re1 Qc7")),
        topEngineMove =
          Some(
            EngineAlternative(
              uci = "d1e2",
              san = Some("Qe2"),
              cpAfterAlt = Some(22),
              cpLossVsPlayed = Some(84),
              pv = List("d1e2", "c6b4", "g3g7", "f8e8")
            )
          ),
        signalDigest =
          NarrativeSignalDigest(
            decisionComparison =
              Some(
                DecisionComparisonDigest(
                  chosenMove = Some("Rg3"),
                  engineBestMove = Some("Qe2"),
                  engineBestScoreCp = Some(22),
                  cpLossVsChosen = Some(8),
                  deferredMove = None,
                  deferredReason = None,
                  deferredSource = Some("verified_best"),
                  evidence = None,
                  practicalAlternative = false,
                  chosenMatchesBest = false
                )
              ),
            deploymentContribution = Some("Pressure on g7 is the point."),
            deploymentSurfaceMode = Some("exact"),
            counterplayScoreDrop = Some(90),
            strategicFlow = Some("Keep the kingside pressure rolling."),
            opponentPlan = Some("queenside counterplay")
          )
      )
    val frame = CertifiedDecisionFrameBuilder.build(activeMoment, deltaBundle, None)
    val selection =
      ActiveStrategicCoachingBriefBuilder
        .selectPlannerSurface(activeMoment, deltaBundle, None, frame)
        .getOrElse(fail("expected timing planner selection"))

    assertEquals(selection.primary.questionKind, lila.llm.model.authoring.AuthorQuestionKind.WhyNow)

      val note =
        ActiveStrategicCoachingBriefBuilder
          .buildDeterministicNote(selection, activeMoment)
          .getOrElse(fail("expected timing note"))

      assert(note.contains("Qe2") || note.toLowerCase.contains("84cp"), clue(note))
      assert(note.toLowerCase.contains("now"), clue(note))
      assert(note.toLowerCase.contains("if delayed") || note.split("[.!?]").count(_.trim.nonEmpty) >= 2, clue(note))
    }

  test("WhyNow active note skips duplicate contrast and keeps the next anchored support") {
    val activeMoment =
      moment(
        authorQuestions = List(authorQuestion("q_now_dup", "WhyNow")),
        authorEvidence =
          List(
            authorEvidence(
              "q_now_dup",
              "WhyNow",
              "reply_multipv",
              "a) Further probe work still targets Maintain color complex clamp and restrict counterplay through Bd4 and Ba5."
            )
          )
      )
    val selection =
      ActiveStrategicCoachingBriefBuilder.PlannerSurfaceSelection(
        primary =
          QuestionPlan(
            questionId = "q_now_dup",
            questionKind = lila.llm.model.authoring.AuthorQuestionKind.WhyNow,
            priority = 100,
            claim = "The timing matters now because other moves allow the position to slip away.",
            evidence =
              Some(
                QuestionPlanEvidence(
                  text = "a) Further probe work still targets Maintain color complex clamp and restrict counterplay through Bd4 and Ba5.",
                  purposes = List("planner_line_proof"),
                  sourceKinds = List("timing_proof"),
                  branchScoped = true
                )
              ),
            contrast = Some("Other moves allow the position to slip away."),
            consequence = None,
            fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
            strengthTier = QuestionPlanStrengthTier.Strong,
            sourceKinds = List("decision_comparison"),
            admissibilityReasons = List("timing_reason"),
            ownerFamily = OwnerFamily.DecisionTiming,
            ownerSource = "decision_comparison"
          ),
        secondary = None,
        inputs =
          QuestionPlannerInputs(
            mainBundle = None,
            quietIntent = None,
            decisionFrame = decisionFrame(activeMoment),
            decisionComparison = None,
            alternativeNarrative = None,
            truthMode = PlayerFacingTruthMode.Strategic,
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
      )

    val debug = ActiveStrategicCoachingBriefBuilder.debugDeterministicNote(selection, activeMoment)
    val note = debug.result.getOrElse(fail(s"expected deterministic note, got $debug"))

    assertEquals(debug.selectedSupportSource, Some("evidence"))
    assert(debug.supportCandidates.exists(candidate =>
      candidate.source == "contrast" && candidate.droppedReasons.contains("duplicate_claim")
    ), clue(debug))
    assert(note.contains("Bd4 and Ba5"), clue(note))
    assertEquals(note.split("[.!?]").count(_.trim.nonEmpty), 2, clue(note))
  }

  test("WhosePlanIsFaster never survives as the active note owner") {
    val activeMoment =
      moment(
        authorQuestions = List(authorQuestion("q_race", "WhosePlanIsFaster")),
        signalDigest =
          NarrativeSignalDigest(
            counterplayScoreDrop = Some(150),
            opponentPlan = Some("queenside counterplay"),
            deploymentContribution = Some("Pressure on g7 is the point."),
            deploymentSurfaceMode = Some("exact")
          )
      )
    val frame = decisionFrame(activeMoment)

    val selection =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(activeMoment, deltaBundle, dossier, frame)

    assertEquals(selection, None)
  }

  test("deterministic note stays omitted when no planner-approved primary survives") {
    val activeMoment =
      moment(
        authorQuestions = Nil,
        signalDigest =
          NarrativeSignalDigest(
            deploymentContribution = Some("Pressure on g7 is the point."),
            deploymentSurfaceMode = Some("exact")
          )
      )
    val frame = decisionFrame(activeMoment)

    val note =
      ActiveStrategicCoachingBriefBuilder.buildDeterministicNote(
        moment = activeMoment,
        deltaBundle = deltaBundle,
        dossier = dossier,
        decisionFrame = Some(frame)
      )

    assertEquals(note, None)
  }

  test("strict compensation fallback stays compact and avoids generic coaching families") {
    val compensationPack = Some(
      StrategyPack(
        sideToMove = "black",
        pieceMoveRefs = List(
          StrategyPieceMoveRef(
            ownerSide = "black",
            piece = "Q",
            from = "d8",
            target = "b6",
            idea = "fix the queenside targets",
            evidence = List("target_pawn")
          )
        ),
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "target_b2",
            ownerSide = "black",
            piece = "Q",
            from = "d8",
            targetSquare = "b2",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("fixed target"),
            evidence = List("target_pawn")
          )
        ),
        strategicIdeas = List(
          StrategyIdeaSignal(
            ideaId = "idea_qs_targets",
            ownerSide = "black",
            kind = StrategicIdeaKind.TargetFixing,
            group = StrategicIdeaGroup.StructuralChange,
            readiness = StrategicIdeaReadiness.Build,
            focusSquares = List("b2"),
            focusFiles = List("b"),
            focusZone = Some("queenside"),
            beneficiaryPieces = List("Q"),
            confidence = 0.84
          )
        ),
        longTermFocus = List("fix the queenside targets before recovering the pawn"),
        signalDigest = Some(
          NarrativeSignalDigest(
            compensation = Some("return vector through line pressure and delayed recovery"),
            compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
            investedMaterial = Some(100),
            dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
            dominantIdeaGroup = Some(StrategicIdeaGroup.StructuralChange),
            dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
            dominantIdeaFocus = Some("b2")
          )
        )
      )
    )

    val note =
      ActiveStrategicCoachingBriefBuilder.buildStrictCompensationFallbackNote(
        strategyPack = compensationPack,
        dossier = None,
        routeRefs = Nil,
        moveRefs = Nil
      )

    assert(note.exists(_.toLowerCase.contains("compensation comes from")), clue(note))
    assert(note.exists(_.toLowerCase.contains("anchored on b2")), clue(note))
    assert(!note.exists(_.contains("The key idea is")), clue(note))
    assert(!note.exists(_.contains("A likely follow-up is")), clue(note))
    assert(!note.exists(_.contains("A concrete target is")), clue(note))
  }
