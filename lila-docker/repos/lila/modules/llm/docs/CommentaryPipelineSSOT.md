# Commentary Pipeline SSOT

This file is the canonical audit for the current Chesstory commentary-analysis
pipeline.

It intentionally describes the latest live pipeline and deletes the older
dated update-log style material. Historical side paths are not canonical.

## Authority

- Canonical audit for commentary-analysis in `modules/llm`
- Canonical audit for backend prompt/polish behavior that affects commentary
- Canonical audit for `app/controllers/LlmController.scala` and
  `ui/analyse/src` consumption when those paths surface commentary data

If code and this document disagree, the code is stale until this document is
updated in the same change.

## Scope

Current canonical scope includes:

- strategic / semantic / endgame / opening / probe / plan / pawn / structure /
  threat / practicality / counterfactual helper modules
- Chronicle / Game Arc generation
- Bookmaker context, rendering, slot building, optional polish, and frontend
  consumption
- Active-note selection, deterministic draft generation, optional polish, and
  frontend consumption
- prompt-bearing surfaces that affect commentary behavior
- signoff / audit entry points used by `RealPgnNarrativeEvalRunner`

Out of scope:

- removed legacy QC / BookCommentary / debug / shadow / structure-endgame side
  workflows
- temporary rerun outputs under `tmp/commentary-player-qc/...`
- historical design notes that no longer describe the current runtime

## Current Principles

### 1. Deterministic-first canonical path

- Canonical decisive-move truth is derived before prose generation.
- Canonical Chronicle / Game Arc behavior is deterministic.
- Canonical Bookmaker draft generation is deterministic.
- Canonical Active-note attach / omit behavior is deterministic.
- LLM polish may improve wording, but it does not own strategic truth or note
  existence on the signoff path.

### 1A. Truth-first move fact layer

- Per key moment, the canonical runtime derives an internal
  `MoveTruthFrame` before Chronicle / Bookmaker / Active prose is built.
- `MoveTruthFrame` is the fact layer; `DecisiveTruthContract` is only the
  surface projection consumed by selection and rendering.
- The frame classifies seven fact families:
  - `MoveQualityFact`
    - `best / acceptable / inaccuracy / mistake / blunder / missed_win`
    - cp-loss
    - win-chance loss / severity band
  - `BenchmarkFact`
    - verified best move
    - only-move / unique-good-move
    - benchmark naming permission
  - `TacticalFact`
    - immediate tactical refutation
    - forcing line / forced mate / forced draw resource
    - tactical motifs carried from verified facts
  - `MaterialEconomicsFact`
    - actual current-move material delta
    - sacrifice kind
    - value-down capture / recoup / overinvestment / uncompensated loss
  - `StrategicOwnershipFact`
    - `first_investment_commitment`
    - `compensation_maintenance`
    - `conversion_followthrough`
    - verified payoff anchor / chain key / fresh current-move evidence
  - `PunishConversionFact`
    - immediate punishment vs latent punishment
    - conversion route / concession summary
  - `DifficultyNoveltyFact`
    - depth-sensitive move
    - only-move defense
    - shallow-underestimated move
- The canonical projection from `MoveTruthFrame` into
  `DecisiveTruthContract` binds:
  - played move
  - verified best move when one is safely known
  - truth class
  - truth phase / ownership for investment families
  - cp-loss / swing severity
  - decisive reason family
  - whether naming a concrete benchmark is allowed
- For investment families, the decisive-truth contract distinguishes:
  - `first_investment_commitment`
  - `compensation_maintenance`
  - `conversion_followthrough`
- The decisive-truth contract also resolves four separate internal policies:
  - `TruthOwnershipRole`
    - `commitment_owner`
    - `maintenance_echo`
    - `conversion_owner`
    - `blunder_owner`
    - `none`
  - `TruthVisibilityRole`
    - `primary_visible`
    - `supporting_visible`
    - `hidden`
  - `TruthSurfaceMode`
    - `investment_explain`
    - `maintenance_preserve`
    - `conversion_explain`
    - `failure_explain`
    - `neutral`
  - `TruthExemplarRole`
    - `verified_exemplar`
    - `provisional_exemplar`
    - `non_exemplar`
