package lila.chessjudgment.analysis.policy

import lila.chessjudgment.model.judgment.*

object BoardAnchorEvidencePolicy:

  def relativeCauseAnchorKinds(payload: BoardFactEvidence): List[BoardAnchorKind] =
    payload.boardAnchors.collect {
      case anchor if canSupportRelativeCause(anchor) =>
        anchor.kind
    }.distinct

  def hasRelativeCauseAnchor(payload: BoardFactEvidence, kind: BoardAnchorKind): Boolean =
    payload.boardAnchors.exists(anchor => anchor.kind == kind && canSupportRelativeCause(anchor))

  private def canSupportRelativeCause(anchor: BoardAnchor): Boolean =
    anchor.kind match
      case BoardAnchorKind.LooseMaterial =>
        anchor.signal == BoardAnchorSignal.HangingPiece ||
          anchor.detail.exists(_.defenderSquares.isEmpty)
      case BoardAnchorKind.PinPressure =>
        anchor.detail.flatMap(_.isAbsolute).contains(true) || anchor.magnitude > 1
      case BoardAnchorKind.SkewerPressure | BoardAnchorKind.ForkPressure | BoardAnchorKind.XRayPressure |
          BoardAnchorKind.Outpost =>
        true
      case BoardAnchorKind.BatteryPressure =>
        anchor.detail.flatMap(_.axis).contains(BoardAnchorAxis.Diagonal)
      case BoardAnchorKind.CenterControl | BoardAnchorKind.Space | BoardAnchorKind.Development |
          BoardAnchorKind.FileControl | BoardAnchorKind.Activity | BoardAnchorKind.CounterplayRestraint |
          BoardAnchorKind.KingSafety | BoardAnchorKind.PawnStructure | BoardAnchorKind.WeakSquare =>
        false
