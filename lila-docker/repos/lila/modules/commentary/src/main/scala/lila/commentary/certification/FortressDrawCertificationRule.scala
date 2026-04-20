package lila.commentary.certification

import chess.Color

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object FortressDrawCertificationRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("FortressDrawCertification")
  val scope: CertificationScope = CertificationScope.CurrentPosition
  val burdenTag: CertificationBurdenTag =
    CertificationBurdenTag("fortress_hold_certification")
  protected val helperTags: Vector[String] =
    Vector("fortress_draw_burden", "best_defense_survival")
  override protected val requiredSupportFamilies: Vector[CertificationSupportFamily] =
    Vector(CertificationSupportFamily("FortressHoldingShell"))
  // The shell gates admission; explicit best-defense evidence is what lifts the
  // hold from support-only to certified.
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(CertificationEvidencePurpose.BestDefenseSurvival)
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.SupportOnly

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    context.current.board.kingSquare(color).flatMap: kingSquare =>
      squareObject(context, "FortressHoldingShell", color, kingSquare).map: shell =>
        CertificationCandidate(
          payload = WitnessPayload(
            "holder" -> WitnessValue.ColorValue(color),
            "king_square" -> WitnessValue.SquareValue(kingSquare)
          ),
          support = support(
            indices = shell.support.rootIndices,
            targetSquares = shell.support.targetSquares
          )
        )
