# Chess Model Architecture

This document describes the current pre-render chess model target for this
branch.

## Current Scope

The current checkpoint covers only board truth, primitive board geometry,
Stage 1 board facts, Stage 2 Story Proof, and the public Story boundary. It is
intentionally not a full product commentary backend.

The safe target is to make unproven commentary impossible before opening any
positive public claim. Broad strategy, plan labels, conversion claims, and
renderer openings remain downstream of proof-backed Stories.

The current branch owns only:

`Board Truth / Primitive Geometry / Story boundary / Verdict boundary`

Current implementation scope is Stage 3 first narrow positive Story only.
Stage 3 is open only for Material proof kernel, `Tactic.Hanging`, and Hanging
negative corpus. Stages 4-11 below are a dependency map for product design;
they are not permission to implement those systems in this checkpoint.

Product north-star philosophy:

- Engine is the truth oracle.
- Backend is the proof and pedagogy system.
- LLM is the narrator.

The LLM does not judge chess. Engine truth, exact-board validation, legal
replay, proof sidecars, and `StoryTable` decide what can be said. Backend
policy owns proof, Story selection, arbitration, and pedagogy. The LLM may
phrase selected `Verdict` or `Explanation IR` data only; it must not decide,
prove, rank, repair, or invent chess meaning.

## Model Style

The model is an HCE-style deterministic chess scorer. It is not a trained
neural model. Input and output shapes are fixed so decisions can be logged,
audited, and later used for training.

The model replaces legacy selection authority, not renderer authority.

Code names must follow `ChessModelContract.md`: no type or module name may carry
version suffixes or developer-facing abstraction names for the new core model.
Schema numbers and shape sizes live in companion constants.

## Authority Consolidation

The architecture must reduce duplicated meaning, not multiply authority names.
Before adding a new type, module, row, or observation family, first test whether
the chess meaning already has a home and whether the existing owner can carry a
new field. When a file-related observation can live inside `FileFact`, a
piece-contact observation can live inside `PieceContact`, or a line-geometry
observation can live inside `LineFact`, prefer extending the existing owner over creating a second authority name.

Small implementation steps are good. Similar row families that each own a slice
of the same chess phenomenon are not. If a later `Story` proof would have to
choose between two similarly named inputs, the design has already split
authority and must be consolidated before proceeding.

## Live Authority Chain

The current chess authority path is:

`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`

`BoardMood`, `Story`, `StoryTable`, and `Verdict` shapes, names, family counts,
and deletion rules are fixed by `ChessModelContract.md`.

Long term, the product backend may grow into this ordered structure:

`Board Truth` -> `Engine Truth` -> `Primitive Geometry` -> `Tactical/Strategic Story Birth` -> `Engine Validation` -> `Causal Arbitration` -> `Pedagogical Policy` -> `Explanation IR` -> `LLM Narration` -> `Verifier`

That product structure does not open the current public surface. In this branch,
`BoardMood` supplies board facts and primitive geometry, `Story` is the first
public-meaning unit, `StoryTable` decides roles and lead eligibility, and
`Verdict` is the only language-neutral output visible downstream.

## Stage Opening Order

Stages open in this order only. Do not implement downstream product stages
before earlier authority stages are proven.

Only Stage 3 first narrow positive Story is active implementation authority
now, and only for `Tactic.Hanging`. Stages 4-11 are dependency-map-only and
stay closed until their predecessor exits are proven.

Dependency order:

- Stage 1 depends on Stage 0.
- Stage 2 depends on Stage 1.
- Stage 3 depends on Stage 2.
- Stage 4 depends on Stage 3.
- Stage 5 depends on Stage 4.
- Stage 6 depends on Stage 5.
- Stage 7 depends on Stage 6.
- Stage 8 depends on Stage 7.
- Stage 9 depends on Stage 8.
- Stage 10 depends on Stage 9.
- Stage 11 depends on Stage 10.

