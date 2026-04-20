# Witnesses 61

This document freezes the `61`-descriptor inventory that sits above the
`2891`-dimensional root-state vector.

The important correction on this branch is that **the `61` entries are not one
flat witness class**.

Each row now carries one primary owner layer in the frozen staircase:

- `Witness / U-primary`
  - deterministic `phi(R)` witness instances with explicit anchors and payloads
- `Witness / U-attached`
  - deterministic descriptors that require a host witness or host object
- `Object`
- `Delta`
- `Certification`
- `Projection`

`upper-layer` survives only as a historical umbrella phrase for older review
notes.

The canonical row map now lives in
[DescriptorOwnershipMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DescriptorOwnershipMatrix.md).

The branch-level freeze summary now lives in
[DecisionFreezeLedger.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DecisionFreezeLedger.md).

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

Counts are by **primary owner layer per inventory row**.

Split rows still count once.

| Primary owner layer | Count |
| --- | ---: |
| `Witness / U-primary` | 18 |
| `Witness / U-attached` | 11 |
| `Object` | 7 |
| `Delta` | 2 |
| `Certification` | 10 |
| `Projection` | 13 |
| `Renderer` | 0 |
| `Total descriptor inventory` | 61 |

Derived historical rollup:

- former `upper-layer` umbrella = `Object 7 + Delta 2 + Certification 10 + Projection 13 = 32`

## Canonical Owner Matrix

The full row-by-row canonical map is frozen in
[DescriptorOwnershipMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DescriptorOwnershipMatrix.md).

This document keeps:

- `U`-layer contract law
- row-by-row reclassification rationale
- negative boundaries

Any later occurrence of `upper-layer` in retained review text is historical
umbrella shorthand only.

## R4 Absorption Rule

The former `R4` tier is not preserved as root truth.

The surviving effects are distributed like this:

- `initiative`, `mobility edge`, and `king safety edge` remain in the
  descriptor inventory but are now certification-side rows above `U`
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

The old high-risk review table used the historical `upper-layer` umbrella and is
no longer the canonical ownership source.

Use:

- [DescriptorOwnershipMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DescriptorOwnershipMatrix.md)
  for final owner layer and owner home
- the row sections below for exact contract law and negative boundaries

### Opening-Tempo Continuity Rehome

The historical inventory label `opening-tempo` survives for `61`-table
continuity only.

The row no longer remains admitted as a live `U-primary` witness on the
current branch.

Its continuity meaning now lives above `U`, inside the object-side owner
`OpeningDevelopmentRegime`.

Current branch rules:

- no active `U` runtime descriptor id survives for this row
- `phase_gate` remains only an upper release guard, not an admission source
- opening-development continuity may still project through object-side temporal
  release, but it must not re-enter `U` as a raw witness

The review conclusion is:

- current local docs do not expose an honest exact lower contract sufficient to
  keep this row in `U`
- the row therefore leaves `U` rather than surviving as a degenerate phase
  witness
- `OpeningDevelopmentRegime` becomes the canonical continuity owner above `U`

Object-side runtime contract:

- helper `opening_development_window`
- requires each side to retain at least one home-rank minor on its original
  square, at least one side to retain two such minors, each side to retain at
  least one home-rank rook on an original corner, and both central files `d`
  and `e` to remain unopened
- forbids move-count, tempo, and `phase_gate` admission
- runtime status: implemented in
  `modules/commentary/src/main/scala/lila/commentary/strategic/OpeningDevelopmentRegimeRule.scala`
  and verified by `StrategicObject7RuleTest` plus
  `StrategicObjectCorpusRuntimeTest`

Negative boundary:

- generic opening activity
- mere move-count or tempo narration
- king-safety release guards
- broad opening-specific strategy explanation
- phase/posture inflation across cells
- using `phase_gate` as the admission source
- treating the label as an upper-layer projection

### Middlegame-Positional Continuity Rehome

The historical inventory label `middlegame-positional` survives for `61`-table
continuity only.

The row no longer remains admitted as a live `U-primary` witness on the
current branch.

Its continuity meaning now lives above `U`, inside the object-side owner
`DistributedContactRegime`.

Current branch rules:

- no active `U` runtime descriptor id survives for this row
- `contested_sectors` remains payload geometry only and is not a standalone
  lower witness
- middlegame continuity may still use distributed-contact geometry above `U`,
  but that geometry must not be mistaken for a live `U` discriminator

The review conclusion is:

- current local docs do not expose an honest exact lower contract sufficient to
  keep this row in `U`
- the row therefore leaves `U` rather than surviving as a broad phase proxy
- `DistributedContactRegime` becomes the canonical continuity owner above `U`

Object-side runtime contract:

- helper `distributed_contact_spread`
- requires connected `contact_square` components under `front_connectivity` in
  at least two sectors, with every admitted component carrying at least two
  squares plus both contested and occupied-contact evidence, and with both
  colors already showing non-pawn deployment away from the home rank
- `contested_sectors` remains payload only and never becomes admission law
- runtime status: implemented in
  `modules/commentary/src/main/scala/lila/commentary/strategic/DistributedContactRegimeRule.scala`
  and verified by `StrategicObject7RuleTest` plus
  `StrategicObjectCorpusRuntimeTest`

Negative boundary:

- generic middlegame activity
- mere positional narration
- broad opening-to-middlegame storyline
- king-safety release guards
- phase/posture inflation across cells
- axis-independence claims not proven by the board
- treating the label as evaluative `active` wording
- treating the label as an upper-layer projection

### Transition-Liquidation Continuity Rehome

The historical inventory label `transition-liquidation` survives for
`61`-table continuity only.

The row no longer remains admitted as a live `U-primary` witness on the
current branch.

Its continuity meaning now lives above `U`, inside the delta-side owner
`TradeCompressionCorridor`.

Current branch rules:

- no active `U` runtime descriptor id survives for this row
- broad generic-liquidation wording remains only a negative boundary
- transition-compression meaning may still be carried by delta-layer trade
  corridors, but it must not re-enter `U` as a vague phase witness

The review conclusion is:

- current local docs do not expose an honest exact lower contract sufficient to
  keep this row in `U`
- the row therefore leaves `U` rather than surviving as a transition-phase
  proxy
- `TradeCompressionCorridor` becomes the canonical continuity owner above `U`

Delta-side start contract:

- scope: `move_local` only
- primary anchor: `board`
- runtime lives under
  `modules/commentary/src/main/scala/lila/commentary/delta`
- `StrategicDeltaRuntime` registers `TradeCompressionCorridor` before
  `TradeInvariant`
- public `CommentaryCore` exposes `activeDeltaFamilyIds`,
  `extractStrategicDeltas(...)` overloads from object extractions and from
  before/after `Fen` plus `playedMove`, and fail-closed delta extraction
  overloads
- helper `reciprocal_exchange_corridor`
- helper `compressed_trade_window`
- helper `trade_compression_transition`
  - the first live slice requires:
    - a board-coherent non-king capture on the played move
  - no queens on the after-board
  - at most `4` total non-king non-pawn pieces on the after-board
  - one canonical opposing non-king pair that currently attacks each other
    along one shared file or diagonal corridor on the after-board
  - the before-board failed either the corridor predicate or the compressed
    window
  - forbidden-rival rejection tracks the actual current-worktree
    `TradeInvariant` first slice rather than bare `EndgameRaceScaffold`
    persistence
- quiet corridor alignment, generic liquidation, or broad transition-storyline
  wording stays negative only
- runtime status: live; `TradeCompressionCorridorRuleTest`,
  `TradeInvariantRuleTest`, `StrategicDeltaBoundaryTest`,
  `DeltaExpectationCorpusTest`, and `CommentaryCoreBoundaryTest` are live
- `DeltaExpectationCorpusTest` now asserts live runtime extraction against the
  delta corpus rows

Negative boundary:

- generic liquidation only
- mere tempo narration
- whole-board language from a local phase row
- broad transition-storyline
- over-admitting `TransitionBridge`
- over-admitting `MoveLocal`
- over-admitting `PlanRace`
- over-admitting `InitiativeWindow`
- over-admitting `ConversionFunnel`
- over-admitting `PasserComplex`
- phase/posture inflation across cells
- treating the label as an upper-layer projection

### Endgame-Race Continuity Rehome

The historical inventory label `endgame-race` survives for `61`-table
continuity only.

The row no longer remains admitted as a live `U-primary` witness on the
current branch.

Its continuity meaning now lives above `U`, inside the object-side owner
`EndgameRaceScaffold`.

Current branch rules:

- no active `U` runtime descriptor id survives for this row
- low-material regime remains contextual only and is not a conversion claim
- race-trigger continuity may still live in object-side race scaffolds, but it
  must not re-enter `U` as a raw witness

The review conclusion is:

- current local docs do not expose an honest exact lower contract sufficient to
  keep this row in `U`
- the row therefore leaves `U` rather than surviving as a low-material phase
  witness
- `EndgameRaceScaffold` becomes the canonical continuity owner above `U`
- `promotion race` is already a certification-side conversion verdict and must
  remain distinct

Object-side runtime contract:

- helper `dual_run_endgame_trigger`
- requires a queenless board plus an advanced owner-side run resource for both
  colors on or beyond the fifth rank relative to each owner, where at least one
  such resource per color has an empty immediate forward square; passed/candidate
  evidence stays optional support when present
- runtime status: implemented in
  `modules/commentary/src/main/scala/lila/commentary/strategic/EndgameRaceScaffoldRule.scala`
  and verified by `StrategicObject7RuleTest` plus
  `StrategicObjectCorpusRuntimeTest`
- `promotion race`, `PasserComplex`, and `ConversionFunnel` remain outside
  object admission

Negative boundary:

- generic race talk
- mere tempo narration
- whole-board language from a local phase row
- broad endgame storyline
- over-admitting `PlanRace`
- over-admitting `PasserComplex`
- over-admitting `ConversionFunnel`
- over-admitting `promotion race`
- phase/posture inflation across cells
- treating the label as an upper-layer projection

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

claimable_sector_square(beneficiary, square) :=
  square lies in the host-projected sector
  and square is empty
  and controlled_by(beneficiary, square)

