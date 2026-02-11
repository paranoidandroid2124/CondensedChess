package controllers

import lila.app.*
import lila.llm.CreditApi

final class AnalystProPlan(
    creditApi: CreditApi,
    env: Env
) extends LilaController(env):

  def index = Auth { ctx ?=> me ?=>
    creditApi.remaining(me.userId.value).flatMap { status =>
      Ok.page(views.llm.plan(me, status)(using ctx))
    }
  }
