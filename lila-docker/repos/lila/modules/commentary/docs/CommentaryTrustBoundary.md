# Commentary Trust Boundary

This file is the canonical trust-risk map for Chesstory commentary. Use it for
false positives, overclaim control, support-only reinflation, fallback rewrites,
surface-consumption risk, and lesson-readiness gating.

It complements `CommentaryPipelineSSOT.md` and `CommentaryTruthBoundary.md`.

## Current State

The live product trust boundary is MoveReview-only. Retained Chronicle, Active,
Game Arc, and whole-game names are internal diagnostics or shared
infrastructure unless a current MoveReview consumer is named explicitly.

Current operating posture:

- maintain existing exact-board promoted slices
- keep broad strategic expansion closed
- allow new runtime authority only through proof contracts and the
  claim-authority kernel
- keep support-only/deferred/latent carriers internal
- keep Track 5 lesson authority deferred

## Authority Ladder

Internal trust decisions use this ladder:

| tier | user-facing meaning | allowed use |
| --- | --- | --- |
| `CertifiedOwner` | exact proof may own the claim | main MoveReview explanation |
| `SupportedLocal` | bounded local evidence may speak | qualified local reading/support |
| `DiagnosticOnly` | useful for review, not release | traces, corpus review, tests |
| `Suppressed` | blocked | no released claim |

`ClaimAuthorityResolver` owns final authority decisions from packet, plan, and
truth contract. `PlannerClaimAdmission` is the planner adapter.
`ClaimAuthorityPolicy` is only a compatibility facade.

The planner may rank and select questions, but it must not decide proof-family,
source, scope, tactical-veto, or fallback authority by itself.

## Proof And Packet Boundary

`PlayerFacingClaimPacket` is the runtime carrier for exact claim candidates.
`ProofContractRules` decides which proof families can be certified or supported
locally.

Current strict rules:

- `PositionLocal` scope alone never admits `WhatMattersHere`.
- exact owner slices require certified source/family predicates.
- support material never enters the owner pool directly.
- tactical truth veto outranks strategic authority.
- line-scoped claims may survive only as subordinate evidence unless a main
  path strategic claim is independently admitted.
- support-only carriers may not re-inflate after certification failed closed.

Color-complex squeeze is explicitly authority-closed:

- proof family: `color_complex_squeeze`
- status: `Deferred`
- certified eligible: false
- supported-local eligible: false
- default failure: `color_complex_authority_closed`

Readiness scans and exact-FEN review artifacts are local evidence for future
authority review, not runtime admission.

## Render Trust Boundary

`FragmentAuthority` owns render-release tags:

- render-only text may release
- support-only text may release only when sufficiently grounded
- unsafe truth text is dropped
- unsafe lesson text is dropped
- future-lesson candidates are dropped
- move-linked-anchor fragments require their anchor

`NarrativeOutlineBuilder` assembles beats; it does not own release legality.
`NarrativeOutlineValidator` remains the final scrubber.

## API And Frontend Trust Boundary

`CommentaryApi` must serialize typed surviving payloads without recomputing
authority.

Frontend code must not rebuild strategic meaning from:

- `topEngineMove`
- `cpLossVsChosen`
- latent/deferred fields
- support-only carriers
- raw `strategyPack`
- omitted decision-comparison data
- free-form helper prose

MoveReview and narrative views render only typed payload fields that survived
backend authority.

## Active Risk Map

| risk | current control |
| --- | --- |
| support-only becomes owner | claim-authority kernel, proof contracts, planner adapter |
| fallback truth rewrite | truth contract first; no-contract fallback is failure-only |
| broad strategic overclaim | exact packet/certified slice required |
| renderer leaks unsafe prose | `FragmentAuthority` plus validator scrub |
| frontend rebuilds omitted meaning | typed payload only; no fallback reconstruction |
| color-complex premature release | deferred contract and authority-closed failure |
| lesson overgeneralization | Track 5 deferred; scoped takeaway only |

## CTH Priority Summary

Detailed historical B-frontier logs are no longer canonical in this file. The
current conclusion is:

- B1/B2/B3 exact slices remain maintained.
- B4/B5/B6 remain narrow bounded-scope results only.
- B7/B8 and broad color-complex, heavy-piece, mobility-cage, or global squeeze
  expansion remain design/recon territory.
- New authority must start from exact board positions, exact witness extraction,
  best-defense evidence where relevant, and proof-contract promotion.

Underlying evidence lives in local generated artifacts and targeted test/tool
reports. Those artifacts are evidence, not authority predicates.

## Track 5 Defer Rationale

Broad lesson authority remains closed because current local proof can validate
only a reviewed move, FEN, branch, and evidence tier. It cannot safely state a
general chess lesson without additional corpus coverage, exception handling,
and user-facing scope wording.

Allowed today:

- exact factual fallback
- bounded `SupportedLocal` phrasing
- `MoveReviewScopedTakeaway` tied to the reviewed move and branch

Not allowed today:

- broad rules such as "always" or "in every position"
- shared-lesson helper labels
- whole-position strategic truth from local support rows
- lesson claims from color-complex readiness rows

## Maintenance Triggers

Update this file in the same change when trust-relevant behavior changes in:

- fallback truth projection or rewrite behavior
- cross-surface contract consumption
- support-only carrier exposure
- proof-contract eligibility
- claim-authority resolution
- planner admission
- renderer release tags
- lexicon/template authority boundaries
- frontend support rendering
- scoped takeaway or lesson-readiness guards

Report future cleanup as either `boundary cleanup only` or
`boundary cleanup + verified compile/test`; do not claim product quality gains
from mixed or unverified diffs.
