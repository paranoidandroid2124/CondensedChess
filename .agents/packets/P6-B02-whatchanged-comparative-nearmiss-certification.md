# Packet: P6-B02 WhatChanged comparative near-miss certification

## Scope

- phase anchor: `Phase 6`
- basis / surface: certification-owned comparative near-miss fail-closed
- families in scope:
  - `FixedTargetComplex` + `RestrictionShell` shared-target comparative support
  - shallow comparative rows from `P6-A02a` (`DevelopmentCoordinationState`,
    `RedeploymentRoute`)

## Goal

Close the certification boundary for `WhatChanged` comparative near-miss so
comparative evidence may remain admissible, but only `comparative_primary` can
own the `WhatChanged` primary axis; near-miss or shallow comparative must
demote to `SupportOnly` or `Deferred` and never own the primary lane.

## Non-goals

- no planner admission changes in this packet
- no renderer or wording changes
- no broad comparative reopening beyond the packet-owned exact rows
- no legacy topology resurrection
- no raw board reopening below the canonical boundary

## Touched Files

- runtime:
  - `modules/llm/src/main/.../ClaimCertification.scala`
- tests:
  - `modules/llm/src/test/.../ClaimCertificationTest.scala`
- corpus:
  - packet-owned comparative near-miss / shallow rows only if missing
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `.agents/packets/queue.md`
  - `.agents/README.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` only if runtime trust
    meaning moves

## Exact Rows

- positive:
  - exact comparative-support control from `P9-A01`
- negative:
  - `K03A`
- near_miss:
  - `shared-target-support-near-miss`
  - shallow comparative rows from `P6-A02a` (support-only expected)
- nasty_negative:
  - comparative-support wrong restriction support must remain closed

Only keep the row classes that actually apply to the packet.

## Validation

- `sbt -batch "llm/testOnly lila.llm.strategicobject.ClaimCertificationTest"`
- `sbt -batch "llm/testOnly lila.llm.strategicobject.QuestionPlannerTest"`
  (planner must still read certified outcomes without reopening primary)
- `sbt -batch "llm/compile"`

## Exit Criteria

- near-miss comparative is certified `SupportOnly` or `Deferred` and never
  `comparative_primary`
- exact comparative-support remains certified
- packet non-goals remain true
- required doc sync lands in the same change if runtime meaning moved

## Review Focus

- scope creep risks: planner demotion logic sneaking into certification
- boundary risks: near-miss or shallow comparative treated as primary
- legacy resurrection risks: old readiness-only certification mapping

## Certify Focus

- object admission: unchanged
- delta witness: comparative near-miss does not satisfy `WhatChanged` primary
  burden
- counterpart admissibility: unchanged
- planner ownership: no primary `WhatChanged` ownership from near-miss
- promotion / defer: no new promotion; near-miss stays support-only or deferred

## Status Notes

- start status: `ready`
- result: `passed`
- pass condition:
  - certified near-miss comparative demotes to `SupportOnly` / `Deferred` and
    cannot own `WhatChanged` primary
- blocked condition:
  - near-miss comparative still certifies as primary, or certification must
    widen beyond the packet-owned exact rows to keep it closed
