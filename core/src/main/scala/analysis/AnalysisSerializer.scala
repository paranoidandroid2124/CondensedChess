package chess
package analysis

import AnalysisModel.*
import AnalyzeUtils.escape

object AnalysisSerializer:

  def render(output: Output): String =
    val sb = new StringBuilder(256 + output.timeline.size * 128)
    sb.append('{')
    output.summaryText.foreach { s =>
      sb.append("\"summaryText\":\"").append(escape(s)).append("\",")
    }
    output.root.foreach { r =>
      sb.append("\"root\":")
      renderTree(sb, r)
      sb.append(',')
    }
    if output.studyChapters.nonEmpty then
      sb.append("\"studyChapters\":[")
      output.studyChapters.zipWithIndex.foreach { case (ch, idx) =>
        if idx > 0 then sb.append(',')
        renderStudyChapter(sb, ch)
      }
      sb.append("],")
    output.opening.foreach { op =>
      sb.append("\"opening\":{")
      sb.append("\"name\":\"").append(escape(op.opening.name.value)).append("\",")
      sb.append("\"eco\":\"").append(escape(op.opening.eco.value)).append("\",")
      sb.append("\"ply\":").append(op.ply.value)
      sb.append("},")
    }
    output.openingStats.foreach { os =>
      sb.append("\"openingStats\":")
      renderOpeningStats(sb, os)
      sb.append(',')
    }
    sb.append("\"oppositeColorBishops\":").append(output.oppositeColorBishops).append(',')
    output.openingSummary.foreach { s =>
      sb.append("\"openingSummary\":\"").append(escape(s)).append("\",")
    }
    output.bookExitComment.foreach { s =>
      sb.append("\"bookExitComment\":\"").append(escape(s)).append("\",")
    }
    output.openingTrend.foreach { s =>
      sb.append("\"openingTrend\":\"").append(escape(s)).append("\",")
    }
    sb.append("\"critical\":[")
    output.critical.zipWithIndex.foreach { case (c, idx) =>
      if idx > 0 then sb.append(',')
      renderCritical(sb, c)
    }
    sb.append("],")
    if output.pgn.nonEmpty then
      sb.append("\"pgn\":\"").append(escape(output.pgn)).append("\",")
    output.accuracyWhite.foreach { acc =>
      sb.append("\"accuracyWhite\":").append(fmt(acc)).append(",")
    }
    output.accuracyBlack.foreach { acc =>
      sb.append("\"accuracyBlack\":").append(fmt(acc)).append(",")
    }
    sb.append("\"timeline\":[")
    output.timeline.zipWithIndex.foreach { case (ply, idx) =>
      if idx > 0 then sb.append(',')
      sb.append('{')
      sb.append("\"ply\":").append(ply.ply.value).append(',')
      sb.append("\"turn\":\"").append(ply.turn.name).append("\",")
      sb.append("\"san\":\"").append(escape(ply.san)).append("\",")
      sb.append("\"uci\":\"").append(escape(ply.uci)).append("\",")
      sb.append("\"fen\":\"").append(escape(ply.fen)).append("\",")
      sb.append("\"fenBefore\":\"").append(escape(ply.fenBefore)).append("\",")
      sb.append("\"legalMoves\":").append(ply.legalMoves).append(',')
      renderFeatures(sb, ply.features)
      sb.append(',')
      renderEval(sb, "evalBeforeShallow", ply.evalBeforeShallow)
      sb.append(',')
      renderEval(sb, "evalBeforeDeep", ply.evalBeforeDeep)
      sb.append(',')
      sb.append("\"winPctBefore\":").append(fmt(ply.winPctBefore)).append(',')
      sb.append("\"winPctAfterForPlayer\":").append(fmt(ply.winPctAfterForPlayer)).append(',')
      sb.append("\"deltaWinPct\":").append(fmt(ply.deltaWinPct)).append(',')
      sb.append("\"epBefore\":").append(fmt(ply.epBefore)).append(',')
      sb.append("\"epAfter\":").append(fmt(ply.epAfter)).append(',')
      sb.append("\"epLoss\":").append(fmt(ply.epLoss)).append(',')
      sb.append("\"judgement\":\"").append(ply.judgement).append('"')
      ply.bestVsSecondGap.foreach { gap =>
        sb.append(",\"bestVsSecondGap\":").append(fmt(gap))
      }
      ply.bestVsPlayedGap.foreach { gap =>
        sb.append(",\"bestVsPlayedGap\":").append(fmt(gap))
      }
      ply.special.foreach { s =>
        sb.append(",\"special\":\"").append(escape(s)).append('"')
      }
      ply.mistakeCategory.foreach { cat =>
        sb.append(",\"mistakeCategory\":\"").append(escape(cat)).append('"')
      }
      ply.phaseLabel.foreach { phase =>
        sb.append(",\"phaseLabel\":\"").append(escape(phase)).append('"')
      }
      sb.append(",\"phase\":\"").append(escape(ply.phase)).append('"')
      if ply.semanticTags.nonEmpty then
        sb.append(",\"semanticTags\":[")
        ply.semanticTags.zipWithIndex.foreach { case (t, i) =>
          if i > 0 then sb.append(',')
          sb.append("\"").append(escape(t)).append('"')
        }
        sb.append(']')
      ply.shortComment.foreach { txt =>
        sb.append(",\"shortComment\":\"").append(escape(txt)).append('"')
      }
      if ply.studyTags.nonEmpty then
        sb.append(",\"studyTags\":[")
        ply.studyTags.zipWithIndex.foreach { case (t, i) =>
          if i > 0 then sb.append(',')
          sb.append("\"").append(escape(t)).append('"')
        }
        sb.append(']')
      sb.append(",\"studyScore\":").append(fmt(ply.studyScore))
      ply.practicality.foreach { p =>
        sb.append(",\"practicality\":{")
        sb.append("\"overall\":").append(fmt(p.overall)).append(',')
        sb.append("\"robustness\":").append(fmt(p.robustness)).append(',')
        sb.append("\"horizon\":").append(fmt(p.horizon)).append(',')
        sb.append("\"naturalness\":").append(fmt(p.naturalness)).append(',')
        sb.append("\"categoryGlobal\":\"").append(escape(p.categoryGlobal)).append("\",")
        p.categoryPersonal.foreach(cp => sb.append("\"categoryPersonal\":\"").append(escape(cp)).append("\","))
        // Backward compatibility
        sb.append("\"category\":\"").append(escape(p.categoryGlobal)).append('"')
        sb.append('}')
      }
      sb.append(',')
      renderConcepts(sb, "concepts", ply.concepts)
      sb.append(',')
      renderConcepts(sb, "conceptsBefore", ply.conceptsBefore)
      sb.append(',')
      renderConcepts(sb, "conceptDelta", ply.conceptDelta)
      sb.append('}')
    }
    sb.append("]}")
    sb.result()

  def escapeJson(in: String): String = escape(in)

  private def renderFeatures(sb: StringBuilder, f: FeatureExtractor.SideFeatures): Unit =
    sb.append("\"features\":{")
    sb.append("\"pawnIslands\":").append(f.pawnIslands).append(',')
    sb.append("\"isolatedPawns\":").append(f.isolatedPawns).append(',')
    sb.append("\"doubledPawns\":").append(f.doubledPawns).append(',')
    sb.append("\"passedPawns\":").append(f.passedPawns).append(',')
    sb.append("\"rookOpenFiles\":").append(f.rookOpenFiles).append(',')
    sb.append("\"rookSemiOpenFiles\":").append(f.rookSemiOpenFiles).append(',')
    sb.append("\"bishopPair\":").append(f.bishopPair).append(',')
    sb.append("\"kingRingPressure\":").append(f.kingRingPressure).append(',')
    sb.append("\"spaceControl\":").append(f.spaceControl)
    sb.append('}')

  private def fmt(d: Double): String = f"$d%.2f"
  private def pct(d: Double): Double = if d > 1.0 then d else d * 100.0

  private def renderConcepts(sb: StringBuilder, key: String, c: Concepts): Unit =
    sb.append('"').append(key).append("\":{")
    sb.append("\"dynamic\":").append(fmt(c.dynamic)).append(',')
    sb.append("\"drawish\":").append(fmt(c.drawish)).append(',')
    sb.append("\"imbalanced\":").append(fmt(c.imbalanced)).append(',')
    sb.append("\"tacticalDepth\":").append(fmt(c.tacticalDepth)).append(',')
    sb.append("\"blunderRisk\":").append(fmt(c.blunderRisk)).append(',')
    sb.append("\"pawnStorm\":").append(fmt(c.pawnStorm)).append(',')
    sb.append("\"fortress\":").append(fmt(c.fortress)).append(',')
    sb.append("\"colorComplex\":").append(fmt(c.colorComplex)).append(',')
    sb.append("\"badBishop\":").append(fmt(c.badBishop)).append(',')
    sb.append("\"goodKnight\":").append(fmt(c.goodKnight)).append(',')
    sb.append("\"rookActivity\":").append(fmt(c.rookActivity)).append(',')
    sb.append("\"kingSafety\":").append(fmt(c.kingSafety)).append(',')
    sb.append("\"dry\":").append(fmt(c.dry)).append(',')
    sb.append("\"comfortable\":").append(fmt(c.comfortable)).append(',')
    sb.append("\"unpleasant\":").append(fmt(c.unpleasant)).append(',')
    sb.append("\"engineLike\":").append(fmt(c.engineLike)).append(',')
    sb.append("\"conversionDifficulty\":").append(fmt(c.conversionDifficulty)).append(',')
    sb.append("\"sacrificeQuality\":").append(fmt(c.sacrificeQuality)).append(',')
    sb.append("\"alphaZeroStyle\":").append(fmt(c.alphaZeroStyle))
    sb.append('}')

  private def renderOpeningStats(sb: StringBuilder, os: OpeningExplorer.Stats): Unit =
    sb.append('{')
    sb.append("\"bookPly\":").append(os.bookPly).append(',')
    sb.append("\"noveltyPly\":").append(os.noveltyPly).append(',')
    os.games.foreach(g => sb.append("\"games\":").append(g).append(','))
    os.winWhite.foreach(w => sb.append("\"winWhite\":").append(fmt(pct(w))).append(','))
    os.winBlack.foreach(w => sb.append("\"winBlack\":").append(fmt(pct(w))).append(','))
    os.draw.foreach(d => sb.append("\"draw\":").append(fmt(pct(d))).append(','))
    os.minYear.foreach(y => sb.append("\"minYear\":").append(y).append(','))
    os.maxYear.foreach(y => sb.append("\"maxYear\":").append(y).append(','))
    if os.yearBuckets.nonEmpty then
      sb.append("\"yearBuckets\":{")
      os.yearBuckets.zipWithIndex.foreach { case ((k, v), idx) =>
        if idx > 0 then sb.append(',')
        sb.append("\"").append(escape(k)).append("\":").append(v)
      }
      sb.append("},")
    if os.topMoves.nonEmpty then
      sb.append("\"topMoves\":[")
      os.topMoves.zipWithIndex.foreach { case (m, idx) =>
        if idx > 0 then sb.append(',')
        sb.append('{')
        sb.append("\"san\":\"").append(escape(m.san)).append("\",")
        sb.append("\"uci\":\"").append(escape(m.uci)).append("\",")
        sb.append("\"games\":").append(m.games)
        m.winPct.foreach(w => sb.append(',').append("\"winPct\":").append(fmt(pct(w))))
        m.drawPct.foreach(d => sb.append(',').append("\"drawPct\":").append(fmt(pct(d))))
        sb.append('}')
      }
      sb.append("],")
    if os.topGames.nonEmpty then
      sb.append("\"topGames\":[")
      os.topGames.zipWithIndex.foreach { case (g, idx) =>
        if idx > 0 then sb.append(',')
        sb.append('{')
        sb.append("\"white\":\"").append(escape(g.white)).append("\",")
        sb.append("\"black\":\"").append(escape(g.black)).append("\",")
        g.whiteElo.foreach(e => sb.append("\"whiteElo\":").append(e).append(','))
        g.blackElo.foreach(e => sb.append("\"blackElo\":").append(e).append(','))
        sb.append("\"result\":\"").append(escape(g.result)).append("\",")
        g.date.foreach(d => sb.append("\"date\":\"").append(escape(d)).append("\","))
        g.event.foreach(e => sb.append("\"event\":\"").append(escape(e)).append("\","))
        if sb.length() > 0 && sb.charAt(sb.length - 1) == ',' then sb.setLength(sb.length - 1)
        sb.append('}')
      }
      sb.append("],")
    sb.append("\"source\":\"").append(escape(os.source)).append('"')
    sb.append('}')

  private def renderEval(sb: StringBuilder, key: String, eval: EngineEval): Unit =
    sb.append('"').append(key).append("\":{")
    sb.append("\"depth\":").append(eval.depth).append(',')
    sb.append("\"lines\":[")
    eval.lines.zipWithIndex.foreach { case (l, idx) =>
      if idx > 0 then sb.append(',')
      sb.append('{')
      sb.append("\"move\":\"").append(escape(l.move)).append("\",")
      sb.append("\"winPct\":").append(fmt(l.winPct)).append(',')
      l.cp.foreach(cp => sb.append("\"cp\":").append(cp).append(','))
      l.mate.foreach(m => sb.append("\"mate\":").append(m).append(','))
      sb.append("\"pv\":[")
      l.pv.zipWithIndex.foreach { case (m, i) =>
        if i > 0 then sb.append(',')
        sb.append("\"").append(escape(m)).append("\"")
      }
      sb.append("]}")
    }
    sb.append("]}")

  private def renderCritical(sb: StringBuilder, c: CriticalNode): Unit =
    sb.append('{')
    sb.append("\"ply\":").append(c.ply.value).append(',')
    sb.append("\"reason\":\"").append(escape(c.reason)).append("\",")
    sb.append("\"deltaWinPct\":").append(fmt(c.deltaWinPct)).append(',')
    c.bestVsSecondGap.foreach { g =>
      sb.append("\"bestVsSecondGap\":").append(fmt(g)).append(',')
    }
    c.bestVsPlayedGap.foreach { g =>
      sb.append("\"bestVsPlayedGap\":").append(fmt(g)).append(',')
    }
    c.legalMoves.foreach { lm =>
      sb.append("\"legalMoves\":").append(lm).append(',')
    }
    sb.append("\"forced\":").append(c.forced).append(',')
    c.mistakeCategory.foreach { cat =>
      sb.append("\"mistakeCategory\":\"").append(escape(cat)).append("\",")
    }
    if c.tags.nonEmpty then
      sb.append("\"tags\":[")
      c.tags.zipWithIndex.foreach { case (t, idx) =>
        if idx > 0 then sb.append(',')
        sb.append("\"").append(escape(t)).append('"')
      }
      sb.append("],")
    c.comment.foreach { txt =>
      sb.append("\"comment\":\"").append(escape(txt)).append("\",")
    }
    c.practicality.foreach { p =>
      sb.append("\"practicality\":{")
      sb.append("\"overall\":").append(fmt(p.overall)).append(',')
      sb.append("\"robustness\":").append(fmt(p.robustness)).append(',')
      sb.append("\"horizon\":").append(fmt(p.horizon)).append(',')
      sb.append("\"naturalness\":").append(fmt(p.naturalness)).append(',')
      sb.append("\"categoryGlobal\":\"").append(escape(p.categoryGlobal)).append("\",")
      p.categoryPersonal.foreach(cp => sb.append("\"categoryPersonal\":\"").append(escape(cp)).append("\","))
      sb.append("\"category\":\"").append(escape(p.categoryGlobal)).append('"')
      sb.append("},")
    }
    c.opponentRobustness.foreach { or =>
      sb.append("\"opponentRobustness\":").append(fmt(or)).append(',')
    }
    sb.append("\"isPressurePoint\":").append(c.isPressurePoint).append(',')
    sb.append("\"branches\":[")
    c.branches.zipWithIndex.foreach { case (b, idx) =>
      if idx > 0 then sb.append(',')
      sb.append('{')
      sb.append("\"move\":\"").append(escape(b.move)).append("\",")
      sb.append("\"winPct\":").append(fmt(b.winPct)).append(',')
      sb.append("\"label\":\"").append(escape(b.label)).append("\",")
      sb.append("\"pv\":[")
      b.pv.zipWithIndex.foreach { case (m, i) =>
        if i > 0 then sb.append(',')
        sb.append("\"").append(escape(m)).append("\"")
      }
      sb.append("]}")
    }
    sb.append("]}")

  private def renderStudyChapter(sb: StringBuilder, ch: StudyChapter): Unit =
    sb.append('{')
    sb.append("\"id\":\"").append(escape(ch.id)).append("\",")
    sb.append("\"anchorPly\":").append(ch.anchorPly).append(',')
    sb.append("\"fen\":\"").append(escape(ch.fen)).append("\",")
    sb.append("\"played\":\"").append(escape(ch.played)).append("\",")
    ch.best.foreach(b => sb.append("\"best\":\"").append(escape(b)).append("\","))
    sb.append("\"deltaWinPct\":").append(fmt(ch.deltaWinPct)).append(',')
    sb.append("\"studyScore\":").append(fmt(ch.studyScore)).append(',')
    ch.practicality.foreach { p =>
      sb.append("\"practicality\":{")
      sb.append("\"overall\":").append(fmt(p.overall)).append(',')
      sb.append("\"robustness\":").append(fmt(p.robustness)).append(',')
      sb.append("\"horizon\":").append(fmt(p.horizon)).append(',')
      sb.append("\"naturalness\":").append(fmt(p.naturalness)).append(',')
      sb.append("\"categoryGlobal\":\"").append(escape(p.categoryGlobal)).append("\",")
      p.categoryPersonal.foreach(cp => sb.append("\"categoryPersonal\":\"").append(escape(cp)).append("\","))
      sb.append("\"category\":\"").append(escape(p.categoryGlobal)).append('"')
      sb.append("},")
    }
    sb.append("\"tags\":[")
    ch.tags.zipWithIndex.foreach { case (t, idx) =>
      if idx > 0 then sb.append(',')
      sb.append("\"").append(escape(t)).append('"')
    }
    sb.append("],")
    sb.append("\"lines\":[")
    ch.lines.zipWithIndex.foreach { case (l, idx) =>
      if idx > 0 then sb.append(',')
      sb.append('{')
      sb.append("\"label\":\"").append(escape(l.label)).append("\",")
      sb.append("\"winPct\":").append(fmt(l.winPct)).append(',')
      sb.append("\"pv\":[")
      l.pv.zipWithIndex.foreach { case (m, i) =>
        if i > 0 then sb.append(',')
        sb.append("\"").append(escape(m)).append('"')
      }
      sb.append("]}")
    }
    sb.append("]")
    ch.summary.foreach { s =>
      sb.append(",\"summary\":\"").append(escape(s)).append('"')
    }
    sb.append(",\"phase\":\"").append(escape(ch.phase)).append("\",")
    sb.append("\"winPctBefore\":").append(fmt(ch.winPctBefore)).append(',')
    sb.append("\"winPctAfter\":").append(fmt(ch.winPctAfter))
    
    ch.metadata.foreach { m =>
      sb.append(",\"metadata\":{")
      sb.append("\"name\":\"").append(escape(m.name)).append("\",")
      sb.append("\"description\":\"").append(escape(m.description)).append("\"")
      sb.append("}")
    }

    ch.rootNode.foreach { r =>
      sb.append(",\"rootNode\":")
      renderTree(sb, r)
    }
    sb.append('}')

  private def renderTree(sb: StringBuilder, n: TreeNode): Unit =
    sb.append('{')
    sb.append("\"ply\":").append(n.ply).append(',')
    sb.append("\"san\":\"").append(escape(n.san)).append("\",")
    sb.append("\"uci\":\"").append(escape(n.uci)).append("\",")
    sb.append("\"fen\":\"").append(escape(n.fen)).append("\",")
    sb.append("\"eval\":").append(fmt(n.eval)).append(',')
    sb.append("\"evalType\":\"").append(n.evalType).append("\",")
    sb.append("\"judgement\":\"").append(escape(n.judgement)).append("\",")
    sb.append("\"glyph\":\"").append(escape(n.glyph)).append("\",")
    sb.append("\"nodeType\":\"").append(escape(n.nodeType)).append("\",")
    sb.append("\"tags\":[")
    n.tags.zipWithIndex.foreach { case (t, idx) =>
      if idx > 0 then sb.append(',')
      sb.append("\"").append(escape(t)).append('"')
    }
    sb.append("],")
    n.bestMove.foreach(m => sb.append("\"bestMove\":\"").append(escape(m)).append("\","))
    n.bestEval.foreach(v => sb.append("\"bestEval\":").append(fmt(v)).append(','))
    if n.pv.nonEmpty then
      sb.append("\"pv\":[")
      n.pv.zipWithIndex.foreach { case (m, idx) =>
        if idx > 0 then sb.append(',')
        sb.append("\"").append(escape(m)).append('"')
      }
      sb.append("],")
    n.comment.foreach(c => sb.append("\"comment\":\"").append(escape(c)).append("\","))
    
    n.concepts.foreach { c =>
      sb.append("\"concepts\":")
      renderConcepts(sb, c)
      sb.append(',')
    }
    
    n.features.foreach { f =>
      renderFeatures(sb, f)
      sb.append(',')
    }

    n.practicality.foreach { p =>
        sb.append("\"practicality\":{")
        sb.append("\"overall\":").append(fmt(p.overall)).append(',')
        sb.append("\"robustness\":").append(fmt(p.robustness)).append(',')
        sb.append("\"horizon\":").append(fmt(p.horizon)).append(',')
        sb.append("\"naturalness\":").append(fmt(p.naturalness)).append(',')
        sb.append("\"categoryGlobal\":\"").append(escape(p.categoryGlobal)).append("\",")
        p.categoryPersonal.foreach(cp => sb.append("\"categoryPersonal\":\"").append(escape(cp)).append("\","))
        sb.append("\"category\":\"").append(escape(p.categoryGlobal)).append('"')
        sb.append("},")
    }

    sb.append("\"children\":[")
    n.children.zipWithIndex.foreach { case (c, idx) =>
      if idx > 0 then sb.append(',')
      renderTree(sb, c)
    }
    sb.append("]}")

  private def renderConcepts(sb: StringBuilder, c: Concepts): Unit =
    sb.append('{')
    sb.append("\"dynamic\":").append(fmt(c.dynamic)).append(',')
    sb.append("\"drawish\":").append(fmt(c.drawish)).append(',')
    sb.append("\"imbalanced\":").append(fmt(c.imbalanced)).append(',')
    sb.append("\"tacticalDepth\":").append(fmt(c.tacticalDepth)).append(',')
    sb.append("\"blunderRisk\":").append(fmt(c.blunderRisk)).append(',')
    sb.append("\"pawnStorm\":").append(fmt(c.pawnStorm)).append(',')
    sb.append("\"fortress\":").append(fmt(c.fortress)).append(',')
    sb.append("\"colorComplex\":").append(fmt(c.colorComplex)).append(',')
    sb.append("\"badBishop\":").append(fmt(c.badBishop)).append(',')
    sb.append("\"goodKnight\":").append(fmt(c.goodKnight)).append(',')
    sb.append("\"rookActivity\":").append(fmt(c.rookActivity)).append(',')
    sb.append("\"kingSafety\":").append(fmt(c.kingSafety)).append(',')
    sb.append("\"dry\":").append(fmt(c.dry)).append(',')
    sb.append("\"comfortable\":").append(fmt(c.comfortable)).append(',')
    sb.append("\"unpleasant\":").append(fmt(c.unpleasant)).append(',')
    sb.append("\"engineLike\":").append(fmt(c.engineLike)).append(',')
    sb.append("\"conversionDifficulty\":").append(fmt(c.conversionDifficulty)).append(',')
    sb.append("\"sacrificeQuality\":").append(fmt(c.sacrificeQuality)).append(',')
    sb.append("\"alphaZeroStyle\":").append(fmt(c.alphaZeroStyle))
    sb.append('}')
