# Strategic Object Rewrite Roadmap

This document is the execution roadmap for the rewrite governed by
`StrategicObjectModel.md`.

It is intentionally demolition-first.

## Rewrite Principles

- legacy topology must not be preserved by default
- compile break is acceptable during demolition
- old topology tests must not force old module resurrection
- fixture corpus is more important than legacy harness code
- truth spine survives; topology scaffolding does not

## Phase 0. Authority Shift

Goals:

- establish `StrategicObjectModel.md` as the rewrite north-star
- mark legacy commentary docs as migration references only
- stop using legacy SSoT docs as design authority for new work

Outputs:

- canonical north-star doc
- updated contributor instructions

## Phase 1. Red Spine Extraction

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

## Phase 2. Fixture Bank Preservation

Goals:

- extract exact FEN/PGN/branch-key phenomenon data from useful legacy tests
- preserve examples while deleting harnesses

Each preserved fixture should keep:

- FEN
- PGN move index when relevant
- positive or negative label
- expected phenomenon
- branch key when known

## Phase 3. Demolition

Goals:

- remove legacy semantic topology before new synthesis begins

Primary demolition targets:

- `PlanMatcher`
- `StrategyPackBuilder`
- `StrategyPackSurface`
- `PlayerFacingTruthModePolicy`
- `MainPathMoveDeltaClaimBuilder`
- `QuietMoveIntentBuilder`
- `CertifiedDecisionFrameBuilder`
- `QuestionFirstCommentaryPlanner`

This phase may leave compile red on purpose.

## Phase 4. Skeleton Layout

Goals:

- leave only the new architecture boundary in source form

Required skeleton:

- `RawPositionEvidence`
- `StrategicObjectSynthesizer`
- `StrategicObjectDeltaProjector`
- `ClaimCertification`
- `QuestionPlanner`
- `Renderer`

At this point, old topology should no longer be tempting to restore.

## Phase 5. Primitive Extraction

Goals:

- define primitive extraction from existing low-level evidence

Examples:

- target square
- break axis
- access route
- exchange square
- entry square
- defended resource

The key rule is primitive-first, not strategy-name-first.

## Phase 6. First Object Families

Initial families to implement:

- `FixedTargetComplex`
- `TradeInvariant`

Why:

- they map to already-known exact cases
- they stress both current-position and move-local semantics
- they test whether the new architecture can replace existing narrow slices

## Phase 7. Delta Projection

Goals:

- compute:
  - move-local delta
  - position-local probe
  - comparative delta

These deltas should be object-native, not family-helper-native.

## Phase 8. Certification

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

## Phase 9. Planner Rebuild

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

## Phase 10. Renderer Thin Shell

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

## Phase 11. Reconstitution Pass

Rebuild the already-proven semantics on the new architecture first.

Suggested order:

1. exact target fixation
2. bounded favorable simplification
3. current-position fixed target probe
4. exact comparative support
5. current-position coordination probe

The rewrite should earn back old exact successes from the new architecture,
not preserve them through adapters.

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
