# Witnesses 61

This document freezes the `61`-descriptor inventory that sits above the
`2891`-dimensional root-state vector.

The important correction on this branch is that **the `61` entries are not all
standalone witness instances**.

Instead, the inventory is split across three ownership classes:

- `U-primary`
  - deterministic `phi(R)` witness instances with explicit anchors and payloads
- `U-attached`
  - deterministic descriptors that require a host witness or host object
- `upper-layer`
  - descriptors that stay in the `61` inventory for continuity, but whose
    actual truth ownership lives in `O/Δ`, certification, or strategy
    projection rather than in raw witness admission

## Global Contract

The current branch freezes the following `U`-layer rules.

1. `U = phi(R)` only
   - engine or probe evidence must not participate in witness derivation
2. witness and descriptor presence is strict `present / absent`
3. all `U`-owned outputs are deterministic for the same root vector
4. witness output is `Boolean + Payload`, not prose
5. `required_roots` may include:
   - direct root atoms
   - derived formulas over `R`
6. one descriptor may carry variant-specific contracts when one label would
   otherwise hide structurally distinct cases
7. `break_square` is not a root atom and does not survive as a standalone
   witness
   - it is carried upward only as payload on lever or counterplay-source style
     witnesses
8. attached descriptors must bind to a host
   - they are not allowed to float without a host witness or host object
9. global verdict words such as:
   - `initiative`
   - `mobility edge`
   - `king safety edge`
   - `counterplay not ready`
   are not allowed to re-enter `U` as if they were exact-board witness facts
10. historical inventory labels may survive for continuity, but runtime
    descriptor ids may narrow a legacy label when the legacy wording carries
    comparative or evaluative drift
11. one `U`-owned descriptor must not embed another `U`-owned descriptor as
    support payload
    - cross-witness composition begins only above `U`
12. a legacy `U-primary` descriptor that mixes multiple anchor shapes or
    upper-layer wording may narrow to one active runtime contract and one
    primary anchor

## Anchor Grammar

`U-primary` descriptors may use only these primary anchor types:

- `board`
- `sector`
- `file`
- `square`
- `piece-square`
- `ray`

`cluster` is not a primary anchor type.

If a descriptor needs clustered geometry, the cluster must be carried in payload
fields such as:

- `squareMask`
- `memberSquares`
- `targetSquares`
- `rayMask`

## Polarity Rules

Polarity is frozen as follows:

- `neutral`
  - for pure structural state that does not yet imply an exploiter
- `owner`
  - for entity-owned witnesses or configurations
- `beneficiary`
  - for exploiter-facing local witnesses
- `host`
  - for attached descriptors that inherit their host polarity by contract

`inherit` is not used as a free-form label.

If a host is `neutral`, then an attached descriptor that requires
`beneficiary` or `owner` polarity cannot attach to it without an explicit
translation rule.

Attached descriptors do not inherit host polarity by default.

If a host is `neutral` and only supplies scope, an attached descriptor may
declare `beneficiary` polarity with an explicit root-level translation rule.

## Dedup, Merge, And Ordering

If two outputs share the same:

- family
- descriptor
- polarity
- primary anchor

they must merge into a single instance.

Merged payload keeps the union of:

- supporting root indices
- target squares
- geometry masks
- supporting tags

Canonical ordering is:

1. family `U1 -> U10`
2. descriptor name
3. polarity `white -> black -> neutral`
4. anchor order
   - `board`
   - `sector`
   - `file`
   - `square`
   - `piece-square`
   - `ray`

## Ownership Count

| Ownership | Count |
| --- | ---: |
| `U-primary` | 36 |
| `U-attached` | 15 |
| `upper-layer` | 10 |
| `Total descriptor inventory` | 61 |

## Descriptor Ownership Map

