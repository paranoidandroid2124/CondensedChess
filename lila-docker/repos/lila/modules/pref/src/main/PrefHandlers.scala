package lila.pref

import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }

// Simplified PrefHandlers for analysis-only system
private object PrefHandlers:

  given BSONDocumentHandler[Pref.BoardPref] = Macros.handler

  given BSONDocumentHandler[Pref] = new BSON[Pref]:

    def reads(r: BSON.Reader): Pref =
      Pref(
        id = r.get[UserId]("_id"),
        bg = r.getD("bg", Pref.default.bg),
        bgImg = r.strO("bgImg"),
        is3d = r.getD("is3d", Pref.default.is3d),
        theme = r.getD("theme", Pref.default.theme),
        pieceSet = r.getD("pieceSet", Pref.default.pieceSet),
        theme3d = r.getD("theme3d", Pref.default.theme3d),
        pieceSet3d = r.getD("pieceSet3d", Pref.default.pieceSet3d),
        soundSet = r.getD("soundSet", Pref.default.soundSet),
        animation = r.getD("animation", Pref.default.animation),
        highlight = r.getD("highlight", Pref.default.highlight),
        destination = r.getD("destination", Pref.default.destination),
        coords = r.getD("coords", Pref.default.coords),
        keyboardMove = r.getD("keyboardMove", Pref.default.keyboardMove),
        voice = r.getO("voice"),
        zen = r.getD("zen", Pref.default.zen),
        rookCastle = r.getD("rookCastle", Pref.default.rookCastle),
        pieceNotation = r.getD("pieceNotation", Pref.default.pieceNotation),
        resizeHandle = r.getD("resizeHandle", Pref.default.resizeHandle),
        moveEvent = r.getD("moveEvent", Pref.default.moveEvent),
        board = r.getD("board", Pref.default.board),
        tags = r.getD("tags", Pref.default.tags)
      )

    def writes(w: BSON.Writer, o: Pref) =
      $doc(
        "_id" -> o.id,
        "bg" -> o.bg,
        "bgImg" -> o.bgImg,
        "is3d" -> o.is3d,
        "theme" -> o.theme,
        "pieceSet" -> o.pieceSet,
        "theme3d" -> o.theme3d,
        "pieceSet3d" -> o.pieceSet3d,
        "soundSet" -> SoundSet.name2key(o.soundSet),
        "animation" -> o.animation,
        "highlight" -> o.highlight,
        "destination" -> o.destination,
        "coords" -> o.coords,
        "keyboardMove" -> o.keyboardMove,
        "voice" -> o.voice,
        "zen" -> o.zen,
        "rookCastle" -> o.rookCastle,
        "moveEvent" -> o.moveEvent,
        "pieceNotation" -> o.pieceNotation,
        "resizeHandle" -> o.resizeHandle,
        "board" -> o.board,
        "tags" -> o.tags
      )
