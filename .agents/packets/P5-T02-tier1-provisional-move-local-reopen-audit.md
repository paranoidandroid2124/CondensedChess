# Packet: P5-T02 Tier 1 provisional move-local reopen audit

## Scope

- phase anchor: `Phase 5`
- basis / surface: selective provisional move-local reopen audit
- families in scope:
  - the same eight provisional Tier 1 families, but only after comparative
    re-audit has stabilized

## Goal

Audit whether any provisional Tier 1 family can reopen `move_local` under a
strict exact-board transition witness burden. This is a selective reopen pass,
not a blanket upgrade. Families that cannot survive exact positives plus nasty
move-local negatives must remain closed.

## Non-goals

- no blanket move-local reopening
- no Tier 2 / Tier 3 widening
- no renderer/UI work

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectDeltaProjector.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectFamilyContract.scala`
  - certification files only if admission meaning changes
- tests:
  - move-local projector and certification tests for provisional families
- corpus:
  - exact, move_local_false_witness, near_miss, and nasty-negative rows
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` if trust meaning changes

## Exact Rows

- positive:
  - exact move-local positives for any candidate family
- negative:
  - weak transition rows that must stay closed
- near_miss:
  - family-specific move-local near misses
- move_local_false_witness:
  - remains first-class
- nasty_negative:
  - packet-owned move-local inflation rows

## Validation

- packet-specific `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- each reopened move-local lane has strict exact-board witness burden
- families without adequate evidence stay closed explicitly
- promotion/defer rationale is family-specific and exact-board grounded

## Review Focus

- move-local inflation
- readiness drift hidden inside projector helpers

## Certify Focus

- exact-board witness quality for each reopened family
- nasty negative resistance on move-local rows

## Status Notes

- start status: `ready`
- pass condition: only justified provisional families reopen move-local
- blocked condition: exact-board witness burden is still too weak for safe
  provisional move-local reopening
- final status: `blocked`
- final verdict:
  - `KingSafetyShell` -> remain closed
  - `DevelopmentCoordinationState` -> remain closed
  - `PieceRoleFitness` -> remain closed
  - `SpaceClamp` -> remain closed
  - `CounterplayAxis` -> remain closed
  - `RestrictionShell` -> remain closed
  - `MobilityCage` -> remain closed
  - `RedeploymentRoute` -> remain closed
- exact-board outcome:
  - no family currently carries a `move_local` `exact` positive row
  - no family currently carries a `move_local` `nasty_negative` row
  - all eight families do carry `move_local` `near_miss` and
    `move_local_false_witness` rows, and those rows stay fail-closed
  - the packet therefore does not justify selective move-local reopening; the
    missing exact-positive plus move-local nasty-negative burden blocks the
    audit at certification readiness rather than requiring projector or
    certification semantics to change
- frontier note:
  - this blocked evidence burden is orthogonal to the next Phase 8/9 vertical
    tranche because that tranche consumes only already re-earned stable slices
    and does not depend on provisional move-local reopening
- verification note:
  - `sbt -batch "llm/testOnly lila.llm.strategicobject.StrategicObjectDeltaProjectorTest"`
    reran the packet-owned provisional move-local checks successfully, but the
    suite still failed on the pre-existing unrelated
    `delta expectation fixed-target-move-exact`
  - `sbt -batch "llm/compile"` passed
