package lila.user

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import lila.core.user.*
import lila.core.userId.*
import lila.core.email.EmailAddress

import scala.util.{ Success, Try }

object BSONHandlers:

  given userIdHandler: BSONHandler[UserId] = stringIsoHandler
  given userNameHandler: BSONHandler[UserName] = stringIsoHandler
  given userEnabledHandler: BSONHandler[UserEnabled] = booleanIsoHandler
  given roleDbKeyHandler: BSONHandler[RoleDbKey] = stringIsoHandler
  given totpSecretHandler: BSONHandler[TotpSecret] = BSONBinaryHandler.as[TotpSecret](TotpSecret.apply, _.value)
  given emailAddressHandler: BSONHandler[EmailAddress] = stringIsoHandler

  given userTierHandler: BSONHandler[lila.core.user.UserTier] = BSONHandler.from[lila.core.user.UserTier](
    {
      case BSONString(s) => Success(lila.core.user.UserTier.values.find(_.name == s) | lila.core.user.UserTier.Free)
      case _             => Success(lila.core.user.UserTier.Free)
    },
    tier => Success(BSONString(tier.name))
  )

  given userBSONReader: BSONDocumentReader[User] = BSONDocumentReader.option[User]: doc =>
    for
      id <- doc.getAsOpt[UserId]("_id")
      username <- doc.getAsOpt[UserName](BSONFields.username)
      enabled <- doc.getAsOpt[UserEnabled](BSONFields.enabled)
      roles = doc.getAsOpt[List[RoleDbKey]](BSONFields.roles) | Nil
      createdAt <- doc.getAsOpt[Instant](BSONFields.createdAt)
      seenAt = doc.getAsOpt[Instant](BSONFields.seenAt)
      lang = doc.getAsOpt[String](BSONFields.lang)
      totpSecret = doc.getAsOpt[TotpSecret](BSONFields.totpSecret)
      email = doc.getAsOpt[EmailAddress](BSONFields.email)
      tier = doc.getAsOpt[UserTier](BSONFields.tier) | UserTier.Free
      expiresAt = doc.getAsOpt[Instant](BSONFields.expiresAt)
    yield User(
      id = id,
      username = username,
      enabled = enabled,
      roles = roles,
      createdAt = createdAt,
      seenAt = seenAt,
      lang = lang,
      totpSecret = totpSecret,
      email = email,
      tier = tier,
      expiresAt = expiresAt
    )

  given userBSONWriter: BSONDocumentWriter[User] = BSONDocumentWriter[User]: u =>
    $doc(
      "_id" -> u.id,
      BSONFields.username -> u.username,
      BSONFields.enabled -> u.enabled,
      BSONFields.roles -> u.roles,
      BSONFields.createdAt -> u.createdAt,
      BSONFields.seenAt -> u.seenAt,
      BSONFields.lang -> u.lang,
      BSONFields.totpSecret -> u.totpSecret,
      BSONFields.email -> u.email,
      BSONFields.tier -> u.tier,
      BSONFields.expiresAt -> u.expiresAt
    )
