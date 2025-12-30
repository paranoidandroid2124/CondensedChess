package lila.web

object AnnounceApi:

  def cli(words: List[String]): Fu[String] = fuccess("Announce disabled (no socket)")

  def get: Option[Nothing] = None

  def cancel = ()
