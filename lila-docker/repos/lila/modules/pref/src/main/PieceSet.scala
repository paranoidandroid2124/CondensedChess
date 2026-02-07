package lila.pref

import play.api.libs.json.*
import lila.common.Json.given

opaque type Featured = Boolean
object Featured extends YesNo[Featured]

case class PieceSet private[pref] (name: String, featured: Featured = Featured.No) extends lila.core.pref.PrefPieceSet:

  override def toString = name

sealed trait PieceSetObject:
  given Writes[PieceSet] = Json.writes[PieceSet]

  def default = all.head
  val all: List[PieceSet]

  lazy val allByName = all.mapBy(_.name)

  def get(name: String): PieceSet = allByName.getOrElse(name, default)
  def get(name: Option[String]): PieceSet = name.fold(default)(get)

  def contains(name: String) = allByName contains name

object PieceSet extends PieceSetObject:

  val all = List(
    PieceSet("cburnett", Featured.Yes), // 50/2 poll votes [for]/[against]
    PieceSet("merida", Featured.Yes), // 28/1
    PieceSet("pirouetti"), // 1/30
    PieceSet("chessnut"), // 8/21
    PieceSet("kosal", Featured.Yes), // 14/13
    PieceSet("fantasy"), // 11/20
    PieceSet("spatial"), // 6/22
    PieceSet("celtic"), // 3/24
    PieceSet("pixel"), // 9/23
    PieceSet("firi"),
    PieceSet("rhosgfx", Featured.Yes), // 0/0 submitted after poll
    PieceSet("governor"), // 14/15
    PieceSet("mpchess", Featured.Yes), // 17/12
    PieceSet("kiwen-suwi"), // 8/21
    PieceSet("shapes"), // 15/15
    PieceSet("letter") // 17/11
  )

object PieceSet3d extends PieceSetObject:

  val all = List(
    PieceSet("Basic", Featured.Yes), // 12/8
    PieceSet("Wood", Featured.Yes), // 13/4
    PieceSet("Metal"), // 5/12
    PieceSet("RedVBlue", Featured.Yes), // 10/7
    PieceSet("ModernJade"), // 4/13
    PieceSet("ModernWood", Featured.Yes), // 10/7
    PieceSet("Glass"), // 5/12
    PieceSet("Trimmed", Featured.Yes), // 10/7
    PieceSet("Experimental", Featured.Yes), // 9/9
    PieceSet("CubesAndPi", Featured.Yes) // 10/7
  )
