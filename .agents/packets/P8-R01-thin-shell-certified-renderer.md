# Packet: P8-R01 thin-shell certified renderer

## Scope

- phase anchor: `Phase 8`
- basis / surface: thin-shell certified renderer / API / frontend delivery
- families in scope:
  - already re-earned stable slices only
  - `FixedTargetComplex`
  - `RestrictionShell`
  - `TradeInvariant`
  - `DevelopmentCoordinationState`

## Goal

Prove that renderer, API, and frontend delivery can consume already certified
planner output from the landed stable slices without semantic salvage,
support-only promotion, or cross-slice drift.

## Non-goals

- no Tier 2 / Tier 3 widening unless this packet explicitly says so
- no new object, delta, certification, or planner ownership rules
- no new exact slice admission
- no legacy topology resurrection
- no raw board reopening below the current canonical boundary

## Touched Files

- runtime:
  - renderer / API / frontend thin-shell files only
- tests:
  - renderer / API / frontend packet-owned exact-slice tests
- corpus:
  - packet-owned presentation expectations only if needed
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `.agents/packets/queue.md`
  - `.agents/README.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` only if runtime trust meaning moves

## Exact Rows

- positive:
  - one exact comparative-support slice from `P9-A01`
  - one exact target-fixation slice from `P9-A02`
  - one exact bounded favorable-simplification slice from `P9-A03`
  - one exact current-position fixed-target probe from `P9-A04`
  - one exact current-position coordination probe from `P9-A05`
- negative:
  - support-only or scope-shell rows that must not become primary at the shell
- near_miss:
  - `K03A`
  - `K09E`
- nasty_negative:
  - single-active-piece mirage

## Validation

- targeted renderer / API / frontend `sbt testOnly` commands for the packet-owned shell tests
- `sbt -batch "llm/compile"`

## Exit Criteria

- certified primary/support ownership survives shell delivery unchanged
- support-only material stays support-only at the shell
- no semantic salvage or fallback truth rewrite is reintroduced
- required doc sync lands in the same change if runtime meaning moved

## Review Focus

- scope creep risks: shell work drifting into new semantics
- boundary risks: renderer/API/frontend rebuilding strategy from raw or support-only material
- legacy resurrection risks: reintroducing old commentary salvage paths

## Certify Focus

- object admission: unchanged
- delta witness: unchanged
- counterpart admissibility: unchanged
- planner ownership: shell preserves certified primary/support/no-admission exactly
- promotion / defer: no new promotion; only shell proof

## Status Notes

- start status: `ready`
- end status: `passed`
- pass condition: thin shell delivery remains semantically thin on already re-earned stable slices
- blocked condition: shell delivery requires new semantics or reintroduces salvage to stay user-visible
- completion:
  - Bookmaker renderer/API/frontend shell is thin on certified planner output
  - Bookmaker controller/frontend path no longer exports/decodes/reconstructs
    `strategyPack` / `signalDigest`
  - canonical thin-shell renderer mirrors planner `claimIds/supportClaimIds`
  - board-anchored shell tests cover the re-earned stable slices and hold
    closure on `K03A`, `K09E`, and the single-active-piece mirage
