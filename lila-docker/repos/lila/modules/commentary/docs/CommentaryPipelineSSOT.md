# Commentary Pipeline SSOT

This file is the canonical runtime audit for the current Chesstory
commentary-analysis pipeline.

For orientation and document roles, read `CommentaryProgramMap.md` first.
For truth signoff, read `CommentaryTruthBoundary.md`. For false-positive and
overclaim risk, read `CommentaryTrustBoundary.md`.

## Authority

This document owns the current pipeline map for:

- `modules/commentaryCore/src/main/scala/lila/commentary/analysis`
- `modules/commentary/src/main/CommentaryApi.scala`
- `app/controllers/CommentaryController.scala`
- `ui/analyse/src`
- commentary prompt/polish paths when they affect released commentary

If a runtime pipeline change makes this document stale, update this document in
the same change. Do not append dated logs; rewrite the current-state map.

## Product Surface

User-facing commentary authority is MoveReview-only.

Removed product surfaces such as Game Chronicle, Guided Review, Defeat DNA, and
Active strategic-note UI/API entrypoints are not public authority paths. Names
for Chronicle, Active, Game Arc, or whole-game replay may remain in source and
test tooling only as internal diagnostics, shared proof/planner infrastructure,
or historical fixtures.

No current change may reintroduce public payloads, frontend panels, or owner
claims from those removed surfaces without a new runtime audit.

## Runtime Path

The maintained path is:

1. Board, PGN, engine, probe, and semantic signals enter the commentary runtime.
2. `CommentaryEngine` derives move-local facts and candidate key moments.
3. `MoveTruthFrame` is built before prose.
4. `DecisiveTruthContract` projects the truth frame into the surface contract.
5. Strategic carriers are built by `StrategyPackBuilder` and normalized by
   `StrategyPackSurface`.
6. `PlanEvidenceEvaluator` validates probe evidence and owns plan promotion
   authority: `ProbeBacked`, `StructuralOnly`, `PvCoupledOnly`, `Deferred`,
   and `Refuted`. `StrategicPlanEvidenceView` is the internal typed read-model
   projected from the evaluator for downstream consumers. Only `ProbeBacked`
   evaluated plans can enter selected main-plan authority; `StructuralOnly`
   and `PvCoupledOnly` remain diagnostic/support-only and cannot own a main
   claim.
   `PlanMatcher` compatibility policy applies immediate tactical overrides per
   distinct non-tactical theme, not per plan instance, so duplicate plans in the
   same theme cannot receive exponential score decay.
   Strategic break-file admission is centralized in `BreakFileToken`; dual-axis,
   route-network, local file-entry, heavy-piece bind, and prophylaxis analyzer
   paths must consume square/file-marker tokens rather than arbitrary letters
   from prose or Java word-boundary parsing.
7. Proof producers may emit `PlayerFacingClaimPacket` values.
8. `ProofContractRules` states which proof families can ever reach authority.
9. `analysis.claim` resolves claim authority:
   - `ClaimAuthorityDecision` defines the tier and failure-code model.
   - `ClaimAuthorityResolver` owns certified/support/suppressed/diagnostic
     decisions from packet, plan, and truth contract. `PositionLocal` scope is
     never authority by itself: position probes must be certified exact-slice
     packets, or a supported-local contract whose failure-code set is empty.
     `experimentConfidence` is ranking/diagnostic metadata only. Tactical veto
     softening is limited to non-tactical surfaces with no tactical-failure
     contract and an observed cp loss of <= 30cp.
     Timing-witness coupling is structured-token only: UCI moves, board
     squares, and piece-square anchors may match; generic long prose words may
     not couple a timing plan to a packet.
   - `PlannerClaimAdmission` connects planner inputs to the resolver.
   - `OpeningFamilyClaimResolver` owns textual opening-family claim
     validation from `OpeningFamilyMatchProof` (`opening`, phase, ply, FEN);
     FEN structure checks use the chess board parser rather than manual rank
     string parsing.
   - `ClaimAuthorityPolicy` remains a compatibility facade only.
10. `QuestionFirstCommentaryPlanner` selects and ranks questions. It does not
   own low-level proof/source/scope/fallback authority.
11. `NarrativeOutlineBuilder` assembles beats from admitted inputs.
12. `analysis.render.FragmentAuthority` decides fragment release safety for
    render-only, support-only, unsafe truth, unsafe lesson, and anchor-required
    fragments.
