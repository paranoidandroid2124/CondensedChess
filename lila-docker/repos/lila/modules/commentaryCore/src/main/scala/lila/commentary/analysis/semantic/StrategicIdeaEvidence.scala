package lila.commentary.analysis.semantic

import lila.commentary.{ StrategicIdeaGroup, StrategicIdeaKind }
import lila.commentary.analysis.semantic.StrategicObservationIds.{ EvidenceSourceId, FactId }

private[commentary] final case class StrategicIdeaEvidence(
    ownerSide: String,
    kind: String,
    group: String,
    readiness: String,
    source: EvidenceSourceId,
    confidence: Double,
    focusSquares: List[String] = Nil,
    focusFiles: List[String] = Nil,
    focusDiagonals: List[String] = Nil,
    focusZone: Option[String] = None,
    beneficiaryPieces: List[String] = Nil,
    factIds: List[FactId] = Nil
):
  def signature: String = s"$ownerSide|$kind"

private[commentary] object StrategicIdeaEvidence:

  def from(
      ownerSide: String,
      kind: String,
      readiness: String,
      source: EvidenceSourceId,
      confidence: Double,
      focusSquares: List[String] = Nil,
      focusFiles: List[String] = Nil,
      focusDiagonals: List[String] = Nil,
      focusZone: Option[String] = None,
      beneficiaryPieces: List[String] = Nil,
      factIds: List[String] = Nil,
      typedFactIds: List[FactId] = Nil
  ): StrategicIdeaEvidence =
    StrategicIdeaEvidence(
      ownerSide = ownerSide,
      kind = kind,
      group = groupForKind(kind),
      readiness = readiness,
      source = source,
      confidence = confidence.max(0.0).min(0.98),
      focusSquares = focusSquares.distinct.filter(_.nonEmpty),
      focusFiles = focusFiles.distinct.filter(_.nonEmpty),
      focusDiagonals = focusDiagonals.distinct.filter(_.nonEmpty),
      focusZone = focusZone.map(_.trim).filter(_.nonEmpty),
      beneficiaryPieces = beneficiaryPieces.distinct.filter(_.nonEmpty),
      factIds = (typedFactIds ++ factIds.distinct.flatMap(FactId.dynamic)).distinct
    )

  def groupForKind(kind: String): String =
    kind match
      case StrategicIdeaKind.PawnBreak | StrategicIdeaKind.SpaceGainOrRestriction | StrategicIdeaKind.TargetFixing =>
        StrategicIdeaGroup.StructuralChange
      case StrategicIdeaKind.LineOccupation | StrategicIdeaKind.OutpostCreationOrOccupation |
          StrategicIdeaKind.MinorPieceImbalanceExploitation =>
        StrategicIdeaGroup.PieceAndLineManagement
      case _ =>
        StrategicIdeaGroup.InteractionAndTransformation
