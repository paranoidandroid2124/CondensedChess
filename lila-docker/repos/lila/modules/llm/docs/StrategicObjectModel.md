# Strategic Object Model

This document is the canonical north-star for the Chesstory rewrite.

For rewrite work, this document has higher authority than the legacy commentary
docs. The existing commentary docs remain useful only as migration references
for current-state behavior, truth spine, and regression risk.

Demolition is intentional in this rewrite. Legacy topology may be deleted
before a replacement is complete, and compile-red intermediate states are
acceptable if they remove old semantic authority.

## Purpose

Chesstory must move from local feature accumulation plus family-specific
admission toward canonical strategic objects, object deltas, and certified
player-facing claims.

The core problem is not lack of low-level board features. The problem is that
many local signals never compress into one stable strategic object, so the
system becomes strong at legality and fail-closed behavior but weak at human
strategic explanation.

## North Star

The mature system must explain three things reliably:

1. Why the engine prefers one move over realistic alternatives.
2. What strategically matters in the current position, even beyond the best move.
3. What strategically changed because of the move that was played.

These correspond to:

- engine-rationale explanation
- position-probe explanation
- state-delta explanation

Current rewrite checkpoint:

- `Phase 3` primitive extraction is complete
- `Phase 4` objectization, hardening, and exact-board calibration are complete
- `Phase 5` currently runs on the Tier 1 direct-delta-owner lane with exact-
  board reinforcement and provisional-family hardening already landed
- `Phase 6` delta-aware certification is complete
- `Phase 7` question admission, `WhyNow`, explanation trace, and tail-risk
  gate are complete

Current post-spine frontier:

- the certified planner spine is now structurally landed
- comparative-quality certification is now closed
- one exact comparative-support slice is now re-earned on the new spine
- one exact target-fixation slice is now re-earned on the new spine
- one exact bounded favorable-simplification slice is now re-earned on the new
  spine
- one exact current-position fixed-target probe is now re-earned on the new
  spine
- one exact current-position coordination probe is now re-earned on the new
  spine (K09A only; K09D/K09E and single-active-piece mirage remain closed)
- this means:
  - the first reopened comparative-support slice is one exact same-owner
    shared-target `FixedTargetComplex` + `RestrictionShell` `WhatChanged`
    support lane only
  - the first reopened target-fixation slice is one exact
    `FixedTargetComplex` `WhyThis` lane keyed to the target's fixation square
    on a truly `fixed=true` object, not a generic target-touch move
  - the first reopened favorable-simplification slice is one exact same-task
    `TradeInvariant` move-local `WhyThis` lane keyed to a centralized bounded
    projector predicate, not a broad conversion, task-shift, or endgame lane
  - Tier 1 provisional comparative re-audit is now passed_with_defer:
    all eight provisional families stay `Provisional`, and family-complete
    shallow comparative rows stay `SupportOnly` / planner `none`
  - Tier 1 provisional move-local reopen audit is now blocked:
    all eight provisional families stay `move_local` closed because no
    family-complete exact-positive plus move-local nasty-negative witness pack
    exists yet
- the next active frontier is now a vertical Phase 8/9 proving tranche, not a
  new horizontal gate tranche
- that vertical tranche should:
  - prove a thin shell renderer/API/front can carry certified planner output
    without semantic salvage
  - prove one fixed-weakness target campaign end-to-end across
    `WhatMattersHere`, `WhyThis`, and `WhatChanged`
  - prove one bounded favorable-simplification slice end-to-end
  - prove one bounded current-position coordination probe end-to-end in its
    current deferred form
- `P8-R01` thin-shell certified renderer passed on 2026-04-09:
  Bookmaker shell delivery is now thin on certified planner ownership and no
  longer exports/decodes/reconstructs `strategyPack` / `signalDigest` on this
  path; `P8-R02` is now the next highest-priority `ready` packet
- the blocked provisional move-local reopen lane is a stopping rule for that
  evidence pack, not a blocker to vertical proof on already re-earned stable
  slices

