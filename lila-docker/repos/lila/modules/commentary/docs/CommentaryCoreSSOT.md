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

## Public Consumption Boundary

The current worktree exposes a deliberately narrow public extraction boundary
at [CommentaryCore.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/main/scala/lila/commentary/CommentaryCore.scala).

This boundary currently authorizes external consumption of:

- active `U-primary 18` descriptor ids
- active `U-attached 1` descriptor id
- active `Object 7` family ids
- active `Delta 2` family ids
- root-backed `U-primary` witness extraction from exact `Fen` or
  `RootStateVector`
- root-backed `U-attached` witness extraction from exact `Fen` or
  `RootStateVector`
- root-backed strategic-object extraction from exact `Fen` or
  `RootStateVector`
- strategic-delta extraction from exact before/after `Fen` pairs or object
  extractions plus a played move
- fail-closed extraction for exact-board input discipline on both public
  witness facades
- fail-closed extraction for the public strategic-object facade
- fail-closed extraction for the public strategic-delta facade

The public boundary now publishes only the live `U-attached`
`structural_space_claim` contract.

It also publishes the seven live strategic-object families through
`activeObjectFamilyIds` plus the `extractStrategicObjects*` public overloads.

It also publishes the two live strategic-delta families through
`activeDeltaFamilyIds` plus the `extractStrategicDeltas*` public overloads.

Those object overloads currently return a bundled
`StrategicObjectExtraction` carrying `rootState`, the primary/attached witness
snapshots used for extraction, and the extracted strategic objects.

Those delta overloads currently return a bundled
`StrategicDeltaExtraction` carrying the before/after strategic-object
extractions, the played move, and the extracted strategic deltas.

The remaining attached `10` rows stay shell-only and remain outside standalone
public/runtime descriptor registration and extraction. Their host vocabulary may
still surface only as payload under the live `structural_space_claim` contract.

It does **not** authorize claims that planner, outline, renderer, API, or
frontend are already wired to the same truth path.

## Source Context Boundary

Offline source-context validation for opening, motif, endgame-study, and
retrieval references is frozen in
[SourceContextContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/SourceContextContract.md).

This is a test/tooling contract only.

It does not add a public `CommentaryCore` facade and does not make external
source rows Root, U, Object, Delta, Certification, or Sxx truth owners.

Game-context source input is deferred to a separate lane. Clock, rating, time-control,
repetition, tournament, study-chapter, draw-offer, and human-risk context are
outside the current source-context contract.

Endgame result-service source input is deferred. Result-table storage, probing,
result-distance fields, and remote fallback are outside the current
source-context contract.

Endgame-study context is admitted only as named-pattern applicability with exact
material and placement rules. It does not own win, draw, loss, best move,
forced-line, or conversion truth.

See
[ExternalConsumptionAuditEvidence.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ExternalConsumptionAuditEvidence.md)
and
[CommentaryCoreBoundaryTest.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/test/scala/lila/commentary/CommentaryCoreBoundaryTest.scala)
for the current-worktree verification boundary.

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
- `candidate_passer` uses the frozen three-file forward-cone
  support/opposition balance; friendly pawns ahead in the cone count as
  support, and same-rank adjacent friendly pawns also count for the first
  advance
- `loose_piece` is decided by local exchange loss, not raw attacker/defender
  counts
- `pinned_piece` includes relative slider pins to a more valuable friendly
  anchor, not only king pins
- `trapped_piece` is an extreme high-precision non-pawn atom with zero safe
  exits under the local safety rule

Root breadth status is narrower than this semantic freeze.

Current root `broad-confidence-green` set:

- `piece_on`
- `controlled_by`
- `pawn_controlled_by`
- `contested`
- `open_file`
- `half_open_file`
- `king_ring_square`
- `weak_square`
- `isolated_pawn`
- `backward_pawn`
- `doubled_file`
- `candidate_passer`
- `fixed_pawn`
- `en_passant_state`
- `lever_available`
- `loose_piece`
- `overloaded_piece`
- `outpost_square`
- `pinned_piece`
- `passed_pawn`
- `trapped_piece`
- `xray_target`
- `king_shelter_hole`
- `side_to_move`
- `castling_rights`

A future root `broad-confidence-green` claim is allowed only when:

- the schema-local breadth and floor ledger in
  `RootCoverageMatrix.scala` is closed and its tracked markdown snapshot
  remains synchronized
- the exact meaning remains the one frozen in
  [RootAtoms.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/RootAtoms.md)
- engine, when present at all, is still only a confound filter for the
  explicitly frozen high-risk root schemas
  - for engine-required schemas, any selected confound-probe buckets used by
    the green claim must also be frozen in `RootCoverageMatrix.scala`

Projection and renderer stay out of scope for this root breadth state.

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

U broad validation is a test-side confidence state, not a runtime expansion.

Current U broad scope is limited to:

- active `U-primary 18`
- active `U-attached 1`
  - `structural_space_claim`

The owner for U broad buckets, floors, status, and R dependency gates
is `UBroadCoverageMatrix.scala`, mirrored by `u-coverage-matrix.md`.

Current U `broad-confidence-green` descriptors:

- `available_lever_trigger`
- `bishop_pair_state`
- `diagonal_lane_only`
- `duty_bound_defender`
- `file_lane_state`
- `fork`
- `knight_on_outpost_square`
- `loose_piece_target_state`
- `overload`
- `pawn_push_break_contact_source`
- `passed_pawn_entity_state`
- `pin`
- `rook_on_open_file_state`
- `sector_asymmetry_state`
- `short_run_slider_gate_restriction`
- `skewer`
- `structural_space_claim`
- `weak_outpost_square_state`
- `weak_pawn_target_state`

The formal `witness-expectations.jsonl` corpus has `218` descriptor-local broad
rows. The R dependency gates are closed by the Root 25/25
`broad-confidence-green` result, and no live U descriptor remains `thin` or
root-blocked under the current descriptor-local broad corpus.
The U corpus gate is descriptor-local: formal rows must use active runtime
`descriptorId` values and match the per-descriptor counts in the U broad matrix,
with frozen `coverageAxis` / `coverageBucket` breadth tags; aggregate row count
alone cannot promote a descriptor.

Engine/probe evidence must not participate in U witness derivation. If a U row
appears tactically confounded, the row is rejected or moved to a root/object/
certification validation channel; U truth remains `phi(R)` only.

Current code freeze:

- active attached runtime ids are now frozen to `structural_space_claim` only
- `material gain`, `structural damage`, `center`, `kingside`, `queenside`,
  `whole-board`, `closed center`, `fixed chain`, `open line`, and
  `create passer` are code-frozen shell-only rows
- `closed center` and `fixed chain` remain host vocabulary only; they are not
  standalone attached extractors

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
- `TradeCompressionCorridor` is frozen to a `move_local` board-anchored delta
  only
- the first live slice must stay on:
  - `reciprocal_exchange_corridor`
  - `compressed_trade_window`
  - `trade_compression_transition`
  - the first live slice requires:
    - a board-coherent non-king capture on the played move
  - no queens on the after-board
  - at most `4` total non-king non-pawn pieces on the after-board
  - one canonical opposing non-king pair that currently attacks each other
    along one shared file or diagonal corridor on the after-board
  - the before-board failed either the corridor predicate or the compressed
    window
  - forbidden-rival rejection must track the actual current-worktree
    `TradeInvariant` first slice rather than bare board-level
    `EndgameRaceScaffold` persistence
- generic liquidation, quiet corridor alignment, and broad transition-storyline
  wording remain negative only
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
- runtime closure is now fixed in code:
  - `closed center` host requires a connected fixed central frontier that
    spans both `d` and `e` files and contains fixed pawns from both colors
  - `fixed chain` host requires a same-color rear-supported fixed-pawn segment
    of length `>= 2` inside one sector
  - when several same-owner fixed-chain segments survive in one sector, runtime
    keeps the strongest segment that yields a live claim
  - `fixed chain` host is emitted per host owner color and never as a mixed
    white/black boundary
  - `frontier_seed` remains `controlled_by(beneficiary, forward(host_boundary))`
  - `claimed_square_set` is one deterministic strongest connected component of
    empty beneficiary-controlled sector squares attached to those frontier
    seeds; occupied frontier squares are not claim squares
  - presence still requires `|claimed_square_set| >= 2`
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
- code freeze keeps the row outside attached runtime registration
- exact center meaning stays in narrower structural or helper rows; the label
  must not be widened into `open center`, `closed center`, `central tension`,
  `SpaceClamp`, `FixedTargetComplex`, or `TensionState`
- inventory label `kingside`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- code freeze keeps the row outside attached runtime registration
- the label must not leak into `king attack`, `king shelter`, or other
  kingside-specific upper families
- any assignment to `SpaceClamp`, `FixedTargetComplex`, `TensionState`, or a
  kingside-specific upper family is overfit
- inventory label `queenside`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- code freeze keeps the row outside attached runtime registration
- the label must not leak into castling-provenance, `wing_asymmetry_state`, or
  `king attack`
- any assignment to `SpaceClamp`, `FixedTargetComplex`, `TensionState`, or a
  queenside-specific upper family is overfit
- inventory label `whole-board`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- code freeze keeps the row outside attached runtime registration
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
- code freeze keeps the row outside attached runtime registration
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
- code freeze keeps the row outside attached runtime registration
- it is surface/projection vocabulary only, not a truth-owning material-swing
  predicate
- lower examples, if named, are illustrative consequence motifs only
- such examples are non-exhaustive, non-conjunctive, and not polarity proof
- realized conversion/result meaning belongs above `U` in `material harvest`
  or `winning endgame`
