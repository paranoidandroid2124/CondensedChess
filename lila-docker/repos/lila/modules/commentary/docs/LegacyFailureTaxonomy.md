# Legacy Failure Taxonomy

This document records the failure classes repeatedly observed on `master` and
`codex/strategic-object-demolition`.

Its purpose is to stop the new `commentary` backend from rediscovering the same
mistakes through new helper layers, new wording salvage, or duplicated
admission gates.

Primary source references:

- `master:lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md`
- `master:lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md`
- `codex/strategic-object-demolition:lila-docker/repos/lila/modules/llm/docs/StrategicObjectModel.md`
- `codex/strategic-object-demolition:lila-docker/repos/lila/modules/llm/docs/StrategicObjectRoadmap.md`

## Core Observation

The old system did not fail only because some claims were wrong.

It failed because the same claim-quality problem was repeatedly addressed in
multiple layers at once:

- builder or analyzer heuristics
- validator or omit layers
- certification or support-only wrappers
- planner admission
- renderer or replay fallback

That duplication made `false positive`, `underfire`, `support-only`, and
`deferred` difficult to diagnose and easy to reintroduce.

## Canonical Failure Owners

The new branch freezes the owner of each failure class.

| Failure class | Canonical owner | Forbidden duplicate owners |
| --- | --- | --- |
| `false_positive` | witness admission or object generation | planner, renderer, replay |
| `underfire` | witness admission or object composition | certification, renderer |
| `support_only` | certification only | object generation, planner wording |
| `deferred` | certification only | planner, renderer, replay |
| `planner_none` | planner only | certification, renderer |
| `surface_reinflation` | replay/surface validation only | object generation, witness rules |

## Repeated Legacy Failure Classes

### 1. Cooperative Defense

Legacy meaning:

- a strategic route looked strong only if the defender cooperated
- best defense killed the claim

Observed repeatedly in trust hardening:

- `cooperative_defense`
- “certification fails unless a concrete best defense is found and the route
  still survives it”

New-system owner:

- engine-backed certification

New-system rule:

- no move-local or comparative primary claim without a concrete best-defense
  survival check on the same branch

### 2. PV Restatement Only

Legacy meaning:

- engine PV was paraphrased as strategy discovery
- no distinct strategic burden existed beyond the line itself

Observed repeatedly in trust hardening:

- `pv_restatement_only`
- `engine_pv_paraphrase`

New-system owner:

- certification

New-system rule:

- a claim must show object anchor, witness bundle, and falsification burden
- pretty PV prose is never a strategic owner by itself

### 3. Hidden Freeing Break

Legacy meaning:

- one apparently suppressed axis still had an untested freeing break or reroute

Observed repeatedly in trust hardening:

- `hidden_freeing_break`
- off-file or off-sector releases surviving while a local shell was narrated as
  total suppression

New-system owner:

- object generation and certification

New-system rule:

- every suppression-style object must enumerate `releaseRisksRemaining`
- certification fails if any live freeing break remains

### 4. Hidden Tactical Release

Legacy meaning:

- exchange sacrifice, perpetual, forcing checks, active-king route, rook lift,
  or similar tactical resource punctured a quiet strategic shell

Observed repeatedly in trust hardening:

- `hidden_tactical_release`

New-system owner:

- engine-backed certification

New-system rule:

- every quiet restriction or bind claim must run a tactical-release inventory
  under best defense

### 5. Local To Global Overreach

Legacy meaning:

- a local file, square, or route edge was narrated as whole-position
  domination or no-counterplay

Observed repeatedly in trust hardening:

- `local_to_global_overreach`

New-system owner:

- object scope and certification scope

New-system rule:

- every object and delta carries local scope explicitly
- planner and renderer may not widen scope

### 6. Waiting Move Disguised As Strategic Bind

Legacy meaning:

- a useful waiting or improving move was narrated as a real opponent-facing
  bind even though no resource loss was proven

Observed repeatedly in trust hardening:

- `waiting_move_disguised_as_bind`
- `waiting_move_disguised_as_plan`

New-system owner:

- witness admission and object generation

New-system rule:

- “nice improvement” is not enough
- the move must prove opponent-facing restriction, target fixation, route loss,
  or break denial

### 7. Surface Reinflation

Legacy meaning:

- an uncertified or downgraded claim came back stronger on Chronicle, Bookmaker,
  Active, wrapper, or whole-game surfaces

Observed repeatedly in trust hardening:

- `surface_reinflation`
- `whole_game_wrapper_leak`
- support-only reuse becoming stronger on replay surfaces

New-system owner:

- replay and surface validation only

New-system rule:

- planner-certified output is the only reusable source
- support-only or deferred material can never revive as stronger replay truth

### 8. Axis Independence Not Proven

Legacy meaning:

- two attractive positional axes were counted as independent strategic burden
  without proving that they were distinct

Observed repeatedly in trust hardening:

- `axis_independence_not_proven`
- `dual_axis_burden_missing`

New-system owner:

- object composition

New-system rule:

- every multi-axis object needs explicit non-redundancy proof
- otherwise it stays a smaller object or support-only bundle

### 9. Route Or Network Mirage

Legacy meaning:

- a set of nice-looking squares or one-file pressure was mistaken for a real
  route network

Observed repeatedly in trust hardening:

- `false_square_network`
- `routeNetworkMirageRisk`

New-system owner:

- witness admission and object generation

New-system rule:

- route claims need ordered edges, continuity, and defender-facing reroute loss
- node beauty alone is not enough

### 10. Phase Or Posture Inflation

Legacy meaning:

- a truth that survived on one narrow posture or phase cell was silently reused
  in slightly-better, heavy-piece, transition, or replay cells

Observed repeatedly in trust hardening:

- `posture_inflation`
- repeated `likely deferred` cells across heavy-piece and transition categories

New-system owner:

- validation charter and rollout gating

New-system rule:

- no family expands across phase/posture cells without its own exact-board
  criticism pack

### 11. Payload Geometry Hardened As Admission

Legacy meaning:

- payload-only geometry or support summaries were silently promoted into
  admission law
- the system started treating a descriptive bundle as if it were a certified
  lower carrier

Observed repeatedly in rewrite review:

- `contested_sectors` treated as if it were a lower witness
- attractive support lists reused as if they were conjunctive admission bundles

New-system owner:

- witness admission and object generation

New-system rule:

- payload geometry stays payload until a canonical helper is frozen
- illustrative support lists are never promoted into required admission law

### 12. Carrier-Only Attack Inflation

Legacy meaning:

- one file, diagonal, rook placement, or attack map was narrated as a whole
  king-attack object
- geometry-only carriers were allowed to self-certify attack ownership

Observed repeatedly in rewrite review:

- `rook on open file` or king-theater-linked file/diagonal access geometry
  restated as attack
- battery or rook-lift language treated as attack proof without king-theater
  linkage

New-system owner:

- object generation and certification boundary

New-system rule:

- attack ownership needs a king-theater-linked carrier plus a distinct
  vulnerability/support fragment on the same defending king
- no carrier fragment alone may become `AttackScaffold` or a certified
  king-safety edge

### 13. Shell Shape False Hold

Legacy meaning:

- a quiet-looking shell or favorable trade picture was narrated as fortress or
  hold truth
- static shape was mistaken for denied-entry or blockade survival

Observed repeatedly in rewrite review:

- `draw/hold` wording inferred from shell geometry alone
- `TradeInvariant` or favorable simplification reused as if it proved a hold

New-system owner:

- object generation and certification boundary

New-system rule:

- `FortressHoldingShell` needs current denied-entry and blockade geometry on
  the board
- `TradeInvariant` and `perpetual/fortress` stay distinct; neither may
  short-circuit the other
- a `SupportOnly` or `Deferred` hold certification may remain explanatory
  support, but it must not become projection admission

