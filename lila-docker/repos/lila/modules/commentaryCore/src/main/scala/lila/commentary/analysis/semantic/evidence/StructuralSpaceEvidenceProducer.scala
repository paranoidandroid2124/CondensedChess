package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.analysis.PlanMoveEvidenceSupport.pawnAt
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceProducer, StrategicIdeaEvidenceTier }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import _root_.chess.{ Color, Square }
import lila.commentary.model.{ PlanId }
import lila.commentary.model.strategic.{ PositionalTag }
import lila.commentary.model.structure.{ CenterState, StructureId }


private[commentary] object StructuralSpaceEvidenceProducer extends StrategicIdeaEvidenceProducer:

  import StrategicIdeaEvidenceSupport.*

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val side = pack.sideToMove
    collectSpaceEvidence(side, semantic)

  private def collectSpaceEvidence(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val enemyColorComplexWeakness =
      semantic.positionalFeatures
        .collect {
          case PositionalTag.ColorComplexWeakness(owner, squareColor, squares) if !matchesSide(owner, side) =>
            (
              colorComplexToken(squareColor),
              squares.map(_.key).distinct.take(3)
            )
        }
        .sortBy { case (squareColor, squares) => (-squares.size, squareColor.getOrElse("")) }
        .headOption

    val tagEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.SpaceAdvantage(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.SpaceGainOrRestriction,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.SpaceAdvantageTag,
            confidence = 0.84,
            focusZone = Some("center"),
            factIds = List("tag_space_advantage")
          )
      }

    val clampEvidence =
      semantic.strategicState
        .filter(colorComplexClampFor(side, _))
        .map { _ =>
          val complexToken = enemyColorComplexWeakness.flatMap(_._1)
          val weakSquares = enemyColorComplexWeakness.map(_._2).getOrElse(Nil)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.SpaceGainOrRestriction,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.ColorComplexClamp,
            confidence = 0.80,
            tier =
              if weakSquares.nonEmpty && complexToken.nonEmpty then StrategicIdeaEvidenceTier.ValidatedPressure
              else StrategicIdeaEvidenceTier.SelectorSupport,
            focusSquares = weakSquares,
            focusZone = complexToken.map(token => s"$token-square complex").orElse(Some("center")),
            factIds =
              List("state_color_complex_clamp") ++
                Option.when(weakSquares.nonEmpty)("enemy_color_complex_weakness").toList ++
                complexToken.map(token => s"color_complex_$token").toList
          )
        }
        .toList

    val centralSpaceEvidence =
      semantic.positionFeatures
        .flatMap { features =>
          Option.when(spaceDiffFor(side, features) >= 2) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.SpaceGainOrRestriction,
              readiness = StrategicIdeaReadiness.Ready,
              source = EvidenceSourceId.CentralSpaceEdge,
              confidence = 0.74 + math.min(0.08, (spaceDiffFor(side, features) - 2) * 0.02),
              focusZone = Some("center"),
              factIds = List("central_space_edge")
            )
          }
        }
        .toList

    val mobilityRestriction =
      semantic.positionFeatures
        .flatMap { features =>
          val enemyLow = lowMobilityPiecesFor(opponentSide(side), features)
          val ours = lowMobilityPiecesFor(side, features)
          Option.when(enemyLow > ours) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.SpaceGainOrRestriction,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.MobilityRestriction,
              confidence = 0.68 + math.min(0.06, (enemyLow - ours) * 0.02),
              focusZone = Some("center"),
              factIds = List("mobility_restriction")
            )
          }
        }
        .toList

    val lockedCenterBind =
      semantic.structureProfile.toList.flatMap { profile =>
        semantic.positionFeatures.flatMap { features =>
          Option.when(
            profile.centerState == CenterState.Locked &&
              (spaceDiffFor(side, features) > 0 || semantic.strategicState.exists(colorComplexClampFor(side, _)))
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.SpaceGainOrRestriction,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.LockedCenterBind,
              confidence = 0.70,
              focusZone = Some("center"),
              factIds = List("structure_locked_center")
            )
          }
        }
      }

    val alignmentSpaceRace =
      Option.when(semantic.planAlignmentReasonCodes.contains("SPACE_RACE")) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.PlanAlignmentSpaceRace,
          confidence = 0.68,
          focusZone = Some("center"),
          factIds = List("alignment_space_race")
        )
      }.toList

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(plan.plan.id == PlanId.SpaceAdvantage || plan.plan.id == PlanId.CentralControl) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.SpaceGainOrRestriction,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PlanMatchSpaceAdvantage,
            confidence = 0.78 + math.min(0.06, plan.score * 0.08),
            focusZone = Some("center"),
            factIds = List("plan_match_space_advantage", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    val maroczyProfile =
      Option.when(structureIs(semantic, StructureId.MaroczyBind) && side == "white") {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          readiness = StrategicIdeaReadiness.Ready,
          source = EvidenceSourceId.MaroczyBindProfile,
          confidence = 0.86,
          focusZone = Some("center"),
          factIds = List("structure_maroczy_bind")
        )
      }.toList

    val iqpSpaceBridge =
      Option.when(structureIs(semantic, StructureId.IQPWhite) && side == "white") {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.IqpSpaceBridge,
          confidence = 0.84,
          focusZone = Some("center"),
          factIds = List("structure_iqp_white")
        )
      }.toList

    val iqpCentralPresence =
      Option.when(
        structureIs(semantic, StructureId.IQPWhite) &&
          side == "white" &&
          semantic.board.exists(board => pawnAt(board, Color.White, Square.D4))
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.IqpCentralPresence,
          confidence = 0.82,
          focusSquares = List("d4"),
          focusZone = Some("center"),
          factIds = List("structure_iqp_white", "iqp_central_presence")
        )
      }.toList

    tagEvidence ++ clampEvidence ++ centralSpaceEvidence ++ mobilityRestriction ++ lockedCenterBind ++ alignmentSpaceRace ++
      planBridge ++ maroczyProfile ++ iqpSpaceBridge ++ iqpCentralPresence

  private def colorComplexToken(squareColor: String): Option[String] =
    val normalized = squareColor.trim.toLowerCase
    if normalized.contains("dark") then Some("dark")
    else if normalized.contains("light") then Some("light")
    else Option.when(normalized.nonEmpty)(normalized)
