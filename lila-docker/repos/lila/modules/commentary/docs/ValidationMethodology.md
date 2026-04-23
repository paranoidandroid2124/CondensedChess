# Validation Methodology

This document defines how the `commentary` backend will be validated over many
exact chess positions without falling back into ad hoc reruns and layered
patches.

The branch-level design-freeze summary lives in
[DecisionFreezeLedger.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DecisionFreezeLedger.md).

The goal is not only to know whether a claim is right.

The goal is to know exactly which layer failed:

- root extraction
- witness admission
- object composition
- delta emission
- certification
- projection handoff
- planner admission
- surface replay

## Validation Philosophy

The methodology inherits three strict lessons from the previous branches.

1. validation must be exact-board, never verbal-motif-only
2. best-defense matters whenever the claim is persistence-bearing,
   denial-bearing, or comparative
3. replay surfaces must consume carried contracts, not recomposed meaning

## Canonical Validation Ladder

The new backend validates one layer at a time.

### Layer 0: Root-State Validation

Purpose:

- prove deterministic extraction of the `2891`-dimensional root-state vector

Checks:

- bit presence or absence
- count integrity
- mirror/color symmetry when expected
- no extraction drift for the same FEN and aux state

Primary corpora:

- `root-expectations.jsonl`
- deterministic unit tests

### Root Broad-Confidence-Green

`broad-confidence-green` at root is narrower than count-freeze, symmetry
coverage, or a single exact/near-miss pair.

It means one schema-local claim only:

- the same narrow exact root atom survives across many exact-board buckets that
  are relevant to that atom
- the schema still fails closed outside those buckets
- confounded FENs do not get to count as broad-confidence evidence
- engine never becomes the owner of root truth

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

A future root promotion is allowed only when:

- the schema's intended exact meaning remains unchanged from
  [RootAtoms.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/RootAtoms.md)
- the live corpus closes every schema-local breadth bucket frozen in
  `root-coverage-matrix.md`
- the schema's minimum corpus floor is met by exact-board rows rather than by
  symmetry alone
- required fail-closed case families are present for that risk class
- engine-required schemas also carry their confound filter rows and those rows
  stay green without redefining the atom

All non-listed root schemas remain below `broad-confidence-green` until their
own schema-local buckets and floors are closed.

The canonical per-schema owner for counts, breadth buckets, floors, status, and
blockers is:

- `modules/commentary/src/test/scala/lila/commentary/root/RootCoverageMatrix.scala`

The tracked markdown snapshot consumed by doc readers must stay UTF-8
text-equivalent to that renderer output after newline normalization:

- `modules/commentary/src/test/resources/commentary-corpus/root-coverage-matrix.md`

### Root Risk-Class Rule

Root broad validation is frozen to three risk classes:

- `deterministic / no-engine broad validation`
  - exact local board law is already narrow enough that engine adds no truth
  - broad promotion still needs multiple exact-board buckets plus fail-closed
    negatives when the schema is easy to over-admit
- `structural / engine-optional sanity`
  - the atom remains root-owned and exact-board, but some wider corpus rows may
    need calmness screening when material or tactical skew would make the row
    non-representative
  - engine may reject a row as confounded, but it may not certify the atom
- `structural / engine-required confound filter`
  - the atom stays root-owned, but broad-counting rows are not accepted unless
    they also survive an explicit confound filter
  - current worktree owner set:
    - `outpost_square`
    - `candidate_passer`
    - `trapped_piece`
    - `king_shelter_hole`

### Root Engine Confound Policy

Engine/probe is still not part of the root layer.

At root it may be used only to reject a corpus row as confounded when the row is
supposed to validate a narrow structural atom but the board is instead dominated
by some unrelated tactical or material event.

Allowed root confound rejections are:

- immediate forced mate
- large unrelated eval spike
- severe material skew that makes the structural slice meaningless
- tactical release dominating what is supposed to be a structural exact slice

Root engine sanity may not:

- create a root atom
- replace the extractor's exact-board law
- promote a schema on centipawn strength alone

Current required root-engine sanity contracts:

| Schema | Runnable engine check now | Additional row-selection guard still curated manually |
| --- | --- | --- |
| `outpost_square` | `maxMatePly = 2`; `maxAbsCp = 250` | reject queen-or-rook-equivalent material-cliff rows and tactical-release rows unless the declared bucket explicitly says otherwise |
| `candidate_passer` | `maxMatePly = 2`; `maxAbsCp = 300` | reject queen-or-rook-equivalent material-cliff rows and tactical-release rows unless the declared bucket explicitly says otherwise |
| `trapped_piece` | `maxMatePly = 2` for selected exact trap rows; `maxAbsCp = 350` only for selected fail-closed rows | reject rows where the trapped side is already in a gross material cliff, and reject tactical-release rows unless the declared bucket explicitly says otherwise; exact trap rows may have large cp when that value is intrinsic to the trapped piece itself |
| `king_shelter_hole` | `maxMatePly = 2`; `maxAbsCp = 300` | reject rows where home-shelter meaning is dead because both sides lack enough non-pawn force, and reject tactical-release rows |

The current runnable scaffold proves only the numeric engine budgets above plus
probe-row presence/coverage. Material-cliff, force-retention, and
tactical-release exclusions remain audit-side row curation rules until a richer
root probe schema exists.

Selected `r-*` rows now exist in `engine-probe-expectations.jsonl` for all four
engine-required schemas, so the root engine-confound scaffold is live on the
current branch.

Those rows do **not** grant root `broad-confidence-green` by themselves.
They only prove that calm exact and fail-closed probe rows exist for the four
schemas; each schema still needs its remaining schema-local bucket and corpus
floor closure from `RootCoverageMatrix.scala`, plus serial extractor/probe test
execution before final promotion.
The current scaffold is selective rather than exhaustive over every existing
engine-required root corpus row.

If an engine-required root schema is later promoted to
`broad-confidence-green`, the selected probe buckets used for that promotion
must themselves be frozen in `RootCoverageMatrix.scala` and owned by tests,
rather than being implied from a generic one-exact / one-fail-closed scaffold
alone.

`candidate_passer` is now `broad-confidence-green`: 14 exact-board rows close
the polarity, phase, topology, support-shape, under-supported, and
already-passed negative buckets, and every bucket has a selected root
engine-probe row that stays within `maxMatePly = 2` and `maxAbsCp = 300`.

`loose_piece` is now `broad-confidence-green`: 12 exact-board rows close
polarity, pawn/minor/rook/queen family, undefended and
nominally-defended-but-losing exact exchange states, stably-defended
fail-closed rows, and mixed/heavy material regimes. Engine remains optional for
this schema and did not define the atom.

`overloaded_piece` is now `broad-confidence-green`: 9 exact-board rows close
white/black polarity, minor/rook/queen defender families, two-target and
three-plus defensive burdens, minor-pair/minor-rook/three-minor target mixes,
and one-target fail-closed rows. Engine remains optional only as a confound
filter; the atom is owned by the exact board condition that removing the
defender leaves at least two friendly non-king targets under enemy attacker
majority.

`xray_target` is now `broad-confidence-green`: 9 exact-board rows close
white/black polarity, rook/bishop/queen slider families, file/diagonal/rank
geometry, own/enemy blocker ownership, and target-absent/two-blocker
fail-closed rows. Engine remains optional only as a confound filter; the atom is
owned by the exact board condition that a beneficiary slider has line geometry
to an enemy non-king target with exactly one occupied blocker between them.

`trapped_piece` is now `broad-confidence-green`: 10 exact-board rows close
minor/rook/queen family, zero-safe/all-exits-lose/one-exit fail-closed modes,
white/black polarity, and king/pawn category rejection. The selected root
engine-probe buckets for this engine-required schema stay outside immediate
mate; selected fail-closed rows also stay within `maxAbsCp = 350`. Exact
trap-row cp magnitude is not capped when it is intrinsic to the trapped piece
itself, because engine remains a confound filter only and does not define the
atom.

`king_shelter_hole` is now `broad-confidence-green`: 11 exact-board rows close
opening/middlegame phase, white/black defender-home masks, center/wing shield
files, exact home-shelter holes, pawn-cover and no-access fail-closed rows, and
out-of-regime rejection. The selected root engine-probe buckets stay outside
immediate mate and within `maxAbsCp = 300`, so engine remains a confound filter
for calm home-shelter rows rather than a root-truth owner.

