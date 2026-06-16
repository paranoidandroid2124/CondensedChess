package lila.commentary.analysis

import chess.*
import munit.FunSuite
import lila.commentary.analysis.render.QuietStrategicSupportComposer
import lila.commentary.*
import lila.commentary.model.*
import lila.commentary.model.authoring.*
import lila.commentary.model.strategic.{ EngineEvidence, PvMove, VariationLine }
import scala.annotation.unused

class MoveReviewPolishSlotsTest extends FunSuite:

  private def assertNoStateSummaryLeak(text: String): Unit =
    val low = MoveReviewProseContract.stripMoveHeader(text).toLowerCase
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
      slotsOpt: Option[MoveReviewPolishSlots],
      expectedLens: StrategicLens,
      expectedFragment: String
  ): Unit =
    val slots = slotsOpt.getOrElse(fail("expected quiet-intent slots"))
    val claim = MoveReviewProseContract.stripMoveHeader(slots.claim)
    assertEquals(slots.lens, expectedLens)
    assert(claim.toLowerCase.contains(expectedFragment.toLowerCase), clues(claim))
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.supportSecondary, None)
    assertEquals(slots.tension, None)
    assertEquals(slots.evidenceHook, None)
    assertEquals(slots.paragraphPlan, List("p1=claim"))
    assertNoStateSummaryLeak(slots.claim)

  private def assertExactFactualFallback(
      slots: MoveReviewPolishSlots,
      expectedClaim: String
  ): Unit =
    assertEquals(slots.lens, StrategicLens.Decision)
    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    val stripped = MoveReviewProseContract.stripMoveHeader(slots.claim)
    assertEquals(stripped, expectedClaim)
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
      reasonFamily: DecisiveReasonKind = DecisiveReasonKind.QuietTechnicalMove
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
      deploymentRoute: List[String],
      structuralCue: Option[String],
      practicalVerdict: Option[String]
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

  private def localFallbackCtx(
      fen: String,
      playedMove: String,
      playedSan: String,
      ply: Int = 1
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = ply,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary("local factual fallback", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "local factual fallback"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def legalRefsForLine(
      startFen: String,
      ucis: List[String],
      sans: List[String],
      lineId: String = "line_01"
  ): MoveReviewRefs =
    val fens =
      ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewRefs(
      startFen = startFen,
      startPly = NarrativeUtils.plyFromFen(startFen).map(_ + 1).getOrElse(1),
      variations = List(
        MoveReviewVariationRef(
          lineId = lineId,
          scoreCp = 32,
          mate = None,
          depth = 18,
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
                marker = None
              )
            }
        )
      )
    )

  private def question(
      id: String,
      kind: AuthorQuestionKind,
      priority: Int = 100,
      evidencePurposes: List[String]
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
      bestDefense: Option[String],
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

  MoveReviewProseGoldenFixtures.all.foreach { fixture =>
    test(s"${fixture.id} direct moveReview slots stay non-empty for ${fixture.expectedLens}") {
      val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
      val slots = MoveReviewPolishSlotsBuilder.buildOrFallback(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)
      assert(MoveReviewProseContract.stripMoveHeader(slots.claim).nonEmpty, clues(fixture.id))
    }
  }

  test("moveReview falls back to exact local facts when race support lacks admitted planner ownership") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
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
          ),
        strategicPlanEvidence =
          StrategicPlanEvidenceTestSupport.probeBacked(
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
            )
          )
      )
    val strategyPack = surfaceDrivenPack(routePurpose = "kingside pressure", targetSquare = "g7")
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = Some(strategyPack))
    val slotsOpt =
      MoveReviewCompressionPolicy
        .buildSlots(ctx, outline, None, Some(strategyPack))
    assertEquals(slotsOpt, None)

    val fallbackSlots =
      MoveReviewCompressionPolicy.buildSlotsOrFallback(ctx, outline, None, Some(strategyPack))
    val claim = MoveReviewProseContract.stripMoveHeader(fallbackSlots.claim)
    assertEquals(fallbackSlots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(claim, "This puts the rook on c3.")
    assertEquals(fallbackSlots.supportPrimary, None)
    assertEquals(fallbackSlots.paragraphPlan, List("p1=claim"))
  }

  test("moveReview does not attach uncertified decision-frame support when only fallback survives") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
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
          ),
        strategicPlanEvidence = PlanEvidenceEvaluator.StrategicPlanEvidenceView.empty
      )
    val strategyPack = surfaceDrivenPack(routePurpose = "kingside pressure", targetSquare = "g7")
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        genericDecisionOutline("This increases pressure on g7.", "The route stays concrete."),
        refs = None,
        strategyPack = Some(strategyPack)
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "This puts the rook on c3.")
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.supportSecondary, None)
  }

  test("moveReview fails closed when concrete tactical authority owns a WhyNow request") {
    val ctx =
      tacticalCtx(MoveReviewProseGoldenFixtures.openFileFight.ctx).copy(
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
              reasonFamily = DecisiveReasonKind.TacticalRefutation
            )
          )
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallback(
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
              reasonFamily = DecisiveReasonKind.TacticalRefutation
            )
          )
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("state-only structure fixture falls back to exact local facts instead of generic strategic prose") {
    val fixture = MoveReviewProseGoldenFixtures.entrenchedPiece
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)
    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "This puts the knight on f1.")
  }

  test("quiet intent ignores break-ready hints and refuses to emit break-preparation prose") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
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
      MoveReviewPolishSlots(
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
    val repaired = MoveReviewSoftRepair.repair(broken, slots)
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
      MoveReviewPolishSlots(
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
    val repaired = MoveReviewSoftRepair.repair(generic, slots)
    assert(repaired.applied)
    assert(repaired.actions.contains("claim_restore"))
    assert(!repaired.materialApplied)
    assertEquals(repaired.materialActions, Nil)
  }

  test("soft repair keeps natural claim paraphrase when concrete move anchor remains") {
    val slots =
      MoveReviewPolishSlots(
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
    val natural =
      """White takes the center with e4.
        |
        |It opens lines for both bishops.""".stripMargin
    val repaired = MoveReviewSoftRepair.repair(natural, slots)
    assert(!repaired.actions.contains("claim_restore"), clues(repaired.actions, repaired.text))
    assert(repaired.text.startsWith("White takes the center with e4."), clues(repaired.text))
    assert(repaired.evaluation.claimLikeFirstParagraph, clues(repaired.evaluation, repaired.text))
  }

  test("deterministic third paragraph wraps bare variation evidence in prose") {
    val slots =
      MoveReviewPolishSlots(
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
    val paragraphs = MoveReviewSoftRepair.deterministicParagraphs(slots)
    assertEquals(paragraphs.size, 3)
    assert(paragraphs(2).contains("One concrete line that keeps the idea in play is a) ...c5 Nf3 Nc6 (+0.2)"))
  }

  test("sanitizer preserves chess ellipsis markers in prose") {
    val raw =
      "Probe evidence starts with. Rc8: Rc8 Rc3 Rg6 (+0.42). The alternative is 12.. Qh5, and fixing lines with.. h5 makes.. Rh6-g6 realistic."
    val sanitized = UserFacingSignalSanitizer.sanitize(raw)
    assertEquals(
      sanitized,
      "Probe evidence starts with ...Rc8: Rc8 Rc3 Rg6 (+0.42). The alternative is 12...Qh5, and fixing lines with ...h5 makes ...Rh6-g6 realistic."
    )
  }

  test("label-only tactical blunder WhyThis does not produce direct planner slots") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
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
      MoveReviewPolishSlotsBuilder.build(
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
              reasonFamily = DecisiveReasonKind.TacticalRefutation
            )
          )
      )
    assertEquals(slots, None)
  }

  test("compensation slots may omit moveReview prose instead of inventing a stronger compensation thesis") {
    val fixture = MoveReviewProseGoldenFixtures.exchangeSacrifice
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slotsOpt =
      MoveReviewPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)

    assert(
      slotsOpt.forall { slots =>
        val rendered = (slots.claim :: slots.support).appended(slots.tension.getOrElse("")).mkString(" ").toLowerCase
        !rendered.contains("180cp")
      }
    )
  }

  test("compensation omission path avoids broken compensation skeleton fragments") {
    val fixture = MoveReviewProseGoldenFixtures.exchangeSacrifice
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slotsOpt =
      MoveReviewPolishSlotsBuilder.build(fixture.ctx, outline, refs = None, strategyPack = fixture.strategyPack)

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
    val ctx = MoveReviewProseGoldenFixtures.openFileFight.ctx
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
      MoveReviewPolishSlotsBuilder.buildOrFallback(ctx, outline, refs = None, strategyPack = weakPack)

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("surface-only strategic pack cannot revive a moveReview thesis beyond exact factual fallback") {
    val ctx = MoveReviewProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(ctx, outline, refs = None, strategyPack = Some(surfaceDrivenPack()))

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("pv-coupled support traces cannot prevent exact factual fallback") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
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
          )
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
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = Some(shellPack)
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("uncertified restricted-defense conversion falls back instead of reviving a technical conversion claim") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
        playedMove = Some("c3c4"),
        playedSan = Some("Rc4"),
        phase = PhaseContext("Endgame", "Technical conversion edge"),
        authorQuestions = Nil,
        authorEvidence = Nil,
        mainStrategicPlans =
          List(
            PlanHypothesis(
              planId = "simplification_conversion",
              planName = "Simplify into a winning pawn ending",
              rank = 1,
              score = 0.82,
              preconditions = Nil,
              executionSteps = List("Trade the last active defender."),
              failureModes = List("Wrong move order restores counterplay."),
              viability = PlanViability(score = 0.8, label = "high", risk = "test"),
              evidenceSources = List("theme:advantage_transformation"),
              themeL1 = PlanTaxonomy.PlanTheme.AdvantageTransformation.id,
              subplanId = Some(PlanTaxonomy.PlanKind.SimplificationConversion.id)
            )
          ),
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "simplification_conversion",
              themeL1 = PlanTaxonomy.PlanTheme.AdvantageTransformation.id,
              subplanId = Some(PlanTaxonomy.PlanKind.SimplificationConversion.id),
              evidenceTier = "deferred",
              supportProbeCount = 1,
              bestReplyStable = true,
              futureSnapshotAligned = true
            )
          ),
        strategicPlanEvidence = PlanEvidenceEvaluator.StrategicPlanEvidenceView.empty
      )
    val outline =
      genericDecisionOutline(
        "A quiet conversion move.",
        "The edge still needs clean technique."
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallback(
        ctx = ctx,
        outline = outline,
        refs = None,
        strategyPack = None
      )

    assertExactFactualFallback(slots, "This puts the rook on c4.")
    assertNoStateSummaryLeak(slots.claim)
  }

  test("EVA01-style surface pack drops unsupported routed thesis and keeps only exact factual fallback") {
    val ctx = tacticalCtx(MoveReviewProseGoldenFixtures.openFileFight.ctx)
    val outline =
      genericDecisionOutline(
        "The whole decision turns on b7.",
        "The practical alternative Ng5 stays secondary because the attack is not ready."
      )
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
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
    val ctx = tacticalCtx(MoveReviewProseGoldenFixtures.openFileFight.ctx)
    val outline =
      genericDecisionOutline(
        "The whole decision turns on e5.",
        "The practical alternative Qd2 stays secondary because the attack is not ready."
      )
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
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
    val ctx = MoveReviewProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
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
    val ctx = MoveReviewProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
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
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil,
        strategicSalience = lila.commentary.model.strategic.StrategicSalience.Low
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
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = shellOnlyPack
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("quiet bogus tactical labels collapse to an exact factual one-liner") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil,
        strategicSalience = lila.commentary.model.strategic.StrategicSalience.Low
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
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = shellOnlyPack
      )

    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("only-move defense can render a typed WhyNow surface without route prose") {
    val ctx =
      tacticalCtx(MoveReviewProseGoldenFixtures.openFileFight.ctx).copy(
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
      MoveReviewCompressionPolicy.buildSlotsOrFallback(
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
              reasonFamily = DecisiveReasonKind.OnlyMoveDefense
            )
          )
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    val rendered = MoveReviewProseContract.stripMoveHeader(slots.validationSeedText).toLowerCase
    assert(rendered.contains("now") || rendered.contains("qxf7"), clues(rendered, slots))
    assert(!rendered.contains("queen route"), clues(rendered, slots))
    assert(!rendered.contains("route toward"), clues(rendered, slots))
    assert(!rendered.contains("e5"), clues(rendered, slots))
  }

  test("moveReview does not keep defensive primary from scoped threat text without typed proof") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
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
          reasonFamily = DecisiveReasonKind.OnlyMoveDefense
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
      MoveReviewCompressionPolicy.renderSelection(inputs, rankedPlans, contract)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = contract
      )

    assertEquals(rankedPlans.primary, None, clues(rankedPlans))
    assertEquals(renderSelection, None, clues(renderSelection))
    assertExactFactualFallback(slots, "This puts the rook on c3.")
  }

  test("neutral truth contract drops raw compensation prose and falls back to exact move semantics") {
    val ctx = MoveReviewProseGoldenFixtures.openFileFight.ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val rawCompensationPack =
      Some(
        surfaceDrivenPack(
          compensation = Some("pressure on g7"),
          investedMaterial = Some(100)
        )
      )

    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
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

  test("direct moveReview fallback returns an exact factual one-liner when quiet taxonomy also fails") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
        playedSan = Some("Qxc6"),
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil,
        pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet")
      )
    val outline = genericDecisionOutline("A capture.", "Nothing else is stable.")
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None
      )

    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "This captures on c6.")
    assertEquals(slots.paragraphPlan, List("p1=claim"))
  }

  test("direct moveReview fallback uses legal board truth to name the captured piece") {
    val fen = "7k/8/2n5/8/2Q5/8/8/4K3 w - - 0 1"
    val ctx =
      localFallbackCtx(
        fen = fen,
        playedMove = "c4c6",
        playedSan = "Qxc6"
      )
    val outline = genericDecisionOutline("A capture.", "Nothing else is stable.")
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None
      )

    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "This captures the knight on c6.")
    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(slots.supportPrimary, Some("The local material change is a captured knight."))
    assertEquals(slots.paragraphPlan, List("p1=claim", "p2=support_chain"))
  }

  test("direct moveReview fallback names only legal local pawn and promotion facts") {
    val pawnCtx =
      localFallbackCtx(
        fen = "4k3/8/8/8/8/8/P7/4K3 w - - 0 1",
        playedMove = "a2a3",
        playedSan = "a3"
      )
    val promotionCtx =
      localFallbackCtx(
        fen = "8/4P3/k7/8/8/8/8/4K3 w - - 0 1",
        playedMove = "e7e8q",
        playedSan = "e8=Q"
      )
    val outline = genericDecisionOutline("A local move.", "Nothing else is stable.")
    val pawnSlots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(pawnCtx, outline, refs = None, strategyPack = None)
    val promotionSlots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(promotionCtx, outline, refs = None, strategyPack = None)

    assertEquals(MoveReviewProseContract.stripMoveHeader(pawnSlots.claim), "This moves the pawn to a3.")
    assertEquals(MoveReviewProseContract.stripMoveHeader(promotionSlots.claim), "This promotes to a queen on e8.")
    assertEquals(pawnSlots.supportPrimary, None)
    assertEquals(promotionSlots.supportPrimary, Some("The local material change is a pawn becoming a queen."))
  }

  test("direct moveReview surface adds coupled PV line consequence only when the line replays") {
    val fen = "7k/8/2n5/8/2Q5/8/8/4K3 w - - 0 1"
    val ctx =
      localFallbackCtx(
        fen = fen,
        playedMove = "c4c6",
        playedSan = "Qxc6"
      )
    val refs =
      legalRefsForLine(
        fen,
        List("c4c6", "h8g8", "c6d6"),
        List("Qxc6", "Kg8", "Qd6")
      )
    val corrupted =
      refs.copy(
        variations =
          refs.variations.map(line =>
            line.copy(moves = line.moves.updated(1, line.moves(1).copy(fenAfter = line.moves.head.fenAfter)))
          )
      )
    val outline = genericDecisionOutline("A capture.", "Nothing else is stable.")
    val supported =
      MoveReviewPolishSlotsBuilder.buildOrFallback(ctx, outline, refs = Some(refs), strategyPack = None)
    val unsupported =
      MoveReviewPolishSlotsBuilder.buildOrFallback(ctx, outline, refs = Some(corrupted), strategyPack = None)

    val supportedClaim = MoveReviewProseContract.stripMoveHeader(supported.claim)
    assertEquals(supported.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assertEquals(supported.localFact.map(_.family), Some(MoveReviewLocalFact.Family.LineConsequence), clues(supported.localFact))
    assert(supportedClaim.contains("Qxc6 is tied to a checked material transition"), clues(supportedClaim, supported))
    assert(supportedClaim.contains("Kg8") && supportedClaim.contains("Qd6"), clues(supportedClaim, supported))
    assertEquals(supported.supportPrimary, None)
    assertEquals(supported.evidenceHook, Some("Short line: Qxc6 Kg8 Qd6."))
    assertEquals(supported.paragraphPlan, List("p1=claim", "p2=cited_line"))
    assertEquals(unsupported.supportPrimary, Some("The local material change is a captured knight."))
    assertEquals(unsupported.paragraphPlan, List("p1=claim", "p2=support_chain"))
  }

  test("typed line consequence surface does not read surface-only strategy pack prose") {
    val fen = "7k/8/2n5/8/2Q5/8/8/4K3 w - - 0 1"
    val ctx =
      localFallbackCtx(
        fen = fen,
        playedMove = "c4c6",
        playedSan = "Qxc6"
      )
    val refs =
      legalRefsForLine(
        fen,
        List("c4c6", "h8g8", "c6d6"),
        List("Qxc6", "Kg8", "Qd6")
      )
    val strategyPack =
      Some(
        surfaceDrivenPack(
          routePurpose = "compensation route pressure",
          targetSquare = "g7",
          compensation = Some("compensation for material")
        )
      )
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        genericDecisionOutline("A capture.", "Nothing else is stable."),
        refs = Some(refs),
        strategyPack = strategyPack
      )
    val renderedFallback =
      (List(MoveReviewProseContract.stripMoveHeader(slots.claim)) ++ slots.supportPrimary.toList)
        .mkString(" ")
        .toLowerCase

    val claim = MoveReviewProseContract.stripMoveHeader(slots.claim)
    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assertEquals(slots.localFact.map(_.family), Some(MoveReviewLocalFact.Family.LineConsequence), clues(slots.localFact))
    assert(claim.contains("Qxc6 is tied to a checked material transition"), clues(claim, slots))
    assert(claim.contains("Kg8") && claim.contains("Qd6"), clues(claim, slots))
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.evidenceHook, Some("Short line: Qxc6 Kg8 Qd6."))
    assert(!renderedFallback.contains("compensation"), clue(renderedFallback))
    assert(!renderedFallback.contains("pressure"), clue(renderedFallback))
    assert(!renderedFallback.contains("route"), clue(renderedFallback))
    assert(!renderedFallback.contains("g7"), clue(renderedFallback))
  }

  test("direct moveReview fallback names special capture and promotion material facts") {
    val enPassant =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        localFallbackCtx(
          fen = "rnbqkbnr/pp2pppp/8/2ppP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3",
          playedMove = "e5d6",
          playedSan = "exd6"
        ),
        genericDecisionOutline("A local move.", "Nothing else is stable."),
        refs = None,
        strategyPack = None
      )
    val promotionCapture =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        localFallbackCtx(
          fen = "7r/6P1/k7/8/8/8/8/4K3 w - - 0 1",
          playedMove = "g7h8q",
          playedSan = "gxh8=Q"
        ),
        genericDecisionOutline("A local move.", "Nothing else is stable."),
        refs = None,
        strategyPack = None
      )

    assertEquals(MoveReviewProseContract.stripMoveHeader(enPassant.claim), "This captures the pawn en passant and lands on d6.")
    assertEquals(enPassant.supportPrimary, Some("The local material change is a captured pawn."))
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(promotionCapture.claim),
      "This captures the rook on h8 and promotes to a queen."
    )
    assertEquals(
      promotionCapture.supportPrimary,
      Some("The local material change is a captured rook plus a pawn becoming a queen.")
    )
  }

  test("direct moveReview fallback adds only board-local tactical check support") {
    val checkSlots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        localFallbackCtx(
          fen = "4k3/8/8/8/8/8/4R3/4K3 w - - 0 1",
          playedMove = "e2e7",
          playedSan = "Re7+"
        ),
        genericDecisionOutline("A local move.", "Nothing else is stable."),
        refs = None,
        strategyPack = None
      )
    val mateSlots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        localFallbackCtx(
          fen = "7k/8/5KQ1/8/8/8/8/8 w - - 0 1",
          playedMove = "g6g7",
          playedSan = "Qg7#"
        ),
        genericDecisionOutline("A local move.", "Nothing else is stable."),
        refs = None,
        strategyPack = None
      )

    assertEquals(MoveReviewProseContract.stripMoveHeader(checkSlots.claim), "This moves the rook from e2 to e7.")
    assertEquals(checkSlots.supportPrimary, Some("It also gives check."))
    assertEquals(MoveReviewProseContract.stripMoveHeader(mateSlots.claim), "This moves the queen from g6 to g7.")
    assertEquals(mateSlots.supportPrimary, Some("It also gives checkmate."))
  }

  test("direct moveReview fallback does not render candidate tactical motifs as exact facts") {
    val fen = "4k3/4r3/8/8/3N3q/8/8/2K5 w - - 0 1"
    val forkCandidate =
      CandidateInfo(
        move = "Nf5",
        uci = Some("d4f5"),
        annotation = "",
        planAlignment = "local tactic",
        tacticalAlert = None,
        practicalDifficulty = "clean",
        whyNot = None,
        lineMotifs =
          List(Motif.Fork(Knight, List(Rook, Queen), Square.F5, List(Square.E7, Square.H4), Color.White, 0, Some("Nf5")))
      )
    val laterForkCandidate =
      forkCandidate.copy(lineMotifs =
        List(Motif.Fork(Knight, List(Rook, Queen), Square.F5, List(Square.E7, Square.H4), Color.White, 1, Some("Nf5")))
      )
    val quietMotifCandidate =
      forkCandidate.copy(lineMotifs =
        List(Motif.Centralization(Knight, Square.F5, Color.White, 0, Some("Nf5")))
      )
    val baseCtx =
      localFallbackCtx(
        fen = fen,
        playedMove = "d4f5",
        playedSan = "Nf5"
      )
    val outline = genericDecisionOutline("A local move.", "Nothing else is stable.")
    val owned =
      MoveReviewPolishSlotsBuilder.buildOrFallback(baseCtx.copy(candidates = List(forkCandidate)), outline, refs = None, strategyPack = None)
    val plyLater =
      MoveReviewPolishSlotsBuilder.buildOrFallback(baseCtx.copy(candidates = List(laterForkCandidate)), outline, refs = None, strategyPack = None)
    val quiet =
      MoveReviewPolishSlotsBuilder.buildOrFallback(baseCtx.copy(candidates = List(quietMotifCandidate)), outline, refs = None, strategyPack = None)

    assertEquals(MoveReviewProseContract.stripMoveHeader(owned.claim), "This moves the knight from d4 to f5.")
    assertEquals(owned.supportPrimary, None)
    assertEquals(plyLater.supportPrimary, None)
    assertEquals(quiet.supportPrimary, None)
    assert(!owned.factGuardrails.exists(_.contains("tactical motif")), clue(owned.factGuardrails))
  }

  test("direct moveReview fallback keeps ambiguous captures literal") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
        playedSan = Some("Qx"),
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil,
        pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet")
      )
    val outline = genericDecisionOutline("A capture.", "Nothing else is stable.")
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None
      )

    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "This captures.")
    assertEquals(slots.paragraphPlan, List("p1=claim"))
  }

  test("exact factual fallback keeps quiet-support diagnostic-only") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
        authorQuestions = Nil,
        authorEvidence = Nil
      )
    val strategyPack =
      Some(
        phaseAQuietSupportPack(
          deploymentRoute = List("c3", "g3"),
          structuralCue = None,
          practicalVerdict = None
        )
      )
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = strategyPack)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)
    val plannerSlots = MoveReviewCompressionPolicy.buildSlots(ctx, outline, refs = None, strategyPack = strategyPack)
    val quietTrace =
      MoveReviewCompressionPolicy.exactFactualQuietSupportTrace(
        ctx = ctx,
        refs = None,
        strategyPack = strategyPack
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallback(
        ctx = ctx,
        outline = outline,
        refs = None,
        strategyPack = strategyPack
      )

    assertEquals(plannerSlots, None, clues(rankedPlans))
    assertEquals(rankedPlans.primary, None, clues(rankedPlans))
    assertEquals(rankedPlans.ownerTrace.selectedQuestion, None, clues(rankedPlans.ownerTrace))
    assertEquals(rankedPlans.ownerTrace.selectedPlannerOwnerKind, None, clues(rankedPlans.ownerTrace))
    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "This puts the rook on c3.")
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.supportSecondary, None)
    assertEquals(slots.paragraphPlan, List("p1=claim"))
    assertEquals(quietTrace.liftApplied, false, clues(quietTrace))
    assert(quietTrace.rejectReasons.contains("quiet_support_diagnostic_only"), clues(quietTrace))
    assertEquals(quietTrace.composerTrace.gatePassed, true, clues(quietTrace))
    assertEquals(quietTrace.composerTrace.gate.sceneType, SceneType.TransitionConversion.wireName, clues(quietTrace))
    assertEquals(quietTrace.composerTrace.gate.pvDeltaAvailable, true, clues(quietTrace))
    assertEquals(quietTrace.composerTrace.gate.moveLinkedPvDeltaAnchorAvailable, true, clues(quietTrace))
    assertEquals(
      quietTrace.composerTrace.line.map(_.bucket),
      Some(QuietStrategicSupportComposer.Bucket.SlowRouteImprovement),
      clues(quietTrace)
    )
  }

  test("planner-owned moveReview rows skip quiet-support fallback lifting") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
        playedMove = Some("c1c2"),
        playedSan = Some("Rc2"),
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
        engineEvidence = Some(
          EngineEvidence(
            depth = 12,
            variations =
              List(
                VariationLine(List("c1c2", "c6c5", "d4c5", "c8c5", "f3d4"), scoreCp = 10, depth = 12)
              )
          )
        ),
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
          ),
        strategicPlanEvidence =
          StrategicPlanEvidenceTestSupport.probeBacked(
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
            )
          )
      )
    val strategyPack = Some(surfaceDrivenPack(routePurpose = "kingside pressure", targetSquare = "g7"))
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = strategyPack)
    val plannerOwned =
      MoveReviewCompressionPolicy
        .buildSlots(ctx, outline, refs = None, strategyPack = strategyPack)
        .getOrElse(fail(s"expected planner-owned slots; outline=${outline.diagnostics.map(_.summary)}; mainBundle=${QuestionPlannerInputsBuilder.build(ctx, strategyPack, None).mainBundle}; ranked=${QuestionFirstCommentaryPlanner.plan(ctx, QuestionPlannerInputsBuilder.build(ctx, strategyPack, None), None).primary.map(_.questionKind)}; rejected=${QuestionFirstCommentaryPlanner.plan(ctx, QuestionPlannerInputsBuilder.build(ctx, strategyPack, None), None).rejected.map(r => r.questionKind -> r.reasons)}"))
    val fallbackAware =
      MoveReviewCompressionPolicy.buildSlotsOrFallback(
        ctx = ctx,
        outline = outline,
        refs = None,
        strategyPack = strategyPack
      )

    assertEquals(fallbackAware, plannerOwned)
  }

  test("line-scoped tactical proof alone no longer owns moveReview prose and falls back to exact move semantics") {
    val ctx =
      MoveReviewProseGoldenFixtures.openFileFight.ctx.copy(
        semantic = None,
        decision = None,
        mainStrategicPlans = Nil,
        strategicPlanExperiments = Nil
      )
    val outline =
      genericDecisionOutline(
        "A quiet move.",
        "Nothing concrete changes yet."
      )
    val refs =
      Some(
        MoveReviewRefs(
          startFen = ctx.fen,
          startPly = ctx.ply,
          variations = List(
            MoveReviewVariationRef(
              lineId = "pv1",
              scoreCp = 42,
              mate = None,
              depth = 18,
              moves = List(
                MoveReviewMoveRef("m1", "Qxf7+", "f3f7", ctx.fen, ctx.ply + 1, (ctx.ply + 2) / 2, None),
                MoveReviewMoveRef("m2", "Kxf7", "g8f7", ctx.fen, ctx.ply + 2, (ctx.ply + 3) / 2, None),
                MoveReviewMoveRef("m3", "Bxd5+", "c4d5", ctx.fen, ctx.ply + 3, (ctx.ply + 4) / 2, None)
              )
            )
          )
        )
      )

    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(ctx, outline, refs = refs, strategyPack = None)

    val claim = MoveReviewProseContract.stripMoveHeader(slots.claim)
    assertEquals(claim, "This puts the rook on c3.")
    assertEquals(slots.evidenceHook, None)
  }
