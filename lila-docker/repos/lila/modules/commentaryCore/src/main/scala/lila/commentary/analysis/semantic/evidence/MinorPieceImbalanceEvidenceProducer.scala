package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.StrategicIdeaSemanticContext
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceProducer }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import _root_.chess.Square
import lila.commentary.model.strategic.{ PositionalTag }
import lila.commentary.model.structure.{ StructureId }


private[commentary] object MinorPieceImbalanceEvidenceProducer extends StrategicIdeaEvidenceProducer:

  import StrategicIdeaEvidenceSupport.*

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val side = pack.sideToMove
    collectMinorPieceImbalanceEvidence(side, semantic)

  private def collectMinorPieceImbalanceEvidence(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val bishopPair =
      semantic.positionalFeatures.collect {
        case PositionalTag.BishopPairAdvantage(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
            readiness = StrategicIdeaReadiness.Ready,
            source = EvidenceSourceId.BishopPairAdvantage,
            confidence = 0.82,
            beneficiaryPieces = List("B"),
            factIds = List("bishop_pair_advantage")
          )
      }

    val enemyBadBishop =
      semantic.positionalFeatures.collect {
        case PositionalTag.BadBishop(color) if !matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.EnemyBadBishop,
            confidence = 0.80,
            beneficiaryPieces = List("N", "B"),
            factIds = List("enemy_bad_bishop")
          )
      }

    val goodBishop =
      semantic.positionalFeatures.collect {
        case PositionalTag.GoodBishop(color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.GoodBishop,
            confidence = 0.74,
            beneficiaryPieces = List("B"),
            factIds = List("good_bishop")
          )
      }

    val oppositeColorBishops =
      semantic.positionalFeatures.collect {
        case PositionalTag.OppositeColorBishops =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.OppositeColorBishops,
            confidence = 0.68,
            beneficiaryPieces = List("B"),
            factIds = List("opposite_color_bishops")
          )
      }

    val strongKnightBridge =
      for
        square <- semantic.positionalFeatures.collect {
          case PositionalTag.StrongKnight(sq, color) if matchesSide(color, side) => sq.key
        }
        if enemyBadBishop.nonEmpty
      yield
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
          readiness = StrategicIdeaReadiness.Ready,
          source = EvidenceSourceId.StrongKnightVsBadBishop,
          confidence = 0.78,
          focusSquares = List(square),
          beneficiaryPieces = List("N"),
          factIds = List("strong_knight_vs_bad_bishop", s"strong_knight_$square")
        )

    val activityBridge =
      semantic.board.toList.flatMap { board =>
        semantic.pieceActivity.flatMap { activity =>
          Option.when(activity.isBadBishop && board.colorAt(activity.square).exists(color => !matchesSide(color, side))) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.PieceActivityBadBishop,
              confidence = 0.74,
              focusSquares = List(activity.square.key),
              beneficiaryPieces = List("N", "B"),
              factIds = List("piece_activity_bad_bishop", s"enemy_bad_bishop_${activity.square.key}")
            )
          }
        }
      }

    val countBasedImbalance =
      semantic.positionFeatures
        .flatMap { features =>
          val bishopEdge = bishopCountFor(side, features) - bishopCountFor(opponentSide(side), features)
          val knightEdge = knightCountFor(side, features) - knightCountFor(opponentSide(side), features)
          val facts =
            List(
              Option.when(bishopPairFor(side, features))("bishop_pair_count_edge"),
              Option.when(goodBishop.nonEmpty && bishopEdge > 0)("good_bishop_count_edge"),
              Option.when(enemyBadBishop.nonEmpty && knightEdge >= 0 && strongKnightBridge.nonEmpty)("knight_vs_bishop_count_edge")
            ).flatten
          Option.when(facts.nonEmpty) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.MinorPieceCountImbalance,
              confidence = 0.72 + math.min(0.06, facts.size * 0.02),
              beneficiaryPieces =
                List(
                  Option.when(bishopEdge > 0 || bishopPairFor(side, features))("B"),
                  Option.when(knightEdge >= 0 && enemyBadBishop.nonEmpty)("N")
                ).flatten,
              factIds = List("minor_piece_count_imbalance") ++ facts
            )
          }
        }
        .toList

    val frenchProfileBridge =
      Option.when(
        structureIs(semantic, StructureId.FrenchAdvanceChain) &&
          side == "white" &&
          (enemyBadBishop.nonEmpty || goodBishop.nonEmpty || strongKnightBridge.nonEmpty)
      ) {
        evidence(
          ownerSide = side,
          kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
          readiness = StrategicIdeaReadiness.Build,
          source = EvidenceSourceId.FrenchMinorPieceProfile,
          confidence = 0.80,
          beneficiaryPieces = List("N", "B"),
          factIds = List("structure_french_advance_chain", "french_minor_piece_profile")
        )
      }.toList

    bishopPair ++ enemyBadBishop ++ goodBishop ++ oppositeColorBishops ++ strongKnightBridge ++ activityBridge ++
      countBasedImbalance ++ frenchProfileBridge
