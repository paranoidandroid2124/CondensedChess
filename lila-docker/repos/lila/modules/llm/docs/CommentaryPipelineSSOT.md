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
- how do Game Arc and Bookmaker paths differ?

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
- Game Arc path
- single-position Bookmaker path
- carrier/model layers
- outline / renderer layers
- API / response models
- frontend consumption
- continuity tokens and cache keys

Related runtime-contract doc:
- `modules/llm/docs/BookmakerProseContract.md`
- internal commentary ops metrics notes are maintained outside the public repository

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
  - maintained outside the public repository
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

### Game Arc path

Entry chain:
- `app/controllers/LlmController.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`

Primary path:
1. `LlmController.analyzeGameLocal`
2. `LlmApi.analyzeGameChronicleLocal`
3. `CommentaryEngine.generateGameArc`
4. `NarrativeContextBuilder.build`
5. `BookStyleRenderer.render`
6. `CommentaryEngine.renderHybridMomentNarrative`
7. `GameChronicleResponse.fromGameArc`
8. `ui/analyse/src/narrative/*`

Key references:
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:540`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:549`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:645`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:672`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:675`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala:790`
- `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala:47`
- `ui/analyse/src/narrative/narrativeCtrl.ts:40`

Important caveat:
- Historical audit note:
  - before remediation, Game Arc path truncated rendered prose to the first
    `3-4` paragraphs via `focusMomentBody`, which caused systematic late-beat
    loss.
- Current working-tree state:
  - Game Arc path now builds a validated outline first and preserves beats by
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