- inventory label `draw/hold`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy inventory wording at the descriptor surface; it is not
  itself an exact-board witness, host owner, standalone object runtime id,
  transformation row, or result verdict
- the descriptor row's canonical semantic home on this branch is object layer
  `FortressHoldingShell`
- bounded favorable simplification stays on the distinct `TradeInvariant` lane
- `neutralization / consolidation / fortress holding` remains projection-band
  vocabulary above that object home
- `simplify`, `perpetual/fortress`, and `winning endgame` remain related but
  distinct rows and must not collapse into `draw/hold`
- inventory label `king attack`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy inventory wording at the descriptor surface; it is not
  itself an exact-board witness, host shell, standalone object runtime id, or
  king-safety verdict
- the descriptor row's canonical semantic home on this branch is object layer
  `AttackScaffold`
- `direct piece concentration king attack` and `color-complex king attack`
  remain projection bands above that object home
- king-theater-linked file/diagonal access geometry remains a documentation
  shorthand only, while `file_lane_state`, `diagonal_lane_only`, and
  `rook_on_open_file_state` remain the live lower carriers
- `king safety edge`, `initiative`, and `mate net` remain related but distinct
  upper rows and must not collapse into `king attack`
- inventory label `simplify`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy surface transformation wording only, not an exact-board
  witness, transformation owner, object family, or result verdict
- certified simplification-side semantic ownership stays in `TradeInvariant`
- bounded favorable simplification remains a same-task move-local slice only
- `TradeInvariant` is frozen to a `move_local` board-anchored delta only
- the first live slice must stay on:
  - `bounded_material_reduction`
  - `persistent_object_carrier`
  - `trade_invariant_transition`
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
  remain negative only
- `favorable simplification` is projection-band vocabulary only
- `draw/hold`, `winning endgame`, `perpetual/fortress`, `material gain`, and
  `promotion/passer` remain related but distinct rows and must not collapse into
  `simplify`
- inventory label `open line`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a `U-attached` host-scoped transformation shell only
- code freeze keeps the row outside attached runtime registration
- attachment mode is `host-scoped` and polarity remains `host`
- exact lower line geometry stays on `open_file_state`,
  `semi_open_file_state`, `rook_on_open_file_state`, and
  `diagonal_lane_only`
- the label must not be treated as a truth-owning lane, access, pressure,
  attack, or simplification witness
- the shell must not invent explicit certified hosts or a runtime witness id
- inventory label `create passer`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a `U-attached` host-scoped transformation shell only
- code freeze keeps the row outside attached runtime registration
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
- live `S17` lower support seeds are owned by `StrategySupportSeedInventory.md`;
  exact band start-ready status is owned by `StrategyProjectionBoundaryMatrix.md`
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
- live `S17` lower support seeds are owned by `StrategySupportSeedInventory.md`;
  exact band start-ready status is owned by `StrategyProjectionBoundaryMatrix.md`
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
  king-theater-linked file/diagonal access geometry, and `central tension` remain
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
- broad horizontal-rank meaning remains outside `U`; the current live
  first-slice support for projection is only `rank_corridor_state_seed` under
  `S25`, not a `U` witness
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
- current `S25` support does not revive move-history `rook lift`; it admits
  only legal current-board cross-wing rank switching through
  `rank_corridor_state_seed`
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
- the previous documentation phrase for a king-theater entry axis does not
  survive as a live runtime id on this branch
- it is not a new `61` inventory label or an admitted host-scoped lower
  fragment below `king safety edge` and `initiative`
- any such access meaning is represented compositionally through
  `file_lane_state` / `diagonal_lane_only` under the existing
  `king_theater_link` gate
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

Objects are formed from witness material plus exact-board root support routed
through the shared strategic-object context.

### Object 7 Runtime Contract

The current branch now carries live runtime extraction for the seven `Object`
homes in `modules/commentary/src/main/scala/lila/commentary/strategic`.

The helper and admission laws below are now implemented and exact-board
verified. They are no longer scaffold-only design notes.

Shared helpers:

- `occupied(square)` means `exists piece_on(_, _, square)`
- `sector_mask(sector, square)` follows the canonical file split already
  implied by `WitnessSector`
- `contact_square(square)` means `contested(square)` or an occupied square
  directly attacked by the opponent of the occupant
- `front_connectivity(square_a, square_b)` means the two squares lie in the
  same maximal orthogonally connected component of `contact_square` inside the
  chosen mask
- `central_sector_mask(square)` is the extended center band on files `c-f` and
  ranks `3-6`
- `king_theater_link(fragment, defending_king)` remains the canonical
  king-theater gate reused unchanged by `AttackScaffold`
- `KingSafetyShell` reuses `home_shelter_mask` geometry but adds its own
  home-wing king proxy rather than sharing the full `king_theater_link`

Frozen object homes:

- `OpeningDevelopmentRegime`
  - helper: `opening_development_window`
  - present iff:
    - each side still keeps at least one home-rank bishop or knight on an
      original start square
    - at least one side still keeps at least two such minors
    - each side still keeps at least one home-rank rook on an original corner
    - neither the `d` file nor the `e` file is open
    - no live `CentralContactFront`, `DistributedContactRegime`, or
      `EndgameRaceScaffold` already owns the same board
  - forbids:
    - move-count or tempo narration
    - `phase_gate` admission
    - release-guard or king-safety wording

- `DistributedContactRegime`
  - helper: `distributed_contact_spread`
  - present iff:
    - both colors already have at least one non-pawn piece off the home rank
    - at least two distinct `sector_mask` sectors each contain a connected
      `contact_square` component under `front_connectivity` with at least two
      squares
    - every admitted sector component contains both a contested square and an
      occupied contact square
    - both colors contribute occupancy or current control to every admitted
      sector component
    - one admitted component lies outside the central-only contact band so the
      row does not collapse into `CentralContactFront`
  - forbids:
    - `contested_sectors` as admission proof
    - one-sector tactical shells narrated as regime continuity
    - axis-independence claims that are not board-proven

- `EndgameRaceScaffold`
  - helper: `dual_run_endgame_trigger`
  - `advanced_run_resource(color, square)` means an owner pawn already sits on
    or beyond the fifth rank relative to that color; `passed_pawn_entity_state`
    and root `candidate_passer` remain optional support when present
  - `forward_run_clear(color, square)` means the immediate next square on that
    pawn's advance file is empty on the current board
  - present iff:
    - no queens remain on the board
    - both colors have at least one `advanced_run_resource`
    - each color has at least one such resource with `forward_run_clear` on the
      current board
  - forbids:
    - low-material context alone
    - one-sided passer presence alone
    - directly blockaded runner geometry
    - direct collapse into `promotion_race`, `PasserComplex`, or
      `ConversionFunnel`

- `AttackScaffold`
  - helper: `attack_host_core`
  - present iff one attacking color has at least two distinct
    `king_theater_link` fragments aimed at the same defending king, where:
    - at least one fragment is a carrier from `rook_on_open_file_state` or a
      king-theater-linked `file_lane_state` / `diagonal_lane_only`
    - at least one fragment is a vulnerability/support fragment from
      `king_shelter_hole`, `duty_bound_defender`,
      `short_run_slider_gate_restriction`, `xray_target`, `pinned_piece`, or
      `loose_piece`
    - a shelter-hole-only support picture still needs a second distinct carrier
      fragment; lone local diagonal/file pressure plus holes stays outside the
      host core
  - forbids:
    - attack-map pressure alone
    - carrier-only admission
    - self-certification into `certified_king_safety_edge`, `initiative`, or
      `mate net`

- `FortressHoldingShell`
  - helper: `fortress_entry_denial_shell`
  - `fortress_shell_mask(holder_king, square)` means the square lies on the
    holder king's file or an adjacent file, and on the king's home rank or the
    next two ranks toward the board center
  - present iff one holding side shows all of:
    - no queens remain on the board
    - the holder king remains on its home rank
    - at least two friendly occupied non-king squares lie in
      `fortress_shell_mask`
    - the attacker occupies no square in `fortress_shell_mask`
    - the attacker lacks a current file or diagonal entry axis into any square
      in `fortress_shell_mask`
    - no open or semi-open file on the holder king file or an adjacent file
      currently carries an attacker rook or queen into that shell theater by
      live attack geometry on a shell square
    - any attacker passed pawn on the holder king file or an adjacent shell
      file is already blockaded by immediate holder occupancy
  - forbids:
    - shell shape alone
    - `TradeInvariant` alone
    - `perpetual/fortress` certification alone

- `KingSafetyShell`
  - helper: `home_shelter_shell`
  - `home_shelter_mask(defending_king, square)` means the square lies on the
    defending king file or an adjacent file, and one or two ranks toward the
    board center from the home edge
  - present iff:
    - the defending king remains on its home rank on the current exact-board
      home-wing proxy file (`c` or `g`), so central or uncastled home-rank
      kings stay outside this shell object
    - at least two distinct `king_shelter_hole` squares for the same defender
      lie inside `home_shelter_mask`
    - at least one such pair is edge-adjacent inside that mask
  - forbids:
    - `king attack` wording alone
    - generic pressure away from the home shelter
    - direct collapse into `comparative_king_fragility` or
      `certified_king_safety_edge`

- `CentralContactFront`
  - helper: `central_contact_front_state`
  - present iff one connected `contact_square` component inside
    `central_sector_mask` contains at least two squares, contains both a
    contested square and an occupied contact square, and both colors contribute
    occupancy or current control to that same component
  - if multiple disconnected qualifying components exist, runtime keeps one
    canonical strongest component rather than merging disconnected fronts into
    one sector identity
  - forbids:
    - `open center` narration
    - `closed center` or `fixed chain` host vocabulary
    - initiative, king-safety, or `TensionState` wording

