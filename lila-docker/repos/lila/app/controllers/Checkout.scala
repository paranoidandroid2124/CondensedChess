package controllers

import lila.app.*
import lila.llm.CreditConfig

final class Checkout(
    creditApi: lila.llm.CreditApi,
    env: Env
) extends LilaController(env):

  // Production checkout stays disabled until a real payment provider is wired.
  private val checkoutEnabled = env.mode.isDev

  def show = Auth { ctx ?=> me ?=>
    Ok.page(views.llm.checkout(me, checkoutEnabled)(using ctx))
  }

  def process = Auth { ctx ?=> me ?=>
    if !checkoutEnabled then
      Redirect("/plan?checkout=disabled").toFuccess
    else
      // Development-only sandbox upgrade flow.
      creditApi.upgradeTier(me.userId.value, CreditConfig.Tier.Pro).map { _ =>
        Redirect("/plan/checkout/success")
      }
  }

  def success = Auth { ctx ?=> me ?=>
    if !checkoutEnabled then Redirect("/plan?checkout=disabled").toFuccess
    else Ok.page(views.llm.checkoutSuccess(me)(using ctx))
  }
