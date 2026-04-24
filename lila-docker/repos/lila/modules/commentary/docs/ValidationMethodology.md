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

Source-context validation adds a separate metadata gate. Opening move-stat
sources are admitted only when manifest scope, use, license, provenance,
checksum, storage policy, and parser version are explicit. Online trend and
master reference statistics are context references, not exact-board truth:
they may not create claims about optimal moves, theoretical truth, forced lines,
results, or engine evidence. Master reference rows additionally require
attribution/share-alike notice, OTB/online scope, player-level policy, date
window, time-control scope,
dedupe policy, annotation policy, and per-position sample-size threshold before
any local-only ingest can be treated as a valid source statistic.
Opening source consumption adds one more executable metadata gate:
`OpeningContextCandidate` fixtures must preserve exact `positionKey` and source
taxonomy identity, keep master-reference and online-trend vectors separate,
downgrade or suppress below-threshold master rows, reject pipeline-smoke-only
input, and defer specific game citation to retrieval.

Endgame study source-context validation is exact-board context validation, not
oracle validation. Study labels are admitted only when `materialClass`,
`sideToMove`, placement rules, and relation rules are all satisfied by
FEN-derived board evidence. The admitted labels may carry human-known technical
pattern context and candidate-plan tokens, but they must not claim win, draw,
loss, forced conversion, tablebase/Syzygy truth, WDL/DTZ/DTM, best moves, or
Sxx admission. Labels that need pawn-race calculation, fortress certification,
tempo-route proof, or tablebase-style result evidence remain deferred until a
separate exact-board certification owner exists.
The exact-board helper also checks piece ownership where material identity
depends on it: rook-pawn-vs-rook requires one rook for each side, and wrong
rook-pawn bishop context requires the bishop to belong to the pawn side.
Opposition rows require empty king lines for the declared opposition relation,
and Vancura/Lucena subcontexts require their declared rook geometry relation
rather than material counts alone.
Accepted endgame fixture refs must pair `endgame-study:<studyId>:applicable`
with `endgame-study-applicability:<fixture-id>`.

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
- the current live certification registry exposes `12 active certification
  families`:
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
- `CertificationEngineEvidenceContract` is the frozen typed Engine E admission
  adapter for certification-bound evidence; `CertificationEngineEvidence.fromProbe(...)`
  remains fail-closed empty, and UI/API raw probe sidecars are not
  certification evidence unless they are later normalized through that typed
  adapter
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
| `SpaceBindRestrictionCertification` | `space_bind_persistence` | none | `best_defense_survival`, `tactical_release_detection` | `SupportOnly` |

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

For `S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25`,
`projection-expectations.jsonl` is now populated with exact, near-miss,
nasty-negative, and where applicable `comparative_false_rival` rows that
exercise the frozen admission scaffold.

No coverage-complete-only strategy band is broad-deployed or live as runtime
projection on the current branch. The clustered positional-access, conversion/hold/target, initiative/release/counterplay, king-attack,
pawn-structure, and passer creation/suppression set
`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`,
`S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`,
`S23`, `S24`, and `S25` is coverage-complete under the current JSONL/test
broad-coverage gates. `S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25` keep
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

A future live-deployment `coverage-complete` claim is allowed only when:

- the band is already `start-ready`
- every required `coverageAxis` / `coverageBucket` pair frozen in
  `StrategyProjectionBoundaryMatrix.md` is present in
  `projection-expectations.jsonl`
- the band's explicit coverage-complete blocker is closed

Coverage-only rows close the executable broad coverage gate without satisfying
this live-deployment prerequisite. Coverage completion is not live projection
runtime admission.

The S01-S25 coverage contract closes the owner split: `StrategyProjectionCoverageContract`
is the executable owner for coverage gates, lower-carrier ownership,
helper/admission laws, and exact-board validation scaffolds for every
`S01-S25` band. `ProjectionExpectationCorpus.requiredCoveragePairsFor` derives
from that contract, and `ProjectionExpectationCorpusTest` asserts that the
contract-owned key sets match the clustered corpus bands exactly. The runtime
boundary remains fail-closed because `StrategyProjectionAdmission` admits only
`StrategyProjectionScopeContract.startReadyBandIds` (`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`,
`S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25`) and rejects every
`StrategyProjectionCoverageContract.coverageOnlyBandIds` entry.

This is executable, not just documentary: `ProjectionExpectationCorpusTest`
computes missing required coverage pairs from the JSONL corpus and keeps the
current branch runtime fail-closed even when those pairs are complete.
The coverage-presence counter is not raw tag counting: positive route, scope,
durability, access, creation, and suppression buckets count only `exact` /
`admitted` rows; same-cluster near-miss buckets count only rejected near-miss
or contrastive or `comparative_false_rival` rows; shortcut-negative buckets
count only rejected nasty-negative or negative rows.

The current clustered positional-access executable coverage-complete gate-band set is
`S06` / `S09` / `S10` / `S12` / `S20` / `S23` / `S25`.

