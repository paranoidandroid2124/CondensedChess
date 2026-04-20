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
- `engine-probe-expectations.jsonl` for local Stockfish sanity on the object
  test positions themselves

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

- typed delta tag correctness
- anchor correctness
- scope correctness
- board-coherent `fenBefore -> playedMove -> fenAfter` transition correctness
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

Primary corpora:

- `certification-expectations.jsonl`
- engine/probe bundles

Current `Certification` start-gate rule:

- `certification-expectations.jsonl` is now a live pre-implementation scaffold
  rather than an empty placeholder
- the current frozen certification runtime-family set is:
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
- certification scaffold tests may currently validate:
  - exact-board FEN parseability
  - row-family coverage
  - verdict-lattice coverage
  - engine/probe bundle linkage
  - frozen burden/helper/shortcut contracts
- certification scaffold tests may not yet claim live certification extraction
  because no certification runtime package is live in `src/main`

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

It is no longer acceptable to treat certification as one generic future layer
without row-local burden and engine-purpose freeze.

Projection and renderer are downstream consumers of certified claims.

They are not truth-owning semantic layers.

### Projection Handoff Validation

Purpose:

- prove that each `S01-S24` band has enough certified lower-layer carrier input
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

Blocked strategy bands that still need new lower/support seed families before
live admission are inventoried in
[StrategySupportSeedInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategySupportSeedInventory.md).

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

It is a separate validation channel consumed at delta and certification time.

### Engine Evidence Uses

Engine or probe evidence is required for:

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
{"id":"u-weak-pawn-c6","caseType":"exact","fen":"r1bqrnk1/1p2bppp/2p2n2/p2p2B1/3P4/P1NBP3/1PQ1NPPP/1R3RK1 b - - 0 12","descriptor":"weak pawn","owner":"white","expectation":"present","requiredRoots":["fixed_pawn:black:c6","half_open_file:white:c"],"forbiddenRoots":[]}
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
