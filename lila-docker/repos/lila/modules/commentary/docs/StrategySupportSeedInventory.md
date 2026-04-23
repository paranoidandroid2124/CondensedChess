# Strategy Support Seed Inventory

This document freezes lower/support seed families that must exist before
selected strategy bands may be released as live projections.

It does **not** introduce a new semantic layer.

Every row below is intended to land inside the existing
`Witness -> Object -> Delta -> Certification -> Projection` staircase.

The purpose is to prevent post-`U` work from improvising missing lower
contracts at the moment of projection release.

## Current Scope

The current branch tracks seed-family status for strategy bands that previously
blocked on missing lower support:

- `S17 worst-piece improvement / bad-piece exchange` has live
  Witness-boundary support seeds and is now start-ready through
  `StrategyProjectionAdmission`
- `S23 king activation / opposition / penetration` has live
  Witness-boundary support seeds and is now start-ready through
  `StrategyProjectionAdmission`
- `S24 tactical conversion of a prepared target` has live
  Witness-boundary support seeds and is now start-ready through
  `StrategyProjectionAdmission`
- `S25 horizontal rank access` has a live Witness-boundary support seed and is
  now start-ready through `StrategyProjectionAdmission`
- `S06 space bind / restriction` has live runtime admission for the narrow
  exact-board route and a support-only lower certification carrier.

## Global Rules

- A support seed is not a renderer phrase, planner hint, or engine score.
- A support seed must carry an exact anchor, local meaning, and negative
  boundary.
- Engine/probe may later certify whether the seed-bearing plan survives best
  defense, but engine/probe does not create the seed.
- The rows below are live `Witness` support seeds below the `61` inventory;
  they remain support-only until their owning band freezes the required
  certification/admission companion and projection validation package.

## S06 Live Support Freeze

`S06` does not introduce a new live support seed. Its live freeze records the
lower carriers that runtime admission binds without making any of them
projection truth owners.

The lower support side is limited to exact current-board facts already owned
below projection:

- `structural_space_claim` as the broad lower witness that a restriction shell
  exists on the current board
- same-host outpost or short-run restriction support, such as a concrete
  `knight_on_outpost_square` attacking the structural host claim or a local
  slider-gate restriction touching that claim, when it names the same board
  anchor as the projected bind
- `SpaceBindRestrictionCertification` as support-only lower certification,
  never as the S-layer truth owner

The live projection evidence name is
`space_bind_restriction_route_certified`. `StrategyProjectionAdmission` admits
`S06` only when that evidence mirrors the exact route anchor, structural host,
route kind, route-host link, and outpost or restriction square already present
on the current board and certified by the support-only lower certification.

The exact negative boundary is part of the freeze: exact S05 center-release and
exact S20 domination false-rival rows, `MobilityComparison` alone, wrong-anchor
restriction evidence, stale evidence, or shortcut space wording are not S06
runtime admission proof.

## S17 Seed Family

`S17` has narrow live projection runtime admission. The runtime admission
companion is `liability_relief_certified` evidence bound to the same piece as
the live liability anchor and to a matching `repair_route` or
`exchange_relief` seed.

Existing rows such as `loose_piece_target_state`, `duty_bound_defender`,
`weak_outpost_square_state`, `pinned_piece`, and `trapped_piece` identify
liability only.

Existing rows such as `TradeInvariant`, `MobilityComparison`,
`bishop_pair_state`, or `exchange defender` are related context only.

They do not identify the same piece, the same-piece relieving route, or the
same-piece exchange that removes that liability.

### Selected Live Seeds

| Seed family | Intended owner layer | Anchor | Standalone | Local meaning | Negative boundary | Future consumers |
| --- | --- | --- | --- | --- | --- | --- |
| `same_piece_liability_anchor_seed` | live `Witness` support seed | `piece-square` | yes | one concrete beneficiary minor piece is fixed as the exact liability carrier, with its liability bundle drawn from current exact support on that same piece | generic `bad piece` wording; liability spread across multiple pieces; bishop-pair or mobility-edge meaning; any claim that the anchored piece can change by prose | `S17`, future minor-piece-liability object |
| `same_piece_repair_route_seed` | live `Witness` support seed | `piece-square -> relieving-square route` | no; host-scoped to `same_piece_liability_anchor_seed` | the same anchored piece has an exact route to a relieving square or function that removes or materially relieves the anchored liability without relying on generic redeployment or access wording | route on a different piece; generic redeployment/access meaning; route requiring non-admitted tactical cleanup; `MobilityComparison` alone; arrival square that leaves the same liability intact | `S17`, future liability-relief certification |
| `same_piece_exchange_relief_seed` | live `Witness` support seed | `piece-square + exact exchange contact` | no; host-scoped to `same_piece_liability_anchor_seed` | the same anchored piece can be exchanged on exact contact terms that remove the anchored liability without collapsing into broad simplification, tactical conversion, or harvest wording | any exchange not involving the anchored piece; `TradeInvariant` alone; `exchange defender` alone; `material_harvest` alone; exact tactic family alone | `S17`, future liability-relief certification |

