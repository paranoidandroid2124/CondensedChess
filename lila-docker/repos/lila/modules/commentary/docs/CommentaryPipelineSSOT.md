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

Removed product surfaces and historical diagnostics are not public authority
paths. Historical planner, thread-selection, note-composition, and compression
code is not runtime infrastructure: it lives only under
`modules/commentaryTools/src/test` for diagnostics and corpus review tooling.
The maintained `src/main` runtime must not call those helpers to decide
released commentary. Historical replay DTOs must not live in or be referenced by
`modules/commentaryCore/src/main` or `modules/commentary/src/main`.
Compatibility callers that still need historical replay diagnostics must project
through compact tooling carriers such as `DecisionFrameCarrierInput` before
entering shared runtime builders.
Runtime `GameArc` may retain generic diagnostic data such as top-engine
alternatives in memory for tooling, but its JSON writer omits raw strategic,
probe, authoring, plan-experiment, and historical note carriers.
`UserFacingPayloadSanitizer` is MoveReview/bootstrap-only and does not carry a
historical response sanitizer in the runtime API layer.

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
   authority: `ProbeBacked`, `TranspositionAligned`, `StructuralOnly`,
   `PvCoupledOnly`, `Deferred`, and `Refuted`. `StrategicPlanEvidenceView` is
   the internal typed read-model projected from the evaluator for downstream
   consumers. `ProbeBacked` and `TranspositionAligned` evaluated plans can
   enter selected main-plan authority; `TranspositionAligned` remains distinct
   provenance and is not serialized as `probe_backed`. `StructuralOnly`
   and `PvCoupledOnly` remain diagnostic/support-only and cannot own a main
   claim. `StructuralOnly` serializes as structural-only compatibility data,
   never as `evidence_backed`. Probe results must echo the exact requested FEN
   and probed move before they can satisfy a board-bound request; both echoed
   FEN strings must parse and echoed moves must be valid UCI. Multi-move
   requests require the returned `probedMove`/candidate move to be present and
   to match one of the requested moves. Unknown request purposes have no probe
   contract and fail closed even when explicit `requiredSignals` are present.
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
   Exact-slice target witness labels such as fixed targets, coordinated
   targets, weak squares, local file-entry structure markers, target-focused
   support markers, and color-complex minor-piece attack markers are formatted
   through `PlayerFacingExactSliceProofFacts`, not producer-local prefix
   assembly.
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
     observed win-percentage drop of < 10.0pp (with further limits down to 5.0pp for forcing moves); missing context or truth contract produces
     tactical-veto failure codes instead of opening authority.
     Timing-witness coupling is structured-token only. For
     `neutralize_key_break` / `counterplay_axis_suppression`, the planner
     named-break token must match the packet's typed exact-slice proof token;
     owner, anchor, structure, continuation, and generic prose terms never
     couple a timing plan to a packet.
     `MoveDelta` supported-local plan matching uses the structured `MoveLocal`
     packet plus planner source and contract admissibility; `claimText` and
     planner prose are display text and are not compared for authority.
   - `PlannerClaimAdmission` connects planner inputs to the resolver.
   - `OpeningFamilyClaimResolver` owns opening-family admission from a
     structured catalog family wire key plus `OpeningFamilyMatchProof`
     (`opening`, phase, ply, FEN). The legacy `OpeningFamilyId` enum is only a
     typed facade for older call sites; new catalog rows do not require resolver
     enum growth. The proof FEN must resolve through the static
     `OpeningNameLookup` ECO/opening book to the same requested family. Same-
     EPD transposed endpoint aliases are preserved as static book evidence for
     this resolver path, so canonical display lookup cannot suppress another
     same-position family alias that also matches the structured opening label.
     Shallow board-piece structure checks and strategic `TranspositionPvAligner`
     proofs are not opening-family authority sources. Family aliases,
     display labels, and public target-square allowlists come from
     `OpeningFamilyCatalog` main-resource TSV data, not local resolver or
     sanitizer maps. `openings.tsv` is the static book coverage pool for
    common opening-family variation endpoints. Removed fixture floors are no
    longer coverage authority; provenance cleanup is
     handled by opening audit tooling over the runtime TSV rows. The current
     pool is pruned to 1276 runtime rows that replay against captured Lichess
     masters evidence as `master-backed`; 438 live-audited
     `not-found-in-masters` expansion rows were removed. Adding rows expands
     lookup/admission coverage only through this resolver path and does not
     create a separate runtime opening authority.
     Further static pool expansion is paused pending provenance refinement:
     `OpeningPoolAudit` and `OpeningMasterDbAuditRunner` live under
     `modules/commentaryTools/src/test` and report parse problems, normalized
     endpoint duplicates/transpositions, request URLs for the configured masters
     endpoint (`--base-url`, defaulting to Lichess), OAuth-backed master
     evidence when `--live` is used, optional `--since`/`--until` query windows
     for endpoints that accept them, and replayable JSONL cache evidence
     supplied through `--evidence-cache`. Current Lichess `/masters` live audit
     should normally run without date windows because date-windowed master
     queries can return `HTTP 400`.
     Live runs may write raw-response JSONL with `--write-evidence-cache`;
     fetch or response-parse failures are surfaced as `master-fetch-error` rows
     rather than hidden as clean `unverified` entries. Reports include
     `provenanceStatusCounts`; `--only-status` is a tooling-only cleanup filter
     and does not change runtime lookup behavior. `--skip-rows`, `--max-rows`,
     and `--request-timeout-seconds` support chunked/resumable live audit.
     Cache rows bind by endpoint-stable audit `rowId` and contain `response` or
     `rawResponse` Lichess masters JSON; legacy line-number rowIds are accepted
     only for replaying older captured evidence. This tooling does not alter
     runtime lookup or make unverified rows canonical.
     `OpeningRouteMiningRunner` is also tooling-only. It derives knight-route
     candidates from the master-backed runtime opening pool, compares them to
     `opening_routes.tsv`, filters out one-ply generic development and
     repeated-square paths, and reports remaining deferred candidates; it does
     not create a separate runtime route authority.
     A structured claim needs both an opening-label family match and an
     opening-book FEN family match.
     Raw prose sentence parsing is not an authority API; unsupported family
     wording must be excluded before rendering.
   - `OpeningGoals` remains the opening-goal prose evaluator. It reads the
     post-move FEN and engine evidence from `NarrativeContextBuilder`, then
     stores the selected result on `NarrativeContext.openingGoalEvaluation` for
     `NarrativeOutlineBuilder` and `MoveReviewExplanationBuilder`. Family-
     specific additions such as `GruenfeldCenterChallenge`,
     `SlavFreeingBreak`, `DutchE4Outpost`, `QueensIndianE4Outpost`, and
     `BogoIndianE4Outpost`, plus Catalan-specific
     `CatalanTensionRelease` and `OpenCatalanPawnRecovery`, Sicilian
     `SicilianC5Challenge`, and King's Gambit `KingsGambitF4Break`, are
     board-pattern support for prose selection only; they do not replace
     `OpeningFamilyClaimResolver`, create a new opening-family carrier, or
     certify target/owner truth. The c/f-pawn opening-break prose entries do
     not expand `CentralBreakTimingWitness` or produce `central_break`
     authority. Central-break structure markers such as `central_break` and
     `break_move` come from `CentralBreakTimingWitness` rather than policy-local
     reconstruction.
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
    For single-square break tokens, collision checking uses the reviewed
    `playedMove` as legal UCI replayed from the current FEN and compares
    `move.dest.key`; SAN is player-facing display only. Missing or illegal UCI
    fails closed for the single-square row.
    The player-facing row uses checked-line chess wording rather than exposing
    the internal `SupportedLocal`/local-reading label. Tokenless rows and
    single-square tokens that collide with the played move fail closed. The row
    strips public `source`, does not expose raw proof metadata, and carries
    public `authority.kind = "counterplay_break"` plus canonical
    `authority.token` on the `chesstory.move_review.player_surface.v2` row.
    It is not a
    `CertifiedOwner` expansion. The builder may also project an `Opening family`
    summary row through the same product surface when `ctx.openingData` or an
    opening intro supplies a structured opening name,
    `OpeningFamilyCatalog.familiesForOpening` maps it to a catalog family, and
    `OpeningFamilyClaimResolver` admits that family as `SupportedLocal` against
    phase, ply, and FEN proof. This row carries
    `MoveReviewSurfaceAuthority.OpeningFamily` plus `openingFamily`; it does
    not parse rendered prose and does not trust a generic phase label alone. It
    may attach `authority.target` only when a same-family `OpeningRouteCatalog`
    descriptor is legally satisfied by `PieceRouteEvidence.fromContext`,
    `OpeningRouteTargetEvidence.checkRouteEvidence` accepts the replayed
    terminal board and `target_mode`, and `OpeningFamilyCatalog.targetAllowed`
    allowlists that family/target pair. When `ctx.openingData` carries an
    `OpeningReference`,
    the row may also attach public `authority.openingBook` metadata reduced to
    sanitized ECO, positive aggregate total-game count, and up to three SAN top
    moves. Raw explorer responses, source ids, sample games, and provenance
    caches are not emitted.
    `MoveReviewSupportedLocalSurfaceRows` may also
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
    route token; the surface gate only validates the exact witness route token
    shape and does not reclassify raw prose or expose proof ids.
    Historical precedent identifiers are not injected by
    `CentralBreakTimingWitness`; source-row catalogs may retain them in
    test/tooling, but runtime witness tags stay generic.
    `LineConsequenceEvaluator` may interpret checked PV/ref lines as typed
    local consequences (`ExchangeSequence`, `ForcingCheckSequence`,
    `CentralBreakTiming`, `CentralPawnAdvance`, `MaterialTransition`, or
    `PreviewOnly`). A consequence is player-surface eligible only when it comes
    from `MoveReviewRefs` validated by `MoveReviewPvLine` legal FEN replay.
    PV support markers (`pv:*`) are projected through
    `MoveReviewPvLine.pvMoveTerms`, so witness producers keep legal line-prefix
    traceability without repeating raw prefix formatting.
    Legal engine-only `VariationLine` replay may become
    `ReplayBackedInternal` for decision evidence, narrative hooks, and ledger
    notes, but it remains blocked from the player decision strip. The public
    ref path remains strict across the checked line. Engine-only internal
    interpretation may keep a concrete legal prefix as `ReplayBackedInternal`
    when that prefix already proves `ExchangeSequence`, `MaterialTransition`,
    `CentralPawnAdvance`, or a replayed check sequence; stale tail moves are
    trimmed from the internal evidence and cannot contribute mate/check proof.
    A legal prefix that is only `PreviewOnly` remains diagnostic-only. The public
    decision strip uses the six-ply surface window; internal narrative evidence
    may inspect an eight-ply replay window. Central-break timing consequences
    reuse `CentralBreakTimingWitness.exact` rather than duplicating
    central-break authority; replayed central pawn advances without that witness
    are described only as observed local pawn transitions. Sicilian c-pawn and
    King's Gambit f-pawn opening breaks remain OpeningGoals prose support, not
    exact `central_break_timing` witnesses.
    `MoveReviewPlayerPayloadBuilder.decisionComparisonSurface` is the sole gate
    for `moveReviewPlayerSurface.decisionComparison`: it consumes sanitized
    `DecisionComparisonDigest` plus a surface-candidate line consequence, applies
    the 35cp-or-exact-comparison/practical-alternative gate, admits same-first
    move comparisons only when a typed consequence appears after the first ply,
    and emits only the existing public decision-comparison field. Once that
    strip is admitted, the builder may attach bounded `targetComparison`
    metadata for the engine-best line versus the reviewed/chosen line. That
    metadata is computed from `WeaknessTargetProfile` at the line endpoint
    (`resultingFen` when supplied, otherwise short legal UCI replay) and does
    not create plan promotion or proof authority. Backend sanitization and the
    frontend decoder both require legal square tokens and
    authority-key-shaped target kinds; malformed comparison metadata is dropped
    instead of reconstructing a fallback comparison. The analyse frontend may
    render the surviving metadata only as a compact subordinate line-target
    note inside an already rendered decision strip; it must not synthesize
    target comparison from prose, engine raw fields, or fallback carriers. Known
    surface-blocking line reject reasons still fail closed; diagnostic tags that
    are not surface blockers do not by themselves close the strip. `PreviewOnly`
    consequences cannot provide player decision secondary text; release safety
    is typed by `LineConsequenceEvidence`, not by English phrase denylists.
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
    fallback text. `MoveReviewPlayerPayloadBuilder.ledgerRows` accepts only
    sanctioned ledger line sources (`probe`, `decision_compare`, `variation`,
    `authoring`) with non-empty SAN moves before projecting them to
    `moveReviewPlayerSurface.probeRows`; malformed ledger lines are ignored.
    Deferred decision moves are not an admitted player-surface field.
