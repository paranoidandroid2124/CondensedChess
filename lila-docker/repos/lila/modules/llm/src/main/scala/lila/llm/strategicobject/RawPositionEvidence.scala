package lila.llm.strategicobject

import chess.{ Board, Color, Position }
import chess.format.Fen
import lila.llm.analysis.{
  ActivityFeatures,
  CentralSpaceFeatures,
  KingSafetyFeatures,
  LineControlFeatures,
  MaterialImbalanceFeatures,
  MaterialPhaseFeatures,
  PawnStructureFeatures,
  PositionAnalyzer
}
import lila.llm.analysis.FactExtractor
import lila.llm.model.Fact

final case class BoardFeatureEvidence(
    pawns: PawnStructureFeatures,
    activity: ActivityFeatures,
    kingSafety: KingSafetyFeatures,
    materialPhase: MaterialPhaseFeatures,
    lineControl: LineControlFeatures,
    imbalance: MaterialImbalanceFeatures,
    centralSpace: CentralSpaceFeatures
)

final case class RawPositionEvidence(
    fen: String,
    position: Position,
    playedMove: Option[String] = None,
    moveIndex: Option[Int] = None,
    features: BoardFeatureEvidence,
    staticFactsByColor: Map[Color, List[Fact]]
):
  def board: Board = position.board
  def sideToMove: Color = position.color
  def factsFor(color: Color): List[Fact] =
    staticFactsByColor.getOrElse(color, Nil)

object RawPositionEvidence:

  def fromFen(
      fen: String,
      playedMove: Option[String] = None,
      moveIndex: Option[Int] = None
  ): Either[String, RawPositionEvidence] =
    val parsedPosition =
      Fen
        .read(chess.variant.Standard, Fen.Full(fen))
        .toRight(s"invalid fen: $fen")
    val parsedFeatures =
      PositionAnalyzer
        .extractFeatures(fen, moveIndex.getOrElse(0))
        .toRight(s"feature extraction failed for fen: $fen")

    for
      position <- parsedPosition
      extracted <- parsedFeatures
    yield RawPositionEvidence(
      fen = fen,
      position = position,
      playedMove = playedMove,
      moveIndex = moveIndex,
      features = BoardFeatureEvidence(
        pawns = extracted.pawns,
        activity = extracted.activity,
        kingSafety = extracted.kingSafety,
        materialPhase = extracted.materialPhase,
        lineControl = extracted.lineControl,
        imbalance = extracted.imbalance,
        centralSpace = extracted.centralSpace
      ),
      staticFactsByColor = Map(
        Color.White -> FactExtractor.extractStaticFacts(position.board, Color.White),
        Color.Black -> FactExtractor.extractStaticFacts(position.board, Color.Black)
      )
    )
