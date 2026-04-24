package lila.commentary.render

import lila.commentary.selection.{ CommentaryPlan, WordingStrength }

object CommentaryRenderer:

  def render(plan: CommentaryPlan): CommentaryRender =
    val contractRender = CommentaryRendererContract.render(plan)
    contractRender.copy(
      blocks = contractRender.blocks.map(block =>
        block.copy(text = block.text.copy(publicText = labelFor(block)))
      )
    )

  private def labelFor(block: RenderBlock): Option[String] =
    Option.when(block.wordingStrength.rank >= WordingStrength.ContextOnly.rank):
      block.role match
        case RenderRole.Primary => "Primary"
        case RenderRole.Supporting => "Support"
        case RenderRole.Context => "Context"
        case RenderRole.Contrast => "Contrast"
