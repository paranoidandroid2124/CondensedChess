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

The next runtime frontier is not more family sprawl.
It is:

1. comparative / counterpart contract tightening
2. delta-aware certification
3. question-native planner semantics
4. trace-backed evaluation and promotion loop
