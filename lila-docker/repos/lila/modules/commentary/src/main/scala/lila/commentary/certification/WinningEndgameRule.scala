package lila.commentary.certification

import chess.{ Bishop, Color, File, Knight, Pawn, Queen, Rook }

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object WinningEndgameRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("WinningEndgame")
  val scope: CertificationScope = CertificationScope.CurrentPosition
  val burdenTag: CertificationBurdenTag = CertificationBurdenTag("conversion_result")
  protected val helperTags: Vector[String] =
    Vector("winning_endgame_conversion", "best_defense_survival")
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(
      CertificationEvidencePurpose.BestDefenseSurvival,
      CertificationEvidencePurpose.ConversionRouteSurvival
    )
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.Deferred

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    val ownerPawns = context.current.activePieceSquares(color, Pawn)
    val rivalPawns = context.current.activePieceSquares(!color, Pawn)
    val ownerKingSquare = context.current.board.kingSquare(color)
    val rivalKingSquare = context.current.board.kingSquare(!color)
    val nonPawnMaterial =
      Vector(Knight, Bishop, Rook, Queen).exists(role =>
        context.current.activePieceSquares(Color.White, role).nonEmpty ||
          context.current.activePieceSquares(Color.Black, role).nonEmpty
      )

    Option.when(context.sideToMove == color):
      ownerPawns
      .filter(square =>
        if color.white then square.rank == chess.Rank.Sixth else square.rank == chess.Rank.Third
      )
      .filter(square => square.file != File.A && square.file != File.H)
      .find: pawnSquare =>
        ownerKingSquare.exists(king =>
          kingDistance(king, pawnSquare) <= 1 &&
            (if color.white then king.rank.value >= chess.Rank.Fifth.value
             else king.rank.value <= chess.Rank.Fourth.value)
        ) &&
          rivalKingSquare.exists(king =>
            kingDistance(king, pawnSquare) > 1 &&
              king.file != pawnSquare.file
          )
      .filter(_ => !nonPawnMaterial && ownerPawns.size == 1 && rivalPawns.isEmpty)
      .map: pawnSquare =>
        val currentOwnerKingSquare = ownerKingSquare.get
        CertificationCandidate(
          payload = WitnessPayload(
            "owner" -> WitnessValue.ColorValue(color),
            "runner_square" -> WitnessValue.SquareValue(pawnSquare),
            "owner_king_square" -> WitnessValue.SquareValue(currentOwnerKingSquare)
          )
        )
    .flatten
