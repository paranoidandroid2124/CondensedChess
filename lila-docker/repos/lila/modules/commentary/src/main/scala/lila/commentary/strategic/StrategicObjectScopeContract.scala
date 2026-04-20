package lila.commentary.strategic

object StrategicObjectScopeContract:

  val activeObjectFamilyIds: Vector[StrategicObjectId] = Vector(
    StrategicObjectId("OpeningDevelopmentRegime"),
    StrategicObjectId("DistributedContactRegime"),
    StrategicObjectId("EndgameRaceScaffold"),
    StrategicObjectId("AttackScaffold"),
    StrategicObjectId("FortressHoldingShell"),
    StrategicObjectId("KingSafetyShell"),
    StrategicObjectId("CentralContactFront")
  )

  private val activeObjectFamilyIdSet: Set[StrategicObjectId] = activeObjectFamilyIds.toSet
  private val activeObjectFamilyIdValues: Set[String] = activeObjectFamilyIds.map(_.value).toSet

  require(
    activeObjectFamilyIds.size == 7,
    s"Expected exactly 7 active object families, found ${activeObjectFamilyIds.size}"
  )
  require(
    activeObjectFamilyIds.distinct.size == activeObjectFamilyIds.size,
    "Duplicate strategic object families are not allowed"
  )

  def isActiveObjectFamilyId(id: StrategicObjectId): Boolean =
    activeObjectFamilyIdSet.contains(id)

  private[strategic] def requireActiveObjectFamilyIds(
      familyIds: IterableOnce[StrategicObjectId]
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
        .filterNot(activeObjectFamilyIdValues)
        .distinct
        .sorted

    require(
      duplicateFamilyIds.isEmpty,
      s"Duplicate strategic object family ids are not allowed: ${duplicateFamilyIds.mkString(", ")}"
    )
    require(
      outOfScope.isEmpty,
      s"Out-of-scope strategic object family ids: ${outOfScope.mkString(", ")}"
    )
