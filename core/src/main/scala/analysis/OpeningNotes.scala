package chess
package analysis

import AnalyzeDomain.PlyOutput
import AnalyzeUtils.{ moveLabel, pctInt }
import chess.opening.Opening

object OpeningNotes:
  def buildOpeningNotes(
      opening: Option[Opening.AtPly],
      openingStats: Option[OpeningExplorer.Stats],
      timeline: Vector[PlyOutput]
  ): (Option[String], Option[String], Option[String]) =
    val openingName = opening.map(_.opening.name.value).getOrElse("Opening")
    val summary = openingStats.map { os =>
      val topMove = os.topMoves.headOption
        .map { tm =>
          val win = tm.winPct.map(pctInt).map(w => s", win $w%").getOrElse("")
          s"top move ${tm.san} (${tm.games} games$win)"
        }
        .getOrElse("no top move data")
      val gamesStr = os.games.map(g => s"$g games").getOrElse("few games")
      val source = s"source=${os.source}"
      s"$openingName book to ply ${os.bookPly} (novelty ${os.noveltyPly}, $gamesStr, $topMove, $source)"
    }

    val exitPly = openingStats.map(_.bookPly + 1).orElse(opening.map(op => op.ply.value + 1))
    val bookExit = for
      ep <- exitPly
      move <- timeline.find(_.ply.value == ep)
    yield
      val label = moveLabel(move.ply, move.san, move.turn)
      val topAlt = openingStats.flatMap(_.topMoves.headOption).map { tm =>
        val win = tm.winPct.map(pctInt).map(w => s", win $w%").getOrElse("")
        s"${tm.san} (${tm.games} games$win)"
      }
      val notableGame = openingStats.flatMap(_.topGames.headOption).map { g =>
        val date = g.date.map(d => s" $d").getOrElse("")
        s"${g.white}-${g.black}$date"
      }
      val topMoveStr = topAlt.map(m => s"book line: $m").getOrElse("book line unknown")
      val notableStr = notableGame.map(g => s"; notable game $g").getOrElse("")
      val phaseNote = move.phaseLabel.map(ph => s"; phase=$ph").getOrElse("")
      s"Left book at $label playing ${move.san}; $topMoveStr$notableStr$phaseNote."

    val trend = openingStats.flatMap { os =>
      val buckets = os.yearBuckets
      val recent = buckets.getOrElse("2020_plus", 0) + buckets.getOrElse("2018_2019", 0)
      val prior = buckets.getOrElse("pre2012", 0) + buckets.getOrElse("2012_2017", 0)
      val span = (os.minYear, os.maxYear) match
        case (Some(minY), Some(maxY)) if minY > 0 && maxY > 0 => s" (${minY}-${maxY})"
        case _ => ""
      if recent >= 10 && prior > 0 && recent.toDouble / prior >= 1.5 then
        Some(s"Line popularity rising post-2018: recent ${recent} vs prior ${prior} games$span.")
      else if recent >= 5 && prior > 0 && recent.toDouble / prior <= 0.6 then
        Some(s"Line less common after 2018: recent ${recent} vs prior ${prior} games$span.")
      else if recent + prior >= 8 then
        Some(s"Line stable across eras: recent ${recent}, prior ${prior}$span.")
      else None
    }

    (summary, bookExit, trend)
