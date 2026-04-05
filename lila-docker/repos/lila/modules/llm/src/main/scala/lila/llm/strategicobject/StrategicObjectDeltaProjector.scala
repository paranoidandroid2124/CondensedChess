package lila.llm.strategicobject

import lila.llm.analysis.DecisiveTruthContract

enum StrategicDeltaScope:
  case MoveLocal
  case PositionLocal
  case Comparative

final case class StrategicObjectDelta(
    objectId: String,
    scope: StrategicDeltaScope,
    evidenceRefs: List[String] = Nil
)

trait StrategicObjectDeltaProjector:
  def project(
      contract: DecisiveTruthContract,
      objects: List[StrategicObject]
  ): List[StrategicObjectDelta]

object CanonicalStrategicObjectDeltaProjector extends StrategicObjectDeltaProjector:

  def project(
      contract: DecisiveTruthContract,
      objects: List[StrategicObject]
  ): List[StrategicObjectDelta] =
    objects.flatMap { obj =>
      eligibleScopes(contract, obj).map { scope =>
        StrategicObjectDelta(
          objectId = obj.id,
          scope = scope,
          evidenceRefs = evidenceRefs(obj, scope)
        )
      }
    }.sortBy(delta => (delta.objectId, delta.scope.ordinal))

  private def eligibleScopes(
      contract: DecisiveTruthContract,
      obj: StrategicObject
  ): List[StrategicDeltaScope] =
    obj.readiness match
      case StrategicObjectReadiness.Stable =>
        List(
          StrategicDeltaScope.MoveLocal,
          StrategicDeltaScope.PositionLocal,
          StrategicDeltaScope.Comparative
        )
      case StrategicObjectReadiness.Provisional =>
        List(StrategicDeltaScope.PositionLocal) ++
          Option.when(contract.hasVisibleTruth || contract.prefersDecisivePromotion)(
            StrategicDeltaScope.Comparative
          )
      case StrategicObjectReadiness.DeferredForDelta =>
        Nil

  private def evidenceRefs(
      obj: StrategicObject,
      scope: StrategicDeltaScope
  ): List[String] =
    obj.supportingPrimitives
      .take(4)
      .map { ref =>
        val anchor = ref.anchorSquares.headOption.map(_.key).getOrElse("cluster")
        s"${scope.toString.toLowerCase}:${ref.kind}:$anchor"
      }
