package lila.security

import lila.common.*
import lila.oauth.EndpointScopes
import lila.oauth.OAuthServer.*

final class SecurityApi(
    userRepo: lila.user.UserRepo,
    sessionStore: SessionStore
)(using Executor):

  def restoreUser(req: play.api.mvc.RequestHeader): Fu[Option[lila.user.User]] =
    // Simplified analysis-only implementation
    fuccess(none)
    
  def oauthScoped(req: play.api.mvc.RequestHeader, accepted: EndpointScopes): lila.oauth.OAuthServer.AuthFu =
    lila.oauth.OAuthServer.NoSuchToken.raise
