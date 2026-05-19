package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceProducer }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import _root_.chess.{ File, Queen, Rank, Rook, Square }
import lila.commentary.model.{ PlanId }
import lila.commentary.model.strategic.{ PositionalTag }


private[commentary] object LineOccupationEvidenceProducer extends StrategicIdeaEvidenceProducer:

  import StrategicIdeaEvidenceSupport.*

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val side = pack.sideToMove
    collectLineOccupationEvidence(side, pack, semantic)

  private def collectLineOccupationEvidence(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val openFileEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.OpenFile(file, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.OpenFileControl,
            confidence = 0.80,
            focusFiles = List(fileToken(file)),
            beneficiaryPieces = List("R", "Q"),
            factIds = List(s"open_file_${fileToken(file)}")
          )
      }

    val doubledRooks =
      semantic.positionalFeatures.collect {
        case PositionalTag.DoubledRooks(file, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.DoubledRooks,
            confidence = 0.74,
            focusFiles = List(fileToken(file)),
            beneficiaryPieces = List("R"),
            factIds = List(s"doubled_rooks_${fileToken(file)}")
          )
      }

    val connectedRooks =
      semantic.positionalFeatures.collect {
        case PositionalTag.ConnectedRooks(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.ConnectedRooks,
            confidence = 0.64,
            beneficiaryPieces = List("R"),
            factIds = List("connected_rooks")
          )
      }

    val rookOnSeventh =
      semantic.positionalFeatures.collect {
        case PositionalTag.RookOnSeventh(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.RookOnSeventh,
            confidence = 0.72,
            focusZone = Some("back rank"),
            beneficiaryPieces = List("R"),
            factIds = List("rook_on_seventh")
          )
      }

    val occupiedLineEvidence =
      semantic.board.toList.flatMap { board =>
        val color = sideColor(side)
        val occupiedSquares =
          board.byPiece(color, Rook).map(_ -> "R").toList ++
            board.byPiece(color, Queen).map(_ -> "Q").toList

        occupiedSquares.flatMap { case (square, piece) =>
          val open = isOpenFile(board, square.file)
          val semiOpen = isSemiOpenFileFor(board, square.file, color)
          val seventh = isSeventhRankFor(side, square)
          Option.when(open || semiOpen || seventh) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.LineOccupation,
              readiness = StrategicIdeaReadiness.Ready,
              source = EvidenceSourceId.OccupiedLineControl,
              confidence =
                (if piece == "R" then 0.78 else 0.72) +
                  (if open then 0.03 else if semiOpen then 0.01 else 0.0) +
                  (if seventh then 0.02 else 0.0),
              focusSquares = List(square.key),
              focusFiles = Option.when(open || semiOpen)(List(fileToken(square.file))).getOrElse(Nil),
              focusZone = if seventh then Some("back rank") else zoneFromFileToken(fileToken(square.file)),
              beneficiaryPieces = List(piece),
              factIds =
                List(
                  Some("occupied_line_control"),
                  Some(s"occupied_${piece.toLowerCase}_${square.key}"),
                  Option.when(open)(s"open_file_${fileToken(square.file)}"),
                  Option.when(semiOpen)(s"semi_open_file_${fileToken(square.file)}"),
                  Option.when(seventh)("occupied_seventh_rank")
                ).flatten
            )
          }
        }
      }

    val routeEvidence =
      pack.pieceRoutes
        .filter(route => route.ownerSide == side && route.surfaceMode != RouteSurfaceMode.Hidden && isMajorPiece(route.piece))
        .flatMap { route =>
          route.route.lastOption.flatMap(squareFromKey).flatMap { endpoint =>
            lineAccessFacts(side, endpoint, semantic).map { case (focusFiles, focusZone, factIds) =>
              evidence(
                ownerSide = side,
                kind = StrategicIdeaKind.LineOccupation,
                readiness = ideaReadinessFromRoute(route.surfaceMode),
                source = EvidenceSourceId.RouteLineAccess,
                confidence =
                  (
                    route.surfaceMode match
                      case RouteSurfaceMode.Exact  => 0.60
                      case RouteSurfaceMode.Toward => 0.48
                      case _                       => 0.44
                  ) +
                    route.surfaceConfidence * 0.08 +
                    Option.when(route.piece == "R" && focusFiles.nonEmpty)(0.02).getOrElse(0.0),
                focusSquares = List(endpoint.key),
                focusFiles = focusFiles,
                focusZone = focusZone,
                beneficiaryPieces = List(route.piece),
                factIds = factIds ++ List(s"route_surface_${route.surfaceMode.toLowerCase}")
              )
            }
          }
        }

    val directionalEvidence =
      pack.directionalTargets
        .filter(target => target.ownerSide == side && isMajorPiece(target.piece))
        .flatMap { target =>
          squareFromKey(target.targetSquare).flatMap { endpoint =>
            lineAccessFacts(side, endpoint, semantic).map { case (focusFiles, focusZone, factIds) =>
              evidence(
                ownerSide = side,
                kind = StrategicIdeaKind.LineOccupation,
                readiness = ideaReadinessFromDirectionalTarget(target.readiness),
                source = EvidenceSourceId.DirectionalLineAccess,
                confidence =
                  0.46 +
                    readinessBonus(target.readiness) * 0.6 +
                    Option.when(target.piece == "R" && focusFiles.nonEmpty)(0.02).getOrElse(0.0),
                focusSquares = List(endpoint.key),
                focusFiles = focusFiles,
                focusZone = focusZone,
                beneficiaryPieces = List(target.piece),
                factIds = factIds ++ List("directional_line_access")
              )
            }
          }
        }

    val featureSupport =
      semantic.positionFeatures
        .flatMap { features =>
          val hasMajorAccess =
            pack.pieceRoutes.exists(route =>
              route.ownerSide == side &&
                route.surfaceMode != RouteSurfaceMode.Hidden &&
                isMajorPiece(route.piece)
            ) ||
              pack.directionalTargets.exists(target =>
                target.ownerSide == side &&
                  isMajorPiece(target.piece)
              )

          Option.when(hasMajorAccess && (semiOpenFilesFor(side, features) > 0 || openFilesCount(features) > 0)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.LineOccupation,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.LineControlFeatures,
              confidence = 0.56,
              beneficiaryPieces = List("R", "Q"),
              factIds = List("line_control_features")
            )
          }
        }
        .toList

    val compensationOpenLines =
      semantic.positionFeatures
        .flatMap { features =>
          val lineCount = semiOpenFilesFor(side, features) + openFilesCount(features)
          val developmentLead = developmentLeadFor(side, features)
          Option.when(
            hasCompensationMaterialDeficitFor(side, features) &&
              isCompensationEligiblePhase(semantic) &&
              lineCount > 0 &&
              hasCompensationLinePlanSupport(side, semantic) &&
              hasCompensationLineAccess(side, pack, semantic)
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.LineOccupation,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.CompensationOpenLines,
              confidence = 0.70 + math.min(0.08, lineCount * 0.02) + Option.when(developmentLead >= 2)(0.04).getOrElse(0.0),
              beneficiaryPieces = List("R", "Q"),
              factIds =
                List("compensation_open_lines", "material_deficit_compensation") ++
                  Option.when(developmentLead >= 2)("development_lead_compensation").toList
            )
          }
        }
        .toList

    val delayedRecoveryWindow =
      semantic.positionFeatures
        .flatMap { features =>
          Option.when(
            hasCompensationMaterialDeficitFor(side, features) &&
              isCompensationEligiblePhase(semantic) &&
              developmentLeadFor(side, features) >= 2 &&
              hasDelayedRecoveryCompensationPlan(side, semantic) &&
              hasCompensationLineAccess(side, pack, semantic)
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.LineOccupation,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.DelayedRecoveryWindow,
              confidence = 0.74,
              beneficiaryPieces = List("R", "Q"),
              factIds = List("delayed_material_recovery", "development_lead_compensation", "material_deficit_compensation")
            )
          }
        }
        .toList

    val planBridge =
      topPlansFor(side, semantic).flatMap { plan =>
        Option.when(plan.plan.id == PlanId.FileControl || plan.plan.id == PlanId.RookActivation) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.LineOccupation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PlanMatchLineOccupation,
            confidence = 0.72 + math.min(0.04, plan.score * 0.06),
            beneficiaryPieces = List("R", "Q"),
            factIds = List("plan_match_line_occupation", s"plan_${plan.plan.id.toString.toLowerCase}")
          )
        }
      }

    openFileEvidence ++ doubledRooks ++ connectedRooks ++ rookOnSeventh ++ occupiedLineEvidence ++ routeEvidence ++
      directionalEvidence ++ featureSupport ++ compensationOpenLines ++ delayedRecoveryWindow ++ planBridge
