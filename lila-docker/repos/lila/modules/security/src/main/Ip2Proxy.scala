package lila.security

import play.api.mvc.RequestHeader
import lila.core.net.IpAddress
import lila.core.security.{ Ip2ProxyApi, IsProxy }
import lila.core.lilaism.Core.*

final class Ip2ProxySkip extends Ip2ProxyApi:
  def ofReq(req: RequestHeader): Fu[IsProxy] = Future.successful(IsProxy.empty)
  def ofIp(ip: IpAddress): Fu[IsProxy] = Future.successful(IsProxy.empty)
  def keepProxies(ips: Seq[IpAddress]): Fu[Map[IpAddress, String]] = Future.successful(Map.empty)