- The projected contract also binds:
  - whether the surfaced move owns the decisive explanation
  - the verified payoff anchor
  - whether compensation prose is allowed
  - whether benchmark prose is allowed
- serialized truth fields on `GameArcMoment` / `GameChronicleMoment` are
  compatibility/debug projections only. Runtime selection, whole-game binding,
  and signoff-path control flow must consume canonical `MoveTruthFrame` /
  `DecisiveTruthContract`, not re-derive truth from serialized strings.
- visible exemplar preservation is separate from ownership and from
  compensation-positive prose permission:
  - `verified_exemplar` may own and explain the decisive investment
  - `provisional_exemplar` may stay visible while surface mode remains
    `neutral`
  - `non_exemplar` rows may not inherit investment ownership from stale shells
- Surface layers may become less specific than the contract, but they may not
  contradict it.
- If the runtime cannot verify a concrete benchmark, prose must stay compact and
  truthful instead of inventing a sharper alternative move.
- Current-move material truth and inherited compensation shell are not the same
  thing. `investedMaterial` or durable-pressure text alone may keep a move
  visible, but they may not by themselves certify the current move as a fresh
  sacrifice commitment.

### 2. Shared strategic carriers

- `StrategyPackBuilder` builds the shared strategic carrier.
- `StrategyPackSurface` is the shared extraction / normalization layer used by
  Chronicle, Bookmaker, and Active note.
- compensation family / theater / mode / normalization are resolved once and
  reused across surfaces instead of being independently reinterpreted.
- decisive-truth derivation suppresses fake compensation on bad moves, but it
  preserves real investment exemplars when compensation-valid payoff is
  verified from accepted compensation evidence and current-move truth.
- decisive-truth derivation keeps commitment, maintenance, and conversion as
  separate truth phases; later maintenance or later cash-out does not inherit
  fresh investment ownership by default.
- decisive-truth derivation reads the raw pre-sanitization `StrategyPack`
  before surface cleanup. Investment exemplars are classified from verified raw
  pack evidence first, and only then are Chronicle / Bookmaker / Active
  surfaces sanitized from the finalized contract.
- verified investment payoff can come either from accepted structured
  compensation decisions or from backward-compatible durable-investment
  carriers already encoded in the raw pack (`investedMaterial` +
  compensation summary + durable deferred-payoff text). Those legacy carriers
  may preserve visibility or exemplar candidacy, but they do not by themselves
  certify that the current move is a fresh commitment owner.

### 3. Bounded probe evidence

- probe paths are evidence support, not a second commentary engine
- probe results can validate or refute a plan branch
- probe surfaces may introduce bounded alternative-path evidence, but they do
  not bypass the canonical strategic contract

### 4. UI owns structure

- frontend cards, sections, badges, and ledgers are UI-owned
- LLM-owned output is prose only
- Bookmaker / Active / Puzzle prompts must not recreate UI chrome

## Current Runtime Path Map

### Chronicle / Game Arc

Current canonical flow:

1. PGN + analysis + helper signals enter `CommentaryEngine`.
2. `CommentaryEngine` runs a baseline truth pass for each candidate key moment.
3. truth-critical candidates then run selective deep verification:
   - played move
   - verified best move
   - top MultiPV alternatives
   - blunder / missed-win candidates
   - sacrifice / investment / only-move / conversion candidates
4. `CommentaryEngine` synthesizes investment chains so later maintenance cannot
   steal ownership from an earlier commitment and later conversion cannot be
   mistaken for compensation.
5. The result is a canonical `MoveTruthFrame`; `DecisiveTruthContract` is
   projected from that frame.
6. Ownership role, visibility role, surface mode, and exemplar role are all
   resolved from the same canonical frame instead of being re-inferred from
   local prose cues.
