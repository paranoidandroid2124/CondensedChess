package chess
package analysis

import AnalyzeDomain.*
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
      val arc = NarrativeBuilder.detectArc(anchor.deltaWinPct)
      val template = NarrativeBuilder.narrativeTemplate(tags, arc)
      template

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
      altLine.foreach { l =>
        lines += StudyLine(label = "alt", pv = pvToSan(anchor.fenBefore, l.pv), winPct = l.winPct)
      }

      val enrichedTags = (phase :: anchor.studyTags).take(6)

      StudyChapter(
        id = id,
        anchorPly = anchor.ply.value,
        fen = anchor.fenBefore,
        played = anchor.san,
        best = bestLine.flatMap(bl => if playedLine.contains(bl) then None else Some(uciToSanSingle(anchor.fenBefore, bl.move))),
        deltaWinPct = anchor.deltaWinPct,
        tags = enrichedTags,
        lines = lines.toList,
        summary = Some(narrativeSummary(anchor, enrichedTags, phase, anchor.winPctBefore, anchor.winPctAfterForPlayer)),
        studyScore = anchor.studyScore,
        phase = phase,
        winPctBefore = anchor.winPctBefore,
        winPctAfter = anchor.winPctAfterForPlayer,
        practicality = anchor.practicality
      )
    }.toVector
