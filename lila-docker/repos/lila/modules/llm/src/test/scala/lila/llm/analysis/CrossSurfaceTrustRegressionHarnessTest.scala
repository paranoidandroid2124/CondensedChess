package lila.llm.analysis

import munit.FunSuite

import lila.llm.*
import lila.llm.analysis.practical.ContrastiveSupportAdmissibility
import lila.llm.analysis.render.QuietStrategicSupportComposer
import lila.llm.model.*
import lila.llm.model.authoring.*
class CrossSurfaceTrustRegressionHarnessTest extends FunSuite:

  private enum SurfaceMode:
    case PlannerOwned
    case ExactFactualFallback
    case ExactFactualFallbackWithSupport
    case Omitted

  private enum ActiveExpectation:
    case MustMatch
    case MayOmitOrMatch
    case MustOmit

  private final case class BookChronicleScene(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack] = None,
      truthContract: Option[DecisiveTruthContract] = None,
      expectedFallbackClaim: Option[String] = None
  )

  private final case class ActiveScene(
      moment: GameChronicleMoment,
      deltaBundle: PlayerFacingMoveDeltaBundle,
      dossier: Option[ActiveBranchDossier],
      truthContract: Option[DecisiveTruthContract] = None
  )

  private final case class FixtureExpectation(
      bookmakerOwner: Option[(AuthorQuestionKind, OwnerFamily)],
      chronicleOwner: Option[(AuthorQuestionKind, OwnerFamily)],
      bookmakerMode: SurfaceMode,
      chronicleMode: SurfaceMode,
      activeExpectation: ActiveExpectation,
      activeOwner: Option[(AuthorQuestionKind, OwnerFamily)] = None,
      forbidGeneralizationLeak: Boolean = true,
      forbidFallbackRewrite: Boolean = true,
      forbiddenFragments: List[String] = Nil,
      bookmakerMustLiftQuietSupport: Boolean = false,
      chronicleMustBlockQuietSupport: Boolean = false
  )

  private final case class HarnessFixture(
      id: String,
      scene: BookChronicleScene,
      active: ActiveScene,
      expectation: FixtureExpectation
  )

  private final case class BookmakerObservation(
      owner: Option[(AuthorQuestionKind, OwnerFamily)],
      mode: SurfaceMode,
      claim: String,
      prose: String,
      quietSupportLifted: Boolean,
      supportText: Option[String],
      slots: BookmakerPolishSlots
  )

  private final case class ChronicleObservation(
      owner: Option[(AuthorQuestionKind, OwnerFamily)],
      mode: SurfaceMode,
      narrative: Option[String],
      quietSupportApplied: Boolean,
      quietSupportRejectReasons: List[String]
  )

  private final case class ActiveObservation(
      owner: Option[(AuthorQuestionKind, OwnerFamily)],
      mode: SurfaceMode,
      note: Option[String]
  )

  private final case class FixtureResult(
      fixture: HarnessFixture,
      bookmaker: BookmakerObservation,
      chronicle: ChronicleObservation,
      active: ActiveObservation
  )

  private val emptyParts =
    CommentaryEngine.HybridNarrativeParts(
      lead = "Lead",
      defaultBridge = "Bridge",
      criticalBranch = None,
      body = "Body",
      primaryPlan = None,
      focusedOutline = NarrativeOutline(beats = Nil),
      phase = "Middlegame",
      tacticalPressure = false,
      cpWhite = Some(20),
      bead = 1
    )

  private val GeneralizationMarkers =
    List(
      "shared lesson:",
      "across these branches,",
      "common pattern:",
      "all cited branches revolve around",
      "the recurring practical theme across these games is",
      "these precedent lines point to one key driver:"
    )

  private def plannerRuntimeFixture(id: String): BookmakerProseGoldenFixtures.PlannerRuntimeFixture =
    BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.id == id)
      .getOrElse(fail(s"missing planner runtime fixture: $id"))

  private val activeStrategyPack = Some(
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
          strategicReasons = List("pressure on g7"),
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

  private val activeRouteRef =
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

  private val activeMoveRef =
    ActiveStrategicMoveRef(
      label = "Engine preference",
      source = "top_engine_move",
      uci = "c3g3",
      san = Some("Rg3")
    )

  private val activeTarget =
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

  private val activeDeltaBundle =
    PlayerFacingMoveDeltaBundle(
      claims =
        List(
          PlayerFacingMoveDeltaClaim(
            deltaClass = PlayerFacingMoveDeltaClass.PressureIncrease,
            anchorText = "g7",
            reasonText = Some("This move increases pressure on g7."),
            routeCue = None,
            moveCue = None,
            directionalTargets = List(activeTarget),
            evidenceLines = List("14...Rc8 15.Re1 Qc7"),
            sourceKind = "strategic_delta"
          )
        ),
      visibleRouteRefs = List(activeRouteRef),
      visibleMoveRefs = List(activeMoveRef),
      visibleDirectionalTargets = List(activeTarget),
      tacticalLead = None,
      tacticalEvidence = None
    )

  private val activeDossier =
    Some(
      ActiveBranchDossier(
        dominantLens = "pressureincrease",
        chosenBranchLabel = "pressure increase -> g7",
        whyChosen = Some("This move increases pressure on g7."),
        whyDeferred = Some("If White drifts, Black can untangle and challenge the g-file."),
        opponentResource = Some("Black is trying to untangle and hit the g-file first."),
        practicalRisk = Some("If White drifts, Black can untangle and challenge the g-file."),
        routeCue =
          Some(
            ActiveBranchRouteCue(
              routeId = activeRouteRef.routeId,
              ownerSide = activeRouteRef.ownerSide,
              piece = activeRouteRef.piece,
              route = activeRouteRef.route,
              purpose = activeRouteRef.purpose,
              strategicFit = activeRouteRef.strategicFit,
              tacticalSafety = activeRouteRef.tacticalSafety,
              surfaceConfidence = activeRouteRef.surfaceConfidence,
              surfaceMode = activeRouteRef.surfaceMode
            )
          ),
        evidenceCue = Some("Pressure on g7 is the point.")
      )
    )

  private def activePlan(name: String = "Kingside Pressure"): PlanHypothesis =
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

  private def activeAuthorQuestion(
      id: String,
      kind: String,
      priority: Int = 100,
      why: Option[String] = None
  ): AuthorQuestionSummary =
    AuthorQuestionSummary(
      id = id,
      kind = kind,
      priority = priority,
      question = s"placeholder-$id",
      why = why,
      anchors = List("g7"),
      confidence = "Heuristic"
    )

  private def activeAuthorEvidence(
      questionId: String,
      kind: String,
      purpose: String,
      line: String
  ): AuthorEvidenceSummary =
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

  private def activeMoment(
      authorQuestions: List[AuthorQuestionSummary],
      authorEvidence: List[AuthorEvidenceSummary] = Nil,
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
      topEngineMove = None,
      collapse = None,
      strategyPack = activeStrategyPack,
      signalDigest = Some(signalDigest),
      authorQuestions = authorQuestions,
      authorEvidence = authorEvidence,
      mainStrategicPlans = List(activePlan()),
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

  private def activeDecisionFrame(
      moment: GameChronicleMoment,
      dossier: Option[ActiveBranchDossier]
  ): CertifiedDecisionFrame =
    CertifiedDecisionFrameBuilder.build(moment, activeDeltaBundle, dossier)

  private def phaseAQuietSupportPack(
      deploymentRoute: List[String]
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      signalDigest =
        Some(
          NarrativeSignalDigest(
            deploymentRoute = deploymentRoute
          )
        )
    )

  private def bookmakerObservation(scene: BookChronicleScene): BookmakerObservation =
    val outline =
      BookStyleRenderer.validatedOutline(
        scene.ctx,
        strategyPack = scene.strategyPack,
        truthContract = scene.truthContract
      )
    val plannerInputs =
      QuestionPlannerInputsBuilder.build(scene.ctx, scene.strategyPack, scene.truthContract)
    val rankedPlans =
      QuestionFirstCommentaryPlanner.plan(scene.ctx, plannerInputs, scene.truthContract)
    val selection =
      BookmakerLiveCompressionPolicy.renderSelection(plannerInputs, rankedPlans, scene.truthContract)
    val slots =
      BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
        ctx = scene.ctx,
        outline = outline,
        refs = None,
        strategyPack = scene.strategyPack,
        truthContract = scene.truthContract
      )
    val quietTrace =
      BookmakerLiveCompressionPolicy.exactFactualQuietSupportTrace(
        ctx = scene.ctx,
        refs = None,
        strategyPack = scene.strategyPack,
        truthContract = scene.truthContract
      )
    val prose = LiveNarrativeCompressionCore.deterministicProse(slots)
    val mode =
      if selection.nonEmpty then SurfaceMode.PlannerOwned
      else if quietTrace.liftApplied then SurfaceMode.ExactFactualFallbackWithSupport
      else SurfaceMode.ExactFactualFallback

    BookmakerObservation(
      owner = selection.map(sel => sel.primary.questionKind -> sel.primary.ownerFamily),
      mode = mode,
      claim = BookmakerProseContract.stripMoveHeader(slots.claim),
      prose = prose,
      quietSupportLifted = quietTrace.liftApplied,
      supportText = slots.supportPrimary.orElse(slots.supportSecondary),
      slots = slots
    )

  private def chronicleObservation(scene: BookChronicleScene): ChronicleObservation =
    val outline =
      BookStyleRenderer.validatedOutline(
        scene.ctx,
        strategyPack = scene.strategyPack,
        truthContract = scene.truthContract
      )
    val plannerInputs =
      QuestionPlannerInputsBuilder.build(scene.ctx, scene.strategyPack, scene.truthContract)
    val rankedPlans =
      QuestionFirstCommentaryPlanner.plan(scene.ctx, plannerInputs, scene.truthContract)
    val selection =
      GameChronicleCompressionPolicy.selectPlannerSurface(rankedPlans, plannerInputs)
    val artifact =
      GameChronicleCompressionPolicy.renderWithTrace(
        ctx = scene.ctx,
        parts = emptyParts.copy(focusedOutline = outline),
        strategyPack = scene.strategyPack,
        truthContract = scene.truthContract
      )
    val mode =
      if selection.nonEmpty then SurfaceMode.PlannerOwned
      else artifact match
        case Some(value) if value.quietSupportTrace.applied => SurfaceMode.ExactFactualFallbackWithSupport
        case Some(_)                                        => SurfaceMode.ExactFactualFallback
        case None                                           => SurfaceMode.Omitted

    ChronicleObservation(
      owner = selection.map(sel => sel.primary.questionKind -> sel.primary.ownerFamily),
      mode = mode,
      narrative = artifact.map(_.narrative),
      quietSupportApplied = artifact.exists(_.quietSupportTrace.applied),
      quietSupportRejectReasons = artifact.toList.flatMap(_.quietSupportTrace.rejectReasons)
    )

  private def activeObservation(scene: ActiveScene): ActiveObservation =
    val frame = activeDecisionFrame(scene.moment, scene.dossier)
    val selection =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        scene.moment,
        scene.deltaBundle,
        scene.dossier,
        frame,
        scene.truthContract
      )
    val note =
      selection.flatMap(sel => ActiveStrategicCoachingBriefBuilder.buildDeterministicNote(sel, scene.moment))
    val mode =
      if selection.nonEmpty && note.nonEmpty then SurfaceMode.PlannerOwned
      else SurfaceMode.Omitted

    ActiveObservation(
      owner = selection.map(sel => sel.primary.questionKind -> sel.primary.ownerFamily),
      mode = mode,
      note = note
    )

  private def observe(fixture: HarnessFixture): FixtureResult =
    FixtureResult(
      fixture = fixture,
      bookmaker = bookmakerObservation(fixture.scene),
      chronicle = chronicleObservation(fixture.scene),
      active = activeObservation(fixture.active)
    )

  private def containsGeneralizationLeak(text: String): Boolean =
    val lowered = text.toLowerCase
    GeneralizationMarkers.exists(lowered.contains)

  private def assertNoForbiddenQuietSupport(text: String): Unit =
    val lowered = text.toLowerCase
    List("prepare", "launch", "force", "secure", "neutraliz").foreach { stem =>
      assert(!lowered.matches(s""".*\\b${stem}\\w*\\b.*"""), clues(text, stem))
    }

  private def assertExpectation(result: FixtureResult): Unit =
    val expectation = result.fixture.expectation
    val fixtureId = result.fixture.id

    assertEquals(result.bookmaker.mode, expectation.bookmakerMode, clues(fixtureId, result.bookmaker))
    assertEquals(result.chronicle.mode, expectation.chronicleMode, clues(fixtureId, result.chronicle))

    expectation.bookmakerOwner.foreach(expected =>
      assertEquals(result.bookmaker.owner, Some(expected), clues(fixtureId, result.bookmaker))
    )
    expectation.chronicleOwner.foreach(expected =>
      assertEquals(result.chronicle.owner, Some(expected), clues(fixtureId, result.chronicle))
    )

    expectation.activeExpectation match
      case ActiveExpectation.MustMatch =>
        assertEquals(result.active.mode, SurfaceMode.PlannerOwned, clues(fixtureId, result.active))
        expectation.activeOwner.foreach(expected =>
          assertEquals(result.active.owner, Some(expected), clues(fixtureId, result.active))
        )
      case ActiveExpectation.MayOmitOrMatch =>
        result.active.mode match
          case SurfaceMode.Omitted =>
            assertEquals(result.active.note, None, clues(fixtureId, result.active))
          case SurfaceMode.PlannerOwned =>
            expectation.activeOwner.foreach(expected =>
              assertEquals(result.active.owner, Some(expected), clues(fixtureId, result.active))
            )
          case other =>
            fail(clue(s"$fixtureId unexpected Active mode: $other / ${result.active}"))
      case ActiveExpectation.MustOmit =>
        assertEquals(result.active.mode, SurfaceMode.Omitted, clues(fixtureId, result.active))
        assertEquals(result.active.note, None, clues(fixtureId, result.active))

    result.fixture.scene.expectedFallbackClaim.foreach { expected =>
      assertEquals(result.bookmaker.claim, expected, clues(fixtureId, result.bookmaker))
      expectation.chronicleMode match
        case SurfaceMode.ExactFactualFallback | SurfaceMode.ExactFactualFallbackWithSupport =>
          assertEquals(result.chronicle.narrative, Some(expected), clues(fixtureId, result.chronicle))
        case _ =>
          ()
    }

    if expectation.forbidGeneralizationLeak then
      List(
        "bookmaker" -> Some(result.bookmaker.prose),
        "chronicle" -> result.chronicle.narrative,
        "active" -> result.active.note
      ).foreach { case (surface, textOpt) =>
        textOpt.foreach(text =>
          assert(!containsGeneralizationLeak(text), clues(fixtureId, surface, text))
        )
      }

    if expectation.forbidFallbackRewrite then
      expectation.bookmakerMode match
        case SurfaceMode.ExactFactualFallback =>
          assertEquals(result.bookmaker.supportText, None, clues(fixtureId, result.bookmaker))
          assertEquals(result.bookmaker.slots.paragraphPlan, List("p1=claim"), clues(fixtureId, result.bookmaker.slots))
        case SurfaceMode.ExactFactualFallbackWithSupport =>
          assert(result.bookmaker.supportText.nonEmpty, clues(fixtureId, result.bookmaker))
        case _ =>
          ()

    if expectation.bookmakerMustLiftQuietSupport then
      assertEquals(result.bookmaker.mode, SurfaceMode.ExactFactualFallbackWithSupport, clues(fixtureId, result.bookmaker))
      assertEquals(result.bookmaker.quietSupportLifted, true, clues(fixtureId, result.bookmaker))
      assert(result.bookmaker.supportText.exists(_.toLowerCase.contains("available")), clues(fixtureId, result.bookmaker))
      assertEquals(result.bookmaker.slots.paragraphPlan, List("p1=claim", "p2=support_chain"), clues(fixtureId, result.bookmaker.slots))
      assertNoForbiddenQuietSupport(result.bookmaker.supportText.getOrElse(""))

    if expectation.chronicleMustBlockQuietSupport then
      assertEquals(result.chronicle.quietSupportApplied, false, clues(fixtureId, result.chronicle))
      assert(
        result.chronicle.quietSupportRejectReasons.contains("chronicle_exact_factual_support_blocked"),
        clues(fixtureId, result.chronicle)
      )

    expectation.forbiddenFragments.foreach { fragment =>
      val lowered = fragment.toLowerCase
      List(
        "bookmaker" -> result.bookmaker.prose,
        "chronicle" -> result.chronicle.narrative.getOrElse(""),
        "active" -> result.active.note.getOrElse("")
      ).foreach { case (surface, text) =>
        assert(!text.toLowerCase.contains(lowered), clues(fixtureId, surface, text, fragment))
      }
    }
  end assertExpectation

  private val whatChangedFixture = plannerRuntimeFixture("what_changed_positive")
  private val whyThisFallbackFixture = plannerRuntimeFixture("why_this_fallback")
  private val whyThisNegativeFixture = plannerRuntimeFixture("why_this_negative")
  private val stopPositiveFixture = plannerRuntimeFixture("what_must_be_stopped_positive")

  private val fixtures =
    List(
      HarnessFixture(
        id = "planner_owned_positive",
        scene =
          BookChronicleScene(
            ctx = whatChangedFixture.ctx,
            strategyPack = whatChangedFixture.strategyPack,
            truthContract = whatChangedFixture.truthContract
          ),
        active =
          ActiveScene(
            moment =
              activeMoment(
                authorQuestions = List(activeAuthorQuestion("q_changed", "WhatChanged")),
                authorEvidence = List(activeAuthorEvidence("q_changed", "WhatChanged", "reply_multipv", "14...Rc8 15.Re1 Qc7"))
              ),
            deltaBundle = activeDeltaBundle,
            dossier = activeDossier
          ),
        expectation =
          FixtureExpectation(
            bookmakerOwner = Some(AuthorQuestionKind.WhatChanged -> OwnerFamily.MoveDelta),
            chronicleOwner = Some(AuthorQuestionKind.WhatChanged -> OwnerFamily.MoveDelta),
            bookmakerMode = SurfaceMode.PlannerOwned,
            chronicleMode = SurfaceMode.PlannerOwned,
            activeExpectation = ActiveExpectation.MayOmitOrMatch,
            activeOwner = Some(AuthorQuestionKind.WhatChanged -> OwnerFamily.MoveDelta)
          )
      ),
      HarnessFixture(
        id = "exact_factual_fallback",
        scene =
          BookChronicleScene(
            ctx = whyThisFallbackFixture.ctx,
            strategyPack = whyThisFallbackFixture.strategyPack,
            truthContract = whyThisFallbackFixture.truthContract,
            expectedFallbackClaim = whyThisFallbackFixture.expectedFallbackClaim
          ),
        active =
          ActiveScene(
            moment =
              activeMoment(
                authorQuestions = Nil,
                signalDigest =
                  NarrativeSignalDigest(
                    deploymentContribution = Some("Pressure on g7 is the point."),
                    deploymentSurfaceMode = Some("exact")
                  )
              ),
            deltaBundle = activeDeltaBundle,
            dossier = activeDossier
          ),
        expectation =
          FixtureExpectation(
            bookmakerOwner = None,
            chronicleOwner = None,
            bookmakerMode = SurfaceMode.ExactFactualFallback,
            chronicleMode = SurfaceMode.ExactFactualFallback,
            activeExpectation = ActiveExpectation.MustOmit
          )
      ),
      HarnessFixture(
        id = "quiet_support_residual",
        scene =
          BookChronicleScene(
            ctx =
              BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
                authorQuestions = Nil,
                authorEvidence = Nil
              ),
            strategyPack = Some(phaseAQuietSupportPack(deploymentRoute = List("c3", "g3"))),
            truthContract = None,
            expectedFallbackClaim = Some("This puts the rook on c3.")
          ),
        active =
          ActiveScene(
            moment =
              activeMoment(
                authorQuestions = Nil,
                signalDigest =
                  NarrativeSignalDigest(
                    deploymentContribution = Some("Pressure on g7 is the point."),
                    deploymentSurfaceMode = Some("exact")
                  )
              ),
            deltaBundle = activeDeltaBundle,
            dossier = activeDossier
          ),
        expectation =
          FixtureExpectation(
            bookmakerOwner = None,
            chronicleOwner = None,
            bookmakerMode = SurfaceMode.ExactFactualFallbackWithSupport,
            chronicleMode = SurfaceMode.ExactFactualFallback,
            activeExpectation = ActiveExpectation.MustOmit,
            bookmakerMustLiftQuietSupport = true,
            chronicleMustBlockQuietSupport = true
          )
      ),
      HarnessFixture(
        id = "generalized_support_negative",
        scene =
          BookChronicleScene(
            ctx = whyThisNegativeFixture.ctx,
            strategyPack = whyThisNegativeFixture.strategyPack,
            truthContract = whyThisNegativeFixture.truthContract,
            expectedFallbackClaim = whyThisNegativeFixture.expectedFallbackClaim
          ),
        active =
          ActiveScene(
            moment =
              activeMoment(
                authorQuestions = List(activeAuthorQuestion("q_why_this", "WhyThis")),
                authorEvidence = List(activeAuthorEvidence("q_why_this", "WhyThis", "reply_multipv", "14...Rc8 15.Re1 Qc7"))
              ),
            deltaBundle = activeDeltaBundle,
            dossier = activeDossier
          ),
        expectation =
          FixtureExpectation(
            bookmakerOwner = None,
            chronicleOwner = None,
            bookmakerMode = SurfaceMode.ExactFactualFallback,
            chronicleMode = SurfaceMode.ExactFactualFallback,
            activeExpectation = ActiveExpectation.MustOmit,
            forbiddenFragments = List("pressure on g7", "keep pressure on g7", "mating net")
          )
      ),
      HarnessFixture(
        id = "active_diagnostic_residual",
        scene =
          BookChronicleScene(
            ctx = stopPositiveFixture.ctx,
            strategyPack = stopPositiveFixture.strategyPack,
            truthContract = stopPositiveFixture.truthContract
          ),
        active =
          ActiveScene(
            moment =
              activeMoment(
                authorQuestions =
                  List(
                    activeAuthorQuestion(
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
              ),
            deltaBundle = activeDeltaBundle,
            dossier = activeDossier
          ),
        expectation =
          FixtureExpectation(
            bookmakerOwner = Some(AuthorQuestionKind.WhatMustBeStopped -> OwnerFamily.ForcingDefense),
            chronicleOwner = Some(AuthorQuestionKind.WhatMustBeStopped -> OwnerFamily.ForcingDefense),
            bookmakerMode = SurfaceMode.PlannerOwned,
            chronicleMode = SurfaceMode.PlannerOwned,
            activeExpectation = ActiveExpectation.MayOmitOrMatch,
            activeOwner = Some(AuthorQuestionKind.WhatMustBeStopped -> OwnerFamily.ForcingDefense)
          )
      )
    )

  fixtures.foreach { fixture =>
    test(s"${fixture.id} cross-surface trust bundle stays within documented residuals") {
      val result = observe(fixture)
      assertExpectation(result)
    }
  }

  private object RiskClass:
    val SupportOnlyOverreach = "support_only_overreach"
    val FallbackRewriteRegression = "fallback_rewrite_regression"
    val UnsupportedGeneralization = "unsupported_generalization"
    val OutOfSceneGeneralization = "out_of_scene_generalization"
    val OwnerOrStrengthOverclaim = "owner_or_strength_overclaim"
    val WholeGameSupportPromotion = "whole_game_support_promotion"
    val ActiveDiagnosticResidualMisuse = "active_diagnostic_residual_misuse"

  private final case class NegativeFixture(
      id: String,
      riskClass: String,
      surfacesUnderTest: List[String],
      mustNotHappen: List[String],
      allowedResidual: List[String],
      whyItIsDangerous: String,
      assertion: () => Unit
  )

  private final case class ActiveTrustObservation(
      replay: ActiveStrategicCoachingBriefBuilder.PlannerReplay,
      selection: Option[ActiveStrategicCoachingBriefBuilder.PlannerSurfaceSelection],
      note: Option[String]
  )

  private def harnessFixture(id: String): HarnessFixture =
    fixtures.find(_.id == id).getOrElse(fail(s"missing harness fixture: $id"))

  private def activeTrustObservation(
      moment: GameChronicleMoment,
      dossier: Option[ActiveBranchDossier] = activeDossier,
      truthContract: Option[DecisiveTruthContract] = None
  ): ActiveTrustObservation =
    val frame = activeDecisionFrame(moment, dossier)
    val replay =
      ActiveStrategicCoachingBriefBuilder
        .replayPlanner(moment, activeDeltaBundle, dossier, frame, truthContract)
        .getOrElse(fail("expected active replay"))
    val selection =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        moment,
        activeDeltaBundle,
        dossier,
        frame,
        truthContract
      )
    val note = selection.flatMap(sel => ActiveStrategicCoachingBriefBuilder.buildDeterministicNote(sel, moment))
    ActiveTrustObservation(replay = replay, selection = selection, note = note)

  private def genericDecisionOutline(claim: String, followUp: String): NarrativeOutline =
    NarrativeOutline(
      beats = List(
        OutlineBeat(kind = OutlineBeatKind.Context, text = "Context beat."),
        OutlineBeat(kind = OutlineBeatKind.MainMove, text = s"$claim $followUp")
      )
    )

  private def wholeGameAnalysisData(
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

  private def wholeGameMoment(
      ply: Int,
      momentType: String,
      narrative: String,
      moveClassification: Option[String] = None,
      cpBefore: Int = 0,
      cpAfter: Int = 0,
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
      analysisData = wholeGameAnalysisData(ply),
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

  private def wholeGameTruthContract(
      ownershipRole: TruthOwnershipRole,
      visibilityRole: TruthVisibilityRole,
      surfaceMode: TruthSurfaceMode,
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
    val resolvedReasonFamily =
      if reasonFamily != DecisiveReasonFamily.InvestmentSacrifice then reasonFamily
      else if ownershipRole == TruthOwnershipRole.ConversionOwner then DecisiveReasonFamily.Conversion
      else if ownershipRole == TruthOwnershipRole.BlunderOwner then DecisiveReasonFamily.TacticalRefutation
      else DecisiveReasonFamily.InvestmentSacrifice

    DecisiveTruthContract(
      playedMove = Some("d1d5"),
      verifiedBestMove = Some("d1d5"),
      truthClass = truthClass,
      cpLoss = cpLoss,
      swingSeverity = swingSeverity,
      reasonFamily = resolvedReasonFamily,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = surfaceMode == TruthSurfaceMode.InvestmentExplain,
      truthPhase = truthPhase,
      ownershipRole = ownershipRole,
      visibilityRole = visibilityRole,
      surfaceMode = surfaceMode,
      exemplarRole =
        if ownershipRole == TruthOwnershipRole.CommitmentOwner then TruthExemplarRole.VerifiedExemplar
        else TruthExemplarRole.NonExemplar,
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

  private val negativeFixtures =
    List(
      NegativeFixture(
        id = "bookmaker_quiet_support_residual_stays_bounded",
        riskClass = RiskClass.SupportOnlyOverreach,
        surfacesUnderTest = List("Bookmaker", "Chronicle"),
        mustNotHappen = List("support-only quiet support becomes bookmaker thesis", "chronicle lifts the same support sentence"),
        allowedResidual = List("Bookmaker may keep one bounded quiet-support sentence", "Chronicle may stay claim-only"),
        whyItIsDangerous = "Quiet support is the easiest place for support-only carrier to reacquire truth-like force.",
        assertion = () =>
          val result = observe(harnessFixture("quiet_support_residual"))
          assertEquals(result.bookmaker.mode, SurfaceMode.ExactFactualFallbackWithSupport, clues(result.bookmaker))
          assertEquals(result.bookmaker.claim, "This puts the rook on c3.", clues(result.bookmaker))
          assert(result.bookmaker.supportText.exists(_.toLowerCase.contains("available")), clues(result.bookmaker))
          assertEquals(result.bookmaker.slots.paragraphPlan, List("p1=claim", "p2=support_chain"), clues(result.bookmaker.slots))
          assertNoForbiddenQuietSupport(result.bookmaker.supportText.getOrElse(""))
          assertEquals(result.chronicle.mode, SurfaceMode.ExactFactualFallback, clues(result.chronicle))
          assertEquals(result.chronicle.narrative, Some("This puts the rook on c3."), clues(result.chronicle))
          assertEquals(result.chronicle.quietSupportApplied, false, clues(result.chronicle))
      ),
      NegativeFixture(
        id = "chronicle_non_movedelta_row_rejects_quiet_support",
        riskClass = RiskClass.SupportOnlyOverreach,
        surfacesUnderTest = List("Chronicle"),
        mustNotHappen = List("non-MoveDelta row picks up quiet-support sentence"),
        allowedResidual = List("plain planner claim without extra quiet support"),
        whyItIsDangerous = "Chronicle quiet support must stay attached to exact move-delta replay rows only.",
        assertion = () =>
          val fixture = plannerRuntimeFixture("what_changed_positive")
          val artifact =
            GameChronicleCompressionPolicy
              .renderPlanSurface(
                fixture.ctx,
                GameChronicleCompressionPolicy.ChronicleRenderSurface(
                  primary =
                    QuestionPlan(
                      questionId = "q_forcing",
                      questionKind = AuthorQuestionKind.WhyNow,
                      priority = 100,
                      claim = "The move has to happen now.",
                      evidence = None,
                      contrast = None,
                      consequence = None,
                      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
                      strengthTier = QuestionPlanStrengthTier.Moderate,
                      sourceKinds = List("threat"),
                      admissibilityReasons = List("test"),
                      ownerFamily = OwnerFamily.ForcingDefense,
                      ownerSource = "threat"
                    ),
                  secondary = None,
                  contrastTrace = ContrastiveSupportAdmissibility.ContrastSupportTrace(),
                  quietSupportTrace =
                    QuietStrategicSupportComposer.QuietStrategicSupportTrace(
                      emitted = true,
                      line =
                        Some(
                          QuietStrategicSupportComposer.QuietStrategicSupportLine(
                            text = "This reinforces the fluid center.",
                            bucket = QuietStrategicSupportComposer.Bucket.LongStructuralSqueeze,
                            sourceKinds = List("MoveDelta.pv_delta", "Digest.structure"),
                            verbFamily = QuietStrategicSupportComposer.VerbFamily.Reinforces
                          )
                        ),
                      rejectReasons = Nil,
                      gatePassed = true,
                      gate =
                        QuietStrategicSupportComposer.QuietStrategicSupportGateTrace(
                          sceneType = "quiet_improvement",
                          selectedOwnerFamily = Some("MoveDelta"),
                          selectedOwnerSource = Some("pv_delta"),
                          pvDeltaAvailable = true,
                          signalDigestAvailable = true,
                          openingRelationClaimPresent = false,
                          endgameTransitionClaimPresent = false,
                          moveLinkedPvDeltaAnchorAvailable = true,
                          rejectReasons = Nil
                        )
                    )
                ),
                beatEvidence = Nil
              )
              .getOrElse(fail("expected chronicle render artifact"))

          assert(!artifact.narrative.contains("This reinforces the fluid center."), clue(artifact))
          assertEquals(artifact.quietSupportTrace.applied, false, clue(artifact))
          assert(
            artifact.quietSupportTrace.rejectReasons.contains("surface_primary_not_movedelta_pv_delta"),
            clue(artifact.quietSupportTrace)
          )
      ),
      NegativeFixture(
        id = "active_support_only_defense_cannot_promote_owner",
        riskClass = RiskClass.SupportOnlyOverreach,
        surfacesUnderTest = List("Active"),
        mustNotHappen = List("support-only defensive carrier becomes active owner"),
        allowedResidual = List("Active may omit the note entirely"),
        whyItIsDangerous = "Active still has diagnostic residuals, so support-only defensive scenes must not acquire a canonical owner.",
        assertion = () =>
          val observation =
            activeTrustObservation(
              activeMoment(
                authorQuestions =
                  List(
                    activeAuthorQuestion(
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
            )

          assertEquals(observation.replay.rankedPlans.primary, None, clue(observation.replay))
          assert(
            observation.replay.rankedPlans.ownerTrace.ownerCandidateLabels.exists(label =>
              label.contains("ForcingDefense") && label.contains("admission_decision=SupportOnly")
            ),
            clue(observation.replay.rankedPlans.ownerTrace.ownerCandidateLabels)
          )
          assertEquals(observation.selection, None, clue(observation))
          assertEquals(observation.note, None, clue(observation))
      ),
      NegativeFixture(
        id = "ambiguous_capture_stays_literal",
        riskClass = RiskClass.FallbackRewriteRegression,
        surfacesUnderTest = List("Bookmaker"),
        mustNotHappen = List("ambiguous capture regains exchange or simplification thesis"),
        allowedResidual = List("single exact-factual fallback sentence"),
        whyItIsDangerous = "Fallback rewrite drift tends to reintroduce semantic meaning that the engine bundle did not certify.",
        assertion = () =>
          val ctx =
            BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
              playedSan = Some("Qx"),
            semantic = None,
            decision = None,
            mainStrategicPlans = Nil,
            strategicPlanExperiments = Nil,
            pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet")
          )
          val slots =
            BookmakerPolishSlotsBuilder.buildOrFallback(
              ctx,
              genericDecisionOutline("A capture.", "Nothing else is stable."),
              refs = None,
              strategyPack = None
            )
          val claim = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase

          assertEquals(claim, "this captures.", clues(slots))
          assertEquals(slots.paragraphPlan, List("p1=claim"), clues(slots))
          assert(!claim.contains("exchange"), clues(claim))
          assert(!claim.contains("simplif"), clues(claim))
      ),
      NegativeFixture(
        id = "chronicle_factual_fallback_blocks_quiet_support_rewrite",
        riskClass = RiskClass.FallbackRewriteRegression,
        surfacesUnderTest = List("Chronicle"),
        mustNotHappen = List("chronicle factual fallback picks up quiet-support rewrite"),
        allowedResidual = List("claim-only exact factual fallback"),
        whyItIsDangerous = "Chronicle fallback rows are especially sensitive to quiet-support re-entry because the composer can still emit background text.",
        assertion = () =>
          val artifact =
            GameChronicleCompressionPolicy
              .renderWithTrace(
                ctx =
                  BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
                    authorQuestions = Nil,
                    authorEvidence = Nil
                  ),
                parts = emptyParts,
                strategyPack = Some(phaseAQuietSupportPack(deploymentRoute = List("c3", "g3"))),
                truthContract = None
              )
              .getOrElse(fail("expected chronicle factual fallback artifact"))

          assertEquals(artifact.narrative, "This puts the rook on c3.", clue(artifact))
          assertEquals(artifact.quietSupportTrace.applied, false, clue(artifact.quietSupportTrace))
          assert(
            artifact.quietSupportTrace.rejectReasons.contains("chronicle_exact_factual_support_blocked"),
            clue(artifact.quietSupportTrace)
          )
          assert(!artifact.narrative.toLowerCase.contains("available"), clue(artifact.narrative))
      ),
      NegativeFixture(
        id = "validator_strips_shared_lesson_release",
        riskClass = RiskClass.UnsupportedGeneralization,
        surfacesUnderTest = List("Validator"),
        mustNotHappen = List("Shared lesson release text survives validation"),
        allowedResidual = List("ordinary opening reference sentence"),
        whyItIsDangerous = "Unsafe lesson families were intentionally tagged and must never re-enter released prose.",
        assertion = () =>
          val validated =
            NarrativeOutlineValidator.validate(
              NarrativeOutline(
                List(
                  OutlineBeat(
                    kind = OutlineBeatKind.OpeningTheory,
                    text = "Reference paths still matter. Shared lesson: central control decides everything."
                  )
                )
              ),
              new TraceRecorder()
            )
          val opening = validated.beats.headOption.getOrElse(fail("missing opening beat"))

          assertEquals(opening.text, "Reference paths still matter.")
      ),
      NegativeFixture(
        id = "validator_strips_helper_labels_from_context",
        riskClass = RiskClass.UnsupportedGeneralization,
        surfacesUnderTest = List("Validator"),
        mustNotHappen = List("helper-labeled support text survives as released context prose"),
        allowedResidual = List("plain support sentence without helper label"),
        whyItIsDangerous = "Support carriers should never look like canonical truth because an internal label leaked through.",
        assertion = () =>
          val validated =
            NarrativeOutlineValidator.validate(
              NarrativeOutline(
                List(
                  OutlineBeat(kind = OutlineBeatKind.Context, text = "alignment intent: pressure on e6.")
                )
              ),
              new TraceRecorder()
            )
          val context = validated.beats.headOption.getOrElse(fail("missing context beat"))

          assertEquals(context.text, "pressure on e6.")
      )
      ,
      NegativeFixture(
        id = "validator_drops_unanchored_across_branches_context",
        riskClass = RiskClass.UnsupportedGeneralization,
        surfacesUnderTest = List("Validator"),
        mustNotHappen = List("ungrounded across-branches generalization survives in Context beat"),
        allowedResidual = List("same text may stay in OpeningTheory"),
        whyItIsDangerous = "Scene-external summaries must not pass as local truth without an anchored opening-theory scope.",
        assertion = () =>
          val validated =
            NarrativeOutlineValidator.validate(
              NarrativeOutline(
                List(
                  OutlineBeat(
                    kind = OutlineBeatKind.OpeningTheory,
                    text = "Across these branches, results changed by which side better handled central control."
                  ),
                  OutlineBeat(
                    kind = OutlineBeatKind.Context,
                    text = "Across these branches, results changed by which side better handled central control."
                  )
                )
              ),
              new TraceRecorder()
            )
          assert(
            validated.beats.exists(beat =>
              beat.kind == OutlineBeatKind.OpeningTheory &&
                beat.text == "Across these branches, results changed by which side better handled central control."
            ),
            clue(validated.beats)
          )
          assert(!validated.beats.exists(_.kind == OutlineBeatKind.Context), clue(validated.beats))
      ),
      NegativeFixture(
        id = "generalized_support_negative_bundle_stays_grounded",
        riskClass = RiskClass.OutOfSceneGeneralization,
        surfacesUnderTest = List("Bookmaker", "Chronicle", "Active"),
        mustNotHappen = List("generalized support carrier leaks pressure thesis across surfaces"),
        allowedResidual = List("Bookmaker and Chronicle may fall back to exact factual claim", "Active may omit"),
        whyItIsDangerous = "Cross-surface drift is how generalized support reappears as if it were canonical truth.",
        assertion = () =>
          val result = observe(harnessFixture("generalized_support_negative"))
          assertEquals(result.bookmaker.mode, SurfaceMode.ExactFactualFallback, clues(result.bookmaker))
          assertEquals(result.chronicle.mode, SurfaceMode.ExactFactualFallback, clues(result.chronicle))
          assertEquals(result.active.mode, SurfaceMode.Omitted, clues(result.active))
          List(
            result.bookmaker.prose,
            result.chronicle.narrative.getOrElse(""),
            result.active.note.getOrElse("")
          ).foreach { text =>
            val lowered = text.toLowerCase
            assert(!lowered.contains("pressure on g7"), clues(text))
            assert(!lowered.contains("keep pressure on g7"), clues(text))
            assert(!lowered.contains("mating net"), clues(text))
          }
      ),
      NegativeFixture(
        id = "whole_game_helper_leak_cannot_escape_scene_scope",
        riskClass = RiskClass.OutOfSceneGeneralization,
        surfacesUnderTest = List("WholeGame"),
        mustNotHappen = List("helper-leaky whole-game anchor becomes decisive wrapper"),
        allowedResidual = List("no decisive or payoff wrapper"),
        whyItIsDangerous = "Whole-game wrappers are the most dangerous place for out-of-scene support promotion.",
        assertion = () =>
          val support =
            CommentaryEngine.buildWholeGameConclusionSupport(
              moments =
                List(
                  wholeGameMoment(
                    ply = 36,
                    momentType = "AdvantageSwing",
                    moveClassification = Some("Blunder"),
                    cpBefore = 15,
                    cpAfter = 240,
                    narrative = "The turning point came through Maneuver(knight, rerouting).",
                    signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("Maneuver(knight, rerouting)"))),
                    strategyPack =
                      Some(
                        StrategyPack(
                          sideToMove = "white",
                          longTermFocus = List("Maneuver(knight, rerouting)")
                        )
                      )
                  )
                ),
              strategicThreads = Nil,
              themes = List("Central pressure")
            )

          assertEquals(support.decisiveShift, None)
          assertEquals(support.payoff, None)
      ),
      NegativeFixture(
        id = "weak_compensation_cannot_revive_bookmaker_thesis",
        riskClass = RiskClass.OwnerOrStrengthOverclaim,
        surfacesUnderTest = List("Bookmaker"),
        mustNotHappen = List("weak compensation summary becomes bookmaker compensation thesis"),
        allowedResidual = List("exact factual fallback"),
        whyItIsDangerous = "Compensation prose is easy to overclaim because it sounds strategic even when unsupported.",
        assertion = () =>
          val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
          val outline = BookStyleRenderer.validatedOutline(ctx)
          val slots =
            BookmakerPolishSlotsBuilder.buildOrFallback(
              ctx,
              outline,
              refs = None,
              strategyPack =
                Some(
                  StrategyPack(
                    sideToMove = "white",
                    signalDigest =
                      Some(
                        NarrativeSignalDigest(
                          compensation = Some("initiative against the king"),
                          investedMaterial = Some(100),
                          dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
                          dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
                          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
                          dominantIdeaFocus = Some("g7")
                        )
                      )
                  )
                )
            )

          assertEquals(BookmakerProseContract.stripMoveHeader(slots.claim), "This puts the rook on c3.", clues(slots))
          assertEquals(slots.paragraphPlan, List("p1=claim"), clues(slots))
          assert(!LiveNarrativeCompressionCore.deterministicProse(slots).toLowerCase.contains("compensation"), clues(slots))
          assert(!LiveNarrativeCompressionCore.deterministicProse(slots).toLowerCase.contains("initiative against the king"), clues(slots))
      ),
      NegativeFixture(
        id = "active_race_heavy_whythis_cannot_invent_owner",
        riskClass = RiskClass.OwnerOrStrengthOverclaim,
        surfacesUnderTest = List("Active"),
        mustNotHappen = List("race-heavy WhyThis becomes active owner"),
        allowedResidual = List("Active may omit the note entirely"),
        whyItIsDangerous = "Active diagnostic traces can look convincing even when no legal primary is admitted.",
        assertion = () =>
          val observation =
            activeTrustObservation(
              activeMoment(
                authorQuestions = List(activeAuthorQuestion("q_why_this", "WhyThis")),
                authorEvidence = List(activeAuthorEvidence("q_why_this", "WhyThis", "reply_multipv", "14...Rc8 15.Re1 Qc7"))
              )
            )

          assertEquals(observation.replay.rankedPlans.primary, None, clue(observation.replay))
          assertEquals(observation.replay.rankedPlans.ownerTrace.sceneType, SceneType.PlanClash)
          assert(
            observation.replay.rankedPlans.ownerTrace.ownerCandidateLabels.exists(label =>
              label.contains("MoveDelta") && label.contains("admission_decision=SupportOnly")
            ),
            clue(observation.replay.rankedPlans.ownerTrace.ownerCandidateLabels)
          )
          assertEquals(observation.selection, None, clue(observation))
          assertEquals(observation.note, None, clue(observation))
      ),
      NegativeFixture(
        id = "whole_game_support_only_carriers_cannot_seed_wrappers",
        riskClass = RiskClass.WholeGameSupportPromotion,
        surfacesUnderTest = List("WholeGame"),
        mustNotHappen = List("support-only carrier seeds decisive shift or payoff wrapper"),
        allowedResidual = List("whole-game support may stay empty"),
        whyItIsDangerous = "Support-only whole-game carriers are portable and can otherwise escape into decisive summary prose.",
        assertion = () =>
          val support =
            CommentaryEngine.buildWholeGameConclusionSupport(
              moments =
                List(
                  wholeGameMoment(
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
                ),
              strategicThreads = Nil,
              themes = List("Central pressure"),
              truthContractsByPly =
                Map(
                  66 ->
                    wholeGameTruthContract(
                      ownershipRole = TruthOwnershipRole.BlunderOwner,
                      visibilityRole = TruthVisibilityRole.PrimaryVisible,
                      surfaceMode = TruthSurfaceMode.FailureExplain,
                      truthClass = DecisiveTruthClass.Blunder
                    )
                )
            )

          assertEquals(support.decisiveShift, None)
          assertEquals(support.payoff, None)
      ),
      NegativeFixture(
        id = "whole_game_requires_proof_before_wrappers",
        riskClass = RiskClass.WholeGameSupportPromotion,
        surfacesUnderTest = List("WholeGame"),
        mustNotHappen = List("result wrapper appears without decisive proof"),
        allowedResidual = List("main contest may remain", "decisive and payoff wrappers stay empty"),
        whyItIsDangerous = "Result-aware wrapper prose can overstate a whole game if proof requirements weaken.",
        assertion = () =>
          val support =
            CommentaryEngine.buildWholeGameConclusionSupport(
              moments =
                List(
                  wholeGameMoment(
                    ply = 46,
                    momentType = "AdvantageSwing",
                    moveClassification = Some("Mistake"),
                    cpBefore = 60,
                    cpAfter = 180,
                    narrative = "The decisive shift came through pressure on d5.",
                    transitionType = Some("AdvantageSwing")
                  )
                ),
              strategicThreads =
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
                ),
              themes = List("center tension"),
              result = "1-0",
              truthContractsByPly = Map.empty
            )

          assert(support.mainContest.nonEmpty)
          assertEquals(support.decisiveShift, None)
          assertEquals(support.payoff, None)
      ),
      NegativeFixture(
        id = "active_no_primary_stays_omitted",
        riskClass = RiskClass.ActiveDiagnosticResidualMisuse,
        surfacesUnderTest = List("Active"),
        mustNotHappen = List("diagnostic residual produces an active note without a planner-approved primary"),
        allowedResidual = List("note omission"),
        whyItIsDangerous = "Active is allowed to omit; it must not fabricate a note from diagnostic leftovers.",
        assertion = () =>
          val moment =
            activeMoment(
              authorQuestions = Nil,
              signalDigest =
                NarrativeSignalDigest(
                  deploymentContribution = Some("Pressure on g7 is the point."),
                  deploymentSurfaceMode = Some("exact")
                )
            )
          val frame = activeDecisionFrame(moment, activeDossier)
          val selection =
            ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
              moment,
              activeDeltaBundle,
              activeDossier,
              frame
            )
          val note =
            ActiveStrategicCoachingBriefBuilder.buildDeterministicNote(
              moment = moment,
              deltaBundle = activeDeltaBundle,
              dossier = activeDossier,
              decisionFrame = Some(frame)
            )

          assertEquals(selection, None, clue(moment))
          assertEquals(note, None, clue(moment))
      ),
      NegativeFixture(
        id = "active_whose_plan_is_faster_never_owns_note",
        riskClass = RiskClass.ActiveDiagnosticResidualMisuse,
        surfacesUnderTest = List("Active"),
        mustNotHappen = List("WhosePlanIsFaster becomes active note owner"),
        allowedResidual = List("selection and note omission"),
        whyItIsDangerous = "Race framing is explicitly diagnostic-only on Active unless another admitted owner survives.",
        assertion = () =>
          val observation =
            activeTrustObservation(
              activeMoment(
                authorQuestions = List(activeAuthorQuestion("q_race", "WhosePlanIsFaster")),
                signalDigest =
                  NarrativeSignalDigest(
                    counterplayScoreDrop = Some(150),
                    opponentPlan = Some("queenside counterplay"),
                    deploymentContribution = Some("Pressure on g7 is the point."),
                    deploymentSurfaceMode = Some("exact")
                  )
              )
            )

          assertEquals(observation.selection, None, clue(observation))
          assertEquals(observation.note, None, clue(observation))
      )
    )

  test("step 7 negative fixture pack covers every trust risk class with at least two fixtures") {
    val coverage = negativeFixtures.groupBy(_.riskClass).view.mapValues(_.size).toMap
    val coveredSurfaces = negativeFixtures.flatMap(_.surfacesUnderTest).toSet

    assertEquals(negativeFixtures.size, 16)
    List(
      RiskClass.SupportOnlyOverreach,
      RiskClass.FallbackRewriteRegression,
      RiskClass.UnsupportedGeneralization,
      RiskClass.OutOfSceneGeneralization,
      RiskClass.OwnerOrStrengthOverclaim,
      RiskClass.WholeGameSupportPromotion,
      RiskClass.ActiveDiagnosticResidualMisuse
    ).foreach { riskClass =>
      assert(coverage.getOrElse(riskClass, 0) >= 2, clues(riskClass, coverage))
    }
    negativeFixtures.foreach { fixture =>
      assert(fixture.surfacesUnderTest.nonEmpty, clue(fixture.id))
      assert(fixture.mustNotHappen.nonEmpty, clue(fixture.id))
      assert(fixture.allowedResidual.nonEmpty, clue(fixture.id))
      assert(fixture.whyItIsDangerous.nonEmpty, clue(fixture.id))
    }
    assertEquals(
      coveredSurfaces,
      Set("Bookmaker", "Chronicle", "Active", "Validator", "WholeGame")
    )
  }

  negativeFixtures.foreach { fixture =>
    test(s"${fixture.id} negative trust fixture holds the regression line") {
      fixture.assertion()
    }
  }
