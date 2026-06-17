package lila.commentary.analysis

import munit.FunSuite

import lila.commentary.model.*

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
      renderMode = NarrativeRenderMode.MoveReview
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

  test("raw attack labels are rejected without current king-pressure board evidence") {
    val quietFen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
    val ctx =
      baseContext(playedMove = None, playedSan = None).copy(
        fen = quietFen,
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation =
              Some(compensationInfo(100, Map("Attack on King" -> 0.8), "Mating Attack")),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        )
      )

    val decision = CompensationInterpretation.currentSemanticDecision(ctx).getOrElse(fail("missing decision")).decision
    assert(!decision.accepted)
    assertEquals(decision.rejectionReason, Some("missing_structural_carrier"))
  }

  test("king-pressure compensation remains accepted when the board proves the pressure") {
    val pressureFen = "6k1/8/6Q1/8/8/8/8/4K3 w - - 0 1"
    val ctx =
      baseContext(playedMove = None, playedSan = None).copy(
        fen = pressureFen,
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation =
              Some(compensationInfo(100, Map("King Pressure" -> 0.8), "king pressure")),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        )
      )

    val decision = CompensationInterpretation.currentSemanticDecision(ctx).getOrElse(fail("missing decision")).decision
    assert(decision.accepted)
    assertEquals(decision.persistenceClass, "non_immediate_transition")
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

  test("generic target pressure is rejected when the only weak pawn is unrelated") {
    val unrelatedIsolatedPawnFen = "4k3/p7/8/8/8/8/8/4K3 w - - 0 1"
    val ctx =
      baseContext(playedMove = None, playedSan = None).copy(
        fen = unrelatedIsolatedPawnFen,
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation =
              Some(compensationInfo(100, Map("Target Pressure" -> 0.7), "target pressure")),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        )
      )

    val decision = CompensationInterpretation.currentSemanticDecision(ctx).getOrElse(fail("missing decision")).decision
    assert(!decision.accepted)
    assert(!decision.durableStructuralPressure)
    assertEquals(decision.rejectionReason, Some("missing_structural_carrier"))
  }

  test("target-fixing compensation remains accepted for exact Carlsbad target shape") {
    val carlsbadFen = "4k3/8/2p5/3p4/3P4/8/1P6/4K3 w - - 0 1"
    val ctx =
      baseContext(playedMove = None, playedSan = None).copy(
        fen = carlsbadFen,
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation =
              Some(compensationInfo(100, Map("Fixed Targets" -> 0.7), "fixed queenside targets")),
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

  test("target-fixing compensation uses shared pawn targets for advanced Carlsbad and Benoni shapes") {
    val cases =
      List(
        "4k3/8/2p5/3p4/1P1P4/8/8/4K3 w - - 0 1" -> "fixed queenside targets",
        "4k3/8/2p5/1P1p4/3P4/8/8/4K3 w - - 0 1" -> "fixed queenside targets",
        "4k3/8/3p4/2pP4/8/8/8/4K3 w - - 0 1" -> "fixed pawn target on d6",
        "4k3/8/8/2n5/2pp4/3P4/8/4K3 b - - 0 1" -> "fixed pawn target on d3"
      )

    cases.foreach { case (fen, plan) =>
      val ctx =
        baseContext(playedMove = None, playedSan = None).copy(
          fen = fen,
          semantic = Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity = Nil,
              positionalFeatures = Nil,
              compensation =
                Some(compensationInfo(100, Map("Fixed Targets" -> 0.7), plan)),
              endgameFeatures = None,
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil
            )
          )
        )

      val decision = CompensationInterpretation.currentSemanticDecision(ctx).getOrElse(fail("missing decision")).decision
      assert(decision.accepted, clue(fen))
      assert(decision.durableStructuralPressure, clue(fen))
      assertEquals(decision.persistenceClass, "durable_pressure", clue(fen))
    }
  }

  test("CompensationInterpretation does not keep local Carlsbad or Benoni board-condition copies") {
    val source =
      java.nio.file.Files.readString(
        java.nio.file.Paths.get(
          "modules/commentaryCore/src/main/scala/lila/commentary/analysis/CompensationInterpretation.scala"
        )
      )

    assert(source.contains("PawnStructureTargets"), clue(source))
    assert(!source.contains("private def carlsbadTargetSquare"), clue(source))
    assert(!source.contains("val benoniD6"), clue(source))
  }
