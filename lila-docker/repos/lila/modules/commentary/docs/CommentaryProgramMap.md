# Commentary Program Map

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
  [CommentaryProgramMap.md](modules/commentary/docs/CommentaryProgramMap.md)
- Purpose:
  onboarding, roadmap status, current active work, document navigation

### 2. Runtime audit

- Canonical runtime audit:
  [CommentaryPipelineSSOT.md](modules/commentary/docs/CommentaryPipelineSSOT.md)
- Purpose:
  latest live commentary-analysis pipeline, runtime contracts, planner/build/
  replay behavior, stable architecture

### 3. Truth signoff gate

- Canonical truth/signoff gate:
  [CommentaryTruthBoundary.md](modules/commentary/docs/CommentaryTruthBoundary.md)
- Purpose:
  decisive-truth release criteria, zero-tolerance truth failures, signoff
  interpretation

### 4. Trust boundary map

- Canonical trust-boundary map:
  [CommentaryTrustBoundary.md](modules/commentary/docs/CommentaryTrustBoundary.md)
- Purpose:
  trust-risk inventory, CTH audit baseline, boundary priorities, and deferred
  authority rationale

## Canonical Vocabulary

Use these layer names when discussing current commentary-analysis work:

| layer | canonical name | meaning |
| --- | --- | --- |
| strategic taxonomy namespace | `PlanTaxonomy` | the chess-plan vocabulary registry |
| top-level strategic topic | `PlanTheme` | broad strategy bucket such as restriction or exchange |
| concrete plan vocabulary item | `PlanKind` | one of the 35 tracked plan kinds |
| latent seed category | `SeedKind` | low-level seed type before it becomes a plan hypothesis |
| proof lane source | `proofSource` | the runtime/source-review lane that produced a claim packet |
| proof contract | `proofFamily` / `ProofContract` | the authority contract a packet must satisfy |
| planner owner lane | `PlannerOwnerKind` | the question-planner owner kind, separate from proof contracts |
| source-review bucket | `reviewGroup` | an intake group such as `A:break_prevention` |
| repeatable admission unit | `AdmissionUnitSpec` | one `PlanKind + proofSource + proofFamily` review unit |

Do not use `family` as a shorthand for all of these layers. Historical prose
may still say family when it is not naming a current pipeline layer.

## Current Canonical State

- 2026-05-20 MoveReview-only authority consolidation:
  user-facing commentary authority is now MoveReview-only. Chronicle/Game
  Chronicle, Guided Review, Defeat DNA, Active strategic-note generation, and
  full-game commentary API endpoints are removed product surfaces. Shared
  Game Arc / planner / proof helpers may remain only as internal diagnostics,
  historical fixtures, test/tooling support, or MoveReview-consumed
  infrastructure. Future `SupportedLocal` / `ScopedTakeaway` expansion is
  MoveReview-first and may not reopen Chronicle/Active authority paths.
- 2026-05-21 MoveReview evidence coverage follow-through:
  the test/tooling corpus path now reuses the same after-PV proof-variation
  carrier as runtime instead of a separate projection helper, and the
  MoveReview basic lane has one bounded `line_backed_local` descriptor for
  opening-phase / opening-labeled early-ply rows with validated coupled PV
  proof. The full 315-row audit (`move_review_coverage_full_v5`) records
  `planner=196`, `basic_move_explanation=119`, `exact_factual_fallback=0`,
  with no basic evidence-gap or `SupportedLocal` evidence-gap candidates.
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
  `WhyThis`; Chronicle / MoveReview / Active / whole-game replay remain
  fail-closed, broader route-chain / sector-network / color-complex shells
  remain deferred or unsafe, and B6 family closeout is now explicitly
  plateaued:
  the 2026-04-03 widened exact-FEN matrix reran `25` rows on the same helper
  lane, including the current control, the `K09*` / `B21A` pool, local B4/B5/B7
  blockers, posture blockers, and external historical leads. It still
  reproduced the after-trigger defended branch
  `a7a5 b4a5 c6a5 f3e5 ... a5c4`, but root best on the control FEN stayed
  `c3b4`, not `a3b4`, and the matrix produced `0` independent broader B6
  survivors. The practical result is narrower than the earlier completion-candidate
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
  No runtime slice, owner lane, schema, or surface phrase class is open, and any
  future B8 work must restart from fresh ontology/corpus rather than reuse the
  cleared artifacts.