The S01-S25 projection coverage closure set is exactly `S01`, `S02`, `S03`, `S04`, `S05`,
`S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`,
`S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25`. The current
JSONL corpus reports that full set as coverage-complete for the clustered positional-access,
conversion/hold/target, initiative/release/counterplay, king-attack, pawn-structure, and passer creation/suppression broad gates. This is
corpus/validation-only: those bands do not become broad-deployed, and live
runtime admission remains limited to explicit `startReadyBandIds` rather than
being granted by coverage completion alone.
S09 now has a narrow live runtime admission branch.
`file_penetration_route_certified` is the live evidence kind listed in
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand` for `S09`. The
exact-board burden is
`file_penetration_route_requires_owner_file_lane_state`,
`file_penetration_route_requires_same_file_entry_or_penetration_consequence`,
`same_task_projection_evidence_must_mirror_s09_owner_file_source_entry_and_route`,
`s02_attack_s23_king_activity_s25_rank_access_or_optional_strengthening_is_non_admitting`,
and
`wrong_file_wrong_source_wrong_entry_wrong_route_stale_or_support_only_evidence_is_non_admitting`.
The helper law
`same_task_projection_evidence_must_mirror_s09_owner_file_source_entry_and_route_law`
and exact validation scaffold
`wrong_file_wrong_source_wrong_entry_wrong_route_stale_or_support_only_evidence_not_counted`
are executable freeze names. Lower ownership is
`ProjectionEvidence:file_penetration_route_certified`
beside `file_lane_state` / `rook_on_open_file_state`; Object and Certification
families remain support-only. Runtime boundary tokens
keep broad S09 rows countable while only exact
`file_penetration_route_certified` rows are live admission authority; `S02`,
`S23`, and `S25` adjacent rivals retain independent meanings, `S01` keeps its
separate exact `king_wing_storm_route_certified` branch, `S02` keeps its
separate exact `king_ring_concentration_route_certified` branch, `S03` keeps
its separate exact `diagonal_king_attack_route_certified` branch, and `S04`
keeps its separate exact `king_shelter_breach_route_certified` branch.
S20 is a live exact-slice exception inside the domination coverage cluster:
`domination_route_requires_certified_same_board_mobility_comparison`,
`mobility_plus_restriction_requires_same_owner_short_run_slider_gate_restriction`,
`defender_starvation_requires_same_owner_duty_bound_defender`, and
`same_task_projection_evidence_must_mirror_s20_route_owner_anchor_and_support`
are enforced by `StrategyProjectionAdmission`, while
`mobility_domination_route_certified` is the live evidence kind. Runtime
boundary checks still require exact-board negative coverage for
`wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_not_counted`
and reject adjacent `S06`/`S12`/`S17` rivals unless the S20 route carrier,
same-board lower certification, and evidence payload all match.
S10 is additionally live-admitted only for the exact same-anchor knight-outpost
occupation slice: `weak_outpost_square_state` and `knight_on_outpost_square`
must agree on one anchor square, `outpost_occupation_route_certified` is the
live evidence kind, and runtime evidence must mirror the same anchor, same
outpost square, and `knight_only_outpost_occupancy` or
`same_anchor_eviction_denial` route. Runtime boundary checks still reject S12
weak-square/diagonal access rivals, good-piece wording, non-durable occupancy,
non-knight occupancy, stale evidence, and wrong-anchor/wrong-route claims.

S12 is now live runtime-admitted only for the exact local access-superiority
slice. It is listed in `StrategyProjectionScopeContract.startReadyBandIds`, and
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand` lists
`local_access_superiority_route_certified`. The exact-board burden is:
`access_route_requires_same_anchor_weak_outpost_or_diagonal_lane`,
`access_route_requires_support_linked_short_run_restriction_reinforcement`,
`local_access_superiority_requires_same_owner_anchor_route_and_support`,
`same_task_projection_evidence_must_mirror_s12_owner_anchor_route_and_support`,
`s03_diagonal_attack_s10_outpost_s20_mobility_or_optional_strengthening_is_non_admitting`,
and
`wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_is_non_admitting`.
Lower ownership remains with `weak_outpost_square_state` or king-independent
`diagonal_lane_only` plus same-local `short_run_slider_gate_restriction`.
`CertificationSupportOnly:MobilityComparison(non_truth_owner)` stays support
only and is not S12 projection truth. Exact validation keeps
`projection_evidence_mirrors_s12_owner_anchor_route_and_support`,
`wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_not_counted`,
and S03/S10/S20 false-rival rejection visible. Coverage rows remain countable,
but only exact S12 evidence rows are live admission authority; coverage-only
rows without live evidence and adjacent S03/S10/S20 rivals stay fail-closed.

The current runtime-start-ready subset (`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`,
`S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25`) and the S15/S16 passer creation/suppression
coverage split have executable broad coverage rows in
`projection-expectations.jsonl`, with the frozen gates owned by
`StrategyProjectionCoverageContract` and `StrategyProjectionCoverageContractTest`.
This adds countable `coverageAxis` / `coverageBucket` rows while keeping live
projection admission limited to explicit narrow branches:

