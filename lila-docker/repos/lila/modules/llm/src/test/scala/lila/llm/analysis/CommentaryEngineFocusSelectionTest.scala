package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*

class CommentaryEngineFocusSelectionTest extends FunSuite:

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
