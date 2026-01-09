package views.base

import lila.app.UiEnv.{ *, given }
import lila.i18n.trans

def notFound(msg: Option[String]) =
  Page(msg | "Page not found").css("bits.not-found"):
    main(cls := "not-found page-small box box-pad")(
      header(
        h1("404"),
        div(
          strong("Page not found!"),
          msg.map(em(_)),
          p(cls := "text")(
            trans.pageNotFound,
            br,
            a(href := routes.UserAnalysis.index.url)("the homepage"),
            br,
            a(href := routes.Main.contact.url)("Contact")
          )
        )
      ),
      div(cls := "game")(
        iframe(
          src := staticAssetUrl(s"vendor/ChessPursuit/bin-release/index.html"),
          st.frameborder := 0,
          widthA := 400,
          heightA := 500,
          frame.credentialless
        ),
        p(cls := "credits")(
          a(href := "https://github.com/Saturnyn/ChessPursuit")("ChessPursuit"),
          " courtesy of ",
          a(href := "https://github.com/Saturnyn")("Saturnyn")
        )
      )
    )

def notFoundEmbed(msg: Option[String])(using EmbedContext) =
  views.base.embed.site(title = msg | "Page not found", cssKeys = List("bits.embed-not-found"))(
    main(cls := "not-found page-small box box-pad")(
      header(
        h1("404"),
        div(
          strong("Page not found!"),
          msg.map(em(_))
        )
      )
    )
  )
