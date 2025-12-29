package lila.ui

import scala.annotation.targetName
import chess.{ PlayerTitle, IntRating }
import chess.rating.{ IntRatingDiff, RatingProvisional }

import lila.core.LightUser
import lila.ui.ScalatagsTemplate.{ *, given }

trait UserHelper:
  self: I18nHelper & NumberHelper & AssetHelper =>
  def lightUserSync: LightUser.GetterSync

  def usernameOrId(userId: UserId): UserName = lightUserSync(userId).fold(userId.into(UserName))(_.name)
  def titleNameOrId(userId: UserId): String = lightUserSync(userId).fold(userId.value)(_.titleName)
  def titleNameOrAnon(userId: Option[UserId]): String =
    userId.flatMap(lightUserSync).fold(UserName.anonymous.value)(_.titleName)



  def anonUserSpan(cssClass: Option[String] = None, modIcon: Boolean = false) =
    span(cls := List("offline" -> true, "user-link" -> true, ~cssClass -> cssClass.isDefined))(
      if modIcon then frag(moderatorIcon, UserName.anonMod)
      else UserName.anonymous
    )

  def userIdLink[U: UserIdOf](
      userIdOption: Option[U],
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      truncate: Option[Int] = None,
      params: String = "",
      modIcon: Boolean = false,
      withFlair: Boolean = true,
      withPowerTip: Boolean = true
  )(using Translate): Tag =
    userIdOption
      .flatMap(u => lightUserSync(u.id))
      .fold[Tag](anonUserSpan(cssClass, modIcon)): user =>
        userIdNameLink(
          userId = user.id,
          username = user.name,
          cssClass = cssClass,
          withOnline = withOnline,
          truncate = truncate,
          params = params,
          modIcon = modIcon,
          withPowerTip = withPowerTip
        )

  def lightUserLink(
      user: LightUser,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      truncate: Option[Int] = None,
      params: String = ""
  )(using Translate): Tag =
    userIdNameLink(
      userId = user.id,
      username = user.name,
      cssClass = cssClass,
      withOnline = withOnline,
      truncate = truncate,
      params = params,
      modIcon = false
    )

  def lightUserSpan(
      user: LightUser,
      cssClass: Option[String] = None,
      withOnline: Boolean = true
  )(using Translate): Tag =
    span(
      cls := userClass(user.id, cssClass),
      dataHref := userUrl(user.name)
    )(
      user.name
    )

  def userLink(
      user: User,
      withOnline: Boolean = true,
      withPowerTip: Boolean = true,
      withTitle: Boolean = true,
      name: Option[Frag] = None,
      params: String = "",
      withFlair: Boolean = true
  )(using Translate): Tag =
    a(
      cls := userClass(user.id, none, withPowerTip),
      href := userUrl(user.username, params)
    )(userLinkContent(user, name))

  def userSpan(
      user: User,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withPowerTip: Boolean = true,
      withTitle: Boolean = true,
      name: Option[Frag] = None
  )(using Translate): Tag =
    span(
      cls := userClass(user.id, cssClass, withPowerTip),
      dataHref := userUrl(user.username)
    )(userLinkContent(user, name))

  def userLinkContent(
      user: User,
      name: Option[Frag] = None
  )(using Translate) = frag(
    name | user.username
  )

  def userIdNameLink(
      userId: UserId,
      username: UserName,
      cssClass: Option[String],
      withOnline: Boolean,
      truncate: Option[Int],
      params: String,
      modIcon: Boolean,
      withPowerTip: Boolean = true
  )(using Translate): Tag =
    a(
      cls := userClass(userId, cssClass, withPowerTip),
      st.href := userUrl(username, params = params).getOrElse("#")
    )(
      if modIcon then moderatorIcon else emptyFrag,
      truncate.fold(username.value)(username.value.take)
    )

  def userIdSpanMini(userId: UserId, withOnline: Boolean = false)(using Translate): Tag =
    val user = lightUserSync(userId)
    val name = user.fold(userId.into(UserName))(_.name)
    span(
      cls := userClass(userId, none),
      dataHref := userUrl(name)
    )(
      withOnline.so(lineIcon),
      name
    )

  def userUrl(username: UserName, params: String = ""): Option[String] =
    Option.when(username.id.noGhost):
      s"${routes.User.show(username)}$params"

  def userClass(
      userId: UserId,
      cssClass: Option[String],
      withPowerTip: Boolean = true
  ): List[(String, Boolean)] =
    if userId.isGhost then List("user-link" -> true, ~cssClass -> cssClass.isDefined)
    else
      List(
        "user-link" -> true,
        ~cssClass -> cssClass.isDefined,
        "ulpt" -> withPowerTip
      )


  val lineIconChar = Icon.Disc

  val lineIcon: Frag = i(cls := "line")

  val moderatorIcon: Frag = i(cls := "line moderator", title := "Lichess Mod")

  def lineIcon(user: User)(using Translate): Frag = emptyFrag
  def lineIconChar(user: User): Icon = lineIconChar
