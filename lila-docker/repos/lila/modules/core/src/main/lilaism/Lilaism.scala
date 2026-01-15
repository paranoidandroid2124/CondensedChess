package lila.core.lilaism

object Lilaism extends LilaLibraryExtensions:

  export chess.Color
  export lila.core.id.{
    GameId,
    StudyId,
    StudyChapterId,
    RoomId
  }
  export lila.core.userId.{ UserId, UserName, UserStr, MyId, UserIdOf }
  export lila.core.data.{ Markdown, Html, JsonStr, Url }
  export lila.core.perf.PerfKey
  export lila.core.email.EmailAddress
  export lila.core.user.{ User, Me, NbGames }
  export lila.core.game.{ Game, Pov }

  def some[A](a: A): Option[A] = Some(a)

  trait StringValue extends Any:
    def value: String
    override def toString = value
  given cats.Show[StringValue] = cats.Show.show(_.value)

  given cats.Show[play.api.mvc.Call] = cats.Show.show(_.url)


  import play.api.Mode
  extension (mode: Mode)
    inline def isDev = mode == Mode.Dev
    inline def isProd = mode == Mode.Prod
    inline def notProd = mode != Mode.Prod
