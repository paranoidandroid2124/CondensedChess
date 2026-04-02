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

## Current Canonical State

- Step 1-7:
  signoff-ready
- CQF Track 0-4:
  closed or maintenance-only
- CQF Track 5:
  deferred
- CTH-A:
  core complete, maintenance-only
- CTH-B:
  bounded B1 / B2 / B3 remain implemented and adversarial-review green, B4 is
  now `current bounded scope complete` only as one narrow local file-entry bind
  slice, B5b is now `current bounded scope complete` only as one bounded
  negative-first containment slice, and B6 is live only as one narrow
  planner-owned named route-network slice
- CTH-B5a:
  design/recon baseline ready only:
  no B5 runtime slice is open, and the selected next recon family is
  heavy-piece local bind on a negative-first criticism lane
- CTH-B5b:
  bounded negative-first containment slice now `current bounded scope complete`
  only: replayable two-ply same-branch identity, exact-branch replay proof,
  fixed-depth PV1 reproduction, and cross-surface non-reinflation are green;
  heavy-piece positive rollout remains closed
- CTH-B7:
  design/recon/charter baseline ready only:
  no B7 runtime slice is open, and the only candidate allowed into B7b review
  is one same-defended-branch bounded task shift after forced simplification;
  exact-FEN positive control plus an exact-board nasty-case pack remain
  mandatory before any rollout
- CTH-B8:
  `B8a` first-slice design + corpus validation is now
  `still too fuzzy / more corpus work needed`:
  the only plausible unit left is one same-branch sector-local active-plan
  collapse, but current exact-FEN reruns do not produce two independent
  survivors and still fail on progress visibility, family relabel drift,
  heavy-piece leakage, or other-wing escape
- Current operating rule:
  maintain the live B1 / B2 / B3 / B4 runtime, the shipped B5b containment
  slice, and the narrow B6 planner-only route-network slice; keep broader
  B-frontier expansion closed, and let post-B4 work advance only through
  bounded design/recon/charter baselines. B7a defines proof burden only; it
  does not authorize a new runtime path, new owner lane, `WhatChanged`, replay,
  or whole-game reuse. Any B7b work must stay on the existing architecture and
  begin with the fail-closed charter below.
- CTH board-truth validation rule:
  B-track design, self-critique, and validation must stay tied to exact board
  positions plus engine-backed best-defense / best-path verification. Verbal
  sketches alone do not count as strategic validation; if the exact position
  and best line are not established, the claim stays deferred, support-only, or
  negative-first.

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
- strategic-puzzle cross-surface rule:
  the public solve shell is plan-first, while exact line/tree data remains a
  proof/replay layer; v2 should consume that layer only through the nested
  runtime `proof` adapter. Flat legacy tree fields are removed from the public
  shell, stored pre-v2 puzzle docs must be republished, and solve completion
  should trust `planId + startUci` rather than any client-supplied line
  replay. Reveal copy should explain the shared bounded task and accepted
  start before it narrates the proof line. Frontend review labels must stay
  player-facing too: do not surface raw generated `planId` strings where the
  user should see the bounded task, theme, or accepted-start context. Reveal
  first-frame board semantics should also stay plan-first:
  start-applied position first, exact proof board only as a secondary review
  focus

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
core complete on the current trust-hardening tranche; now maintenance-only for
accepted residuals, regression watch, and follow-up frontend/support cleanup.

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
bounded B1 / B2 / B3 slices are implemented, and the first B4 slice is now
implemented too: a bounded late-middlegame local file-entry bind certificate.
The broader implementation frontier still remains unopened. B5a now adds a
canonical design baseline only: heavy-piece local bind is the most reusable
next recon family, but only on a negative-first criticism lane. Do not treat
B4 or B5a as permission to widen runtime semantics into heavy-piece positives,
slightly-better positives, standalone square-network positives,
transition-adjacent positive-route claims, or broader whole-position bind
claims.

### CTH-B0 — Strategic Discovery Recon Baseline

Purpose:
establish the canonical audit baseline for what the current commentary pipeline
can already answer as strategic discovery, what it still cannot answer
without overclaim, and what validation burden must exist before deeper
strategic explanation is implemented.

Status:
complete as an audit-only baseline; use this before any future CTH-B
implementation slice and update it when the strategic-discovery assumptions
change.

Current baseline:

- the current strategic-discovery evidence carrier is
  `lila.llm.model.NarrativeContext`
  with:
  `counterfactual`, `probeRequests`, `mainStrategicPlans`,
  `strategicPlanExperiments`, `strategicFlow`, `opponentPlan`,
  `engineEvidence`, and `authorEvidence`
- `NarrativeContextBuilder.scala` is the main discovery assembly point:
  it builds probe requests, validates plan partitions, derives strategic flow,
  and computes opponent-plan and experiment sidecars
- `PlanEvidenceEvaluator.scala` is the current falsification core:
  it already provides fail-closed plan partitioning, refutation handling,
  alternative-dominance checks, provenance classes such as
  `ProbeBacked`, `StructuralOnly`, `PvCoupledOnly`, and claim-certification
  sidecars
- `ProbeDetector.scala` already contains bounded validation primitives such as
  `theme_plan_validation`, `route_denial_validation`,
  `long_term_restraint_validation`, `convert_reply_multipv`, and
  `defense_reply_multipv`, but these are still hypothesis-validation probes,
  not a whole-position strategic search layer
- `QuestionFirstCommentaryPlanner.scala` already consumes rich strategic input:
  `decisionFrame`, `decisionComparison`, `counterfactual`,
  `evidenceBackedPlans`, `opponentPlan`, `openingRelationClaim`, and
  `endgameTransitionClaim`, but question ownership remains move-local and
  scene-first
- `CertifiedDecisionFrameBuilder.scala` keeps evidence-backed plans aligned with
  battlefront/intention carriers and already suppresses support-only drift
- `StrategyPackBuilder.scala`, `NarrativeSignalDigestBuilder.scala`, and
  `UserFacingPayloadSanitizer.scala` confirm that
  `longTermFocus`, `deploymentPurpose`, `deploymentContribution`,
  `strategicFlow`, and `opponentPlan` are still support carriers or validation
  hints rather than canonical truth owners

Currently answerable query classes:

- `restricted-defense / WhatMustBeStopped`
  - strong:
    planner-owned, threat-backed, and prevented-resource-backed
- `timing-sensitive WhyNow`
  - strong:
    concrete best-defense, only-move, prevented-plan, and decision-comparison
    timing are already admitted
- `WhosePlanIsFaster`
  - bounded:
    requires a certified local race pair and still fails closed when that pair
    is missing
- `move-local WhyThis`
  - bounded:
    explains why the played move serves an admitted idea, not the whole
    position's strategic story
- `WhatChanged`
  - strong:
    move-attributed delta and prevented-plan change remain canonical
- `opening relation` and `endgame transition`
  - strong but narrow:
    domain translator ownership exists, but this is not broad strategic
    discovery
- local conversion / restraint / squeeze hypotheses
  - bounded:
    probe-backed and stability-backed inside current experiment lanes; B1 now
    adds a narrower restricted-defense conversion certificate for
    `AdvantageTransformation` plans only

Not yet answerable without new validation burden:

- whole-position winning conversion routes
- idle / free-tempo plans
- no-counterplay global plans
- fortress-break / bind / squeeze routes as counterexample-resistant route
  certificates
- technical endgame routes beyond narrow transition translation
- long-horizon strategic improvement as a truth-owning route rather than a
  support carrier

Current proof and falsification inventory:

- already present:
  best-defense comparison, counterfactual line comparison, plan stability
  (`bestReplyStable`, `futureSnapshotAligned`), alternative-dominance checks,
  and contract-consistent sanitize/release rules
- still missing:
  whole-position route comparison beyond the B1 restricted-defense slice,
  free-tempo falsifiers, technical endgame route certificates, and a full
  validation matrix over opening / early middlegame / pure endgame plus
  defending-posture cells beyond the broad-validation coverage pack

Broad validation charter baseline:

- phase coverage must separate opening / middlegame / endgame
- evaluation posture must separate winning / equal / defending
- texture must separate tactical / quiet / technical / strategic
- query-family coverage must explicitly include:
  restricted-defense, no-counterplay, free-tempo, conversion, bind, squeeze,
  fortress-break, technical endgame, and long-horizon improvement
- each future CTH-B claim must survive:
  support proof, best-defense resistance, stronger-sibling exclusion, and
  surface-safe rendering
- each future CTH-B claim must also answer the criticism tests:
  cooperative-defense dependence, PV restatement risk, scene-local to
  global-plan overreach, and support-carrier misread risk

CTH-B risk map baseline:

- `cooperative_plan_hallucination`
- `pv_restatement_disguised_as_strategy`
- `long_horizon_overclaim`
- `insufficient_best_defense`
- `local_truth_to_global_plan_overreach`
- `minor_criticism_fragility`
- `surface_reinflation_of_strategic_claim`

B1 slice chosen from the B0 baseline:

- `restricted-defense conversion certification`
- reason:
  this reuses the most existing structure while keeping the falsification
  burden narrow enough to audit
- reuse path:
  `NarrativeContextBuilder -> PlanEvidenceEvaluator -> StrategicPlanExperiment -> QuestionFirstCommentaryPlanner -> DecisiveTruth sanitize -> existing surfaces`
- success condition:
  not richer prose, but proving that a bounded conversion route can survive
  best-defense resistance without cooperative-plan hallucination

### CTH-B1 — Restricted-Defense Conversion Certification

Purpose:
separate real conversion followthrough from cooperative-defense hallucination in
positions where the defender's usable resources are structurally compressed.

Status:
implemented as the first narrow CTH-B runtime slice. The slice reuses the
existing `NarrativeContextBuilder -> PlanEvidenceEvaluator ->
StrategicPlanExperiment -> QuestionFirstCommentaryPlanner -> DecisiveTruth
sanitize -> existing surfaces` path, adds no new public payload/schema, and
keeps the certification contract backend-only.

#### Restricted-defense definition used in B1

B1 treats a position as `restricted-defense` only when all of the following are
true for an `AdvantageTransformation` strategic plan:

- evaluation posture is already conversion-ready:
  side-to-move advantage is at least `+200cp`
- defender resource count is compressed:
  direct reply probes expose at most two distinct first defensive replies from
  `convert_reply_multipv` / `defense_reply_multipv`
- best defense is concrete and stable:
  direct reply evidence exists, a best defense is identified, reply coverage is
  present, and no collapse reason appears on the best-reply branch
- future snapshot persistence survives:
  at least one future snapshot remains positive after the conversion step on
  that same defended branch
- prevented-resource pressure is real:
  current `PreventedPlan` evidence shows counterplay compression through one of
  `counterplayScoreDrop >= 80`, `breakNeutralized`,
  `deniedResourceClass in {break, entry_square, forcing_threat}`,
  `defensiveSufficiency >= 60`, or `breakNeutralizationStrength >= 60`
- counterplay remains compressed after the move:
  either prevented-resource pressure is explicit, or the compressed defender
  reply set is reinforced by positive counterplay/future-snapshot persistence

This is intentionally narrower than "the defender seems short of ideas". B1
requires measured reply compression, concrete best-defense handling, and
evidence that the conversion route still holds after those replies.

#### Conversion certification contract

The runtime contract is private/backend-only and currently lives in
`RestrictedDefenseConversionCertification.scala`. It records:

- `strategyHypothesis`:
  the player-facing conversion idea being tested
- `restrictedDefenseEvidence`:
  measured booleans for reply compression, best-defense stability, future
  snapshot persistence, prevented-resource pressure, and counterplay
  compression
- `defenderResources`:
  the distinct defensive first replies observed in the bounded reply probes
- `bestDefenseFound`:
  the concrete best defensive reply branch, if one exists
- `bestDefenseBranchKey`:
  the normalized defended branch identity that persistence must stay aligned to
- `routePersistence`:
  whether the conversion route still holds after best defense and future
  snapshots, including direct best-defense presence and same-defended-branch
  continuity
- `failsIf`:
  explicit failure reasons such as `cooperative_defense`,
  `hidden_defensive_resource`, `route_persistence_missing`,
  `insufficient_counterplay_suppression`, `move_order_fragility`,
  `pv_restatement_only`, `stitched_defended_branch`, and
  `local_to_global_overreach`
- `moveOrderFragility`:
  whether the route is resilient, fragile, or collapsed by best defense
- `confidence`:
  certification confidence derived from the same bounded evidence
- `evidenceSources`:
  the reply/persistence/prevented-resource sources used for the contract

The contract is consumed by `NarrativeContextBuilder` to downgrade uncertified
conversion experiments from `evidence_backed` to `deferred` before shared
planner/surface reuse. It does not add a new payload field and does not create
a parallel runtime path.

#### Criticism matrix

| Axis | Why risky | Current guard before B1 | B1 verification |
| --- | --- | --- | --- |
| `cooperative_defense` | a nice-looking route exists only if the defender cooperates | probe-backed plan evidence plus reply probes | certification fails unless a concrete best defense is found and the route still survives it |
| `pv_restatement_only` | paraphrasing engine PV can masquerade as strategic discovery | plan evidence already requires bounded probe support | certification fails when direct reply-multipv evidence is missing or when the claim is only a restated line |
| `hidden_defensive_resource` | a supposedly compressed defense may still have several viable holds | `bestReplyStable` / future snapshot checks already existed in partial form | defender resource count is measured explicitly and certification fails when replies stay too broad |
| `move_order_fragility` | the idea works only in one exact order and collapses under the best defensive move order | existing experiment metadata could mark move-order sensitivity | certification converts that fragility into an explicit failure reason and downgrades planner admission |
| `local_to_global_overreach` | a move-local edge is overstated as a whole-position conversion story | truth gate already bans scene-local to global-owner revival | B1 requires conversion-ready eval posture and bounded route persistence; equal/unclear positions fail |
| `surface_reinflation` | Chronicle / Bookmaker / Active or whole-game replay can revive a weakened claim | shared planner/filter path plus factual fallback already existed | uncertified conversion experiments are downgraded before planner reuse, and surface tests require factual fallback instead of revived conversion prose |

#### Fixture scope

The B1 fixture pack now includes:

- `true_restricted_defense_conversion`
- `cooperative_plan_fake`
- `hidden_defense_resource`
- `move_order_fragile_conversion`
- `pv_restatement_only`
- `local_truth_overreach`

Surface-safety coverage is also included through replay/bookmaker regressions
that verify uncertified conversion ideas stay out of Chronicle / Bookmaker /
Active primaries and collapse to exact factual fallback instead of
stronger-looking strategic prose.

#### Broad validation and corpus expansion

Purpose:
stress the existing B1 certificate against a broader late-middlegame /
transition corpus and against real commentary surfaces without widening runtime
semantics.

Status:
validation-only expansion completed. No new runtime helper, public payload
field, or prose family was added. The work stays in test/tooling and reuses the
existing planner/build/replay path. Reverse adversarial review is now green for
the bounded B1 charter after closing cross-branch stitched-persistence
loopholes in runtime/tests.

Broad validation pack:

- contract/builder alignment:
  `RestrictedDefenseConversionBroadValidationTest` checks that the
  certification contract and `NarrativeContextBuilder` gate stay aligned across
  a broader corpus
- real-surface validation:
  replay/bookmaker/chronicle/active expectations are exercised through
  `RestrictedDefenseConversionBroadValidationTest`,
  `SurfaceReplayParityTest`, and the existing bookmaker fixture path
- whole-game support reuse:
  failed restricted-defense conversion ideas are checked to remain support-only
  in wrap-up/whole-game reuse instead of reviving as decisive conversion truth

Acceptance matrix:

- phase:
  - `covered`:
    `late middlegame`, `transition / endgame-adjacent`
  - `deferred`:
    opening, early middlegame, and pure technical endgame cells
  - `failed`:
    none in the targeted broad-validation matrix
- evaluation posture:
  - `covered`:
    clearly winning technical conversion, slightly better but
    non-conversion-ready fail-closed, and equal/unclear fail-closed
  - `deferred`:
    defending / materially worse posture
  - `failed`:
    none in the targeted broad-validation matrix
- texture:
  - `covered`:
    technical simplification, counterplay suppression, quiet improvement before
    conversion, tactical-looking but fail/defer, and move-order-sensitive
    conversion
  - `deferred`:
    broad quiet squeeze certification, free-tempo routes, whole-position route
    search, fortress-break generalization
  - `failed`:
    none in the targeted broad-validation matrix
- criticism class:
  - `covered`:
    `cooperative_defense`, `pv_restatement_only`, `hidden_defensive_resource`,
    `move_order_fragility`, `stitched_defended_branch`,
    `local_to_global_overreach`,
    `surface_reinflation`
  - `deferred`:
    none inside the targeted broad-validation criticism pack
  - `failed`:
    none in the targeted broad-validation matrix
- surface:
  - `covered`:
    Bookmaker, Chronicle, Active, and whole-game/support reuse
  - `deferred`:
    none inside the targeted broad-validation surface pack
  - `failed`:
    none in the targeted broad-validation matrix

Actual surface validation status:

- certified positive restricted-defense conversion currently survives as a
  planner-owned `WhyThis` technical conversion on Bookmaker / Chronicle /
  Active, not as a stronger `WhyNow` timing claim
- fragile or uncertified restricted-defense conversion ideas fail closed to
  exact factual fallback and stay out of planner-owned Chronicle / Active /
  Bookmaker primaries
- whole-game / wrap-up reuse keeps failed restricted-defense conversion support
  as support-only and does not revive it as `decisiveShift` or `payoff`

Failure inventory from broad validation:

- `runtime bug`:
  no new runtime bug remains in the targeted broad-validation pack after the
  `stitched_defended_branch` fix-up
- `overclaim risk`:
  no new overclaim path observed; uncertified cases stayed fail-closed across
  planner and replay surfaces
- `insufficient validation`:
  opening, early middlegame, pure technical endgame, and defending-posture
  cells are still outside the B1 validation pack
- `fixture gap`:
  no new targeted gap inside the B1 matrix; broader CTH-B cells remain future
  work rather than B1 fixture debt
- `acceptable residual`:
  certified positives currently surface as bounded `WhyThis` conversion prose
  rather than preserving a `WhyNow` timing answer; this is acceptable for broad validation
  because it reduces claim strength rather than inflating it

B1 close-readiness after broad validation:

- the narrow restricted-defense conversion certificate now has broad validation
  over its intended late-middlegame / transition cells and real-surface reuse
- B1 should still be read as a bounded certificate, not as general winning-route
  discovery

Explicitly deferred after B1:

- middlegame-wide tactical conversion discovery
- broad quiet squeeze certification outside this restricted-defense slice
- free-tempo and whole-position winning-tree search
- fortress-break generalization
- technical endgame route certificates beyond bounded conversion followthrough
- opening / early middlegame / pure endgame / defending-posture validation
  completion
- Track 5 lesson extraction

### CTH-B2 — No-Counterplay / Quiet Squeeze / Named-Break Suppression

Purpose:
define what must be proven, falsified, and broad-validated before the pipeline
may certify `no-counterplay` or `quiet squeeze` claims.

Status:
design baseline only; no runtime semantics, public payload/schema, or prose
family changed in this step. Use this section as the canonical B2 entry
charter before any later B2 extension slice.

#### Design posture

- proof / falsification first:
  define what must be measured before asking for richer prose
- broad validation before confidence:
  no B2 slice is close-ready from smoke coverage alone
- minor-criticism resistance:
  the slice must survive cooperative-defense, hidden-resource, and
  move-order-fragility criticism without rhetorical escape hatches
- fail-closed over rhetorical richness:
  uncertified squeeze/no-counterplay ideas must downgrade to `deferred`,
  support-only, or exact factual fallback
- architecture reuse only:
  stay inside
  `NarrativeContextBuilder -> PlanEvidenceEvaluator -> StrategicPlanExperiment -> QuestionFirstCommentaryPlanner -> DecisiveTruth sanitize -> existing surfaces`
  and do not open a new runtime lane
- surface inflation forbidden:
  support carriers, Chronicle/Active replay, and whole-game wrappers may not
  strengthen a squeeze/no-counterplay claim beyond the certification result

#### No-counterplay definition draft

For B2, `no-counterplay` is not:

- "the opponent looks passive"
- "there is no immediate tactical threat"
- "one file or square is controlled"
- "engine PV keeps the edge"
- "the position feels easier to play"

For B2, `no-counterplay` is only certifiable as a bounded
`counterplay-axis suppression` claim when all of the following are true:

- a concrete defender plan/resource family is identified:
  freeing break, entry square, forcing threat, release trade, or active-king
  route
- direct best-defense evidence is present:
  bounded reply probes show the meaningful defensive first replies rather than
  only a beautified PV
- meaningful defender resources are compressed:
  the measured reply set is small enough to enumerate and inspect
- no freeing break remains live inside the certified horizon:
  existing `PreventedPlan` / reply / future-snapshot evidence does not leave an
  untested break or entry route standing
- no tactical release remains live inside the certified horizon:
  the best defensive replies do not recover activity through exchange sacrifice,
  perpetual resource, forcing threat, or king activation that reopens the
  position
- the suppression persists after best defense:
  future snapshots and collapse reasons do not show the route falling apart
- the claim scope stays local:
  B2 may certify "the opponent's main counterplay axis is shut down here", not
  "the whole position has no counterplay forever"

Current B2 draft implication:

- the first B2 runtime slice should certify only bounded
  `no meaningful counterplay on the identified break / entry / release axis`
  claims
- whole-position `no-counterplay` remains outside admissible positive scope
  until a later validation burden is met

#### Quiet squeeze definition draft

For B2, `quiet squeeze` is not:

- a generic waiting move
- a lone king or piece improvement
- a pleasant-looking positional move with no measured restriction
- a fortress-looking hold with no certified progress route
- a paraphrased PV that happens to use words like `clamp`, `bind`, or
  `squeeze`

For B2, `quiet squeeze` is a restriction-first route only when all of the
following are true:

- the move is quiet in surface terms:
  it is not relying on immediate tactical refutation/exchange-forcing language
  to justify itself
- the move changes a measurable restriction state:
  break neutralization, route denial, square/file bind, color-complex bind, or
  durable counterplay compression is evidenced
- the restriction is opponent-facing rather than self-congratulatory:
  it must say what defender resource was removed, narrowed, or fixed
- the route persists against best defense:
  the same squeeze shell survives direct reply evidence and future snapshots
- there is a real follow-through path:
  conversion, further restriction, or durable route persistence remains visible
  after the quiet move

Operational distinction:

- `waiting move`:
  no measurable defender-resource reduction and no persistence burden met
- `quiet improvement`:
  local improvement exists, but defender-resource collapse is not proven
- `quiet squeeze`:
  restriction evidence plus best-defense persistence plus route continuity are
  all present

#### Quiet squeeze certification contract draft

Any future B2 contract should remain backend-only and follow the B1 pattern:
evaluate inside `NarrativeContextBuilder`, downgrade uncertified experiments
before `mainStrategicPlans` are filtered, and avoid new public schema.

Proposed bounded contract:

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `strategyHypothesis` | names the tested squeeze/no-counterplay idea | `PlanHypothesis.planName`, existing experiment hypothesis text |
| `claimScope` | keeps the claim local (`break_axis`, `entry_axis`, `route_window`) and blocks global overreach | existing planner scene-first ownership plus new backend-only scope field |
| `squeezeArchetype` | stable role label such as `route_denial`, `file_bind`, `color_complex`, `long_term_restraint` | `PlanEvidenceEvaluator` ontology family plus `ThemePlanProbePurpose` |
| `restrictionEvidence` | records the actual restraining proof | `PreventedPlan`, `counterplayScoreDrop`, `breakNeutralized`, `deniedResourceClass`, denied squares, move-local delta |
| `defenderResources` | enumerates meaningful defender resources instead of gesturing at them | direct `defense_reply_multipv` / `reply_multipv` probe heads and best reply |
| `freeingBreaksRemaining` | lists still-live breaks/entries that falsify no-counterplay | existing `PreventedPlan` + reply/future-snapshot inspection |
| `tacticalReleasesRemaining` | lists still-live tactical release routes that falsify squeeze/no-counterplay | reply probes, `l1Delta.collapseReason`, `keyMotifs`, `futureSnapshot.newThreatKinds` |
| `bestDefenseFound` | anchors the certification to a real defensive try | current best-reply extraction pattern from B1 |
| `routePersistence` | proves the squeeze persists after best defense | `bestReplyStable`, future snapshots, blocker removal / threat-resolution fields |
| `failsIf` | explicit fail-closed reasons | same backend-only failure-list pattern as B1 |
| `moveOrderFragility` | records collapse under best defense or exact-order dependence | existing experiment fragility + B1 move-order pattern |
| `counterplayReinflationRisk` | flags whether replay/support carriers are likely to overstate the claim | current shared replay gate + support-only discipline |
| `confidence` | bounded confidence from measured evidence only | same bounded-score pattern as B1 |
| `evidenceSources` | shows which existing carriers justified the contract | existing hypothesis/probe/prevented-plan source lists |

Mandatory fail-closed reasons for B2 should include at least:

- `cooperative_defense`
- `pv_restatement_only`
- `hidden_freeing_break`
- `hidden_tactical_release`
- `move_order_fragility`
- `local_to_global_overreach`
- `waiting_move_disguised_as_plan`
- `fortress_like_but_not_winning`
- `surface_reinflation`

#### B2 criticism matrix

| Axis | Why risky | Current guard / reuse | Future B2 guard | Required fixture shape |
| --- | --- | --- | --- | --- |
| `cooperative_defense` | the route works only if the defender stays passive | B1 best-defense stability, reply probes, alternative-dominance checks | no certification unless the squeeze survives the concrete best defense | squeeze-looking clamp where passive defense loses but best defense keeps activity |
| `pv_restatement_only` | strategic discovery collapses into paraphrased engine PV | direct-reply requirement in B1, claim quantifier/modality checks | require restriction evidence plus falsification burden, not line prettification | quiet PV with no measured restriction/resource loss |
| `hidden_freeing_break` | one live break invalidates `no-counterplay` | `PreventedPlan.breakNeutralized`, route-denial probe purposes, null-move threat checks | enumerate `freeingBreaksRemaining` and fail if any certified break/entry route survives | clamp-looking scene with one untested pawn break or entry square |
| `hidden_tactical_release` | exchange sac / perpetual / active king route revives counterplay | reply probes, collapse reasons, future snapshots | enumerate `tacticalReleasesRemaining`; require no forcing release under best defense | quiet bind that fails to an exchange sacrifice, perpetual, or king route |
| `move_order_fragility` | one inaccurate order collapses the clamp | existing `moveOrderSensitive`, B1 fragility handling | certification must reject exact-order-only squeeze shells | same plan succeeds in PV order but fails when the defender changes move order |
| `local_to_global_overreach` | a local restraint claim becomes whole-position `no-counterplay` | TruthGate bans scene-local to global revival; B1 uses eval/scope guard | backend `claimScope` must stay local until a later whole-position matrix exists | one denied file/square inflated into a full winning/no-counterplay thesis |
| `waiting_move_disguised_as_plan` | a harmless improving move is mistaken for a squeeze | quiet-intent fallback already exists and is intentionally weak | require measurable restriction delta plus persistence; otherwise demote to quiet intent or exact factual | king/piece improvement with no counterplay compression |
| `fortress_like_but_not_winning` | static restriction is confused with a certifiable squeeze route | B1 conversion-ready threshold and whole-game decisive-proof rules | distinguish `restraining but not winning` from `certified squeeze`; block strong payoff/conversion language | passive-looking bind with no breakthrough and equal/near-equal eval |
| `surface_reinflation` | uncertified squeeze shell returns on Chronicle/Active/whole-game | shared replay filter, exact factual fallback, support-only whole-game checks | B2 experiments must downgrade before planner reuse and remain support-only afterward | uncertified squeeze text present in support carriers but forbidden from every primary surface |

