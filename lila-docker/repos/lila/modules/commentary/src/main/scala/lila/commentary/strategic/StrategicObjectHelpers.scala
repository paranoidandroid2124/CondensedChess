package lila.commentary.strategic

import scala.collection.mutable

import chess.{ Bishop, Color, File, Knight, Pawn, Queen, Rank, Rook, Square }

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors, canonicalSquares }
import lila.commentary.witness.{ Witness, WitnessAnchor, WitnessDirection, WitnessPayload, WitnessSector, WitnessSupport, WitnessValue }

private[commentary] final case class ContactComponent(
    squares: Vector[Square],
    contestedSquares: Vector[Square],
    occupiedContactSquares: Vector[Square],
    contributingColors: Set[Color],
    sectors: Set[WitnessSector]
):
  def liesOutsideCenterSector: Boolean = sectors.exists(_ != WitnessSector.Center)

private[commentary] final case class EntryAxis(
    hostId: String,
    sourcePieceSquares: Vector[Square],
    feederEntryPairs: Vector[(Square, Square)]
):
  lazy val feederSquares: Vector[Square] = feederEntryPairs.map(_._1).distinct.sortBy(_.value)
  lazy val entrySquares: Vector[Square] = feederEntryPairs.map(_._2).distinct.sortBy(_.value)
  lazy val targetSquares: Vector[Square] = (feederSquares ++ entrySquares).distinct.sortBy(_.value)

private[commentary] final case class EndgameRaceSnapshot(
    whiteAdvancedRunSquares: Vector[Square],
    blackAdvancedRunSquares: Vector[Square],
    whiteClearRunSquares: Vector[Square],
    blackClearRunSquares: Vector[Square]
)

