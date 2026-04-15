package lila.llm.strategicobject

import chess.Square

private[strategicobject] object FixedTargetClusterWitnessBoundary:

  def hasClusterWitness(
      claim: CertifiedClaim
  ): Boolean =
    fixedTargetClusterWitnesses(claim).nonEmpty

  def sharesClusterWitness(
      left: CertifiedClaim,
      right: CertifiedClaim
  ): Boolean =
    fixedTargetClusterWitnesses(left).intersect(fixedTargetClusterWitnesses(right)).nonEmpty

  def hasFocalTargetSquare(
      claim: CertifiedClaim,
      targetSquare: Square
  ): Boolean =
    fixedTargetClusterWitnesses(claim).exists(_.focalTargetSquare == targetSquare)

  def existsWitness(
      claim: CertifiedClaim
  )(
      predicate: FixedTargetClusterWitness => Boolean
  ): Boolean =
    fixedTargetClusterWitnesses(claim).exists(predicate)

  private def fixedTargetClusterWitnesses(
      claim: CertifiedClaim
  ): Set[FixedTargetClusterWitness] =
    claim.boundaryWitnesses.collect {
      case CertifiedBoundaryWitness.FixedTargetCluster(witness) => witness
    }
