# Commentary Program Map

This is the onboarding map for the current Chesstory commentary-analysis work.
It points to the live authority documents and summarizes the current operating
state.

## Current Status

Current work is a boundary redesign plus docs compression:

- claim authority is centralized under `analysis.claim`
- planner authority checks are delegated through `PlannerClaimAdmission`
- render release safety is centralized under `analysis.render.FragmentAuthority`
- public MoveReview API/wire/frontend shape is unchanged
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

`ClaimAuthorityPolicy` remains as a compatibility facade to reduce call-site
churn during the redesign.

## Active Frontier

Open for maintenance:

- exact-board promoted slices already covered by proof contracts
- source/test tooling that improves exact witness quality
- docs and package cleanup that preserves the current authority boundary

Closed unless a new audit explicitly opens them:

- broad color-complex squeeze authority
- broad heavy-piece/local-bind/global-squeeze expansion
- Track 5 lesson authority
- public API/frontend wire changes
- support-only or deferred carrier promotion

Color-complex has an explicit deferred contract:

| proof family | status | certified | supported local | failure |
| --- | --- | --- | --- | --- |
| `color_complex_squeeze` | `Deferred` | false | false | `color_complex_authority_closed` |

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
