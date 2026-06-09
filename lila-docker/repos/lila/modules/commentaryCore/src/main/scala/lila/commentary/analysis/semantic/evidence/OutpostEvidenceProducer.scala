package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.{ StrategicIdeaSemanticContext }
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceProducer }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import _root_.chess.Square
import lila.commentary.model.Motif
import lila.commentary.model.strategic.{ PositionalTag }


private[commentary] object OutpostEvidenceProducer extends StrategicIdeaEvidenceProducer:

  import StrategicIdeaEvidenceSupport.*

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val side = pack.sideToMove
    collectOutpostEvidence(side, pack, semantic)

  private def collectOutpostEvidence(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val taggedOutpostSquares = taggedOutpostSquaresFor(side, semantic)
    val occupiedStrongKnightSquares = occupiedStrongKnightSquaresFor(side, semantic)
    val stablePlanEvidence = hasStablePlanEvidence(semantic, StrategicIdeaKind.OutpostCreationOrOccupation)

    val tagEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.Outpost(square, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.OutpostCreationOrOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.OutpostTag,
            confidence = 0.84,
            focusSquares = List(square.key),
            beneficiaryPieces = List("N", "B"),
            factIds = List(s"outpost_${square.key}")
          )
      }

    val motifOutpostEvidence =
      semantic.motifs.collect {
        case Motif.Outpost(piece, square, color, _, _) if matchesSide(color, side) && isMinorPiece(roleToken(piece)) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.OutpostCreationOrOccupation,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.OutpostTag,
            confidence = 0.84,
            focusSquares = List(square.key),
            beneficiaryPieces = List(roleToken(piece)),
            factIds = List(s"outpost_${square.key}")
          )
      }

    val strongKnightEvidence =
      semantic.positionalFeatures.collect {
        case PositionalTag.StrongKnight(square, color) if matchesSide(color, side) =>
          val occupiedAnchor = occupiedStrongKnightSquares.contains(square.key)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.OutpostCreationOrOccupation,
            readiness = if occupiedAnchor then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.StrongKnight,
            confidence = if occupiedAnchor then 0.76 else 0.68,
            focusSquares = List(square.key),
            beneficiaryPieces = List("N"),
            factIds = List(s"strong_knight_${square.key}")
          )
      }

    val entrenchedSupport =
      semantic.strategicState
        .filter(state =>
          entrenchedPiecesFor(side, state) > 0 &&
            (taggedOutpostSquares.nonEmpty || occupiedStrongKnightSquares.nonEmpty || stablePlanEvidence)
        )
        .map { state =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.OutpostCreationOrOccupation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.EntrenchedPieceState,
            confidence = 0.68 + math.min(0.08, entrenchedPiecesFor(side, state) * 0.02),
            beneficiaryPieces = List("N", "B"),
            factIds = List("entrenched_piece_state")
          )
        }
        .toList

    val routeEvidence =
      pack.pieceRoutes
        .filter(route => route.ownerSide == side && route.surfaceMode != RouteSurfaceMode.Hidden && isMinorPiece(route.piece))
        .flatMap { route =>
          route.route.lastOption
            .flatMap(squareFromKey)
            .filter(endpoint => taggedOutpostSquares.contains(endpoint.key))
            .map { endpoint =>
              evidence(
                ownerSide = side,
                kind = StrategicIdeaKind.OutpostCreationOrOccupation,
                readiness = ideaReadinessFromRoute(route.surfaceMode),
                source = EvidenceSourceId.RouteOutpostAccess,
                confidence = 0.60 + route.surfaceConfidence * 0.08,
                focusSquares = List(endpoint.key),
                beneficiaryPieces = List(route.piece),
                factIds = List("route_outpost_access_shape", s"route_surface_${route.surfaceMode.toLowerCase}")
              )
            }
        }

    val directionalEvidence =
      pack.directionalTargets
        .filter(target => target.ownerSide == side && isMinorPiece(target.piece))
        .flatMap { target =>
          squareFromKey(target.targetSquare)
            .filter(endpoint => taggedOutpostSquares.contains(endpoint.key))
            .map { endpoint =>
              evidence(
                ownerSide = side,
                kind = StrategicIdeaKind.OutpostCreationOrOccupation,
                readiness = ideaReadinessFromDirectionalTarget(target.readiness),
                source = EvidenceSourceId.DirectionalOutpostAccess,
                confidence = 0.60 + readinessBonus(target.readiness),
                focusSquares = List(endpoint.key),
                beneficiaryPieces = List(target.piece),
                factIds = List("directional_outpost_access_shape")
              )
            }
        }

    tagEvidence ++ motifOutpostEvidence ++ strongKnightEvidence ++ entrenchedSupport ++ routeEvidence ++ directionalEvidence
