package lila.commentary.analysis.claim

private[commentary] enum ClaimAuthorityTier:
  case CertifiedOwner
  case SupportedLocal
  case DiagnosticOnly
  case Suppressed

private[commentary] final case class ClaimAuthorityDecision(
    tier: ClaimAuthorityTier,
    failureCodes: List[String] = Nil
):
  def admitted: Boolean =
    tier == ClaimAuthorityTier.CertifiedOwner ||
      tier == ClaimAuthorityTier.SupportedLocal

  def supportedLocalWithoutTacticalVeto: Boolean =
    tier == ClaimAuthorityTier.SupportedLocal && vetoReasons.isEmpty

  def vetoReasons: List[String] =
    ClaimAuthorityDecision.tacticalVetoCodes(failureCodes)

private[commentary] object ClaimAuthorityDecision:

  private val TacticalVetoCodes =
    Set(
      "truth_contract_blunder",
      "truth_contract_missed_win",
      "truth_contract_tactical_refutation",
      "truth_contract_tactical_failure_mode",
      "planner_truth_mode_tactical",
      "main_claim_tactical",
      "context_severe_counterfactual",
      "tactical_context_missing",
      "truth_contract_missing"
    )

  def tacticalVetoCodes(codes: List[String]): List[String] =
    codes.filter(TacticalVetoCodes.contains)

  def isTacticalVetoCode(raw: String): Boolean =
    val code = normalize(raw)
    TacticalVetoCodes.contains(code) || code.contains("tactical")

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
