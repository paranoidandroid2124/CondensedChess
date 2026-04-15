# Strategic Object Rewrite Roadmap

This document is the execution roadmap for the rewrite governed by
`StrategicObjectModel.md`.

It is intentionally demolition-first.

This roadmap follows the original design-memo phase numbering.

- `Phase 0` through `Phase 2` are preparatory boundary-setting phases.
- demolition and skeleton layout are recorded as completed rewrite checkpoints,
  not as separate long-term semantic phases.
- the first active rebuild phase after demolition was `Phase 3. Primitive
  Extraction Layer`; the live rewrite frontier now sits in `Phase 7` below.

## Rewrite Principles

- legacy topology must not be preserved by default
- compile break is acceptable during demolition
- old topology tests must not force old module resurrection
- fixture corpus is more important than legacy harness code
- truth spine survives; topology scaffolding does not

## Current Checkpoint

The rewrite has already completed the following checkpoints:

- authority shift to `StrategicObjectModel.md`
- red-spine extraction and fixture-bank preservation
- strategic-object model definition
- demolition of the legacy commentary semantic topology
- source-tree skeleton layout for the new architecture
- `Phase 3. Primitive Extraction Layer`
- `Phase 4. StrategicObjectSynthesizer`
  - pass 1 board-direct first-order objectization
  - pass 2 graph-derived first-order objectization
- strategic-object hardening and exact-board calibration
- `Phase 5` Tier 1 direct-delta-owner opening
- `Phase 5` pass 1 reinforcement
  - strict move-local witness
  - typed delta preservation through certification/planner
  - family-aware comparative witness and metric profile
- `Phase 5` Tier 1 provisional hardening / promotion audit
  - `KingSafetyShell`
  - `DevelopmentCoordinationState`
  - `PieceRoleFitness`
  - `SpaceClamp`
  - `CounterplayAxis`
  - `RestrictionShell`
  - `MobilityCage`
  - `RedeploymentRoute`
  - all eight remain `Provisional`
- `Phase 6. ClaimCertification Rewrite`
  - certification is now delta-aware rather than readiness-only
  - `Stable` claims require scope-specific typed delta burden before they may
    stay `Certified`
  - shallow-but-typed stable deltas now downgrade to `SupportOnly`, and
    insufficient exact-board support now downgrades to `Deferred`
  - `Provisional` typed deltas remain `SupportOnly`
- `Phase 7. Planner Rewrite`
  - question admission matrix is now typed-delta-native
  - `WhyNow` is now a separate timing-sensitive certified lane
  - pre-sanitization explanation trace and tail-risk gate are landed on the
    test/research boundary

The current active implementation frontier is therefore now just beyond the
Phase 7 planner/eval spine, not back inside `Phase 7`.

Near-term canonical sequence from the current checkpoint:

1. `Phase 8` must now prove a thin shell certified renderer on top of the
   landed planner/eval spine
2. the first vertical proving tranche is now closed on rerun:
   - `P8-R02` passed on 2026-04-11 through the packet-owned `d6` continuity
     witness and thin-shell rerun
   - `P8-R03` passed on 2026-04-11 through the packet-owned `k09b`
     primary-isolation rerun
   - `P8-R04` proved one bounded current-position coordination probe end-to-
     end in `passed_with_defer`
3. `P5-U01-trade-invariant-primary-simplification` is now passed:
   - the packet-owned primary-isolation boundary on `curated-exact:k09b` is
     closed
   - `P8-R03` shell rerun now also passes in `P9-R05`
4. the comparative near-miss follow-through is now landed:
   - `P6-B02-whatchanged-comparative-nearmiss-certification`
      - passed
   - `P7-Q03-whatchanged-comparative-demotion-matrix`
      - passed on 2026-04-10
      - `WhatChanged` primary now stays on `comparative_primary` only
   - `P7-E03-comparative-nearmiss-tail-risk`
      - passed on 2026-04-10; trace/tail-risk now expose comparative near-miss
        demotion and hard-fail `WhatChanged` primary leakage
