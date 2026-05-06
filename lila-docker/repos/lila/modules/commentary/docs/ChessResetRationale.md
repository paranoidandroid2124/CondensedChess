# Chess Reset Rationale

This document records why this branch rebuilds chess commentary instead of
patching the old commentary stack.

## Failure We Are Leaving

The old stack repeatedly confused lower-layer success with public commentary readiness. A root atom, source row, tactical tag, engine eval, or opening label
could look locally valid, but the final surface still failed because the public
claim had no side, target, anchor, route, rival, required legal line,
same-root proof sidecar, or source binding.

That failure mode produced a predictable loop:

- add one more special case
- add one more test around that special case
- add one more module with local authority
- reach the renderer or public surface
- fail closed because the public claim still cannot be proven

This branch stops that loop. A fact being extracted is not the same as a fact
being safe to say.

The core lesson is:

- feature is not a claim
- claim is not a public `Story`
- public `Story` requires proof-bearing identity

The old question was: what tactical or strategic feature is visible in this
position? The new question is: can this feature become a Story with side,
target, anchor, route, rival, required legal line, and same-root proof? If the
answer is missing, the correct behavior is silence, observation, or
blocked/context-only output.

## Model Lesson

Chess commentary is not a single-score problem. One position usually has many
interacting facts: material, king safety, mobility, pawn structure, tactical
motifs, long-term plans, opening context, engine pressure, and source context.

The model therefore follows a Stockfish HCE-like lesson:

- define chess facts operationally
- gather them into one shared board representation
- let interactions happen over the shared representation
- keep deterministic shapes so every decision can be audited

The model does not copy Stockfish's output. Stockfish reduces features to one
eval scalar. This branch must produce `Story` rows that compete through
`StoryTable` and end as a deterministic `Verdict`, not a single number.

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

## Current Freeze

This reset keeps the public-surface no-go state while Stage 3 opens only one
positive Story writer: `Tactic.Hanging`. There is still no public surface
opening, no `BoardMood` Sxxx expansion or re-entry, no positive `Story` proof
writer beyond `Tactic.Hanging`, and no renderer opening.

The registered render routes, `/api/commentary/render` and
`/internal/commentary/render-local-probe`, are fail-closed tombstones only. No
`200`, rendered payload, environment switch, or frontend mock opens them.

`BoardMood` does not expand beyond `48` bits, `256` scalars, and `3,328` total
values in this checkpoint. Split/cut re-entry requires a named law and
same-board producer proof.

The blockers are known. Numeric `Proof` scores may rank blocked/context
`Verdict` rows only; they cannot set `leadAllowed=true` or produce `Role.Lead`.
That is why public output remains closed until same-root side, target, anchor,
route, rival, required legal line, and proof sidecars are enforced.
`Scene.Opening` is context-only and must not lead over a board-backed `Story`.

Missing side, target, anchor, route, rival, required legal line, or same-root
proof sidecar is a hard public-output block, not weak scoring or renderer
repair.

Old failing tests showed that lower facts, scaffold paths, and renderers do not
upgrade themselves into public chess meaning. They did not prove default runtime
FEN to public `Verdict`.

## Non-Negotiable Lessons

- Legal destination masks are not proof of origin, route, castling, en-passant,
  promotion, tactics, or public claim legality.
- A pin motif is not permission to publicly claim a pin story.
- A tactic motif, plan affordance, source row, or engine number is not a public
  `Story`.
- Opening/source context is not public chess truth ownership.
- Engine eval is pressure context, not a replacement for chess proof.
- Engine lines, mate/tablebase proof, SEE, and bounded material results are
  truth-oracle evidence for backend proof. Raw engine numbers and engine text
  are never public claim owners.
- The LLM is not the intelligence of commentary; it is a terminal phraser for
  already-proven `Verdict` data.
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
complete only when the next public boundary cannot discover that required
facts, ownership, route, source, or proof were missing.

Every new producer must answer:

- what exact board state owns this fact
- whether the fact is `known && sane`
- which `BoardMood` slot it fills
- whether it is public proof or only a diagnostic summary
- which closed values must stay zero, and which named law can open them

If those answers are missing, the correct result is fail closed, not a renderer
patch.
