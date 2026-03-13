# Commentary Pipeline Consumption SSoT

This file is the single source of truth for the March 6, 2026 audit of the
Chesstory commentary-analysis pipeline, with emphasis on helper-module signal
production, transport, and actual user-facing consumption.

## Authority

- Snapshot date: `2026-03-06`
- Snapshot commit: `a083b84fe839ad121a48fecd30b072eacf25d1f2`
- Working tree status: dirty (includes uncommitted remediation for finding 1)
- Workspace root used for references: `C:\Codes\CondensedChess\lila-docker\repos\lila`

Use this document as the default reference for questions of the form:
- which helper modules are fully/partially/under-utilized?
- where does a signal get dropped?
- which runtime path consumes a signal?
- how do full-game and Bookmaker paths differ?

Re-audit only if files in the covered runtime paths change after this snapshot,
or if a request explicitly asks for a fresh audit.

## Scope

Covered module families:
- `strategic`
- `semantic`
- `endgame`
- `opening`
- `probe`
- `plan`
- `pawn`
- `structure`
- `threat`
- `practicality`
- `counterfactual`
- `authoring`

Covered runtime paths:
- full-game path
- single-position Bookmaker path
- carrier/model layers
- outline / renderer layers
- API / response models
- frontend consumption
- continuity tokens and cache keys

Related runtime-contract doc:
- `modules/llm/docs/BookmakerProseContract.md`
- `modules/llm/docs/CommentaryOpsMetrics.md`

Validation artifacts for thesis-driven Bookmaker prose:
- golden snapshots:
  - `modules/llm/src/test/resources/bookmaker_thesis_goldens/*.slots.txt`
  - `modules/llm/src/test/resources/bookmaker_thesis_goldens/*.draft.txt`
  - `modules/llm/src/test/resources/bookmaker_thesis_goldens/*.final.txt`
- golden regression tests:
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerProseGoldenTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerProseGoldenFixtures.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
- refresh / QA tools:
  - `modules/llm/src/test/scala/lila/llm/tools/BookmakerProseGoldenDump.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/BookmakerThesisQaRunner.scala`
- latest QA report:
  - `modules/llm/docs/BookmakerThesisQaReport.md`
  - current live snapshot:
    - six thesis motif fixtures active
    - `polish_acceptance_ratio=1.000`
    - `polish_fallback_rate=0.000`
    - `claim_like_first_paragraph=6/6`
    - `paragraph_budget_2_4=6/6`
    - `placeholder_leakage=6/6`

Internal-only live ops metrics:
- surfaced through the existing Prometheus scrape path
- not user-facing UI
- operator-only surfaces:
  - Prometheus scrape: `/prometheus-metrics/<internal-key>`
  - sampled drilldown JSON: `/internal/commentary-ops/<internal-key>`
- current runtime tracks:
  - soft repair `any` vs `material`
  - decision comparison consistency for `bookmaker` and `fullgame`
  - Active thesis agreement
  - sampled internal-only failure-class logs with no raw user text
  - response-level Bookmaker fallback trace samples for
    `fallback_rule_invalid`, `fallback_rule_empty`, and `rule_circuit_open`

## Runtime Path Map

### Full-game path

Entry chain:
- `app/controllers/LlmController.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`

Primary path:
1. `LlmController.analyzeGameLocal`
2. `LlmApi.analyzeFullGameLocal`
3. `CommentaryEngine.generateFullGameNarrative`
4. `NarrativeContextBuilder.build`
5. `BookStyleRenderer.render`
6. `CommentaryEngine.renderHybridMomentNarrative`
7. `GameNarrativeResponse.fromNarrative`
8. `ui/analyse/src/narrative/*`

Key references:
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:540`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:549`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:645`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:672`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:675`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:790`
- `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala:47`
- `ui/analyse/src/narrative/narrativeCtrl.ts:40`

Important caveat:
- Historical audit note:
  - before remediation, full-game path truncated rendered prose to the first
    `3-4` paragraphs via `focusMomentBody`, which caused systematic late-beat
    loss.
- Current working-tree state:
  - full-game path now builds a validated outline first and preserves beats by
    `focusPriority` / `fullGameEssential` before rendering the focused body.

### Bookmaker path

Entry chain:
- `app/controllers/LlmController.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
- `ui/analyse/src/bookmaker.ts`

Primary path:
1. `LlmController.bookmakerPosition`
2. `LlmApi.bookmakerCommentPosition`
3. `LlmApi.computeBookmakerResponse`
4. `CommentaryEngine.assessExtended`
5. `NarrativeContextBuilder.build`
6. `BookStyleRenderer.render`
7. `CommentResponse`
8. `ui/analyse/src/bookmaker/responsePayload.ts`
9. `ui/analyse/src/bookmaker.ts`

Key references:
- `app/controllers/LlmController.scala:137`
- `app/controllers/LlmController.scala:162`
- `app/controllers/LlmController.scala:182`
- `app/controllers/LlmController.scala:191`
- `app/controllers/LlmController.scala:263`
- `modules/llm/src/main/LlmApi.scala:2216`
- `modules/llm/src/main/LlmApi.scala:2338`
- `modules/llm/src/main/LlmApi.scala:2350`
- `modules/llm/src/main/LlmApi.scala:2391`
- `modules/llm/src/main/LlmApi.scala:2394`
- `ui/analyse/src/bookmaker/responsePayload.ts:59`
- `ui/analyse/src/bookmaker.ts:449`
- `ui/analyse/src/bookmaker.ts:462`
- `ui/analyse/src/bookmaker.ts:482`

Important caveat:
- Bookmaker does not use the `NarrativeGenerator.describeHierarchical` fallback.
  It relies on `BookStyleRenderer.render` directly.
- Frontend runtime is now explicitly on-demand:
  - position changes still refresh Bookmaker context and surface cached entries
    automatically
  - fresh `/api/llm/bookmaker-position` requests are started only from the
    explicit Bookmaker request button, not on every move change
  - stale in-flight Bookmaker/opening requests are aborted on context change,
    and obviously unsafe raw fallback text is blocked behind a retry state
    rather than surfaced directly
- Persistence shape is now two-tiered:
  - general analysis restores Bookmaker entries for the current browser tab
    from session storage keyed by page scope + node path
  - study chapters persist exact Bookmaker panel snapshots in local browser
    storage keyed by `study/chapter/commentPath`, and also persist AI
    commentary + inserted PV lines on the server via `bookmaker-sync`
  - if a study snapshot is absent locally, frontend falls back to the saved
    `Chesstory AI` external node comment and reconstructs a minimal saved-study
    Bookmaker surface from the current study tree
- Premium Bookmaker requests now use a dedicated burst limiter path with a
  longer queue timeout than full-game premium analysis, so interactive
  single-move commentary is less likely to die waiting behind the shared
  sequencer.

### State continuity and cache

Conclusion:
- `planStateToken` and `endgameStateToken` continuity is not the main broken
  area in this audit.
- Both the backend cache key and the Bookmaker frontend cache key include
  state-token fingerprints.

Key references:
- `modules/llm/src/main/scala/lila/llm/CommentaryCache.scala:89`
- `modules/llm/src/main/scala/lila/llm/CommentaryCache.scala:101`
- `modules/llm/src/main/LlmApi.scala:2394`
- `modules/llm/src/main/LlmApi.scala:2395`
- `ui/analyse/src/bookmaker.ts:165`
- `ui/analyse/src/bookmaker.ts:195`
- `ui/analyse/src/bookmaker.ts:430`
- `ui/analyse/src/bookmaker.ts:431`
- `ui/analyse/src/bookmaker.ts:449`
- `ui/analyse/src/bookmaker.ts:450`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:450`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:483`

## Post-audit updates

- `2026-03-06` working-tree update:
  - `DecisionRationale / MetaSignals / StrategicFlow / OpponentPlan` is no
    longer dead on the primary prose path.
  - `NarrativeOutlineBuilder` now injects `strategicFlow`, `opponentPlan`, and
    choice/concurrency meta into the `Context` beat, and injects
    `DecisionRationale` plus `MetaSignals.whyNot/error/divergence` into the
    `DecisionPoint` beat.
  - `DecisionPoint` can now be emitted even when there is no non-latent author
    question, so `ctx.decision` is no longer gated on authoring presence.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:603`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:608`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:613`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:637`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:667`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeSignalConsumptionTest.scala:34`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeSignalConsumptionTest.scala:92`
