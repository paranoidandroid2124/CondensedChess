package chess
package analysis

import AnalysisModel.*
import AnalyzeUtils.pvToSan
@deprecated("Will be replaced by BookBuilder in Phase 4.6", "2024-12-09")
object ReviewTreeBuilder:
  def buildTree(
      timeline: Vector[PlyOutput], 
      critical: Vector[CriticalNode], 
      client: StockfishClient, 
      config: AnalysisModel.EngineConfig
  ): TreeNode =
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
                  // Convert remaining UCI PV to SAN from the updated position
                  val pvSan = pvToSan(fen, rest)

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
                    pv = pvSan,
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

        // 1.5. Hypothesis Branches (Why Not?)
        // Only trigger for critical nodes or mistakes to explain "Why not X?"
        val hypothesisBranches = 
          val bestMoveOpt = p.evalBeforeDeep.lines.headOption
          if (p.mistakeCategory.isDefined || p.judgement == "blunder" || p.judgement == "mistake") && bestMoveOpt.isDefined then
             // Inline hypothesis finding (migrated from HypothesisValidator)
             val candidatesEval = EngineProbe.evalFen(client, p.fenBefore, config.shallowDepth, multiPv = 5, moveTimeMs = Some(300))
             val currentEval = p.winPctBefore.max(bestMoveOpt.get.winPct)
             val candidates = candidatesEval.lines
               .filter(_.move != bestMoveOpt.get.move)
               .filter(l => (currentEval - l.winPct).abs > 15.0)
               .take(2)
             
             candidates.flatMap { line =>
               val label = "Hypothesis"
               val h = (line.move, line.winPct, line.pv, label, (currentEval - line.winPct).abs)
               pvToNodeChain(p.fenBefore, h._1 :: h._3, h._2, "hypothesis", h._4)
             }
          else Nil

        // 2. Variations from Critical Nodes
        // 2. Variations: Critical + Best + Practical
        val criticalVariations = criticalByPly.get(p.ply.value) match
          case Some(c) =>
            c.branches.take(3).flatMap { b =>
              val pv = b.move :: b.pv
              pvToNodeChain(p.fenBefore, pv, b.winPct, "critical", b.label)
            }.toList
          case None => Nil

        // Best Move from Engine (if not played)
        val bestLine = p.evalBeforeDeep.lines.headOption
        val bestVariation = bestLine.filter(_.move != p.uci).flatMap { l =>
          pvToNodeChain(p.fenBefore, l.move :: l.pv, l.winPct, "best", "Best")
        }

        // Practical Move (2nd best, close to best)
        val practicalVariation = p.evalBeforeDeep.lines.drop(1).headOption.filter { l =>
          bestLine.exists(b => (b.winPct - l.winPct).abs < 10.0) && l.move != p.uci
        }.flatMap { l =>
          pvToNodeChain(p.fenBefore, l.move :: l.pv, l.winPct, "practical", "Practical")
        }

        // Merge and Deduplicate by first move (UCI)
        // Order: Critical -> Best -> Practical -> Hypothesis
        val variations = (criticalVariations ++ bestVariation ++ practicalVariation ++ hypothesisBranches)
          .foldLeft(List.empty[TreeNode]) { (acc, node) =>
            if (acc.exists(_.uci == node.uci)) acc else acc :+ node
          }

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

        // Convert Engine PV to SAN
        val bestLinePvSan = p.evalBeforeDeep.lines.headOption.map(_.pv).getOrElse(Nil)

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
          pv = bestLinePvSan,
          comment = p.shortComment,
          children = mainlineChild.toList ++ variations,
          nodeType = "mainline",
          concepts = Some(p.concepts),
          features = Some(p.features),
          practicality = p.practicality
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
