package lila.commentary.witness.seed

object StrategySupportSeedScopeContract:

  val S17SamePieceLiabilityAnchor: StrategySupportSeedId =
    StrategySupportSeedId("same_piece_liability_anchor_seed")
  val S17SamePieceRepairRoute: StrategySupportSeedId =
    StrategySupportSeedId("same_piece_repair_route_seed")
  val S17SamePieceExchangeRelief: StrategySupportSeedId =
    StrategySupportSeedId("same_piece_exchange_relief_seed")

  val S23KingEntrySquare: StrategySupportSeedId =
    StrategySupportSeedId("king_entry_square_seed")
  val S23KingAccessRoute: StrategySupportSeedId =
    StrategySupportSeedId("king_access_route_seed")
  val S23KingOppositionContact: StrategySupportSeedId =
    StrategySupportSeedId("king_opposition_contact_seed")

  val S24TargetResourceDependency: StrategySupportSeedId =
    StrategySupportSeedId("target_resource_dependency_seed")
  val S24TargetAttackConvergence: StrategySupportSeedId =
    StrategySupportSeedId("target_attack_convergence_seed")

  val S25RankCorridorState: StrategySupportSeedId =
    StrategySupportSeedId("rank_corridor_state_seed")

  val seedIdsByBand: Map[String, Vector[StrategySupportSeedId]] = Map(
    "S17" -> Vector(
      S17SamePieceLiabilityAnchor,
      S17SamePieceRepairRoute,
      S17SamePieceExchangeRelief
    ),
    "S23" -> Vector(
      S23KingEntrySquare,
      S23KingAccessRoute,
      S23KingOppositionContact
    ),
    "S24" -> Vector(
      S24TargetResourceDependency,
      S24TargetAttackConvergence
    ),
    "S25" -> Vector(
      S25RankCorridorState
    )
  )

  val allowedSeedIds: Vector[StrategySupportSeedId] =
    Vector("S17", "S23", "S24", "S25").flatMap(seedIdsByBand)

  val allowedSeedIdSet: Set[StrategySupportSeedId] = allowedSeedIds.toSet
  private val allowedSeedIdValues: Set[String] = allowedSeedIds.map(_.value).toSet

  require(
    allowedSeedIds.size == 9,
    s"Expected exactly 9 allowed strategy support seed ids, found ${allowedSeedIds.size}"
  )
  require(
    allowedSeedIds.distinct.size == allowedSeedIds.size,
    "Duplicate strategy support seed ids are not allowed"
  )

  def isAllowedSeedId(seedId: StrategySupportSeedId): Boolean =
    allowedSeedIdSet.contains(seedId)

  private[seed] def requireAllowedSeedIds(seedIds: IterableOnce[StrategySupportSeedId]): Unit =
    val outOfScope =
      seedIds.iterator
        .map(_.value)
        .filterNot(allowedSeedIdValues)
        .toSet
        .toVector
        .sorted

    require(
      outOfScope.isEmpty,
      s"Out-of-scope strategy support seed ids: ${outOfScope.mkString(", ")}"
    )
