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

This document is not a license to add parallel wrappers around an already
mapped boundary. For no-behavior-change cleanup, verify the live implementation
and call sites, remove or collapse duplicated responsibility where possible, and
preserve the documented truth/trust contract without minting new authority
surfaces.

## Product Surface

User-facing commentary authority is MoveReview-only.

Removed product surfaces such as Game Chronicle, Guided Review, Defeat DNA, and
Active strategic-note UI/API entrypoints are not public authority paths. The
legacy Chronicle/Active runtime and tooling are deleted: `GameChronicle*`,
`GameArc*`, Active-note DTOs, `ActivePlanRef` tags, branch/thread payloads,
bridge/thread-selection/planner code, compression/evaluation helpers, and
full-game narration tools must not be referenced by the maintained MoveReview
runtime. `UserFacingPayloadSanitizer` is MoveReview/bootstrap-only and does not
carry a Chronicle/Active response sanitizer in the runtime API layer.

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
     `MoveDelta` supported-local plan matching uses
     `ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision` over the
     structured `MoveLocal` packet plus planner source, contract admissibility,
     exact-slice requirements, and tactical veto codes; `claimText` and planner
     prose are display text and are not compared for authority.
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
     `BogoIndianE4Outpost`, plus Catalan-specific
     `CatalanTensionRelease` and `OpenCatalanPawnRecovery`, Sicilian
     `SicilianC5Challenge`, and King's Gambit `KingsGambitF4Break`, are
     board-pattern support for prose selection only. `openingData` by itself
     may keep this carrier open only in the early opening-data ply window;
     later non-opening phases need an opening phase or explicit opening event.
     Opening goals do not replace `OpeningFamilyClaimResolver`, create a new
     opening-family carrier, or certify target/owner truth. The c/f-pawn
     opening-break entries do not expand `CentralBreakTimingWitness` or product
     `central_break` authority, but achieved or partial board-pattern opening
     break goals may later surface as lower-authority `Opening break`
     `PracticalPlan` rows only when the reviewed pawn move legally replays,
     matches the selected `OpeningGoals` trigger, and the top PV legally starts
     with that same move. The Dutch, Queen's Indian,
     and Bogo-Indian e4 outpost goals may surface only as a separate
     lower-authority `Opening outpost` row after the
     post-move board-pattern goal is achieved and the reviewed move legally
     replays as a knight move to the exact outpost square named by the goal
     evidence, with the legal top PV starting from that same reviewed move.
10. `QuestionFirstCommentaryPlanner` selects and ranks questions. It does not
   own low-level proof/source/scope/fallback authority. Prevented-plan
   break/timing surfaces are admitted only through the shared
   `ClaimAuthorityResolver` neutralize-key-break timing gate with a typed exact
   break witness; generic prevented threat labels or cp-only counterplay
   windows remain support material. Single-line planner evidence strips any
   existing `Line:`/branch label before adding the canonical branch label, and
   probe-request reminder prose is not promoted into branch-scoped line
   evidence. Tactical-failure scene ownership consumes the same
   `DecisiveTruthContract.blocksStrategicSupport` boundary used by player
   support vetoes, so a tactical-failure `failureMode` cannot be outranked by
   quiet intent, opening, endgame, or strategic-plan translators. The fresh
   MoveReview compression basic-explanation lane also consumes this boundary:
   positive `opening_goal`, `certified_strategy_support`, and
   `basic_move_explanation` sources are rejected when strategic support is
   blocked, leaving tactical or exact factual fallback to own the prose.
   `MoveReviewCompressionPolicy` does not treat a selected `QuestionPlan.claim`
   as renderer authority by itself. High-risk causal question kinds (`WhyThis`,
   `WhyNow`, `WhatChanged`, `WhatMustBeStopped`, and `WhosePlanIsFaster`) must
   first pass the `MoveReviewCausalClaim` gate with a concrete causal anchor and
   typed support evidence (`contrast`, branch line, timing tension, certified
   consequence, or timing witness). If that gate fails, planner selection remains
   diagnostic/runtime trace, but MoveReview prose falls through to the existing
   basic, exact factual, or thematic fallback lanes. Templates and polish slots
   may surface the certified text; they do not invent the causal relationship.
   The gate builds a typed causal evidence candidate before admission. Each
   evidence item records kind, source, subject role, line binding, and
   guardrails, so renderer-facing authority is measured from typed evidence
   rather than from words such as `pressure`, `support`, `threat`, or `now`.
   Support kind is not enough by itself: the same gate also classifies the
   causal relation (`alternative_contrast`, `played_move_consequence`,
   `timing_constraint`, `defensive_resource`, `plan_race`, or
   `change_consequence`). `WhyThis` prose may render only with admissible
   alternative contrast or played-move-owned consequence; branch/PV evidence and
   coda text alone remain support, not role authority.
   When the certified relation is `WhyThis` + `alternative_contrast`, the
   admissible contrast sentence is the certified surface claim. The raw planner
   claim is not kept ahead of it as the first paragraph, and the renderer may
   omit a duplicate visible support paragraph while retaining the typed causal
   guardrail.
   When the certified relation is played-move-owned line consequence,
   `MoveReviewCompressionPolicy` may likewise replace the raw planner claim
   with a `LineConsequenceEvaluator.surfaceCandidate` narrative, but only for
   `WhyThis` or `WhatChanged`, only from FEN-validated `MoveReviewRefs`, and
   only when the typed consequence is not `PreviewOnly`. Engine-only
   `ReplayBackedInternal` lines and preview-only legal prefixes remain
   diagnostic/support material for this gate; branch text alone is not promoted
   into a played-move consequence surface.
   `WhatChanged` uses the same role check: `change_consequence` may be backed by
   the played-move-owned consequence relation or by admissible contrast, but a
   certified coda/branch line without played-move ownership no longer opens a
   causal change claim.
   `WhyNow` uses an even narrower timing relation: generic urgency/tension text
   can remain visible support only after a surface is admitted, but it does not
   create `timing_constraint` authority by itself. The timing relation opens
   only from a `QuestionPlanTimingWitness` or from an admissible contrast trace
   whose source kind is `explicit_reply_loss`.
   `opening_relation_translator` is narrower still: a `WhyThis` opening-relation
   claim needs admissible contrastive support before it can render. Branch/PV
   evidence or a line consequence alone can remain diagnostic support, but it
   cannot make the played move, the preferred alternative, and the checked line
   own the same causal story.
   The fallback lane now applies the same ownership direction to
   `basic_move_explanation` and scoped takeaways. `MoveReviewLocalFact` owns
   the shared typed local-fact vocabulary and the candidate/admit policy. It
   accepts source, subject, anchor, line-binding, and evidence-ref inputs and
   returns an admitted `local_fact_family` (`attack`, `defense`, `threat`,
   `pressure`, `plan_support`, `line_consequence`, `timing`, plus existing
   opening, king-safety, capture, and endgame families) plus
   `local_fact_authority`, while preserving line binding and evidence refs on
   the admission for downstream diagnostics and renderer gating.
   `CommentaryIdeaSurface` submits candidates and
   attaches only the admitted result to every admitted MoveReview basic
   descriptor before it renders prose. When `MoveReviewCausalClaim` rejects a high-risk
   planner row, `MoveReviewCompressionPolicy` calls the basic builder in strict
   local-fact mode. Soft line-only or strategic-plan descriptors that are not
   strict-eligible fall through to exact factual fallback rather than producing
   a new causal or strategic explanation. Canonical fact descriptors must be
   played-move-owned before release: target pressure requires the reviewed move
   to be a recorded attacker, defensive wording requires only-move defense truth,
   and endgame facts must be owned by the reviewed move role/square.
   `MoveReviewScopedTakeaway` consumes the admitted local fact metadata and
   gates every scoped purpose by admitted family plus `pv_coupled` line binding.
   It may show the next checked-line move and the bounded local detail, but it
   does not turn a line-only compatibility default or purpose string into a
   target, threat, defense, plan-support, timing, or line-consequence claim.
   Accepted high-risk planner claims carry the same `MoveReviewLocalFact`
   admission shape on the causal claim itself.
   `MoveReviewCausalClaim.CertifiedClaim`
   records the emitted family, authority, strict flag, and local guardrails
   before the planner slot can render. Support-required questions without an
   admitted local fact are rejected with `local_fact_admission_missing` instead
   of relying on a grammatical claim string. Planner rows therefore report
   `moveReviewLocalFactStatus=emitted` from the rendered causal claim, not from
   a basic fallback candidate that happened to be preempted.
   Planner local-fact admission is source/evidence based, not text inferred:
   line-consequence authority opens from `LineConsequenceEvaluator`
   `SurfaceCandidate` output or a typed `pv_delta` owner, timing opens from a
   timing relation/witness, defense from forcing-defense relation/truth, and
   pressure/opening/endgame from their typed owner families. That
   source/evidence mapping now lives in `MoveReviewLocalFact.admitPlanner`,
   while `MoveReviewCausalClaim` supplies the certified relation/evidence
   kinds. Strategic basic descriptors likewise use `MoveReviewLocalFact`'s
   centralized `PlayerFacingMoveDeltaClass` -> local-fact-family mapping.
   Words such as
   `supports`, `plan`, `pressure`, `target`, or `threat` inside the rendered
   sentence do not mint a local fact family.
   Basic/scoped rows preserve the same admission on the internal
   `MoveReviewPolishSlots.localFact` carrier; `reasonTags` may still expose
   compatibility/display tags, but tooling must not treat those strings as the
   local-fact authority source.
   Tooling copies this through `MoveReviewCoverageDiagnostics` and corpus
   `moveReviewLocalFact*` fields so PGN/engine runs can count emitted,
   candidate, and strict-rejected local fact families without parsing prose.
   `MoveReviewCompressionPolicy.causalClaimTrace` records the same gate result
   that slot rendering uses. `RuntimeResult`, `MoveReviewPlannerTrace`, and
   `MoveReviewCorpusRunner` expose the status, question, subject role,
   evidence kinds, relation kinds, reject reasons, support-embedded flag, and
   guardrail through `moveReviewCausalClaim*` fields so corpus review can
   measure accepted versus rejected causal authority without parsing prose.
   The same role boundary applies after fallback. `basic_move_explanation`,
   `CommentaryIdeaSurface`, and `MoveReviewScopedTakeaway` may state the played
   SAN, the next checked-line move, and the bounded local evidence that survived,
   but they must not describe a target as newly created, forced to be answered,
   or verified by the PV unless a typed causal claim/evidence carrier authorized
   that relationship. Basic/scoped surfaces also keep checked-line replies
   role-neutral (`next checked-line move`) and filter generic display anchors
   such as `plan activation lane`, `plan`, and `main plan` before rendering.
11. `NarrativeOutlineBuilder` assembles beats from admitted inputs.
    Reply SAN display derived from raw UCI lines uses legal replay of the first
    move before formatting the reply; an illegal first move leaves the display
    fallback closed instead of formatting from the original FEN.
12. `analysis.render.FragmentAuthority` decides fragment release safety for
    render-only, support-only, unsafe truth, unsafe lesson, and anchor-required
    fragments.