- `2026-03-20` working-tree update:
  - Bookmaker and Game Chronicle user-facing language now share a stricter
    player-language rewrite pass. The runtime contract keeps long-term plans
    and strategic ideas, but rewrites them into concrete player language tied
    to squares, files, exchanges, denied breaks, king safety, and practical
    handling rather than surfacing internal commentary-analysis bookkeeping.
  - `LiveNarrativeCompressionCore` now owns a shared player-language policy
    layer:
    - banned meta phrases such as `plan fit`, `nominal evaluation`,
      `practical conversion`, `conversion window`, `forgiveness`,
      `activation lane`, `counterplay suppression`, `cuts out counterplay`,
      and `making ... available`
    - concrete-anchor checks for abstract strategic words such as
      `initiative`, `counterplay`, `compensation`, `conversion`, `pressure`,
      `attack`, `plan`, `objective`, and `execution`
    - common rewrites such as `making g7 available for the queen` ->
      `bringing the queen to g7`
  - `StrategicThesisBuilder` source templates now prefer concrete-first
    language across the three highest-risk phrasing families:
    - development:
      `why this move`, `what square/file it works through`, `what break or
      exchange it prepares`
    - prophylaxis:
      `what move/break/entry square it stops` instead of generic
      `cuts out counterplay`
    - compensation / practical:
      `what is given up`, `what concrete pressure/initiative is gained`,
      `why the material does not need to be recovered immediately`, and
      `why the position is easier to handle over the board`
  - default Support panels and collapsed Advanced details now use the same
    player-language filter. Rows are kept only if they can be translated into
    concrete chess language; raw cp bookkeeping, latent/fit bookkeeping,
    probe/admin wording, and authoring/evidence console phrasing are pruned.
  - Long-term plans are not removed by this rewrite. Evidence-backed plan
    language still survives when it explains the move, but it is translated
    into concrete move-purpose language instead of being surfaced as internal
    planning jargon.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/LiveNarrativeCompressionCore.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeSignalDigestBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerLiveCompressionPolicy.scala`
  - `ui/analyse/src/chesstory/signalFormatting.ts`
  - `ui/analyse/src/chesstory/compactSupportSurface.ts`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/src/narrative/narrativeView.ts`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CompensationDisplayPhrasingTest.scala`
  - `ui/analyse/tests/compactSupportSurface.test.ts`
  - `ui/analyse/tests/frontendAuditRegression.test.ts`
- `2026-03-22` QC workflow update:
  - Commentary-player audit now has a deterministic 202-game audit-set layer
    above raw run artifacts. The set is built from:
    - `tmp/commentary-player-qc/manifests/active_parity_closure_runs_v2/*/report.json`
      `games[].id`
    - singled-out residual corpora
      `tmp/commentary-player-qc/manifests/single_actorxu_77.json`
      and
      `tmp/commentary-player-qc/manifests/single_infernal_59.json`
  - The deterministic audit set is emitted by:
    - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerAuditSetBuilder.scala`
  - Review queue generation now has an audit-set / full-review mode:
    - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerReviewQueueBuilder.scala`
    - `--audit-set <path>`
    - `--full-review` / `--no-sampling`
  - In audit-set mode, the queue no longer samples by default and emits four
    explicit audit surfaces:
    - Chronicle whole-game rows
    - Chronicle focus-moment rows
    - Bookmaker focus rows with support/advanced surfaces
    - Active-note parity rows
  - Review queue rows and manual judgments now carry richer audit metadata:
    - `gameId`
    - `reviewKind`
    - `tier`
    - `openingFamily`
    - optional `surface` on judgments for direct merge attribution
  - Review sharding now supports audit-mode partitioning by surface and tier:
    - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerReviewShardBuilder.scala`
  - Review merge now emits audit-level family distributions and patch guidance:
    - surface-level family counts
    - tier-level family counts
    - opening-family counts
    - exemplar bundle
    - root-cause hints
    - patch-target mapping
    - output paths:
      - `reports/fix-priority.json`
      - `reports/audit-report.md`
  - This is a QA / audit workflow change only.
- `2026-03-22` working-tree update:
  - Active-note strict compensation rescue now prioritizes the coaching
    brief's dominant idea over noisy dossier `whyNow` filler when assembling a
    fallback note.
  - This is a narrow parity-closure fix for residual compensation survivors
    where the contract/anchor/continuation were present, but the fallback note
    could still fail validation because generic route-start prose displaced the
    dominant idea sentence.
  - Verification targets:
    - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
    - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
    - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicNoteValidatorTest.scala`
    - `modules/llm/src/test/scala/lila/llm/LlmApiActiveParityTest.scala`
    - Public API/schema: unchanged
    - Bookmaker / Chronicle runtime response contracts: unchanged
- `2026-03-20` working-tree update:
  - a second cleanup pass hardens the remaining player-facing fallback/scaffold
    families that still showed up in live corpus review:
    - opening fallback wrappers such as
      `The follow-up is to ... without losing the position's balance` now
      collapse into direct move-purpose phrasing like
      `The next step is to finish development without giving up the center.`
      instead of reopening opening-name / balance-only filler.
    - Chronicle active strategic notes now pass through the same stricter
      player-facing sentence filter, so lines such as
      `The plan still revolves around ...`,
      `A useful route is ...`, and
      `The next useful target is ...`
      are either rewritten into simpler coaching language or dropped when they
      cannot clear the concrete-anchor gate.
    - default Support panels for both Bookmaker and Chronicle now render
      filtered `mainPlanTexts` and `holdReasons` from the same compact support
      surface, instead of showing the raw plan labels directly while the main
      prose is already compressed.
  - this pass reduces the remaining gap where the body prose had become more
    player-facing but support/active-note surfaces still leaked system
    scaffolding or opening-label filler.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/LiveNarrativeCompressionCore.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
  - `ui/analyse/src/chesstory/signalFormatting.ts`
  - `ui/analyse/src/chesstory/compactSupportSurface.ts`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/src/narrative/narrativeView.ts`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `ui/analyse/tests/compactSupportSurface.test.ts`
  - `ui/analyse/tests/frontendAuditRegression.test.ts`
- `2026-03-20` working-tree update:
  - live isolated-move Bookmaker prose now routes through
    `BookmakerLiveCompressionPolicy` before polish instead of letting the raw
    validated outline expand into a wide thesis/support/wrap-up surface.
  - Bookmaker slot selection is now constrained to a single main claim family
    with one primary support line and at most one optional third paragraph.
    Default live output budget is `2-3` paragraphs / `2-4` total sentences,
    with compact standard early openings allowed to collapse to a single brief
    paragraph.
  - internal system-language fragments such as `strategic stack`,
    `ranked stack`, `current support centers on`, `support still centers on`,
    `plan first`, `latent plan`, `probe evidence says`, and
    `nominal evaluation` are now explicitly banned from Bookmaker body prose at
    the slot/prompt/soft-repair layer.
  - Bookmaker body prose no longer uses sidecar-only state such as
    `whyAbsentFromTopMultiPV`, latent-plan bookkeeping, strategic-plan
    experiment status, authoring evidence summaries, or wrap-up stack summaries
    as standalone topics. These remain sidecar-only unless already converted
    into the compressed claim/support surface.
  - named strategic plans now stay in live Bookmaker body prose only when they
    are evidence-backed and actually explain the move's purpose; otherwise the
    prose falls back to move-purpose / square / file / practical-consequence
    language.
  - `BookmakerSoftRepair` and `PolishPrompt` now treat Bookmaker polishing as
    slot realization rather than free-form refinement: no new topic
    introduction, no fourth paragraph, and no cited-line paragraph without an
    actual SAN-backed line.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerLiveCompressionPolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/LineScopedCitation.scala`
  - `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala`
  - `modules/llm/src/test/scala/lila/llm/PolishPromptTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerProseGoldenTest.scala`
  - `modules/llm/src/test/resources/bookmaker_thesis_goldens/*.slots.txt`
- `2026-03-20` working-tree update:
  - Game Chronicle focused-moment prose now routes through
    `GameChronicleCompressionPolicy` before the older hybrid
    `lead + bridge + body` fallback. Chronicle main prose is therefore
    compressed to a small player-facing surface instead of inheriting the full
    validated outline expansion by default.
  - Chronicle claim selection now prefers `MainMove`, `DecisionPoint`,
    `Context`, and other player-facing beats, while strategic-distribution
    bookkeeping (`strategic_distribution_first`,
    `plan_evidence_three_stage`) and similar ranked-stack / support-center
    language are suppressed from the main moment narrative.
  - branch-based Chronicle follow-up prose now behaves like Bookmaker's cited
    line slot: an optional extra paragraph survives only when a concrete
    SAN-backed line is available. Otherwise the branch-specific extra prose is
    dropped instead of falling back to source-label-only narration.
  - `focusMomentOutline` now filters strategic-distribution wrap-up beats so
    they no longer consume scarce focused-moment slots or crowd out move-
    purpose / decision / cited-line beats.
  - `outlineBridgeCandidate` also ignores strategic-distribution beats, so the
    fallback bridge no longer reintroduces the bookkeeping prose that the
    focused outline removed.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/GameChronicleCompressionPolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala`
- `2026-03-20` working-tree update:
  - standard-chess commentary now has a shared backend claim-strength gate in
    `StandardCommentaryClaimPolicy`, used by both Bookmaker final prose and
    Game Chronicle hybrid prose before user-facing text is emitted.
  - quiet standard positions now prefer a brief backend-owned no-event note
    rather than filler strategic narration. This is active for very early
    standard openings by default and for later standard positions when there is
    no meaningful opening event, no evidence-backed main plan, no durable
    structural commitment, and no strong tactical driver.
  - the standard early-opening collapse is now significance-aware in one more
    way: durable structural commitments such as explicit structure/open-file
    shifts, structural weaknesses, strong break commitments, or clear king-
    safety damage bypass the opening clamp instead of being flattened into the
    generic quiet path.
  - `Fact.HangingPiece` is no longer treated as a synonym for any attacked,
    undefended unit in user-facing standard commentary. Single-attacked,
    undefended pawns are now extracted as `TargetPiece` rather than
    `HangingPiece`, and standard prose suppresses or downgrades hanging
    language unless the liability is concretely punishable.
  - direct bypass phrasing such as `tactical liability`, `direct tactical
    target`, `urgent`, and `requires immediate attention` is now normalized in
    the shared final-prose pass when standard runtime evidence does not justify
    the stronger tier.
  - Bookmaker and Game Chronicle now share the same quiet standard note path,
    so the same low-entropy standard position no longer reads restrained in one
    surface and inflated in the other.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StandardCommentaryClaimPolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/EarlyOpeningNarrationPolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/FactExtractor.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeLexicon.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CriticalAnnotationPolicy.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StandardCommentaryClaimPolicyTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/FactExtractorTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/EarlyOpeningNarrationPolicyTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeLexiconTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CriticalAnnotationPolicyTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerProseGoldenTest.scala`
- `2026-03-19` working-tree update:
  - Bookmaker rule fallback prose now runs through the same user-facing
    placeholder scrubber that already cleans slot/evidence surfaces, instead of
    relying on opening-family sanitization alone.
  - This closes the specific `fallback_rule_invalid` -> frontend retry path
    where safe fallback prose still leaked internal labels such as
    `PlayableByPV`, `under strict evidence mode`, `probe evidence pending`,
    `[subplan:...]`, or `{seed}`, causing the Bookmaker panel to show the
    generic timeout/retry state even though the backend returned `200`.
  - `UserFacingSignalSanitizer` now rewrites the `PlayableByPV` bridge itself
    into user-facing language, so the backend fallback no longer trips the
    frontend suspicious-fallback guard on that token alone.
  - Early-game gating is now explicit on both backend and frontend:
    - Bookmaker requests are rejected until `ply >= 5`, and the Bookmaker panel
      shows an on-surface locked state instead of wasting a polish attempt on
      opening positions that are still too branch-heavy to narrate cleanly.
      Player-facing copy now refers to move numbers rather than raw `ply`
      counts.
    - Game Chronicle requests are rejected until the PGN reaches `ply >= 9`,
      and the review / narrative entry points now render a disabled state with
      a short-game explanation rather than submitting doomed async/local jobs.
      Player-facing copy now uses move-based phrasing rather than `ply`.
- Verification:
  - `modules/llm/src/main/LlmApi.scala`
  - `modules/llm/src/main/scala/lila/llm/models.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/UserFacingSignalSanitizer.scala`
  - `app/controllers/LlmController.scala`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/src/bookmaker/blockingState.ts`
  - `ui/analyse/src/narrative/narrativeCtrl.ts`
  - `ui/analyse/src/narrative/narrativeView.ts`
  - `ui/analyse/src/review/view.ts`
  - `modules/llm/src/test/scala/lila/llm/RequestValidationTest.scala`
  - `modules/llm/src/test/scala/lila/llm/RuleTemplateSanitizerTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/FullGameDraftNormalizerTest.scala`
- `2026-03-19` working-tree update:
  - user-facing cleanup now has a payload-level backend gate in
    `UserFacingPayloadSanitizer`, not just prose-level placeholder scrubbing.
    Structured response fields such as `whyAbsentFromTopMultiPV`,
    latent-plan hold reasons, strategy-pack digest text, bookmaker ledger
    notes, and Game Chronicle moment text are sanitized through the same
    backend layer before they reach JSON/page rendering.
  - `PlanEvidenceEvaluator` no longer emits `PlayableByPV`,
    `engine-coupled continuation`, raw probe contract wording, or raw missing
    signal names into user-facing reasons. The source-stage evidence reasons are
    now written directly in club-player language such as current engine-line
    support vs. still-thin independent evidence.
  - compensation prose is now normalized away from internal payoff jargon:
    user-facing templates no longer rely on `return vector`, `cash out`,
    `delayed recovery`, or bare `line pressure` phrasing in the primary
    compensation claims/support text.
  - strategic-puzzle bootstrap/read paths now sanitize stored `runtimeShell`
    text on read in the controller layer, so legacy saved shells do not keep
    leaking internal commentary tokens into the reveal panel.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/UserFacingPayloadSanitizer.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/PlanEvidenceEvaluator.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/LlmApi.scala`
  - `app/controllers/LlmController.scala`
  - `app/controllers/StrategicPuzzle.scala`
  - `ui/analyse/src/bookmaker.ts`
  - `modules/llm/src/test/scala/lila/llm/UserFacingPayloadSanitizerTest.scala`
  - `modules/llm/src/test/scala/lila/llm/RuleTemplateSanitizerTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/PlanEvidenceEvaluatorTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CompensationDisplayPhrasingTest.scala`
  - `ui/analyse/tests/frontendAuditRegression.test.ts`
- `2026-03-16` working-tree update:
  - `StrategicIdeaSelector.enrich` now promotes typed compensation evidence
    into `strategyPack.signalDigest` even when `ctx.semantic.compensation` is
    absent. Carrier promotion now derives `compensation`,
    `compensationVectors`, and `investedMaterial` from existing typed
    compensation anchors such as `compensation_king_window`,
    `compensation_open_lines`, `delayed_recovery_window`, and
    `exchange_availability_bridge`, instead of leaving the Bookmaker payload
    compensation-empty.
  - `StrategyPackBuilder` now refreshes the `dominant thesis` after selector
    enrichment, so `longTermFocus` / `evidence` stop carrying a stale
    pre-enrichment thesis when the enriched pack upgrades the position into a
    compensation lens.
  - `StrategyPackSurface` now exposes digest-backed compensation vectors, and
    `StrategicThesisBuilder` consumes those vectors on the compensation path.
    This means selector-enriched carriers can directly drive Bookmaker claims
    and supports such as `compensation investment`, `initiative`, `line
    pressure`, `delayed recovery`, and `return vector` without depending on a
    semantic `CompensationInfo`.
  - Latest real-PGN rerun confirms the upstream carrier effect on the narrow
    compensation subset: Bookmaker direct `compensation / initiative` mention
    rose from `2/8` to `6/8`, while `BEN01` moment recall remains `0` and
    `CAT02 ply 45` / `QID02 ply 52` still show missing raw compensation
    carriers on the single-position path.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSelector.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategyPackBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/docs/RealPgnNarrativeEvalReport.latest.md`
- `2026-03-16` working-tree update:
  - Bookmaker deterministic thesis selection now lets `strategyPack` surface
    compensation carry the prose path even when `ctx.semantic.compensation` is
    absent, so single-position commentary no longer depends on semantic
    `CompensationInfo` alone to mention compensation / initiative.
  - The compensation lens now falls back to `signalDigest.compensation`,
    invested-material metadata, dominant idea, and execution/objective carriers
    before dropping into a generic decision thesis.
  - When Bookmaker stays on the decision lens but a dominant strategic surface
    exists, the first sentence now prefers `dominant thesis +
    execution/objective` wording over the generic `The key decision is to
    choose...` scaffold.
  - Surface-backed decision support also stops defaulting to the bare
    `The whole decision turns on...` sentence; it now prefers objective /
    rationale support and only falls back to focal-square wording when needed.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala:140`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala:147`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala:315`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala:354`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala:146`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala:381`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala:221`
- `2026-03-16` working-tree update:
  - Bookmaker tactical/annotation claim override no longer outranks a surfaced
    strategic thesis when `strategyPack` already provides dominant idea,
    execution, objective, or compensation carriers.
  - This prevents tactical/critical main-move scaffolds from reintroducing
    `The whole decision turns on...` as paragraph 1 when a stronger strategic
    claim is already available.
  - Compensation theses now prioritize explicit compensation lexicon in the
    claim/support chain: `compensation investment`, `cash out`, `return
    vector`, `initiative`, `delayed recovery`, or `line pressure` are emitted
    ahead of generic execution/objective filler.
  - Decision-surface fallback now only emits `The whole decision turns on...`
    when no dominant idea, execution, objective, or compensation carrier is
    available.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala:40`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala:119`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala:147`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala:315`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala:266`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala:275`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala:300`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala:320`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala:146`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala:430`
- `2026-03-16` working-tree update:
  - Bookmaker single-position runtime now preserves after-move compensation as
    an internal semantic carrier (`afterCompensation`) instead of using
    `afterAnalysis` only for delta/phase hints.
  - `NarrativeSignalDigestBuilder` is now after-move aware: it prefers current
    `compensation`, then `afterCompensation`, and only then falls back to typed
    selector promotion. This keeps raw `signalDigest.compensation`,
    `compensationVectors`, and `investedMaterial` alive when the compensation
    only stabilizes after the played move.
  - `afterCompensation` promotion is now guarded by a recapture-neutralization
    check. If the played move is a parity-restoring capture that simply erases
    the current material deficit, `NarrativeContextBuilder` drops the
    after-move compensation carrier and `NarrativeSignalDigestBuilder` applies
    the same gate before digest fallback. This prevents tactical recaptures
    like `...exf4` after `Bxf4` from surfacing as long-term compensation.
  - `NarrativeContextBuilder` now calibrates raw semantic compensation before
    it becomes `compensation` / `afterCompensation`:
    - non-opening positions with only a thin `return vector` carrier are
      dropped
    - late technical endgames with a large material edge but no
      initiative / line-pressure / delayed-recovery carrier are dropped
  - `StrategicIdeaSelector` typed fallback now stays dormant when the digest
    already carries a compensation payload, and its remaining fallback widens
    two narrow miss classes: IQP/exchange-backed transformation windows and
    established line-pressure / fixed-target pressure with real line-access
    carriers.
  - Compensation digest calibration is now more conservative in two edge
    cases:
    - late technical endgames with a large material edge but no initiative /
      line-pressure / delayed-recovery carrier no longer auto-promote a
      compensation digest
    - non-opening positions that only produce a thin `return vector` summary
      without initiative / line-pressure / delayed-recovery support no longer
      auto-tag compensation
  - The regression target is the exact Bookmaker API path, not a prose-only
    helper: `CAT02 ply 45` and `QID02 ply 52` now have direct tests that pass
    `fen + afterFen + afterEval + afterVariations` and assert both raw digest
    compensation carriers and visible `compensation` / `initiative` lexicon.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeSignalDigestBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSemanticContext.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSelector.scala`
  - `modules/llm/src/test/scala/lila/llm/BookmakerAfterCompensationCarrierTest.scala`
- `2026-03-17` working-tree update:
  - Compensation acceptance no longer lives as separate ad hoc rules in
    `NarrativeContextBuilder`, `NarrativeSignalDigestBuilder`, selector-side
    derived fallback, and prose consumers.
  - A shared internal helper,
    `modules/llm/src/main/scala/lila/llm/analysis/CompensationInterpretation.scala`,
    now centralizes compensation interpretation for all three source classes:
    - current semantic compensation
    - after-move semantic compensation
    - derived carrier compensation from digest / `StrategyPackSurface`
  - The centralized decision emits an internal verdict instead of rewriting raw
    `CompensationInfo`:
    - `accepted`
    - `rejectionReason`
    - `recaptureNeutralized`
    - `thinReturnVectorOnly`
    - `lateTechnicalConversionTail`
    - `durableStructuralPressure`
    - `persistenceClass`
  - The acceptance order is now fixed across the runtime path:
    - `acceptance`
    - `subtype`
    - `display normalization`
    - `prose`
  - `current compensation` now shares the same rejection gate family as
    `afterCompensation`; both are rejected by the same centralized logic when
    they are:
    - parity-restoring recaptures
    - thin `return vector` stories with no durable carrier
    - late technical conversion tails with no durable pressure
  - `NarrativeContextBuilder` now only surfaces `semantic.compensation` /
    `semantic.afterCompensation` when the shared interpretation layer accepts
    them.
  - `NarrativeSignalDigestBuilder` no longer runs its own independent
    compensation acceptance logic; digest fallback now comes from the same
    shared interpretation result.
  - Selector-side derived compensation promotion is no longer calibrated
    independently. `StrategicIdeaSelector` now accepts a derived compensation
    carrier only when the shared interpretation layer accepts the derived
    summary/vectors/invested-material combination.
  - Direct prose bypasses were reduced:
    - `NarrativeOutlineBuilder` compensation wrap-up text now consumes the
      effective interpreted signal instead of reading raw semantic compensation
      directly
    - `StrategicThesisBuilder` compensation lens entry now prefers the shared
      interpretation result, so `Bookmaker` no longer decides compensation from
      raw semantic/digest carriers on its own
    - `BookmakerStrategicLedgerBuilder` and `StructurePlanArcBuilder` also use
      the shared semantic decision for compensation motifs / coda text
  - The practical regression target is no longer only “tagging in the report”.
    Real-PGN signoff now includes explicit negative-guard and agreement
    metrics:
    - false positive count
    - false negative count on positive exemplars
    - cross-surface agreement rate
    - subtype agreement rate
    - negative-guard pass/fail
  - `TAT06 ply 60` is now a fixed negative guard. It must satisfy both:
    - not compensation-tagged in the report
    - no compensation lexicon in raw `Bookmaker` prose
  - Latest strict real-PGN rerun after this centralization/signoff pass:
    - `11` games
    - `33` focus moments
    - signoff `falsePositiveCount = 0`
    - signoff `falseNegativeCount = 4`
    - signoff `crossSurfaceAgreementRate = 0.9375`
    - signoff `subtypeAgreementRate = 0.6875`
    - negative guards `1 / 1` passing, with `TAT06 ply 60` kept out of both
      report tagging and raw `Bookmaker` compensation wording
  - Quiet-compensation `Active Note` wording now goes through the shared
    `StrategyPackSurface.compensationWhyNowText(...)` templates and explicitly
    uses compensation / line-pressure lexicon for durable non-attack cases, so
    the signoff agreement metric is no longer held down by notes that had the
    right subtype but only said `file pressure` or `investment` indirectly.
  - `StrategyPackSurface` display subtype scoring is now explicitly split into:
    - `preparation path` subtype, derived from route / move-ref / directional
      anchors and used as the execution-side explanation
    - `payoff theater` subtype, derived from fixed-target focus/objective/text
      anchors and used as the intended dominant/focus truth when its confidence
      is high enough
  - The internal display layer now keeps:
    - `preparationSubtype`
    - `payoffSubtype`
    - `selectedDisplaySubtype`
    - `displaySubtypeSource`
    - `pathConfidence`
    - `payoffConfidence`
  - Consumer contract remains:
    - `dominantIdea` / `focus` / subtype label should follow the selected
      display subtype
    - `execution` may still mention the preparation path
  - The current calibration result after the preparation/payoff split is:
    - `falsePositiveCount = 0`
    - `falseNegativeCount = 3`
    - `crossSurfaceAgreementRate = 1.0`
    - `subtypeAgreementRate = 0.7857142857142857`
    - `negativeGuardPassCount = 1 / 1`
  - Remaining real-PGN subtype mismatches are now concentrated in three path
    alignment cases:
    - `MOR01 ply 25`: `Game Arc = center/line_occupation`, `Bookmaker = queenside/target_fixing`
    - `QID02 ply 42`: `Game Arc = kingside/target_fixing`, `Bookmaker = center/target_fixing`
    - `CAT02 ply 33`: `Game Arc = queenside/target_fixing`, `Bookmaker = kingside/target_fixing`
  - Interpretation: compensation existence and cross-surface compensation
    agreement are effectively closed, but final subtype theater/mode alignment
    still depends on how strongly the path scorer separates preparation lanes
    from true payoff squares in those three cases.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/CompensationInterpretation.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeSignalDigestBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSemanticContext.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSelector.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StructurePlanArcBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CompensationInterpretationTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeContextBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalCalibrationTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalRunner.scala`
  - `modules/llm/docs/RealPgnNarrativeEvalReport.latest.md`
- `2026-03-16` working-tree update:
  - Active-note attachment on the full-game `Game Arc` path no longer depends
    on `allowLlmPolish = true`.
  - `analyzeGameChronicleLocal` now preserves the requested active-note level
    (`requestedLevel = active`) for the downstream note-attachment path even
    when base polish is downgraded to `effectiveLevel = polish`.
  - `maybePolishGameChronicle` no longer exits early before active-note
    attachment when LLM polish is disabled or the global provider is `none`;
    it now skips base prose polish but still runs `attachActiveStrategicNotes`
    for `Pro + Active` requests.
  - `maybeGenerateActiveStrategicNote` now has a deterministic rule fallback
    built from `ActiveStrategicCoachingBriefBuilder`, and that fallback is also
    used whenever the LLM route is unavailable or returns an omitted note.
  - The deterministic active-note path is now live in the real-PGN evaluation
    runner: the latest rerun produced `75` attached notes overall, `31/33`
    focus moments with active notes, and `18/18` compensation-tagged focus
    moments with direct compensation / initiative wording. The two remaining
    focus moments without active notes were not note-generation failures; they
    stayed outside the `strategicBranch` selection set.
- Verification:
  - `modules/llm/src/main/LlmApi.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicNoteValidatorTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalRunner.scala`
  - `modules/llm/docs/RealPgnNarrativeEvalReport.latest.md`
- `2026-03-16` working-tree update:
  - `StrategicBranchSelector` active-note selection is no longer limited to
    thread representatives.
  - When spare active-note capacity remains, the selector now fills it from
    visible strategic key moments with real strategy carriers, and in
    no-thread positions it can fall back to strategic carrier moments instead
    of emitting no active-note candidates at all.
  - This is a selection-quality change only; it does not widen visible moments
    indiscriminately and it does not alter the `Benko` recall contract beyond
    making note-candidate selection less brittle once a moment is already
    visible.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicBranchSelector.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicBranchSelectorTest.scala`
- `2026-03-16` working-tree update:
  - Deterministic Active-note prose now normalizes raw route / move-ref wording
    more aggressively:
    - raw owner-prefixed piece shorthands such as `Black R toward ...` are
      rewritten into player-facing piece names
    - awkward `for contest ...` / `for keep ...` purpose tails are rewritten
      into `to contest ...` / `to keep the pressure fixed there`
    - duplicated objective scaffolds such as
      `working toward working toward ...` are removed before final note
      assembly
  - The real-PGN rerun now shows `32/33` focus moments with deterministic
    `rule` notes present; the only remaining `missing` focus note is
    `QID02 ply 38`, which is a non-compensation, non-`strategicBranch` focus
    moment and therefore outside the attach contract.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicNoteValidatorTest.scala`
  - `modules/llm/docs/RealPgnNarrativeEvalReport.latest.md`
- `2026-03-16` working-tree update:
  - Real-PGN compensation calibration is now explicit and regression-tested in
    the runner layer rather than left to ad hoc manual interpretation.
  - The current calibration keeps early / dynamic compensation moments such as
    `EVA01 ply 17`, but demotes late technical tails that only preserve a thin
    static compensation story:
    - `EVA02 ply 73`
    - `EVA02 ply 75`
    - `QID02 ply 38`
  - After calibration, the latest real-PGN rerun keeps an `18`-moment
    compensation-tagged focus subset across the `11`-game corpus, with
    Bookmaker and deterministic Active-note both speaking in explicit
    compensation / initiative language on those tagged focus moments.
- Verification:
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalRunner.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalCalibrationTest.scala`
  - `modules/llm/docs/RealPgnNarrativeEvalReport.latest.md`
- `2026-03-16` working-tree update:
  - Probe prioritization is now strategy-aware on both the real-PGN eval path
    and the local Game Arc refinement path.
  - Probe planning no longer sorts by raw ply alone; it now prefers, in order:
    - compensation + `strategicBranch`
    - `strategicBranch` + strategic carrier
    - key moments with strategic carrier
    - remaining compensation moments
    - remaining strategic-carrier moments
  - Frontend `probePlanning.ts` and the real-PGN runner now share this
    priority shape closely enough that runtime and evaluation probe usage stop
    drifting apart.
  - Latest real-PGN rerun summary after this priority change:
    - `11` games
    - `33` focus moments
    - probe refinement used in `10` games
    - probe requests `23 / 23 / 0` for
      `candidate / executed / unsupported`
- Verification:
  - `ui/analyse/src/narrative/probePlanning.ts`
  - `ui/analyse/tests/narrativeProbePlanning.test.ts`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalRunner.scala`
  - `modules/llm/docs/RealPgnNarrativeEvalReport.latest.md`
- `2026-03-16` working-tree update:
  - Long-term compensation is now decomposed internally instead of being treated
    as a single attack-flavored carrier.
  - `StrategyPackSurface` derives an internal-only `compensationSubtype` from
    existing shipped carriers with four axes:
    - `pressureTheater`: `kingside / queenside / center / mixed`
    - `pressureMode`: `line_occupation / target_fixing / break_preparation /
      defender_tied_down / counterplay_denial / conversion_window`
    - `recoveryPolicy`: `immediate / delayed / intentionally_deferred`
    - `stabilityClass`: `tactical_window / durable_pressure / transition_only`
  - Public schema is unchanged. The subtype is consumed only by selector
    weighting, moment ranking, probe priority, and deterministic phrasing.
  - `StrategicIdeaSelector` is less attack-biased in quiet compensation:
    - weak-king-window compensation no longer auto-promotes
      `king_attack_build_up`
    - `line_occupation`, `target_fixing`, and
      `counterplay_suppression` now receive compensation-aware bridges when the
      investment instead buys durable files, fixed targets, or passive-defense
      tie-down
    - a new typed suppression carrier (`compensation_counterplay_denial`) keeps
      denied-break / passive-defender shells positional instead of drifting back
      into attack
  - `StrategicBranchSelector` and the real-PGN focus ranking now explicitly
    favor quiet durable compensation. Moments with
    `line_occupation / target_fixing / counterplay_denial` plus deferred or
    durable compensation can outrank louder tactical tails when they are the
    clearest explanation of the investment.
  - Probe reuse changed only in planning/consumption, not in public contract:
    - no new probe purpose or new public response field was added
    - backend runner and frontend `probePlanning.ts` now prioritize
      `durable_pressure` / quiet-compensation moments ahead of tactical tails
    - probe-backed reasoning is now intended to answer whether compensation
      remains durable (`delayed recovery`, `fixed targets`, `line pressure`,
      `counterplay denial`) rather than only whether a forcing tactic exists
  - Deterministic phrasing is now subtype-aware instead of always sounding like
    a kingside initiative attack:
    - Bookmaker compensation claims/supports prefer subtype text such as
      `durable pressure`, `open-file control`, `fixed targets`,
      `counterplay remains tied down`, or `access bought by the investment`
    - Game Arc hybrid bridge prefers subtype persistence text before generic
      compensation wording
    - Active-note `why now` / `longTermObjective` now share the same display
      gate, so quiet positional compensation uses subtype text only when the
      shared normalization layer is active; attack-led samples fall back to raw
      initiative wording instead of inheriting quiet `open-line pressure`
      language
  - A second pass adds subtype-aware display normalization inside
    `StrategyPackSurface` without changing raw selector output:
    - raw `dominantIdea`, route, move-ref, target, and `longTermFocus` carriers
      remain intact
    - `StrategyPackSurface` now keeps owner-prioritized
      `allRoutes / allMoveRefs / allDirectionalTargets` in addition to top
      route/move-ref/target so subtype derivation and display normalization are
      not forced to overfit a single tactical-looking anchor
    - an internal-only display layer now exposes normalized
      `dominantIdea / execution / objective / longTermFocus /
      compensation lead` when subtype confidence is high
    - this display layer is the only place where quiet compensation is
      re-labeled, so `Bookmaker`, `Game Arc`, and `Active Note` no longer
      interpret subtype separately
    - normalization is gated off for low-confidence, tactical-window, or
      transition-only compensation, and also for immediate kingside attack
      cases where the raw selector still clearly says `king_attack_build_up`
    - quiet positional compensation can now override attack-flavored theater
      only when there are real structural anchors (fixed-target move refs,
      file-pressure routes, delayed recovery / return-vector carriers), so
      subtype promotion is less likely to hallucinate a queenside story from a
      purely tactical route
    - Bookmaker compensation lead/support now only consumes subtype payoff and
      persistence text when `normalizationActive` is true; otherwise it falls
      back to raw compensation carriers instead of forcing quiet-pressure prose
  - Latest strict real-PGN rerun after this subtype pass:
    - `11` games, `33` focus moments
    - probe requests `22 / 22 / 0` for `candidate / executed / unsupported`
    - compensation-tagged focus moments: `22`
    - subtype distribution:
      - `center/target_fixing/intentionally_deferred/durable_pressure = 1`
      - `queenside/target_fixing/intentionally_deferred/durable_pressure = 5`
      - `kingside/line_occupation/intentionally_deferred/durable_pressure = 3`
      - `center/line_occupation/immediate/durable_pressure = 4`
      - `center/line_occupation/intentionally_deferred/durable_pressure = 5`
      - `kingside/line_occupation/immediate/durable_pressure = 2`
    - Benko no longer disappears from the evaluation corpus. Its later quiet
      focus (`BEN01 ply 41`) now surfaces as
      `queenside/target_fixing/intentionally_deferred/durable_pressure`, while
      its earlier compensation focus (`BEN01 ply 23`) remains a calibration
      case rather than collapsing into a generic missing/empty moment.
- `2026-03-17` working-tree update:
  - Display-subtype normalization is now less circular and less attack-biased
    in route-less compensation shells:
    - theater / mode voting no longer re-consumes `longTermFocus` text as a
      structural anchor, so a previously generated `queenside file pressure`
      string is less able to force the next normalization pass to stay on the
      same flank
    - delayed-recovery rescue now remains available for raw
      `transition_only` / `conversion_window` shells when the same pack still
      carries enough structural pressure anchors to justify a durable
      compensation display subtype
    - route-less positions with multiple directional targets now get a narrow
      plurality override for `pressureTheater`, so the display layer can follow
      the actual target cluster instead of overfitting a single tactical-looking
      target
    - weak `pressure on a fixed weakness` hints no longer count as a strong
      target-fixing anchor by themselves; explicit `attacking fixed pawn` /
      `static weakness fixation` cues still do
  - Latest strict real-PGN rerun after this follow-up pass:
    - signoff `falsePositiveCount = 0`
    - signoff `falseNegativeCount = 3`
    - signoff `crossSurfaceAgreementRate = 1.0`
    - signoff `subtypeAgreementRate = 0.8125`
    - negative guards `1 / 1` passing
  - This lifted subtype agreement from `0.75` to `0.8125` without regressing
    the negative guard or the cross-surface agreement rate.
  - Remaining subtype mismatches after the pass are down to three real-PGN
    cases:
    - `KG01 ply 39`
    - `QID02 ply 54`
    - `CAT02 ply 43`
  - These remaining mismatches are no longer generic compensation/no-compensation
    disagreements. They are specifically:
    - `kingside line occupation` vs `kingside target fixing`
    - `center line occupation` vs `center target fixing`
    - `center line occupation` vs `queenside line occupation`
  - In other words, the remaining calibration work is now almost entirely
    about final theater/mode alignment inside the shared display subtype layer,
    not about missing compensation carriers or consumer-specific prose drift.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSelector.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicBranchSelector.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicIdeaSelectorTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicBranchSelectorTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalCalibrationTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalRunner.scala`
  - `ui/analyse/src/narrative/probePlanning.ts`
  - `ui/analyse/tests/narrativeProbePlanning.test.ts`
  - `modules/llm/docs/RealPgnNarrativeEvalReport.latest.md`
- `2026-03-17` working-tree update:
  - The shared display-normalization layer now carries a small amount of
    internal-only late carrier context in `StrategyPackSurface.Snapshot`:
    - `evidenceHints`
    - `strategicStack`
    - `latentPlan`
    - `decisionEvidence`
  - These are **not** reused as generic theater-voting text. They only add a
    narrow `targetFixingAnchorStrength` bonus when fixed-target phrases survive
    only in late bookkeeping carriers such as `Attacking fixed Pawn`,
    `backward pawn`, or `weakness fixation`.
  - `StrategyPackBuilder.buildLongTermFocus(...)` also now sees `pieceMoveRefs`
    and can add a raw
    `keep the fixed central targets under pressure before recovering material`
    focus line when centered open-file occupation and repeated `target_pawn`
    cues confirm that the compensation shell is about static targets instead of
    generic line pressure.
  - `StrategicThesisBuilder` now also distinguishes two late display-subtype
    rescue paths:
    - fixed-pawn **plan-carrier rescue** driven by `latentPlan` /
      `decisionEvidence` mentions such as `Attacking fixed Pawn`
    - repeated-`target_pawn` rescue for centered quiet-compensation shells even
      when same-theater file routes are sparse
  - A narrow line-occupation lock was retained for raw line-occupation shells
    that only pick up late fixed-pawn hints without a matching fixed-pawn plan
    carrier.
  - Latest strict real-PGN rerun after this narrower late-carrier pass:
    - signoff `falsePositiveCount = 0`
    - signoff `falseNegativeCount = 3`
    - signoff `crossSurfaceAgreementRate = 1.0`
    - signoff `subtypeAgreementRate = 0.75`
    - negative guards `1 / 1` passing
  - This pass preserved the negative guard and the cross-surface compensation
    agreement, but it still did **not** push subtype agreement beyond the prior
    plateau. The remaining disagreement moved rather than disappeared.
  - Current remaining real-PGN subtype mismatches are:
    - `CAT01 ply 39`
    - `MOR01 ply 19`
    - `EVA01 ply 23`
    - `CAT02 ply 33`
  - In other words, the remaining calibration work is still concentrated in
    the final shared display-subtype layer: specifically `target_fixing` vs
    `line_occupation`, and central vs queenside theater resolution.
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
  - `LatentPlan` no longer dies outright on the Game Arc path when probe
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
  - `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala:47`
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
 - `2026-03-18` compensation cleanup-only refactor:
   - `StrategyPackSurface` remains the single consumer-facing facade for
     compensation display data.
   - display-subtype selection is now routed through one internal
     `CompensationDisplaySubtypeResolver` helper, while normalized payoff prose
     is routed through one internal `CompensationDisplayPhrasing` helper.
   - the runtime contract, consumer accessors, real-PGN signoff targets, and
     public JSON/report shape are intentionally unchanged in this pass.
   - payoff-scoring now treats generic `decisionEvidence` as preparation-path
     context rather than generic payoff text, and fixed-target objective
     anchors are weighted ahead of that path noise in the display subtype
     resolver.
  - frontend Chesstory signal shape and text-format helpers now live in shared
    modules so Bookmaker and Game Chronicle no longer maintain parallel
    `NarrativeSignalDigest` / `StrategicIdea*` definitions or duplicate token /
    evidence / deployment formatting logic.
  - authoring evidence payload assembly now has a single `build(ctx)` bundle,
    and the Game Arc path reuses that bundle plus its headline instead of
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
- `2026-03-18` working-tree update:
  - `/analysis` frontend runtime now keeps its direct affordances aligned with
    the actual handlers:
    - under `Reference > Import`, the FEN workspace now binds `Enter` directly
      to `changeFen`, so the visible helper copy is no longer misleading
    - the keyboard-help modal is served again from `/analysis/help`, so `?`
      no longer opens a dead route
  - review-shell / notebook frontend consumption now includes explicit dark
    theme styling for the `Review Shell` and notebook-style atlas surfaces,
    instead of leaving those panels on the light parchment palette when the
    outer shell is dark
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
- `2026-03-20` working-tree update:
  - Phase 1 expression-suppression now lands before any plan-selection /
    fallback rewrite.
  - `CommentResponse` and `GameChronicleMoment` now expose
    `strategicPlanExperiments` for surfaced `mainStrategicPlans`, preserving
    the existing experiment shape (`planId`, `subplanId`, `evidenceTier`,
    `moveOrderSensitive`, counts/confidence) into both Bookmaker and Game Arc
    payloads.
  - Frontend consumption now joins `mainStrategicPlans` with
    `strategicPlanExperiments` by `planId + subplanId`, and renders explicit
    support state:
    - `evidence_backed -> Probe-backed`
    - `pv_coupled -> Engine-line only`
    - `moveOrderSensitive -> Move-order sensitive`
  - `whyAbsentFromTopMultiPV` is no longer suppressed when a canonical
    decision-comparison surface exists. Both Bookmaker and Game Arc now keep
    those reasons visible under the plan-gating label
    `Why it stayed conditional`.
  - Deterministic overclaiming was softened without changing plan ranking or
    `leadingPlanName` fallback:
    - generic negative annotations no longer use unqualified
      `required / only stable route / necessary`
    - decision / compensation thesis copy no longer says
      `dominant thesis`, `The whole decision turns on ...`, or
      `The compensation only works if ...` in the generic path
    - Game Arc hybrid bridge now uses the same softened compensation /
      leading-idea language as Bookmaker rather than a stronger bridge-only
      assertive shell
- Verification:
  - `modules/llm/src/main/scala/lila/llm/models.scala`
  - `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala`
  - `modules/llm/src/main/scala/lila/llm/UserFacingPayloadSanitizer.scala`
  - `modules/llm/src/main/LlmApi.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeLexicon.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `ui/analyse/src/bookmaker/responsePayload.ts`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/src/narrative/narrativeCtrl.ts`
  - `ui/analyse/src/narrative/narrativeView.ts`

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
    - `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala:47`
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

### 2. Strategic distribution / latent-plan / opening / practical wrap-up on Game Arc path

- Verdict: `Underutilized`
- Why this is a problem:
  - These beats reach the outline, but late paragraphs are clipped on the
    Game Arc path, so final narrative under-consumes them.
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
  - Game Chronicle frequently loses opening theory, strategic stack,
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
    `mainStrategicPlans`, `strategicPlanExperiments`, `latentPlans`, and
    `whyAbsentFromTopMultiPV`, but the frontend still does not render the
    authoring/evidence summaries as a dedicated visible block.
  - Game Arc path passes `probeResults = Nil`, weakening latent-plan and
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
  - `modules/llm/src/main/scala/lila/llm/model/GameArc.scala:18`
  - `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala:43`
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
  - plan-support metadata now survives into both consumers:
    - `ui/analyse/src/bookmaker/responsePayload.ts:100`
    - `ui/analyse/src/bookmaker.ts:495`
    - `ui/analyse/src/narrative/narrativeView.ts:1209`
  - Bookmaker / Game Arc now visibly render plan support state and plan-gating
    reasons, but still not the richer author-question / branch-proof block:
    - `ui/analyse/src/bookmaker.ts:562`
    - `ui/analyse/src/narrative/narrativeView.ts:1324`
- User-facing impact:
  - plan partitioning and support-tier uncertainty now survive into the API
    layer and are visible in both interfaces.
  - users still do not see the actual author-question / branch-proof objects
    directly in the interface.
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
  - `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala:69`
  - `app/controllers/LlmController.scala:191`
- Real consumption / non-consumption:
  - Bookmaker build gate:
    - `modules/llm/src/main/LlmApi.scala:2363`
  - Controller hardcodes `Polish`:
    - `app/controllers/LlmController.scala:263`
  - Bookmaker payload type now decodes a minimal `strategyPack` subset and feeds
    it into the existing `Strategic Signals` block:
    - `ui/analyse/src/bookmaker/responsePayload.ts`
    - `ui/analyse/src/bookmaker.ts`
  - Game narrative UI type now carries `strategyPack` and uses it in both the
    moment signal shell and `Strategic Note` fallback surfaces:
    - `ui/analyse/src/narrative/narrativeCtrl.ts`
    - `ui/analyse/src/narrative/narrativeView.ts`
- User-facing impact:
  - `strategyPack` is no longer backend-only; the existing Bookmaker and Game
    Arc shells now surface idea / campaign / execution / objective / focus from
    the same carrier.
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
  - `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala`
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
  - `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala`
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
- `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala`
- `modules/llm/src/main/scala/lila/llm/model/GameArc.scala`
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
  - `GameChronicleResponse.schema = chesstory.gameNarrative.v5`
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
  - if no canonical thread and no tactical/opening filler survives, the selector now
    falls back to carrier-backed strategic key moments instead of returning an empty
    visible list; this fallback only uses already-shipped `strategyPack/signalDigest`
    surfaces (`dominantIdea`, execution/objective/focus, compensation carrier)
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
- `analyzeGameChronicleLocal(... disablePolishCircuit = true)` is benchmark-only
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
    - code defaults now align with the intended runtime split:
      - global/base polish defaults to OpenAI `gpt-5-mini`
      - `LLM_PROVIDER_ACTIVE_NOTE` defaults to `gemini`
      - OpenAI active-note fallback defaults to `gpt-5-mini`
      - `reasoning_effort = none` still applies on the OpenAI active-note path
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
- `modules/llm/src/main/scala/lila/llm/model/GameArc.scala`
- `modules/llm/src/main/scala/lila/llm/models.scala`
- `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala`
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
  - `GameChronicleResponse.schema = chesstory.gameNarrative.v6`
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
- private design notes retained outside the public repository
- `modules/llm/src/main/scala/lila/llm/models.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/strategic/StrategicAnalyzers.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSelector.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicNoteValidator.scala`
- `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala`
- `modules/llm/src/main/scala/lila/llm/model/GameArc.scala`
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

## 2026-03-16 Bookmaker / Game Arc / Active Note Strategy Surface Wiring

- No public schema bump in this step. `StrategyPack v2` and
  `GameChronicleResponse v6` remain current.
- Shared carrier change:
  - `StrategyPack` is now consumed as a prose/surface carrier instead of a
    summary-only byproduct.
  - a local `StrategyPackSurface` extractor derives dominant idea,
    campaign owner, owner mismatch, top route, top move-ref, directional
    target, focus, and compensation context from the existing typed payload.
- Bookmaker runtime:
  - `LlmApi.computeBookmakerResponse` now builds `strategyPack` before
    deterministic thesis slots and uses the slots as the default prose seed.
  - `StrategicThesisBuilder` now accepts optional `strategyPack` input, so
    compensation / prophylaxis / structure claims can surface campaign-owner,
    execution, and objective text without creating a new taxonomy.
  - `evaluateStrategyCoverage` is no longer the old one-axis lenient gate:
    - general strategic prose now needs two of `plan / route / focus`, or
      dominant-idea + focus grounding
    - invested-material / compensation prose must mention compensation or
      initiative plus a return vector / execution / objective
    - owner-mismatch positions now fail with
      `strategy_campaign_owner_missing` if the commentary never makes the
      campaign owner explicit
  - Bookmaker frontend keeps the existing `Strategic Signals` block but now
    renders `Idea`, conditional `Campaign`, `Execution`, and `Objective`
    directly from decoded `strategyPack`.
- Game Arc runtime:
  - `generateGameArc` / `buildMomentNarratives` now accept optional internal
    `probeResultsByPly: Map[Int, List[ProbeResult]] = Map.empty`.
  - full-game local refinement is now a frontend-assisted `2-pass` flow:
    1. initial `Game Chronicle` response is generated without probe fanout
    2. backend marks up to `3` internal probe candidates via
       `GameArcMoment.probeRefinementRequests`
    3. `ui/analyse/src/narrative/narrativeCtrl.ts` runs those requests through
       the existing WASM `createProbeOrchestrator`
    4. refined per-ply probe results are sent back through the existing local
       endpoint with `X-Chesstory-GameArc-Refine: 1`
    5. backend rerenders the same `Game Arc` with those per-ply probe results
  - internal probe fanout and external evidence surface are now split:
    - internal probe planning budget: `3` moments, `1` request per moment
    - external evidence surface budget: unchanged
      `FullGameEvidenceSurfacePolicy.MaxMoments = 2`
  - probe planning still reuses existing typed carriers; no new analyzer stack
    or response family was added.
  - per-moment surfaced evidence remains `probeRequests` /
    `authorQuestions` / `authorEvidence`, while the extra internal probe budget
    now rides on the non-rendered `probeRefinementRequests` carrier.
  - moment generation order is now:
    1. `NarrativeContextBuilder.build`
    2. `StrategyPackBuilder.build`
    3. `AuthoringEvidenceSummaryBuilder.build`
    4. `buildMomentSignalDigest`
    5. final `renderHybridMomentNarrative`
  - the hybrid bridge is now strategy-aware and can surface compensation,
    dominant idea, campaign owner, execution, objective, and opponent-plan
    hints before the focused body is assembled.
  - Game Arc frontend keeps the existing `Strategic Signals` and
    `Strategic Note` blocks but now uses `strategyPack` as a fallback for
    idea / execution / objective / focus rows and owner-mismatch badges.
- Active note runtime:
  - `ActiveStrategicCoachingBriefBuilder` now reuses the same carrier to:
    - mark owner mismatch in `campaignRole` / `primaryIdea`
    - prefer compensation/initiative wording in `whyNow` and
      `longTermObjective`
    - fall back from routes to move refs and then directional targets when the
      route surface is weak or absent
  - `ActiveStrategicNoteValidator` coverage now hard-fails owner-mismatch notes
    that never identify whose campaign is being described
    (`campaign_owner_missing`).
  - Verification targets for this stream:
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerProseGoldenTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/FullGameEvidenceSurfacePolicyTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicNoteValidatorTest.scala`
  - `ui/analyse/tests/bookmakerResponsePayload.test.ts`
  - `ui/analyse/tests/narrativeProbePlanning.test.ts`
  - `ui/analyse/tests/narrativeView.test.ts`

## 2026-03-12 Typed Semantic Strategic Idea Selector Overhaul

- No public schema bump in this step. `StrategyPack v2` and `GameChronicleResponse v6`
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
- `NarrativeContextBuilder` now also bridges compact plan / probe experiment
  summaries into `StrategicIdeaSemanticContext` from existing
  `PlanEvidenceEvaluator.partition(...).evaluated` output. This is not a new
  analyzer layer; it reuses existing plan evidence statuses and probe outcomes
  so the selector can score ideas with:
  - probe-backed viability
  - best-reply stability
  - future-snapshot alignment
  - counter-break neutralization
  - move-order sensitivity
- The selector now emits an idea only when typed evidence exists; the generic
  fallback to `space_gain_or_restriction` is removed.
- `StrategicIdeaSelector` is now a staged typed-evidence resolver:
  - Stage 1 chooses a thesis family first:
    `forcing_or_tactical_now`, `slow_structural`,
    `prevention_or_suppression`, or `conversion_or_transformation`
  - Stage 2 resolves the final unchanged 10-kind `StrategicIdeaKind` only
    inside the selected family/families
- Bridged plan / probe experiment summaries still modulate ranking, but
  `pv_coupled` move-order-sensitive experiments no longer blanket-block all
  slow strategic kinds. They now penalize / shape family and kind resolution
  instead of erasing legitimate structural theses by default.
- Follow-up retuning also made two pre-existing overdominant buckets more
  conservative:
  - `favorable_trade_or_transformation` now needs stronger structural /
    conversion backing before it outranks other strategic themes; the
    `IQPBlack` trade-down bridge now comes from existing typed move-ref / plan
    evidence instead of weak prose classification
  - `line_occupation` route / directional line-access bridges now score more
    conservatively, so weak multi-route accumulation does not overwhelm space /
    outpost / fixing signals by default
  - `target_fixing` no longer wins broad structural cases as easily; concrete
    line / outpost / space families can outrank plan-led fixation unless real
    fixation anchors are present
- Runtime route semantics remain unchanged:
  - hidden routes stay hidden
  - unsafe routes are not revived by idea selection
  - directional targets remain separate from routes and keep their existing
    readiness semantics
- Coverage of the 10 idea kinds is now typed:
  - `pawn_break` from pawn-break readiness / tension / file-opening consequences,
    with direct `PawnPlayAnalysis.advanceOrCapture`, `counterBreak`, and
    `tensionSquares` bridges
    - `2026-03-16` working-tree update:
      French counterbreak profile shortcuts no longer stay active in endgames by
      default; the selector now gates the `FrenchAdvanceChain` counterbreak / `f6`
      bridges behind non-endgame phase so material-up rook endings do not get
      mislabeled as late pawn-break theses
  - `space_gain_or_restriction` from space tags, clamp state, and mobility
    restriction, plus locked-center / space-race derived typed support
  - `target_fixing` from weak-square / color-complex / fixation signals and
    `WeakComplex` backfill
    - `2026-03-16` working-tree update:
      compensation-heavy openings now also bridge into `target_fixing` through a
      typed `compensation_target_fixation` source when all of the following hold:
      material deficit for the acting side, non-conversion phase, real weak-square
      or structural target evidence, and plan/line support such as
      `WeakPawnAttack`, `FileControl`, `Blockade`, `MinorityAttack`, or existing
      directional / line-access pressure
  - `line_occupation` from open-file / doubled-rook / route line-access signals
  - `outpost_creation_or_occupation` from outpost / strong-knight / entrenched
    piece signals, with route / directional bridges limited to tagged anchored
    outpost squares rather than generic forward-square geometry
  - `minor_piece_imbalance_exploitation` from bishop-pair / bad-bishop /
    good-bishop / piece-count imbalance signals
  - `prophylaxis` from structured prevented-plan evidence plus `ThreatAnalysis`
    defensive/prophylaxis signals; board-pattern cues such as bishop-pin /
    clamp watches now require real typed support (threat / prevented-plan /
    supported prophylaxis plan context) instead of standing alone
    - `2026-03-16` working-tree update:
      the weaker plan+board-pattern fallback is now disabled in compensation
      deficit contexts, so gambit / delayed-recovery positions do not get
      promoted to prophylaxis from geometry alone
  - `king_attack_build_up` from king-pressure / mate-net / hook / attack-lane
    signals plus typed motif bridges (`RookLift`, `Battery`, `PieceLift`,
    `Check`) and `threatsToThem`
    - `2026-03-16` working-tree update:
      when the side to move is down limited material but keeps a real
      initiative window, the selector now reuses existing typed carriers to
      bridge compensation into this family:
      `PositionFeatures.materialDiff`, development lag, king-castling state,
      exposed files / king-ring pressure, motifs, and route/directional attack
      lanes
      - concrete bridges:
        `compensation_development_lead`
        `compensation_king_window`
        `compensation_diagonal_battery`
      - this is still a typed-evidence reuse of existing plan / feature / motif
        outputs, not a new analyzer stack and not a new taxonomy kind
  - `favorable_trade_or_transformation` from removing-the-defender,
    classification simplify/convert windows, plan-alignment transformation
    reason codes, and winning-transition signals
  - `counterplay_suppression` from denied-break / counterplay-drop suppression
    signals, `opponentPawnAnalysis.counterBreak`, and `ThreatAnalysis`
  - `2026-03-16` working-tree update:
    `line_occupation` also now gets explicit compensation bridges when the side
    to move is down material but is intentionally delaying recovery in favor of
    open-line pressure:
    - reused typed inputs:
      `PositionFeatures.materialDiff`
      development lag
      line-control features
      existing major-piece route / directional-target access
      top plan matches such as `OpeningDevelopment`, `PieceActivation`,
      `RookActivation`, and `FileControl`
    - concrete bridges:
      `compensation_open_lines`
      `delayed_recovery_window`
    - these bridges feed both Bookmaker and Game Arc through the existing
      `StrategyPack -> StrategicIdeaSelector -> signalDigest/longTermFocus`
      path; no prose-only classifier was added
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
  - `2026-03-16` working-tree update:
    the bank is now split into:
    - `canonical` seeds for core structural families
    - `stockfishBalancedSupplemental` follow-ups for equal-material continuations
    - `stockfishCompensationAcceptance` for real material-deficit compensation
      positions
    - the compensation acceptance layer records:
      `stockfishScoreCp`
      `stockfishMaxAbsCp`
      `compensationSide`
      `sideToMoveMismatch`
      and explicit material-imbalance requirements
    - this acceptance layer is where Open Catalan / QID / Benko / Blumenfeld /
      Smith-Morra boundaries are now audited:
      when compensation should stay `king_attack_build_up`
      when it should stay `line_occupation`
      when it should stay `target_fixing`
      and when the evaluating side differs from the long-term compensation side
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
    - `PawnStorm_Kingside` latent seeding is now intentionally weak: it
      requires a locked center, a real kingside space edge, and a quiet
      tactical state before surfacing, and wrap-up prose now suppresses
      same-meaning `planName` / `seed` / evidence restatements for the same
      theme/subplan
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
      Game Arc path is improved but does not yet meet a strict
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
- `2026-03-18` working-tree update:
  - frontend probe-planning carriers now explicitly accept `strategyPack.signalDigest = null`
    from `GameChronicleResponse` payloads. This matches the current runtime
    contract instead of forcing `narrativeCtrl` callers to cast away a legal
    server value before probe refinement can run.
  - notebook dossier frontend validation now enforces the `productKind ->
    subject.role` invariant through the shared product-role map rather than
    duplicating branch-local literals, so the role contract remains aligned
    with the product taxonomy used by notebook consumers.
  - underboard PGN draft actions now pass a staged PGN override into
    `narrativeCtrl.fetchNarrative(...)` instead of always exporting the current
    board tree. This keeps the frontend Game Chronicle trigger truthful when
    the user has pasted a draft but has not imported it yet.
  - analysis direct entry with `?engine=` now strips only the transient engine
    query from the current URL via `history.replaceState`, preserving the
    active variant/FEN/import route instead of collapsing back to plain
    `/analysis`.
  - frontend labels that previously split between `Insights`,
    `Narrative Analysis`, and `Game Chronicle` are now normalized on the
    primary narrative surfaces to `Game Chronicle`, so the same LLM-backed
    product is not advertised under conflicting names.
- `2026-03-19` working-tree update:
  - full-game intro prose now uses plain text and metadata-aware fallback
    phrasing instead of the older dramatic template that emitted raw
    `**Unknown**`, result `*`, and `plies` wording into the review shell when
    PGN tags were missing.
  - short unfinished opening lines are now described as still being in the
    opening after `N moves`, and the intro focuses on highlighted moments
    rather than claiming a "sharp battle" or "critical turning points" when the
    game has not actually resolved into that kind of narrative.
  - frontend Game Chronicle payloads now persist in session storage under a
    hashed full-PGN context key and are restored on constructor/reload for the
    same analysis route. Hard refresh therefore preserves a completed review
    for the same game, while switching to a different PGN clears the stale
    Chronicle instead of replaying unrelated narrative state.
  - moment prose sanitization now strips residual markdown emphasis and raw
    `PlayedPV`-style carrier labels before the review shell renders them, and
    frontend badges/themes humanize raw camel-case/internal labels such as
    `OpeningBranchPoint` before display.
  - early opening moments now suppress the critical-branch engine digression
    and clamp the rendered body to a shorter budget, so move-2 / move-3 opening
    references do not balloon into multi-paragraph pseudo-drama before the game
    has actually left theory.
  - the review-shell `Moments` tab now renders a compact, frontend-sanitized
    prose summary instead of dumping the full raw Chronicle paragraph budget
    into narrow cards. The view strips markdown/control labels, converts
    lingering `ply N` wording into `move N`, cleans concept chips, and caps
    early-opening moment cards to a shorter sentence budget.
  - review-shell Chronicle hover previews are now wired on the shell itself,
    not just the standalone narrative document view. Inline move refs and route
    chips can therefore surface the commentary miniboard preview from the
    review-shell `Overview`/`Moments` surfaces as well.
  - Chronicle session persistence has been version-bumped so stale pre-sanitize
    payloads do not keep resurfacing after refresh. A hard refresh now drops
    old cached review payloads and rehydrates only the newer cleaned format.
- `2026-03-20` working-tree update:
  - Phase 2 generation alignment removed the remaining raw `plans.top5`
    plan-name fallback from thesis-bearing prose paths. Bookmaker thesis
    selection, outline strategic-stack beats, book-style wrap-up text, opening
    precedent plan hints, and Game Arc bridge sourcing now treat a plan name as
    thesis-worthy only when it is backed by the `mainStrategicPlans` partition
    and an `evidence_backed` `strategicPlanExperiments` match.
  - When runtime experiment metadata is absent, thesis helpers still fall back
    to `mainStrategicPlans` for older/manual test fixtures, but they no longer
    promote raw heuristic `plans.top5` names into user-facing main-plan prose.
  - If no evidence-backed main plan exists, strategic prose now falls back to
    non-plan framing already present in context (structure profile, opening
    theme, decision logic, compensation summary, latent-plan/hold metadata)
    rather than restating a weaker heuristic plan label.
  - `StructurePlanArcBuilder` now distinguishes evidence-backed named plans
    from alignment-intent fallbacks, so alignment prose is rendered as
    structure-directed guidance instead of "the long plan is X" unless the plan
    is actually backed.
  - Game Arc hybrid bridge now reuses the validated strategic thesis claim
    first, otherwise an already-validated outline sentence, and only then the
    neutral/default bridge. It no longer synthesizes an independent strategic
    bridge sentence from raw strategy-pack or signal-digest heuristics.
- Verification:
  - `ui/analyse/src/narrative/probePlanning.ts`
  - `ui/analyse/src/notebookDossier.ts`
  - `ui/analyse/src/view/components.ts`
  - `ui/analyse/src/narrative/narrativeCtrl.ts`
  - `ui/analyse/src/narrative/narrativeView.ts`
  - `ui/analyse/src/ctrl.ts`
  - `ui/analyse/tests/narrativeProbePlanning.test.ts`
  - `ui/analyse/tests/notebookDossier.test.ts`
  - `ui/analyse/tests/frontendAuditRegression.test.ts`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeLexicon.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicNarrativePlanSupport.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StructurePlanArcBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/OpeningPrecedentBranching.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeGenerator.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeLexiconTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala`
- `2026-03-20` working-tree update:
  - Phase 3 added an explicit `variant` carrier on both single-position
    Bookmaker requests and full-game Game Chronicle requests, then normalized
    that value into `NarrativeContext.variantKey`. Missing or unsupported
    request values now default to `standard`; `chess960`, `freestyle`, and
    `960` normalize to the Chess960 bucket.
  - A new centralized `EarlyOpeningNarrationPolicy` now gates very early
    opening verbosity. The collapse path activates only for `standard`
    openings at `ply <= 10` when there is no stronger explanatory event.
    `OpeningEvent.Intro` is now treated as weak contextual metadata and no
    longer authorizes rich prose on its own.
  - Strong escape hatches are limited to concrete events already carried by
    the pipeline: `BranchPoint`, `OutOfBook`, `TheoryEnds`, `Novelty`,
    forced/critical states, strong tactical pressure, or severe
    counterfactuals. Chess960 is explicitly excluded from this default clamp.
  - Under the collapsed standard-opening path, Bookmaker now suppresses
    opening-precedent comparison text, strategic distribution beats, and
    generic wrap-up beats, then clamps the final user-facing prose to at most
    two sentences. This keeps early standard openings terse unless a real
    theory split or tactical event justifies expansion.
  - Game Arc now uses the same collapse policy for moment rendering. Early
    standard opening moments skip critical-branch insertion, avoid rebuilding
    a larger preface around the shortened text, and clamp the final moment
    narrative to at most two sentences across the assembled output.
  - Phase 1/2 contracts remain unchanged in this phase: no plan-ranking
    changes, no evidence-policy changes, and no response-schema additions.
- Verification:
  - `app/controllers/LlmController.scala`
  - `modules/llm/src/main/LlmApi.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/EarlyOpeningNarrationPolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
  - `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala`
  - `modules/llm/src/main/scala/lila/llm/models.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/EarlyOpeningNarrationPolicyTest.scala`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/src/bookmaker/requestPayload.ts`
  - `ui/analyse/src/narrative/narrativeCtrl.ts`
  - `ui/analyse/src/narrative/requestPayload.ts`
  - `ui/analyse/tests/requestPayload.test.ts`
- `2026-03-20` working-tree update:
  - Temporal / Branch Provenance Hardening v2 narrowed `FactScope.Now` to
    current-board-verifiable facts only. The runtime now distinguishes
    `Now`, `MainPv`, `ThreatLine`, `Counterfactual`, and `CandidatePv`
    provenance, and only `Now` facts may feed unqualified board-anchor,
    key-fact, or present-tense prose.
  - `NarrativeContext` now carries line-scoped facts separately from generic
    current-board facts. Main-PV, threat-line, and counterfactual motifs are
    preserved as internal-only carriers, while candidate facts are explicitly
    tagged as `CandidatePv`. This prevents future-line motifs from silently
    re-entering generic context through the shared `facts` pool.
  - `FactExtractor.fromMotifs(..., FactScope.Now)` is now strict for
    line-sensitive tactical/structural motifs. `Pin`, `Skewer`, `Fork`, and
    related fact families no longer reuse motif-role fallbacks when the root
    board cannot verify the pieces directly. Future-only motifs therefore stay
    line-scoped instead of being restated as current-board facts.
  - Future-line prose can no longer rely on source labels alone. Main-PV,
    threat/refutation, counterfactual, and candidate claims now require a
    concrete SAN citation in the same sentence. Tactical motifs require SAN
    coverage through the motif-trigger move with a bounded token budget;
    non-tactical line claims still require a shorter SAN prefix. If the line
    cannot be rendered safely, the claim is suppressed rather than paraphrased
    as generic prose.
  - Threat/refutation-derived prophylaxis is now treated as line-scoped
    support, not thesis text. `current_board` prophylaxis may still surface as
    a generic lens, but threat-line prophylaxis is barred from paragraph-1
    claim slots and only survives when the supporting SAN line is rendered in
    the same sentence.
  - Counterfactual-only motifs no longer mix into generic context beats,
    board anchors, bridge prose, or canonical motif summaries. They remain
    available only in explicit teaching/comparison/evidence zones, where they
    must also carry SAN citation. This closes the path where missed-line forks,
    pins, or other tactical motifs could be rephrased as the ambient character
    of the current position.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/model/Fact.scala`
  - `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala`
  - `modules/llm/src/main/scala/lila/llm/model/strategic/StrategicModels.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/LineScopedCitation.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/FactExtractor.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CounterfactualAnalyzer.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicFeatureExtractorImpl.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StructurePlanArcBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/strategic/StrategicAnalyzers.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/LineScopedCitationTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/FactExtractorTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeContextBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StandardCommentaryClaimPolicyTest.scala`
- `2026-03-20` working-tree update:
  - Branch provenance is now carried through renderer selection, not just
    producer and outline construction. `OutlineBeat` gained internal
    `branchScoped` and `supportKinds` metadata so focus selection can treat
    cited branch claims as dependent on the evidence surface that justifies
    them.
  - `focusMomentOutline` no longer drops `Evidence` and `Alternatives` beats
    wholesale. It now preserves support beats when a surviving branch-scoped
    claim depends on them, and if the support beat cannot fit in the moment
    budget the claim is dropped first. This enforces the player-facing rule
    that inline SAN provenance survives with the claim or the claim is removed.
  - Hybrid moment assembly now suppresses lead/bridge prefaces whenever the
    focused body already contains branch-scoped cited prose or a cited critical
    branch block. This removes the old `claim before qualifier` ordering where
    generic framing appeared before the concrete SAN line that actually backed
    the point.
  - Evidence branch identity is no longer deduped by head SAN alone.
    `LineScopedCitation` now builds stable SAN-prefix signatures from concrete
    branch lines, so two continuations that share the same first move but
    diverge later stay distinct in decision questions and evidence beats.
  - Decision/evidence surfaces now render SAN-prefix lines directly instead of
    first-move labels or source-label-only phrasing. Generic meta text such as
    `Probe evidence says ...` is suppressed unless the underlying text already
    carries inline SAN citation.
  - Branch-derived prophylaxis remains available in main prose only when cited,
    but its threat-line form no longer reappears in signal-digest sidecars as
    generic `cuts out ...` summary text. The digest now exposes prophylaxis
    summary fields only for `current_board` prophylaxis, leaving threat-line
    provenance to the cited main narrative and explicit evidence surfaces.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/model/authoring/NarrativeOutline.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BranchScopedSentencePolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/LineScopedCitation.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeSignalDigestBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/LineScopedCitationTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BranchProvenanceRegressionTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala`
- `2026-03-20` working-tree update:
  - Legacy prose-expanding fallback targets have been removed from the live
    Bookmaker and Chronicle paths. Bookmaker no longer falls back from missing
    slots to raw validated-outline prose, Chronicle no longer falls back from
    failed compression to the old hybrid renderer, and polish failure now
    returns deterministic compressed prose derived from structured slots rather
    than the original widened draft.
  - `LiveNarrativeCompressionCore` now owns the shared live compression
    contract for both surfaces. Claim-family priority, system-language bans,
    player-language rewrites, deterministic slot rendering, and compressed
    fallback reconstruction now come from one backend module instead of
    product-specific policy forks.
  - Runtime bookkeeping prose has been removed from the main narrative path.
    `buildStrategicDistributionBeat` is deleted from outline construction, wrap
    up no longer restates threat warnings or hypothesis-difference bookkeeping,
    and the dormant legacy wrap-up renderer has been narrowed to
    player-facing practical/compensation summaries only.
  - Signal-digest and strategy-pack bookkeeping no longer feed main prose.
    Live narrative generation now blocks latent-plan bookkeeping, hold/refute
    summaries, authoring-evidence summaries, and related stack/precondition
    metadata from re-entering Bookmaker/Chronicle body text through
    `StrategyPackBuilder` and `StrategyPackSurface`.
  - Default frontend support UI now follows a compact player-facing contract.
    Bookmaker and Chronicle default panels keep only main plans with support
    badges, decision compare, `Why it stayed conditional`, and compact Opening
    / Opponent / Structure / Piece deployment / Practical rows. Evidence
    probes, authoring evidence, branch dossiers, strategic notes, latent-plan
    bookkeeping, and related analyst-console material move behind one collapsed
    `Advanced details` disclosure or remain internal-only.
  - Chronicle now follows the same default product philosophy as Bookmaker:
    compressed prose first, one compact support panel second, and optional
    collapsed detail surfaces only after that. This is a UX convergence pass,
    not an API contract change.
- Verification:
  - `modules/llm/src/main/LlmApi.scala`
  - `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/LiveNarrativeCompressionCore.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerLiveCompressionPolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/GameChronicleCompressionPolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/src/narrative/narrativeView.ts`
  - `ui/analyse/src/chesstory/compactSupportSurface.ts`
  - `ui/analyse/tests/compactSupportSurface.test.ts`
  - `ui/analyse/tests/frontendAuditRegression.test.ts`
- `2026-03-20` working-tree update:
  - The player-language rewrite now happens at the source idea layer, not only
    as downstream cleanup. `StrategicIdeaSelector` gained
    `playerFacingIdeaText`, which renders break / pressure / exchange /
    outpost / target-fixing ideas with concrete anchors (`the ...e5 break`,
    `pressure on d5`, `exchanges on c3`, `an outpost on d5`) instead of the
    old `kind around focus` scaffold.
  - `StrategyPackSurface` now consumes those player-facing idea summaries
    directly. `StrategicThesisBuilder.ideaText`, `targetText`, and
    `ActiveStrategicCoachingBriefBuilder.primaryIdeaLabel` no longer surface
    `pawn break around ...`, `pressure toward ...`, `exchanges around ...`,
    or `making c4 available for the knight` style text in default prose.
  - Decision/support phrasing has been shifted further toward coach language.
    `StrategicThesisBuilder` now renders support/objective surfaces as
    `A likely follow-up is ...`, `The knight can head for ... next`, `The move
    prepares ...`, `The move increases pressure on ...`, or `The move leans
    toward exchanges on ...` instead of generic `The move is really about ...`
    or `The next useful route/target is ...` scaffolding.
  - Support placeholders are now suppressed instead of being reworded into a
    different meta sentence. `LiveNarrativeCompressionCore`,
    `UserFacingSignalSanitizer`, and frontend `signalFormatting.ts` remove
    `still needs more concrete support` / `the idea still needs concrete
    support` from default user-facing surfaces rather than preserving the
    underlying evidence-bookkeeping state in prose.
  - Frontend support labels now humanize strategic plan names before display.
    `planSupportSurface.ts` and `signalFormatting.ts` normalize labels such as
    `Preparing e-break Break`, `Piece Activation`, and `Opening Development and
    Center Control` before they reach the compact support panel, while the
    compact panel continues to drop rows that remain non-player-facing after
    rewriting.
  - Spot-check reruns on the `club_000` shard confirmed the main prose gains:
    Bookmaker dropped `pawn break around` from `6 -> 0`, `pressure toward`
    from `4 -> 0`, `exchanges around` from `2 -> 0`, and concrete-support
    placeholder text from `24 -> 0`. Chronicle report prose also dropped
    `king-attack build-up around`, `outpost creation or occupation around`,
    and concrete-support placeholders to `0`, while now surfacing the shared
    `The key idea is ...` / `A likely follow-up is ...` / `A concrete target
    is ...` language family.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicIdeaSelector.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/LiveNarrativeCompressionCore.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/UserFacingSignalSanitizer.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `ui/analyse/src/chesstory/signalFormatting.ts`
  - `ui/analyse/src/bookmaker/planSupportSurface.ts`
  - `ui/analyse/tests/compactSupportSurface.test.ts`
- `2026-03-20` working-tree update:
  - User-facing support and advanced-row label cleanup now happens in the same
    backend/player-facing sanitation path as prose cleanup. Raw label families
    such as `Preparing e-break Break`, `Preparing d-break Break`,
    `Opening Development and Center Control`, `Piece Activation`,
    `Exploiting Space Advantage`, `Simplification into Endgame`, and
    `Immediate Tactical Gain Counterplay` are rewritten into compact chess
    language before they reach Bookmaker support rows, Chronicle support rows,
    or the external QA pipeline.
  - `UserFacingSignalSanitizer` now rewrites those label families directly and
    `UserFacingPayloadSanitizer` carries the same cleanup through payload
    serialization, so user-facing rows and payload dumps stop exposing raw
    strategic taxonomy names even when the underlying carriers still use them.
  - The QA corpus tooling mirrors that same contract. `CommentaryPlayerQcSupport`
    now sanitizes main-plan labels, `whyAbsent` rows, opening/opponent/structure
    summaries, deployment text, practical notes, and advanced metadata rows
    before writing review manifests, so manual PGN review sees the same
    player-facing language the product shows.
  - Compensation phrasing has been pushed another step away from system-summary
    wording. `StrategicThesisBuilder.CompensationDisplayPhrasing` now prefers
    coach-style language such as `The material can wait`, `winning the material
    back`, `use the queenside files`, `use the central files`, `force the
    favorable exchanges first`, and `keep the break ready` instead of older
    `recover the material`, `file pressure alive`, or bookkeeping-style
    conversion phrasing.
  - This pass does not remove long-term ideas; it translates them into concrete
    player language. Compensation text is now expected to say what was given,
    what concrete pressure or initiative was gained, and why immediate material
    recovery can wait, rather than narrating internal evaluation state.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/UserFacingSignalSanitizer.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/UserFacingPayloadSanitizerTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerQcSupportTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CompensationDisplayPhrasingTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `ui/analyse/src/chesstory/signalFormatting.ts`
- `2026-03-21` working-tree update:
  - Compensation claim/support text is now rendered as a small clause-shaped
    skeleton instead of raw string concatenation. The active Bookmaker and
    Chronicle compensation path in `StrategicThesisBuilder` now limits itself
    to `main reason + optional follow-up + optional practical explanation`, so
    broken hybrids such as `via queen toward e4` and
    `while aiming for the knight can head for d4` are no longer emitted.
  - `CompensationDisplayPhrasing` now prefers anchored compensation windows
    from existing typed carriers (`compensationSummary`, objective text,
    execution text) before falling back to generic `initiative`/`attack`
    wording. This keeps compensation prose tied to concrete targets, files, or
    follow-up squares when they are available.
  - The remaining raw fallback in `NarrativeOutlineBuilder` also now prefers
    concrete compensation carriers such as open lines, fixed targets, or
    ongoing attack pressure over the older `keep the compensation justified`
    summary.
  - Compensation-context support rows remain available, but abstract generic
    labels are now hidden unless they can be rewritten with a concrete anchor.
    In practice this means rows such as generic piece-placement or counterplay
    summaries drop out of the default support panel when they cannot name a
    real piece, square, file, or break.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/UserFacingSignalSanitizer.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CompensationDisplayPhrasingTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerQcSupportTest.scala`
  - `tmp/commentary-player-qc/runs/bookmaker/post_patch6_comp_focus/bookmaker_outputs.jsonl`
  - `tmp/commentary-player-qc/runs/bookmaker/post_patch6_master_000/bookmaker_outputs.jsonl`
  - `tmp/commentary-player-qc/runs/chronicle/post_patch6_master_000/report.md`
- `2026-03-21` working-tree update:
  - Compensation prose now collapses repeated recovery ideas before rendering.
    Claim/support pairs such as `material can wait` plus another `winning the
    material back can wait` sentence no longer survive together by default,
    and duplicate square-follow-up phrasing such as `pressure on d3` plus
    `bringing the queen to d3` is pruned in the compensation skeleton.
  - Weak compensation shells no longer force the compensation lens. When the
    strategy-pack carrier can name invested material but cannot concretely say
    what was gained or why recovery can wait, `buildCompensation` now reframes
    the moment through an ordinary decision-style claim instead of emitting a
    generic compensation sentence.
  - Semantic compensation without a strategy-pack carrier remains eligible for
    compensation narration. The v2 gating now distinguishes between
    surface-driven compensation prose and semantic-only compensation so that
    the older semantic compensation path is not accidentally suppressed.
  - Chronicle focused compensation prose now matches Bookmaker's gating more
    closely because both consume the same `StrategicThesisBuilder`
    compensation/reframe decision, rather than Chronicle keeping a separate
    report-like compensation lead when the surface is too weak.
  - Compensation-context support rows are stricter on both backend and
    frontend. Generic attack/initiative bookkeeping no longer counts as a
    concrete anchor, while real files, squares, routes, or named follow-up
    moves still survive in user-facing support.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/UserFacingSignalSanitizer.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CompensationDisplayPhrasingTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerQcSupportTest.scala`
  - `ui/analyse/src/chesstory/signalFormatting.ts`
  - `ui/analyse/tests/compactSupportSurface.test.ts`
- `2026-03-21` working-tree update:
  - Compensation / player-language code has been structurally refactored
    without changing the Bookmaker, Game Chronicle, support-panel, public API,
    or payload-schema contract.
  - `StrategicThesisBuilder` is now reduced to orchestration. It still chooses
    lens order, compensation-vs-reframe behavior, and ordinary thesis
    assembly, but the large nested helper objects are no longer the ownership
    boundary for display wording.
  - `StrategyPackSurface` now owns strategy-pack snapshot building, display
    normalization, and compensation subtype derivation in one dedicated module.
  - `CompensationDisplayPhrasing` is now the single backend owner for
    compensation-specific phrasing, eligibility gating, anchored support
    selection, subtype-specific wording, follow-up rendering, and duplicate
    support pruning.
  - `StrategicSentenceRenderer` now owns the shared piece-pattern and
    clause-rendering helpers used by both ordinary thesis wording and
    compensation follow-up rendering. The duplicate local/global
    `Piece*Pattern` and execution-follow-up implementations were collapsed
    into this one helper.
  - Backend/frontend support filtering now has an explicit authority split:
    backend `PlayerFacingSupportPolicy` is the source of truth for label
    cleanup and compensation-context support gating, while frontend
    `signalFormatting.ts` mirrors that contract for persisted/client-composed
    rows.
  - Drift protection is now explicit. Shared fixture
    `modules/llm/src/test/resources/playerFacingSupportContract.json` is
    consumed by both Scala and frontend tests so label cleanup and
    compensation-context filtering remain aligned.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackSurface.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/CompensationDisplayPhrasing.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicSentenceRenderer.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/PlayerFacingSupportPolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/UserFacingSignalSanitizer.scala`
  - `ui/analyse/src/chesstory/signalFormatting.ts`
  - `ui/analyse/src/chesstory/compactSupportSurface.ts`
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/src/narrative/narrativeView.ts`
  - `modules/llm/src/test/resources/playerFacingSupportContract.json`
  - `modules/llm/src/test/scala/lila/llm/analysis/PlayerFacingSupportPolicyContractTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CompensationDisplayPhrasingTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala`
  - `modules/llm/src/test/scala/lila/llm/UserFacingPayloadSanitizerTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerQcSupportTest.scala`
  - `ui/analyse/tests/playerFacingSupportContract.test.ts`
  - `ui/analyse/tests/compactSupportSurface.test.ts`
  - `ui/analyse/tests/frontendAuditRegression.test.ts`
- `2026-03-21` working-tree update:
  - Ordinary thesis selection is now more purpose-conformant for Bookmaker and
    Chronicle. `StrategicThesisBuilder` now lets a concrete
    `strategyPack.surface` idea become the main thesis before dropping into
    generic decision/opening filler, and the decision lens now keeps
    focal/anchored support in the first support slots instead of letting
    conditionality boilerplate crowd it out.
  - Broad or weakly anchored surface ideas are now downgraded before they reach
    the player-facing main claim. In practice this means overloaded `pressure`
    claims without matching objective/execution anchors no longer survive as the
    headline thesis just because the surface named them.
  - Live Bookmaker compression now explicitly prefers anchored support over
    low-value opening filler. Generic sentences such as `finish development
    without giving up the center` are treated as low-value tension/filler, while
    anchored support or decision-specific tension survives into the third slot
    first.
  - QA review tooling now audits the same gap directly. `CommentaryPlayerQcSupport`
    adds explicit review flags for:
    - `generic_filler_main_prose`
    - `anchored_support_missing_from_prose`
    - `conditionality_blur`
    - `taxonomy_residue:*`
    This makes current must-fix Bookmaker/Chronicle failures reviewable without
    changing the public payload/API contract.
  - Real-PGN Chronicle signoff now emits explicit `mustFixFailures` instead of
    only aggregate counts. The report/JSON surface now records positive
    exemplar misses, negative-guard failures, cross-surface parity misses,
    subtype mismatches, and path-vs-payoff divergence as concrete blocker rows,
    plus a simple `releaseGatePassed` boolean.
  - Strategic Puzzle now has a dedicated reveal audit path:
    `StrategicPuzzleRevealAuditRunner` / `StrategicPuzzleRevealAuditSupport`.
    This audits stored terminal reveal prose against the same player-facing
    purpose rubric by flagging:
    - `false_claim_risk`
    - `provenance_blur`
    - `missing_anchor`
    - `strategic_flattening`
    The new audit uses saved `runtimeShell` reveal text, so puzzle reveal
    conformance can now be signed off separately from prompt-only QA.
- Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerLiveCompressionPolicy.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/LiveNarrativeCompressionCore.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerQcSupport.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerQcSupportTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalRunner.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/StrategicPuzzleRevealAuditSupport.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/StrategicPuzzleRevealAuditSupportTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/StrategicPuzzleRevealAuditRunner.scala`
- `2026-03-21` working-tree update:
  - Compensation conformance is now audited against an explicit canonical
    internal contract. `StrategyPackSurface` owns the strict compensation
    subtype / path / payoff / selected-display decision, and the public
    Chronicle / Bookmaker schema stays unchanged. The important change is that
    strict compensation signoff is now judged from this shared internal
    contract instead of letting each surface drift through raw fallback labels.
  - `StrategicBranchSelector` now keeps evidence-backed strict compensation
    moments visible ahead of ordinary fallback strategic moments, so
    compensation-positive turning points are less likely to be displaced by
    generic thread fillers.
  - Active-note parity is no longer treated as optional for compensation
    moments. `ActiveStrategicNoteValidator` rejects compensation-positive notes
    that do not mention the canonical compensation family, but it now allows a
    compensation note to pass without a separate opponent-warning sentence if
    the compensation family and forward plan are explicit. `LlmApi` also now
    tries a strict-compensation deterministic rescue note before dropping the
    active surface entirely.
  - Positive compensation exemplar signoff is now grounded in real PGNs rather
    than synthetic mini-lines. `RealPgnPositiveCompensationExemplarBuilder`
    carves a dedicated exemplar corpus out of the selected 360-game manifest,
    and `RealPgnNarrativeEvalRunner.PositiveCompensationExemplars` now points
    at six real, currently compensation-positive moment keys.
  - Empirical status after this pass:
    - dedicated positive exemplar rerun:
      `falseNegativeCount=0`, `positiveExemplarCoverage=6/6`,
      `crossSurfaceAgreementRate=100%`, `releaseGatePassed=true`
    - offender-heavy shard reruns show the original four blocker families have
      mostly collapsed into one residual family:
      - `edge_case_000`: must-fix `72 -> 9`
      - `master_classical_000`: must-fix `48 -> 6`
      - `master_classical_080`: must-fix `91 -> 9`
      - `titled_practical_000`: must-fix `56 -> 9`
      - `titled_practical_040`: must-fix `54 -> 8`
      - across all five reruns, `positive_exemplar_missed=0`,
        `subtype_mismatch=0`, and blocker-grade
        `path_payoff_divergence=0`; the remaining must-fix rows are all
        `cross_surface_parity` caused by active-note omission on a smaller set
        of compensation moments
    - path/payoff disagreement still appears as a diagnostic metric in shard
      reports, but unresolved divergence is no longer the dominant blocker in
      these reruns; the remaining release-gate pressure is concentrated in
      active-note parity.
  - Verification:
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackSurface.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/StrategicBranchSelector.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicNoteValidator.scala`
  - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
  - `modules/llm/src/main/LlmApi.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalRunner.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnPositiveCompensationExemplarBuilder.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnPositiveCompensationExemplarBuilderTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalSignoffTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicNoteValidatorTest.scala`
  - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
  - `modules/llm/docs/RealPgnNarrativeEvalPositiveCompensationExemplars.json`
  - `tmp/commentary-player-qc/positive_exemplar_check/report.md`
  - `tmp/commentary-player-qc/manifests/comp_sprint_runs/edge_case_000/report.md`
  - `tmp/commentary-player-qc/manifests/comp_sprint_runs/master_classical_000/report.md`
  - `tmp/commentary-player-qc/manifests/comp_sprint_runs/master_classical_080/report.md`
  - `tmp/commentary-player-qc/manifests/comp_sprint_runs/titled_practical_000/report.md`
  - `tmp/commentary-player-qc/manifests/comp_sprint_runs/titled_practical_040/report.md`
- `2026-03-22` working-tree update:
  - The deterministic 202-game audit queue now preserves `auditId` on every
    review row, so singled-out residual exemplars remain distinct from the 200
    shard-derived games even when `gameId` overlaps. The audit/merge tooling
    still keys judgments by `sampleId`, but the queue now carries the
    higher-level audit-game identity needed for reliable manual review and
    exemplar bundling.
  - Active-note compensation parity selection now follows the resolved display
    contract rather than the narrower strict-compensation subset. In practice,
    `attachActiveStrategicNotes` uses
    `activeCompensationParityEligible(...)`, which accepts resolved
    compensation candidates whenever:
    - `compensationPosition = true`
    - `compensationContractResolved = true`
    - `resolvedDisplaySubtypeSource != raw_fallback`
    - a canonical compensation subtype label exists
    This widens attachment to contract-resolved transition moments without
    loosening truth checks.
  - Active-note validation still keeps compensation-family mention as a hard
    requirement for compensation-positive notes, but grounded non-compensation
    notes can now pass without a separate opponent-warning sentence when they
    already carry:
    - the dominant idea
    - a concrete anchor
    - a forward continuation
    This is intended to reduce avoidable active-note omission on route-led
    strategic moments while keeping anchorless or generic notes rejected.
  - Bookmaker objective phrasing now frames route ideas as availability rather
    than as current-board facts:
    - `The queen can head for c3 next.` -> `A likely route keeps the queen headed for c3.`
    - `The move keeps the queen pointed toward c3.` -> `The move keeps the queen route toward c3 available.`
    The third-paragraph variation hook is also softened from `A concrete line
    is ...` to `One concrete line that keeps the idea in play is ...`, which
    better matches the product's conditionality contract for candidate ideas.
  - Chronicle whole-game conclusions now avoid the stock
    `ultimately prevailed, capitalizing on the critical moments` boilerplate.
    The conclusion instead anchors itself in the lead theme and the practical
    error profile:
    - clean games emphasize the lead strategic thread
    - volatile games explicitly mention blunder / missed-win counts
    This does not yet solve the full whole-game narrative problem, but it
    reduces generic `TensionPeak`-style boilerplate in the game-level verdict.
  - Shared rewrite guardrails now remove several recurring low-value sentence
    shells across Bookmaker, Chronicle, and active-note prose before they
    reach player-facing text:
    - `keeps the pieces coordinated`
    - `keeps the position easy to handle`
    - `holds the position together`
    - `X is the square that keeps ... grounded`
    - `The next step is to keep bringing the ...`
    These paths now prefer existing `objective`, `execution`, `dominant idea`,
    `latent plan`, and compensation-family anchors when they are available. If
    there is no concrete anchor, the prose is downgraded instead of hiding the
    gap behind generic filler.
  - Bookmaker structure claims now preserve evidence-backed strategic labels
    when the structure actually supports them. In practice, structure-led prose
    now keeps:
    - the structure name
    - the evidence-backed plan label when it is truly backed
    - the concrete deployment anchor
    This fixes cases such as Carlsbad / minority-attack positions where the
    old surface flattened the claim into generic route or reorganization prose
    even though the structure and move clearly pointed to a named strategic
    plan.
  - Compensation phrasing was tightened to preserve provenance and concrete
    anchors without falling back to the old `material can wait` boilerplate as
    the default narration. The current contract is:
    - prefer `The move gives up material because ...`
    - follow with `That only works while ...`
    - keep subtype-specific support concrete (`queenside targets`, `central
      files`, `favorable exchanges`, etc.)
    - allow raw-fallback compensation surfaces to keep raw anchored idea text
      when the path/payoff contract is not stable enough to promote normalized
      display prose
    This keeps compensation language aligned with the stricter
    purpose-conformance audit while still allowing playable non-best ideas to
    be described when they have real anchors.
  - Chronicle audit tooling now exposes more of the whole-game narrative
    contract directly in the review queue. Whole-game review rows include
    opening/middlegame/endgame story summaries, side plans, turning-point
    anchors, and punishment summaries. The short-game fallback is also tighter:
    a game is only treated as `non-strategic short game` when it is genuinely
    too short or strategically empty, instead of collapsing any thin game arc
    into the fallback bucket.
  - Verification:
    - `modules/llm/src/main/LlmApi.scala`
    - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
    - `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicNoteValidator.scala`
    - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerLiveCompressionPolicy.scala`
    - `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
    - `modules/llm/src/main/scala/lila/llm/analysis/CompensationDisplayPhrasing.scala`
    - `modules/llm/src/main/scala/lila/llm/analysis/LiveNarrativeCompressionCore.scala`
    - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeLexicon.scala`
    - `modules/llm/src/main/scala/lila/llm/analysis/StrategicSentenceRenderer.scala`
    - `modules/llm/src/main/scala/lila/llm/analysis/StrategicThesisBuilder.scala`
    - `modules/llm/src/test/scala/lila/llm/LlmApiActiveParityTest.scala`
    - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilderTest.scala`
    - `modules/llm/src/test/scala/lila/llm/analysis/ActiveStrategicNoteValidatorTest.scala`
    - `modules/llm/src/test/scala/lila/llm/analysis/CompensationDisplayPhrasingTest.scala`
    - `modules/llm/src/test/scala/lila/llm/analysis/BookmakerPolishSlotsTest.scala`
    - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeLexiconTest.scala`
    - `modules/llm/src/test/scala/lila/llm/analysis/StrategicSentenceRendererTest.scala`
    - `modules/llm/src/test/scala/lila/llm/analysis/StrategicThesisBuilderTest.scala`
    - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerAuditWorkflowTest.scala`
    - `modules/llm/src/test/scala/lila/llm/tools/CommentaryPlayerQcSupportTest.scala`

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
- `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala`
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
