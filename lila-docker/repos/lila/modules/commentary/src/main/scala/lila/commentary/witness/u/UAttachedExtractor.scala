package lila.commentary.witness.u

import scala.util.Try

import chess.format.Fen

import lila.commentary.root.{ RootExtractor, RootStateVector }
import lila.commentary.witness.WitnessSet

final case class UAttachedExtraction(rootState: RootStateVector, witnesses: WitnessSet)

object UAttachedExtractor:

  def fromRoot(rootState: RootStateVector): UAttachedExtraction =
    val context = UExtractionContext(rootState)
    UAttachedExtraction(rootState = rootState, witnesses = UAttachedInternalRuntime.extract(context))

  private[u] def fromRootFailClosed(rootState: RootStateVector): Either[String, UAttachedExtraction] =
    Try(fromRoot(rootState)).toEither.left.map: error =>
      val message = Option(error.getMessage).filter(_.nonEmpty).getOrElse(error.getClass.getSimpleName)
      s"U-attached extraction failed closed: $message"

  def fromFen(fen: Fen.Full): Either[String, UAttachedExtraction] =
    RootExtractor.fromFen(fen).map(fromRoot)

  def fromFenFailClosed(fen: Fen.Full): Either[String, UAttachedExtraction] =
    RootExtractor.fromFenFailClosed(fen).flatMap(fromRootFailClosed)
