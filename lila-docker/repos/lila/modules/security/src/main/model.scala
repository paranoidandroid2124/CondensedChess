package lila.security

import lila.core.id.SessionId
import lila.core.net.{ IpAddress, UserAgent, ApiVersion }
import java.time.Instant
import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros
import lila.db.dsl.{ *, given }

case class UserSession(
  _id: SessionId,
  ip: IpAddress,
  ua: Option[UserAgent],
  date: Instant,
  up: Boolean,
  api: Option[ApiVersion]
)

object UserSession:
  given BSONDocumentReader[UserSession] = Macros.reader

case class Dated[A](value: A, date: Instant)
