package lila.web
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

object bits:

  object splitNumber:
    private val NumberFirstRegex = """(\d++)\s(.+)""".r
    private val NumberLastRegex = """\s(\d++)$""".r.unanchored

    def apply(s: Frag)(using ctx: Context): Frag =
      if ctx.blind then s
      else
        val rendered = s.render
        rendered match
          case NumberFirstRegex(number, html) =>
            frag(
              strong(number.toIntOption.getOrElse(0)),
              br,
              raw(html)
            )
          case NumberLastRegex(n) if rendered.length > n.length + 1 =>
            frag(
              raw(rendered.dropRight(n.length + 1)),
              br,
              strong(n.toIntOption.getOrElse(0))
            )
          case h => raw(h.replaceIf('\n', "<br>"))


  val connectLinks: Frag = frag()
