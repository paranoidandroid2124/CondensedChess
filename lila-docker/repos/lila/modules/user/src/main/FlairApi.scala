package lila.user

case class Flair(name: String)

object FlairApi:
  def exists(flair: Flair): Boolean = false
  def find(name: String): Option[Flair] = None
  
  // Dummy formField for UserForm compatibility
  def formField(anyFlair: Boolean, asMod: Boolean): play.api.data.Mapping[Option[Flair]] =
    play.api.data.Forms.optional(play.api.data.Forms.text.transform[Flair](Flair(_), _.name))

  def formPair(asMod: Boolean) =
    "flair" -> formField(anyFlair = false, asMod = asMod)

  val adminFlairs: Set[Flair] = Set.empty

final class FlairApi(getFile: lila.common.config.GetRelativeFile)(using Executor)(using
    scheduler: Scheduler
):
  // No extends lila.core.user.FlairApi as it is removed from core
  
  import FlairApi.*
  export FlairApi.{ find, formField, adminFlairs }
  
  // No logic, purely Analysis Only
  def get(userId: UserId): Option[String] = None