boundary_attached_component(beneficiary, component) :=
  component is one 8-connected component of claimable_sector_square values
  and at least one member of component either is a frontier_seed or is
  king-adjacent to a frontier_seed

claimed_square_set(beneficiary, sector) :=
  the strongest boundary_attached_component for that beneficiary and host,
  chosen deterministically by larger component size, then larger attached
  frontier-seed count, then lower square order
```

Presence requires:

- `|claimed_square_set| >= 2`

Current runtime closure:

- active attached runtime ids are frozen to `structural_space_claim` only
- `closed center` host is admitted only when a connected fixed central frontier
  spans both `d` and `e` files and contains fixed pawns from both colors
- `fixed chain` host is admitted only when one sector contains a same-color
  rear-supported fixed-pawn segment of length `>= 2`
- when several same-owner fixed-chain segments survive in one sector, runtime
  keeps the strongest segment that yields a live claim
- runtime variants are:
  - `closed_center_host`
  - `fixed_chain_host_white_segment`
  - `fixed_chain_host_black_segment`
- occupied frontier squares do not count as `claimed_squares`
- each emitted witness keeps one deterministic strongest connected attached
  component instead of unioning disconnected islands

Negative boundary:

- single-square forward post
- occupied frontier contact alone
- a lone locked center file is not `closed center`
- host exists but no connected `claimed_square_set`
- disconnected attached islands must not be unioned into one claim
- control squares exist but are not attached to any structural frontier
- the supposed claim is only a restatement of `restriction geometry`

Payload is restricted to:

- `host_id`
- `host_owner` for `fixed_chain` hosts only
- `sector`
- `beneficiary`
- `claimed_squares`
- `boundary_pawn_squares`

The payload must not embed parallel `U-primary` witnesses such as:

- `available lever`
- `weak square/outpost`
- `restriction geometry`

### Center Theater Shell Classification

The historical inventory label `center` survives for `61`-table continuity
only.

No admitted `U` runtime contract survives for this label on the current branch.

The row remains a `U-attached` shell with these rules:

- polarity: `neutral`
- local meaning:
  - theater descriptor only
  - central-theater wording only
  - not an exact-board structural, tactical, or evaluative witness
- code freeze:
  - the row is listed in the shell-only attached set
  - no attached runtime registration is admitted

The review conclusion is:

- no certified `center` object family exists in the rewrite north-star
- nearby exact center facts live in narrower structural or helper fragments,
  not in this row
- `open center`, `closed center`, `fixed chain`, `central tension`, and
  `majority/minority asymmetry` remain distinct rows and must not collapse into
  bare `center`
- any canonical assignment to `SpaceClamp`, `FixedTargetComplex`, or
  `TensionState` would be overfit

Negative boundary:

- openness meaning
- closure meaning
- clamp / break / initiative meaning
- treating the label as an exact-board witness
- treating `center` as `SpaceClamp`
- treating `center` as `FixedTargetComplex`
- treating `center` as `TensionState`

### Kingside Theater Shell Classification

The historical inventory label `kingside` survives for `61`-table continuity
only.

No admitted `U` runtime contract survives for this label on the current branch.

The row remains a `U-attached` shell with these rules:

- polarity: `neutral`
- local meaning:
  - theater descriptor only
  - kingside-theater wording only
  - not an exact-board structural, tactical, or evaluative witness
- code freeze:
  - the row is listed in the shell-only attached set
  - no attached runtime registration is admitted

The review conclusion is:

- no certified `kingside` object family exists in the rewrite north-star
- the only nearby rewrite mention is a fixed-target leak example, not a
  canonical owner
- `king attack`, `king shelter`, `rook lift`, `queen-bishop battery`, and
  opposite-side castling or wing-asymmetry rows remain distinct and must not
  collapse into bare `kingside`
- any canonical assignment to `SpaceClamp`, `FixedTargetComplex`,
  `TensionState`, or a kingside-specific upper family would be overfit

Negative boundary:

- openness meaning
- closure meaning
- clamp / break / initiative meaning
- king attack leakage
- king shelter leakage
- treating the label as an exact-board witness
- treating `kingside` as `SpaceClamp`
- treating `kingside` as `FixedTargetComplex`
- treating `kingside` as `TensionState`

### Queenside Theater Shell Classification

The historical inventory label `queenside` survives for `61`-table continuity
only.

No admitted `U` runtime contract survives for this label on the current branch.

The row remains a `U-attached` shell with these rules:

- polarity: `neutral`
- local meaning:
  - theater descriptor only
  - queenside-theater wording only
  - not an exact-board structural, tactical, or evaluative witness
- code freeze:
  - the row is listed in the shell-only attached set
  - no attached runtime registration is admitted

The review conclusion is:

- no certified `queenside` object family exists in the rewrite north-star
- the only nearby rewrite mention is a fixed-target leak example, not a
  canonical owner
- `opposite-side castling/wing asymmetry`, castling-provenance, and
  `king attack` rows remain distinct and must not collapse into bare
  `queenside`
- any canonical assignment to `SpaceClamp`, `FixedTargetComplex`,
  `TensionState`, or a queenside-specific upper family would be overfit

Negative boundary:

- openness meaning
- closure meaning
- clamp / break / initiative meaning
- treating the label as an exact-board witness
- treating `queenside` as `SpaceClamp`
- treating `queenside` as `FixedTargetComplex`
- treating `queenside` as `TensionState`
- leaking into `opposite-side castling/wing asymmetry`
- leaking into castling-provenance or `wing_asymmetry_state`
- leaking into `king attack`

### Whole-Board Theater Shell Classification

The historical inventory label `whole-board` survives for `61`-table
continuity only.

No admitted `U` runtime contract survives for this label on the current branch.

The row remains a `U-attached` shell with these rules:

- polarity: `neutral`
- local meaning:
  - theater descriptor only
  - whole-board wording only
  - not an exact-board structural, tactical, or evaluative witness
- code freeze:
  - the row is listed in the shell-only attached set
  - no attached runtime registration is admitted

The review conclusion is:

- no certified `whole-board` object family exists in the rewrite north-star
- broad whole-board access-shadow aggregates are not canonical conversion state
- local scope may not widen into whole-board language without a later dedicated
  matrix
- the row remains a neutral theater shell, not a canonical object family

Negative boundary:

- openness meaning
- closure meaning
- clamp / break / initiative meaning
- broad entry/channel/exit unions
- trade-only loops
- access-shadow aggregate meaning
- widening local scope into whole-board claim
- treating the label as an exact-board witness
- treating `whole-board` as `SpaceClamp`
- treating `whole-board` as `FixedTargetComplex`
- treating `whole-board` as `TensionState`

### Open/Semi-Open File Narrowing

The historical inventory label `open/semi-open file` survives for `61`-table
continuity only.

The canonical runtime contract id is `file_lane_state`.

`file_lane_state` remains `U-primary` with these rules:

- primary anchor: `file`
- polarity: `neutral`
- local meaning:
  - pure structural file-state substrate only
  - no pressure, penetration, access-lane, king-theater, counterplay, or
    initiative meaning

The contract is variant-scoped rather than root-conjunctive.

The active variants are:

- `open_file_state`
- `semi_open_file_state`

Variant admission is defined from `R` only:

```text
open_file_state(file) iff
  open_file(file)

semi_open_file_state(file) iff
  exists unique color c such that
    half_open_file(c, file)
```

Payload is restricted to:

- `file`
- `state`
  - `open`
  - `semi_open`
- `open_for_color`
  - only for `semi_open_file_state`

Runtime identifiers such as:

- `open_file`
- `semi_open_file`

are not used as active witness ids on this branch because the row survives as
one neutral file-state substrate with explicit variants.

Forbidden meaning:

- `rook on open file`
- pressure or penetration
- access-lane or king-theater meaning
- counterplay or initiative meaning
- any direct usefulness claim for one side without a downstream host/object

Negative boundary:

- `rook on open file` only
- file penetration only
- king-entry axis only
- wrong-wing semi-open file overstated as active pressure

### Rook On Open File Narrowing

The historical inventory label `rook on open file` survives for `61`-table
continuity only.

The canonical runtime contract id is `rook_on_open_file_state`.

`rook_on_open_file_state` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- polarity: `owner`
- local meaning:
  - a friendly rook occupies a square on an open file
  - no pressure, penetration, access-lane, king-theater, or initiative meaning

The contract is defined from `R` plus exact square-file binding only:

```text
rook_square_on_file(rook_square, file) :=
  rook_square.file == file
```

Presence requires:

- `open_file(file)`
- `piece_on(owner, rook, rook_square)`
- `rook_square_on_file(rook_square, file)`

Payload is restricted to:

- `rook_square`
- `file`

Forbidden meaning:

- pressure or penetration
- usefulness inference
- attack or initiative wording
- king-theater wording
- semi-open-file substitution

Negative boundary:

- semi-open file only
- file pressure only
- file penetration only
- king-entry axis only
- active-rook wording without a downstream host/object

### Bishop Pair Narrowing

The historical inventory label `bishop pair` survives for `61`-table
continuity only.

The canonical runtime contract id is `bishop_pair_state`.

`bishop_pair_state` remains `U-primary` with these rules:

- primary anchor: `board`
- polarity: `owner`
- local meaning:
  - the owner has at least two bishops
  - no color-complex, mobility, attack, king-theater, or evaluative meaning

Presence requires:

- there exist at least two distinct owner bishops

Payload is restricted to:

- `bishop_member_squares`

Forbidden meaning:

- open-position utility
- color-complex dominance
- long-term bishop strength
- mobility or attack requirement
- initiative wording
- king-safety wording
- exact-two-only admission
- direct use of evaluative former-`R4` support such as
  `favorable_minor_piece_relation`

Negative boundary:

- bishop activity only
- diagonal lane only
- color-complex only
- attack scaffold only
- superiority wording without downstream object/certification

### Knight Outpost Narrowing

The historical inventory label `knight outpost` survives for `61`-table
continuity only.

The canonical runtime contract id is `knight_on_outpost_square`.

`knight_on_outpost_square` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- polarity: `owner`
- local meaning:
  - an owner knight occupies a square that is already certified as
    `outpost_square(owner, square)`
  - no strategic value, domination, pressure, or clamp meaning

Presence requires:

- `piece_on(owner, knight, knight_square)`
- `outpost_square(owner, knight_square)`

Payload is restricted to:

- `knight_square`

Forbidden meaning:

- advanced-knight-only admission
- weak-square-only admission
- strategic value wording
- attack or pressure wording
- king-theater wording
- initiative wording
- direct use of evaluative former-`R4` support such as
  `favorable_minor_piece_relation`

Negative boundary:

- knight activity only
- square target only
- good-knight wording
- domination or clamp wording
- superiority wording without downstream object/certification

### Weak Square/Outpost Narrowing

The historical inventory label `weak square/outpost` survives for `61`-table
continuity only.

The canonical runtime contract id is `weak_outpost_square_state`.

`weak_outpost_square_state` remains `U-primary` with these rules:

- primary anchor: `square`
- polarity: `beneficiary`
- local meaning:
  - a beneficiary-facing square target state only
  - no owner occupancy, strategic value, pressure, king-theater, or upper-layer
    meaning

The contract is variant-priority rather than slash-bundled.

The active variants are:

- `outpost_square_state`
- `weak_square_state`

Variant admission is defined from `R` only:

```text
outpost_square_state(square) iff
  outpost_square(beneficiary, square)