#### Broad validation charter

The B2 validation burden must be read as a positive-certification matrix plus a
negative-corpus matrix:

- positive certification may target only cells marked `likely coverable now`
- any combination touching a `likely deferred` cell needs its own corpus
  expansion before positive certification is accepted
- any combination touching a `likely unsafe` cell is negative-only for the
  early B2 slices and must prove fail-closed behavior rather than positive
  certification

Phase matrix:

| Phase cell | Status | Why |
| --- | --- | --- |
| `late middlegame` | `likely coverable now` | existing prevented-plan, reply-probe, and route-denial signals are strongest here |
| `heavy-piece middlegame` | `likely deferred` | tactical-release density is higher and requires a broader criticism pack |
| `transition / endgame-adjacent` | `likely deferred` | overlaps with fortress and technical-conversion ambiguity |
| `pure endgame` | `likely unsafe` | fortress-like false positives and active-king routes dominate the risk profile |

Evaluation posture matrix:

| Eval posture | Status | Why |
| --- | --- | --- |
| `clearly better` | `likely coverable now` | easiest place to certify bounded counterplay suppression without global overreach |
| `slightly better` | `likely deferred` | squeeze may be real, but winning-route and fortress criticism are harder to separate |
| `equal / unclear` | `likely unsafe` | positive no-counterplay certification would overstate local truth |
| `defending side` | `likely unsafe` | the early B2 slice should not certify the defender as owning a squeeze/no-counterplay route |

Texture matrix:

| Texture | Status | Why |
| --- | --- | --- |
| `prophylactic clamp` | `likely coverable now` | best match for existing prevented-plan and route-denial evidence |
| `square/file bind` | `likely coverable now` | can stay move-local when the denied route is concrete |
| `color-complex squeeze` | `likely deferred` | current signals exist, but counterexample resistance is weaker |
| `quiet king/piece improvement` | `likely unsafe` | too easy to confuse with waiting or generic improvement |
| `restriction-first then conversion` | `likely coverable now` | reuses the B1 conversion tail once the suppression half is certified |
| `hidden-break fake squeeze` | `likely unsafe` | mandatory negative corpus, not positive scope |
| `tactical release case` | `likely unsafe` | mandatory negative corpus, not positive scope |
| `fortress-like near-miss` | `likely unsafe` | mandatory negative corpus, not positive scope |

Surface matrix:

| Surface | Status | Why |
| --- | --- | --- |
| `Bookmaker` | `likely coverable now` | strongest planner-owned/fallback discipline for positive and negative cases |
| `Chronicle` | `likely coverable now` | shared planner path is available, but must mirror the same downgrade rules |
| `Active` | `likely deferred` | omission/parity can be tested now, but positive squeeze ownership remains diagnostic-sensitive |
| `whole-game / wrap-up / support reuse` | `likely unsafe` | early B2 should validate only non-reinflation; positive wrapper reuse is too inflation-prone |

#### Existing capability reuse map

- `NarrativeContextBuilder`:
  remains the main B2 contract evaluation site because it already sees
  prevented plans, validated probe results, eval posture, opponent-plan sidecar,
  and strategic experiments before planner exposure
- `PlanEvidenceEvaluator`:
  remains the falsification core; reuse its quantifier, stability,
  alternative-dominance, attribution, and ontology-family checks so B2 does not
  create a second truth grammar
- `ProbeDetector`:
  already has the relevant bounded probe purposes:
  `route_denial_validation`, `color_complex_squeeze_validation`,
  `long_term_restraint_validation`, `defense_reply_multipv`,
  `convert_reply_multipv`, and `NullMoveThreat`
- `QuestionFirstCommentaryPlanner`:
  should stay unchanged in role; it may consume only already-certified
  experiments through the existing evidence-backed plan pool
- `StrategicNarrativePlanSupport`:
  remains the shared replay gate so Bookmaker / Chronicle / Active all see the
  same downgraded planner pool
- `DecisiveTruth`:
  keeps the same fail-closed replay and wrapper discipline; B2 should not add a
  second projection policy
- `CommentaryEngine`:
  continues to treat whole-game support reuse as support-only unless a stronger
  contract already exists upstream
- existing trust harnesses:
  `RestrictedDefenseConversionCertificationTest`,
  `RestrictedDefenseConversionBroadValidationTest`,
  `SurfaceReplayParityTest`, and
  `CrossSurfaceTrustRegressionHarnessTest` already provide the patterns needed
  for B2 contract tests, corpus expansion, parity checks, and
  surface-reinflation regressions

Why no parallel runtime path is needed:

- B1 already proved that a backend-only contract can gate strategic experiments
  without adding public schema
- the current planner/build/replay path already contains the fail-closed points
  B2 needs
- a parallel path would immediately re-open surface divergence and
  support-carrier reinflation risk

#### Recommended first implementation slice

Recommended first slice:

- `late-middlegame prophylactic clamp / named-break suppression certification`

Narrow scope:

- phase:
  `late middlegame` only
- eval posture:
  `clearly better` only
- texture:
  quiet, restriction-first moves that shut down one concrete freeing break or
  entry route
- positive output cap:
  bounded planner-owned `WhyThis` only as the certified positive lane;
  existing move-delta `WhatChanged` may still survive when independently legal,
  but this slice does not widen a separate timing, whole-position, or
  whole-game positive lane

Why this slice should go first:

- it reuses the strongest existing signals:
  `PreventedPlan.breakNeutralized`, denied-resource classes, reply probes, and
  route-denial/long-term-restraint purposes
- it directly attacks the most important new criticism:
  `hidden_freeing_break`
- it avoids the highest-risk early cells:
  pure endgame fortress cases, color-complex generalization, quiet king
  improvement ambiguity, and whole-game reinflation
- it composes naturally with B1:
  restriction-first suppression can later feed into already-bounded conversion
  followthrough without inventing a new truth lane

Session verdict:

- `CTH-B2 design baseline ready`
- reason:
  B2 now has a canonical definition draft, falsification burden, criticism
  matrix, broad validation charter, and narrow B2 entry slice without
  widening runtime semantics

#### Live runtime slice — late-middlegame prophylactic clamp / named-break suppression

Purpose:
implement the narrowest B2 runtime slice:
bounded certification that a quiet restriction-first move suppresses one named
freeing break or entry route in late middlegame and keeps that suppression
alive against direct best defense on that same defended branch.

Status:
implemented as a backend-only certification helper plus planner/replay
containment. No public payload/schema field or new prose family was added.
Reverse adversarial review is now green for the bounded B2 charter after
closing validation-only best-defense and stitched-branch persistence loopholes.

#### Runtime boundary

- helper:
  `CounterplayAxisSuppressionCertification`
- owner path:
  `NarrativeContextBuilder -> StrategicPlanExperiment evidence tier downgrade -> StrategicNarrativePlanSupport.filterEvidenceBacked -> QuestionFirstCommentaryPlanner -> Chronicle / Bookmaker / Active replay`
- applicable experiments only:
  `ThemeL1.RestrictionProphylaxis` with
  `SubplanId.BreakPrevention` or `SubplanId.KeySquareDenial`
- positive certification only when all of the following hold:
  late middlegame, clearly better eval posture, one named break/entry axis,
  route-denial or long-term-restraint validation, concrete direct best defense,
  future-snapshot persistence on that same defended branch, no live tactical
  release, and no move-order fragility
- positive claim cap:
  bounded axis-only suppression such as
  `this keeps the ...c5 break shut` or
  `this denies the entry route on b4`
- forbidden overclaim remains explicit:
  no whole-position `no-counterplay`, no grand squeeze shell, no winning-route
  payoff prose, and no whole-game positive wrapper reuse

#### Planner and surface behavior

- uncertified suppression experiments downgrade to `deferred` before
  `mainStrategicPlans` are filtered, so Chronicle / Bookmaker / Active all see
  the same reduced planner pool
- certified suppression plans may stay in the move-delta `WhyThis` lane even
  when the same move also carries prevented-plan support:
  `QuestionFirstCommentaryPlanner` now suppresses the
  `prevented_plan -> ForcingDefense` primary-owner path for already-certified
  restriction-prophylaxis plans
- threat-owned forcing defense is unchanged:
  concrete opponent threats may still own `WhyNow` / `WhatMustBeStopped`
- Active remains bounded:
  this slice verifies non-reinflation and replay parity, not a new Active-only
  ownership family
- whole-game remains negative-only:
  support carriers may mention the move locally, but they may not seed decisive
  shift or payoff wrappers

#### Fail-closed criticism handling

- `hidden_freeing_break`:
  fail if more than one live break/entry axis survives
- `hidden_tactical_release`:
  fail if best defense revives forcing tactics, perpetual resources, or active
  release motifs
- `move_order_fragility`:
  fail on collapse reasons, PV-coupled shells, or unstable reply order
- `pv_restatement_only`:
  fail when restriction validation is missing
- `direct_best_defense_missing`:
  fail when only validation probes carry the shell and no direct best-defense
  reply exists
- `stitched_defended_branch`:
  fail when persistence is borrowed from a different defensive branch instead of
  the defended branch established by direct best defense
- `waiting_move_disguised_as_plan`:
  fail when counterplay compression / break neutralization is not measured
- `local_to_global_overreach`:
  fail outside the late-middlegame clearly-better slice and keep claim scope
  axis-local
- `surface_reinflation`:
  fail when the shell language itself overclaims global shutdown or when replay
  tries to promote a downgraded shell

#### Validation baseline

Positive fixtures:

- `true_named_break_suppression`
- `true_entry_route_denial`

Negative / near-miss fixtures:

- `hidden_freeing_break`
- `hidden_tactical_release`
- `move_order_fragile_clamp`
- `pv_restatement_only_quiet`
- `waiting_move_only`
- `slightly_better_but_not_certifiable`
- `heavy_piece_local_overreach_shell`
- `transition_endgame_adjacent_shell`
- `pure_endgame_defending_shell`
- explicit `surface_reinflation` replay fixture

Surface verification:

- planner-owned `WhyThis` parity is green for certified B2 positives
- Chronicle / Bookmaker reuse the same downgraded planner pool
- Active is checked for parity/non-reinflation only
- whole-game support is verified negative-only:
  no decisive-shift or payoff wrapper survives from the uncertified shell

Deferred / unsafe cells remain unchanged:

- deferred:
  heavy-piece middlegame, transition/endgame-adjacent positives,
  slightly-better positives
- unsafe:
  pure endgame positives, equal/unclear, defending side, quiet
  king/piece-improvement squeeze, tactical-release positives, fortress-like
  positives, whole-game positive reuse

Verification:

- `CounterplayAxisSuppressionCertificationTest`
- `CounterplayAxisSuppressionBroadValidationTest`
- `SurfaceReplayParityTest`
- `QuestionFirstCommentaryPlannerTest`
- `BookmakerPolishSlotsTest`
- `CrossSurfaceTrustRegressionHarnessTest`

Session verdict:

- `current B2 slice passed`
- reason:
  late-middlegame named-break / entry-axis suppression is now certified only in
  the narrow bounded slice with direct best-defense plus same-defended-branch
  persistence, and every broader squeeze/no-counterplay surface remains
  fail-closed

#### Close review

Verdict:

- `B2 close-ready`

Reason:

- the implemented B2 slice stays narrow and consistent across runtime, tests,
  and docs:
  late middlegame, clearly better, one named break/entry axis, planner-owned
  local suppression, direct best-defense-backed same-branch persistence, no
  whole-position `no-counterplay`, and no whole-game positive reuse
- criticism coverage is closed on the intended B2 lane:
  `hidden_freeing_break`, `hidden_tactical_release`,
  `move_order_fragility`, `pv_restatement_only`,
  `direct_best_defense_missing`, `stitched_defended_branch`,
  `waiting_move_disguised_as_plan`, `local_to_global_overreach`, and
  `surface_reinflation`
- the remaining cells below stay outside B2 positive scope rather than as open
  B2 blockers:
  heavy-piece middlegame positives, slightly-better positives,
  transition/endgame-adjacent positives, pure endgame positives,
  equal/unclear, defending side, broader quiet squeeze / fortress-like
  positives, and whole-game positive reuse

Positive-scope note:

- certified B2 positives are review-closed on planner-owned `WhyThis`
- existing move-delta `WhatChanged` remains only the preexisting
  independently-legal lane; it is not a broader B2 positive-expansion claim

Recommended next move from the B2 review:

- the next bounded design baseline is completed below
- current follow-on below:
  the narrow late-middlegame dual-axis clamp / bind certification slice

### CTH-B3 — Multi-Axis Squeeze / Bind

Purpose:
define what must be proven, falsified, and broad-validated before the pipeline
may certify bounded `multi-axis squeeze` or `bind` claims.

Status:
design baseline only; no runtime semantics, public payload/schema, or prose
family changed in this step. Use this section as the canonical B3 entry
charter after B2 close-ready and before any later B3 implementation slice.

#### Design posture

- proof / falsification first:
  define what must be measured before the word `squeeze` or `bind` may own the
  explanation
- broad validation before confidence:
  B3 carries a heavier false-positive burden than B2 and cannot be inferred
  from a narrow smoke pack
- minor-criticism resistance:
  the slice must survive cooperative-defense, hidden-resource,
  fortress-near-miss, and move-order criticism without rhetorical escape hatches
- fail-closed over rhetorical richness:
  uncertified multi-axis shells must downgrade to `deferred`, support-only, or
  exact factual fallback
- architecture reuse only:
  stay inside
  `NarrativeContextBuilder -> PlanEvidenceEvaluator -> StrategicPlanExperiment -> QuestionFirstCommentaryPlanner -> DecisiveTruth sanitize -> existing surfaces`
  and do not open a new runtime lane
- surface inflation forbidden:
  Bookmaker, Chronicle, Active, and whole-game/support reuse may not strengthen
  a squeeze/bind claim beyond the upstream certification result
- local bounded claim before whole-position ambition:
  B3 may certify a bounded local `dual-axis squeeze` shell, not a whole-position
  `they have no moves` or global winning-route thesis

#### Multi-axis squeeze definition draft

For B3, `multi-axis squeeze` is not:

- a list of several positional advantages
- one denied break or square repeated with two different labels
- a generic space edge with no measured defender-resource loss
- a quiet move that simply improves one piece or king
- a fortress-looking hold with no visible progress route
- whole-position `no-counterplay` prose

Axis families for the B3 draft are:

- freeing break
- entry route / invasion square / sector access
- file or line access
- king route / king activation corridor
- square network or color complex
- piece mobility cage

For B3, `multi-axis squeeze` is only certifiable when all of the following are
true:

- at least two independent defender resource axes are simultaneously narrowed:
  the claim must identify a primary axis and at least one corroborating axis
- at least one axis is a concrete counterplay route:
  break, entry route, file access, or king route rather than only a pleasant
  structural description
- the axes are non-redundant:
  multiple squares or phrases that merely restate one denied route still count
  as one axis
- each axis is opponent-facing:
  the contract must say what defender resource was lost, delayed, or fixed
- best-defense inspection still leaves the squeeze shell standing:
  direct reply evidence and future snapshots may not reopen a live escape route
- the claim scope stays local:
  B3 may say the move compresses two bounded defender resource axes here, not
  that the whole position is strategically finished

Operational note:

- a second axis may be a square network, color complex, or mobility cage only
  when it adds an independent defender-resource loss beyond the primary break or
  route
- if the supposed second axis disappears once the primary break/entry claim is
  removed, the shell is not multi-axis

#### Certified bind definition draft

For B3, the relevant distinction is:

- `static bind`:
  the position looks cramped or controlled, but best-defense persistence,
  opponent-resource loss, or route continuity is not proven
- `dynamic restriction`:
  at least one defender resource axis is measurably narrowed, but axis
  independence, best-defense persistence, or continuity is still incomplete
- `certified bind`:
  two independent axes are measurably narrowed, best defense does not reopen the
  shell, the shell keeps a bounded continuation route, and no tactical or
  fortress-like objection survives inside the tested horizon

For B3, a `certified bind` therefore requires all of the following:

- measurable restriction:
  prevented-plan, denied-square, counterplay-drop, route-denial, or
  mobility-restraint evidence is present
- dynamic persistence:
  the shell survives the best defensive replies rather than only passive defense
- route continuity:
  future snapshots or conversion-followthrough evidence still show further
  restriction, pressure, or conversion continuity after the bind
- anti-fortress protection:
  the shell is not merely a static hold with no bounded progress route
- bounded language:
  payoff language stays at `bind` / `squeeze` / `resource compression`, not
  whole-position domination or global winning-route language

Operational distinction for early B3:

- `restraining but not winning`:
  may still be a valid negative or support-only outcome when the bind is real
  but continuity or progress is not proven
- `certified squeeze`:
  requires the bind plus bounded continuation continuity; otherwise the result
  must fail closed

#### B3 certification contract draft

Any future B3 contract should remain backend-only and follow the B1/B2 pattern:
evaluate inside `NarrativeContextBuilder`, downgrade uncertified experiments
before `mainStrategicPlans` are filtered, and avoid new public schema.

Proposed bounded contract:

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `strategyHypothesis` | names the tested squeeze/bind idea | `PlanHypothesis.planName`, existing experiment hypothesis text |
| `claimScope` | keeps the claim local (`dual_axis_local`, `break_plus_entry`, `break_plus_network`) and blocks whole-position inflation | existing planner scene-first ownership plus backend-only certification scope |
| `primaryAxis` | records the concrete main defender resource being compressed | `PreventedPlan.breakNeutralized`, `deniedResourceClass`, denied squares, route-denial language |
| `corroboratingAxes` | records the additional independent axes needed for B3 | existing `PreventedPlan` set, denied-square network, `futureSnapshot`, subplan family, existing theme aliases |
| `axisIndependence` | proves the second axis is not just a restatement of the first | existing reply/future-snapshot evidence plus backend-only independence check |
| `bindArchetype` | stable role label such as `break_plus_entry`, `break_plus_network`, `entry_plus_mobility`, `file_plus_king_route` | `ThemeTaxonomy` subplans, `PlanEvidenceEvaluator` ontology family, `ThemePlanProbePurpose` |
| `restrictionEvidence` | records the actual multi-axis proof | `PreventedPlan`, `counterplayScoreDrop`, `breakNeutralized`, denied squares, `defensiveSufficiency`, `breakNeutralizationStrength` |
| `defenderResources` | enumerates the meaningful defensive resources instead of gesturing at them | direct `reply_multipv` / `defense_reply_multipv` probe heads and best reply |
| `freeingResourcesRemaining` | lists still-live breaks, entries, or king/file routes that falsify the bind | existing `PreventedPlan` set plus reply/future-snapshot inspection |
| `tacticalReleasesRemaining` | lists still-live tactical releases that puncture the shell | reply probes, `l1Delta.collapseReason`, `keyMotifs`, `futureSnapshot.newThreatKinds` |
| `bestDefenseFound` | anchors the certification to a real defensive try | current best-reply extraction and alternative-dominance pattern |
| `persistenceAfterBestDefense` | proves the squeeze shell survives the best defense | `bestReplyStable`, future snapshots, blocker removal / threat-resolution fields |
| `routeContinuity` | shows the bind opens a bounded follow-through route without global overreach | `futureSnapshot`, `convert_reply_multipv`, existing conversion-tail evidence |
| `fortressRisk` | blocks static-hold positions from being misread as certified squeeze | future snapshots, eval posture, continuity absence, existing decisive-proof discipline |
| `moveOrderFragility` | records collapse under best defense or exact-order dependence | existing experiment fragility, B1/B2 move-order handling |
| `counterplayReinflationRisk` | flags whether replay/support carriers are likely to overstate the shell | shared replay gate and support-only discipline |
| `claimCertification` | keeps B3 inside the same truth grammar as B1/B2 | `ClaimCertification`, quantifier/modality/attribution/stability/ontology reuse |
| `evidenceSources` | shows which existing carriers justified the contract | existing hypothesis/probe/prevented-plan source lists |

Mandatory fail-closed reasons for B3 should include at least:

- `cooperative_defense`
- `hidden_freeing_break`
- `hidden_tactical_release`
- `pv_restatement_only`
- `move_order_fragility`
- `fortress_like_but_not_winning`
- `local_to_global_overreach`
- `waiting_move_disguised_as_bind`
- `surface_reinflation`
- `axis_independence_not_proven`
- `route_continuity_missing`

#### B3 criticism matrix

| Axis | Why risky | Current guard / reusable guard | Future B3 guard | Required fixture shape |
| --- | --- | --- | --- | --- |
| `cooperative_defense` | the bind works only if the defender stays passive | reply probes, best-reply stability, alternative-dominance checks | no B3 certification unless the shell survives the concrete best defense | dual-axis-looking squeeze where passive defense collapses but best defense keeps one escape route alive |
| `hidden_freeing_break` | one untested break or entry route invalidates the squeeze thesis | `PreventedPlan.breakNeutralized`, denied squares, route-denial probe purposes, future-snapshot blocker checks | enumerate remaining resource axes and fail if a live break/entry/file/king route survives | move appears to stop one resource but a second freeing break or entry square remains |
| `hidden_tactical_release` | exchange sac, forcing threat, perpetual, or king route punctures the shell | `defense_reply_multipv`, collapse reasons, key motifs, future snapshots | require no tactical release under best defense across the tested horizon | bind-looking move that fails once the defender finds an exchange sacrifice, perpetual, or king walk |
| `pv_restatement_only` | engine PV is merely rewritten as `squeeze` or `bind` | claim-certification quantifier/modality checks, probe-purpose contracts, source attribution | require explicit axis bundle, resource loss, and falsification burden rather than pretty PV prose | PV keeps the edge but no distinct axis compression or resource enumeration exists |
| `move_order_fragility` | one move-order change collapses the shell | existing `moveOrderSensitive`, B1/B2 fragility handling, reply/future-snapshot checks | reject exact-order-only shells from B3 certification | same bind works only in the PV order and fails when the defender changes move order |
| `fortress_like_but_not_winning` | static restriction is confused with a certifiable squeeze route | existing whole-position caution, conversion-tail checks, decisive-proof discipline | require bounded route continuity and block strong payoff language when progress is not visible | cramped-looking hold with no breakthrough, no bounded follow-through, and no worsening defender resource state |
| `local_to_global_overreach` | local multi-axis restriction gets inflated into whole-position domination | TruthGate scene-local limits, planner ownership, support-only replay discipline | backend `claimScope` must stay local and B3 positives stay off whole-game reuse | one sector bind is rewritten as `the opponent has no play anywhere` |
| `waiting_move_disguised_as_bind` | a quiet improving move is mistaken for a squeeze | quiet-intent fallback, move-delta anchoring, prevented-plan gating | require opponent-facing multi-axis restriction plus continuity; otherwise demote to support-only or exact factual | king/piece improvement with nicer coordination but no measurable defender-resource loss |
| `surface_reinflation` | uncertified shell returns on Bookmaker, Chronicle, Active, or wrappers as a stronger claim | shared replay gate, `StrategicNarrativePlanSupport`, `CommentaryEngine`, existing parity harnesses | certification must downgrade before planner reuse and remain support-only everywhere else | uncertified squeeze wording appears in sidecars/support carriers and tries to revive on a stronger surface |

#### Broad validation charter

The B3 validation burden must be read as a positive-certification matrix plus a
negative-corpus matrix:

- positive certification may target only cells marked `likely coverable now`
- any combination touching a `likely deferred` cell needs its own corpus
  expansion before positive certification is accepted
- any combination touching a `likely unsafe` cell is negative-only for the
  early B3 slices and must prove fail-closed behavior rather than positive
  certification

Phase matrix:

| Phase cell | Status | Why |
| --- | --- | --- |
| `late middlegame` | `likely coverable now` | existing prevented-plan, route-denial, reply-probe, and future-snapshot signals are strongest here |
| `heavy-piece middlegame` | `likely deferred` | tactical-release density is higher and minor-criticism resistance is harder |
| `transition / endgame-adjacent` | `likely deferred` | route continuity and fortress ambiguity are harder to separate |
| `pure endgame` | `likely unsafe` | fortress-like false positives and active-king resources dominate the risk profile |

Evaluation posture matrix:

| Eval posture | Status | Why |
| --- | --- | --- |
| `clearly better` | `likely coverable now` | easiest place to certify bounded multi-axis compression without global overreach |
| `slightly better` | `likely deferred` | squeeze may be real, but fortress/progress criticism is much harder to separate |
| `equal / unclear` | `likely unsafe` | positive bind certification would overstate local truth |
| `defending side` | `likely unsafe` | the early B3 slice should not certify the defender as owning the squeeze shell |

Texture matrix:

| Texture | Status | Why |
| --- | --- | --- |
| `break suppression + entry denial` | `likely coverable now` | best fit for existing `PreventedPlan` + route-denial evidence and non-redundant axis counting |
| `file / square bind` | `likely deferred` | axis independence is plausible, but overcounting and PV restatement risk are still high |
| `color-complex squeeze` | `likely deferred` | current ontology and probe hooks exist, but counterexample resistance is weaker |
| `piece mobility cage` | `likely deferred` | mobility evidence exists, but tactical-release and overreading risk are still high |
| `quiet king/piece improvement only` | `likely unsafe` | too easy to confuse with waiting or generic improvement |
| `fortress-like near-miss` | `likely unsafe` | mandatory negative corpus, not early positive scope |
| `tactical-release fake squeeze` | `likely unsafe` | mandatory negative corpus, not early positive scope |
| `restriction-first then conversion` | `likely coverable now` | can reuse the existing conversion tail once the local bind is certified first |

Surface matrix:

| Surface | Status | Why |
| --- | --- | --- |
| `planner-owned WhyThis` | `likely coverable now` | strongest owner path for bounded local squeeze/bind explanation |
| `planner-owned WhatChanged` | `likely deferred` | existing move-delta lane remains legal, but B3-specific positive ownership should not begin here |
| `Bookmaker` | `likely coverable now` | strongest planner-first/fallback discipline once upstream certification exists |
| `Chronicle` | `likely coverable now` | shared planner/replay path can mirror the same downgrade rules |
| `Active` | `likely deferred` | parity/non-reinflation can be tested now, but positive bind ownership remains diagnostic-sensitive |
| `whole-game / wrap-up / support reuse` | `likely unsafe` | B3 early slices should validate only non-reinflation, not positive wrapper reuse |

#### Existing capability reuse map

- `NarrativeContextBuilder`:
  remains the main B3 contract evaluation site because it already sees
  prevented plans, validated probe results, eval posture, phase, and strategic
  experiments before planner exposure
- `PlanEvidenceEvaluator`:
  remains the falsification core; reuse its quantifier, stability,
  alternative-dominance, attribution, and ontology-family checks so B3 does not
  create a second truth grammar
- `ThemeTaxonomy`:
  already contains the subplan families B3 needs to group candidate bind axes:
  `BreakPrevention`, `KeySquareDenial`, `FlankClamp`, `CentralSpaceBind`,
  `MobilitySuppression`, `OpenFilePressure`, `StaticWeaknessFixation`, and
  `OutpostEntrenchment`
- `ProbeDetector` and `ThemePlanProbePurpose`:
  already have the relevant bounded probe purposes:
  `route_denial_validation`, `color_complex_squeeze_validation`,
  `long_term_restraint_validation`, `reply_multipv`,
  `defense_reply_multipv`, and `convert_reply_multipv`
