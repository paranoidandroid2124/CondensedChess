package lila.web

import java.time.Instant
import reactivemongo.api.bson.{ Macros, BSONHandler, BSONValue, BSONLong, BSONDocument, BSONDocumentHandler }
import lila.db.dsl.*
import scala.concurrent.duration.*
import scala.util.{ Try, Success, Failure }

// Package-level givens for implicit resolution

given BSONHandler[Instant] with
  def readTry(v: BSONValue) = v.asTry[Long].map(Instant.ofEpochMilli)
  def writeTry(v: Instant) = Success(BSONLong(v.toEpochMilli))

case class Announce(
    _id: String,
    text: String,
    date: Instant,
    url: Option[String]
):
  def json = play.api.libs.json.Json.obj(
    "id" -> _id,
    "text" -> text,
    "date" -> date.toEpochMilli,
    "url" -> url
  )

// Use Macros.handler for robust BSONDocumentHandler generation
given BSONDocumentHandler[Announce] = Macros.handler[Announce]

final class AnnounceApi(coll: Coll)(using exec: Executor, scheduler: Scheduler):

  // Register instance for static CLI access
  AnnounceApi.setInstance(this)

  private var cache: Option[Announce] = None

  def current: Option[Announce] = cache
  
  def getCurrent: Fu[Option[Announce]] = fuccess(cache)

  private def refresh: Funit =
    coll.find($empty).sort($sort.desc("date")).one[Announce].map: a =>
      cache = a

  // Periodically refresh
  scheduler.scheduleWithFixedDelay(10.seconds, 1.minute)(() => { refresh; () })
  
  // Initial refresh
  refresh

  def add(text: String, url: Option[String]): Funit =
    coll.insert.one(Announce(
      _id = java.util.UUID.randomUUID().toString.take(8),
      text = text,
      date = Instant.now,
      url = url
    )).flatMap(_ => refresh)

  def cli(words: List[String]): Fu[String] = words match
    case "add" :: text :: rest =>
      val url = rest.headOption
      add(text, url) inject "Announcement added"
    case "get" :: Nil =>
      fuccess(current.fold("No announcement")(_.toString))
    case _ => fuccess("usage: announce add <text> [url] | get")

object AnnounceApi:
  private var instance: Option[AnnounceApi] = None
  def setInstance(api: AnnounceApi) = instance = Some(api)
  
  def cli(words: List[String]): Fu[String] = 
    instance.fold(fuccess("AnnounceApi not ready"))(_.cli(words))
