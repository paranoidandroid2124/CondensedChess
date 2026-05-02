package lila.commentary.projection

import chess.Color

import lila.commentary.root.RootStateVector
import lila.commentary.selection.WordingStrength
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

final case class StrategyProjectionPhraseCapability(
    allowedPredicateKey: String,
    sanOnlyVariationEvidence: Boolean,
    allowsResultLanguage: Boolean,
    allowsBestForcedLanguage: Boolean,
    allowsEngineLanguage: Boolean,
    allowsFallbackText: Boolean,
    forbiddenTerms: Vector[String]
):
  require(allowedPredicateKey.trim.nonEmpty, "Strategy projection phrase capability requires a predicate key")
  require(forbiddenTerms.nonEmpty, "Strategy projection phrase capability requires forbidden terms")

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

enum StrategyProjectionAdmissionStatus(val key: String):
  case Admitted extends StrategyProjectionAdmissionStatus("admitted")
  case Rejected extends StrategyProjectionAdmissionStatus("rejected")

enum StrategyProjectionAdmissionAuthority(val key: String):
  case DescriptorCertifiedRuntime extends StrategyProjectionAdmissionAuthority("descriptor_certified_runtime")
  case LegacyValidationScaffold extends StrategyProjectionAdmissionAuthority("legacy_validation_scaffold")

enum StrategyProjectionCarrierKind(val key: String):
  case ExactBoard extends StrategyProjectionCarrierKind("exact_board")
  case Root extends StrategyProjectionCarrierKind("root")
  case Witness extends StrategyProjectionCarrierKind("witness")
  case Object extends StrategyProjectionCarrierKind("object")
  case Delta extends StrategyProjectionCarrierKind("delta")
  case Certification extends StrategyProjectionCarrierKind("certification")

final case class StrategyProjectionCarrierRef(
    kind: StrategyProjectionCarrierKind,
    id: String,
    owner: String,
    anchor: String,
    route: String,
    scope: String,
    binding: Map[String, String] = Map.empty
):
  require(id.trim.nonEmpty, "Strategy projection carrier id must be non-empty")
  require(owner.trim.nonEmpty, "Strategy projection carrier owner must be non-empty")
  require(anchor.trim.nonEmpty, "Strategy projection carrier anchor must be non-empty")
  require(route.trim.nonEmpty, "Strategy projection carrier route must be non-empty")
  require(scope.trim.nonEmpty, "Strategy projection carrier scope must be non-empty")

  def bindingValue(key: String): Option[String] =
    binding.get(key).map(_.trim).filter(_.nonEmpty)

final case class StrategyProjectionAdmissionResult private[projection] (
    projectionId: String,
    authority: StrategyProjectionAdmissionAuthority,
    status: StrategyProjectionAdmissionStatus,
    runtimeKId: Option[String],
    bandId: StrategyProjectionBandId,
    sourceRootState: RootStateVector,
    currentRootState: RootStateVector,
    evidenceKinds: Vector[StrategyProjectionEvidenceKind],
    owner: Color,
    beneficiary: Option[Color],
    defender: Option[Color],
    anchor: WitnessAnchor,
    route: String,
    scope: String,
    lowerCarrierRefs: Vector[StrategyProjectionCarrierRef],
    wordingStrengthCap: WordingStrength,
    phraseCapability: StrategyProjectionPhraseCapability,
    publicSurfaceForbiddenTerms: Vector[String],
    rejectionReason: Option[String]
):
  require(projectionId.trim.nonEmpty, "Strategy projection admission id must be non-empty")
  require(route.trim.nonEmpty, "Strategy projection admission route must be non-empty")
  require(scope.trim.nonEmpty, "Strategy projection admission scope must be non-empty")
  runtimeKId.foreach(id => require(id.trim.nonEmpty, "Strategy projection runtime K id must be non-empty"))

  def admitted: Boolean = status == StrategyProjectionAdmissionStatus.Admitted