15. `CommentaryApi` serializes typed payloads through
    `MoveReviewResponsePayload`; it does not recompute boundary authority
    after the product surface is built. The MoveReview public wire omits raw
    `strategyPack`, top-level `signalDigest`, raw author/probe evidence
    summaries, concepts, plan-tier/commentary-mode controls, full
    `mainStrategicPlans`, and `strategicPlanExperiments`. It may expose the
    sanitized plan count as `mainStrategicPlanCount`, structured continuity
    tokens, an empty `probeRequests` compatibility array, backend-certified
    `moveReviewPlayerSurface`, public polish metadata, and backend-owned
    diagnostics. Top-level `moveReviewExplanation` and `moveReviewLedger`
    remain internal/cache-compatibility carriers rather than fresh public wire
    authority; sanitizer strips explanation `factFragments` and keeps a
    top-level ledger only for admitted plans with the expected schema,
    authority-key-shaped motif/stage keys, sanctioned line sources, and
    non-empty SAN line moves. Public polish metadata excludes validation reasons, token
    counts, cost estimates, and strategy-coverage diagnostics. The payload does
    not introduce a Gzip/Base64 opaque strategic token; structured continuity
    tokens remain compatibility state until a signed/versioned/expiring server
    token contract exists.
    MoveReview cache lookups build a single typed `CommentaryCache.Key` per
    request and reuse it across miss/put. Cache state fingerprints come from
    lazy, locale-stable model fingerprints rather than Play JSON
    serialization, and probe fingerprints use compact non-cryptographic hash
    rows over probe fields that can affect downstream evidence, contract,
    future-state, motif-tag authority, and authoring behavior. Opening reference data also
    contributes an opening fingerprint so cache hits cannot reuse commentary
    across different explorer/opening evidence.
    Template-quality polish skipping is language-aware: English marker scoring
    can skip AI polish only for English requests; non-English requests do not
    use English markers as a skip signal.
