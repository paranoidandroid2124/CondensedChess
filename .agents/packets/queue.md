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
- `P9-A02-exact-target-fixation-reconstitution`
  - passed
  - one exact `FixedTargetComplex` move-local `WhyThis` slice is re-earned on
    a centralized fixation-square witness, while pressure-only and near-miss
    target pictures remain planner `none`
- `P9-A03-bounded-favorable-simplification`
  - passed
  - one exact same-task `TradeInvariant` move-local `TradePreserved`
    `WhyThis` slice is re-earned on the new spine, while target-led,
    contrastive, and heavy-piece-release lookalikes remain fail-closed
- `P9-A04-current-position-fixed-target-probe`
  - passed
  - reopened one bounded current-position fixed-target probe; B15A is primary while K03A and K09E stay closed
- `P9-A05-current-position-coordination-probe`
  - passed_with_defer
  - reopened one bounded current-position coordination probe on K09A; K09D, K09E, and the single-active-piece mirage stay closed
- narrow-slice reconstitution baseline through `P9-A05`
  - landed
  - exact comparative support, target fixation, bounded favorable
    simplification, current-position fixed-target probe, and current-position
    coordination probe have each been re-earned at least once on the new spine
- Tier 1 provisional readiness re-audit through `P5-T02`
  - landed
  - comparative re-audit is `passed_with_defer`, and move-local reopen remains
    `blocked` on missing exact-positive plus nasty-negative evidence
- certified planner spine through `P7-E02`
  - landed
  - object delta, delta-aware certification, question admission, `WhyNow`,
    explanation trace, and tail-risk gate now form one bounded runtime/test
    spine
- `P8-R01-thin-shell-certified-renderer`
  - passed
  - Bookmaker renderer/API/frontend shell now stays thin on certified planner
    ownership; this path no longer exports/decodes/reconstructs
    `strategyPack` / `signalDigest`, thin-shell rendering mirrors
    `claimIds/supportClaimIds`, and packet-owned exact-row shell tests cover
    the re-earned stable slices plus `K03A`, `K09E`, and single-active-piece
    mirage closure
- `P8-R02-exact-target-campaign-e2e`
  - blocked
  - the current exact fixed-target tranche does not yet contain one real
    shared-target campaign across all three axes: `P9-A04` is anchored on
    `c6`, while `P9-A02` and `P9-A01` are anchored on `d6`, so the runtime
    currently proves three independent exact lanes rather than one exact
    shared-target campaign
- `P8-R04-current-position-coordination-e2e`
  - passed_with_defer
  - one bounded `K09A` current-position coordination probe now survives thin-
    shell delivery end-to-end, while `K09D`, `K09E`, and the single-active-
    piece mirage remain planner/shell closed

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
| `P9-A01-exact-comparative-support` | `passed` | 12 | narrow-slice reconstitution | exact same-owner shared-target comparative support is re-earned on the new spine; shallow comparative stays fail-closed at planner `none` / localization `certification` |
| `P9-A02-exact-target-fixation-reconstitution` | `passed` | 13 | narrow-slice reconstitution | one exact target-fixation slice is re-earned from a fixation-square move-local witness; pressure-only and near-miss target rows remain fail-closed |
| `P9-A03-bounded-favorable-simplification` | `passed` | 14 | narrow-slice reconstitution | exact same-task `TradeInvariant -> TradePreserved -> WhyThis` slice re-earned; negative/contrastive/near-miss closure held |
| `P9-A04-current-position-fixed-target-probe` | `passed` | 15 | narrow-slice reconstitution | reopened one bounded current-position fixed-target probe; B15A primary, K03A/K09E closed |
| `P9-A05-current-position-coordination-probe` | `passed_with_defer` | 16 | narrow-slice reconstitution | reopened one exact current-position coordination probe on K09A only; K09D/K09E/single-active-piece mirage remain closed |
| `P5-T01-tier1-provisional-comparative-reaudit` | `passed_with_defer` | 17 | readiness / promotion | family-complete comparative shallow rows now exist for all eight provisional families; all eight stay `Provisional`, and shallow comparative remains `SupportOnly` / planner `none` |
| `P5-T02-tier1-provisional-move-local-reopen-audit` | `blocked` | 18 | readiness / promotion | all eight provisional families remain move-local closed; no family-complete move-local exact positive or move-local nasty-negative burden exists yet, so selective reopen is not certified; this evidence stop is orthogonal to the next vertical tranche on already re-earned stable slices |
| `P8-R01-thin-shell-certified-renderer` | `passed` | 19 | vertical slice / thin shell | landed Bookmaker thin-shell closure: controller/frontend path no longer exports/decodes/reconstructs `strategyPack` / `signalDigest`; renderer mirrors planner `claimIds/supportClaimIds`; exact-row shell closure held on `K03A` / `K09E` / single-active-piece mirage |
| `P8-R02-exact-target-campaign-e2e` | `blocked` | 20 | vertical slice / target campaign | exact missing boundary: no current packet-scoped shared-target continuity exists across all three axes because `P9-A04` is `c6`-anchored while `P9-A02` and `P9-A01` are `d6`-anchored; current runtime proves separate exact lanes, not one shared campaign |
| `P8-R03-bounded-favorable-simplification-e2e` | `blocked` | 21 | vertical slice / simplification | exact missing boundary: on `curated-exact:k09b`, `WhyThis` still emits mixed move-local `claimIds` (`AccessNetwork`, `FixedTargetComplex`, opponent `TradeInvariant`, plus the exact simplification claim), so the thin shell cannot isolate one bounded favorable-simplification explanation without a new primary-claim selection boundary |
| `P8-R04-current-position-coordination-e2e` | `passed_with_defer` | 22 | vertical slice / coordination | bounded `K09A` coordination probe survives end-to-end on `WhatMattersHere`; `K09D`, `K09E`, and single-active-piece mirage stay fail-closed |

## Current Vertical Tranche Rule

- `P8-R04` is now closed as `passed_with_defer`; `P8-R02` and `P8-R03` remain
  `blocked` on the exact missing boundaries recorded in their queue notes.
- `P5-T02` remains orthogonal to this tranche because these packets consume
  only already re-earned stable slices and do not depend on provisional
  move-local reopening.
- Any future horizontal gate, ownership, or certification packet must be
  derived from one of those exact blocked vertical-slice failures rather than
  infra widening.

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
