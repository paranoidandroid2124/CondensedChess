package lila.commentary.selection

object CandidateLineSelection:

  enum LineStrength(val key: String):
    case Strong extends LineStrength("strong")
    case WeakContext extends LineStrength("weak_context")
    case SupportOnly extends LineStrength("support_only")
    case NegativeOnly extends LineStrength("negative_only")

  enum DecisionReason(val key: String):
    case StrongPrimarySelected extends DecisionReason("strong_primary_selected")
    case CandidateWithoutDefenderResource extends DecisionReason("candidate_without_defender_resource")
    case NonExactBoardOwner extends DecisionReason("non_exact_board_owner")
    case UnsafeProof extends DecisionReason("unsafe_proof")
    case SourceContextAnnotatedSelectedProof extends DecisionReason("source_context_annotated_selected_proof")
    case SourceContextUnownedProof extends DecisionReason("source_context_unowned_proof")
    case NegativeProofNotPrimary extends DecisionReason("negative_proof_not_primary")
    case EmptySelection extends DecisionReason("empty_selection")

  final case class SelectedLine(
      proof: PreparedVariationEvidence,
      boundClaimId: String,
      strength: LineStrength,
      companionProofIds: Vector[String] = Vector.empty,
      sourceContextRefs: Vector[EvidenceRef] = Vector.empty
  )

  final case class SourceContextLine(
      proofId: String,
      refs: Vector[EvidenceRef],
      kind: Option[SourceContextKind]
  )

  final case class Decision(
      proofId: Option[String],
      claimId: Option[String],
      reason: DecisionReason
  )

  final case class Result(
      primary: Option[SelectedLine],
      weakContextLines: Vector[SelectedLine],
      supportLines: Vector[SelectedLine],
      negativeLines: Vector[SelectedLine],
      sourceContext: Vector[SourceContextLine],
      decisions: Vector[Decision],
      publicVariationEvidence: Vector[PreparedVariationEvidence] = Vector.empty,
      annotationSelection: Option[PlanAnnotationSelection] = None
  )

  def publicVariationEvidenceFor(result: Result): Vector[PreparedVariationEvidence] =
    result.publicVariationEvidence

  def annotationSelectionsFor(result: Result): Vector[PlanAnnotationSelection] =
    result.annotationSelection.toVector

  def select(outline: CommentaryOutline): Result =
    val selectedBoardClaims = outline.lead.toVector ++ outline.support ++ outline.contrast
    val lineGroups = selectedBoardClaims.flatMap(selected => claimLines(selected.claim))
    val claimDecisions =
      selectedBoardClaims.flatMap(selected => claimLineDecisions(selected.claim))

    val leadLines =
      outline.lead.toVector.flatMap(selected => claimLines(selected.claim))
    val leadCandidateLines =
      leadLines.filter(line => isCandidateLine(line.proof)).sortBy(_.proof.proofId)
    val leadDefenderLines =
      leadLines.filter(line => isDefenderResourceLine(line.proof)).sortBy(_.proof.proofId)

    val primary =
      for
        candidate <- leadCandidateLines.headOption
        if leadDefenderLines.nonEmpty
      yield candidate.copy(
        strength = LineStrength.Strong,
        companionProofIds = leadDefenderLines.map(_.proof.proofId)
      )

    val weakLines =
      if primary.nonEmpty then Vector.empty
      else leadCandidateLines.map(_.copy(strength = LineStrength.WeakContext))

    val strongCompanionProofIds = primary.toVector.flatMap(_.companionProofIds).toSet
    val selectedProofIds =
      (primary.toVector ++
        weakLines ++
        leadDefenderLines.filter(line => strongCompanionProofIds.contains(line.proof.proofId)) ++
        lineGroups.filter(line => isSupportLine(line.proof) || isNegativeLine(line.proof)))
        .map(_.proof.proofId)
        .toSet
    val sourceContext = sourceContextLines(outline.context, selectedProofIds)
    val sourceRefsByProofId =
      sourceContext
        .groupMapReduce(_.proofId)(_.refs)(_ ++ _)
        .view
        .mapValues(_.distinct)
        .toMap

    val annotatedPrimary =
      primary.map(line => line.copy(sourceContextRefs = sourceRefsByProofId.getOrElse(line.proof.proofId, Vector.empty)))
    val annotatedWeak =
      weakLines.map(line => line.copy(sourceContextRefs = sourceRefsByProofId.getOrElse(line.proof.proofId, Vector.empty)))
    val primaryProofIds = annotatedPrimary.toVector.map(_.proof.proofId).toSet
    val weakProofIds = annotatedWeak.map(_.proof.proofId).toSet
    val supportLines =
      lineGroups
        .filter(line => isSupportLine(line.proof))
        .filterNot(line => primaryProofIds.contains(line.proof.proofId) || weakProofIds.contains(line.proof.proofId))
        .sortBy(_.proof.proofId)
        .map(line =>
          line.copy(
            strength = LineStrength.SupportOnly,
            sourceContextRefs = sourceRefsByProofId.getOrElse(line.proof.proofId, Vector.empty)
          )
        )
    val negativeLines =
      lineGroups
        .filter(line => isNegativeLine(line.proof))
        .sortBy(_.proof.proofId)
        .map(line =>
          line.copy(
            strength = LineStrength.NegativeOnly,
            sourceContextRefs = sourceRefsByProofId.getOrElse(line.proof.proofId, Vector.empty)
          )
        )

    val primaryDecision =
      annotatedPrimary match
        case Some(line) =>
          Vector(Decision(Some(line.proof.proofId), Some(line.boundClaimId), DecisionReason.StrongPrimarySelected))
        case None if leadCandidateLines.nonEmpty =>
          leadCandidateLines.map(line =>
            Decision(Some(line.proof.proofId), Some(line.boundClaimId), DecisionReason.CandidateWithoutDefenderResource)
          )
        case None => Vector.empty
    val sourceDecisions =
      sourceContext.map(line =>
        Decision(Some(line.proofId), None, DecisionReason.SourceContextAnnotatedSelectedProof)
      )
    val unownedSourceDecisions =
      unownedSourceContextProofIds(outline.context, selectedProofIds).map(proofId =>
        Decision(Some(proofId), None, DecisionReason.SourceContextUnownedProof)
      )
    val negativeDecisions =
      negativeLines.map(line =>
        Decision(Some(line.proof.proofId), Some(line.boundClaimId), DecisionReason.NegativeProofNotPrimary)
      )
    val emptyDecision =
      Option.when(annotatedPrimary.isEmpty && annotatedWeak.isEmpty && supportLines.isEmpty && negativeLines.isEmpty)(
        Decision(None, None, DecisionReason.EmptySelection)
      ).toVector
    val annotationSelection =
      annotatedPrimary.flatMap(line =>
        annotationSelectionFor(
          line = line,
          supportLines = supportLines,
          negativeLines = negativeLines,
          sourceContext = sourceContext,
          wordingCap = WordingStrength.weaker(line.proof.wordingCap, outline.wordingStrengthCap)
        )
      )

    Result(
      primary = annotatedPrimary,
      weakContextLines = annotatedWeak,
      supportLines = supportLines,
      negativeLines = negativeLines,
      sourceContext = sourceContext,
      decisions =
        (claimDecisions ++ primaryDecision ++ sourceDecisions ++ unownedSourceDecisions ++ negativeDecisions ++ emptyDecision)
          .distinct,
      publicVariationEvidence =
        annotatedPrimary match
          case Some(line) =>
            val companionProofIds = line.companionProofIds.toSet
            val companionProofs = leadDefenderLines.map(_.proof).filter(proof => companionProofIds.contains(proof.proofId))
            (Vector(line.proof) ++ companionProofs ++ supportLines.map(_.proof) ++ negativeLines.map(_.proof)).distinct
          case None => Vector.empty,
      annotationSelection = annotationSelection
    )

  private def annotationSelectionFor(
      line: SelectedLine,
      supportLines: Vector[SelectedLine],
      negativeLines: Vector[SelectedLine],
      sourceContext: Vector[SourceContextLine],
      wordingCap: WordingStrength
  ): Option[PlanAnnotationSelection] =
    val proofIds =
      Vector(line.proof.proofId) ++
        line.companionProofIds ++
        supportLines.map(_.proof.proofId) ++
        negativeLines.map(_.proof.proofId)
    Option.when(proofIds.forall(publicSafeProofId))(
      PlanAnnotationSelection(
        claimId = line.boundClaimId,
        primaryProofId = line.proof.proofId,
        companionProofIds = line.companionProofIds,
        supportProofIds = supportLines.map(_.proof.proofId),
        negativeProofIds = negativeLines.map(_.proof.proofId),
        sourceFrames = sourceFrames(sourceContext),
        strength = PlanAnnotationStrength.Strong,
        wordingCap = wordingCap
      )
    )

  private def sourceFrames(sourceContext: Vector[SourceContextLine]): Vector[PlanAnnotationFrame] =
    sourceContext.flatMap: line =>
      line.kind.map(kind =>
        PlanAnnotationFrame(
          kind = PlanAnnotationFrameKind.fromSourceContextKind(kind),
          proofId = line.proofId,
          sourceRefIds = line.refs.map(_.id)
        )
      )

  private def claimLines(claim: CommentaryClaim): Vector[SelectedLine] =
    if !isExactBoardLineOwner(claim) then Vector.empty
    else
      claim.variationEvidence
        .filter(publicSafeVariationEvidenceForClaim(claim, _))
        .map(proof =>
          SelectedLine(
            proof = publicProof(proof),
            boundClaimId = claim.id,
            strength = LineStrength.SupportOnly
          )
        )
        .sortBy(_.proof.proofId)

  private def claimLineDecisions(claim: CommentaryClaim): Vector[Decision] =
    if claim.variationEvidence.isEmpty then Vector.empty
    else if !isExactBoardLineOwner(claim) then
      claim.variationEvidence.map(proof =>
        Decision(publicDecisionProofId(proof.proofId), Some(claim.id), DecisionReason.NonExactBoardOwner)
      )
    else
      claim.variationEvidence.collect:
        case proof if !publicSafeVariationEvidenceForClaim(claim, proof) =>
          Decision(publicDecisionProofId(proof.proofId), Some(claim.id), DecisionReason.UnsafeProof)

  private def isExactBoardLineOwner(claim: CommentaryClaim): Boolean =
    claim.status == ClaimStatus.Admitted &&
      claim.exactBoardBound &&
      claim.layer != ClaimLayer.SourceContext &&
      claim.layer != ClaimLayer.Engine &&
      claim.layer != ClaimLayer.Renderer

  private def publicProof(proof: PreparedVariationEvidence): PreparedVariationEvidence =
    proof.copy(debug = None)

  private def isCandidateLine(proof: PreparedVariationEvidence): Boolean =
    proof.moveRole == VariationMoveRole.CandidateMove &&
      !isDefenderResourceLine(proof) &&
      !isNegativeLine(proof)

  private def isDefenderResourceLine(proof: PreparedVariationEvidence): Boolean =
    proof.role == VariationEvidenceRole.DefenderResource ||
      proof.moveRole == VariationMoveRole.DefenderResource ||
      proof.defenderResource.nonEmpty ||
      proof.resourceLine.nonEmpty

  private def isNegativeLine(proof: PreparedVariationEvidence): Boolean =
    proof.role == VariationEvidenceRole.FailedTemptingMove ||
      proof.role == VariationEvidenceRole.PrematureMove ||
      proof.role == VariationEvidenceRole.ReleaseRisk ||
      proof.testResult == VariationTestResult.MovePremature ||
      proof.testResult == VariationTestResult.ReleasesCounterplay ||
      proof.wordingCap == WordingStrength.NegativeOnly

  private def isSupportLine(proof: PreparedVariationEvidence): Boolean =
    !isCandidateLine(proof) && !isDefenderResourceLine(proof) && !isNegativeLine(proof)

  private def publicSafeVariationEvidenceForClaim(
      claim: CommentaryClaim,
      proof: PreparedVariationEvidence
  ): Boolean =
    PublicVariationEvidenceSafety.publicSafeForClaim(claim, proof)

  private def publicSafeProofId(proofId: String): Boolean =
    PublicVariationEvidenceSafety.publicSafeProofId(proofId)

  private def publicDecisionProofId(proofId: String): Option[String] =
    Option.when(publicSafeProofId(proofId))(proofId)

  private def sourceContextLines(
      context: Vector[SelectedClaim],
      selectedProofIds: Set[String]
  ): Vector[SourceContextLine] =
    context.flatMap: selected =>
      if !isSourceContextClaim(selected.claim) then Vector.empty
      else
        selected.claim.evidenceRefs.flatMap: ref =>
          Option.when(ref.kind == EvidenceRefKind.SourceContext)(ref)
            .flatMap(sourceRef => lineTestProofId(sourceRef.id, selected.claim.sourceContextKind).map(sourceRef -> _))
            .filter((_, proofId) => selectedProofIds.contains(proofId))
            .map((sourceRef, proofId) => SourceContextLine(proofId, Vector(sourceRef), selected.claim.sourceContextKind))

  private def unownedSourceContextProofIds(
      context: Vector[SelectedClaim],
      selectedProofIds: Set[String]
  ): Vector[String] =
    context
      .flatMap: selected =>
        if !isSourceContextClaim(selected.claim) then Vector.empty
        else
          selected.claim.evidenceRefs.flatMap: ref =>
            Option.when(ref.kind == EvidenceRefKind.SourceContext)(ref)
              .flatMap(sourceRef => lineTestProofId(sourceRef.id, selected.claim.sourceContextKind))
      .filterNot(selectedProofIds.contains)
      .distinct

  private def isSourceContextClaim(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.SourceContext &&
      claim.sourceContextKind.nonEmpty

  private def lineTestProofId(ref: String, kind: Option[SourceContextKind]): Option[String] =
    kind.flatMap(PublicVariationEvidenceSafety.lineTestProofIdForKind(ref, _))