weak_square_state(square) iff
  weak_square(beneficiary, square)
  and not outpost_square(beneficiary, square)
```

Payload is restricted to:

- `square`
- `state`
  - `outpost`
  - `weak`

The `outpost` variant has priority over the residual `weak` variant so the same
square is not emitted twice under one family.

Forbidden meaning:

- owner occupancy as admission criterion
- knight requirement as admission criterion
- strategic value wording
- attack or pressure wording
- king-theater wording
- initiative wording
- direct use of evaluative former-`R4` support such as
  `favorable_minor_piece_relation`

Negative boundary:

- weak square mislabeled as outpost
- outpost square double-counted as a separate weak-square witness
- `knight_on_outpost_square` restated as square-target witness
- good-post, domination, or clamp wording
- superiority wording without downstream object/certification

### Weak Pawn Narrowing

The historical inventory label `weak pawn` survives for `61`-table continuity
only.

The canonical runtime contract id is `weak_pawn_target_state`.

`weak_pawn_target_state` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- polarity: `beneficiary`
- local meaning:
  - a beneficiary-facing local weak-pawn target class only
  - no fixation, attack plan, conversion, or broader pawn-quality meaning

The contract uses explicit owner/target binding:

- `defender := owner of the pawn on the anchor square`
- `beneficiary := not defender`

Admission is defined from `R` only:

```text
weak_pawn_liability_basis(square) :=
  fixed_pawn(defender, square)
  or backward_pawn(defender, square)
  or isolated_pawn(defender, square)

weak_pawn_target_state(square) iff
  piece_on(defender, pawn, square)
  and weak_pawn_liability_basis(square)
```

Payload is restricted to:

- `square`
- `weakness_tags`
  - `fixed`
  - `backward`
  - `isolated`

Forbidden meaning:

- passed-pawn wording
- candidate-passer wording
- square-quality wording
- fixation wording
- attack-plan wording
- conversion wording
- initiative wording
- king-safety wording

Negative boundary:

- passed pawn only
- candidate passer only
- weak square only
- outpost square only
- pawn-quality projection above the exact target class

### Passed Pawn Narrowing

The historical inventory label `passed pawn` survives for `61`-table continuity
only.

The canonical runtime contract id is `passed_pawn_entity_state`.

`passed_pawn_entity_state` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- polarity: `owner`
- local meaning:
  - an owner-side exact-board passed-pawn entity state only
  - no promotion-readiness, conversion, route, or broader passer-play meaning

The contract uses explicit owner binding:

- `owner := owner of the pawn on the anchor square`

Admission is defined from `R` only:

```text
passed_pawn_entity_state(square) iff
  piece_on(owner, pawn, square)
  and passed_pawn(owner, square)
