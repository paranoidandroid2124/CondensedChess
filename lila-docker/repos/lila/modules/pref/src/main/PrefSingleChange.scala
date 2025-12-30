package lila.pref

import monocle.syntax.all.*

// Simplified PrefSingleChange for analysis-only system
object PrefSingleChange:

  type Change[A] = lila.common.Form.SingleChange.Change[Pref, A]
  private def changing[A] = lila.common.Form.SingleChange.changing[Pref, PrefForm.fields.type, A]

  val changes: Map[String, Change[?]] = List[Change[?]](
    changing(_.bg): v =>
      Pref.Bg.fromString.get(v).fold[Pref => Pref](identity)(bg => _.copy(bg = bg)),
    changing(_.bgImg): v =>
      _.copy(bgImg = v.some.filterNot(_.isBlank)),
    changing(_.theme): v =>
      _.copy(theme = v),
    changing(_.pieceSet): v =>
      _.copy(pieceSet = v),
    changing(_.theme3d): v =>
      _.copy(theme3d = v),
    changing(_.pieceSet3d): v =>
      _.copy(pieceSet3d = v),
    changing(_.is3d): v =>
      _.copy(is3d = v),
    changing(_.soundSet): v =>
      _.copy(soundSet = v),
    changing(_.zen): v =>
      _.copy(zen = v),
    changing(_.voice): v =>
      _.copy(voice = v.some),
    changing(_.keyboardMove): v =>
      _.copy(keyboardMove = v | Pref.KeyboardMove.NO),
    changing(_.board.brightness): v =>
      _.focus(_.board.brightness).replace(v),
    changing(_.board.opacity): v =>
      _.focus(_.board.opacity).replace(v),
    changing(_.board.hue): v =>
      _.focus(_.board.hue).replace(v)
  ).map: change =>
    change.field -> change
  .toMap