- `2026-03-06` working-tree update:
  - full-game moment bodies no longer clip by raw paragraph count first; they
    now select a focused subset of validated outline beats by priority.
  - `Context`, `DecisionPoint`, and `MainMove` are marked as
    `fullGameEssential`, while `ConditionalPlan`, `OpeningTheory`,
    `StrategicDistribution`, and `WrapUp` now carry explicit `focusPriority`.
  - `opening`, `strategic stack`, and `structural cue` are also injected into
    early prose, and `practical / compensation` are partially lifted into the
    `MainMove` beat.
  - `LatentPlan` no longer dies outright on the full-game path when probe
    evidence is absent; a heuristic path now survives validation with lowered
    confidence.
  - `StrategyPack` is now generated on the polish path as well, and both
    `CommentResponse` and full-game moments carry a structured
    `signalDigest`.
  - Bookmaker frontend now renders a lightweight `Strategic Signals` summary
    above prose when plan / latent / digest payload is present.
  - Bookmaker summary now also renders `Evidence Probes` rows from
    `probeRequests`, so probe/authoring intent is no longer count-only during
    the single-position flow.
  - Backend carrier/API now also exports structured `authorQuestions` and
    `authorEvidence` summaries on both the Bookmaker response and full-game
    moment payloads, and `signalDigest` / `strategyPack` can now carry an
    authoring-evidence headline for downstream prompt shaping.
  - full-game frontend now renders a `Strategic Signals` block using
    `signalDigest`, `mainStrategicPlans`, `latentPlans`, and
    `whyAbsentFromTopMultiPV`.
  - `StructureProfile / PlanAlignment / Prophylaxis / Practicality /
    Compensation` no longer lose key carrier fields before rendering:
    `PracticalInfo` now preserves bias factors, `PreventedPlanInfo` now
    preserves `counterplayScoreDrop`, and `NarrativeSignalDigest` now carries
    structure/alignment/prophylaxis/practical/compensation details as distinct
    fields rather than a single flattened cue.
  - full-game and Bookmaker visible summaries now surface these richer fields
    directly, and primary prose selectively promotes them through the
    `Context`, `MainMove`, and `WrapUp` beats.
  - deterministic post-processing no longer redacts named structure taxonomy
     like `Carlsbad`/`Hedgehog` into generic `structure` labels.
  - Bookmaker prose structure is now explicitly fixed as an `L2.5 hybrid`
    contract: UI owns summary sections/cards, while the LLM only rewrites the
    commentary body as paragraph-structured prose.
  - `PolishPrompt.systemPrompt`, `buildPolishPrompt`, `buildRepairPrompt`, and
    segment-only `buildSegmentRepairPrompt` now instruct polish/repair models
    not to emit UI section headers, to
    preserve paragraph boundaries, and to target `2-4` short paragraphs in
    isolated-move / Bookmaker mode.
  - To reduce prompt cost, the full contract lives in documentation and the
    cached system prompt, while per-request polish/repair prompts now carry only
    a short structure reminder rather than the full contract text.
  - The contract is documented separately in
    `modules/llm/docs/BookmakerProseContract.md`.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/model/authoring/NarrativeOutline.scala:40`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:154`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:705`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:995`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineValidator.scala:104`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala:48`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:549`
  - `modules/llm/src/main/LlmApi.scala:2362`
  - `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala:47`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeSignalDigestBuilder.scala:1`
  - `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala:343`
  - `modules/llm/src/main/scala/lila/llm/models.scala:110`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala:16`
  - `modules/llm/src/main/scala/lila/llm/analysis/PostCritic.scala:18`
  - `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala:12`
  - `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala:144`
  - `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala:184`
  - `modules/llm/src/test/scala/lila/llm/PolishPromptTest.scala:5`
- `2026-03-08` working-tree update:
  - `StructureProfile / PlanAlignment / pieceActivity/keyRoutes` now feed a
    shared synthesis helper, `StructurePlanArcBuilder`, instead of reaching
    Bookmaker prose only as loosely connected generic structure text.
  - Bookmaker structure thesis can now consume a deterministic
    `structure -> long plan -> primary deployment -> current move contribution`
    arc.
  - `NarrativeSignalDigest` now carries optional deployment fields
    (`deploymentOwnerSide`, `deploymentPiece`, `deploymentRoute`,
    `deploymentPurpose`, `deploymentContribution`, `deploymentStrategicFit`,
    `deploymentTacticalSafety`, `deploymentSurfaceConfidence`,
    `deploymentSurfaceMode`), and Bookmaker renders a lightweight
    `Piece Deployment` row inside `Strategic Signals`.
  - Active mode reuses the same deployment cue through `StrategyPackBuilder`,
    `longTermFocus`, `evidence`, and `ActiveStrategicPrompt`.
  - This does not introduce a new producer; it deterministically joins already
    existing structure/alignment/piece-activity signals.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StructurePlanArcBuilder.scala:1`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala:82`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeSignalDigestBuilder.scala:12`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala:16`
  - `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala:3`
  - `modules/llm/src/main/scala/lila/llm/models.scala:110`
  - `ui/analyse/src/bookmaker/responsePayload.ts:52`
  - `ui/analyse/src/bookmaker.ts:139`
  - `modules/llm/src/test/scala/lila/llm/analysis/StructurePlanArcBuilderTest.scala:1`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala:1`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategyPackBuilderTest.scala:1`
  - `modules/llm/src/test/scala/lila/llm/ActiveStrategicPromptTest.scala:1`
- `2026-03-08` cleanup update:
  - route-purpose / route-confidence heuristics are no longer duplicated across
    `StructurePlanArcBuilder` and `StrategyPackBuilder`; the strategy-pack path
    now reuses `StructurePlanArcBuilder.cueFromStrategicActivity`.
  - `LlmApi` no longer carries `ccaHistoryRepo = null` as a sentinel default;
    the dependency is now modeled as `Option[CcaHistoryRepo]`.
  - user-facing placeholder scrubbing is now centralized in
    `UserFacingSignalSanitizer` and reused by Bookmaker slot sanitation,
    strategy-pack authoring evidence summaries, and thesis evidence hooks.
- `2026-03-12` route reliability remediation:
  - `StrategyPieceRoute` now represents redeployment only.
  - enemy-occupied non-origin route squares are reclassified into
    `StrategyPieceMoveRef`, not surfaced as route destinations.
  - `PieceActivity` now carries separate `keyRoutes` and `concreteTargets`.
  - route carriers preserve `ownerSide` and split the old scalar into:
    - `strategicFit`
    - `tacticalSafety`
    - `surfaceConfidence`
    - `surfaceMode`
  - `surfaceMode` drives all user-facing route surfacing:
    - `< 0.55` => hidden
    - otherwise => `toward`
    - `exact` requires high fit, high safety, empty transit squares, and
      piece-policy approval
  - exact-path guardrails are now piece-specific:
    - knight: allowed
    - bishop: short routes only
    - rook: short file/lift routes only
    - queen: never exact in v1
  - fallback active-note chips now hide opponent-owned routes from the default
    chip row and emit raw `a-b-c` paths only for `surfaceMode = exact`
  - active-note ops telemetry now tracks:
    - `route_redeploy_count`
    - `route_move_ref_count`
    - `route_hidden_safety_count`
    - `route_toward_only_count`
    - `route_exact_surface_count`
    - `route_opponent_hidden_count`
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala:90`
  - `modules/llm/src/main/scala/lila/llm/analysis/StructurePlanArcBuilder.scala:91`
  - `modules/llm/src/main/LlmApi.scala:15`
  - `modules/llm/src/main/Env.scala:32`
  - `modules/llm/src/main/scala/lila/llm/analysis/UserFacingSignalSanitizer.scala:1`
  - `modules/llm/src/main/scala/lila/llm/analysis/AuthoringEvidenceSummaryBuilder.scala:7`
  - `modules/llm/src/main/scala/lila/llm/analysis/strategic/StrategicAnalyzers.scala:1`
  - `modules/llm/src/main/scala/lila/llm/models.scala:131`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryOpsBoard.scala:36`
  - `ui/analyse/src/narrative/narrativeView.ts:448`
  - `ui/analyse/src/bookmaker.ts:130`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala:8`
  - `modules/llm/src/test/scala/lila/llm/ActiveStrategicPromptTest.scala:5`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategyPackBuilderTest.scala:205`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeSignalConsumptionTest.scala:144`
- `2026-03-12` Bookmaker frontend cleanup-only update:
  - the Bookmaker initial-response and refined-response paths no longer decode the
    same payload fields independently inside `bookmaker.ts`; both now go through a
    shared `decodeBookmakerResponse` helper.
  - stored Bookmaker cache entries are now materialized through one
    `buildStoredBookmakerEntry` helper instead of duplicating count/metadata
    assembly at each fetch stage.
  - the Bookmaker frontend `NarrativeSignalDigest` adapter now preserves
    `authoringEvidence`, matching the backend wire shape.
  - no Active-note or full-game runtime contract changed in this cleanup.
- Verification:
  - `ui/analyse/src/bookmaker/responsePayload.ts`
  - `ui/analyse/src/bookmaker/studyPersistence.ts`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/tests/bookmakerResponsePayload.test.ts`
  - `ui/analyse/tests/bookmakerPersistence.test.ts`
- `2026-03-12` cleanup-only follow-up:
  - the old strategic-idea keyword classifier is no longer shipped from
    `src/main`; it now lives under `src/test` as a comparison helper for
    selector regression tests only.
  - frontend Chesstory signal shape and text-format helpers now live in shared
    modules so Bookmaker and full-game narrative no longer maintain parallel
    `NarrativeSignalDigest` / `StrategicIdea*` definitions or duplicate token /
    evidence / deployment formatting logic.
  - authoring evidence payload assembly now has a single `build(ctx)` bundle,
    and the full-game path reuses that bundle plus its headline instead of
    recomputing question/evidence/headline slices independently.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/AuthoringEvidenceSummaryBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeSignalDigestBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
  - `modules/llm/src/main/LlmApi.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/LegacyStrategicIdeaTextClassifier.scala`
  - `ui/analyse/src/chesstory/signalTypes.ts`
  - `ui/analyse/src/chesstory/signalFormatting.ts`
  - `ui/analyse/src/bookmaker/responsePayload.ts`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/src/narrative/narrativeCtrl.ts`
  - `ui/analyse/src/narrative/narrativeView.ts`
