# Packet: P5-C01 comparative/counterpart contract

## Scope

- phase anchor: `Phase 5`
- basis / surface: comparative counterpart admissibility
- families in scope:
  - Tier 1 direct delta owners
  - focus on the provisional Tier 1 families and family-compatible stable
    counterparts

## Goal

Tighten comparative admission so player-facing comparison opens only from a
real same-family or explicitly family-compatible counterpart with exact anchor,
file, square, route, or direct-rival witness. Broad overlap, same sector, and
owner-isolated summary must not reopen comparative.

## Non-goals

- no Tier 2 / Tier 3 direct-owner widening
- no renderer wording or foil prose work
- no move-local reopening for the provisional Tier 1 families
- no new human-policy or external audience model

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectDeltaProjector.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectDelta.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectFamilyContract.scala`
- tests:
  - `modules/llm/src/test/scala/lila/llm/strategicobject/StrategicObjectDeltaProjectorTest.scala`
- corpus:
  - `modules/llm/src/test/resources/strategic-object-corpus/delta-expectations.jsonl`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` if trust boundary changes

## Exact Rows

- positive:
  - Tier 1 comparative `exact`
  - Tier 1 comparative `contrastive`
- negative:
  - `comparative_false_rival`
  - broad-overlap comparative near misses

## Validation

- `sbt -batch "llm/testOnly lila.llm.strategicobject.PrimitiveExtractionTest lila.llm.strategicobject.PrimitiveBoundaryTest lila.llm.strategicobject.StrategicObjectSynthesizerTest lila.llm.strategicobject.StrategicObjectDeltaProjectorTest lila.llm.strategicobject.ClaimCertificationTest lila.llm.strategicobject.QuestionPlannerTest"`
- `sbt -batch "llm/compile"`

## Exit Criteria

- broad-overlap-only rivals no longer admit comparative
- family-compatible counterpart logic is centralized, not ad hoc per caller
- comparative false-rival rows pass
- provisional families remain `Provisional` unless the packet explicitly proves
  otherwise

## Review Focus

- broad helper generalization beyond counterpart admission
- accidental planner semantics creep
- provisional move-local reopening by side effect

## Certify Focus

- counterpart admissibility
- family-aware comparative witness correctness
- exact-board comparative negatives

## Status Notes

- start status: `ready`
- pass condition: comparative contract hardened without widening scope
- blocked condition: counterpart burden cannot be centralized without first
  changing certification semantics
