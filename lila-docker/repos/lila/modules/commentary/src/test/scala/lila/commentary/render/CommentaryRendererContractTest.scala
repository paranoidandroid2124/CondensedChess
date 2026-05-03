package lila.commentary.render

import lila.commentary.selection.*

import java.nio.file.{ Files, Paths }

class CommentaryRendererContractTest extends munit.FunSuite:

  private def assert(condition: => Boolean)(using loc: munit.Location): Unit =
    super.assert(condition, clues(""))

  private def assertEquals[A, B](obtained: A, expected: B)(using
      loc: munit.Location,
      compare: munit.Compare[A, B],
      diffOptions: munit.diff.DiffOptions
  ): Unit =
    super.assertEquals(obtained, expected, clues(""))

  test("renderer maps plan sections to stable public blocks without reranking"):
    val plan = planWith(
      main = Some(section(
        PlanRole.Main,
        selected(
          "main-s07",
          ClaimLayer.Projection,
          ClaimBucket.ShouldLead,
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Projection, "initiative_conversion_route_certified"))
        )
      )),
      support = section(
        PlanRole.Support,
        selected(
          "support-s21",
          ClaimLayer.Projection,
          ClaimBucket.Support,
          evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source"))
        )
      ),
      context = section(PlanRole.Context, selected("context-opening", ClaimLayer.SourceContext, ClaimBucket.ContextOnly)),
      contrast = section(PlanRole.Contrast, selected("contrast-s08", ClaimLayer.Projection, ClaimBucket.Support)),
      evidence = Vector(
        PlanEvidence(EvidenceRef(EvidenceRefKind.Projection, "initiative_conversion_route_certified")),
        PlanEvidence(EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source"))
      ),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.wording.maxStrength, WordingStrength.QualifiedSupport)
    assertEquals(render.blocks.map(_.role), Vector(RenderRole.Primary, RenderRole.Supporting, RenderRole.Context, RenderRole.Contrast))
    assertEquals(render.blocks.map(_.claimId), Vector("main-s07", "support-s21", "context-opening", "contrast-s08"))
    assertEquals(render.evidenceRefs.map(_.id), Vector("initiative_conversion_route_certified", "pawn_push_break_contact_source"))
    assertEquals(render.blocks.exists(_.text.publicText.nonEmpty), false)

  test("P2 renderer lowers selected claims to PublicClaim before public blocks"):
    val projectionRef =
      EvidenceRef(EvidenceRefKind.Projection, "king_entry_conversion_certified", Some("white"), Some("board"), Some("king_entry_conversion"), Some("exact_current_board"))
    val exactCarrier =
      EvidenceRef(EvidenceRefKind.ExactBoard, "k-s23-exact-board-carrier", Some("white"), Some("board"), Some("king_entry_conversion"), Some("exact_current_board"))
    val plan = planWith(
      main = Some(section(
        PlanRole.Main,
        selected(
          "projection-s23-white-board-king-entry",
          ClaimLayer.Projection,
          ClaimBucket.ShouldLead,
          evidenceRefs = Vector(projectionRef),
          lowerCarrierRefs = Vector(exactCarrier),
          wordingStrengthCap = WordingStrength.QualifiedSupport
        )
      )),
      evidence = Vector(PlanEvidence(projectionRef), PlanEvidence(exactCarrier)),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val publicClaims = CommentaryRendererContract.publicClaims(plan)
    val render = CommentaryRendererContract.render(plan)

    assertEquals(publicClaims.map(_.claimId), Vector("projection-s23-white-board-king-entry"))
    assertEquals(render.blocks.map(_.claimId), publicClaims.map(_.claimId))
    assertEquals(publicClaims.head.evidenceIds, Vector("king_entry_conversion_certified"))
    assertEquals(render.blocks.head.evidenceIds, publicClaims.head.evidenceIds)
    assert(publicClaims.head.phraseCapability.allowedPredicates.contains(PublicClaimPredicate.StrategyProjection))
    assertEquals(publicClaims.head.phraseCapability.allowsEngineLanguage, false)
    assertEquals(publicClaims.head.phraseCapability.allowsResultLanguage, false)
    assert(publicClaims.head.phraseCapability.forbiddenTerms.contains("engine says"))

  test("public surface template strips public text not authorized by phrase capability"):
    val plan = planWith(
      main = Some(section(PlanRole.Main, selected("main-s07", ClaimLayer.Projection, ClaimBucket.ShouldLead))),
      maxStrength = WordingStrength.QualifiedSupport
    )
    val publicPlan = CommentaryRendererContract.publicPlan(plan)
    val forgedTextPlan = publicPlan.copy(
      publicClaims = publicPlan.publicClaims.map(claim =>
        claim.copy(text = RenderText(Some("Primary fallback from raw evidence MaterialHarvest"), Vector.empty))
      )
    )

    val render = CommentaryRendererContract.render(forgedTextPlan)

    assertEquals(render.blocks.map(_.text.publicText), Vector(None))

  test("publicPlan render cannot turn raw PublicClaim text into phrase text even when line commentary is allowed"):
    val lead = selected("line-owner", ClaimLayer.Certification, ClaimBucket.MustLead)
    val proof =
      safeVariationProof("line-owner").copy(
        proofId = "candidate-main",
        lineSan = Vector("Nf6", "Ng5"),
        lineUci = Vector("g8f6", "f3g5"),
        candidateMove = Some(VariationMove("Nf6", "g8f6")),
        continuation = Vector(VariationMove("Ng5", "f3g5")),
        replyLine = Vector(VariationMove("Ng5", "f3g5"))
      )
    val plan = planWith(
      main = Some(section(PlanRole.Main, lead)),
      variationEvidence = Vector(PlanVariationEvidence(proof)),
      maxStrength = WordingStrength.QualifiedSupport
    )
    val publicPlan = CommentaryRendererContract.publicPlan(plan)
    val forgedTextPlan = publicPlan.copy(
      publicClaims = publicPlan.publicClaims.map(claim =>
        claim.copy(text = RenderText(Some("MaterialHarvest pressure route raw evidence prose"), Vector.empty))
      )
    )

    val render = CommentaryRendererContract.render(forgedTextPlan)

    assertEquals(publicPlan.publicClaims.head.phraseCapability.allowsLineCommentary, true)
    assertEquals(render.blocks.map(_.text.publicText), Vector(None))

  test("public render blocks carry phrase capability as surface permission metadata"):
    val plan = planWith(
      main = Some(section(PlanRole.Main, selected("main-s07", ClaimLayer.Projection, ClaimBucket.ShouldLead))),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRendererContract.render(plan)

    assert(render.blocks.head.productElementNames.toVector.contains("phraseCapability"))

  test("P2 renderer boundary does not public-lower terminal non-certified dispositions"):
    val terminalClaims =
      Vector(
        selectedStatus("support-only-projection", ClaimStatus.SupportOnly),
        selectedStatus("deferred-projection", ClaimStatus.Deferred),
        selectedStatus("anti-case-projection", ClaimStatus.AntiCase),
        selectedStatus("rejected-projection", ClaimStatus.Rejected)
      )
    val plan = planWith(
      main = Some(section(PlanRole.Main, terminalClaims.head)),
      support = PlanSection(PlanRole.Support, terminalClaims.tail),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val publicClaims = CommentaryRendererContract.publicClaims(plan)
    val render = CommentaryRendererContract.render(plan)

    assertEquals(publicClaims, Vector.empty)
    assertEquals(render.status, RenderStatus.NoCommentary)
    assertEquals(render.blocks, Vector.empty)

  test("minimal renderer does not turn role labels into public prose"):
    val plan = planWith(
      main = Some(section(PlanRole.Main, selected("main-s07", ClaimLayer.Projection, ClaimBucket.ShouldLead))),
      support = section(PlanRole.Support, selected("support-s21", ClaimLayer.Projection, ClaimBucket.Support)),
      context = section(PlanRole.Context, selected("context-opening", ClaimLayer.SourceContext, ClaimBucket.ContextOnly)),
      contrast = section(PlanRole.Contrast, selected("contrast-s08", ClaimLayer.Projection, ClaimBucket.Support)),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRenderer.render(plan)

    assertEquals(render.blocks.map(_.text.publicText), Vector(None, None, None, None))

  test("renderer uses safe English line commentary for annotated primary block only"):
    val lead = selected("line-owner", ClaimLayer.Certification, ClaimBucket.MustLead)
    val primaryProof =
      safeVariationProof("line-owner").copy(
        proofId = "candidate-main",
        lineSan = Vector("Nf6", "Ng5"),
        lineUci = Vector("g8f6", "f3g5"),
        candidateMove = Some(VariationMove("Nf6", "g8f6")),
        continuation = Vector(VariationMove("Ng5", "f3g5")),
        replyLine = Vector(VariationMove("Ng5", "f3g5"))
      )
    val defenderProof =
      safeVariationProof("line-owner").copy(
        proofId = "defender-resource",
        role = VariationEvidenceRole.DefenderResource,
        moveRole = VariationMoveRole.DefenderResource,
        lineSan = Vector("...Qb6", "Qd2"),
        lineUci = Vector("d8b6", "d1d2"),
        defenderResource = Some(VariationMove("...Qb6", "d8b6")),
        resourceLine = Vector(VariationMove("...Qb6", "d8b6"), VariationMove("Qd2", "d1d2")),
        replyLine = Vector(VariationMove("Ng5", "f3g5")),
        testResult = VariationTestResult.DoesNotRestoreCounterplay,
        proves = "defender_resource_does_not_restore_counterplay",
        proofPurpose = VariationProofPurpose.DeniesResource
      )
    val plan =
      planWith(
        main = Some(section(PlanRole.Main, lead)),
        variationEvidence = Vector(primaryProof, defenderProof).map(PlanVariationEvidence.apply),
        annotationSelections = Vector(
          PlanAnnotationSelection(
            claimId = "line-owner",
            primaryProofId = "candidate-main",
            companionProofIds = Vector("defender-resource"),
            supportProofIds = Vector.empty,
            negativeProofIds = Vector.empty,
            sourceFrames = Vector.empty,
            strength = PlanAnnotationStrength.Strong,
            wordingCap = WordingStrength.QualifiedSupport
          )
        ),
        maxStrength = WordingStrength.QualifiedSupport
      )

    val render = CommentaryRenderer.render(plan)
    val fallback = CommentaryRenderer.render(plan.copy(annotationSelections = Vector.empty))
    val publicClaims = CommentaryRendererContract.publicClaims(plan)

    assertEquals(
      render.blocks.map(_.text.publicText),
      Vector(Some("After Nf6 Ng5, ...Qb6 Qd2 is met by Ng5, and the pressure stays on."))
    )
    assertEquals(fallback.blocks.map(_.text.publicText), Vector(None))
    assertEquals(publicClaims.head.phraseCapability.allowsLineCommentary, true)

  test("renderer does not synthesize reason text from selected evidence ids"):
    val materialRef =
      EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("board"), Some("route"), Some("position_local"))
    val kingRef =
      EvidenceRef(EvidenceRefKind.Certification, "CertifiedKingSafetyEdge", Some("white"), Some("board"), Some("route"), Some("position_local"))
    val targetRef =
      EvidenceRef(EvidenceRefKind.Projection, "same_target_forcing_realization", Some("white"), Some("board"), Some("route"), Some("position_local"))
    val liabilityRef =
      EvidenceRef(EvidenceRefKind.Projection, "liability_relief_certified", Some("white"), Some("board"), Some("route"), Some("position_local"))
    val initiativeRef =
      EvidenceRef(EvidenceRefKind.Certification, "InitiativeWindow", Some("white"), Some("board"), Some("route"), Some("position_local"))
    val structureRef =
      EvidenceRef(EvidenceRefKind.Projection, "weak_pawn_target_pressure_persistence_certified", Some("white"), Some("d5"), Some("target_pressure"), Some("position_local"))
    val endgameRef =
      EvidenceRef(EvidenceRefKind.Projection, "fortress_hold_certified", Some("white"), Some("board"), Some("hold"), Some("position_local"))
    val activityRef =
      EvidenceRef(EvidenceRefKind.Projection, "mobility_domination_route_certified", Some("white"), Some("e5"), Some("mobility_plus_restriction"), Some("position_local"))

    def primaryTextFor(ref: EvidenceRef): Option[String] =
      val selectedClaim = selected(
        id = s"claim-${ref.id}",
        layer = if ref.kind == EvidenceRefKind.Projection then ClaimLayer.Projection else ClaimLayer.Certification,
        bucket = ClaimBucket.ShouldLead,
        evidenceRefs = Vector(ref),
        owner = ref.owner.getOrElse("white")
      )
      CommentaryRenderer
        .render(
          planWith(
            main = Some(section(PlanRole.Main, selectedClaim)),
            evidence = Vector(PlanEvidence(ref)),
            maxStrength = WordingStrength.QualifiedSupport
          )
        )
        .blocks
        .headOption
        .flatMap(_.text.publicText)

    Vector(
      materialRef,
      kingRef,
      targetRef,
      liabilityRef,
      initiativeRef,
      structureRef,
      endgameRef,
      activityRef,
      materialRef.copy(owner = Some("black"))
    ).foreach: ref =>
      assertEquals(primaryTextFor(ref), None)

  test("minimal renderer does not emit role fragments for negative-only blocks"):
    val plan = planWith(
      main = Some(section(PlanRole.Main, selected(
        "negative-only-main",
        ClaimLayer.Projection,
        ClaimBucket.ShouldLead,
        wordingStrengthCap = WordingStrength.NegativeOnly
      ))),
      maxStrength = WordingStrength.NegativeOnly
    )

    val render = CommentaryRenderer.render(plan)

    assertEquals(render.blocks.map(_.wordingStrength), Vector(WordingStrength.NegativeOnly))
    assertEquals(render.blocks.flatMap(_.text.publicText), Vector.empty)

  test("minimal renderer keeps noCommentary silent while preserving internal blocked metadata"):
    val blockedClaim = CommentaryClaim(
      id = "blocked-source-truth",
      layer = ClaimLayer.SourceContext,
      status = ClaimStatus.Rejected,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:catalan:best-move"))
    )
    val plan = planWith(
      blocked = Vector(BlockedClaim(blockedClaim, Vector(SuppressionReason.SourceContextOnly, SuppressionReason.ForbiddenShortcut))),
      evidence = Vector(PlanEvidence(EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:catalan:best-move"))),
      maxStrength = WordingStrength.Hidden
    )

    val render = CommentaryRenderer.render(plan)

    assertEquals(render.status, RenderStatus.NoCommentary)
    assertEquals(render.blocks, Vector.empty)
    assertEquals(render.evidenceRefs, Vector.empty)
    assertEquals(render.boundaries, Vector.empty)
    assertEquals(render.suppressions.map(_.claimId), Vector("blocked-source-truth"))
    assertEquals(render.suppressions.forall(!_.public), true)

  test("renderer does not expose blocked-only evidence when another public block renders"):
    val publicEvidence = EvidenceRef(EvidenceRefKind.ExactBoard, "exact-board-selected")
    val blockedEvidence = EvidenceRef(EvidenceRefKind.SourceContext, "blocked-opening-shortcut")
    val lead = selected(
      "safe-lead",
      ClaimLayer.Projection,
      ClaimBucket.ShouldLead,
      evidenceRefs = Vector(publicEvidence)
    )
    val blockedClaim = CommentaryClaim(
      id = "blocked-source-shortcut",
      layer = ClaimLayer.SourceContext,
      status = ClaimStatus.Rejected,
      evidenceRefs = Vector(blockedEvidence)
    )
    val plan = planWith(
      main = Some(section(PlanRole.Main, lead)),
      blocked = Vector(BlockedClaim(blockedClaim, Vector(SuppressionReason.SourceContextOnly, SuppressionReason.ForbiddenShortcut))),
      evidence = Vector(PlanEvidence(publicEvidence), PlanEvidence(blockedEvidence)),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.status, RenderStatus.Rendered)
    assertEquals(render.evidenceRefs.map(_.id), Vector("exact-board-selected"))
    assertEquals(render.blocks.head.evidenceIds, Vector("exact-board-selected"))
    assertEquals(render.suppressions.map(_.claimId), Vector("blocked-source-shortcut"))
    assertEquals(render.suppressions.forall(!_.public), true)

  test("renderer does not revive a claim that appears in both selected sections and blocked metadata"):
    val safeEvidence = EvidenceRef(EvidenceRefKind.ExactBoard, "safe-exact-board")
    val blockedEvidence = EvidenceRef(EvidenceRefKind.SourceContext, "blocked-conflict-ref")
    val blockedSelected = selected(
      "blocked-selected-conflict",
      ClaimLayer.SourceContext,
      ClaimBucket.ContextOnly,
      evidenceRefs = Vector(blockedEvidence),
      wordingStrengthCap = WordingStrength.ContextOnly
    )
    val blockedClaim = blockedSelected.claim.copy(status = ClaimStatus.Rejected, evidenceRefs = Vector.empty)
    val safeLead = selected(
      "safe-lead",
      ClaimLayer.Projection,
      ClaimBucket.ShouldLead,
      evidenceRefs = Vector(safeEvidence)
    )
    val plan = planWith(
      main = Some(section(PlanRole.Main, safeLead)),
      context = PlanSection(
        PlanRole.Context,
        Vector(blockedSelected),
        Vector(PlanBoundary(blockedSelected.claim.id, SuppressionReason.SourceContextOnly))
      ),
      blocked = Vector(BlockedClaim(blockedClaim, Vector(SuppressionReason.SourceContextOnly, SuppressionReason.ForbiddenShortcut))),
      evidence = Vector(PlanEvidence(safeEvidence), PlanEvidence(blockedEvidence)),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.blocks.map(_.claimId), Vector("safe-lead"))
    assertEquals(render.evidenceRefs.map(_.id), Vector("safe-exact-board"))
    assertEquals(render.boundaries, Vector.empty)
    assertEquals(render.suppressions.map(_.claimId), Vector("blocked-selected-conflict"))
    assertEquals(render.suppressions.forall(!_.public), true)

  test("minimal renderer keeps opening context as context and preserves separate source refs"):
    val opening = selected(
      "opening-context-master-online",
      ClaimLayer.SourceContext,
      ClaimBucket.ContextOnly,
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-source-use:master_reference"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-source-use:online_trend")
      ),
      softReasons = Vector(SuppressionReason.SourceContextOnly),
      wordingStrengthCap = WordingStrength.ContextOnly
    )
    val plan = planWith(
      context = PlanSection(PlanRole.Context, Vector(opening), Vector(PlanBoundary(opening.claim.id, SuppressionReason.SourceContextOnly))),
      evidence = opening.claim.evidenceRefs.map(PlanEvidence.apply),
      maxStrength = WordingStrength.ContextOnly
    )

    val render = CommentaryRenderer.render(plan)

    assertEquals(render.status, RenderStatus.ContextOnly)
    assertEquals(render.blocks.map(_.text.publicText), Vector(None))
    assertEquals(render.blocks.head.nonAuthoritative, true)
    assertEquals(render.evidenceRefs.map(_.id), Vector("opening-source-use:master_reference", "opening-source-use:online_trend"))
    assertEquals(render.evidenceRefs.exists(_.id.contains("merged")), false)

  test("renderer preserves blocked claims as internal non-public metadata"):
    val blockedClaim = CommentaryClaim(
      id = "raw-engine-shortcut",
      layer = ClaimLayer.Engine,
      status = ClaimStatus.Rejected,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-cp-plus-500"))
    )
    val plan = planWith(
      blocked = Vector(BlockedClaim(blockedClaim, Vector(SuppressionReason.RawEngineOnly, SuppressionReason.NoBoardReason))),
      evidence = Vector(PlanEvidence(EvidenceRef(EvidenceRefKind.RawEngine, "raw-cp-plus-500"))),
      maxStrength = WordingStrength.Hidden
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.status, RenderStatus.NoCommentary)
    assertEquals(render.blocks, Vector.empty)
    assertEquals(render.suppressions.map(_.claimId), Vector("raw-engine-shortcut"))
    assertEquals(render.suppressions.forall(!_.public), true)
    assertEquals(render.evidenceRefs.exists(_.kind == EvidenceRefKind.RawEngine), false)

  test("hidden noCommentary stays silent and does not emit fallback chess narration"):
    val plan = planWith(
      evidence = Vector(PlanEvidence(EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("board"), Some("route"), Some("position_local")))),
      maxStrength = WordingStrength.Hidden
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.status, RenderStatus.NoCommentary)
    assertEquals(render.blocks, Vector.empty)
    assertEquals(render.evidenceRefs, Vector.empty)
    assertEquals(render.wording.maxStrength, WordingStrength.Hidden)
    assertEquals(render.wording.allowedPublicText, false)

  test("wording cap is preserved for every executable strength"):
    WordingStrength.values.foreach: strength =>
      val plan = planWith(
        main = Option.when(strength.rank >= WordingStrength.QualifiedSupport.rank)(
          section(PlanRole.Main, selected("main-certified", ClaimLayer.Certification, ClaimBucket.MustLead, wordingStrengthCap = strength))
        ),
        context = Option.when(strength == WordingStrength.ContextOnly)(
          section(PlanRole.Context, selected("context-only", ClaimLayer.SourceContext, ClaimBucket.ContextOnly, wordingStrengthCap = WordingStrength.ContextOnly))
        ).getOrElse(PlanSection(PlanRole.Context, Vector.empty)),
        maxStrength = strength
      )

      val render = CommentaryRendererContract.render(plan)

      assertEquals(render.wording.maxStrength, strength)
      assert(render.blocks.forall(_.wordingStrength.rank <= strength.rank))

  test("renderer keeps source context non-authoritative and cannot create best or theory wording"):
    val opening = selected(
      "opening-context-master-online",
      ClaimLayer.SourceContext,
      ClaimBucket.ContextOnly,
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-source-use:master_reference"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-source-use:online_trend"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-boundary:no_specific_game_citation")
      ),
      softReasons = Vector(SuppressionReason.SourceContextOnly),
      wordingStrengthCap = WordingStrength.ContextOnly
    )
    val plan = planWith(
      context = PlanSection(PlanRole.Context, Vector(opening), Vector(PlanBoundary(opening.claim.id, SuppressionReason.SourceContextOnly))),
      evidence = opening.claim.evidenceRefs.map(ref => PlanEvidence(ref)),
      maxStrength = WordingStrength.ContextOnly
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.status, RenderStatus.ContextOnly)
    assertEquals(render.blocks.map(_.role), Vector(RenderRole.Context))
    assertEquals(render.blocks.head.nonAuthoritative, true)
    assertEquals(render.blocks.head.text.forbiddenTerms.contains("best"), true)
    assertEquals(render.blocks.head.text.forbiddenTerms.contains("theory"), true)
    assertEquals(render.evidenceRefs.map(_.id).filter(_.contains("opening-source-use")), Vector("opening-source-use:master_reference", "opening-source-use:online_trend"))
    assertEquals(render.evidenceRefs.exists(_.id.contains("merged")), false)

  test("renderer does not invent evidence from selected claim-local refs"):
    val lead = selected(
      "lead-with-claim-local-ref",
      ClaimLayer.Projection,
      ClaimBucket.ShouldLead,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Projection, "claim-local-ref"))
    )
    val plan = planWith(
      main = Some(section(PlanRole.Main, lead)),
      evidence = Vector(PlanEvidence(EvidenceRef(EvidenceRefKind.ExactBoard, "outline-owned-ref"))),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.evidenceRefs, Vector.empty)
    assertEquals(render.blocks.head.evidenceIds, Vector.empty)

  test("renderer exposes selected bounded board-reason lower carriers"):
    val boardReason =
      EvidenceRef(EvidenceRefKind.Delta, "capture_transition", Some("white"), Some("board"), Some("route"), Some("position_local"))
    val lead = selected(
      "lead-with-board-reason",
      ClaimLayer.Certification,
      ClaimBucket.MustLead,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("board"), Some("route"), Some("position_local"))),
      lowerCarrierRefs = Vector(boardReason),
      wordingStrengthCap = WordingStrength.AssertiveCertified
    )
    val plan = planWith(
      main = Some(section(PlanRole.Main, lead)),
      evidence = (lead.claim.evidenceRefs ++ lead.claim.lowerCarrierRefs).map(PlanEvidence.apply),
      maxStrength = WordingStrength.AssertiveCertified
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.evidenceRefs.map(_.kind), Vector(EvidenceRefKind.Certification, EvidenceRefKind.Delta))
    assertEquals(render.blocks.head.evidenceIds, Vector("MaterialHarvest", "capture_transition"))

  test("renderer does not attach plan evidence by id collision with wrong kind or binding"):
    val lead = selected(
      "lead-with-colliding-ref",
      ClaimLayer.Projection,
      ClaimBucket.ShouldLead,
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Projection, "colliding-ref", Some("white"), Some("board"), Some("route"), Some("position_local"))
      )
    )
    val plan = planWith(
      main = Some(section(PlanRole.Main, lead)),
      evidence = Vector(
        PlanEvidence(EvidenceRef(EvidenceRefKind.Certification, "colliding-ref", Some("black"), Some("other"), Some("other_route"), Some("position_local")))
      ),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.evidenceRefs, Vector.empty)
    assertEquals(render.blocks.head.evidenceIds, Vector.empty)

  test("renderer records engine certification refs only as bounded refs and filters raw engine refs"):
    val boardReason =
      EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val plan = planWith(
      main = Some(section(PlanRole.Main, selected(
        "certified-engine-owner",
        ClaimLayer.Certification,
        ClaimBucket.MustLead,
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("board"), Some("material_harvest"), Some("position_local")),
          EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("white"), Some("board"), Some("material_harvest"), Some("position_local")),
          boardReason
        ),
        wordingStrengthCap = WordingStrength.AssertiveCertified
      ))),
      evidence = Vector(
        PlanEvidence(EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))),
        PlanEvidence(EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))),
        PlanEvidence(boardReason),
        PlanEvidence(EvidenceRef(EvidenceRefKind.RawEngine, "raw-pv"))
      ),
      maxStrength = WordingStrength.AssertiveCertified
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.evidenceRefs.map(_.kind), Vector(EvidenceRefKind.Certification, EvidenceRefKind.EngineCertification, EvidenceRefKind.Delta))
    assertEquals(render.evidenceRefs.exists(_.kind == EvidenceRefKind.RawEngine), false)
    assertEquals(render.blocks.head.text.forbiddenTerms.contains("engine says"), true)
    assertEquals(render.blocks.head.text.forbiddenTerms.contains("forced"), true)
    assertEquals(render.blocks.head.text.forbiddenTerms.contains("best"), true)
    assertEquals(render.blocks.head.text.forbiddenTerms.contains("winning"), true)

  test("renderer filters EngineCertification refs without same-binding board reason"):
    val certification =
      EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val engine =
      EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val plan = planWith(
      main = Some(section(PlanRole.Main, selected(
        "opaque-engine-owner",
        ClaimLayer.Certification,
        ClaimBucket.MustLead,
        evidenceRefs = Vector(certification, engine),
        wordingStrengthCap = WordingStrength.AssertiveCertified
      ))),
      evidence = Vector(
        PlanEvidence(certification),
        PlanEvidence(engine),
        PlanEvidence(EvidenceRef(EvidenceRefKind.Object, "AttackScaffold", Some("white"), Some("other"), Some("material_harvest"), Some("position_local")))
      ),
      maxStrength = WordingStrength.AssertiveCertified
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.evidenceRefs.map(_.kind), Vector(EvidenceRefKind.Certification))
    assertEquals(render.blocks.head.evidenceIds, Vector("MaterialHarvest"))

  test("renderer does not expose EngineCertification with only generic exact-board lower carrier"):
    val certification =
      EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val engine =
      EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val exactBoard =
      EvidenceRef(EvidenceRefKind.ExactBoard, "certification-current-board", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val lead =
      selected(
        "exact-board-backed-engine-certification",
        ClaimLayer.Certification,
        ClaimBucket.MustLead,
        evidenceRefs = Vector(certification, engine),
        lowerCarrierRefs = Vector(exactBoard),
        wordingStrengthCap = WordingStrength.AssertiveCertified
      )
    val plan = planWith(
      main = Some(section(PlanRole.Main, lead)),
      evidence = Vector(PlanEvidence(certification), PlanEvidence(engine)),
      maxStrength = WordingStrength.AssertiveCertified
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.evidenceRefs.map(_.kind), Vector(EvidenceRefKind.Certification))
    assertEquals(render.blocks.head.evidenceIds, Vector("MaterialHarvest"))

  test("renderer filters unbounded Certification refs"):
    val boundedCertification =
      EvidenceRef(EvidenceRefKind.Certification, "bounded-cert", Some("white"), Some("board"), Some("route"), Some("position_local"))
    val plan = planWith(
      main = Some(section(PlanRole.Main, selected(
        "certified-owner",
        ClaimLayer.Certification,
        ClaimBucket.MustLead,
        evidenceRefs = Vector(boundedCertification),
        wordingStrengthCap = WordingStrength.AssertiveCertified
      ))),
      evidence = Vector(
        PlanEvidence(EvidenceRef(EvidenceRefKind.Certification, "unbounded-cert")),
        PlanEvidence(EvidenceRef(EvidenceRefKind.Certification, "missing-scope-cert", Some("white"), Some("board"), Some("route"), None)),
        PlanEvidence(boundedCertification)
      ),
      maxStrength = WordingStrength.AssertiveCertified
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.evidenceRefs.map(_.id), Vector("bounded-cert"))
    assertEquals(render.blocks.head.evidenceIds, Vector("bounded-cert"))

  test("renderer filters unbounded EngineCertification refs without same-binding Certification evidence"):
    val plan = planWith(
      main = Some(section(PlanRole.Main, selected("unbounded-engine-owner", ClaimLayer.Certification, ClaimBucket.MustLead, wordingStrengthCap = WordingStrength.AssertiveCertified))),
      evidence = Vector(
        PlanEvidence(EvidenceRef(EvidenceRefKind.EngineCertification, "unbounded-engine-evidence")),
        PlanEvidence(EvidenceRef(EvidenceRefKind.EngineCertification, "wrong-binding-engine-evidence", Some("white"), Some("board"), Some("route"), Some("position_local"))),
        PlanEvidence(EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("board"), Some("other_route"), Some("position_local")))
      ),
      maxStrength = WordingStrength.AssertiveCertified
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.evidenceRefs, Vector.empty)

  test("renderer does not expose plan-wide EngineCertification evidence through a non-certification public claim"):
    val certification =
      EvidenceRef(EvidenceRefKind.Certification, "MaterialHarvest", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val engine =
      EvidenceRef(EvidenceRefKind.EngineCertification, "bounded-engine-evidence", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val boardReason =
      EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", Some("white"), Some("board"), Some("material_harvest"), Some("position_local"))
    val projection = selected(
      "sxx-projection-with-engine-ref",
      ClaimLayer.Projection,
      ClaimBucket.ShouldLead,
      evidenceRefs = Vector(engine),
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )
    val plan = planWith(
      main = Some(section(PlanRole.Main, projection)),
      evidence = Vector(PlanEvidence(certification), PlanEvidence(engine), PlanEvidence(boardReason)),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.evidenceRefs, Vector.empty)
    assertEquals(render.blocks.head.evidenceIds, Vector.empty)

  test("renderer filters variation evidence whose binding disagrees with rendered claim"):
    val lead = selected("line-owner", ClaimLayer.Certification, ClaimBucket.MustLead)
    val wrongOwnerProof =
      safeVariationProof("line-owner").copy(
        owner = "black",
        provenanceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "CertifiedLine", Some("black"), Some("board"), Some("route"), Some("position_local")))
      )
    val wrongRouteProof =
      safeVariationProof("line-owner").copy(
        proofId = "line-proof-wrong-route",
        route = "other_route",
        provenanceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "CertifiedLine", Some("white"), Some("board"), Some("other_route"), Some("position_local")))
      )
    val plan = planWith(
      main = Some(section(PlanRole.Main, lead)),
      variationEvidence = Vector(PlanVariationEvidence(wrongOwnerProof), PlanVariationEvidence(wrongRouteProof)),
      maxStrength = WordingStrength.AssertiveCertified
    )

    val render = CommentaryRendererContract.render(plan)

    assertEquals(render.variationEvidence, Vector.empty)
    assertEquals(render.blocks.head.variationEvidenceIds, Vector.empty)

  test("renderer exposes only sanitized public line roles and no proof tokens for variation evidence"):
    val lead = selected("line-owner", ClaimLayer.Certification, ClaimBucket.MustLead)
    val proofs = Vector(
      safeVariationProof("line-owner").copy(
        proofId = "line-resource",
        role = VariationEvidenceRole.DefenderResource,
        moveRole = VariationMoveRole.DefenderResource,
        testResult = VariationTestResult.ResourceFails,
        proves = "resource_fails_in_line",
        proofPurpose = VariationProofPurpose.DeniesResource
      ),
      safeVariationProof("line-owner").copy(
        proofId = "line-failed",
        role = VariationEvidenceRole.FailedTemptingMove,
        testResult = VariationTestResult.MovePremature,
        proves = "tempting_move_fails",
        proofPurpose = VariationProofPurpose.Fails,
        wordingCap = WordingStrength.NegativeOnly
      ),
      safeVariationProof("line-owner").copy(
        proofId = "line-premature",
        role = VariationEvidenceRole.PrematureMove,
        testResult = VariationTestResult.MovePremature,
        proves = "move_is_premature",
        proofPurpose = VariationProofPurpose.Fails,
        wordingCap = WordingStrength.NegativeOnly
      ),
      safeVariationProof("line-owner").copy(
        proofId = "line-release",
        role = VariationEvidenceRole.ReleaseRisk,
        testResult = VariationTestResult.ReleasesCounterplay,
        proves = "line_releases_counterplay",
        proofPurpose = VariationProofPurpose.ReleasesCounterplay
      ),
      safeVariationProof("line-owner").copy(
        proofId = "line-hold",
        role = VariationEvidenceRole.Hold,
        testResult = VariationTestResult.DefensiveHold,
        proves = "defense_holds",
        proofPurpose = VariationProofPurpose.Holds
      ),
      safeVariationProof("line-owner").copy(
        proofId = "line-conversion",
        role = VariationEvidenceRole.Conversion,
        testResult = VariationTestResult.Converts,
        proves = "bounded_conversion_continues",
        proofPurpose = VariationProofPurpose.Simplifies
      ),
      safeVariationProof("line-owner").copy(
        proofId = "line-persistence",
        role = VariationEvidenceRole.Persistence,
        testResult = VariationTestResult.PressurePersists,
        proves = "pressure_preserved",
        proofPurpose = VariationProofPurpose.PreservesPressure
      ),
      safeVariationProof("line-owner").copy(
        proofId = "line-simplification",
        role = VariationEvidenceRole.Simplification,
        testResult = VariationTestResult.Simplifies,
        proves = "bounded_simplification",
        proofPurpose = VariationProofPurpose.Simplifies
      )
    )
    val plan = planWith(
      main = Some(section(PlanRole.Main, lead)),
      variationEvidence = proofs.map(PlanVariationEvidence.apply),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRendererContract.render(plan)
    val publicSurface =
      render.variationEvidence.map(_.role.key).mkString(" ") +
        render.variationEvidence.flatMap(_.productElementNames).mkString(" ") +
        render.variationEvidence.mkString(" ")

    assertEquals(
      render.variationEvidence.map(_.role),
      Vector(
        RenderLineRole.Resource,
        RenderLineRole.Caution,
        RenderLineRole.Caution,
        RenderLineRole.Caution,
        RenderLineRole.Hold,
        RenderLineRole.Conversion,
        RenderLineRole.Pressure,
        RenderLineRole.Simplification
      )
    )
    assert(!publicSurface.contains("failed_tempting_move"), clues(publicSurface))
    assert(!publicSurface.toLowerCase.contains("tempting"), clues(publicSurface))
    assert(!publicSurface.contains("proves"), clues(publicSurface))
    Vector(
      "startFen",
      "lineUci",
      "provenanceRefs",
      "boundary",
      "depthFloor",
      "realizedDepth",
      "multiPv",
      "g8f6",
      "f1b5"
    ).foreach(token => assert(!publicSurface.contains(token), clues(token, publicSurface)))

  test("renderer contract docs and surface corpus keep executable names"):
    val contractDoc = Files.readString(Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/CommentaryRendererContract.md"))
    val coreDoc = Files.readString(Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/CommentaryCoreSSOT.md"))
    val validationDoc = Files.readString(Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/ValidationMethodology.md"))
    val surfaceRows = Files.readString(Paths.get("modules/commentary/src/test/resources/commentary-corpus/surface-expectations.jsonl"))
    val requiredTokens = Vector(
      "CommentaryRenderer",
      "CommentaryRender",
      "RenderBlock",
      "RenderRole",
      "RenderStatus",
      "RenderLineRole",
      "RenderText",
      "RenderEvidenceRef",
      "RenderBoundary",
      "RenderSuppression",
      "RenderWording",
      "PublicClaim",
      "PublicClaimPredicate",
      "PhraseCapability",
      "PublicSurfaceTemplate",
      "PublicPhrase",
      "PublicCommentaryPlan",
      "EnglishLineCommentary",
      "EnglishLineCommentaryWriter",
      "EnglishLineCommentaryContractTest.scala",
      "CommentaryPlan only",
      "RawEngine",
      "master_reference",
      "online_trend"
    )

    requiredTokens.foreach: token =>
      assert(contractDoc.contains(token), clues(token))
    assert(coreDoc.contains("CommentaryRendererContract.md"))
    assert(validationDoc.contains("CommentaryRendererContractTest.scala"))
    assert(surfaceRows.contains("surface-renderer-plan-only-input"))
    assert(surfaceRows.contains("surface-renderer-blocked-internal-only"))
    assert(surfaceRows.contains("surface-renderer-no-commentary-silent"))
    assert(surfaceRows.contains("surface-renderer-no-evidence-invention"))
    assert(surfaceRows.contains("surface-renderer-raw-engine-filtered"))
    assert(surfaceRows.contains("surface-renderer-no-role-label-public-text"))
    assert(surfaceRows.contains("surface-renderer-blocked-selected-conflict-denied"))
    assert(surfaceRows.contains("surface-renderer-engine-certification-board-reason-required"))
    assert(surfaceRows.contains("surface-renderer-plan-wide-engine-evidence-not-public"))
    assertEquals(RenderStatus.values.map(_.key).toVector, Vector("rendered", "contextOnly", "noCommentary"))

  private def planWith(
      main: Option[PlanSection] = None,
      support: PlanSection = PlanSection(PlanRole.Support, Vector.empty),
      context: PlanSection = PlanSection(PlanRole.Context, Vector.empty),
      contrast: PlanSection = PlanSection(PlanRole.Contrast, Vector.empty),
      blocked: Vector[BlockedClaim] = Vector.empty,
      evidence: Vector[PlanEvidence] = Vector.empty,
      variationEvidence: Vector[PlanVariationEvidence] = Vector.empty,
      annotationSelections: Vector[PlanAnnotationSelection] = Vector.empty,
      maxStrength: WordingStrength
  ): CommentaryPlan =
    CommentaryPlan(
      main = main,
      support = support,
      context = context,
      contrast = contrast,
      blocked = blocked,
      evidence = evidence,
      variationEvidence = variationEvidence,
      wordingRules = WordingRules(maxStrength),
      annotationSelections = annotationSelections
    )

  private def section(role: PlanRole, claims: SelectedClaim*): PlanSection =
    PlanSection(role, claims.toVector)

  private def selected(
      id: String,
      layer: ClaimLayer,
      bucket: ClaimBucket,
      evidenceRefs: Vector[EvidenceRef] = Vector.empty,
      lowerCarrierRefs: Vector[EvidenceRef] = Vector.empty,
      softReasons: Vector[SuppressionReason] = Vector.empty,
      wordingStrengthCap: WordingStrength = WordingStrength.QualifiedSupport,
      owner: String = "white"
  ): SelectedClaim =
    SelectedClaim(
      CommentaryClaim(
        id = id,
        layer = layer,
        status = if layer == ClaimLayer.SourceContext then ClaimStatus.Context else ClaimStatus.Admitted,
        owner = Option.when(layer != ClaimLayer.SourceContext)(owner),
        beneficiary = Option.when(layer != ClaimLayer.SourceContext)(owner),
        defender = Option.when(layer != ClaimLayer.SourceContext)(if owner == "white" then "black" else "white"),
        sideToMove = Option.when(layer != ClaimLayer.SourceContext)("white"),
        anchor = Option.when(layer != ClaimLayer.SourceContext)("board"),
        route = Option.when(layer != ClaimLayer.SourceContext)("route"),
        scope = Option.when(layer != ClaimLayer.SourceContext)("position_local"),
        evidenceRefs = evidenceRefs,
        lowerCarrierRefs = lowerCarrierRefs,
        exactBoardBound = layer != ClaimLayer.SourceContext,
        wordingStrengthCap = wordingStrengthCap,
        sourceContextKind = Option.when(layer == ClaimLayer.SourceContext)(SourceContextKind.Opening)
      ),
      bucket,
      softReasons
    )

  private def selectedStatus(id: String, status: ClaimStatus): SelectedClaim =
    SelectedClaim(
      CommentaryClaim(
        id = id,
        layer = ClaimLayer.Projection,
        status = status,
        band = Some("S23"),
        owner = Some("white"),
        beneficiary = Some("white"),
        defender = Some("black"),
        sideToMove = Some("white"),
        anchor = Some("board"),
        route = Some("king_entry_conversion"),
        scope = Some("exact_current_board"),
        evidenceRefs = Vector(
          EvidenceRef(EvidenceRefKind.Projection, "king_entry_conversion_certified", Some("white"), Some("board"), Some("king_entry_conversion"), Some("exact_current_board"))
        ),
        lowerCarrierRefs = Vector(
          EvidenceRef(EvidenceRefKind.ExactBoard, "k-s23-exact-board-carrier", Some("white"), Some("board"), Some("king_entry_conversion"), Some("exact_current_board"))
        ),
        exactBoardBound = true,
        wordingStrengthCap = WordingStrength.QualifiedSupport
      ),
      ClaimBucket.ShouldLead
    )

  private def safeVariationProof(boundClaimId: String): PreparedVariationEvidence =
    PreparedVariationEvidence(
      proofId = "line-proof-safe",
      boundClaimId = boundClaimId,
      startFen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3",
      owner = "white",
      defender = Some("black"),
      anchor = "board",
      route = "route",
      scope = "position_local",
      moveRole = VariationMoveRole.CandidateMove,
      lineSan = Vector("Nf6", "Bb5"),
      lineUci = Vector("g8f6", "f1b5"),
      candidateMove = Some(VariationMove("Nf6", "g8f6")),
      continuation = Vector(VariationMove("Bb5", "f1b5")),
      role = VariationEvidenceRole.Persistence,
      testedMove = Some(VariationMove("Nf6", "g8f6")),
      testedLine = Vector(VariationMove("Nf6", "g8f6"), VariationMove("Bb5", "f1b5")),
      testResult = VariationTestResult.PressurePersists,
      proves = "pressure_preserved",
      proofPurpose = VariationProofPurpose.PreservesPressure,
      boundary = PreparedVariationBoundary(
        depthFloor = 16,
        realizedDepth = 18,
        multiPv = 2,
        freshnessChecked = true,
        legalReplayChecked = true,
        baselineChecked = false
      ),
      wordingCap = WordingStrength.QualifiedSupport,
      provenanceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "CertifiedLine", Some("white"), Some("board"), Some("route"), Some("position_local"))),
      surfaceAllowance = VariationSurfaceAllowance.PublicLine,
      publicSafe = true
    )
