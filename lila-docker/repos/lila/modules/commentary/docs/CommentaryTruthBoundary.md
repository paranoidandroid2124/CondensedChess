# Commentary Truth Boundary

This file defines the chess-truth signoff boundary for Chesstory commentary.
It complements `CommentaryPipelineSSOT.md`, which maps the runtime pipeline.

## Core Rule

Truth comes before prose richness. If Chesstory cannot verify a decisive claim,
benchmark, compensation story, strategic plan, or lesson-like implication, the
surface must become less specific or fail closed.

## Signoff Surface

MoveReview is the only user-facing commentary truth surface. Chronicle,
Active, and Game Arc are legacy diagnostic/tooling surfaces. Their bridge,
thread, note-composition, and compression helpers are isolated to
`modules/commentaryTools/src/test` and may not participate in released truth
signoff unless a current MoveReview consumer and runtime audit explicitly
reopen that boundary.
Runtime truth projection must not accept `GameChronicleMoment` or
`GameChronicleResponse` as signoff inputs. Chronicle fallback semantics may
exist only in test/tooling diagnostics; released truth paths consume the current
MoveReview/arc runtime models and proof-contract projections.
Active-note payload fields and active branch dossiers are not truth inputs in
runtime `GameArc`; tooling that reconstructs them must do so outside `src/main`.

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

`ReplayBackedInternal` line-consequence evidence is not a truth-owner
promotion. It means the PV was legally replayed for bounded narrative or ledger
use, but it cannot emit player decision-strip claims unless the line also comes
from `MoveReviewRefs` as a `SurfaceCandidate`. A replay-observed central pawn
advance without `CentralBreakTimingWitness.exact` is only a local line
transition, not the `central_break_timing` proof family.

Strategic plan refutation must be exact-board/probe truth. Sibling plan ranking
or alternative-dominance metadata can demote selection, but it is not a chess
refutation and must not be reported as `Refuted`.

Strategic plan promotion and demotion must be consumed through the typed
`StrategicPlanEvidenceView` derived from `PlanEvidenceEvaluator`. Raw
`StrategicPlanExperiment.evidenceTier` strings, compatibility markers, and
subplan ids cannot by themselves certify truth, refutation, provenance,
quantifier, stability, or exact-family ownership.
Board-bound probe truth requires the returned probe certificate to echo the
requested FEN and probed move; missing values are not truth. For multi-move
requests, the certified move must be one of the requested moves. Unknown
request purposes have no truth contract and cannot be rescued by explicit
signal names or by the result's own purpose. Unknown-purpose probes also cannot
be treated as refutations through the default cp-loss bound. Refutation probes
are negative evidence only: if they do not refute a plan, they clear that
specific punishment path but do not certify the plan as true.

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
Central-break runtime truth is generic board/PV truth only. Historical source
rows may remain in test/tooling precedent catalogs, but runtime witness
construction must not stamp source-specific FEN markers into `sourceTags`,
owner seeds, or release terms.

Exact-slice signoff must consume the typed `PlayerFacingExactSliceProof`
created by the board/probe witness branch. Owner, anchor, structure,
continuation, and prose terms may explain or diagnose the claim, but they are
not truth objects and must not be parsed back into exact proof.
For prophylactic restraint, exact-slice truth is limited to a square/route or
typed denied-resource token derived from runtime resource classification.
English plan labels and generic counterplay phrases do not certify the exact
resource.
Carlsbad fixed-target truth is mirror-slice based: White-side pressure targets
Black `c6` only when the board has the `c6`/`d5` enemy chain and `b2`/`d4`
friendly support; Black-side pressure targets White `c3` only when the mirrored
`c3`/`d4` enemy chain and `b7`/`d5` friendly support are present. Both sides
still require minority-attack semantic consequence and typed
`CarlsbadFixedTarget` proof.

Break/file-axis truth must come from structured square or file-marker evidence
parsed by the shared runtime boundary, not from incidental letters inside
English prose or underscore-separated plan ids. Heavy-piece release truth must
come from exact branch replay: queen centralization, one check, an unrecaptured
rook capture, or a back-rank rook shuffle is not enough to refute a local bind.
Forced-line trap truth may not release a named trap from a legal move string
alone. A named opening trap requires the reviewed move to be the trap entry and
a coupled PV/variation line to confirm the remaining sequence before the line is
replayed. Non-mating trap lines must also show a material-balance gain for the
side that makes the final material-winning move in the verified line.

Greek Gift truth must be board-backed by the reviewed move: the move must
actually capture on `h7`/`h2` with a bishop, the captured square must have held
the opponent pawn before the move, the resulting position must be check, and
kingside support geometry must be visible. A queen, rook, or knight check on the
same square is not Greek Gift truth.

Color-complex squeeze truth is exact-board only. The live proof source is
`color_complex_squeeze_probe`, and release requires a parsed FEN, an
opponent-owned semantic weak square, matching color-complex/hole/fianchetto
evidence, and a friendly bishop or knight that actually attacks that square on
the board. Coordinate and minor-piece words in packet terms are trace labels
only; they cannot certify truth. Final release still requires proven
same-branch state and stable persistence.

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
- line/PV consequence wording is surfaced from engine-only `VariationLine`
  interpretation or failed ref replay instead of a `MoveReviewRefs` line that
  passed legal FEN replay
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
