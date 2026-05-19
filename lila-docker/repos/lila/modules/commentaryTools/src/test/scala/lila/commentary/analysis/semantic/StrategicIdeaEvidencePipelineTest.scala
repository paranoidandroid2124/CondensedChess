package lila.commentary.analysis.semantic

import _root_.chess.{ Color, File, Rook, Square }

import lila.commentary.StrategyPack
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.model.strategic.{ PositionalTag, PreventedPlan }
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
    assert(observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.RemovingTheDefender), clues(observations))
    assert(observations.exists(_.source == StrategicObservationIds.EvidenceSourceId.CounterplaySuppression), clues(observations))
    assert(observations.flatMap(_.factIds).forall(fact => !fact.wireKey.startsWith("source:")))
  }
