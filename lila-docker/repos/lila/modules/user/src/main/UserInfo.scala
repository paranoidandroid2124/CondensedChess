package lila.user

case class UserInfo(
    user: User,
    nb: Int, // Corrected from User.Nb to Int
    nbFollowers: Int, // Placeholder
    nbFollowing: Int, // Placeholder
    nbBlockers: Option[Int]
)

object UserInfo:
  def apply(user: User, nb: Int, nbFollowers: Int, nbFollowing: Int, nbBlockers: Option[Int]): UserInfo =
    new UserInfo(user, nb, nbFollowers, nbFollowing, nbBlockers)