```

Payload is restricted to:

- `square`
- `owner`

Forbidden meaning:

- candidate-passer wording
- promotion-readiness wording
- conversion wording
- route/access wording
- passer-race wording
- weak-pawn wording
- initiative wording
- king-safety wording

Negative boundary:

- candidate_passer only
- create passer only
- promotion/passer upper-layer or review-only support meaning only
- PasserComplex or broader passer-play meaning above `U`
- weak-pawn liability wording

### Promotion/Passer Reclassification

The historical inventory label `promotion/passer` survives for `61`-table
continuity only.

No admitted `U` runtime contract survives for this label on the current branch.

The review conclusion is:

- exact passed-pawn entity truth stays in `passed_pawn_entity_state`
- not-yet-passed support truth stays in root `candidate_passer(c, s)`
- any broad passer conversion, promotion corridor, or passer-play meaning
  belongs above `U` inside `PasserComplex`, `ConversionFunnel`, or
  `promotion race`

The explored `U-attached` host-tag draft failed review because it did not own a
new exact-board admission and remained too vacuous to count as admitted runtime
ownership.

Negative boundary:

- `passed_pawn_entity_state` only
- `candidate_passer(c, s)` only
- `create passer` only
- `promotion race` only
- any review-only support/render label presented as admitted runtime ownership

### Structural Damage Shell Classification

The historical inventory label `structural damage` survives for `61`-table
continuity only.

No active runtime contract survives for this label on the current branch.

The row remains a `U-attached` host-scoped objective shell only.

Code freeze:

- the row is listed in the shell-only attached set
- no attached runtime registration is admitted

The review conclusion is:

- exact lower support stays in structural-cause roots such as:
  - `isolated_pawn(c, s)`
  - `backward_pawn(c, s)`
  - `fixed_pawn(c, s)`
  - `doubled_file(c, f)`
- related lower witness examples may include `weak_pawn_target_state`
- all such support is illustrative, non-exhaustive, and non-conjunctive
- the row must not absorb lower witnesses into a required bundle
- broad structural-damage wording remains objective/surface vocabulary, not a
  new exact-board admission

Negative boundary:

- `isolated_pawn` only
- `backward_pawn` only
- `fixed_pawn` only
- `doubled_file` only
- `weak_pawn_target_state` only
- any broad structural verdict presented as lower-layer truth

### Material Gain Shell Classification

The historical inventory label `material gain` survives for `61`-table
continuity only.

No active runtime contract survives for this label on the current branch.

The row remains a `U-attached` host-scoped objective shell only.

Code freeze:

- the row is listed in the shell-only attached set
- no attached runtime registration is admitted

The review conclusion is:

- the row is surface/projection vocabulary only
- it must not be used as a truth-owning material-swing predicate
- lower examples, if named, are illustrative consequence motifs only
- such examples are non-exhaustive, non-conjunctive, and not polarity proof
- realized conversion/result meaning belongs above `U` in `material harvest`
  or `winning endgame`

Negative boundary:

- `loose_piece_target_state` only
- `exchange defender` only
- any single U9 tactic only
- `material harvest` only
- `winning endgame` only
- any broad material-gain verdict presented as lower-layer truth

### Draw/Hold Reclassification

The historical inventory label `draw/hold` survives for `61`-table continuity
only.

No admitted `U` runtime contract survives for this label on the current branch.

The review conclusion is:

- the label is legacy inventory wording only at the descriptor surface
- it is not itself a `U` runtime contract or standalone object runtime id
- `FortressHoldingShell` is the canonical object-layer semantic home for any
  surviving hold-side meaning
- `TradeInvariant` remains a related but distinct simplification lane
- `neutralization / consolidation / fortress holding` remains a projection band
  only
- `simplify`, `perpetual/fortress`, and `winning endgame` remain related but
  distinct rows and must not collapse into this label
- `favorable_simplification` remains a dissolved legacy path and does not
  reopen `draw/hold` as an admitted runtime owner

Object-side runtime contract:

- helper `fortress_entry_denial_shell`
- requires a king-centered shell mask on the holder king file plus adjacent
  files, at least two friendly occupied shell squares, no current attacker
  occupancy or current file/diagonal entry axis into that mask, no same-file
  attacker rook/queen with live attack geometry into the shell squares, and
  blocker presence on any same-file or adjacent attacker passer lane
- `TradeInvariant` and `perpetual/fortress` stay outside direct object
  admission
- runtime status: implemented in
  `modules/commentary/src/main/scala/lila/commentary/strategic/FortressHoldingShellRule.scala`
  and verified by `FortressHoldingShellRuleTest` plus
  `StrategicObjectCorpusRuntimeTest`

Negative boundary:

- bare material balance
- generic endgame
- `simplify` row alone
- bounded favorable simplification alone
- `TradeInvariant` alone
- `winning endgame` verdict alone
- `perpetual/fortress` verdict alone
- bare shell-shaped picture without certified denied-entry/blockade
- any mixed predicate collapsing simplify, hold shell, and result outcome

### King Attack Reclassification

The historical inventory label `king attack` survives for `61`-table continuity
only.

No admitted `U` runtime contract survives for this label on the current branch.

The review conclusion is:

- the label is legacy inventory wording only at the descriptor surface
- it is not itself a `U` runtime contract or standalone object runtime id
- `AttackScaffold` is the canonical object-layer semantic home
- `direct piece concentration king attack` and `color-complex king attack`
  remain projection bands only
- king-theater-linked file/diagonal access geometry remains documentation
  shorthand only, while `file_lane_state`, `diagonal_lane_only`, and
  `rook_on_open_file_state` remain the live lower carriers
- `certified_king_safety_edge`, `comparative_king_fragility`, `initiative`, and
  `mate net` remain related but distinct upper rows
- `king attack` never reopens as a `U-attached` host shell or self-certifying
  proof of a king-safety edge

Object-side runtime contract:

- helper `attack_host_core`
- requires at least one king-theater-linked carrier fragment and one distinct
  king-theater-linked vulnerability/support fragment on the same defending king
- a shelter-hole-only support picture still needs a second distinct carrier
  fragment; lone local diagonal/file pressure plus holes stays negative-first
- carrier-only pressure never self-certifies attack ownership
- runtime status: implemented in
  `modules/commentary/src/main/scala/lila/commentary/strategic/AttackScaffoldRule.scala`
  and verified by `AttackScaffoldRuleTest` plus
  `StrategicObjectCorpusRuntimeTest`

Negative boundary:

- attack-map pressure only
- king-theater-linked file or diagonal access geometry alone
- `rook_on_open_file_state` alone
- local file or diagonal pressure away from the king theater
- any claim that `king attack` alone proves initiative or king-safety
  superiority

### Simplify Reclassification

The historical inventory label `simplify` survives for `61`-table continuity
only.

No admitted `U` runtime contract survives for this label on the current branch.

The review conclusion is:

- the label is legacy surface transformation wording only
- `TradeInvariant` is the canonical delta-layer semantic home
- bounded favorable simplification remains a same-task move-local slice only
- `favorable simplification` remains a projection band only
- `draw/hold`, `winning endgame`, `perpetual/fortress`, `material gain`, and
  `promotion/passer` remain related but distinct rows and must not collapse into
  this label
- `favorable_simplification` remains a dissolved legacy path and does not reopen
  `simplify` as an admitted runtime owner

Delta-side start contract:

- scope: `move_local` only
- primary anchor: `board`
- runtime lives under
  `modules/commentary/src/main/scala/lila/commentary/delta`
- `StrategicDeltaRuntime` registers `TradeCompressionCorridor` before
  `TradeInvariant`
- public `CommentaryCore` exposes `activeDeltaFamilyIds`,
  `extractStrategicDeltas(...)` overloads from object extractions and from
  before/after `Fen` plus `playedMove`, and fail-closed delta extraction
  overloads
- helper `bounded_material_reduction`
- helper `persistent_object_carrier`
- helper `trade_invariant_transition`
  - the first live slice requires:
    - a board-coherent non-king capture on the played move
  - total non-king non-pawn material count drops by exactly `1`
  - one same-family same-anchor object persists from before-board to
    after-board
  - the mover-side clear-run carrier must stay continuous across the move:
    - either the same clear runner remains on the same square
    - or the moving pawn itself remains the clear runner on its destination
- the current-worktree first live slice admits only
  `EndgameRaceScaffold` persistence on the `board` anchor with mover-side
  clear-run carrier continuity
- `FortressHoldingShell`, `AttackScaffold`, and `KingSafetyShell`
  generalization remain deferred until they carry separate delta corpus rows
- generic favorable exchange, task-switch creation, and upper result wording
  stay negative only
- runtime status: live; `TradeCompressionCorridorRuleTest`,
  `TradeInvariantRuleTest`, `StrategicDeltaBoundaryTest`,
  `DeltaExpectationCorpusTest`, and `CommentaryCoreBoundaryTest` are live
- `DeltaExpectationCorpusTest` now asserts live runtime extraction against the
  delta corpus rows

Negative boundary:

- favorable exchange only
- generic trade-down only
- generic liquidation only
- non-bounded or non-centralized trade semantics
- `TradeInvariant` objects outside the certified bounded favorable-simplification
  slice
- `draw/hold` row alone
- `winning endgame` verdict alone
- `perpetual/fortress` verdict alone
- `material gain` shell alone
- `promotion/passer` row alone

### Open Line Shell Classification

The historical inventory label `open line` survives for `61`-table continuity
only.

No admitted `U` runtime contract survives for this label on the current branch.

The row remains a `U-attached` shell with these rules:

- attachment mode: `host-scoped`
- polarity: `host`
- local meaning:
  - transformation descriptor only
  - host-scoped line-opening wording only
  - not an exact-board lane, access, pressure, attack, or simplification
    witness
- code freeze:
  - the row is listed in the shell-only attached set
  - no attached runtime registration is admitted

Certified lower support remains outside this row:

- `open_file_state`
- `semi_open_file_state`
- `rook_on_open_file_state`
- `diagonal_lane_only`

The review conclusion is:

- exact geometry stays on narrower file, ray, and king-entry lanes
- promoting `open line` to a witness would alias line-opening with already
  owned substrate
- the shell must not invent certified hosts or a runtime witness id
- attack-side and simplification-side ownership remain above this row

Negative boundary:

- `open_file_state` alone
- `semi_open_file_state` alone
- `rook_on_open_file_state` alone
- king-theater-linked file or diagonal access geometry alone
- open-file pressure only
- diagonal access only
- attack ownership
- simplification ownership

### Create Passer Shell Classification

The historical inventory label `create passer` survives for `61`-table
continuity only.

No admitted `U` runtime contract survives for this label on the current branch.

The row remains a `U-attached` shell with these rules:

- attachment mode: `host-scoped`
- polarity: `host`
- local meaning:
  - transformation descriptor only
  - host-scoped passer-creation wording only
  - not an exact-board support, entity, conversion, or result witness
- code freeze:
  - the row is listed in the shell-only attached set
  - no attached runtime registration is admitted

Exact lower support remains outside this row:

- `candidate_passer(c, s)`

Related upper projection remains outside this row:

- `promotion/passer`

The review conclusion is:

- exact passer support truth stays in `candidate_passer(c, s)`
- `passed_pawn_entity_state` is downstream entity truth and is not lower truth
  for this row
- broader passer conversion meaning remains above `U` inside
  `PasserComplex`, `ConversionFunnel`, or `promotion race`
- promoting `create passer` to a witness would collapse support, entity, and
  conversion lanes

Negative boundary:

- `candidate_passer(c, s)` alone
- `passed_pawn_entity_state` alone
- `promotion/passer` row alone
- passer conversion meaning
- route/access meaning
- result meaning

### Exchange Defender Reclassification

The historical inventory label `exchange defender` survives for `61`-table
continuity only.

No admitted `U` runtime contract survives for this label on the current branch.

The review conclusion is:

- the label is legacy surface transformation wording only
- no distinct exact-board discriminator survives for this row
- `exchange square` and `defended resource` remain related primitive seeds only
- `TradeInvariant` and `DefenderDependencyNetwork` remain related but distinct
  upper families and must not be collapsed into one canonical owner for this
  label
- narrower defender-duty and tactical removal semantics remain on distinct rows
  such as `duty_bound_defender`, `overload`, `deflection/decoy`, and
  `demolition/undermining`
- `material gain` and `simplify` remain related but distinct upper rows and
  must not collapse into `exchange defender`

Negative boundary:

- `exchange square` alone
- `defended resource` alone
- `duty_bound_defender` alone
- `overload` alone
- `deflection/decoy` alone
- `demolition/undermining` alone
- generic exchange only
- `material gain` shell alone
- `material harvest` verdict alone
- `winning endgame` verdict alone

### Loose/Overloaded Piece Narrowing

The historical inventory label `loose/overloaded piece` survives for `61`-table
continuity only.

The canonical runtime contract id is `loose_piece_target_state`.

`loose_piece_target_state` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- polarity: `beneficiary`
- local meaning:
  - a beneficiary-facing local tactical target only
  - the anchored enemy piece is immediately exchange-losing on its current
    square
  - no overload, dependency, conversion, or initiative meaning

The contract is defined from `R` only:

```text
loose_piece_target_state(square) iff
  loose_piece(defender, square)

defender :=
  owner of the loose piece on the anchor square

beneficiary :=
  not defender
```

Payload is restricted to:

- `square`

Forbidden meaning:

- `overloaded_piece` as admission criterion
- overload-style dependency wording
- defender-duty wording
- pin/trap coupling
- attack-strength wording
- king-theater wording
- initiative wording
- raw attacker/defender-count heuristics

Negative boundary:

- overloaded but not loose defender
- `duty_bound_defender` only
- `U9 overload` tactic only
- pinned or trapped defender with no immediate exchange loss
- generic tactical target wording with no `loose_piece` root

### Bad Piece Reclassification

The historical inventory label `bad piece` survives for `61`-table continuity
only.

There is no active `U` runtime contract for this label on the current branch.

The exact-board liabilities that can feed `bad piece` meaning remain below `U`
in narrower witnesses or roots. Illustrative support material includes:

- `loose_piece_target_state`
- `duty_bound_defender`
- `weak_outpost_square_state`
- `knight_on_outpost_square`
- `pinned_piece`
- `trapped_piece`

This support list is illustrative, non-exhaustive, and non-conjunctive. It does
not define a required bundle.

Future `S17` lower support is frozen in `StrategySupportSeedInventory.md`;
exact band blocker/status is owned by `StrategyProjectionBoundaryMatrix.md`.

Upper-layer owner families include:

- `improve_worst_piece`
- `minor_piece_liability`
- `favorable_minor_piece_relation` support only

These labels remain documentation vocabulary only on the current branch.

None is a live `src/main` runtime contract id.

Runtime identifiers such as:

- `bad_piece`
- `bad_bishop`
- `bad_knight`
- `poor_piece_state`

are not valid active `U` witness ids on this branch.

Why no active `U` contract survives:

- no clean exact root discriminator exists for generic piece-quality meaning
- broad `bad piece` meaning is evaluative
- existing exact failure modes already live in narrower witnesses
- generic bad-piece wording over-admits blocked-but-fine or
  undeveloped-but-functional pieces

Negative boundary:

- blocked bishop only
- undeveloped knight only
- loose piece only
- pinned piece only
- trapped piece only
- weak-square relation only
- any generic piece-quality wording without upper-layer composition

### Improve Worst Piece Reclassification

The historical inventory label `improve worst piece` survives for `61`-table
continuity only.

No admitted `U` runtime contract survives for this label on the current branch.

The review conclusion is:

- the label is legacy piece-quality projection wording only
- exact liabilities remain below `U` in narrower witnesses such as
  `loose_piece_target_state`, `weak_outpost_square_state`, and
  `duty_bound_defender`
- support-only exact failure material may also include `pinned_piece` and
  `trapped_piece`
- future `S17` lower support is frozen in `StrategySupportSeedInventory.md`;
  exact band blocker/status is owned by `StrategyProjectionBoundaryMatrix.md`
- broad improvement meaning belongs above `U`, alongside
  `improve_worst_piece` and `minor_piece_liability`
- `favorable_minor_piece_relation(c)` remains broader upstream support and is
  not a lower witness for this row
- those labels remain documentation vocabulary only on the current branch,
  not live `src/main` runtime contract ids
- keeping the label in `U` would collapse liability, value, and
  redeployment-like improvement meaning into one surface bucket

Negative boundary:

- `loose_piece_target_state` alone
- `weak_outpost_square_state` alone
- `duty_bound_defender` alone
- `pinned_piece` alone
- `trapped_piece` alone
- generic bad-piece wording alone
- route/redeployment/access meaning as lower-layer truth
- overload/pressure/initiative wording as witness truth

### Key File/Rank Reclassification

The historical inventory label `key file/rank` survives for `61`-table
continuity only.

There is no active `U` runtime contract for this label on the current branch.

The file-side meaning is absorbed by the existing `file_lane_state` family:

- `file_lane_state`
- `open_file_state`
- `semi_open_file_state`

The horizontal-rank side is deferred outside `U` as:

- `horizontal_rank_access`

No runtime identifiers such as:

- `key_file`
- `key_rank`
- `key_file_rank`

are valid active `U` witness ids on this branch.

Why no active `U` contract survives:

- the legacy row mixes two anchor families:
  - `file`
  - horizontal `ray`
- `key` wording imports usefulness rather than exact-board witness meaning
- the file-side truth is already covered by the neutral substrate family
  `file_lane_state`
- the horizontal-rank side drifts into access/route meaning rather than a clean
  single-anchor `U-primary` witness

Negative boundary:

- a quiet open file restated as `key`
- a horizontal rank corridor restated as file substrate
- access/route usefulness released as a raw `U` witness

### Diagonal/Color Complex Narrowing

The historical inventory label `diagonal/color complex` survives for
`61`-table continuity only.

The canonical runtime contract id is `diagonal_lane_only`.

`color_complex_only` is deferred outside `U` on the current branch.

Runtime identifiers such as:

- `diagonal_color_complex`
- `color_complex`
- `color_complex_only`

are not valid active `U` witness ids on this branch.

`diagonal_lane_only` remains `U-primary` with these rules:

- primary anchor: `ray`
  - `source square + one diagonal direction`
- polarity: `neutral`
- local meaning:
  - one exact bishop/queen diagonal projection exists as geometry only

The contract is defined from `R` only:

```text
occupied(square) :=
  exists piece_on(_, _, square)