This document remains the capability north-star, not the active packet queue.
Use `StrategicObjectRoadmap.md` for live execution order and current frontier
status.

## Structural Diagnosis

Current strengths:

- truth-first discipline
- fail-closed release
- move attribution
- support-only containment
- strong exact-board validation culture

Current weaknesses:

- no canonical intermediate strategic object layer
- no canonical object delta layer
- theme/plan inference still leans too much on local feature weighting
- planner/policy/rendering layers still reconstruct semantics too late

The rewrite therefore keeps low-level truth and local sensing, but replaces the
middle semantic stack.

## Rewrite Boundary

Keep:

- L0 Raw fact layer
- L1 Local analyzer layer
- L2 Canonical truth / verification

Replace:

- L3 Theme / plan inference
- L4 Shared semantic carrier
- L5 Player-facing claim synthesis
- L6 Question planner / admission

Keep thin:

- L7 Renderer / UI / polish

## Demolition Contract

During the demolition phase:

- L0-L2 are the only runtime spine that must survive
- L3-L7 legacy topology is delete-first, not preserve-first
- no adapter, shim, or compatibility wrapper should be added to keep the old
  planner/carrier forest alive
- fixture examples are preserved separately from the harnesses that used them

## Target Architecture

- N0 `RawPositionEvidence`
- N1 `MoveTruthFrame` / `DecisiveTruthContract`
- N2 `StrategicObjectSynthesizer`
- N3 `StrategicObjectDeltaProjector`
- N4 `ClaimCertification`
- N5 `QuestionPlanner`
- N6 `Renderer`

## Demolition Phase Boundary

Keep line:

- L0 raw board-truth primitives and exact board evidence
- L1 local analyzers, detectors, and probe validity contracts
- L2 `MoveTruthFrame` / `DecisiveTruthContract` / truth gate enforcement

Demolition line:

- L3 theme and plan inference forests
- L4 shared semantic carrier layers
- L5 player-facing claim synthesis and certification forests
- L6 question-planner, admission, and replay salvage layers
- L7 renderer, payload, and polish salvage beyond a thin shell

Current canonical source boundary:

- `modules/llm/src/main/scala/lila/llm/analysis/DecisiveTruth.scala`
  - N1 truth core
- `modules/llm/src/main/scala/lila/llm/strategicobject/`
  - N0 and N2-N6 demolition skeleton

## Canonical Source Boundary

The demolition skeleton should be reflected in source form before semantic
reconstitution begins.

- N1 currently remains anchored in `analysis/DecisiveTruth.scala` through
  `MoveTruthFrame` and `DecisiveTruthContract`
- N0 and N2-N6 should exist as the minimal canonical package boundary under
  `src/main/scala/lila/llm/strategicobject`
- legacy planner, carrier, support, compression, replay, and player-facing
  policy files are not migration anchors

### Layer Intent

`RawPositionEvidence`
: local board facts, motifs, threats, structure facts, route candidates, probe
results, and engine evidence

`MoveTruthFrame` / `DecisiveTruthContract`
: chess-truth core and catastrophic blocker enforcement

`StrategicObjectSynthesizer`
: converts low-level evidence into typed strategic objects

`StrategicObjectDeltaProjector`
: projects move-local or position-local change over strategic objects

`ClaimCertification`
: certifies whether an object or delta is fit for player-facing release

`QuestionPlanner`
: chooses which question axis best projects the certified object or delta

`Renderer`
: only turns certified claims/support into prose; it must not invent strategy

## Core Invariant

Every player-facing strategic claim must map to:

- exactly one canonical strategic object, and
- exactly one move-local or position-local delta

This is the main semantic invariant of the rewrite.

## Delta Principle

Each certified claim must project exactly one object-native delta:

- `move_local`
- `position_local`
- `comparative`

The planner and renderer may choose projection and wording, but they must not
invent delta semantics that were not already certified at the object layer.

## Strategic Object Principle

Do not model strategic names directly as atomic runtime families.

Instead:

- strategic names are surface labels
- strategic objects are the runtime semantic core
- certificates are orthogonal to object kind

