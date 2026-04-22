package lila.commentary.witness.seed

import chess.{ Color, File, Position, Queen, Rank, Role, Rook, Square }
import chess.variant

import lila.commentary.root.RootAtomRegistry.canonicalColors
import lila.commentary.witness.*
import lila.commentary.witness.seed.StrategySupportSeedHelpers.*
import lila.commentary.witness.u.UExtractionContext

private[seed] object RankCorridorStateSeedRule extends StrategySupportSeedRule:

  val seedId: StrategySupportSeedId =
    StrategySupportSeedScopeContract.S25RankCorridorState

  def extract(context: UExtractionContext): Vector[StrategySupportSeed] =
    canonicalColors.flatMap: ownerColor =>
      val legalPosition = Position(context.board.toBoard, variant.Standard, ownerColor)
      majorPieceSources(context, ownerColor).flatMap: (source, role) =>
        horizontalDirections.flatMap: direction =>
          oppositeWingEntries(context, legalPosition, source, direction).map: entry =>
            val corridorSquares = squaresBetweenInclusive(source, entry, direction)
            val crossedCenterSquares = corridorSquares.filter(isCenterFile)
            owner(
              color = ownerColor,
              anchor = WitnessAnchor.PieceSquareAnchor(source),
              payload = WitnessPayload(
                "source_square" -> WitnessValue.SquareValue(source),
                "source_role" -> WitnessValue.RoleValue(role),
                "source_sector" -> WitnessValue.Token(wingSector(source).get),
                "entry_square" -> WitnessValue.SquareValue(entry),
                "entry_sector" -> WitnessValue.Token(wingSector(entry).get),
                "direction" -> WitnessValue.DirectionValue(direction),
                "corridor_squares" -> WitnessValue.SquareListValue(corridorSquares),
                "crossed_center_squares" -> WitnessValue.SquareListValue(crossedCenterSquares),
                "corridor_kind" -> WitnessValue.Token("cross_wing_rank_switch")
              ),
              support = rootSupport(
                indices = Vector(context.pieceOnRootIndex(ownerColor, role, source)).flatten,
                targetSquares = source +: corridorSquares,
                supportingTags = Vector("cross_wing_rank_switch")
              ),
              variant = Some(WitnessVariantId(s"${direction.key}_to_${entry.key}"))
            )

  private def majorPieceSources(context: UExtractionContext, ownerColor: Color): Vector[(Square, Role)] =
    (context.activePieceSquares(ownerColor, Rook).map(_ -> Rook) ++
      context.activePieceSquares(ownerColor, Queen).map(_ -> Queen))
      .filter { case (source, _) => activeSwitchRank(source) && wingSector(source).nonEmpty }
      .sortBy { case (source, role) => (source.value, role.toString) }

  private val horizontalDirections: Vector[WitnessDirection] =
    Vector(WitnessDirection.East, WitnessDirection.West)

  private def oppositeWingEntries(
      context: UExtractionContext,
      legalPosition: Position,
      source: Square,
      direction: WitnessDirection
  ): Vector[Square] =
    val sourceSector = wingSector(source)
    sourceSector.toVector.flatMap: sector =>
      context.board.clearRay(source, direction)
        .filter(entry =>
          wingSector(entry).exists(_ != sector) &&
            squaresBetweenInclusive(source, entry, direction).exists(isCenterFile) &&
            legalPosition.generateMovesAt(source).exists(_.dest == entry)
        )
        .take(1)

  private def squaresBetweenInclusive(
      source: Square,
      entry: Square,
      direction: WitnessDirection
  ): Vector[Square] =
    val builder = Vector.newBuilder[Square]
    var next = direction.step(source)
    while next.nonEmpty && next.get != entry do
      builder += next.get
      next = direction.step(next.get)
    next.foreach(builder += _)
    builder.result()

  private def activeSwitchRank(square: Square): Boolean =
    square.rank >= Rank.Third && square.rank <= Rank.Sixth

  private def wingSector(square: Square): Option[String] =
    square.file match
      case File.A | File.B | File.C => Some("queenside")
      case File.F | File.G | File.H => Some("kingside")
      case _ => None

  private def isCenterFile(square: Square): Boolean =
    square.file == File.D || square.file == File.E