diagonal_source(source_square) :=
  exists color such that
    piece_on(color, bishop, source_square)
    or piece_on(color, queen, source_square)

same_diagonal(source_square, target_square) :=
  source_square and target_square lie on one exact diagonal

clear_diagonal_segment(source_square, target_square) :=
  every square strictly between source_square and target_square is unoccupied

diagonal_lane_endpoint(source_square, target_square) :=
  diagonal_source(source_square)
  and same_diagonal(source_square, target_square)
  and clear_diagonal_segment(source_square, target_square)
  and controlled_by(color_of(source_square), target_square)

diagonal_lane(ray) :=
  an ordered non-empty maximal diagonal segment reconstructed from
  one source square and one diagonal direction
```

Presence requires:

- there exists `diagonal_lane(ray)`
- projected lane length `>= 2`

Negative boundary:

- same-color weak-square cluster with no one exact shared diagonal
- king-side color weakness with no exact diagonal lane
- queen-bishop carrier alignment restated as neutral lane
- king-entry/access claim
- bishop-quality or domination wording

Payload is restricted to:

- `ray`
- `source_piece_squares`
- `endpoint_squares`
- optional `blocker_square`

The payload must not embed or restate parallel witnesses or deferred seeds such as:

- `qb_diagonal_alignment_seed`
- king-theater-linked file/diagonal access geometry
- `weak square/outpost`
- `king shelter` / `KingSafetyShell` shelter-shell meaning
- legacy `key file/rank` label

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
- legacy `rook lift` / redeployment-route meaning
- `duty_bound_defender`

### Pin Narrowing

The historical inventory label `pin` survives for `61`-table continuity only.

The canonical runtime contract id is `pin`.

Runtime identifiers such as:

- `pin_ray_relation`
- `pin_line_relation`
- `pin_geometry`

are not valid witness ids on this branch.

`pin` remains `U-primary` with these rules:

- primary anchor: `ray`
- polarity: `beneficiary`
- local meaning:
  - beneficiary-side exact-board pin witness only
  - attacker slider, pinned blocker, and hidden anchor must lie on one exact
    ray
  - the blocker must be the sole blocker on that ray; moving it concedes the
    line to the king or a more valuable friendly anchor
  - no generic line pressure, throttling, duty, attack, conversion,
    `xray_target`, or skewer-only meaning

The contract is defined from `R` only:

```text
pin(ray) iff
  exists attacker_square, blocker_square, anchor_square such that
    piece_on(beneficiary, slider_role, attacker_square)
    and pinned_piece(defender, blocker_square)
    and attacker_square, blocker_square, and anchor_square lie on ray
    and blocker_square is the sole blocker between attacker_square and
        anchor_square
    and moving the blocker concedes the line to the king or to a more valuable
        friendly anchor
```

Payload is restricted to:

- `ray`
- `attacker_square`
- `blocker_square`
- `anchor_square`
- `pin_mode`
  - `absolute_king_pin`
  - `relative_anchor_pin`

Negative boundary:

- `pinned_piece` only
- `xray_target` only
- `duty_bound_defender` only
- `short_run_slider_gate_restriction` only
- `restriction geometry` only
- skewer-like ray geometry without pinned-blocker concession
- generic line pressure only
- king attack, initiative, or conversion wording

### Fork Narrowing

The historical inventory label `fork` survives for `61`-table continuity only.

The canonical runtime contract id is `fork`.

Runtime identifiers such as:

- `fork_attack_fan`
- `fork_geometry`
- `real_fork_gate`

are not valid witness ids on this branch.

`fork` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- polarity: `beneficiary`
- local meaning:
  - beneficiary-side exact-board fork witness only
  - one beneficiary piece on the anchor square attacks two or more distinct
    enemy occupied squares from the current board
  - no overload, defender-duty, lure, pressure-scoring, initiative,
    conversion, or king-safety meaning

The contract is defined from `R` only:

```text
fork(anchor_square) iff
  piece_on(beneficiary, attacker_role, anchor_square)
  and there exists a same-length aligned payload of target_squares and
      target_roles such that
      the list contains at least two targets
      and for every i,
        piece_on(defender, target_roles[i], target_squares[i])
        and the anchored attacker attacks target_squares[i] on the current
            board
      and all target_squares[i] are pairwise distinct
```

Payload is restricted to:

- `attacker_role`
- `anchor_square`
- `target_squares`
- `target_roles`

Negative boundary:

- generic multi-attack scoring only
- overload or duty-bound-defender meaning
- loose-piece target meaning
- deflection/decoy meaning
- pin, skewer, or xray line geometry
- single-target pressure only
- value-filter-only or undefended-only heuristic gating
- king attack, mate net, winning capture, or conversion wording

### Skewer Narrowing

The historical inventory label `skewer` survives for `61`-table continuity
only.

The canonical runtime contract id is `skewer`.

Runtime identifiers such as:

- `skewer_ray_relation`
- `skewer_geometry`
- `ordered_xray_skewer`

are not valid witness ids on this branch.

`skewer` remains `U-primary` with these rules:

- primary anchor: `ray`
- polarity: `beneficiary`
- local meaning:
  - beneficiary-side exact-board skewer witness only
  - attacker slider must be a `bishop`, `rook`, or `queen`
  - attacker slider, front target, and rear target must lie on one exact ray
  - the front target is the sole blocker between attacker and rear target
  - the front target is a non-king piece that is strictly more valuable than
    the rear target
  - moving the front target off that ray exposes the rear target to the same
    ray attack
  - no generic line pressure, throttling, pin, `xray_target`, attack,
    conversion, initiative, or material-harvest meaning

The contract is defined from `R` only:

```text
skewer(ray) iff
  exists attacker_square, front_square, rear_square such that
    piece_on(beneficiary, slider_role, attacker_square)
    and piece_on(defender, front_piece_role, front_square)
    and piece_on(defender, rear_piece_role, rear_square)
    and slider_role in {bishop, rook, queen}
    and attacker_square, front_square, and rear_square lie on ray
    and front_square is the sole blocker between attacker_square and rear_square
    and front_piece_role != king
    and piece_value(front_piece_role) > piece_value(rear_piece_role)
    and moving the front target off that ray exposes the rear target to the
        same ray attack
```

Payload is restricted to:

- `ray`
- `attacker_square`
- `slider_role`
- `front_square`
- `rear_square`
- `front_piece_role`
- `rear_piece_role`

Negative boundary:

- `pin`
- `pinned_piece` only
- `duty_bound_defender` only
- `xray_target` only
- `short_run_slider_gate_restriction` only
- `restriction geometry` only
- generic line pressure only
- king-front skewer legacy detector case only
- king attack, initiative, conversion, or material-harvest wording

### Overload Narrowing

The historical inventory label `overload` survives for `61`-table continuity
only.

The canonical runtime contract id is `overload`.

Runtime identifiers such as:

- `overload_dependency`
- `critical_duty_bundle`
- `motif_overloading`

are not valid witness ids on this branch.

`overload` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- polarity: `beneficiary`
- local meaning:
  - beneficiary-side exact-board overload witness only
  - one anchored enemy non-king defender currently covers two or more distinct
    same-color non-king occupied squares from its present square
  - for each listed duty square, removing the anchor causes the remaining
    same-color defender count to fall below the opposing attacker count on that
    square
  - no bound-duty, lure, multi-attack, exchange, initiative, or conversion
    meaning

The contract is defined from `R` only:

```text
occupied_same_color_non_king(square) :=
  exists piece_on(defender, duty_piece_role, square)
  and duty_piece_role != king

