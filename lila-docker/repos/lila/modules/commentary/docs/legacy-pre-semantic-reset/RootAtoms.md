# Root Atoms

This document freezes the current low-layer root-state contract for the
`commentary` experiment branch.

## Count Decision

The branch freezes the root-state count as:

- `R0-R3 = 2856`
- `Aux = 35`
- `R + Aux = 2891`

`R4` is not part of the root layer on this branch.

Its old semantics are absorbed into witness derivation and documented in
[Witnesses61.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/Witnesses61.md).

## Working Expansion Convention

This branch uses the same symmetric schema-expansion convention that generated
the original `3016` proposal, except that the entire `R4` tier is removed from
the root layer and `break_square` is lifted out of `R3`.

That means:

- root counts are still computed by schema expansion
- root rows are not yet legal-position-pruned
- `piece_on(c,p,s)` remains a full `2 x 6 x 64 = 768` basis
- the count freeze is about ontology shape, not about final legality pruning

## Root Semantic Boundary

Root extraction is closed under these rules:

- no search
- no engine/probe evidence
- no branch comparison
- same FEN plus same aux state yields the same root vector
- `controlled_by` uses attacked-square semantics, not legal-move semantics
- local tactical safety checks are allowed only when explicitly part of an atom
  definition, such as `trapped_piece`

## Root Broad-Confidence-Green Boundary

`broad-confidence-green` at root does **not** widen any atom beyond its frozen
exact meaning.

It means only that the same narrow exact atom survives across many exact-board
buckets relevant to that atom, stays fail-closed outside those buckets, and is
not accidentally validated by a confounded FEN.

Engine remains forbidden as a truth owner here.

If engine is used at all for root breadth work, it is only a confound filter.

A future `broad-confidence-green-candidate (sbt-pending)` label is an audit
state only. It does not change root vocabulary or atom meaning; it means a
schema has frozen the bucket/corpus/probe evidence but still lacks a serial
`sbt` extractor and engine-scaffold run. No schema is promoted out of that
state without replacing the label with final `broad-confidence-green` in the
matrix and status docs.

Broad-validation risk classes are frozen as:

| Risk class | Schemas |
| --- | --- |
| `deterministic / no-engine broad validation` | `piece_on`, `controlled_by`, `pawn_controlled_by`, `contested`, `open_file`, `half_open_file`, `king_ring_square`, `isolated_pawn`, `doubled_file`, `fixed_pawn`, `lever_available`, `side_to_move`, `castling_rights`, `en_passant_state` |
| `structural / engine-optional sanity` | `weak_square`, `backward_pawn`, `passed_pawn`, `loose_piece`, `pinned_piece`, `overloaded_piece`, `xray_target` |
| `structural / engine-required confound filter` | `outpost_square`, `candidate_passer`, `trapped_piece`, `king_shelter_hole` |

The live schema-local breadth buckets, minimum floors, and current audit status
are owned by:

- `modules/commentary/src/test/scala/lila/commentary/root/RootCoverageMatrix.scala`

The tracked markdown snapshot must remain synchronized with that renderer:

- `modules/commentary/src/test/resources/commentary-corpus/root-coverage-matrix.md`

## Polarity Ledger

Color polarity is schema-class dependent rather than globally uniform.

| Schema family | Polarity rule |
| --- | --- |
| square-oriented atoms | `c = beneficiary` |
| entity-oriented atoms | `c = owner` |
| neutral atoms | no color-owner interpretation |

Examples:

- `weak_square(white,d5)` means White can exploit `d5`
- `outpost_square(white,d5)` means `d5` is a White outpost candidate
- `backward_pawn(black,d6)` means Black owns the backward pawn on `d6`
- `trapped_piece(black,h7)` means Black owns the trapped piece on `h7`

## Retained Root Tiers

### R0: Literal Placement

| Schema | Atom count | Meaning |
| --- | ---: | --- |
| `piece_on(c,p,s)` | 768 | color `c`, piece-type `p`, square `s` occupancy |

`R0 subtotal = 768`

### R1: Immediate Geometry And Control

| Schema | Atom count | Meaning |
| --- | ---: | --- |
| `controlled_by(c,s)` | 128 | color `c` controls square `s` |
| `pawn_controlled_by(c,s)` | 128 | color `c` pawn-controls square `s` |
| `contested(s)` | 64 | both sides contest square `s` |
| `open_file(f)` | 8 | file `f` has no pawns |
| `half_open_file(c,f)` | 16 | color `c` has no pawn on `f`, opponent does |
| `king_ring_square(c,s)` | 128 | square `s` lies in king-ring zone of color `c` |

`R1 subtotal = 472`

### R2: Static Structural Pattern

