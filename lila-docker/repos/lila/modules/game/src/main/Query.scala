package lila.game

import lila.db.dsl.*
import lila.core.game.Source

object Query:
  val finished = $doc("s" -> $doc("$gte" -> 20))
  def user(u: UserId) = $doc("u" -> u.value)
  def win(u: UserId) = $doc("u" -> u.value, "w" -> true)
  def loss(u: UserId) = $doc("u" -> u.value, "w" -> false)
  def draw(u: UserId) = $doc("u" -> u.value, "w" -> $doc("$exists" -> false))
  def imported(u: UserId) = $doc("u" -> u.value, "s" -> Source.Import.id)
  def users(ids: List[UserId]) = $doc("u" -> $doc("$in" -> ids.map(_.value)))
  def analysed(v: Boolean) = $doc("a" -> v)
  def turns(r: Range) = $doc("t" -> $doc("$gte" -> r.start, "$lte" -> r.end))
  def nowPlaying(u: UserId) = $doc("u" -> u.value, "s" -> $doc("$lt" -> 20))
  def started = $doc("s" -> $doc("$gte" -> 20))
  def playable = $doc("s" -> $doc("$lt" -> 30))
  val sortCreated = $sort.desc("ca")
  val sortCreatedAsc = $sort.asc("ca")
