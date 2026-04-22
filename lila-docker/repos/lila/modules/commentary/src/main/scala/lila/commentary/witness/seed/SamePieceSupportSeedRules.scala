package lila.commentary.witness.seed

import chess.{ Bishop, Bitboard, Board, Color, King, Knight, Position, Queen, Role, Rook, Square }
import chess.variant

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors }
import lila.commentary.witness.*
import lila.commentary.witness.seed.StrategySupportSeedHelpers.*
import lila.commentary.witness.u.UExtractionContext

private[seed] final case class SamePieceLiability(
    color: Color,
    role: Role,
    square: Square,
    liabilityTags: Vector[String],
    rootIndices: Vector[Int]
)

private[seed] object SamePieceLiability:

  def all(context: UExtractionContext): Vector[SamePieceLiability] =
    canonicalColors.flatMap: ownerColor =>
      Vector(Knight, Bishop).flatMap: role =>
        context.activePieceSquares(ownerColor, role).flatMap: square =>
          val tagRoots =
            Vector(
              liabilityTagRoot(context, ownerColor, square, SchemaId.LoosePiece, "loose_piece"),
              liabilityTagRoot(context, ownerColor, square, SchemaId.PinnedPiece, "pinned_piece"),
              liabilityTagRoot(context, ownerColor, square, SchemaId.TrappedPiece, "trapped_piece")
            ).flatten

          Option.when(tagRoots.nonEmpty):
            SamePieceLiability(
              color = ownerColor,
              role = role,
              square = square,
              liabilityTags = tagRoots.map(_._1).sortBy(identity),
              rootIndices = Vector(context.pieceOnRootIndex(ownerColor, role, square)).flatten ++
                tagRoots.map(_._2)
            )

  private def liabilityTagRoot(
      context: UExtractionContext,
      ownerColor: Color,
      square: Square,
      schemaId: String,
      tag: String
  ): Option[(String, Int)] =
    context.colorSquareRootIndex(schemaId, ownerColor, square).map(tag -> _)

  def tagsOnBoard(
      board: Board,
      ownerColor: Color,
      role: Role,
      square: Square
  ): Vector[String] =
    board.pieceAt(square) match
      case Some(piece) if piece.color == ownerColor && piece.role == role =>
        Vector(
          Option.when(isLoosePiece(board, ownerColor, square))("loose_piece"),
          Option.when(isPinnedPiece(board, ownerColor, square, piece.role))("pinned_piece"),
          Option.when(isTrappedPiece(board, ownerColor, square, piece.role))("trapped_piece")
        ).flatten.sorted
      case _ => Vector.empty

  private def isLoosePiece(board: Board, ownerColor: Color, square: Square): Boolean =
    board.pieceAt(square).exists: piece =>
      piece.color == ownerColor && piece.role != King &&
        opponentBestExchangeNet(board, square, !ownerColor) > 0

  private def isPinnedPiece(
      board: Board,
      ownerColor: Color,
      square: Square,
      role: Role
  ): Boolean =
    role != King &&
      (board.kingPosOf(ownerColor).exists(king => board.sliderBlockers(king, ownerColor).contains(square)) ||
        isRelativelyPinned(board, ownerColor, square, role))

  private def isRelativelyPinned(
      board: Board,
      ownerColor: Color,
      square: Square,
      role: Role
  ): Boolean =
    sliderSquaresOf(board, !ownerColor).exists: sniperSquare =>
      board.pieceAt(sniperSquare).exists: sniper =>
        sliderCanUseLine(sniper.role, sniperSquare, square) &&
          moreValuableFriendlyAnchors(board, ownerColor, square, role).exists: anchor =>
            sliderCanUseLine(sniper.role, sniperSquare, anchor) &&
              hasSingleBlockerBetween(board, anchor, sniperSquare, square)

  private def isTrappedPiece(
      board: Board,
      ownerColor: Color,
      square: Square,
      role: Role
  ): Boolean =
    role != King &&
      board.attackers(square, !ownerColor).nonEmpty &&
      Position(board, variant.Standard, ownerColor).generateMovesAt(square).forall: move =>
        opponentBestExchangeNet(move.after.board, move.dest, !ownerColor) > 0

  private def opponentBestExchangeNet(boardState: Board, square: Square, attacker: Color): Int =
    boardState.pieceAt(square).fold(0): occupant =>
      legalAttackers(boardState, square, attacker)
        .map: origin =>
          val afterCapture = boardState.taking(origin, square).get
          pieceValue(occupant.role) - opponentBestExchangeNet(afterCapture, square, !attacker)
        .foldLeft(0)(math.max)

  private def legalAttackers(boardState: Board, square: Square, attacker: Color): Vector[Square] =
    val attackerPosition = Position(boardState, variant.Standard, attacker)
    boardState
      .attackers(square, attacker)
      .filter(origin => attackerPosition.generateMovesAt(origin).exists(_.dest == square))
      .toVector

  private def sliderSquaresOf(board: Board, color: Color): Vector[Square] =
    board.pieceMap.collect:
      case (square, piece) if piece.color == color && Set(Bishop, Rook, Queen).contains(piece.role) => square
    .toVector

  private def sliderCanUseLine(role: Role, from: Square, to: Square): Boolean =
    role match
      case Bishop => from.onSameDiagonal(to)
      case Rook => from.onSameLine(to)
      case Queen => from.onSameDiagonal(to) || from.onSameLine(to)
      case _ => false

  private def moreValuableFriendlyAnchors(
      board: Board,
      ownerColor: Color,
      square: Square,
      role: Role
  ): Vector[Square] =
    board.pieceMap.collect:
      case (anchor, anchorPiece)
          if anchor != square &&
            anchorPiece.color == ownerColor &&
            anchorPiece.role != King &&
            pieceValue(anchorPiece.role) > pieceValue(role) =>
        anchor
    .toVector

  private def hasSingleBlockerBetween(
      board: Board,
      anchor: Square,
      sniper: Square,
      blocker: Square
  ): Boolean =
    val blockers = Bitboard.between(anchor, sniper) & board.occupied
    blockers.count == 1 && blockers.contains(blocker)

