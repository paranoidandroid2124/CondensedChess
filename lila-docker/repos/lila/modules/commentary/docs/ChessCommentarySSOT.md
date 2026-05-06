# Chess Commentary SSOT

This branch rebuilds commentary around a pre-render deterministic chess model.
The live backend authority chain is:

`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`

No other path owns current public chess meaning.

This branch is not yet a product-level commentary backend. Its current job is
the proof-first Story kernel: prevent unproven chess meaning from reaching the
public surface. Good commentary is deferred until the public boundary is closed
against false claims.

Product north-star philosophy:

- Engine is the truth oracle.
- Backend is the proof and pedagogy system.
- LLM is the narrator.

The LLM does not judge chess. Engine truth, exact-board validation, legal
replay, proof sidecars, and `StoryTable` decide what can be said. LLM phrasing
comes only after a safe downstream payload exists.

Engine lines, mate/tablebase proof, SEE, and bounded material results are
truth-oracle evidence for backend proof. Raw engine numbers and engine text are
never public claim owners. Backend policy owns proof, Story selection,
arbitration, and pedagogy. The LLM may phrase selected `Verdict` or
`Explanation IR` data only; it must not decide, prove, rank, repair, or invent
chess meaning.

## Core Decision

`BoardMood` gathers shared board facts. `Story` is the only chess unit allowed
to compete for commentary. `StoryTable` applies deterministic interaction and
ordering rules. `Verdict` is the language-neutral result consumed downstream.

The renderer is explicitly downstream. It may express selected structured
intent, but it must not create chess meaning, repair missing evidence, upgrade
wording strength, or turn source and engine context into truth.

The branch invariant is:

`BoardMood` observes.
`Story` proves.
`StoryTable` arbitrates.
`Verdict` speaks.
Renderer only phrases.

`BoardMood` observations such as weak squares, open files, attacked pieces,
legal pawn levers, and king-ring attack maps are not public claims. For
example, a rook/open-file/entry-square observation does not prove that a rook
controls or can use the open file. Public meaning begins only at `Story`.

The proof boundary is:

- feature is not a claim
- claim is not a public `Story`
- public `Story` requires proof-bearing identity

The authority-consolidation boundary is:

- One chess meaning, one home.
- One observation family, one owner.
- One public claim, one proof path.

Too many small modules and duplicated roles caused authority explosion in the
old commentary stack. Splitting work into small implementation steps is still
allowed, but splitting one chess meaning across several similarly named types,
modules, rows, or docs authority paths is not allowed. A new type, module, row, or docs-authority name is the last resort.

Before adding a new name, ask:

- is this really a different chess meaning from an existing one?
- can the existing Fact owner carry this as a field?
- does this name create a new authority by sounding like one?
- will two names now own the same board phenomenon?
- will a later `Story` proof know exactly which input to trust?

The implementation question is not "which tactic or strategy feature is
visible?" The implementation question is whether that feature can become a
Story with side, target, anchor, route, rival, required legal line, and
same-root proof. If the answer is missing, the system must stay silent or keep
the row blocked/context-only.

Implementation must open in this order only:

`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `Explanation IR` -> Renderer

The current branch owns the early kernel: board truth, primitive geometry,
Story boundary, and Verdict boundary. Downstream product stages stay closed
until earlier authority stages are proven.

Current implementation scope is Stage 1 Board Facts only. Stages 2-11 are a
dependency map, not permission to open those systems.

## Current No-Go State

The current checkpoint is closed at the public boundary. Stage 1 `Board Facts`
may organize small current-board observations under `BoardFacts.md`, but there
is no public surface opening, no broad `BoardMood` Sxxx expansion or re-entry,
no `Story` proof writer opening, and no renderer opening until prerequisite
laws and tests exist.

`/api/commentary/render` and `/internal/commentary/render-local-probe` are
registered only as fail-closed tombstones. No `200`, rendered payload,
environment switch, or frontend mock can open them.

`BoardMood` remains fixed at `48` bits, `256` scalars, and `3,328` total values.
Split/cut re-entry requires a named law and same-board producer proof.

Open file, pin, weak square, loose piece, pawn lever, attacked piece,
king-ring attack, and legal move facts remain observations only. They do not
become public claims until `Story` supplies side, target, anchor, route, rival,
required legal line, and same-root proof sidecar.

Known blockers are authority blockers, not implementation permission:

- Numeric `Proof` scores may rank blocked/context `Verdict` rows only. They
  cannot set `leadAllowed=true` or produce `Role.Lead` until same-root side,
  target, anchor, route, rival, required legal line, and proof sidecars exist.
- Missing side, target, anchor, route, rival, required legal line, or same-root
  proof sidecar is a hard public-output block, not weak scoring or renderer
  repair.
- Runtime proof-sidecar writers for that full tuple do not exist, so public
  output remains closed until the tuple is enforced.
- `Scene.Opening` is context-only and must not lead over a board-backed
  `Story`.
- Old failing tests proved lower/scaffold/renderer non-upgrade, not default
  runtime FEN to public `Verdict`.

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
may feed `BoardMood` or same-root Story sidecars only when a live authority
document admits that path; they do not supply proof authority by name and do not
bypass `StoryTable`.

Root atoms, source rows, engine evals, and support-only carriers must not be
shown directly to renderers or LLM phrasing layers. They may become evidence
only after a `Story` binds the required identity and proof tuple.

Forbidden shortcut patterns:

- feature to public claim
- root atom to public claim
- `BoardMood` scalar to public claim
- source row to public truth
- raw engine eval to public truth
- high numeric `Proof` score to `Role.Lead` without sidecar
- renderer repair of missing chess evidence
- LLM inference of chess meaning
- broad strategic `Story` before narrow tactical/material `Story`
- family-specific shortcut growth before common proof coordinates exist

If a change makes commentary richer but weakens proof ownership, it is rejected.

Dependency shortcuts are forbidden:

- Renderer before Story proof sidecar is forbidden.
- LLM before Explanation IR is forbidden.
- Strategy before tactical/material proof is forbidden.
- Pedagogy before causal arbitration is forbidden.
- Personalization before stable Story taxonomy is forbidden.

## Required Bindings

These are non-negotiable bindings, not soft preferences:

- exact board identity
- legal replay
- owner, defender, anchor, route, and scope binding
- same-root proof sidecar binding
- raw engine and source context non-ownership
- public-safe line evidence
- stale evidence rejection
- forbidden shortcut rejection
- renderer non-authority

The public binding tuple is fail-closed. Missing side, target, anchor, route,
rival, required legal line, or same-root proof sidecar is a hard public-output
block even if other evidence looks strong.

## Scoring Responsibilities

The chess model scores what is worth saying after required bindings:

- `Proof` computes `truth`, `tacticHeat`, `planHeat`, and `publicStrength`.
- `Story` encodes scene, plan, tactic, identity, and proof slots in exactly
  `160` values.
- `StoryTable` keeps at most `8` verdicts and owns deterministic ordering.
- `Verdict` encodes rank, role, lead permission, strength, and story references
  in exactly `96` values.

## Renderer Boundary

Templates and LLM renderers must consume only selected `Verdict` data. They may
paraphrase; they may not infer new claims.

Pedagogy is backend policy over selected proof-backed `Verdict` data. Renderer
and LLM layers may not choose instructional emphasis, promote context into
lessons, or turn observations into advice.
