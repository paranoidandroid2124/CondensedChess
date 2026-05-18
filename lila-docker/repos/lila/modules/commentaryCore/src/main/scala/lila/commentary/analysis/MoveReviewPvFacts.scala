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

  def toSquare(uci: String): Option[String] =
    val move = normalizeUci(uci)
    Option.when(move.length >= 4)(move.slice(2, 4))

  def fromSquare(uci: String): Option[String] =
    val move = normalizeUci(uci)
    Option.when(move.length >= 4)(move.take(2))

  def isCaptureLike(move: MoveReviewMoveRef): Boolean =
    val uci = normalizeUci(move.uci)
    move.san.contains("x") ||
      (uci.length >= 4 && uci.charAt(0) != uci.charAt(2) && move.san.headOption.forall(!"KQRBN".contains(_)))

  def isImmediateRecapture(played: MoveReviewBoardFacts.MoveFacts, reply: Option[MoveReviewMoveRef]): Boolean =
    reply.exists { move =>
      isCaptureLike(move) && toSquare(move.uci).contains(played.toKey)
    }

  def isCentralBreak(uci: String, whiteMove: Boolean): Boolean =
    val move = normalizeUci(uci)
    val whiteBreaks = Set("d2d4", "e2e4", "c2c4")
    val blackBreaks = Set("d7d5", "e7e5", "c7c5")
    if whiteMove then whiteBreaks.contains(move) else blackBreaks.contains(move)

  def isCenterCapture(uci: String): Boolean =
    val move = normalizeUci(uci)
    move.length >= 4 &&
      MoveReviewBoardFacts.CenterSquares.contains(move.slice(2, 4)) &&
      move.take(2).headOption.exists(file => file != move.slice(2, 4).headOption.getOrElse(file))

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