- `QuestionFirstCommentaryPlanner`:
  should stay unchanged in role; B3 positives should begin only on
  planner-owned `WhyThis`, while `WhatChanged` remains the preexisting
  independently-legal move-delta lane
- `StrategicNarrativePlanSupport`:
  remains the shared replay gate so Bookmaker, Chronicle, and Active all see
  the same downgraded planner pool
- `DecisiveTruth` and `PlayerFacingTruthModePolicy`:
  keep the same local-scope, ontology, and replay discipline so B3 does not add
  a second projection policy
- `CommentaryEngine`:
  continues to treat whole-game support reuse as support-only unless a stronger
  contract already exists upstream
- existing trust harnesses:
  `DualAxisBindCertificationTest`,
  `DualAxisBindBroadValidationTest`,
  `CounterplayAxisSuppressionCertificationTest`,
  `CounterplayAxisSuppressionBroadValidationTest`,
  `SurfaceReplayParityTest`,
  `BookmakerPolishSlotsTest`, and
  `CrossSurfaceTrustRegressionHarnessTest` already provide the patterns needed
  for B3 contract tests, corpus expansion, parity checks, and
  surface-reinflation regressions

Why no parallel runtime path is needed:

- B1 and B2 already proved that a backend-only certification contract can gate
  strategic experiments without adding public schema
- the current planner/build/replay path already contains the fail-closed points
  B3 needs
- a parallel path would immediately re-open surface divergence and
  support-carrier reinflation risk

#### Recommended first implementation slice

Recommended first slice:

- `late-middlegame dual-axis clamp / bind certification`

Narrow scope:

- phase:
  `late middlegame` only
- eval posture:
  `clearly better` only
- texture:
  `break suppression + entry denial` only:
  one named break or entry route must be compressed, and a second independent
  route or square-network access point must also be shown as narrowed
- positive output cap:
  planner-owned `WhyThis` only;
  existing move-delta `WhatChanged` may still survive when independently legal,
  but B3 does not widen a separate timing, whole-position, or whole-game
  positive lane

Why this slice should go first:

- it reuses the strongest existing signals:
  `PreventedPlan.breakNeutralized`, denied squares, `counterplayScoreDrop`,
  route-denial probes, best-reply stability, and future-snapshot continuity
- it directly tests the core new B3 burden:
  proving non-redundant dual-axis compression rather than single-axis
  suppression
- it avoids the highest-risk early cells:
  heavy-piece tactical-release density, pure-endgame fortress ambiguity,
  color-complex overreading, and generic mobility-cage inflation
- it composes naturally with B1/B2:
  bounded local suppression can later feed the existing conversion tail without
  inventing a new truth lane

Session verdict:

- `CTH-B3 design baseline ready`
- reason:
  B3 now has a canonical bounded definition draft, certification contract,
  criticism matrix, broad validation charter, and narrow B3 entry slice
  without widening runtime semantics

#### Live runtime slice — late-middlegame dual-axis clamp / bind

Purpose:
implement the narrowest B3 runtime slice:
bounded certification that a quiet restriction-first move compresses one named
break axis and one independent entry axis at the same time, keeps that
dual-axis shell standing against direct best defense, and still shows only a
bounded follow-through route on that same defended branch.

Status:
implemented as a backend-only certification helper plus existing
planner/replay containment. No public payload/schema field or new prose family
was added. The B3 fix-up for the known loophole trio is now in place, and the
follow-on reverse adversarial review plus targeted seam review are green for the
bounded B1 / B2 / B3 maintenance charter.

#### Runtime boundary

- implementation site:
  `DualAxisBindCertification.scala` evaluated from
  `NarrativeContextBuilder.buildStrategicPlanExperiments`
- applicable plan family only:
  `ThemeL1.RestrictionProphylaxis` with
  `SubplanId.BreakPrevention` or `SubplanId.KeySquareDenial`
- positive certification only when all of the following hold:
  late middlegame, clearly better eval posture, queen-light late middlegame
  texture, one named break axis plus one independent entry axis, measurable
  counterplay compression on both axes individually, concrete direct
  best-defense evidence, future-snapshot persistence plus bounded continuation
  on the same defended branch, no live freeing axis, no tactical release, no
  fortress-like static hold, and no move-order fragility
- positive claim cap:
  bounded local dual-axis bind only, such as
  `this keeps the ...c5 break shut while b4 stays unavailable`
  or
  `this compresses one break axis and one entry axis at once`
- positive owner cap:
  planner-owned `WhyThis` only;
  existing move-delta `WhatChanged` may survive only when it was already legal
  on its own lane, not as a new B3 owner family
- explicit non-goals remain forbidden:
  whole-position `no-counterplay`, global squeeze, winning-route discovery,
  fortress-break, broad color-complex domination, positive whole-game reuse,
  and Active positive-owner widening

#### Certification contract and fail reasons

- backend-only contract fields now implemented:
  `strategyHypothesis`, `claimScope`, `primaryAxis`, `corroboratingAxes`,
  `axisIndependence`, `bindArchetype`, `restrictionEvidence`,
  `defenderResources`, `freeingResourcesRemaining`,
  `tacticalReleasesRemaining`, `bestDefenseFound`,
  `bestDefenseBranchKey`, `persistenceAfterBestDefense`, `routeContinuity`,
  `fortressRisk`, `moveOrderFragility`, `counterplayReinflationRisk`,
  `claimCertification`, `failsIf`, `confidence`, and `evidenceSources`
- `restrictionEvidence` now records per-axis burden separately so one strong
  axis cannot certify a fake dual-axis shell
- `routeContinuity` now records both `directBestDefensePresent` and
  `sameDefendedBranch` so validation-only or stitched proof bundles fail closed
- current B3 fail-closed reasons are:
  `pv_restatement_only`, `local_to_global_overreach`,
  `waiting_move_disguised_as_bind`, `dual_axis_burden_missing`,
  `axis_independence_not_proven`, `hidden_freeing_break`,
  `hidden_tactical_release`, `cooperative_defense`,
  `direct_best_defense_missing`, `stitched_defended_branch`,
  `route_continuity_missing`, `fortress_like_but_not_winning`,
  `move_order_fragility`, and `surface_reinflation`
- axis independence is intentionally narrow:
  the second axis must be an entry-denial square/file distinct from the primary
  break axis rather than a rephrased copy of the same route
- route continuity is intentionally narrower than plain restriction:
  denial language alone is not enough; the live B3 slice requires bounded follow-through
  evidence rather than treating `entry denied` itself as a conversion route
- fortress protection is explicit:
  a static hold that survives best defense but shows no bounded continuation is
  fail-closed as `fortress_like_but_not_winning`

#### Criticism coverage closed in the live B3 slice

- `axis_independence_not_proven`:
  duplicate break/entry restatements fail instead of counting as two axes
- `hidden_freeing_break`:
  extra live break / entry axes keep the shell uncertified
- `hidden_tactical_release`:
  exchange-sac / perpetual / forcing-release motifs and new tactical threats
  fail the shell
- `move_order_fragility`:
  collapse under best defense or exact-order dependence fails the shell
- `dual_axis_burden_missing`:
  one strong axis plus one decorative axis is not enough; both axes must carry
  measurable restriction burden
- `pv_restatement_only`:
  direct reply PV without route-denial / restraint validation is insufficient
- `direct_best_defense_missing`:
  validation-only corroboration cannot certify a shell without direct
  best-defense evidence
- `stitched_defended_branch`:
  best defense, persistence, and bounded continuation may not be borrowed from
  different probe fragments unless they stay on the same defended branch
- `fortress_like_but_not_winning`:
  static hold without bounded continuation is blocked explicitly
- `local_to_global_overreach`:
  heavy-piece, slightly-better, transition-adjacent, color-complex, and other
  out-of-scope positives stay fail-closed
- `waiting_move_disguised_as_bind`:
  quiet improvement without measurable opponent-facing restriction fails
- `route_continuity_missing`:
  persistence without a bounded follow-through route stays uncertified
- `surface_reinflation`:
  uncertified or overclaiming dual-axis shells are downgraded before planner
  reuse and remain blocked from stronger Bookmaker / Chronicle / Active /
  whole-game ownership

#### Residual cells still outside the live B3 positive scope

- heavy-piece middlegame positives
- transition / endgame-adjacent positives
- pure endgame positives
- slightly better positives
- equal / unclear
- defending side
- color-complex positives
- piece-mobility-cage positives
- quiet king/piece improvement positives
- fortress-like positives
- whole-game / wrap-up / support positive reuse

#### Verification

- `llm/compile`
- targeted runtime and regression suites green:
  `DualAxisBindCertificationTest`,
  `DualAxisBindBroadValidationTest`,
  `RestrictedDefenseConversionCertificationTest`,
  `RestrictedDefenseConversionBroadValidationTest`,
  `CounterplayAxisSuppressionCertificationTest`,
  `CounterplayAxisSuppressionBroadValidationTest`,
  `SurfaceReplayParityTest`,
  `QuestionFirstCommentaryPlannerTest`,
  `BookmakerPolishSlotsTest`, and
  `CrossSurfaceTrustRegressionHarnessTest`

Current operating rule:

- keep the live B1 / B2 / B3 runtime in maintenance-only re-review
- keep B4 in maintenance-only re-review inside its narrow local file-entry
  charter
- broader B4 expansion is still closed except for new design/recon baselines
- do not treat B4 as approval to open heavy-piece positives,
  slightly-better positives, standalone square-network positives, or broader
  whole-position bind claims

#### Reverse review / seam review status

- the live B3 loophole trio is now fix-up green:
  dual-axis burden, direct best-defense evidence, and same-defended-branch
  continuity are all enforced in runtime/tests
- reverse adversarial review is green on the bounded B2 / B1 slices:
  validation-only best-defense shells and stitched proof bundles now fail
  closed instead of certifying
- targeted A/CQF seam review found no new loophole across fallback rewrite,
  truth-gate re-entry, whole-game support reuse, or cross-surface divergence;
  keep runtime maintenance-only watch; the only reopened lane is the separate
  B4 design baseline below

### CTH-B4 — Local Bind Expansion

Purpose:
define the next bounded positive frontier after the live B3 slice, compare the leading
candidates, and set the canonical contract / criticism / broad-validation
burden before any B4 implementation slice is opened.

Status:
design baseline only. No runtime semantics, public payload/schema, prose
family, or new owner lane changed in this step. B4 implementation remains
unopened; the goal is to decide what can be certified next without opening
heavy-piece, slight-edge, or color-complex overclaim.

#### Design posture

- proof / falsification first:
  define what must be measured and what must fail before any new bind language
  is admitted
- broad validation before confidence:
  B4 may not be inferred from a narrow smoke pack
- minor-criticism resistance:
  file-bind, heavy-piece, and slight-edge claims must survive nasty small
  counterexamples rather than rhetorical confidence
- fail-closed over rhetorical richness:
  when proof is incomplete, the result stays `deferred`, support-only, or exact
  factual fallback
- architecture reuse only:
  stay inside
  `NarrativeContextBuilder -> PlanEvidenceEvaluator -> StrategicPlanExperiment -> QuestionFirstCommentaryPlanner -> StrategicNarrativePlanSupport -> DecisiveTruth sanitize -> existing surfaces`
- no new runtime path, payload/schema, prose family, or whole-game owner lane
- surface inflation forbidden:
  uncertified B4 shells must downgrade before Chronicle / Bookmaker / Active /
  whole-game reuse
- texture expansion before phase/eval expansion:
  B4 should widen the certifiable local bind texture before reopening
  heavy-piece phase or slightly-better eval posture

#### Candidate frontier map

| Candidate | Expected positive scope | Main overclaim risk | Main falsification burden | Expected surface risk | Strongest reusable signals | B4 verdict |
| --- | --- | --- | --- | --- | --- | --- |
| `heavy-piece local bind` | one bounded file / entry corridor looks restricted while major pieces remain | queen infiltration, perpetuals, rook lifts, and tactical releases make a local shell look like a whole-position clamp | enumerate forcing releases under best defense, keep same-branch persistence, and block queen-check/perpetual seams | `high` | direct reply probes, `futureSnapshot.newThreatKinds`, `keyMotifs`, branch-key reuse | `deferred`; not the next slice |
| `slightly-better local squeeze` | local restriction language below winning-route level | a comfortable edge gets narrated as a certified squeeze | separate progress from fortress/static hold, cap language, and fail closed on small releases | `high` | current eval gating, fortress-risk reuse, route continuity, same-branch persistence | `deferred`; positive B4 not recommended |
| `file bind` | one denied file corridor plus one single corroborating entry square on the same defended branch | file occupancy or pressure is mistaken for denied file access | prove opponent-facing file usability loss, not just our piece placement; enumerate off-file releases | `medium` | builder-level `PreventedPlan.deniedEntryScope` / `deniedSquares` / `deniedResourceClass` / `counterplayScoreDrop`, reply probes, `futureSnapshot`, branch-key reuse | `deferred until carrier/surface gap closure`; still the preferred B4 entry slice |
| `square-network bind` | a small named route network, not generic square rhetoric | isolated squares are overcounted as a network | prove route continuity and non-redundant square chaining | `medium-high` | `deniedSquares`, key-square denial, `futureSnapshot.planBlockersRemoved`, B3 axis-independence grammar | `deferred`; not part of the first positive slice beyond a single corroborating entry square |
| `bounded color-complex restriction` | a color-complex idea tied to a named route loss | pretty positional prose outruns certifiable local truth | tie every color-complex claim to concrete entry/break loss plus same-branch persistence | `very high` | `ColorComplexSqueezeValidation`, weak-complex / denied-square signals, `futureSnapshot` | `unsafe` as the next positive slice |

Operational conclusion:

- B4 should widen texture, not phase or eval posture
- the preferred next B4 target remains a bounded local `file bind` with one
  single corroborating entry square, but it is not yet surface-ready on the
  current carrier path
- heavy-piece positives and slightly-better positives remain deferred
- bounded color-complex positives remain unsafe

#### Reusable signal inventory

Reusable now:

- `PreventedPlan.breakNeutralized`, `deniedSquares`, `deniedResourceClass`,
  `deniedEntryScope`, `counterplayScoreDrop`,
  `breakNeutralizationStrength`, and `defensiveSufficiency`
- direct reply probes:
  `defense_reply_multipv`, `reply_multipv`, and `convert_reply_multipv`
- `futureSnapshot.planBlockersRemoved`, `planPrereqsMet`,
  `resolvedThreatKinds`, and `newThreatKinds`
- `PlanEvidenceEvaluator.ClaimCertification`:
  distinctive attribution, ontology family, stability, and
  `alternativeDominance = false`
- existing branch-key / same-defended-branch / move-order-fragility grammar
- existing `counterplayReinflationRisk` fail-closed pattern

Reusable only as support, not as proof:

- `OpenFilePressure` or other theme labels by themselves
- generic `targetsDelta` or `c-file pressure` wording
- generic square-control, mobility-clamp, or color-complex prose
- support-only carriers that are not tied to direct best-defense proof

Current builder / carrier gap:

- the backend model already carries
  `deniedEntryScope = single_square | file | sector`, and selector logic
  already treats `file` as counterplay suppression
- `NarrativeContextBuilder.convertPreventedPlan` currently drops
  `deniedEntryScope` and `deniedResourceClass` when `PreventedPlan` is lowered
  into `PreventedPlanInfo`
- builder-level contract reuse is therefore plausible, but the current
  player-facing carrier does not yet preserve explicit file scope
- a future B4 slice may close that gap only inside the existing
  `PreventedPlan` / probe / planner path; it may not add payload/schema

Current surface phrasing gap:

- current player-facing phrasing still prefers `breakNeutralized` or the first
  `deniedSquare`
- `QuietMoveIntentBuilder` and `MainPathMoveDeltaClaimBuilder` do not yet have
  a surface-safe phrase path for `file usability loss`
- because of that gap, positive B4 file-bind readiness is still deferred on
  planner-owned `WhyThis`, Bookmaker, and Chronicle even though the upstream
  builder/runtime path is the preferred reuse lane

#### Backend-only contract drafts

Recommended contract draft:

`localFileEntryBind`

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `claimScope` | keep the claim local to one file corridor plus one single corroborating entry square | existing planner ownership limits plus backend-only scope gating |
| `axisFamilies` | record `file_access + single_entry_square` instead of generic `bind` wording | `PreventedPlan.deniedEntryScope`, `deniedResourceClass`, denied squares |
| `axisIndependence` | prove the entry square is not just a renamed copy of the file claim | B3 axis-independence grammar, denied-square / file token comparison |
| `fileUsabilityEvidence` | show the opponent lost usable file access rather than only seeing our rook pressure | builder-level `deniedEntryScope`, denied entry squares, `counterplayScoreDrop`, `defensiveSufficiency`, reply probes |
| `singleCorroboratingEntryAxis` | keep the first slice at one corroborating entry square, not a standalone square network | denied-square extraction plus same-branch validation |
| `entryAxisPersistence` | show that one corroborating entry square stays unavailable on the defended branch | `futureSnapshot.planBlockersRemoved`, `planPrereqsMet`, branch-key reuse |
| `pressurePersistence` | keep the claim tied to best-defense stability plus bounded persistence | direct reply probes, `bestReplyStable`, same-branch validation |
| `bestDefenseBranchKey` | force file proof, release checks, and continuity to stay on one defended branch | existing branch-key matching from B1/B3 |
| `routeContinuity` | keep the bind tied to a bounded follow-through route, not global winning-route language | `futureSnapshot`, `convert_reply_multipv`, continuity motifs |
| `releaseRisksRemaining` | enumerate live freeing breaks, file switches, or tactical releases that still puncture the shell | remaining prevented-plan axes, `newThreatKinds`, `collapseReason`, motifs |
| `fileOccupancyOnlyRisk` | fail if the file claim is really just our piece placement | denied-file proof vs raw pressure comparison |
| `fortressRisk` | block static hold from becoming a bind certificate | B3 fortress-risk pattern, lack of bounded continuation |
| `slightEdgeOverclaimRisk` | block slightly-better positions from inheriting B4 positivity too early | eval posture gate plus bounded-language cap |
| `counterplayReinflationRisk` | flag replay/support carriers likely to overstate the shell | existing replay gate and support-only discipline |
| `claimCertification` | keep B4 inside the same quantifier/modality/attribution grammar as B1/B2/B3 | `PlanEvidenceEvaluator.ClaimCertification` |
| `confidence` | derive confidence from the same bounded file/square evidence | existing confidence-score pattern |
| `evidenceSources` | show which carriers justified the contract | current hypothesis/probe/prevented-plan source lists |

Deferred contract draft:

`heavyPieceBindShell`

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `claimScope` | keep the claim local to one bounded sector under heavy-piece tension | backend-only scope gating |
| `axisFamilies` | record the local resource axes without implying global domination | prevented-plan axes plus route-denial aliases |
| `pressurePersistence` | tie the shell to direct best-defense handling | reply probes, `bestReplyStable`, same-branch persistence |
| `bestDefenseBranchKey` | prevent stitched heavy-piece proof bundles | existing branch-key reuse |
| `releaseRisksRemaining` | enumerate still-live breaks, infiltrations, and tactical releases | `newThreatKinds`, motifs, collapse reasons, prevented-plan residues |
| `heavyPieceReleaseRisks` | separate queen-check, rook-lift, and forcing-chain seams from ordinary route denial | direct reply probes plus tactical-release parsing |
| `perpetualRisk` | keep perpetual-like escapes from being narrated as a bind | reply probes, forcing motifs, future snapshots |
| `routeContinuity` | require bounded continuation on that same defended branch | B3 route-continuity grammar |
| `counterplayReinflationRisk` | stop a noisy heavy-piece shell from inflating on stronger surfaces | existing replay gate |
| `confidence` / `evidenceSources` | make any future heavy-piece certification auditable | existing contract pattern |

Deferred contract draft:

`slightEdgeRestriction`

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `claimScope` | keep the claim explicitly local and non-winning | backend-only scope gating |
| `axisFamilies` | record the bounded restriction without global squeeze rhetoric | prevented-plan axes, route-denial aliases |
| `pressurePersistence` | verify the local shell survives best defense | reply probes, best-reply stability |
| `bestDefenseBranchKey` | keep proof on one defended branch | existing branch-key reuse |
| `routeContinuity` | separate real progress from mere static comfort | `futureSnapshot`, continuity motifs, conversion-tail reuse |
| `progressVisibility` | require a visible worsening of defender resources before any positive language survives | future snapshots, blocker-removal / target-delta evidence |
| `fortressRisk` | block static equality-like holds from sounding winning | B3 fortress-risk pattern |
| `slightEdgeOverclaimRisk` | make low-margin inflation explicit and fail-closed | eval gating plus language-cap policy |
| `releaseRisksRemaining` | enumerate the small defensive resources that still keep equality-like resistance alive | reply probes, tactical-release parsing, remaining axes |
| `counterplayReinflationRisk` | stop support carriers from turning a mild edge into a squeeze | existing replay gate |
| `confidence` / `evidenceSources` | keep any future slight-edge lane auditable and capped | existing contract pattern |

Rejected for positive drafting now:

- do not open a positive `boundedColorComplexRestriction` contract yet
- keep color-complex work negative-only until the claim can point to a named
  entry / break / file loss and survive same-branch release testing

#### Criticism matrix

| Axis | Why risky | Current reusable guard | Required future B4 guard | Required difficult fixture shape |
| --- | --- | --- | --- | --- |
| `cooperative_defense` | the shell survives only if the defender stays passive | direct reply probes, best-reply stability, alternative-dominance checks | concrete best defense plus same-branch persistence for every positive B4 claim | file grip that works unless the defender immediately contests the file or switches sector |
| `hidden_freeing_break` | one unclosed break or route kills the bind thesis | prevented-plan enumeration, blocker checks, future snapshots | `releaseRisksRemaining` must list every live break / file switch / sector release and fail if any survive | c-file looks shut, but `...a5` or `...e5` still frees the game |
| `hidden_tactical_release` | exchange sac, perpetual, forcing checks, or rook lift puncture the shell | `collapseReason`, `keyMotifs`, `newThreatKinds`, reply probes | explicit tactical-release inventory, especially for heavy-piece lanes | file clamp fails once the defender finds an exchange sac or check chain |
| `engine_pv_paraphrase` | an engine line is rewritten as `bind` without local resource proof | claim-certification attribution/ontology checks, validation purposes | require opponent-facing resource enumeration and route loss, not raw PV pressure | doubling on a file is described as a bind even though no entry route is actually denied |
| `move_order_fragility` | the idea works only in one exact move order | existing `moveOrderSensitive`, collapse reasons, B1/B2/B3 fragility handling | contract-level `moveOrderFragility` must fail exact-order-only shells | same file bind works only if the defender chooses the wrong rook-trade order |
| `false_file_bind` | occupying or pressuring a file is mistaken for denying that file | `deniedEntryScope`, denied squares, counterplay-drop signals | add `fileUsabilityEvidence` and `fileOccupancyOnlyRisk`; require entry denial plus persistence | rooks occupy the c-file, but c2/c7 or a side break still keeps the file usable |
| `false_square_network` | separate squares are overcounted as a meaningful network | denied-square lists, B3 axis-independence grammar | keep standalone square-network bind deferred; first B4 slice allows only one corroborating entry square | c5 and d6 look controlled, but the defender reroutes through e5/f4 or changes color complex |
| `fortress_like_but_not_progressing` | a static hold is confused with a certifiable bind | B3 fortress-risk and route-continuity checks | require `progressVisibility` or bounded continuation; otherwise fail closed | locked file and denied square with no breakthrough, no worsening defender state, and no conversion tail |
| `slight_edge_overclaim` | a mild edge is narrated as a squeeze | B2/B3 clearly-better gate | explicit `slightEdgeOverclaimRisk`, bounded language cap, and continued defer of slightly-better positives | `+80` to `+120` position with local control but no real progress route |
| `cross_branch_stitching` | best defense, persistence, and continuation are borrowed from different probe branches | existing branch-key / same-defended-branch logic | every B4 proof component must key to the same defended branch | best defense comes from branch A, file denial from branch B, and continuation from branch C |
| `surface_reinflation` | support carriers revive a stronger claim on Bookmaker / Chronicle / Active / wrap-up | `StrategicNarrativePlanSupport`, replay parity tests, cross-surface trust harness | downgrade before planner reuse and keep whole-game/support reuse blocked | uncertified local file shell reappears as `no counterplay` or `squeeze` on a stronger surface |

#### Broad validation charter

The B4 validation burden must be read as a texture-expansion charter, not as
permission to widen phase, eval posture, and surface all at once:

- positive certification may target only cells marked `likely coverable now`
- any `likely deferred` cell needs its own corpus expansion and criticism pack
  before positive certification is accepted
- any `likely unsafe` cell is negative-only for early B4 work and must prove
  fail-closed behavior rather than positive admission

Phase matrix:

| Phase cell | Status | Why |
| --- | --- | --- |
| `late middlegame` | `likely coverable now` | existing prevented-plan, reply-probe, and future-snapshot signals are strongest here |
| `heavy-piece middlegame` | `likely deferred` | forcing-release density is higher and file/square shells are easier to overread |
| `transition / endgame-adjacent` | `likely deferred` | file grip vs fortress/progress ambiguity is harder to separate |
| `pure endgame` | `likely unsafe` | active-king routes and fortress false positives dominate |

Evaluation posture matrix:

| Eval posture | Status | Why |
| --- | --- | --- |
| `clearly better` | `likely coverable now` | strongest place to admit a bounded local bind without implying a whole winning route |
| `slightly better` | `likely deferred` | local restriction may be real, but progress vs fortress remains too fragile |
| `equal / unclear` | `likely unsafe` | positive bind certification would overstate local truth |
| `defending side` | `likely unsafe` | would turn hold/survival language into a false squeeze owner lane |

Texture matrix:

| Texture | Status | Why |
| --- | --- | --- |
| `break + entry shell` | `likely coverable now` | already certified by B3 and should remain the B4 regression control cell |
| `file bind` | `likely deferred` | still the preferred first expansion, but current carrier/surface gaps must close before positive certification |
| `square-network bind` | `likely deferred` | route continuity and mirage risk still need their own criticism pack |
| `heavy-piece clamp` | `likely deferred` | tactical-release density and perpetual risk are still too high |
| `slight-edge local squeeze` | `likely unsafe` | rhetorical inflation and progress ambiguity remain too large |
| `static hold / fortress near-miss` | `likely unsafe` | mandatory negative corpus, not early positive scope |
| `tactical-release fake shell` | `likely unsafe` | mandatory negative corpus, not early positive scope |
| `waiting-move-only pseudo bind` | `likely unsafe` | too easy to confuse with generic improvement or style choice |

Surface matrix:

| Surface | Status | Why |
| --- | --- | --- |
| `planner-owned WhyThis` | `likely deferred` | strongest eventual owner path, but current carrier and phrasing paths do not yet preserve file usability loss cleanly |
| `planner-owned WhatChanged` | `likely deferred` | keep the move-delta lane independent; do not widen B4 positivity here first |
| `Bookmaker` | `likely deferred` | planner-first replay is reusable, but current phrasing still prefers break or first-square language over file usability loss |
| `Chronicle` | `likely deferred` | shared replay path is reusable, but current phrasing still prefers break or first-square language over file usability loss |
| `Active` | `likely deferred` | parity/non-reinflation can be tested now, but positive ownership remains diagnostic-sensitive |
| `whole-game / wrap-up / support reuse` | `likely unsafe` | early B4 work should validate only non-reinflation, not positive wrapper reuse |

