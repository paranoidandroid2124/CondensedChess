# Packet: P9-R05 blocked slice rerun

## Scope

- phase anchor: `Phase 8/9`
- basis / surface: rerun the two blocked vertical slices after composite
  semantics land
- packets in scope:
  - `P8-R02-exact-target-campaign-e2e`
  - `P8-R03-bounded-favorable-simplification-e2e`

## Goal

Rerun the two blocked vertical slices after `P5-U01` and `P6-B01` land, using
only the newly landed composite semantics and no fresh infra, to see whether
the exact blockers are actually resolved.

## Non-goals

- no new gate packet before the rerun proves it is needed
- no broad renderer or planner overhaul
- no Tier 2 / Tier 3 widening beyond what `P5-U01` and `P6-B01` already land
- no legacy topology resurrection

## Touched Files

- runtime:
  - only if rerun evidence shows one of the blocked slices now passes
- tests:
  - rerun-specific exact-row proof tests
- corpus:
  - only packet-owned rerun expectations if needed
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `.agents/packets/queue.md`
  - `.agents/README.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` only if runtime trust meaning moves

## Exact Rows

- rerun target campaign rows from `P8-R02`
- rerun simplification isolation rows from `P8-R03`

## Validation

- packet-owned rerun `sbt testOnly`
- relevant planner / shell regression `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- rerun either closes `P8-R02` / `P8-R03`, or names one remaining exact
  missing boundary with no extra infra drift
- no fresh gate packet is created unless the rerun still fails and the missing
  boundary is exact and explicit

## Review Focus

- scope creep risks: sneaking in new infra instead of rerunning exact slices
- boundary risks: re-explaining old blockers without exact rerun proof
- legacy resurrection risks: broad fallback or campaign wording salvage

## Certify Focus

- object admission: consume only already landed composite semantics
- delta witness: unchanged except where `P5-U01` / `P6-B01` explicitly land
- planner ownership: rerun must show whether the exact blocked slices now pass
- promotion / defer: packet closes only the blocked rerun verdicts

## Status Notes

- start status: `ready`
- gate:
  - run only after `P5-U01` and `P6-B01` land or are explicitly resolved
- finish status: `passed` on `2026-04-11`
- result:
  - `P8-R02` now passes: the packet-owned `d6` target campaign survives
    `WhatMattersHere`, `WhyThis`, and `WhatChanged` to thin shell on the
    landed `P6-B01` continuity witness
  - `P8-R03` now passes: `curated-exact:k09b` keeps one isolated bounded
    favorable-simplification primary explanation to thin shell on the landed
    `P5-U01` boundary
  - no runtime or trust-behavior change was needed; rerun closed both slices
    with tests plus status/doc sync only
- pass condition:
  - `P8-R02` and/or `P8-R03` are rerun on exact rows and their new verdict is
    grounded in landed composite semantics
- blocked condition:
  - one or both slices still fail and a new exact boundary must be named
