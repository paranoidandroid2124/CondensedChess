# Packet: P8-R02 exact target campaign e2e

## Scope

- phase anchor: `Phase 8/9`
- basis / surface: fixed-weakness target campaign end-to-end proof
- families in scope:
  - `FixedTargetComplex`
  - `RestrictionShell`
  - `CriticalSquareComplex`
  - `AccessNetwork` only where already certified by the existing slices

## Goal

Prove that one fixed-weakness target campaign can survive end-to-end across
`WhatMattersHere`, `WhyThis`, and `WhatChanged` by consuming the already
re-earned exact comparative-support, target-fixation, and current-position
fixed-target slices without reopening new semantics.

## Non-goals

- no Tier 2 / Tier 3 widening unless this packet explicitly says so
- no new target-family admission rules
- no new comparative-quality or planner gate packet unless this packet fails on an exact slice
- no legacy topology resurrection
- no raw board reopening below the current canonical boundary

## Touched Files

- runtime:
  - thin shell / delivery files needed to carry the certified target campaign only
- tests:
  - packet-owned end-to-end target campaign tests
- corpus:
  - packet-owned exact target-campaign expectations if needed
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `.agents/packets/queue.md`
  - `.agents/README.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` only if runtime trust meaning moves

## Exact Rows

- positive:
  - the exact comparative-support control from `P9-A01`
  - the exact target-fixation control from `P9-A02`
  - the exact current-position fixed-target probe control from `P9-A04`
- negative:
  - `K03A`
- contrastive:
  - the exact compared-move support control from `P9-A01`
- near_miss:
  - `K09E`
- nasty_negative:
  - pressure-only or target-touch-only rows that must not become a full target campaign

## Validation

- packet-owned end-to-end target campaign `sbt testOnly`
- relevant planner / shell regression `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- one fixed-weakness campaign survives end-to-end across `WhatMattersHere`, `WhyThis`, and `WhatChanged`
- certified target slices remain distinct and do not collapse into one broad target slogan
- no support-only or pressure-only row becomes a target campaign owner
- required doc sync lands in the same change if runtime meaning moved

## Review Focus

- scope creep risks: broad target doctrine reopening
- boundary risks: mixing target fixation, target probe, and comparative support into one unsliced shell
- legacy resurrection risks: old campaign wording or replay salvage paths

## Certify Focus

- object admission: unchanged
- delta witness: each target slice keeps its own witness burden
- counterpart admissibility: comparative support stays packet-bounded
- planner ownership: `WhatMattersHere`, `WhyThis`, and `WhatChanged` remain slice-correct
- promotion / defer: no new family promotion; campaign proof only

## Status Notes

- start status: `ready`
- end status: `blocked`
- blocked on: `2026-04-09`
- exact missing boundary:
  - the current exact `WhatMattersHere` fixed-target slice from `P9-A04` is
    anchored on `c6`, while the current exact `WhyThis` target-fixation slice
    from `P9-A02` and exact `WhatChanged` comparative-support slice from
    `P9-A01` are anchored on `d6`
  - the current packet-scoped runtime therefore does not expose one real exact
    shared-target campaign that survives across all three axes; it exposes
    three independent exact lanes
  - without either an already-certified current-position `d6` fixed-target
    probe or another packet-allowed shared-target continuity boundary, this
    packet cannot truthfully pass
  - follow-on ownership is now transferred to
    `P6-B01-shared-target-continuity-certification`; do not answer this
    blocker by reopening broad `TransitionBridge` semantics
- pass condition: one fixed-weakness target campaign survives end-to-end without new semantics
- blocked condition: the campaign cannot survive shell delivery without naming a missing exact boundary first