- Current operating mode:
  maintain the live B1 / B2 / B3 / B4 runtime, the maintenance-only B5b
  containment slice, and the current B6 route-network slice; B7 is
  baseline-ready only and opens no runtime semantics yet. B6 optional expansion
  remains a separate queue, and any future B7b work must stay inside the frozen
  owner-path / task-vocabulary baseline plus the same-branch nasty-case
  charter. Sector-network and color-complex route expansion remain later,
  riskier B6 cells; exact color-complex pressure may be detected only as
  non-authority `semantic.evidence` / `ValidatedPressure`, not as a positive
  owner lane. Exact color-complex FEN readiness now has a dedicated private proof-readiness
  helper (`ColorComplexSqueezeProof`) plus commentaryTools corpus fixtures with
  explicit pass/fail reason codes. The helper separates weak-complex squeeze
  proof from route-denial file / entry / reroute proof, but it does not open
  `SupportedLocal`, `WhatMattersHere`, a packet path, or lesson-level
  explanation. Its readiness gate accepts a majority same-color weak-complex
  core with adjacent off-color support squares, functional defender paralysis
  beyond raw mobility delta, stable zone persistence wording, and
  escape-denial wording without treating those relaxations as authority. The
  scanner now keeps two-square color-complex cores in review scope instead of
  filtering them out before proof review. It still rejects stale proof probes
  whose FEN key does not match the evaluated current position, so Karpov-style
  source identity is a regression fixture rather than a whitelist predicate.
  The scanner separates `itemDetected`, `squeezeReadiness`, and
  `authorityDecision`, and `authorityDecision` remains `authority_closed` for
  every row. Engine review also separates usable `escape=` squares from
  `artifactEscape=` geometry that does not release a constrained defender
  piece. The 2026-05-20 full-game `SourceWitnessCatalog` scanner found 16
  `readiness_pass` rows across 9 distinct FENs and 5784 `readiness_fail` rows;
  the candidate review grouped them into 4 reviewable strategy events and 2
  artifact events. Karpov-Unzicker 1974 and Alekhine-Bogoljubow 1936 are
  `engine_persistent_readiness_candidate` rows at
  `color_complex_squeeze_readiness` scope; Lokvenc-Czerniak 1952 and
  Maderna-Palermo 1955 remain `engine_best_defense_review_required` because
  engine MultiPV lines expose usable escape or persistence blockers on the
  same proof shape. Treat those rows as source intake, not authority.
  Treat the later frontier map as:
  `B6 = named route-network bind`,
  `B7 = transition-adjacent strategic route`,
  `B8 = slight-edge local squeeze` only after fortress/progress separation is
  closed, and
  `B9 = bounded color-complex / mobility-cage` as the last and most
  risk-sensitive family
- Break-prevention source-intake tooling now uses the same resource/release
  separation: `BreakClampCandidateScanner` reports `itemDetected`,
  `releaseReadiness`, `releaseDecision=authority_closed`, and raw / usable /
  artifact release routes, while `BreakClampSourceTriage` ranks event-level
  `release_pass` survivors and harmless-transform artifacts instead of raw
  clean-row count. The 2026-05-20 ModernBenoni6e4 broad slice found 15
  release-pass artifact events. Route-token alignment now rejects transient
  source rows when the scanner route and materialized SourceReview surface
  drift apart; the guarded triage has 9 admitted rows, 6 review-required rows,
  and 4 clean black-midgame admitted events. Pomar-Toran 1969 ply 27 is now the
  fixed real-game harmless-transform source-intake row, with `14.b4` denying
  `...b5-b4` while the raw `...c5xb4` transform is branch-proven harmless and
  selected `SourceWindowReview` admits through the existing
  `neutralize_key_break` packet. This improves source review triage only;
  existing `neutralize_key_break` authority still requires the fixed exact packet,
  proof contract, tactical veto, planner admission, and claim-only surface.
