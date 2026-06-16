package lila.ui

import lila.ui.ScalatagsTemplate.{ *, given }

object bits:

  def pageMenuSubnav(mods: Modifier*) = st.aside(cls := "subnav"):
    st.nav(cls := "subnav__inner page-menu__menu")(mods)

  def mselect(id: String, current: Frag, items: Seq[Tag]) =
    div(cls := "mselect")(
      input(
        tpe := "checkbox",
        cls := "mselect__toggle fullscreen-toggle",
        st.id := s"mselect-$id",
        autocomplete := "off"
      ),
      label(`for` := s"mselect-$id", cls := "mselect__label")(current),
      label(`for` := s"mselect-$id", cls := "fullscreen-mask"),
      st.nav(cls := "mselect__list")(items.map(_(cls := "mselect__item")))
    )