These contracts are now live in the current worktree.

Current-worktree evidence is carried by
`StrategicObject7RuleTest`,
`StrategicObjectCorpusRuntimeTest`,
and the public-boundary coverage in
`CommentaryCoreBoundaryTest`.

### Layer 4: Deltas

A delta is the typed change or scope statement about an object:

- what changed because of the move
- what matters in the current position
- what local or comparative scope now applies

`Delta` remains truth-owning.

Current-worktree `Delta 2` now has live runtime code in
`modules/commentary/src/main/scala/lila/commentary/delta`, with both delta
families registered together and `TradeCompressionCorridor` ordered before
`TradeInvariant`.

Frozen family ids:

- `TradeCompressionCorridor`
- `TradeInvariant`

Runtime boundary:

- current runtime package:
  `modules/commentary/src/main/scala/lila/commentary/delta`
- present files:
  - `TradeCompressionCorridorRule.scala`
  - `TradeInvariantRule.scala`
  - `StrategicDeltaModel.scala`
  - `StrategicDeltaContext.scala`
  - `StrategicDeltaScopeContract.scala`
  - `StrategicDeltaRuntime.scala`
  - `StrategicDeltaExtractor.scala`
- the public `CommentaryCore` facade exposes `activeDeltaFamilyIds`,
  `extractStrategicDeltas(...)` overloads from object extractions and from
  before/after `Fen` plus `playedMove`, and fail-closed delta extraction
  overloads
- delta extraction must consume exact before/after position truth rather than a
  single static board:
  - before `StrategicObjectExtraction`
  - after `StrategicObjectExtraction`
    - both supplied object carriers must remain canonical:
      - witness/object payloads must exactly match the live object extractor
        output for their root states
    - one exact-board `playedMove`
      - side-to-move, castling-rights, and en-passant auxiliary state must be
        rehydrated from the root-state vector rather than guessed from piece
        placement alone
      - legal castling and legal en-passant transitions are part of the live
        exact-board boundary
- `StrategicDeltaExtractor.scala` is live
- `StrategicDeltaRuntime.scala` registers both families together with corridor
  before invariant ordering
- `TradeCompressionCorridorRuleTest`, `TradeInvariantRuleTest`,
  `StrategicDeltaBoundaryTest`, `DeltaExpectationCorpusTest`, and
  `CommentaryCoreBoundaryTest` are live
- `DeltaExpectationCorpusTest` now asserts live runtime extraction against the
  delta corpus rows

### Layer 5: Certification

Certification is where the branch decides whether an object or delta survives as
actionable, comparative, denial-bearing, or conversion-bearing truth.

This is where best-defense, persistence, superiority, and route-survival burdens
are paid.

### Certification Verdict Lattice

Certification owns four verdict outcomes only:

- `Certified`
- `SupportOnly`
- `Deferred`
- `Rejected`

Meaning:

- `Certified`:
  - the row's exact-board burden is met at the currently frozen depth and reply
    standard
- `SupportOnly`:
  - the semantic idea remains real on the exact board, but the branch burden is
    too thin for release as a full verdict
- `Deferred`:
  - the row stays fail-closed because best-defense, comparative, or route
    survival is still reply-incomplete or depth-unstable
- `Rejected`:
  - the exact board fails the row's admission or negative boundary outright

Support and deferred are both real endpoints.

They are not planner hints and they are not projection seeds.

### Certification Runtime Boundary Design

Current-worktree status:

- certification is now live in `src/main` on one canonical package boundary:
  `modules/commentary/src/main/scala/lila/commentary/certification`
- `CommentaryCore` now exposes:
  - `activeCertificationFamilyIds`
  - typed `extractCertifications(...)`
  - typed `extractCertificationsFailClosed(...)`
- those public helpers require a current `StrategicObjectExtraction` or
  `StrategicDeltaExtraction` plus one explicit certification-side engine/probe
  evidence bundle
- any supplied current `StrategicObjectExtraction` must remain canonical:
  - witness/object payload must exactly match the live object extractor output
    for the current root state

The live certification runtime stays on one canonical package
boundary:

- `modules/commentary/src/main/scala/lila/commentary/certification`

The 61-row descriptor ownership ledger still counts the historical
certification-owned descriptor rows by row, but the current live certification
registry exposes `12 active certification families` through
`CertificationScopeContract.activeCertificationFamilyIds`:

- `DevelopmentComparison`
- `InitiativeWindow`
- `MobilityComparison`
- `ComparativeKingFragility`
- `CertifiedKingSafetyEdge`
- `MateNetCertification`
- `MaterialHarvest`
- `WinningEndgame`
- `FortressDrawCertification`
- `PerpetualCheckHolding`
- `PromotionRace`
- `SpaceBindRestrictionCertification`

Inventory mapping stays frozen as:

- `development lag` and `development lead` both map to
  `DevelopmentComparison`
- `king safety edge` remains one inventory row but splits into:
  - `ComparativeKingFragility`
  - `CertifiedKingSafetyEdge`
- `perpetual/fortress` remains one inventory row but splits into:
  - `FortressDrawCertification`
  - `PerpetualCheckHolding`

The live runtime extractor consumes only:

- current `StrategicObjectExtraction`
- current `StrategicDeltaExtraction`
- one explicit certification evidence bundle; `CertificationEvidenceBundle.empty`
  is the explicit unbound fail-closed sentinel, while any non-empty bundle
  created by `forObjectExtraction` or `forDeltaExtraction` must be bound to
  the same current root state
- any supplied `StrategicDeltaExtraction` must be canonical:
  - exact before/after/move validation must still pass
  - the carried `deltas` set must exactly match the canonical delta runtime
    result for that transition
- live certification extraction must reject any non-empty evidence bundle
  whose bound root state does not exactly match the current extraction
- live legal-move reconstruction inside certification must also rebuild the
  exact board from root auxiliary state:
  - side to move
  - castling rights
  - current en-passant availability
  This state must not be guessed from piece placement or board geometry alone.

The live runtime has a certification-only Engine E intake adapter:

- `CertificationEngineEvidence.fromProbe(...)` stays fail-closed empty until a
  raw-probe adapter lands
- `CertificationEngineRuntimeIntake` is the current analyse/backend packet
  intake model; it normalizes runtime packet fields into
  `EngineEvidencePacket` and then delegates to
  `CertificationEngineEvidenceContract`
- current certification Engine E admission does not consume UI/API raw probe
  sidecars; those sidecars may exist elsewhere in the product, but they are not
  certification evidence until normalized through the typed contract below

### Engine E certification evidence freeze

Engine E is certification evidence only. It is not a
`Root/U/Object/Delta/Sxx truth owner`, and it must not create, widen, or promote
root facts, U witnesses, strategic objects, deltas, projection bands, renderer
wording, or Sxx runtime admission.

The frozen live admission boundary for typed E evidence is
`CertificationEngineEvidenceContract` in the certification package.
`CertificationEngineRuntimeIntake` may accept an optional analyse/backend
runtime packet, normalize it into an `EngineEvidencePacket`, and delegate to
that contract. It may produce a `CertificationEvidenceBundle` only after exact
FEN, node identity, freshness, depth, MultiPV, score, and legal-PV checks pass
for the same current extraction.
In short: exact FEN, node identity, freshness, depth, MultiPV, score, and legal-PV checks are the live intake gate.

Packet identity is exact-board:

- object-backed evidence binds to the requested exact normalized full-FEN
  string, including halfmove/fullmove clock fields, plus expected `nodeId` and
  `ply`; `StrategicObjectExtraction` remains root-state-bound, so the
  requested full-FEN is the clock-field authority for live intake
- delta-backed evidence must additionally bind `beforeFen`, `playedMove`, and
  `afterFen`; both endpoint FEN strings must match the requested normalized
  full-FEN strings, and the move and both endpoint boards must match the
  canonical `StrategicDeltaExtraction`
- transition-bound packets are rejected on the object path

Freshness and search state are fail-closed:

- incomplete search, stale `generatedAtEpochMs` beyond `maxAgeMs`, insufficient
  realized depth, insufficient `MultiPV`, or too-short PV lines are rejected
- best-defense and reply-branch claims require at least `MultiPV 3` with
  distinct first moves
- raw probe objects remain untrusted; `CertificationEngineEvidence.fromProbe`
  still returns `CertificationEvidenceBundle.empty`

Score normalization is typed:

- centipawn and mate scores are separate domains
- side-to-move scores are normalized through the bound FEN's side to move before
  applying owner-side burdens
- eval-swing and conversion burdens require typed thresholds; a mate score must
  not masquerade as centipawns
- scoreless bounded claims are rejected
- eval-swing claims require a bound, fresh, same-engine-config baseline packet
  whose FEN matches the transition `beforeFen`; an unbound baseline is not
  evidence
- eval-swing baselines must also bind node/ply: if a transition `beforeNode` is
  supplied it must match exactly, otherwise the baseline ply must immediately
  precede the transition packet ply
- inactive certification family ids, unknown purpose/strength keys, invalid
  owner colors, and invalid UCI PV moves are rejected during runtime intake

PV legality is exact-board:

- every PV line must replay legally from the bound FEN
- illegal continuations and truncation below the claim's `minPvPlies` fail
  closed
- best-defense, eval-shift, conversion, or persistence claims may be admitted
  only when the bounded claim declares its purpose and its typed score/PV
  burden passes

It must not reopen raw root or witness admission from inside certification.

The live public facade stays fail-closed:

