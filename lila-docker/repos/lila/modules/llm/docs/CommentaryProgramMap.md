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
  the 2026-04-02 helper-backed closeout campaign reproduced the after-trigger
  defended branch `a7a5 b4a5 c6a5 f3e5 ... a5c4`, but did not reproduce the
  earlier `a3b4` root-best reading on the control FEN. The same session also
  screened `K09C`, `K09G`, `K09H`, `B21A`, and `Rubinstein-Duras 1911` and
  found no second independent exact-FEN survivor for the same
  planner-owned intermediate-only charter. The practical result is narrower
  than the earlier close-ready reading:
  current branch keeps the exact intermediate route-chain visible only on the
  backend helper lane, not as planner-owned live truth, so A-track closeout
  does not advance from it, and
  broader posture/phase expansion plus replay / whole-game positive reuse stay
  deferred behind the exact-board burden
- CTH-B7:
  design/recon/charter baseline ready only:
  no B7 runtime slice is open, and the only candidate allowed into B7b review
  is one same-defended-branch bounded task shift after forced simplification.
  Broad `good endgame` / favorable-transition rhetoric remains closed; B7b
  now has one exact-FEN review control plus a stronger exact-board nasty-case
  pack, but no rollout is reviewable until the owner-path and task-vocabulary
  blockers are closed. The 2026-04-02 exact-FEN corpus recon strengthened the
  nasty pack, and the late-middlegame positive hunt did recover one reviewable
  local branch candidate on
  `r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13`
  after root-best `Nxe6`. But the B7b implementation review blocked rollout:
  the current runtime still sees that position as `Opening / ExplainTactics`
  with no planner-owned move owner, so the only surviving hook is the
  line-scoped preview `Nxe6 fxe6 Rad1`. Runtime therefore remains closed. The
  blocker is now modeled as two linked absences on the existing architecture:
  no owner-admissible backend-only task-shift carrier/materialization and no
  closed `currentTask -> shiftedTask` vocabulary for the bounded handoff.
  The backend-only contract design freeze is now the canonical baseline, and
  runtime remains deferred on that frozen envelope. The 2026-04-02 validation
  pass held the frozen pair against the current near-miss / blocker corpus, but
  reopen now also requires one second non-`K09B` exact-FEN survivor for the
  same pair. The 2026-04-02 second-survivor hunt did not find one, so B7
  remains explicitly deferred as a one-row-overfit family on the current corpus.
  A follow-up parallel local/web-assisted hunt also reran `K09I` plus three
  external exact-FEN leads and found that none even survived the root-best
  simplification gate, so B7 status remains `still overfit / more corpus work
  needed`
- CTH-B8:
  `B8a` first-slice design + corpus validation is now
  `still too fuzzy / more corpus work needed`.
  The 2026-04-02 pass narrowed the only plausible unit to one same-branch
  sector-local active-plan collapse, but the rerun corpus did not produce two
  independent exact-FEN survivors.
  Local QC seeds such as `Qa5` on
  `2b3k1/p3qp1p/6p1/4P3/3p4/P2B3P/3Q1PP1/6K1 w - - 0 28`
  and `Ke7` on
  `3R1k2/5ppp/ppb2n2/2r5/P1p5/2N1P1N1/1PP3PP/6K1 b - - 0 27`
  stayed slight-edge but failed the contract because the defender kept
  multiple active families on the root-best branch, while external exact-FEN
  leads either equalized, drifted into clearly-better pressure, or collapsed
  into B1 / B6 / B7 relabel risk.
  B8 therefore remains design-only:
  no runtime slice, owner lane, schema, or prose family is opened.
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
  but the 2026-04-02 closeout campaign did not find the required second
  survivor for B6 family completion. The helper-backed rerun kept the after-
  trigger `a5` detour branch but returned `c3b4`, not `a3b4`, as root best on
  the control FEN, and no admissible second survivor emerged from the screened
  `K09*`, `B21A`, or `Rubinstein-Duras` pool. B6 family expansion therefore
  remains one-example heavy / plateaued at A-track closeout, and posture/phase
  plus replay reuse remain deferred behind the same exact-board burden
- `CTH-B7`:
  design/recon/charter baseline ready only:
  no runtime slice is open; the selected candidate is one same-defended-branch
  bounded task shift after forced simplification, but rollout remains closed
  because the current K09B review control survives exact-board engine replay
  but not runtime-owner review; the blocker is now fixed as owner-path
  admissibility plus a missing backend-only `currentTask -> shiftedTask`
  vocabulary, not as exact-board discovery. The backend-only contract design
  freeze is now canonical, and B7 runtime remains deferred on that baseline
  rather than runtime reopening. The follow-up second-survivor hunt found no
  non-`K09B` late-middlegame exact-FEN survivor for the frozen pair, so current
  B7 status stays `still overfit / more corpus work needed`; the later parallel
  hunt tightened that conclusion by eliminating `K09I` and three web-recovered
  historical exact-FEN leads at the root-best trigger gate
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

`Step 1-7 is complete and signoff-ready. That closed scene-first admission, owner legality, planner/build/replay consistency, surface parity, and collision signoff. We are now in CQF, but the active trust-owning rollout remains narrow: the live runtime is still bounded B1 / B2 / B3 / B4, B5b is current-bounded-scope complete as a negative-first containment slice only, and B6b remains the only route-network positive attempt on the same restriction-prophylaxis lane: clearly-better late-middlegame named route-network bind still requires a certified B4 file-entry pair plus one non-redundant reroute denial on the same defended branch, opens only planner-owned WhyThis wording, and stays fail-closed on Chronicle / Bookmaker / Active / whole-game replay. The 2026-04-02 closeout campaign did not find a second independent B6 survivor and did not reproduce the earlier `a3b4` root-best reading under the helper-backed matrix rerun, so A-track closeout is plateaued rather than advanced. The broader route-chain intermediate remains backend-visible only on the current branch; planner-owned live wording for that wider slice is closed again until a fresh exact-FEN root-best control exists. Broad route-chain, sector-network, heavy-piece positive, slight-edge squeeze, and color-complex expansion stay closed. Use CommentaryProgramMap first, then SSoT and TruthGate; detailed rerun evidence lives in local quality artifacts rather than a separate appendix document.`
