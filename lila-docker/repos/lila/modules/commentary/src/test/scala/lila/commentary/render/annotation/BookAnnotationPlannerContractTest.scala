package lila.commentary.render.annotation

import lila.commentary.selection.*

class BookAnnotationPlannerContractTest extends munit.FunSuite:

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

  test("strong exact-board lead with candidate and defender-resource proof creates one annotation unit"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("candidate-main", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )

    val annotation = BookAnnotationPlanner.plan(strongPlan(claim))

    assertEquals(annotation.units.size, 1)
    assertEquals(annotation.units.head.claimId, "pressure-claim")
    assertEquals(annotation.units.head.lineSan, Vector("Nf6", "Ng5"))
    assertEquals(annotation.units.head.lineUci, Vector("g8f6", "f3g5"))
    assertEquals(annotation.units.head.resourceLine.map(_.uci), Vector("d8b6", "d1d2"))
    assertEquals(annotation.units.head.replyLine.map(_.uci), Vector("f3g5"))
    assertEquals(annotation.units.head.proofRole, VariationEvidenceRole.Persistence)
    assertEquals(annotation.units.head.testResult, VariationTestResult.PressurePersists)
    assertEquals(annotation.units.head.wordingCap, WordingStrength.QualifiedSupport)
    assertEquals(annotation.units.head.proofIds.primaryProofId, "candidate-main")
    assertEquals(annotation.units.head.proofIds.companionProofIds, Vector("defender-resource"))
    assertEquals(annotation.boundaries, Vector.empty)
    assertEquals(annotation.wording.maxStrength, WordingStrength.QualifiedSupport)

  test("candidate-only proof produces no annotation unit"):
    val claim = exactBoardClaim("pressure-claim", Vector(candidateProof("candidate-main", "pressure-claim")))

    val annotation = BookAnnotationPlanner.plan(planWithMain(claim))

    assertEquals(annotation.units, Vector.empty)
    assert(annotation.boundaries.exists(_.reason == BookAnnotationBoundaryReason.MissingAnnotationSelection))

  test("defender-only proof produces no annotation unit"):
    val claim = exactBoardClaim("pressure-claim", Vector(defenderProof("defender-resource", "pressure-claim")))

    val annotation = BookAnnotationPlanner.plan(
      planWithMain(
        claim,
        variationEvidence = claim.variationEvidence,
        annotationSelections = Vector(
          PlanAnnotationSelection(
            claimId = "pressure-claim",
            primaryProofId = "defender-resource",
            companionProofIds = Vector.empty,
            supportProofIds = Vector.empty,
            negativeProofIds = Vector.empty,
            sourceFrames = Vector.empty,
            strength = PlanAnnotationStrength.Strong,
            wordingCap = WordingStrength.QualifiedSupport
          )
        )
      )
    )

    assertEquals(annotation.units, Vector.empty)
    assert(annotation.boundaries.exists(_.reason == BookAnnotationBoundaryReason.PrimaryProofNotCandidate))

  test("companion proof must be explicitly defender-resource owned"):
    val misleadingCompanion =
      candidateProof("misleading-resource-line", "pressure-claim").copy(
        resourceLine = Vector(BookAnnotationMove("...Qb6", "d8b6")).map(move => VariationMove(move.san, move.uci)),
        replyLine = Vector(VariationMove("Qd2", "d1d2")),
        proofPurpose = VariationProofPurpose.DeniesResource
      )
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("candidate-main", "pressure-claim"), misleadingCompanion)
      )

    val annotation = BookAnnotationPlanner.plan(
      planWithMain(
        claim,
        variationEvidence = claim.variationEvidence,
        annotationSelections = Vector(annotationSelection("pressure-claim", "candidate-main", Vector("misleading-resource-line")))
      )
    )

    assertEquals(annotation.units, Vector.empty)
    assert(annotation.boundaries.exists(_.reason == BookAnnotationBoundaryReason.CompanionProofNotDefenderResource))

  test("blocked selected claim produces no annotation unit"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("candidate-main", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )

    val annotation = BookAnnotationPlanner.plan(
      strongPlan(claim).copy(blocked = Vector(BlockedClaim(claim, Vector(SuppressionReason.ForbiddenShortcut))))
    )

    assertEquals(annotation.units, Vector.empty)
    assert(annotation.boundaries.exists(_.claimId.contains("pressure-claim")))
    assert(annotation.boundaries.exists(_.reason == BookAnnotationBoundaryReason.BlockedClaim))

  test("support and negative proof ids appear only when a strong primary unit exists"):
    val strongClaim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          candidateProof("candidate-main", "pressure-claim"),
          defenderProof("defender-resource", "pressure-claim"),
          supportProof("support-line", "pressure-claim"),
          failedProof("failed-line", "pressure-claim")
        )
      )
    val weakClaim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          candidateProof("candidate-main", "pressure-claim"),
          supportProof("support-line", "pressure-claim"),
          failedProof("failed-line", "pressure-claim")
        )
      )

    val strongAnnotation = BookAnnotationPlanner.plan(strongPlan(strongClaim))
    val weakAnnotation = BookAnnotationPlanner.plan(planWithMain(weakClaim))

    assertEquals(strongAnnotation.units.map(_.proofIds.supportProofIds), Vector(Vector("support-line")))
    assertEquals(strongAnnotation.units.map(_.proofIds.negativeProofIds), Vector(Vector("failed-line")))
    assertEquals(weakAnnotation.units.flatMap(_.supportingLines), Vector.empty)
    assertEquals(weakAnnotation.units.flatMap(_.cautionLines), Vector.empty)
    assertEquals(weakAnnotation.units, Vector.empty)
    assert(!weakAnnotation.toString.contains("support-line"))
    assert(!weakAnnotation.toString.contains("failed-line"))

  test("strong primary with support proof carries structured supporting line detail"):
    val supportLine =
      supportProof("support-line", "pressure-claim").copy(
        lineSan = Vector("Nxd5", "...exd5"),
        lineUci = Vector("f4d5", "e6d5"),
        testedMove = Some(VariationMove("Nxd5", "f4d5")),
        testedLine = Vector(VariationMove("Nxd5", "f4d5"), VariationMove("...exd5", "e6d5")),
        replyLine = Vector(VariationMove("...exd5", "e6d5")),
        resourceLine = Vector(VariationMove("...Qb6", "d8b6"))
      )
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          candidateProof("candidate-main", "pressure-claim"),
          defenderProof("defender-resource", "pressure-claim"),
          supportLine
        )
      )

    val annotation = BookAnnotationPlanner.plan(strongPlan(claim))
    val detail = annotation.units.head.supportingLines.head.detail

    assertEquals(annotation.units.head.supportingLines.map(_.kind), Vector(LineSupportKind.Converts))
    assertEquals(detail.lineSan, Vector("Nxd5", "...exd5"))
    assertEquals(detail.lineUci, Vector("f4d5", "e6d5"))
    assertEquals(detail.testedMove.map(_.uci), Some("f4d5"))
    assertEquals(detail.testedLine.map(_.uci), Vector("f4d5", "e6d5"))
    assertEquals(detail.replyLine.map(_.uci), Vector("e6d5"))
    assertEquals(detail.resourceLine.map(_.uci), Vector("d8b6"))
    assertEquals(annotation.units.head.cautionLines, Vector.empty)

  test("strong primary with negative proofs carries structured caution line detail"):
    val failedLine =
      failedProof("failed-line", "pressure-claim").copy(
        lineSan = Vector("Nxd5", "...exd5"),
        lineUci = Vector("f4d5", "e6d5"),
        testedMove = Some(VariationMove("Nxd5", "f4d5")),
        testedLine = Vector(VariationMove("Nxd5", "f4d5"), VariationMove("...exd5", "e6d5")),
        replyLine = Vector(VariationMove("...exd5", "e6d5"))
      )
    val prematureLine =
      prematureProof("premature-line", "pressure-claim").copy(
        lineSan = Vector("e5", "...dxe5"),
        lineUci = Vector("e4e5", "d6e5"),
        testedMove = Some(VariationMove("e5", "e4e5")),
        testedLine = Vector(VariationMove("e5", "e4e5"), VariationMove("...dxe5", "d6e5")),
        replyLine = Vector(VariationMove("...dxe5", "d6e5"))
      )
    val releaseLine =
      releaseRiskProof("release-risk-line", "pressure-claim").copy(
        lineSan = Vector("Bg5", "...h6"),
        lineUci = Vector("c1g5", "h7h6"),
        testedMove = Some(VariationMove("Bg5", "c1g5")),
        testedLine = Vector(VariationMove("Bg5", "c1g5"), VariationMove("...h6", "h7h6")),
        replyLine = Vector(VariationMove("...h6", "h7h6"))
      )
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          candidateProof("candidate-main", "pressure-claim"),
          defenderProof("defender-resource", "pressure-claim"),
          failedLine,
          prematureLine,
          releaseLine
        )
      )

    val annotation =
      BookAnnotationPlanner.plan(
        planWithMain(
          claim,
          variationEvidence = claim.variationEvidence,
          annotationSelections = Vector(
            annotationSelection(
              claimId = "pressure-claim",
              primaryProofId = "candidate-main",
              companionProofIds = Vector("defender-resource"),
              negativeProofIds = Vector("failed-line", "premature-line", "release-risk-line")
            )
          )
        )
      )
    val cautionLines = annotation.units.head.cautionLines
    val cautionSurface = cautionLines.toString.toLowerCase

    assertEquals(
      cautionLines.map(_.kind),
      Vector(LineCautionKind.EarlyMoveCaution, LineCautionKind.PrematureMove, LineCautionKind.ReleasesCounterplay)
    )
    assertEquals(cautionLines.map(_.detail.lineUci), Vector(Vector("f4d5", "e6d5"), Vector("e4e5", "d6e5"), Vector("c1g5", "h7h6")))
    assertEquals(cautionLines.flatMap(_.detail.testedMove.map(_.uci)), Vector("f4d5", "e4e5", "c1g5"))
    assert(!cautionSurface.contains("natural"), clues(cautionSurface))
    assert(!cautionSurface.contains("tempting"), clues(cautionSurface))
    assert(!cautionSurface.contains("source_ref"), clues(cautionSurface))
    assert(!cautionSurface.contains("raw"), clues(cautionSurface))

  test("missing support or caution tested move data fails closed for that detail"):
    val supportLine = supportProof("support-line", "pressure-claim").copy(testedMove = None)
    val failedLine = failedProof("failed-line", "pressure-claim").copy(testedMove = None)
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          candidateProof("candidate-main", "pressure-claim"),
          defenderProof("defender-resource", "pressure-claim"),
          supportLine,
          failedLine
        )
      )

    val annotation =
      BookAnnotationPlanner.plan(
        planWithMain(
          claim,
          variationEvidence = claim.variationEvidence,
          annotationSelections = Vector(
            annotationSelection(
              claimId = "pressure-claim",
              primaryProofId = "candidate-main",
              companionProofIds = Vector("defender-resource"),
              supportProofIds = Vector("support-line"),
              negativeProofIds = Vector("failed-line")
            )
          )
        )
      )

    assertEquals(annotation.units.head.proofIds.supportProofIds, Vector("support-line"))
    assertEquals(annotation.units.head.proofIds.negativeProofIds, Vector("failed-line"))
    assertEquals(annotation.units.head.supportingLines, Vector.empty)
    assertEquals(annotation.units.head.cautionLines, Vector.empty)

  test("unsafe support and caution proof ids fail closed through public safety gates"):
    val cases = Vector(
      (
        supportProof("support-boundary-only", "pressure-claim").copy(surfaceAllowance = VariationSurfaceAllowance.BoundaryOnly),
        BookAnnotationBoundaryReason.SupportProofUnsafe
      ),
      (
        supportProof("support-internal-only", "pressure-claim").copy(surfaceAllowance = VariationSurfaceAllowance.InternalOnly),
        BookAnnotationBoundaryReason.SupportProofUnsafe
      ),
      (
        supportProof("support-raw-provenance", "pressure-claim").copy(
          provenanceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "rawLine", owner, anchor, route, scope))
        ),
        BookAnnotationBoundaryReason.SupportProofUnsafe
      ),
      (
        failedProof("caution-source-provenance", "pressure-claim").copy(
          provenanceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "sourceLine", owner, anchor, route, scope))
        ),
        BookAnnotationBoundaryReason.NegativeProofUnsafe
      ),
      (
        failedProof("caution-stale-boundary", "pressure-claim").copy(
          boundary = baseProof("unused-boundary-copy", "pressure-claim").boundary.copy(freshnessChecked = false)
        ),
        BookAnnotationBoundaryReason.NegativeProofUnsafe
      ),
      (
        failedProof("caution-illegal-boundary", "pressure-claim").copy(
          boundary = baseProof("unused-boundary-copy", "pressure-claim").boundary.copy(legalReplayChecked = false)
        ),
        BookAnnotationBoundaryReason.NegativeProofUnsafe
      )
    )

    cases.foreach:
      case (unsafeProof, expectedReason) =>
      val claim =
        exactBoardClaim(
          "pressure-claim",
          Vector(candidateProof("candidate-main", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"), unsafeProof)
        )
      val annotation =
        BookAnnotationPlanner.plan(
          planWithMain(
            claim,
            variationEvidence = claim.variationEvidence,
            annotationSelections = Vector(
              annotationSelection(
                claimId = "pressure-claim",
                primaryProofId = "candidate-main",
                companionProofIds = Vector("defender-resource"),
                supportProofIds = Option.when(expectedReason == BookAnnotationBoundaryReason.SupportProofUnsafe)(unsafeProof.proofId).toVector,
                negativeProofIds = Option.when(expectedReason == BookAnnotationBoundaryReason.NegativeProofUnsafe)(unsafeProof.proofId).toVector
              )
            )
          )
        )

      assertEquals(annotation.units, Vector.empty)
      assert(
        annotation.boundaries.exists(boundary =>
          boundary.proofId.contains(unsafeProof.proofId) &&
            boundary.reason == expectedReason
        )
      )

  test("release-risk failed tempting and premature proofs cannot become primary unit"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(
          releaseRiskProof("release-risk-line", "pressure-claim"),
          failedProof("failed-line", "pressure-claim"),
          prematureProof("premature-line", "pressure-claim"),
          defenderProof("defender-resource", "pressure-claim")
        )
      )

    val annotations =
      Vector("release-risk-line", "failed-line", "premature-line").map: proofId =>
        BookAnnotationPlanner.plan(
          planWithMain(
            claim,
            variationEvidence = claim.variationEvidence,
            annotationSelections = Vector(
              PlanAnnotationSelection(
                claimId = "pressure-claim",
                primaryProofId = proofId,
                companionProofIds = Vector("defender-resource"),
                supportProofIds = Vector.empty,
                negativeProofIds = Vector.empty,
                sourceFrames = Vector.empty,
                strength = PlanAnnotationStrength.Strong,
                wordingCap = WordingStrength.NegativeOnly
              )
            )
          )
        )

    assertEquals(annotations.flatMap(_.units), Vector.empty)
    assert(annotations.forall(_.boundaries.exists(_.reason == BookAnnotationBoundaryReason.PrimaryProofNotCandidate)))

  test("source line-test matching selected proof adds frame metadata only"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("candidate-main", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )
    val plan = strongPlan(claim).copy(
      annotationSelections = Vector(
        annotationSelection(
          "pressure-claim",
          "candidate-main",
          Vector("defender-resource"),
          sourceFrames = Vector(
            PlanAnnotationFrame(
              kind = PlanAnnotationFrameKind.Opening,
              proofId = "candidate-main",
              sourceRefIds = Vector("opening-line-test:candidate-main:context")
            )
          )
        )
      )
    )

    val annotation = BookAnnotationPlanner.plan(plan)

    assertEquals(annotation.units.flatMap(_.sourceFrames.map(_.kind)), Vector(PlanAnnotationFrameKind.Opening))
    assertEquals(annotation.units.flatMap(_.sourceFrames.flatMap(_.sourceRefIds)), Vector("opening-line-test:candidate-main:context"))
    assertEquals(annotation.units.head.proofIds.primaryProofId, "candidate-main")

  test("unmatched source line-test fails closed"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("candidate-main", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )
    val unmatchedFrame =
      PlanAnnotationFrame(
        kind = PlanAnnotationFrameKind.Opening,
        proofId = "unmatched-proof",
        sourceRefIds = Vector("opening-line-test:unmatched-proof:context")
      )

    val withStrong = BookAnnotationPlanner.plan(
      strongPlan(claim).copy(
        annotationSelections = Vector(annotationSelection("pressure-claim", "candidate-main", Vector("defender-resource"), sourceFrames = Vector(unmatchedFrame)))
      )
    )
    val sourceOnly = BookAnnotationPlanner.plan(
      planWithMain(
        claim.copy(variationEvidence = Vector.empty),
        variationEvidence = Vector.empty,
        annotationSelections = Vector(annotationSelection("pressure-claim", "unmatched-proof", Vector.empty, sourceFrames = Vector(unmatchedFrame)))
      )
    )

    assertEquals(withStrong.units, Vector.empty)
    assert(withStrong.boundaries.exists(_.reason == BookAnnotationBoundaryReason.UnmatchedSourceFrame))
    assertEquals(sourceOnly.units, Vector.empty)
    assert(sourceOnly.boundaries.exists(_.reason == BookAnnotationBoundaryReason.PrimaryProofMissing))

  test("malformed or wrong-family source frame refs fail closed"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("candidate-main", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )
    val badFrames =
      Vector(
        PlanAnnotationFrame(
          kind = PlanAnnotationFrameKind.Retrieval,
          proofId = "candidate-main",
          sourceRefIds = Vector("opening-line-test:candidate-main:context")
        ),
        PlanAnnotationFrame(
          kind = PlanAnnotationFrameKind.Opening,
          proofId = "candidate-main",
          sourceRefIds = Vector("opening-line-test:defender-resource:context")
        ),
        PlanAnnotationFrame(
          kind = PlanAnnotationFrameKind.Opening,
          proofId = "candidate-main",
          sourceRefIds = Vector("branch-cache-source-row")
        )
      )

    badFrames.foreach: frame =>
      val annotation = BookAnnotationPlanner.plan(
        strongPlan(claim).copy(
          annotationSelections = Vector(annotationSelection("pressure-claim", "candidate-main", Vector("defender-resource"), sourceFrames = Vector(frame)))
        )
      )

      assertEquals(annotation.units, Vector.empty)
      assert(annotation.boundaries.exists(_.reason == BookAnnotationBoundaryReason.UnmatchedSourceFrame), clues(annotation))

  test("retrieval frame is illustrative and non-authoritative"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("candidate-main", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )
    val retrievalFrame =
      PlanAnnotationFrame(
        kind = PlanAnnotationFrameKind.Retrieval,
        proofId = "candidate-main",
        sourceRefIds = Vector("retrieval-line-test:candidate-main:context")
      )

    val annotation = BookAnnotationPlanner.plan(
      strongPlan(claim).copy(
        annotationSelections = Vector(annotationSelection("pressure-claim", "candidate-main", Vector("defender-resource"), sourceFrames = Vector(retrievalFrame)))
      )
    )

    assertEquals(annotation.units.flatMap(_.sourceFrames.map(_.kind)), Vector(PlanAnnotationFrameKind.Retrieval))
    assertEquals(annotation.units.flatMap(_.sourceFrames.map(_.authoritative)), Vector(false))

  test("planner output contains no final English prose or forbidden phrase fields"):
    val claim =
      exactBoardClaim(
        "pressure-claim",
        Vector(candidateProof("candidate-main", "pressure-claim"), defenderProof("defender-resource", "pressure-claim"))
      )

    val annotation = BookAnnotationPlanner.plan(strongPlan(claim))
    val productElementNames =
      (annotation.productElementNames.toVector ++
        annotation.units.head.productElementNames.toVector ++
        annotation.units.head.proofIds.productElementNames.toVector ++
        annotation.wording.productElementNames.toVector)
        .map(_.toLowerCase)
    val forbiddenFieldTokens = Vector("text", "publictext", "template", "sentence", "phrase")
    val forbiddenSurfaceTokens = Vector("best", "theory", "forced", "winning", "drawing", "engine says")
    val surface = annotation.toString.toLowerCase

    forbiddenFieldTokens.foreach: token =>
      assert(!productElementNames.exists(_.contains(token)), clues(productElementNames))
    forbiddenSurfaceTokens.foreach: token =>
      assert(!surface.contains(token), clues(surface))

  private def strongPlan(claim: CommentaryClaim): CommentaryPlan =
    planWithMain(
      claim,
      variationEvidence = claim.variationEvidence,
      annotationSelections = Vector(
        annotationSelection(
          claimId = claim.id,
          primaryProofId = claim.variationEvidence.find(_.moveRole == VariationMoveRole.CandidateMove).map(_.proofId).getOrElse("missing"),
          companionProofIds = claim.variationEvidence.filter(_.moveRole == VariationMoveRole.DefenderResource).map(_.proofId),
          supportProofIds = claim.variationEvidence.filter(_.proofId.startsWith("support")).map(_.proofId),
          negativeProofIds = claim.variationEvidence.filter(proof => proof.wordingCap == WordingStrength.NegativeOnly).map(_.proofId)
        )
      )
    )

  private def planWithMain(
      claim: CommentaryClaim,
      variationEvidence: Vector[PreparedVariationEvidence] = Vector.empty,
      annotationSelections: Vector[PlanAnnotationSelection] = Vector.empty
  ): CommentaryPlan =
    CommentaryPlan(
      main = Some(PlanSection(PlanRole.Main, Vector(SelectedClaim(claim, ClaimBucket.ShouldLead)))),
      support = PlanSection(PlanRole.Support, Vector.empty),
      context = PlanSection(PlanRole.Context, Vector.empty),
      contrast = PlanSection(PlanRole.Contrast, Vector.empty),
      blocked = Vector.empty,
      evidence = claim.evidenceRefs.map(PlanEvidence.apply),
      variationEvidence = variationEvidence.map(PlanVariationEvidence.apply),
      wordingRules = WordingRules(WordingStrength.QualifiedSupport),
      annotationSelections = annotationSelections
    )

  private def annotationSelection(
      claimId: String,
      primaryProofId: String,
      companionProofIds: Vector[String],
      supportProofIds: Vector[String] = Vector.empty,
      negativeProofIds: Vector[String] = Vector.empty,
      sourceFrames: Vector[PlanAnnotationFrame] = Vector.empty
  ): PlanAnnotationSelection =
    PlanAnnotationSelection(
      claimId = claimId,
      primaryProofId = primaryProofId,
      companionProofIds = companionProofIds,
      supportProofIds = supportProofIds,
      negativeProofIds = negativeProofIds,
      sourceFrames = sourceFrames,
      strength = PlanAnnotationStrength.Strong,
      wordingCap = WordingStrength.QualifiedSupport
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
