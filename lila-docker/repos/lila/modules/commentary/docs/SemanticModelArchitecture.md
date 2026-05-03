# Semantic Model Architecture

This document freezes the first pre-render semantic model target for this
branch.

## Model Style

Version 1 is an HCE-style deterministic scorer. It is not a trained neural
model. All input and output shapes are still fixed so decisions can later be
logged and used for training.

The model replaces selector authority, not renderer authority.

## Input Unit

The model scores one `SemanticCandidate` at a time.

A candidate is built from:

- current request identity
- before/current root state when available
- typed lower feature evidence
- existing claim-shaped data
- prepared variation evidence when available
- source context when available

## Input Vector

Logical candidate input is `8,969` dimensions.

```text
positionVector: 8,713
  R_before: 2,891
  R_current: 2,891
  R_delta: 2,891
  position/move/engine scalars: 40

candidateVector: 256
  claim shape: 32
  family ids: 72
  owner/anchor/route/scope binding: 24
  evidence coverage: 32
  ClaimImpact: 9
  line proof: 40
  source context: 16
  risk/suppression hints: 31
```

Family id slots:

- `0-17`: U-primary descriptor families
- `18`: U-attached family
- `19-25`: strategic object families
- `26-27`: strategic delta families
- `28-39`: certification families
- `40-64`: S01-S25 projection families
- `65-71`: reserved

## Output Vector

The selector emits top `K = 8` candidates.

Each candidate has a `64`-dimension `CandidateScoreVector`, so the fixed
numeric output tensor is `8 x 64 = 512`.

```text
0-15: gate bits
16-31: suppression bits
32-47: scores
48-59: frame scores
60-63: final selection fields
```

Gate bits:

- exact board
- legal replay
- root support
- evidence present
- owner bound
- anchor bound
- route bound
- scope bound
- no raw engine owner
- source safe
- public variation safe
- depth/freshness safe
- same-root certification
- no tactical release
- no forbidden shortcut
- render contract safe

Suppression bits:

- support only
- deferred
- rejected
- anti-case
- stale
- wrong owner
- wrong anchor
- wrong route
- scope mismatch
- no board reason
- raw engine only
- source context only
- forbidden shortcut
- ambiguous transposition
- duplicate or rival
- renderer not allowed

Scores:

- lead
- support
- context
- contrast
- negative
- salience
- confidence
- specificity
- pedagogical clarity
- tactical urgency
- strategic relevance
- line support
- source fit
- novelty
- redundancy penalty
- abstain

Frame scores:

- TacticalAlert
- BlunderOrMaterial
- OpeningStructure
- PawnStructure
- PieceActivity
- KingSafety
- StrategicPlan
- DefensiveResource
- Conversion
- EndgameTechnique
- SourceContext
- NoCommentary

Final fields:

- role code
- wording code
- rank ordinal
- public eligible bit

## Selection Policy

Lead selection requires:

- public eligible
- `leadScore >= 55`
- `confidence >= 50`
- `abstainScore < 60`

Support selection keeps at most two non-duplicate support candidates with:

- `supportScore >= 45`
- redundancy penalty `< 50`

If no candidate qualifies, select `NoCommentary`.

## Pin Policy

`pinned_piece` is a feature, not a public explanation.

A pin-related public lead requires:

- a same-owner `pin` witness geometry
- pinner, blocker, anchor, ray, and mode
- evidence that the pin matters to the selected frame
- no stronger tactical, material, or opening-context contradiction

Otherwise pin evidence stays support/context or silent.

