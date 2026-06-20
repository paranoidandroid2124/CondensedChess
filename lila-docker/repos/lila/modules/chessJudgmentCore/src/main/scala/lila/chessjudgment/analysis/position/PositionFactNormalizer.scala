package lila.chessjudgment.analysis.position

import lila.chessjudgment.model.Fact
import lila.chessjudgment.model.judgment.*

object PositionFactNormalizer:

  def fromBoardFacts(
      id: String,
      facts: List[Fact],
      features: Option[PositionFeatures],
      position: PositionNodeRef,
      scope: EvidenceScope
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.BoardFactProducer,
        layer = EvidenceLayer.Board,
        position = position,
        line = None,
        scope = scope,
        confidence = EvidenceConfidence.BoardDerived
      )
    EvidenceRecord(
      ref = ref,
      payload = BoardFactEvidence(
        facts = facts,
        features = features
      )(
        anchors = features.toList.flatMap(boardAnchors)
      )
    )

  private def boardAnchors(features: PositionFeatures): List[BoardAnchor] =
    val side = features.sideToMove
    val opponent = !side
    val centerEdge = features.centralSpace.whiteCenterControl - features.centralSpace.blackCenterControl
    val sideCenterEdge = if side.white then centerEdge else -centerEdge
    val spaceEdge = if side.white then features.centralSpace.spaceDiff else -features.centralSpace.spaceDiff
    val developmentEdge = developmentLead(features, side)
    val mobilityEdge = mobilityLead(features, side)
    val opponentLowMobility = lowMobility(features, opponent)
    val sideSemiOpenFiles = semiOpenFiles(features, side)
    val sideRookOnSeventh = rookOnSeventh(features, side)
    val opponentExposure = kingExposure(features, opponent)
    val opponentKingPressure = kingPressure(features, opponent)
    List(
      Option.when(sideCenterEdge >= 2)(
        BoardAnchor(BoardAnchorKind.CenterControl, side, BoardAnchorSignal.CenterControlEdge, sideCenterEdge, 0.72)
      ),
      Option.when(spaceEdge >= 2)(
        BoardAnchor(BoardAnchorKind.Space, side, BoardAnchorSignal.SpaceEdge, spaceEdge, 0.72)
      ),
      Option.when(developmentEdge >= 2)(
        BoardAnchor(BoardAnchorKind.Development, side, BoardAnchorSignal.DevelopmentLead, developmentEdge, 0.74)
      ),
      Option.when(sideSemiOpenFiles > 0)(
        BoardAnchor(BoardAnchorKind.FileControl, side, BoardAnchorSignal.SemiOpenFileAccess, sideSemiOpenFiles, 0.68)
      ),
      Option.when(sideRookOnSeventh)(
        BoardAnchor(BoardAnchorKind.FileControl, side, BoardAnchorSignal.RookOnSeventh, 1, 0.82)
      ),
      Option.when(mobilityEdge >= 5)(
        BoardAnchor(BoardAnchorKind.Activity, side, BoardAnchorSignal.MobilityEdge, mobilityEdge, 0.70)
      ),
      Option.when(opponentLowMobility >= 2)(
        BoardAnchor(BoardAnchorKind.CounterplayRestraint, side, BoardAnchorSignal.OpponentLowMobility, opponentLowMobility, 0.72)
      ),
      Option.when(opponentExposure >= 2)(
        BoardAnchor(BoardAnchorKind.KingSafety, side, BoardAnchorSignal.KingExposure, opponentExposure, 0.72)
      ),
      Option.when(opponentKingPressure >= 4)(
        BoardAnchor(BoardAnchorKind.KingSafety, side, BoardAnchorSignal.KingPressure, opponentKingPressure, 0.74)
      ),
      Option.when(features.pawns.whiteIQP || features.pawns.blackIQP || features.pawns.whiteHangingPawns || features.pawns.blackHangingPawns)(
        BoardAnchor(BoardAnchorKind.PawnStructure, side, BoardAnchorSignal.PawnStructureShape, 1, 0.68)
      )
    ).flatten

  private def developmentLead(features: PositionFeatures, side: chess.Color): Int =
    if side.white then features.activity.blackDevelopmentLag - features.activity.whiteDevelopmentLag
    else features.activity.whiteDevelopmentLag - features.activity.blackDevelopmentLag

  private def mobilityLead(features: PositionFeatures, side: chess.Color): Int =
    if side.white then features.activity.whitePseudoMobility - features.activity.blackPseudoMobility
    else features.activity.blackPseudoMobility - features.activity.whitePseudoMobility

  private def lowMobility(features: PositionFeatures, side: chess.Color): Int =
    if side.white then features.activity.whiteLowMobilityPieces else features.activity.blackLowMobilityPieces

  private def semiOpenFiles(features: PositionFeatures, side: chess.Color): Int =
    if side.white then features.lineControl.whiteSemiOpenFiles else features.lineControl.blackSemiOpenFiles

  private def rookOnSeventh(features: PositionFeatures, side: chess.Color): Boolean =
    if side.white then features.lineControl.whiteRookOn7th else features.lineControl.blackRookOn7th

  private def kingExposure(features: PositionFeatures, side: chess.Color): Int =
    if side.white then features.kingSafety.whiteKingExposedFiles else features.kingSafety.blackKingExposedFiles

  private def kingPressure(features: PositionFeatures, side: chess.Color): Int =
    if side.white then features.kingSafety.whiteAttackersCount else features.kingSafety.blackAttackersCount
