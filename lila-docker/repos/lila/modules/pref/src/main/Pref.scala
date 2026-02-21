package lila.pref

import reactivemongo.api.bson.Macros.Annotations.Key

// Simplified Pref for analysis-only system
// Removed: message, studyInvite, insightShare, blogFilter, follow, challenge, etc.
case class Pref(
    @Key("_id") id: UserId,
    bg: Int,
    bgImg: Option[String],
    is3d: Boolean,
    theme: String,
    pieceSet: String,
    theme3d: String,
    pieceSet3d: String,
    soundSet: String,
    animation: Int,
    highlight: Boolean,
    destination: Boolean,
    coords: Int,
    keyboardMove: Int,
    voice: Option[Int],
    zen: Int,
    rookCastle: Int,
    moveEvent: Int,
    pieceNotation: Int,
    resizeHandle: Int,
    board: Pref.BoardPref,
    tags: Map[String, String] = Map.empty
) extends lila.core.pref.Pref:

  import Pref.*

  def realTheme = Theme(theme)
  def realPieceSet = PieceSet.get(pieceSet)
  def realTheme3d = Theme3d(theme3d)
  def realPieceSet3d = PieceSet3d.get(pieceSet3d)

  val themeColorLight = "#dbd7d1"
  val themeColorDark = "#2e2a24"
  def themeColor = if bg == Bg.LIGHT then themeColorLight else themeColorDark
  def themeColorClass =
    if bg == Bg.LIGHT then "light".some
    else if bg == Bg.TRANSPARENT then "transp".some
    else if bg == Bg.SYSTEM then none
    else if bg == Bg.PRO then "pro".some
    else "dark".some

  def realSoundSet = SoundSet(soundSet)

  def coordsClass = Coords.classOf(coords)

  def hasDgt = tags contains Tag.dgt

  def animationMillis: Int =
    animation match
      case Animation.NONE => 0
      case Animation.FAST => 120
      case Animation.NORMAL => 250
      case Animation.SLOW => 500
      case _ => 250

  def bgImgOrDefault = bgImg | Pref.defaultBgImg

  def pieceNotationIsLetter: Boolean = pieceNotation == PieceNotation.LETTER

  def isZen = zen == Zen.YES
  def isZenAuto = zen == Zen.GAME_AUTO

  def is2d = !is3d

  def hasKeyboardMove = keyboardMove == KeyboardMove.YES
  def hasVoice = voice.has(Voice.YES)
  def hasSpeech = soundSet == SoundSet.speech.toString

  def botCompatible =
    theme == "brown" &&
      pieceSet == "cburnett" &&
      is2d &&
      animation == Animation.NONE &&
      highlight &&
      coords == Coords.OUTSIDE

  def simpleBoard =
    board.hue == 0 && board.brightness == 100 && (board.opacity == 100 || bg != Bg.TRANSPARENT)

  def currentTheme = Theme(theme)
  def currentTheme3d = Theme3d(theme3d)
  def currentPieceSet = PieceSet.get(pieceSet)
  def currentPieceSet3d = PieceSet3d.get(pieceSet3d)
  def currentSoundSet = SoundSet(soundSet)
  def currentBg: String =
    if bg == Pref.Bg.TRANSPARENT then "transp"
    else if bg == Pref.Bg.LIGHT then "light"
    else if bg == Pref.Bg.SYSTEM then "system"
    else if bg == Pref.Bg.PRO then "pro"
    else "dark"

  def forceDarkBg = copy(bg = Pref.Bg.DARK)

  def set(name: String, value: String): Pref =
    Pref.validatedUpdate(name, value).fold(this)(_(this))

