package lila.commentary.analysis

import lila.commentary.*

private[commentary] final case class PlayerFacingMoveDeltaClaim(
    deltaClass: PlayerFacingMoveDeltaClass,
    anchorText: String,
    reasonText: Option[String],
    routeCue: Option[ActiveBranchRouteCue] = None,
    moveCue: Option[ActiveBranchMoveCue] = None,
    directionalTargets: List[StrategyDirectionalTarget] = Nil,
    evidenceLines: List[String] = Nil,
    sourceKind: String
)

private[commentary] final case class PlayerFacingMoveDeltaBundle(
    claims: List[PlayerFacingMoveDeltaClaim],
    visibleRouteRefs: List[ActiveStrategicRouteRef],
    visibleMoveRefs: List[ActiveStrategicMoveRef],
    visibleDirectionalTargets: List[StrategyDirectionalTarget],
    tacticalLead: Option[String],
    tacticalEvidence: Option[String]
):
  def hasVisibleSupport: Boolean =
    claims.nonEmpty ||
      visibleRouteRefs.nonEmpty ||
      visibleMoveRefs.nonEmpty ||
      visibleDirectionalTargets.nonEmpty ||
      tacticalLead.nonEmpty ||
      tacticalEvidence.nonEmpty
