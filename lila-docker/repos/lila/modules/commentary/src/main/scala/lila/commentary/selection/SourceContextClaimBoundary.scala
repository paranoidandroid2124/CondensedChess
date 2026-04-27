package lila.commentary.selection

final case class SourceContextExactRef(
    id: String,
    owner: Option[String] = None,
    anchor: Option[String] = None,
    route: Option[String] = None,
    scope: Option[String] = None
):
  require(id.trim.nonEmpty, "SourceContextExactRef id must be non-empty")

final case class SourceContextCandidate(
    candidateId: String,
    kind: SourceContextKind,
    sourceRefs: Vector[String],
    exactBoardRefs: Vector[SourceContextExactRef] = Vector.empty
):
  require(candidateId.trim.nonEmpty, "SourceContextCandidate id must be non-empty")
  require(SourceContextCandidate.isPublicSafeId(candidateId), "SourceContextCandidate id must be public-safe")

object SourceContextCandidate:
  def hasPublicSafeShape(candidateId: String): Boolean =
    candidateId.trim.matches("^[A-Za-z0-9][A-Za-z0-9_-]*$")

  def isPublicSafeId(candidateId: String): Boolean =
    val trimmed = candidateId.trim
    hasPublicSafeShape(trimmed) && !containsForbiddenPublicIdToken(trimmed)

  private def containsForbiddenPublicIdToken(candidateId: String): Boolean =
    val normalized = candidateId.toLowerCase.replace('-', '_').replace(':', '_').replace(' ', '_')
    Vector(
      "best",
      "recommend",
      "recommendation",
      "theory",
      "truth",
      "forced",
      "result",
      "engine",
      "oracle",
      "winning",
      "drawing",
      "wdl",
      "dtz",
      "dtm",
      "player",
      "event",
      "game_url",
      "gameurl",
      "url",
      "citation",
      "metadata",
      "draw_offer",
      "repetition",
      "tournament",
      "rating",
      "time_control",
      "1_0",
      "0_1",
      "1_2_1_2"
    ).exists(normalized.contains)

object SourceContextClaimBoundary:

  def toClaim(candidate: SourceContextCandidate): CommentaryClaim =
    val sourceEvidence =
      candidate.sourceRefs
        .filter(_.trim.nonEmpty)
        .map(ref => EvidenceRef(EvidenceRefKind.SourceContext, ref.trim))
    val exactEvidence =
      candidate.exactBoardRefs.map(ref =>
        EvidenceRef(
          kind = EvidenceRefKind.ExactBoard,
          id = ref.id.trim,
          owner = ref.owner,
          anchor = ref.anchor,
          route = ref.route,
          scope = ref.scope
        )
      )
    CommentaryClaim(
      id = candidate.candidateId.trim,
      layer = ClaimLayer.SourceContext,
      status = ClaimStatus.Context,
      route = Some(candidate.kind.key),
      scope = Some("context_only"),
      evidenceRefs = (sourceEvidence ++ exactEvidence).distinct,
      exactBoardBound = false,
      wordingStrengthCap = WordingStrength.ContextOnly,
      sourceContextKind = Some(candidate.kind)
    )
