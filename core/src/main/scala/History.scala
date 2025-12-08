package chess

import format.Uci

case class History(
    lastMove: Option[Uci] = None,
    positionHashes: PositionHash = PositionHash.empty,
    castles: Castles = Castles.init,
    unmovedRooks: UnmovedRooks,
    halfMoveClock: HalfMoveClock = HalfMoveClock.initial
):

  def setHalfMoveClock(v: HalfMoveClock): History = copy(halfMoveClock = v)

  inline def threefoldRepetition: Boolean = positionHashes.isRepetition(3)
  inline def fivefoldRepetition: Boolean = positionHashes.isRepetition(5)

  inline def canCastle(inline color: Color): Boolean = castles.can(color)
  inline def canCastle(inline color: Color, inline side: Side): Boolean = castles.can(color, side)

  inline def withoutCastles(inline color: Color): History = copy(castles = castles.without(color))

  inline def withoutAnyCastles: History = copy(castles = Castles.none)

  inline def withoutCastle(color: Color, side: Side): History = copy(castles = castles.without(color, side))

  inline def withCastles(inline c: Castles): History = copy(castles = c)