5. `P9-A04b-d6-current-position-fixed-target-probe` is now passed:
   - the current-position fixed-target tranche now contains both the preserved
     B15A `c6` exact probe and the packet-owned `d6` exact probe on the same
     centralized boundary
   - the quiet positional salvage tranche on 2026-04-15 strengthens that same
     boundary only with two extra exact quiet `d6` descriptors plus one exact
     `RestrictionShell-white-center-d4-de` support descriptor:
     same-batch `45 / 360 / max8` raises `FixedTargetComplex`
     `WhatMattersHere / position_local` owner rows `0 -> 2` and
     `RestrictionShell` `WhatMattersHere / position_local` planner-support
     rows `0 -> 2`, while family/system aggregates stay flat and the quiet
     `d4` near-miss remains closed
   - the descriptor-bound quiet lane generalization tranche on 2026-04-15 is
     now closed as `descriptor-bound ceiling confirmed`:
     the smallest boundary-visible structural relaxation leaked
     `FixedTargetComplex` `WhatMattersHere / position_local` owner rows
     `2 -> 23` on same-batch `45 / max8`, while
     `RestrictionShell` planner-support rows stayed `2`; future promotion now
     requires a new exact-board positive pack or a richer certified target-
     cluster witness, not looser descriptor matching
   - `FixedTargetClusterWitness` passed on 2026-04-15 as that richer witness:
     `CanonicalStrategicObjectDeltaProjector` now lowers one typed
     `PositionLocal` fixed-target cluster witness
     (`focalTargetSquare`, `clusterSquares`, matching access / restriction /
     defender ids, disambiguation) onto the fixed-target owner and matched
     `RestrictionShell` support deltas, `ClaimCertification` preserves it, and
     `QuestionPlanner` attaches `RestrictionShell` `WhatMattersHere` support
     only when primary and support share that same witness rather than a
     common `currentPositionProbeKind`
   - same-batch `45 / 360 / max8` after the witness boundary raises
      `FixedTargetComplex` `WhatMattersHere / position_local` owner rows
      `2 -> 7` and `RestrictionShell` planner-support rows `2 -> 7`;
      non-target family aggregates stay flat, system
      `supportActivationCount` moves only `9 -> 14`, and the exact quiet `d4`
      near-miss plus the `f6` / `c5` / `e5` leak-bank corpus rows remain closed
   - `FixedTargetClusterWitness` access/corroboration de-stringification
     hardening passed on 2026-04-15: access lane and corroborating-route
     flavor now come from `AccessNetwork` profile / route geometry plus
     anchor/evidence footprint rather than `objectId` suffix parsing, and the
     same-batch rerun stays flat at `FixedTargetComplex` owners `7`,
     `RestrictionShell` support `7`, and system `supportActivationCount = 14`
     with no new family drift
   - `FixedTargetClusterWitness` defender-leg de-stringification hardening
     passed on 2026-04-15: `FixedTargetClusterWitnessBoundary` no longer uses
     `DefenderDependencyNetwork-` prefix shortcuts for defender admission.
     The same boundary now reconstructs the defender connector from the
     certified position-local support graph and its related fixed-target /
     restriction footprint, keeping `objectId` as witness bookkeeping only.
     The same-batch `45 / 360 / max8` rerun remains flat at
     `FixedTargetComplex` owners `7`, `RestrictionShell` support `7`, and
     system `supportActivationCount = 14`, with zero family drift, zero
     row-level admission drift, and the five witness-positive batch samples
     preserved while the quiet `d4` and `f6` / `c5` / `e5` leak-bank negatives
     stay closed
   - `PositionLocal` cluster witness projection passed on 2026-04-15:
     the current-position fixed-target truth boundary now lives on the
     projector side as a typed
     `StrategicPositionLocalWitness.FixedTargetCluster` attached to the owner
     and matched `RestrictionShell` support deltas under
     `StrategicDeltaProjection.PositionLocal`; `ClaimCertification` only
     preserves that witness, `QuestionPlanner` only consumes it,
     `CurrentPositionProbeSlice` now carries coordination only, the packet `d6`
     continuity lane reads a packet-isolated preserved-witness helper rather
     than a fixed-target descriptor table, and
     `FixedTargetClusterWitnessBoundary` is reduced to a preserved-witness
     reader rather than a reconstruction boundary. The same-batch
     `45 / 360 / max8` rerun stays flat at `FixedTargetComplex` owners `7`,
     `RestrictionShell` support `7`, system `supportActivationCount = 14`,
     `primaryActivationCount = 1444`, and `objectOnlyActivationCount = 522`,
     with family drift `0`, row-level admission drift `0`, the seven
     owner/support rows retained, and no reopen of the quiet `d4` or
     `f6` / `c5` / `e5` closed negatives
6. `P6-B01` is now passed:
   - exact closure: certification now owns one packet-bounded shared-target
     continuity witness for the `d6` lane across `WhatMattersHere`,
     `WhyThis`, and `WhatChanged`
   - `WhatChanged` comparative support no longer relies on planner-side
     salvage; only claims carrying that certified witness may attach, while
     near-miss, wrong-support, and preserved `c6` rows stay outside it
7. `P9-R05-blocked-slice-rerun` is now passed:
   - rerun closed `P8-R02` and `P8-R03` on 2026-04-11 using only landed
     `P5-U01` / `P6-B01` semantics
   - no new infra, doctrine widening, or legacy salvage was needed
8. only after the continuity closure should any new horizontal gate packets reopen
9. Tier 1 provisional move-local reopen audit is now closed as
   `CounterplayAxis = exact RestrictionShell support narrow-go`:
   all eight families remain `Provisional`, `CounterplayAxis` now has one
   centralized exact-board positive pack that yields same-batch
   `MoveLocal` `SupportOnly` / narrow planner-support rows without loosening
   certification, the official pack split is now `8 / 4 / 12` positive /
   frozen / negative, and the other seven provisional families remain
   `move_local` closed. Official capability authority is fixed by
   `CounterplayAxis.official-capability-pack.json`; official non-capability is
   fixed by `CounterplayAxis.frozen-out-of-scope-pack.json` and
   `CounterplayAxis.negative-pack.json`; reopen is forbidden unless a new
   exact positive evidence pack passes the same-batch `45 / 360 / max8` rerun
   with no broad loosen and no other family drift

Comparative-quality certification is now complete; `P6-A02` is resolved.

For live rewrite status, treat the phase sections below, especially `Phase 5`
onward, as authoritative. This checkpoint is only the top-level summary.

## Phase 0. Rewrite Boundary Declaration

Goals:

- establish `StrategicObjectModel.md` as the rewrite north-star
- mark legacy commentary docs as migration references only
- stop using legacy SSoT docs as design authority for new work

Outputs:

- canonical north-star doc
- updated contributor instructions

## Phase 1. Test Spine Extraction

Goals:

- classify current tests into:
  - red spine
  - fixture-bank candidates
  - legacy topology tests
- preserve only truth-critical tests

Keep:

- truth core
- catastrophic blockers
- probe validity
- small curated acceptance corpus

Delete or archive:

- stale goldens
- old parity suites
- topology-preserving planner/policy tests
- support/fallback topology tests
- replay/compression tests whose purpose is legacy structure preservation

This phase includes fixture-bank preservation. Useful exact cases must be
preserved even when the old harnesses are deleted.

Each preserved fixture should keep:

- FEN
- PGN move index when relevant
- positive or negative label
- expected phenomenon
- branch key when known

## Phase 2. Strategic Object Model Definition

Goals:

- define the 24 object basis families
- define relation operators
- define certificate schema
- define delta kinds

Outputs:

- object type spec
- certificate spec
- relation spec
- delta spec

This phase also records the keep/replace boundary:

- keep `L0` through `L2`
- replace `L3` through `L6`
- keep `L7` thin

## Completed Demolition Checkpoint

The following rewrite prerequisites are already complete and should not be
renumbered as future semantic phases:

- legacy semantic/planner/carrier topology demolition
- compile-red intermediate state acceptance during demolition
- minimal skeleton boundary in source form:
  - `RawPositionEvidence`
  - `StrategicObjectSynthesizer`
  - `StrategicObjectDeltaProjector`
  - `ClaimCertification`
  - `QuestionPlanner`
  - `Renderer`

The point of this checkpoint was to make old topology restoration harder than
new object-native construction.

## Documentation Hygiene

These authority markdown files are not free-form scratchpads.

For each phase, keep only:

- canonical scope
- current execution status
- gate or exit criteria
- calibrated family/readiness rules when they materially affect later phases

If intermediate notes accumulate during a phase, fold them back into the
canonical phase section and remove redundant narration.
New sessions should be able to read the phase sections directly without
recovering intent from stale progress notes.

## Phase 3. Primitive Extraction Layer

Goals:

- define primitive extraction from existing low-level evidence

Examples:

- target square
- break candidate
- access route
- diagonal lane seed
- lift corridor seed
- knight route seed
- redeployment path seed
- route contest seed
- exchange square
- defended resource
- hook contact seed
- tension contact seed
- counterplay resource seed
- release candidate

The key rule is primitive-first, not strategy-name-first.

Planner and claim code must not read raw motif piles directly once this phase
lands.

### Phase 3 Exit Criteria

Phase 3 is complete only if all of the following are true:

- primitive canonical types are fixed under stable role-based names
- primitive extraction reads only `L0` through `L2` evidence
- downstream object/planner/claim code no longer reads raw motif piles or
  legacy semantic helper ingress directly
- the same semantic meaning is not recomputed again downstream from raw feature
  piles once a primitive exists for it
- primitive names do not collide with mature object-family names
- primitive extraction preserves exact-board inputs for later counterplay,
  access-race, and timing-shaped deltas without reopening raw ingress
- preserved fixtures have primitive-backed expectation coverage
- each reopened primitive axis has fixture-bank coverage for exact positive,
  exact negative, contrastive asymmetric, and noisy near-miss rows
- Phase 3 contrastive coverage must preserve non-file route geometry,
  hook/lever contact, generic counterplay resources, and
  tension/release-maintain anchors without object synthesis
- only `PrimitiveExtractor.extract(RawPositionEvidence, MoveTruthFrame,
  DecisiveTruthContract): PrimitiveBank` may consume raw evidence
- forbidden raw or legacy ingress is mechanically fenced by source/signature
  tests
- the new path makes old semantic ingress harder to restore than the primitive
  path is to extend

Phase 3 fails if it degenerates into:

- a thin wrapper over raw feature maps
- a second semantic ingress running in parallel with old raw feature access
- premature object synthesis
- planner projection
- claim wording
- user-facing capability rollout

## Phase 4. StrategicObjectSynthesizer Implementation

Phase 4 must establish the full strategic-object vocabulary in code, even if
the first executable frontier is intentionally smaller.

Phase 4 also owns family hardening.
That means the object layer must add canonical generation contracts and a
readiness band per family rather than treating all 24 families as equally
delta-ready on day one.

Phase 4 also needs an exact-board calibration pass for high-risk graph-derived
families.
That pass is not a vocabulary reduction step.
It must:

- expand exact positive / contrastive / near-miss / broad-overlap corpus rows
  for high-risk families
- prove that bilateral presence, source self-reuse, shell shape, ordinary
  defense, or generic activity do not over-admit `PlanRace`,
  `TransitionBridge`, `FortressHoldingShell`, `DefenderDependencyNetwork`, or
  `InitiativeWindow`
- tighten contracts when exact-board nasty rows still leak
- keep readiness conservative unless the corpus, not verbal intuition, proves
  a broader release burden

The four-family frontier below is an implementation starting point, not the
intended richness ceiling of the object layer.

Required vocabulary richness for the first object layer:

- all 24 object families remain canonical
- board-direct first-order families should be objectizable directly from the
  primitive bank
- graph-derived first-order families must still live at object-layer state,
  not appear for the first time in delta/planner logic

Board-direct first-order families:

- `PawnStructureRegime`
- `KingSafetyShell`
- `DevelopmentCoordinationState`
- `PieceRoleFitness`
- `SpaceClamp`
- `CriticalSquareComplex`
- `FixedTargetComplex`
- `BreakAxis`
- `AccessNetwork`
- `CounterplayAxis`
- `RestrictionShell`
- `MobilityCage`
- `RedeploymentRoute`
- `PasserComplex`

Graph-derived first-order families:

- `DefenderDependencyNetwork`
- `TradeInvariant`
- `TensionState`
- `AttackScaffold`
- `MaterialInvestmentContract`
- `InitiativeWindow`
- `PlanRace`
- `TransitionBridge`
- `ConversionFunnel`
- `FortressHoldingShell`

Initial families to implement:

- `FixedTargetComplex`
- `CounterplayAxis`
- `TradeInvariant`
- `AccessNetwork`

Why:

- they map to already-known exact cases
- they stress both current-position and move-local semantics
- they test whether the new architecture can replace existing narrow slices

The first goal is object instantiation from exact known cases, not broad family
rollout. But this must happen inside a full-vocabulary schema, not a reduced
"four thin objects" schema.

