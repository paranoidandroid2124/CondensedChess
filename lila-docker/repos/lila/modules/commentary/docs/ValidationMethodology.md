# Validation Methodology

This document defines how the `commentary` backend will be validated over many
exact chess positions without falling back into ad hoc reruns and layered
patches.

The goal is not only to know whether a claim is right.

The goal is to know exactly which layer failed:

- root extraction
- witness admission
- object composition
- delta projection
- certification
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

This mirrors the strategic-object branch format:

```json
{"id":"king-safety-exact","caseType":"exact","fen":"6k1/8/6p1/8/6BP/8/8/6K1 w - - 0 1","expectation":"present","family":"KingSafetyShell","owner":"black","anchor":"g6"}
```

### Layer 3: Delta Validation

Purpose:

- prove that the right object emits the right scope:
  - `move_local`
  - `position_local`
  - `comparative`

Checks:

- typed delta tag correctness
- anchor correctness
- scope correctness
- false-witness rejection
- false-rival rejection

Primary corpora:

- `delta-expectations.jsonl`

This mirrors the strategic-object branch format:

```json
{"id":"pawn-structure-move-exact","caseType":"exact","fen":"6k1/8/8/4p3/3P4/2P5/8/6K1 w - - 0 1","family":"PawnStructureRegime","scope":"move_local","expectation":"present","deltaTag":"BreakDrivenShift","playedMove":"c3c4"}
```

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
{"id":"cert-fixed-target-d6","caseType":"best_defense_breaks_claim","fen":"r2qk2r/1b1nbppp/pp1ppn2/8/2PQ4/BPN2NP1/P3PPBP/R2R2K1 w kq - 2 11","family":"FixedTargetComplex","scope":"comparative","playedMove":"c4d4","engineRequirement":"required","expectation":"support_only","failureReason":"best_defense_not_beaten"}
```

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