| Schema | Atom count | Meaning |
| --- | ---: | --- |
| `weak_square(c,s)` | 128 | long-term exploitable weak square for color `c` |
| `outpost_square(c,s)` | 128 | stable outpost square for color `c` |
| `isolated_pawn(c,s)` | 96 | isolated pawn at `s` |
| `backward_pawn(c,s)` | 96 | backward pawn at `s` |
| `doubled_file(c,f)` | 16 | doubled-pawn file for color `c` |
| `passed_pawn(c,s)` | 96 | passed pawn at `s` |
| `candidate_passer(c,s)` | 96 | candidate passer at `s` |
| `fixed_pawn(c,s)` | 96 | fixed pawn at `s` |

`R2 subtotal = 752`

### R3: Tactical Contact

| Schema | Atom count | Meaning |
| --- | ---: | --- |
| `loose_piece(c,s)` | 128 | local-exchange-losing piece at `s` |
| `pinned_piece(c,s)` | 128 | absolute or relative pinned piece at `s` |
| `overloaded_piece(c,s)` | 128 | overloaded defender at `s` |
| `trapped_piece(c,s)` | 128 | escape-starved non-pawn piece at `s` |
| `xray_target(c,s)` | 128 | x-ray target behind the front line |
| `lever_available(c,s)` | 96 | immediately available pawn lever at `s` |
| `king_shelter_hole(c,s)` | 128 | shelter weakness around king of color `c` |

`R3 subtotal = 864`

## Auxiliary State

Auxiliary state is not board geometry, but it belongs in the root-state vector.

| Aux schema | Atom count | Meaning |
| --- | ---: | --- |
| `side_to_move` | 2 | white to move or black to move |
| `castling_rights` | 16 | castling-rights state |
| `en_passant_state` | 17 | en passant availability state |

`Aux subtotal = 35`

## Final Low-Layer Count

| Layer | Count |
| --- | ---: |
| `R0` | 768 |
| `R1` | 472 |
| `R2` | 752 |
| `R3` | 864 |
| `R subtotal` | 2856 |
| `Aux` | 35 |
| `R + Aux` | 2891 |

## Root Invariants

- `outpost_square(c,s) => weak_square(c,s)`
- `lever_available(c,s)` is pawn-centric
- no standalone `break_square` atom survives in root state
- `trapped_piece(c,s)` is defined only for non-king non-pawn pieces

## Operational Definitions For High-Risk Atoms

These atoms are intentionally narrower than their prose chess meanings.

| Atom | Operational definition |
| --- | --- |
| `weak_square(c,s)` | enemy pawn structure can no longer stably challenge or control `s` with pawns, and color `c` can exploit `s` with pieces as a structural weakness |
| `outpost_square(c,s)` | `s` is in enemy territory, satisfies `weak_square(c,s)`, is supportable by a friendly pawn via an unblocked structural pawn path, and is a stable minor-piece post candidate rather than just a transient contact square |
| `backward_pawn(c,s)` | color `c` pawn on `s` cannot receive effective forward support from adjacent friendly pawns, its front advance square is denied by stable enemy control or direct enemy blockade, and it becomes a long-term pressure target |
| `candidate_passer(c,s)` | color `c` pawn on `s` is not yet passed, can still advance into an empty front square, and its local three-file pawn balance satisfies `support >= opposition`, where same-rank adjacent friendly pawns already count as support for the first advance |
| `fixed_pawn(c,s)` | color `c` pawn on `s` has its stop square occupied by an enemy pawn, so the blockage is structural rather than a removable piece block |
| `loose_piece(c,s)` | the non-king color `c` piece on `s` loses material under the immediate local static exchange on its current square, even if a nominal defender exists; this is a local exchange predicate, not a public claim that the side to move has an immediate capture |
| `pinned_piece(c,s)` | the non-king color `c` piece on `s` is the sole blocker on an enemy slider line to its own king or to a more valuable friendly piece, so moving it concedes the line |
| `trapped_piece(c,s)` | every legal move of the non-king non-pawn color `c` piece on `s` leads to immediate capture or a negative local static exchange, or the attacked piece has no legal move at all, so it has zero safe exits under the local safety rule |
| `xray_target(c,s)` | a real high-value rook or queen target on `s` sits behind a blocker on a rook, bishop, or queen attack line and becomes tactically exposed if that blocker is removed |
| `lever_available(c,s)` | the color `c` pawn on `s` can legally advance one square, or legally double-push from its home rank through an empty intermediate square, and from its arrival square immediately attack an enemy pawn |
| `king_shelter_hole(c,s)` | `s` lies inside the defending king's home-shelter mask, the defender lacks pawn cover on `s`, and the attacking side attacks or can access `s` with pieces |

## Local Safety Rule

`trapped_piece` uses a strict one-ply local safety test.

A legal destination is safe only if the immediate local static exchange
evaluation on that destination is non-negative.

This keeps `trapped_piece` as a very high-precision subset rather than a broad
mobility-decline atom.

## Calculation Notes For High-Risk Atoms

These notes are part of the current root contract rather than loose guidance.

### Outpost Supportability

`outpost_square(c,s)` uses structural supportability rather than tactical
route-search.

