package lila.commentary.selection

class CandidateLineSelectionContractTest extends munit.FunSuite:

  private def assert(condition: => Boolean)(using loc: munit.Location): Unit =
    super.assert(condition, clues(""))

  private def assertEquals[A, B](obtained: A, expected: B)(using
      loc: munit.Location,
      compare: munit.Compare[A, B],
      diffOptions: munit.diff.DiffOptions
  ): Unit =
    super.assertEquals(obtained, expected, clues(""))

  private val validFen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"
  private val owner = Some("white")
  private val defender = Some("black")
  private val anchor = Some("board")
  private val route = Some("pressure_route")
  private val scope = Some("position_local")

  test("selects a strong primary line only when the lead exact-board claim has candidate and defender-resource proofs"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("candidate-main", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )

    val result = CandidateLineSelection.select(outline(lead = Some(claim)))

    assertEquals(result.primary.map(_.proof.proofId), Some("candidate-main"))
    assertEquals(result.primary.map(_.strength), Some(CandidateLineSelection.LineStrength.Strong))
    assertEquals(result.primary.toVector.flatMap(_.companionProofIds), Vector("defender-resource"))
    assertEquals(result.weakContextLines, Vector.empty)
    assertEquals(result.annotationSelection.map(_.claimId), Some("pressure-claim"))
    assertEquals(result.annotationSelection.map(_.primaryProofId), Some("candidate-main"))
    assertEquals(result.annotationSelection.toVector.flatMap(_.companionProofIds), Vector("defender-resource"))
    assertEquals(result.annotationSelection.map(_.strength), Some(PlanAnnotationStrength.Strong))

  test("does not select a strong line when only a candidate proof exists"):
    val claim = exactBoardClaim("pressure-claim", Vector(candidateProof("candidate-main", "pressure-claim")))

    val result = CandidateLineSelection.select(outline(lead = Some(claim)))

    assertEquals(result.primary, None)
    assertEquals(result.weakContextLines.map(_.proof.proofId), Vector("candidate-main"))
    assertEquals(result.weakContextLines.map(_.strength), Vector(CandidateLineSelection.LineStrength.WeakContext))
    assertEquals(CandidateLineSelection.publicVariationEvidenceFor(result), Vector.empty)
    assertEquals(result.annotationSelection, None)

  test("public line evidence requires strong primary and then keeps selected support and negative proofs deterministic"):
    val strongClaim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          candidateProof("candidate-main", "pressure-claim"),
          defenderProof("defender-resource", "pressure-claim"),
          supportProof("z-support", "pressure-claim"),
          failedProof("failed-proof", "pressure-claim")
        )
      )
    val weakClaim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          candidateProof("candidate-main", "pressure-claim"),
          supportProof("z-support", "pressure-claim"),
          failedProof("failed-proof", "pressure-claim")
        )
      )

    val strongResult = CandidateLineSelection.select(outline(lead = Some(strongClaim)))
    val weakResult = CandidateLineSelection.select(outline(lead = Some(weakClaim)))

    assertEquals(
      CandidateLineSelection.publicVariationEvidenceFor(strongResult).map(_.proofId),
      Vector("candidate-main", "defender-resource", "z-support", "failed-proof")
    )
    assertEquals(strongResult.primary.map(_.proof.proofId), Some("candidate-main"))
    assertEquals(weakResult.primary, None)
    assertEquals(CandidateLineSelection.publicVariationEvidenceFor(weakResult), Vector.empty)

  test("source-context and raw-engine owned proofs cannot become selected line owners"):
    val sourceOwned =
      sourceContextClaim("source-with-proof", SourceContextKind.Opening, Vector("opening-position:sample:canonical"))
        .copy(variationEvidence = Vector(candidateProof("source-owned-proof", "source-with-proof")))
    val rawEngineOwned =
      CommentaryClaim(
        id = "raw-engine-with-proof",
        layer = ClaimLayer.Engine,
        status = ClaimStatus.Admitted,
        variationEvidence = Vector(candidateProof("raw-engine-proof", "raw-engine-with-proof")),
        evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-engine-line"))
      )

    val result =
      CandidateLineSelection.select(
        outline(context = Vector(sourceOwned), support = Vector(rawEngineOwned))
      )

    assertEquals(result.primary, None)
    assertEquals(result.weakContextLines, Vector.empty)
    assert(result.decisions.exists(_.reason == CandidateLineSelection.DecisionReason.NonExactBoardOwner))

  test("source line-test refs annotate only a proof already selected from an exact-board claim"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("selected-proof", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )
    val contexts =
      Vector(
        sourceContextClaim("opening-context", SourceContextKind.Opening, Vector("opening-line-test:selected-proof:context")),
        sourceContextClaim("motif-context", SourceContextKind.Motif, Vector("motif-line-test:selected-proof:context")),
        sourceContextClaim("endgame-context", SourceContextKind.EndgameStudy, Vector("endgame-line-test:selected-proof:context")),
        sourceContextClaim("retrieval-context", SourceContextKind.Retrieval, Vector("retrieval-line-test:selected-proof:context"))
      )

    val result = CandidateLineSelection.select(outline(lead = Some(claim), context = contexts))

    assertEquals(result.primary.map(_.sourceContextRefs.map(_.id)), Some(contexts.flatMap(_.evidenceRefs.map(_.id))))
    assertEquals(result.sourceContext.map(_.proofId).distinct, Vector("selected-proof"))
    assertEquals(result.sourceContext.map(_.kind), contexts.map(_.sourceContextKind))
    assertEquals(
      result.annotationSelection.toVector.flatMap(_.sourceFrames.map(_.kind)),
      Vector(
        PlanAnnotationFrameKind.Opening,
        PlanAnnotationFrameKind.Motif,
        PlanAnnotationFrameKind.EndgameStudy,
        PlanAnnotationFrameKind.Retrieval
      )
    )
    assertEquals(
      result.annotationSelection.toVector.flatMap(_.sourceFrames.flatMap(_.sourceRefIds)),
      contexts.flatMap(_.evidenceRefs.map(_.id))
    )

  test("source line-test prefix must match the source-context family before it can frame a selected proof"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("selected-proof", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )
    val mismatchedRetrieval =
      sourceContextClaim("retrieval-with-opening-ref", SourceContextKind.Retrieval, Vector("opening-line-test:selected-proof:context"))
    val unknownFamily =
      sourceContextClaim("unknown-family-line-test", SourceContextKind.Opening, Vector("branch-id-line-test:selected-proof:context"))

    val directResult = CandidateLineSelection.select(outline(lead = Some(claim), context = Vector(mismatchedRetrieval, unknownFamily)))
    val selectedOutline = ClaimSelector.select(Vector(claim, mismatchedRetrieval, unknownFamily))

    assertEquals(directResult.sourceContext, Vector.empty)
    assertEquals(directResult.annotationSelection.toVector.flatMap(_.sourceFrames), Vector.empty)
    assertEquals(selectedOutline.context, Vector.empty)
    assertEquals(selectedOutline.annotationSelections.flatMap(_.sourceFrames), Vector.empty)

  test("internal-looking proof ids are rejected and not returned as public proof ids"):
    val unsafeProofId = "branch-id-cache-key-probe-payload"
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof(unsafeProofId, "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )

    val result = CandidateLineSelection.select(outline(lead = Some(claim)))
    val returnedProofIds =
      result.primary.toVector.map(_.proof.proofId) ++
        result.weakContextLines.map(_.proof.proofId) ++
        result.supportLines.map(_.proof.proofId) ++
        result.negativeLines.map(_.proof.proofId) ++
        result.sourceContext.map(_.proofId) ++
        result.decisions.flatMap(_.proofId)

    assertEquals(result.primary, None)
    assertEquals(result.weakContextLines, Vector.empty)
    assertEquals(result.supportLines, Vector.empty)
    assertEquals(result.negativeLines, Vector.empty)
    assertEquals(result.sourceContext, Vector.empty)
    assert(!returnedProofIds.contains(unsafeProofId), clues(returnedProofIds))
    assert(result.decisions.exists(_.reason == CandidateLineSelection.DecisionReason.UnsafeProof))

  test("public proof-id safety is shared by claim selection source line-tests and candidate line selection"):
    val unsafeProofId = "branch-id-cache-key-probe-payload"
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof(unsafeProofId, "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )
    val context = sourceContextClaim("opening-internal-line-test", SourceContextKind.Opening, Vector(s"opening-line-test:$unsafeProofId:context"))

    val selectedOutline = ClaimSelector.select(Vector(claim, context))
    val result = CandidateLineSelection.select(outline(lead = Some(claim), context = Vector(context)))

    assertEquals(PublicVariationEvidenceSafety.lineTestProofId(s"opening-line-test:$unsafeProofId:context"), None)
    assertEquals(PublicVariationEvidenceSafety.publicSafeForClaim(claim, claim.variationEvidence.head), false)
    assertEquals(selectedOutline.context, Vector.empty)
    assertEquals(selectedOutline.variationEvidence, Vector.empty)
    assertEquals(result.primary, None)
    assertEquals(CandidateLineSelection.publicVariationEvidenceFor(result), Vector.empty)

  test("line-test shaped raw-engine or exact-board refs do not source-context annotate a selected proof"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("selected-proof", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )
    val contextWithNonSourceRefs =
      sourceContextClaim("mixed-context", SourceContextKind.Opening, Vector("opening-line-test:missing-proof:context"))
        .copy(
          evidenceRefs = Vector(
            EvidenceRef(EvidenceRefKind.RawEngine, "opening-line-test:selected-proof:context"),
            EvidenceRef(EvidenceRefKind.ExactBoard, "motif-line-test:selected-proof:context"),
            EvidenceRef(EvidenceRefKind.RawEngine, "retrieval-line-test:unowned-proof:context")
          )
        )

    val result = CandidateLineSelection.select(outline(lead = Some(claim), context = Vector(contextWithNonSourceRefs)))

    assertEquals(result.primary.map(_.sourceContextRefs), Some(Vector.empty))
    assertEquals(result.sourceContext, Vector.empty)
    assert(!result.decisions.exists(_.reason == CandidateLineSelection.DecisionReason.SourceContextAnnotatedSelectedProof))
    assert(!result.decisions.exists(_.reason == CandidateLineSelection.DecisionReason.SourceContextUnownedProof))

  test("retrieval-only or source-only line-test candidate stays empty and silent"):
    val result =
      CandidateLineSelection.select(
        outline(
          context = Vector(
            sourceContextClaim("retrieval-only", SourceContextKind.Retrieval, Vector("retrieval-line-test:missing-proof:context")),
            sourceContextClaim("opening-only", SourceContextKind.Opening, Vector("opening-line-test:source-proof:context"))
          )
        )
      )

    assertEquals(result.primary, None)
    assertEquals(result.weakContextLines, Vector.empty)
    assertEquals(result.supportLines, Vector.empty)
    assertEquals(result.negativeLines, Vector.empty)
    assertEquals(result.sourceContext, Vector.empty)

  test("failed tempting premature and release-risk proofs cannot become primary"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          failedProof("failed-proof", "pressure-claim"),
          prematureProof("premature-proof", "pressure-claim"),
          releaseRiskProof("release-risk-proof", "pressure-claim")
        )
      )

    val result = CandidateLineSelection.select(outline(lead = Some(claim)))

    assertEquals(result.primary, None)
    assertEquals(result.negativeLines.map(_.proof.proofId), Vector("failed-proof", "premature-proof", "release-risk-proof"))

  test("public result order is deterministic and strips internal debug handles"):
    val noisyCandidate =
      candidateProof("candidate-main", "pressure-claim")
        .copy(debug = Some(PreparedVariationDebug(rawPacketId = Some("branch-id-cache-key-probe-payload"))))
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          supportProof("z-support", "pressure-claim"),
          defenderProof("defender-resource", "pressure-claim"),
          noisyCandidate,
          supportProof("a-support", "pressure-claim")
        )
      )

    val first = CandidateLineSelection.select(outline(lead = Some(claim)))
    val second = CandidateLineSelection.select(outline(lead = Some(claim.copy(variationEvidence = claim.variationEvidence.reverse))))
    val publicText = first.toString.toLowerCase

    assertEquals(first.primary.map(_.proof.proofId), second.primary.map(_.proof.proofId))
    assertEquals(first.supportLines.map(_.proof.proofId), Vector("a-support", "z-support"))
    assert(!publicText.contains("branch-id"), clues(publicText))
    assert(!publicText.contains("cache-key"), clues(publicText))
    assert(!publicText.contains("probe-payload"), clues(publicText))

  private def outline(
      lead: Option[CommentaryClaim] = None,
      support: Vector[CommentaryClaim] = Vector.empty,
      context: Vector[CommentaryClaim] = Vector.empty
  ): CommentaryOutline =
    val selectedLead = lead.map(claim => SelectedClaim(claim, ClaimBucket.ShouldLead))
    val selectedSupport = support.map(claim => SelectedClaim(claim, ClaimBucket.Support))
    val selectedContext = context.map(claim => SelectedClaim(claim, ClaimBucket.ContextOnly))
    CommentaryOutline(
      context = selectedContext,
      lead = selectedLead,
      support = selectedSupport,
      contrast = Vector.empty,
      suppressedClaims = Vector.empty,
      evidenceRefs = Vector.empty,
      variationEvidence = (selectedLead.toVector ++ selectedSupport ++ selectedContext).flatMap(_.claim.variationEvidence),
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

  private def exactBoardClaim(id: String, proofs: Vector[PreparedVariationEvidence]): CommentaryClaim =
    CommentaryClaim(
      id = id,
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = owner,
      beneficiary = owner,
      defender = defender,
      sideToMove = owner,
      anchor = anchor,
      route = route,
      scope = scope,
      impact = ClaimImpact(resultMaterialImpact = 60, persistenceAfterDefense = 80, evidenceConfidence = 80, boardExplainability = 70),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "CertifiedLine", owner, anchor, route, scope)),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport,
      variationEvidence = proofs
    )

  private def sourceContextClaim(id: String, kind: SourceContextKind, refs: Vector[String]): CommentaryClaim =
    CommentaryClaim(
      id = id,
      layer = ClaimLayer.SourceContext,
      status = ClaimStatus.Context,
      evidenceRefs = refs.map(ref => EvidenceRef(EvidenceRefKind.SourceContext, ref)),
      sourceContextKind = Some(kind),
      wordingStrengthCap = WordingStrength.ContextOnly
    )

  private def candidateProof(proofId: String, boundClaimId: String): PreparedVariationEvidence =
    baseProof(proofId, boundClaimId).copy(
      role = VariationEvidenceRole.Persistence,
      moveRole = VariationMoveRole.CandidateMove,
      testResult = VariationTestResult.PressurePersists,
      proves = "pressure_preserved",
      proofPurpose = VariationProofPurpose.PreservesPressure
    )

  private def defenderProof(proofId: String, boundClaimId: String): PreparedVariationEvidence =
    baseProof(proofId, boundClaimId).copy(
      role = VariationEvidenceRole.DefenderResource,
      moveRole = VariationMoveRole.DefenderResource,
      defenderResource = Some(VariationMove("...Qb6", "d8b6")),
      resourceLine = Vector(VariationMove("...Qb6", "d8b6"), VariationMove("Qd2", "d1d2")),
      testResult = VariationTestResult.DoesNotRestoreCounterplay,
      proves = "defender_resource_does_not_restore_counterplay",
      proofPurpose = VariationProofPurpose.DeniesResource
    )

  private def supportProof(proofId: String, boundClaimId: String): PreparedVariationEvidence =
    baseProof(proofId, boundClaimId).copy(
      role = VariationEvidenceRole.Conversion,
      moveRole = VariationMoveRole.Continuation,
      testResult = VariationTestResult.Converts,
      proves = "bounded_conversion_continues",
      proofPurpose = VariationProofPurpose.Simplifies
    )

  private def failedProof(proofId: String, boundClaimId: String): PreparedVariationEvidence =
    baseProof(proofId, boundClaimId).copy(
      role = VariationEvidenceRole.FailedTemptingMove,
      wordingCap = WordingStrength.NegativeOnly,
      testResult = VariationTestResult.MovePremature,
      proves = "tempting_move_fails",
      proofPurpose = VariationProofPurpose.Fails
    )

  private def prematureProof(proofId: String, boundClaimId: String): PreparedVariationEvidence =
    baseProof(proofId, boundClaimId).copy(
      role = VariationEvidenceRole.PrematureMove,
      wordingCap = WordingStrength.NegativeOnly,
      testResult = VariationTestResult.MovePremature,
      proves = "move_premature",
      proofPurpose = VariationProofPurpose.Fails
    )

  private def releaseRiskProof(proofId: String, boundClaimId: String): PreparedVariationEvidence =
    baseProof(proofId, boundClaimId).copy(
      role = VariationEvidenceRole.ReleaseRisk,
      testResult = VariationTestResult.ReleasesCounterplay,
      proves = "line_releases_counterplay",
      proofPurpose = VariationProofPurpose.ReleasesCounterplay
    )

  private def baseProof(proofId: String, boundClaimId: String): PreparedVariationEvidence =
    PreparedVariationEvidence(
      proofId = proofId,
      boundClaimId = boundClaimId,
      startFen = validFen,
      owner = "white",
      defender = defender,
      anchor = "board",
      route = "pressure_route",
      scope = "position_local",
      moveRole = VariationMoveRole.CandidateMove,
      lineSan = Vector("Nf6", "Ng5"),
      lineUci = Vector("g8f6", "f3g5"),
      candidateMove = Some(VariationMove("Nf6", "g8f6")),
      continuation = Vector(VariationMove("Ng5", "f3g5")),
      role = VariationEvidenceRole.Persistence,
      testedMove = Some(VariationMove("Nf6", "g8f6")),
      testedLine = Vector(VariationMove("Nf6", "g8f6"), VariationMove("Ng5", "f3g5")),
      replyLine = Vector(VariationMove("Ng5", "f3g5")),
      testResult = VariationTestResult.PressurePersists,
      proves = "pressure_preserved",
      proofPurpose = VariationProofPurpose.PreservesPressure,
      provenanceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "CertifiedLine", owner, anchor, route, scope)),
      boundary = PreparedVariationBoundary(
        depthFloor = 18,
        realizedDepth = 20,
        multiPv = 3,
        freshnessChecked = true,
        legalReplayChecked = true,
        baselineChecked = true
      ),
      wordingCap = WordingStrength.QualifiedSupport,
      surfaceAllowance = VariationSurfaceAllowance.PublicLine,
      publicSafe = true
    )
