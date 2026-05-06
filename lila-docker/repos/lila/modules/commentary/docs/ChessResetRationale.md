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
positive Story writer: `Tactic.Hanging`, Stage 4 opens only Engine Check
evidence for that writer, Stage 5-1 opens only Hanging role ordering, and
Stage 5-2 fixes deterministic ordering for those rows, and Stage 5-3 tightens
close Hanging blockers without opening broader conflict systems, and Stage 5-4
keeps Verdict diagnostics out of public numeric values and public surfaces.
Stage 5 closeout keeps the handoff limited to selected Verdict data. Stage 6-0
opens only the Explanation Plan charter and selected-Verdict speech boundary.
Stage 6-1 opens only the Explanation Plan shape for one selected
`Tactic.Hanging` Lead Verdict. `allowedClaim` stays a structured key such as
`can_win_piece`; the first shape carries `bounded` strength and forbidden
wording, not public prose. Stage 6-2 opens only `Tactic.Hanging` allowed claim
mapping. Uncapped Lead Verdict only may carry an allowed claim key. Support,
Context, Blocked, and engine-capped Verdicts do not create standalone public
claims; `engineStrengthLimited` suppresses claim keys and strengthens
forbidden wording. Stage 6-3 opens only forbidden wording boundary.
Explanation Plan must carry the default forbidden wording set.
`Tactic.Hanging` remains bounded material tactic wording only, and
`engineStrengthLimited` strengthens forbidden wording without carrying a claim.
Stage 6-4 opens only Support and Context relation structure. Uncapped Lead
only carries an allowed claim. Support and Context create no standalone public
claim. Blocked remains debug-only relation structure, and proofFailures do not
feed relation wording.
Stage 6-5 opens only the selected Verdict input guard. Explanation Plan accepts
selected Verdict only. Raw BoardFacts, BoardMood, root atoms, CaptureResult,
EngineCheck, EngineEval, EngineLine, raw PV, proofFailures text, unselected
Story, unselected Verdict, and source rows remain forbidden inputs.
Stage 6 closeout confirms Explanation Plan only. Blocked, Support, Context,
engine-capped, and engine-refuted Verdicts create no allowed claim or public
claim. Stage 7 deterministic renderer may receive Explanation Plan only and
must not read raw Verdict, EngineCheck, CaptureResult, Board Facts, BoardMood,
raw PV, proofFailures text, source rows, or raw engine evidence directly.
There is still no public surface opening, no
`BoardMood` Sxxx expansion or re-entry, no positive `Story` proof writer beyond
`Tactic.Hanging`, no engine PV commentary, no best-move explanation, no
deterministic renderer, no LLM narration, no user-facing prose, no pedagogy,
and no engine explanation.

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
Engine Check evidence can check only an existing `Tactic.Hanging` Story after
same-board and freshness evidence exists. It cannot create a Story, rank a
Story, write a `Verdict`, feed a renderer, feed an LLM, or become public truth.
Stage 4-2 tightens that guard: different-FEN engine lines, route-mismatched
engine lines, stale engine data, depth-missing engine data, eval-only input
without a Story, and PV-only input without a Story are diagnostic only.
Stage 4-3 attaches EngineCheck only to `Tactic.Hanging`; `Supports` creates no
winning, best-move, decisive, PV-explanation, or public-eval claim, `Caps`
forbids strong expression, and `Refutes` blocks Hanging.
Stage 5 role ordering can choose among existing `Tactic.Hanging` Story rows,
but it cannot create a Story, open a new positive family, or turn
engine eval, Board Facts, or `CaptureResult` into public material meaning.
Support and Context roles remain non-sentence structure, and unknown engine
checks create no engine claim.
Raw engine eval, raw PV text, proofFailures text, Board Facts row count,
`CaptureResult` text, renderer wording, and input order remain outside
StoryTable ordering authority.
Stage 6-0 receives selected Verdict data only. Raw Board Facts, raw BoardMood,
root atoms, `CaptureResult`, `EngineCheck`, `EngineEval`, `EngineLine`, raw PV,
proofFailures text, source rows, renderer wording, and LLM wording remain
outside Explanation Plan input authority.
Stage 6-1 can shape only one selected Hanging Lead Verdict into role, scene,
tactic, side, target, anchor, route, allowedClaim, evidenceLine, strength,
forbiddenWording, and empty supportContextLinks. Full sentences, user-facing
prose, engine-says wording, best-move wording, winning wording, decisive
wording, and public eval remain closed.
Stage 6-2 can map only an uncapped Hanging Lead Verdict to a safe structured
claim key. Support, Context, Blocked, and engine-capped Verdicts create no
standalone public claim, and `engineStrengthLimited` suppresses claim keys
while tightening forbidden wording.
Stage 6-3 can define only forbidden wording for Explanation Plan. It does not
open renderer or LLM speech. `Tactic.Hanging` remains bounded material tactic
wording only.
Stage 6-4 can define only relation structure for Support, Context, capped, and
engine-refuted selected Verdict rows. proofFailures text remains outside
Explanation Plan wording and relation text.
Stage 6-5 can enforce only selected Verdict input. Explanation Plan does not
read raw proof material directly and creates no chess meaning beyond the
selected Verdict.
Stage 6 closeout confirms Explanation Plan only. Stage 7 deterministic
renderer may receive Explanation Plan only, not raw Verdict, EngineCheck,
CaptureResult, Board Facts, BoardMood, raw PV, proofFailures text, source
rows, or raw engine evidence.

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
