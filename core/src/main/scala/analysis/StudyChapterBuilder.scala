package chess
package analysis

import AnalysisModel.*
import AnalyzeUtils.{ pvToSan, uciToSanSingle }

object StudyChapterBuilder:

  def buildStudyChapters(timeline: Vector[PlyOutput]): Vector[StudyChapter] =
    val candidates = timeline.filter(_.studyScore > 0).sortBy(p => -p.studyScore).take(20)
    val anchors = scala.collection.mutable.ArrayBuffer.empty[PlyOutput]
    candidates.foreach { p =>
      if anchors.forall(a => (a.ply.value - p.ply.value).abs > 2) then anchors += p
    }

    def detectPhase(ply: Int, tags: List[String]): String =
      val hasOpeningTag = tags.exists(t => t.contains("opening") || t.contains("theory"))
      val hasEndgameTag = tags.exists(t => t.contains("endgame") || t.contains("conversion") || t.contains("fortress"))
      if ply <= 15 || hasOpeningTag then "opening"
      else if ply >= 40 || hasEndgameTag then "endgame"
      else "middlegame"

    def narrativeSummary(anchor: PlyOutput, tags: List[String], phase: String, winBefore: Double, winAfter: Double): String =
      val arc = NarrativeTemplates.detectArc(anchor.deltaWinPct)
      val template = NarrativeTemplates.narrativeTemplate(tags, arc)
      template

    def buildTree(anchor: PlyOutput, timeline: Vector[PlyOutput], maxDepth: Int = 4): Option[TreeNode] =
      // Helper to convert a PV line into a linear TreeNode chain
      def pvToNodeChain(startFen: String, pv: List[String], eval: Double, nodeType: String): Option[TreeNode] =
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
                      evalType = "cp", // Assuming cp for now, or pass it down
                      judgement = "variation",
                      glyph = "",
                      tags = List.empty,
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

        rec(pv, game, anchor.ply.value)

      // Recursive function to build the tree from timeline and variations
      def extractSubtree(plyIndex: Int, currentDepth: Int): Option[TreeNode] =
        if currentDepth >= maxDepth || plyIndex >= timeline.length then None
        else
          val p = timeline(plyIndex)
          
          // 1. Mainline Child (Next move in game)
          val mainlineChild = extractSubtree(plyIndex + 1, currentDepth + 1).map { child =>
            child.copy(nodeType = "mainline")
          }

          // 2. Variation Children (Engine Best & Practical)
          // We only add variations at the anchor (depth 0) or maybe depth 1?
          // Let's add them at depth 0 and 1 for now to keep it clean.
          val variations = if currentDepth <= 1 then
            val bestLine = p.evalBeforeDeep.lines.headOption
            val practicalLine = p.evalBeforeDeep.lines.drop(1).headOption.filter { l =>
              bestLine.exists(b => (b.winPct - l.winPct).abs < 5.0)
            }
            
            val playedUci = p.uci
            
            val bestNode = bestLine.filter(_.move != playedUci).flatMap { l =>
              pvToNodeChain(p.fenBefore, l.move :: l.pv, l.cp.getOrElse(0).toDouble, "critical")
            }
            
            val practicalNode = practicalLine.filter(_.move != playedUci).flatMap { l =>
              // If practical is same as best, don't duplicate
              if bestLine.exists(_.move == l.move) then None
              else pvToNodeChain(p.fenBefore, l.move :: l.pv, l.cp.getOrElse(0).toDouble, "sideline")
            }
            
            List(bestNode, practicalNode).flatten
          else
            List.empty

          // Construct current node
          // Note: 'p' is the state *after* the move was made? 
          // No, PlyOutput represents the move *at* that ply.
          // But TreeNode usually represents the node *arrived at*.
          // Wait, the Root of the chapter should be the position *before* the anchor move?
          // The Chapter has 'anchorPly'.
          // If we want the root to be the anchor position, then the children are the moves from there.
          // So we need to construct the node for 'p'.
          // 3. Build Current Node
          Some(TreeNode(
            ply = p.ply.value,
            san = p.san,
            uci = p.uci,
            fen = p.fen,
            eval = p.evalBeforeDeep.lines.headOption.flatMap(_.cp).getOrElse(0).toDouble, // Use deep eval
            evalType = "cp",
            judgement = p.judgement,
            glyph = "", // TODO: Map judgement to glyph
            tags = p.semanticTags,
            bestMove = p.evalBeforeDeep.lines.headOption.map(_.move),
            bestEval = p.evalBeforeDeep.lines.headOption.flatMap(_.cp).map(_.toDouble),
            pv = p.evalBeforeDeep.lines.headOption.map(_.pv).getOrElse(Nil),
            comment = p.shortComment,
            children = mainlineChild.toList ++ variations,
            nodeType = if currentDepth == 0 then "root" else "mainline",
            concepts = Some(p.concepts),
            features = Some(p.features)
          ))

      // The root of the tree should be the node *representing* the anchor move?
      // Or the node *before* the anchor move?
      // Usually a Tree View starts at a position and shows moves *from* it.
      // So we want a Root Node (Position Before) -> Children (Moves).
      // But PlyOutput is "The Move".
      // So we can create a "Virtual Root" for the position before anchor?
      // Or just return the TreeNode corresponding to the Anchor Move, and its siblings?
      // But StudyChapter has 'rootNode'.
      // If 'rootNode' is the Anchor Move, then we can't show alternatives to the anchor move easily 
      // unless the UI treats the root's siblings as alternatives.
      // But TreeNode structure is `children`.
      // So we should probably create a Root Node that represents `fenBefore` of the anchor.
      
      val rootFen = anchor.fenBefore
      val rootEval = anchor.evalBeforeDeep.lines.headOption.flatMap(_.cp).getOrElse(0).toDouble
      
      // We need to build children for this virtual root:
      // 1. The Played Move (Anchor)
      // 2. Alternatives (Best, Practical)
      
      val playedChild = extractSubtree(timeline.indexWhere(_.ply.value == anchor.ply.value), 0)
      
      val bestLine = anchor.evalBeforeDeep.lines.headOption
      val practicalLine = anchor.evalBeforeDeep.lines.drop(1).headOption.filter { l =>
        bestLine.exists(b => (b.winPct - l.winPct).abs < 5.0)
      }
      
      val playedUci = anchor.uci
      
      val bestNode = bestLine.filter(_.move != playedUci).flatMap { l =>
        pvToNodeChain(rootFen, l.move :: l.pv, l.cp.getOrElse(0).toDouble, "critical")
      }
      
      val practicalNode = practicalLine.filter(_.move != playedUci).flatMap { l =>
        if bestLine.exists(_.move == l.move) then None
        else pvToNodeChain(rootFen, l.move :: l.pv, l.cp.getOrElse(0).toDouble, "sideline")
      }
      
      Some(TreeNode(
        ply = anchor.ply.value - 1, // Virtual ply
        san = "...",
        uci = "",
        fen = rootFen,
        eval = rootEval,
        evalType = "cp",
        judgement = "none",
        glyph = "",
        tags = List.empty,
        bestMove = bestLine.map(_.move),
        bestEval = bestLine.flatMap(_.cp).map(_.toDouble),
        pv = List.empty,
        comment = Some("Chapter Start"),
        children = playedChild.toList ++ List(bestNode, practicalNode).flatten,
        nodeType = "root"
      ))

    val chaptersWithPhase = anchors.take(10).map { anchor =>
      val phase = detectPhase(anchor.ply.value, anchor.studyTags)
      (anchor, phase)
    }

    chaptersWithPhase.map { case (anchor, phase) =>
      val id = s"ch-${anchor.ply.value}"
      val bestLine = anchor.evalBeforeDeep.lines.headOption
      val altLine = anchor.evalBeforeDeep.lines.drop(1).headOption
      val playedLine = anchor.evalBeforeDeep.lines.find(_.move == anchor.uci)
      val includePlayed = (for
        played <- playedLine
        best <- bestLine
      yield played.move != best.move && played.winPct < best.winPct).getOrElse(false)
      val lines = scala.collection.mutable.ListBuffer.empty[StudyLine]
      if includePlayed then
        playedLine.foreach { l =>
          lines += StudyLine(label = "played", pv = pvToSan(anchor.fenBefore, l.pv), winPct = l.winPct)
        }
      bestLine.foreach { l =>
        if !playedLine.contains(l) then
          lines += StudyLine(label = "engine", pv = pvToSan(anchor.fenBefore, l.pv), winPct = l.winPct)
      }
      
      val practicalLine = anchor.evalBeforeDeep.lines.drop(1).headOption.filter { l =>
        bestLine.exists(b => (b.winPct - l.winPct).abs < 5.0)
      }

      practicalLine.foreach { l =>
        if !playedLine.contains(l) && !bestLine.contains(l) then
           lines += StudyLine(label = "practical", pv = pvToSan(anchor.fenBefore, l.pv), winPct = l.winPct)
      }

      val enrichedTags = (phase :: anchor.studyTags).take(6)

      val arc = NarrativeTemplates.detectArc(anchor.deltaWinPct)
      val metadata = NarrativeTemplates.buildChapterMetadata(anchor.ply.value, enrichedTags, phase, arc, anchor.studyScore)

      StudyChapter(
        id = id,
        anchorPly = anchor.ply.value,
        fen = anchor.fenBefore,
        played = anchor.san,
        best = bestLine.flatMap(bl => if playedLine.contains(bl) then None else Some(uciToSanSingle(anchor.fenBefore, bl.move))),
        deltaWinPct = anchor.deltaWinPct,
        tags = enrichedTags,
        lines = lines.toList,
        summary = Some(metadata.description),
        studyScore = anchor.studyScore,
        phase = phase,
        winPctBefore = anchor.winPctBefore,
        winPctAfter = anchor.winPctAfterForPlayer,
        practicality = anchor.practicality,
        metadata = Some(metadata),
        rootNode = buildTree(anchor, timeline)
      )
    }.toVector
