package lila.commentary.witness.u

import chess.Color

import lila.commentary.witness.*

private[u] trait UAttachedRule:
  def extract(context: UExtractionContext): Vector[Witness]

private[u] trait UScopedAttachedRule extends UAttachedRule:
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

  protected final def host(
      anchor: WitnessAnchor,
      color: Option[Color] = None,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): Witness =
    Witness.host(descriptorId, anchor, color, payload, support, variant)

private[u] object UAttachedInternalRuntime:

  private[witness] def validateRegisteredRules(
      candidateRules: IterableOnce[UScopedAttachedRule]
  ): Vector[UScopedAttachedRule] =
    val rules = candidateRules.iterator.toVector
    UAttachedScopeContract.requireActiveAttachedDescriptorIds(rules.map(_.descriptorId))

    val duplicateDescriptorIds =
      rules
        .groupBy(_.descriptorId.value)
        .collect { case (descriptorId, grouped) if grouped.size > 1 => descriptorId }
        .toVector
        .sorted

    require(
      duplicateDescriptorIds.isEmpty,
      s"Duplicate U-attached rule registrations are not allowed: ${duplicateDescriptorIds.mkString(", ")}"
    )

    rules

  private val rules: Vector[UScopedAttachedRule] = validateRegisteredRules(
    Vector(
      StructuralSpaceClaimRule
    )
  )

  def extract(context: UExtractionContext): WitnessSet =
    WitnessSet(rules.iterator.flatMap(_.extract(context)).toVector)
