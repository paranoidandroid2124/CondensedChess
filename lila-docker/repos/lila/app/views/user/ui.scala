package views.user

import lila.app.UiEnv.{ *, given }

lazy val bits = new:
  def apply(args: Any*)(using Context) = emptyFrag
lazy val download = new:
  def apply(args: Any*)(using Context) = emptyFrag

def gamesAll(u: User, cross: Option[Any])(using Context) = emptyFrag
def games(u: User, cross: Option[Any])(using Context) = emptyFrag