16. `MoveReviewResponseDiagnostics` derives backend-owned
    `diagnostics.status` and `diagnostics.sourceModeReason` from the final
    serialized response source mode, polish validation reasons, and
    `PlayerProseBoundary` fallback validation. This is the only MoveReview
    retry/fallback leak code consumed by the frontend. `PlayerProseBoundary`
    also denies leaked relation helper calls from the catalog inventory as
    prose helper symbols only; this negative gate is not a relation-admission
    path.
17. `moveReview.ts` and `responsePayload.ts` render typed
    fields and do not rebuild hidden strategic meaning from fallback carriers.
    MoveReview retry/drop behavior for fallback responses reads
    `diagnostics.status`; it must not parse commentary prose for helper labels,
    English phrases, or internal marker strings.
    Public `probeRequests` are compatibility-only empty arrays. The analyse
    frontend no longer runs a post-response raw-probe refinement fetch from
    decoded payload carriers, and `responsePayload.ts` does not accept raw
    probe or authoring arrays as fallback data. It validates optional
    target-comparison square/key metadata and silently drops malformed
    comparison metadata for that field only. `moveReview.ts` renders optional
    target-comparison text only after `moveReviewPlayerSurface.decisionComparison`
    itself exists and continues to treat it as endpoint metadata, not proof or
    planner authority.
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
the sanitizer, and only matching main-admitted evaluated plans can retain
`mainStrategicPlans` and matching `strategicPlanExperiments`: `ProbeBacked`
requires non-empty support probe ids, while `TranspositionAligned` requires
non-empty transposition proof ids plus `transposition_aligned` provenance.
The same `PlanEvidenceEvaluator.isMainAdmittedPlan` predicate is used by the
typed read-model and downstream payload/sanitizer sanity checks, so a proof id
or compatibility marker without matching typed provenance still fails closed.
Cached/default sanitize paths and historical moments do not have this
typed admission input and fail closed for strategic plan payload metadata.
`TranspositionAligned` is produced only by `TranspositionPvAligner`, which
legally replays a PV line from the current FEN, requires a supplied endpoint or
at least five UCI plies, recomputes `WeaknessTargetProfile` at the terminal
position, binds only expected weakness-target squares from the plan evidence
sources when the source is an exact `target:<square>` or
`weakness_target:<square>` token, requires attacker-minus-defender control to
be positive, and vetoes
lines with mate against the mover or mover loss above the alignment threshold.
It can select a main plan, but it does not unlock exact proof contracts,
`check_qualifying`, or `ProbeBacked` provenance.
`StructuralOnly` and `PvCoupledOnly` evaluated plans may appear only as
bounded MoveReview practical-guidance rows on `moveReviewPlayerSurface`. They
do not enter `selectedMainEvaluatedPlans`, `check_qualifying`, retained
`mainStrategicPlans`, or certified/main-claim authority.
When promoted plan advanced rows are absent or leave room, the same bounded
practical plans may contribute `Practical objective` and `Practical steps`
advanced rows from their existing preconditions and execution steps. These rows
are surface guidance only and do not create plan promotion. Practical advanced
rows are suppressed when a promoted plan already covers the same top-level
theme or at least 70% of the practical plan's execution steps, so sibling plans
do not repeat the same guidance under a softer label.
Weakness-fixation practical plans may also add a `Practical target` advanced
row. The target comes from `analysis.structure.WeaknessTargetProfile`, a
canonical board-backed profile that reuses `PositionAnalyzer` pawn primitives
for backward, isolated, IQP, doubled, and fixed pawn targets. This row is still
bounded practical guidance with `practical_plan` authority; it does not expose
`authority.target` and does not promote the plan. If the current best engine
line is available and legal replay or a supplied `resultingFen` shows that the
target is immediately liquidated by the defending side, the `Practical target`
row is suppressed. Capturing the target by the pressure side is treated as a
resolved target, not as a ghost-target veto. Same-square persistence is trusted
only when the best line supplies a `resultingFen` or at least five UCI plies;
shorter persistent lines are treated as too shallow for target-persistence
display.
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
row. They may appear only as lower-authority `Central liquidation` or
`Central challenge` practical rows when legal FEN/UCI replay classifies the
reviewed move and tactical truth does not veto support. Otherwise typed
probe-backed support remains a generic plan-advance signal.

