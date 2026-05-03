# Commentary Core SSOT

This branch rebuilds the commentary backend around a pre-render semantic model.
The previous commentary documentation set is archived under
`legacy-pre-semantic-reset/` and is no longer live authority.

## Core Decision

The backend model is reset to:

`feature extraction -> candidate generation -> semantic scoring/gating -> outline -> renderer`

The renderer is explicitly downstream. It must not create chess meaning, repair
missing evidence, upgrade wording strength, or turn source/engine context into
truth.

## Authority Model

The old documents do not need a gradual authority revocation in this branch.
Their authority is already removed by archival.

Live authority now sits here:

- Root, witness, object, delta, certification, projection, source, and engine
  data are model inputs or hard-gate evidence.
- Public commentary truth is selected only by the semantic selector output.
- `pin`, `xray`, `weak pawn`, opening tags, source rows, raw engine eval, and
  prepared lines are not standalone public truth owners.
- Existing legacy extractors may be reused as feature producers, but their old
  public-claim paths are legacy behavior unless re-admitted through the new
  semantic selector contract.

## Backend Boundary

The first target replacement point is the current claim-selection boundary.
The new selector should consume typed candidates and produce a language-neutral
outline. It should not depend on renderer prose.

The intended runtime boundary is:

```text
CommentaryPipelineInput
  -> lower feature extraction
  -> SemanticCandidate generation
  -> hard gates
  -> HCE-style candidate scoring
  -> top-K SemanticDecision
  -> CommentaryOutline
  -> CommentaryPlan
  -> renderer
```

## Hard-Gate Responsibilities

These remain non-negotiable gates, not soft preferences:

- exact board identity
- legal replay
- owner, defender, anchor, route, and scope binding
- same-root certification binding
- raw engine/source context non-ownership
- public-safe line evidence
- stale evidence rejection
- forbidden shortcut rejection
- renderer non-authority

Weak evidence usually lowers confidence. It becomes a hard failure only when a
candidate would otherwise make a public chess claim without the required
binding.

## Scoring Responsibilities

The semantic model scores what is worth saying after hard gates:

- lead value
- support value
- context value
- confidence
- salience
- specificity
- pedagogical clarity
- tactical urgency
- strategic relevance
- source fit
- line support
- novelty
- redundancy
- abstain pressure

## Renderer Boundary

Renderer work is deferred. Any future template or LLM renderer must consume
only selected structured intent. It may paraphrase; it may not infer new
claims.

## Legacy Policy

The archived documents are useful for diagnosis and migration references only.
They must not be cited as current branch authority unless a new live document
explicitly imports a specific rule.

