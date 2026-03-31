# Agent Instructions

For tasks touching the Chesstory commentary-analysis pipeline, first use
`lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md`
as the onboarding map, then use
`lila-docker/repos/lila/modules/llm/docs/CommentaryPipelineSSOT.md`
as the canonical runtime audit.

Scope includes helper modules and consumption paths across:
- `strategic`
- `semantic`
- `endgame`
- `opening`
- `probe`
- `plan`
- `pawn`
- `structure`
- `threat`
- `practicality`
- `counterfactual`
- `authoring`
- outline / renderer / API / frontend

Do not redo the full `producer -> carrier/model -> builder -> outline -> renderer -> API -> frontend`
trace if the request is already answered by that SSoT.

Use the document roles strictly:
- `CommentaryProgramMap.md`
  - onboarding / current status / Step 1-7 + CQF map
- `CommentaryPipelineSSOT.md`
  - canonical runtime audit
- `CommentaryTruthGate.md`
  - canonical signoff / truth gate
- `CommentaryTrustHardening.md`
  - canonical trust-risk map, CTH audit baseline, Track 5 defer rationale,
    trust-hardening priorities

There is no separate CQF appendix anymore. CQF status, track map, and active
quality-work handoff live in `CommentaryProgramMap.md`, while canonical signoff
and runtime policy remain in `CommentaryTruthGate.md` and
`CommentaryPipelineSSOT.md`. Trust-hardening work and lesson-readiness questions
must also use `CommentaryTrustHardening.md`.

Re-audit only if:
- the user explicitly asks for a fresh audit,
- code changed after the SSoT snapshot in
  `lila-docker/repos/lila/modules/llm/src/main`,
  `lila-docker/repos/lila/app/controllers/LlmController.scala`, or
  `lila-docker/repos/lila/ui/analyse/src`,
- or the task introduces a runtime path not covered by the SSoT.

When changing the audited pipeline, update the SSoT in the same change.

When changing trust-relevant behavior, update
`lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md`
in the same change. This includes:
- fallback truth projection or rewrite behavior
- cross-surface contract consumption
- support-only carrier exposure that can alter user-facing implication
- lexicon/template authority boundaries
- Track 5 lesson-readiness guards or defer rationale

## Naming, Packaging, and Verification Guardrails

When doing module cleanup, naming cleanup, or package boundary work in this repo,
follow these rules strictly:

### Stable names only
- Do **not** put temporary rollout labels in module, package, or file names.
- Avoid names such as `Track`, `Phase`, `Frontier`, `Prototype`, `Kickoff`, `V2`, `V3`
  unless they are part of a real long-lived domain model.
- Prefer role-based, stable names such as:
  - `quality`
  - `contrast`
  - `quiet`
  - `render`
  - `practical`
  - `review`
  - `realpgn`
- If a name reflects a project step or roadmap phase rather than a runtime/test role,
  it is probably the wrong name.
- This rule applies to:
  - source file names
  - package names
  - object/class names
  - runner/support/test helper names
- Experiment or run identifiers may appear in generated report filenames under `tmp/` or
  similar output directories, but **must not** become source module names.

### Runtime and test/tooling must not blur together
- Keep runtime helpers under `modules/llm/src/main/...` in role-based packages.
- Keep CQF runners, corpus tools, eval scaffolds, and report builders under
  `modules/llm/src/test/...` in test/tooling packages.
- Do **not** create runtime modules whose main purpose is experiment staging,
  report generation, or corpus evaluation.
- Do **not** mix runtime owner-path logic with test-only evaluation scaffolds.
- If a helper exists only to evaluate, compare, score, classify, or report over artifacts,
  it belongs in `src/test`, not `src/main`.
- If a helper participates in live planner/build/replay behavior, it belongs in `src/main`
  and must not depend on test/tooling code.

### Boundary cleanup before rename churn
- For package cleanup, define the target boundary first, then move files by role.
- Do **not** rename broadly first and discover boundaries later.
- Prefer subpackages over new flat helper files when a role is shared across
  multiple call sites.
- If logic is single-surface and single-call-site, prefer a private helper over
  introducing a new top-level support file.
- Before starting a cleanup, write down the target boundary map explicitly:
  - which files are runtime
  - which files are test/tooling
  - which package each family should live in
- Do **not** leave mixed-role siblings in a flat directory when they clearly belong to
  different families such as `quality`, `bookmaker`, `active`, `review`, or `realpgn`.
- If a family has become large enough to need multiple helper files, give it a dedicated
  subpackage instead of keeping it in a catch-all flat package.

### Verification discipline
- After package moves or naming cleanup, run compile plus targeted tests before
  reporting the work as done.
- Do **not** claim quality gain, acceptance, or signoff from a mixed-worktree
  diff. If the result is confounded by unrelated drift, report it as isolation
  only, not as acceptance.
- When before/after evidence is scope-sensitive, explicitly separate:
  - target slice vs visible aggregate
  - runtime behavior vs test/tooling artifact changes
  - replay-layer issues vs upstream input issues
- For naming/package cleanup specifically, do not stop at file moves:
  - fix imports
  - fix package declarations
  - fix `runMain` / runner strings
  - fix doc references to the moved module names
- Report cleanup as:
  - `boundary cleanup only`, or
  - `boundary cleanup + verified compile/test`
  rather than implying product behavior changed.

### Commentary pipeline-specific reminder
- New CQF or commentary-analysis helpers must reuse the existing planner/build/
  replay architecture rather than introducing parallel runtime paths.
- If a cleanup changes audited runtime package paths, update
  `lila-docker/repos/lila/modules/llm/docs/CommentaryPipelineSSOT.md`
  in the same change.
- If a cleanup changes canonical helper names or directory ownership, update the relevant
  CQF or truth-gate docs in the same change so a new session does not reintroduce stale
  rollout-era names.