`side_to_move` is now `broad-confidence-green`: 4 exact-board rows close both
literal FEN side states across opening and sparse board contexts. This is a
deterministic no-engine schema; engine evidence is not relevant to the atom.

`piece_on` is now `broad-confidence-green`: 10 exact-board rows close pawn,
minor, rook, queen, and king role buckets across white/black polarity and
center/edge square topology. This is a deterministic no-engine schema; literal
occupancy remains exact root truth.

`controlled_by` is now `broad-confidence-green`: 10 exact-board rows close
pawn, knight, bishop, rook, queen, and king attacker families across
white/black polarity, center/edge/corner source topology, and empty/occupied
target buckets. This is a deterministic no-engine schema; attacked-square
membership remains exact board truth rather than legal-move wording.

`contested` is now `broad-confidence-green`: 8 exact-board rows close open-file
rook overlap, blocked-file overlap, knight, rook-rank, bishop, queen, occupied
and empty overlap buckets, plus a disjoint-kings fail-closed row. This is a
deterministic no-engine schema; overlap of the two attacked-square masks remains
the atom.

`open_file` is now `broad-confidence-green`: 4 exact-board rows close all-open
and mixed-board contexts, center/edge/all-files topology, and isolated/clustered
open-file buckets. This is a deterministic no-engine schema; pawn absence on the
file remains exact root truth.

`half_open_file` is now `broad-confidence-green`: 6 exact-board rows close
white/black ownership, edge/center topology, single/stacked rival-pawn layouts,
and own-pawn-restored near-miss buckets. This is a deterministic no-engine
schema; own-pawn absence plus rival-pawn presence remains exact root truth.

`king_ring_square` is now `broad-confidence-green`: 6 exact-board rows close
white/black polarity, corner/wing/center king-file contexts, and
orthogonal/diagonal ring-square adjacency buckets. This is a deterministic
no-engine schema; king-adjacent square membership remains exact root truth.

`weak_square` is now `broad-confidence-green`: 9 exact-board rows close
white/black polarity, center/edge topology, empty and beneficiary-occupied
states, minor/heavy exploitation regimes, and challenge-restored fail-closed
rows. Engine remains optional only as a confound filter; the atom is still owned
by enemy-territory, no enemy-pawn challenge, and beneficiary piece exploitation
on the exact board.

`pawn_controlled_by` is now `broad-confidence-green`: 6 exact-board rows close
white/black polarity, center/edge pawn-attack targets, and dual-polarity
near-miss rows. This is a deterministic no-engine schema; pawn attack geometry
remains exact root truth.

`isolated_pawn` is now `broad-confidence-green`: 6 exact-board rows close
edge/center files, home/advanced ranks, white/black polarity, and
neighbor-restored fail-closed rows. This is a deterministic no-engine schema.

`doubled_file` is now `broad-confidence-green`: 7 exact-board rows close
edge/center files, adjacent/split pawn stacks, white/black polarity, and
non-doubled fail-closed rows. This is a deterministic no-engine schema.

`fixed_pawn` is now `broad-confidence-green`: 6 exact-board rows close
white/black polarity, edge/center files, and single-stop/mutual-chain fixed-pawn
shapes. This is a deterministic no-engine schema.

`lever_available` is now `broad-confidence-green`: 7 exact-board rows close
single/double pushes, edge/center files, left/right contact directions,
white/black polarity, and blocked-push fail-closed rows. This is a
deterministic no-engine schema.

`castling_rights` is now `broad-confidence-green`: 8 exact-board rows close
none, single-side, same-color, cross-color, and full-rights states, while
duplicate and orphan castling fields fail closed at the root input boundary. This
is a deterministic no-engine schema; the legal FEN auxiliary state remains exact
root truth.

### Root Corpus Floor Policy

Root breadth promotion is cell-local, not one branch-wide score.

The schema-local floor is frozen per schema in `RootCoverageMatrix.scala` and
mirrored in `root-coverage-matrix.md`.

Current policy:

- deterministic schemas need enough rows to close their literal topology/state
  buckets, not just one seed exact
- structural schemas need enough rows to close both positive and fail-closed
  buckets for the same narrow meaning
- finite auxiliary families still need breadth by state family, not just one
  representative row
- until root rows carry explicit row-local bucket tags, the frozen bucket list
  in `RootCoverageMatrix.scala` remains the promotion owner; any non-listed
  schema stays outside root `broad-confidence-green`

### Layer 1: Witness Validation

Purpose:

- prove that `U`-owned descriptors rise only from the right root bundles
- prove that attached descriptors bind only to legal hosts
- prove that upper-layer-owned inventory entries do not leak back in as raw
  witness verdicts

Checks:

- required-root satisfaction
- forbidden-root rejection
- negative-boundary rejection
- family-local near-miss resistance

Primary corpora:

- `witness-expectations.jsonl`
- witness-level near-miss rows

### U Broad-Validation Contract

`broad-confidence-green` at `U` is descriptor-local.

It does not broaden a descriptor label into strategic prose. It means only that
the same deterministic `U = phi(R)` witness law survives across the descriptor's
exact-board buckets, keeps the same anchor/polarity/payload contract, and fails
closed at its negative boundaries.

The live `U` broad scope is frozen to:

- active `U-primary 18`
- active `U-attached 1`
  - `structural_space_claim`

Shell-only, host-vocabulary, and above-`U` rows are negative-boundary material.
They may appear in tests only to prove non-admission or legal host attachment.
They must not become standalone `U` runtime descriptors.

Current U broad owner:

- `modules/commentary/src/test/scala/lila/commentary/witness/u/UBroadCoverageMatrix.scala`

Tracked snapshot:

- `modules/commentary/src/test/resources/commentary-corpus/u-coverage-matrix.md`

Current audit result:

- `witness-expectations.jsonl` has completed the current descriptor-local broad
  corpus coverage for every live U descriptor
- formal U corpus rows are descriptor-local and must use runtime
  `descriptorId` values from the active U inventory, `caseType` in
  `exact|near_miss|nasty_negative`, `expectation` in `present|absent`, and
  descriptor-local `coverageAxis` / `coverageBucket` tags from the frozen
  breadth buckets
- existing `U` rule tests are useful local evidence, but they are not a formal
  broad corpus
- Root dependency gates are closed by the current 25/25 Root
  `broad-confidence-green` result
- current U `broad-confidence-green` descriptors:
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
- current formal U corpus row count is `218`
- no live U descriptor remains `thin` or root-blocked under the current
  descriptor-local broad corpus

Promotion requires all of:

- formal witness corpus rows for the descriptor
- descriptor-local corpus counts matching the U broad matrix, not just one
  aggregate row count
- exact, near-miss, and nasty-negative buckets required by the descriptor's
  risk class
- minimum corpus floor and frozen breadth-bucket closure for the descriptor
- stable anchor, polarity, variant, and payload invariants
- no shell-only or above-`U` row admitted as a runtime descriptor
- every required root dependency is already root `broad-confidence-green`
  - if a listed dependency is not green, the descriptor remains thin/blocked
    until the root gate closes or the U descriptor contract is narrowed to remove
    that dependency

Engine/probe remains forbidden as `U` truth.

If a row needs engine sanity, it belongs to root confound filtering or to
Object/Certification validation. It may not create or promote a `U` witness.

### Layer 2: Object Validation

Purpose:

- prove that witness bundles objectize into the right strategic object families

Checks:

- family present or absent
- owner and anchor correctness
- rival exclusion correctness
- no object inflation from aesthetic but shallow bundles

Primary corpora:

- `object-expectations.jsonl`
- `engine-probe-expectations.jsonl` for local Stockfish sanity on the object,
  certification, and selected root confound-filter positions themselves

This mirrors the strategic-object branch format:

```json
{"id":"o7-king-safety-exact","caseType":"exact","fen":"6k1/8/6p1/4B3/8/8/7R/6K1 w - - 0 1","expectation":"present","family":"KingSafetyShell","owner":"black","anchor":"g7","pressureTarget":"adjacent_double_hole","helpers":["home_shelter_shell"]}
```

Current `Object 7` start-gate rule:

- `object-expectations.jsonl` must carry `exact`, `near_miss`, and
  `nasty_negative` rows for:
  - `OpeningDevelopmentRegime`
  - `DistributedContactRegime`
  - `EndgameRaceScaffold`
  - `AttackScaffold`
  - `FortressHoldingShell`
  - `KingSafetyShell`
  - `CentralContactFront`
