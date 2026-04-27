package lila.commentary.claim

import chess.{ Color, King, Pawn, Piece, Position }

import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.root.RootPositionSupport
import lila.commentary.selection.*
import lila.commentary.strategic.StrategicObjectExtraction

object ExactBoardClaimProducer:

  def produce(
      currentExtraction: StrategicObjectExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction]
  ): Vector[CommentaryClaim] =
    val objectClaims =
      RootPositionSupport.exactPosition(currentExtraction.rootState).fold(
        _ => Vector.empty,
        position => materialInventoryClaim(position) ++ immediateCheckClaim(position)
      )
    val deltaClaims = deltaExtraction.toVector
      .filter(delta => delta.after.rootState == currentExtraction.rootState)
      .flatMap(transitionClaims)
    objectClaims ++ deltaClaims

  private def materialInventoryClaim(position: Position): Vector[CommentaryClaim] =
    val nonKingPieces = position.board.pieceMap.values.count(_.role != King)
    Option
      .when(nonKingPieces > 0)(
        CommentaryClaim(
          id = "exact-board-piece-inventory",
          layer = ClaimLayer.Object,
          status = ClaimStatus.Admitted,
          anchor = Some("board"),
          route = Some("piece_inventory"),
          scope = Some("position_local"),
          impact = ClaimImpact(boardExplainability = 20, pedagogicalClarity = 20),
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Object, "piece_inventory")),
          exactBoardBound = true,
          wordingStrengthCap = WordingStrength.QualifiedSupport
        )
      )
      .toVector

  private def immediateCheckClaim(position: Position): Vector[CommentaryClaim] =
    Option
      .when(position.check.yes):
        val defender = position.color
        val owner = !defender
        val kingAnchor = position.board.kingPosOf(defender).map(_.key).getOrElse("king")
        CommentaryClaim(
          id = s"exact-board-immediate-check-${colorKey(defender)}",
          layer = ClaimLayer.Object,
          status = ClaimStatus.Admitted,
          owner = Some(colorKey(owner)),
          beneficiary = Some(colorKey(owner)),
          defender = Some(colorKey(defender)),
          sideToMove = Some(colorKey(defender)),
          anchor = Some(kingAnchor),
          route = Some("immediate_check"),
          scope = Some("position_local"),
          impact = ClaimImpact(immediacy = 35, boardExplainability = 45, pedagogicalClarity = 35),
          evidenceRefs = Vector(
            EvidenceRef(
              EvidenceRefKind.Object,
              "immediate_check",
              owner = Some(colorKey(owner)),
              anchor = Some(kingAnchor),
              route = Some("immediate_check"),
              scope = Some("position_local")
            )
          ),
          exactBoardBound = true,
          wordingStrengthCap = WordingStrength.QualifiedSupport
        )
      .toVector

  private def transitionClaims(delta: StrategicDeltaExtraction): Vector[CommentaryClaim] =
    val exact =
      for
        before <- RootPositionSupport.exactPosition(delta.before.rootState)
        after <- RootPositionSupport.exactPosition(delta.after.rootState)
        move <- before.move(delta.playedMove).left.map(_.toString)
        movingPiece <- before.pieceAt(delta.playedMove.orig).toRight("missing moving piece")
        _ <- Either.cond(
          move.after.position.board == after.board &&
            move.after.position.color == after.color &&
            move.after.position.history.castles.value == after.history.castles.value &&
            move.after.position.enPassantSquare == after.enPassantSquare,
          (),
          "transition mismatch"
        )
      yield (before, move, movingPiece)

    exact.fold(
      _ => Vector.empty,
      (before, move, movingPiece) =>
        val capturedPiece = move.capture.flatMap(before.pieceAt)
        Vector(
          Some(lastMoveClaim(delta, movingPiece)),
          captureClaim(delta, movingPiece, capturedPiece),
          pawnTransitionClaim(delta, movingPiece, capturedPiece)
        ).flatten
    )

  private def lastMoveClaim(delta: StrategicDeltaExtraction, movingPiece: Piece): CommentaryClaim =
    deltaClaim(
      id = s"exact-transition-last-move-${delta.playedMove.uci}",
      evidenceId = "last_move_transition",
      route = "last_move_transition",
      owner = movingPiece.color,
      impact = ClaimImpact(immediacy = 20, boardExplainability = 35, pedagogicalClarity = 25)
    )

  private def captureClaim(
      delta: StrategicDeltaExtraction,
      movingPiece: Piece,
      capturedPiece: Option[Piece]
  ): Option[CommentaryClaim] =
    capturedPiece.filter(_.role != King).map: _ =>
      deltaClaim(
        id = s"exact-transition-capture-${delta.playedMove.uci}",
        evidenceId = "capture_transition",
        route = "capture_transition",
        owner = movingPiece.color,
        impact = ClaimImpact(immediacy = 30, boardExplainability = 45, pedagogicalClarity = 35)
      )

  private def pawnTransitionClaim(
      delta: StrategicDeltaExtraction,
      movingPiece: Piece,
      capturedPiece: Option[Piece]
  ): Option[CommentaryClaim] =
    Option.when(movingPiece.role == Pawn || capturedPiece.exists(_.role == Pawn))(
      deltaClaim(
        id = s"exact-transition-pawn-structure-${delta.playedMove.uci}",
        evidenceId = "pawn_structure_transition",
        route = "pawn_structure_transition",
        owner = movingPiece.color,
        impact = ClaimImpact(boardExplainability = 35, pedagogicalClarity = 30)
      )
    )

  private def deltaClaim(
      id: String,
      evidenceId: String,
      route: String,
      owner: Color,
      impact: ClaimImpact
  ): CommentaryClaim =
    val ownerKey = colorKey(owner)
    val defenderKey = colorKey(!owner)
    CommentaryClaim(
      id = id,
      layer = ClaimLayer.Delta,
      status = ClaimStatus.Admitted,
      owner = Some(ownerKey),
      beneficiary = Some(ownerKey),
      defender = Some(defenderKey),
      sideToMove = Some(defenderKey),
      anchor = Some("board"),
      route = Some(route),
      scope = Some("move_local"),
      impact = impact,
      evidenceRefs = Vector(
        EvidenceRef(
          kind = EvidenceRefKind.Delta,
          id = evidenceId,
          owner = Some(ownerKey),
          anchor = Some("board"),
          route = Some(route),
          scope = Some("move_local")
        )
      ),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"
