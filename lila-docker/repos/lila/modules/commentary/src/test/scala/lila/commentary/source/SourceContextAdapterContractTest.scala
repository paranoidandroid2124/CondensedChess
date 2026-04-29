package lila.commentary.source

import lila.commentary.api.{ CommentaryApiJson, CommentaryBackendSeam, CommentaryRequest }
import lila.commentary.render.{ CommentaryRenderer, RenderLineRole, RenderStatus }
import lila.commentary.selection.*
import play.api.libs.json.Json

class SourceContextAdapterContractTest extends munit.FunSuite:

  private val validFen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"
  private val endgameFen = "8/3k4/8/3P4/3K4/4r3/8/4R3 b - - 0 1"

  test("accepted source context reaches the plan only through normalized handoff"):
    val normalized = SourceContextAdapter
      .normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-catalan-context",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("master_reference", "online_trend")
        )
      )
      .fold(error => fail(error.reason), identity)

    val claim = SourceContextClaimBoundary.toClaim(normalized)
    val outline = ClaimSelector.select(Vector(claim))
    val plan = CommentaryOutlineBuilder.build(outline)
    val render = CommentaryRenderer.render(plan)

    assertEquals(claim.layer, ClaimLayer.SourceContext)
    assertEquals(plan.main, None)
    assertEquals(plan.context.claims.map(_.claim.id), Vector("adapter-catalan-context"))
    assertEquals(render.status, RenderStatus.ContextOnly)
    assertEquals(render.blocks.map(_.nonAuthoritative), Vector(true))
    assertEquals(render.blocks.flatMap(_.text.publicText), Vector("Context"))
    assert(!render.blocks.flatMap(_.text.publicText).mkString(" ").contains("catalan-open"))
    assertEquals(render.evidenceRefs.map(_.id).filter(_.contains("opening-source-use")), Vector("opening-source-use:master_reference", "opening-source-use:online_trend"))

  test("opening sequence context can link to public-safe variation evidence without becoming proof"):
    val proofId = "line-proof-catalan-e4-break"
    val boardClaim = exactBoardClaimWithLineProof(proofId)
    val normalized = SourceContextAdapter
      .normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-catalan-sequence-context",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("master_reference", "online_trend"),
          sequenceContexts = Vector(
            OpeningSequenceContext(
              role = OpeningSequenceContextRole.PawnBreak,
              ref = "e2e4_break_context",
              linkedVariationProofIds = Vector(proofId),
              boundaries = Vector("opening_sequence_context_only", "line_test_link_is_not_proof")
            ),
            OpeningSequenceContext(
              role = OpeningSequenceContextRole.MasterOnlineDivergence,
              ref = "master_online_divergence_context"
            )
          )
        )
      )
      .fold(error => fail(error.reason), identity)
    val openingClaim = SourceContextClaimBoundary.toClaim(normalized)
    val outline = ClaimSelector.select(Vector(boardClaim, openingClaim))
    val plan = CommentaryOutlineBuilder.build(outline)
    val render = CommentaryRenderer.render(plan)

    assertEquals(outline.lead.map(_.claim.id), Some(boardClaim.id))
    assertEquals(outline.context.map(_.claim.id), Vector("adapter-catalan-sequence-context"))
    assert(outline.variationEvidence.map(_.proofId).contains(proofId))
    assertEquals(openingClaim.variationEvidence, Vector.empty)
    assert(openingClaim.evidenceRefs.exists(_.id == s"opening-line-test:$proofId:context"))
    assert(openingClaim.evidenceRefs.exists(_.id == "opening-sequence:pawn_break:e2e4_break_context"))
    assert(openingClaim.evidenceRefs.forall(_.kind == EvidenceRefKind.SourceContext))
    assert(render.variationEvidence.map(_.proofId).contains(proofId))
    assertEquals(render.variationEvidence.flatMap(_.provenanceRefs).map(_.kind).toSet, Set(EvidenceRefKind.Certification))
    assert(render.evidenceRefs.exists(_.id == s"opening-line-test:$proofId:context"))

  test("opening adapter rejects product candidate source uses and sequence overclaim wording"):
    val rejected = Vector(
      SourceContextAdapter.normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-smoke-opening",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("pipeline_smoke")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-taxonomy-opening",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("taxonomy_reference")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-best-sequence",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("master_reference"),
          sequenceContexts = Vector(
            OpeningSequenceContext(
              role = OpeningSequenceContextRole.MoveOrder,
              ref = "best_theory_sequence"
            )
          )
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-recommendation-source-use",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("move_recommendation")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-theory-truth-words",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("master_reference"),
          sequenceContexts = Vector(
            OpeningSequenceContext(
              role = OpeningSequenceContextRole.MoveOrder,
              ref = "theory truth move order"
            )
          )
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-deferred-game-context-words",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("master_reference"),
          sequenceContexts = Vector(
            OpeningSequenceContext(
              role = OpeningSequenceContextRole.MoveOrder,
              ref = "draw_offer_context"
            )
          )
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-opening-missing-line-boundary",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("master_reference"),
          sequenceContexts = Vector(
            OpeningSequenceContext(
              role = OpeningSequenceContextRole.PawnBreak,
              ref = "e2e4_break_context",
              linkedVariationProofIds = Vector("line-proof-catalan-e4-break"),
              boundaries = Vector("opening_sequence_context_only")
            )
          )
        )
      )
    )

    assert(rejected.forall(_.isLeft))
    assertEquals(rejected.leftReasons.toSet, Set("opening_product_rejected_source", "opening_truth_claim", "opening_sequence_truth_claim"))

  test("selector suppresses unsafe opening refs when adapter boundary is bypassed"):
    val unsafe = SourceContextClaimBoundary.toClaim(
      SourceContextCandidate(
        candidateId = "bypassed-opening-context",
        kind = SourceContextKind.Opening,
        sourceRefs = Vector(
          "opening-position:catalan-open:canonical",
          "opening-source-use:move_recommendation",
          "opening-sequence:move_order:draw_offer_context"
        )
      )
    )
    val outline = ClaimSelector.select(Vector(unsafe))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.context, Vector.empty)
    assertEquals(outline.suppressedClaims.map(_.claim.id), Vector("bypassed-opening-context"))
    assertEquals(render.status, RenderStatus.NoCommentary)

  test("selector suppresses product-rejected opening source refs when adapter boundary is bypassed"):
    val unsafe = SourceContextClaimBoundary.toClaim(
      SourceContextCandidate(
        candidateId = "bypassed-opening-product-source",
        kind = SourceContextKind.Opening,
        sourceRefs = Vector(
          "opening-position:catalan-open:canonical",
          "opening-source-use:pipeline_smoke",
          "opening-source-use:taxonomy_reference"
        )
      )
    )
    val outline = ClaimSelector.select(Vector(unsafe))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.context, Vector.empty)
    assertEquals(outline.suppressedClaims.map(_.claim.id), Vector("bypassed-opening-product-source"))
    assertEquals(render.status, RenderStatus.NoCommentary)

  test("motif line context cannot enter line explanation without detector carrier parity"):
    val rejected = SourceContextAdapter
      .normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-motif-line-mismatch",
          motifExampleId = "motif-pin-example",
          detectorCarrierId = "motif-fork-example",
          motifId = Some("pin"),
          lineContexts = Vector(
            MotifLineContext(
              role = MotifLineContextRole.PinnedDefender,
              ref = "pinned_defender_e2",
              linkedVariationProofIds = Vector("line-proof-pin-resource-fails"),
              boundaries = Vector("motif_line_context_only", "line_test_link_is_not_proof")
            )
          )
        )
      )

    assertEquals(rejected.left.map(_.reason), Left("motif_carrier_mismatch"))

  test("motif context can link to bounded defender-resource line evidence without becoming proof"):
    val proofId = "line-proof-pin-resource-fails"
    val boardClaim = exactBoardClaimWithMotifProof(proofId, includeDebug = false)
    val normalized = SourceContextAdapter
      .normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-pin-line-context",
          motifExampleId = "motif-pin-example",
          detectorCarrierId = "motif-pin-example",
          motifId = Some("pin"),
          lineContexts = Vector(
            MotifLineContext(
              role = MotifLineContextRole.PinnedDefender,
              ref = "pinned_defender_e2",
              linkedVariationProofIds = Vector(proofId),
              boundaries = Vector("motif_line_context_only", "line_test_link_is_not_proof")
            ),
            MotifLineContext(
              role = MotifLineContextRole.FailedResource,
              ref = "natural_resource_fails"
            ),
            MotifLineContext(
              role = MotifLineContextRole.HeldResource,
              ref = "resource_holds_as_context"
            )
          )
        )
      )
      .fold(error => fail(error.reason), identity)
    val motifClaim = SourceContextClaimBoundary.toClaim(normalized)
    val outline = ClaimSelector.select(Vector(boardClaim, motifClaim))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.lead.map(_.claim.id), Some(boardClaim.id))
    assertEquals(outline.context.map(_.claim.id), Vector("adapter-pin-line-context"))
    assertEquals(motifClaim.variationEvidence, Vector.empty)
    assert(motifClaim.evidenceRefs.exists(_.id == s"motif-line-test:$proofId:context"))
    assert(motifClaim.evidenceRefs.exists(_.id == "motif-line-context:pinned_defender:pinned_defender_e2"))
    assert(motifClaim.evidenceRefs.exists(_.id == "motif-line-context:held_resource:resource_holds_as_context"))
    assert(motifClaim.evidenceRefs.exists(ref => ref.kind == EvidenceRefKind.ExactBoard && ref.id == "motif-detector-carrier:motif-pin-example"))
    assert(render.variationEvidence.map(_.proofId).contains(proofId))
    assert(render.variationEvidence.map(_.role).contains(RenderLineRole.Resource))
    assert(render.variationEvidence.map(_.testResult).contains(VariationTestResult.ResourceFails))
    assert(render.blocks.flatMap(_.text.forbiddenTerms).contains("best"))
    assert(!render.blocks.flatMap(_.text.publicText).mkString(" ").contains("pin"))

  test("deferred motifs and certification-only motifs are blocked at motif adapter boundary"):
    val rejected = Vector(
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-discovered-attack",
          motifExampleId = "motif-discovered-attack-reject",
          detectorCarrierId = "motif-discovered-attack-reject",
          motifId = Some("discovered_attack")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-deflection",
          motifExampleId = "motif-deflection-reject",
          detectorCarrierId = "motif-deflection-reject",
          motifId = Some("deflection")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-back-rank",
          motifExampleId = "motif-back-rank-reject",
          detectorCarrierId = "motif-back-rank-reject",
          motifId = Some("back_rank")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-clearance",
          motifExampleId = "motif-clearance-reject",
          detectorCarrierId = "motif-clearance-reject",
          motifId = Some("clearance")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-interference",
          motifExampleId = "motif-interference-reject",
          detectorCarrierId = "motif-interference-reject",
          motifId = Some("interference")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-mate-net-no-cert",
          motifExampleId = "motif-mate-net-example",
          detectorCarrierId = "motif-mate-net-example",
          motifId = Some("mate_net"),
          carrierKind = "u_witness"
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-perpetual-no-cert",
          motifExampleId = "motif-perpetual-check-example",
          detectorCarrierId = "motif-perpetual-check-example",
          motifId = Some("perpetual_check"),
          carrierKind = "u_witness"
        )
      )
    )

    assert(rejected.forall(_.isLeft))
    assertEquals(rejected.leftReasons.toSet, Set("motif_deferred_helper_required", "motif_certification_carrier_required"))

  test("certification-only motifs stay blocked without exact certification carrier despite certified-line label"):
    val rejected = Vector(
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-mate-net-certified-line-label",
          motifExampleId = "motif-mate-net-example",
          detectorCarrierId = "motif-mate-net-example",
          motifId = Some("mate_net"),
          carrierKind = "certified_line"
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-perpetual-certified-line-label",
          motifExampleId = "motif-perpetual-check-example",
          detectorCarrierId = "motif-perpetual-check-example",
          motifId = Some("perpetual_check"),
          carrierKind = "certified_line"
        )
      )
    )

    assert(rejected.forall(_.isLeft))
    assertEquals(rejected.leftReasons.toSet, Set("motif_certification_carrier_required"))

  test("motif adapter rejects unsafe line context wording and missing line-test boundary"):
    val rejected = Vector(
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-motif-missing-line-boundary",
          motifExampleId = "motif-pin-example",
          detectorCarrierId = "motif-pin-example",
          motifId = Some("pin"),
          lineContexts = Vector(
            MotifLineContext(
              role = MotifLineContextRole.PinnedDefender,
              ref = "pinned_defender_e2",
              linkedVariationProofIds = Vector("line-proof-pin-resource-fails"),
              boundaries = Vector("motif_line_context_only")
            )
          )
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-motif-line-truth",
          motifExampleId = "motif-pin-example",
          detectorCarrierId = "motif-pin-example",
          motifId = Some("pin"),
          lineContexts = Vector(
            MotifLineContext(
              role = MotifLineContextRole.FailedResource,
              ref = "forced_result_resource"
            )
          )
        )
      )
    )

    assert(rejected.forall(_.isLeft))
    assertEquals(rejected.leftReasons.toSet, Set("motif_line_context_truth_claim"))

  test("endgame technique context can link to public-safe variation evidence without becoming proof"):
    val proofId = "line-proof-philidor-third-rank-hold"
    val boardClaim = exactBoardClaimWithEndgameTechniqueProof(proofId, includeDebug = false)
    val normalized = SourceContextAdapter
      .normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-philidor-technique-context",
          studyId = "philidor_rook_pawn",
          applicabilityFixtureId = "study-philidor-context",
          applicabilityVerified = true,
          techniqueContexts = Vector(
            EndgameTechniqueContext(
              role = EndgameTechniqueContextRole.ThirdRankSetup,
              ref = "third_rank_setup_context",
              linkedVariationProofIds = Vector(proofId),
              boundaries = Vector("endgame_technique_context_only", "line_test_link_is_not_proof")
            ),
            EndgameTechniqueContext(
              role = EndgameTechniqueContextRole.CheckingDistance,
              ref = "checking_distance_context"
            )
          )
        )
      )
      .fold(error => fail(error.reason), identity)
    val endgameClaim = SourceContextClaimBoundary.toClaim(normalized)
    val outline = ClaimSelector.select(Vector(boardClaim, endgameClaim))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.lead.map(_.claim.id), Some(boardClaim.id))
    assertEquals(outline.context.map(_.claim.id), Vector("adapter-philidor-technique-context"))
    assertEquals(endgameClaim.variationEvidence, Vector.empty)
    assert(endgameClaim.evidenceRefs.exists(_.id == s"endgame-line-test:$proofId:context"))
    assert(endgameClaim.evidenceRefs.exists(_.id == "endgame-technique:third_rank_setup:third_rank_setup_context"))
    assert(endgameClaim.evidenceRefs.exists(_.id == "endgame-technique:checking_distance:checking_distance_context"))
    assert(endgameClaim.evidenceRefs.exists(ref => ref.kind == EvidenceRefKind.ExactBoard && ref.id == "endgame-study-applicability:study-philidor-context"))
    assert(render.variationEvidence.map(_.proofId).contains(proofId))
    assert(render.variationEvidence.map(_.role).contains(RenderLineRole.Hold))
    assert(render.variationEvidence.map(_.testResult).contains(VariationTestResult.DefensiveHold))
    assert(!render.blocks.flatMap(_.text.publicText).mkString(" ").contains("Philidor"))

  test("endgame adapter requires exact applicability verification before route binding"):
    val unverified = SourceContextAdapter.normalize(
      SourceContextInput.EndgameStudy(
        candidateId = "adapter-unverified-philidor-context",
        studyId = "philidor_rook_pawn",
        applicabilityFixtureId = "unknown-philidor-fixture"
      )
    )
    val verified = SourceContextAdapter
      .normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-verified-philidor-context",
          studyId = "philidor_rook_pawn",
          applicabilityFixtureId = "study-philidor-context",
          applicabilityVerified = true
        )
      )
      .fold(error => fail(error.reason), identity)

    assertEquals(unverified.left.map(_.reason), Left("endgame_unverified_applicability"))
    assertEquals(verified.exactBoardRefs.exists(_.scope.contains("exact_endgame_applicability")), true)

  test("selector suppresses unverified endgame applicability when adapter boundary is bypassed"):
    val unsafe = SourceContextClaimBoundary.toClaim(
      SourceContextCandidate(
        candidateId = "bypassed-endgame-unverified",
        kind = SourceContextKind.EndgameStudy,
        sourceRefs = Vector("endgame-study:philidor_rook_pawn:applicable"),
        exactBoardRefs = Vector(SourceContextExactRef("endgame-study-applicability:unknown-philidor-fixture"))
      )
    )
    val outline = ClaimSelector.select(Vector(unsafe))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.context, Vector.empty)
    assertEquals(outline.suppressedClaims.map(_.claim.id), Vector("bypassed-endgame-unverified"))
    assertEquals(render.status, RenderStatus.NoCommentary)

  test("endgame adapter blocks result oracle forced conversion and deferred technique shortcuts"):
    val rejected = Vector(
      SourceContextAdapter.normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-endgame-outside-passer",
          studyId = "outside_passer",
          applicabilityFixtureId = "study-outside-passer-context"
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-endgame-forced-technique",
          studyId = "lucena_rook_pawn",
          applicabilityFixtureId = "study-lucena-context",
          applicabilityVerified = true,
          techniqueContexts = Vector(
            EndgameTechniqueContext(
              role = EndgameTechniqueContextRole.BridgeSetup,
              ref = "forced_conversion_path"
            )
          )
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-endgame-oracle",
          studyId = "philidor_rook_pawn",
          applicabilityFixtureId = "tablebase_oracle_fixture"
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-endgame-missing-line-boundary",
          studyId = "philidor_rook_pawn",
          applicabilityFixtureId = "study-philidor-context",
          applicabilityVerified = true,
          techniqueContexts = Vector(
            EndgameTechniqueContext(
              role = EndgameTechniqueContextRole.ThirdRankSetup,
              ref = "third_rank_setup_context",
              linkedVariationProofIds = Vector("line-proof-philidor-third-rank-hold"),
              boundaries = Vector("endgame_technique_context_only")
            )
          )
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-endgame-result-context",
          studyId = "philidor_rook_pawn",
          applicabilityFixtureId = "study-philidor-context",
          applicabilityVerified = true,
          techniqueContexts = Vector(
            EndgameTechniqueContext(
              role = EndgameTechniqueContextRole.MethodException,
              ref = "result_context"
            )
          )
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-endgame-oracle-context",
          studyId = "philidor_rook_pawn",
          applicabilityFixtureId = "study-philidor-context",
          applicabilityVerified = true,
          techniqueContexts = Vector(
            EndgameTechniqueContext(
              role = EndgameTechniqueContextRole.MethodException,
              ref = "oracle_context"
            )
          )
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-endgame-deferred-context",
          studyId = "philidor_rook_pawn",
          applicabilityFixtureId = "study-philidor-context",
          applicabilityVerified = true,
          techniqueContexts = Vector(
            EndgameTechniqueContext(
              role = EndgameTechniqueContextRole.MethodException,
              ref = "outside_passer_context"
            )
          )
        )
      )
    )

    assertEquals(rejected.forall(_.isLeft), true)
    assertEquals(rejected.leftReasons.toSet, Set("endgame_deferred_study", "endgame_technique_truth_claim", "endgame_result_claim"))

  test("retrieval illustrative line support stays subordinate to exact-board variation evidence"):
    val proofId = "line-proof-catalan-e4-break"
    val boardClaim = exactBoardClaimWithLineProof(proofId)
    val normalized = SourceContextAdapter
      .normalize(
        SourceContextInput.Retrieval(
          candidateId = "adapter-retrieval-illustrative-context",
          retrievalExampleId = "broadcast-catalan-context",
          illustrationContexts = Vector(
            RetrievalIllustrationContext(
              role = RetrievalIllustrationContextRole.ComparableLine,
              ref = "catalan_example_sequence",
              linkedVariationProofIds = Vector(proofId),
              boundaries = Vector("retrieval_illustration_context_only", "line_test_link_is_not_proof")
            ),
            RetrievalIllustrationContext(
              role = RetrievalIllustrationContextRole.ThemeExample,
              ref = "queenside_tension_theme"
            )
          )
        )
      )
      .fold(error => fail(error.reason), identity)
    val retrievalClaim = SourceContextClaimBoundary.toClaim(normalized)
    val outline = ClaimSelector.select(Vector(boardClaim, retrievalClaim))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.lead.map(_.claim.id), Some(boardClaim.id))
    assertEquals(outline.context.map(_.claim.id), Vector("adapter-retrieval-illustrative-context"))
    assertEquals(retrievalClaim.variationEvidence, Vector.empty)
    assert(retrievalClaim.evidenceRefs.exists(_.id == s"retrieval-line-test:$proofId:context"))
    assert(retrievalClaim.evidenceRefs.exists(_.id == "retrieval-illustration:comparable_line:catalan_example_sequence"))
    assert(retrievalClaim.evidenceRefs.exists(_.id == "retrieval-illustration:theme_example:queenside_tension_theme"))
    assert(render.variationEvidence.map(_.proofId).contains(proofId))
    assert(!render.blocks.flatMap(_.text.publicText).mkString(" ").contains("broadcast"))

  test("source line-test context is suppressed without matching public-safe variation proof"):
    val normalized = SourceContextAdapter
      .normalize(
        SourceContextInput.Retrieval(
          candidateId = "adapter-retrieval-orphan-line-test",
          retrievalExampleId = "broadcast-catalan-context",
          illustrationContexts = Vector(
            RetrievalIllustrationContext(
              role = RetrievalIllustrationContextRole.ComparableLine,
              ref = "catalan_example_sequence",
              linkedVariationProofIds = Vector("line-proof-not-selected"),
              boundaries = Vector("retrieval_illustration_context_only", "line_test_link_is_not_proof")
            )
          )
        )
      )
      .fold(error => fail(error.reason), identity)
    val claim = SourceContextClaimBoundary.toClaim(normalized)
    val outline = ClaimSelector.select(Vector(claim))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.context, Vector.empty)
    assertEquals(outline.suppressedClaims.map(_.claim.id), Vector("adapter-retrieval-orphan-line-test"))
    assertEquals(render.status, RenderStatus.NoCommentary)

  test("retrieval adapter blocks current-position proof metadata verdict and display shortcuts"):
    val rejected = Vector(
      SourceContextAdapter.normalize(
        SourceContextInput.Retrieval(
          candidateId = "adapter-retrieval-current-proof",
          retrievalExampleId = "broadcast-catalan-context",
          currentPositionClaim = true
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Retrieval(
          candidateId = "adapter-retrieval-result-verdict",
          retrievalExampleId = "result-verdict-example"
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Retrieval(
          candidateId = "adapter-retrieval-recommendation",
          retrievalExampleId = "quiet-reference",
          illustrationContexts = Vector(
            RetrievalIllustrationContext(
              role = RetrievalIllustrationContextRole.SimilarPlanSequence,
              ref = "move_recommendation_from_retrieved_game"
            )
          )
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Retrieval(
          candidateId = "adapter-retrieval-display-candidate",
          retrievalExampleId = "broadcast-catalan-context",
          displayCandidate = true
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Retrieval(
          candidateId = "adapter-retrieval-missing-line-boundary",
          retrievalExampleId = "broadcast-catalan-context",
          illustrationContexts = Vector(
            RetrievalIllustrationContext(
              role = RetrievalIllustrationContextRole.ComparableLine,
              ref = "catalan_example_sequence",
              linkedVariationProofIds = Vector("line-proof-catalan-e4-break"),
              boundaries = Vector("retrieval_illustration_context_only")
            )
          )
        )
      )
    )

    assert(rejected.forall(_.isLeft))
    assertEquals(rejected.leftReasons.toSet, Set("retrieval_truth_claim", "retrieval_illustration_truth_claim", "retrieval_display_deferred"))

  test("selector suppresses unsafe retrieval refs when adapter boundary is bypassed"):
    val unsafe = SourceContextClaimBoundary.toClaim(
      SourceContextCandidate(
        candidateId = "bypassed-retrieval-verdict",
        kind = SourceContextKind.Retrieval,
        sourceRefs = Vector(
          "retrieval-example:result-verdict-example",
          "retrieval-illustration:similar_plan_sequence:move_recommendation_from_retrieved_game"
        )
      )
    )
    val outline = ClaimSelector.select(Vector(unsafe))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.context, Vector.empty)
    assertEquals(outline.suppressedClaims.map(_.claim.id), Vector("bypassed-retrieval-verdict"))
    assertEquals(render.status, RenderStatus.NoCommentary)

  test("source-family forbidden claims are rejected before selection"):
    val rejected = Vector(
      SourceContextAdapter.normalize(
        SourceContextInput.Opening(
          candidateId = "adapter-merged-opening",
          canonicalPositionId = "catalan-open",
          sourceUses = Vector("master_reference+online_trend")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Motif(
          candidateId = "adapter-motif-mismatch",
          motifExampleId = "motif-pin-example",
          detectorCarrierId = "motif-fork-example"
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-endgame-result",
          studyId = "lucena_rook_pawn",
          applicabilityFixtureId = "study-lucena-context",
          outcomeClaim = Some("wdl_win")
        )
      ),
      SourceContextAdapter.normalize(
        SourceContextInput.Retrieval(
          candidateId = "adapter-retrieval-truth",
          retrievalExampleId = "retrieval-example-current-truth",
          currentPositionClaim = true
        )
      )
    )

    assertEquals(rejected.forall(_.isLeft), true)
    assertEquals(rejected.leftReasons.toSet, Set("opening_ranking_merge", "motif_carrier_mismatch", "endgame_result_claim", "retrieval_truth_claim"))

  test("suppressed source context does not leak into rendering"):
    val unsafe = SourceContextClaimBoundary.toClaim(
      SourceContextCandidate(
        candidateId = "adapter-bypassed-opening-context",
        kind = SourceContextKind.Opening,
        sourceRefs = Vector(
          "opening-position:catalan-open:canonical",
          "opening-position:catalan-open:best-move"
        )
      )
    )

    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(ClaimSelector.select(Vector(unsafe))))

    assertEquals(render.status, RenderStatus.NoCommentary)
    assertEquals(render.blocks, Vector.empty)
    assertEquals(render.evidenceRefs, Vector.empty)
    assertEquals(render.suppressions.map(_.claimId), Vector("adapter-bypassed-opening-context"))
    assert(render.suppressions.forall(_.public == false))

  test("backend public response excludes internal source-context suppressions"):
    val blocked = SourceContextClaimBoundary.toClaim(
      SourceContextCandidate(
        candidateId = "adapter-backend-blocked-source",
        kind = SourceContextKind.Opening,
        sourceRefs = Vector("opening-position:catalan-open:best-move")
      )
    )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(blocked))

    val publicResponse = seam.render(request())
    val debugResponse = seam.renderDebug(request())

    assertEquals(publicResponse.render.suppressions, Vector.empty)
    assertEquals(publicResponse.internal, None)
    assertEquals(publicResponse.render.evidenceRefs, Vector.empty)
    assert(debugResponse.internal.exists(_.suppressions.map(_.claimId) == Vector("adapter-backend-blocked-source")))
    assertEquals(debugResponse.render.suppressions, Vector.empty)

  test("backend does not expose raw opening source rows when sequence context is present"):
    val proofId = "line-proof-catalan-e4-break"
    val boardClaim = exactBoardClaimWithLineProof(proofId)
    val openingClaim = SourceContextClaimBoundary.toClaim(
      SourceContextAdapter
        .normalize(
          SourceContextInput.Opening(
            candidateId = "adapter-backend-sequence-context",
            canonicalPositionId = "catalan-open",
            sourceUses = Vector("master_reference", "online_trend"),
            sequenceContexts = Vector(
              OpeningSequenceContext(
                role = OpeningSequenceContextRole.MoveOrder,
                ref = "catalan_open_move_order",
                linkedVariationProofIds = Vector(proofId),
                boundaries = Vector("opening_sequence_context_only", "line_test_link_is_not_proof")
              )
            )
          )
        )
        .fold(error => fail(error.reason), identity)
    )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardClaim, openingClaim))

    val response = seam.render(request())
    import CommentaryApiJson.given
    val json = Json.toJson(response).toString

    assertEquals(response.render.status, RenderStatus.Rendered)
    assert(response.render.variationEvidence.map(_.proofId).contains(proofId))
    assert(!json.contains("primaryReferenceStats"))
    assert(!json.contains("secondaryTrendStats"))
    assert(!json.contains("moveOrder"))
    assert(!json.contains("frequency"))
    assert(!json.contains("opening-themes"))

  test("backend does not expose raw motif source rows or internal line proof packets"):
    val proofId = "line-proof-pin-resource-fails"
    val boardClaim = exactBoardClaimWithMotifProof(proofId, includeDebug = true)
    val motifClaim = SourceContextClaimBoundary.toClaim(
      SourceContextAdapter
        .normalize(
          SourceContextInput.Motif(
            candidateId = "adapter-backend-pin-line-context",
            motifExampleId = "motif-pin-example",
            detectorCarrierId = "motif-pin-example",
            motifId = Some("pin"),
            lineContexts = Vector(
              MotifLineContext(
                role = MotifLineContextRole.FailedResource,
                ref = "natural_resource_fails",
                linkedVariationProofIds = Vector(proofId),
                boundaries = Vector("motif_line_context_only", "line_test_link_is_not_proof")
              )
            )
          )
        )
        .fold(error => fail(error.reason), identity)
    )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardClaim, motifClaim))

    val response = seam.renderDebug(request())
    import CommentaryApiJson.given
    val json = Json.toJson(response).toString

    assertEquals(response.render.status, RenderStatus.Rendered)
    assert(response.render.variationEvidence.map(_.proofId).contains(proofId))
    assert(!json.contains("detectorCarrier"))
    assert(!json.contains("sourceTags"))
    assert(!json.contains("motif-examples"))
    assert(!json.contains("pin-debug-hash"))
    assert(!json.contains("pin-raw-packet"))
    assert(!json.contains("rawLineIndex"))

  test("backend does not expose raw endgame source rows or internal line proof packets"):
    val proofId = "line-proof-philidor-third-rank-hold"
    val boardClaim = exactBoardClaimWithEndgameTechniqueProof(proofId, includeDebug = true)
    val endgameClaim = SourceContextClaimBoundary.toClaim(
      SourceContextAdapter
        .normalize(
        SourceContextInput.EndgameStudy(
          candidateId = "adapter-backend-philidor-technique-context",
          studyId = "philidor_rook_pawn",
          applicabilityFixtureId = "study-philidor-context",
          applicabilityVerified = true,
          techniqueContexts = Vector(
            EndgameTechniqueContext(
                role = EndgameTechniqueContextRole.ThirdRankSetup,
                ref = "third_rank_setup_context",
                linkedVariationProofIds = Vector(proofId),
                boundaries = Vector("endgame_technique_context_only", "line_test_link_is_not_proof")
              )
            )
          )
        )
        .fold(error => fail(error.reason), identity)
    )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardClaim, endgameClaim))

    val response = seam.renderDebug(endgameRequest())
    import CommentaryApiJson.given
    val json = Json.toJson(response).toString

    assertEquals(response.render.status, RenderStatus.Rendered)
    assert(response.render.variationEvidence.map(_.proofId).contains(proofId))
    assert(!json.contains("placementEvidence"))
    assert(!json.contains("relationEvidence"))
    assert(!json.contains("candidatePlans"))
    assert(!json.contains("outcomeClaim"))
    assert(!json.contains("philidor-debug-hash"))
    assert(!json.contains("philidor-raw-packet"))
    assert(!json.contains("rawLineIndex"))

  test("backend does not expose raw retrieval rows or internal line proof packets"):
    val proofId = "line-proof-retrieval-illustration"
    val boardClaim = exactBoardClaimWithRetrievalProof(proofId, includeDebug = true)
    val retrievalClaim = SourceContextClaimBoundary.toClaim(
      SourceContextAdapter
        .normalize(
          SourceContextInput.Retrieval(
            candidateId = "adapter-backend-retrieval-illustrative-context",
            retrievalExampleId = "broadcast-catalan-context",
            illustrationContexts = Vector(
              RetrievalIllustrationContext(
                role = RetrievalIllustrationContextRole.ComparableLine,
                ref = "catalan_example_sequence",
                linkedVariationProofIds = Vector(proofId),
                boundaries = Vector("retrieval_illustration_context_only", "line_test_link_is_not_proof")
              )
            )
          )
        )
        .fold(error => fail(error.reason), identity)
    )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardClaim, retrievalClaim))

    val response = seam.renderDebug(request())
    import CommentaryApiJson.given
    val json = Json.toJson(response).toString

    assertEquals(response.render.status, RenderStatus.Rendered)
    assert(response.render.variationEvidence.map(_.proofId).contains(proofId))
    assert(!json.contains("gameMetadata"))
    assert(!json.contains("players"))
    assert(!json.contains("event"))
    assert(!json.contains("1/2-1/2"))
    assert(!json.contains("1-0"))
    assert(!json.contains("snippetText"))
    assert(!json.contains("retrieval-debug-hash"))
    assert(!json.contains("retrieval-raw-packet"))
    assert(!json.contains("rawLineIndex"))

  private def request(): CommentaryRequest =
    CommentaryRequest(
      currentFen = validFen,
      beforeFen = None,
      playedMove = None,
      nodeId = "mainline:0",
      ply = 0
    )

  private def endgameRequest(): CommentaryRequest =
    request().copy(currentFen = endgameFen, nodeId = "endgame:0")

  private def exactBoardClaimWithLineProof(proofId: String): CommentaryClaim =
    val owner = "white"
    val anchor = "board"
    val route = "catalan_e4_break"
    val scope = "position_local"
    CommentaryClaim(
      id = "cert-catalan-e4-break",
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = Some(owner),
      beneficiary = Some(owner),
      defender = Some("black"),
      sideToMove = Some(owner),
      anchor = Some(anchor),
      route = Some(route),
      scope = Some(scope),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport,
      impact = ClaimImpact(evidenceConfidence = 80, boardExplainability = 70),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Certification, "cert-catalan-e4-break", Some(owner), Some(anchor), Some(route), Some(scope))
      ),
      variationEvidence = Vector(
        PreparedVariationEvidence(
          proofId = proofId,
          boundClaimId = "cert-catalan-e4-break",
          startFen = validFen,
          owner = owner,
          defender = Some("black"),
          anchor = anchor,
          route = route,
          scope = scope,
          role = VariationEvidenceRole.Persistence,
          moveRole = VariationMoveRole.CandidateMove,
          lineSan = Vector("e4", "Nc6"),
          lineUci = Vector("e2e4", "b8c6"),
          candidateMove = Some(VariationMove("e4", "e2e4")),
          testedMove = Some(VariationMove("e4", "e2e4")),
          testedLine = Vector(VariationMove("e4", "e2e4"), VariationMove("Nc6", "b8c6")),
          replyLine = Vector(VariationMove("Nc6", "b8c6")),
          testResult = VariationTestResult.PressurePersists,
          proves = "pressure_persists_after_e4_break",
          proofPurpose = VariationProofPurpose.PreservesPressure,
          provenanceRefs = Vector(
            EvidenceRef(EvidenceRefKind.Certification, "cert-catalan-e4-break", Some(owner), Some(anchor), Some(route), Some(scope))
          ),
          boundary = PreparedVariationBoundary(
            depthFloor = 12,
            realizedDepth = 16,
            multiPv = 3,
            freshnessChecked = true,
            legalReplayChecked = true,
            baselineChecked = true
          ),
          wordingCap = WordingStrength.QualifiedSupport,
          surfaceAllowance = VariationSurfaceAllowance.PublicLine,
          publicSafe = true
        ),
        PreparedVariationEvidence(
          proofId = s"$proofId-defender",
          boundClaimId = "cert-catalan-e4-break",
          startFen = validFen,
          owner = owner,
          defender = Some("black"),
          anchor = anchor,
          route = route,
          scope = scope,
          role = VariationEvidenceRole.DefenderResource,
          moveRole = VariationMoveRole.DefenderResource,
          lineSan = Vector("...c5", "Nf3"),
          lineUci = Vector("c7c5", "g1f3"),
          defenderResource = Some(VariationMove("...c5", "c7c5")),
          testedMove = Some(VariationMove("...c5", "c7c5")),
          testedLine = Vector(VariationMove("...c5", "c7c5"), VariationMove("Nf3", "g1f3")),
          replyLine = Vector(VariationMove("Nf3", "g1f3")),
          resourceLine = Vector(VariationMove("...c5", "c7c5"), VariationMove("Nf3", "g1f3")),
          testResult = VariationTestResult.ResourceWorks,
          proves = "defender_resource_available",
          proofPurpose = VariationProofPurpose.DeniesResource,
          provenanceRefs = Vector(
            EvidenceRef(EvidenceRefKind.Certification, "cert-catalan-e4-break", Some(owner), Some(anchor), Some(route), Some(scope))
          ),
          boundary = PreparedVariationBoundary(
            depthFloor = 12,
            realizedDepth = 16,
            multiPv = 2,
            freshnessChecked = true,
            legalReplayChecked = true,
            baselineChecked = true
          ),
          wordingCap = WordingStrength.QualifiedSupport,
          surfaceAllowance = VariationSurfaceAllowance.PublicLine,
          publicSafe = true
        )
      )
    )

  private def exactBoardClaimWithMotifProof(proofId: String, includeDebug: Boolean): CommentaryClaim =
    val owner = "white"
    val anchor = "ray:e8:south"
    val route = "pin_resource_test"
    val scope = "position_local"
    CommentaryClaim(
      id = "cert-pin-resource-test",
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = Some(owner),
      beneficiary = Some(owner),
      defender = Some("black"),
      sideToMove = Some(owner),
      anchor = Some(anchor),
      route = Some(route),
      scope = Some(scope),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport,
      impact = ClaimImpact(evidenceConfidence = 82, boardExplainability = 78),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Certification, "cert-pin-resource-test", Some(owner), Some(anchor), Some(route), Some(scope))
      ),
      variationEvidence = Vector(
        PreparedVariationEvidence(
          proofId = s"$proofId-candidate",
          boundClaimId = "cert-pin-resource-test",
          startFen = validFen,
          owner = owner,
          defender = Some("black"),
          anchor = anchor,
          route = route,
          scope = scope,
          role = VariationEvidenceRole.Persistence,
          moveRole = VariationMoveRole.CandidateMove,
          lineSan = Vector("Re1", "...Re7"),
          lineUci = Vector("e2e1", "e8e7"),
          candidateMove = Some(VariationMove("Re1", "e2e1")),
          testedMove = Some(VariationMove("Re1", "e2e1")),
          testedLine = Vector(VariationMove("Re1", "e2e1"), VariationMove("...Re7", "e8e7")),
          replyLine = Vector(VariationMove("...Re7", "e8e7")),
          testResult = VariationTestResult.PressurePersists,
          proves = "pressure_persists_after_pin_move",
          proofPurpose = VariationProofPurpose.PreservesPressure,
          provenanceRefs = Vector(
            EvidenceRef(EvidenceRefKind.Certification, "cert-pin-resource-test", Some(owner), Some(anchor), Some(route), Some(scope))
          ),
          boundary = PreparedVariationBoundary(
            depthFloor = 12,
            realizedDepth = 18,
            multiPv = 3,
            freshnessChecked = true,
            legalReplayChecked = true,
            baselineChecked = true
          ),
          wordingCap = WordingStrength.QualifiedSupport,
          surfaceAllowance = VariationSurfaceAllowance.PublicLine,
          publicSafe = true
        ),
        PreparedVariationEvidence(
          proofId = proofId,
          boundClaimId = "cert-pin-resource-test",
          startFen = validFen,
          owner = owner,
          defender = Some("black"),
          anchor = anchor,
          route = route,
          scope = scope,
          role = VariationEvidenceRole.DefenderResource,
          moveRole = VariationMoveRole.DefenderResource,
          lineSan = Vector("...Re7", "Rxe7"),
          lineUci = Vector("e8e7", "e2e7"),
          defenderResource = Some(VariationMove("...Re7", "e8e7")),
          testedMove = Some(VariationMove("...Re7", "e8e7")),
          testedLine = Vector(VariationMove("...Re7", "e8e7"), VariationMove("Rxe7", "e2e7")),
          replyLine = Vector(VariationMove("Rxe7", "e2e7")),
          resourceLine = Vector(VariationMove("...Re7", "e8e7"), VariationMove("Rxe7", "e2e7")),
          testResult = VariationTestResult.ResourceFails,
          proves = "defender_resource_fails_under_pin",
          proofPurpose = VariationProofPurpose.DeniesResource,
          provenanceRefs = Vector(
            EvidenceRef(EvidenceRefKind.Certification, "cert-pin-resource-test", Some(owner), Some(anchor), Some(route), Some(scope))
          ),
          boundary = PreparedVariationBoundary(
            depthFloor = 12,
            realizedDepth = 18,
            multiPv = 3,
            freshnessChecked = true,
            legalReplayChecked = true,
            baselineChecked = true
          ),
          wordingCap = WordingStrength.QualifiedSupport,
          surfaceAllowance = VariationSurfaceAllowance.PublicLine,
          publicSafe = true,
          debug =
            Option.when(includeDebug)(
              PreparedVariationDebug(
                variationHash = Some("pin-debug-hash"),
                engineConfigFingerprint = Some("pin-engine-config"),
                rawPacketId = Some("pin-raw-packet"),
                rawLineIndex = Some(4)
              )
            )
        )
      )
    )

  private def exactBoardClaimWithEndgameTechniqueProof(proofId: String, includeDebug: Boolean): CommentaryClaim =
    val owner = "black"
    val anchor = "third_rank"
    val route = "philidor_third_rank_hold"
    val scope = "position_local"
    CommentaryClaim(
      id = "cert-philidor-third-rank-hold",
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = Some(owner),
      beneficiary = Some(owner),
      defender = Some("white"),
      sideToMove = Some(owner),
      anchor = Some(anchor),
      route = Some(route),
      scope = Some(scope),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport,
      impact = ClaimImpact(evidenceConfidence = 84, boardExplainability = 76),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Certification, "cert-philidor-third-rank-hold", Some(owner), Some(anchor), Some(route), Some(scope))
      ),
      variationEvidence = Vector(
        PreparedVariationEvidence(
          proofId = s"$proofId-candidate",
          boundClaimId = "cert-philidor-third-rank-hold",
          startFen = endgameFen,
          owner = owner,
          defender = Some("white"),
          anchor = anchor,
          route = route,
          scope = scope,
          role = VariationEvidenceRole.Persistence,
          moveRole = VariationMoveRole.CandidateMove,
          lineSan = Vector("...Re3+", "Kc4"),
          lineUci = Vector("e3e4", "d4c4"),
          candidateMove = Some(VariationMove("...Re3+", "e3e4")),
          testedMove = Some(VariationMove("...Re3+", "e3e4")),
          testedLine = Vector(VariationMove("...Re3+", "e3e4"), VariationMove("Kc4", "d4c4")),
          replyLine = Vector(VariationMove("Kc4", "d4c4")),
          testResult = VariationTestResult.PressurePersists,
          proves = "pressure_persists_in_hold_line",
          proofPurpose = VariationProofPurpose.PreservesPressure,
          provenanceRefs = Vector(
            EvidenceRef(EvidenceRefKind.Certification, "cert-philidor-third-rank-hold", Some(owner), Some(anchor), Some(route), Some(scope))
          ),
          boundary = PreparedVariationBoundary(
            depthFloor = 12,
            realizedDepth = 18,
            multiPv = 3,
            freshnessChecked = true,
            legalReplayChecked = true,
            baselineChecked = true
          ),
          wordingCap = WordingStrength.QualifiedSupport,
          surfaceAllowance = VariationSurfaceAllowance.PublicLine,
          publicSafe = true
        ),
        PreparedVariationEvidence(
          proofId = proofId,
          boundClaimId = "cert-philidor-third-rank-hold",
          startFen = endgameFen,
          owner = owner,
          defender = Some("white"),
          anchor = anchor,
          route = route,
          scope = scope,
          role = VariationEvidenceRole.Hold,
          moveRole = VariationMoveRole.DefenderResource,
          lineSan = Vector("...Re3+", "Kc4", "Re4+"),
          lineUci = Vector("e3e4", "d4c4", "e4e5"),
          defenderResource = Some(VariationMove("...Re3+", "e3e4")),
          testedMove = Some(VariationMove("...Re3+", "e3e4")),
          testedLine = Vector(VariationMove("...Re3+", "e3e4"), VariationMove("Kc4", "d4c4"), VariationMove("Re4+", "e4e5")),
          replyLine = Vector(VariationMove("Kc4", "d4c4"), VariationMove("Re4+", "e4e5")),
          resourceLine = Vector(VariationMove("...Re3+", "e3e4"), VariationMove("Kc4", "d4c4"), VariationMove("Re4+", "e4e5")),
          testResult = VariationTestResult.DefensiveHold,
          proves = "third_rank_defensive_hold_in_shown_line",
          proofPurpose = VariationProofPurpose.Holds,
          provenanceRefs = Vector(
            EvidenceRef(EvidenceRefKind.Certification, "cert-philidor-third-rank-hold", Some(owner), Some(anchor), Some(route), Some(scope))
          ),
          boundary = PreparedVariationBoundary(
            depthFloor = 12,
            realizedDepth = 18,
            multiPv = 3,
            freshnessChecked = true,
            legalReplayChecked = true,
            baselineChecked = true
          ),
          wordingCap = WordingStrength.QualifiedSupport,
          surfaceAllowance = VariationSurfaceAllowance.PublicLine,
          publicSafe = true,
          debug =
            Option.when(includeDebug)(
              PreparedVariationDebug(
                variationHash = Some("philidor-debug-hash"),
                engineConfigFingerprint = Some("philidor-engine-config"),
                rawPacketId = Some("philidor-raw-packet"),
                rawLineIndex = Some(2)
              )
            )
        )
      )
    )

  private def exactBoardClaimWithRetrievalProof(proofId: String, includeDebug: Boolean): CommentaryClaim =
    val owner = "white"
    val anchor = "board"
    val route = "retrieval_illustration_line"
    val scope = "position_local"
    CommentaryClaim(
      id = "cert-retrieval-illustration-line",
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = Some(owner),
      beneficiary = Some(owner),
      defender = Some("black"),
      sideToMove = Some(owner),
      anchor = Some(anchor),
      route = Some(route),
      scope = Some(scope),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport,
      impact = ClaimImpact(evidenceConfidence = 80, boardExplainability = 72),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Certification, "cert-retrieval-illustration-line", Some(owner), Some(anchor), Some(route), Some(scope))
      ),
      variationEvidence = Vector(
        PreparedVariationEvidence(
          proofId = proofId,
          boundClaimId = "cert-retrieval-illustration-line",
          startFen = validFen,
          owner = owner,
          defender = Some("black"),
          anchor = anchor,
          route = route,
          scope = scope,
          role = VariationEvidenceRole.Persistence,
          moveRole = VariationMoveRole.CandidateMove,
          lineSan = Vector("e4", "Nc6"),
          lineUci = Vector("e2e4", "b8c6"),
          candidateMove = Some(VariationMove("e4", "e2e4")),
          testedMove = Some(VariationMove("e4", "e2e4")),
          testedLine = Vector(VariationMove("e4", "e2e4"), VariationMove("Nc6", "b8c6")),
          replyLine = Vector(VariationMove("Nc6", "b8c6")),
          continuation = Vector(VariationMove("Nc6", "b8c6")),
          testResult = VariationTestResult.PressurePersists,
          proves = "pressure_persists_in_shown_line",
          proofPurpose = VariationProofPurpose.PreservesPressure,
          provenanceRefs = Vector(
            EvidenceRef(EvidenceRefKind.Certification, "cert-retrieval-illustration-line", Some(owner), Some(anchor), Some(route), Some(scope))
          ),
          boundary = PreparedVariationBoundary(
            depthFloor = 12,
            realizedDepth = 16,
            multiPv = 3,
            freshnessChecked = true,
            legalReplayChecked = true,
            baselineChecked = true
          ),
          wordingCap = WordingStrength.QualifiedSupport,
          surfaceAllowance = VariationSurfaceAllowance.PublicLine,
          publicSafe = true,
          debug =
            Option.when(includeDebug)(
              PreparedVariationDebug(
                variationHash = Some("retrieval-debug-hash"),
                engineConfigFingerprint = Some("retrieval-engine-config"),
                rawPacketId = Some("retrieval-raw-packet"),
                rawLineIndex = Some(3)
              )
            )
        ),
        PreparedVariationEvidence(
          proofId = s"$proofId-defender",
          boundClaimId = "cert-retrieval-illustration-line",
          startFen = validFen,
          owner = owner,
          defender = Some("black"),
          anchor = anchor,
          route = route,
          scope = scope,
          role = VariationEvidenceRole.DefenderResource,
          moveRole = VariationMoveRole.DefenderResource,
          lineSan = Vector("...c5", "Nf3"),
          lineUci = Vector("c7c5", "g1f3"),
          defenderResource = Some(VariationMove("...c5", "c7c5")),
          testedMove = Some(VariationMove("...c5", "c7c5")),
          testedLine = Vector(VariationMove("...c5", "c7c5"), VariationMove("Nf3", "g1f3")),
          replyLine = Vector(VariationMove("Nf3", "g1f3")),
          resourceLine = Vector(VariationMove("...c5", "c7c5"), VariationMove("Nf3", "g1f3")),
          testResult = VariationTestResult.ResourceWorks,
          proves = "defender_resource_available",
          proofPurpose = VariationProofPurpose.DeniesResource,
          provenanceRefs = Vector(
            EvidenceRef(EvidenceRefKind.Certification, "cert-retrieval-illustration-line", Some(owner), Some(anchor), Some(route), Some(scope))
          ),
          boundary = PreparedVariationBoundary(
            depthFloor = 12,
            realizedDepth = 16,
            multiPv = 2,
            freshnessChecked = true,
            legalReplayChecked = true,
            baselineChecked = true
          ),
          wordingCap = WordingStrength.QualifiedSupport,
          surfaceAllowance = VariationSurfaceAllowance.PublicLine,
          publicSafe = true
        )
      )
    )

  extension (results: Vector[Either[SourceContextAdapterReject, SourceContextCandidate]])
    private def leftReasons: Vector[String] =
      results.collect { case Left(error) => error.reason }