private[seed] object SamePieceLiabilityAnchorSeedRule extends StrategySupportSeedRule:

  val seedId: StrategySupportSeedId =
    StrategySupportSeedScopeContract.S17SamePieceLiabilityAnchor

  def extract(context: UExtractionContext): Vector[StrategySupportSeed] =
    SamePieceLiability.all(context).map: liability =>
      owner(
        color = liability.color,
        anchor = WitnessAnchor.PieceSquareAnchor(liability.square),
        payload = WitnessPayload(
          "liability_anchor_square" -> WitnessValue.SquareValue(liability.square),
          "piece_role" -> WitnessValue.RoleValue(liability.role),
          "liability_tags" -> WitnessValue.TokenListValue(liability.liabilityTags)
        ),
        support = rootSupport(
          indices = liability.rootIndices,
          targetSquares = Vector(liability.square)
        )
      )

private[seed] object SamePieceRepairRouteSeedRule extends StrategySupportSeedRule:

  val seedId: StrategySupportSeedId =
    StrategySupportSeedScopeContract.S17SamePieceRepairRoute

  def extract(context: UExtractionContext): Vector[StrategySupportSeed] =
    SamePieceLiability.all(context).flatMap: liability =>
      val reliefSquares = legalQuietReliefSquares(context, liability)

      Option.when(reliefSquares.nonEmpty):
        owner(
          color = liability.color,
          anchor = WitnessAnchor.PieceSquareAnchor(liability.square),
          payload = WitnessPayload(
            "liability_anchor_square" -> WitnessValue.SquareValue(liability.square),
            "piece_role" -> WitnessValue.RoleValue(liability.role),
            "relief_squares" -> WitnessValue.SquareListValue(reliefSquares)
          ),
          support = rootSupport(
            indices = liability.rootIndices,
            targetSquares = liability.square +: reliefSquares,
            supportingTags = Vector("same_piece_quiet_repair")
          )
        )

  private def legalQuietReliefSquares(
      context: UExtractionContext,
      liability: SamePieceLiability
  ): Vector[Square] =
    val position = Position(context.board.toBoard, variant.Standard, liability.color)

    position
      .generateMovesAt(liability.square)
      .collect:
        case move
            if move.piece.role == liability.role &&
              move.orig == liability.square &&
              move.promotion.isEmpty &&
              !move.captures &&
              move.after.board.pieceAt(move.dest).exists(piece =>
                piece.color == liability.color && piece.role == liability.role
              ) &&
              SamePieceLiability
                .tagsOnBoard(move.after.board, liability.color, liability.role, move.dest)
                .isEmpty =>
          move.dest
      .toVector
      .distinct
      .sortBy(_.value)

