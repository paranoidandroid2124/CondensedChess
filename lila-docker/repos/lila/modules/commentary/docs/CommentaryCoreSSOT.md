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

`root truths -> typed witnesses -> strategic objects -> deltas -> certification -> strategy projections -> renderer`

Truth ownership above `U` sits on the `object -> delta -> certification`
chain.

`projection` and `renderer` are downstream consumers, not owners of truth.

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
and [DecisionFreezeLedger.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DecisionFreezeLedger.md)
and [Witnesses61.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/Witnesses61.md)
and [DescriptorOwnershipMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DescriptorOwnershipMatrix.md)
and [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md)
and [StrategySupportSeedInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategySupportSeedInventory.md)
and [BlockedUPrimaryDiscriminatorInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/BlockedUPrimaryDiscriminatorInventory.md)
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

The branch keeps a fixed `61`-descriptor inventory above root truths.

The canonical primary owner-layer split is now:

- `Witness / U-primary`
  - active deterministic witness instances
- `Witness / U-attached`
  - deterministic descriptors that require a host
- `Object`
  - stable strategic owners built from witnesses
- `Delta`
  - move-local or scope-aware change owners built from objects
- `Certification`
  - comparative, persistence, denial, and conversion verdict owners
- `Projection`
  - human strategy vocabulary only

`upper-layer` survives only as a historical umbrella phrase for older notes.

Renderer owns zero descriptor-inventory rows.

#### Witness Boundary Rules

The historical inventory label is not automatically the runtime contract id.

If a legacy inventory label carries comparative or verdict drift, the runtime
contract may use a narrower witness id while the `61` table keeps the legacy
label for continuity.

The same narrowing rule also applies when a legacy `U-primary` row mixes local
witness meaning with upper-layer wording or with multiple anchor shapes.

If a legacy row collapses to a raw root alias with no distinct witness
discriminator, it leaves `U` rather than surviving as a degenerate runtime id.

An attached descriptor does not inherit host polarity by default.

If the host is only a neutral scope provider, beneficiary polarity must be
derived by an explicit root-level rule inside the host-projected scope.

An attached descriptor must not absorb parallel `U-primary` witnesses into its
payload.

Cross-witness composition begins only above `U`.

Current recorded examples:

- inventory label `opening-tempo`
- the row leaves `U`
- continuity meaning lives in `OpeningDevelopmentRegime`
- `phase_gate` is an upper release guard only and is not an admission source
- exact phase-witness admission does not survive as a runtime contract
- inventory label `middlegame-positional`
- the row leaves `U`
- continuity meaning lives in `DistributedContactRegime`
- `contested_sectors` remains payload geometry only and never became a
  standalone lower witness
- phase/posture inflation and unproven axis-independence remain forbidden
- inventory label `transition-liquidation`
- the row leaves `U`
- continuity meaning lives in delta-layer `TradeCompressionCorridor`
- `TransitionBridge`, `MoveLocal`, `PlanRace`, `InitiativeWindow`,
  `ConversionFunnel`, and `PasserComplex` stay outside raw witness admission
- generic liquidation only and phase/posture inflation across cells remain
  forbidden
- inventory label `endgame-race`
- the row leaves `U`
- continuity meaning lives in `EndgameRaceScaffold`
- `low-material regime` stays contextual only and is not a conversion claim
- `PlanRace`, `PasserComplex`, `ConversionFunnel`, and `promotion race` remain
  above `U`
- phase/posture inflation across cells remains forbidden
- inventory label `space gain`
- runtime contract id `structural_space_claim`
- anchor `sector`
- allowed hosts `closed center` and `fixed chain`
- disallowed hosts `majority/minority asymmetry` and `restriction geometry`
- the present claim is a beneficiary-controlled connected square set attached
  to a host-supplied structural frontier
- inventory label `open/semi-open file`
- runtime contract id `file_lane_state`
- primary anchor `file`
- variants are `open_file_state` and `semi_open_file_state`
- local meaning is pure structural file-state substrate only, not pressure,
  penetration, access, or utility
