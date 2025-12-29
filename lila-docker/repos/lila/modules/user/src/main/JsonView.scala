package lila.user

import play.api.libs.json.*
import lila.core.LightUser
import lila.core.user.User

final class JsonView(isOnline: UserId => Boolean) extends lila.core.user.JsonView:

  def full(u: User): JsObject =
    if u.enabled.no then disabled(u.light)
    else
      base(u) ++ Json.obj(
        "seenAt" -> u.seenAt
      )

  private def base(u: User) =
    Json.obj(
      "id" -> u.id.value, // Convert UserId to String
      "username" -> u.username.value, // Convert UserName to String
      "online" -> isOnline(u.id)
    )

  def disabled(u: LightUser) = Json.obj(
    "id" -> u.id.value,
    "username" -> u.name.value,
    "disabled" -> true
  )

  def ghost = disabled(LightUser.ghost)