For example, "minority attack" is not a basis object. It is usually a composed
bundle involving fixed targets, break axes, access networks, and conversion
paths.

## Strategic Object Basis

The mature target model uses 24 near-orthogonal object families.

### A. Position Substrate

1. `PawnStructureRegime`
2. `KingSafetyShell`
3. `DevelopmentCoordinationState`
4. `PieceRoleFitness`
5. `SpaceClamp`
6. `CriticalSquareComplex`

### B. Resources, Targets, Geometry

7. `FixedTargetComplex`
8. `BreakAxis`
9. `AccessNetwork`
10. `CounterplayAxis`
11. `RestrictionShell`
12. `MobilityCage`

### C. Interaction and Reconfiguration

13. `RedeploymentRoute`
14. `DefenderDependencyNetwork`
15. `TradeInvariant`
16. `TensionState`
17. `AttackScaffold`
18. `MaterialInvestmentContract`

### D. Temporal and Transition

19. `InitiativeWindow`
20. `PlanRace`
21. `TransitionBridge`
22. `ConversionFunnel`
23. `PasserComplex`
24. `FortressHoldingShell`

The rewrite must preserve all 24 families as the canonical object vocabulary.
No future phase should reinterpret the architecture as "only the first few
families matter". A narrow implementation frontier is allowed, but a narrow
vocabulary is not.

## First-Order Object Richness

Phase 4 must not collapse into "four thin objects". The first strategic object
layer should be rich enough that later delta/planner phases do not need to
rediscover semantics from raw board state.

### Board-Direct First-Order Objects

These families should be expected to rise directly from primitive material once
Phase 4 starts.

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

These are "board-direct" not because they are trivial, but because the Phase 3
primitive bank should already contain enough exact-board material to objectize
them without reopening raw board discovery.

### Graph-Derived First-Order Objects

These families may require object-graph composition inside Phase 4, but they
still belong to object state, not later planner invention.

15. `DefenderDependencyNetwork`
16. `TradeInvariant`
17. `TensionState`
18. `AttackScaffold`
19. `MaterialInvestmentContract`
20. `InitiativeWindow`
21. `PlanRace`
22. `TransitionBridge`
23. `ConversionFunnel`
24. `FortressHoldingShell`

These "graph-derived" families must still exist as object-layer state before
Phase 5. Delta projection may compare or transform them, but it must not invent
them from scratch.

## Phase 4 Frontier Rule

The roadmap may name a small executable frontier such as:

- `FixedTargetComplex`
- `CounterplayAxis`
- `TradeInvariant`
- `AccessNetwork`

That frontier is only the initial implementation order. It must not be read as
the intended long-term size of the object layer.

Phase 4 therefore has two simultaneous duties:

- fix the full 24-family vocabulary in code and schema
- make a small exact executable frontier real first

## Strategic Object Schema

A strategic object must carry more than an id and a family label. The first
object layer should be able to represent, at minimum:

- `id`
- `family`
- `owner`
- `locus` or `sector`
- `anchors`
- `supportingPrimitives`
- `supportingPieces`
- `rivalResourcesOrObjects`
- `relations`
- `stateStrength`
- `horizonClass`
- `evidenceFootprint`

An object schema that is too thin will push real semantics back into later
delta/planner phases, which the rewrite explicitly forbids.

## Family Hardening And Readiness

The full 24-family vocabulary remains canonical, but family admission strength
is not uniform.

Phase 4 objectization therefore requires a canonical family contract for each
family with, at minimum:

- required primitive witnesses
- required source or rival object witnesses when the family is graph-derived
- required relation or continuity patterns when the family is transition-shaped
- minimum shared-anchor or contested-anchor burden
- explicit forbidden loose patterns such as broad-overlap-only admission

Each synthesized object must also carry one readiness band orthogonal to
`StrategicStrengthBand`:

- `Stable`
- `Provisional`
- `DeferredForDelta`

Readiness is not object strength, and object strength is not readiness.
A `Dominant` object may still be `DeferredForDelta`, while an `Established`
object may still be `Stable`.

