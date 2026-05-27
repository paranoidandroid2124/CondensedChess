package lila.commentary.analysis

import lila.commentary.*
import lila.commentary.model.*
import munit.FunSuite

final class MoveReviewCompressionPolicyTest extends FunSuite:

  private def quietH3Ctx: NarrativeContext =
    NarrativeContext(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      header = ContextHeader("Opening", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 1,
      playedMove = Some("h2h3"),
      playedSan = Some("h3"),
      summary = NarrativeSummary("quiet move", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Opening", "quiet opening move"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.MoveReview
    )

  private val italianBeforeBc4 =
    "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"

  private def italianCtx: NarrativeContext =
    NarrativeContext(
      fen = italianBeforeBc4,
      header = ContextHeader("Opening", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 5,
      playedMove = Some("f1c4"),
      playedSan = Some("Bc4"),
      summary = NarrativeSummary("Italian Game development", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Opening", "Italian Game development"),
      candidates = Nil,
      openingData = Some(
        OpeningReference(
          eco = Some("C50"),
          name = Some("Italian Game"),
          totalGames = 420000,
          topMoves = List(ExplorerMove("f1c4", "Bc4", 210000, 93000, 52000, 65000, 2460)),
          sampleGames = Nil
        )
      ),
      openingGoalEvaluation = Some(
        OpeningGoals.Evaluation(
          goalName = "Development Logic",
          status = OpeningGoals.Status.Achieved,
          supportedEvidence = List("Minor piece developed"),
          missingEvidence = Nil,
          confidence = 0.86
        )
      ),
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def refsForLine(startFen: String, ucis: List[String], sans: List[String]): MoveReviewRefs =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewRefs(
      startFen = startFen,
      startPly = NarrativeUtils.plyFromFen(startFen).map(_ + 1).getOrElse(1),
      variations = List(
        MoveReviewVariationRef(
          lineId = "line_01",
          scoreCp = 16,
          mate = None,
          depth = 16,
          moves =
            ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
              val ply = NarrativeUtils.plyFromFen(startFen).map(_ + 1 + idx).getOrElse(idx + 1)
              MoveReviewMoveRef(
                refId = s"line_01_m${idx + 1}",
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

  test("basic lane stays closed when no primitive is safe and exact factual fallback remains") {
    val ctx = quietH3Ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val explanation = MoveReviewExplanationBuilder.build(ctx, None)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )

    assertEquals(explanation, None, clues(explanation))
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3.",
      clues(slots)
    )
    assertEquals(slots.paragraphPlan, List("p1=claim"), clues(slots))
    assertEquals(slots.moveReviewExplanation, None, clues(slots))
  }

  test("basic lane carries the same move review explanation used for visible slots") {
    val ctx = italianCtx
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        BookStyleRenderer.validatedOutline(ctx),
        refs = Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))),
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.BasicMoveExplanation, clues(slots))
    assert(slots.moveReviewExplanation.exists(_.source == "opening_goal"), clues(slots))
    assert(slots.moveReviewExplanation.exists(_.reasonTags.contains("review_intent:normal_development")), clues(slots))
    assert(slots.factGuardrails.exists(_ == "MoveReview review intent: normal_development"), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_ == "MoveReview character band: neutral"), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_ == "MoveReview line proof: opening_goal"), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_ == "MoveReview PV subject: f1c4"), clues(slots.factGuardrails))
    assert(slots.moveReviewExplanation.exists(explanation => slots.claim.contains(explanation.prose.take(24).trim)), clues(slots))
  }

  test("existing planner-positive fixture still outranks the new basic lane") {
    val fixture =
      MoveReviewProseGoldenFixtures.plannerRuntimeFixtures.find(_.expectedClaimFragment.nonEmpty).get
    val outline =
      BookStyleRenderer.validatedOutline(
        fixture.ctx,
        strategyPack = fixture.strategyPack,
        truthContract = fixture.truthContract
      )
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        fixture.ctx,
        outline,
        refs = None,
        strategyPack = fixture.strategyPack,
        truthContract = fixture.truthContract
      )
    val claim = MoveReviewProseContract.stripMoveHeader(slots.claim).toLowerCase

    assertNotEquals(slots.sourceKind, "basic_move_explanation", clues(fixture.id, slots))
    assertEquals(slots.moveReviewExplanation, None, clues(fixture.id, slots))
    assert(
      claim.contains(fixture.expectedClaimFragment.get.toLowerCase),
      clues(fixture.id, claim, slots)
    )
    assertNotEquals(slots.paragraphPlan, List("p1=claim"), clues(fixture.id, slots))
  }

  test("thematic fallback is selected when strategic plans are active but exact witness proof fails") {
    val ctx = quietH3Ctx.copy(
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "OpenFilePressure",
            score = 0.92,
            evidence = List("rook doubling on open d-file"),
            supports = Nil,
            blockers = Nil,
            missingPrereqs = Nil
          )
        ),
        suppressed = Nil
      )
    )
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ThematicFallback)
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "The strategic plan is to activate the pieces and find more active squares for them."
    )
    assertEquals(slots.paragraphPlan, List("p1=claim"))
  }

  test("thematic fallback is blocked (fail-closed) when truthContract indicates a blunder or tactical refutation") {
    val ctx = quietH3Ctx.copy(
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "OpenFilePressure",
            score = 0.92,
            evidence = List("rook doubling on open d-file"),
            supports = Nil,
            blockers = Nil,
            missingPrereqs = Nil
          )
        ),
        suppressed = Nil
      )
    )
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val blunderContract = DecisiveTruthContract(
      playedMove = Some("h2h3"),
      verifiedBestMove = Some("e2e4"),
      truthClass = DecisiveTruthClass.Blunder,
      cpLoss = 300,
      swingSeverity = 300,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      allowConcreteBenchmark = false,
      chosenMatchesBest = false,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.BlunderOwner,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = true,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = false,
      failureMode = FailureInterpretationMode.TacticalRefutation,
      failureIntentConfidence = 0.9,
      failureIntentAnchor = None,
      failureInterpretationAllowed = true
    )

    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = Some(blunderContract)
      )

    assertNotEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ThematicFallback)
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3."
    )
  }

  test("thematic fallback remains available for non-tactical inaccuracy truth") {
    val ctx = quietH3Ctx.copy(
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "OpenFilePressure",
            score = 0.92,
            evidence = List("rook doubling on open d-file"),
            supports = Nil,
            blockers = Nil,
            missingPrereqs = Nil
          )
        ),
        suppressed = Nil
      )
    )
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val inaccuracyContract = DecisiveTruthContract(
      playedMove = Some("h2h3"),
      verifiedBestMove = Some("e2e4"),
      truthClass = DecisiveTruthClass.Inaccuracy,
      cpLoss = 45,
      swingSeverity = 45,
      reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
      allowConcreteBenchmark = false,
      chosenMatchesBest = false,
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
      benchmarkCriticalMove = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = Some(inaccuracyContract)
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ThematicFallback)
  }
