# Codex Packet Control Plane

This directory is the repo-resident control plane for the
`codex/strategic-object-demolition` rewrite lane.

It exists so new sessions do not need a long chat replay to recover:

- current rewrite frontier
- packet order
- packet scope and non-goals
- review rules
- certification rules

## Authority Order

Always read these first:

1. [AGENTS.md](/C:/Codes/CondensedChess/AGENTS.md)
2. [StrategicObjectModel.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/StrategicObjectModel.md)
3. [StrategicObjectRoadmap.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/StrategicObjectRoadmap.md)

Trust-relevant runtime changes must also update:

- [CommentaryTrustHardening.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)

Legacy commentary docs are migration references only.

## Operating Model

Use a packetized single-writer pipeline.

- `Supervisor`: picks the next packet and enforces scope
- `Writer`: the only writable implementation thread
- `Reviewer`: read-only boundary and diff review
- `Certifier`: read-only exact-board and admission verification

Do not run two writers on the same packet.

## Canonical Files

- [packets/queue.md](/C:/Codes/CondensedChess/.agents/packets/queue.md)
  - packet order and packet status
- [packets/_template.md](/C:/Codes/CondensedChess/.agents/packets/_template.md)
  - canonical packet shape
- [review/code_review.md](/C:/Codes/CondensedChess/.agents/review/code_review.md)
  - reviewer checklist
- [certify/strategic_object_certify.md](/C:/Codes/CondensedChess/.agents/certify/strategic_object_certify.md)
  - certifier checklist

## Minimal Loop

1. Read `queue.md` and pick one packet only.
2. Read the packet plus the authority docs.
3. Implement only inside the packet boundary.
4. Run packet validation.
5. Review against `code_review.md`.
6. Certify against `strategic_object_certify.md`.
7. If blocked, stop and record a blocker note inside the packet or queue entry.
8. If passed, update packet status in `queue.md`.

## Current Rewrite Context

This control plane assumes the following rewrite state is already landed:

- Phase 3 complete
- Phase 4 pass 1 and pass 2 complete
- strategic object hardening and calibration complete
- Phase 5 pass 1 and reinforcement complete
- Tier 1 provisional hardening/promotion audit complete
- Phase 6 delta-aware certification complete
- Phase 7 question admission / `WhyNow` / explanation trace / tail-risk gate
  complete

The next runtime frontier is not more family sprawl.
It is:

1. `P8-R01-thin-shell-certified-renderer`
   - passed
   - Bookmaker renderer/API/frontend thin shell is landed on certified planner
     ownership; this path no longer exports/decodes/reconstructs
     `strategyPack` / `signalDigest`
2. `P8-R02-exact-target-campaign-e2e`
   - blocked
   - current exact slices do not yet form one real shared-target campaign:
     the `WhatMattersHere` probe is `c6`-anchored while the `WhyThis` and
     `WhatChanged` exact controls are `d6`-anchored
3. `P8-R03-bounded-favorable-simplification-e2e`
   - blocked
   - on `curated-exact:k09b`, the exact simplification claim still shares the
     `WhyThis` primary payload with unrelated move-local `AccessNetwork`,
     `FixedTargetComplex`, and opponent-side `TradeInvariant` claims, so the
     thin shell could not isolate one bounded explanation before `P5-U01`;
     that primary-isolation boundary is now closed, and `P9-R05` remains the
     rerun gate
4. `P8-R04-current-position-coordination-e2e`
   - passed_with_defer
   - one bounded `K09A` current-position coordination probe survives end-to-
     end while `K09D`, `K09E`, and single-active-piece mirage remain closed
5. `P5-U01-trade-invariant-primary-simplification`
   - passed
   - one bounded favorable-simplification claim now owns the primary
     `WhyThis` explanation on `curated-exact:k09b`; unrelated move-local
     claims no longer remain in the primary simplification payload
6. `P6-B01-shared-target-continuity-certification`
   - blocked
   - exact missing boundary: the packet-owned `WhatChanged`
     comparative-support lane is not stable enough for continuity
     certification because `shared-target-support-near-miss` still leaks
     support admission / wrong restriction support under the existing exact
     comparative-support slice
7. `P9-R05-blocked-slice-rerun`
   - do not run yet
   - no new packet was opened from this session; `P9-R05` stays behind the
     unresolved `P6-B01` boundary

Current blocking note:

- `P5-T02-tier1-provisional-move-local-reopen-audit` remains `blocked` on
  missing family-complete move-local exact-positive and nasty-negative
  evidence, but that burden is orthogonal to the current vertical tranche
  because the tranche consumes only already re-earned stable slices.

Operational rule:

- the first vertical proving tranche is exhausted:
  - `P8-R02` and `P8-R03` failed on exact slice boundaries
  - `P8-R04` closed in `passed_with_defer`
- `P5-U01` has now closed the bounded favorable-simplification isolation
  boundary, but `P8-R03` still remains blocked and `P6-B01` is now also
  blocked on its own exact comparative-support boundary
- the next packets must therefore derive from those exact failures rather than
  widen infra generically, and `P9-R05` must not run yet
