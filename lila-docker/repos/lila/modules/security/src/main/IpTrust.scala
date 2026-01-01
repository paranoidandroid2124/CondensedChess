package lila.security

import play.api.mvc.RequestHeader
import lila.common.{ Fu, Executor, fuccess }
final class IpTrust(config: SecurityConfig)(using Executor):


  def isPubOrTor(req: RequestHeader): Fu[Boolean] =
    // Stub implementation for analysis-only
    fuccess(false)