| Descriptor | Family | Ownership | Formal anchor | Polarity | Notes |
| --- | --- | --- | --- | --- | --- |
| `opening-tempo` | U1 | `U-primary` | `board` | `neutral` | global phase witness |
| `middlegame-positional` | U1 | `U-primary` | `board` | `neutral` | payload uses `contested_sectors`, not evaluative `active` wording |
| `transition-liquidation` | U1 | `U-primary` | `board` | `neutral` | global phase witness |
| `endgame-race` | U1 | `U-primary` | `board` | `neutral` | race trigger plus low-material regime |
| `king attack` | U2 | `U-attached` | `-` | `host` | objective descriptor only |
| `material gain` | U2 | `U-attached` | `-` | `host` | objective descriptor only |
| `structural damage` | U2 | `U-attached` | `-` | `host` | objective descriptor only |
| `space gain` | U2 | `U-attached` | `-` | `beneficiary` | legacy inventory label only; runtime contract is `structural_space_claim` |
| `promotion/passer` | U2 | `U-attached` | `-` | `host` | objective descriptor only |
| `draw/hold` | U2 | `U-attached` | `-` | `host` | objective descriptor only |
| `center` | U3 | `U-attached` | `-` | `neutral` | theater descriptor only |
| `kingside` | U3 | `U-attached` | `-` | `neutral` | theater descriptor only |
| `queenside` | U3 | `U-attached` | `-` | `neutral` | theater descriptor only |
| `open/semi-open file` | U3 | `U-primary` | `file` | `neutral` | empty structure retains value before occupation |
| `diagonal/color complex` | U3 | `U-primary` | `ray` | `neutral` | structural lane or color-complex witness |
| `whole-board` | U3 | `U-attached` | `-` | `neutral` | theater descriptor only |
| `king shelter` | U4 | `U-primary` | `square` | `beneficiary` | local target class |
| `weak pawn` | U4 | `U-primary` | `piece-square` | `beneficiary` | local target class |
| `passed pawn` | U4 | `U-primary` | `piece-square` | `owner` | entity target class |
| `weak square/outpost` | U4 | `U-primary` | `square` | `beneficiary` | square target class |
| `key file/rank` | U4 | `U-primary` | `file` or `ray` | `beneficiary` | rank is treated as horizontal ray, not square |
| `bad piece` | U4 | `U-primary` | `piece-square` | `beneficiary` | local liability target |
| `loose/overloaded piece` | U4 | `U-primary` | `piece-square` | `beneficiary` | local tactical target |
| `development lag` | U4 | `upper-layer` | `-` | `-` | comparative development verdict; not a raw witness |
| `counterplay source/break-point` | U4 | `U-primary` | `piece-square` | `owner` | legacy inventory label only; runtime contract is `pawn_push_break_contact_source`; `break-point` is payload only and not a witness id |
| `open center` | U5 | `U-primary` | `sector` | `neutral` | structural trigger |
| `closed center` | U5 | `U-primary` | `sector` | `neutral` | structural trigger |
| `fixed chain` | U5 | `U-primary` | `sector` | `neutral` | structural trigger |
| `majority/minority asymmetry` | U5 | `U-primary` | `sector` | `neutral` | structure only; side information stays in payload |
| `available lever` | U5 | `U-primary` | `piece-square` | `owner` | immediate local trigger |
| `opposite-side castling/wing asymmetry` | U5 | `U-primary` | `board` | `neutral` | global structural trigger |
| `rook on open file` | U6 | `U-primary` | `piece-square` | `owner` | concrete configuration |
| `bishop pair` | U6 | `U-primary` | `board` | `owner` | board-anchored configuration; bishop member squares live in payload |
| `knight outpost` | U6 | `U-primary` | `piece-square` | `owner` | concrete configuration |
| `queen-bishop battery` | U6 | `U-primary` | `ray` | `owner` | geometric configuration |
| `rook lift` | U6 | `U-primary` | `piece-square` | `owner` | route/configuration witness |
| `defender shortage` | U6 | `U-primary` | `piece-square` | `beneficiary` | legacy inventory label only; runtime contract is `duty_bound_defender`; shortage wording is not a witness meaning |
| `domination net/restriction geometry` | U6 | `U-primary` | `piece-square` | `beneficiary` | legacy inventory label only; runtime contract is `short_run_slider_gate_restriction`; `domination net` is not a witness id |
| `initiative` | U7 | `upper-layer` | `-` | `-` | persistence and denial verdict, not a raw witness |
| `development lead` | U7 | `upper-layer` | `-` | `-` | comparative development verdict |
| `central tension` | U7 | `U-primary` | `sector` | `neutral` | deterministic contact-state witness |
| `mobility edge` | U7 | `upper-layer` | `-` | `-` | comparative verdict |
| `king safety edge` | U7 | `upper-layer` | `-` | `-` | upper-layer only; `U` runtime ids are forbidden; upper-layer contracts split into `comparative_king_fragility` and `certified_king_safety_edge` |
| `open line` | U8 | `U-attached` | `-` | `host` | transformation descriptor only |
| `exchange defender` | U8 | `U-attached` | `-` | `host` | transformation descriptor only |
| `simplify` | U8 | `U-attached` | `-` | `host` | transformation descriptor only |
| `create passer` | U8 | `U-attached` | `-` | `host` | transformation descriptor only |
| `improve worst piece` | U8 | `U-attached` | `-` | `host` | transformation descriptor only |
| `pin` | U9 | `U-primary` | `ray` | `beneficiary` | geometry payload is mandatory; contract is variant-sensitive |
| `fork` | U9 | `U-primary` | `piece-square` | `beneficiary` | tactical witness |
| `skewer` | U9 | `U-primary` | `ray` | `beneficiary` | geometric tactical witness |
| `overload` | U9 | `U-primary` | `piece-square` | `beneficiary` | tactical witness |
| `deflection/decoy` | U9 | `U-primary` | `square` | `beneficiary` | tactical witness |
| `interference` | U9 | `U-primary` | `square` | `beneficiary` | tactical witness |
| `clearance` | U9 | `U-primary` | `square` | `beneficiary` | tactical witness |
| `demolition/undermining` | U9 | `U-primary` | `square` | `beneficiary` | tactical witness |
| `mate net` | U10 | `upper-layer` | `-` | `-` | conversion verdict |
| `material harvest` | U10 | `upper-layer` | `-` | `-` | conversion or result verdict |
| `winning endgame` | U10 | `upper-layer` | `-` | `-` | conversion verdict |
| `perpetual/fortress` | U10 | `upper-layer` | `-` | `-` | conversion verdict |
| `promotion race` | U10 | `upper-layer` | `-` | `-` | conversion verdict |