| stage | opens | prerequisite proof | forbidden shortcut | exit criteria | proof-deficit coordinate |
|---|---|---|---|---|---|
| 0 Closed Kernel | Global no-go authority and fail-closed public boundary. | Live docs and tests prove missing Story tuple blocks public output. | Renderer, LLM, public route `200`, broad strategy, broad scalar re-entry, source/engine public truth, high numeric `Proof` lead. | Public chess meaning cannot escape without side, target, anchor, route, rival, required legal line, and same-root proof sidecar. | Missing tuple member or missing no-go authority. |
| 1 Board Facts | Small current-board observations. | Exact board identity, legal replay diagnostics, known/sane BoardFacts. | Plan speech, tactic verdict, conversion, counterplay, initiative, score/heat/pressure public meaning, engine eval as truth. | `BoardMood` creates no public chess meaning. | Missing board identity, producer proof, or observation identity. |
| 2 Story Proof | Code-enforced proof-bearing Story identity. | Stage 1 observations plus side, target, anchor, route, rival, required legal line, same-root proof. | Numeric proof score as public authority. | No Story Proof or incomplete Story Proof yields blocked/context only. | Missing sidecar field, stale sidecar, or wrong root. |
| 3 First Narrow Positive Story | Exactly one positive Story family at a time, with Material proof kernel as sub-proof only. | Complete Stage 2 sidecar, same-root legal replay, named writer, and family-specific proof. | Complete StoryProof opening a family, family piggybacking, renderer, LLM, public route `200`, or broad strategic family before tactical/material proof. | One narrow Hanging Story can speak with proof; close false positives stay silent. | Missing target, attack, legal capture, defender, recapture, positive bounded material result or SEE, same-board proof, named writer, or legal replay. |
| 4 Engine Validation | Engine evidence attached to identified Story. | A Story candidate already exists with identity and legal line. | Engine eval/PV/best move as explanation, strategy, or pedagogy by itself. | Engine confirms, caps, or refutes the Story without speaking alone. | Missing same-root replay, SEE, line stability, MultiPV, mate/tablebase proof, or eval stability. |
| 5 Causal Arbitration | Lead/support/context/blocked roles across Stories. | Multiple proof-backed Stories and interaction laws. | Pedagogical advice before causal ordering. | `StoryTable` deterministically decides roles under tactic, blunder, source, counterplay, and quiet rules. | Missing override, cap, blocker, role, or rival-resource proof. |
| 6 Explanation IR | Renderer-safe language-neutral explanation payload. | Selected Verdicts with allowed claims and evidence. | Raw `BoardMood`, root atom, source row, or engine eval read by IR. | IR lists allowed claim, evidence, strength, role, support/context relation, and forbidden wording. | Missing allowed claim, evidence line, strength, role, relationship, or forbidden wording. |
| 7 Deterministic Renderer | Template baseline over Explanation IR. | Explanation IR is complete and renderer-safe. | Natural language that exceeds IR or repairs missing proof. | Deterministic text is no stronger than IR. | Missing template boundary, unsupported phrase, or unrepresented claim. |
| 8 LLM Narration | Wording, tone, level adjustment, compression. | Deterministic renderer baseline and safe IR. | New move, tactic, plan, causal explanation, evaluation, line, or claim strength. | LLM output verbalizes only allowed claims. | Unsupported chess fact, strengthened wording, or invented line. |
| 9 Natural-Language Verifier | Claim extraction and rejection over rendered text. | IR and rendered text are available for comparison. | Accepting stronger natural language than IR. | Verifier rejects mate/win/only-move/tactic/plan/source claims without matching proof. | Extracted claim exceeds allowed claim, evidence, role, or strength. |
| 10 Pedagogical Policy | Level-aware teaching choices. | Stable causal arbitration and verified narration. | Pedagogy before causal truth. | Same Story can be explained differently by user level without changing chess truth. | Missing user level, learning objective, or allowed teaching transform. |
| 11 Personal Learning Loop | Motif memory, spaced repetition, custom practice, progress. | Stable Story taxonomy and pedagogical policy. | Personalization before reliable Story classification. | Repeated Story patterns can drive training without changing proof rules. | Missing motif identity, history window, review cadence, or progress evidence. |

### Stage 0 - Closed Kernel

Goal: prove that no public chess meaning escapes without required Story
bindings.

Required:

- public routes remain tombstones
- renderer remains closed
- `BoardMood` cut and split slots remain `0`/silent unless admitted by live law
- numeric `Proof` scores cannot create `Role.Lead`
- selected `Verdict` data cannot be rendered publicly

Completion standard: tests show that missing side, target, anchor, route,
rival, required legal line, or same-root proof sidecar blocks public output.

### Stage 1 - Board Facts

Goal: populate only small current-board observations.

Core sentence: Board state observes. Story proves.

Allowed facts are side, square, file, rank, piece, pawn, legal move, attack,
guard, `PieceContact`, `FileFact`, `LineFact`, defender, blocker, line, ray,
x-ray shape, pin-to-king geometry, open and semi-open file state, rook file
entry observation, unguarded non-pawn non-king piece observation, pawn lever,
pawn challenge, front blocker, current pawn structure observation, reachable
square, square guard map, king square, king-ring square, king-ring attack,
king-ring defender, legal king move, contact-check observation, non-public
king-line geometry, and promotion path.

