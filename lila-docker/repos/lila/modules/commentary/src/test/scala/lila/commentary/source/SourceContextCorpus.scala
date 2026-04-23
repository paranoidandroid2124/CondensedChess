package lila.commentary.source

import chess.format.Fen

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

enum SourceFamilyStatus:
  case Active, Deferred, Rejected

private[source] object SourceContextCorpus:

  private val manifestPath = "/commentary-corpus/opening-sources.jsonl"
  private val openingLinePath = "/commentary-corpus/opening-lines.jsonl"
  private val openingPositionPath = "/commentary-corpus/opening-positions.jsonl"
  private val openingMoveStatPath = "/commentary-corpus/opening-move-stats.jsonl"
  private val openingThemePath = "/commentary-corpus/opening-themes.jsonl"
  private val motifExamplePath = "/commentary-corpus/motif-examples.jsonl"
  private val motifRejectPath = "/commentary-corpus/motif-reject-fixtures.jsonl"
  private val endgameStudyPath = "/commentary-corpus/endgame-studies.jsonl"
  private val endgameStudyFixturePath = "/commentary-corpus/endgame-study-fixtures.jsonl"
  private val endgameStudyRejectPath = "/commentary-corpus/endgame-study-reject-fixtures.jsonl"
  private val retrievalExamplePath = "/commentary-corpus/retrieval-examples.jsonl"

  val resourceFileNames: Vector[String] = Vector(
    "opening-sources.jsonl",
    "opening-lines.jsonl",
    "opening-positions.jsonl",
    "opening-move-stats.jsonl",
    "opening-themes.jsonl",
    "motif-examples.jsonl",
    "motif-reject-fixtures.jsonl",
    "endgame-studies.jsonl",
    "endgame-study-fixtures.jsonl",
    "endgame-study-reject-fixtures.jsonl",
    "retrieval-examples.jsonl"
  )

  private val activeFamilies = Set("opening", "motif", "endgameStudy", "retrieval")
  private val allowedLicenses = Set("CC0-1.0", "CC-BY-SA-4.0", "public-domain-facts")
  private val allowedRedistribution = Set("allowed", "derived_only")
  private val allowedDerivedData = Set("allowed")
  private val allowedSourceTypes = Set(
    "public_data_aggregate",
    "curated_public_facts",
    "derived_example_index"
  )
  private val allowedOpeningAuthorities = Set("opening_reference", "opening_context", "opening_statistic")
  private val allowedMotifAuthorities = Set("motif_detector", "motif_example")
  private val allowedEndgameAuthorities = Set("endgame_study_context")
  private val allowedRetrievalAuthorities = Set("retrieval_example")
  private val allowedCandidateKinds = Set("statistical_reference", "context_reference")
  private val forbiddenCandidateWords = Set("be" + "st", "theory", "truth")
  private val allowedStudyMaterialClasses = Set(
    "rook_pawn_vs_rook",
    "king_pawn_vs_king",
    "wrong_rook_pawn",
    "fortress_pattern"
  )
  private val resultWords = Set("win", "draw", "loss", "won", "lost", "forced", "conversion", "convert")
  private val truthBearingLabels = Set("current_position_truth", "current_position_claim", "position_truth")
  private val retrievalFields = Set(
    "id",
    "sourceId",
    "sourceRef",
    "fen",
    "similarityKey",
    "similarityScore",
    "snippetRole",
    "currentPositionClaim",
    "authority",
    "tags"
  )

  def sourceFamilyStatus(sourceFamily: String): SourceFamilyStatus =
    if activeFamilies.contains(sourceFamily) then SourceFamilyStatus.Active
    else SourceFamilyStatus.Rejected

  final case class SourceManifestRow(
      sourceId: String,
      sourceFamily: String,
      sourceType: String,
      license: String,
      redistribution: String,
      derivedData: String,
      attributionRequired: Boolean,
      sourceUrl: String,
      notes: Option[String]
  ):
    def validatedSourceId: String =
      requireToken("Source", sourceId, "sourceId")

    def validatedSourceFamily: String =
      sourceFamilyStatus(sourceFamily) match
        case SourceFamilyStatus.Active => sourceFamily
        case SourceFamilyStatus.Deferred =>
          throw IllegalArgumentException(s"Source $sourceId sourceFamily $sourceFamily is deferred in this contract")
        case SourceFamilyStatus.Rejected =>
          throw IllegalArgumentException(s"Source $sourceId sourceFamily $sourceFamily is unsupported")

    def validatedSourceType: String =
      require(
        allowedSourceTypes.contains(sourceType),
        s"Source $sourceId uses unsupported sourceType $sourceType"
      )
      sourceType

    def validatedLicense: String =
      ensure(
        allowedLicenses.contains(license),
        s"Source $sourceId uses unsupported license $license"
      )
      license

    def validatedRedistribution: String =
      ensure(
        allowedRedistribution.contains(redistribution),
        s"Source $sourceId uses unsupported redistribution $redistribution"
      )
      redistribution

    def validatedDerivedData: String =
      ensure(
        allowedDerivedData.contains(derivedData),
        s"Source $sourceId uses unsupported derivedData $derivedData"
      )
      derivedData

    def validatedAttribution: Boolean =
      if license == "CC-BY-SA-4.0" then
        require(attributionRequired, s"Source $sourceId must require attribution for $license")
      attributionRequired

    def validatedSourceUrl: String =
      ensure(sourceUrl.trim.nonEmpty, s"Source $sourceId must declare a provenance URL")
      require(
        sourceUrl.startsWith("https://") || sourceUrl.startsWith("local-curation:"),
        s"Source $sourceId uses unsupported provenance URL $sourceUrl"
      )
      sourceUrl

  final case class OpeningLine(
      id: String,
      sourceId: String,
      eco: String,
      family: String,
      variation: String,
      names: List[String],
      moveOrder: List[String],
      lineKey: String,
      authority: String,
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "opening", manifests)

    def validatedEco: String =
      require(eco.matches("^[A-E][0-9]{2}$"), s"Opening line $id uses invalid ECO $eco")
      eco

    def validatedName: String =
      require(names.nonEmpty, s"Opening line $id must declare at least one name")
      require(names.forall(_.trim.nonEmpty), s"Opening line $id has an empty opening name")
      require(family.trim.nonEmpty, s"Opening line $id must declare family")
      require(variation.trim.nonEmpty, s"Opening line $id must declare variation")
      names.head

    def validatedMoveOrder: Vector[String] =
      validateMoveOrder("Opening line", id, moveOrder)

    def validatedAuthority: String =
      require(
        allowedOpeningAuthorities.contains(authority) && authority == "opening_reference",
        s"Opening line $id uses unsupported authority $authority"
      )
      authority

    def validatedNegativeBoundaries: Vector[String] =
      validateTokenList("Opening line", id, "negativeBoundaries", negativeBoundaries)

  final case class OpeningPosition(
      id: String,
      sourceId: String,
      lineId: String,
      eco: String,
      name: String,
      positionKey: String,
      fen: String,
      ply: Int,
      moveOrder: List[String],
      transpositionState: String,
      status: String,
      authority: String,
      contextRefs: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "opening", manifests)

    def validatedLineId(lines: Iterable[OpeningLine]): String =
      val line = lines.find(_.id == lineId).getOrElse(
        throw IllegalArgumentException(s"Opening position $id references unknown lineId $lineId")
      )
      ensure(
        line.eco == eco && line.names.contains(name),
        s"Opening position $id metadata must match lineId $lineId"
      )
      lineId

    def validatedPositionKey: String =
      ensure(positionKey.trim.nonEmpty, s"Opening position $id must declare normalized positionKey")
      require(positionKey.startsWith("std:"), s"Opening position $id uses unsupported positionKey $positionKey")
      ensure(positionKey == positionKeyFromFen(fen), s"Opening position $id positionKey must match normalized FEN key")
      positionKey

    def validatedFen: Fen.Full =
      Fen.Full.clean(fen)

    def validatedMoveOrder: Vector[String] =
      validateMoveOrder("Opening position", id, moveOrder)

    def validatedTransposition: String =
      require(
        Set("canonical", "transposed", "ambiguous").contains(transpositionState),
        s"Opening position $id uses unsupported transpositionState $transpositionState"
      )
      require(Set("indexed", "downgraded").contains(status), s"Opening position $id uses unsupported status $status")
      if transpositionState == "ambiguous" then
        ensure(status == "downgraded", s"Opening position $id ambiguous transpositions must be downgraded")
      status

    def validatedAuthority: String =
      require(
        allowedOpeningAuthorities.contains(authority) && authority == "opening_context",
        s"Opening position $id uses unsupported authority $authority"
      )
      authority

  final case class OpeningMoveStat(
      id: String,
      sourceId: String,
      positionKey: String,
      move: String,
      sampleSize: Int,
      whiteWins: Int,
      draws: Int,
      blackWins: Int,
      frequency: Double,
      candidateKind: String,
      authority: String,
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "opening", manifests)

    def validatedPositionKey(positions: Iterable[OpeningPosition]): String =
      ensure(
        positions.exists(position => position.positionKey == positionKey && position.status == "indexed"),
        s"Opening move stat $id references no indexed positionKey"
      )
      positionKey

    def validatedMove: String =
      requireUciMove("Opening move stat", id, move)

    def validatedCounts: Int =
      require(sampleSize > 0, s"Opening move stat $id must have positive sampleSize")
      require(
        whiteWins >= 0 && draws >= 0 && blackWins >= 0,
        s"Opening move stat $id cannot use negative result counts"
      )
      require(
        whiteWins + draws + blackWins == sampleSize,
        s"Opening move stat $id result counts must sum to sampleSize"
      )
      require(frequency >= 0.0 && frequency <= 1.0, s"Opening move stat $id frequency out of range")
      sampleSize

    def validatedAuthority: String =
      require(
        allowedOpeningAuthorities.contains(authority) && authority == "opening_statistic",
        s"Opening move stat $id uses unsupported authority $authority"
      )
      authority

    def validatedCandidateKind: String =
      val normalized = candidateKind.toLowerCase
      if forbiddenCandidateWords.exists(normalized.contains) then
        throw IllegalArgumentException(s"Opening move stat $id uses forbidden candidateKind")
      require(
        allowedCandidateKinds.contains(candidateKind),
        s"Opening move stat $id uses unsupported candidateKind $candidateKind"
      )
      candidateKind

  final case class OpeningTheme(
      id: String,
      sourceId: String,
      positionKey: String,
      themeId: String,
      family: String,
      contextTags: List[String],
      planRefs: List[String],
      breakRefs: List[String],
      authority: String,
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "opening", manifests)

    def validatedPositionKey(positions: Iterable[OpeningPosition]): String =
      ensure(
        positions.exists(position => position.positionKey == positionKey && position.status == "indexed"),
        s"Opening theme $id references no indexed positionKey"
      )
      positionKey

    def validatedTheme: String =
      requireToken("Opening theme", themeId, "themeId")
      validateTokenList("Opening theme", id, "contextTags", contextTags)
      validateTokenList("Opening theme", id, "planRefs", planRefs)
      validateTokenList("Opening theme", id, "breakRefs", breakRefs)
      themeId

    def validatedAuthority: String =
      require(
        allowedOpeningAuthorities.contains(authority) && authority == "opening_context",
        s"Opening theme $id uses unsupported authority $authority"
      )
      authority

  final case class DetectorCarrier(descriptorId: String, anchor: String, carrierKind: String):
    def validated(rowId: String, motifId: String): DetectorCarrier =
      requireToken("Motif detector", descriptorId, "descriptorId")
      require(anchor.trim.nonEmpty, s"Motif example $rowId detector carrier must declare anchor")
      require(
        Set("u_witness", "root_atom", "certified_line").contains(carrierKind),
        s"Motif example $rowId uses unsupported detector carrierKind $carrierKind"
      )
      val allowedCarriers =
        motifId match
          case "hanging_piece" => Set("loose_piece_target_state")
          case other          => Set(other)
      ensure(
        allowedCarriers.contains(descriptorId),
        s"Motif example $rowId detector carrier must match motifId $motifId"
      )
      this

  final case class MotifExample(
      id: String,
      sourceId: String,
      motifId: String,
      fen: String,
      detectorCarrier: Option[DetectorCarrier],
      involvedSquares: List[String],
      sourceTags: List[String],
      authority: String,
      currentPositionClaim: Boolean,
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "motif", manifests)

    def validatedMotifId: String =
      requireToken("Motif example", motifId, "motifId")
      ensure(!isTruthBearingLabel(motifId), s"Motif example $id motifId contains truth-bearing label")
      motifId

    def validatedFen: Fen.Full =
      Fen.Full.clean(fen)

    def validatedDetectorCarrier: DetectorCarrier =
      validatedMotifId
      detectorCarrier
        .getOrElse(throw IllegalArgumentException(s"Motif example $id must bind an exact-board detector carrier"))
        .validated(id, motifId)

    def validatedSourceTags: Vector[String] =
      val tags = validateTokenList("Motif example", id, "sourceTags", sourceTags)
      ensure(
        !tags.exists(isTruthBearingLabel),
        s"Motif example $id sourceTags contain truth-bearing labels"
      )
      tags

    def validatedAuthority: String =
      require(allowedMotifAuthorities.contains(authority), s"Motif example $id uses unsupported authority $authority")
      authority

    def validatedCurrentPositionBoundary: Boolean =
      require(!currentPositionClaim, s"Motif example $id must not become a current-position claim")
      currentPositionClaim

  final case class MotifRejectFixture(
      id: String,
      sourceId: String,
      motifId: String,
      fen: String,
      sourceTags: List[String],
      expectation: String,
      rejectReason: String,
      negativeBoundaries: List[String]
  ):
    def validatedRejectReason: String =
      require(expectation == "rejected", s"Motif reject fixture $id must be rejected")
      requireToken("Motif reject fixture", rejectReason, "rejectReason")

    def asExampleCandidate: MotifExample =
      MotifExample(
        id = id,
        sourceId = sourceId,
        motifId = motifId,
        fen = fen,
        detectorCarrier = None,
        involvedSquares = Nil,
        sourceTags = sourceTags,
        authority = "motif_example",
        currentPositionClaim = false,
        negativeBoundaries = negativeBoundaries
      )

  final case class EndgameStudy(
      studyId: String,
      names: List[String],
      materialClass: String,
      sideToMove: List[String],
      placementRules: List[String],
      relationRules: List[String],
      allowedTransforms: List[String],
      candidatePlans: List[String],
      sourceRefs: List[String],
      authority: String,
      outcomeClaim: String,
      negativeBoundaries: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): Vector[String] =
      sourceRefs.map(ref => requireSource(ref, "endgameStudy", manifests)).toVector

    def validatedStudyId: String =
      requireToken("Endgame study", studyId, "studyId")

    def validatedNames: Vector[String] =
      val values = names.map(_.trim).filter(_.nonEmpty).toVector
      require(values.nonEmpty, s"Endgame study $studyId must declare at least one name")
      values

    def validatedMaterialClass: String =
      require(
        allowedStudyMaterialClasses.contains(materialClass),
        s"Endgame study $studyId uses unsupported materialClass $materialClass"
      )
      materialClass

    def validatedSideToMove: Vector[String] =
      val values = validateTokenList("Endgame study", studyId, "sideToMove", sideToMove)
      require(
        values.forall(Set("stronger", "defender", "white", "black").contains),
        s"Endgame study $studyId uses unsupported sideToMove ${values.mkString(", ")}"
      )
      values

    def validatedPlacementRules: Vector[String] =
      val placement = validateTokenList("Endgame study", studyId, "placementRules", placementRules)
      val relations = validateTokenList("Endgame study", studyId, "relationRules", relationRules)
      if studyId.toLowerCase.contains("lucena") then
        val requiredPlacement = Set("strong_king_near_pawn", "pawn_on_seventh", "defender_king_cut_off")
        val requiredRelations = Set("rook_check_shelter")
        ensure(
          requiredPlacement.subsetOf(placement.toSet) && requiredRelations.subsetOf(relations.toSet),
          s"Endgame study $studyId Lucena context lacks required applicability rules"
        )
      if studyId.toLowerCase.contains("philidor") then
        val requiredPlacement = Set("defender_king_in_front", "attacker_pawn_not_on_sixth")
        val requiredRelations = Set("defender_rook_on_third_rank")
        ensure(
          requiredPlacement.subsetOf(placement.toSet) && requiredRelations.subsetOf(relations.toSet),
          s"Endgame study $studyId Philidor context lacks required defender relation rules"
        )
      if studyId.toLowerCase.contains("vancura") then
        val requiredPlacement = Set("rook_pawn_on_sixth_or_seventh", "defender_king_near_corner")
        val requiredRelations = Set("defender_rook_lateral_checking_file")
        ensure(
          requiredPlacement.subsetOf(placement.toSet) && requiredRelations.subsetOf(relations.toSet),
          s"Endgame study $studyId Vancura context lacks required applicability rules"
        )
      placement

    def validatedCandidatePlans: Vector[String] =
      val values = validateTokenList("Endgame study", studyId, "candidatePlans", candidatePlans)
      val leaks = values.filter(value => resultWords.exists(word => value.toLowerCase.contains(word)))
      ensure(leaks.isEmpty, s"Endgame study $studyId candidatePlans contain result claims: ${leaks.mkString(", ")}")
      values

    def validatedContextBoundary: String =
      require(
        allowedEndgameAuthorities.contains(authority) && authority == "endgame_study_context",
        s"Endgame study $studyId uses unsupported authority $authority"
      )
      ensure(outcomeClaim == "none", s"Endgame study $studyId must not declare outcomeClaim $outcomeClaim")
      outcomeClaim

    def validatedNegativeBoundaries: Vector[String] =
      validateTokenList("Endgame study", studyId, "negativeBoundaries", negativeBoundaries)

  final case class EndgameStudyFixture(
      id: String,
      studyId: String,
      fen: String,
      materialClass: String,
      sideToMove: String,
      placementEvidence: List[String],
      relationEvidence: List[String],
      contextStatus: String,
      outcomeClaim: String,
      sourceRefs: List[String]
  ):
    def validatedStudyId(studies: Iterable[EndgameStudy]): String =
      require(studies.exists(_.studyId == studyId), s"Endgame fixture $id references unknown studyId $studyId")
      studyId

    def validatedFen: Fen.Full =
      Fen.Full.clean(fen)

    def validatedMaterialClass(studies: Iterable[EndgameStudy]): String =
      val study = findStudy(studies)
      require(materialClass == study.materialClass, s"Endgame fixture $id materialClass mismatch")
      require(materialClassFromFen(fen) == materialClass, s"Endgame fixture $id FEN does not match $materialClass")
      materialClass

    def validatedApplicability(studies: Iterable[EndgameStudy]): String =
      val study = findStudy(studies)
      require(contextStatus == "matches", s"Endgame fixture $id must be a matching fixture")
      require(
        study.placementRules.toSet.subsetOf(placementEvidence.toSet),
        s"Endgame fixture $id missing placement evidence for $studyId"
      )
      require(
        study.relationRules.toSet.subsetOf(relationEvidence.toSet),
        s"Endgame fixture $id missing relation evidence for $studyId"
      )
      val boardEvidence = EndgameBoardEvidence.fromFen(fen)
      ensure(
        study.placementRules.toSet.subsetOf(boardEvidence.placementEvidence),
        s"Endgame fixture $id placement evidence does not match exact board"
      )
      ensure(
        study.relationRules.toSet.subsetOf(boardEvidence.relationEvidence),
        s"Endgame fixture $id relation evidence does not match exact board"
      )
      contextStatus

    def validatedContextBoundary: String =
      require(outcomeClaim == "none", s"Endgame fixture $id must not declare outcomeClaim $outcomeClaim")
      outcomeClaim

    private def findStudy(studies: Iterable[EndgameStudy]): EndgameStudy =
      studies.find(_.studyId == studyId).getOrElse(throw IllegalArgumentException(s"Unknown studyId $studyId"))

  final case class EndgameStudyRejectFixture(
      id: String,
      studyId: String,
      fen: String,
      materialClass: String,
      expectation: String,
      rejectReason: String,
      negativeBoundaries: List[String]
  ):
    def validatedStudyId(studies: Iterable[EndgameStudy]): String =
      require(studies.exists(_.studyId == studyId), s"Endgame reject fixture $id references unknown studyId $studyId")
      studyId

    def validatedRejectReason: String =
      require(expectation == "rejected", s"Endgame reject fixture $id must be rejected")
      requireToken("Endgame reject fixture", rejectReason, "rejectReason")

    def validatedApplicabilityRejected(studies: Iterable[EndgameStudy]): String =
      val study = studies.find(_.studyId == studyId).getOrElse(
        throw IllegalArgumentException(s"Endgame reject fixture $id references unknown studyId $studyId")
      )
      val boardEvidence = EndgameBoardEvidence.fromFen(fen)
      ensure(
        !study.placementRules.toSet.subsetOf(boardEvidence.placementEvidence) ||
          !study.relationRules.toSet.subsetOf(boardEvidence.relationEvidence),
        s"Endgame reject fixture $id unexpectedly satisfies study applicability"
      )
      rejectReason

  final case class RetrievalExample(
      id: String,
      sourceId: String,
      sourceRef: String,
      fen: String,
      similarityKey: JsObject,
      similarityScore: Double,
      snippetRole: String,
      currentPositionClaim: Boolean,
      authority: String,
      tags: List[String]
  ):
    def validatedSourceId(manifests: Iterable[SourceManifestRow]): String =
      requireSource(sourceId, "retrieval", manifests)

    def validatedFen: Fen.Full =
      Fen.Full.clean(fen)

    def validatedSimilarityKey: JsObject =
      require(similarityKey.keys.nonEmpty, s"Retrieval example $id must declare similarityKey")
      similarityKey

    def validatedSourceRef: String =
      ensure(sourceRef.trim.nonEmpty, s"Retrieval example $id must declare sourceRef")
      require(
        sourceRef.matches("^[A-Za-z][A-Za-z0-9_:-]*$"),
        s"Retrieval example $id uses invalid sourceRef $sourceRef"
      )
      sourceRef

    def validatedSimilarityScore: Double =
      require(
        similarityScore >= 0.0 && similarityScore <= 1.0,
        s"Retrieval example $id similarityScore out of range"
      )
      similarityScore

    def validatedAuthority: String =
      require(
        allowedRetrievalAuthorities.contains(authority) && authority == "retrieval_example",
        s"Retrieval example $id uses unsupported authority $authority"
      )
      authority

    def validatedCurrentPositionBoundary: Boolean =
      ensure(
        !currentPositionClaim && snippetRole == "non_authoritative_context",
        s"Retrieval example $id must remain non-authoritative"
      )
      currentPositionClaim

    def validatedTags: Vector[String] =
      val values = validateTokenList("Retrieval example", id, "tags", tags)
      ensure(
        !values.exists(isTruthBearingLabel),
        s"Retrieval example $id tags contain truth-bearing labels"
      )
      values

  private given Reads[SourceManifestRow] =
    (
      (__ \ "sourceId").read[String] and
        (__ \ "sourceFamily").read[String] and
        (__ \ "sourceType").read[String] and
        (__ \ "license").read[String] and
        (__ \ "redistribution").read[String] and
        (__ \ "derivedData").read[String] and
        (__ \ "attributionRequired").read[Boolean] and
        (__ \ "sourceUrl").read[String] and
        (__ \ "notes").readNullable[String]
    )(SourceManifestRow.apply)

  private given Reads[OpeningLine] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "eco").read[String] and
        (__ \ "family").read[String] and
        (__ \ "variation").read[String] and
        (__ \ "names").read[List[String]] and
        (__ \ "moveOrder").read[List[String]] and
        (__ \ "lineKey").read[String] and
        (__ \ "authority").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(OpeningLine.apply)

  private given Reads[OpeningPosition] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "lineId").read[String] and
        (__ \ "eco").read[String] and
        (__ \ "name").read[String] and
        (__ \ "positionKey").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "ply").read[Int] and
        (__ \ "moveOrder").read[List[String]] and
        (__ \ "transpositionState").read[String] and
        (__ \ "status").read[String] and
        (__ \ "authority").read[String] and
        (__ \ "contextRefs").read[List[String]]
    )(OpeningPosition.apply)

  private given Reads[OpeningMoveStat] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "positionKey").read[String] and
        (__ \ "move").read[String] and
        (__ \ "sampleSize").read[Int] and
        (__ \ "whiteWins").read[Int] and
        (__ \ "draws").read[Int] and
        (__ \ "blackWins").read[Int] and
        (__ \ "frequency").read[Double] and
        (__ \ "candidateKind").read[String] and
        (__ \ "authority").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(OpeningMoveStat.apply)

  private given Reads[OpeningTheme] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "positionKey").read[String] and
        (__ \ "themeId").read[String] and
        (__ \ "family").read[String] and
        (__ \ "contextTags").read[List[String]] and
        (__ \ "planRefs").read[List[String]] and
        (__ \ "breakRefs").read[List[String]] and
        (__ \ "authority").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(OpeningTheme.apply)

  private given Reads[DetectorCarrier] =
    (
      (__ \ "descriptorId").read[String] and
        (__ \ "anchor").read[String] and
        (__ \ "carrierKind").read[String]
    )(DetectorCarrier.apply)

  private given Reads[MotifExample] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "motifId").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "detectorCarrier").readNullable[DetectorCarrier] and
        (__ \ "involvedSquares").readWithDefault[List[String]](Nil) and
        (__ \ "sourceTags").readWithDefault[List[String]](Nil) and
        (__ \ "authority").read[String] and
        (__ \ "currentPositionClaim").read[Boolean] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(MotifExample.apply)

  private given Reads[MotifRejectFixture] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "motifId").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "sourceTags").readWithDefault[List[String]](Nil) and
        (__ \ "expectation").read[String] and
        (__ \ "rejectReason").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(MotifRejectFixture.apply)

  private given Reads[EndgameStudy] =
    (
      (__ \ "studyId").read[String] and
        (__ \ "names").read[List[String]] and
        (__ \ "materialClass").read[String] and
        (__ \ "sideToMove").read[List[String]] and
        (__ \ "placementRules").read[List[String]] and
        (__ \ "relationRules").read[List[String]] and
        (__ \ "allowedTransforms").read[List[String]] and
        (__ \ "candidatePlans").read[List[String]] and
        (__ \ "sourceRefs").read[List[String]] and
        (__ \ "authority").read[String] and
        (__ \ "outcomeClaim").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(EndgameStudy.apply)

  private given Reads[EndgameStudyFixture] =
    (
      (__ \ "id").read[String] and
        (__ \ "studyId").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "materialClass").read[String] and
        (__ \ "sideToMove").read[String] and
        (__ \ "placementEvidence").read[List[String]] and
        (__ \ "relationEvidence").read[List[String]] and
        (__ \ "contextStatus").read[String] and
        (__ \ "outcomeClaim").read[String] and
        (__ \ "sourceRefs").read[List[String]]
    )(EndgameStudyFixture.apply)

  private given Reads[EndgameStudyRejectFixture] =
    (
      (__ \ "id").read[String] and
        (__ \ "studyId").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "materialClass").read[String] and
        (__ \ "expectation").read[String] and
        (__ \ "rejectReason").read[String] and
        (__ \ "negativeBoundaries").read[List[String]]
    )(EndgameStudyRejectFixture.apply)

  private given Reads[RetrievalExample] =
    (
      (__ \ "id").read[String] and
        (__ \ "sourceId").read[String] and
        (__ \ "sourceRef").read[String] and
        (__ \ "fen").read[String] and
        (__ \ "similarityKey").read[JsObject] and
        (__ \ "similarityScore").read[Double] and
        (__ \ "snippetRole").read[String] and
        (__ \ "currentPositionClaim").read[Boolean] and
        (__ \ "authority").read[String] and
        (__ \ "tags").read[List[String]]
    )(RetrievalExample.apply)

  def parseManifest(json: JsValue): SourceManifestRow = parseJson[SourceManifestRow](json, "source manifest")
  def parseOpeningPosition(json: JsValue): OpeningPosition = parseJson[OpeningPosition](json, "opening position")
  def parseOpeningMoveStat(json: JsValue): OpeningMoveStat = parseJson[OpeningMoveStat](json, "opening move stat")
  def parseMotifExample(json: JsValue): MotifExample = parseJson[MotifExample](json, "motif example")
  def parseEndgameStudy(json: JsValue): EndgameStudy = parseJson[EndgameStudy](json, "endgame study")
  def parseRetrievalExample(json: JsValue): RetrievalExample =
    rejectUnknownFields(json, retrievalFields, "Retrieval example")
    parseJson[RetrievalExample](json, "retrieval example")

  def loadManifests(): Vector[SourceManifestRow] = loadJsonl[SourceManifestRow](manifestPath, "source manifest")
  def loadOpeningLines(): Vector[OpeningLine] = loadJsonl[OpeningLine](openingLinePath, "opening line")
  def loadOpeningPositions(): Vector[OpeningPosition] = loadJsonl[OpeningPosition](openingPositionPath, "opening position")
  def loadOpeningMoveStats(): Vector[OpeningMoveStat] = loadJsonl[OpeningMoveStat](openingMoveStatPath, "opening move stat")
  def loadOpeningThemes(): Vector[OpeningTheme] = loadJsonl[OpeningTheme](openingThemePath, "opening theme")
  def loadMotifExamples(): Vector[MotifExample] = loadJsonl[MotifExample](motifExamplePath, "motif example")
  def loadMotifRejectFixtures(): Vector[MotifRejectFixture] = loadJsonl[MotifRejectFixture](motifRejectPath, "motif reject")
  def loadEndgameStudies(): Vector[EndgameStudy] = loadJsonl[EndgameStudy](endgameStudyPath, "endgame study")
  def loadEndgameStudyFixtures(): Vector[EndgameStudyFixture] =
    loadJsonl[EndgameStudyFixture](endgameStudyFixturePath, "endgame study fixture")
  def loadEndgameStudyRejectFixtures(): Vector[EndgameStudyRejectFixture] =
    loadJsonl[EndgameStudyRejectFixture](endgameStudyRejectPath, "endgame study reject")
  def loadRetrievalExamples(): Vector[RetrievalExample] =
    loadJsonValues(retrievalExamplePath, "retrieval example").map(parseRetrievalExample)

  def resourceExists(fileName: String): Boolean =
    Option(getClass.getResourceAsStream(s"/commentary-corpus/$fileName")).exists: stream =>
      stream.close()
      true

  private def parseJson[A: Reads](json: JsValue, label: String): A =
    json.validate[A].fold(
      errors => throw IllegalArgumentException(s"Invalid $label row: ${JsError.toJson(errors)}"),
      identity
    )

  private def rejectUnknownFields(json: JsValue, allowed: Set[String], label: String): Unit =
    val obj = json.asOpt[JsObject].getOrElse(throw IllegalArgumentException(s"$label row must be a JSON object"))
    val id = (obj \ "id").asOpt[String].getOrElse("unknown")
    val extra = obj.keys.filterNot(allowed.contains).toVector.sorted
    if extra.nonEmpty then throw IllegalArgumentException(s"$label $id uses unsupported fields: ${extra.mkString(", ")}")

  private def loadJsonl[A: Reads](resourcePath: String, label: String): Vector[A] =
    loadJsonValues(resourcePath, label).map: json =>
      json.validate[A].fold(
        errors => throw IllegalArgumentException(s"Invalid $label row: ${JsError.toJson(errors)}"),
        identity
      )

  private def loadJsonValues(resourcePath: String, label: String): Vector[JsValue] =
    val source =
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(resourcePath))
          .getOrElse(throw IllegalStateException(s"Missing test resource $resourcePath"))
      )
    try
      source
        .getLines()
        .filter(_.trim.nonEmpty)
        .zipWithIndex
        .map: (line, index) =>
          try Json.parse(line)
          catch
            case error: Throwable =>
              throw IllegalArgumentException(s"Invalid $label row ${index + 1}: ${error.getMessage}")
        .toVector
    finally source.close()

  private def requireSource(sourceId: String, family: String, manifests: Iterable[SourceManifestRow]): String =
    val manifest =
      manifests.find(_.sourceId == sourceId).getOrElse(throw IllegalArgumentException(s"Unknown sourceId $sourceId"))
    require(manifest.validatedSourceFamily == family, s"Source $sourceId must be family $family")
    manifest.validatedLicense
    sourceId

  private def validateMoveOrder(label: String, id: String, moves: List[String]): Vector[String] =
    val values = moves.map(_.trim).filter(_.nonEmpty).toVector
    require(values.nonEmpty, s"$label $id must declare moveOrder")
    values.foreach(requireUciMove(label, id, _))
    values

  private def requireUciMove(label: String, id: String, move: String): String =
    require(
      move.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"),
      s"$label $id uses invalid UCI move $move"
    )
    move

  private def validateTokenList(label: String, id: String, field: String, values: List[String]): Vector[String] =
    val normalized = values.map(_.trim).filter(_.nonEmpty).toVector
    require(normalized.nonEmpty, s"$label $id must declare $field")
    require(
      normalized.distinct == normalized,
      s"$label $id declares duplicate $field entries: ${normalized.mkString(", ")}"
    )
    normalized.foreach(value => requireToken(label, value, field))
    normalized

  private def requireToken(label: String, value: String, field: String): String =
    require(value.trim.nonEmpty, s"$label $field must not be empty")
    require(
      value.matches("^[A-Za-z][A-Za-z0-9_:-]*$"),
      s"$label uses invalid $field $value"
    )
    value

  private def materialClassFromFen(fen: String): String =
    val board = fen.takeWhile(_ != ' ')
    val pieces = board.filter(_.isLetter)
    val nonKings = pieces.filter(ch => ch.toLower != 'k')
    val rookCount = nonKings.count(ch => ch.toLower == 'r')
    val pawnCount = nonKings.count(ch => ch.toLower == 'p')
    val otherCount = nonKings.count(ch => !Set('r', 'p').contains(ch.toLower))
    if rookCount == 2 && pawnCount == 1 && otherCount == 0 then "rook_pawn_vs_rook"
    else "unsupported"

  private def positionKeyFromFen(fen: String): String =
    val fields = Fen.Full.clean(fen).toString.split(" ").take(4).mkString(" ")
    s"std:$fields"

  private def isTruthBearingLabel(value: String): Boolean =
    val normalized = value.toLowerCase
    truthBearingLabels.contains(normalized) ||
      normalized.matches("^s[0-9]{2}$") ||
      normalized.contains("truth") ||
      normalized.contains("claim")

  private final case class BoardPiece(role: Char, color: Char, file: Int, rank: Int)

  private final case class EndgameBoardEvidence(
      placementEvidence: Set[String],
      relationEvidence: Set[String]
  )

  private object EndgameBoardEvidence:
    def fromFen(fen: String): EndgameBoardEvidence =
      val pieces = piecesFromFen(fen)
      val nonKings = pieces.filterNot(_.role == 'k')
      val pawns = nonKings.filter(_.role == 'p')
      val rooks = nonKings.filter(_.role == 'r')
      val placement = scala.collection.mutable.Set.empty[String]
      val relations = scala.collection.mutable.Set.empty[String]

      if pawns.size == 1 && rooks.size == 2 then
        val pawn = pawns.head
        val attacker = pawn.color
        val defender = if attacker == 'w' then 'b' else 'w'
        val attackerKing = pieces.find(piece => piece.role == 'k' && piece.color == attacker)
        val defenderKing = pieces.find(piece => piece.role == 'k' && piece.color == defender)
        val defenderRook = rooks.find(_.color == defender)

        if (attacker == 'w' && pawn.rank == 7) || (attacker == 'b' && pawn.rank == 2) then
          placement += "pawn_on_seventh"
        if (pawn.file == 1 || pawn.file == 8) &&
          ((attacker == 'w' && Set(6, 7).contains(pawn.rank)) ||
            (attacker == 'b' && Set(2, 3).contains(pawn.rank)))
        then placement += "rook_pawn_on_sixth_or_seventh"
        if (attacker == 'w' && pawn.rank != 6) || (attacker == 'b' && pawn.rank != 3) then
          placement += "attacker_pawn_not_on_sixth"
        attackerKing.foreach: king =>
          if distance(king, pawn) <= 1 then placement += "strong_king_near_pawn"
        defenderKing.foreach: king =>
          if distance(king, pawn) > 1 then placement += "defender_king_cut_off"
          if king.file == pawn.file &&
            ((attacker == 'w' && king.rank > pawn.rank) || (attacker == 'b' && king.rank < pawn.rank))
          then placement += "defender_king_in_front"
          val promotionRank = if attacker == 'w' then 8 else 1
          val promotionCornerFile = pawn.file
          if pawn.file == 1 || pawn.file == 8 then
            val promotionCorner = BoardPiece('k', defender, promotionCornerFile, promotionRank)
            if distance(king, promotionCorner) <= 1 then placement += "defender_king_near_corner"
        if rooks.size == 2 then relations += "rook_check_shelter"
        defenderRook.foreach: rook =>
          if (defender == 'b' && rook.rank == 3) || (defender == 'w' && rook.rank == 6) then
            relations += "defender_rook_on_third_rank"
          if rook.rank == pawn.rank && rook.file != pawn.file then
            relations += "defender_rook_lateral_checking_file"

      EndgameBoardEvidence(placement.toSet, relations.toSet)

    private def piecesFromFen(fen: String): Vector[BoardPiece] =
      val board = fen.takeWhile(_ != ' ')
      board
        .split("/")
        .zipWithIndex
        .flatMap: (rankText, rankIndex) =>
          var file = 1
          val rank = 8 - rankIndex
          rankText.flatMap:
            case digit if digit.isDigit =>
              file += digit.asDigit
              None
            case pieceChar =>
              val piece = BoardPiece(
                role = pieceChar.toLower,
                color = if pieceChar.isUpper then 'w' else 'b',
                file = file,
                rank = rank
              )
              file += 1
              Some(piece)
        .toVector

    private def distance(a: BoardPiece, b: BoardPiece): Int =
      math.max(math.abs(a.file - b.file), math.abs(a.rank - b.rank))

  private def ensure(condition: Boolean, message: => String): Unit =
    if !condition then throw IllegalArgumentException(message)
