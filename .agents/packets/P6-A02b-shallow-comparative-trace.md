# Packet: P6-A02b shallow comparative trace

## Scope

- phase anchor: `Phase 7`
- basis / surface: trace and tail-risk localization for shallow comparative
- lane in scope:
  - `src/test` / research-only trace tooling

## Goal

Teach the trace and tail-risk tooling to localize shallow-comparative failures
as certification or planner outcomes rather than collapsing them into generic
absence. This packet consumes the `P6-A02a` corpus and makes the failure mode
visible to evaluation.

## Non-goals

- no runtime payload widening
- no certification outcome policy in this packet
- no renderer/UI work

## Touched Files

- test/tooling:
  - `modules/llm/src/test/scala/lila/llm/tools/strategicobject/...`
  - `modules/llm/src/test/resources/strategic-object-corpus/delta-expectations.jsonl`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - packet docs if localization stages change materially

## Exact Rows

- planner_negative:
  - shallow comparative rows that should surface as `planner_none`
- near_miss:
  - rows that should localize at certification or planner without collapsing
    into plain absence
- contrastive:
  - strong comparative rows that still reach planner-primary when appropriate

## Validation

- `sbt -batch "llm/testOnly lila.llm.tools.strategicobject.StrategicObjectExplanationTraceSupportTest"`
- `sbt -batch "llm/Test/runMain lila.llm.tools.strategicobject.StrategicObjectExplanationTraceRunner --tail-risk --tail-risk-threshold=0.98"`

## Exit Criteria

- shallow-comparative rows have a distinct localization path in trace/tail-risk
- tail-risk can fail on shallow comparative planner leaks without pretending the
  row was absent upstream

## Review Focus

- runtime/test boundary blur
- localization that is too vague to be actionable

## Certify Focus

- trace localization distinguishes absence from admitted-but-shallow contrast
- tail-risk gate can target the new shallow-comparative rows

## Status Notes

- start status: `ready`
- pass condition: trace/tail-risk localize shallow comparative failures
  explicitly
- blocked condition: the trace contract still cannot tell shallow comparative
  from generic absence
