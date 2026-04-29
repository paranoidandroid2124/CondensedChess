package lila.commentary.render

import lila.commentary.render.annotation.{ BookAnnotationPlanner, EnglishLineCommentaryWriter, LineCommentaryPlanner }
import lila.commentary.selection.{ CommentaryPlan, WordingStrength }

object CommentaryRenderer:

  def render(plan: CommentaryPlan): CommentaryRender =
    val contractRender = CommentaryRendererContract.render(plan)
    val lineComments =
      EnglishLineCommentaryWriter
        .write(LineCommentaryPlanner.plan(BookAnnotationPlanner.plan(plan)))
        .comments
        .map(comment => comment.annotationId -> comment.comment)
        .toMap
    contractRender.copy(
      blocks = contractRender.blocks.map(block =>
        block.copy(text = block.text.copy(publicText = publicTextFor(block, lineComments)))
      )
    )

  private def publicTextFor(block: RenderBlock, lineComments: Map[String, String]): Option[String] =
    if block.role == RenderRole.Primary then lineComments.get(block.claimId).orElse(labelFor(block))
    else labelFor(block)

  private def labelFor(block: RenderBlock): Option[String] =
    Option.when(block.wordingStrength.rank >= WordingStrength.ContextOnly.rank):
      block.role match
        case RenderRole.Primary => "Primary"
        case RenderRole.Supporting => "Support"
        case RenderRole.Context => "Context"
        case RenderRole.Contrast => "Contrast"
