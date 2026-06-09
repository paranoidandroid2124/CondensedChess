package lila.commentary.tools.opening

import play.api.libs.json.*

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object OpeningMasterDbAudit:

  val DefaultMastersBaseUrl = "https://explorer.lichess.org/masters"

  private val YearPattern = """\b(\d{4})\b""".r

  def extractYear(value: String): Option[String] =
    Option(value).flatMap(v => YearPattern.findFirstMatchIn(v.trim).map(_.group(1)))

  enum QueryMode:
    case Fen, Play

  final case class MastersRequest(rowId: String, mode: QueryMode, url: String)

  final case class MastersEvidence(
      totalGames: Int,
      openingEco: Option[String],
      openingName: Option[String],
      topGameIds: List[String]
  ):
    def isMasterBacked: Boolean = totalGames > 0 || topGameIds.nonEmpty

  def mastersRequest(
      row: OpeningPoolAudit.AuditedRow,
      mode: QueryMode,
      baseUrl: String = DefaultMastersBaseUrl,
      since: Option[String] = None,
      until: Option[String] = None
  ): MastersRequest =
    val query =
      mode match
        case QueryMode.Play if row.uciPlay.nonEmpty =>
          "play" -> row.uciPlay.mkString(",")
        case _ =>
          "fen" -> row.endpointKey.map(endpoint => s"$endpoint 0 1").getOrElse("")
    val cleanSince = since.flatMap(extractYear)
    val cleanUntil = until.flatMap(extractYear)
    val params =
      List(query, "moves" -> "0", "topGames" -> "3") ++
        cleanSince.map("since" -> _) ++
        cleanUntil.map("until" -> _)
    val encodedParams =
      params
        .map { case (key, value) => s"$key=${URLEncoder.encode(value, StandardCharsets.UTF_8)}" }
        .mkString("&")
    MastersRequest(
      rowId = row.stableId,
      mode = mode,
      url = s"${baseUrl.stripSuffix("?")}?$encodedParams"
    )

  def parseMastersResponse(raw: String): Either[String, MastersEvidence] =
    try
      val json = Json.parse(raw)
      val total =
        (json \ "white").asOpt[Int].getOrElse(0) +
          (json \ "draws").asOpt[Int].getOrElse(0) +
          (json \ "black").asOpt[Int].getOrElse(0)
      val opening = (json \ "opening").asOpt[JsObject]
      val topGameIds =
        (json \ "topGames")
          .asOpt[List[JsObject]]
          .getOrElse(Nil)
          .flatMap(game => (game \ "id").asOpt[String])
      Right(
        MastersEvidence(
          totalGames = total,
          openingEco = opening.flatMap(js => (js \ "eco").asOpt[String]),
          openingName = opening.flatMap(js => (js \ "name").asOpt[String]),
          topGameIds = topGameIds
        )
      )
    catch
      case e: Throwable => Left(e.getMessage)