- each row must already carry:
  - `family`
  - `owner`
  - `anchor`
  - `pressureTarget`
  - `helpers`
  - `expectation`
- object rows now bind against live
  `CommentaryCore.extractStrategicObjects`; scaffold-only object rows are no
  longer honest on this branch
- positive object rows must also reject extra same-family identities at the
  wrong anchor or color; exact-board validation is not satisfied by
  “expected object present somewhere” if false-positive siblings survive
- `nasty_negative` rows must pressure the frozen positive law rather than fail
  only because a trivial prerequisite disappeared
- object test positions now also bind against
  `engine-probe-expectations.jsonl`; engine is not object-truth authority, but
  it must at least certify that the corpus row is not already a trivial
  short-horizon tactical collapse, and calmness-sensitive rows may carry a
  coarse eval cap
- rule-test-only exact boards that are not promoted into
  `object-expectations.jsonl` must still survive a local Stockfish smoke check;
  a branch test is not accepted if the FEN itself is engine-rejected because
  the side-to-move or board legality drifted away from the intended geometry
- broader negative-first families such as `exact negative`, `false-witness`,
  and related reinflation checks remain part of branch-wide audit work even
  when the minimum live runtime corpus stays on `exact` / `near_miss` /
  `nasty_negative`

Recommended object row shape:

```json
{"id":"o7-central-contact-exact","caseType":"exact","fen":"4k3/8/8/3pp3/3PP3/3N4/4n3/4K3 w - - 0 1","expectation":"present","family":"CentralContactFront","owner":"neutral","anchor":"center","pressureTarget":"connected_central_front","helpers":["central_sector_mask","contact_square","front_connectivity","central_contact_front_state"],"notes":"The exact front is central, connected, and contributed to by both colors."}
```

### Layer 3: Delta Validation

Purpose:

- prove that the right object emits the right scope:
  - `move_local`
  - `position_local`
  - `comparative`

Current-worktree start gate:

- `Delta 2` is frozen to two families:
  - `TradeCompressionCorridor`
  - `TradeInvariant`
- both current-worktree delta families are live and remain `move_local` only
- both are `board`-anchored
- delta validation must use exact before/after positions rather than one static
  board only
- `TradeInvariant` now has live runtime extraction and remains narrower than
  the broader future envelope: its live slice admits only
  `EndgameRaceScaffold` persistence on the `board` anchor with mover-side
  clear-run carrier continuity

Checks:

- supplied `StrategicObjectExtraction` carriers must be canonical:
  - `before` and `after` witness/object payloads must exactly match the live
    object extractor output for their root states
- typed delta tag correctness
- anchor correctness
- scope correctness
- exact auxiliary-state-preserving
  `fenBefore -> playedMove -> fenAfter` transition correctness:
  - side to move must match the moving piece
  - castling rights must survive or clear exactly as the move requires
  - en-passant availability must be rehydrated from the root aux state rather
    than guessed from piece placement alone
  - legal castling and legal en-passant transitions must be accepted
  - board-coherent but wrong-side or aux-mismatched transitions must fail
    closed
- false-witness rejection
- false-rival rejection
  - `TradeCompressionCorridor` rival rejection must track the actual live
    `TradeInvariant` first slice rather than raw board-level
    `EndgameRaceScaffold` persistence
- row-specific pressure-law correctness:
  - corridor vs generic liquidation separation for `TradeCompressionCorridor`
  - compressed-window count gating and canonical-pair uniqueness for
    `TradeCompressionCorridor`
  - persistent-carrier vs task-switch separation for `TradeInvariant`
    - board-level scaffold presence alone is not enough when the mover's
      clear-run carrier switches to a different pawn
  - bounded-material-reduction separation from pawn-only captures for
    `TradeInvariant`

Primary corpora:

- `delta-expectations.jsonl`

Each current delta row must carry:

- `id`
- `caseType`
- `fenBefore`
- `playedMove`
- `fenAfter`
- `expectation`
- `family`
- `owner`
- `scope`
- `deltaTag`
- `anchor`
- `pressureTarget`
- `helpers`
- `forbiddenRivalFamilies`
- row-specific exact evidence when required:
  - `canonicalCorridorPairAfter` for `TradeCompressionCorridor`
  - `persistentCarrierFamily` plus `persistentCarrierAnchor` for
    `TradeInvariant`

Current-worktree required case types for each delta family:

- `exact`
- `near_miss`
- `nasty_negative`
- `move_local_false_witness`

This mirrors the strategic-object branch format, but delta rows now carry
before/after exact-board truth explicitly:

```json
{"id":"delta-trade-invariant-exact","caseType":"exact","fenBefore":"4k3/2n5/3P4/8/6p1/8/4K3/8 w - - 0 1","playedMove":"d6c7","fenAfter":"4k3/2P5/8/8/6p1/8/4K3/8 b - - 0 1","family":"TradeInvariant","owner":"white","scope":"move_local","expectation":"present","deltaTag":"bounded_favorable_simplification","anchor":"board","pressureTarget":"bounded_capture_preserves_endgame_race","helpers":["bounded_material_reduction","persistent_object_carrier","trade_invariant_transition"],"persistentCarrierFamily":"EndgameRaceScaffold","persistentCarrierAnchor":"board","forbiddenRivalFamilies":["TradeCompressionCorridor"]}
```

Live delta tests:

- `TradeCompressionCorridorRuleTest`
- `TradeInvariantRuleTest`
- `StrategicDeltaBoundaryTest`
- `DeltaExpectationCorpusTest`
- `CommentaryCoreBoundaryTest`

Current-worktree delta validation now also keeps these exact-board guards live:

- `TradeCompressionCorridor`
  - a diagonal exact row, so corridor geometry is not file-only by accident
  - an exact row with surviving `EndgameRaceScaffold`, so board-level race
    persistence alone cannot suppress a real corridor transition as a fake
    `TradeInvariant` rival
  - a queenless-but-overcrowded negative, so compressed-window validation does
    not collapse to queen removal alone
  - a multi-corridor negative, so ambiguous after-board corridor pairs do not
    pass as canonical transitions
- `TradeInvariant`
  - a pawn-capture false witness, so carrier persistence alone does not
    masquerade as bounded material reduction
  - a carrier-switch negative, so board-level scaffold survival does not
    masquerade as same-carrier persistence
- public delta facade:
  - an overlapping-board mutual-exclusion assertion, so the public surface
    cannot co-emit `TradeCompressionCorridor` and `TradeInvariant` on the same
    move
  - a corridor-with-race-scaffold assertion, so the public surface does not
    suppress `TradeCompressionCorridor` merely because the lower carrier object
    survives while actual `TradeInvariant` continuity fails
  - corpus-driven `CommentaryCore` extraction parity, so every live delta row is
    checked through the public delta facade as well as the internal extractor
- transition hardening:
  - a wrong-side-move negative, so piece-coherent board drift alone cannot pass
    as an exact delta transition
  - a legal-castling exact row, so delta validation does not reject auxiliary
    state carrying moves just because the king moved more than one file
  - a legal-en-passant exact row, so delta validation does not reject captures
    whose captured pawn is not on the landing square
  - an after-state aux-mismatch negative, so piece-placement parity alone does
    not masquerade as exact transition truth
- corpus contract completeness:
  - `DeltaExpectationCorpusTest` now freezes the full per-family
    `pressureTarget` inventory, so deleting one live pressure-law row and
    duplicating another cannot slip through on case-type coverage alone

### Layer 4: Certification Validation

Purpose:

- prove whether a claim is:
  - `Certified`
  - `SupportOnly`
  - `Deferred`
  - `Rejected`

Checks:

- exact-board burden met or not
- best-defense survival
- comparative burden sufficiency
- support-only containment
- deferred fail-closed behavior
- supplied `StrategicObjectExtraction` carriers must be canonical:
  - current witness/object payload must exactly match the live object extractor
    output for the current root state
- supplied delta extractions, when present, must be canonical:
  - exact before/after/move validation must still pass
  - the carried delta set must exactly equal the canonical delta runtime
    result for that transition
- legal-move reconstruction must consume the root aux state:
  - castling rights must not be invented from open geometry alone
  - current-turn en-passant captures must survive when the root carries them
  - comparative move counts must not silently drop side/castling/en-passant
    state on rebuild

Primary corpora:

- `certification-expectations.jsonl`
- engine/probe bundles

Current `Certification` start-gate rule:

- `certification-expectations.jsonl` is now a live row-local certification
  scaffold corpus rather than an empty placeholder
- the current branch keeps `10` certification inventory rows mapped to `11`
  runtime families:
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
- the current worktree also carries a live certification runtime under
  `modules/commentary/src/main/scala/lila/commentary/certification`, with
  typed `CommentaryCore` entry points that require an explicit certification
  evidence bundle; `CertificationEvidenceBundle.empty` is the explicit
  unbound fail-closed sentinel, while any non-empty bundle created by
  `forObjectExtraction` or `forDeltaExtraction` must be bound to the same
  current root state
- any supplied `StrategicDeltaExtraction` must also be canonical:
  - exact before/after/move validation must still pass
  - the carried `deltas` set must exactly match the canonical delta runtime
    result for that transition
- live certification extraction must reject any non-empty evidence bundle
  whose bound root state does not exactly match the current extraction
- the live runtime does not yet consume a typed probe adapter;
  `CertificationEngineEvidence.fromProbe(...)` remains fail-closed empty and
  current probe usage stays in validation-side scaffold tests
- every certification family must now carry:
  - `exact`
  - `near_miss`
  - `nasty_negative`
  - `best_defense_breaks_claim`
  rows
- every certification family must explicitly cover the verdict lattice needed
  for its first live slice:
  - `Certified`
  - `Rejected`
  - plus `SupportOnly` or `Deferred` where the branch burden intentionally
    degrades rather than failing outright
- every certification row must declare:
  - `family`
  - `owner`
  - `scope`
  - `anchor`
  - `burdenTag`
  - `helpers`
  - `requiredSupportFamilies` when the first live slice depends on lower object,
    delta, or certification-side support families
  - `engineRequirement`
  - `enginePurposes`
  - `forbiddenShortcuts`
- every certification row with `engineRequirement = required` must have a
  matching engine/probe bundle row keyed by the same `id`
- certification-side support-family presence is satisfied only by a live
  non-`Rejected` claim of the required family for the same owner polarity
- certification scaffold tests may currently validate:
  - exact-board FEN parseability
  - row-family coverage
  - verdict-lattice coverage
  - engine/probe bundle linkage
  - frozen burden/helper/shortcut contracts
- certification scaffold tests alone do not certify live extraction semantics
- live certification extraction now exists in `src/main` and must be validated
  separately from the corpus scaffold against the canonical runtime boundary
  and the explicit evidence-bundle contract
- live certification extraction must reject any non-empty evidence bundle
  whose bound root state does not exactly match the current extraction

Current row-local certification family freeze:

| Family | First live burden | Required support families | Engine purposes | Best-defense downgrade |
| --- | --- | --- | --- | --- |
| `DevelopmentComparison` | `development_superiority` | `OpeningDevelopmentRegime` | `comparative_superiority` | `SupportOnly` |
| `InitiativeWindow` | `counterplay_denial_window` | `DevelopmentComparison` | `counterplay_denial`, `best_defense_survival` | `Deferred` |
| `MobilityComparison` | `mobility_superiority` | none | `comparative_superiority` | `SupportOnly` |
| `ComparativeKingFragility` | `king_fragility_asymmetry` | none | `comparative_superiority` | `SupportOnly` |
| `CertifiedKingSafetyEdge` | `king_safety_edge_certification` | `AttackScaffold`, `ComparativeKingFragility` | `comparative_superiority`, `best_defense_survival` | `Deferred` |
| `MateNetCertification` | `forcing_mate_net` | none | `best_defense_survival`, `tactical_release_detection` | `Deferred` |
| `MaterialHarvest` | `realized_material_conversion` | none | `best_defense_survival`, `tactical_release_detection` | `SupportOnly` |
| `WinningEndgame` | `conversion_result` | none | `best_defense_survival`, `conversion_route_survival` | `Deferred` |
| `FortressDrawCertification` | `fortress_hold_certification` | `FortressHoldingShell` | `best_defense_survival` | `SupportOnly` |
| `PerpetualCheckHolding` | `perpetual_check_hold` | none | `best_defense_survival` | `Deferred` |
| `PromotionRace` | `promotion_route_survival` | `EndgameRaceScaffold` | `best_defense_survival`, `conversion_route_survival` | `Deferred` |

This matrix is now the live row-local exact-board scaffold contract for
`certification-expectations.jsonl`.

Current result/draw/race hardening reminders:

- `MaterialHarvest` exact admission must stay on a current-turn capture whose
  destination is not immediately recapturable by the rival.
- `WinningEndgame` exact admission must keep rook-pawn corner-draw shells below
  certification and must also keep rival pawn counterplay below certification;
  the current live slice stays on a single non-rook-pawn runner with owner king
  support, owner to move, and no rival pawns.
- `FortressDrawCertification` exact admission must stay shell-backed and
  explicit-best-defense-burden-backed; shell presence alone is not the draw
  proof owner, and the validation corpus must keep fortress exact and
  best-defense rows inside an explicit drawish `maxAbsCp` budget.
- `PromotionRace` exact admission must not let non-king interceptors masquerade
  as route survival; the current live slice stays on kings-and-pawns clear-run
  race boards and uses tempo plus rival-king-distance burden rather than a full
  universal route proof.
- certification legal-move rebuild now also stays exact-board with respect to
  side-to-move, castling rights, and current en-passant availability; runtime
  and boundary tests must keep both no-rights-castling negatives and exact
  en-passant positives live

It is no longer acceptable to treat certification as one generic future layer
without row-local burden and engine-purpose freeze.

Projection and renderer are downstream consumers of certified claims.

They are not truth-owning semantic layers.

### Projection Handoff Validation

Purpose:

- prove that each `S01-S25` band has enough certified lower-layer carrier input
- prove that rival strategy bands remain semantically separable on exact boards

Checks:

- minimum certified carrier coverage
- optional carrier strengthening without admission drift
- rival-band separation on shared tactical or structural positions
- no projection admission from legacy label, renderer wording, or planner lane
  alone

Primary corpora:

- `projection-expectations.jsonl`
- exact-board rival-band near-miss rows

The semantic boundary for this layer is frozen in
[StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md).

Strategy bands whose start-ready handoff depends on explicit lower/support seed
families are inventoried in
[StrategySupportSeedInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategySupportSeedInventory.md).

Projection start-ready status is narrower than semantic-boundary freeze.

A band is `start-ready` only when:

- its semantic boundary row is frozen
- its canonical exact carrier bundle is frozen
- its required projection corpus shape is frozen
- its promotion criterion or blocker is explicit

`start-ready` authorizes building the projection runtime for that band.

For `S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`,
`projection-expectations.jsonl` is now populated with exact, near-miss,
nasty-negative, and where applicable `comparative_false_rival` rows that
exercise the frozen admission scaffold.

No coverage-complete-only strategy band is broad-deployed or live as runtime
projection on the current branch. The staged wave-1, wave-2, wave-3, wave-4,
wave-5, and wave-6 set
`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`,
`S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`,
`S23`, `S24`, and `S25` is coverage-complete under the current JSONL/test
broad-coverage gates. `S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25` keep
only their existing narrow live admission slices; the remaining
coverage-complete rows stay non-live.

S06 live runtime admission is a narrow exact-board closure. The required
evidence kind is `space_bind_restriction_route_certified`, and admission must
prove `structural_space_claim`, an outpost or short-run restriction linked to
that same structural host, support-only `SpaceBindRestrictionCertification`
with a matching `route_host_links` payload, same-task evidence mirroring of
route / anchor / host / restriction, exact S05 center-release and exact S20
domination false-rival rejection, and stale / wrong-anchor evidence rejection.
`MobilityComparison`, `InitiativeWindow`, and the lower certification remain
support-only lower carriers, not projection truth owners.

A future live-deployment `broad-ready` claim is allowed only when:

- the band is already `start-ready`
- every required `coverageAxis` / `coverageBucket` pair frozen in
  `StrategyProjectionBoundaryMatrix.md` is present in
  `projection-expectations.jsonl`
- the band's explicit broad-ready blocker is closed

Coverage-only rows close the executable broad coverage gate without satisfying
this live-deployment prerequisite. Coverage completion is not live projection
runtime admission.

