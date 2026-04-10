# Packet: P5-U01 trade invariant primary simplification

## Scope

- phase anchor: `Phase 5/8`
- basis / surface: narrow `TradeInvariant` reopen for primary simplification
- families in scope:
  - `TradeInvariant`
  - packet-bounded support from `FixedTargetComplex` / `AccessNetwork` only
    where already certified by the existing exact slice

## Goal

Reopen `TradeInvariant` narrowly so one bounded favorable-simplification claim
can become the primary `WhyThis` explanation on the exact `curated-exact:k09b`
lane instead of surviving only as one member of a mixed move-local payload.

## Non-goals

- no broad conversion doctrine
- no endgame or generic simplification reopening
- no new general primary-claim selection framework beyond the packet-owned
  exact boundary
- no legacy topology resurrection
- no raw board reopening below the canonical boundary

## Touched Files

- runtime:
  - certification / planner / renderer files only if the packet-owned exact
    boundary needs a narrow composite rule
- tests:
  - packet-owned simplification primary-isolation tests
- corpus:
  - packet-owned exact simplification expectations if needed
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `.agents/packets/queue.md`
  - `.agents/README.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` only if runtime trust meaning moves

## Exact Rows

- positive:
  - `curated-exact:k09b`
- negative:
  - exact rows where the same trade does not preserve the same local task
- contrastive:
  - target-led or task-shift lookalikes from `P9-A03`
- near_miss:
  - rows where simplification looks pleasant but is not bounded favorable
    simplification
- nasty_negative:
  - heavy-piece-release or broad endgame-inflation lookalikes

## Validation

- packet-owned simplification primary-isolation `sbt testOnly`
- relevant planner / shell regression `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- one bounded favorable-simplification claim can survive shell delivery as the
  primary `WhyThis` explanation on `curated-exact:k09b`
- unrelated move-local claims demote to support-only or stay out of the primary
  simplification payload
- negative, contrastive, near_miss, and nasty-negative rows remain fail-closed
- required doc sync lands in the same change if runtime meaning moved

## Review Focus

- scope creep risks: broad simplification doctrine or endgame inflation
- boundary risks: generic primary-claim selection spilling beyond the packet
  row
- legacy resurrection risks: old conversion or endgame fallback language paths

## Certify Focus

- object admission: unchanged except for the narrow composite `TradeInvariant`
  burden on the packet row
- delta witness: same-task bounded favorable-simplification witness remains
  intact
- planner ownership: `WhyThis` can isolate the simplification claim without
  reopening generic move-local primary selection
- promotion / defer: no family-wide promotion; packet-owned reopen only

## Status Notes

- start status: `ready`
- finish status: `passed` on `2026-04-10`
- pass condition:
  - one bounded favorable-simplification claim owns the primary `WhyThis`
    explanation on `curated-exact:k09b` without broad doctrine
- outcome:
  - the packet-owned exact boundary is now closed
  - unrelated move-local claims were demoted out of the primary simplification
    payload
  - `P8-R03` remains blocked only until the rerun confirms the shell outcome
