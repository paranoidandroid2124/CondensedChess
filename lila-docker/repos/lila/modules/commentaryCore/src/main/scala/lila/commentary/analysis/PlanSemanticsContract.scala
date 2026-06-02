package lila.commentary.analysis

import lila.commentary.StrategicIdeaKind
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme, SubplanCatalog }
import lila.commentary.analysis.semantic.StrategicObservationIds.ProofFamilyId

object PlanSemanticsContract:

  enum ClaimFamily:
    case Opening, Restriction, Redeployment, Space, Weakness, PawnBreak, Exchange, Flank, Conversion, Tactical, Unknown

  enum FallbackSafety:
    case StrategicAllowed, FactualOnly

  final case class FallbackPolicy(
      mayEmitStrategicFallback: Boolean,
      safety: FallbackSafety
  )

  final case class Contract(
      kind: PlanKind,
      theme: PlanTheme,
      claimFamily: ClaimFamily,
      strategicIdeaKinds: Set[String],
      fallbackSafety: FallbackSafety,
      probePurpose: String,
      requiredSignals: List[String],
      horizon: String,
      proposalOnly: Boolean = false,
      objective: String = "strategic objective"
  )

  private val contracts: Map[PlanKind, Contract] =
    PlanKind.values.toList.map { kind =>
      val spec = SubplanCatalog.specs.get(kind)
      kind -> Contract(
        kind = kind,
        theme = kind.theme,
        claimFamily = claimFamily(kind.theme),
        strategicIdeaKinds = ideaKinds(kind),
        fallbackSafety = fallbackSafety(kind.theme),
        probePurpose = probePurpose(kind),
        requiredSignals = spec.map(_.requiredSignals).getOrElse(Nil),
        horizon = spec.map(_.horizon).getOrElse("medium"),
        proposalOnly =
          kind == PlanKind.OpeningDevelopment ||
            kind.theme == PlanTheme.ImmediateTacticalGain,
        objective = spec.map(_.objective).getOrElse("strategic objective")
      )
    }.toMap

  def forKind(kind: PlanKind): Option[Contract] =
    contracts.get(kind)

  def forTheme(theme: PlanTheme): List[Contract] =
    contracts.valuesIterator.filter(_.theme == theme).toList.sortBy(_.kind.id)

  def strategicIdeaKinds(kind: PlanKind): Set[String] =
    forKind(kind).map(_.strategicIdeaKinds).getOrElse(Set.empty)

  def fallbackPolicy(theme: PlanTheme): FallbackPolicy =
    val safety = fallbackSafety(theme)
    FallbackPolicy(
      mayEmitStrategicFallback = safety == FallbackSafety.StrategicAllowed,
      safety = safety
    )

  def triggerKind(kind: PlanKind): String =
    kind match
      case PlanKind.BreakPrevention      => "break_neutralization"
      case PlanKind.KeySquareDenial      => "entry_square_denial"
      case PlanKind.OpenFilePressure     => "bounded_file_pressure"
      case PlanKind.RookFileTransfer     => "bounded_file_pressure"
      case PlanKind.DefenderTrade        => ProofFamilyId.TradeKeyDefender.wireKey
      case PlanKind.SimplificationWindow => PlanKind.SimplificationWindow.id
      case PlanKind.ProphylaxisRestraint => ProofFamilyId.CounterplayRestraint.wireKey
      case other                         => other.id

  def triggerKind(theme: PlanTheme, kind: Option[PlanKind]): String =
    kind.map(triggerKind).orElse(Option.when(theme != PlanTheme.Unknown)(theme.id)).getOrElse("strategic_claim")

  def triggerKind(themeId: String, subplanId: Option[String]): String =
    triggerKind(
      PlanTheme.fromId(themeId).getOrElse(PlanTheme.Unknown),
      subplanId.flatMap(PlanKind.fromId)
    )

  def proofFamily(kind: PlanKind): String =
    kind match
      case PlanKind.BreakPrevention      => ProofFamilyId.NeutralizeKeyBreak.wireKey
      case PlanKind.KeySquareDenial      => ProofFamilyId.HalfOpenFilePressure.wireKey
      case PlanKind.OpenFilePressure     => ProofFamilyId.HalfOpenFilePressure.wireKey
      case PlanKind.RookFileTransfer     => ProofFamilyId.HalfOpenFilePressure.wireKey
      case PlanKind.DefenderTrade        => ProofFamilyId.TradeKeyDefender.wireKey
      case PlanKind.SimplificationWindow => PlanKind.SimplificationWindow.id
      case other                         => other.id

  def proofFamily(theme: PlanTheme, kind: Option[PlanKind]): String =
    kind.map(proofFamily).orElse(Option.when(theme != PlanTheme.Unknown)(theme.id)).getOrElse("strategic_claim")

  def proofFamily(themeId: String, subplanId: Option[String]): String =
    proofFamily(
      PlanTheme.fromId(themeId).getOrElse(PlanTheme.Unknown),
      subplanId.flatMap(PlanKind.fromId)
    )

  private def claimFamily(theme: PlanTheme): ClaimFamily =
    theme match
      case PlanTheme.OpeningPrinciples       => ClaimFamily.Opening
      case PlanTheme.RestrictionProphylaxis  => ClaimFamily.Restriction
      case PlanTheme.PieceRedeployment       => ClaimFamily.Redeployment
      case PlanTheme.SpaceClamp              => ClaimFamily.Space
      case PlanTheme.WeaknessFixation        => ClaimFamily.Weakness
      case PlanTheme.PawnBreakPreparation    => ClaimFamily.PawnBreak
      case PlanTheme.FavorableExchange       => ClaimFamily.Exchange
      case PlanTheme.FlankInfrastructure     => ClaimFamily.Flank
      case PlanTheme.AdvantageTransformation => ClaimFamily.Conversion
      case PlanTheme.ImmediateTacticalGain   => ClaimFamily.Tactical
      case PlanTheme.Unknown                 => ClaimFamily.Unknown

  private def fallbackSafety(theme: PlanTheme): FallbackSafety =
    theme match
      case PlanTheme.OpeningPrinciples | PlanTheme.ImmediateTacticalGain | PlanTheme.Unknown =>
        FallbackSafety.FactualOnly
      case _ =>
        FallbackSafety.StrategicAllowed

  private def probePurpose(kind: PlanKind): String =
    kind match
      case PlanKind.KeySquareDenial =>
        ThemePlanProbePurpose.RouteDenialValidation
      case PlanKind.ProphylaxisRestraint | PlanKind.BreakPrevention |
          PlanKind.FlankClamp | PlanKind.CentralSpaceBind | PlanKind.MobilitySuppression =>
        ThemePlanProbePurpose.LongTermRestraintValidation
      case PlanKind.OppositeBishopsConversion =>
        ThemePlanProbePurpose.ColorComplexSqueezeValidation
      case _ =>
        ThemePlanProbePurpose.ThemePlanValidation

  private def ideaKinds(kind: PlanKind): Set[String] =
    kind match
      case PlanKind.BreakPrevention | PlanKind.KeySquareDenial =>
        Set(StrategicIdeaKind.CounterplaySuppression, StrategicIdeaKind.Prophylaxis)
      case PlanKind.ProphylaxisRestraint =>
        Set(StrategicIdeaKind.Prophylaxis)
      case PlanKind.OutpostEntrenchment =>
        Set(StrategicIdeaKind.OutpostCreationOrOccupation)
      case PlanKind.WorstPieceImprovement | PlanKind.BishopReanchor =>
        Set(StrategicIdeaKind.MinorPieceImbalanceExploitation, StrategicIdeaKind.LineOccupation)
      case PlanKind.RookFileTransfer | PlanKind.OpenFilePressure =>
        Set(StrategicIdeaKind.LineOccupation)
      case PlanKind.FlankClamp | PlanKind.CentralSpaceBind | PlanKind.MobilitySuppression =>
        Set(StrategicIdeaKind.SpaceGainOrRestriction)
      case PlanKind.StaticWeaknessFixation | PlanKind.MinorityAttackFixation |
          PlanKind.BackwardPawnTargeting | PlanKind.IQPInducement =>
        Set(StrategicIdeaKind.TargetFixing)
      case PlanKind.CentralBreakTiming | PlanKind.WingBreakTiming | PlanKind.TensionMaintenance =>
        Set(StrategicIdeaKind.PawnBreak)
      case PlanKind.SimplificationWindow | PlanKind.DefenderTrade | PlanKind.QueenTradeShield |
          PlanKind.SimplificationConversion | PlanKind.PasserConversion | PlanKind.PassedPawnManufacture |
          PlanKind.BadPieceLiquidation | PlanKind.InvasionTransition | PlanKind.OppositeBishopsConversion =>
        Set(StrategicIdeaKind.FavorableTradeOrTransformation)
      case PlanKind.RookPawnMarch | PlanKind.HookCreation | PlanKind.RookLiftScaffold =>
        Set(StrategicIdeaKind.KingAttackBuildUp)
      case PlanKind.OpeningDevelopment | PlanKind.ForcingTacticalShot | PlanKind.DefenderOverload |
          PlanKind.ClearanceBreak | PlanKind.BatteryPressure =>
        Set.empty
