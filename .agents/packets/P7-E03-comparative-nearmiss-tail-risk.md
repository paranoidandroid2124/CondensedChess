# Packet: P7-E03 comparative near-miss tail-risk gate

## Scope

- phase anchor: `Phase 7`
- basis / surface: explanation trace + tail-risk gate for comparative near-miss
- families in scope:
  - `FixedTargetComplex` + `RestrictionShell` shared-target comparative support
  - shallow comparative rows from `P6-A02a`

## Goal

Expose comparative near-miss demotion in the explanation trace and add a
tail-risk gate that hard-fails any `WhatChanged` primary leakage from
near-miss or support-only comparative rows.

## Non-goals

- no certification outcome changes in this packet
- no planner admission changes in this packet
- no renderer or wording changes
- no legacy topology resurrection
- no raw board reopening below the canonical boundary

## Touched Files

- runtime:
  - none
- tests / tools:
  - `modules/llm/src/test/.../StrategicObjectExplanationTraceSupport.scala`
  - `modules/llm/src/test/.../StrategicObjectExplanationTraceSupportTest.scala`
  - `modules/llm/src/test/.../StrategicObjectExplanationTraceRunner.scala`
- corpus:
  - packet-owned trace expectations for near-miss comparative rows
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `.agents/packets/queue.md`
  - `.agents/README.md`

## Exact Rows

- positive:
  - exact comparative-support control from `P9-A01`
- negative:
  - `K03A`
- near_miss:
  - `shared-target-support-near-miss`
  - shallow comparative rows from `P6-A02a` (trace must show demotion)
- nasty_negative:
  - synthetic near-miss leak for tail-risk hard-fail

Only keep the row classes that actually apply to the packet.

## Validation

- `sbt -batch "llm/testOnly lila.llm.tools.strategicobject.StrategicObjectExplanationTraceSupportTest"`
- `sbt -batch "llm/Test/runMain lila.llm.tools.strategicobject.StrategicObjectExplanationTraceRunner --tail-risk --tail-risk-threshold=0.98"`

## Exit Criteria

- trace shows comparative near-miss demotion reason and localization
- tail-risk gate hard-fails any `WhatChanged` primary leak from near-miss
- packet non-goals remain true

## Review Focus

- scope creep risks: runtime payload widening
- boundary risks: trace silently dropping near-miss demotion state
- legacy resurrection risks: old tail-risk definitions ignoring near-miss

## Certify Focus

- object admission: unchanged
- delta witness: trace reflects certified comparative outcome accurately
- planner ownership: trace must show demotion/none for near-miss
- promotion / defer: no new promotion

## Status Notes

- status: `passed`
- result:
  - trace/tail-risk explicitly exposes comparative near-miss demotion and
    hard-fails leakage from near-miss or support-only comparative rows
