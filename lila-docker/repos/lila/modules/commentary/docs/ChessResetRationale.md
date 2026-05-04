# Chess Reset Rationale

This document records why this branch rebuilds chess commentary instead of
patching the old commentary stack.

## Failure We Are Leaving

The old stack repeatedly confused lower-layer success with public commentary readiness. A root atom, source row, tactical tag, engine eval, or opening label
could look locally valid, but the final surface still failed because the public
claim had no owner, anchor, route, legal replay, same-root proof, or source
binding.

That failure mode produced a predictable loop:

- add one more special case
- add one more test around that special case
- add one more module with local authority
- reach the renderer or public surface
- fail closed because the public claim still cannot be proven

This branch stops that loop. A fact being extracted is not the same as a fact
being safe to say.

## Model Lesson

Chess commentary is not a single selector problem. One position usually has
many interacting facts: material, king safety, mobility, pawn structure, tactical
motifs, long-term plans, opening context, engine pressure, and source context.

The model therefore follows a Stockfish HCE-like lesson:

- define chess facts operationally
- gather them into one shared board representation
- let interactions happen over the shared representation
- keep deterministic shapes so every decision can be audited

The model does not copy Stockfish's output. Stockfish reduces features to one
eval scalar. This branch must produce structured commentary candidates. The
output side is therefore `Story`, `StoryTable`, and `Verdict`, not a single
number.

## New Authority Chain

The live authority chain is:

`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`

`BoardMood` is the shared board-facing fact state. It is not public proof by
itself.

`Story` is the only unit that may become public chess meaning.

`StoryTable` decides lead, support, context, blocking, and ordering under
deterministic interaction rules.

`Verdict` is the language-neutral result handed downstream.

Renderer code, templates, and LLM phrasing are downstream. They may express a
selected `Verdict`; they may not create chess meaning, repair missing evidence,
or increase claim strength.

## Non-Negotiable Lessons

- Legal destination masks are not proof of origin, route, castling, en-passant,
  promotion, tactics, or public claim legality.
- A pin motif is not permission to publicly claim a pin story.
- Opening/source context is not public chess truth ownership.
- Engine eval is pressure context, not a replacement for chess proof.
- Missing `known && sane` facts must not be hidden behind default zeroes.
- `BoardMood.fromPieces` is scaffold-only and must not make runtime positions
  ready.
- Proof, binding, source, and line slots must stay zero unless their sidecars
  actually provide same-root evidence.

## Naming Lesson

Names are part of the architecture. Legacy and project-management names pull
the model back toward the old failure mode.

Do not reintroduce new core names such as `Pipeline`, `Selector`, `Semantic`,
candidate-line transport names, `Certification`, `Object`, `Delta`, `Gate`, or
`ScoreVector`.

Use chess-facing names such as `BoardFacts`, `BoardMood`, `Material`, `Pawns`,
`Pieces`, `Heat`, `KingHeat`, `Tactics`, `Story`, `StoryTable`, and `Verdict`.

## Completion Standard

A stage is not complete because a lower layer can extract something. It is
complete only when the next public boundary cannot later discover that required
facts, ownership, route, source, or proof were missing.

Every new producer must answer:

- what exact board state owns this fact
- whether the fact is `known && sane`
- which `BoardMood` slot it fills
- whether it is public proof or only a diagnostic summary
- what must stay zero until a later sidecar exists

If those answers are missing, the correct result is fail closed, not a renderer
patch.
