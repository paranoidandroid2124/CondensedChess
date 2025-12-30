package lila.pref

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ numberIn, stringIn, tolerantBoolean }

// Simplified PrefForm for analysis-only system
// Removed game-play fields: autoQueen, takeback, moretime, etc.
object PrefForm:

  private def containedIn(choices: Seq[(Int, String)]): Int => Boolean =
    choice => choices.exists(_._1 == choice)

  private def checkedNumber(choices: Seq[(Int, String)]) =
    number.verifying(containedIn(choices))

  private lazy val booleanNumber =
    number.verifying(Pref.BooleanPref.verify)

  object fields:
    val theme = "theme" -> text.verifying(Theme.contains(_))
    val theme3d = "theme3d" -> text.verifying(Theme3d.contains(_))
    val pieceSet = "pieceSet" -> text.verifying(PieceSet.contains(_))
    val pieceSet3d = "pieceSet3d" -> text.verifying(PieceSet3d.contains(_))
    val soundSet = "soundSet" -> text.verifying(SoundSet.contains(_))
    val bg = "bg" -> stringIn(Pref.Bg.fromString.keySet)
    val bgImg = "bgImg" -> text(maxLength = 400).verifying(
      "URL must use https",
      url => url.isBlank || url.startsWith("https://") || url.startsWith("//")
    )
    val is3d = "is3d" -> tolerantBoolean
    val zen = "zen" -> checkedNumber(Pref.Zen.choices)
    val voice = "voice" -> booleanNumber
    val keyboardMove = "keyboardMove" -> booleanNumber
    object board:
      val brightness = "boardBrightness" -> number(0, 150)
      val opacity = "boardOpacity" -> number(0, 100)
      val hue = "boardHue" -> number(0, 100)

  def pref(lichobile: Boolean) = Form(
    mapping(
      "display" -> mapping(
        "animation" -> numberIn(Set(0, 1, 2, 3)),
        "highlight" -> booleanNumber,
        "destination" -> booleanNumber,
        "coords" -> checkedNumber(Pref.Coords.choices),
        "pieceNotation" -> optional(booleanNumber),
        fields.zen.map2(optional),
        "resizeHandle" -> optional(checkedNumber(Pref.ResizeHandle.choices))
      )(DisplayData.apply)(unapply),
      "behavior" -> mapping(
        "moveEvent" -> optional(numberIn(Set(0, 1, 2))),
        fields.keyboardMove.map2(optional),
        fields.voice.map2(optional),
        "rookCastle" -> optional(booleanNumber)
      )(BehaviorData.apply)(unapply)
    )(PrefData.apply)(unapply)
  )

  case class DisplayData(
      animation: Int,
      highlight: Int,
      destination: Int,
      coords: Int,
      pieceNotation: Option[Int],
      zen: Option[Int],
      resizeHandle: Option[Int]
  )

  case class BehaviorData(
      moveEvent: Option[Int],
      keyboardMove: Option[Int],
      voice: Option[Int],
      rookCastle: Option[Int]
  )

  case class PrefData(
      display: DisplayData,
      behavior: BehaviorData
  ):
    def apply(pref: Pref) =
      pref.copy(
        highlight = display.highlight == 1,
        destination = display.destination == 1,
        coords = display.coords,
        animation = display.animation,
        keyboardMove = behavior.keyboardMove | pref.keyboardMove,
        voice = if pref.voice.isEmpty && !behavior.voice.contains(1) then None else behavior.voice,
        zen = display.zen | pref.zen,
        resizeHandle = display.resizeHandle | pref.resizeHandle,
        rookCastle = behavior.rookCastle | pref.rookCastle,
        pieceNotation = display.pieceNotation | pref.pieceNotation,
        moveEvent = behavior.moveEvent | pref.moveEvent
      )

  object PrefData:
    def apply(pref: Pref): PrefData =
      PrefData(
        display = DisplayData(
          highlight = if pref.highlight then 1 else 0,
          destination = if pref.destination then 1 else 0,
          animation = pref.animation,
          coords = pref.coords,
          zen = pref.zen.some,
          resizeHandle = pref.resizeHandle.some,
          pieceNotation = pref.pieceNotation.some
        ),
        behavior = BehaviorData(
          moveEvent = pref.moveEvent.some,
          keyboardMove = pref.keyboardMove.some,
          voice = pref.voice.getOrElse(0).some,
          rookCastle = pref.rookCastle.some
        )
      )

  def prefOf(p: Pref): Form[PrefData] = pref(lichobile = false).fill(PrefData(p))

  val cfRoutingForm = Form(single("cfRouting" -> boolean))
