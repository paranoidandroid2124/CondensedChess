# Commentary Program Map

> Legacy migration reference only.
>
> This document describes the pre-demolition commentary-analysis program state.
> It is not rewrite design authority. Use `StrategicObjectModel.md` as the
> north-star and `StrategicObjectRoadmap.md` as the execution roadmap.

Legacy migration reference only. This document is not rewrite design
authority. For commentary-analysis rewrite direction, target architecture, and
demolition boundary decisions, use `StrategicObjectModel.md` first and
`StrategicObjectRoadmap.md` second. This file remains useful only for legacy
program status, CQF history, and migration-risk context.

This document is the single onboarding and navigation entry point for the
Chesstory commentary-analysis program.

Use it when a new session needs to understand:

- what Step 1-7 accomplished
- what is already closed and must not be reopened casually
- what CQF means
- which document is canonical for runtime audit and truth signoff
- what the current operating state is

This file is an index and status map. It is not the canonical runtime audit.

## Canonical Document Map

### 1. Program map

- This file:
  [CommentaryProgramMap.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md)
- Purpose:
  onboarding, roadmap status, current active work, document navigation

### 2. Runtime audit

- Canonical runtime audit:
  [CommentaryPipelineSSOT.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryPipelineSSOT.md)
- Purpose:
  latest live commentary-analysis pipeline, runtime contracts, planner/build/
  replay behavior, stable architecture

### 3. Truth signoff gate

- Canonical truth/signoff gate:
  [CommentaryTruthGate.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTruthGate.md)
- Purpose:
  decisive-truth release criteria, zero-tolerance truth failures, signoff
  interpretation

### 4. Trust hardening map

- Canonical trust-hardening map:
  [CommentaryTrustHardening.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)
- Purpose:
  trust-risk inventory, CTH audit baseline, hardening priorities, and Track 5
  defer rationale

## Current Canonical State

- Step 1-7:
  closed and signoff-ready
- CQF Track 0-4:
  closed or maintenance-only
- CQF Track 5:
  deferred
- CTH-A:
  core complete, maintenance-only
- CTH-B:
  bounded B1 / B2 / B3 are implemented and adversarial-review green; B4 is
  now `current bounded scope complete` only as one narrow local file-entry
  bind slice
- CTH-B5a:
  design/recon/charter baseline ready only:
  no new runtime slice is open, and the selected next recon family is
  heavy-piece local bind on a negative-first criticism lane rather than an
  approved positive rollout
- CTH-B5b:
  bounded queen-on heavy-piece local bind containment slice is now
  `current bounded scope complete` only inside that narrow negative-first
  charter; heavy-piece positive wording, owner lanes, and public-schema
  expansion remain closed
- CTH-B6:
  `B6b` narrow positive slice is now implemented on the existing
  `RestrictionProphylaxis -> file-entry reuse` lane, but only for one bounded
  `file-entry pair + one non-redundant reroute denial` on the same defended
  branch, only in clearly-better late middlegames, only for planner-owned
  `WhyThis`; Chronicle / Bookmaker / Active / whole-game replay remain
  fail-closed, broader route-chain / sector-network / color-complex shells
  remain deferred or unsafe, and B6 family closeout is now explicitly
  plateaued:
  the 2026-04-03 widened exact-FEN matrix reran `25` rows on the same helper
  lane, including the current control, the `K09*` / `B21A` pool, local B4/B5/B7
  blockers, posture blockers, and external historical leads. It still
  reproduced the after-trigger defended branch
  `a7a5 b4a5 c6a5 f3e5 ... a5c4`, but root best on the control FEN stayed
  `c3b4`, not `a3b4`, and the matrix produced `0` independent broader B6
  survivors. The practical result is narrower than the earlier close-ready
  reading:
  current branch keeps the exact intermediate route-chain visible only on the
  backend helper lane, not as planner-owned live truth, so A-track closeout
  does not advance from it. The current control now reads as an
  `after-trigger intermediate detour only` near miss with downstream reroute
  still visible and B5-style release leakage still live, and
  broader posture/phase expansion plus replay / whole-game positive reuse stay
  deferred behind the exact-board burden