### Why These Three

- `same_piece_liability_anchor_seed` fixes which minor piece is actually the
  worst-piece carrier.
- `same_piece_repair_route_seed` gives `S17` exact same-piece repair truth.
- `same_piece_exchange_relief_seed` gives `S17` exact same-piece exchange-relief
  truth that cannot be reduced to simplification or tactics.

Current `S17` projection admission admits only from either:

- `same_piece_liability_anchor_seed` plus
  `same_piece_repair_route_seed`, or
- `same_piece_liability_anchor_seed` plus
  `same_piece_exchange_relief_seed`

In both cases the projection corpus must still keep `S18` and `S20` out on
exact rival rows.

## S23 Seed Family

`S23` has narrow live projection runtime admission. The runtime admission
companion is either same-entry `king_entry_conversion_certified` evidence or
same-contact `king_opposition_certified` evidence.

Existing rows such as `endgame-race`, `whole-board`, `file_lane_state`, or
`winning_endgame` are related context only.

They do not identify a king route, king-entry square, or opposition relation.

### Selected Live Seeds

| Seed family | Intended owner layer | Anchor | Standalone | Local meaning | Negative boundary | Future consumers |
| --- | --- | --- | --- | --- | --- | --- |
| `king_entry_square_seed` | live `Witness` support seed | `square` | yes | a concrete square whose occupation by the beneficiary king yields an admitted endgame penetration or decisive approach point | any generic central square; `whole-board` scope only; rook/file penetration not involving the king | `S23`, future king-activity object, endgame entry certification |
| `king_access_route_seed` | live `Witness` support seed | `king-square -> entry-square route` | no; host-scoped to `king_entry_square_seed` | the beneficiary king has an exact traversable route from its current square to an admitted `king_entry_square_seed` | hand-wavy king activity; blocked route; route requiring non-admitted tactical cleanup; lane access owned by pieces rather than the king | `S23`, future endgame entry certification |
| `king_opposition_contact_seed` | live `Witness` support seed | `king-square pair` | yes | the kings stand in an exact opposition/contact relation that creates or preserves endgame access, restraint, or zugzwang pressure | generic king proximity; mere adjacency; broad endgame advantage wording; any claim that opposition alone already proves winning | `S23`, future opposition/endgame certification |

### Why These Three

- `king_entry_square_seed` gives `S23` a destination truth.
- `king_access_route_seed` gives `S23` a route truth.
- `king_opposition_contact_seed` gives `S23` the king-vs-king relation truth
  that cannot be reduced to route or lane language.

Current `S23` projection admission admits from either:

- `king_opposition_contact_seed` plus endgame certification, or
- `king_entry_square_seed` plus `king_access_route_seed` plus entry or
  conversion certification.

## S24 Seed Family

`S24` has narrow live projection runtime admission. The runtime admission
companion requires same-target
`same_target_forcing_realization` and `same_target_conversion_certified`
evidence on the target named by the live dependency and convergence seeds.

Existing rows such as `pin`, `fork`, `skewer`, `overload`,
`MaterialHarvest`, or `exchange defender` are related but insufficient.

They either describe the eventual tactic/result, or they remain broad surface
labels with no distinct exact discriminator.

### Selected Live Seeds

| Seed family | Intended owner layer | Anchor | Standalone | Local meaning | Negative boundary | Future consumers |
| --- | --- | --- | --- | --- | --- | --- |
| `target_resource_dependency_seed` | live `Witness` support seed | `target piece-square` | yes | a concrete target piece depends on a narrow supporting defender, blocker, or protection link whose failure would expose that same target piece | generic weak piece wording; `duty_bound_defender` alone; `exchange square` alone; `defended resource` alone; target-square-without-piece anchoring | `S24`, future defender-dependency object, future prepared-target package |
| `target_attack_convergence_seed` | live `Witness` support seed | `target piece-square + beneficiary attack footprint` | no; host-scoped to `target_resource_dependency_seed` | the current board already contains a concrete beneficiary-side attack geometry centered on the same named target piece, without yet claiming defender failure, tactic family, or harvest result | any exact tactic alone; generic pressure or tactical smell; geometry not tied to the named target piece; dependency wording alone; result wording such as `MaterialHarvest` or `mate net`; unsupported “prepared target” phrasing | `S24`, future prepared-target object, future prepared-target package |

