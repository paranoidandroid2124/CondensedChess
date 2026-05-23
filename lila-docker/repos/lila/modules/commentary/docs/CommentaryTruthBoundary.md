# Commentary Truth Boundary

This file defines the chess-truth signoff boundary for Chesstory commentary.
It complements `CommentaryPipelineSSOT.md`, which maps the runtime pipeline.

## Core Rule

Truth comes before prose richness. If Chesstory cannot verify a decisive claim,
benchmark, compensation story, strategic plan, or lesson-like implication, the
surface must become less specific or fail closed.

## Signoff Surface

MoveReview is the only user-facing commentary truth surface. Retained
Chronicle, Active, or Game Arc code paths are internal diagnostics or shared
infrastructure unless a current MoveReview consumer is named explicitly.

Future `SupportedLocal` or scoped-takeaway expansion must enter through
MoveReview first and must preserve the audited payload contract.

## Truth Stack

The runtime must resolve truth in this order:

1. Build `MoveTruthFrame` before prose exists.
2. Classify move quality, benchmark truth, tactics, material economics,
   strategic ownership, punishment/conversion, and difficulty/novelty.
3. Run selective deep verification for truth-critical moves.
4. Synthesize commitment, maintenance, and conversion chains.
5. Project `DecisiveTruthContract`.
6. Resolve ownership, visibility, surface mode, and exemplar role.
7. Admit claims only through the claim-authority kernel and proof contracts.
8. Render and validate prose after truth and authority are fixed.

Serialized/debug fields may aid audit, but they may not recreate ownership when
canonical truth data is available.

## Authority And Truth Interaction

`CertifiedOwner` and `SupportedLocal` are authority decisions, not chess-truth
shortcuts. They are legal only when the current board truth, proof contract,
and tactical veto checks agree.

`DiagnosticOnly`, `Suppressed`, support-only, deferred, latent, and fallback
carriers may not emit owner claims. They may remain in internal reports or
debug traces.

Strategic plan refutation must be exact-board/probe truth. Sibling plan ranking
or alternative-dominance metadata can demote selection, but it is not a chess
refutation and must not be reported as `Refuted`.

Strategic plan promotion and demotion must be consumed through the typed
`StrategicPlanEvidenceView` derived from `PlanEvidenceEvaluator`. Raw
`StrategicPlanExperiment.evidenceTier` strings, compatibility markers, and
subplan ids cannot by themselves certify truth, refutation, provenance,
quantifier, stability, or exact-family ownership.

Exact-family strategic truth remains witness-bound. A plan taxonomy label such
as `central_break_timing` does not certify the exact central-break proof family
unless the exact board witness is present. A product surface row is allowed
only after that exact witness and a `SupportedLocal` packet both survive the
runtime boundary; this does not make the family `CertifiedOwner`. For this
SupportedLocal row, equal or near-equal PV scores and missing two-move branch
keys do not by themselves refute the witness; the required truth is the legal
non-capturing same-file d/e pawn advance to `d4`, `e4`, `d5`, or `e5`, plus a
board-backed link from the played move. The played move itself can be that
exact witness when it legally makes the same central break; top-PV omission is
not by itself a truth failure for this bounded support row. Diagonal captures
and prep/challenge pawn moves are not truth for the product-visible `Central
break` row.

Break/file-axis truth must come from structured square or file-marker evidence
parsed by the shared runtime boundary, not from incidental letters inside
English prose or underscore-separated plan ids. Heavy-piece release truth must
come from exact branch replay: queen centralization, one check, an unrecaptured
rook capture, or a back-rank rook shuffle is not enough to refute a local bind.

Color-complex squeeze authority is currently closed. `color_complex_squeeze`
has an explicit deferred proof contract and default failure
`color_complex_authority_closed`; corpus/readiness rows are evidence for future
review only.

## No-Go Truth Failures

These are release blockers:

- verified-best played move is called wrong, loose, inaccurate, or blunder-like
- benchmark move is named when it is not verified best
- blunder truth is softened into compensation-positive framing
- compensation-positive prose appears without investment ownership
- maintenance is narrated as the original investment
- conversion is narrated as compensation
- support-only or deferred carriers revive owner claims after certification
  failed closed
- fallback projection reconstructs investment, maintenance, conversion, or
  strategic plan truth from serialized strings
- frontend fallback retry/drop behavior is inferred from commentary prose
  rather than backend `diagnostics.status`
- broad lesson or rule wording appears without local scoped-takeaway authority
- decision comparison is rebuilt from `topEngineMove`, `cpLossVsChosen`, or
  latent/deferred carriers
- strategic plan refutation is inferred from sibling score/ranking rather than
  exact board/probe evidence
- strategic plan truth is inferred from raw `evidenceTier`, compatibility
  markers, or subplan ids instead of typed evaluator evidence plus exact
  witness where required
- local bind truth is rejected from broad heavy-piece movement labels without
  exact replay evidence of a real release
- MoveReview strategic support, probe, authoring, or decision-comparison UI is
  rebuilt from raw `strategyPack`, `signalDigest`, `authorEvidence`,
  `probeRequests`, full `mainStrategicPlans`, or strategic-plan experiment
  metadata instead of the backend-certified `moveReviewPlayerSurface`
- strategic intent is invented for a low-confidence or tactical-first failure

## Scoped Takeaway Boundary

`MoveReviewScopedTakeaway` is local instruction only. It must be tied to:

- reviewed UCI
- post-move FEN
- same coupled PV branch
- evidence tier
- guardrail tags

It may project into the existing MoveReview learning-point compatibility field,
but it is not Track 5 lesson authority and must not generalize into broad rules.

## Audit Gate

Automatic checks are necessary but not sufficient. Truth signoff requires:

- targeted compile/tests for touched runtime paths
- family-wise truth metrics
- GM-style manual review on the master audit corpus when release signoff is in
  scope
- explicit separation of target slice, aggregate behavior, runtime behavior,
  and test/tooling artifacts

Primary truth metrics:

- verified-best mislabeled count
- wrong benchmark naming count
- blunder softening count
- fake compensation count
- missed commitment count
- maintenance-as-commitment count
- conversion-as-compensation count
- visible exemplar lost count

Health signals such as parity, release-gate flags, provider-none stability, and
cross-surface agreement remain secondary to chess truth.

## Maintenance Triggers

Update this file when any change affects:

- truth/signoff behavior
- fallback truth projection
- claim authority signoff
- benchmark naming
- compensation/investment/conversion ownership
- scoped takeaway or lesson-readiness guards
- frontend/API behavior that could alter user-facing truth