7. truth ownership stays strict, visibility is allowed to be more
   recall-friendly than prose permission, and surface mode remains strict. A
   move may stay supporting-visible without being allowed to claim investment
   ownership or compensation-positive prose.
8. exemplar preservation is explicit: a `provisional_exemplar` may remain
   visible even when Chronicle / Bookmaker / Active are still forced to stay
   neutral about why the investment works.
9. decisive-moment promotion prefers verified blunders, missed wins, verified
   exemplars, first investment commitments, and forced conversion pivots over
   weaker salience-only moments.
10. `CommentaryEngine` then builds game-level narrative state and moment-level
   strategic carriers from that truth-bound context.
11. Chronicle / Game Arc prose is rendered deterministically from those
   carriers.
12. Chronicle conclusion quality is carried by a deterministic game-level
   binder inside `CommentaryEngine`, not by an LLM-only summary layer.
13. That binder reuses visible moments, strategic threads, and surfaced
   strategy-pack evidence to carry:
   - the main two-sided strategic contest
   - the decisive shift / turning point
   - punishment or conversion payoff when the game actually hinged on one
   - player-language anchors lifted from narrative/support evidence before any
     raw route or square evidence
14. Full-game review uses this deterministic path as the canonical signoff
   surface.

Current implications:

- whole-game story quality lives in the deterministic Chronicle path
- opening / middlegame / endgame transitions are narrated from the shared
  strategic state, not from an LLM-only interpretation layer
- whole-game conclusion density must come from the deterministic binder folded
  into the existing `conclusion` field, not from adding a new response field
- if the only remaining shift/payoff anchor is a raw square list, bare theater
  token, or piece-route stub, Chronicle omits that sentence instead of shipping
  rough prose
- quiet / balanced games stay compact and may end with contest framing only;
  the binder does not fabricate a turning point when evidence is weak
- punishment / turning-point / whole-game-plan behavior is judged against this
  deterministic surface during manual audit

Primary files:

- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineValidator.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`

### Bookmaker

Current canonical flow:

1. `NarrativeContextBuilder` builds the Bookmaker context from analysis,
   references, probe evidence, and strategic carriers.
2. decisive-truth sanitization removes unverified benchmark and compensation
   claims before Bookmaker outline / prose rendering.
3. deterministic draft prose is rendered through the Bookmaker path.
4. `BookmakerPolishSlots` / `BookmakerPolishSlotsBuilder` expose the narrow
   slot contract for optional prose polish.
5. `PolishPrompt` may polish the Bookmaker body only.
6. `BookmakerSoftRepair` and payload normalization preserve the structural
   contract after polish.

Current rules:

- Bookmaker ledger rows are computed, evidence-gated, and UI-owned.
- the commentary body is optional-polish prose only
- LLM polish must stay slot-grounded and must not add new topics
- Bookmaker may not name a concrete benchmark unless the decisive-truth
  contract marks that benchmark as verified.
- Bookmaker may not use compensation-positive framing on a verified bad move.
- Bookmaker may use compensation-positive framing on a verified
  `winning_investment` or `compensated_investment` only when the payoff anchor
  comes from the decisive-truth contract rather than raw route-purpose shells.
- `The move gives up material ...` belongs only to a truth-owning commitment
  row. Maintenance rows describe preservation, and conversion rows describe
  realization.
- provisional exemplars may remain visible in focus selection while Bookmaker
  stays neutral and avoids compensation-positive ownership language.
- Bookmaker remains compatible with rule-only signoff and provider-none audit
  runs

Primary files:

- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala`
- `modules/llm/src/main/LlmApi.scala`

### Active Note

Current canonical flow:

1. `LlmApi` selects the active-note subset from Chronicle moments.
2. shared strategic carriers are converted into:
   - idea refs
   - route refs
   - move refs
   - directional targets
   - `ActiveBranchDossier`