13. `NarrativeOutlineValidator` remains the final scrubber.
14. `MoveReviewPlayerPayloadBuilder` builds the backend-certified MoveReview
    player product surface payload from selected evaluated plans, the certified move
    explanation, the strategic ledger, refs, authoring summaries, and
    internally projected supported-local rows. It does not consume raw
    `strategyPack`, raw `signalDigest`, or outbound `probeRequests` as product
    authority. `MoveReviewSupportedLocalSurfaceRows` may add a
    `Counterplay break` summary row only when `ClaimAuthorityResolver` admits a
    `neutralize_key_break` / `counterplay_axis_suppression` timing plan or
    main-path packet claim as `SupportedLocal` with no tactical veto and an
    exact owner path. The row also requires a named break token from the timing
    witness or packet owner/structure/anchor terms, such as `...c5`, `c5`, or
    `d4-d5`; generic shared words such as `counterplay`, `break`, or
    `timing` are not witness tokens, and raw claim prose is not parsed for the
    token. When
    `BreakClampMaterializer` proves that the played move occupies the
    opponent break destination, the producer carries the full route token
    (`e4-e5`, `...b5-b4`) instead of the self-referential destination square.
    The player-facing row uses checked-line chess wording rather than exposing
    the internal `SupportedLocal`/local-reading label. Tokenless rows and
    single-square tokens that collide with the played move fail closed. The row
    strips public `source`, does not expose raw proof metadata, and does not
    change the public JSON schema. It is not a
    `CertifiedOwner` expansion. `MoveReviewSupportedLocalSurfaceRows` may also
    add a `Central break` summary row when the main-path packet is admitted as
    `SupportedLocal`, `CentralBreakTimingWitness.exact` is present, the packet
    source/family is exactly `central_break_timing`, and tactical veto reasons
    are empty. The exact witness is limited to a non-capturing same-file d/e
    pawn advance whose destination is `d4`, `e4`, `d5`, or `e5`, with a
    board-backed link from the played move. Diagonal captures such as `d4-e5`
    and prep/challenge moves such as `...d7-d6` or `e2-e3` fail closed for this
    row. A played move that itself satisfies that exact same-file central-break
    shape may seed the witness even when the top PV omits the move; this is
    still a board-truth source, not a raw prose or taxonomy inference. PV gap
    and two-move branch key are retained as diagnostics, not hard gates, for
    this SupportedLocal row. The row uses the witness route token such as
    `e4-e5` or `...d6-d5`, with subordinate wording (`also plays` / `also
    leaves`); it does not parse raw prose or expose proof ids.
    `LineConsequenceEvaluator` may interpret checked PV/ref lines as typed
    local consequences (`ExchangeSequence`, `ForcingCheckSequence`,
    `CentralBreakTiming`, `CentralPawnAdvance`, `MaterialTransition`, or
    `PreviewOnly`). A consequence is player-surface eligible only when it comes
    from `MoveReviewRefs` validated by `MoveReviewPvLine` legal FEN replay.
    Legal engine-only `VariationLine` replay may become
    `ReplayBackedInternal` for decision evidence, narrative hooks, and ledger
    notes, but it remains blocked from the player decision strip. The public
    decision strip uses the six-ply surface window; internal narrative evidence
    may inspect an eight-ply replay window. Central-break timing consequences
    reuse `CentralBreakTimingWitness.exact` rather than duplicating
    central-break authority; replayed central pawn advances without that witness
    are described only as observed local pawn transitions.
    `MoveReviewPlayerPayloadBuilder.decisionComparisonSurface` is the sole gate
    for `moveReviewPlayerSurface.decisionComparison`: it consumes sanitized
    `DecisionComparisonDigest` plus a surface-candidate line consequence, applies
    the 35cp-or-exact-comparison/practical-alternative gate, admits same-first
    move comparisons only when a typed consequence appears after the first ply,
    and emits only the existing public decision-comparison field. Known
    surface-blocking line reject reasons still fail closed; diagnostic tags that
    are not surface blockers do not by themselves close the strip.
    `CommentaryApi` obtains
    `MoveReviewPolishSlots` and the planner inputs/ranked plans from the same
    `MoveReviewCompressionPolicy` runtime result, so prose and player-surface
    projection reuse one planner runtime instead of replaying planner logic or
    reconstructing from raw carriers. When the main planner fails, a mid-tier
    thematic fallback (`thematicFallbackSlots` mapped via active plan themes) generates generic
    strategic guidelines instead of falling back straight to raw move descriptions.
    Prose constraints in `MoveReviewProseContract` and `MoveReviewSoftRepair` are relaxed (allowing up to 3 sentences in single-paragraph and 6 in multi-paragraph slots). SupportedLocal strategic claims are no longer limited to claim-only formatting (supportedLocalSurfaceOnly is disabled) and can render full support paragraphs and replayed multi-move variation narratives generated by `VariationNarrativeBuilder`.
    `AlternativeNarrativeSupport` can supply close-candidate PV contrast to
    planner/compression prose when the branches are legal FEN-replayed
    `VariationLine` evidence. UCI-only source fixtures do not need
    `parsedMoves`; SAN is derived from the exact FEN. Replayed close-candidate
    prose describes both candidate branches as viable rather than projecting a
    hard `best`/inferior hierarchy onto near-top MultiPV choices.
    `ContrastiveSupportAdmissibility` admits this support only when the
    sentence is an enriched comparison (`while`, `whereas`, or `both` form).
    `QuestionFirstCommentaryPlanner` traces enriched close-candidate support
    separately from raw close-candidate carriers; raw carriers remain rejected
    as support-only material.
    `MoveReviewPlayerPayloadBuilder` is the product authority for MoveReview
    support, advanced support, ledger-backed probe rows, authoring rows, and
    decision-comparison rows when those rows are backed by certified surface
    inputs. Request-derived authoring metadata such as raw probe purposes,
    objectives, plan names, and seed IDs is not re-projected into the player
    surface. Probe-backed and authoring-backed ledger rows may surface certified
    line/eval evidence, not request/result purpose or objective metadata, raw
    source IDs, row provenance/source metadata, or `signalDigest` decision
    fallback text. Deferred decision moves are not an admitted player-surface
    field.