covered_by_anchor(square) :=
  the defender piece on anchor_square currently attacks or line-covers square
  from its present square

under_defended_without_anchor(square) :=
  on the board with the piece at anchor_square removed,
  defender_attacker_count(square) < beneficiary_attacker_count(square)

overload(anchor_square) iff
  overloaded_piece(defender, anchor_square)
  and piece_on(defender, anchor_piece_role, anchor_square)
  and anchor_piece_role != king
  and there exists a pairwise-distinct payload of duty_squares such that
      duty_squares.size >= 2
      and for every duty_square in duty_squares,
        occupied_same_color_non_king(duty_square)
        and covered_by_anchor(duty_square)
        and under_defended_without_anchor(duty_square)
```

Payload is restricted to:

- `anchor_piece_role`
- `anchor_square`
- `duty_squares`

Negative boundary:

- `duty_bound_defender` only
- `fork` only
- `deflection/decoy` only
- `loose_piece_target_state` only
- `overloaded_piece` with fewer than two qualifying duty squares
- generic defensive dependency with no anchored current-duty multiplicity
- `Motif.Overloading` heuristic only
- exchange, initiative, or conversion wording

### Deflection/Decoy Reclassification

The historical inventory label `deflection/decoy` survives for `61`-table
continuity only.

There is no active `U` runtime contract for this row.

Runtime identifiers such as:

- `deflection`
- `decoy`
- `removing_the_defender`
- `deflection_decoy_relation`

are not valid witness ids on this branch.

The row is `projection-layer` only:

- legacy tactical forcing / lure label only
- no exact-board admission of its own
- not a standalone defender-duty witness, move-local fact, or conversion owner

Related heuristic motifs remain distinct:

- `Motif.Deflection`
- `Motif.Decoy`
- `Motif.RemovingTheDefender`

Why no active contract survives:

- current branch has no `RootAtoms` residue, `RootExtractor` admission path, or
  corpus contract for `deflection/decoy`
- baseline-head `detectDeflection`, `detectDecoy`, and
  `detectRemovingTheDefender` are move-local heuristics, not exact lower
  admission law
- none of these motifs descends into admitted `Fact` runtime
- the merged label collapses attacked-defender `from-square`,
  lure-destination `to-square`, and separate capture-removal route logic into
  one phrase

Surface rule:

- use only as optional wording for already certified tactical forcing or
  defensive-collapse relations
- do not use it as admission source, host owner, or exact lower witness id

Negative boundary:

- `overload` only
- `duty_bound_defender` only
- `fork` only
- `demolition/undermining` only
- `Motif.Deflection` heuristic only
- `Motif.Decoy` heuristic only
- `Motif.RemovingTheDefender` heuristic only
- generic tactical-collapse narration only

### Demolition/Undermining Reclassification

The historical inventory label `demolition/undermining` survives for `61`-table
continuity only.

There is no active `U` runtime contract for this row.

Runtime identifiers such as:

- `demolition`
- `undermining`
- `removing_the_defender`
- `demolition_undermining_relation`

are not valid witness ids on this branch.

The row is `projection-layer` only:

- legacy tactical removal / support-breaking label only
- no exact-board admission of its own
- not a standalone lower witness, move-local fact, or conversion owner

Related heuristic motifs remain distinct:

- `Motif.RemovingTheDefender`

Related relation/operator context:

- `overloads_or_undermines`

Why no active contract survives:

- current branch has no `RootAtoms` residue, `RootExtractor` admission path, or
  corpus contract for `demolition/undermining`
- baseline-head `detectRemovingTheDefender` is move-local heuristic logic, not
  exact lower admission law
- `RemovingTheDefender` does not descend into admitted `Fact` runtime
- north-star keeps undermining semantics in relation-operator space rather than
  a lower exact witness family
- the merged label remains broad removal/support-breaking wording rather than
  one certified current-board discriminator

Surface rule:

- use only as optional wording for already certified tactical removal or
  defensive-collapse relations
- do not use it as admission source, host owner, or exact lower witness id

Negative boundary:

- `overload` only
- `duty_bound_defender` only
- `fork` only
- `deflection/decoy` only
- `Motif.RemovingTheDefender` heuristic only
- generic tactical-collapse narration only

### Interference Reclassification

The historical inventory label `interference` survives for `61`-table
continuity only.

There is no active `U` runtime contract for this row.

Runtime identifiers such as:

- `interference`
- `line_interference`
- `interference_relation`

are not valid witness ids on this branch.

The row is `projection-layer` only:

- legacy tactical obstruction / line-disruption label only
- no exact-board admission of its own
- not a standalone lower witness, move-local fact, or conversion owner

Related heuristic motifs remain distinct:

- `Motif.Interference`

Why no active contract survives:

- `Witnesses61` still carries a legacy `U-primary` square/beneficiary row, but
  the current branch does not admit `interference` through `RootAtoms`,
  `RootAtomRegistry`, `RootExtractor`, or corpus contract
- baseline-head `detectInterference` is move-local heuristic logic, not exact
  lower admission law
- `Motif.Interference` does not descend into admitted `Fact` runtime
- the surviving motif payload keeps only blocker square and piece roles, not
  enough for one certified exact-board discriminator

Surface rule:

- use only as optional wording for already certified tactical obstruction or
  disruption relations
- do not use it as admission source, host owner, or exact lower witness id

Negative boundary:

- `overload` only
- `fork` only
- `clearance` only
- `deflection/decoy` only
- `demolition/undermining` only
- `Motif.Interference` heuristic only
- generic line-disruption narration only

### Clearance Reclassification

The historical inventory label `clearance` survives for `61`-table continuity
only.

There is no active `U` runtime contract for this row.

Runtime identifiers such as:

- `clearance`
- `line_clearance`
- `clearance_relation`

are not valid witness ids on this branch.

The row is `projection-layer` only:

- legacy tactical line-release label only
- no exact-board admission of its own
- not a standalone lower witness, move-local fact, or conversion owner

Related baseline-head motifs remain reference-only:

- `Motif.Clearance`

Why no active contract survives:

- `Witnesses61` still carries a legacy `U-primary` square/beneficiary row, but
  the current branch does not admit `clearance` through `RootAtoms`,
  `RootAtomRegistry`, `RootExtractor`, or corpus contract
- baseline-head `detectClearance` is move-local motif logic, not exact lower
  admission law on the current branch
- baseline-head `Motif.Clearance` does not descend into admitted `Fact`
  runtime on the current branch
- the surviving motif payload keeps only clearing piece role, origin square,
  line type, and beneficiary role, not enough for one certified exact-board
  discriminator
- the merged label spans a release family rather than one exact lower witness

Surface rule:

- use only as optional wording for already certified tactical line-release
  relations
- do not use it as admission source, host owner, or exact lower witness id

Negative boundary:

- `overload` only
- `fork` only
- `interference` only
- `deflection/decoy` only
- `demolition/undermining` only
- baseline-head `Motif.Clearance` only
- generic line-opening narration only

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
    or xray_target(beneficiary, square)
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

### Open Center Reclassification

The historical inventory label `open center` survives for `61`-table
continuity only.

There is no active `U` runtime contract for this label on the current branch.

The row is projection-only above `U`.

Illustrative lower support material includes:

- `file_lane_state` on central files
- `fixed_pawn`
- `central tension`
- `available_lever_trigger`

This lower support list is illustrative, non-exhaustive, and non-conjunctive.
It does not define a required bundle.

The row leaves `U` because:

- the branch does not yet have one clean exact-board discriminator for broad
  central openness
- file-centric slices such as `open_file(d/e)` over-cover some cases and
  under-cover others
- broad central-openness meaning drifts into central break, initiative, and
  king-safety projection if treated as a raw witness

Current narrow projection handoff stays above `U`:

- `S05` may consume exact central release bundles
- `S21` may consume an exact central `pawn_push_break_contact_source` only when
  the same board also carries certified `InitiativeWindow`
- openness wording alone never admits either band

Runtime identifiers such as:

- `open_center`
- `open_center_state`
- `central_file_openness_state`

are not valid active `U` witness ids on this branch.

Negative boundary:

- central tension only
- fixed central barrier only
- central file openness restated as full open-center meaning
- central break or initiative inferred from openness alone

### Closed Center Host-Shell Reclassification

The historical inventory label `closed center` remains in the `61` table as a
`U-attached` host shell on the current branch.

No standalone `U-primary` runtime contract survives for this row.

The current branch rejected that proposal because:

- the proposed mixed-color `8`-connected barrier topology was too
  motif-specific
- it risked under-firing on ordinary closed-center positions
- it behaved more like one special central lock pattern than a stable exact
  `closed center` slice

Runtime identifiers such as:

- `closed_center_barrier_state`

are not valid active witness ids on the current branch.

Current boundary status:

- the row survives only as neutral host vocabulary for `structural_space_claim`
- it is no longer counted as a `U-primary` blocker
- it must not be widened into clamp value, break denial, initiative, or
  king-safety meaning
- no standalone exact `closed center` witness survives in `U`

### Fixed Chain Host-Shell Reclassification

The historical inventory label `fixed chain` remains in the `61` table as a
`U-attached` host shell on the current branch.

The reviewed narrowing proposal `fixed_chain_state` is not admitted as an
active runtime contract.

The current branch rejected that proposal because:

- the proposed connected-component chain topology was too loose for a true
  chess chain and admitted branched fixed-pawn clusters
- it reconstructed a richer chain topology than current roots cleanly certify
- no cleaner exact-board chain slice is yet certified as an active runtime
  contract
- it risked drifting from a neutral sector trigger into pawn-plan semantics

Runtime identifiers such as:

- `fixed_chain_state`

are not valid active witness ids on the current branch.

Current boundary status:

- the row survives only as neutral host vocabulary for `structural_space_claim`
- it is no longer counted as a `U-primary` blocker
- it must not be widened into clamp value, break denial, initiative, or
  king-safety meaning
- it must not be widened into pawn-plan meaning
- no standalone exact `fixed chain` witness survives in `U`

### Central Tension Continuity Rehome

The historical inventory label `central tension` remains in the `61` table as
an object-side continuity row on the current branch.

The reviewed narrowing proposal `central_pawn_contact_state` is not admitted as
an active runtime contract.

The current branch rejected that proposal because:

- the proposed mutual pawn-contact admission was too pawn-specific for the
  broader `central tension` label
- it risked under-firing on genuine central contact states that are contested
  without a strict bidirectional pawn-vs-pawn pair
- it narrowed the row into one contact motif rather than a stable exact
  central-tension slice

Runtime identifiers such as:

- `central_pawn_contact_state`

are not valid active witness ids on the current branch.

Current boundary status:

- no standalone `U-primary` witness survives for this row
- object-side continuity now points to `CentralContactFront`
- unlike `closed center` and `fixed chain`, it is not host vocabulary for
  `structural_space_claim`
- it must not be widened into openness, closure, break, space, initiative,
  king-safety, or `TensionState` meaning
- object admission now stays on frozen `central_sector_mask` and
  `front_connectivity` helpers

Object-side runtime contract:

- helper `central_contact_front_state`
- requires one connected central contact component with at least two squares,
  at least one contested square, at least one occupied contact square, and
  contribution from both colors inside the same front
- if multiple disconnected qualifying central fronts exist, runtime keeps one
  canonical strongest component rather than merging them under one center
  identity
- `open center`, `closed center`, `fixed chain`, initiative, and king-safety
  wording remain outside this row
- runtime status: implemented in
  `modules/commentary/src/main/scala/lila/commentary/strategic/CentralContactFrontRule.scala`
  and verified by `CentralContactFrontRuleTest` plus
  `StrategicObjectCorpusRuntimeTest`

### Majority/Minority Asymmetry Narrowing

The historical inventory label `majority/minority asymmetry` survives for
`61`-table continuity only.

The canonical runtime contract id is `sector_asymmetry_state`.

`sector_asymmetry_state` remains `U-primary` with these rules:

- primary anchor: `sector`
- polarity: `neutral`
- local meaning:
  - a raw sector-level pawn-count imbalance only
  - no plan, value, route, or attack meaning

The contract is defined from `R` only:

```text
sector_pawn_count(color, sector) :=
  number of squares in sector occupied by color pawns