Wave-7 closes the global freeze owner split: `StrategyProjectionCoverageContract`
is the executable owner for coverage gates, lower-carrier ownership,
helper/admission laws, and exact-board validation scaffolds for every
`S01-S25` band. `ProjectionExpectationCorpus.requiredCoveragePairsFor` derives
from that contract, and `ProjectionExpectationCorpusTest` asserts that the
contract-owned key sets match the staged corpus bands exactly. The runtime
boundary remains fail-closed because `StrategyProjectionAdmission` admits only
`StrategyProjectionScopeContract.startReadyBandIds` (`S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`,
`S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`) and rejects every
`StrategyProjectionCoverageContract.coverageOnlyBandIds` entry.

This is executable, not just documentary: `ProjectionExpectationCorpusTest`
computes missing required coverage pairs from the JSONL corpus and keeps the
current branch runtime fail-closed even when those pairs are complete.
The coverage-presence counter is not raw tag counting: positive route, scope,
durability, access, creation, and suppression buckets count only `exact` /
`admitted` rows; same-cluster near-miss buckets count only rejected near-miss
or contrastive or `comparative_false_rival` rows; shortcut-negative buckets
count only rejected nasty-negative or negative rows.

The current staged wave-1 executable broad-ready gate-band set is
`S06` / `S09` / `S10` / `S12` / `S20` / `S23` / `S25`.

The wave-7 global closure set is exactly `S01`, `S02`, `S03`, `S04`, `S05`,
`S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`,
`S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25`. The current
JSONL corpus reports that full set as coverage-complete for the staged wave-1,
wave-2, wave-3, wave-4, wave-5, and wave-6 broad gates. This is
corpus/validation-only: those bands do not become broad-deployed, and live
runtime admission remains limited to explicit `startReadyBandIds` rather than
being granted by coverage completion alone.

The current runtime-start-ready subset (`S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`,
`S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`) and the S15/S16 wave-6
coverage split have executable broad coverage rows in
`projection-expectations.jsonl`, with the frozen gates owned by
`StrategyProjectionCoverageContract` and `StrategyProjectionCoverageContractTest`.
This adds countable `coverageAxis` / `coverageBucket` rows while keeping live
projection admission limited to explicit narrow branches:

- `S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and
  `S25` keep start-ready admission scaffolds
- S16 rows remain countable broad coverage rows, but live admission is limited
  to exact same-enemy-passer suppression routes with
  `passer_suppression_route_certified` evidence
- the coverage set passes the normal coverage-presence burden above

`S05`, `S07`, `S08`, and `S21` now have executable initiative / release /
counterplay broad-ready coverage rows. Their gates are owned by
`StrategyProjectionCoverageContract` and verified by
`StrategyProjectionCoverageContractTest` plus `ProjectionExpectationCorpusTest`.
This closes the wave-3 coverage-completion gate:

- `S05`: exact central release requires same-anchor `available_lever_trigger`
  and `pawn_push_break_contact_source` with a center-file source, center-file
  target, and a `center_release_route_certified` claim whose route is
  `center_pawn_target` or `central_axis_continuation`; `CentralContactFront`,
  `InitiativeWindow`, and central file support are support-only here. Wrong
  source, wrong target, wrong route, stale evidence, adjacent S06/S14/S21
  evidence, open-center wording, center shell, `CentralContactFront`,
  `InitiativeWindow`, and optional file support remain fail-closed.
- `S07`: exact initiative conversion requires `DevelopmentComparison` plus
  certified `InitiativeWindow` for the same owner; `OpeningDevelopmentRegime`
  is support-only and development wording does not admit the projection band.
  S07 live runtime admission names `initiative_conversion_route_certified` as
  the board-anchored projection evidence kind in live
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`. The route task
  is limited to `development_led_window` or `move_right_window`, and lower
  `DevelopmentComparison` / `InitiativeWindow` certification truth must not
  become S-layer projection truth.
- `S08`: denial must be tied to an exact rival release source being suppressed;
  `InitiativeWindow` alone is not a projection denial claim. S08 live runtime
  admission uses `counterplay_denial_route_certified`; evidence mirrors the
  same rival source, target, and denial route, the same board must not carry an
  owner-side S21 survival carrier, and wrong-source, wrong-target, wrong-route,
  and stale evidence are rejected. `InitiativeWindow` remains lower
  Certification support, not projection truth.
- `S21`: counterplay survival requires exact owner
  non-center `pawn_push_break_contact_source` plus same-board certified
  `InitiativeWindow`;
  source presence alone, certified `InitiativeWindow` alone, deferred
  initiative, center-source/center-target S05-shaped release, open-center
  wording, and asymmetry wording remain fail-closed.
  S21 live runtime admission uses
  `counterplay_survival_route_certified` for same
  owner/source/target/route/certification mirroring without making
  `InitiativeWindow` the projection truth owner

`S05`, `S07`, `S08`, and `S21` are narrow live admission bands and remain
countable in the broad coverage corpus. S08 runtime evidence is limited to
`counterplay_denial_route_certified` and same rival source, target, and denial
route proof.
Their required coverage pairs are counted by
`ProjectionExpectationCorpus.requiredCoveragePairsFor`, `coveragePairsByBand`,
`missingRequiredCoveragePairsByBand`, and `bandsWithCompleteBroadReadyCoverage`.
`StrategyProjectionAdmission` admits S07 only with exact
`initiative_conversion_route_certified` evidence plus same-owner certified
DevelopmentComparison / InitiativeWindow lower support; S08 admits only with
exact `counterplay_denial_route_certified` evidence plus same-board
InitiativeWindow lower support, while S21 rejects source-only,
initiative-only, stale, or adjacent-rival rows fail-closed.

`S01`, `S02`, `S03`, and `S04` now have executable wave-4 king-attack
broad-ready coverage rows. Their gates are owned by
`StrategyProjectionCoverageContract` and verified by
`StrategyProjectionCoverageContractTest` plus `ProjectionExpectationCorpusTest`.
This closes the wave-4 coverage-completion gate only:

- `S01`: same-wing owner contact plus same-defender attack and certified
  king-safety edge; castling-side context and wing-shell wording remain
  fail-closed
- `S02`: direct king-ring concentration; `AttackScaffold`, lane support, or
  king-attack wording alone cannot admit the projection band
- `S03`: king-facing `diagonal_lane_only` plus same-king fragility and
  certified king-safety edge; bishop-pair state and non-king diagonal pressure
  remain fail-closed
- `S04`: same-defender `KingSafetyShell`, shell-payload or support-break
  breach, and same-defender certified king-safety deterioration; shell-only
  weakness and generic attack pressure remain fail-closed

These four bands are broad-ready coverage-only bands. Their required coverage
pairs are counted by `ProjectionExpectationCorpus.requiredCoveragePairsFor`,
`coveragePairsByBand`, `missingRequiredCoveragePairsByBand`, and
`bandsWithCompleteBroadReadyCoverage`. `StrategyProjectionAdmission` still
rejects them as unsupported live admission bands.

`S11`, `S13`, and `S14` now have executable pawn-target /
structural-damage broad-ready coverage rows. Their gates are owned by
`StrategyProjectionCoverageContract` and verified by
`StrategyProjectionCoverageContractTest` plus `ProjectionExpectationCorpusTest`.
This closes the wave-5 coverage-completion gate for all three rows and the
narrow S11/S13/S14 runtime-admission gates:

- `S11` exact validation must keep `weak_pawn_target_state`, pressure or
  repeated pressure, and the current fixed-pawn persistence proof on the same
  weak-pawn square;
  weak-pawn presence alone and target swaps by prose stay non-counting
- S11 runtime validation must also keep the live admission freeze exact-board
  and fail-closed:
  `target_pressure_route_requires_same_square_weak_pawn_target_and_owner_pressure`,
  `fixation_route_requires_current_fixed_pawn_persistence_on_same_target`,
  `repeated_pressure_route_requires_two_owner_attackers_on_same_target`,
  `projection_evidence_mirrors_weak_pawn_target_pressure_and_persistence`,
  `weak_pawn_only_target_swap_adjacent_rival_or_optional_strengthening_is_non_admitting`,
  `fixed_pawn_persistence_is_current_board_carrier_not_certification_truth_law`,
  `same_task_projection_evidence_must_mirror_s11_target_pressure_and_persistence_law`,
  and `pressure_without_fixed_persistence_not_counted`. The live evidence kind
  is `weak_pawn_target_pressure_persistence_certified`; it is the only S11 entry
  in `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`.