15. `CommentaryApi` serializes typed payloads through
    `MoveReviewResponsePayload`; it does not recompute boundary authority
    after the product surface is built. The MoveReview public wire omits raw
    `strategyPack`, top-level `signalDigest`, raw author/probe evidence
    summaries, concepts, plan-tier/commentary-mode controls, full
    `mainStrategicPlans`, and `strategicPlanExperiments`. It may expose the
    sanitized plan count as `mainStrategicPlanCount`, structured continuity
    tokens, an empty `probeRequests` compatibility array, backend-certified
    `moveReviewPlayerSurface`, public polish metadata, and backend-owned
    diagnostics. Public polish metadata excludes validation reasons, token
    counts, cost estimates, and strategy-coverage diagnostics. The payload does
    not introduce a Gzip/Base64 opaque strategic token; structured continuity
    tokens remain compatibility state until a signed/versioned/expiring server
    token contract exists.
    MoveReview cache lookups build a single typed `CommentaryCache.Key` per
    request and reuse it across miss/put. Cache state fingerprints come from
    lazy, locale-stable model fingerprints rather than Play JSON
    serialization, and probe fingerprints use compact non-cryptographic hash
    rows over probe fields that can affect downstream evidence, contract,
    future-state, and authoring behavior. Opening reference data also
    contributes an opening fingerprint so cache hits cannot reuse commentary
    across different explorer/opening evidence.
16. `MoveReviewResponseDiagnostics` derives backend-owned
    `diagnostics.status` and `diagnostics.sourceModeReason` from the final
    serialized response source mode, polish validation reasons, and
    `PlayerProseBoundary` fallback validation. This is the only MoveReview
    retry/fallback leak code consumed by the frontend.
17. `moveReview.ts`, `narrativeView.ts`, and `responsePayload.ts` render typed
    fields and do not rebuild hidden strategic meaning from fallback carriers.
    MoveReview retry/drop behavior for fallback responses reads
    `diagnostics.status`; it must not parse commentary prose for helper labels,
    English phrases, or internal marker strings.
    QC/report queue tooling consumes `moveReviewPlayerSurface` for MoveReview
    support rows; without that surface, MoveReview support and advanced rows
    fail closed instead of being reconstructed from raw carriers.

## Ownership Map

Runtime ownership boundaries:

- Truth: `MoveTruthFrame`, `DecisiveTruthContract`,
  `PlayerFacingTruthModePolicy`
- Proof contracts: `ProofContractRules`
- Plan promotion authority: `PlanEvidenceEvaluator`
- Downstream plan-evidence consumption: `StrategicPlanEvidenceView`
- Claim authority: `analysis.claim`
- Opening-family textual claim proof: `OpeningFamilyClaimResolver`
- Planner selection: `QuestionFirstCommentaryPlanner`
- Render release safety: `analysis.render.FragmentAuthority`
- Final cleanup: `NarrativeOutlineValidator`
- MoveReview player product surface payload: `MoveReviewPlayerPayloadBuilder`
  and `MoveReviewSupportedLocalSurfaceRows` for authority-resolved
  supported-local summary rows
