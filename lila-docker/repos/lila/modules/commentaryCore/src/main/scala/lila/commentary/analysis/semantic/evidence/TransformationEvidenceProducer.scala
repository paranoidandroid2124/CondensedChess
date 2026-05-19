package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceProducer }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import lila.commentary.model.{ PlanId }
import lila.commentary.model.strategic.{ PositionalTag, TheoreticalOutcomeHint }
import lila.commentary.model.structure.{ StructureId }


private[commentary] object TransformationEvidenceProducer extends StrategicIdeaEvidenceProducer:

  import StrategicIdeaEvidenceSupport.*

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val side = pack.sideToMove
    val removingTheDefender =
      semantic.positionalFeatures.collect {
        case PositionalTag.RemovingTheDefender(target, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.RemovingTheDefender,
            confidence = 0.84,
            beneficiaryPieces = List(roleToken(target)),
            factIds = List("removing_the_defender", s"removing_defender_${target.name.toLowerCase}")
          )
      }

    val winningEndgameTransition =
      semantic.endgameFeatures
        .filter(_.theoreticalOutcomeHint == TheoreticalOutcomeHint.Win)
        .flatMap { feature =>
          semantic.positionFeatures.flatMap { features =>
            Option.when(materialEdgeFor(side, features) >= 100 || semantic.phase == "endgame") {
              evidence(
                ownerSide = side,
                kind = StrategicIdeaKind.FavorableTradeOrTransformation,
                readiness = StrategicIdeaReadiness.Ready,
                source = EvidenceSourceId.WinningEndgameTransition,
                confidence = 0.80,
                focusSquares = feature.keySquaresControlled.map(_.key).take(3),
                factIds = List("winning_endgame_transition")
              )
            }
          }
        }
        .toList

    val classificationWindow =
      semantic.classification.toList.flatMap { classification =>
        semantic.positionFeatures.flatMap { features =>
          val evalEdge = materialEdgeFor(side, features)
          val facts =
            List(
              Option.when(classification.simplifyBias.shouldSimplify)("simplify_window"),
              Option.when(classification.taskMode.isConvertMode)("convert_mode"),
              Option.when(hasAlignmentReason(semantic, "TRANSFORMATION"))("alignment_transformation")
            ).flatten
          Option.when(facts.nonEmpty && (evalEdge >= 80 || classification.taskMode.isConvertMode)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.FavorableTradeOrTransformation,
              readiness = if evalEdge >= 160 then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.ClassificationTransformationWindow,
              confidence = 0.62 + Option.when(classification.taskMode.isConvertMode)(0.02).getOrElse(0.0),
              factIds = List("classification_transformation_window") ++ facts
            )
          }
        }
      }

    val exchangeAvailabilityBridge =
      semantic.classification.toList.flatMap { classification =>
        Option.when(
          classification.simplifyBias.exchangeAvailable &&
            structureIs(semantic, StructureId.IQPBlack) &&
            side == "white"
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.ExchangeAvailabilityBridge,
            confidence = 0.64,
            factIds =
              List(
                Some("exchange_availability_bridge"),
                Some("structure_iqp_black")
              ).flatten
          )
        }
      }

    val moveRefEvidence =
      pack.pieceMoveRefs
        .filter(ref => ref.ownerSide == side && ref.tacticalTheme.contains("capture_or_exchange"))
        .flatMap { ref =>
          Option.when(favorableTradeContext(side, ref, semantic, removingTheDefender.nonEmpty || winningEndgameTransition.nonEmpty)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.FavorableTradeOrTransformation,
              readiness =
                if winningEndgameTransition.nonEmpty || removingTheDefender.nonEmpty then StrategicIdeaReadiness.Ready
                else StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.CaptureExchangeTransformation,
              confidence = 0.62 + moveRefSupportBonus(side, ref, semantic),
              focusSquares = List(ref.target),
              beneficiaryPieces = List(ref.piece),
              factIds = List("capture_or_exchange") ++ ref.evidence.filter(_.startsWith("target_"))
            )
          }
        }

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          (
            plan.plan.id == PlanId.Exchange ||
              plan.plan.id == PlanId.Simplification ||
              plan.plan.id == PlanId.QueenTrade
          ) &&
            (
              removingTheDefender.nonEmpty ||
                winningEndgameTransition.nonEmpty ||
                classificationWindow.nonEmpty ||
                exchangeAvailabilityBridge.nonEmpty ||
                structureIs(semantic, StructureId.IQPBlack)
            )
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PlanMatchTransformation,
            confidence = 0.68 + math.min(0.04, plan.score * 0.06),
            factIds = List("plan_match_transformation", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val iqpSimplification =
      semantic.classification.toList.flatMap { classification =>
        val exchangeMoveRefs =
          pack.pieceMoveRefs.filter(ref =>
            ref.ownerSide == side && ref.tacticalTheme.contains("capture_or_exchange")
          )
        val exchangePlanSupport =
          topPlansFor(side, semantic).exists(plan =>
            plan.plan.id == PlanId.Exchange ||
              plan.plan.id == PlanId.Simplification ||
              plan.plan.id == PlanId.QueenTrade
          )
        Option.when(
          structureIs(semantic, StructureId.IQPBlack) &&
            side == "white" &&
            (
              classification.simplifyBias.exchangeAvailable ||
                classification.simplifyBias.shouldSimplify ||
                exchangeMoveRefs.nonEmpty ||
                exchangePlanSupport
            )
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.IqpSimplificationProfile,
            confidence =
              if exchangeMoveRefs.nonEmpty || exchangePlanSupport then 0.78
              else 0.64,
            focusSquares = exchangeMoveRefs.map(_.target).distinct.take(3),
            factIds =
              List("structure_iqp_black", "iqp_simplification_profile") ++
                Option.when(exchangeMoveRefs.nonEmpty)("capture_or_exchange").toList ++
                Option.when(exchangePlanSupport)("iqp_trade_down_plan").toList
          )
        }
      }

    removingTheDefender ++ winningEndgameTransition ++ classificationWindow ++ exchangeAvailabilityBridge ++ moveRefEvidence ++ planBridge ++ iqpSimplification
