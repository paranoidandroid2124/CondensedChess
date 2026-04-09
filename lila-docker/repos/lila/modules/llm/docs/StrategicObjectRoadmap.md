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

1. current-position fixed-target probe is passed; current-position
   coordination probe is next after the first exact comparative-support,
   target-fixation, and bounded favorable-simplification slices
2. Tier 1 provisional comparative re-audit after early narrow-slice parity
3. selective provisional move-local reopen audit only where exact-board
   resistance justifies it

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

- all eight provisional Tier 1 families remain closed on `move_local`
- promotion is deferred until broader exact-board positive and nasty-negative
  resistance justifies reopening, rather than being inferred from a small
  positive corpus

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
  - `Certified` typed `Comparative` only
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

## Post-Phase-7 Frontier

The certified planner spine is now landed through:

- typed delta projection
- delta-aware certification
- question-native planner admission
- separate timing-sensitive `WhyNow`
- pre-sanitization explanation trace
- tail-risk gating

The next frontier is not more family sprawl.
It is chess-meaning closure on top of that spine.

Canonical next tranche order:

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

1. `P9-A05` current-position coordination probe
2. `P5-T01` Tier 1 provisional comparative re-audit
3. `P5-T02` Tier 1 provisional move-local reopen audit

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
It is whether a comparative that survives object and delta admission is
actually chess-meaningfully different enough to deserve certification and
planner ownership.

## Phase 9. Narrow Slice Reconstitution

Rebuild the already-proven semantics on the new architecture first.

Suggested order:

1. current-position fixed-target probe
   - passed on 2026-04-09
   - the exact B15A slice now reopens only through the certified current-position fixed-target bundle; K03A and K09E stay closed
2. current-position coordination probe

The rewrite should earn back old exact successes from the new architecture,
not preserve them through adapters.

Current lane note:

- exact comparative support is now closed as the first Phase 9 slice
- exact target fixation is now also closed on one fixation-square move-local
  slice
- bounded favorable simplification is now also closed on one same-task
  `TradeInvariant` move-local slice
- the current-position fixed-target probe is now passed on the same new spine
- the next frontier is the current-position coordination probe on the same
  new spine

## Phase 10. Expansion

Once narrow slice parity exists on the new architecture, coverage should grow by
adding object contracts rather than adding new family-specific rollout paths.

Initial expansion priorities:

- `CounterplayAxis`
- `AccessNetwork`
- `TradeInvariant`
- `ConversionFunnel`
- `PlanRace`

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
