package lila.commentary.source

import lila.commentary.selection.{ SourceContextCandidate, SourceContextExactRef, SourceContextKind }

final case class SourceContextAdapterReject(reason: String):
  require(reason.trim.nonEmpty, "SourceContextAdapterReject reason must be non-empty")

enum OpeningSequenceContextRole(val key: String):
  case MoveOrder extends OpeningSequenceContextRole("move_order")
  case PawnBreak extends OpeningSequenceContextRole("pawn_break")
  case DevelopmentLag extends OpeningSequenceContextRole("development_lag")
  case FileOwnership extends OpeningSequenceContextRole("file_ownership")
  case KingSafety extends OpeningSequenceContextRole("king_safety")
  case Transposition extends OpeningSequenceContextRole("transposition")
  case Compensation extends OpeningSequenceContextRole("compensation")
  case MasterOnlineDivergence extends OpeningSequenceContextRole("master_online_divergence")

object OpeningSequenceContextRole:
  def fromKey(key: String): Option[OpeningSequenceContextRole] =
    values.find(_.key == key)

final case class OpeningSequenceContext(
    role: OpeningSequenceContextRole,
    ref: String,
    linkedVariationProofIds: Vector[String] = Vector.empty,
    boundaries: Vector[String] = Vector("opening_sequence_context_only")
):
  require(ref.trim.nonEmpty, "Opening sequence context ref must be non-empty")

enum MotifLineContextRole(val key: String):
  case AttackedTarget extends MotifLineContextRole("attacked_target")
  case RestrictedPiece extends MotifLineContextRole("restricted_piece")
  case PinnedDefender extends MotifLineContextRole("pinned_defender")
  case OverloadedDefender extends MotifLineContextRole("overloaded_defender")
  case TrappedPiece extends MotifLineContextRole("trapped_piece")
  case NaturalResource extends MotifLineContextRole("natural_resource")
  case FailedResource extends MotifLineContextRole("failed_resource")
  case HeldResource extends MotifLineContextRole("held_resource")
  case PartialResource extends MotifLineContextRole("partial_resource")

object MotifLineContextRole:
  def fromKey(key: String): Option[MotifLineContextRole] =
    values.find(_.key == key)

final case class MotifLineContext(
    role: MotifLineContextRole,
    ref: String,
    linkedVariationProofIds: Vector[String] = Vector.empty,
    boundaries: Vector[String] = Vector("motif_line_context_only")
):
  require(ref.trim.nonEmpty, "Motif line context ref must be non-empty")

enum EndgameTechniqueContextRole(val key: String):
  case Opposition extends EndgameTechniqueContextRole("opposition")
  case CheckingDistance extends EndgameTechniqueContextRole("checking_distance")
  case RookActivity extends EndgameTechniqueContextRole("rook_activity")
  case PawnEndingTransitionRisk extends EndgameTechniqueContextRole("pawn_ending_transition_risk")
  case WrongExchange extends EndgameTechniqueContextRole("wrong_exchange")
  case BridgeSetup extends EndgameTechniqueContextRole("bridge_setup")
  case ThirdRankSetup extends EndgameTechniqueContextRole("third_rank_setup")
  case SideCheckingSetup extends EndgameTechniqueContextRole("side_checking_setup")
  case MethodException extends EndgameTechniqueContextRole("method_exception")
  case DefenderResource extends EndgameTechniqueContextRole("defender_resource")
  case HoldLine extends EndgameTechniqueContextRole("hold_line")

object EndgameTechniqueContextRole:
  def fromKey(key: String): Option[EndgameTechniqueContextRole] =
    values.find(_.key == key)

final case class EndgameTechniqueContext(
    role: EndgameTechniqueContextRole,
    ref: String,
    linkedVariationProofIds: Vector[String] = Vector.empty,
    boundaries: Vector[String] = Vector("endgame_technique_context_only")
):
  require(ref.trim.nonEmpty, "Endgame technique context ref must be non-empty")