- `2026-03-08` working-tree update:
  - Bookmaker prose is now thesis-driven before LLM polish. The deterministic
    draft no longer treats rich helper signals as a flat parallel list when a
    dominant strategic lens is available.
  - `NarrativeContext` now carries an internal `renderMode`, so the new thesis
    path is scoped to Bookmaker while full-game continues to use the existing
    focused-outline flow.
  - `StrategicThesisBuilder` selects one internal lens in fixed priority order:
    `compensation -> prophylaxis -> structure -> decision -> practical -> opening`.
  - When a thesis exists, Bookmaker outline construction collapses to
    `MoveHeader + Context(claim) + MainMove(cause/effect) + optional
    DecisionPoint(tension/evidence) + optional WrapUp(practical/compensation)`.
  - Standalone Bookmaker paragraphs are therefore explicitly organized as:
    dominant claim, causal support, optional tension/evidence, optional
    practical/compensation coda.
  - Bookmaker polish is now slot-driven rather than prose-blob-driven.
    `BookmakerPolishSlots` carries `claim`, `supportPrimary`,
    `supportSecondary`, `tension`, `evidenceHook`, `coda`,
    `factGuardrails`, and `paragraphPlan`, and the LLM sees labeled slot
    sections instead of a long draft blob.
  - User-facing sanitization of authoring / probe / evidence placeholders now
    happens before polish via `BookmakerSlotSanitizer`; raw labels like
    `subplan:*`, `theme:*`, `support:*`, `seed:*`, `proposal:*`, and
    placeholder tokens such as `{them}` are stripped or rewritten before the
    prompt is built.
  - `LlmApi` now applies a Bookmaker-only soft repair pass after LLM polish.
    Paragraph count, missing claim, and placeholder leakage are repaired
    deterministically without turning prose-shape issues into hard fallback
    reasons.
  - Slot-mode hard validation now stays focused on factual preservation.
    Length-ratio failures and paragraph-shape issues are repaired softly,
    while SAN / eval / branch integrity still remains the hard gate.
  - `PolishPrompt` now explicitly tells the LLM not to flatten the dominant
    strategic claim or its cause/effect chain into generic advantage wording.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/LlmApi.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
  - `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala`
  - `modules/llm/docs/BookmakerProseContract.md`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeSignalConsumptionTest.scala`
  - `modules/llm/src/test/scala/lila/llm/PolishPromptTest.scala`
- `2026-03-08` closeout-quality update:
  - provider payload wrappers are now normalized through
    `CommentaryPayloadNormalizer` before Bookmaker soft repair or contract
    validation. JSON / quoted-JSON / fenced payload wrappers are no longer
    treated as prose failures.
  - slot-mode polish/repair prompts now carry the exact opening clause of the
    claim, so paragraph 1 is pushed to preserve the thesis opening clause more
    literally instead of rephrasing it into generic first-sentence prose.
  - deterministic paragraph 3 now wraps bare variation evidence as prose
    (`A concrete line is ...`) so tension/evidence paragraphs survive polish
    with less repair pressure.
  - closeout QA now distinguishes `soft_repair_applied_rate` from
    `soft_repair_material_rate`: claim-only opening-clause restoration is
    treated as cosmetic, while paragraph/evidence/placeholder fixes remain
    material.
 - `2026-03-10` output-budget update:
   - Bookmaker / full-game / Active no longer rely on the old
     `OPENAI_MAX_OUTPUT_TOKENS=256` budget in practice.
   - Runtime output caps were raised to reduce truncation-driven invalid polish:
     - Bookmaker adaptive polish cap now targets `480-840` tokens on sync and
       `640-1200` on async.
     - full-game segment polish cap now allows up to `420` sync / `520` async.
     - Active strategic notes now request `420` sync / `560` async output
       tokens.
   - The environment fallback default `OPENAI_MAX_OUTPUT_TOKENS` was raised to
     `640` for paths that do not supply a per-call override.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryPayloadNormalizer.scala`
  - `modules/llm/src/main/LlmApi.scala`
  - `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryPayloadNormalizerTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CoreCommentaryCloseoutQaRunner.scala`
- `2026-03-08` working-tree update:
  - tactical mistake / blunder criticism now has higher priority on the
    Bookmaker prose path than a competing strategic thesis.
  - when `MetaSignals.errorClass` or counterfactual severity marks the move as
    a tactical mistake / blunder, `BookmakerPolishSlotsBuilder` now promotes
    the negative `MainMove` annotation sentence into `p1=claim` instead of
    opening with structure / plan prose and deferring the error to later
    paragraphs.
  - `NarrativeOutlineBuilder` also now prioritizes tactical missed-motif and
    forcing-reply issue text ahead of generic practical framing, and prefers
    forcing tactical punishment in consequence text before falling back to
    generic severity summaries.
  - result: single-position commentary starts with the concrete tactical
    verdict more reliably when the played move is a tactical error, while the
    rest of the Bookmaker slot / polish contract remains unchanged.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
- `2026-03-08` working-tree update:
  - opening precedents are now consumed as `player game -> strategic branch`
    rather than only as loose historical snippets.
  - a new shared helper ranks opening sample games by local move overlap,
    metadata richness, and coarse mechanism/plan compatibility, then derives a
    representative strategic branch label such as a pressure / simplification /
    structural-transformation branch.
  - `StrategicThesisBuilder` now uses that representative line on the opening
    lens so Bookmaker prose can mention a concrete player game and explain
    which strategic branch it points toward.
  - `NarrativeOutlineBuilder` now also injects the same branch summary into the
    opening-theory beat and prefers the representative branch sentence over a
    raw precedent block when only a single focused precedent is needed.
  - the opening path now also states whether the current move stays inside that
    representative branch or bends away from it. This relation is derived
    deterministically from the current opening event (`BranchPoint`,
    `OutOfBook`, `Novelty`, `TheoryEnds`, `Intro`), the representative
    trigger move, and opening top-move membership.
  - this closes the intended `L3` opening contract for Bookmaker:
    opening name, representative player game, strategic branch label, and the
    current move's relation to that branch.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/OpeningPrecedentBranching.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/OpeningPrecedentBranchingTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
- `2026-03-08` working-tree update:
  - cleanup-only pass reduced active duplication in the Bookmaker/opening stack.
  - `OpeningPrecedentBranching` now owns shared opening-precedent player
    normalization, SAN tokenization, and mechanism inference used by both the
    thesis path and the older opening-theory comparison path.
  - tactical/blunder escalation policy is now centralized in
    `CriticalAnnotationPolicy`, so Bookmaker claim promotion and outline-level
    `tacticalEmphasis` no longer keep separate predicates.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/OpeningPrecedentBranching.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CriticalAnnotationPolicy.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CriticalAnnotationPolicyTest.scala`
- `2026-03-08` working-tree update:
  - `mode: userAnalysis` now consumes full-game commentary through a
    narrative-first `Review Shell`, not an optional side tool. The primary
    frontend path is `Overview / Moments / Repair / Patterns`, while
    `Moves / Reference` keep the raw move tree, explorer, board settings, and
    PGN/FEN import as secondary surfaces.
  - `OpeningExplorerClient` now prefers configured
    `explorer.internal_endpoint` / `explorer.endpoint` values before falling
    back to env/default upstreams, so commentary explorer lookups no longer
    depend on a single hard-coded runtime endpoint.
  - `AnalyseCtrl` owns explicit review UI state
    (`primaryTab`, `referenceTab`, `momentFilter`,
    `selectedMomentPly`, `selectedCollapseId`) and only the free-analysis
    surface uses it. `replay` / `study` continue to use the legacy stacked tool
    model.
  - `NarrativeCtrl.enabled` is no longer used as the visibility source of truth
    on free analysis. The review shell is always present, and narrative fetches
    are now triggered from `Overview` / `openNarrative()` while
    `Moments / Repair / Patterns` reuse the same narrative, collapse, and DNA
    payloads.
  - the former underboard PGN/FEN import flow has moved into
    `Reference > Import`, so the right-hand tools column is now the single
    consumption surface for review, reference, and raw analysis on `/analysis`.
  - review-shell polish now persists tab / filter / selected review targets for
    the session and keeps the active tab or selected moment / collapse card in
    view while the board is navigated from the control bar.
- Verification:
  - `ui/analyse/src/ctrl.ts:940`
  - `ui/analyse/src/ctrl.ts:952`
  - `ui/analyse/src/ctrl.ts:1114`
  - `ui/analyse/src/ctrl.ts:1130`
  - `ui/analyse/src/ctrl.ts:1144`
  - `ui/analyse/src/ctrl.ts:950`
  - `ui/analyse/src/ctrl.ts:991`
  - `ui/analyse/src/ctrl.ts:1082`
  - `ui/analyse/src/view/components.ts:93`
  - `ui/analyse/src/view/components.ts:222`
  - `ui/analyse/src/view/components.ts:294`
  - `ui/analyse/src/view/components.ts:306`
  - `ui/analyse/src/view/controls.ts:21`
  - `ui/analyse/src/view/actionMenu.ts:18`
  - `ui/analyse/src/explorer/explorerView.ts:286`
  - `ui/analyse/src/review/view.ts:56`
  - `ui/analyse/src/review/view.ts:387`
  - `ui/analyse/src/review/view.ts:111`
  - `ui/analyse/src/review/view.ts:205`
  - `ui/analyse/src/review/view.ts:245`
  - `ui/analyse/src/review/view.ts:285`
  - `ui/analyse/src/review/view.ts:312`
  - `ui/analyse/src/narrative/narrativeView.ts:208`
  - `ui/analyse/src/narrative/narrativeView.ts:225`
  - `ui/analyse/src/narrative/narrativeView.ts:290`
  - `ui/analyse/src/narrative/narrativeView.ts:421`
  - `ui/analyse/src/narrative/narrativeView.ts:461`
  - `ui/analyse/src/narrative/narrativeView.ts:821`

## Findings

### 1. DecisionRationale / MetaSignals / StrategicFlow / OpponentPlan

- Verdict: `Partially utilized`
- Why this is a problem:
  - The primary runtime prose path now consumes these signals in `Context` and
    `DecisionPoint`, but they are still not exposed as structured response
    fields and they still do not directly reach the frontend as distinct data.
  - `MetaSignals.targets` remains mostly indirect through `DecisionRationale`
    rather than being rendered explicitly.
- Producer:
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:142`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:147`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:152`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:156`
- Carrier / model:
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:228`
  - `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala:47`
  - `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala:54`
  - `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala:57`