private[commentary] object StrategicObjectHelpers:

  private val whiteHomeMinorSquares =
    Vector(Square(File.B, Rank.First), Square(File.G, Rank.First), Square(File.C, Rank.First), Square(File.F, Rank.First))
  private val blackHomeMinorSquares =
    Vector(Square(File.B, Rank.Eighth), Square(File.G, Rank.Eighth), Square(File.C, Rank.Eighth), Square(File.F, Rank.Eighth))
  private val whiteHomeRookSquares = Vector(Square(File.A, Rank.First), Square(File.H, Rank.First))
  private val blackHomeRookSquares = Vector(Square(File.A, Rank.Eighth), Square(File.H, Rank.Eighth))
  private val homeWingKingFiles = Set(File.C, File.G)

  def sectorOf(file: File): WitnessSector =
    file.value match
      case 0 | 1 | 2 => WitnessSector.Queenside
      case 3 | 4 => WitnessSector.Center
      case _ => WitnessSector.Kingside

  def sectorMask(sector: WitnessSector, square: Square): Boolean =
    sectorOf(square.file) == sector

  def centralSectorMask(square: Square): Boolean =
    Set(File.C, File.D, File.E, File.F).contains(square.file) &&
      square.rank.value >= Rank.Third.value &&
      square.rank.value <= Rank.Sixth.value

  def occupied(context: StrategicObjectContext, square: Square): Boolean =
    context.pieceAt(square).nonEmpty

  def contactSquare(context: StrategicObjectContext, square: Square): Boolean =
    context.hasNeutralSquare(SchemaId.Contested, square) ||
      context.pieceAt(square).exists(piece =>
        context.hasColorSquare(SchemaId.ControlledBy, !piece.color, square)
      )

  def contactComponents(
      context: StrategicObjectContext,
      mask: Square => Boolean
  ): Vector[ContactComponent] =
    val candidates = canonicalSquares.filter(square => mask(square) && contactSquare(context, square))
    val remaining = mutable.Set.from(candidates)
    val components = Vector.newBuilder[ContactComponent]

    while remaining.nonEmpty do
      val start = remaining.head
      val queue = mutable.Queue(start)
      val members = mutable.Set.empty[Square]

      while queue.nonEmpty do
        val square = queue.dequeue()
        if remaining.contains(square) then
          remaining -= square
          members += square
          orthogonalNeighbors(square).filter(next => mask(next) && remaining.contains(next)).foreach(queue.enqueue(_))

      val squares = members.toVector.sortBy(_.value)
      val contestedSquares = squares.filter(context.hasNeutralSquare(SchemaId.Contested, _))
      val occupiedContactSquares = squares.filter(square =>
        context.pieceAt(square).exists(piece => context.hasColorSquare(SchemaId.ControlledBy, !piece.color, square))
      )
      val contributingColors =
        canonicalColors.filter: color =>
          squares.exists(square =>
            context.pieceAt(square).exists(_.color == color) ||
              context.hasColorSquare(SchemaId.ControlledBy, color, square)
          )
        .toSet
      val sectors = squares.map(square => sectorOf(square.file)).toSet

      components += ContactComponent(
        squares = squares,
        contestedSquares = contestedSquares,
        occupiedContactSquares = occupiedContactSquares,
        contributingColors = contributingColors,
        sectors = sectors
      )

    components.result().sortBy(component => (component.squares.head.value, component.squares.size))

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

  def retainedHomeMinors(context: StrategicObjectContext, color: Color): Vector[Square] =
    homeMinorSquares(color).filter(square =>
      context.pieceAt(square).exists(piece =>
        piece.color == color && Set(Bishop, Knight).contains(piece.role)
      )
    )

  def retainedHomeRooks(context: StrategicObjectContext, color: Color): Vector[Square] =
    homeRookSquares(color).filter(square =>
      context.pieceAt(square).exists(piece =>
        piece.color == color && piece.role == Rook
      )
    )

  def hasNonPawnDevelopmentOffHomeRank(context: StrategicObjectContext, color: Color): Boolean =
    Vector(Bishop, Knight, Rook, Queen).exists(role =>
      context.activePieceSquares(color, role).exists(square => square.rank != homeRank(color))
    )

  def homeRank(color: Color): Rank =
    if color.white then Rank.First else Rank.Eighth

  def noQueensRemain(context: StrategicObjectContext): Boolean =
    canonicalColors.forall(color => context.activePieceSquares(color, Queen).isEmpty)

  def openingDevelopmentWindow(context: StrategicObjectContext): Boolean =
    val whiteHomeMinors = retainedHomeMinors(context, Color.White)
    val blackHomeMinors = retainedHomeMinors(context, Color.Black)
    val reserveSideExists = whiteHomeMinors.size >= 2 || blackHomeMinors.size >= 2
    val whiteMajorReserve = retainedHomeRooks(context, Color.White).nonEmpty
    val blackMajorReserve = retainedHomeRooks(context, Color.Black).nonEmpty

    whiteHomeMinors.nonEmpty &&
      blackHomeMinors.nonEmpty &&
      reserveSideExists &&
      whiteMajorReserve &&
      blackMajorReserve &&
      !isOpenFile(context, File.D) &&
      !isOpenFile(context, File.E)

  def centralContactFrontComponent(
      context: StrategicObjectContext
  ): Option[ContactComponent] =
    contactComponents(context, centralSectorMask)
      .filter(isCentralContactFrontComponent)
      .sortBy(component =>
        (
          -component.squares.size,
          -component.contestedSquares.size,
          -component.occupiedContactSquares.size,
          component.squares.head.value
        )
      )
      .headOption

  def distributedContactRegimeComponents(
      context: StrategicObjectContext
  ): Vector[(WitnessSector, ContactComponent)] =
    val whiteDeveloped = hasNonPawnDevelopmentOffHomeRank(context, Color.White)
    val blackDeveloped = hasNonPawnDevelopmentOffHomeRank(context, Color.Black)
    val admittedComponents =
      Option.when(whiteDeveloped && blackDeveloped):
        Vector(WitnessSector.Queenside, WitnessSector.Center, WitnessSector.Kingside).flatMap: sector =>
          contactComponents(context, square => sectorMask(sector, square))
            .filter(isDistributedContactComponent)
            .map(component => sector -> component)
      .getOrElse(Vector.empty)

    val admittedSectors = admittedComponents.map(_._1).distinct
    val hasOutsideCenterBand = admittedComponents.exists(_._2.liesOutsideCenterSector)

    Option.when(admittedSectors.size >= 2 && hasOutsideCenterBand)(admittedComponents).getOrElse(Vector.empty)

  def endgameRaceScaffoldSnapshot(
      context: StrategicObjectContext
  ): Option[EndgameRaceSnapshot] =
    val whiteAdvancedRunSquares = context.activePieceSquares(Color.White, Pawn).filter: square =>
      square.rank >= Rank.Fifth
    val blackAdvancedRunSquares = context.activePieceSquares(Color.Black, Pawn).filter: square =>
      square.rank <= Rank.Fourth
    val whiteClearRunSquares =
      whiteAdvancedRunSquares.filter(square => forwardRunClear(context, Color.White, square))
    val blackClearRunSquares =
      blackAdvancedRunSquares.filter(square => forwardRunClear(context, Color.Black, square))

    Option.when(
      noQueensRemain(context) &&
        whiteAdvancedRunSquares.nonEmpty &&
        blackAdvancedRunSquares.nonEmpty &&
        whiteClearRunSquares.nonEmpty &&
        blackClearRunSquares.nonEmpty
    )(
      EndgameRaceSnapshot(
        whiteAdvancedRunSquares = whiteAdvancedRunSquares,
        blackAdvancedRunSquares = blackAdvancedRunSquares,
        whiteClearRunSquares = whiteClearRunSquares,
        blackClearRunSquares = blackClearRunSquares
      )
    )

  def isAdvancedRunResource(context: StrategicObjectContext, color: Color, square: Square): Boolean =
    val passedWitnessPresent =
      context.primaryWitnessesFor("passed_pawn_entity_state").exists(witness =>
        witness.color.contains(color) &&
          witness.anchor == WitnessAnchor.PieceSquareAnchor(square)
      )
    val candidatePasserPresent =
      context.hasColorPawnSquare(SchemaId.CandidatePasser, color, square)

    (passedWitnessPresent || candidatePasserPresent) &&
    (if color.white then square.rank >= Rank.Fifth else square.rank <= Rank.Fourth)

  def forwardRunClear(context: StrategicObjectContext, color: Color, square: Square): Boolean =
    context.forwardSquare(color, square).exists(next => context.pieceAt(next).isEmpty)

  def homeShelterMask(context: StrategicObjectContext, defender: Color): Set[Square] =
    context.board.kingSquare(defender).filter(_.rank == homeRank(defender)).toSet.flatMap: kingSquare =>
      kingCenteredMask(kingSquare, defender, includeHomeRank = false, stepsTowardCenter = Vector(1, 2))

  def homeWingKingSquare(
      context: StrategicObjectContext,
      defender: Color
  ): Option[Square] =
    context.board.kingSquare(defender).filter(square =>
      square.rank == homeRank(defender) && homeWingKingFiles.contains(square.file)
    )

  def fortressShellMask(context: StrategicObjectContext, holder: Color): Set[Square] =
    context.board.kingSquare(holder).filter(_.rank == homeRank(holder)).toSet.flatMap: kingSquare =>
      kingCenteredMask(kingSquare, holder, includeHomeRank = true, stepsTowardCenter = Vector(1, 2))

  def kingRingAndShelterMask(context: StrategicObjectContext, defender: Color): Set[Square] =
    context.kingRingSquaresFor(defender).toSet ++ homeShelterMask(context, defender)

  def homeShelterHoles(context: StrategicObjectContext, defender: Color): Vector[Square] =
    homeShelterMask(context, defender).toVector
      .filter(square => context.hasColorSquare(SchemaId.KingShelterHole, !defender, square))
      .sortBy(_.value)

  def bestEdgeAdjacentHolePair(
      context: StrategicObjectContext,
      defender: Color
  ): Option[(Square, Square)] =
    for
      kingSquare <- context.board.kingSquare(defender).filter(_.rank == homeRank(defender))
      pair <- edgeAdjacentPairs(homeShelterHoles(context, defender)).sortBy { case (left, right) =>
        (
          manhattanDistance(kingSquare, left) + manhattanDistance(kingSquare, right),
          left.value,
          right.value
        )
      }.headOption
    yield pair

  def edgeAdjacentPairs(squares: Vector[Square]): Vector[(Square, Square)] =
    squares.combinations(2).collect:
      case Vector(left, right)
          if (left.file == right.file && math.abs(left.rank.value - right.rank.value) == 1) ||
            (left.rank == right.rank && math.abs(left.file.value - right.file.value) == 1) =>
        if left.value <= right.value then (left, right) else (right, left)
    .toVector
      .sortBy { case (left, right) => (left.value, right.value) }

  def fileEntryAxesIntoMask(
      context: StrategicObjectContext,
      attacker: Color,
      targetMask: Set[Square]
  ): Vector[EntryAxis] =
    context.primaryWitnessesFor("file_lane_state").flatMap: witness =>
      witness.anchor match
        case WitnessAnchor.FileAnchor(file) =>
          val sources =
            (context.activePieceSquares(attacker, Rook) ++ context.activePieceSquares(attacker, Queen))
              .filter(_.file == file)
              .distinct
              .sortBy(_.value)
          val pairs =
            sources.flatMap(source =>
              orderedEntryPairsOnFile(
                context,
                attacker,
                source,
                targetMask.filter(_.file == file).toVector.sortBy(_.value)
              )
            )
          Option.when(pairs.nonEmpty)(
            EntryAxis(
              hostId = "file_lane_state",
              sourcePieceSquares = sources,
              feederEntryPairs = pairs.distinct.sortBy(pair => (pair._2.value, pair._1.value))
            )
          )
        case _ => None

  def entryAxesIntoMask(
      context: StrategicObjectContext,
      attacker: Color,
      targetMask: Set[Square]
  ): Vector[EntryAxis] =
    (fileEntryAxesIntoMask(context, attacker, targetMask) ++
      diagonalEntryAxesIntoMask(context, attacker, targetMask))
      .sortBy(axis =>
        (
          axis.entrySquares.headOption.map(_.value).getOrElse(Int.MaxValue),
          axis.hostId,
          axis.sourcePieceSquares.headOption.map(_.value).getOrElse(Int.MaxValue)
        )
      )

  def fileLaneWitnessOnFile(
      context: StrategicObjectContext,
      file: File
  ): Option[Witness] =
    context.primaryWitnessesFor("file_lane_state").find(_.anchor == WitnessAnchor.FileAnchor(file))

  def rookOnOpenFileWitnesses(
      context: StrategicObjectContext,
      color: Color
  ): Vector[Witness] =
    context.primaryWitnessesFor("rook_on_open_file_state").filter(_.color.contains(color))

  def occupiedNonKingSquares(
      context: StrategicObjectContext,
      color: Color,
      mask: Set[Square]
  ): Vector[Square] =
    mask.toVector
      .filter(square =>
        context.pieceAt(square).exists(piece => piece.color == color && piece.role != chess.King)
      )
      .sortBy(_.value)

  def occupiedSquaresByColor(
      context: StrategicObjectContext,
      color: Color,
      mask: Set[Square]
  ): Vector[Square] =
    mask.toVector
      .filter(square => context.pieceAt(square).exists(_.color == color))
      .sortBy(_.value)

  def passedPawnWitnessSquares(
      context: StrategicObjectContext,
      color: Color
  ): Vector[Square] =
    context.primaryWitnessesFor("passed_pawn_entity_state").collect:
      case witness
          if witness.color.contains(color) &&
            witness.anchor.isInstanceOf[WitnessAnchor.PieceSquareAnchor] =>
        witness.anchor.asInstanceOf[WitnessAnchor.PieceSquareAnchor].square
    .distinct
      .sortBy(_.value)

  def adjacentFiles(file: File): Set[File] =
    Set(file.value - 1, file.value + 1).flatMap(File(_))

  def sameAndAdjacentFiles(file: File): Set[File] =
    adjacentFiles(file) + file

  def kingNeighborhoodFiles(
      context: StrategicObjectContext,
      color: Color
  ): Set[File] =
    context.board.kingSquare(color).map(square => sameAndAdjacentFiles(square.file)).getOrElse(Set.empty)

  def witnessSquares(payload: WitnessPayload, fields: String*): Vector[Square] =
    fields.toVector.flatMap(field =>
      payload.get(field) match
        case Some(WitnessValue.SquareValue(square)) => Vector(square)
        case Some(WitnessValue.SquareListValue(values)) => values
        case _ => Vector.empty
    ).distinct.sortBy(_.value)

  def diagonalEntryAxesIntoMask(
      context: StrategicObjectContext,
      attacker: Color,
      targetMask: Set[Square]
  ): Vector[EntryAxis] =
    context.primaryWitnessesFor("diagonal_lane_only").flatMap: witness =>
      witness.anchor match
        case WitnessAnchor.RayAnchor(ray) =>
          val laneSquares = diagonalLaneSquares(context, witness, ray)
          val sourceSquare =
            context.squareList(witness.payload, "source_piece_squares").find(square =>
              context.pieceAt(square).exists(_.color == attacker)
            )
          sourceSquare.flatMap { source =>
            val indexedSquares = laneSquares.zipWithIndex.toMap
            val pairs =
              laneSquares.flatMap { entry =>
                indexedSquares.get(entry).flatMap(index => Option.when(index > 0)(index)).flatMap { index =>
                  val feeder = laneSquares(index - 1)
                  Option.when(
                    targetMask.contains(entry) &&
                      !targetMask.contains(feeder) &&
                      context.hasColorSquare(SchemaId.ControlledBy, attacker, entry) &&
                      context.hasColorSquare(SchemaId.ControlledBy, attacker, feeder) &&
                      clearLine(context, source, entry) &&
                      clearLine(context, source, feeder)
                  )(feeder -> entry)
                }
              }
            Option.when(pairs.nonEmpty)(
              EntryAxis(
                hostId = "diagonal_lane_only",
                sourcePieceSquares = Vector(source),
                feederEntryPairs = pairs.distinct.sortBy(pair => (pair._2.value, pair._1.value))
              )
            )
          }
        case _ => None

  def rootSquares(
      context: StrategicObjectContext,
      schemaId: String,
      color: Color
  ): Vector[Square] =
    schemaId match
      case SchemaId.CandidatePasser | SchemaId.FixedPawn =>
        context.activeColorPawnSquares(schemaId, color)
      case _ =>
        context.activeColorSquares(schemaId, color)

  def rootIndicesForSquares(
      context: StrategicObjectContext,
      schemaId: String,
      color: Color,
      squares: IterableOnce[Square]
  ): Vector[Int] =
    squares.iterator.flatMap: square =>
      schemaId match
        case SchemaId.CandidatePasser | SchemaId.FixedPawn | SchemaId.PassedPawn =>
          context.colorPawnSquareRootIndex(schemaId, color, square)
        case _ =>
          context.colorSquareRootIndex(schemaId, color, square)
    .toVector
      .distinct
      .sorted

  def occupiedPieceRootIndices(
      context: StrategicObjectContext,
      squares: IterableOnce[Square]
  ): Vector[Int] =
    squares.iterator.flatMap(square =>
      context.pieceAt(square).flatMap(piece => context.pieceOnRootIndex(piece.color, piece.role, square))
    ).toVector
      .distinct
      .sorted

  private def homeMinorSquares(color: Color): Vector[Square] =
    if color.white then whiteHomeMinorSquares else blackHomeMinorSquares

  private def homeRookSquares(color: Color): Vector[Square] =
    if color.white then whiteHomeRookSquares else blackHomeRookSquares

  private def manhattanDistance(left: Square, right: Square): Int =
    math.abs(left.file.value - right.file.value) + math.abs(left.rank.value - right.rank.value)

  private def kingCenteredMask(
      kingSquare: Square,
      color: Color,
      includeHomeRank: Boolean,
      stepsTowardCenter: Vector[Int]
  ): Set[Square] =
    val homeRankSquares =
      Option.when(includeHomeRank)(
        Vector(-1, 0, 1).flatMap(fileOffset =>
          Square.at(kingSquare.file.value + fileOffset, kingSquare.rank.value)
        )
      ).getOrElse(Vector.empty)
    val forwardSquares =
      stepsTowardCenter.flatMap(step =>
        Vector(-1, 0, 1).flatMap(fileOffset =>
          Square.at(
            kingSquare.file.value + fileOffset,
            kingSquare.rank.value + (if color.white then step else -step)
          )
        )
      )

    (homeRankSquares ++ forwardSquares).toSet

  private def orthogonalNeighbors(square: Square): Vector[Square] =
    Vector(
      Square.at(square.file.value + 1, square.rank.value),
      Square.at(square.file.value - 1, square.rank.value),
      Square.at(square.file.value, square.rank.value + 1),
      Square.at(square.file.value, square.rank.value - 1)
    ).flatten

  private def orderedEntryPairsOnFile(
      context: StrategicObjectContext,
      attacker: Color,
      source: Square,
      targets: Vector[Square]
  ): Vector[(Square, Square)] =
    targets.flatMap: entry =>
      val direction = WitnessDirection.between(source, entry)
      direction.flatMap: step =>
        step.step(entry).filter(_ != source).filter(feeder =>
          feeder.file == source.file &&
            !targets.contains(feeder) &&
            context.hasColorSquare(SchemaId.ControlledBy, attacker, entry) &&
            context.hasColorSquare(SchemaId.ControlledBy, attacker, feeder) &&
            clearLine(context, source, entry) &&
            clearLine(context, source, feeder)
        ).map(feeder => feeder -> entry)

  private def diagonalLaneSquares(
      context: StrategicObjectContext,
      witness: Witness,
      ray: lila.commentary.witness.WitnessRay
  ): Vector[Square] =
    val sourceSquare = context.squareList(witness.payload, "source_piece_squares").headOption
    sourceSquare
      .map(source => ray.direction.raySquaresFrom(source))
      .getOrElse(witness.support.targetSquares)

  private def clearLine(
      context: StrategicObjectContext,
      source: Square,
      target: Square
  ): Boolean =
    source != target &&
      context.board.occupiedBetween(source, target).isEmpty

  private def isOpenFile(context: StrategicObjectContext, file: File): Boolean =
    context.primaryWitnessesFor("file_lane_state").exists: witness =>
      witness.anchor == WitnessAnchor.FileAnchor(file) &&
        witness.variant.exists(_.value == "open_file_state")

  private def isCentralContactFrontComponent(component: ContactComponent): Boolean =
    component.squares.size >= 2 &&
      component.contestedSquares.nonEmpty &&
      component.occupiedContactSquares.nonEmpty &&
      component.contributingColors.size == 2

  private def isDistributedContactComponent(component: ContactComponent): Boolean =
    component.squares.size >= 2 &&
      component.contestedSquares.nonEmpty &&
      component.occupiedContactSquares.nonEmpty &&
      component.contributingColors == Set(Color.White, Color.Black)