- do not add certification convenience helpers that fabricate missing
  certification evidence from raw FEN alone
- `CertificationEngineEvidence` is an opaque Engine E evidence facade and
  exposes no direct claim constructors; E-named evidence creation must go
  through `CertificationEngineEvidenceContract` or the fail-closed
  `fromProbe(...)` placeholder
- keep the public certification surface limited to typed object/delta extraction
  entry points with an explicit certification evidence bundle, where
  `CertificationEvidenceBundle.empty` is the explicit unbound fail-closed
  sentinel and any non-empty bundle must stay exact-position-bound
- missing, rejected, or insufficient runtime E packets leave the existing
  object/delta/certification path on the base evidence bundle; they do not
  fabricate a stronger certification
- projection runtime admission consumes only certified lower carriers and an
  externally supplied same-root `CertificationEvidenceBundle`; projection does
  not consume Engine E directly
- no planner, projection, or renderer layer may revive `SupportOnly` or
  `Deferred` rows

### Certification First-Live Freeze

The live certification boundary stays narrow by freezing the first live slices
rather than opening broad upper prose.

Where a first live slice explicitly depends on a lower object, delta, or
certification-side support family, the certification scaffold must declare that
dependency as a `requiredSupportFamilies` contract rather than leaving it as
prose only.

Certification-side support-family presence is satisfied only by a live
non-`Rejected` claim of the required family for the same owner polarity.

- `DevelopmentComparison`
  - scope: `comparative`
  - anchor: `board`
  - first live slice: `OpeningDevelopmentRegime`-backed comparative development
    superiority only
  - helpers:
    - `development_balance_count`
    - `development_gap_floor`
  - forbids:
    - `OpeningDevelopmentRegime` alone
    - `phase_gate`
    - development wording alone

- `InitiativeWindow`
  - scope: `comparative`
  - anchor: `board`
  - first live slice: development-led initiative only
  - helpers:
    - `initiative_window_contract`
    - `rival_counterplay_source`
    - `move_order_relevance_gate`
  - forbids:
    - `DevelopmentComparison` alone
    - `AttackScaffold` alone
    - counterplay wording alone
  - current projection handoff:
    - supports narrow live `S21` only when the same board also carries an exact
      owner `pawn_push_break_contact_source`, certified `InitiativeWindow`, and
      `counterplay_survival_route_certified` projection evidence mirroring the
      same source, target, route, and certification family

- `MobilityComparison`
  - scope: `comparative`
  - anchor: `board`
  - first live slice: restriction-backed mobility superiority only
  - S20 live projection admission:
    - `mobility_domination_route_certified` is the live projection
      evidence-kind name in
      `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`
    - row-specific burden is enforced as
      `domination_route_requires_certified_same_board_mobility_comparison`,
      `mobility_plus_restriction_requires_same_owner_short_run_slider_gate_restriction`,
      `defender_starvation_requires_same_owner_duty_bound_defender`,
      and
      `same_task_projection_evidence_must_mirror_s20_route_owner_anchor_and_support`
    - `Certification:MobilityComparison(lower_truth_owner_not_projection_truth)`
      remains the lower certification owner; projection only mirrors the route
      after runtime revalidates the same board, owner, route, anchor, support
      witness, and support squares
    - `space_access_liability_or_lower_certification_alone_is_non_admitting`
      and
      `wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_is_non_admitting`
      are live admission blockers
  - helpers:
    - `mobility_balance_count`
    - `mobility_gap_floor`
    - `restriction_support_gate`
  - forbids:
    - restriction geometry alone
    - space-clamp wording alone
    - bad-piece wording alone

- `ComparativeKingFragility`
  - scope: `comparative`
  - anchor: `board`
  - first live slice: home-wing king-theater asymmetry only
  - helpers:
    - `king_theater_fragility_bundle`
    - `king_fragility_asymmetry`
  - forbids:
    - `KingSafetyShell` alone
    - hole count alone
    - attack wording alone

- `CertifiedKingSafetyEdge`
  - scope: `comparative`
  - anchor: `board`
  - first live slice: `AttackScaffold` plus comparative king fragility plus
    explicit host, budget, move-order, and best-defense burdens
  - helpers:
    - `attack_host_viability`
    - `attacker_budget_present`
    - `move_order_relevance_gate`
    - `best_defense_survival`
    - `major_piece_presence`
  - forbids:
    - `AttackScaffold` alone
    - `ComparativeKingFragility` alone
    - phase proxy alone

- `MateNetCertification`
  - scope: `current_position`
  - anchor: `board`
  - first live slice: forcing mate-net certification only
  - helpers:
    - `mate_net_forcing_window`
    - `best_defense_survival`
  - forbids:
    - `AttackScaffold` alone
    - certified king-safety edge alone
    - mate-threat wording alone

- `MaterialHarvest`
  - scope: `current_position`
  - anchor: `board`
  - first live slice: realized non-king material conversion only, via a
    current-turn capture that the rival cannot immediately recapture
  - helpers:
    - `material_conversion_realization`
    - `best_defense_survival`
  - forbids:
    - `material gain` shell wording
    - tactical smell alone
    - `winning endgame` wording alone

- `WinningEndgame`
  - scope: `current_position`
  - anchor: `board`
  - first live slice: certified conversion/result verdict only, currently
    narrowed to a single non-rook-pawn runner with owner king support, owner to
    move, and no rival pawn counterplay so corner-draw rook-pawn shells and
    counter-races stay out
  - helpers:
    - `winning_endgame_conversion`
    - `best_defense_survival`
  - forbids:
    - `TradeInvariant` alone
    - `FortressHoldingShell` alone
    - material-edge wording alone

- `FortressDrawCertification`
  - scope: `current_position`
  - anchor: `board`
  - first live slice: `FortressHoldingShell`-backed hold certification only,
    with the draw burden still carried by explicit best-defense evidence rather
    than by shell presence alone; the validation corpus also keeps the slice on
    drawish exact boards via explicit fortress `maxAbsCp` budgets
  - helpers:
    - `fortress_draw_burden`
    - `best_defense_survival`
  - forbids:
    - `FortressHoldingShell` alone
    - `TradeInvariant` alone
    - draw wording alone

- `PerpetualCheckHolding`
  - scope: `current_position`
  - anchor: `board`
  - first live slice: stable perpetual-check hold only
  - helpers:
    - `perpetual_check_loop`
    - `best_defense_survival`
  - forbids:
    - checking-sequence wording alone
    - `AttackScaffold` alone
    - draw wording alone

- `PromotionRace`
  - scope: `current_position`
  - anchor: `board`
  - first live slice: kings-and-pawns-only clear-run promotion-race
    certification on top of `EndgameRaceScaffold`, using tempo plus
    rival-king-distance burden so non-king interceptors do not masquerade as
    route survival
  - helpers:
    - `promotion_route_survival`
    - `best_defense_survival`
  - forbids:
    - `EndgameRaceScaffold` alone
    - `PasserComplex` wording alone
    - `ConversionFunnel` wording alone

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

They are a separate side evidence channel consumed only in validation-side
checks at:

- `Root` broad-validation confound filtering for selected engine-required rows
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
- `S09` / `S23` / `S25` in the file, king, and rank-access cluster
- `S19` / `S22` / `S23` / `S24` in the endgame, simplification, and tactical
  conversion cluster

The boundary freeze for `S01-S25` now lives in
[StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md).

The explicit lower/support seed families required by current start-ready
strategy handoffs live in
[StrategySupportSeedInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategySupportSeedInventory.md).

The next projection step is therefore not renderer wording.

It is a carrier-coverage matrix and corpus plan that proves:

- the minimum required certified carriers for each strategy band
- the optional strengthening carriers for each strategy band
- the rival bands that must remain separable on exact boards

Current projection handoff status on this worktree is now frozen as:

- live-runtime start-ready rows, owned by `StrategyProjectionScopeContract` and
  `StrategyProjectionAdmission`:
  - `S01`
  - `S02`
  - `S03`
  - `S04`
  - `S05`
  - `S06`
  - `S07`
  - `S08`
  - `S09`
  - `S10`
  - `S11`
  - `S12`
  - `S13`
  - `S14`
  - `S15`
  - `S16`
  - `S17`
  - `S18`
  - `S19`
  - `S20`
  - `S21`
  - `S22`
  - `S23`
  - `S24`
  - `S25`
- S06 live runtime admission is part of that explicit live set and is owned by
  `StrategyProjectionAdmission` and `StrategyProjectionCoverageContract`:
  - required evidence name `space_bind_restriction_route_certified`
  - exact lower burden is current-board `structural_space_claim`, same-anchor
    outpost or short-run restriction linked to the same structural host, and
    support-only `SpaceBindRestrictionCertification` with matching
    `route_host_links`
  - exact S05 center-release and exact S20 domination false-rival rows remain
    non-admitting
  - coverage rows remain countable, but only exact same-anchor route evidence is
    runtime authority
