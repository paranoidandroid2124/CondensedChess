# Commentary Pipeline SSoT

This document is the current runtime map for Chesstory commentary. It is a
boundary map, not a preservation order. Live code, typed models, call sites, and
runtime replay behavior remain the final authority.

## Runtime Path

MoveReview commentary flows through one path:

1. `NarrativeContext` and `MoveReviewRefs` carry board, move, PV, and replay
   context.
2. `DecisiveTruthContract` classifies evaluation risk and truth boundaries.
3. producers build typed evidence packets: tactical motif, defense, line
   consequence, timing, alternative comparison, plan support, pressure, opening,
   endgame, or factual fallback.
4. `QuestionPlannerInputsBuilder` gathers those packets without turning prose
   into authority.
5. `QuestionFirstCommentaryPlanner` selects a question and planner owner from
   typed evidence.
6. `MoveReviewCausalClaim` builds the CausalFrame and typed evidence list.
7. `MoveReviewLocalFact` admits a local fact family and authority.
8. `MoveReviewCompressionPolicy` renders only the surface allowed by the
   CausalFrame/local fact contract.
9. API/front-end payloads expose the selected surface and diagnostics.

There is no parallel MoveReview explanation path. Exact factual fallback is the
fail-closed renderer lane when no typed causal authority is admitted.

## Authority Model

The pipeline separates five concepts.

`RiskGate`

- Examples: blunder, missed win, `blocksStrategicSupport`, severe
  counterfactual, bad tactical refutation.
- Role: suppress or demote unsafe positive explanation.
- Non-role: it cannot create tactical, line, timing, alternative, or defense
  authority by itself.

`CausalMechanism`

- Examples: concrete tactical motif, defensive necessity, line consequence,
  alternative role difference, timing window, compensation failure, strategic
  concession, factual fallback.
- Role: says what kind of chess reason the evidence supports.

`EvidenceAuthority`

- Examples: motif detector, PV-coupled line consequence, role-aware decision
  comparison, forced-defense witness, `only_move_defense`, timing witness,
  supported local packet, exact factual replay.
- Role: says where the claim permission came from.

`SurfacePermission`

- Examples: may say tactical, forced, only move, timing, alternative, checked
  line, what-if-not, or factual-only.
- Role: limits renderer language.

`SceneSummary`

- Examples: `concrete_tactical`, `forcing_defense`, `line_consequence`,
  `alternative_comparison`, `plan_clash`, `opening_relation`,
  `endgame_transition`, `transition_conversion`, `quiet_improvement`.
- Role: summarize selected mechanism for diagnostics.
- Non-role: scene summary must not be the original source of sentence
  authority.

## Planner Owners

The planner owner families are mechanism labels, not prose templates:

- `ConcreteTactical`: current-move-owned tactical proof or motif-backed threat.
- `ForcingDefense`: a typed defensive need, urgent threat, only-move window, or
  allowed prevented resource.
- `LineConsequence`: a played-move-owned PV/replay consequence.
- `AlternativeComparison`: a branch-scoped comparison between played move and
  alternative/best move.
- `DecisionTiming`: typed timing witness or admissible timing contrast.
- `PlanRace`: certified own plan, opponent pressure, and timing anchor.
- `MoveDelta`: move-owned state or plan-support change.
- `PositionProbe`, `OpeningRelation`, `EndgameTransition`: domain-specific
  owners with their own support gates.

`truth_contract` alone is not a planner owner. It may close unsafe surfaces, but
it may not invent a causal mechanism.
Only a certified only-move defensive proof may project the positive
`only_move_defense` source, and it may own played-move timing only when the
reviewed move matches the chosen best move. A missed benchmark move remains an
alternative/comparison fact, not a played-move `WhyNow` owner.

`pv_coupled_plan_support` can support a move-delta plan claim only when the
truth/risk gate does not block strategic support. Under a blocking truth gate,
it must not render positive plan-viability prose.

## Line Consequence

Line consequence authority requires a typed `LineConsequenceEvidence` packet
whose line is FEN/replay validated and owned by the reviewed move or by an
admissible branch comparison. Current consequence kinds include exchange,
forcing-check, central-break/central-pawn, material transition, passed-pawn
creation, and promotion-race evidence.

For a missed benchmark `OnlyMoveDefense`, the admissible branch comparison is
the verified best/benchmark move against the played move. Both branches need
role-scoped line evidence, and a concrete played branch needs a meaningful
checked evaluation gap; otherwise the row remains fallback or support-only. This
does not create played-move `WhyNow` or `ForcingDefense` ownership.

When admitted, the owner is `LineConsequence` or `AlternativeComparison`, and
the local fact family remains `line_consequence`. It is not tactical motif
authority, forced-reply authority, or timing authority.

## Concrete Tactics

Concrete tactical authority requires current-move ownership from a tactical
claim or a motif-backed threat/counterfactual packet. Broad tactical wording,
sacrifice wording, tactical truth mode, or `blocksStrategicSupport` is not
enough.

When `ConcreteTactical` owns the surface, the rendered claim itself must carry
a concrete causal anchor such as a checked line, move, threat, tactic, reply,
or material consequence. A generic tactical label plus a cited line is not
enough for direct planner prose.

## Rendering

Renderers consume CausalFrame/local fact contracts. They do not combine raw
planner labels into new chess meaning. If a required typed relation is absent,
MoveReview falls back to exact factual or bounded support-only wording.

## Diagnostics

Replay and QC outputs should expose mechanism-specific fields such as
`plannerConcreteTacticalSources`, `plannerLineConsequenceSources`, and
`plannerAlternativeComparisonSources`. Diagnostics should stay mechanism-specific
and must not collapse these families into a broad tactical bucket.