13. `NarrativeOutlineValidator` remains the final scrubber.
14. `MoveReviewPlayerPayloadBuilder` builds the backend-certified MoveReview
    player product surface payload from selected evaluated plans, the certified move
    explanation, the strategic ledger, refs, authoring summaries, and
    internally projected supported-local rows. It does not consume raw
    `strategyPack`, raw `signalDigest`, or outbound `probeRequests` as product
    authority. During fresh API construction it receives the same
    `DecisiveTruthContract` used by the prose pipeline; when that contract is a
    blunder, missed win, bad tactical refutation, or tactical failure mode
    (`blocksStrategicSupport`), the builder suppresses strategic/practical
    support rows rather than pairing a failure explanation with plan-positive
    support. `MoveReviewSupportedLocalSurfaceRows` may add a
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
    It is not a `CertifiedOwner` expansion. When multiple exact/supported-local
    candidates independently pass their authority gates, the supported-local
    projector may return at most three distinct player summary rows. When a
    certified typed surface carrier such as route-network, dual-axis bind, or
    restricted-defense conversion is present alongside packet/plan rows, the
    projector preserves the leading packet/plan row and reserves the remaining
    slots for admitted typed surface rows. Practical fallback rows still appear
    only when no exact/supported-local row survives.
    The builder may also project an `Opening family`
    summary row through the same product surface when `ctx.openingData` or an
    opening intro supplies a structured opening name,
    `OpeningFamilyCatalog.familiesForOpening` maps it to a catalog family, and
    `OpeningFamilyClaimResolver` admits that family as `SupportedLocal` against
    phase, ply, and FEN proof. This row carries
    `MoveReviewSurfaceAuthority.OpeningFamily` plus `openingFamily`; it does
    not parse rendered prose and does not trust a broad phase label alone. It
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
    `OpeningReference.sampleGames` remains internal replay support for
    `OpeningPrecedentBranching` and opening-relation claims; full PGN samples
    are normalized by stripping tag-pair lines and results before SAN route
    extraction, and catch-all opening macro families such as `other` or
    `unknown` are not valid peer-precedent keys. Planner-owned opening-relation
    replay additionally requires at least two total/sample games plus an
    explicit opening event or the early opening-data ply window; self-only
    replay remains support-only.
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
    The current product-surface witness is the Maderna exact scene at
    `nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17`,
    played `e4e5`, with the typed `CentralBreakTiming("e4e5", "e5", "e4-e5")`
    packet. This is a MoveReview player-surface witness, not a promotion of the
    source-catalog Maderna row, which remains deferred in source review.
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
    are described only as observed local pawn transitions. Exchange-sequence
    narration must describe the most salient replayed exchange rather than
    blindly using the first capture; a pawn-capture prelude does not hide a
    later queen trade on the same checked line. Sicilian c-pawn and
    King's Gambit f-pawn opening breaks remain `OpeningGoals` support, not exact
    `central_break_timing` witnesses; when the post-move board-pattern
    evaluation is achieved or partial, tactical truth does not veto support,
    the selected `OpeningGoals` trigger matches the reviewed pawn move, and the
    top PV legally starts with that move,
    `MoveReviewSupportedLocalSurfaceRows` may expose them as an `Opening break`
    row with `authority.kind = "practical_plan"` and no token. The same
    lower-authority fallback may expose the bounded Dutch/Queen's Indian/
    Bogo-Indian `...Ne4` goal as an `Opening outpost` row only when the
    achieved goal evidence names the outpost square and the reviewed UCI move
    legally replays as a knight landing on that square, with the legal top PV
    starting from that same reviewed move.
    `MoveReviewPlayerPayloadBuilder.decisionComparisonSurface` is the sole gate
    for `moveReviewPlayerSurface.decisionComparison`: it consumes sanitized
    `DecisionComparisonDigest` plus a surface-candidate line consequence, applies
    the 35cp-or-exact-comparison/practical-alternative gate, admits same-first
    move comparisons only when a typed consequence appears after the first ply,
    and emits only the existing public decision-comparison field. Once that
    strip is admitted, the builder may attach bounded `targetComparison`
    metadata for the engine-best line versus the reviewed/chosen line. That
    metadata is computed from `WeaknessTargetProfile` at the line endpoint
    (`resultingFen` when supplied, otherwise short legal UCI replay), but only
    when the engine-best branch has a non-empty move head distinct from the
    reviewed move, and does not create plan promotion or proof authority. Backend sanitization and the
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
    reconstructing from raw carriers. When the main planner and exact local
    factual fallback both fail, a lower mid-tier thematic fallback
    (`theme_fallback` / `thematicFallbackSlots` mapped via active plan themes)
    generates short move-facing theme summaries instead of raw move
    descriptions or repeated `The strategic plan is...` boilerplate.
    However, if the truth contract indicates a blunder, missed win, or tactical
    refutation, thematic fallback is disabled (fail-closed) to ensure fallback
    to exact factual/default move descriptions. Fresh MoveReview early-opening
    prose clamping consumes the same truth contract so critical/tactical escape
    hatches are not lost when intro-only opening prose is compacted.
    Slot polish prompts preserve concrete slot facts, SAN tokens, move numbers,
    and square anchors, but they do not require the first clause to be copied
    verbatim; repair prompts follow the same fact-preserving/paraphrase-allowed
    boundary while still banning system/sidecar language. Polish validation
    canonicalizes SAN case, treats duplicate draft SAN mentions as one required
    ordered core occurrence, and allows equivalent or omitted eval tokens rather
    than forcing template rollback for harmless formatting cleanup. Prose
    constraints in
    `MoveReviewProseContract` and `MoveReviewSoftRepair` are relaxed (allowing
    up to 3 sentences in single-paragraph and 6 in multi-paragraph slots).
    SupportedLocal strategic claims can render full support paragraphs and
    replayed multi-move variation narratives generated by
    `VariationNarrativeBuilder`; only support-free planner slots collapse to
    claim-only formatting. Planner-owned claim-only MoveReview slots still pass
    through the same move-header prefixing as supported slots, so a support-free
    rule-mode claim does not lose the reviewed move label.
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
    Bad tactical truth-contract surfaces suppress plan/strategy/relation support
    rows; factual explanation, refs, decision comparison, ledger, and authoring
    rows keep their own certification gates.
    During fresh MoveReview construction, the builder may also project the
    current backend-built `moveReviewExplanation.shortLine` plus its bounded
    `pvInterpretation.learningPoint` into an authority-free `Checked line`
    summary row. This is a player-surface projection from fresh builder inputs,
    not reconstruction from cached top-level explanation JSON or raw fragments.
    If that explanation line is absent, the builder may project the preferred
    current-move `MoveReviewRefs` variation into the same authority-free
    `Checked line` row so planner-owned prose has a visible checked line without
    parsing free-form prose.
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
    `PlayerProseBoundary` fallback validation. This is the MoveReview
    retry/fallback status producer consumed by the frontend; the detailed
    trust rule that fallback prose is never a relation-admission path is kept
    in `CommentaryTrustBoundary.md`.