#### Mandatory self-critique pass

Candidate attacked:

- the first draft treated `file / square bind` as one unified next-step
  frontier

Nasty cases used to attack that draft:

| Self-critique case | Why the first draft was vulnerable | B4 correction |
| --- | --- | --- |
| `occupied_file_not_denied` | the draft could confuse our heavy-piece occupation with the opponent actually losing file access | add `fileUsabilityEvidence` and `fileOccupancyOnlyRisk`; file bind now requires opponent-facing denial, not our pressure alone |
| `off_file_break_release` | the draft focused on the file itself and could miss a freeing break elsewhere | add `releaseRisksRemaining` and expand `hidden_freeing_break` fixtures to include off-file releases |
| `square_network_mirage` | the draft could count two pretty squares as a route network | split `file bind` from `standalone square-network bind`; keep the latter deferred and cap the first slice at one corroborating entry square |
| `heavy_piece_perpetual_shell` | the draft was too willing to imagine the same contract under queens/major pieces | keep heavy-piece positives deferred and require a dedicated forcing-release corpus before reopening them |
| `slight_edge_comfort_shell` | the draft could have admitted a low-margin restriction as a squeeze because the local shell looked real | add `slightEdgeOverclaimRisk` and keep slightly-better positives deferred |
| `fortress_file_grip` | the draft could call a static hold a bind without visible progress | strengthen `fortressRisk` and keep `routeContinuity` / `progressVisibility` mandatory |
| `stitched_branch_bundle` | the draft could still borrow best defense, file proof, and continuation from different branches | keep `bestDefenseBranchKey` / same-defended-branch proof mandatory for every positive B4 claim |

Design changes forced by self-critique:

- split candidate C into:
  `file bind` vs `standalone square-network bind`
- move `file bind` from `likely coverable now` to `likely deferred` until the
  carrier/surface gap closes
- add `fileUsabilityEvidence`, `singleCorroboratingEntryAxis`,
  `entryAxisPersistence`, `fileOccupancyOnlyRisk`, `releaseRisksRemaining`,
  and `slightEdgeOverclaimRisk` to the contract drafts
- keep heavy-piece positives and slightly-better positives outside the first B4
  implementation slice
- keep color-complex positives explicitly unsafe rather than merely deferred

#### Recommended first implementation slice

Recommended first slice:

- `late-middlegame clearly-better file-bind with one single corroborating entry-square axis, after carrier/surface gap closure`
  this is the recommendation that the live B4 slice below now consumes

Narrow scope:

- applicable owner lane:
  stay inside the existing `RestrictionProphylaxis` planner/runtime path
- applicable plan family:
  prefer `BreakPrevention` / `KeySquareDenial` cases where the opponent's file
  access can be represented as `deniedEntryScope = file` or equivalent bounded
  route-denial evidence inside the current carrier
- positive output cap:
  planner-owned `WhyThis` first after the carrier/surface gap closes;
  Bookmaker / Chronicle may mirror the upstream certificate only after the same
  phrasing gap is closed, while `WhatChanged`, Active, and whole-game reuse
  remain non-inflation-only
- explicit non-goals:
  heavy-piece positive certification, slightly-better positive squeeze,
  standalone square-network positives, color-complex positives, whole-position
  `no-counterplay`, and winning-route discovery

Why this slice should go first:

- it expands texture while keeping phase and eval posture closed
- it reuses the existing B3 branch-key / continuation / reinflation grammar
- it has the smallest new falsification burden:
  file usability vs occupancy, off-file release enumeration, and one bounded
  corroborating entry-square axis
- it avoids the two most dangerous temptation seams:
  heavy-piece tactical illusion and slight-edge rhetorical inflation

#### Live runtime slice — late-middlegame local file-entry bind certification

Status:

- implemented
- `implementation-green`
- known B4 fix-up findings closed
- `adversarial-review green`
- `current bounded scope complete`

Canonical runtime scope:

- `RestrictionProphylaxis` only
- `BreakPrevention` / `KeySquareDenial` only
- late middlegame only
- clearly-better posture only
- queen-light only as an extra fail-closed guard against heavy-piece release
- one denied file corridor plus one independent corroborating entry square only

Carrier / surface gap closure actually taken:

- `StrategicAnalyzers.ProphylaxisAnalyzerImpl` may now preserve
  `deniedEntryScope = file` for bounded break-prevention cases when the denied
  break file is concrete
- `NarrativeContextBuilder.convertPreventedPlan` now preserves
  `deniedResourceClass` and `deniedEntryScope` inside `PreventedPlanInfo`, so
  the existing carrier can express file usability loss without a new public
  API/schema field
- surface reuse remains fail-closed:
  `MainPathMoveDeltaClaimBuilder`, `QuietMoveIntentBuilder`, and
  `QuestionFirstCommentaryPlanner` may use the bounded file-entry wording only
  when `StrategicNarrativePlanSupport.evidenceBackedMainPlans` still confirms
  an evidence-backed restriction plan and that plan text itself names the same
  file plus entry-square pair

Positive scope now open:

- bounded planner-owned `WhyThis`
- bounded move-linked `WhatChanged`
- Bookmaker / Chronicle parity only through the same planner/builder gate

Still deferred or negative-only:

- `WhyNow`
- Active positive ownership expansion
- whole-game / wrap-up / support positive reuse
- heavy-piece middlegame positives
- slightly-better positives
- standalone square-network positives
- bounded color-complex positives

Fail-closed burdens enforced in the live slice:

- `file_occupancy_only`
- `hidden_off_file_release`
- `entry_axis_not_independent`
- `entry_axis_persistence_missing`
- `direct_best_defense_missing`
- `stitched_defended_branch`
- `hidden_tactical_release`
- `move_order_fragility`
- `fortress_like_but_not_progressing`
- `slight_edge_overclaim`
- `surface_reinflation`

Adversarial-review correction made during implementation:

- the first B4 runtime draft left a real reinflation seam:
  `LocalFileEntryBindCertification.certifiedSurfacePair(ctx)` was reading raw
  `ctx.mainStrategicPlans`, so a deferred experiment with residual main-plan
  text could still seed file-entry phrasing in builder-owned surfaces
- the shipped slice fixes that by using
  `StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx)` and by
  requiring the surviving evidence-backed restriction plan's affirmative text
  to name the same file plus entry-square pair before any surface claim is
  built
- broad-validation now includes the explicit nasty-case fixture
  `deferred experiment plus residual main plan cannot stitch a file-entry claim
  back into builder or planner surfaces`

B4 fix-up hardening closed in this step:

- `entryAxisPersistence` is now a legality burden, not just a confidence input:
  the corroborating entry square must stay unavailable on the same defended
  branch, and `entry_axis_persistence_missing` now hard-fails the certificate
- same-defended-branch identity is now replayable-first:
  `bestDefenseBranchKey` must resolve from `variationHash`, `seedId`, or one
  exact two-ply reply-line key; same-first-move divergent continuations and
  short candidate/probed fallbacks no longer count as same-branch proof, and
  ambiguous direct-reply branch sets no longer leak a selected defended-branch
  key downstream
- affirmative-only surface matching is now explicit:
  the file-entry pair may be recovered only from affirmative plan text
  (`planName`, `executionSteps`); negative/caveat text such as
  `failureModes` or `refutation` may not unlock a positive surface pair
- new negative fixtures now cover the remaining narrow-slice seams:
  `entry_axis_persistence_missing` and
  `pair_only_in_failure_mode_or_refutation`, plus the same-first-move
  divergent defended-branch near-miss
- result:
  the B4 slice moves from `implementation-green` only to
  `adversarial-review green` and `current bounded scope complete` inside its
  narrow charter only; that means maintenance-only watch for the bounded local
  file-entry slice, not broader B4 family completion

Next move:

- `B4 maintenance-only watch`; any broader B4 texture / posture / owner-surface
  expansion still needs a new bounded charter

Verification burden closed in the same session:

- targeted compile: `llm/compile`
- B4 broad validation:
  `LocalFileEntryBindBroadValidationTest`
- regression packs:
  `DualAxisBindBroadValidationTest`,
  `CounterplayAxisSuppressionBroadValidationTest`,
  `SurfaceReplayParityTest`,
  `BookmakerPolishSlotsTest`,
  `CrossSurfaceTrustRegressionHarnessTest`,
  `ActiveStrategicCoachingBriefBuilderTest`

Session verdict:

- `CTH-B4 design baseline ready`
- reason:
  B4 now has a canonical candidate map, backend-only contract drafts,
  criticism matrix, broad validation charter, and self-critique-revised next
  slice without changing runtime semantics

### CTH-B5a — Next Local Strategic Frontier Design

Purpose:
choose which deferred harder local strategic family, if any, can become the
next bounded certification target after the live B4 slice, and set the
canonical contract / criticism / broad-validation burden before any B5
implementation opens.

Status:
design baseline only. No runtime semantics, public payload/schema, prose
family, owner lane, or new test harness changed in this step. B5 positive
rollout remains unopened.

#### Design posture

- proof / falsification first:
  a B5 candidate must earn a measurable local truth claim, not just a
  convincing strategic sentence
- broad validation before confidence:
  one attractive corpus shard is not enough for heavy-piece, transition, or
  route-network work
- minor-criticism resistance:
  the candidate must survive nasty but realistic small objections
- fail-closed over rhetorical richness:
  incomplete proof stays `deferred`, support-only, or exact factual fallback
- architecture reuse only:
  stay inside
  `NarrativeContextBuilder -> PlanEvidenceEvaluator -> StrategicPlanExperiment -> QuestionFirstCommentaryPlanner -> StrategicNarrativePlanSupport -> DecisiveTruth sanitize -> existing surfaces`
- no new runtime path, payload/schema, prose family, or whole-game owner lane
- surface reinflation forbidden:
  uncertified B5 shells must downgrade before Chronicle / Bookmaker / Active /
  whole-game reuse
- local bounded claim before whole-position ambition:
  B5 is not a permission slip for `no counterplay`, global squeeze, or
  winning-route discovery
- self-critique during design, not afterthought:
  the nastiest cases below are mandatory charter inputs, not optional examples

#### Candidate frontier map

| Candidate | Expected positive scope | Main overclaim risk | Main falsification burden | Expected surface risk | Strongest reusable signals | B5 verdict |
| --- | --- | --- | --- | --- | --- | --- |
| `heavy-piece local bind` | one bounded file / entry corridor remains restricted with major pieces still on the board | queen infiltration, rook lifts, forcing checks, and perpetuals make a local shell look like a whole-position clamp | enumerate heavy-piece release inventory under concrete best defense, keep same-branch persistence, and prove the shell survives without stitched branches | `very high`; Bookmaker / Chronicle / Active can sound much stronger than the local truth | B4 file-entry contract shape, direct reply probes, `bestDefenseBranchKey`, `futureSnapshot.newThreatKinds`, `keyMotifs`, `deniedEntryScope`, `deniedSquares`, claim certification | `deferred` for positive rollout, but it is the most reusable next recon family and the recommended negative-first B5b lane |
| `slightly-better local squeeze` | local restriction language below winning-route level in a small edge posture | a comfortable edge is narrated as a certified squeeze even when no progress route exists | separate progress from fortress/static hold, require visible defender-resource worsening, and hard-fail small surviving releases | `very high`; every surface wants to inflate mild-edge control into squeeze rhetoric | best-defense branch reuse, bounded continuation, fortress-risk patterns, `futureSnapshot`, claim certification | `unsafe` as the first positive B5 slice; keep negative-only until progress proof exists |
| `named route-network bind` | a small named route network, not generic square-control rhetoric | isolated squares or one-file pressure are overcounted as a meaningful network | prove route continuity, non-redundant square chaining, and no reroute through an untouched sector or color complex | `high`; square naming is especially vulnerable to pretty positional prose | `deniedSquares`, route-denial validation, B3/B4 axis-independence grammar, same-branch persistence, `futureSnapshot.planBlockersRemoved` | `deferred`; next positive frontier candidate after B5 |
| `transition-adjacent route` | one bounded move-local route into a better technical task near a middlegame -> endgame handoff | a local route becomes a whole-ending verdict, fortress verdict, or king-race thesis | prove same-branch task shift, route continuity, and fortress/king-race exclusion under best defense | `high`; `WhatChanged`, Chronicle, and wrap-up can overstate it into the ending's full story | B1 restricted-defense route persistence, `convert_reply_multipv`, same-branch keys, `futureSnapshot`, `endgameTransitionClaim`, endgame features as support only | `deferred`; second-line recon family, not the first B5 implementation slice |
| `bounded color-complex / mobility-cage` | a color-complex idea tied to one named route loss or one bounded mobility cage | dark-square / light-square rhetoric outruns certifiable local truth and generic mobility-cage prose inflates too easily | tie every abstract claim to concrete entry/break/file loss on the same defended branch and block rhetoric drift when that route proof is missing | `very high`; lexicon and support carriers already make this sound stronger than it is | `ColorComplexSqueezeValidation`, `WeakComplex`, denied squares, `futureSnapshot` | `unsafe`; last and most optional frontier family |
| `none / keep positive frontier closed` | no new positive charter until one family survives negative-first criticism | reuse bias makes a familiar local shell feel safer than it is | prove that the leading candidate survives the nasty-case pack before any positive wording is approved | `lowest` immediate surface risk, but only if the frontier stays explicitly closed | existing B2 / B3 / B4 fail-closed gates, cross-surface harness, evidence-backed plan filter | `recommended current baseline`: no B5 positive slice opens yet |

Operational conclusion:

- B5 opens no new positive local strategic family today
- `B5` remains a heavy-piece negative-first containment lane only
- the confirmed post-B5 frontier map is:
  `B6 = named route-network bind`
- then:
  `B7 = transition-adjacent strategic route`
- then only after fortress/progress separation materially improves:
  `B8 = slight-edge local squeeze`
- last and still optional / unsafe:
  `B9 = bounded color-complex / mobility-cage`

#### Reusable signal inventory

Reusable now:

- direct reply probes:
  `reply_multipv`, `defense_reply_multipv`, and `convert_reply_multipv`
- same-defended-branch grammar:
  `bestDefenseBranchKey`, branch-key reuse, same-branch persistence, and
  existing stitched-proof failure reasons
- measured local restriction signals:
  `PreventedPlan.deniedSquares`, `deniedResourceClass`, `deniedEntryScope`,
  `counterplayScoreDrop`, `breakNeutralizationStrength`, and
  `defensiveSufficiency`
- bounded continuation and release signals:
  `futureSnapshot.planPrereqsMet`, `planBlockersRemoved`,
  `resolvedThreatKinds`, `newThreatKinds`, and `targetsDelta`
- claim-certification grammar:
  quantifier, modality, attribution, stability, ontology family, and
  `alternativeDominance`
- existing fortress/static-hold suspicion:
  B3 / B4 `fortressRisk`, bounded-continuation checks, and static-hold token
  handling
- transition support-only signals:
  `endgameTransitionClaim`, `EndgameFeature` patterns, and existing
  `AdvantageTransformation` route-persistence grammar

Reusable only as support, not as proof:

- generic `OpenFilePressure`, square-control, or mobility-cage wording
- plain eval edge without measurable local resource loss
- `WeakComplex` / color-complex language without a named route-loss anchor
- surface persistence by itself:
  Bookmaker / Chronicle / Active survival does not certify the route
- whole-game / wrap-up support reuse:
  it is a reinflation seam, not proof

#### Backend-only contract drafts

Selected next recon-family draft:

`heavyPieceLocalBind`

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `claimScope` | keep the claim local to one file / entry corridor under heavy-piece tension | existing backend-only scope gating plus B4 file-entry limits |
| `axisFamilies` | record `file_access + corroborating_entry_square` without implying whole-position domination | `deniedEntryScope`, `deniedResourceClass`, `deniedSquares` |
| `axisIndependence` | prove the entry square is not just a renamed copy of the file claim | B3 / B4 axis-independence grammar |
| `bestDefenseBranchKey` | keep every proof component on one defended branch | existing branch-key reuse |
| `pressurePersistence` | require the shell to survive concrete best defense | direct reply probes, `bestReplyStable`, same-branch persistence |
| `routeContinuity` | show bounded continuation still exists on that same branch | `futureSnapshot`, `convert_reply_multipv`, continuation motifs |
| `heavyPieceReleaseInventory` | enumerate forcing checks, queen infiltrations, rook lifts, exchange sacs, and perpetual seams that remain live | `newThreatKinds`, `keyMotifs`, `collapseReason`, direct reply probes |
| `perpetualRisk` | keep perpetual-like resources from being narrated as a bind | reply probes, forcing motifs, future snapshots |
| `tacticalReleaseDensity` | distinguish a locally stable shell from a tactically noisy one | release inventory plus reply branching counts |
| `bestDefenseReleaseSurvivors` | record which release attempts still survive best defense even if the local file shell looks strong | pinned `bestReplyPv` plus same-branch validation best-reply bundle |
| `counterplayReinflationRisk` | stop a noisy heavy-piece shell from inflating on stronger surfaces | existing replay gate and evidence-backed-plan filter |
| `claimCertification` / `confidence` / `evidenceSources` | keep any future heavy-piece certification auditable inside the existing grammar | `PlanEvidenceEvaluator.ClaimCertification` plus current source lists |

Deferred draft:

`routeNetworkBind`

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `claimScope` | keep the claim local to one named route chain, not generic square rhetoric | backend-only scope gating |
| `axisFamilies` | record which entry squares or route nodes are claimed to matter | `deniedSquares`, `deniedResourceClass` |
| `bestDefenseBranchKey` | keep route proof on one defended branch | existing branch-key reuse |
| `pressurePersistence` | require the route shell to survive best defense | reply probes, same-branch persistence |
| `routeContinuity` | prove the chain still points to one bounded follow-through route | `futureSnapshot.planPrereqsMet`, `planBlockersRemoved`, continuation motifs |
| `routeNodes` | enumerate the exact route nodes instead of letting prose invent them later | denied-square extraction |
| `networkIndependence` | prevent one square or one file from being relabeled as a network | B3 / B4 independence grammar |
| `rerouteResourcesRemaining` | fail when a nearby reroute keeps the route alive | denied-square comparison plus future-snapshot blockers |
| `routeNetworkMirageRisk` | mark when the chain is too aesthetic and not sufficiently opponent-facing | route-denial validation plus support-only discipline |
| `counterplayReinflationRisk` / `confidence` / `evidenceSources` | keep any future network work auditable and fail-closed | existing contract pattern |

Deferred draft:

`transitionAdjacentRoute`

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `claimScope` | keep the claim local to a transition-adjacent route, not the whole ending verdict | backend-only scope gating |
| `phaseEnvelope` | freeze the charter to middlegame -> endgame-adjacent handoff cells | existing phase gating plus `endgameTransitionClaim` |
| `bestDefenseBranchKey` | keep task-shift proof on one defended branch | B1 branch-key reuse |
| `pressurePersistence` | require the route to survive concrete best defense | direct reply probes, `bestReplyStable` |
| `routeContinuity` | show that the technical task shift still points to a bounded route | `convert_reply_multipv`, `futureSnapshot`, continuation motifs |
| `endgameTaskShift` | record the specific task change being claimed | existing transition translator plus B1 route evidence |
| `fortressRisk` | block fortress-like or static-hold readings | existing fortress heuristics plus endgame support signals |
| `kingRaceAmbiguity` | block race-style or king-activity ambiguity from sounding like a certifiable route | `EndgameFeature.kingActivityDelta`, motifs, best-defense replies |
| `tacticalReleaseDensity` | keep transition claims from hiding tactical liquidations | release inventory over reply probes |
| `counterplayReinflationRisk` / `confidence` / `evidenceSources` | keep a future transition slice auditable and bounded | existing contract pattern |

Deferred draft:

`slightEdgeLocalSqueeze`

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `claimScope` | keep the claim explicitly local and non-winning | backend-only scope gating |
| `axisFamilies` | record the bounded restriction without global squeeze rhetoric | prevented-plan axes, route-denial aliases |
| `bestDefenseBranchKey` | keep proof on one defended branch | existing branch-key reuse |
| `pressurePersistence` | verify the shell survives best defense | reply probes, best-reply stability |
| `routeContinuity` | separate real progress from static comfort | `futureSnapshot`, continuation motifs |
| `progressVisibility` | require visible defender-resource worsening before any positive wording survives | future snapshots, blocker-removal / target-delta evidence |
| `fortressRisk` | stop static equality-like holds from sounding winning | B3 / B4 fortress-risk patterns |
| `slightEdgeOverclaimRisk` | make low-margin inflation explicit and fail-closed | eval gating plus bounded-language cap |
| `tacticalReleaseDensity` | record the small tactical resources that still keep the edge non-certifiable | reply probes and release inventory |
| `counterplayReinflationRisk` / `confidence` / `evidenceSources` | keep any future slight-edge work auditable and capped | existing contract pattern |

Rejected for positive drafting now:

- do not open a positive `boundedColorComplexRestriction` contract yet
- keep color-complex work negative-only until the claim can point to one named
  entry / break / file loss and survive same-branch release testing

#### Criticism matrix

| Axis | Why risky | Current reusable guard | Required future B5 guard | Required difficult fixture shape |
| --- | --- | --- | --- | --- |
| `cooperative_defense` | the line works only if the defender stays passive | direct reply probes, best-reply stability, resource caps | every B5 contract must survive one concrete best defense on the same branch | local bind survives unless the defender immediately contests the file or activates the queen |
| `hidden_freeing_break` | one surviving break or reroute kills the strategic shell | prevented-plan enumeration, alternative-axis tracking, future snapshots | explicit `releaseRisksRemaining` / `rerouteResourcesRemaining` inventory for each candidate | the claimed bind on one wing collapses once `...a5` or `...e5` reopens play elsewhere |
| `hidden_tactical_release` | exchange sac, perpetual, forcing checks, or rook lift puncture the shell | `collapseReason`, `newThreatKinds`, `keyMotifs` | candidate-specific tactical-release inventory and density gate | a file clamp fails once the defender finds an exchange sac or forcing-check chain |
| `engine_pv_paraphrase` | a PV line is restated as a strategic thesis without opponent-facing proof | claim-certification quantifier / attribution / ontology checks | require resource-loss enumeration plus bounded route loss, not just line pressure | doubling rooks or a king move is narrated as a bind even though no route was actually denied |
| `move_order_fragility` | the shell works only in one exact move order | `moveOrderSensitive`, collapse reasons, B1 / B3 / B4 fragility handling | contract-level hard fail for exact-order-only routes | the route works only if the defender chooses the wrong rook-trade or queen-check order |
| `route_network_mirage` | a pretty square picture masquerades as a real network | denied-square lists and B3 / B4 independence grammar | `routeNodes`, `networkIndependence`, and `rerouteResourcesRemaining` must all pass | c5 and d6 look controlled, but the defender reroutes through e5 / f4 or flips the color complex |
| `heavy_piece_release_illusion` | local restriction language survives in prose while forcing heavy-piece resources stay live | B4 queen-light fail-closed guard, tactical-release parsing, `newThreatKinds` | `heavyPieceReleaseInventory`, `perpetualRisk`, and `bestDefenseReleaseSurvivors` are mandatory before any positive reopening | the c-file looks closed, but queen infiltration, rook lift, or perpetual-check geometry still exists |
| `fortress_like_but_not_progressing` | a static hold is confused with a certifiable route | B3 / B4 fortress-risk and bounded-continuation checks | require `progressVisibility` or a specific task shift; otherwise fail closed | the shell stays intact, but no defender resource worsens and no conversion tail appears |
| `slight_edge_overclaim` | a mild edge is narrated as a squeeze | clearly-better gates and existing fail-closed posture cuts | explicit `slightEdgeOverclaimRisk` plus bounded-language cap and progress proof | `+80` to `+120` with local control but no concrete worsening of defender resources |
| `cross_branch_stitching` | best defense, persistence, and continuation are borrowed from different probe branches | existing branch-key / same-defended-branch logic | every proof component must key to the same defended branch, including release suppression | best defense comes from branch A, route persistence from B, and transition proof from C |
| `surface_reinflation` | support carriers revive a stronger claim on planner / replay / wrap-up surfaces | evidence-backed-plan filter, replay parity tests, trust harness | downgrade before planner reuse and keep whole-game reuse blocked until explicitly certified | a deferred shell reappears as `squeeze`, `no counterplay`, or a whole-ending thesis |
| `color_complex_rhetoric_drift` | dark-square / light-square prose outruns the actual local route proof | ontology classification plus current unsafe/deferred boundary | require one named route-loss anchor and block positive prose when that anchor is missing | `dark-square bind` appears even though the defender still has an untouched entry route on the same color complex |

#### Broad validation charter

The B5 validation burden should be read as a candidate-selection charter, not
as permission to widen every dimension at once:

- `likely coverable now` means the dimension itself is not the main blocker for
  the next design slice; it does **not** mean positive rollout is approved
- any `likely deferred` cell needs its own corpus expansion and criticism pack
  before positive certification is accepted
- any `likely unsafe` cell is negative-only for early B5 work and must prove
  fail-closed behavior rather than positive admission

Phase matrix:

| Phase cell | Status | Why |
| --- | --- | --- |
| `late middlegame` | `likely coverable now` | every plausible B5 family still wants the late-middlegame signal density that B2 / B3 / B4 already rely on |
| `heavy-piece middlegame` | `likely deferred` | this is the leading next recon family, but tactical-release density is still the main blocker |
| `transition / endgame-adjacent` | `likely deferred` | B1 route persistence is reusable, but task-shift vs fortress/race ambiguity is not yet closed |
| `pure endgame` | `likely unsafe` | fortress false positives and king-route ambiguity dominate the risk profile |

Evaluation posture matrix:

| Eval posture | Status | Why |
| --- | --- | --- |
| `clearly better` | `likely coverable now` | the only plausible B5 work still needs the strongest local truth posture |
| `slightly better` | `likely deferred` | later work may revisit it, but not through first-slice squeeze rhetoric without explicit progress proof |
| `equal / unclear` | `likely unsafe` | positive local strategic certification would overstate the truth |
| `defending side` | `likely unsafe` | it would turn survival / hold language into a false owner lane |

Texture matrix:

| Texture | Status | Why |
| --- | --- | --- |
| `file-entry bind` | `likely coverable now` | already live as the B4 regression-control cell; reuse is available, but it is not new B5 scope |
| `route-network bind` | `likely deferred` | route continuity and mirage risk need their own criticism pack |
| `heavy-piece local bind` | `likely deferred` | highest reuse, but the heavy-piece release burden is not yet closed |
| `slightly-better local squeeze` | `likely unsafe` | rhetorical inflation and progress ambiguity remain too large |
| `transition-adjacent route` | `likely deferred` | B1 reuse exists, but technical-task ambiguity is still too open |
| `color-complex restriction` | `likely unsafe` | current signals are too rhetorical and insufficiently opponent-facing |
| `fortress/static hold near-miss` | `likely unsafe` | mandatory negative corpus, not early positive scope |
| `tactical-release fake shell` | `likely unsafe` | mandatory negative corpus, not early positive scope |
| `waiting-move-only pseudo strategy` | `likely unsafe` | too easy to confuse with generic improvement or style choice |

Surface matrix:

| Surface | Status | Why |
| --- | --- | --- |
| `planner-owned WhyThis` | `likely deferred` | strongest eventual owner path, but no B5 positive family is approved yet |
| `planner-owned WhatChanged` | `likely deferred` | transition-adjacent reuse is plausible later, but the task-shift proof is not ready |
| `Bookmaker` | `likely deferred` | replay path is reusable, but positional phrasing is too inflation-prone at B5 risk level |
| `Chronicle` | `likely deferred` | same replay reuse exists, but route and ending implications still inflate easily |
| `Active` | `likely deferred` | parity / non-reinflation can be tested now, but positive ownership remains diagnostic-sensitive |
| `whole-game / wrap-up / support reuse` | `likely unsafe` | early B5 work should validate only non-reinflation, not positive wrapper reuse |

#### Mandatory self-critique pass

Candidate attacked:

- the first B5 draft treated `heavy-piece local bind` as the likely next
  positive slice because the B4 file-entry contract looked almost reusable

Nasty cases used to attack that draft:

| Self-critique case | Why the first draft was vulnerable | B5a correction |
| --- | --- | --- |
| `queen_infiltration_shell` | the draft trusted local file-entry denial too much and under-read queen checks/infiltration | add `heavyPieceReleaseInventory`, `perpetualRisk`, and `bestDefenseReleaseSurvivors`; do not approve heavy-piece positivity yet |
| `rook_lift_switch` | the draft watched the file itself and missed cross-rank release geometry | expand `hidden_tactical_release` into heavy-piece-specific release inventory |
| `perpetual_check_escape` | a clearly better shell still looked stable even though the defender could force checks forever | make perpetual-like resources a separate hard-fail burden, not a soft confidence penalty |
| `off_sector_break_after_file_lock` | the draft could call one wing restricted while another freeing break stayed live | strengthen `releaseRisksRemaining` for heavy-piece work and keep heavy-piece scope deferred |
| `waiting_move_pressure_only` | the draft could mistake rook/queen pressure for an actual opponent-facing loss of route usability | keep opponent-facing route-loss proof mandatory and block pressure-only shells |
| `stitched_heavy_piece_bundle` | best defense, release suppression, and continuation could still be borrowed from different branches | keep same-branch proof mandatory for every heavy-piece component |

Design changes forced by self-critique:

- downgrade `heavy-piece local bind` from tentative positive favorite to
  `deferred` / negative-first only
- add `heavyPieceReleaseInventory`, `perpetualRisk`,
  `bestDefenseReleaseSurvivors`, and `tacticalReleaseDensity` to the draft
  contract
- keep `heavy-piece middlegame` and `heavy-piece local bind` at
  `likely deferred` in the validation charter rather than `likely coverable now`
- keep planner / Bookmaker / Chronicle / Active positive ownership closed for
  heavy-piece work until that negative-first burden is cleared
- leave `slightly-better local squeeze` and `bounded color-complex
  restriction` outside the first B5 slice entirely instead of treating them as
  secondary positive options

#### Recommended B5b first slice

Recommended first slice:

- `heavy-piece local bind negative-first validation inside the existing RestrictionProphylaxis -> file-entry reuse lane`

Narrow scope:

- applicable owner lane:
  stay inside the existing `RestrictionProphylaxis` planner/runtime path
- applicable plan family:
  `BreakPrevention` / `KeySquareDenial` only
- applicable phase/eval:
  clearly-better late-middlegame positions with heavy pieces still present
- implementation goal:
  add the backend-only heavy-piece contract / criticism pack and broad
  negative corpus only; do **not** open player-facing heavy-piece positive
  wording yet
- acceptance bar before any later positive reopening:
  same-branch best defense, empty heavy-piece release inventory,
  non-fragile continuation, and green non-reinflation behavior across planner /
  Bookmaker / Chronicle / Active / whole-game negative packs

Why this slice should go first:

- it reuses B4 file-entry and B3 same-branch grammar more directly than the
  other deferred families
- it attacks the most tempting false-positive seam first:
  a familiar local shell that becomes unsafe as soon as heavy-piece tactics are
  live
- if heavy-piece local bind cannot survive that negative-first burden, route
  networks, transition-adjacent routes, and mild-edge squeeze claims should
  stay closed

Implemented B5b slice:

- `queen-on heavy-piece local bind negative-first validation inside the existing RestrictionProphylaxis -> file-entry reuse lane`
- runtime helper:
  backend-only `HeavyPieceLocalBindValidation`
- runtime behavior:
  record a heavy-piece criticism contract, force the affected experiment to
  `deferred`, and block replay/planner reuse of heavy-piece file-entry shells;
  do **not** open player-facing heavy-piece positive wording
- exact-branch release rule:
  `heavyPieceReleaseInventory` now scans replayable exact FEN-backed reply
  branches, while `bestDefenseReleaseSurvivors` stays pinned to
  `bestReplyPv` plus same-branch validation best replies; illegal,
  paraphrased, wrong-base, short-fragment, or legal-prefix-plus-illegal-tail
  heavy-piece lines do not count as release proof
- same-branch identity rule:
  `bestDefenseBranchKey` now requires replayable two-ply defended-branch
  identity from the exact variation or exact best-reply / validation line
  itself; same-first-move fragments and candidate/probed fallbacks do not count
- exact-position regression pack:
  `HeavyPieceLocalBindNegativeValidationTest` with FEN-backed cases for
  `queen_infiltration_shell`, `rook_lift_switch`,
  `perpetual_check_escape`, `off_sector_break_release`,
  `pressure_only_waiting_move`, `direct_best_defense_missing`,
  `stitched_heavy_piece_bundle`, `move_order_fragility`,
  `fortress_like_but_not_progressing`, and `exchange_sac_release`
- exact-path verification helpers:
  `HeavyPieceLocalBindExactBranchReplayTest` proves line-derived release
  features and rejects short / incomplete / illegal paraphrase lines;
  `HeavyPieceLocalBindEngineVerificationTest` pins the full fixed-depth PV1
  path plus replay-derived feature set on exact FEN fixtures, so the engine
  best-path anchor is automatically reproducible beyond the first move only,
  and the verifier now prefers an explicit configured binary before the local
  fallback
- criticism pack now implemented:
  `heavy_piece_release_illusion`, `hidden_off_sector_break`,
  `pressure_only_waiting_move`, `direct_best_defense_missing`,
  `stitched_heavy_piece_bundle`, `move_order_fragility`,
  `fortress_like_but_not_progressing`, `engine_pv_paraphrase`, and
  `surface_reinflation`
- boundary rule:
  B5b applies only to queen-on heavy-piece shells, so the existing B4
  queen-light local file-entry positive slice remains intact
- verification status:
  implementation-green, adversarial-review green, and `current bounded scope
  complete` inside the bounded negative-first charter after the final fix-up
  closed the same-first-move defended-branch seam by requiring replayable
  two-ply identity; B4 queenless certification stayed live, and PV1-only best-
  defense survivors plus wrong-base replay fail-closed behavior remain green
- positive scope after B5b:
  still deferred; this is containment-slice completion only, not heavy-piece
  positive-family completion

Session verdict:

- `CTH-B5a baseline ready`
- reason:
  B5a now has a canonical candidate map, reusable-signal inventory,
  backend-only contract drafts, criticism matrix, broad validation charter,
  and self-critique-revised B5b recommendation without changing runtime
  semantics
- `CTH-B5b negative-first slice complete`
- reason:
  the existing runtime path now contains queen-on heavy-piece local bind shells
  with exact-FEN negative fixtures, exact-branch replay release proof, fixed-
  depth engine-path reproduction, replayable two-ply same-branch identity, and
  cross-surface non-reinflation checks, while heavy-piece positive wording
  remains closed
- `CTH-B5 current bounded scope complete`
- reason:
  the current B5b runtime now has no remaining required work inside its bounded
  negative-first containment charter: exact-board / PV1 truth discipline,
  nasty-case criticism coverage, cross-surface non-reinflation, and B4/B6
  boundary protection are green; this is maintenance-only status for the
  containment slice, not heavy-piece family completion
- next move:
  `B5 maintenance-only watch`; any broader heavy-piece positive or adjacent
  B5/B6 frontier work needs a new bounded charter rather than reopening this
  slice
- residual watch result:
  the final same-first-move defended-branch seam was closed by requiring
  replayable two-ply branch identity and by keeping short / incomplete replay
  fragments fail-closed; no new blocking seam was found after the bounded
  fix-up

## CTH-B6 — Named Route-Network Bind Design Baseline

### Problem framing

- B6 exists only because B4 already owns one bounded
  `file + corroborating entry-square` truth, while B5b only hardens
  heavy-piece fail-closed behavior
- the next positive frontier is worth opening only if `named route-network
  bind` means a bounded opponent-facing route loss that B4 cannot already
  certify
- B6 is **not**:
  - generic square control
  - one-file pressure restated with nicer names
  - an engine maneuver line paraphrased as strategy
  - a whole-position `no counterplay`, domination, or winning-route thesis
- B6 may open only if the claim stays:
  - local
  - exact-board anchored
  - same-defended-branch
  - reroute-aware
  - fail-closed before Chronicle / Bookmaker / Active / whole-game reuse
- architectural boundary:
  any future B6 work must stay inside the existing
  `RestrictionProphylaxis -> file-entry reuse` lane with backend-only
  contract / downgrade discipline; do **not** add a new runtime path,
  owner lane, public payload/schema, or prose family

### Candidate claim map

| Candidate | Expected positive scope | Main overclaim risk | Main falsification burden | Expected surface risk | Strongest reusable signals | B6 verdict |
| --- | --- | --- | --- | --- | --- | --- |
| `file-entry pair + one non-redundant reroute denial` | one file corridor remains usable while one primary entry square and one nearby reroute square/edge both stay denied on the same defended branch | it is just B4 file-entry bind with one extra square counted as a `network` | prove the extra node is independent of the file-entry pair, prove reroute denial survives best defense, and prove bounded continuation still exists without untouched-sector or color-complex escape | `high`; named squares sound richer than the underlying local truth | B4 file-entry pair logic, `PreventedPlan.deniedSquares`, `deniedEntryScope`, `futureSnapshot.planBlockersRemoved`, `bestDefenseBranchKey`, same-branch persistence, claim certification | `selected B6b first-slice candidate`; `likely coverable now` only in this very narrow form |
| `one named route chain + one reroute denial` | one bounded three-node route chain survives as an actual local strategic route rather than a single-axis clamp | route order and connectivity are invented from a maneuver line or from adjacent controlled squares | prove `routeEdges`, route order, same-branch persistence, and defender-facing reroute collapse rather than a pretty node list | `very high`; planner and replay prose can easily narrate the chain more strongly than the proof | denied-square inventory, B3 / B4 independence grammar, `futureSnapshot`, claim certification | `likely deferred`; current signals are still stronger on nodes than on graph continuity |
| `bounded sector route network` | a small sector-level network with more than one entry branch | local restriction inflates into whole-wing or whole-position control | prove untouched-sector reroutes are dead, avoid branch stitching, and keep the claim from sounding like domination | `very high`; Chronicle / wrap-up pressure would overstate it quickly | same-branch grammar, prevented-plan inventory, future snapshots | `likely unsafe`; too close to reinflation and whole-position rhetoric |
| `color-complex / mobility-cage route shell` | a route claim narrated through one color complex or one mobility cage | abstract square-complex prose outruns the exact local route loss | tie the claim to one exact named route loss on the same branch and exclude opposite-color reroutes | `very high`; the rhetoric is stronger than the current proof | denied squares, `WeakComplex`, mobility/cage support signals | `likely unsafe`; keep support-only / negative-first |
| `none / keep the positive frontier closed` | no new positive B6 slice opens until the network candidate survives nasty-case review | reuse bias makes the route idea feel safer than it is | keep the closure fallback available if the selected candidate fails exact-board criticism | `lowest` immediate surface risk | existing B2 / B3 / B4 / B5b fail-closed gates | `fallback only`; not chosen after the self-critique below |

### Backend-only contract drafts

Selected draft:

`namedRouteNetworkBind`

No new public schema is allowed here.
These fields describe a backend-only certification contract only, using the
existing evidence tier / downgrade path.

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `claimScope` | keep the claim local to one file-entry corridor plus one reroute denial, not a sector or whole-position bind | existing backend-only scope gating and B4 local claim limits |
| `primaryAxis` | retain the already-bounded B4 anchor rather than inventing a new owner lane | B4 file-axis / entry-axis grammar |
| `routeNodes` | enumerate the exact route squares that are claimed to matter so later prose cannot invent extra nodes | `PreventedPlan.deniedSquares`, denied entry/break nodes already present in strategic evidence |
| `routeEdges` | record which node-to-node connections are actually being claimed instead of treating adjacency as proof | `futureSnapshot.planPrereqsMet`, `planBlockersRemoved`, continuation motifs, direct reply probes |
| `rerouteDenials` | capture the exact alternative entry / reroute that must also stay shut for the network claim to be non-trivial | prevented-plan inventory, `deniedResourceClass`, blocked-break evidence |
| `axisIndependence` | prove the reroute node is not just another spelling of the file or first entry-square claim | B3 / B4 axis-independence grammar |
| `bestDefenseBranchKey` | keep every proof component on the same defended branch | existing branch-key reuse |
| `sameDefendedBranch` | fail closed when node denial, reroute denial, and continuation come from different branches | B1 / B3 / B4 / B5 same-branch grammar |
| `pressurePersistence` | require the bounded shell to survive concrete best defense | `reply_multipv`, `defense_reply_multipv`, `bestReplyStable` |
| `routeContinuity` | show that the denied network still points to one bounded follow-through route for the stronger side | `futureSnapshot.planBlockersRemoved`, `targetsDelta`, `convert_reply_multipv`, continuation motifs |
| `continuationBound` | keep the continuation explicitly local so the claim does not drift into global domination or winning-route language | bounded continuation checks, B4/B5b local truth discipline |
| `releaseRisksRemaining` | enumerate still-live releases, untouched-sector breaks, and tactical escapes that make the network non-certifiable | `newThreatKinds`, `keyMotifs`, collapse reasons, direct reply probes |
| `routeNetworkMirageRisk` | mark when the node picture is aesthetically plausible but not sufficiently opponent-facing | denied-square inventory plus route-denial validation discipline |
| `redundantAxisRisk` | fail when the proposed network is just one axis counted twice | B3 / B4 independence grammar, file-entry control cell comparison |
| `counterplayReinflationRisk` | block stronger surface rhetoric if the local truth is uncertified or weakly bounded | existing replay parity / trust harness / evidence-tier downgrade path |
| `claimCertification` / `failsIf` / `confidence` / `evidenceSources` | keep any future B6 work inside the existing auditable certification grammar | `PlanEvidenceEvaluator.ClaimCertification` and current contract patterns |

Deferred broader drafts:

| Candidate draft | Extra burden beyond the selected draft | Current verdict |
| --- | --- | --- |
| `routeChainBind` | explicit route order, edge continuity beyond one file anchor, and a stronger anti-PV-paraphrase proof | `likely deferred` |
| `sectorRouteNetworkBind` | sector boundary definition, untouched-sector reroute inventory, and stronger surface containment | `likely unsafe` |
| `colorComplexRouteShell` | exact route anchor on one color complex plus opposite-color escape exclusion | `likely unsafe` |

### Criticism matrix

| Axis | Why risky | Current reusable guard | Required future B6 guard | Required difficult fixture shape |
| --- | --- | --- | --- | --- |
| `route_network_mirage` | a square bundle sounds coherent even though no defender-facing route continuity was actually removed | denied-square inventory, B3 / B4 axis-independence grammar, same-branch persistence | require `routeEdges`, non-empty `rerouteDenials`, and bounded continuation on the same branch | reuse the B4 late-middlegame control FEN `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24` and try to certify `{c-file,b4,c5}` without proving a cut-off reroute beyond the B4 pair |
| `redundant_square_counting` | one file axis is counted as several nodes so the claim sounds like a network | B4 file-entry control cell, B3 independence grammar | require `redundantAxisRisk` hard-fail whenever the extra node sits on the already-certified axis or adds no new reroute burden | use the same B4 control FEN and treat `c5` as an extra node on the same c-file shell; B6 must fail unless an independent reroute is also denied |
| `untouched_sector_reroute` | one local route is closed while another nearby sector still keeps the defender's counterplay route alive | prevented-plan inventory, `futureSnapshot`, B5 release-risk grammar | require `releaseRisksRemaining` / `rerouteDenials` to inventory off-sector breaks and wing switches explicitly | reuse `off_sector_break_release` at `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P2N2/PPQ2PPP/2RR2K1 w - - 0 24` and ensure one denied route does not hide a still-live sector reroute |
| `color_complex_escape` | the route looks closed on one color complex while an opposite-color approach still survives | denied squares, `WeakComplex` support signals, existing unsafe/deferred boundary | require an explicit opposite-color escape check before any positive wording survives | reuse `stitched_heavy_piece_bundle` at `2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1PN2/PPQ2P1P/2RR2K1 w - - 0 24` and test that a same-sector-looking net still fails if the opposite-color reroute remains |
| `move_order_fragility` | the lock holds only in one exact sequence and collapses under the best practical move order | existing fragility handling, `moveOrderSensitive`, collapse reasons | contract-level hard fail for `move_order_fragility` before any route-network certification | reuse `move_order_fragility` at `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PNP1/PPQ2P1P/2RR2K1 w - - 0 24` and require the network to survive best-order defense |
| `engine_pv_paraphrase` | a maneuver line gets retold as a network bind even though no bounded route loss was measured | claim-certification quantifier / attribution / ontology checks | require node/edge/reroute evidence and opponent-facing denial, not just a PV maneuver | reuse the heavy-piece middlegame control FEN `r1bq1rk1/pp3ppp/2n1pn2/2bp4/3P4/2N1PN2/PPQ2PPP/R1B2RK1 w - - 0 12`; a line like rook doubling or king improvement alone must not certify a network |
| `cross_branch_stitching` | best defense, reroute denial, and continuation are borrowed from different branches | existing branch-key / same-defended-branch logic | require node denial, reroute denial, persistence, and continuation to share one `bestDefenseBranchKey` | reuse `stitched_heavy_piece_bundle` at `2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1PN2/PPQ2P1P/2RR2K1 w - - 0 24` and block any stitched network shell |
| `static_net_without_progress` | a stable-looking net is confused with certifiable strategic progress even though no bounded continuation exists | B3 / B4 fortress-risk and bounded-continuation checks | require `continuationBound` plus visible blocker removal / target change, otherwise fail closed | reuse `fortress_like_but_not_progressing` at `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P2NP1/PPQ2P1P/2RR2K1 w - - 0 24` and block network wording when no defender resource actually worsens |
| `surface_reinflation` | uncertified local route shells are restated as stronger planner/replay/wrap-up theses | B5b replay gate, trust harness, evidence-backed-plan filter | keep downgrade-before-reuse discipline and block whole-game reuse until explicit later signoff | reuse the surface reinflation FEN `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/P1P1PN2/1PQ2PPP/2R2RK1 w - - 0 24` across planner / Bookmaker / Chronicle / Active / whole-game |
| `file_entry_restatement_only` | the draft does not add a new burden beyond B4 and simply renames the file-entry pair as a network | existing B4 certified surface-pair gate and local file-entry contract | require one independently denied reroute beyond the B4 pair or fail back to B4-only truth | reuse the B4 positive control FEN `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24` with the existing `c-file + b4` pair and confirm B6 does not open without a third non-redundant denial |

### Broad validation charter

The B6 validation burden should be read as a narrow admission charter only:

- `likely coverable now` means the cell could belong to the first B6b
  implementation slice **if** the selected contract above is enforced
- `likely deferred` means the cell needs more exact-board corpus, broader
  criticism, or stronger surface blocking before positive certification
- `likely unsafe` means early B6 work should validate only fail-closed behavior

Phase matrix:

| Phase cell | Status | Why |
| --- | --- | --- |
| `late middlegame` | `likely coverable now` | this is the only phase where B4-style local route truth, reroute denial, and bounded continuation currently overlap strongly enough |
| `heavy-piece middlegame` | `likely deferred` | B5b shows the release burden is still too large for early positive network wording |
| `transition / endgame-adjacent` | `likely deferred` | B1 route persistence is reusable, but task-shift and fortress ambiguity remain too open |
| `pure endgame` | `likely unsafe` | route-network rhetoric would outrun technical truth too easily |

Evaluation posture matrix:

| Eval posture | Status | Why |
| --- | --- | --- |
| `clearly better` | `likely coverable now` | the first B6 slice still needs the strongest local truth posture |
| `slightly better` | `likely deferred` | mild-edge control is still too easy to narrate as a network bind without progress proof |
| `equal / unclear` | `likely unsafe` | positive route-network certification would overstate the truth |
| `defending side` | `likely unsafe` | this would create a false owner lane for survival / hold claims |

Texture matrix:

| Texture | Status | Why |
| --- | --- | --- |
| `file-entry bind` | `likely coverable now` | existing B4 control cell remains the comparison baseline, not new B6 scope |
| `named route-network bind` | `likely coverable now` | but only for `file-entry pair + one non-redundant reroute denial` inside one bounded sector |
| `heavy-piece local bind` | `likely deferred` | B5b remains containment-only and release-heavy |
| `transition-adjacent route` | `likely deferred` | B1 reuse exists, but not for first B6 implementation |
| `slight-edge local squeeze` | `likely unsafe` | progress and rhetoric risk are still too open |
| `color-complex / mobility-cage` | `likely unsafe` | current proof is too abstract and too prose-prone |
| `static near-miss` | `likely unsafe` | mandatory negative corpus, not early positive scope |
| `tactical-release fake shell` | `likely unsafe` | mandatory negative corpus, not early positive scope |

Surface matrix:

| Surface | Status | Why |
| --- | --- | --- |
| `planner-owned WhyThis` | `likely coverable now` | this is the narrowest owner path for a local route-network claim if the B6 contract certifies |
| `planner-owned WhatChanged` | `likely deferred` | change-language drifts too easily into bigger route or conversion theses |
| `Bookmaker` | `likely deferred` | replay phrasing is reusable later, but the network wording is still inflation-prone |
| `Chronicle` | `likely deferred` | same replay reuse exists, but route prose still needs stronger containment |
| `Active` | `likely deferred` | non-reinflation can be checked, but positive ownership is still diagnostic-sensitive |
| `whole-game / wrap-up / support reuse` | `likely unsafe` | B6 should validate non-reinflation only, not positive wrapper reuse |

### Mandatory self-critique pass

Candidate attacked:

- the first B6 draft treated `one file + two linked squares` as enough to call
  the position a `named route-network bind`

Nasty cases used to attack that draft:

| Self-critique case | Why the first draft was vulnerable | B6 correction |
| --- | --- | --- |
| `file_entry_restatement_only` | the draft could promote the existing B4 `file + entry-square` truth into a `network` just by counting one extra square on the same axis | narrow the candidate to `file-entry pair + one non-redundant reroute denial`; add `redundantAxisRisk` hard-fail |
| `route_network_mirage` | the draft trusted named squares more than actual defender-facing route continuity | add explicit `routeEdges` plus non-empty `rerouteDenials`; require same-branch bounded continuation |
| `untouched_sector_reroute` | the draft watched the local corridor but could still ignore another live route in the same sector or on the other wing | add `releaseRisksRemaining` and `rerouteDenials` as mandatory certification fields |
| `cross_branch_stitching` | node denial, persistence, and continuation could be borrowed from different branches and still sound coherent in prose | make `sameDefendedBranch` explicit in the B6 draft and fail whenever proof components do not share one branch key |
| `move_order_only_route_lock` | the route lock could hold only if the defender cooperated with one exact order | add contract-level `move_order_fragility` hard fail before any positive surface eligibility |
| `static_net_without_progress` | the draft could narrate a stable shape as a route bind even when there was no bounded continuation or resource worsening | add `continuationBound` and require visible blocker removal / target change before certification |
| `surface_reinflation` | planner/replay/wrap-up surfaces could still revive a stronger thesis from an uncertified local shell | keep only `planner-owned WhyThis` as the first potentially coverable owner path and leave replay / wrap-up deferred or unsafe |
| `engine_pv_paraphrase` | a line of maneuvers could still be restated as a network without measured route loss | require node/edge/reroute proof and keep line paraphrase alone non-certifying |

Design changes forced by self-critique:

- narrow the preferred candidate from
  `one file + two linked squares` to
  `one file-entry pair + one non-redundant reroute denial`
- add `routeEdges`, `rerouteDenials`, `sameDefendedBranch`,
  `continuationBound`, `releaseRisksRemaining`, and `redundantAxisRisk`
  to the selected draft
- keep `named route-network bind` as `likely coverable now` only for the narrow
  first slice above, not for general route chains or sector networks
- tighten the surface charter:
  only `planner-owned WhyThis` is tentatively coverable;
  `WhatChanged`, Bookmaker, Chronicle, and Active stay `likely deferred`;
  whole-game / wrap-up remain `likely unsafe`
- keep heavy-piece, transition-adjacent, slight-edge, and color-complex route
  families outside the first B6 slice

Revision delta after self-critique:

- before:
  B6 could have been read as `file + two named squares`
- after:
  B6 opens only when the third node is an independently denied reroute and the
  proof survives same-branch best defense with bounded continuation
- before:
  replay surfaces looked tempting because B4 replay infrastructure already
  exists
- after:
  replay and wrap-up remain closed for early B6; only planner-owned WhyThis is
  even tentatively admissible

### Recommended B6b first slice

Recommended first slice:

- `one file-entry pair + one non-redundant reroute denial on the same defended branch inside the existing RestrictionProphylaxis -> file-entry reuse lane`

Narrow scope:

- applicable owner lane:
  stay inside the existing `RestrictionProphylaxis` planner/runtime path
- applicable plan family:
  `BreakPrevention` / `KeySquareDenial` only
- applicable phase/eval:
  clearly-better late-middlegame positions only
- structural burden:
  the B4 file-entry pair must already be locally certifiable, and B6 adds
  exactly one extra reroute denial that is independent of that pair
- continuation burden:
  require same-branch best defense, bounded continuation, and visible local
  blocker / target change
- exclusion burden:
  fail for heavy-piece queen-on shells, untouched-sector reroutes,
  opposite-color escapes, exact-order-only locks, static holds, stitched
  branches, and surface reinflation
- surface scope:
  if B6b opens later, open only `planner-owned WhyThis` first; keep
  `WhatChanged`, Bookmaker, Chronicle, Active, and whole-game / wrap-up closed
- implementation boundary:
  backend-only contract / downgrade work only; no new public payload/schema,
  owner lane, or prose family

Why this slice should go first:

- it is the smallest route-network claim that is genuinely stricter than B4
- it reuses existing branch-key, denied-square, prevented-plan, future-snapshot,
  and claim-certification grammar without inventing a parallel runtime path
- it attacks the nastiest early false positive first:
  counting a pretty square picture as a `network`
- if this slice cannot survive exact-board reroute and reinflation criticism,
  broader route-chain or sector-network claims should stay closed

Session verdict:

- `CTH-B6a baseline ready`
- reason:
  B6 now has a canonical problem frame, candidate map, backend-only contract
  draft, criticism matrix, broad validation charter, and self-critique-revised
  B6b recommendation without changing runtime semantics

## CTH-B6b — Named Route-Network Bind Narrow Positive Slice

### Runtime scope

- B6b now opens only one positive slice:
  one B4-certifiable `file-entry pair + one non-redundant reroute denial` on
  the same defended branch, only in clearly-better late middlegames, only on
  the existing `RestrictionProphylaxis -> file-entry reuse` lane
- no new runtime path, payload/schema field, prose family, or owner lane was
  introduced
