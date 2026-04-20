package lila.commentary.certification

import scala.annotation.targetName

import chess.Color

import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.root.RootStateVector
import lila.commentary.strategic.StrategicObjectExtraction
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessSupport }

opaque type CertificationId = String
object CertificationId:

  def apply(value: String): CertificationId =
    val normalized = value.trim
    require(
      normalized.matches("^[A-Z][A-Za-z0-9]*$"),
      s"Invalid certification id: '$value'"
    )
    normalized

  extension (id: CertificationId) def value: String = id

type CertificationFamilyId = CertificationId
object CertificationFamilyId:

  def apply(value: String): CertificationFamilyId =
    CertificationId(value)

  extension (id: CertificationFamilyId) def value: String = CertificationId.value(id)

enum CertificationVerdict(val key: String, val sortOrder: Int):
  case Certified extends CertificationVerdict("certified", 0)
  case SupportOnly extends CertificationVerdict("support_only", 1)
  case Deferred extends CertificationVerdict("deferred", 2)
  case Rejected extends CertificationVerdict("rejected", 3)

object CertificationVerdict:

  def fromKey(key: String): Option[CertificationVerdict] =
    CertificationVerdict.values.find(_.key == key.trim)

enum CertificationScope(val key: String):
  case Comparative extends CertificationScope("comparative")
  case CurrentPosition extends CertificationScope("current_position")

object CertificationScope:

  def fromKey(key: String): Option[CertificationScope] =
    CertificationScope.values.find(_.key == key.trim)

opaque type CertificationBurdenTag = String
object CertificationBurdenTag:

  def apply(value: String): CertificationBurdenTag =
    val normalized = value.trim
    require(
      normalized.matches("^[a-z][a-z0-9_]*$"),
      s"Invalid certification burden tag: '$value'"
    )
    normalized

  extension (tag: CertificationBurdenTag) def value: String = tag

opaque type CertificationSupportFamily = String
object CertificationSupportFamily:

  def apply(value: String): CertificationSupportFamily =
    val normalized = value.trim
    require(
      normalized.matches("^[A-Z][A-Za-z0-9]*$"),
      s"Invalid certification support family: '$value'"
    )
    normalized

  extension (family: CertificationSupportFamily) def value: String = family

enum CertificationEvidencePurpose(val key: String):
  case ComparativeSuperiority extends CertificationEvidencePurpose("comparative_superiority")
  case CounterplayDenial extends CertificationEvidencePurpose("counterplay_denial")
  case BestDefenseSurvival extends CertificationEvidencePurpose("best_defense_survival")
  case TacticalReleaseDetection extends CertificationEvidencePurpose("tactical_release_detection")
  case ConversionRouteSurvival extends CertificationEvidencePurpose("conversion_route_survival")

object CertificationEvidencePurpose:

  def fromKey(key: String): Option[CertificationEvidencePurpose] =
    CertificationEvidencePurpose.values.find(_.key == key.trim)

enum CertificationEvidenceStrength(val key: String):
  case Satisfied extends CertificationEvidenceStrength("satisfied")
  case Insufficient extends CertificationEvidenceStrength("insufficient")

object CertificationEvidenceStrength:

  def fromKey(key: String): Option[CertificationEvidenceStrength] =
    CertificationEvidenceStrength.values.find(_.key == key.trim)

final case class CertificationEvidenceClaim(
    familyId: CertificationId,
    owner: Color,
    anchor: WitnessAnchor = WitnessAnchor.BoardAnchor,
    purposeStrengths: Map[CertificationEvidencePurpose, CertificationEvidenceStrength],
    payload: WitnessPayload = WitnessPayload.empty
):

  def color: Color = owner

  def strengthFor(
      purpose: CertificationEvidencePurpose
  ): Option[CertificationEvidenceStrength] =
    purposeStrengths.get(purpose)

  private[certification] def lookupKey: CertificationEvidenceClaim.LookupKey =
    CertificationEvidenceClaim.LookupKey(familyId, owner, anchor)

object CertificationEvidenceClaim:

  private[certification] final case class LookupKey(
      familyId: CertificationId,
      owner: Color,
      anchor: WitnessAnchor
  )

