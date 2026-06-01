package lila.commentary.analysis

object ProbePurposeClassifier:

  final case class PurposeProfile(
      purpose: String,
      objective: String,
      requiredSignals: List[String],
      horizon: String,
      defaultMaxCpLoss: Int,
      refutation: Boolean
  )

  private val CompetitiveProbeIdPrefix = "competitive"
  private val AggressiveProbeIdPrefix = "aggressive"

  val FreeTempoBranches = "free_tempo_branches"
  val LatentPlanImmediate = "latent_plan_immediate"
  val LatentPlanRefutation = "latent_plan_refutation"

  private val HypothesisProfiles: Map[String, PurposeProfile] =
    List(
      PurposeProfile(
        purpose = FreeTempoBranches,
        objective = "validate_latent_plan",
        requiredSignals = List("replyPvs", "futureSnapshot"),
        horizon = "long",
        defaultMaxCpLoss = 90,
        refutation = false
      ),
      PurposeProfile(
        purpose = LatentPlanImmediate,
        objective = "validate_immediate_viability",
        requiredSignals = List("replyPvs", "l1Delta"),
        horizon = "medium",
        defaultMaxCpLoss = 80,
        refutation = false
      ),
      PurposeProfile(
        purpose = LatentPlanRefutation,
        objective = "refute_plan",
        requiredSignals = List("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"),
        horizon = "long",
        defaultMaxCpLoss = 120,
        refutation = true
      )
    ).map(profile => normalize(profile.purpose) -> profile).toMap

  def profileForPurpose(raw: String): Option[PurposeProfile] =
    HypothesisProfiles.get(normalize(raw))

  def requiredSignalsForPurpose(raw: String): Option[List[String]] =
    profileForPurpose(raw).map(_.requiredSignals)

  def objectiveForPurpose(raw: String): Option[String] =
    profileForPurpose(raw).map(_.objective)

  def horizonForPurpose(raw: String): Option[String] =
    profileForPurpose(raw).map(_.horizon)

  def defaultMaxCpLossForPurpose(raw: String): Option[Int] =
    if ThemePlanProbePurpose.isThemeValidationPurpose(raw) then Some(95)
    else if ThemePlanProbePurpose.isAuthorEvidencePurpose(raw) then Some(100)
    else profileForPurpose(raw).map(_.defaultMaxCpLoss)

  def isKnownSupportPurpose(raw: String): Boolean =
    ThemePlanProbePurpose.isThemeValidationPurpose(raw) ||
      ThemePlanProbePurpose.isAuthorEvidencePurpose(raw) ||
      isLatentSupportPurpose(raw)

  def isKnownProbePurpose(raw: String): Boolean =
    isKnownSupportPurpose(raw) || isRefutationPurpose(raw)

  def isHypothesisPurpose(raw: String): Boolean =
    ThemePlanProbePurpose.isThemeValidationPurpose(raw) ||
      profileForPurpose(raw).isDefined

  def isLatentSupportPurpose(raw: String): Boolean =
    profileForPurpose(raw).exists(profile => !profile.refutation)

  def isRefutationPurpose(raw: String): Boolean =
    profileForPurpose(raw).exists(_.refutation)

  def isCompetitiveProbeId(raw: String): Boolean =
    normalize(raw).startsWith(CompetitiveProbeIdPrefix)

  def isAggressiveProbeId(raw: String): Boolean =
    normalize(raw).startsWith(AggressiveProbeIdPrefix)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