majority_side(sector) :=
  the unique color c such that
  sector_pawn_count(c, sector) > sector_pawn_count(!c, sector)

minority_side(sector) :=
  the unique color c such that
  sector_pawn_count(c, sector) < sector_pawn_count(!c, sector)
```

Presence requires:

- `sector_asymmetry_state(sector)` iff
  `sector_pawn_count(white, sector) != sector_pawn_count(black, sector)`

Payload is restricted to:

- `sector`
- `majority_side`
- `minority_side`
- `white_pawn_count`
- `black_pawn_count`

Forbidden meaning:

- minority-attack wording
- majority-attack wording
- favorable-imbalance wording
- space-claim wording
- break-source wording
- access, route, or conversion wording
- initiative wording

Negative boundary:

- equal pawn counts in the sector
- board-global wing plan restated as sector trigger
- harmless imbalance overstated as useful imbalance
- minority attack restated from count imbalance alone

### Opposite-Side Castling/Wing Asymmetry Reclassification

The historical inventory label `opposite-side castling/wing asymmetry` survives
for `61`-table continuity only.

There is no active `U` runtime contract for this label on the current branch.

Illustrative lower support material includes:

- `castling_rights`
- `king_shelter_hole`
- king-theater-linked file/diagonal access geometry
- `central tension`

This lower support list is illustrative, non-exhaustive, and non-conjunctive.
It does not define a required bundle.

The row leaves `U` because:

- the slash label bundles castling provenance and wing-placement asymmetry
- current king-wing placement alone is too loose for the castling label
- practical race, pawn-storm, initiative, and king-safety meaning belongs above
  `U`

Deferred outside `U`:

- `opposite_king_wing_state` if a future branch wants a pure wing-placement
  witness

Runtime identifiers such as:

- `opposite_side_castling`
- `wing_asymmetry_state`
- `opposite_king_wing_state`

are not valid active `U` witness ids on this branch.

Negative boundary:

- opposite king wings with no castling provenance
- same-side castling with wing asymmetry
- pawn-storm or race meaning inferred from placement alone

### Available Lever Narrowing

The historical inventory label `available lever` survives for `61`-table
continuity only.

The canonical runtime contract id is `available_lever_trigger`.

`available_lever_trigger` remains `U-primary` with these rules:

- primary anchor: `piece-square`
- polarity: `owner`
- local meaning:
  - an immediate local pawn trigger only
  - the owner pawn on the anchor square has one or more legal push-contact
    variants
  - no strategic success, readiness, counterplay, or break-point meaning

The contract is variant-scoped rather than payload-only.

The active variants are:

- `single_push_lever_state`
- `double_push_lever_state`

The contract is defined from `R` only:

```text
single_push_lever(anchor_square) :=
  a legal single push from the owner pawn on anchor_square
  reaches an empty arrival square
  and from that arrival square immediately attacks at least one defender pawn

double_push_lever(anchor_square) :=
  a legal double push from the home rank through an empty intermediate square
  reaches an empty arrival square
  and from that arrival square immediately attacks at least one defender pawn
```

Presence requires:

- `single_push_lever_state(anchor_square)` iff
  `lever_available(owner, anchor_square)` and `single_push_lever(anchor_square)`
- `double_push_lever_state(anchor_square)` iff
  `lever_available(owner, anchor_square)` and `double_push_lever(anchor_square)`

Payload is restricted to:

- `owner_pawn_square`
- `available_variants`

Forbidden meaning:

- strategic target filtering
- break-point wording
- counterplay-source wording
- readiness or initiative wording
- pressure, penetration, or access wording
- tactical soundness requirement
- capture-arrival source
- en-passant source

Negative boundary:

- no legal push-contact variant
- capture-only contact source
- en-passant-only source
- raw break-point restatement
- useful lever or successful lever wording

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
  - the witness is still only a source; narrow `S21` survival admission now
    requires same-board certified `InitiativeWindow`

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

The payload must not embed parallel `U` witnesses, deferred seeds, or
target-role tags such as:

- `weak pawn`
- `open/semi-open file`
- `qb_diagonal_alignment_seed`
- `xray_target`
- `initiative`

### King Shelter Reclassification

The historical inventory label `king shelter` survives for `61`-table
continuity only.

This branch does not admit an active `U` runtime witness for `king shelter`.

Runtime identifiers such as:

- `king_shelter`
- `weak_king_shelter`
- `broken_king_shelter`
- `king_shelter_hole_target`

are not valid active `U` witness ids on this branch.

The exact local board fact remains in `R`:

```text
king_shelter_hole(c, s)
```

That root atom is the only exact square-level shelter fact currently supported
by the branch root set and extractor.

`king shelter` leaves `U` because:

- no distinct witness discriminator survives beyond raw `king_shelter_hole`
- the extractor is intentionally restricted to the defending king's
  home-shelter regime
- the root already mixes shelter weakness with piece attack or access, so a
  separate `U` witness would duplicate access-lane meaning rather than owning a
  new exact-board contract

The broad shelter-shell meaning belongs above `U`, inside rewrite
`KingSafetyShell` / upper-layer king-safety composition rather than inside raw
witness admission.

Allowed support-only usage:

- `king_shelter_hole` as exact support evidence
- `king_shelter_hole` as optional strengthener for king-theater-linked
  file/diagonal access geometry
- shelter wording only above `U` inside king-safety-shell composition

Object-side runtime contract:

- helper `home_shelter_shell`
- requires the defending king to remain on its home rank on the current
  exact-board home-wing proxy file (`c` or `g`) and at least two edge-adjacent
  `king_shelter_hole` squares inside the king-centered home-shelter mask
- `KingSafetyShell` remains separate from `comparative_king_fragility` and
  `certified_king_safety_edge`
- runtime status: implemented in
  `modules/commentary/src/main/scala/lila/commentary/strategic/KingSafetyShellRule.scala`
  and verified by `KingSafetyShellRuleTest` plus
  `StrategicObjectCorpusRuntimeTest`

Negative boundary:

- central king
- uncastled king
- walked king
- file or diagonal entry axis only
- raw hole count only
- attacked-square count only
- comparative or certified king-safety wording
- initiative, mate, or forcing-attack wording

### Rook Lift Reclassification

The historical inventory label `rook lift` survives for `61`-table continuity
only.

This branch does not admit an active `U` runtime witness for `rook lift`.

Runtime identifiers such as:

- `rook_lift`
- `lift_corridor_seed`
- `rook_lift_seed`

are not valid active witness ids on this branch.

`rook lift` leaves `U` because:

- current-board truth cannot certify move-history provenance between a real
  lift, a lateral swing, and generic rook redeployment
- the reviewed `lift_corridor_seed` contract stayed too thin and collapsed into
  elevated-rook geometry plus generic same-rank mobility
- meaningful route, access, or attack semantics would immediately overlap with
  `open/semi-open file`, `rook on open file`,
  king-theater-linked file/diagonal access geometry, `king attack`, or
  upper-layer initiative

The broad `rook lift` meaning belongs above `U`, inside route/access/attack
composition such as rewrite `RedeploymentRoute`, `AccessNetwork`, or
`AttackScaffold`.

Any future `lift corridor seed` belongs to deferred primitive/support territory
only. It is not yet an admitted root schema and it is not an active `U`
witness on this branch.

Negative boundary:

- elevated rook square only
- lateral relocation only
- blocked home bridge
- future lift potential only
- open-file or semi-open-file restatement only
- access-lane or king-attack restatement
- initiative, release, or hidden tactical release wording

### Queen-Bishop Battery Reclassification

The historical inventory label `queen-bishop battery` survives for
`61`-table continuity only.

This branch does not admit an active `U` runtime witness for
`queen-bishop battery`.

Runtime identifiers such as:

- `queen_bishop_battery`
- `queen_bishop_battery_ray`

are not valid active witness ids on this branch.

`queen-bishop battery` leaves `U` because:

- a broad battery witness either over-admits inert same-diagonal alignment or
  imports target, pressure, access, or king-safety semantics
- the reviewed `queen_bishop_battery_ray` contract stayed too strong for
  `battery` meaning and collapsed into geometry-only ordered alignment
- the only contract that survived adversarial review was a deferred primitive:
  `qb_diagonal_alignment_seed`

`qb_diagonal_alignment_seed` belongs to deferred primitive/support territory
only. It is not an admitted root schema and it is not an active `U` witness on
this branch.

It is restricted to:

- same-owner queen and bishop
- one exact diagonal alignment ray
- clear segment between queen and bishop
- bishop ahead of queen on the chosen ray

Negative boundary:

- blocked same-diagonal alignment
- reverse ordering only
- any exact lane, feeder-entry, target, or pressure claim
- any king-theater, king-safety, initiative, or attack-strength wording

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

The certification-side contracts are split into:

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
    - a king-shelter diagonal lane
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
  - `open/semi-open file`
  - `diagonal_lane_only`
  - `rook on open file`
- `duty_bound_defender`
- king-theater-linked file/diagonal access geometry
- `short_run_slider_gate_restriction`
- `central tension`
- legacy `king attack` label:
    - upper-layer projection only
    - host-chain only
    - never self-certifying
- deferred support seeds:
  - `qb_diagonal_alignment_seed`

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
- `move_order_relevance_gate`
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

### King-Theater Access Composition

This branch does not admit a standalone lower-fragment runtime id for
king-theater file/diagonal entry below upper-layer `king safety edge` and
`initiative`.

The older documentation phrase for that entry pattern survives only as
documentation shorthand.

It is not a historical `61` inventory label, a new top-level inventory row, or
an admitted `U-attached` contract.

Current-worktree access meaning is represented compositionally through:

- `file_lane_state`
- `diagonal/color complex`:
  - `diagonal_lane_only`
- the existing `king_theater_link` gate when upper attack-side consumers certify
  the same defending king

Disallowed hosts:

- `king attack`
- `rook on open file`
- legacy `queen-bishop battery` label
- legacy `open center` label
- legacy `king shelter` label
- legacy `key file/rank` label
- `diagonal/color complex`:
  - `color_complex_only`

The contract is defined from `R` plus the neutral host projection:

```text
occupied(square) :=
  exists piece_on(_, _, square)