- `S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and
  `S25` keep start-ready admission scaffolds
- S16 rows remain countable broad coverage rows, but live admission is limited
  to exact same-enemy-passer suppression routes with
  `passer_suppression_route_certified` evidence
- the coverage set passes the normal coverage-presence burden above

`S05`, `S07`, `S08`, and `S21` now have executable initiative / release /
counterplay coverage-complete rows. Their gates are owned by
`StrategyProjectionCoverageContract` and verified by
`StrategyProjectionCoverageContractTest` plus `ProjectionExpectationCorpusTest`.
This closes the initiative/release/counterplay coverage-completion gate:

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
`missingRequiredCoveragePairsByBand`, and `bandsWithCompleteCoverage`.
`StrategyProjectionAdmission` admits S07 only with exact
`initiative_conversion_route_certified` evidence plus same-owner certified
DevelopmentComparison / InitiativeWindow lower support; S08 admits only with
exact `counterplay_denial_route_certified` evidence plus same-board
InitiativeWindow lower support, while S21 rejects source-only,
initiative-only, stale, or adjacent-rival rows fail-closed.

Live `S01`, live `S02`, live `S03`, and live `S04` now have executable king-attack
coverage-complete rows. Their gates are owned by
`StrategyProjectionCoverageContract` and verified by
`StrategyProjectionCoverageContractTest` plus `ProjectionExpectationCorpusTest`.
This closes the king-attack coverage-completion gate for all four rows and opens
only the exact S01, S02, S03, and S04 live-runtime slices:

- `S01`: non-center same-wing owner contact plus same-defender attack and certified
  king-safety edge; castling-side context and wing-shell wording remain
  fail-closed. Its live exact validation scaffold is now frozen as
  `non_center_same_wing_owner_contact_source_and_target_present`,
  `attack_scaffold_same_defending_king_present`,
  `certified_king_safety_edge_same_owner_present`,
  `same_task_projection_evidence_must_mirror_s01_source_target_defending_king_and_route`,
  `wrong_source_wrong_target_wrong_king_wrong_route_stale_or_support_only_evidence_not_counted`,
  and `castling_side_context_ignored`; the live admission burden is
  `king_wing_storm_route_requires_same_anchor_lever_and_contact_source`,
  `king_wing_storm_route_requires_non_center_owner_wing_contact_source_and_target`,
  `king_wing_storm_route_requires_same_defending_king_attack_scaffold_and_certified_edge`,
  `same_task_projection_evidence_must_mirror_s01_source_target_defending_king_and_route`,
  `s05_center_release_s21_counterplay_castling_shell_or_optional_strengthening_is_non_admitting`,
  and
  `wrong_source_wrong_target_wrong_king_wrong_route_stale_or_support_only_evidence_is_non_admitting`.
  Runtime evidence kind `king_wing_storm_route_certified` must mirror the same
  source, target, defending king, and route. S05 center release, S21
  counterplay survival, center-edge release, optional strengthening, stale evidence, and lower
  object/certification support-only bundles remain non-admitting.
- `S02`: direct king-ring concentration; `AttackScaffold`,
  `CertifiedKingSafetyEdge`, lane support, or king-attack wording alone cannot
  admit the projection band. The live exact validation scaffold now includes
  `same_task_projection_evidence_must_mirror_s02_owner_defending_king_ring_targets_source_set_and_route`,
  `wrong_owner_wrong_king_wrong_targets_wrong_sources_wrong_route_stale_or_support_only_evidence_not_counted`,
  and the evidence name `king_ring_concentration_route_certified`; the live
  branch admits only when the evidence mirrors the same defending king, source
  set, king-ring target set, and route
- `S03`: king-facing `diagonal_lane_only` plus same-king fragility and
  certified king-safety edge; bishop-pair state and non-king diagonal pressure
  remain fail-closed. The live validation scaffold additionally freezes
  `attack_scaffold_only`, `certification_support_only`,
  `diagonal_certification_without_scaffold`, and
  `scaffold_certification_without_diagonal` as exact-board nasty negatives.
  `diagonal_king_attack_route_certified` is the live evidence kind only for
  S03; stale, wrong-owner, wrong-king, wrong-source, wrong-route, support-only,
  object-only, certification-only, S02 concentration, and S12 access rows stay
  fail-closed unless exact S03 evidence mirrors the current-board carrier.
  The exact lower carriers remain support-only:
  `ObjectSupportOnly:AttackScaffold(non_truth_owner)` and
  `CertificationSupportOnly:ComparativeKingFragility|CertifiedKingSafetyEdge(non_truth_owner)`.
- `S04`: same-defender `KingSafetyShell`, shell-payload or support-break
  breach, and same-owner `CertifiedKingSafetyEdge` bound to that defender
  shell; shell-only weakness and generic attack pressure remain fail-closed.
  Positive S04 broad rows must declare `KingSafetyShell` as required defender
  lower object support, not optional strengthening; the support-break row must
  additionally declare exact diagonal-lane support, so
  `shell_payload_and_support_break_use_distinct_declared_support_burdens`.
  Its live validation scaffold now additionally freezes
  `same_task_projection_evidence_must_mirror_s04_owner_defending_king_shell_anchor_breach_squares_and_route`,
  `wrong_owner_wrong_defender_wrong_shell_wrong_route_stale_or_support_only_evidence_not_counted`,
  `s01_s02_s03_s24_false_rivals_not_counted`,
  `optional_strengthening_not_counted`, and the evidence name
  `king_shelter_breach_route_certified` as live only when it mirrors the same
  owner, defending king, shell anchor, breach squares, route, and
  `CertifiedKingSafetyEdge`

S01, S02, S03, and S04 exact rows now have separate live admission slices. Their required coverage pairs are
counted by `ProjectionExpectationCorpus.requiredCoveragePairsFor`,
`coveragePairsByBand`, `missingRequiredCoveragePairsByBand`, and
`bandsWithCompleteCoverage`. `StrategyProjectionAdmission` admits S01
only through exact same-anchor `king_wing_storm_route_certified` rows and S02
only through exact same-king `king_ring_concentration_route_certified` rows;
S03 only through exact same-king `diagonal_king_attack_route_certified` rows;
S04 only through exact same-defender `king_shelter_breach_route_certified`
rows with same-owner `CertifiedKingSafetyEdge` support. S04 coverage rows
remain countable, but any evidence-empty, stale,
wrong-owner, wrong-defender, wrong-shell, wrong-route, support-only,
shell-only, certification-only, attack-scaffold-only,
optional-strengthening-only, S01 storm, S02 concentration, S03 diagonal attack,
or S24 prepared-target row must stay fail-closed unless the live branch
revalidates the exact same-defender carrier.

`S11`, `S13`, and `S14` now have executable pawn-target /
structural-damage coverage-complete rows. Their gates are owned by
`StrategyProjectionCoverageContract` and verified by
`StrategyProjectionCoverageContractTest` plus `ProjectionExpectationCorpusTest`.
This closes the pawn-structure coverage-completion gate for all three rows and the
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
`missingRequiredCoveragePairsByBand`, and `bandsWithCompleteCoverage`.
Adjacent S05/S11/S13/S15 rivals, stale evidence, and optional strengthening
remain fail-closed; S15 admits only through its explicit same-candidate
creation branch, and S16 admits only through its explicit same-enemy-passer
suppression branch.

For the conversion/hold/target five-band coverage set, exact validation stays board-bound:

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

`S15` and `S16` are passer creation/suppression coverage-complete bands. S15 now has narrow live
admission: per-position admission remains disjunctive over `s13_wing_damage`
or `s14_chain_base`, while coverage-complete rows prove both route buckets plus
exact false-rival boundaries against `S13`, `S14`, and `S16`. The S15
exact-board law is same-candidate: the `candidate_passer` root pawn must be the
same pawn as the S13/S14 contact source, while create-passer shell-only,
candidate-only, existing-passer-only, and split-anchor route rows remain
rejected. The S15 evidence kind `passer_creation_route_certified` is live in
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand`, but it is only
an admission companion and does not own lower root or certification truth.
`S16` now has narrow live admission. Its coverage-complete rows prove
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

