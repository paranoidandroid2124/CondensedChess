package lila.chessjudgment.analysis.opening

import scala.io.Source
import scala.util.Using

private[chessjudgment] final class OpeningRouteCatalog private (
    routesById: Map[String, OpeningRouteCatalog.Route]
):

  def routes: List[OpeningRouteCatalog.Route] =
    routesById.values.toList.sortBy(_.routeId)

  def route(routeId: String): Option[OpeningRouteCatalog.Route] =
    routesById.get(OpeningRouteCatalog.normalize(routeId))

  def routesForTarget(targetSquare: String): List[OpeningRouteCatalog.Route] =
    val target = OpeningRouteCatalog.normalize(targetSquare)
    routesById.values.filter(_.targetSquare == target).toList

private[chessjudgment] object OpeningRouteCatalog:

  final case class Route(
      routeId: String,
      family: String,
      targetSquare: String,
      role: String,
      from: String,
      via: List[String],
      to: String,
      maxReplayPlies: Int,
      targetMode: String = OpeningRouteCatalog.AttackWeakPawn
  ):
    def path: List[String] = from :: (via :+ to)

  private val ResourcePath = "lila/chessjudgment/openings/opening_routes.tsv"
  val AttackWeakPawn = "attack_weak_pawn"
  val OccupyTarget = "occupy_target"

  lazy val default: OpeningRouteCatalog =
    fromRows(loadResourceRows(ResourcePath))

  def fromTsvLines(lines: IterableOnce[String]): OpeningRouteCatalog =
    fromRows(parseRows(lines))

  private def fromRows(rows: Iterable[Route]): OpeningRouteCatalog =
    OpeningRouteCatalog(rows.map(row => row.routeId -> row).toMap)

  private def loadResourceRows(path: String): List[Route] =
    Option(getClass.getClassLoader.getResourceAsStream(path)).toList.flatMap: stream =>
      Using.resource(Source.fromInputStream(stream, "UTF-8")): source =>
        parseRows(source.getLines()).toList

  private def parseRows(lines: IterableOnce[String]): Iterable[Route] =
    lines.iterator
      .map(_.trim)
      .filter(line => line.nonEmpty && !line.startsWith("#"))
      .dropWhile(_.toLowerCase.startsWith("route_id\t"))
      .flatMap(parseLine)
      .toList

  private def parseLine(line: String): Option[Route] =
    line.split("\t", -1).toList match
      case routeId :: family :: target :: role :: from :: viaRaw :: to :: maxReplayRaw :: targetModeRaw :: _ =>
        parseRoute(routeId, family, target, role, from, viaRaw, to, maxReplayRaw, Some(targetModeRaw))
      case routeId :: family :: target :: role :: from :: viaRaw :: to :: maxReplayRaw :: Nil =>
        parseRoute(routeId, family, target, role, from, viaRaw, to, maxReplayRaw, None)
      case _ => None

  private def parseRoute(
      routeId: String,
      family: String,
      target: String,
      role: String,
      from: String,
      viaRaw: String,
      to: String,
      maxReplayRaw: String,
      targetModeRaw: Option[String]
  ): Option[Route] =
        val normalizedVia = splitList(viaRaw).map(normalize)
        val maxReplayPlies = maxReplayRaw.trim.toIntOption.getOrElse(8).max(0)
        val normalizedTarget = normalize(target)
        val normalizedTo = normalize(to)
        val targetMode =
          targetModeRaw.map(normalize).filter(_.nonEmpty).getOrElse(defaultTargetMode(normalizedTarget, normalizedTo))
        val route =
          Route(
            routeId = normalize(routeId),
            family = normalize(family),
            targetSquare = normalizedTarget,
            role = normalize(role),
            from = normalize(from),
            via = normalizedVia,
            to = normalizedTo,
            maxReplayPlies = maxReplayPlies,
            targetMode = targetMode
          )
        Option.when(
          route.routeId.nonEmpty &&
            route.family.nonEmpty &&
            validSquare(route.targetSquare) &&
            validSquare(route.from) &&
            route.via.forall(validSquare) &&
            validSquare(route.to) &&
            validTargetMode(route.targetMode) &&
            route.path.distinct.size == route.path.size
        )(route)

  private def splitList(raw: String): List[String] =
    Option(raw).getOrElse("").split("[|]").toList.map(_.trim).filter(_.nonEmpty)

  private[opening] def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def validSquare(square: String): Boolean =
    square.matches("""[a-h][1-8]""")

  private def defaultTargetMode(target: String, to: String): String =
    if target == to then OccupyTarget else AttackWeakPawn

  private def validTargetMode(targetMode: String): Boolean =
    targetMode == AttackWeakPawn || targetMode == OccupyTarget