### Why These Two

- `target_resource_dependency_seed` identifies what is actually prepared.
- `target_attack_convergence_seed` identifies the target-centered attack
  geometry that can later be realized tactically
  before the tactic is played.

The lower prepared-target package should later freeze only from:

- `target_resource_dependency_seed`
- `target_attack_convergence_seed`

The current live slice is piece-square only. A future target-square-without-piece
prepared resource would require a separate lower-carrier freeze and must not be
silently accepted by projection admission.

The `S24` projection realization companion now requires, outside that lower
package:

- same-target `same_target_forcing_realization`
- same-target `same_target_conversion_certified`

## S25 Seed Family

`S25` is start-ready for projection implementation, not broad-ready
deployment. The start-ready admission companion is
`rank_access_consequence_certified` evidence bound to the same source anchor,
same `entry_square`, and `cross_wing_rank_switch` kind as the live
rank-corridor seed.

Existing rows such as `file_lane_state`, `open_file_state`,
`semi_open_file_state`, `rook_on_open_file_state`, `diagonal_lane_only`,
`weak_outpost_square_state`, `king_entry_square_seed`, or exact tactic
families are related context only.

They do not identify a legal same-rank cross-wing rank switch.

### Selected Live Seeds

| Seed family | Intended owner layer | Anchor | Standalone | Local meaning | Negative boundary | Future consumers |
| --- | --- | --- | --- | --- | --- | --- |
| `rank_corridor_state_seed` | live `Witness` support seed | `piece-square + entry-square variant` | yes | one owner rook or queen on ranks `3` through `6` has a legal current-board same-rank move from one wing sector to the nearest opposite-wing empty entry square, crossing the center files, with payload `corridor_kind = cross_wing_rank_switch` | piece on rank only; blocked or illegal same-rank move; home-rank or back-rank defense; generic rook-lift wording; move-history lift provenance; file substrate only; king activity only; weak-square or diagonal access only; tactic/result only | `S25`, future rank-access certification |

### Why This One

- `rank_corridor_state_seed` gives `S25` exact-board rank access without
  reviving `rook lift` as a move-history owner.
- The seed is current-position and legal-move based, so pinned or blocked
  lateral switches do not release the band.
- The first live slice is only `cross_wing_rank_switch`; 7th-rank invasion,
  home-rank lateral pressure, rank-side target pressure, and broader rank
  corridor claims need later exact-board coverage before they can count toward
  broad-ready.

Any later `S25` projection admission should admit only from:

- `rank_corridor_state_seed`
- same-source `rank_access_consequence_certified` evidence
- matching `entry_square`
- matching `corridor_kind = cross_wing_rank_switch`

## Existing Nearby Rows That Are Not Enough

| Existing row / family | Why it is not the missing seed |
| --- | --- |
| `file_lane_state` / `open_file_state` / `semi_open_file_state` | file substrate belongs to `S09`; it is not a same-rank access carrier |
| `rook_on_open_file_state` / `open line` shell | lane support only; no same-rank cross-wing entry square |
| `rook lift` | move-history / redeployment label; not a current-board legal rank-corridor seed |
| `horizontal_rank_access` | broad future projection vocabulary; current live support is only `rank_corridor_state_seed` |
| `endgame-race` | phase/race trigger only; not king activity truth |
| `whole-board` | theater shell only |
| `TradeInvariant` | delta/result-side simplification lane, not target preparation |
| `DefenderDependencyNetwork` | upper family name, not a frozen lower contract |
| `pin` / `fork` / `skewer` / `overload` | tactic means only, not prepared-target truth |
| `material_harvest` / `winning_endgame` / `mate_net_certification` | result/certification, not lower seed |

## Freeze Meaning

The `S17`, `S23`, `S24`, and `S25` seed families are live as Witness-boundary
support-seed runtime contracts.

Nothing in this file authorizes live projection release. It only records the
lower support seeds consumed by the now-frozen start-ready admission scaffold.