- MoveReview fallback/retry diagnostics: `MoveReviewResponseDiagnostics`
- API payload shape: `MoveReviewResponsePayload`
- UI structure: analyse frontend modules

`QuestionFirstCommentaryPlanner` may record diagnostic fields such as
`authorityTier`, `authorityFailureCodes`, `proofFamily`, and `proofSource`, but
those fields are not a new public owner kind and do not change wire shape.

## Authority Tiers

The internal claim-authority tiers are:

- `CertifiedOwner`: exact proof contract and truth context allow the claim to
  own the player-facing explanation.
- `SupportedLocal`: exact local evidence allows bounded phrasing, usually
  qualified as a local reading.
- `DiagnosticOnly`: the runtime may trace or evaluate the idea, but it cannot
  release as a player-facing owner or support-local claim.
- `Suppressed`: the idea is blocked by tactical truth, missing proof, scope
  mismatch, fallback risk, or another explicit failure code.

Support-only, deferred, latent, or diagnostic carriers may not become owner
claims through planner, renderer, API, or frontend code.

## Plan Promotion Boundary

`PlanEvidenceEvaluator` is the single runtime owner for strategic plan
promotion. Probe-contract hard failures are board/evidence failures such as
FEN mismatch, probed-move mismatch, missing required board signals, depth-floor
failure, refutation, mate, or cp-loss beyond contract. Bookkeeping drift such
as purpose/objective label mismatch or missing/mismatched hash/fingerprint is a
soft diagnostic and does not by itself block probe-backed promotion.

`alternativeDominance` is ranking/demotion metadata, not chess refutation.
Refuted plan status is reserved for exact board/probe truth.

Current runtime consumers do not read `StrategicPlanExperiment.evidenceTier` as
player-facing authority. `NarrativeContextBuilder` creates
`ctx.strategicPlanEvidence` from `PlanEvidenceEvaluator.PartitionedPlans`, and
planner, truth-mode, quiet-intent, outline, and surface builders consume that
typed view. `StrategicPlanExperiment.evidenceTier` and
`probe_backed:validated_support` remain compatibility/reporting carriers only.
`UserFacingPayloadSanitizer` does not promote plans from that marker. The
fresh MoveReview API path passes `contextBuild.selectedMainEvaluatedPlans` into
the sanitizer, and only matching `ProbeBacked` evaluated plans with non-empty
support probe ids can retain `mainStrategicPlans` and matching
`strategicPlanExperiments`. Cached/default sanitize paths and Chronicle moments
do not have this typed admission input and fail closed for strategic plan
payload metadata.
Cache hits use `sanitizeCachedMoveReview`: a previously sanitized MoveReview
response may preserve its retained plans, matching experiments, continuity
token, and ledger only when it already has `moveReviewPlayerSurface` and its
plan `evidenceSources` are empty. Cached marker-bearing plan metadata remains
closed.

Exact proof-family names may not be inferred from a plan subplan alone. For
example, `central_break_timing` can use the exact central-break family only
when the exact board witness is present. That exact board witness is the narrow
non-capturing same-file central advance described above, either replayed on the
checked PV window or supplied by the reviewed played move itself. Captures and
prep/challenge pawn moves remain outside the product-visible `Central break`
row. Otherwise typed probe-backed support remains a generic plan-advance signal.

## Proof Contract Registry

`ProofContractRules` owns the relation between proof families and admissible
authority tiers. It should define only contract eligibility, required witness
shape, source compatibility, scope compatibility, and default failure taxonomy.

It must not contain planner ranking policy or renderer wording policy.

Opening-family textual claims use the claim-boundary resolver instead of API
regex authority. `OpeningFamilyClaimResolver` parses the claimed family,
checks `OpeningFamilyMatchProof`, and returns `SupportedLocal` when either the
opening label or exact FEN structure supports each claimed family. It returns
`Suppressed` with mismatch failure codes when any claimed family lacks both
label support and board-structure proof. Short aliases are exact-match only, so
substring accidents such as `Caro-Kann` matching `Kan` do not create authority.
`RuleTemplateSanitizer` may neutralize released prose from that decision, but
it does not own the opening-family proof rules.

Current explicit promoted family:

| proof family | proof source | status | certified | supported local | default failure |
| --- | --- | --- | --- | --- | --- |
| `color_complex_squeeze` | `color_complex_squeeze_probe` | `Releasable` | true | true | `color_complex_authority_closed` |

