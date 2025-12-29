package lila.user

import play.api.libs.json.*

import lila.core.LightUser
import lila.core.user.User

final class JsonView(isOnline: lila.core.socket.IsOnline) extends lila.core.user.JsonView:

  def full(
      u: User
  ): JsObject =
    if u.enabled.no then disabled(u.light)
    else
      base(u) ++ Json
        .add("seenAt" -> u.seenAt)

  private def base(u: User) =
    Json
      .obj(
        "id" -> u.id,
        "username" -> u.username
      )
      .obj(
        "id" -> u.id,
        "username" -> u.username
      )

  def disabled(u: LightUser) = Json.obj(
    "id" -> u.id,
    "username" -> u.name,
    "disabled" -> true
  )
  def ghost = disabled(LightUser.ghost)
