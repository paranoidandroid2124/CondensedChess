# Commentary Trust Hardening

This document is the canonical trust-hardening map for the Chesstory
commentary-analysis pipeline.

Use it when work is primarily about:

- false positive reduction
- overclaim reduction
- overgeneralization control
- out-of-scene generalization control
- cross-surface consistency
- fallback truth rewrite risk
- support-only overreach
- lesson-readiness gating

This document does not replace the runtime audit or truth gate.

Use the documents in this order:

1. `CommentaryProgramMap.md`
2. `CommentaryPipelineSSOT.md`
3. `CommentaryTruthGate.md`
4. this file

## Canonical Role

- `CommentaryProgramMap.md`
  - onboarding, current status, roadmap, active priority
- `CommentaryPipelineSSOT.md`
  - canonical runtime audit
- `CommentaryTruthGate.md`
  - canonical release/signoff truth gate
- `CommentaryTrustHardening.md`
  - canonical trust-risk map, CTH audit baseline, trust-hardening priority map

## Current Status

- Step 1-7:
  `signoff-ready`
- CQF Track 0-4:
  completed or maintenance-only enough that Track 5 is no longer the immediate
  next priority
- CQF Track 5:
  explicitly deferred
- current priority:
  `CTH`

CTH exists because Track 5 would otherwise amplify unresolved trust-critical
problems:

- false positive
- overclaim
- overgeneralization
- out-of-scene generalization failure
- cross-surface inconsistency

## CTH Structure

### CTH-0 — Full Pipeline Audit

Purpose:
establish the full trust-owning path inventory, trust-risk map, lexicon
authority map, and Track 5 defer rationale.

Status:
complete as an audit baseline; this document records that baseline and must be
updated when later trust-hardening work changes the audited behavior.

### CTH-A — Trust Hardening

Purpose:
reduce false positive, overclaim, unsupported generalization, fallback truth
rewrite, and cross-surface inconsistency without reopening legality/ranking.

Status:
next implementation priority.

### CTH pre-pass — narrow dead-code census

Purpose:
remove dead helpers inside the trust-critical path only when they obscure
authority/risk reading without changing runtime trust semantics.

Current note:
the pre-pass removed unreferenced support/lesson helpers that were not part of
the current runtime or test path, so later CTH-A work can audit live authority
and trust risk without dead lesson/support branches in the way.

### CTH-B — Search-Backed Strategic Discovery

Purpose:
expand explainable strategic depth only after CTH-A makes the trust envelope
stronger.

Status:
design/research only until CTH-A closes the primary trust risks.

## CTH-0 Audit Baseline

### 1. Truth-Owning Path Inventory

#### Evidence carriers

`NarrativeContextBuilder.scala` aggregates probe, threat, pawn, plan, semantic,
opening, endgame, practical, and counterfactual signals into `NarrativeContext`.
This layer is an evidence carrier, not the canonical truth owner.

Support-only producer families include:

- `AlternativeNarrativeSupport.scala`
- `StrategicNarrativePlanSupport.scala`
- `NarrativeSignalDigestBuilder.scala`
- `StrategyPackBuilder.scala`
- `PlayerFacingMoveDeltaBuilder.scala`

#### Canonical truth owners

The decisive truth owner is `DecisiveTruth.scala`.

Its core truth-bearing artifacts are:

- `MoveTruthFrame`
- `DecisiveTruthContract`

Claim strength, ownership, compensation allowance, and benchmark allowance are
derived there.

Planner-level claim admissibility is then enforced by
`QuestionFirstCommentaryPlanner.scala`, where support material, close-candidate
material, and raw opening/endgame replay claims are demoted to support-only
unless they satisfy the planner gates.

#### Surface consumption

Chronicle is the most canonical truth-bound surface path:

- `CommentaryEngine.scala`
  builds the truth frame / truth contract
- sanitized context and strategy pack then feed outline / render

Bookmaker is also truth-bound:

- `BookmakerLiveCompressionPolicy.scala`
- `LlmApi.scala`

It stays planner-first except when exact-factual fallback is explicitly taken.

