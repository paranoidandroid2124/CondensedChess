package lila.commentary.tools.opening

import chess.format.pgn.{ Parser, PgnStr }
import chess.{ Color, Knight, Move }
import lila.commentary.analysis.{ OpeningFamilyCatalog, OpeningRouteCatalog }
import play.api.libs.json.*

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

object OpeningRouteMiningRunner:

  final case class Config(
      openingsPath: Path = Paths.get("modules/commentaryCore/src/main/resources/lila/commentary/openings/openings.tsv"),
      routesPath: Path = Paths.get("modules/commentaryCore/src/main/resources/lila/commentary/openings/opening_routes.tsv"),
      outJsonPath: Path = Paths.get("modules/commentaryTools/target/opening-route-mining-candidates.json"),
      outTsvPath: Path = Paths.get("modules/commentaryTools/target/opening-route-mining-candidates.tsv"),
      minRows: Int = 3,
      minWeightedGames: Int = 3,
      maxRoutePlies: Int = 12
  )

  final case class Candidate(
      routeId: String,
      family: String,
      targetSquare: String,
      role: String,
      from: String,
      via: List[String],
      to: String,
      maxReplayPlies: Int,
      targetMode: String,
      rowSupport: Int,
      weightedGames: Int,
      sampleRows: List[String],
      alreadyCataloged: Boolean
  )

  private final case class OpeningRow(lineNo: Int, eco: String, name: String, pgn: String)
  private final case class RouteKey(family: String, from: String, via: List[String], to: String)
  private final case class RouteHit(rowId: String, games: Int)
  private final case class Track(color: Color, from: String, route: List[String])

  given Writes[Candidate] = Json.writes[Candidate]

  def main(args: Array[String]): Unit =
    buildReport(parseConfig(args.toList)) match
      case Left(error) =>
        System.err.println(s"[opening-route-mining] $error")
        sys.exit(1)
      case Right(candidates) =>
        val config = parseConfig(args.toList)
        Option(config.outJsonPath.getParent).foreach(Files.createDirectories(_))
        Option(config.outTsvPath.getParent).foreach(Files.createDirectories(_))
        Files.writeString(config.outJsonPath, Json.prettyPrint(Json.toJson(candidates)) + "\n", StandardCharsets.UTF_8)
        Files.writeString(config.outTsvPath, renderTsv(candidates), StandardCharsets.UTF_8)
        val newRoutes = candidates.count(!_.alreadyCataloged)
        println(
          s"[opening-route-mining] wrote ${config.outJsonPath} candidates=${candidates.size} new=$newRoutes " +
            s"minRows=${config.minRows} minWeightedGames=${config.minWeightedGames}"
        )

  def buildReport(config: Config): Either[String, List[Candidate]] =
    try
      val openings = readOpenings(config.openingsPath)
      val existing = readExistingRouteKeys(config.routesPath)
      val familyCatalog = OpeningFamilyCatalog.default
      val hits = mutable.Map.empty[RouteKey, mutable.ListBuffer[RouteHit]]
      openings.foreach { row =>
        val families = familyCatalog.familiesForOpening(row.name).map(_.wireKey).distinct
        if families.nonEmpty then
          minedKnightRoutes(row).foreach { route =>
            families.foreach { family =>
              if familyCatalog.targetAllowed(family, route.to) then
                val key = RouteKey(family, route.from, route.via, route.to)
                val buffer = hits.getOrElseUpdate(key, mutable.ListBuffer.empty)
                buffer += RouteHit(rowId(row), masterGameWeight(row))
            }
          }
      }
      val candidates =
        hits.toList.flatMap { case (key, buffer) =>
          val path = key.from :: (key.via :+ key.to)
          val rowSupport = buffer.map(_.rowId).distinct.size
          val weighted = buffer.map(_.games).sum
          Option.when(
            key.via.nonEmpty &&
              path.distinct.size == path.size &&
              rowSupport >= config.minRows &&
              weighted >= config.minWeightedGames
          ) {
            val maxReplayPlies = math.min(6 + key.via.size * 2, config.maxRoutePlies)
            Candidate(
              routeId = routeId(key),
              family = key.family,
              targetSquare = key.to,
              role = "knight",
              from = key.from,
              via = key.via,
              to = key.to,
              maxReplayPlies = maxReplayPlies,
              targetMode = OpeningRouteCatalog.OccupyTarget,
              rowSupport = rowSupport,
              weightedGames = weighted,
              sampleRows = buffer.map(_.rowId).distinct.take(8).toList,
              alreadyCataloged = existing.contains(key)
            )
          }
        }.sortBy(candidate => (!candidate.alreadyCataloged, -candidate.rowSupport, -candidate.weightedGames, candidate.routeId))
      Right(candidates)
    catch case NonFatal(e) => Left(Option(e.getMessage).filter(_.nonEmpty).getOrElse(e.getClass.getSimpleName))

  private def readOpenings(path: Path): List[OpeningRow] =
    Files
      .readAllLines(path, StandardCharsets.UTF_8)
      .asScala
      .toList
      .zipWithIndex
      .flatMap { case (raw, idx) =>
        val line = raw.trim.stripPrefix("\uFEFF")
        if line.isEmpty || line.toLowerCase.startsWith("eco\tname\t") then None
        else
          line.split('\t').toList match
            case eco :: name :: pgn :: _ => Some(OpeningRow(idx + 1, eco.trim, name.trim, pgn.trim))
            case _                       => None
      }

  private def readExistingRouteKeys(path: Path): Set[RouteKey] =
    OpeningRouteCatalog
      .fromTsvLines(Files.readAllLines(path, StandardCharsets.UTF_8).asScala)
      .routes
      .map(route => RouteKey(route.family, route.from, route.via, route.to))
      .toSet

  private def minedKnightRoutes(row: OpeningRow): List[RouteKey] =
    Parser.mainline(PgnStr(row.pgn)).toOption.toList.flatMap { parsed =>
      val replay = chess.Replay.makeReplay(parsed.toGame, parsed.moves).replay.chronoMoves
      val tracksBySquare = mutable.Map.empty[String, Track]
      val finished = mutable.ListBuffer.empty[Track]
      replay.foreach {
        case move: Move if move.piece.role == Knight =>
          val from = move.orig.key
          val to = move.dest.key
          val track = tracksBySquare.remove(from).getOrElse(Track(move.piece.color, from, List(from)))
          val next = track.copy(route = track.route :+ to)
          tracksBySquare.update(to, next)
          if next.route.size >= 2 then finished += next
        case _ =>
      }
      finished.toList
        .map(track => normalizeRoute(track.route))
        .filter(_.size >= 2)
        .distinct
        .map(path => RouteKey("", path.head, path.drop(1).dropRight(1), path.last))
    }

  private def normalizeRoute(path: List[String]): List[String] =
    path.foldLeft(List.empty[String]) { (acc, square) =>
      if acc.lastOption.contains(square) then acc else acc :+ square
    }

  private def routeId(key: RouteKey): String =
    val path = (key.from :: (key.via :+ key.to)).mkString("_")
    s"${key.family}_${path}"

  private def rowId(row: OpeningRow): String =
    s"${row.lineNo}:${row.eco}:${row.name}"

  private def masterGameWeight(row: OpeningRow): Int =
    // The runtime pool is already pruned to master-backed rows. Until a stable
    // per-row evidence cache is promoted into source control, row support is the
    // reliable in-repo weight and this constant avoids overfitting to stale cache.
    1

  private def renderTsv(candidates: List[Candidate]): String =
    val header =
      "route_id\tfamily\ttarget_square\trole\tfrom\tvia\tto\tmax_replay_plies\ttarget_mode\trow_support\tweighted_games\talready_cataloged\tsample_rows"
    val rows =
      candidates.map { candidate =>
        List(
          candidate.routeId,
          candidate.family,
          candidate.targetSquare,
          candidate.role,
          candidate.from,
          candidate.via.mkString("|"),
          candidate.to,
          candidate.maxReplayPlies.toString,
          candidate.targetMode,
          candidate.rowSupport.toString,
          candidate.weightedGames.toString,
          candidate.alreadyCataloged.toString,
          candidate.sampleRows.mkString(" | ")
        ).mkString("\t")
      }
    (header :: rows).mkString("", "\n", "\n")

  private def parseConfig(args: List[String]): Config =
    def loop(rest: List[String], config: Config): Config =
      rest match
        case Nil => config
        case "--openings" :: value :: tail => loop(tail, config.copy(openingsPath = Paths.get(value)))
        case "--routes" :: value :: tail   => loop(tail, config.copy(routesPath = Paths.get(value)))
        case "--out-json" :: value :: tail => loop(tail, config.copy(outJsonPath = Paths.get(value)))
        case "--out-tsv" :: value :: tail  => loop(tail, config.copy(outTsvPath = Paths.get(value)))
        case "--min-rows" :: value :: tail => loop(tail, config.copy(minRows = value.toIntOption.getOrElse(config.minRows)))
        case "--min-weighted-games" :: value :: tail =>
          loop(tail, config.copy(minWeightedGames = value.toIntOption.getOrElse(config.minWeightedGames)))
        case "--max-route-plies" :: value :: tail =>
          loop(tail, config.copy(maxRoutePlies = value.toIntOption.getOrElse(config.maxRoutePlies)))
        case _ :: tail => loop(tail, config)
    loop(args, Config())