- `S13` exact validation must keep `sector_asymmetry_state`, same-wing-sector
  `available_lever_trigger`, and same-anchor `pawn_push_break_contact_source`
  tied to a target role recomputed from the exact board as `phalanx_edge_target`
  or `structurally_burdened_target`; for live S13 admission, a phalanx-edge
  surface that is also a chain-base target stays S14-adjacent and
  non-counting. Raw asymmetry or a pre-existing weak pawn alone stays
  non-counting. Its live admission freeze names
  `wing_damage_route_requires_same_sector_asymmetry_and_same_anchor_contact_source`,
  `wing_damage_route_excludes_center_sector_targets`,
  `phalanx_edge_route_requires_recomputed_phalanx_edge_target`,
  `phalanx_edge_route_excludes_chain_base_targets`,
  `burdened_target_route_requires_recomputed_structurally_burdened_target`,
  `projection_evidence_mirrors_owner_source_target_sector_and_damage_route`,
  and
  `asymmetry_weak_pawn_adjacent_rival_or_optional_strengthening_is_non_admitting`.
  The live projection evidence kind is `wing_damage_route_certified`, and it is
  the S13 entry in `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`.
- `S13` helper and scaffold tokens are
  `same_sector_asymmetry_plus_contact_source_law`,
  `wing_damage_target_role_recomputed_from_exact_board_law`,
  `phalanx_edge_target_excludes_chain_base_law`,
  `center_sector_damage_is_not_wing_damage_law`,
  `same_task_projection_evidence_must_mirror_s13_source_target_sector_and_route_law`,
  `asymmetry_or_preexisting_weak_pawn_is_not_damage_law`,
  `sector_asymmetry_present`,
  `same_sector_lever_and_break_contact_present`,
  `wing_sector_damage_route_present`,
  `contact_target_role_is_non_chain_base_phalanx_edge_or_structurally_burdened`,
  `weak_pawn_or_asymmetry_only_not_counted`, and
  `stale_or_adjacent_runtime_evidence_not_counted_before_live_admission`.
  These keep stale evidence, adjacent `S11` / `S14` / `S15` rivals, optional
  `weak_pawn_target_state` / `file_lane_state` strengthening, and Object /
  Delta / Certification truth-owner leakage out of S13 admission.
- `S14` exact validation must keep base-contact lever/source on the same anchor,
  require the same source/target pair for the base-contact continuation,
  recompute the contact target as a non-center `chain_base_target`, and admit
  `base_contact_continuation` only when the target's forward-supported pawn
  itself supports the next defender pawn on the exact board;
  fixed-chain and structural-damage shell vocabulary remain context only. Its
  live admission freeze names
  `chain_base_route_requires_same_anchor_lever_and_contact_source`,
  `chain_base_route_requires_recomputed_non_center_chain_base_target`,
  `base_contact_continuation_requires_same_source_target_pair`,
  `projection_evidence_mirrors_owner_source_target_and_chain_base_route`, and
  `fixed_chain_structural_shell_adjacent_rival_or_optional_strengthening_is_non_admitting`.
  The live evidence kind is `chain_base_contact_route_certified`, and it is the
  S14 entry in `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`.
  Its helper/law and scaffold tokens are `same_anchor_chain_base_contact_law`,
  `non_center_chain_base_role_recomputed_from_exact_board_law`,
  `base_contact_must_bind_same_source_and_target_law`,
  `same_task_projection_evidence_must_mirror_s14_source_target_and_route_law`,
  `fixed_chain_or_structural_damage_shell_is_not_truth_owner_law`,
  `base_contact_lever_and_break_contact_present`,
  `contact_target_role_is_non_center_chain_base`,
  `base_contact_continuation_same_anchor_present`,
  `fixed_chain_or_structural_damage_shell_only_not_counted`, and
  `stale_or_adjacent_runtime_evidence_not_counted_before_live_admission`.
  Lower ownership remains
  `Witness:available_lever_trigger(base_contact)`,
  `Witness:pawn_push_break_contact_source(same_anchor_chain_base_target)`,
  `ProjectionValidation:non_center_chain_base_target_recomputed|base_contact_continuation`,
  and `SupportOnly:weak_pawn_target_state|structural_damage_shell|fixed_chain_context`;
  no support seed, Object, Delta, or Certification family is an S14 projection
  truth owner. Runtime admission also requires projection evidence to mirror
  the contact source, target, chain-base route token, and exact chain-base
  forward support squares; a chain-base-target row cannot be relabeled as a
  base-contact-continuation row by evidence token alone.

S13's live slice is executable only for exact current-board same-wing-sector
wing-damage routes. S14's live slice is executable only for exact current-board
same-anchor non-center chain-base contact routes. All three rows' required
coverage pairs are counted by
`ProjectionExpectationCorpus.requiredCoveragePairsFor`, `coveragePairsByBand`,
`missingRequiredCoveragePairsByBand`, and `bandsWithCompleteBroadReadyCoverage`.
Adjacent S05/S11/S13/S15 rivals, stale evidence, and optional strengthening
remain fail-closed; S15 admits only through its explicit same-candidate
creation branch, and S16 admits only through its explicit same-enemy-passer
suppression branch.

For the wave-2 five-band coverage set, exact validation stays board-bound:

- `S17`: exact same-piece liability, same-piece repair or exchange relief, and
  same-piece relief evidence
- `S18`: exact same-owner current-board `bishop_pair_state` substrate with at
  least one active bishop-pair member plus certified conversion burden;
  bishop-pair or minor-edge wording alone is negative. The live runtime
  admission freeze names no support seed, object, or delta carrier for S18, and
  lower certification remains the truth owner:
  `InitiativeWindow`, `MobilityComparison`, or `MaterialHarvest`.
  `WinningEndgame` is support-only future carrier because current lower law
  cannot certify it on a board that still has bishops. The frozen burden names are
  `initiative_route_requires_bishop_pair_state_and_same_board_initiative_window`,
  `structure_route_requires_bishop_pair_state_and_same_board_mobility_comparison`,
  `material_route_requires_bishop_pair_member_material_harvest`,
  `projection_evidence_mirrors_conversion_family_targets_and_bishop_pair`, and
  `relation_only_optional_strengthening_or_adjacent_rival_is_non_admitting`.
  The frozen live projection evidence-kind names are
  `bishop_pair_initiative_conversion_certified`,
  `bishop_pair_structure_conversion_certified`, and
  `bishop_pair_material_conversion_certified`; they exactly match
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` for S18. Exact
  validation remains board-bound through
  `s18_projection_evidence_same_task_same_target_law`,
  `active_bishop_pair_member_present`,
  `same_current_board_conversion_certification_present`,
  `projection_evidence_mirrors_conversion_family_targets_and_bishop_pair`,
  `material_conversion_bishop_pair_member_capture_present`,
  `initiative_or_structure_conversion_support_targets_present`, and
  `bishop_pair_minor_edge_or_optional_strengthening_only_not_counted`.
- `S19`: exact `TradeInvariant` transition from before/after FEN plus played
  move; countable material or hold rows must also certify the same exact task
  endpoint through `MaterialHarvest` or `FortressDrawCertification`, while
  result-only simplification remains non-counting without a same-task lower law.
  The live admission freeze names the burden as
  `material_route_requires_canonical_trade_invariant_and_same_move_material_harvest`,
  `hold_route_requires_canonical_trade_invariant_and_post_trade_fortress_certification`,
  and `result_only_material_only_or_adjacent_rival_is_non_admitting`.
  `MaterialHarvest` must expose `capture_from` / `capture_to` matching the exact
  `TradeInvariant` played move; S22/S24 rival rows must expose their own exact
  lower truth without becoming S19 truth. S19 projection evidence-kind names are
  live as `trade_invariant_material_simplification_certified` and
  `trade_invariant_hold_simplification_certified`; those evidence claims are
  admission companions only and do not replace `TradeInvariant`,
  `MaterialHarvest`, `FortressDrawCertification`, or `FortressHoldingShell` as
  lower truth owners.
- `S22`: same-holder exact `FortressHoldingShell` plus certified
  `FortressDrawCertification`, or certified current-board
  `PerpetualCheckHolding`; shell, draw wording, loose checking-sequence wording,
  exact S19 `TradeInvariant` rival truth, or `SupportOnly` / `Deferred` hold
  evidence is negative unless the same-root S22 evidence kind and lower
  object/certification revalidation both pass from an externally supplied
  same-root `CertificationEvidenceBundle`
- `S24`: exact same target piece-square across dependency, convergence,
  forcing realization, and conversion certification; raw tactic or trade/result
  wording alone is negative

`S15` and `S16` are wave-6 broad-ready coverage bands. S15 now has narrow live
admission: per-position admission remains disjunctive over `s13_wing_damage`
or `s14_chain_base`, while broad-ready coverage proves both route buckets plus
exact false-rival boundaries against `S13`, `S14`, and `S16`. The S15
exact-board law is same-candidate: the `candidate_passer` root pawn must be the
same pawn as the S13/S14 contact source, while create-passer shell-only,
candidate-only, existing-passer-only, and split-anchor route rows remain
rejected. The S15 evidence kind `passer_creation_route_certified` is live in
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand`, but it is only
an admission companion and does not own lower root or certification truth.
`S16` now has narrow live admission. Its broad-ready coverage proves
`blockade_hold`, `restriction_hold`, and `non_losing_race`, and every positive
suppression row binds enemy passer truth to exact same-board blockade,
restriction, and certified race/hold proof without making the certification
itself the passer truth owner. For `blockade_hold`, the fortress certification
support must target the same passer/blocker shell. For `restriction_hold`, the blocker must be an
owner `short_run_slider_gate_restriction` gate square rather than an unrelated
restriction elsewhere on the board. Live admission uses
`passer_suppression_route_certified` evidence to mirror the same enemy
passed-pawn anchor, route token, route-specific blocker when applicable, and
same-board certified hold/race support. That evidence name is the only S16
entry in `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`, so
S16 admits only through the exact same-enemy-passer branch and remains
fail-closed for enemy-passer-only, blocker-picture-only, stale evidence, and
adjacent S15/S22/S23 rival rows.

