# Commentary Pipeline SSOT

This file is the canonical audit for the current Chesstory commentary-analysis
pipeline.

For onboarding, Step 1-7 status, CQF track status, and document navigation, see
[`CommentaryProgramMap.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md)
first.

It intentionally describes the latest live pipeline and deletes the older
dated update-log style material. Historical side paths are not canonical.

Current program mode is maintenance-only. This file is the runtime source of
truth, not the active-work tracker; use
[`CommentaryProgramMap.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md)
and
[`CommentaryTrustHardening.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)
for frontier status and re-review triggers.

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
- signoff / audit entry points used by the real-PGN signoff runner

Out of scope:

- removed legacy QC / BookCommentary / debug / shadow / structure-endgame side
  workflows
- temporary rerun outputs under the local quality-audit temp root
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
- no-contract `fallbackMomentProjection` is now failure-only:
  raw `blunder` / `missed_win` classification may preserve a compact failure
  fallback, but serialized `truthPhase`, `surfacedMoveOwnsTruth`,
  `investmentTruthChainKey`, `verifiedPayoffAnchor`, and conversion-like
  `transitionType` strings may not recreate investment / maintenance /
  conversion ownership or visibility.
- when a `DecisiveTruthContract` exists, runtime semantic consumers must stay
  truth-first:
  - raw `momentType`, `moveClassification`, `criticality`, and `choiceType`
    remain compatibility / display / fallback fields
  - raw `StrategyPackSurface` compensation flags remain extraction helpers and
    no-contract fallbacks
  - helpers such as whole-game binders, dossier builders, selector scoring, and
    tactical-pressure checks must prefer canonical truth semantics over those
    raw fields
- visible exemplar preservation is separate from ownership and from
  compensation-positive prose permission:
  - `verified_exemplar` may own and explain the decisive investment
  - `provisional_exemplar` may stay visible while surface mode remains
    `neutral`
  - a truth-bound `compensation_maintenance` row stays `non_exemplar` in the
    public/runtime contract; a private maintenance-exemplar candidate may keep
    that row eligible for focus selection / audit without upgrading ownership
  - public/runtime `maintenance_echo + supporting_visible +
    maintenance_preserve` is reserved for `critical maintenance`: verified
    payoff anchor, verified investment payoff, direct current semantic/carrier
    anchor match, and a pressure signal (`only_move`, `unique_good_move`,
    forcing proof, bad followthrough, or accepted current-semantic match)
  - routine follow-up may keep the internal `compensation_maintenance` truth
    phase while the public/runtime projection is forced back to
    `none / hidden / neutral`
  - `non_exemplar` rows may not inherit investment ownership from stale shells
- fresh commitment recognition now starts before any explicit
  `investedMaterial` requirement:
  - current-move material loss (`sacrificeKind`, value-down capture, or
    rising deficit) may seed `first_investment_commitment`
  - that seed still needs a move-local payoff anchor
  - when ownership proof is still too weak, the move may project only as a
    `provisional_exemplar`
- payoff-anchor extraction for fresh commitment seeding is widened, but still
  move-local and truth-first:
  - accepted current semantic summary / vectors
  - current dominant / secondary idea text
  - current objective / focus text
  - current directional-target reasons
  - current route / move-ref purpose text
  - current long-term focus text
  - stale compensation shell text is not allowed to originate a fresh seed
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
- raw compensation flags on `StrategyPackSurface`
  (`compensationPosition`, `strictCompensationPosition`,
  `durableCompensationPosition`, `quietCompensationPosition`) are carrier
  extraction only. Once a decisive-truth contract exists, they may not recreate
  compensation significance on their own.
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
- decisive-truth derivation tracks four evidence provenance buckets:
  - `current_material`
  - `current_semantic`
  - `after_semantic`
  - `legacy_shell`
- provenance policy is strict:
  - `current_material` may seed a fresh commitment
  - `current_semantic` may verify that seed and upgrade ownership
  - `current_semantic` also includes concrete current-move payoff-route
    carriers from the raw pack when those carriers directly match the verified
    payoff anchor and keep a maintenance move truth-bound without recreating
    ownership
  - `after_semantic` may support maintenance / conversion continuity
  - `legacy_shell` may preserve visibility only
  - `after_semantic` and `legacy_shell` may not originate fresh commitment
    ownership
- verified investment payoff can come either from accepted structured
  compensation decisions or from backward-compatible durable-investment
  carriers already encoded in the raw pack (`investedMaterial` +
  compensation summary + durable deferred-payoff text). Those legacy carriers
  may preserve truth-phase continuity or private maintenance-candidate
  eligibility when direct current-move evidence also survives, but they do not
  by themselves certify that the current move is a fresh commitment owner or a
  public maintenance echo.
- bad-move interpretation is also gated canonically:
  - `failure_intent` is a private/internal fact, not a public payload field
  - `tactical_refutation`, `only_move_failure`,
    `quiet_positional_collapse`, `speculative_investment_failed`, and
    `no_clear_plan` are derived from benchmark pressure, proof line, and
    move-local plan carriers
  - route / purpose / target evidence only counts when it directly matches the
    verified payoff anchor; bare square-access scaffolding is not an intent
    anchor
  - accepted current semantic support may keep intent alive only when it also
    matches the verified payoff anchor
  - `speculative_investment_failed` requires fresh current-move matched
    investment evidence; inherited shells alone may not trigger it
  - low-confidence or `no_clear_plan` failures must not keep strategic route /
    target / plan carriers alive in sanitized commentary surfaces
  - commentary may explain collapse or refutation without inventing a player
    intention that the truth layer could not support
- canonical key-moment anchoring is also truth-first:
  - WPA remains the first swing gate for decisive anchors
  - the runtime rescues catastrophic CP / mate swings when WPA saturation would
    otherwise miss an engine-severe move
  - when a severe move survives only as a candidate bridge, canonical internal
    admission rescues that bridge from runtime truth (`blunder` /
    `missed_win`, mate shock, or severe `only_move_failure`) instead of leaving
    it witness-only
  - if a soft label and a decisive label land on the same ply, the decisive
    `blunder` / `missed_win` anchor wins

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
11. whole-game carry is question/evidence-aligned before Chronicle / Active
    replay:
    `FullGameEvidenceSurfacePolicy.runtimePayload` keeps at most two carried
    author questions, but those questions are chosen to stay aligned with the
    carried author-evidence rows rather than blindly preserving the first two
    question summaries.
12. `QuestionPlannerInputsBuilder` is built once per surfaced Chronicle moment
    from live move-owned carriers plus cited-line candidates from the selected
    outline beats.
13. `QuestionFirstCommentaryPlanner` ranks `primary + optional secondary`
    question plans from that shared moment bundle.
    - shared-planner scene-first admission now runs ahead of ranking:
      ranking:
      planner output now records `scene_type`, raw `owner_candidates`,
      `admitted_families`, `dropped_families`, `demotion_reasons`,
      `selected_question`, `selected_owner_family`, and
      `selected_owner_source`
    - shadow normalization remains visible, per candidate:
      `owner_family`, `source_kind`, `move_linked`, `support_material`,
      `materiality`, `timing_source`, raw support-material separation, and
      proposed family mapping
    - every normalized candidate now also carries `admission_decision`,
      `admission_reason`, and optional `demoted_to`; ranking sees only
      `PrimaryAllowed`
    - raw close alternatives from `AlternativeNarrativeSupport` are traced as
      `DecisionTiming` support material only and remain illegal as direct
      owners
    - `DecisionTiming` trace stays split by materiality and source, and
      `decision_comparison` timing is now further refined into
      `concrete_reply_or_reason` vs `bare_engine_gap`, so later recovery can
      reopen only the concrete subset while keeping raw close-candidate
      support, prevented-resource timing, and only-move timing distinct
    - opening precedent summaries and endgame theoretical/oracle hints are
      traced as raw domain support material, while
      `OpeningPrecedentBranching.relationSentence` and move-attributed endgame
      transition continuity are traced separately as move-linked candidates
    - scene precedence is now trace-first
      `tactical_failure > plan_clash > forcing_defense > transition_conversion >
      opening_relation > endgame_transition > quiet_improvement`
      so `plan_clash` is no longer shadow-hidden by a concurrent forcing-defense
      signal
    - when move-linked `OpeningRelation` and move-linked `EndgameTransition`
      translators coexist with a move-local transition anchor, shadow
      classification absorbs them into `transition_conversion` rather than
      forcing an arbitrary opening-vs-endgame prior
    - v1 admission is intentionally conservative:
      `support_material` never enters the owner pool;
      `DecisionTiming(close_candidate)` is always `SupportOnly`;
      `PlanRace` is `PrimaryAllowed` only in `plan_clash`;
      `MoveDelta` is `PrimaryAllowed` only in
      `quiet_improvement | transition_conversion`;
      `TacticalFailure` and `ForcingDefense` only own their matching scenes
    - move-linked `OpeningRelation` / `EndgameTransition` translators are
      `PrimaryAllowed` only in their own scenes, and only through the shared
      planner's scene-first `WhyThis` / `WhatChanged` builders; raw opening
      precedent summaries and raw endgame theoretical/oracle hints remain
      support-only and never become direct owners
    - the shared planner now hydrates replay-safe
      `openingRelationClaim` / `endgameTransitionClaim` fields through
      `QuestionPlannerInputs` and `NarrativeSignalDigest`, so Chronicle /
      Active replay can reuse the same legal planner-owned domain claim
      instead of minting a new owner locally
    - `WhyNow` / `WhatChanged` plans that rely only on `DecisionTiming` are
      filtered out of the legal pool in v1; even the
      `concrete_reply_or_reason` decision-comparison subtype remains trace-only
      at this stage, so fail-closed fallback or another already-legal owner
      must carry the scene instead
    - demotions from `WhyNow` / `WhatMustBeStopped` /
      `WhosePlanIsFaster` now resolve in-band:
      a synthesized legal `WhyThis` fallback stays planner-owned, while a
      missing fallback is recorded as an explicit suppressed intentional drop
      rather than a ghost demotion that later hard-drops without trace
    - Chronicle / Active replay may still consume the shared trace, but they
      may not override planner legality to resurrect timing-only or raw-domain
      owners
    - `WhatChanged` may now stay planner-owned from move-attributed
      `preventedPlans` / counterplay-window change or concrete
      decision-comparison balance shift, but not from state-only structure
      summary
14. `GameChronicleCompressionPolicy` is planner-first:
    - Chronicle may swap only within planner top-2
    - Chronicle surface preference is
      `WhyNow > WhatChanged > WhatMustBeStopped > WhyThis > WhosePlanIsFaster`
    - a swap is allowed only when claim ownership, evidence quality,
      fallback strength, and plan strength do not get weaker
    - replay-side claim ownership scoring now keeps a legal opening/endgame
      domain primary ahead of a more generic swap candidate; Chronicle may
      reorder only inside that preserved legal planner pool
15. Chronicle sentence composition is planner-owned:
    - sentence 1 comes from `primary.claim`
    - for `WhyThis` / `WhyNow`, sentence 2 may replace raw `primary.contrast`
      with `ContrastiveSupportAdmissibility` support when that support is
      already certified by shared-planner/truth inputs; the primary question,
      owner, and ranking stay unchanged
    - sentence 3 may use planner evidence or one beat-derived cited line only
      when the line is anchored/cited and still matches the selected plan
    - `secondary` may contribute only one non-duplicate support sentence
16. if no admissible Chronicle surface survives, runtime falls straight to
    compact factual fallback or omission; it does not revive raw `mainBundle`,
    `quietIntent`, or generic strategic shell text as a prose owner path.
    exact-factual Chronicle fallback also stays claim-only even if the
    quiet-support composer finds a bounded candidate; that diagnostic trace may
    still be recorded, but it may not append a support sentence on the fallback
    surface.
17. Chronicle conclusion quality is carried by a deterministic game-level
    binder inside `CommentaryEngine`, not by an LLM-only summary layer.
18. local Chronicle runtime also exposes an internal artifact split for
    validation:
    `analyzeGameChronicleLocalArtifacts` returns both the unsanitized internal
    response and the user-facing sanitized response, and real-PGN validation
    tooling must use the internal response when replaying planner ownership.
19. That binder reuses visible moments, strategic threads, and surfaced
    strategy-pack evidence to carry:
   - the main two-sided strategic contest
   - the decisive shift / turning point
   - punishment or conversion payoff when the game actually hinged on one
   - player-language anchors lifted from narrative/support evidence before any
      raw route or square evidence
14. whole-game priority, decisive-shift binding, and punish / conversion
    realization are truth-first:
    - canonical helpers consume `truthClass`, `reasonFamily`, `ownershipRole`,
      `surfaceMode`, `failureMode`, and exemplar visibility first
    - raw `momentType` / `moveClassification` remain display and fallback aids
      for opening / mate / synthetic-bridge cases only
    - raw investment / maintenance / conversion-like serialized fields may not
      reopen whole-game owner projection when the canonical contract is absent
15. Full-game review uses this deterministic path as the canonical signoff
    surface.

Current implications:

- whole-game story quality lives in the deterministic Chronicle path
- opening / middlegame / endgame transitions are narrated from the shared
  strategic state, not from an LLM-only interpretation layer
- whole-game conclusion density must come from the deterministic binder folded
  into the existing `conclusion` field, not from adding a new response field
- surfaced Chronicle moments are planner-first:
  `primary` owns the opening sentence, `secondary` is support-only, and
  `GameChronicleCompressionPolicy` does not restore raw `mainBundle` /
  `quietIntent` prose when planner admission fails
- Chronicle top-2 swap is surface-local only:
  it may reorder planner `primary/secondary` only when truth ownership,
  evidence quality, and fallback strength are not weakened
- Chronicle evidence is still citation-bound:
  branch-scoped lines survive only as inline cited proof, and uncited branch
  leakage is omitted instead of being paraphrased
- whole-game support reuse is projection-first:
  raw `signalDigest` / `StrategyPack` carriers are passed through a local
  whole-game support projection before anchor normalization, so helper labels
  such as `continuity:` or `alignment intent:` are not reused verbatim as
  released turning-point / payoff anchors
- whole-game support projection is now authority-aware:
  verified payoff anchors remain separate from projected `support_only`
  carriers, so binders can prefer contract-owned anchors without reopening raw
  helper labels as truth owners
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
3. `QuestionPlannerInputsBuilder` is built once for the Bookmaker owner path
   from live carriers only:
   `MainPathMoveDeltaClaimBuilder`, `QuietMoveIntentBuilder`,
   `CertifiedDecisionFrameBuilder`, sanitized `DecisionComparison`,
   `AlternativeNarrativeSupport`, current-board `preventedPlans`, `PVDelta`,
   threat tables, opponent plan, and cleaned candidate evidence lines.
4. `QuestionFirstCommentaryPlanner` ranks `primary + optional secondary`
   question plans from that shared bundle.
   - the shared planner also emits owner-admission diagnostics for Bookmaker
     signoff:
     `scene_type`, `owner_candidates`, `admitted_families`,
     `dropped_families`, `demotion_reasons`, `selected_question`,
     `selected_owner_family`, `selected_owner_source`
   - shadow normalization distinguishes owner candidates from support material:
     raw close alternatives, opening precedent summaries, and endgame
     theoretical hints are traced but stay support-only in this pass
5. `BookmakerLiveCompressionPolicy` is planner-first:
   - `primary` owns `claim`
   - `primary` evidence owns `evidenceHook` only when it stays cited /
     anchored
   - `secondary` may fill support-only slots and may not replace the main
     claim or evidence owner
   - `mainBundle` / `quietIntent` are input-only and no longer directly own
     Bookmaker prose
  - the Bookmaker-side contrast-support render guard is renderer-scoped and
     trace-preserving:
     when raw planner `primary` is `WhatMustBeStopped`, shared-planner
     `secondary` is `WhyNow`, both stay in `ForcingDefense`, and the raw primary
     contrast trace rejects with `question_outside_scope`, slot rendering may
     use the `WhyNow` secondary for visible prose/contrast while leaving the raw
     planner trace unchanged; this is not a legality, ranking, or owner-path
     change
6. deterministic draft prose is rendered from those planner-owned slots.
   - `NarrativeOutlineBuilder` still emits plain `OutlineBeat.text`, but the
     runtime now keeps several mixed families split locally until the final
     stitch:
     opening precedent comparison body vs ordinary summary vs shared-lesson
     phrasing, main-move annotation terminal/tag/difficulty hints vs
     severity-tail projection, and wrap-up planner/practical/compensation
     fragments
  - the current Step 4 / 5 authority slice stays file-local inside
    `NarrativeOutlineBuilder`, `CommentaryEngine`, and
    `NarrativeOutlineValidator`:
    opening-precedent body/ordinary summary are released as
    `support_only`, `sharedLesson` is retained as `unsafe_as_lesson`, and
    annotation `severityTail` is retained as `unsafe_as_truth`; current
    final stitch omits the unsafe lesson/truth fragments rather than letting
    them strengthen released prose
  - Step 5 now adds fragment-local generalization admissibility before final
    release:
    `unsafe_as_lesson`, `unsafe_as_truth`, and
    `candidate_for_future_lesson` remain blocked;
    `requires_move_linked_anchor` may release only when a move-linked,
    grounded admission exists; generalized `support_only` fragments may
    release only when they are move-linked, scene-grounded, evidence-backed,
    planner-owned, or contract-consistent
  - Context now keeps opening / motif / flow / plan / fact / pawn / opponent /
    meta families as authority-bearing fragments until final beat stitch;
    `getOpening` is contained as `unsafe_as_truth`, while surviving released
    Context carriers are `support_only`
  - WrapUp now keeps planner consequence / practical verdict / compensation as
    separate fragments until the final join, with released wrap-up prose kept
    in `support_only`
  - whole-game support reuse is admissibility-gated too:
    `support_only` projected carriers remain reusable support but may not seed
    decisive-shift / payoff wrappers; those wrappers now require a verified
    payoff anchor or equivalent structured, contract-consistent proof
  - `NarrativeOutlineValidator` now applies one common authority leak cleanup
    plus narrow admissibility backup before duplicate/evidence gates:
    it removes leaked `Shared lesson:` sentences, strips raw helper-label
    prefixes, and drops ungrounded generalized fallback families outside
    anchored / opening-theory scopes if they survive builder release
7. `BookmakerPolishSlots` / `BookmakerPolishSlotsBuilder` expose the narrow
   slot contract for optional prose polish.
8. `PolishPrompt` may polish the Bookmaker body only.
9. `BookmakerSoftRepair` and payload normalization preserve the structural
   contract after polish.
10. user-facing Bookmaker prose must pass one shared hard gate before it may
   enter the payload: no helper/debug notation leak, placeholder/meta leak,
   broken fragment, unparsed JSON wrapper, or duplicated sentence survives.
11. user-facing prose is assembled through one canonical dedupe layer before
   final emission:
   - `BookStyleRenderer`, whole-game conclusion support, and payload assembly
     reduce exact duplicates, near-duplicates, and wrapper restatements at the
     claim-family / fingerprint level
   - visible surfaces keep one canonical representative per semantic claim
     family rather than concatenating every rendered restatement

Current rules:

- Bookmaker ledger rows are computed, evidence-gated, and UI-owned.
- the commentary body is optional-polish prose only
- LLM polish must stay slot-grounded and must not add new topics
- Bookmaker slot ownership is planner-first:
  - `primary.claim` is the only owner of the main Bookmaker claim
  - `secondary` may fill only `supportPrimary` / `supportSecondary`
  - `secondary` may not replace `claim` or `evidenceHook`
  - `WhosePlanIsFaster` race framing may surface only after certified planner
    admission; generic race shells do not survive slot cleaning
- Bookmaker fails closed directly from the planner path:
  - when no admissible `primary` survives, runtime goes straight to the exact
    factual one-liner
  - when planner-owned slots fail slot sanitization or the structural prose
    contract, runtime also goes straight to the exact factual one-liner
  - old `mainBundle` / `quietIntent` direct compression is not a prose salvage
    owner path anymore
- release-safe payload prose is stricter than internal support text:
  - final payload prose is either a hard-gate-passing polished candidate, a
    hard-gate-passing deterministic fallback, or omitted
  - `UserFacingPayloadSanitizer` remains a last scrubber, not a semantic
    translator for raw helper/debug strings
  - active strategic note prose is only emitted when the validated note
    survives both the shared hard gate and canonical dedupe as content distinct
    from the main prose
  - active dossier / route / move / target carriers do not inherit authority
    from raw state-summary text or note survival alone; they attach only when
    the selected planner result and certified decision frame admit them
  - payload sanitization covers active route refs as well as active idea /
    move / target carriers; no active-side route purpose may bypass the final
    scrubber
- Bookmaker runtime semantics are truth-first:
  - compensation significance comes from `ownershipRole`, `visibilityRole`,
    `surfaceMode`, `exemplarRole`, and `maintenanceExemplarCandidate` when a
    contract exists
  - raw compensation surface flags may only assist no-contract fallback flows
  - one shared player-facing truth mode policy now owns visible packaging
    across Chronicle / Bookmaker / Active:
    - `Minimal` is the default for quiet / weak-evidence moments; it keeps the
      surface terse, suppresses speculative route / target / long-plan prose,
      and prefers move-local factual copy or existing cited-line evidence over
      invented strategic story
    - `Tactical` is allowed only for contract-owned blunders, missed wins,
      proof-backed `only_move_defense`, explicit tactical refutations, or a
      tactical sacrifice whose material imbalance settles inside the current
      search horizon; raw `momentType`, `criticality`, `choiceType`, or cp
      swing size alone may not promote a move into tactical narration
    - `only_move_defense` and `tactical_refutation` do not own player-facing
      tactical narration on contract shape alone; they need a shared forcing
      proof inside the current horizon before Chronicle / Bookmaker may lead
      with tactical text
    - when a move is `Tactical`, the immediate tactical truth must lead before
      any route / target narration, and cited variations may survive only when
      they prove that tactical point
    - `Strategic` is allowed only when current semantics or verified benchmark
      evidence can back a concrete route / target / plan claim; raw theme
      labels, legacy shells, weak support rows, and MultiPV alternatives alone
      may not promote a strategic story
    - strategic move commentary is move-delta-owned, not state-feature-owned:
      the surface must be able to point to what the move changed, using one of
      the canonical delta families (`new_access`, `pressure_increase`,
      `exchange_forcing`, `counterplay_reduction`, `resource_removal`,
      `plan_advance`)
    - route / target / concrete-square narration needs move-linked evidence:
      the claim must stay anchored to a surfaced route / directional target /
      move ref or a verified payoff anchor that still matches the current move;
      “concrete” wording alone is not enough
    - route / pressure / exchange / prophylaxis prose is admitted only when
      that move-linked anchor also carries a delta-backed reason:
      “route exists”, “pressure exists”, “exchange is thematic”, or “the
      structure points to it” are not sufficient on their own
    - sacrifice naming is also mode-owned:
      tactical sacrifices require immediate recoup / forcing recovery /
      concrete payoff inside the current horizon; strategic sacrifices require
      a verified compensation anchor and otherwise collapse back to `Minimal`
  - tactical tension, cited-line pressure, and early-opening escape hatches
    consume a shared canonical tension policy rather than re-reading raw
    `criticality` / `choiceType` strings in each surface
- Bookmaker may not name a concrete benchmark unless the decisive-truth
  contract marks that benchmark as verified.
- Bookmaker may not use compensation-positive framing on a verified bad move.
- Bookmaker may use compensation-positive framing on a verified
  `winning_investment` or `compensated_investment` only when the payoff anchor
  comes from the decisive-truth contract rather than raw route-purpose shells.
- `The move gives up material ...` belongs only to a truth-owning commitment
  row. Maintenance rows describe preservation, and conversion rows describe
  realization.
- private maintenance-exemplar candidates may remain focus-visible for audit /
  signoff while Bookmaker stays neutral and avoids compensation-positive
  ownership language.
- Bookmaker / Chronicle may omit weak strategic packaging entirely:
  - user-facing main-path legacy thesis / fallback helpers are no longer
    canonical runtime behavior; `StrategicThesisBuilder`-style state-summary
    promotion does not own Bookmaker / Chronicle claims anymore
  - repo-wide runtime no longer emits or consumes `dominant thesis:` /
    `dominant_thesis:` carriers:
    outline, strategy-pack enrichment, full-game evidence gating, hybrid
    bridge helpers, and bookmaker strategic ledger selection now read direct
    semantic / digest / move-delta signals instead of thesis revival
  - bookmaker and chronicle main-path claim permission now flows through the
    shared `MainPathMoveDeltaClaimBuilder` carrier:
    tactical ownership is checked first, then strategic move-delta ownership,
    then move-linked evidence; sanitized prose is the last filter rather than
    the first source of truth
  - main-path claims are now scope-owned as well:
    `MoveLocal` claims may become the main sentence, while `LineScoped` claims
    may survive only as explicit subordinate lines (`Line: ...`, `After ...`,
    `If ...`); tactical proof lines may stand alone, but strategic
    line-scoped consequences may not survive without a move-local strategic
    claim
  - cross-surface trust regression watch now exists in test tooling:
    `CrossSurfaceTrustRegressionHarnessTest` replays the same fixture bundles
    through Bookmaker / Chronicle / Active entrypoints and compares owner,
    strength, scope, support-only carry, and fallback rewrite behavior against
    the documented residual envelope rather than adding a new runtime path
  - that test-side watch is now widened by a large negative fixture pack:
    the same harness also freezes forbidden fallback rewrite / unsupported
    generalization / whole-game support-promotion / Active residual misuse
    lanes, again without introducing a parallel runtime comparison path
  - `exchange_forcing` and `resource_removal` remain canonical delta classes,
    but only with current-move causal proof:
    a thematic exchange or generic defensive easing is not enough; runtime
    needs either an explicit prevented resource, or a proving line that is
    backed by move-linked exchange evidence that this move itself made the
    exchange/resource change more forcing or no longer available; a direct
    capture on the anchor square does not count as `exchange_forcing` by
    itself
  - probe validation is now position-bound, not merely id-bound:
    `ProbeContractValidator.validateAgainstRequest` binds request/result by
    `fen`, candidate move membership, `purpose` / `objective`, `depthFloor`,
    `variationHash`, and `engineConfigFingerprint`; contract outcomes are
    layered as `valid`, `weakly_valid`, `invalid`, or
    `stale_or_mismatched`, and only `valid` probes may support runtime
    promotion
  - theme-plan validation is no longer one generic purpose only:
    restriction-family probes may bind through family-specific purposes such as
    `route_denial_validation`, `color_complex_squeeze_validation`, and
    `long_term_restraint_validation`; those purposes carry their own objective /
    required-signal / horizon contract instead of borrowing a generic
    `theme_plan_validation` shell
  - strategic plan support is now claim-certified as well as probe-backed:
    `PlanEvidenceEvaluator` emits diagnostic-sidecar certification fields for
    `certificateStatus`, `quantifier`, `modalityTier`, `attributionGrade`,
    `stabilityGrade`, `provenanceClass`, `taintFlags`, and `ontologyFamily`;
    `alternative_dominance` may refute a plan even when no immediate tactical
    collapse appears, so a weaker sibling plan does not get surfaced as a
    misleading strategic owner
  - the diagnostic sidecar now also carries shadow-equivalent certification
    summary counts:
    `blockedStrongClaims`, `downgradedWeakClaims`, `attributionFailures`,
    `quantifierFailures`, and `stabilityFailures` are computed from the same
    hard-gate certification state that drives user-facing admission
  - quiet move recovery is now explicit and narrow:
    if tactical ownership and admitted strategic delta both fail, runtime may
    recover one move-local quiet-intent claim from the canonical
    `QuietMoveIntentBuilder` taxonomy only:
    `piece_improvement`, `king_safety`, `counterplay_restraint`, or
    `technical_conversion_step`
  - quiet-intent recovery is move-local only:
    it may use exact SAN semantics, explicit `preventedPlans`, and
    move-linked engine evidence, but it may not reuse latent break hints,
    `breakReady`, free-text delta keywords, theme labels, shell names,
    state-only structure summaries, or generic route / target possibility on
    their own
  - quiet intent is now certificate-gated too:
    even exact quiet taxonomy prose must inherit probe-backed provenance plus
    acceptable quantifier / attribution / stability grades; otherwise runtime
    falls straight to the exact factual sentence (`This puts the rook on c3.`,
    `This castles.`, etc.) rather than reviving a softer strategic intention
  - user-facing strategic plans are probe-backed only:
    runtime `mainStrategicPlans` may surface only hypotheses with validated
    supportive probe results plus move-local delta ownership; `pv_coupled`,
    `deferred`, and structural-only escalations are removed from the runtime
    hot path
  - latent / pv-coupled / deferred strategic hypotheses are no longer runtime
    carriers:
    `latentPlans` and `whyAbsentFromTopMultiPV` stay empty in user-facing
    runtime responses, while structured diagnostic entries are emitted only to
    local raw/debug sidecars
  - runtime upstream no longer keeps dead latent/hold-reason branches alive:
    outline strategic-key derivation, strategic-stack context sentences,
    commentary-engine moment assembly, and debug narrative dumps do not rebuild
    prose or semantic keys from `latentPlans` / `whyAbsentFromTopMultiPV`
  - Chronicle-side strategic metadata is also stripped from user-facing
    runtime payloads when it is not probe-backed:
    moment `concepts`, response `themes`, thread summaries/counterplans, and
    signal-digest strategic idea / opponent-plan hints do not survive the
    runtime sanitizer unless a probe-backed strategic plan owns them
  - direct Bookmaker requests do not surface blank prose:
    after tactical / strategic / quiet-intent checks fail, runtime may still
    emit one exact factual fallback sentence from current move semantics
    - that exact-factual floor is literal move-shape only:
      planner/direct fallback may describe castles, destination-anchored piece
      moves, and target-anchored captures, but when capture targeting is
      unavailable it must fail closed to `This captures.` rather than adding
      generalized simplification / exchange meaning from fallback state
    - the quiet-support baseline cohort adds one narrow fallback-lift above
      that floor:
      when planner-owned slots do not survive, runtime may attach at most one
      bounded quiet-support sentence under the exact factual claim, but only on
      the move-linked `MoveDelta.pv_delta` subset backed by `signalDigest`
      route / structure / pressure material
    - that fallback-lift is support-only:
      it does not reopen owner-family admission, question ranking, legality, or
      raw domain-hint revival, and planner trace still records the row as an
      exact-factual fallback rather than a planner-owned promotion
    - QC reruns now also record a quiet-support diagnostic trace for the
      exact-factual path, including runtime gate status
      (`sceneType`, selected owner family/source, `pvDelta` availability,
      move-linked `pv_delta` anchor availability, and gate reject reasons),
      so quiet-support acceptance can separate baseline-selected rows into
      runtime-gate-pass, runtime-gate-fail, and planner drift outside
      quiet-support attach
    - planner owner-preservation now distinguishes blocked `NoClearPlan`
      forcing rows from quiet only-move benchmark rows when `pvDelta` is
      present but empty:
      `QuestionFirstCommentaryPlanner` keeps `MoveDelta` quiet-primary
      behavior for the quiet-support-selected `OnlyMoveFailure` /
      benchmark-critical path, but it preserves planner-owned
      `ForcingDefense` when a blocked `InvestmentSacrifice` / `Conversion`
      row still has an urgent opponent threat and would otherwise drop from
      `planner_owned` to exact factual
    - best-hold `TacticalRefutation` rows with empty `pvDelta` content now
      keep truth-contract `ForcingDefense` through the same planner-only
      owner-preservation gate; this does not reopen legality, ranking, or
      quiet-support attach rules
    - the upstream-isolation artifact now records the before/after ingress
      fields needed to localize those failures:
      raw/sanitized `decision` and `pvDelta` presence, truth-contract
      `truthClass` / `reasonFamily` / `failureMode`, and raw planner candidate
      source splits for `TacticalFailure`, `ForcingDefense`, and `MoveDelta`
    - planner owner trace now also carries `sceneReasons`, and the isolation
      evaluator emits an `upstreamPrimarySubsystem` / `upstreamPrimaryCause`
      attribution so rows can be pinned to `NarrativeContextBuilder`,
      `DecisiveTruth`, `QuestionFirstCommentaryPlanner`, or selector mismatch
      without changing player-facing runtime wording
    - the first upstream-fix pass now narrows those root causes in runtime:
      `NarrativeContextBuilder` no longer hard-drops `decision` / `pvDelta`
      for every `StyleChoice`, and instead keeps them when the best move has
      probe-backed move-linked evidence; `DecisiveTruth.isCriticalBestMove`
      no longer treats best `TacticalRefutation` holds as critical only-move
      pressure; `QuestionFirstCommentaryPlanner` no longer opens a
      `TacticalFailure` owner just because a best hold is tagged
      `TacticalRefutation`; and `WhatChanged` no longer owner-promotes
      decision-comparison prose when `pvDelta` exists but is structurally empty
    - the Quiet Support selector contract is now intentionally narrower than
      the original baseline: baseline-cohort selector rows still require
      `MoveDelta.pv_delta` plus whitelisted digest support, but they also
      require the before-artifact planner scene to be quiet-compatible
      (`quiet_improvement` / `transition_conversion`, or absent), so preexisting
      `tactical_failure` rows stay in `selector_mismatch` rather than the
      baseline-selected lane
    - latest real16 upstream-fix rerun:
      the local quiet-support prototype summary under the quality-audit report
      root
      shows the selected quiet-support subset repaired (`runtimeGatePassCount=4`,
      quiet-support lift applied in the artifact field `quietSupportLiftAppliedCount=4`,
      baseline selected owner/question divergence `=0`, stronger-verb leakage
      `=0`), and the blocked/non-eligible lane is reduced to one residual
      fallback spike (`blockedFallbackSpikeCount=1`) after planner-owned
      `ForcingDefense` is preserved on the representative `Ke7`, `Nxe3`, and
      `Rxc4` blocked rows
    - the remaining blocked spike is the style-choice `Ng4` row where
      `NarrativeContextBuilder` does not surface move-linked `pv_delta`
      ingress (`style_choice_pv_delta_unavailable`); that residual sits
      outside the Track 3 Phase A quiet-support owner-preservation gate
  - surfaced Chronicle moments also do not surface blank prose:
    if a selected visible move renders empty after tactical / strategic /
    quiet-intent gating, `CommentaryEngine` runs a post-selection
    commentability pass that reselects the next commentable move in the same
    thread or drops that surfaced move when no same-thread replacement exists
    - Chronicle quiet-support replay now mirrors the accepted Bookmaker
      fallback-lift only on the same narrow quiet subset
      (`long_structural_squeeze`, `slow_route_improvement`,
      `pressure_maintenance_without_immediate_tactic`) and only when the
      replayed planner surface is already legal `MoveDelta/pv_delta`
    - the Chronicle attach point is renderer-only and claim-only:
      `GameChronicleCompressionPolicy` may add at most one bounded quiet
      support sentence only when the selected Chronicle replay surface has a
      cleaned primary claim and no surviving contrast, evidence, consequence,
      or secondary-support sentence
    - this Chronicle mirror is replay-safe:
      it does not reopen owner-family admission, question ranking, legality,
      top-2 swap policy, or blocked-lane interpretation, and blocked forcing
      rows remain closed unless the planner already selected them as legal
      non-quiet owners
    - Chronicle replay QC now records the same bounded quiet-support gate and
      candidate trace (`applied`, reject reasons, runtime gate inputs,
      candidate bucket/source kinds/verb family/text) so real-shard before /
      after reruns can audit selected quiet rows, blocked-lane contamination,
      and cross-surface owner carry without inventing a new runtime path
  - support rows may no longer promote themselves into stronger claims; support
    can justify an admitted claim, but it may not replace a missing canonical
    claim or upgrade a weak one
  - Bookmaker now consumes planner output rather than raw support bridges:
    `QuestionFirstCommentaryPlanner.primary` is mapped to
    `claim/evidenceHook/coda`, `secondary` is support-only, and direct
    `mainBundle` / `quietIntent` compression is reserved for exact factual
    fallback only
  - bookmaker / chronicle support slots now run through one backend-only
    `CertifiedDecisionFrameBuilder` bridge:
    the move-local claim still leads, and only after that claim survives may
    runtime append at most two certified support sentences from the fixed
    `Intent`, `Battlefront`, and `Urgency` axes
  - that decision-frame bridge keeps strict probe-backed ownership:
    `Intent` and `Battlefront` require a probe-backed main strategic plan plus
    alignment with the admitted move claim and concrete route / target / theme
    carriers; raw strategy-pack text, generic shell families, latent ideas,
    `pv_coupled`, and `deferred` carriers remain non-owners
  - `Intent` alignment is now literal rather than merely contextual:
    the chosen target / route / move carrier must match the admitted move-local
    keys or the concrete alignment keys extracted from the probe-backed main
    plan itself; probe-backed plan presence alone is not enough, and raw plan
    labels by themselves do not count as concrete plan alignment
  - in bookmaker / chronicle runtime that alignment is fail-closed:
    if no admitted move-local keys survive (`mainBundle` / `quietIntent`), the
    decision frame may still certify `Urgency`, but it may not surface
    `Intent` or `Battlefront` from probe-backed support carriers alone
  - `Urgency` is a separate certified axis:
    it may describe `immediate`, `pressing`, or `slow` timing only from the
    shared truth mode plus forcing / only-move / tactical-tension signals; it
    may not revive raw plan labels or generic coaching text
  - `Battlefront` certification is now family-aware:
    the selected theater must have at least two independent carrier families
    on the same key, and at least one of them must be a move-local or
    probe-backed-plan anchor; surface / theme support alone cannot satisfy the
    anchor rule
  - decision-frame source extraction is taint-aware at the text level too:
    `latent plan`, `pv coupled`, `deferred`, `the key idea is`, `a likely
    follow-up is`, `a concrete target is`, and similar generic-shell carriers
    are ignored as alignment / battlefront origins rather than merely cleaned
    after rendering
  - active-side theater admission is owner-scoped:
    opposite-side route / target carriers do not contribute admitted keys or
    battlefront support for the current mover's decision frame
  - active-side carrier filtering follows certified carrier alignment, not
    just the final support sentence wording:
    once `Intent` or `Battlefront` certifies, active route / target / dossier
    carriers stay keyed to the admitted move-local alignment set that backed
    that certification, so a square-focused rendered sentence does not
    accidentally hide an aligned route/dossier carrier
  - bookmaker / chronicle strategic prose now consumes certified delta atoms
    only:
    `MainPathMoveDeltaClaimBuilder` and `QuietMoveIntentBuilder` read
    provenance-aware carrier objects with modality tiers such as `available`,
    `supports`, `advances`, `forces`, and `removes`; weak certificates may
    still surface only with weak lexical envelopes (`still leaves`,
    `continues to allow`, `keeps ... available`), while uncertified causal
    connectors (`therefore`, `thereby`, `which means`) are disallowed
  - `Minimal` and `Tactical` packaging may not surface unsupported `Better is
    ...`, `The concrete square is ...`, or `A concrete target is ...` claims
    unless the shared truth mode policy has already admitted a concrete,
    evidence-backed anchor
  - weak-evidence fallback may choose omission; if no move-local factual claim
    or proving line survives, runtime should prefer saying less over inventing
    a strategic story
  - rollout diagnostics are now preserved without reviving a separate shadow
    runtime:
    ops metrics aggregate claim-certification failure rates from the diagnostic
    sidecar (`claim_cert_blocked_strong_rate`,
    `claim_cert_downgraded_weak_rate`,
    `claim_cert_attribution_failure_rate`,
    `claim_cert_quantifier_failure_rate`,
    `claim_cert_stability_failure_rate`) so production hard-gate behavior still
    leaves an auditable rollout trail
  - legacy probe-fen telemetry is no longer carried as a parallel rollout
    signal:
    position-bound request/result certification is tracked through
    `contract_drop_rate`, so the old `legacy_fen_missing_rate` path is removed
    instead of remaining as overlapping transitional telemetry
  - proving lines are also delta-aware: a cited variation may survive only when
    it proves the tactical point or demonstrates the admitted strategic delta;
    boilerplate “keeps the idea in play” lines are not canonical
- Bookmaker-only corpus tooling now records internal planner selection
  (`primary kind`, `secondary kind`, planner-owned vs exact-factual fallback)
  for signoff review without changing the typed runtime payload
- `BookmakerPlannerSliceBuilder` is the canonical real-PGN slice miner for
  selected-ply Bookmaker signoff: it scans catalog PGNs ply-by-ply, selects Bookmaker-only
  planner-positive or fail-closed moments, and writes a dedicated signoff slice
  manifest rather than reusing the generic mixed-surface slice manifest
- `BookmakerPlannerSignoffRunner` now records deterministic Bookmaker prose from
  the same `truthContract + refs + planner + slots` path that runtime uses;
  outline prose is not canonical signoff output for Bookmaker anymore
- selected-ply Bookmaker signoff must also preserve runtime authoring parity:
  `CommentaryPlayerQcSupport.analyzePly` passes the played UCI as both
  `playedMove` and `prevMove`, matching selected-ply runtime seeding so
  `AuthorQuestionGenerator` and planner signoff observe the same question
  inventory
- Bookmaker slice mining and signoff reruns also reset the UCI engine with
  `ucinewgame` before each before/after position analysis, so planner-positive
  selection is not polluted by prior hash state from unrelated corpus entries
- Bookmaker remains compatible with rule-only signoff and provider-none audit
  runs

Primary files:

- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/CertifiedDecisionFrameBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/MainPathMoveDeltaClaimBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala`
- `modules/llm/src/main/LlmApi.scala`

### Active Note

Current canonical flow:

1. `LlmApi` selects the active-note subset from Chronicle moments.
   - the same Chronicle build also keeps a backend-only per-ply
     `DecisiveTruthContract` sidecar from
     `CommentaryEngine.generateGameArcDiagnostic`; `LlmApi.attachActiveStrategicNotes`
     reuses that sidecar by ply and does not add a new payload field
2. `PlayerFacingMoveDeltaBuilder` and `ActiveBranchDossierBuilder` build the
   active-support carrier bundle for each selected moment.
3. `CertifiedDecisionFrameBuilder` certifies the only support bridge allowed to
   align note-adjacent route / move / target / dossier carriers.
4. `ActiveStrategicCoachingBriefBuilder` replays planner-relevant
   `authorQuestions` / `authorEvidence` from the Chronicle moment and combines
   them with the delta bundle, dossier, decision frame, strategy-pack support,
   and signal digest into one shared planner-input view.
   - replay-safe `openingRelationClaim` / `endgameTransitionClaim` are carried
     through the digest so Active can reconstruct the same legal planner-owned
     domain claim family without promoting raw support text
5. `QuestionFirstCommentaryPlanner` is reused for Active note ownership; Active
   may only pick from planner top-2.
   - Active replay now consumes the same decisive-truth contract that Chronicle
     already bound for that ply; planner replay is no longer contract-free
6. `ActiveStrategicCoachingBriefBuilder` applies the Active-local surface rule:
   - note-body preference is
     `WhyThis > WhatMustBeStopped > WhyNow > WhatChanged`
   - `WhosePlanIsFaster` is never allowed to own the note body
   - a top-2 swap is allowed only when truth ownership, evidence quality, and
     fallback strength do not get weaker
   - replay-side claim ownership scoring preserves legal opening/endgame
     domain primaries so Active does not swap them away for a generic surface
     preference winner
7. the deterministic Active note is planner-first short-form:
   - the note body is built from one selected `primary` question plan only
   - `secondary` never enters the note body
   - for `WhyThis` / `WhyNow`, the first support sentence may replace raw
     `primary.contrast` with `ContrastiveSupportAdmissibility` support drawn
     from existing shared-planner/truth inputs; this may improve support prose
     only and may not change the selected primary
   - that contrast admissibility step now reuses the same per-ply decisive-truth
     contract as the planner replay rather than re-running as a contract-free
     Active-local heuristic
   - planner-owned `WhyNow` short notes now skip support candidates that merely
     restate the same timing shell as the lead, and the surviving lead may be
     rewritten around the chosen move so the note stays independent from the
     carried Chronicle sentence while keeping the same concrete timing owner
   - idea/route/move/target/dossier side surfaces are filtered from that same
     selected planner result and certified decision frame
8. `ActiveStrategicNoteValidator` validates the candidate note.
   - planner-owned `WhyNow` short notes are validated against concrete timing
     ownership rather than the old generic coaching-brief contract; they still
     need a real timing anchor such as a named move, cited line, or explicit
     cp-loss window
   - planner-approved `TacticalFailure`, `MoveDelta`, `OpeningRelation`, and
     `EndgameTransition` primaries now use a planner-aware minimal note
     contract:
     the note may attach when it preserves the selected planner claim plus one
     anchored support / consequence sentence, even if the old strategic-brief
     completeness contract would have failed it
   - for those planner-approved minimal-contract notes, route / plan / focus /
     execution completeness is side-surface warning material only
     (`side_route_missing`, `side_plan_missing`, `side_focus_missing`,
     `side_execution_or_objective_missing`, `side_coverage_low`) and does not
     block note attach on its own
   - planner-owned `WhyNow` may reuse a carried proof sentence only when the
     lead sentence is independently grounded as a new timing claim; reusing the
     prior shell alone still fails the independence gate
   - final Active attach rejection is normalized through one mutually exclusive
     final reason code (`no_planner_primary`, `preselection_blocked`,
     `visible_support_missing`, `note_body_empty`,
     `validator_independence_fail`, `missing_required_route`,
     `missing_required_plan`, `missing_required_focus`,
     `note_body_too_generic`) even though lower-level validator details may
     still be preserved for diagnostics
9. only after a deterministic planner-owned note exists may
   `ActiveStrategicPrompt` attempt wording polish.
10. if planner approval fails, or if the validated note does not survive the
    hard gate, the note and aligned side surfaces are omitted together.

Current rules:

- Active note existence does not depend on provider availability.
- `sourceMode = rule / llm_polished / omitted` stays externally stable.
- `llm_polished` means wording polish over a deterministic draft, not fresh
  canonical note generation.
- planner-approved `WhyNow` may enter the active-note attach subset even when
  the moment was not preselected by legacy `strategicBranch` routing or when
  route/move support is absent; this exception is narrow and exists only for
  concrete timing-owner notes
- planner-approved `TacticalFailure` / `MoveDelta` / `OpeningRelation` /
  `EndgameTransition` notes attach on a narrower planner-first contract than
  the old coaching card:
  the note body must keep the selected claim and one anchored support, but
  side-surface completeness is not itself an attach gate
- active-note emission also obeys the shared player-facing truth mode policy:
  - `Minimal` moments do not create a user-facing active note even if the
    selector kept the moment active internally
  - only `Tactical` or evidence-backed `Strategic` moments may surface a note
- Active note ownership is planner-first:
  - the note body comes from one planner-approved `primary` question only
  - `secondary` is support-only and may filter side surfaces, but it may not
    originate the note body
  - Active may reorder planner top-2 only when the replacement is not weaker on
    truth ownership, evidence quality, or fallback strength
  - replay may reuse carried opening/endgame translator claims from the shared
    digest, but it may not mint a new owner family or promote raw domain
    summaries into the note body
  - `WhosePlanIsFaster` may never own the Active note body; it may survive only
    as certified side-support when another primary already exists
- active/support is no longer a state-summary coaching card:
  - raw `structuralCue`, `structureProfile`, `strategicFlow`,
    `dominantIdeaFocus`, `deploymentPurpose`, `deploymentContribution`,
    `opponentPlan`, `prophylaxisThreat`, `practicalVerdict`, raw
    `longTermFocus`, and `StrategyPackSurface.executionText/objectiveText/
    focusText` may not directly own note prose
  - active/support strategic prose is admitted only from a delta-backed active
    carrier (`PlayerFacingMoveDeltaBuilder`) or contract-owned tactical truth
    - a backend-only certified decision frame may append support language, but
      only after the deterministic lead sentence exists:
      `Intent`, `Battlefront`, and `Urgency` are the only allowed axes, and they
      must come from concrete probe-backed alignment rather than raw strategy
      labels
    - generic active/support families such as `The key idea is ...`, `A likely
      follow-up is ...`, `A concrete target is ...`, and `Further probe work
      still targets ...` are not canonical runtime behavior anymore
    - if no planner-approved primary survives, active note, dossier, visible
      route refs, move refs, and directional targets all omit together rather
      than partially rebuilding a strategic panel
  - active-note visible surface is stricter than active-note selection:
    - the note text itself must survive the shared hard gate
    - if no non-empty validated note survives, user-facing note-dependent
      surfaces (`activeStrategicNote`, dossier, idea/route/move/target payloads)
      are omitted rather than rendered as an empty strategic-note block
    - side-surface incompleteness may not veto an already-valid
      planner-approved note:
      missing route / plan / focus completeness downgrades side-surface
      completeness or records warnings, but attach still follows the note
      contract unless a real hard validator failure remains
    - `LlmApi` may not auto-attach dossier / refs from raw carriers; the note
      and the side surfaces must agree on the same selected planner result and
      certified decision frame
    - active route refs, move refs, directional targets, and dossier are now
      also filtered through that same certified decision frame:
      uncertified `Intent` / `Battlefront` support does not leak through side
      panels, and active-note carrier surfaces align to the same frame that owns
    the note wording
  - legacy generic strategic-shell rewrites (`The key idea is ...`,
    `A likely follow-up is ...`, `A concrete target is ...`) are not
    regenerated by runtime normalization helpers; runtime support phrasing must
    stay on the canonical certified/delta-backed path instead of reviving those
    shells
  - `activeStrategicIdeas` is no longer a dead compatibility field:
    it now derives only from certified `Intent` / `Battlefront` supports, so
    the active-note idea chips share the same owner boundary as the note and
    side-panel carriers
  - frontend active-note rendering uses active-only payload, not
    `strategyPack` fallback, when deciding which idea / execution / objective
    surfaces may appear beside the note
- Active selection and dossier semantics are truth-first:
  - the dossier lens comes from canonical truth semantics when a contract
    exists, not from raw `momentType`, `moveClassification`, or compensation
    digest strings
  - raw label strings remain compatibility / display / no-contract fallback
    fields only
  - selection scoring may keep a private maintenance-exemplar candidate
    focus-visible without widening the public exemplar enum
  - a proof-backed best `only_move_defense` may project as
    `none / primary_visible / neutral`; this keeps a critical hold visible
    without reclassifying it as failure or investment ownership
  - benchmark-critical quiet holds may remain `hidden / neutral`, but they are
    thread-local tie-break material only; they do not receive global
    visible-slot or active-note protection on their own
  - Active selection is split into two internal layers:
    thread-local representative choice first, then global surfaced
    visible / active-note competition under the fixed caps
  - threaded representative selection is truth-aware and now runs through a
    single canonical picker per thread: strong visible / failure-significant
    candidates first, then seed/build/finisher stage coverage, then existing
    ply order as the final tie-break
  - representative ordering stays fixed-cap and truth-first, while protected
    visible selection and the base active-note protected pass now share the
    same canonical protected ordering:
    severe failures first, then one threaded representative promoted
    `Best OnlyMoveDefense` occupant per thread, then non-threaded promoted
    holds, then remaining same-thread promoted duplicates, then
    verified/provisional exemplar and commitment/conversion ownership
  - selector assembly computes that canonical protected stream, visible merge,
    and base active-note protected seed once in a shared core path; runtime
    truth tracing now records only canonical survival outcomes from that path:
    final-internal / visible / active / whole-game promotion plus truth class,
    reason family, surfaced thread id, representative selection, and the
    private maintenance-exemplar candidate bit
  - generic hidden best tactical/technical moves may help same-thread
    replacement only when a thread lacks stronger truth-visible/failure reps;
    they may not outrank those protected families in the global visible /
    active-note caps
  - the protected pass reserves visible / active-note space first, but any
    remaining visible slots are still backfilled by truth-eligible fallback
    moments; protection may not collapse the visible set to only the protected
    family
  - inside the fixed visible `12`, severe failures remain absolute first, and
    the remaining protected visible seats follow that shared canonical
    protected ordering
  - when a protected visible seat already occupies a thread, the selector may
    not spend an additional same-thread `visibleThread` seat on a non-protected
    representative from that thread
  - thread occupancy / same-thread dedupe in protected visible and active-note
    overflow is keyed only by surfaced strategic threads from the audited
    thread builder; raw `activePlan.subplanId` tags that do not survive into a
    surfaced thread may not act as hidden thread ids
  - below the protected and surfaced-thread layers, visible selection now uses
    one unified non-protected stream instead of separate
    compensation/core/fallback ladders; active-note selection mirrors the same
    three-layer shape (`protected base -> thread note -> non-protected stream`)
  - that unified non-protected stream still keys “threaded” only from those
    surfaced thread refs; a fourth-or-lower strategic thread that misses the
    `MaxThreads` cut may still compete through generic visible selection
    instead of being hidden by pre-cap thread tagging
  - active-note overflow is allowed only for `Blunder`, `MissedWin`, and
    promoted `Best OnlyMoveDefense`; exemplar/owner families stay inside the
    base `8`, and overflow still dedupes repeated notes by thread / chain
  - investment-chain dedupe still defaults to one supporting-visible move per
    chain, but a second support is allowed when that move is a private
    maintenance-exemplar candidate or a non-best / failure-significant follow-up
- compensation-positive note survival is closed through the deterministic
  planner-first path; helper compensation fallback text is not a standalone
  owner path for note attachment.
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
- runtime authoring is question-first and evidence-purpose-bound:
  - the live author-question taxonomy is:
    - `WhyThis`
    - `WhyNow`
    - `WhatChanged`
    - `WhatMustBeStopped`
    - `WhosePlanIsFaster`
  - older author-question families (`TensionDecision`, `PlanClash`,
    `TacticalTest`, `StructuralCommitment`, `ConversionPlan`,
    `DefensiveTask`, `LatentPlan`) are not canonical runtime taxonomy anymore
  - `AuthorQuestion.evidencePurposes` is the authoritative probe/evidence
    contract:
    `EvidencePlanner`, `ProbeDetector`, `AuthorEvidenceBuilder`,
    `NarrativeOutlineBuilder`, and `NarrativeOutlineValidator` consume explicit
    purposes instead of inferring proof requirements from legacy question-kind
    names
  - bounded author-evidence purposes are runtime-scoped:
    `reply_multipv`, `defense_reply_multipv`, `convert_reply_multipv`,
    `recapture_branches`, and `keep_tension_branches`
  - restricted-defense conversion certification is also runtime-scoped:
    `NarrativeContextBuilder` evaluates a backend-only certificate for
    `ThemeL1.AdvantageTransformation` experiments using existing
    `PlanEvidenceEvaluator` output, `preventedPlans`, side-to-move eval posture,
    and direct `convert_reply_multipv` / `defense_reply_multipv` evidence
  - certification is intentionally narrow:
    it requires conversion-ready advantage (`>= +200cp` for the side to move),
    at most two distinct defensive first replies, a concrete/stable best
    defense, future-snapshot persistence, and either explicit
    prevented-resource pressure or equivalent counterplay compression
  - uncertified conversion contracts fail closed:
    the experiment evidence tier is downgraded to `deferred` before
    `mainStrategicPlans` are filtered for shared planner/Chronicle/Bookmaker/
    Active reuse
  - counterplay-axis suppression certification is also runtime-scoped:
    `NarrativeContextBuilder` evaluates a backend-only certificate for
    `ThemeL1.RestrictionProphylaxis` experiments with
    `SubplanId.BreakPrevention` / `SubplanId.KeySquareDenial` using existing
    `PlanEvidenceEvaluator` output, `preventedPlans`, validated probe results,
    side-to-move eval posture, and current phase / ply / FEN context
  - this B2b slice is intentionally narrower than B1:
    it accepts only late-middlegame, clearly-better positions with one named
    break/entry axis, route-denial or long-term-restraint validation,
    concrete/stable best defense, future-snapshot persistence, and no tactical
    release or move-order fragility
  - uncertified suppression contracts fail closed the same way:
    the experiment evidence tier is downgraded to `deferred` before planner
    reuse, so no new public payload/schema field is needed
  - planner ownership stays on the existing lane:
    certified evidence-backed restriction-prophylaxis plans may remain in the
    quiet move-delta `WhyThis` path, while threat-owned forcing defense remains
    unchanged and uncertified shells stay outside the primary pool
  - dual-axis bind certification is now runtime-scoped on top of that same
    restriction-prophylaxis lane:
    `NarrativeContextBuilder` evaluates a backend-only
    `DualAxisBindCertification` contract for
    `ThemeL1.RestrictionProphylaxis` experiments with
    `SubplanId.BreakPrevention` / `SubplanId.KeySquareDenial` when current
    `PreventedPlan` evidence shows one named break axis plus one independent
    entry axis
  - this B3b slice is intentionally narrower than B2b:
    it accepts only queen-light late-middlegame, clearly-better positions where
    the named break axis and the independent entry axis each show measurable
    burden, direct-reply best defense is concrete/stable, future-snapshot
    persistence plus bounded continuation stay on that same defended branch, and
    no extra live freeing axis, tactical release, fortress-like static hold, or
    move-order fragility survives
  - uncertified dual-axis contracts fail closed before planner reuse:
    when the dual-axis contract is present and uncertified,
    `NarrativeContextBuilder` forces the experiment evidence tier to
    `deferred` before `mainStrategicPlans` are filtered for shared
    planner/Chronicle/Bookmaker/Active reuse
  - validation probes may corroborate the shell, but they may not replace
    direct best-defense evidence or stitch persistence / continuation from a
    different defended branch; explicit fail-closed reasons include
    `dual_axis_burden_missing`, `direct_best_defense_missing`, and
    `stitched_defended_branch`
  - planner ownership still stays on the existing quiet move-delta lane:
    certified dual-axis plans may surface only as bounded planner-owned
    `WhyThis`; existing move-delta `WhatChanged` remains legal only when it was
    already admissible on its own lane, and no new whole-position or whole-game
    owner path is opened
  - no new public payload/schema field exists for this slice; the certification
    contract is backend-only
  - removed latent probe purposes (`latent_plan_immediate`,
    `latent_plan_refutation`, `free_tempo_branches`) are not owner-path
    contracts anymore
- outline authoring is also question-first:
  - `NarrativeOutline` no longer emits `ConditionalPlan`
  - `QuestionPlannerInputsBuilder` is the shared owner-path bundle for
    `NarrativeOutlineBuilder`, `BookmakerLiveCompressionPolicy`, and
    `GameChronicleCompressionPolicy`
  - the shared bundle is built once from live carriers only:
    `MainPathMoveDeltaClaimBuilder`, `QuietMoveIntentBuilder`,
    `CertifiedDecisionFrameBuilder`, sanitized `DecisionComparison`,
    `AlternativeNarrativeSupport`, `PlayerFacingTruthModePolicy`,
    current-board `preventedPlans`, `PVDelta`, threat tables, opponent plan,
    and cleaned candidate evidence lines
  - `QuestionFirstCommentaryPlanner` is the canonical internal planner for
    question composition
  - planner output is internal-only and ranked as `primary + optional
    secondary + rejected`; no new API/frontend payload fields are exposed in
    Phase 2
  - `DecisionPoint` is no longer built from raw author-question prose; it is
    mapped from planner `claim` plus concrete `contrast`
  - `Evidence` is mapped from planner evidence ownership; bounded
    author-evidence branches stay preferred, while single-line proof hooks use
    internal planner-only purposes and stay validator-scoped
  - `WrapUp` / `TeachingPoint` may consume planner consequences only when they
    remain move-attributed and certified
  - when no admissible question plan survives, outline falls back directly to
    exact factual move text instead of reviving question shells or speculative
    strategic prose
  - planner factual fallback reasons (`missing_claim`, `missing_move_owner`,
    `state_truth_only`, `missing_certified_race_pair`) remain trace-only:
    the surfaced fallback claim comes from literal `exactFactualSentence`
    move-shape semantics, not from those rejection labels
  - frontend probe panels consume structured payloads rather than mined prose

Primary files:

- `modules/llm/src/main/scala/lila/llm/analysis/AuthorQuestionGenerator.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/AuthorEvidenceBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/QuestionFirstCommentaryPlanner.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/EvidencePlanner.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/ProbeDetector.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineValidator.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- `ui/analyse/src/bookmaker/probeOrchestrator.ts`

## Shared Strategic Logic

### Strategy carrier

- `StrategyPack` is the shared strategic payload
- it carries dominant ideas, routes, move refs, directional targets,
  long-term focus, and signal digest
- Chronicle / Bookmaker / Active all consume it through current shared
  extraction logic
- `StrategyPackSurface.Snapshot` no longer carries legacy dead-stub fields such
  as `strategicStack`, `latentPlan`, or `decisionEvidence`; runtime consumers
  must read live digest / evidence carriers instead of reviving those stubs

### Restricted-defense conversion certification

- backend-only helper `RestrictedDefenseConversionCertification` gates only
  `AdvantageTransformation` strategic experiments
- the helper reuses existing `PlanEvidenceEvaluator` data rather than opening a
  new runtime lane:
  reply PVs, best reply, future snapshots, prevented-resource evidence, and
  current eval posture are all read from the existing narrative build inputs
- the helper records a bounded internal contract:
  strategy hypothesis, restricted-defense evidence, defender resources, best
  defense found, defended-branch key, route persistence (including direct
  best-defense presence and same-defended-branch continuity), failure reasons,
  move-order fragility, confidence, and evidence sources
- certification stays bounded to direct best-defense reality:
  future-snapshot persistence must survive on that same defended branch rather
  than being stitched from a different direct-reply fragment; explicit
  fail-closed reasons now include `stitched_defended_branch`
- `NarrativeContextBuilder` applies the contract before exposing
  `mainStrategicPlans`; uncertified conversion experiments become `deferred`
  instead of remaining `evidence_backed`
- `StrategicNarrativePlanSupport.filterEvidenceBacked` is the shared replay
  gate used by `NarrativeContextBuilder`, `CertifiedDecisionFrameBuilder`, and
  `ActiveStrategicCoachingBriefBuilder`, so Chronicle / Bookmaker / Active all
  consume the same downgraded planner pool
- when that pool becomes empty, runtime remains on the existing exact-factual
  fallback path rather than keeping a stronger-looking conversion shell

### Counterplay-axis suppression certification

- backend-only helper `CounterplayAxisSuppressionCertification` gates only
  `RestrictionProphylaxis` strategic experiments with
  `BreakPrevention` / `KeySquareDenial`
- the helper reuses existing narrative build inputs instead of opening a new
  runtime lane:
  `PlanEvidenceEvaluator` output, validated probe purposes
  (`route_denial_validation`, `long_term_restraint_validation`,
  `defense_reply_multipv`, `reply_multipv`), `PreventedPlan` data, and current
  phase / ply / FEN metadata all come from the existing build path
- the helper records a bounded internal contract:
  strategy hypothesis, claim scope (`break_axis` / `entry_axis`),
  squeeze archetype, restriction evidence, enumerated defender resources,
  remaining freeing breaks / tactical releases, best defense found,
  defended-branch key, route persistence (including direct best-defense
  presence and same-defended-branch continuity), fail reasons, move-order
  fragility, reinflation risk, confidence, and evidence sources
- certification is late-middlegame / clearly-better only:
  whole-position `no-counterplay`, fortress claims, and broad squeeze shells
  remain outside admissible positive scope, and a positive B2b shell now
  requires both route-denial / restraint validation and concrete direct
  best-defense evidence on the same defended branch
- `NarrativeContextBuilder` applies the B2b contract before exposing
  `mainStrategicPlans`; uncertified suppression experiments become `deferred`
  instead of remaining `evidence_backed`
- validation-only suppression shells and cross-branch stitched persistence
  bundles fail closed at that builder gate with explicit reasons
  `direct_best_defense_missing` or `stitched_defended_branch`
- `StrategicNarrativePlanSupport.filterEvidenceBacked` remains the shared replay
  gate, so Chronicle / Bookmaker / Active reuse the same downgraded pool and
  whole-game helpers never see a revived positive shell
- `QuestionFirstCommentaryPlanner` keeps the existing ownership architecture:
  for already-certified suppression plans, `prevented_plan` support no longer
  overrides the quiet move-delta `WhyThis` owner; concrete threat-based forcing
  defense is unchanged

### Dual-axis bind certification

- backend-only helper `DualAxisBindCertification` gates only the first B3 slice:
  `RestrictionProphylaxis` strategic experiments with
  `BreakPrevention` / `KeySquareDenial` when current `PreventedPlan` evidence
  shows one named break axis plus one independent entry axis
- the helper reuses the existing narrative build inputs instead of opening a new
  runtime lane:
  `PlanEvidenceEvaluator` output, validated probe purposes
  (`route_denial_validation`, `long_term_restraint_validation`,
  `defense_reply_multipv`, `reply_multipv`, `convert_reply_multipv`),
  `PreventedPlan` data, and current phase / ply / FEN metadata all come from
  `NarrativeContextBuilder`
- the helper records a bounded internal contract:
  strategy hypothesis, local claim scope (`dual_axis_local`), primary and
  corroborating axes, axis independence, bind archetype, restriction evidence
  (including per-axis burden flags), enumerated defender resources, remaining
  freeing axes / tactical releases, best defense found, defended-branch key,
  persistence after best defense, route continuity (including direct
  best-defense presence and same-defended-branch continuity), fortress risk,
  move-order fragility, reinflation risk, fail reasons, confidence, and
  evidence sources
- certification is intentionally narrower than B2b:
  queen-light late middlegame only, clearly-better posture only, dual-axis
  `break suppression + entry denial` only; both axes must each carry
  measurable restriction burden, direct best-defense evidence is mandatory, the
  bounded continuation proof must stay on the same defended branch, and
  whole-position `no-counterplay`, fortress-break, color-complex positives,
  mobility-cage positives, and whole-game positive reuse all remain outside
  admissible positive scope
- `NarrativeContextBuilder` applies the B3b contract after the
  restricted-defense evidence-tier gate and before the B2b single-axis
  player-facing tier is finalized; when the dual-axis contract exists and is
  uncertified, the experiment becomes `deferred` instead of remaining
  `evidence_backed`
- validation-only shells, single-axis burden shells, and cross-branch stitched
  proof bundles fail closed at that builder gate rather than borrowing broader
  validation support into a positive B3b certification
- `StrategicNarrativePlanSupport.filterEvidenceBacked` remains the shared replay
  gate, so Chronicle / Bookmaker / Active all consume the same downgraded pool
  and whole-game helpers never see a revived positive dual-axis shell
- `QuestionFirstCommentaryPlanner` reuses the same containment rule already used
  for certified restriction-prophylaxis clamps:
  dual-axis positives may surface only as bounded planner-owned `WhyThis`,
  while uncertified shells may not re-inflate into forcing-defense, payoff,
  wrap-up, or whole-position owner lanes

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
- parity eligibility is only a candidate-selection helper:
  planner approval still owns note attachment, and compensation helpers do not
  create a note on their own

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

This section is the maintained prompt-family contract. No separate prompt-policy
markdown file is authoritative anymore.

## Provider Split and Signoff Path

### Runtime defaults

- `LLM_PROVIDER` default remains `openai`
- `LLM_PROVIDER_ACTIVE_NOTE` default remains `gemini`

This split is a runtime polish-routing choice, not a canonical attach contract.

### Signoff / eval path

- audit-grade evaluation uses the real-PGN signoff runner
- signoff path is deterministic canonical first
- provider-none eval remains the canonical measurement path for release signoff
- rerun outputs under the local quality-audit temp root are local evidence only
- historical thesis snapshot tooling is removed from canonical signoff:
  `BookmakerProseGoldenTest`, `BookmakerProseGoldenDump`,
  `BookmakerThesisQaRunner`, and `src/test/resources/bookmaker_thesis_goldens`
  are stale non-canonical regressions, not release gates
- merged shard signoff must preserve the canonical positive-exemplar gate
  carried in the truth inventory; exemplar coverage is not reduced to the
  intersection of exemplar keys with the current main-corpus focus set
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

### Step 7 Signoff Snapshot

Current deterministic rerun status for Step 7 final signoff:

- stable shadowgames comparison
  (legacy shadow-summary artifact -> planner-surface shadow summary)
  preserves the conservative fail-closed target behavior while improving
  visible carry:
  Chronicle visible planner-owned moments moved `1 -> 2`, Active visible
  attached notes moved `0 -> 1`, blank-like stayed `0 -> 0`, and
  `missing_move_owner` dropped `21 -> 20`
- current real-slice selected-target rerun
  (planner-surface real summary under the local quality-audit report root)
  remains fail-closed on target moments (`15` factual fallback, `114` omitted,
  no blank-like output) while visible whole-game carry keeps `42` Chronicle
  planner-owned moments and `41` Active attaches
- current visible cross-surface owner carry stays aligned on the shared
  planner-owned overlap:
  `41` Chronicle/Active shared planner-visible rows, `0` owner-family/source
  divergence
- no Step-5 regression was reintroduced on the rerun:
  admitted-but-unbuildable remains `0`, demote hard-drop remains `0`, and the
  carried owner on same-key shadowgames overlap does not change
- historical `validator_independence_fail` residual from the earlier phase-6
  shadowgames rerun does not reproduce on the current rerun; the only current
  visible post-primary omission is the fail-closed
  `visible_support_missing` row at
  `2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:106`
- surviving residuals are classified as fail-closed suppressions, not surface
  legality violations:
  `missing_move_owner`, `missing_certified_race_pair`, and
  `decision_timing_support_only_in_v1` remain intentional suppression buckets;
  the single visible `endgame_transition` mismatch above is treated as an
  upstream input-bundle/support-gate issue rather than a replay-layer owner
  revival
- Step 7 verdict: signoff-ready for the question-first shared-planner legality
  contract; remaining residuals are isolated fail-closed suppressions or
  upstream-classified mismatches, not planner/surface owner divergence

### Quality-Audit Scaffold After The Signoff-Ready Snapshot

After the `signoff-ready` snapshot, the parity-baseline lane and
quality-rubric lane may extend input-quality diagnostics and internal quality
measurement, but they may not reopen legality, owner admission, or replay
consistency.

Current quality-audit scaffold rules:

- the current quality-audit scaffold is a `src/test` tooling/scaffold advance,
  not a claim that runtime user-facing commentary quality already improved
- same-ply parity is now an upstream-input diagnostic lane, not a legality
  lane
- parity classification must keep `replay_layer_rewrite` separate from
  `upstream_layer_mismatch`
- surface digest bundles are the canonical reason this separation is auditable:
  snapshot / carry / augmentation / bundle hashes are emitted for Bookmaker,
  Chronicle, and Active audit outputs
- the fixed mismatch taxonomy is:
  `bundle_missing`, `carry_mismatch`, `snapshot_skew`,
  `surface_only_augmentation`, `replay_layer_rewrite`,
  `upstream_layer_mismatch`
- the currently allowed `surface_only_augmentation` set is closed to:
  `active_no_primary_vs_chronicle_factual_fallback`,
  `active_attached_vs_chronicle_planner_owned`,
  `active_omitted_after_primary_vs_planner_owned`, and
  `active_attached_vs_bookmaker_planner_owned`,
  `active_no_primary_vs_bookmaker_exact_factual`
- real16 parity currently reports `groupedPlies=107`, `mismatchedPlies=107`,
  `taxonomyCounts={carry_mismatch=2, snapshot_skew=43, upstream_layer_mismatch=62}`,
  `layerCounts={upstream=107}`
- the standalone real16 parity baseline and the contrast-support after-rerun
  are now different artifacts:
  the baseline still shows upstream digest drift, while the contrast-support
  after-rerun has already closed the previously targeted
  `active/bookmaker:surface_only_augmentation=6` and primary
  `bookmaker/chronicle:replay_layer_rewrite=4` blocker set
- the internal LLM judge scaffold is internal-only and must not outrank the truth
  gate or human signoff
- the internal keep/review selector is fixed at:
  `move_attribution_correctness >= 4` to unlock usefulness credit,
  `selectorScore >= 12`, `overclaim_penalty <= 1`
- the contrast-support runtime is signoff-scoped on Bookmaker / Chronicle and
  diagnostic-only on Active, and it stays support-slot only:
  `WhyThis` / `WhyNow` may replace planner `primary.contrast` or the first
  support sentence only when `ContrastiveSupportAdmissibility` marks the
  support admissible from a preexisting shared-planner/truth input; it may not
  reopen owner choice, ranking, or legality
- contrast-support trace fields are fixed at:
  `contrast_source_kind`, `contrast_anchor`, `contrast_consequence`,
  `contrast_admissible`, `contrast_reject_reason`
- contrast-support selector accounting keeps Bookmaker fail-closed rows out of
  gain
  accounting:
  rows that were `planner_owned` before but land at
  `afterBookmakerFallbackMode=exact_factual` after the rerun are classified as
  `after_fallback_blocked`, not `eligible`
- current real16 contrast-support result for the narrow Bookmaker-first
  prototype is signoff-ready, not rollout-ready:
  `totalWhyRows=46`, `contrastEligibleRows=40`, `upstreamBlockedRows=0`,
  `replayBlockedRows=0`, `track1BlockedRows=0`, `questionFilteredRows=0`,
  `eligibleKeepCount=9`, `eligibleRejectCount=31`, `afterFallbackCount=0`,
  `afterFallbackBlockedRows=6`, `changedCount=40`, `degradedCount=0`,
  `track1RegressionStatus=no_track1_regression`
- current real16 Chronicle contrast-support surface verdict is signoff-ready on the
  after-state planner-selected `WhyThis` / `WhyNow` rows:
  `totalWhyRows=46`, `plannerOwnedRows=46`, `factualFallbackRows=0`,
  `primaryMismatchRows=0`, `ownerMismatchRows=0`
- same-key before/after Chronicle deltas on the original Why slice are not
  replay-layer bugs:
  `upstream_input_bundle_mismatch=2`
  (`ForcingDefense/WhyNow -> TacticalFailure/WhyThis`) and
  `intentional_suppression=4` (planner primary dropped before replay, leaving
  Chronicle at factual fallback)
- current real16 Active contrast-support diagnostic lane shows that Active is not a
  viable signoff surface for this narrow experiment:
  on the after-state planner-selected `WhyThis` / `WhyNow` rows,
  `totalWhyRows=46`, `attachedRows=1`, `composeRows=32`,
  `contrastRawRows=1`, `contrastSelectedRows=1`,
  `primaryMismatchRows=0`, `ownerMismatchRows=0`
- forcing Active to stay inside contrast-support scope leaves only diagnostic blocker
  counts, not a usable contrast slice:
  `validator_independence_fail_with_no_contrast=21`
  (`selectedSupport=evidence:19`, `fallback_narrative_tail:2`,
  `contrast missing_raw=21`),
  `missing_required_plan_with_no_contrast=7`
  (`selectedSupport=evidence=7`, `contrast missing_raw=7`),
  `note_body_empty_from_support_duplicate_claim=3`
  (`support_duplicate_claim=3`, `sentence_count_1_of_2=3`),
  `legacy_preselection_blocked=14`
- because Active contributes only one contrast-bearing Why row on the current
  real16 shard, and that row already attaches, contrast-support is
  canonically cut to Bookmaker + Chronicle signoff scope; reopening Active
  would require a later explicit scope expansion, not a silent lane drift
- current pair-scoped blocker decomposition on those `46` Bookmaker target rows is
  now empty:
  `bookmakerPairBlockerCounts={}`, `upstreamPairBlockerCounts={}`,
  `replayPairBlockerCounts={}`;
  with the blocker set closed, the remaining primary admissibility blocker on
  real eligible rows is `missing_concrete_consequence=30`, with
  `question_outside_scope=0` and `vague_engine_preference=1` secondary
- the current real shard now proves narrow positive gain on a chosen-best tactical
  subset without reopening quality-rubric regressions:
  nine eligible rows now keep, and the aggregate deltas stay flat-to-better
  (`contrastUsefulnessDelta=0`, `dryButTruePenaltyDelta=-0.15`,
  `moveAttributionDelta=0`)
- aggregate maintenance baseline after the Chronicle / Active contrast-support rerun
  reads:
  Bookmaker `keep=55/review=52/gateFail=0`, Chronicle
  `keep=57/review=50/gateFail=0`, Active `keep=2/review=105/gateFail=57`,
  `crossSurfaceVerdictDisagreementCount=55`
- current cross-surface contrast-support verdict is:
  Bookmaker `signoff-ready`, Chronicle `signoff-ready`,
  Active `scope-cut (diagnostic-only)`; quality-rubric regression remains
  `no_track1_regression` on the Bookmaker signoff path, and `rollout-ready` is
  still not claimed
- the corpus-maintenance lane may add scene buckets, but it may not create new
  owner families or bypass shared-planner legality
- the current Track 4 scene-coverage lane is `close-candidate` on the
  report-only corpus-maintenance path:
  the important bucket set is materialized with target candidate / curated /
  stable-fixture coverage, and the final `complex_sacrifice` widening probe
  added no new failure class

Detailed quality-audit artifacts and sample rows live in the local quality-audit
report root; use
[`CommentaryProgramMap.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md)
for the current CQF track map and status summary.