Forbidden public meanings are plan speech, tactic verdict, conversion speech,
counterplay speech, initiative speech, score/heat/pressure speech, and engine
eval as truth. `BoardMood` facts are observations, not claims.

### Stage 2 - Story Proof

Goal: create the first real proof sidecar required for public Story birth.

A Story cannot lead unless it has side, target, anchor, route, rival, required
legal line, and same-root proof sidecar. A high numeric `Proof` score without
that sidecar remains blocked/context only.

Core sentence: Board Facts observes. Story Proof binds. Story may speak only
after proof.

### Stage 3 - First Narrow Positive Story

Goal: open exactly one narrow proof-backed Story family.

`StoryInteractionLaw.md` owns the Stage 3 charter. Architecture records the
dependency boundary: Stage 3 consumes Stage 2 StoryProof, adds backend Material
proof evidence, and opens only the named `Tactic.Hanging` writer. Stage 4
Engine Validation, renderer, LLM, public route `200`, and every other positive
Story family remain closed.

The recommended implementation order is Material proof kernel,
`Tactic.Hanging`, Hanging negative corpus, `Tactic.Fork`, Fork negative corpus,
`Scene.Material`, then `Scene.Defense`. The first implementation scope is only
Material proof kernel, `Tactic.Hanging`, and Hanging negative corpus.

Material proof kernel is sub-proof, not public claim. It may compute legal
capture, captured piece, capturing piece, recapture map, material after one ply
or a bounded line, and SEE-like exchange result.

`Tactic.Hanging` may speak only when target piece exists, target is attacked,
legal capture exists, target has no adequate defender or recapture, bounded
material result is positive, StoryProof is complete, same-board proof is
present, and the `Tactic.Hanging` writer is explicitly open.

Stage 3 first success means one narrow Hanging Story can speak with proof, and
all close false positives still stay silent.

The preferred first families after that scope are `Tactic.Hanging`,
`Tactic.Fork`, `Scene.Material`, and `Scene.Defense`, in that order. Broad
strategic families remain closed at this stage.

### Stage 4 - Engine Validation

Goal: attach engine evidence only as proof for an already-identified Story.

Engine data may confirm, cap, or refute a Story. It may not speak by itself.
Engine evidence may include legal replay, SEE or bounded material result,
engine line, MultiPV comparison, mate proof, tablebase proof, or eval
stability. Raw engine eval is not public meaning.

Engine lines, mate/tablebase proof, SEE, and bounded material results are
truth-oracle evidence for backend proof. Raw engine numbers and engine text are
never public claim owners.

### Stage 5 - Causal Arbitration

Goal: allow `StoryTable` to decide which proof-backed Stories lead, support,
block, or stay context.

Required interactions: tactic overrides plan, blunder suppresses strategic
praise, source/opening cannot lead over board-backed Story, high counterplay
risk caps conversion/plan lead, and quiet leads only when no non-quiet Story
reaches public floor.

### Stage 6 - Explanation IR

Goal: convert selected Verdicts into a renderer-safe language-neutral
explanation payload.

Explanation IR may include allowed claim, allowed evidence line, allowed
strength, forbidden wording, user-facing role, and support/context
relationship. It must not read raw `BoardMood`, root atoms, source rows, or
engine eval directly.

Pedagogy is backend policy over selected proof-backed `Verdict` data.
Explanation IR may carry backend-owned instructional emphasis only after causal
arbitration. Renderer and LLM layers may not choose that emphasis or turn
observations into advice.

### Stage 7 - Deterministic Renderer

Goal: provide a template baseline over Explanation IR before LLM narration.

The deterministic renderer may verbalize selected Verdict or Explanation IR
only. It must not create chess meaning, repair missing proof, upgrade claim
strength, infer new tactics, infer new plans, or reinterpret source or engine
context as truth. Its text must be no stronger than the IR.

### Stage 8 - LLM Narration

Goal: adjust wording, tone, level, compression, and example phrasing over
Explanation IR and deterministic renderer baseline.

LLM narration may not add a new move, tactic, plan, causal explanation,
evaluation, line, or claim strength. LLM before Explanation IR is forbidden.

### Stage 9 - Natural-Language Verifier

Goal: check that rendered text does not exceed Verdict or Explanation IR.

