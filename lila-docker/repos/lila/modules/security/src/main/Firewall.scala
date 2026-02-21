package lila.security

import play.api.mvc.RequestHeader
import lila.core.net.IpAddress
import lila.db.dsl.{ *, given }
import lila.core.lilaism.Core.*
import lila.common.{ so => _, nowInstant => _, * }
import scala.concurrent.duration.*
import scala.annotation.unused

final class Firewall(
    coll: Coll,
    @unused config: SecurityConfig,
    scheduler: Scheduler
)(using Executor):

  private var current: Set[String] = Set.empty

  scheduler.scheduleOnce(49.seconds):
    loadFromDb()

  def blocksIp(ip: IpAddress): Boolean = current.contains(ip.value)

  def blocks(req: RequestHeader): Boolean =
    blocksIp(lila.common.HTTPRequest.ipAddress(req))

  def accepts(req: RequestHeader): Boolean = !blocks(req)

  def blockIps(ips: Iterable[IpAddress]): Funit =
    if ips.isEmpty then Future.successful(())
    else
      for
        _ <- ips.toList.sequentiallyVoid: ip =>
          coll.update.one(
            $id(ip),
            $doc("_id" -> ip, "date" -> nowInstant),
            upsert = true
          )
        _ <- loadFromDb()
      yield ()

  def unblockIps(ips: Iterable[IpAddress]): Funit =
    if ips.isEmpty then Future.successful(())
    else
      for _ <- coll.delete.one($inIds(ips)) yield loadFromDb()

  private def loadFromDb(): Funit =
    coll.distinctEasy[String, Set]("_id", $empty).map { ips =>
      current = ips
    }
