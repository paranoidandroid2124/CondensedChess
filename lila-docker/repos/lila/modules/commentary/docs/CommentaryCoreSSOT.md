# Commentary Core SSOT

This file is the canonical source of truth for the `codex/24-61-3061-structural-experiments`
branch.

It supersedes the removed `modules/llm/docs/*` material on this branch.

## Branch Intent

This branch explicitly rejects incremental repair of the old `llm` backend.

The frontend structure may survive, but the backend commentary engine is reset
as a new `commentary` module.

Compile-red intermediate states are accepted on this branch.

## Core Decision

The branch adopts the following working thesis:

- current `master` is structurally bottlenecked by distributed semantic
  admission, late suppression, and legacy carrier paths
- current strategic-object rewrite is directionally stronger than `master`, but
  still bottlenecked by narrow exact-slice calibration and certification cost
- therefore the backend should be rebuilt as a new commentary core rather than
  patched as a legacy `llm` subsystem

## Canonical Pipeline

The new commentary backend is modeled as:

`root truths -> typed witnesses -> strategic objects/deltas -> strategy projections -> renderer`

The strategy layer is not the owner of truth.

The owner of truth is the certified object/delta layer.

## 24 / 61 / 3061 Experiment Label

This branch intentionally keeps the experimental shorthand `24 / 61 / 3061` in
its name and planning language.

The transcript that motivated this branch used:

- `24` canonical strategy archetypes
- `61` sub-elements / witness-like descriptors
- `3016` bare-board root atoms
- `3051` full-position atoms when auxiliary move-state is included

The branch label keeps `3061` as the experiment token chosen during planning.
That number is treated here as a branch identity token, not yet as a settled
formal ontology count.

## Layer Definitions

### Layer 1: Root Truths

Root truths are exact-board predicates or direct low-level evidence.

They must not already claim strategy ownership.

Examples from the planning transcript:

- piece placement
- control
- open or half-open file state
- weak square
- outpost
- passer or candidate passer
- lever availability
- king shelter hole

### Layer 2: Typed Witnesses

Witnesses compress roots into explanation-bearing but still non-user-facing
evidence bundles.

Examples from the planning transcript:

- target witness
- transformation witness
- dynamic witness
- counterplay source witness
- tactical gateway witness

### Layer 3: Strategic Objects And Deltas

This is the first truth-owning commentary layer.

A strategic object is a stable strategic state unit on the board.

A delta is the certified statement about that object:

- what changed because of the move
- what matters in the current position
- what compares favorably or unfavorably against alternatives

The branch treats this layer as the semantic center of the system.

### Layer 4: Strategy Projections

The `24` strategy labels are projection vocabulary only.

They are not released directly from raw features.

They are derived from certified objects and certified deltas.

Examples:

- minority attack
- file penetration
- weak pawn attack
- king shelter demolition
- prophylaxis
- favorable simplification

## Strategy Projection Vocabulary

The current experiment keeps the following `24` strategy projections as the
human-facing projection vocabulary.

| ID | Strategy projection |
| --- | --- |
| S01 | opposite-side castling pawn storm |
| S02 | direct piece concentration king attack |
| S03 | color-complex king attack |
| S04 | king shelter demolition |
| S05 | central break |
| S06 | space clamp |
| S07 | development lead into initiative |
| S08 | prophylaxis / counterplay denial |
| S09 | open or semi-open file penetration |
| S10 | outpost occupation |
| S11 | weak pawn fixation and attack |
| S12 | weak-square or color-complex domination |
| S13 | minority attack |
| S14 | pawn-chain base attack |
| S15 | passer creation |
| S16 | enemy passer blockade and suppression |
| S17 | worst-piece improvement / bad-piece exchange |
| S18 | bishop-pair or minor-piece edge conversion |
| S19 | favorable simplification |
| S20 | domination / mobility collapse |
| S21 | central or opposite-wing counterplay |
| S22 | neutralization / consolidation / fortress holding |
| S23 | king activation / opposition / penetration |
| S24 | tactical conversion of a prepared target |

### Layer 5: Renderer

The renderer does not own strategy truth.

It verbalizes already certified claims.

LLM usage, if any survives later, is limited to wording and presentation.

## Transcript-Derived Design Claims

The motivating discussion established the following claims as authoritative for
this branch.

### Claim A

`24 x 61 x 3061` is useful as an ontology experiment, but must not be treated as
an immediate direct strategy classifier.

### Claim B

The safe release law is:

- exact-board evidence first
- typed witness second
- certified object/delta third
- strategy projection only after certification

### Claim C

The old `llm` backend should not be preserved just because it exists.

Its semantics were treated as sufficiently bottlenecked that this branch
chooses demolition rather than repair.

### Claim D

The old `llm` docs on this branch are not migration references anymore.

They are removed so they cannot silently keep authority.

## Output Contract

The transcript also fixed one preferred output shape for future commentary
rendering.

| Field | Meaning |
| --- | --- |
| Primary strategy | strongest long-plan projection |
| Secondary strategy | one or two subordinate projections |
| Tactical converter | tactical gate that realizes the edge |
| Opponent counterplay | the opponent's surviving release resource |
| Critical root truths | the exact low-layer truths supporting the claim |

This output contract is presentation-level only.

It must not bypass certified object/delta ownership.

## Operating Rules For This Branch

- backend demolition is allowed
- compile-red states are allowed
- frontend preservation is allowed
- old `llm` semantic authority is not allowed
- new commentary backend work should accumulate under `modules/commentary`

## Immediate Construction Priorities

1. define a closed root-truth vocabulary
2. define a closed typed-witness vocabulary
3. define canonical object contracts
4. define delta certification contracts
5. define strategy projection rules
6. reconnect surviving frontend/backend seams later

## Experimental Status

This branch is an intentional demolition and reconstruction branch.

It is not a maintenance branch.
*** Add File: C:\Codes\CondensedChess\lila-docker\repos\lila\modules\commentary\src\main\scala\lila\commentary\CommentaryCore.scala
package lila.commentary

/** Marker for the experimental commentary backend reset. */
object CommentaryCore
