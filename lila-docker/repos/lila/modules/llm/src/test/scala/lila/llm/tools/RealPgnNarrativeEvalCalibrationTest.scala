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
      investedMaterial: Option[Int] = Some(300),
      subtype: StrategyPackSurface.CompensationSubtype =
        StrategyPackSurface.CompensationSubtype(
          pressureTheater = "center",
          pressureMode = "line_occupation",
          recoveryPolicy = "delayed",
          stabilityClass = "durable_pressure"
        )
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
      compensationSummary = compensationSummary,
      compensationVectors = compensationVectors,
      investedMaterial = investedMaterial,
      compensationSubtype = Some(subtype),
      displayNormalization = Some(
        StrategyPackSurface.DisplayNormalization(
          normalizedDominantIdeaText = None,
          normalizedExecutionText = None,
          normalizedObjectiveText = None,
          normalizedLongTermFocusText = None,
          normalizedCompensationLead = None,
          normalizedCompensationSubtype = Some(subtype),
          normalizationActive = true,
          normalizationConfidence = 7,
          preparationSubtype = Some(subtype),
          payoffSubtype = Some(subtype),
          selectedDisplaySubtype = Some(subtype),
          displaySubtypeSource = "path",
          payoffConfidence = 6,
          pathConfidence = 7
        )
      )
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
        investedMaterial = Some(200),
        subtype =
          StrategyPackSurface.CompensationSubtype(
            pressureTheater = "mixed",
            pressureMode = "conversion_window",
            recoveryPolicy = "delayed",
            stabilityClass = "transition_only"
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
        investedMaterial = Some(300),
        subtype =
          StrategyPackSurface.CompensationSubtype(
            pressureTheater = "center",
            pressureMode = "line_occupation",
            recoveryPolicy = "immediate",
            stabilityClass = "durable_pressure"
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

  test("signoff compensation is demoted when Chronicle and Bookmaker do not share a canonical subtype") {
    val structuralMoment = moment(moveNumber = 24, momentType = "SustainedPressure")
    val gameArcSurface =
      snapshot().copy(
        displayNormalization = Some(
          StrategyPackSurface.DisplayNormalization(
            normalizedDominantIdeaText = None,
            normalizedExecutionText = None,
            normalizedObjectiveText = None,
            normalizedLongTermFocusText = None,
            normalizedCompensationLead = None,
            normalizedCompensationSubtype = None,
            normalizationActive = true,
            normalizationConfidence = 7,
            preparationSubtype = Some(
              StrategyPackSurface.CompensationSubtype("queenside", "target_fixing", "delayed", "durable_pressure")
            ),
            payoffSubtype = Some(
              StrategyPackSurface.CompensationSubtype("queenside", "target_fixing", "delayed", "durable_pressure")
            ),
            selectedDisplaySubtype = Some(
              StrategyPackSurface.CompensationSubtype("queenside", "target_fixing", "delayed", "durable_pressure")
            ),
            displaySubtypeSource = "path",
            payoffConfidence = 6,
            pathConfidence = 7
          )
        )
      )
    val bookmakerSurface =
      gameArcSurface.copy(
        displayNormalization = Some(
          gameArcSurface.displayNormalization.get.copy(
            preparationSubtype = Some(
              StrategyPackSurface.CompensationSubtype("center", "line_occupation", "delayed", "durable_pressure")
            ),
            payoffSubtype = Some(
              StrategyPackSurface.CompensationSubtype("center", "line_occupation", "delayed", "durable_pressure")
            ),
            selectedDisplaySubtype = Some(
              StrategyPackSurface.CompensationSubtype("center", "line_occupation", "delayed", "durable_pressure")
            )
          )
        )
      )

    assert(!RealPgnNarrativeEvalCalibration.compensationEvalPosition(structuralMoment, gameArcSurface, bookmakerSurface))
  }