For `S25`, the current coverage-complete route bucket is only
`rank_access_route = cross_wing_rank_switch`; wider horizontal-rank meanings
remain future scope and do not count yet.

### Projection Core Matrix

Before a band may be claimed projection-complete or coverage-complete,
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

Coverage-complete projection validation must also cover:

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
`coverage-complete` claim.

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
do not count as coverage-complete rows by themselves.

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
  layer; current `S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25` start-ready status is tied to
  the populated rows and `ProjectionExpectationCorpusTest`
- live/runtime broad deployment remains ungranted branch-wide; the current test
  suite reports the S01-S25 projection coverage closure set as complete broad-coverage bands
  from the JSONL corpus
- start-ready bands and unresolved blockers live in
  [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md)
- the previous unresolved `S05`/`S13`/`S14`/`S17`/`S18`/`S19`/`S22`/`S23`/`S24` blockers and the `S25`
  start-ready candidate are closed at the start-ready boundary; their live
  runtime admission remains narrow and does not grant broad deployment

### Layer 5: Planner Validation

Purpose:

- prove that only certified typed deltas choose primary question lanes
- prove that synthesis/editor selection chooses lead/support/context/suppress
  only from already admitted typed claims

Checks:

- `WhyThis`
- `WhatChanged`
- `WhatMattersHere`
- `WhyNow`
- `WhatMustBeStopped`
- `planner_none`
- `CommentaryOutline`
- `ClaimBucket`
- `SuppressionReason`
- `wordingStrengthCap`

Primary corpora:

- `planner-expectations.jsonl`

Current selector contract:

- owned by
  [CommentarySelectionContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentarySelectionContract.md)
- executable model under `lila.commentary.selection`
- synthetic selector contract rows in `planner-expectations.jsonl`
- renderer safety scaffold rows in `surface-expectations.jsonl`
- upstream exact-board truth remains owned by Root, Object, Delta,
  Certification, and Sxx admission tests; selector rows validate outline
  bucketing/suppression for typed inputs and do not replace FEN/PGN-backed
  source proof

Selection rules:

- exact-board admitted typed claims are considered before context
- lead bucket priority is `mustLead > shouldLead > canLead`; impact score and
  `novelty` are same-bucket tie-breaks only
- Engine E only reaches selection through Certification
- `EngineCertification` refs require same-root `Certification` evidence with
  matching owner, anchor, route, and scope before they can influence selection
- unscoped `Certification`, `EngineCertification`, or engine-certified board
  reason refs fail closed before ranking
- raw engine evidence is suppressed with `raw_engine_only` and
  `no_board_reason`
- raw engine lower carriers are suppressed on both Sxx Projection claims and
  non-Projection board claims
- source/retrieval context cannot become current-position truth
- source/retrieval refs cannot serve as board-claim evidence
- S24 is not the generic tactic owner
- S24 must carry both `same_target_forcing_realization` and
  `same_target_conversion_certified`
- Sxx lower carriers must carry owner, anchor, route, and scope binding
- ambiguous side-to-move, owner, beneficiary, or defender fails closed
- duplicate suppression is scoped to competing Sxx projections
- renderer requests cannot upgrade `wordingStrengthCap`

Current runtime-expansion selection rows add the S07/S08/S21
initiative/release/counterplay cluster:

- exact admitted S07 initiative conversion can be selected as `shouldLead`
- nonredundant S21 counterplay survival can remain `support`
- adjacent S08 release-denial rivals can be suppressed with `rival_band`
- support-only InitiativeWindow, raw-engine initiative shortcuts, source
  context, and wrong owner/anchor/route/scope inputs fail closed
- renderer strength requests against this cluster still produce
  `renderer_not_allowed`

Current runtime-expansion selection rows also add the S15/S16 passer
creation/suppression cluster with S13/S14 structural support:

- exact admitted S15 can be selected as `shouldLead` only when same-owner
  Root `candidate_passer` and Witness same-candidate creation-route lower
  support are bound on frozen `s13_wing_damage` or `s14_chain_base` routes
- exact admitted S16 can be selected as `shouldLead` only when same-enemy
  defender-owned enemy Witness `passed_pawn_entity_state` and route-specific
  same-owner certification support are bound: `FortressDrawCertification` for
  `blockade_hold`, `short_run_slider_gate_restriction` plus
  `PerpetualCheckHolding` for `restriction_hold`, and `PromotionRace` for
  `non_losing_race`
- S13/S14 support for S15 requires same owner, candidate anchor, and scope
- S13/S14 remain support and do not become S15 truth owners, even when their
  impact score is higher than the admitted S15 claim
