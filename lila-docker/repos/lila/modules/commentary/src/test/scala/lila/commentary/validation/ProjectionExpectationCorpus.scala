package lila.commentary.validation

import chess.{ Color, Square }
import chess.format.{ Fen, Uci }

import lila.commentary.projection.{
  StrategyProjectionBandId,
  StrategyProjectionCoverageContract,
  StrategyProjectionEvidenceClaim,
  StrategyProjectionEvidenceKind,
  StrategyProjectionScopeContract
}
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessValue }
import lila.commentary.witness.seed.StrategySupportSeedId

import play.api.libs.json.*

private[validation] object ProjectionExpectationCorpus:

  val resourcePath = "/commentary-corpus/projection-expectations.jsonl"

  val liveAdmissionBands: Set[String] =
    StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
  val legacyRuntimeExceptionBands: Set[String] =
    Set.empty
  val deferredCoverageGateBands: Set[String] =
    Set.empty
  val positionalAccessCoverageBands: Set[String] =
    StrategyProjectionCoverageContract.positionalAccessBandIds.map(_.value).toSet
  val conversionHoldTargetCoverageBands: Set[String] =
    StrategyProjectionCoverageContract.conversionHoldTargetBandIds.map(_.value).toSet
  val initiativeReleaseCounterplayCoverageBands: Set[String] =
    StrategyProjectionCoverageContract.initiativeReleaseCounterplayBandIds.map(_.value).toSet
  val kingAttackCoverageBands: Set[String] =
    StrategyProjectionCoverageContract.kingAttackBandIds.map(_.value).toSet
  val pawnStructureDamageCoverageBands: Set[String] =
    StrategyProjectionCoverageContract.pawnStructureDamageBandIds.map(_.value).toSet
  val passerCreationSuppressionCoverageBands: Set[String] =
    StrategyProjectionCoverageContract.passerCreationSuppressionBandIds.map(_.value).toSet
  val allCoverageBands: Set[String] =
    StrategyProjectionCoverageContract.allProjectionBandIds.map(_.value).toSet
  val countableCoverageBands: Set[String] =
    positionalAccessCoverageBands ++
      conversionHoldTargetCoverageBands ++
      initiativeReleaseCounterplayCoverageBands ++
      kingAttackCoverageBands ++
      pawnStructureDamageCoverageBands ++
      passerCreationSuppressionCoverageBands
  val pendingCoverageGateBands: Set[String] =
    Set.empty
  val coverageGateBands: Set[String] =
    countableCoverageBands ++ pendingCoverageGateBands
  val requiredBands: Vector[String] =
    (liveAdmissionBands ++ coverageGateBands).toVector.sorted
  val coverageCaseTypes: Set[String] =
    Set("exact", "near_miss", "contrastive", "comparative_false_rival", "nasty_negative", "negative")
  val requiredRuntimeCaseTypes: Set[String] =
    Set("exact", "near_miss", "nasty_negative", "comparative_false_rival")
  val requiredCaseTypes: Set[String] = coverageCaseTypes
  val requiredExpectations: Set[String] = Set("admitted", "rejected")
  val requiredOwners: Set[String] = Set("white", "black")
  val requiredEvidenceKindsByBand: Map[String, Set[String]] =
    StrategyProjectionScopeContract.requiredEvidenceKindsByBand.view
      .mapValues(_.map(_.value).toSet)
      .toMap
  private val projectionEvidenceKindNames: Set[String] =
    requiredEvidenceKindsByBand.values.flatten.toSet

  val allowedSupportSeedIdsByBand: Map[String, Set[String]] = Map(
    "S17" -> Set(
      "same_piece_liability_anchor_seed",
      "same_piece_repair_route_seed",
      "same_piece_exchange_relief_seed"
    ),
    "S23" -> Set(
      "king_entry_square_seed",
      "king_access_route_seed",
      "king_opposition_contact_seed"
    ),
    "S24" -> Set(
      "target_resource_dependency_seed",
      "target_attack_convergence_seed"
    ),
    "S25" -> Set(
      "rank_corridor_state_seed"
    )
  )

  val requiredForbiddenRivalBandsByBand: Map[String, Set[String]] = Map(
    "S01" -> Set("S05", "S21"),
    "S02" -> Set("S03", "S04", "S09"),
    "S03" -> Set("S02", "S12"),
    "S04" -> Set("S01", "S02", "S03", "S24"),
    "S05" -> Set("S06", "S14", "S21"),
    "S06" -> Set("S05", "S20"),
    "S07" -> Set("S08"),
    "S08" -> Set("S07", "S20", "S21"),
    "S09" -> Set("S02", "S23", "S25"),
    "S10" -> Set("S12"),
    "S11" -> Set("S13", "S14"),
    "S12" -> Set("S03", "S10", "S20"),
    "S13" -> Set("S11", "S14", "S15"),
    "S14" -> Set("S05", "S11", "S13", "S15"),
    "S15" -> Set("S13", "S14", "S16"),
    "S16" -> Set("S15", "S22", "S23"),
    "S17" -> Set("S18", "S20"),
    "S18" -> Set("S12", "S17", "S20"),
    "S19" -> Set("S22", "S24"),
    "S20" -> Set("S06", "S12", "S17"),
    "S21" -> Set("S01", "S05", "S08"),
    "S22" -> Set("S19", "S23"),
    "S23" -> Set("S09", "S16", "S22"),
    "S24" -> Set("S04", "S19"),
    "S25" -> Set("S02", "S03", "S04", "S09", "S12", "S23", "S24")
  )

  final case class CoverageGateSpec(axis: String, buckets: Set[String]):
    require(axis.matches("^[a-z][a-z0-9_]*$"), s"Invalid projection coverage axis $axis")
    require(buckets.nonEmpty, s"Projection coverage axis $axis must have at least one bucket")
    require(
      buckets.forall(_.matches("^[a-z][a-z0-9_]*$")),
      s"Invalid projection coverage buckets for $axis: ${buckets.toVector.sorted.mkString(", ")}"
    )

    def pairs: Set[(String, String)] = buckets.map(axis -> _)

  private val positionalAccessCoverageGateSpecsByBand: Map[String, Vector[CoverageGateSpec]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(positionalAccessCoverageBands.contains)
      .mapValues(gates => gates.map(gate => CoverageGateSpec(gate.axis, gate.buckets.toSet)))
      .toMap

  private val conversionHoldTargetCoverageGateSpecsByBand: Map[String, Vector[CoverageGateSpec]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(conversionHoldTargetCoverageBands.contains)
      .mapValues(gates => gates.map(gate => CoverageGateSpec(gate.axis, gate.buckets.toSet)))
      .toMap

  private val initiativeReleaseCounterplayCoverageGateSpecsByBand: Map[String, Vector[CoverageGateSpec]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(initiativeReleaseCounterplayCoverageBands.contains)
      .mapValues(gates => gates.map(gate => CoverageGateSpec(gate.axis, gate.buckets.toSet)))
      .toMap

  private val kingAttackCoverageGateSpecsByBand: Map[String, Vector[CoverageGateSpec]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(kingAttackCoverageBands.contains)
      .mapValues(gates => gates.map(gate => CoverageGateSpec(gate.axis, gate.buckets.toSet)))
      .toMap

  private val pawnStructureDamageCoverageGateSpecsByBand: Map[String, Vector[CoverageGateSpec]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(pawnStructureDamageCoverageBands.contains)
      .mapValues(gates => gates.map(gate => CoverageGateSpec(gate.axis, gate.buckets.toSet)))
      .toMap

  private val passerCreationSuppressionCoverageGateSpecsByBand: Map[String, Vector[CoverageGateSpec]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(passerCreationSuppressionCoverageBands.contains)
      .mapValues(gates => gates.map(gate => CoverageGateSpec(gate.axis, gate.buckets.toSet)))
      .toMap

  private val pendingCoverageGatesByBand: Map[String, Vector[CoverageGateSpec]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(pendingCoverageGateBands.contains)
      .mapValues(gates => gates.map(gate => CoverageGateSpec(gate.axis, gate.buckets.toSet)))
      .toMap

  val countableCoverageGatesByBand: Map[String, Vector[CoverageGateSpec]] =
    positionalAccessCoverageGateSpecsByBand ++
      conversionHoldTargetCoverageGateSpecsByBand ++
      initiativeReleaseCounterplayCoverageGateSpecsByBand ++
      kingAttackCoverageGateSpecsByBand ++
      pawnStructureDamageCoverageGateSpecsByBand ++
      passerCreationSuppressionCoverageGateSpecsByBand
  val allCoverageGatesByBand: Map[String, Vector[CoverageGateSpec]] =
    countableCoverageGatesByBand ++ pendingCoverageGatesByBand

  require(
    countableCoverageGatesByBand.keySet == countableCoverageBands,
    "Projection countable coverage gates must match the executable coverage-band set"
  )
  require(
    countableCoverageBands == allCoverageBands,
    "Projection countable coverage bands must match the complete executable band set"
  )
  require(
    allCoverageGatesByBand.keySet == coverageGateBands,
    "Projection coverage gates must match the executable coverage-band set"
  )

  def requiredCoveragePairsFor(band: String): Set[(String, String)] =
    allCoverageGatesByBand.getOrElse(band, Vector.empty).flatMap(_.pairs).toSet

  private val admittedCoverageAxes: Set[String] =
    Set(
      "center_release_route",
      "initiative_conversion_route",
      "denial_route",
      "counterplay_survival_route",
      "restriction_route",
      "penetration_route",
      "occupancy_scope",
      "durability_route",
      "access_route",
      "domination_route",
      "king_activity_route",
      "creation_route",
      "suppression_route",
      "rank_access_route",
      "liability_relief_route",
      "minor_edge_conversion_route",
      "simplification_route",
      "hold_route",
      "prepared_target_route",
      "storm_route",
      "attack_concentration_route",
      "diagonal_attack_route",
      "shelter_breach_route",
      "target_pressure_route",
      "wing_damage_route",
      "chain_base_route"
    )
  private val nearMissCoverageAxes: Set[String] =
    Set("same_cluster_near_miss")
  private val shortcutNegativeCoverageAxes: Set[String] =
    Set("shortcut_negative")

  private val countableCoverageAxes: Set[String] =
    admittedCoverageAxes ++ nearMissCoverageAxes ++ shortcutNegativeCoverageAxes

  def coveragePairsByBand(rows: Iterable[Row]): Map[String, Set[(String, String)]] =
    rows.foldLeft(Map.empty[String, Set[(String, String)]]): (acc, row) =>
      row.countableCoveragePair match
        case Some(pair) =>
          acc.updated(row.band, acc.getOrElse(row.band, Set.empty) + pair)
        case None => acc

  def missingRequiredCoveragePairsByBand(rows: Iterable[Row]): Map[String, Set[(String, String)]] =
    val present = coveragePairsByBand(rows)
    countableCoverageBands.flatMap: band =>
      val missing = requiredCoveragePairsFor(band) -- present.getOrElse(band, Set.empty)
      Option.when(missing.nonEmpty)(band -> missing)
    .toMap

  def missingCoverageGatePairsByBand(rows: Iterable[Row]): Map[String, Set[(String, String)]] =
    val present = coveragePairsByBand(rows)
    coverageGateBands.flatMap: band =>
      val missing = requiredCoveragePairsFor(band) -- present.getOrElse(band, Set.empty)
      Option.when(missing.nonEmpty)(band -> missing)
    .toMap

  def bandsWithCompleteCoverage(rows: Iterable[Row]): Set[String] =
    val missing = missingRequiredCoveragePairsByBand(rows).keySet
    countableCoverageBands.filterNot(missing.contains)

  final case class EvidenceClaimRow(
      kind: String,
      anchor: String,
      reliefKind: Option[String],
      corridorKind: Option[String],
      entrySquare: Option[String],
      contactSquare: Option[String],
      targetSquare: Option[String],
      sourceSquares: List[String],
      kingRingTargetSquares: List[String],
      contactSourceSquare: Option[String],
      defendingKingSquare: Option[String],
      shellAnchorSquare: Option[String],
      breachSquares: List[String],
      kingWingStormRoute: Option[String],
      kingRingConcentrationRoute: Option[String],
      diagonalKingAttackRoute: Option[String],
      kingShelterBreachRoute: Option[String],
      centerReleaseRoute: Option[String],
      filePenetrationRoute: Option[String],
      initiativeConversionRoute: Option[String],
      counterplayDenialRoute: Option[String],
      counterplaySurvivalRoute: Option[String],
      outpostOccupationRoute: Option[String],
      accessRoute: Option[String],
      accessWitnessId: Option[String],
      localAccessSuperiority: Option[String],
      weakOutpostSquare: Option[String],
      weakOutpostState: Option[String],
      diagonalSourceSquare: Option[String],
      diagonalEndpointSquares: List[String],
      dominationRoute: Option[String],
      supportWitnessId: Option[String],
      supportTargetSquares: List[String],
      spaceBindRoute: Option[String],
      routeAnchorSquare: Option[String],
      structuralSector: Option[String],
      structuralHostId: Option[String],
      outpostSquare: Option[String],
      restrictionAnchorSquare: Option[String],
      creationRoute: Option[String],
      suppressionRoute: Option[String],
      passerSquare: Option[String],
      blockerSquare: Option[String],
      damageRoute: Option[String],
      chainBaseRoute: Option[String],
      chainBaseForwardSquares: List[String],
      damageSector: Option[String],
      pressureRoute: Option[String],
      persistenceKind: Option[String],
      pressureSourceSquares: List[String],
      certificationFamily: Option[String],
      activeBishopSquare: Option[String],
      bishopMemberSquares: List[String],
      conversionTargetSquares: List[String]
  ):
    def toRuntimeClaim(row: Row): StrategyProjectionEvidenceClaim =
      val payloadEntries =
          reliefKind.toVector.map(kind => "relief_kind" -> WitnessValue.Token(kind)) ++
          corridorKind.toVector.map(kind => "corridor_kind" -> WitnessValue.Token(kind)) ++
          entrySquare.toVector.map(square => "entry_square" -> WitnessValue.SquareValue(parseSquare(square))) ++
          contactSquare.toVector.map(square => "contact_square" -> WitnessValue.SquareValue(parseSquare(square))) ++
          targetSquare.toVector.map(square => "target_square" -> WitnessValue.SquareValue(parseSquare(square))) ++
          Option.when(sourceSquares.nonEmpty)(
            "source_squares" -> WitnessValue.SquareListValue(sourceSquares.map(parseSquare).toVector)
          ).toVector ++
          Option.when(kingRingTargetSquares.nonEmpty)(
            "king_ring_target_squares" -> WitnessValue.SquareListValue(kingRingTargetSquares.map(parseSquare).toVector)
          ).toVector ++
          contactSourceSquare.toVector.map(square =>
            "contact_source_square" -> WitnessValue.SquareValue(parseSquare(square))
          ) ++
          defendingKingSquare.toVector.map(square =>
            "defending_king_square" -> WitnessValue.SquareValue(parseSquare(square))
          ) ++
          shellAnchorSquare.toVector.map(square =>
            "shell_anchor_square" -> WitnessValue.SquareValue(parseSquare(square))
          ) ++
          Option.when(breachSquares.nonEmpty)(
            "breach_squares" -> WitnessValue.SquareListValue(breachSquares.map(parseSquare).toVector)
          ).toVector ++
          kingWingStormRoute.toVector.map(route => "king_wing_storm_route" -> WitnessValue.Token(route)) ++
          kingRingConcentrationRoute.toVector.map(route =>
            "king_ring_concentration_route" -> WitnessValue.Token(route)
          ) ++
          diagonalKingAttackRoute.toVector.map(route =>
            "diagonal_king_attack_route" -> WitnessValue.Token(route)
          ) ++
          kingShelterBreachRoute.toVector.map(route =>
            "king_shelter_breach_route" -> WitnessValue.Token(route)
          ) ++
          centerReleaseRoute.toVector.map(route => "center_release_route" -> WitnessValue.Token(route)) ++
          filePenetrationRoute.toVector.map(route => "file_penetration_route" -> WitnessValue.Token(route)) ++
          initiativeConversionRoute.toVector.map(route =>
            "initiative_conversion_route" -> WitnessValue.Token(route)
          ) ++
          counterplayDenialRoute.toVector.map(route =>
            "counterplay_denial_route" -> WitnessValue.Token(route)
          ) ++
          counterplaySurvivalRoute.toVector.map(route =>
            "counterplay_survival_route" -> WitnessValue.Token(route)
          ) ++
          outpostOccupationRoute.toVector.map(route =>
            "outpost_occupation_route" -> WitnessValue.Token(route)
          ) ++
          accessRoute.toVector.map(route => "access_route" -> WitnessValue.Token(route)) ++
          accessWitnessId.toVector.map(witnessId => "access_witness_id" -> WitnessValue.Token(witnessId)) ++
          localAccessSuperiority.toVector.map(value =>
            "local_access_superiority" -> WitnessValue.Token(value)
          ) ++
          weakOutpostSquare.toVector.map(square =>
            "weak_outpost_square" -> WitnessValue.SquareValue(parseSquare(square))
          ) ++
          weakOutpostState.toVector.map(state => "weak_outpost_state" -> WitnessValue.Token(state)) ++
          diagonalSourceSquare.toVector.map(square =>
            "diagonal_source_square" -> WitnessValue.SquareValue(parseSquare(square))
          ) ++
          Option.when(diagonalEndpointSquares.nonEmpty)(
            "diagonal_endpoint_squares" -> WitnessValue.SquareListValue(diagonalEndpointSquares.map(parseSquare).toVector)
          ).toVector ++
          dominationRoute.toVector.map(route => "domination_route" -> WitnessValue.Token(route)) ++
          supportWitnessId.toVector.map(witnessId => "support_witness_id" -> WitnessValue.Token(witnessId)) ++
          Option.when(supportTargetSquares.nonEmpty)(
            "support_target_squares" -> WitnessValue.SquareListValue(supportTargetSquares.map(parseSquare).toVector)
          ).toVector ++
          spaceBindRoute.toVector.map(route => "space_bind_route" -> WitnessValue.Token(route)) ++
          routeAnchorSquare.toVector.map(square =>
            "route_anchor_square" -> WitnessValue.SquareValue(parseSquare(square))
          ) ++
          structuralSector.toVector.map(sector => "structural_sector" -> WitnessValue.Token(sector)) ++
          structuralHostId.toVector.map(host => "structural_host_id" -> WitnessValue.Token(host)) ++
          outpostSquare.toVector.map(square => "outpost_square" -> WitnessValue.SquareValue(parseSquare(square))) ++
          restrictionAnchorSquare.toVector.map(square =>
            "restriction_anchor_square" -> WitnessValue.SquareValue(parseSquare(square))
          ) ++
          creationRoute.toVector.map(route => "creation_route" -> WitnessValue.Token(route)) ++
          suppressionRoute.toVector.map(route => "suppression_route" -> WitnessValue.Token(route)) ++
          passerSquare.toVector.map(square => "passer_square" -> WitnessValue.SquareValue(parseSquare(square))) ++
          blockerSquare.toVector.map(square => "blocker_square" -> WitnessValue.SquareValue(parseSquare(square))) ++
          damageRoute.toVector.map(route => "damage_route" -> WitnessValue.Token(route)) ++
          chainBaseRoute.toVector.map(route => "chain_base_route" -> WitnessValue.Token(route)) ++
          Option.when(chainBaseForwardSquares.nonEmpty)(
            "chain_base_forward_squares" -> WitnessValue.SquareListValue(chainBaseForwardSquares.map(parseSquare).toVector)
          ).toVector ++
          damageSector.toVector.map(sector => "damage_sector" -> WitnessValue.Token(sector)) ++
          pressureRoute.toVector.map(route => "pressure_route" -> WitnessValue.Token(route)) ++
          persistenceKind.toVector.map(kind => "persistence_kind" -> WitnessValue.Token(kind)) ++
          Option.when(pressureSourceSquares.nonEmpty)(
            "pressure_source_squares" -> WitnessValue.SquareListValue(pressureSourceSquares.map(parseSquare).toVector)
          ).toVector ++
          certificationFamily.toVector.map(family => "certification_family" -> WitnessValue.Token(family)) ++
          activeBishopSquare.toVector.map(square =>
            "active_bishop_square" -> WitnessValue.SquareValue(parseSquare(square))
          ) ++
          Option.when(bishopMemberSquares.nonEmpty)(
            "bishop_member_squares" -> WitnessValue.SquareListValue(bishopMemberSquares.map(parseSquare).toVector)
          ).toVector ++
          Option.when(conversionTargetSquares.nonEmpty)(
            "conversion_target_squares" -> WitnessValue.SquareListValue(conversionTargetSquares.map(parseSquare).toVector)
          ).toVector
      StrategyProjectionEvidenceClaim(
        bandId = row.validatedBand,
        kind = StrategyProjectionEvidenceKind(kind),
        owner = row.validatedOwner,
        anchor = parseAnchor(anchor),
        payload = WitnessPayload.from(payloadEntries)
      )

  final case class Row(
      id: String,
      caseType: String,
      fen: String,
      fenBefore: Option[String],
      playedMove: Option[String],
      fenAfter: Option[String],
      expectation: String,
      band: String,
      owner: String,
      anchor: String,
      requiredWitnessIds: List[String],
      requiredObjectFamilies: List[String],
      requiredDeltaFamilies: List[String],
      requiredCertificationFamilies: List[String],
      supportShellIds: List[String],
      optionalStrengtheningFamilies: List[String],
      supportWitnessIds: List[String],
      rivalBands: List[String],
      forbiddenShortcuts: List[String],
      coverageAxis: Option[String],
      coverageBucket: Option[String],
      admissionPath: String,
      requiredSupportSeedIds: List[String],
      evidenceClaims: List[EvidenceClaimRow],
      notes: Option[String]
  ):
    def normalizedFen: Fen.Full = Fen.Full.clean(fen)

    def validatedDeltaCompanion: Option[(Fen.Full, Uci.Move, Fen.Full)] =
      val fields = Vector(fenBefore, playedMove, fenAfter)
      require(
        fields.forall(_.isDefined) || fields.forall(_.isEmpty),
        s"Row $id must declare fenBefore, playedMove, and fenAfter together"
      )
      for
        before <- fenBefore
        move <- playedMove
        after <- fenAfter
      yield
        val validatedMove =
          Uci(move) match
            case Some(value: Uci.Move) => value
            case Some(other) =>
              throw IllegalArgumentException(s"Row $id expected move UCI but found $other")
            case None =>
              throw IllegalArgumentException(s"Row $id uses invalid playedMove $move")
        (Fen.Full.clean(before), validatedMove, Fen.Full.clean(after))

    def validatedCaseType: String =
      require(
        requiredCaseTypes.contains(caseType),
        s"Row $id uses unsupported caseType $caseType"
      )
      caseType

    def validatedExpectation: String =
      require(
        requiredExpectations.contains(expectation),
        s"Row $id uses unsupported expectation $expectation"
      )
      expectation

    def validatedBand: StrategyProjectionBandId =
      require(requiredBands.contains(band), s"Row $id uses unsupported band $band")
      StrategyProjectionBandId(band)

    def validatedOwner: Color =
      owner match
        case "white" => Color.White
        case "black" => Color.Black
        case other =>
          throw IllegalArgumentException(s"Row $id uses unsupported owner $other")

    def validatedAnchor: String =
      val normalized = anchor.trim
      require(normalized.nonEmpty, s"Row $id declares an empty anchor")
      normalized match
        case "board" => normalized
        case value if value.startsWith("square:") =>
          parseSquare(value.stripPrefix("square:"))
          normalized
        case value if value.startsWith("piece:") =>
          parseSquare(value.stripPrefix("piece:"))
          normalized
        case value if value.matches("^[a-z][a-z0-9_]*$") => normalized
        case other => throw IllegalArgumentException(s"Row $id uses invalid anchor $other")

    def validatedRequiredWitnessIds: Vector[String] =
      validatedTokenList("requiredWitnessIds", requiredWitnessIds)

    def validatedRequiredObjectFamilies: Vector[String] =
      validatedTokenList("requiredObjectFamilies", requiredObjectFamilies)

    def validatedRequiredDeltaFamilies: Vector[String] =
      validatedTokenList("requiredDeltaFamilies", requiredDeltaFamilies)

    def validatedRequiredCertificationFamilies: Vector[String] =
      val values = validatedTokenList("requiredCertificationFamilies", requiredCertificationFamilies)
      val projectionEvidenceLeaks = values.filter(projectionEvidenceKindNames.contains)
      require(
        projectionEvidenceLeaks.isEmpty,
        s"Row $id must keep projection evidence out of requiredCertificationFamilies: ${projectionEvidenceLeaks.mkString(", ")}"
      )
      values

    def validatedSupportShellIds: Vector[String] =
      validatedTokenList("supportShellIds", supportShellIds)

    def validatedOptionalStrengtheningFamilies: Vector[String] =
      validatedTokenList("optionalStrengtheningFamilies", optionalStrengtheningFamilies)

    def validatedSupportWitnessIds: Vector[String] =
      validatedTokenList("supportWitnessIds", supportWitnessIds)

    def validatedAdmissionPath: String =
      require(
        admissionPath.matches("^[a-z][a-z0-9_]*$"),
        s"Row $id uses invalid admissionPath $admissionPath"
      )
      admissionPath

    def validatedRequiredSupportSeedIds: Vector[StrategySupportSeedId] =
      val normalized = requiredSupportSeedIds.map(_.trim).filter(_.nonEmpty).toVector
      require(
        normalized.distinct == normalized,
        s"Row $id declares duplicate support seeds: ${normalized.mkString(", ")}"
      )
      if normalized.isEmpty then Vector.empty
      else
        val allowed =
          allowedSupportSeedIdsByBand.getOrElse(
            validatedBand.value,
            throw IllegalArgumentException(s"Row $id has no support-seed contract for $band")
          )
        require(
          normalized.forall(allowed.contains),
          s"Row $id uses out-of-band support seeds: ${normalized.filterNot(allowed.contains).mkString(", ")}"
        )
        normalized.map(StrategySupportSeedId.apply)

    def validatedEvidenceKinds: Vector[StrategyProjectionEvidenceKind] =
      val normalized = evidenceClaims.map(_.kind.trim).filter(_.nonEmpty).toVector
      require(
        normalized.distinct == normalized,
        s"Row $id declares duplicate evidence kinds: ${normalized.mkString(", ")}"
      )
      if normalized.isEmpty then
        if expectation == "admitted" && liveAdmissionBands.contains(validatedBand.value) then
          require(
            normalized.nonEmpty,
            s"Row $id admitted runtime projection rows must declare exact evidence"
          )
        Vector.empty
      else
        val allowed =
          requiredEvidenceKindsByBand.getOrElse(
            validatedBand.value,
            throw IllegalArgumentException(s"Row $id has no evidence-kind contract for $band")
          )
        require(
          normalized.forall(allowed.contains),
          s"Row $id uses out-of-band evidence kinds: ${normalized.filterNot(allowed.contains).mkString(", ")}"
        )
        if expectation == "admitted" then
          require(
            normalized.nonEmpty,
            s"Row $id admitted projection rows must declare exact evidence"
          )
        normalized.map(StrategyProjectionEvidenceKind.apply)

    def validatedRivalBands: Vector[String] =
      val normalized = rivalBands.map(_.trim).filter(_.nonEmpty).toVector
      require(
        normalized.distinct == normalized,
        s"Row $id declares duplicate rival bands: ${normalized.mkString(", ")}"
      )
      val expected =
        requiredForbiddenRivalBandsByBand.getOrElse(
          validatedBand.value,
          throw IllegalArgumentException(s"Row $id has no rival-band contract for $band")
        )
      if caseType == "comparative_false_rival" then
        require(
          normalized.toSet == expected,
          s"Row $id rival bands mismatch: expected ${expected.toVector.sorted.mkString(", ")} but found ${normalized.sorted.mkString(", ")}"
        )
      else
        require(
          normalized.forall(expected.contains),
          s"Row $id uses out-of-band rival bands: ${normalized.filterNot(expected.contains).mkString(", ")}"
        )
      normalized

    def validatedForbiddenShortcuts: Vector[String] =
      validatedTokenList("forbiddenShortcuts", forbiddenShortcuts)

    def validatedCoveragePair: Option[(String, String)] =
      val normalizedAxis = coverageAxis.map(_.trim).filter(_.nonEmpty)
      val normalizedBucket = coverageBucket.map(_.trim).filter(_.nonEmpty)
      require(
        normalizedAxis.isDefined == normalizedBucket.isDefined,
        s"Row $id must declare coverageAxis and coverageBucket together"
      )
      normalizedAxis.zip(normalizedBucket).headOption.map: (axis, bucket) =>
        require(
          axis.matches("^[a-z][a-z0-9_]*$"),
          s"Row $id uses invalid coverageAxis $axis"
        )
        require(
          bucket.matches("^[a-z][a-z0-9_]*$"),
          s"Row $id uses invalid coverageBucket $bucket"
        )
        val allowedPairs = requiredCoveragePairsFor(band)
        if allowedPairs.nonEmpty then
          require(
            countableCoverageAxes.contains(axis),
            s"Row $id uses unsupported coverage axis $axis for $band"
          )
          require(
            allowedPairs.contains(axis -> bucket),
            s"Row $id uses out-of-band coverage pair $axis/$bucket for $band"
          )
        axis -> bucket

    def countableCoveragePair: Option[(String, String)] =
      validatedCoveragePair.filter: (axis, _) =>
        val caseTypeValue = validatedCaseType
        val expectationValue = validatedExpectation
        if admittedCoverageAxes.contains(axis) then
          caseTypeValue == "exact" && expectationValue == "admitted"
        else if nearMissCoverageAxes.contains(axis) then
          Set("near_miss", "contrastive", "comparative_false_rival").contains(caseTypeValue) &&
            expectationValue == "rejected"
        else if shortcutNegativeCoverageAxes.contains(axis) then
          Set("nasty_negative", "negative").contains(caseTypeValue) &&
            expectationValue == "rejected"
        else false

    def evidenceClaimsForRuntime: Vector[StrategyProjectionEvidenceClaim] =
      validatedEvidenceKinds
      evidenceClaims.map(_.toRuntimeClaim(this)).toVector

    private def validatedTokenList(field: String, values: List[String]): Vector[String] =
      val normalized = values.map(_.trim).filter(_.nonEmpty).toVector
      require(
        normalized.distinct == normalized,
        s"Row $id declares duplicate $field entries: ${normalized.mkString(", ")}"
      )
      require(
        normalized.forall(_.matches("^[A-Za-z][A-Za-z0-9_]*$")),
        s"Row $id declares invalid $field entries: ${normalized.filterNot(_.matches("^[A-Za-z][A-Za-z0-9_]*$")).mkString(", ")}"
      )
      normalized

  private given Reads[EvidenceClaimRow] = Reads: json =>
    for
      kind <- (json \ "kind").validate[String]
      anchor <- (json \ "anchor").validate[String]
      reliefKind <- (json \ "reliefKind").validateOpt[String]
      corridorKind <- (json \ "corridorKind").validateOpt[String]
      entrySquare <- (json \ "entrySquare").validateOpt[String]
      contactSquare <- (json \ "contactSquare").validateOpt[String]
      targetSquare <- (json \ "targetSquare").validateOpt[String]
      sourceSquares <- (json \ "sourceSquares").validateOpt[List[String]]
      kingRingTargetSquares <- (json \ "kingRingTargetSquares").validateOpt[List[String]]
      contactSourceSquare <- (json \ "contactSourceSquare").validateOpt[String]
      defendingKingSquare <- (json \ "defendingKingSquare").validateOpt[String]
      shellAnchorSquare <- (json \ "shellAnchorSquare").validateOpt[String]
      breachSquares <- (json \ "breachSquares").validateOpt[List[String]]
      kingWingStormRoute <- (json \ "kingWingStormRoute").validateOpt[String]
      kingRingConcentrationRoute <- (json \ "kingRingConcentrationRoute").validateOpt[String]
      diagonalKingAttackRoute <- (json \ "diagonalKingAttackRoute").validateOpt[String]
      kingShelterBreachRoute <- (json \ "kingShelterBreachRoute").validateOpt[String]
      centerReleaseRoute <- (json \ "centerReleaseRoute").validateOpt[String]
      filePenetrationRoute <- (json \ "filePenetrationRoute").validateOpt[String]
      initiativeConversionRoute <- (json \ "initiativeConversionRoute").validateOpt[String]
      counterplayDenialRoute <- (json \ "counterplayDenialRoute").validateOpt[String]
      counterplaySurvivalRoute <- (json \ "counterplaySurvivalRoute").validateOpt[String]
      outpostOccupationRoute <- (json \ "outpostOccupationRoute").validateOpt[String]
      accessRoute <- (json \ "accessRoute").validateOpt[String]
      accessWitnessId <- (json \ "accessWitnessId").validateOpt[String]
      localAccessSuperiority <- (json \ "localAccessSuperiority").validateOpt[String]
      weakOutpostSquare <- (json \ "weakOutpostSquare").validateOpt[String]
      weakOutpostState <- (json \ "weakOutpostState").validateOpt[String]
      diagonalSourceSquare <- (json \ "diagonalSourceSquare").validateOpt[String]
      diagonalEndpointSquares <- (json \ "diagonalEndpointSquares").validateOpt[List[String]]
      dominationRoute <- (json \ "dominationRoute").validateOpt[String]
      supportWitnessId <- (json \ "supportWitnessId").validateOpt[String]
      supportTargetSquares <- (json \ "supportTargetSquares").validateOpt[List[String]]
      spaceBindRoute <- (json \ "spaceBindRoute").validateOpt[String]
      routeAnchorSquare <- (json \ "routeAnchorSquare").validateOpt[String]
      structuralSector <- (json \ "structuralSector").validateOpt[String]
      structuralHostId <- (json \ "structuralHostId").validateOpt[String]
      outpostSquare <- (json \ "outpostSquare").validateOpt[String]
      restrictionAnchorSquare <- (json \ "restrictionAnchorSquare").validateOpt[String]
      creationRoute <- (json \ "creationRoute").validateOpt[String]
      suppressionRoute <- (json \ "suppressionRoute").validateOpt[String]
      passerSquare <- (json \ "passerSquare").validateOpt[String]
      blockerSquare <- (json \ "blockerSquare").validateOpt[String]
      damageRoute <- (json \ "damageRoute").validateOpt[String]
      chainBaseRoute <- (json \ "chainBaseRoute").validateOpt[String]
      chainBaseForwardSquares <- (json \ "chainBaseForwardSquares").validateOpt[List[String]]
      damageSector <- (json \ "damageSector").validateOpt[String]
      pressureRoute <- (json \ "pressureRoute").validateOpt[String]
      persistenceKind <- (json \ "persistenceKind").validateOpt[String]
      pressureSourceSquares <- (json \ "pressureSourceSquares").validateOpt[List[String]]
      certificationFamily <- (json \ "certificationFamily").validateOpt[String]
      activeBishopSquare <- (json \ "activeBishopSquare").validateOpt[String]
      bishopMemberSquares <- (json \ "bishopMemberSquares").validateOpt[List[String]]
      conversionTargetSquares <- (json \ "conversionTargetSquares").validateOpt[List[String]]
    yield EvidenceClaimRow(
      kind = kind,
      anchor = anchor,
      reliefKind = reliefKind,
      corridorKind = corridorKind,
      entrySquare = entrySquare,
      contactSquare = contactSquare,
      targetSquare = targetSquare,
      sourceSquares = sourceSquares.getOrElse(Nil),
      kingRingTargetSquares = kingRingTargetSquares.getOrElse(Nil),
      contactSourceSquare = contactSourceSquare,
      defendingKingSquare = defendingKingSquare,
      shellAnchorSquare = shellAnchorSquare,
      breachSquares = breachSquares.getOrElse(Nil),
      kingWingStormRoute = kingWingStormRoute,
      kingRingConcentrationRoute = kingRingConcentrationRoute,
      diagonalKingAttackRoute = diagonalKingAttackRoute,
      kingShelterBreachRoute = kingShelterBreachRoute,
      centerReleaseRoute = centerReleaseRoute,
      filePenetrationRoute = filePenetrationRoute,
      initiativeConversionRoute = initiativeConversionRoute,
      counterplayDenialRoute = counterplayDenialRoute,
      counterplaySurvivalRoute = counterplaySurvivalRoute,
      outpostOccupationRoute = outpostOccupationRoute,
      accessRoute = accessRoute,
      accessWitnessId = accessWitnessId,
      localAccessSuperiority = localAccessSuperiority,
      weakOutpostSquare = weakOutpostSquare,
      weakOutpostState = weakOutpostState,
      diagonalSourceSquare = diagonalSourceSquare,
      diagonalEndpointSquares = diagonalEndpointSquares.getOrElse(Nil),
      dominationRoute = dominationRoute,
      supportWitnessId = supportWitnessId,
      supportTargetSquares = supportTargetSquares.getOrElse(Nil),
      spaceBindRoute = spaceBindRoute,
      routeAnchorSquare = routeAnchorSquare,
      structuralSector = structuralSector,
      structuralHostId = structuralHostId,
      outpostSquare = outpostSquare,
      restrictionAnchorSquare = restrictionAnchorSquare,
      creationRoute = creationRoute,
      suppressionRoute = suppressionRoute,
      passerSquare = passerSquare,
      blockerSquare = blockerSquare,
      damageRoute = damageRoute,
      chainBaseRoute = chainBaseRoute,
      chainBaseForwardSquares = chainBaseForwardSquares.getOrElse(Nil),
      damageSector = damageSector,
      pressureRoute = pressureRoute,
      persistenceKind = persistenceKind,
      pressureSourceSquares = pressureSourceSquares.getOrElse(Nil),
      certificationFamily = certificationFamily,
      activeBishopSquare = activeBishopSquare,
      bishopMemberSquares = bishopMemberSquares.getOrElse(Nil),
      conversionTargetSquares = conversionTargetSquares.getOrElse(Nil)
    )

  private given Reads[Row] = Reads: json =>
    for
      id <- (json \ "id").validate[String]
      caseType <- (json \ "caseType").validate[String]
      fen <- (json \ "fen").validate[String]
      fenBefore <- (json \ "fenBefore").validateOpt[String]
      playedMove <- (json \ "playedMove").validateOpt[String]
      fenAfter <- (json \ "fenAfter").validateOpt[String]
      expectation <- (json \ "expectation").validate[String]
      band <- (json \ "band").validate[String]
      owner <- (json \ "owner").validate[String]
      anchor <- (json \ "anchor").validate[String]
      requiredWitnessIds <- (json \ "requiredWitnessIds").validate[List[String]]
      requiredObjectFamilies <- (json \ "requiredObjectFamilies").validate[List[String]]
      requiredDeltaFamilies <- (json \ "requiredDeltaFamilies").validate[List[String]]
      requiredCertificationFamilies <- (json \ "requiredCertificationFamilies").validate[List[String]]
      supportShellIds <- (json \ "supportShellIds").validate[List[String]]
      optionalStrengtheningFamilies <- (json \ "optionalStrengtheningFamilies").validate[List[String]]
      supportWitnessIds <- (json \ "supportWitnessIds").validate[List[String]]
      rivalBands <- (json \ "rivalBands").validate[List[String]]
      forbiddenShortcuts <- (json \ "forbiddenShortcuts").validate[List[String]]
      coverageAxis <- (json \ "coverageAxis").validateOpt[String]
      coverageBucket <- (json \ "coverageBucket").validateOpt[String]
      admissionPath <- (json \ "admissionPath").validateOpt[String].map(_.getOrElse("unspecified"))
      requiredSupportSeedIds <- (json \ "requiredSupportSeedIds").validateOpt[List[String]].map(_.getOrElse(Nil))
      evidenceClaims <- (json \ "evidenceClaims").validateOpt[List[EvidenceClaimRow]].map(_.getOrElse(Nil))
      notes <- (json \ "notes").validateOpt[String]
    yield Row(
      id,
      caseType,
      fen,
      fenBefore,
      playedMove,
      fenAfter,
      expectation,
      band,
      owner,
      anchor,
      requiredWitnessIds,
      requiredObjectFamilies,
      requiredDeltaFamilies,
      requiredCertificationFamilies,
      supportShellIds,
      optionalStrengtheningFamilies,
      supportWitnessIds,
      rivalBands,
      forbiddenShortcuts,
      coverageAxis,
      coverageBucket,
      admissionPath,
      requiredSupportSeedIds,
      evidenceClaims,
      notes
    )

  def loadAll(): Vector[Row] =
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(resourcePath))
          .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath"))
      )
    try
      source
        .getLines()
        .filter(_.nonEmpty)
        .zipWithIndex
        .map: (line, index) =>
          Json.parse(line).validate[Row].fold(
            errors => throw IllegalArgumentException(s"Invalid projection row ${index + 1}: $errors"),
            identity
          )
        .toVector
    finally source.close()

  private def parseAnchor(value: String): WitnessAnchor =
    val normalized = value.trim
    normalized match
      case "board" => WitnessAnchor.BoardAnchor
      case square if square.startsWith("square:") =>
        WitnessAnchor.SquareAnchor(parseSquare(square.stripPrefix("square:")))
      case pieceSquare if pieceSquare.startsWith("piece:") =>
        WitnessAnchor.PieceSquareAnchor(parseSquare(pieceSquare.stripPrefix("piece:")))
      case other =>
        throw IllegalArgumentException(s"Unsupported projection evidence anchor $other")

  private def parseSquare(value: String): Square =
    Square
      .fromKey(value)
      .getOrElse(throw IllegalArgumentException(s"Invalid square key $value"))
