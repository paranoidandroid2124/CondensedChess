package lila.ui

import play.api.i18n.Lang
import java.time.Month
import java.time.format.TextStyle

// Minimal helpers trait for UI components
trait Helpers:
  def showMonth(m: Month)(using lang: Lang): String =
    m.getDisplayName(TextStyle.FULL, lang.locale)
