package lila.analyse
package ui

import lila.llm.model.strategic.VariationLine
import lila.ui.ScalatagsTemplate.*
import lila.llm.analysis.NarrativeUtils
import scalalib.StringUtils.escapeHtmlRaw
import scala.collection.mutable

/**
 * Bookmaker Renderer
 * 
 * Transforms raw LLM text and engine variation data into 
 * interactive HTML fragments for the Chesstory AI commentary UI.
 */
object BookmakerRenderer:

  private case class MoveBoardRef(uci: String, fenAfter: String)

  /** Use scalalib's escapeHtmlRaw for consistency */
  private def escapeHtml(text: String): String = escapeHtmlRaw(text)

  /**
   * Main entry point for rendering a Bookmaker panel.
   * @param commentary The raw text from the LLM.
   * @param variations Structured Multi-PV data.
   */
  def render(commentary: String, variations: List[VariationLine], fenBefore: String): Frag =
    // Preserve SAN occurrence order across variations to keep move-chip mapping stable.
    val sanRefs: List[(String, MoveBoardRef)] = variations.flatMap { v =>
      val sanList = NarrativeUtils.uciListToSan(fenBefore, v.moves)
      val fensAfter = fenAfterEachMove(fenBefore, v.moves)
      sanList.zip(v.moves).zip(fensAfter).map { case ((san, uci), fenAfter) =>
        san -> MoveBoardRef(uci = uci, fenAfter = fenAfter)
      }
    }
    
    div(cls := "bookmaker-content")(
      div(cls := "bookmaker-toolbar")(
        button(
          cls := "bookmaker-score-toggle",
          attr("type") := "button",
          attr("aria-pressed") := "true"
        )("Eval: On")
      ),
      // Hover preview board (mounted by client; stays hidden until hover).
      div(cls := "bookmaker-pv-preview"),
      div(cls := "commentary")(
        renderTextWithMoves(commentary, sanRefs, variations, fenBefore)
      ),
      variations.nonEmpty.option:
        div(cls := "alternatives")(
          h3("Alternative Options"),
          renderVariations(variations, fenBefore)
        )
    )

  /**
   * Wraps chess moves appearing in the text in span tags for client-side interaction.
   * NOTE: This uses raw HTML injection; ensure input text is sanitized.
   */
  private def renderTextWithMoves(
      text: String,
      sanRefs: List[(String, MoveBoardRef)],
      variations: List[VariationLine],
      fenBefore: String
  ): Frag =
    // Support Markdown-style emphasis used by the LLM prompt (**...**).
    val boldRegex = """\*\*(.+?)\*\*""".r

    // Match standard SAN moves like Nf3, e4, O-O, etc.
    val moveRegex = """\b([NBKRQ]?[a-h]x?[a-h]?[1-8][+#]?|O-O(?:-O)?|1\.\s+[a-h][1-8])\b""".r

    // Detect LLM "alternative" lines like: a) Bb5 ...  b) Bc4 ...  c) d4 ...
    // When possible, replace them with the structured engine variations so each move can drive a mini-board hover preview.
    val pvLineRegex = """^\s*([a-z])\)\s+.*$""".r

    val sanQueues: mutable.Map[String, mutable.Queue[MoveBoardRef]] =
      mutable.Map.from:
        sanRefs
          .groupBy(_._1)
          .view
          .mapValues(entries => mutable.Queue.from(entries.map(_._2)))
          .toMap

    val sanFallback: Map[String, MoveBoardRef] =
      sanRefs.groupBy(_._1).view.mapValues(_.head._2).toMap

    val inlineVariationHtml: Map[Char, String] =
      variations.zipWithIndex.map { (v, idx) =>
        val labelChar = (idx + 97).toChar
        val label = s"$labelChar)"
        val sanList = NarrativeUtils.uciListToSan(fenBefore, v.moves)
        val renderedSans = sanList.take(12)
        val fensAfterEach = fenAfterEachMove(fenBefore, v.moves).take(renderedSans.size)
        val pv = span(cls := "pv-line pv-line--inline")(
          strong(label),
          " ",
          renderedSans.zip(v.moves).zip(fensAfterEach).map { case ((san, uci), fenAfter) =>
            span(cls := "pv-san", attr("data-board") := s"$fenAfter|$uci")(san)
          },
          span(cls := "eval-badge eval-badge--inline")(
            s" (${normalizedScore(v.scoreCp)})"
          )
        )
        labelChar -> pv.toString
      }.toMap

    def renderNormalLine(line: String): String =
      val safeLine = escapeHtml(line)
      val withMoveChips = moveRegex.replaceAllIn(safeLine, m =>
        val san = m.group(1)
        val ref =
          sanQueues.get(san).flatMap(q => Option.when(q.nonEmpty)(q.dequeue())).orElse(sanFallback.get(san))
        val attrs = ref.map { r =>
          val board = s"${r.fenAfter}|${r.uci}"
          s""" data-uci="${escapeHtml(r.uci)}" data-board="${escapeHtml(board)}""""
        }.getOrElse("")
        s"""<span class="move-chip" data-san="$san"$attrs>$san</span>"""
      )
      boldRegex.replaceAllIn(withMoveChips, m => s"<strong>${m.group(1)}</strong>")

    def renderLine(line: String): String =
      line match
        case pvLineRegex(label) =>
          val key = label.toLowerCase.head
          inlineVariationHtml.getOrElse(key, renderNormalLine(line))
        case _ => renderNormalLine(line)

    // Preserve paragraph structure for book-style output.
    val normalized = text.replace("\r\n", "\n")
    val paragraphs = normalized
      .split("\n\n+")
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { p =>
        val renderedLines = p.split("\n").map(renderLine).mkString("<br/>")
        s"<p>$renderedLines</p>"
      }
      .mkString

    raw(paragraphs)

  /**
   * Renders a list of interactive variation lines with mini-board hover support.
   */
  private def renderVariations(vars: List[VariationLine], fen: String): Frag =
    div(cls := "variation-list")(
      vars.zipWithIndex.map { (v, idx) =>
        val label = (idx + 97).toChar.toString + ")"
        val sanList = NarrativeUtils.uciListToSan(fen, v.moves)
        val renderedSans = sanList.take(8)
        val fensAfterEach = fenAfterEachMove(fen, v.moves).take(renderedSans.size)
        
        div(cls := "variation-item")(
          span(cls := "pv-line")(
            strong(label),
            " ",
            renderedSans.zip(v.moves).zip(fensAfterEach).map { case ((san, uci), fenAfter) =>
              span(cls := "pv-san", attr("data-board") := s"$fenAfter|$uci")(san)
            }
          ),
          span(cls := "eval-badge")(
            s" (${normalizedScore(v.scoreCp)})"
          )
        )
      }
    )

  private def fenAfterEachMove(startFen: String, ucis: List[String]): List[String] =
    var current = startFen
    ucis.map { uci =>
      current = NarrativeUtils.uciListToFen(current, List(uci))
      current
    }

  private def normalizedScore(cp: Int): String =
    f"${cp / 100.0}%+.1f"
