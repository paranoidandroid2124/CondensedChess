package lila.beta

import com.softwaremill.macwire.*

import lila.core.config.*
import lila.core.user.UserApi

@Module
final class Env(
    mongo: lila.db.Env,
    userApi: UserApi
)(using Executor):

  private val feedbackEventColl = mongo.mainDb(CollName("beta_feedback_event"))
  private val waitlistLeadColl = mongo.mainDb(CollName("beta_waitlist_lead"))

  lazy val api = new BetaFeedbackApi(
    feedbackEventColl = feedbackEventColl,
    waitlistLeadColl = waitlistLeadColl,
    userApi = userApi
  )