- `open_for_color` exists only on the `semi_open_file_state` payload and does
  not create a witness polarity
- inventory label `center`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- exact center meaning stays in narrower structural or helper rows; the label
  must not be widened into `open center`, `closed center`, `central tension`,
  `SpaceClamp`, `FixedTargetComplex`, or `TensionState`
- inventory label `kingside`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- the label must not leak into `king attack`, `king shelter`, or other
  kingside-specific upper families
- any assignment to `SpaceClamp`, `FixedTargetComplex`, `TensionState`, or a
  kingside-specific upper family is overfit
- inventory label `queenside`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- the label must not leak into castling-provenance, `wing_asymmetry_state`, or
  `king attack`
- any assignment to `SpaceClamp`, `FixedTargetComplex`, `TensionState`, or a
  queenside-specific upper family is overfit
- inventory label `whole-board`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- broad whole-board access-shadow aggregates are not canonical conversion state
- local scope may not widen into whole-board language without a later dedicated
  matrix
- inventory label `rook on open file`
- runtime contract id `rook_on_open_file_state`
- primary anchor `piece-square`
- local meaning is a rook placement on an open file only, not pressure,
  penetration, access, king-theater relevance, or initiative
- semi-open files stay outside this witness
- inventory label `bishop pair`
- runtime contract id `bishop_pair_state`
- primary anchor `board`
- local meaning is bishop-pair possession only, not bishop-pair advantage,
  color-complex dominance, mobility edge, attack strength, or king-safety
  implication
- admission is `>= 2` same-owner bishops; promoted extra bishops do not
  invalidate the witness
- inventory label `knight outpost`
- runtime contract id `knight_on_outpost_square`
- primary anchor `piece-square`
- local meaning is owner-side knight occupancy on an already certified
  `outpost_square`, not generic good-knight value, clamp, pressure, or
  superiority
- the square-side target meaning remains in `weak_outpost_square_state`; this
  row is occupancy/configuration only
- inventory label `weak pawn`
- runtime contract id `weak_pawn_target_state`
- primary anchor `piece-square`
- local meaning is a beneficiary-facing local weak-pawn target class only, not
  fixation, attack plan, conversion, or broader pawn-quality meaning
- `defender` is the owner of the pawn on the anchor square and `beneficiary`
  is derived as `not defender`
- admission is `piece_on(defender, pawn, square)` plus at least one of:
  - `fixed_pawn(defender, square)`
  - `backward_pawn(defender, square)`
  - `isolated_pawn(defender, square)`
- this row does not collapse into `passed pawn`, `candidate_passer`, or
  `weak square/outpost`
- inventory label `passed pawn`
- runtime contract id `passed_pawn_entity_state`
- primary anchor `piece-square`
- local meaning is an owner-side exact-board passed-pawn entity only, not
  promotion-readiness, conversion, route, or broader passer-play meaning
- `owner` is the owner of the pawn on the anchor square
- admission is `piece_on(owner, pawn, square)` plus `passed_pawn(owner, square)`
- this row does not collapse into `candidate_passer`, `promotion/passer`, or
  broader passer-play above `U`
- inventory label `promotion/passer`
- there is no admitted `U` runtime contract for this label on the current
  branch
- exact lower support remains in `passed_pawn_entity_state` and root
  `candidate_passer(c, s)`
- broader passer conversion or promotion meaning belongs above `U`, inside
  `PasserComplex`, `ConversionFunnel`, or `promotion race`
- any surviving support/render use is review-only and must not be presented as
  admitted runtime ownership
- inventory label `structural damage`
- there is no active runtime contract for this label on the current branch
- the row remains a `U-attached` host-scoped objective shell only
- exact lower support remains in structural-cause roots such as:
  - `isolated_pawn(c, s)`
  - `backward_pawn(c, s)`
  - `fixed_pawn(c, s)`
  - `doubled_file(c, f)`
- related lower witness examples may include `weak_pawn_target_state`
- all support examples are illustrative only and must not become a required
  bundle