- existing-passer-only, blocker-shell-only, split-anchor, forged carrier-kind,
  unsupported-route, wrong owner/anchor/route/scope, ambiguous or incoherent
  beneficiary/defender/side-to-move rival bindings, S16 self-owned passer
  suppression, raw-engine, source-context, retrieval, and renderer-strength
  shortcuts fail closed

Current runtime-expansion selection rows also add the S17/S18/S19/S22
conversion/simplification/hold cluster:

- exact admitted S17 can be selected as `shouldLead` only when same-owner
  Witness `same_piece_liability_anchor_seed`, typed
  `same_piece_repair_route_seed` or `same_piece_exchange_relief_seed` lower
  support, and `liability_relief_certified` projection evidence are bound
- exact admitted S18 can be selected as `shouldLead` only when same-owner
  Witness `bishop_pair_state` and route-specific Certification support are
  bound for `bishop_pair_to_initiative`, `bishop_pair_to_structure`, or
  `bishop_pair_to_material`
- exact admitted S19 can be selected as `shouldLead` only when same-owner Delta
  `TradeInvariant` and route-specific Certification support are bound for
  `trade_invariant_to_material` or `trade_invariant_to_hold`
- exact admitted S22 can be selected as `shouldLead` only when same-owner
  certified `fortress_draw_hold` or `perpetual_hold` support is bound
- declared Projection evidence must be bound to the same owner, anchor, route,
  and scope as the selected S17/S18/S19/S22 claim
- S19 remains below same-owner/anchor/scope S17 liability-relief, S18
  bishop-pair conversion, and S22 hold ownership
- conversion claims remain below certified hold unless result/material impact
  and the `bishop_pair_to_material` route with bound
  `bishop_pair_material_conversion_certified` evidence justify conversion
- generic relief wording, unbacked conversion, wrong owner/anchor/route/scope,
  raw-engine, source-context, retrieval, and renderer-strength shortcuts fail
  closed

Current runtime-expansion selection rows also add the S01/S02/S03/S04
king-attack cluster:

- exact admitted S01 can be selected as `shouldLead` only when same-owner
  Witness `available_lever_trigger`, Witness
  `pawn_push_break_contact_source`, Object `AttackScaffold`, Certification
  `CertifiedKingSafetyEdge`, and bound `king_wing_storm_route_certified`
  projection evidence are present on frozen `same_wing_contact` or
  `attack_edge_same_king` routes
- exact admitted S02 can be selected as `shouldLead` only when same-owner
  Object `AttackScaffold`, Certification `CertifiedKingSafetyEdge`, and bound
  `king_ring_concentration_route_certified` projection evidence are present on
  frozen `direct_piece_concentration` or `lane_strengthened_concentration`
  routes
- exact admitted S03 can be selected as `shouldLead` only when same-owner
  Witness `diagonal_lane_only`, Object `AttackScaffold`, Certification
  `ComparativeKingFragility`, Certification `CertifiedKingSafetyEdge`, and
  bound `diagonal_king_attack_route_certified` projection evidence are present
  on frozen `king_facing_diagonal_entry` or `fragility_linked_diagonal` routes
- exact admitted S04 can be selected as `shouldLead` only when same-owner
  Certification `CertifiedKingSafetyEdge`, defender-owned Object
  `KingSafetyShell`, and bound `king_shelter_breach_route_certified`
  projection evidence are present on frozen `shell_payload_breach` or
  `support_break_breach` routes; `support_break_breach` also requires
  same-owner Witness `diagonal_lane_only`
- owner, defending king anchor, defender, route, and scope must stay clear;
  king-attack projection fails closed when `owner` and `defender` identify the
  same side
- same-king S01/S02/S03/S04 claims remain nonredundant when route or anchor
  keeps exact storm, concentration, diagonal attack, and shelter-breach claims
  distinct
- certified result/conversion owners may outrank king-attack projections, but
  raw engine eval cannot
- generic attack wording, king-side vibes, source/retrieval motif tags, wrong
  owner/anchor/route/scope, raw-engine, and renderer-strength shortcuts fail
  closed

Current runtime-expansion selection rows also add the engine-certified eval
swing versus board-explainable Sxx conflict boundary:

- raw engine eval swing is always suppressed with `raw_engine_only` and
  `no_board_reason`
- bounded `EngineCertification` can influence ranking only through a
  Certification-layer claim with same-root `Certification` evidence and a
  same-binding typed Root, Witness, Object, or Delta board reason
- engine-certified eval swing without that board reason is suppressed with
  `no_board_reason`
- board-explainable admitted Sxx can beat a larger opaque engine-certified
  eval swing
- engine-certified result/material Certification can beat Sxx only when the
  result/material impact and same-binding typed board reason justify it
- stale, wrong-node, wrong-FEN, wrong-route, and wrong-engine-config
  `EngineCertification` refs are suppressed before ranking with
  `stale_evidence`, `wrong_owner`, `wrong_anchor`, `wrong_route`, or
  `scope_mismatch`
- MultiPV ambiguity cannot become best-defense or lead explanation after
  Certification rejection; mate/cp comparison cannot become a scalar ranking
  shortcut unless normalized by the certification contract
- renderer/surface rows freeze that eval swing cannot be upgraded into best
  move, winning, or forced-result wording

Current runtime-expansion selection rows also add source-context fallback
breadth:

- opening context can be emitted as `contextOnly` only when no exact-board
  lead exists; it cannot become best move, theory truth, forced line, result,
  or current-position proof; canonical fallback requires
  `opening-position:*:canonical`, while `opening-position:*:ambiguous` is
  suppressed
- endgame-study context can be emitted as `contextOnly` only with exact
  material/placement applicability matched between `endgame-study:*:applicable`
  and `endgame-study-applicability:*`; result-language and forced-conversion
  shortcuts are suppressed