- Real consumption / non-consumption:
  - Primary renderer entry:
    - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala:48`
  - Primary outline now consumes these signals directly:
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:603`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:608`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:613`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:637`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:667`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:726`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:759`
  - Bookmaker and full-game both benefit because both render through the same
    `BookStyleRenderer.render` path:
    - `modules/llm/src/main/LlmApi.scala:2350`
    - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:540`
  - Structured response models still do not expose these fields:
    - `modules/llm/src/main/scala/lila/llm/models.scala:174`
    - `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala:47`
- User-facing impact:
  - "Why this move?", "what was the opponent trying to do?", and "why was the
    alternative rejected?" now show up in final prose much more reliably.
  - Remaining loss is mainly structured/API visibility, not primary prose
    omission.
- Fix difficulty / expected return:
  - Difficulty: completed in working tree for primary prose
  - Return: high, with remaining follow-up in API/frontend exposure
- Verification:
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeContextBuilderTest.scala:290`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeContextBuilderTest.scala:440`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeContextBuilderTest.scala:1234`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeContextBuilderTest.scala:1263`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeSignalConsumptionTest.scala:34`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeSignalConsumptionTest.scala:92`

### 2. Strategic distribution / latent-plan / opening / practical wrap-up on full-game path

- Verdict: `Underutilized`
- Why this is a problem:
  - These beats reach the outline, but late paragraphs are clipped on the
    full-game path, so final narrative under-consumes them.
- Producer:
  - beat order:
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:121`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:139`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:146`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:149`
  - detailed beat builders:
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:153`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:999`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:1629`
- Carrier / renderer:
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:540`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala:50`
- Real consumption / non-consumption:
  - full-game truncation:
    - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:675`
    - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:797`
  - Bookmaker keeps rendered body:
    - `modules/llm/src/main/LlmApi.scala:2350`
- User-facing impact:
  - full-game narrative frequently loses opening theory, strategic stack,
    practical verdict, and compensation specificity.
- Fix difficulty / expected return:
  - Difficulty: low to medium
  - Return: very high
- Confidence note:
  - Strongly supported by beat order and clipping rules.
  - Exact production loss frequency was not batch-measured during the audit.

### 3. Probe / authoring / plan partition

- Verdict: `Partially utilized`
- Why this is a problem:
  - Backend now ships `probeRequests`, `authorQuestions`, `authorEvidence`,
    `mainStrategicPlans`, `latentPlans`, and `whyAbsentFromTopMultiPV`, but
    the frontend still does not render the authoring/evidence summaries as a
    dedicated visible block.
  - Full-game path passes `probeResults = Nil`, weakening latent-plan and
    author-evidence consumption.
- Producer:
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:348`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:355`
  - `modules/llm/src/main/scala/lila/llm/analysis/AuthorQuestionGenerator.scala:11`
  - `modules/llm/src/main/scala/lila/llm/analysis/AuthorEvidenceBuilder.scala:13`
- Carrier / model / API:
  - `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala:41`
  - `modules/llm/src/main/LlmApi.scala:2391`
  - `modules/llm/src/main/scala/lila/llm/analysis/AuthoringEvidenceSummaryBuilder.scala:1`
  - `modules/llm/src/main/scala/lila/llm/model/FullGameNarrative.scala:18`
  - `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala:43`
  - `modules/llm/src/main/scala/lila/llm/models.scala:199`
  - `app/controllers/LlmController.scala:182`
- Real consumption / non-consumption:
  - latent plan beat requires author evidence:
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:718`
  - latent beat can be dropped by validator:
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineValidator.scala:121`
  - full-game passes empty probe list:
    - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:544`
  - Bookmaker and full-game payloads now carry structured authoring summaries:
    - `modules/llm/src/main/LlmApi.scala:2387`
    - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:569`
  - Bookmaker payload parsing:
    - `ui/analyse/src/bookmaker/responsePayload.ts:100`
  - Bookmaker UI uses counts / attrs rather than visible rendering:
    - `ui/analyse/src/bookmaker.ts:482`
    - `ui/analyse/src/bookmaker.ts:490`
    - `ui/analyse/src/bookmaker.ts:558`
- User-facing impact:
  - plan partitioning and evidence summaries now survive into the API layer,
    but users still do not see the actual author-question / branch-proof
    objects directly in the interface.
  - There is strong full-game vs Bookmaker skew.
- Fix difficulty / expected return:
  - Difficulty: medium
  - Return: very high
- Supporting tests:
  - `modules/llm/src/test/scala/lila/llm/analysis/PlanProposalEngineTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/PlanEvidenceEvaluatorTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ProbeDetectorTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/AuthoringEvidenceSummaryBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategyPackBuilderTest.scala`

### 4. StructureProfile / PlanAlignment / Prophylaxis / Practicality / Compensation

- Verdict: `Distorted`
- Why this is a problem:
  - These modules produce rich structured semantics, but final prose collapses
    them into broad hints, single-line lexicon calls, or generic tone changes.
- Producer:
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:294`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:330`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicFeatureExtractorImpl.scala:172`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicFeatureExtractorImpl.scala:193`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicFeatureExtractorImpl.scala:204`
- Carrier / model:
  - `modules/llm/src/main/scala/lila/llm/model/ExtendedAnalysisData.scala:44`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:2028`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:2031`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:2032`
- Real consumption / non-consumption:
  - alignment reduced to summary / flow hints:
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:307`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:538`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala:1060`
  - renderer uses alignment mainly as fallback anchor:
    - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala:151`
  - prophylaxis reduced to one statement:
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:974`
  - practical / compensation reduced in wrap-up:
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:1641`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala:1645`
- User-facing impact:
  - expensive theory modules feel "technically present but narratively generic."
- Fix difficulty / expected return:
  - Difficulty: medium
  - Return: high
- Testing caveat:
  - coverage is more quality-runner-heavy than user-facing prose-golden-heavy:
    - `modules/llm/src/test/scala/lila/llm/tools/PawnStructureQualityRunner.scala`

### 5. StrategyPackBuilder

- Verdict: `Unused/Dead` on Bookmaker, `Underutilized` overall
- Why this is a problem:
  - Bookmaker only builds `strategyPack` when `llmLevel == Active`, but the
    controller hardcodes `Polish`.
  - Full-game API can carry the pack, but the frontend type layer ignores it.
- Producer:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala:18`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:566`
- Carrier / API:
  - `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala:69`
  - `app/controllers/LlmController.scala:191`
- Real consumption / non-consumption:
  - Bookmaker build gate:
    - `modules/llm/src/main/LlmApi.scala:2363`
  - Controller hardcodes `Polish`:
    - `app/controllers/LlmController.scala:263`
  - Bookmaker payload type has no `strategyPack`:
    - `ui/analyse/src/bookmaker/responsePayload.ts:59`
  - Game narrative UI type has no `strategyPack`:
    - `ui/analyse/src/narrative/narrativeCtrl.ts:40`
- User-facing impact:
  - `strategyPack` exists mostly as backend-internal or test-time material.
- Fix difficulty / expected return:
  - Difficulty: low to medium
  - Return: medium
- Supporting tests:
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategyPackBuilderTest.scala:9`
  - `modules/llm/src/test/scala/lila/llm/ActiveStrategicPromptTest.scala:59`

## Coverage Matrix

