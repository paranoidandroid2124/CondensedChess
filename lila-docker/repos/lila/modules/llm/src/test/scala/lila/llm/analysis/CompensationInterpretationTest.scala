package lila.llm.analysis

import munit.FunSuite

import lila.llm.*
import lila.llm.model.*

class CompensationInterpretationTest extends FunSuite:

  private val Tat06BeforeFen = "3r3k/2pq3p/1p3pr1/p1p1p2Q/P1P1PB2/3P1P2/2P3P1/R4R1K b - - 0 30"

  private def baseContext(
      phase: String = "Middlegame",
      playedMove: Option[String] = Some("e5f4"),
      playedSan: Option[String] = Some("exf4")
  ): NarrativeContext =
    NarrativeContext(
      fen = Tat06BeforeFen,
      header = ContextHeader(phase, "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 60,
      playedMove = playedMove,
      playedSan = playedSan,
      summary = NarrativeSummary("Recapture", None, "NarrowChoice", "Maintain", "-0.49"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext(phase, phase),
      candidates = Nil,
      renderMode = NarrativeRenderMode.Bookmaker
    )

  private def compensationInfo(
      investedMaterial: Int,
      returnVector: Map[String, Double],
      conversionPlan: String
  ): CompensationInfo =
    CompensationInfo(
      investedMaterial = investedMaterial,
      returnVector = returnVector,
      expiryPly = None,
      conversionPlan = conversionPlan
    )

  test("current and after compensation share the same recapture-neutralized rejection") {
    val compensation =
      compensationInfo(
        investedMaterial = 300,
        returnVector = Map("Initiative" -> 0.6, "Line Pressure" -> 0.6, "Delayed Recovery" -> 0.4),
        conversionPlan = "return vector through initiative and line pressure"
      )
    val ctx =
      baseContext().copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = Some(compensation),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil,
            afterCompensation = Some(compensation)
          )
        )
      )

    val current = CompensationInterpretation.currentSemanticDecision(ctx).getOrElse(fail("missing current decision"))
    val after = CompensationInterpretation.afterSemanticDecision(ctx).getOrElse(fail("missing after decision"))

    assert(current.decision.recaptureNeutralized)
    assert(after.decision.recaptureNeutralized)
    assertEquals(current.decision.rejectionReason, Some("recapture_neutralized"))
    assertEquals(after.decision.rejectionReason, Some("recapture_neutralized"))
    assertEquals(CompensationInterpretation.effectiveSemanticDecision(ctx), None)
  }

  test("thin return-vector-only compensation is rejected") {
    val ctx =
      baseContext(playedMove = None, playedSan = None).copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation =
              Some(compensationInfo(200, Map("Return Vector" -> 0.7), "return vector")),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        )
      )

    val decision = CompensationInterpretation.currentSemanticDecision(ctx).getOrElse(fail("missing decision")).decision
    assert(!decision.accepted)
    assert(decision.thinReturnVectorOnly)
    assertEquals(decision.rejectionReason, Some("thin_return_vector_only"))
  }

  test("late technical conversion tail is rejected when no durable carrier survives") {
    val ctx =
      baseContext(phase = "Endgame", playedMove = None, playedSan = None).copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation =
              Some(compensationInfo(600, Map("Return Vector" -> 0.7), "cash out the compensation into a clean transition")),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        )
      )

    val decision = CompensationInterpretation.currentSemanticDecision(ctx).getOrElse(fail("missing decision")).decision
    assert(!decision.accepted)
    assert(decision.lateTechnicalConversionTail)
    assertEquals(decision.rejectionReason, Some("late_technical_conversion_tail"))
  }

  test("durable line-pressure compensation remains accepted") {
    val ctx =
      baseContext(playedMove = Some("a8b8"), playedSan = Some("Rb8")).copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation =
              Some(
                compensationInfo(
                  100,
                  Map("Line Pressure" -> 0.7, "Delayed Recovery" -> 0.6),
                  "return vector through line pressure and delayed recovery"
                )
              ),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        )
      )

    val decision = CompensationInterpretation.currentSemanticDecision(ctx).getOrElse(fail("missing decision")).decision
    assert(decision.accepted)
    assert(decision.durableStructuralPressure)
    assertEquals(decision.persistenceClass, "durable_pressure")
  }
