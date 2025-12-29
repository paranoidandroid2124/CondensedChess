package lila.user
import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{
  cleanFewSymbolsAndNonEmptyText,
  cleanFewSymbolsText,
  cleanNonEmptyText,
  cleanTextWithSymbols,
  into,
  playerTitle
}
import lila.common.LameName

final class UserForm:

  def username(user: User): Form[UserName] = Form(
    single(
      "username" -> cleanNonEmptyText
        .into[UserName]
        .verifying(
          "changeUsernameNotSame",
          name => name.id == user.username.id && name != user.username
        )
        .verifying(
          "usernameUnacceptable",
          name => !LameName.hasTitle(name.value) || LameName.hasTitle(user.username.value)
        )
    )
  ).fill(user.username)

  def usernameOf(user: User) = username(user).fill(user.username)

