package lila.commentary.witness.u

import chess.format.Fen

import lila.commentary.root.{ RootExtractor, RootStateVector }
import lila.commentary.witness.WitnessSet

final case class UWitnessExtraction(rootState: RootStateVector, witnesses: WitnessSet)

object UWitnessExtractor:

  def fromRoot(rootState: RootStateVector): UWitnessExtraction =
    val context = UExtractionContext(rootState)
    UWitnessExtraction(rootState = rootState, witnesses = UInternalRuntime.extract(context))

  def fromFen(fen: Fen.Full): Either[String, UWitnessExtraction] =
    RootExtractor.fromFen(fen).map(fromRoot)

  def fromFenFailClosed(fen: Fen.Full): Either[String, UWitnessExtraction] =
    RootExtractor.fromFenFailClosed(fen).map(fromRoot)
