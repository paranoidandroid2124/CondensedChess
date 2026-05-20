# Commentary Pipeline SSOT

This file is the canonical runtime audit for the current Chesstory
commentary-analysis pipeline.

For orientation and document roles, read `CommentaryProgramMap.md` first.
For truth signoff, read `CommentaryTruthBoundary.md`. For false-positive and
overclaim risk, read `CommentaryTrustBoundary.md`.

## Authority

This document owns the current pipeline map for:

- `modules/commentaryCore/src/main/scala/lila/commentary/analysis`
- `modules/commentary/src/main/CommentaryApi.scala`
- `app/controllers/CommentaryController.scala`
- `ui/analyse/src`
- commentary prompt/polish paths when they affect released commentary

If a runtime pipeline change makes this document stale, update this document in
the same change. Do not append dated logs; rewrite the current-state map.

## Product Surface

User-facing commentary authority is MoveReview-only.

Removed product surfaces such as Game Chronicle, Guided Review, Defeat DNA, and
Active strategic-note UI/API entrypoints are not public authority paths. Names
for Chronicle, Active, Game Arc, or whole-game replay may remain in source and
test tooling only as internal diagnostics, shared proof/planner infrastructure,
or historical fixtures.

No current change may reintroduce public payloads, frontend panels, or owner
claims from those removed surfaces without a new runtime audit.

## Runtime Path

The maintained path is:

1. Board, PGN, engine, probe, and semantic signals enter the commentary runtime.
2. `CommentaryEngine` derives move-local facts and candidate key moments.
3. `MoveTruthFrame` is built before prose.
4. `DecisiveTruthContract` projects the truth frame into the surface contract.
5. Strategic carriers are built by `StrategyPackBuilder` and normalized by
   `StrategyPackSurface`.
6. Proof producers may emit `PlayerFacingClaimPacket` values.
7. `ProofContractRules` states which proof families can ever reach authority.
8. `analysis.claim` resolves claim authority:
   - `ClaimAuthorityDecision` defines the tier and failure-code model.
   - `ClaimAuthorityResolver` owns certified/support/suppressed/diagnostic
     decisions from packet, plan, and truth contract.
   - `PlannerClaimAdmission` connects planner inputs to the resolver.
   - `ClaimAuthorityPolicy` remains a compatibility facade only.
9. `QuestionFirstCommentaryPlanner` selects and ranks questions. It does not
   own low-level proof/source/scope/fallback authority.
10. `NarrativeOutlineBuilder` assembles beats from admitted inputs.
11. `analysis.render.FragmentAuthority` decides fragment release safety for
    render-only, support-only, unsafe truth, unsafe lesson, and anchor-required
    fragments.
12. `NarrativeOutlineValidator` remains the final scrubber.
13. `CommentaryApi` serializes typed payloads; it does not recompute boundary
    authority.
14. `moveReview.ts`, `narrativeView.ts`, and `responsePayload.ts` render typed
    fields and do not rebuild hidden strategic meaning from fallback carriers.

## Ownership Map

Runtime ownership boundaries:

- Truth: `MoveTruthFrame`, `DecisiveTruthContract`,
  `PlayerFacingTruthModePolicy`
- Proof contracts: `ProofContractRules`
- Claim authority: `analysis.claim`
- Planner selection: `QuestionFirstCommentaryPlanner`
- Render release safety: `analysis.render.FragmentAuthority`
- Final cleanup: `NarrativeOutlineValidator`
- API payload shape: `CommentaryApi`
- UI structure: analyse frontend modules

`QuestionFirstCommentaryPlanner` may record diagnostic fields such as
`authorityTier`, `authorityFailureCodes`, `proofFamily`, and `proofSource`, but
those fields are not a new public owner kind and do not change wire shape.

## Authority Tiers

The internal claim-authority tiers are:

- `CertifiedOwner`: exact proof contract and truth context allow the claim to
  own the player-facing explanation.
- `SupportedLocal`: exact local evidence allows bounded phrasing, usually
  qualified as a local reading.
- `DiagnosticOnly`: the runtime may trace or evaluate the idea, but it cannot
  release as a player-facing owner or support-local claim.
- `Suppressed`: the idea is blocked by tactical truth, missing proof, scope
  mismatch, fallback risk, or another explicit failure code.

Support-only, deferred, latent, or diagnostic carriers may not become owner
claims through planner, renderer, API, or frontend code.

## Proof Contract Registry

`ProofContractRules` owns the relation between proof families and admissible
authority tiers. It should define only contract eligibility, required witness
shape, source compatibility, scope compatibility, and default failure taxonomy.

It must not contain planner ranking policy or renderer wording policy.

Current explicit authority-closed family:

| proof family | status | certified | supported local | default failure |
| --- | --- | --- | --- | --- |
| `color_complex_squeeze` | `Deferred` | false | false | `color_complex_authority_closed` |

Color-complex support may remain selector or corpus-review evidence. It is not
runtime authority until exact-board survivor proof is promoted by contract.

## Renderer Boundary

Renderer authority is separated from beat assembly:

- `NarrativeOutlineBuilder` chooses and joins outline beats.
- `FragmentAuthority` decides whether tagged fragments release text.
- `NarrativeOutlineValidator` removes any leaked unsafe or helper-labeled text.

Unsafe lesson/truth fragments are dropped. Generalized support-only fragments
release only when they are move-linked, scene-grounded, evidence-backed,
planner-owned, or contract-consistent.

## API And Frontend Boundary

Backend and frontend consumers must not reconstruct strategic panels or
decision comparisons from fallback-only data.

Forbidden reconstruction sources include:

- `topEngineMove`
- `cpLossVsChosen`
- latent plans
- deferred carriers
- support-only packets
- free-form strategy text
- absent decision-comparison diagnostics

The frontend renders typed surviving payload fields only.

## Current Verification Targets

After claim authority, planner, proof-contract, renderer, or package-boundary
changes, run compile plus targeted tests serially. Relevant targets include:

- claim authority / proof contract tests
- `QuestionFirstCommentaryPlannerTest`
- surface replay / cross-surface trust regression tests when present in the
  current worktree
- color-complex tests
- commentary core compilation when package paths move

Do not run parallel `sbt` commands in this worktree.

## Maintenance Triggers

Update this file when any of these change:

- runtime path or module ownership
- claim authority tiering or resolver behavior
- proof contract eligibility
- planner admission or ranking inputs
- renderer release rules
- API/commentary payload semantics
- frontend commentary consumption
