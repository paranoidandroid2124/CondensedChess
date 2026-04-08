# Packet: P9-A01 exact comparative support

## Scope

- phase anchor: `Phase 9`
- basis / surface: one exact comparative-support slice on the new spine
- families in scope:
  - exactly one narrow comparative-support slice chosen from previously known
    exact successes

## Goal

Re-earn one exact comparative-support success on the new object -> delta ->
certification -> planner spine without reviving legacy topology. This is the
first proof that the landed spine can express a genuinely instructional
comparative explanation, not just an internally valid comparative object.

## Non-goals

- no broad comparative rollout
- no second slice in the same packet
- no family-sprawl expansion
- no renderer/UI polish

## Touched Files

- runtime:
  - only the minimal runtime files needed by the chosen slice
- tests:
  - exact slice tests and planner/admission tests for the chosen slice
- corpus:
  - exact positive, negative, and contrastive rows for the chosen slice
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - packet docs if ownership moves

## Exact Rows

- positive:
  - one exact comparative-support row for the selected slice
- negative:
  - same-board false wording or false-slice variants
- contrastive:
  - one real alternative where the support thesis differs
- near_miss:
  - structurally similar board where the support slice should not surface

## Validation

- packet-specific `sbt testOnly` commands for the chosen slice
- canonical planner/certification tests if touched
- `sbt -batch "llm/compile"`

## Exit Criteria

- the selected slice is recovered on the new spine only
- no legacy adapter or raw semantic reopening is introduced
- comparative support is instructional rather than merely admissible

## Review Focus

- legacy slice resurrection through adapters
- broader rollout hidden inside a narrow-slice packet

## Certify Focus

- chosen slice is exact-board grounded
- support thesis is genuinely distinct from nearby negative or near-miss rows

## Status Notes

- start status: `ready`
- pass condition: one exact comparative-support slice is rebuilt on the new
  spine only
- blocked condition: the selected slice still depends on legacy topology or
  lacks exact-board contrast closure