17. `moveReview.ts` and `responsePayload.ts` render typed
    fields and do not rebuild hidden strategic meaning from fallback carriers.
    MoveReview retry/drop behavior for fallback responses reads
    `diagnostics.status`; prose-parsing prohibitions are tracked in
    `CommentaryTrustBoundary.md`.
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
    support rows: `summaryRows` and backend `decisionComparison` become support
    rows, while `advancedRows`, `probeRows`, and author rows become advanced
    review details. Without that surface, MoveReview support and advanced rows
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
fresh MoveReview API path passes the full `ctx.strategicPlanEvidence.evaluatedPlans`
read-model into `MoveReviewPlayerPayloadBuilder` so bounded practical rows can
see `StructuralOnly` and `PvCoupledOnly` plans, while plan retention still uses
`contextBuild.selectedMainEvaluatedPlans` as the sanitizer admission input. Only
matching main-admitted evaluated plans can retain `mainStrategicPlans` and
matching `strategicPlanExperiments`: `ProbeBacked`
requires non-empty support probe ids, while `TranspositionAligned` requires
non-empty transposition proof ids plus `transposition_aligned` provenance.
The same `PlanEvidenceEvaluator.isMainAdmittedPlan` predicate is used by the
typed read-model and downstream payload/sanitizer sanity checks, so a proof id
or compatibility marker without matching typed provenance still fails closed.
Outline strategic-stack context sentences consume the typed
`mainAdmittedPlanHypotheses` projection, so a `TranspositionAligned` selected
main plan is not hidden behind probe-only support helpers.
Cached/default sanitize paths and legacy Chronicle moments do not have this
typed admission input and fail closed for strategic plan payload metadata.
`TranspositionAligned` is produced only by `TranspositionPvAligner`, which
legally replays a PV line from the current FEN, requires a supplied endpoint or
at least five UCI plies, recomputes `WeaknessTargetProfile` at the terminal
position, binds only expected weakness-target squares from the plan evidence
sources when the source is an exact `target:<square>`,
`weakness_target:<square>`, `fixed_target:<square>`, or
`coordinated_target:<square>` token, or a route/weak-complex exact square token
such as `target_fixing:<square>`, `enemy_weak_square:<square>`, or
`weak_complex:<square>`, requires attacker-minus-defender control to
be positive, and vetoes
lines with mate against the mover or mover loss above the alignment threshold.
It can select a main plan, but it does not unlock exact proof contracts,
`check_qualifying`, or `ProbeBacked` provenance.
`StructuralOnly` and `PvCoupledOnly` evaluated plans may appear only as
bounded MoveReview practical-guidance rows on `moveReviewPlayerSurface`. They
do not enter `selectedMainEvaluatedPlans`, `check_qualifying`, retained
`mainStrategicPlans`, or certified/main-claim authority.
For `StructuralOnly` practical-plan summary rows, `MoveReviewPlayerPayloadBuilder`
may add the typed structure label when the current semantic
`StructureProfileInfo` is not `Unknown`, has at least 0.70 confidence, and the
paired plan-alignment band is `OnBook` or `Playable`, with the matched
alignment plan or narrative intent also matching the bounded plan name. Plan
matching is exact against normalized plan text and typed matched-plan ids; prose
containment still requires the contained phrase to have at least three words, so
broad labels such as `pressure`, `attack`, `break`, or `queenside pressure` do
not unlock structure prose. If a `StructurePlanArcBuilder` arc is prose-eligible
and its plan label or typed `matchedPlanIds` match the bounded plan name, the
row may reuse that existing structure/route prose. Both
forms keep `authority.kind = "practical_plan"`, publish no target metadata, and
do not convert the structure read or playbook entry into exact motif authority.
When the profile fallback path is used, the row may also include the typed
`centerState` as current-board structure context, for example `with the center
locked`, after the same profile-confidence, plan-alignment, and plan-match gates
pass.
The same plan-matched, prose-eligible structure arc may add one `Practical
route` advanced row using `StructurePlanArcBuilder.supportPrimaryText`. That
row describes a safe, lower-authority piece deployment cue from the current
board, semantic piece activity, and route-safety assessment; it does not claim
an exact route proof, file-entry proof, outpost proof, or supported-local
ownership.
`Practical move` may appear beside it only when the reviewed `playedMove`
legally replays as a one-ply UCI move from the current FEN, starts from the
arc's deployment origin, and lands on one of that arc's route squares. Fallback
route prose, SAN-only pawn-move guesses, unrelated legal moves, same-file
reinforcement, adjacent-route guards, and other pieces landing on route squares
do not open this row.
If the same plan-matched arc also carries current-board `prophylaxisSupport`
from the first semantic `PreventedPlanInfo` with `sourceScope = Now`, it may add
one `Practical restraint` advanced row only when that prevented plan also has a
positive `counterplayScoreDrop` or negative `mobilityDelta`. Line-scoped/citation
prophylaxis support, and default-current plans without those current-board
impact signals, do not enter this row; they remain outside the board-structural
practical fallback because they would imply continuation authority or unsupported
causality.
The plan-matched structure arc may also add one cautionary `Practical fit` row
from non-positive `PlanAlignmentInfo.reasonCodes` already humanized by
`StructurePlanArcBuilder`, such as missing structural preconditions. The row is
lower-authority context about why the structure fit is partial; `PA_MATCH` alone
does not create it, and it does not publish owner, motif, target, or proof
authority.
When that same plan-matched, prose-eligible structure arc carries
`practicalCoda`, MoveReview may add one `Practical task` row. The row is allowed
only as a practical verdict riding on the already admitted structure arc; it
does not make `PracticalInfo.verdict` an independent structural witness or exact
authority source.
When promoted plan advanced rows are absent or leave room, the same bounded
practical plans may contribute `Practical objective` and `Practical steps`
advanced rows from their existing preconditions and execution steps. These rows
are surface guidance only and do not create plan promotion. Practical advanced
rows are suppressed when a promoted plan already covers the same top-level
theme or at least 70% of the practical plan's execution steps, so sibling plans
do not repeat the same guidance under a softer label.
MoveReview may also add one `Practical pressure` advanced row from an existing
`StrategyPack` target-fixing idea only when its typed evidence refs include
`source:carlsbad_fixation_profile` plus a `target_pressure_semantic` /
`target_pressure_*` fact and pack-side owner match, or
`source:minority_attack_semantic` plus a
`target_pressure_semantic` / `target_pressure_*` fact and pack-side owner match, or
`source:minority_attack_support` plus a `minority_attack_support_<flank>` fact
and a valid structural target square, or a current-board
weakness source with its matching fact (`source:enemy_weak_square` plus
`enemy_weak_square_<square>`, or `source:weak_complex_fixation` plus a
`weak_complex_*` fact such as `weak_complex_isolated_pawn` or
`weak_complex_backward_pawn` and a valid focus square), or
`source:doubled_pawn_pressure_motif` plus `doubled_pawn_pressure_shape`,
`doubled_pawn_file_<file>`, a valid focus file, Build readiness, pack-side
owner match, and typed confidence, or compensation target pressure with
`source:compensation_target_fixation`, `compensation_target_fixation`,
`material_deficit_compensation`, a valid focus square, pack-side owner match,
and typed confidence. The
weakness-source branch requires pack-side owner match. Its named target list
excludes squares already eligible for the lower-authority `Practical target`
row on the same target square. Exact `Fixed target` / `Minority attack` rows
with builder-owned exact wording and practical-plan target metadata remove the
same target square from every target-naming pressure branch. A practical target
plan or exact target row on a different square does not close this pressure
context; the row may still describe the remaining typed focus squares. This
square-target filtering is not applied to the doubled-pawn pressure branch,
whose witness and text are file-scoped (`doubled_pawn_file_<file>` plus a valid
focus file), with emitted file text taken from that matched file fact. This is support-only board-structural
pressure context with `PracticalPlan` authority and no target metadata; generic plan-match
target-fixing ideas, color-complex-only weakness tags, directional target
access without board weakness corroboration, source-only weak-square tags,
source-only/no-square weak-complex motif tags, source-only/no-target minority
support, source-only/shape-only/no-file doubled-pawn pressure tags, and
source-only/no-fact compensation target fixation do not open it.
When several selector evidence carriers merge into one strategic idea, surface
projection preserves enough selected source, shape, and witness facts for these
bounded support-only gates to evaluate them without upgrading them to exact
authority.
The structural-idea surface may keep at most three distinct practical-row
families for one move. This preserves independent board-structural teaching
cues, such as line and trade cues behind a pressure cue, but it still emits at
most one row per practical label and does not publish owner, target, token, or
proof metadata.
For color complexes, this row is structure-context grammar: an already visible
exact `Color complex` row with builder-owned bishop/knight wording,
`PracticalPlan` target authority, and the same dark/light complex suppresses
the lower-authority `Practical space` context row; stale labels, non-minor-piece
prose, or targetless approximate wording do not suppress it.
MoveReview may similarly add one `Practical space` advanced row from an
existing `StrategyPack` space/restriction idea only when the typed evidence refs
carry one of the narrow structure-backed profiles: `source:color_complex_clamp`
plus `enemy_color_complex_weakness` and a specific dark/light color-complex
token with at least two valid focus squares and pack-side owner match; the
single dark/light token names the complex and overrides stale `focusZone` text;
when no single token is available, the color-complex clamp row stays closed. `source:maroczy_bind_profile` plus
`structure_maroczy_bind` for White with pack-side owner match; or
`source:iqp_central_presence` plus `structure_iqp_white`,
`iqp_central_presence_shape`, and focus square `d4` for White with pack-side owner match;
or `source:iqp_space_bridge` plus `structure_iqp_white` for White, center focus,
`Build` readiness, pack-side owner match, and the producer confidence floor;
or `source:locked_center_bind` plus `structure_locked_center`, center focus,
pack-side owner match, and typed locked-center confidence. The locked-center
source is emitted only after the producer sees a locked center with same-side
space edge or color-complex clamp support. It may also use typed central-space
edge support only when refs carry `source:central_space_edge` and
`central_space_edge_shape`, with `Ready` readiness, center focus, pack-side owner
match, and the producer confidence floor. It may also use current-board
space-advantage motif support only when refs carry `source:space_advantage_motif`,
`space_advantage_motif_shape`, and a `space_pawn_delta_<n>` fact with `n >= 2`,
with `Ready` readiness, center focus, pack-side owner match, and motif confidence.
It may also use current-board central pawn-advance motif support only when refs
carry `source:central_pawn_advance_motif`,
`central_pawn_advance_shape`, a matching central `central_pawn_file_<file>` fact,
and `central_pawn_to_rank_<n>` with `n >= 4`, with center focus, `Build`
readiness, pack-side owner match, and the motif confidence floor; the emitted
file token is the matched central file fact, not merely the first focus file.
It may also use current-board pawn-chain motif support only when refs carry
`source:pawn_chain_space_motif`, `pawn_chain_space_shape`, and a
`pawn_chain_<base>_<tip>` fact, with at least two valid focus files, `Build`
readiness, pack-side owner match, and the pawn-chain confidence floor. The row
names kingside/queenside space only when the pawn-chain facts resolve to one
flank zone; mixed-flank or center-only chain facts stay flank-space-neutral.
It may also use typed aggregate
mobility-restriction support only when refs carry `source:mobility_restriction`
and `mobility_restriction_shape`, center focus, pack-side owner match, and confidence
at least `0.72`, which corresponds to at least a two-piece low-mobility gap in
the current board-derived producer. This is support-only structure context with
`PracticalPlan` authority and no target metadata; source-only central-space,
source-only or one-piece mobility restriction, source-only or weak-delta
space-advantage motif support, source-only or non-central-file central pawn
advance motif support, source-only or shape-only pawn-chain motif
support, pawn-chain support without two valid files, generic
locked-center, source-only IQP space bridge, or plan-match space carriers do not open it.
MoveReview may also add one `Practical break` advanced row from an existing
`StrategyPack` pawn-break idea only when typed evidence refs carry
`source:pawn_analysis_break_ready` or `source:pawn_play_break_ready`, the matching
`pawn_analysis_break_ready_shape` or `pawn_play_break_ready_shape` fact,
`Ready` readiness, a valid focus file, and high confidence; when a matching
`break_file_<file>` fact is present, the emitted break file uses that fact.
It may surface a file-opening break consequence only when refs carry
`source:file_opening_consequence` and a `contested_file_<file>` fact matching
the focus file, with owner/pack side agreement and typed confidence; the
emitted opening file is the matched contested-file fact.
It may surface central-tension break support only when refs carry
`source:central_break_tension`, a valid central focus file, center focus,
pack-side owner match, typed confidence, and either `locked_center` or `Ready`
readiness; because this source carries no file-specific fact, the row names a
break file only when the merged idea has exactly one valid central focus file,
otherwise it stays central-break-cue-neutral.
It may also surface current pawn-break motif support only when refs carry
`source:pawn_break_motif`, `pawn_break_motif_shape`, and a `break_file_<file>`
fact matching the focus file, with `Build` readiness, pack-side owner match, and
the motif confidence floor; the emitted break file is the matched file fact.
It may also surface the French Advance ...f6 seed only when refs carry
`source:french_f6_break_seed`, `french_f6_break_seed_shape`, `white_e5_chain`, and
`black_f7_break_pawn`, with Black owner, f-file focus, e5/f6 focus squares, and
high confidence. This is a support-only current-structure break cue with
`PracticalPlan` authority and no route/token metadata; plan-match break
preparation, counter-break race, source-only central-break tension,
source-only/shape-only pawn-break motif carriers,
non-central-file tension, and broad French profile carriers do not open this row
or satisfy `CentralBreakTiming`.
MoveReview may also add one `Practical restraint` advanced row from an existing
`StrategyPack` counterplay-suppression idea only when typed evidence refs carry
board-geometry support for Hedgehog break denial (`source:hedgehog_break_denial_geometry`,
`structure_hedgehog`, `hedgehog_break_denial_shape`, White owner, b/d file
focus, queenside zone, high confidence) or Maroczy break denial
(`source:maroczy_break_denial_geometry`, `structure_maroczy_bind`,
`maroczy_break_denial_shape`, White owner, c/d file focus, center zone, high
confidence). This is support-only current-structure restraint context with
`PracticalPlan` authority and no route/token metadata. Duplicate suppression is
subtype-specific: exact `Counterplay break`, `Counterplay restraint`, `Break and
entry`, or `Route denial` rows close break/counterplay-denial practical context,
but do not close passer-blockade practical context. For this duplicate
suppression, exact means the builder-owned generated wording plus the
corresponding public authority: `CounterplayBreak` token authority for
`Counterplay break`, or `PracticalPlan` authority for `Counterplay restraint`,
`Break and entry`, and `Route denial`. Stale labels or approximate route-denial
prose do not close the matching lower-authority row. It may also use an
opponent-counterbreak denial bridge only when refs carry
`source:opponent_counterbreak_denial`, the surviving `opponent_counter_break`
fact, a concrete focus file, `Ready` readiness, pack-side owner match, and the
producer confidence floor. It may also use current-board counterplay-suppression
support only when refs carry `source:counterplay_suppression`,
`counterplay_suppression_shape`, the single-evidence `counterplay_break_denial` marker,
`break_neutralized`, and `denied_break_resource`, with a concrete focus file,
`Ready` readiness, pack-side owner match, and the producer confidence floor. It
may also use current-board passer-blockade motif support only when refs carry
`source:passer_blockade_motif`, `passer_blockade_shape`,
`blockade_square_<square>`, and `blockaded_pawn_<square>`, with `Ready`
readiness, pack-side owner match, and the motif confidence floor. This remains a
lower-authority practical brake on the named passed pawn; it does not satisfy the
exact `Passer blockade` top-PV/public row. It may also use compensation
counterplay-denial support only when refs carry
`source:compensation_counterplay_denial`, `material_deficit_compensation`,
and `break_neutralized`, with a concrete focus file, pack-side owner match, and
the compensation producer confidence floor. These bridge rows name a break file
only when the merged idea has exactly one valid focus file; multi-file focus
stays break-cue-neutral. Broad Hedgehog containment, broad
Maroczy counterplay suppression,
source-only opponent-counterbreak rows, source-only/generic counterplay-suppression
rows, source-only/shape-only/no-pawn passer-blockade motif rows,
source-only/generic compensation counterplay-denial rows, compensation
denied-square-only rows, merged-ref counterplay-suppression rows,
counterplay-score-drop-only rows, and plan-match suppression carriers do not open
this row or satisfy
`CounterplayBreak` / `CounterplayRestraint` proof authority.
MoveReview may also add one `Practical line` advanced row from an existing
`StrategyPack` line-occupation idea only when typed evidence refs carry current
occupied major-piece line support (`source:occupied_line_control`, an
`occupied_r_<square>` or `occupied_q_<square>` fact matching a focus square, and
an open/semi-open file fact matching the focus file; queen support may also use
`occupied_seventh_rank`) or doubled-rook file support
(`source:doubled_rooks` with a `doubled_rooks_<file>` fact matching a focus
file, including current `Motif.DoubledPieces` carriers when the role is rook)
or seventh-rank rook support, including seventh-rank invasion motif
carriers, (`source:rook_on_seventh` with the `rook_on_seventh_shape` fact and a
rook beneficiary) or current open-file major-piece support, including
open-file motif carriers (`source:open_file_control` with an `open_file_<file>`
fact matching a focus file and a rook/queen beneficiary), or current
semi-open-file motif support (`source:semi_open_file_control` with a
`semi_open_file_<file>` fact matching a focus file and a rook/queen beneficiary)
or connected-rooks coordination support
(`source:connected_rooks` with the `connected_rooks_shape` fact and a rook
beneficiary), or directional rook-line support (`source:directional_line_access`
with `directional_line_access_shape`, a valid focus square, and an open/semi-open file
fact matching a focus file), or exact-route line-control support
(`source:line_control_features` with `line_control_shape`,
`source:route_line_access`, `route_surface_exact`, a valid focus square, and an
open/semi-open file fact matching a focus file), or compensation line support (`source:compensation_open_lines`
with `compensation_open_lines_shape`, `material_deficit_compensation`, and an
open/semi-open file fact matching a focus file), or delayed-recovery line
support (`source:delayed_recovery_window` with `delayed_material_recovery`,
`development_lead_compensation`, `material_deficit_compensation`, and an
open/semi-open file fact matching a focus file). Broad route, directional, compensation,
and delayed-recovery line rows name a file only when the merged idea has exactly
one open/semi-open file fact; multi-file focus stays line-access-neutral. The
idea must name the matching rook or queen beneficiary, match
the pack side, and carry typed confidence; queen and doubled-rook support must
also be `Ready`, seventh-rank support must be `Ready` with confidence at least
`0.72`, open-file support must be `Ready` with confidence at least `0.80`,
semi-open-file motif support must be `Ready` with confidence at least `0.78`,
while connected-rook support may be `Build` or `Ready`,
directional rook-line support must be `Build` with a rook
beneficiary and confidence at least `0.50`, exact-route line-control support
must be `Ready` with a rook beneficiary and confidence at least `0.60`,
compensation line support must be `Build` with confidence at
least `0.70`, and delayed-recovery support must be `Build` with confidence at
least `0.74`. Duplicate suppression is subtype-specific and file-specific:
exact `File entry` closes file/post/open-file line context only for the same
file named by the practical row, exact `Seventh-rank entry` closes
rook seventh-rank context only for the exact row's generated rook wording,
exact `Connected rooks` closes connected-rooks context,
and exact `Doubled rooks` closes doubled-rooks context only for the same file
named by the practical row. A different exact line subtype, exact `File entry`
on a different file, exact `Doubled rooks` on a different file, or exact rook
seventh-rank entry beside a queen seventh-rank cue does not close unrelated
practical line context. Duplicate suppression requires the current
builder-owned exact wording and public authority: practical-plan target metadata
plus the same generated `through <file>-file` wording for `File entry`, where
the target square is on that named file, or `PracticalPlan` authority plus the
generated top-PV wording for
`Seventh-rank entry`, `Connected rooks`, and `Doubled rooks`; seventh-rank
suppression is role-aware. Exact-looking
stale labels with lower-authority line prose are not enough to close this
context row. This is
support-only current-board line
context with `PracticalPlan` authority and no target/token metadata; source-only
`open_file_control`, open-file support without a major-piece beneficiary,
source-only/no-file semi-open-file tags, semi-open-file tags without a
major-piece beneficiary,
source-only occupied-line, source-only doubled-rook,
non-rook doubled-piece motif carriers, source-only seventh-rank,
source-only connected-rook, connected-rook without a
rook beneficiary, source-only/no-route line-control, non-exact route line-access,
source-only, no-file, queen, or
low-confidence directional line-access,
source-only or no-file compensation open-line/delayed-recovery carriers,
plan-match line carriers, and
source-only open-file tags do not open this row or satisfy `local_file_entry_bind`
or `half_open_file_pressure` proof authority.
MoveReview may also add one `Practical outpost` advanced row from an existing
`StrategyPack` outpost idea only when typed evidence refs carry
`source:outpost_tag` plus an `outpost_<square>` fact matching a valid focus
square, including current minor-piece `Motif.Outpost` carriers, or occupied
strong-knight support with `source:strong_knight` plus a
`strong_knight_<square>` fact matching a valid focus square and a knight
beneficiary, or exact route-outpost support with `source:route_outpost_access`,
`route_outpost_access_shape`, and `route_surface_exact` aimed at a valid focus square
by a minor piece; the outpost square is named only when the merged idea carries
a single focus square, otherwise the practical row stays outpost-cue-neutral; or
directional outpost support with
`source:directional_outpost_access`, `directional_outpost_access_shape`, and a valid
focus square aimed at by a minor piece; the outpost square is named only when
the merged idea carries a single focus square, otherwise the practical row stays
outpost-cue-neutral. The idea must match the pack side and
carry typed confidence for the underlying outpost/strong-knight/route/directional
bridge; tag, strong-knight, and exact-route support must be `Ready`, while
directional outpost support must be `Build` with confidence at least `0.64`.
The row is suppressed only when an already visible exact `Opening outpost` /
`Knight outpost` row certifies the same outpost square the practical row would
name; merged multi-square route/directional outpost support stays open only when
at least one focus square is not already exact-certified. For `Knight outpost`,
duplicate suppression requires the current exact
builder `knight` wording and same-square practical-plan target metadata; an
exact-looking stale label with lower-authority outpost prose is not enough to
close this context row. For `Opening outpost`, duplicate suppression likewise
requires the opening-goal builder wording with `PracticalPlan` authority and
the same extracted outpost square; a stale, lower-authority, or different-square
`Opening outpost` label does not close this context row. This is
support-only current-board outpost context with `PracticalPlan` authority and no
target/token metadata; unoccupied/build strong-knight, entrenched-piece,
non-exact route outpost access, source-only, non-minor, non-`Build`, or
low-confidence directional outpost access, opening-goal
outpost, and plan-match outpost carriers do not open this row or satisfy exact
`OutpostOccupation` authority.
MoveReview may also add one `Practical trade` advanced row from an existing
`StrategyPack` favorable-trade/transformation idea only when typed evidence refs
carry `source:iqp_simplification_profile`, `structure_iqp_black`, and either
`capture_or_exchange` or `iqp_trade_down_plan`, with White owner, pack-side owner match, and high typed
confidence. If an exact `Simplification` row is already visible, this row stays
closed. The duplicate gate requires the current exact row wording plus
same-square practical-plan target metadata; an exact-looking stale
`Simplification` label with lower-authority exchange prose is not enough to
close this context row. The exchange square is named only when the idea carries
a single focus square; multi-target exchange support stays trade-cue-neutral
because no square-specific fact is published for this branch. It may also use IQP exchange-availability support only when refs carry
`source:exchange_availability_bridge` and `structure_iqp_black`, with White
owner, `Build` readiness, pack-side owner match, and the exchange-availability
confidence floor. This is support-only IQP trade context with `PracticalPlan`
authority and no target/token metadata; classification windows, generic
capture/exchange, source-only exchange-availability bridges, plan-match
transformation, soft transformation plan support, and non-IQP transformation
carriers do not open this row or satisfy exact `SimplificationWindow` authority.
MoveReview may also add one `Practical conversion` advanced row from an existing
`StrategyPack` favorable-trade/transformation idea only when typed evidence refs
carry both `source:winning_endgame_transition` and `winning_endgame_transition_shape`,
the idea is `Ready`, the owner matches the pack side, confidence reaches the
producer's winning-endgame threshold, and no exact `Technical conversion` row is
already visible; because the producer may publish up to three key-square focus
cues under one shape fact, this row names a square only when the merged idea
has exactly one valid focus square, otherwise it stays conversion-cue-neutral.
It may instead use current rook-endgame motif support when refs carry
`source:rook_endgame_pattern`, `rook_endgame_pattern_shape`, and either
`rook_behind_passed_pawn` or `king_cut_off`, with `Build` readiness, pack-side
owner match, confidence at least `0.72`, and no exact `Technical conversion`
row already visible; rook-endgame wording names a specific pattern only when
exactly one rook-endgame pattern fact survives, otherwise the row stays
rook-endgame-map neutral, or when refs carry current endgame-technique motif support
with `source:endgame_technique_motif`, `endgame_technique_shape`, an
`opposition_direct`, `opposition_distant`, `opposition_diagonal`, or
`zugzwang_shape` fact, or endgame-phase `Motif.KingStep.Activation` support with
`king_activity_shape`, endgame focus, `Build` readiness, pack-side owner match,
confidence at least `0.72`, and no exact `Technical conversion` row already
visible; endgame-technique wording names a specific technique only when exactly
one technique fact survives, otherwise the row stays endgame-technique-map
neutral, or when refs
carry current passed-pawn motif support with
`source:passed_pawn_conversion_motif`, `passed_pawn_conversion_shape`, and a
`passed_pawn_<square>` fact matching a valid focus square, including
`Motif.PassedPawnPush` carriers that also keep `passed_pawn_push` and advanced
rank support when applicable, and `Motif.PawnPromotion` carriers that also keep
`pawn_promotion`, `promotion_piece_<role>`, and optional `underpromotion`
support, with `Build` readiness, pack-side owner match, confidence at least
`0.72`, and no exact `Technical conversion` row already visible; when the row
names a passed-pawn or promotion square, the emitted square is the matching
`passed_pawn_<square>` fact, not merely the first focus square. This is
support-only endgame or passer-structure context with `PracticalPlan` authority
and no target/token metadata. Exact `Technical conversion` suppression means the
restricted-defense conversion row's builder-owned wording with `PracticalPlan`
authority; a stale or lower-authority `Technical conversion` label does not
close practical conversion context. Classification-only
transformation windows, source-only or shape-only rook-endgame pattern carriers,
source-only or shape-only endgame-technique motif carriers, endgame-technique
motif carriers without opposition, zugzwang, or endgame king-activity facts,
source-only or shape-only passed-pawn motif carriers, passed-pawn carriers
without a matching square fact, soft plan matches, and generic exchange carriers
do not open this row or satisfy restricted-defense `Technical conversion`
authority.
MoveReview may also add one `Practical minor` advanced row from an existing
`StrategyPack` minor-piece idea when typed evidence refs carry a concrete
minor-piece bridge: `source:strong_knight_vs_bad_bishop` plus a
`strong_knight_<square>` fact matching a focus square, or
`source:piece_activity_bad_bishop` plus an `enemy_bad_bishop_<square>` fact
matching a focus square, current-board enemy-bad-bishop support with
`source:enemy_bad_bishop` plus the `enemy_bad_bishop_shape` fact, current-board bishop-pair support with
`source:bishop_pair_advantage` plus the `bishop_pair_advantage_shape` fact,
current-board opposite-colored-bishop support with
`source:opposite_color_bishops` plus the `opposite_color_bishops_shape` fact, or a
current-board good-bishop count edge carrying `source:good_bishop`,
`good_bishop_shape`, `source:minor_piece_count_imbalance`,
`minor_piece_count_imbalance_shape`, and `good_bishop_count_edge`, or current
minor-piece centralization motif support with `source:piece_centralization_motif`,
`piece_centralization_shape`, and a `centralized_piece_<square>` fact matching a
focus square, current minor-piece maneuver support with
`source:piece_maneuver_motif`, `piece_maneuver_shape`, and a minor beneficiary,
or current knight-vs-bishop motif support with
`source:knight_vs_bishop_motif`, `knight_vs_bishop_motif_shape`, and
`knight_preferred_over_bishop`. French Advance profile refs
`source:french_minor_piece_profile` and `structure_french_advance_chain` keep
the existing French-specific wording, while non-French bridge rows use current
minor-piece-map wording. Centralization rows name a concrete role/square pair
only when the merged idea has exactly one minor beneficiary and exactly one
centralized-piece fact; merged multi-piece or multi-square centralization stays
minor-piece-neutral. The idea must name the relevant minor beneficiary,
match the pack side, and carry typed confidence for the underlying bridge;
non-French strong-knight and bishop-pair rows must also be `Ready`; bishop-pair
support closes when an exact `Bishop pair` row is already visible, and
opposite-colored-bishop support closes when an exact `Opposite-color bishops`
row is already visible. For those bishop rows, exact means the builder-owned
capture wording with `PracticalPlan` authority; a stale or lower-authority
`Bishop pair`/`Opposite-color bishops` label does not close the practical
minor-piece context. This is support-only structural minor-piece context with
`PracticalPlan` authority and no target/token metadata; French profile alone,
source-only bishop-pair advantage,
source-only good bishop, count imbalance without
the good-bishop count-edge pair, source-only opposite-color bishop carriers, and
source-only or no-square/no-minor centralization motif carriers,
source-only/no-minor maneuver motif carriers, source-only/shape-only
knight-vs-bishop motif carriers, and plan-match
minor-piece carriers do not open this row or satisfy exact outpost,
bishop-pair, opposite-color-bishops, or supported-local authority.
MoveReview may also add one `Practical prophylaxis` advanced row from an
existing `StrategyPack` prophylaxis idea only when typed evidence refs carry a
board-pattern prophylaxis source and matching focus geometry:
`source:bishop_pin_watch` with a valid `g4`/`g5` focus square, or
`source:queenside_counterbreak_watch` with a `b`-file focus. The idea owner
must match the pack side, carry the producer confidence for that board pattern,
and no promoted exact/probe-backed `Prophylaxis` plan row may already be
visible. This is support-only current-board prophylaxis context with
`PracticalPlan` authority and no target/token metadata; plan-match
prophylaxis, threat-analysis prophylaxis, prevented-plan, counterbreak-watch,
raw prophylaxis plans, and probe-backed prophylaxis rows do not open this row
or satisfy exact prophylaxis-restraint authority.
MoveReview may also add one `Practical attack` advanced row from an existing
`StrategyPack` king-attack idea only when typed evidence refs jointly carry
`source:fianchetto_assault_profile`, `source:opposite_side_storm`, and
`structure_fianchetto_shell`, with kingside focus, `Build` readiness,
pack-side owner match, and typed fianchetto-assault confidence; or when refs
carry current king-ring pressure with `source:king_ring_pressure` plus the
`king_ring_pressure_shape` fact, a concrete enemy-king focus zone, `Build` readiness,
pack-side owner match, and the producer confidence floor; or when refs carry
enemy king central-exposure support with `source:enemy_king_stuck_center` plus
`enemy_king_central_exposure`, a concrete enemy-king focus zone, `Build` readiness,
pack-side owner match, and the producer confidence floor; or when refs carry
enemy weak-back-rank support, including weak-back-rank motif carriers, with
`source:enemy_weak_back_rank` plus `enemy_weak_back_rank_shape`, a concrete
enemy-king focus zone, `Build` readiness, pack-side owner match, and the
producer confidence floor; or when refs carry
current flank-pawn hook pressure with `source:flank_pawn_pressure` plus
`hook_creation_chance`, a concrete enemy-king focus zone, `Build` readiness,
pack-side owner match, and the producer confidence floor, unless an exact
`Hook creation` or `Rook-pawn march` row is already visible; or when refs carry
flank pawn-advance motif support with `source:flank_pawn_advance_motif`,
`flank_pawn_advance_shape`, a matching `flank_pawn_file_*` fact on the
a/b/g/h files, a `flank_pawn_to_rank_*` fact at relative rank four or beyond,
`Build` readiness, pack-side owner match, and the motif confidence floor,
unless an exact `Hook creation` or `Rook-pawn march`
row is already visible. Pawn-file wording is used only when exactly one
flank-pawn file fact survives; kingside/queenside wording is derived only from
the surviving flank-pawn file facts, and mixed-flank facts stay attack-cue-neutral;
or compensation diagonal-battery support when refs jointly carry
`source:compensation_diagonal_battery`,
`compensation_diagonal_battery`, `material_deficit_compensation`, B/Q
beneficiary pieces, king-side/center attack focus, pack-side owner match, and
the compensation-battery confidence floor; or when refs carry
`source:compensation_development_lead`, `development_lead_compensation`, and
`material_deficit_compensation`, a concrete enemy-king focus zone, `Build`
readiness, pack-side owner match, and the producer confidence floor; or when
refs carry `source:compensation_king_window`,
`uncastled_or_unsettled_king_window`, and `material_deficit_compensation`, a
concrete enemy-king focus zone, `Build` readiness, pack-side owner match, and
the producer confidence floor; or
when refs carry exact route-attack support with `source:route_attack_lane`,
`route_attack_lane_shape`, a concrete enemy-king focus square and zone, a
beneficiary piece, `Ready` readiness, pack-side owner match, and the
route-attack confidence floor; the route square is named only when the merged
idea carries a single focus square, otherwise the practical row stays
route-neutral; or when refs carry directional attack-lane
support with `source:directional_attack_lane`,
`directional_attack_lane_shape`, a concrete enemy-king focus square and zone, a
beneficiary piece, `Build` readiness, pack-side owner match, and the
directional producer confidence floor; the target square is named only when the
merged idea carries a single focus square, otherwise the practical row stays
target-map-neutral; or when refs carry current motif-battery
support with `source:motif_battery`, a diagonal/file battery-axis fact, two
concrete battery squares, two beneficiary pieces, a concrete enemy-king focus
zone, `Build` readiness, pack-side owner match, and the motif-battery confidence
floor, unless an exact `Battery pressure` row is already visible; battery-axis
wording is used only when exactly one battery-axis fact survives, otherwise the
row stays battery-neutral; or when refs
carry current rook-lift motif support with `source:motif_rook_lift`, a concrete
focus file, rook beneficiary, concrete enemy-king focus zone, `Build` readiness,
pack-side owner match, and the motif-rook-lift confidence floor, unless an exact
`Rook lift` row is already visible; because this motif source carries no
file-specific fact, the row names a file only when the merged idea has exactly
one valid focus file, otherwise it stays rook-lift-cue-neutral; or when refs
carry current piece-lift motif
support with `source:motif_piece_lift` plus `motif_piece_lift_shape`, a
beneficiary piece, concrete enemy-king focus zone, `Build` readiness, pack-side
owner match, and the motif-piece-lift confidence floor. Because this motif source
carries no role-specific fact, the row names a lifted piece only when the merged
idea has exactly one beneficiary piece, otherwise it stays piece-lift-neutral; or when refs carry
current normal/discovered check-motif support with `source:motif_check_pressure`,
a `check_type_normal` or `check_type_discovered` fact, a concrete enemy-king
focus square and zone, a beneficiary piece, `Ready` readiness, pack-side owner
match, and the motif-check confidence floor; the checked square is named only
when the merged idea carries a single focus square, and the checking piece is
named only when the merged idea has exactly one beneficiary piece, otherwise the
practical row stays check-motif-neutral; or when refs carry current
fianchetto motif support with `source:fianchetto_motif`,
`fianchetto_motif_shape`, a `fianchetto_side_kingside` or
`fianchetto_side_queenside` fact, a concrete focus zone, bishop beneficiary,
`Build` readiness, pack-side owner match, and the motif confidence floor; or
when refs carry current initiative motif support with `source:initiative_motif`,
`initiative_motif_shape`, a concrete enemy-king focus zone, `Build` readiness,
pack-side owner match, the motif confidence floor, and an `initiative_score_*`
fact at ten or above. Duplicate suppression is subtype-specific: exact
back-rank mate, mate-net, or Greek-gift rows close generic attack context,
exact `Hook creation` / `Rook-pawn march` rows close flank-hook and flank-pawn
advance context, exact `Battery pressure` closes battery context, and exact
`Rook lift` closes rook-lift context. A different exact attack subtype does not
close unrelated practical attack context. This is support-only current-structure
attack context with `PracticalPlan` authority and no target/token metadata; mate-net,
source-only king-ring pressure, source-only enemy king central exposure,
source-only weak back rank, source-only flank-pawn pressure, rook-pawn-ready-only
flank pressure, source-only/flank-shape-only flank pawn-advance motif carriers,
non-flank pawn-advance motif carriers, attacking-threat analysis, motif attack lanes,
source-only or non-exact route-attack lanes, source-only directional attack lanes,
source-only compensation development, source-only compensation king-window,
source-only or axis-only/no-square motif battery, source-only or no-file/no-rook
motif rook lift, source-only or no-zone/no-piece motif piece lift, source-only
or no-square/no-zone/no-piece motif check pressure, mate/double/smothered check
motifs, source-only/no-bishop/mirrored fianchetto motif carriers,
source-only/no-zone/low-score initiative motif carriers, plan-match
king-attack carriers, and opposite-side storm alone do not open this row or
satisfy exact tactical authority.
For duplicate suppression, exact attack/flank/battery/rook-lift rows mean the
builder-owned generated wording plus `PracticalPlan` authority for
`Back-rank mate`, `Mate net`, `Greek gift`, `Hook creation`, `Rook-pawn march`,
`Battery pressure`, or `Rook lift`. A stale label, approximate prose, or row
without that authority does not close the matching lower-authority
`Practical attack` construction.
Weakness-fixation practical plans, and bounded practical plans carrying an
exact square target hint, may also add a `Practical target` advanced row. The
target comes from `analysis.structure.WeaknessTargetProfile`, a canonical
board-backed profile that reuses `PositionAnalyzer` pawn primitives for
backward, isolated, IQP, doubled, and fixed pawn targets. If the plan carries
`weakness_target:<square>`, `fixed_target:<square>`,
`coordinated_target:<square>`, exact `target:<square>`, `target_fixing:<square>`,
`enemy_weak_square:<square>`, or `weak_complex:<square>` evidence, the row may
use only that square after it is present in either the current board profile or
the checked best-line endpoint profile. Role targets such as `target:e5:queen`
are not weakness-target hints. Untargeted weakness-fixation practical plans may
use only the current profile fallback. This row is still bounded practical
guidance with `practical_plan` authority; it does not expose `authority.target`
and does not promote the plan. Current-board target context may publish without
a best-line persistence witness, and may prefix the sentence with the profile's
typed non-generic structure context when available. Generic center classifications
such as Open/Locked/Fluid/Symmetric are not used as this prefix; concrete
structure names remain educational context, not motif admission or target
authority. The row is suppressed when the available best-line outcome shows
that the defending side liquidates the target.
Capturing the target by the pressure side is treated as a resolved target, not
as a ghost-target veto. New endpoint-only targets still require an exact square
target hint plus a trusted endpoint or at least five legal UCI plies, because
those rows say the checked line leaves a target that was not already present in
the root structure.
Resolved compensation surfaces may also contribute bounded `Compensation`
advanced rows. `MoveReviewPlayerPayloadBuilder` consumes the existing
`StrategyPackSurface` and `CompensationDisplayPhrasing` boundary only after the
surface has a strict resolved compensation subtype, a non-fallback compensation
display contract, a durable-pressure strict subtype, invested material or
equivalent compensation position, and the strong-anchor narration eligibility
check. If a `DecisiveTruthContract` is present and
`compensationProseAllowed=false`, this row gate remains closed even if a raw
strategy pack still carries a resolved compensation surface. The row uses
`authority.kind = "practical_plan"` and exposes at most two player-facing
sentences: the material-investment reason and the durable pressure anchor. The
second row prefers the resolved persistence wording over the broader objective
fallback so the bounded public support states what must actually remain.
Generic initiative shells, unresolved subtype paths, transition-only conversion
stories, non-durable tactical windows, and unanchored compensation prose produce
no player-surface compensation row.
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

