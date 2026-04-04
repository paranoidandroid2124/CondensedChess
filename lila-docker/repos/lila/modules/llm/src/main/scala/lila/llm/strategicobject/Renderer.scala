package lila.llm.strategicobject

final case class RenderedCommentary(
    claimIds: List[String],
    supportClaimIds: List[String] = Nil
)

trait Renderer:
  def render(
      question: PlannedQuestion,
      claims: List[CertifiedClaim]
  ): RenderedCommentary
