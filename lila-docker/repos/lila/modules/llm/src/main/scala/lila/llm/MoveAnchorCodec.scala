package lila.llm

import scala.collection.mutable

/** Encodes SAN/marker tokens into stable placeholders for LLM polish, then decodes back. */
object MoveAnchorCodec:

  final case class EncodedCommentary(
      anchoredText: String,
      refById: Map[String, MoveRefV1],
      expectedMoveOrder: List[String],
      expectedMarkerOrder: List[String]
  ):
    def hasAnchors: Boolean = expectedMoveOrder.nonEmpty

  private val sanTokenRegex =
    """(?<![A-Za-z0-9])((?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?)(?:[+#])?(?:[!?]{1,2})?)""".r
  private val trailingMarkerRegex = """(?s)(.*?)(\d+(?:\.\.\.|\.))(\s*)$""".r
  private val moveAnchorRegex = """\[\[MV_([A-Za-z0-9_\-]+)\]\]""".r
  private val markerAnchorRegex = """\[\[MK_([A-Za-z0-9_\-]+)\]\]""".r
  private val fullAnchorRegex = """\[\[(?:MV|MK)_[A-Za-z0-9_\-]+\]\]""".r

  private def canonicalSan(san: String): String =
    Option(san).getOrElse("").trim
      .replaceAll("""[!?]+$""", "")
      .replaceAll("""[+#]+$""", "")

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

  private def asQueues(refs: List[MoveRefV1]): mutable.Map[String, mutable.Queue[MoveRefV1]] =
    mutable.Map.from:
      refs
        .groupBy(m => canonicalSan(m.san))
        .view
        .mapValues(xs => mutable.Queue.from(xs))
        .toMap

  def encode(text: String, refs: Option[BookmakerRefsV1]): EncodedCommentary =
    refs match
      case None => EncodedCommentary(text, Map.empty, Nil, Nil)
      case Some(r) if r.variations.isEmpty => EncodedCommentary(text, Map.empty, Nil, Nil)
      case Some(r) =>
        val flatRefs = r.variations.flatMap(_.moves)
        val refById = flatRefs.map(m => m.refId -> m).toMap
        val queues = asQueues(flatRefs)
        val moveIds = List.newBuilder[String]
        val markerIds = List.newBuilder[String]
        val src = Option(text).getOrElse("")
        val sb = new StringBuilder()
        var last = 0
        sanTokenRegex.findAllMatchIn(src).foreach { m =>
          val san = canonicalSan(m.group(1))
          val refOpt = queues.get(san).flatMap(q => Option.when(q.nonEmpty)(q.dequeue()))
          refOpt match
            case None =>
              sb.append(src.substring(last, m.end))
            case Some(ref) =>
              val before = src.substring(last, m.start)
              val (prefix, markerAnchored) =
                trailingMarkerRegex.findFirstMatchIn(before) match
                  case Some(mm) if ref.marker.exists(_.trim == mm.group(2)) =>
                    val ws = if mm.group(3).nonEmpty then mm.group(3) else " "
                    markerIds += ref.refId
                    (s"${mm.group(1)}[[MK_${ref.refId}]]$ws", true)
                  case _ => (before, false)
              sb.append(prefix)
              if markerAnchored || ref.marker.isEmpty then
                ()
              sb.append(s"[[MV_${ref.refId}]]")
              moveIds += ref.refId
          last = m.end
        }
        sb.append(src.substring(last))
        EncodedCommentary(
          anchoredText = sb.toString,
          refById = refById,
          expectedMoveOrder = moveIds.result(),
          expectedMarkerOrder = markerIds.result()
        )

  def decode(text: String, refById: Map[String, MoveRefV1]): String =
    val withMarkers = markerAnchorRegex.replaceAllIn(
      Option(text).getOrElse(""),
      m => refById.get(m.group(1)).flatMap(_.marker).getOrElse("")
    )
    moveAnchorRegex.replaceAllIn(
      withMarkers,
      m => refById.get(m.group(1)).map(_.san).getOrElse("")
    )

  def hasBrokenAnchorPrefix(text: String): Boolean =
    val t = Option(text).getOrElse("")
    val residue = fullAnchorRegex.replaceAllIn(t, "")
    residue.contains("[[MV_") || residue.contains("[[MK_") || residue.contains("[[MV") || residue.contains("[[MK")

  def validateAnchors(
      text: String,
      expectedMoveOrder: List[String],
      expectedMarkerOrder: List[String]
  ): List[String] =
    if expectedMoveOrder.isEmpty then Nil
    else
      val actualMoves = moveAnchorRegex.findAllMatchIn(Option(text).getOrElse("")).map(_.group(1)).toList
      val actualMarkers = markerAnchorRegex.findAllMatchIn(Option(text).getOrElse("")).map(_.group(1)).toList
      val reasons = List.newBuilder[String]
      if hasBrokenAnchorPrefix(text) then reasons += "truncated_output"

      val moveSet = expectedMoveOrder.toSet
      val actualMovesFiltered = actualMoves.filter(moveSet.contains)
      if actualMovesFiltered.size < expectedMoveOrder.size then reasons += "anchor_missing"
      if !isSubsequence(expectedMoveOrder, actualMovesFiltered) then reasons += "anchor_order_violation"

      if expectedMarkerOrder.nonEmpty then
        val markerSet = expectedMarkerOrder.toSet
        val actualMarkerFiltered = actualMarkers.filter(markerSet.contains)
        if actualMarkerFiltered.size < expectedMarkerOrder.size then reasons += "anchor_missing"
        if !isSubsequence(expectedMarkerOrder, actualMarkerFiltered) then reasons += "anchor_order_violation"

      reasons.result().distinct
