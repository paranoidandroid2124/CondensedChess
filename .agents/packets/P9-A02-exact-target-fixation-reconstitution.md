# Packet: P9-A02 exact target fixation reconstitution

## Scope

- phase anchor: `Phase 9`
- basis / surface: one exact target-fixation slice on the new spine
- families in scope:
  - `FixedTargetComplex`
  - closely-related support only where the chosen slice requires it

## Goal

Re-earn one exact target-fixation success on the new spine after exact
comparative support is stable. The slice must prove that a fixed-target claim
can survive object, delta, certification, and planner admission without raw
reconstruction or legacy helper revival.

## Non-goals

- no broad fixed-target rollout
- no new comparative family contract work outside the chosen slice
- no renderer/UI work

## Touched Files

- runtime:
  - only the files required by the chosen exact target-fixation slice
- tests:
  - exact slice tests
- corpus:
  - exact target-fixation rows and nearby negatives
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - packet docs if scope meaning moves

## Exact Rows

- positive:
  - one exact target-fixation positive
- negative:
  - same-board pressure picture without real fixation
- contrastive:
  - fixation vs non-fixation nearby board
- near_miss:
  - target exists but fixation should not surface

## Validation

- packet-specific `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- exact target-fixation slice is recovered on the new spine
- fixation does not surface from broad pressure pictures alone

## Review Focus

- slice sprawl
- hidden raw board reopening

## Certify Focus

- exact fixation survives
- non-fixation pressure remains fail-closed

## Status Notes

- start status: `ready`
- current status: `passed`
- completion note:
  - `FixedTargetComplex` move-local now reopens only from a centralized
    fixation-square witness on the target's exact forward square when the
    object is truly `fixed=true`
  - packet-owned exact and contrastive rows are now board-consistent
    non-capture bishop moves onto the fixation square, and both survive object
    -> delta -> certification -> planner as `WhyThis`
  - packet-owned negative and near-miss rows keep `FixedTargetComplex`
    objectization without any move-local `TargetFixed` delta or planner
    admission when the move only pressures the target but does not create the
    fixation geometry
- pass condition: one exact target-fixation slice is re-earned cleanly
- blocked condition: the slice still depends on legacy fixation-specific
  topology