## R4 Absorption Rule

The former `R4` tier is not preserved as root truth.

The surviving effects are distributed like this:

- `initiative`, `mobility edge`, and `king safety edge` remain in the
  descriptor inventory but are now `upper-layer`
- `counterplay_available` is absorbed into local `counterplay source`
  derivation, specifically `pawn_push_break_contact_source`, not into a global
  readiness verdict
- `favorable_simplification` no longer survives as a raw witness
- `space_edge` no longer survives as a standalone descriptor
  - it only strengthens structural descriptors such as:
    - `space gain`
    - `majority/minority asymmetry`

## Break-Point Absorption Rule

`break_square` is not a root atom and not a standalone witness.

Break-point identity is witness payload derived from:

- `lever_available`
- `fixed_pawn`
- `contested`
- `controlled_by`
- supporting placement and file geometry

## Reviewed High-Risk Verdicts

The current review verdicts for the most dangerous descriptors are:

| Descriptor | Verdict |
| --- | --- |
| `space gain` | `U-attached` only; runtime contract narrows to `structural_space_claim` |
| `defender shortage` | `U-primary` only; runtime contract narrows to `duty_bound_defender`; overload-style dependency stays outside this witness |
| `domination net/restriction geometry` | only `restriction geometry` survives as active `U-primary`; runtime contract narrows to `short_run_slider_gate_restriction` |
| `counterplay source/break-point` | `counterplay source` survives as active `U-primary`; runtime contract narrows to `pawn_push_break_contact_source`; `break-point` is payload only |
| `king safety edge` | `upper-layer` only; do not create a `U` runtime witness; use `comparative_king_fragility` and `certified_king_safety_edge` above `U` |
| `initiative` | `upper-layer` |

### Space Gain Narrowing

The historical inventory label `space gain` survives for `61`-table continuity
only.

The canonical runtime contract id is `structural_space_claim`.

Runtime identifiers such as:

- `space_gain`
- `space_advantage`
- `sector_space_advantage`

are not valid witness ids on this branch.

`structural_space_claim` remains `U-attached` with these rules:

- allowed hosts:
  - `closed center`
  - `fixed chain`
- disallowed hosts:
  - `majority/minority asymmetry`
  - `restriction geometry`
