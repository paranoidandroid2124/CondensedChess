package lila.commentary.certification

object CertificationScopeContract:

  val frozenCertificationInventoryRowCount: Int = 10

  val frozenCertificationFamilyIds: Vector[CertificationFamilyId] = Vector(
    CertificationFamilyId("DevelopmentComparison"),
    CertificationFamilyId("InitiativeWindow"),
    CertificationFamilyId("MobilityComparison"),
    CertificationFamilyId("ComparativeKingFragility"),
    CertificationFamilyId("CertifiedKingSafetyEdge"),
    CertificationFamilyId("MateNetCertification"),
    CertificationFamilyId("MaterialHarvest"),
    CertificationFamilyId("WinningEndgame"),
    CertificationFamilyId("FortressDrawCertification"),
    CertificationFamilyId("PerpetualCheckHolding"),
    CertificationFamilyId("PromotionRace")
  )

  val activeCertificationFamilyIds: Vector[CertificationFamilyId] =
    frozenCertificationFamilyIds

  val activeFamilyIds: Vector[CertificationFamilyId] =
    activeCertificationFamilyIds

  private val activeFamilyIdSet: Set[CertificationFamilyId] = activeCertificationFamilyIds.toSet
  private val activeFamilyIdValues: Set[String] = activeCertificationFamilyIds.map(_.value).toSet

  require(
    activeCertificationFamilyIds.size == 11,
    s"Expected exactly 11 active certification families, found ${activeCertificationFamilyIds.size}"
  )
  require(
    activeCertificationFamilyIds.distinct.size == activeCertificationFamilyIds.size,
    "Duplicate certification families are not allowed"
  )

  def isActiveFamilyId(id: CertificationFamilyId): Boolean =
    activeFamilyIdSet.contains(id)

  private[certification] def requireActiveFamilyIds(
      familyIds: IterableOnce[CertificationFamilyId]
  ): Unit =
    val normalizedFamilyIds = familyIds.iterator.map(_.value).toVector
    val duplicateFamilyIds =
      normalizedFamilyIds
        .groupBy(identity)
        .collect { case (familyId, grouped) if grouped.size > 1 => familyId }
        .toVector
        .sorted
    val outOfScope =
      normalizedFamilyIds
        .filterNot(activeFamilyIdValues)
        .distinct
        .sorted

    require(
      duplicateFamilyIds.isEmpty,
      s"Duplicate certification family ids are not allowed: ${duplicateFamilyIds.mkString(", ")}"
    )
    require(
      outOfScope.isEmpty,
      s"Out-of-scope certification family ids: ${outOfScope.mkString(", ")}"
    )
