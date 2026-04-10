# Packet: P8-R03 bounded favorable simplification e2e

## Scope

- phase anchor: `Phase 8/9`
- basis / surface: bounded favorable-simplification end-to-end proof
- families in scope:
  - `TradeInvariant`
  - existing packet-bounded support only where already certified by `P9-A03`

## Goal

Prove that one bounded favorable-simplification explanation can survive
end-to-end from certified move-local delta to user-visible shell without
broadening into generic conversion, endgame, or task-shift prose.

## Non-goals

- no Tier 2 / Tier 3 widening unless this packet explicitly says so
- no new favorable-simplification admission outside the current exact slice
- no conversion doctrine or endgame reopening
- no legacy topology resurrection
- no raw board reopening below the current canonical boundary

## Touched Files

- runtime:
  - thin shell / delivery files needed to carry the certified simplification slice only
- tests:
  - packet-owned end-to-end simplification tests
- corpus:
  - packet-owned simplification expectations if needed
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `.agents/packets/queue.md`
  - `.agents/README.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` only if runtime trust meaning moves

## Exact Rows

- positive:
  - the exact bounded favorable-simplification control from `P9-A03`
- negative:
  - exact rows where the same trade does not preserve the same local task
- contrastive:
  - target-led or task-shift lookalikes from the `P9-A03` packet
- near_miss:
  - rows where simplification looks pleasant but is not bounded favorable simplification
- nasty_negative:
  - heavy-piece-release or broad endgame-inflation lookalikes

## Validation

- packet-owned end-to-end favorable-simplification `sbt testOnly`
- relevant planner / shell regression `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- the bounded favorable-simplification slice survives end-to-end without broad conversion or endgame inflation
- negative, contrastive, near-miss, and nasty-negative rows remain fail-closed
- required doc sync lands in the same change if runtime meaning moved

## Review Focus

- scope creep risks: conversion doctrine or endgame inflation
- boundary risks: shell rephrasing that drops the bounded same-task constraint
- legacy resurrection risks: old simplification or endgame fallback families

## Certify Focus

- object admission: unchanged
- delta witness: same-task bounded favorable-simplification witness remains intact
- counterpart admissibility: unchanged
- planner ownership: `WhyThis` stays bounded and packet-specific
- promotion / defer: no new promotion; one exact user-visible proof only

## Status Notes

- start status: `ready`
- finish status: `blocked` on `2026-04-09`
- failed validation:
  - `sbt -batch "llm/testOnly *FavorableSimplificationAdmissionTest"` with a
    packet-owned shell-bound assertion on `curated-exact:k09b`
- exact missing boundary:
  - the exact `TradeInvariant-white-center-e6-de:movelocal` simplification
    claim does survive certification and planner admission, but the current
    `WhyThis` primary payload still carries unrelated move-local claim ids from
    `AccessNetwork`, `FixedTargetComplex`, and the opponent-side
    `TradeInvariant`
  - because the thin shell mirrors planner `claimIds`, one bounded favorable-
    simplification explanation cannot reach the shell in isolation without a
    new exact primary-claim selection boundary
  - follow-on ownership is now transferred to
    `P5-U01-trade-invariant-primary-simplification`; do not answer this
    blocker by broadening generic simplification or endgame doctrine
- pass condition: one bounded favorable-simplification slice survives end-to-end without inflation
- blocked condition: shell delivery cannot preserve the bounded same-task meaning without new exact boundary work