object StrategyProjectionAdmissionResult:

  private val PublicSafeId = "^[A-Za-z0-9][A-Za-z0-9_-]*$".r
  private val AllowedScopes = Set("position_local", "move_local", "exact_current_board", "exact_transition")
  private val ForbiddenTokens =
    Vector("best", "forced", "winning", "drawn", "result", "oracle", "theory", "recommend", "engine", "source")
  private val DefaultPhraseCapability =
    StrategyProjectionPhraseCapability(
      allowedPredicateKey = "strategy_projection",
      sanOnlyVariationEvidence = true,
      allowsResultLanguage = false,
      allowsBestForcedLanguage = false,
      allowsEngineLanguage = false,
      allowsFallbackText = false,
      forbiddenTerms = ForbiddenTokens
    )

  private[projection] def fromDecision(
      projectionId: String,
      authority: StrategyProjectionAdmissionAuthority,
      runtimeK: Option[StrategyRuntimeKProducer.RuntimeK] = None,
      bandId: StrategyProjectionBandId,
      sourceRootState: RootStateVector,
      currentRootState: RootStateVector,
      evidenceKinds: Vector[StrategyProjectionEvidenceKind],
      owner: Color,
      beneficiary: Option[Color],
      defender: Option[Color],
      anchor: WitnessAnchor,
      route: String,
      scope: String,
      lowerCarrierRefs: Vector[StrategyProjectionCarrierRef],
      wordingStrengthCap: WordingStrength,
      phraseCapability: StrategyProjectionPhraseCapability = DefaultPhraseCapability,
      publicSurfaceForbiddenTerms: Vector[String] = Vector.empty,
      decision: Either[String, Boolean]
  ): StrategyProjectionAdmissionResult =
    val normalizedEvidenceKinds =
      evidenceKinds.distinct.sortBy(_.value)
    val rejectionReason =
      decision.left.toOption
        .orElse(Option.when(decision.contains(false))("projection_admission_rejected"))
        .orElse(Option.when(authority == StrategyProjectionAdmissionAuthority.DescriptorCertifiedRuntime && runtimeK.isEmpty)("projection_runtime_k_required"))
        .orElse(Option.when(sourceRootState != currentRootState)("projection_root_binding_mismatch"))
        .orElse(Option.when(lowerCarrierRefs.isEmpty)("projection_lower_carrier_required"))
        .orElse(Option.when(!validLowerCarrierBinding(bandId, owner, defender, anchor, route, scope, lowerCarrierRefs))("projection_lower_carrier_binding_mismatch"))
        .orElse(Option.when(normalizedEvidenceKinds.isEmpty)("projection_evidence_required"))
        .orElse(Option.when(!normalizedEvidenceKinds.forall(StrategyProjectionScopeContract.isAllowedEvidenceKind(bandId, _)))("projection_evidence_kind_not_allowed"))
        .orElse(Option.when(!AllowedScopes.contains(scope))("projection_scope_not_exact"))
        .orElse(Option.when(!publicSafeId(projectionId))("projection_id_not_public_safe"))
        .orElse(Option.when(containsForbiddenToken(projectionId) || containsForbiddenToken(route) || containsForbiddenToken(scope))("projection_wording_forbidden"))

    StrategyProjectionAdmissionResult(
      projectionId = projectionId,
      authority = authority,
      status = if rejectionReason.isEmpty then StrategyProjectionAdmissionStatus.Admitted else StrategyProjectionAdmissionStatus.Rejected,
      runtimeKId = runtimeK.map(_.truth.id),
      bandId = bandId,
      sourceRootState = sourceRootState,
      currentRootState = currentRootState,
      evidenceKinds = normalizedEvidenceKinds,
      owner = owner,
      beneficiary = beneficiary,
      defender = defender,
      anchor = anchor,
      route = route,
      scope = scope,
      lowerCarrierRefs = lowerCarrierRefs,
      wordingStrengthCap = WordingStrength.weaker(wordingStrengthCap, WordingStrength.QualifiedSupport),
      phraseCapability = phraseCapability,
      publicSurfaceForbiddenTerms = publicSurfaceForbiddenTerms.distinct,
      rejectionReason = rejectionReason
    )

  private def validLowerCarrierBinding(
      bandId: StrategyProjectionBandId,
      owner: Color,
      defender: Option[Color],
      anchor: WitnessAnchor,
      route: String,
      scope: String,
      refs: Vector[StrategyProjectionCarrierRef]
  ): Boolean =
    refs.forall(ref =>
      validCarrierOwner(bandId, owner, defender, ref) &&
        ref.anchor == anchor.key &&
        ref.route == route &&
        ref.scope == scope
    )

  private def validCarrierOwner(
      bandId: StrategyProjectionBandId,
      owner: Color,
      defender: Option[Color],
      ref: StrategyProjectionCarrierRef
  ): Boolean =
    ref.owner == colorKey(owner) ||
      defender.exists(defenderColor =>
        ref.owner == colorKey(defenderColor) &&
          ((bandId == StrategyProjectionScopeContract.S04 &&
            ref.kind == StrategyProjectionCarrierKind.Object &&
            ref.id == "KingSafetyShell") ||
            (bandId == StrategyProjectionScopeContract.S16 &&
              ref.kind == StrategyProjectionCarrierKind.Witness &&
              ref.id == "passed_pawn_entity_state"))
      )

  private def publicSafeId(value: String): Boolean =
    PublicSafeId.matches(value)

  private def containsForbiddenToken(value: String): Boolean =
    val normalized = value.toLowerCase.replace('-', '_').replace(':', '_').replace(' ', '_')
    ForbiddenTokens.exists(normalized.contains)

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"
