package lila.chessjudgment.analysis.structure

import _root_.chess.{ Bishop, Board, Color, King, Knight, Pawn, Queen, Role, Rook, Square }

import lila.chessjudgment.analysis.position.{ PositionAnalyzer, PositionFeatures }

private[chessjudgment] final case class StructuralDelta(
    openedFiles: List[String] = Nil,
    semiOpenedFiles: List[String] = Nil,
    newWeakPawns: List[String] = Nil,
    newWeakSquares: List[String] = Nil,
    createdTension: List[String] = Nil,
    resolvedTension: List[String] = Nil,
    pawnTensionBefore: Int = 0,
    pawnTensionAfter: Int = 0,
    targetPressureDelta: Int = 0,
    fileAccessDelta: Int = 0,
    kingShelterDelta: Int = 0,
    mobilityDelta: Int = 0,
    developmentDelta: Int = 0,
    centerControlDelta: Int = 0,
    lineUnlockDelta: Int = 0,
    developmentMoves: List[DevelopmentMoveDelta] = Nil,
    fileOccupation: List[String] = Nil,
    releasedTargetPressure: List[String] = Nil,
    createdTargetPressure: List[String] = Nil
):
  def pawnTensionDelta: Int = pawnTensionAfter - pawnTensionBefore
  def targetPressureRelease: Int = releasedTargetPressure.size
  def targetPressureGain: Int = createdTargetPressure.size
  def hasConsequence: Boolean =
    openedFiles.nonEmpty ||
      semiOpenedFiles.nonEmpty ||
      newWeakPawns.nonEmpty ||
      newWeakSquares.nonEmpty ||
      createdTension.nonEmpty ||
      resolvedTension.nonEmpty ||
      pawnTensionDelta > 0 ||
      targetPressureDelta > 0 ||
      fileAccessDelta > 0 ||
      kingShelterDelta != 0 ||
      mobilityDelta != 0 ||
      developmentDelta > 0 ||
      developmentMoves.nonEmpty ||
      centerControlDelta > 0 ||
      lineUnlockDelta > 0 ||
      fileOccupation.nonEmpty ||
      releasedTargetPressure.nonEmpty ||
      createdTargetPressure.nonEmpty

private[chessjudgment] final case class DevelopmentMoveDelta(
    role: String,
    from: String,
    to: String,
    fromBackRank: Boolean,
    toBackRank: Boolean,
    destinationCenterDistance: Int,
    mobilityDelta: Int,
    centerControlDelta: Int,
    defendedAfter: Boolean,
    enemyAttackersAfter: Int
)

