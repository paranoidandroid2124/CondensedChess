# Chess Commentary SSOT

This branch rebuilds commentary around a pre-render deterministic chess model.
The live backend authority chain is:

`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`

No other path owns current public chess meaning.

## Core Decision

`BoardMood` gathers shared board facts. `Story` is the only chess unit allowed
to compete for commentary. `StoryTable` applies deterministic interaction and
ordering rules. `Verdict` is the language-neutral result consumed downstream.

The renderer is explicitly downstream. It may express selected structured
intent, but it must not create chess meaning, repair missing evidence, upgrade
wording strength, or turn source and engine context into truth.

## Authority Model

Live authority now sits here:

- `BoardMood` holds shared board facts, bit maps, and scalar slots.
- `Story` binds public scene, optional plan, optional tactic, identity, and
  proof.
- `StoryTable` decides lead eligibility, support/context/blocking roles, owner
  interaction, source limits, quiet limits, and deterministic top-K ordering.
- `Verdict` encodes rank, role, lead permission, strength, and story references.
- Chess-facing model names, fixed shape counts, story families, and legacy
  survival rules are governed by `ChessModelContract.md`.

Opening tags, source rows, raw engine eval, prepared lines, pins, xrays, weak
pawns, and similar extracted facts are not standalone public truth owners. They
may feed `BoardMood` or provide proof for a `Story`; they do not bypass
`StoryTable`.

## Required Bindings

These are non-negotiable bindings, not soft preferences:

- exact board identity
- legal replay
- owner, defender, anchor, route, and scope binding
- same-root certification binding
- raw engine and source context non-ownership
- public-safe line evidence
- stale evidence rejection
- forbidden shortcut rejection
- renderer non-authority

Weak evidence usually lowers confidence. It becomes a hard failure only when a
story would otherwise make a public chess claim without the required binding.

## Scoring Responsibilities

The chess model scores what is worth saying after required bindings:

- `Proof` computes `truth`, `tacticHeat`, `planHeat`, and `publicStrength`.
- `Story` encodes scene, plan, tactic, identity, and proof slots in exactly
  `160` values.
- `StoryTable` keeps at most `8` verdicts and owns deterministic ordering.
- `Verdict` encodes rank, role, lead permission, strength, and story references
  in exactly `96` values.

## Renderer Boundary

Any future template or LLM renderer must consume only selected structured
intent. It may paraphrase; it may not infer new claims.
