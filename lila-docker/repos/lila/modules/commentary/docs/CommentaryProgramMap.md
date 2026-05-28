# Commentary Program Map

This is the onboarding map for the current Chesstory commentary-analysis work.
It points to the live authority documents and summarizes the current operating
state.

## Current Status

Current work is a boundary redesign plus docs compression:

- claim authority is centralized under `analysis.claim`
- planner authority checks are delegated through `PlannerClaimAdmission`
- render release safety is centralized under `analysis.render.FragmentAuthority`
- public MoveReview wire is minimized around backend-owned structured
  diagnostics and the certified `moveReviewPlayerSurface`
- opening-family support rows now enter that player surface only through
  `OpeningFamilyCatalog` plus `OpeningFamilyClaimResolver` `SupportedLocal`
  admission, not through rendered prose or frontend reconstruction
- static opening coverage now comes from `openings.tsv` runtime rows plus
  tooling provenance reports, while preserving the same label-plus-FEN family
  proof boundary; the removed Scala broad-variation fixture floor is no longer
  treated as coverage authority. The pool is currently pruned to 1276 rows that
  replay against captured Lichess masters evidence as `master-backed`; the live
  audit found and removed 438 `not-found-in-masters` expansion rows.
- static opening coverage expansion is paused for provenance cleanup:
  `lila.commentary.tools.opening.OpeningPoolAudit` and
  `OpeningMasterDbAuditRunner` now classify parse issues, normalized endpoint
  duplicates/transpositions, and optional master DB evidence under
  `modules/commentaryTools/src/test`; `--base-url` selects the masters endpoint
  while the default remains Lichess. `--since`/`--until` are optional query
  window parameters for endpoints that accept them; current `/masters` live
  audit should normally run without dates because date-windowed master queries
  can return `HTTP 400`. Live runs can write replayable raw-response JSONL with
  `--write-evidence-cache`; fetch or parse failures are reported as
  `master-fetch-error`. Reports include `provenanceStatusCounts`, and
  `--only-status` narrows the output rows for cleanup triage. `--skip-rows`,
  `--max-rows`, and `--request-timeout-seconds` support chunked/resumable live
  audit. Rows remain unverified unless a live OAuth-backed master DB report or
  replayable JSONL evidence cache keyed by endpoint-stable `rowId` marks them
  `master-backed`
- opening knight-route coverage is expanding through `opening_routes.tsv`
  descriptors while preserving legal replay plus target-mode proof gates.
  `OpeningRouteMiningRunner` is tooling-only support under
  `modules/commentaryTools/src/test`; it mines candidate knight routes from the
  master-backed `openings.tsv` pool, filters out one-ply generic development
  and repeated-square paths, and leaves low-support candidates deferred. The
  current runtime route catalog contains 48 descriptors, and every route target
  is present in the corresponding `OpeningFamilyCatalog` target allowlist.
- opening goal/prose coverage is expanding inside the existing `OpeningGoals`
  evaluator for Gruenfeld `...d5`, Slav/Semi-Slav `...e5`, Dutch `...Ne4`,
  Queen's Indian `...Ne4`, and Bogo-Indian `...Ne4` structures; those
  evaluations still flow only through `openingGoalEvaluation` into
  outline/explanation consumers
- canonical docs stay as four files, but historical CTH logs are compressed
  into current-state rules and summary tables

Do not use branch-external or branch-removed documents as authority for this
worktree.

## Document Roles

Read the documents in this order:

1. `CommentaryProgramMap.md`
   - onboarding, current status, active frontier, document navigation
2. `CommentaryPipelineSSOT.md`
   - canonical runtime path and module ownership
3. `CommentaryTruthBoundary.md`
   - chess-truth signoff and no-go failures
4. `CommentaryTrustBoundary.md`
   - trust-risk map, CTH summary, authority ladder, defer rationale

## Live Architecture Boundary

Current authority is internal and MoveReview-first:

- `ProofContractRules` defines proof-family eligibility.
- `ClaimAuthorityResolver` resolves `CertifiedOwner`, `SupportedLocal`,
  `DiagnosticOnly`, or `Suppressed`.
- `PlannerClaimAdmission` connects planner plans/inputs to that resolver.
- `QuestionFirstCommentaryPlanner` ranks questions; it does not own proof
  authority.
- `FragmentAuthority` decides renderer release safety.
- `CommentaryApi` and frontend code consume typed payloads only.

## Active Frontier

Open for maintenance:

- exact-board promoted slices already covered by proof contracts
- source/test tooling that improves exact witness quality
- trigger hardening that removes generic string overlap, unknown-subtype
  fail-open, or blocked battery geometry without expanding public authority
- docs and package cleanup that preserves the current authority boundary

Closed unless a new audit explicitly opens them:

- broad heavy-piece/local-bind/global-squeeze expansion
- Track 5 lesson authority
- Chronicle/Active runtime reopening; their remaining planner and compression
  helpers are test/tooling-only legacy diagnostics
- public API/frontend wire expansion except audited typed diagnostics or
  payload minimization that does not create product authority
- support-only or deferred carrier promotion

Color-complex has an explicit exact-board contract. It is closed to generic
`color_complex_squeeze` source packets and opens only through
`color_complex_squeeze_probe`, where the board proves that a friendly bishop or
knight attacks the opponent-owned semantic weak square and the packet branch is
proven/stable. The packet contract consumes a typed exact-slice proof, not
generic coordinate/minor-piece strings:

| proof family | proof source | status | certified | supported local | failure |
| --- | --- | --- | --- | --- | --- |
| `color_complex_squeeze` | `color_complex_squeeze_probe` | `Releasable` | true | true | `color_complex_authority_closed` |

## Verification Expectations

For package/naming/boundary work:

- compile and run targeted tests serially
- keep runtime and test/tooling boundaries separate
- update the relevant canonical doc in the same change
- report unverified work as boundary cleanup only

For claim authority or trust behavior:

- include claim/proof tests
- include planner tests when admission changes
- include renderer or parity tests when release text changes
- include color-complex tests when deferred authority is touched

## Maintenance Triggers

Re-audit the runtime docs only when:

- the user asks for a fresh audit
- code changed after the relevant snapshot in commentary runtime, controller,
  API, or analyse frontend paths
- a task introduces a runtime path not covered by the current SSoT

When changing the audited pipeline, update `CommentaryPipelineSSOT.md`.
When changing trust-relevant behavior, update `CommentaryTrustBoundary.md`.
When changing truth/signoff behavior, update `CommentaryTruthBoundary.md`.
