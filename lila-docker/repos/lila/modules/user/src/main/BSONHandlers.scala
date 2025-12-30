package lila.user

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import lila.core.user.*
import lila.core.userId.*
import lila.core.email.EmailAddress

object BSONHandlers:

  given userIdHandler: BSONHandler[UserId] = stringIsoHandler
  given userNameHandler: BSONHandler[UserName] = stringIsoHandler
  given userEnabledHandler: BSONHandler[UserEnabled] = booleanIsoHandler
  given roleDbKeyHandler: BSONHandler[RoleDbKey] = stringIsoHandler
  given totpSecretHandler: BSONHandler[TotpSecret] = BSONBinaryHandler.as[TotpSecret](TotpSecret.apply, _.value)
  given emailAddressHandler: BSONHandler[EmailAddress] = stringIsoHandler

  given BSONDocumentReader[User] = BSONDocumentReader.option[User]: doc =>
    for
      id <- doc.getAsOpt[UserId]("_id")
      username <- doc.getAsOpt[UserName](BSONFields.username)
      enabled <- doc.getAsOpt[UserEnabled](BSONFields.enabled)
      roles = doc.getAsOpt[List[RoleDbKey]](BSONFields.roles) | Nil
      createdAt <- doc.getAsOpt[Instant](BSONFields.createdAt)
      seenAt = doc.getAsOpt[Instant](BSONFields.seenAt)
    yield User(
      id = id,
      username = username,
      enabled = enabled,
      roles = roles,
      createdAt = createdAt,
      seenAt = seenAt
    )
