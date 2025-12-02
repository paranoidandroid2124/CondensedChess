package chess
package analysis

import chess.format.pgn.SanStr
import chess.opening.Opening
import ujson.*

import java.nio.file.{ Files, Paths }

/** Lightweight Opening Explorer:
  * - noveltyPly: first ply after the known book line
  * - optional frequency/win rates/top moves from an external stats JSON (opt-in)
  *
  * Stats file shape (example):
  * {
  *   "e4 e5 Nf3 Nc6 Bb5": {
  *     "bookPly": 8,
  *     "freq": 0.12,
  *     "winWhite": 0.54,
  *     "winBlack": 0.23,
  *     "draw": 0.23,
  *     "topMoves": [
  *       {"san": "a6", "uci": "a7a6", "freq": 0.62, "winPct": 0.55},
  *       {"san": "Nf6", "uci": "g8f6", "freq": 0.22, "winPct": 0.51}
  *     ]
  *   }
  * }
  */
object OpeningExplorer:
  final case class TopMove(san: String, uci: String, freq: Double, winPct: Double)
  final case class Stats(
      bookPly: Int,
      noveltyPly: Int,
      freq: Option[Double],
      winWhite: Option[Double],
      winBlack: Option[Double],
      draw: Option[Double],
      topMoves: List[TopMove],
      source: String
  )

  private lazy val statsByKey: Map[String, Stats] = loadStats()

  private def keyFromSans(sans: Iterable[SanStr]): String =
    sans.take(20).map(_.value).mkString(" ")

  private def loadStats(): Map[String, Stats] =
    val path = sys.env.getOrElse("OPENING_STATS_JSON", "opening_stats.json")
    val p = Paths.get(path)
    if !Files.exists(p) then Map.empty
    else
      try
        val json = ujson.read(Files.readString(p))
        json.obj.toMap.flatMap { case (k, v) =>
          parseStats(k, v).map(k -> _)
        }
      catch
        case e: Throwable =>
          System.err.println(s"[opening-stats] failed to load $path: ${e.getMessage}")
          Map.empty

  private def parseStats(key: String, v: Value): Option[Stats] =
    try
      val obj = v.obj
      val bookPly = obj.get("bookPly").flatMap(_.num.toIntOption).getOrElse(0)
      val novelty = obj.get("noveltyPly").flatMap(_.num.toIntOption).getOrElse(bookPly + 1)
      val freq = obj.get("freq").map(_.num.toDouble)
      val winWhite = obj.get("winWhite").map(_.num.toDouble)
      val winBlack = obj.get("winBlack").map(_.num.toDouble)
      val draw = obj.get("draw").map(_.num.toDouble)
      val topMoves = obj
        .get("topMoves")
        .map(_.arr.toList.flatMap { mv =>
          val mobj = mv.obj
          for
            san <- mobj.get("san").map(_.str)
            uci <- mobj.get("uci").map(_.str)
            f <- mobj.get("freq").flatMap(_.num.toDoubleOption)
            w <- mobj.get("winPct").flatMap(_.num.toDoubleOption)
          yield TopMove(san = san, uci = uci, freq = f, winPct = w)
        })
        .getOrElse(Nil)
      Some(Stats(bookPly = bookPly, noveltyPly = novelty, freq = freq, winWhite = winWhite, winBlack = winBlack, draw = draw, topMoves = topMoves, source = "file"))
    catch
      case _: Throwable => None

  def explore(opening: Option[Opening.AtPly], sans: List[SanStr]): Option[Stats] =
    val key = keyFromSans(sans)
    statsByKey
      .get(key)
      .orElse {
        opening.map { op =>
          val bookPly = op.ply.value
          val novelty = math.min(math.max(1, bookPly + 1), sans.length)
          Stats(
            bookPly = bookPly,
            noveltyPly = novelty,
            freq = None,
            winWhite = None,
            winBlack = None,
            draw = None,
            topMoves = Nil,
            source = "book"
          )
        }
      }