For `S25`, the current broad-ready route bucket is only
`rank_access_route = cross_wing_rank_switch`; wider horizontal-rank meanings
remain future scope and do not count yet.

### Projection Core Matrix

Before a band may be claimed projection-complete or broad-ready,
`projection-expectations.jsonl` must carry countable rows for each frozen
`coverageAxis` / `coverageBucket` pair owned by that band:

- positive route, scope, durability, access, creation, and suppression buckets require
  `exact` / `admitted` rows
- same-cluster near-miss buckets require rejected `near_miss`, `contrastive`,
  or `comparative_false_rival` rows
  - include an exact-board row for every explicitly named rival band in
    `StrategyProjectionBoundaryMatrix.md`
- shortcut-negative buckets require rejected `nasty_negative` or `negative`
  rows

`negative` is an allowed shortcut-negative row class, not an additional
mandatory row class when the frozen shortcut bucket is already covered by a
rejected `nasty_negative` row.

Additional `negative` rows are required whenever:

- a shell-only attached row or legacy alias survives without the admitting gate
- a required certification family can degrade to `SupportOnly` or `Deferred`
- the exact support witnesses are present but the frozen projection gate is not
- a strengthener is present but the admitting carrier bundle is not

`SupportOnly` and `Deferred` lower claims remain fail-closed here too.

They may strengthen explanation after admission, but they do **not** admit a
projection band.

For broader deployment, one exact-board row per rival is not enough.

Broad-ready projection validation must also cover:

- route diversity where the same strategic band can arise through multiple exact
  structures
- phase or material-regime diversity when the band is known to survive outside
  one narrow slice
- repeated same-cluster near-miss pressure so abstract support bundles do not
  over-admit in wider corpora

These broader-deployment rows are counted only when they carry explicit
`coverageAxis` / `coverageBucket` tags that match a frozen band-level breadth
gate.

### Projection Row Shape

Each projection scaffold row must declare:

- `id`
- `caseType`
- `fen`
- `band`
- `expectation`
- `owner`
- `anchor`
- `requiredWitnessIds`
- `requiredObjectFamilies`
- `requiredDeltaFamilies`
- `requiredCertificationFamilies`
- `supportShellIds`
- `optionalStrengtheningFamilies`
- `supportWitnessIds`
- `rivalBands`
- `forbiddenShortcuts`
- `coverageAxis`
- `coverageBucket`

For start-ready-only rows, `coverageAxis` and `coverageBucket` may be `null`.

They become mandatory once a row is intended to count toward a future
`broad-ready` claim.

A non-null `coverageAxis` / `coverageBucket` pair counts toward coverage only
when the row's `caseType` and `expectation` satisfy that axis's burden:

- positive route/scope/durability/access/creation/release/initiative/denial/
  survival axis: `exact` + `admitted`
- same-cluster near-miss axis: rejected `near_miss`, `contrastive`, or
  `comparative_false_rival`
- shortcut-negative axis: rejected `nasty_negative` or `negative`

Current start-ready admission rows may additionally declare runtime companion
fields such as `admissionPath`, `requiredSupportSeedIds`, `evidenceClaims`,
and S25 rank-access payload fields such as `entrySquare` and `corridorKind`.
Those fields are optional extensions to the documented projection row shape and
do not count as broad-ready coverage by themselves.

Delta-backed projection coverage rows may additionally declare `fenBefore`,
`playedMove`, and `fenAfter`. These three fields are all-or-nothing and are
validated through the live exact delta extractor before a required delta family
such as `TradeInvariant` may count as lower-carrier evidence.

Recommended row shape:

```json
{"id":"projection-s05-center-release-exact","caseType":"exact","fen":"6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1","band":"S05","expectation":"admitted","owner":"white","anchor":"center","requiredWitnessIds":["available_lever_trigger","pawn_push_break_contact_source"],"requiredObjectFamilies":["CentralContactFront"],"requiredDeltaFamilies":[],"requiredCertificationFamilies":[],"supportShellIds":["center"],"optionalStrengtheningFamilies":["InitiativeWindow","file_lane_state"],"supportWitnessIds":["available_lever_trigger"],"rivalBands":["S06","S14","S21"],"forbiddenShortcuts":["open_center_wording_only","center_shell_only"],"coverageAxis":"center_release_route","coverageBucket":"center_pawn_target","admissionPath":"center_pawn_target","requiredSupportSeedIds":[],"evidenceClaims":[{"kind":"center_release_route_certified","anchor":"square:d5","targetSquare":"d5","contactSourceSquare":"c2","centerReleaseRoute":"center_pawn_target"}]}
```

Current contract-closure status:

