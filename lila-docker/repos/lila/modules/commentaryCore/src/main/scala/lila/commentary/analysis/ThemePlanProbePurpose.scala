package lila.commentary.analysis

import lila.commentary.analysis.PlanTaxonomy.PlanKind

object ThemePlanProbePurpose:

  final case class PurposeContract(
      purpose: String,
      objective: String,
      requiredSignals: List[String],
      horizon: String
  )

  val ThemePlanValidation = "theme_plan_validation"
  val RouteDenialValidation = "route_denial_validation"
  val ColorComplexSqueezeValidation = "color_complex_squeeze_validation"
  val LongTermRestraintValidation = "long_term_restraint_validation"
  val ReplyMultiPv = "reply_multipv"
  val DefenseReplyMultiPv = "defense_reply_multipv"
  val ConvertReplyMultiPv = "convert_reply_multipv"
  val RecaptureBranches = "recapture_branches"
  val KeepTensionBranches = "keep_tension_branches"
  val PlayedMoveCounterfactual = "played_move_counterfactual"
  val NullMoveThreat = "NullMoveThreat"

  private val contracts: Map[String, PurposeContract] = Map(
    ThemePlanValidation -> PurposeContract(
      purpose = ThemePlanValidation,
      objective = "validate_plan_presence",
      requiredSignals = List("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"),
      horizon = "medium"
    ),
    RouteDenialValidation -> PurposeContract(
      purpose = RouteDenialValidation,
      objective = "validate_route_denial",
      requiredSignals = List("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"),
      horizon = "medium"
    ),
    ColorComplexSqueezeValidation -> PurposeContract(
      purpose = ColorComplexSqueezeValidation,
      objective = "validate_color_complex_squeeze",
      requiredSignals = List("replyPvs", "keyMotifs", "futureSnapshot"),
      horizon = "long"
    ),
    LongTermRestraintValidation -> PurposeContract(
      purpose = LongTermRestraintValidation,
      objective = "validate_long_term_restraint",
      requiredSignals = List("replyPvs", "keyMotifs", "futureSnapshot"),
      horizon = "long"
    )
  )

  private val themeValidationPurposes: Set[String] = contracts.keySet
  private val directReplyPurposes: Set[String] =
    Set(DefenseReplyMultiPv, ReplyMultiPv).map(normalize)
  private val conversionReplyPurposes: Set[String] =
    Set(ConvertReplyMultiPv, DefenseReplyMultiPv).map(normalize)
  private val routeValidationPurposes: Set[String] =
    Set(RouteDenialValidation, LongTermRestraintValidation).map(normalize)
  private val routeContinuityPurposes: Set[String] =
    routeValidationPurposes + normalize(ConvertReplyMultiPv)
  private val authorReplyBranchPurposes: Set[String] =
    Set(ReplyMultiPv, DefenseReplyMultiPv, ConvertReplyMultiPv).map(normalize)
  private val signalPriority = List("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot")
  private val purposeRequiredSignals: Map[String, List[String]] =
    Map(
      ReplyMultiPv -> List("replyPvs"),
      DefenseReplyMultiPv -> List("replyPvs"),
      ConvertReplyMultiPv -> List("replyPvs"),
      RecaptureBranches -> List("replyPvs"),
      KeepTensionBranches -> List("replyPvs"),
      PlayedMoveCounterfactual -> List("replyPvs", "l1Delta"),
      NullMoveThreat -> List("replyPvs", "keyMotifs", "l1Delta")
    ).map { case (purpose, signals) => normalize(purpose) -> signals }
  private val purposeObjectives: Map[String, String] =
    Map(
      ReplyMultiPv -> "compare_reply_branches",
      DefenseReplyMultiPv -> "validate_defensive_resources",
      ConvertReplyMultiPv -> "validate_conversion_route",
      RecaptureBranches -> "compare_recapture_structures",
      KeepTensionBranches -> "compare_tension_branches",
      PlayedMoveCounterfactual -> "counterfactual_probe",
      NullMoveThreat -> "validate_restriction_prophylaxis"
    ).map { case (purpose, objective) => normalize(purpose) -> objective }
  private val purposeHorizons: Map[String, String] =
    Map(
      ReplyMultiPv -> "short",
      DefenseReplyMultiPv -> "short",
      ConvertReplyMultiPv -> "medium",
      RecaptureBranches -> "short",
      KeepTensionBranches -> "short",
      PlayedMoveCounterfactual -> "short",
      NullMoveThreat -> "short"
    ).map { case (purpose, horizon) => normalize(purpose) -> horizon }
  private val purposeBudgets: Map[String, Int] =
    Map(
      ThemePlanValidation -> 3,
      RouteDenialValidation -> 2,
      ColorComplexSqueezeValidation -> 2,
      LongTermRestraintValidation -> 2,
      ReplyMultiPv -> 2,
      DefenseReplyMultiPv -> 2,
      ConvertReplyMultiPv -> 2,
      RecaptureBranches -> 1,
      KeepTensionBranches -> 1,
      PlayedMoveCounterfactual -> 1,
      NullMoveThreat -> 2
    ).map { case (purpose, budget) => normalize(purpose) -> budget }

  def isThemeValidationPurpose(raw: String): Boolean =
    themeValidationPurposes.contains(normalize(raw))

  def isDirectReplyPurpose(raw: String): Boolean =
    directReplyPurposes.contains(normalize(raw))

  def isReplyMultiPvPurpose(raw: String): Boolean =
    normalize(raw) == normalize(ReplyMultiPv)

  def isDefenseReplyPurpose(raw: String): Boolean =
    normalize(raw) == normalize(DefenseReplyMultiPv)

  def isConversionReplyPurpose(raw: String): Boolean =
    conversionReplyPurposes.contains(normalize(raw))

  def isRouteValidationPurpose(raw: String): Boolean =
    routeValidationPurposes.contains(normalize(raw))

  def isRouteContinuityPurpose(raw: String): Boolean =
    routeContinuityPurposes.contains(normalize(raw))

  def isAuthorReplyBranchPurpose(raw: String): Boolean =
    authorReplyBranchPurposes.contains(normalize(raw))

  def isRecaptureBranchPurpose(raw: String): Boolean =
    normalize(raw) == normalize(RecaptureBranches)

  def isKeepTensionBranchPurpose(raw: String): Boolean =
    normalize(raw) == normalize(KeepTensionBranches)

  def isAuthorEvidencePurpose(raw: String): Boolean =
    isAuthorReplyBranchPurpose(raw) ||
      isRecaptureBranchPurpose(raw) ||
      isKeepTensionBranchPurpose(raw)

  def requiresMultipleBranches(raw: String): Boolean =
    isRecaptureBranchPurpose(raw) || isKeepTensionBranchPurpose(raw)

  def isConvertReplyPurpose(raw: String): Boolean =
    normalize(raw) == normalize(ConvertReplyMultiPv)

  def isPlayedMoveCounterfactualPurpose(raw: String): Boolean =
    normalize(raw) == normalize(PlayedMoveCounterfactual)

  def isNullMoveThreatPurpose(raw: String): Boolean =
    normalize(raw) == normalize(NullMoveThreat)

  def contractForPurpose(raw: String): Option[PurposeContract] =
    contracts.get(normalize(raw))

  def requiredSignalsForPurpose(raw: String): Option[List[String]] =
    contractForPurpose(raw).map(_.requiredSignals).orElse(purposeRequiredSignals.get(normalize(raw)))

  def objectiveForPurpose(raw: String): Option[String] =
    contractForPurpose(raw).map(_.objective).orElse(purposeObjectives.get(normalize(raw)))

  def horizonForPurpose(raw: String): Option[String] =
    contractForPurpose(raw).map(_.horizon).orElse(purposeHorizons.get(normalize(raw)))

  def budgetForPurpose(raw: String): Option[Int] =
    purposeBudgets.get(normalize(raw))

  def contractForSubplan(subplan: Option[PlanKind]): PurposeContract =
    subplan
      .flatMap(sp => contractForPurpose(purposeForSubplan(sp)))
      .getOrElse(contracts(ThemePlanValidation))

  def purposeForSubplan(subplan: PlanKind): String =
    subplan match
      case PlanKind.KeySquareDenial =>
        RouteDenialValidation
      case PlanKind.ProphylaxisRestraint | PlanKind.BreakPrevention |
          PlanKind.FlankClamp | PlanKind.CentralSpaceBind | PlanKind.MobilitySuppression =>
        LongTermRestraintValidation
      case PlanKind.OppositeBishopsConversion =>
        ColorComplexSqueezeValidation
      case _ =>
        ThemePlanValidation

  def mergedSignals(base: List[String], purpose: String): List[String] =
    val contractSignals = contractForPurpose(purpose).map(_.requiredSignals).getOrElse(Nil).toSet
    orderSignals(base.toSet ++ contractSignals)

  def orderSignals(signals: Iterable[String]): List[String] =
    val signalSet = signals.toSet
    signalPriority.filter(signalSet.contains) ++ (signalSet -- signalPriority.toSet).toList.sorted

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