- retrieval can be emitted only as non-authoritative reference context and
  stays marked with `retrieval_non_authoritative`; retrieval similarity keys
  are closed per `similarityKind`, exact-position keys must match the row
  `positionKey`, opening pawn-structure tokens must match the example FEN,
  motif/endgame refs must resolve to local fixture refs and match the declared
  motif/study plus row FEN when cross-source validation is available, and
  retrieval truth-promotion ids are suppressed
- motif tags require exact detector carrier evidence before they are usable
  context; `motif-example:*` must match `motif-detector-carrier:*`, deferred
  motif ids such as `back_rank_mate` remain suppressed, and tags alone,
  mismatched carriers, or truth/Sxx/result/engine shortcut ids are suppressed
  with `forbidden_shortcut` and `no_board_reason`
- exact-board leads keep source rows bounded to support/context and prevent
  source rows from becoming lead truth
- ambiguous opening transpositions are suppressed with
  `ambiguous_transposition`
- gameContext, practicality, tablebase, and endgame result-service families
  remain deferred or rejected with `forbidden_shortcut`
- renderer/surface rows freeze that source context cannot be upgraded into
  current-position truth, best move, result, forced line, or theory-proof
  wording
- global closure rows freeze that every `S01-S25` start-ready band can be
  selected when it is already admitted with exact owner, anchor, route, scope,
  row-specific lower-carrier role shape, and its frozen allowed projection
  evidence kind. This proves selector breadth only; projection admission,
  cluster-specific rival handling, and suppression laws remain owned by their
  lower contracts and targeted selection rows.
- global closure keeps S06 tied to same-route
  `SpaceBindRestrictionCertification`, and S16 tied to route-specific
  `FortressDrawCertification`, `PerpetualCheckHolding`, or `PromotionRace`
  support, matching the current projection authority.
- selection rival rows must be executable without preloaded suppression hints:
  S07/S08 and S15/S16 `rival_band` is derived by the selector from typed
  admitted claims.
- source-context rows reject opening best-move, theory-truth,
  current-position-proof, forced-line, and result-style refs before selected
  context evidence is emitted.
- selected source-context payloads are capped at `context_only`, and no
  selected claim payload may carry a stronger cap than the computed outline
  `wordingStrengthCap`.
- safe selected source-context rows carry `source_context_only` or
  `retrieval_non_authoritative` as soft context reasons on the selected item,
  not as suppressed claims. `suppressedClaims` remains reserved for rows the
  renderer must not revive.

### Layer 6: Surface Validation

Purpose:

- prove that replay or wrapper surfaces do not reinflate weakened truth
- freeze that renderer/surface code cannot strengthen a selector outline or
  `CommentaryPlan`
- prove that the outline builder maps selector-owned structure without adding
  meaning before renderer prose exists
- prove that the renderer contract maps `CommentaryPlan` into structured
  role-based render output with deterministic role fragments only, and without
  broad chess narration, API wiring, frontend wiring, raw engine intake, or raw
  source truth

Checks:

- planner vs renderer parity
- support-only non-revival
- deferred non-revival
- whole-game wrapper leak checks
- `renderer_not_allowed`
- `wordingStrengthCap` preservation
- selected claim payload cap clamping, with source context capped at
  `context_only`
- no renderer-side lead admission
- no renderer-side ranking
- no renderer-side source-truth conversion
- no renderer-side suppression discard
- no renderer-side raw-engine rendering
- no renderer-side unpaired `EngineCertification` rendering
- no renderer-side board-reason-unbacked `EngineCertification` rendering
- no renderer-side evidence invention
- no renderer-side publication of plan-wide evidence without unblocked public
  selected-claim ownership
- no renderer-side opening `master_reference` / `online_trend` merge
- blocked claims remain non-public suppression metadata by default
- selected-plus-blocked malformed plans cannot revive the blocked claim through
  public blocks, public evidence refs, or public boundary metadata
- hidden and no-commentary plans stay silent without fallback chess narration
  or public evidence refs
- no outline-builder lead selection, support reranking, source-truth
  promotion, suppressed-claim revival, evidence recomputation, or
  wording-strength upgrade
- selected source-context `softReasons` preserved as context boundary metadata
- context-only outlines remain no-main context-only plans
- empty selected commentary sections produce a `noCommentary` plan while
  preserving blocked claims and evidence references
- opening source context stays structured; `master_reference` and
  `online_trend` references are not merged, and specific-game citation remains
  retrieval-lane material
- opening source-context refs carrying specific game, player, event, URL, or
  raw HTTP citation material fail closed before context fallback

Primary corpora:

- `surface-expectations.jsonl`

Executable owner:

- `modules/commentary/src/main/scala/lila/commentary/selection/CommentaryOutlineBuilder.scala`
- `modules/commentary/src/test/scala/lila/commentary/selection/CommentaryOutlineBuilderContractTest.scala`
- `modules/commentary/src/main/scala/lila/commentary/render/CommentaryRenderer.scala`
- `modules/commentary/src/test/scala/lila/commentary/render/CommentaryRendererContractTest.scala`

Contract authority:

- `modules/commentary/docs/CommentaryOutlineBuilderContract.md`
- `modules/commentary/docs/CommentaryRendererContract.md`

## Backend Commentary Seam Validation

The backend seam validation freezes the first future-frontend-facing structured
request/response contract without opening frontend UI, API controller wiring,
live source integration, or model-authored prose generation.

Validation checks:

- exact-board `currentFen` intake fails closed before any render output
- optional `beforeFen` and `playedMove` are all-or-nothing and validate through
  the existing fail-closed delta path
- optional `RuntimeEnginePacket` is accepted only by
  `CertificationEngineRuntimeIntake`
- `EngineEvidencePacket`, raw eval, raw PV, centipawn, mate, and engine labels
  are not response evidence