Phase 4 is complete only if:

- the full object vocabulary remains explicit in code/schema
- the initial frontier is implemented as exact object contracts
- graph-derived and overlap-heavy families have canonical generation contracts
  that reject broad-overlap-only admission
- every family has a canonical readiness default orthogonal to strength
- later phases do not need to rediscover missing object richness from raw board
  state
- object schema is rich enough to carry owner, locus/sector, anchors,
  supporting primitives, supporting pieces, rival resources or objects,
  relations, state strength, readiness, horizon class, and evidence footprint

## Phase 5. StrategicObjectDeltaProjector Implementation

Goals:

- compute:
  - move-local delta
  - position-local probe
  - comparative delta

These deltas should be object-native, not family-helper-native.

Phase 5 works over the full 24-family object universe.
The conceptual north-star delta space is therefore `24 families x 3 scopes = 72
delta cells`.

But Phase 5 must not treat all 24 families as equal direct delta owners.

### Tier 1. Direct Delta Owners

Implementation should begin here.
These families are the primary Phase 5 owners and should be the first pass of
delta projection work:

1. `PawnStructureRegime`
2. `KingSafetyShell`
3. `DevelopmentCoordinationState`
4. `PieceRoleFitness`
5. `SpaceClamp`
6. `CriticalSquareComplex`
7. `FixedTargetComplex`
8. `BreakAxis`
9. `AccessNetwork`
10. `CounterplayAxis`
11. `RestrictionShell`
12. `MobilityCage`
13. `RedeploymentRoute`
14. `PasserComplex`

### Tier 2. Conditional Composite Delta Owners

These belong inside the same Phase 5 universe, but should be reopened only
after Tier 1 has a stable object-native delta path:

1. `TradeInvariant`
2. `TensionState`
3. `AttackScaffold`
4. `MaterialInvestmentContract`
5. `InitiativeWindow`
6. `ConversionFunnel`

`ConversionFunnel` salvage tranche on 2026-04-14 closes as `freeze` on the
current evidence pack:
the same-batch `45 / 360 / max8` audit produced 15 object-only rows, but none
survived exact-board review as a capability-positive continuation slice.
The canonical runtime boundary is now one centralized exact same-file corridor
(`FixedTargetComplex` entry ahead of own `PasserComplex`, file-lane
`AccessNetwork`, optional exact `RestrictionShell` gate, exact square witnesses
on both links). No delta, certification, or planner reopen follows from this
freeze result. Future reopening is forbidden unless a new exact positive pack
passes the same-batch rerun with no broad loosen and no other-family drift.

### Tier 3. Meta / Interpretive Delta Families

These still belong inside the Phase 5 working universe, but should normally
remain support-oriented or deferred unless later calibration justifies more:

1. `DefenderDependencyNetwork`
2. `PlanRace`
3. `TransitionBridge`
4. `FortressHoldingShell`

Readiness must constrain delta eligibility here:

- `Stable`
  - may project move-local, position-local, or comparative candidates
- `Provisional`
  - defaults to position-local and may stay support-only on comparative lanes
- `DeferredForDelta`
  - remains object-layer state only and must not emit player-facing delta

The target is to re-express old exact breakthroughs as object deltas rather
than family-specific helpers.

Phase 5 should therefore proceed in this order:

1. Tier 1 direct delta owners
2. Tier 2 conditional composite owners
3. Tier 3 meta / interpretive families

### Phase 5 Pass 1 Status

The current pass opens only the Tier 1 direct delta owners on the runtime
delta boundary.

Current canonical delta contract:

- `StrategicObjectDelta`
  - `objectId`, `family`, `owner`, `scope`
  - typed family `profile`
  - typed scope `projection`
  - `changedAnchors`
  - `supportingObjectIds`
  - `rivalObjectIds`
  - typed `evidenceRefs`

Current projector rules:

- Tier 1 only at the direct delta boundary
- `Stable`
  - may project all three scopes, but only through family-specific typed
    witnesses rather than scope placeholders
- `Provisional`
  - remains position-local first and may reopen comparative conservatively
- Tier 2 / Tier 3
  - remain object-layer state only on this pass; no direct delta emission

Phase 5 pass 1 reinforcement now also tightens the already-landed structure:

- `MoveLocal`
  - reopened only from a strict transition-aware witness assembled from the
    canonical played move trace plus surviving object-graph evidence; current
    object labeling alone is no longer sufficient
- `ClaimCertification` / `QuestionPlanner`
  - preserve and consume the admitted typed delta snapshot instead of
    collapsing back to scope-only release metadata
- `Comparative`
  - now carries family-aware comparative witness plus family-aware metric
    profile, and rival admission is tightened through a centralized family
    counterpart contract: only same-family or explicitly family-compatible
    counterparts with exact square/file/route/target or direct-rival witness
    survive, rather than broad overlap alone

Phase 5 Tier-1 provisional hardening and promotion audit on 2026-04-06 keeps
the remaining provisional direct-owner families
`KingSafetyShell`, `DevelopmentCoordinationState`, `PieceRoleFitness`,
`SpaceClamp`, `CounterplayAxis`, `RestrictionShell`, `MobilityCage`, and
`RedeploymentRoute` on the same runtime lane:

- all eight stay `Provisional` on this pass
- `MoveLocal` remains closed for all eight families
- `Comparative` may reopen only from exact-board rival/counterpart evidence
  plus typed family metrics; provisional rows now require at least three typed
  metrics, and the admitted counterpart witness must now carry explicit exact
  witness kinds instead of relying on same-sector or owner-isolated summary
