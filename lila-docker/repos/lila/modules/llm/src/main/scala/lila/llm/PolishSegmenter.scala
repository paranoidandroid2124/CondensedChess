package lila.llm

/** Splits commentary into sentence-like chunks and marks structurally safe chunks as editable.
  * Structural chunks (SAN/eval/variation labels/move markers/anchors) are locked.
  */
object PolishSegmenter:

  final case class Segment(id: String, text: String, editable: Boolean)

  final case class MaskedSegment(
      maskedText: String,
      tokenById: Map[String, String],
      expectedOrder: List[String]
  ):
    def hasLocks: Boolean = expectedOrder.nonEmpty

  final case class Segmentation(segments: Vector[Segment]):
    def editableSegments: Vector[Segment] = segments.filter(_.editable)

    def merge(rewrites: Map[String, String]): String =
      segments
        .map { s =>
          if s.editable then rewrites.getOrElse(s.id, s.text)
          else s.text
        }
        .mkString

  private val sentenceBoundaryRegex = java.util.regex.Pattern.compile("(?<=[.!?])\\s+|\\n+")
  private val moveTokenRegex =
    """(?<![A-Za-z0-9])((?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?)(?:[+#])?(?:[!?]{1,2})?)""".r
  private val moveMarkerRegex =
    """(?<![A-Za-z0-9])(\d+)(\.\.\.|\.|)(?=\s*(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?(?:[+#])?(?:[!?]{1,2})?))""".r
  private val evalTokenRegex =
    """(?i)\(\s*(?:[+-]?\d+(?:\.\d+)?|#?[+-]?\d+|mate\s+in\s+\d+|mated\s+in\s+\d+)\s*\)""".r
  private val variationBranchRegex = """(?m)^\s*([a-z]\))""".r
  private val anchorRegex = """\[\[(?:MV|MK|EV|VB)_[A-Za-z0-9_\-]+\]\]""".r
  private val uciRegex = """(?<![A-Za-z0-9])[a-h][1-8][a-h][1-8][qrbn]?(?![A-Za-z0-9])""".r
  private val lockAnchorRegex = """\[\[LK_([0-9]{3})\]\]""".r
  private final case class TokenSpan(start: Int, end: Int, token: String, priority: Int)

  private def containsProtectedTokens(chunk: String): Boolean =
    val t = Option(chunk).getOrElse("")
    moveTokenRegex.findFirstIn(t).nonEmpty ||
    moveMarkerRegex.findFirstIn(t).nonEmpty ||
    evalTokenRegex.findFirstIn(t).nonEmpty ||
    variationBranchRegex.findFirstIn(t).nonEmpty ||
    anchorRegex.findFirstIn(t).nonEmpty ||
    uciRegex.findFirstIn(t).nonEmpty

  private def shouldEdit(chunk: String): Boolean =
    val core = Option(chunk).getOrElse("").trim
    if core.isEmpty then false
    else
      val words = core.split("\\s+").count(_.nonEmpty)
      if words < 3 then false
      else if !containsProtectedTokens(core) then true
      else
        val masked = maskStructuralTokens(core).maskedText
        val proseOnly = lockAnchorRegex.replaceAllIn(masked, " ")
        val proseWords = proseOnly.split("\\s+").count(w => w.exists(_.isLetter))
        proseWords >= 2

  def segment(text: String): Segmentation =
    val src = Option(text).getOrElse("")
    if src.isEmpty then Segmentation(Vector.empty)
    else
      val matcher = sentenceBoundaryRegex.matcher(src)
      val chunks = Vector.newBuilder[String]
      var start = 0
      while matcher.find() do
        chunks += src.substring(start, matcher.end())
        start = matcher.end()
      if start < src.length then chunks += src.substring(start)

      val result = chunks.result().zipWithIndex.map { case (chunk, i) =>
        Segment(
          id = f"s${i + 1}%04d",
          text = chunk,
          editable = shouldEdit(chunk)
        )
      }
      Segmentation(result)

  def maskStructuralTokens(text: String): MaskedSegment =
    val tokenById = scala.collection.mutable.LinkedHashMap.empty[String, String]
    val expectedOrder = List.newBuilder[String]
    val src = Option(text).getOrElse("")
    if src.isEmpty then MaskedSegment("", Map.empty, Nil)
    else
      val spans = Vector.newBuilder[TokenSpan]

      anchorRegex.findAllMatchIn(src).foreach(m =>
        spans += TokenSpan(m.start, m.end, m.group(0), priority = 0)
      )
      evalTokenRegex.findAllMatchIn(src).foreach(m =>
        spans += TokenSpan(m.start, m.end, m.group(0), priority = 1)
      )
      variationBranchRegex.findAllMatchIn(src).foreach(m =>
        spans += TokenSpan(m.start(1), m.end(1), m.group(1), priority = 2)
      )
      moveMarkerRegex.findAllMatchIn(src).foreach(m =>
        spans += TokenSpan(m.start, m.end, m.group(0), priority = 3)
      )
      moveTokenRegex.findAllMatchIn(src).foreach(m =>
        spans += TokenSpan(m.start(1), m.end(1), m.group(1), priority = 4)
      )
      uciRegex.findAllMatchIn(src).foreach(m =>
        spans += TokenSpan(m.start, m.end, m.group(0), priority = 5)
      )

      val sorted = spans.result().sortBy(s => (s.start, s.priority, -(s.end - s.start)))
      val accepted = Vector.newBuilder[TokenSpan]
      var cursor = 0
      sorted.foreach { span =>
        if span.start >= cursor then
          accepted += span
          cursor = span.end
      }

      var idx = 0

      def lockToken(token: String): String =
        idx += 1
        val id = f"t$idx%03d"
        tokenById.update(id, token)
        expectedOrder += id
        s"[[LK_${id.drop(1)}]]"

      val sb = new StringBuilder(src.length + 32)
      var pos = 0
      accepted.result().foreach { span =>
        if span.start > pos then sb.append(src.substring(pos, span.start))
        sb.append(lockToken(span.token))
        pos = span.end
      }
      if pos < src.length then sb.append(src.substring(pos))

      MaskedSegment(sb.toString, tokenById.toMap, expectedOrder.result())

  def unmask(maskedText: String, tokenById: Map[String, String]): String =
    lockAnchorRegex.replaceAllIn(
      Option(maskedText).getOrElse(""),
      m => tokenById.getOrElse(s"t${m.group(1)}", "")
    )

  def validateLocks(text: String, expectedOrder: List[String]): List[String] =
    if expectedOrder.isEmpty then Nil
    else
      val actual = lockAnchorRegex.findAllMatchIn(Option(text).getOrElse("")).map(m => s"t${m.group(1)}").toList
      val reasons = List.newBuilder[String]
      if actual.size < expectedOrder.size then reasons += "anchor_missing"
      if !isSubsequence(expectedOrder, actual) then reasons += "anchor_order_violation"
      reasons.result().distinct

  private def isSubsequence[T](xs: List[T], ys: List[T]): Boolean =
    if xs.isEmpty then true
    else
      @annotation.tailrec
      def loop(restXs: List[T], restYs: List[T]): Boolean =
        (restXs, restYs) match
          case (Nil, _) => true
          case (_, Nil) => false
          case (xh :: xt, yh :: yt) =>
            if xh == yh then loop(xt, yt) else loop(restXs, yt)
      loop(xs, ys)
