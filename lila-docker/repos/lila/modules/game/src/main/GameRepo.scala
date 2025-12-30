package lila.game

import lila.core.game.*
import lila.db.dsl.{ *, given }

final class GameRepo(c: Coll)(using Executor) extends lila.core.game.GameRepo(c):

  import BSONHandlers.given

  def game(gameId: GameId): Fu[Option[Game]] = coll.byId[Game](gameId)
  def remove(id: GameId): Funit = coll.delete.one($id(id)).void
  def exists(id: GameId) = coll.exists($id(id))
  def analysed(id: GameId): Fu[Option[Game]] = fuccess(None)
  def initialFen(gameId: GameId): Fu[Option[chess.format.Fen.Full]] = fuccess(None)
  def count(query: Query.type => Bdoc): Fu[Int] = fuccess(0)
  def countSec(query: Query.type => Bdoc): Fu[Int] = fuccess(0)
  def recentAnalysableGamesByUserId(userId: UserId, nb: Int): Fu[List[Game]] = fuccess(Nil)
  def deleteAllSinglePlayerOf(id: UserId): Fu[List[GameId]] = fuccess(Nil)
  def lastPlayedPlayingId(userId: UserId): Fu[Option[GameId]] = fuccess(None)
  def sortedCursor(user: UserId, pk: PerfKey): reactivemongo.akkastream.AkkaStreamCursor[Game] = ???
  
  override given gameHandler: reactivemongo.api.bson.BSONDocumentHandler[Game] = BSONHandlers.gameHandler
  override given statusHandler: reactivemongo.api.bson.BSONHandler[chess.Status] = BSONHandlers.statusHandler

  val light: GameLightRepo = new GameLightRepo:
    def gamesFromSecondary(gameIds: Seq[GameId]): Fu[List[LightGame]] = fuccess(Nil)
    def gamesFromPrimary(gameIds: Seq[GameId]): Fu[List[LightGame]] = fuccess(Nil)

  def gameFromSecondary(gameId: GameId): Fu[Option[Game]] = fuccess(None)
  def gamesFromSecondary(gameIds: Seq[GameId]): Fu[List[Game]] = fuccess(Nil)
  def gameOptionsFromSecondary(gameIds: Seq[GameId]): Fu[List[Option[Game]]] = fuccess(gameIds.map(_ => None).toList)
  def getSourceAndUserIds(id: GameId): Fu[(Option[Source], List[UserId])] = fuccess(None -> Nil)
  def initialFen(game: Game): Fu[Option[chess.format.Fen.Full]] = fuccess(None)
  def withInitialFen(game: Game): Fu[WithInitialFen] = fuccess(WithInitialFen(game, None))
  def gameWithInitialFen(gameId: GameId): Fu[Option[WithInitialFen]] = fuccess(None)
  def isAnalysed(game: Game): Fu[Boolean] = fuccess(false)
  def insertDenormalized(g: Game, initialFen: Option[chess.format.Fen.Full] = None): Funit = funit
  def lastGamesBetween(u1: lila.core.user.User, u2: lila.core.user.User, since: java.time.Instant, nb: Int): Fu[List[Game]] = fuccess(Nil)
  def setAnalysed(id: GameId, v: Boolean): Funit = funit
  def finish(id: GameId, winnerColor: Option[chess.Color], winnerId: Option[UserId], status: chess.Status): Funit = funit
  def countWhereUserTurn(userId: UserId): Fu[Int] = fuccess(0)