The verifier rejects forced mate without mate proof, winning claims without
conversion proof, only-move claims without benchmark proof, tactic claims
without legal line, plan claims without route/anchor/proof, and source/opening
claims that override board truth.

### Stage 10 - Pedagogical Policy

Goal: choose level-aware teaching emphasis over proof-backed, causally
arbitrated Verdicts.

Pedagogical policy depends on causal arbitration and verifier-safe language. It
may adapt explanation level, but it must not change chess truth or promote
context/observation rows into advice.

### Stage 11 - Personal Learning Loop

Goal: use stable Story taxonomy for motif memory, spaced repetition, custom
practice, next-game focus, and progress tracking.

Personalization depends on stable Story taxonomy and pedagogical policy. It
must not alter proof requirements or treat repeated observations as public
claims.

## Closed Boundaries

The architecture target does not open the public path. Public surface behavior,
renderer behavior, `BoardMood` Sxxx expansion/re-entry, and `Story` proof
writers stay closed until prerequisite laws and docs tests exist.

`/api/commentary/render` and `/internal/commentary/render-local-probe` are
registered only as fail-closed tombstones. No runtime FEN, environment switch,
frontend mock, or renderer payload opens those routes.

At this checkpoint, `StoryTable` lead decisions are closed. Numeric `Proof`
scores may rank blocked/context `Verdict` rows only; they cannot set
`leadAllowed=true` or produce `Role.Lead` until the full side, target, anchor,
route, rival, required legal line, and same-root proof sidecar tuple exists.

## BoardMood

`BoardMood` is the shared middle board representation:

- `48` bit slots
- `256` scalar slots
- `3,328` total values

B00..B45 are packed root transport words, B46..B47 are legal-destination
summaries, and S000..S255 are named scalar slots. Missing producer inputs are
zero-filled only through the explicit slot contract; there is no unnamed
expansion region in the live `BoardMood` contract.

`BoardMood` must remain an observation layer. It must not create broad scalar
claims for pressure, conversion, counterplay, strategy, or plan labels. Split
or cut ideas may re-enter only as smaller current-board observations admitted
by a live law; they must not be revived under old slot names as public
authority.

## Root Transport

`BoardFacts.fromFen` is the strict runtime entrypoint into `BoardMood`. It uses
the strict `RootExtractor.fromFenWithPositionFailClosed` helper, derives legal,
material, control, and pawn facts from the same exact board, and returns
`Either[String, BoardFacts]`.

`BoardFacts.fromPosition` is internal/test-only. It requires an explicit
positive fullmove number from the internal caller, may support parity checks,
and is not an external escape hatch around strict raw-FEN validation.

B46/B47 and `Moves.lines` remain diagnostic summaries. They do not prove
origin, route, castling, en-passant, promotion, tactic, line, or public claim
legality.

## Story

`Story` is the public chess unit. It carries:

- one public `Scene`
- optional `Plan`
- optional `Tactic`
- compact identity: `side`, `target`, `anchor`, `route`, and `rival`
- `Proof` scores using the exact names from `ChessModelContract.md`

Story owns identity. StoryProof owns proof and missing evidence. Verdict
carries the result. `StoryProof` checks whether the identity already carried by
`Story` is bound by legal line and same-board proof; it must not carry a second
copy of side, target, anchor, route, or rival.

`proofFailures` is internal diagnostic data only. It may explain why the kernel
cannot speak during tests or debugging, but it is not public payload, renderer
input, or LLM input.

`Story.values` is exactly `160` values. It one-hot encodes public families and
stores proof scores in the proof segment.

The current implementation target is the first narrow positive Story, not
renderer opening. A Story cannot lead until it has concrete side, target,
anchor, route, rival, required legal line, same-root proof sidecar, a named
positive Story writer, and family-specific proof. Numeric proof scores alone
are not authority because they can be forged without the bound sidecar tuple.

## StoryTable

`StoryTable` orders stories into at most `8` verdicts. Lead permission depends
on public strength, truth, counterplay risk, quiet/source restrictions, and
owner-aware tactical interaction.

A plan lead is blocked only by an opposing `Tactic` or `Blunder` story at the
public floor. Source stories cannot lead over a non-source board-backed story at
the public floor. Opening stories are context-only and cannot lead over a
board-backed story at the public floor.

## Verdict

`Verdict` is the language-neutral result handed to downstream display work. It
stores story reference, rank, role, lead permission, and strength.

`Verdict.values` is exactly `96` values.
