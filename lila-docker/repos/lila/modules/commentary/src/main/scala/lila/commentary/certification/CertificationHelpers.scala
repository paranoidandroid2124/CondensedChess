package lila.commentary.certification

import chess.{ Bishop, Color, Pawn, Piece, Position, Queen, Rook, Square }

import lila.commentary.certification.CertificationVerdict
import lila.commentary.strategic.StrategicObject
import lila.commentary.strategic.StrategicObjectHelpers.*
import lila.commentary.witness.{ WitnessAnchor, WitnessValue }
import lila.commentary.witness.WitnessSupport

private[certification] object CertificationHelpers:

  def support(
      indices: IterableOnce[Int] = Vector.empty,
      targetSquares: IterableOnce[Square] = Vector.empty,
      tags: IterableOnce[String] = Vector.empty
  ): WitnessSupport =
    val withRoots =
      indices.iterator.foldLeft(WitnessSupport.empty)((acc, index) => acc.addRootIndex(index))
    val withTargets =
      targetSquares.iterator.foldLeft(withRoots)((acc, square) => acc.addTargetSquare(square))
    tags.iterator.foldLeft(withTargets)((acc, tag) => acc.addTag(tag))

  def allRequiredEvidenceSatisfied(
      evidence: CertificationEvidenceClaim,
      requiredPurposes: IterableOnce[CertificationEvidencePurpose]
  ): Boolean =
    requiredPurposes.iterator.forall: purpose =>
      evidence.strengthFor(purpose).contains(CertificationEvidenceStrength.Satisfied)

  def developmentCount(context: CertificationContext, color: Color): Int =
    (context.current.activePieceSquares(color, Bishop) ++
      context.current.activePieceSquares(color, chess.Knight))
      .count(_.rank != homeRank(color))

  def majorPieceCount(context: CertificationContext, color: Color): Int =
    context.current.activePieceSquares(color, Rook).size +
      context.current.activePieceSquares(color, Queen).size

  def attacksKingTheaterCount(
      context: CertificationContext,
      attacker: Color,
      defender: Color
  ): Int =
    val mask = kingRingAndShelterMask(context.current, defender)
    mask.count(square => context.current.board.attackersOf(square, attacker).nonEmpty)

  def attackedKingRingSquares(
      context: CertificationContext,
      attacker: Color,
      defender: Color
  ): Vector[Square] =
    context.current
      .kingRingSquaresFor(defender)
      .filter(square => context.current.board.attackersOf(square, attacker).nonEmpty)
      .distinct
      .sortBy(_.value)

  def attackedShelterSquares(
      context: CertificationContext,
      attacker: Color,
      defender: Color
  ): Vector[Square] =
    homeShelterMask(context.current, defender).toVector
      .filter(square => context.current.board.attackersOf(square, attacker).nonEmpty)
      .distinct
      .sortBy(_.value)

  def boardObject(
      context: CertificationContext,
      familyId: String,
      color: Option[Color] = None
  ): Option[StrategicObject] =
    context.currentExtraction.objects
      .forFamilyId(familyId)
      .find(obj =>
        obj.anchor == WitnessAnchor.BoardAnchor &&
          color.forall(obj.color.contains)
      )

  def squareObject(
      context: CertificationContext,
      familyId: String,
      color: Color,
      square: Square
  ): Option[StrategicObject] =
    context.currentExtraction.objects
      .forFamilyId(familyId)
      .find(obj =>
        obj.anchor == WitnessAnchor.SquareAnchor(square) &&
          obj.color.contains(color)
      )

  def supportiveVerdict(
      certifications: CertificationSet,
      familyId: String,
      color: Color
  ): Boolean =
    certifications
      .verdictOf(CertificationId(familyId), color)
      .exists(_ != CertificationVerdict.Rejected)

  def advancedPawnSquares(
      context: CertificationContext,
      color: Color
  ): Vector[Square] =
    context.current.activePieceSquares(color, Pawn).filter: square =>
      if color.white then square.rank.value >= chess.Rank.Fourth.value
      else square.rank.value <= chess.Rank.Fifth.value

  def kingDistance(left: Square, right: Square): Int =
    math.max(
      math.abs(left.file.value - right.file.value),
      math.abs(left.rank.value - right.rank.value)
    )

  def tokenList(payload: lila.commentary.witness.WitnessPayload, field: String): Vector[String] =
    payload.get(field).collect { case WitnessValue.TokenListValue(values) => values }.getOrElse(Vector.empty)

  def squareList(payload: lila.commentary.witness.WitnessPayload, field: String): Vector[Square] =
    payload.get(field).collect { case WitnessValue.SquareListValue(values) => values }.getOrElse(Vector.empty)

  def captureOptions(
      context: CertificationContext,
      color: Color
  ): Vector[(chess.Move, Piece)] =
    Option.when(context.sideToMove == color)(context.legalMoves(color)).getOrElse(Vector.empty).flatMap: move =>
      context.current.pieceAt(move.dest).collect:
        case target if target.color == !color && target.role != chess.King =>
          move -> target

  def legalMoves(position: Position): Vector[chess.Move] =
    chess.Square.all
      .filter(square => position.pieceAt(square).exists(_.color == position.color))
      .flatMap(position.generateMovesAt)
      .sortBy(move => (move.orig.value, move.dest.value))
      .toVector
