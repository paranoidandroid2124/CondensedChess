package controllers

import lila.app.*
import lila.llm.CreditApi

final class AnalystProPlan(
    creditApi: CreditApi,
    env: Env
) extends LilaController(env):

  private val checkoutEnabled = env.mode.isDev

  def index = Open: ctx ?=>
    ctx.me match
      case Some(me) =>
        creditApi.remaining(me.userId.value).flatMap { status =>
          Ok.page(views.llm.plan(Some(me), status, checkoutEnabled)(using ctx))
        }
      case None =>
        // Guest preview
        val dummyStatus = lila.llm.CreditApi.CreditStatus(
          remaining = 150,
          maxCredits = 150,
          tier = "free",
          resetAt = java.time.Instant.now.plus(java.time.Duration.ofDays(30))
        )
        Ok.page(views.llm.plan(None, dummyStatus, checkoutEnabled)(using ctx))
