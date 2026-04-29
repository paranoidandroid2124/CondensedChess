package lila.commentary.render.annotation

import lila.commentary.selection.*
import lila.commentary.selection.PublicVariationEvidenceSafety

final case class BookAnnotationPlan(
    units: Vector[BookAnnotationUnit],
    boundaries: Vector[BookAnnotationBoundary],
    wording: BookAnnotationWordingRules
)

final case class BookAnnotationUnit(
    claimId: String,
    lineSan: Vector[String],
    lineUci: Vector[String],
    resourceLine: Vector[BookAnnotationMove],
    replyLine: Vector[BookAnnotationMove],
    proofRole: VariationEvidenceRole,
    testResult: VariationTestResult,
    sourceFrames: Vector[BookAnnotationSourceFrame],
    wordingCap: WordingStrength,
    proofIds: BookAnnotationProofIds,
    supportingLines: Vector[LineSupport] = Vector.empty,
    cautionLines: Vector[LineCaution] = Vector.empty
)

final case class BookAnnotationMove(
    san: String,
    uci: String
)

object BookAnnotationMove:
  def from(move: VariationMove): BookAnnotationMove =
    BookAnnotationMove(move.san, move.uci)

final case class LineCommentaryDetail(
    lineSan: Vector[String],
    lineUci: Vector[String],
    testedMove: Option[BookAnnotationMove],
    testedLine: Vector[BookAnnotationMove],
    replyLine: Vector[BookAnnotationMove],
    resourceLine: Vector[BookAnnotationMove],
    wordingCap: WordingStrength
)

final case class LineSupport(
    kind: LineSupportKind,
    detail: LineCommentaryDetail
)

enum LineSupportKind(val key: String):
  case PressurePersists extends LineSupportKind("pressure_persists")
  case DoesNotRestoreCounterplay extends LineSupportKind("does_not_restore_counterplay")
  case ResourceFails extends LineSupportKind("resource_fails")
  case ResourceWorks extends LineSupportKind("resource_works")
  case DefensiveHold extends LineSupportKind("defensive_hold")
  case Simplifies extends LineSupportKind("simplifies")
  case Converts extends LineSupportKind("converts")

final case class LineCaution(
    kind: LineCautionKind,
    detail: LineCommentaryDetail
)

enum LineCautionKind(val key: String):
  case EarlyMoveCaution extends LineCautionKind("early_move_caution")
  case PrematureMove extends LineCautionKind("premature_move")
  case ReleasesCounterplay extends LineCautionKind("releases_counterplay")

final case class BookAnnotationSourceFrame(
    kind: PlanAnnotationFrameKind,
    proofId: String,
    sourceRefIds: Vector[String],
    authoritative: Boolean
)

final case class BookAnnotationProofIds(
    primaryProofId: String,
    companionProofIds: Vector[String],
    supportProofIds: Vector[String],
    negativeProofIds: Vector[String]
)

final case class BookAnnotationBoundary(
    claimId: Option[String],
    proofId: Option[String],
    reason: BookAnnotationBoundaryReason
)