final class CertificationEvidence private (
    private val boundRootState: Option[RootStateVector],
    val all: Vector[CertificationEvidenceClaim]
):

  private lazy val claimsByKey: Map[CertificationEvidenceClaim.LookupKey, CertificationEvidenceClaim] =
    all.map(claim => claim.lookupKey -> claim).toMap

  def evidenceFor(
      familyId: CertificationId,
      owner: Color,
      anchor: WitnessAnchor = WitnessAnchor.BoardAnchor
  ): Option[CertificationEvidenceClaim] =
    claimsByKey.get(CertificationEvidenceClaim.LookupKey(familyId, owner, anchor))

  def isEmpty: Boolean = all.isEmpty
  def matches(rootState: RootStateVector): Boolean = boundRootState.forall(_ == rootState)

  override def equals(other: Any): Boolean =
    other match
      case that: CertificationEvidence =>
        boundRootState == that.boundRootState && all == that.all
      case _ => false

  override def hashCode(): Int =
    31 * boundRootState.hashCode() + all.hashCode()

  override def toString: String =
    s"CertificationEvidence(bound=${boundRootState.nonEmpty}, claims=$all)"

object CertificationEvidence:

  val empty: CertificationEvidence = new CertificationEvidence(None, Vector.empty)

  def forRootState(
      rootState: RootStateVector,
      claims: IterableOnce[CertificationEvidenceClaim]
  ): CertificationEvidence =
    build(Some(rootState), claims)

  def forObjectExtraction(
      current: StrategicObjectExtraction,
      claims: IterableOnce[CertificationEvidenceClaim]
  ): CertificationEvidence =
    forRootState(current.rootState, claims)

  def forDeltaExtraction(
      delta: StrategicDeltaExtraction,
      claims: IterableOnce[CertificationEvidenceClaim]
  ): CertificationEvidence =
    forRootState(delta.after.rootState, claims)

  private def build(
      boundRootState: Option[RootStateVector],
      claims: IterableOnce[CertificationEvidenceClaim]
  ): CertificationEvidence =
    val normalized = claims.iterator.toVector
    val duplicateKeys =
      normalized
        .groupBy(_.lookupKey)
        .collect { case (key, grouped) if grouped.size > 1 =>
          s"${key.familyId.value}|${if key.owner.white then "white" else "black"}|${key.anchor.key}"
        }
        .toVector
        .sorted
    require(
      duplicateKeys.isEmpty,
      s"Duplicate certification evidence entries are not allowed: ${duplicateKeys.mkString(", ")}"
    )
    new CertificationEvidence(
      boundRootState,
      normalized.sortBy(claim =>
        s"${claim.familyId.value}|${if claim.owner.white then 0 else 1}|${claim.anchor.kind.sortOrder}|${claim.anchor.key}"
      )
    )

  def apply(
      familyId: CertificationId,
      color: Color,
      anchor: WitnessAnchor = WitnessAnchor.BoardAnchor,
      purposeStrengths: Map[CertificationEvidencePurpose, CertificationEvidenceStrength],
      payload: WitnessPayload = WitnessPayload.empty
  ): CertificationEvidenceClaim =
    CertificationEvidenceClaim(
      familyId = familyId,
      owner = color,
      anchor = anchor,
      purposeStrengths = purposeStrengths,
      payload = payload
    )

type CertificationEngineEvidence = CertificationEvidence
object CertificationEngineEvidence:

  val empty: CertificationEngineEvidence = CertificationEvidence.empty

  def forObjectExtraction(
      current: StrategicObjectExtraction,
      claims: IterableOnce[CertificationEvidenceClaim]
  ): CertificationEngineEvidence =
    CertificationEvidence.forObjectExtraction(current, claims)

  def forDeltaExtraction(
      delta: StrategicDeltaExtraction,
      claims: IterableOnce[CertificationEvidenceClaim]
  ): CertificationEngineEvidence =
    CertificationEvidence.forDeltaExtraction(delta, claims)

  def apply(
      familyId: CertificationId,
      color: Color,
      anchor: WitnessAnchor = WitnessAnchor.BoardAnchor,
      purposeStrengths: Map[CertificationEvidencePurpose, CertificationEvidenceStrength],
      payload: WitnessPayload = WitnessPayload.empty
  ): CertificationEvidenceClaim =
    CertificationEvidence(familyId, color, anchor, purposeStrengths, payload)

  /** Fail-closed placeholder until a typed probe adapter lands. */
  def fromProbe(probe: Any): CertificationEngineEvidence =
    probe match
      case _ => empty

type CertificationEvidenceBundle = CertificationEvidence
object CertificationEvidenceBundle:

  val empty: CertificationEvidenceBundle = CertificationEvidence.empty

  def forObjectExtraction(
      current: StrategicObjectExtraction,
      claims: IterableOnce[CertificationEvidenceClaim]
  ): CertificationEvidenceBundle =
    CertificationEvidence.forObjectExtraction(current, claims)

  def forDeltaExtraction(
      delta: StrategicDeltaExtraction,
      claims: IterableOnce[CertificationEvidenceClaim]
  ): CertificationEvidenceBundle =
    CertificationEvidence.forDeltaExtraction(delta, claims)