- family generation/admission is tightened against false-positive pictures such
  as shell-pressure-only, activity-without-coordination, bad-piece-with-exits,
  single-file space pictures, counterplay seeds without an axis, restriction
  pictures without denial coverage, trapped-shape-only cages, and one-step
  maneuver mirages
- `SpaceClamp` now requires multi-file clamp evidence at generation time so a
  file-duel picture cannot rise as clamp ownership

Current verification baseline:

- exact-board Tier 1 delta fixture bank under
  `src/test/resources/strategic-object-corpus/delta-expectations.jsonl`
- per-family coverage for:
  - `exact`
  - `negative`
  - `contrastive`
  - `near_miss`
- provisional Tier 1 coverage also requires:
  - object rows: `nasty_negative`
  - delta rows: `nasty_negative`, `move_local_false_witness`,
    `comparative_false_rival`
- projector boundary tests now also fence primitive ingress after
  `StrategicObjectSynthesizer`

### Phase 5 Tier 1 Provisional Hardening Status

The current promotion-audit pass is a hardening pass, not a forced-promotion
pass.

Completed on this pass:

- the eight provisional Tier 1 direct owners now carry exact-board corpus on
  `exact`, `contrastive`, `near_miss`, `nasty_negative`,
  `move_local_false_witness`, and `comparative_false_rival`
- comparative calibration is now family-aware at the typed-metric level; the
  provisional comparative lane requires a real admissible counterpart plus at
  least three typed metrics
- comparative shallow re-audit is now family-complete across all eight
  provisional families, and those shallow rows stay `SupportOnly` /
  planner `none` rather than promoting comparative ownership
- synthesis/projector admission is tightened at the family boundary instead of
  by planner or wording fallback
- `SpaceClamp` now requires real multi-file clamp evidence; single-file
  pressure pictures are not enough

Current verdict on this roadmap pass:

- `KingSafetyShell` -> stay `Provisional`
- `DevelopmentCoordinationState` -> stay `Provisional`
- `PieceRoleFitness` -> stay `Provisional`
- `SpaceClamp` -> stay `Provisional`
- `CounterplayAxis` -> stay `Provisional`
- `RestrictionShell` -> stay `Provisional`
- `MobilityCage` -> stay `Provisional`
- `RedeploymentRoute` -> stay `Provisional`

Current move-local policy on this pass:

- seven provisional Tier 1 families remain closed on `move_local`
- `CounterplayAxis` now closes as
  `exact RestrictionShell support narrow-go` rather than an open frontier:
  same-batch final rerun on 2026-04-14 yields 8 `MoveLocal` `SupportOnly`
  claim rows and 7 planner-support rows, 62 additional move-witness rows
  remain blocked at provisional scope, 0 admitted rows are blocked by
  certification, and the official pack split is now 8 reopened, 4 frozen
  out-of-scope rows, and 12 negatives. Official capability authority is fixed
  by `CounterplayAxis.official-capability-pack.json`; official non-capability
  authority is fixed by
  `CounterplayAxis.frozen-out-of-scope-pack.json` and
  `CounterplayAxis.negative-pack.json`
- `P5-T02` therefore moves from `blocked` to `go`:
  the exact `CounterplayAxis` slice passed without projector or certification
  widening, the remaining frontier is now explicitly separated into frozen
  out-of-scope vs negative packs rather than left as an open reopen queue, and
  no future reopening is allowed without a new exact positive evidence pack
  plus the same-batch `45 / 360 / max8` rerun showing no broad loosen and no
  other family drift

## Phase 6. ClaimCertification Rewrite

Goals:

- move release logic onto object certificates
- keep fail-closed philosophy
- stop rebuilding semantic meaning inside player-facing policy code

Keep:

- provenance
- quantifier
- attribution
- stability
- blocker logic

Replace:

- family-forest branching
- slice-specific release hacks

Claim certification must consume canonical object readiness rather than
primitive-count or family-name heuristics. `Provisional` and
`DeferredForDelta` families may survive in object state but must not be
silently promoted into primary claims.

Current Phase 6 status:

- certification now reads readiness plus typed delta burden
- `Stable` is no longer an automatic `Certified`
- `MoveLocal`
  - needs a transition-aware typed witness plus exact-board support to remain
    primary
- `PositionLocal`
  - needs focal anchors plus exact-board support to remain primary
- `Comparative`
  - needs exact counterpart witness, rival object context, and at least two
    typed metrics to remain primary; shallow one-metric contrast stays
    `SupportOnly`
- insufficient exact-board support now downgrades even `Stable` claims to
  `Deferred`
- `Provisional` typed deltas remain `SupportOnly`

## Phase 7. Planner Rewrite

Goals:

- planner reads only object delta plus certified support
- planner chooses question projection only

Planner inputs:

- `DecisiveTruthContract`
- `StrategicObjectDelta`
- `CertifiedSupport`

Planner outputs:

- `WhyThis`
- `WhatChanged`
- `WhatMattersHere`
- `WhyNow`
- `WhatMustBeStopped`

Planner should choose projection only; it must not rediscover semantics from raw
features.

Planner primary admission must therefore remain readiness-bound:

- only `Stable` certified claims may drive `claimIds`
- `Provisional` claims may survive only as support or secondary material
- `DeferredForDelta` families remain blocked from primary planner ownership

Current `P7-Q01` / `P7-Q02` planner admission matrix on top of delta-aware
certification:

- `WhyNow`
  - `Certified` typed `MoveLocal` only on non-bad contracts when the
    projection carries timing-sensitive witness such as `ReleaseCandidate`
    primitive evidence or a timing tag like `BreakAccelerated`,
    `BreakDelayed`, `RouteShortened`, or `PasserAccelerated`
- `WhyThis`
  - `Certified` typed `MoveLocal` only, and only when `DecisiveTruthContract`
    is not `isBad`