Active is the main exception:

- `LlmApi.scala`
  now reuses the backend-only per-ply decisive-truth sidecar from
  `CommentaryEngine.generateGameArcDiagnostic` for Active attach
- `ActiveStrategicCoachingBriefBuilder.scala`
  now replays planner output and contrast-support admissibility with that same
  contract instead of `truthContract = None`
- `ActiveStrategicNoteValidator.scala`
  remains a post-hoc attach gate

This closes the primary contract-free replay gap, but Active remains the main
trust-divergent surface because validator-gated attach/omit behavior still
keeps the surface diagnostic-only.

#### Fallback truth rewrite points

The primary fallback truth rewrite sites are:

- `DecisiveTruth.scala`
  - `fallbackMomentProjection`
    - before CTH-A Step 2b, this could reconstruct investment / maintenance /
      conversion owner semantics from serialized `truthPhase`,
      `surfacedMoveOwnsTruth`, `investmentTruthChainKey`, payoff-anchor, and
      `transitionType` fields when the canonical contract was absent
- `QuestionFirstCommentaryPlanner.scala`
  - factual fallback branches such as `missing_claim`,
    `missing_move_owner`, `state_truth_only`,
    `missing_certified_race_pair`
- Active contract-free replay

Frontend does not mint new truth, but it does give support material direct
user-facing implication via:

- `bookmaker.ts`
- `narrativeView.ts`

### 2. Lexicon / Template Authority Map

The existing lexicon and outline assets are not authority-tagged in code. CTH
treats that as a structural risk.

| Asset | Current use site | Authority class | Primary risk | Track 5 eligibility |
| --- | --- | --- | --- | --- |
| `momentBlockLead` | `CommentaryEngine.scala` | `render_only` | `out_of_scene_generalization` | No |
| `hybridBridge` | `CommentaryEngine.scala` | `render_only` | `unsupported_generalization` | No |
| `getOpening` | `NarrativeOutlineBuilder.scala` | `unsafe_as_truth` | `out_of_scene_generalization` | No |
| `getOpeningReference` | `BookStyleRenderer.scala`, `NarrativeOutlineBuilder.scala` | `support_only` | `support_only_overreach` | Evidence-only, not lesson core |
| `getThreatStatement` | `NarrativeOutlineBuilder.scala` | `requires_move_linked_anchor` | `overclaim_strength` | Maybe, only with move-linked anchor |
| `getPlanStatement` | `NarrativeOutlineBuilder.scala` | `requires_move_linked_anchor` | `unsupported_generalization` | No now |
| `getPawnPlayStatement` | `NarrativeOutlineBuilder.scala` | `requires_move_linked_anchor` | `out_of_scene_generalization` | Maybe, only with structure + move anchor |
| `getFactStatement` | `NarrativeOutlineBuilder.scala` | `requires_move_linked_anchor` | `false_positive_claim` if detached from board state | Maybe, but fact-only |
| `getPreventedPlanStatement` | `NarrativeOutlineBuilder.scala` | `requires_move_linked_anchor` | `support_only_overreach` | No now |
| `getCompensationStatement` | `NarrativeOutlineBuilder.scala` | `unsafe_as_truth` | `overclaim_strength` | No |
| `getAnnotationDifficultyHint` | `NarrativeOutlineBuilder.scala` | `unsafe_as_lesson` | `out_of_scene_generalization` | No |
| `getAnnotationTagHint` | `NarrativeOutlineBuilder.scala` | `unsafe_as_lesson` | `unsupported_generalization` | No |
| `AlternativeNarrativeSupport.renderSentence` | `NarrativeOutlineBuilder.scala` | `support_only` | `support_only_overreach` | No |
| `counterfactualTeachingSentence` | `NarrativeOutlineBuilder.scala` | `candidate_for_future_lesson` | bounded by citation loss | Yes, after explicit lesson gate |
| `shared lesson template` | `NarrativeOutlineBuilder.scala` | `unsafe_as_lesson` | `out_of_scene_generalization` | No |
| `buildSeverityTail` | `NarrativeOutlineBuilder.scala` | `unsafe_as_truth` | `overclaim_strength` | No |