enum BookAnnotationBoundaryReason(val key: String):
  case NoSelectedMainClaim extends BookAnnotationBoundaryReason("no_selected_main_claim")
  case MultipleSelectedMainClaims extends BookAnnotationBoundaryReason("multiple_selected_main_claims")
  case BlockedClaim extends BookAnnotationBoundaryReason("blocked_claim")
  case NonAdmittedClaim extends BookAnnotationBoundaryReason("non_admitted_claim")
  case NonExactBoardClaim extends BookAnnotationBoundaryReason("non_exact_board_claim")
  case SourceContextOwner extends BookAnnotationBoundaryReason("source_context_owner")
  case RawEngineOwner extends BookAnnotationBoundaryReason("raw_engine_owner")
  case RendererOwner extends BookAnnotationBoundaryReason("renderer_owner")
  case MissingAnnotationSelection extends BookAnnotationBoundaryReason("missing_annotation_selection")
  case SelectionClaimMismatch extends BookAnnotationBoundaryReason("selection_claim_mismatch")
  case NonStrongSelection extends BookAnnotationBoundaryReason("non_strong_selection")
  case UnsafeProofId extends BookAnnotationBoundaryReason("unsafe_proof_id")
  case PrimaryProofMissing extends BookAnnotationBoundaryReason("primary_proof_missing")
  case PrimaryProofUnsafe extends BookAnnotationBoundaryReason("primary_proof_unsafe")
  case PrimaryProofBindingMismatch extends BookAnnotationBoundaryReason("primary_proof_binding_mismatch")
  case PrimaryProofNotCandidate extends BookAnnotationBoundaryReason("primary_proof_not_candidate")
  case MissingCompanionProof extends BookAnnotationBoundaryReason("missing_companion_proof")
  case CompanionProofUnsafe extends BookAnnotationBoundaryReason("companion_proof_unsafe")
  case CompanionProofBindingMismatch extends BookAnnotationBoundaryReason("companion_proof_binding_mismatch")
  case CompanionProofNotDefenderResource extends BookAnnotationBoundaryReason("companion_proof_not_defender_resource")
  case SupportProofUnsafe extends BookAnnotationBoundaryReason("support_proof_unsafe")
  case NegativeProofUnsafe extends BookAnnotationBoundaryReason("negative_proof_unsafe")
  case UnmatchedSourceFrame extends BookAnnotationBoundaryReason("unmatched_source_frame")

final case class BookAnnotationWordingRules(
    maxStrength: WordingStrength
)

