package lila.game

import reactivemongo.api.bson.*
import scala.util.{ Failure, Success, Try }
import lila.core.game.{ Game, LightGame, Source }
import lila.db.BSON.*

object BSONHandlers:

  given statusHandler: BSONHandler[chess.Status] = tryHandler[chess.Status](
    { case BSONInteger(v) =>
      chess.Status(v).fold[Try[chess.Status]](Failure(new Exception(s"Invalid status id $v")))(Success(_))
    },
    x => BSONInteger(x.id)
  )

  given sourceHandler: BSONHandler[Source] = tryHandler[Source](
    { case BSONInteger(v) => Success(Source.byId.getOrElse(v, Source.Lobby)) },
    x => BSONInteger(x.id)
  )

  given gameHandler: lila.db.BSON[Game] with
    def reads(r: Reader): Game = 
      throw new UnsupportedOperationException("Game loading from DB disabled - Study-only system")
    def writes(w: Writer, o: Game) = BSONDocument()

  given lightGameReader: lila.db.BSONReadOnly[lila.core.game.LightGame] with
    def reads(r: Reader): lila.core.game.LightGame = 
      throw new UnsupportedOperationException("LightGame loading from DB disabled - Study-only system")
