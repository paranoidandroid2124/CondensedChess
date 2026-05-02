package lila.commentary.render

import lila.commentary.render.annotation.{ BookAnnotationPlanner, EnglishLineCommentaryWriter, LineCommentaryPlanner }
import lila.commentary.selection.CommentaryPlan

object CommentaryRenderer:

  def render(plan: CommentaryPlan): CommentaryRender =
    val publicPlan = CommentaryRendererContract.publicPlan(plan)
    val linePhrases =
      EnglishLineCommentaryWriter
        .write(LineCommentaryPlanner.plan(BookAnnotationPlanner.plan(plan)))
        .comments
        .map(comment =>
          comment.annotationId -> PublicPhrase(
            claimId = comment.annotationId,
            text = comment.comment,
            predicate = PublicClaimPredicate.LineCommentary,
            wordingStrength = comment.wordingCap
          )
        )
        .toMap
    CommentaryRendererContract.render(publicPlan, linePhrases)