- S09 live runtime admission is part of that explicit live set and is owned by
  `StrategyProjectionAdmission` and `StrategyProjectionCoverageContract`:
  - required evidence name:
    `file_penetration_route_certified`
  - row burden is frozen as
    `file_penetration_route_requires_owner_file_lane_state`,
    `file_penetration_route_requires_same_file_entry_or_penetration_consequence`,
    `same_task_projection_evidence_must_mirror_s09_owner_file_source_entry_and_route`,
    `s02_attack_s23_king_activity_s25_rank_access_or_optional_strengthening_is_non_admitting`,
    and
    `wrong_file_wrong_source_wrong_entry_wrong_route_stale_or_support_only_evidence_is_non_admitting`
  - lower carrier truth stays in `Witness:file_lane_state` and
    `Witness:rook_on_open_file_state`; the projection evidence companion is
    recorded as `ProjectionEvidence:file_penetration_route_certified`
  - `CertificationSupportOnly:MaterialHarvest|WinningEndgame` and
    `SupportOnly:AttackScaffold|CertifiedKingSafetyEdge` remain support-only
    and must not become projection truth owners
  - exact validation scaffold includes
    `wrong_file_wrong_source_wrong_entry_wrong_route_stale_or_support_only_evidence_not_counted`
    and
    `support_only_attack_shell_or_certification_only_not_counted`
  - runtime boundary remains narrow: broad S09 coverage rows are countable, but
    only rows carrying exact `file_penetration_route_certified` evidence are
    live admission authority; `S02`, `S23`, and `S25` adjacent rivals retain
    their independent meanings, `S01` keeps its separate exact
    `king_wing_storm_route_certified` branch, `S02` keeps its separate
    `king_ring_concentration_route_certified` branch, `S03` keeps its separate
    `diagonal_king_attack_route_certified` branch, and `S04` keeps its separate
    exact `king_shelter_breach_route_certified` branch
- S10 live runtime admission is part of that explicit live set and is owned by
  `StrategyProjectionAdmission` and `StrategyProjectionCoverageContract`:
  - required evidence name `outpost_occupation_route_certified`
  - exact lower burden is same-anchor `weak_outpost_square_state` with
    same-anchor `knight_on_outpost_square`; lower weak-outpost witness truth
    stays below projection truth
  - exact same-anchor route evidence must mirror the same anchor, same outpost
    square, and `knight_only_outpost_occupancy` or
    `same_anchor_eviction_denial`
  - exact S12 access rows remain non-admitting
  - coverage rows remain countable, but only exact same-anchor outpost route
    evidence is runtime authority
- S12 has narrow live runtime admission for exact local access-superiority:
  - `S12` is listed in
    `StrategyProjectionScopeContract.startReadyBandIds` and
    `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`
  - live evidence name:
    `local_access_superiority_route_certified`
  - row-specific burden is frozen as
    `access_route_requires_same_anchor_weak_outpost_or_diagonal_lane`,
    `access_route_requires_support_linked_short_run_restriction_reinforcement`,
    `local_access_superiority_requires_same_owner_anchor_route_and_support`,
    `same_task_projection_evidence_must_mirror_s12_owner_anchor_route_and_support`,
    `s03_diagonal_attack_s10_outpost_s20_mobility_or_optional_strengthening_is_non_admitting`,
    and
    `wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_is_non_admitting`
  - lower carrier truth stays below projection:
    `weak_outpost_square_state` or king-independent `diagonal_lane_only`, plus
    support-linked `short_run_slider_gate_restriction`; `MobilityComparison` is
    `CertificationSupportOnly:MobilityComparison(non_truth_owner)` and
    optional `knight_on_outpost_square` remains support/rival context only
  - exact validation proves
    `projection_evidence_mirrors_s12_owner_anchor_route_and_support`,
    `wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_not_counted`,
    and S03/S10/S20 false-rival rejection
  - runtime boundary remains narrow: broad S12 coverage rows are countable, but
    only rows carrying exact `local_access_superiority_route_certified` evidence
    are live admission authority; coverage-only rows and adjacent non-live bands
    remain fail-closed
- S01-S25 projection coverage owner:
  `StrategyProjectionCoverageContract` owns the row-specific coverage gates,
  lower-carrier ownership, helper/admission laws, and exact-validation
  scaffolds for every `S01-S25` band, with
  `allProjectionBandIds` fixed to exactly that full range
- current S01/S02/S03/S04/S05/S06/S07/S08/S09/S10/S11/S12/S13/S14/S15/S16/S17/S18/S19/S20/S21/S22/S23/S24/S25 blockers are closed at the runtime start-ready handoff boundary:
  - `S01` has same-anchor king-wing storm admission evidence and projection
    validation scaffold, with lower truth supplied by current
    `available_lever_trigger`, current `pawn_push_break_contact_source`,
    same-defending-king `AttackScaffold`, and certified same-owner
    `CertifiedKingSafetyEdge`
  - `S03` has same-king diagonal attack admission evidence and projection
    validation scaffold, with lower truth supplied by current king-theater
    `diagonal_lane_only`, same-defending-king `AttackScaffold`, and certified
    same-owner `ComparativeKingFragility` plus `CertifiedKingSafetyEdge`
  - `S05` has same-anchor center-release admission evidence and projection
    validation scaffold, with lower truth supplied by current
    `available_lever_trigger` plus current `pawn_push_break_contact_source` on
    the same center-file source and center-file target
  - `S11` has same-target weak-pawn pressure/persistence admission evidence
    and projection validation scaffold, with lower truth supplied by current
    `weak_pawn_target_state` plus current-board legal pressure and fixed-pawn
    persistence on the same target square
  - `S13` has same-wing-sector wing-damage admission evidence and projection
    validation scaffold, with lower truth supplied by current
    `sector_asymmetry_state`, same-sector `available_lever_trigger`, same-anchor
    `pawn_push_break_contact_source`, and exact-board target-role recomputation
  - `S14` has same-anchor chain-base admission evidence and projection
    validation scaffold, with lower truth supplied by current base-contact
    lever/source and exact-board non-center chain-base target recomputation
  - `S15` has same-candidate passer-creation admission evidence and projection
    validation scaffold, with lower truth supplied by current root
    `candidate_passer` plus exact-board S13 wing-damage or S14 chain-base
    route recomputation
  - `S16` has same-enemy-passer suppression admission evidence and projection
    validation scaffold, with lower truth supplied by current-board enemy
    `passed_pawn_entity_state` plus exact blockade, restriction-hold, or
    non-losing-race proof and lower certification support
  - `S17` has same-piece liability-relief admission evidence and projection
    validation scaffold
  - `S18` has exact current-board bishop-pair conversion admission evidence
    and projection validation scaffold, with lower conversion proof supplied by
    same-board `InitiativeWindow`, `MobilityComparison`, or bishop-member
    `MaterialHarvest`
  - `S19` has delta-backed simplification admission evidence and projection
    validation scaffold, with `TradeInvariant` supplied by canonical exact
    before/move/after delta extraction and lower material/hold proof supplied by
    exact current-board certification bundles
  - `S22` has certified fortress/perpetual hold evidence and projection
    validation scaffold, with lower hold proof supplied by a same-root
    `CertificationEvidenceBundle` rather than fabricated inside projection
    admission
  - `S23` has same-entry / same-contact king-activity admission evidence and
    projection validation scaffold
  - `S24` has same-target forcing + conversion admission evidence and
    projection validation scaffold
  - `S25` has same-source, same-entry, same-kind rank-access evidence and
    projection validation scaffold

This status split is contract-only.

It authorizes narrow first-slice projection work plus projection-corpus
authoring only.

It does **not** make projection broad-deployed or widen runtime beyond the
explicit admission branches.

Executable live projection admission remains the narrower
`StrategyProjectionScopeContract` / `StrategyProjectionAdmission` surface. On
the current worktree that live-admission surface is still limited to
`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25`. Other rows named
start-ready, promoted, or coverage-complete above are semantic / corpus-handoff
statuses unless they are explicitly added to that runtime admission surface.
Rows outside that explicit `S01-S25` live set stay outside live admission, and
within `S01-S25` only the exact narrow admission slices are runtime authority;
their broad exact-board corpus rows remain countability authority and do not
widen runtime.

No coverage-complete-only projection band is broad-deployed or live as runtime
projection on the current branch. `S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`,
`S20`, `S21`, `S22`, `S23`, `S24`, and `S25` keep only their already-authored narrow live admission
slices.

`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`,
`S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`,
`S23`, `S24`, and `S25` are now coverage-complete against their current clustered
positional-access through passer creation/suppression coverage gate buckets. That status is
corpus/validation-only and does not add broad runtime projection admission or broad
deployment for those bands; for `S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`,
`S22`, `S23`, `S24`, and `S25` it does not widen their existing narrow live admission
slices.