- `projection-expectations.jsonl` is the required target scaffold for this
  layer; current `S05`/`S06`/`S07`/`S08`/`S11`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S21`/`S22`/`S23`/`S24`/`S25` start-ready status is tied to
  the populated rows and `ProjectionExpectationCorpusTest`
- live/runtime broad deployment remains ungranted branch-wide; the current test
  suite reports the wave-7 global closure set as complete broad-coverage bands
  from the JSONL corpus
- start-ready bands and unresolved blockers live in
  [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md)
- the previous unresolved `S05`/`S13`/`S14`/`S17`/`S18`/`S19`/`S22`/`S23`/`S24` blockers and the `S25`
  start-ready candidate are closed at the start-ready boundary; their live
  runtime admission remains narrow and does not grant broad deployment

### Layer 5: Planner Validation

Purpose:

- prove that only certified typed deltas choose primary question lanes

Checks:

- `WhyThis`
- `WhatChanged`
- `WhatMattersHere`
- `WhyNow`
- `WhatMustBeStopped`
- `planner_none`

Primary corpora:

- `planner-expectations.jsonl`

### Layer 6: Surface Validation

Purpose:

- prove that replay or wrapper surfaces do not reinflate weakened truth

Checks:

- planner vs renderer parity
- support-only non-revival
- deferred non-revival
- whole-game wrapper leak checks

Primary corpora:

- `surface-expectations.jsonl`

## Corpus Taxonomy

Every family or slice must eventually carry rows in these categories.

| Case type | Meaning |
| --- | --- |
| `exact` | direct positive |
| `negative` | direct negative |
| `contrastive` | asymmetric contrast that should survive |
| `near_miss` | almost right but should still fail |
| `nasty_negative` | attractive but wrong picture |
| `move_local_false_witness` | wrong transition signal |
| `comparative_false_rival` | wrong comparative counterpart |
| `surface_reinflation` | replay surface tries to revive weak truth |
| `engine_instability` | result shifts too much across depth or multipv |
| `best_defense_breaks_claim` | nice line fails under concrete defense |

The strategic-object branch already established the core exact-board pattern:

- `primitive-expectations.jsonl`
- `object-expectations.jsonl`
- `delta-expectations.jsonl`
- `comparative-support-expectations.jsonl`
- `current-position-fixed-target-expectations.jsonl`
- `current-position-coordination-probe-expectations.jsonl`

The new branch should preserve that layered style rather than inventing one big
mixed corpus.

## Engine And Probe Validation

Engine evidence is not part of the root-state vector.

It is a separate validation channel consumed at root broad-validation confound
filtering time, delta time, and certification time.

### Engine Evidence Uses

Engine or probe evidence is required for:

- root broad-validation confound rejection for engine-required structural
  schemas
- best-defense survival
- move-local persistence
- comparative superiority
- counterplay denial
- conversion route survival
- tactical release detection
- shallow-vs-real comparative separation

Engine evidence is optional or absent for:

- most static root extraction
- most static witness admission
- low-risk position-local object presence

### Engine Validation Principle

Do not certify from raw centipawn values alone.

Instead, normalize engine output into categorical evidence:

- top-line survives
- best defense refutes
- best defense preserves
- branch gap thin
- branch gap clear
- continuation stable
- continuation unstable
- tactical release present
- tactical release absent

### Probe Contract Fields

Every engine-backed validation row should carry or derive:

- `purpose`
- `depthFloor`
- `realizedDepth`
- `multiPv`
- `baselineMove`
- `baselineEvalCp`
- `baselineMate`
- `variationHash`
- `engineConfigFingerprint`

The old branch already used this shape in
`lila-docker/repos/lila/modules/llm/src/main/scala/lila/llm/model/ProbeModel.scala`
and `ProbeDetector.scala`.

### Depth And MultiPV Policy

The new branch should not tie truth to one exact numeric score.

Instead, use a depth ladder and branch-stability test.

Recommended ladder for engine-required rows:

- `depth 12`
- `depth 16`
- `depth 20`

Recommended MultiPV:

- `1` for simple persistence checks
- `2` for narrow branch contrast
- `3` for best-defense and reply-branch audits

A claim may be:

- `Certified` only if the categorical verdict stays stable at or above the
  declared depth floor
- `SupportOnly` if the idea persists but the branch burden is thin
- `Deferred` if the verdict is depth-unstable or reply-incomplete

## JSONL Artifact Plan

The new branch should build one layered corpus directory under
`modules/commentary/src/test/resources/commentary-corpus/`.

Minimum files:

- `root-expectations.jsonl`
- `witness-expectations.jsonl`
- `object-expectations.jsonl`
- `delta-expectations.jsonl`
- `certification-expectations.jsonl`
- `planner-expectations.jsonl`
- `surface-expectations.jsonl`
- `engine-probe-expectations.jsonl`

The current branch should keep all of these files present even before every
later-layer extractor is live.

`object-expectations.jsonl` is now a live validation corpus for `Object 7`,
not a pre-implementation scaffold. Current-worktree object validation is
carried by `StrategicObjectCorpusRuntimeTest`.

### Suggested Root Row Shape

The root corpus row should be schema-generic rather than square-only.

Recommended shape:

```json
{
  "id": "r-weak-square-d5",
  "caseType": "exact",
  "fen": "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
  "schema": "weak_square",
  "polarityColor": "white",
  "trueSquares": ["d5", "f5"],
  "trueFiles": [],
  "trueState": null,
  "expectedMask64": "0x0000002800000000"
}
```

Rules:

- square schemas use `trueSquares`
- file schemas use `trueFiles`
- aux/state schemas use `trueState`
- `expectedMask64` is optional but recommended for square/file schemas
- `expectedMask64` is optional for square schemas
- `expectedFileMask8` is optional for file schemas
- debug-oriented coordinate arrays and machine-oriented masks should coexist

### Suggested Root Presence Row

Presence-style rows are still allowed when multiple schemas are checked
together.

```json
{"id":"r-fixed-pawn-c6","caseType":"exact","fen":"r1bqrnk1/1p2bppp/2p2n2/p2p2B1/3P4/P1NBP3/1PQ1NPPP/1R3RK1 b - - 0 12","aux":{"sideToMove":"black","castlingRights":"-","enPassant":"-"},"expectPresent":["fixed_pawn:black:c6","controlled_by:white:d5"],"expectAbsent":["passed_pawn:black:c6"]}
```

### Suggested Witness Row

```json
{"id":"u-weak-pawn-d4","caseType":"exact","fen":"4k3/8/8/3p4/3P4/8/8/4K3 w - - 0 1","descriptorId":"weak_pawn_target_state","owner":"black","expectation":"present","coverageAxis":"weakness_tag","coverageBucket":"fixed","requiredRoots":["piece_on:white:pawn:d4","fixed_pawn:white:d4"],"forbiddenRoots":["passed_pawn:white:d4"]}
```

### Suggested Certification Row

```json
{"id":"cert-certified-king-safety-edge-best-defense","caseType":"best_defense_breaks_claim","fen":"6k1/8/6p1/4B3/8/8/7R/6K1 w - - 0 1","expectation":"deferred","family":"CertifiedKingSafetyEdge","owner":"white","scope":"comparative","anchor":"board","burdenTag":"king_safety_edge_certification","helpers":["attack_host_viability","attacker_budget_present","move_order_relevance_gate","best_defense_survival","major_piece_presence"],"requiredSupportFamilies":["AttackScaffold","ComparativeKingFragility"],"engineRequirement":"required","enginePurposes":["comparative_superiority","best_defense_survival"],"forbiddenShortcuts":["attack_scaffold_alone","comparative_fragility_alone","phase_proxy_only"]}
```

## Persisted Audit Evidence Rules

Persisted audit evidence must be durable, tracked, and rerunnable from the
repository.

What counts:

- a tracked Markdown ledger under `modules/commentary/docs/`
- a tracked runnable artifact linked from that ledger
- for exact-board claims, tracked JSONL corpus rows under
  `modules/commentary/src/test/resources/commentary-corpus/` plus a
  corpus-driven test under `src/test`
- for internal exact-board witness claims, a tracked witness-level test under
  `src/test` is enough only when the wording stays at that internal boundary
- for boundary/integration claims, a tracked test or tiny consumer under
  `modules/commentary/src/test` that exercises the same public facade named in
  the wording

What does **not** count by itself:

- chat output
- subagent chat output
- an internal extractor test when the documentation claims a public
  `CommentaryCore` guarantee
- `tmp/` or `tmp_*`
- `target/`
- orchestrator state directories
- `.manual-verification/`

Use repository docs to state the authorized wording, and use tracked runnable
artifacts to carry the proof.

Boundary wording must fail closed too.

If the proof only reaches an internal extractor or scope contract, the docs
must not widen that into a public `CommentaryCore` contract.

## Large-Scale Position Sweep Method

The system should be evaluated on many positions by exact cell, not by one
global score.

Recommended sweep axes:

- phase:
  - opening
  - middlegame
  - transition
  - endgame
- posture:
  - clearly better
  - slightly better
  - equalish
- material regime:
  - minor-piece heavy
  - heavy-piece
  - queenless
  - pure endgame
- claim family:
  - target fixation
  - simplification
  - restriction
  - king-shell pressure
  - route bind
  - passer

The output should be cell-local metrics, not one aggregate number.

## Required Metrics

| Metric | Meaning |
| --- | --- |
| `root_exactness` | deterministic root extraction correctness |
| `witness_precision` | witness positives that should really be positive |
| `witness_near_miss_rejection` | near-miss fail-closed quality |
| `object_precision` | object presence correctness |
| `delta_scope_precision` | correct move/position/comparative scope |
| `certified_precision` | `Certified` claims that were truly safe |
| `support_only_containment` | support-only did not revive as stronger truth |
| `deferred_discipline` | insufficient proof stayed deferred |
| `planner_primary_precision` | primary question lane correctness |
| `surface_non_reinflation` | replay surfaces did not strengthen weak truth |
| `engine_stability_rate` | engine-required claims stable across declared depth ladder |

## Acceptance Gates

No new family or slice should advance because it “looks promising”.

A slice may advance only if:

1. root and witness rows are green
2. object and delta rows are green
3. best-defense and engine-required rows are green
4. support-only and deferred rows remain fail-closed
5. planner primary stays correct
6. surface reinflation remains blocked

## Implementation Consequence

The codebase should mirror the corpus structure.

That means:

- one extractor boundary for `R`
- one witness admission boundary
- one object synthesis boundary
- one delta projection boundary
- one certification boundary
- one planner boundary
- one replay/surface boundary

No layer should own another layer's failure class.
