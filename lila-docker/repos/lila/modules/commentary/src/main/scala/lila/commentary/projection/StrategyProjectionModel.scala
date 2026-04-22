package lila.commentary.projection

import chess.Color

import lila.commentary.root.RootStateVector
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload }
import lila.commentary.witness.seed.StrategySupportSeedExtraction

opaque type StrategyProjectionBandId = String
object StrategyProjectionBandId:

  def apply(value: String): StrategyProjectionBandId =
    val normalized = value.trim
    require(
      normalized.matches("^S[0-9]{2}$"),
      s"Invalid strategy projection band id: '$value'"
    )
    normalized

  extension (id: StrategyProjectionBandId) def value: String = id

opaque type StrategyProjectionEvidenceKind = String
object StrategyProjectionEvidenceKind:

  def apply(value: String): StrategyProjectionEvidenceKind =
    val normalized = value.trim
    require(
      normalized.matches("^[a-z][a-z0-9_]*$"),
      s"Invalid strategy projection evidence kind: '$value'"
    )
    normalized

  extension (kind: StrategyProjectionEvidenceKind) def value: String = kind

final case class StrategyProjectionEvidenceClaim(
    bandId: StrategyProjectionBandId,
    kind: StrategyProjectionEvidenceKind,
    owner: Color,
    anchor: WitnessAnchor,
    payload: WitnessPayload = WitnessPayload.empty
):

  require(
    StrategyProjectionScopeContract.isAllowedEvidenceKind(bandId, kind),
    s"Evidence kind ${kind.value} is not allowed for projection band ${bandId.value}"
  )

  private[projection] def lookupKey: StrategyProjectionEvidenceClaim.LookupKey =
    StrategyProjectionEvidenceClaim.LookupKey(bandId, kind, owner, anchor)

object StrategyProjectionEvidenceClaim:

  private[projection] final case class LookupKey(
      bandId: StrategyProjectionBandId,
      kind: StrategyProjectionEvidenceKind,
      owner: Color,
      anchor: WitnessAnchor
  )

final class StrategyProjectionEvidence private (
    private val boundRootState: Option[RootStateVector],
    val all: Vector[StrategyProjectionEvidenceClaim]
):

  private lazy val claimsByKey: Map[
    StrategyProjectionEvidenceClaim.LookupKey,
    StrategyProjectionEvidenceClaim
  ] =
    all.map(claim => claim.lookupKey -> claim).toMap

  def evidenceFor(
      bandId: StrategyProjectionBandId,
      kind: StrategyProjectionEvidenceKind,
      owner: Color,
      anchor: WitnessAnchor
  ): Option[StrategyProjectionEvidenceClaim] =
    claimsByKey.get(StrategyProjectionEvidenceClaim.LookupKey(bandId, kind, owner, anchor))

  def matches(rootState: RootStateVector): Boolean =
    boundRootState.forall(_ == rootState)

  def isEmpty: Boolean = all.isEmpty

  override def equals(other: Any): Boolean =
    other match
      case that: StrategyProjectionEvidence =>
        boundRootState == that.boundRootState && all == that.all
      case _ => false

  override def hashCode(): Int =
    31 * boundRootState.hashCode() + all.hashCode()

object StrategyProjectionEvidence:

  val empty: StrategyProjectionEvidence =
    new StrategyProjectionEvidence(None, Vector.empty)

  def forRootState(
      rootState: RootStateVector,
      claims: IterableOnce[StrategyProjectionEvidenceClaim]
  ): StrategyProjectionEvidence =
    build(Some(rootState), claims)

  def forSeedExtraction(
      extraction: StrategySupportSeedExtraction,
      claims: IterableOnce[StrategyProjectionEvidenceClaim]
  ): StrategyProjectionEvidence =
    forRootState(extraction.rootState, claims)

  private def build(
      rootState: Option[RootStateVector],
      claims: IterableOnce[StrategyProjectionEvidenceClaim]
  ): StrategyProjectionEvidence =
    val normalized = claims.iterator.toVector
    val duplicateKeys =
      normalized
        .groupBy(_.lookupKey)
        .collect { case (key, grouped) if grouped.size > 1 =>
          s"${key.bandId.value}|${key.kind.value}|${if key.owner.white then "white" else "black"}|${key.anchor.key}"
        }
        .toVector
        .sorted

    require(
      duplicateKeys.isEmpty,
      s"Duplicate strategy projection evidence entries are not allowed: ${duplicateKeys.mkString(", ")}"
    )

    new StrategyProjectionEvidence(
      rootState,
      normalized.sortBy(claim =>
        s"${claim.bandId.value}|${claim.kind.value}|${if claim.owner.white then 0 else 1}|${claim.anchor.kind.sortOrder}|${claim.anchor.key}"
      )
    )
