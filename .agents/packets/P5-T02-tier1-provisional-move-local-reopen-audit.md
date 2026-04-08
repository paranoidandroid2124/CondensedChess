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
