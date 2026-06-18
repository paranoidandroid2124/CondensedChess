package lila.chessjudgment.analysis

import chess.format.{ Fen, Uci }
import chess.variant.Standard

private[chessjudgment] object PrincipalVariationEvidence:

  final case class LineMoveRef(
      ply: Int,
      uci: String,
      fenAfter: String
  )

  final case class LineVariationRef(
      moves: List[LineMoveRef]
  )

  final case class LineEvidenceRefs(
      startFen: String,
      variations: List[LineVariationRef]
  )

  final case class LineFacts(
      line: LineVariationRef,
      first: LineMoveRef,
      reply: Option[LineMoveRef],
      continuation: Option[LineMoveRef],
      continuationTail: List[LineMoveRef] = Nil
  ):
    def checkedContinuations: List[LineMoveRef] =
      (continuation.toList ++ continuationTail).distinct

  final case class ValidatedLine(
      line: LineVariationRef,
      moves: List[LineMoveRef]
  ):
    def first: Option[LineMoveRef] = moves.headOption
    def reply: Option[LineMoveRef] = moves.lift(1)
    def continuation: Option[LineMoveRef] = moves.lift(2)

  def firstCoupled(startFen: String, playedUci: String, refs: Option[LineEvidenceRefs]): Option[LineFacts] =
    val normalizedPlayed = normalizeUci(playedUci)
    refs.filter(ref => normalizeFen(ref.startFen) == normalizeFen(startFen)).toList
      .flatMap(_.variations)
      .flatMap(line => validatedLine(startFen, line, normalizedPlayed))
      .find(_.reply.nonEmpty)
      .flatMap { validated =>
        validated.first.map { first =>
          LineFacts(
            validated.line,
            first,
            validated.reply,
            validated.continuation,
            validated.moves.drop(3).take(3)
          )
        }
      }

  def playedCoupled(startFen: String, playedUci: String, refs: Option[LineEvidenceRefs]): Option[LineFacts] =
    val normalizedPlayed = normalizeUci(playedUci)
    refs.filter(ref => normalizeFen(ref.startFen) == normalizeFen(startFen)).toList
      .flatMap(_.variations)
      .flatMap(line => validatedLine(startFen, line, normalizedPlayed))
      .flatMap { validated =>
        validated.first.map { first =>
          LineFacts(
            validated.line,
            first,
            validated.reply,
            validated.continuation,
            validated.moves.drop(3).take(3)
          )
        }
      }
      .headOption

  def validatedLine(
      startFen: String,
      line: LineVariationRef,
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

  def validatedLineFromStart(
      startFen: String,
      line: LineVariationRef
  ): Option[ValidatedLine] =
    val moves = line.moves
    Option.when(
      moves.nonEmpty &&
        moves.forall(_.fenAfter.trim.nonEmpty) &&
        strictlyOrdered(moves)
    )(moves)
      .flatMap(replay(startFen, _))
      .map(validatedMoves => ValidatedLine(line, validatedMoves))

  def legalFenAfter(fen: String, uciMove: String): Option[String] =
    val normalized = normalizeUci(uciMove)
    Option.when(normalized.matches("(?i)^[a-h][1-8][a-h][1-8][qrbn]?$"))(normalized).flatMap { moveStr =>
      Fen.read(Standard, Fen.Full(fen)).flatMap { position =>
        Uci(moveStr)
          .collect { case move: Uci.Move => move }
          .flatMap(position.move(_).toOption)
          .map(result => Fen.write(result.after).value)
          .filter(after => boardStateFen(after) != boardStateFen(fen))
      }
    }

  def normalizeUci(uci: String): String =
    Option(uci).getOrElse("").trim.toLowerCase

  private def replay(startFen: String, moves: List[LineMoveRef]): Option[List[LineMoveRef]] =
    val accepted = scala.collection.mutable.ListBuffer.empty[LineMoveRef]
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

  private def strictlyOrdered(moves: List[LineMoveRef]): Boolean =
    moves.sliding(2).forall {
      case List(left, right) => left.ply < right.ply
      case _                 => true
    }

  private def normalizeFen(fen: String): String =
    Option(fen).getOrElse("").trim.split("\\s+").filter(_.nonEmpty).mkString(" ")

  private def boardStateFen(fen: String): String =
    normalizeFen(fen).split("\\s+").take(4).mkString(" ")