Color-complex support is promoted only by the exact board witness in
`PlayerFacingTruthModePolicy`: parsed FEN, opponent-owned semantic weak square,
color-complex/hole/fianchetto evidence, actual friendly bishop/knight attack
geometry on that square, same-square surface/semantic evidence, and a proven
stable packet branch. Coordinate/minor-piece terms remain trace labels only.
All `ExactSlice` contracts now require a typed `PlayerFacingExactSliceProof`
attached to `PlayerFacingProofPathWitness`; generic anchor, continuation, or
structure terms are not enough to satisfy the slice witness.

## Renderer Boundary

Renderer authority is separated from beat assembly:

- `NarrativeOutlineBuilder` chooses and joins outline beats.
- `FragmentAuthority` decides whether tagged fragments release text.
- `NarrativeOutlineValidator` removes any leaked unsafe or helper-labeled text.

Unsafe lesson/truth fragments are dropped. Generalized support-only fragments
release only when they are move-linked, scene-grounded, evidence-backed,
planner-owned, or contract-consistent.

## API And Frontend Boundary

Backend and frontend consumers must not reconstruct strategic panels or
decision comparisons from fallback-only data.

For MoveReview, the only product authority for player-visible strategic
support panels is `moveReviewPlayerSurface`:

- backend model: `MoveReviewPlayerSurface`
- backend builder: `MoveReviewPlayerPayloadBuilder`
- frontend decoder: `MoveReviewPlayerSurfaceV1`
- frontend rendering: `moveReview.ts` consumes the decoded surface only

Raw payload fields such as `strategyPack`, top-level `signalDigest`,
`authorQuestions`, `authorEvidence`, `concepts`, full `mainStrategicPlans`,
and `strategicPlanExperiments` are not part of the current public MoveReview
wire. `MoveReviewResponsePayload` exposes `mainStrategicPlanCount` instead of
full plan arrays for UI metadata, so marker strings or experiment tiers cannot
be consumed by the frontend as admission inputs. Legacy/cached payloads may
still be decoded defensively, but raw carriers are not product authority for
the MoveReview support panel. Cache-hit serialization uses the same minimized
payload shape from the prior fresh response rather than rerunning marker-based
admission.
`probeRequests` remain a compatibility array on the public schema, but the
current sanitized MoveReview response emits it empty. Public MoveReview does
not expose raw probe orchestration requests; probe metadata is not
support-panel authority.
MoveReview API responses also include backend-owned `diagnostics` with
`status` and `sourceModeReason`. `fallback_available` means deterministic
fallback prose can be rendered, `retryable_fallback` means the frontend should
retry or ignore that response, and `ready` covers non-fallback accepted modes.
The frontend must not infer those states from commentary text, source-mode
prefixes, or polish validation strings on its own. Frontend response decoding
and stored snapshot restore also strip internal polish validation reasons,
token/cost accounting, and strategy-coverage diagnostics so stale storage
cannot reintroduce them as DOM metadata.

Forbidden reconstruction sources include:

- `topEngineMove`
- `cpLossVsChosen`
- latent plans
- deferred carriers
- support-only packets
- free-form strategy text
- raw `strategyPack`, `signalDigest`, `authorEvidence`, `probeRequests`,
  full `mainStrategicPlans`, or strategic-plan experiment metadata
- absent decision-comparison diagnostics

If a cached or legacy MoveReview response lacks `moveReviewPlayerSurface`, the
frontend may render the base commentary/html and existing title only. It must
not rebuild the strategic support panel from raw diagnostic fields.

Corpus and QC tooling follows the same boundary. MoveReview performance
reports derive `supportRows` and `advancedRows` from
`moveReviewPlayerSurface`. Raw carrier reconstruction is not used for
MoveReview QC support rows. If a MoveReview raw artifact or canonical surface is
absent, QC queues keep the MoveReview commentary only and do not synthesize
support rows from raw carriers or chronicle metadata. Active-note QC rows also
avoid exporting chronicle objective/focus/execution metadata as support rows.

## Current Verification Targets

After claim authority, planner, proof-contract, renderer, or package-boundary
changes, run compile plus targeted tests serially. Relevant targets include:

- claim authority / proof contract tests
- `QuestionFirstCommentaryPlannerTest`
- surface replay / cross-surface trust regression tests when present in the
  current worktree
- color-complex tests
- commentary core compilation when package paths move

Do not run parallel `sbt` commands in this worktree.

## Maintenance Triggers

Update this file when any of these change:

- runtime path or module ownership
- claim authority tiering or resolver behavior
- proof contract eligibility
- planner admission or ranking inputs
- renderer release rules
- API/commentary payload semantics
- frontend commentary consumption