final case class Certification(
    familyId: CertificationId,
    scope: CertificationScope,
    burdenTag: CertificationBurdenTag,
    verdict: CertificationVerdict,
    anchor: WitnessAnchor,
    owner: Option[Color] = None,
    payload: WitnessPayload = WitnessPayload.empty,
    support: WitnessSupport = WitnessSupport.empty
):

  def color: Option[Color] = owner

  def merge(other: Certification): Certification =
    require(
      identityKey == other.identityKey,
      s"Cannot merge different certification identities: $identityKey vs ${other.identityKey}"
    )
    copy(
      payload = payload.merge(other.payload),
      support = support.merge(other.support)
    )

  def sortKey: String =
    f"${familyId.value}|${scope.key}|${verdict.sortOrder}%02d|${ownerSortOrder}%02d|${anchor.kind.sortOrder}%02d|${anchor.key}"

  private[certification] def identityKey: Certification.IdentityKey =
    Certification.IdentityKey(familyId, owner, anchor)

  private def ownerSortOrder: Int =
    owner match
      case Some(color) if color.white => 0
      case Some(_) => 1
      case None => 2

object Certification:

  def apply(
      familyId: CertificationId,
      scope: CertificationScope,
      burdenTag: CertificationBurdenTag,
      verdict: CertificationVerdict,
      anchor: WitnessAnchor,
      color: Color,
      payload: WitnessPayload,
      support: WitnessSupport
  ): Certification =
    new Certification(
      familyId = familyId,
      scope = scope,
      burdenTag = burdenTag,
      verdict = verdict,
      anchor = anchor,
      owner = Some(color),
      payload = payload,
      support = support
    )

  private[certification] final case class IdentityKey(
      familyId: CertificationId,
      owner: Option[Color],
      anchor: WitnessAnchor
  )

type CertificationClaim = Certification

final case class CertificationSet private (all: Vector[Certification]):

  lazy val byFamilyId: Map[CertificationId, Vector[Certification]] =
    all.groupBy(_.familyId).view.mapValues(_.sortBy(_.sortKey)).toMap

  def forFamilyId(familyId: CertificationId): Vector[Certification] =
    byFamilyId.getOrElse(familyId, Vector.empty)

  @targetName("forFamilyIdString")
  def forFamilyId(familyId: String): Vector[Certification] =
    forFamilyId(CertificationId(familyId))

  def verdictOf(
      familyId: CertificationId,
      owner: Color,
      anchor: WitnessAnchor = WitnessAnchor.BoardAnchor
  ): Option[CertificationVerdict] =
    forFamilyId(familyId).find(cert =>
      cert.owner.contains(owner) && cert.anchor == anchor
    ).map(_.verdict)

  def contains(
      familyId: CertificationId,
      anchor: WitnessAnchor,
      owner: Option[Color] = None,
      verdict: Option[CertificationVerdict] = None
  ): Boolean =
    all.exists(cert =>
      cert.familyId == familyId &&
        cert.anchor == anchor &&
        owner.forall(cert.owner.contains) &&
        verdict.forall(_ == cert.verdict)
    )

  @targetName("containsString")
  def contains(
      familyId: String,
      anchor: WitnessAnchor,
      owner: Option[Color],
      verdict: Option[CertificationVerdict]
  ): Boolean =
    contains(CertificationId(familyId), anchor, owner, verdict)

  def isEmpty: Boolean = all.isEmpty

object CertificationSet:

  val empty: CertificationSet = new CertificationSet(Vector.empty)

  def apply(claims: IterableOnce[Certification]): CertificationSet =
    val merged =
      claims.iterator.foldLeft(Map.empty[Certification.IdentityKey, Certification]):
        case (acc, claim) =>
          acc.updatedWith(claim.identityKey):
            case Some(existing) => Some(existing.merge(claim))
            case None => Some(claim)

    new CertificationSet(merged.values.toVector.sortBy(_.sortKey))

final case class CertificationExtraction(
    current: StrategicObjectExtraction,
    delta: Option[StrategicDeltaExtraction],
    evidence: CertificationEvidenceBundle,
    claims: CertificationSet
):

  def objectExtraction: StrategicObjectExtraction = current
  def strategicObjects: StrategicObjectExtraction = current
  def evidenceBundle: CertificationEvidenceBundle = evidence
  def certifications: CertificationSet = claims
