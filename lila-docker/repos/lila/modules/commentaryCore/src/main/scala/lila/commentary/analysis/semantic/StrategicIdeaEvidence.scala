package lila.commentary.analysis.semantic

import lila.commentary.{ StrategicIdeaGroup, StrategicIdeaKind, StrategyRelationSupport }
import lila.commentary.analysis.semantic.StrategicObservationIds.{ EvidenceSourceId, FactId }

private[commentary] enum StrategicIdeaEvidenceTier:
  case SelectorSupport
  case ValidatedPressure

private[commentary] final case class StrategicIdeaEvidence(
    ownerSide: String,
    kind: String,
    group: String,
    readiness: String,
    source: EvidenceSourceId,
    confidence: Double,
    tier: StrategicIdeaEvidenceTier = StrategicIdeaEvidenceTier.SelectorSupport,
    focusSquares: List[String] = Nil,
    focusFiles: List[String] = Nil,
    focusDiagonals: List[String] = Nil,
    focusZone: Option[String] = None,
    targetSquare: Option[String] = None,
    beneficiaryPieces: List[String] = Nil,
    factIds: List[FactId] = Nil,
    relationKind: Option[String] = None,
    relationFocusSquares: List[String] = Nil,
    relationSupport: Option[StrategyRelationSupport] = None
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
      targetSquare: Option[String] = None,
      beneficiaryPieces: List[String] = Nil,
      factIds: List[String] = Nil,
      typedFactIds: List[FactId] = Nil,
      relationKind: Option[String] = None,
      relationFocusSquares: List[String] = Nil,
      relationSupport: Option[StrategyRelationSupport] = None,
      tier: StrategicIdeaEvidenceTier = StrategicIdeaEvidenceTier.SelectorSupport
  ): StrategicIdeaEvidence =
    val normalizedRelationKind =
      relationKind.map(_.trim).filter(RelationObservationCatalog.isImplementedKind)
    val normalizedRelationSupport =
      normalizedRelationKind.flatMap(kind =>
        relationSupport.filter(support => support.relationKind == kind)
      )
    StrategicIdeaEvidence(
      ownerSide = ownerSide,
      kind = kind,
      group = groupForKind(kind),
      readiness = readiness,
      source = source,
      confidence = confidence.max(0.0).min(0.98),
      tier = tier,
      focusSquares = focusSquares.distinct.filter(_.nonEmpty),
      focusFiles = focusFiles.distinct.filter(_.nonEmpty),
      focusDiagonals = focusDiagonals.distinct.filter(_.nonEmpty),
      focusZone = focusZone.map(_.trim).filter(_.nonEmpty),
      targetSquare = targetSquare.map(_.trim.toLowerCase).filter(_.matches("""[a-h][1-8]""")),
      beneficiaryPieces = beneficiaryPieces.distinct.filter(_.nonEmpty),
      factIds = (typedFactIds ++ factIds.distinct.flatMap(FactId.dynamic)).distinct,
      relationKind = normalizedRelationKind,
      relationFocusSquares = normalizedRelationKind.toList.flatMap(_ => normalizedSquareKeys(relationFocusSquares)),
      relationSupport = normalizedRelationSupport
    )

  private def normalizedSquareKeys(keys: List[String]): List[String] =
    keys
      .flatMap(key => Option(key).map(_.trim.toLowerCase).filter(_.matches("""[a-h][1-8]""")))
      .distinct

  def groupForKind(kind: String): String =
    kind match
      case StrategicIdeaKind.PawnBreak | StrategicIdeaKind.SpaceGainOrRestriction | StrategicIdeaKind.TargetFixing =>
        StrategicIdeaGroup.StructuralChange
      case StrategicIdeaKind.LineOccupation | StrategicIdeaKind.OutpostCreationOrOccupation |
          StrategicIdeaKind.MinorPieceImbalanceExploitation =>
        StrategicIdeaGroup.PieceAndLineManagement
      case _ =>
        StrategicIdeaGroup.InteractionAndTransformation
