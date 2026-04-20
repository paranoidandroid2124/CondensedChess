package lila.commentary.strategic

import scala.util.Try

import chess.format.Fen

import lila.commentary.root.{ RootExtractor, RootStateVector }
import lila.commentary.witness.WitnessSet
import lila.commentary.witness.u.{ UAttachedExtractor, UWitnessExtractor }

final case class StrategicObjectExtraction(
    rootState: RootStateVector,
    primaryWitnesses: WitnessSet,
    attachedWitnesses: WitnessSet,
    objects: StrategicObjectSet
)

object StrategicObjectExtractor:

  def fromRoot(rootState: RootStateVector): StrategicObjectExtraction =
    val primaryExtraction = UWitnessExtractor.fromRoot(rootState)
    val attachedExtraction = UAttachedExtractor.fromRoot(rootState)
    val context = StrategicObjectContext(
      rootState = rootState,
      primaryWitnesses = primaryExtraction.witnesses,
      attachedWitnesses = attachedExtraction.witnesses
    )
    StrategicObjectExtraction(
      rootState = rootState,
      primaryWitnesses = primaryExtraction.witnesses,
      attachedWitnesses = attachedExtraction.witnesses,
      objects = StrategicInternalRuntime.extract(context)
    )

  private[strategic] def fromRootFailClosed(
      rootState: RootStateVector
  ): Either[String, StrategicObjectExtraction] =
    Try(fromRoot(rootState)).toEither.left.map: error =>
      val message = Option(error.getMessage).filter(_.nonEmpty).getOrElse(error.getClass.getSimpleName)
      s"Strategic object extraction failed closed: $message"

  def fromFen(fen: Fen.Full): Either[String, StrategicObjectExtraction] =
    RootExtractor.fromFen(fen).map(fromRoot)

  def fromFenFailClosed(fen: Fen.Full): Either[String, StrategicObjectExtraction] =
    RootExtractor.fromFenFailClosed(fen).flatMap(fromRootFailClosed)
