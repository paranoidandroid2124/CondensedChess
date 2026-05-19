package lila.commentary.analysis.semantic

import chess.{ Color, Square }
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.StrategyPack
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.model.strategic.WeakComplex
import lila.commentary.model.structure.{ CenterState, StructureId, StructureProfile }
import munit.FunSuite

class StrategicSemanticObservationPipelineTest extends FunSuite:

  private def boardFromFen(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))

  test("minority producer emits typed selector source plus support-only target pressure facts") {
    val fen = "4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        fen = fen,
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.C6),
            isOutpost = false,
            cause = "Minority-attack target on c6"
          )
        ),
        structureProfile = Some(
          StructureProfile(
            primary = StructureId.Carlsbad,
            confidence = 0.98,
            alternatives = Nil,
            centerState = CenterState.Locked,
            evidenceCodes = List("structure_carlsbad")
          )
        )
      )

    val observations =
      StrategicSemanticObservationPipeline.collect(StrategyPack(sideToMove = "white"), semantic)

    assert(
      observations.exists(observation =>
        observation.id == StrategicObservationIds.SemanticObservationId.MinorityAttackSemantic &&
          observation.source.contains(StrategicObservationIds.EvidenceSourceId.MinorityAttackSemantic) &&
          observation.wireEvidenceRefs.contains("source:minority_attack_semantic") &&
          observation.wireEvidenceRefs.contains("target_pressure_semantic") &&
          !observation.wireEvidenceRefs.contains("source:target_pressure_semantic")
      ),
      clues(observations.map(_.wireEvidenceRefs))
    )
  }