Operational rule:

- `render_only` assets must never become truth owners
- `support_only` assets must never be promoted into lesson truth without a new
  explicit admissibility gate
- `unsafe_as_truth` and `unsafe_as_lesson` assets are red-team targets for
  future trust hardening

### 2A. Step 4 Preflight Status

Current CTH-A Step 4 preflight is structural only.

It does not add authority tags yet, but it now exposes tagging-ready local
fragment boundaries in the live runtime:

- `NarrativeOutlineBuilder.scala`
  - opening precedent comparison now keeps comparison body, ordinary summary,
    and shared-lesson phrasing separate until the final beat stitch
  - main-move annotation prose now keeps terminal/tag/difficulty hint
    selection separate from the final text join, and severity-tail projection
    also stays separate until the last annotation stitch
  - wrap-up assembly now keeps planner consequence, practical verdict, and
    compensation coda separate until final beat assembly
- `CommentaryEngine.scala`
  - whole-game support reuse now passes raw digest / strategy-pack carriers
    through a local projection step before whole-game anchor normalization, so
    helper labels such as `continuity:` or `alignment intent:` are not reused
    verbatim as released whole-game anchors

This is intentionally limited:

- no new public payload/schema
- no authority enum/class yet
- no Track 5 lesson admissibility change yet

### 3. Trust Risk Map

#### False positive claim

Observed in current runtime:

- `ActiveStrategicCoachingBriefBuilder.scala`
- `DecisiveTruth.scala`

Current guards:

- Active validator
- fail-closed fallback behavior

Residual problem:

The main no-contract replay path is now closed, but Active still relies on a
validator/omit layer that can suppress planner-approved rows after replay, so
the surface remains diagnostic-only rather than signoff-clean.

#### Overclaim strength

Observed in current runtime:

- `NarrativeLexicon.scala`
- `NarrativeOutlineBuilder.scala`

Current guards:

- sanitize path in `DecisiveTruth.scala`
- benchmark neutralization in `NarrativeOutlineBuilder.scala`

Residual problem:

lexicon/template assets are not authority-tagged, so prose strength can still
outrun current trust authority.

#### Out-of-scene generalization

Observed in current runtime:

- `NarrativeLexicon.scala`
- `NarrativeOutlineBuilder.scala`

Current guards:

- limited low-value suppression only

Residual problem:

generic strategic phrasing and lesson-like stems can outgrow the current scene
scope.

#### Surface divergence

Observed in current runtime:

- `GameChronicleCompressionPolicy.scala`
- `BookmakerLiveCompressionPolicy.scala`
- `ActiveStrategicCoachingBriefBuilder.scala`
- `LlmApi.scala`

Current guards:

- Chronicle threat-stop preservation
- TruthGate requirement for same-contract consumption

Residual problem:

Bookmaker and Chronicle are mostly contract-bound; Active replay now consumes
the same truth contract, but validator-gated attach/omit behavior still leaves
the surface diagnostic-only and not signoff-aligned.

#### Unsupported generalization

Observed in current runtime:

- `NarrativeSignalDigestBuilder.scala`
- `StrategyPackBuilder.scala`
- `bookmaker.ts`
- `narrativeView.ts`

Current guards:

- planner support-only admission
- some digest/pack sanitization

Residual problem:

support carriers already contain general-purpose meaning and are rendered in
stable UI surfaces.

#### Support-only overreach

Observed in current runtime:

- `AlternativeNarrativeSupport.scala`
- `StrategicNarrativePlanSupport.scala`
- `PlayerFacingMoveDeltaBuilder.scala`
- `bookmaker.ts`
- `narrativeView.ts`

Current guards:

- planner demotion
- active gate
- TruthGate prohibition on raw support sentence families becoming truth owners

Residual problem:

support-only carriers travel through many consumer surfaces.

#### Fallback truth rewrite

Observed in current runtime:

- `DecisiveTruth.scala`
- `QuestionFirstCommentaryPlanner.scala`
- `BookmakerLiveCompressionPolicy.scala`
- `GameChronicleCompressionPolicy.scala`