The purpose of readiness is not to delete weak families from object state.
The purpose is to preserve the full object graph while constraining downstream
delta, certification, and planner admission:

- `Stable`
  - primary candidate for delta, certification, and planner admission
- `Provisional`
  - object survives and may support position-local or support-only release
- `DeferredForDelta`
  - object survives in the graph but must not project primary player-facing
    delta until later hardening phases reopen it

High-risk graph-derived families also require exact-board calibration, not only
structural hardening.

- `PlanRace`
  - must not rise from bilateral presence or same-family overlap alone; it
    needs shared goal geometry or an explicit race/conflict link
- `TransitionBridge`
  - must not self-satisfy source and destination from the same bare survivor;
    it needs a real bridge witness plus destination continuity
- `FortressHoldingShell`
  - must prove denied-entry and blockade on the actual threatened hold lane,
    not just a shell-shaped picture in the same sector
- `InitiativeWindow`
  - must prove a timing witness; ordinary active pressure or broad overlap is
    insufficient
- `DefenderDependencyNetwork`
  - must prove a load-bearing constrained defender under pressure, not merely a
    defended square with nearby activity

Until later phases reopen these families with broader exact-board evidence,
readiness should stay conservative rather than being promoted on verbal motif
confidence.

## Relation Operators

The object layer also needs stable relation operators. Minimum target set:

- `enables`
- `denies`
- `fixes`
- `preserves`
- `transforms_to`
- `races_with`
- `depends_on`
- `overloads_or_undermines`

Surface strategy names are composed from object families plus these relations.

## Delta Principle

Strategic explanation is always delta-bearing.

- every released claim must bind exactly one certified object
- every released claim must also bind exactly one certified delta
- deltas may be move-local, position-local, or branch-comparative
- if no certified delta survives, the planner must defer or stay support-only

Readiness constrains this delta principle.
Delta, certification, and planner layers must consume the canonical object
readiness rather than re-deriving release confidence from family names,
primitive count, or wording-level heuristics.

## Phase 5 Delta Universe

Phase 5 operates over the full 24-family object vocabulary.

- working universe: `24` strategic-object families
- conceptual delta cells: `72`
  - `24 families x 3 scopes`
  - `move_local`
  - `position_local`
  - `comparative`

This does **not** mean that all 24 families have equal delta ownership or equal
player-facing authority.

### Tier 1. Direct Delta Owners

These are the core Phase 5 owner families. Their object state should project
cleanly into move-local, position-local, and comparative delta once readiness
allows it.

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

These families belong inside the Phase 5 universe, but they usually require
stronger relation, continuity, or witness burden before they should behave like
direct move-local owners.

1. `TradeInvariant`
2. `TensionState`
3. `AttackScaffold`
4. `MaterialInvestmentContract`
5. `InitiativeWindow`
6. `ConversionFunnel`

### Tier 3. Meta / Interpretive Delta Families

These families must still be computed inside Phase 5, but they should usually
remain support-oriented or deferred unless later calibration materially upgrades
their readiness.

1. `DefenderDependencyNetwork`
2. `PlanRace`
3. `TransitionBridge`
4. `FortressHoldingShell`

This 14 / 6 / 4 split is the north-star Phase 5 target map.
It does not erase the existing readiness system.
Instead, it explains how the full 24-family universe should be interpreted once
delta projection becomes the center of the pipeline.

Implementation order inside Phase 5 should therefore begin with Tier 1 direct
delta owners, then move to Tier 2 composite owners, and only then reopen Tier 3
meta families.

### Canonical Delta Contract

Phase 5 pass 1 opens direct delta ownership only for the Tier 1 families above.
The canonical runtime delta is no longer a scope placeholder.

Each emitted `StrategicObjectDelta` must carry, at minimum:

- `objectId`
- `family`
- `owner`
- `scope`
- typed family `profile`
- typed scope `projection`
- `changedAnchors`
- `supportingObjectIds`
- `rivalObjectIds`
- typed `evidenceRefs`

