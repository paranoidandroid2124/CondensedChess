package lila.commentary.witness.seed

import chess.Color

import lila.commentary.witness.{
  WitnessAnchor,
  WitnessPayload,
  WitnessSupport,
  WitnessVariantId
}
import lila.commentary.witness.u.UExtractionContext

private[seed] trait StrategySupportSeedRule:
  def seedId: StrategySupportSeedId
  def extract(context: UExtractionContext): Vector[StrategySupportSeed]

  protected final def neutral(
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): StrategySupportSeed =
    StrategySupportSeed.neutral(seedId, anchor, payload, support, variant)

  protected final def owner(
      color: Color,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): StrategySupportSeed =
    StrategySupportSeed.owner(seedId, color, anchor, payload, support, variant)

  protected final def beneficiary(
      color: Color,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): StrategySupportSeed =
    StrategySupportSeed.beneficiary(seedId, color, anchor, payload, support, variant)

private[seed] object StrategySupportSeedRuntime:

  private[seed] def validateRegisteredRules(
      candidateRules: IterableOnce[StrategySupportSeedRule]
  ): Vector[StrategySupportSeedRule] =
    val rules = candidateRules.iterator.toVector
    StrategySupportSeedScopeContract.requireAllowedSeedIds(rules.map(_.seedId))

    val duplicateSeedIds =
      rules
        .groupBy(_.seedId.value)
        .collect { case (seedId, grouped) if grouped.size > 1 => seedId }
        .toVector
        .sorted

    require(
      duplicateSeedIds.isEmpty,
      s"Duplicate strategy support seed rule registrations are not allowed: ${duplicateSeedIds.mkString(", ")}"
    )

    rules

  private val rules: Vector[StrategySupportSeedRule] =
    validateRegisteredRules(
      Vector(
        SamePieceLiabilityAnchorSeedRule,
        SamePieceRepairRouteSeedRule,
        SamePieceExchangeReliefSeedRule,
        KingEntrySquareSeedRule,
        KingAccessRouteSeedRule,
        KingOppositionContactSeedRule,
        TargetResourceDependencySeedRule,
        TargetAttackConvergenceSeedRule,
        RankCorridorStateSeedRule
      )
    )

  val liveSeedIds: Vector[StrategySupportSeedId] =
    rules.map(_.seedId)

  def extract(context: UExtractionContext): StrategySupportSeedSet =
    StrategySupportSeedSet(rules.iterator.flatMap(_.extract(context)).toVector)