private[seed] object SamePieceExchangeReliefSeedRule extends StrategySupportSeedRule:

  val seedId: StrategySupportSeedId =
    StrategySupportSeedScopeContract.S17SamePieceExchangeRelief

  def extract(context: UExtractionContext): Vector[StrategySupportSeed] =
    SamePieceLiability.all(context).flatMap: liability =>
      val contacts = legalExchangeContacts(context, liability)

      Option.when(contacts.nonEmpty):
        owner(
          color = liability.color,
          anchor = WitnessAnchor.PieceSquareAnchor(liability.square),
          payload = WitnessPayload(
            "liability_anchor_square" -> WitnessValue.SquareValue(liability.square),
            "piece_role" -> WitnessValue.RoleValue(liability.role),
            "exchange_target_squares" -> WitnessValue.SquareListValue(contacts.map(_.targetSquare)),
            "exchange_target_roles" -> WitnessValue.RoleListValue(contacts.map(_.targetRole))
          ),
          support = rootSupport(
            indices = liability.rootIndices ++ contacts.flatMap(_.rootIndices),
            targetSquares = liability.square +: contacts.map(_.targetSquare),
            supportingTags = Vector("same_piece_minor_exchange_contact")
          )
        )

  private final case class ExchangeContact(
      targetSquare: Square,
      targetRole: Role,
      rootIndices: Vector[Int]
  )

  private def legalExchangeContacts(
      context: UExtractionContext,
      liability: SamePieceLiability
  ): Vector[ExchangeContact] =
    val position = Position(context.board.toBoard, variant.Standard, liability.color)

    position
      .generateMovesAt(liability.square)
      .flatMap: move =>
        context.pieceAt(move.dest).flatMap: targetPiece =>
          Option.when(
            move.piece.role == liability.role &&
              move.orig == liability.square &&
              move.promotion.isEmpty &&
              move.captures &&
              targetPiece.color == !liability.color &&
              targetPiece.role == liability.role &&
              hasLegalRecapture(move.after.board, move.dest, !liability.color)
          ):
            ExchangeContact(
              targetSquare = move.dest,
              targetRole = targetPiece.role,
              rootIndices = Vector(
                context.pieceOnRootIndex(targetPiece.color, targetPiece.role, move.dest)
              ).flatten
            )
      .distinct
      .sortBy(_.targetSquare.value)
      .toVector

  private def hasLegalRecapture(
      boardAfterCapture: Board,
      occupiedSquare: Square,
      recapturingColor: Color
  ): Boolean =
    val recapturingPosition = Position(boardAfterCapture, variant.Standard, recapturingColor)
    boardAfterCapture
      .attackers(occupiedSquare, recapturingColor)
      .exists(origin => recapturingPosition.generateMovesAt(origin).exists(_.dest == occupiedSquare))