The canonical scope projections are:

- `MoveLocal`
  - typed change-bearing delta only when a family-specific transition witness
    survives from the canonical move trace plus object-graph evidence; current
    object shape alone is not sufficient
- `PositionLocal`
  - typed current-state probe over the certified object slice
- `Comparative`
  - typed bilateral object comparison with counterpart ids, family-aware
    comparative witness, and family-aware comparative profile; it must not
    degrade into owner-isolated summary or broad-overlap-only rivalry

Pass 1 reinforcement also keeps the typed delta alive past projection:

- `ClaimCertification`
  - preserves the canonical typed delta snapshot on each admitted certified
    claim instead of collapsing to scope-only release metadata
- `QuestionPlanner`
  - may bucket or admit only from the certified typed delta contract; it does
    not need to mint wording here, but it must not lose which transition or
    comparative axis was certified

Phase 6 then moves certification off readiness-only mapping.

- `Stable`
  - is not an automatic certification grant; the typed delta must still carry
    scope-specific exact-board burden
- `MoveLocal`
  - needs a transition-aware witness with anchored board support to remain
    primary
- `PositionLocal`
  - needs focal anchors plus exact-board evidence to remain primary
- `Comparative`
  - needs exact counterpart witness, rival-object context, and at least two
    typed metrics to remain primary
- shallow-but-typed deltas may survive as `SupportOnly`
- insufficient exact-board support must downgrade to `Deferred` rather than
  overclaiming from readiness alone
- `Provisional`
  - still may not become primary on this pass even when its typed delta burden
    is otherwise adequate

Pass 1 keeps the readiness boundary intact inside the projector:

- `Stable`
  - Tier 1 family may emit move-local, position-local, or comparative only
    when the family-specific projection witness survives
- `Provisional`
  - Tier 1 family defaults to position-local and may reopen comparative only
    conservatively; move-local stays closed in the current pass
- `DeferredForDelta`
  - object survives in graph state but emits no delta

Tier 2 and Tier 3 families remain in the Phase 5 universe and remain in the
object graph, but they do not gain direct delta ownership in pass 1 even when
their object readiness is `Stable` or `Provisional`.

Phase 5 Tier-1 provisional hardening and promotion audit on 2026-04-06 does
not change that readiness boundary. The eight provisional Tier 1 direct owners
`KingSafetyShell`, `DevelopmentCoordinationState`, `PieceRoleFitness`,
`SpaceClamp`, `CounterplayAxis`, `RestrictionShell`, `MobilityCage`, and
`RedeploymentRoute` remain object-valid but delta-conservative:

- all eight stay `Provisional`
- `MoveLocal` remains closed for all eight families even under forced-stable
  replay
- `Comparative` may reopen only from exact-board counterpart evidence plus a
  typed family profile with at least three metrics
- provisional-family corpus must now prove exact-board positive, contrastive,
  near-miss, nasty-negative, move-local false-witness, and comparative
  false-rival behavior instead of relying on verbal motif coverage

### Phase 5 Tier 1 Provisional Audit

The current conservative boundary for the eight provisional Tier 1 direct
owners is now exact-board audited rather than motif-driven:

- `KingSafetyShell`
- `DevelopmentCoordinationState`
- `PieceRoleFitness`
- `SpaceClamp`
- `CounterplayAxis`
- `RestrictionShell`
- `MobilityCage`
- `RedeploymentRoute`

Current north-star outcome:

- all eight stay `Provisional`
- all eight keep `move_local` closed on the current rewrite lane
- comparative may reopen only from family-aware counterpart witness plus a
  typed comparative metric profile; broad same-sector or same-file rivalry is
  not sufficient
- family-complete shallow comparative rows now survive exact-board projection
  for all eight families but remain `SupportOnly` / planner `none`, so no
  comparative-only promotion is justified on this pass
- each family now requires exact-board corpus on:
  - `exact`
  - `contrastive`
  - `near_miss`
  - `nasty_negative`
  - `move_local_false_witness`
  - `comparative_false_rival`

