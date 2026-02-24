package lila.analyse
package ui

import lila.llm.model.strategic.VariationLine
import lila.llm.BookmakerRefsV1
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

  private case class MoveBoardRef(refId: Option[String], uci: String, fenAfter: String)
  private case class NumberedSan(prefix: Option[String], san: String)

  /** Use scalalib's escapeHtmlRaw for consistency */
  private def escapeHtml(text: String): String = escapeHtmlRaw(text)
  private def canonicalSan(san: String): String =
    Option(san).getOrElse("").trim
      .replaceAll("""[!?]+$""", "")
      .replaceAll("""[+#]+$""", "")

  private def numberSans(startPly: Int, sans: List[String]): List[NumberedSan] =
    var ply = startPly
    var lastMoveNo: Option[Int] = None
    var lastWasWhite = false
    sans.map { san =>
      val moveNo = (ply + 1) / 2
      val prefix =
        if (ply % 2 == 1) Some(s"$moveNo.")
        else if (lastWasWhite && lastMoveNo.contains(moveNo)) None
        else Some(s"$moveNo...")
      lastMoveNo = Some(moveNo)
      lastWasWhite = (ply % 2 == 1)
      ply += 1
      NumberedSan(prefix = prefix, san = san)
    }

  private def renderNumberedSanSequence(
      startPly: Int,
      sans: List[String],
      ucis: List[String],
      fensAfterEach: List[String],
      refIds: List[Option[String]] = Nil
  ): List[Frag] =
    val numbered = numberSans(startPly, sans)
    val size =
      if refIds.nonEmpty then List(numbered.size, ucis.size, fensAfterEach.size, refIds.size).min
      else List(numbered.size, ucis.size, fensAfterEach.size).min
    (0 until size).toList.flatMap { idx =>
      val item = numbered(idx)
      val uci = ucis(idx)
      val fenAfter = fensAfterEach(idx)
      val refId = if refIds.nonEmpty then refIds(idx) else None
      val prefixFrags: List[Frag] = item.prefix.toList.flatMap { p =>
        List[Frag](span(cls := "pv-move-no")(p), raw(" "))
      }
      val attrs = List[Modifier](
        cls := "pv-san move-chip--interactive",
        attr("data-board") := s"$fenAfter|$uci",
        attr("tabindex") := "0",
        attr("role") := "button",
        attr("aria-label") := s"Preview move ${item.san}"
      ) ++ refId.map(id => attr("data-ref-id") := id)
      val moveFrag: Frag =
        span(attrs*)(item.san)
      val sep: List[Frag] = Option.when(idx < size - 1)(raw(" ")).toList
      prefixFrags ++ List(moveFrag) ++ sep
    }

  /**
   * Main entry point for rendering a Bookmaker panel.
   * @param commentary The raw text from the LLM.
   * @param variations Structured Multi-PV data.
   */
  def render(commentary: String, variations: List[VariationLine], fenBefore: String, refs: Option[BookmakerRefsV1] = None): Frag =
    // Preserve SAN occurrence order across variations to keep move-chip mapping stable.
    val refsFromContract: List[(String, MoveBoardRef)] =
      refs.toList
        .flatMap(_.variations)
        .flatMap(_.moves)
        .map(m => m.san -> MoveBoardRef(refId = Some(m.refId), uci = m.uci, fenAfter = m.fenAfter))
    val sanRefs: List[(String, MoveBoardRef)] =
      if refsFromContract.nonEmpty then refsFromContract
      else
        variations.flatMap { v =>
          val sanList = NarrativeUtils.uciListToSan(fenBefore, v.moves)
          val fensAfter = fenAfterEachMove(fenBefore, v.moves)
          sanList.zip(v.moves).zip(fensAfter).map { case ((san, uci), fenAfter) =>
            san -> MoveBoardRef(refId = None, uci = uci, fenAfter = fenAfter)
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
        renderTextWithMoves(commentary, sanRefs, variations, fenBefore, refs)
      ),
      variations.nonEmpty.option:
        div(cls := "alternatives")(
          h3("Alternative Options"),
          renderVariations(variations, fenBefore, refs)
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
      fenBefore: String,
      refs: Option[BookmakerRefsV1]
  ): Frag =
    // Support Markdown-style emphasis used by the LLM prompt (**...**).
    val boldRegex = """\*\*(.+?)\*\*""".r

    // Match SAN moves including check/mate and annotation markers.
    val moveRegex =
      """(?<![A-Za-z0-9])((?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?)(?:[+#])?(?:[!?]{1,2})?)""".r

    // Detect LLM "alternative" lines like: a) Bb5 ...  b) Bc4 ...  c) d4 ...
    // When possible, replace them with the structured engine variations so each move can drive a mini-board hover preview.
    val pvLineRegex = """^\s*([a-z])\)\s+.*$""".r
    val startPly = NarrativeUtils.plyFromFen(fenBefore).map(_ + 1).getOrElse(1)

    val sanQueues: mutable.Map[String, mutable.Queue[MoveBoardRef]] =
      mutable.Map.from:
        sanRefs
          .groupBy((san, _) => canonicalSan(san))
          .view
          .mapValues(entries => mutable.Queue.from(entries.map(_._2)))
          .toMap

    val sanFallback: Map[String, MoveBoardRef] =
      sanRefs.groupBy((san, _) => canonicalSan(san)).view.mapValues(_.head._2).toMap

    val inlineVariationHtml: Map[Char, String] =
      variations.zipWithIndex.map { (v, idx) =>
        val labelChar = (idx + 97).toChar
        val label = s"$labelChar)"
        val refLine = refs.flatMap(_.variations.lift(idx)).map(_.moves).getOrElse(Nil)
        val hasRefLine = refLine.nonEmpty
        val renderedSans =
          if hasRefLine then refLine.take(12).map(_.san)
          else NarrativeUtils.uciListToSan(fenBefore, v.moves).take(12)
        val renderedMoves =
          if hasRefLine then refLine.take(renderedSans.size).map(_.uci)
          else v.moves.take(renderedSans.size)
        val fensAfterEach =
          if hasRefLine then refLine.take(renderedSans.size).map(_.fenAfter)
          else fenAfterEachMove(fenBefore, renderedMoves).take(renderedSans.size)
        val refIds =
          if hasRefLine then refLine.take(renderedSans.size).map(m => Option(m.refId))
          else Nil
        val numberedSans = renderNumberedSanSequence(startPly, renderedSans, renderedMoves, fensAfterEach, refIds)
        val pv = span(cls := "pv-line pv-line--inline")(
          strong(label),
          " ",
          numberedSans,
          span(cls := "eval-badge eval-badge--inline")(
            s" (${normalizedScore(v.scoreCp)})"
          )
        )
        labelChar -> pv.toString
      }.toMap

    def renderNormalLine(line: String): String =
      val safeLine = escapeHtml(line)
      val withMoveChips = moveRegex.replaceAllIn(safeLine, m =>
        val shown = m.group(1)
        val san = canonicalSan(shown)
        val ref =
          sanQueues.get(san).flatMap(q => Option.when(q.nonEmpty)(q.dequeue())).orElse(sanFallback.get(san))
        val attrs = ref.map { r =>
          val board = s"${r.fenAfter}|${r.uci}"
          val refAttr = r.refId.map(id => s" data-ref-id=\"${escapeHtml(id)}\"").getOrElse("")
          s" data-uci=\"${escapeHtml(r.uci)}\" data-board=\"${escapeHtml(board)}\"$refAttr tabindex=\"0\" role=\"button\" aria-label=\"Play move ${escapeHtml(shown)}\""
        }.getOrElse("")
        val klass = if (ref.isDefined) "move-chip move-chip--interactive" else "move-chip"
        s"""<span class="$klass" data-san="${escapeHtml(san)}"$attrs>$shown</span>"""
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
  private def renderVariations(vars: List[VariationLine], fen: String, refs: Option[BookmakerRefsV1]): Frag =
    val startPly = NarrativeUtils.plyFromFen(fen).map(_ + 1).getOrElse(1)
    div(cls := "variation-list", attr("data-variation-swipe") := "true")(
      vars.zipWithIndex.map { (v, idx) =>
        val label = (idx + 97).toChar.toString + ")"
        val numberedSans =
          refs
            .flatMap(_.variations.lift(idx))
            .map(_.moves)
            .filter(_.nonEmpty)
            .map { moves =>
              val rendered = moves.take(12)
              val renderedSans = rendered.map(_.san)
              val renderedMoves = rendered.map(_.uci)
              val fensAfterEach = rendered.map(_.fenAfter)
              val refIds = rendered.map(m => Option(m.refId))
              renderNumberedSanSequence(startPly, renderedSans, renderedMoves, fensAfterEach, refIds)
            }
            .getOrElse {
              val sanList = NarrativeUtils.uciListToSan(fen, v.moves)
              val renderedSans = sanList.take(12)
              val renderedMoves = v.moves.take(renderedSans.size)
              val fensAfterEach = fenAfterEachMove(fen, renderedMoves).take(renderedSans.size)
              renderNumberedSanSequence(startPly, renderedSans, renderedMoves, fensAfterEach)
            }
        
        div(cls := "variation-item", attr("data-variation-index") := idx.toString)(
          span(cls := "pv-line")(
            strong(label),
            " ",
            numberedSans
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