host_lane_square(square) :=
  square lies on the host-projected file or diagonal lane

lane_compatible_source_piece(source_square) :=
      host_lane_square(source_square)
      and (
        (
          host is file_lane_state
          and (
            piece_on(beneficiary, rook, source_square)
            or piece_on(beneficiary, queen, source_square)
      )
    )
    or
    (
      host is diagonal_lane_only
      and (
        piece_on(beneficiary, bishop, source_square)
        or piece_on(beneficiary, queen, source_square)
      )
    )
  )

clear_lane_segment(source_square, target_square) :=
  source_square and target_square lie on the same host lane
  and every square strictly between them on that lane is unoccupied

lane_reaches_from_source(source_square, square) :=
  lane_compatible_source_piece(source_square)
  and clear_lane_segment(source_square, square)

lane_controlled_by_host(square) :=
  exists source_square such that
    lane_reaches_from_source(source_square, square)
    and controlled_by(beneficiary, square)

entry_square(square) :=
  host_lane_square(square)
  and king_ring_square(defender, square)
  and lane_controlled_by_host(square)

feeder_square(square) :=
  host_lane_square(square)
  and not king_ring_square(defender, square)
  and lane_controlled_by_host(square)

adjacent_on_host_lane(feeder, entry) :=
  feeder and entry are consecutive squares on the same host lane

feeder_entry_pair(feeder, entry) :=
  feeder_square(feeder)
  and entry_square(entry)
  and adjacent_on_host_lane(feeder, entry)

lane_source_exists :=
  exists source_square, feeder, entry such that
    lane_compatible_source_piece(source_square)
    and feeder_entry_pair(feeder, entry)
    and clear_lane_segment(source_square, feeder)
    and clear_lane_segment(source_square, entry)
```

Presence requires:

- the host provides a concrete file or diagonal projection into the defender
  king ring
- `lane_source_exists`

Optional strengtheners:

- `king_shelter_hole(defender, entry)` for an admitted `feeder_entry_pair`
- multiple `feeder_entry_pair` instances on the same host lane

Negative boundary:

- generic attacked-square pressure with no lane-compatible source
- off-lane control restated as lane access
- host lane reaches the king ring but no lane-compatible source exists
- source exists but no admitted feeder-entry pair exists
- single controlled king-ring square with no feeder
- `king_shelter_hole` only
- knight or pawn control on the host lane with no compatible file/diagonal
  carrier
- unsafe-king, strong-attack, initiative, or mate wording

Payload is restricted to:

- `host_id`
- `beneficiary`
- `source_piece_squares`
- `feeder_entry_pairs`
- optional `shelter_entry_squares`

Deferred outside the current contract:

- horizontal rank access
- rook-swing corridor access
- walked-king lateral corridor access

The next review wave should continue from the most failure-prone `U-primary`
descriptors rather than broadening the table first.

## Certification 10 Inventory / 11 Runtime Family Freeze

Current-worktree certification status:

- row ownership is already frozen
- current worktree now carries a live certification runtime under
  `modules/commentary/src/main/scala/lila/commentary/certification`
- `CommentaryCore` now publishes `activeCertificationFamilyIds` plus typed
  `extractCertifications` and `extractCertificationsFailClosed` facades
- certification still starts only from the frozen first-live slices below
- ownership stays frozen at `10` certification inventory rows mapped to `11`
  runtime families

Runtime-family split note:

- `development lag` and `development lead` both map to
  `DevelopmentComparison`
- `king safety edge` stays one inventory row but splits into:
  - `ComparativeKingFragility`
  - `CertifiedKingSafetyEdge`
- `perpetual/fortress` stays one inventory row but splits into:
  - `FortressDrawCertification`
  - `PerpetualCheckHolding`

First-live certification rows:

- when a first-live certification slice explicitly depends on a lower family
  seed, the scaffold must declare that dependency as
  `requiredSupportFamilies`

- `development lag` / `development lead`
  - owner home: `DevelopmentComparison`
  - first live slice: `OpeningDevelopmentRegime`-backed comparative
    development superiority only
  - negative boundary:
    - opening regime alone
    - `phase_gate`
    - development wording alone

- `initiative`
  - owner home: `InitiativeWindow`
  - first live slice: development-led initiative only
  - negative boundary:
    - `DevelopmentComparison` alone
    - `AttackScaffold` alone
    - counterplay wording alone

- `mobility edge`
  - owner home: `MobilityComparison`
  - first live slice: restriction-backed comparative mobility only
  - negative boundary:
    - restriction geometry alone
    - space-clamp wording alone
    - bad-piece wording alone

- `king safety edge`
  - owner homes:
    - `ComparativeKingFragility`
    - `CertifiedKingSafetyEdge`
  - first live comparative slice:
    - home-wing king-theater fragility asymmetry only
  - first live certified slice:
    - `AttackScaffold` plus comparative king fragility plus host/budget/
      move-order/best-defense burden
  - negative boundary:
    - `KingSafetyShell` alone
    - `AttackScaffold` alone
    - phase proxy or attack wording alone

- `mate net`
  - owner home: `MateNetCertification`
  - first live slice: forcing mate-net certification only
  - negative boundary:
    - `AttackScaffold` alone
    - certified king-safety edge alone
    - mate-threat wording alone

- `material harvest`
  - owner home: `MaterialHarvest`
  - first live slice: realized non-king material conversion only, via a
    current-turn capture that the rival cannot immediately recapture
  - negative boundary:
    - `material gain` shell wording
    - tactical smell alone
    - `winning endgame` wording alone

- `winning endgame`
  - owner home: `WinningEndgame`
  - first live slice: certified conversion/result verdict only, currently
    narrowed to a single non-rook-pawn runner with owner king support, owner to
    move, and no rival pawn counterplay so corner-draw rook-pawn shells and
    counter-races stay out
  - negative boundary:
    - `TradeInvariant` alone
    - `FortressHoldingShell` alone
    - material-edge wording alone

- `perpetual/fortress`
  - owner homes:
    - `FortressDrawCertification`
    - `PerpetualCheckHolding`
  - first live fortress slice:
    - `FortressHoldingShell`-backed hold certification only, with the draw
      burden still carried by explicit best-defense evidence rather than shell
      presence alone, and with the validation corpus kept inside explicit
      drawish `maxAbsCp` budgets
  - first live perpetual slice:
    - stable perpetual-check hold only
  - negative boundary:
    - shell wording alone
    - checking-sequence wording alone
    - draw wording alone

- `promotion race`
  - owner home: `PromotionRace`
  - first live slice: kings-and-pawns-only clear-run promotion-race
    certification on top of `EndgameRaceScaffold`, using tempo plus
    rival-king-distance burden so non-king interceptors do not masquerade as
    route survival
  - negative boundary:
    - `EndgameRaceScaffold` alone
    - `PasserComplex` wording alone
    - `ConversionFunnel` wording alone

Certification-side runtime design is also frozen:

- canonical runtime package:
  `modules/commentary/src/main/scala/lila/commentary/certification`
- live extractor inputs:
  - `StrategicObjectExtraction`
  - `StrategicDeltaExtraction`
  - explicit certification evidence bundle, with
    `CertificationEvidenceBundle.empty` as the explicit unbound fail-closed
    sentinel and any non-empty bundle created by `forObjectExtraction` or
    `forDeltaExtraction` bound to the same current root state
  - live certification extraction must reject any non-empty evidence bundle
    whose bound root state does not exactly match the current extraction
- live probe adapter status:
  - `CertificationEngineEvidence.fromProbe(...)` remains fail-closed empty
  - probe usage is currently validation-side scaffold only
- live public facade stays fail-closed:
  - no raw-FEN-only certification convenience helper may fabricate evidence
  - public certification helpers stay limited to typed extraction entry points
- `SupportOnly` and `Deferred` remain fail-closed endpoints rather than planner
  or projection seeds