Counterplay-restraint packets have a narrow MoveReview surface consumer.
`MoveReviewSupportedLocalSurfaceRows` may project `prophylactic_move` /
`counterplay_restraint` packets as a bounded `Counterplay restraint` summary
row only after `ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision`
admits the packet and the typed
`PlayerFacingExactSliceProof.ProphylacticRestraint` matches the packet
source/family and the same named resource token is mirrored in packet witness
terms. The row repeats only the named prevented resource, such as a square token
or `denied_resource:<class>`, and arbitrary prophylaxis prose cannot be parsed
into proof.

Named route-network bind has a typed MoveReview surface consumer, not a broad
heavy-piece/local-bind release. `QuestionPlannerInputsBuilder` may carry
`RouteNetworkBindProof.SurfaceNetwork` from the certified route-network proof,
and `MoveReviewSupportedLocalSurfaceRows` may project it as one bounded
`Route denial` summary row only after
`ClaimAuthorityResolver.namedRouteNetworkSurfaceDecision` admits it. The public
row is limited to the simple file, entry-square, and reroute-square triple.
Route-chain surfaces with an intermediate square stay backend-only, and
heavy-piece/local-bind blocks or tactical truth modes suppress the row.

Dual-axis bind has a typed MoveReview surface consumer, not a broad
local-bind/global-squeeze release. `NarrativeContextBuilder` preserves the
highest-confidence certified `TwoAxisBindProof.Contract` from the existing
plan-experiment certification pass, and `QuestionPlannerInputsBuilder` carries
that contract as `dualAxisBindSurface`. `MoveReviewSupportedLocalSurfaceRows`
may project one bounded `Break and entry` row only after
`ClaimAuthorityResolver.dualAxisBindSurfaceDecision` admits a certified
`dual_axis_local` / `break_plus_entry` contract with `bounded_dual_axis_only`,
an explicit break axis, an explicit independent entry axis, no
heavy-piece/local-bind block, and no tactical veto. The public row may name only
those two axes; it does not claim global no-counterplay or whole-position bind
authority.

