package lila.llm.analysis

object ProbePurposeClassifier:

  private val HypothesisPurposes = Set(
    "free_tempo_branches",
    "latent_plan_immediate",
    "latent_plan_refutation"
  )

  private val LatentSupportPurposes = Set(
    "free_tempo_branches",
    "latent_plan_immediate"
  )

  private val RefutationPurposes = Set("latent_plan_refutation")

  def isHypothesisPurpose(raw: String): Boolean =
    ThemePlanProbePurpose.isThemeValidationPurpose(raw) ||
      HypothesisPurposes.contains(normalize(raw))

  def isLatentSupportPurpose(raw: String): Boolean =
    LatentSupportPurposes.contains(normalize(raw))

  def isRefutationPurpose(raw: String): Boolean =
    RefutationPurposes.contains(normalize(raw))

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
