package chess
package analysis

import ujson.*

/** Lightweight LLM annotator: single-call helpers for summary, mainline short comments,
  * and critical comments. All calls are optional (skip if key missing or failure).
  * Parsing is best-effort; JSON mode 응답을 기대하지만 파싱 실패 시 무시합니다.
  */
object LlmAnnotator:
  private def clampNodes[T](xs: Seq[T], max: Int): Seq[T] = xs.take(max)
  private def pctInt(d: Double): Int = math.round(if d > 1.0 then d else d * 100.0).toInt
  private def round2(d: Double): Double = math.round(d * 100.0) / 100.0
  private def colorName(color: Color): String = if color == Color.White then "White" else "Black"
  private def arrStr(values: Iterable[String]): ujson.Arr = ujson.Arr.from(values.map(ujson.Str(_)))
  private def forcedFlag(legalMoves: Int, gapOpt: Option[Double]): Boolean =
    legalMoves <= 1 || gapOpt.exists(_ >= 20.0)

  private def conceptShifts(delta: AnalyzePgn.Concepts, limit: Int = 2): ujson.Arr =
    val pairs = List(
      "kingSafety" -> delta.kingSafety,
      "rookActivity" -> delta.rookActivity,
      "pawnStorm" -> delta.pawnStorm,
      "dynamic" -> delta.dynamic,
      "tacticalDepth" -> delta.tacticalDepth,
      "blunderRisk" -> delta.blunderRisk,
      "colorComplex" -> delta.colorComplex,
      "goodKnight" -> delta.goodKnight,
      "badBishop" -> delta.badBishop,
      "fortress" -> delta.fortress,
      "conversionDifficulty" -> delta.conversionDifficulty,
      "alphaZeroStyle" -> delta.alphaZeroStyle
    )
    val top = pairs.sortBy { case (_, v) => -math.abs(v) }.take(limit)
    ujson.Arr.from(top.map { case (k, v) => Obj("name" -> Str(k), "delta" -> Num(round2(v))) })

  private def moveLabel(ply: Ply, san: String, turn: Color): String =
    val moveNumber = (ply.value + 1) / 2
    val sep = if turn == Color.White then "." else "..."
    s"$moveNumber$sep $san"

  private def labelForPly(timelineByPly: Map[Int, AnalyzePgn.PlyOutput], ply: Int): String =
    timelineByPly
      .get(ply)
      .map(t => moveLabel(t.ply, t.san, t.turn))
      .getOrElse(s"ply $ply")

  private def labelSafe(text: String, maxMoveNumber: Int): Boolean =
    // 허용 범위를 벗어난 수 번호가 등장하면 폐기
    val movePattern = "(\\d+)\\.{1,3}".r
    val nums = movePattern.findAllMatchIn(text).flatMap(m => m.group(1).toIntOption).toList
    nums.isEmpty || nums.forall(n => n >= 1 && n <= maxMoveNumber)

  private def filterWithLog[A](data: Map[Int, String], maxMoveNumber: Int, label: String): Map[Int, String] =
    data.flatMap { case (k, v) =>
      if labelSafe(v, maxMoveNumber) then Some(k -> v)
      else
        System.err.println(s"[llm-$label] drop comment for ply=$k due to out-of-range move reference")
        None
    }

  def annotate(output: AnalyzePgn.Output): AnalyzePgn.Output =
    val timelineByPly = output.timeline.map(t => t.ply.value -> t).toMap
    val maxMoveNumber = math.max(1, (output.timeline.lastOption.map(_.ply.value).getOrElse(1) + 1) / 2)
    val summary = LlmClient.summarize(renderPreview(output, timelineByPly)).filter(labelSafe(_, maxMoveNumber))
    // one-line timeline comments are intentionally disabled; focus on structured critical comments instead
    val criticalComments = filterWithLog(LlmClient.criticalComments(criticalPreview(output, timelineByPly)), maxMoveNumber, "critical")
    val treeComments = filterWithLog(LlmClient.treeComments(treePreview(output, timelineByPly)), maxMoveNumber, "tree")

    val updatedTimeline = output.timeline.map { t =>
      t.copy(shortComment = None) // suppress per-move one-liners
    }
    val criticalMap = criticalComments
    val updatedCritical = output.critical.map { c =>
      val add = criticalMap.get(c.ply.value)
      c.copy(comment = c.comment.orElse(add))
    }
    val updatedRoot = output.root.map(applyTreeComments(_, treeComments))

    output.copy(
      summaryText = output.summaryText.orElse(summary),
      timeline = updatedTimeline,
      critical = updatedCritical,
      root = updatedRoot
    )

  private def renderPreview(output: AnalyzePgn.Output, timelineByPly: Map[Int, AnalyzePgn.PlyOutput]): String =
    val openingObj = Obj("name" -> Str(output.opening.map(_.opening.name.value).getOrElse("unknown")))
    output.opening.foreach(op => openingObj("bookToPly") = Num(op.ply.value))
    output.openingStats.foreach { os =>
      val stats = Obj(
        "bookPly" -> Num(os.bookPly),
        "noveltyPly" -> Num(os.noveltyPly),
        "source" -> Str(os.source)
      )
      os.games.foreach(g => stats("games") = Num(g))
      os.winWhite.foreach(w => stats("winWhite") = Num(pctInt(w)))
      os.winBlack.foreach(b => stats("winBlack") = Num(pctInt(b)))
      os.draw.foreach(d => stats("draw") = Num(pctInt(d)))
      os.minYear.foreach(y => stats("minYear") = Num(y))
      os.maxYear.foreach(y => stats("maxYear") = Num(y))
      if os.yearBuckets.nonEmpty then
        val buckets = os.yearBuckets.map { case (k, v) => k -> Num(v) }
        stats("yearBuckets") = Obj.from(buckets)
      os.topMoves.headOption.foreach { tm =>
        val tmObj = Obj("san" -> Str(tm.san), "games" -> Num(tm.games))
        tm.winPct.foreach(w => tmObj("winPct") = Num(pctInt(w)))
        tm.drawPct.foreach(d => tmObj("drawPct") = Num(pctInt(d)))
        stats("topMove") = tmObj
      }
      openingObj("stats") = stats
    }
    output.openingSummary.foreach(s => openingObj("summary") = Str(s))
    output.bookExitComment.foreach(s => openingObj("bookExitComment") = Str(s))
    output.openingTrend.foreach(s => openingObj("trend") = Str(s))

    val topSwings = output.timeline
      .sortBy(t => -math.abs(t.deltaWinPct))
      .take(4)
      .map { t =>
        val obj = Obj(
          "ply" -> Num(t.ply.value),
          "label" -> Str(moveLabel(t.ply, t.san, t.turn)),
          "turn" -> Str(colorName(t.turn)),
          "judgement" -> Str(t.judgement),
          "deltaWinPct" -> Num(round2(t.deltaWinPct)),
          "winBefore" -> Num(round2(t.winPctBefore)),
          "winAfter" -> Num(round2(t.winPctAfterForPlayer)),
          "epLoss" -> Num(round2(t.epLoss)),
          "semanticTags" -> arrStr(t.semanticTags.take(6)),
          "legalMoves" -> Num(t.legalMoves),
          "forced" -> Bool(forcedFlag(t.legalMoves, t.bestVsSecondGap))
        )
        t.bestVsSecondGap.foreach(g => obj("bestVsSecondGap") = Num(round2(g)))
        t.bestVsPlayedGap.foreach(g => obj("bestVsPlayedGap") = Num(round2(g)))
        val shiftArr = conceptShifts(t.conceptDelta, limit = 2)
        if shiftArr.value.nonEmpty then obj("conceptShift") = shiftArr
        t.mistakeCategory.foreach(mc => obj("mistakeCategory") = Str(mc))
        t.special.foreach(s => obj("special") = Str(s))
        obj
      }

    val criticalNodes = clampNodes(output.critical, 4).map { c =>
      val label = labelForPly(timelineByPly, c.ply.value)
      val branches = c.branches.take(3).map { b =>
        Obj(
          "label" -> Str(b.label),
          "move" -> Str(b.move),
          "pv" -> arrStr(b.pv.take(6)),
          "winPct" -> Num(round2(b.winPct))
        )
      }
      val obj = Obj(
        "ply" -> Num(c.ply.value),
        "label" -> Str(label),
        "reason" -> Str(c.reason),
        "deltaWinPct" -> Num(round2(c.deltaWinPct)),
        "branches" -> Arr.from(branches)
      )
      timelineByPly.get(c.ply.value).foreach { t =>
        obj("winBefore") = Num(round2(t.winPctBefore))
        obj("winAfter") = Num(round2(t.winPctAfterForPlayer))
        obj("epLoss") = Num(round2(t.epLoss))
        obj("legalMoves") = Num(t.legalMoves)
        obj("forced") = Bool(c.forced || forcedFlag(t.legalMoves, c.bestVsSecondGap.orElse(t.bestVsSecondGap)))
        t.bestVsSecondGap.foreach(g => obj("bestVsSecondGap") = Num(round2(g)))
        t.bestVsPlayedGap.foreach(g => obj("bestVsPlayedGap") = Num(round2(g)))
        val shiftArr = conceptShifts(t.conceptDelta, limit = 2)
        if shiftArr.value.nonEmpty then obj("conceptShift") = shiftArr
      }
      c.bestVsSecondGap.foreach(g => obj("bestVsSecondGap") = Num(round2(g)))
      c.bestVsPlayedGap.foreach(g => obj("bestVsPlayedGap") = Num(round2(g)))
      c.legalMoves.foreach(lm => obj("legalMoves") = Num(lm))
      obj("forced") = Bool(c.forced || c.legalMoves.exists(_ <= 1))
      if c.tags.nonEmpty then obj("tags") = arrStr(c.tags.take(6))
      c.mistakeCategory.foreach(mc => obj("mistakeCategory") = Str(mc))
      obj
    }

    val instructions = Obj(
      "goal" -> Str("Coach-style 3-4 sentence recap grounded in the provided review data."),
      "style" -> Str("Narrative and concise: describe what could change, what was chosen, and consequences; no inventions."),
      "rules" -> Arr.from(
        List(
          "Follow the instructions block plus rules; use only provided data.",
          "Reference move labels exactly (e.g., \"13. Qe2\", \"12...Ba6\"); never renumber.",
          "Lean on semanticTags/mistakeCategory/conceptShift/critical.reason to explain why evaluations shifted.",
          "Mention opening/book/novelty briefly when stats are present; if opening.summary/bookExitComment/trend is present, include one sentence.",
          "Spell out forced/only-move or large best-vs-second gaps first; avoid generic advice."
        ).map(Str(_))
      )
    )

    val payload = Obj(
      "instructions" -> instructions,
      "opening" -> openingObj,
      "oppositeColorBishops" -> Bool(output.oppositeColorBishops),
      "timelineSize" -> Num(output.timeline.size),
      "keySwings" -> Arr.from(topSwings),
      "critical" -> Arr.from(criticalNodes)
    )
    output.timeline.lastOption.foreach { last =>
      payload("finalFrame") = Obj(
        "label" -> Str(moveLabel(last.ply, last.san, last.turn)),
        "evalAfter" -> Num(round2(last.winPctAfterForPlayer)),
        "judgement" -> Str(last.judgement)
      )
    }

    ujson.write(payload)

  private def criticalPreview(output: AnalyzePgn.Output, timelineByPly: Map[Int, AnalyzePgn.PlyOutput]): String =
    val instructions = Obj(
      "goal" -> Str("Return JSON array of {\"ply\":number,\"label\":string,\"comment\":string} covering each critical move."),
      "style" -> Str("Crisp coach notes using the slot format: Heading | Why(delta/gap/forced/conceptShift) | Alternatives(PV labels) | Consequence."),
      "rules" -> Arr.from(
        List(
          "Use provided labels; do not invent ply numbers or moves.",
          "Base severity on deltaWinPct/judgement/mistakeCategory; stay consistent with tags.",
          "Cite branches/pv for refutations without fabricating new lines.",
          "Lead with forced/only-move (legalMoves<=1 or big best-vs-second gap) and conceptShift; do not omit these signals when present."
        ).map(Str(_))
      )
    )

    val nodes = clampNodes(output.critical, 4).map { c =>
      val timeline = timelineByPly.get(c.ply.value)
      val label = labelForPly(timelineByPly, c.ply.value)
      val mergedTags = (timeline.map(_.semanticTags).getOrElse(Nil) ++ c.tags).distinct.take(6)
      val branches = c.branches.take(3).map { b =>
        Obj(
          "label" -> Str(b.label),
          "move" -> Str(b.move),
          "pv" -> arrStr(b.pv.take(6)),
          "winPct" -> Num(round2(b.winPct))
        )
      }
      val obj = Obj(
        "ply" -> Num(c.ply.value),
        "label" -> Str(label),
        "reason" -> Str(c.reason),
        "deltaWinPct" -> Num(round2(c.deltaWinPct)),
        "branches" -> Arr.from(branches)
      )
      timeline.foreach { t =>
        obj("turn") = Str(colorName(t.turn))
        obj("judgement") = Str(t.judgement)
        obj("winBefore") = Num(round2(t.winPctBefore))
        obj("winAfter") = Num(round2(t.winPctAfterForPlayer))
        obj("epLoss") = Num(round2(t.epLoss))
        obj("legalMoves") = Num(t.legalMoves)
        obj("forced") = Bool(c.forced || forcedFlag(t.legalMoves, c.bestVsSecondGap.orElse(t.bestVsSecondGap)))
        t.bestVsSecondGap.foreach(g => obj("bestVsSecondGap") = Num(round2(g)))
        t.bestVsPlayedGap.foreach(g => obj("bestVsPlayedGap") = Num(round2(g)))
        val shiftArr = conceptShifts(t.conceptDelta, limit = 2)
        if shiftArr.value.nonEmpty then obj("conceptShift") = shiftArr
      }
      if mergedTags.nonEmpty then obj("semanticTags") = arrStr(mergedTags)
      c.mistakeCategory.orElse(timeline.flatMap(_.mistakeCategory)).foreach(mc => obj("mistakeCategory") = Str(mc))
      c.bestVsSecondGap.foreach(g => obj("bestVsSecondGap") = Num(round2(g)))
      c.bestVsPlayedGap.foreach(g => obj("bestVsPlayedGap") = Num(round2(g)))
      c.legalMoves.foreach(lm => obj("legalMoves") = Num(lm))
      obj("forced") = Bool(c.forced || c.legalMoves.exists(_ <= 1))
      obj
    }

    val payload = Obj(
      "instructions" -> instructions,
      "critical" -> Arr.from(nodes)
    )

    ujson.write(payload)

  private def treePreview(output: AnalyzePgn.Output, timelineByPly: Map[Int, AnalyzePgn.PlyOutput]): String =
    output.root match
      case None =>
        ujson.write(Obj("instructions" -> Obj("goal" -> Str("no tree")), "nodes" -> Arr()))
      case Some(r) =>
        def flatten(n: AnalyzePgn.TreeNode): List[AnalyzePgn.TreeNode] =
          n :: n.children.flatMap(flatten)

        val nodes = flatten(r)
          .filter(_.judgement != "variation")
          .map { n =>
            val turn = timelineByPly.get(n.ply).map(_.turn).getOrElse(if n.ply % 2 == 1 then Color.White else Color.Black)
            val obj = Obj(
              "ply" -> Num(n.ply),
              "label" -> Str(moveLabel(Ply(n.ply), n.san, turn)),
              "judgement" -> Str(n.judgement),
              "glyph" -> Str(n.glyph),
              "tags" -> arrStr(n.tags.filter(_.nonEmpty).take(6)),
              "pv" -> arrStr(n.pv.take(5))
            )
            timelineByPly.get(n.ply).foreach { t =>
              obj("legalMoves") = Num(t.legalMoves)
              obj("forced") = Bool(forcedFlag(t.legalMoves, t.bestVsSecondGap))
              val shiftArr = conceptShifts(t.conceptDelta, limit = 2)
              if shiftArr.value.nonEmpty then obj("conceptShift") = shiftArr
            }
            n.bestMove.foreach(b => obj("bestMove") = Str(b))
            n.bestEval.foreach(ev => obj("bestEval") = Num(round2(ev)))
            obj
          }

        val instructions = Obj(
          "goal" -> Str("Return JSON array of {\"ply\":number,\"label\":string,\"comment\":string} to annotate mainline nodes."),
          "style" -> Str("One sentence per node highlighting plan/idea/outcome: what was tried, why it mattered, and any forced follow-up."),
          "rules" -> Arr.from(
            List(
              "Use provided labels; do not renumber or invent lines.",
              "Anchor to judgement/tags/pv/bestMove/conceptShift instead of generic advice.",
              "Call out forced/only-move or big best-vs-second gaps first when present."
            ).map(Str(_))
          )
        )

        val payload = Obj(
          "instructions" -> instructions,
          "nodes" -> Arr.from(nodes)
        )

        ujson.write(payload)

  private def applyTreeComments(root: AnalyzePgn.TreeNode, comments: Map[Int, String]): AnalyzePgn.TreeNode =
    root.copy(
      comment = root.comment.orElse(comments.get(root.ply)),
      children = root.children.map(c => applyTreeComments(c, comments))
    )
