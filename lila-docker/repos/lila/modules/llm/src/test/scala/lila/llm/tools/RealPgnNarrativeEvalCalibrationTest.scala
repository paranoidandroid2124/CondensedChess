package lila.llm.tools

import munit.FunSuite

import lila.llm.GameChronicleMoment
import lila.llm.analysis.StrategyPackSurface

class RealPgnNarrativeEvalCalibrationTest extends FunSuite:

  private def moment(moveNumber: Int, momentType: String = "TensionPeak") =
    GameChronicleMoment(
      momentId = s"ply_${moveNumber * 2}",
      ply = moveNumber * 2,
      moveNumber = moveNumber,
      side = "black",
      moveClassification = None,
      momentType = momentType,
      fen = "8/8/8/8/8/8/8/8 b - - 0 1",
      narrative = "Narrative",
      concepts = Nil,
      variations = Nil,
      cpBefore = 0,
      cpAfter = 0,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = None,
      strategicSalience = Some("Low"),
      transitionType = None,
      transitionConfidence = None,
      activePlan = None,
      topEngineMove = None,
      collapse = None
    )

  private def snapshot(
      compensationSummary: Option[String] = Some("return vector through initiative and line pressure"),
      compensationVectors: List[String] = List("Initiative (0.6)", "Line Pressure (0.5)"),
      investedMaterial: Option[Int] = Some(300)
  ) =
    StrategyPackSurface.Snapshot(
      sideToMove = Some("black"),
      dominantIdea = None,
      secondaryIdea = None,
      campaignOwner = Some("black"),
      ownerMismatch = false,
      allRoutes = Nil,
      topRoute = None,
      allMoveRefs = Nil,
      topMoveRef = None,
      allDirectionalTargets = Nil,
      topDirectionalTarget = None,
      longTermFocus = None,
      evidenceHints = Nil,
      strategicStack = Nil,
      latentPlan = None,
      decisionEvidence = None,
      compensationSummary = compensationSummary,
      compensationVectors = compensationVectors,
      investedMaterial = investedMaterial,
      compensationSubtype = None
    )

  test("late technical tails with only static space compensation are demoted") {
    val technicalMoment = moment(moveNumber = 38)
    val staticSurface =
      snapshot(
        compensationSummary = Some("space advantage"),
        compensationVectors = List("Space Advantage (0.8)"),
        investedMaterial = Some(600)
      )

    assert(!RealPgnNarrativeEvalCalibration.compensationEvalPosition(technicalMoment, staticSurface, staticSurface))
  }

  test("dynamic compensation tails remain eligible when initiative and line pressure survive") {
    val technicalMoment = moment(moveNumber = 39)
    val dynamicSurface = snapshot()

    assert(RealPgnNarrativeEvalCalibration.compensationEvalPosition(technicalMoment, dynamicSurface, dynamicSurface))
  }

  test("transition-only compensation tails are demoted in late technical moments") {
    val technicalMoment = moment(moveNumber = 41)
    val transitionSurface =
      snapshot(
        compensationSummary = Some("cash out the compensation into a clean transition"),
        compensationVectors = List("Return Vector (0.5)"),
        investedMaterial = Some(200)
      ).copy(
        compensationSubtype = Some(
          StrategyPackSurface.CompensationSubtype(
            pressureTheater = "mixed",
            pressureMode = "conversion_window",
            recoveryPolicy = "delayed",
            stabilityClass = "transition_only"
          )
        )
      )

    assert(!RealPgnNarrativeEvalCalibration.compensationEvalPosition(technicalMoment, transitionSurface, transitionSurface))
  }

  test("TAT06 recapture neutralization is treated as a negative compensation guard") {
    val tat06Moment =
      moment(moveNumber = 30).copy(
        fen = "3r3k/2pq3p/1p3pr1/p1p1p2Q/P1P1PB2/3P1P2/2P3P1/R4R1K b - - 0 30"
      )
    val recaptureSurface =
      snapshot(
        compensationSummary = Some("return vector through initiative and line pressure"),
        compensationVectors = List("Initiative (0.6)", "Line Pressure (0.6)", "Delayed Recovery (0.4)"),
        investedMaterial = Some(300)
      ).copy(
        compensationSubtype = Some(
          StrategyPackSurface.CompensationSubtype(
            pressureTheater = "center",
            pressureMode = "line_occupation",
            recoveryPolicy = "immediate",
            stabilityClass = "durable_pressure"
          )
        )
      )

    assert(
      !RealPgnNarrativeEvalCalibration.compensationEvalPosition(
        tat06Moment,
        recaptureSurface,
        recaptureSurface,
        Some("e5f4")
      )
    )
  }