private[chessjudgment] object StructuralDeltaAnalyzer:

  private final case class StructureSnapshot(
      features: Option[PositionFeatures],
      openFiles: Set[String],
      semiOpenFilesForSide: Set[String],
      weakPawnsForEnemy: Set[String],
      weakSquaresForEnemy: Set[String],
      pawnTensions: Set[String],
      targetPressure: Map[String, Int],
      fileAccess: Int
  )

  def delta(
      beforeFen: String,
      beforeBoard: Board,
      afterFen: String,
      afterBoard: Board,
      side: Color,
      files: List[Char],
      targets: List[String],
      createdTensionFrom: Option[String] = None,
      moveUci: Option[String] = None
  ): Option[StructuralDelta] =
    val normalizedFiles = files.distinct
    val normalizedTargets =
      if targets.nonEmpty then targets.distinct
      else dynamicTargets(beforeBoard, afterBoard, side)
    val before = structureSnapshot(beforeFen, beforeBoard, side, normalizedFiles, normalizedTargets)
    val after = structureSnapshot(afterFen, afterBoard, side, normalizedFiles, normalizedTargets)
    val beforeAttacks = normalizedTargets.filter(target => sidePawnAttacksTarget(beforeBoard, side, target))
    val afterAttacks = normalizedTargets.filter(target => sidePawnAttacksTarget(afterBoard, side, target))
    val newTargets = afterAttacks.diff(beforeAttacks).distinct
    val createdTension =
      createdTensionFrom.toList.flatMap { origin =>
        normalizedTargets
          .filter(target => pawnAttacks(origin, side).contains(target) && !beforeAttacks.contains(target))
          .map(target => s"$origin-$target")
      }.distinct
    val targetPressureDelta =
      normalizedTargets.map(target => after.targetPressure.getOrElse(target, 0) - before.targetPressure.getOrElse(target, 0)).sum
    val pieceTargetPressure = moveUci
      .map(uci => pieceTargetPressureDelta(beforeBoard, afterBoard, side, uci))
      .getOrElse(PieceTargetPressureDelta(Nil, Nil))
    val mobilityDeltaValue = mobilityDelta(before.features, after.features, side)
    val developmentDeltaValue = developmentDelta(before.features, after.features, side)
    val centerControlDeltaValue = centerControlDelta(before.features, after.features, side)
    Some(
      StructuralDelta(
        openedFiles = after.openFiles.diff(before.openFiles).toList.sorted,
        semiOpenedFiles = after.semiOpenFilesForSide.diff(before.semiOpenFilesForSide).toList.sorted,
        newWeakPawns = (after.weakPawnsForEnemy.diff(before.weakPawnsForEnemy) ++ newTargets).toList.sorted.distinct,
        newWeakSquares = (after.weakSquaresForEnemy.diff(before.weakSquaresForEnemy) ++ newTargets).toList.sorted.distinct,
        createdTension = createdTension,
        resolvedTension = before.pawnTensions.diff(after.pawnTensions).toList.sorted,
        pawnTensionBefore = beforeAttacks.size,
        pawnTensionAfter = afterAttacks.size,
        targetPressureDelta = targetPressureDelta,
        fileAccessDelta = after.fileAccess - before.fileAccess,
        kingShelterDelta = kingShelterDelta(before.features, after.features, side, normalizedFiles),
        mobilityDelta = mobilityDeltaValue,
        developmentDelta = developmentDeltaValue,
        centerControlDelta = centerControlDeltaValue,
        lineUnlockDelta = moveUci.map(uci => lineUnlockDelta(beforeBoard, afterBoard, side, uci)).getOrElse(0),
        developmentMoves = moveUci.flatMap(uci => developmentMoveDelta(beforeBoard, afterBoard, side, uci)).toList,
        fileOccupation = moveUci.map(uci => fileOccupationGain(beforeBoard, afterBoard, after, side, uci)).getOrElse(Nil),
        releasedTargetPressure = pieceTargetPressure.released,
        createdTargetPressure = pieceTargetPressure.created
      )
    )

  private def developmentMoveDelta(
      beforeBoard: Board,
      afterBoard: Board,
      side: Color,
      moveUci: String
  ): Option[DevelopmentMoveDelta] =
    val origin = squareAt(moveUci.take(2))
    val dest = squareAt(moveUci.drop(2).take(2))
    val movedPiece = origin.flatMap(beforeBoard.pieceAt)
    (origin, dest, movedPiece) match
      case (Some(from), Some(to), Some(piece))
          if piece.color == side && Set(Knight, Bishop).contains(piece.role) && from != to =>
        val beforeMobility = pieceMobility(beforeBoard, from, piece.role, side)
        val afterMobility = pieceMobility(afterBoard, to, piece.role, side)
        val beforeCenter = centerControlByPiece(beforeBoard, from, piece.role, side)
        val afterCenter = centerControlByPiece(afterBoard, to, piece.role, side)
        Some(
          DevelopmentMoveDelta(
            role = piece.role.name,
            from = from.key,
            to = to.key,
            fromBackRank = backRank(from, side),
            toBackRank = backRank(to, side),
            destinationCenterDistance = centerDistance(to),
            mobilityDelta = afterMobility - beforeMobility,
            centerControlDelta = afterCenter - beforeCenter,
            defendedAfter = afterBoard.attackers(to, side).nonEmpty,
            enemyAttackersAfter = afterBoard.attackers(to, !side).count
          )
        )
      case _ =>
        None

  private final case class PieceTargetPressureDelta(
      released: List[String],
      created: List[String]
  )

  private def pieceTargetPressureDelta(
      beforeBoard: Board,
      afterBoard: Board,
      side: Color,
      moveUci: String
  ): PieceTargetPressureDelta =
    val origin = squareAt(moveUci.take(2))
    val dest = squareAt(moveUci.drop(2).take(2))
    val movedPiece = origin.flatMap(beforeBoard.pieceAt)
    (origin, dest, movedPiece) match
      case (Some(from), Some(to), Some(piece)) if piece.color == side && piece.role != Pawn =>
        val beforeTargets = enemyOccupiedSquares(beforeBoard, side).filter(square =>
          beforeBoard.attackers(square, side).squares.contains(from)
        )
        val afterTargets = enemyOccupiedSquares(afterBoard, side).filter(square =>
          afterBoard.attackers(square, side).squares.contains(to)
        )
        val released = beforeTargets.filter(square =>
          afterBoard.attackers(square, side).count < beforeBoard.attackers(square, side).count
        )
        val created = afterTargets.filter(square =>
          beforeBoard.attackers(square, side).count < afterBoard.attackers(square, side).count
        )
        PieceTargetPressureDelta(
          released = released.map(_.key).distinct.sorted,
          created = created.map(_.key).distinct.sorted
        )
      case _ =>
        PieceTargetPressureDelta(Nil, Nil)

  private def fileOccupationGain(
      beforeBoard: Board,
      afterBoard: Board,
      after: StructureSnapshot,
      side: Color,
      moveUci: String
  ): List[String] =
    val origin = squareAt(moveUci.take(2))
    val dest = squareAt(moveUci.drop(2).take(2))
    val movedPiece = origin.flatMap(beforeBoard.pieceAt)
    (origin, dest, movedPiece) match
      case (Some(from), Some(to), Some(piece))
          if piece.color == side && Set(Rook, Queen).contains(piece.role) && from != to =>
        val file = to.key.take(1)
        val fileAccessible = after.openFiles.contains(file) || after.semiOpenFilesForSide.contains(file)
        val movedPieceArrived = afterBoard.pieceAt(to).exists(afterPiece => afterPiece.color == side && afterPiece.role == piece.role)
        val squareWasOccupied = beforeBoard.pieceAt(to).exists(_.color == side)
        if fileAccessible && movedPieceArrived && !squareWasOccupied then List(s"$file:${to.key}") else Nil
      case _ =>
        Nil

  private def lineUnlockDelta(
      beforeBoard: Board,
      afterBoard: Board,
      side: Color,
      moveUci: String
  ): Int =
    val origin = squareAt(moveUci.take(2))
    val movedPiece = origin.flatMap(beforeBoard.pieceAt)
    movedPiece match
      case Some(piece) if piece.color == side && piece.role == Pawn =>
        ownSlidingPieces(beforeBoard, side).map { case (square, role) =>
          val beforeMobility = slidingMobility(beforeBoard, square, role, side)
          val afterMobility =
            if afterBoard.pieceAt(square).exists(afterPiece => afterPiece.color == side && afterPiece.role == role)
            then slidingMobility(afterBoard, square, role, side)
            else 0
          (afterMobility - beforeMobility).max(0)
        }.sum
      case _ =>
        0

  private def ownSlidingPieces(board: Board, side: Color): List[(Square, Role)] =
    Square.all.toList.flatMap { square =>
      board.pieceAt(square).collect {
        case piece if piece.color == side && Set(Bishop, Rook, Queen).contains(piece.role) =>
          square -> piece.role
      }
    }

  private def slidingMobility(board: Board, square: Square, role: Role, side: Color): Int =
    val targets =
      role match
        case Bishop => square.bishopAttacks(board.occupied)
        case Rook   => square.rookAttacks(board.occupied)
        case Queen  => square.queenAttacks(board.occupied)
        case _      => _root_.chess.Bitboard.empty
    (targets & ~board.byColor(side)).count

  private def pieceMobility(board: Board, square: Square, role: Role, side: Color): Int =
    val targets =
      role match
        case Knight => square.knightAttacks
        case Bishop => square.bishopAttacks(board.occupied)
        case Rook   => square.rookAttacks(board.occupied)
        case Queen  => square.queenAttacks(board.occupied)
        case King   => square.kingAttacks
        case Pawn   => square.pawnAttacks(side)
    (targets & ~board.byColor(side)).count

  private def centerControlByPiece(board: Board, square: Square, role: Role, side: Color): Int =
    val targets =
      role match
        case Knight => square.knightAttacks
        case Bishop => square.bishopAttacks(board.occupied)
        case Rook   => square.rookAttacks(board.occupied)
        case Queen  => square.queenAttacks(board.occupied)
        case King   => square.kingAttacks
        case Pawn   => square.pawnAttacks(side)
    (targets & centerSquares).count

  private val centerSquares =
    Square.D4.bb | Square.E4.bb | Square.D5.bb | Square.E5.bb

  private def backRank(square: Square, side: Color): Boolean =
    if side.white then square.rank.value == 0 else square.rank.value == 7

  private def centerDistance(square: Square): Int =
    List(Square.D4, Square.E4, Square.D5, Square.E5)
      .map(center => (square.file.value - center.file.value).abs + (square.rank.value - center.rank.value).abs)
      .minOption
      .getOrElse(0)

  private def enemyOccupiedSquares(board: Board, side: Color): List[Square] =
    Square.all.toList.filter(square => board.pieceAt(square).exists(_.color != side))

  private def structureSnapshot(
      fen: String,
      board: Board,
      side: Color,
      files: List[Char],
      targets: List[String]
  ): StructureSnapshot =
    val features = if fen.trim.nonEmpty then PositionAnalyzer.extractFeatures(fen, 1) else None
    val openFiles = files.filter(file => pawnsOnFile(board, file).isEmpty).map(_.toString).toSet
    val semiOpenFiles =
      files
        .filter(file =>
          val pawns = pawnsOnFile(board, file)
          pawns.nonEmpty && pawns.forall(square => board.pieceAt(square).exists(_.color != side))
        )
        .map(_.toString)
        .toSet
    val weakPawns =
      WeaknessTargetProfile
        .targetsForPressure(board, side)
        .map(_.targetSquare)
        .filter(square => targets.contains(square) || square.headOption.exists(files.contains))
        .toSet
    val weakSquares =
      weakPawns.flatMap(square => adjacentSquares(square) :+ square).filter(square =>
        square.headOption.exists(files.contains)
      )
    StructureSnapshot(
      features = features,
      openFiles = openFiles,
      semiOpenFilesForSide = semiOpenFiles,
      weakPawnsForEnemy = weakPawns,
      weakSquaresForEnemy = weakSquares,
      pawnTensions = pawnTensionEdges(board, side, files),
      targetPressure = targets.map(target => target -> targetPressure(board, side, target)).toMap,
      fileAccess = openFiles.size * 2 + semiOpenFiles.size
    )

  private def dynamicTargets(beforeBoard: Board, afterBoard: Board, side: Color): List[String] =
    (
      WeaknessTargetProfile.targetsForPressure(beforeBoard, side) ++
        WeaknessTargetProfile.targetsForPressure(afterBoard, side)
    ).map(_.targetSquare).distinct ++
      (pawnAttackedEnemyTargets(beforeBoard, side) ++ pawnAttackedEnemyTargets(afterBoard, side)).distinct

  private def pawnAttackedEnemyTargets(board: Board, side: Color): List[String] =
    val enemyPawns = board.byPiece(!side, Pawn).squares.map(_.key).toSet
    board
      .byPiece(side, Pawn)
      .squares
      .flatMap(square => pawnAttacks(square.key, side).filter(enemyPawns.contains))
      .toList
      .distinct

  private def pawnTensionEdges(board: Board, side: Color, files: List[Char]): Set[String] =
    val sidePawns = board.byPiece(side, Pawn).squares
    val enemyPawns = board.byPiece(!side, Pawn).squares.map(_.key).toSet
    sidePawns
      .filter(square => square.key.headOption.exists(files.contains))
      .flatMap(square =>
        pawnAttacks(square.key, side)
          .filter(enemyPawns.contains)
          .map(target => s"${square.key}-$target")
      )
      .toSet

  private def targetPressure(board: Board, side: Color, target: String): Int =
    squareAt(target).map(square => board.attackers(square, side).count).getOrElse(0)

  private def kingShelterDelta(
      before: Option[PositionFeatures],
      after: Option[PositionFeatures],
      side: Color,
      files: List[Char]
  ): Int =
    val kingside = files.exists(file => Set('f', 'g', 'h').contains(file))
    if !kingside then 0
    else
      before.zip(after).map { case (beforeFeatures, afterFeatures) =>
        if side.white then beforeFeatures.kingSafety.blackKingShield - afterFeatures.kingSafety.blackKingShield
        else beforeFeatures.kingSafety.whiteKingShield - afterFeatures.kingSafety.whiteKingShield
      }.getOrElse(0)

  private def mobilityDelta(
      before: Option[PositionFeatures],
      after: Option[PositionFeatures],
      side: Color
  ): Int =
    before.zip(after).map { case (beforeFeatures, afterFeatures) =>
      if side.white then afterFeatures.activity.whitePseudoMobility - beforeFeatures.activity.whitePseudoMobility
      else afterFeatures.activity.blackPseudoMobility - beforeFeatures.activity.blackPseudoMobility
    }.getOrElse(0)

  private def developmentDelta(
      before: Option[PositionFeatures],
      after: Option[PositionFeatures],
      side: Color
  ): Int =
    before.zip(after).map { case (beforeFeatures, afterFeatures) =>
      if side.white then beforeFeatures.activity.whiteDevelopmentLag - afterFeatures.activity.whiteDevelopmentLag
      else beforeFeatures.activity.blackDevelopmentLag - afterFeatures.activity.blackDevelopmentLag
    }.getOrElse(0)

  private def centerControlDelta(
      before: Option[PositionFeatures],
      after: Option[PositionFeatures],
      side: Color
  ): Int =
    before.zip(after).map { case (beforeFeatures, afterFeatures) =>
      if side.white then afterFeatures.centralSpace.whiteCenterControl - beforeFeatures.centralSpace.whiteCenterControl
      else afterFeatures.centralSpace.blackCenterControl - beforeFeatures.centralSpace.blackCenterControl
    }.getOrElse(0)

  private def pawnsOnFile(board: Board, file: Char): List[Square] =
    Square.all.toList.filter(square =>
      square.key.headOption.contains(file) &&
        board.pieceAt(square).exists(_.role == Pawn)
    )

  private def sidePawnAttacksTarget(
      board: Board,
      side: Color,
      target: String
  ): Boolean =
    Square.all.exists { square =>
      board.pieceAt(square).exists(piece => piece.color == side && piece.role == Pawn) &&
        pawnAttacks(square.key, side).contains(target)
    }

  private def pawnAttacks(square: String, side: Color): List[String] =
    for
      file <- fileOf(square).toList
      rank <- rankOf(square).toList
      targetRank = rank + forwardDirection(side)
      targetFile <- List((file - 1).toChar, (file + 1).toChar)
      if targetFile >= 'a' && targetFile <= 'h' && targetRank >= 1 && targetRank <= 8
    yield s"$targetFile$targetRank"

  private def adjacentSquares(square: String): List[String] =
    for
      file <- fileOf(square).toList
      rank <- rankOf(square).toList
      df <- List(-1, 0, 1)
      dr <- List(-1, 0, 1)
      if df != 0 || dr != 0
      nextFile = (file + df).toChar
      nextRank = rank + dr
      if nextFile >= 'a' && nextFile <= 'h' && nextRank >= 1 && nextRank <= 8
    yield s"$nextFile$nextRank"

  private def squareAt(key: String): Option[Square] =
    Square.all.find(_.key == key)

  private def fileOf(square: String): Option[Char] =
    square.headOption.filter(file => file >= 'a' && file <= 'h')

  private def rankOf(square: String): Option[Int] =
    square.lift(1).flatMap(char => Option.when(char >= '1' && char <= '8')(char.asDigit))

  private def forwardDirection(side: Color): Int =
    if side.white then 1 else -1
