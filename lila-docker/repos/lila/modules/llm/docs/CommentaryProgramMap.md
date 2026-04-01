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
  bounded B1 / B2 / B3 slices implemented and adversarial-review green inside
  their current charters
- Current operating mode:
  maintenance-only re-review, not a new automatic frontier
- Reopen broader CTH work only if a fresh runtime change touches an audited
  seam or a new trust regression appears

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
- `CTH-B1` / `CTH-B1b`:
  bounded restricted-defense conversion slice implemented, broad validation and
  reverse adversarial review green
- `CTH-B2a` / `CTH-B2b` / close review:
  bounded named-break / entry-axis suppression slice implemented and
  `close-ready` inside that narrow charter
- `CTH-B3a` / `CTH-B3b`:
  bounded dual-axis clamp / bind slice implemented and adversarial-review green
  inside that narrow charter
- broader `CTH-B`:
  paused; do not open a new frontier automatically

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
  Track 3 Phase A acceptance passed on real16:
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

The latest CQF Track 3 Phase A rerun remains acceptance-passing, and CQF Track 4
scene coverage is now a close-candidate on the corpus-maintenance lane.

### Current status

Track 3 is in maintenance monitoring, not an active blocker.

Quiet-support wording itself is no longer the main blocker, and the
planner-owned blocked-lane forcing-row preservation blocker remains closed.

Representative preserved rows include:

- `Ke7`
- `Nxe3`
- `Rxc4`

The selected quiet-support subset also remains intact on the representative
`34. a5` row (`runtimeGatePassCount=4`, `quietSupportLiftAppliedCount=4`,
baseline selected owner/question divergence `=0`, stronger-verb leakage `=0`).

The remaining residual review row is `Ng4`, a style-choice /
`pv_delta_missing` upstream lane outside the Track 3 Phase A planner fix.

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

The current trust posture is maintenance-only:

- CTH-A is no longer an active implementation frontier
- bounded CTH-B1 / B2 / B3 slices are implemented and adversarial-review green
- broader squeeze / no-counterplay / strategic-discovery expansion is paused
- if a fresh runtime change reopens an audited seam, update
  [CommentaryTrustHardening.md](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)
  and extend the relevant regression packs before reopening the frontier

## How To Read The Docs In A New Session

If a user says:

- `Step 1-7 is done`
- `CQF is ongoing`
- `Track 4 is the active lane`

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

`Step 1-7 is complete and signoff-ready. That closed scene-first admission, owner legality, planner/build/replay consistency, surface parity, and collision signoff. We are now in CQF, which improves commentary quality without reopening legality. Track 1 is maintenance-mode, Track 2 contrast is signoff-ready on Bookmaker/Chronicle, Track 3 quiet-support Phase A remains acceptance-passing on real16, and Track 4 scene coverage is a close-candidate on the corpus-maintenance lane after the important bucket set was fully materialized without a new failure class. Use CommentaryProgramMap first, then SSoT and TruthGate; detailed rerun evidence lives in local quality artifacts rather than a separate appendix document.`
