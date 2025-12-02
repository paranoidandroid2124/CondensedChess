package chess
package analysis

/** Lightweight LLM annotator: single-call helpers for summary, mainline short comments,
  * and critical comments. All calls are optional (skip if key missing or failure).
  * Parsing is best-effort; JSON mode 응답을 기대하지만 파싱 실패 시 무시합니다.
  */
object LlmAnnotator:
  private def clampNodes[T](xs: Seq[T], max: Int): Seq[T] = xs.take(max)
  private def pctInt(d: Double): Int = math.round(if d > 1.0 then d else d * 100.0).toInt

  private def moveLabel(ply: Ply, san: String, turn: Color): String =
    val moveNumber = (ply.value + 1) / 2
    val sep = if turn == Color.White then "." else "..."
    s"$moveNumber$sep $san"

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
    val maxMoveNumber = math.max(1, (output.timeline.lastOption.map(_.ply.value).getOrElse(1) + 1) / 2)
    val summary = LlmClient.summarize(renderPreview(output)).filter(labelSafe(_, maxMoveNumber))
    // one-line timeline comments are intentionally disabled; focus on structured critical comments instead
    val criticalComments = filterWithLog(LlmClient.criticalComments(criticalPreview(output)), maxMoveNumber, "critical")
    val treeComments = filterWithLog(LlmClient.treeComments(treePreview(output)), maxMoveNumber, "tree")

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

  private def renderPreview(output: AnalyzePgn.Output): String =
    val openingLine =
      output.openingStats
        .map { os =>
          val top = os.topMoves.headOption
            .map { tm =>
              val freq = pctInt(tm.freq)
              val win = pctInt(tm.winPct)
              s"top move ${tm.san} (${freq}% freq, win ${win}%)"
            }
            .getOrElse("no stats")
          s"bookPly=${os.bookPly}, novelty=${os.noveltyPly}, $top"
        }
        .orElse(output.opening.map(op => Some(s"book up to ply ${op.ply.value}")))
        .getOrElse("opening unknown")
    val crit = clampNodes(output.critical, 5).map { c =>
      val turn = if c.ply.value % 2 == 1 then Color.White else Color.Black
      val san = output.timeline.find(_.ply == c.ply).map(_.san).getOrElse("")
      val label = moveLabel(c.ply, san, turn)
      val cat = c.mistakeCategory.map(mc => s"[$mc]").getOrElse("")
      val tagStr = if c.tags.nonEmpty then s" tags=${c.tags.mkString("|")}" else ""
      s"$label$cat: ${c.reason} Δ${"%.2f".format(c.deltaWinPct)}$tagStr"
    }.mkString("; ")
    val swings = output.timeline
      .sortBy(-_.deltaWinPct.abs)
      .take(5)
      .map { t =>
        val cat = t.mistakeCategory.map(mc => s"[$mc]").getOrElse("")
        val tags = if t.semanticTags.nonEmpty then s" tags=${t.semanticTags.take(3).mkString("|")}" else ""
        s"${moveLabel(t.ply, t.san, t.turn)}$cat Δ${"%.2f".format(t.deltaWinPct)} (${t.judgement})$tags"
      }
      .mkString("; ")
    s"Opening=${output.opening.map(_.opening.name.value).getOrElse("unknown")} ($openingLine), swings=$swings, critical=$crit"

  private def criticalPreview(output: AnalyzePgn.Output): String =
    val nodes = clampNodes(output.critical, 6).map { c =>
      val branches = c.branches.take(3).map(b => s"""{"move":"${b.move}","pv":"${b.pv.take(5).mkString(" ")}","win":${"%.2f".format(b.winPct)}}""").mkString(",")
      val tagField = if c.tags.nonEmpty then s""""tags":"${c.tags.mkString(",")}",""" else ""
      val catField = c.mistakeCategory.map(mc => s""""mistakeCategory":"$mc",""").getOrElse("")
      s"""{"ply":${c.ply.value},"reason":"${c.reason}","delta":${"%.2f".format(c.deltaWinPct)},$catField$tagField"branches":[$branches]}"""
    }.mkString(",")
    s"""{"critical":[$nodes]}"""

  private def treePreview(output: AnalyzePgn.Output): String =
    output.root match
      case None => """{"nodes":[]}"""
      case Some(r) =>
        def flatten(n: AnalyzePgn.TreeNode): List[AnalyzePgn.TreeNode] =
          n :: n.children.flatMap(flatten)
        val nodes = flatten(r)
          .filter(_.judgement != "variation")
          .map { n =>
            val tagStr = n.tags.mkString(",")
            s"""{"ply":${n.ply},"san":"${n.san}","judgement":"${n.judgement}","tags":"$tagStr","pv":"${n.pv.take(5).mkString(" ")}"}"""
          }
          .mkString(",")
        s"""{"nodes":[$nodes]}"""

  private def applyTreeComments(root: AnalyzePgn.TreeNode, comments: Map[Int, String]): AnalyzePgn.TreeNode =
    root.copy(
      comment = root.comment.orElse(comments.get(root.ply)),
      children = root.children.map(c => applyTreeComments(c, comments))
    )