- primary anchor: `sector`
- host role: neutral scope provider only
- beneficiary is derived separately from the side that forms the present claim
  component inside the host-projected sector

The contract is defined from `R` plus host payload only:

```text
structural_frontier(host, sector) :=
  the host-derived set of boundary pawn squares inside the projected sector,
  computed only from host payload plus supporting `piece_on(_, pawn, _)` and
  `fixed_pawn(_, _)` roots

frontier_seed(beneficiary, square) :=
  square lies in the host-projected sector
  and controlled_by(beneficiary, square)
  and square is immediately beyond some frontier square in the beneficiary
  forward direction

boundary_attached(beneficiary, square) :=
  square lies in the same 8-connected component of beneficiary-controlled
  sector squares as at least one frontier_seed

claimed_square_set(beneficiary, sector) :=
  { square in host-projected sector | boundary_attached(beneficiary, square) }
```

Presence requires:

- `|claimed_square_set| >= 2`

Negative boundary:

- single-square forward post
- host exists but no connected `claimed_square_set`
- control squares exist but are not attached to any structural frontier
- the supposed claim is only a restatement of `restriction geometry`

Payload is restricted to:

- `host_id`
- `sector`
- `beneficiary`
- `claimed_squares`
- `boundary_pawn_squares`

The payload must not embed parallel `U-primary` witnesses such as:

- `available lever`
- `weak square/outpost`
- `restriction geometry`

### Restriction Geometry Narrowing

The historical inventory label `domination net/restriction geometry` survives
for `61`-table continuity only.

The canonical runtime contract id is `short_run_slider_gate_restriction`.

Runtime identifiers such as:

- `domination_net`
- `restriction_geometry`
- `slider_restriction`

are not valid witness ids on this branch.

`short_run_slider_gate_restriction` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- anchor target:
  - defender `bishop`
  - defender `rook`
  - defender `queen`
- local meaning:
  - beneficiary-caused short-run throttling of an anchored enemy slider's
    testable directions

The contract is defined from `R` only:

```text
occupied_by(color, square) :=
  exists piece_on(color, _, square)

first_step(anchor_square, direction) :=
  the first on-board square from the anchor in that slider-legal direction

testable_direction(direction) :=
  first_step exists
  and not occupied_by(defender, first_step)

legal_ray_run(direction) :=
  the ordered legal landing squares from the anchor in that direction,
  stopping before any defender-owned blocker and including the first
  beneficiary-occupied blocker square as the last capturable landing square

ray_mobility(direction) :=
  |legal_ray_run(direction)|

beneficiary_claimed(square) :=
  occupied_by(beneficiary, square) or controlled_by(beneficiary, square)

short_run_throttled(direction) :=
  testable_direction(direction)
  and 1 <= ray_mobility(direction) <= 2
  and every square in legal_ray_run(direction) satisfies
      beneficiary_claimed(square)
  and exists square in legal_ray_run(direction) satisfying
      controlled_by(beneficiary, square)
```

Presence requires:

- `testable_count >= 2`
- `restricted_count >= 2`
- `restricted_count * 2 >= testable_count`
- at least one `testable_direction` remains not `short_run_throttled`

Negative boundary:

- self-blocked directions only
- edge-shortened mobility only
- beneficiary blocker with no beneficiary control
- remote wall only after `3+` free landing squares
- full entrapment
- pin restatement only
- space-claim restatement only
- counterplay-denial restatement only

Payload is restricted to:

- `anchor_piece_square`
- `beneficiary`
- `throttled_directions`
- `testable_directions`
- `ray_mobility_by_direction`
- `beneficiary_occupied_gate_squares`
- `beneficiary_controlled_gate_squares`
- `open_testable_directions`

The payload must not embed parallel `U` witnesses such as:

- `pin`
- `counterplay source`
- `structural_space_claim`
- `rook lift`
- `duty_bound_defender`

### Defender Shortage Narrowing

The historical inventory label `defender shortage` survives for `61`-table
continuity only.

The canonical runtime contract id is `duty_bound_defender`.

Runtime identifiers such as:

- `defender_shortage`
- `defender_constrained`
- `defender_dependency`

are not valid witness ids on this branch.

`duty_bound_defender` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- anchor target:
  - defender `knight`
  - defender `bishop`
  - defender `rook`
  - defender `queen`