Move-local favorable-exchange sources follow the same split between proof and
commentary. `defender_trade` and `bad_piece_liquidation` witnesses in
`PlayerFacingTruthModePolicy` are built through `MoveReviewExchangeAnalyzer`,
which performs bounded legal FEN/PV replay instead of UCI substring slicing.
The same analyzer now exposes a shared `RelationWitness` read-model for
move-local attack/defense relations. Defender-trade, bad-piece-liquidation,
overload, deflection, discovered-attack, double-check, back-rank mate,
mate-net, Greek gift, fork, hanging-piece, x-ray, clearance, battery, pin,
skewer, interference, and decoy observations enter as selector/support evidence
from the same bounded replay boundary and the same catalog-driven relation
producer. `RelationObservationCatalog`
maps implemented relation witness kinds to semantic observation ids, typed
selector sources, strategic idea kind/readiness, confidence, and beneficiary
metadata. The relation-kind inventory now keeps a non-runtime deferred list for
motif families that exist in adjacent model or detector assets but do not yet
have a board-replayed relation witness:
`zwischenzug`, `domination`, `trapped_piece`, `stalemate_trap`, and
`perpetual_check`. Each deferred catalog entry records the required
board-replayed witness shape, defer reason, and fallback lane, and projects a
catalog-owned `DeferredRelationFallback` read-model for downstream prose
consumers. The fallback projection is lower-authority guidance only:
`zwischenzug` and `trapped_piece` may degrade to practical guidance,
`domination` may degrade to thematic fallback, and
`stalemate_trap`/`perpetual_check` remain diagnostic-only because draw-resource
claims are tactical truth claims. Deferred entries do not carry source,
semantic, readiness, confidence, or public relation-surface metadata. Deferred
kinds are not produced by the relation producer and are not public relation
tokens; they remain outside `strategic_relation` until a board-replayed witness
is implemented and cataloged. The catalog exposes a single inventory view for
implemented plus deferred kinds, while runtime descriptor lookup and evidence
admission continue to resolve only implemented kinds. The intermediate
`StrategicIdeaEvidence` carrier follows the same boundary: relation identity and
relation focus are retained only when the kind is implemented in
`RelationObservationCatalog`; deferred or unknown kinds are reduced to ordinary
non-relation evidence before selector merge.
`NarrativeMotifPrefixTable`, `NarrativeOutlineBuilder` theme-keyword and
canonical motif-term prose, and `NarrativeLexicon` motif appears/fades delta
sentences consume that deferred fallback projection:
practical/thematic lanes may emit softer non-relation wording, while
diagnostic-only deferred motifs emit no motif prefix, theme keyword, or
canonical/delta term. This keeps deferred motif names from bypassing the
relation catalog through legacy motif text.
Legacy `PlanMatcher` evidence that receives a deferred domination motif also
uses the catalog fallback label rather than emitting `domination` as a plan
evidence term.
`UserFacingSignalSanitizer` routes deferred relation helper notation through
the same catalog fallback text: `Domination(...)`, `TrappedPiece(...)`, and
`Zwischenzug(...)` become softer non-relation wording, while diagnostic-only
`StalemateTrap(...)` and `PerpetualCheck(...)` are suppressed.
`NarrativeContextBuilder` also maps threat-summary motif labels through the
deferred relation catalog, so a `ThreatAnalysis` motif such as `Domination`
cannot become a raw key-threat label.
`StrategyPackBuilder` and `StructurePlanArcBuilder` use the same catalog-owned
fallback evidence term for trapped-piece activity, so sanitized strategy-pack
evidence does not expose raw `trapped_piece` relation labels before a
board-replayed trapped-piece witness exists. The final user-facing sanitizer
also strips deferred relation motif terms from cached or legacy strategy-pack
evidence lists while preserving the catalog fallback evidence term.
Deferred relation motif tags also do not promote the context beat into the
high-tension tactical opening tone; tactical tone still requires non-deferred
motifs or actual threat evidence.
`NarrativeLexicon.isMotifPrefixSignal` also checks the deferred relation catalog
before accepting a motif as a generic prefix signal, so new motif-prefix
consumers do not re-admit deferred relation names by accident.
`CommentaryIdeaSurface.motifCorroboratedByFact` follows the same rule: ordinary
facts may corroborate implemented or generic motif families, but they do not
turn a deferred relation tag such as `trapped_piece` into board-proofed motif
support.
Unknown relation witnesses do not use a generic
selector fallback; they fail closed before semantic observation emission.
Analyzer witness facts remain support facts only: `FactId.dynamic` rejects
semantic observation ids, evidence-source ids, proof-source ids, proof-family
ids, and `source:` wire keys, so only the catalog descriptor can mint the
source/semantic admission pair for a relation observation. The shared
`RelationWitness` read-model now carries typed relation details for the
implemented relation catalog, including defender-trade, bad-piece liquidation,
overload, deflection, discovered-attack, king-attack, material-target, line, and
lure-and-win relations; policy and future consumers must use analyzer-owned
detail helpers rather than parsing dynamic fact strings.
Semantic relation observations must consume the analyzer-owned
`relationProjectionFromWitness` projection, which bundles relation kind, focus,
target, and support fact terms after the typed-detail boundary has run. This
keeps semantic producers from separately reading stale raw witness focus,
target, or fact fields.
If a witness carries non-empty typed details that do not match its relation
kind, the semantic producer rejects the witness instead of falling back to raw
focus, target, or fact strings.
Semantic relation fact terms and owner/transition terms are likewise
canonicalized through the same analyzer-owned relation projection. Raw witness
facts are only fallback data for relation witnesses that do not yet carry typed
details, and invalid typed-detail mismatches do not emit branch, owner-seed, or
transition terms.
Analyzer-owned owner seed expansion uses the same relation projection, so
defender/bad-piece owner packets do not reintroduce stale raw witness focus or
fact fields while deriving owner terms.
The discovered-attack descriptor keeps a small selector margin over generic
clearance when both witnesses arise from the same cleared ray, so the specific
relation identity can reach support rows without becoming proof authority.
The analyzer treats `VariationLine.moves` as the canonical engine PV identity;
`parsedMoves` is only a fallback when the raw UCI list has no usable moves, and
parsed metadata cannot override a supplied engine line.
Exact owner witnesses use proof-sized legal prefixes, not unchecked tail text:
defender trade requires the first three replayed plies, while bad-piece
liquidation requires at least the direct capture/recapture prefix and may use a
longer legal prefix for delayed bishop liquidation. A stale or illegal PV tail
after the proof-sized prefix is ignored for the owner witness and cannot add
continuation terms; an illegal move before the required prefix still fails
closed.
The same raw-first rule is used for line-scoped SAN citations, decision
comparison best-move labels, alternative branch labels, and engine-line
evidence hooks: SAN is derived from replaying or formatting the raw UCI line
before any parsed metadata is considered.
These owner witnesses require the reviewed `playedMove`; the top PV first move
is never substituted as move identity when `playedMove` is missing.
Defender-trade release requires the traded defender to have actually defended a
non-exchange target before the replayed exchange and for that defense relation
to be removed after the recapture with the target's defender count strictly
reduced. If the traded defender disappears but another defender line opens and
preserves the target's defense count, the exact defender-trade owner path stays
closed. The target must be a declared focal target or a structural weakness
target; arbitrary defended pieces and the exchange square itself are not
fallback targets. Defender-trade family visibility must come from typed
plan/proof-family context plus the replayed branch; decision or planner text
saying "defender", "guard", "trade", or "remove" does not open the exact
defender-trade owner path. Bad-piece liquidation release requires a real bishop
on the current board, same-color central pawn constraint plus low mobility or a
central own-pawn ray blocker, and a replayed bishop capture followed by a legal
recapture of that bishop after the bishop has entered the branch. When those
proof witnesses fail, the exact owner path stays closed; legally replayed
exchange or material lines may still surface only through lower-authority line
consequence or practical/thematic guidance.
The same constrained-bishop test is reused by the strategic activity analyzer
for support/ranking signals, so a mere count of same-color central pawns no
longer marks a bishop as bad anywhere in the active commentary analysis path.

