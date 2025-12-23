package chess
package analysis

import AnalysisModel.*
import AnalysisTypes.*
import ConceptLabeler.*
import AnalyzeUtils.escape

object AnalysisSerializer:

  def render(output: Output): String =
    val sb = new StringBuilder(256 + output.timeline.size * 128)
    sb.append('{')
    
    // Root Metadata (Hardening)
    sb.append("\"schemaVersion\":").append(ApiTypes.SCHEMA_VERSION).append(',')
    sb.append("\"createdAt\":\"").append(java.time.Instant.now().toString).append("\",")
    sb.append("\"engineInfo\":{")
    sb.append("\"name\":\"Stockfish 16\",") // Placeholder or pass from config
    sb.append("\"depth\":\"Variable\"")
    sb.append("},")

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
    output.book.foreach { b =>
      sb.append("\"book\":")
      renderBook(sb, b)
      sb.append(",")
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
      ply.playedEvalCp.foreach(cp => sb.append(",\"playedEvalCp\":").append(cp))
      sb.append(',')
      renderConcepts(sb, "concepts", ply.concepts)
      sb.append(',')
      renderConcepts(sb, "conceptsBefore", ply.conceptsBefore)
      sb.append(',')
      renderConcepts(sb, "conceptDelta", ply.conceptDelta)
      if ply.hypotheses.nonEmpty then
        sb.append(',')
        sb.append("\"hypotheses\":[")
        ply.hypotheses.zipWithIndex.foreach { case (b, idx) =>
          if idx > 0 then sb.append(',')
          sb.append('{')
          sb.append("\"move\":\"").append(escape(b.move)).append("\",")
          sb.append("\"winPct\":").append(fmt(b.winPct)).append(',')
          sb.append("\"label\":\"").append(escape(b.label)).append("\",")
          b.comment.foreach { txt =>
            sb.append("\"comment\":\"").append(escape(txt)).append("\",")
          }
          sb.append("\"pv\":[")
          b.pv.zipWithIndex.foreach { case (m, i) =>
            if i > 0 then sb.append(',')
            sb.append("\"").append(escape(m)).append("\"")
          }
          sb.append("]}")
        }
        sb.append("]")
      ply.conceptLabels.foreach { cl =>
        sb.append(",\"conceptLabels\":")
        renderConceptLabels(cl, sb)
      }
      sb.append("]")
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
      b.cp.foreach(cp => sb.append("\"cp\":").append(cp).append(','))
      b.mate.foreach(m => sb.append("\"mate\":").append(m).append(','))
      sb.append("\"label\":\"").append(escape(b.label)).append("\",")
      b.comment.foreach { txt =>
        sb.append("\"comment\":\"").append(escape(txt)).append("\",")
      }
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
      l.cp.foreach(cp => sb.append("\"cp\":").append(cp).append(','))
      l.mate.foreach(m => sb.append("\"mate\":").append(m).append(','))
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

  // --- Phase 4.6 Book Serialization ---

  private def renderBook(sb: StringBuilder, book: BookModel.Book): Unit =
    sb.append('{')
    sb.append("\"gameMeta\":")
    renderGameMeta(sb, book.gameMeta)
    sb.append(',')
    
    sb.append("\"sections\":[")
    book.sections.zipWithIndex.foreach { case (s, idx) =>
      if idx > 0 then sb.append(',')
      renderBookSection(sb, s)
    }
    sb.append("],")

    sb.append("\"turningPoints\":[")
    book.turningPoints.zipWithIndex.foreach { case (tp, idx) =>
      if idx > 0 then sb.append(',')
      renderTurningPoint(sb, tp)
    }
    sb.append("],")
    
    sb.append("\"tacticalMoments\":[")
    book.tacticalMoments.zipWithIndex.foreach { case (tm, idx) =>
      if idx > 0 then sb.append(',')
      renderTacticalMoment(sb, tm)
    }
    sb.append("],")
    
    sb.append("\"checklist\":[")
    book.checklist.zipWithIndex.foreach { case (cb, idx) =>
      if idx > 0 then sb.append(',')
      renderChecklistBlock(sb, cb)
    }
    sb.append("]")
    
    sb.append('}')

  private def renderGameMeta(sb: StringBuilder, m: BookModel.GameMeta): Unit =
    sb.append('{')
    sb.append("\"white\":\"").append(escape(m.white)).append("\",")
    sb.append("\"black\":\"").append(escape(m.black)).append("\",")
    sb.append("\"result\":\"").append(escape(m.result)).append("\"")
    m.openingName.foreach(n => sb.append(",\"openingName\":\"").append(escape(n)).append("\""))
    sb.append('}')

  private def renderBookDiagram(sb: StringBuilder, d: BookModel.BookDiagram): Unit =
    sb.append('{')
    sb.append("\"id\":\"").append(escape(d.id)).append("\",")
    sb.append("\"fen\":\"").append(escape(d.fen)).append("\",")
    sb.append("\"roles\":[")
    d.roles.zipWithIndex.foreach { case (r, i) =>
      if i > 0 then sb.append(',')
      sb.append("\"").append(escape(r)).append("\"")
    }
    sb.append("],")
    sb.append("\"ply\":").append(d.ply).append(',')
    sb.append("\"tags\":")
    renderTagBundle(sb, d.tags)
    sb.append('}')

  private def renderTagBundle(sb: StringBuilder, t: BookModel.TagBundle): Unit =
    sb.append('{')
    sb.append("\"structure\":[")
    t.structure.zipWithIndex.foreach { case (s, i) => if i > 0 then sb.append(','); sb.append("\"").append(s.toString).append("\"") }
    sb.append("],")
    sb.append("\"plan\":[")
    t.plan.zipWithIndex.foreach { case (s, i) => if i > 0 then sb.append(','); sb.append("\"").append(s.toString).append("\"") }
    sb.append("],")
    sb.append("\"tactic\":[")
    t.tactic.zipWithIndex.foreach { case (s, i) => if i > 0 then sb.append(','); sb.append("\"").append(s.toString).append("\"") }
    sb.append("],")
    sb.append("\"mistake\":[")
    t.mistake.zipWithIndex.foreach { case (s, i) => if i > 0 then sb.append(','); sb.append("\"").append(s.toString).append("\"") }
    sb.append("],")
    sb.append("\"endgame\":[")
    t.endgame.zipWithIndex.foreach { case (s, i) => if i > 0 then sb.append(','); sb.append("\"").append(s.toString).append("\"") }
    sb.append("],")
    sb.append("\"transition\":[")
    t.transition.zipWithIndex.foreach { case (s, i) => if i > 0 then sb.append(','); sb.append("\"").append(s.toString).append("\"") }
    sb.append("]")
    sb.append('}')

  private def renderTurningPoint(sb: StringBuilder, tp: BookModel.BookTurningPoint): Unit =
    sb.append('{')
    sb.append("\"ply\":").append(tp.ply).append(',')
    sb.append("\"side\":\"").append(tp.side).append("\",")
    sb.append("\"playedMove\":\"").append(escape(tp.playedMove)).append("\",")
    sb.append("\"bestMove\":\"").append(escape(tp.bestMove)).append("\",")
    sb.append("\"evalBefore\":").append(tp.evalBefore).append(',')
    sb.append("\"evalAfterPlayed\":").append(tp.evalAfterPlayed).append(',')
    sb.append("\"evalAfterBest\":").append(tp.evalAfterBest).append(',')
    sb.append("\"mistakeTags\":[")
    tp.mistakeTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(t.toString).append("\"") }
    sb.append("]")
    sb.append('}')

  private def renderTacticalMoment(sb: StringBuilder, tm: BookModel.BookTacticalMoment): Unit =
    sb.append('{')
    sb.append("\"ply\":").append(tm.ply).append(',')
    sb.append("\"side\":\"").append(tm.side).append("\",")
    sb.append("\"motifTags\":[")
    tm.motifTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(t.toString).append("\"") }
    sb.append("],")
    tm.evalGainIfPlayed.foreach(g => sb.append("\"evalGainIfPlayed\":").append(g).append(','))
    sb.append("\"wasMissed\":").append(tm.wasMissed)
    sb.append('}')

  private def renderBookSection(sb: StringBuilder, s: BookModel.BookSection): Unit =
    sb.append('{')
    sb.append("\"title\":\"").append(escape(s.title)).append("\",")
    sb.append("\"sectionType\":\"").append(escape(s.sectionType.toString)).append("\",")
    sb.append("\"narrativeHint\":\"").append(escape(s.narrativeHint)).append("\",")
    sb.append("\"startPly\":").append(s.startPly).append(',')
    sb.append("\"endPly\":").append(s.endPly).append(',')
    sb.append("\"diagrams\":[")
    s.diagrams.zipWithIndex.foreach { case (d, idx) =>
      if idx > 0 then sb.append(',')
      renderBookDiagram(sb, d)
    }
    sb.append("]")
    sb.append('}')

  private def renderChecklistBlock(sb: StringBuilder, cb: BookModel.ChecklistBlock): Unit =
    sb.append('{')
    sb.append("\"category\":\"").append(escape(cb.category)).append("\",")
    sb.append("\"hintTags\":[")
    cb.hintTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t)).append("\"") }
    sb.append("]")
    sb.append('}')

  // --- Phase 1: New Chapter Serialization (Checklist-aligned) ---

  def renderGameChapter(chapter: BookModel.GameChapter): String =
    val sb = new StringBuilder(1024)
    sb.append('{')
    sb.append("\"chapterId\":\"").append(escape(chapter.chapterId)).append("\",")
    sb.append("\"schemaVersion\":").append(ApiTypes.SCHEMA_VERSION).append(',')
    sb.append("\"meta\":")
    renderGameMetaV2(sb, chapter.meta)
    sb.append(",\"sections\":[")
    chapter.sections.zipWithIndex.foreach { case (s, idx) =>
      if idx > 0 then sb.append(',')
      renderChapterSection(sb, s)
    }
    sb.append("]}")
    sb.result()

  private def renderGameMetaV2(sb: StringBuilder, m: BookModel.GameMeta): Unit =
    sb.append('{')
    sb.append("\"white\":\"").append(escape(m.white)).append("\",")
    sb.append("\"black\":\"").append(escape(m.black)).append("\",")
    sb.append("\"result\":\"").append(escape(m.result)).append("\"")
    m.openingName.foreach(n => sb.append(",\"openingName\":\"").append(escape(n)).append("\""))
    m.createdAt.foreach(t => sb.append(",\"createdAt\":\"").append(escape(t)).append("\""))
    sb.append('}')

  private def renderChapterSection(sb: StringBuilder, s: BookModel.ChapterSection): Unit =
    sb.append('{')
    sb.append("\"type\":\"").append(sectionTypeKey(s.sectionType)).append("\",")
    sb.append("\"data\":")
    renderSectionData(sb, s.data)
    sb.append('}')

  private def sectionTypeKey(t: BookModel.SectionType): String = t match
    case BookModel.SectionType.TitleSummary => "title_summary"
    case BookModel.SectionType.KeyDiagrams => "key_diagrams"
    case BookModel.SectionType.OpeningReview => "opening_review"
    case BookModel.SectionType.TurningPoints => "turning_points"
    case BookModel.SectionType.TacticalMoments => "tactical_moments"
    case BookModel.SectionType.MiddlegamePlans => "middlegame_plans"
    case BookModel.SectionType.EndgameLessons => "endgame_lessons"
    case BookModel.SectionType.FinalChecklist => "final_checklist"
    // Legacy types removed
    case BookModel.SectionType.TacticalStorm => "tactical_storm"

  private def renderSectionData(sb: StringBuilder, data: BookModel.SectionData): Unit = data match
    case d: BookModel.TitleSummaryData =>
      sb.append('{')
      sb.append("\"title\":\"").append(escape(d.title)).append("\",")
      sb.append("\"summary\":\"").append(escape(d.summary)).append("\"")
      sb.append('}')
    case d: BookModel.KeyDiagramsData =>
      sb.append("{\"diagrams\":[")
      d.diagrams.zipWithIndex.foreach { case (diag, i) =>
        if i > 0 then sb.append(',')
        renderKeyDiagram(sb, diag)
      }
      sb.append("]}")
    case d: BookModel.OpeningReviewData =>
      sb.append('{')
      sb.append("\"structure\":\"").append(escape(d.structure)).append("\",")
      sb.append("\"mainPlans\":[")
      d.mainPlans.zipWithIndex.foreach { case (p, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(p)).append("\"") }
      sb.append("]")
      d.deviation.foreach(dev => sb.append(",\"deviation\":\"").append(escape(dev)).append("\""))
      sb.append('}')
    case d: BookModel.TurningPointsData =>
      sb.append("{\"points\":[")
      d.points.zipWithIndex.foreach { case (tp, i) =>
        if i > 0 then sb.append(',')
        renderTurningPoint(sb, tp)
      }
      sb.append("]}")
    case d: BookModel.TacticalMomentsData =>
      sb.append("{\"moments\":[")
      d.moments.zipWithIndex.foreach { case (tm, i) =>
        if i > 0 then sb.append(',')
        renderTacticalMoment(sb, tm)
      }
      sb.append("]}")
    case d: BookModel.MiddlegamePlansData =>
      sb.append('{')
      sb.append("\"dominantStructure\":\"").append(escape(d.dominantStructure)).append("\",")
      sb.append("\"plans\":[")
      d.plans.zipWithIndex.foreach { case (p, i) =>
        if i > 0 then sb.append(',')
        renderPlanSummary(sb, p)
      }
      sb.append("]}")
    case d: BookModel.EndgameLessonsData =>
      sb.append("{\"principles\":[")
      d.principles.zipWithIndex.foreach { case (p, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(p)).append("\"") }
      sb.append("]}")
    case d: BookModel.FinalChecklistData =>
      sb.append("{\"items\":[")
      d.items.zipWithIndex.foreach { case (item, i) =>
        if i > 0 then sb.append(',')
        renderChecklistItem(sb, item)
      }
      sb.append("]}")
    case d: BookModel.LegacySectionData =>
      sb.append("{\"diagrams\":[")
      d.diagrams.zipWithIndex.foreach { case (diag, i) =>
        if i > 0 then sb.append(',')
        renderBookDiagram(sb, diag)
      }
      sb.append("],\"narrativeHint\":\"").append(escape(d.narrativeHint)).append("\"}")

  private def renderKeyDiagram(sb: StringBuilder, d: BookModel.KeyDiagram): Unit =
    sb.append('{')
    sb.append("\"fen\":\"").append(escape(d.fen)).append("\",")
    sb.append("\"role\":\"").append(escape(d.role)).append("\",")
    sb.append("\"tags\":[")
    d.tags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t)).append("\"") }
    sb.append("],\"referenceMoves\":[")
    d.referenceMoves.zipWithIndex.foreach { case (m, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(m)).append("\"") }
    sb.append("]}")

  private def renderPlanSummary(sb: StringBuilder, p: BookModel.PlanSummary): Unit =
    sb.append('{')
    sb.append("\"planType\":\"").append(escape(p.planType)).append("\",")
    sb.append("\"quality\":\"").append(escape(p.quality)).append("\",")
    sb.append("\"description\":\"").append(escape(p.description)).append("\"")
    sb.append('}')

  private def renderChecklistItem(sb: StringBuilder, item: BookModel.ChecklistItem): Unit =
    sb.append('{')
    sb.append("\"id\":\"").append(escape(item.id)).append("\",")
    sb.append("\"category\":\"").append(escape(item.category)).append("\",")
    sb.append("\"text\":\"").append(escape(item.text)).append("\",")
    sb.append("\"tags\":[")
    item.tags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t)).append("\"") }
    sb.append("]}")

  private def renderConceptLabels(cl: ConceptLabels, sb: StringBuilder): Unit =
    sb.append('{')
    sb.append("\"structureTags\":[")
    cl.structureTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t.toString)).append("\"") }
    sb.append("],\"planTags\":[")
    cl.planTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t.toString)).append("\"") }
    sb.append("],\"tacticTags\":[")
    cl.tacticTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t.toString)).append("\"") }
    sb.append("],\"mistakeTags\":[")
    cl.mistakeTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t.toString)).append("\"") }
    sb.append("],\"endgameTags\":[")
    cl.endgameTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t.toString)).append("\"") }
    sb.append("],\"positionalTags\":[")
    cl.positionalTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t.toString)).append("\"") }
    sb.append("],\"transitionTags\":[")
    cl.transitionTags.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t.toString)).append("\"") }
    sb.append("],\"missedPatternTypes\":[")
    cl.missedPatternTypes.zipWithIndex.foreach { case (t, i) => if i > 0 then sb.append(','); sb.append("\"").append(escape(t)).append("\"") }
    sb.append("],\"richTags\":[")
    cl.richTags.zipWithIndex.foreach { case (rt, i) =>
       if i > 0 then sb.append(',')
       sb.append("{\"id\":\"").append(escape(rt.id)).append("\",")
       sb.append("\"score\":").append(fmt(rt.score)).append(',')
       sb.append("\"category\":\"").append(escape(rt.category.toString)).append("\",")
       sb.append("\"evidenceRefs\":[")
       rt.evidenceRefs.zipWithIndex.foreach { case (ref, ri) =>
         if ri > 0 then sb.append(',')
         sb.append("{\"kind\":\"").append(escape(ref.kind)).append("\",\"id\":\"").append(escape(ref.id)).append("\"}")
       }
       sb.append("]}")
    }
    sb.append("],\"evidence\":")
    renderEvidencePack(cl.evidence, sb)
    sb.append('}')

  private def renderEvidencePack(ev: EvidencePack, sb: StringBuilder): Unit =
    sb.append('{')
    sb.append("\"kingSafety\":{")
    ev.kingSafety.zipWithIndex.foreach { case ((id, e), i) =>
      if i > 0 then sb.append(',')
      sb.append("\"").append(escape(id)).append("\":{\"square\":\"").append(escape(e.square)).append("\",\"openFiles\":[")
      e.openFiles.zipWithIndex.foreach { case (f, fi) => if fi > 0 then sb.append(','); sb.append("\"").append(escape(f)).append("\"") }
      sb.append("],\"attackers\":").append(e.attackers).append(',')
      sb.append("\"checks\":[")
      e.checks.zipWithIndex.foreach { case (c, ci) => if ci > 0 then sb.append(','); sb.append("\"").append(escape(c)).append("\"") }
      sb.append("],\"defenders\":").append(e.defenders).append('}')
    }
    sb.append("},\"structure\":{")
    ev.structure.zipWithIndex.foreach { case ((id, e), i) =>
      if i > 0 then sb.append(',')
      sb.append("\"").append(escape(id)).append("\":{\"description\":\"").append(escape(e.description)).append("\",\"squares\":[")
      e.squares.zipWithIndex.foreach { case (s, si) => if si > 0 then sb.append(','); sb.append("\"").append(escape(s)).append("\"") }
      sb.append("]}")
    }
    sb.append("},\"tactics\":{")
    ev.tactics.zipWithIndex.foreach { case ((id, e), i) =>
      if i > 0 then sb.append(',')
      sb.append("\"").append(escape(id)).append("\":{\"motif\":\"").append(escape(e.motif)).append("\",\"sequence\":[")
      e.sequence.zipWithIndex.foreach { case (s, si) => if si > 0 then sb.append(','); sb.append("\"").append(escape(s)).append("\"") }
      sb.append("]")
      e.captured.foreach(c => sb.append(",\"captured\":\"").append(escape(c)).append("\""))
      sb.append('}')
    }
    sb.append("},\"placement\":{")
    ev.placement.zipWithIndex.foreach { case ((id, e), i) =>
      if i > 0 then sb.append(',')
      sb.append("\"").append(escape(id)).append("\":{\"piece\":\"").append(escape(e.piece)).append("\",\"reason\":\"").append(escape(e.reason)).append("\",\"startSquare\":\"").append(escape(e.startSquare)).append("\",\"targetSquare\":\"").append(escape(e.targetSquare)).append("\",\"delta\":").append(fmt(e.delta)).append('}')
    }
    sb.append("},\"plans\":{")
    ev.plans.zipWithIndex.foreach { case ((id, e), i) =>
      if i > 0 then sb.append(',')
      sb.append("\"").append(escape(id)).append("\":{\"concept\":\"").append(escape(e.concept)).append("\",\"starterMove\":\"").append(escape(e.starterMove)).append("\",")
      sb.append("\"goal\":\"").append(escape(e.goal)).append("\",\"successScore\":").append(fmt(e.successScore)).append(',')
      sb.append("\"pv\":[")
      e.pv.zipWithIndex.foreach { case (m, mi) => if mi > 0 then sb.append(','); sb.append("\"").append(escape(m)).append("\"") }
      sb.append("]}")
    }
    sb.append("},\"pv\":{")
    ev.pv.zipWithIndex.foreach { case ((id, e), i) =>
      if i > 0 then sb.append(',')
      sb.append("\"").append(escape(id)).append("\":{")
      sb.append("\"line\":[")
      e.line.zipWithIndex.foreach { case (m, mi) => if mi > 0 then sb.append(','); sb.append("\"").append(escape(m)).append("\"") }
      sb.append("],")
      renderEval(sb, "eval", e.eval)
      sb.append('}')
    }
    sb.append("}}")