- local meaning:
  - a load-bearing defender is physically bound by pin or trap while still
    carrying a current defensive duty from its present square

The contract is defined from `R` only:

```text
occupied_by(color, square) :=
  exists piece_on(color, _, square)

anchor_covers_from_current_geometry(square) :=
  the defender piece on anchor_square currently attacks, line-covers, or
  guards square from its present square by attacked-square geometry;
  do not require a legal move by the anchor

occupied_pressure_duty_square(square) :=
  occupied_by(defender, square)
  and (
    controlled_by(beneficiary, square)
    or xray_target(defender, square)
  )

king_gate_duty_square(square) :=
  king_ring_square(defender, square)
  and controlled_by(beneficiary, square)
  and contested(square)

duty_square(square) :=
  occupied_pressure_duty_square(square)
  or king_gate_duty_square(square)

assigned_duty_square(square) :=
  duty_square(square)
  and anchor_covers_from_current_geometry(square)

pin_bound_duty(anchor_square) :=
  pinned_piece(defender, anchor_square)
  and exists square such that assigned_duty_square(square)

trapped_bound_duty(anchor_square) :=
  trapped_piece(defender, anchor_square)
  and exists square such that assigned_duty_square(square)
```

Presence requires:

- `pin_bound_duty(anchor_square)`
  or `trapped_bound_duty(anchor_square)`

Negative boundary:

- board-level defender count shortage
- overloaded-only picture
- pinned or trapped piece with no current duty assignment
- nearby activity with no anchored duty
- empty non-king blockade square only
- generic shortage with no local anchor
- initiative, denial, or king-safety restatement

Payload is restricted to:

- `anchor_piece_square`
- `beneficiary`
- `bound_modes`
- `assigned_duty_squares`
- `occupied_pressure_duty_squares`
- `king_gate_duty_squares`

The payload must not embed parallel `U` witnesses or broader dependency claims
such as:

- `loose/overloaded piece`
- `pin`
- `king attack`
- `initiative`
- `counterplay not ready`

### Counterplay Source Narrowing

The historical inventory label `counterplay source/break-point` survives for
`61`-table continuity only.

The canonical runtime contract id is `pawn_push_break_contact_source`.

Runtime identifiers such as:

- `counterplay_source`
- `break_point`
- `counterplay_ready`

are not valid witness ids on this branch.

`pawn_push_break_contact_source` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- anchor target:
  - owner `pawn`
- local meaning:
  - an owner pawn has one or more legal push-contact variants that create
    immediate contact against a strategic defender pawn target

The contract is defined from `R` only:

```text
legal_push_contact_variant(anchor_square, variant) :=
  variant in {single_push, double_push}
  and the push is legal from the owner pawn on anchor_square
  and the arrival square is empty by pawn-push legality
  and from that arrival square the pawn immediately attacks at least one
      defender pawn
  # capture-arrival and en-passant are excluded in the current branch contract

contact_variants(anchor_square) :=
  all legal_push_contact_variant(anchor_square, variant)

arrival_square(anchor_square, variant) :=
  the arrival square of that contact variant

attacked_pawn_targets(anchor_square, variant) :=
  defender pawn squares immediately attacked from
  arrival_square(anchor_square, variant)

supports_forward(defender, square) :=
  the defender pawn on square attacks a defender pawn on a forward-diagonal
  square

supported_from_rear(defender, square) :=
  a defender pawn on a rear-diagonal square attacks square

same_rank_adjacent_count(defender, square) :=
  the number of same-rank horizontally adjacent defender pawns

structurally_burdened_target(square) :=
  fixed_pawn(defender, square)
  or backward_pawn(defender, square)
  or isolated_pawn(defender, square)

center_pawn_target(square) :=
  piece_on(defender, pawn, square)
  and file(square) in {c, d, e, f}

chain_base_target(square) :=
  piece_on(defender, pawn, square)
  and supports_forward(defender, square)
  and not supported_from_rear(defender, square)

chain_head_target(square) :=
  piece_on(defender, pawn, square)
  and supported_from_rear(defender, square)
  and not supports_forward(defender, square)

phalanx_edge_target(square) :=
  piece_on(defender, pawn, square)
  and same_rank_adjacent_count(defender, square) = 1

strategic_contact_target(square) :=
  structurally_burdened_target(square)
  or center_pawn_target(square)
  or chain_base_target(square)
  or chain_head_target(square)
  or phalanx_edge_target(square)

strategic_contact_targets(anchor_square, variant) :=
  { square in attacked_pawn_targets(anchor_square, variant) |
    strategic_contact_target(square) }
```