### 14. Upper Verdict Back-Projected Into Object Admission

Legacy meaning:

- an object row borrowed an upper-layer verdict as if it were a legal
  admission or exclusion guard
- certification wording was reused to decide whether the lower object existed

Observed repeatedly in rewrite review:

- `promotion_race` or `winning endgame` used as a reason to suppress
  `EndgameRaceScaffold`
- later-layer labels treated as if they were board-exact blockers

New-system owner:

- object generation and certification boundary

New-system rule:

- object admission must use board-exact predicates only
- upper-layer verdicts may remain related rows, but they may not become object
  guards

### 15. Prose Region Masquerading As Geometry

Legacy meaning:

- shell, region, or approach wording sounded precise but had no frozen square
  mask behind it
- prose-only locality was mistaken for an exact helper

Observed repeatedly in rewrite review:

- `same shelter region` used without a king-centered mask
- `local shell` or `immediate approach band` used without explicit entry
  squares

New-system owner:

- object generation and certification boundary

New-system rule:

- shell and shelter helpers must freeze king-centered masks and square-level
  entry conditions
- prose locality may explain a helper, but may not replace its board geometry

### 16. Generic Liquidation Inflated Into Delta Corridor

Legacy meaning:

- any trade-down, capture, or compressed-looking ending was narrated as if it
  already owned transition-compression semantics
- a local exchange was mistaken for a canonical delta corridor

Observed repeatedly in rewrite review:

- generic liquidation wording reused as if it proved `TradeCompressionCorridor`
- quiet file alignment or reciprocal pressure treated as if a real
  move-local compression transition had already happened

New-system owner:

- delta generation and delta validation boundary

New-system rule:

- `TradeCompressionCorridor` needs both:
  - a board-coherent non-king capture on the played move
  - an after-board reciprocal exchange corridor inside a frozen compressed
    trade window
- generic liquidation, quiet corridor alignment, and broad transition-storyline
  wording stay outside delta admission

### 17. Result-Side Simplification Collapsed Into Invariant Lane

Legacy meaning:

- a simplification that merely looked favorable was narrated as if it already
  preserved the same underlying task
- result wording or task-switch creation was mistaken for a real trade
  invariant

Observed repeatedly in rewrite review:

- one good exchange treated as if it proved `TradeInvariant`
- a move that created a new object or only suggested `winning endgame` / hold
  value was narrated as if it preserved a pre-existing task

New-system owner:

- delta generation and certification boundary

New-system rule:

- `TradeInvariant` needs both:
  - bounded material reduction on the played move
  - one same-family same-anchor object that persists from before-board to
    after-board
- result-side wording, task-switch creation, fortress/result verdicts, and
  projection `S19` wording may support later layers but may not become delta
  admission

### 18. Comparative Certification Collapsed Into One Local Support

Legacy meaning:

- one local support fragment or one suggestive object was narrated as if it
  already proved a comparative certification verdict
- broad superiority wording was mistaken for a real comparative burden

Observed repeatedly in rewrite review:

- development lead inferred from opening posture alone
- king fragility inferred from shell damage or attack pressure alone
- mobility edge inferred from restriction geometry alone

New-system owner:

- certification generation and certification validation boundary

New-system rule:

- comparative certification rows need explicit asymmetry or comparison helpers
- local support may strengthen the row, but may not replace the comparative
  burden

### 19. Result Certification Borrowed From Lower Carrier Presence

Legacy meaning:

- an object or delta survived and was then treated as if the result-side
  certification was already proved
- conversion or hold wording was mistaken for a finished verdict

Observed repeatedly in rewrite review:

- `EndgameRaceScaffold` treated as if it already proved `promotion_race`
- `FortressHoldingShell` treated as if it already proved fortress draw
- `TradeInvariant` treated as if it already proved `winning_endgame`

New-system owner:

- certification generation and certification validation boundary

New-system rule:

- lower carrier presence is never enough by itself for a result-side
  certification
- certification must still pay best-defense, route-survival, or hold burden as
  frozen by the row contract

### 20. Raw Engine Score Masquerading As Certified Verdict

Legacy meaning:

- one centipawn number or one shallow engine line was narrated as if it owned
  the branch truth
- numeric eval drift was mistaken for a stable certification verdict

Observed repeatedly in rewrite review:

- one favorable engine number reused as if it proved winning endgame
- one shallow tactical line reused as if it proved mate net or perpetual

New-system owner:

- engine/probe contract and certification validation boundary

New-system rule:

- engine output must be normalized into categorical burden evidence
- engine/probe is side evidence only
- numeric eval alone may not create a certification row
- stale or cross-board evidence bundles may not be replayed onto a different
  certification extraction; exact-board evidence mismatch fails closed
- Engine E certification evidence freeze: Engine E is certification evidence
  only, not a `Root/U/Object/Delta/Sxx truth owner`
- `CertificationEngineEvidenceContract` is the typed admission boundary; raw
  eval/PV dumps are not evidence until exact FEN, node identity, freshness,
  depth, MultiPV, score, and legal-PV checks pass
- `CertificationEngineRuntimeIntake` may normalize optional analyse/backend
  packets into the typed contract, but missing, rejected, inactive-family,
  invalid-owner, invalid-UCI, stale, or insufficient packets leave the base
  certification path unchanged rather than upgrading truth
- illegal PV replay, truncated PV replay, stale search state, mate-as-centipawn
  conversion, and undeclared best-defense / conversion / persistence purposes
  fail closed
- scoreless bounded claims, duplicate bounded claims, duplicate MultiPV first
  moves for best-defense, and unbound or cross-board eval-swing baselines fail
  closed
- eval-swing baselines with stale or mismatched node/ply binding fail closed
- root-state equivalence is not enough for Engine E identity; normalized
  full-FEN strings, including clock fields, must match

### 21. Reference Context Inflated Into Truth

Legacy meaning:

- source-backed names, examples, or study labels sounded authoritative and were
  treated as if they proved the current board
- an opening label, motif tag, endgame-study name, or retrieved example became
  a current-position claim without paying the exact-board carrier burden

New-system owner:

- source-context validation and the later consuming layer

New-system rule:

- opening rows are context, line identity, or statistics only
- motif tags need an exact-board detector carrier
- endgame-study labels need exact material, placement, and relation
  applicability before they become context
- endgame-study context is not win, draw, loss, forced-conversion, or
  forced-line evidence
- retrieved examples and snippets are non-authoritative references only
- game-context and endgame result-service family inputs are rejected by the
  current source-context schema and deferred to separate lanes

## Global Rules Learned From Legacy Failures

### Rule 1: No Late Semantic Salvage

Planner, renderer, and replay layers may not “save” a weak claim by inventing
missing strategic meaning.

### Rule 2: Best Defense Is Mandatory For Strong Strategic Claims

If a claim depends on persistence, denial, conversion, or branch superiority,
best-defense proof is part of the claim.

### Rule 3: Local Scope Must Stay Local

The system may not widen a local file, square, or route claim into whole-board
language unless a later dedicated matrix proves that broader scope.

### Rule 4: Support-Only Is A Real Endpoint

Support-only is not a staging area to be revived by wording.

### Rule 5: Deferred Is A Real Endpoint

Deferred means the evidence burden is not met. It is not a planner hint and not
a replay seed.

### Rule 6: Every New Family Starts Negative-First

No new strategic family should open on broad positive wording first.

Each family must first survive:

- exact positive
- exact negative
- near miss
- nasty negative
- false-witness
- false-rival
- surface reinflation

## Immediate Use In The New Branch

These legacy lessons must be consumed by:

- `ObjectDeltaContract.md`
- `EngineEvidenceContract.md`
- `ValidationMethodology.md`

No implementation work should bypass this taxonomy.