- Separate from the B-route map, `central_break_timing` now has a bounded
  `SupportedLocal` release boundary on the existing move-delta path:
  board-backed break support may release only through the certified
  `central_break_timing` packet after same-branch proof, stable persistence,
  tactical-first veto, and rival-family veto; plan-only central-break rows are
  review / diagnostic material only, exact rows are regression seeds, and broad
  generic `plan_advance` remains owner-closed.

## Mid-Layer Cluster Map

Use
[`strategic-promotion-record.md`](modules/commentary/tmp/strategic-promotion-record.md)
as the local result-only ledger for cluster / family / subfamily cell status.
Keep campaign logs, proving notes, and temporary triage history out of the
canonical docs.

| Cluster | Family bucket | Included families / subfamilies | Current result | Next action |
| --- | --- | --- | --- | --- |
| A | Restriction / Counterplay Suppression | `15.neutralize_key_break`, `15.prophylactic_move`, `15.defensive_regrouping`, `8.half_open_file_pressure`, `8.open_file_control` | `neutralize_key_break`, `prophylactic_move`, and `half_open_file_pressure` promoted; `defensive_regrouping` absorbed into `prophylactic_move`; `open_file_control` absorbed into `half_open_file_pressure` | closed unless new exact-board corpus changes one of the absorbed cells |
| B | Target Fixation / Weakness Pressure | reviewed `static_weakness_fixation`, `backward_pawn_targeting`, `target_focused_coordination`, `minority_attack_fixation`, `iqp_inducement` | cluster B now has three live exact slices on the canonical path: `B21` / `B21A` keep the existing move-local `exact_target_fixation` lane (`bestDefenseBranchKey=f3d2|b8a6`, `sameBranchState=Proven`, `persistence=Stable`, `main_bundle=This keeps the pressure fixed on d6.`, planner-primary `WhatChanged=This changes the position by fixing d6 as the target.` plus same-branch contrast/consequence), `B15A` / `B16B` keep the Carlsbad fixed-target current-position probe (`proofSource=carlsbad_fixed_target_probe`, `proofFamily=backward_pawn_targeting`, `scope=PositionLocal`, `main_bundle=The key strategic fact here is that c6 is the fixed target.`, planner-primary `WhatMattersHere`, deterministic MoveReview / Chronicle primary, and coda `So the task is to keep the queenside pressure trained on c6 instead of rushing a conversion.`), and `K09A` / `K09D` now open the second planner-owned current-position probe lane on exact coordination (`proofSource=target_focused_coordination_probe`, `proofFamily=target_focused_coordination`, `scope=PositionLocal`, `bestDefenseBranchKey=d1b3|d8d7` on `K09A` / `h2h3|g4f3` on `K09D`, `sameBranchState=Proven`, `persistence=Stable`, `main_bundle=The key strategic fact here is that the pressure is coordinated on c6.`, planner-primary `WhatMattersHere`, and deterministic MoveReview / Chronicle coda `So the task is to keep the pressure coordinated on c6 until the target has to give way.`); the same exact `B21A` packet still backs the only support-only `decisionComparison` comparative digest (`Nd2` versus `Qc2`: `Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch.`); `minority_attack_fixation` stays absorbed support-only; `iqp_inducement` stays structure/conversion support-only; `K03A` remains fail-closed and `K09E` remains file-pressure / release-rival blocked | keep all three exact lanes narrow: `B21` / `B21A` stay move-owned delta only, `B15A` / `B16B` stay Carlsbad current-position probe only, `K09A` / `K09D` stay exact coordination current-position probe only, and fail-close siblings unless a new reviewed row survives exact board + same-branch proof without relabel drift |
| C | Exchange / Removal Without Task-Shift | `17.trade_key_defender`, bounded `favorable_simplification`, bounded `trade_attacking_piece`, bounded `remove_key_defender` trigger-only form | bounded `favorable_simplification` is now promoted on one exact same-task simplification slice; `trade_key_defender` stays blocked; `trade_attacking_piece` is now reviewed fail-closed because `K08A` is not a real live-attacker removal row, `K08D` is not root-best, and exploratory `MI5` collapses into tactic-first `Qxd6` relief; `remove_key_defender` is now reviewed fail-closed because `K09B`/`K09F` only survive as same-task simplification on `d4e6|f7e6`, `K09A` / `K09D` now belong to the separate `target_focused_coordination` current-position probe lane, `K09E` stays file-pressure / release-rival dominated, and `MI5` is still tactic-first relief rather than a one-defender local trigger | keep the promoted simplification slice narrow and keep `trade_key_defender`, `trade_attacking_piece`, and `remove_key_defender` blocked; reopen `trade_attacking_piece` or `remove_key_defender` only with a new exact row whose planner-owned move-local owner survives without relabeling into simplification, blocked defender-trade ownership, attack-piece removal, tactical cleanup, or the exact coordination probe lane |
| D | Coordination / Access Conversion | bounded `support_file_occupation`, bounded `entry_square_enable_deny`, bounded `target_focused_piece_coordination` | `entry_square_enable_deny` reviewed and blocked; `support_file_occupation` and `target_focused_piece_coordination` remain queued | keep `entry_square_enable_deny` blocked unless a new exact root-best row materializes a one-square move-local owner without relabeling into prophylaxis or file-entry reuse; keep the other two cells queue-only |
| E | Pawn-Break Timing | `central_break_timing`, `wing_break_timing`, `tension_maintenance` | `central_break_timing` has a bounded `SupportedLocal` path through board-backed break support; plan-only central-break rows are review-only; `wing_break_timing` and `tension_maintenance` remain closed | keep central-break release limited to the certified packet; do not widen generic `plan_advance`, exact-row whitelists, or plan-only carriers into player-facing owners |

