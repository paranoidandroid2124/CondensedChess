package lila.security

import lila.core.security.FingerHash
import scalalib.newtypes.TotalWrapper

opaque type FingerPrint = String
object FingerPrint extends TotalWrapper[FingerPrint, String]:
  extension (a: FingerPrint) def hash: Option[FingerHash] = FingerHashGenerator.from(a)

object FingerHashGenerator:

  val length = 12

  def from(print: FingerPrint): Option[FingerHash] =
    try
      import java.util.Base64
      import org.apache.commons.codec.binary.Hex
      Some(lila.core.security.FingerHash {
        Base64.getEncoder
          .encodeToString(Hex.decodeHex(normalize(print).toArray))
          .take(length)
      })
    catch case _: Exception => Option.empty

  private def normalize(fp: FingerPrint): String =
    val str = fp.value.replace("-", "")
    if str.length % 2 != 0 then s"${str}0" else str
