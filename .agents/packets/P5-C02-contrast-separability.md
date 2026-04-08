# Packet: P5-C02 contrast separability

## Scope

- phase anchor: `Phase 5`
- basis / surface: comparative quality and low-separation rejection
- families in scope:
  - Tier 1 direct delta owners
  - emphasis on provisional comparative lanes

## Goal

Add a canonical separability burden so a comparative pair is admitted only when
the chosen object and counterpart differ strongly enough in typed strategic
meaning. Comparatives that are technically legal but semantically shallow
should close rather than drift into weak player-facing contrast.

## Non-goals

- no planner wording
- no renderer/UI work
- no new foil subsystem outside the current object/delta/cert spine
- no readiness promotion by default

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectDeltaProjector.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectDelta.scala`
- tests:
  - `modules/llm/src/test/scala/lila/llm/strategicobject/StrategicObjectDeltaProjectorTest.scala`
- corpus:
  - `modules/llm/src/test/resources/strategic-object-corpus/delta-expectations.jsonl`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`

## Exact Rows

- positive:
  - high-separation comparative `contrastive`
- negative:
  - low-separation comparative near misses
  - `comparative_false_rival`
  - packet-specific shallow contrast rows

## Validation

- same `sbt testOnly` bundle as `P5-C01`
- `sbt -batch "llm/compile"`

## Exit Criteria

- comparative survives only when typed metrics show meaningful contrast
- low-separation comparative rows stay closed
- exact contrastive rows still survive

## Review Focus

- pseudo-math or string scoring without exact-board grounding
- hidden readiness or planner behavior widening

## Certify Focus

- false-positive reduction on low-separation comparative rows
- survival of exact contrastive positives

## Status Notes

- start status: `ready`
- pass condition: low-separation comparative inflation is reduced without
  killing valid Tier 1 comparative lanes
- blocked condition: separability burden clearly belongs in certification, not
  projector
