package lila.security

import play.api.mvc.RequestHeader
import lila.core.net.IpAddress
import lila.core.security.{ Ip2ProxyApi, IsProxy }

final class Ip2ProxySkip extends Ip2ProxyApi:
  def ofReq(req: RequestHeader): Fu[IsProxy] = fuccess(IsProxy.empty)
  def ofIp(ip: IpAddress): Fu[IsProxy] = fuccess(IsProxy.empty)
  def keepProxies(ips: Seq[IpAddress]): Fu[Map[IpAddress, String]] = fuccess(Map.empty)