This is a readiness-discipline rule, not a wording rule.
Tier 1 provisional families survive in the object graph and in conservative
comparative lanes, but they should not be promoted on verbal motif confidence
or on single-board positive examples alone.

## Certificate Axes

Object certificates reuse the existing strength of the current system.

Required axes:

- certificate status
- quantifier
- modality
- attribution
- stability
- provenance

These are certificate dimensions, not object families.

## Primitive-First Synthesis

The rewrite must not jump from local features directly to strategic names.

Required flow:

1. raw board and analyzer evidence
2. primitive extraction
3. typed object assembly
4. object certification
5. object graph composition
6. question projection
7. surface wording

Example primitives:

- target square
- break candidate
- diagonal lane seed
- lift corridor seed
- knight route seed
- redeployment path seed
- route contest seed
- exchange square
- access lane
- hook contact seed
- tension contact seed
- counterplay resource seed
- release candidate
- outpost
- passer potential

Phase 3 primitive names must stay strictly below object-family names.

- use names such as `BreakCandidate`, not `BreakAxis`
- preserve exact board-grounded contrast material such as contested route-entry
  seeds
- do not let a primitive name pre-claim an object family or relation verdict

Phase 3 primitive coverage is contract-bearing, not harness-only.

For each reopened primitive axis, the fixture bank must contain exact-board
rows for:

- certified positive
- certified negative
- contrastive asymmetric comparison
- noisy near-miss rejection

Phase 3 contrastive coverage is primitive-only. It proves that exact-board
route geometry, hook/lever contact, counterplay resources, and
tension/release-maintain anchors survive extraction. It does not authorize
planner projection, claim wording, or user-facing contrastive explanation
before object deltas exist.

The primitive boundary is singular:

- raw evidence and legacy semantic carriers may enter only through
  `RawPositionEvidence -> PrimitiveExtractor`
- later phases must consume primitive anchors rather than reopen board-state
  discovery for route geometry, counterplay resources, or tension contacts

## Planner Contract

The planner must not read raw feature piles.

The planner should read only:

- `DecisiveTruthContract`
- `StrategicObjectDelta`
- `CertifiedSupport`

The planner's job is not to rediscover strategy. Its job is to choose the best
question projection:

- `WhyThis`
- `WhatChanged`
- `WhatMattersHere`
- `WhyNow`
- `WhatMustBeStopped`

Canonical admission matrix on the current planner lane:

- `WhyNow`
  - opens only on non-bad contracts from a `Certified` typed `MoveLocal`
    delta with a timing-sensitive witness such as `ReleaseCandidate`
    primitive evidence or a timing tag like `BreakAccelerated`,
    `BreakDelayed`, `RouteShortened`, or `PasserAccelerated`
- `WhyThis`
  - opens only from a `Certified` typed `MoveLocal` delta when the contract is
    not `isBad`
- `WhatMustBeStopped`
  - opens only from a `Certified` typed `MoveLocal` delta when the contract is
    `isBad`
- `WhatChanged`
  - opens only from a `Certified` typed `Comparative` delta
- `WhatMattersHere`
  - opens only from a `Certified` typed `PositionLocal` delta
- `SupportOnly`
  - typed claims may attach only as secondary material behind an already
    admitted primary axis
- `Deferred` or scope-only shells
  - do not open planner ownership
- `WhyNow`
  - remains a separate timing-owned lane and is not reopened by the
    non-timing `WhyThis` path; bad-contract move-local timing still stays on
    `WhatMustBeStopped`

## Post-Spine Frontier

Comparative-quality certification remains the gate to close on this spine, not
a projector-side widening step.
The next north-star frontier is chess-meaning closure on top of the landed
spine.

Comparative-quality certification is now closed.
Exact comparative support is now re-earned on one narrow same-owner
shared-target `FixedTargetComplex` + `RestrictionShell` slice, while shallow
comparative stays planner `none` and localizes at `certification`.
Exact target fixation is now also re-earned on one narrow move-local
`FixedTargetComplex` slice keyed to the exact fixation square, while
pressure-only target pictures stay planner `none` and localize no higher than
`object`.