- CTH-B7:
  design/recon/charter baseline ready only:
  no B7 runtime slice is open. The 2026-04-04 owner-admission pass resolved
  the old `K09B` / `K09F` trade-down near miss onto Cluster C instead:
  on the exact defended branch `d4e6|f7e6`, the current runtime now
  materializes planner-owned same-task simplification on the existing
  `MoveDelta -> WhyThis` lane (`This trade keeps the same local edge on e6.`),
  not a task shift. Broad `good endgame` / favorable-transition rhetoric
  remains closed; the backend-only `currentTask -> shiftedTask` contract freeze
  is still the canonical B7 baseline, but it has no certified exact-FEN
  positive control on the current corpus. Because `K09B` / `K09F` now belong to
  promoted bounded `favorable_simplification` and follow-up hunts still found
  no independent survivor, B7 remains explicitly deferred as
  `still overfit / more corpus work needed`
- CTH-B8:
  B8 has been reset to a clean design-only placeholder.
  Prior B8a corpus/acquisition artifacts were intentionally cleared after
  failing to produce either a stable taxonomy or any exact-board survivor worth
  carrying forward as an active queue.
  No runtime slice, owner lane, schema, or prose family is open, and any
  future B8 work must restart from fresh ontology/corpus rather than reuse the
  cleared artifacts.
- Current operating mode:
  maintain the live B1 / B2 / B3 / B4 runtime, the maintenance-only B5b
  containment slice, and the current B6 route-network slice; B7 is
  baseline-ready only and opens no runtime semantics yet. B6 optional expansion
  remains a separate queue, and any future B7b work must stay inside the frozen
  owner-path / task-vocabulary baseline plus the same-branch nasty-case
  charter. Sector-network and color-complex route expansion remain later,
  riskier B6 cells. Treat the later frontier map as:
  `B6 = named route-network bind`,
  `B7 = transition-adjacent strategic route`,
  `B8 = slight-edge local squeeze` only after fortress/progress separation is
  closed, and
  `B9 = bounded color-complex / mobility-cage` as the last and most
  risk-sensitive family

## Mid-Layer Cluster Map