The conversion / simplification / holding coverage cluster `S17` / `S18` /
`S19` / `S22` / `S24` now has executable broad coverage rows in
`projection-expectations.jsonl`, with the gate contract in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`.
That contract freezes:

- row-specific broad coverage gates
- lower-carrier ownership by support seed, object, delta, and certification
  layer
- S18 live runtime admission freeze:
  - S18 is in `StrategyProjectionScopeContract.startReadyBandIds`; its
    `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` entry is
    limited to the three explicit bishop-pair conversion evidence kinds, and
    `StrategyProjectionAdmission` rejects all shortcut, adjacent-rival, stale,
    or lower-carrier-only bundles
  - lower-carrier ownership is exact current-board active-member
    `bishop_pair_state(same_owner_current_board_substrate)`, with
    `WitnessSeed:None`, `Object:None`, `Delta:None`, and lower certification
    truth owned by `InitiativeWindow|MobilityComparison|MaterialHarvest`;
    `WinningEndgame` is support-only future carrier because current lower law
    cannot certify it on a board that still has bishops
  - row-specific burden names:
    `initiative_route_requires_bishop_pair_state_and_same_board_initiative_window`,
    `structure_route_requires_bishop_pair_state_and_same_board_mobility_comparison`,
    `material_route_requires_bishop_pair_member_material_harvest`,
    `projection_evidence_mirrors_conversion_family_targets_and_bishop_pair`,
    and `relation_only_optional_strengthening_or_adjacent_rival_is_non_admitting`
  - live projection evidence-kind names:
    `bishop_pair_initiative_conversion_certified`,
    `bishop_pair_structure_conversion_certified`, and
    `bishop_pair_material_conversion_certified`
  - helper and exact-scaffold freezes use
    `same_current_board_bishop_pair_conversion_law`,
    `s18_projection_evidence_same_task_same_target_law`,
    `conversion_certification_is_lower_truth_owner_law`,
    `active_bishop_pair_member_present`,
    `same_current_board_conversion_certification_present`,
    `projection_evidence_mirrors_conversion_family_targets_and_bishop_pair`,
    `material_conversion_bishop_pair_member_capture_present`,
    `initiative_or_structure_conversion_support_targets_present`, and
    `bishop_pair_minor_edge_or_optional_strengthening_only_not_counted`
- exact delta companion fields for `S19` `TradeInvariant` rows
- S19 live admission burden law:
  - `material_route_requires_canonical_trade_invariant_and_same_move_material_harvest`
  - `hold_route_requires_canonical_trade_invariant_and_post_trade_fortress_certification`
  - `result_only_material_only_or_adjacent_rival_is_non_admitting`
  - live projection evidence-kind names
    `trade_invariant_material_simplification_certified` and
    `trade_invariant_hold_simplification_certified`
  - those S19 evidence-kind names exactly match
    `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`; S19
    admission remains fail-closed without canonical delta evidence plus the
    same-task current-board certification carrier
- S22 row-specific burden law:
  - same-holder `FortressHoldingShell` plus certified
    `FortressDrawCertification`, or certified current-board
    `PerpetualCheckHolding`
  - `SupportOnly` / `Deferred` hold evidence remains non-admitting
  - exact S19 `TradeInvariant` rivals, checking-sequence shortcuts, and
    deferred perpetual evidence are explicit rejected projection rows
- S22 live projection evidence-kind names:
  `fortress_hold_certified` and `perpetual_hold_certified`; these exactly match
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` and do not
  replace lower object/certification truth ownership or synthesize
  best-defense certification proof
- fail-closed separation between broad coverage candidates and live
  `StrategyProjectionAdmission`

This coverage closure does not make the cluster broad-deployed. `S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`,
`S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25` keep narrow
live start-ready admission companions; adjacent coverage-only rows remain
fail-closed.

The initiative / release / counterplay cluster `S05` / `S07` / `S08` / `S21`
now has executable coverage-complete rows in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`.
That contract freezes:

- row-specific coverage burdens for central release, development-led
  initiative, exact rival-source denial, and counterplay survival
- shared helper laws:
  - `S05` uses same-anchor center-file source/target contact plus
    `center_release_route_certified`; projection evidence mirrors the same
    source, target, and center-release route
  - `S07` uses same-owner `DevelopmentComparison` plus certified
    `InitiativeWindow`; S07 live runtime admission uses board-anchored
    `initiative_conversion_route_certified` evidence while keeping
    `OpeningDevelopmentRegime` support-only and lower certifications as truth
    owners
  - `S08` uses exact rival release-source suppression rather than
    `InitiativeWindow` wording alone. S08 live runtime admission uses
    `counterplay_denial_route_certified`; evidence must mirror the same rival
    source, target, and denial route, and the same board must not carry an
    owner-side S21 survival carrier. `InitiativeWindow` remains lower
    Certification support, not projection truth.
  - `S21` uses non-center owner `pawn_push_break_contact_source` plus
    same-board certified `InitiativeWindow`; center-source/center-target release
    remains S05-adjacent, and certified `InitiativeWindow` alone is not S21 proof
- lower-carrier ownership as witness, object-support, certification, or
  projection-validation ownership; projection does not become the truth owner
  for lower Object / Delta / Certification facts
- exact validation scaffold counting for the authored JSONL rows
- fail-closed runtime separation: `S05` admits only through its explicit
  center-release branch; S07 live runtime admission admits only through exact
  `initiative_conversion_route_certified` evidence and certified lower
  DevelopmentComparison / InitiativeWindow support; S08 live runtime admission
  admits only through exact `counterplay_denial_route_certified` evidence and
  same-board InitiativeWindow support when owner-side S21 survival is absent;
  S21 live runtime admission admits only through exact
  `counterplay_survival_route_certified` evidence.
- S07 live runtime admission keeps `OpeningDevelopmentRegime` support-only and
  keeps `DevelopmentComparison` and certified `InitiativeWindow` as lower
  Certification truth owners. The route task is `development_led_window` or
  `move_right_window`; S08 rival-source denial, owner target-state leakage,
  development-only, initiative-only, stale evidence, and optional strengthening
  remain non-admitting.
- S21 live runtime admission: `counterplay_survival_route_certified` is listed
  in `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`; admitted
  S21 corpus rows carry exact same owner/source/target/route/certification
  evidence, while certified `InitiativeWindow` alone is not S21 proof
- S05 live-admission freeze: row-specific burden, helper law, lower-carrier
  ownership, exact validation scaffold, runtime fail-closed boundary, and live
  evidence kind `center_release_route_certified` are now frozen.
  `CentralContactFront`, `InitiativeWindow`, and file support remain lower
  support only and do not become S05 projection truth owners. The frozen
  evidence kind is present in
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`.

`ProjectionExpectationCorpus.requiredCoveragePairsFor`,
`coveragePairsByBand`, `missingRequiredCoveragePairsByBand`, and
`bandsWithCompleteCoverage` now count `S05`, `S07`, `S08`, and `S21`
as completed coverage-complete bands once the exact-board JSONL rows are
loaded.

The king-attack family `S01` / `S02` / `S03` / `S04` now has executable
coverage-complete rows in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`.
The JSONL rows are authored and countable. `S01`, `S02`, `S03`, and `S04` also
have narrow live runtime admission branches. That contract freezes:

- row-specific coverage burdens for same-wing storm, direct king-ring
  concentration, king-facing diagonal attack, and same-king shelter breach
- shared helper laws:
  - `S01` requires non-center same-wing contact plus same-defender attack / king-safety
    carriers; castling context is not proof. Its live freeze names
    `king_wing_storm_route_certified` as exact projection evidence, with
    `same_task_projection_evidence_must_mirror_s01_source_target_defending_king_and_route_law`
    and
    `s05_center_release_or_s21_counterplay_survival_is_not_s01_storm_law`
    enforced at admission
  - `S02` requires direct king-ring concentration; `AttackScaffold`,
    `CertifiedKingSafetyEdge`, and lane support cannot become projection truth
    owners by themselves. Its live freeze names
    `king_ring_concentration_requires_same_defending_king_attack_scaffold`,
    `king_ring_concentration_requires_certified_same_owner_king_safety_edge`,
    `same_task_projection_evidence_must_mirror_s02_owner_defending_king_ring_targets_source_set_and_route`,
    `s03_diagonal_s04_shell_s09_file_penetration_or_optional_strengthening_is_non_admitting`,
    and
    `wrong_owner_wrong_king_wrong_targets_wrong_sources_wrong_route_stale_or_support_only_evidence_is_non_admitting`;
    `king_ring_concentration_route_certified` is live-allowed only for S02 and
    must mirror the same defending king, source set, king-ring target set, and
    route
  - `S03` requires king-theater `diagonal_lane_only` plus same-king fragility
    and king-safety certification; bishop-pair state remains support/contrast
    only. Its live freeze is exact and non-broad:
    `diagonal_king_attack_requires_king_theater_diagonal_lane`,
    `diagonal_king_attack_requires_same_defending_king_attack_scaffold`,
    `diagonal_king_attack_requires_comparative_fragility_and_certified_edge`,
    `same_task_projection_evidence_must_mirror_s03_owner_defending_king_diagonal_source_entry_squares_and_route`,
    `s02_concentration_s12_local_access_bishop_pair_or_non_king_diagonal_is_non_admitting`,
    and
    `attack_scaffold_certification_diagonal_only_stale_or_wrong_task_evidence_is_non_admitting`.
    The live evidence kind `diagonal_king_attack_route_certified` is present in
    `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` only for S03
    and must mirror the same owner, defending king, diagonal source, king-ring
    entry/endpoints, and route. Stale, wrong-owner, wrong-king, wrong-source,
    wrong-route, object-only, certification-only, bishop-pair-only, S02
    concentration, and S12 local-access rows remain fail-closed. Lower carriers
    remain `ObjectSupportOnly:AttackScaffold(non_truth_owner)` and
    `CertificationSupportOnly:ComparativeKingFragility|CertifiedKingSafetyEdge(non_truth_owner)`.
  - `S04` requires same-king shell breach from the `KingSafetyShell` payload or
    a same-king support-break route plus same-defender king-safety
    deterioration; `KingSafetyShell` is required defender lower object support
    for S04 positives, not optional strengthening or projection-owned truth.
    The support-break broad row also declares exact diagonal-lane support, while
    `shell_payload_and_support_break_use_distinct_declared_support_burdens`.
    Its live freeze names
    `king_shelter_breach_requires_same_defender_king_safety_shell`,
    `king_shelter_breach_requires_shell_payload_or_support_break_route`,
    `king_shelter_breach_requires_same_defender_certified_king_safety_edge`,
    `same_task_projection_evidence_must_mirror_s04_owner_defending_king_shell_anchor_breach_squares_and_route`,
    `s01_storm_s02_concentration_s03_diagonal_s24_tactic_or_optional_strengthening_is_non_admitting`,
    and
    `wrong_owner_wrong_defender_wrong_shell_wrong_route_stale_or_support_only_evidence_is_non_admitting`
    as the row-specific live-admission burden
- lower-carrier ownership as witness, object-support, certification-support,
  support-only, or projection-validation ownership; S-layer projection does not
  own lower Object / Delta / Certification truth. For S01,
  `ObjectSupportOnly:AttackScaffold(non_truth_owner)` and
  `CertificationSupportOnly:CertifiedKingSafetyEdge(non_truth_owner)` remain
  lower support; live `ProjectionEvidence:king_wing_storm_route_certified`
  admits only when the exact same non-center source, target, defending king, and route are
  mirrored
- for S03, `ObjectSupportOnly:AttackScaffold(non_truth_owner)` and
  `CertificationSupportOnly:ComparativeKingFragility|CertifiedKingSafetyEdge(non_truth_owner)`
  remain lower support; live
  `ProjectionEvidence:diagonal_king_attack_route_certified` admits only when
  the exact same owner, defending king, diagonal source, entry/endpoints, and
  route are mirrored
- for S04,
  `ObjectSupportOnly:KingSafetyShell(non_truth_owner)`,
  `CertificationSupportOnly:CertifiedKingSafetyEdge(non_truth_owner)`, and
  `SupportOnly:AttackScaffold|ComparativeKingFragility|same_anchor_contact_target`
  remain lower support. `ProjectionValidation:shell_payload_breach|support_break_breach(same_defender_king)`
  is the route-validation burden, and
  `ProjectionEvidence:king_shelter_breach_route_certified`
  admits only after exact current-board recomputation of the same owner,
  defending king, shell anchor, breach squares, route, and certification family
- exact validation scaffold names for all four rows, with same-board carrier
  checks in `ProjectionExpectationCorpusTest`
- fail-closed runtime separation: S01 is live only through its exact
  same-anchor `king_wing_storm_route_certified` branch. S02 is live only
  through exact same-king `king_ring_concentration_route_certified` evidence.
  S03 is live only through exact same-king
  `diagonal_king_attack_route_certified` evidence. S04 is live only through
  exact same-defender `king_shelter_breach_route_certified` evidence; broad S04
  coverage rows remain countable, but evidence-empty, stale, wrong-owner,
  wrong-defender, wrong-shell, wrong-route, support-only, optional
  strengthening, and adjacent S01/S02/S03/S24 rows remain fail-closed.
  S02's runtime boundary rejects stale evidence,
  lower object/certification support as projection truth, optional
  strengthening alone, and adjacent S03/S04/S09 rows without S02 projection
  evidence.
  S01's runtime boundary rejects wrong source,
  wrong target, wrong defending king, wrong route, stale evidence,
  support-only lower bundles, center-edge release, S05 center-release rivals,
  S21 counterplay rivals, castling-shell shortcuts, and optional strengthening.

`ProjectionExpectationCorpus.requiredCoveragePairsFor`, `coveragePairsByBand`,
`missingRequiredCoveragePairsByBand`, and
`bandsWithCompleteCoverage` now count the exact-board S01-S04 rows as
completed coverage-complete once `projection-expectations.jsonl` is loaded.

The pawn-target / structural-damage cluster `S11` / `S13` / `S14` now has an
executable coverage-complete gate in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`.
The JSONL rows are authored and countable. `S11`, `S13`, and `S14` also have
narrow live `StrategyProjectionAdmission` slices. The
contract freezes:

- row-specific coverage burdens for same-target weak-pawn pressure, wing-damage
  leverage against non-chain-base phalanx-edge or structurally burdened targets,
  and chain-base contact
- shared helper laws that keep target identity exact-board-bound:
  - `S11` requires weak-pawn target, pressure, and the current fixed-pawn
    persistence proof to remain on the same square
  - `S13` requires sector asymmetry plus same-wing-sector lever/contact source,
    with target role recomputed from the exact board rather than inherited from
    pawn-structure narration
  - `S14` requires base-contact lever/source and exact-board recomputation of
    the contact target as a non-center `chain_base_target`; the
    `base_contact_continuation` route additionally requires the target's
    forward-supported pawn to continue the defender chain on the same board
- lower-carrier ownership as witness, certification-support, support-only, or
  projection-validation ownership; S-layer projection does not own lower
  Object / Delta / Certification truth and does not turn structural shell
  vocabulary into admission proof
- exact validation scaffold checks for the authored JSONL rows
- fail-closed runtime separation: `S14`, `S15`, and `S16` admit only through
  their explicit start-ready branches; adjacent coverage-only rows remain
  unsupported by live admission

S14 also has a live admission freeze, owned in
`StrategyProjectionCoverageContract`, `StrategyProjectionScopeContract`, and
`StrategyProjectionAdmission`. Its row-specific burden tokens are
`chain_base_route_requires_same_anchor_lever_and_contact_source`,
`chain_base_route_requires_recomputed_non_center_chain_base_target`,
`base_contact_continuation_requires_same_source_target_pair`,
`projection_evidence_mirrors_owner_source_target_and_chain_base_route`, and
`fixed_chain_structural_shell_adjacent_rival_or_optional_strengthening_is_non_admitting`.
The live evidence kind is `chain_base_contact_route_certified`, and it is the
S14 entry in `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`.
Its helper/law tokens are
`same_anchor_chain_base_contact_law`,
`non_center_chain_base_role_recomputed_from_exact_board_law`,
`base_contact_must_bind_same_source_and_target_law`,
`same_task_projection_evidence_must_mirror_s14_source_target_and_route_law`,
and `fixed_chain_or_structural_damage_shell_is_not_truth_owner_law`. Its lower
carrier ownership is
`Witness:available_lever_trigger(base_contact)`,
`Witness:pawn_push_break_contact_source(same_anchor_chain_base_target)`,
`ProjectionValidation:non_center_chain_base_target_recomputed|base_contact_continuation`,
and `SupportOnly:weak_pawn_target_state|structural_damage_shell|fixed_chain_context`.
Its exact validation scaffold is
`base_contact_lever_and_break_contact_present`,
`contact_target_role_is_non_center_chain_base`,
`base_contact_continuation_same_anchor_present`,
`fixed_chain_or_structural_damage_shell_only_not_counted`, and
`stale_or_adjacent_runtime_evidence_not_counted_before_live_admission`. The
freeze prevents S05 center-release overlap, S11 weak-pawn leakage, S13 wing
damage leakage, S15 passer-creation leakage, optional-strengthening proof,
stale evidence, and Object / Delta / Certification truth-owner leakage from
admitting S14. The admission branch recomputes the exact non-center chain-base
target and requires projection evidence to mirror the same contact source,
target, route token, and exact chain-base forward support squares. The route
token is not optional prose: `chain_base_target` is admitted only when the base
has no further forward chain continuation, while `base_contact_continuation`
is admitted only when the forward-supported pawn itself supports the next
defender pawn on the exact board.

S13 now has a live runtime-admission contract executable in
`StrategyProjectionCoverageContract`, `StrategyProjectionScopeContract`, and
`StrategyProjectionAdmission`. This closes the live promotion boundary for the
row-specific burden, helper/law, lower-carrier ownership, exact validation
scaffold, runtime boundary, and projection evidence kind:

- row-specific burden names:
  - `wing_damage_route_requires_same_sector_asymmetry_and_same_anchor_contact_source`
  - `wing_damage_route_excludes_center_sector_targets`
  - `phalanx_edge_route_requires_recomputed_phalanx_edge_target`
  - `phalanx_edge_route_excludes_chain_base_targets`
  - `burdened_target_route_requires_recomputed_structurally_burdened_target`
  - `projection_evidence_mirrors_owner_source_target_sector_and_damage_route`
  - `asymmetry_weak_pawn_adjacent_rival_or_optional_strengthening_is_non_admitting`
- live projection evidence kind:
  - `wing_damage_route_certified`
- helper / scaffold names:
  - `same_sector_asymmetry_plus_contact_source_law`
  - `wing_damage_target_role_recomputed_from_exact_board_law`
  - `phalanx_edge_target_excludes_chain_base_law`
  - `center_sector_damage_is_not_wing_damage_law`
  - `same_task_projection_evidence_must_mirror_s13_source_target_sector_and_route_law`
  - `asymmetry_or_preexisting_weak_pawn_is_not_damage_law`
  - `sector_asymmetry_present`
  - `same_sector_lever_and_break_contact_present`
  - `wing_sector_damage_route_present`
  - `contact_target_role_is_non_chain_base_phalanx_edge_or_structurally_burdened`
  - `weak_pawn_or_asymmetry_only_not_counted`
  - `stale_or_adjacent_runtime_evidence_not_counted_before_live_admission`
- lower ownership:
  `sector_asymmetry_state`, same-wing-sector `available_lever_trigger`, and
  same-anchor `pawn_push_break_contact_source` are witness carriers;
  non-chain-base `phalanx_edge_target` and `structurally_burdened_target` are
  exact-board projection-validation roles; `weak_pawn_target_state` and
  `file_lane_state` are support-only; no support seed, Object, Delta, or
  Certification family is an S13 projection truth owner
