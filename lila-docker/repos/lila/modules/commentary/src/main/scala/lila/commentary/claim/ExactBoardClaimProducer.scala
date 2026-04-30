package lila.commentary.claim

import chess.{ Color, King, Knight, Pawn, Piece, Position, Queen, Rook, Square }

import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.root.RootAtomRegistry
import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.root.RootPositionSupport
import lila.commentary.selection.*
import lila.commentary.strategic.StrategicObjectExtraction

object ExactBoardClaimProducer:

  def produce(
      currentExtraction: StrategicObjectExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction]
  ): Vector[CommentaryClaim] =
    val hasMatchingDelta = deltaExtraction.exists(delta => delta.after.rootState == currentExtraction.rootState)
    val objectClaims =
      RootPositionSupport.exactPosition(currentExtraction.rootState).fold(
        _ => Vector.empty,
        position =>
          materialInventoryClaim(position) ++
            mateInOneClaim(position) ++
            immediateCheckClaim(position) ++
            Option.when(!hasMatchingDelta)(immediateCaptureClaims(position, currentExtraction)).getOrElse(Vector.empty)
      )
    val rootClaims = tacticalLiabilityRootClaims(currentExtraction, transitionContext = hasMatchingDelta)
    val deltaClaims = deltaExtraction.toVector
      .filter(delta => delta.after.rootState == currentExtraction.rootState)
      .flatMap(transitionClaims)
    objectClaims ++ deltaClaims ++ rootClaims

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

  private def mateInOneClaim(position: Position): Vector[CommentaryClaim] =
    mateInOne(position).toVector.map: move =>
      val owner = position.color
      val defender = !owner
      val kingAnchor = move.after.position.board.kingPosOf(defender).map(_.key).getOrElse("king")
      CommentaryClaim(
        id = s"exact-board-mate-in-one-${move.orig.key}${move.dest.key}",
        layer = ClaimLayer.Object,
        status = ClaimStatus.Admitted,
        owner = Some(colorKey(owner)),
        beneficiary = Some(colorKey(owner)),
        defender = Some(colorKey(defender)),
        sideToMove = Some(colorKey(owner)),
        anchor = Some(kingAnchor),
        route = Some("immediate_mate"),
        scope = Some("position_local"),
        impact = ClaimImpact(resultMaterialImpact = 90, forcedness = 95, immediacy = 95, boardExplainability = 80, pedagogicalClarity = 70),
        evidenceRefs = Vector(
          EvidenceRef(
            EvidenceRefKind.Object,
            "immediate_mate",
            owner = Some(colorKey(owner)),
            anchor = Some(kingAnchor),
            route = Some("immediate_mate"),
            scope = Some("position_local")
          )
        ),
        exactBoardBound = true,
        wordingStrengthCap = WordingStrength.QualifiedSupport
      )

  private def mateInOne(position: Position): Option[chess.Move] =
    legalMoves(position).find: move =>
      move.after.check.yes && legalMoves(move.after.position).isEmpty

  private def immediateCaptureClaims(
      position: Position,
      currentExtraction: StrategicObjectExtraction
  ): Vector[CommentaryClaim] =
    legalMoves(position)
      .filter(move => move.capture.nonEmpty)
      .flatMap: move =>
        val owner = position.color
        val defender = !owner
        position.pieceAt(move.dest).filter(piece => piece.color == defender && piece.role != King).flatMap: captured =>
          Option.when(loosePieceAt(currentExtraction, defender, move.dest)):
            val ownerKey = colorKey(owner)
            val defenderKey = colorKey(defender)
            val anchor = move.dest.key
            CommentaryClaim(
              id = s"exact-board-immediate-capture-${move.orig.key}${move.dest.key}",
              layer = ClaimLayer.Object,
              status = ClaimStatus.Admitted,
              owner = Some(ownerKey),
              beneficiary = Some(ownerKey),
              defender = Some(defenderKey),
              sideToMove = Some(ownerKey),
              anchor = Some(anchor),
              route = Some("immediate_capture"),
              scope = Some("position_local"),
              impact = ClaimImpact(resultMaterialImpact = pieceValue(captured.role) / 10, immediacy = 75, boardExplainability = 70, pedagogicalClarity = 50),
              evidenceRefs = Vector(
                EvidenceRef(
                  kind = EvidenceRefKind.Object,
                  id = "immediate_capture",
                  owner = Some(ownerKey),
                  anchor = Some(anchor),
                  route = Some("immediate_capture"),
                  scope = Some("position_local")
                )
              ),
              lowerCarrierRefs = Vector(
                EvidenceRef(
                  kind = EvidenceRefKind.Root,
                  id = "loose_piece",
                  owner = Some(ownerKey),
                  anchor = Some(anchor),
                  route = Some("tactical_liability"),
                  scope = Some("position_local")
                )
              ),
              exactBoardBound = true,
              wordingStrengthCap = WordingStrength.QualifiedSupport
            )

  private final case class TacticalRootDescriptor(
      schemaId: String,
      evidenceId: String,
      ownerFromRootColor: Color => Color
  )

  private val TacticalRootDescriptors: Vector[TacticalRootDescriptor] =
    Vector(
      TacticalRootDescriptor(SchemaId.LoosePiece, "loose_piece", vulnerable => !vulnerable),
      TacticalRootDescriptor(SchemaId.PinnedPiece, "pinned_piece", vulnerable => !vulnerable),
      TacticalRootDescriptor(SchemaId.OverloadedPiece, "overloaded_piece", vulnerable => !vulnerable),
      TacticalRootDescriptor(SchemaId.TrappedPiece, "trapped_piece", vulnerable => !vulnerable),
      TacticalRootDescriptor(SchemaId.XrayTarget, "xray_target", identity)
    )

  private def tacticalLiabilityRootClaims(current: StrategicObjectExtraction, transitionContext: Boolean): Vector[CommentaryClaim] =
    for
      descriptor <- TacticalRootDescriptors
      vulnerableOrOwner <- RootAtomRegistry.canonicalColors
      square <- activeSquares(current, descriptor.schemaId, vulnerableOrOwner)
    yield
      val owner = descriptor.ownerFromRootColor(vulnerableOrOwner)
      val defender = !owner
      val ownerKey = colorKey(owner)
      val defenderKey = colorKey(defender)
      val squareKey = square.key
      CommentaryClaim(
        id = s"exact-root-${descriptor.evidenceId}-$ownerKey-$squareKey",
        layer = ClaimLayer.Object,
        status = ClaimStatus.SupportOnly,
        owner = Some(ownerKey),
        beneficiary = Some(ownerKey),
        defender = Some(defenderKey),
        anchor = Some(squareKey),
        route = Some("tactical_liability"),
        scope = Some("position_local"),
        impact =
          if transitionContext then ClaimImpact(immediacy = 10, boardExplainability = 20, pedagogicalClarity = 15)
          else ClaimImpact(resultMaterialImpact = 45, immediacy = 45, boardExplainability = 55, pedagogicalClarity = 40),
        evidenceRefs = Vector(
          EvidenceRef(
            kind = EvidenceRefKind.Root,
            id = descriptor.evidenceId,
            owner = Some(ownerKey),
            anchor = Some(squareKey),
            route = Some("tactical_liability"),
            scope = Some("position_local")
          )
        ),
        exactBoardBound = true,
        wordingStrengthCap = WordingStrength.QualifiedSupport
      )

  private def activeSquares(
      current: StrategicObjectExtraction,
      schemaId: String,
      color: Color
  ): Vector[Square] =
    current.rootState
      .squareMask64(schemaId, color = Some(color))
      .toVector
      .flatMap(mask => RootAtomRegistry.canonicalSquares.filter(square => (mask & square.bl) != 0L))

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
          pawnTransitionClaim(delta, movingPiece, capturedPiece),
          movedPieceLeftLooseClaim(delta, before, move.after.position, movingPiece),
          nonSliderRoyalForkClaim(delta, before, move.after.position, movingPiece.color)
        ).flatten
    )

  private def lastMoveClaim(delta: StrategicDeltaExtraction, movingPiece: Piece): CommentaryClaim =
    deltaClaim(
      id = s"exact-transition-last-move-${delta.playedMove.uci}",
      evidenceId = "last_move_transition",
        route = "last_move_transition",
        owner = movingPiece.color,
        status = ClaimStatus.SupportOnly,
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
        status = ClaimStatus.SupportOnly,
        impact = ClaimImpact(boardExplainability = 35, pedagogicalClarity = 30)
      )
    )

  private def movedPieceLeftLooseClaim(
      delta: StrategicDeltaExtraction,
      before: Position,
      after: Position,
      movingPiece: Piece
  ): Option[CommentaryClaim] =
    val destination = delta.playedMove.dest
    val destinationPiece = after.pieceAt(destination)
    immediateCaptureOn(after, destination, !movingPiece.color).filter(_ =>
        movingPiece.role != King &&
        movingPiece.role != Pawn &&
        !loosePieceAt(delta.before.rootState, movingPiece.color, delta.playedMove.orig) &&
        before.pieceAt(delta.playedMove.orig).contains(movingPiece) &&
        destinationPiece.contains(movingPiece) &&
        loosePieceAt(delta, movingPiece.color, destination)
    ).map: _ =>
      val owner = !movingPiece.color
      val ownerKey = colorKey(owner)
      val defenderKey = colorKey(movingPiece.color)
      val anchor = destination.key
      val route = "moved_piece_left_loose"
      val scope = "move_local"
      CommentaryClaim(
        id = s"exact-transition-moved-piece-left-loose-${delta.playedMove.uci}",
        layer = ClaimLayer.Delta,
        status = ClaimStatus.Admitted,
        owner = Some(ownerKey),
        beneficiary = Some(ownerKey),
        defender = Some(defenderKey),
        sideToMove = Some(ownerKey),
        anchor = Some(anchor),
        route = Some(route),
        scope = Some(scope),
        impact = ClaimImpact(resultMaterialImpact = 70, immediacy = 70, boardExplainability = 65, pedagogicalClarity = 45),
        evidenceRefs = Vector(
          EvidenceRef(
            kind = EvidenceRefKind.Delta,
            id = "moved_piece_left_loose_transition",
            owner = Some(ownerKey),
            anchor = Some(anchor),
            route = Some(route),
            scope = Some(scope)
          )
        ),
        lowerCarrierRefs = Vector(
          EvidenceRef(
            kind = EvidenceRefKind.Root,
            id = "loose_piece",
            owner = Some(ownerKey),
            anchor = Some(anchor),
            route = Some(route),
            scope = Some(scope)
          ),
          EvidenceRef(
            kind = EvidenceRefKind.Object,
            id = "immediate_capture",
            owner = Some(ownerKey),
            anchor = Some(anchor),
            route = Some(route),
            scope = Some(scope)
          )
        ),
        exactBoardBound = true,
        wordingStrengthCap = WordingStrength.QualifiedSupport
      )

  private def immediateCaptureOn(position: Position, square: Square, attacker: Color): Option[chess.Move] =
    legalMoves(position.withColor(attacker)).find(move => move.dest == square && move.capture.nonEmpty)

  private def loosePieceAt(delta: StrategicDeltaExtraction, color: Color, square: Square): Boolean =
    delta.after.rootState
      .squareMask64(SchemaId.LoosePiece, color = Some(color))
      .exists(mask => (mask & square.bl) != 0L)

  private def loosePieceAt(rootState: lila.commentary.root.RootStateVector, color: Color, square: Square): Boolean =
    rootState
      .squareMask64(SchemaId.LoosePiece, color = Some(color))
      .exists(mask => (mask & square.bl) != 0L)

  private def loosePieceAt(current: StrategicObjectExtraction, color: Color, square: Square): Boolean =
    current.rootState
      .squareMask64(SchemaId.LoosePiece, color = Some(color))
      .exists(mask => (mask & square.bl) != 0L)

  private def nonSliderRoyalForkClaim(
      delta: StrategicDeltaExtraction,
      before: Position,
      after: Position,
      moverColor: Color
  ): Option[CommentaryClaim] =
    val owner = after.color
    Option.when(owner == !moverColor)(())
      .filter(_ => nonSliderRoyalFork(before.withColor(owner)).isEmpty)
      .flatMap(_ => nonSliderRoyalFork(after))
      .map: fork =>
        val ownerKey = colorKey(owner)
        val defenderKey = colorKey(!owner)
        CommentaryClaim(
          id = s"exact-transition-non-slider-royal-fork-${delta.playedMove.uci}-${fork.move.orig.key}${fork.move.dest.key}",
          layer = ClaimLayer.Delta,
          status = ClaimStatus.Admitted,
          owner = Some(ownerKey),
          beneficiary = Some(ownerKey),
          defender = Some(defenderKey),
          sideToMove = Some(ownerKey),
          anchor = Some(fork.target.key),
          route = Some("non_slider_royal_fork"),
          scope = Some("move_local"),
          impact = ClaimImpact(immediacy = 70, boardExplainability = 70, pedagogicalClarity = 55),
          evidenceRefs = Vector(
            EvidenceRef(
              EvidenceRefKind.Object,
              "royal_fork",
              owner = Some(ownerKey),
              anchor = Some(fork.target.key),
              route = Some("non_slider_royal_fork"),
              scope = Some("move_local")
            )
          ),
          exactBoardBound = true,
          wordingStrengthCap = WordingStrength.QualifiedSupport
        )

  private final case class RoyalFork(move: chess.Move, target: Square)

  private def nonSliderRoyalFork(position: Position): Option[RoyalFork] =
    legalMoves(position)
      .iterator
      .flatMap: move =>
        position.pieceAt(move.orig).filter(piece => piece.role == Knight || piece.role == Pawn).flatMap: piece =>
          Option.when(move.after.check.yes)(()).flatMap: _ =>
            Square.all.find: target =>
              move.after.position.pieceAt(target).exists: targetPiece =>
                targetPiece.color == !piece.color &&
                  (targetPiece.role == Queen || targetPiece.role == Rook) &&
                  move.after.position.board.attackers(target, piece.color).contains(move.dest)
            .map(RoyalFork(move, _))
      .nextOption()

  private def legalMoves(position: Position): Vector[chess.Move] =
    Square.all
      .filter(square => position.pieceAt(square).exists(_.color == position.color))
      .flatMap(position.generateMovesAt)
      .toVector

  private def pieceValue(role: chess.Role): Int = role match
    case Pawn            => 100
    case Knight          => 300
    case chess.Bishop    => 300
    case Rook            => 500
    case Queen           => 900
    case King            => 0

  private def deltaClaim(
      id: String,
      evidenceId: String,
      route: String,
      owner: Color,
      status: ClaimStatus = ClaimStatus.Admitted,
      impact: ClaimImpact
  ): CommentaryClaim =
    val ownerKey = colorKey(owner)
    val defenderKey = colorKey(!owner)
    CommentaryClaim(
      id = id,
      layer = ClaimLayer.Delta,
      status = status,
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
