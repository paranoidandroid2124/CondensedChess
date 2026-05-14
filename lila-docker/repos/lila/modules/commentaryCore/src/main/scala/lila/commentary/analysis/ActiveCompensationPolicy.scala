package lila.commentary.analysis

import lila.commentary.StrategyPack

private[commentary] object ActiveCompensationPolicy:

  def noteExpected(surface: StrategyPackSurface.Snapshot): Boolean =
    CompensationContractMatcher
      .canonicalSubtype(surface)
      .exists(subtype => surface.compensationPosition && !subtype.transitionOnly)

  def noteExpected(strategyPack: Option[StrategyPack]): Boolean =
    noteExpected(StrategyPackSurface.from(strategyPack))

  def parityEligible(surface: StrategyPackSurface.Snapshot): Boolean =
    val canonicalLabel = StrategyPackSurface.strictCompensationSubtypeLabel(surface)
    surface.compensationPosition &&
      surface.compensationContractResolved &&
      StrategyPackSurface.resolvedDisplaySubtypeSource(surface) != "raw_fallback" &&
      canonicalLabel.nonEmpty

  def parityEligible(strategyPack: Option[StrategyPack]): Boolean =
    parityEligible(StrategyPackSurface.from(strategyPack))

  def selectionEligible(surface: StrategyPackSurface.Snapshot): Boolean =
    def hasConcreteCarrierAnchor: Boolean =
      List(
        StrategyPackSurface.resolvedNormalizedExecutionText(surface),
        StrategyPackSurface.resolvedNormalizedObjectiveText(surface),
        StrategyPackSurface.resolvedNormalizedLongTermFocusText(surface),
        surface.executionText,
        surface.objectiveText,
        surface.focusText
      ).flatten.exists(_.trim.nonEmpty)

    parityEligible(surface) ||
      (noteExpected(surface) && hasConcreteCarrierAnchor)

  def selectionEligible(strategyPack: Option[StrategyPack]): Boolean =
    selectionEligible(StrategyPackSurface.from(strategyPack))