`queen_trade_shield` and bounded favorable-simplification exchange-square
checks use the same replay boundary. A queen-trade shield witness requires a
legal queen capture followed by a legal opposing king recapture of that queen;
same-destination UCI text is not enough. A bounded simplification exchange
square is recognized only from a legal immediate capture and recapture. Branch
keys, best-defense move terms, and exact-slice continuation terms for
MoveReview owner packets now come from bounded replayed moves over the current
FEN, not raw `VariationLine.moves` slicing.
`iqp_inducement` likewise evaluates only legal replayed prefixes before it
claims that an opponent central pawn became isolated; the prefix must be
anchored to the reviewed `playedMove`, and illegal, malformed, or
move-unanchored PV text yields no evaluated prefix and the witness fails
closed.
Move-delta resource-removal line evidence follows the same board boundary for
PV-derived support: break and denied-square hits are checked against replayed
origin/destination squares from the current FEN, while raw line text remains
only citation prose. `citationLine`, `whyNot`, and other rendered resource
phrases cannot promote `ResourceRemoval`; without a replayed resource hit the
line degrades to counterplay-reduction support when otherwise eligible.
The same downgrade applies to active strategic delta text: resolved-threat or
lost-motif phrases about resources, guards, or defensive cover are treated as
counterplay reduction unless a replayed resource-removal witness exists.
Generic exchange-forcing line support is also bounded to replayed capture
steps; parsed PV capture flags alone do not prove an anchored exchange.
Likewise, active strategic delta text that merely says "exchange", "trade",
or "simplify" does not create `ExchangeForcing` truth mode; it can only remain
generic plan/practical support unless a replay-backed exchange witness exists.
Weakness-target practical rows may use a `resultingFen` only when the supplied
PV legally replays to that board state; stale or non-replayable `resultingFen`
values cannot prove persistence, resolution, or liquidation.
MoveReview tactical-window gates for sacrifice classification, forcing-proof
availability, and IQP tactical-first veto use the same legal replay window for
capture/check counts. Engine mate/forced tags are considered only when that
branch replays from the current FEN. Tactical line-only evidence in
`MainPathMoveDeltaClaimBuilder` is also replay-bound: SAN text with capture or
check glyphs cannot create a tactical line proof unless a current-FEN engine PV
legally replays and satisfies the tactical window. Exchange-forcing line
evidence in `PlayerFacingTruthModePolicy` likewise requires a replay-backed
exchange packet with a branch key and continuation/structure witness; line text
containing `x`, `trade`, or `exchange` is not enough.
`BreakPreventionWitness` uses the same bounded replay boundary for its
best-defense branch key. Raw `VariationLine.moves` or parsed UCI lists cannot
make a neutralize-key-break witness exact-ready unless the branch legally
replays.
Its route-evidence line is likewise selected only from a legal replayed line:
the played move, best-line prefix, or played+continuation splice is discarded
when it does not replay from the current FEN. `CentralBreakTimingWitness`
also stores best-branch and PV structure-transition terms only from replayed
moves, while `QuietMoveIntentBuilder` derives quiet-claim best-defense move
and branch-key metadata from replay rather than parsed or raw PV text.
`StructurePlanArcBuilder` also treats played-move route contribution as
board-bound support text: it derives origin/destination squares by replaying
the reviewed move from the current FEN, so illegal or stale UCI text degrades to
generic route support instead of saying the move started or connected a route.
`L3.ThreatAnalyzer` may still use MultiPV score gaps for threat strength, but
capture-derived threat evidence, attack squares, and best-defense UCI metadata
come from a legal first-step replay over the supplied FEN.
`CommentaryIdeaSurface` consumes already-coupled MoveReview PV line facts, but
it still treats capture/recapture semantics as board truth: immediate
recapture, center-capture tension release, and opponent-reply recapture labels
are now checked by replaying the referenced reply/continuation move from the
line's FEN state. Pin/skewer continuation confirmation also replays the
continuation move before accepting that the line touches the pinned, behind,
front, or back square. SAN `x` markers and UCI file changes are display hints
only.

