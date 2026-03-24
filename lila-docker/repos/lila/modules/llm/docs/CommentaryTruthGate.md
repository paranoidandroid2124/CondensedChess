# Commentary Truth Gate

This document defines the canonical chess-truth signoff gate for Chesstory.

It complements
[`CommentaryPipelineSSOT.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryPipelineSSOT.md)
by specifying how decisive-move truth is judged after the runtime pipeline
produces its surfaces.

## Core Rule

Truth comes before prose richness.

If Chesstory cannot verify a decisive claim, a concrete benchmark, or a
compensation story, it must become less specific instead of inventing a more
vivid explanation.

## Canonical Signoff Order

1. Derive a `MoveTruthFrame` for every candidate key moment.
2. The frame must classify seven fact families before prose exists:
   - move quality
   - benchmark truth
   - tactical truth
   - material economics
   - strategic ownership
   - punishment / conversion
   - difficulty / novelty
3. Run the truth stack in three stages:
   - baseline truth pass
   - selective deep verification for truth-critical moves
   - chain synthesis for commitment / maintenance / conversion ownership
4. Project `DecisiveTruthContract` from the finalized `MoveTruthFrame`.
5. Resolve `TruthOwnershipRole` so commitment, maintenance, conversion, and
   blunder truth do not share the same decisive explanation.
6. Resolve `TruthVisibilityRole` so real exemplars can stay visible even when
   prose permission is too weak to let them overclaim.
7. Resolve `TruthSurfaceMode` so Chronicle / Bookmaker / Active know whether a
   row may explain investment, preserve it, explain conversion, explain
   failure, or stay neutral.
8. Resolve `TruthExemplarRole` so visible exemplar preservation is measured
   separately from ownership and separately from compensation-positive prose.
9. Promote decisive moments using truth and exemplar preservation, not only
   salience.
10. Realize Chronicle / Bookmaker / Active from that truth-bound state.
11. Allow optional wording polish only after truth-bound prose exists.
12. Judge release readiness with both automatic health checks and GM-style
   manual audit.

Automatic green checks alone do not imply chess-truth signoff.

## Primary Truth Metrics

The first-class automatic metrics are family-wise truth metrics, not prose-only
health metrics:

- verified-best move mislabeled count
- wrong benchmark naming count
- blunder softening count
- fake compensation count
- commitment missed count
- maintenance-as-commitment count
- conversion-as-compensation count
- visible exemplar lost count

The older release/parity metrics remain required, but they are secondary.

## Master-140 GM Audit

The required truth gate is the master-only 140-game corpus under
`tmp/commentary-player-qc/manifests/master_only_signoff_140_20260324`.

This corpus is the mandatory manual audit gate for decisive-move truth.

Its merged signoff must preserve the dedicated positive-exemplar audit set even
when some exemplar games live outside the main 140-game corpus. Exemplar
coverage is judged against the audited exemplar set, not only against the
intersection of exemplar keys with the current shard/game list, and not only
against compensation-positive prose permission.

Verified `WinningInvestment` / `CompensatedInvestment` moments stay eligible as
positive compensation exemplars during audit calibration when Game Arc and
Bookmaker agree on the same compensation contract. Legacy suppression rules may
still remove fake compensation, but they must not erase truth-bound investment
exemplars.

## Manual Outcome Labels

- `hard_fail`
  - decisive explanation is materially false
  - named benchmark is wrong
  - a blunder is softened into compensation or technical-neutral language
  - a verified-best played move is called inaccurate, loose, or blunder-like
- `partial_fail`
  - broad direction is roughly correct, but the decisive reason, punishment, or
    benchmark is underexplained or partly distorted
- `quiet_pass`
  - explanation is truthful but compact; no major decisive falsehood
- `strong_pass`
  - explanation identifies the real decisive move, why it matters, and the
    actual punishment / conversion route in player language

## Zero-Tolerance Catastrophic Families

The following are automatic truth-gate blockers:

- played move is verified best, but a surface calls it wrong / loose /
  inaccurate / blunder-like
- a surface names a benchmark move that is not the verified best move
- internal blunder truth is softened into compensation-positive Bookmaker or
  Active framing
- whole-game decisive shift / punishment story contradicts the decisive-truth
  contract
- a verified or provisional positive investment exemplar is missing from the
  focus-moment exemplar set
- a maintenance move is surfaced as if it were the original investment
- a conversion move is surfaced as compensation rather than conversion
- compensation-positive prose appears on a move that does not own investment
  truth

## Health Signals vs Truth Gate

The following remain useful, but they are secondary:

- `crossSurfaceAgreementRate`
- `releaseGatePassed`
- parity metrics
- provider-none rerun stability

These are runtime health signals, not the final chess-truth gate.

For the same reason, `pathVsPayoffDivergenceCount` is only meaningful for
unresolved divergence. If display normalization has already resolved a path vs
payoff split by selecting a canonical subtype, that resolved split is not a
truth-gate failure and must not count as remaining divergence.

## Required Runtime Behavior

- Chronicle, Bookmaker, and Active must all consume the same decisive-truth
  contract for a given key moment.
- Ownership, visibility, and prose permission are separate:
  - a move may be `supporting_visible` without owning the decisive
    explanation
  - a move may remain visible while `TruthSurfaceMode` is still `neutral`
  - visibility is not permission to emit compensation-positive prose
  - visibility is not itself ownership; `TruthExemplarRole` may be
    `provisional_exemplar` while ownership remains `none`
- serialized/debug truth fields are audit aids only. Signoff-path runtime logic
  must not reconstruct ownership or surface permission from those strings when
  canonical `MoveTruthFrame` / `DecisiveTruthContract` data is available.
- Investment families must consume the same truth-phase ownership:
  - `first_investment_commitment`
  - `compensation_maintenance`
  - `conversion_followthrough`
- Bookmaker and Active may use compensation-positive language only when the
  contract allows compensation framing.
- fake compensation suppression and real investment exemplar preservation are
  both required. Passing one while failing the other is not truth-clean.
- only the truth-owning commitment move may explain why the investment works;
  maintenance preserves it and conversion cashes it out.
- current-move evidence and inherited shell are distinct:
  - `investedMaterial` or durable-pressure residue may keep a row visible
  - they may not, by themselves, certify a fresh commitment move
- Concrete benchmark naming is allowed only when the verified best move exists
  and the contract explicitly allows it.
- Whole-game Chronicle binders must anchor decisive shift / punishment prose in
  the same decisive-truth contract, or omit those sentences.
