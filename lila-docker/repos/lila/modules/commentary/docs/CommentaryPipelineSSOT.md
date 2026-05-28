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
     `OpeningNameLookup` ECO/opening book to the same requested family; shallow
     board-piece structure checks are not an authority source. Family aliases,
     display labels, and public target-square allowlists come from
     `OpeningFamilyCatalog` main-resource TSV data, not local resolver or
     sanitizer maps. `openings.tsv` is the static book coverage pool for
     common opening-family variation endpoints. The Scala broad-variation
     fixture floor is no longer a coverage authority; provenance cleanup is
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
     `BogoIndianE4Outpost` are board-pattern support for prose selection only;
     they do not replace `OpeningFamilyClaimResolver`, create a new
     opening-family carrier, or certify target/owner truth.
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
    not parse rendered prose and does not trust a broad phase label alone. It
    may attach `authority.target` only when a same-family `OpeningRouteCatalog`
    descriptor is legally satisfied by `KnightRouteEvidence.fromContext`,
    `OpeningRouteTargetEvidence.checkRouteBoard` accepts the parsed board and
    `target_mode`, and `OpeningFamilyCatalog.targetAllowed` allowlists that
    family/target pair.
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
    retry/fallback leak code consumed by the frontend.
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
Cached/default sanitize paths and legacy Chronicle moments do not have this
typed admission input and fail closed for strategic plan payload metadata.
`TranspositionAligned` is produced only by `TranspositionPvAligner`, which
legally replays a PV line from the current FEN, requires a supplied endpoint or
at least five UCI plies, recomputes `WeaknessTargetProfile` at the terminal
position, binds only expected weakness-target squares from the plan evidence
sources, requires attacker-minus-defender control to be positive, and vetoes
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
requested family. Unknown catalog keys fail closed. Shallow piece-square
structure checks are not used as opening truth and cannot create structure-only
authority. Raw rendered sentences are not parsed for opening-family names and
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
`attack_weak_pawn` or `occupy_target`; `KnightRouteEvidence` verifies legal UCI
replay, and `OpeningRouteTargetEvidence.checkRouteBoard` verifies that the
route's target mode matches the parsed board. `PlayerFacingTruthModePolicy`
then uses `findRouteWitness` to scan catalog routes instead of naming the Benoni
route in source. Benoni `Nf3-d2-c4` against `d6`, reversed Benoni black
`Nf6-d7-c5` / `Nb8-d7-c5` against `d3`, and King's Indian `Nf6-d7-c5` to `c5`
therefore share the same witness path. The starter route pack now also covers
48 route descriptors across Sicilian, Queen's Gambit, Slav/Semi-Slav,
Nimzo-Indian, English, Dutch, Scandinavian, Pirc/Austrian, Catalan, London,
Bird, Queen's Indian, Bogo-Indian, King's Gambit, Caro-Kann, French, Open
Games, Gruenfeld, Alekhine, and Nimzowitsch structures using the same
descriptor/evaluator path; Queen's Indian and Bogo-Indian also carry direct
`Nf6-e4` descriptors for positions where the book proof already starts after
`...Nf6`. Current route data includes the master-backed mined routes with at
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
`token`, `openingFamily`, and `target`; raw `proofSource` and `proofFamily` are
not public row fields. Backend sanitization preserves valid authority objects
and drops malformed or unsupported shapes. The analyse frontend applies the
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
proof reconstructed from cached prose.
Current practical-guidance row kinds are `practical_plan`,
`central_liquidation`, and `central_challenge`. These are display support
rows, not certified owner claims and not frontend reconstruction inputs.

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
