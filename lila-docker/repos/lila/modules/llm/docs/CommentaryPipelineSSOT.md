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
  - `PolishPrompt.systemPrompt`, `buildPolishPrompt`, and `buildRepairPrompt`
    now instruct polish/repair models not to emit UI section headers, to
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
  - `ui/analyse/src/narrative/narrativeView.ts:448`
  - `ui/analyse/src/bookmaker.ts:130`
  - `modules/llm/src/test/scala/lila/llm/analysis/CommentaryEngineFocusSelectionTest.scala:8`
  - `modules/llm/src/test/scala/lila/llm/ActiveStrategicPromptTest.scala:5`
  - `modules/llm/src/test/scala/lila/llm/analysis/StrategyPackBuilderTest.scala:205`
  - `modules/llm/src/test/scala/lila/llm/analysis/NarrativeSignalConsumptionTest.scala:144`

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
