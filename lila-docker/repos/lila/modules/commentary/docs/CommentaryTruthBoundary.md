# Commentary Truth Boundary

This document states what `DecisiveTruthContract` can and cannot prove for
MoveReview commentary.

## What Truth Contract Can Prove

`DecisiveTruthContract` may prove:

- move quality class, such as best, acceptable, inaccuracy, mistake, blunder,
  missed win, or investment class;
- reason family, such as tactical refutation, only-move defense, investment
  sacrifice, quiet technical move, or conversion;
- failure mode, such as tactical refutation, only-move failure, quiet
  positional collapse, speculative investment failed, or no clear plan;
- verified best move and evaluation risk;
- whether broad strategic support should be blocked.

## What Truth Contract Cannot Prove Alone

Truth contract alone cannot prove:

- a concrete tactical motif;
- a forced reply count or forced reply role;
- a line consequence;
- a role-aware alternative comparison;
- a timing window;
- a strategic plan or concession;
- renderer wording beyond safe risk/factual boundaries.

`blocksStrategicSupport` is a truth/risk gate, not a positive explanation
authority.

## Positive Authority Requirements

Positive causal prose requires a typed producer packet:

- tactical prose: current-move tactical ownership or motif-backed threat;
- defensive prose: defensive witness, urgent threat, or only-move proof;
- played-move only-move timing: only when the truth contract also says the
  reviewed move matches the chosen best move;
- line consequence prose: FEN/replay-validated `LineConsequenceEvidence`;
- alternative prose: branch-scoped role comparison; for a missed benchmark
  only-move, this means verified best/benchmark branch versus played branch, not
  played-move timing authority;
- timing prose: timing witness or admissible timing contrast;
- plan/pressure prose: certified local packet that passes claim authority gates.

If the typed packet is absent, the truth contract may still close unsafe
surfaces, but it must not fill the missing chess reason.

## Scene Summary

Scene summaries are post-classification diagnostics. They are not truth
contracts. A `concrete_tactical` scene summarizes admitted concrete tactical
authority; a `line_consequence` scene summarizes line authority; an
`alternative_comparison` scene summarizes branch-comparison authority.

No broad tactical scene is part of the current truth boundary.

## Fallback Truth

Exact factual fallback may state:

- played move,
- destination square,
- capture/check basics when exact,
- short checked-line prefix,
- absence of admitted causal authority.

It may not state a cause, threat, plan, timing, or alternative unless the
corresponding typed evidence was admitted.
