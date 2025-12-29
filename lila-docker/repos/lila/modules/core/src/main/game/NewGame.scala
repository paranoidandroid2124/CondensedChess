package lila.core
package game

import _root_.chess.format.Fen
import _root_.chess.{ ByColor, Game as ChessGame, Status }
import scalalib.ThreadLocalRandom

import lila.core.id.GameId

case class ImportedGame(sloppy: Game, initialFen: Option[Fen.Full] = None):

  def withId(id: GameId): Game = sloppy.copy(id = id)

def newImportedGame(
    chess: ChessGame,
    players: ByColor[Player],
    source: Source,
    pgnImport: Option[PgnImport]
): ImportedGame = ImportedGame(newSloppy(chess, players, source, pgnImport))

// Wrapper around newly created games. We do not know if the id is unique, yet.
case class NewGame(sloppy: Game):
  def withId(id: GameId): Game = sloppy.copy(id = id)

def newGame(
    chess: ChessGame,
    players: ByColor[Player],
    source: Source,
    pgnImport: Option[PgnImport]
): NewGame = NewGame(newSloppy(chess, players, source, pgnImport))

private def newSloppy(
    chess: ChessGame,
    players: ByColor[Player],
    source: Source,
    pgnImport: Option[PgnImport]
): Game =
  val createdAt = nowInstant
  new Game(
    id = IdGenerator.uncheckedGame,
    players = players,
    chess = chess,
    status = Status.Created,
    metadata = newMetadata(source).copy(pgnImport = pgnImport),
    createdAt = createdAt,
    movedAt = createdAt
  )
