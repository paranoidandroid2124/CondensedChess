package chess
package analysis

import AnalysisModel.*

object TreeBuilder:

  /**
   * Reconstructs a full TreeNode tree from the flat timeline and critical variations.
   */
  def buildTree(timeline: Vector[PlyOutput], critical: Vector[CriticalNode]): Option[TreeNode] =
    if timeline.isEmpty then return None

    // Recursive function to build the Mainline + Variations
    val criticalByPly = critical.groupBy(_.ply.value)

    def buildNode(plyIndex: Int): Option[TreeNode] =
      if plyIndex >= timeline.length then None
      else
        val p = timeline(plyIndex)
        val nextMainline = buildNode(plyIndex + 1)
        
        val variations = criticalByPly.get(p.ply.value).map { nodes =>
          nodes.flatMap { c =>
            c.branches.map { branch =>
              buildVariationBranch(p.fenBefore, branch, p.ply.value)
            }
          }
        }.getOrElse(Nil)

        Some(TreeNode(
          ply = p.ply.value,
          san = p.san,
          uci = p.uci,
          fen = p.fen,
          eval = p.evalBeforeDeep.lines.headOption.flatMap(l => l.cp.map(_.toDouble).orElse(l.mate.map(_.toDouble))).getOrElse(p.winPctBefore),
          evalType = p.evalBeforeDeep.lines.headOption.flatMap(l => l.mate.map(_ => "mate").orElse(l.cp.map(_ => "cp"))).getOrElse("win%"),
          judgement = p.judgement,
          glyph = p.judgement match
            case "brilliant" => "!!"
            case "good" | "best" => "!"
            case "mistake" => "?"
            case "blunder" => "??"
            case "inaccuracy" => "?!"
            case _ => ""
          ,
          tags = p.semanticTags,
          bestMove = p.evalBeforeDeep.lines.headOption.map(_.move),
          bestEval = p.evalBeforeDeep.lines.headOption.flatMap(_.cp).map(_.toDouble),
          pv = p.evalBeforeDeep.lines.headOption.map(_.pv).getOrElse(Nil),
          comment = p.shortComment,
          children = nextMainline.toList ++ variations,
          nodeType = "mainline",
          concepts = Some(p.concepts),
          features = Some(p.features)
        ))

    // Helper to build a variation branch from a Branch object
    def buildVariationBranch(startFen: String, branch: Branch, startPly: Int): TreeNode =
      // We need to trace the full PV of the branch
      // Ensure the first move is included if PV doesn't include it.
      // Usually Branch.pv is the continuation *after* Branch.move.
      
      val variationMoves = if branch.pv.headOption.contains(branch.move) then branch.pv else branch.move :: branch.pv
      
      def buildChain(currentFen: String, moves: List[String], currentPly: Int, isFirst: Boolean): Option[TreeNode] =
        moves match
          case Nil => None
          case uci :: rest =>
            val currentFullFen = chess.format.Fen.Full(currentFen)
            val currentFenOpt = Some(currentFullFen)
            val g = chess.Game(chess.variant.Standard, currentFenOpt)
            chess.format.Uci(uci).flatMap { u =>
              g.apply(u).toOption.map { case (nextGame, _) =>
                val san = nextGame.sans.lastOption.map(_.value).getOrElse(uci)
                val fen = chess.format.Fen.write(nextGame).value
                
                TreeNode(
                  ply = currentPly,
                  san = san,
                  uci = uci,
                  fen = fen,
                  eval = branch.cp.map(_.toDouble).orElse(branch.mate.map(_.toDouble)).getOrElse(branch.winPct),
                  evalType = branch.mate.map(_ => "mate").orElse(branch.cp.map(_ => "cp")).getOrElse("win%"),
                  judgement = "variation",
                  glyph = "",
                  tags = List("variation"),
                  bestMove = None,
                  bestEval = None,
                  pv = rest,
                  comment = if isFirst then branch.comment else None,
                  children = buildChain(fen, rest, currentPly + 1, false).toList,
                  nodeType = if isFirst then determineNodeType(branch.label) else "variation"
                )
              }
            }

      buildChain(startFen, variationMoves, startPly, true).getOrElse {
         // Fallback
         TreeNode(startPly, branch.label, branch.move, "", 0.0, "win%", "variation", "", Nil, None, None, Nil, None, Nil)
      }

    def determineNodeType(label: String): String =
      label.toLowerCase match
        case l if l.contains("best") || l.contains("critical") => "critical"
        case l if l.contains("practical") => "sideline"
        case l if l.contains("refutation") || l.contains("bad") => "hypothesis"
        case _ => "sideline"

    // Build the tree starting from the first move of the mainline
    buildNode(0)
