package controllers

import lila.app.*

final class Importer(env: Env) extends LilaController(env):

  def importGame = Open { _ ?=>
    fuccess(Redirect(routes.UserAnalysis.index))
  }

  def sendGame = Open { _ ?=>
    fuccess(Redirect(routes.UserAnalysis.index))
  }

  def apiSendGame = Open { _ ?=>
    fuccess(Redirect(routes.UserAnalysis.index))
  }