Structural risk not fully realized yet:

- backend fallback rewrite sites still exist, but the Chronicle frontend
  decision-comparison support box no longer rebuilds
  `DecisionComparisonDigest` from `topEngineMove` when the canonical digest is
  missing
- Chronicle backend exact-factual fallback now stays claim-only even when the
  quiet-support composer emits a candidate, so factual fallback does not
  re-lift support-only digest material into user-facing Chronicle prose
- `DecisiveTruth.fallbackMomentProjection` now fails closed for strategic owner
  families:
  serialized investment / maintenance / conversion shells no longer recreate
  owner/visibility projection when the canonical contract is missing; only raw
  `blunder` / `missed_win` failure fallback remains

Current guards:

- sanitize path in `DecisiveTruth.scala`
- duplicate / contract-safe checks in Bookmaker and Chronicle fallback support
- `narrativeView.ts` now requires canonical `signalDigest.decisionComparison`
  before rendering decision-comparison support
- Chronicle factual fallback rejects quiet-support lift after planner-owned
  surface failure and keeps the composer trace diagnostic-only
- no-contract `fallbackMomentProjection` no longer trusts serialized ownership
  or transition strings for strategic-family replay
- planner exact-factual fallback now stays literal move-shape only:
  `missing_claim` / `missing_move_owner` / `state_truth_only` /
  `missing_certified_race_pair` remain trace labels, and ambiguous captures
  fail closed to `This captures.` instead of generalized simplifying/exchange
  prose

#### Blocked-lane contamination

Observed in current runtime:

- Chronicle quiet-support fallback attach path
- Bookmaker quiet-support fallback attach path
- omitted Active note surfaces and frontend support/detail blocks

Current guards:

- fail-closed classification
- TruthGate/SSOT blocked-lane prohibitions
- duplicate/same-sentence rejection on Bookmaker/Chronicle

Residual problem:

omitted strategic panels still create a structural contamination risk family,
but the former `topEngineMove -> DecisionComparisonDigest` Chronicle frontend
fallback path is now closed, and Chronicle backend exact-factual fallback no
longer appends quiet-support prose.

### 4. Frontend Payload Risk Map

| Payload field | Source definition | Current render site | Classification | Risk |
| --- | --- | --- | --- | --- |
| `strategyPack.strategicIdeas`, `pieceRoutes`, `pieceMoveRefs`, `directionalTargets`, `longTermFocus` | `responsePayload.ts`, `models.scala` | `bookmaker.ts`, `narrativeView.ts` | `support_only carrier` | `unsupported_generalization` |
| `signalDigest.decisionComparison` | `models.scala`, `responsePayload.ts` | `bookmaker.ts`, `narrativeView.ts` | `support_only`, benchmark-adjacent | `fallback_truth_rewrite` if a non-canonical frontend/backend reprojection is reintroduced |
| `signalDigest.structuralCue`, `structureProfile`, `centerState` | `models.scala` | `bookmaker.ts`, `narrativeView.ts` | `support_only` | `unsupported_generalization` |
| `signalDigest.prophylaxisPlan`, `prophylaxisThreat`, `counterplayScoreDrop` | `models.scala` | `bookmaker.ts`, `narrativeView.ts` | `support_only` | `support_only_overreach` |
| `signalDigest.compensation`, `compensationVectors`, `investedMaterial` | `models.scala` | `bookmaker.ts`, `narrativeView.ts` | `support_only`, contract-sensitive | `overclaim_strength` |
| `activeStrategicNote`, `activeStrategicIdeas`, `activeStrategicRoutes`, `activeStrategicMoves`, `activeDirectionalTargets`, `activeBranchDossier` | `GameChronicleResponse.scala` | `narrativeView.ts` | `Active-only surface` | `surface_divergence` |
| `topEngineMove` | `GameChronicleResponse.scala` | `narrativeView.ts` | `fallback input only` | `blocked_lane_contamination` residual; Chronicle decision-comparison fallback rewrite closed |

