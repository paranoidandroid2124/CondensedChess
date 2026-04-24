package lila.commentary.selection

class CommentaryOutlineBuilderContractTest extends munit.FunSuite:

  private def assert(condition: => Boolean)(using loc: munit.Location): Unit =
    super.assert(condition, clues(""))

  private def assertEquals[A, B](obtained: A, expected: B)(using
      loc: munit.Location,
      compare: munit.Compare[A, B],
      diffOptions: munit.diff.DiffOptions
  ): Unit =
    super.assertEquals(obtained, expected, clues(""))

  test("lead maps to main and support contrast map without reranking"):
    val lead = selectedClaim("lead-s07", ClaimLayer.Projection, ClaimBucket.ShouldLead)
    val support = selectedClaim("support-s21", ClaimLayer.Projection, ClaimBucket.Support)
    val contrast = selectedClaim("contrast-s08", ClaimLayer.Projection, ClaimBucket.Support)
    val outline = outlineWith(
      lead = Some(lead),
      support = Vector(support),
      contrast = Vector(contrast),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Projection, "initiative_conversion_route_certified"),
        EvidenceRef(EvidenceRefKind.Witness, "pawn_push_break_contact_source")
      ),
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

    val plan = CommentaryOutlineBuilder.build(outline)

    assertEquals(plan.main.map(_.role), Some(PlanRole.Main))
    assertEquals(plan.main.toVector.flatMap(_.claims).map(_.claim.id), Vector("lead-s07"))
    assertEquals(plan.support.role, PlanRole.Support)
    assertEquals(plan.support.claims.map(_.claim.id), Vector("support-s21"))
    assertEquals(plan.contrast.role, PlanRole.Contrast)
    assertEquals(plan.contrast.claims.map(_.claim.id), Vector("contrast-s08"))
    assertEquals(plan.wordingRules.maxStrength, WordingStrength.QualifiedSupport)

  test("context maps to context with soft source reasons as boundary metadata"):
    val context = selectedClaim(
      "opening-context",
      ClaimLayer.SourceContext,
      ClaimBucket.ContextOnly,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:catalan:canonical")),
      softReasons = Vector(SuppressionReason.SourceContextOnly)
    )
    val outline = outlineWith(
      context = Vector(context),
      evidenceRefs = context.claim.evidenceRefs,
      wordingStrengthCap = WordingStrength.ContextOnly
    )

    val plan = CommentaryOutlineBuilder.build(outline)

    assertEquals(plan.main, None)
    assertEquals(plan.context.role, PlanRole.Context)
    assertEquals(plan.context.claims.map(_.claim.id), Vector("opening-context"))
    assertEquals(
      plan.context.boundaries,
      Vector(PlanBoundary("opening-context", SuppressionReason.SourceContextOnly))
    )
    assertEquals(plan.wordingRules.maxStrength, WordingStrength.ContextOnly)

  test("suppressed claims map to blocked do-not-say entries and are not revived"):
    val rawEngine = CommentaryClaim(
      id = "raw-engine-shortcut",
      layer = ClaimLayer.Engine,
      status = ClaimStatus.Rejected,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-cp-plus-500"))
    )
    val sourcePromotion = CommentaryClaim(
      id = "source-truth-promotion",
      layer = ClaimLayer.SourceContext,
      status = ClaimStatus.Rejected,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "opening-best-move-forbidden"))
    )
    val outline = outlineWith(
      suppressedClaims = Vector(
        SuppressedClaim(rawEngine, Vector(SuppressionReason.RawEngineOnly, SuppressionReason.NoBoardReason)),
        SuppressedClaim(sourcePromotion, Vector(SuppressionReason.SourceContextOnly, SuppressionReason.ForbiddenShortcut))
      ),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-cp-plus-500")),
      wordingStrengthCap = WordingStrength.Hidden
    )

    val plan = CommentaryOutlineBuilder.build(outline)

    assertEquals(plan.main, None)
    assertEquals(plan.support.claims, Vector.empty)
    assertEquals(plan.blocked.map(_.claim.id), Vector("raw-engine-shortcut", "source-truth-promotion"))
    assertEquals(plan.blocked.head.reasons, Vector(SuppressionReason.RawEngineOnly, SuppressionReason.NoBoardReason))
    assertEquals(plan.wordingRules.maxStrength, WordingStrength.Hidden)

  test("context-only outline remains no-main context-only plan"):
    val context = selectedClaim(
      "retrieval-example",
      ClaimLayer.SourceContext,
      ClaimBucket.ContextOnly,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "retrieval-example:lucena")),
      softReasons = Vector(SuppressionReason.SourceContextOnly, SuppressionReason.RetrievalNonAuthoritative)
    )
    val outline = outlineWith(
      context = Vector(context),
      evidenceRefs = context.claim.evidenceRefs,
      wordingStrengthCap = WordingStrength.ContextOnly
    )

    val plan = CommentaryOutlineBuilder.build(outline)

    assertEquals(plan.main, None)
    assertEquals(plan.context.claims.map(_.bucket), Vector(ClaimBucket.ContextOnly))
    assertEquals(plan.wordingRules.maxStrength, WordingStrength.ContextOnly)
    assertEquals(plan.noCommentary, false)

  test("empty outline remains no-commentary plan while blocked claims can still be preserved"):
    val blocked = CommentaryClaim(
      id = "blocked-only-raw-engine",
      layer = ClaimLayer.Engine,
      status = ClaimStatus.Rejected,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-pv"))
    )
    val outline = outlineWith(
      suppressedClaims = Vector(
        SuppressedClaim(blocked, Vector(SuppressionReason.RawEngineOnly, SuppressionReason.NoBoardReason))
      ),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-pv")),
      wordingStrengthCap = WordingStrength.Hidden
    )

    val plan = CommentaryOutlineBuilder.build(outline)

    assertEquals(plan.main, None)
    assertEquals(plan.support.claims, Vector.empty)
    assertEquals(plan.context.claims, Vector.empty)
    assertEquals(plan.contrast.claims, Vector.empty)
    assertEquals(plan.noCommentary, true)
    assertEquals(plan.blocked.map(_.claim.id), Vector("blocked-only-raw-engine"))
    assertEquals(plan.evidence.map(_.ref.kind), Vector(EvidenceRefKind.RawEngine))
    assertEquals(plan.wordingRules.maxStrength, WordingStrength.Hidden)

  test("renderer cap request cannot be upgraded by builder"):
    val lead = selectedClaim(
      "qualified-lead",
      ClaimLayer.Projection,
      ClaimBucket.ShouldLead,
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )
    val outline = outlineWith(
      lead = Some(lead),
      suppressedClaims = Vector(
        SuppressedClaim(
          CommentaryClaim(
            id = "renderer-wording-upgrade",
            layer = ClaimLayer.Renderer,
            status = ClaimStatus.Rejected,
            wordingStrengthCap = WordingStrength.AssertiveCertified
          ),
          Vector(SuppressionReason.RendererNotAllowed)
        )
      ),
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

    val plan = CommentaryOutlineBuilder.build(outline)

    assertEquals(plan.wordingRules.maxStrength, WordingStrength.QualifiedSupport)
    assertEquals(plan.blocked.map(_.claim.id), Vector("renderer-wording-upgrade"))
    assertEquals(plan.main.toVector.flatMap(_.claims).map(_.claim.wordingStrengthCap), Vector(WordingStrength.QualifiedSupport))

  test("opening context evidence is preserved as references without merged ranking or truth promotion"):
    val opening = selectedClaim(
      "open-context-catalan-master-online",
      ClaimLayer.SourceContext,
      ClaimBucket.ContextOnly,
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-context-candidate:open-context-catalan-master-online"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-source-use:master_reference"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-source-use:online_trend"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-boundary:no_specific_game_citation")
      ),
      softReasons = Vector(SuppressionReason.SourceContextOnly)
    )
    val outline = outlineWith(
      context = Vector(opening),
      evidenceRefs = opening.claim.evidenceRefs,
      wordingStrengthCap = WordingStrength.ContextOnly
    )

    val plan = CommentaryOutlineBuilder.build(outline)

    assertEquals(plan.main, None)
    assertEquals(plan.context.claims.map(_.claim.id), Vector("open-context-catalan-master-online"))
    assertEquals(plan.evidence.map(_.ref), opening.claim.evidenceRefs)
    assert(!plan.evidence.exists(_.ref.id.contains("merged")))
    assert(!plan.evidence.exists(_.ref.id.contains("best")))
    assertEquals(plan.wordingRules.maxStrength, WordingStrength.ContextOnly)

  test("evidence refs are copied from outline and not recomputed from claims"):
    val lead = selectedClaim(
      "lead-with-extra-claim-ref",
      ClaimLayer.Projection,
      ClaimBucket.ShouldLead,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Projection, "claim-local-ref"))
    )
    val outlineEvidence = Vector(EvidenceRef(EvidenceRefKind.ExactBoard, "outline-owned-ref"))
    val outline = outlineWith(
      lead = Some(lead),
      evidenceRefs = outlineEvidence,
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

    val plan = CommentaryOutlineBuilder.build(outline)

    assertEquals(plan.evidence.map(_.ref), outlineEvidence)
    assert(!plan.evidence.exists(_.ref.id == "claim-local-ref"))

  private def selectedClaim(
      id: String,
      layer: ClaimLayer,
      bucket: ClaimBucket,
      evidenceRefs: Vector[EvidenceRef] = Vector(EvidenceRef(EvidenceRefKind.ExactBoard, "exact-board-ref")),
      softReasons: Vector[SuppressionReason] = Vector.empty,
      wordingStrengthCap: WordingStrength = WordingStrength.QualifiedSupport
  ): SelectedClaim =
    SelectedClaim(
      CommentaryClaim(
        id = id,
        layer = layer,
        status = if layer == ClaimLayer.SourceContext then ClaimStatus.Context else ClaimStatus.Admitted,
        owner = Option.when(layer != ClaimLayer.SourceContext)("white"),
        beneficiary = Option.when(layer != ClaimLayer.SourceContext)("white"),
        defender = Option.when(layer != ClaimLayer.SourceContext)("black"),
        sideToMove = Option.when(layer != ClaimLayer.SourceContext)("white"),
        anchor = Option.when(layer != ClaimLayer.SourceContext)("board"),
        route = Option.when(layer != ClaimLayer.SourceContext)("route"),
        scope = Option.when(layer != ClaimLayer.SourceContext)("position_local"),
        evidenceRefs = evidenceRefs,
        exactBoardBound = layer != ClaimLayer.SourceContext,
        wordingStrengthCap = wordingStrengthCap,
        sourceContextKind = Option.when(layer == ClaimLayer.SourceContext)(SourceContextKind.Opening)
      ),
      bucket,
      softReasons
    )

  private def outlineWith(
      context: Vector[SelectedClaim] = Vector.empty,
      lead: Option[SelectedClaim] = None,
      support: Vector[SelectedClaim] = Vector.empty,
      contrast: Vector[SelectedClaim] = Vector.empty,
      suppressedClaims: Vector[SuppressedClaim] = Vector.empty,
      evidenceRefs: Vector[EvidenceRef] = Vector.empty,
      wordingStrengthCap: WordingStrength
  ): CommentaryOutline =
    CommentaryOutline(
      context = context,
      lead = lead,
      support = support,
      contrast = contrast,
      suppressedClaims = suppressedClaims,
      evidenceRefs = evidenceRefs,
      wordingStrengthCap = wordingStrengthCap
    )
