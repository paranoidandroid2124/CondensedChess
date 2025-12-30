package lila.security

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
