package lila.commentary.analysis

import chess.format.{ Fen, Uci }
import chess.variant.Standard
import lila.commentary.{ MoveReviewExplanation, MoveReviewRefs, StrategyPack }
import lila.commentary.model.*

private[commentary] object MoveReviewExplanationBuilder:

  def build(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract] = None,
      strategyPack: Option[StrategyPack] = None
  ): Option[MoveReviewExplanation] =
    for
      played <- current(ctx)
      lineFacts = MoveReviewPvLine.firstCoupled(ctx.fen, played.uci, refs)
      evidence = moveReviewEvidence(ctx, played, strategyPack, truthContract)
      descriptor <- CommentaryIdeaSurface.describe(played, evidence, lineFacts, truthContract)
      pvInterpretation = descriptor.pvInterpretation(lineFacts)
      shortLine = MoveReviewPvLine.shortLine(refs, pvInterpretation.flatMap(_.supportedByLineId))
    yield
      MoveReviewExplanation(
        title = descriptor.title,
        prose = descriptor.prose,
        qualityLabel = qualityLabel(truthContract),
        reasonTags = descriptor.reasonTags,
        shortLine = shortLine,
        pvInterpretation = pvInterpretation,
        source = descriptor.source,
        factFragments = Option.when(descriptor.factFragments.nonEmpty)(descriptor.factFragments)
      )

  def current(ctx: NarrativeContext): Option[CommentaryIdeaSurface.PlayedMove] =
    for
      uci <- ctx.playedMove.map(MoveReviewPvLine.normalizeUci)
      parsed <- Uci(uci).collect { case move: Uci.Move => move }
      san <- ctx.playedSan.map(_.trim).filter(_.nonEmpty)
      before <- Fen.read(Standard, Fen.Full(ctx.fen))
      move <- before.move(parsed).toOption
      after = move.after
      afterFen = Fen.write(after).value
      movedPiece <- after.board.pieceAt(parsed.dest)
      if movedPiece.color == move.piece.color
    yield
      CommentaryIdeaSurface.PlayedMove(
        uci = uci,
        san = san,
        from = parsed.orig,
        to = parsed.dest,
        piece = move.piece,
        afterFen = afterFen,
        capturedRole = before.board.pieceAt(parsed.dest).filter(_.color != move.piece.color).map(_.role)
      )

  private def moveReviewEvidence(
      ctx: NarrativeContext,
      played: CommentaryIdeaSurface.PlayedMove,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract]
  ): CommentaryIdeaSurface.MoveReviewEvidence =
    val playedCandidate =
      ctx.candidates.find(candidate =>
        candidate.uci.exists(uci => MoveReviewPvLine.normalizeUci(uci) == played.uci) ||
          sanitizeSan(candidate.move) == sanitizeSan(played.san)
      )
    val facts =
      (playedCandidate.toList.flatMap(_.facts) ++ ctx.facts ++ ctx.mainPvFacts ++ ctx.threatLineFacts ++ ctx.counterfactualFacts)
        .distinct
    val motifs =
      playedCandidate.toList.flatMap(_.lineMotifs).distinct
    val openingGoal =
      ctx.openingGoalEvaluation
        .filter(goal => goal.status == OpeningGoals.Status.Achieved || goal.status == OpeningGoals.Status.Partial)
    CommentaryIdeaSurface.MoveReviewEvidence(
      facts = facts,
      motifs = motifs,
      openingGoal = openingGoal,
      openingName = ctx.openingData.flatMap(_.name).orElse(openingNameFromEvent(ctx)),
      strategicDelta = PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(ctx, StrategyPackSurface.from(strategyPack), truthContract),
      phase = Option(ctx.phase.current).filter(_.trim.nonEmpty).getOrElse(ctx.header.phase),
      ply = ctx.ply
    )

  private def qualityLabel(truthContract: Option[DecisiveTruthContract]): Option[String] =
    truthContract.map(_.truthClass.toString)

  private def openingNameFromEvent(ctx: NarrativeContext): Option[String] =
    ctx.openingEvent.collect { case event: OpeningEvent.Intro => event.name }

  private def sanitizeSan(san: String): String =
    Option(san).getOrElse("").trim.replaceAll("""[+#?!]+$""", "")