- `WhatMustBeStopped`
  - `Certified` typed `MoveLocal` only, and only when `DecisiveTruthContract`
    is `isBad`
- `WhatChanged`
  - `comparative_primary` only (the certification-native `Certified` typed
    `Comparative` primary lane); support-only or shallow comparative evidence
    must stay demoted/deferred and must not reconstruct primary ownership
- `WhatMattersHere`
  - `Certified` typed `PositionLocal` only
- `SupportOnly`
  - typed claims may remain secondary only after a matching primary axis is
    already admitted
- scope-only shells and `Deferred`
  - do not choose a primary question lane
- `WhyNow`
  - remains a separate timing-owned lane and is not reopened by the
    non-timing `WhyThis` path; bad-contract move-local timing still stays on
    `WhatMustBeStopped`

Current `P7-E01` explanation-trace status on the same planner lane:

- canonical pre-sanitization explanation trace now lives only on the
  `src/test` / research boundary
- the trace runner exports one row per exact corpus slice with:
  - row id / case type / FEN / played move
  - family / readiness / scope
  - typed projection / witness / exact-board evidence summary
  - certification status
  - planner axis plus primary/support/no-admission bucket
  - localized stage counts so exact and nasty rows can stop at object, delta,
    certification, or planner rather than only final payload observation
- this packet does not widen runtime payloads, planner wording, renderer
  semantics, or Tier 2 / Tier 3 admission

Current `P7-E02` tail-risk eval status on the same planner lane:

- the test-only trace contract now emits a dedicated tail-risk evaluation report
  over `P7-E01` rows with macro pass-rate and hardest-slice separation
- macro metric is `passRate` over the existing trace rows, while the hard gate
  separately counts planner-leak failures on the packet-target negative slices
- hardest metric enforces planner-blocked hardest slices:
  - `near_miss`
  - `nasty_negative`
  - `move_local_false_witness`
  - `comparative_false_rival`
  - packet-owned `planner_negative` rows in test-only evaluation
- evaluator fails fast when any hardest-slice row that explicitly expects
  planner blocking, whether upstream `absent` or upstream `present` with a
  planner expectation of `none`, leaks into planner admission
  (`primary`/`support`) even when macro pass rate still stays above `0.98`
- evaluation is available via
  `StrategicObjectExplanationTraceRunner --tail-risk --tail-risk-threshold=0.98` and
  writes `*.tail-risk.json` on top of the existing `*.jsonl` trace output

## Phase 8. Renderer / UI Thin Shell

Goals:

- renderer consumes planner output only
- no semantic reconstruction at rendering time

Keep:

- stitching
- ordering
- payload normalization

Remove:

- semantic salvage
- fallback-driven strategy invention

Current vertical proving order:

1. `P8-R01-thin-shell-certified-renderer`
   - passed on 2026-04-09
   - Bookmaker renderer/API/frontend shell now consumes certified planner
     ownership only and does not reintroduce semantic salvage
2. `P8-R02-exact-target-campaign-e2e`
   - passed on 2026-04-11
   - the packet-owned `d6` current-position probe, `WhyThis(d6)` fixation
     lane, and exact `WhatChanged(d6)` comparative-support lane now survive
     thin-shell delivery as one certified shared-target campaign
3. `P8-R03-bounded-favorable-simplification-e2e`
   - passed on 2026-04-11
   - on `curated-exact:k09b`, the bounded favorable-simplification claim now
     reaches the thin shell as one isolated primary `WhyThis` explanation
4. `P8-R04-current-position-coordination-e2e`
   - passed_with_defer on 2026-04-10
   - one bounded `K09A` current-position coordination probe survives end-to-
     end while `K09D`, `K09E`, and single-active-piece mirage remain closed
5. composite chess semantics tranche
   - `P5-U01-trade-invariant-primary-simplification`
     - passed on 2026-04-10; one bounded favorable simplification claim now
       owns the main `WhyThis` explanation on `curated-exact:k09b`
   - comparative near-miss fail-closed tranche
      - `P6-B02-whatchanged-comparative-nearmiss-certification`
        - passed; packet-owned near-miss comparative now stays support-only or
          deferred and never owns `WhatChanged` primary
      - `P7-Q03-whatchanged-comparative-demotion-matrix`
        - passed on 2026-04-10; planner now keeps `WhatChanged` primary on
          `comparative_primary` only
    - `P7-E03-comparative-nearmiss-tail-risk`
      - passed on 2026-04-10; trace/tail-risk now expose comparative near-miss
        demotion and hard-fail `WhatChanged` primary leakage
    - `P6-B01-shared-target-continuity-certification`
      - passed on 2026-04-10
      - certification now stamps one packet-owned shared-target continuity
        witness on the `d6` current-position probe, target-fixation, and the
        exact packet-owned comparative-support claim; planner support pairing
        for `WhatChanged` consumes only that witness while the `d5`
        contrastive pair stays outside it
    - `P9-R05-blocked-slice-rerun`
      - passed on 2026-04-11; rerun closed both blocked vertical slices using
        only landed `P5-U01` / `P6-B01` semantics

Execution rule:

- do not add another horizontal gate, ownership, or support-only packet before
  one of these vertical packets fails on an exact slice
- `P5-T02` does not block this tranche because the tranche consumes only
  already re-earned stable slices and does not depend on provisional
  move-local reopening

## Post-Phase-7 Frontier

The certified planner spine is now landed through:

- typed delta projection
- delta-aware certification
- question-native planner admission
- separate timing-sensitive `WhyNow`
- pre-sanitization explanation trace
- tail-risk gating

The next frontier is not more family sprawl.
It is chess-meaning closure on top of that spine, proved vertically rather
than by adding more horizontal infra first.

Canonical next tranche order:

`P8-R01` thin-shell certified renderer passed on 2026-04-09:
Bookmaker renderer/API/frontend shell now stays semantically thin on certified
planner output; controller/frontend Bookmaker payload no longer
exports/decodes/reconstructs `strategyPack` / `signalDigest` on that path; the
canonical thin-shell renderer mirrors planner `claimIds/supportClaimIds`; and
board-anchored shell tests covered the re-earned stable slices plus `K03A`,
`K09E`, and the single-active-piece mirage closure.

`P9-A01` exact comparative support passed on 2026-04-09:
the new spine now re-earns one exact same-owner shared-target
`FixedTargetComplex` + `RestrictionShell` `WhatChanged` support slice, while
shallow comparative stays planner `none` and localizes at `certification`.

`P9-A02` exact target fixation also passed on 2026-04-09:
the new spine now re-earns one exact `FixedTargetComplex` move-local
`WhyThis` slice only when the played move hits the target's exact fixation
square on a truly `fixed=true` target; pressure-only target pictures remain
planner `none`.

`P9-A03` bounded favorable simplification also passed on 2026-04-09:
the new spine now re-earns one exact same-task `TradeInvariant` move-local
`WhyThis` slice only when a centralized bounded favorable-simplification
predicate admits the object at the projector boundary; target-led,
contrastive, and heavy-piece-release lookalikes remain move-local closed.

`TradeInvariant` strengthening tranche closed on 2026-04-15 as
`narrow-owner ceiling confirmed`:
exact-board re-audit defends the current three primaries
(`2 x BreakBackedInvariant`, `1 x PacketOwnedFixedTargetSlice`), the strongest
object-only candidates still fail move-local because the played move never
touches the certified trade-preserving exchange square, and the same-batch
`45 / 360 / max8` rerun remains `164 / 3 / 161` with no other-family drift.
Further reopen is blocked unless a new exact-board positive pack promotes
additional move-local claims without access-shadow relapse or broad loosen.

`P9-A05` current-position coordination probe also passed with defer on 2026-04-09:
the new spine now re-earns one exact `DevelopmentCoordinationState`
`WhatMattersHere` slice on `K09A` only; `K09D`, `K09E`, and the
single-active-piece mirage remain fail-closed.

`P8-R02` passed on 2026-04-11:
the packet-owned `d6` current-position probe, the exact `WhyThis(d6)`
fixation lane, and the exact `WhatChanged(d6)` comparative-support lane now
survive thin-shell delivery as one certified shared-target campaign.
The `WhyThis` row may still carry unrelated non-packet move-local primary
claims, but the certified `d6` continuity-bound lane survives end-to-end
without reopening `TransitionBridge` or broad campaign-threading.

`P8-R03` passed on 2026-04-11:
the exact `curated-exact:k09b` simplification claim now reaches the thin shell
as one isolated primary `WhyThis` explanation after `P5-U01` closed the
packet-owned primary-isolation boundary.

`P8-R04` passed_with_defer on 2026-04-10:
the exact `P9-A05` deferred slice now survives end-to-end on `K09A`, while
`K09D`, `K09E`, and the single-active-piece mirage remain closed.

The next frontier is now a comparative near-miss fail-closed tranche:

`P6-B02-whatchanged-comparative-nearmiss-certification` is `passed`:
packet-owned near-miss comparative now stays support-only or deferred and
never owns `WhatChanged` primary.

`P7-Q03-whatchanged-comparative-demotion-matrix` is passed on 2026-04-10:
`WhatChanged` primary now stays on `comparative_primary` only, and support-only
or shallow comparative evidence cannot reconstruct primary ownership.

`P7-E03-comparative-nearmiss-tail-risk` passed on 2026-04-10:
trace/tail-risk now expose comparative near-miss demotion and hard-fail
near-miss `WhatChanged` leakage.

Only after that tranche landed could `P6-B01` reopen shared-target
continuity, and it is now passed: certification stamps one packet-owned
shared-target continuity witness on the `d6` current-position probe,
target-fixation lane, and the exact packet-owned comparative-support claim,
while the `d5` contrastive pair, `shared-target-support-near-miss`,
wrong-support, and preserved `c6` stay outside the witness boundary.

`P9-R05` passed on 2026-04-11:
rerun closed the two blocked slices using only landed `P5-U01` / `P6-B01`
semantics, so this roadmap does not open a new packet here.

`P5-T01` Tier 1 provisional comparative re-audit passed_with_defer on 2026-04-09:
all eight provisional families stay `Provisional`; the comparative lane now
has family-complete shallow rows that remain projector-admissible but localize
at `certification` as `SupportOnly` and stay planner `none`.

`P5-T02` Tier 1 provisional move-local reopen audit is go on 2026-04-14:
all eight provisional families stay `Provisional`, `CounterplayAxis` now
materializes 8 same-batch `MoveLocal` `SupportOnly` claim rows from one
centralized exact-board positive pack, 62 additional move-witness rows now
localize at provisional scope, the official pack split is `8 / 4 / 12`
positive / blocked / negative, and the other seven provisional families
remain `move_local` closed because the corpus is not family-complete.

Comparative-quality certification is now complete.

- `P6-A02`
  - passed
  - absorbed the `P5-C02` shallow-contrast burden into canonical
    `Certified` / `SupportOnly` / `Deferred` / `planner_none` outcomes
- `P6-A02a`
  - passed
- `P6-A02b`
  - passed
- `P6-A02c`
  - passed

This order is intentional.
The current bottleneck is not raw planner control anymore.
It is whether the already re-earned exact slices can survive all the way to a
thin user-visible shell without semantic drift, and only then whether new gate
work is justified by exact slice failure.

## Phase 9. Narrow Slice Reconstitution

