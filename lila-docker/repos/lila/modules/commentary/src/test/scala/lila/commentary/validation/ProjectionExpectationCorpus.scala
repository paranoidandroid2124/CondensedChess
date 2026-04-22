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

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

private[validation] object ProjectionExpectationCorpus:

  val resourcePath = "/commentary-corpus/projection-expectations.jsonl"

  val runtimeAdmissionBands: Set[String] =
    StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
  val closedLegacyBroadBlockerBands: Set[String] =
    Set("S06", "S10", "S12", "S15")
  val laterBroadReadyCoverageGateBands: Set[String] =
    Set.empty
  val stagedWave1BroadReadyCoverageGateBands: Set[String] =
    StrategyProjectionCoverageContract.foundationBroadReadyCoverageBandIds.map(_.value).toSet
  val stagedWave2BroadReadyCoverageGateBands: Set[String] =
    StrategyProjectionCoverageContract.broadCoverageCandidateBandIds.map(_.value).toSet
  val stagedWave3BroadReadyCoverageGateBands: Set[String] =
    StrategyProjectionCoverageContract.initiativeReleaseCoverageFreezeBandIds.map(_.value).toSet
  val stagedWave4BroadReadyCoverageGateBands: Set[String] =
    StrategyProjectionCoverageContract.kingAttackCoverageFreezeBandIds.map(_.value).toSet
  val stagedWave5BroadReadyCoverageGateBands: Set[String] =
    StrategyProjectionCoverageContract.pawnTargetStructuralDamageCoverageFreezeBandIds.map(_.value).toSet
  val stagedWave6BroadReadyCoverageGateBands: Set[String] =
    StrategyProjectionCoverageContract.passerCreationSuppressionCoverageFreezeBandIds.map(_.value).toSet
  val globalClosureBroadReadyCoverageGateBands: Set[String] =
    StrategyProjectionCoverageContract.globalClosureBroadReadyCoverageBandIds.map(_.value).toSet
  val broadReadyCoverageGateBands: Set[String] =
    stagedWave1BroadReadyCoverageGateBands ++
      stagedWave2BroadReadyCoverageGateBands ++
      stagedWave3BroadReadyCoverageGateBands ++
      stagedWave4BroadReadyCoverageGateBands ++
      stagedWave5BroadReadyCoverageGateBands ++
      stagedWave6BroadReadyCoverageGateBands
  val pendingCoverageFreezeGateBands: Set[String] =
    Set.empty
  val coverageFreezeGateBands: Set[String] =
    broadReadyCoverageGateBands ++ pendingCoverageFreezeGateBands
  val requiredBands: Vector[String] =
    (runtimeAdmissionBands ++ coverageFreezeGateBands).toVector.sorted
  val broadReadyCaseTypes: Set[String] =
    Set("exact", "near_miss", "contrastive", "comparative_false_rival", "nasty_negative", "negative")
  val requiredStartReadyCaseTypes: Set[String] =
    Set("exact", "near_miss", "nasty_negative", "comparative_false_rival")
  val requiredCaseTypes: Set[String] = broadReadyCaseTypes
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
    "S14" -> Set("S05", "S11", "S13"),
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

  final case class BroadReadyCoverageGate(axis: String, buckets: Set[String]):
    require(axis.matches("^[a-z][a-z0-9_]*$"), s"Invalid projection coverage axis $axis")
    require(buckets.nonEmpty, s"Projection coverage axis $axis must have at least one bucket")
    require(
      buckets.forall(_.matches("^[a-z][a-z0-9_]*$")),
      s"Invalid projection coverage buckets for $axis: ${buckets.toVector.sorted.mkString(", ")}"
    )

    def pairs: Set[(String, String)] = buckets.map(axis -> _)

  private val wave1BroadReadyCoverageGatesByBand: Map[String, Vector[BroadReadyCoverageGate]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(stagedWave1BroadReadyCoverageGateBands.contains)
      .mapValues(gates => gates.map(gate => BroadReadyCoverageGate(gate.axis, gate.buckets.toSet)))
      .toMap

  private val wave2BroadReadyCoverageGatesByBand: Map[String, Vector[BroadReadyCoverageGate]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(stagedWave2BroadReadyCoverageGateBands.contains)
      .mapValues(gates => gates.map(gate => BroadReadyCoverageGate(gate.axis, gate.buckets.toSet)))
      .toMap

  private val wave3BroadReadyCoverageGatesByBand: Map[String, Vector[BroadReadyCoverageGate]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(stagedWave3BroadReadyCoverageGateBands.contains)
      .mapValues(gates => gates.map(gate => BroadReadyCoverageGate(gate.axis, gate.buckets.toSet)))
      .toMap

  private val wave4BroadReadyCoverageGatesByBand: Map[String, Vector[BroadReadyCoverageGate]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(stagedWave4BroadReadyCoverageGateBands.contains)
      .mapValues(gates => gates.map(gate => BroadReadyCoverageGate(gate.axis, gate.buckets.toSet)))
      .toMap

  private val wave5BroadReadyCoverageGatesByBand: Map[String, Vector[BroadReadyCoverageGate]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(stagedWave5BroadReadyCoverageGateBands.contains)
      .mapValues(gates => gates.map(gate => BroadReadyCoverageGate(gate.axis, gate.buckets.toSet)))
      .toMap

  private val wave6BroadReadyCoverageGatesByBand: Map[String, Vector[BroadReadyCoverageGate]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(stagedWave6BroadReadyCoverageGateBands.contains)
      .mapValues(gates => gates.map(gate => BroadReadyCoverageGate(gate.axis, gate.buckets.toSet)))
      .toMap

  private val pendingCoverageFreezeGatesByBand: Map[String, Vector[BroadReadyCoverageGate]] =
    StrategyProjectionCoverageContract.coverageGatesByBand.view
      .filterKeys(pendingCoverageFreezeGateBands.contains)
      .mapValues(gates => gates.map(gate => BroadReadyCoverageGate(gate.axis, gate.buckets.toSet)))
      .toMap

  val broadReadyCoverageGatesByBand: Map[String, Vector[BroadReadyCoverageGate]] =
    wave1BroadReadyCoverageGatesByBand ++
      wave2BroadReadyCoverageGatesByBand ++
      wave3BroadReadyCoverageGatesByBand ++
      wave4BroadReadyCoverageGatesByBand ++
      wave5BroadReadyCoverageGatesByBand ++
      wave6BroadReadyCoverageGatesByBand
  val coverageFreezeGatesByBand: Map[String, Vector[BroadReadyCoverageGate]] =
    broadReadyCoverageGatesByBand ++ pendingCoverageFreezeGatesByBand

  require(
    broadReadyCoverageGatesByBand.keySet == broadReadyCoverageGateBands,
    "Projection broad-ready closure gates must match the executable gate-band set"
  )
  require(
    broadReadyCoverageGateBands == globalClosureBroadReadyCoverageGateBands,
    "Projection broad-ready global closure must match the staged executable gate-band set"
  )
  require(
    coverageFreezeGatesByBand.keySet == coverageFreezeGateBands,
    "Projection coverage freeze gates must match the executable freeze-band set"
  )

  def requiredCoveragePairsFor(band: String): Set[(String, String)] =
    coverageFreezeGatesByBand.getOrElse(band, Vector.empty).flatMap(_.pairs).toSet

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
    broadReadyCoverageGateBands.flatMap: band =>
      val missing = requiredCoveragePairsFor(band) -- present.getOrElse(band, Set.empty)
      Option.when(missing.nonEmpty)(band -> missing)
    .toMap

  def missingCoverageFreezePairsByBand(rows: Iterable[Row]): Map[String, Set[(String, String)]] =
    val present = coveragePairsByBand(rows)
    coverageFreezeGateBands.flatMap: band =>
      val missing = requiredCoveragePairsFor(band) -- present.getOrElse(band, Set.empty)
      Option.when(missing.nonEmpty)(band -> missing)
    .toMap

  def bandsWithCompleteBroadReadyCoverage(rows: Iterable[Row]): Set[String] =
    val missing = missingRequiredCoveragePairsByBand(rows).keySet
    broadReadyCoverageGateBands.filterNot(missing.contains)

  final case class EvidenceClaimRow(
      kind: String,
      anchor: String,
      reliefKind: Option[String],
      corridorKind: Option[String],
      entrySquare: Option[String]
  ):
    def toRuntimeClaim(row: Row): StrategyProjectionEvidenceClaim =
      val payloadEntries =
        reliefKind.toVector.map(kind => "relief_kind" -> WitnessValue.Token(kind)) ++
          corridorKind.toVector.map(kind => "corridor_kind" -> WitnessValue.Token(kind)) ++
          entrySquare.toVector.map(square => "entry_square" -> WitnessValue.SquareValue(parseSquare(square)))
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
        if expectation == "admitted" && runtimeAdmissionBands.contains(validatedBand.value) then
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

  private given Reads[EvidenceClaimRow] =
    (
      (__ \ "kind").read[String] and
        (__ \ "anchor").read[String] and
        (__ \ "reliefKind").readNullable[String] and
        (__ \ "corridorKind").readNullable[String] and
        (__ \ "entrySquare").readNullable[String]
    )(EvidenceClaimRow.apply)

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