- any broad structural-damage verdict must not be presented as a lower-layer
  truth claim
- inventory label `material gain`
- there is no active runtime contract for this label on the current branch
- the row remains a `U-attached` host-scoped objective shell only
- it is surface/projection vocabulary only, not a truth-owning material-swing
  predicate
- lower examples, if named, are illustrative consequence motifs only
- such examples are non-exhaustive, non-conjunctive, and not polarity proof
- realized conversion/result meaning belongs above `U` in `material harvest`
  or `winning endgame`
- inventory label `draw/hold`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy projection wording only, not an exact-board witness, host
  owner, object family, transformation row, or result verdict
- certified holding-side meaning stays in `FortressHoldingShell`
- bounded favorable simplification stays on the distinct `TradeInvariant` lane
- `neutralization / consolidation / fortress holding` is projection-band
  vocabulary only
- `simplify`, `perpetual/fortress`, and `winning endgame` remain related but
  distinct rows and must not collapse into `draw/hold`
- inventory label `king attack`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy projection wording only, not an exact-board witness, host
  shell, object family, or king-safety verdict
- certified attack-side semantic ownership stays in `AttackScaffold`
- `direct piece concentration king attack` and `color-complex king attack`
  remain projection bands only
- `king_file_diagonal_entry_axis`, `file_lane_state`, `diagonal_lane_only`, and
  `rook_on_open_file_state` remain related but distinct lower fragments
- `king safety edge`, `initiative`, and `mate net` remain related but distinct
  upper rows and must not collapse into `king attack`
- inventory label `simplify`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy surface transformation wording only, not an exact-board
  witness, transformation owner, object family, or result verdict
- certified simplification-side semantic ownership stays in `TradeInvariant`
- bounded favorable simplification remains a same-task move-local slice only
- `favorable simplification` is projection-band vocabulary only
- `draw/hold`, `winning endgame`, `perpetual/fortress`, `material gain`, and
  `promotion/passer` remain related but distinct rows and must not collapse into
  `simplify`
- inventory label `open line`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a `U-attached` host-scoped transformation shell only
- attachment mode is `host-scoped` and polarity remains `host`
- exact lower line geometry stays on `open_file_state`,
  `semi_open_file_state`, `rook_on_open_file_state`, and
  `king_file_diagonal_entry_axis`
- the label must not be treated as a truth-owning lane, access, pressure,
  attack, or simplification witness
- the shell must not invent explicit certified hosts or a runtime witness id
- inventory label `create passer`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a `U-attached` host-scoped transformation shell only
- attachment mode is `host-scoped` and polarity remains `host`
- exact lower support stays in `candidate_passer(c, s)`
- `passed_pawn_entity_state` is downstream entity truth, not lower truth for
  this row
- `promotion/passer` remains only a neighboring upper-layer legacy projection
  row
- broader passer conversion meaning remains above `U` inside `PasserComplex`,
  `ConversionFunnel`, or `promotion race`
- the label must not be treated as a truth-owning support, entity,
  conversion, or result witness
- inventory label `exchange defender`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy surface transformation wording only, not an exact-board
  witness, transformation owner, tactic owner, object family, or result shell
- `exchange square` and `defended resource` remain related primitive seeds only
- `TradeInvariant` and `DefenderDependencyNetwork` remain related but distinct
  upper families and must not be collapsed into one canonical owner for this
  label
- `duty_bound_defender` and `overload` remain related but distinct lower rows
- `deflection/decoy` and `demolition/undermining` remain related but distinct
  upper-layer tactical labels
- `material gain` and `simplify` remain related but distinct upper rows and
  must not collapse into `exchange defender`
- inventory label `weak square/outpost`
- runtime contract id `weak_outpost_square_state`
- primary anchor `square`
- variants are `outpost_square_state` and residual `weak_square_state`
- `outpost_square_state` has priority over `weak_square_state` so a stronger
  outpost square is not double-counted as a separate weak-square witness
