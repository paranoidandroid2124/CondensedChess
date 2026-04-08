# Packet: P9-A03 bounded favorable simplification

## Scope

- phase anchor: `Phase 9`
- basis / surface: one bounded favorable-simplification slice
- families in scope:
  - the minimal set needed for a single exact simplification slice

## Goal

Re-earn one bounded favorable-simplification slice on the new spine without
letting the claim expand into theorem-level endgame or conversion rhetoric. The
slice must stay narrow, exact-board grounded, and planner-bounded.

## Non-goals

- no endgame theorem rollout
- no broad conversion doctrine
- no replay or renderer widening

## Touched Files

- runtime:
  - only the minimal files required by the selected simplification slice
- tests:
  - exact simplification slice tests
- corpus:
  - positive, negative, and near-miss simplification rows
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` if trust meaning moves

## Exact Rows

- positive:
  - one exact bounded favorable-simplification slice
- negative:
  - simplification picture without favorable contract
- contrastive:
  - same tactical shell but different strategic outcome
- near_miss:
  - simplification exists but the bounded thesis should not surface

## Validation

- packet-specific `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- bounded favorable simplification is re-earned on the new spine
- the slice remains bounded and does not reinflate into broad ending rhetoric

## Review Focus

- endgame verdict inflation
- replay or renderer reopening

## Certify Focus

- simplification thesis is exact-board grounded
- bounded containment is preserved

## Status Notes

- start status: `ready`
- pass condition: one bounded favorable-simplification slice is recovered
  cleanly
- blocked condition: the slice cannot stay bounded on the new spine
