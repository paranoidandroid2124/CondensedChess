package lila.oauth

import lila.core.misc.oauth.AccessTokenId

sealed abstract class OAuthScope(val key: String, val name: String):
  override def toString = s"Scope($key)"

opaque type OAuthScopes = List[OAuthScope]
object OAuthScopes extends TotalWrapper[OAuthScopes, List[OAuthScope]]:
  extension (e: OAuthScopes)
    def has(s: OAuthScope): Boolean = e contains s
    def has(s: OAuthScope.Selector): Boolean = has(s(OAuthScope))
    def keyList: String = e.map(_.key).mkString(",")
    def intersects(other: OAuthScopes): Boolean = e.exists(other.has)
    def isEmpty = e.isEmpty

opaque type TokenScopes = List[OAuthScope]
object TokenScopes extends TotalWrapper[TokenScopes, List[OAuthScope]]:
  extension (e: TokenScopes)
    def intersects(other: OAuthScopes): Boolean = e.exists(other.contains)
    def has(s: OAuthScope.Selector): Boolean = e.contains(s(OAuthScope))

opaque type EndpointScopes = List[OAuthScope]
object EndpointScopes extends TotalWrapper[EndpointScopes, List[OAuthScope]]:
  extension (e: EndpointScopes)
    def isEmpty = e.isEmpty
    def compatible(token: TokenScopes): Boolean = e.exists(token.has)
    def show = e.map(_.key).mkString(" || ")

object OAuthScope:

  given Eq[OAuthScope] = Eq.fromUniversalEquals

  object Preference:
    case object Read extends OAuthScope("preference:read", "Read preferences")
    case object Write extends OAuthScope("preference:write", "Write preferences")

  object Email:
    case object Read extends OAuthScope("email:read", "Read email address")

  object Challenge:
    case object Read extends OAuthScope("challenge:read", "Read challenges")
    case object Write extends OAuthScope("challenge:write", "Write challenges")
    case object Bulk extends OAuthScope("challenge:bulk", "Bulk challenges")

  object Study:
    case object Read extends OAuthScope("study:read", "Read studies")
    case object Write extends OAuthScope("study:write", "Write studies")

  object Tournament:
    case object Write extends OAuthScope("tournament:write", "Write tournaments")

  object Racer:
    case object Write extends OAuthScope("racer:write", "Write racer")

  object Puzzle:
    case object Read extends OAuthScope("puzzle:read", "Read puzzles")
    case object Write extends OAuthScope("puzzle:write", "Solve puzzles")

  object Team:
    case object Read extends OAuthScope("team:read", "Read teams")
    case object Write extends OAuthScope("team:write", "Write teams")
    case object Lead extends OAuthScope("team:lead", "Lead teams")

  object Follow:
    case object Read extends OAuthScope("follow:read", "Read follows")
    case object Write extends OAuthScope("follow:write", "Write follows")

  object Msg:
    case object Write extends OAuthScope("msg:write", "Write messages")

  object Board:
    case object Play extends OAuthScope("board:play", "Play with board")

  object Bot:
    case object Play extends OAuthScope("bot:play", "Play as bot")

  object Engine:
    case object Read extends OAuthScope("engine:read", "Read engine")
    case object Write extends OAuthScope("engine:write", "Write engine")

  object Web:
    case object Login extends OAuthScope("web:login", "Web login")
    case object Mobile extends OAuthScope("web:mobile", "Official Lichess mobile app")
    case object Mod extends OAuthScope("web:mod", "Web mod")

  case class Scoped(me: Me, scopes: TokenScopes):
    def user: User = me.value

  case class Access(scoped: Scoped, tokenId: AccessTokenId):
    export scoped.*

  type Selector = OAuthScope.type => OAuthScope

  val all: List[OAuthScope] = List(
    Preference.Read,
    Preference.Write,
    Email.Read,
    Challenge.Read,
    Challenge.Write,
    Challenge.Bulk,
    Study.Read,
    Study.Write,
    Tournament.Write,
    Racer.Write,
    Puzzle.Read,
    Puzzle.Write,
    Team.Read,
    Team.Write,
    Team.Lead,
    Follow.Read,
    Follow.Write,
    Msg.Write,
    Board.Play,
    Bot.Play,
    Engine.Read,
    Engine.Write,
    Web.Login,
    Web.Mobile,
    Web.Mod
  )

  val classified: List[(String, List[OAuthScope])] = List(
    "User account" -> List(Email.Read, Preference.Read, Preference.Write, Web.Mod),
    "Interactions" -> List(Follow.Read, Follow.Write, Msg.Write),
    "Play games" -> List(Challenge.Read, Challenge.Write, Challenge.Bulk, Tournament.Write),
    "Teams" -> List(Team.Read, Team.Write, Team.Lead),
    "Puzzles" -> List(Puzzle.Read, Puzzle.Write, Racer.Write),
    "Studies & Broadcasts" -> List(Study.Read, Study.Write),
    "External play" -> List(Board.Play, Bot.Play),
    "External engine" -> List(Engine.Read, Engine.Write)
  )

  val dangerList: OAuthScopes = OAuthScope.select(
    _.Team.Lead,
    _.Web.Login,
    _.Web.Mod,
    _.Web.Mobile,
    _.Msg.Write
  )

  val relevantToMods: OAuthScopes = OAuthScope.select(
    _.Team.Lead,
    _.Web.Login,
    _.Web.Mobile,
    _.Msg.Write,
    _.Board.Play
  )

  val byKey: Map[String, OAuthScope] = all.mapBy(_.key)

  def select(selectors: Iterable[Selector]): OAuthScopes =
    OAuthScopes(selectors.map(_(OAuthScope)).toList)
  def select(selectors: Selector*): OAuthScopes = select(selectors)

  def canUseWebMod(using Option[Me]) =
    import lila.core.perm.*
    List[Permission.Selector](_.Shusher, _.BoostHunter, _.CheatHunter, _.StudyAdmin, _.ApiChallengeAdmin)
      .exists(Granter.opt)

  import reactivemongo.api.bson.*
  import lila.db.dsl.*
  private[oauth] given BSONHandler[OAuthScope] = tryHandler[OAuthScope](
    { case b: BSONString => OAuthScope.byKey.get(b.value).toTry(s"No such scope: ${b.value}") },
    s => BSONString(s.key)
  )