That frontier should proceed in this order:

1. `Phase 8` thin-shell certified renderer
   - prove renderer/API/frontend delivery consumes certified planner output
     without semantic salvage or support-only promotion
2. fixed-weakness target campaign end-to-end
   - use the already re-earned exact comparative-support, target-fixation, and
     current-position fixed-target slices to prove one campaign survives across
     `WhatMattersHere`, `WhyThis`, and `WhatChanged`
3. bounded favorable-simplification end-to-end
   - use the already re-earned same-task `TradeInvariant` slice to prove one
     bounded simplification explanation survives user-visible delivery
4. current-position coordination end-to-end
   - use the current `K09A` coordination slice in its `passed_with_defer`
     state to prove one bounded probe survives user-visible delivery while the
     closed siblings stay closed
5. only then derive new gate packets from exact slice failures
   - if one of the vertical packets fails, derive the needed ownership,
     certification, or renderer packet from that exact failure instead of
     widening infra proactively

This frontier is about separating:

- "a comparative exists" from
- "the comparative expresses a genuinely different strategic campaign"

It is therefore a chess-meaning problem, not a renderer or wording problem.

## Renderer Contract

The renderer must stay thin.

It may:

- stitch certified sentences
- order support
- normalize payloads
- perform bounded surface shaping

It must not:

- salvage semantics from fallback text
- infer strategy from raw support
- widen a blocked claim into lesson-style prose

Current proving rule:

- renderer/API/frontend work should now proceed only as vertical proof over
  already re-earned stable slices
- new renderer, planner, or certification abstractions should be opened only
  when one of those exact vertical slices fails and names the missing boundary

## What Must Stop

The rewrite explicitly rejects:

- strategy-name-first validators
- feature-weighted theme selection as the main semantic core
- player-facing policy that implicitly rebuilds objects from branchy family code
- planner logic that mixes semantic synthesis, legality, ranking, and rhetoric
- renderer-level semantic revival

## Legacy Authority Policy

The following docs are not rewrite design authorities:

- `CommentaryProgramMap.md`
- `CommentaryPipelineSSOT.md`
- `CommentaryTruthGate.md`
- `CommentaryTrustHardening.md`

They remain important for:

- current-state audit
- truth spine reference
- migration risk awareness
- blocker preservation

They do not define the rewrite architecture.

## Migration Doctrine

The rewrite is demolition-first, not adapter-first.

That means:

- remove legacy topology before building the new one
- allow temporary compile break during demolition
- keep only the red truth spine
- do not preserve old helper forests just because tests still reference them

## Demolition Boundary

The demolition line is not abstract. During this phase, the rewrite explicitly
demotes and removes L3-L7 legacy topology such as:

- `PlanMatcher`
- `StrategyPackBuilder`
- `StrategyPackSurface`
- `PlayerFacingTruthModePolicy`
- `MainPathMoveDeltaClaimBuilder`
- `QuietMoveIntentBuilder`
- `CertifiedDecisionFrameBuilder`
- `QuestionFirstCommentaryPlanner`
- legacy narrative / outline / replay / compression helper forests

Legacy truth-gate and trust-hardening constraints still bind as migration
guardrails until object-native certification replaces their release controls.

## Red Spine

The rewrite must preserve only the minimal hard spine:

- truth core tests
- catastrophic blocker tests
- probe contract validity
- curated end-to-end acceptance corpus

Topology-preserving legacy tests are not protected by this document.

## Immediate Rewrite Priorities

1. establish this document as canonical authority
2. archive or delete legacy topology tests
3. preserve exact-board fixture corpus
4. demolish L3-L7 legacy semantic topology
5. reintroduce object synthesis through a small number of certified object
   families

## First Reconstitution Targets

The first object families to reconstitute should be:

- `FixedTargetComplex`
- `TradeInvariant`

These map cleanly onto already-known exact cases while testing both static
target and task-preserving trade semantics.
