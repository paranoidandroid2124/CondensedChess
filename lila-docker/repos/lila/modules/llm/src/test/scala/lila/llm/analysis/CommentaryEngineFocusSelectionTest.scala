package lila.llm.analysis

import munit.FunSuite
import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*

class CommentaryEngineFocusSelectionTest extends FunSuite:

  private def countOccurrences(text: String, needle: String): Int =
    text.sliding(needle.length).count(_ == needle)

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

  test("focusMomentOutline keeps essential beats and highest-priority late beats") {
    val outline = NarrativeOutline(
      List(
        OutlineBeat(kind = OutlineBeatKind.MoveHeader, text = "12...Qe7"),
        OutlineBeat(kind = OutlineBeatKind.Context, text = "Context.", focusPriority = 100, fullGameEssential = true),
        OutlineBeat(kind = OutlineBeatKind.DecisionPoint, text = "Decision.", focusPriority = 96, fullGameEssential = true),
        OutlineBeat(kind = OutlineBeatKind.MainMove, text = "Main move.", focusPriority = 92, fullGameEssential = true),
        OutlineBeat(kind = OutlineBeatKind.Alternatives, text = "Alternatives.", focusPriority = 40),
        OutlineBeat(kind = OutlineBeatKind.WrapUp, text = "The position remains dynamically balanced.", focusPriority = 60),
        OutlineBeat(kind = OutlineBeatKind.OpeningTheory, text = "Opening theory.", focusPriority = 82),
        OutlineBeat(kind = OutlineBeatKind.ConditionalPlan, text = "Latent plan.", focusPriority = 84)
      )
    )

    val focused = CommentaryEngine.focusMomentOutline(outline, hasCriticalBranch = true)

    assertEquals(
      focused.beats.map(_.kind),
      List(
        OutlineBeatKind.Context,
        OutlineBeatKind.DecisionPoint,
        OutlineBeatKind.MainMove,
        OutlineBeatKind.OpeningTheory,
        OutlineBeatKind.ConditionalPlan
      )
    )
    assert(!focused.beats.exists(_.text.contains("dynamically balanced")))
  }

  test("latent plan beat survives with heuristic support when probes are absent") {
    val latentInfo = LatentPlanInfo(
      seedId = "latent_rook_lift",
      seedFamily = SeedFamily.Piece,
      narrative = NarrativeTemplate("A rook lift remains a conditional attacking idea if White gets time.")
    )
    val ctx =
      NarrativeContext(
        fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
        ply = 24,
        summary = NarrativeSummary("Kingside expansion", None, "NarrowChoice", "Maintain", "0.00"),
        threats = ThreatTable(Nil, Nil),
        pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
        plans = PlanTable(Nil, Nil),
        delta = None,
        phase = PhaseContext("Middlegame", "Balanced middlegame"),
        candidates = Nil,
        latentPlans = List(
          LatentPlanNarrative(
            seedId = "latent_rook_lift",
            planName = "Rook lift",
            viabilityScore = 0.71,
            whyAbsentFromTopMultiPv = "it needs one free tempo first"
          )
        ),
        whyAbsentFromTopMultiPV = List("it needs one free tempo first"),
        authorQuestions = List(
          AuthorQuestion(
            id = "q_latent",
            kind = AuthorQuestionKind.LatentPlan,
            priority = 1,
            question = "What slower attacking idea is waiting in reserve?",
            latentPlan = Some(latentInfo)
          )
        )
      )

    val rec = new TraceRecorder()
    val (outline, diag) = NarrativeOutlineBuilder.build(ctx, rec)
    val validated = NarrativeOutlineValidator.validate(outline, diag, rec, Some(ctx))
    val latentBeat = validated.beats.find(_.kind == OutlineBeatKind.ConditionalPlan).getOrElse(fail("missing latent beat"))

    assert(latentBeat.evidencePurposes.contains("latent_plan_heuristic"))
    assertEquals(latentBeat.requiresEvidence, false)
    assert(latentBeat.confidenceLevel >= 0.7)
  }

  test("focusMomentOutline keeps alternative-support decision text when it is marked essential") {
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

  test("focusMomentOutline keeps structure deployment main-move text ahead of generic wrap-up") {
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

  test("focusMomentOutline keeps required evidence beats with branch-scoped decision text") {
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

  test("focusMomentOutline drops branch-scoped claim when required evidence cannot fit") {
    val outline = NarrativeOutline(
      List(
        OutlineBeat(kind = OutlineBeatKind.Context, text = "Context.", focusPriority = 100, fullGameEssential = true),
        OutlineBeat(kind = OutlineBeatKind.MainMove, text = "Main move.", focusPriority = 99, fullGameEssential = true),
        OutlineBeat(kind = OutlineBeatKind.OpeningTheory, text = "Opening.", focusPriority = 98, fullGameEssential = true),
        OutlineBeat(kind = OutlineBeatKind.ConditionalPlan, text = "Plan.", focusPriority = 97, fullGameEssential = true),
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
        )
      )
    )

    val focused = CommentaryEngine.focusMomentOutline(outline, hasCriticalBranch = true)

    assert(!focused.beats.exists(_.kind == OutlineBeatKind.DecisionPoint))
    assert(!focused.beats.exists(_.kind == OutlineBeatKind.Evidence))
  }

  test("focusMomentOutline drops strategic-distribution wrap-up beats even with high focus priority") {
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

  test("trimHybridBodyRepetition drops redundant strategic stack meta") {
    val body =
      "This middlegame block near ply 28 is defined by cumulative pressure and move-order accuracy. " +
        "Strategically, this phase rewards a coherent plan around PawnStorm Kingside. " +
        "Key theme: **PawnStorm Kingside**. " +
        "The strategic stack still favors PawnStorm Kingside first, with Attacking fixed Pawn as the backup route. " +
        "The leading route is PawnStorm Kingside. " +
        "The backup strategic stack is 1. PawnStorm Kingside (0.81); 2. Attacking fixed Pawn (0.77). " +
        "The main signals are pawnstorm kingside, plan first, flank infrastructure, rook pawn march."

    val trimmed = CommentaryEngine.trimHybridBodyRepetition(body, Some("PawnStorm Kingside"))

    assertEquals(countOccurrences(trimmed, "Strategically, this phase rewards a coherent plan around PawnStorm Kingside."), 1)
    assert(trimmed.contains("PawnStorm Kingside"))
    assert(!trimmed.contains("The strategic stack still favors"))
    assert(!trimmed.contains("The leading route is"))
    assert(!trimmed.contains("The backup strategic stack is"))
    assert(!trimmed.contains("The main signals are"))
  }

  test("assembleHybridNarrativeDraft suppresses duplicate preface when body already frames the moment") {
    val rendered =
      CommentaryEngine.assembleHybridNarrativeDraft(
        lead = "This middlegame block near ply 28 is defined by cumulative pressure and move-order accuracy.",
        bridge = "Strategically, this phase rewards a coherent plan around PawnStorm Kingside.",
        criticalBranch = Some("Critical branch: Compared with **Nf6**, **Qe6** holds roughly a 0.5-pawn edge."),
        body =
          "This middlegame block near ply 28 is defined by cumulative pressure and move-order accuracy. " +
            "Strategically, this phase rewards a coherent plan around PawnStorm Kingside. " +
            "Key theme: **PawnStorm Kingside**. " +
            "The strategic stack still favors PawnStorm Kingside first, with Attacking fixed Pawn as the backup route.",
        primaryPlan = Some("PawnStorm Kingside")
    )

    assertEquals(countOccurrences(rendered, "This middlegame block near ply 28 is defined by cumulative pressure and move-order accuracy."), 1)
    assertEquals(countOccurrences(rendered, "Strategically, this phase rewards a coherent plan around PawnStorm Kingside."), 1)
    assert(!rendered.contains("The strategic stack still favors"))
  }

  test("assembleHybridNarrativeDraft drops preface ahead of cited branch prose") {
    val rendered =
      CommentaryEngine.assembleHybridNarrativeDraft(
        lead = "This middlegame block near ply 28 is defined by cumulative pressure and move-order accuracy.",
        bridge = "Strategically, this phase rewards a coherent plan around PawnStorm Kingside.",
        criticalBranch = Some("After 12...Bf5 13.Nc3 Qa5, Black keeps the cleaner continuation."),
        body = "Body sentence.",
        primaryPlan = Some("PawnStorm Kingside"),
        suppressPreface = true
      )

    assert(!rendered.contains("This middlegame block near ply 28"))
    assert(!rendered.contains("Strategically, this phase rewards"))
    assert(rendered.contains("After 12.."), clues(rendered))
    assert(rendered.contains("Qa5"), clues(rendered))
  }

  test("renderHybridMomentNarrative compresses chronicle prose and drops strategic-distribution meta") {
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

    val (rendered, _) = CommentaryEngine.renderHybridMomentNarrative(ctx, moment, prepared = Some(prepared))
    val paragraphs = rendered.split("\n\n").toList.filter(_.trim.nonEmpty)

    assert(paragraphs.size <= 3, clue(rendered))
    assert(rendered.contains("Qe2 keeps the e4 push available"), clue(rendered))
    assert(rendered.contains("center still needs one more defender"), clue(rendered))
    assert(!rendered.contains("strategic stack"), clue(rendered))
    assert(!rendered.contains("Current support centers on"), clue(rendered))
    assert(!rendered.contains("This middlegame block near ply 20"), clue(rendered))
  }

  test("renderHybridMomentNarrative keeps a cited line as the optional third paragraph") {
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

    val (rendered, _) = CommentaryEngine.renderHybridMomentNarrative(ctx, moment, prepared = Some(prepared))
    val sentenceCount = rendered.split("(?<=[.!?])\\s+").count(_.trim.nonEmpty)

    assert(sentenceCount >= 3, clue(rendered))
    assert(rendered.contains("A concrete line is"), clue(rendered))
    assert(rendered.contains("Qa5"), clue(rendered))
    assert(!rendered.contains("Lead."), clue(rendered))
    assert(!rendered.contains("Bridge."), clue(rendered))
  }

  test("hybrid bridge reuses the validated thesis claim when one is available") {
    val ctx = BookmakerProseGoldenFixtures.exchangeSacrifice.ctx
    val moment =
      KeyMoment(
        ply = ctx.ply,
        momentType = "SustainedPressure",
        score = 0,
        description = "Compensation bridge"
      )
    val parts = CommentaryEngine.buildHybridNarrativeParts(ctx, moment)
    val strategyPack = BookmakerProseGoldenFixtures.exchangeSacrifice.strategyPack
    val digest = strategyPack.flatMap(_.signalDigest)
    val thesis =
      StrategicThesisBuilder
        .build(ctx, strategyPack)
        .getOrElse(fail("missing validated thesis for bridge reuse"))

    val bridge = CommentaryEngine.buildHybridNarrativeBridge(ctx, parts, strategyPack, digest)

    assertEquals(bridge, thesis.claim)
    assert(
      bridge.toLowerCase.contains("material can wait") ||
      bridge.toLowerCase.contains("gives up material") ||
      bridge.toLowerCase.contains("winning it back") ||
        bridge.toLowerCase.contains("initiative") ||
        bridge.toLowerCase.contains("attack"),
      clue(bridge)
    )
  }

  test("hybrid bridge keeps quiet compensation wording aligned with the validated thesis") {
    val ctx = BookmakerProseGoldenFixtures.openFileFight.ctx
    val moment =
      KeyMoment(
        ply = ctx.ply,
        momentType = "StrategicBridge",
        score = 0,
        description = "Quiet compensation bridge"
      )
    val parts = CommentaryEngine.buildHybridNarrativeParts(ctx, moment)
    val strategyPack =
      Some(
        StrategyPack(
          sideToMove = "black",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "black",
              piece = "R",
              from = "a8",
              route = List("a8", "d8", "d3"),
              purpose = "kingside clamp",
              strategicFit = 0.82,
              tacticalSafety = 0.77,
              surfaceConfidence = 0.79,
              surfaceMode = RouteSurfaceMode.Toward
            )
          ),
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "black",
              piece = "Q",
              from = "d8",
              target = "b6",
              idea = "fix the queenside targets"
            )
          ),
          strategicIdeas = List(
            StrategyIdeaSignal(
              ideaId = "idea_benko_line",
              ownerSide = "black",
              kind = StrategicIdeaKind.LineOccupation,
              group = "slow_structural",
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("b2", "c4", "d4"),
              focusFiles = List("b", "c", "d"),
              focusZone = Some("queenside"),
              beneficiaryPieces = List("R", "Q"),
              confidence = 0.86
            ),
            StrategyIdeaSignal(
              ideaId = "idea_benko_targets",
              ownerSide = "black",
              kind = StrategicIdeaKind.TargetFixing,
              group = "slow_structural",
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("b2", "a6"),
              focusFiles = List("a", "b"),
              focusZone = Some("queenside"),
              beneficiaryPieces = List("R"),
              confidence = 0.79
            )
          ),
          longTermFocus = List("fix the queenside targets before recovering the pawn"),
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through line pressure and delayed recovery"),
              compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
              investedMaterial = Some(100),
              dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
              dominantIdeaGroup = Some("slow_structural"),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("b2, c4, d4")
            )
          )
        )
      )
    val digest = strategyPack.flatMap(_.signalDigest)
    val thesis =
      StrategicThesisBuilder
        .build(ctx, strategyPack)
        .getOrElse(fail("missing quiet-compensation thesis for bridge reuse"))

    val bridge = CommentaryEngine.buildHybridNarrativeBridge(ctx, parts, strategyPack, digest)

    assertEquals(bridge, thesis.claim)
    assert(
      bridge.toLowerCase.contains("queenside targets under pressure") ||
        bridge.toLowerCase.contains("queenside targets"),
      clue(bridge)
    )
    assert(bridge.toLowerCase.contains("queenside"), clue(bridge))
    assert(!bridge.toLowerCase.contains("kingside clamp"), clue(bridge))
  }

  test("hybrid bridge reframes weak compensation shells through the validated non-compensation thesis") {
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
    val strategyPack =
      Some(
        StrategyPack(
          sideToMove = "white",
          signalDigest = Some(
            NarrativeSignalDigest(
              compensation = Some("initiative against the king"),
              compensationVectors = List("Initiative (0.6)"),
              investedMaterial = Some(100),
              dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
              dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
              dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
              dominantIdeaFocus = Some("g7")
            )
          )
        )
      )
    val digest = strategyPack.flatMap(_.signalDigest)
    val thesis =
      StrategicThesisBuilder
        .build(ctx, strategyPack)
        .getOrElse(fail("missing weak-compensation reframed thesis"))

    val bridge = CommentaryEngine.buildHybridNarrativeBridge(ctx, parts, strategyPack, digest)

    assertNotEquals(thesis.lens, StrategicLens.Compensation)
    assertEquals(bridge, thesis.claim)
    assert(!bridge.toLowerCase.contains("material can wait"), clue(bridge))
    assert(!bridge.toLowerCase.contains("winning the material back"), clue(bridge))
  }

  test("hybrid bridge falls back to neutral copy when only pv-coupled plan metadata exists") {
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
    val bridge = CommentaryEngine.buildHybridNarrativeBridge(ctx, parts)
    val hierarchicalFallback = NarrativeGenerator.describeHierarchical(ctx)

    assert(parts.focusedOutline.beats.exists(_.text.contains(bridge.stripSuffix("."))), clue(parts.focusedOutline.beats.map(_.text)))
    assert(!parts.body.toLowerCase.contains("kingside expansion"), clue(parts.body))
    assert(!hierarchicalFallback.toLowerCase.contains("kingside expansion"), clue(hierarchicalFallback))
    assert(!bridge.toLowerCase.contains("kingside expansion"), clue(bridge))
  }
