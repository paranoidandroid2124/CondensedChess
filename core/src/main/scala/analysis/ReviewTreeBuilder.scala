package chess
package analysis

import AnalyzeDomain.*
import AnalyzeUtils.{ pvToSan, uciToSanSingle }

object ReviewTreeBuilder:
  def buildTree(timeline: Vector[PlyOutput], critical: Vector[CriticalNode]): TreeNode =
    val criticalByPly = critical.map(c => c.ply.value -> c).toMap
    def glyphOf(j: String): String = j match
      case "blunder" => "??"
      case "mistake" => "?"
      case "inaccuracy" => "?!"
      case "good" | "best" => "!"
      case "book" => "="
      case _ => ""

    def tagsOf(p: PlyOutput): List[String] =
      (List(p.judgement) ++ p.special.toList ++ p.mistakeCategory.toList ++ p.semanticTags)
        .filter(_.nonEmpty)

    val mainNodes = timeline.map { t =>
      val bestLine = t.evalBeforeDeep.lines.headOption
      val bestMove = bestLine.map(_.move)
      val bestEval = bestLine.map(_.winPct)
      val pvSan = bestLine.map(bl => pvToSan(t.fenBefore, bl.pv)).getOrElse(Nil)
      TreeNode(
        ply = t.ply.value,
        san = t.san,
        uci = t.uci,
        fen = t.fen,
        eval = t.winPctAfterForPlayer,
        evalType = "cp",
        judgement = t.judgement,
        glyph = glyphOf(t.judgement),
        tags = tagsOf(t),
        bestMove = bestMove,
        bestEval = bestEval,
        pv = pvSan,
        comment = t.shortComment,
        children = Nil
      )
    }

    def attachVariations(node: TreeNode, fenBefore: String): TreeNode =
      criticalByPly.get(node.ply) match
        case None => node
        case Some(c) =>
          val vars = c.branches.take(3).map { b =>
            val pvSan = pvToSan(fenBefore, b.pv)
            val sanMove = uciToSanSingle(fenBefore, b.move)
            val fallbackComment =
              c.comment.orElse {
                val reason = c.reason
                val delta = f"${b.winPct}%.1f"
                Some(s"Line ${b.label}: ${sanMove} (${delta}% win). ${reason}")
              }
            TreeNode(
              ply = node.ply,
              san = sanMove,
              uci = b.move,
              fen = node.fen,
              eval = b.winPct,
              evalType = "cp",
              judgement = "variation",
              glyph = "",
              tags = List("variation", c.reason),
              bestMove = None,
              bestEval = None,
              pv = pvSan,
              comment = fallbackComment,
              children = Nil
            )
          }
          node.copy(children = vars)

    val nodesWithVars = mainNodes.zip(timeline).map { case (n, t) => attachVariations(n, t.fenBefore) }
    val root = nodesWithVars.headOption.getOrElse(
      TreeNode(0, "start", "", "", 50.0, "cp", "book", "", Nil, None, None, Nil, None, Nil)
    )
    nodesWithVars.drop(1).foldLeft(root) { (acc, n) => acc.copy(children = acc.children :+ n) }