Primary tools:

- real-PGN signoff runner in `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalRunner.scala`
- signoff report merge in `modules/llm/src/test/scala/lila/llm/tools/RealPgnNarrativeEvalReportMerge.scala`

## Frontend Consumption

Current frontend rule:

- frontend consumes typed payloads
- frontend renders structure
- frontend does not reconstruct hidden strategy from free-form prose
- Chronicle frontend decision-comparison support consumes only canonical
  `signalDigest.decisionComparison`
- `topEngineMove` remains a fallback/debug payload input and may not be
  reprojected into a user-facing decision-comparison surface when the canonical
  digest is absent
- bookmaker controller / decoder no longer serialize or expect legacy
  `latentPlans` / `whyAbsentFromTopMultiPV` fields on the user-facing runtime
  response
- compact support / decision-compare frontend helpers no longer carry
  `holdReason` fallback state; support secondary text must come from surviving
  canonical decision-comparison surface only
- frontend support-text cleanup may normalize helper wording, but it may not
  rewrite support rows back into generic shells such as `The key idea is ...`,
  `A likely follow-up is ...`, or `A concrete target is ...`
- narrative advanced-details surfaces do not rebuild omitted strategic panels
  from `strategyPack` fallback; only surviving active carriers may render those
  panels
- no new user-facing wire fields are introduced for Stage-4 uplift:
  certified decision-frame state stays backend-only and may appear only in
  internal audit/debug paths, while Bookmaker / Chronicle / Active prose
  consumes it without changing the typed runtime payload shape

