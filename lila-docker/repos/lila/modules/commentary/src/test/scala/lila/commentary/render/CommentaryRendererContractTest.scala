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

  test("minimal renderer emits deterministic role fragments without chess narration"):
    val plan = planWith(
      main = Some(section(PlanRole.Main, selected("main-s07", ClaimLayer.Projection, ClaimBucket.ShouldLead))),
      support = section(PlanRole.Support, selected("support-s21", ClaimLayer.Projection, ClaimBucket.Support)),
      context = section(PlanRole.Context, selected("context-opening", ClaimLayer.SourceContext, ClaimBucket.ContextOnly)),
      contrast = section(PlanRole.Contrast, selected("contrast-s08", ClaimLayer.Projection, ClaimBucket.Support)),
      maxStrength = WordingStrength.QualifiedSupport
    )

    val render = CommentaryRenderer.render(plan)

    assertEquals(render.blocks.map(_.text.publicText), Vector(Some("Primary"), Some("Support"), Some("Context"), Some("Contrast")))
    render.blocks.flatMap(_.text.publicText).foreach: text =>
      assert(!text.toLowerCase.contains("best"))
      assert(!text.toLowerCase.contains("theory"))
      assert(!text.toLowerCase.contains("forced"))
      assert(!text.toLowerCase.contains("result"))
      assert(!text.toLowerCase.contains("winning"))

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
    assertEquals(render.blocks.map(_.text.publicText), Vector(Some("Context")))
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

  test("renderer contract docs and surface corpus keep executable names"):
    val contractDoc = Files.readString(Paths.get("modules/commentary/docs/CommentaryRendererContract.md"))
    val coreDoc = Files.readString(Paths.get("modules/commentary/docs/CommentaryCoreSSOT.md"))
    val validationDoc = Files.readString(Paths.get("modules/commentary/docs/ValidationMethodology.md"))
    val surfaceRows = Files.readString(Paths.get("modules/commentary/src/test/resources/commentary-corpus/surface-expectations.jsonl"))
    val requiredTokens = Vector(
      "CommentaryRenderer",
      "CommentaryRender",
      "RenderBlock",
      "RenderRole",
      "RenderStatus",
      "RenderText",
      "RenderEvidenceRef",
      "RenderBoundary",
      "RenderSuppression",
      "RenderWording",
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
    assert(surfaceRows.contains("surface-renderer-deterministic-role-fragments"))
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
      maxStrength: WordingStrength
  ): CommentaryPlan =
    CommentaryPlan(
      main = main,
      support = support,
      context = context,
      contrast = contrast,
      blocked = blocked,
      evidence = evidence,
      wordingRules = WordingRules(maxStrength)
    )

  private def section(role: PlanRole, claims: SelectedClaim*): PlanSection =
    PlanSection(role, claims.toVector)

  private def selected(
      id: String,
      layer: ClaimLayer,
      bucket: ClaimBucket,
      evidenceRefs: Vector[EvidenceRef] = Vector.empty,
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