object Pref:

  val defaultBgImg = "/assets/images/background/landscape.jpg"

  case class BoardPref(
      brightness: Int,
      opacity: Int,
      hue: Int
  ) extends lila.core.pref.PrefBoard

  trait BooleanPref:
    val NO = 0
    val YES = 1
    val choices = Seq(NO -> "No", YES -> "Yes")

  object BooleanPref:
    val verify = (v: Int) => v == 0 || v == 1

  object Bg:
    val LIGHT = 100
    val DARK = 200
    val DARKBOARD = 300
    val TRANSPARENT = 400
    val SYSTEM = 500
    val PRO = 600

    val choices = Seq(
      LIGHT -> "Light",
      DARK -> "Dark",
      DARKBOARD -> "Dark Board",
      TRANSPARENT -> "Transparent",
      SYSTEM -> "Device theme",
      PRO -> "Analyst Pro"
    )

    val fromString = Map(
      "light" -> LIGHT,
      "dark" -> DARK,
      "darkBoard" -> DARKBOARD,
      "transp" -> TRANSPARENT,
      "system" -> SYSTEM,
      "pro" -> PRO
    )

    val asString = fromString.map(_.swap)

  object Tag:
    val dgt = "dgt"

  object KeyboardMove extends BooleanPref
  object Voice extends BooleanPref

  object RookCastle:
    val NO = 0
    val YES = 1
    val choices = Seq(
      NO -> "Castle by moving by two squares",
      YES -> "Castle by moving onto the rook"
    )

  object MoveEvent:
    val CLICK = 0
    val DRAG = 1
    val BOTH = 2
    val choices = Seq(
      CLICK -> "Click two squares",
      DRAG -> "Drag a piece",
      BOTH -> "Both clicks and drag"
    )

  object PieceNotation:
    val SYMBOL = 0
    val LETTER = 1
    val choices = Seq(
      SYMBOL -> "Chess piece symbol",
      LETTER -> "PGN letter (K, Q, R, B, N)"
    )

  object Animation:
    val NONE = 0
    val FAST = 1
    val NORMAL = 2
    val SLOW = 3
    val choices = Seq(
      NONE -> "None",
      FAST -> "Fast",
      NORMAL -> "Normal",
      SLOW -> "Slow"
    )

  object Coords:
    val NONE = 0
    val INSIDE = 1
    val OUTSIDE = 2
    val ALL = 3
    val choices = Seq(
      NONE -> "No",
      INSIDE -> "Inside the board",
      OUTSIDE -> "Outside the board",
      ALL -> "Inside all squares of the board"
    )
    def classOf(v: Int) =
      v match
        case INSIDE => "in"
        case OUTSIDE => "out"
        case ALL => "all"
        case _ => "no"

  object ResizeHandle:
    val NEVER = 0
    val INITIAL = 1
    val ALWAYS = 2
    val choices = Seq(
      NEVER -> "Never",
      INITIAL -> "On initial position",
      ALWAYS -> "Always"
    )

  object Zen:
    val NO = 0
    val YES = 1
    val GAME_AUTO = 2
    val choices = Seq(
      NO -> "No",
      YES -> "Yes",
      GAME_AUTO -> "In-game only"
    )

  private def intIn(value: String, allowed: Set[Int]): Option[Int] =
    value.toIntOption.filter(allowed)

  private def boolFrom01(value: String): Option[Boolean] =
    value match
      case "0" => false.some
      case "1" => true.some
      case _   => none

  private def bgImgFromValue(value: String): Option[Option[String]] =
    val trimmed = value.trim
    if trimmed.isEmpty then Some(None)
    else if trimmed.length <= 400 && (trimmed.startsWith("https://") || trimmed.startsWith("//"))
    then Some(trimmed.some)
    else None

  def validatedUpdate(name: String, value: String): Option[Pref => Pref] =
    name match
      case "bg" => Bg.fromString.get(value).map(bg => _.copy(bg = bg))
      case "bgImg" => bgImgFromValue(value).map(bgImg => _.copy(bgImg = bgImg))
      case "theme" if Theme.contains(value) => Some(_.copy(theme = value))
      case "pieceSet" if PieceSet.contains(value) => Some(_.copy(pieceSet = value))
      case "theme3d" if Theme3d.contains(value) => Some(_.copy(theme3d = value))
      case "pieceSet3d" if PieceSet3d.contains(value) => Some(_.copy(pieceSet3d = value))
      case "soundSet" if SoundSet.contains(value) => Some(_.copy(soundSet = value))
      case "animation" => intIn(value, Set(0, 1, 2, 3)).map(v => _.copy(animation = v))
      case "highlight" => boolFrom01(value).map(v => _.copy(highlight = v))
      case "destination" => boolFrom01(value).map(v => _.copy(destination = v))
      case "coords" => intIn(value, Set(0, 1, 2, 3)).map(v => _.copy(coords = v))
      case "keyboardMove" => intIn(value, Set(0, 1)).map(v => _.copy(keyboardMove = v))
      case "zen" => intIn(value, Set(0, 1, 2)).map(v => _.copy(zen = v))
      case "rookCastle" => intIn(value, Set(0, 1)).map(v => _.copy(rookCastle = v))
      case "moveEvent" => intIn(value, Set(0, 1, 2)).map(v => _.copy(moveEvent = v))
      case "pieceNotation" => intIn(value, Set(0, 1)).map(v => _.copy(pieceNotation = v))
      case _ => None

  val darkByDefaultSince = instantOf(2021, 11, 7, 8, 0)
  val systemByDefaultSince = instantOf(2022, 12, 23, 8, 0)

  def create(id: UserId) = default.copy(id = id)

  def create(user: User) = default.copy(
    id = user.id,
    bg =
      if user.createdAt.isAfter(systemByDefaultSince) then Bg.SYSTEM
      else if user.createdAt.isAfter(darkByDefaultSince) then Bg.DARK
      else Bg.LIGHT
  )

  lazy val default = Pref(
    id = UserId(""),
    bg = Bg.PRO,
    bgImg = none,
    is3d = false,
    theme = Theme.default.name,
    pieceSet = PieceSet.default.name,
    theme3d = Theme3d.default.name,
    pieceSet3d = PieceSet3d.default.name,
    soundSet = SoundSet.default.key,
    animation = Animation.NORMAL,
    highlight = true,
    destination = true,
    coords = Coords.INSIDE,
    keyboardMove = KeyboardMove.NO,
    voice = None,
    zen = Zen.NO,
    rookCastle = RookCastle.YES,
    moveEvent = MoveEvent.BOTH,
    pieceNotation = PieceNotation.SYMBOL,
    resizeHandle = ResizeHandle.INITIAL,
    board = BoardPref(brightness = 100, opacity = 100, hue = 0),
    tags = Map.empty
  )

  import alleycats.Zero
  given PrefZero: Zero[Pref] = Zero(default)