- accepted engine intake without matching bounded engine evidence refs by
  canonical id, owner, and anchor cannot unlock `EngineCertification` claims
- engine-intake debug reasons are sanitized keys, not raw certification
  validation messages
- response `status` mirrors `CommentaryRender.status` for valid requests
- malformed input returns `invalidRequest` plus silent
  `RenderStatus.NoCommentary`
- public payload carries `CommentaryRender` blocks, evidence refs, boundaries,
  wording, and no-commentary state without requiring frontend reranking
- blocked claims and suppression reasons are hidden by default
- debug/internal metadata is explicitly separate, server-owned, and cannot
  revive blocked claims
- opening source context remains context-only; `master_reference` and
  `online_trend` refs stay separate
- no admitted claim produces no fallback broad chess narration
- JSON request/response round trip preserves stable role/status keys

Executable owner:

- `modules/commentary/src/main/scala/lila/commentary/api/CommentaryBackendSeam.scala`
- `modules/commentary/src/test/scala/lila/commentary/api/CommentaryBackendSeamContractTest.scala`

Contract authority:

- `modules/commentary/docs/CommentaryBackendSeamContract.md`

## Minimal Frontend Bridge Validation

The minimal frontend bridge validation freezes the adapter-only analyse-side
contract for consuming backend `CommentaryResponse` / `CommentaryRender`
payloads before a frontend rewrite.

Validation checks:

- request building emits only `currentFen`, optional paired `beforeFen` /
  `playedMove`, `nodeId`, `ply`, and optional `enginePacket`
- raw source context and opening candidate fields are not accepted as request
  truth
- public decoding returns only public render blocks, public evidence refs,
  public boundaries, and wording cap
- `internal`, blocked claims, suppressions, engine-intake diagnostics, and
  invalid-input reasons are not exposed as public display data
- `noCommentary`, `hidden`, `negative_only`, and `invalidRequest` are silent
  public states
- `contextOnly` remains non-authoritative display and the bridge adds no
  best-move, theory-truth, forced-line, result, or engine wording
- stale `currentFen`, `nodeId`, or `ply` after the async response is discarded
  as `stale_node`
- the bridge does not rank, admit, suppress, revive, reinterpret, merge source
  rankings, render raw engine values, or upgrade wording

Executable owner:

- `ui/analyse/src/chesstory/commentaryBridge.ts`
- `ui/analyse/tests/commentaryBridge.test.ts`

Contract authority:

- `modules/commentary/docs/CommentaryFrontendBridgeContract.md`

## Source Context Validation

Opening, motif, endgame-study, and retrieval source context is validated as
offline test/tooling material.

It is not a new truth layer in the canonical ladder.

The current executable owner is:

- `modules/commentary/src/test/scala/lila/commentary/source/SourceContextCorpus.scala`
- `modules/commentary/src/test/scala/lila/commentary/source/SourceContextCorpusTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/source/opening/OpeningSourceToolingTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/source/opening/OpeningSourceConsumptionTest.scala`
- `modules/commentary/src/test/tooling/retrieval/retrieval_quality_index.py`
- `modules/commentary/src/test/tooling/retrieval/retrieval_quality_index_test.py`

The current fixture files are:

- `opening-sources.jsonl`
- `opening-lines.jsonl`
- `opening-positions.jsonl`
- `opening-move-stats.jsonl`
- `opening-themes.jsonl`
- `opening-aliases.jsonl`
- `opening-context-candidates.jsonl`
- `motif-examples.jsonl`
- `motif-reject-fixtures.jsonl`
- `endgame-studies.jsonl`
- `endgame-study-fixtures.jsonl`
- `endgame-study-reject-fixtures.jsonl`
- `retrieval-examples.jsonl`
- `retrieval-reject-fixtures.jsonl`

Validation rules:

- source manifests allow only `opening`, `motif`, `endgameStudy`, and
  `retrieval`
- game-context and endgame result-service family inputs are rejected by this
  schema and deferred to separate lanes
- unknown license or provenance fails closed
- opening names require exact normalized position keys before they can become
  context
- opening candidates remain statistics or references, not best-move or theory
  truth
- opening display/context aliases must preserve the exact taxonomy source row;
  unsupported Anti-Meran wording remains rejected rather than leaking through
  a generic Meran row
- opening trend-stat source rows require recent year/month, matching
  `yearMonth`, bucket metadata, `ratedOnly`, parser version, checksum, and
  local-only generated storage
- ambiguous opening transpositions are downgraded rather than indexed as
  canonical
- motif tags require exact-board detector carriers before they can count as a
  motif example; admitted motif fixture FENs must still emit their declared
  U/root/certification carrier through the current backend extractors
- the frozen motif source contract validates `MotifExample` rows only as
  source/context/test-tooling material: admitted rows must bind
  `motif-example:<id>` to `motif-detector-carrier:<id>`, declare
  `displayName`, `involvedSquares`, strict `sourceRefs`, and
  `negativeBoundaries`, reject unknown top-level and nested carrier fields, and
  keep `currentPositionClaim=false`
- admitted motif ids are limited to `loose_piece`, `pin`, `fork`, `skewer`,
  `overload`, `trapped_piece`, `mate_net`, and `perpetual_check`; `mate_net`
  and `perpetual_check` require certification carriers
- `discovered_attack`, `deflection`, `back_rank`, `clearance`, and
  `interference` remain deferred/helper-required motif ids until an exact
  current-branch detector contract exists
- endgame-study labels require exact material, placement, and relation
  evidence before they can become context
- endgame-study context cannot claim win, draw, loss, forced conversion, or
  forced line
