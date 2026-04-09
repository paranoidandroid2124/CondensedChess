# Packet: P6-A02c comparative certification outcomes

## Scope

- phase anchor: `Phase 6`
- basis / surface: certification and planner outcomes for shallow comparative
- families in scope:
  - Tier 1 comparative lanes

## Goal

Define the canonical certification/planner outcome matrix for admissible-but-
shallow comparative rows after `P6-A02a` corpus and `P6-A02b` trace
localization exist. This packet decides when such rows become `SupportOnly`,
`Deferred`, or `planner_none`, and when strong exact contrast remains
`Certified`.

## Non-goals

- no new projector-side separability logic
- no wording or renderer semantics
- no Tier 2 / Tier 3 widening

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/ClaimCertification.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/QuestionPlanner.scala`
- tests:
  - `modules/llm/src/test/scala/lila/llm/strategicobject/ClaimCertificationTest.scala`
  - `modules/llm/src/test/scala/lila/llm/strategicobject/QuestionPlannerTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/strategicobject/StrategicObjectExplanationTraceSupportTest.scala`
- corpus:
  - `modules/llm/src/test/resources/strategic-object-corpus/delta-expectations.jsonl`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `modules/llm/docs/CommentaryTrustHardening.md`

## Exact Rows

- positive:
  - strong exact comparative rows that remain `Certified`
- near_miss:
  - shallow comparative rows that fall to `SupportOnly` or `planner_none`
- planner_negative:
  - rows that must not own a primary contrast
- contrastive:
  - exact rows whose strategic campaign is genuinely distinct and should remain
    primary-capable

## Validation

- `sbt -batch "llm/testOnly lila.llm.strategicobject.ClaimCertificationTest lila.llm.strategicobject.QuestionPlannerTest lila.llm.tools.strategicobject.StrategicObjectExplanationTraceSupportTest"`
- `sbt -batch "llm/Test/runMain lila.llm.tools.strategicobject.StrategicObjectExplanationTraceRunner --tail-risk --tail-risk-threshold=0.98"`
- `sbt -batch "llm/compile"`

## Exit Criteria

- strong exact contrast stays `Certified`
- admissible-but-shallow exact contrast has a canonical fail-closed outcome
- planner primary ownership no longer follows from comparative admissibility
  alone
- `P6-A02` umbrella may be considered resolved

## Review Focus

- certification/planner boundary blur
- hidden projector or wording widening

## Certify Focus

- outcome matrix is exact-board grounded
- strong and shallow comparative cases are clearly separated

## Status Notes

- start status: `ready`
- current status: `passed`
- pass condition: canonical certification/planner outcomes exist for shallow
  comparative rows
- blocked condition: even with corpus and trace localization, certification
  still cannot separate strong contrast from admissible-but-shallow contrast
