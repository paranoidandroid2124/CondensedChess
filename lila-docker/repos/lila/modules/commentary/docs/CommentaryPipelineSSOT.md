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
Active strategic-note UI/API entrypoints are not public authority paths. Legacy
Chronicle/Active planner, thread-selection, active-note, and chronicle
compression code is not runtime infrastructure: it lives only under
`modules/commentaryTools/src/test` for historical diagnostics and corpus
review tooling. The maintained `src/main` runtime must not call Active bridge
planning, Active thread selection, Active strategic-note composition, or
Chronicle compression to decide released commentary.
`GameChronicleResponse` and `GameChronicleMoment` are test/tooling DTOs only;
they must not live in or be referenced by `modules/commentaryCore/src/main` or
`modules/commentary/src/main`. Compatibility callers that still need historical
Chronicle replay diagnostics must project through compact tooling carriers such
as `DecisionFrameCarrierInput` before entering shared runtime builders.
Runtime `GameArc` may retain generic diagnostic data such as top-engine
alternatives in memory for tooling, but its JSON writer omits raw strategic,
probe, authoring, plan-experiment, and Active-style carriers. It must not
expose Active-note payload fields, active branch dossiers, strategic-thread
lists, or `ActivePlanRef` plan tags.
`UserFacingPayloadSanitizer` is MoveReview/bootstrap-only and does not carry a
Chronicle/Active response sanitizer in the runtime API layer.

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
   claim. Probe results must echo the exact requested FEN and probed move
   before they can satisfy a board-bound request; multi-move requests require
   the returned `probedMove`/candidate move to be present and to match one of
   the requested moves. Unknown request purposes have no probe contract and
   fail closed even when explicit `requiredSignals` are present.
   Request-to-plan coupling uses exact plan id, exact seed, exact normalized
   plan name, or unbound typed theme/subplan contract alignment. When more
   than one explicit binding is supplied, all supplied bindings must match the
   same hypothesis; substring name matches and sibling relinks are not
   authority.
   Refutation probes may clear an immediate punishment, but absence of
   refutation is not positive `ProbeBacked` support.
   `PlanMatcher` compatibility policy applies immediate tactical overrides per
   distinct non-tactical theme, not per plan instance, so duplicate plans in the
   same theme cannot receive exponential score decay.
   Strategic break-file admission is centralized in `BreakFileToken`; dual-axis,
   route-network, local file-entry, heavy-piece bind, and prophylaxis analyzer
   paths must consume square/file-marker tokens rather than arbitrary letters
   from prose or Java word-boundary parsing.
7. Proof producers may emit `PlayerFacingClaimPacket` values. For exact-slice
   ownership, the packet must carry a typed `PlayerFacingExactSliceProof`
   produced in the same board/probe witness branch; owner, anchor, structure,
   and continuation terms are trace/prose data and are never reconstructed into
   proof authority downstream.
8. `ProofContractRules` states which proof families can ever reach authority
   and validates required witnesses fail-closed before supported or certified
   admission.
9. `analysis.claim` resolves claim authority:
   - `ClaimAuthorityDecision` defines the tier and failure-code model.
   - `ClaimAuthorityResolver` owns certified/support/suppressed/diagnostic
     decisions from packet, plan, and truth contract. `PositionLocal` scope is
     never authority by itself: position probes must be certified exact-slice
     packets, or a supported-local contract whose failure-code set is empty.
     `experimentConfidence` is ranking/diagnostic metadata only. Tactical veto
     softening is limited to non-tactical surfaces with a present narrative
     context, a present truth contract, no tactical-failure contract, and an
     observed cp loss of <= 30cp; missing context or truth contract produces
     tactical-veto failure codes instead of opening authority.
     Timing-witness coupling is structured-token only: UCI moves, board
     squares, and piece-square anchors may match; generic long prose words may
     not couple a timing plan to a packet.
   - `PlannerClaimAdmission` connects planner inputs to the resolver.
   - `OpeningFamilyClaimResolver` owns opening-family admission from a
     structured `OpeningFamilyId` plus `OpeningFamilyMatchProof` (`opening`,
     phase, ply, FEN); FEN structure checks use the chess board parser rather
     than manual rank string parsing. Arbitrary prose sentences are legacy
     suppression-only inputs and cannot create `SupportedLocal` opening
     authority.
   - `ClaimAuthorityPolicy` remains a compatibility facade only.