### 5. Surface Consistency Audit

#### Bookmaker

Bookmaker remains the most stable trust-bound surface.

- planner-first
- exact-factual fallback only when needed
- narrow secondary swap only in constrained conditions

Accepted residuals:

1. `WhatMustBeStopped -> WhyNow` secondary swap is allowed only inside the same
   `ForcingDefense` family when the primary contrast is
   `QuestionOutsideScope`.
2. exact-factual fallback may still receive one quiet-support sentence, but
   only if duplicate rejection and `bookmakerContractSafe` pass.
3. planner/direct exact-factual fallback may stay as a narrow residual only at
   literal move-shape scope; ambiguous captures may not pick up
   simplification/exchange meaning.

#### Chronicle

Chronicle is mostly contract-bound but allows more implication variance than
Bookmaker.

Accepted residuals:

1. top-2 swap is allowed only when Chronicle priority is higher and
   `notWeaker` holds.
2. quiet support is attached only for `MoveDelta + pv_delta` primary rows with
   no existing support and no duplicate sentence; exact-factual fallback stays
   claim-only.
3. no-contract fallback may still preserve raw `blunder` / `missed_win`
   failure classification as a narrow exact-factual residual; strategic owner
   families still require canonical truth or separate support-only fallback.
4. threat-stop primary is preserved even when `WhyNow` would otherwise outrank
   it.

#### Active

Active must not be documented as an accepted residual surface.

Canonical status:

- `diagnostic-only`
- `scope-cut`

Structural problem:

it now consumes the same decisive truth contract at replay time, but the
surface still is not an accepted residual because validator/omit behavior can
fail closed after planner replay and Active remains scope-cut.

### 6. Track 5 Readiness Verdict

Track 5 must remain deferred.

Why:

1. existing lexicon/template assets are not authority-tagged
2. fallback truth rewrite still exists
3. support carriers can already surface generalized implication
4. Active remains diagnostic-only even after the contract-alignment slice, so
   lesson extraction would still inherit a non-signoff surface

Without additional guards, lesson extraction would promote fallback or
support-only material into portable lesson truth.

#### Required guards before Track 5

| Required guard | Why it is necessary | First files / paths to touch |
| --- | --- | --- |
| canonical fallback rewrite closure | lesson cannot sit on top of fallback-projected truth | `DecisiveTruth.scala`, `QuestionFirstCommentaryPlanner.scala`, `BookmakerLiveCompressionPolicy.scala`, `GameChronicleCompressionPolicy.scala`, `narrativeView.ts` |
| Active contract alignment | lesson divergence would leak through the Active surface immediately | `ActiveStrategicCoachingBriefBuilder.scala`, `LlmApi.scala`, `ActiveStrategicNoteValidator.scala` |
| lexicon authority tagging | generalized prose cannot be safely reused for lesson without explicit authority classes | `NarrativeLexicon.scala`, `NarrativeOutlineBuilder.scala`, `CommentaryEngine.scala` |
| lesson admissibility gate | only move-linked, scene-scoped, owner-bound, evidence-bound lesson candidates may survive | `NarrativeOutlineBuilder.scala`, `NarrativeLexicon.scala` |

## CTH-A Priority List

1. fallback truth re-projection removal or isolation
2. Active alignment to the canonical decisive truth contract
3. lexicon authority tagging
4. explicit generalization admissibility gate
5. cross-surface trust regression harness
6. negative fixture pack for false positive / overclaim / unsupported
   compensation / out-of-scene generalization / blocked-lane contamination
7. frontend support implication cap
8. opening/endgame replay claim hardening as a fixed regression target
9. support-only overreach audit across runtime and frontend surfaces

## Maintenance Rules

This document is not a one-time audit dump.

Update it whenever trust-relevant behavior changes in:

- fallback truth projection
- surface contract consumption
- lexicon/template authority
- frontend support rendering
- Active contract alignment
- lesson-readiness gating

When a later task claims to reduce false positive, overclaim,
overgeneralization, fallback rewrite, or surface inconsistency, the task should
update this document in the same change.
