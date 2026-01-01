
package lila.security

import lila.core.userId.UserId
import lila.core.security.ClearPassword
import lila.core.lilaism.Core.{ *, given }
import scalalib.future.extensions.*

final class Cli(
    authenticator: Authenticator,
    firewall: Firewall
)(using Executor):

  lila.common.Cli.handle:
    case "password" :: id :: password :: Nil =>
      authenticator.setPassword(UserId(id), ClearPassword(password)) inject "Password updated"
    case "firewall" :: "block" :: ips =>
      firewall.blockIps(ips.flatMap(lila.core.net.IpAddress.from)) inject "IPs blocked"
    case "firewall" :: "unblock" :: ips =>
      firewall.unblockIps(ips.flatMap(lila.core.net.IpAddress.from)) inject "IPs unblocked"