`StrategicSemanticObservationPipeline` is a multi-producer semantic pipeline,
not a minority-attack singleton. It now collects minority-attack and bounded
catalog relation observations. The relation producer reuses
`MoveReviewExchangeAnalyzer` and emits typed selector evidence only after legal
replay succeeds and a normalized reviewed `playedMove` is present; the producer
does not substitute the top PV
move for a missing played move. The semantic pipeline builds one bounded
top-PV replay context per collection pass, extracts relation witnesses once
from that replay, and lets the relation producer consume only implemented
catalog kinds through the shared context's kind-filtered witness access. This
avoids divergent replay windows, witness extraction, or local kind filtering.
For defender-trade, bad-piece liquidation, overload, deflection, discovered attack, double check, back-rank mate, mate-net, Greek gift, fork, hanging piece,
x-ray, clearance, battery, pin, skewer, interference, and decoy, the public role remains selector/support evidence unless a
separate proof contract admits an owner packet. Explicit target lists are
binding for relation witnesses: if an exact target is supplied, the analyzer
does not fall back to arbitrary material targets, including discovered-attack
target selection. Double-check support is king-targeted and requires the
replayed reviewed move to leave the opposing king in check from at least two
checker squares; because it is not a structural/material target relation, any
supplied explicit target list closes the double-check candidate.
Back-rank mate support reuses the existing tactical pattern detector and
requires the replayed reviewed move to end in `checkMate` with the defender's
king on the back rank; like double-check, any supplied explicit target list
closes this king-targeted relation rather than reopening material fallback.
Mate-net support reuses the existing mate-required tactical pattern detectors
other than back-rank mate, requires the replayed reviewed move to end in
`checkMate`, and carries the detector id only as a support fact; explicit target
lists close this king-targeted relation rather than reopening material
fallback.
Greek-gift support reuses the existing tactical pattern detector, requires a
legal reviewed bishop sacrifice on h7/h2 that gives check, and accepts only
immediate kingside support or support that appears by replaying the supplied PV
continuation. Explicit target lists close this pattern relation rather than
reopening material fallback.
Fork support requires a replayed move attacking at least two bound
material/king targets.
Hanging-piece support requires the replayed mover
to attack an undefended bound target. Pin relation support requires a replayed long-range move with
actual ray geometry through the pinned piece and the piece behind it; skewer
support uses the same replayed ray boundary with a higher-value front piece and
a non-pawn back target. Invalid
explicit target tokens also close the relation
candidate instead of reopening structural or material fallback. `StrategicIdeaSelector`
consumes relation
observations only through `RelationObservationCatalog`, so the same descriptor
controls the observation source, downstream `StrategyIdeaSignal` metadata, and
bounded player-surface relation labels.
The relation producer derives its active relation-kind set directly from the
implemented catalog, so new board-replayed relation kinds are not silently
skipped by a stale producer list.
For relation evidence, the analyzer-carried `targetSquare` is preserved through
`StrategicSemanticObservation`, `StrategicIdeaEvidence`, and `StrategyIdeaSignal`;
the selected idea also carries the representative `relationKind`, so surface
projection uses the selector-chosen relation before catalog-order fallback.
Relation surface target fallback is descriptor-owned: if an older carrier lacks
an analyzer target, `RelationObservationCatalog` supplies the focus-square
fallback policy instead of `MoveReviewPlayerPayloadBuilder` repeating
relation-kind branches. Legacy carriers without a selected `relationKind` do
not get to treat their generic `targetSquare` as an analyzer relation target.
The selector also keeps the chosen relation's source ref and semantic fact ahead
of generic evidence truncation, so the catalog admission pair is not lost before
`moveReviewPlayerSurface` projection.
Catalog-order fallback is allowed only for older carriers with no
`relationKind`, exactly one matching relation source/fact pair, and non-empty
`relationFocusSquares`; ambiguous or relation-focusless legacy carriers stay
silent rather than picking the first catalog descriptor or borrowing generic
idea focus.
Once a selected idea names a relation kind, the public row must match that
kind's catalog source and semantic fact or stay silent.
It also carries `relationFocusSquares`; relation rows use that relation-specific
focus only, so prose does not mix squares from another relation witness or
generic merged idea focus. Missing relation-specific focus keeps any public
relation row silent. The player-surface target uses a target carried by a
selected relation only when it is also present in the relation focus squares,
then falls back to the catalog descriptor's target policy over that same
relation focus.
Selector merge likewise carries only the relation evidence's preserved
`relationFocusSquares` and analyzer-supplied relation target; it does not
synthesize relation focus from generic `focusSquares` or relation target from
generic `targetSquare` after evidence grouping.
`StrategyPackBuilder` builds the base pack before applying the legacy
plan/route carrier gate, so a relation-only semantic observation can keep a
`StrategyPack` alive only when the selector emits a typed strategic idea from
that cataloged evidence. Empty replay, missing reviewed move identity, or
non-selector support still produces no relation pack.
These observations can support practical selector ranking, but they do not
bypass `ProofContractRules`,
`ClaimAuthorityResolver`, or the move-local owner witnesses above.
Defender-trade and bad-piece owner witnesses in `PlayerFacingTruthModePolicy`
also derive their owner branch squares and common owner/transition terms from
the same analyzer `RelationWitness` projection, keeping owner packets and
semantic rows on one replayed branch read-model. Detail decoding is owned by
analyzer helpers, not raw key reads in the policy layer. Common owner-seed and
transition term expansion is also analyzer-owned and projection-backed, so
policy code does not read `RelationWitness.facts` or `focusSquares` directly
for those owner packets. Other implemented relation rows stay support-only here, but
their king, attacker, defender/cleared-square, line or material target,
lure/execution, mate pattern, and front/back or pinned/behind coordinates are
also preserved as typed analyzer details for any later certified or
softer-label consumer. Defender-trade and bad-piece policy continuation terms
consume analyzer-built structure terms instead of reassembling relation markers
locally.
Branch-key and branch-fact string formatting is likewise centralized in
`MoveReviewExchangeAnalyzer`; policy consumers may reuse replayed witness
line moves, but they must not rebuild branch keys with local UCI slicing or
`mkString` logic, including `branch:*`, `best_branch:*`, and
`exchange_square:*` fact terms.
The same analyzer owns proof-consumer reads of probe reply coverage,
best-reply heads, displayable best-reply lines, all/best branch reply lines,
and distinct defender-resource reply heads through `probeHasReplyCoverage`,
`probeBestReplyHead`, `probeBestReplyLineDisplay`,
`probeDisplayReplyLines`, `probeAllReplyLines`, `probeBestReplyLines`, and
`probeDistinctReplyHeads`; best-reply prefix/length consumers use
`probeBestReplyPrefix` and `probeBestReplyLength`. Strategic-plan
promotion/experiment code plus authoring/candidate evidence, strategic feature
extraction, strategic ledger line candidates, heavy-piece, local-file,
route-network, two-axis, counterplay, and restricted-conversion proofs must not
keep local `replyPvs` branch/resource parsers.
Probe purpose classification is owned by `ThemePlanProbePurpose`: reply,
defense-reply, conversion-reply, route-validation, route-continuity,
author-evidence, played-counterfactual, and null-move threat consumers use its
constants and predicates instead of local raw string sets or substring checks.
Author evidence branch-cardinality gates and request budgets, required signals,
objectives, and horizons for these purpose families also use this helper
boundary.
Latent hypothesis/refutation purpose profiles, including required signals,
objectives, horizons, and default cp-loss gates, are owned by
`ProbePurposeClassifier` instead of detector/evaluator-local raw matches.
Prevented-plan evidence term projection (`counterplay_drop`,
`neutralized_break`, `denied_squares`, `denied_resource`,
`denied_entry_scope`) is owned by `PlanEvidenceEvaluator`; proof modules consume
that helper instead of repeating term formatting. Prophylactic denied-resource
class normalization and exact-slice token validation use the same evaluator
boundary. Plan certification trace terms (`universal`, `best_response`,
`stable`, `probe_backed`) and `support_probe:*` markers are likewise projected
through `PlanEvidenceEvaluator`.
Theme/subplan support tags are created and parsed through
`PlanTaxonomy.ThemeResolver`; planner, detector, and hypothesis code must not
repeat raw `theme:`/`subplan:` prefix slicing. Embedded subplan annotations on
probe plan names, their user-facing stripping, and embedded theme preconditions
use the same resolver boundary. Taxonomy-backed proof contract ids also use
this resolver rather than local prefix assembly. Structural-state and
latent-seed evidence tags are also created and parsed through
`PlanTaxonomy.ThemeResolver`.
Legacy candidate probe id families such as competitive/aggressive probes are
classified by `ProbePurposeClassifier` helpers before candidate tags,
plan-alignment labels, or why-not prose are built.
`PlayerFacingTruthModePolicy` may create suppression-local rival assessment
trace terms such as `secondary_plan:*`, `secondary_idea:*`, and `exact:*`
evidence tags inside `RivalAssessment`; these are not proof source/family
authority and must not become expansion owner names or public row kinds.
`MoveReviewPlayerPayloadBuilder` may project selected relation ideas into
`moveReviewPlayerSurface.advancedRows` with `authority.kind =
strategic_relation`, the relation token, and one relation-focus-derived target square. That is
public support metadata only: it comes from the cataloged selector evidence and
requires both the catalog source ref and the matching semantic observation fact.
The source/fact admission pair, source/fact match, and selected-`relationKind`
fallback rule are owned by `RelationObservationCatalog`, so the selector,
builder, and tests consume the same descriptor helpers rather than repeating
relation matching locally.
Additional analyzer witness facts cannot mint another catalog admission fact;
dynamic facts reject semantic/source/proof authority wire keys and remain
supplemental support evidence only.
It does not create proof/source/family authority or frontend reconstruction rights.
The final `UserFacingPayloadSanitizer` strips raw top-level
`strategyPack.strategicIdeas`; the player surface authority row is the bounded
public relation carrier. Backend and frontend sanitizers accept only relation
tokens present in `RelationObservationCatalog.Implemented`; key-shaped but
uncataloged relation ids are dropped, and cataloged relation tokens without a
valid target square are downgraded to display text.
Backend sanitization preserves `strategic_relation` authority only on
`advancedRows`, matching the builder's projection path; stale summary, probe,
or author rows lose relation authority even when the token is cataloged.
The analyse frontend decoder applies the same lane rule when reading cached or
legacy JSON: `strategic_relation` authority is accepted only inside
`advancedRows`.
The MoveReview API path passes the sanitized `strategyPack` into that builder,
so relation rows originate from the same truth-vetted strategy pack that feeds
the prose and ledger rather than from cached prose or raw carrier fields.
`PlanMatcher` does not mint `defender_trade` from a raw
`RemovingTheDefender` motif or generic reason-code text. Those signals can only
support a generic `simplification_window`/favorable-exchange idea until the
semantic replay producer or owner witness proves the defender-trade branch.
`PlayerFacingTruthModePolicy` follows the same downgrade path: generic
defender/trade prose cannot create a `trade_key_defender` owner seed. If a
legal immediate capture/recapture sequence, move-linked exchange cue, and
narrative anchor are all present, the row degrades to `simplification_window`;
without those pieces it stays generic or closed.
The generic owner fallback also strips exact favorable-exchange families
(`defender_trade`, `bad_piece_liquidation`, `queen_trade_shield`) when their
replay witness is absent, so a generic plan owner cannot bypass the analyzer
after fail-closed rejection.
`StrategicFeatureExtractorImpl` follows the same softer-label rule for
high-level concepts: raw `RemovingTheDefender` tactical tags surface as generic
`Exchange pressure`, not as a user-facing "removing defenders" concept.

