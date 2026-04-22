package lila.commentary.witness.seed

import chess.{ Color, King, Position, Role, Square }
import chess.variant

import lila.commentary.root.RootAtomRegistry.canonicalColors
import lila.commentary.witness.*
import lila.commentary.witness.seed.StrategySupportSeedHelpers.*
import lila.commentary.witness.u.UExtractionContext

private[seed] final case class PreparedTargetDependency(
    beneficiaryColor: Color,
    targetColor: Color,
    targetSquare: Square,
    targetRole: Role,
    dependencyDefenderSquare: Square,
    dependencyDefenderRole: Role,
    attackSourceSquares: Vector[Square],
    rootIndices: Vector[Int]
)

private[seed] object PreparedTargetDependency:

  def all(context: UExtractionContext): Vector[PreparedTargetDependency] =
    canonicalColors.flatMap: targetColor =>
      val beneficiaryColor = !targetColor
      context.board.squaresOf(targetColor).flatMap: targetSquare =>
        context.pieceAt(targetSquare).toVector.flatMap: targetPiece =>
          val attackSources = legalAttackersOf(context, targetSquare, beneficiaryColor)
          val dependencyDefenders =
            context.board
              .attackersOf(targetSquare, targetColor)
              .filterNot(_ == targetSquare)
              .filter(square => context.pieceAt(square).exists(_.role != King))
              .sortBy(_.value)
          val hasSingleLowerValueDefender =
            dependencyDefenders.headOption.exists: defenderSquare =>
              context
                .pieceAt(defenderSquare)
                .exists(defender => pieceValue(defender.role) < pieceValue(targetPiece.role))

          Option.when(
            targetPiece.role != King &&
              attackSources.nonEmpty &&
              dependencyDefenders.size == 1 &&
              hasSingleLowerValueDefender
          ):
            val defenderSquare = dependencyDefenders.head
            val defenderRole = context.pieceAt(defenderSquare).get.role
            PreparedTargetDependency(
              beneficiaryColor = beneficiaryColor,
              targetColor = targetColor,
              targetSquare = targetSquare,
              targetRole = targetPiece.role,
              dependencyDefenderSquare = defenderSquare,
              dependencyDefenderRole = defenderRole,
              attackSourceSquares = attackSources,
              rootIndices = Vector(
                context.pieceOnRootIndex(targetColor, targetPiece.role, targetSquare),
                context.pieceOnRootIndex(targetColor, defenderRole, defenderSquare)
              ).flatten ++ attackSources.flatMap(source =>
                context.pieceAt(source).flatMap(piece => context.pieceOnRootIndex(beneficiaryColor, piece.role, source))
              )
            )

  private def legalAttackersOf(
      context: UExtractionContext,
      targetSquare: Square,
      attackerColor: Color
  ): Vector[Square] =
    val position = Position(context.board.toBoard, variant.Standard, attackerColor)
    context.board
      .attackersOf(targetSquare, attackerColor)
      .filter(origin => position.generateMovesAt(origin).exists(_.dest == targetSquare))
      .sortBy(_.value)

private[seed] object TargetResourceDependencySeedRule extends StrategySupportSeedRule:

  val seedId: StrategySupportSeedId =
    StrategySupportSeedScopeContract.S24TargetResourceDependency

  def extract(context: UExtractionContext): Vector[StrategySupportSeed] =
    PreparedTargetDependency.all(context).map: dependency =>
      beneficiary(
        color = dependency.beneficiaryColor,
        anchor = WitnessAnchor.PieceSquareAnchor(dependency.targetSquare),
        payload = WitnessPayload(
          "target_square" -> WitnessValue.SquareValue(dependency.targetSquare),
          "target_role" -> WitnessValue.RoleValue(dependency.targetRole),
          "dependency_defender_square" -> WitnessValue.SquareValue(dependency.dependencyDefenderSquare),
          "dependency_defender_role" -> WitnessValue.RoleValue(dependency.dependencyDefenderRole),
          "dependency_kind" -> WitnessValue.Token("single_defender"),
          "attack_source_squares" -> WitnessValue.SquareListValue(dependency.attackSourceSquares)
        ),
        support = rootSupport(
          indices = dependency.rootIndices,
          targetSquares = dependency.targetSquare +:
            dependency.dependencyDefenderSquare +:
            dependency.attackSourceSquares,
          supportingTags = Vector("single_defender_dependency")
        )
      )

private[seed] object TargetAttackConvergenceSeedRule extends StrategySupportSeedRule:

  val seedId: StrategySupportSeedId =
    StrategySupportSeedScopeContract.S24TargetAttackConvergence

  def extract(context: UExtractionContext): Vector[StrategySupportSeed] =
    PreparedTargetDependency.all(context).flatMap: dependency =>
      Option.when(dependency.attackSourceSquares.size >= 2):
        beneficiary(
          color = dependency.beneficiaryColor,
          anchor = WitnessAnchor.PieceSquareAnchor(dependency.targetSquare),
          payload = WitnessPayload(
            "target_square" -> WitnessValue.SquareValue(dependency.targetSquare),
            "target_role" -> WitnessValue.RoleValue(dependency.targetRole),
            "dependency_defender_square" -> WitnessValue.SquareValue(dependency.dependencyDefenderSquare),
            "attack_source_squares" -> WitnessValue.SquareListValue(dependency.attackSourceSquares)
          ),
          support = rootSupport(
            indices = dependency.rootIndices,
            targetSquares = dependency.targetSquare +:
              dependency.dependencyDefenderSquare +:
              dependency.attackSourceSquares,
            supportingTags = Vector("target_centered_attack_convergence")
          )
        )
