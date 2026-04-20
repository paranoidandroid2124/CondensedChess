package lila.commentary.certification

import chess.{ Bishop, Color, Knight, Queen, Rook }

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object PromotionRaceRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("PromotionRace")
  val scope: CertificationScope = CertificationScope.CurrentPosition
  val burdenTag: CertificationBurdenTag =
    CertificationBurdenTag("promotion_route_survival")
  protected val helperTags: Vector[String] =
    Vector("promotion_route_survival", "best_defense_survival")
  override protected val requiredSupportFamilies: Vector[CertificationSupportFamily] =
    Vector(CertificationSupportFamily("EndgameRaceScaffold"))
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
    Option
      .when(context.sideToMove == color && onlyKingsAndPawnsRemain(context))(())
      .flatMap(_ => boardObject(context, "EndgameRaceScaffold"))
      .flatMap: raceObject =>
      val ownerField = if color.white then "white_clear_run_squares" else "black_clear_run_squares"
      val rivalField = if color.white then "black_clear_run_squares" else "white_clear_run_squares"
      val ownerSquares = squareList(raceObject.payload, ownerField)
      val rivalSquares = squareList(raceObject.payload, rivalField)
      val ownerRace = ownerSquares.map(square => square -> promotionDistance(color, square)).sortBy(_._2).headOption
      val rivalRace = rivalSquares.map(square => square -> promotionDistance(!color, square)).sortBy(_._2).headOption
      val rivalKingSquare = context.current.board.kingSquare(!color)

      Option.when(
        ownerRace.nonEmpty &&
          rivalRace.nonEmpty &&
          rivalKingSquare.nonEmpty &&
          ownerRace.get._2 < rivalRace.get._2 + 1 &&
          kingDistance(rivalKingSquare.get, promotionSquare(color, ownerRace.get._1)) > ownerRace.get._2
      )(
        CertificationCandidate(
          payload = WitnessPayload(
            "owner" -> WitnessValue.ColorValue(color),
            "owner_clear_run_squares" -> WitnessValue.SquareListValue(ownerSquares),
            "rival_clear_run_squares" -> WitnessValue.SquareListValue(rivalSquares),
            "owner_promotion_distance" -> WitnessValue.Number(ownerRace.get._2),
            "rival_promotion_distance" -> WitnessValue.Number(rivalRace.get._2)
          ),
          support = support(
            indices = raceObject.support.rootIndices,
            targetSquares = raceObject.support.targetSquares
          )
        )
      )

  private def promotionDistance(color: Color, square: chess.Square): Int =
    if color.white then chess.Rank.Eighth.value - square.rank.value else square.rank.value

  private def promotionSquare(color: Color, square: chess.Square): chess.Square =
    chess.Square
      .at(square.file.value, if color.white then chess.Rank.Eighth.value else chess.Rank.First.value)
      .get

  private def onlyKingsAndPawnsRemain(context: CertificationContext): Boolean =
    Vector(Knight, Bishop, Rook, Queen).forall(role =>
      context.current.activePieceSquares(Color.White, role).isEmpty &&
        context.current.activePieceSquares(Color.Black, role).isEmpty
    )
