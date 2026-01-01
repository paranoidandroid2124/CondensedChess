package lila.analyse
package ui

import lila.llm.model.strategic.VariationLine
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.llm.analysis.NarrativeUtils

/**
 * Bookmaker Renderer
 * 
 * Transforms raw LLM text and engine variation data into 
 * interactive HTML fragments for the Lichess AI commentary UI.
 */
object BookmakerRenderer:

  /**
   * Main entry point for rendering a Bookmaker panel.
   * @param commentary The raw text from the LLM.
   * @param variations Structured Multi-PV data.
   * @param fenBefore The FEN of the current position.
   */
  def render(commentary: String, variations: List[VariationLine], fenBefore: String): Frag =
    // Pre-build SAN→UCI mapping from engine-generated variations
    val sanToUciMap: Map[String, String] = variations.flatMap { v =>
      val sanList = NarrativeUtils.uciListToSan(fenBefore, v.moves)
      sanList.zip(v.moves)
    }.toMap
    
    div(cls := "bookmaker-content")(
      div(cls := "commentary")(
        renderTextWithMoves(commentary, sanToUciMap)
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
  private def renderTextWithMoves(text: String, sanToUciMap: Map[String, String]): Frag =
    // Match standard SAN moves like Nf3, e4, O-O, etc.
    val moveRegex = """\b([NBKRQ]?[a-h]x?[a-h]?[1-8][+#]?|O-O(?:-O)?|1\.\s+[a-h][1-8])\b""".r
    val html = moveRegex.replaceAllIn(text, m => 
      val san = m.group(1)
      val uciAttr = sanToUciMap.get(san).map(u => s""" data-uci="$u"""").getOrElse("")
      s"""<span class="move-chip" data-san="$san"$uciAttr>$san</span>"""
    )
    raw(html)

  /**
   * Renders a list of interactive variation lines with mini-board hover support.
   */
  private def renderVariations(vars: List[VariationLine], fen: String): Frag =
    div(cls := "variation-list")(
      vars.zipWithIndex.map { (v, idx) =>
        val label = (idx + 97).toChar.toString + ")"
        val fenAfter = v.resultingFen.getOrElse(NarrativeUtils.uciListToFen(fen, v.moves))
        val sanList = NarrativeUtils.uciListToSan(fen, v.moves)
        val sanText = sanList.take(6).mkString(" ")
        
        // Always show mini-boards from White's perspective for consistency
        val color = chess.White
        val lastMoveUci = v.moves.lastOption
        
        div(cls := "variation-item")(
          span(
            cls := "pv-line mini-board", 
            attr("data-fen") := fenAfter,
            attr("data-color") := color.name,
            lastMoveUci.map(m => attr("data-lastmove") := m).getOrElse(emptyFrag)
          )(
            strong(label),
            " ",
            sanText
          ),
          span(cls := "eval-badge")(
            s" (${normalizedScore(v.scoreCp)})"
          )
        )
      }
    )

  private def normalizedScore(cp: Int): String =
    f"${cp / 100.0}%+.1f"
