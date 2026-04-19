package lila.commentary.witness.u

import chess.Color

import lila.commentary.witness.*

private[u] trait UWitnessRule:
  def extract(context: UExtractionContext): Vector[Witness]

private[u] trait UScopedWitnessRule extends UWitnessRule:
  def descriptorId: WitnessDescriptorId

  protected final def neutral(
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): Witness =
    Witness.neutral(descriptorId, anchor, payload, support, variant)

  protected final def owner(
      color: Color,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): Witness =
    Witness.owner(descriptorId, color, anchor, payload, support, variant)

  protected final def beneficiary(
      color: Color,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): Witness =
    Witness.beneficiary(descriptorId, color, anchor, payload, support, variant)

private[u] object UInternalRuntime:

  private[witness] def validateRegisteredRules(
      candidateRules: IterableOnce[UScopedWitnessRule]
  ): Vector[UScopedWitnessRule] =
    val rules = candidateRules.iterator.toVector
    UScopeContract.requireActivePrimaryDescriptorIds(rules.map(_.descriptorId))

    val duplicateDescriptorIds =
      rules
        .groupBy(_.descriptorId.value)
        .collect { case (descriptorId, grouped) if grouped.size > 1 => descriptorId }
        .toVector
        .sorted

    require(
      duplicateDescriptorIds.isEmpty,
      s"Duplicate U witness rule registrations are not allowed: ${duplicateDescriptorIds.mkString(", ")}"
    )

    rules

  private val rules: Vector[UScopedWitnessRule] = validateRegisteredRules(
    Vector(
      FileLaneStateRule,
      DiagonalLaneOnlyRule,
      WeakPawnTargetStateRule,
      PassedPawnEntityStateRule,
      WeakOutpostSquareStateRule,
      LoosePieceTargetStateRule,
      PawnPushBreakContactSourceRule,
      SectorAsymmetryStateRule,
      AvailableLeverTriggerRule,
      RookOnOpenFileStateRule,
      BishopPairStateRule,
      KnightOnOutpostSquareRule,
      DutyBoundDefenderRule,
      ShortRunSliderGateRestrictionRule,
      PinRule,
      ForkRule,
      SkewerRule,
      OverloadRule
    )
  )

  def extract(context: UExtractionContext): WitnessSet =
    WitnessSet(rules.iterator.flatMap(_.extract(context)).toVector)