Presence requires:

- `|contact_variants(anchor_square)| >= 1`
- there exists a `variant` in `contact_variants(anchor_square)` such that
  `|strategic_contact_targets(anchor_square, variant)| >= 1`

Negative boundary:

- raw lever with no strategic contact target
- chain middle-node target only
- phalanx middle-node target only
- open-file pressure only
- battery or xray pressure only
- capture-arrival source only
- en-passant source only
- break-point resurrected as standalone witness
- `counterplay_available` restatement

Payload is restricted to:

- `owner_pawn_square`
- `contact_variants`:
  - `push_variant`
  - `arrival_square`
  - `target_pawn_squares`
  - optional `break_point_squares`

The payload must not embed parallel `U` witnesses or target-role tags such as:

- `weak pawn`
- `open/semi-open file`
- `queen-bishop battery`
- `xray_target`
- `initiative`

### King Safety Edge Layering

The historical inventory label `king safety edge` survives for `61`-table
continuity only.

This branch does not admit a `U` runtime witness for king-safety comparison.

Runtime identifiers such as:

- `king_safety_edge`
- `safer_king`
- `king_danger_edge`
- `unsafe_king`
- `king_exposure_edge`

are not valid `U` witness ids on this branch.

The upper-layer contracts are split into:

- `comparative_king_fragility`
- `certified_king_safety_edge`

`king safety edge` stays above `U` because it is:

- comparative rather than local-present/absent
- best-defense-sensitive
- dependent on cross-witness composition
- unstable under a single `U` anchor and a single `U` polarity

Lower fragments may contribute only when they are linked to the defending
king's theater:

```text
king_theater_link(fragment, defending_king) :=
  the fragment anchor intersects or directly projects into at least one of:
    - the defending king ring
    - the defending king's home-shelter mask
    - a king-adjacent open or semi-open file lane
    - a king-shelter diagonal or color-complex lane
    - a king escape or king-approach gate geometry
```

Allowed lower fragments when `king_theater_link` holds:

- roots:
  - `king_ring_square`
  - `king_shelter_hole`
  - `controlled_by`
  - `pinned_piece`
  - `xray_target`
  - `loose_piece`
- witnesses:
  - `king shelter`
  - `open/semi-open file`
  - `diagonal/color complex`
  - `rook on open file`
  - `queen-bishop battery`
  - `rook lift`
  - `duty_bound_defender`
  - `short_run_slider_gate_restriction`
  - `central tension`
  - `open center`
  - `opposite-side castling/wing asymmetry`
  - `king attack`:
    - host-chain only
    - never self-certifying

Forbidden fragment use:

- any fragment not linked to the relevant king theater
- local file pressure away from the king theater
- local diagonal pressure away from the king shelter
- `king attack` alone as proof of an edge
- hole count alone
- attacked-square count alone

Outside the current fragment grammar unless separately certified:

- central king
- uncastled king
- walked king
- post-shelter-regime king positions

`comparative_king_fragility` means:

- one king is structurally or positionally more fragile than the other

It requires:

- an asymmetric bundle of king-theater-linked fragments

It forbids:

- edge wording
- forcing-attack wording
- best-defense claims

`certified_king_safety_edge` means:

- one side holds an actionable, certified king-safety edge

It requires:

- `comparative_king_fragility` present
- `attack_host_viability`
- `attacker_budget_present`
- `phase_gate`
- `initiative_or_move_order_relevance`
- `best_defense_survival`

Material gate:

- `major_piece_presence` unless a separate certified forcing exception exists

It forbids:

- structure-only certification
- queenless or pure-endgame overclaim without explicit host viability

Support and consequence visibility remain allowed above the owner boundary:

- `king cover loosened`
- `defensive coordination strain`
- `king exposure increased`

These phrasings may appear as support or consequence of another owner, but they
do not create a lower-layer king-safety-edge owner.

The next review wave should continue from the most failure-prone `U-primary`
descriptors rather than broadening the table first.
