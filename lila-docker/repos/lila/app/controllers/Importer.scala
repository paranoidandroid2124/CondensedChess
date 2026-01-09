package controllers

import play.api.mvc.*
import lila.app.{ *, given }

final class Importer(env: Env) extends LilaController(env):

  def importGame = Open { _ ?=>
    fuccess(Ok("Import game stub"))
  }

  def sendGame = Open { _ ?=>
    fuccess(NotFound)
  }

  def apiSendGame = Open { _ ?=>
    fuccess(NotFound)
  }
