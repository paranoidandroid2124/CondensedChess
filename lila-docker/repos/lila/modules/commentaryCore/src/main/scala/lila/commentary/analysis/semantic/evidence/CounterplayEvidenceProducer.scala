package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceProducer }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import _root_.chess.{ Color, File, Square }
import lila.commentary.model.{ PlanId }
import lila.commentary.model.structure.{ StructureId }


private[commentary] object CounterplayEvidenceProducer extends StrategicIdeaEvidenceProducer:

  import StrategicIdeaEvidenceSupport.*

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val side = pack.sideToMove
    val preventedEvidence =
      semantic.preventedPlans.flatMap { prevented =>
        Option.when(isCounterplaySuppression(prevented)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.CounterplaySuppression,
            confidence = 0.82 + counterplaySuppressionBonus(prevented),
            focusSquares = prevented.deniedSquares.map(_.key).take(3),
            focusFiles = prevented.breakNeutralized.toList.flatMap(normalizeFileToken),
            factIds =
              List("counterplay_suppression") ++
                Option.when(prevented.breakNeutralized.isDefined)("break_neutralized").toList ++
                Option.when(prevented.counterplayScoreDrop >= 100)("counterplay_score_drop").toList ++
                Option.when(prevented.deniedResourceClass.contains("break"))("denied_break_resource").toList
          )
        }
      }

    val counterBreakBridge =
      semantic.opponentPawnAnalysis.toList.flatMap { analysis =>
        val file = analysis.breakFile.flatMap(normalizeFileToken)
        Option.when(analysis.counterBreak && semantic.preventedPlans.exists(preventsCounterBreak(_, analysis))) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.OpponentCounterbreakDenial,
            confidence = 0.80 + Option.when(analysis.pawnBreakReady)(0.04).getOrElse(0.0),
            focusSquares = semantic.preventedPlans.flatMap(_.deniedSquares.map(_.key)).distinct.take(3),
            focusFiles = file.toList,
            focusZone = file.flatMap(zoneFromFileToken),
            factIds = List("opponent_counterbreak_denial", "opponent_counter_break")
          )
        }
      }

    val threatBridge =
      semantic.threatsToUs.toList.flatMap { threats =>
        Option.when(isThreatDrivenCounterplaySuppression(threats, semantic.opponentPawnAnalysis, semantic.preventedPlans)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.ThreatAnalysisCounterplay,
            confidence = 0.78 + threatSuppressionBonus(threats),
            focusSquares = threatSquares(threats),
            focusZone = threatFocusZone(threats),
            factIds =
              List("threat_analysis_counterplay") ++
                Option.when(threats.strategicThreat)("strategic_threat").toList ++
                Option.when(threats.maxLossIfIgnored >= 180)("high_counterplay_cost").toList
          )
        }
      }

    val structureBridge =
      List(
        Option.when(structureIs(semantic, StructureId.Hedgehog) && side == "white") {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.HedgehogContainmentProfile,
            confidence = 0.88,
            focusZone = Some("queenside"),
            factIds = List("structure_hedgehog", "hedgehog_containment_profile")
          )
        },
        Option.when(
          structureIs(semantic, StructureId.Hedgehog) &&
            side == "white" &&
            semantic.board.exists(board =>
              pawnAt(board, Color.White, Square.C4) &&
                pawnAt(board, Color.Black, Square.A6) &&
                pawnAt(board, Color.Black, Square.B6) &&
                pawnAt(board, Color.Black, Square.D6)
            )
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.HedgehogBreakDenialGeometry,
            confidence = 0.92,
            focusFiles = List("b", "d"),
            focusZone = Some("queenside"),
            factIds = List("structure_hedgehog", "hedgehog_break_denial_geometry")
          )
        },
        Option.when(
          structureIs(semantic, StructureId.MaroczyBind) &&
            side == "white" &&
            (clampForSide(side, semantic) || mobilityClampForSide(side, semantic))
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.MaroczyCounterplaySuppression,
            confidence = 0.82,
            focusZone = Some("center"),
            factIds = List("structure_maroczy_bind", "maroczy_counterplay_suppression")
          )
        },
        Option.when(
          structureIs(semantic, StructureId.MaroczyBind) &&
            side == "white" &&
            semantic.board.exists(board =>
              pawnAt(board, Color.White, Square.C4) &&
                pawnAt(board, Color.White, Square.E4) &&
                pawnAt(board, Color.Black, Square.C6) &&
                pawnAt(board, Color.Black, Square.D6)
            )
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.MaroczyBreakDenialGeometry,
            confidence = 0.88,
            focusFiles = List("c", "d"),
            focusZone = Some("center"),
            factIds = List("structure_maroczy_bind", "maroczy_break_denial_geometry")
          )
        }
      ).flatten

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(
          structureIs(semantic, StructureId.Hedgehog) &&
            (plan.plan.id == PlanId.Prophylaxis || plan.plan.id == PlanId.SpaceAdvantage)
        ) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.CounterplaySuppression,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PlanMatchCounterplaySuppression,
            confidence = 0.80 + math.min(0.04, plan.score * 0.06),
            factIds = List("plan_match_counterplay_suppression", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val compensationCounterplayDenial =
      semantic.positionFeatures
        .flatMap { features =>
          val neutralizedBreak = semantic.preventedPlans.flatMap(_.breakNeutralized.toList).flatMap(normalizeFileToken).distinct
          val deniedSquares = semantic.preventedPlans.flatMap(_.deniedSquares.map(_.key)).distinct.take(3)
          val passiveDefender =
            semantic.preventedPlans.exists(plan =>
              isCounterplaySuppression(plan) || isPreventiveWithoutCounterplaySuppression(plan)
            ) ||
              semantic.opponentPawnAnalysis.exists(analysis =>
                analysis.counterBreak && semantic.preventedPlans.exists(preventsCounterBreak(_, analysis))
              )
          Option.when(
            hasCompensationMaterialDeficitFor(side, features) &&
              isCompensationEligiblePhase(semantic) &&
              passiveDefender &&
              (neutralizedBreak.nonEmpty || deniedSquares.nonEmpty)
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.CounterplaySuppression,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.CompensationCounterplayDenial,
              confidence = 0.78,
              focusSquares = deniedSquares,
              focusFiles = neutralizedBreak,
              focusZone = neutralizedBreak.headOption.flatMap(zoneFromFileToken),
              factIds =
                List("material_deficit_compensation", "compensation_counterplay_denial") ++
                  Option.when(neutralizedBreak.nonEmpty)("break_neutralized").toList
            )
          }
        }
        .toList

    preventedEvidence ++ counterBreakBridge ++ threatBridge ++ structureBridge ++ planBridge ++ compensationCounterplayDenial
