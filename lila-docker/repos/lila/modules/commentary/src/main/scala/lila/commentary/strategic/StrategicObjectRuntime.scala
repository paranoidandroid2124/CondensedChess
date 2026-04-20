package lila.commentary.strategic

import chess.Color

import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessSupport }

private[strategic] trait StrategicObjectRule:
  def familyId: StrategicObjectId
  def extract(
      context: StrategicObjectContext,
      extractedSoFar: StrategicObjectSet
  ): Vector[StrategicObject]

  protected final def neutral(
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty
  ): StrategicObject =
    StrategicObject(
      familyId = familyId,
      anchor = anchor,
      color = None,
      payload = payload,
      support = support
    )

  protected final def owned(
      color: Color,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty
  ): StrategicObject =
    StrategicObject(
      familyId = familyId,
      anchor = anchor,
      color = Some(color),
      payload = payload,
      support = support
    )

private[strategic] object StrategicInternalRuntime:

  private def validateRegisteredRules(
      candidateRules: IterableOnce[StrategicObjectRule]
  ): Vector[StrategicObjectRule] =
    val rules = candidateRules.iterator.toVector
    StrategicObjectScopeContract.requireActiveObjectFamilyIds(rules.map(_.familyId))
    rules

  private val rules: Vector[StrategicObjectRule] = validateRegisteredRules(
    Vector(
      CentralContactFrontRule,
      DistributedContactRegimeRule,
      EndgameRaceScaffoldRule,
      OpeningDevelopmentRegimeRule,
      AttackScaffoldRule,
      FortressHoldingShellRule,
      KingSafetyShellRule
    )
  )

  def extract(context: StrategicObjectContext): StrategicObjectSet =
    rules.foldLeft(StrategicObjectSet.empty): (acc, rule) =>
      StrategicObjectSet(acc.all ++ rule.extract(context, acc))