The current FEN-backed public surface witness is the queenless late-middlegame
fixture in `TwoAxisBindProofTest`: `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24`.
The certified contract uses direct best defense `f8e8`, branch key
`f8e8`, future-snapshot persistence, and bounded continuation visibility
before `MoveReviewSupportedLocalSurfaceRows` emits the `Break and entry` row.

Restricted-defense conversion has a typed MoveReview surface consumer for
technical conversion support, not a general winning-plan proof.
`NarrativeContextBuilder` preserves the highest-confidence certified
`RestrictedDefenseConversionProof.Contract` from the existing plan-experiment
certification pass, and `QuestionPlannerInputsBuilder` carries that contract as
`restrictedDefenseConversionSurface`. `MoveReviewSupportedLocalSurfaceRows` may
project one bounded `Technical conversion` row only after
`ClaimAuthorityResolver.restrictedDefenseConversionSurfaceDecision` admits a
certified contract with a direct best defense, same-branch persistence,
compressed defender resources, continuing counterplay suppression, and no
tactical veto. The row may name only the checked best-defense reply when it can
be legally rendered from the reviewed move; otherwise it stays generic.
Winning-endgame strategy evidence can support only the lower-authority
`Practical conversion` row above; it does not bypass the restricted-defense
proof contract required for `Technical conversion`.

IQP inducement has a lower-authority MoveReview surface consumer. The producer
may create an `iqp_inducement_probe` / `iqp_inducement` packet only when legal
FEN/PV replay shows that the reviewed move or checked prefix creates a new
opponent isolated central pawn, and the packet carries
`PlayerFacingExactSliceProof.IqpInducement(targetSquare, lineMoves)` mirrored by
both `after_isolated:<square>` and `isolated_pawn:<square>`, the
`central_isolated_pawn` marker, and the checked line moves.
`MoveReviewSupportedLocalSurfaceRows` may project that admitted packet as one
bounded `IQP target` summary row through
`ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision`. The row keeps
`MoveReviewSurfaceAuthority.PracticalPlan` authority but may carry only the
typed proof's target square in `target`; generic IQP labels, source names, or
target-square text without the induced-pawn transition term and typed
exact-slice proof do not create the row.
The current public source examples include
`source-karpov-andersson-1975-iqp-inducement`: ply 49 `cxd5` is a
`SupportedLocal` source row only after exact replay, top-PV identity, the
`iqp_inducement_probe` packet source/family, the induced `isolated_pawn:d5`
continuation, and the `subplan:iqp_inducement` contract all agree.

Bounded simplification-window support uses the same move-delta authority path.
A `simplification_window` packet may reach the public `Simplification` row only
after the existing proof contract has owner seed, continuation, proven branch,
stable persistence, structure transition, and no rival/tactical release risk.
`MoveReviewSupportedLocalSurfaceRows` additionally requires a matching
`PlayerFacingExactSliceProof.SimplificationWindow(exchangeSquare)` and an
`exchange_square:<square>` witness term from the checked continuation before it
can name the exchange. Broad favorable-exchange prose, a naked square anchor,
or a conversion label such as "same local edge" does not create the row.
The current public source example is
`source-botvinnik-vidmar-1936-simplification-window`: ply 31 `Nxd5` is a
`SupportedLocal` source row only after exact replay, near-top MultiPV evidence,
the `simplification_window` packet source/family, the checked `exchange_square:d5`
continuation, and the `subplan:simplification_window` contract all agree.