- this row is beneficiary-side square target state only, not owner occupancy,
  strategic value, pressure, or king-theater meaning
- inventory label `loose/overloaded piece`
- runtime contract id `loose_piece_target_state`
- primary anchor `piece-square`
- local meaning is beneficiary-facing loose-piece target only, not overload,
  defender-duty, pin/trap coupling, pressure, or initiative
- the admitted root is `loose_piece(defender, square)` with beneficiary derived
  as `not defender`
- `overloaded_piece` does not survive inside this row; overload meaning stays
  outside it
- inventory label `bad piece`
- there is no active `U` runtime contract for this label on the current branch
- exact-board liability slices stay in narrower witnesses such as
  `loose_piece_target_state`, `duty_bound_defender`, and
  `weak_outpost_square_state`
- the lower support list is illustrative only; it is not exhaustive and does
  not define a required bundle
- broad piece-quality meaning belongs above `U`, inside `improve_worst_piece`,
  `minor_piece_liability`, or `favorable_minor_piece_relation` support
- inventory label `improve worst piece`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy piece-quality projection wording only, not a witness or
  transformation shell
- exact liabilities remain below `U` in narrower witnesses such as
  `loose_piece_target_state`, `weak_outpost_square_state`, and
  `duty_bound_defender`
- support-only exact failure material may also include `pinned_piece` and
  `trapped_piece`
- broad improvement meaning remains above `U`, alongside `improve_worst_piece`
  and `minor_piece_liability`
- `favorable_minor_piece_relation(c)` remains broader upstream support, not a
  lower witness for this label
- inventory label `available lever`
- runtime contract id `available_lever_trigger`
- primary anchor `piece-square`
- local meaning is pure one-move pawn trigger only, not strategic success,
  readiness, counterplay, or break-point meaning
- active variants are `single_push_lever_state` and `double_push_lever_state`
- the variant split keeps the witness on immediate push-contact geometry rather
  than letting it drift into `pawn_push_break_contact_source`
- inventory label `majority/minority asymmetry`
- runtime contract id `sector_asymmetry_state`
- primary anchor `sector`
- local meaning is count-only sector pawn imbalance with side information in
  payload
- minority-attack, majority-play, favorable-imbalance, and route/conversion
  meaning stay above `U`
- inventory label `opposite-side castling/wing asymmetry`
- there is no active `U` runtime contract for this label on the current branch
- lower support examples such as `castling_rights`, `king_shelter_hole`,
  `king_file_diagonal_entry_axis`, and `central tension` remain
  illustrative only, not a required bundle
- mixed castling-provenance and wing-asymmetry meaning stays above `U`; attack
  race, pawn-storm, initiative, and king-safety readings do not survive as raw
  witnesses
- inventory label `open center`
- there is no active `U` runtime contract for this label on the current branch
- the row is projection-only above `U`
- lower support examples such as `file_lane_state` on central files,
  `fixed_pawn`, `central tension`, and `available_lever_trigger` remain
  illustrative only, not a required bundle
- broad central-openness meaning stays above `U`; central break, initiative,
  and king-theater projection must not be restated as raw witness admission
- inventory label `closed center`
- the reviewed narrowing proposal `closed_center_barrier_state` is rejected on
  the current branch
- the proposed mixed-color `8`-connected barrier topology was too
  motif-specific and under-fired on ordinary closed-center positions
- the row no longer remains a `U-primary` placeholder
- it survives only as neutral host vocabulary for `structural_space_claim`
- broader clamp, break-denial, initiative, and king-safety readings must stay
  outside this row
- inventory label `fixed chain`
- the reviewed narrowing proposal `fixed_chain_state` is rejected on the
  current branch
- the proposed connected-component chain topology was too loose for a true
  chess chain and too rich relative to current roots
- the current branch does not certify a cleaner exact-board chain slice as an
  active runtime contract
- the row no longer remains a `U-primary` placeholder
- `structural_space_claim` remains the surviving `U-attached` host contract,
  and `fixed chain` stays allowed host vocabulary inside it
