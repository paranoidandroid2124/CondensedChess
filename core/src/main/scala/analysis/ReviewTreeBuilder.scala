package chess
package analysis

import AnalysisModel.*

object ReviewTreeBuilder:
  def buildTree(timeline: Vector[PlyOutput], critical: Vector[CriticalNode]): TreeNode =
    val criticalByPly = critical.map(c => c.ply.value -> c).toMap

    // Helper to convert a PV line into a linear TreeNode chain
    def pvToNodeChain(startFen: String, pv: List[String], eval: Double, nodeType: String, label: String): Option[TreeNode] =
      val fullFen = chess.format.Fen.Full.clean(startFen)
      val game = chess.Game(chess.variant.Standard, Some(fullFen))
      
      def rec(moves: List[String], g: chess.Game, currentPly: Int): Option[TreeNode] =
        moves match
          case Nil => None
          case uciStr :: rest =>
            chess.format.Uci(uciStr).flatMap { uci =>
              g.apply(uci) match
                case Right((nextGame, _)) =>
                  val san = nextGame.sans.lastOption.map(_.value).getOrElse(uciStr)
                  val fen = chess.format.Fen.write(nextGame).value
                  val node = TreeNode(
                    ply = currentPly,
                    san = san,
                    uci = uciStr,
                    fen = fen,
                    eval = eval,
                    evalType = "cp",
                    judgement = "variation",
                    glyph = "",
                    tags = List(label),
                    bestMove = None,
                    bestEval = None,
                    pv = rest,
                    comment = None,
                    children = rec(rest, nextGame, currentPly + 1).toList,
                    nodeType = nodeType
                  )
                  Some(node)
                case _ => None
            }

      rec(pv, game, 0) // Ply doesn't matter much for variations unless we track it strictly

    def extractSubtree(plyIndex: Int): Option[TreeNode] =
      if plyIndex >= timeline.length then None
      else
        val p = timeline(plyIndex)
        
        // 1. Mainline Child
        val mainlineChild = extractSubtree(plyIndex + 1).map { child =>
          child.copy(nodeType = "mainline")
        }

        // 2. Variations from Critical Nodes
        val variations = criticalByPly.get(p.ply.value) match
          case Some(c) =>
            c.branches.take(3).flatMap { b =>
              // We need to reconstruct the PV nodes
              // The 'b.move' is the first move of the variation
              val pv = b.move :: b.pv
              pvToNodeChain(p.fenBefore, pv, b.winPct, "critical", b.label)
            }.toList
          case None => Nil

        // Construct current node
        
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

        Some(TreeNode(
          ply = p.ply.value,
          san = p.san,
          uci = p.uci,
          fen = p.fen,
          eval = p.winPctAfterForPlayer,
          evalType = "cp",
          judgement = p.judgement,
          glyph = glyphOf(p.judgement),
          tags = tagsOf(p),
          bestMove = p.evalBeforeDeep.lines.headOption.map(_.move),
          bestEval = p.evalBeforeDeep.lines.headOption.map(_.winPct),
          pv = p.evalBeforeDeep.lines.headOption.map(_.pv).getOrElse(Nil),
          comment = p.shortComment,
          children = mainlineChild.toList ++ variations,
          nodeType = "mainline",
          concepts = Some(p.concepts),
          features = Some(p.features)
        ))

    // The root of the entire game tree is usually the starting position
    // But our recursive function builds from the first move.
    // So we create a virtual root for the start position.
    
    val firstMove = extractSubtree(0)
    
    TreeNode(
      ply = 0,
      san = "Start",
      uci = "",
      fen = timeline.headOption.map(_.fenBefore).getOrElse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
      eval = 50.0,
      evalType = "cp",
      judgement = "book",
      glyph = "",
      tags = Nil,
      bestMove = None,
      bestEval = None,
      pv = Nil,
      comment = Some("Game Start"),
      children = firstMove.toList,
      nodeType = "root"
    )

