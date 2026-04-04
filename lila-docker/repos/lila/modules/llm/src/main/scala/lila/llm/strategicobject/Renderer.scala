package lila.llm.strategicobject

final case class RenderedCommentary(
    primary: String,
    support: List[String] = Nil
)

trait Renderer:
  def render(
      question: PlannedQuestion,
      claims: List[CertifiedClaim]
  ): RenderedCommentary
