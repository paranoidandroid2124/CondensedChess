package lila.pref

final class SoundSet private (val key: String, val name: String):

  override def toString = key

  def cssClass = key

object SoundSet:

  val default = new SoundSet("sfx", "SFX")
  val silent = new SoundSet("silent", "Silent")
  val speech = new SoundSet("speech", "Speech")

  val list = List(
    silent,
    default,
    new SoundSet("piano", "Piano"),
    new SoundSet("nes", "NES"),
    new SoundSet("futuristic", "Futuristic"),
    speech
  )

  val allByKey = list.mapBy(_.key)
  val allByName = list.mapBy(_.name)

  def apply(key: String) = allByKey.getOrElse(key.toLowerCase, default)

  def contains(key: String) = allByKey contains key.toLowerCase

  def name2key(name: String): String =
    allByName.get(name).fold(name.toLowerCase)(_.key)
