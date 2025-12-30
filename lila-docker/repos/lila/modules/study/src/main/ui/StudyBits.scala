package lila.study
package ui

import lila.common.String.removeMultibyteSymbols
import lila.core.study.StudyOrder
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StudyBits(helpers: Helpers):
  import helpers.{ *, given }

  def orderSelect(order: StudyOrder, active: String, url: StudyOrder => Call)(using Context) =
    val orders =
      if active == "search" then Orders.search
      else if active == "all" then Orders.withoutSelector
      else if active.startsWith("topic") then Orders.list
      else Orders.withoutMine
    lila.ui.bits.mselect(
      "orders",
      span(Orders.name(order)),
      Orders.list.map: o =>
        a(href := url(o), cls := (order == o).option("current"))(Orders.name(o))
    )

  def newForm()(using Context) =
    postForm(cls := "new-study", action := routes.Study.create)(
      submitButton(
        cls := "button button-green",
        dataIcon := Icon.PlusButton,
        title := "Create a new study"
      )
    )

  def authLinks(active: String, order: StudyOrder)(using Context) =
    def activeCls(c: String) = cls := (c == active).option("active")
    frag(
      a(activeCls("mine"), href := routes.Study.mine(order, 1))("My studies"),
      a(activeCls("mineMember"), href := routes.Study.mineMember(order, 1))(
        "Studies I contribute to"
      ),
      a(activeCls("minePublic"), href := routes.Study.minePublic(order, 1))("My public studies"),
      a(activeCls("minePrivate"), href := routes.Study.minePrivate(order, 1))(
        "My private studies"
      ),
      a(activeCls("mineLikes"), href := routes.Study.mineLikes(order, 1))("My favorite studies")
    )

  def widget(s: Study.WithChaptersAndLiked, tag: Tag = h2)(using ctx: Context) =
    frag(
      a(cls := "overlay", href := routes.Study.show(s.study.id), title := s.study.name),
      div(cls := "top")(
        div(cls := "study__icon")(
          iconTag(Icon.StudyBoard)
        ),
        div(
          tag(cls := "study-name")(s.study.name),
          span(
            (!s.study.isPublic).option(
              frag(
                iconTag(Icon.Padlock)(cls := "private", ariaTitle("Private")),
                " "
              )
            ),
            iconTag(if s.liked then Icon.Heart else Icon.HeartOutline),
            " ",
            s.study.likes.value,
            " • ",
            titleNameOrId(s.study.ownerId),
            " • ",
            momentFromNow(s.study.createdAt)
          )
        )
      ),
      div(cls := "body")(
        ol(cls := "chapters")(
          s.chapters.map: name =>
            li(cls := "text", dataIcon := Icon.DiscBigOutline)(
              if ctx.userId.exists(s.study.isMember) then name.value
              else removeMultibyteSymbols(name.value)
            )
        ),
        ol(cls := "members")(
          s.study.members.members.values
            .take(Study.previewNbMembers)
            .map: m =>
              li(cls := "text", dataIcon := (if m.canContribute then Icon.RadioTower else Icon.Eye))(
                titleNameOrId(m.id)
              )
            .toList
        )
      )
    )

