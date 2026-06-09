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
                s"this exchange sequence trades the ${pieceName(first.role)} for the ${pieceName(first.capturedRole)} on $target, leaving a different pawn structure"
              case None =>
                "this exchange sequence resolves the tension and alters the pawn structure"
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