Current mid-layer priority remains machinery-first for the remaining unopened
review cells:
deepen owner seed, same-branch continuation, rival-story arbitration, and
structure-transition witness inside the existing canonical owner-path modules
while keeping the newly opened Cluster B slice narrow, the central-break timing
packet bounded, the remaining C / D reviews fail-closed, and any future
pawn-break timing broadening closed until it survives the same proof burden.

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
   - the historical Chronicle / MoveReview / Active replay pass verified that
     planner legality was not rewritten; current product authority keeps only
     the MoveReview side of that result
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
- ranking/planner-owner-kind redesign
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

## CTH: Commentary Trust Boundary

CTH is the trust-first workstream that sits in front of Track 5.

Its job is:

`reduce false positive, overclaim, overgeneralization, out-of-scene generalization failure, and cross-surface inconsistency before lesson extraction`

Use [CommentaryTrustBoundary.md](modules/commentary/docs/CommentaryTrustBoundary.md)
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
  current bounded scope complete inside that narrow charter
- `CTH-B3`:
  bounded dual-axis clamp / bind slice implemented and adversarial-review green
  inside that narrow charter
- `CTH-B4`:
  design baseline consumed by one narrow runtime slice; boundary fix-up is
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
  - MoveReview: `signoff-ready`
  - Chronicle / Active: `removed product surfaces; historical signoff context only`

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
  legality, ranking, or planner-owner-kind design

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
  explicitly deferred behind trust boundary; do not treat as the next
  implementation priority while false positive / overclaim /
  overgeneralization / out-of-scene generalization failure /
  cross-surface inconsistency remain active concerns

### Where CQF details now live

There is no separate CQF appendix anymore.

Use:

- [CommentaryProgramMap.md](modules/commentary/docs/CommentaryProgramMap.md)
  for track map and current status
- [CommentaryTruthBoundary.md](modules/commentary/docs/CommentaryTruthBoundary.md)
  for signoff interpretation and closed quality-lane boundaries that affect
  authority decisions
- [CommentaryTrustBoundary.md](modules/commentary/docs/CommentaryTrustBoundary.md)
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

- the B-route runtime remains narrow:
  bounded B1 / B2 / B3 / B4, B5b containment, and B6b planner-only
- separately, `central_break_timing` has a bounded `SupportedLocal`
  move-delta path through board-backed break support
- the only live B4 positive scope remains:
  clearly-better late-middlegame local file-entry bind certification with one
  single corroborating entry-square axis on the existing
  `RestrictionProphylaxis` lane
- B5a is design-only:
  no new positive slice is open, and the selected next recon family is
  heavy-piece local bind on a negative-first criticism/validation lane
