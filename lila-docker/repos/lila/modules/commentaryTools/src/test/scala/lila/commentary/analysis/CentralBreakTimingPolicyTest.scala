package lila.commentary.analysis

import lila.commentary.model.*
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind, NarrativeOutline, PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.VariationLine
import munit.FunSuite

class CentralBreakTimingPolicyTest extends FunSuite:

  private final case class Snapshot(
      ctx: NarrativeContext,
      pack: lila.commentary.StrategyPack,
      inputs: QuestionPlannerInputs,
      ranked: RankedQuestionPlans,
      moveReview: String
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
  private val DirectBreakLines =
    List(
      VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 72, depth = 18),
      VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 28, depth = 18)
    )

  test("Maderna-Palermo exact row owns the direct central-break timing packet") {
    val scene =
      snapshot(
        fen = MadernaExactFen,
        ply = 33,
        playedMove = "e4e5",
        lines = MadernaExactLines
      )
    val packet = centralPacket(scene)

    assertEquals(packet.proofSource, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assertEquals(packet.scope, PlayerFacingPacketScope.MoveLocal)
    assertEquals(packet.fallbackMode, PlayerFacingClaimFallbackMode.WeakMain)
    assertEquals(packet.sameBranchState, PlayerFacingSameBranchState.Proven)
    assertEquals(packet.persistence, PlayerFacingClaimPersistence.Stable)
    assert(scene.ranked.primary.exists(_.plannerSource == PlanTaxonomy.PlanKind.CentralBreakTiming.id), clues(scene.ranked))
  }

  test("central-break runtime witness does not stamp historical fixture tags") {
    val scene =
      snapshot(
        fen = MadernaExactFen,
        ply = 33,
        playedMove = "e4e5",
        lines = MadernaExactLines
      )
    val witness =
      CentralBreakTimingWitness
        .exact(scene.ctx)
        .getOrElse(fail(s"missing central-break witness: ${CentralBreakTimingWitness.diagnose(scene.ctx)}"))

    assert(!witness.sourceTags.exists(_.contains("maderna-palermo")), clues(witness.sourceTags))
    assert(!witness.ownerSeedTerms.exists(_.contains("maderna-palermo")), clues(witness.ownerSeedTerms))
  }

  test("board-backed central break can materialize without a source-row whitelist") {
    val scene =
      snapshot(
        fen = DirectBreakFen,
        ply = 15,
        playedMove = "d4d5",
        lines = DirectBreakLines
      )
    val packet = centralPacket(scene)

    assertEquals(packet.proofSource, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assert(packet.proofPathWitness.ownerSeedTerms.exists(_.contains("d5")), clues(packet))
    assert(packet.proofPathWitness.structureTransitionTerms.nonEmpty, clues(packet))
  }

  test("exact row and board support overlap keeps one canonical packet and no duplicate player prose") {
    val scene =
      snapshot(
        fen = MadernaExactFen,
        ply = 33,
        playedMove = "e4e5",
        lines = MadernaExactLines
      )
    val claim =
      scene.inputs.mainBundle
        .flatMap(_.mainClaim)
        .getOrElse(fail("central-break overlap should leave one admitted main claim"))

    assertEquals(claim.packet.map(_.proofSource), Some(PlanTaxonomy.PlanKind.CentralBreakTiming.id))
    assertEquals(scene.ranked.primary.map(_.claim), Some(claim.claimText))
    assertEquals(sentenceOccurrences(scene.moveReview, claim.claimText), 1, clues(scene.moveReview))
  }

  test("tactical-first ownership vetoes central-break timing prose") {
    val scene =
      snapshot(
        fen = MadernaExactFen,
        ply = 33,
        playedMove = "e4e5",
        lines = MadernaExactLines,
        truthContract = Some(tacticalTruthContract())
      )

    assertEquals(PlayerFacingTruthModePolicy.classify(scene.ctx, Some(scene.pack), Some(tacticalTruthContract())), PlayerFacingTruthMode.Tactical)
    assertNoCentralRelease(scene)
  }

  test("plan support without a direct board link does not release") {
    val scene =
      snapshot(
        fen = DirectBreakFen,
        ply = 15,
        playedMove = "b1c3",
        lines =
          List(
            VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 72, depth = 18),
            VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 28, depth = 18)
          )
      )

    assertNoCentralRelease(scene)
  }

  test("central-break timing releases when the board-backed break is not best-line-exclusive") {
    val scene =
      snapshot(
        fen = DirectBreakFen,
        ply = 15,
        playedMove = "d4d5",
        lines =
          List(
            VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 68, depth = 18),
            VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 31, depth = 18)
          )
      )

    val packet = centralPacket(scene)

    assertEquals(packet.proofSource, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assert(scene.ranked.primary.exists(_.plannerSource == PlanTaxonomy.PlanKind.CentralBreakTiming.id), clues(scene.ranked))
  }

  test("rival-family relabel does not promote a central-break packet") {
    val rivalPlan =
      PlanHypothesis(
        planId = "rival_break_prevention",
        planName = "Keep the opponent's break contained",
        rank = 1,
        score = 0.82,
        preconditions = Nil,
        executionSteps = List("Keep the route closed."),
        failureModes = Nil,
        viability = PlanViability(score = 0.8, label = "high", risk = "test"),
        evidenceSources = List(s"theme:${PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id}"),
        themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
        subplanId = Some(PlanTaxonomy.PlanKind.BreakPrevention.id)
      )
    val rivalExperiment =
      StrategicPlanExperiment(
        planId = rivalPlan.planId,
        themeL1 = rivalPlan.themeL1,
        subplanId = rivalPlan.subplanId,
        evidenceTier = "evidence_backed",
        supportProbeCount = 1,
        refuteProbeCount = 0,
        bestReplyStable = true,
        futureSnapshotAligned = true,
        counterBreakNeutralized = true,
        moveOrderSensitive = false,
        experimentConfidence = 0.86
      )
    val scene =
      snapshot(
        fen = DirectBreakFen,
        ply = 15,
        playedMove = "d4d5",
        lines = DirectBreakLines,
        mutate = _.copy(
          mainStrategicPlans = List(rivalPlan),
          strategicPlanExperiments = List(rivalExperiment)
        )
      )

    assertNoCentralRelease(scene)
  }

  test("central-break timing release does not require a two-move branch key") {
    val scene =
      snapshot(
        fen = DirectBreakFen,
        ply = 15,
        playedMove = "d4d5",
        lines =
          List(
            VariationLine(List("d4d5"), scoreCp = 0, depth = 18),
            VariationLine(List("b1c3"), scoreCp = 0, depth = 18)
          )
      )
    val packet = centralPacket(scene)

    assertEquals(packet.bestDefenseBranchKey, None)
    assertEquals(packet.proofSource, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
  }

  test("played same-file central break releases even when top PV omits that break") {
    val scene =
      snapshot(
        fen = DirectBreakFen,
        ply = 15,
        playedMove = "d4d5",
        lines =
          List(
            VariationLine(List("b1c3", "e8g8", "h2h3"), scoreCp = 0, depth = 18)
          )
      )
    val packet = centralPacket(scene)
    val witness =
      CentralBreakTimingWitness
        .exact(scene.ctx)
        .getOrElse(fail(s"missing central-break witness: ${CentralBreakTimingWitness.diagnose(scene.ctx)}"))

    assertEquals(packet.proofSource, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assertEquals(packet.proofFamily, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assertEquals(witness.breakMove, "d4d5")
    assert(witness.sourceTags.contains("board:played_break"), clues(witness))
    assert(witness.sourceTags.contains("board:played_move_direct"), clues(witness))
  }

  test("diagonal central captures stay outside central-break timing release") {
    val diagonalCaptureFen =
      "r2q1rk1/pp2npb1/2p1b1pp/3pB3/3PN3/1BP2N2/PP3PPP/R2Q1RK1 b - - 0 15"
    val scene =
      snapshot(
        fen = diagonalCaptureFen,
        ply = 30,
        playedMove = "d5e4",
        lines =
          List(
            VariationLine(List("d5e4"), scoreCp = 0, depth = 18),
            VariationLine(List("g7e5"), scoreCp = 0, depth = 18)
          )
      )

    assertNoCentralRelease(scene)
    assert(
      CentralBreakTimingWitness
        .diagnose(scene.ctx)
        .failureCodes
        .contains("central_break_timing:diagonal_capture_liquidation"),
      clue(CentralBreakTimingWitness.diagnose(scene.ctx))
    )
  }

  test("prep or challenge pawn moves stay outside central-break timing release") {
    val prepFen =
      "rnbqkbnr/pp1ppppp/8/2p1P3/2B5/5Q2/PPPP1PPP/RNB1K1NR b KQkq - 2 4"
    val scene =
      snapshot(
        fen = prepFen,
        ply = 8,
        playedMove = "d7d6",
        lines =
          List(
            VariationLine(List("d7d6"), scoreCp = 0, depth = 18),
            VariationLine(List("b8c6"), scoreCp = 0, depth = 18)
          )
      )

    assertNoCentralRelease(scene)
    assert(
      CentralBreakTimingWitness
        .diagnose(scene.ctx)
        .failureCodes
        .contains("central_break_timing:prep_or_challenge"),
      clue(CentralBreakTimingWitness.diagnose(scene.ctx))
    )
  }

  test("c-pawn and f-pawn opening breaks stay outside exact central-break timing") {
    val cPawnScene =
      snapshot(
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        ply = 2,
        playedMove = "c7c5",
        lines = List(VariationLine(List("c7c5", "g1f3"), scoreCp = 0, depth = 18))
      )
    val fPawnScene =
      snapshot(
        fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
        ply = 3,
        playedMove = "f2f4",
        lines = List(VariationLine(List("f2f4", "e5f4"), scoreCp = 0, depth = 18))
      )

    assertNoCentralRelease(cPawnScene)
    assertNoCentralRelease(fPawnScene)
    assert(
      CentralBreakTimingWitness.diagnose(cPawnScene.ctx).failureCodes.contains(CentralBreakTimingWitness.Failure.NoCentralBreak),
      clue(CentralBreakTimingWitness.diagnose(cPawnScene.ctx))
    )
    assert(
      CentralBreakTimingWitness.diagnose(fPawnScene.ctx).failureCodes.contains(CentralBreakTimingWitness.Failure.NoCentralBreak),
      clue(CentralBreakTimingWitness.diagnose(fPawnScene.ctx))
    )
  }

  private def snapshot(
      fen: String,
      ply: Int,
      playedMove: String,
      lines: List[VariationLine],
      truthContract: Option[DecisiveTruthContract] = None,
      mutate: NarrativeContext => NarrativeContext = identity
  ): Snapshot =
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
    val ctx =
      mutate(
        NarrativeContextBuilder
          .build(data, data.toContext, None)
          .copy(authorQuestions = defaultQuestions)
      )
    val pack =
      StrategyPackBuilder
        .build(data, ctx)
        .getOrElse(fail(s"strategy pack missing for $fen"))
    val inputs = QuestionPlannerInputsBuilder.build(ctx, Some(pack), truthContract)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract)
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = Some(pack), truthContract = truthContract)
    val moveReview =
      LiveNarrativeCompressionCore.deterministicProse(
        MoveReviewCompressionPolicy.buildSlotsOrFallback(
          ctx = ctx,
          outline = outline,
          refs = None,
          strategyPack = Some(pack),
          truthContract = truthContract
        )
      )
    Snapshot(ctx, pack, inputs, ranked, moveReview)

  private def centralPacket(scene: Snapshot): PlayerFacingClaimPacket =
    scene.inputs.mainBundle
      .flatMap(_.mainClaim)
      .flatMap(_.packet)
      .filter(_.proofFamily == PlanTaxonomy.PlanKind.CentralBreakTiming.id)
      .getOrElse(fail(s"missing central-break packet: ${scene.inputs.mainBundle}; witness=${CentralBreakTimingWitness.diagnose(scene.ctx)}"))

  private def assertNoCentralRelease(scene: Snapshot): Unit =
    assert(
      scene.inputs.mainBundle.flatMap(_.mainClaim).flatMap(_.packet).forall(_.proofFamily != PlanTaxonomy.PlanKind.CentralBreakTiming.id),
      clues(scene.inputs.mainBundle)
    )
    assert(
      !scene.ranked.primary.exists(_.plannerSource == PlanTaxonomy.PlanKind.CentralBreakTiming.id),
      clues(scene.ranked)
    )

  private def sentenceOccurrences(text: String, sentence: String): Int =
    Option(text).getOrElse("").sliding(sentence.length).count(_ == sentence)

  private def tacticalTruthContract(): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("e4e5"),
      verifiedBestMove = Some("e4e5"),
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

  private val defaultQuestions =
    List(
      AuthorQuestion("why_this", AuthorQuestionKind.WhyThis, 100, "Why this move?"),
      AuthorQuestion("what_matters_here", AuthorQuestionKind.WhatMattersHere, 90, "What matters here?"),
      AuthorQuestion("what_changed", AuthorQuestionKind.WhatChanged, 80, "What changed?"),
      AuthorQuestion("why_now", AuthorQuestionKind.WhyNow, 60, "Why now?")
    )


