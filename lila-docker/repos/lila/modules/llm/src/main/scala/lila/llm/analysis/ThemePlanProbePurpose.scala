package lila.llm.analysis

import lila.llm.analysis.ThemeTaxonomy.SubplanId

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

  def isThemeValidationPurpose(raw: String): Boolean =
    themeValidationPurposes.contains(normalize(raw))

  def contractForPurpose(raw: String): Option[PurposeContract] =
    contracts.get(normalize(raw))

  def contractForSubplan(subplan: Option[SubplanId]): PurposeContract =
    subplan
      .flatMap(sp => contractForPurpose(purposeForSubplan(sp)))
      .getOrElse(contracts(ThemePlanValidation))

  def purposeForSubplan(subplan: SubplanId): String =
    subplan match
      case SubplanId.KeySquareDenial =>
        RouteDenialValidation
      case SubplanId.ProphylaxisRestraint | SubplanId.BreakPrevention |
          SubplanId.FlankClamp | SubplanId.CentralSpaceBind | SubplanId.MobilitySuppression =>
        LongTermRestraintValidation
      case SubplanId.OppositeBishopsConversion =>
        ColorComplexSqueezeValidation
      case _ =>
        ThemePlanValidation

  def mergedSignals(base: List[String], purpose: String): List[String] =
    val contractSignals = contractForPurpose(purpose).map(_.requiredSignals).getOrElse(Nil).toSet
    val merged = base.toSet ++ contractSignals
    val signalPriority = List("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot")
    signalPriority.filter(merged.contains) ++ (merged -- signalPriority.toSet).toList.sorted

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
