package lila.commentary.analysis.semantic

import _root_.chess.{ Color, File, Rook, Square }

import lila.commentary.{ StrategicIdeaKind, StrategicIdeaReadiness, StrategyPack }
import lila.commentary.analysis.{ PositionFeatures, StrategicIdeaSemanticContext, StrategicStateFeatures }
import lila.commentary.model.FactScope
import lila.commentary.model.strategic.{ EndgameFeature, PositionalTag, PreventedPlan, TheoreticalOutcomeHint }
import munit.FunSuite

class StrategicIdeaEvidencePipelineTest extends FunSuite:

  test("slow-structural producers emit typed selector evidence for typed selector families") {
    val semantic =
      StrategicIdeaSemanticContext
        .empty("white")
        .copy(
          positionalFeatures = List(
            PositionalTag.SpaceAdvantage(Color.White),
            PositionalTag.OpenFile(File.C, Color.White),
            PositionalTag.Outpost(Square.E5, Color.White),
            PositionalTag.BishopPairAdvantage(Color.White)
          )
        )

    val observations =
      StrategicIdeaEvidencePipeline.collect(StrategyPack(sideToMove = "white"), semantic)

    assert(observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.SpaceAdvantageTag), clues(observations))
    assert(observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.OpenFileControl), clues(observations))
    assert(observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.OutpostTag), clues(observations))
    assert(observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.BishopPairAdvantage), clues(observations))
    assert(observations.flatMap(_.factIds).forall(fact => !fact.wireKey.startsWith("source:")))
  }

  test("color-complex clamp carries exact weak-square support without proof authority") {
    val semantic =
      StrategicIdeaSemanticContext
        .empty("white")
        .copy(
          strategicState = Some(StrategicStateFeatures.empty.copy(whiteColorComplexClamp = true)),
          positionalFeatures = List(
            PositionalTag.ColorComplexWeakness(Color.Black, "dark", List(Square.F6, Square.H6, Square.G7))
          )
        )

    val observations =
      StrategicIdeaEvidencePipeline.collect(StrategyPack(sideToMove = "white"), semantic)
    val colorComplex =
      observations.find(_.source == StrategicObservationIds.EvidenceSourceId.ColorComplexClamp)

    assertEquals(colorComplex.map(_.kind), Some(StrategicIdeaKind.SpaceGainOrRestriction))
    assertEquals(colorComplex.map(_.tier), Some(StrategicIdeaEvidenceTier.ValidatedPressure))
    assertEquals(colorComplex.map(_.focusSquares), Some(List("f6", "h6", "g7")))
    assertEquals(colorComplex.flatMap(_.focusZone), Some("dark-square complex"))
    assert(colorComplex.exists(_.factIds.exists(_.wireKey == "state_color_complex_clamp")), clues(colorComplex))
    assert(colorComplex.exists(_.factIds.exists(_.wireKey == "enemy_color_complex_weakness")), clues(colorComplex))
    assert(colorComplex.exists(_.factIds.exists(_.wireKey == "color_complex_dark")), clues(colorComplex))
    assert(colorComplex.exists(_.factIds.forall(fact => !fact.wireKey.startsWith("source:"))), clues(colorComplex))
  }

  test("theme producers emit typed selector evidence for typed selector families") {
    val semantic =
      StrategicIdeaSemanticContext
        .empty("white")
        .copy(
          positionalFeatures = List(
            PositionalTag.MateNet(Color.White),
            PositionalTag.RemovingTheDefender(Rook, Color.White)
          ),
          preventedPlans = List(
            PreventedPlan(
              planId = "StopMate",
              deniedSquares = List(Square.H7),
              breakNeutralized = None,
              mobilityDelta = -1,
              counterplayScoreDrop = 20,
              preventedThreatType = Some("Mate"),
              deniedResourceClass = Some("forcing_threat")
            ),
            PreventedPlan(
              planId = "DenyBreak",
              deniedSquares = List(Square.B5, Square.D5),
              breakNeutralized = Some("b5"),
              mobilityDelta = -2,
              counterplayScoreDrop = 140,
              deniedResourceClass = Some("break"),
              deniedEntryScope = Some("file"),
              breakNeutralizationStrength = Some(80)
            )
          )
        )

    val observations =
      StrategicIdeaEvidencePipeline.collect(StrategyPack(sideToMove = "white"), semantic)

    assert(observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.PreventedPlan), clues(observations))
    assert(observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.MateNet), clues(observations))
    val counterplaySuppression =
      observations.find(_.source == StrategicObservationIds.EvidenceSourceId.CounterplaySuppression)
    assert(counterplaySuppression.nonEmpty, clues(observations))
    assert(counterplaySuppression.exists(_.factIds.exists(_.wireKey == "counterplay_suppression_shape")), clues(observations))
    assert(counterplaySuppression.exists(_.factIds.exists(_.wireKey == "counterplay_break_denial")), clues(observations))
    assert(observations.flatMap(_.factIds).forall(fact => !fact.wireKey.startsWith("source:")))
  }

  test("winning endgame transition keeps shape fact for support surface") {
    val semantic =
      StrategicIdeaSemanticContext
        .empty("white")
        .copy(
          phase = "endgame",
          positionFeatures = Some(PositionFeatures.empty),
          endgameFeatures = Some(
            EndgameFeature(
              hasOpposition = false,
              isZugzwang = false,
              keySquaresControlled = List(Square.E6),
              theoreticalOutcomeHint = TheoreticalOutcomeHint.Win
            )
          )
        )

    val observations =
      StrategicIdeaEvidencePipeline.collect(StrategyPack(sideToMove = "white"), semantic)
    val winningEndgame =
      observations.find(_.source == StrategicObservationIds.EvidenceSourceId.WinningEndgameTransition)

    assert(winningEndgame.nonEmpty, clues(observations))
    assert(winningEndgame.exists(_.factIds.exists(_.wireKey == "winning_endgame_transition_shape")), clues(observations))
    assert(winningEndgame.exists(_.factIds.forall(fact => !fact.wireKey.startsWith("source:"))), clues(winningEndgame))
  }

  test("counterplay suppression evidence stays on current-board prevented plans") {
    val semantic =
      StrategicIdeaSemanticContext
        .empty("white")
        .copy(
          preventedPlans =
            List(
              PreventedPlan(
                planId = "DenyBreakLine",
                deniedSquares = List(Square.B5),
                breakNeutralized = Some("b5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 140,
                deniedResourceClass = Some("break"),
                sourceScope = FactScope.ThreatLine
              )
            )
        )

    val observations =
      StrategicIdeaEvidencePipeline.collect(StrategyPack(sideToMove = "white"), semantic)

    assert(
      !observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.CounterplaySuppression),
      clues(observations)
    )
  }

  test("raw removing-defender tags degrade to a generic exchange label without defender authority") {
    val semantic =
      StrategicIdeaSemanticContext
        .empty("white")
        .copy(
          positionalFeatures = List(PositionalTag.RemovingTheDefender(Rook, Color.White))
        )

    val observations =
      StrategicIdeaEvidencePipeline.collect(StrategyPack(sideToMove = "white"), semantic)

    assert(
      !observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.RemovingTheDefender),
      clues(observations)
    )
    assert(
      observations.exists(observation =>
        observation.source == StrategicObservationIds.EvidenceSourceId.CaptureExchangeTransformation &&
          observation.readiness == StrategicIdeaReadiness.Build
      ),
      clues(observations)
    )
  }