- admitted endgame-study source fixtures currently cover Lucena, Philidor,
  Vancura, basic opposition, distant opposition, wrong rook pawn with wrong
  bishop, and active rook behind the passed pawn; outside passer, fortress,
  rook on seventh, triangulation, corresponding squares, shouldering,
  breakthrough, and reserve tempo remain deferred
- accepted endgame-study fixtures must bind exact-board applicability refs, and
  reject fixtures must still parse as exact clean FENs before they can count as
  negative evidence
- retrieval examples remain non-authoritative source-backed references and
  cannot become current-position truth. Accepted rows must carry
  `retrievalId`, `sourceId`, `sourceRef`, exact FEN, normalized `positionKey`,
  matching `sideToMove`, typed `similarityKey`, `similarityScore`,
  `similarityKind`, `matchedFeatures`, `sourceQuality`, optional citation
  metadata with license/attribution when present, `snippetRole`,
  `currentPositionClaim=false`, `authority=retrieval_example`, tags, and
  negative boundaries. Reject rows cover missing source/provenance keys,
  FEN/position-key/side-to-move mismatch, low similarity, duplicate source
  refs, truth/result/best/forced wording, coded result/proof tokens such as
  WDL, DTZ, DTM, `1-0`, `0-1`, or `1/2-1/2` in proof-bearing fields,
  result-as-claim leakage, same-opening wrong-structure false positives
  including token-consistent wrong structure, exact-position rows with false
  optional opening structure, motif tag without exact carrier, borrowed
  motif/endgame refs, mixed-feature attempts to bypass or omit the same
  family-specific FEN/ref checks, truth-bearing `retrievalId`/`sourceRef`
  evidence ids, and unknown fields.
- retrieval quality index tooling is validated as local-only test/tooling output:
  `retrieval_quality_index.py` may generate `retrieval-index.jsonl`,
  `retrieval-rejects.jsonl`, and `retrieval-summary.json` only below
  `tmp/commentary-opening/retrieval/`. The quality report records total,
  accepted, rejected-by-reason, duplicate source-ref, duplicate normalized
  similarity-evidence, low-similarity, exact/feature split, and citation
  completeness counts. It also records a `qualityReport` with the local
  quality decision, high-quality accepted count, low-confidence/suppressed
  count, display-candidate count, source-quality comparison, nasty-negative
  reject results, validator-reason reject counts, and the retrieval
  consumption policy. Nasty-negative quality buckets are counted from
  validator reasons, not fixture-facing labels, so reject fixtures cannot
  inflate a validator boundary they did not actually hit. These generated files
  are ignored local artifacts, not runtime inputs. Accepted quality rows require
  license and attribution fields; reject-ledger rows keep fixture
  `rejectReason` as report `reason` and store local validator detail separately
  as `validatorReason`.
- retrieval quality audit passes only when `displayCandidateCount=0`, retrieval
  remains an optional citation/example layer, default prose stays game/player/
  event/result silent, and retrieval never outranks master-reference
  statistics, source statistics, or certification.

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

### Engine E certification evidence freeze

Engine E is certification evidence only. It is not a
`Root/U/Object/Delta/Sxx truth owner`, and projection runtime admission must not
consume E directly. E can affect projection only indirectly as a same-root
`CertificationEvidenceBundle` consumed by certification extraction; projection
then consumes the resulting certified lower carrier, not raw E.

`CertificationEngineRuntimeIntake` is the certification-only live runtime intake
for analyse/backend packets. It is not a renderer, synthesis, projection, or
raw probe sidecar intake. It normalizes packet fields into
`EngineEvidencePacket` and delegates to `CertificationEngineEvidenceContract`,
the exact-board adapter. An engine packet is evidence only after exact FEN,
node identity, freshness, depth, MultiPV, score, and legal-PV checks pass:

- object evidence binds to the current extraction's exact FEN plus node/ply
  identity through the requested full-FEN authority; exact FEN means the
  normalized full-FEN string, including halfmove/fullmove clock fields, not only
  root-state equivalence
- delta evidence must bind `beforeFen`, `playedMove`, and `afterFen` to the
  canonical delta extraction, with literal normalized full-FEN endpoint matches
- stale or incomplete search, insufficient depth, insufficient `MultiPV`,
  illegal PV replay, truncated PV replay, and mismatched node identity fail
  closed
- best-defense and reply-branch claims require at least `MultiPV 3` with
  distinct first moves
- centipawn scores, mate scores, side-to-move perspective, eval swing, and
  conversion thresholds remain typed; mate must not be converted into
  centipawns
- scoreless bounded claims fail closed
- eval swing requires a bound, fresh, same-engine-config baseline packet whose
  FEN matches the transition `beforeFen`; stale or cross-board baselines fail
  closed
- eval-swing baselines must also satisfy node/ply binding: exact transition
  `beforeNode` when supplied, otherwise immediately preceding baseline ply
- Engine E-derived evidence uses the opaque `CertificationEngineEvidence`
  facade and may be converted into certification evidence only by the bounded
  adapter
- inactive certification family ids, unknown purpose/strength keys, invalid
  owner colors, invalid UCI PV moves, and missing typed score requirements fail
  closed at runtime intake or contract validation
- missing or rejected runtime E packets keep the base certification evidence
  path intact and do not upgrade a `SupportOnly` or `Deferred` endpoint
- best-defense, eval-shift, conversion, or persistence admission requires a
  bounded claim with declared purposes and passing typed burdens

If E is absent or insufficient, the result is
`CertificationEvidenceBundle.empty`, a rejected bounded claim, or a weaker
certification endpoint such as `SupportOnly` / `Deferred`; raw engine narration
does not upgrade the verdict.

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

Current-worktree validation authority is the commentary certification and
probe scaffold under `modules/commentary`; branch-external probe paths are not
live authority for this worktree.

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
