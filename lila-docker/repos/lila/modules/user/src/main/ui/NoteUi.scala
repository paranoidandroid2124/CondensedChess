package lila.user
package ui

import scalalib.paginator.Paginator

import lila.core.config.NetDomain
import lila.ui.*

import lila.ui.ScalatagsTemplate.{ *, given }

final class NoteUi(helpers: Helpers)(using NetDomain):
  import helpers.{ *, given }

  def zone(u: User, notes: List[Note])(using ctx: Context) = div(cls := "note-zone")(
    postForm(cls := "note-form", action := routes.User.writeNote(u.username))(
      form3.textarea(lila.user.UserForm.note("text"))(
        placeholder := trans.site.writeAPrivateNoteAboutThisUser.txt()
      ),
      submitButton(cls := "button", name := "noteType", value := "normal")(trans.site.save())
    ),
    notes.isEmpty.option(div(trans.site.noNoteYet())),
    notes.map: note =>
      div(cls := "note")(
        p(cls := "note__text")(richText(note.text, expandImg = false)),
        (note.mod && Granter.opt(_.Admin)).option(
          postForm(
            action := routes.User.setDoxNote(note._id, !note.dox)
          ):
            submitButton(cls := "button-empty yes-no-confirm button text")("Toggle Dox")
        ),
        p(cls := "note__meta")(
          userIdLink(note.from.some),
          br,
          note.dox.option("dox "),
          if Granter.opt(_.ModNote) then momentFromNowServer(note.date)
          else momentFromNow(note.date),
          (ctx.me.exists(note.isFrom) && !note.mod).option(
            frag(
              br,
              postForm(action := routes.User.deleteNote(note._id))(
                submitButton(
                  cls := "button-empty button-red yes-no-confirm button text",
                  style := "float:right",
                  dataIcon := Icon.Trash
                )(trans.site.delete())
              )
            )
          )
        )
      )
  )