## Module Map Appendix

Current owner map for Stage-4 surface uplift:

- live owners:
  - `PlanEvidenceEvaluator`
  - `RestrictedDefenseConversionCertification`
  - `DualAxisBindCertification`
  - `PlayerFacingTruthModePolicy`
  - `MainPathMoveDeltaClaimBuilder`
  - `QuietMoveIntentBuilder`
  - `CertifiedDecisionFrameBuilder`
  - `QuestionFirstCommentaryPlanner`
  - `BookmakerLiveCompressionPolicy`
  - `GameChronicleCompressionPolicy`
  - `ActiveStrategicCoachingBriefBuilder`
- support-only promotion candidates:
  - `StrategyPackBuilder` / `StrategyPackSurface`
  - `StrategicSentenceRenderer`
  - `ActiveThemeSurfaceBuilder`
- remain non-owner / suppressed:
  - `latentPlans`
  - `whyAbsentFromTopMultiPV`
  - generic active/support strategic families
  - old thesis / hold-reason revival paths

Primary files:

- `ui/analyse/src/bookmaker.ts`
- `ui/analyse/src/bookmaker/responsePayload.ts`
- `modules/llm/src/main/scala/lila/llm/analysis/RestrictedDefenseConversionCertification.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/DualAxisBindCertification.scala`
- `ui/analyse/src/narrative/narrativeCtrl.ts`

## Source-of-Truth Document Set

Current maintained markdown contracts in `modules/llm/docs`:

- `CommentaryPipelineSSOT.md`
  - full commentary-analysis runtime, prompt ownership, Bookmaker prose
    boundary, and signoff contract
- `CommentaryTruthGate.md`
  - decisive-truth signoff contract and GM-audit grading

Older narrative update-log style sections are intentionally removed from this
document. The current-state contract above is the only maintained markdown
authority for the commentary pipeline.

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
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerLiveCompressionPolicy.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/practical/ContrastiveSupportAdmissibility.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/GameChronicleCompressionPolicy.scala`
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