enum RetrievalIllustrationContextRole(val key: String):
  case ComparableLine extends RetrievalIllustrationContextRole("comparable_line")
  case SimilarPlanSequence extends RetrievalIllustrationContextRole("similar_plan_sequence")
  case ThemeExample extends RetrievalIllustrationContextRole("theme_example")
  case CitationContext extends RetrievalIllustrationContextRole("citation_context")

object RetrievalIllustrationContextRole:
  def fromKey(key: String): Option[RetrievalIllustrationContextRole] =
    values.find(_.key == key)

final case class RetrievalIllustrationContext(
    role: RetrievalIllustrationContextRole,
    ref: String,
    linkedVariationProofIds: Vector[String] = Vector.empty,
    boundaries: Vector[String] = Vector("retrieval_illustration_context_only")
):
  require(ref.trim.nonEmpty, "Retrieval illustration context ref must be non-empty")

enum SourceContextInput:
  case Opening(
      candidateId: String,
      canonicalPositionId: String,
      sourceUses: Vector[String] = Vector.empty,
      sequenceContexts: Vector[OpeningSequenceContext] = Vector.empty
  )
  case Motif(
      candidateId: String,
      motifExampleId: String,
      detectorCarrierId: String,
      motifId: Option[String] = None,
      carrierKind: String = "u_witness",
      lineContexts: Vector[MotifLineContext] = Vector.empty
  )
  case EndgameStudy(
      candidateId: String,
      studyId: String,
      applicabilityFixtureId: String,
      applicabilityVerified: Boolean = false,
      outcomeClaim: Option[String] = None,
      techniqueContexts: Vector[EndgameTechniqueContext] = Vector.empty
  )
  case Retrieval(
      candidateId: String,
      retrievalExampleId: String,
      currentPositionClaim: Boolean = false,
      illustrationContexts: Vector[RetrievalIllustrationContext] = Vector.empty,
      displayCandidate: Boolean = false
  )

