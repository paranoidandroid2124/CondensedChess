package lila.user

import lila.core.security.HashedPassword
import lila.db.dsl.*
import lila.user.BSONFields as F
import lila.core.userId.UserId

case class AuthData(
    _id: UserId,
    bpass: HashedPassword,
    salt: Option[String] = None,
    sha512: Option[Boolean] = None
)

object AuthData:
  val projection = $doc(F.bpass -> true, F.salt -> true, F.sha512 -> true)

  import reactivemongo.api.bson.*
  import lila.user.BSONHandlers.given
  
  given BSONHandler[HashedPassword] = BSONBinaryHandler.as[HashedPassword](HashedPassword.apply, _.bytes)

  given BSONDocumentReader[AuthData] = BSONDocumentReader.from[AuthData]: doc =>
    for
      id <- doc.getAsTry[UserId]("_id")
      bpass <- doc.getAsTry[HashedPassword](F.bpass)
      salt = doc.getAsOpt[String](F.salt)
      sha512 = doc.getAsOpt[Boolean](F.sha512)
    yield AuthData(id, bpass, salt, sha512)
