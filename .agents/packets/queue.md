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
- `P6-A01-delta-aware-certification`
  - passed
  - certification now reads typed delta burden instead of mapping readiness
    directly to release
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
- `P7-E02-tail-risk-eval`
  - passed
  - tail-risk evaluation now gates hardest-slice planner leaks separately from
    macro pass rate, including packet-owned `planner_negative` checks
- `P6-A02a-shallow-comparative-corpus`
  - passed
  - canonical exact-board shallow-comparative rows now exist on the
    same-owner `DevelopmentCoordinationState` / `RedeploymentRoute` pair from
    `exact:redeployment-path`; the comparative is projector-admissible but is
    taxonomy-distinct from both family false-rival rows and the stronger
    `contrastive:redeployment-path` rows
- `P6-A02b-shallow-comparative-trace`
  - passed
  - trace/tail-risk now carry explicit planner and localization expectations
    for shallow-comparative rows, so upstream-present shallow contrast can be
    localized as certification/planner behavior and packet-owned
    `planner_negative` leaks no longer need to fake generic absence
- `P6-A02c-comparative-certification-outcomes`
  - passed
  - canonical exact-board shallow-comparative rows now have the closed
    certification/planner outcome matrix: strong exact contrast stays
    certified, shallow exact contrast stays support-only or deferred, and
    shallow-only planner ownership stays closed
- certified planner spine through `P7-E02`
  - landed
  - object delta, delta-aware certification, question admission, `WhyNow`,
    explanation trace, and tail-risk gate now form one bounded runtime/test
    spine

## Active Queue

| Packet | Status | Priority | Basis / Surface | Notes |
|---|---|---:|---|---|
| `P5-C01-comparative-counterpart-contract` | `passed` | 1 | counterpart admissibility | landed on the current packet lane |
| `P5-C02-contrast-separability` | `blocked` | 2 | comparative quality | retry failed to prove a distinct exact-board shallow-contrast row; current burden appears to belong in certification rather than projector |
| `P6-A01-delta-aware-certification` | `passed` | 3 | certification burden | certification now reads typed delta burden; stable weak deltas downgrade to support-only or deferred |
| `P7-Q01-question-admission-matrix` | `passed` | 4 | planner semantics | centralized typed-delta question matrix landed; `WhyNow` remained deferred until `P7-Q02` |
| `P7-Q02-whynow-admission` | `passed` | 5 | planner semantics | timing-sensitive WhyNow lane landed from certified move-local witness |
| `P7-E01-explanation-trace` | `passed` | 6 | eval trace | canonical pre-sanitization explanation trace now exports row-local projector / certification / planner state at the test boundary |
| `P7-E02-tail-risk-eval` | `passed` | 7 | eval gate | tail-risk evaluation now enforces hardest-slice planner-leak rejection plus packet-owned `planner_negative` coverage |
| `P6-A02-comparative-quality-certification` | `passed` | 8 | comparative quality certification | umbrella packet resolved; `P6-A02a`, `P6-A02b`, and `P6-A02c` are all passed |
| `P6-A02a-shallow-comparative-corpus` | `passed` | 9 | comparative quality corpus | canonical same-owner `DevelopmentCoordinationState` / `RedeploymentRoute` shallow-comparative rows now exist on `exact:redeployment-path` without collapsing back into `comparative_false_rival` or reusing the strong contrast board |
| `P6-A02b-shallow-comparative-trace` | `passed` | 10 | comparative quality trace | trace/tail-risk now preserve explicit planner/localization expectations for shallow-comparative rows, and `planner_negative` hard-fail checks can target upstream-present rows without collapsing them into generic absence |
| `P6-A02c-comparative-certification-outcomes` | `passed` | 11 | comparative quality certification | canonical `Certified` / `SupportOnly` / `Deferred` / `planner_none` outcomes are now locked for admissible-but-shallow comparative rows |
| `P9-A01-exact-comparative-support` | `ready` | 12 | narrow-slice reconstitution | comparative-quality certification is closed; re-earn one exact comparative-support slice on the new spine before broader slice parity claims |
| `P9-A02-exact-target-fixation-reconstitution` | `ready` | 13 | narrow-slice reconstitution | re-earn one exact target-fixation slice on the new architecture without legacy adapters |
| `P9-A03-bounded-favorable-simplification` | `ready` | 14 | narrow-slice reconstitution | re-earn one bounded favorable-simplification slice on the new spine only |
| `P9-A04-current-position-fixed-target-probe` | `ready` | 15 | narrow-slice reconstitution | reopen one current-position fixed-target probe after exact comparative support is stable |
| `P9-A05-current-position-coordination-probe` | `ready` | 16 | narrow-slice reconstitution | reopen one current-position coordination probe after earlier Phase 9 slices hold |
| `P5-T01-tier1-provisional-comparative-reaudit` | `ready` | 17 | readiness / promotion | re-audit provisional Tier 1 comparative lanes after comparative-quality closure and first slice parity |
| `P5-T02-tier1-provisional-move-local-reopen-audit` | `ready` | 18 | readiness / promotion | reopen provisional move-local only where exact-board positives and nasty negatives justify it |

## Packet Selection Rule

Always pick the highest-priority packet that is:

- `ready`, and
- not blocked by an unfinished earlier packet whose burden has not been
  explicitly transferred, deferred, or contained by queue notes and the
  blocked packet's status notes.

Do not combine packets unless the queue is explicitly edited to do so.

## Status Vocabulary

- `ready`
- `in_progress`
- `blocked`
- `passed`
- `passed_with_defer`

Use `passed_with_defer` when the packet succeeds but a family or lane remains
intentionally conservative.
