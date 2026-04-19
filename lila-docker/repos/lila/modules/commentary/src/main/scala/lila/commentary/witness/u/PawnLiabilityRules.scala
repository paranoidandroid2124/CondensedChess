package lila.commentary.witness.u

import chess.{ Color, File, Pawn, Square }

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors }
import lila.commentary.witness.*
import lila.commentary.witness.u.UWitnessHelpers.*

private[u] final case class ContactVariant(
    token: String,
    arrivalSquare: Square,
    targetPawnSquares: Vector[Square],
    breakPointSquares: Vector[Square]
)

private[u] object WeakPawnTargetStateRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("weak_pawn_target_state")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: defenderColor =>
      val beneficiaryColor = !defenderColor
      context.activePieceSquares(defenderColor, Pawn).flatMap: square =>
        val weaknessTags =
          Vector(
            Option.when(context.hasColorPawnSquare(SchemaId.FixedPawn, defenderColor, square))("fixed"),
            Option.when(context.hasColorPawnSquare(SchemaId.BackwardPawn, defenderColor, square))("backward"),
            Option.when(context.hasColorPawnSquare(SchemaId.IsolatedPawn, defenderColor, square))("isolated")
          ).flatten

        Option.when(weaknessTags.nonEmpty):
          beneficiary(
            color = beneficiaryColor,
            anchor = WitnessAnchor.PieceSquareAnchor(square),
            payload = WitnessPayload(
              "square" -> WitnessValue.SquareValue(square),
              "weakness_tags" -> WitnessValue.TokenListValue(weaknessTags)
            ),
            support = rootSupport(
              indices = Vector(
                context.pieceOnRootIndex(defenderColor, Pawn, square),
                context.colorPawnSquareRootIndex(SchemaId.FixedPawn, defenderColor, square),
                context.colorPawnSquareRootIndex(SchemaId.BackwardPawn, defenderColor, square),
                context.colorPawnSquareRootIndex(SchemaId.IsolatedPawn, defenderColor, square)
              ).flatten,
              targetSquares = Vector(square)
            )
          )

private[u] object PassedPawnEntityStateRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("passed_pawn_entity_state")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: ownerColor =>
      context.activePieceSquares(ownerColor, Pawn).collect:
        case square if context.hasColorPawnSquare(SchemaId.PassedPawn, ownerColor, square) =>
          owner(
            color = ownerColor,
            anchor = WitnessAnchor.PieceSquareAnchor(square),
            payload = WitnessPayload(
              "square" -> WitnessValue.SquareValue(square),
              "owner" -> WitnessValue.ColorValue(ownerColor)
            ),
            support = rootSupport(
              indices = Vector(
                context.pieceOnRootIndex(ownerColor, Pawn, square),
                context.colorPawnSquareRootIndex(SchemaId.PassedPawn, ownerColor, square)
              ).flatten,
              targetSquares = Vector(square)
            )
          )

private[u] object SectorAsymmetryStateRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("sector_asymmetry_state")

  def extract(context: UExtractionContext): Vector[Witness] =
    WitnessSector.values.toVector.flatMap: sector =>
      val whiteCount = sectorPawnCount(context, Color.White, sector)
      val blackCount = sectorPawnCount(context, Color.Black, sector)

      Option.when(whiteCount != blackCount):
        neutral(
          anchor = WitnessAnchor.SectorAnchor(sector),
          payload = WitnessPayload.from(
            Vector(
              "sector" -> WitnessValue.SectorValue(sector),
              "white_pawn_count" -> WitnessValue.Number(whiteCount),
              "black_pawn_count" -> WitnessValue.Number(blackCount)
            ) ++ majorityMinorityEntries(whiteCount, blackCount)
          ),
          support = rootSupport(
            indices =
              context.activePieceSquares(Color.White, Pawn).collect {
                case square if sectorOf(square.file) == sector =>
                  context.pieceOnRootIndex(Color.White, Pawn, square).get
              } ++
                context.activePieceSquares(Color.Black, Pawn).collect {
                  case square if sectorOf(square.file) == sector =>
                    context.pieceOnRootIndex(Color.Black, Pawn, square).get
                }
          )
        )

  private def sectorPawnCount(
      context: UExtractionContext,
      color: Color,
      sector: WitnessSector
  ): Int =
    context.activePieceSquares(color, Pawn).count(square => sectorOf(square.file) == sector)

  private def majorityMinorityEntries(
      whiteCount: Int,
      blackCount: Int
  ): Vector[(String, WitnessValue)] =
    if whiteCount > blackCount then
      Vector(
        "majority_side" -> WitnessValue.ColorValue(Color.White),
        "minority_side" -> WitnessValue.ColorValue(Color.Black)
      )
    else
      Vector(
        "majority_side" -> WitnessValue.ColorValue(Color.Black),
        "minority_side" -> WitnessValue.ColorValue(Color.White)
      )

