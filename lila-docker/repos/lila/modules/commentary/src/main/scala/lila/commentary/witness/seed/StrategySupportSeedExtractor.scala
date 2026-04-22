package lila.commentary.witness.seed

import chess.format.Fen

import lila.commentary.root.{ RootExtractor, RootStateVector }
import lila.commentary.witness.u.UExtractionContext

final case class StrategySupportSeedExtraction(
    rootState: RootStateVector,
    seeds: StrategySupportSeedSet
)

object StrategySupportSeedExtractor:

  val liveSeedIds: Vector[StrategySupportSeedId] =
    StrategySupportSeedRuntime.liveSeedIds

  def fromRoot(rootState: RootStateVector): StrategySupportSeedExtraction =
    val context = UExtractionContext(rootState)
    StrategySupportSeedExtraction(
      rootState = rootState,
      seeds = StrategySupportSeedRuntime.extract(context)
    )

  def fromFen(fen: Fen.Full): Either[String, StrategySupportSeedExtraction] =
    RootExtractor.fromFen(fen).map(fromRoot)

  def fromFenFailClosed(fen: Fen.Full): Either[String, StrategySupportSeedExtraction] =
    RootExtractor.fromFenFailClosed(fen).map(fromRoot)