- runtime boundary:
  S13 is in `StrategyProjectionScopeContract.startReadyBandIds`, outside
  `coverageOnlyBandIds`, and its
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` entry is
  limited to `wing_damage_route_certified`. `StrategyProjectionAdmission`
  admits only when the current-board extractor revalidates same-sector
  asymmetry, same-anchor lever/contact source, exact target role, and matching
  projection evidence; stale evidence, optional strengthening, and adjacent
  S11/S14/S15 rivals remain non-admitting.

S11 has a live runtime-admission contract executable in
`StrategyProjectionCoverageContract`, `StrategyProjectionScopeContract`, and
`StrategyProjectionAdmission`:

- row-specific burden names:
  - `target_pressure_route_requires_same_square_weak_pawn_target_and_owner_pressure`
  - `fixation_route_requires_current_fixed_pawn_persistence_on_same_target`
  - `repeated_pressure_route_requires_two_owner_attackers_on_same_target`
  - `projection_evidence_mirrors_weak_pawn_target_pressure_and_persistence`
  - `weak_pawn_only_target_swap_adjacent_rival_or_optional_strengthening_is_non_admitting`
- live projection evidence kind:
  - `weak_pawn_target_pressure_persistence_certified`
- helper / scaffold names:
  - `fixed_pawn_persistence_is_current_board_carrier_not_certification_truth_law`
  - `same_task_projection_evidence_must_mirror_s11_target_pressure_and_persistence_law`
  - `pressure_without_fixed_persistence_not_counted`
- runtime boundary:
  S11 is in `StrategyProjectionScopeContract.startReadyBandIds`, outside
  `coverageOnlyBandIds`, and its
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` entry is
  limited to `weak_pawn_target_pressure_persistence_certified`
- lower ownership:
  `weak_pawn_target_state` is the current-board witness carrier only;
  pressure and fixed-pawn persistence are projection-validation carriers;
  `MobilityComparison` and `InitiativeWindow` are support-only and must not
  become S11 projection truth owners

S15 now has a live runtime-admission contract executable in
`StrategyProjectionCoverageContract`, `StrategyProjectionScopeContract`, and
`StrategyProjectionAdmission`:

- row-specific burden names:
  - `creation_route_requires_same_owner_candidate_passer_anchor`
  - `s13_route_requires_same_candidate_contact_source_and_recomputed_wing_damage_target`
  - `s14_route_requires_same_candidate_contact_source_and_recomputed_chain_base_target`
  - `projection_evidence_mirrors_candidate_source_target_and_creation_route`
  - `candidate_shell_existing_passer_split_anchor_optional_strengthening_or_adjacent_rival_is_non_admitting`
- live projection evidence kind:
  - `passer_creation_route_certified`
- helper / scaffold names:
  - `same_candidate_passer_contact_source_route_law`
  - `per_position_creation_route_disjunction_broad_coverage_conjunction_law`
  - `candidate_create_shell_existing_passer_or_split_anchor_is_not_creation_law`
  - `upper_passer_consequence_is_not_truth_owner_law`
  - `candidate_passer_same_anchor_present`
  - `same_candidate_s13_or_s14_creation_route_present`
  - `live_passer_creation_evidence_same_candidate_present`
- runtime boundary:
  S15 is in `StrategyProjectionScopeContract.startReadyBandIds`, outside
  `coverageOnlyBandIds`, and its
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` entry is
  limited to `passer_creation_route_certified`
- lower ownership:
  `candidate_passer` remains root truth, same-candidate S13/S14 route roles are
  projection-validation carriers, `create passer` is shell support only, and
  `PasserComplex`, `ConversionFunnel`, and `PromotionRace` are support-only
  consequences rather than S15 projection truth owners

`ProjectionExpectationCorpus.requiredCoveragePairsFor` and
`coveragePairsByBand`, `missingRequiredCoveragePairsByBand`, and
`bandsWithCompleteCoverage` now count `S11`, `S13`, and `S14` as
completed coverage-complete once the exact-board JSONL rows are loaded.

The passer creation / passer suppression pair `S15` / `S16` now has executable
passer creation/suppression coverage-complete rows in `projection-expectations.jsonl`, with the
gate contract in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`.
S15 and S16 also have narrow live `StrategyProjectionAdmission` branches. The
contract freezes:

- row-specific coverage-complete burdens:
  - `S15` creation requires both broad coverage buckets
    `s13_wing_damage` and `s14_chain_base`, even though live per-position
    admission may use either route. In every exact route, the
    `candidate_passer` pawn must be the same pawn as the S13/S14 contact
    source; false-rival coverage also separates `S13`, `S14`, and `S16`
  - `S16` suppression requires `blockade_hold`, `restriction_hold`, and
    `non_losing_race` broad coverage buckets, plus a row-specific live
    admission burden freeze: live admission must bind the same enemy
    `passed_pawn_entity_state` anchor to the blockade, restriction-hold, or
    non-losing-race route, `blockade_hold` requires fortress certification
    support targeting the same passer/blocker shell, and `restriction_hold`
    additionally requires the blocker square to appear in the owner
    `short_run_slider_gate_restriction` gate-square payload while lower
    hold/race certifications remain
    same-board support rather than passer truth owners; enemy-passer, blocker,
    restriction, hold, race, adjacent-rival, or optional-strengthening facts
    alone remain rejected
- shared helper laws:
  - `S15` keeps `candidate_passer` as root truth, `create passer` as shell
    support, and upper passer consequences out of projection truth ownership
  - `S16` keeps enemy `passed_pawn_entity_state` as entity truth and requires
    certified hold or race proof before suppression can count
- lower-carrier ownership as root, witness, object-support,
  certification-support, shell-support, support-only, or projection-validation
  ownership; S-layer projection does not own lower Object / Delta /
  Certification truth
- exact validation scaffold checks for the authored JSONL rows
- S15 live runtime evidence kind freeze:
  `passer_creation_route_certified` is the S15 entry in
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`
- S16 live projection evidence kind freeze:
  `passer_suppression_route_certified` is frozen in
  `StrategyProjectionCoverageContract.declaredProjectionEvidenceKindsByBand` and
  is the only S16 entry in
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`; the live
  branch revalidates the same enemy passer and route-specific proof without
  turning same-board hold/race certifications into S16 truth owners
- fail-closed runtime separation: `ProjectionExpectationCorpus` tracks `S15`
  and `S16` in `broadReadyCoverageGateBands`, reports no missing required
  pairs for either band, expects S15 and S16 admitted rows to require exact
  live evidence, and still expects coverage-only bands outside the live set to
  be rejected as unsupported

The methodology owner is `ValidationMethodology.md`; band-local breadth gates
for `S01-S25` are now frozen in `StrategyProjectionBoundaryMatrix.md`.

The projection corpus validator computes required bucket presence for the
S01-S25 projection coverage closure set over the clustered positional-access, conversion/hold/target, initiative/release/counterplay, king-attack,
pawn-structure, and passer creation/suppression completed executable gate-band rows (`S01`, `S02`, `S03`,
`S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`,
`S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and
`S25`) from the JSONL corpus. It currently reports all twenty-five completed
clustered bands as coverage-complete.
Coverage counting is axis-burdened: positive route buckets require
`exact` / `admitted` rows, while near-miss and shortcut-negative buckets require
their corresponding rejected row classes.
The king-attack set (`S01`, `S02`, `S03`, and `S04`), pawn-structure set (`S11`, `S13`,
and `S14`), and passer creation/suppression set (`S15` and `S16`) are executable and counted as
complete coverage. `S01`, `S02`, `S03`, and `S04` in that king-attack
set have narrow live runtime admission branches. `S11`, `S13`, and `S14` in
that pawn-structure set have narrow live runtime admission branches. `S15` and `S16` in passer creation/suppression also have
narrow live runtime admission branches. S16 is in
`StrategyProjectionScopeContract.startReadyBandIds`, has the
`passer_suppression_route_certified` evidence entry in
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand`, and has a
`StrategyProjectionAdmission` branch. That branch remains narrow: it admits
only exact current-board same-enemy-passer suppression routes and does not let
hold/race certification or object shells become projection truth owners.

The previous broad blocker set `S06`, `S10`, `S12`, and `S15` is closed at
gate-decision level only. `S10` is now narrow live only for same-anchor
knight-outpost occupation routes with `outpost_occupation_route_certified`
evidence, remains knight-only for current-branch scope, and does not widen
into generic weak-square access or non-knight outpost claims. `S15` uses disjunctive per-position route admission but conjunctive
broad-coverage over both frozen creation routes plus `S13` / `S14` / `S16`
false-rival separation and candidate-only / shell-only / existing-passer /
split-anchor negatives. `S15` and `S16` are now clustered as completed passer creation/suppression
coverage bands, with both promoted to narrow live admission. `S09`, `S20`, `S23`, and
`S25` are tracked in positional-access alongside `S06`,
`S10`, and `S12`; `S25` remains limited to the `cross_wing_rank_switch`
rank-access route for the current broad gate, while wider rank-access meanings
remain future scope.

Current broader-coverage corpus debt is closed by the S01-S25 projection coverage closure over
the clustered positional-access through passer creation/suppression clusters: `S01`, `S02`, `S03`, `S04`, `S05`,
`S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`,
`S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25` have
exact-board route, same-cluster near-miss / false-rival, and shortcut-negative
coverage against the current frozen gates.
That still does not make them broad-deployed or newly live as runtime projection
bands. `S10` remains narrow-by-design live only for same-anchor knight-outpost
occupation routes with `outpost_occupation_route_certified` evidence mirrored
to the same anchor and outpost square. `S15` remains narrow-by-design live only
for same-candidate creation routes, and `S16` remains narrow-by-design live only for same-enemy-passer
suppression routes with `passer_suppression_route_certified` evidence mirrored
to the route-specific proof.
S20 is additionally live-admitted only for the exact mobility-domination slice:
`same_board_mobility_certification_and_support_witness_law`,
`same_task_projection_evidence_must_mirror_s20_route_owner_anchor_and_support_law`,
`mobility_comparison_is_lower_certification_not_projection_truth_owner_law`,
`future_projection_evidence_mirrors_route_owner_anchor_and_support`, and
`wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_not_counted`
are enforced by `StrategyProjectionAdmission`; adjacent coverage-only rows
remain unsupported unless they have their own explicit live branch.

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
