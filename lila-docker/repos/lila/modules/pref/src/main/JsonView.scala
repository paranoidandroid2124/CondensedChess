package lila.pref

import play.api.libs.json.*

// Simplified JsonView for analysis-only system
def toJson(p: Pref, lichobileCompat: Boolean) = Json.obj(
  "dark" -> (p.bg != Pref.Bg.LIGHT),
  "transp" -> (p.bg == Pref.Bg.TRANSPARENT),
  "bgImg" -> p.bgImgOrDefault,
  "is3d" -> p.is3d,
  "theme" -> p.theme,
  "pieceSet" -> p.pieceSet,
  "theme3d" -> p.theme3d,
  "pieceSet3d" -> p.pieceSet3d,
  "soundSet" -> p.soundSet,
  "animation" -> p.animation,
  "pieceNotation" -> p.pieceNotation,
  "highlight" -> p.highlight,
  "destination" -> p.destination,
  "coords" -> p.coords,
  "keyboardMove" -> p.keyboardMove,
  "voiceMove" -> p.hasVoice,
  "zen" -> p.zen,
  "moveEvent" -> p.moveEvent,
  "rookCastle" -> p.rookCastle
)