Rebuild the already-proven semantics on the new architecture first.

Suggested order:

1. exact comparative support
   - passed on 2026-04-09
   - the exact same-owner shared-target support slice is re-earned on the new spine
2. exact target fixation
   - passed on 2026-04-09
   - the exact fixation-square move-local slice is re-earned on the new spine
3. bounded favorable simplification
   - passed on 2026-04-09
   - the exact same-task `TradeInvariant` simplification slice is re-earned
4. current-position fixed-target probe
   - passed on 2026-04-09
   - the exact B15A c6 slice, packet-owned d6 slice, and two additional quiet
     d6 exact descriptors now reopen only through the certified
     current-position fixed-target bundle; matching exact
     `RestrictionShell-white-center-d4-de` support may attach only from the
     same certified probe kind, while K03A, K09E, and the quiet `d4`
     near-miss stay closed
5. current-position coordination probe
   - passed_with_defer on 2026-04-09
   - the exact K09A slice now reopens only through the certified current-position coordination probe; K09D, K09E, and the single-active-piece mirage stay closed

The rewrite should earn back old exact successes from the new architecture,
not preserve them through adapters.

Current lane note:

- exact comparative support is now closed as the first Phase 9 slice
- exact target fixation is now also closed on one fixation-square move-local
  slice
- bounded favorable simplification is now also closed on one same-task
  `TradeInvariant` move-local slice, and its current rewrite capability is now
  ceiling-confirmed as a three-row narrow-owner pack rather than an open
  family-wide frontier
- the current-position fixed-target tranche is now passed on the same new
  spine with both the preserved `c6` probe and the packet-owned `d6` probe
- quiet current-position generalization is now ceiling-confirmed as
  descriptor-bound on the current evidence pack:
  boundary-visible structural matching leaked cross-axis fixed targets and
  raised same-batch `45 / max8`
  `FixedTargetComplex` `WhatMattersHere / position_local` owner rows
  `2 -> 23` while `RestrictionShell` support stayed fixed at `2`
- `FixedTargetClusterWitness` is now the only admitted follow-through beyond
  that descriptor ceiling:
  five new exact-board same-batch positives reopen both
  `FixedTargetComplex` owners and `RestrictionShell` support together
  (`2 -> 7` / `2 -> 7`) only through a shared backend witness, not through any
  broader descriptor matcher
- the next current-position follow-through after that witness is now passed on
  the same boundary:
  `StrategicDeltaProjection.PositionLocal` carries the typed witness directly,
  so certification/planner no longer reconstruct fixed-target cluster truth on
  the current-position path
- the current-position coordination probe is now also passed (with defer) on
  the same new spine
- Phase 9 is therefore no longer the active packet frontier by itself, and
  `P9-R05` has now also passed on 2026-04-11 after the landed `P6-B01`
  continuity boundary
- Tier 1 provisional comparative re-audit is now passed_with_defer with all
  eight families staying `Provisional`
- Tier 1 provisional move-local reopen audit is now go for one exact
  `CounterplayAxis` support slice:
  `CounterplayAxis` alone passes one centralized exact-board support slice
  with an official `8 / 4 / 12` pack split, while the other seven provisional
  families stay move-local closed on the current evidence pack

## Phase 10. Expansion

Once narrow slice parity exists on the new architecture, coverage should grow by
adding object contracts rather than adding new family-specific rollout paths.

Initial expansion priorities:

- `CounterplayAxis`
- `AccessNetwork`
- `TradeInvariant`
- `PlanRace`
- `ConversionFunnel` remains frozen pending a new exact positive pack that
  passes the same-batch `45 / 360 / max8` rerun with no other-family drift

## Capability Maturation Map

This roadmap defines not only implementation order, but also when the rewrite
becomes a real strategic explanation system.

- `Phase 3. Primitive Extraction Layer`
  - raw strategic ingredients only
  - contrastive-ready primitive anchors exist here, but only below the object
    layer
  - not yet a strategic explanation layer

- `Phase 4. StrategicObjectSynthesizer`
  - object-level strategic state appears
  - strategy becomes object-native, but not yet delta-explanatory

- `Phase 5. StrategicObjectDeltaProjector`
  - first phase where contrastive strategic explanation becomes structurally
    possible
  - the system can express what a move changed in strategic-object terms
  - counterplay and race-shaped discussion become structurally possible here

- `Phase 6. ClaimCertification`
  - strategic explanation becomes trustworthy here
  - persistence, proof strength, and fail-closed release are decided here

- `Phase 7. Planner`
  - certified object deltas are projected into question forms:
    - `WhyThis`
    - `WhatChanged`
    - `WhatMattersHere`
    - `WhyNow`
    - `WhatMustBeStopped`
  - planner chooses projection only; it does not create semantics

- `Phase 8. Renderer / UI Thin Shell`
  - strategic explanation becomes user-visible
  - no semantic reconstruction is allowed here

- `Phase 9. Narrow Slice Reconstitution`
  - proves that previously successful exact slices can be rebuilt on the new
    architecture

- `Phase 10. Expansion`
  - mature doctrine-level and opening-specific strategic explanation belongs
    here
  - richer race, conversion, and counterplay narratives should become fully
    mature here rather than merely structurally representable

## Execution Rules for Agents

- do not preserve old modules just because tests still fail
- do not write compatibility wrappers to restore legacy topology
- do not treat legacy commentary docs as north-star design
- do not widen capability during cleanup
- when in doubt, prefer deletion plus fixture preservation over adapter code

## Success Criteria

The rewrite succeeds when:

- low-level truth remains trustworthy
- strategic semantics are object-native
- planner sees objects and deltas, not raw feature piles
- renderer stays semantically thin
- old topology is no longer the easiest local minimum for an agent
