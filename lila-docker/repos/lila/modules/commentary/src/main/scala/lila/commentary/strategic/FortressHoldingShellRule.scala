package lila.commentary.strategic

import chess.Color

import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessValue }
import lila.commentary.strategic.StrategicObjectHelpers.*

private[strategic] object FortressHoldingShellRule extends StrategicObjectRule:

  val familyId: StrategicObjectId = StrategicObjectId("FortressHoldingShell")

  def extract(
      context: StrategicObjectContext,
      extractedSoFar: StrategicObjectSet
  ): Vector[StrategicObject] =
    Vector(Color.White, Color.Black).flatMap: holder =>
      context.board.kingSquare(holder).toVector.flatMap: kingSquare =>
        if kingSquare.rank != homeRank(holder) || !noQueensRemain(context) then Vector.empty
        else
          val shellMask = fortressShellMask(context, holder)
          val shellSquares = shellMask.toVector.sortBy(_.value)
          val occupiedShellSquares = occupiedNonKingSquares(context, holder, shellMask)
          val enemyOccupiedShellSquares = occupiedSquaresByColor(context, !holder, shellMask)
          val deniedEntryAxes = entryAxesIntoMask(context, !holder, shellMask)
          val nearbyMajorFilePressure =
            kingNeighborhoodFiles(context, holder).exists: file =>
              fileLaneWitnessOnFile(context, file).nonEmpty &&
                context
                  .activePieceSquares(!holder, chess.Rook)
                  .concat(context.activePieceSquares(!holder, chess.Queen))
                  .filter(_.file == file)
                  .exists(origin => shellSquares.exists(target => context.board.attacksSquare(origin, target)))
          val attackerPassedPawns =
            passedPawnWitnessSquares(context, !holder).filter(square =>
              sameAndAdjacentFiles(kingSquare.file).contains(square.file)
            )
          val blockedPasserSquares =
            attackerPassedPawns.filter(square =>
              context.forwardSquare(!holder, square).exists(next => context.pieceAt(next).exists(_.color == holder))
            )

          Option.when(
            occupiedShellSquares.size >= 2 &&
              enemyOccupiedShellSquares.isEmpty &&
              deniedEntryAxes.isEmpty &&
              !nearbyMajorFilePressure &&
              attackerPassedPawns.forall(blockedPasserSquares.contains)
          )(
            owned(
              color = holder,
              anchor = WitnessAnchor.SquareAnchor(kingSquare),
              payload = WitnessPayload(
                "holder" -> WitnessValue.ColorValue(holder),
                "king_square" -> WitnessValue.SquareValue(kingSquare),
                "shell_squares" -> WitnessValue.SquareListValue(shellSquares),
                "occupied_shell_squares" -> WitnessValue.SquareListValue(occupiedShellSquares),
                "attacker_passed_pawn_squares" -> WitnessValue.SquareListValue(attackerPassedPawns),
                "blocked_passer_squares" -> WitnessValue.SquareListValue(blockedPasserSquares)
              ),
              support = support(
                indices =
                  (occupiedPieceRootIndices(context, occupiedShellSquares) ++
                    rootIndicesForSquares(context, SchemaId.PassedPawn, !holder, attackerPassedPawns) ++
                    blockedPasserSquares.flatMap(square =>
                      context.forwardSquare(!holder, square).toVector.flatMap(blocker =>
                        context.pieceAt(blocker).flatMap(piece =>
                          context.pieceOnRootIndex(piece.color, piece.role, blocker)
                        )
                      )
                    )).distinct.sorted,
                targetSquares = occupiedShellSquares ++ blockedPasserSquares
              )
            )
          ).toVector
