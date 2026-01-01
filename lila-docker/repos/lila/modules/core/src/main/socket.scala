package lila.core.socket

import scalalib.newtypes.TotalWrapper

opaque type SocketVersion = Int
object SocketVersion extends TotalWrapper[SocketVersion, Int]