object SourceContextAdapter:

  def normalize(input: SourceContextInput): Either[SourceContextAdapterReject, SourceContextCandidate] =
    if !SourceContextCandidate.hasPublicSafeShape(candidateIdOf(input)) then reject("source_context_invalid_candidate_id")
    else
      input match
        case SourceContextInput.Opening(candidateId, canonicalPositionId, sourceUses, sequenceContexts) =>
          normalizeOpening(candidateId, canonicalPositionId, sourceUses, sequenceContexts)
        case SourceContextInput.Motif(candidateId, motifExampleId, detectorCarrierId, motifId, carrierKind, lineContexts) =>
          normalizeMotif(candidateId, motifExampleId, detectorCarrierId, motifId, carrierKind, lineContexts)
        case SourceContextInput.EndgameStudy(candidateId, studyId, applicabilityFixtureId, applicabilityVerified, outcomeClaim, techniqueContexts) =>
          normalizeEndgameStudy(candidateId, studyId, applicabilityFixtureId, applicabilityVerified, outcomeClaim, techniqueContexts)
        case SourceContextInput.Retrieval(candidateId, retrievalExampleId, currentPositionClaim, illustrationContexts, displayCandidate) =>
          normalizeRetrieval(candidateId, retrievalExampleId, currentPositionClaim, illustrationContexts, displayCandidate)

  private def candidateIdOf(input: SourceContextInput): String =
    input match
      case SourceContextInput.Opening(candidateId, _, _, _) => candidateId
      case SourceContextInput.Motif(candidateId, _, _, _, _, _) => candidateId
      case SourceContextInput.EndgameStudy(candidateId, _, _, _, _, _) => candidateId
      case SourceContextInput.Retrieval(candidateId, _, _, _, _) => candidateId

  private def normalizeOpening(
      candidateId: String,
      canonicalPositionId: String,
      sourceUses: Vector[String],
      sequenceContexts: Vector[OpeningSequenceContext]
  ): Either[SourceContextAdapterReject, SourceContextCandidate] =
    val uses = sourceUses.map(_.trim).filter(_.nonEmpty)
    val hasMergedRanking =
      uses.exists(use => normalized(use).contains("master_reference") && normalized(use).contains("online_trend"))
    val hasTruthToken =
      (Vector(canonicalPositionId) ++ uses).exists(containsTruthToken)
    val hasUnsupportedSourceUse =
      uses.exists(use => !allowedOpeningSourceUses.contains(normalized(use)))
    val hasProductRejectedSource =
      uses.exists(use => Set("pipeline_smoke", "taxonomy_reference").contains(normalized(use)))
    val hasUnsafeSequenceContext =
      sequenceContexts.exists(unsafeOpeningSequenceContext)
    if canonicalPositionId.trim.isEmpty then reject("opening_missing_canonical_position")
    else if hasMergedRanking then reject("opening_ranking_merge")
    else if hasProductRejectedSource then reject("opening_product_rejected_source")
    else if hasTruthToken || hasUnsupportedSourceUse then reject("opening_truth_claim")
    else if hasUnsafeSequenceContext then reject("opening_sequence_truth_claim")
    else if !SourceContextCandidate.isPublicSafeId(candidateId) then reject("source_context_invalid_candidate_id")
    else
      Right(
        SourceContextCandidate(
          candidateId = candidateId,
          kind = SourceContextKind.Opening,
          sourceRefs =
            (Vector(s"opening-position:${canonicalPositionId.trim}:canonical") ++
              uses.map(use => s"opening-source-use:$use") ++
              sequenceContexts.map(context => s"opening-sequence:${context.role.key}:${context.ref.trim}") ++
              sequenceContexts.flatMap(_.linkedVariationProofIds.map(id => s"opening-line-test:${id.trim}:context"))).distinct
        )
      )

  private def normalizeMotif(
      candidateId: String,
      motifExampleId: String,
      detectorCarrierId: String,
      motifId: Option[String],
      carrierKind: String,
      lineContexts: Vector[MotifLineContext]
  ): Either[SourceContextAdapterReject, SourceContextCandidate] =
    val _ = carrierKind
    val resolvedMotifId = motifId.map(_.trim).filter(_.nonEmpty).getOrElse(inferMotifId(motifExampleId))
    if motifExampleId.trim.isEmpty || detectorCarrierId.trim.isEmpty then reject("motif_missing_carrier")
    else if motifExampleId.trim != detectorCarrierId.trim then reject("motif_carrier_mismatch")
    else if containsTruthToken(motifExampleId) || containsTruthToken(detectorCarrierId) || containsTruthToken(resolvedMotifId) then reject("motif_truth_claim")
    else if deferredMotifIds.contains(normalized(resolvedMotifId)) then reject("motif_deferred_helper_required")
    else if !admittedMotifIds.contains(normalized(resolvedMotifId)) then reject("motif_unknown")
    else if certificationOnlyMotifIds.contains(normalized(resolvedMotifId)) then reject("motif_certification_carrier_required")
    else if lineContexts.exists(unsafeMotifLineContext) then reject("motif_line_context_truth_claim")
    else if !SourceContextCandidate.isPublicSafeId(candidateId) then reject("source_context_invalid_candidate_id")
    else
      Right(
        SourceContextCandidate(
          candidateId = candidateId,
          kind = SourceContextKind.Motif,
          sourceRefs =
            (Vector(s"motif-example:${motifExampleId.trim}") ++
              lineContexts.map(context => s"motif-line-context:${context.role.key}:${context.ref.trim}") ++
              lineContexts.flatMap(_.linkedVariationProofIds.map(id => s"motif-line-test:${id.trim}:context"))).distinct,
          exactBoardRefs = Vector(SourceContextExactRef(s"motif-detector-carrier:${detectorCarrierId.trim}"))
        )
      )

  private def normalizeEndgameStudy(
      candidateId: String,
      studyId: String,
      applicabilityFixtureId: String,
      applicabilityVerified: Boolean,
      outcomeClaim: Option[String],
      techniqueContexts: Vector[EndgameTechniqueContext]
  ): Either[SourceContextAdapterReject, SourceContextCandidate] =
    if studyId.trim.isEmpty || applicabilityFixtureId.trim.isEmpty then reject("endgame_missing_applicability")
    else if deferredEndgameStudyIds.contains(normalized(studyId)) then reject("endgame_deferred_study")
    else if !admittedEndgameStudyIds.contains(normalized(studyId)) then reject("endgame_unknown_study")
    else if outcomeClaim.exists(claim => claim.trim.nonEmpty && claim.trim.toLowerCase != "none") then reject("endgame_result_claim")
    else if containsEndgameResultToken(studyId) || containsEndgameResultToken(applicabilityFixtureId) then reject("endgame_result_claim")
    else if !applicabilityVerified then reject("endgame_unverified_applicability")
    else if techniqueContexts.exists(unsafeEndgameTechniqueContext) then reject("endgame_technique_truth_claim")
    else if !SourceContextCandidate.isPublicSafeId(candidateId) then reject("source_context_invalid_candidate_id")
    else
      Right(
        SourceContextCandidate(
          candidateId = candidateId,
          kind = SourceContextKind.EndgameStudy,
          sourceRefs =
            (Vector(s"endgame-study:${studyId.trim}:applicable") ++
              techniqueContexts.map(context => s"endgame-technique:${context.role.key}:${context.ref.trim}") ++
              techniqueContexts.flatMap(_.linkedVariationProofIds.map(id => s"endgame-line-test:${id.trim}:context"))).distinct,
          exactBoardRefs = Vector(
            SourceContextExactRef(
              id = s"endgame-study-applicability:${applicabilityFixtureId.trim}",
              route = Some(studyId.trim),
              scope = Some("exact_endgame_applicability")
            )
          )
        )
      )

  private def normalizeRetrieval(
      candidateId: String,
      retrievalExampleId: String,
      currentPositionClaim: Boolean,
      illustrationContexts: Vector[RetrievalIllustrationContext],
      displayCandidate: Boolean
  ): Either[SourceContextAdapterReject, SourceContextCandidate] =
    if displayCandidate then reject("retrieval_display_deferred")
    else if currentPositionClaim || containsTruthToken(retrievalExampleId) || containsRetrievalVerdictToken(retrievalExampleId) then reject("retrieval_truth_claim")
    else if retrievalExampleId.trim.isEmpty then reject("retrieval_missing_example")
    else if illustrationContexts.exists(unsafeRetrievalIllustrationContext) then reject("retrieval_illustration_truth_claim")
    else if !SourceContextCandidate.isPublicSafeId(candidateId) then reject("source_context_invalid_candidate_id")
    else
      Right(
        SourceContextCandidate(
          candidateId = candidateId,
          kind = SourceContextKind.Retrieval,
          sourceRefs =
            (Vector(s"retrieval-example:${retrievalExampleId.trim}") ++
              illustrationContexts.map(context => s"retrieval-illustration:${context.role.key}:${context.ref.trim}") ++
              illustrationContexts.flatMap(_.linkedVariationProofIds.map(id => s"retrieval-line-test:${id.trim}:context"))).distinct
        )
      )

  private def reject(reason: String): Either[SourceContextAdapterReject, SourceContextCandidate] =
    Left(SourceContextAdapterReject(reason))

  private def containsTruthToken(value: String): Boolean =
    val text = normalized(value)
    Vector(
      "best",
      "recommend",
      "recommendation",
      "theory",
      "truth",
      "theory_truth",
      "current_position_truth",
      "current_position_proof",
      "forced",
      "result",
      "engine",
      "oracle",
      "winning",
      "drawing",
      "wdl",
      "dtz",
      "dtm",
      "draw_offer",
      "repetition",
      "tournament",
      "rating",
      "time_control",
      "game_context",
      "play_environment"
    ).exists(text.contains)

  private def unsafeOpeningSequenceContext(context: OpeningSequenceContext): Boolean =
    val values = Vector(context.ref) ++ context.linkedVariationProofIds ++ context.boundaries
    values.exists(value => value.trim.isEmpty || containsTruthToken(value)) ||
      !context.boundaries.contains("opening_sequence_context_only") ||
      (context.linkedVariationProofIds.nonEmpty && !context.boundaries.contains("line_test_link_is_not_proof"))

  private def unsafeMotifLineContext(context: MotifLineContext): Boolean =
    val values = Vector(context.ref) ++ context.linkedVariationProofIds ++ context.boundaries
    values.exists(value => value.trim.isEmpty || containsTruthToken(value)) ||
      !context.boundaries.contains("motif_line_context_only") ||
      (context.linkedVariationProofIds.nonEmpty && !context.boundaries.contains("line_test_link_is_not_proof"))

  private def unsafeEndgameTechniqueContext(context: EndgameTechniqueContext): Boolean =
    val values = Vector(context.ref) ++ context.linkedVariationProofIds ++ context.boundaries
    values.exists(value => value.trim.isEmpty || containsEndgameResultToken(value) || containsDeferredEndgameToken(value)) ||
      !context.boundaries.contains("endgame_technique_context_only") ||
      (context.linkedVariationProofIds.nonEmpty && !context.boundaries.contains("line_test_link_is_not_proof"))

  private def unsafeRetrievalIllustrationContext(context: RetrievalIllustrationContext): Boolean =
    val values = Vector(context.ref) ++ context.linkedVariationProofIds ++ context.boundaries
    values.exists(value => value.trim.isEmpty || containsTruthToken(value) || containsRetrievalVerdictToken(value)) ||
      !context.boundaries.contains("retrieval_illustration_context_only") ||
      (context.linkedVariationProofIds.nonEmpty && !context.boundaries.contains("line_test_link_is_not_proof"))

  private def containsEndgameResultToken(value: String): Boolean =
    val text = normalized(value)
    Vector("win", "draw", "loss", "result", "oracle", "wdl", "dtz", "dtm", "forced", "conversion", "tablebase").exists(text.contains)

  private def containsDeferredEndgameToken(value: String): Boolean =
    val text = normalized(value)
    deferredEndgameStudyIds.exists(text.contains)

  private def containsRetrievalVerdictToken(value: String): Boolean =
    val text = normalized(value)
    Vector(
      "recommendation",
      "recommend",
      "verdict",
      "game_result",
      "result_metadata",
      "player_names_are_recommendations",
      "display_player",
      "display_event",
      "display_result",
      "famous_player"
    ).exists(text.contains)

  private def normalized(value: String): String =
    value.toLowerCase.replace('-', '_').replace(':', '_').replace('+', '_').replace(' ', '_')

  private val allowedOpeningSourceUses: Set[String] =
    Set("master_reference", "online_trend")

  private val admittedMotifIds: Set[String] =
    Set("loose_piece", "pin", "fork", "skewer", "overload", "trapped_piece", "mate_net", "perpetual_check")

  private val certificationOnlyMotifIds: Set[String] =
    Set("mate_net", "perpetual_check")

  private val deferredMotifIds: Set[String] =
    Set("discovered_attack", "deflection", "back_rank", "back_rank_mate", "clearance", "interference")

  private def inferMotifId(motifExampleId: String): String =
    val text = normalized(motifExampleId)
    (admittedMotifIds ++ deferredMotifIds).toVector.sortBy(id => -id.length).find(text.contains).getOrElse(text)

  private val admittedEndgameStudyIds: Set[String] =
    Set(
      "lucena_rook_pawn",
      "philidor_rook_pawn",
      "vancura_rook_pawn",
      "basic_opposition_kpk",
      "distant_opposition_kpk",
      "wrong_rook_pawn_bishop",
      "rook_behind_passed_pawn"
    )

  private val deferredEndgameStudyIds: Set[String] =
    Set(
      "outside_passer",
      "fortress_pattern",
      "rook_on_seventh",
      "triangulation",
      "corresponding_squares",
      "shouldering",
      "breakthrough",
      "reserve_tempo"
    )