Move-local favorable-exchange sources follow the same split between proof and
commentary. `defender_trade` and `bad_piece_liquidation` witnesses in
`PlayerFacingTruthModePolicy` are built through `MoveReviewExchangeAnalyzer`,
which performs bounded legal FEN/PV replay instead of UCI substring slicing.
Both planner release and player-row projection consume
`ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision`, so source,
scope, exact-slice, branch/persistence, and tactical-veto checks stay
centralized. Defender-trade, bad-piece, and queen-trade rows repeat only the
admitted local-branch or queenless-branch fact; generic favorable-exchange
labels, malformed branch terms, or missing typed proofs fail closed instead of
becoming owner proof. Detailed truth/trust conditions for these exchange rows
are owned by `CommentaryTruthBoundary.md` and `CommentaryTrustBoundary.md`.
The current public bad-piece source example is
`source-capablanca-golombek-1939-bad-piece-liquidation`: ply 43 `Bxd6` is a
`SupportedLocal` source row only after exact replay, top-PV identity, the
`bad_piece_liquidation` packet source/family, and the
`subplan:bad_piece_liquidation` contract all agree.
The same analyzer now exposes a shared `RelationWitness` read-model for
move-local attack/defense relations. Defender-trade, bad-piece-liquidation,
overload, deflection, discovered-attack, double-check, back-rank mate,
mate-net, Greek gift, fork, hanging-piece, trapped-piece, domination,
zwischenzug, x-ray, clearance, battery, pin,
skewer, interference, decoy, PV-backed stalemate-trap, and PV-backed
perpetual-check observations enter as selector/support evidence from the same
catalog-driven relation producer. Standard relation witnesses consume the first
six plies of the legal top-PV replay prefix; PV-backed draw-resource witnesses
(`stalemate_trap` and `perpetual_check`) may consume up to twelve plies from
that same top-PV replay while still calling the analyzer-owned witness functions. The same
draw-resource replay exception may also consume validated root `ProbeResult`
reply PVs, but only when the probe result is contract-valid, its FEN matches the
current root FEN, and its probed/candidate move matches the reviewed played move.
`RelationObservationCatalog`
maps implemented relation witness kinds to semantic observation ids, typed
selector sources, strategic idea kind/readiness, confidence, and beneficiary
metadata. MoveReview relation support rows also take their public row label and
bounded wording from this catalog context: draw-resource, move-order,
restriction, line-geometry, and tactical-motif rows are phrased differently but
all remain display metadata over the same analyzer witness. The MoveReview
advanced row cap still bounds relation display, but implemented board-replayed
descriptors whose raw motif tags are witness-only (`trapped_piece`,
`domination`, and `zwischenzug`) sort ahead of generic relation rows once
admitted, while draw-resource relations keep the highest relation priority. The relation-kind inventory still exposes a deferred list for
non-public relation-shaped motif families, but that list is currently empty.
Motif vocabulary in adjacent model or detector assets does not become public
until it receives a board-replayed relation witness, semantic id, source id, and
implemented catalog descriptor. The catalog exposes a single inventory view for
implemented plus deferred kinds, while runtime descriptor lookup and evidence
admission continue to resolve only implemented kinds. The intermediate
`StrategicIdeaEvidence` carrier follows the same boundary: relation identity and
relation focus are retained only when the kind is implemented in
`RelationObservationCatalog`; unknown or uncataloged kinds are reduced to
ordinary non-relation evidence before selector merge.
`trapped_piece` is implemented through an analyzer-owned board witness: the
first replayed move must match the reviewed move, the moved attacker must attack
an explicit bound target, the explicit target set must be valid and non-empty,
the target must be a higher-value enemy non-pawn/non-king piece, and that target
must have no legal escape square that preserves the piece outside attack by the
pressure side. If the same reviewed move would otherwise produce `hanging_piece`,
the exact trapped-piece witness wins. Generic `PieceActivity.isTrapped`, raw
motif text, helper notation, and source-shaped strings do not mint this
relation.
The analyzer projection boundary requires typed `RelationDetails` for
`trapped_piece`, `domination`, `zwischenzug`, `stalemate_trap`, and
`perpetual_check`; a raw `RelationWitness` with only focus/fact strings fails
closed before semantic observation or player-surface relation rows.
`domination` is implemented through the same analyzer-owned board boundary, but
only for same/lower-value explicit targets that are not better handled by the
trapped-piece witness and are not ray pin/skewer geometry. The reviewed move
must legally replay, the moved attacker must attack the explicit valid target,
all target pseudo-escape squares must be controlled by the pressure side, and
the target must have no legal safe escape from the post-move position.
`MoveAnalyzer` domination motifs, generic restriction prose, helper notation,
and source-shaped strings do not mint this relation.
The supported-local public rows for these exact-board motifs are `Trapped
piece`, `Domination`, and `Zwischenzug`. Generic advanced relation metadata may
still categorize the same motifs as mobility-restriction or move-order relation
evidence, but when the supported-local row is present the player-surface builder
and backend sanitizer suppress the duplicate `strategic_relation` advanced row
for that same relation kind. This is a display/category projection of the
existing cataloged relation witnesses, not a separate mobility-cage or
move-order proof family.
`zwischenzug` is implemented through a narrow analyzer-owned board witness: the
reviewed move must be the first legal replayed move, explicit targets must parse
to a non-empty bound target set, the side to move must have had a legal non-pawn
capture on the expected recapture square before the reviewed move, the reviewed
move must not be that recapture, and the moved piece must give direct check.
This intentionally does not promote all existing `MoveAnalyzer.detectZwischenzug`
motifs; fork-only and winning-capture intermediate moves remain outside the
public relation until they receive a similarly bounded witness.
`stalemate_trap` is implemented only through the PV-backed draw-resource witness:
the first replayed move must match the played move, the bounded legal replay must
end in an actual stalemate position, and the engine score must stay within the
draw-stable centipawn band with no mate score. The semantic producer may inspect
up to twelve legal top-PV plies, or the same bound from validated root probe
reply PVs, for this draw-resource witness; other standard relation witnesses
keep the six-ply relation replay window. `MoveReviewSupportedLocalSurfaceRows`
may additionally consume the top-PV witness as a bounded `Stalemate resource`
summary row; validated probe reply PVs do not enter this summary fallback lane.
Board shape, detector tags, raw motif text, or helper notation alone do not
mint this relation.
`perpetual_check` follows the same draw-resource boundary: the first replayed
move must match the played checking move, the bounded legal replay must show at
least three checks by the same side, a repetition-compatible position key must
return inside the line, and the engine score must stay within the draw-stable
centipawn band with no mate score. The same twelve-ply draw-resource top-PV or
validated root probe-PV prefix is the only deeper public relation replay
exception here. `MoveReviewSupportedLocalSurfaceRows` may additionally consume
the top-PV witness as a bounded `Perpetual check` summary row; validated probe
reply PVs do not enter this summary fallback lane. Heavy-piece check density,
detector tags, raw motif text, or helper notation alone do not mint this
relation.
Legacy motif prose consumers and support cleanup all consume the relation
catalog boundary. Deferred lanes may emit softer non-relation wording where the
catalog allows it; witness-only raw relation tags emit no motif prose, helper
notation, threat label, high-tension admission, or fact-corroboration authority.
The detailed public/support-only/deferred trust rules for those consumers are
canonical in `CommentaryTrustBoundary.md`; this pipeline map keeps only the
runtime ownership fact that `RelationObservationCatalog` owns fallback labels
and that only implemented board/PV witnesses can mint relation authority.
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
Every implemented relation witness must carry typed details before it can
project semantic observation or MoveReview support. If a witness omits typed
details, or carries typed details that do not match its relation kind, the
semantic producer rejects the witness instead of falling back to raw focus,
target, or fact strings.
Semantic relation fact terms and owner/transition terms are likewise
canonicalized through the same analyzer-owned relation projection. Raw witness
fields remain diagnostic/source material on the witness object, but they are no
longer a projection fallback for implemented relation kinds, and invalid or
missing typed details do not emit branch, owner-seed, or transition terms.
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
same-destination UCI text is not enough, and a legal proof-sized prefix is
sufficient even when later PV text is stale. A bounded simplification exchange
square is recognized only from a legal immediate capture and recapture. Branch
keys, best-defense move terms, and exact-slice continuation terms for MoveReview
owner packets now come from bounded replayed moves over the current FEN, not raw
`VariationLine.moves` slicing.
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
line degrades to broader counterplay-reduction support when otherwise eligible.
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
containing raw `x`, `trade`, or `exchange` is not enough. After that packet is
replay-backed, concrete SAN capture tokens such as `Bxc6`, `dxc6`, or `Qxd8`
count as the line's exchange cue. Strategic line-scoped support uses the same
`lineShowsMainPathDelta` gate as engine-generated move-delta evidence, so
access, pressure, and plan-advance lines are not silently excluded by a separate
line-support mini-gate.
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
standard top-PV replay context for ordinary relation witnesses and keeps the
draw-resource exception on dedicated replay inputs: the twelve-ply top-PV prefix
and validated root probe reply PVs. The shared context assembles the resulting
relation-witness list and lets the relation producer consume only implemented
catalog kinds through kind-filtered witness access. This avoids divergent
standard replay windows, duplicate draw-resource projection, and ad hoc local
kind filtering.
For the implemented relation witnesses named above, the broad relation
observation itself remains selector/support evidence unless a separate proof
contract admits an owner packet. Explicit target lists are binding for relation
witnesses: if an exact target is supplied, the analyzer does not fall back to
arbitrary material targets, including discovered-attack target selection.
Double-check support is king-targeted and requires the replayed reviewed move
to leave the opposing king in check from at least two checker squares; because
it is not a structural/material target relation, any supplied explicit target
list closes the double-check candidate.
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
projection requires the selector-chosen relation and no longer infers a relation
from catalog-order source/fact matches alone. The selected relation must also
carry the descriptor-owned analyzer witness fact term, such as
`xray_relation_witness` or `defender_trade_branch`; source and semantic fact
alone remain internal support.
Relation surface target fallback is descriptor-owned: when a selected relation
lacks an analyzer target, `RelationObservationCatalog` supplies the
focus-square fallback policy instead of `MoveReviewPlayerPayloadBuilder`
repeating relation-kind branches. Carriers without a selected `relationKind`
stay silent and do not get to treat their generic `targetSquare` as analyzer
relation target evidence.
The selector also keeps the chosen relation's source ref, semantic fact, and
required analyzer witness fact ahead of generic evidence truncation, so the
catalog admission triple is not lost before `moveReviewPlayerSurface`
projection.
Once a selected idea names a relation kind, the public row must match that
kind's catalog source, semantic fact, and required witness fact or stay silent.
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
semantic rows on one replayed branch read-model. They now carry the same typed
branch details as exact-slice proof payloads for player-row verification; this
does not change their SupportedLocal contract tier. Detail decoding is owned by
analyzer helpers, not raw key reads in the policy layer. Common owner-seed and
transition term expansion is also analyzer-owned and projection-backed, so
policy code does not read `RelationWitness.facts` or `focusSquares` directly
for those owner packets. Other implemented relation rows stay support-only here, but
their king, attacker, defender/cleared-square, line or material target,
lure/execution, mate pattern, and front/back or pinned/behind coordinates are
also preserved as typed analyzer details for any later certified or
softer-label consumer.
Branch-key and branch-fact string formatting is likewise centralized in
`MoveReviewExchangeAnalyzer`; policy consumers may reuse replayed witness
line moves, but they must not rebuild branch keys with local UCI slicing or
`mkString` logic.
`MoveReviewPlayerPayloadBuilder` may project selected relation ideas into
`moveReviewPlayerSurface.advancedRows` with `authority.kind =
strategic_relation`, the relation token, and one relation-focus-derived target square. That is
public support metadata only: it comes from the cataloged selector evidence and
requires the catalog source ref, the matching semantic observation fact, and the
descriptor-owned analyzer witness fact.
The builder may expose up to four distinct catalog-approved relation rows in
`advancedRows`; the outer advanced-row cap is sized so the relation,
compensation, and typed structural-idea caps can all survive together before
later plan-detail rows, so this broadens public relation coverage without
creating additional proof authority.
When a supported-local practical row already surfaces the same relation kind
from a concrete board/PV witness, the builder suppresses the duplicate
`strategic_relation` advanced row for that relation kind. Exact defender-trade
and bad-piece-liquidation owner packets may carry that same bounded
`strategic_relation` metadata directly on their summary row after resolver
admission and `PlayerFacingExactSliceProofFacts.matchesPacket`; other
supported-local practical rows remain lower-authority `practical_plan` support.
The more specific practical row remains the user-facing line; other cataloged
relation kinds from the same `StrategyPack` can still project.
The backend cached-payload sanitizer applies the same duplicate suppression
after sanitizing summary rows, and only practical rows with practical-plan
authority or exact defender-trade / bad-piece-liquidation summary
`strategic_relation` authority can suppress matching advanced
`strategic_relation` authority. A stale label without authority does not
suppress a valid advanced relation row.
The source/semantic/witness admission triple, selected-`relationKind` match,
and no-fallback rule are owned by `RelationObservationCatalog`, so the selector,
builder, and tests consume the same descriptor helpers rather than repeating
relation matching locally. The same descriptor owns surface priority:
PV-backed draw-resource rows for stalemate-trap and perpetual-check evidence
are ordered ahead of generic relation rows inside the four-row display cap.
This priority is display-only and does not relax the legal PV, twelve-ply
draw-resource replay cap, stalemate or repeated-check, and draw-stable score
witness requirements.
The descriptor also owns the relation row kind used for prose selection
(`TacticalRelation`, `DrawResource`, `MoveOrder`, `MobilityRestriction`, or
`LineGeometry`); `surfaceRowLabel` remains display text and is not a renderer
policy key.
Additional analyzer witness facts cannot mint another catalog source or
semantic admission fact;
dynamic facts reject semantic/source/proof authority wire keys and remain
supplemental support evidence only.
It does not create proof/source/family authority or frontend reconstruction rights.
The final `UserFacingPayloadSanitizer` strips raw top-level
`strategyPack.strategicIdeas`; the player surface authority row is the bounded
public relation carrier. Backend and frontend sanitizers accept only relation
tokens present in `RelationObservationCatalog.Implemented`; key-shaped but
uncataloged relation ids are dropped, and cataloged relation tokens without a
valid target square are downgraded to display text.
The analyse frontend relation-token allowlist mirrors the full implemented
backend catalog, including witness-only promoted motifs such as trapped-piece,
domination, zwischenzug, stalemate-trap, and perpetual-check rows; it does not
define a smaller public relation subset.
Backend sanitization preserves `strategic_relation` authority on
`advancedRows` and on exact defender-trade / bad-piece-liquidation
supported-local summary rows whose label, cataloged token, and square target
match that exchange owner family; probe or author rows lose relation authority
even when the token is cataloged. The analyse frontend decoder applies the same
lane rule when reading cached or legacy JSON.
The MoveReview API path passes the sanitized `strategyPack` into that builder,
so relation rows originate from the same truth-vetted strategy pack that feeds
the prose and ledger rather than from cached prose or raw carrier fields.
`PlanMatcher` does not mint `defender_trade` from a raw
`RemovingTheDefender` motif or broad reason-code text. Those signals can only
support a generic `simplification_window`/favorable-exchange idea until the
semantic replay producer or owner witness proves the defender-trade branch.
`PlayerFacingTruthModePolicy` follows the same downgrade path: broad
defender/trade prose cannot create a `trade_key_defender` owner seed. If a
legal immediate capture/recapture sequence, move-linked exchange cue, and
narrative anchor are all present, the row degrades to `simplification_window`;
without those pieces it stays generic or closed.
The generic owner fallback also strips exact favorable-exchange families
(`defender_trade`, `bad_piece_liquidation`, `queen_trade_shield`) when their
replay witness is absent, so a broad plan owner cannot bypass the analyzer
after fail-closed rejection.
`StrategicFeatureExtractorImpl` follows the same softer-label rule for
high-level concepts: raw `RemovingTheDefender` tactical tags surface as generic
`Exchange pressure`, not as a user-facing "removing defenders" concept.

## Strategic Expansion Boundary Names

Closed expansion names are not maintained pipeline stages. They are audit
shorthand that must be translated before implementation:

- `broad heavy-piece/local-bind/global-squeeze expansion` maps to existing
  resource and route restriction boundaries. `LocalFileEntryBind`,
  `CounterplayAxisSuppression`, `ProphylacticRestraint`,
  `RouteNetworkBindProof`, and `TwoAxisBindProof` are positive/contract-facing
  names; `HeavyPieceLocalBindValidation` is a release-risk and negative-lane
  guard, not a place to add broad public claims.
- `B7` and `B8` are historical frontier/coverage labels. Runtime authority,
  proof families, source ids, and public row kinds must use chess/proof names
  instead.
- `broad color-complex expansion` is not a pipeline hook. The releasable path is
  the `ColorComplexSqueeze` exact-slice proof with
  `color_complex_squeeze_probe`; generic `color_complex_clamp` or color-complex
  support remains selector/support evidence. The bounded `Practical space` row
  can describe a typed current-board clamp at lower authority, but it does not
  open this exact-slice authority path or publish target metadata.
  The current source-window public examples are
  `source-botvinnik-vidmar-1936-flank-clamp`,
  `source-botvinnik-vidmar-1936-e4-color-complex-squeeze`,
  `source-camara-bazan-1960-d5-color-complex-squeeze`, and
  `source-pfleger-maalouf-1961-d5-color-complex-squeeze`, admitted only as
  `PositionLocal`/`CertifiedOwner` rows after exact FEN replay, top-PV move
  identity, and the typed `PlayerFacingExactSliceProof.ColorComplexSqueeze`
  packet match.
- `mobility-cage expansion` remains closed as a catch-all admission path. The
  only current public mobility-restriction surface is the cataloged
  `trapped_piece`/`domination` relation witness; route denial and broader
  mobility support still need their own concrete witness family before release.
- `broad lesson authority` is not a runtime authority layer. The current
  runtime boundary is `MoveReviewScopedTakeaway`; broad lesson release remains
  closed.
- Chronicle/Active runtime reopening remains closed under the removed-surface
  boundary above.

When adding a new strategic asset, name the source module after the stable
domain/proof role and route it through the existing owner:
`ProofContractRules` for authority eligibility, `ClaimAuthorityResolver` for
admission, `StrategicSemanticObservationContext`
and `RelationObservationCatalog` for selector/support observations, and
`MoveReviewPlayerPayloadBuilder` for product surface projection. Do not create
parallel expansion-specific runners, carriers, or product rows from umbrella
labels.

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

Current promoted proof-contract families:

This table summarizes public-surface eligibility. Witness requirements remain
owned by `ProofContractRules`; promoted families may require exact-slice proof,
structure-transition proof, or both.

| proof family | proof source | status | certified | supported local | default failure |
| --- | --- | --- | --- | --- | --- |
| `static_weakness_fixation` | `exact_target_fixation` | `Releasable` | true | true | `position_probe_not_certified` |
| `backward_pawn_targeting` | `carlsbad_fixed_target_probe` | `Releasable` | true | true | `position_probe_not_certified` |
| `half_open_file_pressure` | `local_file_entry_bind` | `Releasable` | true | true | `certified_owner_path` |
| `neutralize_key_break` | `counterplay_axis_suppression` | `Releasable` | true | true | `certified_owner_path` |
| `counterplay_restraint` | `prophylactic_move` | `Releasable` | true | true | `certified_owner_path` |
| `color_complex_squeeze` | `color_complex_squeeze_probe` | `Releasable` | true | true | `color_complex_authority_closed` |
| `target_focused_coordination` | `target_focused_coordination_probe` | `Releasable` | true | true | `certified_owner_path` |
| `outpost_entrenchment` | `outpost_entrenchment` | `Releasable` | false | true | `outpost_entrenchment_witness_missing` |
| `iqp_inducement` | `iqp_inducement_probe` | `Releasable` | false | true | `iqp_inducement_probe_missing` |
| `simplification_window` | `simplification_window` | `Releasable` | true | true | `same_job_or_conversion_relabel_blocked` |
| `defender_trade` | `defender_trade` | `Releasable` | false | true | `attacking_piece_trade_unowned` |
| `queen_trade_shield` | `queen_trade_shield` | `Releasable` | false | true | `source_queen_trade_boundary` |
| `bad_piece_liquidation` | `bad_piece_liquidation` | `Releasable` | false | true | `attacking_piece_trade_unowned` |
| `central_break_timing` | `central_break_timing` | `Releasable` | false | true | `central_break_timing_witness_missing` |