## Strategic Asset Naming

Do not introduce rollout, history, or umbrella expansion labels as runtime
modules, proof families, public authority tokens, package names, or product row
kinds. When adding a new strategic asset, name the source module after the
stable domain/proof role and route it through the existing owner:
`ProofContractRules` for authority eligibility, `ClaimAuthorityResolver` and
`PlannerClaimAdmission` for admission, `StrategicSemanticObservationContext`
and `RelationObservationCatalog` for selector/support observations, and
`MoveReviewPlayerPayloadBuilder` for product surface projection. Do not create
parallel runners, carriers, or product rows from umbrella labels.

## Proof Contract Registry

`ProofContractRules` owns the relation between proof families and admissible
authority tiers. It should define only contract eligibility, required witness
shape, source compatibility, scope compatibility, and default failure taxonomy.
Required witnesses are runtime predicates: `NoTacticalVeto` fails on tactical
veto or missing tactical-context codes, and `ClaimOnlySurface` fails outside the
bounded weak-main claim surface.

It must not contain planner ranking policy or renderer wording policy.

Opening-family claims use the claim-boundary resolver instead of API regex
authority. `OpeningFamilyClaimResolver` admits only structured catalog family
wire-key claims against `OpeningFamilyMatchProof`, returning `SupportedLocal`
only when the opening label and static opening-book FEN lookup both support the
requested family. Static book proof checks all same-EPD rows retained by
`OpeningNameLookup.lookupAll`, not just the canonical display row, to avoid
move-order transposition aliases being lost. Unknown catalog keys fail closed.
Shallow piece-square structure checks are not used as opening truth and cannot
create structure-only authority. Raw rendered sentences are not parsed for opening-family names and
cannot create or suppress authority after the fact; the structured claim must be
accepted or suppressed before prose is built. Opening-family aliases are
prefix-token phrase matches against the structured opening label/book name, so
subvariation suffixes are accepted while substring accidents such as `Caro-Kann`
matching `Kan` do not create authority.
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
Generic exact target fixation may only bind a target square that is present in
the same FEN's `WeaknessTargetProfile`; focus-square text or an enemy pawn on a
named square is not enough. Multi-focus target-fixing ideas must additionally
name the selected square in the same idea id or typed evidence refs; otherwise
the square remains support-only and the exact-slice surface fails closed.
The Carlsbad fixed-target exact slice accepts the mirrored target shape:
`c6` for White-side pressure and `c3` for Black-side pressure, both with the
minority-support predicate present. The minority pawn support predicate accepts
the starting and advanced b-pawn squares (`b2`/`b4`/`b5` for White pressure and
`b7`/`b5`/`b4` for Black pressure), so an already-started minority attack does
not fail only because the original b-pawn square is empty.
Opening-route target fixation is catalog-driven. `OpeningRouteCatalog` reads the
family/target/from/via/to/max-replay data plus a `target_mode` such as
`attack_weak_pawn` or `occupy_target`; `PieceRouteEvidence` reads raw
`VariationLine.moves` before stale parsed metadata, verifies legal UCI replay,
and carries the replayed terminal `Position`. `KnightRouteEvidence` is the
knight-only exact-slice facade. `OpeningRouteTargetEvidence.checkRouteEvidence`
then verifies that the route's target mode matches the replayed terminal board,
not the root board before the route is completed. `PlayerFacingTruthModePolicy`
then uses `findRouteWitness` to scan catalog routes instead of naming the Benoni
route in source, but the selected route target must match the declared focal,
directional, idea-focus, move-ref, semantic structural-weakness, or root-board
weak-pawn target when such a target is present.
Benoni `Nf3-d2-c4` against `d6`, reversed Benoni black
`Nf6-d7-c5` / `Nb8-d7-c5` against `d3`, and King's Indian `Nf6-d7-c5` to `c5`
therefore share the same witness path. The starter route pack now also covers
52 route descriptors across Sicilian, Queen's Gambit, Slav/Semi-Slav,
Nimzo-Indian, English, Dutch, Scandinavian, Pirc/Austrian, Catalan, London,
Bird, Queen's Indian, Bogo-Indian, King's Gambit, Caro-Kann, French, Open
Games, Gruenfeld, Alekhine, and Nimzowitsch structures using the same
descriptor/evaluator path; Queen's Indian and Bogo-Indian also carry direct
`Nf6-e4` descriptors for positions where the book proof already starts after
`...Nf6`. Four bishop fianchetto rows use `PieceRouteEvidence` only for
opening-family player-surface target metadata; exact target-fixation witness
promotion remains on the `KnightRouteEvidence` path unless separately
certified. Current route data includes the master-backed mined routes with at
least five opening-row witnesses; lower-support mined candidates remain
deferred. Every route target is mirrored in `OpeningFamilyCatalog` target
allowlists so backend player-surface target projection is not silently blocked
after legal route evidence passes. Exact-slice release still requires the
surrounding board witness and proof contract; route catalog membership alone is
not public authority.

