package lila.oauth
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TokenUi(helpers: Helpers)(
    AccountPage: (String, String) => Context ?=> Page,
    mode: play.api.Mode
):
  import helpers.{ *, given }

  def index(tokens: List[AccessToken])(using Context) =
    AccountPage("Personal Access Tokens", "oauth.token"):
      div(cls := "oauth box")(
        boxTop(
          h1("Personal Access Tokens"),
          st.form(cls := "box-top__actions", action := routes.OAuthToken.create)(
            button(
              tpe := "submit",
              cls := "button frameless",
              st.title := "New access token",
              dataIcon := Icon.PlusButton
            )
          )
        ),
        // standardFlash removed
        p(cls := "box__pad force-ltr")(
          "You can make OAuth requests using the ",
          a(href := s"/api#section/Introduction/Authentication")("authorization code flow"),
          br,
          br,
          "Instead, you can generate a personal access token below. ",
          a(href := routes.OAuthToken.create)("Generate a personal token"),
          br,
          br,
          "Guard these tokens carefully! They are like passwords.",
          br,
          br,
          "For developers: ",
          a(href := "https://github.com/lichess-org/api/tree/master/example/oauth-personal-token")(
            "Personal token app example"
          ),
          " | ",
          a(href := "/api")("API documentation")
        ),
        tokens.headOption.filter(_.isBrandNew).map { token =>
          div(cls := "box__pad brand")(
            if token.isDangerous then iconTag(Icon.CautionTriangle)(cls := "is-red")
            else iconTag(Icon.Checkmark)(cls := "is-green"),
            div(
              if token.isDangerous
              then p(strong("Do not share it!"))
              else p("Copy this token now. You won't be able to see it again."),
              code(token.plain.value)
            )
          )
        },
        table(cls := "slist slist-pad")(
          tokens.map: t =>
            tr(
              td(
                strong(t.description | "Unnamed"),
                br,
                em(t.scopes.value.map(_.name).mkString(", ")),
                mode.isDev.option(frag(br, em("Visible in DEV mode: "), code(t.plain.value)))
              ),
              td(cls := "date")(
                t.created.map: created =>
                  frag("Created: ", created.toString, br),
                t.usedAt.map: used =>
                  frag("Last used: ", used.toString)
              ),
              td(cls := "action")(
                form(method := "post", action := routes.OAuthToken.delete(t.id.value))(
                  button(
                    tpe := "submit",
                    cls := "button button-red button-empty yes-no-confirm"
                  )("Delete")
                )
              )
            )
        )
      )

  def create(form: Form[OAuthTokenForm.Data], me: User)(using Context) =
    AccountPage("New Access Token", "oauth.token"):
      div(cls := "oauth box box-pad")(
        h1(cls := "box__top")("New Access Token"),
        st.form(cls := "form3", method := "post", action := routes.OAuthToken.create)(
          div(cls := "form-group")(
            p("A token grants permission to your account."),
            p("Carefully select the permissions you need.")
          ),
          div(cls := "form-group")(
            label("Token description"),
            input(
              tpe := "text",
              name := form("description").name,
              value := form("description").value.getOrElse(""),
              placeholder := "What is this token for?",
              autofocus
            ),
            small("Remember what you use this token for.")
          ),
          br,
          br,
          h2("What the token can do"),
          div(cls := "scopes")(
            OAuthScope.classified.map: (categ, scopes) =>
              fieldset(
                legend(categ),
                scopes.map: scope =>
                  val disabled = false // Simplified: removed bot/board restrictions
                  val hidden = scope == OAuthScope.Web.Mod && !OAuthScope.canUseWebMod
                  val id = s"oauth-scope-${scope.key.replace(":", "_")}"
                  (!hidden).option(
                    div(cls := List("danger" -> OAuthScope.dangerList.has(scope)))(
                      span(
                        input(
                          tpe := "checkbox",
                          st.id := id,
                          name := s"${form("scopes").name}[]",
                          value := scope.key,
                          checked := !disabled && form.data.valuesIterator.contains(scope.key),
                          st.disabled := disabled
                        )
                      ),
                      label(`for` := id)(
                        scope.name,
                        em(scope.key)
                      )
                    )
                  )
              )
          ),
          div(cls := "form-actions")(
            a(href := routes.OAuthToken.index)("Cancel"),
            button(tpe := "submit", cls := "button")("Create")
          )
        )
      )
