package lila.commentary.witness.seed

import chess.{ Color, King, Pawn, Rank, Square }

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors, canonicalSquares }
import lila.commentary.witness.*
import lila.commentary.witness.seed.StrategySupportSeedHelpers.*
import lila.commentary.witness.u.UExtractionContext

private[seed] object KingEntrySquareSeedRule extends StrategySupportSeedRule:

  val seedId: StrategySupportSeedId =
    StrategySupportSeedScopeContract.S23KingEntrySquare

  def extract(context: UExtractionContext): Vector[StrategySupportSeed] =
    canonicalColors.flatMap: beneficiaryColor =>
      entrySquares(context, beneficiaryColor).map: entry =>
        val adjacentEnemyPawns = enemyPawnsAdjacentTo(context, beneficiaryColor, entry)
        owner(
          color = beneficiaryColor,
          anchor = WitnessAnchor.SquareAnchor(entry),
          payload = WitnessPayload(
            "entry_square" -> WitnessValue.SquareValue(entry),
            "adjacent_enemy_pawns" -> WitnessValue.SquareListValue(adjacentEnemyPawns)
          ),
          support = rootSupport(
            indices = adjacentEnemyPawns.flatMap(square =>
              context.pieceOnRootIndex(!beneficiaryColor, Pawn, square)
            ),
            targetSquares = entry +: adjacentEnemyPawns,
            supportingTags = Vector("king_pawn_entry")
          )
        )

private[seed] object KingAccessRouteSeedRule extends StrategySupportSeedRule:

  val seedId: StrategySupportSeedId =
    StrategySupportSeedScopeContract.S23KingAccessRoute

  def extract(context: UExtractionContext): Vector[StrategySupportSeed] =
    canonicalColors.flatMap: beneficiaryColor =>
      context.board.kingSquare(beneficiaryColor).toVector.flatMap: kingSquare =>
        entrySquares(context, beneficiaryColor).flatMap: entry =>
          kingRoute(context, beneficiaryColor, kingSquare, entry).map: route =>
            owner(
              color = beneficiaryColor,
              anchor = WitnessAnchor.PieceSquareAnchor(kingSquare),
              payload = WitnessPayload(
                "king_square" -> WitnessValue.SquareValue(kingSquare),
                "entry_square" -> WitnessValue.SquareValue(entry),
                "route_squares" -> WitnessValue.SquareListValue(route)
              ),
              support = rootSupport(
                indices = Vector(context.pieceOnRootIndex(beneficiaryColor, King, kingSquare)).flatten,
                targetSquares = route,
                supportingTags = Vector("king_entry_route")
              ),
              variant = Some(WitnessVariantId(s"to_${entry.key}"))
            )

private[seed] object KingOppositionContactSeedRule extends StrategySupportSeedRule:

  val seedId: StrategySupportSeedId =
    StrategySupportSeedScopeContract.S23KingOppositionContact

  def extract(context: UExtractionContext): Vector[StrategySupportSeed] =
    for
      sideToMove <- context.sideToMove.toVector
      beneficiaryColor = !sideToMove
      beneficiaryKing <- context.board.kingSquare(beneficiaryColor).toVector
      rivalKing <- context.board.kingSquare(sideToMove).toVector
      contactSquare <- directOppositionContact(context, beneficiaryKing, rivalKing).toVector
    yield
      owner(
        color = beneficiaryColor,
        anchor = WitnessAnchor.SquareAnchor(contactSquare),
        payload = WitnessPayload(
          "beneficiary_king_square" -> WitnessValue.SquareValue(beneficiaryKing),
          "rival_king_square" -> WitnessValue.SquareValue(rivalKing),
          "contact_square" -> WitnessValue.SquareValue(contactSquare),
          "relation" -> WitnessValue.Token("direct_opposition")
        ),
        support = rootSupport(
          indices = Vector(
            context.pieceOnRootIndex(beneficiaryColor, King, beneficiaryKing),
            context.pieceOnRootIndex(sideToMove, King, rivalKing)
          ).flatten,
          targetSquares = Vector(beneficiaryKing, contactSquare, rivalKing),
          supportingTags = Vector("direct_king_opposition")
        )
      )

private[seed] def entrySquares(
    context: UExtractionContext,
    beneficiaryColor: Color
): Vector[Square] =
  canonicalSquares.filter: square =>
    context.pieceAt(square).isEmpty &&
      isEnemyTerritory(beneficiaryColor, square) &&
      !context.hasColorSquare(SchemaId.ControlledBy, !beneficiaryColor, square) &&
      !adjacentToEnemyKing(context, beneficiaryColor, square) &&
      enemyPawnsAdjacentTo(context, beneficiaryColor, square).nonEmpty

private def isEnemyTerritory(color: Color, square: Square): Boolean =
  color.fold(square.rank >= Rank.Fifth, square.rank <= Rank.Fourth)

private def enemyPawnsAdjacentTo(
    context: UExtractionContext,
    beneficiaryColor: Color,
    square: Square
): Vector[Square] =
  square.kingAttacks.squares.toVector
    .filter(target => context.hasPieceOn(!beneficiaryColor, Pawn, target))
    .sortBy(_.value)

private def adjacentToEnemyKing(
    context: UExtractionContext,
    beneficiaryColor: Color,
    square: Square
): Boolean =
  context.board.kingSquare(!beneficiaryColor).exists(enemyKing =>
    square == enemyKing || square.kingAttacks.contains(enemyKing)
  )

private def kingRoute(
    context: UExtractionContext,
    beneficiaryColor: Color,
    kingSquare: Square,
    entry: Square
): Option[Vector[Square]] =
  val maxSteps = 4
  var queue = Vector(Vector(kingSquare))
  var seen = Set(kingSquare)

  while queue.nonEmpty do
    val path = queue.head
    queue = queue.tail
    val current = path.last
    if current == entry then return Some(path)
    if path.size <= maxSteps then
      val nextSquares =
        current.kingAttacks.squares.toVector
          .sortBy(_.value)
          .filterNot(seen)
          .filter(square => kingCanOccupy(context, beneficiaryColor, kingSquare, square))
      nextSquares.foreach: square =>
        seen += square
        queue :+= (path :+ square)

  None

private def kingCanOccupy(
    context: UExtractionContext,
    beneficiaryColor: Color,
    kingSquare: Square,
    square: Square
): Boolean =
  (square == kingSquare || context.pieceAt(square).isEmpty) &&
    !context.hasColorSquare(SchemaId.ControlledBy, !beneficiaryColor, square) &&
    !adjacentToEnemyKing(context, beneficiaryColor, square)

private def directOppositionContact(
    context: UExtractionContext,
    beneficiaryKing: Square,
    rivalKing: Square
): Option[Square] =
  val sameFileDistance =
    beneficiaryKing.file == rivalKing.file &&
      math.abs(beneficiaryKing.rank.value - rivalKing.rank.value) == 2
  val sameRankDistance =
    beneficiaryKing.rank == rivalKing.rank &&
      math.abs(beneficiaryKing.file.value - rivalKing.file.value) == 2

  Option.when(sameFileDistance || sameRankDistance):
    val file = (beneficiaryKing.file.value + rivalKing.file.value) / 2
    val rank = (beneficiaryKing.rank.value + rivalKing.rank.value) / 2
    Square.at(file, rank).filter(square => context.pieceAt(square).isEmpty)
  .flatten