Shared pawn-target board facts live in `structure.PawnStructureTargets`.
`PlayerFacingTruthModePolicy` and `CompensationInterpretation` both consume that
helper for Carlsbad minority targets and fixed-pawn targets, so compensation
acceptance no longer carries a separate Carlsbad or Benoni pawn-shape copy.

## Renderer Boundary

Renderer authority is separated from beat assembly:

- `NarrativeOutlineBuilder` chooses and joins outline beats.
- `FragmentAuthority` decides whether tagged fragments release text.
- `NarrativeOutlineValidator` removes any leaked unsafe or helper-labeled text.

Unsafe lesson/truth fragments are dropped. Generalized support-only fragments
release only when they are move-linked, scene-grounded, evidence-backed,
planner-owned, or contract-consistent.
`NarrativeLexicon` motif prefixes delegate to the ordered
`NarrativeMotifPrefixTable`; adding a motif-prefix template is a table/factory
change, not a new `getMotifPrefix` branch. Template lookup is wording only;
motif tags and lexicon rules do not create truth or authority without the
upstream detector/proof boundary.
Greek Gift pattern truth can consume replayed continuation lines supplied to
`ForcedLineTruth`: immediate support on `h7`/`h2` or legal follow-up support
created by the PV, such as `...Kxh7 Ng5+` and later `Qh5`, is checked in the
tactical detector before a label is released.

## API And Frontend Boundary

Backend and frontend consumers must not reconstruct strategic panels or
decision comparisons from fallback-only data.

For MoveReview, the only product authority for player-visible strategic
support panels is `moveReviewPlayerSurface`:

- backend model: `MoveReviewPlayerSurface` with schema
  `chesstory.move_review.player_surface.v2`
- backend builder: `MoveReviewPlayerPayloadBuilder`
- frontend decoder: `MoveReviewPlayerSurfaceV1` accepts cached legacy payloads
  and current schema payloads; legacy rows have no public authority object
- frontend rendering: `moveReview.ts` consumes the decoded surface only; row
  `tone` and `authority.kind` may add CSS classes for bounded-support styling,
  but they do not reconstruct proof authority

`MoveReviewPlayerSurfaceRow.authority` is the public structured support
boundary for narrow row-level meaning. Allowed public fields are `kind`,
`token`, `openingFamily`, `target`, and opening-family-only `openingBook`; raw
`proofSource`, `proofFamily`, explorer source payloads, sample games, and audit
cache evidence are not public row fields. Backend sanitization preserves valid
authority objects and drops malformed or unsupported shapes. The analyse frontend applies the
same sanctioned-kind and token-shape checks when decoding cached or stale
surfaces: `counterplay_break` may carry a square or route token, while
`central_break`, `central_liquidation`, and `central_challenge` require a
route-shaped token and unsupported authority kinds are downgraded without
reconstructing fallback authority. `opening_family` rows may carry
`openingFamily` from the product builder's `SupportedLocal` resolver path.
They may carry `target` only for backend-allowlisted opening/target pairs from
`OpeningFamilyCatalog`; unsupported targets are stripped without dropping the
opening-family row. `MoveReviewPlayerPayloadBuilder` emits this target metadata
only from exact legal route evidence for the same opening family; frontend code
must still treat the target chip as metadata on an admitted support row, not as
proof reconstructed from cached prose. `openingBook` is decoded only on
`opening_family` authority and is bounded to ECO, aggregate total games, and
SAN top moves; it is display metadata, not a new admission input.
Current practical-guidance row kinds are `practical_plan`,
`central_liquidation`, and `central_challenge`. These are display support
rows, not certified owner claims and not frontend reconstruction inputs.
Cataloged relation evidence may also appear as `strategic_relation` authority
on advanced rows. Its token is restricted to a public relation key and its
target is optional square metadata; it is not a proof family, proof source, or
row admission input. Frontend rendering may show the relation token as a badge
and the target as a square chip, but it must not reconstruct relation evidence
from cached prose or legacy raw carriers.

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
Legacy or cached top-level `moveReviewExplanation` may supply an existing title
only; `factFragments` are ignored by the decoder and stripped by the backend
sanitizer. Legacy or cached top-level `moveReviewLedger` is metadata/signal
only: malformed ledger keys drop the ledger, malformed line rows are dropped,
and top-level ledger lines are not rendered as support/probe rows. Player-facing
probe/support rows come from `moveReviewPlayerSurface.probeRows`.
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
support rows from raw carriers or historical metadata. Historical QC helpers
are test/tooling-only and cannot feed MoveReview release
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
