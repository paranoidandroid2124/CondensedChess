# Packet Queue

Branch:

- `codex/strategic-object-demolition`

Authority:

- [AGENTS.md](/C:/Codes/CondensedChess/AGENTS.md)
- [StrategicObjectModel.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/StrategicObjectModel.md)
- [StrategicObjectRoadmap.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/StrategicObjectRoadmap.md)
- [CommentaryTrustHardening.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)

## Completed Checkpoints

- authority sync for rewrite and legacy doc roles
- Phase 3 primitive extraction
- Phase 4 pass 1 board-direct objectization
- Phase 4 pass 2 graph-derived objectization
- strategic object hardening and exact-board calibration
- Phase 5 pass 1 Tier 1 direct-delta-owner opening
- Phase 5 pass 1 reinforcement
- Tier 1 provisional hardening / promotion audit
  - all eight remain `Provisional`
- `P5-C01-comparative-counterpart-contract`
  - passed
  - comparative counterpart admissibility is now centralized at the family
    contract boundary

## Active Queue

| Packet | Status | Priority | Basis / Surface | Notes |
|---|---|---:|---|---|
| `P5-C01-comparative-counterpart-contract` | `passed` | 1 | counterpart admissibility | landed on the current packet lane |
| `P5-C02-contrast-separability` | `ready` | 2 | comparative quality | next runtime tranche; keep broad contrast out of player-facing lanes |
| `P6-A01-delta-aware-certification` | `ready` | 3 | certification burden | move beyond readiness-only mapping |
| `P7-Q01-question-admission-matrix` | `ready` | 4 | planner semantics | define typed admission per question kind |
| `P7-Q02-whynow-admission` | `ready` | 5 | planner semantics | open WhyNow only from timing witness |
| `P7-E01-explanation-trace` | `ready` | 6 | eval trace | test/research only, no runtime payload widening |
| `P7-E02-tail-risk-eval` | `ready` | 7 | eval gate | hardest-slice CI, not macro average only |

## Packet Selection Rule

Always pick the highest-priority packet that is:

- `ready`, and
- not blocked by an unfinished earlier packet.

Do not combine packets unless the queue is explicitly edited to do so.

## Status Vocabulary

- `ready`
- `in_progress`
- `blocked`
- `passed`
- `passed_with_defer`

Use `passed_with_defer` when the packet succeeds but a family or lane remains
intentionally conservative.
