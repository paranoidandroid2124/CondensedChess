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
- `P7-Q01-question-admission-matrix`
  - passed
  - planner primary admission now follows a centralized certified typed-delta
    matrix; support-only and scope-shell claims no longer choose the primary
    question lane, and `WhyNow` remained deferred to `P7-Q02`
- `P7-Q02-whynow-admission`
  - passed
  - WhyNow is now a separate certified timing lane from timing-sensitive
    move-local witnesses; support-only timing deltas alone do not open it
- `P7-E01-explanation-trace`
  - passed
  - canonical pre-sanitization explanation trace now exports exact-row and
    nasty-row localizable projector / certification / planner state at the
    test-research boundary without widening runtime payloads

## Active Queue

| Packet | Status | Priority | Basis / Surface | Notes |
|---|---|---:|---|---|
| `P5-C01-comparative-counterpart-contract` | `passed` | 1 | counterpart admissibility | landed on the current packet lane |
| `P5-C02-contrast-separability` | `blocked` | 2 | comparative quality | retry failed to prove a distinct exact-board shallow-contrast row; current burden appears to belong in certification rather than projector |
| `P6-A01-delta-aware-certification` | `passed` | 3 | certification burden | certification now reads typed delta burden; stable weak deltas downgrade to support-only or deferred |
| `P7-Q01-question-admission-matrix` | `passed` | 4 | planner semantics | centralized typed-delta question matrix landed; `WhyNow` remained deferred until `P7-Q02` |
| `P7-Q02-whynow-admission` | `passed` | 5 | planner semantics | timing-sensitive WhyNow lane landed from certified move-local witness |
| `P7-E01-explanation-trace` | `passed` | 6 | eval trace | canonical pre-sanitization explanation trace now exports row-local projector / certification / planner state at the test boundary |
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
