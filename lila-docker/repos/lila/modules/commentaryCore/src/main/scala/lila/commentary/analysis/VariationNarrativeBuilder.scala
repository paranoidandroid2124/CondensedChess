package lila.commentary.analysis

import chess.*
import lila.commentary.model.NarrativeContext

private[analysis] object VariationNarrativeBuilder:

  def build(ctx: NarrativeContext, evidence: LineConsequenceEvidence): Option[String] =
    val steps = LineConsequenceEvaluator.replaySteps(ctx.fen, evidence.uciMoves, evidence.sanMoves)
    if (steps.isEmpty) None
    else
      val startPly = NarrativeUtils.plyFromFen(ctx.fen).map(_ + 1).getOrElse(1)
      val formattedLine = NarrativeUtils.formatSanWithMoveNumbers(startPly, steps.map(_.san))
      
      evidence.kind match
        case LineConsequenceKind.ExchangeSequence =>
          val exchanges = steps.filter(_.captures)
          val structureDetail = LineConsequenceEvaluator.primaryStructureDetailSurface(evidence.structureDetails)
          val queenTradeSquare =
            exchanges
              .filter(_.capturedRole.contains(Queen))
              .groupBy(_.dest)
              .collectFirst { case (square, queenCaptures) if queenCaptures.size >= 2 => square.key }
          val description =
            queenTradeSquare match
              case Some(square) =>
                s"this exchange sequence includes a queen trade on $square, changing which pieces remain"
              case None if exchanges.length >= 2 =>
                val first = exchanges.head
                val target = first.dest.key
                val result = structureDetail.getOrElse("settling which pieces and pawns remain")
                s"this exchange sequence trades the ${pieceName(first.role)} for the ${pieceName(first.capturedRole)} on $target, $result"
              case None =>
                structureDetail
                  .map(detail => s"this exchange sequence resolves the tension, $detail")
                  .getOrElse("this exchange sequence resolves the tension")
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.ForcingCheckSequence =>
          val checks = steps.filter(step => step.givesCheck || step.san.contains("+") || step.san.contains("#"))
          val description =
            if (checks.nonEmpty) {
              s"the forcing sequence beginning with ${checks.head.san} delivers check, narrowing the defender's replies before the position settles"
            } else {
              "this forcing line checks the king and narrows the replies before the position can settle"
            }
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.MaterialTransition =>
          val firstCapture = steps.find(_.captures)
          val description =
            firstCapture match
              case Some(step) if step.capturedRole.exists(_ != Pawn) =>
                s"this shifts the material balance, transitioning into a setup with a ${pieceName(step.role)} for ${pieceName(step.capturedRole)}"
              case _ =>
                "this trade transitions the material balance"
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.ImmediateOpponentPawnCapture =>
          val capture = steps.lift(1).filter(step => step.captures && step.capturedRole.contains(Pawn))
          val description =
            capture match
              case Some(step) =>
                s"${sideName(step.color)} answers with ${step.san}, immediately taking the pawn on ${step.dest.key}"
              case None =>
                "the opponent immediately answers by taking a pawn"
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.ImmediateOpponentTargetPressure =>
          val reply = steps.lift(1).filter(step => step.color != steps.head.color)
          val targetSurface = LineConsequenceEvaluator.targetPressureDetailSurface(evidence.targetDetails)
          val description =
            reply match
              case Some(step) =>
                targetSurface
                  .map(targets => s"${sideName(step.color)} answers with ${step.san}, putting immediate pressure on $targets near the king")
                  .getOrElse(s"${sideName(step.color)} answers with ${step.san}, putting immediate pressure around the king")
              case None =>
                targetSurface
                  .map(targets => s"the opponent immediately creates pressure on $targets near the king")
                  .getOrElse("the opponent immediately creates pressure around the king")
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.PlayedMoveTargetPressure =>
          for
            targetSurface <- LineConsequenceEvaluator.targetPressureDetailSurface(evidence.targetDetails)
            continuation <- LineConsequenceEvaluator.firstContinuationByPlayedPiece(steps)
          yield
            val lead = steps.head
            val description =
              s"${lead.san} puts pressure on $targetSurface, and the same ${pieceName(lead.role)} stays active with ${continuation.san}"
            s"On the checked line $formattedLine, $description."

        case LineConsequenceKind.DelayedPawnCapture =>
          val capture = steps.find(step => step.captures && step.capturedRole.contains(Pawn))
          val description =
            capture match
              case Some(step) =>
                s"the line reaches ${step.san}, so the pawn capture arrives after the first move"
              case None =>
                "the line reaches a pawn capture after the first move"
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.CentralBreakTiming =>
          val breakMove = evidence.triggerSan.getOrElse(steps.head.san)
          val description = s"this times the central break with $breakMove, changing the pawn structure before counterplay settles"
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.CentralPawnAdvance =>
          val advance = steps.find(step =>
            step.role == Pawn &&
              !step.captures &&
              Set(Square.D4, Square.E4, Square.D5, Square.E5).contains(step.dest)
          )
          val description =
            advance match
              case Some(step) => s"the pawn advances to ${step.dest.key}, modifying the central pawn structure"
              case _ => "a central pawn advance changes the pawn structure"
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.PassedPawnCreation =>
          val passer = steps.find(_.createsPassedPawn)
          val description =
            passer match
              case Some(step) =>
                s"the pawn reaches ${step.dest.key} as a passed pawn, giving the line a concrete passer cue"
              case _ =>
                "the line creates a passed pawn and gives the position a concrete passer cue"
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.PromotionRace =>
          val passer = steps.find(step => step.promotes || step.advancesPassedPawn)
          val description =
            passer match
              case Some(step) if step.promotes =>
                s"${step.san} promotes the pawn, so promotion is the concrete line feature"
              case Some(step) =>
                s"the passed pawn advances to ${step.dest.key}, making promotion tempo the concrete issue"
              case _ =>
                "the passed-pawn advance makes promotion tempo the concrete issue"
          Some(s"On the checked line $formattedLine, $description.")

        case LineConsequenceKind.OriginSquareClearance =>
          for
            lead <- steps.headOption
            reuse <- LineConsequenceEvaluator.originSquareClearanceStep(ctx, steps)
          yield
            val description =
              s"${lead.san} clears ${lead.orig.key}, and ${reuse.san} later uses that square"
            s"On the checked line $formattedLine, $description."

        case LineConsequenceKind.MinorPieceReroute =>
          for
            lead <- steps.headOption
            continuation <- LineConsequenceEvaluator.firstContinuationByPlayedPiece(steps)
          yield
            val description =
              s"${lead.san} starts a ${pieceName(lead.role)} route, and ${continuation.san} shows the same ${pieceName(lead.role)} continuing that route"
            s"On the checked line $formattedLine, $description."

        case LineConsequenceKind.PreviewOnly =>
          val preview = evidence.sanMoves.take(4).map(_.trim).filter(_.nonEmpty).mkString(" ")
          Option.when(preview.nonEmpty)(s"The checked line continues $preview.")

  private def pieceName(role: Role): String =
    role match
      case Pawn   => "pawn"
      case Knight => "knight"
      case Bishop => "bishop"
      case Rook   => "rook"
      case Queen  => "queen"
      case King   => "king"

  private def pieceName(roleOpt: Option[Role]): String =
    roleOpt.map(pieceName).getOrElse("piece")

  private def sideName(color: Color): String =
    if color.white then "White" else "Black"
