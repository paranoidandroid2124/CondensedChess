package lila.llm

import scala.collection.mutable

/** Encodes SAN/marker tokens into stable placeholders for LLM polish, then decodes back. */
object MoveAnchorCodec:

  final case class EncodedCommentary(
      anchoredText: String,
      refById: Map[String, MoveRefV1],
      evalById: Map[String, String],
      branchById: Map[String, String],
      expectedMoveOrder: List[String],
      expectedMarkerOrder: List[String],
      expectedEvalOrder: List[String],
      expectedBranchOrder: List[String]
  ):
    def hasAnchors: Boolean =
      expectedMoveOrder.nonEmpty ||
        expectedMarkerOrder.nonEmpty ||
        expectedEvalOrder.nonEmpty ||
        expectedBranchOrder.nonEmpty

  private val sanTokenRegex =
    """(?<![A-Za-z0-9])((?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?)(?:[+#])?(?:[!?]{1,2})?)""".r
  private val trailingMarkerRegex = """(?s)(.*?)(\d+(?:\.\.\.|\.))(\s*)$""".r
  private val evalTokenRegex =
    """(?i)\(\s*(?:[+-]?\d+(?:\.\d+)?|#?[+-]?\d+|mate\s+in\s+\d+|mated\s+in\s+\d+)\s*\)""".r
  private val branchLabelRegex = """(?m)^(\s*)([a-z]\))(?=\s+)""".r
  private val moveAnchorRegex = """\[\[MV_([A-Za-z0-9_\-]+)\]\]""".r
  private val markerAnchorRegex = """\[\[MK_([A-Za-z0-9_\-]+)\]\]""".r
  private val evalAnchorRegex = """\[\[EV_([A-Za-z0-9_\-]+)\]\]""".r
  private val branchAnchorRegex = """\[\[VB_([A-Za-z0-9_\-]+)\]\]""".r
  private val fullAnchorRegex = """\[\[(?:MV|MK|EV|VB)_[A-Za-z0-9_\-]+\]\]""".r

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

  private def anchorMoveAndMarkerTokens(
      text: String,
      refs: Option[BookmakerRefsV1]
  ): (String, Map[String, MoveRefV1], List[String], List[String]) =
    refs match
      case None => (text, Map.empty, Nil, Nil)
      case Some(r) if r.variations.isEmpty => (text, Map.empty, Nil, Nil)
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
        (sb.toString, refById, moveIds.result(), markerIds.result())

  private def anchorEvalTokens(text: String): (String, Map[String, String], List[String]) =
    val evalById = mutable.LinkedHashMap.empty[String, String]
    val order = List.newBuilder[String]
    var idx = 0
    val anchored = evalTokenRegex.replaceAllIn(
      Option(text).getOrElse(""),
      m =>
        idx += 1
        val id = f"e$idx%03d"
        evalById.update(id, m.group(0))
        order += id
        s"[[EV_$id]]"
    )
    (anchored, evalById.toMap, order.result())

  private def anchorBranchLabels(text: String): (String, Map[String, String], List[String]) =
    val branchById = mutable.LinkedHashMap.empty[String, String]
    val order = List.newBuilder[String]
    var idx = 0
    val anchored = branchLabelRegex.replaceAllIn(
      Option(text).getOrElse(""),
      m =>
        idx += 1
        val id = f"b$idx%03d"
        val label = m.group(2)
        branchById.update(id, label)
        order += id
        s"${m.group(1)}[[VB_$id]]"
    )
    (anchored, branchById.toMap, order.result())

  def encode(text: String, refs: Option[BookmakerRefsV1]): EncodedCommentary =
    val (moveAnchored, refById, moveOrder, markerOrder) = anchorMoveAndMarkerTokens(text, refs)
    val (evalAnchored, evalById, evalOrder) = anchorEvalTokens(moveAnchored)
    val (fullyAnchored, branchById, branchOrder) = anchorBranchLabels(evalAnchored)
    EncodedCommentary(
      anchoredText = fullyAnchored,
      refById = refById,
      evalById = evalById,
      branchById = branchById,
      expectedMoveOrder = moveOrder,
      expectedMarkerOrder = markerOrder,
      expectedEvalOrder = evalOrder,
      expectedBranchOrder = branchOrder
    )

  def decode(
      text: String,
      refById: Map[String, MoveRefV1],
      evalById: Map[String, String] = Map.empty,
      branchById: Map[String, String] = Map.empty
  ): String =
    val withBranches = branchAnchorRegex.replaceAllIn(
      Option(text).getOrElse(""),
      m => branchById.getOrElse(m.group(1), "")
    )
    val withEval = evalAnchorRegex.replaceAllIn(
      withBranches,
      m => evalById.getOrElse(m.group(1), "")
    )
    val withMarkers = markerAnchorRegex.replaceAllIn(
      withEval,
      m => refById.get(m.group(1)).flatMap(_.marker).getOrElse("")
    )
    moveAnchorRegex.replaceAllIn(
      withMarkers,
      m => refById.get(m.group(1)).map(_.san).getOrElse("")
    )

  def hasBrokenAnchorPrefix(text: String): Boolean =
    val t = Option(text).getOrElse("")
    val residue = fullAnchorRegex.replaceAllIn(t, "")
    residue.contains("[[MV_") || residue.contains("[[MK_") || residue.contains("[[EV_") || residue.contains("[[VB_") ||
      residue.contains("[[MV") || residue.contains("[[MK") || residue.contains("[[EV") || residue.contains("[[VB")

  def validateAnchors(
      text: String,
      expectedMoveOrder: List[String],
      expectedMarkerOrder: List[String],
      expectedEvalOrder: List[String] = Nil,
      expectedBranchOrder: List[String] = Nil
  ): List[String] =
    val hasExpected =
      expectedMoveOrder.nonEmpty ||
        expectedMarkerOrder.nonEmpty ||
        expectedEvalOrder.nonEmpty ||
        expectedBranchOrder.nonEmpty
    if !hasExpected then Nil
    else
      val actualMoves = moveAnchorRegex.findAllMatchIn(Option(text).getOrElse("")).map(_.group(1)).toList
      val actualMarkers = markerAnchorRegex.findAllMatchIn(Option(text).getOrElse("")).map(_.group(1)).toList
      val actualEval = evalAnchorRegex.findAllMatchIn(Option(text).getOrElse("")).map(_.group(1)).toList
      val actualBranch = branchAnchorRegex.findAllMatchIn(Option(text).getOrElse("")).map(_.group(1)).toList
      val reasons = List.newBuilder[String]
      if hasBrokenAnchorPrefix(text) then reasons += "truncated_output"

      def check(expected: List[String], actual: List[String]): Unit =
        if expected.nonEmpty then
          val expectedSet = expected.toSet
          val actualFiltered = actual.filter(expectedSet.contains)
          if actualFiltered.size < expected.size then reasons += "anchor_missing"
          if !isSubsequence(expected, actualFiltered) then reasons += "anchor_order_violation"

      check(expectedMoveOrder, actualMoves)
      check(expectedMarkerOrder, actualMarkers)
      check(expectedEvalOrder, actualEval)
      check(expectedBranchOrder, actualBranch)

      reasons.result().distinct
