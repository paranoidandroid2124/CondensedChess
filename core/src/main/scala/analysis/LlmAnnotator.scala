package chess
package analysis

import ujson.*
import AnalyzeUtils.{ pvToSan, uciToSanSingle }

import scala.concurrent.{ Future, Await, ExecutionContext }
import scala.concurrent.duration.*
import java.util.concurrent.Executors

/** Lightweight LLM annotator: single-call helpers for summary, mainline short comments,
  * and critical comments. All calls are optional (skip if key missing or failure).
  * Parsing is best-effort; JSON mode 응답을 기대하지만 파싱 실패 시 무시합니다.
  */
object LlmAnnotator:
  private val executor = Executors.newCachedThreadPool()
  private given ExecutionContext = ExecutionContext.fromExecutor(executor)

  private def clampNodes[T](xs: Seq[T], max: Int): Seq[T] = xs.take(max)
  private def pctInt(d: Double): Int = math.round(if d > 1.0 then d else d * 100.0).toInt
  private def round2(d: Double): Double = math.round(d * 10.0) / 10.0
  private def colorName(color: Color): String = if color == Color.White then "White" else "Black"
  private def arrStr(values: Iterable[String]): ujson.Arr = ujson.Arr.from(values.map(ujson.Str(_)))
  private def forcedFlag(legalMoves: Int, gapOpt: Option[Double]): Boolean =
    legalMoves <= 1 || gapOpt.exists(_ >= 20.0)

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
    val result = nums.isEmpty || nums.forall(n => n >= 1 && n <= maxMoveNumber)
    if !result then
      System.err.println(s"[llm-labelSafe] REJECTED text (maxMove=$maxMoveNumber, found=$nums): ${text.take(100)}...")
    result

  private def filterWithLog[A](data: Map[Int, String], maxMoveNumber: Int, label: String): Map[Int, String] =
    data.flatMap { case (k, v) =>
      if labelSafe(v, maxMoveNumber) then Some(k -> v)
      else
        System.err.println(s"[llm-$label] drop comment for ply=$k due to out-of-range move reference")
        None
    }

  def annotate(output: AnalyzePgn.Output): AnalyzePgn.Output =
    val timelineByPly = output.timeline.map(t => t.ply.value -> t).toMap
    
    // Helper to find max ply in tree
    def findMaxPly(node: AnalyzePgn.TreeNode): Int =
      node.children.map(findMaxPly).foldLeft(node.ply)(math.max)

    val maxPly = output.root.map(findMaxPly).getOrElse(output.timeline.lastOption.map(_.ply.value).getOrElse(0))
    val maxMoveNumber = math.max(1, (maxPly + 1) / 2)
    val preview = renderPreview(output, timelineByPly)
    val critPreview = criticalPreview(output, timelineByPly)

    // Parallelize LLM calls with individual recovery
    val summaryFuture = Future {
      System.err.println("[LlmAnnotator] Starting summary generation...")
      val res = LlmClient.summarize(preview).filter(labelSafe(_, maxMoveNumber)).orElse(fallbackSummary(output))
      System.err.println("[LlmAnnotator] Summary generation finished.")
      res
    }.recover { case e: Throwable =>
      System.err.println(s"[LlmAnnotator] Summary failed: ${e.getMessage}")
      fallbackSummary(output)
    }
    
    val criticalFuture = Future {
      System.err.println("[LlmAnnotator] Starting critical comments generation...")
      val res = filterWithLog(LlmClient.criticalComments(critPreview), maxMoveNumber, "critical")
      System.err.println("[LlmAnnotator] Critical comments generation finished.")
      res
    }.recover { case e: Throwable =>
      System.err.println(s"[LlmAnnotator] Critical comments failed: ${e.getMessage}")
      Map.empty[Int, String]
    }

    val studyFuture = Future {
      System.err.println("[LlmAnnotator] Starting study chapter comments generation...")
      val res = if output.studyChapters.nonEmpty then LlmClient.studyChapterComments(preview)
      else Map.empty[String, LlmClient.ChapterAnnotation]
      System.err.println("[LlmAnnotator] Study chapter comments generation finished.")
      res
    }.recover { case e: Throwable =>
      System.err.println(s"[LlmAnnotator] Study comments failed: ${e.getMessage}")
      Map.empty[String, LlmClient.ChapterAnnotation]
    }

    val (summary, criticalComments, studyAnnotations) = 
      try
        Await.result(
          for
            s <- summaryFuture
            c <- criticalFuture
            st <- studyFuture
          yield (s, c, st),
          120.seconds
        )
      catch
        case e: Throwable =>
          System.err.println(s"[LlmAnnotator] Await failed (timeout?): ${e.getMessage}")
          (fallbackSummary(output), Map.empty[Int, String], Map.empty[String, LlmClient.ChapterAnnotation])

    // Merge critical comments with key move comments from chapters
    // Priority: Critical > Chapter Key Move
    // Critical comments are Ply-based (mainline only), so we convert them to (Ply, SAN)
    val criticalCommentsWithSan = criticalComments.flatMap { case (ply, comment) =>
      timelineByPly.get(ply).map(t => (ply, t.san) -> comment)
    }
    
    val chapterMoveComments = studyAnnotations.values.flatMap(_.keyMoves).toMap
    val mergedComments = chapterMoveComments ++ criticalCommentsWithSan // critical overrides if overlap

    val updatedTimeline = output.timeline.map { t =>
      t.copy(shortComment = None) // suppress per-move one-liners
    }
    
    val updatedCritical = output.critical.map { c =>
      val add = criticalComments.get(c.ply.value)
      c.copy(comment = c.comment.orElse(add))
    }
    
    // Apply merged comments to the tree
    val updatedRoot = output.root.map(applyTreeComments(_, mergedComments))

    val updatedChapters =
      if studyAnnotations.nonEmpty then
        output.studyChapters.map { ch =>
          studyAnnotations.get(ch.id) match
            case Some(ann) if ann.summary.nonEmpty => 
              ch.copy(
                summary = Some(ann.summary),
                metadata = Some(AnalysisModel.StudyChapterMetadata(ann.title, ann.summary)),
                rootNode = ch.rootNode.map(applyTreeComments(_, mergedComments))
              )
            case _ => 
              // Even if no specific chapter annotation, we should apply global critical comments
              ch.copy(rootNode = ch.rootNode.map(applyTreeComments(_, mergedComments)))
        }
      else output.studyChapters

    output.copy(
      summaryText = output.summaryText.orElse(summary),
      timeline = updatedTimeline,
      critical = updatedCritical,
      root = updatedRoot,
      studyChapters = updatedChapters
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
        t.phaseLabel.foreach(ph => obj("phaseLabel") = Str(ph))
        t.mistakeCategory.foreach(mc => obj("mistakeCategory") = Str(mc))
        t.special.foreach(s => obj("special") = Str(s))
        
        // Tactical Context Injection
        obj("materialDiff") = Num(round2(t.materialDiff))
        t.bestMaterialDiff.foreach(b => obj("bestMaterialDiff") = Num(round2(b)))
        t.tacticalMotif.foreach(m => obj("tacticalMotif") = Str(m))

        obj
      }

    val criticalNodes = clampNodes(output.critical, 16).map { c =>
      val label = labelForPly(timelineByPly, c.ply.value)
      val branches = c.branches.take(3).map { b =>
        val fenForBranch = timelineByPly.get(c.ply.value).map(_.fenBefore).getOrElse("")
        val sanMove = if fenForBranch.nonEmpty then uciToSanSingle(fenForBranch, b.move) else b.move
        val pvSan = if fenForBranch.nonEmpty then pvToSan(fenForBranch, b.pv).take(6) else b.pv.take(6)
        Obj(
          "label" -> Str(b.label),
          "move" -> Str(sanMove),
          "pv" -> arrStr(pvSan),
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
        t.phaseLabel.foreach(ph => obj("phaseLabel") = Str(ph))
        // Tactical Context Injection (Critical)
        obj("materialDiff") = Num(round2(t.materialDiff))
        t.bestMaterialDiff.foreach(b => obj("bestMaterialDiff") = Num(round2(b)))
        t.tacticalMotif.foreach(m => obj("tacticalMotif") = Str(m))
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
          "Spell out forced/only-move or large best-vs-second gaps first; avoid generic advice.",
          "PRIORITIZE tactical details (Material Loss, Missed Win, tacticalMotif) over abstract tags like 'Pawn Storm'. If tacticalMotif is present, explain IT.",
          "If a line is labeled 'practical', explicitly mention it as a 'Practical Choice' and explain why it might be easier/safer than the engine best.",
          "If 'Plan Change' tag is present, explicitly state: 'Black shifted focus from [Prev Dominant Concept] to [New Concept]' using conceptShift values.",
          "Humanize labels/tags (replace underscores with spaces); do not echo raw snake_case."
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
    if output.studyChapters.nonEmpty then
      val chapters = output.studyChapters.take(12).map { ch =>
        val turn = timelineByPly.get(ch.anchorPly).map(_.turn).getOrElse(if ch.anchorPly % 2 == 1 then Color.White else Color.Black)
        val label = labelForPly(timelineByPly, ch.anchorPly)
        val obj = Obj(
          "id" -> Str(ch.id),
          "anchorPly" -> Num(ch.anchorPly),
          "label" -> Str(label),
          "turn" -> Str(colorName(turn)),
          "played" -> Str(ch.played),
          "deltaWinPct" -> Num(round2(ch.deltaWinPct)),
          "winPctBefore" -> Num(round2(ch.winPctBefore)),
          "winPctAfter" -> Num(round2(ch.winPctAfter)),
          "phase" -> Str(ch.phase),
          "studyScore" -> Num(round2(ch.studyScore)),
          "tags" -> arrStr(ch.tags.take(6))
        )
        ch.best.foreach(b => obj("best") = Str(b))
        if ch.lines.nonEmpty then
          val lines = ch.lines.take(4).map { l =>
            Obj(
              "label" -> Str(l.label),
              "pv" -> arrStr(l.pv.take(6)),
              "winPct" -> Num(round2(l.winPct))
            )
          }
          obj("lines") = Arr.from(lines)
        ch.practicality.foreach { p =>
          obj("practicality") = Obj(
            "overall" -> Num(round2(p.overall)),
            "categoryGlobal" -> Str(p.categoryGlobal),
            "categoryPersonal" -> p.categoryPersonal.map(Str(_)).getOrElse(Null)
          )
        }
        timelineByPly.get(ch.anchorPly).foreach { t =>
          val shiftArr = conceptShifts(t.conceptDelta, limit = 2)
          if shiftArr.value.nonEmpty then obj("conceptShift") = shiftArr
        }
        obj
      }
      payload("studyChapters") = Arr.from(chapters)
    output.timeline.lastOption.foreach { last =>
      payload("finalFrame") = Obj(
        "label" -> Str(moveLabel(last.ply, last.san, last.turn)),
        "evalAfter" -> Num(round2(last.winPctAfterForPlayer)),
        "judgement" -> Str(last.judgement)
      )
    }

    ujson.write(payload)

  private def fallbackSummary(output: AnalyzePgn.Output): Option[String] =
    val topCrit = output.critical.headOption
    val openingName = output.opening.map(_.opening.name.value)
    val openingPart = openingName.map(n => s"Opening: $n.").getOrElse("")
    val critPart = topCrit.map(c => s"Big swing at ply ${c.ply.value}: ${c.reason}.").getOrElse("")
    val endPart = output.timeline.lastOption.map(t => s"Finished at ${t.judgement} after ${t.ply.value} plies.").getOrElse("")
    val text = List(openingPart, critPart, endPart).filter(_.nonEmpty).mkString(" ")
    if text.nonEmpty then Some(text) else None

  private def criticalPreview(output: AnalyzePgn.Output, timelineByPly: Map[Int, AnalyzePgn.PlyOutput]): String =
    val instructions = Obj(
      "goal" -> Str("Return JSON array of {\"ply\":number,\"label\":string,\"comment\":string} covering each critical move."),
      "style" -> Str("Crisp coach notes using the slot format: Heading | Why(delta/gap/forced/conceptShift) | Alternatives(PV labels) | Consequence."),
      "rules" -> Arr.from(
            List(
              "Use provided labels; do not invent ply numbers or moves.",
              "Base severity on deltaWinPct/judgement/mistakeCategory; stay consistent with tags.",
              "Cite branches/pv for refutations without fabricating new lines.",
              "Lead with forced/only-move (legalMoves<=1 or big best-vs-second gap) and conceptShift; do not omit these signals when present.",
              "Humanize labels/tags (replace underscores with spaces); do not echo raw snake_case."
            ).map(Str(_))
          )
        )

    val nodes = clampNodes(output.critical, 16).map { c =>
      val timeline = timelineByPly.get(c.ply.value)
      val label = labelForPly(timelineByPly, c.ply.value)
      val mergedTags = (timeline.map(_.semanticTags).getOrElse(Nil) ++ c.tags).distinct.take(6)
      val branches = c.branches.take(3).map { b =>
        val fenForBranch = timelineByPly.get(c.ply.value).map(_.fenBefore).getOrElse("")
        val sanMove = if fenForBranch.nonEmpty then uciToSanSingle(fenForBranch, b.move) else b.move
        val pvSan = if fenForBranch.nonEmpty then pvToSan(fenForBranch, b.pv).take(6) else b.pv.take(6)
        Obj(
          "label" -> Str(b.label),
          "move" -> Str(sanMove),
          "pv" -> arrStr(pvSan),
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
        t.phaseLabel.foreach(ph => obj("phaseLabel") = Str(ph))
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



  private def applyTreeComments(root: AnalyzePgn.TreeNode, comments: Map[(Int, String), String]): AnalyzePgn.TreeNode =
    root.copy(
      comment = root.comment.orElse(comments.get((root.ply, root.san))),
      children = root.children.map(c => applyTreeComments(c, comments))
    )
  @scala.annotation.nowarn("msg=unused")
  private def conceptShifts(delta: AnalyzePgn.Concepts, limit: Int = 3): ujson.Arr =
    val pairs = List(
      "Drawishness" -> delta.drawish,
      "Dull/Dry Position" -> delta.dry,
      "White Comfortable" -> delta.comfortable,
      "Black Unpleasant" -> delta.unpleasant,
      "King Safety" -> delta.kingSafety,
      "Rook Activity" -> delta.rookActivity,
      "Pawn Storm" -> delta.pawnStorm,
      "Dynamic Sharpness" -> delta.dynamic,
      "Tactical Complexity" -> delta.tacticalDepth,
      "Blunder Risk" -> delta.blunderRisk,
      "Color Complex" -> delta.colorComplex,
      "Good Knight" -> delta.goodKnight,
      "Bad Bishop" -> delta.badBishop,
      "Fortress Potential" -> delta.fortress,
      "Endgame Complexity" -> delta.conversionDifficulty,
      "Long-term Compensation" -> delta.alphaZeroStyle
    )
    // Filter noise: only show shifts >= 0.15
    val top = pairs
      .filter { case (_, v) => math.abs(v) >= 0.15 }
      .sortBy { case (_, v) => -math.abs(v) }
      .take(limit)
    ujson.Arr.from(top.map { case (k, v) => Obj("name" -> Str(k), "delta" -> Num(round2(v))) })
