# Packet: P7-Q01 question admission matrix

## Scope

- phase anchor: `Phase 7`
- basis / surface: planner semantics
- question kinds in scope:
  - `WhyThis`
  - `WhatChanged`
  - `WhatMattersHere`
  - `WhatMustBeStopped`

## Goal

Define a canonical question-admission matrix so the planner consumes certified
typed delta rather than inventing semantics from scope, family names, or broad
salience alone.

## Non-goals

- no renderer wording
- no UI copy
- no external audience model
- no `WhyNow` in this packet

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/QuestionPlanner.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/ClaimCertification.scala` if claim fields need planner-facing tightening
- tests:
  - `modules/llm/src/test/scala/lila/llm/strategicobject/QuestionPlannerTest.scala`
  - `modules/llm/src/test/scala/lila/llm/strategicobject/ClaimCertificationTest.scala`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `modules/llm/docs/CommentaryTrustHardening.md`

## Exact Rows

- positive:
  - question-kind rows aligned to certified typed deltas
- negative:
  - scope-only shells rejected
  - support-only claims blocked from primary lanes

## Validation

- canonical `sbt testOnly` bundle
- `sbt -batch "llm/compile"`

## Exit Criteria

- planner primary admission is based on certified typed delta
- support-only and deferred claims do not drift into primary lanes
- no question kind relies on broad scope alone

## Review Focus

- question semantics recreated in planner instead of consumed from upstream
- wording creep

## Certify Focus

- planner ownership and admission
- exact-slice discipline

## Status Notes

- start status: `ready`
- pass condition: question admission is typed-delta-native
- blocked condition: certification contract still too weak