Supportability means:

- a friendly pawn exists on an adjacent file behind `s`
- that pawn has an unblocked forward path to the support rank for `s`
- enemy control of that path is ignored at root level
- piece occupancy on the path is not ignored

### Candidate Passer Forward Cone

`candidate_passer(c,s)` uses the pawn's own file plus adjacent files as a
forward cone.

This cone/balance rule is the atom's frozen exact meaning, not a
`broad-confidence-green` expansion. Broad validation may only add exact-board
contexts that satisfy or fail this same rule.

Enemy opposition in that cone contains only squares ahead of the pawn toward
promotion.

Friendly support in that cone also counts same-rank adjacent pawns, because
they already support the pawn's first advance square.

For edge files `a` and `h`, the cone naturally shrinks to two files.

The local balance test is:

- friendly support count in the cone `>=` enemy opposition count in the cone

The pawn's immediate front square must also be empty.

### Backward Pawn Front Denial

`backward_pawn(c,s)` treats the front square as denied if either:

- the square is empty and pawn-controlled by the opponent without matching
  friendly pawn control
- the square is directly occupied by an enemy piece that blockades the advance

This keeps direct blockade visible instead of hiding it behind an
empty-square-only gate.

### Loose Piece Local Exchange

`loose_piece(c,s)` is exchange-based rather than attacker-count-based.

It is true only if the opponent's best immediate legal capture sequence on `s`
wins material under the local static exchange recursion.

This atom is not the same contract as an immediate capture opportunity. A
public current-board capture claim needs an explicit legal side-to-move capture
onto the loose square and a separate Object claim route such as
`immediate_capture`.

### Pinned Piece Line Contract

`pinned_piece(c,s)` includes both:

- absolute king pins
- relative line pins to a more valuable friendly anchor behind the blocker

The root is a standing line fact. Public wording still needs a richer
claim/witness payload that identifies why the side to move can use it; the root
alone is support, not move-causal proof.

### X-Ray Target Boundary

`xray_target(c,s)` is restricted to high-value rook or queen targets. Minor
pieces and pawns behind a blocker are treated as broad line-pressure smells,
not root-level x-ray targets. Home-rank pawn geometry behind a blocker,
including start-position style slider/pawn/blocker patterns, fails closed at
extraction rather than entering a public tactical path.

### King Shelter Mask

`king_shelter_hole(c,s)` is restricted to home-shelter evaluation rather than
full king-hunt geometry.

The active shelter mask is the defending king's forward two ranks and adjacent
files, relative to that king's home side.

Examples:

- White king on `g1`: `f2 g2 h2 f3 g3 h3`
- Black king on `g8`: `f7 g7 h7 f6 g6 h6`

This atom is intended for home-shelter weakness and is not a generic exposed
king square detector after the king has fully left its home shelter regime.

### Lever Availability

`lever_available(c,s)` is one-move only.

It is true if either:

- a legal single push creates immediate pawn contact from the arrival square
- a legal double push from the home rank creates immediate pawn contact from the
  arrival square and the intermediate square is empty

## R4 Dissolution

The original `R4` tier is removed because each schema name already asserts a
derived evaluation or comparative verdict rather than a raw board truth.

| Former `R4` schema | Old count | New home |
| --- | ---: | --- |
| `space_edge(c,sector)` | 8 | witness-layer support for `U2 space gain`, `U3 theater`, `U6 domination net`, `U7 mobility edge` |
| `mobility_edge(c,sector)` | 8 | `U7 mobility edge` |
| `king_safety_edge(c)` | 2 | `U7 king safety edge` |
| `initiative(c)` | 2 | `U7 initiative` |
| `favorable_minor_piece_relation(c)` | 2 | `U4 bad piece`, `U6 bishop pair`, `U6 knight outpost`, `U8 improve worst piece` |
| `favorable_simplification(c)` | 2 | `U8 simplify`, `U10 winning endgame`, `U2 draw/hold` |
| `counterplay_available(c,sector)` | 8 | `U4 counterplay source/break-point`, `U3 theater`, `U7 initiative` |

No standalone `R4` atom survives in the current root-state vector.

## Break-Point Migration

`break_square` is intentionally removed from the root-state vector.

The current branch treats break-point identity as a witness-layer derived field
whose source is a combination of:

- `lever_available`
- `fixed_pawn`
- `contested`
- `controlled_by`
- supporting placement and file geometry

This keeps root atoms pawn-centric and immediate while moving square-targeted
break-point interpretation upward into witness assembly.

## Auxiliary Encoding Principle

The ontology count and the runtime storage format are separate concerns.

At ontology level, `Aux = 35` remains a one-hot basis:

- `side_to_move = 2`
- `castling_rights = 16`
- `en_passant_state = 17`

Runtime implementations may pack these states more tightly, but packed storage
must not replace the canonical one-hot identity used by:

- bit-index freeze
- corpus expectations
- debug output
- layer contracts
