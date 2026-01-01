package lila.security

import lila.core.net.ApiVersion

// Stub for Mobile API - Study-only system
object Mobile:
  object Api:
    val currentVersion: Int = 0
    def requestVersion(req: play.api.mvc.RequestHeader): Option[ApiVersion] = None