| Module | Produces | Stored in model/context | Reaches API | Reaches frontend | Used in outline | Used in final prose | Tests exist | Overall verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Threat / ThreatExtractor | threats, urgency, threat concepts | Yes | Indirect | Prose only | Yes | Yes | Unclear | Fully utilized |
| Break / pawnPlay | break file, impact, tension | Yes | Indirect | Prose only | Yes | Yes | Unclear | Fully utilized |
| Endgame / Oracle / State | endgame patterns, transition, token | Yes | Full-game + BM | BM token + prose | Yes | Yes | Yes | Fully utilized |
| Opening explorer / event | opening ref, event, precedents | Yes | Full-game + BM indirect | Prose only | Yes | Yes, but game clipped | Partial | Partially utilized |
| Counterfactual | cpLoss, causality, teaching motif | Yes | Indirect | Prose only | Yes | Yes | Unclear | Fully utilized |
| Plan proposal / hypothesis / evaluator | main plans, latent plans, hold reasons | Yes | BM explicit, full-game weak | BM count-only, full-game no | Yes | BM yes, full-game weak | Yes | Partially utilized |
| Probe / authoring evidence | probe requests, questions, evidence | Yes | Full-game + BM explicit | Partial: probe summary only, no author-evidence render | Yes, gated | Weak to moderate | Yes | Partially utilized |
| Decision / meta / flow / opponent intent | rationale, divergence, opponent plan | Yes | No explicit | No | Yes | Yes | Yes | Partially utilized |
| Structure profile / alignment | structure schema, plan fit | Yes | Indirect | Prose only | Weak | Generic only | Tools / partial | Distorted |
| Prophylaxis | prevented plan objects | Yes | Indirect | Prose only | Weak | One-line only | Unclear | Distorted |
| Practicality / compensation | verdict, conversion, material story | Yes | Indirect | Prose only | Yes | Weak / often clipped | Unclear | Partially utilized |
| StrategyPack | structured strategic bundle | Yes | Full-game + BM field | No | No | No direct prose | Yes | Unused/Dead on BM |
| CausalCollapseAnalyzer | collapse analysis | Moment only | Full-game only | Full-game only | No | No direct prose | Yes | Partially utilized |

## Priority Recommendations

### 1. Reduce or guard full-game clipping in `focusMomentBody`

- Why first:
  - it likely removes already-built high-value beats
- Minimum edit sites:
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:675`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:790`
- Test method:
  - fixture containing opening + strategic stack + practical wrap-up
  - assert mandatory beats survive paragraph reduction

### 2. Render plan / authoring payloads in frontend instead of only counting them

- Why first:
  - Bookmaker already pays the cost of generating these objects
  - UI leaves most of their value unused
- Minimum edit sites:
  - `app/controllers/LlmController.scala`
  - `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/src/narrative/narrativeCtrl.ts`
- Test method:
  - payload round-trip
  - frontend state / render assertions

### 3. Split structure / prophylaxis / practicality into richer outline beats or templates

- Why first:
  - these are theory-rich modules whose prose impact is disproportionately weak
- Minimum edit sites:
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeLexicon.scala`
- Test method:
  - structure fixture should expose `centerState`, `reasonCodes`, or
    `preventedThreatType` in final prose

### 4. Expose decision / meta / opponent / flow structurally to API and active prompts

- Why first:
  - the primary prose fix is in place, but downstream consumers still cannot
    access these signals as structured fields
  - `active` mode still depends more on `strategyPack` than on these direct
    narrative fields
- Minimum edit sites:
  - `modules/llm/src/main/scala/lila/llm/models.scala`
  - `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala`
- Test method:
  - response schema snapshots
  - active prompt fixture asserting decision/meta/opponent hints are present

### 5. Decide whether `strategyPack` should live or be removed

- Why first:
  - current Bookmaker path is effectively dead-wired
- Minimum edit sites if kept:
  - `app/controllers/LlmController.scala:263`
  - `ui/analyse/src/bookmaker/responsePayload.ts`
  - `ui/analyse/src/narrative/narrativeCtrl.ts`
- Test method:
  - non-empty pack generation
  - visible UI consumption

## Gaps and Ambiguities

- The frequency of full-game clipping loss was not batch-measured; the finding
  is strongly inferred from beat order and clipping rules.
- `maybePolishCommentary` may make prose more generic after deterministic
  rendering. This audit focused on deterministic carrier and consumption paths,
  not model-output sampling quality.
- `data-llm-*` root attributes may still be consumed by CSS or analytics outside
  the audited `ui/analyse/src` surface. No visible rendering path was found in
  the audited tree.

## 2026-03-09 Update

- `chosen / engine best / deferred / why / evidence` comparison semantics are
  now normalized through a shared backend carrier rather than being inferred
  separately by Bookmaker prose, full-game signal rows, and Active prompt
  shaping.
- `DecisionComparisonBuilder` now feeds
  `NarrativeSignalDigest.decisionComparison`, `StrategyPack` prompt/evidence,
  and `ActiveStrategicPrompt`.
- frontend comparison consumption now uses the normalized digest in both
  Bookmaker and full-game shells; full-game also synthesizes a fallback
  comparison from `topEngineMove` when the normalized digest is absent, so
  ad hoc `why not top line` / `Why Not?` cards are no longer a separate
  primary comparison surface.
- current frontend rendering keeps the same normalized semantics but now splits
  density by surface:
  - Bookmaker renders a compact compare strip above prose
  - full-game renders an expandable compare card inside the signal shell
  - both remain UI-owned and do not parse LLM prose for comparison structure
- current interaction layer also treats comparison and evidence key moves as
  structured UI chips rather than prose fragments:
  - Bookmaker compare moves and authoring-evidence branch moves reuse existing
    ref-backed preview / move-chip interaction
  - full-game compare moves and evidence branch keys reuse the existing
    preview-only `data-board` hover surface derived from moment variations

Verification:
- `modules/llm/src/main/scala/lila/llm/analysis/DecisionComparisonBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeEvidenceHooks.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeSignalDigestBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala`
- `modules/llm/src/main/scala/lila/llm/models.scala`
- `ui/analyse/src/decisionComparison.ts`
- `ui/analyse/src/bookmaker/responsePayload.ts`
- `ui/analyse/src/bookmaker.ts`
- `ui/analyse/src/narrative/narrativeView.ts`

## 2026-03-09 Active Dossier Update

- Active premium notes are no longer treated as a raw `strategyPack` dump with
  extra prose. The runtime now deterministically synthesizes an
  `ActiveBranchDossier` from existing shipped signals:
  - `StrategicThesis`
  - `DecisionComparisonDigest`
  - `StructurePlanArc`
  - `StrategyPack`
  - `NarrativeSignalDigest`
  - authoring / probe evidence
  - route refs / move refs
- The dossier is attached only on the existing `full-game + Pro + Active` path.
  Bookmaker remains unchanged.
- Active prompt construction now prefers an explicit dossier block containing:
  - chosen branch
  - engine / deferred branch
  - why chosen / why deferred
  - opponent resource
  - route cue
  - move cue
  - evidence cue
  - continuation / practical risk
- Active validation and ops now measure dossier-aware behavior rather than only
  sentence count + strategy coverage:
  - branch dossier presence
  - compare presence
  - deferred branch hits when present
  - opponent resource hits when present
  - route / move reference citation hits
- Frontend full-game premium rendering now keeps the existing `Strategic Note`
  prose box but adds a compact secondary `Branch Dossier` summary under it.
  This surface is UI-owned and uses structured payload, not LLM prose parsing.

Verification:
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveBranchDossierBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/main/scala/lila/llm/models.scala`
- `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala`
- `modules/llm/src/main/scala/lila/llm/model/FullGameNarrative.scala`
- `ui/analyse/src/narrative/narrativeCtrl.ts`
- `ui/analyse/src/narrative/narrativeView.ts`
- `ui/analyse/css/_narrative.scss`

## 2026-03-10 Active Independence Update

- Active strategic notes no longer receive the prior moment prose as a primary
  generation block. `ActiveStrategicPrompt.buildPrompt` now starts from moment
  context + structured evidence (`Active Dossier`, `Strategy Pack`, route refs,
  move refs) and explicitly asks for an independent strategic thesis instead of
  preserving the prior wording.
- Repair prompts still receive the prior note, but only as a contradiction
  guard. The prompt now explicitly tells the model not to mirror its wording or
  sentence structure.
- `ActiveBranchDossierBuilder` no longer backfills dossier cues from prior prose
  when structured evidence is absent:
  - `chosenBranchLabel` no longer falls back to the first sentence of
    `moment.narrative`
  - `whyChosen` no longer falls back to tactical narrative prose
  - `evidenceCue` no longer falls back to `authorEvidence.summary.question`
- Active validation now includes an independence guard:
  - `ActiveNoteIndependenceGuard` rejects direct reuse of long normalized prior
    phrases or long shared sentence openings
  - `LlmApi.validateActiveStrategicNote` applies this guard against the prior
    `moment.narrative` before accepting an Active note
- Resulting behavior: Active notes are still grounded by shipped strategic
  signals, but they are now intended to synthesize a fresh long-term strategic
  idea from those signals instead of paraphrasing the base narrative.

Verification:
- `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveBranchDossierBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveNoteIndependenceGuard.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/test/scala/lila/llm/ActiveStrategicPromptTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveBranchDossierBuilderTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveNoteIndependenceGuardTest.scala`

## 2026-03-11 Bookmaker Ledger Update

- Bookmaker now emits a compact `bookmakerLedger` in parallel to prose and
  `signalDigest`.
- This ledger is intentionally outside the
  `thesis -> outline -> validator -> prose` compression path.
- The ledger builder is evidence-gated:
  - it may return `None`
  - weak fallback motif/stage rows are not rendered by default
- Builder input is structured runtime state and normalized backend carriers:
  - `NarrativeContext`
  - `StrategyPack`
  - `StrategicThesisBuilder` output
  - `NarrativeSignalDigestBuilder` output
  - `DecisionComparisonBuilder` output
  - Bookmaker refs / variation previews
  - probe results
  - continuity tokens
- It does not read outline slots, validated outline, polished prose, or parse
  free-form `whyAbsent` / prompt text back into ledger fields.
- Builder output is compact and fixed-shape:
  - dominant motif
  - stage / carry-over / stage reason
  - prerequisites / conversion trigger
  - `primaryLine` / `resourceLine`
