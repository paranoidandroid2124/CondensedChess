package lila.commentary.certification

import chess.Color

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object MaterialHarvestRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("MaterialHarvest")
  val scope: CertificationScope = CertificationScope.CurrentPosition
  val burdenTag: CertificationBurdenTag =
    CertificationBurdenTag("realized_material_conversion")
  protected val helperTags: Vector[String] =
    Vector("material_conversion_realization", "best_defense_survival")
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(
      CertificationEvidencePurpose.BestDefenseSurvival,
      CertificationEvidencePurpose.TacticalReleaseDetection
    )
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.SupportOnly

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    captureOptions(context, color)
      .flatMap: (move, capturedPiece) =>
        context.current.pieceAt(move.orig).map(originPiece =>
          val capturedValue = context.current.board.pieceValue(capturedPiece.role)
          val originValue = context.current.board.pieceValue(originPiece.role)
          (move, capturedPiece, originPiece, capturedValue, originValue)
        )
      .filter: (_, capturedPiece, originPiece, capturedValue, _) =>
        capturedPiece.role != chess.Pawn &&
          capturedValue >= context.current.board.pieceValue(chess.Knight) &&
          capturedPiece.role != originPiece.role
      .filter: (move, _, _, _, _) =>
        immediateRecaptureReplies(move).isEmpty
      .sortBy: (move, _, _, capturedValue, originValue) =>
        (capturedValue, -originValue, move.orig.value, move.dest.value)
      .lastOption
      .map: (move, capturedPiece, originPiece, capturedValue, _) =>
        CertificationCandidate(
          payload = WitnessPayload(
            "owner" -> WitnessValue.ColorValue(color),
            "capture_from" -> WitnessValue.SquareValue(move.orig),
            "capture_to" -> WitnessValue.SquareValue(move.dest),
            "captured_role" -> WitnessValue.RoleValue(capturedPiece.role),
            "capturing_role" -> WitnessValue.RoleValue(originPiece.role),
            "material_swing" -> WitnessValue.Number(capturedValue)
          ),
          support = support(
            indices =
              Vector(
                context.current.pieceOnRootIndex(color, originPiece.role, move.orig),
                context.current.pieceOnRootIndex(!color, capturedPiece.role, move.dest)
              ).flatten,
            targetSquares = Vector(move.dest)
          )
        )

  private def immediateRecaptureReplies(move: chess.Move): Vector[chess.Move] =
    legalMoves(move.after.position).filter(_.dest == move.dest)
