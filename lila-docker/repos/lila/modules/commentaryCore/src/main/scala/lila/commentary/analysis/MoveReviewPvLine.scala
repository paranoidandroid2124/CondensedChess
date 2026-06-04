package lila.commentary.analysis

import lila.commentary.{ MoveReviewMoveRef, MoveReviewRefs, MoveReviewShortLine, MoveReviewVariationRef }

private[commentary] object MoveReviewPvLine:

  final case class LineFacts(
      line: MoveReviewVariationRef,
      first: MoveReviewMoveRef,
      reply: Option[MoveReviewMoveRef],
      continuation: Option[MoveReviewMoveRef]
  )

  final case class ValidatedLine(
      line: MoveReviewVariationRef,
      moves: List[MoveReviewMoveRef]
  ):
    def first: Option[MoveReviewMoveRef] = moves.headOption
    def reply: Option[MoveReviewMoveRef] = moves.lift(1)
    def continuation: Option[MoveReviewMoveRef] = moves.lift(2)

  def firstCoupled(startFen: String, playedUci: String, refs: Option[MoveReviewRefs]): Option[LineFacts] =
    val normalizedPlayed = normalizeUci(playedUci)
    refs.filter(ref => normalizeFen(ref.startFen) == normalizeFen(startFen)).toList
      .flatMap(_.variations)
      .flatMap(line => validatedLine(startFen, line, normalizedPlayed))
      .find(_.reply.nonEmpty)
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

  def validatedLine(
      startFen: String,
      line: MoveReviewVariationRef,
      playedUci: String
  ): Option[ValidatedLine] =
    val normalizedPlayed = normalizeUci(playedUci)
    val moves = line.moves
    Option.when(
      moves.headOption.exists(move => normalizeUci(move.uci) == normalizedPlayed) &&
        moves.forall(_.fenAfter.trim.nonEmpty) &&
        strictlyOrdered(moves)
    )(moves)
      .flatMap(replay(startFen, _))
      .map(validatedMoves => ValidatedLine(line, validatedMoves))

  def legalFenAfter(fen: String, uciMove: String): Option[String] =
    val normalized = normalizeUci(uciMove)
    Option.when(normalized.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"))(normalized).flatMap { moveStr =>
      val after = NarrativeUtils.uciListToFen(fen, List(moveStr))
      Option.when(boardStateFen(after) != boardStateFen(fen))(after)
    }

  def normalizeUci(uci: String): String =
    NarrativeUtils.normalizeUciMove(uci)

  private def replay(startFen: String, moves: List[MoveReviewMoveRef]): Option[List[MoveReviewMoveRef]] =
    val accepted = scala.collection.mutable.ListBuffer.empty[MoveReviewMoveRef]
    var currentFen = normalizeFen(startFen)
    var ok = true
    val it = moves.iterator
    while it.hasNext && ok do
      val move = it.next()
      legalFenAfter(currentFen, move.uci) match
        case Some(actualFen) if boardStateFen(actualFen) == boardStateFen(move.fenAfter) =>
          accepted += move
          currentFen = actualFen
        case _ =>
          ok = false
    Option.when(ok)(accepted.toList)

  private def strictlyOrdered(moves: List[MoveReviewMoveRef]): Boolean =
    moves.sliding(2).forall {
      case List(left, right) => left.ply < right.ply
      case _                 => true
    }

  private def normalizeFen(fen: String): String =
    Option(fen).getOrElse("").trim.split("\\s+").filter(_.nonEmpty).mkString(" ")

  private def boardStateFen(fen: String): String =
    normalizeFen(fen).split("\\s+").take(4).mkString(" ")
