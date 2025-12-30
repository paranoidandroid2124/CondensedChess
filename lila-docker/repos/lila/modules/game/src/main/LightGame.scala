package lila.game

import chess.Color

import lila.core.game.LightPlayer

object LightGame:

  def projection =
    lila.db.dsl.$doc(
      "p0" -> true,
      "p1" -> true,
      "us" -> true,
      "w" -> true,
      "s" -> true,
      "v" -> true
    )

object LightPlayer:

  import reactivemongo.api.bson.*
  import lila.db.dsl.{ *, given }

  private[game] type Builder = Color => Option[UserId] => LightPlayer

  given lightPlayerReader: BSONDocumentReader[Builder] with
    import scala.util.{ Try, Success }
    def readDocument(doc: Bdoc): Try[Builder] = Success(builderRead(doc))

  def builderRead(doc: Bdoc): Builder = color =>
    userId =>
      new LightPlayer(
        color = color,
        aiLevel = doc.int("ai"),
        userId = userId
      )
