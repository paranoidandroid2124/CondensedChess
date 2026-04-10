# Packet: P7-Q03 WhatChanged comparative demotion matrix

## Scope

- phase anchor: `Phase 7`
- basis / surface: planner admission demotion for comparative near-miss
- families in scope:
  - `FixedTargetComplex` + `RestrictionShell` shared-target comparative support
  - shallow comparative rows from `P6-A02a`

## Goal

Enforce a planner demotion matrix so only `comparative_primary` can own the
`WhatChanged` primary axis; `comparative_support_only` and
`comparative_near_miss` must demote to support or defer, and planner must not
reconstruct primary ownership from certified near-miss or shallow evidence.

## Non-goals

- no certification outcome changes in this packet
- no renderer or wording changes
- no broad question-matrix expansion
- no legacy topology resurrection
- no raw board reopening below the canonical boundary

## Touched Files

- runtime:
  - `modules/llm/src/main/.../QuestionPlanner.scala`
- tests:
  - `modules/llm/src/test/.../QuestionPlannerTest.scala`
- corpus:
  - packet-owned comparative near-miss rows only if missing
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
  - shallow comparative rows from `P6-A02a` (must not open `WhatChanged`)
- nasty_negative:
  - wrong-axis comparative support must remain closed

Only keep the row classes that actually apply to the packet.

## Validation

- `sbt -batch "llm/testOnly lila.llm.strategicobject.QuestionPlannerTest"`
- `sbt -batch "llm/testOnly lila.llm.strategicobject.ClaimCertificationTest"`
- `sbt -batch "llm/compile"`

## Exit Criteria

- `WhatChanged` primary is owned only by `comparative_primary`
- near-miss or support-only comparative never opens `WhatChanged` primary
- packet non-goals remain true
- required doc sync lands in the same change if runtime meaning moved

## Review Focus

- scope creep risks: planner tries to invent comparative_primary from
  support-only evidence
- boundary risks: `WhatChanged` admission ignoring certified comparative status
- legacy resurrection risks: old planner heuristics bypassing certification

## Certify Focus

- object admission: unchanged
- delta witness: planner relies on certified comparative status only
- counterpart admissibility: unchanged
- planner ownership: `WhatChanged` primary only on `comparative_primary`
- promotion / defer: no new promotion; near-miss stays demoted

## Status Notes

- start status: `ready`
- end status: `passed` on 2026-04-10
- landed:
  - planner `WhatChanged` primary admission now routes through
    `comparative_primary` only
  - support-only or shallow comparative evidence no longer reconstructs
    `WhatChanged` primary ownership
  - packet-owned shared-target `RestrictionShell` support still pairs on the
    exact / contrastive rows, while the near-miss support row stays demoted
- pass condition:
  - near-miss and support-only comparative are demoted at planner admission
    and never open `WhatChanged` primary
- blocked condition:
  - planner still opens `WhatChanged` primary from near-miss or support-only
    comparative
