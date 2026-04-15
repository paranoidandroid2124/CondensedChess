package lila.llm.strategicobject

import chess.Square

final case class AccessNetworkBridgeAdmissionAssessment(
    traceAdmittedSlice: Boolean,
    routeWitnessRetained: Boolean,
    contestedTargetRetained: Boolean
):
  def admitted: Boolean =
    traceAdmittedSlice && routeWitnessRetained && contestedTargetRetained

object AccessNetworkBridgeAdmissionBoundary:

  def admits(
      accessClaim: CertifiedClaim,
      residualClaim: CertifiedClaim
  ): Boolean =
    assess(accessClaim, residualClaim).admitted

  def assess(
      accessClaim: CertifiedClaim,
      residualClaim: CertifiedClaim
  ): AccessNetworkBridgeAdmissionAssessment =
    val traceAdmittedSlice =
      accessClaim.id != residualClaim.id &&
        accessClaim.deltaScope == StrategicDeltaScope.MoveLocal &&
        residualClaim.deltaScope == StrategicDeltaScope.MoveLocal &&
        isAccessNetworkClaim(accessClaim) &&
        residualClaim.plannerMetadata.residualSpecificityClass.nonEmpty &&
        sameClaimOwner(accessClaim, residualClaim) &&
        accessClaim.hasTypedDelta &&
        residualClaim.hasTypedDelta

    val routeWitnessRetained =
      traceAdmittedSlice &&
        accessRouteWitnessSquares(accessClaim)
          .intersect(claimWitnessSquares(residualClaim))
          .nonEmpty

    val contestedTargetRetained =
      traceAdmittedSlice &&
        accessContestedTargetSquares(accessClaim)
          .intersect(claimWitnessSquares(residualClaim))
          .nonEmpty

    AccessNetworkBridgeAdmissionAssessment(
      traceAdmittedSlice = traceAdmittedSlice,
      routeWitnessRetained = routeWitnessRetained,
      contestedTargetRetained = contestedTargetRetained
    )

  private def isAccessNetworkClaim(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(_.family == StrategicObjectFamily.AccessNetwork)

  private def sameClaimOwner(
      left: CertifiedClaim,
      right: CertifiedClaim
  ): Boolean =
    left.delta.map(_.owner) == right.delta.map(_.owner)

  private def accessRouteWitnessSquares(
      claim: CertifiedClaim
  ): Set[Square] =
    claim.delta.toSet.flatMap {
      case StrategicObjectDelta(
            _,
            StrategicObjectFamily.AccessNetwork,
            _,
            _,
            StrategicObjectProfile.AccessNetwork(_, route, _, contestedSquares),
            projection,
            changedAnchors,
            _,
            _,
            evidenceRefs
          ) =>
        val contestedTargets = contestedSquares.toSet ++ evidenceRefs.flatMap(_.contestedSquares)
        val profileRouteSquares =
          route.toSet.flatMap(_.allSquares.filterNot(contestedTargets.contains))
        val anchorRouteSquares =
          changedAnchors.flatMap(_.route.toSet.flatMap(_.allSquares.filterNot(contestedTargets.contains)))
        val anchorSquares =
          changedAnchors.flatMap(_.squares).filterNot(contestedTargets.contains)
        val evidenceAnchorSquares =
          evidenceRefs.flatMap(_.anchorSquares).filterNot(contestedTargets.contains)
        val matchedSquares =
          projection.moveWitness.toSet.flatMap(_.matchedSquares.filterNot(contestedTargets.contains))

        (profileRouteSquares ++
          anchorRouteSquares ++
          anchorSquares ++
          evidenceAnchorSquares ++
          matchedSquares).toSet
      case _ =>
        Set.empty[Square]
    }

  private def accessContestedTargetSquares(
      claim: CertifiedClaim
  ): Set[Square] =
    claim.delta.toSet.flatMap {
      case StrategicObjectDelta(
            _,
            StrategicObjectFamily.AccessNetwork,
            _,
            _,
            StrategicObjectProfile.AccessNetwork(_, route, _, contestedSquares),
            _,
            _,
            _,
            _,
            evidenceRefs
          ) =>
        (
          contestedSquares ++
            route.toList.map(_.target) ++
            evidenceRefs.flatMap(_.contestedSquares)
        ).toSet
      case _ =>
        Set.empty[Square]
    }

  private def claimWitnessSquares(
      claim: CertifiedClaim
  ): Set[Square] =
    claim.delta.toSet.flatMap(delta =>
      delta.changedAnchors.flatMap(anchor => anchor.squares ++ anchor.route.toList.flatMap(_.allSquares)) ++
        delta.evidenceRefs.flatMap(ref => ref.anchorSquares ++ ref.contestedSquares) ++
        delta.projection.moveWitness.toList.flatMap(_.matchedSquares)
    )
