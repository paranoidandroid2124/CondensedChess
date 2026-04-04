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
- break axis
- entry square
- exchange square
- route
- outpost
- passer potential

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
