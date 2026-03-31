package lila.llm.analysis

import munit.FunSuite
import lila.llm.analysis.practical.ContrastiveSupportAdmissibility
import lila.llm.analysis.render.QuietStrategicSupportComposer
import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.{ EngineEvidence, PvMove, VariationLine }
import scala.annotation.unused

class BookmakerPolishSlotsTest extends FunSuite:

  private def assertNoStateSummaryLeak(text: String): Unit =
    val low = BookmakerProseContract.stripMoveHeader(text).toLowerCase
    assert(!low.contains("carlsbad"), clues(low))
    assert(!low.contains("fluidcenter"), clues(low))
    assert(!low.contains("french chain"), clues(low))
    assert(!low.contains("route toward"), clues(low))
    assert(!low.contains("concrete target"), clues(low))
    assert(!low.contains("better is"), clues(low))
    assert(!low.contains("pressure on g7"), clues(low))
    assert(!low.contains("exchange on"), clues(low))
    assert(!low.contains("defensive resource"), clues(low))
    assert(!low.contains("queen route"), clues(low))

  @unused private def assertSingleParagraphQuietIntent(
      slotsOpt: Option[BookmakerPolishSlots],
      expectedLens: StrategicLens,
      expectedFragment: String
  ): Unit =
    val slots = slotsOpt.getOrElse(fail("expected quiet-intent slots"))
    val claim = BookmakerProseContract.stripMoveHeader(slots.claim)
    assertEquals(slots.lens, expectedLens)
    assert(claim.toLowerCase.contains(expectedFragment.toLowerCase), clues(claim))
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.supportSecondary, None)
    assertEquals(slots.tension, None)
    assertEquals(slots.evidenceHook, None)
    assertEquals(slots.paragraphPlan, List("p1=claim"))
    assertNoStateSummaryLeak(slots.claim)

  private def assertExactFactualFallback(
      slots: BookmakerPolishSlots,
      expectedClaim: String
  ): Unit =
    assertEquals(slots.lens, StrategicLens.Decision)
    assertEquals(BookmakerProseContract.stripMoveHeader(slots.claim), expectedClaim)
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.supportSecondary, None)
    assertEquals(slots.tension, None)
    assertEquals(slots.evidenceHook, None)
    assertEquals(slots.paragraphPlan, List("p1=claim"))

  private def truthContract(
      ownershipRole: TruthOwnershipRole,
      visibilityRole: TruthVisibilityRole,
      surfaceMode: TruthSurfaceMode,
      truthClass: DecisiveTruthClass = DecisiveTruthClass.Best,
      reasonFamily: DecisiveReasonFamily = DecisiveReasonFamily.QuietTechnicalMove
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("c3g3"),
      verifiedBestMove = Some("c3g3"),
      truthClass = truthClass,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = reasonFamily,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = ownershipRole,
      visibilityRole = visibilityRole,
      surfaceMode = surfaceMode,
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

  private def surfaceDrivenPack(
      ideaKind: String = StrategicIdeaKind.KingAttackBuildUp,
      focusSquares: List[String] = List("g7"),
      routePiece: String = "R",
      route: List[String] = List("c3", "g3"),
      routePurpose: String = "rook lift",
      targetPiece: String = "Q",
      targetSquare: String = "g7",
      compensation: Option[String] = None,
      investedMaterial: Option[Int] = None
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_attack_g7",
          ownerSide = "white",
          kind = ideaKind,
          group = StrategicIdeaGroup.InteractionAndTransformation,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = focusSquares,
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.91
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = routePiece,
          from = route.headOption.getOrElse("c3"),
          route = route,
          purpose = routePurpose,
          strategicFit = 0.88,
          tacticalSafety = 0.74,
          surfaceConfidence = 0.82,
          surfaceMode = RouteSurfaceMode.Exact,
          evidence = List("probe-route", routePurpose)
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_g7",
          ownerSide = "white",
          piece = targetPiece,
          from = "d1",
          targetSquare = targetSquare,
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("mating net"),
          evidence = List("probe-target", s"pressure on $targetSquare")
        )
      ),
      longTermFocus = List(s"keep pressure on $targetSquare"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = compensation,
          investedMaterial = investedMaterial,
          dominantIdeaKind = Some(ideaKind),
          dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some(focusSquares.mkString(", "))
        )
      )
    )

  private def phaseAQuietSupportPack(
      deploymentRoute: List[String] = Nil,
      structuralCue: Option[String] = None,
      practicalVerdict: Option[String] = None
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      signalDigest =
        Some(
          NarrativeSignalDigest(
            deploymentRoute = deploymentRoute,
            structuralCue = structuralCue,
            practicalVerdict = practicalVerdict
          )
        )
    )

  private def assertNoForbiddenQuietSupport(text: String): Unit =
    val lowered = text.toLowerCase
    List("prepare", "launch", "force", "secure", "neutraliz").foreach { stem =>
      assert(!lowered.matches(s""".*\\b${stem}\\w*\\b.*"""), clues(text, stem))
    }

  private def tacticalCtx(base: NarrativeContext): NarrativeContext =
    base.copy(
      meta = Some(
        MetaSignals(
          choiceType = ChoiceType.NarrowChoice,
          targets = Targets(Nil, Nil),
          planConcurrency = PlanConcurrency("Attack", None, "independent"),
          errorClass = Some(
            ErrorClassification(
              isTactical = true,
              missedMotifs = List("Fork"),
              errorSummary = "tactical miss"
            )
          )
        )
      )
    )

  private def genericDecisionOutline(claim: String, followUp: String): NarrativeOutline =
    NarrativeOutline(
      beats = List(
        OutlineBeat(kind = OutlineBeatKind.Context, text = "Context beat."),
        OutlineBeat(kind = OutlineBeatKind.MainMove, text = s"$claim $followUp")
      )
    )

  private def question(
      id: String,
      kind: AuthorQuestionKind,
      priority: Int = 100,
      evidencePurposes: List[String] = Nil
  ): AuthorQuestion =
    AuthorQuestion(
      id = id,
      kind = kind,
      priority = priority,
      question = s"placeholder-$id",
      evidencePurposes = evidencePurposes
    )

  private def evidence(questionId: String, purpose: String, lines: List[String]): QuestionEvidence =
    QuestionEvidence(
      questionId = questionId,
      purpose = purpose,
      branches = lines.zipWithIndex.map { case (line, idx) =>
        EvidenceBranch(
          keyMove = s"line_${idx + 1}",
          line = line,
          evalCp = Some(40 - idx * 10)
        )
      }
    )

  private def threat(
      kind: String,
      lossIfIgnoredCp: Int,
      bestDefense: Option[String] = None,
      turnsToImpact: Int = 1
  ): ThreatRow =
    ThreatRow(
      kind = kind,
      side = "US",
      square = None,
      lossIfIgnoredCp = lossIfIgnoredCp,
      turnsToImpact = turnsToImpact,
      bestDefense = bestDefense,
      defenseCount = 1,
      insufficientData = false
    )

  BookmakerProseGoldenFixtures.all.foreach { fixture =>
    test(s"${fixture.id} direct bookmaker slots stay non-empty for ${fixture.expectedLens}") {
      val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
      val slots = BookmakerPolishSlotsBuilder.buildOrFallback(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)
      assert(BookmakerProseContract.stripMoveHeader(slots.claim).nonEmpty, clues(fixture.id))
    }
  }

  test("bookmaker maps a race question plan into claim support and cited proof slots") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        summary = NarrativeSummary("Kingside Pressure", None, "NarrowChoice", "Maintain", "+0.20"),
        plans =
          PlanTable(
            top5 =
              List(
                PlanRow(
                  rank = 1,
                  name = "Kingside Pressure",
                  score = 0.82,
                  evidence = List("probe-backed"),
                  confidence = ConfidenceLevel.Probe
                )
              ),
            suppressed = Nil
          ),
        threats = ThreatTable(toUs = List(threat("Counterplay", 220, Some("...Rc8"))), toThem = Nil),
        authorQuestions = List(
          question("q_race", AuthorQuestionKind.WhosePlanIsFaster, evidencePurposes = List("reply_multipv"))
        ),
        authorEvidence =
          List(
            evidence(
              "q_race",
              "reply_multipv",
              List("23...Rc8 24.Rg3 Rc7 25.Qxg7+", "23...Rc8 24.Qh5 Rc7 25.Rg3")
            )
          ),
        opponentPlan = Some(PlanRow(1, "Queenside Counterplay", 0.72, List("...c5 break"))),
        mainStrategicPlans =
          List(
            PlanHypothesis(
              planId = "kingside_expansion",
              planName = "Kingside Pressure",
              rank = 1,
              score = 0.82,
              preconditions = Nil,
              executionSteps = List("Keep the pressure on g7."),
              failureModes = Nil,
              viability = PlanViability(score = 0.82, label = "high", risk = "stable"),
              evidenceSources = List("probe-backed"),
              themeL1 = "kingside_space"
            )
          ),
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "kingside_expansion",
              evidenceTier = "evidence_backed",
              bestReplyStable = true,
              futureSnapshotAligned = true
            )
          )
      )
    val strategyPack = surfaceDrivenPack(routePurpose = "kingside pressure", targetSquare = "g7")
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = Some(strategyPack))
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, Some(strategyPack), truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, None)
    val slots =
      BookmakerLiveCompressionPolicy
        .buildSlots(ctx, outline, None, Some(strategyPack))
        .getOrElse(fail(outline.diagnostics.map(_.summary).getOrElse("expected bookmaker slots")))

    val claim = BookmakerProseContract.stripMoveHeader(slots.claim)
    assertEquals(
      rankedPlans.primary.map(_.questionKind),
      Some(AuthorQuestionKind.WhosePlanIsFaster),
      clues(plannerInputs.decisionFrame, rankedPlans, claim)
    )
    assert(claim.toLowerCase.contains("queenside counterplay"), clues(claim))
    assert(
      slots.supportPrimary.exists(text =>
        text.toLowerCase.contains("kingside") || text.toLowerCase.contains("real fight")
      ),
      clues(slots)
    )
    assert(slots.evidenceHook.exists(_.startsWith("a)")), clues(slots.evidenceHook))
    assertEquals(slots.paragraphPlan, List("p1=claim", "p2=support_chain", "p3=tension_or_evidence"))
  }

  test("bookmaker does not attach uncertified decision-frame support when only fallback survives") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "kingside_attack",
              evidenceTier = "pv_coupled",
              bestReplyStable = false,
              futureSnapshotAligned = false
            )
          )
      )
    val strategyPack = surfaceDrivenPack(routePurpose = "kingside pressure", targetSquare = "g7")
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        genericDecisionOutline("This increases pressure on g7.", "The route stays concrete."),
        refs = None,
        strategyPack = Some(strategyPack)
      )

    assertEquals(BookmakerProseContract.stripMoveHeader(slots.claim), "This puts the rook on c3.")
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.supportSecondary, None)
  }

  test("bookmaker fails closed when tactical failure owns a WhyNow request") {
    val ctx =
      tacticalCtx(BookmakerProseGoldenFixtures.openFileFight.ctx).copy(
        threats = ThreatTable(toUs = List(threat("Mate", 900, Some("Qd8"))), toThem = Nil),
        authorQuestions = List(
          question("q_now", AuthorQuestionKind.WhyNow, evidencePurposes = List("reply_multipv"))
        ),
        authorEvidence =
          List(
            evidence(
              "q_now",
              "reply_multipv",
              List("14...Rc8 15.Re1 Qd8", "14...Rc8 15.a4 Qd8")
            )
          )
      )
    val outline =
      BookStyleRenderer.validatedOutline(
        ctx,
        truthContract =
          Some(
            truthContract(
              ownershipRole = TruthOwnershipRole.BlunderOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Blunder,
              reasonFamily = DecisiveReasonFamily.TacticalRefutation
            )
          )
      )
    val slots =
      BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
        ctx = ctx,
        outline = outline,
        refs = None,
        strategyPack = None,
        truthContract =
          Some(
            truthContract(
              ownershipRole = TruthOwnershipRole.BlunderOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Blunder,
              reasonFamily = DecisiveReasonFamily.TacticalRefutation
            )
          )
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("state-only structure fixture now falls back to an exact factual one-liner") {
    val fixture = BookmakerProseGoldenFixtures.entrenchedPiece
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)
    assertExactFactualFallback(slots, "This puts the knight on f1.")
  }

  test("quiet intent ignores break-ready hints and refuses to emit break-preparation prose") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        playedSan = Some("Qc7"),
        semantic = None,
        pawnPlay = PawnPlayTable(
          breakReady = true,
          breakFile = Some("d"),
          breakImpact = "High",
          tensionPolicy = "Maintain",
          tensionReason = "test",
          passedPawnUrgency = "Background",
          passerBlockade = None,
          counterBreak = false,
          primaryDriver = "break_ready"
        )
      )

    val quietClaim = QuietMoveIntentBuilder.build(ctx).map(_.claimText.toLowerCase).getOrElse("")
    assert(!quietClaim.contains("break"), clues(quietClaim))
  }

  test("soft repair restores missing claim and strips placeholders") {
    val slots =
      BookmakerPolishSlots(
        lens = StrategicLens.Decision,
        claim = "12... Qc7: This increases pressure on h7.",
        supportPrimary = None,
        supportSecondary = None,
        tension = Some("The move stays tied to that pressure."),
        evidenceHook = None,
        coda = None,
        factGuardrails = Nil,
        paragraphPlan = List("p1=claim", "p2=practical_nuance")
      )
    val broken =
      """The move keeps pressure and remains preferable.
        |
        |The direct alternative stays secondary because Piece Activation is deferred as PlayableByPV under strict evidence mode (supported by engine-coupled continuation (probe evidence pending)). Further probe work still targets Piece Activation [subplan:worst piece improvement].""".stripMargin
    val repaired = BookmakerSoftRepair.repair(broken, slots)
    assert(repaired.applied)
    assertEquals(repaired.evaluation.placeholderHits, Nil)
    assertEquals(repaired.evaluation.genericHits, Nil)
    assert(repaired.evaluation.claimLikeFirstParagraph)
    assert(repaired.evaluation.paragraphBudgetOk)
    assert(repaired.materialApplied)
    assert(repaired.materialActions.nonEmpty)
  }

  test("soft repair treats claim-only restore as cosmetic") {
    val slots =
      BookmakerPolishSlots(
        lens = StrategicLens.Decision,
        claim = "1. e4: This move claims central space.",
        supportPrimary = Some("It opens lines for both bishops."),
        supportSecondary = None,
        tension = None,
        evidenceHook = None,
        coda = None,
        factGuardrails = Nil,
        paragraphPlan = List("p1=claim", "p2=support_chain")
      )
    val generic =
      """The move remains preferable.
        |
        |It opens lines for both bishops.""".stripMargin
    val repaired = BookmakerSoftRepair.repair(generic, slots)
    assert(repaired.applied)
    assert(repaired.actions.contains("claim_restore"))
    assert(!repaired.materialApplied)
    assertEquals(repaired.materialActions, Nil)
  }

  test("deterministic third paragraph wraps bare variation evidence in prose") {
    val slots =
      BookmakerPolishSlots(
        lens = StrategicLens.Decision,
        claim = "1. e4: This move claims central space.",
        supportPrimary = Some("It opens lines for both bishops."),
        supportSecondary = Some("This move prepares rapid development."),
        tension = Some("Black can still challenge the center if White drifts."),
        evidenceHook = Some("a) ...c5 Nf3 Nc6 (+0.2)"),
        coda = None,
        factGuardrails = Nil,
        paragraphPlan = List("p1=claim", "p2=support_chain", "p3=tension_or_evidence")
      )
    val paragraphs = BookmakerSoftRepair.deterministicParagraphs(slots)
    assertEquals(paragraphs.size, 3)
    assert(paragraphs(2).contains("One concrete line that keeps the idea in play is a) ...c5 Nf3 Nc6 (+0.2)"))
  }

  test("sanitizer preserves chess ellipsis markers in prose") {
    val raw =
      "Probe evidence starts with. Rc8: Rc8 Rc3 Rg6 (+0.42). The alternative is 12.. Qh5, and fixing lines with.. h5 makes.. Rh6-g6 realistic."
    val sanitized = BookmakerSlotSanitizer.sanitizeUserText(raw)
    assertEquals(
      sanitized,
      "Probe evidence starts with ...Rc8: Rc8 Rc3 Rg6 (+0.42). The alternative is 12...Qh5, and fixing lines with ...h5 makes ...Rh6-g6 realistic."
    )
  }

  test("tactical blunders now surface through planner-owned WhyThis claims") {
    val ctx =
      BookmakerProseGoldenFixtures.rookPawnMarch.ctx.copy(
        authorQuestions = List(
          question("q_blunder", AuthorQuestionKind.WhyThis, evidencePurposes = List("reply_multipv"))
        ),
        authorEvidence =
          List(
            evidence(
              "q_blunder",
              "reply_multipv",
              List("...Rc1+ 2.Bf1 Qxf1+", "...Rc1+ 2.Qxc1 Qxc1")
            )
          ),
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.NarrowChoice,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Rook-Pawn March", None, "independent"),
            errorClass = Some(
              ErrorClassification(
                isTactical = true,
                missedMotifs = List("Fork"),
                errorSummary = "전술(320cp, Fork)"
              )
            )
          )
        )
      )
    val outline =
      NarrativeOutline(
        beats = List(
          OutlineBeat(
            kind = OutlineBeatKind.Context,
            text = "The move reorganizes the pieces around Kingside Clamp, aiming at Rook-Pawn March."
          ),
          OutlineBeat(
            kind = OutlineBeatKind.MainMove,
            text =
              "**h5??** is a tactical blunder; it misses the idea of a fork on e6 and allows ...Rc1+ by force. Better is **Rc3** to keep tighter control. The forcing reply lands before the kingside plan gets going."
          )
        )
      )
    val slots =
      BookmakerPolishSlotsBuilder.build(
        ctx,
        outline,
        refs = None,
        truthContract =
          Some(
            truthContract(
              ownershipRole = TruthOwnershipRole.BlunderOwner,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Blunder,
              reasonFamily = DecisiveReasonFamily.TacticalRefutation
            )
          )
      ).getOrElse(fail("missing slots"))
    val claimCore = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(claimCore.contains("blunder"))
    assert(!claimCore.contains("reorganizes the pieces around kingside clamp"))
    val rendered = (slots.claim :: slots.support).appended(slots.evidenceHook.getOrElse("")).mkString(" ").toLowerCase
    assert(!rendered.contains("better is"), clue(rendered))
  }

  test("compensation slots may omit bookmaker prose instead of inventing a stronger compensation thesis") {
    val fixture = BookmakerProseGoldenFixtures.exchangeSacrifice
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slotsOpt =
      BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)

    assert(
      slotsOpt.forall { slots =>
        val rendered = (slots.claim :: slots.support).appended(slots.tension.getOrElse("")).mkString(" ").toLowerCase
        !rendered.contains("180cp")
      }
    )
  }

  test("compensation omission path avoids broken compensation skeleton fragments") {
    val fixture = BookmakerProseGoldenFixtures.exchangeSacrifice
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slotsOpt =
      BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)

    assert(
      slotsOpt.forall { slots =>
        val rendered = (slots.claim :: slots.support).appended(slots.tension.getOrElse("")).mkString(" ").toLowerCase
        !rendered.contains("while aiming for") &&
        !rendered.contains("via queen toward") &&
        !rendered.contains("the play still runs through") &&
        !rendered.contains("pressure keeps building through")
      }
    )
  }

  test("weak compensation slots collapse to an exact factual one-liner instead of compensation copy") {
    val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val weakPack =
      Some(
        StrategyPack(
          sideToMove = "white",
          signalDigest = Some(
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

    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(ctx, outline, refs = None, strategyPack = weakPack)

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("surface-only strategic pack cannot revive a bookmaker thesis beyond exact factual fallback") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        whyAbsentFromTopMultiPV = List("the immediate 'Qh5' thrust lets Black trade queens and kill the attack")
      )
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(ctx, outline, refs = None, strategyPack = Some(surfaceDrivenPack()))

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("latent or pv-coupled support traces cannot prevent exact factual fallback") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        semantic = None,
        decision = None,
        mainStrategicPlans =
          List(
            PlanHypothesis(
              planId = "kingside_attack",
              planName = "Kingside Pressure",
              rank = 1,
              score = 0.82,
              preconditions = Nil,
              executionSteps = List("Keep the pressure on g7."),
              failureModes = Nil,
              viability = PlanViability(score = 0.82, label = "high", risk = "stable"),
              evidenceSources = List("probe-backed"),
              themeL1 = "kingside_attack"
            )
          ),
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "kingside_attack",
              evidenceTier = "pv_coupled",
              bestReplyStable = false,
              futureSnapshotAligned = false
            )
          ),
        latentPlans =
          List(
            LatentPlanNarrative(
              seedId = "latent_1",
              planName = "Kingside Pressure",
              viabilityScore = 0.72,
              whyAbsentFromTopMultiPv = "probe evidence pending"
            )
          ),
        whyAbsentFromTopMultiPV = List("Further probe work still targets the kingside lift.")
      )
    val outline = genericDecisionOutline("A route exists.", "The key idea is kingside pressure.")
    val shellPack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("Further probe work still targets kingside pressure."),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              strategicFlow = Some("The key idea is kingside pressure.")
            )
          )
      )
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(shellPack)
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("EVA01-style surface pack drops unsupported routed thesis and keeps only exact factual fallback") {
    val ctx = tacticalCtx(BookmakerProseGoldenFixtures.openFileFight.ctx)
    val outline =
      genericDecisionOutline(
        "The whole decision turns on b7.",
        "The practical alternative Ng5 stays secondary because the attack is not ready."
      )
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(
          surfaceDrivenPack(
            focusSquares = List("d6"),
            routePiece = "N",
            route = List("b1", "a3", "c2", "e3"),
            routePurpose = "plan activation lane",
            targetPiece = "N",
            targetSquare = "d6"
          )
        )
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("KG01-style surface pack drops unsupported route-first thesis and keeps only exact factual fallback") {
    val ctx = tacticalCtx(BookmakerProseGoldenFixtures.openFileFight.ctx)
    val outline =
      genericDecisionOutline(
        "The whole decision turns on e5.",
        "The practical alternative Qd2 stays secondary because the attack is not ready."
      )
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(
          surfaceDrivenPack(
            focusSquares = List("e5", "g5"),
            routePiece = "Q",
            route = List("d1", "f3", "f5"),
            routePurpose = "kingside pressure",
            targetPiece = "N",
            targetSquare = "e5"
          )
        )
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("CAT02-style state-only exchange thesis cannot outlive exact factual fallback") {
    val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(
          surfaceDrivenPack(
            ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
            focusSquares = List("d4", "d5"),
            routePiece = "R",
            route = List("a8", "a6", "c6"),
            routePurpose = "open file occupation",
            targetPiece = "R",
            targetSquare = "b3"
          )
        )
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("QID02-style state-only routed thesis cannot outlive exact factual fallback") {
    val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(
          surfaceDrivenPack(
            focusSquares = Nil,
            routePiece = "Q",
            route = List("d7", "b5", "f5"),
            routePurpose = "plan activation lane",
            targetPiece = "Q",
            targetSquare = "d3"
          )
        )
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("quiet low-evidence moves now return an exact factual one-liner instead of route narration") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil,
        latentPlans = Nil,
        whyAbsentFromTopMultiPV = Nil,
        strategicSalience = lila.llm.model.strategic.StrategicSalience.Low
      )
    val outline =
      genericDecisionOutline(
        "The move keeps the route toward e5 available while the plan stays flexible.",
        "The route toward e5 is still the long-term point."
      )
    val shellOnlyPack =
      Some(
        StrategyPack(
          sideToMove = "white",
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_shell_only",
              ownerSide = "white",
              kind = StrategicIdeaKind.TargetFixing,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = Nil,
              confidence = 0.66
            )
          ),
          longTermFocus = List("Carlsbad pressure")
        )
      )

    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = shellOnlyPack
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("quiet bogus tactical labels collapse to an exact factual one-liner") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil,
        latentPlans = Nil,
        whyAbsentFromTopMultiPV = Nil,
        strategicSalience = lila.llm.model.strategic.StrategicSalience.Low
      )
    val outline =
      genericDecisionOutline(
        "Tactically, check enters the position. The concrete square is d6.",
        "Better is Qa5 once the route toward d6 is ready."
      )
    val shellOnlyPack =
      Some(
        StrategyPack(
          sideToMove = "white",
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_shell_only",
              ownerSide = "white",
              kind = StrategicIdeaKind.TargetFixing,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = Nil,
              confidence = 0.62
            )
          ),
          longTermFocus = List("FluidCenter pressure")
        )
      )

    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = shellOnlyPack
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("only-move defense stays support-only when no bookmaker-safe WhyNow surface survives") {
    val ctx =
      tacticalCtx(BookmakerProseGoldenFixtures.openFileFight.ctx).copy(
        authorQuestions =
          List(question("q_only", AuthorQuestionKind.WhyNow, evidencePurposes = List("defense_reply_multipv"))),
        authorEvidence =
          List(
            evidence(
              "q_only",
              "defense_reply_multipv",
              List("Qf3 g6 Qxf7+ Kxf7", "Qf3 Rc7 Qxf7+")
            )
          ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("d1f3", "g7g6", "f3f7", "g8f7"),
                scoreCp = 65,
                depth = 18,
                parsedMoves = List(
                  PvMove("d1f3", "Qf3", "d1", "f3", "Q", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("g7g6", "g6", "g7", "g6", "P", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("f3f7", "Qxf7+", "f3", "f7", "Q", isCapture = true, capturedPiece = Some("p"), givesCheck = true),
                  PvMove("g8f7", "Kxf7", "g8", "f7", "K", isCapture = true, capturedPiece = Some("q"), givesCheck = false)
                )
              )
            )
          )
        )
      )
    val outline =
      genericDecisionOutline(
        "The move keeps the queen route toward e5 available while pressure builds.",
        "The route is ready once the rook joins."
      )
    val slots =
      BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
        ctx = ctx,
        outline = outline,
        refs = None,
        strategyPack =
          Some(
            surfaceDrivenPack(
              focusSquares = List("e5"),
              routePiece = "Q",
              route = List("d1", "f3", "f5"),
              routePurpose = "kingside pressure",
              targetPiece = "N",
              targetSquare = "e5"
            )
          ),
        truthContract =
          Some(
            truthContract(
              ownershipRole = TruthOwnershipRole.NoneRole,
              visibilityRole = TruthVisibilityRole.PrimaryVisible,
              surfaceMode = TruthSurfaceMode.FailureExplain,
              truthClass = DecisiveTruthClass.Best,
              reasonFamily = DecisiveReasonFamily.OnlyMoveDefense
            )
          )
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("bookmaker keeps WhyNow as the rendered primary when threat-stop would leak outside contrast scope") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        threats = ThreatTable(toUs = List(threat("Material threat", 220, Some("Qd8"))), toThem = Nil),
        authorQuestions =
          List(
            question("q_now_scope", AuthorQuestionKind.WhyNow, evidencePurposes = List("reply_multipv")),
            question("q_stop_scope", AuthorQuestionKind.WhatMustBeStopped, evidencePurposes = List("reply_multipv"))
          ),
        authorEvidence =
          List(
            evidence("q_now_scope", "reply_multipv", List("14...Rc8 15.Re1 Qd8", "14...Rc8 15.a4 Qd8")),
            evidence("q_stop_scope", "reply_multipv", List("14...Rc8 15.Re1 Qd8"))
          )
      )
    val contract =
      Some(
        truthContract(
          ownershipRole = TruthOwnershipRole.NoneRole,
          visibilityRole = TruthVisibilityRole.PrimaryVisible,
          surfaceMode = TruthSurfaceMode.FailureExplain,
          truthClass = DecisiveTruthClass.Best,
          reasonFamily = DecisiveReasonFamily.OnlyMoveDefense
        )
      )
    val outline =
      genericDecisionOutline(
        "The move keeps the rook coordinated before the position loosens.",
        "The timing window closes if White drifts."
      )
    val inputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = contract)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, inputs, contract)
    val renderSelection =
      BookmakerLiveCompressionPolicy
        .renderSelection(inputs, rankedPlans, contract)
        .getOrElse(fail("expected bookmaker render selection"))
    val slots =
      BookmakerLiveCompressionPolicy
        .buildSlots(ctx, outline, refs = None, strategyPack = None, truthContract = contract)
        .getOrElse(fail("expected bookmaker slots"))

    assertEquals(rankedPlans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatMustBeStopped), clues(rankedPlans))
    assertEquals(rankedPlans.secondary.map(_.questionKind), Some(AuthorQuestionKind.WhyNow), clues(rankedPlans))
    assertEquals(renderSelection.primary.questionKind, AuthorQuestionKind.WhyNow, clues(renderSelection))
    assertEquals(
      renderSelection.contrastTrace.contrast_source_kind,
      Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss),
      clues(renderSelection.contrastTrace)
    )
    val claim = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(claim.contains("timing") || claim.contains("now"), clues(claim, slots))
    assert(!claim.contains("has to stop"), clues(claim, slots))
    assert(slots.supportPrimary.exists(_.contains("Qd8")), clues(slots.supportPrimary, slots))
  }

  test("neutral truth contract drops raw compensation prose and falls back to exact move semantics") {
    val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val rawCompensationPack =
      Some(
        surfaceDrivenPack(
          compensation = Some("pressure on g7"),
          investedMaterial = Some(100)
        )
      )

    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = rawCompensationPack,
        truthContract =
          Some(
            truthContract(
              ownershipRole = TruthOwnershipRole.NoneRole,
              visibilityRole = TruthVisibilityRole.Hidden,
              surfaceMode = TruthSurfaceMode.Neutral
            )
          )
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("direct bookmaker fallback returns an exact factual one-liner when quiet taxonomy also fails") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        playedSan = Some("Qxc6"),
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil,
        latentPlans = Nil,
        whyAbsentFromTopMultiPV = Nil,
        pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet")
      )
    val outline = genericDecisionOutline("A capture.", "Nothing else is stable.")
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None
      )

    assertEquals(BookmakerProseContract.stripMoveHeader(slots.claim), "This captures on c6.")
    assertEquals(slots.paragraphPlan, List("p1=claim"))
  }

  test("direct bookmaker fallback keeps ambiguous captures literal") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        playedSan = Some("Qx"),
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil,
        latentPlans = Nil,
        whyAbsentFromTopMultiPV = Nil,
        pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet")
      )
    val outline = genericDecisionOutline("A capture.", "Nothing else is stable.")
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None
      )

    assertEquals(BookmakerProseContract.stripMoveHeader(slots.claim), "This captures.")
    assertEquals(slots.paragraphPlan, List("p1=claim"))
  }

  test("exact factual fallback can lift an eligible quiet-support row with one bounded support sentence") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        authorQuestions = Nil,
        authorEvidence = Nil
      )
    val strategyPack =
      Some(
        phaseAQuietSupportPack(
          deploymentRoute = List("c3", "g3")
        )
      )
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = strategyPack)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)
    val plannerSlots = BookmakerLiveCompressionPolicy.buildSlots(ctx, outline, refs = None, strategyPack = strategyPack)
    val quietTrace =
      BookmakerLiveCompressionPolicy.exactFactualQuietSupportTrace(
        ctx = ctx,
        refs = None,
        strategyPack = strategyPack
      )
    val slots =
      BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
        ctx = ctx,
        outline = outline,
        refs = None,
        strategyPack = strategyPack
      )

    assertEquals(plannerSlots, None, clues(rankedPlans))
    assertEquals(rankedPlans.primary, None, clues(rankedPlans))
    assertEquals(rankedPlans.ownerTrace.selectedQuestion, None, clues(rankedPlans.ownerTrace))
    assertEquals(rankedPlans.ownerTrace.selectedOwnerFamily, None, clues(rankedPlans.ownerTrace))
    assertEquals(BookmakerProseContract.stripMoveHeader(slots.claim), "This puts the rook on c3.")
    assert(slots.supportPrimary.exists(_.toLowerCase.contains("available")), clues(slots))
    assertEquals(slots.supportSecondary, None)
    assertEquals(slots.paragraphPlan, List("p1=claim", "p2=support_chain"))
    assertEquals(quietTrace.liftApplied, true, clues(quietTrace))
    assertEquals(quietTrace.rejectReasons, Nil, clues(quietTrace))
    assertEquals(quietTrace.composerTrace.gatePassed, true, clues(quietTrace))
    assertEquals(quietTrace.composerTrace.gate.sceneType, SceneType.TransitionConversion.wireName, clues(quietTrace))
    assertEquals(quietTrace.composerTrace.gate.pvDeltaAvailable, true, clues(quietTrace))
    assertEquals(quietTrace.composerTrace.gate.moveLinkedPvDeltaAnchorAvailable, true, clues(quietTrace))
    assertEquals(
      quietTrace.composerTrace.line.map(_.bucket),
      Some(QuietStrategicSupportComposer.Bucket.SlowRouteImprovement),
      clues(quietTrace)
    )
    assertNoForbiddenQuietSupport(slots.supportPrimary.getOrElse(""))
  }

  test("planner-owned bookmaker rows skip quiet-support fallback lifting") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        summary = NarrativeSummary("Kingside Pressure", None, "NarrowChoice", "Maintain", "+0.20"),
        plans =
          PlanTable(
            top5 =
              List(
                PlanRow(
                  rank = 1,
                  name = "Kingside Pressure",
                  score = 0.82,
                  evidence = List("probe-backed"),
                  confidence = ConfidenceLevel.Probe
                )
              ),
            suppressed = Nil
          ),
        threats = ThreatTable(toUs = List(threat("Counterplay", 220, Some("...Rc8"))), toThem = Nil),
        authorQuestions = List(
          question("q_race", AuthorQuestionKind.WhosePlanIsFaster, evidencePurposes = List("reply_multipv"))
        ),
        authorEvidence =
          List(
            evidence(
              "q_race",
              "reply_multipv",
              List("23...Rc8 24.Rg3 Rc7 25.Qxg7+", "23...Rc8 24.Qh5 Rc7 25.Rg3")
            )
          ),
        opponentPlan = Some(PlanRow(1, "Queenside Counterplay", 0.72, List("...c5 break"))),
        mainStrategicPlans =
          List(
            PlanHypothesis(
              planId = "kingside_expansion",
              planName = "Kingside Pressure",
              rank = 1,
              score = 0.82,
              preconditions = Nil,
              executionSteps = List("Keep the pressure on g7."),
              failureModes = Nil,
              viability = PlanViability(score = 0.82, label = "high", risk = "stable"),
              evidenceSources = List("probe-backed"),
              themeL1 = "kingside_space"
            )
          ),
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "kingside_expansion",
              evidenceTier = "evidence_backed",
              bestReplyStable = true,
              futureSnapshotAligned = true
            )
          )
      )
    val strategyPack = Some(surfaceDrivenPack(routePurpose = "kingside pressure", targetSquare = "g7"))
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = strategyPack)
    val plannerOwned =
      BookmakerLiveCompressionPolicy
        .buildSlots(ctx, outline, refs = None, strategyPack = strategyPack)
        .getOrElse(fail("expected planner-owned bookmaker slots"))
    val fallbackAware =
      BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
        ctx = ctx,
        outline = outline,
        refs = None,
        strategyPack = strategyPack
      )

    assertEquals(fallbackAware, plannerOwned)
  }

  test("line-scoped tactical proof alone no longer owns bookmaker prose and falls back to exact move semantics") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil,
        latentPlans = Nil,
        whyAbsentFromTopMultiPV = Nil
      )
    val outline =
      genericDecisionOutline(
        "A quiet move.",
        "Nothing concrete changes yet."
      )
    val refs =
      Some(
        BookmakerRefsV1(
          startFen = ctx.fen,
          startPly = ctx.ply,
          variations = List(
            VariationRefV1(
              lineId = "pv1",
              scoreCp = 42,
              mate = None,
              depth = 18,
              moves = List(
                MoveRefV1("m1", "Qxf7+", "f3f7", ctx.fen, ctx.ply + 1, (ctx.ply + 2) / 2, None),
                MoveRefV1("m2", "Kxf7", "g8f7", ctx.fen, ctx.ply + 2, (ctx.ply + 3) / 2, None),
                MoveRefV1("m3", "Bxd5+", "c4d5", ctx.fen, ctx.ply + 3, (ctx.ply + 4) / 2, None)
              )
            )
          )
        )
      )

    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(ctx, outline, refs = refs, strategyPack = None)

    val claim = BookmakerProseContract.stripMoveHeader(slots.claim)
    assertEquals(claim, "This puts the rook on c3.")
    assertEquals(slots.evidenceHook, None)
  }
