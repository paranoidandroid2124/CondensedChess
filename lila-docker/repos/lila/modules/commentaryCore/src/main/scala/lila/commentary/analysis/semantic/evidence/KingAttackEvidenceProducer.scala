package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceProducer }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import _root_.chess.File
import lila.commentary.analysis.L3.{ ThreatKind }
import lila.commentary.model.{ Motif, PlanId }
import lila.commentary.model.strategic.{ PositionalTag }
import lila.commentary.model.structure.{ StructureId }


private[commentary] object KingAttackEvidenceProducer extends StrategicIdeaEvidenceProducer:

  import StrategicIdeaEvidenceSupport.*

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val side = pack.sideToMove
    val enemyKingZone = semantic.board.flatMap(board => board.kingPosOf(sideColor(opponentSide(side)))).flatMap(zoneFromSquare)

    val mateNet =
      semantic.positionalFeatures.collect {
        case PositionalTag.MateNet(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.MateNet,
            confidence = 0.88,
            focusZone = enemyKingZone,
            factIds = List("mate_net")
          )
      }

    val stuckCenter =
      semantic.positionalFeatures.collect {
        case PositionalTag.KingStuckCenter(color) if !matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.EnemyKingStuckCenter,
            confidence = 0.80,
            focusZone = enemyKingZone.orElse(Some("center")),
            factIds = List("enemy_king_central_exposure")
          )
      }

    val weakBackRank =
      semantic.positionalFeatures.collect {
        case PositionalTag.WeakBackRank(color) if !matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.EnemyWeakBackRank,
            confidence = 0.74,
            focusZone = enemyKingZone,
            factIds = List("enemy_weak_back_rank_shape")
          )
      }

    val weakBackRankMotifEvidence =
      semantic.motifs.collect {
        case Motif.WeakBackRank(color, _, _) if !matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.EnemyWeakBackRank,
            confidence = 0.74,
            focusZone = enemyKingZone,
            factIds = List("enemy_weak_back_rank_shape")
          )
      }

    val kingRingPressure =
      semantic.positionFeatures
        .flatMap { features =>
          val attackers = attackersCountFor(side, features)
          val ring = enemyKingRingAttackedFor(side, features)
          val exposed = enemyKingExposedFilesFor(side, features)
          Option.when(attackers >= 2 && (ring >= 2 || exposed > 0)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.KingRingPressure,
              confidence = 0.78 + math.min(0.08, ring * 0.02),
              focusZone = enemyKingZone,
              factIds = List("king_ring_pressure_shape") ++ Option.when(exposed > 0)("king_exposed_files").toList
            )
          }
        }
        .toList

    val flankPawns =
      semantic.strategicState.toList.flatMap { state =>
        val facts =
          List(
            Option.when(hookCreationChanceFor(side, state))("hook_creation_chance"),
            Option.when(rookPawnMarchReadyFor(side, state))("rook_pawn_march_ready")
          ).flatten
        Option.when(facts.nonEmpty) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.FlankPawnPressure,
            confidence = 0.74 + (facts.size * 0.03),
            focusZone = enemyKingZone,
            factIds = facts
          )
        }
      }

    val attackingThreats =
      semantic.threatsToThem.toList.flatMap { threats =>
        Option.when(isKingAttackThreatProfile(threats, side, semantic)) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = if threats.immediateThreat then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.AttackingThreatAnalysis,
            confidence = 0.78 + Option.when(threats.primaryDriver == "mate_threat")(0.06).getOrElse(0.0),
            focusSquares = threatSquares(threats),
            focusZone = enemyKingZone.orElse(threatFocusZone(threats)),
            factIds =
              List("attacking_threat_analysis") ++
                Option.when(threats.primaryDriver == "mate_threat")("mate_threat").toList ++
                Option.when(threats.threats.exists(_.kind == ThreatKind.Mate))("mate_threat_kind").toList
          )
        }
      }

    val motifPressure =
      semantic.motifs.flatMap {
        case Motif.RookLift(file, _, _, color, _, _) if matchesSide(color, side) =>
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.MotifRookLift,
              confidence = 0.78,
              focusFiles = List(fileToken(file)),
              focusZone = enemyKingZone.orElse(zoneFromFileToken(fileToken(file))),
              beneficiaryPieces = List("R"),
              factIds = List("motif_rook_lift")
            )
          )
        case Motif.Battery(front, back, axis, color, _, _, frontSq, backSq)
            if matchesSide(color, side) &&
              (axis == Motif.BatteryAxis.File || axis == Motif.BatteryAxis.Diagonal) =>
          val squares = (frontSq.toList ++ backSq.toList).distinct
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.MotifBattery,
              confidence = 0.74,
              focusSquares = squares.map(_.key).take(2),
              focusZone = enemyKingZone.orElse(zoneFromSquares(squares)),
              beneficiaryPieces = List(roleToken(front), roleToken(back)),
              factIds = List("motif_battery", s"battery_axis_${axis.toString.toLowerCase}")
            )
          )
        case Motif.PieceLift(piece, _, _, color, _, _) if matchesSide(color, side) =>
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.MotifPieceLift,
              confidence = 0.72,
              focusZone = enemyKingZone,
              beneficiaryPieces = List(roleToken(piece)),
              factIds = List("motif_piece_lift", "motif_piece_lift_shape")
            )
          )
        case Motif.Check(piece, targetSquare, checkType, color, _, _) if matchesSide(color, side) =>
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Ready,
              source = EvidenceSourceId.MotifCheckPressure,
              confidence = 0.68 + checkTypeBonus(checkType),
              focusSquares = List(targetSquare.key),
              focusZone = enemyKingZone.orElse(zoneFromSquare(targetSquare)),
              beneficiaryPieces = List(roleToken(piece)),
              factIds = List("motif_check_pressure", s"check_type_${checkType.toString.toLowerCase}")
            )
          )
        case Motif.Fianchetto(fianchettoSide, color, _, _) if matchesSide(color, side) =>
          val sideKey = fianchettoSide.toString.toLowerCase
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.FianchettoMotif,
              confidence = 0.70,
              focusZone = Some(if fianchettoSide == Motif.FianchettoSide.Kingside then "kingside" else "queenside"),
              beneficiaryPieces = List("B"),
              factIds = List("fianchetto_motif_shape", s"fianchetto_side_$sideKey")
            )
          )
        case Motif.Initiative(color, score, _, _) if matchesSide(color, side) && score >= 10 =>
          Some(
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.InitiativeMotif,
              confidence = 0.70 + math.min(0.04, (score - 10) * 0.005),
              focusZone = enemyKingZone,
              factIds = List("initiative_motif_shape", s"initiative_score_$score")
            )
          )
        case motif @ Motif.PawnAdvance(file, _, _, color, _, _) if matchesSide(color, side) =>
          val fileKey = fileToken(file)
          Option
            .when(Set("a", "b", "g", "h").contains(fileKey) && motif.relativeTo >= 4) {
              evidence(
                ownerSide = side,
                kind = StrategicIdeaKind.KingAttackBuildUp,
                readiness = StrategicIdeaReadiness.Build,
                source = EvidenceSourceId.FlankPawnAdvanceMotif,
                confidence = 0.70,
                focusFiles = List(fileKey),
                focusZone = zoneFromFileToken(fileKey),
                factIds =
                  List(
                    "flank_pawn_advance_shape",
                    s"flank_pawn_file_$fileKey",
                    s"flank_pawn_to_rank_${motif.relativeTo}"
                  )
              )
            }
        case _ => None
      }

    val routePressure =
      pack.pieceRoutes
        .filter(route => route.ownerSide == side && route.surfaceMode != RouteSurfaceMode.Hidden)
        .flatMap { route =>
          route.route.lastOption.flatMap(squareFromKey).filter(isNearEnemyKing(side, _, semantic)).map { endpoint =>
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = ideaReadinessFromRoute(route.surfaceMode),
              source = EvidenceSourceId.RouteAttackLane,
              confidence = 0.70 + route.surfaceConfidence * 0.10,
              focusSquares = List(endpoint.key),
              focusZone = enemyKingZone,
              beneficiaryPieces = List(route.piece),
              factIds = List("route_attack_lane_shape", s"route_surface_${route.surfaceMode.toLowerCase}")
            )
          }
        }

    val directionalPressure =
      pack.directionalTargets
        .filter(_.ownerSide == side)
        .flatMap { target =>
          squareFromKey(target.targetSquare).filter(isNearEnemyKing(side, _, semantic)).map { endpoint =>
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = ideaReadinessFromDirectionalTarget(target.readiness),
              source = EvidenceSourceId.DirectionalAttackLane,
              confidence = 0.68 + readinessBonus(target.readiness),
              focusSquares = List(endpoint.key),
              focusZone = enemyKingZone,
              beneficiaryPieces = List(target.piece),
              factIds = List("directional_attack_lane_shape")
            )
          }
        }

    val compensationDevelopmentLead =
      semantic.positionFeatures
        .flatMap { features =>
          val developmentLead = developmentLeadFor(side, features)
          val enemyWindow =
            enemyKingCastledSideFor(side, features) == "none" ||
              enemyKingExposedFilesFor(side, features) > 0
          Option.when(
            hasCompensationMaterialDeficitFor(side, features) &&
              isCompensationEligiblePhase(semantic) &&
              developmentLead >= 2 &&
              enemyWindow &&
              hasCompensationAttackPlanSupport(side, semantic)
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.CompensationDevelopmentLead,
              confidence = 0.76 + math.min(0.06, developmentLead * 0.02),
              focusZone = enemyKingZone,
              factIds = List("material_deficit_compensation", "development_lead_compensation")
            )
          }
        }
        .toList

    val compensationKingWindow =
      semantic.positionFeatures
        .flatMap { features =>
          val attackers = attackersCountFor(side, features)
          val ring = enemyKingRingAttackedFor(side, features)
          val exposed = enemyKingExposedFilesFor(side, features)
          Option.when(
            hasCompensationMaterialDeficitFor(side, features) &&
              isCompensationEligiblePhase(semantic) &&
              hasCompensationAttackPlanSupport(side, semantic) &&
              (
                enemyKingCastledSideFor(side, features) == "none" ||
                  exposed > 0
              ) &&
              (
                attackers >= 2 ||
                  ring >= 2 ||
                  hasAttackLaneTowardEnemyKing(side, pack, semantic)
              )
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.CompensationKingWindow,
              confidence = 0.74 + math.min(0.06, ring * 0.02) + Option.when(exposed > 0)(0.03).getOrElse(0.0),
              focusZone = enemyKingZone.orElse(Some("center")),
              factIds =
                List("material_deficit_compensation", "uncastled_or_unsettled_king_window") ++
                  Option.when(exposed > 0)("king_exposed_files").toList
            )
          }
        }
        .toList

    val compensationDiagonalBattery =
      semantic.positionFeatures
        .flatMap { features =>
          Option.when(
            hasCompensationMaterialDeficitFor(side, features) &&
              isCompensationEligiblePhase(semantic) &&
              hasDiagonalBatteryCompensation(side, semantic) &&
              (
                developmentLeadFor(side, features) >= 1 ||
                  hasCompensationAttackPlanSupport(side, semantic)
              ) &&
              (
                enemyKingCastledSideFor(side, features) == "none" ||
                  enemyKingExposedFilesFor(side, features) > 0 ||
                  enemyKingRingAttackedFor(side, features) >= 1
              )
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.KingAttackBuildUp,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.CompensationDiagonalBattery,
              confidence = 0.74 + Option.when(bishopPairFor(side, features))(0.04).getOrElse(0.0),
              focusZone = enemyKingZone,
              beneficiaryPieces = List("B", "Q"),
              factIds =
                List("compensation_diagonal_battery", "material_deficit_compensation") ++
                  Option.when(bishopPairFor(side, features))("bishop_pair_compensation").toList
            )
          }
        }
        .toList

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(plan.plan.id == PlanId.KingsideAttack || plan.plan.id == PlanId.PawnStorm) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.KingAttackBuildUp,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PlanMatchKingAttack,
            confidence = 0.82 + math.min(0.06, plan.score * 0.08),
            focusZone = enemyKingZone,
            factIds = List("plan_match_king_attack", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val oppositeSideStorm =
      Option.when(hasOppositeSideStormAttack(side, semantic)) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.KingAttackBuildUp,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.OppositeSideStorm,
          confidence = 0.84,
          focusZone = enemyKingZone,
          factIds = List("opposite_side_storm")
        )
      }.toList

    val fianchettoAssault =
      Option.when(
        structureIs(semantic, StructureId.FianchettoShell) &&
          hasOppositeSideStormAttack(side, semantic) &&
          semantic.board.exists(board =>
            board.kingPosOf(sideColor(side)).exists(king => king.file.value <= File.C.value || king.file.value >= File.F.value)
          )
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.KingAttackBuildUp,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.FianchettoAssaultProfile,
          confidence = 0.90,
          focusZone = enemyKingZone,
          factIds = List("structure_fianchetto_shell", "fianchetto_assault_profile", "opposite_side_storm")
        )
      }.toList

    mateNet ++ stuckCenter ++ weakBackRank ++ weakBackRankMotifEvidence ++ kingRingPressure ++ flankPawns ++ attackingThreats ++ motifPressure ++ routePressure ++ directionalPressure ++ compensationDevelopmentLead ++ compensationKingWindow ++ compensationDiagonalBattery ++ planBridge ++ oppositeSideStorm ++ fianchettoAssault