The current public target-coordination examples are
`K09A-certified-coordination` and `K09D-certified-coordination`; both project as
`CertifiedOwner` only through `runtime:target_focused_coordination` after legal
fixture PV replay and a matching
`PlayerFacingExactSliceProof.TargetFocusedCoordination` packet with
`coordinated_target:c6` terms. The `Target coordination` row may carry only
that same typed square as its practical-plan `target`, and cached public
wording must show two distinct support squares. They do not create a plan-kind
admission unit.

The current public source queen-trade row is
`source-carlsen-anand-2014-g6-queen-trade-completion`, ply 17 `Qxd8+` from
`r1bqk2r/1p3ppp/p1p1pn2/8/1bP1P3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 0 9`; it
projects only as `SupportedLocal` after legal replay of the queen-capture plus
king-recapture prefix `d3d8 e8d8` and matching `queen_trade_shield` proof terms.

Local-file entry support is promoted only by the exact file/entry witness:
`PlayerFacingTruthModePolicy` attaches
`PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare)` from
`LocalFileEntryProof.certifiedSurfacePair` only when the pair has valid typed
same-file geometry, and the packet still needs owner seed, continuation, proven
branch, stable persistence, no rival release, no tactical veto, and the
matching `HalfOpenFilePressure` contract.
Admission-unit tooling tracks `OpenFilePressure`, `KeySquareDenial`, and
`RookFileTransfer` as separate plan-kind work units for this same exact slice,
but all three use the same `local_file_entry_bind` proof source and
`half_open_file_pressure` family; raw plan-kind sources are not accepted as
public proof.
Public consumers must use the context-aware certification path: the current FEN
and legal reviewed-move replay must show a major piece on the claimed file and
the named same-file entry square either occupied or controlled by the moving
side. The typed proof fields must also be mirrored in packet witness terms as
the same file/entry pair through a single `file-entry:<file>:<square>` or
`file_entry:<file>:<square>` term. Split file and entry terms under the
`local_file_entry_bind` marker are not public release proof. The typed entry
square must lie on the claimed file; off-file pairs fail closed. The
FEN-less `certifiedSurfacePair(preventedPlans, evidenceBackedPlans)` overload is
only an internal pair extractor and is not a public commentary authority path.
The current public planner witness is `prophylactic_cut`: FEN
`2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23`,
played `a2a3`, certified pair `c-file`/`b4`. This is the shared public lane for
`OpenFilePressure`, `KeySquareDenial`, and `RookFileTransfer`; standalone
plan-kind labels do not open separate authority.
`MoveReviewSupportedLocalSurfaceRows` may project that packet as a bounded
`File entry` summary row through
`ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision`. The row text is
limited to the file and entry square; it is not a broad heavy-piece bind,
global squeeze, or no-counterplay claim. When the row comes from the matching
typed `LocalFileEntryBind`, it may carry only the same typed entry square as
practical-plan target metadata, and the public wording must name the entry
square's own file; top-PV practical file-entry rows, off-file wording, and stale
file-entry labels do not create target authority.

Named route-network support is promoted only from the existing certified
surface carrier. `RouteNetworkBindProof.certifiedSurfaceNetwork` supplies the
file/entry/reroute tuple through `QuestionPlannerInputs`; the MoveReview
surface consumes it only through
`ClaimAuthorityResolver.namedRouteNetworkSurfaceDecision`. The admitted row
uses practical-plan authority, exposes no proof metadata, and cannot be built
from planner prose, route text, or an intermediate route chain.

IQP inducement support is promoted only from the existing
`iqp_inducement_probe` contract. The public `IQP target` row requires
SupportedLocal move-delta admission plus
`PlayerFacingExactSliceProof.IqpInducement(targetSquare, lineMoves)` whose
target and checked line moves are mirrored in the board/PV transition terms. It
is not certified owner authority, and it is not inferred from broad IQP prose,
structural tags, or source-catalog labels. The Karpov-Andersson source row is
open only on that exact replayed IQP-inducement lane; adjacent same-game IQP
windows remain non-public unless admitted through the same contract boundary.

Simplification-window support is promoted only from the existing
`simplification_window` packet after the same resolver admission. The public
row uses practical-plan authority and may carry only the replayed exchange
square as its `target`; it is not a defender-trade, bad-piece, queen-trade, or
general conversion owner path. The Botvinnik-Vidmar source row is open only on that
exact replayed exchange-square lane; the Salov-Ljubojevic source row remains
suppressed because its owner proof is missing.

Outpost support is promoted only from the `outpost_entrenchment` exact-slice
packet. `PlayerFacingTruthModePolicy` may attach
`PlayerFacingExactSliceProof.OutpostOccupation("knight", square)` only when the
reviewed UCI move legally replays from the current FEN as a same-side knight
move to that square, the legal top PV starts with that reviewed move, and the
current/main-PV facts contain `Fact.Outpost(square, Knight, Now)`. The packet
must keep the outpost square, `piece:knight`, and
`outpost_occupation:knight:<square>` mirrored in witness terms, with a proven
same branch and stable persistence. The MoveReview row is bounded to
`Knight outpost` with practical-plan authority and may carry only that same
outpost square as its `target` when the row uses the builder-owned `knight`
wording; broad semantic outpost tags, route evidence, non-knight outpost prose,
or opening-goal outposts do not satisfy this exact-slice contract.
The current FEN-backed MoveReview witness is
`6k1/8/8/8/3P4/5N2/8/6K1 w - - 0 1`, played `f3e5`, checked top-PV line
`f3e5 g8f8 d4d5 f8e8`, and `Fact.Outpost(e5, Knight, Now)`. It creates an
`outpost_entrenchment` packet carrying
`PlayerFacingExactSliceProof.OutpostOccupation("knight", "e5")`; the row may
state only that the knight reaches the `e5` outpost and may expose only `e5`
as its public practical-plan target.

Quiet-move intent has a lower-authority MoveReview surface consumer.
`QuietMoveIntentBuilder` may feed `MoveReviewSupportedLocalSurfaceRows` only
after its packet passes `QuietMoveIntentClaim.allowsUserFacing`, stays
MoveLocal with no suppression or release risk, and the packet proof family,
proof source, and ontology family match the quiet-intent class. The reviewed
UCI move must legally replay from the current FEN. The replayed move must match
the quiet class and the packet square anchor: piece-improvement and
technical-conversion rows require a non-capturing non-pawn move to the anchored
square, and king-safety rows require a legal king move or matching castle
source. The row
uses `authority.kind = "practical_plan"` as `Piece improvement`,
`King safety`, or `Technical conversion`; it does not outrank exact,
central-break, opening-goal, or tactical truth rows. `CounterplayRestraint`
quiet intents remain closed on this path because exact counterplay/prophylaxis
rows own that public lane.

