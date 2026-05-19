package lila.commentary.analysis.semantic

import lila.commentary.StrategyPack
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.analysis.semantic.evidence.{
  CounterplayEvidenceProducer,
  KingAttackEvidenceProducer,
  LineOccupationEvidenceProducer,
  MinorPieceImbalanceEvidenceProducer,
  OutpostEvidenceProducer,
  ProphylaxisEvidenceProducer,
  StructuralSpaceEvidenceProducer,
  TransformationEvidenceProducer
}

private[commentary] trait StrategicIdeaEvidenceProducer:
  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence]

private[commentary] object StrategicIdeaEvidencePipeline:

  private val producers: List[StrategicIdeaEvidenceProducer] =
    List(
      StructuralSpaceEvidenceProducer,
      LineOccupationEvidenceProducer,
      OutpostEvidenceProducer,
      MinorPieceImbalanceEvidenceProducer,
      ProphylaxisEvidenceProducer,
      KingAttackEvidenceProducer,
      TransformationEvidenceProducer,
      CounterplayEvidenceProducer
    )

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    producers.flatMap(_.collect(pack, semantic)).distinct
