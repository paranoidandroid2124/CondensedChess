package lila.ops

import lila.core.config.{ BaseUrl, CollName }
import lila.core.lilaism.Core.*

final class Env(
    db: lila.db.Db,
    userEnv: lila.user.Env,
    securityEnv: lila.security.Env,
    mailerEnv: lila.mailer.Env,
    baseUrl: BaseUrl
)(using Executor):

  lazy val api = new OpsApi(
    userRepo = userEnv.repo,
    sessionStore = securityEnv.sessionStore,
    passwordResetToken = securityEnv.passwordResetToken,
    automaticEmail = mailerEnv.automaticEmail,
    auditColl = db(CollName("ops_member_audit")),
    planLedgerColl = db(CollName("user_plan_ledger")),
    baseUrl = baseUrl
  )
