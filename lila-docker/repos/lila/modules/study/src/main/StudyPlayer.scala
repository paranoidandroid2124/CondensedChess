package lila.study

import chess.{ ByColor, PlayerName, PlayerTitle, FideId, Centis, IntRating }
import chess.format.pgn.Tags

// Chesstory: removed Federation - analysis system doesn't need it
type Players = ByColor[StudyPlayer]

case class StudyPlayer(
    fideId: Option[FideId],
    title: Option[PlayerTitle],
    name: Option[PlayerName],
    rating: Option[IntRating],
    team: Option[String]
):
  def id: Option[StudyPlayer.Id] = fideId.filter(_ != FideId(0)).orElse(name)

object StudyPlayer:

  type Id = FideId | PlayerName

  def fromTags(tags: Tags): Option[Players] =
    val names = tags.names
    Option.when(names.exists(_.isDefined)):
      val ratings = tags.ratings.map(_.filter(_ > IntRating(0)))
      (tags.fideIds, tags.titles, names, ratings, tags.teams).mapN(StudyPlayer.apply)

  object json:
    import play.api.libs.json.*
    import lila.common.Json.given
    given studyPlayerWrites: OWrites[StudyPlayer] =
      OWrites: p =>
        Json.obj(
          "name" -> p.name,
          "title" -> p.title,
          "rating" -> p.rating,
          "fideId" -> p.fideId,
          "team" -> p.team
        ).value.foldLeft(Json.obj()):
          case (obj, (k, JsNull)) => obj
          case (obj, (k, v)) => obj + (k -> v)

    given chapterPlayerWrites: OWrites[ChapterPlayer] = OWrites: p =>
      Json.toJsObject(p.player) + ("clock" -> Json.toJson(p.clock))

case class ChapterPlayer(player: StudyPlayer, clock: Option[Centis]):
  export player.*

object ChapterPlayer:

  def fromTags(tags: Tags, clocks: Chapter.BothClocks): Option[ByColor[ChapterPlayer]] =
    StudyPlayer
      .fromTags(tags)
      .map:
        _.zip(clocks).map: (player, clock) =>
          ChapterPlayer(player, clock)

