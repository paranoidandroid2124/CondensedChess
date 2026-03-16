package lila.llm.analysis

import munit.FunSuite
import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*

class CommentaryEngineFocusSelectionTest extends FunSuite:

  private def countOccurrences(text: String, needle: String): Int =
    text.sliding(needle.length).count(_ == needle)

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
    assert(rendered.contains("Critical branch: Compared with **Nf6**, **Qe6** holds roughly a 0.5-pawn edge."))
    assert(!rendered.contains("The strategic stack still favors"))
  }

  test("strategy-aware hybrid bridge reflects compensation owner and execution focus") {
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

    val bridge = CommentaryEngine.buildHybridNarrativeBridge(ctx, parts, strategyPack, digest)

    assert(bridge.contains("White"))
    assert(bridge.toLowerCase.contains("compensation"))
    assert(bridge.toLowerCase.contains("execution still runs through"))
    assert(bridge.toLowerCase.contains("long-term objective"))
  }

  test("strategy-aware hybrid bridge uses normalized quiet compensation language across surfaces") {
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

    val bridge = CommentaryEngine.buildHybridNarrativeBridge(ctx, parts, strategyPack, digest)

    assert(bridge.toLowerCase.contains("fixed queenside targets"), clue(bridge))
    assert(bridge.toLowerCase.contains("queenside"), clue(bridge))
    assert(!bridge.toLowerCase.contains("kingside clamp"), clue(bridge))
  }