- positive owner scope is planner-owned `WhyThis` only
- `WhatChanged`, Bookmaker, Chronicle, Active, and whole-game / wrap-up remain
  replay-closed for `sourceKinds += named_route_network_bind`

### Contract and hard fails

- runtime helper:
  `NamedRouteNetworkBindCertification`
- reused evidence:
  existing B4 contract, `PreventedPlan.deniedSquares`, `deniedEntryScope`,
  `futureSnapshot.planBlockersRemoved`, `bestDefenseBranchKey`, same-branch
  grammar, and evidence-backed-plan filtering
- new backend-only fields:
  primary axis, route nodes, route edges, reroute denials, axis independence,
  same-branch continuity, continuation bound, release risks, mirage /
  redundancy / reinflation risk, move-order fragility, fail reasons, and
  evidence sources
- branch-identity fix-up:
  same-defended-branch proof no longer falls back to `moves.headOption`; B6 now
  requires a replayable defended-branch token (`variationHash`, `seedId`, or a
  two-ply reply-line key), and missing / ambiguous branch identity fails closed
  as `same_branch_identity_missing` or `ambiguous_defended_branch`
- planner surface fix-up:
  planner-owned `WhyThis` no longer rebuilds a route network from raw
  `preventedPlansNow`; it reads only the exact carried B6 triplet
  (`file`, `entrySquare`, `rerouteSquare`), while
  `NamedRouteNetworkBindCertification.certifiedSurfaceNetwork` itself now fails
  closed unless one exact plan-scoped triplet is uniquely named by the
  experiment-filtered evidence-backed plans
- mandatory hard fails:
  `file_entry_restatement_only`, `route_network_mirage`,
  `same_branch_identity_missing`, `ambiguous_defended_branch`,
  `redundant_square_counting`, `untouched_sector_reroute`,
  `color_complex_escape`, `cross_branch_stitching`,
  `static_net_without_progress`, `engine_pv_paraphrase`,
  `move_order_fragility`, `heavy_piece_release_shell`, and
  `surface_reinflation`

### Exact-position validation

- exact positive control fixture:
  `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24`
- exact route witness used by the local verifier:
  prefix `a3b4 a7a5 b4a5 c6a5 f3e5`
- exact negative / near-miss fixture set now covers:
  `file_entry_restatement_only`, `route_network_mirage`,
  `same_first_move_divergent_branch`,
  `ambiguous_defended_branch`,
  `redundant_square_counting`, `untouched_sector_reroute`,
  `color_complex_escape`, `move_order_fragility`,
  `cross_branch_stitching`, `static_net_without_progress`,
  `engine_pv_paraphrase`, `surface_reinflation`,
  `missing_branch_identity_fails_close`, and `heavy_piece_release_shell`
- self-critique result:
  same-first-move divergent replies now fail closed instead of borrowing
  first-move branch identity, and planner `WhyThis` now stays pinned to the
  exact carried triplet even when raw prevented plans contain a stronger-looking
  composite shell

### Session status

- implementation status:
  implementation-green on the bounded planner-only slice
- review status:
  adversarial-review green on the bounded planner-only slice after the branch
  identity and planner surface fix-up
- operational consequence:
  B6b is now `close-ready` inside the narrow planner-only charter, which closes
  the B6 minimum slice only; the next required work remains B6-family
  expansion, with broader route-chain coverage first, then broader
  posture/phase coverage, before any replay / whole-game positive reuse review.
  Keep sector-network, color-complex, and stronger whole-position thesis cells
  closed

## CTH-B6 Expansion Baseline

### Problem framing

- B6 minimum slice is complete:
  B6b is now `close-ready` inside one bounded planner-owned `WhyThis` charter
- B6 product-complete is not:
  the live slice still proves only one `file-entry pair + one reroute denial`
- next work remains inside B6, not B7:
  choose which route-network expansion can stay truth-owning on the existing
  `RestrictionProphylaxis -> file-entry reuse` architecture
- exact-board rule:
  every expansion candidate must stay tied to exact FEN-backed best-defense and
  same-branch continuation proof; route rhetoric does not count as validation
- boundary rule:
  no new runtime path, payload/schema, prose family, replay lane, or owner lane
  may open as part of this baseline

### Candidate map

| Candidate | Expected positive scope | Product value | Trust risk | Strongest reusable signals | New falsification burden | Surface risk | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `broader route-chain rollout` | one current B6 triplet plus one exact intermediate node and one reroute denial on the same defended branch, still clearly-better late middlegame, still planner-owned `WhyThis` only | first product-meaningful widening beyond a local triplet because the route claim starts expressing actual continuity | `high` but still bounded | B4 file-entry pair, B6 reroute denial, same-defended-branch grammar, `futureSnapshot.planBlockersRemoved`, `planPrereqsMet`, `PreventedPlan.deniedSquares`, `deniedEntryScope`, `claimCertification` | `fake_route_chain`, `redundant_intermediate_node`, `chain_only_on_nonbest_branch`, `untouched_sector_escape`, root-best failure on the played move | `medium-high` if replay stays closed | `blocked on the current exact FEN`; after-move proof survives, but the move itself does not pass the live root-best gate |
| `broader posture/phase expansion` | the same B6/B6-expansion truth in slightly-better late middlegame, heavy-piece middlegame, or transition-adjacent cells | broad row coverage once the route contract is trusted | `very high` | current B6 route continuity, B1 persistence grammar, B5b heavy-piece exclusion, current eval/phase gating | `posture_inflation`, heavy-piece release proof, transition task-shift proof, stronger progress burden | `high` | `deferred` until a broader route-chain slice survives exact-board criticism on the current cell |
| `replay / whole-game positive reuse review` | limited Chronicle / Bookmaker / Active / wrapper reuse of planner-certified route truth | high cross-surface impact | `very high` | replay-closed `sourceKinds`, `StrategicNarrativePlanSupport.evidenceBackedMainPlans`, planner replay filters, existing non-reinflation harnesses | `replay_reinflation`, `whole_game_wrapper_leak`, exact carried-contract parity across surfaces | `very high` | `deferred` until route-chain truth is broader and stable on planner-only scope |
| `sector-network rollout` | more than one local chain or more than one entry branch in the same sector | medium-high if it ever becomes safe | `extreme` | prevented-plan inventory, `releaseRisksRemaining`, same-branch grammar, `deniedEntryScope = sector` support signals | sector-boundary definition, multi-chain continuity, `untouched_sector_escape`, local-to-sector overreach | `extreme` | `likely unsafe` as the first expansion candidate |
| `color-complex route expansion` | a route shell narrated through one color complex or mobility cage | medium prose value, low truth margin | `extreme` | denied-square inventory, weak-complex / mobility-cage support signals, `futureSnapshot` | `color_complex_escape`, exact route anchor on one branch, mobility-cage release proof, rhetoric drift | `extreme` | `likely unsafe`; keep support-only / negative-first |
| `none / more B6 hardening first` | keep the positive frontier closed and do no B6-family widening yet | lowest immediate risk but no product movement | `low` | existing B2 / B3 / B4 / B5b / B6b fail-closed gates | none beyond current regression watch | `lowest` | selected by the 2026-04-02 live-runtime triage because the only plausible route-chain slice failed the root-best gate |

### Backend-only contract drafts

Selected draft:

`namedRouteChainBind`

No new public schema is allowed here.
These fields describe a backend-only certification contract only, still using
the existing evidence-tier / downgrade path.

| Field | Purpose | Existing reuse path |
| --- | --- | --- |
| `claimScope` | keep the claim local to one B6-certified corridor plus one exact intermediate node and one reroute denial | current B4/B6 local-scope gating |
| `primaryAxis` | retain the already-certified B4 file anchor instead of inventing a new owner lane | B4 file-axis grammar |
| `routeNodes` | enumerate the exact node set the chain claims to deny | `PreventedPlan.deniedSquares`, denied entry/break nodes already present in the carrier |
| `intermediateNodes` | record the single added chain node explicitly so the first expansion cannot drift into sector graphs | current denied-square inventory plus B3/B4 independence grammar |
| `routeEdges` | require an ordered `entry -> intermediate -> reroute` witness path on the same defended branch | `futureSnapshot.planPrereqsMet`, `planBlockersRemoved`, continuation motifs, direct reply probes |
| `rerouteDenials` | capture the alternative reroute that makes the chain non-trivial | prevented-plan inventory, `deniedResourceClass`, current B6 reroute reuse |
| `axisIndependence` | prove the intermediate node is not just a renamed file / entry / reroute square | B3 / B4 / B6 independence grammar |
| `bestDefenseBranchKey` | keep every proof component on one defended branch | existing branch-key reuse |
| `sameDefendedBranch` | fail closed when node proof, edge proof, or continuation come from different branches | current B1 / B3 / B4 / B6 same-branch grammar |
| `routeContinuity` | require concrete best-defense presence, same-branch edge proof, reroute proof, and bounded continuation | `reply_multipv`, `defense_reply_multipv`, `futureSnapshot`, `convert_reply_multipv` |
| `continuationBound` | keep the continuation explicitly local so the chain does not drift into sector or whole-position rhetoric | B4/B6 bounded-continuation discipline |
| `releaseRisksRemaining` | inventory live sector escapes, breaks, and tactical releases that would puncture the chain | `newThreatKinds`, `keyMotifs`, collapse reasons, direct reply probes, B5b release grammar |
| `routeChainMirageRisk` | mark when the node list looks coherent but ordered continuity is not defender-facing enough | B6 mirage logic plus explicit edge witness burden |
| `postureInflationRisk` | force the first expansion to fail closed outside clearly-better late middlegame | current eval / phase gate plus B5b heavy-piece exclusion |
| `counterplayReinflationRisk` | keep replay / wrapper rhetoric blocked if the chain is uncertified or weakly bounded | existing replay parity / trust harness / evidence-tier downgrade path |
| `claimCertification` / `confidence` / `evidenceSources` | keep the expansion auditable inside the existing certification grammar | `PlanEvidenceEvaluator.ClaimCertification` and current contract patterns |

Deferred broader drafts:

| Candidate draft | Extra burden beyond the selected draft | Current verdict |
| --- | --- | --- |
| `routeNetworkPhaseExpansion` | explicit `phaseCell`, `evalPosture`, `taskShiftRisk`, stronger progress proof, and a real `postureInflationRisk` gate | `likely deferred` |
| `routeNetworkReplayReuseReview` | `surfaceScope`, replay parity, wrapper leak proof, and exact carried-contract reuse instead of replay recomposition | `likely deferred` |
| `sectorRouteNetworkBind` | sector-boundary ownership, multi-chain continuity, untouched-sector escape inventory, and stronger anti-whole-position wording proof | `likely unsafe` |
| `colorComplexRouteShell` | one exact color-complex route anchor, opposite-color escape exclusion, and mobility-cage release proof | `likely unsafe` |

### Criticism matrix

| Axis | Why risky | Current reusable guard | Required B6 expansion guard | Required difficult fixture shape |
| --- | --- | --- | --- | --- |
| `fake_route_chain` | adjacent denied squares can sound like a chain even when no ordered defender-facing continuity was removed | current B6 `routeEdges`, `rerouteDenials`, same-branch persistence, B4 control pair | require per-edge witnesses on the best-defense branch for `entry -> intermediate` and `intermediate -> reroute`; no edge may come from prose alone | use the B6 positive control FEN `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24` and try to certify `{b4,d5,a5}` without same-branch edge proof |
| `redundant_intermediate_node` | the added node may just restate the file or entry axis and make B4 sound richer than it is | B3 / B4 independence grammar, B6 `redundant_square_counting` | compare intermediate-node role against file / entry / reroute roles and fail when it adds no new reroute burden | reuse the same control FEN and treat `c5` as the supposed intermediate node on the existing c-file shell |
| `chain_only_on_nonbest_branch` | the chain may survive only on a non-best branch while best defense still keeps the route alive | B6 branch-identity hardening, `bestDefenseBranchKey`, same-branch filter | every chain edge, reroute denial, and continuation witness must be present on the exact best-defense branch, not only on validation side branches | reuse `same_first_move_divergent_branch` on `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24` with direct reply `a7a5 b4a5 c6a5` versus validation `a7a5 h4h5` |
| `untouched_sector_escape` | a local chain is closed while another wing or sector still gives the defender a practical reroute | current `releaseRisksRemaining`, B5b release grammar, B6 `untouched_sector_reroute` | keep the first expansion local and fail whenever another sector escape remains live; sector language does not open here | reuse `off_sector_break_release` at `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P2N2/PPQ2PPP/2RR2K1 w - - 0 24` |
| `posture_inflation` | once chain prose sounds richer, slightly-better or phase-shifted cells are easy to overstate as bind truth | clearly-better phase gate, heavy-piece exclusion, B4/B6 local-scope discipline | add `postureInflationRisk`, keep the next slice on the existing clearly-better late-middlegame cell, and require a separate corpus before any posture/phase widening | reuse `slightly_better_overclaim_shell` on `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24` with the slight-edge eval bucket only |
| `replay_reinflation` | planner-only chain truth can be rebuilt into a stronger replay thesis than the contract actually proved | replay-closed `sourceKinds`, planner-first replay filters, trust harness | keep replay positive reuse closed on the next slice and require exact carried-chain parity before later review | reuse `planner_raw_recomposition_stronger_than_contract` on `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24` |
| `whole_game_wrapper_leak` | support-only route residue can seed wrap-up or decisive-wrapper rhetoric | whole-game / wrap-up are already closed to B6, truth gate bans support-only decisive wrappers | keep wrapper reuse explicitly unsafe until a later review proves exact carried-chain consumption and no new decisive wrapper seed | reuse `surface_reinflation_case` at `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/P1P1PN2/1PQ2PPP/2R2RK1 w - - 0 24` across planner / replay / whole-game |
| `heavy_piece_route_shell` | queen-on or release-rich middlegames can mimic a chain while hiding tactical or perpetual escape | B5b `HeavyPieceLocalBindValidation`, `heavyPieceLocalBindBlocked`, current queen-count gate | inherit the heavy-piece negative-first exclusion unchanged; no positive chain rollout with queen-on shells | reuse the heavy-piece control FEN `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24` |
| `engine_pv_paraphrase` | a maneuver line can be retold as a strategic chain even though no route loss was measured | claim-certification quantifier / attribution / ontology checks, current B6 `engine_pv_paraphrase` fail | require prevented-plan nodes plus same-branch edge and reroute proof; plan text or PV alone never certifies the chain | reuse the heavy-piece middlegame control FEN `r1bq1rk1/pp3ppp/2n1pn2/2bp4/3P4/2N1PN2/PPQ2PPP/R1B2RK1 w - - 0 12` |

### Broad validation charter

The next B6 expansion burden should still be read as a narrow admission charter:

- `likely coverable next` means the cell can belong to the next bounded B6
  implementation slice **if** the selected route-chain contract above is
  enforced
- `likely deferred` means the cell needs its own exact-board corpus, criticism
  pack, or stronger containment before positive certification
- `likely unsafe` means early B6 work should validate only fail-closed behavior

Phase matrix:

| Phase cell | Status | Why |
| --- | --- | --- |
| `late middlegame` | `likely coverable next` | this is still the only phase where local route truth, same-branch reroute proof, and bounded continuation overlap strongly enough |
| `heavy-piece middlegame` | `likely deferred` | B5b already shows the release burden is too large for early positive route-chain wording |
| `transition / endgame-adjacent` | `likely deferred` | B1 persistence reuse exists, but task-shift and fortress/progress ambiguity are still too open |
| `pure endgame` | `likely unsafe` | route-chain rhetoric would outrun technical truth too easily |

Evaluation posture matrix:

| Eval posture | Status | Why |
| --- | --- | --- |
| `clearly better` | `likely coverable next` | the first expansion still needs the strongest local truth posture |
| `slightly better` | `likely deferred` | route-chain wording is too easy to inflate without stronger progress proof |
| `equal / unclear` | `likely unsafe` | positive route-chain certification would overstate the truth |
| `defending side` | `likely unsafe` | this would create a false owner lane for hold/survival claims |

Texture matrix:

| Texture | Status | Why |
| --- | --- | --- |
| `B6 current narrow slice` | `likely coverable next` | this remains the regression control cell that every broader candidate must preserve |
| `broader route-chain` | `likely coverable next` | but only as one exact intermediate node plus one reroute denial on the same defended branch |
| `posture/phase expansion` | `likely deferred` | more row coverage is attractive, but the trust burden is still larger than the texture gain from staying on the current cell |
| `replay reuse` | `likely deferred` | review can come later, but only after a broader planner-only truth contract exists |
| `sector-network` | `likely unsafe` | too close to sector rhetoric and untouched-sector escape overclaim |
| `color-complex route shell` | `likely unsafe` | current proof remains too abstract and too prose-prone |

Surface matrix:

| Surface | Status | Why |
| --- | --- | --- |
| `planner-owned WhyThis` | `likely coverable next` | this is still the narrowest owner path for a bounded route-chain claim |
| `planner-owned WhatChanged` | `likely deferred` | change-language drifts too easily into broader route or conversion theses |
| `Bookmaker` | `likely deferred` | replay phrasing is reusable later, but the chain wording is still inflation-prone |
| `Chronicle` | `likely deferred` | same replay reuse exists, but exact carried-chain parity is not proven yet |
| `Active` | `likely deferred` | non-reinflation can be checked, but positive ownership is still diagnostic-sensitive |
| `whole-game / wrap-up / support reuse` | `likely unsafe` | the next slice should validate non-reinflation only, not positive wrapper reuse |

### Mandatory self-critique pass

Candidate attacked:

- the first expansion draft treated `one named route chain + one reroute denial`
  as enough, without pinning the intermediate node, the best-defense branch, or
  replay closure tightly enough

Nasty cases used to attack that draft:

| Self-critique case | Why the first draft was vulnerable | B6 expansion correction |
| --- | --- | --- |
| `fake_route_chain` | the draft could count an aesthetically plausible intermediate square even when no ordered same-branch edge was proved | require per-edge witnesses on the best-defense branch for both chain edges |
| `redundant_intermediate_node` | the extra node could just restate the existing B4/B6 axis and sound richer than the truth | add explicit `intermediateNodes` plus stronger `axisIndependence` comparison against file / entry / reroute roles |
| `chain_only_on_nonbest_branch` | the draft could borrow the chain edge from a non-best validation branch while best defense still kept the route | require all node, edge, reroute, and continuation proof on one `bestDefenseBranchKey` |
| `untouched_sector_escape` | the draft watched the local chain but not a still-live sector break | keep the first expansion local-only and require explicit off-sector escape inventory inside `releaseRisksRemaining` |
| `replay_reinflation` | replay infrastructure already exists, so the chain could have been allowed to spread too early | keep the next slice planner-owned `WhyThis` only and postpone positive replay review |
| `whole_game_wrapper_leak` | support-only chain residue could seed wrap-up or decisive-shift rhetoric | keep whole-game / wrap-up positive reuse explicitly `unsafe` in the charter |
| `heavy_piece_route_shell` | queen-on shells can sound like route chains even when they are release-rich | inherit B5b heavy-piece exclusion unchanged and keep queen-on positives negative-first only |
| `engine_pv_paraphrase` | an engine maneuver line could be rewritten as a route chain without measured route loss | require prevented-plan node inventory plus same-branch edge/reroute proof rather than plan text or PV alone |

Design changes forced by self-critique:

- narrow the selected candidate from
  `one named route chain + one reroute denial`
  to
  `one B6-certified triplet plus one exact intermediate node, with both chain
  edges and the reroute denial witnessed on the same defended branch`
- add `intermediateNodes`, stronger per-edge `routeEdges`, and
  `postureInflationRisk` to the selected draft
- keep the surface charter strict:
  only `planner-owned WhyThis` is `likely coverable next`;
  replay review stays `likely deferred`;
  whole-game / wrap-up stay `likely unsafe`
- keep heavy-piece, posture/phase, sector-network, and color-complex cells
  outside the next slice entirely

Revision delta after self-critique:

- before:
  the next slice could have been read as any plausible `route chain`
- after:
  the next slice is only one exact intermediate-node chain above the current B6
  triplet, with same-branch edge proof and bounded continuation
- before:
  replay and wrapper review looked like a small follow-on
- after:
  replay remains `deferred` and whole-game / wrap-up remain `unsafe` until a
  later explicit review
- before:
  posture/phase widening looked parallel with route-chain widening
- after:
  posture/phase widening is explicitly behind the selected route-chain burden

### Recommended next B6 implementation slice

Recommended next slice:

- no released B6-family expansion slice on the current branch:
  the only plausible bounded candidate remained the broader route-chain rollout
  above the current B6 triplet, but the 2026-04-02 exact-board rerun blocked it
  because the played move did not survive the live root-best gate

Why this slice should go first:

- it was the smallest product-meaningful widening that was genuinely stricter
  than current B6b
- it kept phase, eval posture, and surface scope fixed while reusing the
  current B4/B6 branch-key, denied-square, prevented-plan, future-snapshot,
  and claim-certification grammar
- it directly attacked the next early false positive:
  telling a prettier chain story than the board truth can actually sustain
- because this slice did not survive exact-board root-best criticism, broader
  posture/phase widening, replay reuse, sector-network, and color-complex work
  must all stay closed

Session verdict:

- `CTH-B6 expansion baseline ready`
- reason:
  B6 expansion now has a canonical candidate map, selected backend-only
  contract draft, criticism matrix, broad validation charter, and self-critique
  delta that keep the next queue inside B6 rather than drifting into B7 or a
  broader route-network rollout

### Broader route-chain closeout campaign

Purpose:
decide whether the current B6 intermediate-only live slice is more than a
one-example shell, and either produce a second exact-FEN survivor or stop
A-track closeout honestly.

Current scope:

- same clearly-better late middlegame cell only
- same planner-owned `WhyThis` only
- same `RestrictionProphylaxis -> file-entry reuse` architecture only
- one exact B6 triplet plus one exact intermediate node
- same defended branch only
- no new runtime semantics; local verification and docs only

Runtime reuse:

- no new ingress, planner lane, replay lane, owner family, payload field, or
  prose family was added in this campaign
- the session used the existing engine/replay helpers plus the new
  `NamedRouteValidationMatrixWorkup.scala` test/tooling runner to refresh the
  current control, candidate pool, and blocker pack on the same branch

Current hard-fail set added by the broader slice:

- `fake_route_chain`
- `redundant_intermediate_node`
- `chain_only_on_nonbest_branch`
- `untouched_sector_escape`
- `posture_inflation`
- `heavy_piece_route_shell`
- `replay_reinflation`
- `whole_game_wrapper_leak`

Exact-board validation status:

- positive control FEN:
  `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24`
- current 2026-04-02 local Stockfish depth-18 defended-branch line after the
  played move `a3b4`:
  `a7a5 b4a5 c6a5 f3e5 f7f6 e5d3 a5c4`
- exact truth outcome:
  the best-defense line proves the exact `a5` intermediate node, but it also
  shows that downstream `c4` is still reachable, so the old
  `a5 -> c4` denial wording had to be removed from the live owner claim
- root-best refresh:
  the same helper-backed matrix rerun returned `c3b4`, not `a3b4`, as root
  best on the original control FEN at both depth 18 and depth 24, so the
  earlier `a3b4` root-best pass could not be re-used as a stable closeout
  anchor
- second-survivor screen:
  `K09C`, `K09G`, `K09H`, `B21A`, and `Rubinstein-Duras 1911` were all rerun
  locally from exact FEN with root-best plus after-trigger best-defense
  checks. `K09C`, `K09G`, and `K09H` failed the root-best gate; `B21A` stayed
  distinctively target-fixing rather than route-chain; `Rubinstein-Duras`
  stayed release-heavy (`queen_infiltration`, `rook_lift`,
  `exchange_sac_release`) and therefore failed B5/B7 distinctness
- exact negative / near-miss pack now includes:
  `fake_route_chain`, `redundant_intermediate_node`,
  `chain_only_on_nonbest_branch`, `untouched_sector_escape`,
  `posture_inflation`, `surface_reinflation`,
  `heavy_piece_route_shell`, and `engine_pv_paraphrase`

Self-critique result:

- the first implementation draft was too permissive about what could count as
  an intermediate node
- the runtime now restricts intermediate-node pickup to
  `route_node / intermediate_node` resource classes, so a second reroute square
  cannot silently become the `intermediate` witness

Verification status:

- implementation status:
  no runtime widening was added in this closeout pass; verification stayed on
  exact-FEN engine/replay screening, blocker reruns, targeted tests, and doc
  reconciliation
- adversarial-review status:
  no second survivor found, and the current control stayed root-best-unstable
  under the helper-backed rerun
- deferred residue:
  the downstream `a5 -> c4` denial remains backend-only / deferred, the
  intermediate `a5` detour is now backend-visible only rather than
  planner-owned live truth, and the whole B6 route-chain family remains
  one-example heavy at A-track closeout

Current verdict:

- `A-track plateau reached`
- reason:
  the closeout campaign reproduced the backend after-trigger `a5` detour but
  did not reproduce `a3b4` as root best under the helper-backed rerun, and no
  second independent exact-FEN survivor survived root-best, same-branch,
  release-suppression, and family-distinctness screening. Keep broader
  route-chain, replay reuse, and posture/phase widening deferred

## CTH-B7 — Transition-Adjacent Strategic Route Design Baseline

Purpose:
decide whether `B7 = transition-adjacent strategic route` can open as a
separate truth-owning family, and if so, define the narrowest certifiable claim
before any runtime work is allowed.

Current status:
design baseline ready only. No B7 runtime slice, new payload field, new prose
family, or new owner lane is opened by this section.

### Problem framing

- B7 is not generic simplification, not a whole-ending verdict, and not a
  permission slip to reuse the existing `endgame_transition_translator`
  ownership lane
- B6 proves a bounded route-network bind inside the same current technical
  task; B7 is distinct only if the same defended branch proves that the current
  task has narrowed into a different and more technical next task after the
  simplification trigger
- the certifiable unit is:
  one bounded same-branch task shift, not
  `this is now a good endgame`, `the ending is winning`, or
  `simplification is favorable in general`
- exact board truth comes first:
  if the trigger, best defense, continuation, and release suppression are not
  all anchored to the same concrete branch, the claim stays closed

### Candidate claim map

