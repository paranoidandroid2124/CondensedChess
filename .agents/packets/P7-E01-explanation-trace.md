# Packet: P7-E01 explanation trace

## Scope

- phase anchor: `Phase 7`
- basis / surface: evaluation trace export
- lane in scope:
  - `src/test` / research-facing tooling only

## Goal

Create a canonical pre-sanitization trace artifact for exact-board explanation
evaluation so regressions can be localized to planner, certification,
counterpart selection, or delta witness instead of only being observed in final
payloads.

## Non-goals

- no runtime payload widening
- no new user-facing fields
- no planner semantics widening beyond trace emission
- no move generation or external service integration

## Touched Files

- test/tooling:
  - `modules/llm/src/test/...` trace helpers and/or exporters
  - test resources if trace fixtures are needed
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/CommentaryTrustHardening.md`

## Exact Rows

- positive:
  - trace rows for exact and contrastive cases
- negative:
  - trace rows for near-miss and nasty-negative cases

## Validation

- packet-specific test/export command
- canonical `sbt testOnly` bundle if shared runtime contracts moved

## Exit Criteria

- one trace row can show family, readiness, scope, typed projection, witness,
  certification result, planner bucket, and case type
- trace lives outside runtime payload contracts

## Review Focus

- runtime/test boundary blur
- trace schema that is too wording-specific

## Certify Focus

- localizability of exact-board failures
- no trust-risk leak into user-facing carriers

## Status Notes

- start status: `ready`
- pass condition: pre-sanitization explanation trace is available for eval
- blocked condition: planner/cert fields are still too unstable to trace
