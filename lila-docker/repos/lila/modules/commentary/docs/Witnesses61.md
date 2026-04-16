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
| `counterplay source/break-point` | U4 | `U-primary` | `square` or `file` or `ray` | `owner` | active witness is local counterplay source; `break-point` is payload only |
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
| `defender shortage` | U6 | `U-primary` | `piece-square` | `beneficiary` | renamed in contract to `defender_constrained`; shortage wording is legacy inventory only |
| `domination net/restriction geometry` | U6 | `U-primary` | `piece-square` or `sector` or `ray` | `beneficiary` | active witness is `restriction geometry`; `domination net` verdict is upper-layer meaning |
| `initiative` | U7 | `upper-layer` | `-` | `-` | persistence and denial verdict, not a raw witness |
| `development lead` | U7 | `upper-layer` | `-` | `-` | comparative development verdict |
| `central tension` | U7 | `U-primary` | `sector` | `neutral` | deterministic contact-state witness |
| `mobility edge` | U7 | `upper-layer` | `-` | `-` | comparative verdict |
| `king safety edge` | U7 | `upper-layer` | `-` | `-` | comparative verdict; use local fragments instead |
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
  derivation, not into a global readiness verdict
- `favorable_simplification` no longer survives as a raw witness
- `space_edge` no longer survives as a standalone descriptor
  - it only strengthens structural descriptors such as:
    - `space gain`
    - `majority/minority asymmetry`
    - `restriction geometry`

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
| `domination net/restriction geometry` | only `restriction geometry` survives as active `U-primary` |
| `counterplay source/break-point` | `counterplay source` survives as active `U-primary`; `break-point` is payload only |
| `king safety edge` | `upper-layer` |
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

The next review wave should continue from the most failure-prone `U-primary`
descriptors rather than broadening the table first.