- broader clamp, break-denial, initiative, king-safety, and pawn-plan readings
  must stay outside this row
- inventory label `central tension`
- the reviewed narrowing proposal `central_pawn_contact_state` is rejected on
  the current branch
- the proposed mutual pawn-contact admission was too pawn-specific and
  under-fired on broader central contact states
- the row no longer remains a `U-primary` placeholder
- continuity meaning now lives in object-side `CentralContactFront`
- unlike `closed center` and `fixed chain`, it is not host vocabulary for
  `structural_space_claim`
- openness, closure, break, space, initiative, king-safety, and `TensionState`
  readings must stay outside this row
- inventory label `key file/rank`
- there is no active `U` runtime contract for this label on the current branch
- the legacy mixed-anchor row does not survive because it bundles:
  - `file`
  - horizontal `ray`
- file-side substrate is absorbed by:
  - `file_lane_state`
  - `open_file_state`
  - `semi_open_file_state`
- horizontal-rank meaning is deferred outside `U` as
  `horizontal_rank_access`
- `key` usefulness wording belongs above `U`, not inside a raw witness id
- inventory label `diagonal/color complex`
- runtime contract id `diagonal_lane_only`
- primary anchor `ray`
  - `source square + one diagonal direction`
- local meaning is one exact diagonal projection only, not color-complex
  weakness, bishop quality, king access, or carrier configuration
- `color_complex_only` is deferred outside `U` on the current branch
- inventory label `king shelter`
- there is no active `U` runtime contract for this label on the current branch
- exact local fact stays in root `king_shelter_hole`
- broad shelter-shell meaning belongs above `U`, inside `KingSafetyShell` /
  upper-layer king-safety composition
- `king_shelter_hole_target` is not a valid active witness id on this branch
- inventory label `rook lift`
- there is no active `U` runtime contract for this label on the current branch
- the reviewed `lift_corridor_seed` proposal stayed too thin and is deferred
  to primitive/support territory rather than admitted as a witness
- broad lift meaning belongs above `U`, inside route/access/attack composition
  such as `RedeploymentRoute`, `AccessNetwork`, or `AttackScaffold`
- inventory label `queen-bishop battery`
- there is no active `U` runtime contract for this label on the current branch
- the reviewed `queen_bishop_battery_ray` proposal still overclaimed `battery`
  meaning and collapsed into geometry-only ordered alignment
- the surviving exact-board relation is deferred as `qb_diagonal_alignment_seed`
  in primitive/support territory, not as an admitted witness or root schema
- inventory label `domination net/restriction geometry`
- runtime contract id `short_run_slider_gate_restriction`
- primary anchor `piece-square`
- anchor target is an enemy `bishop`, `rook`, or `queen`
- divisor uses `testable_directions`, not all on-board directions
- local meaning is short-run gate throttling only, not domination or
  no-counterplay
- inventory label `counterplay source/break-point`
- runtime contract id `pawn_push_break_contact_source`
- primary anchor `piece-square`
- anchor target is an owner `pawn`
- contact is push-only; capture-arrival and en-passant are excluded
- break-point remains payload only and is not a witness id
- local meaning is strategic pawn-contact source only, not counterplay
  readiness or initiative
- inventory label `pin`
- runtime contract id `pin`
- primary anchor `ray`
- polarity `beneficiary`
- local meaning is beneficiary-side exact-board pin witness only
- attacker slider, pinned blocker, and hidden anchor must lie on one exact ray
- the blocker is the sole blocker on that ray; moving it concedes the line to
  the king or to a more valuable friendly anchor
- `pinned_piece` is required root support, but `pin` is not identical to
  `pinned_piece` alone
- `duty_bound_defender`, `short_run_slider_gate_restriction`, `xray_target`,
  and `skewer` remain distinct
- inventory label `fork`
- runtime contract id `fork`
- primary anchor `piece-square`
- polarity `beneficiary`
- local meaning is beneficiary-side exact-board fork witness only
- one beneficiary piece on the anchor square attacks two or more distinct
  enemy occupied squares from the current board
