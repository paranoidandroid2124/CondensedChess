package views.user
package show

import lila.app.UiEnv.{ *, given }
import lila.user.{ Trophy, TrophyKind }

object otherTrophies:

  import bits.awards.*

  // Simplified - trophies, coach, streamer modules deleted
  def apply(info: lila.app.mashup.UserInfo)(using ctx: Context) =
    emptyFrag  // No trophies to display - modules deleted
