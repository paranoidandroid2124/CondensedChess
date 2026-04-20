package lila.commentary.delta

import chess.Color

import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessSupport }

private[delta] trait StrategicDeltaRule:
  def familyId: StrategicDeltaId
  def extract(
      context: StrategicDeltaContext,
      extractedSoFar: StrategicDeltaSet
  ): Vector[StrategicDelta]

  protected final def neutral(
      scope: StrategicDeltaScope,
      deltaTag: StrategicDeltaTag,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty
  ): StrategicDelta =
    StrategicDelta(
      familyId = familyId,
      scope = scope,
      deltaTag = deltaTag,
      anchor = anchor,
      color = None,
      payload = payload,
      support = support
    )

  protected final def owned(
      color: Color,
      scope: StrategicDeltaScope,
      deltaTag: StrategicDeltaTag,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty
  ): StrategicDelta =
    StrategicDelta(
      familyId = familyId,
      scope = scope,
      deltaTag = deltaTag,
      anchor = anchor,
      color = Some(color),
      payload = payload,
      support = support
    )

private[delta] object StrategicInternalDeltaRuntime:

  private def validateRegisteredRules(
      candidateRules: IterableOnce[StrategicDeltaRule]
  ): Vector[StrategicDeltaRule] =
    val rules = candidateRules.iterator.toVector
    StrategicDeltaScopeContract.requireActiveDeltaFamilyIds(rules.map(_.familyId))
    require(
      rules.map(_.familyId).toSet == StrategicDeltaScopeContract.activeDeltaFamilyIds.toSet,
      s"Strategic delta runtime must register exactly ${StrategicDeltaScopeContract.activeDeltaFamilyIds.map(_.value).mkString(", ")}"
    )
    rules

  private val rules: Vector[StrategicDeltaRule] = validateRegisteredRules(
    Vector(
      TradeCompressionCorridorRule,
      TradeInvariantRule
    )
  )

  def extract(context: StrategicDeltaContext): StrategicDeltaSet =
    rules.foldLeft(StrategicDeltaSet.empty): (acc, rule) =>
      StrategicDeltaSet(acc.all ++ rule.extract(context, acc))