private[u] object AvailableLeverTriggerRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("available_lever_trigger")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: ownerColor =>
      context.activePieceSquares(ownerColor, Pawn).flatMap: square =>
        val variants = leverVariants(context, ownerColor, square)
        val availableVariantTokens = variants.map(_.variantToken)
        val payload = WitnessPayload(
          "owner_pawn_square" -> WitnessValue.SquareValue(square),
          "available_variants" -> WitnessValue.TokenListValue(availableVariantTokens)
        )

        Option
          .when(context.hasColorPawnSquare(SchemaId.LeverAvailable, ownerColor, square) && variants.nonEmpty):
            variants.map: variant =>
              owner(
                color = ownerColor,
                anchor = WitnessAnchor.PieceSquareAnchor(square),
                payload = payload,
                support = rootSupport(
                  indices = Vector(
                    context.pieceOnRootIndex(ownerColor, Pawn, square),
                    context.colorPawnSquareRootIndex(SchemaId.LeverAvailable, ownerColor, square)
                  ).flatten,
                  targetSquares = variants.flatMap(_.targetSquares)
                ),
                variant = variant.variantId
              )
          .getOrElse(Vector.empty)

  private final case class LeverVariant(
      variantId: Option[WitnessVariantId],
      variantToken: String,
      targetSquares: Vector[Square]
  )

  private def leverVariants(
      context: UExtractionContext,
      ownerColor: Color,
      square: Square
  ): Vector[LeverVariant] =
    context.pawnAdvancesFrom(ownerColor, square).flatMap: advance =>
      val targetSquares = immediatePawnTargetsFromArrival(context, ownerColor, advance.to)
      val variant =
        if advance.steps == 1 then SinglePushLeverStateVariant -> "single_push_lever_state"
        else DoublePushLeverStateVariant -> "double_push_lever_state"
      Option.when(targetSquares.nonEmpty)(LeverVariant(variant._1, variant._2, targetSquares))

private[u] object PawnPushBreakContactSourceRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("pawn_push_break_contact_source")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: ownerColor =>
      context.activePieceSquares(ownerColor, Pawn).flatMap: anchorSquare =>
        val contactVariants = strategicContactVariants(context, ownerColor, anchorSquare)
        Option.when(
          context.hasColorPawnSquare(SchemaId.LeverAvailable, ownerColor, anchorSquare) &&
            contactVariants.nonEmpty
        ):
          owner(
            color = ownerColor,
            anchor = WitnessAnchor.PieceSquareAnchor(anchorSquare),
            payload = WitnessPayload(
              "owner_pawn_square" -> WitnessValue.SquareValue(anchorSquare),
              "contact_variants" -> objectList(contactVariants.map(variantPayload))
            ),
            support = rootSupport(
              indices = Vector(
                context.pieceOnRootIndex(ownerColor, Pawn, anchorSquare),
                context.colorPawnSquareRootIndex(SchemaId.LeverAvailable, ownerColor, anchorSquare)
              ).flatten ++ contactVariants.flatMap(variant =>
                variant.targetPawnSquares.flatMap(target => strategicTargetRootIndices(context, !ownerColor, target))
              ),
              targetSquares = contactVariants.flatMap(_.targetPawnSquares)
            )
          )

  private def strategicContactVariants(
      context: UExtractionContext,
      ownerColor: Color,
      anchorSquare: Square
  ): Vector[ContactVariant] =
    context.pawnAdvancesFrom(ownerColor, anchorSquare).flatMap: advance =>
      val defenderColor = !ownerColor
      val strategicTargets =
        immediatePawnTargetsFromArrival(context, ownerColor, advance.to).filter(target =>
          isStrategicContactTarget(context, defenderColor, target)
        )
      Option.when(strategicTargets.nonEmpty):
        ContactVariant(
          token = if advance.steps == 1 then "single_push" else "double_push",
          arrivalSquare = advance.to,
          targetPawnSquares = strategicTargets,
          breakPointSquares = strategicTargets.filter(target => isBreakPoint(context, ownerColor, defenderColor, target))
        )

  private def variantPayload(variant: ContactVariant): WitnessPayload =
    WitnessPayload.from(
      Vector(
        "push_variant" -> WitnessValue.Token(variant.token),
        "arrival_square" -> WitnessValue.SquareValue(variant.arrivalSquare),
        "target_pawn_squares" -> WitnessValue.SquareListValue(variant.targetPawnSquares)
      ) ++ Option.when(variant.breakPointSquares.nonEmpty)(
        "break_point_squares" -> WitnessValue.SquareListValue(variant.breakPointSquares)
      )
    )

  private def isStrategicContactTarget(
      context: UExtractionContext,
      defenderColor: Color,
      square: Square
  ): Boolean =
    isStructurallyBurdened(context, defenderColor, square) ||
      isCenterPawnTarget(square) ||
      isChainBaseTarget(context, defenderColor, square) ||
      isChainHeadTarget(context, defenderColor, square) ||
      isPhalanxEdgeTarget(context, defenderColor, square)

  private def strategicTargetRootIndices(
      context: UExtractionContext,
      defenderColor: Color,
      square: Square
  ): Vector[Int] =
    Vector(
      context.pieceOnRootIndex(defenderColor, Pawn, square),
      context.colorPawnSquareRootIndex(SchemaId.FixedPawn, defenderColor, square),
      context.colorPawnSquareRootIndex(SchemaId.BackwardPawn, defenderColor, square),
      context.colorPawnSquareRootIndex(SchemaId.IsolatedPawn, defenderColor, square)
    ).flatten

  private def isStructurallyBurdened(
      context: UExtractionContext,
      defenderColor: Color,
      square: Square
  ): Boolean =
    context.hasColorPawnSquare(SchemaId.FixedPawn, defenderColor, square) ||
      context.hasColorPawnSquare(SchemaId.BackwardPawn, defenderColor, square) ||
      context.hasColorPawnSquare(SchemaId.IsolatedPawn, defenderColor, square)

  private def isCenterPawnTarget(square: Square): Boolean =
    Set(File.C, File.D, File.E, File.F).contains(square.file)

  private def isChainBaseTarget(
      context: UExtractionContext,
      defenderColor: Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      supportsForward(context, defenderColor, square) &&
      !supportedFromRear(context, defenderColor, square)

  private def isChainHeadTarget(
      context: UExtractionContext,
      defenderColor: Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      supportedFromRear(context, defenderColor, square) &&
      !supportsForward(context, defenderColor, square)

  private def isPhalanxEdgeTarget(
      context: UExtractionContext,
      defenderColor: Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      sameRankAdjacentCount(context, defenderColor, square) == 1

  private def supportsForward(
      context: UExtractionContext,
      defenderColor: Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      square.pawnAttacks(defenderColor).squares.exists(target => context.hasPieceOn(defenderColor, Pawn, target))

  private def supportedFromRear(
      context: UExtractionContext,
      defenderColor: Color,
      square: Square
  ): Boolean =
    context.hasPieceOn(defenderColor, Pawn, square) &&
      square.pawnAttacks(!defenderColor).squares.exists(source => context.hasPieceOn(defenderColor, Pawn, source))

  private def sameRankAdjacentCount(
      context: UExtractionContext,
      defenderColor: Color,
      square: Square
  ): Int =
    Vector(square.file.value - 1, square.file.value + 1)
      .flatMap(File(_))
      .count(file => context.hasPieceOn(defenderColor, Pawn, Square(file, square.rank)))

  private def isBreakPoint(
      context: UExtractionContext,
      ownerColor: Color,
      defenderColor: Color,
      square: Square
  ): Boolean =
    context.hasColorPawnSquare(SchemaId.FixedPawn, defenderColor, square) ||
      context.hasNeutralSquare(SchemaId.Contested, square) ||
      context.hasColorSquare(SchemaId.ControlledBy, ownerColor, square)

private[u] def immediatePawnTargetsFromArrival(
    context: UExtractionContext,
    ownerColor: Color,
    arrivalSquare: Square
): Vector[Square] =
  arrivalSquare.pawnAttacks(ownerColor).squares.toVector.filter(target =>
    context.hasPieceOn(!ownerColor, Pawn, target)
  )