| Candidate | Expected positive scope | Product value | Main overclaim risk | Main falsification burden | Expected surface risk | Strongest reusable signals | B7 verdict |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `one favorable trade corridor` | one exact exchange corridor that removes a blocker on the best-defense branch | medium | favorable trade language inflates into `better ending` or `nearly winning` rhetoric | pin the exact trigger, exact best-defense reply, and show that the corridor survives without branch stitching | `high`; simplification phrasing inflates quickly on replay surfaces | `bestDefenseBranchKey`, same-branch reuse, `convert_reply_multipv`, `moveOrderFragility`, `futureSnapshot.planBlockersRemoved` | `deferred` as a trigger-only subcomponent, not the first truth-owning claim |
| `one bounded task-shift after forced simplification` | one same-branch shift from the current task into one narrower technical task such as king activation, target fixation, or file occupation | highest | `fake_transition`, `endgame_verdict_inflation`, and `engine_pv_paraphrase` | prove trigger, shifted task, bounded continuation, and surviving release suppression on the same defended branch | `high`, but containable if it stays planner `WhyThis` only | B1 route persistence, B6 branch identity grammar, `futureSnapshot.planPrereqsMet`, `planBlockersRemoved`, `targetsDelta`, `convert_reply_multipv` | `likely coverable next`, but still closed until the proof burden below is met |
| `one conversion-adjacent technical route` | one bounded route into a narrower conversion task after the simplification | medium-high | a local route becomes a broader conversion thesis or endgame verdict | separate the route from the ending verdict and require that the defender's best branch still leaves one bounded next task rather than a narrative of the whole ending | `very high`; Chronicle / Bookmaker can turn it into whole-game meaning fast | B1 conversion persistence, same-branch keys, `futureSnapshot`, existing conversion motifs | `deferred` until the narrower task-shift slice survives criticism |
| `one king-activation / target-fixation route` | one exact post-simplification technical task involving king path, target fixation, or file occupation | medium | king activity or target language drifts into race, fortress, or theorem-level endgame prose | require exact current-task removal plus exact shifted-task witness on the same branch; keep theoretical outcome support-only | `very high`; overlaps too easily with `EndgameTransition` replay prose | `futureSnapshot.targetsDelta`, `planPrereqsMet`, `EndgameFeature` as support only, same-branch keys | `deferred` as a downstream specialization, not the first slice |
| `whole ending thesis` | whole ending verdict or long-run winning-route narration | superficially high | immediate overclaim and cross-surface reinflation | impossible to keep bounded enough for first B7 work | `maximal` | none that are safe for a first slice | `unsafe` |
| `none / keep runtime closed` | design baseline only until one candidate survives exact-board criticism | near-term trust value is highest | reuse bias can still pressure a premature rollout | require one exact-FEN positive control plus near-miss negatives before any slice opens | lowest | existing B1 / B5b / B6 fail-closed gates, cross-surface regression harness | `current operating rule` until the corpus burden is satisfied |

### Backend-only contract draft

No new public payload/schema is authorized. If B7b is attempted later, the
first contract must remain backend-only on the existing experiment/certification
lane.

| Field | Purpose | Strongest reusable source or guard |
| --- | --- | --- |
| `claimScope` | keep the claim local to one transition-adjacent task shift rather than the whole ending verdict | backend-only scope gating from B1 / B6 |
| `currentTask` | name the exact pre-trigger technical task that the move is trying to resolve | existing route / entry / target evidence plus current plan hypothesis |
| `shiftedTask` | name the exact post-trigger technical task that becomes narrower on the same branch | `futureSnapshot.planPrereqsMet`, `targetsDelta`, bounded continuation motifs |
| `transitionTrigger` | pin the exact move or exchange that is supposed to cause the shift | best-reply PV plus `moveOrderFragility` |
| `tradeCorridor` | record the exact exchange corridor when a trade is part of the trigger | `convert_reply_multipv`, direct reply probes, best-defense branch reuse |
| `routePersistence` | prove that the shifted task remains live after best defense | B1 route persistence and B6 route continuity grammar |
| `bestDefenseBranchKey` | keep trigger, task shift, and continuation on one defended branch | current branch-key reuse |
| `sameDefendedBranch` | explicit stitched-proof blocker for the entire bundle | existing `stitched_defended_branch` / same-branch failures |
| `continuationBound` | cap the proof at one bounded next task and bounded continuation window | `futureSnapshot.planBlockersRemoved`, `planPrereqsMet`, bounded continuation tokens |
| `technicalGain` | record the exact technical gain, such as entry sealed, target fixed, king path clarified, or file occupied | `targetsDelta`, prevented-plan grammar, route/file-entry evidence |
| `releaseRisksRemaining` | enumerate heavy-piece release, tactical release, fortress hold, or king-race ambiguity that still survives | B5b release inventory, fortress/static-hold heuristics, reply probes |
| `transitionMirageRisk` | mark when the move only improves locally without changing the task | explicit B7 `fake_transition` guard |
| `tradeIllusionRisk` | mark when the favorable trade exists only on non-best or cooperative branches | best-defense proof plus `moveOrderFragility` |
| `endgameInflationRisk` | keep local task shift from becoming an ending verdict | separation from `endgameTransitionClaim` and replay surfaces |
| `counterplayReinflationRisk` | block stronger surfaces from reviving unsupported ending claims | current replay-closure pattern and evidence-backed-plan filter |
| `confidence` | keep the contract auditable and fail-closed | existing `PlanEvidenceEvaluator.ClaimCertification` grammar |
| `evidenceSources` | list exact FEN / probe / future snapshot inputs | current certification source lists |

Candidate-specific rules:

- `one favorable trade corridor`
  - may populate `transitionTrigger` and `tradeCorridor`, but may not surface a
    positive B7 claim by itself
- `one bounded task-shift after forced simplification`
  - requires non-empty `currentTask`, `shiftedTask`, `continuationBound`, and
    `technicalGain` on the same defended branch
- `one conversion-adjacent technical route`
  - may reuse the same contract only after the narrower task-shift slice proves
    that it does not inflate into whole-ending narration
- `one king-activation / target-fixation route`
  - keeps king-race, theoretical outcome, Lucena/Philidor-style theorem labels,
    and generic ending verdicts support-only

### Criticism matrix

| Axis | Why it is dangerous | Current reusable guard | Required future B7 guard | Required difficult fixture shape |
| --- | --- | --- | --- | --- |
| `fake_transition` | a move-local improvement gets narrated as a task shift even though the same technical task still governs the position | B6 route continuity and current same-branch grammar | require `currentTask != shiftedTask` plus same-branch proof that the old blocker actually clears | exact B6 route-chain near-miss: `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24`, move `a3b4`, best line `a7a5 b4a5 c6a5 f3e5 f7f6 e5d3 a5c4` |
| `favorable_trade_illusion` | the trade looks available, but best defense keeps another holding route or avoids the corridor entirely | direct reply probes, `moveOrderFragility`, heavy-piece negative-first review | require `tradeCorridor` to stay on the best-defense branch and fail if the defender can sidestep the trade without losing the same bounded task | queen-on heavy-piece exact FEN from B5b such as `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24` |
| `technical_route_only_on_nonbest_branch` | the attractive technical route exists only on a side PV, not on the best reply | existing branch-key reuse and stitched-branch failures | require trigger, shifted task, and continuation to share one `bestDefenseBranchKey` | one exact FEN with best defense refusing the simplification while a secondary PV cooperates; harness must capture both PV identities explicitly |
| `endgame_verdict_inflation` | bounded local truth mutates into `good ending`, `winning ending`, or theorem-level prose | existing separation between planner claims and `endgameTransition` support claims | hard-cap `claimScope`, keep B7 off `endgame_transition_translator`, and close `WhatChanged` / replay until separately audited | pure-endgame exact FENs such as Lucena / Philidor / Vancura patterns from `endgame_goldset_v2_patterns.jsonl` |
| `holdable_simplification` | simplification happens, but the defender still reaches a static shell with no real progress route conceded | B1 / B3 / B4 fortress-risk and static-hold heuristics | require `technicalGain` to show more than liquidation: one blocker removed and one bounded next task opened | transition near-miss exact FEN `8/5pk1/3b2p1/3P4/1P3P2/6P1/5BK1/8 w - - 0 45` from `DualAxisBindBroadValidationTest.scala` |
| `branch_stitched_transition` | trigger, task shift, continuation, and release suppression are borrowed from different branches | current same-branch failures and B6 branch identity hardening | require one branch token across trigger, best defense, continuation, and release inventory; no first-move fallback | one exact FEN where PV1 gives the trade, PV2 gives the continuation, and PV3 removes the release; certification must fail |
| `heavy_piece_release_survives` | a transition-looking trade leaves queen/rook counterplay or forcing checks alive | B5b heavy-piece negative validation pack | require `releaseRisksRemaining` to be empty or the slice fails closed | B5b heavy-piece negatives including `2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1PN2/PPQ2P1P/2RR2K1 w - - 0 24` and related shells |
| `route_network_restatement_only` | a B6 route denial gets relabeled as `transition` without any true task change | B6 exact contract and planner surface narrowing | require explicit `currentTask` / `shiftedTask` delta and fail if the evidence only proves route denial | the B6 route-chain FEN above, where the only surviving wider witness is the backend-visible `a5` detour |
| `surface_reinflation` | planner-local truth is revived by Chronicle / Bookmaker / Active as a stronger ending thesis | evidence-backed-plan filter and existing replay closures for B6 | clone the replay-closure posture: planner `WhyThis` only first, keep `WhatChanged`, Chronicle, Bookmaker, Active, and whole-game closed | one planner-local exact FEN whose replay wrapper tries to say `better ending` or `technical win` |
| `engine_pv_paraphrase` | the model paraphrases an engine liquidation line as if it were a stable strategic route | current `pv_restatement_only` criticism from B1/B6 families | require measurable task-shift fields and bounded continuation, not just a neat PV | one exact FEN where the PV simplifies but `planBlockersRemoved` / `planPrereqsMet` do not prove a new bounded task |

### Broad validation charter

Phase matrix:

| Phase | Status | Why |
| --- | --- | --- |
| `late middlegame` | `likely coverable next` | best environment for a same-branch simplification trigger without theorem-level endgame inflation |
| `heavy-piece middlegame` | `likely unsafe` | B5b still shows too many surviving release and reinflation seams |
| `transition / endgame-adjacent` | `likely deferred` | product value is real, but task-shift ambiguity is too close to ending rhetoric |
| `pure endgame` | `likely unsafe` | the work would collapse into theorem labels or existing endgame translator ownership |

Eval posture matrix:

| Eval posture | Status | Why |
| --- | --- | --- |
| `clearly better` | `likely coverable next` | strongest posture for a bounded task-shift claim with no whole-ending inflation |
| `slightly better` | `likely deferred` | too easy to turn a practical edge into a fake transition |
| `equal / unclear` | `likely unsafe` | positive transition ownership would overstate the truth |
| `defending side` | `likely unsafe` | this would create a false survival/hold owner lane |

Texture matrix:

| Texture | Status | Why |
| --- | --- | --- |
| `favorable trade corridor` | `likely deferred` | useful trigger evidence, but not safe as a standalone B7 claim |
| `bounded task-shift` | `likely coverable next` | narrowest slice that is actually distinct from B6 |
| `conversion-adjacent technical route` | `likely deferred` | route language broadens too fast without a proven narrower task shift |
| `king-activation / target-fixation route` | `likely deferred` | technically meaningful, but too close to endgame translator rhetoric for a first slice |
| `whole ending thesis` | `likely unsafe` | out of scope for B7 |
| `route-network restatement` | `likely unsafe` | that is B6 restated, not B7 |
| `heavy-piece fake transition` | `likely unsafe` | release density remains too high |
| `static simplification shell` | `likely unsafe` | simplification without progress is a mandatory negative corpus family |

Surface matrix:

| Surface | Status | Why |
| --- | --- | --- |
| `planner-owned WhyThis` | `likely coverable next` | narrowest owner path and easiest place to keep the claim bounded |
| `planner-owned WhatChanged` | `likely deferred` | it overstates task-shift truth too easily on first contact |
| `Bookmaker` | `likely deferred` | replay phrasing is reusable later, but it is too inflation-prone for first-slice B7 |
| `Chronicle` | `likely deferred` | same replay risk; too easy to turn the claim into the ending's full story |
| `Active` | `likely deferred` | parity and non-reinflation can be tested, but positive ownership should stay closed |
| `whole-game / wrap-up / support reuse` | `likely unsafe` | strongest reinflation seam and explicitly out of scope for first B7 work |

### Strict proof structure

#### Exact-board corpus burden

- B7b may not open from verbal sketches or synthetic-only probes
- minimum corpus gate:
  one exact-FEN positive control plus at least five exact-FEN near-miss
  negatives
- every fixture must record the same schema:
  `fen`, `currentMove`, `exactBestDefenseLine`, `bestDefenseBranchKey`,
  `sameBranchPersistenceWitness`, `taskShiftWitness`, `continuationWitness`,
  `releaseSuppressionWitness`, and `failReason` or `successReason`
- current exact-board seed pack is intentionally negative-heavy and therefore
  insufficient for rollout on its own:
  - B6 near-miss route chain:
    `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24`
  - B5b heavy-piece trade/release shells:
    `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24`,
    `2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1PN2/PPQ2P1P/2RR2K1 w - - 0 24`,
    and related exact negatives
  - transition near-miss:
    `8/5pk1/3b2p1/3P4/1P3P2/6P1/5BK1/8 w - - 0 45`
  - pure-endgame inflation blockers:
    Lucena / Philidor / Vancura exact FENs from the goldset resources
- current gap:
  no exact-FEN positive control is certified yet, so B7a may define the burden
  but may not authorize B7b rollout

#### Engine-backed proof burden

- root move agreement alone is not enough
- a future B7 candidate must pin at least:
  - the `transitionTrigger`
  - the defender's best reply
  - one bounded continuation window that still exhibits the same shifted task
- acceptable proof is:
  exact move sequence plus bounded continuation witness on the same branch
- unacceptable proof is:
  vague eval drift such as
  `good ending eventually`, `technically winning`, or a naked PV paraphrase

#### Branch identity burden

- B7 is invalid unless the following live on one identical defended branch:
  - `transitionTrigger`
  - `tradeCorridor` if present
  - `shiftedTask`
  - `continuationBound`
  - `releaseSuppressionWitness`
- no first-move-only branch identity fallback is allowed
- if the trigger comes from PV1, the shifted task from PV2, or the release
  suppression from PV3, the result is `branch_stitched_transition` and fails
  closed

#### Surface containment burden

- first B7 slice, if it ever opens, starts at planner-owned `WhyThis` only
- `planner-owned WhatChanged` stays closed for the first slice
- Chronicle, Bookmaker, Active, and whole-game / wrap-up reuse stay fail-closed
- B7 may not reactivate or piggyback on the existing
  `endgame_transition_translator` owner lane
- support-only signals such as `endgameTransitionClaim` and endgame pattern
  features may assist criticism, but they may not certify the B7 claim

#### Downgrade / defer protocol

- if exact position, exact move, or exact best-defense line is missing:
  `deferred`
- if the trigger exists but the task shift is not proven:
  support-only or exact factual fallback only, never a positive B7 claim
- if same-branch persistence is missing:
  `deferred` with `branch_stitched_transition`
- if release risks remain live:
  `deferred` or negative-first only
- if the only truthful statement is
  `the move simplifies` or `the move forces one trade`:
  keep that exact factual fallback and do not open B7 wording

### Mandatory self-critique pass

Candidate attacked:

- `one bounded task-shift after forced simplification`

Why the first draft was vulnerable:

- it treated `trade corridor + continuation visible` as almost enough
- it assumed that a future technical task could be inferred from a pretty PV
- it left `WhatChanged` and transition-adjacent phases too loosely open

Nasty cases used against that draft:

| Case | Exact anchor | Why the first draft was fooled | Correction forced into the baseline |
| --- | --- | --- | --- |
| `fake_transition` | `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24` after `a3b4` | the line looked like simplification into conversion, but the exact best-defense branch still reaches `...a5c4`, so the task did not actually shift | `tradeCorridor` was demoted to trigger-only evidence and explicit `currentTask` / `shiftedTask` became mandatory |
| `route_network_restatement_only` | the same B6 exact FEN | B6 truth was easy to rename as B7 because `conversion route stabilizes` already existed in support text | B7 now requires a proved task delta, not more route vocabulary |
| `heavy_piece_release_survives` | `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24` | favorable trade language hid surviving queen/rook release resources | `releaseRisksRemaining` became a mandatory field and heavy-piece middlegame was tightened to `likely unsafe` for first-slice work |
| `holdable_simplification` | `8/5pk1/3b2p1/3P4/1P3P2/6P1/5BK1/8 w - - 0 45` | restriction-first wording made a static shell look like a real transition | `technicalGain` must now show one blocker removed plus one bounded next task opened |
| `endgame_verdict_inflation` | Lucena / Philidor / Vancura exact FENs | existing endgame translator prose made it too easy to leap from local shift to theorem-level ending story | pure endgame stayed `likely unsafe`, and B7 is now explicitly barred from `endgame_transition_translator` |
| `branch_stitched_transition` | one exact FEN with trigger on PV1 and continuation on PV2 | the first draft did not force one shared branch token across the whole bundle | `sameDefendedBranch` and branch-wide identity are now mandatory |

Before/after deltas caused by self-critique:

- before:
  `trade corridor` looked like a possible standalone first slice
- after:
  `trade corridor` is trigger evidence only
- before:
  `heavy-piece middlegame` looked merely deferred
- after:
  it is `likely unsafe` for first B7 rollout
- before:
  `planner-owned WhatChanged` was loosely plausible on the first slice
- after:
  it is explicitly closed for first-slice B7
- before:
  the corpus burden could have been satisfied by attractive near-miss lines
- after:
  one exact-FEN positive control is a hard precondition for B7b

### Recommended B7b first slice

Recommended next slice, if and only if the corpus burden is first satisfied:

- `one bounded task-shift after forced simplification on the same defended branch`

Why this slice is first:

- it is the narrowest candidate that is genuinely distinct from B6
- it reuses the strongest existing signals:
  same-branch identity, bounded continuation, prevented-plan grammar, and
  future-snapshot task witnesses
- it can stay on the existing architecture and planner `WhyThis` lane without
  opening a new public payload, owner family, or replay surface
- the broader alternatives either collapse into B6 restatement or inflate into
  whole-ending rhetoric too easily

Hard preconditions before any B7b implementation:

- retain one exact-FEN positive control for this slice
- add at least five exact-FEN near-miss negatives across
  `fake_transition`, `trade_illusion`, `holdable_simplification`,
  `branch_stitched_transition`, and `endgame_verdict_inflation`
- keep the first slice on clearly-better late middlegame only
- keep the first admissible task pair closed to
  `secure_favorable_simplification -> pressure_fixed_weak_complex`;
  `occupy_support_file_pressure` may appear only as a bounded same-branch
  witness, never as the owned shifted task
- keep the only positive surface at planner-owned `WhyThis`
- keep `WhatChanged`, Chronicle, Bookmaker, Active, and whole-game reuse
  fail-closed

### Exact-FEN corpus recon status

The 2026-04-02 corpus workup materially strengthened the nasty pack, and the
follow-up late-middlegame hunt did find one reviewable B7b branch candidate,
but the implementation-time review blocked it on the current runtime path:

- K09B exact-branch review candidate:
  `r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13`
  still survives local Stockfish review with root-best trigger `Nxe6` (`d4e6`)
  and best-defense reply `...fxe6`; the same defended branch keeps the bounded
  continuation
  `...Rd1 ... Bf4 ... Qb6 ...`
  alive and therefore remains a valid exact-board review control
- B7b review outcome on the current pipeline:
  blocked, not passed; the live runtime still classifies the position as
  `phase=Opening` / `taskMode=ExplainTactics`, produces no evidence-backed
  strategic main plan, no `preventedPlansNow`, no named-route/local-file
  surface, no `main_bundle`, and no planner-owned `WhyThis` owner. The only
  surviving move-linked text is the line-scoped preview
  `Line: a) Nxe6 fxe6 Rad1.`, which means a rollout here would collapse into
  `engine_pv_paraphrase`
- B7 containment consequence:
  runtime remains closed because the current audited architecture does not yet
  carry a measurable `currentTask -> shiftedTask` proof for K09B without either
  new private/backend-only materialization or a weaker prose paraphrase; a new
  owner lane remains unapproved and out of scope

#### 2026-04-02 narrow redesign diagnosis

The exact-board blocker is no longer discovery. K09B remains useful as a
positive control, owner-path blocker, and narrow redesign smoke candidate. The
canonical blocker is now split into two linked absences:

- no owner-admissible backend-only carrier/materialization for a bounded
  task-shift proof on the live planner path
- no closed task vocabulary that can encode a real
  `currentTask -> shiftedTask` handoff without collapsing into B1 conversion,
  B6 route-bind language, or generic `better ending` rhetoric

Task vocabulary gap map:

| proposed task id | bounded chess meaning | why existing vocabulary is insufficient | B7 first-slice verdict |
| --- | --- | --- | --- |
| `secure_favorable_simplification` | finish or force the favorable simplification under live middlegame tension | existing exchange / simplification labels name the family, not the `from` side of a handoff | admissible as `currentTask` only |
| `pressure_fixed_weak_complex` | after best defense fixes a new weakness complex, the bounded job becomes pressuring that exact complex on the same branch | current weakness/fixation labels describe a target, but not that the target is newly created and replaces the old task | admissible as `shiftedTask`; first-slice core |
| `occupy_support_file_pressure` | file occupation/redeployment that helps the pressure continue | by itself it reads like B6 file/route truth or a move list, not a distinct owned task | support-only witness |
| `force_target_fixation` | fix or hold a target without a simplification handoff | valid family, but no task delta; it can exist without any B7 transition | inadmissible first slice |
| `activate_king_route` | king activation in the technical phase | too close to endgame translator rhetoric and whole-ending inflation | inadmissible first slice |
| `technical_conversion_pressure` | generic technical conversion after simplification | theme/conversion name only; fails `currentTask != shiftedTask` discipline | inadmissible first slice |
| `route_bind_restatement` | restate a denied route/reroute as the post-trade job | if the truth is route loss, the family is B6 already | fail-close to B6 |
| `generic_endgame_improvement` | say the move reaches a better ending or easier phase | not measurable enough to be a bounded task identity | fail-close |

Backend-only task-shift contract draft:

| field | role |
| --- | --- |
| `claimScope` | freeze the slice to `task_shift_after_forced_simplification` |
| `currentTask` | closed backend-only task ID for the job the trigger is finishing |
| `shiftedTask` | closed backend-only task ID for the narrower job after best defense |
| `transitionTrigger` | exact move-local trigger plus defended reply that creates the handoff |
| `bestDefenseBranchKey` | tie the proof to one exact defended branch |
| `sameDefendedBranch` | require trigger, shift witness, and continuation to stay on that branch |
| `continuationBound` | require one bounded continuation window after best defense |
| `taskShiftWitness` | concrete evidence that the owned job changed, not merely the PV continued |
| `releaseSuppressionWitness` | inventory of release resources that did not become the real story |
| `distinctFromExistingFamily` | explicit `not_b1_conversion` / `not_b6_route_bind` tags |
| `transitionMirageRisk` | fail `task_label_only` / fake-transition shells |
| `routeRestatementRisk` | fail B6 relabels |
| `endgameInflationRisk` | fail `good ending` / theorem-level drift |
| `surfaceContainmentMarker` | keep the slice planner `WhyThis` only |
| `claimCertification` | reuse measurable quantifier/provenance/ontology gates |
| `confidence` | bounded audit score only, never a release override |
| `evidenceSources` | exact probe / branch / future-snapshot provenance |

Owner-path admissibility map:

| stage | current state | failure point | required minimal redesign |
| --- | --- | --- | --- |
| ingress | exact-board trigger, best defense, and same-branch continuation already exist for K09B | not the blocker anymore | none at producer discovery |
| carrier/materialization | current carriers expose theme/subplan/line data only | no private record of `currentTask`, `shiftedTask`, or their witnesses | add a private/backend-only task-shift certification beside existing builder certifications |
| planner admissibility | `buildWhyThisPlan` can admit `mainBundle`, `quietIntent`, or `namedRouteNetworkSurface` only | no B7 planner source exists, so K09B falls to `missing_move_owner` | add a private task-shift planner input/claim; if it cannot stay private and planner-local, keep B7 deferred |
| replay/whole-game containment | current safe fallback is the line preview only | any broader surface would reinflate into route or ending rhetoric | require `surfaceContainmentMarker = planner_why_this_only`; keep `WhatChanged`, Chronicle, Bookmaker, Active, and whole-game fail-closed |

Canonical admissibility conclusion:

- as-is, the current architecture still blocks B7
- a future B7 slice does not yet prove that a new owner lane is necessary, but
  it does require new private/backend-only materialization on the existing
  architecture
- if the redesign pressures public carriers such as `PlanHypothesis`,
  `StrategicPlanExperiment`, `NarrativeContext`, or response payloads, B7 must
  remain deferred

#### Task vocabulary freeze

The B7 task model is now frozen as a closed backend-only ID set. Free-text
`currentTask` / `shiftedTask` labels are forbidden.

| `task_id` | `chess_meaning` | `required_signals` | `same_branch_requirement` | `why_existing_theme_is_not_enough` | `first_slice_status` |
| --- | --- | --- | --- | --- | --- |
| `secure_favorable_simplification` | finish or force the favorable simplification while live middlegame tension still exists | exact trigger move, exact best-defense reply, favorable-exchange evidence, no tactical refutation | trigger and defended reply must anchor the same branch that later carries the post-trade task witness | existing exchange / simplification themes name a family, not the `from` side of a bounded handoff | `admissible` as `currentTask` only |
| `pressure_fixed_weak_complex` | after best defense fixes a new weakness complex, the bounded job becomes pressuring that exact complex | post-recapture fixed-target witness, same-branch continuation, target/file pressure witness, no release takeover | the fixed weakness, continuation, and pressure witness must all remain on the same defended branch | current weakness/fixation themes describe a target family, but not that the target was created by the trigger and replaced the old job | `admissible` as `shiftedTask` only |
| `occupy_support_file_pressure` | occupy or reinforce a file to help the pressure continue | file pressure witness, continuation witness, bounded support line | same branch only, and only as a corroborating continuation witness | by itself it collapses into B6 file/route truth or a raw move list | `witness_only` |
| `force_target_fixation` | induce or maintain a fixed target without a simplification handoff | fixed-target witness, future target persistence, no task-removal proof | if used at all, it must still survive same-branch criticism, but that does not make it B7 | this can be true without any task shift and therefore does not define a B7 delta | `forbidden` |
| `activate_king_route` | activate the king in the technical phase after simplification | exact endgame geometry, king path, race/fortress exclusion | would require same-branch proof plus pure-endgame safety, which first-slice B7 does not have | existing endgame families and translator hints are too broad and phase-heavy | `forbidden` |
| `technical_conversion_pressure` | generic technical conversion after simplification | conversion summary, generic follow-through, eval drift | same-branch persistence alone is insufficient because the job identity stays blurry | this is a theme label, not a bounded `to` task | `forbidden` |
| `route_bind_restatement` | restate a denied route or reroute as the post-trade task | route-edge/reroute denial evidence | same-branch route proof would still belong to B6, not B7 | if the owned truth is route loss, the family is B6 already | `forbidden` |
| `generic_endgame_improvement` | say the move reaches a better ending or easier technical phase | phase change, eval drift, endgame hint | same-branch replay does not rescue it because the task identity is still unbounded | this is exactly the overbroad rhetoric B7 is meant to avoid | `forbidden` |

Frozen first-slice admissible pair:

- `secure_favorable_simplification -> pressure_fixed_weak_complex`

Frozen exclusions:

- `occupy_support_file_pressure` may not become the owned `shiftedTask`
- any pair where `currentTask == shiftedTask` is an automatic fail
- any pair whose distinctness depends only on theme/subplan renaming is an
  automatic fail

#### Backend-only contract freeze

The contract remains backend-only, planner-local, and serialization-free. It is
frozen as a design target only:

```scala
private[analysis] enum TaskShiftTaskId:
  case SecureFavorableSimplification
  case PressureFixedWeakComplex
  case OccupySupportFilePressure
  case ForceTargetFixation
  case ActivateKingRoute
  case TechnicalConversionPressure
  case RouteBindRestatement
  case GenericEndgameImprovement

private[analysis] final case class TaskShiftContract(
    claimScope: String,
    currentTask: TaskShiftTaskId,
    shiftedTask: TaskShiftTaskId,
    transitionTrigger: String,
    bestDefenseBranchKey: Option[String],
    sameDefendedBranch: Boolean,
    continuationBound: Boolean,
    taskShiftWitness: List[String],
    releaseSuppressionWitness: List[String],
    distinctFromExistingFamily: List[String],
    transitionMirageRisk: String,
    routeRestatementRisk: String,
    endgameInflationRisk: String,
    surfaceContainmentMarker: String,
    claimCertification: PlanEvidenceEvaluator.ClaimCertification,
    confidence: Double,
    evidenceSources: List[String]
)
```

