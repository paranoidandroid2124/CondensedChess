package lila.security

import lila.core.lilaism.Core.*
import lila.core.net.{ IpAddress, UserAgent }
import lila.user.User
import java.time.Instant
import scala.annotation.unused

case class UserLogins(
    ips: List[UserLogins.IPData],
    uas: List[Dated[UserAgent]]
)

final class UserLoginsApi(
    store: SessionStore,
    @unused userRepo: lila.user.UserRepo
)(using Executor):

  import UserLogins.*

  def apply(user: User, @unused maxOthers: Int): Fu[UserLogins] =
    store.chronoInfoByUser(user).map { infos =>
      UserLogins(
        ips = distinctRecent(infos.map(_.datedIp)).map(IPData.apply).toList,
        uas = distinctRecent(infos.map(_.datedUa)).toList
      )
    }

object UserLogins:

  case class IPData(ip: Dated[IpAddress])

  def distinctRecent[V](all: List[Dated[V]]): scala.collection.View[Dated[V]] =
    all
      .foldLeft(Map.empty[V, Instant]):
        case (acc, Dated(v, _)) if acc.contains(v) => acc
        case (acc, Dated(v, date)) => acc + (v -> date)
      .view
      .map { case (v, d) => Dated(v, d) }
