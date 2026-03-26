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
4. Canonical key-moment anchoring must rescue engine-severe CP / mate swings
   even when WPA saturation alone would miss them; if such a move survives only
   as a candidate bridge, canonical internal admission must still rescue it
   from runtime truth instead of leaving it witness-only, and decisive
   `blunder` / `missed_win` labels beat softer same-ply labels.
5. Project `DecisiveTruthContract` from the finalized `MoveTruthFrame`.
6. Resolve `TruthOwnershipRole` so commitment, maintenance, conversion, and
   blunder truth do not share the same decisive explanation.
7. Resolve `TruthVisibilityRole` so real exemplars can stay visible even when
   prose permission is too weak to let them overclaim.
8. Resolve `TruthSurfaceMode` so Chronicle / Bookmaker / Active know whether a
   row may explain investment, preserve it, explain conversion, explain
   failure, or stay neutral.
9. Resolve `TruthExemplarRole` so visible exemplar preservation is measured
   separately from ownership and separately from compensation-positive prose.
10. Promote decisive moments using truth and exemplar preservation, not only
   salience.
11. Realize Chronicle / Bookmaker / Active from that truth-bound state.
12. Allow optional wording polish only after truth-bound prose exists.
13. Judge release readiness with both automatic health checks and GM-style
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

Its merged signoff must preserve the canonical positive-exemplar gate carried in
the truth inventory even when some exemplar games live outside the main
140-game corpus. Exemplar coverage is judged against that audited gate, not
only against the intersection of exemplar keys with the current shard/game
list, and not only against compensation-positive prose permission.

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
- a low-confidence blunder is narrated as if the player had a verified
  strategic intent

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
  - a truth-bound `compensation_maintenance` move stays `non_exemplar` in the
    public/runtime contract; a private maintenance-exemplar candidate may keep
    it focus-visible without becoming the commitment owner
  - public/runtime `maintenance_echo + supporting_visible +
    maintenance_preserve` is reserved for `critical maintenance`: verified
    payoff anchor, verified investment payoff, direct current semantic/carrier
    anchor match, and pressure evidence (`only_move`, `unique_good_move`,
    forcing proof, bad followthrough, or accepted current-semantic match)
  - routine maintenance may keep the internal truth phase while the public
    projection is forced back to `none / hidden / neutral`
  - a proof-backed best `only_move_defense` may still project
    `none / primary_visible / neutral`; this is a visible critical hold, not a
    failure owner and not a compensation owner
  - a benchmark-critical quiet hold without proof stays `hidden / neutral`
- fresh commitment seeding happens before any explicit `investedMaterial`
  carrier is required:
  - current-move material loss plus a move-local payoff anchor may produce a
    `provisional_exemplar`
  - that path may classify as `first_investment_commitment` even when the raw
    carrier has no `investedMaterial`
  - ownership remains stricter than visibility
- serialized/debug truth fields are audit aids only. Signoff-path runtime logic
  must not reconstruct ownership or surface permission from those strings when
  canonical `MoveTruthFrame` / `DecisiveTruthContract` data is available.
- truth-first runtime semantics are mandatory when a contract exists:
  - raw `momentType`, `moveClassification`, `criticality`, and `choiceType`
    may remain in payloads for compatibility, display, or fallback only
  - raw `StrategyPackSurface` compensation flags may help extract carriers or
    support no-contract fallback, but they may not recreate compensation
    significance once the contract is present
  - whole-game binders, selector scoring, dossiers, and prose policies must use
    canonical truth semantics first and treat raw strings as fallback only
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
- provenance rules are explicit:
  - `current_material` may originate a fresh seed
  - `current_semantic` may verify and upgrade that seed into ownership
  - `current_semantic` also covers concrete current-move payoff-route
    carriers that keep a maintenance move focus-visible only when they directly
    match the verified payoff anchor; generic route/target scaffolding does not
    qualify
  - `after_semantic` may support maintenance or conversion continuity
  - `legacy_shell` may preserve visibility only
  - `after_semantic` and `legacy_shell` may not create fresh commitment
    ownership
- bad-move intent is separately gated:
  - `failure_intent` is internal and evidence-backed
  - move-local route / purpose / target evidence must directly match the
    verified payoff anchor before intent is allowed; bare square-access
    scaffolding is not enough
  - accepted current semantic support may keep intent alive only when it also
    directly matches the verified payoff anchor
  - `speculative_investment_failed` requires fresh current-move matched
    investment evidence; inherited maintenance shells alone may not trigger it
  - low-confidence or `no_clear_plan` failures may not keep route / target /
    plan carriers alive in sanitized commentary surfaces
  - commentary may describe tactical collapse or speculative failure, but it
    may not attribute a concrete player plan unless that plan survived the
    intent-confidence gate
- tactical tension is also canonicalized:
  - forced / critical / tactical-pressure checks must come from one shared
    truth-first policy
  - when the contract exists, `reasonFamily`, `failureMode`, benchmark
    pressure, and intent-confidence gating outrank raw `criticality` /
    `choiceType` strings
  - raw tension strings may remain fallback inputs only when no decisive-truth
    contract is available
- selector/runtime significance is also canonicalized:
  - threaded representative selection must be truth-aware; a hidden/neutral
    nominal representative must yield to a truth-visible or
    significance-qualified move from the same thread when one exists
  - representative replacement stays stage-aware; local tactical / only-move
    failures may replace hidden thread shells without becoming global protected
    families
  - hidden `benchmarkCriticalMove` rows are diagnostic/thread-local only; they
    do not earn global visible-slot or active-note protection on their own, and
    quiet benchmark-critical holds may only replace thread shells when the
    thread lacks stronger truth-visible/failure representatives
  - representative and active-note ordering remain fixed-cap and truth-first:
    severe failures first, then promoted best holds, then
    verified/provisional exemplar ownership
  - the protected pass reserves visible / active-note space first, but the
    selector must still backfill remaining visible slots with truth-eligible
    fallback moments instead of collapsing the visible set to the protected
    family only
  - generic hidden best tactical/technical moves may help same-thread
    replacement, but they may not outrank those protected families in the
    global visible / active-note caps
  - investment-chain dedupe keeps one supporting-visible move per chain by
    default, but a second support is allowed for a private
    maintenance-exemplar candidate or a non-best / failure-significant follow-up
- Concrete benchmark naming is allowed only when the verified best move exists
  and the contract explicitly allows it.
- Whole-game Chronicle binders must anchor decisive shift / punishment prose in
  the same decisive-truth contract, or omit those sentences.