Use
[`strategic-promotion-record.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/tmp/strategic-promotion-record.md)
as the local result-only ledger for cluster / family / subfamily cell status.
Keep campaign logs, proving notes, and temporary triage history out of the
canonical docs.

| Cluster | Family bucket | Included families / subfamilies | Current result | Next action |
| --- | --- | --- | --- | --- |
| A | Restriction / Counterplay Suppression | `15.neutralize_key_break`, `15.prophylactic_move`, `15.defensive_regrouping`, `8.half_open_file_pressure`, `8.open_file_control` | `neutralize_key_break`, `prophylactic_move`, and `half_open_file_pressure` promoted; `defensive_regrouping` absorbed into `prophylactic_move`; `open_file_control` absorbed into `half_open_file_pressure` | closed unless new exact-board corpus changes one of the absorbed cells |
| B | Target Fixation / Weakness Pressure | reviewed `static_weakness_fixation`, `backward_pawn_targeting`, `target_focused_coordination`, `minority_attack_fixation`, `iqp_inducement` | cluster B now has three live exact slices on the canonical path: `B21` / `B21A` keep the existing move-local `exact_target_fixation` lane (`bestDefenseBranchKey=f3d2|b8a6`, `sameBranchState=Proven`, `persistence=Stable`, `main_bundle=This keeps the pressure fixed on d6.`, planner-primary `WhatChanged=This changes the position by fixing d6 as the target.` plus same-branch contrast/consequence), `B15A` / `B16B` keep the Carlsbad fixed-target current-position probe (`ownerSource=carlsbad_fixed_target_probe`, `ownerFamily=backward_pawn_targeting`, `scope=PositionLocal`, `main_bundle=The key strategic fact here is that c6 is the fixed target.`, planner-primary `WhatMattersHere`, deterministic Bookmaker / Chronicle primary, and coda `So the task is to keep the queenside pressure trained on c6 instead of rushing a conversion.`), and `K09A` / `K09D` now open the second planner-owned current-position probe lane on exact coordination (`ownerSource=target_focused_coordination_probe`, `ownerFamily=target_focused_coordination`, `scope=PositionLocal`, `bestDefenseBranchKey=d1b3|d8d7` on `K09A` / `h2h3|g4f3` on `K09D`, `sameBranchState=Proven`, `persistence=Stable`, `main_bundle=The key strategic fact here is that the pressure is coordinated on c6.`, planner-primary `WhatMattersHere`, and deterministic Bookmaker / Chronicle coda `So the task is to keep the pressure coordinated on c6 until the target has to give way.`); the same exact `B21A` packet still backs the only support-only `decisionComparison` comparative digest (`Nd2` versus `Qc2`: `Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch.`); `minority_attack_fixation` stays absorbed support-only; `iqp_inducement` stays structure/conversion support-only; `K03A` remains fail-closed and `K09E` remains file-pressure / release-rival blocked | keep all three exact lanes narrow: `B21` / `B21A` stay move-owned delta only, `B15A` / `B16B` stay Carlsbad current-position probe only, `K09A` / `K09D` stay exact coordination current-position probe only, and fail-close siblings unless a new reviewed row survives exact board + same-branch proof without relabel drift |
| C | Exchange / Removal Without Task-Shift | `17.trade_key_defender`, bounded `favorable_simplification`, bounded `trade_attacking_piece`, bounded `remove_key_defender` trigger-only form | bounded `favorable_simplification` is now promoted on one exact same-task simplification slice; `trade_key_defender` stays blocked; `trade_attacking_piece` is now reviewed fail-closed because `K08A` is not a real live-attacker removal row, `K08D` is not root-best, and exploratory `MI5` collapses into tactic-first `Qxd6` relief; `remove_key_defender` is now reviewed fail-closed because `K09B`/`K09F` only survive as same-task simplification on `d4e6|f7e6`, `K09A` / `K09D` now belong to the separate `target_focused_coordination` current-position probe lane, `K09E` stays file-pressure / release-rival dominated, and `MI5` is still tactic-first relief rather than a one-defender local trigger | keep the promoted simplification slice narrow and keep `trade_key_defender`, `trade_attacking_piece`, and `remove_key_defender` blocked; reopen `trade_attacking_piece` or `remove_key_defender` only with a new exact row whose planner-owned move-local owner survives without relabeling into simplification, blocked defender-trade ownership, attack-piece removal, tactical cleanup, or the exact coordination probe lane |
| D | Coordination / Access Conversion | bounded `support_file_occupation`, bounded `entry_square_enable_deny`, bounded `target_focused_piece_coordination` | `entry_square_enable_deny` reviewed and blocked; `support_file_occupation` and `target_focused_piece_coordination` remain queued | keep `entry_square_enable_deny` blocked unless a new exact root-best row materializes a one-square move-local owner without relabeling into prophylaxis or file-entry reuse; keep the other two cells queue-only |

Current mid-layer priority remains machinery-first for the remaining unopened
review cells:
deepen owner seed, same-branch continuation, rival-story arbitration, and
structure-transition witness inside the existing canonical owner-path modules
while keeping the newly opened Cluster B slice narrow and the remaining C / D
reviews fail-closed until they survive the same proof burden.

## What Step 1-7 Closed

Step 1-7 was the architecture stabilization program.

Its purpose was not “make commentary rich”, but:

- close owner legality
- stop unsafe owner revival
- make planner/build/replay consistent
- keep surface parity
- finish collision-corpus signoff

### Step Summary

1. `Trace / diagnostics`
   - planner trace and audit carriers were made explicit
2. `SceneType classification`
   - scene-first classification was introduced and stabilized
3. `OwnerCandidate normalization`
   - owner/support/timing/domain trace contract was normalized
4. `AdmissionMatrix`
   - conservative legality baseline was applied
5. `Planner integration`
   - admitted-but-unbuildable and demote dead-end paths were closed
6. `Surface replay`
   - Chronicle / Bookmaker / Active were verified not to rewrite planner
     legality
7. `Collision corpus / signoff`
   - final verdict became `signoff-ready`

### Meaning Of Step 1-7 Completion

Step 1-7 completion means:

- `scene-first admission` is closed
- `owner legality` is closed
- `planner/build/replay consistency` is closed
- `surface parity` is closed
- `collision signoff` is closed

It does **not** mean the commentary system is final or fully rich.

It means the system now has a stable answer to:

`who is allowed to own the explanation`

## What Must Stay Closed

Unless a task explicitly reopens core architecture, do not casually reopen:

- owner legality
- ranking/owner-family redesign
- raw opening precedent as direct owner
- raw endgame hint as direct owner
- raw close alternative as direct owner
- latent / pv-coupled / deferred revival
- surface-local owner minting outside the shared planner

## CQF: Commentary Quality Frontier

After Step 1-7 reached `signoff-ready`, the program moved into CQF.

CQF is not another legality pass.

Its job is:

`take already-legal truth and explain it more usefully without breaking truth ownership`

## CTH: Commentary Trust Hardening

CTH is the trust-first workstream that sits in front of Track 5.

Its job is:

`reduce false positive, overclaim, overgeneralization, out-of-scene generalization failure, and cross-surface inconsistency before lesson extraction`

Use [CommentaryTrustHardening.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)
as the canonical trust-risk map.

Current status:

- `CTH-A`:
  core complete, maintenance-only
- `CTH-B0`:
  recon baseline complete
- `CTH-B1`:
  bounded restricted-defense conversion slice implemented, broad validation and
  reverse adversarial review green
- `CTH-B2`:
  bounded named-break / entry-axis suppression slice implemented and
  `close-ready` inside that narrow charter
- `CTH-B3`:
  bounded dual-axis clamp / bind slice implemented and adversarial-review green
  inside that narrow charter
- `CTH-B4`:
  design baseline consumed by one narrow runtime slice; hardening fix-up is
  closed and the slice is now `current bounded scope complete` inside the
  narrow local charter:
  clearly-better late-middlegame local file-entry bind certification with one
  single corroborating entry-square axis; replayable two-ply same-branch
  identity and cross-surface non-reinflation are now green on that bounded
  slice, broader B4 expansion remains closed, and the next move is
  `B4 maintenance-only watch`
- `CTH-B5a`:
  next-local-frontier design baseline ready only:
  no B5 runtime slice is open; the most reusable deferred family is
  heavy-piece local bind, but it stays deferred after self-critique and is
  approved only as a negative-first criticism/validation charter for any B5b
  work
- `CTH-B5b`:
  bounded queen-on heavy-piece local bind containment slice implemented and
  `current bounded scope complete` inside that narrow negative-first charter
  only; exact-FEN nasty cases, exact-branch replay, fixed-depth PV1
  reproduction, replayable two-ply same-branch identity, and cross-surface
  non-reinflation are green, and heavy-piece positive wording remains deferred
- `CTH-B6`:
  `B6b` narrow positive slice:
  backend-only certification plus planner-owned `WhyThis` wording is now live
  for one bounded `file-entry pair + one non-redundant reroute denial` on the
  same defended branch; replay / whole-game surfaces remain fail-closed, and
  the branch-identity / planner-surface fix-up kept that bounded slice alive,
  but the 2026-04-03 broadened closeout matrix still did not find the required
  second, third, or any independent survivor for B6 family completion. The
  helper-backed rerun kept the after-trigger `a5` detour branch but returned
  `c3b4`, not `a3b4`, as root best on the control FEN, and the full `25`-row
  exact-FEN matrix yielded `0` broader B6 survivors while strengthening the
  blocker pack across branch identity, route mirage, B4 relabel, B5 leakage,
  B7 drift, posture inflation, engine PV paraphrase, and untouched-sector
  escape. B6 family expansion therefore remains one-example heavy / plateaued
  at A-track closeout, and posture/phase plus replay reuse remain deferred
  behind the same exact-board burden
- `CTH-B7`:
  design/recon/charter baseline ready only:
  no runtime slice is open. The old `K09B` review control is no longer a B7
  near miss: `K09B` / `K09F` now materialize only as promoted bounded
  `favorable_simplification` on the exact `d4e6|f7e6` branch, so B7 still
  lacks a certified exact-FEN positive control. The backend-only
  `currentTask -> shiftedTask` contract freeze remains canonical, and B7
  runtime stays deferred on that baseline until some new exact-board survivor
  proves a distinct task handoff rather than same-task simplification
- `CTH-B8`:
  confirmed later frontier:
  slight-edge local squeeze; deferred until fortress/progress discrimination is
  materially stronger
- `CTH-B9`:
  confirmed last frontier:
  bounded color-complex / mobility-cage; optional and still the highest-risk
  abstract family
- broader `CTH-B frontier`:
  still not opened for new positive semantics; do not treat B4 or B5a as
  approval to widen runtime semantics

### CQF Tracks

#### Track 0 — Upstream Input Quality

- Goal:
  same-ply input bundle parity, digest/hash visibility, upstream-vs-replay
  mismatch separation
- Status:
  scaffold established

#### Track 1 — Human-Facing Metrics Scaffold

- Goal:
  internal rubric and selector threshold for quality experiments
- Status:
  `signoff-ready for Track 2 entry`
- Important note:
  maintenance mode, not retired

#### Track 2 — Contrastive Explanation

- Goal:
  narrow contrast support on already-legal owners
- Current surface status:
  - Bookmaker: `signoff-ready`
  - Chronicle: `signoff-ready`
  - Active: `scope-cut (diagnostic-only)`

#### Track 3 — Quiet / Strategic Richness

- Goal:
  reduce dry exact-factual one-liners in quiet/strategic rows without
  overclaim
- Current status:
  the quiet-support acceptance subset remains passing on real16:
  the selected quiet-support subset stays intact and the representative
  blocked forcing rows no longer fall to quiet fallback
- Current active work:
  monitor the residual style-choice `pv_delta` ingress gap outside the
  quiet-support owner-preservation gate; do not reopen composer wording,
  legality, ranking, or owner-family design

#### Track 4 — Scene Coverage Lane

- Goal:
  corpus maintenance lane for new scene buckets and failure modes
- Status:
  `close-candidate` on the report-only corpus-maintenance lane:
  the important bucket set is materialized in artifact form with target
  candidate / curated / stable-fixture coverage, and the final
  `complex_sacrifice` widening probe added no new failure class

#### Track 5 — Pedagogical Lesson Extraction

- Goal:
  derive lesson-level explanation only after contrast/quiet quality is mature
  and trust-critical risks are further closed first
- Status:
  explicitly deferred behind trust hardening; do not treat as the next
  implementation priority while false positive / overclaim /
  overgeneralization / out-of-scene generalization failure /
  cross-surface inconsistency remain active concerns

### Where CQF details now live

There is no separate CQF appendix anymore.

Use:

- [CommentaryProgramMap.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md)
  for track map and current status
- [CommentaryTruthGate.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTruthGate.md)
  for signoff interpretation and closed quality-lane boundaries that affect
  release decisions
- [CommentaryTrustHardening.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)
  for trust-risk inventory, CTH priorities, and Track 5 defer rationale
- local generated artifacts under the quality-audit report root for concrete
  row-level evidence and rerun summaries

## Current Active Work

The current active posture is trust-maintenance on the live B-frontier:
keep the shipped B1 / B2 / B3 / B4 runtime stable, keep B5b as a closed bounded
containment slice rather than a permission slip for broader B5 work, and treat
B6b as a narrowly live planner-only slice rather than a permission slip for a
broad route-network rollout.
The current design/recon frontier inside B6 is now narrower than that:
stay on the same clearly-better late-middlegame / planner-owned `WhyThis` cell
and widen texture first to one bounded route chain before any posture/phase or
replay widening.
CQF Track 3 and Track 4 remain
maintenance/report lanes, not the active implementation frontier.

### Current status

Current frontier status:

- the live trust-owning runtime is still:
  bounded B1 / B2 / B3 / B4 only
- the only live B4 positive scope remains:
  clearly-better late-middlegame local file-entry bind certification with one
  single corroborating entry-square axis on the existing
  `RestrictionProphylaxis` lane
- B5a is design-only:
  no new positive slice is open, and the selected next recon family is
  heavy-piece local bind on a negative-first criticism/validation lane
- B5b is implemented only as a negative-first containment slice and is now
  `close-ready` inside that bounded charter:
  queen-on heavy-piece local bind shells now downgrade through the existing
  `RestrictionProphylaxis` file-entry reuse path, with exact-FEN nasty cases,
  exact-branch replay release proof, fixed-depth engine-path reproduction, and
  cross-surface non-reinflation checks; heavy-piece positive wording remains
  closed
- latest B5b residual watch result:
  slice-level stable after tightening exact-branch release proof so only
  complete proof-eligible lines count and after making the engine verifier
  prefer an explicit configured binary over the local fallback
- B6b is narrowly live:
  named route-network bind now opens only for one bounded `file-entry pair +
  one non-redundant reroute denial` on the same defended branch, still inside
  the existing `RestrictionProphylaxis -> file-entry reuse` lane, with planner-
  owned `WhyThis` as the only positive owner scope; replay / whole-game reuse,
  route-chain / sector-network / color-complex expansion, and heavy-piece
  positives remain closed
- B6 family expansion baseline is now design-ready:
  the next bounded candidate is broader route-chain rollout, but only as one
  exact same-branch intermediate-node chain above the current B6 triplet;
  posture/phase expansion stays deferred until that chain survives exact-board
  criticism, replay / whole-game positive reuse stays deferred after that, and
  sector-network / color-complex remain unsafe
- confirmed post-B5 frontier map:
  `B6 = named route-network bind`
- then:
  `B7 = transition-adjacent strategic route`
- later only after stronger fortress/progress separation:
  `B8 = slight-edge local squeeze`
- last and still most abstract / risk-sensitive:
  `B9 = bounded color-complex / mobility-cage`

CQF maintenance status:

- Track 3 is in maintenance monitoring, not an active blocker
- quiet-support wording itself is no longer the main blocker, and the
  planner-owned blocked-lane forcing-row preservation blocker remains closed
- Track 4 no longer has a collection blocker and remains a close-candidate on
  the corpus-maintenance lane

Representative preserved rows include:

- `Ke7`
- `Nxe3`
- `Rxc4`

The selected quiet-support subset also remains intact on the representative
`34. a5` row (`runtimeGatePassCount=4`, `quietSupportLiftAppliedCount=4`,
baseline selected owner/question divergence `=0`, stronger-verb leakage `=0`).

The remaining residual review row is `Ng4`, a style-choice /
`pv_delta_missing` upstream lane outside the Track 3 quiet-support planner fix.

Track 4 no longer has a collection blocker. The important bucket set is now
materialized in artifact form:

- `prophylaxis_restraint`
- `opening_deviation_after_middlegame_plan_clash`
- `transition_heavy_endgames`
- `slow_route_improvement`
- `complex_sacrifice`
- `pressure_maintenance_without_immediate_tactic`
- `long_structural_squeeze`
- `opening_deviation_after_stable_development`

Each of those buckets now sits inside the target candidate / curated /
stable-fixture band, and the final `complex_sacrifice` widening probe added no
new failure class.

Track 5 remains deferred.

The current trust posture is bounded live runtime plus closed broader frontier:

- CTH-A is no longer an active implementation frontier
- bounded CTH-B1 / B2 / B3 slices are implemented and adversarial-review green
- bounded CTH-B4 is now implemented too:
  the live positive scope is only a bounded file-bind plus one single
  corroborating entry-square local certificate inside the existing owner path,
  and the hardening fix-up has closed its known entry-persistence,
  affirmative-matcher, and same-first-move defended-branch seams; that narrow
  slice is now `current bounded scope complete` only and broader B4 family
  expansion remains closed
- bounded CTH-B5a is now documented too:
  no new positive scope opens; the next recon family is heavy-piece local
  bind, but only on a negative-first criticism/validation lane until it
  survives the B5 nasty-case burden
- heavy-piece middlegame positives and slightly-better positives remain
  deferred
- standalone square-network positives and transition-adjacent positive route
  claims remain deferred
- bounded color-complex positives remain unsafe
- if any post-B4 runtime change reopens an
  audited seam, update
  [CommentaryTrustHardening.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)
  and extend the relevant regression packs before reopening the frontier; if a
  future session wants B5b work, start from the B5a criticism matrix and
  broad-validation charter there rather than widening the live slice ad hoc

## How To Read The Docs In A New Session

If a user says:

- `Step 1-7 is done`
- `CQF is ongoing`
- `trust-maintenance is the active posture`

read documents in this order:

1. [CommentaryProgramMap.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md)
2. [CommentaryPipelineSSOT.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryPipelineSSOT.md)
3. [CommentaryTruthGate.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTruthGate.md)
4. [CommentaryTrustHardening.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)

Do not start by re-auditing the whole pipeline unless the code path changed or
the user explicitly asks for a fresh audit.

## Short Handoff Vocabulary

Use this shorthand consistently:

- `Step 1-7`
  - architecture stabilization and signoff
- `CQF`
  - post-signoff commentary quality frontier
- `runtime audit`
  - SSoT
- `truth gate`
  - signoff criteria
- `quality artifacts`
  - local rerun/eval outputs, not canonical docs

## One-Paragraph Handoff

Use this wording when you need a concise new-session handoff:

`Step 1-7 is complete and signoff-ready. That closed scene-first admission, owner legality, planner/build/replay consistency, surface parity, and collision signoff. We are now in CQF, but the active trust-owning rollout remains narrow: the live runtime is still bounded B1 / B2 / B3 / B4, B5b is current-bounded-scope complete as a negative-first containment slice only, and B6b remains the only route-network positive attempt on the same restriction-prophylaxis lane: clearly-better late-middlegame named route-network bind still requires a certified B4 file-entry pair plus one non-redundant reroute denial on the same defended branch, opens only planner-owned WhyThis wording, and stays fail-closed on Chronicle / Bookmaker / Active / whole-game replay. The 2026-04-03 broadened exact-FEN matrix reran 25 rows, found 0 independent broader B6 survivors, and again failed to reproduce the earlier `a3b4` root-best reading under the helper-backed rerun, so A-track closeout is plateaued rather than advanced. The broader route-chain intermediate remains backend-visible only on the current branch; planner-owned live wording for that wider slice is closed again until a fresh exact-FEN root-best control exists. Broad route-chain, sector-network, heavy-piece positive, slight-edge squeeze, and color-complex expansion stay closed. Use CommentaryProgramMap first, then SSoT and TruthGate; detailed rerun evidence lives in local quality artifacts rather than a separate appendix document.`