object BookAnnotationPlanner:

  def plan(plan: CommentaryPlan): BookAnnotationPlan =
    val selectedMainClaims = plan.main.toVector.flatMap(_.claims)
    val mainBoundary =
      if selectedMainClaims.isEmpty then
        Vector(BookAnnotationBoundary(None, None, BookAnnotationBoundaryReason.NoSelectedMainClaim))
      else if selectedMainClaims.size > 1 then
        selectedMainClaims.map(selected =>
          BookAnnotationBoundary(Some(selected.claim.id), None, BookAnnotationBoundaryReason.MultipleSelectedMainClaims)
        )
      else Vector.empty

    val selectedMain = selectedMainClaims.headOption.filter(_ => selectedMainClaims.size == 1)
    val selectedResult = selectedMain.map(selected => planForSelectedMain(plan, selected)).getOrElse(PlanResult.empty)

    BookAnnotationPlan(
      units = selectedResult.units,
      boundaries = mainBoundary ++ selectedResult.boundaries,
      wording = BookAnnotationWordingRules(plan.wordingRules.maxStrength)
    )

  private final case class PlanResult(
      units: Vector[BookAnnotationUnit],
      boundaries: Vector[BookAnnotationBoundary]
  )

  private object PlanResult:
    val empty: PlanResult = PlanResult(Vector.empty, Vector.empty)

  private def planForSelectedMain(plan: CommentaryPlan, selected: SelectedClaim): PlanResult =
    val claim = selected.claim
    val claimBoundaries = claimAdmissionBoundaries(plan, claim)
    if claimBoundaries.nonEmpty then PlanResult(Vector.empty, claimBoundaries)
    else
      val selections = plan.annotationSelections.filter(_.claimId == claim.id)
      if selections.isEmpty then
        PlanResult(Vector.empty, Vector(BookAnnotationBoundary(Some(claim.id), None, BookAnnotationBoundaryReason.MissingAnnotationSelection)))
      else
        val selectionMismatch =
          plan.annotationSelections
            .filterNot(_.claimId == claim.id)
            .map(selection =>
              BookAnnotationBoundary(Some(selection.claimId), Some(selection.primaryProofId), BookAnnotationBoundaryReason.SelectionClaimMismatch)
            )
        val selectedResults = selections.map(selection => planForSelection(plan, claim, selection))
        PlanResult(
          units = selectedResults.flatMap(_.units),
          boundaries = selectionMismatch ++ selectedResults.flatMap(_.boundaries)
        )

  private def claimAdmissionBoundaries(plan: CommentaryPlan, claim: CommentaryClaim): Vector[BookAnnotationBoundary] =
    val blockedClaimIds = plan.blocked.map(_.claim.id).toSet
    Vector(
      Option.when(blockedClaimIds.contains(claim.id))(
        BookAnnotationBoundary(Some(claim.id), None, BookAnnotationBoundaryReason.BlockedClaim)
      ),
      Option.when(claim.status != ClaimStatus.Admitted)(
        BookAnnotationBoundary(Some(claim.id), None, BookAnnotationBoundaryReason.NonAdmittedClaim)
      ),
      Option.when(!claim.exactBoardBound)(
        BookAnnotationBoundary(Some(claim.id), None, BookAnnotationBoundaryReason.NonExactBoardClaim)
      ),
      Option.when(claim.layer == ClaimLayer.SourceContext)(
        BookAnnotationBoundary(Some(claim.id), None, BookAnnotationBoundaryReason.SourceContextOwner)
      ),
      Option.when(claim.layer == ClaimLayer.Engine)(
        BookAnnotationBoundary(Some(claim.id), None, BookAnnotationBoundaryReason.RawEngineOwner)
      ),
      Option.when(claim.layer == ClaimLayer.Renderer)(
        BookAnnotationBoundary(Some(claim.id), None, BookAnnotationBoundaryReason.RendererOwner)
      )
    ).flatten

  private def planForSelection(
      plan: CommentaryPlan,
      claim: CommentaryClaim,
      selection: PlanAnnotationSelection
  ): PlanResult =
    val selectionBoundaries =
      Vector(
        Option.when(selection.strength != PlanAnnotationStrength.Strong)(
          BookAnnotationBoundary(Some(claim.id), Some(selection.primaryProofId), BookAnnotationBoundaryReason.NonStrongSelection)
        )
      ).flatten ++ proofIdBoundaries(claim.id, selection)

    if selectionBoundaries.nonEmpty then PlanResult(Vector.empty, selectionBoundaries)
    else
      val proofById = plan.variationEvidence.map(_.proof).map(proof => proof.proofId -> proof).toMap
      val primary = proofById.get(selection.primaryProofId)
      val primaryBoundaries = primary match
        case None =>
          Vector(BookAnnotationBoundary(Some(claim.id), Some(selection.primaryProofId), BookAnnotationBoundaryReason.PrimaryProofMissing))
        case Some(proof) =>
          primaryProofBoundaries(claim, proof)

      if primaryBoundaries.nonEmpty then PlanResult(Vector.empty, primaryBoundaries)
      else
        val companionResults = selection.companionProofIds.map(proofId => companionProofResult(claim, proofById, proofId))
        val companionBoundaries = companionResults.flatMap(_.boundaries)
        val companionProofs = companionResults.flatMap(_.proof)
        val supportResults = selection.supportProofIds.map(proofId => detailProofResult(claim, proofById, proofId, BookAnnotationBoundaryReason.SupportProofUnsafe))
        val negativeResults = selection.negativeProofIds.map(proofId => detailProofResult(claim, proofById, proofId, BookAnnotationBoundaryReason.NegativeProofUnsafe))
        val detailBoundaries = supportResults.flatMap(_.boundaries) ++ negativeResults.flatMap(_.boundaries)

        if companionBoundaries.nonEmpty || detailBoundaries.nonEmpty then
          PlanResult(Vector.empty, companionBoundaries ++ detailBoundaries)
        else if companionProofs.isEmpty then
          PlanResult(
            Vector.empty,
            Vector(BookAnnotationBoundary(Some(claim.id), Some(selection.primaryProofId), BookAnnotationBoundaryReason.MissingCompanionProof))
          )
        else
          val frameResult = sourceFrameResult(selection, proofById.keySet)
          if frameResult.boundaries.nonEmpty then PlanResult(Vector.empty, frameResult.boundaries)
          else
            PlanResult(
              units = Vector(unitFor(claim, primary.get, companionProofs, supportResults.flatMap(_.proof), negativeResults.flatMap(_.proof), selection, frameResult.frames)),
              boundaries = Vector.empty
            )

  private def proofIdBoundaries(claimId: String, selection: PlanAnnotationSelection): Vector[BookAnnotationBoundary] =
    val proofIds =
      Vector(selection.primaryProofId) ++
        selection.companionProofIds ++
        selection.supportProofIds ++
        selection.negativeProofIds
    proofIds
      .filterNot(PublicVariationEvidenceSafety.publicSafeProofId)
      .map(proofId => BookAnnotationBoundary(Some(claimId), Some(proofId), BookAnnotationBoundaryReason.UnsafeProofId))

  private def primaryProofBoundaries(claim: CommentaryClaim, proof: PreparedVariationEvidence): Vector[BookAnnotationBoundary] =
    val base = publicSafeProofBoundaries(claim, proof, BookAnnotationBoundaryReason.PrimaryProofUnsafe)
    base ++ Vector(
      Option.when(!variationProofBoundToClaim(proof, claim))(
        BookAnnotationBoundary(Some(claim.id), Some(proof.proofId), BookAnnotationBoundaryReason.PrimaryProofBindingMismatch)
      ),
      Option.when(!isCandidatePrimary(proof))(
        BookAnnotationBoundary(Some(claim.id), Some(proof.proofId), BookAnnotationBoundaryReason.PrimaryProofNotCandidate)
      )
    ).flatten

  private final case class ProofResult(
      proof: Vector[PreparedVariationEvidence],
      boundaries: Vector[BookAnnotationBoundary]
  )

  private def companionProofResult(
      claim: CommentaryClaim,
      proofById: Map[String, PreparedVariationEvidence],
      proofId: String
  ): ProofResult =
    proofById.get(proofId) match
      case None =>
        ProofResult(Vector.empty, Vector(BookAnnotationBoundary(Some(claim.id), Some(proofId), BookAnnotationBoundaryReason.MissingCompanionProof)))
      case Some(proof) =>
        val boundaries =
          publicSafeProofBoundaries(claim, proof, BookAnnotationBoundaryReason.CompanionProofUnsafe) ++
            Vector(
              Option.when(!variationProofBoundToClaim(proof, claim))(
                BookAnnotationBoundary(Some(claim.id), Some(proof.proofId), BookAnnotationBoundaryReason.CompanionProofBindingMismatch)
              ),
              Option.when(!isDefenderResourceOrReply(proof))(
                BookAnnotationBoundary(Some(claim.id), Some(proof.proofId), BookAnnotationBoundaryReason.CompanionProofNotDefenderResource)
              )
            ).flatten
        ProofResult(Option.when(boundaries.isEmpty)(proof).toVector, boundaries)

  private def detailProofResult(
      claim: CommentaryClaim,
      proofById: Map[String, PreparedVariationEvidence],
      proofId: String,
      unsafeReason: BookAnnotationBoundaryReason
  ): ProofResult =
    proofById.get(proofId) match
      case None =>
        ProofResult(Vector.empty, Vector(BookAnnotationBoundary(Some(claim.id), Some(proofId), unsafeReason)))
      case Some(proof) =>
        val boundaries =
          publicSafeProofBoundaries(claim, proof, unsafeReason) ++
            Option.when(!variationProofBoundToClaim(proof, claim))(
              BookAnnotationBoundary(Some(claim.id), Some(proof.proofId), unsafeReason)
            ).toVector
        ProofResult(Option.when(boundaries.isEmpty)(proof).toVector, boundaries)

  private def publicSafeProofBoundaries(
      claim: CommentaryClaim,
      proof: PreparedVariationEvidence,
      unsafeReason: BookAnnotationBoundaryReason
  ): Vector[BookAnnotationBoundary] =
    Option
      .when(!PublicVariationEvidenceSafety.publicSafeForClaim(claim, proof) || proof.surfaceAllowance != VariationSurfaceAllowance.PublicLine)(
        BookAnnotationBoundary(Some(claim.id), Some(proof.proofId), unsafeReason)
      )
      .toVector

  private final case class SourceFrameResult(
      frames: Vector[BookAnnotationSourceFrame],
      boundaries: Vector[BookAnnotationBoundary]
  )

  private def sourceFrameResult(
      selection: PlanAnnotationSelection,
      selectedProofIds: Set[String]
  ): SourceFrameResult =
    val (matched, unmatched) =
      selection.sourceFrames.partition(frame =>
        frame.proofId == selection.primaryProofId ||
          selection.companionProofIds.contains(frame.proofId) ||
          selection.supportProofIds.contains(frame.proofId) ||
          selection.negativeProofIds.contains(frame.proofId)
      )
    val frames =
      matched
        .filter(frame => selectedProofIds.contains(frame.proofId))
        .filter(validSourceFrame)
        .map(frame =>
          BookAnnotationSourceFrame(
            kind = frame.kind,
            proofId = frame.proofId,
            sourceRefIds = frame.sourceRefIds,
            authoritative = false
          )
        )
    val missingMatched =
      matched.filterNot(frame => selectedProofIds.contains(frame.proofId) && validSourceFrame(frame))
    SourceFrameResult(
      frames = frames,
      boundaries =
        (unmatched ++ missingMatched).map(frame =>
          BookAnnotationBoundary(Some(selection.claimId), Some(frame.proofId), BookAnnotationBoundaryReason.UnmatchedSourceFrame)
        )
    )

  private def validSourceFrame(frame: PlanAnnotationFrame): Boolean =
    sourceFrameKind(frame.kind).exists(kind =>
      frame.sourceRefIds.nonEmpty &&
        frame.sourceRefIds.forall(ref => PublicVariationEvidenceSafety.lineTestProofIdForKind(ref, kind).contains(frame.proofId))
    )

  private def sourceFrameKind(kind: PlanAnnotationFrameKind): Option[SourceContextKind] =
    kind match
      case PlanAnnotationFrameKind.Opening => Some(SourceContextKind.Opening)
      case PlanAnnotationFrameKind.Motif => Some(SourceContextKind.Motif)
      case PlanAnnotationFrameKind.EndgameStudy => Some(SourceContextKind.EndgameStudy)
      case PlanAnnotationFrameKind.Retrieval => Some(SourceContextKind.Retrieval)

  private def unitFor(
      claim: CommentaryClaim,
      primary: PreparedVariationEvidence,
      companions: Vector[PreparedVariationEvidence],
      support: Vector[PreparedVariationEvidence],
      negative: Vector[PreparedVariationEvidence],
      selection: PlanAnnotationSelection,
      sourceFrames: Vector[BookAnnotationSourceFrame]
  ): BookAnnotationUnit =
    val resourceLine = companions.flatMap(_.resourceLine).distinct
    val replyLine = (primary.replyLine ++ companions.flatMap(_.replyLine)).distinct
    BookAnnotationUnit(
      claimId = claim.id,
      lineSan = primary.lineSan,
      lineUci = primary.lineUci,
      resourceLine = resourceLine.map(BookAnnotationMove.from),
      replyLine = replyLine.map(BookAnnotationMove.from),
      proofRole = primary.role,
      testResult = primary.testResult,
      sourceFrames = sourceFrames,
      wordingCap = WordingStrength.weaker(primary.wordingCap, selection.wordingCap),
      proofIds = BookAnnotationProofIds(
        primaryProofId = primary.proofId,
        companionProofIds = companions.map(_.proofId),
        supportProofIds = support.map(_.proofId),
        negativeProofIds = negative.map(_.proofId)
      ),
      supportingLines = support.flatMap(supportingLine),
      cautionLines = negative.flatMap(cautionLine)
    )

  private def supportingLine(proof: PreparedVariationEvidence): Option[LineSupport] =
    for
      kind <- supportKind(proof)
      detail <- lineDetail(proof)
      if isSupportProof(proof)
    yield LineSupport(kind, detail)

  private def cautionLine(proof: PreparedVariationEvidence): Option[LineCaution] =
    for
      kind <- cautionKind(proof)
      detail <- lineDetail(proof)
      if isNegativeProof(proof)
    yield LineCaution(kind, detail)

  private def lineDetail(proof: PreparedVariationEvidence): Option[LineCommentaryDetail] =
    Option
      .when(proof.lineSan.nonEmpty && proof.lineSan.size == proof.lineUci.size && proof.testedMove.nonEmpty && proof.testedLine.nonEmpty)(
        LineCommentaryDetail(
          lineSan = proof.lineSan,
          lineUci = proof.lineUci,
          testedMove = proof.testedMove.map(BookAnnotationMove.from),
          testedLine = proof.testedLine.map(BookAnnotationMove.from),
          replyLine = proof.replyLine.map(BookAnnotationMove.from),
          resourceLine = proof.resourceLine.map(BookAnnotationMove.from),
          wordingCap = proof.wordingCap
        )
      )

  private def supportKind(proof: PreparedVariationEvidence): Option[LineSupportKind] =
    (proof.role, proof.testResult) match
      case (VariationEvidenceRole.Persistence, VariationTestResult.PressurePersists) =>
        Some(LineSupportKind.PressurePersists)
      case (VariationEvidenceRole.DefenderResource, VariationTestResult.DoesNotRestoreCounterplay) =>
        Some(LineSupportKind.DoesNotRestoreCounterplay)
      case (VariationEvidenceRole.DefenderResource, VariationTestResult.ResourceFails) =>
        Some(LineSupportKind.ResourceFails)
      case (VariationEvidenceRole.DefenderResource, VariationTestResult.ResourceWorks) =>
        Some(LineSupportKind.ResourceWorks)
      case (VariationEvidenceRole.Hold, VariationTestResult.DefensiveHold) =>
        Some(LineSupportKind.DefensiveHold)
      case (VariationEvidenceRole.Simplification, VariationTestResult.Simplifies) =>
        Some(LineSupportKind.Simplifies)
      case (VariationEvidenceRole.Conversion, VariationTestResult.Simplifies) =>
        Some(LineSupportKind.Simplifies)
      case (VariationEvidenceRole.Conversion, VariationTestResult.Converts) =>
        Some(LineSupportKind.Converts)
      case _ => None

  private def cautionKind(proof: PreparedVariationEvidence): Option[LineCautionKind] =
    (proof.role, proof.testResult) match
      case (VariationEvidenceRole.FailedTemptingMove, VariationTestResult.MovePremature) =>
        Some(LineCautionKind.EarlyMoveCaution)
      case (VariationEvidenceRole.PrematureMove, VariationTestResult.MovePremature) =>
        Some(LineCautionKind.PrematureMove)
      case (VariationEvidenceRole.ReleaseRisk, VariationTestResult.ReleasesCounterplay) =>
        Some(LineCautionKind.ReleasesCounterplay)
      case _ => None

  private def variationProofBoundToClaim(
      proof: PreparedVariationEvidence,
      claim: CommentaryClaim
  ): Boolean =
    proof.boundClaimId == claim.id &&
      claim.owner.contains(proof.owner) &&
      claim.anchor.contains(proof.anchor) &&
      claim.route.contains(proof.route) &&
      claim.scope.contains(proof.scope) &&
      proof.defender.forall(defender => claim.defender.contains(defender))

  private def isCandidatePrimary(proof: PreparedVariationEvidence): Boolean =
    proof.moveRole == VariationMoveRole.CandidateMove &&
      !isDefenderResourceOrReply(proof) &&
      !isNegativeProof(proof)

  private def isDefenderResourceOrReply(proof: PreparedVariationEvidence): Boolean =
    proof.role == VariationEvidenceRole.DefenderResource ||
      proof.moveRole == VariationMoveRole.DefenderResource

  private def isNegativeProof(proof: PreparedVariationEvidence): Boolean =
    proof.role == VariationEvidenceRole.FailedTemptingMove ||
      proof.role == VariationEvidenceRole.PrematureMove ||
      proof.role == VariationEvidenceRole.ReleaseRisk ||
      proof.testResult == VariationTestResult.MovePremature ||
      proof.testResult == VariationTestResult.ReleasesCounterplay ||
      proof.wordingCap == WordingStrength.NegativeOnly

  private def isSupportProof(proof: PreparedVariationEvidence): Boolean =
    !isCandidatePrimary(proof) &&
      !isDefenderResourceOrReply(proof) &&
      !isNegativeProof(proof)