10. `QuestionFirstCommentaryPlanner` selects and ranks questions. It does not
   own low-level proof/source/scope/fallback authority. Prevented-plan
   break/timing surfaces are admitted only through the shared
   `ClaimAuthorityResolver` neutralize-key-break timing gate with a typed exact
   break witness; generic prevented threat labels or cp-only counterplay
   windows remain support material.
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
    exact owner path. The plan path requires the timing witness named-break
    token to match the packet's typed
    `PlayerFacingExactSliceProof.CounterplayAxisSuppression` token; the packet
    path consumes only that typed proof token. Packet owner, structure, anchor,
    and raw claim terms are not parsed to recover tokens. Valid examples are
    `...c5`, `c5`, or `d4-d5`; generic shared words such as `counterplay`,
    `break`, or `timing` are not witness tokens. When
    `BreakClampMaterializer` proves that the played move occupies the
    opponent break destination, the producer carries the full route token
    (`e4-e5`, `...b5-b4`) instead of the self-referential destination square.
    The player-facing row uses checked-line chess wording rather than exposing
    the internal `SupportedLocal`/local-reading label. Tokenless rows and
    single-square tokens that collide with the played move fail closed. The row
    strips public `source`, does not expose raw proof metadata, and carries
    public `authority.kind = "counterplay_break"` plus canonical
    `authority.token` on the `chesstory.move_review.player_surface.v2` row.
    It is not a
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
    leaves`), and carries public `authority.kind = "central_break"` plus the
    route token; it does not parse raw prose or expose proof ids.
    Historical precedent identifiers are not injected by
    `CentralBreakTimingWitness`; source-row catalogs may retain them in
    test/tooling, but runtime witness tags stay generic.
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
    thematic fallback (`theme_fallback` / `thematicFallbackSlots` mapped via active plan themes) generates generic
    strategic guidelines instead of falling back straight to raw move descriptions. However, if the truth contract indicates a blunder or tactical refutation, thematic fallback is disabled (fail-closed) to ensure fallback to exact factual/default move descriptions.
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
    Public `probeRequests` are compatibility-only empty arrays. The analyse
    frontend no longer runs a post-response raw-probe refinement fetch from
    decoded payload carriers, and `responsePayload.ts` does not accept raw
    probe or authoring arrays as fallback data.
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
`strategicPlanExperiments`. Cached/default sanitize paths and legacy Chronicle
moments do not have this typed admission input and fail closed for strategic
plan payload metadata.
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
Required witnesses are runtime predicates: `NoTacticalVeto` fails on tactical
veto or missing tactical-context codes, and `ClaimOnlySurface` fails outside the
bounded weak-main claim surface.

It must not contain planner ranking policy or renderer wording policy.

Opening-family claims use the claim-boundary resolver instead of API regex
authority. `OpeningFamilyClaimResolver` admits only structured
`OpeningFamilyId` claims against `OpeningFamilyMatchProof`, returning
`SupportedLocal` when either the opening label or exact FEN structure supports
the requested family. Arbitrary prose is retained only as a legacy
suppression-only guard: it can return `Suppressed` with mismatch failure codes
when a sentence mentions a family unsupported by both label and board
structure, but it cannot create `SupportedLocal` authority. Short aliases are
exact word-slice matches only, so substring accidents such as `Caro-Kann`
matching `Kan`, or `nimz` matching `nimzo-indian`, do not create authority.
Rendered prose is not split and rewritten by `CommentaryApi` for
opening-family mismatch; unsupported family prose must be excluded before
rendering, while the final prose sanitizer is presentation-only.

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
structure terms are not enough to satisfy the slice witness. The contract
matches the proof ADT case against the packet source/family and required
structured fields; it does not rebuild proof from witness-term strings.
The Carlsbad fixed-target exact slice accepts the mirrored target shape:
`c6` for White-side pressure and `c3` for Black-side pressure, both with the
minority-support predicate present.

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

- backend model: `MoveReviewPlayerSurface` with schema
  `chesstory.move_review.player_surface.v2`
- backend builder: `MoveReviewPlayerPayloadBuilder`
- frontend decoder: `MoveReviewPlayerSurfaceV1` accepts cached v1 payloads and
  v2 payloads; v1 rows have no public authority object
- frontend rendering: `moveReview.ts` consumes the decoded surface only

`MoveReviewPlayerSurfaceRow.authority` is the public structured support
boundary for narrow row-level meaning. Allowed public fields are `kind`,
`token`, `openingFamily`, and `target`; raw `proofSource` and `proofFamily` are
not public row fields. Backend sanitization preserves valid authority objects
and drops malformed or unsupported shapes.

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
support rows from raw carriers or legacy chronicle metadata. Active-note and
Chronicle QC helpers are test/tooling-only and cannot feed MoveReview release
authority.

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
