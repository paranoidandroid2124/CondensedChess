package lila.commentary.delta

import chess.Color

import lila.commentary.witness.{ WitnessPayload, WitnessSupport, WitnessValue }

private[delta] object TradeInvariantRule extends StrategicDeltaRule:

  val familyId: StrategicDeltaId = StrategicDeltaId("TradeInvariant")

  private val boundedFavorableSimplification =
    StrategicDeltaTag("bounded_favorable_simplification")
  private val forbiddenRivalFamily = StrategicDeltaId("TradeCompressionCorridor")

  def extract(
      context: StrategicDeltaContext,
      extractedSoFar: StrategicDeltaSet
  ): Vector[StrategicDelta] =
    for
      moverColor <- context.moverColor.toVector
      carrierEvidence <- TradeInvariantAdmission.firstSliceEvidence(context, moverColor).toVector
      if tradeInvariantTransition(extractedSoFar)
    yield buildDelta(moverColor, context, carrierEvidence)

  private def buildDelta(
      moverColor: Color,
      context: StrategicDeltaContext,
      carrierEvidence: TradeInvariantAdmission.CarrierEvidence
  ): StrategicDelta =
    owned(
      color = moverColor,
      scope = StrategicDeltaScope.MoveLocal,
      deltaTag = boundedFavorableSimplification,
      anchor = TradeInvariantAdmission.persistentCarrierAnchor,
      payload = WitnessPayload(
        "persistent_carrier_family" -> WitnessValue.Token(
          TradeInvariantAdmission.persistentCarrierFamily.value
        ),
        "persistent_carrier_anchor" -> WitnessValue.Token(
          TradeInvariantAdmission.persistentCarrierAnchor.key
        ),
        "material_reduction" -> WitnessValue.Number(context.nonKingNonPawnReduction)
      ),
      support = buildSupport(context, carrierEvidence)
    )

  private def buildSupport(
      context: StrategicDeltaContext,
      carrierEvidence: TradeInvariantAdmission.CarrierEvidence
  ): WitnessSupport =
    val withRoots =
      carrierEvidence.rootIndices.foldLeft(WitnessSupport.empty):
        case (acc, index) => acc.addRootIndex(index)
    val withCaptureSquare = withRoots.addTargetSquare(context.playedMove.dest)
    TradeInvariantAdmission.helperTags.foldLeft(withCaptureSquare):
      case (acc, tag) => acc.addTag(tag)

  private def tradeInvariantTransition(extractedSoFar: StrategicDeltaSet): Boolean =
    !extractedSoFar.contains(
        familyId = forbiddenRivalFamily,
        anchor = TradeInvariantAdmission.persistentCarrierAnchor,
        color = None,
        scope = Some(StrategicDeltaScope.MoveLocal),
        deltaTag = None
      )