- Dominant motif precedence is now intentionally concrete-first:
  - `whole_board_play` is no longer a Bookmaker dominant motif
  - cross-board coordination remains a `Flow` / stage-reason concern
  - `opposite_bishops_conversion` is allowed only when opposite-colored bishops
    evidence aligns with an advantage-transformation plan family and a real
    conversion state
- Frontend consumption stays inside existing Bookmaker surfaces only:
  - `Strategic Signals` gets `Motif`, `Stage`, `Carry-over`, `Prereqs`,
    `Conversion`
  - `Evidence Probes` gets `Plan line`, `Counter-resource`
- Session/study snapshot payloads now persist:
  - `bookmakerLedger`
  - `planStateToken`
  - `endgameStateToken`
- token restore context (`stateKey`, `analysisFen`, `originPath`)
- Restore rehydrates those tokens only when the stored token context matches
  the current Bookmaker analysis state, so stale study snapshots do not inject
  continuity state into a different board context.

Verification:
- `modules/llm/src/main/scala/lila/llm/models.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilder.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilderTest.scala`
- `ui/analyse/src/bookmaker/responsePayload.ts`
- `ui/analyse/src/bookmaker/ledgerSurface.ts`
- `ui/analyse/src/bookmaker/surfaceShared.ts`
- `ui/analyse/src/bookmaker/stateContinuity.ts`
- `ui/analyse/src/bookmaker/studyPersistence.ts`
- `ui/analyse/src/bookmaker.ts`
- `ui/analyse/tests/bookmakerLedgerSurface.test.ts`
- `ui/analyse/tests/bookmakerPersistence.test.ts`

## 2026-03-11 Active Full-PGN Narrative (Current)

- Current external contract:
  - `GameNarrativeResponse.schema = chesstory.gameNarrative.v5`
  - top-level `strategicThreads`
  - per-moment `strategicThread`
  - public `review` count fields:
    - `internalMomentCount`
    - `visibleMomentCount`
    - `polishedMomentCount`
    - `visibleStrategicMomentCount`
    - `visibleBridgeMomentCount`
- Historical note:
  - `v3` introduced explicit cross-moment campaign metadata
  - `v4` introduced bridge-aware widening
  - both are intermediate steps only; `v5` is the current sparse-response API
- Thread detection is deterministic and runs after full-game rule/polish
  assembly, not inside the low-level analyzers:
  - `ActiveThemeSurfaceBuilder` promotes shipped signals into a user-facing
    theme surface
  - `ActiveStrategicThreadBuilder` groups moments by side + theme + continuity
  - `StrategicBranchSelector.buildSelection` is now the canonical visible
    projection step
- Canonical thread themes currently surfaced by Active:
  - `whole_board_play`
  - `opposite_bishops_conversion`
  - `active_passive_exchange`
  - `rook_lift_attack`
  - `minority_attack`
  - `outpost_entrenchment`
  - `rook_pawn_march`
  - `invasion_transition`
  - `prophylactic_restriction`
  - `compensation_attack`
- Full-game pipeline is now explicitly two-layer:
  - internal coverage = anchor moments + opening events + bridge widening result
  - visible narrative = sparse projection only
  - hidden/internal moment arrays are not exposed through the public API
- Internal widening still uses the bridge-aware two-pass flow:
  - `anchor moment -> preliminary thread ranking -> candidate bridge analysis ->
    selected bridge re-analysis`
  - only top `3` strategic threads are widened
  - planner candidate window = `seedPly - 4 .. lastPly + 4`
  - candidate cap = `14` per thread, `36` total
- Visible sparse projection is fixed:
  - top `3` threads only
  - up to `3` representative moments per thread
  - bridge moments are visible only when they occupy a representative stage slot
  - non-thread filler priority = `Blunder` -> `MissedWin` -> `MatePivot` ->
    `OpeningBranchPoint / OpeningOutOfBook / OpeningTheoryEnds / OpeningNovelty`
  - `OpeningIntro` is not used as a visible filler slot
  - visible cap = `12`
- Surface metadata:
  - opening-derived moments use `selectionKind = opening`,
    `selectionLabel = Opening Event`
  - bridge-derived moments use `momentType = StrategicBridge`,
    `selectionKind = thread_bridge`, `selectionLabel = Campaign Bridge`
  - `selectionReason` explains why a visible bridge was projected
- Polish and Active-note coverage are narrower than visible coverage:
  - intro/conclusion remain separate polish targets
  - moment polish cap = `10`
  - polish priority = visible thread representatives -> visible blunder/missed
    win/mate pivots -> visible opening branch events
  - visible strategic representatives keep `strategicThread`
  - Active note subset keeps `strategicBranch = true`
  - hidden/internal moments never receive Active notes
  - Active note cap remains `8`
- Thread metadata is still attached even when Active note generation is gated
  off, so rule-path/fullgame rendering can surface campaign structure without
  requiring `Pro + Active`.
- Active note is now treated as a forward-looking strategic coaching layer,
  not as a structured-evidence recap:
  - carrier/model stays the same (`activeStrategicNote: String`) but the
    prompt contract is now `next plan + why now + opponent reply/trigger`
  - the prompt is built from a shared `coaching brief` derived from
    `strategyPack + dossier + route/move refs`
  - raw `Campaign Thread`, `Strategy Pack`, `Dossier`, and reference dumps are
    not sent to the model anymore; only the compressed coaching brief is
    serialized
  - generated notes must stay short (`2-3` sentences, usually `50-90` words)
    and explicitly describe a follow-up plan rather than merely restating the
    current position
  - active-note prompt now also carries a deterministic `opening lens`
    (`timing-first`, `problem-first`, `consequence-first`,
    `target-purpose-first`, `campaign-role-first`, or `plan-first`) derived from the same coaching
    brief plus moment context, so the model is pushed away from repeating the
    same bare imperative opener on every note
  - active-note prose shaping is now explicitly closer to human strategy-book
    explanation patterns:
    - first sentence should usually open from situation/timing/problem/
      consequence rather than a bare command
    - the note should prefer `why now -> plan` ordering when the position
      offers a clear rationale
    - opponent replies/triggers should be folded into dependent clauses when
      natural, rather than isolated warning sentences
    - route language in the prompt is lowered from raw square chains to target
      square / purpose phrasing (`knight toward e3 for kingside clamp`) unless
      some other path chooses to surface exact deployment text
    - mild hedge / measured coaching language is preferred over hard imperative
      prose
- Benchmark/tooling path diverges from product runtime only at the polish
  circuit breaker:
- `analyzeFullGameLocal(... disablePolishCircuit = true)` is benchmark-only
  - runtime product path still honors `rule_circuit_open`
