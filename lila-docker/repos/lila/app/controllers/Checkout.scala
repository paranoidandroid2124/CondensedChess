package controllers

import lila.app.*
import lila.llm.CreditConfig

final class Checkout(
    creditApi: lila.llm.CreditApi,
    env: Env
) extends LilaController(env):

  def show = Auth { ctx ?=> me ?=>
    Ok.page(views.llm.checkout(me)(using ctx))
  }

  def process = Auth { ctx ?=> me ?=>
    // Simulate payment processing delay or validation
    creditApi.upgradeTier(me.userId.value, CreditConfig.Tier.Pro).map { _ =>
      Redirect("/plan/checkout/success")
    }
  }

  def success = Auth { ctx ?=> me ?=>
    Ok.page(views.llm.checkoutSuccess(me)(using ctx))
  }
