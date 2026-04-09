# Packet: P5-T01 Tier 1 provisional comparative re-audit

## Scope

- phase anchor: `Phase 5`
- basis / surface: Tier 1 provisional comparative re-audit after quality
  closure
- families in scope:
  - `KingSafetyShell`
  - `DevelopmentCoordinationState`
  - `PieceRoleFitness`
  - `SpaceClamp`
  - `CounterplayAxis`
  - `RestrictionShell`
  - `MobilityCage`
  - `RedeploymentRoute`

## Goal

Re-audit the provisional Tier 1 comparative lanes only after comparative-quality
certification and early Phase 9 slice parity exist. The goal is not forced
promotion; it is to determine which provisional comparative lanes now resist
shallow contrast and nasty exact-board negatives strongly enough to justify
promotion or stronger admission.

## Non-goals

- no forced promotion
- no move-local reopening in this packet
- no Tier 2 / Tier 3 widening
- no renderer/UI work

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectFamilyContract.scala`
  - certification/planner files only if readiness meaning changes
- tests:
  - comparative and certification tests for the eight families
- corpus:
  - provisional-family comparative exact/nasty rows
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` if trust meaning changes

## Exact Rows

- positive:
  - exact comparative positives for each provisional family still in scope
- negative:
  - same-campaign or low-separation comparative rows
- near_miss:
  - family-specific comparative near misses
- nasty_negative:
  - packet-owned false-positive pictures by family
- comparative_false_rival:
  - remains part of the gate

## Validation

- packet-specific `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- each provisional family gets an explicit comparative verdict:
  - stay `Provisional`
  - or promote on comparative only
- no family is promoted from verbal motif confidence alone

## Review Focus

- forced promotion
- comparative inflation hidden inside family-specific tweaks

## Certify Focus

- family-by-family exact-board comparative resistance
- promotion or defer rationale remains explicit

## Status Notes

- start status: `ready`
- pass condition: comparative re-audit is explicit and exact-board grounded
- blocked condition: comparative-quality closure is still too weak to support
  family-level promotion decisions
- final status: `passed_with_defer`
- final verdict:
  - `KingSafetyShell` -> stay `Provisional`
  - `DevelopmentCoordinationState` -> stay `Provisional`
  - `PieceRoleFitness` -> stay `Provisional`
  - `SpaceClamp` -> stay `Provisional`
  - `CounterplayAxis` -> stay `Provisional`
  - `RestrictionShell` -> stay `Provisional`
  - `MobilityCage` -> stay `Provisional`
  - `RedeploymentRoute` -> stay `Provisional`
- exact-board outcome:
  - all eight families now carry explicit comparative `near_miss` rows in
    addition to `contrastive`, `nasty_negative`, and
    `comparative_false_rival` coverage
  - shallow comparative stays projector-admissible but localizes at
    `certification` as `SupportOnly` and stays planner `none`
  - no comparative-only promotion was justified on this packet
- verification note:
  - `ClaimCertificationTest` and `QuestionPlannerTest` passed after the packet
    diff
  - `StrategicObjectDeltaProjectorTest` remained blocked by the dirty-worktree
    unrelated failure on `fixed-target-move-exact`; all packet-owned
    provisional comparative rows passed inside that suite