- Prompt/runtime shaping updates in the current working tree:
  - `PolishPrompt` now emits stable `REQUEST -> CONTEXT -> DRAFT/SLOTS` ordering
    so provider-side prefix caching has a larger shared prefix without removing
    any factual context fields
  - `PolishPrompt` and `ActiveStrategicPrompt` now omit empty/default context
    rows such as blank `FEN`, `Opening: unknown`, empty `Concepts`, and default
    salience rows, rather than serializing placeholder values
  - `ActiveStrategicPrompt` no longer repeats thread label/stage/summary fields
    inside both `CAMPAIGN THREAD` and `ACTIVE DOSSIER`; thread metadata lives in
    `CAMPAIGN THREAD`, while dossier keeps branch-specific cues
  - `ActiveStrategicPrompt` now slims `Strategy Pack` prompt surface without
    changing the shipped carrier/model:
    - `Signal Digest` no longer serializes `preservedSignals`
    - structural/deployment summaries take precedence over lower-level raw
      support rows when both exist
    - `DecisionComparison` prompt rows are limited to chosen/engine-best/
      deferred move + deferred reason
    - `Evidence` is capped to the first rendered line instead of dumping the
      full list
  - Active-note validation is now coaching-contract based:
    - hard fails are limited to empty/unparsed/truncated/leaked output,
      prior-phrase reuse, `strategy_coverage_low`, and the new
      `forward_plan_missing`
    - sentence-count, semantic route/move reference misses, and dossier
      compare/deferred/opponent gaps are warnings only
    - route/move reference checks accept semantic mentions such as SAN/UCI,
      ordered route squares, or piece+square cues rather than exact labels only
    - prompt + validator share the same `ActiveStrategicCoachingBriefBuilder`
      coverage helper so forward-looking coverage is checked against the same
      normalized brief that shaped the prompt
    - active-note repair runs only when hard-fail reasons are present
  - Active-note prompt shaping now naturalizes internal metadata before it ever
    reaches the model:
    - stage labels such as `Seed/Build/Switch/Convert` are rendered as
      human-role descriptions instead of title-case tags
    - route references are rendered as player-facing route summaries
      (`white knight toward e3 for kingside clamp`, or an exact chain only when
      the route already cleared the `exact` surface gate) rather than `route_1`
    - move references are rendered as player-facing move descriptions
      (`engine-preferred move Nf1 (d2-f1)`) rather than literal move labels or
      raw UCI
    - prompt instructions explicitly ban exposing internal ids / labels / UCI
      and push shorter notes with more varied, concrete sentence openings
  - Active-note provider/model routing is split from fullgame polish routing:
    - global polish/segment path still follows `LLM_PROVIDER` and the standard
      `OPENAI_MODEL_*` / `GEMINI_MODEL` settings
    - Active note path resolves `LLM_PROVIDER_ACTIVE_NOTE` first and falls back
      to the global provider if unset
    - OpenAI active-note-only envs:
      `OPENAI_MODEL_ACTIVE_SYNC`, `OPENAI_MODEL_ACTIVE_ASYNC`,
      `OPENAI_MODEL_ACTIVE_FALLBACK`, `OPENAI_REASONING_EFFORT_ACTIVE`
    - Gemini active-note-only env:
      `GEMINI_MODEL_ACTIVE`
    - code default OpenAI active-note route remains `gpt-5.2` with
      `reasoning_effort = none`; global/base polish can still remain on
      `gpt-5-mini`
    - current compose/runtime default in `lila-docker/settings.env` is:
      - global polish / Bookmaker on OpenAI `gpt-5-mini`
      - Active note on Gemini `gemini-3-flash-preview`
      - Gemini active-note path remains uncached in runtime, because
        `GeminiClient.activeStrategicNote` calls `callWithSystemPrompt` with
        `allowContextCache = false`
      - when Active note primary provider is Gemini and OpenAI is enabled,
        `LlmApi` now retries the note through the OpenAI active-note route on
        Gemini empty / invalid / repair-failed outcomes; telemetry exposes the
        OpenAI primary model as the configured fallback model for that Gemini
        route
    - `OpenAiClient` now treats `gpt-5.2*` / `gpt-5.4*` as modern GPT-5 routes
      and never sends legacy `reasoning_effort = minimal` to them
  - segment polish is now narrower:
    - intro/conclusion moments do not enter the segment path
    - prose must be at least `80` words
    - the segment attempt exits immediately if no editable segment survives
      structural-token masking
    - candidate editable segments now also need enough prose on their own:
      - at least `12` total words
      - at least `8` prose words
      - lock-bearing segments need at least `10` prose words
      - lock-dense segments (`protectedTokenCount / totalWords > 0.28`) are
        skipped before segment polish
    - max rewritten segments = `2` async / `1` sync
    - segment repair is now reason-aware: structural failures such as missing
      lock anchors, wrapper leakage, truncation, or length-ratio damage bypass
      the LLM repair pass and fall straight back to the original segment
    - the common `san_order_violation + count_budget_exceeded` pair is now also
      treated as low-yield for segment repair, so the runtime keeps the original
      segment instead of paying for a repair that historically fails the same
      structural checks again
    - merged segment output now gets a deterministic soft-repair pass before
      whole-prose fallback: invalid rewrites are greedily reverted when that
      restores SAN / marker validity without discarding the entire segment path
    - merged structural failures can now skip the old whole-prose retry path;
      the runtime keeps the original prose instead of paying for a low-yield
      second rewrite attempt
  - internal-only commentary ops snapshot now exposes per-family prompt-usage
    aggregates (`promptUsage`) across polish / repair / segment / active-note
    calls, including attempts, cached hits, prompt tokens, completion tokens,
    and estimated cost
  - full-game ops telemetry additionally exposes:
    - `repairAttempts`
    - `repairBypassed`
    - `softRepairApplied`
    - `mergedRetrySkipped`
    - `invalidReasonCounts`
    - sampled failure classes:
      `fullgame_polish.invalid`, `fullgame_segment_repair_bypassed`,
      `fullgame_segment_soft_repair`, `fullgame_segment_retry_skipped`
  - active-note ops telemetry additionally exposes:
    - `provider`
    - `configuredModel`
    - `fallbackModel`
    - `reasoningEffort`
    - `observedModelDistribution`
    - `primaryAccepted`
    - `repairAttempts`
    - `repairRecovered`
    - `warningReasons`
    - `route_redeploy_count`
    - `route_move_ref_count`
    - `route_hidden_safety_count`
    - `route_toward_only_count`
    - `route_exact_surface_count`
    - `route_opponent_hidden_count`
  - full-response cache keying now includes the resolved Active-note route
    fingerprint (`provider/model/fallback/effort`) so changing the
    active-note-only provider/model does not reuse stale cached fullgame
    responses
- Frontend consumes only the sparse public response:
  - no hidden/internal debug panel
  - thread summaries, theme/stage badges, and `Campaign Bridge` badges render
    from visible moments only
  - review surfaces summarize internal-vs-visible-vs-polished counts

Verification:
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveThemeSurfaceBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicThreadBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveBridgeMomentPlanner.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicBranchSelector.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveBranchDossierBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala`
- `modules/llm/src/main/scala/lila/llm/model/FullGameNarrative.scala`
- `modules/llm/src/main/scala/lila/llm/models.scala`
- `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveThemeSurfaceBuilderTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicThreadBuilderTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveBridgeMomentPlannerTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/StrategicBranchSelectorTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveBranchDossierBuilderTest.scala`
- `modules/llm/src/test/scala/lila/llm/ActiveStrategicPromptTest.scala`
- `modules/llm/src/test/scala/lila/llm/tools/ActiveNarrativeCorpusRunner.scala`
- `modules/llm/src/test/scala/lila/llm/tools/ActiveNarrativeCorpusSupport.scala`
- `modules/llm/src/test/scala/lila/llm/tools/ActiveNarrativeCorpusToolsTest.scala`
- `ui/analyse/src/narrative/narrativeCtrl.ts`
- `ui/analyse/src/narrative/narrativeView.ts`
- `ui/analyse/css/_narrative.scss`
- `ui/analyse/tests/narrativeView.test.ts`

## 2026-03-12 Active Idea-Centered Note Redesign

- Public contract bump:
  - `StrategyPack.schema = chesstory.strategyPack.v2`
  - `GameNarrativeResponse.schema = chesstory.gameNarrative.v6`
- Active note is now explicitly idea-led rather than route-led.
- New backend/public carriers now ride alongside the existing route/move refs:
  - `StrategyDirectionalTarget`
  - `StrategyIdeaSignal`
  - `ActiveStrategicIdeaRef`
- `StrategyPack` now preserves three distinct execution buckets without relaxing
  route hardening:
  - `pieceRoutes`: currently realistic redeployment only
  - `directionalTargets`: empty strategic squares worth working toward, but not
    yet route-quality
  - `pieceMoveRefs`: capture / exchange / tactical entry squares only
- Directional targets are derived only from empty squares; enemy-occupied
  squares remain excluded and stay in move-ref/tactical handling.
- A new deterministic `StrategicIdeaSelector` now runs on top of the built
  `StrategyPack` before Active-note shaping:
  - precedence: explicit plan/digest/prophylaxis/structure cues ->
    directional targets -> routes -> move refs / counterplay cues
  - emits exactly one dominant idea and at most one secondary idea
  - secondary idea is allowed only within the documented score window and from
    a different `StrategicIdeaGroup`
- `NarrativeSignalDigest` now carries summary-only idea fields:
  - `dominantIdeaKind`, `dominantIdeaGroup`, `dominantIdeaReadiness`,
    `dominantIdeaFocus`
  - `secondaryIdeaKind`, `secondaryIdeaGroup`, `secondaryIdeaFocus`
- Full-game moment payload now also carries Active-note-specific structured
  surfaces:
  - `activeStrategicIdeas`
  - `activeDirectionalTargets`
  - existing `activeStrategicRoutes` and `activeStrategicMoves` remain present
- `ActiveStrategicCoachingBriefBuilder` contract is now:
  - `campaignRole`
  - `primaryIdea`
  - `whyNow`
  - `opponentReply`
  - `executionHint`
  - `longTermObjective`
  - `keyTrigger`
  - Prompt/validation behavior changed accordingly:
    - Active prompt centers the dominant idea and allows at most one support from
      `executionHint` or `longTermObjective`
    - when shipped move evidence shows an immediate tactical or material gain,
      the coaching brief now promotes that fact into `whyNow`, and the active
      prompt explicitly asks for that concrete gain to appear in the first
      sentence before the note widens back out to the strategic plan
    - occupied friendly squares are no longer naively rephrased as if they still
      need to be newly occupied; the coaching-brief layer only rewrites
      obviously occupancy-shaped prompts such as `focus on c3` into
      `keep the knight anchored on c3`, while genuine square-control language is
      preserved
    - route wording is explicitly supporting evidence, not the thesis sentence
    - validator no longer expects generic route/move mention by default
    - hard requirements are now:
    - grounded dominant idea
    - forward plan
    - opponent resource or failure trigger
- Frontend Active-note UI no longer uses a single fallback chip row as the main
  explanation surface. It now renders three explicit UI-owned surfaces:
  - `Idea`
  - `Execution`
  - `Objective`
- Rendering rules:
  - exact routes keep raw path preview
  - `toward` execution cues do not expose raw path payload
  - directional targets do not reuse route-chip styling
  - move refs stay out of the default user-facing chip row in this stream
- Minimal non-Active support added elsewhere:
  - Bookmaker / digest payload types accept the new idea summary fields
  - no new Bookmaker prose contract was introduced in this stream

Verification:
- `modules/llm/docs/ActiveNoteIdeaRedesignPlan_20260312.md`
- `modules/llm/docs/ActiveNoteIdeaRedesignPrompt_20260312.md`
- `modules/llm/src/main/scala/lila/llm/models.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/strategic/StrategicAnalyzers.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSelector.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicNoteValidator.scala`
- `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala`
- `modules/llm/src/main/scala/lila/llm/model/FullGameNarrative.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/StrategicIdeaSelectorTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/StrategyPackBuilderTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicNoteValidatorTest.scala`
- `modules/llm/src/test/scala/lila/llm/ActiveStrategicPromptTest.scala`
- `ui/analyse/src/narrative/narrativeCtrl.ts`
- `ui/analyse/src/narrative/narrativeView.ts`
- `ui/analyse/src/bookmaker/responsePayload.ts`
- `ui/analyse/css/_narrative.scss`
- `ui/analyse/tests/narrativeView.test.ts`

## 2026-03-12 Typed Semantic Strategic Idea Selector Overhaul

- No public schema bump in this step. `StrategyPack v2` and `GameNarrativeResponse v6`
  remain current.
- Active-note idea selection now has an explicit typed source registry:
  - `authoritative`: board state, `PositionFeatures`, `StrategicStateFeatures`,
    `PositionalTag`, `PieceActivity`, `WeakComplex`, `PreventedPlan`,
    `ThreatAnalysis`, `PawnPlayAnalysis`, `EndgameFeature`, motifs, and route /
    directional-target structural facts
  - `derived typed`: `PositionClassification`, `StructureProfile`,
    `PlanAlignment.reasonCodes`, and the compact `PawnPlayTable`
  - `prose-only` and non-authoritative: digest strings, `planName`,
    `longTermFocus`, `route.purpose`, directional-target reasons /
    prerequisites, and structure narrative intent / risk text
- `StrategicIdeaSelector` no longer classifies ideas from `planName`, digest prose,
  `longTermFocus`, `route.purpose`, or directional-target reasons/prerequisites.
- `StrategicIdeaSemanticContext` now bridges `IntegratedContext.classification`,
  `pawnAnalysis`, `opponentPawnAnalysis`, `threatsToUs`, `threatsToThem`,
  `structureProfile`, `planAlignment.reasonCodes`, and motifs directly into
  selector input, instead of relying on downstream prose summaries.
- The selector now emits an idea only when typed evidence exists; the generic
  fallback to `space_gain_or_restriction` is removed.
- Runtime route semantics remain unchanged:
  - hidden routes stay hidden
  - unsafe routes are not revived by idea selection
  - directional targets remain separate from routes and keep their existing
    readiness semantics
- Coverage of the 10 idea kinds is now typed:
  - `pawn_break` from pawn-break readiness / tension / file-opening consequences,
    with direct `PawnPlayAnalysis.advanceOrCapture`, `counterBreak`, and
    `tensionSquares` bridges
  - `space_gain_or_restriction` from space tags, clamp state, and mobility
    restriction, plus locked-center / space-race derived typed support
  - `target_fixing` from weak-square / color-complex / fixation signals and
    `WeakComplex` backfill
  - `line_occupation` from open-file / doubled-rook / route line-access signals
  - `outpost_creation_or_occupation` from outpost / strong-knight / entrenched
    piece signals
  - `minor_piece_imbalance_exploitation` from bishop-pair / bad-bishop /
    good-bishop / piece-count imbalance signals
  - `prophylaxis` from structured prevented-plan evidence plus `ThreatAnalysis`
    defensive/prophylaxis signals
  - `king_attack_build_up` from king-pressure / mate-net / hook / attack-lane
    signals plus typed motif bridges (`RookLift`, `Battery`, `PieceLift`,
    `Check`) and `threatsToThem`
  - `favorable_trade_or_transformation` from removing-the-defender,
    classification simplify/convert windows, plan-alignment transformation
    reason codes, and winning-transition signals
  - `counterplay_suppression` from denied-break / counterplay-drop suppression
    signals, `opponentPawnAnalysis.counterBreak`, and `ThreatAnalysis`
- The old keyword selector path still exists only as the test-only helper
  `LegacyStrategicIdeaTextClassifier` under `modules/llm/src/test`. It is not
  used in runtime ranking.
- `PreventedPlan` received additive internal-only density fields
  (`deniedResourceClass`, `deniedEntryScope`, `breakNeutralizationStrength`,
  `defensiveSufficiency`) so selector logic can separate prophylaxis from
  counterplay suppression without widening the public Active-note surface.
- Gold semantic regression for idea extraction now has a dedicated FEN-grounded
  fixture bank:
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicIdeaFenFixtures.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicIdeaSelectorFenFixtureTest.scala`
- That fixture bank does not inject typed idea evidence. It runs the real
  `FEN -> CommentaryEngine.assessExtended -> NarrativeContextBuilder ->
  StrategicIdeaSemanticContext -> StrategyPackBuilder -> StrategicIdeaSelector`
  path and asserts:
  - FEN legality and material parity
  - producer-level board/structure checks such as French `...f6` break seeds,
    bishop-pin prophylaxis, queenside clamp watches, IQP simplification bridges,
    and Hedgehog / Maroczy break-denial geometry
  - gold dominant-idea labels after fixture re-audit, where the gold label is
    the board-grounded strategic theme and not an earlier prose intention
  - prose-noise invariance, so misleading `planName` / digest / focus text does
    not change the dominant typed idea
