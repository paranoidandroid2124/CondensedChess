package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceProducer }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import _root_.chess.{ File }
import lila.commentary.model.{ PlanId }


private[commentary] object ProphylaxisEvidenceProducer extends StrategicIdeaEvidenceProducer:

  import StrategicIdeaEvidenceSupport.*

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val side = pack.sideToMove
    val preventedEvidence =
      semantic.preventedPlans.flatMap { prevented =>
        Option.when(isPreventiveWithoutCounterplaySuppression(prevented)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.Prophylaxis,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.PreventedPlan,
            confidence = 0.78 + prophylaxisThreatBonus(prevented),
            focusSquares = prevented.deniedSquares.map(_.key).take(3),
            focusFiles = prevented.breakNeutralized.toList.flatMap(normalizeFileToken),
            factIds =
              List("prevented_plan") ++
                Option.when(prevented.preventedThreatType.isDefined)("prevented_threat").toList ++
                Option.when(prevented.mobilityDelta < 0)("prevented_mobility").toList ++
                Option.when(prevented.deniedResourceClass.contains("forcing_threat"))("denied_forcing_threat").toList
          )
        }
      }

    val threatBridge =
      semantic.threatsToUs.toList.flatMap { threats =>
        Option.when(isThreatDrivenProphylaxis(threats)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.Prophylaxis,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.ThreatAnalysisProphylaxis,
            confidence = 0.76 + threatDefenseBonus(threats),
            focusSquares = threatSquares(threats),
            focusZone = threatFocusZone(threats),
            factIds =
              List("threat_analysis_prophylaxis") ++
                Option.when(threats.prophylaxisNeeded)("prophylaxis_needed").toList ++
                Option.when(threats.defense.prophylaxisNeeded)("defensive_prophylaxis").toList ++
                Option.when(threats.resourceAvailable)("defensive_resources_available").toList
          )
        }
      }

    val counterBreakWatch =
      semantic.opponentPawnAnalysis.toList.flatMap { analysis =>
        val file = analysis.breakFile.flatMap(normalizeFileToken)
        Option.when(analysis.counterBreak && semantic.threatsToUs.exists(isThreatDrivenProphylaxis)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.Prophylaxis,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.OpponentCounterbreakWatch,
            confidence = 0.70,
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken),
            factIds = List("opponent_counter_break_watch", "opponent_counter_break")
          )
        }
      }

    val realAnchorPresent =
      preventedEvidence.nonEmpty ||
        threatBridge.nonEmpty ||
        counterBreakWatch.nonEmpty ||
        hasStablePlanEvidence(semantic, StrategicIdeaKind.Prophylaxis)

    val planSupportPresent =
      topPlansFor(side, semantic).exists(plan =>
        plan.plan.id == PlanId.Prophylaxis || plan.plan.id == PlanId.DefensiveConsolidation
      )

    val boardPatternPresent =
      hasBishopPinWatch(side, semantic) || hasQueensideClampWatch(side, semantic)

    val compensationContextPresent =
      semantic.positionFeatures.exists(features => hasCompensationMaterialDeficitFor(side, features))

    val typedAnchorPresent =
      realAnchorPresent || (!compensationContextPresent && planSupportPresent && boardPatternPresent)

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          typedAnchorPresent &&
            (plan.plan.id == PlanId.Prophylaxis || plan.plan.id == PlanId.DefensiveConsolidation)
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.Prophylaxis,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PlanMatchProphylaxis,
            confidence = 0.80 + math.min(0.06, plan.score * 0.08),
            focusFiles =
              Option.when(hasQueensideClampWatch(side, semantic))("b").toList ++
                Option.when(hasBishopPinWatch(side, semantic))("g").toList,
            factIds = List("plan_match_prophylaxis", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val bishopPinWatch =
      Option.when(typedAnchorPresent && hasBishopPinWatch(side, semantic)) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.Prophylaxis,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.BishopPinWatch,
          confidence = 0.84,
          focusSquares = if side == "white" then List("g4") else List("g5"),
          focusZone = Some("kingside"),
          factIds = List("bishop_pin_watch")
        )
      }.toList

    val queensideClampWatch =
      Option.when(typedAnchorPresent && hasQueensideClampWatch(side, semantic)) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.Prophylaxis,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.QueensideCounterbreakWatch,
          confidence = 0.90,
          focusFiles = List("b"),
          focusZone = Some("queenside"),
          factIds = List("queenside_counterbreak_watch")
        )
      }.toList

    preventedEvidence ++ threatBridge ++ counterBreakWatch ++ planBridge ++ bishopPinWatch ++ queensideClampWatch
