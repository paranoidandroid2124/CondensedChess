package lila.commentary.analysis.structure

import _root_.chess.{ Board, Color, Pawn, Square }

import lila.commentary.analysis.{ PositionAnalyzer, PositionFeatures }

private[commentary] final case class StructuralDelta(
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
    mobilityDelta: Int = 0
):
  def pawnTensionDelta: Int = pawnTensionAfter - pawnTensionBefore
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
      mobilityDelta != 0

private[commentary] object StructuralDeltaAnalyzer:

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
      createdTensionFrom: Option[String] = None
  ): Option[StructuralDelta] =
    val normalizedFiles = files.distinct
    val normalizedTargets = targets.distinct
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
        mobilityDelta = mobilityDelta(before.features, after.features, side)
      )
    )

  private def structureSnapshot(
      fen: String,
      board: Board,
      side: Color,
      files: List[Char],
      targets: List[String]
  ): StructureSnapshot =
    val features = Option.when(fen.trim.nonEmpty)(PositionAnalyzer.extractFeatures(fen, 1)).flatten
    val openFiles = files.filter(file => pawnsOnFile(board, file).isEmpty).map(_.toString).toSet
    val semiOpenFiles =
      files
        .filter(file =>
          val pawns = pawnsOnFile(board, file)
          pawns.nonEmpty && pawns.forall(square => board.pieceAt(square).exists(_.color != side))
        )
        .map(_.toString)
        .toSet
    val enemyPawns = board.byPiece(!side, Pawn)
    val weakPawns =
      (PositionAnalyzer.backwardPawns(!side, enemyPawns, board) ++ PositionAnalyzer.isolatedPawns(enemyPawns))
        .map(_.key)
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