- the fork payload is an aligned list of enemy-occupied `target_squares` and
  `target_roles`, all pairwise distinct and all attacked by the anchored piece
- `overload`, `duty_bound_defender`, `deflection/decoy`, `pin`, `skewer`, and
  `xray` remain distinct
- value/support/undefended filters remain detector heuristics, not admitted
  witness law
- inventory label `skewer`
- runtime contract id `skewer`
- primary anchor `ray`
- polarity `beneficiary`
- local meaning is beneficiary-side exact-board skewer witness only
- attacker slider must be a `bishop`, `rook`, or `queen`
- attacker slider, front target, and rear target must lie on one exact ray
- the front target is the sole blocker between attacker and rear target
- the front target is a non-king piece that is strictly more valuable than the
  rear target
- moving the front target off that ray exposes the rear target to the same ray
  attack
- `pin`, `pinned_piece`, `duty_bound_defender`, `xray_target`,
  `short_run_slider_gate_restriction`, and `restriction geometry` remain
  distinct
- inventory label `overload`
- runtime contract id `overload`
- primary anchor `piece-square`
- polarity `beneficiary`
- local meaning is beneficiary-side exact-board overloaded-defender witness
  only
- one anchored enemy non-king defender currently covers two or more distinct
  same-color non-king occupied squares from its present square
- removing the anchor causes every listed duty square to become numerically
  under-defended against the opposing side
- `MoveAnalyzer` / `Motif.Overloading` heuristic handling remains negative
  boundary only, not admitted witness law
- `duty_bound_defender`, `loose_piece_target_state`, `fork`, and
  `deflection/decoy` remain distinct
- inventory label `deflection/decoy`
- there is no active `U` runtime contract for this row
- `Motif.Deflection`, `Motif.Decoy`, and `Motif.RemovingTheDefender` remain
  heuristic siblings only, not admitted lower facts
- the merged label collapses attacked-defender `from-square`,
  lure-destination `to-square`, and capture-removal route logic, so it stays
  `upper-layer`
- `overload`, `duty_bound_defender`, and `fork` remain distinct
- inventory label `interference`
- there is no active `U` runtime contract for this row
- `Motif.Interference` remains a heuristic sibling only, not an admitted lower
  fact
- `Witnesses61` still carries a legacy `U-primary` row, but no
  `RootAtoms`/`RootAtomRegistry`/`RootExtractor`/corpus admission path survives
- `overload` and `fork` remain distinct lower rows
- `clearance`, `deflection/decoy`, and `demolition/undermining` remain
  distinct neighboring tactical rows
- inventory label `clearance`
- there is no active `U` runtime contract for this row
- baseline-head `Motif.Clearance` remains reference-only, not an admitted lower
  fact on the current branch
- `Witnesses61` still carries a legacy `U-primary` row, but no
  `RootAtoms`/`RootAtomRegistry`/`RootExtractor`/corpus admission path survives
- `overload` and `fork` remain distinct lower rows
- `interference`, `deflection/decoy`, and `demolition/undermining` remain
  distinct neighboring tactical rows
- inventory label `demolition/undermining`
- there is no active `U` runtime contract for this row
- `Motif.RemovingTheDefender` remains a heuristic sibling only, not an
  admitted lower fact
- `overloads_or_undermines` remains relation/operator context only, not a
  lower exact family
- the broad removal/support-breaking label stays `upper-layer` because no exact
  current-branch admission path survives
- `overload`, `duty_bound_defender`, `fork`, and `deflection/decoy` remain
  distinct
- inventory label `defender shortage`
- runtime contract id `duty_bound_defender`
- primary anchor `piece-square`
- anchor target is a defender `knight`, `bishop`, `rook`, or `queen`
- local meaning is a physically bound load-bearing defender, not defender-count
  shortage or overload-style dependency
- absolute king pin cases use current attacked-square geometry duty, not legal
  move generation
- occupied-pressure duty keeps `xray_target` on beneficiary-side polarity to
  match the attacking-side root contract
