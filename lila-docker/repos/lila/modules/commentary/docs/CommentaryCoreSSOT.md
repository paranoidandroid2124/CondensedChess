# Commentary Core SSOT

This file is the canonical source of truth for the
`codex/24-61-3016+35-structural-experiments` branch.

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
- current strategic-object rewrite was directionally stronger than `master`, but
  still bottlenecked by narrow exact-slice calibration and certification cost
- therefore the backend should be rebuilt as a new commentary core rather than
  patched as a legacy `llm` subsystem

## Canonical Pipeline

The new commentary backend is modeled as:

`root truths -> typed witnesses -> strategic objects/deltas -> strategy projections -> renderer`

The strategy layer is not the owner of truth.

The owner of truth is the certified object/delta layer.

## Count Freeze

The planning discussion started from the shorthand `24 / 61 / 3016+35`.

This branch now freezes the low-layer count as:

- `R0-R3 root atoms = 2856`
- `Aux state atoms = 35`
- `root-state vector = 2891`
- `R4` does not survive as a root tier
- `break_square` does not survive as a root atom

The original `3016` count remains useful only as the historical proposal that
still included `R4`.

The current branch decision is:

- `R0-R3 + Aux` are the root-state layer
- `R4` is dissolved upward into witness derivation
- `break_square` is dissolved upward into witness-level break-point payload
- the descriptor inventory stays fixed at `61`

See [RootAtoms.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/RootAtoms.md)
and [Witnesses61.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/Witnesses61.md)
and [RootIndexFreeze.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/RootIndexFreeze.md)
for the frozen low-layer contract.

Past failure lessons and the current validation charter are fixed in:

- [LegacyFailureTaxonomy.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/LegacyFailureTaxonomy.md)
- [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md)

## Layer Definitions

### Layer 1: Root Truths

Root truths are exact-board predicates or direct low-level evidence.

They must not already claim strategy ownership.

This branch currently keeps only `R0-R3` plus auxiliary move-state in the root
layer.

Root truth is additionally constrained by these semantic rules:

- `controlled_by` is attacked-square, not legal-move, semantics
- square-oriented atoms use beneficiary polarity
- entity-oriented atoms use owner polarity
- `candidate_passer` counts same-rank adjacent friendly support in its local
  majority test
- `loose_piece` is decided by local exchange loss, not raw attacker/defender
  counts
- `pinned_piece` includes relative slider pins to a more valuable friendly
  anchor, not only king pins
- `trapped_piece` is an extreme high-precision non-pawn atom with zero safe
  exits under the local safety rule

### Layer 2: Typed Witnesses

The branch keeps a fixed `61`-descriptor inventory above root truths, but that
inventory is no longer treated as if every entry were a standalone witness.

Instead it is split into:

- `U-primary`
  - active deterministic witness instances
- `U-attached`
  - deterministic descriptors that require a host
- `upper-layer`
  - inventory entries whose actual ownership lives in `O/Δ`, certification, or
    final projection rather than in raw witness admission

This keeps the historical `61` vocabulary while preventing dissolved `R4`
verdict words from sneaking back into `U` as if they were exact-board witness
facts.

#### Attached Witness Boundary Rules

The historical inventory label is not automatically the runtime contract id.

If a legacy inventory label carries comparative or verdict drift, the runtime
contract may use a narrower witness id while the `61` table keeps the legacy
label for continuity.

An attached descriptor does not inherit host polarity by default.

If the host is only a neutral scope provider, beneficiary polarity must be
derived by an explicit root-level rule inside the host-projected scope.

An attached descriptor must not absorb parallel `U-primary` witnesses into its
payload.

Cross-witness composition begins only above `U`.

Current recorded example:

- inventory label `space gain`
- runtime contract id `structural_space_claim`
- anchor `sector`
- allowed hosts `closed center` and `fixed chain`
- disallowed hosts `majority/minority asymmetry` and `restriction geometry`
- the present claim is a beneficiary-controlled connected square set attached
  to a host-supplied structural frontier

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

### Layer 5: Renderer

The renderer does not own strategy truth.

It verbalizes already certified claims.

LLM usage, if any survives later, is limited to wording and presentation.

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

## Transcript-Derived Design Claims

The motivating discussion established the following claims as authoritative for
this branch.

### Claim A

`24 x 61 x 3016+35` was a useful ontology proposal, but the current branch no
longer treats `R4` as root truth.

The living low-layer contract is `24 x 61 x 2856+35`.

### Claim B

The safe release law is:

- exact-board evidence first
- typed witness second
- certified object/delta third
- strategy projection only after certification

### Claim C

The old `llm` backend should not be preserved just because it existed.

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

1. freeze the closed `R0-R3 + Aux` root-state vocabulary
2. freeze the closed `61`-descriptor inventory and its ownership map
3. freeze past-failure taxonomy and canonical owner boundaries
4. freeze large-scale exact-position validation methodology
5. define canonical object contracts
6. define delta certification contracts
7. define strategy projection rules
8. reconnect surviving frontend/backend seams later

## Experimental Status

This branch is an intentional demolition and reconstruction branch.

It is not a maintenance branch.
