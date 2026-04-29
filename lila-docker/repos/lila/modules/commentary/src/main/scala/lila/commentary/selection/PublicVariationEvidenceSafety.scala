package lila.commentary.selection

private[commentary] object PublicVariationEvidenceSafety:

  def publicSafe(proof: PreparedVariationEvidence): Boolean =
    proof.publicSafe &&
      publicSafeProofId(proof.proofId) &&
      proof.surfaceAllowance != VariationSurfaceAllowance.InternalOnly &&
      proof.boundary.publicSafe &&
      proof.lineSan.nonEmpty &&
      proof.lineSan.size == proof.lineUci.size &&
      proof.lineSan.forall(_.trim.nonEmpty) &&
      proof.lineUci.forall(_.trim.nonEmpty) &&
      publicSafeProvenance(proof) &&
      !containsForbiddenProofToken(proof.proves)

  def publicSafeForClaim(claim: CommentaryClaim, proof: PreparedVariationEvidence): Boolean =
    publicSafe(proof) &&
      proof.boundClaimId == claim.id &&
      claim.owner.contains(proof.owner) &&
      claim.anchor.contains(proof.anchor) &&
      claim.route.contains(proof.route) &&
      claim.scope.contains(proof.scope) &&
      proof.defender.forall(defender => claim.defender.contains(defender))

  def lineTestProofId(ref: String): Option[String] =
    val parts = ref.split(":").toVector
    Option
      .when(parts.size == 3 && parts(1).trim.nonEmpty && parts(2) == "context" && allowedLineTestPrefix(parts.head))(
        parts(1).trim
      )
      .filter(publicSafeProofId)

  def lineTestProofIdForKind(ref: String, kind: SourceContextKind): Option[String] =
    val parts = ref.split(":").toVector
    Option
      .when(
        parts.size == 3 &&
          parts(1).trim.nonEmpty &&
          parts(2) == "context" &&
          parts.head == lineTestPrefix(kind)
      )(parts(1).trim)
      .filter(publicSafeProofId)

  def publicSafeProofId(proofId: String): Boolean =
    EvidenceRef.isPublicSafeProvenanceId(proofId)

  def isAllowedLineTestRef(ref: String): Boolean =
    ref.split(":").headOption.exists(allowedLineTestPrefix)

  private def allowedLineTestPrefix(prefix: String): Boolean =
    SourceContextKind.values.exists(kind => prefix == lineTestPrefix(kind))

  private def lineTestPrefix(kind: SourceContextKind): String =
    kind match
      case SourceContextKind.Opening => "opening-line-test"
      case SourceContextKind.Motif => "motif-line-test"
      case SourceContextKind.EndgameStudy => "endgame-line-test"
      case SourceContextKind.Retrieval => "retrieval-line-test"

  private def publicSafeProvenance(proof: PreparedVariationEvidence): Boolean =
    proof.provenanceRefs.nonEmpty &&
      proof.provenanceRefs.forall: ref =>
        ref.kind != EvidenceRefKind.RawEngine &&
          ref.kind != EvidenceRefKind.SourceContext &&
          EvidenceRef.isPublicSafeProvenanceId(ref.id) &&
          ref.owner.contains(proof.owner) &&
          ref.anchor.contains(proof.anchor) &&
          ref.route.contains(proof.route) &&
          ref.scope.contains(proof.scope)

  private def containsForbiddenProofToken(value: String): Boolean =
    val normalized = normalizedToken(value)
    Vector(
      "best",
      "forced",
      "winning",
      "drawing",
      "drawn",
      "result",
      "oracle",
      "engine",
      "raw_pv",
      "eval",
      "theory_truth"
    ).exists(normalized.contains)

  private def normalizedToken(value: String): String =
    value.toLowerCase.replace('-', '_').replace(':', '_').replace(' ', '_').replace('+', '_')
