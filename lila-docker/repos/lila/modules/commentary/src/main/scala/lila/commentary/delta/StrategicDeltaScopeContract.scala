package lila.commentary.delta

object StrategicDeltaScopeContract:

  val activeDeltaFamilyIds: Vector[StrategicDeltaId] = Vector(
    StrategicDeltaId("TradeCompressionCorridor"),
    StrategicDeltaId("TradeInvariant")
  )

  private val activeDeltaFamilyIdSet: Set[StrategicDeltaId] = activeDeltaFamilyIds.toSet
  private val activeDeltaFamilyIdValues: Set[String] = activeDeltaFamilyIds.map(_.value).toSet

  require(
    activeDeltaFamilyIds.size == 2,
    s"Expected exactly 2 active delta families, found ${activeDeltaFamilyIds.size}"
  )
  require(
    activeDeltaFamilyIds.distinct.size == activeDeltaFamilyIds.size,
    "Duplicate strategic delta families are not allowed"
  )

  def isActiveDeltaFamilyId(id: StrategicDeltaId): Boolean =
    activeDeltaFamilyIdSet.contains(id)

  private[delta] def requireActiveDeltaFamilyIds(
      familyIds: IterableOnce[StrategicDeltaId]
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
        .filterNot(activeDeltaFamilyIdValues)
        .distinct
        .sorted

    require(
      duplicateFamilyIds.isEmpty,
      s"Duplicate strategic delta family ids are not allowed: ${duplicateFamilyIds.mkString(", ")}"
    )
    require(
      outOfScope.isEmpty,
      s"Out-of-scope strategic delta family ids: ${outOfScope.mkString(", ")}"
    )
