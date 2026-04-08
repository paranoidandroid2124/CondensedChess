# Packet: P7-E02 tail-risk eval

## Scope

- phase anchor: `Phase 7`
- basis / surface: nasty-slice and tail-risk evaluation
- lane in scope:
  - `src/test` / research-facing tooling only

## Goal

Promote explanation evaluation from aggregate pass/fail toward hardest-slice
control. Add nasty-slice and tail-risk metrics so broad averages no longer hide
the false positives that matter most in exact-board strategic explanation.

## Non-goals

- no runtime semantics widening
- no UI reporting work
- no human-study pipeline in this packet

## Touched Files

- test/tooling:
  - `modules/llm/src/test/...` evaluation helpers or runners
  - `modules/llm/src/test/resources/strategic-object-corpus/...`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/CommentaryTrustHardening.md`

## Exact Rows

- hardest slices only:
  - `near_miss`
  - `nasty_negative`
  - `move_local_false_witness`
  - `comparative_false_rival`
  - packet-specific planner-admission negatives

## Validation

- packet-specific eval command
- canonical `sbt testOnly` bundle if shared tests move

## Exit Criteria

- nasty-slice metrics are emitted separately from macro averages
- hardest-case false positives can fail the packet even when aggregate pass
  rates look good
- exact-board case taxonomy remains the driver

## Review Focus

- runtime/test boundary blur
- weak metrics with no actionability

## Certify Focus

- whether tail-risk metrics actually catch packet-target regressions
- whether nasty exact-board rows remain first-class

## Status Notes

- start status: `ready`
- pass condition: tail-risk becomes a real gate, not a reporting footnote
- blocked condition: trace contract from `P7-E01` is not available
