package lila.commentary.certification

private[certification] object CertificationInternalRuntime:

  private def validateRegisteredRules(
      candidateRules: IterableOnce[CertificationRule]
  ): Vector[CertificationRule] =
    val rules = candidateRules.iterator.toVector
    CertificationScopeContract.requireActiveFamilyIds(rules.map(_.familyId))
    require(
      rules.map(_.familyId).toSet == CertificationScopeContract.activeCertificationFamilyIds.toSet,
      s"Certification runtime must register exactly ${CertificationScopeContract.activeCertificationFamilyIds.map(_.value).mkString(", ")}"
    )
    rules

  private val rules: Vector[CertificationRule] = validateRegisteredRules(
    Vector(
      DevelopmentComparisonRule,
      InitiativeWindowRule,
      MobilityComparisonRule,
      ComparativeKingFragilityRule,
      CertifiedKingSafetyEdgeRule,
      MateNetCertificationRule,
      MaterialHarvestRule,
      WinningEndgameRule,
      FortressDrawCertificationRule,
      PerpetualCheckHoldingRule,
      PromotionRaceRule,
      SpaceBindRestrictionCertificationRule
    )
  )

  def extract(context: CertificationContext): CertificationSet =
    rules.foldLeft(CertificationSet.empty): (acc, rule) =>
      CertificationSet(acc.all ++ rule.extract(context, acc))