Freeze rules:

- `currentTask` and `shiftedTask` must come from the closed enum only; free text
  is forbidden
- first slice admits exactly one owned pair:
  `SecureFavorableSimplification -> PressureFixedWeakComplex`
- `OccupySupportFilePressure` may appear only inside `taskShiftWitness`
- `distinctFromExistingFamily` must explicitly carry
  `not_b1_conversion` and `not_b6_route_bind`
- `surfaceContainmentMarker` must be planner-local and replay-blocking
- generic `OwnerFamily.MoveDelta` is not a sufficient contract by itself; it
  carries no bounded task pair and therefore cannot certify B7

Intentionally excluded from the frozen contract:

- any new public payload/schema field
- any new owner lane
- any public evidence-tier upgrade
- any replay / whole-game reuse grant
- any free-form task prose

#### Family distinction freeze

| `family` | `what truth it owns` | `required evidence shape` | `what it must never claim` | `common relabel drift` | `fail-close rule` |
| --- | --- | --- | --- | --- | --- |
| `B1 restricted-defense conversion` | the move keeps the same conversion job and the defender's real resources are compressed on the surviving branch | direct defended reply, resource compression, same-branch persistence, positive future snapshot | it must never claim that the job itself changed if the truth is still conversion follow-through | a favorable trade is described as a new task instead of the same conversion continuing | if the same job still governs the position, keep it in B1 |
| `B6 named route-network bind` | the move denies one bounded route network or reroute on the same defended branch | route nodes/edges, reroute denial, same-branch continuity, bounded continuation | it must never claim a new post-trade technical job beyond route loss | a reroute denial is renamed as a transition task | if the owned truth is route loss or reroute denial, keep it in B6 |
| `B7 task shift after forced simplification` | the move finishes one bounded job and, after best defense, a different bounded job now governs the same defended branch | exact trigger, exact best defense, same-branch continuation, certified task pair, release suppression, explicit non-B1/non-B6 distinctness | it must never claim `good ending`, generic conversion, generic move improvement, or route-bind truth | B1 conversion relabel, B6 route-bind relabel, or PV paraphrase dressed as a task | if the pair is not distinct and measurable, defer or fail-close out of B7 |

#### Containment freeze

Planner-local containment is mandatory.

Same as B6:

- use a private/backend-only contract
- admit only through planner-local `WhyThis`
- require an explicit containment marker

Stricter than B6:

- no replay reuse review is open
- no Chronicle / Bookmaker / Active positive carryover is open
- no `WhatChanged` ownership is open
- no theme-thread or whole-game wrapper reuse is open

Frozen marker:

- `surfaceContainmentMarker = planner_why_this_only`

Why generic `MoveDelta` is unsafe:

- it can admit a move-local explanation without proving a bounded task pair
- replay consumers can then reconstruct stronger transition language from the
  same generic owner category
- it does not distinguish B7 from quiet improvement, B1 follow-through, or B6
  route truth

#### Downgrade / defer protocol freeze

| condition | mandatory outcome |
| --- | --- |
| exact branch exists, but no task-shift carrier/materialization exists | `deferred` |
| carrier exists, but `currentTask` / `shiftedTask` are free text, equal, or outside the closed set | `deferred` |
| task pair exists, but distinctness relies only on theme/subplan renaming | `support_only` or `deferred`; never B7-owned |
| `taskShiftWitness` is just the PV continuation under a new label | `exact factual fallback` |
| `routeRestatementRisk` remains high or the truth is route loss/reroute denial | fail-close to `B6` or exact factual fallback |
| `transitionMirageRisk` remains high and the move only simplifies without changing the bounded job | `deferred` |
| `releaseSuppressionWitness` fails because heavy-piece or tactical release remains the real story | `deferred` |
| `endgameInflationRisk` remains high or the claim becomes `better ending` rhetoric | `exact factual fallback` |
| any design pressure appears for new public schema, new owner lane, or replay-positive reuse | keep `B7` closed; no materialization beyond private freeze |

#### 2026-04-02 contract validation pass

The frozen contract was then checked against the current exact-FEN control plus
near-miss / blocker rows. The point of this pass was not to reopen B7, but to
prove that the frozen contract fails closed under pressure.

Validation matrix:

| `candidate_id` | `trigger` | `best_defense` | `same_branch` | `currentTask fit` | `shiftedTask fit` | `continuationBound` | `releaseSuppression` | `distinct_from_B1` | `distinct_from_B6` | `containment_risk` | `verdict` | `fail_reason` |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `K09B` | `Nxe6` | `...fxe6` | `yes` | `yes` | `yes`, narrowly | `yes` | narrow pass only; later queen/rook activity exists but does not yet displace the target-led reading | `yes` | `yes` | `high` | survives as `positive control only` | frozen pair is reviewable, but runtime still has no carrier/materialization and therefore no planner owner |
| `K09A` | `Qb3` | `...Qd7` | `yes` | preparatory only | `no` | `yes` | insufficient | `no` | `yes` | `medium` | fail-close | setup move, not the shift-owning trigger |
| `K09F` | `Nxe6` | `...fxe6` | `yes` | `yes` | `no` | `yes` | `no` | `no` | blurred | `high` | fail-close | holdable simplification shell; no crisp post-trade task |
| `MI2` | root best `Kh1` | `...Bc5` | `yes` | `no` | `no` | `yes` | `no` | `no` | `yes` | `medium-high` | fail-close | root branch is not a simplification handoff; generic conversion/cashing-in dominates |
| `MI3` | root best `Nd5` | `...Nxd5` | `yes` | `no` | `no` | `yes` | `no` | `no` | `yes` | `medium-high` | fail-close | wrong family; high-edge coordination/cashing-in, not B7 |
| `B21` | `Nd2` | `...Na6` | `yes` | `no` | `no` | `yes` | not the issue | `yes` | `yes` | `low-medium` | fail-close | target fixation only; no simplification handoff |
| `B21A` | `Nd2` | `...Na6` | `yes` | `no` | `no` | `yes` | not the issue | `yes` | `yes` | `low-medium` | fail-close | same target-fixing family persists; still no handoff |
| `b5_queen_infiltration_shell` | no clean B7 trigger | `...Qd7` line keeps heavy-piece activity live | `yes` | `no` | `no` | `yes` | `no` | n/a | n/a | `very high` | fail-close | heavy-piece / exchange-sac release remains the real story |
| `b6_route_chain_near_miss` | `b4` | `...a5` | `yes` | `no` | `no` | `yes` | n/a | `yes` | `no` | `high` | fail-close to `B6` | route/reroute denial, not a new task pair |
| `lucena_blocker` | `Rb1` | `...Kd6` | `yes` | `no` | `no` | `yes` | n/a | `yes` | `yes` | `very high` | fail-close | pure-endgame inflation blocker; cannot become B7 |

Task-pair proof outcome:

- `SecureFavorableSimplification -> PressureFixedWeakComplex` remains the only
  admissible owned pair because it is the only checked neighborhood where the
  move-local job can plausibly change from securing the favorable trade to
  pressuring a newly fixed weakness complex on the same defended branch
- `OccupySupportFilePressure` remains witness-only because the B6 route blocker
  proves that file occupation language alone is too easy to relabel as route or
  file truth
- `TechnicalConversionPressure` remains forbidden because MI2 / MI3 show that
  generic conversion/cashing-in swallows the distinction and collapses back into
  B1-style or non-B7 language
- `RouteBindRestatement` remains forbidden because the B6 near-miss still fails
  closed cleanly there and should not be admitted into B7 under a new name

Failure taxonomy validation:

| failure type | validated by |
| --- | --- |
| `task_label_only` | `B21`, `B21A` |
| `current_shifted_not_distinct` | `K09F` |
| `route_bind_relabel` | `b6_route_chain_near_miss` |
| `conversion_relabel` | `MI2`, `MI3` |
| `line_preview_disguised_as_task` | current K09B runtime output still collapses to `Line: a) Nxe6 fxe6 Rad1.` plus `missing_move_owner` |
| `surface_overhang` | K09B survives only under planner-only containment; replay reuse remains explicitly closed |
| `schema_pressure` | the current planner/build path still lacks any legal public carrier; any public-carrier pressure would break the frozen contract |
| `heavy_piece_leakage` | `b5_queen_infiltration_shell` |
| `endgame_inflation` | `lucena_blocker` |
| `candidate_overfitting` | only `K09B` survives as a positive control, so reopen must demand more than that single row |

Admissibility decision memo:

- the frozen contract is strict enough to reject all current near-miss and
  blocker rows for different reasons
- the contract does **not** become runtime-open from this validation pass
- `K09B` remains both:
  - the lone positive control for the frozen pair
  - the owner-path blocker because the runtime still cannot materialize the
    contract
- because `candidate_overfitting` remains live, future B7 reopen now requires
  more than the existing freeze:
  - implement private/backend-only materialization only
  - keep planner `WhyThis` only containment
  - rerun this same matrix with no new leakage
  - and add at least one second non-`K09B` exact-FEN survivor for the same
    frozen pair before any runtime reopening review
- 2026-04-02 second-survivor hunt result:
  exact-FEN reruns over `K09C`, `K09G`, `K09H`, `MI1`, `MI4`, `K19`, plus the
  required blocker pack found no second non-`K09B` late-middlegame survivor
  for `SecureFavorableSimplification -> PressureFixedWeakComplex`; the pair
  therefore remains one-row overfit on the current corpus and B7 defer is
  strengthened rather than loosened
- 2026-04-02 parallel hunt addendum:
  `K09I` plus three web-recovered exact-FEN historical leads
  (`Rubinstein-Duras 1911`, `Shirov-Kinsman 1990`, `Alekhine-Vidmar 1936`)
  were also rerun locally and all failed before the frozen pair could even
  activate because none kept a root-best simplification trigger on the
  defended branch

- broad same-branch candidate found:
  `8/5pk1/3b2p1/3P4/1P3P2/6P1/5BK1/8 w - - 0 45` with trigger `Bc5`
  (`f2c5`) and best-defense prefix `...Bb8 b5 ... g5 ... d6 ...`; this is a
  real exact-branch transition-adjacent route, but it lives in a pure-endgame
  cell and is still unsafe for the first B7 slice
- strongest late-middlegame near misses remain:
  `2bqr1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12`
  with trigger `Nxc6` (`d4c6`) stayed `non-best-branch only`,
  `r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PP2PPBP/2RQ1RK1 w - - 4 13`
  stayed too preparatory, and
  `2rqr1k1/pp2bpp1/2n1bn1p/3p4/3N4/P1N1B1P1/1P2PPBP/2RQ1RK1 w - - 1 14`
  still behaved like a holdable simplification shell
- exact-FEN nasty negatives are now anchored across:
  B6 route-network restatement on
  `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24`,
  B5b heavy-piece release shells, and Lucena / Philidor / Vancura pure-endgame
  inflation blockers
- fixture-only transition prose that does not survive exact-board replay stays
  outside the B7 corpus; current blockers include the `open_file_fight` and
  `exchange_sacrifice` bookmaker fixtures, whose recorded move text does not
  replay cleanly from the listed FEN

Net effect:

- the nasty-case burden is materially stronger
- one `clearly-better late middlegame` branch remains useful as a review
  control, but it did not survive implementation-time runtime review
- runtime stays closed because the current pipeline still cannot own that
  branch as a bounded task shift without slipping into PV paraphrase

Current verdict:

- `CTH-B7a baseline ready`
- reason:
  B7 is now narrowly defined as a same-branch bounded task-shift family with an
  explicit falsification burden. The 2026-04-02 narrow redesign pass fixed the
  remaining blocker as owner-path admissibility plus task vocabulary, not exact
  board discovery alone. The backend-only planner-local contract freeze is now
  canonical, and no implementation or surface reuse is authorized unless a
  future attempt stays inside that frozen envelope and then survives both the
  nasty-case pack and runtime-owner review

Recommended next move:

- `B7 defer`
- reason:
  the backend-only contract/vocabulary/containment baseline is now the
  canonical freeze, the validation pass held it against current near-miss
  pressure, and any future reopening must first stay entirely inside that
  frozen envelope **and** produce a second non-`K09B` survivor for the same
  pair before runtime review

## CTH-B8 — Slight-Edge Local Squeeze First-Slice Validation

Purpose:
decide whether `B8 = slight-edge local squeeze` can open as the next
truth-owning family, and if so, define the narrowest certifiable first slice
before any runtime work is allowed.

Current status:
`B8a` design + corpus validation complete only.
No runtime slice, new payload field, new owner lane, or new prose family is
opened by this section.

Current narrowest plausible unit:
- one same-branch `sector-local active-plan collapse`
- meaning:
  before the move, the defender still has multiple active plan families in one
  bounded sector; after the move, the root-best defended branch would need to
  show those families collapsing into at most one holding-only residue for a
  bounded continuation

Exact-FEN validation outcome on the 2026-04-02 pass:
- local QC seeds with direct PGN/FEN recovery, such as
  `Qa5` on `2b3k1/p3qp1p/6p1/4P3/3p4/P2B3P/3Q1PP1/6K1 w - - 0 28`
  and `Ke7` on `3R1k2/5ppp/ppb2n2/2r5/P1p5/2N1P1N1/1PP3PP/6K1 b - - 0 27`,
  stayed inside true slight-edge territory but did not survive the B8 burden:
  the defender still kept multiple active plan families on the root-best
  branch rather than degrading to holding-only
- the best local fixture-style control,
  `rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 0 9`
  after root-best `Nd2`, behaved like target-fixing / prophylaxis with plural
  defender resources still live
- external exact-FEN leads already recoverable inside local tooling, such as
  `r4bk1/1r2n1p1/p2p1p1p/1q1Pp3/2N1P3/RP1QBPP1/6KP/R7 w - - 0 27`
  and
  `8/1prrk1p1/p1p1ppb1/P1P3p1/2BPP3/4KPP1/1R5P/1R6 w - - 0 1`,
  either equalized, lived in clearly-better pressure rather than slight-edge
  squeeze, or kept too much tactical/release content to count as holding-only
- repo-local blocker reruns continued to fail closed exactly where expected:
  B6 route-chain restatement on
  `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24`,
  heavy-piece release on
  `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24`,
  B7 simplification relabel on
  `r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13`,
  and pure-endgame inflation on
  `2K5/2P1k3/8/8/8/8/7r/R7 w - - 0 1`

Contract consequence:
- the existing deferred `slightEdgeLocalSqueeze` draft remains directionally
  right, but any future reopening also needs explicit
  `preMoveActivePlans`, `postMoveActivePlans`, `collapsedPlanFamilies`, and
  `holdingOnlyFamilies` enumeration on the exact best-defense branch
- even with that narrower contract, current corpus evidence is insufficient for
  positive reopening

Current verdict:
- `B8 still too fuzzy / more corpus work needed`
- reason:
  no two independent exact-FEN survivors currently prove a measurable
  same-branch local active-plan collapse in true slight-edge territory

Reference workup:
- [b8a_slight_edge_local_squeeze_20260402.md](C:/Codes/CondensedChess/lila-docker/repos/lila/tmp/b8a_slight_edge_local_squeeze_20260402.md)

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

It stays planner-first except when exact-factual fallback is explicitly taken,
and that fallback is now fail-closed: an empty author-question branch may not
rebuild decision/meta/close-candidate shell prose as a substitute owner.

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

CTH-A Step 4 now introduces a file-local authority slice in code.

Coverage is still intentionally partial, but it now spans the main mixed-fragment
sites that were blocking rollout:

- opening-precedent comparison families
- annotation hint fragments
- annotation severity-tail suffix
- context beat fragments
- wrap-up fragments
- whole-game support projection
- outline validator leak cleanup

Other lexicon/template families still remain structurally risky until they are
tagged at their own fragment or call-site boundary.

| Asset | Current use site | Authority class | Primary risk | Track 5 eligibility |
| --- | --- | --- | --- | --- |
| `momentBlockLead` | `CommentaryEngine.scala` | `render_only` | `out_of_scene_generalization` | No |
| `hybridBridge` | `CommentaryEngine.scala` | `render_only` | `unsupported_generalization` | No |
| `getOpening` | `NarrativeOutlineBuilder.scala` | `unsafe_as_truth` | `out_of_scene_generalization` | No |
| `getOpeningReference` | `BookStyleRenderer.scala`, `NarrativeOutlineBuilder.scala` | `support_only` | `support_only_overreach` | Evidence-only, not lesson core |
| `getThreatStatement` | `NarrativeOutlineBuilder.scala` | `support_only` | `overclaim_strength` | Maybe later, only with tighter anchor gate |
| `getPlanStatement` | `NarrativeOutlineBuilder.scala` | `support_only` | `unsupported_generalization` | No now |
| `getPawnPlayStatement` | `NarrativeOutlineBuilder.scala` | `support_only` | `out_of_scene_generalization` | Maybe later, with tighter board anchor gate |
| `getFactStatement` | `NarrativeOutlineBuilder.scala` | `support_only` | `false_positive_claim` if detached from board state | Maybe, but fact-only |
| `getPreventedPlanStatement` | `NarrativeOutlineBuilder.scala` | `support_only` | `support_only_overreach` | No now |
| `getCompensationStatement` | `NarrativeOutlineBuilder.scala` | `support_only` | `overclaim_strength` | No |
| `getAnnotationDifficultyHint` | `NarrativeOutlineBuilder.scala` | `support_only` | `out_of_scene_generalization` | No |
| `getAnnotationTagHint` | `NarrativeOutlineBuilder.scala` | `support_only` | `unsupported_generalization` | No |
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

### 2A. Step 4 / 5 Status

Current CTH-A Step 4 / 5 slice is file-local, enforcement-first, and now broad
enough to cover Context / WrapUp / whole-game support reuse plus first-pass
generalization admissibility.

It keeps authority internal to `NarrativeOutlineBuilder.scala`,
`CommentaryEngine.scala`, and `NarrativeOutlineValidator.scala` without adding a
public payload/schema field.

The live runtime now has:

- `NarrativeOutlineBuilder.scala`
  - opening precedent comparison now keeps comparison body, ordinary summary,
    and shared-lesson phrasing separate until the final beat stitch
  - opening precedent comparison families now carry file-local authority:
    ordinary summary/body are `support_only`, while `sharedLesson` is
    `unsafe_as_lesson`
  - main-move annotation prose now keeps terminal/tag/difficulty hint
    selection separate from the final text join, and severity-tail projection
    also stays separate until the last annotation stitch
  - annotation hint fragments now carry `support_only`, while
    `severityTail` carries `unsafe_as_truth`
  - final beat stitch now releases only authority-admissible fragments, so
    `unsafe_as_lesson` and `unsafe_as_truth` suffixes do not ship in released
    outline prose from this slice
  - Step 5 now adds fragment-local admissibility:
    generalized `support_only` fragments release only when they are still
    grounded by move link, scene scope, evidence, planner ownership, or
    contract-consistent support
  - context assembly now keeps board anchor / motif / opening / flow / plan /
    fact / pawn / opponent / meta families as authority-bearing fragments until
    the final beat stitch
  - the generalized `getOpening` lead is retained as `unsafe_as_truth`, while
    the surviving Context carriers are released as `support_only`
  - wrap-up assembly now keeps planner consequence, practical verdict, and
    compensation coda separate until final beat assembly, with all released
    wrap-up prose staying `support_only`
- `CommentaryEngine.scala`
  - whole-game support reuse now passes raw digest / strategy-pack carriers
    through a local authority-bearing projection before whole-game anchor
    normalization, so verified payoff anchors stay distinct from projected
    support-only carriers
  - helper labels such as `continuity:` or `alignment intent:` are stripped
    before reuse, but support carriers still remain `support_only` rather than
    becoming truth owners
  - Step 5 now treats `support_only` whole-game carriers as non-admissible
    decisive-wrapper seeds; shift/payoff wrappers must come from verified payoff
    anchors or equivalent structured proof such as directional targets, move
    refs, or piece routes
`NarrativeOutlineValidator.scala`
  - now performs one common authority leak cleanup pass before duplicate /
    evidence / must-mention gates
  - sentences containing `Shared lesson:` are dropped, and raw helper-label
    prefixes such as `continuity:` or `alignment intent:` are stripped if they
    survive builder release
  - Step 5 adds a narrow generalization admissibility backup:
    risky generalization sentence families are dropped unless the beat is
    OpeningTheory, branch-scoped, or otherwise anchor-bearing

This is intentionally limited:

- no new public payload/schema
- authority enum/class stays private/file-local
- no Track 5 lesson extraction change yet
- untagged lexicon/template families still rely on existing planner/truth
  gates rather than the new authority slice
- builder/validator admissibility is still narrow:
  it covers current authority-tagged families and known risky sentence
  templates, not every future generalized family
- whole-game decisive wrappers now require contract-owned or structured proof,
  but non-wrapper support surfaces still carry `support_only` residual text

### 2B. Step 6 Harness Status

Current CTH-A Step 6 slice lives entirely in `src/test` and does not add new
runtime semantics or payload fields.

The current harness is
`modules/llm/src/test/scala/lila/llm/analysis/CrossSurfaceTrustRegressionHarnessTest.scala`.
It reuses existing runtime entrypoints only:

- `BookmakerLiveCompressionPolicy.buildSlotsOrFallback`
- `GameChronicleCompressionPolicy.renderWithTrace`
- `ActiveStrategicCoachingBriefBuilder.selectPlannerSurface` plus
  `buildDeterministicNote`

The current fixture bundle covers:

- planner-owned positive
- exact-factual fallback
- quiet-support residual
- generalized-support negative
- Active diagnostic residual

The harness compares and regressions on:

- `owner_divergence`
- `strength_divergence`
- `scope_divergence`
- `support_only_overreach`
- `fallback_rewrite_regression`

Accepted residuals encoded in the harness:

- Bookmaker and Chronicle must agree on canonical planner owner for the
  positive and defensive-stop fixtures
- Bookmaker may keep the documented bounded quiet-support fallback lift on the
  exact-factual fallback lane, while Chronicle must keep that same fixture
  claim-only
- Active remains diagnostic-only:
  omission is still allowed on the residual fixtures, but any future surfaced
  Active owner must not outrun the canonical Bookmaker / Chronicle owner

### 2C. Step 7 Negative Fixture Pack Status

Current CTH-A Step 7 slice also lives in `src/test` and adds no new runtime
semantics or payload/schema fields.

The Step 6 harness has been extended in
`modules/llm/src/test/scala/lila/llm/analysis/CrossSurfaceTrustRegressionHarnessTest.scala`
with a large negative fixture pack and an explicit expectation table.

Current pack size:

- 16 negative fixtures total
- 7 covered risk classes

Covered risk classes:

- `support_only_overreach`
- `fallback_rewrite_regression`
- `unsupported_generalization`
- `out_of_scene_generalization`
- `owner_or_strength_overclaim`
- `whole_game_support_promotion`
- `active_diagnostic_residual_misuse`

Current surface coverage inside the pack:

- Bookmaker
- Chronicle
- Active
- validator-level release cleanup
- whole-game support / wrapper assembly

Accepted residuals encoded in the pack:

- Bookmaker may keep the documented one-line quiet-support residual on the
  exact-factual fallback lane
- Chronicle must keep the matching fallback lane claim-only
- Active may omit on diagnostic-only lanes; omission remains accepted
- OpeningTheory may keep the currently admitted anchored
  `Across these branches, ...` pattern, while the same sentence family stays
  forbidden in unanchored `Context`

What remains outside the pack:

- Track 5 lesson extraction is still deferred
- frontend support implication cap is still a separate follow-up slice
- not every future generalized lexicon family is enumerated yet; the pack
  freezes the currently known forbidden families and residual lanes

CTH-A close-readiness:

- the current pack is large enough to hold the existing trust-hardening line
  against false positive / overclaim / unsupported generalization regressions
- it is still a regression harness, not signoff that every trust residual is
  eliminated

### 2D. CTH-A Close Status

CTH-A is now `core complete`.

That close status means the current tranche has already shipped:

- fallback truth rewrite hardening
- Active contract alignment
- lexicon authority tagging
- generalization admissibility gating
- cross-surface trust regression watch
- a large negative fixture corpus

CTH-A is therefore no longer the active implementation frontier.

Current posture:

- maintenance-only
- extend the Step 6 / Step 7 harness when new residuals are accepted
- keep updating this document when trust-relevant behavior changes

Residual-only follow-up items:

- frontend support implication cap still remains a separate slice
- Active remains diagnostic-only rather than signoff-clean
- non-wrapper support surfaces still carry bounded `support_only` text
- validator backup cleanup is still narrow-family rather than universal
- compensation / wrap-up families remain intentionally conservative

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
  prose; the no-question outline branch also falls straight to exact factual
  text or omission instead of restitching decision/meta/close-candidate prose

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

1. CTH-A is now core complete, but only as a trust-hardening tranche; it does
   not make lesson extraction automatically admissible
2. support carriers can still surface bounded generalized implication on
   non-wrapper lanes, and frontend support implication cap remains a follow-up
   residual
3. Active remains diagnostic-only even after the contract-alignment slice, so
   lesson extraction would still inherit a non-signoff surface
4. the Step 6 / Step 7 harness freezes current forbidden families and accepted
   residuals, but it is still a regression line rather than proof that every
   future lesson-like family is safe

Without additional guards, lesson extraction would promote fallback or
support-only material into portable lesson truth.

#### Required guards before Track 5

| Required guard | Why it is necessary | First files / paths to touch |
| --- | --- | --- |
| frontend support implication cap | lesson cannot sit on top of frontend-visible support implication that still outruns current trust authority | `bookmaker.ts`, `narrativeView.ts`, `responsePayload.ts` |
| Active signoff cleanup | lesson divergence would still leak through a diagnostic-only surface even after contract alignment | `ActiveStrategicCoachingBriefBuilder.scala`, `LlmApi.scala`, `ActiveStrategicNoteValidator.scala` |
| broader authority rollout maintenance | currently tagged fragment families are enough for CTH-A, but future lesson work still needs broader authority ownership over any newly reused generalized family | `NarrativeLexicon.scala`, `NarrativeOutlineBuilder.scala`, `CommentaryEngine.scala` |
| lesson-specific admissibility layer | only move-linked, scene-scoped, owner-bound, evidence-bound lesson candidates may survive even after the current generalization gate | `NarrativeOutlineBuilder.scala`, `NarrativeLexicon.scala` |

## CTH-A Priority List

1. CTH-A core slices
   - complete:
     fallback truth rewrite hardening, Active contract alignment, authority
     tagging, generalization admissibility, cross-surface regression harness,
     and the first large negative fixture pack
2. extend the Step 6 / Step 7 regression corpus when new residuals are
   accepted or when a surface gains a stronger owner
3. frontend support implication cap
4. opening/endgame replay claim hardening as a fixed regression target
5. support-only overreach audit across runtime and frontend surfaces
6. Active diagnostic-only cleanup if a later workstream wants signoff-clean
   Active behavior
7. CTH-B reconnaissance/design once trust-maintenance work is no longer the
   active blocker

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

Current maintenance rule:

- treat compile/test green as `implementation-green`, not final closure
- require adversarial review before `close-ready`
- if a fresh runtime change touches a bounded B1 / B2 / B3 seam, reopen only
  the touched slice and extend its negative fixtures first
