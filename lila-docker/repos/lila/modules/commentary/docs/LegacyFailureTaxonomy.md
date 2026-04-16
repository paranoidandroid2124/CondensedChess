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