3. decisive-truth sanitization gates benchmark and compensation signals before
   Active-note surface construction.
4. `deterministicActiveStrategicNoteDecision` owns canonical note attachment.
5. `ActiveStrategicCoachingBriefBuilder` builds the active-note brief and
   strict compensation fallback note when needed.
6. `ActiveStrategicNoteValidator` validates the candidate note.
7. only after a deterministic note exists may `ActiveStrategicPrompt` attempt
   wording polish.
8. if polish is empty / invalid / repair-failed, the deterministic draft is
   retained.

Current rules:

- Active note existence does not depend on provider availability.
- `sourceMode = rule / llm_polished / omitted` stays externally stable.
- `llm_polished` means wording polish over a deterministic draft, not fresh
  canonical note generation.
- compensation-positive note survival is closed through the deterministic path,
  including strict compensation rescue.
- Active may not explain a verified blunder as compensation-positive.
- Active may keep compensation-positive language for a verified
  `winning_investment` or `compensated_investment`, but benchmark wording stays
  suppressed unless a separately verified best move is explicitly allowed.
- `The compensation comes from ...` belongs only to a truth-owning commitment
  row. Maintenance rows stay compact and conversion rows describe conversion
  rather than fresh compensation.
- canonical compensation family / theater / mode are preserved before raw route
  wording.
- compensation anchors are rendered through player-facing anchor wording rather
  than placeholder route text such as `can use`.

Primary files:

- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveBranchDossierBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicNoteValidator.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicSentenceRenderer.scala`
- `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala`

### Probe / Alternative Path Evidence

Current canonical flow:

1. plan / tactical / conversion hypotheses may request bounded probes
2. probes validate current-plan evidence or bounded alternative branches
3. probe results flow back into context / evidence surfaces
4. Chronicle / Bookmaker / Active may mention those results when they are
   evidence-backed

Current rules:

- out-of-main-line probe evidence is allowed when bounded and grounded
- probe evidence supports explanation; it does not replace the main strategic
  carrier
- frontend probe panels consume structured payloads rather than mined prose

Primary files:

- `modules/llm/src/main/scala/lila/llm/analysis/ProbeDetector.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- `ui/analyse/src/bookmaker/probeOrchestrator.ts`

## Shared Strategic Logic

### Strategy carrier

- `StrategyPack` is the shared strategic payload
- it carries dominant ideas, routes, move refs, directional targets,
  long-term focus, and signal digest
- Chronicle / Bookmaker / Active all consume it through current shared
  extraction logic

### Compensation normalization

- compensation interpretation is centralized in `StrategyPackSurface` and
  matching helpers
- canonical subtype resolution drives:
  - Game Arc wording
  - Bookmaker support wording
  - Active-note family / idea / anchor selection
- normalized compensation contract is authoritative when normalization is
  active

### Active compensation parity

- parity eligibility and selection eligibility are backend-defined helpers in
  `LlmApi`
- strict compensation rescue exists specifically to keep active-note attachment
  stable when the compensation contract is already canonical but normal prose is
  thin

## Prompt Surface Policy

Prompt-bearing surfaces are intentionally narrow and role-specific.

### `PolishPrompt`

- optional polish over deterministic Chronicle / Bookmaker draft prose
- slot-grounded
- no new topic introduction
- preserves deterministic whole-game anchor nouns instead of rewriting them
  back into generic theme labels, bare square lists, or vague theater-only
  phrasing

### `ActiveStrategicPrompt`

- optional polish over deterministic active-note draft
- does not own note existence
- does not own canonical idea selection
- may improve wording, compression, and flow only

### `StrategicPuzzlePrompt`

- product-specific reveal / summary polish
- separate from commentary signoff canonical logic

The detailed prompt-family contract lives in:

- `modules/llm/docs/PromptSurfacePolicy.md`

## Provider Split and Signoff Path

### Runtime defaults

- `LLM_PROVIDER` default remains `openai`
- `LLM_PROVIDER_ACTIVE_NOTE` default remains `gemini`

