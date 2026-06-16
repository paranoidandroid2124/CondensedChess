package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.{ PlanTaxonomy, StrategicIdeaSemanticContext }
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceProducer }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import lila.commentary.model.{ Motif, PlanId }
import lila.commentary.model.strategic.PositionalTag
import lila.commentary.model.structure.{ StructureId }


private[commentary] object TransformationEvidenceProducer extends StrategicIdeaEvidenceProducer:

  import StrategicIdeaEvidenceSupport.*

  def collect(
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): List[StrategicIdeaEvidence] =
    val side = pack.sideToMove
    val defenderTagExchangeSupport =
      semantic.positionalFeatures.collect {
        case PositionalTag.RemovingTheDefender(target, color) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.CaptureExchangeTransformation,
            confidence = 0.74,
            beneficiaryPieces = List(roleToken(target)),
            factIds = List("capture_or_exchange", s"removing_defender_tag_support_${target.name.toLowerCase}")
          )
      }

    val rookEndgamePattern =
      semantic.motifs.collect {
        case Motif.RookBehindPassedPawn(file, color, _, _) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.RookEndgamePattern,
            confidence = 0.72,
            focusFiles = List(fileToken(file)),
            focusZone = Some("endgame"),
            factIds = List("rook_endgame_pattern_shape", "rook_behind_passed_pawn")
          )
        case Motif.KingCutOff(_, _, color, _, _) if matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.RookEndgamePattern,
            confidence = 0.72,
            focusZone = Some("endgame"),
              factIds = List("rook_endgame_pattern_shape", "king_cut_off")
          )
      }

    val endgameTechniqueMotif =
      semantic.motifs.collect {
        case Motif.Opposition(opponentKingSquare, ownKingSquare, oppType, color, _, _) if matchesSide(color, side) =>
          val oppositionKey = oppType.toString.toLowerCase
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.EndgameTechniqueMotif,
            confidence = if oppType == Motif.OppositionType.Direct then 0.72 else 0.70,
            focusSquares = List(ownKingSquare.key, opponentKingSquare.key),
            focusZone = Some("endgame"),
            factIds = List("endgame_technique_shape", s"opposition_$oppositionKey")
          )
        case Motif.Zugzwang(color, _, _) if !matchesSide(color, side) =>
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.EndgameTechniqueMotif,
            confidence = 0.72,
            focusZone = Some("endgame"),
            factIds = List("endgame_technique_shape", "zugzwang_shape")
          )
        case Motif.KingStep(Motif.KingStepType.Activation, color, _, move) if matchesSide(color, side) && semantic.phase.equalsIgnoreCase("endgame") =>
          val focusSquares =
            move.flatMap(destinationSquareFromSan).map(_.key).toList
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.EndgameTechniqueMotif,
            confidence = 0.72,
            focusZone = Some("endgame"),
            focusSquares = focusSquares,
            factIds = List("endgame_technique_shape", "king_activity_shape") ++
              focusSquares.map(square => s"king_activity_square_$square")
          )
      }

    val passedPawnConversionMotif =
      semantic.motifs.collect {
        case Motif.PassedPawn(file, rank, color, isProtected, _, _)
            if matchesSide(color, side) && rank >= 1 && rank <= 8 =>
          val square = s"${fileToken(file)}$rank"
          val relativeRank = Motif.relativeRank(rank, color)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PassedPawnConversionMotif,
            confidence =
              0.72 +
                Option.when(isProtected)(0.04).getOrElse(0.0) +
                Option.when(relativeRank >= 6)(0.02).getOrElse(0.0),
            focusSquares = List(square),
            focusFiles = List(fileToken(file)),
            focusZone = zoneFromFileToken(fileToken(file)),
            factIds =
              List("passed_pawn_conversion_shape", s"passed_pawn_$square") ++
                Option.when(isProtected)("protected_passed_pawn").toList ++
                Option.when(relativeRank >= 6)("advanced_passed_pawn").toList
          )
        case Motif.PassedPawnPush(file, toRank, color, _, _) if matchesSide(color, side) && toRank >= 1 && toRank <= 8 =>
          val square = s"${fileToken(file)}$toRank"
          val relativeRank = Motif.relativeRank(toRank, color)
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PassedPawnConversionMotif,
            confidence = 0.72 + Option.when(relativeRank >= 6)(0.02).getOrElse(0.0),
            focusSquares = List(square),
            focusFiles = List(fileToken(file)),
            focusZone = zoneFromFileToken(fileToken(file)),
            factIds =
              List("passed_pawn_conversion_shape", s"passed_pawn_$square", "passed_pawn_push") ++
                Option.when(relativeRank >= 6)("advanced_passed_pawn").toList
          )
        case motif @ Motif.PawnPromotion(file, promotedTo, color, _, _) if matchesSide(color, side) =>
          val square = s"${fileToken(file)}${if color.white then 8 else 1}"
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PassedPawnConversionMotif,
            confidence = 0.76,
            focusSquares = List(square),
            focusFiles = List(fileToken(file)),
            focusZone = zoneFromFileToken(fileToken(file)),
            beneficiaryPieces = List(roleToken(promotedTo)),
            factIds =
              List(
                "passed_pawn_conversion_shape",
                s"passed_pawn_$square",
                "pawn_promotion",
                s"promotion_piece_${roleToken(promotedTo).toLowerCase}"
              ) ++ Option.when(motif.isUnderpromotion)("underpromotion").toList
          )
      }

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
        val hasIqpTarget =
          (structureIs(semantic, StructureId.IQPBlack) && side == "white") ||
          (structureIs(semantic, StructureId.IQPWhite) && side == "black")
        val structureCode = if side == "white" then "structure_iqp_black" else "structure_iqp_white"
        Option.when(classification.simplifyBias.exchangeAvailable && hasIqpTarget) {
          evidence(
            ownerSide = side,
            kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.ExchangeAvailabilityBridge,
            confidence = 0.64,
            factIds =
              List(
                Some("exchange_availability_bridge"),
                Some(structureCode)
              ).flatten
          )
        }
      }

    val moveRefEvidence =
      pack.pieceMoveRefs
        .filter(ref => ref.ownerSide == side && ref.tacticalTheme.contains("capture_or_exchange"))
        .flatMap { ref =>
          Option.when(favorableTradeContext(side, ref, semantic, hasStructuredTradeSignal = false)) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.FavorableTradeOrTransformation,
              readiness = StrategicIdeaReadiness.Build,
              source = EvidenceSourceId.CaptureExchangeTransformation,
              confidence = 0.62 + moveRefSupportBonus(side, ref, semantic),
              focusSquares = List(ref.target),
              beneficiaryPieces = List(ref.piece),
              factIds = List("capture_or_exchange") ++ ref.evidence.filter(_.startsWith("target_"))
            )
          }
        }

    def softTransformationPlanSupport(plan: lila.commentary.model.PlanMatch): Boolean =
      plan.supports.exists { raw =>
        val text = raw.trim.toLowerCase
        text.contains("theme:favorable_exchange") ||
          text.contains(s"subplan:${PlanTaxonomy.PlanKind.DefenderTrade.id}") ||
          text.contains(s"subplan:${PlanTaxonomy.PlanKind.SimplificationWindow.id}") ||
          text.contains(s"subplan:${PlanTaxonomy.PlanKind.SimplificationConversion.id}")
      }

    val planBridge =
      semantic.plans
        .filter(plan => matchesSide(plan.plan.color, side) || softTransformationPlanSupport(plan))
        .sortBy(plan => -plan.score)
        .flatMap { plan =>
          val softPlanSupport = softTransformationPlanSupport(plan)
          Option.when(
            (
              plan.plan.id == PlanId.Exchange ||
                plan.plan.id == PlanId.Simplification ||
                plan.plan.id == PlanId.QueenTrade
            ) &&
              (
                defenderTagExchangeSupport.nonEmpty ||
                  classificationWindow.nonEmpty ||
                  exchangeAvailabilityBridge.nonEmpty ||
                  softPlanSupport ||
                  structureIs(semantic, StructureId.IQPBlack)
              )
          ) {
            evidence(
              ownerSide = side,
              kind = StrategicIdeaKind.FavorableTradeOrTransformation,
            readiness = StrategicIdeaReadiness.Build,
            source = EvidenceSourceId.PlanMatchTransformation,
            confidence =
              (if softPlanSupport then 0.92 else 0.68) +
                math.min(0.04, plan.score * 0.06),
              factIds =
                List("plan_match_transformation", s"plan_${plan.plan.id.toString.toLowerCase}") ++
                  Option.when(softPlanSupport)("soft_transformation_plan_support").toList
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
        val hasIqpTarget =
          (structureIs(semantic, StructureId.IQPBlack) && side == "white") ||
          (structureIs(semantic, StructureId.IQPWhite) && side == "black")
        val structureCode = if side == "white" then "structure_iqp_black" else "structure_iqp_white"
        Option.when(
          hasIqpTarget &&
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
              List(structureCode, "iqp_simplification_profile") ++
                Option.when(exchangeMoveRefs.nonEmpty)("capture_or_exchange").toList ++
                Option.when(exchangePlanSupport)("iqp_trade_down_plan").toList
          )
        }
      }

    defenderTagExchangeSupport ++ rookEndgamePattern ++ endgameTechniqueMotif ++ passedPawnConversionMotif ++ classificationWindow ++ exchangeAvailabilityBridge ++ moveRefEvidence ++ planBridge ++ iqpSimplification
