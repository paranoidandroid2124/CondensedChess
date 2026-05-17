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
      bookmaker: String,
      chronicle: String
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
    assertEquals(sentenceOccurrences(scene.bookmaker, claim.claimText), 1, clues(scene.bookmaker))
    assertEquals(sentenceOccurrences(scene.chronicle, claim.claimText), 1, clues(scene.chronicle))
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

  test("central-break timing stays closed when the PV timing gap is below forty centipawns") {
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

    assertNoCentralRelease(scene)
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
    val bookmaker =
      LiveNarrativeCompressionCore.deterministicProse(
        BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
          ctx = ctx,
          outline = outline,
          refs = None,
          strategyPack = Some(pack),
          truthContract = truthContract
        )
      )
    val chronicle =
      GameChronicleCompressionPolicy
        .renderWithTrace(
          ctx = ctx,
          parts = emptyParts.copy(focusedOutline = outline),
          strategyPack = Some(pack),
          truthContract = truthContract
        )
        .map(_.narrative)
        .getOrElse("-")
    Snapshot(ctx, pack, inputs, ranked, bookmaker, chronicle)

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