- `2026-03-13` working-tree update:
  - full-game conditional-plan text no longer reuses raw latent templates with
    `{us}` / `{them}` / `{seed}` placeholders. Full-game draft rendering now
    interpolates side labels and seed text before prose assembly.
  - `renderHybridMomentNarrative` now passes the validated outline through a
    full-game draft normalization step before LLM polish:
    - user-facing sanitizer is applied to the draft
    - `Idea:` / `Primary route is` / `Ranked stack:` / `Signals:` /
      `Refutation/Hold:` meta labels are proseified before the draft reaches
      the full-game polish prompt
    - latent-plan `seedId` placeholders are no longer direct-translation leaks:
      known seed/subplan ids are humanized through taxonomy aliases before they
      reach prose (for example `PawnStorm_Kingside` now renders as
      `a kingside pawn storm`)
    - wrap-up preconditions and hold clauses are emitted as natural sentences
      rather than raw `Preconditions:` / `Signals:` / `Refutation/Hold:`
      fragments
    - raw evidence labels such as `theme:...`, `subplan:...`, and `seed:...`
      are translated into prose-only support clauses before the draft is
      normalized
  - full-game hybrid assembly now removes low-value repetition before polish:
    - duplicate preface/body thesis restatements are suppressed
    - low-value strategic-stack sentences such as `The strategic stack still
      favors ...`, `The leading route is ...`, and `The main signals are ...`
      are dropped from the hybrid body when they repeat the already-shipped
      dominant plan thesis
  - full-game invalid-pair telemetry now records:
    - stage (`segment_primary`, `segment_repair`, `segment_merged`,
      `fullgame_primary`, `fullgame_repair`)
    - sampled `original` / `candidate` excerpts
    - original/candidate word counts
    - length ratio
    - normalized leak hits
  - placeholder leak detection for full-game prose is now narrower:
    - natural prose like `Key theme:` is no longer treated as a raw
      placeholder leak
    - only actual raw label tokens such as `theme:piece_redeployment`,
      `subplan:...`, `support:...`, `proposal:...`, `seed:...`, or bracketed
      `[subplan:...]` patterns count as placeholder leaks
  - local 10-game rerun after fixing the probe env loading bug confirmed:
    - full-game polish is active again (`llm_polished=10/10`)
    - earlier `segment_merged:placeholder_leak_detected` samples were false
      positives caused by `Key theme:` matching the old broad `theme:` detector
    - after narrowing leak detection and adding hybrid dedup, full-game invalid
      reasons on the 10-game rerun were reduced to structural failures only:
      `segment_primary:length_ratio_out_of_bounds`,
      `segment_primary:count_budget_exceeded`, and
      `segment_primary:san_order_violation`
    - hidden segment fallback is still nonzero on that rerun, so the current
      full-game path is improved but does not yet meet a strict
      `segment original fallback = 0` target

Verification:
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSourceRegistry.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSemanticContext.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSelector.scala`
- `modules/llm/src/main/scala/lila/llm/model/strategic/StrategicModels.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/strategic/StrategicAnalyzers.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/LegacyStrategicIdeaTextClassifier.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/StrategicIdeaSelectorTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/StrategyPackBuilderTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicNoteValidatorTest.scala`
- `modules/llm/src/test/scala/lila/llm/ActiveStrategicPromptTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/StrategicIdeaFenFixtures.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/StrategicIdeaSelectorFenFixtureTest.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/FullGameDraftNormalizer.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/UserFacingSignalSanitizer.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/FullGameDraftNormalizerTest.scala`
- `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala`

## Reference Files

Primary files used in this audit:
- `app/controllers/LlmController.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineValidator.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeGenerator.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicFeatureExtractorImpl.scala`
- `modules/llm/src/main/scala/lila/llm/model/ExtendedAnalysisData.scala`
- `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala`
- `modules/llm/src/main/scala/lila/llm/models.scala`
- `modules/llm/src/main/scala/lila/llm/GameNarrativeResponse.scala`
- `modules/llm/src/main/scala/lila/llm/CommentaryCache.scala`
- `modules/llm/src/main/scala/lila/llm/model/strategic/EndgamePatternState.scala`
- `ui/analyse/src/bookmaker.ts`
- `ui/analyse/src/bookmaker/responsePayload.ts`
- `ui/analyse/src/narrative/narrativeCtrl.ts`

## Maintenance Rule

If any change modifies:
- helper-module production,
- `NarrativeContext` payload shape,
- outline construction,
- renderer behavior,
- API serialization,
- or frontend narrative / Bookmaker consumption,

then this file must be updated in the same change.