- B5b is implemented only as a negative-first containment slice and is now
  current bounded scope complete inside that bounded charter:
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
  route-chain / sector-network / color-complex positive authority expansion,
  and heavy-piece positives remain closed
- B6 family expansion baseline is now design-ready:
  the next bounded candidate is broader route-chain rollout, but only as one
  exact same-branch intermediate-node chain above the current B6 triplet;
  posture/phase expansion stays deferred until that chain survives exact-board
  criticism, replay / whole-game positive reuse stays deferred after that, and
  sector-network / color-complex remain unsafe; color-complex exact-FEN
  readiness now has a dedicated proof-shape evaluator, controlled positive
  tests, one real source survivor, and full-game source-catalog review
  candidates, but authority status is unchanged
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
- Track 4 no longer has a collection blocker and remains maintenance-ready on
  the corpus-maintenance lane

Representative preserved rows include:

- `Ke7`
- `Nxe3`
- `Rxc4`

The selected quiet-support subset remains useful for source isolation on the
representative `34. a5` row, but after the diagnostic-only fallback close a
runtime gate pass is not an applied MoveReview lift: exact-factual MoveReview
output must keep `quietSupportLiftApplied=false` and may audit the gate
separately from visible prose.

MoveReview evidence-coverage audit is now a test/tooling-only maintenance
surface. Corpus JSONL rows can record the final source kind
(`planner`, `basic_move_explanation`, or `exact_factual_fallback`), basic-lane
emission / blocker status, and SupportedLocal candidate / admitted / reject
families. `MoveReviewEvidenceCoverageAudit` consumes those diagnostics to rank
the next runtime expansion candidates separately from evidence-gap candidates.
This does not change player-facing prose or reopen the single
planner -> basic move explanation -> exact factual fallback pipeline.

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
  and the boundary fix-up has closed its known entry-persistence,
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
  [CommentaryTrustBoundary.md](modules/commentary/docs/CommentaryTrustBoundary.md)
  and extend the relevant regression packs before reopening the frontier; if a
  future session wants B5b work, start from the B5a criticism matrix and
  broad-validation charter there rather than widening the live slice ad hoc

## How To Read The Docs In A New Session

If a user says:

- `Step 1-7 is done`
- `CQF is ongoing`
- `trust-maintenance is the active posture`

read documents in this order:

1. [CommentaryProgramMap.md](modules/commentary/docs/CommentaryProgramMap.md)
2. [CommentaryPipelineSSOT.md](modules/commentary/docs/CommentaryPipelineSSOT.md)
3. [CommentaryTruthBoundary.md](modules/commentary/docs/CommentaryTruthBoundary.md)
4. [CommentaryTrustBoundary.md](modules/commentary/docs/CommentaryTrustBoundary.md)

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
- `truth boundary`
  - signoff criteria
- `quality artifacts`
  - local rerun/eval outputs, not canonical docs

## One-Paragraph Handoff

Use this wording when you need a concise new-session handoff:

`Step 1-7 is complete and signoff-ready. That closed scene-first admission, owner legality, planner/build/replay consistency, surface parity, and collision signoff. We are now in CQF, but the active trust-owning rollout remains narrow: the B-route runtime is bounded to B1 / B2 / B3 / B4, B5b negative-first containment is current bounded scope complete, and B6b remains planner-only on the same restriction-prophylaxis lane. Separately, central_break_timing has a bounded SupportedLocal move-delta path through board-backed break support; plan-only central-break rows remain review-only. Broad route-chain, sector-network, heavy-piece positive, slight-edge squeeze, and color-complex positive authority stay closed; exact color-complex pressure may appear only as non-authority selector `ValidatedPressure`, while `ColorComplexSqueezeProof` is proof-readiness only. Karpov-Unzicker 1974 and Alekhine-Bogoljubow 1936 are real `engine_persistent_readiness_candidate` rows, but source-catalog readiness rows remain review/source-intake artifacts, not authority. Break-prevention harmless-transform review is route-token guarded and has one fixed real-game intake survivor, Pomar-Toran 1969 ply 27, which admits only through the existing `neutralize_key_break` packet and does not open broad transform authority. Use CommentaryProgramMap first, then SSoT and CommentaryTruthBoundary; detailed rerun evidence lives in local quality artifacts rather than a separate appendix document.`