Knight outpost, bishop-pair retention, opposite-colored bishops, rook-pawn march, hook-creation, rook-lift, seventh-rank entry,
rook-behind-passer, passer blockade, file entry, back-rank mate, detector-named mate nets, Greek gift, double check, defender trade, bad-piece trade, queen trade, fork, overloaded defender, decoy, deflection, discovered attack, hanging piece, skewer, x-ray pressure, interference, battery pressure, pin pressure, connected rooks, doubled rooks,
connected passers, outside passer,
passed-pawn advance, and king activation have only a
lower-authority board-state MoveReview fallback.
`MoveReviewSupportedLocalSurfaceRows` may emit `Knight outpost`, `Bishop
pair`, `Opposite-color bishops`, `Rook-pawn march`, `Hook
creation`, `Rook lift`, `Seventh-rank entry`, `Rook behind passer`, `File
entry`, `Passer blockade`, `Back-rank mate`, `Mate net` or a detector-named mate label such as `Smothered mate`, `Greek gift`, `Double check`, `Defender trade`, `Bad piece trade`, `Queen trade`, `Fork`, `Overloaded defender`, `Decoy`, `Deflection`, `Discovered attack`, `Hanging piece`, `Skewer`, `X-ray pressure`, `Interference`, `Battery pressure`, `Pin pressure`, `Connected rooks`, `Doubled rooks`, `Connected
passers`, `Outside passer`, `Passed pawn
advance`, or `King activation` only when no
exact/supported-local row has survived, tactical truth does not veto support,
and the top PV legally starts with the same reviewed move. Within this fallback
lane, the builder consumes the normalized reviewed UCI through the cached first
`BoundedReplayStep`; it does not reparse raw `playedMove` text after the legal
top-PV start has already been certified. Mate/tactical/relation rows are
evaluated before broader positional rows so a general outpost, rook-lift,
file-entry, or opening support row does not preempt a more specific
board-replayed tactic. Detector-named mate labels are
still the `MateNet` relation contract; the public label may narrow to
`Smothered mate`, `Arabian mate`, `Boden's mate`, `Anastasia's mate`, `Hook
mate`, or `Corner mate` only from the matched `TacticalPatternDetectors` id.
Discovered-attack rows require the analyzer-owned relation witness: the
reviewed move must be the legal first top-PV move, the move must clear a square
on a long-range attack ray, and the after-board attack must newly hit the bound
target.
Knight-outpost rows
require a legal non-capturing knight move to an advanced square, the legal
after-board knight on the destination, friendly pawn support, and no enemy pawn
attack on that square. Bishop-pair rows require a legal bishop capture, the
legal after-board bishop on the destination, and after-board material with two
bishops for the mover and no bishop pair for the opponent. Opposite-color
bishop rows require a legal capture, exactly one bishop for each side on
opposite-colored squares after the move, and no remaining queens, rooks, or
knights. Rook-pawn and hook rows require the reviewed move to legally
replay as a non-capturing same-file a/h-pawn advance and
`PositionAnalyzer.extractStrategicState` to confirm the before/after rook-pawn
or hook state. Rook-lift rows require the reviewed move to legally replay as a
non-capturing rook move from the mover's back rank to the third rank for White
or sixth rank for Black; back-rank shuffles stay closed. Seventh-rank rows
require a legal non-capturing rook move from off the entry rank, and the legal
after-board must keep that rook on the destination square on White's seventh
rank or Black's second rank. Rook-behind-passer rows require a legal rook move,
an endgame material window, the legal after-board rook on the destination
square, and `PositionAnalyzer.passedPawns` to show a same-file friendly passer
in front of that rook. Passer-blockade rows require a legal non-capturing
knight move, the legal after-board knight on the destination, and
`PositionAnalyzer.passedPawns` to show an advanced enemy passer whose forward
stop-square is that destination. File-entry rows require a legal non-capturing rook/queen
move from another file to a non-back-rank square, the legal after-board major
piece on the destination square, and the same open/semi-open file definition
used by existing line-occupation evidence to admit that destination file.
Back-rank mate rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.backRankMateWitness` typed relation details: the
legally replayed reviewed move must end in checkmate and match the existing
back-rank tactical pattern detector. Mate-net rows require the same boundary and
the existing `MoveReviewExchangeAnalyzer.mateNetWitness` typed details: the
legally replayed reviewed move must end in checkmate and match a non-back-rank
mate-pattern detector. Greek-gift rows require the same boundary, the existing
`MoveReviewExchangeAnalyzer.greekGiftWitness` typed details, and top-PV
continuation support for the sacrifice entry; they name the sacrifice entry
only and do not claim forced mate or material conversion.
Double-check rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.doubleCheckWitness` typed relation details: the
legally replayed reviewed move must leave the opponent's king in check from at
least two checker squares, with no explicit target fallback.
Defender-trade rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.defenderTradeRelationWitness` typed branch details:
the checked top PV must legally replay the reviewed capture, the defender's
capture on the exchange square, and the mover's recapture. The target must be a
declared focal target or current `WeaknessTargetProfile` target, must not be the
exchange square, and the replayed recapture must remove that defender while
strictly reducing the target's defender count. The row is practical support and
does not create exact owner proof.
Bad-piece trade rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.badPieceLiquidationRelationWitness` typed branch
details: the reviewed move must legally start a branch where a constrained bad
bishop is traded for a non-pawn piece and recaptured. Open-bishop exchanges stay
closed. The row is practical support and does not create exact owner proof.
Queen-trade rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.queenTradeShieldLine` legal replay helper over the
first two top-PV plies: the reviewed move must be the queen capture and the
reply must be the opponent king recapturing that queen on the same square. The
row names only the queenless branch and does not create exact owner proof.
Fork rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.forkWitness` typed relation details, but the
product fallback narrows public prose to knight/pawn forks or forks that include
the king as a target. Generic queen/rook multi-attacks stay relation-support
evidence only.
Overloaded-defender rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.overloadWitness` typed relation details: the legally
replayed reviewed move must create or increase pressure on at least two targets
that the same defender is still assigned to. The row names the duty conflict
only; it does not claim the defender is lost or that the targets are forced.
Decoy rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.decoyWitness` typed relation details: the checked
top PV must legally replay the reviewed move, the opponent's capture on the
bait square, and the mover's recapture of the lured higher-value piece.
Deflection rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.deflectionWitness` typed relation details: the
checked top PV must legally replay the reviewed move and the defender's reply
moving away from the defended target after being attacked.
Hanging-piece rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.hangingPieceWitness` typed relation details: the
legally replayed reviewed move must attack an undefended analyzer-accepted
target, and without explicit target binding the analyzer admits only higher-value
non-pawn targets.
Skewer rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.skewerWitness` typed relation details: the legally
replayed reviewed move must be a long-range move that lines up a higher-value
front piece or king with a non-pawn back piece on the same ray. The row names
the geometry only; it does not claim a forced material win.
X-ray-pressure rows require the same top-PV/tactical-veto boundary and the
existing `MoveReviewExchangeAnalyzer.xrayWitness` typed relation details: the
legally replayed reviewed move must place a long-range attacker behind an enemy
blocker with an analyzer-accepted non-pawn target further down the same ray.
Generic clearance remains an implemented analyzer/catalog relation and may
publish through bounded `strategic_relation` metadata, but the practical
fallback lane does not emit a second `Clearance` summary row for the same
material line-clearing witness. Those public summary cases surface as
`Discovered attack` so one cleared-square witness does not project twice.
Because no runtime `Clearance` summary row is produced, stale cached or manual
`Clearance` practical labels do not suppress the typed clearance
`strategic_relation` row.
Interference rows require the same boundary and the existing
`MoveReviewExchangeAnalyzer.interferenceWitness` typed relation details: the
legally replayed reviewed move must put the moved piece between an enemy
long-range defender and an analyzer-accepted target that remains pressured.
Battery-pressure rows require the same top-PV/tactical-veto boundary and the
existing `MoveReviewExchangeAnalyzer.batteryWitness` typed relation details:
the legally replayed reviewed move must form a clear front/back queen, rook, or
bishop battery on a file, rank, or diagonal toward an analyzer-accepted target.
Pin-pressure rows require the same top-PV/tactical-veto boundary and the
existing `MoveReviewExchangeAnalyzer.pinWitness` typed relation details, but
they surface only absolute pins where the pinned piece is tied to the king.
Draw-resource summary rows require the same tactical-veto boundary plus the
dedicated twelve-ply top-PV draw-resource witness. `Stalemate resource` consumes
only a typed `stalemate_trap` witness whose legal replay ends in actual
stalemate with draw-stable score and no mate score. `Perpetual check` consumes
only a typed `perpetual_check` witness whose legal replay starts with the played
checking move, proves the repeated checking cycle, and stays draw-stable with no
mate score. Probe-PV draw-resource witnesses remain relation metadata unless
another product path consumes them.
Connected-rook rows require a legal non-capturing rook move, exactly two
friendly rooks on one rank after the move, and no occupied square between them.
Doubled-rook rows require a legal non-capturing rook move, exactly two friendly
rooks on one file after the move, and no occupied square between them.
Connected-passer rows require a legal non-capturing pawn advance, an endgame
material window, no enemy passed pawn, and a legal after-board adjacent
advanced passed-pawn pair no more than one rank apart. Outside-passer rows
require a legal non-capturing pawn advance, an endgame material window, no
enemy passed pawn, an advanced flank passer, a friendly pawn at least three
files away, and no enemy king directly blocking the passer from the front.
Passed-pawn rows
require the reviewed move to legally replay as a non-capturing pawn advance and
the after-board destination pawn to be included by the existing
`PositionAnalyzer.passedPawns` definition. King-activation rows require a legal
non-capturing non-castling king move in an endgame material window, the legal
after-board king on the destination square, a reduced center-distance score,
and no king-mobility loss. This path reuses existing
outpost-square, flank-infrastructure, seventh-rank, rook-behind-passer, file-occupation,
passed-pawn, and king-activity board
facts; it does not create certified owner authority, strategic relation
authority, or public proof metadata for the broader `rook_pawn_march`,
`hook_creation`, `rook_lift_scaffold`, semantic `motif_rook_lift`,
`outpost_entrenchment`, semantic `motif_outpost`, `OutpostOccupation`,
`rook_on_seventh`, `seventh_rank_invasion`, `rook_behind_passed_pawn`,
`RookBehindPassedPawn`, semantic `Blockade`, `Plan.Blockade`, endgame blockade
draw patterns, `bishop_pair_advantage`, `opposite_color_bishops`,
`OppositeColoredBishopsDraw`, minor-piece-imbalance owner authority, `open_file_pressure`,
`local_file_entry_bind`, broad `back_rank_mate`/`mate_net`/`greek_gift`/`defender_trade`/`bad_piece_liquidation`/`queen_trade_shield`/`overload`/`double_check`/`fork`/`decoy`/`deflection`/`hanging_piece`/`skewer`/`xray`/`clearance`/`interference` relation owner authority,
`connected_rooks`, `doubled_rooks`,
`RookFileTransfer`, `BatteryPressure` plan authority, broad `pin` relation owner authority, king-attack proof, `passed_pawn_manufacture`, `ConnectedPassers`,
`OutsidePasserDecoy`, `passer_conversion`, or promotion-race, opposition,
zugzwang, or outcome-proof lanes.

Position-probe exact slices are consumed through the same authority gate.
`MoveReviewSupportedLocalSurfaceRows` may project PositionLocal packets from
`exact_target_fixation`, `carlsbad_fixed_target_probe`,
`target_focused_coordination_probe`, or `color_complex_squeeze_probe` only after
`ClaimAuthorityResolver.decidePositionProbe` admits the packet and the typed
`PlayerFacingExactSliceProof` case matches the packet source/family and packet
witness terms mirror the typed target (`fixed_target:<square>` for target
fixation and Carlsbad, `coordinated_target:<square>` for target-focused
coordination). The rows are bounded target/coordination/color-complex summaries
with `authority.kind = "practical_plan"`; MoveLocal exact target packets remain on
their move-delta path and are not reclassified as position probes.
Color-complex support itself is promoted only by the exact board witness in
`PlayerFacingTruthModePolicy`: parsed FEN, opponent-owned semantic weak square,
color-complex/hole/fianchetto evidence, actual friendly bishop/knight attack
geometry on that square, same-square surface/semantic evidence, and a proven
stable packet branch. The typed proof shape is rechecked on consumption: the
declared color complex must match the target square color, and the named minor
piece geometry must be able to attack that target. Packet witness terms must
mirror the same weak square, color-complex token, role-specific
`minor_piece:<role>_<square>` term, and minor-piece attack pair;
coordinate/minor-piece terms remain trace labels only when they do not match the
typed proof.
Generic `color_complex_clamp` support still does not create a product row.
Admission-unit tooling may queue `StaticWeaknessFixation`,
`BackwardPawnTargeting`, `FlankClamp`, `OutpostEntrenchment`,
`IQPInducement`, `SimplificationWindow`, `DefenderTrade`, `QueenTradeShield`,
`BadPieceLiquidation`, and `CentralBreakTiming` only through their existing
runtime contracts. `target_focused_coordination` is a runtime proof family
rather than a `PlanTaxonomy.PlanKind` admission unit. For `FlankClamp`, the
accepted proof source remains `color_complex_squeeze_probe`; raw
`flank_clamp`, `color_complex_clamp`, `central_space_bind`, or
`mobility_suppression` sources are not public proof.
All `ExactSlice` contracts now require a typed `PlayerFacingExactSliceProof`
attached to `PlayerFacingProofPathWitness`; generic anchor, continuation, or
structure terms are not enough to satisfy the slice witness. The contract
matches the proof ADT case against the packet source/family and required
structured fields; it does not rebuild proof from witness-term strings. Public
non-position exact slices also mirror their typed fields in packet witness
terms: counterplay-axis suppression and prophylactic restraint require the same
named resource token, queen-trade shield requires the typed line moves, and
central-break timing requires the same `break_move`, `break_token`, and central
break square terms. Outpost occupation likewise requires the typed knight role,
outpost square, and `outpost_occupation:knight:<square>` witness term.
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
not fail only because the original b-pawn square is empty. The public MoveReview
row and position-probe main claim may name this as a bounded `Minority attack`
target / minority-attack fixed target only through that typed
`CarlsbadFixedTarget(..., minoritySupport = true)` proof; the standalone
`minority_attack_fixation` proof family remains deferred.
The exact `Minority attack` row may carry only the same typed `c6`/`c3` square
as practical-plan target metadata. Generic minority-attack support and the
lower-authority Carlsbad practical-context row do not expose target metadata.
MoveReview may also emit a lower-authority practical target row for
structural-only Carlsbad/minority-attack or backward-pawn-targeting plans, or
for a structural-only plan with a matching exact `fixed_target:<c6|c3>` hint,
when `PawnStructureTargets.carlsbadTargetForBoard` finds the same mirrored board
shape but no exact target proof is present. This row has `PracticalPlan`
authority without target metadata and uses structural context wording only;
checked-line, persistence, proof, or force wording stays reserved for the typed
`CarlsbadFixedTarget` exact slice above.
Opening-route target fixation is catalog-driven. `OpeningRouteCatalog` reads the
family/target/from/via/to/max-replay data plus a `target_mode` such as
`attack_weak_pawn` or `occupy_target`; `PieceRouteEvidence` reads raw
`VariationLine.moves` before stale parsed metadata, verifies legal UCI replay,
and carries the replayed terminal `Position`. `KnightRouteEvidence` is now only
the knight-route compatibility facade; exact route witnesses consume
`PieceRouteEvidence` directly. `OpeningRouteTargetEvidence.checkRouteEvidence`
then verifies that the route's target mode matches the replayed terminal board,
not the root board before the route is completed. `PlayerFacingTruthModePolicy`
then uses `findRouteWitness` to scan catalog routes instead of naming the Benoni
route in source, but the selected route target must match the declared focal,
directional, idea-focus, move-ref, semantic structural-weakness, or root-board
weak-pawn target when such a target is present. When `openingData.name` or an
`OpeningEvent.Intro` name resolves to an `OpeningFamilyCatalog` key, route
witness selection is also restricted to that same opening family and its target
allowlist; unlabeled positions keep the legal replay plus target allowlist gate.
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
opening-family player-surface target metadata and for exact target-fixation
when the same legal replay, family/target allowlist, declared-target, and
terminal target-mode gates pass. Current route data includes the master-backed mined routes with at
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
reconstructing fallback authority. `practical_plan` authority is normally
tokenless and targetless, except for exact `Fixed target`, `Minority attack`,
`IQP target`, `Simplification`, `Knight outpost`, `File entry`,
`Target coordination`, and `Color complex` rows where backend sanitization and
frontend decoding may preserve the typed proof square already admitted by the
row builder. Cached or stale payloads must match the builder-owned exact row
wording for that square; `Target coordination` target metadata additionally
requires two distinct support squares in that wording, and `Color complex`
target metadata additionally requires the exact bishop/knight and dark/light
color-complex wording plus matching target-square color and minor-piece attack
geometry. Exact-looking labels with lower-authority context prose, duplicate
support-square coordination prose, non-minor-piece prose, invalid minor-piece
geometry, or malformed complex wording keep at most untargeted `practical_plan`
authority.
`opening_family` rows may carry
`openingFamily` from the product builder's `SupportedLocal` resolver path.
They may carry `target` only for backend-allowlisted opening/target pairs from
`OpeningFamilyCatalog`; unsupported targets are stripped without dropping the
opening-family row. `MoveReviewPlayerPayloadBuilder` emits this target metadata
only from exact legal route evidence for the same opening family; frontend code
must still treat the target chip as metadata on an admitted support row, not as
proof reconstructed from cached prose. `openingBook` is decoded only on
`opening_family` authority and is bounded to ECO, aggregate total games, and
SAN top moves; it is display metadata, not a new admission input.
Current public row authority kinds are `counterplay_break`, `central_break`,
`central_liquidation`, `central_challenge`, `practical_plan`,
`opening_family`, and catalog-bound `strategic_relation`. These are display
or bounded support rows, not certified owner claims and not frontend
reconstruction inputs. Cataloged relation evidence may appear as
`strategic_relation` authority on advanced rows or on exact defender-trade /
bad-piece-liquidation supported-local summary rows that already passed resolver
and exact-slice matching. This is a schema recap of the catalog-bound,
target-required support lane above; frontend rendering may show the token and
target but must not reconstruct relation evidence from cached prose or legacy
raw carriers.
Descriptor-specific relation rows, including tactical, draw-resource,
move-order, and mobility-restriction rows, may use only the
analyzer-projected focus-order squares to make support prose less generic; they
do not recover piece roles, branch ownership, or conversion truth from raw
strings. Defender-trade or bad-piece wording in this row remains support-only;
exact owner proof still requires the dedicated typed owner packet.
Line-geometry relation rows may likewise name focus-order squares as
role-neutral geometry only; pin/skewer rows do not claim absolute pins,
material wins, axes, or piece roles unless another typed public contract admits
that stronger claim.

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
Fresh `MoveReviewPlayerPayloadBuilder` construction may project the in-memory
`moveReviewExplanation.shortLine` and bounded PV learning point into an
authority-free `Checked line` summary row on `moveReviewPlayerSurface`; when
that line is missing, it may use the preferred current-move `MoveReviewRefs`
variation for the same authority-free row. Legacy or cached top-level
`moveReviewExplanation` may supply an existing title only; `factFragments` are
ignored by the decoder and stripped by the backend sanitizer. Legacy or cached
top-level `moveReviewLedger` is metadata/signal only: malformed ledger keys drop
the ledger, malformed line rows are dropped, and top-level ledger lines are not
rendered as support/probe rows. Player-facing probe/support rows come from
`moveReviewPlayerSurface`, with ledger-backed probe rows under
`moveReviewPlayerSurface.probeRows`.
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
`moveReviewPlayerSurface`: summary rows and backend decision comparison are the
support lane, and advanced/probe/author rows are the review-detail lane. Raw
carrier reconstruction is not used for
MoveReview QC support rows. If a MoveReview raw artifact or canonical surface is
absent, QC queues keep the MoveReview commentary only and do not synthesize
support rows from raw carriers, legacy chronicle metadata, or removed
Active/Chronicle QC helpers.
MoveReview corpus rows also carry `moveReviewCausalClaimStatus`,
`moveReviewCausalClaimQuestion`, `moveReviewCausalClaimSubject`,
`moveReviewCausalClaimEvidence`, `moveReviewCausalClaimRelations`,
`moveReviewCausalClaimRejectReasons`, `moveReviewCausalClaimSupportEmbedded`,
and `moveReviewCausalClaimGuardrail`. These are diagnostic verification fields:
they mirror the typed causal release gate and must not be used as a separate UI
surface or as a prose-derived authority source.

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