This split is a runtime polish-routing choice, not a canonical attach contract.

### Signoff / eval path

- audit-grade evaluation uses `RealPgnNarrativeEvalRunner`
- signoff path is deterministic canonical first
- provider-none eval remains the canonical measurement path for release signoff
- rerun outputs under `tmp/commentary-player-qc/...` are local evidence only
- merged shard signoff must preserve the dedicated positive-exemplar audit set;
  exemplar coverage is not reduced to the intersection of exemplar keys with
  the current main-corpus focus set
- audit calibration keeps verified `winning_investment` /
  `compensated_investment` pivots eligible as positive exemplars when Game Arc
  and Bookmaker agree on the compensation contract; legacy suppression is for
  fake compensation only
- signoff `path/payoff divergence` counts only unresolved divergence after
  display-subtype resolution, not benign path-vs-payoff differences that have
  already been canonically selected

Current release-signoff interpretation:

- automatic gate judges canonical payload behavior, but it is not sufficient for
  chess-truth signoff
- manual audit judges prose / story quality over that canonical output
- GM-truth signoff requires the master-only 140 corpus defined in
  `CommentaryTruthGate.md`
- `cross-surface parity` and `releaseGatePassed` are health signals, not the
  top-level truth gate
- prompt/provider routing must not be allowed to create or remove active-note
  attachment on the signoff path

Primary files:

- `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalRunner.scala`
- `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalReportMerge.scala`

## Frontend Consumption

Current frontend rule:

- frontend consumes typed payloads
- frontend renders structure
- frontend does not reconstruct hidden strategy from free-form prose

Primary files:

- `ui/analyse/src/bookmaker.ts`
- `ui/analyse/src/bookmaker/responsePayload.ts`
- `ui/analyse/src/narrative/narrativeCtrl.ts`

## Source-of-Truth Document Set

Current SSOT / contract docs in `modules/llm/docs`:

- `CommentaryPipelineSSOT.md`
  - full commentary-analysis runtime and signoff contract
- `CommentaryTruthGate.md`
  - decisive-truth signoff contract and GM-audit grading
- `PromptSurfacePolicy.md`
  - prompt-family role split and polish/canonical ownership
- `BookmakerProseContract.md`
  - Bookmaker body prose contract

Older narrative update-log style sections are intentionally removed from this
document. The current-state contract above is the only maintained authority.

## Reference Files

Primary current-code references:

- `app/controllers/LlmController.scala`
- `modules/llm/src/main/LlmApi.scala`
- `modules/llm/src/main/scala/lila/llm/ActiveStrategicPrompt.scala`
- `modules/llm/src/main/scala/lila/llm/GeminiClient.scala`
- `modules/llm/src/main/scala/lila/llm/OpenAiClient.scala`
- `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala`
- `modules/llm/src/main/scala/lila/llm/StrategicPuzzlePrompt.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveBranchDossierBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicCoachingBriefBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ActiveStrategicNoteValidator.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineValidator.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ProbeDetector.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicSentenceRenderer.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/StrategyPackSurface.scala`
- `modules/llm/src/main/scala/lila/llm/GameChronicleResponse.scala`
- `modules/llm/src/main/scala/lila/llm/models.scala`
- `ui/analyse/src/bookmaker.ts`
- `ui/analyse/src/bookmaker/responsePayload.ts`
- `ui/analyse/src/narrative/narrativeCtrl.ts`

## Maintenance Rule

Update this file in the same change if any of the following changes:

- Chronicle / Game Arc generation logic
- Bookmaker deterministic draft logic
- Active-note selection / attach / omit / polish ownership
- shared strategic carrier extraction or compensation normalization
- prompt-family ownership or provider routing semantics
- API serialization relevant to commentary payloads
- frontend commentary consumption or structural rendering

Do not append dated change logs here anymore. Rewrite the current-state sections
above so the document stays a live description of the latest pipeline.
