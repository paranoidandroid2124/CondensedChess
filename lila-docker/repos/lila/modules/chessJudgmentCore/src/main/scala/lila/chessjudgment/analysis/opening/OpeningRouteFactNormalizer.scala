package lila.chessjudgment.analysis.opening

import lila.chessjudgment.model.judgment.*

object OpeningRouteFactNormalizer:

  def fromRoute(
      id: String,
      route: OpeningRouteCatalog.Route,
      position: PositionNodeRef,
      line: Option[LineNodeRef] = None,
      scope: EvidenceScope,
      confidence: EvidenceConfidence,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.OpeningRouteProducer,
        layer = EvidenceLayer.OpeningRoute,
        position = position,
        line = line,
        scope = scope,
        confidence = confidence
      )
    EvidenceRecord(
      ref = ref,
      payload = OpeningRouteFactEvidence(
        routeId = route.routeId,
        family = route.family,
        targetSquare = EvidenceSquare(route.targetSquare),
        pieceRole = EvidencePieceRole(route.role),
        path = route.path.map(EvidenceSquare(_)),
        targetMode = route.targetMode
      ),
      parents = parents
    )
