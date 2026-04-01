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
design/research only; do not treat as the next automatic implementation step
while CTH-A residual maintenance still owns trust-signoff questions.

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
  defending-posture cells beyond the B1b coverage pack

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
  at least one future snapshot remains positive after the conversion step
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
- `routePersistence`:
  whether the conversion route still holds after best defense and future
  snapshots
- `failsIf`:
  explicit failure reasons such as `cooperative_defense`,
  `hidden_defensive_resource`, `route_persistence_missing`,
  `insufficient_counterplay_suppression`, `move_order_fragility`,
  `pv_restatement_only`, and `local_to_global_overreach`
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
| `pv_restatement` | paraphrasing engine PV can masquerade as strategic discovery | plan evidence already requires bounded probe support | certification fails when direct reply-multipv evidence is missing or when the claim is only a restated line |
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

#### CTH-B1b — Restricted-Defense Conversion Broad Validation / Corpus Expansion

Purpose:
stress the existing B1 certificate against a broader late-middlegame /
transition corpus and against real commentary surfaces without widening runtime
semantics.

Status:
validation-only expansion completed. No new runtime helper, public payload
field, or prose family was added. The work stays in test/tooling and reuses the
existing planner/build/replay path.

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
    none in the B1b targeted matrix
- evaluation posture:
  - `covered`:
    clearly winning technical conversion, slightly better but
    non-conversion-ready fail-closed, and equal/unclear fail-closed
  - `deferred`:
    defending / materially worse posture
  - `failed`:
    none in the B1b targeted matrix
- texture:
  - `covered`:
    technical simplification, counterplay suppression, quiet improvement before
    conversion, tactical-looking but fail/defer, and move-order-sensitive
    conversion
  - `deferred`:
    broad quiet squeeze certification, free-tempo routes, whole-position route
    search, fortress-break generalization
  - `failed`:
    none in the B1b targeted matrix
- criticism class:
  - `covered`:
    `cooperative_defense`, `pv_restatement`, `hidden_defensive_resource`,
    `move_order_fragility`, `local_to_global_overreach`,
    `surface_reinflation`
  - `deferred`:
    none inside the targeted B1b criticism pack
  - `failed`:
    none in the B1b targeted matrix
- surface:
  - `covered`:
    Bookmaker, Chronicle, Active, and whole-game/support reuse
  - `deferred`:
    none inside the targeted B1b surface pack
  - `failed`:
    none in the B1b targeted matrix

Actual surface validation status:

- certified positive restricted-defense conversion currently survives as a
  planner-owned `WhyThis` technical conversion on Bookmaker / Chronicle /
  Active, not as a stronger `WhyNow` timing claim
- fragile or uncertified restricted-defense conversion ideas fail closed to
  exact factual fallback and stay out of planner-owned Chronicle / Active /
  Bookmaker primaries
- whole-game / wrap-up reuse keeps failed restricted-defense conversion support
  as support-only and does not revive it as `decisiveShift` or `payoff`

Failure inventory from B1b:

- `runtime bug`:
  none found in the targeted B1b pack
- `overclaim risk`:
  no new overclaim path observed; uncertified cases stayed fail-closed across
  planner and replay surfaces
- `insufficient validation`:
  opening, early middlegame, pure technical endgame, and defending-posture
  cells are still outside the B1b validation pack
- `fixture gap`:
  no new targeted gap inside the B1b matrix; broader CTH-B cells remain future
  work rather than B1b fixture debt
- `acceptable residual`:
  certified positives currently surface as bounded `WhyThis` conversion prose
  rather than preserving a `WhyNow` timing answer; this is acceptable for B1b
  because it reduces claim strength rather than inflating it

B1 close-readiness after B1b:

- the narrow restricted-defense conversion certificate now has broad validation
  over its intended late-middlegame / transition cells and real-surface reuse
- B1 should still be read as a bounded certificate, not as general winning-route
  discovery

Explicitly deferred after B1/B1b:

- middlegame-wide tactical conversion discovery
- broad quiet squeeze certification outside this restricted-defense slice
- free-tempo and whole-position winning-tree search
- fortress-break generalization
- technical endgame route certificates beyond bounded conversion followthrough
- opening / early middlegame / pure endgame / defending-posture validation
  completion
- Track 5 lesson extraction

### CTH-B2a — No-Counterplay / Quiet Squeeze Certification Design Baseline

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
- `surface_reinflation_risk`

#### B2 criticism matrix

| Axis | Why risky | Current guard / reuse | Future B2 guard | Required fixture shape |
| --- | --- | --- | --- | --- |
| `cooperative_defense` | the route works only if the defender stays passive | B1 best-defense stability, reply probes, alternative-dominance checks | no certification unless the squeeze survives the concrete best defense | squeeze-looking clamp where passive defense loses but best defense keeps activity |
| `pv_restatement` | strategic discovery collapses into paraphrased engine PV | direct-reply requirement in B1, claim quantifier/modality checks | require restriction evidence plus falsification burden, not line prettification | quiet PV with no measured restriction/resource loss |
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

#### Recommended B2b first implementation slice

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
  bounded `WhyThis` / `WhatChanged` only; no global `no-counterplay`, no
  whole-game wrapper, no broader squeeze shell

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

- `CTH-B2a baseline ready`
- reason:
  B2 now has a canonical definition draft, falsification burden, criticism
  matrix, broad validation charter, and narrow B2b entry slice without
  widening runtime semantics

### CTH-B2b — Late-Middlegame Prophylactic Clamp / Named-Break Suppression Certification

Purpose:
implement the narrowest B2 runtime slice:
bounded certification that a quiet restriction-first move suppresses one named
freeing break or entry route in late middlegame and keeps that suppression
alive against best defense.

Status:
implemented as a backend-only certification helper plus planner/replay
containment. No public payload/schema field or new prose family was added.

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
  route-denial or long-term-restraint validation, concrete best defense,
  future-snapshot persistence, no live tactical release, and no move-order
  fragility
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
  fail when restriction validation and concrete best defense are missing
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

- planner-owned `WhyThis` parity is green for certified B2b positives
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

- `CTH-B2b slice passed`
- reason:
  late-middlegame named-break / entry-axis suppression is now certified only in
  the narrow bounded slice, and every broader squeeze/no-counterplay surface
  remains fail-closed

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
