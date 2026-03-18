package lila.security

import play.api.mvc.RequestHeader
import reactivemongo.api.bson.*
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }

import scala.concurrent.blocking

import lila.common.HTTPRequest
import lila.core.userId.UserId
import lila.core.user.User
import lila.core.lilaism.Core.*
import lila.core.id.SessionId
import lila.core.net.{ ApiVersion, IpAddress, UserAgent }
import lila.core.security.IsProxy
import lila.db.dsl.{ *, given }
import java.time.Instant
import scala.concurrent.duration.*
import reactivemongo.api.bson.Macros

case class AuthInfo(userId: UserId)

final class SessionStore(val coll: Coll, cacheApi: lila.memo.CacheApi)(using executor: Executor):

  import SessionStore.*

  private val authCache = cacheApi[SessionId, Option[AuthInfo]](65_536, "security.session.info"):
    _.expireAfterAccess(5.minutes).buildAsyncFuture[SessionId, Option[AuthInfo]]: id =>
      coll
        .find($doc("_id" -> id, "up" -> true), authInfoProjection.some)
        .one[Bdoc]
        .map:
          _.flatMap: doc =>
            if doc.getAsOpt[Instant]("date").forall(_.isBefore(nowInstant.minusHours(12))) then
              coll.updateFieldUnchecked($id(id), "date", nowInstant)
            doc.getAsOpt[UserId]("user").map(AuthInfo.apply)

  def authInfo(sessionId: SessionId) = authCache.get(sessionId)

  private val authInfoProjection = $doc("user" -> true, "date" -> true, "_id" -> false)
  private def uncache(sessionId: SessionId) =
    blocking { blockingUncache(sessionId) }
  private def uncacheAllOf(userId: UserId): Funit =
    coll.distinctEasy[SessionId, Seq]("_id", $doc("user" -> userId)).map { ids =>
      blocking:
        ids.foreach(blockingUncache)
    }

  private def blockingUncache(sessionId: SessionId) =
    authCache.underlying.synchronous.invalidate(sessionId)

  private[security] def save(
      sessionId: SessionId,
      userId: UserId,
      req: RequestHeader,
      apiVersion: Option[ApiVersion],
      up: Boolean,
      proxy: IsProxy,
      pwned: Boolean
  ): Funit =
    coll.insert
      .one:
        $doc(
          "_id" -> sessionId,
          "user" -> userId,
          "ip" -> HTTPRequest.ipAddress(req),
          "ua" -> HTTPRequest.userAgent(req).some.filter(_ != UserAgent.zero),
          "date" -> nowInstant,
          "up" -> up,
          "api" -> apiVersion,
          "proxy" -> proxy.name,
          "pwned" -> pwned.option(true)
        )
      .void


  def delete(sessionId: SessionId): Funit =
    for _ <- coll.update.one($id(sessionId), $set("up" -> false))
    yield uncache(sessionId)

  def closeUserAndSessionId(userId: UserId, sessionId: SessionId): Funit =
    for _ <- coll.update.one($doc("user" -> userId, "_id" -> sessionId, "up" -> true), $set("up" -> false))
    yield uncache(sessionId)

  def closeUserExceptSessionId(userId: UserId, sessionId: SessionId): Funit =
    for _ <- coll.update.one(
        $doc("user" -> userId, "_id" -> $ne(sessionId), "up" -> true),
        $set("up" -> false),
        multi = true
      )
    yield uncacheAllOf(userId)

  def closeAllSessionsOf(userId: UserId): Funit =
    for _ <- coll.update.one(
        $doc("user" -> userId, "up" -> true),
        $set("up" -> false),
        multi = true
      )
    yield uncacheAllOf(userId)

  def deleteAllSessionsOf(userId: UserId): Funit =
    for _ <- coll.delete.one($doc("user" -> userId))
    yield uncacheAllOf(userId)

  def openSessions(userId: UserId, nb: Int): Fu[List[UserSession]] =
    coll
      .find($doc("user" -> userId, "up" -> true))
      .sort($doc("date" -> -1))
      .cursor[UserSession]()
      .list(nb)

  def allSessions(userId: UserId): AkkaStreamCursor[UserSession] =
    coll
      .find($doc("user" -> userId))
      .sort($doc("date" -> -1))
      .cursor[UserSession](ReadPref.sec)

  def chronoInfoByUser(user: User): Fu[List[Info]] =
    coll
      .find(
        $doc(
          "user" -> user.id,
          "date".$gt(user.createdAt.atLeast(nowInstant.minusYears(1)))
        ),
        $doc("_id" -> false, "ip" -> true, "ua" -> true, "date" -> true).some
      )
      .sort($sort.desc("date"))
      .cursor[Info]()
      .list(1000)

  private[security] def deletePreviousSessions(user: User) =
    coll.delete.one($doc("user" -> user.id, "date".$lt(user.createdAt))).void

  private case class DedupInfo(_id: SessionId, ip: String, ua: String):
    def compositeKey = s"$ip $ua"
  private given BSONDocumentReader[DedupInfo] = Macros.reader
  def dedup(userId: UserId, keepSessionId: SessionId): Funit =
    coll
      .find(
        $doc(
          "user" -> userId,
          "up" -> true
        )
      )
      .sort($doc("date" -> -1))
      .cursor[DedupInfo]()
      .list(1000)
      .flatMap { sessions =>
        val olds = sessions
          .groupBy(_.compositeKey)
          .view
          .values
          .flatMap(_.drop(1))
          .filter(_._id != keepSessionId)
          .map(_._id)
        coll.delete.one($inIds(olds)).void
      } >> uncacheAllOf(userId)

  def ips(user: User): Fu[Set[IpAddress]] =
    coll.distinctEasy[IpAddress, Set]("ip", $doc("user" -> user.id))

  private[security] def recentByIpExists(ip: IpAddress, since: FiniteDuration): Fu[Boolean] =
    coll.secondary.exists:
      $doc("ip" -> ip, "date" -> $gt(nowInstant.minusMinutes(since.toMinutes.toInt)))

object SessionStore:
  case class Info(ip: IpAddress, ua: UserAgent, date: Instant):
    def datedIp = Dated(ip, date)
    def datedUa = Dated(ua, date)
  given BSONDocumentReader[Info] = Macros.reader[Info]
