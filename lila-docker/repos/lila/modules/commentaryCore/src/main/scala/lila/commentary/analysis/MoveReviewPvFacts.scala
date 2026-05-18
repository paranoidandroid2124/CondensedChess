package lila.commentary.analysis

import lila.commentary.{ MoveReviewMoveRef, MoveReviewRefs, MoveReviewShortLine, MoveReviewVariationRef }

private[commentary] object MoveReviewPvFacts:

  final case class LineFacts(
      line: MoveReviewVariationRef,
      first: MoveReviewMoveRef,
      reply: Option[MoveReviewMoveRef],
      continuation: Option[MoveReviewMoveRef]
  )

  def firstCoupled(startFen: String, playedUci: String, refs: Option[MoveReviewRefs]): Option[LineFacts] =
    val normalizedPlayed = normalizeUci(playedUci)
    refs.filter(ref => normalizeFen(ref.startFen) == normalizeFen(startFen)).toList
      .flatMap(_.variations)
      .flatMap(line => MoveReviewPvChainValidator.validatedLine(startFen, line, normalizedPlayed))
      .find(validated => coupledLine(validated.line, normalizedPlayed))
      .flatMap { validated =>
        validated.first.map { first =>
          LineFacts(validated.line, first, validated.reply, validated.continuation)
        }
      }

  def shortLine(refs: Option[MoveReviewRefs], preferredLineId: Option[String]): Option[MoveReviewShortLine] =
    refs.flatMap { refs =>
      preferredLineId
        .flatMap(id => refs.variations.find(_.lineId == id))
        .orElse(refs.variations.headOption)
    }.flatMap { line =>
      val moves = line.moves.take(5)
      val san = moves.map(_.san.trim).filter(_.nonEmpty)
      Option.when(san.nonEmpty) {
        MoveReviewShortLine(
          san = san,
          uci = moves.map(_.uci.trim).filter(_.nonEmpty),
          lineId = Some(line.lineId),
          scoreCp = Some(line.scoreCp),
          mate = line.mate,
          depth = Some(line.depth),
          source = "pv"
        )
      }
    }

  def normalizeUci(uci: String): String =
    Option(uci).getOrElse("").trim.toLowerCase

  private def coupledLine(line: MoveReviewVariationRef, playedUci: String): Boolean =
    line.moves.headOption.exists { first =>
      normalizeUci(first.uci) == playedUci &&
        line.moves.size >= 2 &&
        first.fenAfter.trim.nonEmpty &&
        line.moves.take(3).forall(_.fenAfter.trim.nonEmpty) &&
        strictlyOrdered(line.moves)
    }

  private def strictlyOrdered(moves: List[MoveReviewMoveRef]): Boolean =
    moves.sliding(2).forall {
      case List(left, right) => left.ply < right.ply
      case _                 => true
    }

  private def normalizeFen(fen: String): String =
    Option(fen).getOrElse("").trim.split("\\s+").filter(_.nonEmpty).mkString(" ")