- reviewed lower-fragment id `king_file_diagonal_entry_axis`
- it is not a new `61` inventory label; it is a host-scoped lower fragment
  below `king safety edge` and `initiative`
- allowed hosts are `file_lane_state` and `diagonal_lane_only`
- admission core uses king-ring entry plus lane-compatible source and lane
  reach, not generic attacked-square pressure
- `king_shelter_hole` is a strengthener only, not an entry admission core
- horizontal rank access and rook-swing corridor access stay outside this
  fragment
- inventory label `king safety edge`
- there is no valid `U` runtime witness id for king-safety comparison
- upper-layer contracts split into `comparative_king_fragility` and
  `certified_king_safety_edge`
- lower fragments must be linked to the relevant king theater before they may
  participate in an upper-layer king-safety comparison
- `king attack` or attack-map pressure never self-certifies a king-safety edge
- phase, material, attack-host, and best-defense gates are required before a
  certified king-safety edge is released

### Layer 3: Strategic Objects

This is the first truth-owning commentary layer.

A strategic object is a stable strategic state unit on the board.

Objects are formed only from witness material.

### Layer 4: Deltas

A delta is the typed change or scope statement about an object:

- what changed because of the move
- what matters in the current position
- what local or comparative scope now applies

`Delta` remains truth-owning.

### Layer 5: Certification

Certification is where the branch decides whether an object or delta survives as
actionable, comparative, denial-bearing, or conversion-bearing truth.

This is where best-defense, persistence, superiority, and route-survival burdens
are paid.

### Layer 6: Strategy Projections

The `24` strategy labels are projection vocabulary only.

They are not released directly from raw features or raw witnesses.

They are derived only from certified objects and certified deltas.

### Layer 7: Renderer

The renderer does not own strategy truth.

It verbalizes already certified claims.

LLM usage, if any survives later, is limited to wording and presentation.

### Engine / Probe Sidecar

Engine and probe evidence are not part of `R` and do not participate in witness
admission.

They are a separate side evidence channel consumed only at:

- `Delta`
- `Certification`

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

### Strategy Projection Independence Contract

The `24` strategy projections are frozen as projection bands, not as loose
phrasing buckets.

They must satisfy both of these conditions:

1. semantic independence
2. lower-layer composability

Semantic independence means:

- each `Sxx` must have at least one minimally distinguishing certified gate,
  carrier family, or exclusion rule that is not reducible to a single rival
  `Syy`
- a projection band may overlap another band on the same position, but it must
  not collapse into a pure wording variant of that rival band
- no projection band may be admitted from a legacy inventory label alone

Lower-layer composability means:

- each `Sxx` must be constructible from certified `Object`, `Delta`, or
  `Certification` carriers, plus admitted `Witness` support where required
- `Projection` never reaches back into raw root extraction or witness admission
- `Renderer` never supplies missing semantic input

Many-to-one fan-in is allowed.

One certified carrier family may feed multiple strategy bands.

What is forbidden is zero-carrier projection or projection admission from
surface wording alone.

The main current independence hot spots are:

- `S02` / `S03` / `S04` in the king-attack cluster
- `S05` / `S21` in the center-vs-counterplay cluster
- `S11` / `S13` / `S14` / `S15` / `S16` in the pawn-structure conversion
  cluster
- `S17` / `S18` / `S20` in the minor-piece and domination cluster
- `S19` / `S22` / `S23` / `S24` in the endgame, simplification, and tactical
  conversion cluster

The boundary freeze for `S01-S24` now lives in
[StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md).

The currently required future lower/support seed families for blocked strategy
bands live in
[StrategySupportSeedInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategySupportSeedInventory.md).

The next projection step is therefore not renderer wording.

It is a carrier-coverage matrix and corpus plan that proves:

- the minimum required certified carriers for each strategy band
- the optional strengthening carriers for each strategy band
- the rival bands that must remain separable on exact boards

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
- object third
- delta fourth
- certification fifth
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
