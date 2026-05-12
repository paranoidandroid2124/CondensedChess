# Story Interaction Law

Prepared on 2026-05-05 for the public layer above BoardMood.

Stage 2 name is `Story Proof`.

Core sentence: Board Facts observes. Story Proof binds. Story may speak only
after proof.

Stage 3 core sentence: StoryProof is necessary. A named Story writer gives
permission. Family proof gives the reason.

Ownership split:

- Story owns identity.
- StoryProof owns proof and missing evidence.
- Verdict carries the result.

StoryProof may inspect Story identity, but it must not own or duplicate side,
target, secondary target, anchor, route, or rival. Legal line and same-board
proof are evidence for the Story identity, not a second identity home.

Legal line binding is not tactical success proof. In Stage 2 it proves only
that the Story route is tied to a same-board legal path.

Stage 2 StoryProof proves only the minimum form of evidence. Material, tactic,
plan, king, source, and opening Stories remain blocked or context-only until
Stage 3 explicitly opens the first positive Story family through a named Story
writer and family-specific proof.

proofFailures are internal diagnostics only. They exist for tests, debugging,
and locating the next missing proof coordinate. They are not public payload,
renderer input, or LLM input, and missing evidence text must not become user
commentary.

## Law

`Story` is the first public chess unit, but a Story family name is not enough.
Every public Story must bind side, target, anchor, route, rival, required legal
line, and same-root proof sidecar. Multi-target Stories such as Fork must also
bind a secondary target in Story identity. A family row below adds the
chess-specific facts that must support that binding. Missing side, target,
anchor, route, rival, required legal line, same-root proof sidecar, or required
secondary target is a hard public-output block. If a row does not need a move
sequence, its required legal line is the same-root legal replay proving the
named position fact can be spoken.

Nonlinear interaction is explicit. A support fact can help a Story only when no
hard blocker applies. A blocker can cap or silence a Story even when several
support facts are present. A stronger tactical or blunder Story can override a
strategic Story. Engine context can confirm, cap, or contradict a Story, but it
does not speak without the Story identity tuple.

## Stage 3 Charter

Stage 3 opens exactly one positive Story family at a time. In the first scope,
Stage 3 opens exactly one narrow proof-backed Story family. Complete StoryProof
is necessary but not sufficient. A complete StoryProof does not open a family.
A positive Story requires a named Story writer and family-specific proof. No
other Story family may piggyback on the first family.

Renderer, LLM narration, public route `200`, `/api/commentary/render`, and
`/internal/commentary/render-local-probe` remain closed. The Stage 3 charter
opens backend proof and writer permission only; it does not open public
transport, template rendering, frontend mocks, or LLM phrasing.

Stage 4 Engine Check opens only after the Stage 3 charter and only as internal
evidence for existing Stories. Renderer and LLM remain closed after the Stage 3
charter.

Opening Tactic.Hanging does not open Fork, Material, Defense, Plan, Strategy,
renderer, LLM, or public route. The current Fork vertical slice opens only the
named `Tactic.Fork` backend proof and writer path described below. Material-3
opens only the narrow named `Scene.Material` writer described below.

The following remain forbidden: renderer opening outside named
ExplanationPlan-only templates, LLM narration, public route `200`, Board Facts
direct public claim, Proof score alone as Lead, StoryProof alone as Lead, and
positive Story families other than `Tactic.Hanging`, the narrow
`Tactic.Fork` vertical slice, and the narrow `Scene.Material` writer.

Opening `Tactic.Hanging` did not open `Scene.Material`, `Tactic.Fork`,
`Scene.Defense`, `Scene.Plan`, or any Plan row. The current Fork slice opens
`Tactic.Fork` only through MultiTargetProof and the named Fork writer. Opening
`Scene.Material` does not open `Scene.Blunder`, `Scene.Convert`, winning claims,
conversion claims, or decisive-advantage wording.

Board Facts direct public claim, Proof score alone as Lead, StoryProof alone as
Lead, and positive Story families other than `Tactic.Hanging` were forbidden in
the first Stage 3 scope. The current Fork slice opens only
`MultiTargetProof`, the named `Tactic.Fork` writer, EngineCheck attachment,
StoryTable ordering, negative corpus checks, ExplanationPlan mapping, and
Fork-8 deterministic renderer text from ExplanationPlan. The current Material
slice opens only the narrow proof-backed `Scene.Material` vertical slice
through Material-9 LLM smoke.

proofFailures remain internal diagnostics only. They may locate missing proof
coordinates for tests and debugging, but they are not public JSON, renderer
input, LLM input, or text that may be spoken.

The Stage 3 implementation order is:

1. Material proof kernel
2. `Tactic.Hanging`
3. Hanging negative corpus
4. `Tactic.Fork`
5. Fork negative corpus
6. `Scene.Material`
7. `Scene.Defense`

The first implementation scope is only steps 1-3: create the material proof
kernel, open one narrow `Tactic.Hanging` writer on top of that kernel, and prove
near false positives still stay silent.

Material proof kernel is not a public Story family. `CaptureResult` is internal
evidence only. It may compute side, capturing piece, target piece, legal
capture line, captured value, recapture candidates, material result after one
immediate legal reply or a bounded line, same-board proof, and missing evidence. It must not say
wins material, free piece, hanging, best move, blunder, decisive, winning, or
tactic works. A capture line can show a material result; a recapture can cancel
that result; unclear recapture or equality means no positive material proof.
Failed `CaptureResult` rows leave missing evidence and stay silent.

`Tactic.Hanging` is the first positive Story family. It may speak only when all
of these are present:

1. target piece exists
2. target is attacked
3. legal capture exists
4. target has no adequate defender or recapture
5. bounded material result is positive
6. StoryProof is complete
7. same-board proof is present
8. `Tactic.Hanging` writer is explicitly open

`Tactic.Hanging` must not use free piece, blunder, winning, decisive, forced,
king unsafe, file control, plan, counterplay, strategy, or conversion wording.

Stage 3 first success means one narrow Hanging Story can speak with proof, and
all close false positives still stay silent.

The preferred family opening order after the first scope is:

1. `Tactic.Hanging`
2. `Tactic.Fork`
3. `Scene.Material`
4. `Scene.Defense`

`Plan.Convert`, `Plan.Prophy`, `Plan.Initiative`, `Plan.ColorBind`,
`Plan.WeakSquare`, and route-only strategic claims remain closed until their
proof sidecars and line validation are live.

For `Tactic.Hanging`, an attacked-piece guard map is only an observation. A
public hanging-piece Story requires the attacked piece identity, legal capture,
defender and recapture map, positive bounded material result or SEE,
same-board proof, same-root legal replay, complete StoryProof, and a
forbidden-wording boundary. Without that tuple it must degrade to observation,
context, or blocked Story; it must not say that a piece is hanging or that a
move wins material.

## Stage 4 Charter

Stage 4 name is `Engine Check`.

Story comes first. Engine checks, caps, or refutes. Engine never speaks alone.

Stage 4-1 opens only the internal engine evidence shape. The open evidence
names are `EngineCheck`, `EngineLine`, and `EngineEval`. Their job is to record
same-board proof, checked move, engine line, reply line, eval before, eval
after, depth or freshness, and missing evidence.

Engine eval, engine line, reply line, and checked move data cannot create a Story. They cannot open `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, Plan, Strategy, renderer, LLM, public route `200`, engine PV commentary, or best-move explanation.

An engine line may check only an existing Story identity and legal route.
Missing same-board proof, checked move, engine line, reply line, eval before,
eval after, depth or freshness, or fresh engine evidence is internal missing
evidence. It is not public payload, renderer input, LLM input, or text that may
be spoken.

Stage 4-1 does not wire StoryTable consumption. `Tactic.Hanging` remains the only positive Story writer. Renderer, LLM, and public route `200` remain closed.

Stage 4-2 adds same-board and stale engine guards.

Engine evidence must bind to the same board, the same Story route, and the same legal line.

Different-FEN engine lines, route-mismatched engine lines, stale engine data, depth-missing engine data, eval-only input without a Story, and PV-only input without a Story are diagnostic only.

The guard result may confirm that an `EngineCheck` sidecar is usable for an existing Story. It still does not create a Story, does not choose a Story, does not write a `Verdict`, and does not open renderer, LLM, or public route `200`.

Stage 4-3 attaches EngineCheck only to `Tactic.Hanging`.

EngineCheck status starts with exactly `Unknown`, `Supports`, `Caps`, and `Refutes`.

- `Unknown`: engine evidence is absent or unusable. Engine-related expression is forbidden.
- `Supports`: engine evidence does not contradict the current Hanging Story. `Supports` does not mean winning, best move, decisive, PV explanation, or public truth.
- `Caps`: the Hanging Story may remain available, but strong expression is forbidden.
- `Refutes`: `Refutes` blocks the Hanging Story.

`Caps` leaves the Story available but forbids strong expression.
`Supports` does not create a new claim. Engine PV remains evidence only and must not become explanation. Engine eval remains internal evidence only and must not become public truth.

Stage 4 negative corpus covers local material gain that fails to a larger
tactic, refuting engine replies, eval collapse after capture, different-FEN
engine lines, stale engine lines, route-mismatched engine lines, engine data
without `CaptureResult`, engine data without complete StoryProof, and engine
data without a named writer.

A complete same-board Hanging Story may be blocked by `Refutes`. Incomplete,
wrong-board, stale, route-mismatched, writerless, or proofless engine evidence
remains `Unknown` and cannot support.

Stage 4 StoryTable integration is conservative.

EngineCheck never creates a Story in StoryTable. `Refutes` blocks only the
Hanging Lead path. `Caps` records only the internal `engineStrengthLimited`
marker. `Supports` preserves the existing Story without creating a new claim.
`Unknown` leaves no engine-related claim. Renderer and LLM wording remain
closed.

## Stage 4 Closeout

One chess meaning, one home. One rule, one live authority.

Stage 4 ends with `Tactic.Hanging` as the only EngineCheck consumer.
`EngineCheck` is not a Story writer and does not create public truth.
`StoryInteractionLaw.md` owns this closeout; other live docs may summarize scope only.

The closeout state is:

- `Refutes` blocks the Hanging Lead path.
- `Caps` records only an internal strength-limited diagnostic.
- `Supports` preserves an existing Hanging Story without adding a claim.
- `Unknown` carries no engine-related claim.
- Wrong-board, stale, route-mismatched, captureless, proofless, and writerless
  engine evidence cannot support.

Stage 5 Story Order may receive internal EngineCheck diagnostics from selected
Verdict rows. Stage 5 must not open renderer, LLM, public route `200`,
`Tactic.Fork`, `Scene.Material`, `Scene.Defense`, Plan, Strategy, engine PV
commentary, or best-move explanation.

## Stage 5 Charter

Stage 5 name is `Story Order`.

Stage 5 may be described as `StoryTable Arbitration` when a document needs to
explain the role of `StoryTable`.

Core sentence: StoryTable orders. It does not invent.

Many Stories may exist. StoryTable chooses roles. No new chess meaning is
created.

Stage 5-0 fixes this charter before broader arbitration work. Stage 5 first
scope is limited to the existing `Tactic.Hanging` vertical slice.

Allowed:

- Lead
- Support
- Context
- Blocked
- deterministic ordering
- `Refutes` -> blocked
- `Caps` -> strength-limited diagnostic
- `Supports` -> no new claim
- `Unknown` -> no engine claim

Forbidden:

- new Story creation
- new positive family
- engine eval as ranking truth
- Board Facts as direct public claim
- `CaptureResult` as public material story
- pedagogical advice
- Explanation IR
- renderer
- LLM
- public route
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`
- Plan / Strategy
- engine PV commentary
- best-move explanation

StoryTable may order only existing Story rows. It must not manufacture a row
from EngineCheck, EngineEval, Board Facts, `CaptureResult`, a proof number, or
a missing sidecar diagnostic.

For the first scope, multiple proof-backed `Tactic.Hanging` rows may be
assigned stable roles. One may become Lead. Other Hanging rows may become
Support, Context, or Blocked. A `Refutes` EngineCheck sends the Hanging Story
to Blocked. A `Caps` EngineCheck preserves only internal strength limitation.
`Supports` preserves an existing Hanging Story without adding a claim.
`Unknown` creates no engine claim and forbids engine expression.

Stage 5 completion standard: when multiple Hanging Story rows exist,
StoryTable deterministically decides Lead, Support, Context, and Blocked roles
without creating new chess meaning or a new public claim.

`StoryInteractionLaw.md` is the single live authority for the Stage 5 charter.
Other live documents may summarize Stage 5 scope only.

## Stage 5-1 Hanging Role Rules

Stage 5-1 goal: assign roles for existing `Tactic.Hanging` Story rows.

Hanging role rules:

- complete `Tactic.Hanging` Story, positive `CaptureResult`, and no `Refutes`
  may enter Lead selection.
- exactly one selected Hanging row may be Lead.
- lower-strength complete Hanging may become Support or Context.
- `EngineCheck.Refutes` sends Hanging to Blocked.
- incomplete StoryProof sends Hanging to Blocked.
- missing `CaptureResult` sends Hanging to Blocked.
- no writer sends Hanging to Context or Blocked.
- `EngineCheck.Unknown` creates no engine claim.

Support is not yet a public sentence. Context is not yet a public sentence.
Role assignment does not open renderer or LLM.

## Stage 5-2 Deterministic Ordering

Stage 5-2 goal: when multiple Story rows exist, StoryTable always returns the
same order for the same rows.

Allowed sort input:

- Story role eligibility
- publicStrength
- scene / tactic identity
- side
- target
- anchor
- route
- writer presence
- blocked status

Forbidden sort input:

- raw engine eval
- raw PV text
- proofFailures text
- Board Facts row count
- `CaptureResult` text
- renderer wording

Input order must not decide Lead. Equal-strength rows must fall through to
target, anchor, and route identity. proofFailures may explain why a row is
Blocked, but proofFailures text must not sort public rows. Raw engine eval and
raw PV text may affect EngineCheck diagnostics only through admitted status;
they must not order Stories by themselves.

## Stage 5-3 Conflict and Block Rules

Stage 5-3 goal: resolve close blocker relationships for Hanging Story rows
only.

Allowed in the current scope:

- `EngineCheck.Refutes` blocks Hanging.
- Missing StoryProof blocks Hanging.
- Missing `CaptureResult` blocks Hanging.
- Missing writer blocks Hanging.
- Quiet only if no positive Hanging exists.
- `Scene.Source` and `Scene.Opening` cannot outrank board-backed Hanging.

Not implemented in Stage 5-3:

- Tactic vs Plan override.
- Blunder override.
- Defense vs Threat relation.
- Counterplay cap beyond existing `EngineCheck.Caps`.
- Strategy suppression.

Stage 5-3 does not create a Story, open a new positive family, create a new
public claim, open renderer or LLM, or turn Source, Opening, Quiet, Board
Facts, `CaptureResult`, raw engine eval, or raw PV text into public truth.

## Stage 5-4 Verdict Diagnostic Boundary

Stage 5-4 goal: keep StoryTable results from being mistaken for renderer or LLM input.

Diagnostic boundary:

- `Verdict.values` shape stays fixed.
- proofFailures do not enter `Verdict.values`.
- EngineCheck diagnostics do not enter `Verdict.values`.
- `engineStrengthLimited` is an internal diagnostic.
- `Verdict` is not public text.
- renderer, LLM, and public route remain closed.

StoryTable may return Verdict diagnostics for tests and debugging. Those
diagnostics do not become renderer input, LLM input, public route payload,
public text, or public chess claims.

## Stage 5 Closeout

Stage 5 closes with Story ordering only.
Explanation IR, renderer, LLM, and pedagogical advice remain closed.
StoryTable creates no chess meaning. It orders existing Stories.
`EngineCheck`, `CaptureResult`, and Board Facts keep their existing homes.
Refuted, incomplete, writerless, captureless, source-only, opening-only, and Quiet fallback rows cannot become Lead over proof-backed Hanging.
No new type, row, or live md authority is introduced by Stage 5 closeout.
Stage 6 handoff receives selected Verdict only.
Stage 6 must not read raw Board Facts, `CaptureResult`, `EngineCheck`, raw engine eval, or raw PV text directly.

Stage 5 closeout does not open Explanation IR, renderer, LLM, public route,
pedagogical advice, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, Plan,
Strategy, engine PV commentary, or best-move explanation.

## Stage 6 Charter

Stage 6 name is `Explanation Plan`.

Documents may write `Explanation IR` parenthetically when describing the
renderer-safe data shape.

Core sentence: Verdict decides. Explanation Plan bounds speech.

Stage 6-0 fixes this charter before renderer or narration work. The goal is not
natural language. The goal is to receive selected Verdict data and organize
claim, evidence, strength, role, support/context relation, and forbidden
wording that downstream stages may phrase only after their own contracts open.

Allowed input:

- selected Verdict
- Verdict role
- Verdict strength
- Verdict story identity
- Verdict scene / tactic
- Verdict Lead, Support, Context, or Blocked state

Forbidden input:

- raw Board Facts
- raw BoardMood
- root atoms
- `CaptureResult`
- `EngineCheck`
- `EngineEval`
- `EngineLine`
- raw PV
- proofFailures text
- source row
- renderer wording
- LLM wording

Still closed:

- deterministic renderer
- LLM narration
- public route `200`
- user-facing prose
- pedagogy
- new Story family
- engine explanation

Explanation Plan must not decide, prove, rank, repair, or invent chess meaning.
It may bound speech only from selected Verdict data. It must not read raw board
facts, raw engine evidence, source rows, diagnostics, renderer wording, or LLM
wording to expand that boundary.

Stage 6-0 completion standard: Explanation Plan defines what may be said from
the selected Verdict, but it writes no sentence.

`StoryInteractionLaw.md` is the single live authority for the Stage 6 charter.
Other live documents may summarize Stage 6 scope only.

## Stage 6-1 Explanation Plan Shape

Stage 6-1 goal: turn one selected Verdict into a small pre-speech plan.

First scope handles exactly one `Tactic.Hanging` Lead Verdict.

Allowed fields:

- role
- scene
- tactic
- side
- target
- secondaryTarget
- anchor
- route
- allowedClaim
- evidenceLine
- strength
- forbiddenWording
- supportContextLinks

`allowedClaim` is a structured claim key, not a natural-language sentence.
The first live claim key is `can_win_piece`.
The first live strength key is `bounded`.
support/context links stay empty in the first scope.

Stage 6-1 must not read raw Board Facts, raw BoardMood, root atoms,
`CaptureResult`, `EngineCheck`, `EngineEval`, `EngineLine`, raw PV,
proofFailures text, source rows, renderer wording, or LLM wording.

Still closed:

- full sentence generation
- user-facing prose
- `engine says`
- best move
- winning
- decisive
- public eval

The first shape may bound only this claim: the selected side can win the named
target piece along the selected route. It must not call the move free,
blunder, winning, decisive, forced, best, engine-approved, or evaluated.

## Stage 6-2 Tactic.Hanging Allowed Claim Mapping

Stage 6-2 goal: define which claim keys a `Tactic.Hanging` Verdict may lower to.

Allowed claim keys:

- `can_win_piece`
- `piece_can_be_taken_with_gain`
- `capture_leaves_material_gain`

Forbidden claim keys:

- `free_piece`
- `blunder`
- `winning_tactic`
- `decisive_tactic`
- `forced_win`
- `best_move`
- `no_counterplay`
- `engine_approved`

Only uncapped Lead Verdict may carry an allowed claim key.
Support and Context are not standalone claims.
Blocked creates no allowed claim.
`engineStrengthLimited` suppresses allowed claim keys and strengthens forbidden
wording.

`engineStrengthLimited` may add a strong-wording ban only from the selected
Verdict. It must not expose `EngineCheck`, raw engine eval, raw PV, engine
approval, best-move, winning, decisive, forced-win, no-counterplay, or blunder
claims.

Stage 6-2 completion standard: one selected uncapped `Tactic.Hanging` Lead
Verdict may lower to a safe structured claim key. Other roles create no public
claim.

## Stage 6-3 Forbidden Wording Boundary

Stage 6-3 goal: Explanation Plan carries forbidden wording that renderer or
LLM layers must not say.

Default forbidden wording:

- `free piece`
- `blunder`
- `winning`
- `decisive`
- `forced`
- `best move`
- `only move`
- `engine says`
- `no counterplay`
- `king unsafe`
- `file control`
- `outpost`
- `strategic key`
- `conversion`
- `mate net`

`Tactic.Hanging` first allowed claim remains bounded material tactic only.
`engineStrengthLimited=true` strengthens the forbidden wording boundary.
`engineStrengthLimited=true` carries no allowed claim key.
Explanation Plan must make forbidden wording clearer than allowed speech.

Stage 6-3 must not open renderer wording, LLM wording, public route `200`,
user-facing prose, pedagogy, new Story families, engine explanation, engine
approval, best-move explanation, strategic explanation, conversion advice, or
king-safety claims.

Stage 6-3 completion standard: Explanation Plan carries a stronger forbidden
wording boundary than its allowed claim boundary. It still writes no sentence.

## Stage 6-4 Support / Context Relation

Stage 6-4 goal: carry Support and Context as structure-only relations inside
Explanation Plan.

Rules:

- Uncapped Lead only carries an allowed claim.
- Support carries relation to Lead only.
- Context creates no public claim.
- Blocked may enter Explanation Plan only as debug-only relation structure.
- proofFailures must not feed Explanation Plan wording or relation text.

Allowed relation keys:

- `same_family_lower_rank`
- `alternative_hanging_candidate`
- `capped_same_story`
- `blocked_by_engine_refute`

Forbidden:

- Support standalone sentence
- Context standalone sentence
- Blocked debug text as user explanation
- proofFailures text as wording

Stage 6-4 must not read proofFailures text, raw Board Facts, raw BoardMood,
root atoms, `CaptureResult`, `EngineCheck`, `EngineEval`, `EngineLine`, raw
PV, source rows, renderer wording, or LLM wording to create relation text.

Stage 6-4 completion standard: Support and Context enter Explanation Plan only
as relation structure. Uncapped Lead remains the only role with an allowed
claim.

## Stage 6-5 Selected Verdict Only Guard

Stage 6-5 goal: Explanation Plan receives selected Verdict only.

Allowed input:

- selected uncapped Lead Verdict only

Forbidden input:

- raw BoardFacts
- BoardMood
- root atoms
- MultiTargetProof
- CaptureResult
- EngineCheck
- EngineEval
- EngineLine
- raw PV
- proofFailures text
- unselected Story
- unselected Verdict
- source row

Explanation Plan must not expose overloads, constructors, fields, or relation
text paths for raw proof material. It may read only the selected Verdict value
and the fields already carried by that Verdict.

Stage 6-5 completion standard: Explanation Plan does not read raw proof
material directly. It creates no chess meaning beyond the selected Verdict.

## Stage 6 Closeout

Stage 6 closes with Explanation Plan only.

Renderer, LLM, public route `200`, user-facing prose, and pedagogy remain
closed.

Explanation Plan creates no chess meaning. StoryTable and Verdict keep
selection authority. `EngineCheck` and `CaptureResult` keep evidence authority.
Explanation Plan must not duplicate their roles.

Negative corpus:

- Blocked, Support, Context, engine-capped, and engine-refuted Verdicts create
  no allowed claim or public claim.
- Engine-capped Verdicts may carry `capped_same_story` relation structure and
  stronger forbidden wording only.
- Engine-refuted Verdicts may carry debug-only `blocked_by_engine_refute`
  relation structure only.
- proofFailures text remains diagnostic only and must not become explanation
  wording.

Cleanup / consolidation:

- No new Story family opens.
- No renderer, LLM, public route, or pedagogy opens.
- No new row or md authority is introduced by Stage 6 closeout.
- Existing Explanation Plan type names remain shape names only; they do not
  decide, prove, rank, repair, or invent.

Next-stage handoff:

- Stage 7 deterministic renderer may receive Explanation Plan only.
- Stage 7 must not read raw Verdict, `EngineCheck`, `CaptureResult`, Board
  Facts, BoardMood, raw PV, proofFailures text, source rows, or raw engine
  evidence directly.
- Stage 7 must not create chess meaning, repair missing proof, upgrade claim
  strength, or open public route `200` without its own contract and tests.

One chess meaning, one home.
One rule, one live authority.
Verdict decides. Explanation Plan bounds speech.

## Stage 7-0 Deterministic Renderer Charter

Stage 7 name is `Deterministic Renderer`.

Core sentence: Explanation Plan bounds speech. Renderer only phrases it.

Stage 7-0 fixes what the Deterministic Renderer may open before LLM narration
or public route work. The goal is a deterministic template baseline that turns
`ExplanationPlan` into internal text without making chess meaning.

Allowed:

- `ExplanationPlan` only input
- deterministic template
- `Tactic.Hanging` bounded claim
- forbidden wording check
- no LLM
- no public route

Forbidden input / authority:

- raw Verdict
- Story
- Board Facts
- CaptureResult
- EngineCheck
- EngineEval / EngineLine
- raw PV
- proofFailures text
- source row

Forbidden speech:

- user-level pedagogy
- best-move wording
- engine-says wording
- winning wording
- decisive wording
- forced wording
- blunder wording
- free-piece wording

The Deterministic Renderer may phrase only fields already present in the
`ExplanationPlan`. It must not read, recover, repair, or reinterpret raw proof
material. It must not create a new Story, choose a Verdict, explain engine PV,
name a best move, or upgrade bounded material wording into stronger speech.

Every deterministic template must pass the forbidden wording boundary before
text can leave the renderer boundary. LLM narration, public route `200`,
pedagogy, new Story families, engine explanation, engine-says wording,
best-move explanation, winning, decisive, forced, blunder, and free-piece
wording remain closed.

Stage 7-0 completion standard: Stage 7 charter fixes that Deterministic
Renderer creates no chess meaning and phrases Explanation Plan only.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-1 Renderer Input Guard

Stage 7-1 goal: Renderer receives ExplanationPlan only.

Allowed input:

- ExplanationPlan

Forbidden input:

- Verdict
- Story
- BoardFacts
- BoardMood
- CaptureResult
- EngineCheck
- EngineEval
- EngineLine
- raw PV
- proofFailures
- source row

Renderer must expose no `fromVerdict`, `fromStory`, `fromBoardFacts`, or
`fromEngineCheck` path.

Renderer must not create a sentence without an `ExplanationPlan`.

Renderer must not read raw Verdict, Story, BoardFacts, BoardMood,
CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV, proofFailures, or
source-row material directly. It may phrase only the `ExplanationPlan` already
bounded by Stage 6.

LLM narration, public route `200`, pedagogy, new Story families, engine PV
explanation, best-move wording, engine-says wording, winning, decisive,
forced, blunder, and free-piece wording remain closed.

Stage 7-1 completion standard: Renderer does not read proof material directly.

Player notation boundary:

- SAN formats an already-approved legal move only
- legal `Line` endpoints remain proof binding only
- route SAN is the speech notation carried from Story into ExplanationPlan
- SAN check or mate marks are legal-replay notation only
- SAN does not create Story, Proof, Verdict, ExplanationPlan, or public claims
- Renderer and LLM smoke must not phrase moves as origin-destination routes
- SAN text owns no proof, ranking, or chess meaning by itself

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-2 Minimal Tactic.Hanging Template

Stage 7-2 goal: turn only the `can_win_piece` claim key into deterministic text.

Input conditions:

- role is Lead
- allowedClaim is `CanWinPiece`
- strength is `Bounded`
- debugOnly is false
- route exists
- route SAN exists
- target exists
- evidenceLine exists
- forbidden wording set exists

Allowed first template:

- `dxe5 wins material against the piece on e5.`

Forbidden wording:

- free piece
- blunder
- winning
- decisive
- forced
- best move
- only move
- engine says
- no counterplay
- king unsafe
- file control
- outpost

Renderer must refuse Support, Context, Blocked, debug-only, missing-route,
missing-target, missing-evidenceLine, missing-forbidden-wording, and
non-`CanWinPiece` plans.

The template may use only the selected `ExplanationPlan` side, target, route,
route SAN, evidenceLine, allowed claim key, strength, and forbidden wording. It must not
read raw Verdict, Story, BoardFacts, CaptureResult, EngineCheck, EngineEval,
EngineLine, raw PV, proofFailures, or source-row material.

LLM narration, public route `200`, pedagogy, engine explanation, best-move
explanation, engine-says wording, winning, decisive, forced, blunder,
free-piece, no-counterplay, king-safety, file-control, outpost, and strategic
wording remain closed.

Stage 7-2 completion standard: the first `Tactic.Hanging` deterministic text
does not exceed the ExplanationPlan claim key or evidenceLine.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-3 Forbidden Wording Enforcement

Stage 7-3 goal: Renderer output must not violate `ExplanationPlan.forbiddenWording`.

Forbidden wording meanings:

- free piece
- blunder
- winning as position verdict
- decisive
- forced
- best move
- only move
- engine says
- no counterplay
- king unsafe
- file control
- outpost
- strategic key
- conversion
- mate net

Renderer must reject output when any forbidden wording meaning appears in
deterministic text.

`win material` wording is allowed only when `allowedClaim` is `CanWinPiece`.
`winning position` remains forbidden.

Engine-strength-limited plans must fail strong wording output.

Plans with no allowedClaim or debugOnly true must produce no output.

Stage 7-3 opens only forbidden wording enforcement. It does not open LLM
narration, public route `200`, pedagogy, new Story families, raw Verdict,
Story, BoardFacts, BoardMood, CaptureResult, EngineCheck, EngineEval,
EngineLine, raw PV, proofFailures text, source rows, engine explanation,
best-move explanation, or user-level pedagogy.

Stage 7-3 completion standard: Renderer automatically refuses forbidden wording.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-4 No Text for Support / Context / Blocked

Stage 7-4 goal: non-Lead ExplanationPlan rows must not create public text.

Rules:

- Lead with allowedClaim may create text
- Support creates no standalone text
- Context creates no standalone text
- Blocked creates no public text
- debugOnly true creates no public text
- engineStrengthLimited with no allowedClaim creates no public text
- engine-refuted relation creates no public text

Renderer must return no text for Support, Context, Blocked, capped no-claim,
and engine-refuted relation plans.

Stage 7-4 opens only the no-standalone-text boundary. It does not open Support
phrasing, Context phrasing, Blocked phrasing, relation narration, LLM
narration, public route `200`, pedagogy, raw Verdict, Story, BoardFacts,
BoardMood, CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV,
proofFailures text, source rows, engine explanation, best-move explanation, or
user-level pedagogy.

Stage 7-4 completion standard: Renderer phrases only Lead plans with an allowed claim.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-5 Rendered Line Shape

Stage 7-5 goal: deterministic renderer output stays small and verifiable.

`RenderedLine` fields:

- text
- claim key
- strength
- forbidden check passed

`RenderedLine` owns no chess meaning.
`RenderedLine` owns no proof.
`RenderedLine` owns no engine data.
`RenderedLine` is only the expression result of an `ExplanationPlan`.

Forbidden `RenderedLine` fields:

- CaptureResult
- EngineCheck
- BoardFacts
- proofFailures
- raw route analysis
- source row

`RenderedLine` must not include raw route analysis, source rows, proofFailure
text, board facts, capture evidence, engine checks, engine evals, engine
lines, raw PV, Story rows, or Verdict rows. It may carry only the text that
passed deterministic rendering, the selected claim key, the selected strength,
and the forbidden-wording check result.

Stage 7-5 opens only the RenderedLine shape. It does not open public route
`200`, LLM narration, relation narration, pedagogy, new Story families, raw
proof input, raw engine evidence, engine explanation, best-move explanation,
or source-row public claims.

Stage 7-5 completion standard: RenderedLine is only the expression result of ExplanationPlan.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-6 Renderer Baseline Tests

Stage 7-6 goal: renderer baseline tests prove output is no stronger than ExplanationPlan.

Baseline tests:

- Lead + CanWinPiece + bounded strength renders safe deterministic text
- Support renders no text
- Context renders no text
- Blocked renders no text
- debugOnly renders no text
- no allowedClaim renders no text
- engineStrengthLimited without allowedClaim renders no text
- forbidden wording appearing in output is rejected
- renderer cannot read Verdict directly
- renderer cannot read EngineCheck directly
- renderer cannot mention engine
- renderer cannot say best move, blunder, free piece, decisive, forced, or winning position

Stage 7-6 opens only renderer baseline tests.

It opens no new renderer wording, no new input, no route, no public route `200`, and no LLM narration.

Stage 7-6 completion standard: Renderer output is no stronger than ExplanationPlan.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7 Closeout Pass

Stage 7 closeout goal: audit that deterministic renderer is the only opened Stage 7 surface.

Explanation Plan bounds speech.
Renderer only phrases it.
One rule, one live authority.

Scope audit:

- only the deterministic renderer is open
- LLM narration remains closed
- public route `200` remains closed
- pedagogy remains closed
- new Story families remain closed

Authority audit:

- renderer creates no chess meaning
- renderer does not overlap ExplanationPlan ownership
- renderer does not overlap Verdict ownership
- renderer does not overlap StoryTable ownership
- renderer does not overlap EngineCheck ownership
- renderer does not overlap CaptureResult ownership

Negative corpus audit:

- Support plans produce no text
- Context plans produce no text
- Blocked plans produce no text
- capped plans produce no text
- refuted plans produce no text
- no-claim plans produce no text

Cleanup / consolidation:

Stage 7 closeout adds no new Story family, row, route, public payload, or markdown authority file.

Renderer rules live in `StoryInteractionLaw.md` only; other documents may summarize scope only.

Next-stage handoff:

Stage 8 LLM Narration may receive deterministic text and ExplanationPlan only.

Stage 8 must not read raw Verdict, Story, EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV, proofFailures text, or source rows directly.

Stage 7 completion criteria:

1. Renderer accepts `ExplanationPlan` only.
2. Only Lead `Tactic.Hanging` plans with allowedClaim create deterministic text.
3. Support, Context, Blocked, and debugOnly plans create no text.
4. Forbidden wording boundary violations are rejected.
5. Renderer output is no stronger than ExplanationPlan.
6. Renderer never creates engine, best-move, blunder, decisive, forced, free-piece, or winning-position wording.
7. Public route remains closed.
8. LLM narration remains closed.
9. Stage 8 handoff is limited to ExplanationPlan plus deterministic text.
10. Cleanup pass consolidated duplicate authority and over-documentation.

Stage 7 closeout completion standard: deterministic renderer is closed as a template baseline, and Stage 8 handoff is bounded to deterministic text plus ExplanationPlan.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 8 LLM Narration Prompt Smoke

Stage 8 name is `LLM Narration`.

Core sentence: LLM rephrases. It does not add chess meaning.

Stage 8 opens narration behavior smoke only.

It is not production API validation.

The prompt smoke input should match the production Stage 8 prompt shape as
closely as this checkpoint permits.

Open lanes:

- 8A Mock narrator
- 8B Codex CLI prompt smoke test
- 8C Production API micro-test remains closed
- 8D Nightly eval remains closed

8A Mock narrator allowed input:

- ExplanationPlan
- RenderedLine

8B Codex CLI prompt smoke allowed input:

- renderedText
- claimKey
- strength
- forbidden wording list
- instruction: "Rephrase only. Do not add chess facts."

Forbidden input:

- FEN
- PGN
- engine line
- eval
- CaptureResult
- EngineCheck
- BoardFacts
- raw Verdict
- Story
- BoardMood
- engine eval
- raw PV
- proofFailures
- source row

Forbidden output:

- new move
- new line
- new tactic
- new plan
- new cause or causal explanation
- new evaluation
- engine mention
- `engine says`
- best move
- forced
- winning
- decisive
- blunder
- free piece
- claim stronger than deterministic text
- chess meaning absent from ExplanationPlan

The mock narrator may echo deterministic text or perform a rules-based
rephrase. It receives only `ExplanationPlan` and `RenderedLine`. It must return
no narration when `RenderedLine` is absent, when the plan is Support, Context,
or Blocked, when forbidden wording appears, or when output would be stronger
than deterministic text.

The Codex CLI prompt smoke test is an LLM behavior check. Passing it means the
prompt contract is broadly safe enough for this checkpoint. It does not replace
production API validation.

Production API validation remains closed until a separate checkpoint proves:

- same system prompt
- same model / temperature / response format
- stable schema
- forbidden wording checker applied to API output
- cost and latency acceptable
- failure / retry / timeout fail closed

Stage 8B Codex CLI prompt smoke must check whether output:

1. rephrases only from renderedText, claimKey, strength, forbidden wording
   list, and the rephrase-only instruction.
2. respects the forbidden wording list.
3. stays no stronger than deterministic text.
4. adds no move, line, tactic, plan, engine mention, or chess meaning absent
   from ExplanationPlan.

8A/8B closeout checks:

- scope stayed limited to mock narrator plus Codex CLI smoke test.
- production API integration stayed closed.
- Stage 8C production API micro-test remains closed.
- streaming stayed closed.
- public route `200` stayed closed.
- user-facing LLM output stayed closed.
- raw proof material did not enter narration.
- forbidden wording is rejected.
- new move is rejected.
- new line is rejected.
- new tactic or plan is rejected.
- new cause or evaluation is rejected.
- engine mention is rejected.
- strengthened claim is rejected.
- no new Story family opened.
- no pedagogy opened.
- no meaning-duplicating type or markdown authority opened.

Closeout standard:

- Deterministic text is the ceiling.
- LLM only polishes below it.
- No raw proof material enters narration.

Stage 8 opens no public route `200`, no renderer input, no raw proof input, no
production API path, no pedagogy, no new Story family, no engine explanation,
and no natural-language verifier.

Stage 8 completion standard: Codex CLI prompt smoke can check narration behavior without opening production API, public route `200`, or new chess meaning.

One chess meaning, one home.
One rule, one live authority.
LLM rephrases. It does not add chess meaning.

## Scene Law

| scene | class | required support | blockers and caps | public wording |
|---|---|---|---|---|
| Scene.Tactic | live forcing | concrete tactic family, legal line, target, attacker, defender or reply | missing line, missing tactic, illegal route, engine contradiction | tactic wording only inside the legal line. |
| Scene.Blunder | live forcing | engine swing or forced material/mate line, losing move identity, rival refutation | no legal refutation, shallow engine only, no previous-position comparison | blunder wording only with swing or forced line. |
| Scene.Material | live static | material count, target piece or asset, capture or trade identity if dynamic | compensation Story with proof, tactical override, unclear recapture | material wording only for named asset. |
| Scene.King | split public | king square, king-ring attacks, escape issue, route or line proof | legal defense exists, no route, no escape proof, engine contradiction | king safety wording, no mate wording without mate proof. |
| Scene.Defense | live response | rival threat, defensive move or resource, protected target | rival threat unproven, defense illegal, stronger tactic override | defense wording only against named threat. |
| Scene.Opening | context-only | opening label, current board match, known line source | board mismatch, tactical refutation, source-only claim, board-backed Story at public floor | opening context only, no truth override or lead over board-backed Story. |
| Scene.Pawns | split public | exact pawn feature, square/file, blocker, lever, guard or passer identity | tactic override, no pawn identity, engine contradiction for plan wording | pawn structure wording for named feature. |
| Scene.PawnAdvance | split public | already-passed pawn, legal one-square non-capturing non-promotion route, exact after-board passed-pawn persistence | pawn not already passed, capture, promotion, double push, after-board passer missing, conversion or strategy wording | bounded passed-pawn advance wording only. |
| Scene.Plan | split public | one Plan row, affordance fact, route, anchor, proof | opposing tactic or blunder at public floor, missing route, engine contradiction | plan wording only as a plan, not as forced truth. |
| Scene.Pieces | split public | exact piece, mobility or route fact, target or anchor | no named piece, route illegal, tactical override | piece wording for named piece only. |
| Scene.Space | live static | controlled squares, safe mobility, space region, side identity | no same-board control, immediate tactic override | space wording only as board condition. |
| Scene.Initiative | conditional | repeated forcing moves, restricted rival replies, line proof | no forcing route, rival has equal resource, engine contradiction | initiative label only with forcing proof. |
| Scene.Convert | conditional | named advantage, legal route to named gain, rival defense | no route, no gain identity, counterplay risk high, engine contradiction | conversion wording only within proven gain. |
| Scene.Endgame | conditional | material shape, king/pawn route, promotion or fortress identity | middlegame tactics dominate, no route, tablebase or engine contradiction | endgame wording only for named shape. |
| Scene.Counterplay | conditional | resource against rival plan, target, route, timing | rival plan unproven, resource illegal, engine contradiction | counterplay wording only as named resource. |
| Scene.Source | context | source row, opening/game reference, same-board fit | board proof story at public floor, source mismatch | source context only; never leads over board proof. |
| Scene.Quiet | fallback | no other Story reaches public floor | any non-Quiet Story at public floor | quiet wording only as absence of stronger story. |

## Plan Law

Every Plan row is split public. A Plan can support `Scene.Plan`, but the plan
name cannot speak unless the row support is present and no hard blocker applies.

| plan | required affordance | required proof | blockers and caps |
|---|---|---|---|
| Plan.Minority | `queenside_minority_lever` | route, anchor, pawnSupport | no legal lever, tactical override, engine contradiction |
| Plan.Majority | `wing_pawn_majority` | target, anchor, pawnSupport | no passer route, rival blockade, tactical override |
| Plan.CenterBreak | `center_pawn_lever` | route, line or source, pawnSupport | lever illegal, center tactic loses material |
| Plan.FlankBreak | `flank_pawn_lever` | route, anchor, pawnSupport | king exposure without proof, lever illegal |
| Plan.Storm | `king_flank_pawn_step` | route, kingHeat, line if forcing | own king weak, no target, engine contradiction |
| Plan.Expansion | `space_gain_pawn_step` | route, clarity, pawnSupport | overextension tactic, no safe square |
| Plan.Cramp | `piece_mobility_clamp` | target, anchor, persistence | rival break exists, no named clamped piece |
| Plan.Outpost | `pawn_safe_outpost_square` | piece route, anchor, pieceSupport | enemy pawn can chase, no occupying or reachable piece |
| Plan.BadPiece | `boxed_piece` | target piece, route, clarity | no improvement route, tactical override |
| Plan.Reroute | StoryResurrectionLaw S201 | piece route, destination, proof | no legal path, no destination reason |
| Plan.Bishops | `bishop_pair_or_color` | target color, anchor, pieceSupport | closed diagonals without route, tactic override |
| Plan.Blockade | `passer_blockade_square` | blocker route, anchor, line if dynamic | blocker illegal, pawn can advance with gain |
| Plan.OpenFile | `rook_open_file_entry` | rook route, entry square, pieceSupport | entry square controlled with tactic, no rook |
| Plan.Seventh | `rook_seventh_targets` | rook route, targets, line if forcing | no seventh target, rook trapped |
| Plan.ColorBind | `fixed_color_weak_squares` | target color, anchor, persistence | opposite-color counterplay, no fixed weakness |
| Plan.WeakSquare | `pawn_safe_weak_square` | target square, route, pieceSupport | no reachable piece, enemy pawn can contest |
| Plan.Isolani | `isolated_pawn_front_square` | target pawn, blockade or lever route | dynamic break solves it, no attack route |
| Plan.BackwardPawn | `backward_pawn_front_square` | target pawn, anchor, route | pawn can advance safely, no pressure route |
| Plan.HangingPawns | `hanging_pawn_pair` | target pair, lever or blockade route | pair can advance with tempo, tactic override |
| Plan.ChainBase | `pawn_chain_base` | base pawn, attack route, anchor | no attack route, counterplay higher |
| Plan.PasserMake | `candidate_passer_lever` | pawn route, blocker identity, line if dynamic | lever illegal, rival blockade proof |
| Plan.PasserBlock | `passer_block_move` | blocker route, passed pawn target | blocker illegal, pawn promotes by force |
| Plan.Race | StoryResurrectionLaw S214 | both routes, tempo count, proof | tempo not proven, engine contradiction |
| Plan.Trade | StoryResurrectionLaw S215 | pieces, recapture chain, purpose | recapture unclear, tactic loses material |
| Plan.Simplify | StoryResurrectionLaw S216 | exchange path, preserved edge, proof | edge unproven, rival tactic after exchange |
| Plan.KeepPieces | StoryResurrectionLaw S217 | refused trade, preserved resource, proof | resource unproven, trade is forced |
| Plan.Overload | `dual_target_defender` | two targets, defender, legal test | no legal test, defender not tied to targets |
| Plan.Prophy | StoryResurrectionLaw S219 | rival threat, preventing move, proof | threat unproven, prevention illegal |
| Plan.Counterplay | StoryResurrectionLaw S220 | resource, rival plan, timing, proof | rival plan absent, resource too slow |
| Plan.Initiative | StoryResurrectionLaw S221 | forcing route, restricted replies, proof | quiet move breaks sequence, engine contradiction |
| Plan.KingConvert | StoryResurrectionLaw S222 | king route, target gain, proof | mate/material route absent, defense works |
| Plan.Convert | StoryResurrectionLaw S223 | advantage, route to gain, proof | no gain identity, high counterplay risk |

## Tactic Law

Every Tactic row is live forcing only when it has a legal line. Motif facts from
BoardMood can support a tactic, but they cannot prove it.

| tactic | required support | required line | blockers and caps |
|---|---|---|---|
| Tactic.Loose | loose target and attacker | legal capture or pressure line | target defended or recapture equalizes |
| Tactic.Hanging | attacked piece guard map | legal capture and recapture map | defender recaptures favorably |
| Tactic.AbsPin | pinned king line | legal line piece, pinned piece, king behind | line blocked or pinned piece can legally move |
| Tactic.RelPin | pinned valuable target line | legal line piece, screen, target | screen can move with gain or target not valuable |
| Tactic.Pin | pinned-to-king line relation | legal pinning or revealing move | target is king, relation incomplete, or line claim becomes material/king-safety wording |
| Tactic.Skewer | slider, front non-king material target, rear non-king material target | legal move creates or reveals front/rear same-line relation | missing front/rear relation or material/forced wording |
| Tactic.Xray | `white_xray_line` or `black_xray_line` | legal uncovering or pressure line | screen not movable, target defended |
| Tactic.Fork | attacker, two targets | legal move to fork square | fork square unsafe or targets can respond |
| Tactic.DiscoveredAttack | screened line and moving piece | legal discovered move | moving piece illegal or line target absent |
| Tactic.RemoveGuard | one defender guard relation removed | legal remove-guard move | incomplete RemoveGuardProof or stronger material/no-defense wording |
| Tactic.Overload | dual-target guard | legal test of one target | defender can cover both or recapture |
| Tactic.BackRank | back-rank line and flight squares | legal rook or queen check line | king has escape or interposition |
| Tactic.MateNet | king ring, checks, escapes | mate proof or decisive checking line | any legal defense not answered |
| Tactic.SafeCheck | checking move and safe attacker | legal check with reply map | checking piece lost without gain |
| Tactic.PawnFork | pawn move attacks two targets | legal pawn move | target can move with stronger threat |
| Tactic.PawnPush | promotion or tempo pawn step | legal pawn move and reply map | blocker or capture stops it |
| Tactic.Trap | piece mobility clamp and target | legal chase or no-escape line | escape square or counter-threat exists |
| Tactic.QueenHit | queen target and attacker | legal attack or tempo line | queen has stronger reply |
| Tactic.KingOpen | king line or shelter break | legal capture/push/check line | king remains defended or line closes |
| Tactic.Promote | promotion step | legal promotion route | stop square or capture refutes |
| Tactic.Clear | clearance move and line piece | legal clearance route | cleared line has no target |
| Tactic.Decoy | target lure square | legal forcing move to lure | target can decline safely |
| Tactic.Deflect | defender and target | legal deflection move | defender not needed or recaptures |
| Tactic.Tempo | forcing move with gained turn | legal move and rival reply restriction | rival has equal or stronger forcing move |

## Tactics Family Width Map

The width map is a proof-home map, not permission to open a new positive
family. It opens no Story writer, no renderer input, no LLM input, and no
public route.

One tactic name is not one proof system. Similar tactics must share the same
proof home when they rely on the same chess meaning. A proof home name in this
map is a planning label only unless a stage charter, sidecar, negative corpus,
and same-board legal-line tests admit it.

`Tactic.Hanging`, the narrow `Tactic.Fork` vertical slice, the narrow
`Tactic.DiscoveredAttack` vertical slice, the narrow `Tactic.Pin` writer
slice, and the narrow `Tactic.RemoveGuard` writer slice are live positive
tactic writers before the Skewer slice. `Tactic.Skewer` is now a closed narrow
positive slice through Skewer Closeout, with StoryTable, ExplanationPlan,
renderer, and LLM smoke bounded to `skewers_piece_to_piece`. QueenHit Stage-8
is now open only through bounded `attacks_queen` LLM smoke over the selected
QueenHit `RenderedLine`.
All other tactic names below are closed until their proof home and writer are
explicitly admitted.
`CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`,
`ExplanationPlan`, and Renderer remain in their current ownership boundaries.
They may be reused only through selected Verdict handoff and family-specific
charters; none of them may read raw proof material or create a new tactic.

Proof-home width:

| proof home | covered tactic names | shared proof shape | reusable current homes | new home needed |
|---|---|---|---|---|
| CaptureProof | `Tactic.Hanging`, `Tactic.Loose`, capture-shaped `Tactic.QueenHit` | legal capture, target identity, defender or recapture map, bounded material result | `CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, Renderer after claim mapping | no for live Hanging; yes before Loose or QueenHit speech broadens capture meaning |
| TargetProof | `Tactic.Fork`, `Tactic.PawnFork`, `Tactic.Skewer`, `Tactic.QueenHit`, `Tactic.Tempo` | one legal move creates target relation or tempo pressure; reply map proves rival cannot answer all relevant targets | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` for material ending | yes |
| LineProof | `Tactic.AbsPin`, `Tactic.RelPin`, `Tactic.Xray`, `Tactic.DiscoveredAttack`, `Tactic.Clear`, line-shaped `Tactic.Skewer` | same-board ray, screen, front or rear target, reveal or restriction, legal exploitation line | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes |
| PinProof | `Tactic.Pin` | one legal move creates or reveals a pinned-to-king relation over one non-king target | BoardFacts `LineFact`, `LineProof`, `PinProof`, `StoryProof`, `EngineCheck`, `StoryTable`; downstream remains closed | no for narrow Pin-2; yes before broad pin family |
| RemoveGuardProof | `Tactic.RemoveGuard` | one legal move removes one defender guard relation from one non-king material target | BoardFacts guard relation, `RemoveGuardProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; downstream remains bounded | no for narrow RemoveGuard Closeout; yes before broad defender family |
| SkewerProof | `Tactic.Skewer` | one legal move creates or reveals a slider attack on one front non-king material target with a second non-king material target behind it on the same line | BoardFacts `LineFact`, `SkewerProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `TacticSkewer`, `ExplanationPlan`, `DeterministicRenderer`, LLM smoke | no for closed narrow Skewer; yes before broad Skewer, material-gain, forced, or XRay speech |
| DefenderProof | `Tactic.Overload`, `Tactic.Deflect`, `Tactic.Decoy` | defender identity, protected target, dependency relation, legal test that removes or distracts the defender | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes |
| KingProof | `Tactic.SafeCheck`, `Tactic.BackRank`, `Tactic.MateNet`, `Tactic.KingOpen` | legal check or king-line action, escape map, interposition or capture replies, no unchecked king claim | BoardFacts king and line facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes |
| PromotionProof | `Tactic.PawnPush`, `Tactic.Promote` | legal pawn route, stop squares, capture stops, tempo count, promotion prize | BoardFacts pawn and legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes |
| MobilityProof | `Tactic.Trap` | target piece mobility map, chase route or no-escape line, rival counter-threat check | BoardFacts legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes |

Per-tactic width map:

| map | tactic name | proof shape | existing reusable homes | needs new home | false positive risks | EngineCheck needed | open now | blocked by |
|---|---|---|---|---|---|---|---|---|
| W01 | `Tactic.Hanging` | CaptureProof plus StoryProof plus EngineCheck | `CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, Renderer | no for current slice | recapture equalizes, target is pawn or king, wrong board, illegal capture, engine refutes | attached now for cap or refute | live positive only | stronger wording, other capture tactics, public route, and LLM stay closed |
| W02 | `Tactic.Loose` | CaptureProof or pressure line over an underdefended target | `CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, if pressure without capture is admitted | loose observation is not a gain, defender exists, pressure is too slow, capture equalizes | yes for cap or refute | no | CaptureProof broadening plus loose negative corpus |
| W03 | `Tactic.QueenHit` | `QueenHitProof` over one legal move whose exact after-board has the moving side attacking the rival queen | `QueenHitProof`, `StoryProof`, `EngineCheck` refute guard, `TacticQueenHit`, `StoryTable`, `ExplanationPlan`, `DeterministicRenderer`, LLM smoke | no for bounded QueenHit Stage-8; yes before wins-queen, trap, tempo, material-gain, or broad target-pressure speech | no rival queen after move, illegal move, missing same-board replay, queen not attacked after move, sibling tactic contamination, wins-queen/trap/tempo/material wording | yes for cap or refute only | bounded `attacks_queen` through LLM smoke only | Hanging, Fork, Skewer, Pin, RemoveGuard, Material, wins queen, queen trap, queen lost, tempo, material gain, best/only/forced, public route, production API |
| W04 | `Tactic.Fork` | TargetProof over two targets and reply map | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, `MultiTargetProof` | fork square unsafe, one target can move with tempo, rival has equal or stronger forcing move, no gain remains | yes | narrow Fork-4 backend only | renderer, LLM, public route, PawnFork, Skewer, QueenHit, Tempo |
| W05 | `Tactic.PawnFork` | TargetProof with pawn legal move and two target attacks | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, TargetProof with pawn route rule | pawn move illegal, pawn is pinned, target can reply with stronger threat, promotion context contaminates proof | yes | no | TargetProof after Fork plus pawn-fork negative corpus |
| W06 | `Tactic.Skewer` | SkewerProof over one front target and one rear target on a slider line | BoardFacts `LineFact`, `SkewerProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `TacticSkewer`, `ExplanationPlan`, `DeterministicRenderer`, LLM smoke | no for closed narrow Skewer; yes before broad Skewer or material/forced speech | front/rear relation missing, rear target not reachable, line can be blocked, material or forced wording too strong | yes for cap or refute | closed narrow positive slice | broad Skewer family, XRay, Material claim, front-piece-must-move, wins-rear-piece, public route, production API |
| W07 | `Tactic.Tempo` | TargetProof over gained turn and restricted rival reply | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, TargetProof with reply restriction | rival has equal forcing move, gained turn has no target, move is only a threat label | yes | no | TargetProof reply-map charter |
| W09 | `Tactic.AbsPin` | LineProof with king behind screen and legal restriction | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | pinned piece can legally move, line can be blocked, tactic does not exploit pin, king line is stale | yes | no | LineProof charter plus pin exploitation corpus |
| W10 | `Tactic.RelPin` | LineProof with valuable rear target and screen | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | rear target not valuable enough, screen can move with gain, line pressure has no legal use | yes | no | LineProof charter plus relative-pin corpus |
| W25 | `Tactic.Pin` | PinProof plus StoryProof over one legal pinned-to-king relation | BoardFacts `LineFact`, `LineProof`, `PinProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, `DeterministicRenderer`, LLM smoke | no for narrow Pin Closeout; yes before broad pin family | target is king, relation incomplete, slider absent, king-behind-target missing, material claim leaks, Defense or RemoveGuard meaning leaks | yes | Pin Closeout hard cleanup only | no Material claim, king-safety claim, mate threat, cannot-move wording, Defense ownership, RemoveGuard ownership, broad AbsPin or RelPin family, public route, production API, or public/user-facing LLM narration |
| W11 | `Tactic.Xray` | LineProof over screened ray and legal reveal or pressure | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | screen not movable, target defended, reveal loses tempo, ray observation is only geometry | yes | no | LineProof charter plus xray negative corpus |
| W12 | `Tactic.DiscoveredAttack` | LineProof over screened line and moving piece | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, Renderer, LLM smoke; public narration remains closed for Line Closeout | yes, LineProof | moving piece illegal, line target absent, discovered attack can be answered, moving piece hangs, target is king | yes | Line Closeout hard cleanup only | no Material claim, Pin, Skewer, XRay, RemoveGuard, public/user-facing LLM narration, public route, or production API |
| W13 | `Tactic.Clear` | LineProof over clearance move and newly usable line | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | cleared line has no target, clearance loses material, rival closes the line, route not legal | yes | no | LineProof clearance charter |
| W14 | `Tactic.RemoveGuard` | RemoveGuardProof plus StoryProof | BoardFacts guard relation, `RemoveGuardProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, `DeterministicRenderer`, LLM smoke | no for narrow RemoveGuard Closeout; yes before broad defender family | legal move missing, same-board proof missing, target is king, defender did not guard target, defender still guards target after move, Material or Hanging claim leaks, Defense or Pin meaning leaks, DiscoveredAttack meaning leaks | yes | RemoveGuard Closeout hard cleanup only | no broad deflection tactic, overloaded defender theory, no-defender claim, wins-material claim, Material claim, Hanging claim, Defense claim, Pin ownership, DiscoveredAttack ownership, public route, production API, or public/user-facing LLM narration |
| W15 | `Tactic.Overload` | DefenderProof over dual target dependency and legal test | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, DefenderProof | defender can cover both, one target irrelevant, reply order solves both threats, relation is only a plan label | yes | no | DefenderProof dual-target charter |
| W16 | `Tactic.Deflect` | DefenderProof over defender displacement and target gain | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, DefenderProof | defender not needed, defender can decline, displacement gives counterplay, target gain absent | yes | no | DefenderProof displacement corpus |
| W17 | `Tactic.Decoy` | DefenderProof or TargetProof over lure square and forced acceptance | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, DefenderProof with lure relation | target can decline safely, lure square is not exploitable, follow-up is illegal, gain is unproven | yes | no | DefenderProof lure charter |
| W18 | `Tactic.SafeCheck` | KingProof over legal check and reply map | BoardFacts king facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, KingProof | checking piece is lost without gain, legal reply wins tempo, check text becomes best-move or engine wording | yes | no | KingProof safe-check charter |
| W19 | `Tactic.BackRank` | KingProof plus LineProof over back rank and escape map | BoardFacts king and line facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, KingProof | escape square exists, interposition works, checking line illegal, mate wording leaks in | yes | no | KingProof escape corpus |
| W20 | `Tactic.MateNet` | KingProof over checks, escape map, and answered defenses | BoardFacts king facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, KingProof | any legal defense survives, net is only pressure, engine depth missing, mate wording too strong | yes, and mate proof must be explicit | no | KingProof mate-net charter and mate-negative corpus |
| W21 | `Tactic.KingOpen` | KingProof plus LineProof over shelter break or king line | BoardFacts king and line facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, KingProof | king remains defended, opened line closes, sacrifice lacks proof, attack wording outruns evidence | yes | no | KingProof shelter-break corpus |
| W22 | `Tactic.PawnPush` | PromotionProof or TargetProof over pawn step, stop squares, and reply map | BoardFacts pawn and legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, PromotionProof | blocker stops pawn, capture refutes, tempo step has no gain, promotion claim leaks in | yes | no | PromotionProof pawn-route charter |
| W23 | `Tactic.Promote` | PromotionProof over legal promotion route and stop-square proof | BoardFacts pawn and legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, PromotionProof | stop square exists, pawn is captured, promotion race lost, tablebase or engine refutes | yes | no | PromotionProof promotion-route corpus |
| W24 | `Tactic.Trap` | MobilityProof over target mobility and chase or no-escape line | BoardFacts legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, MobilityProof | escape square exists, counter-threat outranks trap, chase line illegal, trapped piece not valuable | yes | no | MobilityProof mobility corpus |

Safe opening order:

1. Keep `Tactic.Hanging` as the first live positive tactic while this map closes
   the width question.
2. Admit TargetProof/MultiTargetProof for `Tactic.Fork` only, with reply-map proof, same-board
   legal move proof, EngineCheck cap/refute attachment, and fork negative
   corpus. `Tactic.PawnFork`, `Tactic.QueenHit`, and `Tactic.Tempo` stay
   closed even though they share TargetProof shape.
   `Tactic.Skewer` is admitted only through its separate `SkewerProof` home,
   not through generic TargetProof.
3. Admit the remaining TargetProof tactics one by one only after the shared
   reply-map shape proves it does not open sibling tactics by name alone.
4. Admit LineProof for pin and ray tactics only after line exploitation is
   proven, not merely line geometry.
5. RemoveGuardProof admitted for narrow `Tactic.RemoveGuard`; broader DefenderProof for overload, deflection, and decoy remains closed.
6. Admit broader DefenderProof only after defender identity, sole-dependency,
   and target-gain tests exist.
7. Admit PromotionProof only after pawn route, stop-square, capture-stop, and
   tempo-count tests exist.
8. Admit KingProof only after escape, interposition, capture reply, and mate
   wording boundaries are proven.
9. Admit MobilityProof only after legal mobility and counter-threat tests
   exist.

Opening a proof home does not open all tactic names in that home. Each tactic
still needs a named Story writer, family proof sidecar, negative corpus,
EngineCheck attachment rule, StoryTable role rule, selected-Verdict
ExplanationPlan mapping, renderer wording boundary, and any LLM smoke boundary.
Production or user-facing LLM narration and public route `200` remain closed
for every non-Hanging tactic. Renderer wording opens tactic by tactic only; the
current Fork slice opens deterministic Fork renderer text in Fork-8 and Fork
LLM smoke in Fork-9.

## Fork-0 Tactic.Fork Charter

Fork is a multi-target Story, not a capture Story.

MultiTargetProof gives the reason. TacticFork writer gives permission.

The first `Tactic.Fork` scope opens only a simple non-pawn multi-target tactic:

- legal move to fork square
- attacker after move attacks two named targets
- two targets have bounded value or importance
- bounded reply map exists
- complete StoryProof
- same-board proof
- named `Tactic.Fork` writer
- EngineCheck support/cap/refute path
- StoryTable ordering with Hanging vs Fork
- ExplanationPlan mapping from selected Fork Verdict
- deterministic renderer text from Fork ExplanationPlan only
- LLM smoke from Fork ExplanationPlan and RenderedLine only

The first Fork scope does not open public material, winning, decisive, forced,
best-move, only-move, no-counterplay, blunder, free-piece, or engine-says
claims.

`Tactic.PawnFork`, `Tactic.Skewer`, `Tactic.QueenHit`, `Tactic.Tempo`,
`Scene.Material`, `Scene.Defense`, Plan, Strategy,
public/user-facing LLM narration, public route `200`, production API, engine
PV commentary, and best-move explanation remain closed.

Fork deterministic renderer text opens only in Fork-8 and only from
ExplanationPlan. Fork LLM smoke opens only in Fork-9 and only from
ExplanationPlan plus RenderedLine. Stage 8 output boundaries remain unchanged:
renderer and LLM smoke may not read raw Story, MultiTargetProof, EngineCheck,
BoardFacts, BoardMood, raw PV, proofFailures text, or source rows directly.

## Fork-1 Geometry Readiness

Fork geometry readiness is internal `MultiTargetProof` evidence only.
BoardFacts may provide observation facts, current board, legal moves, pieces,
and attack tests, but BoardFacts must not say that a fork works.

`MultiTargetProof` may record:

- current board same-board proof
- legal move to the fork square
- attacker piece
- attacker-after-move on the fork square
- post-move attacked target squares
- two named target pieces

Allowed geometry findings are limited to legal move exists, attacker can move
to the fork square, and after that move the attacker attacks target A and
target B.

Fork-1 does not create a public Story, Verdict, renderer sentence, LLM
narration, public route `200`, or public material claim by itself. It does not
say fork succeeds, wins material, wins queen, decisive fork, forced fork, or
best move.

## Fork-2 MultiTargetProof

`MultiTargetProof` is the family-specific proof home for the first Fork slice.
It creates Fork evidence, but it does not directly create a public Story.
`TacticFork` writer permission remains a separate permission step.

`MultiTargetProof` must carry:

- attacker
- legal move to fork square
- fork square
- target A
- target B
- target value or target class
- reply map
- same-board proof
- missing evidence

`MultiTargetProof` is complete only when the move is legal, the attacker
exists, two distinct targets exist, targets are enemy pieces or valid tactical
targets, the attacker attacks both targets after the move, and same-board proof
exists.

The current Fork proof scope admits any legal attacker shape, including a pawn,
when the same existing `MultiTargetProof` identity is complete: legal move,
attacker-after-move, two distinct rival non-king targets, both targets attacked
after the move, reply map, and same-board proof. It still has no skewer,
queen-hit-only tactic, king target, or mate claim, and it must not create
material-win, best-move, forced, decisive, or no-counterplay claims.

## Fork-3 TacticFork Writer

`TacticFork` is the named positive writer for the first narrow `Tactic.Fork`
Story.

`TacticFork` may create a Story only when:

- Story scene is `Scene.Tactic`
- tactic is `Tactic.Fork`
- StoryProof is complete
- MultiTargetProof is complete
- same-board proof is present
- legal move to the fork square is present
- two named targets are present
- target relation is proven after the move
- writer is `StoryWriter.TacticFork`
- EngineCheck does not `Refutes`

The first allowed Fork meaning is limited to move attacks two targets and move
creates a fork on named targets.

`TacticFork` does not open wins-queen, wins-material, decisive, forced,
best-move, only-move, no-counterplay, blunder, or engine-says wording. It does
not open Fork renderer text, public/user-facing Fork LLM narration, public
route `200`, pedagogy, Strategy, Plan, Scene.Material, Scene.Defense, or broad
tactic-family speech.

Completion standard: one narrow Fork Story with complete StoryProof,
complete MultiTargetProof, same-board proof, and no EngineCheck Refutes result
may enter StoryTable as a Lead. A Fork-looking row with unproven target
relation, missing writer, incomplete proof, or EngineCheck Refutes result must
not lead.

## Fork-4 Negative Corpus

Fork-looking false positives stay silent unless `MultiTargetProof`,
StoryProof, the named `Tactic.Fork` writer, same-board proof, and EngineCheck
guards all pass.

The Fork negative corpus must cover:

- no legal move
- missing attacker
- missing fork square
- missing two targets
- duplicated target
- own-piece target
- target not attacked after the move
- fork square unsafe with no compensation
- one reply saving both targets
- stronger rival reply refuting the Story
- skewer trying to enter `Tactic.Fork`
- queen-hit-only trying to enter `Tactic.Fork`
- incomplete StoryProof
- incomplete MultiTargetProof
- EngineCheck Refutes
- high Proof score only

Completion standard: a fork-looking shape with insufficient proof produces no
Lead or becomes Blocked, and sibling tactics cannot piggyback on Fork.

## Fork-5 EngineCheck For Tactic.Fork

Fork reuses the existing `EngineCheck` sidecar. There is no `ForkEngineCheck`
type.

`EngineCheck` may attach to an existing Fork Story with status `Supports`,
`Caps`, `Refutes`, or `Unknown` only when:

- the Fork Story already exists
- same-board proof is present
- checked move binds the same Story route
- checked move is the same legal line
- fresh or depth evidence is present
- EngineCheck evidence is ready

`Supports` creates no new claim. `Caps` limits strength but creates no engine
wording. `Refutes` blocks the Fork Story. `Unknown` is diagnostic only and
creates no engine expression.

Engine evidence cannot create Fork by itself. Engine PV explanation,
best-move explanation, winning, decisive, forced, no-counterplay, public eval,
and engine-says claims remain forbidden.

Completion standard: EngineCheck can support, cap, or refute an existing Fork
Story, but engine lines, engine eval, PV-shaped data, missing-depth checks,
route-mismatched checks, and engine-only checks cannot create Fork or attach as
Fork evidence.

## Fork-6 StoryTable Hanging Vs Fork

Fork-6 opens only StoryTable role ordering for existing `Tactic.Hanging` Story
rows and existing narrow `Tactic.Fork` Story rows.

StoryTable may assign `Lead`, `Support`, `Context`, and `Blocked` roles with
deterministic ordering. A refuted Fork, incomplete Fork, writerless Fork, or
Fork without `MultiTargetProof` becomes `Blocked`. Hanging and Fork may both be
eligible for `Lead`; StoryTable chooses one deterministic `Lead` without
creating a new Story or new chess meaning.

Support and Context are not sentences. MultiTargetProof text stays internal and
does not become public wording.

StoryTable must not create Fork, rank Fork by raw engine eval or raw PV, expose
MultiTargetProof text, or use renderer wording as an ordering input.

Completion standard: when Hanging and Fork exist together, StoryTable performs
deterministic role ordering only. It creates no new chess meaning, public
claim, renderer text, LLM narration, public route `200`, pedagogy, Strategy,
Plan, `Scene.Material`, `Scene.Defense`, or sibling tactic family.

## Fork-7 ExplanationPlan For Fork

Fork-7 opens only ExplanationPlan mapping for a selected narrow `Tactic.Fork`
Verdict.

Allowed input:

- selected Verdict only

Forbidden input:

- MultiTargetProof
- EngineCheck
- CaptureResult
- BoardFacts
- raw PV
- proofFailures
- source row

The first allowed Fork claim keys are:

- `forks_two_targets`
- `attacks_two_targets`

The selected Fork plan may carry role, scene, tactic, side, target,
secondaryTarget, anchor, route, evidenceLine, bounded strength, forbidden
wording, and relation structure. It does not read MultiTargetProof,
EngineCheck, CaptureResult, BoardFacts, raw PV, proofFailures, or source rows
directly.

Forbidden Fork claim keys and wording include:

- `wins_material_by_fork`
- `wins_queen`
- `decisive_fork`
- `forced_win`
- `best_move`
- `no_counterplay`

Support, Context, Blocked, capped, and refuted Fork Verdicts create no
standalone claim, no public sentence, and no Fork ExplanationPlan.
`engineStrengthLimited` suppresses allowed claim keys by keeping the Fork row
silent.

Fork-7 does not open Fork renderer text, public/user-facing Fork LLM
narration, public route `200`, pedagogy, Strategy, Plan, `Scene.Material`,
`Scene.Defense`, engine PV commentary, best-move explanation, or sibling
tactic families.

Completion standard: Fork ExplanationPlan creates no meaning stronger than the
selected Verdict.

## Fork-8 Deterministic Renderer For Fork

Fork-8 opens only deterministic renderer text for a selected Fork
ExplanationPlan.

Allowed input:

- ExplanationPlan only

Forbidden input:

- MultiTargetProof
- EngineCheck
- CaptureResult
- BoardFacts
- BoardMood
- raw PV
- proofFailures
- source row
- raw Verdict
- source Story

The first Fork renderer template is:

`{route} forks the pieces on {targetA} and {targetB}.`

`targetA` and `targetB` must come from structured `target` and
`secondaryTarget` fields already present in the Fork ExplanationPlan. The
renderer must not read MultiTargetProof, EngineCheck, CaptureResult,
BoardFacts, BoardMood, raw PV, proofFailures, source rows, raw Verdict, or
source Story to recover missing targets.

The first Fork renderer permission requires:

- role is `Lead`
- scene is `Scene.Tactic`
- tactic is `Tactic.Fork`
- allowed claim is `forks_two_targets`
- bounded strength
- non-debug plan
- route and evidenceLine match
- target and secondaryTarget are present
- forbidden wording is present

Forbidden Fork renderer wording includes:

- wins queen
- wins material
- decisive
- forced
- best move
- engine says
- no counterplay
- blunder

Support, Context, Blocked, capped, and engine-refuted Fork plans produce no
text. The `attacks_two_targets` claim key remains an internal allowed claim key
but does not open the first Fork renderer template.

Fork-8 itself does not open Fork LLM smoke, public/user-facing LLM narration,
public route `200`, production API, pedagogy, Strategy, Plan,
`Scene.Material`, `Scene.Defense`, engine PV commentary, best-move explanation,
material-win wording, wins-queen wording, or sibling tactic families.

Completion standard: Fork renderer text is no stronger than the selected Fork
ExplanationPlan.

## Fork-9 LLM Smoke For Fork

Fork-9 opens only LLM smoke for selected Fork ExplanationPlan and RenderedLine.
It does not open production API validation, user-facing LLM output, public
route `200`, streaming, pedagogy, Strategy, Plan, `Scene.Material`,
`Scene.Defense`, engine PV commentary, best-move explanation, material-win
wording, wins-queen wording, or sibling tactic families.

Allowed input:

- ExplanationPlan
- RenderedLine

8B Codex CLI prompt smoke allowed input for Fork:

- renderedText
- claimKey
- strength
- forbidden wording list
- instruction: "Rephrase only. Do not add chess facts."

Forbidden input:

- raw Verdict
- Story
- MultiTargetProof
- EngineCheck
- BoardFacts
- BoardMood
- CaptureResult
- EngineEval
- EngineLine
- engine eval
- raw PV
- proofFailures
- source row

Fork LLM smoke must reject output that adds:

- new move
- new line
- new tactic
- new plan
- engine mention
- best move
- forced
- winning
- decisive
- blunder
- wins queen
- wins material
- target piece identity absent from renderedText
- claim stronger than deterministic text

Completion standard: Fork LLM smoke does not strengthen Fork deterministic
text.

## Fork Slice Closeout Pass

Fork slice closeout goal: audit that the Fork closeout opened only the narrow `Tactic.Fork` vertical slice.

Scope audit:

- opened by original Fork closeout: narrow non-pawn `Tactic.Fork`; Stage-0
  Fork-PawnAttacker Admission extends the same Story to pawn attackers
- closed: `Tactic.PawnFork`, `Tactic.Skewer`, `Tactic.QueenHit`, `Tactic.Tempo`
- not opened by Fork closeout: `Scene.Material`, `Scene.Defense`, Plan, Strategy
- closed: public route `200`, production API, public/user-facing LLM narration, pedagogy, engine PV commentary, best-move explanation, material-win wording, wins-queen wording, and sibling tactic-family speech

Authority audit:

- MultiTargetProof does not replace CaptureResult.
- MultiTargetProof does not replace StoryProof.
- MultiTargetProof does not replace EngineCheck.
- MultiTargetProof does not replace StoryTable.
- MultiTargetProof supplies the TargetProof-shaped reason: legal move, attacker, fork square, two target identities, post-move target relation, reply map, same-board proof, bounded result, and missing evidence.
- `TacticFork` writer gives Story permission.
- `StoryProof` remains the same-board Story proof tuple.
- `EngineCheck` checks, caps, or refutes only an existing Fork Story.
- `StoryTable` assigns Lead, Support, Context, or Blocked without creating Fork.
- `ExplanationPlan`, Renderer, and LLM smoke receive only their downstream inputs and create no new chess meaning.

Negative corpus audit:

Fork-looking false positives either produce no Story, no Lead, or Blocked.

The closeout negative corpus remains:

- no legal move
- missing attacker
- missing fork square
- missing two targets
- duplicated target
- own-piece target
- target not attacked after the move
- fork square unsafe with no compensation
- one reply saving both targets
- stronger rival reply or EngineCheck Refutes
- skewer trying to enter `Tactic.Fork`
- queen-hit-only trying to enter `Tactic.Fork`
- incomplete StoryProof
- incomplete MultiTargetProof
- high Proof score only
- LLM rephrase stronger than deterministic Fork text

Cleanup and consolidation:

No new markdown authority file, public row family, public route, production API, or sibling tactic writer opens in Fork closeout.

`MultiTargetProof` is the first live TargetProof-shaped home. It remains a
proof shape, not a tactic family. Its current constructor is scoped to the
existing narrow Fork path, including Stage-0 admitted pawn attackers, but the
shape can support subsequent target-relation tactics only
after each named family opens its own permission path.

The proof shape remains reusable for subsequent PawnFork, Skewer, QueenHit, or Tempo work only after each family gets its own named writer, negative corpus, EngineCheck rule, StoryTable rule, ExplanationPlan mapping, renderer boundary, and LLM smoke boundary.

Next-stage handoff at Fork closeout:

The next family candidates were `Scene.Material` or `Scene.Defense`.

Fork does not open `Scene.Material` or `Scene.Defense` by implication.
Material-3 separately opens only the narrow named `Scene.Material` writer.

Fork does not create material, defense, plan, strategy, or tempo public
meaning. Target values and bounded result stay internal proof evidence until a
separate family authority admits a public claim.

`One tactic name is not one proof system.`
`One proof shape may support multiple tactics.`
`One chess meaning, one home.`

## Stage-0 Fork-PawnAttacker Admission Charter

Stage-0 opens only admission of a pawn attacker into the existing
`Tactic.Fork` proof path.

Stage-0 opens:

- existing `Tactic.Fork` allowed attacker scope extended to pawn attackers.
- no new public Story.
- meaning: after a legal move, one piece attacks two targets at the same time;
  that attacker may be a pawn.

Stage-0 keeps the existing chain:

- Proof home: `MultiTargetProof`
- Story label: `Tactic.Fork`
- Writer: `TacticFork`
- Speech key: existing Fork claim key only

Stage-0 does not open:

- `Tactic.PawnFork`
- `PawnForkProof`
- `TacticPawnFork`
- `pawn_forks_two_targets`
- new StoryTable family
- new ExplanationPlan claim key
- new renderer template for PawnFork
- new LLM smoke path for PawnFork
- promotion threat
- pawn advance
- pawn capture
- material gain
- tempo
- best / only / forced
- decisive / winning
- public route `200`
- production API
- public/user-facing LLM narration

Stage-0 rules:

- A pawn attacker is admitted only by satisfying the existing Fork identity:
  complete `StoryProof`, complete `MultiTargetProof`, same-board legal replay,
  legal move to the fork square, two distinct rival non-king targets, both
  targets attacked after the move, reply-map safety, named `TacticFork` writer,
  and no EngineCheck Refutes result.
- Pawn-specific vocabulary does not create, repair, rank, prove, strengthen, or
  phrase a Story. If a pawn-attacker row lacks the existing Fork proof identity,
  it stays silent or blocked.
- Existing Fork downstream boundaries remain unchanged. StoryTable orders the
  existing `Tactic.Fork` row; `ExplanationPlan` emits only the existing Fork
  claim key; renderer and LLM smoke receive only the existing Fork downstream
  contracts.
- Promotion threat, pawn advance, pawn capture, material gain, tempo,
  best/only/forced, decisive/winning, public route `200`, production API, and
  public/user-facing LLM narration remain closed.

Completion standard: Stage-0 Fork-PawnAttacker Admission closes when a legal
pawn attacker can enter the existing `Tactic.Fork` path through complete
`MultiTargetProof` and `TacticFork`, emits no new Story label, writer, proof
home, StoryTable family, ExplanationPlan claim key, renderer template, or LLM
smoke path, keeps `Tactic.PawnFork`, `PawnForkProof`, `TacticPawnFork`, and
`pawn_forks_two_targets` closed, opens no promotion-threat, pawn-advance,
pawn-capture, material-gain, tempo, best/only/forced, decisive/winning, public
route `200`, production API, or public/user-facing LLM narration meaning, keeps
summary docs summary-only, leaves AGENTS.md unchanged, passes targeted
commentary tests, passes docs authority tests, and passes `git diff --check`.

## Stage-1 MultiTargetProof Pawn Attacker Admission

Stage-1 opens only `MultiTargetProof` completion eligibility for a legal pawn
attacker. It does not create a public Story, writer, StoryTable family,
ExplanationPlan key, renderer template, LLM smoke path, public route, or
production API.

Stage-1 proof conditions:

- same-board proof
- legal move exists
- moving piece before move is pawn
- pawn after move is on route destination
- route is the legal move
- after-board pawn attacks target A
- after-board pawn attacks target B
- target A and target B are distinct rival non-king pieces
- reply map remains complete
- fork square safety / bounded reply rule remains enforced

Stage-1 keeps closed:

- pseudo-legal pawn move
- pinned pawn pseudo-move
- promotion move
- promotion threat
- en passant
- pawn capture as material claim
- pawn advance Story
- passed pawn Story
- tempo
- material win wording
- `Tactic.PawnFork`
- `PawnForkProof`
- `TacticPawnFork`
- `pawn_forks_two_targets`
- new StoryTable family
- new ExplanationPlan claim key
- new renderer template for PawnFork
- new LLM smoke path for PawnFork
- public route `200`
- production API
- public/user-facing LLM narration

Stage-1 rules:

- `MultiTargetProof.publicClaimAllowed` remains false.
- `MultiTargetProof` still does not create Story.
- Pawn attacker admission is an existing Fork proof-home expansion only.
  `TacticFork` remains the named writer for any downstream `Tactic.Fork` row.
- Pawn-route proof rejects promotion and en passant routes. Legal replay still
  rejects pseudo-legal and pinned-pawn pseudo routes.
- Pawn capture, pawn advance, passed pawn, tempo, material gain, best,
  only, forced, decisive, and winning meanings remain closed.

Completion standard: Stage-1 closes when `MultiTargetProof` can complete for a
legal pawn move that lands the pawn on the route destination and attacks two
distinct rival non-king targets on the exact after-board, while en passant,
promotion, pseudo-legal routes, pinned-pawn pseudo routes, missing targets, and
unsafe fork squares remain incomplete, `publicClaimAllowed` remains false,
`MultiTargetProof` creates no Story, no PawnFork public surface appears, summary
docs remain summary-only, AGENTS.md remains unchanged, targeted commentary tests
pass, docs authority tests pass, and `git diff --check` passes.

## Stage-2 TacticFork Writer Admission

Stage-2 opens only `TacticFork` writer admission for an existing `Tactic.Fork`
row whose complete `MultiTargetProof` has a pawn attacker. It does not open a
new Story label, pawn-specific writer, pawn-specific speech key, material claim,
promotion-threat claim, pawn-advance claim, or pawn-capture claim.

Stage-2 writer conditions:

- complete StoryProof
- complete `MultiTargetProof`
- same-board legal replay
- legal route matches proof route
- attacker may be pawn or non-pawn
- two named target squares are bound
- targets are distinct rival non-king pieces
- both targets are attacked after the move
- writer = `TacticFork`
- EngineCheck does not Refute

Stage-2 Story identity:

- scene = Tactic
- tactic = Fork
- plan = None
- side = attacking side
- rival = target owner side
- target = first target square
- secondaryTarget = second target square
- anchor = attacker square after the move
- route = legal fork move

Stage-2 keeps closed:

- tactic = PawnFork
- writer = TacticPawnFork
- pawn-specific speech key
- Material claim
- PromotionThreat claim
- PawnAdvance claim
- PawnCapture claim
- `PawnForkProof`
- `pawn_forks_two_targets`
- new StoryTable family
- new ExplanationPlan claim key
- new renderer template for PawnFork
- new LLM smoke path for PawnFork
- public route `200`
- production API
- public/user-facing LLM narration

Stage-2 rules:

- `TacticFork` writes the existing `Tactic.Fork` identity only.
- The writer must bind Story `anchor` to the attacker square after the move,
  not the pawn's before-square.
- The writer consumes complete `MultiTargetProof`; it does not recreate pawn
  route proof or create a Story from incomplete proof.
- EngineCheck may support, cap, or refute only the already written
  `Tactic.Fork` row. Refuted rows must not lead or speak.
- Pawn capture, pawn advance, passed pawn, promotion threat, material gain,
  tempo, best, only, forced, decisive, and winning meanings remain closed.

Completion standard: Stage-2 closes when `TacticFork.write` can write the
existing `Tactic.Fork` Story for a complete pawn-attacker `MultiTargetProof`
with scene `Tactic`, tactic `Fork`, no plan, attacking side, target-owner rival,
both target squares, anchor on the attacker's after-square, route on the legal
fork move, writer `TacticFork`, no StoryProof or MultiTargetProof failures, and
no new PawnFork writer, proof home, speech key, material claim, promotion-threat
claim, pawn-advance claim, pawn-capture claim, public route, production API, or
public/user-facing LLM narration.

## Stage-3 Negative Corpus

Stage-3 adds and confirms the negative corpus for pawn-attacker admission into
the existing `Tactic.Fork` path. It opens no new chess meaning and no new public
surface.

Stage-3 negative cases:

- illegal pawn move
- pseudo-legal pinned pawn move
- missing same-board proof
- missing exact after-board replay
- pawn does not attack target A after move
- pawn does not attack target B after move
- duplicated targets
- own-piece target
- king target
- one target only
- one reply saves both targets
- stronger rival reply refutes fork
- promotion move contamination
- promotion-threat contamination
- pawn capture / material-gain contamination
- PawnAdvance-only move
- PawnCapture-only move
- QueenHit-only attack
- Loose-only attack
- Skewer-looking line
- EngineCheck-only evidence
- source row saying pawn fork
- proofFailures text saying pawn fork

Stage-3 requirements:

- every Stage-3 negative case creates no public Fork claim.
- `Tactic.PawnFork` remains closed.
- no `TacticPawnFork` writer is added.
- no `PawnForkProof` proof home is added.
- `pawn_forks_two_targets` remains closed.
- wins material remains closed.
- wins piece remains closed.
- tempo remains closed.
- best / only / forced remains closed.

Stage-3 rules:

- The negative corpus hardens the existing `Tactic.Fork` boundary, not a new
  public chess meaning.
- Illegal pawn moves, pinned pawn pseudo-moves, missing same-board proof, and
  missing exact after-board replay stay silent because the proof route is not
  legally bound.
- One unattacked target, one target only, duplicated targets, own-piece targets,
  and king targets stay silent because they do not satisfy two distinct rival
  non-king targets both attacked after the move.
- A single reply that saves both targets, or a stronger rival reply, prevents
  the row from becoming a speaking Fork claim.
- Promotion move, promotion-threat, pawn-capture/material-gain,
  PawnAdvance-only, PawnCapture-only, QueenHit-only, Loose-only, and
  Skewer-looking overlaps stay in their own closed or existing proof homes; they
  do not become pawn-attacker Fork evidence by vocabulary.
- EngineCheck-only evidence, source rows, and proofFailures text cannot create a
  pawn-attacker Fork claim.

Completion standard: Stage-3 closed when docs and tests pinned
pawn-attacker negative corpus cases as silent or blocked, kept
`Tactic.PawnFork`, `TacticPawnFork`, `PawnForkProof`, `pawn_forks_two_targets`,
wins-material, wins-piece, tempo, best, only, and forced meanings closed,
created no separate public Story or speaking Fork claim from any negative case,
and opened no new StoryTable family, pawn-specific ExplanationPlan key,
renderer, LLM smoke path, public route `200`, production API, or
public/user-facing LLM narration.

## Stage-4 EngineCheck Reuse

Stage-4 opens only existing `EngineCheck` reuse for already written
pawn-attacker `Tactic.Fork` rows. It opens no new Story label, no PawnFork
family, no engine-owned public claim, and no public surface.

Stage-4 opens:

- existing `EngineCheck` attachment for complete pawn-attacker `Tactic.Fork`
  rows.

Stage-4 rules:

- EngineCheck cannot create Fork.
- EngineCheck cannot create PawnFork.
- Supports creates no new claim.
- Caps suppresses or bounds already selected Fork speech.
- Refutes blocks the Fork Story.
- Unknown creates no engine expression.
- EngineCheck must bind to the same legal route.
- EngineCheck must be story-bound evidence, not raw engine-only evidence.
- `TacticFork.withEngineCheck` requires complete StoryProof, complete
  `MultiTargetProof`, existing `Tactic.Fork`, writer `TacticFork`, same-board
  proof, and the same legal Fork route.

Stage-4 forbidden wording:

- engine says
- eval number
- raw PV
- best move
- only move
- forced
- wins material
- wins pawn
- pawn fork wins
- decisive
- winning

Stage-4 keeps closed:

- `Tactic.PawnFork`
- `PawnForkProof`
- `TacticPawnFork`
- `pawn_forks_two_targets`
- new StoryTable family
- new ExplanationPlan claim key
- new renderer template for PawnFork
- new LLM smoke path for PawnFork
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-4 closes when EngineCheck attaches only to complete
pawn-attacker `Tactic.Fork` rows, engine-only evidence and wrong-route evidence
cannot attach, Supports creates no new claim, Caps suppresses or bounds selected
Fork speech, Refutes blocks the Fork Story, Unknown creates no engine
expression, Stage-4 forbidden wording is present in the existing Fork
ExplanationPlan boundary, and no PawnFork Story label, proof home, writer,
speech key, StoryTable family, renderer, LLM smoke path, public route `200`,
production API, or public/user-facing LLM narration opens.

## Stage-5 StoryTable Integration

Stage-5 opens only StoryTable ordering for existing `Tactic.Fork` rows whose
attacker may be a pawn. It does not create a PawnFork family, public surface,
or new speech key.

Stage-5 collision targets:

- `Tactic.Fork`
- `Tactic.QueenHit`
- `Tactic.Loose`
- `Tactic.Hanging`
- `Tactic.Skewer`
- `Scene.Material`
- `Scene.PawnAdvance`
- `Scene.PawnCapture`
- `Scene.PromotionThreat`
- `Scene.Promotion`

Stage-5 rules:

- pawn-attacker fork is still `Tactic.Fork`.
- `Tactic.PawnFork` never appears as Lead, Support, Context, or Blocked public
  Story.
- actual material change remains `Scene.Material`.
- actual promotion remains `Scene.Promotion`.
- next-move promotion threat remains `Scene.PromotionThreat`.
- pawn advance remains `Scene.PawnAdvance`.
- pawn capture remains `Scene.PawnCapture`.
- queen-hit-only remains `Tactic.QueenHit`.
- loose-only remains `Tactic.Loose`.
- skewer-shaped line proof remains `Tactic.Skewer`.
- `StoryTable` may order already written rows, but it does not create Fork,
  PawnFork, Material, Promotion, PromotionThreat, PawnAdvance, PawnCapture,
  QueenHit, Loose, Hanging, or Skewer rows from another row's evidence.

Rows that must not speak:

- Support
- Context
- Blocked
- capped
- refuted
- non-Lead

Stage-5 keeps closed:

- `Tactic.PawnFork`
- `PawnForkProof`
- `TacticPawnFork`
- `pawn_forks_two_targets`
- new StoryTable family
- new ExplanationPlan claim key
- new renderer template for PawnFork
- new LLM smoke path for PawnFork
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-5 closes when StoryTable orders pawn-attacker Fork
rows as existing `Tactic.Fork`, never emits `Tactic.PawnFork` as Lead, Support,
Context, or Blocked, keeps all collision targets in their own Story identities,
keeps Support, Context, Blocked, capped, refuted, and non-Lead rows from
speaking, and opens no PawnFork proof home, writer, speech key, StoryTable
family, ExplanationPlan claim key, renderer template, LLM smoke path, public
route `200`, production API, or public/user-facing LLM narration.

## Stage-6 ExplanationPlan

Stage-6 opens only existing Fork `ExplanationPlan` lowering for selected
uncapped Lead `Tactic.Fork` rows whose attacker may be a pawn.

Allowed claim key:

- existing `forks_two_targets` only

Stage-6 must not add:

- `pawn_forks_two_targets`
- `pawn_fork`
- new PawnFork claim key
- promotion claim key
- material claim key
- tempo claim key

Input boundary:

- selected Lead Verdict
- uncapped
- not refuted
- Story tactic = `Tactic.Fork`
- writer = `TacticFork`
- complete `StoryProof`
- complete `MultiTargetProof`
- target and secondaryTarget present
- route present
- evidenceLine = route only

Stage-6 rules:

- `ExplanationPlan` does not prove Fork.
- `ExplanationPlan` does not repair missing pawn route proof.
- Support, Context, Blocked, capped, and refuted rows stay silent.

Stage-6 keeps closed:

- `Tactic.PawnFork`
- `PawnForkProof`
- `TacticPawnFork`
- `pawn_forks_two_targets`
- `pawn_fork`
- new PawnFork claim key
- promotion claim key
- material claim key
- tempo claim key
- new renderer template for PawnFork
- new LLM smoke path for PawnFork
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-6 closes when `ExplanationPlan.fromSelected` lowers
only selected uncapped non-refuted Lead `Tactic.Fork` Verdicts with
`TacticFork`, complete `StoryProof`, complete `MultiTargetProof`, bound target,
secondaryTarget, route, and evidenceLine equal to route; emits only existing
`forks_two_targets`; does not repair missing pawn route proof; keeps Support,
Context, Blocked, capped, and refuted rows silent; and opens no PawnFork proof
home, writer, claim key, renderer template, LLM smoke path, public route `200`,
production API, or public/user-facing narration.

## Stage-7 DeterministicRenderer

Stage-7 opens only existing Fork renderer path use.
existing Fork renderer path may render pawn-attacker Fork for selected
`Tactic.Fork` rows that already lowered through the existing Fork
`ExplanationPlan`.

Renderer input:

- `ExplanationPlan` only

Allowed:

- existing Fork bounded wording only
- existing `forks_two_targets` claim key only

Do not add:

- PawnFork renderer template
- pawn-specific public template
- wins material wording
- wins piece wording
- promotion wording
- tempo wording

Forbidden renderer input:

- raw `Story`
- raw `MultiTargetProof`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`
- source rows

Stage-7 rules:

- `DeterministicRenderer` may render a pawn-attacker Fork only from a valid
  Stage-6 Fork `ExplanationPlan`.
- `DeterministicRenderer` does not prove Fork.
- `DeterministicRenderer` does not read or repair missing pawn route proof.
- Renderer emits no text for Support, Context, Blocked, capped, or refuted rows.
- Renderer emits no PawnFork text.

Stage-7 keeps closed:

- `Tactic.PawnFork`
- `PawnForkProof`
- `TacticPawnFork`
- `pawn_forks_two_targets`
- `pawn_fork`
- PawnFork renderer template
- pawn-specific public template
- promotion wording
- material-gain wording
- tempo wording
- new LLM smoke path for PawnFork
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-7 closes when `DeterministicRenderer.fromPlan`
renders pawn-attacker Fork only through an existing Fork `ExplanationPlan`,
uses only bounded Fork wording and `forks_two_targets`, accepts no raw Story,
raw `MultiTargetProof`, `BoardFacts`, `EngineCheck`, raw PV, `proofFailures`,
or source rows, keeps Support, Context, Blocked, capped, and refuted rows
silent, emits no PawnFork text, and opens no PawnFork renderer template,
LLM smoke path, public route `200`, production API, or public/user-facing
narration.

## Stage-8 LLM Smoke

Stage-8 opens only existing Fork LLM smoke for rendered Fork text.
existing Fork LLM smoke may rephrase rendered Fork text for pawn-attacker Fork rows that
passed Stage-7.

Allowed input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Allowed claimKey:

- existing `forks_two_targets` only

LLM must not add:

- pawn fork as separate claim
- wins material
- wins piece
- promotion threat
- pawn advance
- tempo
- best move
- only move
- forced move
- decisive
- winning
- engine line
- new move
- new variation

Stage-8 rules:

- LLM only polishes.
- Verifier rejects overclaim.
- public/user-facing LLM narration remains closed.
- LLM smoke does not prove Fork.
- LLM smoke does not read or repair missing pawn route proof.

Stage-8 keeps closed:

- `Tactic.PawnFork`
- `PawnForkProof`
- `TacticPawnFork`
- `pawn_forks_two_targets`
- `pawn_fork`
- PawnFork LLM smoke path
- public/user-facing LLM narration
- public route `200`
- production API

Completion standard: Stage-8 closes when existing Fork LLM smoke may receive
only renderedText, claimKey, strength, forbidden wording, and
`Rephrase only. Do not add chess facts.` for selected pawn-attacker Fork
RenderedLine rows; accepts only existing `forks_two_targets`; rejects pawn fork
as a separate claim, wins-material, wins-piece, promotion-threat, pawn-advance,
tempo, best, only, forced-move, decisive, winning, engine-line, new-move, and
new-variation additions; keeps public/user-facing LLM narration closed; and
opens no PawnFork proof home, writer, claim key, renderer template, LLM smoke
path, public route `200`, or production API.

## Stage-9 Fork-PawnAttacker Admission Closeout

Stage-9 opens no new chess meaning beyond existing `Tactic.Fork`. It closes
only pawn attacker admission into existing Fork. Stage-9 must close only pawn
attacker admission into existing Fork.

close only pawn attacker admission into existing Fork.

Authority audit:

- `MultiTargetProof` remains proof home.
- `Tactic.Fork` remains Story label.
- `TacticFork` remains writer.
- existing Fork claim key remains speech key.
- `Tactic.PawnFork` remains closed.
- `PawnForkProof` does not exist.
- `TacticPawnFork` does not exist.
- `pawn_forks_two_targets` does not exist.

Duplication audit:

- pawn-attacker Fork is not a separate public Story.
- pawn-attacker Fork is not PawnAdvance.
- pawn-attacker Fork is not PawnCapture.
- pawn-attacker Fork is not PromotionThreat.
- pawn-attacker Fork is not Promotion.
- pawn-attacker Fork is not Material.
- pawn-attacker Fork is not QueenHit-only.
- pawn-attacker Fork is not Loose-only.
- pawn-attacker Fork is not Tempo.

Still closed:

- public `Tactic.PawnFork`
- pawn-specific speech key
- promotion threat
- material gain
- wins piece/material
- tempo
- best / only / forced
- decisive / winning
- public route `200`
- production API
- public/user-facing LLM narration

Stage-9 verification:

- targeted Fork-PawnAttacker Admission tests
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Completion standard: Stage-9 closes when pawn-attacker admission remains only
existing `Tactic.Fork`; `MultiTargetProof`, `Tactic.Fork`, `TacticFork`, and
the existing Fork claim key remain the single proof, Story, writer, and speech
chain; `Tactic.PawnFork`, `PawnForkProof`, `TacticPawnFork`, and
`pawn_forks_two_targets` remain closed or absent; sibling meanings remain in
their own homes; public route `200`, production API, and public/user-facing LLM
narration remain closed; targeted admission tests, foundation tests, docs
authority tests, and `git diff --check` pass.

## Material-0 Scene.Material Charter

Material-0 opens only the charter for the first narrow `Scene.Material` slice.
It does not open a runtime writer, StoryTable role rule, ExplanationPlan
mapping, deterministic renderer text, LLM smoke, public route `200`,
production API, `Scene.Defense`, Plan, Strategy, or any winning claim.

Core sentences:

`Material is a scene, not a tactic.`
`CaptureResult` or `ExchangeResult` is the proof home.
`Scene.Material` is the Story label.
`material_change` is the speech claim.

First scope:

- simple capture or exchange result
- same-board proof
- legal line
- bounded recapture or exchange check
- known material result
- no tactic label required

`Scene.Material` may label a proof-backed material change without calling that
change a tactic. `CaptureResult` or `ExchangeResult` supplies the proof. The
Material Story supplies the scene label. The speech claim remains a structured
claim key only and starts as `material_change` when the downstream
ExplanationPlan slice opens.

Forbidden Material-0 meanings:

- winning
- decisive
- blunder
- conversion
- best move
- forced
- no counterplay
- engine says
- full evaluation claim

Material gain is not winning. A positive bounded material result may identify a
material change only. It must not become advantage, evaluation, conversion,
decisive result, blunder diagnosis, best-move claim, forced-line claim,
no-counterplay claim, or engine wording.

`Scene.Material` is not another name for `Tactic.Hanging`. Hanging remains a
tactic Story whose permission comes from the named `TacticHanging` writer and
positive `CaptureResult`. Material is a scene Story whose permission requires a
separate Material writer slice. `CaptureResult` remains proof evidence, not a
Story label and not public speech.

Completion standard: `Scene.Material` must not become another name for `Tactic.Hanging` or `CaptureResult`.

## Material-1 Proof Home Decision

Material-1 decides the proof home for the first narrow Material scope.

Decision: the first `Scene.Material` scope reuses `CaptureResult`.

Reasons:

- the first scope is centered on one legal capture line
- `CaptureResult` already records same-board proof, legal capture, capturer,
  target, captured value, recapture candidates, bounded material result, and
  missing evidence
- immediate bounded recapture is still capture proof in this scope
- adding `ExchangeResult` now would split the same simple capture material
  meaning across two proof homes

`CaptureResult is capture proof.`
`ExchangeResult is bounded exchange proof.`
`Scene.Material is not proof.`

Do not create `ExchangeResult` in Material-1.

A new `ExchangeResult` proof home opens only when Material needs a bounded multi-move exchange sequence that `CaptureResult` cannot own without overloading capture meaning.

Material-1 keeps one material meaning in one proof home:

- single legal capture and immediate bounded recapture use `CaptureResult`
- multi-move exchange sequence is outside the first proof scope
- `Scene.Material` may consume only the selected proof-backed result after the
  Material writer slice opens
- `Scene.Material` does not repair missing `CaptureResult` evidence
- `Scene.Material` does not read raw BoardFacts, raw BoardMood, raw engine
  evidence, proofFailures text, source rows, renderer wording, or LLM wording
  to create proof

Completion standard: one material meaning has one proof home.

## Material-2 Material / Exchange Proof Shape

Material-2 opens only the bounded material proof shape. It does not open a
Material writer, StoryTable role rule, ExplanationPlan mapping, deterministic
renderer text, LLM smoke, public route `200`, production API, `Scene.Defense`,
Plan, Strategy, or any public sentence.

The first proof shape lives in `CaptureResult`. It may record exchange-shaped
fields for the simple capture and immediate bounded recapture scope, but it
does not create `ExchangeResult`.

Required proof information:

- side
- legal line
- captured pieces
- recapture candidates
- bounded exchange sequence
- material result
- same-board proof
- missing evidence

Allowed Material-2 proof outcomes:

- line leaves White up material
- line leaves Black up material
- exchange result is known

Forbidden Material-2 meanings:

- winning position
- decisive advantage
- conversion
- blunder
- best move
- forced line

`CaptureResult` may calculate the bounded material result for the checked
capture line. `capturedPieces` and `boundedExchangeSequence` are proof-shape
fields only. They do not create a public Story, a Material writer, a speech
claim, a renderer sentence, or LLM input.

Unknown material, illegal capture, own-piece target, king target, missing
same-board proof, or missing legal line stays missing evidence. The proof shape
may report the missing coordinate internally, but it must not repair the proof
or produce text.

Completion standard: the proof calculates material result, but it does not create a public Story or sentence.

## Material-3 Scene.Material Writer

Material-3 opens the named `SceneMaterial` writer only. It does not open
ExplanationPlan mapping, deterministic renderer text, LLM smoke, public route
`200`, production API, `Scene.Defense`, Plan, Strategy, winning, decisive,
blunder, conversion, best-move, forced, no-counterplay, engine-says, or
full-evaluation claims.

`SceneMaterial` may create a Story only when:

- scene is `Scene.Material`
- StoryProof is complete
- material proof is complete
- same-board proof is present
- legal line is present
- material result is known
- writer is `StoryWriter.SceneMaterial`
- EngineCheck does not `Refutes`

The first allowed Material meanings are:

- this line changes material balance
- this exchange leaves one side ahead in material

Forbidden Material-3 meanings:

- winning
- decisive
- blunder
- conversion
- best move
- forced
- no counterplay
- engine says

The writer consumes `CaptureResult` as proof. It does not make
`CaptureResult` public speech, and it does not turn Material into
`Tactic.Hanging`. A Hanging Story still requires the `TacticHanging` writer and
the tactic label. A Material Story requires the `SceneMaterial` writer and no
tactic label.

EngineCheck may support, cap, or refute only an existing Material Story.
`Supports` creates no engine claim. `Caps` creates only the internal
strength-limited diagnostic. `Refutes` blocks the Material Story. Engine eval
and engine line data still cannot create Material by themselves.

Support, Context, ExplanationPlan, renderer, and LLM wording for Material stay
closed in Material-3.

Completion standard: one narrow Material Story with proof can enter StoryTable.

## Material-4 Material Negative Corpus

Material-4 opens only the Material negative corpus. It does not open
ExplanationPlan mapping, deterministic renderer text, LLM smoke, public route
`200`, production API, `Scene.Defense`, Plan, Strategy, winning, decisive,
blunder, conversion, best-move, forced, no-counterplay, engine-says, or
full-evaluation claims.

Material-looking false positives:

- legal line missing
- same-board proof missing
- capture exists but bounded recapture erases the material result
- exchange result unclear
- target is king
- material result is zero
- EngineCheck `Refutes`
- StoryProof incomplete
- material proof incomplete
- tactic writer tries to speak Material
- Hanging tries to auto-duplicate as Material
- Fork tries to auto-duplicate as Material
- high Proof score only

If `SceneMaterial.write` cannot build a complete proof-backed Story, no
Material Story is created. If a forged or duplicated Material-looking row
enters StoryTable, it must not become Lead. Rows with high Proof scores but no
named `SceneMaterial` writer and complete bounded material proof are Blocked.

Hanging and Fork do not auto-create Material speech. A tactic Story may carry
material-shaped evidence, but it remains in its tactic family unless the named
`SceneMaterial` writer independently creates a complete `Scene.Material`
Story. A tactic writer must not speak Material by changing only the scene
label.

Completion standard: material-looking rows without bounded material proof become no Lead or Blocked.

## Material-5 EngineCheck for Scene.Material

Material-5 opens only existing `EngineCheck` reuse for `Scene.Material`. It
does not open a new engine proof type, ExplanationPlan mapping, deterministic
renderer text, LLM smoke, public route `200`, production API, `Scene.Defense`,
Plan, Strategy, winning, decisive, blunder, conversion, best-move, forced,
no-counterplay, engine-says, or full-evaluation claims.

Allowed Material-5 statuses:

- `Unknown`
- `Supports`
- `Caps`
- `Refutes`

Material `EngineCheck` attachment requires:

- Material Story already exists
- same-board proof
- same Story route
- same legal line
- fresh or depth evidence

Forbidden Material-5 meanings and shortcuts:

- engine creates Material Story
- engine eval becomes public truth
- PV becomes explanation
- best move explanation
- winning claim
- `MaterialEngineCheck` duplicate type

`EngineCheck.fromStory` is the Material attachment path. Raw
`EngineCheck.fromEvidence`, eval-only evidence, PV-only evidence,
route-mismatched evidence, different-board evidence, stale evidence, and
depth-missing evidence stay diagnostic only and cannot attach to
`Scene.Material`.

`Supports` creates no engine claim. `Caps` creates only the internal
strength-limited diagnostic. `Refutes` blocks the existing Material Story.
Engine eval and PV remain evidence only and must not become public truth or
explanation.

Completion standard: `EngineCheck` may support, cap, or refute an existing Material Story, but it must not create Material.

## Material-6 StoryTable Integration

Material-6 opens only StoryTable integration for existing Hanging, Fork, and Material rows.
It does not open Material Story creation by StoryTable, ExplanationPlan
mapping, deterministic renderer text, LLM smoke, public route `200`,
production API, `Scene.Defense`, Plan, Strategy, winning, decisive, blunder,
conversion, best-move, forced, no-counterplay, engine-says, or
full-evaluation claims.

Allowed Material-6 StoryTable roles and inputs:

- Lead
- Support
- Context
- Blocked
- deterministic ordering

Material-6 StoryTable rules:

- Refuted Material becomes Blocked
- incomplete Material becomes Blocked
- writerless Material becomes Blocked
- Material without material proof becomes Blocked
- Hanging, Fork, and Material can compete for Lead
- Material with the same route, target, and material result as positive Hanging orders behind Hanging
- Support and Context are not sentences

Forbidden Material-6 meanings and shortcuts:

- StoryTable creates Material
- raw engine eval ranks Material
- material proof text becomes public
- renderer wording affects order
- Material silently opens conversion or winning

StoryTable may order only existing proof-backed Story rows. It may use role
eligibility, publicStrength, Story identity, writer presence, and blocked
status. It must not create Material, read raw engine eval as ranking
authority, speak material proof text, use renderer wording, or upgrade Material
into conversion, winning, decisive, blunder, best-move, forced,
no-counterplay, or engine-says meaning.

Completion standard: StoryTable deterministically orders the three Story families without creating new chess meaning.

## Material-7 ExplanationPlan for Scene.Material

Material-7 opens only ExplanationPlan mapping for selected `Scene.Material` Verdicts.
It does not open Material renderer text, LLM smoke, public route `200`,
production API, `Scene.Defense`, Plan, Strategy, winning, decisive, blunder,
conversion, best-move, forced, no-counterplay, engine-says, or
full-evaluation claims.

Allowed Material-7 input:

- selected Verdict only

Forbidden Material-7 inputs:

- material proof directly
- `CaptureResult`
- `ExchangeResult`
- `EngineCheck`
- `BoardFacts`
- raw PV
- proofFailures
- source row

Allowed Material-7 claim keys:

- `material_balance_changes`
- `line_leaves_material_gain`
- `exchange_leaves_side_ahead`

The first emitted Material claim key is `material_balance_changes`.

Forbidden Material-7 claim keys:

- `winning_position`
- `decisive_advantage`
- `conversion`
- `blunder`
- `best_move`
- `forced_win`
- `no_counterplay`

The Material ExplanationPlan may copy selected Verdict identity into the speech
boundary: role, scene, side, target, anchor, route, evidenceLine, bounded
strength, relation diagnostics, and forbidden wording. It must not read
material proof directly, `CaptureResult`, `ExchangeResult`, `EngineCheck`,
`BoardFacts`, raw PV, proofFailures, or source row data to create a claim.

Only an uncapped selected Lead Material Verdict may carry the first emitted
Material claim key.
Support, Context, Blocked, capped, and engine-refuted Material plans create no standalone public claim.
Material-7 does not open Material renderer text, LLM smoke, public route `200`, production API, or public sentence output.

Completion standard: Material ExplanationPlan creates no meaning stronger than the selected Verdict.

## Material-8 Deterministic Renderer

Material-8 opens only deterministic renderer text for selected `Scene.Material` ExplanationPlan.
It does not open LLM smoke, public route `200`, production API,
`Scene.Defense`, Plan, Strategy, winning, decisive, blunder, conversion,
best-move, forced, no-counterplay, engine-says, technically-winning, or
full-evaluation claims.

Allowed Material-8 input:

- ExplanationPlan only

First Material-8 templates:

`This line leaves White ahead in material.`
`After {route}, White comes out ahead in material.`

The route template may use only the route, side, allowed claim key, strength,
and forbidden wording already present in the selected Material
ExplanationPlan. It must not read Material Story, raw Verdict, material proof,
`CaptureResult`, `ExchangeResult`, `EngineCheck`, `BoardFacts`, raw PV,
proofFailures, source row data, or renderer wording from any earlier row.

Forbidden Material-8 wording:

- winning
- decisive
- blunder
- forced
- best move
- no counterplay
- engine says
- conversion
- technically winning

Only a Lead, non-debug, bounded Material ExplanationPlan with
`material_balance_changes`, route, side, and forbidden wording present may
render deterministic Material text. Support, Context, Blocked, capped,
engine-refuted, no-claim, wrong-claim, debug-only, missing-route, or
forbidden-wording Material plans render no standalone text.

Material-8 does not open LLM smoke, public/user-facing narration, public route
`200`, production API, pedagogy, engine PV commentary, best-move explanation,
winning-position wording, decisive wording, conversion wording, or sibling
Story families.

Completion standard: Renderer text is no stronger than the Material ExplanationPlan.

## Material-9 LLM Smoke

Material-9 opens only LLM smoke for selected Material ExplanationPlan and RenderedLine.
It does not open production API validation, public/user-facing LLM narration,
public route `200`, streaming, pedagogy, `Scene.Defense`, Plan, Strategy,
engine PV commentary, best-move explanation, winning-position wording,
decisive wording, forced wording, blunder wording, conversion wording,
engine-says wording, full-evaluation claims, or sibling Story families.

Allowed Material-9 input:

- ExplanationPlan
- RenderedLine

8B Material Codex CLI input:

- renderedText
- claimKey
- strength
- forbidden wording
- instruction: Rephrase only. Do not add chess facts.

Forbidden Material-9 input:

- raw Verdict
- Story
- material proof
- CaptureResult
- ExchangeResult
- EngineCheck
- BoardFacts
- engine eval
- raw PV
- proofFailures

Material LLM smoke must reject output that adds:

- new move
- new line
- new tactic
- new plan
- engine mention
- winning, decisive, forced, blunder, or best-move wording
- conversion claim
- stronger claim

The Material smoke prompt may receive only renderedText, claimKey, strength,
forbidden wording, and the instruction `Rephrase only. Do not add chess facts.`
It must not receive raw Verdict, Story, material proof, `CaptureResult`,
`ExchangeResult`, `EngineCheck`, `BoardFacts`, engine eval, raw PV,
proofFailures, or source row data.

Material-9 does not open public/user-facing LLM narration, public route `200`,
production API, pedagogy, engine PV commentary, best-move explanation,
winning, decisive, forced, blunder, conversion, no-counterplay, or
full-evaluation claims.

Completion standard: LLM smoke does not strengthen Material text.

## Material Slice Closeout Pass

Material slice closeout opens no new chess meaning beyond the narrow `Scene.Material` vertical slice.

Scope audit:

- opened: `Scene.Material` only
- still closed: `Scene.Defense`
- still closed: Plan
- still closed: Strategy
- still closed: Conversion
- still closed: Blunder
- still closed: public route `200`
- still closed: production API
- still closed: public/user-facing LLM narration

Authority audit:

- `CaptureResult` owns simple capture and immediate bounded recapture material proof.
- `ExchangeResult` remains unopened and is reserved for bounded multi-move exchange proof outside this slice if needed.
- `StoryProof` owns identity completeness, same-board proof, and legal-line binding.
- `EngineCheck` supports, caps, or refutes only an existing Material Story.
- `StoryTable` orders existing Material rows but creates no Material Story or material proof.
- `Scene.Material` owns the Story label only.
- `material_change` is speech-claim vocabulary; current emitted key is `material_balance_changes`.

Negative corpus audit:

- material-looking false positives produce no Lead or Blocked.
- legal-line-missing Material is silent or Blocked.
- same-board-proof-missing Material is silent or Blocked.
- recapture-cancelled Material is silent or Blocked.
- unclear exchange result is silent or Blocked.
- king target is silent or Blocked.
- zero material result is silent or Blocked.
- engine-refuted Material is Blocked.
- incomplete StoryProof is Blocked.
- incomplete material proof is silent or Blocked.
- tactic writers do not duplicate Material speech.
- Hanging does not automatically duplicate as Material.
- Fork does not automatically duplicate as Material.
- high Proof score alone remains insufficient.

Cleanup and consolidation audit:

- no `ExchangeResult` type was created in this slice.
- no `MaterialEngineCheck` type was created.
- no new markdown authority document was created for Material.
- no new StoryTable creation path was added for Material.
- no direct material proof, `CaptureResult`, `ExchangeResult`, `EngineCheck`,
  `BoardFacts`, raw PV, proofFailures, or source-row input reaches
  ExplanationPlan, Renderer, or LLM smoke.
- Material proof text does not become renderer or LLM input.

Shared skeleton audit:

- Material reuses proof home -> Story writer -> EngineCheck -> StoryTable -> ExplanationPlan -> Renderer -> LLM smoke.
- Reuse the skeleton before adding a new one.
- no second Story writer path, EngineCheck type, StoryTable route, ExplanationPlan input, renderer input, or LLM prompt shape was added.
- if `ExchangeResult` opens in its own slice, it must state how bounded multi-move exchange proof differs from `CaptureResult`.

Home map:

- `CaptureResult`: proof home for simple capture and immediate bounded recapture material result.
- `ExchangeResult`: unopened reserved proof-home name for bounded multi-move exchange proof outside this slice.
- `Scene.Material`: Story label for bounded material-result meaning.
- `material_change`: speech-claim vocabulary; current runtime claim key is `material_balance_changes`.

Next-stage handoff:

- next named slice remains `Scene.Defense`.
- Material does not open Defense, Conversion, Winning, Plan, Strategy, or Blunder.
- Material does not open best move, forced line, no counterplay, decisive,
  conversion, or full engine PV commentary.

`Material is a scene, not a tactic.`
`Material gain is not winning.`
`One chess meaning, one home.`

Completion standard: Material slice is closed as a narrow bounded material-result Story label.

## Defense-0 Scene.Defense Charter

Defense-0 opens only the charter for the first narrow `Scene.Defense` slice.
It does not open a Defense writer, proof sidecar implementation, StoryTable
integration, ExplanationPlan mapping, deterministic renderer, LLM smoke,
public route `200`, production API, king safety, mate defense, strategic
defense, prophylaxis, winning, conversion, best-move, only-move, refutation,
or no-counterplay claim.

Defense requires a threat.

ThreatProof proves what must be stopped.

DefenseProof proves how the move stops it.

Defense is not no-counterplay.

First Defense scope:

- attacked piece exists
- opponent has an immediate material threat
- the threat is same-board legal
- the defended move removes, guards, or saves the target
- material loss is prevented in a bounded way
- no claim of best move or only move

Core distinction:

`ThreatProof = what must be stopped`
`DefenseProof = how it is stopped`
`Scene.Defense = Story label`
`defends_piece / prevents_material_loss = speech claim`

Forbidden Defense-0 claims:

- only move
- best move
- no counterplay
- refutes the attack
- solves the position
- king safety
- mate defense
- strategic defense
- prophylaxis
- winning
- conversion

Completion standard: `Scene.Defense` must not become another name for a good move or for stopping all counterplay.

## Defense-1 ThreatProof

Defense-1 opens only `ThreatProof`.

ThreatProof proves what must be stopped.

ThreatProof does not create a Defense Story.

ThreatProof does not create a public claim.

DefenseProof, the named Scene.Defense writer, StoryTable integration,
ExplanationPlan, renderer, LLM smoke, public route `200`, and production API
remain closed.

Required ThreatProof fields:

- rival side
- threatened target
- attacking piece
- legal threat line
- target value
- material loss if unanswered
- same-board proof
- missing evidence

Allowed Defense-1 meanings:

- rival can capture the target
- target is attacked
- capture would cause material loss
- threat is immediate and legal

Forbidden Defense-1 meanings:

- opponent has an attack
- king is unsafe
- no counterplay
- mate threat
- long-term pressure
- strategic threat
- engine says this is a threat

Completion standard: ThreatProof proves what must be stopped, but it does not create a Defense Story or public claim.

## Defense-2 DefenseProof

Defense-2 opens only `DefenseProof`.

DefenseProof proves how a specific move stops a specific ThreatProof.

DefenseProof does not create a Defense Story.

DefenseProof does not create a public claim.

Allowed Defense-2 move types:

1. target moves away
2. target becomes guarded
3. attacker line is blocked or attacker is captured

Required DefenseProof fields:

- defending side
- defense move
- defended target
- original threat
- after-defense target status
- material loss prevented
- same-board proof
- missing evidence

Allowed Defense-2 meanings:

- the target is no longer capturable for gain
- the target is defended after the move
- the attacker's line is blocked
- the attacker is captured

Forbidden Defense-2 meanings:

- solves the position
- refutes the attack
- stops all threats
- only move
- best defense
- no counterplay
- king safety
- mate defense

Completion standard: DefenseProof proves whether a specific threat is stopped, but it does not create a Defense Story or public claim.

## Defense-3 SceneDefense Writer

Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story.

Required SceneDefense writer evidence:

- scene = Defense
- StoryProof complete
- ThreatProof complete
- DefenseProof complete
- same-board proof present
- defense move legal
- protected target identified
- material loss prevented
- writer = SceneDefense
- EngineCheck does not Refute

First allowed Defense-3 meanings:

- this move defends the attacked piece
- this move prevents immediate material loss

Forbidden Defense-3 meanings:

- only move
- best move
- refutes attack
- stops counterplay
- solves position
- king safe
- mate stopped
- winning
- decisive

Completion standard: one narrow proof-backed Defense Story can enter StoryTable.

## Defense-4 Defense Negative Corpus

Defense-4 opens only the Defense negative corpus.

Defense-looking false positives must stay silent without complete ThreatProof and complete DefenseProof.

Defense-4 negative cases:

- no actual threat
- threat is illegal
- attacked piece is already adequately defended
- defense move does not affect the target
- defense move guards wrong piece
- defense move still loses material
- defense move allows equivalent recapture
- defense only looks like prophylaxis
- defense is actually a tactic / material gain
- king safety claim tries to enter
- mate defense tries to enter
- only-move claim tries to enter
- StoryProof incomplete
- ThreatProof incomplete
- DefenseProof incomplete
- EngineCheck Refutes
- high Proof score only

Completion standard: defense-looking rows have no Lead or are Blocked unless ThreatProof and DefenseProof are complete.

## Defense-5 EngineCheck for Scene.Defense

Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories.

Allowed Defense-5 EngineCheck statuses:

- Unknown
- Supports
- Caps
- Refutes

Required Defense-5 EngineCheck evidence:

- Defense Story already exists
- same-board proof
- same Story route
- same legal line
- fresh/depth evidence

Forbidden Defense-5 meanings and shortcuts:

- engine creates Defense Story
- engine eval becomes public truth
- PV becomes explanation
- best move explanation
- only move claim
- refutes attack claim
- DefenseEngineCheck duplicate type

Completion standard: EngineCheck may support, cap, or refute an existing Defense Story, but it does not create Defense.

## Defense-6 StoryTable Integration

Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows.

Allowed Defense-6 roles and behavior:

- Lead
- Support
- Context
- Blocked
- deterministic ordering

Defense-6 StoryTable rules:

- Refuted Defense -> Blocked
- incomplete Defense -> Blocked
- writerless Defense -> Blocked
- Defense without ThreatProof -> Blocked
- Defense without DefenseProof -> Blocked
- Defense can compete for Lead only if it has complete proof
- Support / Context are not sentences

Forbidden Defense-6 shortcuts:

- StoryTable creates Defense
- raw engine eval ranks Defense
- Defense proof text becomes public
- renderer wording affects order
- Defense silently opens only move or no counterplay

Completion standard: StoryTable deterministically orders Hanging, Fork, Material, and Defense without creating new chess meaning.

## Defense-7 ExplanationPlan for Scene.Defense

Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts.

Defense-7 allowed input:

- selected Verdict only

Defense-7 forbidden inputs:

- ThreatProof directly
- DefenseProof directly
- EngineCheck
- BoardFacts
- raw PV
- proofFailures
- source row

Defense-7 first allowed claim keys:

- defends_piece
- prevents_material_loss
- protects_target

Defense-7 forbidden claim keys:

- only_move
- best_defense
- refutes_attack
- stops_counterplay
- solves_position
- king_safe
- mate_defense
- no_counterplay

Completion standard: Defense ExplanationPlan creates no meaning stronger than the selected Verdict.

## Defense-8 Deterministic Renderer

Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan.

Defense-8 allowed renderer input:

- ExplanationPlan only

Defense-8 first deterministic templates:

- `{route} defends the piece on {target}.`
- `{route} prevents the piece on {target} from being lost immediately.`

Defense-8 forbidden renderer wording:

- only move
- best move
- refutes the attack
- stops all counterplay
- solves the position
- king is safe
- mate is stopped
- winning
- decisive
- forced

Completion standard: Renderer text is no stronger than the Defense ExplanationPlan.

## Defense-9 LLM Smoke

Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine.

Defense-9 allowed LLM smoke input:

- ExplanationPlan
- RenderedLine

Defense-9 Codex CLI smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- instruction: Rephrase only. Do not add chess facts.

Defense-9 forbidden LLM smoke inputs:

- raw Verdict
- Story
- ThreatProof
- DefenseProof
- EngineCheck
- BoardFacts
- engine eval
- raw PV
- proofFailures

Defense-9 smoke rejection checks:

- no new move
- no new line
- no new tactic
- no new plan
- no engine mention
- no only move
- no best move
- no no-counterplay
- no king safety
- no mate defense
- no refutes-attack wording
- no stronger claim

Completion standard: LLM smoke does not make Defense text stronger.

## Defense Slice Closeout Pass

Defense Slice Closeout opens no new chess meaning beyond the narrow `Scene.Defense` vertical slice.

Defense closeout scope audit:

- only `Scene.Defense` opened
- king safety remains closed
- mate defense remains closed
- Plan remains closed
- Strategy remains closed
- Counterplay remains closed beyond existing EngineCheck Caps
- Prophylaxis remains closed

Defense closeout authority audit:

- ThreatProof owns what must be stopped
- DefenseProof owns how the move stops it
- StoryProof owns same-board Story identity evidence
- EngineCheck supports, caps, or refutes an existing Defense Story only
- StoryTable arbitrates roles without creating Defense

Defense closeout negative corpus audit: defense-looking false positives stay silent without complete ThreatProof and DefenseProof.

Defense closeout shared skeleton audit: charter, proof home, named writer, negative corpus, EngineCheck reuse, StoryTable integration, ExplanationPlan, deterministic renderer, LLM smoke, and closeout reused the existing vertical-slice skeleton.

Defense closeout cleanup audit: `ThreatProof`, `DefenseProof`, `Scene.Defense`, and `defends_piece` each have one home.

Defense closeout real-game smoke: Fischer-Spassky 1972 game 6 after 6...h6, 7.Bh4 is covered as an attacked-piece defense smoke.

Defense closeout next-stage handoff: next candidates remain line-based tactic or king-forcing tactic; Defense does not open king safety, mate defense, or counterplay.

Defense requires a threat.
Defense is not no-counterplay.
Reuse the skeleton before adding a new one.
One chess meaning, one home.

Completion standard: Defense closes as a narrow proof-backed attacked-piece material-loss defense slice only.

## Middlegame Interaction Hardening

### MIH-0 Charter

Middlegame Interaction Hardening opens no chess meaning. It stress-tests already-open meanings.

MIH-0 opens only interaction hardening among existing Hanging, Fork, Material, and Defense rows.

MIH-0 opens complex middlegame fixture based role stability checks.

MIH-0 checks selected Verdict, ExplanationPlan, and renderer/LLM smoke boundary stability without opening new speech.

MIH-0 may apply only the minimum StoryTable ordering fix if an existing ordering bug is exposed.

Allowed MIH-0 rows:

- Tactic.Hanging
- Tactic.Fork
- Scene.Material
- Scene.Defense

MIH-0 hardening matrix:

- Material vs Defense
- Hanging vs Defense
- Fork vs Defense
- Hanging vs Material
- Hanging vs Fork
- EngineCheck Supports / Caps / Refutes across these rows

MIH-0 forbidden openings:

- Line/Ray
- RemoveGuard
- Pin
- Skewer
- Pawn
- BackRank
- Plan
- Strategy
- Initiative
- Pressure
- public route 200
- production API
- public/user-facing LLM narration
- new Story family
- new proof home
- new renderer wording

Material vs Defense is the first and highest-risk case because `Scene.Defense` prevents immediate material loss while `Scene.Material` describes material balance changing now.

Hardening role rules:

- If the move actually wins or changes material now, `Scene.Material` may lead.
- If the move prevents an immediate material loss, `Scene.Defense` may lead.
- If both are present on the same-board route, same-board outcome decides:
- actual material gain/change outranks Defense as Material
- prevented immediate loss without actual material gain/change remains Defense
- speculative material loss remains Blocked

Authority boundary:

- StoryTable must not create Defense, Material, Hanging, or Fork rows during this hardening.
- EngineCheck may support, cap, or refute existing rows only.
- Raw engine eval and renderer wording must not rank rows.
- Support and Context remain relation roles, not sentences.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

### MIH-1 Fixture Map

MIH-1 opens only complex middlegame test fixtures for already-open Hanging, Fork, Material, and Defense rows.

Each MIH-1 fixture must state:

- same-board FEN
- side to move
- legal fixture lines
- expected open rows
- expected blocked rows
- expected Lead / Support / Context / Blocked role
- expected selected Verdict
- forbidden claims

Allowed MIH-1 fixture categories:

- Hanging vs Material
- Hanging vs Fork
- Material vs Defense
- Fork vs Defense
- Material vs Defense on same board
- EngineCheck Supports/Caps/Refutes over existing rows

MIH-1 forbidden fixture shortcuts:

- fixture implies a new Story family
- pressure expectation
- initiative expectation
- best move expectation
- only move expectation
- proofFailures text as expected public output

Completion standard: Fixture Map names board, rows, roles, selected Verdict, and forbidden claims without opening new meaning.

### MIH-2 Role Stability

MIH-2 opens only StoryTable role stability checks over existing Hanging, Fork, Material, and Defense rows.

MIH-2 verifies:

- selected Verdict remains stable across input order changes
- same-board collisions create no duplicate Lead
- incomplete rows cannot Lead
- refuted rows become Blocked
- capped rows create no standalone strong claim

MIH-2 specific checks:

- `Scene.Material` and `Tactic.Hanging` on the same capture route cannot both Lead.
- `Scene.Defense` cannot Lead without an actual ThreatProof.
- `Tactic.Fork` cannot create a Material claim without complete two-target proof.

MIH-2 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Role Stability keeps selected Verdict deterministic, prevents duplicate Lead, blocks incomplete or refuted rows, and keeps capped rows from standalone strong claims without opening new meaning.

### MIH-3 Material vs Defense Collision

MIH-3 opens only the Material vs Defense collision rule over existing `Scene.Material` and `Scene.Defense` rows.

MIH-3 rules:

- actual material balance change now gives `Scene.Material` priority
- `Scene.Defense` may speak only when it prevents immediate material loss
- speculative material loss does not open Defense
- same-board Material and Defense rows must distinguish actual material change now from prevented immediate loss

MIH-3 forbidden upgrades:

- Defense as best defense
- Defense as only move
- Defense as refutes attack
- Material as winning
- Material as conversion
- Material as decisive

MIH-3 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Material vs Defense collision selects actual material change now over prevented immediate loss, blocks speculative material loss, and keeps both public boundaries bounded.

### MIH-4 EngineCheck Interaction

MIH-4 opens only existing EngineCheck interaction checks over already-open Hanging, Fork, Material, and Defense rows.

MIH-4 reuses existing `EngineCheck` only.

MIH-4 verifies:

- `Supports` creates no new claim
- `Caps` weakens or suppresses allowed claim
- `Refutes` blocks the checked Story
- `Unknown` creates no engine-related expression

MIH-4 forbidden shortcuts:

- engine eval ordering
- raw PV explanation
- engine says wording
- best-move wording
- eval numbers in public values

MIH-4 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: EngineCheck Interaction reuses existing EngineCheck statuses, keeps Supports and Unknown non-speaking, suppresses or weakens Caps, blocks Refutes, and prevents engine eval, raw PV, engine-says, best-move, and eval-number public leakage.

### MIH-5 Negative Corpus

MIH-5 opens only close false-positive negative corpus tests over already-open Hanging, Fork, Material, Defense, and EngineCheck rows.

Looks plausible is not enough. Complete proof or silence.

MIH-5 must include:

- attacked-looking piece but adequate recapture exists
- fork-looking move but only one real target
- material-looking capture but equal or lost after immediate reply
- defense-looking move but no complete ThreatProof
- defense move guards wrong target
- defense move still leaves material loss
- engine refutes otherwise plausible Story
- same-board proof missing
- route mismatch
- stale or wrong engine line

MIH-5 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Negative Corpus keeps close false positives silent unless complete proof exists, and no plausible-looking row may reach selected public output through StoryTable, ExplanationPlan, renderer, or LLM smoke boundaries.

### MIH-6 Downstream Boundary Smoke

MIH-6 opens only downstream boundary smoke over selected Verdict, existing ExplanationPlan, existing DeterministicRenderer, and existing LLM smoke.

MIH-6 verifies:

- ExplanationPlan input is selected Verdict only
- Renderer input is ExplanationPlan only
- LLM smoke input is renderedText, claimKey, strength, forbidden wording, and rephrase-only instruction only
- Support, Context, Blocked, capped, and refuted rows create no standalone public text

MIH-6 forbidden openings:

- new renderer template
- new LLM behavior
- raw Story to renderer or LLM
- raw Proof to renderer or LLM
- raw EngineCheck to renderer or LLM
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Downstream Boundary Smoke passes only selected Lead Verdict data through existing ExplanationPlan, renderer, and LLM smoke boundaries, while non-Lead, capped, and refuted rows stay silent.

### MIH-7 Diagnostics Boundary

MIH-7 opens only diagnostics boundary smoke over already-open Hanging, Fork, Material, Defense, StoryTable, selected Verdict, ExplanationPlan, renderer, and LLM smoke.

MIH-7 verifies:

- proofFailures are internal diagnostic only
- Verdict.values do not include raw proof failure text or engine text
- source row data does not flow directly into ExplanationPlan
- StoryTable debug relation does not become renderer wording

MIH-7 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- new LLM behavior
- raw Story to renderer or LLM
- raw Proof to renderer or LLM
- raw EngineCheck to renderer or LLM
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Diagnostics Boundary keeps proofFailures, raw proof failure text, engine text, source row data, and StoryTable debug relations out of public meaning, Verdict.values, ExplanationPlan source inputs, renderer wording, and LLM smoke prompts.

### MIH Closeout Hard Cleanup Pass

MIH Closeout opens no chess meaning. It only audits the MIH hardening surface.

MIH Closeout audit checklist:

- no new chess meaning opened
- no new proof home opened
- Hanging, Material, and Defense do not duplicate ownership of the same public meaning
- Story label, proof home, and speech key remain separate
- broad terms remain closed and do not become authority
- detailed MIH rules live in StoryInteractionLaw.md only
- other live docs summarize MIH scope without duplicating rule text
- test helpers remain test-only and do not become runtime authority
- public route 200 remains closed
- production API remains closed
- public/user-facing LLM narration remains closed

MIH Closeout ownership map:

- Tactic.Hanging owns the Story label for the hanging tactic; CaptureResult remains the capture proof home; can_win_piece remains the speech key.
- Scene.Material owns the Story label for current material balance change; CaptureResult remains the simple capture proof home; material_balance_changes remains the first speech key.
- Scene.Defense owns the Story label for preventing immediate material loss; ThreatProof and DefenseProof remain the proof homes; defends_piece remains the first speech key.

Completion standard: MIH closes as interaction hardening only, with no new Story family, no new proof home, no duplicate meaning owner, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no promoted test helper, no public route 200, no production API, and no public/user-facing LLM narration.

## Line / Ray Slice

### Line-0 Charter

Current implementation scope is Line / Ray Slice.

Line-0 opens only the charter for the first narrow line/ray proof slice.

First Line scope: a legal move reveals one slider attack on one non-king material target.

LineFact observes geometry.

LineProof binds the revealed line.

Tactic.DiscoveredAttack may speak only after proof.

Allowed Line-0 opening:

- LineFact observation as existing geometry input
- first narrow LineProof proof slice
- one legal move that moves or removes the blocker from a slider line
- one revealed slider attack
- one non-king material target
- same-board proof

Line-0 forbidden openings:

- broad LineTactic
- Pin
- Skewer
- XRay public Story
- RemoveGuard
- mate threat
- king safety
- pressure
- initiative
- best move
- forced line
- winning
- decisive
- blunder
- public route `200`
- production API

Line-0 opens no broad LineTactic, Pin, Skewer, XRay public Story, RemoveGuard, mate threat, king safety, pressure, initiative, best move, forced line, winning, decisive, blunder, public route `200`, or production API.

Completion standard: Line-0 keeps line/ray work at charter scope and opens no public Story, renderer wording, LLM narration, public route `200`, or production API.

### Line-1 LineProof

Line-1 opens only `LineProof` as a narrow proof home.

LineProof proves side, slider piece, blocker or moved piece, revealed target, legal revealing move, line kind, same-board proof, before-move blocked or inactive line, after-move slider attack, and non-king material target.

LineProof must prove:

- side
- slider piece
- blocker or moved piece
- revealed target
- legal revealing move
- line kind: file / rank / diagonal
- same-board proof
- before move: line blocked or not active
- after move: slider attacks target
- target is non-king material piece

LineProof is not a public Story.

LineFact is not a public Story.

LineProof must not directly say pin, pressure, attack works, or wins material.

LineProof proof failure text must not become renderer or LLM input.

Line-1 forbidden openings:

- broad LineTactic
- Tactic.DiscoveredAttack writer
- Pin
- Skewer
- XRay public Story
- RemoveGuard
- mate threat
- king safety
- pressure
- initiative
- best move
- forced line
- winning
- decisive
- blunder
- StoryTable integration
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: `LineProof` proves only a legal revealed slider attack on one non-king material target, while LineFact, LineProof, proof failures, renderer, and LLM boundaries remain non-speaking.

### Line-2 Tactic.DiscoveredAttack Writer

Line-2 opens only the named `TacticDiscoveredAttack` writer for one narrow `Tactic.DiscoveredAttack` Story.

Tactic.DiscoveredAttack writer conditions:

- complete StoryProof
- complete LineProof
- same-board legal replay
- legal revealing move
- target exists
- after move slider attacks target
- writer = TacticDiscoveredAttack
- EngineCheck does not Refute

Line-2 Story identity:

- tactic = DiscoveredAttack
- side = revealing side
- target = revealed target square
- anchor = moved piece or slider anchor
- route = revealing move
- rival = opposite side

Line-2 opened runtime pieces:

- `Tactic.DiscoveredAttack` tactic identity
- `StoryWriter.TacticDiscoveredAttack`
- `TacticDiscoveredAttack.write`
- `TacticDiscoveredAttack.withEngineCheck`
- StoryTable admission for complete, non-refuted `Tactic.DiscoveredAttack` rows

Line-2 forbidden openings:

- Tactic.Pin
- Tactic.Skewer
- Tactic.XRay
- RemoveGuard
- king target speech
- broad LineTactic
- XRay public Story
- mate threat
- king safety
- pressure
- initiative
- best move
- forced line
- winning
- decisive
- blunder
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Target king remains silent in Line-2.

Completion standard: Tactic.DiscoveredAttack may become a Story only through complete StoryProof plus complete LineProof for one legal revealed slider attack on one non-king material target, while Pin, Skewer, XRay, RemoveGuard, king target speech, ExplanationPlan, renderer, LLM, public route `200`, and production API remain closed.

### Line-3 Negative Corpus

Line-3 opens only the negative corpus for the narrow `Tactic.DiscoveredAttack` slice.

Line-3 negative corpus must close:

- legal move is absent
- same-board proof is absent
- line is not actually opened
- target is still not attacked after the move
- slider is not a slider
- target is king
- blocker moved but another piece still blocks
- discovered-looking move has no target
- pressure, initiative, or mate wording tries to enter
- Pin, Skewer, or XRay classification tries to enter

Geometry is not enough. Revealed attack proof or silence.

Line-3 opens no new Story family, proof home, renderer wording, LLM smoke, public route `200`, production API, pressure, initiative, mate threat, Pin, Skewer, XRay public Story, or RemoveGuard.

Completion standard: Line-3 keeps discovered-attack-looking false positives silent unless complete StoryProof and complete LineProof prove one legal revealed slider attack on one non-king material target.

### Line-4 EngineCheck Reuse

Line-4 opens only existing `EngineCheck` reuse for existing `Tactic.DiscoveredAttack` Stories.

Line-4 EngineCheck rules:

- EngineCheck cannot create DiscoveredAttack
- Supports creates no new claim
- Caps suppresses strong expression
- Refutes blocks the Story
- Unknown creates no engine expression

Line-4 forbidden openings:

- raw eval ordering
- raw PV explanation
- engine says
- best move
- winning tactic
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.DiscoveredAttack` Story; it never creates DiscoveredAttack, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording.

### Line-5 StoryTable Integration

Line-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, and `Tactic.DiscoveredAttack` rows.

Line-5 verification:

- selected Verdict remains stable when input order changes
- DiscoveredAttack does not own a Material claim
- Hanging and Material on the same target do not both become Lead
- Defense without an actual threat cannot create a claim that blocks DiscoveredAttack
- Fork without two-target proof cannot absorb DiscoveredAttack

Line-5 opens no Material claim for DiscoveredAttack, no Defense claim without ThreatProof, no Fork claim without two-target proof, no renderer wording, no LLM smoke, no public route `200`, and no production API.

Completion standard: StoryTable may arbitrate existing Hanging, Fork, Material, Defense, and DiscoveredAttack rows deterministically, while claim ownership remains with each already-open proof-backed Story family and DiscoveredAttack still has no downstream speech.

### Line-6 ExplanationPlan

Line-6 opens only ExplanationPlan mapping for selected Lead `Tactic.DiscoveredAttack` Verdicts.

Line-6 allowed claim key:

- reveals_attack_on_piece

Line-6 forbidden claim keys:

- wins_material
- pins_piece
- skewers_piece
- creates_pressure
- takes_initiative
- mate_threat
- best_move
- forced
- decisive

Support, Context, Blocked, capped, and refuted DiscoveredAttack rows create no standalone claim.

Line-6 opens no renderer wording, LLM smoke, public route `200`, production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure, initiative, mate threat, best-move, forced-line, winning, or decisive claim.

Completion standard: selected uncapped Lead DiscoveredAttack Verdicts may lower only to the internal `reveals_attack_on_piece` claim key with the listed forbidden wording boundary; all non-Lead, capped, and refuted DiscoveredAttack rows remain claimless, and renderer and LLM stages remain closed.

### Line-7 Deterministic Renderer

Line-7 opens only deterministic renderer text for selected `Tactic.DiscoveredAttack` ExplanationPlan.

Renderer input is `ExplanationPlan` only.

Line-7 allowed template:

- `{route} reveals an attack on the piece on {target}.`

Line-7 forbidden renderer wording:

- wins material
- winning
- decisive
- best move
- forces
- pins
- skewers
- puts pressure
- creates a mating threat

Line-7 opens no LLM smoke, public route `200`, production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure, initiative, mate threat, best-move, forced-line, winning, or decisive claim.

Completion standard: DeterministicRenderer may phrase only the selected Lead `reveals_attack_on_piece` ExplanationPlan through the bounded template, must reject stronger wording through forbidden wording checks, and must keep Support, Context, Blocked, capped, refuted, and no-claim DiscoveredAttack plans silent.

### Line-8 LLM Smoke

Line-8 opens only LLM smoke for selected DiscoveredAttack ExplanationPlan and RenderedLine.

Line-8 reuses existing 8B prompt smoke only.

Line-8 LLM input:

- renderedText
- claimKey
- strength
- forbidden wording
- Rephrase only. Do not add chess facts.

Line-8 forbidden inputs and additions:

- raw Story
- raw LineProof
- LineFact
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- new move
- new line
- mate
- pressure
- initiative
- winning claim

Line-8 opens no raw Story, raw LineProof, LineFact, BoardFacts, EngineCheck, raw PV, proofFailures, public/user-facing LLM narration, public route `200`, production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure, initiative, mate threat, best-move, forced-line, winning, or decisive claim.

Completion standard: LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and the instruction `Rephrase only. Do not add chess facts.` for selected DiscoveredAttack RenderedLine; it must reject raw proof/board/engine inputs, new moves, new lines, and mate, pressure, initiative, or winning claims.

### Line Closeout Hard Cleanup

Line Closeout opens no new chess meaning. It only audits the Line / Ray Slice hard cleanup surface.

Line Closeout must confirm:

- LineFact observes geometry only.
- LineProof binds the revealed line only.
- Tactic.DiscoveredAttack owns only the proof-backed Story identity.
- `reveals_attack_on_piece` owns only the bounded speech claim key.
- Broad Line, Ray, XRay, Pin, and Skewer are not live public authority for this slice.
- LineProof does not duplicate StoryProof, CaptureResult, MultiTargetProof, ThreatProof, DefenseProof, or EngineCheck.
- Renderer and LLM smoke cannot create wording stronger than the selected DiscoveredAttack ExplanationPlan.
- Detailed Line authority lives only in `StoryInteractionLaw.md`; AGENTS.md, README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, and ChessModelContract.md may summarize only.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

Line Closeout opens no broad LineTactic, Ray tactic, XRay public Story, Pin, Skewer, RemoveGuard, Material claim, pressure, initiative, mate threat, best-move, forced-line, winning, decisive, blunder, new proof home, new Story family, renderer wording beyond Line-7, LLM input beyond Line-8, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Line closes as a narrow proof-backed discovered attack slice only, with LineFact, LineProof, Tactic.DiscoveredAttack, StoryTable, ExplanationPlan, Renderer, and LLM smoke keeping separate authority and no downstream layer speaking beyond selected proof-backed `reveals_attack_on_piece`.

## Line / Defender Contact Neighborhood

### Pin-0 Charter

Current implementation scope is Line / Defender Contact Neighborhood.

Pin-0 opens only the charter for the second narrow line/defender contact vertical slice.

Pin first positive scope is not a broad pin family.

Pin first scope: a legal move creates or reveals a line where one non-king piece is pinned to its king.

LineFact observes geometry.

LineProof binds the line.

PinProof proves the pinned relation.

Tactic.Pin may speak only after proof.

Pin-0 allowed opening:

- narrow `Tactic.Pin`
- king-behind line relation
- one non-king pinned target
- legal move that creates or reveals the pin relation
- bounded pin wording after selected Verdict only

Pin-0 forbidden openings:

- broad LineTactic
- broad AbsPin or RelPin family
- Skewer
- XRay public Story
- RemoveGuard
- DiscoveredAttack expansion
- mate threat
- king safety
- winning material
- decisive tactic
- forced move
- best move
- cannot move wording
- pressure
- initiative
- public route `200`
- production API
- public/user-facing LLM narration

Pin-0 opens no broad LineTactic, broad AbsPin or RelPin family, Skewer, XRay public Story, RemoveGuard, DiscoveredAttack expansion, mate threat, king safety, winning material, decisive tactic, forced move, best move, cannot move wording, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Pin-0 keeps pin work at charter scope and opens no PinProof runtime, no Tactic.Pin writer, no StoryTable integration, no ExplanationPlan mapping, no renderer wording, no LLM smoke, no public route `200`, and no production API.

### Pin-1 PinProof

Pin-1 opens only `PinProof` as a narrow proof home.

PinProof proves side creating the pin, pinned target, pinning slider, king behind target, legal pinning or revealing move, line kind, same-board proof, before/after relation, target non-king, target and king same side, and slider attacks through target toward king after move.

PinProof must prove:

- side creating the pin
- pinned target
- pinning slider
- king behind target
- legal pinning or revealing move
- line kind: file / rank / diagonal
- same-board proof
- before/after relation
- target is non-king
- target and king are same side
- slider attacks through target toward king after move

PinProof is not a public Story.

LineFact is not a public Story.

LineProof is not a public Story.

PinProof must not say material gain, king unsafe, mate, pressure, or initiative.

PinProof proof failure text must not become renderer or LLM input.

Pin-1 forbidden openings:

- Tactic.Pin writer
- StoryTable integration
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- material gain claim
- king unsafe claim
- mate threat
- pressure
- initiative
- public route `200`
- production API

Completion standard: `PinProof` proves only a legal move creating or revealing one pinned-to-king relation over one non-king target, while LineFact, LineProof, PinProof, proof failures, renderer, and LLM boundaries remain non-speaking.

### Pin-2 Tactic.Pin Writer

Pin-2 opens only the named `TacticPin` writer for one narrow `Tactic.Pin` Story.

TacticPin writer conditions:

- complete StoryProof
- complete PinProof
- same-board legal replay
- legal pinning or revealing move
- pinned target exists
- pinning slider exists
- king-behind-target relation complete
- writer = TacticPin
- EngineCheck does not Refute

Pin-2 Story identity:

- tactic = Pin
- scene = Tactic
- side = pinning side
- rival = pinned side
- target = pinned target square
- anchor = pinning slider square or moved piece square
- route = pinning/revealing move

Pin-2 opened runtime pieces:

- `Tactic.Pin` tactic identity
- `StoryWriter.TacticPin` writer identity
- `TacticPin.write`
- `TacticPin.withEngineCheck`
- StoryTable admission for complete non-refuted `Tactic.Pin` rows

Pin-2 forbidden openings:

- Material claim
- Defense ownership
- RemoveGuard ownership
- king target speech
- broad AbsPin or RelPin family
- Skewer
- XRay public Story
- DiscoveredAttack expansion
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Target king remains silent in Pin-2.

Completion standard: Tactic.Pin may become a Story only through complete StoryProof plus complete PinProof for one legal move creating or revealing one pinned-to-king relation over one non-king target, while Material, Defense, RemoveGuard, king target speech, ExplanationPlan, renderer, LLM, public route `200`, and production API remain closed.

### Pin-3 Negative Corpus

Pin-3 opens only the negative corpus for the narrow `Tactic.Pin` slice.

A line to a king is not enough. Complete pinned relation or silence.

Pin-3 required silent counterexamples:

- legal move absent
- same-board proof absent
- slider is not a slider
- no king behind target
- target and king are not same side
- line does not continue through target to king
- target is king
- another blocker is between slider and king
- pin-looking geometry but no post-move relation
- discovered attack only
- skewer-looking position classified as Pin
- mate wording
- king safety wording
- pressure wording

Pin-3 forbidden openings:

- new Pin writer behavior
- broad AbsPin or RelPin family
- Skewer
- DiscoveredAttack expansion
- Material claim
- Defense ownership
- RemoveGuard ownership
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: Pin-looking rows stay silent unless complete StoryProof and complete PinProof prove one legal move creates or reveals one pinned-to-king relation over one non-king target.

### Pin-4 EngineCheck Reuse

Pin-4 opens only existing `EngineCheck` reuse for existing `Tactic.Pin` Stories.

EngineCheck must not create Pin.

`Supports` creates no new Pin claim.

`Caps` suppresses allowed claim or weakens expression to bounded strength when downstream speech opens.

`Refutes` blocks the Pin Story.

`Unknown` creates no engine expression.

Pin-4 forbidden openings:

- engine says
- best move
- only move
- winning tactic
- forced win
- raw PV explanation
- eval number public value
- new EngineCheck type
- Pin from engine evidence
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.Pin` Story; it never creates Pin, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording.

### Pin-5 StoryTable Integration

Pin-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, and `Tactic.Pin` rows.

Pin-5 StoryTable checks:

- selected Verdict remains stable when input order changes
- Pin does not own Material claim
- Pin does not own king safety claim
- DiscoveredAttack and Pin on the same line do not both become Lead
- actual material change now remains owned by Scene.Material
- Defense creates no defense claim without complete ThreatProof and complete DefenseProof

Pin-5 forbidden openings:

- new Pin proof home
- new Story family
- broad AbsPin or RelPin family
- Material claim from Pin
- king safety claim from Pin
- Defense claim from incomplete Defense rows
- duplicate Lead for same-line DiscoveredAttack and Pin
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: StoryTable orders existing open rows with Pin deterministically, keeps one selected Lead, and keeps Material, Defense, DiscoveredAttack, and Pin claim homes separate.

### Pin-6 ExplanationPlan

Pin-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Pin` Verdicts.

Pin-6 ExplanationPlan input is selected uncapped Lead Verdict only.

Pin-6 allowed claim key:

- pins_piece

Pin-6 forbidden claim keys:

- wins_material
- king_unsafe
- mate_threat
- best_move
- only_move
- forced
- decisive
- creates_pressure
- takes_initiative
- cannot_move

Support, Context, Blocked, capped, and refuted Pin rows create no standalone claim.

Pin-6 forbidden openings:

- wins_material claim
- king_unsafe claim
- mate_threat claim
- best_move claim
- only_move claim
- forced claim
- decisive claim
- creates_pressure claim
- takes_initiative claim
- cannot_move wording
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: selected uncapped Lead `Tactic.Pin` Verdicts may lower only to bounded `pins_piece`; all non-Lead, capped, refuted, and unselected Pin rows remain without standalone claim.

### Pin-7 Deterministic Renderer

Pin-7 opens only deterministic renderer text for selected `Tactic.Pin` ExplanationPlan.

Pin-7 renderer input is `ExplanationPlan` only.

Pin-7 allowed renderer template:

- `{route} pins the piece on {target}.`

Pin-7 forbidden renderer wording:

- cannot move
- the king is unsafe
- wins material
- winning
- decisive
- best move
- only move
- forces
- creates pressure
- threatens mate

Pin-7 forbidden openings:

- raw Verdict input
- raw Story input
- PinProof input
- LineFact input
- LineProof input
- EngineCheck input
- proofFailures input
- LLM smoke
- public route `200`
- production API

Completion standard: Renderer phrases only selected bounded `pins_piece` ExplanationPlan data and refuses wording stronger than the Pin-6 claim boundary.

### Pin-8 LLM Smoke

Pin-8 opens only LLM smoke for selected Pin ExplanationPlan and RenderedLine.

Pin-8 reuses only the existing 8B Codex CLI prompt smoke contract.

Pin-8 LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Pin-8 forbidden inputs and additions:

- raw Story
- raw PinProof
- raw LineProof
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- new move
- new line
- mate claim
- pressure claim
- initiative claim
- winning claim
- cannot-move claim

Pin-8 forbidden openings:

- public/user-facing LLM narration
- public route `200`
- production API
- raw proof repair
- engine explanation

Completion standard: LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for selected Pin RenderedLine; it rejects raw proof/board/engine inputs, new moves, new lines, and mate, pressure, initiative, winning, or cannot-move claims.

### Pin Closeout Hard Cleanup

Pin Closeout opens no new chess meaning. It only audits the Pin hard cleanup surface.

Pin Closeout must confirm:

- LineFact observes geometry only.
- LineProof binds line evidence only and does not own Pin speech.
- PinProof proves only the pinned-to-king relation.
- Tactic.Pin owns only the proof-backed Story identity.
- `pins_piece` owns only the bounded speech claim key.
- Pin does not own Material, Defense, DiscoveredAttack, Skewer, or RemoveGuard meaning.
- Broad Line, Ray, XRay, and broad Pin family terms are not live public authority for this slice.
- Renderer and LLM smoke cannot create wording stronger than `pins_piece`.
- Test helpers are not promoted into runtime authority.
- Detailed Pin authority lives only in `StoryInteractionLaw.md`; AGENTS.md, README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, and ChessModelContract.md may summarize only.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

Pin Closeout opens no broad LineTactic, broad Ray tactic, XRay public Story, broad AbsPin or RelPin family, Skewer, RemoveGuard, Material claim, Defense claim, DiscoveredAttack expansion, pressure, initiative, mate threat, cannot-move wording, best-move, only-move, forced-line, winning, decisive, new proof home, new Story family, renderer wording beyond Pin-7, LLM input beyond Pin-8, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Pin closes as a narrow proof-backed pinned-to-king slice only, with LineFact, LineProof, PinProof, Tactic.Pin, StoryTable, ExplanationPlan, Renderer, and LLM smoke keeping separate authority and no downstream layer speaking beyond selected proof-backed `pins_piece`.

### RemoveGuard-0 Charter

Current implementation scope is Line / Defender Contact Neighborhood.

RemoveGuard-0 opens only the charter for the third narrow line/defender contact vertical slice.

First RemoveGuard positive scope is not a broad remove-the-guard motif.

RemoveGuard first scope: a legal move removes one defender from one non-king material target.

First runtime positive path stays centered on defender capture when possible.

Deflection is allowed only when exact-board proof immediately after the same move shows the defender no longer guards the target.

BoardFacts observes guard relation.

RemoveGuardProof proves the guard was removed.

Tactic.RemoveGuard may speak only after proof.

RemoveGuard-0 allowed opening:

- narrow `Tactic.RemoveGuard`
- one non-king material target
- one defender
- one legal move that removes the defender guard relation
- bounded remove-guard wording after selected Verdict only

RemoveGuard-0 forbidden openings:

- broad deflection tactic
- overloaded defender theory
- discovered attack expansion
- Pin expansion
- Skewer expansion
- XRay expansion
- material win claim
- winning
- decisive
- forced
- best move
- only move
- no defense
- refutes defense
- collapses position
- pressure
- initiative
- public route `200`
- production API
- public/user-facing LLM narration

RemoveGuard-0 opens no broad deflection tactic, overloaded defender theory, discovered attack expansion, Pin/Skewer/XRay expansion, material win claim, winning, decisive, forced, best move, only move, no defense, refutes defense, collapses position, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: RemoveGuard-0 keeps remove-guard work at charter scope and opens no RemoveGuardProof runtime, no Tactic.RemoveGuard writer, no StoryTable integration, no ExplanationPlan mapping, no renderer wording, no LLM smoke, no public route `200`, and no production API.

### RemoveGuard-1 RemoveGuardProof

RemoveGuard-1 opens only `RemoveGuardProof` as a narrow proof home.

RemoveGuardProof must prove:

- side removing the guard
- rival side
- guarded target
- removed defender
- legal remove-guard move
- target is non-king material piece
- defender guarded target before move
- after move defender no longer guards target
- same-board proof
- exact-board after-move relation

RemoveGuard-1 first allowed removal kind:

- DefenderCaptured

RemoveGuard-1 conditional removal kind:

- GuardLineBlocked, only when one legal move blocks a slider defender guard line and exact-board proof shows the defender no longer guards the target

RemoveGuard-1 closed removal kinds:

- opponent-reply deflection
- sacrifice lure
- overloaded defender
- remove guard by long tactic sequence
- defender cannot defend general theory

RemoveGuardProof is not a public Story.

RemoveGuardProof owns no material result.

RemoveGuardProof proof failure text must not become renderer or LLM input.

RemoveGuard-1 forbidden openings:

- Tactic.RemoveGuard writer
- StoryTable integration
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- material win claim
- winning
- decisive
- forced
- best move
- only move
- no defense
- refutes defense
- pressure
- initiative
- public route `200`
- production API

Completion standard: `RemoveGuardProof` proves only that one legal same-board move removes one defender guard relation from one non-king material target, while RemoveGuardProof, proof failures, renderer, LLM, public route `200`, and production API remain non-speaking.

### RemoveGuard-2 Tactic.RemoveGuard Writer

RemoveGuard-2 opens only the named `TacticRemoveGuard` writer for one narrow `Tactic.RemoveGuard` Story.

TacticRemoveGuard writer conditions:

- complete StoryProof
- complete RemoveGuardProof
- same-board legal replay
- legal remove-guard move
- guarded target exists
- removed defender existed and guarded target before move
- defender no longer guards target after move
- writer = TacticRemoveGuard
- EngineCheck does not Refute

RemoveGuard-2 Story identity:

- tactic = RemoveGuard
- scene = Tactic
- side = guard-removing side
- rival = target/defender side
- target = guarded target square
- anchor = removed defender square or moving piece square
- route = remove-guard move

RemoveGuard-2 opened runtime pieces:

- `Tactic.RemoveGuard` tactic identity
- `StoryWriter.TacticRemoveGuard` writer identity
- `TacticRemoveGuard.write`
- `Story.removeGuardProof`
- StoryTable admission for complete non-refuted `Tactic.RemoveGuard` rows

RemoveGuard-2 forbidden openings:

- Scene.Material claim
- Tactic.Hanging replacement
- Defense refutation wording
- material win claim
- winning
- decisive
- forced
- best move
- only move
- no defense
- pressure
- initiative
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: `Tactic.RemoveGuard` may become a Story only through complete StoryProof plus complete RemoveGuardProof for one legal move removing one defender guard relation from one non-king material target, while Material, Hanging, Defense-refutation wording, ExplanationPlan, renderer, LLM, public route `200`, and production API remain closed.

### RemoveGuard-3 Negative Corpus

RemoveGuard-3 opens only the negative corpus for the narrow `Tactic.RemoveGuard` slice.

RemoveGuard-3 required silent counterexamples:

- legal move missing
- same-board proof missing
- target is king
- defender did not guard target
- defender still guards target after move
- another defender remains and broad claim is attempted
- direct material gain claim
- Pin misclassified as RemoveGuard
- DiscoveredAttack misclassified as RemoveGuard
- Skewer misclassified as RemoveGuard
- opponent-reply deflection
- overloaded defender claim
- no defense wording
- wins material wording
- best move wording

Removing one guard is not winning material. Complete guard-removal proof or silence.

RemoveGuard-3 forbidden openings:

- new proof home
- new writer
- StoryTable ordering change
- Scene.Material claim
- Tactic.Hanging replacement
- Defense refutation wording
- Pin ownership
- DiscoveredAttack ownership
- Skewer ownership
- overloaded defender theory
- broad deflection tactic
- material win claim
- no defense
- wins material
- best move
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: RemoveGuard-3 adds only close false-positive corpus coverage; no plausible-looking row may speak unless complete StoryProof plus complete RemoveGuardProof proves one legal same-board move removes one defender guard relation from one non-king material target.

### RemoveGuard-4 EngineCheck Reuse

RemoveGuard-4 opens only existing `EngineCheck` reuse for existing `Tactic.RemoveGuard` Stories.

RemoveGuard-4 EngineCheck rules:

- EngineCheck cannot create RemoveGuard
- Supports creates no new claim
- Caps suppresses standalone claim or weakens expression to bounded strength when downstream speech opens
- Refutes blocks the RemoveGuard Story
- Unknown creates no engine expression

RemoveGuard-4 forbidden openings:

- engine says
- best move
- only move
- winning tactic
- raw PV explanation
- eval number public value
- new EngineCheck type
- RemoveGuard from engine evidence
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.RemoveGuard` Story; it never creates RemoveGuard, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording.

### RemoveGuard-5 StoryTable Integration

RemoveGuard-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, and `Tactic.RemoveGuard` rows.

RemoveGuard-5 StoryTable checks:

- selected Verdict remains stable when input order changes
- RemoveGuard does not own Material claim
- RemoveGuard does not replace Hanging claim
- Defense creates no response claim without complete ThreatProof and complete DefenseProof
- Pin and RemoveGuard on the same defender or line do not both become Lead
- actual material change now remains owned by Scene.Material

RemoveGuard-5 forbidden openings:

- new RemoveGuard proof home
- new Story family
- Material claim from RemoveGuard
- Hanging claim from RemoveGuard
- Defense response from incomplete Defense rows
- duplicate Lead for same-line Pin and RemoveGuard
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: StoryTable orders existing open rows with RemoveGuard deterministically, keeps one selected Lead, and keeps Material, Hanging, Defense, Pin, DiscoveredAttack, Fork, and RemoveGuard claim homes separate.

### RemoveGuard-6 ExplanationPlan

RemoveGuard-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.RemoveGuard` Verdicts.

RemoveGuard-6 ExplanationPlan input is selected uncapped Lead Verdict only.

RemoveGuard-6 allowed claim key:

- removes_defender

RemoveGuard-6 forbidden claim keys:

- wins_material
- target_is_hanging
- no_defense
- refutes_defense
- best_move
- only_move
- forced
- decisive
- creates_pressure
- takes_initiative

Support, Context, Blocked, capped, and refuted RemoveGuard rows create no standalone claim.

RemoveGuard-6 forbidden openings:

- wins_material claim
- target_is_hanging claim
- no_defense claim
- refutes_defense claim
- best_move claim
- only_move claim
- forced claim
- decisive claim
- creates_pressure claim
- takes_initiative claim
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: selected uncapped Lead `Tactic.RemoveGuard` Verdicts may lower only to bounded `removes_defender`; all non-Lead, capped, refuted, and unselected RemoveGuard rows remain without standalone claim.

### RemoveGuard-7 Deterministic Renderer

RemoveGuard-7 opens only deterministic renderer text for selected `Tactic.RemoveGuard` ExplanationPlan.

RemoveGuard-7 renderer input is `ExplanationPlan` only.

RemoveGuard-7 allowed renderer template:

- `{route} removes the defender of the piece on {target}.`

RemoveGuard-7 forbidden renderer wording:

- wins material
- leaves it undefended
- no defender remains
- best move
- only move
- forces
- decisive
- refutes the defense
- creates pressure

RemoveGuard-7 forbidden openings:

- raw Verdict input
- raw Story input
- RemoveGuardProof input
- BoardFacts input
- EngineCheck input
- proofFailures input
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Renderer phrases only selected bounded `removes_defender` ExplanationPlan data and refuses wording stronger than the RemoveGuard-6 claim boundary.

### RemoveGuard-8 LLM Smoke

RemoveGuard-8 opens only LLM smoke for selected RemoveGuard ExplanationPlan and RenderedLine.

RemoveGuard-8 reuses only the existing 8B Codex CLI prompt smoke contract.

RemoveGuard-8 LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

RemoveGuard-8 forbidden inputs and additions:

- raw Story
- raw RemoveGuardProof
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- new move
- new line
- material win claim
- no-defense claim
- pressure claim
- initiative claim

RemoveGuard-8 forbidden openings:

- public/user-facing LLM narration
- public route `200`
- production API
- raw proof repair
- engine explanation

Completion standard: LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for selected RemoveGuard RenderedLine; it rejects raw proof/board/engine inputs, new moves, new lines, and material-win, no-defense, pressure, or initiative claims.

### RemoveGuard Closeout Hard Cleanup

RemoveGuard Closeout opens no new chess meaning. It only audits the RemoveGuard hard cleanup surface.

RemoveGuard Closeout must confirm:

- BoardFacts guard relation observes only same-side guard contact.
- RemoveGuardProof proves only that one defender guard relation was removed from one non-king material target.
- Tactic.RemoveGuard owns only the proof-backed Story identity.
- `removes_defender` owns only the bounded speech claim key.
- RemoveGuard does not own Material, Hanging, Defense, Pin, or DiscoveredAttack meaning.
- Deflection, overload, no-defender, and wins-material terms are not live public authority for this slice.
- Renderer and LLM smoke cannot create wording stronger than `removes_defender`.
- Test helpers are not promoted into runtime authority.
- Detailed RemoveGuard authority lives only in `StoryInteractionLaw.md`; AGENTS.md, README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, and ChessModelContract.md may summarize only.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

RemoveGuard Closeout opens no broad deflection tactic, overloaded defender theory, no-defender claim, wins-material claim, Material claim, Hanging claim, Defense claim, Pin expansion, DiscoveredAttack expansion, Skewer, XRay, pressure, initiative, best-move, only-move, forced-line, winning, decisive, new proof home, new Story family, renderer wording beyond RemoveGuard-7, LLM input beyond RemoveGuard-8, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: RemoveGuard closes as a narrow proof-backed guard-removal slice only, with BoardFacts guard relation, RemoveGuardProof, Tactic.RemoveGuard, StoryTable, ExplanationPlan, Renderer, and LLM smoke keeping separate authority and no downstream layer speaking beyond selected proof-backed `removes_defender`.

## Line / Defender Interaction Hardening

Line/Defender hardening opens no new Story. It proves that existing line and defender Stories do not steal each other's meaning.

### LDH-0 Charter

LDH-0 opens only existing Line/Defender rows interaction hardening.

LDH-0 opens only complex same-board fixture checks.

LDH-0 opens only StoryTable role stability checks.

LDH-0 opens only downstream no-overclaim smoke.

LDH-0 may apply only the minimum StoryTable ordering fix if an existing DiscoveredAttack ordering bug is exposed.

Allowed LDH-0 line and defender rows:

- Tactic.DiscoveredAttack
- Tactic.Pin
- Tactic.RemoveGuard

Allowed LDH-0 collision targets:

- Tactic.Hanging
- Tactic.Fork
- Scene.Material
- Scene.Defense

LDH-0 forbidden openings:

- Tactic.Skewer
- Tactic.XRay
- broad LineTactic
- broad deflection
- overloaded defender
- pressure
- initiative
- mate threat
- king safety
- material win claim
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: LDH-0 hardens only existing row interaction, keeps exactly one selected Lead, keeps non-Lead line/defender rows from standalone downstream claims, and keeps renderer/LLM smoke no stronger than the selected ExplanationPlan.

### LDH-1 Fixture Map

LDH-1 opens only complex same-board Fixture Map coverage.

Each LDH-1 fixture must state:

- same-board FEN
- side to move
- candidate_passer legal lines
- expected open rows
- expected blocked rows
- expected Lead / Support / Context / Blocked role
- expected selected Verdict
- forbidden claims

Required LDH-1 fixture categories:

- DiscoveredAttack vs Pin
- DiscoveredAttack vs RemoveGuard
- Pin vs RemoveGuard
- DiscoveredAttack + Pin + RemoveGuard same-board
- Line/Defender row vs Material
- Line/Defender row vs Hanging
- Line/Defender row vs Defense
- EngineCheck Supports/Caps/Refutes over existing Line/Defender rows

LDH-1 fixture map forbids:

- expecting a Skewer-looking fixture as positive Skewer
- using `wins material`, `best move`, `pressure`, or `initiative` as expected output
- using proofFailures text as public expected output

LDH-1 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-1 records fixture-map coverage only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-2 Role Stability

LDH-2 opens only StoryTable role stability checks over existing Line/Defender rows.

LDH-2 role stability must verify:

- input order changes must keep the same selected Verdict
- same meaning must not become duplicate Lead
- incomplete rows must not become Lead
- refuted rows must become Blocked
- capped rows must create no standalone claim

LDH-2 must specifically check:

- Pin and DiscoveredAttack on the same line must not both become Lead
- RemoveGuard must not own Pin line relation
- DiscoveredAttack must not own RemoveGuard defender relation

LDH-2 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-2 hardens StoryTable role stability only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-3 Meaning Ownership Boundary

LDH-3 opens only Meaning Ownership Boundary checks over existing Line/Defender and collision rows.

LDH-3 owned meanings:

- Tactic.DiscoveredAttack owns only a legal move reveals one slider attack on one material target.
- Tactic.Pin owns only a non-king piece is pinned to its own king on a line.
- Tactic.RemoveGuard owns only one defender no longer guards one target after a legal move.
- Scene.Material owns only actual material balance change now.
- Tactic.Hanging owns only a capturable target with bounded material gain proof.
- Scene.Defense owns only complete ThreatProof plus DefenseProof prevents immediate material loss.

LDH-3 forbidden ownership leaks:

- RemoveGuard must not say material gain.
- Pin must not say cannot-move or king unsafe.
- DiscoveredAttack must not say wins-material.
- Material must not own line tactic identity.
- Defense must not say it stopped the threat without complete threat proof.

LDH-3 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-3 hardens meaning ownership boundaries only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-4 EngineCheck Interaction

LDH-4 opens only existing EngineCheck interaction checks over existing Line/Defender rows.

LDH-4 must verify:

- Supports must not create a new claim.
- Caps must suppress allowed claim or keep downstream speech bounded.
- Refutes must make the checked Story Blocked.
- Unknown must create no engine-related expression.

LDH-4 forbidden public engine wording:

- engine says
- raw PV explanation
- eval number public value
- best move
- only move
- forced line

LDH-4 opens no new EngineCheck proof home, new Story family, engine-says wording, raw PV explanation, eval number public value, best move, only move, forced line, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-4 hardens existing EngineCheck status interaction only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-5 Negative Corpus

LDH-5 opens only close false-positive negative corpus tests over existing Line/Defender rows and already-open collision rows.

LDH-5 close false positives must stay silent:

- line opens but no actual attack
- attack appears but target is king
- pin-looking line but no king behind target
- remove-guard-looking move but defender still guards target
- defender removed but Material or Hanging proof is incomplete
- discovered attack and pin both plausible but one proof is incomplete
- wrong-board or stale same-board proof
- route mismatch
- engine refutes plausible row
- Skewer-looking relation tries to enter before Skewer opens

LDH-5 rule: Looks like a line tactic is not enough. Existing complete proof or silence.

LDH-5 opens no new Story family, proof home, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-5 hardens close false-positive silence only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-6 Downstream Boundary Smoke

LDH-6 opens only downstream boundary smoke over selected Lead Verdicts from existing Line/Defender rows.

LDH-6 must verify:

- ExplanationPlan input is selected Verdict only.
- Renderer input is ExplanationPlan only.
- LLM smoke input is renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction only.
- Support, Context, Blocked, capped, and refuted rows create no standalone text.

LDH-6 forbidden downstream inputs or changes:

- no new renderer template
- no new LLM behavior
- no raw Story, Proof, or EngineCheck reaches renderer or LLM smoke

LDH-6 opens no new Story family, proof home, renderer template, LLM behavior, raw Story, Proof, or EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-6 hardens downstream selected Lead handoff only; it does not add a Story family, proof home, renderer template, LLM behavior, public route, production API, or public/user-facing LLM narration.

### LDH-7 Diagnostics Boundary

LDH-7 opens only diagnostics boundary smoke over existing Line/Defender rows, StoryTable, selected Verdict, ExplanationPlan, renderer, LLM smoke, and test-helper authority.

LDH-7 must verify:

- proofFailures are internal diagnostic only.
- raw proof text does not enter Verdict.values.
- EngineCheck text does not flow directly into ExplanationPlan.
- StoryTable debug relation does not become renderer wording.
- test helpers do not become runtime authority.

LDH-7 opens no new Story family, proof home, renderer wording, LLM behavior, runtime authority helper, raw Story, raw Proof, raw EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-7 keeps proofFailures, raw proof text, EngineCheck text, StoryTable debug relations, and test helpers out of public meaning, Verdict.values, ExplanationPlan direct inputs, renderer wording, LLM smoke prompts, and runtime authority.

### LDH Closeout Hard Cleanup Pass

LDH Closeout opens no chess meaning. It only audits the Line / Defender Interaction Hardening surface.

LDH Closeout audit checklist:

- no new Story family opened.
- no new proof home opened.
- LineFact, LineProof, PinProof, and RemoveGuardProof authority stay separated.
- Tactic.DiscoveredAttack, Tactic.Pin, and Tactic.RemoveGuard do not steal each other's meaning.
- Tactic.DiscoveredAttack, Tactic.Pin, and Tactic.RemoveGuard do not invade Scene.Material, Tactic.Hanging, or Scene.Defense claim homes.
- broad Line, Ray, XRay, Skewer, deflection, overload, pressure, and initiative do not become live authority.
- detailed LDH interaction rules live in StoryInteractionLaw.md only.
- other live docs summarize LDH scope without duplicating rule text.
- public route 200 remains closed.
- production API remains closed.
- public/user-facing LLM narration remains closed.

LDH Closeout ownership map:

- BoardFacts LineFact observes geometry only; it is not a Story or proof home.
- LineProof belongs only to Tactic.DiscoveredAttack for this hardening surface.
- PinProof belongs only to Tactic.Pin.
- RemoveGuardProof belongs only to Tactic.RemoveGuard.
- Scene.Material keeps actual material balance change now.
- Tactic.Hanging keeps capturable target with bounded material gain proof.
- Scene.Defense keeps complete ThreatProof plus DefenseProof preventing immediate material loss.

Completion standard: LDH closes as interaction hardening only, with no new Story family, no new proof home, no mixed LineFact, LineProof, PinProof, or RemoveGuardProof authority, no Line/Defender meaning theft, no Material, Hanging, or Defense claim-home invasion, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no public route 200, no production API, and no public/user-facing LLM narration.

## Skewer Slice

### Skewer-0 Charter

Current implementation scope is Line / Defender Contact Neighborhood late vertical slice.

Skewer-0 opens only the charter for the fourth narrow line/defender contact vertical slice.

First Skewer positive scope is not a broad skewer tactic.

Skewer first scope: a legal move creates or reveals a slider attack on one front non-king material target, with a second non-king material target behind it on the same line.

LineFact observes geometry.

SkewerProof proves the front-and-back target relation.

Tactic.Skewer may speak only after proof.

Skewer-0 allowed opening:

- narrow `Tactic.Skewer`
- one slider
- one front target
- one rear target
- front/rear target same-line relation
- legal move that creates or reveals the front/rear relation
- bounded skewer wording after selected Verdict only

Skewer-0 forbidden openings:

- broad LineTactic
- XRay public Story
- Pin expansion
- RemoveGuard expansion
- material win claim
- front piece must move
- wins rear piece
- forced line
- best move
- only move
- winning
- decisive
- king safety
- mate threat
- pressure
- initiative
- public route `200`
- production API
- public/user-facing LLM narration

Skewer-0 opens only narrow Tactic.Skewer, one slider, one front target, one rear target, front/rear target same-line relation, a legal move that creates or reveals the front/rear relation, and bounded skewer wording after selected Verdict only.

Skewer-0 opens no broad LineTactic, XRay public Story, Pin expansion, RemoveGuard expansion, material win claim, front piece must move, wins rear piece, forced line, best move, only move, winning, decisive, king safety, mate threat, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Skewer-0 keeps skewer work at charter scope and opens no SkewerProof runtime, no Tactic.Skewer writer, no StoryTable integration, no ExplanationPlan mapping, no renderer wording, no LLM smoke, no public route `200`, and no production API.

### Skewer-1 SkewerProof

Skewer-1 opens only `SkewerProof` as a narrow proof home.

SkewerProof proves side creating the skewer, rival side, skewer slider, front target, rear target, legal skewer or revealing move, line kind, same-board proof, front target non-king material piece, rear target non-king material piece, front and rear target same rival side, after move slider attacks front target, rear target behind front target on the same ray, no extra blocker breaks the front-to-rear relation, and before move the skewer relation was absent or blocked.

SkewerProof is not a public Story.

LineFact and LineProof are not public Stories.

SkewerProof says no material gain, front piece must move, or wins rear piece.

SkewerProof proofFailures stay out of renderer/LLM input.

Skewer-1 opens no Tactic.Skewer writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration.

### Skewer-2 TacticSkewer Writer

Skewer-2 opens only the named `TacticSkewer` writer for one narrow `Tactic.Skewer` Story.

TacticSkewer requires complete StoryProof, complete SkewerProof, same-board legal replay, legal skewer or revealing move, front target, rear target, slider, complete front-and-back line relation, writer identity, and no EngineCheck Refutes status.

Skewer Story identity is tactic Skewer, scene Tactic, skewer-creating side, front/rear target side as rival, front target square, rear target square as secondaryTarget, slider or moved-piece anchor, and skewer/revealing route.

TacticSkewer creates no Scene.Material claim.

TacticSkewer does not replace Tactic.Pin.

Skewer-2 keeps rear-target king positions silent.

Skewer-2 opens no StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, Pin replacement, public route `200`, production API, or public/user-facing LLM narration.

### Skewer-3 Negative Corpus

Skewer-3 opens only the negative corpus for the narrow `Tactic.Skewer` slice.

Skewer-3 keeps illegal moves, missing same-board proof, non-sliders, missing front target, missing rear target, front or rear king targets, front/rear targets not on the same rival side, rear targets not behind the front target on the same line, extra blockers between front and rear target, DiscoveredAttack-only lines, Pin-looking positions, front-piece-must-move assumptions, and material-win, forced, or best-move wording silent.

Skewer-3 rule: Two pieces on a line is not enough. Complete front-and-back skewer proof or silence.

Skewer-3 opens no new proof home, new writer, StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, Scene.Material claim, Pin replacement, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration.

### Skewer-4 EngineCheck Reuse

Skewer-4 opens only existing `EngineCheck` reuse for existing `Tactic.Skewer` Stories.

Skewer-4 EngineCheck rules:

- EngineCheck cannot create Skewer
- Supports creates no new claim
- Caps suppresses standalone claim or weakens expression to bounded strength when downstream speech opens
- Refutes blocks the Skewer Story
- Unknown creates no engine expression

Skewer-4 forbidden openings:

- engine says
- best move
- only move
- forced win
- winning tactic
- raw PV explanation
- eval number public value
- new EngineCheck type
- Skewer from engine evidence
- StoryTable Lead admission
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.Skewer` Story; it never creates Skewer, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording.

### Skewer-5 StoryTable Integration

Skewer-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` rows.

Skewer-5 StoryTable checks:

- selected Verdict remains stable when input order changes
- Skewer does not own Material claim
- Skewer does not turn DiscoveredAttack into a duplicate Lead
- Skewer does not own Pin king relation
- Skewer does not own RemoveGuard defender relation
- actual material change now remains owned by Scene.Material
- incomplete front/rear relation leaves DiscoveredAttack or another existing row and keeps Skewer silent

Skewer-5 forbidden openings:

- new Skewer proof home
- new Story family
- broad LineTactic
- broad XRay
- Material claim from Skewer
- DiscoveredAttack duplicate Lead from Skewer
- Pin king relation from Skewer
- RemoveGuard defender relation from Skewer
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: StoryTable orders existing open rows with Skewer deterministically, keeps one selected Lead, and keeps Material, DiscoveredAttack, Pin, RemoveGuard, Defense, Fork, Hanging, and Skewer claim homes separate.

### Skewer-6 ExplanationPlan

Skewer-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Skewer` Verdicts.

Skewer-6 ExplanationPlan input is selected uncapped Lead Verdict only.

Skewer-6 allowed claim key:

- `skewers_piece_to_piece`

Skewer-6 forbidden claim keys:

- `wins_material`
- `wins_rear_piece`
- `front_piece_must_move`
- `best_move`
- `only_move`
- `forced`
- `decisive`
- `king_unsafe`
- `mate_threat`
- `creates_pressure`
- `takes_initiative`

Support, Context, Blocked, capped, and refuted Skewer rows create no standalone claim.

Skewer-6 forbidden openings:

- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Support, Context, Blocked, capped, and refuted Skewer rows create no standalone claim; selected uncapped Lead Skewer rows may lower only the bounded `skewers_piece_to_piece` claim key.

### Skewer-7 Deterministic Renderer

Skewer-7 opens only deterministic renderer text for selected `Tactic.Skewer` ExplanationPlan.

Skewer-7 renderer input is ExplanationPlan only.

Skewer-7 may render `{route} skewers the piece on {target} to the piece on {secondaryTarget}.`

Skewer-7 forbidden wording:

- wins material
- wins the piece behind it
- the front piece must move
- best move
- only move
- forces
- decisive
- king is unsafe
- threatens mate
- creates pressure

Skewer-7 opens no raw Verdict, raw Story, SkewerProof, LineFact, LineProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: DeterministicRenderer may speak only from selected Skewer ExplanationPlan and only with bounded `skewers_piece_to_piece` wording.

### Skewer-8 LLM Smoke

Skewer-8 opens only LLM smoke for selected Skewer ExplanationPlan and RenderedLine.

Skewer-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`

Skewer-8 LLM input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Skewer-8 forbidden openings:

- raw Story
- raw SkewerProof
- raw LineProof
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- new move
- new line
- material win
- forced claim
- pressure claim
- initiative claim
- mate claim
- public/user-facing LLM narration
- public route `200`
- production API
- raw proof repair
- engine explanation

Completion standard: Skewer LLM smoke accepts only rephrases no stronger than renderedText and rejects raw proof, engine, new-move, new-line, material-win, forced, pressure, initiative, and mate additions.

### Skewer Closeout Hard Cleanup

Skewer Closeout opens no new chess meaning. It only audits the Skewer hard cleanup surface.

Skewer Closeout must confirm:

- LineFact, LineProof, SkewerProof, Tactic.Skewer, and the speech key do not invade each other's authority.
- Skewer owns no Material, Hanging, Pin, DiscoveredAttack, RemoveGuard, or Defense meaning.
- `front piece must move`, `wins rear piece`, `wins material`, and `forced skewer` are not live authority.
- detailed docs authority lives only in StoryInteractionLaw.md; other live docs summarize it.
- renderer/LLM wording stays no stronger than `skewers_piece_to_piece`.
- test helpers do not become runtime authority.
- public route `200`, production API, and public/user-facing LLM narration remain closed.

Skewer Closeout duplicate checks:

- meaning duplication: no same chess meaning appears under two Story labels or two proof homes.
- authority duplication: BoardFacts, proof home, Story writer, StoryTable, ExplanationPlan, and renderer do not jointly own the same decision.
- terminology duplication: LineTactic, RayTactic, XRay, SkewerFamily, or equivalent broad names do not become live authority.
- document duplication: detailed rules repeat only here; other live docs summarize.

Skewer Closeout opens no broad LineTactic, broad Skewer family, XRay public Story, Material claim, Hanging claim, Pin expansion, DiscoveredAttack expansion, RemoveGuard expansion, Defense claim, front-piece-must-move wording, wins-rear-piece claim, wins-material claim, forced-skewer wording, pressure, initiative, mate threat, king safety, best-move, only-move, forced-line, winning, decisive, new proof home, new Story family, renderer wording beyond Skewer-7, LLM input beyond Skewer-8, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Skewer closes as a narrow proof-backed front-and-rear target slice, with no sibling meaning ownership, no broad-term authority, no duplicated detailed authority outside StoryInteractionLaw.md, no promoted test helper, and no public route, production API, or public/user-facing LLM narration.

## Line / Defender Neighborhood Closeout

Line/Defender closes as four narrow proof-backed slices. It opens no broad LineTactic, XRay, pressure, initiative, material-win tactic, or public surface.

### LNC-0 Closeout Charter

LNC-0 opens only Line / Defender neighborhood closeout.

LNC-0 closing targets:

- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`

LNC-0 related proof homes:

- `LineProof`
- `PinProof`
- `RemoveGuardProof`
- `SkewerProof`

LNC-0 existing collision targets:

- `Tactic.Hanging`
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`

LNC-0 allowed audit work:

- scope audit
- duplication audit
- authority audit
- docs simplification
- downstream no-overclaim audit
- next-neighborhood handoff

LNC-0 must confirm:

- DiscoveredAttack owns only one revealed slider attack on one non-king material target.
- Pin owns only one non-king piece pinned to its own king on a line.
- RemoveGuard owns only one removed defender guard relation from one non-king material target.
- Skewer owns only one front non-king material target and one rear non-king material target on the same line.
- LineFact observes geometry and owns no public Story.
- LineProof, PinProof, RemoveGuardProof, and SkewerProof are proof homes, not public Stories.
- Hanging, Fork, Material, and Defense keep their existing claim homes.
- ExplanationPlan, renderer, and LLM smoke stay downstream of selected Verdict data.
- detailed docs authority lives only in StoryInteractionLaw.md; other live docs summarize it.
- public route `200`, production API, and public/user-facing LLM narration remain closed.

LNC-0 duplicate checks:

- meaning duplication: no Line / Defender chess meaning appears under two Story labels or two proof homes.
- proof duplication: no proof home proves a sibling Story's distinct public claim.
- authority duplication: BoardFacts, proof home, Story writer, StoryTable, ExplanationPlan, renderer, and LLM smoke do not jointly own the same decision.
- terminology duplication: LineTactic, Ray, XRay, broad deflection, overload, pressure, initiative, and material-win tactic names do not become live authority.
- document duplication: detailed closeout rules repeat only here; other live docs summarize.

LNC-0 opens no new Story family, new proof home, new Story writer, new renderer template, new LLM behavior, XRay, broad LineTactic, broad Ray, broad deflection, overload, pressure, initiative, material win by line tactic, forced response, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Line / Defender Contact Neighborhood closes as four narrow proof-backed slices, with no sibling meaning ownership, no broad-term authority, no collision-home invasion, no duplicated detailed authority outside StoryInteractionLaw.md, no promoted test helper, no new downstream wording or LLM behavior, no public route `200`, no production API, and no public/user-facing LLM narration.

### LNC-1 Scope Audit

LNC-1 opens only Line / Defender Contact Neighborhood scope audit.

LNC-1 confirms the neighborhood has exactly four opened positive Story labels:

- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`

LNC-1 first-scope audit:

- `Tactic.DiscoveredAttack` speaks only one revealed slider attack on one non-king material target.
- `Tactic.Pin` speaks only one non-king piece pinned to its own king on a line.
- `Tactic.RemoveGuard` speaks only one removed defender guard relation from one non-king material target.
- `Tactic.Skewer` speaks only one front non-king material target and one rear non-king material target on the same line.

LNC-1 closed runtime positives:

- XRay is not an opened positive Story in this neighborhood.
- broad Ray is not an opened positive Story in this neighborhood.
- broad LineTactic is not an opened positive Story in this neighborhood.
- broad deflection is not an opened positive Story in this neighborhood.
- overload is not an opened positive Story in this neighborhood.

Closed names are not backlog inside this neighborhood. They remain closed until a separate charter opens them.

LNC-1 opens no new Story family, proof home, Story writer, renderer wording, LLM behavior, XRay, broad Ray, broad LineTactic, broad deflection, overload, pressure, initiative, material-win tactic, public route `200`, production API, or public/user-facing LLM narration.

### LNC-2 Duplication Audit

One chess meaning, one proof home, one Story label, one speech key, one live authority document.

LNC-2 opens only Line / Defender Contact Neighborhood duplication audit.

LNC-2 duplication audit checks:

- no same chess meaning is duplicated under two Story labels.
- no same proof responsibility is duplicated under two proof homes.
- no same speech claim is split across two claim keys.
- no same detailed rule repeats across multiple live documents.

LNC-2 specific ownership checks:

- DiscoveredAttack and Skewer do not duplicate ownership of `line attack`; DiscoveredAttack owns revealed slider attack on one non-king material target, while Skewer owns front-and-rear non-king material targets on one line.
- Pin and Skewer do not duplicate ownership of `front/rear line relation`; Pin owns pinned-to-own-king relation, while Skewer owns front target plus rear target relation.
- RemoveGuard does not grow into Material or Hanging precondition ownership; RemoveGuard owns only removed defender guard relation, Material owns actual material balance change, and Hanging owns capturable target with bounded material gain proof.
- LineProof does not absorb PinProof, RemoveGuardProof, or SkewerProof family-specific relations; LineProof binds only the revealed slider attack line admitted by DiscoveredAttack.
- XRay, Ray, LineTactic, and LineFamily terms are not live authority names for this neighborhood.

LNC-2 speech-key audit:

- `reveals_attack_on_piece` belongs only to `Tactic.DiscoveredAttack`.
- `pins_piece` belongs only to `Tactic.Pin`.
- `removes_defender` belongs only to `Tactic.RemoveGuard`.
- `skewers_piece_to_piece` belongs only to `Tactic.Skewer`.

LNC-2 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, XRay, Ray, LineTactic, LineFamily, broad deflection, overload, Material or Hanging precondition path, public route `200`, production API, or public/user-facing LLM narration.

### LNC-3 Authority Audit

LNC-3 opens only Line / Defender Contact Neighborhood authority audit.

LNC-3 layer authority:

- `BoardFacts.LineFact`: geometry observation only.
- `LineProof`: revealed line / attack binding only.
- `PinProof`: pinned relation only.
- `RemoveGuardProof`: guard relation removal only.
- `SkewerProof`: front/rear target relation only.
- Story writers: named proof-backed Story permission only.
- StoryTable: ordering only.
- Verdict: selected result only.
- ExplanationPlan: bounded speech claim only.
- Renderer: phrasing only.
- LLM smoke: rephrase only.

LNC-3 forbidden authority shortcuts:

- proof home must not speak like a Story label.
- Story writer must not own material result.
- StoryTable must not create new chess meaning.
- ExplanationPlan must not read raw proof.
- Renderer and LLM smoke must not repair or upgrade proof.

LNC-3 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, raw proof downstream path, material-result ownership by Story writer, StoryTable-created meaning, public route `200`, production API, or public/user-facing LLM narration.

### LNC-4 Collision Audit

LNC-4 opens only Line / Defender Contact Neighborhood collision audit.

LNC-4 collision targets:

- DiscoveredAttack vs Pin.
- DiscoveredAttack vs Skewer.
- Pin vs Skewer.
- RemoveGuard vs Material.
- RemoveGuard vs Hanging.
- Skewer vs Material.
- Line/Defender row vs Defense.
- EngineCheck Caps/Refutes over each line/defender row.

LNC-4 verification criteria:

- input order stable.
- no duplicate Lead.
- incomplete row is not Lead.
- capped and refuted rows create no standalone text.
- actual material change now stays in Scene.Material home.
- material gain proof stays in Hanging or Material home.
- line/defender rows speak only their own relation.

LNC-4 existing runtime coverage:

- `Pin-5 StoryTable prevents duplicate Lead for same-line DiscoveredAttack and Pin` covers DiscoveredAttack vs Pin.
- `Skewer-5 separates DiscoveredAttack collision and keeps incomplete Skewer silent` covers DiscoveredAttack vs Skewer.
- `Skewer-5 keeps Material Pin and RemoveGuard claim homes separate` covers Pin vs Skewer and Skewer vs Material.
- `LDH-1 fixture map covers complex same-board line defender interactions` covers RemoveGuard vs Material, RemoveGuard vs Hanging, and Line/Defender row vs Defense.
- `LDH-1 fixture map covers EngineCheck statuses over existing line defender rows` covers EngineCheck Caps/Refutes over DiscoveredAttack, Pin, RemoveGuard, and Skewer.

LNC-4 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, broad LineTactic, XRay, material-win tactic, public route `200`, production API, or public/user-facing LLM narration.

### LNC-5 Downstream Boundary Audit

LNC-5 opens only Line / Defender Contact Neighborhood downstream boundary audit.

LNC-5 downstream authority:

- selected uncapped Lead Verdict only may lower to ExplanationPlan.
- ExplanationPlan input is selected Verdict only.
- Renderer input is ExplanationPlan only.
- LLM smoke input is renderedText, claimKey, strength, forbidden wording, and rephrase-only instruction only.
- Support, Context, Blocked, capped, and refuted rows create no standalone text.

LNC-5 forbidden downstream wording:

- wins material.
- winning / decisive.
- best move / only move.
- forced.
- cannot move.
- no defense.
- front piece must move.
- wins rear piece.
- pressure / initiative.
- mate threat / king unsafe.

LNC-5 existing runtime coverage:

- `LNC-5 Downstream Boundary Audit keeps Line Defender speech bounded` covers DiscoveredAttack, Pin, RemoveGuard, and Skewer downstream handoff and forbidden wording.
- `LDH-6 Downstream Boundary Smoke sends only selected Lead Verdicts to text stages` covers existing Line/Defender selected Lead handoff.
- `Skewer-6 ExplanationPlan gives no standalone claim to non Lead capped refuted or unselected Skewer rows` covers Skewer non-Lead silence.

LNC-5 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration.

### LNC-6 Documentation Simplification

LNC-6 opens only Line / Defender Contact Neighborhood documentation simplification.

LNC-6 documentation authority:

- detailed closeout authority lives only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, AGENTS, and LegacyPruneManifest carry summaries only.
- the same closeout rule must not repeat across live documents.
- closed terms must read as closed summaries, not backlog.
- Line family, Ray family, XRay, and line tactic wording must not read as live authority.

LNC-6 verification:

- docs authority tests enforce summary-only downstream documents.
- detailed closeout checklist appears in exactly one live document.
- live authority lists stay aligned across AGENTS.md, README, LegacyPruneManifest, and docs tests.

LNC-6 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration.

### LNC-7 Test Helper / Runtime Boundary Audit

LNC-7 opens only Line / Defender Contact Neighborhood test helper / runtime boundary audit.

LNC-7 runtime boundary:

- test helpers must not become runtime authority.
- fixture names must not read like new Story families.
- negative corpus helpers must not become production concepts.
- forbidden wording checks must reject public wording without treating snake_case internal field names as public prose.
- runtime source must not contain closeout-only terms.

LNC-7 verification:

- docs authority tests pin this section in StoryInteractionLaw only.
- runtime boundary tests scan production source for closeout-only terminology.
- LLM smoke tests keep forbidden wording matching on public phrases, not internal field keys.

LNC-7 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration.

### LNC Closeout Final Completion Standard

LNC Closeout final completion standard:

- no new Story family.
- no new proof home.
- no new renderer wording.
- no new LLM behavior.
- only `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` remain as the positive closeout baseline slices.
- no duplicate meaning.
- no duplicate authority.
- no duplicate terminology.
- no duplicate detailed docs.
- public route, production API, and public/user-facing LLM narration remain closed.

LNC final verification:

- docs authority tests pass.
- chess foundation tests pass.
- `git diff --check` is clean.

Final LNC closeout opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration.

## DefenderDuty Proof Readiness

### Stage-0 DefenderDuty Proof Readiness Charter

Stage-0 opens internal proof-readiness only.

Stage-0 opens only the defender-duty proof shape.

Stage-0 meaning: on one exact board, one named rival defender guards one named
rival non-king material target.

DefenderDuty is an internal readiness name only. It is not a public label,
Story label, speech key, tactic family, plan family, renderer input, LLM input,
public payload, or production API contract.

Stage-0 proof shape:

- exact board identity
- observing side
- rival side
- one rival defender
- one rival non-king material target
- defender and target are different pieces
- defender and target are both present on the exact board
- defender and target belong to the rival side
- defender attacks or guards the target square on the exact board
- same-board proof
- diagnostic missing-evidence shape

Stage-0 rules:

- The proof shape records one defender-target guard relation only.
- The proof shape does not prove the defender is the only defender.
- The proof shape does not prove the defender is overloaded.
- The proof shape does not prove the defender cannot move.
- The proof shape does not prove that moving, capturing, deflecting, decoying,
  or removing the defender wins material.
- BoardFacts, `PieceContact`, guard rows, line facts, source rows, proof
  failures, renderer text, and LLM text cannot create public defender-duty
  meaning.
- Missing evidence and proof failure text stay internal diagnostics only.

Stage-0 must not open:

- public Story
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- `DefenderDuty` public label
- `DefenderDutyProof` as public claim
- speech key
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- only defender
- overloaded defender
- defender cannot move
- removes defender
- wins material
- pressure / initiative
- best / only / forced
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Stage-0 documentation rule:

- detailed DefenderDuty readiness authority lives only in
  `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Stage-0 DefenderDuty Proof Readiness closes when the
charter admits only an internal proof-readiness shape for one exact-board
rival defender guarding one rival non-king material target; no public Story,
tactic, plan, public `DefenderDuty` label, public `DefenderDutyProof` claim,
speech key, StoryTable Lead, ExplanationPlan, renderer, LLM smoke,
only-defender, overloaded-defender, defender-cannot-move, removes-defender,
wins-material, pressure, initiative, best/only/forced, mate-threat, public
route `200`, production API, or public/user-facing LLM narration surface is
opened; detailed authority stays only in `StoryInteractionLaw.md`; AGENTS.md
remains unchanged; docs authority tests pass when touched; and
`git diff --check` passes.

### Stage-1 DefenderDuty Proof Shape

Stage-1 opens only the internal defender-duty proof shape.

Stage-1 may use `DefenderDutyProof` as an internal sidecar holder if implementation needs a named holder.

If existing guard observations are enough, Stage-1 documents the same shape without making it public.

Stage-1 required proof fields:

- defending side
- rival attacking side
- defender piece identity
- defender square
- defended target identity
- defended target square
- target is same-side as defender
- target is non-king
- target is material piece
- defender legally guards or attacks the target square on the exact board
- same-board proof
- guard relation source
- missing evidence
- complete flag
- `publicClaimAllowed = false`

Stage-1 rules:

- Proof home is not public Story.
- Proof home cannot write Story.
- Proof home cannot create Verdict, ExplanationPlan, renderer text, LLM input, public route, or API output.
- BoardFacts may observe attacks and guards, but cannot create defender-duty public meaning.
- `DefenderDutyProof`, if implemented, remains an internal sidecar only.
- Missing evidence remains diagnostic only and cannot become renderer text, LLM input, public route, or API output.

Stage-1 must not open:

- public Story
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public `DefenderDuty` label
- public `DefenderDutyProof` claim
- speech key
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- only defender
- overloaded defender
- defender cannot move
- removes defender
- wins material
- pressure / initiative
- best / only / forced
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-1 DefenderDuty Proof Shape closes when the internal
shape, or internal `DefenderDutyProof` sidecar if implementation needs a named
holder, contains defending side, rival attacking side, defender identity and
square, defended target identity and square, same-side target proof,
non-king material target proof, exact-board legal guard or attack proof,
same-board proof, guard relation source, missing evidence, complete flag, and
`publicClaimAllowed = false`; the proof home remains unable to write Story,
Verdict, ExplanationPlan, renderer text, LLM input, public route, or API
output; BoardFacts remains observation only; and public Story, Overload,
Deflect, Decoy, Plan.Overload, public DefenderDuty labels, speech keys,
StoryTable Lead, downstream text, public route `200`, production API, and
public/user-facing LLM narration remain closed.

### Stage-2 DefenderDuty Positive Readiness Fixture

Stage-2 opens one narrow positive proof-readiness fixture only.

Stage-2 fixture meaning:

- one rival defender protects one rival non-king material target
- defender and target are both identified
- guard relation is legal on the exact board
- same-board proof is present

Stage-2 allowed target types:

- queen
- rook
- bishop
- knight
- pawn

Stage-2 forbidden target:

- king

Stage-2 expected result:

- internal proof complete
- no public Story
- no StoryTable Lead
- no ExplanationPlan
- no rendered text
- no LLM smoke

Stage-2 fixture rules:

- The positive fixture may satisfy only the Stage-1 internal proof shape.
- The positive fixture must keep `publicClaimAllowed = false`.
- The positive fixture cannot imply only defender, overloaded defender, defender cannot move, removes defender, wins material, pressure, initiative, best, only, forced, or mate threat.
- A king target is incomplete for DefenderDuty readiness and must remain diagnostic only.
- BoardFacts guard observation may supply the guard relation source, but BoardFacts still cannot create defender-duty public meaning.

Stage-2 must not open:

- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- speech key
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-2 DefenderDuty Positive Readiness Fixture closes when exactly one positive internal fixture proves one identified rival defender
legally guards one identified rival queen, rook, bishop, knight, or pawn on
the exact same board; king targets remain forbidden; the internal proof is
complete with `publicClaimAllowed = false`; and the fixture creates no public
Story, no StoryTable Lead, no ExplanationPlan, no rendered text, no LLM smoke,
no tactic or plan family, no speech key, no public route `200`, no production
API, and no public/user-facing LLM narration.

### Stage-3 DefenderDuty Negative Corpus

Stage-3 opens only the DefenderDuty negative corpus.

Stage-3 incomplete or silent cases:

- missing same-board proof
- missing defender
- missing target
- defender and target on opposite sides
- target is king
- defender does not legally guard target square
- guard line blocked
- stale before-board relation only
- source row says defender without proof
- EngineCheck says defender without proof
- BoardFacts-only observation treated as public claim
- ambiguous multiple defenders without one selected relation
- ambiguous multiple targets without one selected relation
- defender is pinned but pin effect is not part of this proof
- target is attacked but not guarded by named defender
- target is loose
- target is hanging
- material is won
- check/mate/mate threat context

Stage-3 forbidden wording:

- only defender
- overloaded
- deflects
- decoys
- removes guard
- wins material
- pressure
- initiative
- best / only / forced

Stage-3 expected result:

- incomplete internal proof or silence
- no public Story
- no StoryTable Lead
- no ExplanationPlan
- no rendered text
- no LLM smoke

Stage-3 rules:

- Negative fixtures may fail readiness, but they do not create public claims.
- Missing same-board proof, stale before-board relation, source-row text, and EngineCheck text remain diagnostics only.
- BoardFacts-only guard observations cannot become public defender-duty meaning.
- Multiple defenders or multiple targets require one selected defender-target relation; otherwise the proof remains incomplete.
- Pin, Loose, Hanging, Material, check, mate, and mate-threat contexts remain sibling or closed meanings, not DefenderDuty proof effects.
- Stage-3 does not open public Story, StoryTable Lead, ExplanationPlan, renderer text, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Stage-3 DefenderDuty Negative Corpus closes when every listed missing, ambiguous, stale, unproven, BoardFacts-only, EngineCheck-only, source-only, sibling-meaning, material, check, mate, and mate-threat case remains incomplete or silent; forbidden only-defender, overloaded, deflects, decoys, removes-guard, wins-material, pressure, initiative, best/only/forced wording remains closed; and no public Story, StoryTable Lead, ExplanationPlan, renderer text, LLM smoke, public route `200`, production API, or public/user-facing LLM narration is opened.

### Stage-4 DefenderDuty Existing Owner Collision Audit

Stage-4 opens only the existing owner collision audit for DefenderDuty readiness.

Stage-4 collision targets:

- `Tactic.RemoveGuard`
- `Scene.Defense`
- `Tactic.Loose`
- `Tactic.Hanging`
- `Scene.Material`
- `Tactic.QueenHit`
- `Tactic.Fork`
- `Tactic.Skewer`
- `Tactic.Pin`
- `Tactic.DiscoveredAttack`

Stage-4 rules:

- DefenderDuty proof does not replace RemoveGuardProof.
- DefenderDuty proof does not create RemoveGuard.
- DefenderDuty proof does not create Defense.
- DefenderDuty proof does not prove a threat is stopped.
- DefenderDuty proof does not prove a target is loose or hanging.
- DefenderDuty proof does not prove material gain.
- DefenderDuty proof does not prove pin, skewer, fork, or discovered attack.
- Existing opened Stories keep their own proof homes and speech keys.

Stage-4 expected result:

- internal defender-duty relation may be present as diagnostic/proof-readiness only.
- all public Story labels remain unchanged.

Stage-4 owner boundaries:

- RemoveGuard meaning stays in `RemoveGuardProof`, `Tactic.RemoveGuard`, and `removes_defender`.
- Defense meaning stays in ThreatProof/DefenseProof, `Scene.Defense`, and its defense speech key.
- Loose meaning stays in `LoosePieceProof`, `Tactic.Loose`, and `attacks_loose_piece`.
- Hanging meaning stays in the existing Hanging proof path, `Tactic.Hanging`, and `can_win_piece`.
- Material meaning stays in the existing material proof path, `Scene.Material`, and `material_balance_changes`.
- QueenHit, Fork, Skewer, Pin, and DiscoveredAttack keep their own proof homes, Story labels, and speech keys.

Stage-4 must not open:

- new public Story
- new proof home
- new StoryTable Lead path
- new ExplanationPlan mapping
- new renderer text
- new LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-4 DefenderDuty Existing Owner Collision Audit closes when DefenderDuty proof remains diagnostic/proof-readiness only; it does not replace RemoveGuardProof, create RemoveGuard or Defense, prove a stopped threat, prove loose or hanging status, prove material gain, or prove pin, skewer, fork, or discovered attack; `Tactic.RemoveGuard`, `Scene.Defense`, `Tactic.Loose`, `Tactic.Hanging`, `Scene.Material`, `Tactic.QueenHit`, `Tactic.Fork`, `Tactic.Skewer`, `Tactic.Pin`, and `Tactic.DiscoveredAttack` keep their existing proof homes, Story labels, and speech keys; all public Story labels remain unchanged; and no StoryTable Lead, ExplanationPlan, renderer, LLM smoke, public route `200`, production API, or public/user-facing LLM narration is opened.

### Stage-5 DefenderDuty EngineCheck Diagnostics Boundary

Stage-5 opens diagnostic boundary tests only.

Stage-5 rules:

- EngineCheck cannot create DefenderDuty proof.
- EngineCheck cannot create Overload, Deflect, Decoy, RemoveGuard, Defense, Material, or Hanging from DefenderDuty.
- Supports creates no claim.
- Caps creates no claim.
- Refutes creates no public defender-duty text.
- Unknown creates no expression.
- proofFailures remain internal diagnostics only.
- raw PV and eval remain non-public.

Stage-5 forbidden diagnostics wording:

- engine says defender
- engine says overloaded
- eval proves defender
- PV proves defender
- best move
- only move
- forced

Stage-5 diagnostic status boundaries:

- `supports` may annotate an already complete internal proof-readiness relation, but it does not complete missing DefenderDuty evidence.
- `caps` may limit confidence for an already complete internal relation, but it does not create public claim strength.
- `refutes` may keep the internal relation incomplete or diagnostic-only, but it does not create public defender-duty wording.
- `unknown` is silent and cannot be lowered into ExplanationPlan, renderer text, LLM input, public route output, or production API output.

Stage-5 must not open:

- DefenderDuty public Story
- Overload, Deflect, Decoy, RemoveGuard, Defense, Material, or Hanging Story creation from DefenderDuty
- StoryTable Lead path
- ExplanationPlan mapping
- renderer text
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-5 DefenderDuty EngineCheck Diagnostics Boundary closes when EngineCheck status, raw PV, raw eval, source rows, and proofFailures remain internal diagnostics only; EngineCheck cannot create DefenderDuty proof or create Overload, Deflect, Decoy, RemoveGuard, Defense, Material, or Hanging from DefenderDuty; supports and caps create no claim; refutes creates no public defender-duty text; unknown creates no expression; forbidden engine-says-defender, engine-says-overloaded, eval-proves-defender, PV-proves-defender, best-move, only-move, and forced wording remains closed; and no StoryTable Lead, ExplanationPlan, renderer, LLM smoke, public route `200`, production API, or public/user-facing LLM narration is opened.

### Stage-6 DefenderDuty Docs Public Surface Boundary

Stage-6 opens docs and public-surface boundary tests only.

Stage-6 docs rules:

- detailed DefenderDuty readiness authority lives only in `StoryInteractionLaw.md`.
- README/SSOT/Architecture/Contract/Manifest stay summary-only if touched.
- AGENTS.md remains unchanged unless durable operator rules change.
- docs tests must prevent detailed readiness duplication outside `StoryInteractionLaw.md`.

Stage-6 public surface rules:

- `/api/commentary/render` remains fail-closed.
- `/internal/commentary/render-local-probe` remains fail-closed.
- no public route 200
- no production API
- no public/user-facing LLM narration

Stage-6 downstream rules:

- no ExplanationPlan
- no Renderer
- no LLM smoke
- no speech key

Stage-6 summary-only allowance:

- Summary docs may say DefenderDuty readiness is internal and non-public only if docs tests require a summary.
- Summary docs must not carry proof fields, fixture details, negative corpus details, collision targets, EngineCheck diagnostics rules, public-surface detail, forbidden wording, or completion standards.
- Summary docs must point to `StoryInteractionLaw.md` for detailed authority when a pointer is needed.

Stage-6 must not open:

- public Story
- `DefenderDuty` public label
- `DefenderDutyProof` as public claim
- StoryTable Lead path
- ExplanationPlan mapping
- renderer text
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-6 DefenderDuty Docs Public Surface Boundary closes when detailed DefenderDuty readiness authority remains only in `StoryInteractionLaw.md`; README, SSOT, Architecture, Contract, and Manifest remain summary-only if touched; `AGENTS.md` remains unchanged unless durable operator rules change; docs tests prevent detailed readiness duplication outside `StoryInteractionLaw.md`; `/api/commentary/render` and `/internal/commentary/render-local-probe` remain fail-closed; no public route 200, production API, public/user-facing LLM narration, ExplanationPlan, Renderer, LLM smoke, or speech key is opened.

### Stage-7 DefenderDuty Readiness Closeout

Stage-7 closes only internal proof-readiness for one defender guarding one non-king material target.

Stage-7 authority audit:

- DefenderDuty proof shape is internal only.
- BoardFacts observes only.
- Story writers do not consume it yet.
- StoryTable does not order it.
- ExplanationPlan does not lower it.
- Renderer does not phrase it.
- LLM does not see it.

Stage-7 still closed:

- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public DefenderDuty label
- only defender
- overloaded defender
- defender cannot move
- removes defender
- wins material
- pressure / initiative
- best / only / forced
- mate threat
- public route 200
- production API
- public/user-facing LLM narration

Stage-7 required verification:

- targeted DefenderDuty readiness tests
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Stage-7 closeout notes:

- The only closed positive readiness is one exact same-board defender-target guard relation.
- Existing proof homes remain authoritative for their own public Stories.
- DefenderDuty readiness does not create a public Story label, speech key, StoryTable row, ExplanationPlan claim key, renderer template, LLM prompt input, public route response, or production API output.
- Detailed DefenderDuty readiness authority remains in this section and the preceding DefenderDuty stages of `StoryInteractionLaw.md` only.

Completion standard: DefenderDuty readiness closes when one exact same-board defender-target guard relation can be proven internally, every public Story path remains closed, existing proof homes are not replaced, public surfaces remain fail-closed, docs authority is not duplicated, and all verification passes.

## DualDefenderDuty Proof Readiness

### Stage-0 DualDefenderDuty Proof Readiness Charter

Stage-0 opens only internal proof-readiness for one defender with two separate
guard duties on the exact board.

Stage-0 meaning:

- the same defender guards two distinct targets
- both targets are non-king material pieces
- both targets are same-side as the defender
- both guard relations are legal on the exact board

Stage-0 proof-readiness shape:

- defending side
- rival side
- defender piece identity
- defender square
- first defended target identity
- first defended target square
- second defended target identity
- second defended target square
- target squares are distinct
- both targets are non-king material pieces
- both targets belong to the defender side
- defender legally guards or attacks both target squares on the exact board
- same-board proof
- guard relation source for each target
- diagnostic missing-evidence shape
- public claim allowed is false

Stage-0 rules:

- DualDefenderDuty readiness is internal proof-readiness only.
- DualDefenderDuty readiness is not public Story.
- DualDefenderDuty readiness is not `Tactic.Overload`,
  `Tactic.Deflect`, `Tactic.Decoy`, or `Plan.Overload`.
- DualDefenderDuty readiness does not prove the defender cannot satisfy both
  duties.
- DualDefenderDuty readiness does not prove the defender is overloaded.
- DualDefenderDuty readiness does not prove either target has only one
  defender.
- DualDefenderDuty readiness does not prove the defender cannot move.
- DualDefenderDuty readiness does not prove material is won.
- BoardFacts, guard rows, line facts, source rows, proof failures,
  EngineCheck diagnostics, renderer text, and LLM text cannot create public
  DualDefenderDuty meaning.
- Missing evidence and proof failure text stay internal diagnostics only.

Stage-0 must not open:

- public Story
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public DualDefenderDuty label
- speech key
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- defender cannot satisfy both
- overloaded defender
- only defender
- defender cannot move
- wins material
- pressure / initiative
- best / only / forced
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Stage-0 documentation rule:

- detailed DualDefenderDuty readiness authority lives only in
  `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Stage-0 DualDefenderDuty Proof Readiness closes when the
charter admits only an internal proof-readiness shape for one exact-board
defender guarding two distinct same-side non-king material targets; no public
Story, tactic, plan, public DualDefenderDuty label, speech key, StoryTable
Lead, ExplanationPlan, renderer, LLM smoke, defender-cannot-satisfy-both,
overloaded-defender, only-defender, defender-cannot-move, wins-material,
pressure, initiative, best/only/forced, mate-threat, public route `200`,
production API, or public/user-facing LLM narration surface is opened;
detailed authority stays only in `StoryInteractionLaw.md`; AGENTS.md remains
unchanged; docs authority tests pass when touched; and `git diff --check`
passes.

### Stage-1 DualDefenderDuty Proof Shape

Stage-1 opens only the internal dual-defender-duty proof shape.

Stage-1 may use `DualDefenderDutyProof` as an internal sidecar holder if
implementation needs a named holder.

If existing guard observations are enough, Stage-1 documents the same exact
shape without adding runtime public authority.

Stage-1 DualDefenderDuty required proof fields:

- defending side
- rival side
- defender piece identity
- defender square
- first defended target identity
- first defended target square
- second defended target identity
- second defended target square
- targets are distinct
- both targets are same-side as defender
- both targets are non-king
- both targets are material pieces
- defender legally guards first target on exact board
- defender legally guards second target on exact board
- same-board proof
- first guard relation source
- second guard relation source
- missing evidence
- complete flag
- `publicClaimAllowed = false`

Stage-1 DualDefenderDuty rules:

- Proof shape is not public Story.
- Proof shape cannot write Story.
- Proof shape cannot create Verdict, ExplanationPlan, renderer text,
  LLM input, public route, or API output.
- BoardFacts may observe attacks and guards, but cannot create dual-duty
  public meaning.
- `DualDefenderDutyProof`, if implemented, remains an internal sidecar only.
- Missing evidence remains diagnostic only and cannot become renderer text,
  LLM input, public route, or API output.

Stage-1 must not open:

- public Story
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public DualDefenderDuty label
- public `DualDefenderDutyProof` claim
- speech key
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- defender cannot satisfy both
- overloaded defender
- only defender
- defender cannot move
- wins material
- pressure / initiative
- best / only / forced
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-1 DualDefenderDuty Proof Shape closes when the
internal shape, or internal `DualDefenderDutyProof` sidecar if implementation
needs a named holder, contains defending side, rival side, defender identity
and square, first and second defended target identities and squares, distinct
target proof, same-side target proof, non-king target proof, material-piece
target proof, exact-board legal guard proof for each target, same-board proof,
first and second guard relation sources, missing evidence, complete flag, and
`publicClaimAllowed = false`; the proof shape remains unable to write Story,
Verdict, ExplanationPlan, renderer text, LLM input, public route, or API
output; BoardFacts remains observation only; and public Story, Overload,
Deflect, Decoy, Plan.Overload, public DualDefenderDuty labels, speech keys,
StoryTable Lead, downstream text, public route `200`, production API, and
public/user-facing LLM narration remain closed.

### Stage-2 DualDefenderDuty Positive Readiness Fixture

Stage-2 opens one narrow positive readiness fixture only.

Stage-2 fixture meaning:

- one defender protects two distinct same-side non-king material targets
- defender is identified
- target A is identified
- target B is identified
- both guard relations are legal on the exact board
- same-board proof is present

Stage-2 allowed target types:

- queen
- rook
- bishop
- knight
- pawn

Stage-2 forbidden target:

- king

Stage-2 expected result:

- internal proof complete
- no public Story
- no StoryTable Lead
- no ExplanationPlan
- no renderer
- no LLM smoke
- no `Tactic.Overload`

Stage-2 fixture rules:

- The positive fixture may satisfy only the Stage-1 internal proof shape.
- The positive fixture must keep `publicClaimAllowed = false`.
- The positive fixture cannot imply defender cannot satisfy both duties,
  overloaded defender, only defender, defender cannot move, wins material,
  pressure, initiative, best, only, forced, or mate threat.
- A king target is incomplete for DualDefenderDuty readiness and must remain
  diagnostic only.
- BoardFacts guard observation may supply each guard relation source, but
  BoardFacts still cannot create dual-duty public meaning.

Stage-2 must not open:

- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public DualDefenderDuty label
- speech key
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-2 DualDefenderDuty Positive Readiness Fixture
closes when exactly one positive internal fixture proves one identified
defender legally guards two identified distinct same-side queen, rook, bishop,
knight, or pawn targets on the exact same board; king targets remain
forbidden; the internal proof is complete with `publicClaimAllowed = false`;
and the fixture creates no public Story, no StoryTable Lead, no
ExplanationPlan, no renderer, no LLM smoke, no `Tactic.Overload`, no tactic
or plan family, no speech key, no public route `200`, no production API, and
no public/user-facing LLM narration.

### Stage-3 DualDefenderDuty Negative Corpus

Stage-3 opens only the DualDefenderDuty negative corpus.

Stage-3 incomplete or silent cases:

- missing same-board proof
- missing defender
- missing first target
- missing second target
- duplicated target
- defender and target A on opposite sides
- defender and target B on opposite sides
- target A is king
- target B is king
- target A is non-material
- target B is non-material
- defender guards only one target
- defender guards neither target
- one guard line is blocked
- both guard relations are stale before-board-only facts
- source row says dual duty without proof
- EngineCheck says dual duty without proof
- BoardFacts-only observation treated as public claim
- ambiguous multiple defenders without one selected defender
- ambiguous multiple target pairs without one selected pair
- one target is loose
- one target is hanging
- material is won
- check/mate/mate threat context

Stage-3 forbidden wording:

- overloaded
- cannot satisfy both
- overworked
- only defender
- deflects
- decoys
- removes guard
- wins material
- pressure
- initiative
- best / only / forced

Stage-3 expected result:

- incomplete internal proof or silence
- no public Story
- no StoryTable Lead
- no ExplanationPlan
- no renderer
- no LLM smoke
- no `Tactic.Overload`

Stage-3 rules:

- Negative fixtures may fail readiness, but they do not create public claims.
- Missing same-board proof, stale before-board guard facts, source-row text,
  and EngineCheck text remain diagnostics only.
- BoardFacts-only guard observations cannot become public dual-duty meaning.
- Multiple defenders require one selected defender; otherwise proof remains
  incomplete.
- Multiple target pairs require one selected target pair; otherwise proof
  remains incomplete.
- Loose, Hanging, Material, check, mate, and mate-threat contexts remain
  sibling or closed meanings, not DualDefenderDuty proof effects.
- Stage-3 does not open public Story, StoryTable Lead, ExplanationPlan,
  renderer text, LLM smoke, public route `200`, production API, or
  public/user-facing LLM narration.

Completion standard: Stage-3 DualDefenderDuty Negative Corpus closes when
every listed missing, duplicated, opposite-side, king-target, non-material,
one-guard-only, no-guard, blocked-line, stale, unproven, BoardFacts-only,
EngineCheck-only, source-only, ambiguous-defender, ambiguous-target-pair,
sibling-meaning, material, check, mate, and mate-threat case remains
incomplete or silent; forbidden overloaded, cannot-satisfy-both, overworked,
only-defender, deflects, decoys, removes-guard, wins-material, pressure,
initiative, and best/only/forced wording remains closed; and no public Story,
StoryTable Lead, ExplanationPlan, renderer text, LLM smoke, `Tactic.Overload`,
public route `200`, production API, or public/user-facing LLM narration is
opened.

### Stage-4 DualDefenderDuty Existing Owner Collision Audit

Stage-4 opens only the existing owner collision audit for DualDefenderDuty
readiness.

Stage-4 collision targets:

- `DefenderDuty` readiness
- `Tactic.RemoveGuard`
- `Scene.Defense`
- `Tactic.Loose`
- `Tactic.Hanging`
- `Scene.Material`
- `Tactic.QueenHit`
- `Tactic.Fork`
- `Tactic.Skewer`
- `Tactic.Pin`
- `Tactic.DiscoveredAttack`

Stage-4 rules:

- DualDefenderDuty proof does not replace DefenderDuty readiness.
- DualDefenderDuty proof may compose two DefenderDuty relations internally,
  but it does not create public meaning.
- DualDefenderDuty proof does not create RemoveGuard.
- DualDefenderDuty proof does not create Defense.
- DualDefenderDuty proof does not prove a threat is stopped.
- DualDefenderDuty proof does not prove a target is loose or hanging.
- DualDefenderDuty proof does not prove material gain.
- DualDefenderDuty proof does not prove pin, skewer, fork, or discovered
  attack.
- Existing opened Stories keep their own proof homes and speech keys.

Stage-4 expected result:

- internal dual-duty relation may be present as diagnostic/proof-readiness
  only.
- all public Story labels remain unchanged.

Stage-4 owner boundaries:

- Single-target DefenderDuty readiness stays its own internal readiness shape.
- RemoveGuard meaning stays in `RemoveGuardProof`, `Tactic.RemoveGuard`, and
  `removes_defender`.
- Defense meaning stays in ThreatProof/DefenseProof, `Scene.Defense`, and its
  defense speech key.
- Loose meaning stays in `LoosePieceProof`, `Tactic.Loose`, and
  `attacks_loose_piece`.
- Hanging meaning stays in the existing Hanging proof path, `Tactic.Hanging`,
  and `can_win_piece`.
- Material meaning stays in the existing material proof path,
  `Scene.Material`, and `material_balance_changes`.
- QueenHit, Fork, Skewer, Pin, and DiscoveredAttack keep their own proof homes,
  Story labels, and speech keys.

Stage-4 must not open:

- new public Story
- new proof home
- new StoryTable Lead path
- new ExplanationPlan mapping
- new renderer text
- new LLM smoke
- overloaded defender
- cannot satisfy both
- overworked defender
- only defender
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-4 DualDefenderDuty Existing Owner Collision Audit
closes when DualDefenderDuty proof remains diagnostic/proof-readiness only; it
does not replace DefenderDuty readiness, create RemoveGuard or Defense, prove
a stopped threat, prove loose or hanging status, prove material gain, or prove
pin, skewer, fork, or discovered attack; `DefenderDuty` readiness,
`Tactic.RemoveGuard`, `Scene.Defense`, `Tactic.Loose`, `Tactic.Hanging`,
`Scene.Material`, `Tactic.QueenHit`, `Tactic.Fork`, `Tactic.Skewer`,
`Tactic.Pin`, and `Tactic.DiscoveredAttack` keep their existing proof homes,
Story labels, and speech keys; all public Story labels remain unchanged; and
no StoryTable Lead, ExplanationPlan, renderer, LLM smoke, public route `200`,
production API, or public/user-facing LLM narration is opened.

### Stage-5 DualDefenderDuty EngineCheck Diagnostics Boundary

Stage-5 opens diagnostic boundary tests only.

Stage-5 rules:

- EngineCheck cannot create DualDefenderDuty proof.
- EngineCheck cannot create Overload, Deflect, Decoy, RemoveGuard, Defense,
  Material, Hanging, or Loose from DualDefenderDuty.
- Supports creates no claim.
- Caps creates no claim.
- Refutes creates no public dual-duty text.
- Unknown creates no expression.
- proofFailures remain internal diagnostics only.
- raw PV and eval remain non-public.

Stage-5 forbidden diagnostics wording:

- engine says overloaded
- engine says dual duty
- eval proves dual duty
- PV proves dual duty
- best move
- only move
- forced

Stage-5 diagnostic status boundaries:

- `supports` may annotate an already complete internal proof-readiness
  relation, but it does not complete missing DualDefenderDuty evidence.
- `caps` may limit confidence for an already complete internal relation, but
  it does not create public claim strength.
- `refutes` may keep the internal relation incomplete or diagnostic-only, but
  it does not create public dual-duty wording.
- `unknown` is silent and cannot be lowered into ExplanationPlan, renderer
  text, LLM input, public route output, or production API output.

Stage-5 must not open:

- DualDefenderDuty public Story
- Overload, Deflect, Decoy, RemoveGuard, Defense, Material, Hanging, or Loose
  Story creation from DualDefenderDuty
- StoryTable Lead path
- ExplanationPlan mapping
- renderer text
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-5 DualDefenderDuty EngineCheck Diagnostics Boundary
closes when EngineCheck status, raw PV, raw eval, source rows, and
proofFailures remain internal diagnostics only; EngineCheck cannot create
DualDefenderDuty proof or create Overload, Deflect, Decoy, RemoveGuard,
Defense, Material, Hanging, or Loose from DualDefenderDuty; supports and caps
create no claim; refutes creates no public dual-duty text; unknown creates no
expression; forbidden engine-says-overloaded, engine-says-dual-duty,
eval-proves-dual-duty, PV-proves-dual-duty, best-move, only-move, and forced
wording remains closed; and no StoryTable Lead, ExplanationPlan, renderer,
LLM smoke, public route `200`, production API, or public/user-facing LLM
narration is opened.

### Stage-6 DualDefenderDuty Docs Public Surface Boundary

Stage-6 opens docs and public-surface boundary tests only.

Stage-6 docs rules:

- detailed DualDefenderDuty readiness authority lives only in
  `StoryInteractionLaw.md`.
- README/SSOT/Architecture/Contract/Manifest stay summary-only if touched.
- AGENTS.md remains unchanged unless durable operator rules change.
- docs tests must prevent detailed readiness duplication outside
  `StoryInteractionLaw.md`.

Stage-6 public surface rules:

- `/api/commentary/render` remains fail-closed.
- `/internal/commentary/render-local-probe` remains fail-closed.
- no public route 200
- no production API
- no public/user-facing LLM narration

Stage-6 downstream rules:

- no ExplanationPlan
- no Renderer
- no LLM smoke
- no speech key

Stage-6 summary-only allowance:

- Summary docs may say DualDefenderDuty readiness is internal and non-public
  only if docs tests require a summary.
- Summary docs must not carry proof fields, fixture details, negative corpus
  details, collision targets, EngineCheck diagnostics rules, public-surface
  detail, forbidden wording, or completion standards.
- Summary docs must point to `StoryInteractionLaw.md` for detailed authority
  when a pointer is needed.

Stage-6 must not open:

- public Story
- public DualDefenderDuty label
- public `DualDefenderDutyProof` claim
- StoryTable Lead path
- ExplanationPlan mapping
- renderer text
- LLM smoke
- speech key
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-6 DualDefenderDuty Docs Public Surface Boundary
closes when detailed DualDefenderDuty readiness authority remains only in
`StoryInteractionLaw.md`; README, SSOT, Architecture, Contract, and Manifest
remain summary-only if touched; `AGENTS.md` remains unchanged unless durable
operator rules change; docs tests prevent detailed readiness duplication
outside `StoryInteractionLaw.md`; `/api/commentary/render` and
`/internal/commentary/render-local-probe` remain fail-closed; no public route
200, production API, public/user-facing LLM narration, ExplanationPlan,
Renderer, LLM smoke, or speech key is opened.

### Stage-7 DualDefenderDuty Readiness Closeout

Stage-7 closes only internal proof-readiness for one defender guarding two
distinct non-king material targets.

Stage-7 authority audit:

- DualDefenderDuty proof shape is internal only.
- It may depend on two DefenderDuty-style relations, but it does not replace
  DefenderDuty readiness.
- BoardFacts observes only.
- Story writers do not consume it yet.
- StoryTable does not order it.
- ExplanationPlan does not lower it.
- Renderer does not phrase it.
- LLM does not see it.

Stage-7 still closed:

- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public DualDefenderDuty label
- overloaded defender
- cannot satisfy both
- only defender
- defender cannot move
- removes defender
- wins material
- pressure / initiative
- best / only / forced
- mate threat
- public route 200
- production API
- public/user-facing LLM narration

Stage-7 required verification:

- targeted DualDefenderDuty readiness tests
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Stage-7 closeout notes:

- The only closed positive readiness is one exact same-board
  defender-two-target guard relation.
- Existing proof homes remain authoritative for their own public Stories.
- DualDefenderDuty readiness does not create a public Story label, speech key,
  StoryTable row, ExplanationPlan claim key, renderer template, LLM prompt
  input, public route response, or production API output.
- Detailed DualDefenderDuty readiness authority remains in this section and the
  preceding DualDefenderDuty stages of `StoryInteractionLaw.md` only.

Completion standard: DualDefenderDuty readiness closes when one exact
same-board defender-two-target guard relation can be proven internally, every
public Story path remains closed, existing proof homes are not replaced,
public surfaces remain fail-closed, docs authority is not duplicated, and all
verification passes.

## OverloadTest Proof Readiness

### Stage-0 OverloadTest Proof Readiness Charter

Stage-0 opens internal proof-readiness only.

Stage-0 opens only one legal test move against one target in an existing
DualDefenderDuty relation.

Stage-0 meaning:

- the same defender guards two target pieces
- one legal move directly tests one of those duty targets

Stage-0 proof-readiness shape:

- existing complete DualDefenderDuty relation
- same defender identity as the DualDefenderDuty relation
- tested target is one of the two DualDefenderDuty duty targets
- one legal test move
- legal test move directly tests the selected duty target
- untested duty target remains relation evidence only
- same-board legal replay
- diagnostic missing-evidence shape
- public claim allowed is false

Stage-0 rules:

- OverloadTest readiness is internal proof-readiness only.
- OverloadTest readiness is not public Story.
- OverloadTest readiness is not `Tactic.Overload`, `Tactic.Deflect`,
  `Tactic.Decoy`, or `Plan.Overload`.
- OverloadTest readiness does not prove the defender is overloaded.
- OverloadTest readiness does not prove the defender cannot satisfy both
  duties.
- OverloadTest readiness does not prove the defender must choose.
- OverloadTest readiness does not prove the defender is the only defender.
- OverloadTest readiness does not prove the defender cannot move.
- OverloadTest readiness does not prove material is won.
- OverloadTest readiness does not prove the tested target is won.
- BoardFacts, DualDefenderDuty readiness, legal move replay, source rows,
  proof failures, EngineCheck diagnostics, renderer text, and LLM text cannot
  create public OverloadTest meaning.
- Missing evidence and proof failure text stay internal diagnostics only.

Stage-0 must not open:

- public Story
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public OverloadTest label
- speech key
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- overloaded defender
- defender cannot satisfy both
- defender must choose
- only defender
- defender cannot move
- wins material
- wins target
- pressure / initiative
- best / only / forced
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Stage-0 documentation rule:

- detailed OverloadTest readiness authority lives only in
  `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Stage-0 OverloadTest Proof Readiness closes when the
charter admits only internal proof-readiness for one legal test move against
one selected duty target in an existing complete DualDefenderDuty relation; the
same defender guards two targets and the legal move directly tests one of those
duty targets; no public Story, `Tactic.Overload`, `Tactic.Deflect`,
`Tactic.Decoy`, `Plan.Overload`, public OverloadTest label, speech key,
StoryTable Lead, ExplanationPlan, renderer, LLM smoke, overloaded-defender,
defender-cannot-satisfy-both, defender-must-choose, only-defender,
defender-cannot-move, wins-material, wins-target, pressure, initiative,
best/only/forced, mate-threat, public route `200`, production API, or
public/user-facing LLM narration surface is opened; detailed authority stays
only in `StoryInteractionLaw.md`; AGENTS.md remains unchanged; docs authority
tests pass when touched; and `git diff --check` passes.

### Stage-1 OverloadTest Proof Shape

Stage-1 opens only the internal overload-test proof shape.

Stage-1 may use `OverloadTestProof` as an internal sidecar holder if
implementation needs a named holder.

If docs-only readiness is enough, Stage-1 documents the same exact shape
without adding runtime public authority.

Stage-1 OverloadTest required proof fields:

- attacking side
- defending side
- defender piece identity
- defender square
- first duty target identity
- first duty target square
- second duty target identity
- second duty target square
- targets are distinct
- complete DualDefenderDuty relation exists
- legal test move identity
- test move origin square
- test move destination square
- tested target identity
- tested target square
- tested target is one of the two duty targets
- test move attacks, captures, or directly threatens the tested target on
  the exact after-board
- exact after-board replay
- same-board proof
- route binds to the test move
- missing evidence
- complete flag
- `publicClaimAllowed = false`

Stage-1 OverloadTest rules:

- Proof shape is not public Story.
- Proof shape cannot write Story.
- Proof shape cannot create Verdict, ExplanationPlan, renderer text,
  LLM input, public route, or API output.
- BoardFacts may observe attacks, captures, guards, and legal moves, but
  cannot create overload-test public meaning.
- `OverloadTestProof`, if implemented, remains an internal sidecar only.
- Missing evidence remains diagnostic only and cannot become renderer text,
  LLM input, public route, or API output.

Stage-1 must not open:

- public Story
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public OverloadTest label
- public `OverloadTestProof` claim
- speech key
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- overloaded defender
- defender cannot satisfy both
- defender must choose
- only defender
- defender cannot move
- wins material
- wins target
- pressure / initiative
- best / only / forced
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-1 OverloadTest Proof Shape closes when the internal
shape, or internal `OverloadTestProof` sidecar if implementation needs a named
holder, contains attacking side, defending side, defender identity and square,
first and second duty target identities and squares, distinct target proof,
complete DualDefenderDuty relation proof, legal test move identity, test move
origin and destination, tested target identity and square, proof that the tested
target is one of the two duty targets, proof that the test move attacks,
captures, or directly threatens the tested target on the exact after-board,
exact after-board replay, same-board proof, route binding to the test move,
missing evidence, complete flag, and `publicClaimAllowed = false`; the proof
shape remains unable to write Story, Verdict, ExplanationPlan, renderer text,
LLM input, public route, or API output; BoardFacts remains observation only;
and public Story, Overload, Deflect, Decoy, Plan.Overload, public OverloadTest
labels, speech keys, StoryTable Lead, downstream text, public route `200`,
production API, and public/user-facing LLM narration remain closed.

### Stage-2 OverloadTest Positive Readiness Fixture

Stage-2 opens one narrow positive readiness fixture only.

Stage-2 fixture meaning:

- one defender has two proven guard duties
- legal test move attacks or captures one of the two defended targets
- tested target is identified
- untested target remains identified as the second duty target
- exact after-board replay exists
- same-board proof is present

Stage-2 allowed test effects:

- attack tested target
- capture tested target
- reveal an attack on tested target

Stage-2 forbidden test effects:

- checkmate
- mate threat
- engine-only threat
- vague pressure
- strategy/plan label
- target not among the two duty targets

Stage-2 expected result:

- internal proof complete
- no public Story
- no StoryTable Lead
- no ExplanationPlan
- no renderer
- no LLM smoke
- no `Tactic.Overload`

Stage-2 fixture rules:

- The positive fixture may satisfy only the Stage-1 internal proof shape.
- The positive fixture must keep `publicClaimAllowed = false`.
- Attack, capture, or revealed attack on the tested target is a direct
  test-effect fact only; it does not prove overloaded defender, cannot satisfy
  both, defender must choose, wins material, wins target, pressure, initiative,
  best, only, forced, checkmate, or mate threat.
- BoardFacts may supply attack, capture, guard, and legal move observations,
  but BoardFacts still cannot create overload-test public meaning.
- A target outside the existing two duty targets is incomplete for OverloadTest
  readiness and must remain diagnostic only.

Stage-2 must not open:

- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public OverloadTest label
- speech key
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-2 OverloadTest Positive Readiness Fixture closes
when exactly one positive internal fixture proves one defender has two proven
guard duties, one legal test move attacks, captures, or reveals an attack on
one identified tested target among those two duty targets, the untested target
remains identified as the second duty target, exact after-board replay exists,
same-board proof is present, the internal proof is complete with
`publicClaimAllowed = false`, and forbidden checkmate, mate-threat,
engine-only-threat, vague-pressure, strategy/plan-label, and target-outside-duty
effects stay incomplete or diagnostic; the fixture creates no public Story, no
StoryTable Lead, no ExplanationPlan, no renderer, no LLM smoke, no
`Tactic.Overload`, no tactic or plan family, no speech key, no public route
`200`, no production API, and no public/user-facing LLM narration.

### Stage-3 OverloadTest Negative Corpus

Stage-3 opens only the OverloadTest negative corpus.

Stage-3 incomplete or silent cases:

- missing same-board proof
- missing DualDefenderDuty relation
- incomplete DualDefenderDuty relation
- missing defender
- missing first duty target
- missing second duty target
- duplicated duty targets
- illegal test move
- missing exact after-board replay
- test move does not attack/capture/reveal attack on a duty target
- tested target is not one of the two duty targets
- target is king
- defender and targets are not same-side
- tested target is already gone before the move
- untested target is missing
- source row says overload without proof
- EngineCheck says overload without proof
- BoardFacts-only attack treated as public claim
- ambiguous multiple defenders without one selected defender
- ambiguous multiple target pairs without one selected pair
- material result is claimed
- check/mate/mate threat context is claimed
- one duty relation is stale before-board-only

Stage-3 forbidden wording:

- overloaded
- cannot satisfy both
- must choose
- overworked
- only defender
- deflects
- decoys
- removes guard
- wins material
- wins target
- pressure
- initiative
- best / only / forced

Stage-3 expected result:

- incomplete internal proof or silence
- no public Story
- no StoryTable Lead
- no ExplanationPlan
- no renderer
- no LLM smoke
- no `Tactic.Overload`

Stage-3 rules:

- Negative fixtures may fail readiness, but they do not create public claims.
- Missing same-board proof, stale before-board duty facts, source-row text,
  and EngineCheck text remain diagnostics only.
- BoardFacts-only attack, capture, guard, or legal-move observations cannot
  become public overload-test meaning.
- Multiple defenders require one selected defender; otherwise proof remains
  incomplete.
- Multiple target pairs require one selected target pair; otherwise proof
  remains incomplete.
- Material, check, mate, mate-threat, pressure, initiative, plan, strategy,
  deflection, decoy, and remove-guard contexts remain sibling or closed
  meanings, not OverloadTest proof effects.
- Stage-3 does not open public Story, StoryTable Lead, ExplanationPlan,
  renderer text, LLM smoke, public route `200`, production API, or
  public/user-facing LLM narration.

Completion standard: Stage-3 OverloadTest Negative Corpus closes when every
listed missing, incomplete, duplicated, illegal, no-after-board,
no-duty-target-effect, non-duty-target, king-target, side-mismatch,
already-gone-target, missing-untested-target, source-only, EngineCheck-only,
BoardFacts-only, ambiguous-defender, ambiguous-target-pair, material-result,
check, mate, mate-threat, and stale-before-board case remains incomplete or
silent; forbidden overloaded, cannot-satisfy-both, must-choose, overworked,
only-defender, deflects, decoys, removes-guard, wins-material, wins-target,
pressure, initiative, and best/only/forced wording remains closed; and no
public Story, StoryTable Lead, ExplanationPlan, renderer text, LLM smoke,
`Tactic.Overload`, public route `200`, production API, or public/user-facing
LLM narration is opened.

### Stage-4 OverloadTest Existing Owner Collision Audit

Stage-4 opens only the existing owner collision audit for OverloadTest
readiness.

Stage-4 collision targets:

- `DefenderDuty` readiness
- `DualDefenderDuty` readiness
- `Tactic.RemoveGuard`
- `Scene.Defense`
- `Tactic.Loose`
- `Tactic.Hanging`
- `Scene.Material`
- `Tactic.QueenHit`
- `Tactic.Fork`
- `Tactic.Skewer`
- `Tactic.Pin`
- `Tactic.DiscoveredAttack`

Stage-4 rules:

- OverloadTest proof does not replace DefenderDuty readiness.
- OverloadTest proof does not replace DualDefenderDuty readiness.
- OverloadTest proof may depend on DualDefenderDuty internally, but it
  does not create public meaning.
- OverloadTest proof does not create RemoveGuard.
- OverloadTest proof does not create Defense.
- OverloadTest proof does not prove a threat is stopped.
- OverloadTest proof does not prove a target is loose or hanging.
- OverloadTest proof does not prove material gain.
- OverloadTest proof does not prove pin, skewer, fork, discovered attack,
  or queen hit.
- Existing opened Stories keep their own proof homes and speech keys.

Stage-4 expected result:

- internal overload-test relation may be present as diagnostic/proof-readiness only.
- all public Story labels remain unchanged.

Stage-4 owner boundaries:

- DefenderDuty readiness stays its own internal readiness shape.
- DualDefenderDuty readiness stays its own internal dual-duty readiness shape.
- RemoveGuard meaning stays in `RemoveGuardProof`, `Tactic.RemoveGuard`, and
  `removes_defender`.
- Defense meaning stays in ThreatProof/DefenseProof, `Scene.Defense`, and its
  defense speech key.
- Loose meaning stays in `LoosePieceProof`, `Tactic.Loose`, and
  `attacks_loose_piece`.
- Hanging meaning stays in the existing Hanging proof path, `Tactic.Hanging`,
  and `can_win_piece`.
- Material meaning stays in the existing material proof path,
  `Scene.Material`, and `material_balance_changes`.
- QueenHit, Fork, Skewer, Pin, and DiscoveredAttack keep their own proof homes,
  Story labels, and speech keys.

Stage-4 must not open:

- new public Story
- new proof home
- new StoryTable Lead path
- new ExplanationPlan mapping
- new renderer text
- new LLM smoke
- overloaded defender
- cannot satisfy both
- must choose
- overworked defender
- only defender
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-4 OverloadTest Existing Owner Collision Audit
closes when OverloadTest proof remains diagnostic/proof-readiness only; it
does not replace DefenderDuty readiness or DualDefenderDuty readiness, create
RemoveGuard or Defense, prove a stopped threat, prove loose or hanging status,
prove material gain, or prove pin, skewer, fork, discovered attack, or queen
hit; `DefenderDuty` readiness, `DualDefenderDuty` readiness,
`Tactic.RemoveGuard`, `Scene.Defense`, `Tactic.Loose`, `Tactic.Hanging`,
`Scene.Material`, `Tactic.QueenHit`, `Tactic.Fork`, `Tactic.Skewer`,
`Tactic.Pin`, and `Tactic.DiscoveredAttack` keep their existing proof homes,
Story labels, and speech keys; all public Story labels remain unchanged; and
no StoryTable Lead, ExplanationPlan, renderer, LLM smoke, public route `200`,
production API, or public/user-facing LLM narration is opened.

### Stage-5 OverloadTest Reply / Cannot-Satisfy Boundary

Stage-5 opens only boundary documentation/test for reply and
cannot-satisfy claims.

Stage-5 opens no public overload conclusion.

Stage-5 rules:

- OverloadTest may identify a legal test against one duty target.
- OverloadTest may identify the second duty target as still relevant.
- OverloadTest must not conclude the defender cannot satisfy both.
- OverloadTest must not conclude the defender must move.
- OverloadTest must not conclude the defender is overloaded.
- OverloadTest must not enumerate a winning line.
- Any cannot-satisfy proof requires a separate slice.

Stage-5 forbidden names:

- `OverloadResolutionProof`
- `CannotSatisfyBothProof`
- `TacticOverload`
- `overloads_defender`
- `wins_material_by_overload`

Stage-5 must not open:

- public Overload Story
- `Tactic.Overload`
- public OverloadTest label
- StoryTable Lead
- Verdict
- ExplanationPlan
- renderer text
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-5 OverloadTest Reply / Cannot-Satisfy Boundary
closes when OverloadTest readiness may identify one legal test against one duty
target and keep the second duty target relevant, but cannot conclude the
defender cannot satisfy both, must move, or is overloaded; cannot enumerate a
winning line; does not introduce `OverloadResolutionProof`,
`CannotSatisfyBothProof`, `TacticOverload`, `overloads_defender`, or
`wins_material_by_overload`; opens no public Overload Story,
`Tactic.Overload`, public OverloadTest label, StoryTable Lead, Verdict,
ExplanationPlan, renderer text, LLM smoke, public route `200`, production API,
or public/user-facing LLM narration; and current readiness stops exactly before
the overload conclusion.

Completion note: current readiness stops exactly before the overload conclusion.

### Stage-6 OverloadTest EngineCheck / Diagnostics Boundary

Stage-6 opens only diagnostic boundary tests for OverloadTest.

Stage-6 opens no public overload conclusion.

Stage-6 rules:

- EngineCheck cannot create OverloadTest proof.
- EngineCheck cannot create Overload, Deflect, Decoy, RemoveGuard,
  Defense, Material, Hanging, Loose, or QueenHit from OverloadTest.
- Supports creates no claim.
- Caps creates no claim.
- Refutes creates no public overload-test text.
- Unknown creates no expression.
- proofFailures remain internal diagnostics only.
- raw PV and eval remain non-public.

Stage-6 forbidden wording:

- engine says overloaded
- engine says cannot satisfy both
- eval proves overload
- PV proves overload
- best move
- only move
- forced

Stage-6 must not open:

- EngineCheck-owned OverloadTest proof
- public Overload Story
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Tactic.RemoveGuard` from OverloadTest
- `Scene.Defense` from OverloadTest
- `Scene.Material` from OverloadTest
- `Tactic.Hanging` from OverloadTest
- `Tactic.Loose` from OverloadTest
- `Tactic.QueenHit` from OverloadTest
- StoryTable Lead
- Verdict
- ExplanationPlan
- renderer text
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-6 OverloadTest EngineCheck / Diagnostics Boundary
closes when EngineCheck cannot create OverloadTest proof and cannot create
Overload, Deflect, Decoy, RemoveGuard, Defense, Material, Hanging, Loose, or
QueenHit from OverloadTest; Supports and Caps create no claim; Refutes creates
no public overload-test text; Unknown creates no expression; proofFailures,
raw PV, and eval remain internal and non-public; engine-says-overloaded,
engine-says-cannot-satisfy-both, eval-proves-overload, PV-proves-overload,
best-move, only-move, and forced wording remains closed; and no public
Overload Story, `Tactic.Overload`, `Tactic.Deflect`, `Tactic.Decoy`,
OverloadTest-owned `Tactic.RemoveGuard`, `Scene.Defense`, `Scene.Material`,
`Tactic.Hanging`, `Tactic.Loose`, or `Tactic.QueenHit`, StoryTable Lead,
Verdict, ExplanationPlan, renderer text, LLM smoke, public route `200`,
production API, or public/user-facing LLM narration is opened.

### Stage-7 OverloadTest Docs / Public Surface

Stage-7 opens only docs and public-surface boundary tests for
OverloadTest.

Stage-7 docs rules:

- detailed OverloadTest readiness authority lives only in
  `StoryInteractionLaw.md`.
- README/SSOT/Architecture/Contract/Manifest stay summary-only if touched.
- AGENTS.md stays unchanged unless durable operator rules change.
- docs tests must prevent detailed readiness duplication outside
  `StoryInteractionLaw.md`.

Stage-7 public surface:

- `/api/commentary/render` remains fail-closed.
- `/internal/commentary/render-local-probe` remains fail-closed.
- no public route `200`
- no production API
- no public/user-facing LLM narration

Stage-7 downstream boundary:

- no ExplanationPlan
- no Renderer
- no LLM smoke
- no speech key

Completion standard: Stage-7 OverloadTest Docs / Public Surface closes when
detailed OverloadTest readiness authority lives only in `StoryInteractionLaw.md`;
README, SSOT, Architecture, Contract, and Manifest remain summary-only if
touched; AGENTS.md stays unchanged unless durable operator rules change; docs
tests prevent detailed readiness duplication outside `StoryInteractionLaw.md`;
`/api/commentary/render` and `/internal/commentary/render-local-probe` remain
fail-closed; no public route `200`, production API, public/user-facing LLM
narration, ExplanationPlan, Renderer, LLM smoke, or speech key is opened.

### Stage-8 OverloadTest Readiness Closeout

Stage-8 closes only internal proof-readiness for one legal test move
against one target in a complete DualDefenderDuty relation.

Stage-8 authority audit:

- OverloadTest proof shape is internal only.
- It may depend on DualDefenderDuty readiness, but it does not replace it.
- It may depend on DefenderDuty-style guard relations, but it does not
  replace them.
- BoardFacts observes only.
- Story writers do not consume it yet.
- StoryTable does not order it.
- ExplanationPlan does not lower it.
- Renderer does not phrase it.
- LLM does not see it.

Stage-8 still closed:

- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public OverloadTest label
- overloaded defender
- cannot satisfy both
- defender must choose
- only defender
- defender cannot move
- removes defender
- wins material
- wins target
- pressure / initiative
- best / only / forced
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Stage-8 required verification:

- targeted OverloadTest readiness tests
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Completion standard: Stage-8 OverloadTest Readiness Closeout
OverloadTest readiness closes when one exact same-board legal test move
against one duty target can be proven internally from a complete
DualDefenderDuty relation, every public Story path remains closed,
existing proof homes are not replaced, public surfaces remain fail-closed,
docs authority is not duplicated, and all verification passes.

## CannotSatisfyBoth Proof Readiness

### Stage-0 CannotSatisfyBoth Proof Readiness Charter

Stage-0 opens internal proof-readiness only.

Stage-0 opens only the proof-readiness shape that checks defender-side
legal replies after a complete OverloadTest relation.

Stage-0 meaning:

- in one complete OverloadTest relation, the defender side has no single
  legal reply that preserves both duty targets.

Stage-0 proof-readiness shape:

- complete OverloadTest relation
- defending side
- attacking side
- defender piece identity
- defender square before the test
- first duty target identity
- first duty target square
- second duty target identity
- second duty target square
- legal test move identity
- exact after-board from the test move
- defender-side legal reply set from the exact after-board
- for each defender-side legal reply, target-one preservation result
- for each defender-side legal reply, target-two preservation result
- no defender-side legal reply preserves both duty targets
- same-board proof
- diagnostic missing-evidence shape
- public claim allowed is false

Stage-0 rules:

- CannotSatisfyBoth readiness is internal proof-readiness only.
- CannotSatisfyBoth readiness is not public Story.
- CannotSatisfyBoth readiness depends on a complete OverloadTest relation
  and must not repair incomplete OverloadTest proof.
- CannotSatisfyBoth readiness checks defender-side legal replies only.
- CannotSatisfyBoth readiness does not prove an overloaded defender.
- CannotSatisfyBoth readiness does not prove the defender must choose.
- CannotSatisfyBoth readiness does not prove the defender is the only defender.
- CannotSatisfyBoth readiness does not prove a forced move.
- CannotSatisfyBoth readiness does not prove material is won.
- CannotSatisfyBoth readiness does not prove either target is won.
- BoardFacts, OverloadTest readiness, legal reply enumeration, source rows,
  proof failures, EngineCheck diagnostics, renderer text, and LLM text cannot
  create public CannotSatisfyBoth meaning.
- Missing evidence and proof failure text stay internal diagnostics only.

Stage-0 must not open:

- public Story
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public CannotSatisfyBoth label
- speech key
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- overloaded defender
- defender must choose
- only defender
- only move
- forced move
- wins material
- wins target
- decisive / winning
- pressure / initiative
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Stage-0 documentation rule:

- detailed CannotSatisfyBoth readiness authority lives only in
  `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Stage-0 CannotSatisfyBoth Proof Readiness closes when
the charter admits only an internal proof-readiness shape over one complete
OverloadTest relation, where defender-side legal replies from the exact
after-board are checked and none preserves both duty targets; incomplete
OverloadTest proof cannot be repaired; no public Story, `Tactic.Overload`,
`Tactic.Deflect`, `Tactic.Decoy`, `Plan.Overload`, public CannotSatisfyBoth
label, speech key, StoryTable Lead, ExplanationPlan, renderer, LLM smoke,
overloaded-defender, defender-must-choose, only-defender, only-move,
forced-move, wins-material, wins-target, decisive/winning, pressure,
initiative, mate-threat, public route `200`, production API, or
public/user-facing LLM narration surface is opened; detailed authority stays
only in `StoryInteractionLaw.md`; AGENTS.md remains unchanged; docs authority
tests pass when touched; and `git diff --check` passes.

### Stage-1 CannotSatisfyBoth Proof Shape

Stage-1 opens only the internal CannotSatisfyBoth proof shape.

Stage-1 may use `CannotSatisfyBothProof` as an internal sidecar holder
if implementation needs a named holder.

If docs-only readiness is enough, Stage-1 documents the same exact shape
without adding runtime public authority.

Stage-1 CannotSatisfyBoth required proof fields:

- attacking side
- defending side
- defender piece identity
- defender square before replies
- first duty target identity
- first duty target square
- second duty target identity
- second duty target square
- complete DualDefenderDuty relation
- complete OverloadTest relation
- legal test move identity
- exact after-board replay after test move
- defending side legal replies after test move
- reply preservation map
- for each legal reply: preserves first target yes/no
- for each legal reply: preserves second target yes/no
- no reply preserves both targets
- same-board proof
- missing evidence
- complete flag
- `publicClaimAllowed = false`

Stage-1 CannotSatisfyBoth rules:

- Proof shape is not public Story.
- Proof shape cannot write Story.
- Proof shape cannot create Verdict, ExplanationPlan, renderer text,
  LLM input, public route, or API output.
- BoardFacts may observe legal replies and attack/guard relations, but
  cannot create cannot-satisfy public meaning.
- `CannotSatisfyBothProof`, if implemented, remains an internal sidecar only.
- Missing evidence remains diagnostic only and cannot become renderer text,
  LLM input, public route, or API output.

Stage-1 must not open:

- public Story
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public CannotSatisfyBoth label
- public `CannotSatisfyBothProof` claim
- speech key
- StoryTable Lead
- Verdict
- ExplanationPlan
- Renderer
- LLM smoke
- overloaded defender
- defender must choose
- only defender
- only move
- forced move
- wins material
- wins target
- decisive / winning
- pressure / initiative
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-1 CannotSatisfyBoth Proof Shape closes when the
internal shape, or internal `CannotSatisfyBothProof` sidecar if implementation
needs a named holder, contains attacking side, defending side, defender
identity and square before replies, first and second duty target identities and
squares, complete DualDefenderDuty relation proof, complete OverloadTest
relation proof, legal test move identity, exact after-board replay after test
move, defending-side legal replies after the test move, reply preservation
map, per-reply first-target preservation result, per-reply second-target
preservation result, proof that no reply preserves both targets, same-board
proof, missing evidence, complete flag, and `publicClaimAllowed = false`; the
proof shape remains unable to write Story, Verdict, ExplanationPlan, renderer
text, LLM input, public route, or API output; BoardFacts remains observation
only; and public Story, Overload, Deflect, Decoy, Plan.Overload, public
CannotSatisfyBoth labels, public `CannotSatisfyBothProof` claims, speech keys,
StoryTable Lead, downstream text, public route `200`, production API, and
public/user-facing LLM narration remain closed.

### Stage-2 CannotSatisfyBoth Reply Preservation Map

Stage-2 opens only the internal reply preservation map.

Stage-2 map requirements:

- enumerate legal replies for defending side after the test move
- each reply is same-board legal
- each reply has exact replay
- each reply is checked for target A preservation
- each reply is checked for target B preservation
- preservation means target remains present and guarded or otherwise not lost
  according to the narrow readiness rule
- no legal reply has both `preservesTargetA = true` and
  `preservesTargetB = true`

Stage-2 rules:

- Reply preservation map is internal proof only.
- Reply preservation map is not public pedagogy.
- Reply preservation map cannot explain publicly why each reply fails.
- Reply preservation map cannot select a best reply.
- Reply preservation map cannot create an only-reply, forced-line,
  winning-line, material-conversion, engine-PV, or eval claim.
- Missing reply preservation evidence remains internal diagnostics only.

Stage-2 must not open:

- public Story
- public CannotSatisfyBoth label
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- best reply
- only reply
- forced line
- engine PV
- eval
- winning line
- material conversion
- public explanation of why each reply fails
- speech key
- StoryTable Lead
- Verdict
- ExplanationPlan
- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-2 CannotSatisfyBoth Reply Preservation Map closes when
the internal map enumerates defending-side legal replies after the test move,
proves each reply same-board legal, carries exact replay for each reply,
checks each reply for target A and target B preservation, defines preservation
only as the target remaining present and guarded or otherwise not lost under
the narrow readiness rule, and proves no legal reply has both
`preservesTargetA = true` and `preservesTargetB = true`; the map remains
internal proof, not public pedagogy; it does not select a best reply, only
reply, forced line, engine PV, eval, winning line, material conversion, or
public explanation of why each reply fails; and no public Story, public
CannotSatisfyBoth label, Overload, Deflect, Decoy, Plan.Overload, speech key,
StoryTable Lead, Verdict, ExplanationPlan, Renderer, LLM smoke, public route
`200`, production API, or public/user-facing LLM narration is opened.

### Stage-3 CannotSatisfyBoth Positive Readiness Fixture

Stage-3 opens one narrow positive readiness fixture only.

Stage-3 fixture meaning:

- complete DualDefenderDuty relation exists
- complete OverloadTest relation exists
- legal replies after the test move are enumerated
- every legal reply fails to preserve at least one of the two duty targets
- no public claim is produced

Stage-3 expected result:

- internal proof complete
- no public Story
- no StoryTable Lead
- no ExplanationPlan
- no renderer
- no LLM smoke
- no `Tactic.Overload`
- no overloaded wording

Stage-3 fixture rules:

- The positive fixture may satisfy only the Stage-1 proof shape and Stage-2 reply map.
- The positive fixture must keep `publicClaimAllowed = false`.
- The positive fixture must not produce public CannotSatisfyBoth text.
- Complete readiness does not open overloaded-defender wording.
- Complete readiness does not create public explanation of reply failures.

Stage-3 must not open:

- public Story
- public CannotSatisfyBoth label
- `Tactic.Overload`
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- overloaded wording
- speech key
- StoryTable Lead
- Verdict
- ExplanationPlan
- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-3 CannotSatisfyBoth Positive Readiness Fixture closes when
one narrow positive readiness fixture proves an existing complete
DualDefenderDuty relation, an existing complete OverloadTest relation, legal
reply enumeration after the test move, exact internal proof that every legal
reply fails to preserve at least one of the two duty targets, and no public
claim output; the internal proof may be complete with `publicClaimAllowed =
false`; no public Story, public CannotSatisfyBoth label, StoryTable Lead,
Verdict, ExplanationPlan, renderer, LLM smoke, `Tactic.Overload`, overloaded
wording, speech key, public route `200`, production API, or
public/user-facing LLM narration is opened.

### Stage-4 CannotSatisfyBoth Negative Corpus

Stage-4 opens the internal negative corpus only.

Stage-4 cases must stay incomplete or silent for:

- missing same-board proof
- missing DualDefenderDuty relation
- incomplete DualDefenderDuty relation
- missing OverloadTest relation
- incomplete OverloadTest relation
- legal replies after test move are not enumerated
- one legal reply preserves both duty targets
- reply preservation map incomplete
- target A missing before test
- target B missing before test
- defender missing before replies
- targets are duplicated
- one target is king
- legal reply replay missing
- stale before-board reply map
- source row says cannot satisfy both without proof
- EngineCheck says cannot satisfy both without proof
- BoardFacts-only observation treated as public claim
- ambiguous multiple defenders without one selected defender
- ambiguous multiple target pairs without one selected pair
- checkmate/mate threat context contaminates proof
- material result is claimed as public meaning

Stage-4 forbidden public wording:

- overloaded
- cannot satisfy both
- must choose
- only defender
- only move
- forced
- wins material
- wins target
- decisive
- winning
- pressure
- initiative

Stage-4 negative corpus rules:

- Negative cases may fail proof completeness or remain silent internally.
- Negative cases must not write public Story, StoryTable Lead, Verdict,
  ExplanationPlan, renderer text, LLM input, public route output, or production
  API output.
- Source rows, EngineCheck, and BoardFacts observations do not repair missing proof.
- Mate, threat, material, and strategy contexts must not contaminate this proof.
- Negative cases must not be explained publicly as reply-failure pedagogy.

Completion standard: Stage-4 CannotSatisfyBoth Negative Corpus closes when all
listed negative cases are pinned to incomplete internal proof or silence; all
listed forbidden public wording remains unavailable for Story, StoryTable,
Verdict, ExplanationPlan, renderer, LLM, public route, and production API
surfaces; source-row text, EngineCheck diagnostics, BoardFacts observations,
mate or threat context, and material-result context cannot repair missing
same-board proof, missing or incomplete DualDefenderDuty, missing or incomplete
OverloadTest, missing reply enumeration, missing reply replay, incomplete reply
preservation map, duplicated targets, king targets, stale before-board reply
maps, ambiguous defender or target-pair selection, or any reply that preserves
both duty targets.

### Stage-5 CannotSatisfyBoth Existing Owner Collision Audit

Stage-5 opens the internal existing-owner collision audit only.

Stage-5 collision targets:

- DefenderDuty readiness
- DualDefenderDuty readiness
- OverloadTest readiness
- `Tactic.RemoveGuard`
- `Scene.Defense`
- `Tactic.Loose`
- `Tactic.Hanging`
- `Scene.Material`
- `Tactic.QueenHit`
- `Tactic.Fork`
- `Tactic.Skewer`
- `Tactic.Pin`
- `Tactic.DiscoveredAttack`

Stage-5 collision rules:

- CannotSatisfyBoth proof does not replace DefenderDuty readiness.
- CannotSatisfyBoth proof does not replace DualDefenderDuty readiness.
- CannotSatisfyBoth proof does not replace OverloadTest readiness.
- CannotSatisfyBoth proof may depend on OverloadTest internally, but it does not create public meaning.
- CannotSatisfyBoth proof does not create RemoveGuard.
- CannotSatisfyBoth proof does not create Defense.
- CannotSatisfyBoth proof does not prove material gain.
- CannotSatisfyBoth proof does not prove pin, skewer, fork, discovered attack, loose, hanging, or queen hit.
- Existing opened Stories keep their own proof homes and speech keys.

Stage-5 expected result:

- internal cannot-satisfy relation may be present as diagnostic/proof-readiness only
- all public Story labels remain unchanged

Stage-5 collision audit rules:

- CannotSatisfyBoth proof-readiness must not become a replacement owner for
  readiness relations that already have their own Stage sections.
- CannotSatisfyBoth proof-readiness must not become a tactical or material
  Story writer.
- A complete internal cannot-satisfy relation may be observed only as
  diagnostic/proof-readiness evidence for internal proof review.
- Any existing opened Story keeps its own proof home, Story label,
  StoryTable role, speech key, and downstream boundary.

Completion standard: Stage-5 CannotSatisfyBoth Existing Owner Collision Audit closes when
DefenderDuty readiness, DualDefenderDuty readiness, OverloadTest readiness,
`Tactic.RemoveGuard`, `Scene.Defense`, `Tactic.Loose`, `Tactic.Hanging`,
`Scene.Material`, `Tactic.QueenHit`, `Tactic.Fork`, `Tactic.Skewer`,
`Tactic.Pin`, and `Tactic.DiscoveredAttack` remain owned by their existing
proof homes and speech keys; CannotSatisfyBoth proof-readiness does not replace
those readiness relations, does not create RemoveGuard or Defense, does not
prove material gain, does not prove pin, skewer, fork, discovered attack,
loose, hanging, or queen hit, and does not create public meaning; all public
Story labels remain unchanged.

### Stage-6 CannotSatisfyBoth Public-Wording Boundary

Stage-6 opens boundary documentation and test coverage only.

Stage-6 opens no public overload wording.

Stage-6 public-wording rules:

- CannotSatisfyBoth may internally prove no reply preserves both duty targets.
- CannotSatisfyBoth must not publicly say overloaded.
- CannotSatisfyBoth must not publicly say cannot satisfy both.
- CannotSatisfyBoth must not publicly say must choose.
- CannotSatisfyBoth must not publicly say only move.
- CannotSatisfyBoth must not publicly say forced.
- CannotSatisfyBoth must not publicly say wins material.
- Any public overload wording requires a separate `Tactic.Overload` vertical slice.

Stage-6 forbidden names:

- `TacticOverload`
- `overloads_defender`
- `overloaded_defender`
- `wins_material_by_overload`
- `only_defender`

Stage-6 must not open:

- public Story
- public CannotSatisfyBoth label
- `Tactic.Overload`
- speech key
- StoryTable Lead
- Verdict
- ExplanationPlan
- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Stage-6 boundary rules:

- Internal proof completeness does not grant player-facing wording.
- Forbidden names remain unavailable as model names, claim keys, speech keys,
  renderer templates, LLM prompt keys, public route values, or production API
  fields for this stage.
- Public overload wording requires its own proof-backed vertical slice and
  cannot be borrowed from CannotSatisfyBoth readiness.

Completion standard: Stage-6 CannotSatisfyBoth Public-Wording Boundary closes when
the internal relation may prove no reply preserves both duty targets, while
public wording remains closed for overloaded, cannot satisfy both, must choose,
only move, forced, and wins material; `TacticOverload`, `overloads_defender`,
`overloaded_defender`, `wins_material_by_overload`, and `only_defender` remain
forbidden names for this stage; any public overload wording remains gated by a
separate `Tactic.Overload` vertical slice; no public Story, public
CannotSatisfyBoth label, `Tactic.Overload`, speech key, StoryTable Lead,
Verdict, ExplanationPlan, Renderer, LLM smoke, public route `200`, production
API, or public/user-facing LLM narration is opened.

### Stage-7 CannotSatisfyBoth EngineCheck Diagnostics Boundary

Stage-7 opens diagnostic boundary tests only.

Stage-7 EngineCheck rules:

- EngineCheck cannot create CannotSatisfyBoth proof.
- EngineCheck cannot create Overload, Deflect, Decoy, RemoveGuard, Defense, Material, Hanging, Loose, or QueenHit from CannotSatisfyBoth.
- Supports creates no claim.
- Caps creates no claim.
- Refutes creates no public cannot-satisfy text.
- Unknown creates no expression.
- proofFailures remain internal diagnostics only.
- raw PV and eval remain non-public.

Stage-7 forbidden diagnostic wording:

- engine says overloaded
- engine says cannot satisfy both
- eval proves overload
- PV proves overload
- best move
- only move
- forced

Stage-7 must not open:

- public Story
- public CannotSatisfyBoth label
- `Tactic.Overload`
- EngineCheck-owned claim
- proofFailure public output
- raw PV public output
- eval public output
- ExplanationPlan
- Renderer
- LLM smoke
- public route `200`
- production API

Stage-7 diagnostics rules:

- EngineCheck may support, cap, refute, or remain unknown only for an already
  existing proof-owned relation.
- EngineCheck status never repairs missing same-board proof, missing relation
  completeness, or an incomplete reply preservation map.
- proofFailures may explain internal failure to tests and debugging tools only.
- Raw PV, eval, and engine text cannot become public values, renderer input,
  LLM input, or source text for CannotSatisfyBoth.

Completion standard: Stage-7 CannotSatisfyBoth EngineCheck Diagnostics Boundary closes when
EngineCheck cannot create CannotSatisfyBoth proof, cannot create Overload,
Deflect, Decoy, RemoveGuard, Defense, Material, Hanging, Loose, or QueenHit
from CannotSatisfyBoth, and cannot create a claim through Supports, Caps,
Refutes, or Unknown; Refutes produces no public cannot-satisfy text; Unknown
produces no expression; proofFailures remain internal diagnostics only; raw PV
and eval remain non-public; engine says overloaded, engine says cannot satisfy
both, eval proves overload, PV proves overload, best move, only move, and forced
remain forbidden diagnostic wording.

### Stage-8 CannotSatisfyBoth Docs And Public Surface

Stage-8 opens docs and public-surface boundary tests only.

Stage-8 docs rules:

- detailed CannotSatisfyBoth readiness authority lives only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, and Manifest remain summary-only if touched.
- AGENTS.md remains unchanged unless durable operator rules change.
- docs tests must prevent detailed readiness duplication outside `StoryInteractionLaw.md`.

Stage-8 public surface rules:

- `/api/commentary/render` remains fail-closed.
- `/internal/commentary/render-local-probe` remains fail-closed.
- no public route `200`
- no production API
- no public/user-facing LLM narration

Stage-8 downstream rules:

- no ExplanationPlan
- no Renderer
- no LLM smoke
- no speech key

Stage-8 must not open:

- public Story
- public CannotSatisfyBoth label
- `Tactic.Overload`
- StoryTable Lead
- Verdict
- public route `200`
- production API

Stage-8 documentation boundary rules:

- Summary documents may mention only that CannotSatisfyBoth remains internal
  proof-readiness, if they are touched for a separate summary need.
- Detailed proof fields, negative corpus, collision audit, public wording,
  EngineCheck, diagnostics, public-surface, and closeout authority remain in
  `StoryInteractionLaw.md` only.
- Public routes must remain fail-closed tombstones and must not return rendered
  CannotSatisfyBoth payloads.
- Downstream expression layers must not be introduced from this readiness slice.

Completion standard: Stage-8 CannotSatisfyBoth Docs And Public Surface closes when
detailed CannotSatisfyBoth readiness authority lives only in
`StoryInteractionLaw.md`; README, SSOT, Architecture, Contract, and Manifest
remain summary-only if touched; AGENTS.md is unchanged unless durable operator
rules change; docs tests prevent detailed readiness duplication outside
`StoryInteractionLaw.md`; `/api/commentary/render` and
`/internal/commentary/render-local-probe` remain fail-closed; no public route
`200`, production API, public/user-facing LLM narration, ExplanationPlan,
Renderer, LLM smoke, speech key, public Story, public CannotSatisfyBoth label,
`Tactic.Overload`, StoryTable Lead, or Verdict is opened.

### Stage-9 CannotSatisfyBoth Readiness Closeout

Stage-9 closes only internal proof-readiness that no legal reply after a complete OverloadTest preserves both duty targets.

Stage-9 authority audit:

- CannotSatisfyBoth proof shape is internal only.
- `publicClaimAllowed=false` on CannotSatisfyBoth readiness or
  `OverloadProof` sidecars means the sidecar/proof home does not own public
  claims; it does not block separate `TacticOverload` Story admission from a
  complete `OverloadProof`.
- CannotSatisfyBoth may depend on OverloadTest readiness, but it does not replace it.
- CannotSatisfyBoth may depend on DualDefenderDuty readiness, but it does not replace it.
- CannotSatisfyBoth may depend on DefenderDuty-style guard relations, but it does not replace them.
- BoardFacts observes only.
- Story writers do not consume CannotSatisfyBoth readiness directly; public
  Overload admission must go through complete `OverloadProof` and
  `TacticOverload`.
- StoryTable does not order it.
- ExplanationPlan does not lower it.
- Renderer does not phrase it.
- LLM does not see it.

Stage-9 still closed:

- `Tactic.Overload` from CannotSatisfyBoth alone
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public CannotSatisfyBoth label
- overloaded defender
- cannot satisfy both public wording
- defender must choose
- only defender
- only move
- forced move
- wins material
- wins target
- pressure / initiative
- decisive / winning
- mate threat
- public route `200`
- production API
- public/user-facing LLM narration

Stage-9 duplication audit:

- one chess meaning, one proof home, one Story label, one speech key principle remains required.
- CannotSatisfyBoth is not DefenderDuty.
- CannotSatisfyBoth is not DualDefenderDuty.
- CannotSatisfyBoth is not OverloadTest.
- CannotSatisfyBoth is not `Tactic.Overload`.
- CannotSatisfyBoth is not `Tactic.RemoveGuard`.
- CannotSatisfyBoth is not `Scene.Defense`.
- CannotSatisfyBoth is not `Scene.Material`.
- CannotSatisfyBoth is not `Tactic.Hanging`.
- CannotSatisfyBoth is not `Tactic.Loose`.
- CannotSatisfyBoth is not `Tactic.QueenHit`.
- CannotSatisfyBoth is not `Tactic.Fork`.
- CannotSatisfyBoth is not `Tactic.Skewer`.
- CannotSatisfyBoth is not `Tactic.Pin`.
- CannotSatisfyBoth is not `Tactic.DiscoveredAttack`.

Stage-9 no duplicate authority:

- no second proof home for DefenderDuty relation
- no second proof home for DualDefenderDuty relation
- no second proof home for OverloadTest relation
- no public CannotSatisfyBoth Story label
- no CannotSatisfyBoth writer
- no CannotSatisfyBoth speech key
- no CannotSatisfyBoth ExplanationPlan claim key
- no CannotSatisfyBoth renderer template
- no CannotSatisfyBoth LLM prompt path

Stage-9 docs duplication:

- detailed CannotSatisfyBoth authority appears only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, and Manifest may contain summary only if touched.
- AGENTS.md remains unchanged.

Stage-9 required verification:

- targeted CannotSatisfyBoth readiness tests
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Completion standard: Stage-9 CannotSatisfyBoth Readiness Closeout closes when,
from a complete OverloadTest relation, every same-board legal defending reply is
internally mapped and none preserves both duty targets; duplication audit proves
it does not replace DefenderDuty, DualDefenderDuty, OverloadTest, RemoveGuard,
Defense, Material, OverloadProof, TacticOverload, or any opened tactic home; no
public CannotSatisfyBoth Story label, writer, speech key, ExplanationPlan claim
key, renderer template, or LLM path is created; public surfaces remain
fail-closed; detailed docs authority is not duplicated; and all verification
passes.

## Tactic.Overload Neighborhood

### Stage-0 Overload Charter

Stage-0 opens the first public `Tactic.Overload` vertical slice.

Stage-0 Overload opens:

- narrow `Tactic.Overload`
- proof home: `OverloadProof`
- Story label: `Tactic.Overload`
- writer: `TacticOverload`
- speech key: `overloads_defender`
- meaning: from complete DefenderDuty, DualDefenderDuty, OverloadTest,
  and CannotSatisfyBoth proof, one legal move overloads a defender
  with two duties.
- player-facing sentence: this move overloads the defender.
- `OverloadProof.publicClaimAllowed=false` means the proof sidecar does not own
  the public claim. It does not block `TacticOverload` from admitting a public
  `Tactic.Overload` Story when the complete proof tuple is present.

Stage-0 first positive scope:

- one defender has two distinct non-king material duty targets
- one legal test move tests one of those targets
- legal reply map after the test move shows no single reply preserves
  both duty targets
- all proof binds to the same board and same legal route

Stage-0 does not open:

- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- mate threat
- only defender unless separately proven
- defender must move
- forced move
- best move
- only move
- wins material
- wins target
- decisive / winning
- pressure / initiative
- public route `200`
- production API
- public/user-facing LLM narration

Stage-0 documentation rule:

- detailed Overload authority lives only in `StoryInteractionLaw.md`.
- summary docs remain summary-only if touched.
- AGENTS.md remains unchanged.

Completion standard: Stage-0 Overload Charter closes when one legal move is
allowed to create a `Tactic.Overload` Story only from complete same-board
DefenderDuty, DualDefenderDuty, OverloadTest, and CannotSatisfyBoth proof; the
proof home is `OverloadProof`; the only writer is `TacticOverload`; the only
speech key is `overloads_defender`; the first positive scope remains one
defender, two distinct non-king material duty targets, one legal test move
testing one target, and a legal reply map where no single reply preserves both
duty targets; Deflect, Decoy, Plan.Overload, mate threat, only-defender,
defender-must-move, forced, best, only-move, wins-material, wins-target,
decisive/winning, pressure/initiative, public route `200`, production API, and
public/user-facing LLM narration remain closed; `AGENTS.md` remains unchanged;
and detailed authority is not duplicated outside `StoryInteractionLaw.md`.

### Stage-1 OverloadProof

Stage-1 opens `OverloadProof` as the runtime proof home for narrow public
`Tactic.Overload`.

`OverloadProof` is not public Story. `OverloadProof` is only proof material
that lets the `TacticOverload` writer create a public `Tactic.Overload` Story.

Stage-1 OverloadProof requires:

- beforeBoard
- legal move route
- afterBoard
- side = mover
- rival = opponent
- defender = rival piece that carries dual duty
- target = tested duty target
- secondaryTarget = other duty target
- anchor = overloaded defender
- complete DefenderDuty relation
- complete DualDefenderDuty relation
- complete OverloadTest relation
- complete CannotSatisfyBoth relation
- same-board legal replay
- `proofComplete = true`

Stage-1 OverloadProof conditions:

- move is legal from beforeBoard to afterBoard.
- defender and both duty targets are on the exact proof board.
- both duty targets are distinct non-king material targets.
- tested target is one of the two duty targets.
- CannotSatisfyBoth proves that no legal rival reply preserves both duty
  targets.
- `publicClaimAllowed = false` means only that `OverloadProof` is not the
  public claim owner; it does not block `TacticOverload` Story admission from a
  complete proof tuple.

Stage-1 rules:

- `OverloadProof` is not public Story.
- `OverloadProof` cannot write Story.
- `OverloadProof` cannot create Verdict, ExplanationPlan, renderer text,
  LLM input, public route, or API output.
- BoardFacts may observe but cannot create `OverloadProof`.
- EngineCheck cannot create `OverloadProof`.
- raw engine eval/PV/SAN/proofFailures may not create `OverloadProof`.

Stage-1 must not open:

- Story writer
- StoryTable Lead
- Verdict
- ExplanationPlan
- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-1 OverloadProof closes when `OverloadProof` is the
only proof home for narrow `Tactic.Overload`, requires beforeBoard, legal move
route, afterBoard, side = mover, rival = opponent, defender, target,
secondaryTarget, anchor, complete DefenderDuty, DualDefenderDuty, OverloadTest,
and CannotSatisfyBoth relations, same-board legal replay, and
`proofComplete = true`; rejects incomplete DefenderDuty, incomplete
DualDefenderDuty, incomplete OverloadTest, incomplete CannotSatisfyBoth,
illegal replay, and board mismatch; keeps `publicClaimAllowed = false`; cannot
write Story or create Verdict, ExplanationPlan, renderer text, LLM input,
public route, or API output; BoardFacts may observe but cannot create
`OverloadProof`; EngineCheck and raw engine eval/PV/SAN/proofFailures cannot
create `OverloadProof`.

### Stage-2 TacticOverload Writer

Stage-2 opens named writer `TacticOverload`, Story label
`Tactic.Overload`, and speech key `overloads_defender`.

Stage-2 writer may create Story only when:

- complete OverloadProof
- complete slice proof
- complete DefenderDuty relation
- complete DualDefenderDuty relation
- complete OverloadTest relation
- complete CannotSatisfyBoth relation
- same-board legal replay
- legal route matches proof route
- proof identity is stable and complete
- defender is identified
- two duty targets are identified and distinct
- tested target is one of the two duty targets
- no legal reply preserves both duty targets
- writer = `TacticOverload`
- EngineCheck does not Refute

Stage-2 Story identity:

- scene = none, encoded as `Scene.Tactic` plus `Tactic.Overload`
- tactic = `Tactic.Overload`
- plan = None
- side = mover
- rival = opponent
- target = tested duty target square
- secondaryTarget = untested duty target square
- anchor = overloaded defender square
- route = legal move route

Stage-2 does not open:

- Material Story
- Hanging Story
- RemoveGuard Story
- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- Defense Story
- best / only / forced
- wins material
- mate threat
- raw proof detail in public text
- proofFailures in public text
- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-2 TacticOverload Writer closes when
`TacticOverload` is the only writer admitted for narrow `Tactic.Overload`;
it writes a Story only with complete OverloadProof, complete slice proof,
complete DefenderDuty, DualDefenderDuty, OverloadTest, and CannotSatisfyBoth
relations, same-board legal replay, a legal route matching the proof route,
stable complete proof identity, one identified defender, two distinct duty
targets, tested target among those duty targets, no legal reply preserving both
duty targets, and no EngineCheck Refutes; Story identity is fixed to scene
none encoded as `Scene.Tactic` plus `Tactic.Overload`, tactic
`Tactic.Overload`, plan None, side as mover, rival as opponent, target as
tested duty target square, secondaryTarget as untested duty target square,
anchor as overloaded defender square, and route as legal move route; speech key
is exactly `overloads_defender`; Material Story, Hanging Story, RemoveGuard
Story, Deflect, Decoy, Plan.Overload, Defense Story, best/only/forced,
wins-material, mate-threat, raw proof detail in public text, proofFailures in
public text, Renderer, LLM smoke, public route `200`, production API, and
public/user-facing LLM narration remain closed.

### Stage-3 Overload Negative Corpus

Stage-3 opens only the negative corpus for narrow `Tactic.Overload`.

Stage-3 must stay silent for:

- defender protects only one target
- two different defenders protect two different targets
- defender has two duties but current move does not test either duty target
- OverloadTest exists but CannotSatisfyBoth is absent
- at least one legal rival reply preserves both duty targets
- target is king
- route is illegal
- proof board and Story board differ
- attack-only row tries to become Overload
- Loose tries to become Overload
- QueenHit tries to become Overload
- Hanging tries to become Overload
- Material tries to become Overload
- RemoveGuard tries to become Overload
- Defense tries to become Overload
- engine eval/PV says advantage but proof is absent
- SAN/check/checkmate annotation tries to create Overload

Stage-3 forbidden wording:

- wins material
- wins a piece
- wins the queen
- forced
- only move
- best move
- decisive
- winning
- no counterplay
- cannot defend everything
- must choose
- loses one target
- defender is removed
- deflects
- decoys

Stage-3 rules:

- every negative fixture produces no public `Tactic.Overload` text.
- no negative fixture lowers to `overloads_defender`.
- proofFailures remain internal diagnostics only.

Completion standard: Stage-3 Overload Negative Corpus closes when every listed
single-defender-only, split-defender, untested-move, OverloadTest-without-
CannotSatisfyBoth, rival-reply-preserves-both, king-target, illegal-route,
proof-board/Story-board mismatch, attack-only, Loose, QueenHit, Hanging,
Material, RemoveGuard, Defense, engine eval/PV without proof, and
SAN/check/checkmate annotation case produces no public `Tactic.Overload` text
and never lowers to `overloads_defender`; forbidden wins material, wins a
piece, wins the queen, forced, only move, best move, decisive, winning, no
counterplay, cannot defend everything, must choose, loses one target, defender
is removed, deflects, and decoys wording remains closed; and proofFailures stay
internal diagnostics only.

### Stage-4 Overload EngineCheck Reuse

Stage-4 opens only:

- existing `EngineCheck` may support, cap, or refute an already proof-backed
  `Tactic.Overload` row.

Stage-4 EngineCheck rules:

- `TacticOverload` writer requires EngineCheck not Refute.
- capped and refuted Overload rows must not produce standalone public text.
- Support, Context, Blocked, capped, and refuted rows must not render as
  independent Overload narration.
- EngineCheck support may affect confidence or strength only within the
  existing bounded model.
- Engine evidence must bind to the same Story route and legal line.

Stage-4 remains closed:

- EngineCheck cannot create `OverloadProof`.
- EngineCheck cannot create `Tactic.Overload`.
- EngineCheck cannot repair incomplete proof.
- raw eval/PV cannot become public wording.

Stage-4 forbidden EngineCheck wording:

- best move
- only move
- forced
- winning
- engine says overload
- eval proves overload
- raw PV proves overload

Stage-4 does not open:

- raw PV public output
- eval public output
- engine-owned public wording
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-4 Overload EngineCheck Reuse closes when EngineCheck
may support, cap, or refute only an already proof-backed `Tactic.Overload`
row; `TacticOverload` requires EngineCheck not Refute; EngineCheck cannot
create `OverloadProof`, cannot create `Tactic.Overload`, and cannot repair
incomplete proof; capped and refuted Overload rows produce no standalone public
text; Support, Context, Blocked, capped, and refuted rows do not render as
independent Overload narration; support affects confidence or strength only
within the existing bounded model; engine evidence binds to the same Story
route and legal line; raw eval/PV never becomes public wording or downstream
input; forbidden best move, only move, forced, winning, engine says overload,
eval proves overload, and raw PV proves overload wording remains closed; and no
engine-owned public wording, public route `200`, production API, or
public/user-facing LLM narration is opened.

### Stage-5 Overload StoryTable Integration

Stage-5 opens only:

- StoryTable may order `Tactic.Overload` rows.

Stage-5 collision targets:

- `Tactic.RemoveGuard`
- `Scene.Defense`
- `Scene.Material`
- `Tactic.Hanging`
- `Tactic.Loose`
- `Tactic.QueenHit`
- `Tactic.Fork`
- `Tactic.Skewer`
- `Tactic.Pin`
- `Tactic.DiscoveredAttack`
- `Tactic.Deflect`
- `Tactic.Decoy`

Stage-5 StoryTable cannot:

- create Overload.
- convert Overload into `Scene.Material`.
- convert Overload into `Tactic.Hanging`.
- convert Overload into `Tactic.RemoveGuard`.
- convert Overload into `Tactic.Deflect`.
- convert Overload into `Tactic.Decoy`.
- promote Support, Context, Blocked, capped, or refuted Overload rows into standalone text.

Stage-5 ordering boundary:

- `Tactic.Overload` can be Lead only when selected, uncapped, unrefuted, and proof-backed.
- Material, Hanging, Loose, QueenHit, and RemoveGuard remain separate proof homes.
- If result Stories are present, they must keep their own labels and proof homes.
- Overload does not say the result by itself.
- Overload owns only the proof-backed overload relation from two
  duties + legal test + no reply preserves both.

Stage-5 rows that must not speak:

- Support
- Context
- Blocked
- capped
- refuted
- non-Lead
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-5 Overload StoryTable Integration closes when
StoryTable may order existing proof-backed `Tactic.Overload` rows, cannot
create Overload, cannot convert Overload into Material, Hanging, RemoveGuard,
Deflect, or Decoy, cannot promote Support, Context, Blocked, capped, refuted,
or non-Lead Overload rows into standalone text, `Tactic.Overload` can be Lead
only when selected, uncapped, unrefuted, and proof-backed, Material, Hanging,
Loose, QueenHit, and RemoveGuard remain separate proof homes, result Stories
keep their own labels and proof homes, Overload does not say the result by
itself, Overload owns only the proof-backed overload relation from two duties
plus legal test plus no legal reply preserving both, and public route `200`,
production API, and public/user-facing LLM narration remain closed.

### Stage-6 Overload ExplanationPlan

Stage-6 opens only:

- ExplanationPlan lowering for selected uncapped Lead `Tactic.Overload` only.
- claimKey: `overloads_defender`.

Stage-6 input conditions:

- selected Lead Verdict
- Story label = `Tactic.Overload`
- row is uncapped
- row is not refuted
- complete Story identity exists

Stage-6 closes:

- ExplanationPlan cannot accept raw OverloadProof directly.
- ExplanationPlan cannot accept BoardFacts directly.
- ExplanationPlan cannot accept EngineCheck directly.
- ExplanationPlan cannot lower Support, Context, Blocked, capped, or refuted rows.
- ExplanationPlan cannot add result claims.

Stage-6 allowed plan fields:

- claimKey = `overloads_defender`
- strength = bounded existing strength model
- side
- rival
- target
- anchor
- route
- forbidden wording list
- wins-material result claims and public wording do not enter the Overload ExplanationPlan.
- `wins_material` may appear only inside the forbidden wording list.

Stage-6 does not open:

- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-6 Overload ExplanationPlan closes when only a
selected uncapped Lead `Tactic.Overload` row with complete Story identity may
lower to claim key `overloads_defender`; the allowed plan fields are bounded
strength, side, rival, target, anchor, route, and forbidden wording list;
ExplanationPlan cannot accept raw OverloadProof, BoardFacts, or EngineCheck
directly; Support, Context, Blocked, capped, and refuted rows produce no
standalone plan; result claims are not added; wins-material result claims and
public wording do not enter the Overload ExplanationPlan except as a forbidden
wording-list item; and Renderer, LLM smoke, public route `200`, production API,
and public/user-facing LLM narration remain closed.

### Stage-7 Overload DeterministicRenderer

Stage-7 opens only:

- Renderer phrase for `overloads_defender`.

Stage-7 renderer input:

- ExplanationPlan only

Stage-7 allowed sentence:

- `This move overloads the defender.`

Stage-7 renderer cannot:

- inspect proof directly.
- inspect BoardFacts.
- inspect EngineCheck raw data.
- mention eval or PV.
- say wins material.
- say forced, best, or only.
- say decisive, winning, or no counterplay.
- say deflects, decoys, or removes the defender.

Stage-7 forbidden wording:

- wins material
- forced
- best move
- only move
- decisive
- winning
- no counterplay
- deflects
- decoys
- removes the defender
- eval
- PV

Stage-7 renderer rules:

- Renderer emits no text for Support/Context/Blocked/capped/refuted rows.

Stage-7 does not open:

- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Stage-7 Overload DeterministicRenderer closes when
renderer text for `overloads_defender` is created only from bounded Overload
ExplanationPlan input, the only admitted sentence is `This move overloads the
defender.`, Renderer does not inspect proof directly, BoardFacts, or EngineCheck
raw data, eval and PV are never mentioned, forbidden wins-material,
forced/best/only, decisive/winning/no-counterplay, and deflects/decoys/removes
the defender wording remains closed, Renderer emits no standalone Overload text
for Support, Context, Blocked, capped, or refuted rows, and LLM smoke, public
route `200`, production API, and public/user-facing LLM narration remain closed.

### Stage-8 Overload LLM Smoke

Stage-8 opens only:

- LLM smoke for an already rendered `overloads_defender` line.

Stage-8 LLM input:

- renderedText
- claimKey = `overloads_defender`
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Stage-8 LLM input cannot include:

- board
- FEN
- PV
- eval
- proof details
- source rows

Stage-8 LLM smoke must preserve:

- `overloads_defender` meaning only

Stage-8 LLM smoke rejects:

- wins material
- wins target
- forced
- best move
- only move
- decisive
- winning
- no counterplay
- mate threat
- engine line
- raw engine data
- new move
- new variation
- deflection
- decoy
- remove guard
- defense claim

Stage-8 LLM rules:

- LLM only polishes.
- Verifier rejects overclaim.
- no public/user-facing production narration.
- public/user-facing LLM narration remains closed.
- no new chess facts.

Stage-8 does not open:

- public route `200`
- production API

Completion standard: Stage-8 Overload LLM Smoke closes when LLM smoke may use
only an already rendered `overloads_defender` line; LLM input contains only
renderedText, claimKey = `overloads_defender`, strength, forbidden wording, and
`Rephrase only. Do not add chess facts.`; LLM input contains no board, FEN, PV,
eval, proof details, or source rows; LLM smoke preserves `overloads_defender`
meaning; verifier rejects wins-material, wins-target, forced, best, only,
decisive, winning, no-counterplay, mate-threat, engine-line, raw engine data,
new-move, new-variation, deflection, decoy, remove-guard, and defense wording;
LLM only polishes; public/user-facing production narration and new chess facts
remain closed; and public route `200` and production API remain closed.

### Stage-9 Overload Closeout / Hard Cleanup

Stage-9 opens no new chess meaning beyond narrow `Tactic.Overload`.

Stage-9 closes only the proof-backed overload relation:

- complete two-duty defender
- legal test move
- no legal reply preserves both duty targets

Stage-9 authority audit:

- `OverloadProof` owns proof home.
- `Tactic.Overload` owns Story label.
- `TacticOverload` owns writer.
- `overloads_defender` owns speech key.
- These are not interchangeable.
- DefenderDuty, DualDefenderDuty, OverloadTest, and CannotSatisfyBoth
  remain internal proof-readiness components.

Stage-9 duplication audit:

- Overload is not DefenderDuty.
- Overload is not DualDefenderDuty.
- Overload is not OverloadTest.
- Overload is not CannotSatisfyBoth alone.
- Overload is not RemoveGuard.
- Overload is not Defense.
- Overload is not Material.
- Overload is not Hanging.
- Overload is not Loose.
- Overload is not QueenHit.
- Overload is not Fork.
- Overload is not Skewer.
- Overload is not Pin.
- Overload is not DiscoveredAttack.
- Overload is not Deflect.
- Overload is not Decoy.
- Overload is not Plan.Overload.
- one chess meaning, one proof home, one Story label, one speech key.

Stage-9 still closed:

- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- wins material
- wins target
- defender must choose
- only defender
- best / only / forced
- decisive / winning
- no counterplay
- mate threat
- pressure / initiative
- public route 200
- production API
- public/user-facing LLM narration

Stage-9 docs duplication:

- detailed Overload authority appears only in `StoryInteractionLaw.md`.
- README/SSOT/Architecture/Contract/Manifest may contain summary only if touched.
- AGENTS.md remains unchanged.

Stage-9 public surface audit:

- `/api/commentary/render` remains fail-closed.
- `/internal/commentary/render-local-probe` remains fail-closed.
- public route 200 remains closed.
- production API remains closed.
- public/user-facing LLM narration remains closed.

Stage-9 required verification:

- `sbt "commentary/testOnly lila.commentary.chess.OverloadRuntimeAdmissionTest"`
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Completion standard: Stage-9 Overload Closeout / Hard Cleanup closes when only
a complete proof-backed two-duty defender plus legal test plus
no-reply-preserves-both relation can become `Tactic.Overload`; `OverloadProof`,
`Tactic.Overload`, `TacticOverload`, and `overloads_defender` remain separated;
DefenderDuty, DualDefenderDuty, OverloadTest, and CannotSatisfyBoth remain
internal proof components; sibling proof homes are not replaced;
Support/Context/Blocked/capped/refuted rows stay silent; public surfaces remain
fail-closed for `/api/commentary/render` and
`/internal/commentary/render-local-probe`; public route 200, production API,
and public/user-facing LLM narration remain closed; detailed docs authority is
not duplicated; AGENTS.md remains unchanged; and all verification passes.

### OIH-0 Charter / No New Meaning

OIH-0 locks the interaction boundary after `Tactic.Overload` runtime admission.

OIH-0 opens no new proof home, Story label, writer, or speech key.

OIH-0 fixed chain:

- `OverloadProof` -> `Tactic.Overload` -> `TacticOverload` -> `overloads_defender`

OIH-0 must not open:

- `Tactic.Deflect`
- `Tactic.Decoy`
- `Plan.Overload`
- public CannotSatisfyBoth label
- public DualDefenderDuty label
- wins material
- wins piece
- forced move
- best move
- only move
- decisive / winning / no-counterplay
- public route 200
- production API
- public/user-facing LLM narration

OIH-0 boundary rules:

- Overload interaction hardening may audit existing runtime admission only.
- It must not add a new proof sidecar, Story enum case, writer implementation,
  writer enum case, ExplanationClaim, speech key, renderer template, LLM prompt
  key, public route response, production API field, or docs-authority name.
- DefenderDuty, DualDefenderDuty, OverloadTest, and CannotSatisfyBoth remain
  internal proof components under `OverloadProof`; none becomes a public label.
- `overloads_defender` remains the only admitted speech key for the narrow
  overload relation.
- Existing sibling meanings keep their own homes; OIH-0 cannot reopen Deflect,
  Decoy, Plan.Overload, material gain, forced/best/only move, decisive/winning,
  no-counterplay, public route, production API, or public/user-facing LLM
  narration.

Completion standard: OIH-0 closes when the fixed chain remains the only Overload runtime admission path; no new proof home, Story label, writer, or speech key is opened; `Tactic.Deflect`, `Tactic.Decoy`, `Plan.Overload`, public CannotSatisfyBoth label, public DualDefenderDuty label, wins-material, wins-piece, forced-move, best-move, only-move, decisive/winning/no-counterplay, public route 200, production API, and public/user-facing LLM narration remain closed; detailed OIH-0 authority lives only in `StoryInteractionLaw.md`; AGENTS.md remains unchanged; targeted docs authority tests pass; and `git diff --check` passes.

### OIH-1 Proof Ingredient Boundary

OIH-1 locks `OverloadProof` so it cannot replace its internal readiness ingredients.

OIH-1 ingredient boundaries:

- DefenderDuty is internal guard relation only.
- DualDefenderDuty is internal dual-duty relation only.
- OverloadTest is internal test-move relation only.
- CannotSatisfyBoth is internal reply-exhaustion relation only.
- These four ingredients are not public Story.
- `OverloadProof` requires all four ingredients.
- `OverloadProof` does not own or replace those ingredients.

OIH-1 runtime tests:

- missing ingredient blocks `Tactic.Overload` creation.
- ingredient-only proof creates no public Story.
- BoardFacts cannot create an ingredient by itself.
- EngineCheck cannot create or repair an ingredient.
- raw PV and eval cannot create or repair an ingredient.
- proofFailures cannot create or repair an ingredient.

OIH-1 boundary rules:

- `OverloadProof.complete` is false unless DefenderDuty, DualDefenderDuty,
  OverloadTest, and CannotSatisfyBoth are all present and complete.
- A complete ingredient remains internal readiness only. It does not become a
  Story, StoryTable row, Verdict, ExplanationPlan, renderer input, LLM input,
  public route value, production API value, or user-facing narration.
- BoardFacts may observe board relations used by named proof builders, but
  BoardFacts alone must not create a public overload ingredient or repair a
  missing ingredient inside `OverloadProof`.
- EngineCheck, raw PV, eval, and proofFailures may diagnose, support, cap, or
  refute already admitted rows only through existing boundaries; they cannot
  create or repair DefenderDuty, DualDefenderDuty, OverloadTest,
  CannotSatisfyBoth, or `OverloadProof`.
- `TacticOverload` remains the only writer for `Tactic.Overload`, and it may
  write only from complete `OverloadProof` whose four named ingredients are
  present and complete.

Completion standard: OIH-1 closes when `OverloadProof` remains incomplete without every named ingredient; DefenderDuty remains internal guard relation only; DualDefenderDuty remains internal dual-duty relation only; OverloadTest remains internal test-move relation only; CannotSatisfyBoth remains internal reply-exhaustion relation only; those four ingredients are not public Story; `OverloadProof` requires all four but does not own or replace them; missing ingredients block `Tactic.Overload`; ingredient-only proof creates no public Story; BoardFacts, EngineCheck, raw PV, eval, and proofFailures cannot create or repair ingredients; detailed OIH-1 authority lives only in `StoryInteractionLaw.md`; targeted runtime tests pass; docs authority tests pass; and `git diff --check` passes.

### OIH-2 Neighbor Story Collision Audit

OIH-2 prevents `Tactic.Overload` from stealing neighboring tactical or scene meanings.

OIH-2 collision targets:

- RemoveGuard
- Defense
- Material
- Hanging
- Loose
- QueenHit
- Fork
- Skewer
- Pin
- DiscoveredAttack
- Deflect
- Decoy

OIH-2 speech boundary:

- Overload may say only that a defender cannot maintain two duties at once.
- Without Material or Hanging proof, Overload must not say `wins material`.
- Without RemoveGuard proof, Overload must not say `defender is removed`.
- Without Deflect or Decoy proof, Overload must not say `deflects` or `decoys`.

OIH-2 runtime tests:

- each sibling fixture keeps its own Story label.
- an Overload fixture emits no sibling Story unless separate complete proof exists.
- a sibling fixture cannot lower to `overloads_defender`.

OIH-2 boundary rules:

- `OverloadProof` proves only the dual-duty overload relation. It does not prove
  material gain, hanging material, guard removal, fork, skewer, pin,
  discovered attack, queen attack, loose piece attack, defense, deflection, or
  decoy.
- A sibling Story may speak only through its own existing proof home, Story
  label, writer, and speech key. Overlap with an Overload board position does
  not let `TacticOverload` create or rename that sibling meaning.
- If an Overload fixture also admits a sibling Story, that sibling must carry a
  separate complete proof sidecar owned by the sibling writer.
- Deflect and Decoy remain closed labels. OIH-2 does not open a proof home,
  writer, speech key, renderer text, LLM narration, route 200, or production API
  for either label.
- Sibling fixtures contaminated with `OverloadProof` must not lower to
  `overloads_defender`. They may remain blocked or speak only their own already
  admitted sibling claim when their own proof home is complete.
- `overloads_defender` remains bounded to the claim that the defender cannot
  maintain both duties at once. It must not imply winning material, winning a
  piece, removing a defender, deflecting, decoying, forcing, best move, only
  move, decisive, winning, or no-counterplay.

Completion standard: OIH-2 closes when neighbor Story labels keep their own proof homes; RemoveGuard, Defense, Material, Hanging, Loose, QueenHit, Fork, Skewer, Pin, DiscoveredAttack, Deflect, and Decoy cannot be stolen by Overload; Overload says only that a defender cannot maintain two duties at once; wins-material wording requires Material or Hanging proof; removed-defender wording requires RemoveGuard proof; deflects or decoys wording requires Deflect or Decoy proof; each sibling fixture keeps its own Story label; an Overload fixture emits no sibling Story unless separate complete proof exists; sibling fixtures cannot lower to `overloads_defender`; detailed OIH-2 authority lives only in `StoryInteractionLaw.md`; targeted runtime tests pass; docs authority tests pass; and `git diff --check` passes.

### OIH-3 EngineCheck / Row-State Boundary

OIH-3 locks EngineCheck and row state below Overload claim creation or strengthening.

OIH-3 EngineCheck boundary:

- EngineCheck may only support, cap, or refute an already proof-backed Overload row.
- EngineCheck cannot create `OverloadProof`.
- EngineCheck cannot repair incomplete `OverloadProof`.
- raw eval and PV cannot become public text.
- raw eval and PV cannot become renderer input.
- raw eval and PV cannot become LLM smoke input.

OIH-3 row-state boundary:

- Support Overload rows produce no standalone text.
- Context Overload rows produce no standalone text.
- Blocked Overload rows produce no standalone text.
- capped Overload rows produce no standalone text.
- refuted Overload rows produce no standalone text.

OIH-3 runtime tests:

- proof absent plus Engine support creates no Overload.
- proof complete plus cap creates no standalone Overload text.
- proof complete plus refute creates no standalone Overload text.
- support, context, and blocked rows do not render independently.

OIH-3 boundary rules:

- `TacticOverload.withEngineCheck` may attach EngineCheck only to a Story that
  already satisfies the complete Overload writer, proof, identity, and route
  binding.
- A Supports EngineCheck keeps an already admitted Lead row eligible, but
  it does not create a Story, create `OverloadProof`, repair missing proof
  ingredients, change the speech key, or strengthen wording.
- A Caps EngineCheck suppresses standalone Overload text.
- A Refutes EngineCheck blocks standalone Overload text.
- Support, Context, Blocked, capped, and refuted Overload rows must not produce
  `ExplanationPlan`, `RenderedLine`, LLM smoke input, public route value,
  production API value, or user-facing narration.
- Engine eval, raw PV, checked move, reply line, depth, freshness, and engine
  status are internal diagnostics only. They do not enter renderer text, LLM
  smoke prompt fields, `Verdict.values`, or public payloads.

Completion standard: OIH-3 closes when EngineCheck and row state remain unable to create or inflate Overload; EngineCheck can only support, cap, or refute an already proof-backed Overload row; EngineCheck cannot create `OverloadProof`; EngineCheck cannot repair incomplete `OverloadProof`; Support, Context, Blocked, capped, and refuted Overload rows produce no standalone text; raw eval and PV do not enter public text, renderer input, or LLM smoke input; proof absent plus Engine support creates no Overload; proof complete plus cap creates no standalone Overload text; proof complete plus refute creates no standalone Overload text; support, context, and blocked rows do not render independently; detailed OIH-3 authority lives only in `StoryInteractionLaw.md`; targeted runtime tests pass; docs authority tests pass; and `git diff --check` passes.

### OIH-4 StoryTable Ordering / Duplication

OIH-4 locks StoryTable below Overload creation and wording conversion.

OIH-4 StoryTable boundary:

- StoryTable may order existing proof-backed Overload rows but cannot create Overload.
- `Tactic.Overload` may lower standalone only from a selected uncapped Lead row.
- StoryTable cannot convert Overload rows into Material, Hanging, RemoveGuard, Deflect, or Decoy rows.
- StoryTable cannot convert result rows into Overload rows.
- duplicate Overload source rows must not create duplicate public narration.
- Overload and result Stories may coexist only when each keeps its own proof home and Story label.

OIH-4 runtime tests:

- one proof-backed Overload creates one selected uncapped Lead row.
- duplicate Overload source rows create no duplicate public narration.
- StoryTable does not turn Overload into result wording.
- result Stories keep their proof homes and do not become Overload wording.

OIH-4 boundary rules:

- `StoryTable.choose` consumes Story rows already created by named proof-backed
  writers. It may assign Lead, Support, Context, or Blocked roles, but it must
  not open a proof home, Story label, writer, or speech key.
- Only a selected uncapped Lead `Tactic.Overload` row with complete
  `OverloadProof`, `StoryWriter.TacticOverload`, and the `overloads_defender`
  claim may lower to standalone Overload text.
- Support, Context, Blocked, capped, refuted, and non-Lead Overload rows must
  not create `ExplanationPlan`, `RenderedLine`, LLM smoke input, public route
  value, production API value, or user-facing narration.
- Duplicate Overload source rows may exist as StoryTable input, but only the
  selected Lead row may produce the bounded `overloads_defender` line.
- StoryTable must not treat Overload as material result, hanging piece,
  removed defender, deflection, or decoying. Those meanings require their own
  admitted proof homes and Story labels.
- When Overload coexists with Material, Hanging, RemoveGuard, Defense, Loose,
  QueenHit, Fork, Skewer, Pin, DiscoveredAttack, or another opened result Story,
  each row keeps its original writer, proof home, Story label, and speech key.

Completion standard: OIH-4 closes when StoryTable can only order Overload rows; StoryTable cannot create Overload; `Tactic.Overload` lowers standalone only from a selected uncapped Lead row; StoryTable cannot convert Overload rows into Material, Hanging, RemoveGuard, Deflect, or Decoy rows; StoryTable cannot convert result rows into Overload rows; duplicate Overload source rows create no duplicate public narration; Overload and result Stories can coexist only with their own proof homes and Story labels intact; one proof-backed Overload creates one selected uncapped Lead row; StoryTable does not turn Overload into result wording; result Stories keep their proof homes and do not become Overload wording; detailed OIH-4 authority lives only in `StoryInteractionLaw.md`; targeted runtime tests pass; docs authority tests pass; and `git diff --check` passes.

### OIH-5 ExplanationPlan / Renderer / LLM Leak Guard

OIH-5 locks downstream expression below Overload meaning creation or expansion.

OIH-5 ExplanationPlan boundary:

- ExplanationPlan accepts only selected uncapped Lead `Tactic.Overload`.
- Overload ExplanationPlan claim key is `overloads_defender` only.
- ExplanationPlan must not take raw `OverloadProof`, BoardFacts, or EngineCheck as direct input.

OIH-5 renderer boundary:

- Renderer input is ExplanationPlan only.
- Overload renderer text is exactly `This move overloads the defender.`

OIH-5 LLM smoke boundary:

- LLM smoke input is renderedText, claimKey, strength, and forbidden wording only.
- LLM smoke instruction is `Rephrase only. Do not add chess facts.`

OIH-5 forbidden wording:

- wins material
- wins a piece
- wins the queen
- forced
- only move
- best move
- decisive
- winning
- no counterplay
- deflects
- decoys
- removes the defender
- cannot defend everything
- must choose

OIH-5 runtime tests:

- ExplanationPlan lowers only selected uncapped Lead Overload to `overloads_defender`.
- renderer emits only the bounded Overload sentence from ExplanationPlan.
- LLM smoke input is bounded and rejects forbidden Overload expansions.

OIH-5 boundary rules:

- ExplanationPlan receives the selected Verdict handoff only. It may not inspect
  raw `OverloadProof`, BoardFacts, EngineCheck, raw PV, raw eval, source rows,
  or proof failures to create or strengthen Overload.
- ExplanationPlan must return no Overload plan for Support, Context, Blocked,
  capped, refuted, non-Lead, wrong-tactic, wrong-writer, incomplete-proof, or
  sibling-result rows.
- DeterministicRenderer receives only an already bounded ExplanationPlan and
  may emit only the exact Overload sentence above.
- LLM smoke receives only the bounded rendered line fields and forbidden
  wording. It must reject any output that adds material gain, piece gain, queen
  gain, forced/best/only/decisive/winning/no-counterplay language, Deflect,
  Decoy, RemoveGuard, or broader dual-duty wording such as "cannot defend
  everything" or "must choose".
- Public route `200`, production API behavior, and public/user-facing LLM
  narration remain closed.

Completion standard: OIH-5 closes when downstream expression cannot create or expand Overload; ExplanationPlan accepts only selected uncapped Lead `Tactic.Overload`; Overload ExplanationPlan claim key is `overloads_defender` only; ExplanationPlan does not take raw `OverloadProof`, BoardFacts, or EngineCheck as direct input; Renderer input is ExplanationPlan only; Overload renderer text is exactly `This move overloads the defender.`; LLM smoke input is renderedText, claimKey, strength, and forbidden wording only; LLM smoke instruction is `Rephrase only. Do not add chess facts.`; wins material, wins a piece, wins the queen, forced, only move, best move, decisive, winning, no counterplay, deflects, decoys, removes the defender, cannot defend everything, and must choose are forbidden; targeted runtime tests pass; docs authority tests pass; and `git diff --check` passes.

### OIH-6 Docs / Public Surface Authority

OIH-6 locks detailed Overload interaction hardening authority to `StoryInteractionLaw.md` only.

OIH-6 docs authority boundary:

- OIH detailed authority lives only in `StoryInteractionLaw.md`.
- README.md is summary-only for OIH.
- ChessCommentarySSOT.md is summary-only for OIH.
- ChessModelArchitecture.md is summary-only for OIH.
- ChessModelContract.md is summary-only for OIH.
- LegacyPruneManifest.md is summary-only for OIH.
- AGENTS.md must not change for OIH-6.

OIH-6 public surface boundary:

- `/api/commentary/render` remains fail-closed.
- `/internal/commentary/render-local-probe` remains fail-closed.
- public route `200` remains closed.
- production API remains closed.
- public/user-facing LLM narration remains closed.

OIH-6 runtime and docs tests:

- ChessDocsAuthorityTest guards OIH authority against duplication.
- public route and API tests confirm both commentary routes stay fail-closed.

OIH-6 boundary rules:

- OIH-0 through OIH-6 detailed headings, rule lists, runtime corpus, completion
  standards, and no-go wording belong to `StoryInteractionLaw.md` only.
- README, SSOT, Architecture, Contract, and Manifest may summarize OIH status
  only. They must not duplicate detailed OIH headings, boundary lists, runtime
  tests, or completion standards.
- `AGENTS.md` remains an operator guide and must not gain OIH-6 detailed law.
- The commentary controller remains a tombstone surface that returns service
  unavailable with `noCommentary` and `render` null.
- Routes may remain declared as fail-closed tombstones, but must not return
  `Ok`, rendered payloads, production switches, Overload writer/proof data, or
  public/user-facing LLM narration.

Completion standard: OIH-6 closes when detailed OIH authority has exactly one live owner; OIH detailed authority lives only in `StoryInteractionLaw.md`; README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, ChessModelContract.md, and LegacyPruneManifest.md remain summary-only for OIH; AGENTS.md remains unchanged; `/api/commentary/render` remains fail-closed; `/internal/commentary/render-local-probe` remains fail-closed; public route `200`, production API, and public/user-facing LLM narration remain closed; ChessDocsAuthorityTest guards OIH authority against duplication; public route and API tests confirm both commentary routes stay fail-closed; docs authority tests pass; and `git diff --check` passes.

### OIH-7 Closeout / Hard Cleanup

OIH-7 closes Overload interaction hardening without opening new chess meaning.

OIH-7 fixed authority chain:

- `OverloadProof` is the proof home.
- `Tactic.Overload` is the Story label.
- `TacticOverload` is the named writer.
- `overloads_defender` is the speech key.

OIH-7 final checks:

- DefenderDuty, DualDefenderDuty, OverloadTest, and CannotSatisfyBoth are internal proof ingredients only.
- Support, Context, Blocked, capped, and refuted Overload rows have no standalone text.
- sibling tactic, material, and defense Stories keep their own meanings.
- forbidden wording does not leak.
- raw engine eval, PV, and proofFailures do not leak publicly.
- public route, production API, and public/user-facing LLM remain closed.
- AGENTS.md remains unchanged.

OIH-7 required verification:

- `sbt "commentary/testOnly lila.commentary.chess.OverloadInteractionHardeningTest"`
- `sbt "commentary/testOnly lila.commentary.chess.OverloadRuntimeAdmissionTest"`
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `sbt "testOnly controllers.CommentaryTest"`
- `git diff --check`

OIH-7 cleanup rules:

- No new proof home, Story label, writer, speech key, public label, route,
  production API field, renderer template, or public/user-facing LLM narration
  opens during closeout.
- The four opened Overload names remain separate and non-interchangeable:
  proof home, Story label, named writer, and speech key.
- The four internal ingredients remain below public Story authority and cannot
  independently lower to Story, Verdict, ExplanationPlan, renderer text, LLM
  smoke input, public route payload, or production API output.
- Existing sibling homes remain authoritative for their meanings. Overload
  cannot borrow material gain, hanging piece, removed defender, defense,
  deflection, decoying, best/only/forced move, decisive/winning/no-counterplay,
  or raw engine support wording.
- OIH closeout must leave public surfaces fail-closed and detailed authority in
  `StoryInteractionLaw.md` only.

Completion standard: OIH-7 closes when the Overload interaction hardening chain remains fixed as `OverloadProof` proof home, `Tactic.Overload` Story label, `TacticOverload` named writer, and `overloads_defender` speech key; DefenderDuty, DualDefenderDuty, OverloadTest, and CannotSatisfyBoth remain internal proof ingredients only; Support, Context, Blocked, capped, and refuted rows have no standalone text; sibling tactic, material, and defense Stories keep their own meanings; forbidden wording does not leak; raw engine eval, PV, and proofFailures do not leak publicly; public route, production API, and public/user-facing LLM remain closed; AGENTS.md remains unchanged; `OverloadInteractionHardeningTest`, `OverloadRuntimeAdmissionTest`, `ChessFoundationTest`, `ChessDocsAuthorityTest`, and `CommentaryTest` pass; and `git diff --check` passes.

## Stage-0: OCBA Charter

Name: Overload Consequence Boundary Audit.

OCBA opens audit-only authority for possible Overload consequence meanings.

OCBA opens collision inventory for Overload vs Material, Hanging, Loose, QueenHit, Fork, RemoveGuard, Defense, CannotSatisfyBoth, and EngineCheck.

OCBA opens no runtime result composition.

OCBA keeps closed:

- no Overload result Story
- no OverloadResultProof
- no wins-material-by-overload claim
- no forced-choice claim
- no only-move/best-move/forced wording
- no public CannotSatisfyBoth
- no new ExplanationPlan claim
- no renderer text
- no LLM smoke expansion
- no public route `200`
- no production API

OCBA proof authority:

- `OverloadProof` owns only narrow `Tactic.Overload`.
- CannotSatisfyBoth remains internal readiness only.
- `Scene.Material` owns material-now meaning.
- EngineCheck may support, cap, or refute only an existing Story.

OCBA forbidden wording:

- overload wins material
- defender must choose
- defender cannot save both
- forced
- only move
- best move
- decisive
- winning
- no counterplay

Completion standard: Stage-0 OCBA closes when docs authority proves this slice is audit-only, collision inventory exists for Overload versus Material, Hanging, Loose, QueenHit, Fork, RemoveGuard, Defense, CannotSatisfyBoth, and EngineCheck, `OverloadProof` owns only narrow `Tactic.Overload`, CannotSatisfyBoth remains internal readiness only, `Scene.Material` owns material-now meaning, EngineCheck may support, cap, or refute only an existing Story, no runtime result composition opens, no Overload result Story, OverloadResultProof, wins-material-by-overload claim, forced-choice claim, public CannotSatisfyBoth, new ExplanationPlan claim, renderer text, LLM smoke expansion, public route `200`, or production API opens, forbidden wording remains closed, docs authority tests pass, and `git diff --check` passes.

## Stage-1: OCBA Parallel Audit Inventory

Stage-1 opens read-only inventory reports only.

Stage-1 report inventory:

- Subagent A returned Overload authority inventory for Overload, DefenderDuty, DualDefenderDuty, OverloadTest, and CannotSatisfyBoth.
- Subagent B returned runtime ownership inventory for OverloadProof, TacticOverload, StoryTable, Verdict, ExplanationPlan, DeterministicRenderer, and LLM smoke reach.
- Subagent C returned collision test inventory for Overload consequence leakage around Material, Hanging, Loose, QueenHit, Fork, RemoveGuard, Defense, CannotSatisfyBoth, and EngineCheck.

Stage-1 integrated findings:

- Overload already reaches StoryTable, Verdict, ExplanationPlan, renderer, and LLM smoke through the narrow `overloads_defender` path.
- current renderer wording is only `This move overloads the defender.`
- result and forced-choice wording currently appears only as internal diagnostics or forbidden wording guards.
- missing negative fixture inventory covers Material, Hanging, Loose, QueenHit, Fork, RemoveGuard, Defense, CannotSatisfyBoth wording variants, and EngineCheck-backed consequence phrasing.

Stage-1 keeps closed:

- no implementation
- no runtime result composition
- no Overload result Story
- no OverloadResultProof
- no wins-material-by-overload claim
- no forced-choice claim
- no public CannotSatisfyBoth
- no new ExplanationPlan claim
- no renderer text
- no LLM smoke expansion
- no public route `200`
- no production API

Stage-1 subagent boundary:

- subagents were read-only.
- subagents did not edit docs.
- subagents did not write tests.
- integrator summarized all three reports before editing.

Completion standard: Stage-1 OCBA closes when all three read-only audit reports are summarized, Overload authority inventory identifies opened and closed meanings, runtime ownership inventory identifies Overload reach through StoryTable, Verdict, ExplanationPlan, renderer, and LLM smoke, collision test inventory identifies missing negative fixtures for consequence leakage, subagents remain read-only, no implementation or runtime result composition opens, no docs or tests are written by subagents, no Overload result Story, OverloadResultProof, wins-material-by-overload claim, forced-choice claim, public CannotSatisfyBoth, new ExplanationPlan claim, renderer text, LLM smoke expansion, public route `200`, or production API opens, docs authority tests pass, and `git diff --check` passes.

## Stage-2: OCBA Consequence Ownership Matrix

Stage-2 opens only an audit matrix mapping possible consequence meanings to existing owners.

| Possible consequence meaning | Existing Story owner | Existing proof home | Existing speech key |
| --- | --- | --- | --- |
| overloads defender | `Tactic.Overload` | `OverloadProof` | `overloads_defender` |
| material gain now | `Scene.Material` | `CaptureResult` | existing Material speech key |
| loose or undefended target | `Tactic.Loose` | `LoosePieceProof` | existing Loose speech key |
| hanging target | `Tactic.Hanging` | `CaptureResult` | existing Hanging speech key |
| queen attacked | `Tactic.QueenHit` | `QueenHitProof` | existing QueenHit speech key |
| defender removed | `Tactic.RemoveGuard` | `RemoveGuardProof` | existing RemoveGuard speech key |
| cannot satisfy both | internal only | CannotSatisfyBoth readiness | none |
| engine refutes/caps/supports | EngineCheck only | EngineCheck diagnostics | none |

Stage-2 keeps closed:

- no new owner for consequence meaning
- no second speech key for existing meanings
- no Overload borrowing Material or Loose wording

Stage-2 proof authority:

- one chess meaning, one proof home, one Story label, one speech key.

Stage-2 forbidden wording:

- Overload proves material
- Overload proves hanging
- Overload proves loose
- Overload proves forced win
- Overload proves no defense

Completion standard: Stage-2 OCBA closes when the consequence ownership matrix maps every required possible consequence meaning to its existing owner, no new owner for consequence meaning opens, no second speech key for existing meanings opens, Overload cannot borrow Material or Loose wording, one chess meaning has one proof home, one Story label, and one speech key, forbidden Overload-proves-material, Overload-proves-hanging, Overload-proves-loose, Overload-proves-forced-win, and Overload-proves-no-defense wording remains closed, docs authority tests confirm the matrix lives only in `StoryInteractionLaw.md`, and `git diff --check` passes.

## Stage-3: OCBA Negative Corpus Lock

Stage-3 opens targeted audit tests proving Overload consequence wording stays closed.

Stage-3 required negative fixtures:

- complete `Tactic.Overload` next to Material: Material owns material wording.
- complete `Tactic.Overload` next to Loose: Loose owns loose-piece wording.
- complete `Tactic.Overload` next to RemoveGuard: RemoveGuard owns removed-guard wording.
- Overload with CannotSatisfyBoth readiness only: no public cannot-save-both text.
- EngineCheck Supports Overload: no stronger consequence claim.
- EngineCheck Caps Overload: no standalone consequence text.
- EngineCheck Refutes Overload: no Overload text.

Stage-3 keeps closed:

- no result-composition implementation
- no new renderer template
- no new LLM prompt field

Stage-3 proof authority:

- StoryTable orders existing proof-backed rows only.
- Support, Context, Blocked, capped, and refuted rows produce no standalone consequence text.

Completion standard: Stage-3 OCBA closes when targeted audit tests prove Overload cannot borrow Material, Loose, RemoveGuard, CannotSatisfyBoth, or EngineCheck consequence wording, no result-composition implementation, renderer template, or LLM prompt field opens, existing Overload runtime and hardening tests still pass, docs authority tests pass, and `git diff --check` passes.

## Stage-4: OCBA Downstream Boundary Audit

Stage-4 opens audit checks for ExplanationPlan, renderer, and LLM boundaries.

Stage-4 keeps closed:

- ExplanationPlan still accepts selected uncapped Lead Verdict only.
- renderer input remains ExplanationPlan only.
- LLM smoke input remains renderedText, claimKey, strength, forbidden wording only.
- LLM instruction remains: `Rephrase only. Do not add chess facts.`

Stage-4 proof authority:

- `overloads_defender` is the only bounded Overload speech key.
- no downstream layer may add material, force, best/only, defender-choice, or win wording.

Completion standard: Stage-4 OCBA closes when tests assert forbidden consequence phrases do not appear in rendered text or LLM smoke accepted output, ExplanationPlan remains limited to selected uncapped Lead Verdict input, renderer input remains ExplanationPlan only, LLM smoke input remains renderedText, claimKey, strength, and forbidden wording only, the LLM instruction remains `Rephrase only. Do not add chess facts.`, `overloads_defender` remains the only bounded Overload speech key, no downstream layer adds material, force, best/only, defender-choice, or win wording, docs authority tests pass, and `git diff --check` passes.

## Stage-5: OCBA Public Surface / Docs Boundary

Stage-5 opens docs authority coverage for audit-only OCBA law.

Stage-5 keeps closed:

- README, SSOT, Architecture, Contract, and Manifest remain summary-only unless tests require a one-line summary.
- AGENTS.md unchanged.
- public route `200` closed.
- production API closed.
- public/user-facing LLM narration closed.

Stage-5 verification:

- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- public route tombstone assertions remain intact.

Completion standard: Stage-5 OCBA closes when docs authority coverage proves OCBA remains audit-only, README, SSOT, Architecture, Contract, and Manifest remain summary-only unless tests require a one-line summary, AGENTS.md remains unchanged, public route `200`, production API, and public/user-facing LLM narration remain closed, public route tombstone assertions remain intact, docs authority tests pass, and `git diff --check` passes.

## Stage-6: OCBA Closeout

Stage-6 opens final audit closeout only.

Stage-6 closeout conditions:

- Overload consequence ownership matrix exists.
- no new public Overload consequence meaning opened.
- no OverloadResultProof or equivalent result proof home added.
- existing Material, Loose, Hanging, QueenHit, RemoveGuard, Defense, CannotSatisfyBoth, and EngineCheck homes keep their meanings.
- `overloads_defender` remains the only Overload speech key.
- detailed authority lives only in `StoryInteractionLaw.md`.

Stage-6 verification:

- `sbt "commentary/testOnly lila.commentary.chess.OverloadRuntimeAdmissionTest"`
- `sbt "commentary/testOnly lila.commentary.chess.OverloadInteractionHardeningTest"`
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Completion standard: Stage-6 OCBA closes when final audit closeout proves no public Overload consequence meaning opened, the Stage-2 ownership matrix remains present, no OverloadResultProof or equivalent result proof home exists, existing Material, Loose, Hanging, QueenHit, RemoveGuard, Defense, CannotSatisfyBoth, and EngineCheck homes keep their meanings, `overloads_defender` remains the only Overload speech key, detailed authority lives only in `StoryInteractionLaw.md`, required verification passes, and `git diff --check` passes.

## Stage-0: Overload Narrow Expansion Audit Charter

Stage-0 opens audit-only scope for possible next `Tactic.Overload` narrow expansion.

Stage-0 opens:

- detailed audit law in `StoryInteractionLaw.md`
- internal audit tests that prove whether any next Overload meaning is unique or duplicate

Stage-0 keeps closed:

- no new Overload Story label
- no OverloadResultProof
- no material/result/consequence claim
- no writer
- no speech key
- no ExplanationPlan claim
- no renderer text
- no LLM smoke expansion except forbidden-boundary checks if needed
- no public route `200`
- no production API
- no public/user-facing LLM narration

Stage-0 proof authority:

- Existing opened chain remains exactly:
  `OverloadProof -> Tactic.Overload -> TacticOverload -> overloads_defender`
- Internal ingredients remain internal:
  `DefenderDuty`, `DualDefenderDuty`, `OverloadTest`, `CannotSatisfyBoth`
- EngineCheck supports, caps, or refutes only an existing Story and cannot create expansion authority.

Stage-0 forbidden wording:

- Do not say Overload wins material.
- Do not say Overload forces a result.
- Do not say the defender cannot satisfy both as public Overload text.
- Do not say the queen/piece becomes loose, hanging, removed, won, trapped, or undefended from Overload.
- Do not say best move, only move, winning, decisive, forced, no counterplay, or engine-approved.

Completion standard: Stage-0 Overload Narrow Expansion Audit closes when docs authority proves this slice is audit-only for possible next `Tactic.Overload` narrow expansion; any internal audit tests prove whether the next possible Overload meaning is unique or duplicate before runtime expansion; the opened chain remains exactly `OverloadProof -> Tactic.Overload -> TacticOverload -> overloads_defender`; DefenderDuty, DualDefenderDuty, OverloadTest, and CannotSatisfyBoth remain internal ingredients only; EngineCheck supports, caps, or refutes only an existing Story and cannot create expansion authority; no new Overload Story label, OverloadResultProof, material/result/consequence claim, writer, speech key, ExplanationPlan claim, renderer text, LLM smoke expansion except forbidden-boundary checks if needed, public route `200`, production API, or public/user-facing LLM narration opens; forbidden wording remains closed; docs authority tests pass; and `git diff --check` passes.

## Stage-1: Overload Read-Only Expansion Inventory

Stage-1 opens read-only expansion inventory only.

Stage-1 audit lanes:

- Overload runtime/proof path audit
- neighbor meaning ownership audit
- downstream wording/negative corpus audit

Stage-1 subagent boundary:

- subagents were read-only.
- subagents did not edit files.
- subagents did not write tests.

Stage-1 inventoried Overload path:

- `OverloadProof`
- `TacticOverload`
- StoryTable behavior
- ExplanationPlan mapping
- renderer mapping
- LLM forbidden wording
- tests
- `StoryInteractionLaw.md`

Stage-1 path findings:

- `OverloadProof` is the proof sidecar and remains non-public by itself.
- `TacticOverload` writes only the existing `Tactic.Overload` row from complete proof.
- StoryTable orders and blocks rows; it does not create Overload.
- ExplanationPlan lowers only selected uncapped Lead Overload to `overloads_defender`.
- renderer text remains exactly `This move overloads the defender.`
- LLM smoke input remains rendered text, claim key, strength, forbidden wording, and rephrase-only instruction.
- tests already cover the proof, writer, negatives, EngineCheck, StoryTable, ExplanationPlan, renderer, LLM smoke, runtime admission, interaction hardening, and consequence negative corpus paths.

Stage-1 neighbor owner inventory:

- Material: owned elsewhere.
- Loose: owned elsewhere.
- Hanging: owned elsewhere.
- QueenHit: owned elsewhere.
- RemoveGuard: owned elsewhere.
- Defense: owned elsewhere.
- CannotSatisfyBoth: owned elsewhere as internal readiness.
- EngineCheck: owned elsewhere as support/cap/refute diagnostics.

Stage-1 candidate inventory:

| candidate | current status | owner | audit note |
| --- | --- | --- | --- |
| overloads defender | duplicate | `OverloadProof -> Tactic.Overload -> TacticOverload -> overloads_defender` | existing narrow path already owns this wording. |
| `Plan.Overload` | duplicate | existing plan enum only | no main runtime public path found. |
| overloaded piece observation | owned elsewhere | root observation / BoardFacts observation layer | observation does not create Story or speech. |
| material gain now | owned elsewhere | `Scene.Material` / `CaptureResult` | Overload cannot borrow material-now wording. |
| wins material or wins piece | owned elsewhere | Material or Hanging proof paths | broader consequence wording has no Overload owner. |
| loose or undefended target | owned elsewhere | `Tactic.Loose` / `LoosePieceProof` | Overload cannot borrow loose-piece wording. |
| hanging target | owned elsewhere | `Tactic.Hanging` / `CaptureResult` | Overload cannot borrow hanging-piece wording. |
| queen attacked | owned elsewhere | `Tactic.QueenHit` / `QueenHitProof` | Overload cannot borrow queen-attack wording. |
| defender removed | owned elsewhere | `Tactic.RemoveGuard` / `RemoveGuardProof` | Overload cannot borrow removed-defender wording. |
| defense unavailable or refuted | owned elsewhere | `Scene.Defense` and `EngineCheck.Refutes` | EngineCheck may only affect existing rows. |
| cannot satisfy both public wording | owned elsewhere | internal CannotSatisfyBoth readiness only | no speech key, renderer text, or public label. |
| EngineCheck-backed stronger wording | owned elsewhere | `EngineCheck` diagnostics | no speech key and no expansion authority. |
| forced-choice / defender must choose | possibly unique | none current | current law forbids this wording. |
| Overload result/composition proof home | possibly unique | none current | no `OverloadResultProof` equivalent found. |

Stage-1 keeps closed:

- no implementation
- no new proof home
- no new public Story
- no renderer or LLM changes
- no new ExplanationPlan claim
- no public route `200`
- no production API
- no public/user-facing LLM narration

Completion standard: Stage-1 Overload Read-Only Expansion Inventory closes when the read-only audit inventory records the current Overload path, neighbor owners, downstream wording and negative-corpus coverage, and candidate status; all duplicate, owned elsewhere, and possibly unique notes stay audit-only; summary docs remain unchanged unless docs tests require a summary assertion; no implementation, new proof home, public Story, renderer change, LLM change, public route `200`, production API, or public/user-facing LLM narration opens; docs authority tests pass; and `git diff --check` passes.

## Stage-2: Overload Candidate Meaning Classification

Stage-2 opens only a candidate classification table.

Stage-2 classification statuses:

- duplicate-owned
- internal-ingredient-only
- possibly-unique
- rejected-overclaim

Stage-2 proof authority:

- one chess meaning, one proof home, one Story label, one speech key
- duplicate-owned means the meaning is already Material, Loose, Hanging, QueenHit, RemoveGuard, Defense, CannotSatisfyBoth, or EngineCheck status.
- internal-ingredient-only means the wording describes DefenderDuty, DualDefenderDuty, OverloadTest, or CannotSatisfyBoth mechanics without a new public chess meaning.
- possibly-unique means no existing proof home can own it and same-board legal replay can bind it.
- rejected-overclaim means the wording would strengthen Overload beyond its proof boundary.

Stage-2 candidate classification table:

| candidate | classification | owner or boundary | classification note |
| --- | --- | --- | --- |
| overloads defender | duplicate-owned | `OverloadProof -> Tactic.Overload -> TacticOverload -> overloads_defender` | existing narrow Overload path already owns this wording. |
| `Plan.Overload` | duplicate-owned | existing plan enum only | no runtime public path found. |
| overloaded piece observation | duplicate-owned | root observation / BoardFacts observation layer | observation does not create Story or speech. |
| material gain now | duplicate-owned | `Scene.Material` / `CaptureResult` | existing Material owner remains outside Overload. |
| loose or undefended target | duplicate-owned | `Tactic.Loose` / `LoosePieceProof` | existing Loose owner remains outside Overload. |
| hanging target | duplicate-owned | `Tactic.Hanging` / `CaptureResult` | existing Hanging owner remains outside Overload. |
| queen attacked | duplicate-owned | `Tactic.QueenHit` / `QueenHitProof` | existing QueenHit owner remains outside Overload. |
| defender removed | duplicate-owned | `Tactic.RemoveGuard` / `RemoveGuardProof` | existing RemoveGuard owner remains outside Overload. |
| defense unavailable or refuted | duplicate-owned | `Scene.Defense` / `EngineCheck.Refutes` | existing Defense and EngineCheck owners remain outside Overload. |
| EngineCheck supports caps or refutes | duplicate-owned | `EngineCheck` status | status can only bind to existing Story rows. |
| DefenderDuty guard relation | internal-ingredient-only | `DefenderDuty` | ingredient mechanics do not create a public chess meaning. |
| DualDefenderDuty two-duty relation | internal-ingredient-only | `DualDefenderDuty` | ingredient mechanics do not create a public chess meaning. |
| OverloadTest legal test move | internal-ingredient-only | `OverloadTest` | ingredient mechanics do not create a public chess meaning. |
| CannotSatisfyBoth reply exhaustion | internal-ingredient-only | `CannotSatisfyBoth` | ingredient mechanics do not create a public chess meaning. |
| cannot defend both public Overload output | rejected-overclaim | forbidden public wording | wording is stronger than the existing speech key. |
| therefore wins | rejected-overclaim | forbidden public wording | wording adds a result claim. |
| result of overload | rejected-overclaim | forbidden public wording | wording adds a consequence claim. |
| broad pressure or initiative | rejected-overclaim | forbidden public wording | broad positional wording has no Overload proof path. |
| same-board legal overload pattern not owned elsewhere | possibly-unique | no current owner | only auditable if legal replay can bind it and no existing proof home can own it. |
| Overload result/composition proof home | rejected-overclaim | no current owner | no proof home, Story label, writer, or speech key is created. |

Stage-2 keeps closed:

- no positive runtime admission
- no Story label
- no writer
- no speech key
- no new proof home
- no public route `200`
- no production API
- no public/user-facing LLM narration

Stage-2 forbidden wording:

- no result of overload
- no therefore wins
- no cannot defend both as public Overload output
- no broad pressure or initiative

Completion standard: Stage-2 Overload Candidate Meaning Classification closes when every possible Overload expansion candidate is classified exactly once as duplicate-owned, internal-ingredient-only, possibly-unique, or rejected-overclaim; duplicate-owned consequence meanings remain outside Overload and inside existing Material, Loose, Hanging, QueenHit, RemoveGuard, Defense, CannotSatisfyBoth, or EngineCheck homes; internal ingredients remain internal only; possibly-unique candidates stay audit-only unless no existing proof home can own them and same-board legal replay can bind them; no positive runtime admission, Story label, writer, speech key, proof home, public route `200`, production API, or public/user-facing LLM narration opens; docs authority tests pass; and `git diff --check` passes.

## Stage-3: Overload Duplicate Collision Negative Corpus

Stage-3 opens runtime negative-corpus tests only.

Stage-3 opens:

- collision fixtures where proof-backed Overload sits beside existing neighbor owners
- assertions that each neighbor meaning remains in its existing proof home
- assertions that Overload speaks only through `overloads_defender`
- assertions that Support, Context, Blocked, capped, and refuted Overload rows produce no standalone text

Stage-3 keeps closed:

- no new Overload proof home
- no new Overload Story label
- no new Overload writer
- no new Overload speech key
- no new ExplanationPlan claim
- no renderer text
- no public route `200`
- no production API
- no public/user-facing LLM narration

Stage-3 collision fixture duties:

- Material stays owned by `Scene.Material` / `CaptureResult`.
- Loose stays owned by `Tactic.Loose` / `LoosePieceProof`.
- Hanging stays owned by `Tactic.Hanging` / `CaptureResult`.
- QueenHit stays owned by `Tactic.QueenHit` / `QueenHitProof`.
- RemoveGuard stays owned by `Tactic.RemoveGuard` / `RemoveGuardProof`.
- Defense stays owned by `Scene.Defense` / `DefenseProof`.
- CannotSatisfyBoth remains an internal readiness ingredient only.
- EngineCheck statuses cannot create stronger Overload public text.

Stage-3 forbidden wording:

- material gain
- wins
- loose
- hanging
- queen attacked
- removes defender
- cannot satisfy both
- engine says
- best
- forced
- decisive
- no counterplay

Stage-3 verification:

- `sbt "commentary/testOnly lila.commentary.chess.OverloadExpansionCollisionNegativeCorpusTest"`
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`

Completion standard: Stage-3 Overload Duplicate Collision Negative Corpus closes when collision fixtures prove Material, Loose, Hanging, QueenHit, RemoveGuard, Defense, CannotSatisfyBoth, and EngineCheck meanings remain outside Overload; selected uncapped Lead Overload produces only `overloads_defender`; Support, Context, Blocked, capped, and refuted Overload rows produce no standalone text; no new proof home, Story label, writer, speech key, ExplanationPlan claim, renderer text, public route `200`, production API, or public/user-facing LLM narration opens; required runtime verification passes; and `git diff --check` passes.

## Stage-4: Overload Possibly-Unique Candidate Proof Test

Stage-4 opens docs evidence for at most one possibly-unique Overload candidate.

Stage-4 admits no positive runtime path.

Stage-4 candidate proof checklist:

- scene/tactic/plan
- side
- rival
- target
- anchor
- route
- same-board legal replay
- complete StoryProof
- EngineCheck not Refute

Stage-4 ownership rejection rules:

- candidate meaning must not be expressible as Material, Loose, Hanging, QueenHit, RemoveGuard, Defense, CannotSatisfyBoth, or EngineCheck.
- candidate wording that requires CannotSatisfyBoth wording is internal-ingredient-only.
- candidate wording that requires result wording is rejected-overclaim.
- a same-defender two-duty relation remains internal-ingredient-only unless a distinct public meaning is proven separate from DualDefenderDuty.
- no public-facing candidate name is allowed before uniqueness is proven.

Stage-4 candidate evidence table:

| audit input | status | reason |
| --- | --- | --- |
| Stage-2 possibly-unique row | rejected-overclaim | it has no distinct proof meaning without result or consequence wording. |
| CannotSatisfyBoth wording | internal-ingredient-only | it stays internal and cannot become public Overload output. |
| same defender has two duties | internal-ingredient-only | DualDefenderDuty already owns the internal relation unless a distinct public meaning is proven. |

Stage-4 result:

- no next Overload public meaning admitted

Stage-4 keeps closed:

- no public Story
- no writer
- no speech key
- no renderer text
- no ExplanationPlan claim
- no public route `200`
- no production API
- no public/user-facing LLM narration

Completion standard: Stage-4 Overload Possibly-Unique Candidate Proof Test closes when docs evidence covers at most one possibly-unique candidate, the proof checklist requires scene/tactic/plan, side, rival, target, anchor, route, same-board legal replay, complete StoryProof, and EngineCheck not Refute, all audited inputs are rejected-overclaim or internal-ingredient-only, no next Overload public meaning admitted is recorded, no public Story, writer, speech key, renderer text, ExplanationPlan claim, public route `200`, production API, or public/user-facing LLM narration opens, docs authority tests pass, and `git diff --check` passes.

## Stage-5: Overload Downstream Boundary Audit

Stage-5 opens downstream boundary tests only.

Stage-5 keeps closed:

- ExplanationPlan accepts only selected uncapped Lead Verdict.
- Renderer input remains `ExplanationPlan` only.
- LLM smoke input remains rendered text, claim key, strength, forbidden wording, and rephrase-only instruction only.
- public route `200`
- production API
- public/user-facing LLM narration

Stage-5 proof authority:

- StoryTable orders only.
- Verdict decides.
- ExplanationPlan bounds speech.
- Renderer phrases.
- LLM only polishes.
- Verifier rejects overclaim.

Stage-5 required downstream contract:

- selected uncapped Lead Overload may lower only to `overloads_defender`.
- Support, Context, Blocked, capped, and refuted Overload rows produce no downstream text.
- renderer has no `fromVerdict` or `fromStory` public path.
- LLM prompt contains only `renderedText`, `claimKey`, `strength`, `forbiddenWording`, and `instruction: Rephrase only. Do not add chess facts.`
- LLM prompt does not expose raw BoardFacts, OverloadProof, EngineCheck, raw PV, evals, DefenderDuty, DualDefenderDuty, OverloadTest, or CannotSatisfyBoth.
- duplicate-owned neighbor wording remains rejected by the smoke verifier.

Stage-5 verification:

- `sbt "commentary/testOnly lila.commentary.chess.OverloadExpansionDownstreamBoundaryTest"`
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`

Completion standard: Stage-5 Overload Downstream Boundary Audit closes when selected uncapped Lead Overload lowers only to `overloads_defender`; all non-speaking Overload row states produce no downstream text; renderer and LLM contracts expose no raw proof, board, engine, source-row, or internal ingredient data; duplicate-owned neighbor wording is rejected by the smoke verifier; no public route `200`, production API, public/user-facing LLM narration, renderer text, writer, speech key, Story label, or proof home opens; required runtime verification passes; and `git diff --check` passes.

## Stage-6: Overload Audit Decision Closeout

Stage-6 records exactly one final audit decision:

- C. Boundary leakage found and fixed without opening new meaning.

Stage-6 leak fixed:

- Stage-3 found that Overload LLM smoke accepted loose-target wording stronger than `overloads_defender`.
- The fix added Overload-only rejection for loose-target wording in the internal LLM smoke verifier.
- No renderer text, public route, production API, public/user-facing LLM narration, Story label, writer, speech key, result Story, broad Overload family, or duplicate consequence owner was added.

Stage-6 proof authority remains:

- StoryTable orders only.
- Verdict decides.
- ExplanationPlan bounds speech.
- Renderer phrases.
- LLM only polishes.
- Verifier rejects overclaim.

Stage-6 current Overload authority remains exactly:

`OverloadProof -> Tactic.Overload -> TacticOverload -> overloads_defender`

Stage-6 keeps closed:

- public route `200`
- production API
- public/user-facing LLM narration
- new Overload result Story
- broad Overload family
- Material consequence ownership
- Loose consequence ownership
- Hanging consequence ownership
- QueenHit consequence ownership
- RemoveGuard consequence ownership
- Defense consequence ownership
- CannotSatisfyBoth public wording
- EngineCheck public claim ownership

Stage-6 verification:

- `sbt "commentary/testOnly lila.commentary.chess.OverloadExpansionCollisionNegativeCorpusTest"`
- `sbt "commentary/testOnly lila.commentary.chess.OverloadExpansionDownstreamBoundaryTest"`
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Completion standard: Stage-6 Overload Audit Decision Closeout closes when exactly decision C is recorded, the fixed LLM smoke leak is documented, the current Overload authority remains exactly `OverloadProof -> Tactic.Overload -> TacticOverload -> overloads_defender`, no new public meaning opens, all duplicate consequence owners remain outside Overload, public route `200`, production API, public/user-facing LLM narration, new Overload result Story, broad Overload family, renderer text, writer, and speech key remain closed, required verification passes, and `git diff --check` passes.

## Pawn / Promotion Neighborhood

### PawnAdvance-0 Charter

PawnAdvance-0 opens only the first narrow Pawn / Promotion Neighborhood vertical slice.

First positive scope is not broad PawnTactic or passed-pawn strategy.

Opened positive Story label:

- `Scene.PawnAdvance`

Opened proof home:

- `PawnAdvanceProof`

Core sentence:

Pawn facts observe structure. PawnAdvanceProof binds this legal advance. Scene.PawnAdvance may speak only bounded pawn progress, not conversion.

PawnAdvance-0 positive scope:

- an already-passed non-king pawn.
- legal one-square non-capturing pawn advance.
- non-promotion advance.
- exact after-board still has the same pawn as a passed pawn.
- complete StoryProof and same-board legal replay.
- selected Lead Verdict may lower to bounded pawn-advance wording only.

Allowed bounded claim:

- `advances_passed_pawn`

Allowed deterministic wording:

- `{route} advances the passed pawn.`

PawnAdvance-0 must stay silent for:

- pawn that was not already passed before the move.
- illegal pawn move.
- capturing pawn move.
- promotion move.
- double pawn push.
- missing same-board proof.
- missing StoryProof.
- after-board where the moved pawn is not a passed pawn.

PawnAdvance-0 opens no broad PawnTactic, `Tactic.PawnPush`, `Tactic.PawnFork`,
passed-pawn strategy, promotion threat, unstoppable pawn, winning endgame,
conversion, pawn race, pawn majority plan, king route, opposition, tablebase
claim, best move, only move, forced move, pressure, initiative, public route
`200`, production API, or public/user-facing LLM narration.

Authority split:

- `BoardFacts.PassedPawnObservation` observes current pawn structure only.
- `PawnAdvanceProof` proves the legal non-capturing non-promotion one-step advance and after-board passed-pawn persistence.
- `ScenePawnAdvance` is the only writer that may create `Scene.PawnAdvance`.
- `StoryTable` orders an existing `Scene.PawnAdvance`; it does not create it.
- `ExplanationPlan` may expose only `advances_passed_pawn` from selected Verdict data.
- Renderer phrases only from ExplanationPlan.
- LLM smoke may rephrase only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`

Duplication audit:

- Passed-pawn structure stays in Board Facts.
- Legal advance persistence stays in `PawnAdvanceProof`.
- Public claim identity stays in `Scene.PawnAdvance`.
- Speech key stays `advances_passed_pawn`.
- Promotion, conversion, race, strategy, and tablebase claims remain closed.

Completion standard: PawnAdvance-0 closes as one narrow proof-backed passed-pawn progress slice with no broad pawn tactic, promotion-route, strategy, conversion, public route, production API, or public/user-facing LLM narration opening.

### PawnAdvance-1 PawnAdvanceProof

`PawnAdvanceProof` is the proof home for narrow `Scene.PawnAdvance`.

PawnAdvanceProof must prove:

- advancing side
- pawn identity
- from square
- to square
- legal pawn advance
- move is non-capture
- move is non-promotion
- pawn was passed before move
- pawn remains passed after move
- same-board proof
- exact after-board replay

`PawnAdvanceProof` is not a public `Story`.

`PassedPawnObservation` is not a public `Story`.

PawnAdvanceProof may emit internal missing evidence, but those diagnostics do
not create a Story, rank a Story, repair a Story, or feed speech.
proofFailures must not become renderer input or LLM input.

PawnAdvance-1 forbidden public wording:

- `unstoppable`
- `wins`
- `queens`
- `promotes next`
- `conversion`
- `clear path`
- `cannot be stopped`

PawnAdvanceProof owns only the proof that this exact legal move advanced an
already-passed pawn and that the moved pawn remains passed on the exact
after-board. It does not own promotion threat, queening, conversion,
unstoppable-pawn, strategy, tablebase, race, best-move, only-move, forced, or
winning claims.

Completion standard: PawnAdvance-1 closes when PawnAdvanceProof exposes the
named proof bits, remains diagnostic below public Story identity, treats
PassedPawnObservation as observation only, and keeps proofFailures and stronger
pawn-conversion wording out of renderer and LLM inputs.

### PawnAdvance-2 Scene.PawnAdvance Writer

Named writer: `ScenePawnAdvance`.

ScenePawnAdvance writer conditions:

- complete `StoryProof`
- complete `PawnAdvanceProof`
- same-board legal replay
- legal non-capturing non-promotion pawn advance
- passed-before and passed-after proof
- writer = `ScenePawnAdvance`
- `EngineCheck` does not `Refute`

ScenePawnAdvance Story identity:

- scene = `PawnAdvance`
- tactic = `None`
- plan = `None`
- side = advancing side
- rival = opposite side
- target = destination square
- anchor = pawn origin square
- route = pawn advance

ScenePawnAdvance must not create a PromotionThreat.

ScenePawnAdvance must not create winning, conversion, or material claims.

ScenePawnAdvance must not create pawn-race or unstoppable-pawn claims.

ScenePawnAdvance owns only the proof-backed public identity that this move
advances an already-passed pawn. It does not own queening, promotion threat,
passed-pawn strategy, race, conversion, material gain, best-move, only-move,
forced, tablebase, winning, or unstoppable-pawn meaning.

Completion standard: PawnAdvance-2 closes when `ScenePawnAdvance` is the only
named writer for `Scene.PawnAdvance`, writer output carries only the named
identity fields above, refuted rows cannot lead, and no stronger pawn,
promotion, material, race, conversion, or winning claim can be created by the
writer.

### PawnAdvance-3 Negative Corpus

PawnAdvance-3 keeps pawn-looking moves silent unless the narrow proof home is
complete.

Required silent negative corpus:

- legal move is absent
- same-board proof is absent
- moving piece is not a pawn
- move is a capture
- move is a promotion
- pawn was not passed before the move
- moved pawn is not passed after the move
- en passant complexity enters the proof attempt
- proof attempt expands into immediate promotion threat
- `unstoppable`, `winning`, or `conversion` wording enters

A pawn move is not a passed-pawn Story. Complete PawnAdvanceProof or silence.

En passant is capture complexity for this slice. A pawn move near promotion may
still only be considered through the bounded `advances_passed_pawn` claim when
`PawnAdvanceProof` is complete; it must not become promotion threat, promotes
next, queening, conversion, unstoppable, winning, or race wording.

Completion standard: PawnAdvance-3 closes when every listed false positive is
silent as a Story without complete `PawnAdvanceProof`, and downstream wording
rejects immediate-promotion, unstoppable, winning, and conversion expansions.

### PawnAdvance-4 EngineCheck Reuse

PawnAdvance-4 reuses only the existing `EngineCheck` sidecar.

`EngineCheck` cannot create `Scene.PawnAdvance`.

EngineCheck status meaning for PawnAdvance:

- `Supports` does not create a new claim.
- `Caps` suppresses standalone `advances_passed_pawn` speech or weakens it to relation-only bounded evidence.
- `Refutes` blocks the PawnAdvance Story.
- `Unknown` creates no engine expression.

Forbidden EngineCheck-derived wording:

- `engine says`
- eval numbers
- best move
- only move
- winning endgame
- tablebase-like claim

Completion standard: PawnAdvance-4 closes when `EngineCheck` can attach only to
an existing same-board `ScenePawnAdvance` Story, cannot create PawnAdvance by
itself, preserves bounded passed-pawn advance identity under `Supports`,
suppresses standalone speech under `Caps`, blocks the Story under `Refutes`,
and leaves no engine expression under `Unknown`.

### PawnAdvance-5 StoryTable Integration

PawnAdvance-5 integrates `Scene.PawnAdvance` into `StoryTable` as a lower
bounded scene claim when existing opened claim homes are present.

Collision rows:

- `Tactic.Hanging`
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`
- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`
- `Scene.PawnAdvance`

StoryTable requirements:

- input order must not change selected role shape
- PawnAdvance must not own tactical or material claims
- actual material change now stays in `Scene.Material`
- immediate tactic meaning stays in the tactic row home
- PawnAdvance remains a lower bounded scene claim only
- capped or refuted PawnAdvance has no standalone text

Completion standard: PawnAdvance-5 closes when `StoryTable` remains stable
across input order, existing immediate tactic, material, and defense homes stay
ahead of `Scene.PawnAdvance`, and non-lead, capped, or refuted PawnAdvance rows
cannot produce renderer or LLM standalone speech.

### PawnAdvance-6 ExplanationPlan

PawnAdvance-6 lowers only a selected uncapped `Lead` Verdict for `Scene.PawnAdvance`.

`ExplanationPlan` may admit only the `advances_passed_pawn` claim key.

Allowed claim key:

- `advances_passed_pawn`

Forbidden claim keys:

- `promotion_threat`
- `unstoppable_pawn`
- `wins_endgame`
- `converts_advantage`
- `best_move`
- `only_move`
- `forced`
- `decisive`
- `creates_pressure`
- `takes_initiative`

Support, Context, Blocked, capped, and refuted PawnAdvance rows have no standalone claim.

Completion standard: PawnAdvance-6 closes when only selected uncapped Lead PawnAdvance Verdicts
lower to `advances_passed_pawn`, every forbidden claim key remains unavailable,
and Support, Context, Blocked, capped, or refuted rows stay silent as standalone claims.

### PawnAdvance-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

Allowed deterministic wording:

- `{route} advances the passed pawn.`

Forbidden renderer wording:

- `cannot be stopped`
- `will promote`
- `wins`
- `winning endgame`
- `converts`
- `best move`
- `only move`
- `forces`
- `decisive`
- `creates pressure`

Renderer must not read `Story`, `Verdict`, `PawnAdvanceProof`, `EngineCheck`, `proofFailures`, or `BoardFacts`.

Completion standard: PawnAdvance-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`,
phrases selected PawnAdvance plans only as bounded passed-pawn advance text,
and rejects promotion, unstoppable, winning, conversion, best-move, only-move,
forced, decisive, or pressure wording.

### PawnAdvance-8 LLM Smoke

PawnAdvance-8 reuses only the existing 8B LLM smoke boundary.

Allowed LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Forbidden LLM smoke input or output expansion:

- raw `Story`
- raw `PawnAdvanceProof`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`
- new move
- new line
- promotion claim
- unstoppable claim
- winning claim
- conversion claim

LLM smoke must not receive proof homes, BoardFacts, EngineCheck, raw PV, or diagnostics.
It may only rephrase the rendered line without adding chess facts.

Completion standard: PawnAdvance-8 closes when LLM smoke receives only rendered text contract fields,
rejects raw proof, board, engine, or diagnostic leaks,
and rejects new moves, new lines, promotion, unstoppable, winning, or conversion claims.

### PawnAdvance Closeout Hard Cleanup

PawnAdvance closes as one narrow bounded scene only.

Authority ownership:

- `PassedPawnObservation` observes same-board pawn structure only.
- `PawnAdvanceProof` binds the legal non-capturing non-promotion advance and exact after-board passed-pawn status.
- `Scene.PawnAdvance` is the only Story label for this bounded pawn-progress meaning.
- `advances_passed_pawn` is the only speech key for this meaning.

PawnAdvance owns no PromotionThreat, Promotion, PawnStop, PawnBreak, Material, Hanging,
Defense, DiscoveredAttack, Pin, RemoveGuard, Skewer, broad Line/Defender tactic,
passed-pawn strategy, unstoppable-pawn, conversion, clear-path, pawn-race, best-move,
only-move, forced, decisive, pressure, initiative, or winning-endgame meaning.

Duplication audit:

- one chess meaning: bounded passed-pawn advance only
- one proof home: `PawnAdvanceProof`
- one Story label: `Scene.PawnAdvance`
- one speech key: `advances_passed_pawn`
- one detailed live authority document: `StoryInteractionLaw.md`

`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PawnAdvance only.
They must not carry detailed PawnAdvance stage law, negative corpus, renderer law,
LLM law, or closeout completion authority.

Renderer and LLM wording must remain no stronger than `advances_passed_pawn`.
They may say only bounded pawn progress and must reject promotion, unstoppable,
winning, conversion, clear-path, strategy, pressure, initiative, best-move,
only-move, forced, decisive, raw proof, raw BoardFacts, EngineCheck, raw PV,
and proofFailures wording.

Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PawnAdvance Closeout closes when `PassedPawnObservation`,
`PawnAdvanceProof`, `Scene.PawnAdvance`, and `advances_passed_pawn` stay in separate
authority layers; PawnAdvance owns no promotion, stopping, breaking, material,
tactical, strategic, conversion, or public-surface meaning; detailed authority remains
only in `StoryInteractionLaw.md`; renderer and LLM smoke remain bounded to
`advances_passed_pawn`.

### PawnStop-0 Charter

PawnStop-0 opens only the second narrow Pawn / Promotion Neighborhood vertical slice.

First positive scope is not broad pawn defense or endgame hold.

Opened positive Story label:

- `Scene.PawnStop`

Opened proof home:

- `PawnStopProof`

Core sentence:

Pawn facts observe structure. PawnStopProof proves the next square is stopped. Scene.PawnStop may speak only bounded immediate stop, not endgame defense.

PawnStop-0 positive scope:

- an already-passed target pawn.
- the target pawn's next advance square.
- a legal move directly stops that next advance square on the exact after-board.
- the target pawn remains present on the exact after-board.
- the next advance square is non-promotion.
- complete StoryProof and same-board legal replay.
- selected Lead Verdict may lower to bounded pawn-stop wording only.

Allowed bounded claim:

- `stops_pawn_advance`

Allowed deterministic wording:

- `{route} stops the passed pawn from advancing next.`

PawnStop-0 must stay silent for:

- pawn that was not already passed before the move.
- move that does not stop the target pawn's next advance square.
- promotion stop.
- permanent stop.
- tablebase draw.
- best defense or only move.
- winning or losing endgame.
- conversion stopped.
- pawn race.
- king route or opposition.
- broad pawn strategy.
- missing same-board proof.
- missing StoryProof.

PawnStop-0 opens no promotion stop, permanent stop, tablebase draw, best
defense, only move, winning/losing endgame, conversion stopped, pawn race, king
route, opposition, broad pawn strategy, public route `200`, production API, or
public/user-facing LLM narration.

Authority split:

- `BoardFacts.PassedPawnObservation` observes current pawn structure only.
- `PawnStopProof` proves the legal move directly stops the already-passed pawn's next advance square on the exact after-board.
- `ScenePawnStop` is the only writer that may create `Scene.PawnStop`.
- `StoryTable` orders an existing `Scene.PawnStop`; it does not create it.
- `ExplanationPlan` may expose only `stops_pawn_advance` from selected Verdict data.
- Renderer phrases only from ExplanationPlan.
- LLM smoke may rephrase only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`

Duplication audit:

- Passed-pawn structure stays in Board Facts.
- Next-square stop proof stays in `PawnStopProof`.
- Public claim identity stays in `Scene.PawnStop`.
- Speech key stays `stops_pawn_advance`.
- Promotion stop, permanent stop, endgame defense, race, strategy, king-route, opposition, and tablebase claims remain closed.

Completion standard: PawnStop-0 closes as one narrow proof-backed passed-pawn
next-square stop slice with no broad pawn defense, promotion-stop, permanent-stop,
endgame-hold, tablebase, strategy, conversion, public route, production API, or
public/user-facing LLM narration opening.

### PawnStop-1 PawnStopProof

`PawnStopProof` is the proof home for narrow `Scene.PawnStop`.

PawnStopProof must prove:

- stopping side
- passed pawn side
- passed pawn identity
- pawn current square
- pawn next advance square
- legal stopping move
- exact after-board replay
- pawn was passed before move
- stopping move directly occupies, attacks, or controls the next advance square
- same-board proof

First allowed stop kinds:

- `NextSquareOccupied`
- `NextSquareAttacked`
- `NextSquareControlledByPawn`

Closed stop kinds:

- long-term blockade
- king opposition
- tablebase draw
- promotion race stop
- tactic sequence stop
- `cannot ever advance` claim

`PawnStopProof` is not a public `Story`.

`PassedPawnObservation` is not a public `Story`.

PawnStopProof may emit internal missing evidence, but those diagnostics do
not create a Story, rank a Story, repair a Story, or feed speech.
proofFailures must not become renderer input or LLM input.

PawnStopProof owns only the proof that this exact legal move immediately stops
an already-passed pawn's next advance square by one of the admitted stop kinds
on the exact after-board. It does not own promotion stop, permanent blockade,
king opposition, tablebase draw, pawn-race, tactic-sequence, cannot-ever-advance,
best-defense, only-move, forced, winning, losing, conversion, or endgame-hold
claims.

Completion standard: PawnStop-1 closes when PawnStopProof exposes the named
proof bits, admits only `NextSquareOccupied`, `NextSquareAttacked`, and
`NextSquareControlledByPawn`, remains diagnostic below public Story identity,
treats `PassedPawnObservation` as observation only, and keeps proofFailures and
stronger pawn-defense wording out of renderer and LLM inputs.

### PawnStop-2 Scene.PawnStop Writer

`ScenePawnStop` is the named writer for `Scene.PawnStop`.

Writer conditions:

- complete StoryProof
- complete PawnStopProof
- same-board legal replay
- legal stopping move
- passed pawn exists
- next square stop relation complete
- writer = `ScenePawnStop`
- EngineCheck does not `Refute`

Story identity:

- `scene = PawnStop`
- `tactic = None`
- `plan = None`
- `side = stopping side`
- `rival = passed pawn side`
- `target = pawn next advance square`
- `anchor = stopping move origin square`
- `route = stopping move`

ScenePawnStop must not own `Scene.Defense` meaning.

ScenePawnStop must not say it stopped `PromotionThreat`.

ScenePawnStop must not create endgame-result or tablebase claims.

Completion standard: PawnStop-2 closes when only `ScenePawnStop` can write the
bounded `Scene.PawnStop` identity above from complete StoryProof and
PawnStopProof, EngineCheck refutation prevents writer-backed public speech, and
defense, promotion-threat, endgame-result, and tablebase meanings remain closed.

### PawnStop-3 Negative Corpus

PawnStop-3 keeps pawn-stop-looking moves silent unless the narrow proof home is
complete.

Required silent negative corpus:

- legal move is absent
- same-board proof is absent
- target pawn is not a passed pawn
- target pawn next advance square cannot be calculated
- after the stopping move, the next square remains empty and safely advanceable
- stop expands into promotion-threat stop
- king opposition, tablebase, or draw claim enters
- `permanently stopped`, `cannot advance`, or `only move` wording enters

Stopping the next square is not stopping the pawn forever.

Complete PawnStopProof or silence.

PawnStop-3 opens no promotion-threat stop, permanent stop, cannot-advance
claim, only-move claim, king opposition, tablebase draw, endgame result,
best-defense, pawn-race, conversion, public route `200`, production API, or
public/user-facing LLM narration.

Completion standard: PawnStop-3 closes when every listed false positive is
silent at the writer/proof boundary or rejected by downstream forbidden wording,
and no incomplete PawnStopProof can create `Scene.PawnStop` speech.

### PawnStop-4 EngineCheck Reuse

PawnStop-4 reuses only the existing `EngineCheck` sidecar.

`EngineCheck` cannot create `Scene.PawnStop`.

EngineCheck status handling:

- `Supports` does not create a new claim.
- `Caps` suppresses standalone `stops_pawn_advance` speech or weakens it to bounded relation-only evidence.
- `Refutes` blocks the PawnStop Story.
- `Unknown` creates no engine expression.

PawnStop-4 forbidden engine wording:

- `engine says`
- eval numbers
- best defense
- only move
- tablebase draw
- winning or losing endgame

Completion standard: PawnStop-4 closes when `EngineCheck` can attach only to
an existing same-board `ScenePawnStop` Story route, cannot create PawnStop,
cannot add a new claim under Supports, suppresses or bounds capped rows, blocks
refuted rows, keeps Unknown engine-silent, and exposes no engine wording, eval
numbers, best-defense, only-move, tablebase, or endgame-result claims.

### PawnStop-5 StoryTable Integration

PawnStop-5 integrates `Scene.PawnStop` into `StoryTable` as a lower
bounded scene claim when existing opened claim homes are present.

Collision rows:

- `Tactic.Hanging`
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`
- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`
- `Scene.PawnAdvance`
- `Scene.PawnStop`

StoryTable requirements:

- input order must not change selected role shape
- PawnStop must not own `Scene.Defense` claims
- PawnStop must not create `PromotionThreat` claims
- PawnAdvance and PawnStop over the same pawn must not both compete as Lead
- immediate tactic meaning stays in the tactic row home
- actual material change stays in `Scene.Material`
- PawnStop remains a lower bounded scene claim only
- capped or refuted PawnStop has no standalone text

Completion standard: PawnStop-5 closes when `StoryTable` remains stable across input order, existing immediate tactic, material, defense, and same-pawn PawnAdvance homes stay ahead of `Scene.PawnStop`, and non-lead, capped, or refuted PawnStop rows cannot produce renderer or LLM standalone speech.

### PawnStop-6 ExplanationPlan

PawnStop-6 lowers only a selected uncapped `Lead` Verdict for `Scene.PawnStop`.

`ExplanationPlan` may admit only the `stops_pawn_advance` claim key.

Allowed claim key:

- `stops_pawn_advance`

Forbidden claim keys:

- `stops_promotion`
- `permanently_stops_pawn`
- `draws_endgame`
- `best_defense`
- `only_move`
- `tablebase_draw`
- `wins_endgame`
- `converts_advantage`
- `forced`

Support, Context, Blocked, capped, and refuted PawnStop rows have no standalone claim.

Completion standard: PawnStop-6 closes when only selected uncapped Lead PawnStop Verdicts
lower to `stops_pawn_advance`, every forbidden claim key remains unavailable,
and Support, Context, Blocked, capped, or refuted rows stay silent as standalone claims.

### PawnStop-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

Allowed deterministic wording:

- `{route} stops the passed pawn from advancing next.`

Forbidden renderer wording:

- `stops promotion`
- `stops the pawn for good`
- `draws`
- `holds the endgame`
- `best defense`
- `only move`
- `forces`
- `wins`
- `tablebase`

Renderer must not read `Story`, `Verdict`, `PawnStopProof`, `EngineCheck`, `proofFailures`, or `BoardFacts`.

Completion standard: PawnStop-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`,
phrases selected PawnStop plans only as bounded next-advance stop text,
rejects every forbidden renderer wording above, and opens no raw Story,
raw Verdict, PawnStopProof, EngineCheck, proofFailures, BoardFacts, public route
`200`, production API, or public/user-facing LLM narration.

### PawnStop-8 LLM Smoke

PawnStop-8 reuses only the existing 8B LLM smoke boundary.

PawnStop-8 LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

PawnStop-8 forbidden LLM smoke inputs:

- raw `Story`
- raw `PawnStopProof`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`

PawnStop-8 LLM smoke must reject output that adds:

- new move
- new line
- promotion claim
- permanent stop claim
- draw claim
- tablebase claim
- winning claim

Completion standard: PawnStop-8 closes when LLM smoke receives only rendered text contract fields,
returns only bounded rephrasing of selected uncapped PawnStop rendered text,
rejects raw Story, raw PawnStopProof, BoardFacts, EngineCheck, raw PV, proofFailures,
new move, new line, promotion, permanent stop, draw, tablebase, and winning claims,
and opens no new LLM channel, production API, public route `200`, or chess-fact creation path.

### PawnStop Closeout Hard Cleanup

PawnStop Closeout opens no new chess meaning. It only audits the PawnStop hard cleanup surface.

PawnStop Closeout must confirm:

- `PassedPawnObservation` observes same-board passed-pawn structure only.
- `PawnStopProof` proves the legal move directly stops the already-passed pawn's next non-promotion advance square on the exact after-board.
- `Scene.PawnStop` is the only Story label for this bounded immediate next-square stop meaning.
- `stops_pawn_advance` is the only speech key for this meaning.

PawnStop owns no `Scene.PawnAdvance`, PromotionThreat, Promotion, or PawnBreak meaning.
PawnStop owns no `Scene.Defense`, `Scene.Material`, `Tactic.Hanging`, or Line / Defender tactic meaning.
`permanent stop`, `draw`, `tablebase`, `best defense`, and `only move` remain forbidden wording only, not live authority.

PawnStop Closeout duplicate checks:

- one chess meaning: bounded immediate passed-pawn next-square stop only
- one proof home: `PawnStopProof`
- one Story label: `Scene.PawnStop`
- one speech key: `stops_pawn_advance`
- one detailed live authority document: `StoryInteractionLaw.md`

`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PawnStop only.
Renderer and LLM wording must remain no stronger than `stops_pawn_advance`.
Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PawnStop Closeout closes when `PassedPawnObservation`,
`PawnStopProof`, `Scene.PawnStop`, and `stops_pawn_advance` remain separate authority homes,
PawnStop owns no PawnAdvance, PromotionThreat, Promotion, PawnBreak, Defense, Material,
Hanging, or Line / Defender tactic meaning, closed endgame and defense wording remains forbidden only,
renderer and LLM smoke stay bounded to `stops_pawn_advance`, detailed authority stays only in
`StoryInteractionLaw.md`, and public route `200`, production API, and public/user-facing LLM
narration remain closed.

### PIH-0 Pawn Interaction Hardening Charter

Pawn Interaction Hardening opens no new pawn meaning. It proves PawnAdvance
and PawnStop do not steal promotion, conversion, defense, or tactic meaning.

PIH-0 opens only:

- existing `Scene.PawnAdvance` and `Scene.PawnStop` interaction hardening
- complex same-board fixtures over already-opened rows
- `StoryTable` role stability
- downstream no-overclaim smoke
- minimum fixes for discovered ordering or boundary bugs

PIH-0 collision rows are only:

- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Tactic.Hanging`
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`
- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`

PIH-0 opens no `Scene.PromotionThreat`, `Scene.Promotion`, `Scene.PawnBreak`,
broad PawnTactic, unstoppable pawn, conversion, winning endgame, tablebase,
pawn race, king route, opposition, best move, only move, public route `200`,
production API, or public/user-facing LLM narration.

Completion standard: PIH-0 closes when existing PawnAdvance and PawnStop rows
remain lower bounded scene claims under opened tactic, material, and defense
homes; same-board role shape is input-order stable; PawnAdvance and PawnStop
proof sidecars cannot contaminate each other; renderer and LLM smoke reject
promotion, conversion, defense, tactic, engine, best-move, only-move, and
public-surface overclaims; and no new pawn Story label, proof home, or speech
key appears.

### PIH-1 Fixture Map

PIH-1 opens only the fixture map for complex same-board Pawn Interaction
Hardening over already-open rows. It does not open PromotionThreat, Promotion,
PawnBreak, broad PawnTactic, unstoppable pawn, conversion, winning endgame,
tablebase, pawn race, king route, opposition, best move, only move, public
route `200`, production API, or public/user-facing LLM narration.

Every PIH-1 fixture must state:

- same-board FEN
- side to move
- legal lines under test
- expected open rows
- expected blocked rows
- expected Lead, Support, Context, or Blocked role for each row
- expected selected Verdict
- forbidden claims

The fixture map is:

| fixture | same-board FEN | side to move | legal lines under test | expected open rows | expected blocked rows | expected roles | expected selected Verdict | forbidden claims |
|---|---|---|---|---|---|---|---|---|
| PawnAdvance vs PawnStop | `4k3/8/8/4P3/8/8/8/4K3 w - - 0 1` | White | `e5e6` | `Scene.PawnAdvance` | `Scene.PawnStop/blocked` | `Scene.PawnAdvance` Lead; `Scene.PawnStop/blocked` Blocked | `Scene.PawnAdvance` | promotion, promotion threat, pawn break, permanent stop, best move, only move |
| PawnAdvance vs Material | `4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1` | White | `d4d5`, `d4e5` | `Scene.PawnAdvance`, `Scene.Material` | none | `Scene.Material` Lead; `Scene.PawnAdvance` Support | `Scene.Material` | promotion, tactic, fork, best move, only move |
| PawnAdvance vs Defense | `4k3/8/8/5n1P/3Q4/8/8/4K3 w - - 0 1` | White | `h5h6`, `d4e4` | `Scene.PawnAdvance`, `Scene.Defense` | none | `Scene.Defense` Lead; `Scene.PawnAdvance` Support | `Scene.Defense` | promotion, tactic, fork, discovered attack, best move, only move |
| PawnStop vs Defense | `4k3/6n1/8/3qP3/5N2/8/8/4K3 b - - 0 1` | Black | `g7e6`, `d5c6` | `Scene.PawnStop`, `Scene.Defense` | none | `Scene.Defense` Lead; `Scene.PawnStop` Support | `Scene.Defense` | promotion stop, permanent stop, draw, tablebase, best defense, only move |
| PawnStop vs Line/Defender tactic | `r5k1/5n2/8/7P/8/8/4N3/4K3 b - - 0 1` | Black | `f7h6`, `a8e8` | `Scene.PawnStop`, `Tactic.Pin` | none | `Tactic.Pin` Lead; `Scene.PawnStop` Support | `Tactic.Pin` | promotion stop, permanent stop, skewer, discovered attack, best move, only move |
| Pawn row vs EngineCheck Supports/Caps/Refutes | `4k3/8/8/4P3/8/8/8/4K3 w - - 0 1` | White | `e5e6` | `Scene.PawnAdvance#Supports`, `Scene.PawnAdvance#Caps`, `Scene.PawnAdvance#Refutes` | `Scene.PawnAdvance#Refutes` | `Scene.PawnAdvance#Supports` Lead; `Scene.PawnAdvance#Caps` Support; `Scene.PawnAdvance#Refutes` Blocked | `Scene.PawnAdvance#Supports` | engine says, best move, only move, winning, conversion |
| Promotion-looking but no PromotionThreat yet | `4k3/8/4P3/8/8/8/8/4K3 w - - 0 1` | White | `e6e7` | `Scene.PawnAdvance` | `promotion-looking/blocked` | `Scene.PawnAdvance` Lead; `promotion-looking/blocked` Blocked | `Scene.PawnAdvance` | promotion, promotion threat, queens, unstoppable, winning, conversion |
| tablebase-looking but no tablebase authority | `4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1` | Black | `g7e6` | `Scene.PawnStop` | `endgame-result/blocked` | `Scene.PawnStop` Lead; `endgame-result/blocked` Blocked | `Scene.PawnStop` | tablebase, draw, permanent stop, best move, only move |

PIH-1 negative rows are fixture probes only. They do not create source-row
authority, proof homes, Story labels, speech keys, public JSON, renderer input,
LLM input, or public expected output. `PromotionThreat` must not appear as a
positive expected row. Endgame-result and tablebase-looking probes must remain
blocked and must not become public claims. `proofFailures` text remains internal
diagnostic material only.

### PIH-2 Role Stability

PIH-2 opens only `StoryTable` role stability checks over already-open
`Scene.PawnAdvance`, `Scene.PawnStop`, Defense, Material, Hanging, Fork,
DiscoveredAttack, Pin, RemoveGuard, and Skewer rows. It opens no new pawn
meaning, no PromotionThreat, no Promotion, no PawnBreak, no conversion, no
tablebase, no endgame-result authority, no public route `200`, no production
API, and no public/user-facing LLM narration.

PIH-2 requires:

- selected Verdict identity remains stable when input order changes
- the same pawn meaning cannot become duplicate Lead
- incomplete PawnAdvance and PawnStop rows cannot Lead
- EngineCheck Refutes rows become Blocked
- EngineCheck Caps rows create no standalone claim, ExplanationPlan, renderer
  output, or LLM claim
- PawnAdvance and PawnStop over the same passed pawn cannot both Lead
- PawnStop cannot own Defense claim meaning
- PawnAdvance cannot own promotion or conversion claim meaning

PIH-2 may fix only ordering or downstream-boundary bugs exposed by those
checks. Such fixes must not use `proofFailures` text as public ordering, must
not make raw engine status public meaning, and must not add a new Story label,
proof home, speech key, renderer route, production API, or public surface.

### PIH-3 Meaning Ownership Boundary

PIH-3 opens only Meaning Ownership Boundary checks over already-open
PawnAdvance, PawnStop, Material, Defense, and Line/Defender tactic rows. It
does not open PromotionThreat, Promotion, PawnBreak, broad PawnTactic,
conversion, unstoppable pawn, tablebase, endgame-result, public route `200`,
production API, or public/user-facing LLM narration.

PIH-3 fixes each row to one meaning:

- `Scene.PawnAdvance` owns only an already-passed pawn making a legal
  non-capturing non-promotion advance and remaining passed.
- `Scene.PawnStop` owns only a legal move directly stopping the passed pawn's
  next advance square.
- `Scene.Material` owns only an actual material balance change now.
- `Scene.Defense` owns only complete `ThreatProof` plus `DefenseProof`
  preventing immediate material loss.
- Line/Defender tactics own only their own line or guard relation:
  DiscoveredAttack reveals an attack, Pin pins, RemoveGuard removes a defender,
  and Skewer skewers one front and one rear target.

PIH-3 forbidden ownership:

- PawnAdvance must not speak will-promote, unstoppable-pawn, conversion,
  winning-endgame, best-move, or only-move meaning.
- PawnStop must not speak promotion-stop, draw, endgame-hold, permanent-stop,
  best-defense, best-move, or only-move meaning.
- PawnStop must not replace general Defense or own `defends_piece` meaning.
- Material and Hanging must not own pawn progress, pawn stop, promotion, or
  conversion meaning.
- Line/Defender tactics must not steal sibling line or guard relation meaning
  and must not own pawn progress or material-win meaning.

PIH-3 downstream smoke is allowed only as a boundary check over selected Lead
Verdicts from existing rows. Support, Context, Blocked, capped, and refuted
rows create no standalone claim, ExplanationPlan, renderer text, LLM input, or
public expected output.

### PIH-4 EngineCheck Interaction

PIH-4 opens only EngineCheck interaction checks over already-open
`Scene.PawnAdvance` and `Scene.PawnStop` rows. It reuses the existing
EngineCheck sidecar and status semantics only. EngineCheck remains incapable of
creating a Story, choosing a Verdict, owning a public claim, feeding renderer or
LLM stages directly, or replacing pawn proof.

PIH-4 requires:

- `Supports` creates no new pawn, engine, promotion, conversion, defense, or
  tactic claim. It may leave an already-selected uncapped pawn Story with the
  same bounded pawn claim.
- `Caps` suppresses standalone pawn expression or weakens it through the
  existing bounded engine-limited path. It must not create renderer text,
  LLM input, or an engine-owned claim.
- `Refutes` makes the corresponding PawnAdvance or PawnStop Story `Blocked`.
  The blocked row must not create an ExplanationPlan, renderer text, LLM input,
  or public expected output.
- `Unknown` creates no engine-related expression. If the underlying pawn row is
  otherwise selected, its text must be identical to the non-engine bounded pawn
  text and must not mention engine evidence.

PIH-4 forbids public or downstream wording that says or implies `engine says`,
raw PV explanation, public eval numbers, best move, only move, tablebase-like
authority, promotion, conversion, unstoppable pawn, draw, or winning endgame.
Raw engine evals, engine lines, source rows, EngineCheck diagnostics, and
`proofFailures` remain internal diagnostics and must not enter `Verdict.values`,
ExplanationPlan, renderer text, LLM prompt input, or public JSON.

PIH-4 does not open a new EngineCheck proof home, Story label, speech key,
public route `200`, production API, public/user-facing LLM narration, or any
new pawn meaning.

### PIH-5 Negative Corpus

PIH-5 opens only the negative corpus for close PawnAdvance and PawnStop false
positives. It proves that looking like pawn progress, a pawn stop, a promotion
race, an endgame tablebase case, or a king-route idea is not enough. Existing
complete proof or silence is the rule.

PIH-5 must keep these rows silent:

- a pawn advances but was not already passed
- a passed pawn advances but lacks exact after-board replay proof
- a pawn-stop-looking move leaves the passed pawn's next advance square still
  available
- a pawn-stop-looking move only claims long-term blockade instead of directly
  stopping the next advance square
- a promotion-looking position before PromotionThreat opens
- a tablebase-looking position
- a king opposition-looking position
- a pawn race-looking position
- a route mismatch between Story identity, proof sidecar, and EngineCheck line
- stale or wrong-board engine evidence
- EngineCheck `Refutes` over a plausible pawn row

PIH-5 negative rows may use proofFailures and diagnostic fields internally, but
they must not become public expected output, `Verdict.values`, ExplanationPlan,
renderer text, LLM prompt input, public JSON, or source-row authority.

PIH-5 does not open PromotionThreat, Promotion, PawnBreak, broad PawnTactic,
long-term blockade, unstoppable pawn, conversion, winning or drawn endgame,
tablebase, pawn race, king route, opposition, best move, only move, public route
`200`, production API, public/user-facing LLM narration, a new proof home, a new
Story label, or a new speech key.

### PIH-6 Downstream Boundary Smoke

PIH-6 opens only downstream boundary smoke over already-open PawnAdvance and
PawnStop rows. It proves that selected Lead Verdict data is the only pawn data
allowed to reach expression stages. Support, Context, Blocked, capped, and
refuted pawn rows remain relation or diagnostic rows and create no standalone
text.

PIH-6 requires:

- ExplanationPlan receives selected Lead Verdict data only for pawn expression.
- DeterministicRenderer receives ExplanationPlan only.
- LLM smoke receives only rendered text, claim key, strength, forbidden wording,
  and the rephrase-only instruction: `Rephrase only. Do not add chess facts.`
- Support, Context, Blocked, capped, and refuted pawn rows produce no standalone
  ExplanationPlan, renderer text, LLM input, prompt, public JSON, or public
  expected output.
- Prompt smoke must not expose BoardFacts, Story rows, Verdict internals,
  EngineCheck, EngineEval, EngineLine, proofFailures, source rows, FEN, raw PV,
  route internals, proof sidecars, or diagnostics.

PIH-6 forbidden downstream wording includes: will promote, unstoppable, wins,
winning, conversion, draws, holds, tablebase, best move, only move, forced,
pressure, and initiative. These are forbidden for both PawnAdvance and PawnStop
selected Lead expression, even when the raw row looks promotion-like,
endgame-like, engine-supported, or strategically suggestive.

PIH-6 does not open PromotionThreat, Promotion, PawnBreak, broad PawnTactic,
unstoppable pawn, conversion, winning or drawn endgame, tablebase, pawn race,
king route, opposition, pressure, initiative, best move, only move, public route
`200`, production API, public/user-facing LLM narration, new renderer routes,
new LLM authority, a new proof home, a new Story label, or a new speech key.

### PIH-7 Diagnostics Boundary

PIH-7 opens only diagnostics boundary hardening over already-open PawnAdvance
and PawnStop rows.

PIH-7 requires:

- proofFailures remain internal diagnostics only.
- raw proof text does not enter `Verdict.values`.
- EngineCheck text, raw PV, eval numbers, and engine sidecar text do not lower
  directly into ExplanationPlan, renderer text, or LLM smoke output.
- StoryTable debug relations, including capped and refuted relation keys, do not
  become renderer wording.
- test helper names, fixture-map language, negative-corpus language, and PIH
  stage labels do not become runtime authority names or production source
  concepts.

PIH-7 diagnostic wording forbidden in public expression includes:

- proofFailures.
- missing evidence.
- same-board proof.
- exact after-board replay.
- EngineCheck, EngineLine, and EngineEval.
- StoryTable debug relation.
- blocked_by_engine_refute.
- capped_same_story.
- same_family_lower_rank.
- raw eval numbers or PV text.

PIH-7 may use diagnostic text inside tests and debugging only. It does not open
a public diagnostic explanation, public JSON diagnostic payload, public route
`200`, production API, public/user-facing LLM narration, a new proof home, a new
Story label, or a new speech key.

### PIH Closeout Hard Cleanup

PIH Closeout opened no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration at the PawnAdvance/PawnStop baseline. PromotionThreat-0 is the third Pawn / Promotion Neighborhood slice.

PIH Closeout must confirm:

- no new Story family was added beyond already-open `Scene.PawnAdvance` and `Scene.PawnStop`.
- no new proof home was added beyond already-open `PawnAdvanceProof` and `PawnStopProof`.
- `PassedPawnObservation`, `PawnAdvanceProof`, and `PawnStopProof` authority remains separated.
- `Scene.PawnAdvance` and `Scene.PawnStop` do not invade each other's meaning.
- PromotionThreat, Promotion, PawnBreak, tablebase, pawn race, king route, and opposition remain closed authority and forbidden wording only.
- Material, Defense, Hanging, and Line / Defender tactic homes are not invaded.

PIH Closeout duplicate audit:

- one chess meaning: bounded passed-pawn advance belongs to `Scene.PawnAdvance`; bounded immediate next-square stop belongs to `Scene.PawnStop`.
- one proof home for each pawn meaning: `PawnAdvanceProof` for advance; `PawnStopProof` for stop.
- one Story label for each pawn meaning: `Scene.PawnAdvance` and `Scene.PawnStop`.
- one speech key for each pawn meaning: `advances_passed_pawn` and `stops_pawn_advance`.
- one detailed live authority document: `StoryInteractionLaw.md`.

`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PIH Closeout only.

Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PIH Closeout closes when PawnAdvance and PawnStop interaction hardening has no new pawn meaning, no mixed proof authority, no sibling claim-home invasion, no duplicated detailed authority outside StoryInteractionLaw.md, no promoted test helper, no public route `200`, no production API, and no public/user-facing LLM narration.

### PromotionThreat-0 Charter

PromotionThreat-0 opens only the third narrow Pawn / Promotion Neighborhood vertical slice.

First positive scope is not broad promotion race, winning pawn, conversion, or
actual Promotion Story.

Core sentence:

Pawn facts observe structure. PromotionThreatProof proves the next-move
promotion threat. Scene.PromotionThreat may speak only immediate threat, not
unstoppable conversion.

PromotionThreat-0 opens:

- narrow `Scene.PromotionThreat`
- legal pawn move
- exact after-board
- next move promotion is legal for the same pawn on that after-board
- promotion square
- promotion route
- selected Verdict after bounded promotion-threat wording
- the bounded speech key `threatens_promotion_next`

PromotionThreat-0 does not open:

- actual Promotion Story
- unstoppable pawn
- cannot be stopped
- winning endgame
- conversion
- tablebase claim
- pawn race result
- best move or only move
- forced win
- no counterplay
- public route `200`
- production API
- public/user-facing LLM narration

`PromotionThreatProof` is the proof home for this slice. It is not a public
`Story` and does not own conversion, endgame result, tablebase, race, or
unstoppable-pawn meaning.

### PromotionThreat-1 PromotionThreatProof

`PromotionThreatProof` is the proof home for the narrow
`Scene.PromotionThreat` slice. It proves immediate next-move promotion-threat
evidence only; it is not a public `Story`.

PromotionThreatProof must prove:

- threatening side
- rival side
- pawn identity
- pawn move that creates the threat
- exact after-board replay
- next promotion move
- promotion square
- promotion route
- next promotion move is legal on the after-board
- pawn is non-promoted before the creating move
- same-board proof

`PromotionThreatProof` is not a public `Story`.
`PawnAdvanceProof` does not own `PromotionThreat`.
`PawnStopProof` does not own `PromotionThreat`.

PromotionThreat-1 forbids:

- `unstoppable`
- `cannot be stopped`
- `will queen`
- `wins`
- `conversion`
- `forced`
- `tablebase`

proofFailures must not become renderer input or LLM input.

### PromotionThreat-2 ScenePromotionThreat Writer

`ScenePromotionThreat` is the named writer for narrow `Scene.PromotionThreat`.

ScenePromotionThreat writer conditions:

- complete `StoryProof`
- complete `PromotionThreatProof`
- same-board legal replay
- legal creating pawn move
- legal next promotion move on exact after-board
- writer = `ScenePromotionThreat`
- `EngineCheck` does not `Refute`

ScenePromotionThreat Story identity:

- `scene = PromotionThreat`
- `tactic = None`
- `plan = None`
- `side = threatening side`
- `rival = rival side`
- `target = promotion square`
- `anchor = pawn origin square`
- `route = creating pawn move`
- `secondaryTarget = None` unless the fixed identity shape requires the promotion destination

`ScenePromotionThreat` must not create actual Promotion.
`ScenePromotionThreat` must not replace `PawnAdvance` or `PawnStop` meaning.
`ScenePromotionThreat` must not create `unstoppable` or `wins` meaning.

### PromotionThreat-3 Negative Corpus

A pawn near promotion is not a PromotionThreat. Legal next-move promotion proof or silence.

PromotionThreat-3 keeps these rows closed:

- legal creating move missing
- same-board proof missing
- move is not a pawn move
- creating move itself is promotion
- next promotion move is not legal on the exact after-board
- promotion square cannot be computed
- next move is not promotion because two or more moves are still needed
- rival stoppability check tries to expand into `unstoppable`
- tablebase, winning, or conversion wording enters the path

`ScenePromotionThreat` must return no Story for every negative corpus row.
`PromotionThreatProof` may record internal missing evidence only.
Renderer and LLM smoke must reject `unstoppable`, `wins`, `tablebase`, and `conversion` wording.

### PromotionThreat-4 EngineCheck Reuse

PromotionThreat-4 reuses only the existing `EngineCheck` sidecar.

`EngineCheck` cannot create `Scene.PromotionThreat`.

EngineCheck status handling for PromotionThreat:

- `Supports` does not create a new claim.
- `Caps` suppresses standalone `threatens_promotion_next` speech or weakens it to bounded relation-only evidence.
- `Refutes` blocks the PromotionThreat Story.
- `Unknown` creates no engine expression.

PromotionThreat-4 forbidden engine wording:

- `engine says`
- eval numbers
- tablebase result
- best move
- only move
- winning endgame
- forced win

Completion standard: PromotionThreat-4 closes when `EngineCheck` can attach only to an existing same-board `ScenePromotionThreat` Story route, cannot create PromotionThreat, cannot add a new claim under Supports, suppresses or bounds capped rows, blocks refuted rows, keeps Unknown engine-silent, and exposes no engine wording, eval numbers, tablebase, best-move, only-move, winning-endgame, or forced-win claims.

### PromotionThreat-5 StoryTable Integration

PromotionThreat-5 integrates `Scene.PromotionThreat` into `StoryTable` collision behavior only.

PromotionThreat-5 collision rows:

- `Scene.PawnAdvance`
- `Scene.PawnStop`
- existing Material / Defense / Hanging / Line rows
- `Scene.PromotionThreat`

PromotionThreat-5 verification:

- input order remains stable
- PromotionThreat does not own PawnAdvance meaning
- PromotionThreat does not own PawnStop meaning
- PawnStop must not create a `stops promotion` claim
- actual material or tactical claim remains in the existing home
- capped or refuted PromotionThreat creates no standalone text

Completion standard: PromotionThreat-5 closes when `StoryTable` orders PromotionThreat deterministically against PawnAdvance, PawnStop, Material, Defense, Hanging, and Line rows; no row borrows another row's claim meaning; actual material and tactical claims stay in their existing homes; and capped or refuted PromotionThreat rows do not reach standalone ExplanationPlan, renderer, or LLM wording.

### PromotionThreat-6 ExplanationPlan

PromotionThreat-6 lowers only a selected uncapped `Lead` Verdict for `Scene.PromotionThreat`.

`ExplanationPlan` may admit only the `threatens_promotion_next` claim key.

PromotionThreat-6 allowed claim keys:

- `threatens_promotion_next`

PromotionThreat-6 forbidden claim keys:

- `unstoppable_pawn`
- `will_promote`
- `cannot_be_stopped`
- `wins_endgame`
- `converts_advantage`
- `best_move`
- `only_move`
- `forced`
- `tablebase_win`
- `no_counterplay`

Support, Context, Blocked, capped, and refuted PromotionThreat rows have no standalone claim.

Completion standard: PromotionThreat-6 closes when only selected uncapped Lead PromotionThreat Verdicts lower to `threatens_promotion_next`; Support, Context, Blocked, capped, and refuted rows create no standalone claim; and forbidden promotion, unstoppable, winning, conversion, best/only, forced, tablebase, and no-counterplay claim keys remain absent.

### PromotionThreat-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

PromotionThreat-7 allowed deterministic wording:

- `{route} threatens to promote next.`

PromotionThreat-7 forbidden renderer wording:

- `will promote`
- `cannot be stopped`
- `unstoppable`
- `wins`
- `winning endgame`
- `converts`
- `best move`
- `only move`
- `forces`
- `tablebase win`
- `no counterplay`

Renderer must not read `Story`, `Verdict`, `PromotionThreatProof`, `BoardFacts`, `EngineCheck`, `proofFailures`, raw legal continuations, or source rows.

Completion standard: PromotionThreat-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead PromotionThreat as `{route} threatens to promote next.`, rejects Support, Context, Blocked, capped, refuted, no-claim, and malformed plans, and emits none of the forbidden promotion, unstoppable, winning, conversion, best/only, forced, tablebase, or no-counterplay wording.

### PromotionThreat-8 LLM Smoke

PromotionThreat-8 reuses only the existing 8B LLM smoke boundary.

PromotionThreat-8 LLM input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

PromotionThreat-8 forbidden LLM input and additions:

- raw `Story`
- raw `PromotionThreatProof`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`
- new move
- new line
- actual promotion claim
- unstoppable claim
- winning claim
- conversion claim
- tablebase claim

Completion standard: PromotionThreat-8 closes when LLM smoke receives only rendered text contract fields for PromotionThreat, rejects raw Story, PromotionThreatProof, BoardFacts, EngineCheck, raw PV, proofFailures, new moves, new lines, actual promotion, unstoppable, winning, conversion, and tablebase claims, and opens no public/user-facing LLM narration, production API, or public route 200.

PromotionThreatProof must prove:

- moving side
- rival side
- pawn identity before the creating move
- pawn identity after the creating move
- legal pawn creating move
- creating move is not itself a promotion
- exact after-board replay
- pawn is on the penultimate rank after the creating move
- promotion square
- promotion route
- same-board proof
- legal next-move promotion continuation on the exact after-board

`ScenePromotionThreat` is the only writer that may create
`Scene.PromotionThreat`.

ScenePromotionThreat writer conditions:

- complete `StoryProof`
- complete `PromotionThreatProof`
- same-board legal replay
- legal pawn creating move
- legal next-move promotion route
- writer = `ScenePromotionThreat`
- `scene = PromotionThreat`
- `tactic = None`
- `plan = None`
- `side = creating pawn side`
- `rival = opposite side`
- `target = promotion square`
- `anchor = creating pawn origin square`
- `route = creating pawn move`

`Scene.PromotionThreat` must not create `Scene.PawnAdvance`,
`Scene.PawnStop`, `Scene.Promotion`, `Tactic.Promote`, broad PawnTactic,
conversion, race, tablebase, or endgame-result meaning.

ExplanationPlan may admit only selected uncapped Lead
`Scene.PromotionThreat` Verdict data. The only allowed claim key is
`threatens_promotion_next`. Support, Context, Blocked, capped, and refuted rows have no
standalone claim.

Allowed deterministic wording:

- `{route} threatens to promote next.`

Forbidden wording includes:

- `will promote`
- `queens`
- `unstoppable`
- `cannot be stopped`
- `wins`
- `winning endgame`
- `conversion`
- `tablebase`
- `pawn race`
- `best move`
- `only move`
- `forced`
- `no counterplay`

Renderer input is `ExplanationPlan` only. Renderer must not read `Story`,
`Verdict`, `PromotionThreatProof`, `BoardFacts`, `EngineCheck`,
`proofFailures`, raw legal continuations, or source rows.

The existing LLM smoke boundary may rephrase only rendered text, claim key,
strength, forbidden wording, and the instruction `Rephrase only. Do not add
chess facts.` It does not open public/user-facing LLM narration.

PromotionThreat-0 duplicate audit:

- one chess meaning: immediate next-move promotion threat only
- one proof home: `PromotionThreatProof`
- one Story label: `Scene.PromotionThreat`
- one speech key: `threatens_promotion_next`
- one detailed live authority document: `StoryInteractionLaw.md`

Completion standard: PromotionThreat-0 closes when a legal pawn move creating a
legal next-move promotion route on the exact after-board can produce only
bounded `Scene.PromotionThreat` wording, while actual promotion, unstoppable
conversion, endgame, tablebase, pawn-race, best/only/forced, public route
`200`, production API, and public/user-facing LLM narration remain closed.

### PromotionThreat Closeout Hard Cleanup

PromotionThreat Closeout opens no new chess meaning. It only audits the PromotionThreat hard cleanup surface.

PromotionThreat Closeout must confirm:

- `PromotionThreatProof` proves only immediate next-move promotion-threat evidence on the exact after-board.
- `Scene.PromotionThreat` is the only Story label for this immediate threat meaning.
- `threatens_promotion_next` is the only speech key for this meaning.

PromotionThreat owns no `Scene.PawnAdvance`, `Scene.PawnStop`, actual Promotion, or PawnBreak meaning.
PromotionThreat owns no `Scene.Material`, `Scene.Defense`, `Tactic.Hanging`, or Line / Defender tactic meaning.
`unstoppable`, `will promote`, `cannot be stopped`, `conversion`, and `tablebase` remain forbidden wording only, not live authority.

PromotionThreat Closeout duplicate checks:

- one chess meaning: immediate next-move promotion threat only
- one proof home: `PromotionThreatProof`
- one Story label: `Scene.PromotionThreat`
- one speech key: `threatens_promotion_next`
- one detailed live authority document: `StoryInteractionLaw.md`

`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PromotionThreat only.
Renderer and LLM wording must remain no stronger than `threatens_promotion_next`.
Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PromotionThreat Closeout closes when `PromotionThreatProof`,
`Scene.PromotionThreat`, and `threatens_promotion_next` remain separate authority homes,
PromotionThreat owns no PawnAdvance, PawnStop, actual Promotion, PawnBreak, Material,
Defense, Hanging, or Line / Defender tactic meaning, closed promotion, unstoppable,
conversion, and tablebase wording remains forbidden only, renderer and LLM smoke stay
bounded to `threatens_promotion_next`, detailed authority stays only in
`StoryInteractionLaw.md`, and public route `200`, production API, and public/user-facing
LLM narration remain closed.

### Promotion-0 Charter

Promotion-0 opens only the fourth narrow Pawn / Promotion Neighborhood vertical slice.

First positive scope is not broad promotion advantage, winning pawn, conversion,
or endgame result.

Core sentence:

PromotionProof proves the legal promotion event. Scene.Promotion may speak only the promotion event, not conversion or winning.

Promotion-0 opens:

- narrow `Scene.Promotion`
- legal pawn promotion move
- non-capturing promotion only
- exact board replay
- promotion square
- promoted piece identity
- selected Verdict after bounded promotion wording

Promotion-0 does not open:

- capture promotion
- promotion material result
- winning endgame
- conversion
- decisive advantage
- best move / only move
- forced win
- tablebase result
- promotion threat
- unstoppable pawn
- public route `200`
- production API
- public/user-facing LLM narration

`PromotionProof` is the proof home for this slice. It is not a public `Story`
and does not own material result, conversion, tablebase, endgame result, or
unstoppable-pawn meaning.

### Promotion-1 PromotionProof

`PromotionProof` is the proof home for the narrow actual promotion event.
It proves legal non-capturing promotion evidence only; it is not a public
`Story`.

PromotionProof must prove:

- promoting side
- rival side
- pawn identity
- origin square
- promotion square
- legal promotion move
- move is non-capturing
- promoted piece identity
- exact board replay
- pawn reaches final rank
- same-board proof

`PromotionProof` is not a public `Story`.
`PromotionThreatProof` does not own actual Promotion.
`PawnAdvanceProof` does not own actual Promotion.
`PawnStopProof` does not own actual Promotion.

Promotion-1 forbids:

- `wins`
- `decisive`
- `conversion`
- `promotion is enough`
- `best move`
- `tablebase`
- material value claim

proofFailures must not become renderer input or LLM input.

### Promotion-2 ScenePromotion Writer

`ScenePromotion` is the named writer for narrow `Scene.Promotion`.

ScenePromotion writer conditions:

- complete `StoryProof`
- complete `PromotionProof`
- same-board legal replay
- legal non-capturing promotion move
- promotion square complete
- promoted piece identity complete
- writer = `ScenePromotion`
- `EngineCheck` does not `Refute`

ScenePromotion Story identity:

- `scene = Promotion`
- `tactic = None`
- `plan = None`
- `side = promoting side`
- `rival = rival side`
- `target = promotion square`
- `anchor = pawn origin square`
- `route = promotion move`

`ScenePromotion` must not own `Scene.PromotionThreat` meaning.
`ScenePromotion` must not create a `Scene.Material` claim.
`ScenePromotion` must not create winning, conversion, or tablebase meaning.

Promotion-2 does not open ExplanationPlan, renderer, LLM narration, public route `200`, or production API.

### Promotion-3 Negative Corpus

A pawn near promotion is not Promotion. Legal promotion event proof or silence.

Promotion-3 must close:

- legal move 아님
- same-board proof 없음
- pawn move가 아님
- final rank에 도달하지 않음
- promotion piece identity 없음
- capture promotion
- promotion threat일 뿐 actual promotion 아님
- material result를 promotion claim으로 말하려 함
- winning/conversion/tablebase wording 유입

`ScenePromotion` must stay silent for every Promotion-3 negative unless complete `PromotionProof` proves the legal non-capturing promotion event.

Promotion-3 does not open capture promotion, promotion material result, winning endgame, conversion, decisive advantage, tablebase result, best move, only move, forced win, promotion threat, unstoppable pawn, ExplanationPlan, renderer, LLM narration, public route `200`, or production API.

### Promotion-4 EngineCheck Reuse

Promotion-4 reuses only the existing `EngineCheck` sidecar.

Promotion-4 rules:

- `EngineCheck` cannot create Promotion.
- `Supports` creates no new claim.
- `Caps` suppresses standalone Promotion speech or weakens it to bounded relation-only evidence when downstream speech opens.
- `Refutes` blocks the Promotion Story.
- `Unknown` creates no engine expression.

Promotion-4 forbidden engine wording:

- engine says
- eval number
- tablebase result
- best move
- only move
- winning endgame
- forced win

Completion standard: Promotion-4 closes when `EngineCheck` can attach only to an existing same-board `ScenePromotion` Story route, cannot create Promotion, cannot add a new claim under Supports, suppresses or bounds capped rows, blocks refuted rows, keeps Unknown engine-silent, and exposes no engine wording, eval numbers, tablebase, best-move, only-move, winning-endgame, or forced-win claims.

### Promotion-5 StoryTable Integration

Promotion-5 integrates `Scene.Promotion` into `StoryTable` collision behavior only.

Promotion-5 collision rows:

- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- existing Material / Defense / Hanging / Line rows

Promotion-5 verification:

- input order remains stable
- Promotion does not own PromotionThreat meaning
- PromotionThreat does not own actual Promotion meaning
- Promotion does not own Material claim
- actual material/capture result remains in the existing material home
- capped or refuted Promotion creates no standalone text

`Scene.Promotion` may be selected only as the legal non-capturing promotion
event. It must not become a promotion-threat, capture-result, material,
winning, conversion, best-move, only-move, tablebase, or forced-win row.

`Scene.Promotion` yields to already-open material, defense, hanging, and line
claim homes when those rows exist. Those rows own their existing claim keys;
Promotion remains event proof and creates no standalone downstream speech in
this slice.

Completion standard: Promotion-5 closes when `StoryTable` orders actual Promotion deterministically against PawnAdvance, PawnStop, PromotionThreat, Material, Defense, Hanging, and Line rows; Promotion and PromotionThreat cannot borrow each other's meaning; actual material and capture-result claims stay in existing material homes; and capped or refuted Promotion rows do not reach standalone ExplanationPlan, renderer, or LLM wording.

### Promotion-6 ExplanationPlan

Promotion-6 lowers only a selected uncapped `Lead` Verdict for `Scene.Promotion`.

`ExplanationPlan` may admit only the `promotes_pawn` claim key.

Promotion-6 allowed claim key:

- `promotes_pawn`

Promotion-6 forbidden claim keys:

- `wins_endgame`
- `converts_advantage`
- `decisive`
- `best_move`
- `only_move`
- `forced_win`
- `tablebase_win`
- `unstoppable_pawn`
- `material_gain`

Support, Context, Blocked, capped, and refuted Promotion rows have no standalone claim.

Promotion-6 opens only the internal ExplanationPlan handoff for the legal
promotion event. It does not open renderer wording, LLM narration, public
route `200`, production API output, conversion, winning, best-move, only-move,
forced-win, tablebase, unstoppable-pawn, or material-gain speech.

Completion standard: Promotion-6 closes when only selected uncapped Lead Promotion Verdicts lower to ExplanationPlan with `promotes_pawn`; forbidden keys remain closed; Support, Context, Blocked, capped, and refuted Promotion rows produce no standalone ExplanationPlan claim; and renderer, LLM, production API, public route, winning, conversion, material-gain, best-move, only-move, forced-win, tablebase, and unstoppable-pawn speech remain closed.

### Promotion-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

Promotion-7 allowed deterministic wording:

- `{route} promotes the pawn.`

Promotion-7 forbidden renderer wording:

- `wins`
- `winning endgame`
- `decisive`
- `converts`
- `best move`
- `only move`
- `forces`
- `tablebase win`
- `wins material`
- `cannot be stopped`

Renderer must not read `Story`, `Verdict`, `PromotionProof`, `BoardFacts`, `EngineCheck`, `proofFailures`, or source rows.

Completion standard: Promotion-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead Promotion as `{route} promotes the pawn.`, rejects Support, Context, Blocked, capped, refuted, no-claim, and malformed plans, and emits none of the forbidden winning, conversion, best/only, forced, tablebase, material-gain, or unstoppable wording.

### Promotion-8 LLM Smoke

Promotion-8 reuses only the existing 8B LLM smoke boundary.

Promotion-8 allowed LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Promotion-8 forbidden LLM smoke inputs:

- raw `Story`
- raw `PromotionProof`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`

Promotion-8 LLM smoke must reject output that adds:

- new move
- new line
- winning, conversion, tablebase, or material-gain claim

Completion standard: Promotion-8 closes when LLM smoke receives only rendered text contract fields for Promotion, rejects raw Story, PromotionProof, BoardFacts, EngineCheck, raw PV, proofFailures, new moves, new lines, and winning, conversion, tablebase, or material-gain claims, and reuses `Rephrase only. Do not add chess facts.` without adding new LLM behavior.

### Promotion Closeout Hard Cleanup

`PromotionProof`, `Scene.Promotion`, and `promotes_pawn` remain separate authority homes.

`PromotionProof` owns only proof of the legal non-capturing promotion event.
`Scene.Promotion` owns only the public Story identity for that event.
`promotes_pawn` owns only bounded downstream speech for the selected uncapped Lead Verdict.

`Scene.Promotion` owns no `Scene.PromotionThreat`, `Scene.PawnAdvance`, `Scene.PawnStop`, or PawnBreak meaning.
`Scene.Promotion` owns no Material, Defense, Hanging, or Line / Defender tactic meaning.

`winning`, `conversion`, `decisive`, `tablebase`, and material-gain wording remain forbidden wording only, not live Promotion authority.
Capture promotion remains closed in this slice.

Promotion Closeout duplicate checks:

- one chess meaning: legal non-capturing pawn promotion event only
- one proof home: `PromotionProof`
- one Story label: `Scene.Promotion`
- one speech key: `promotes_pawn`
- one detailed live authority document: `StoryInteractionLaw.md`

`README.md`, SSOT, Architecture, Contract, and Manifest may summarize Promotion only.
Renderer and LLM wording must remain no stronger than `promotes_pawn`.
Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: Promotion Closeout closes when `PromotionProof`, `Scene.Promotion`, and `promotes_pawn` remain separate authority homes, Promotion owns no PromotionThreat, PawnAdvance, PawnStop, PawnBreak, Material, Defense, Hanging, or Line / Defender tactic meaning, winning, conversion, decisive, tablebase, and material-gain wording remains forbidden only, capture promotion remains closed, renderer and LLM smoke stay bounded to `promotes_pawn`, detailed authority stays only in `StoryInteractionLaw.md`, and public route `200`, production API, and public/user-facing LLM narration remain closed.

### PNC-0 Pawn / Promotion Neighborhood Closeout

Pawn / Promotion closes as four narrow proof-backed slices. It opens no pawn
strategy, conversion, tablebase, pawn race, or public surface.

PNC-0 opens only closeout work:

- Pawn / Promotion neighborhood closeout
- scope audit
- duplication audit
- authority audit
- collision and downstream audit
- docs simplification
- next-neighborhood handoff

PNC-0 closes only already-open narrow slices:

- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- PIH hardening over already-open pawn rows

PNC-0 related proof homes:

- `PawnAdvanceProof`
- `PawnStopProof`
- `PromotionThreatProof`
- `PromotionProof`

PNC-0 opens no:

- new Story family
- new proof home
- new Story writer
- new renderer template
- new LLM behavior
- PawnBreak
- broad PawnTactic
- pawn strategy
- conversion
- winning endgame
- tablebase
- pawn race
- king route or opposition
- public route `200`
- production API
- public/user-facing LLM narration

PNC-0 scope audit:

- `Scene.PawnAdvance` remains only bounded passed-pawn progress with
  `PawnAdvanceProof` and `advances_passed_pawn`.
- `Scene.PawnStop` remains only bounded immediate next-square stop with
  `PawnStopProof` and `stops_pawn_advance`.
- `Scene.PromotionThreat` remains only legal next-move promotion-route threat
  with `PromotionThreatProof` and `threatens_promotion_next`.
- `Scene.Promotion` remains only legal non-capturing promotion event with
  `PromotionProof` and `promotes_pawn`.
- PIH remains hardening only and creates no fifth pawn meaning.

PNC-0 duplication audit:

- one chess meaning per closed slice.
- one proof home per closed slice.
- one Story label per closed slice.
- one speech key per closed slice.
- one detailed live authority document: `StoryInteractionLaw.md`.

PNC-0 authority audit:

- Board Facts observes pawn structure only.
- Proof homes bind only their own exact-board proof.
- Story writers create only their own already-open Story identity.
- StoryTable orders existing rows and creates no pawn meaning.
- ExplanationPlan, renderer, and LLM smoke stay downstream of selected Verdict
  data and do not repair, rank, or strengthen pawn meaning.
- proofFailures, raw proof text, raw engine evidence, raw PV, source rows, and
  test-helper names remain internal diagnostics only.

PNC-0 collision and downstream audit:

- PawnAdvance, PawnStop, PromotionThreat, and Promotion must not borrow from or
  overwrite each other's meaning.
- Material, Defense, Hanging, and Line / Defender homes keep their own claims.
- PawnBreak, broad PawnTactic, pawn strategy, conversion, tablebase, pawn race,
  king route, opposition, winning, decisive, best-move, only-move, forced,
  no-counterplay, and unstoppable-pawn terms remain closed or forbidden wording
  only.
- Support, Context, Blocked, capped, refuted, incomplete, wrong-board, stale,
  and route-mismatched rows create no standalone downstream speech.
- `/api/commentary/render` and `/internal/commentary/render-local-probe` remain
  fail-closed public-surface tombstones.

PNC-0 docs simplification:

- `README.md`, SSOT, Architecture, Contract, and Manifest may summarize only
  that Pawn / Promotion closes as four narrow proof-backed slices.
- Summary docs must not repeat PNC-0 checklists, slice closeout checklists,
  negative corpora, renderer law, LLM law, or completion detail.

PNC-0 next-neighborhood handoff:

- any next neighborhood must start from a new explicit charter in
  `StoryInteractionLaw.md`.
- PNC-0 does not pre-open the next neighborhood, a public surface, or any broad
  pawn meaning.

Completion standard: PNC-0 closes when PawnAdvance, PawnStop,
PromotionThreat, Promotion, and PIH hardening remain separate, proof-backed,
non-duplicated authority homes; no new Story family, proof home, Story writer,
renderer template, LLM behavior, PawnBreak, broad PawnTactic, pawn strategy,
conversion, winning endgame, tablebase, pawn race, king route, opposition,
public route `200`, production API, or public/user-facing LLM narration opens;
summary docs stay short and non-authoritative; detailed authority remains only
in `StoryInteractionLaw.md`; and the next neighborhood remains unopened until a
new explicit charter.

### PNC-1 Scope Audit

PNC-1 confirms:

- the only opened positive pawn/promotion Stories are `Scene.PawnAdvance`,
  `Scene.PawnStop`, `Scene.PromotionThreat`, and `Scene.Promotion`.
- `Scene.PawnAdvance` speaks only its first scope: bounded progress by an
  already-passed pawn through a legal one-square non-capturing non-promotion
  advance that remains passed on the exact after-board.
- `Scene.PawnStop` speaks only its first scope: bounded immediate stop of an
  already-passed pawn's next non-promotion advance square on the exact
  after-board.
- `Scene.PromotionThreat` speaks only its first scope: a legal pawn move that
  creates a legal next-move promotion route on the exact after-board.
- `Scene.Promotion` speaks only its first scope: a legal non-capturing
  promotion event on the exact board.

PNC-1 runtime path audit:

- PawnBreak is not open as a positive Story.
- pawn race is not open as a positive Story.
- tablebase is not open as a positive Story.
- king route is not open as a positive Story.
- opposition is not open as a positive Story.
- conversion is not open as a positive pawn/promotion Story.

Closed pawn names are not missing features inside this neighborhood.
They remain closed until a separate charter opens them.

Completion standard: PNC-1 closes when the positive pawn/promotion Story set
is exactly four, each Story is bounded to its first scope, and PawnBreak, pawn
race, tablebase, king route, opposition, and conversion have no positive Story
runtime path in this neighborhood.

### PNC-2 Duplication Audit

PNC-2 confirms:

- no chess meaning is duplicated across two pawn/promotion Story labels.
- no proof responsibility is duplicated across two pawn/promotion proof homes.
- no speech claim is split across two pawn/promotion claim keys.
- no detailed closeout rule is repeated across multiple live documents.

PNC-2 specific duplication checks:

- `Scene.PawnAdvance` and `Scene.PromotionThreat` do not co-own pawn progress.
  PawnAdvance owns only bounded passed-pawn advance; PromotionThreat owns only
  legal next-move promotion-route threat.
- `Scene.PawnStop` does not own the opposite meaning of PromotionThreat or Promotion.
  PawnStop owns only the immediate next-square stop and does not claim stopped
  promotion, prevented promotion, permanent stop, tablebase draw, or conversion
  prevention.
- `Scene.PromotionThreat` owns next-move promotion-route threat only.
- `Scene.Promotion` owns actual legal non-capturing promotion only.
- `Scene.Promotion` owns no material gain, conversion, or winning meaning.
- `PawnAdvanceProof`, `PawnStopProof`, `PromotionThreatProof`, and
  `PromotionProof` do not invade each other's proof responsibility.

Proof-home separation:

- `PawnAdvanceProof` proves legal one-square non-capturing non-promotion
  advance by an already-passed pawn that remains passed.
- `PawnStopProof` proves a legal move directly stops an already-passed pawn's
  next non-promotion advance square.
- `PromotionThreatProof` proves a legal pawn move creates a legal next-move
  promotion route.
- `PromotionProof` proves a legal non-capturing promotion event.

Speech-key separation:

- `advances_passed_pawn` belongs only to `Scene.PawnAdvance`.
- `stops_pawn_advance` belongs only to `Scene.PawnStop`.
- `threatens_promotion_next` belongs only to `Scene.PromotionThreat`.
- `promotes_pawn` belongs only to `Scene.Promotion`.

Document-authority separation:

- `StoryInteractionLaw.md` owns detailed PNC-2 rules.
- `README.md`, SSOT, Architecture, Contract, and Manifest may summarize only.
- Summary documents must not repeat PNC-2 checklists or completion detail.

One chess meaning, one proof home, one Story label, one speech key, one live authority document.

Completion standard: PNC-2 closes when PawnAdvance, PawnStop,
PromotionThreat, and Promotion each have exactly one meaning, one proof home,
one Story label, and one speech key, and detailed PNC law remains only in
`StoryInteractionLaw.md`.

### PNC-3 Authority Audit

PNC-3 layer authority:

- `PassedPawnObservation`: structure observation only.
- `PawnAdvanceProof`: legal passed-pawn advance remains passed.
- `PawnStopProof`: next advance square is directly stopped.
- `PromotionThreatProof`: next-move promotion threat is legal on the exact after-board.
- `PromotionProof`: actual legal promotion event.
- Story writers: named proof-backed Story permission only.
- `StoryTable`: ordering only.
- `Verdict`: selected result only.
- `ExplanationPlan`: bounded speech claim only.
- Renderer: phrasing only.
- LLM smoke: rephrase only.

PNC-3 forbids:

- proof homes speaking as Story labels.
- Story writers owning conversion or result meaning.
- `StoryTable` creating new pawn meaning.
- `ExplanationPlan` reading raw proof.
- Renderer or LLM smoke repairing or upgrading proof.

PNC-3 confirms the authority chain stays:

`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `ExplanationPlan` -> Renderer -> LLM smoke

Completion standard: PNC-3 closes when each pawn/promotion layer keeps
exactly one authority job, proof homes do not speak as Story labels,
Story writers do not own conversion or result meaning, `StoryTable` creates no
new pawn meaning, `ExplanationPlan` reads no raw proof, and Renderer and LLM
smoke do not repair or upgrade proof.

### PNC-4 Collision Audit

PNC-4 opens no new runtime fixture when existing tests already prove the
collision target. It records the audit only.

PNC-4 collision targets:

- `Scene.PawnAdvance` vs `Scene.PawnStop`.
- `Scene.PawnAdvance` vs `Scene.PromotionThreat`.
- `Scene.PawnStop` vs `Scene.PromotionThreat`.
- `Scene.PromotionThreat` vs `Scene.Promotion`.
- `Scene.Promotion` vs `Scene.Material`.
- pawn rows vs `Scene.Defense`.
- pawn rows vs existing tactic and line rows.
- EngineCheck Caps and Refutes over each pawn row.

PNC-4 runtime checks:

- input order remains stable.
- duplicate Lead is absent.
- incomplete rows are not Lead.
- capped or refuted rows have no standalone text.
- actual promotion stays in `Scene.Promotion`.
- next-move promotion threat stays in `Scene.PromotionThreat`.
- next advance stop stays in `Scene.PawnStop`.
- passed-pawn advance stays in `Scene.PawnAdvance`.
- material and tactical claims stay in their existing homes.

PNC-4 reuses existing runtime tests:

- `PIH-1 fixture map covers pawn interaction hardening categories`.
- `PIH-2 Role Stability keeps pawn rows deterministic without duplicate pawn claims`.
- `PIH-3 Meaning Ownership Boundary keeps pawn collision rows on their own claims`.
- `PIH-4 EngineCheck Interaction reuses existing pawn statuses without engine-owned claims`.
- `PIH-6 Downstream Boundary Smoke sends only selected Lead pawn Verdicts to text stages`.
- `PromotionThreat-5 StoryTable keeps existing rows stable and claim homes separate`.
- `Promotion-5 StoryTable keeps actual Promotion stable and claim homes separate`.

Completion standard: PNC-4 closes when these existing runtime tests keep input
order stable, prevent duplicate Lead, keep incomplete rows out of Lead, keep
capped and refuted rows without standalone text, and preserve each pawn,
promotion, material, defense, tactic, and line claim in its own home.

### PNC-5 Downstream Boundary Audit

PNC-5 opens no new renderer wording, LLM behavior, public route `200`,
production API, or public/user-facing LLM narration.

PNC-5 downstream authority:

- only selected uncapped Lead Verdicts may lower into `ExplanationPlan`.
- `ExplanationPlan` input is selected Verdict only.
- Renderer input is `ExplanationPlan` only.
- LLM smoke input is only renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction.

PNC-5 non-Lead rows:

- Support rows have no standalone text.
- Context rows have no standalone text.
- Blocked rows have no standalone text.
- capped rows have no standalone text.
- refuted rows have no standalone text.

PNC-5 forbidden downstream wording:

- unstoppable.
- will promote.
- cannot be stopped.
- wins or winning.
- decisive.
- conversion.
- tablebase.
- draw or holds.
- best move or only move.
- forced.
- no counterplay.
- pressure or initiative.

PNC-5 reuses existing runtime tests:

- `Stage 6-5 ExplanationPlan accepts selected Verdict only`.
- `Stage 6-5 ExplanationPlan exposes no raw proof material input`.
- `Stage 7-1 DeterministicRenderer accepts ExplanationPlan only`.
- `Stage 7-1 DeterministicRenderer cannot create text without an ExplanationPlan`.
- `Stage 8B Codex CLI prompt smoke uses only rendered text contract`.
- `Stage 8 narration smoke exposes no raw proof or production API input`.
- `PIH-6 Downstream Boundary Smoke sends only selected Lead pawn Verdicts to text stages`.
- `PawnAdvance-8 LLM smoke reuses 8B prompt boundary without new chess facts`.
- `PawnStop-8 LLM smoke reuses 8B prompt boundary without new chess facts`.
- `PromotionThreat-8 LLM smoke reuses 8B boundary without new chess facts`.
- `Promotion-8 LLM smoke reuses 8B boundary without new chess facts`.

Completion standard: PNC-5 closes when downstream stages accept only selected uncapped Lead pawn/promotion claims, `ExplanationPlan`, renderer, and LLM smoke keep their input boundaries, Support, Context, Blocked, capped, and refuted rows produce no standalone text, and forbidden downstream wording remains rejected.

### PNC-6 Documentation Simplification

PNC-6 documentation simplification:

- detailed Pawn / Promotion closeout authority lives only in `StoryInteractionLaw.md`.
- `README.md`, SSOT, Architecture, Contract, AGENTS, and LegacyPruneManifest remain summary-only.
- the same closeout rule must not be repeated across live documents.

PNC-6 summary-only documents:

- `README.md`: summary pointer only.
- `ChessCommentarySSOT.md`: summary pointer only.
- `ChessModelArchitecture.md`: summary pointer only.
- `ChessModelContract.md`: summary pointer only.
- `AGENTS.md`: durable operator guardrails only.
- `LegacyPruneManifest.md`: pruning summary only.

PNC-6 closed-summary wording:

- closed pawn terms are closed-summary terms, not work queue entries.
- PawnTactic, pawn strategy, promotion race, conversion, tablebase, king route, and opposition remain closed-summary terms only.

Completion standard: PNC-6 closes when detailed closeout checklists exist only in `StoryInteractionLaw.md`, summary documents and AGENTS contain no detailed PNC headings, closed pawn terms read only as closed summaries, and live authority lists remain aligned across docs tests.

### PNC-7 Test Helper / Runtime Boundary Audit

PNC-7 test helper runtime boundary:

- test helpers are not runtime authority.
- fixture names are not Story family names.
- negative corpus helpers are not production concepts.
- forbidden wording checks must target public overclaim wording, not arbitrary internal field names.
- runtime source must not contain closeout-only terminology.

PNC-7 forbids helper promotion:

- no test helper may become a proof home, Story label, claim key, renderer input, or LLM input.
- no fixture category may name a new pawn Story family.
- no negative corpus helper may be imported by runtime code.
- no forbidden wording checker may reject safe internal field names by itself.

PNC-7 runtime source guard:

- runtime source must not contain `PNC-`.
- runtime source must not contain `closeout`.
- runtime source must not contain `fixture map`.
- runtime source must not contain `negative corpus`.
- runtime source must not contain `Pawn Interaction Hardening`.

Completion standard: PNC-7 closes when helper names remain test-local, fixture names read only as fixtures, negative corpus helpers stay out of runtime, forbidden wording checks stay bounded to public overclaim wording, and runtime source contains no closeout-only terms.

### PNC Closeout Final Completion Standard

PNC Closeout final completion standard:

- no new Story family.
- no new proof home.
- no new renderer wording.
- no new LLM behavior.
- only `Scene.PawnAdvance`, `Scene.PawnStop`, `Scene.PromotionThreat`, and `Scene.Promotion` remain as the positive closeout baseline slices.
- no duplicate meaning.
- no duplicate authority.
- no duplicate terminology.
- no duplicate detailed docs.
- public route, production API, and public/user-facing LLM narration remain closed.

PNC final verification:

- docs authority tests pass.
- chess foundation tests pass.
- `git diff --check` is clean.

Final PNC closeout opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, PawnBreak, broad PawnTactic, pawn strategy, conversion, winning endgame, tablebase, pawn race, king route, opposition, public route `200`, production API, or public/user-facing LLM narration.

## Pawn Structure / Break Neighborhood

### PawnBreak-0 Charter

PawnBreak-0 opens the first Pawn Structure / Break Neighborhood vertical slice.

Core sentence: Pawn facts observe pawn contact. PawnBreakProof proves one
direct lever. Scene.PawnBreak may speak only bounded pawn contact, not
structural advantage.

PawnBreak-0 opens only:

- narrow `Scene.PawnBreak`
- `PawnBreakProof`
- `ScenePawnBreak`
- legal pawn move
- one rival pawn target
- exact-board direct pawn lever/contact after the move
- selected Verdict to bounded pawn-break wording
- `challenges_pawn`

PawnBreak-0 positive scope:

- a legal pawn move
- the moved pawn remains on the exact after-board
- the moved pawn directly attacks exactly one rival pawn on the exact after-board
- the direct rival-pawn lever was created by the move
- complete StoryProof
- complete `PawnBreakProof`
- named writer = `ScenePawnBreak`

PawnBreak-0 must stay silent for:

- non-pawn moves
- illegal moves
- pawn captures
- zero rival pawn targets
- multiple rival pawn targets
- already-existing pawn contact without a new direct lever
- missing exact-board proof
- incomplete StoryProof
- missing or incomplete `PawnBreakProof`
- writerless or contaminated rows

PawnBreak-0 does not open:

- broad PawnTactic
- broad pawn strategy
- opens position
- breaks through
- creates passed pawn
- weakens structure
- wins space
- initiative or pressure
- conversion
- winning or decisive
- best move or only move
- public route `200`
- production API
- public/user-facing LLM narration

Layer ownership:

- Board Facts owns pawn contact observations.
- `PawnBreakProof` proves only one legal move creating one direct rival-pawn lever.
- `ScenePawnBreak` is the only writer that may create `Scene.PawnBreak`.
- `StoryTable` orders an existing `Scene.PawnBreak`; it does not create it.
- `ExplanationPlan` may lower only selected uncapped Lead `Scene.PawnBreak`
  Verdicts to `challenges_pawn`.
- Renderer and LLM smoke may phrase only bounded direct-pawn-challenge wording.

Completion standard: PawnBreak-0 closes when one legal pawn move creating one
direct rival-pawn lever can become narrow `Scene.PawnBreak`, all zero-target,
multi-target, capture, non-pawn, strategy, structure, and conversion false
positives stay silent, and public route `200`, production API, broad
PawnTactic, broad pawn strategy, structural advantage, initiative, pressure,
conversion, winning, best/only, and public/user-facing LLM narration remain
closed.

### PawnBreak-1 PawnBreakProof

PawnBreak-1 opens `PawnBreakProof` as the proof home for narrow
`Scene.PawnBreak` direct pawn contact.

PawnBreakProof must prove:

- breaking side
- rival side
- pawn identity
- origin square
- destination square
- legal pawn move
- move is non-promotion
- rival pawn target
- after-board pawn lever/contact exists
- same-board proof
- exact after-board replay

First allowed contact kinds:

- `PawnChallengesPawn`
- `PawnLeverCreated`

Closed contact kinds:

- long-term structure weakness
- open-file claim
- passed-pawn creation
- pawn majority plan
- breakthrough sequence
- sacrifice line
- opens the position claim

`PawnBreakProof` is not a public Story.
`BoardFacts.PawnLever` is not a public Story.

Completion standard: PawnBreak-1 closes when `PawnBreakProof` proves only
same-board legal non-promotion pawn movement from origin to destination,
one rival pawn target, exact after-board replay, and one direct pawn contact
through `PawnChallengesPawn` and `PawnLeverCreated`, while all closed contact
kinds remain absent from proof ownership, Story meaning, renderer wording,
LLM smoke, public route `200`, production API, and public/user-facing LLM
narration.

### PawnBreak-2 Scene.PawnBreak Writer

PawnBreak-2 opens `ScenePawnBreak` as the named writer for narrow
`Scene.PawnBreak` only.

ScenePawnBreak writer conditions:

- complete StoryProof
- complete `PawnBreakProof`
- same-board legal replay
- legal non-promotion pawn move
- rival pawn target exists
- pawn lever/contact relation complete
- writer = `ScenePawnBreak`
- EngineCheck does not Refute

Scene.PawnBreak Story identity:

- scene = `PawnBreak`
- tactic = None
- plan = None
- side = breaking side
- rival = rival side
- target = rival pawn square
- anchor = pawn origin square
- route = pawn move

ScenePawnBreak must not create:

- `PassedPawnCreated`
- `Scene.Material` claim
- strategy meaning
- conversion meaning
- initiative meaning

Completion standard: PawnBreak-2 closes when only `ScenePawnBreak` can
write narrow `Scene.PawnBreak` from complete StoryProof plus complete
`PawnBreakProof`, EngineCheck Refutes cannot attach through the writer, Story
identity binds to breaking side, rival side, rival pawn target, pawn origin,
and pawn move, and the writer creates no passed-pawn-created, material,
strategy, conversion, or initiative claim.

### PawnBreak-3 Negative Corpus

PawnBreak-3 closes false positives around narrow `Scene.PawnBreak`.

A pawn move is not a pawn break. Complete direct pawn-contact proof or silence.

PawnBreak-3 must close:

- not a legal move
- no same-board proof
- not a pawn move
- promotion move
- no rival pawn target
- no direct pawn lever/contact on the after-board
- simple space gain being called a pawn break
- passed-pawn creation expansion
- open-file wording
- weak-pawn or weak-structure wording
- initiative wording
- material gain being spoken as a PawnBreak claim

Completion standard: PawnBreak-3 closes when every listed counterexample
fails before `Scene.PawnBreak` speech unless complete same-board
`PawnBreakProof` proves one legal non-promotion pawn move creates one direct
rival-pawn lever on the exact after-board, and downstream wording remains
bounded to direct pawn contact only.

### PawnBreak-4 EngineCheck Reuse

PawnBreak-4 reuses the existing `EngineCheck` sidecar only.

`EngineCheck` cannot create `Scene.PawnBreak`.

PawnBreak EngineCheck status rules:

- Supports creates no new claim
- Caps suppresses standalone `Scene.PawnBreak` claim output
- Refutes keeps the `Scene.PawnBreak` Story but makes it Blocked
- Unknown creates no engine expression

PawnBreak-4 forbids:

- engine says
- eval numbers
- best move
- only move
- breakthrough works
- winning structure

Completion standard: PawnBreak-4 closes when `EngineCheck` can only attach to
an existing same-board `Scene.PawnBreak` with complete `PawnBreakProof`, cannot
create PawnBreak or strengthen `challenges_pawn`, Supports adds no new
claim, Caps suppresses standalone output, Refutes becomes Blocked, Unknown
creates no engine expression, and engine says, eval numbers, best move, only
move, breakthrough works, and winning-structure wording remain forbidden.

### PawnBreak-5 StoryTable Integration

PawnBreak-5 collides narrow `Scene.PawnBreak` with existing open rows.

PawnBreak-5 collision targets:

- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- `Scene.Material`
- `Scene.Defense`
- `Tactic.Hanging`
- Line / Defender tactics
- `Scene.PawnBreak`

PawnBreak-5 must verify:

- input order stability
- `Scene.PawnBreak` owns no Material claim
- `Scene.PawnBreak` creates no PassedPawnCreated claim
- `Scene.PawnBreak` does not replace PawnAdvance or Promotion meaning
- actual material change now remains `Scene.Material`
- promotion/progress claims remain in existing pawn/promotion homes
- capped/refuted `Scene.PawnBreak` has no standalone text

Completion standard: PawnBreak-5 closes when StoryTable collision tests keep
`Scene.PawnBreak` below existing pawn progress, promotion, material, defense,
hanging, and line/defender claim homes; preserve stable results across input
order; keep actual material change now in `Scene.Material`; keep promotion and
progress claims in their existing pawn/promotion homes; prevent
PassedPawnCreated or Material claim ownership by `Scene.PawnBreak`; and leave
capped/refuted PawnBreak without standalone text.

### PawnBreak-6 ExplanationPlan

PawnBreak-6 opens ExplanationPlan only for selected uncapped Lead `Scene.PawnBreak` Verdicts.

PawnBreak-6 allowed claim key:

- `challenges_pawn`

PawnBreak-6 forbidden claim keys:

- `opens_position`
- `breaks_through`
- `creates_passed_pawn`
- `weakens_structure`
- `wins_space`
- `creates_pressure`
- `takes_initiative`
- `converts_advantage`
- `best_move`
- `only_move`
- `forced`

Support, Context, Blocked, capped, and refuted `Scene.PawnBreak` rows have no standalone claim.

Completion standard: PawnBreak-6 closes when only selected uncapped Lead
`Scene.PawnBreak` Verdicts with complete direct pawn-contact proof lower to the
`challenges_pawn` ExplanationPlan claim; Support, Context, Blocked, capped,
refuted, unselected, and non-leadAllowed rows produce no standalone
ExplanationPlan claim or rendered line; and forbidden strategic, conversion,
engine, passed-pawn, open-position, initiative, pressure, best-move,
only-move, and forced claim keys stay unopened.

### PawnBreak-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

Allowed renderer wording:

- `{route} challenges the pawn on {target}.`

Forbidden renderer wording:

- `opens the position`
- `breaks through`
- `creates a passer`
- `weakens the structure`
- `wins space`
- `takes the initiative`
- `creates pressure`
- `best move`
- `only move`
- `forces`

Renderer must not read `Story`, `Verdict`, `PawnBreakProof`, `BoardFacts`, `EngineCheck`, `proofFailures`, source rows, or LLM smoke.

Completion standard: PawnBreak-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead PawnBreak as `{route} challenges the pawn on {target}.`, rejects Support, Context, Blocked, capped, refuted, no-claim, and malformed plans, and emits none of the forbidden strategy, position-opening, passed-pawn, structure, space, initiative, pressure, best/only, or forcing wording.

### PawnBreak-8 LLM Smoke

PawnBreak-8 reuses only the existing 8B LLM smoke boundary.

PawnBreak-8 LLM input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

PawnBreak-8 forbidden LLM input:

- raw `Story`
- raw `PawnBreakProof`
- `BoardFacts`
- `PawnLever` raw data
- `EngineCheck`
- raw PV
- `proofFailures`

PawnBreak-8 forbidden LLM output:

- new move
- new line
- strategy claim
- passed-pawn claim
- open-file claim
- initiative claim
- conversion claim

Completion standard: PawnBreak-8 closes when LLM smoke receives only rendered text contract fields, reuses renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction, rejects raw Story, PawnBreakProof, BoardFacts, PawnLever raw data, EngineCheck, raw PV, proofFailures, new move, new line, and strategy, passed-pawn, open-file, initiative, or conversion claims, and adds no public LLM narration or production API surface.

### PawnBreak Closeout Hard Cleanup

`BoardFacts.PawnLever`, `PawnBreakProof`, `Scene.PawnBreak`, and `challenges_pawn` remain separate authority homes.

`BoardFacts.PawnLever` observes pawn contact only.
`PawnBreakProof` proves one direct rival-pawn lever only.
`Scene.PawnBreak` owns only the public Story identity for bounded direct pawn contact.
`challenges_pawn` owns only bounded downstream speech for selected uncapped Lead Verdicts.

`Scene.PawnBreak` owns no PawnAdvance, PawnStop, PromotionThreat, Promotion, or PassedPawnCreated meaning.
`Scene.PawnBreak` owns no Material, Defense, Hanging, DiscoveredAttack, Pin, RemoveGuard, or Skewer meaning.

`opens position`, `breakthrough`, `weakens structure`, `wins space`, `initiative`, and `pressure` remain forbidden wording only, not live PawnBreak authority.

PawnBreak duplicate checks:

- one chess meaning: one legal pawn move creating one direct rival-pawn lever.
- one proof home: `PawnBreakProof`.
- one Story label: `Scene.PawnBreak`.
- one speech key: `challenges_pawn`.
- one detailed live authority document: `StoryInteractionLaw.md`.

README, SSOT, Architecture, Contract, and Manifest may summarize PawnBreak only.
Renderer and LLM smoke wording must remain no stronger than `challenges_pawn`.
Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PawnBreak Closeout closes when BoardFacts.PawnLever, PawnBreakProof, Scene.PawnBreak, and challenges_pawn remain separate authority homes; PawnBreak owns no PawnAdvance, PawnStop, PromotionThreat, Promotion, PassedPawnCreated, Material, Defense, Hanging, DiscoveredAttack, Pin, RemoveGuard, or Skewer meaning; opens-position, breakthrough, weakens-structure, wins-space, initiative, and pressure remain forbidden wording only; one chess meaning, one proof home, one Story label, one speech key, and one detailed live authority document remain true; summary docs stay summaries only; renderer and LLM smoke remain no stronger than challenges_pawn; and public route `200`, production API, and public/user-facing LLM narration remain closed.

### PawnCapture-0 Charter

PawnCapture-0 opens the second Pawn Structure / Break Neighborhood vertical slice.

Core sentence:

PawnCaptureProof proves the pawn-captures-pawn event. Scene.PawnCapture may speak only that event, not material gain or structure advantage.

PawnCapture-0 opens only:

- narrow `Scene.PawnCapture`
- `PawnCaptureProof`
- `ScenePawnCapture`
- legal pawn move
- captured piece is one rival pawn
- exact-board replay
- selected Verdict to bounded pawn-capture wording
- `captures_rival_pawn`

PawnCapture-0 positive scope:

- a legal pawn move
- the moved pawn captures exactly one rival pawn on the exact board
- complete StoryProof
- complete `PawnCaptureProof`
- named writer = `ScenePawnCapture`

PawnCapture-0 must stay silent for:

- non-pawn moves
- illegal moves
- quiet pawn moves
- captured piece is not a pawn
- missing exact-board proof
- incomplete StoryProof
- missing or incomplete `PawnCaptureProof`
- writerless or contaminated rows

PawnCapture-0 does not open:

- material gain claim
- wins pawn
- creates passed pawn
- opens file
- weakens structure
- breaks through
- pawn majority change
- initiative / pressure
- conversion
- best move / only move
- public route `200`
- production API
- public/user-facing LLM narration

Layer ownership:

- Board Facts observes legal movement and pieces only.
- `PawnCaptureProof` proves only one legal pawn move capturing one rival pawn.
- `ScenePawnCapture` is the only writer that may create `Scene.PawnCapture`.
- `StoryTable` orders an existing `Scene.PawnCapture`; it does not create it.
- `ExplanationPlan` may lower only selected uncapped Lead `Scene.PawnCapture` Verdicts to `captures_rival_pawn`.
- Renderer and LLM smoke may phrase only bounded pawn-captures-pawn wording.

Completion standard: PawnCapture-0 closes when one legal pawn move capturing exactly one rival pawn can become narrow `Scene.PawnCapture`, non-pawn moves, illegal moves, quiet pawn moves, non-pawn captures, missing exact-board proof, incomplete StoryProof, missing or incomplete `PawnCaptureProof`, writerless rows, and contaminated rows stay silent, and material gain, wins-pawn, passed-pawn, open-file, structure-advantage, breakthrough, pawn-majority, initiative, pressure, conversion, best/only, public route `200`, production API, and public/user-facing LLM narration remain closed.

### PawnCapture-1 PawnCaptureProof

PawnCapture-1 narrows `PawnCaptureProof` as the proof home for the current `Scene.PawnCapture` slice.

PawnCaptureProof proves:

- capturing side
- rival side
- capturing pawn identity
- origin square
- capture square
- captured rival pawn identity
- legal pawn capture
- exact after-board replay
- same-board proof

PawnCapture-1 first scope:

- ordinary diagonal pawn capture only

PawnCapture-1 closed scope:

- en passant
- promotion capture
- capture that claims material gain
- capture that claims passed pawn creation
- capture that claims file opening
- capture that claims structural weakness

Proof-home boundary:

- `PawnCaptureProof` is not a public Story.
- `PawnCaptureProof` may bind `Scene.PawnCapture` only through a named `ScenePawnCapture` writer and selected Verdict.
- `CaptureResult` and `Scene.Material` remain the material meaning homes.
- `PawnCaptureProof` does not replace `CaptureResult` or `Scene.Material`.
- `PawnCaptureProof` must not own material gain, passed-pawn creation, file-opening, or structural-weakness meaning.

Completion standard: PawnCapture-1 closes when `PawnCaptureProof` explicitly proves ordinary diagonal legal pawn captures, binds capture square and captured rival pawn identity on the same board, exact after-board replay removes the captured pawn and moves the capturing pawn, en passant and promotion capture stay closed, PawnCaptureProof stays non-public and non-material, and CaptureResult plus Scene.Material remain separate material meaning homes.

### PawnCapture-2 Scene.PawnCapture Writer

`ScenePawnCapture` is the named writer for narrow `Scene.PawnCapture`.

PawnCapture-2 writer conditions:

- complete `StoryProof`
- complete `PawnCaptureProof`
- same-board legal replay
- legal ordinary pawn capture
- captured piece is rival pawn
- writer = `ScenePawnCapture`
- `EngineCheck` does not `Refute`

PawnCapture-2 Story identity:

- `scene = PawnCapture`
- `tactic = None`
- `plan = None`
- `side = capturing side`
- `rival = rival side`
- `target = capture square`
- `anchor = pawn origin square`
- `route = pawn capture`

PawnCapture-2 forbidden writer output:

- `Scene.Material` claim
- `PassedPawnCreated` claim
- open-file meaning
- weakness meaning
- strategy meaning

Writer boundary:

- `ScenePawnCapture` may create only the pawn-captures-pawn event Story.
- `ScenePawnCapture` must not populate `CaptureResult`.
- `ScenePawnCapture` must not create material gain, passed-pawn, open-file, weakness, or strategy meaning.
- `EngineCheck` may attach only to an existing same-board `ScenePawnCapture` Story and must not create the Story.
- Refuting `EngineCheck` evidence blocks attachment and public lead eligibility.

Completion standard: PawnCapture-2 closes when `ScenePawnCapture` alone writes narrow `Scene.PawnCapture`, the Story identity is exactly scene PawnCapture with no tactic or plan, side/rival/target/anchor/route are bound to the PawnCaptureProof tuple, complete StoryProof and PawnCaptureProof are required, EngineCheck Refutes cannot attach, and Scene.Material, PassedPawnCreated, open-file, weakness, and strategy meanings remain closed to this writer.

### PawnCapture-3 Negative Corpus

A pawn capture is not a material or structure claim. Complete PawnCaptureProof or silence.

PawnCapture-3 must close:

- legal move 아님
- same-board proof 없음
- moving piece가 pawn이 아님
- capture가 아님
- captured piece가 pawn이 아님
- captured piece가 rival side가 아님
- en passant
- promotion capture
- material gain을 PawnCapture claim으로 말하려 함
- passed pawn created / open file / weakness wording 유입

Negative corpus boundary:

- incomplete `PawnCaptureProof` means `ScenePawnCapture` stays silent.
- material result stays in `CaptureResult` and `Scene.Material`, not `Scene.PawnCapture`.
- passed-pawn, open-file, weakness, and structure wording stay forbidden to `Scene.PawnCapture`.
- en passant and promotion capture remain outside the ordinary diagonal pawn-capture scope.

Completion standard: PawnCapture-3 closes when every negative corpus row has incomplete `PawnCaptureProof` or blocked `Scene.PawnCapture` output, material gain remains outside PawnCapture claim ownership, passed pawn created, open file, and weakness wording remain forbidden, and the only positive path remains a complete `PawnCaptureProof` for one ordinary legal pawn capture of a rival pawn on the same board.

### PawnCapture-4 EngineCheck Reuse

PawnCapture-4 reuses the existing `EngineCheck` sidecar only.

PawnCapture-4 rules:

- `EngineCheck` cannot create `Scene.PawnCapture`.
- `Supports` creates no new claim.
- `Caps` suppresses the standalone `captures_rival_pawn` claim or weakens it to bounded relation only.
- `Refutes` blocks the existing `Scene.PawnCapture` Story.
- `Unknown` creates no engine expression.

PawnCapture-4 forbids:

- engine says
- eval number
- best move
- only move
- wins pawn
- structural advantage

EngineCheck boundary:

- `EngineCheck` may attach only after complete `StoryProof`, complete `PawnCaptureProof`, and same-board legal replay already produced a `ScenePawnCapture` Story.
- `EngineCheck` may support, cap, or refute that existing Story only.
- `EngineCheck` must not populate `CaptureResult`, material meaning, passed-pawn meaning, open-file meaning, weakness meaning, or strategy meaning.
- `Supports` and `Unknown` do not change the `captures_rival_pawn` wording.
- `Caps` prevents standalone pawn-capture speech unless a bounded relation path is explicitly opened by live law.
- `Refutes` keeps the diagnostic status internal and blocks public lead eligibility.

Completion standard: PawnCapture-4 closes when `EngineCheck` can attach only to an existing same-board `ScenePawnCapture` Story route, cannot create PawnCapture, cannot add a claim under Supports, suppresses or bounds capped rows, blocks refuted rows, keeps Unknown engine-silent, and exposes no engine-says, eval-number, best-move, only-move, wins-pawn, or structural-advantage wording.

### PawnCapture-5 StoryTable Integration

PawnCapture-5 integrates `Scene.PawnCapture` with existing StoryTable rows without opening material or structure meaning.

PawnCapture-5 collision targets:

- `Scene.PawnBreak`
- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- `Scene.Material`
- `Tactic.Hanging`
- `Scene.Defense`
- line tactics
- `Scene.PawnCapture`

PawnCapture-5 verification:

- input order must be stable
- `Scene.PawnCapture` must not own Material claim
- `Scene.PawnCapture` must not create PassedPawnCreated claim
- `Scene.PawnCapture` must not create FileOpens or weakness meaning
- actual material change now stays in `Scene.Material`
- pawn contact/challenge stays in `Scene.PawnBreak`
- capped/refuted `Scene.PawnCapture` has no standalone text

StoryTable boundary:

- `Scene.PawnCapture` may lead only when no higher-priority existing claim home owns the same selection space.
- `Scene.PawnCapture` support rows do not lower to `captures_rival_pawn` standalone text.
- Existing Material, PawnBreak, PawnAdvance, PawnStop, PromotionThreat, Promotion, Hanging, Defense, and line-tactic rows keep their own claim keys.
- Multiple valid `Scene.PawnCapture` rows order deterministically by the existing StoryTable tie-breakers.
- StoryTable must not turn `captures_rival_pawn` into material gain, passed-pawn creation, file-opening, weakness, strategy, best-move, or only-move meaning.

Completion standard: PawnCapture-5 closes when StoryTable ordering is input-order stable across PawnCapture collisions, existing claim homes keep lead ownership over Material, PawnBreak, PawnAdvance, PawnStop, PromotionThreat, Promotion, Hanging, Defense, and line tactics, same-family PawnCapture rows order deterministically, PawnCapture does not own material, passed-pawn, file-opening, or weakness meaning, and capped/refuted PawnCapture rows produce no standalone text.

### PawnCapture-6 ExplanationPlan

PawnCapture-6 lowers only selected uncapped `Lead` Verdicts for `Scene.PawnCapture`.

PawnCapture-6 allowed claim key:

- `captures_rival_pawn`

PawnCapture-6 forbidden claim keys:

- `wins_pawn`
- `wins_material`
- `creates_passed_pawn`
- `opens_file`
- `weakens_structure`
- `breaks_through`
- `creates_pressure`
- `takes_initiative`
- `best_move`
- `only_move`
- `forced`

Support, Context, Blocked, capped, and refuted `Scene.PawnCapture` rows have no standalone claim.

ExplanationPlan boundary:

- input is only the selected `Verdict`
- `Scene.PawnCapture` must be selected, uncapped, `Lead`, and `leadAllowed`
- `Scene.PawnCapture` must keep `tactic = None`, `plan = None`, writer = `ScenePawnCapture`, and no secondary target
- `captures_rival_pawn` speaks only that a pawn captured a rival pawn
- `captures_rival_pawn` must not be interpreted as material gain, passed-pawn creation, file-opening, weakness, breakthrough, pressure, initiative, best-move, only-move, or forced meaning

Completion standard: PawnCapture-6 closes when only selected uncapped Lead PawnCapture Verdicts lower to ExplanationPlan with `captures_rival_pawn`; forbidden keys remain closed; Support, Context, Blocked, capped, and refuted PawnCapture rows produce no standalone ExplanationPlan claim; and renderer, LLM, production API, public route, material gain, passed-pawn, file-opening, weakness, breakthrough, pressure, initiative, best-move, only-move, and forced speech remain closed.

### PawnCapture-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

PawnCapture-7 allowed deterministic wording:

- `{route} captures the pawn on {target}.`

PawnCapture-7 forbidden renderer wording:

- `wins a pawn`
- `wins material`
- `creates a passed pawn`
- `opens the file`
- `weakens the structure`
- `breaks through`
- `takes the initiative`
- `best move`
- `only move`
- `forces`

Renderer boundary:

- `DeterministicRenderer` must not read raw `Story`, `PawnCaptureProof`, `BoardFacts`, `EngineCheck`, source rows, raw PV, or proof failures.
- `DeterministicRenderer` must reject Support, Context, Blocked, capped, refuted, no-claim, wrong-claim, wrong-scene, tactic-tagged, missing-route, missing-SAN, missing-target, missing-anchor, and secondary-target PawnCapture plans.
- Renderer output must keep `claimKey = captures_rival_pawn` and `strength = bounded`.
- Renderer output must not create material gain, passed-pawn, file-opening, weakness, breakthrough, initiative, best-move, only-move, or forcing meaning.

Completion standard: PawnCapture-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead PawnCapture as `{route} captures the pawn on {target}.`, rejects Support, Context, Blocked, capped, refuted, no-claim, wrong-claim, and malformed plans, and emits none of the forbidden wins-pawn, wins-material, passed-pawn, file-opening, weakness, breakthrough, initiative, best-move, only-move, or forcing wording.

### PawnCapture-8 LLM Smoke

PawnCapture-8 reuses only the existing 8B LLM smoke boundary.

PawnCapture-8 LLM input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

PawnCapture-8 forbidden LLM input:

- raw Story
- raw PawnCaptureProof
- CaptureResult
- BoardFacts
- EngineCheck
- raw PV
- proofFailures

PawnCapture-8 forbidden LLM output:

- new move
- new line
- material claim
- passed-pawn claim
- open-file claim
- weakness claim
- strategy claim

LLM smoke boundary:

- no new LLM behavior
- no public/user-facing LLM narration
- no production API
- no public route `200`
- output must stay no stronger than selected `RenderedLine`
- `captures_rival_pawn` remains only a pawn-captures-rival-pawn event, not material or structure meaning

Completion standard: PawnCapture-8 closes when LLM smoke receives only rendered text contract fields, reuses renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction, rejects raw Story, raw PawnCaptureProof, CaptureResult, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, and material, passed-pawn, open-file, weakness, or strategy claims, and adds no new LLM behavior, public LLM narration, production API surface, or public route 200.

### PawnCapture Closeout Hard Cleanup

`PawnCaptureProof`, `Scene.PawnCapture`, and `captures_rival_pawn` remain separate authority homes.

`PawnCaptureProof` proves only a legal ordinary pawn move capturing one rival pawn on the exact board.
`Scene.PawnCapture` owns only the public Story identity for the bounded pawn-captures-rival-pawn event.
`captures_rival_pawn` owns only bounded downstream speech for selected uncapped Lead Verdicts.

`Scene.PawnCapture` owns no PawnBreak, PawnAdvance, PawnStop, PromotionThreat, Promotion, or PassedPawnCreated meaning.
`Scene.PawnCapture` owns no Material, Hanging, Defense, DiscoveredAttack, Pin, RemoveGuard, or Skewer meaning.

`wins pawn`, `creates passed pawn`, `opens file`, `weakens structure`, `initiative`, and `pressure` remain forbidden wording only, not live PawnCapture authority.

PawnCapture duplicate checks:

- one chess meaning: one legal pawn move captures one rival pawn.
- one proof home: `PawnCaptureProof`.
- one Story label: `Scene.PawnCapture`.
- one speech key: `captures_rival_pawn`.
- one detailed live authority document: `StoryInteractionLaw.md`.

README, SSOT, Architecture, Contract, and Manifest may summarize PawnCapture only.
Renderer and LLM smoke wording must remain no stronger than `captures_rival_pawn`.
Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PawnCapture Closeout closes when PawnCaptureProof, Scene.PawnCapture, and captures_rival_pawn remain separate authority homes; PawnCapture owns no PawnBreak, PawnAdvance, PawnStop, PromotionThreat, Promotion, PassedPawnCreated, Material, Hanging, Defense, DiscoveredAttack, Pin, RemoveGuard, or Skewer meaning; wins-pawn, creates-passed-pawn, open-file, weakens-structure, initiative, and pressure remain forbidden wording only; one chess meaning, one proof home, one Story label, one speech key, and one detailed live authority document remain true; summary docs stay summaries only; renderer and LLM smoke remain no stronger than captures_rival_pawn; and public route `200`, production API, and public/user-facing LLM narration remain closed.

### PassedPawnCreated-0 Charter

PassedPawnCreated-0 opens the third Pawn Structure / Break Neighborhood vertical slice.

First positive scope is not broad passed-pawn strategy.

Core sentence:

PassedPawnCreatedProof proves the before/after passed-pawn change. Scene.PassedPawnCreated may speak only the newly-created passer, not promotion or conversion.

PassedPawnCreated-0 opens only:

- narrow `Scene.PassedPawnCreated`
- `PassedPawnCreatedProof`
- a legal move
- before-board where the named pawn is not a passed pawn
- exact after-board where the named pawn is a passed pawn
- exactly one newly-created passed pawn
- selected Verdict to bounded created-passer wording
- `creates_passed_pawn`

PassedPawnCreated-0 must stay silent for:

- illegal move
- non-pawn move
- missing same-board proof
- pawn already passed before the move
- exact after-board where the moved pawn is not passed
- more than one newly-created passed pawn
- promotion move
- missing or incomplete `StoryProof`
- missing or incomplete `PassedPawnCreatedProof`
- writerless or contaminated rows

PassedPawnCreated-0 does not open:

- unstoppable passer
- promotion threat
- actual promotion
- winning endgame
- conversion
- pawn race
- tablebase
- breakthrough
- initiative / pressure
- best move / only move
- public route `200`
- production API
- public/user-facing LLM narration

Authority split:

- `PassedPawnCreatedProof` proves only the before/after passed-pawn change on the exact board.
- `ScenePassedPawnCreated` is the only writer that may create `Scene.PassedPawnCreated`.
- `Scene.PassedPawnCreated` owns only the bounded newly-created passer Story identity.
- `ExplanationPlan` may lower only selected uncapped Lead `Scene.PassedPawnCreated` Verdicts to `creates_passed_pawn`.
- Renderer and LLM smoke may phrase only bounded created-passer wording.

Completion standard: PassedPawnCreated-0 closes when one legal non-promotion pawn move that changes exactly one moved pawn from not-passed before to passed on the exact after-board can become narrow `Scene.PassedPawnCreated`; illegal moves, non-pawn moves, already-passed pawns, after-board non-passers, multiple newly-created passers, promotion moves, missing exact-board proof, incomplete StoryProof, missing or incomplete `PassedPawnCreatedProof`, writerless rows, and contaminated rows stay silent; and unstoppable passer, promotion threat, actual promotion, winning endgame, conversion, pawn race, tablebase, breakthrough, initiative, pressure, best/only, public route `200`, production API, and public/user-facing LLM narration remain closed.

### PassedPawnCreated-1 PassedPawnCreatedProof

PassedPawnCreated-1 makes `PassedPawnCreatedProof` the proof home for the current
newly-created passer slice.

`PassedPawnCreatedProof` proves:

- creating side
- rival side
- created passed pawn identity
- origin square if moved
- after square
- legal creating move
- exact before-board
- exact after-board replay
- pawn was not passed before
- pawn is passed after
- same-board proof

First allowed creation routes:

- ordinary pawn move
- ordinary pawn capture, if the exact after-board creates the passed pawn

Closed creation routes:

- en passant
- promotion
- multi-move sequence
- pawn race result
- tablebase result
- "unstoppable" proof

`PassedPawnCreatedProof` is not a public `Story`.

`PassedPawnObservation` is not a public `Story`.

Completion standard: PassedPawnCreated-1 closes when `PassedPawnCreatedProof`
alone proves the creating side, rival side, created passed pawn identity, origin
square, after square, legal creating move, exact before-board, exact after-board
replay, not-passed-before state, passed-after state, and same-board proof for
one ordinary pawn move or ordinary pawn capture; en passant, promotion,
multi-move sequences, pawn race results, tablebase results, and unstoppable
proof stay closed; and neither `PassedPawnCreatedProof` nor
`PassedPawnObservation` becomes a public `Story`.

### PassedPawnCreated-2 Scene.PassedPawnCreated Writer

Named writer: `ScenePassedPawnCreated`.

PassedPawnCreated-2 writer conditions:

- complete `StoryProof`
- complete `PassedPawnCreatedProof`
- same-board legal replay
- legal creating move
- before-not-passed and after-passed proof
- writer is `ScenePassedPawnCreated`
- `EngineCheck` does not Refute

PassedPawnCreated-2 Story identity:

- `scene = Scene.PassedPawnCreated`
- `tactic = None`
- `plan = None`
- `side = creating side`
- `rival = rival side`
- `target = after square of created passed pawn`
- `anchor = origin square or creating move origin`
- `route = creating move`

PassedPawnCreated-2 forbidden writer ownership:

- `ScenePassedPawnCreated` must not own `Scene.PawnAdvance` meaning.
- `ScenePassedPawnCreated` must not own `Scene.PawnCapture` meaning.
- `ScenePassedPawnCreated` must not create `PromotionThreat`, `Promotion`, or `Conversion` meaning.

Completion standard: PassedPawnCreated-2 closes when `ScenePassedPawnCreated` alone writes narrow `Scene.PassedPawnCreated`, the Story identity is exactly scene PassedPawnCreated with no tactic or plan, side/rival/target/anchor/route are bound to the `PassedPawnCreatedProof` tuple, complete `StoryProof` and `PassedPawnCreatedProof` are required, `EngineCheck` Refutes cannot lead, and `Scene.PawnAdvance`, `Scene.PawnCapture`, `PromotionThreat`, `Promotion`, and `Conversion` meanings remain closed to this writer.

### PassedPawnCreated-3 Negative Corpus

PassedPawnCreated-3 closes the negative corpus for the newly-created passer slice.

Criterion:

A pawn move is not passer creation. Exact before/after passed-pawn proof or silence.

PassedPawnCreated-3 must stay silent for:

- legal move missing
- same-board proof missing
- exact after-board where the moved pawn is not passed
- before-board where the pawn was already passed
- promotion move
- en passant
- two-move passer creation
- pawn capture event exaggerated as `Scene.PassedPawnCreated`
- promotion threat wording
- unstoppable wording
- winning wording

Negative corpus ownership:

- `ScenePawnCapture` may own the bounded pawn-capture event when a pawn captures a rival pawn.
- `ScenePassedPawnCreated` may not upgrade that capture into passed-pawn creation without exact before/after passer proof.
- `ScenePassedPawnCreated` may not infer promotion-threat, unstoppable-passer, winning-endgame, conversion, pawn-race, or tablebase meaning.

Completion standard: PassedPawnCreated-3 closes when illegal moves, missing same-board proof, after-board non-passers, already-passed-before pawns, promotion moves, en passant, two-move passer creation, capture-only pawn events, and promotion-threat, unstoppable, or winning wording all produce incomplete `PassedPawnCreatedProof`, no `Scene.PassedPawnCreated`, or rejected downstream text; and the only positive path remains exact before/after proof that one legal ordinary pawn move or capture creates one newly passed pawn.

### PassedPawnCreated-4 EngineCheck Reuse

PassedPawnCreated-4 reuses only the existing `EngineCheck` sidecar.

EngineCheck rules for `Scene.PassedPawnCreated`:

- `EngineCheck` cannot create `Scene.PassedPawnCreated`.
- `Supports` creates no new claim.
- `Caps` suppresses standalone `Scene.PassedPawnCreated` claim output or weakens it to bounded relation only.
- `Refutes` makes the `Scene.PassedPawnCreated` Story `Blocked`.
- `Unknown` creates no engine expression.

PassedPawnCreated-4 forbidden engine wording:

- `engine says`
- eval number
- best move
- only move
- winning endgame
- tablebase result
- pawn race result

Completion standard: PassedPawnCreated-4 closes when `EngineCheck` can attach only to an already complete `ScenePassedPawnCreated` Story, `Supports` leaves the bounded created-passer claim unchanged, `Caps` suppresses standalone claim output or weakens to bounded relation only, `Refutes` blocks the Story, `Unknown` produces no engine expression, and engine-says, eval-number, best-move, only-move, winning-endgame, tablebase-result, and pawn-race-result wording remain rejected.

### PassedPawnCreated-5 StoryTable Integration

PassedPawnCreated-5 collides `Scene.PassedPawnCreated` with existing StoryTable rows without opening promotion, conversion, or capture-event ownership.

PassedPawnCreated-5 collision targets:

- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- `Scene.Material`
- `Scene.Defense`
- `Tactic.Hanging`
- Line / Defender tactics
- `Scene.PassedPawnCreated`

PassedPawnCreated-5 verification:

- input order stability
- `Scene.PassedPawnCreated` does not own the `Scene.PawnCapture` event
- `Scene.PassedPawnCreated` does not own `Scene.PawnAdvance` meaning
- `Scene.PassedPawnCreated` does not create `Scene.PromotionThreat` or `Scene.Promotion` meaning
- actual material change now remains in `Scene.Material`
- capped or refuted `Scene.PassedPawnCreated` has no standalone text

StoryTable boundary:

- `Scene.PassedPawnCreated` may lead only when no higher-priority existing claim home owns the same selection space.
- Support `Scene.PassedPawnCreated` rows do not lower to `creates_passed_pawn` standalone text.
- Existing PawnBreak, PawnCapture, PawnAdvance, PawnStop, PromotionThreat, Promotion, Material, Defense, Hanging, and line/defender rows keep their own claim keys.
- Multiple valid `Scene.PassedPawnCreated` rows order deterministically by existing StoryTable tie-breakers.
- StoryTable must not turn `creates_passed_pawn` into pawn-capture, pawn-advance, promotion-threat, actual-promotion, material, conversion, winning, tablebase, pawn-race, best-move, or only-move meaning.

Completion standard: PassedPawnCreated-5 closes when StoryTable ordering is input-order stable across PassedPawnCreated collisions, existing claim homes keep lead ownership over PawnBreak, PawnCapture, PawnAdvance, PawnStop, PromotionThreat, Promotion, Material, Defense, Hanging, and line/defender tactics, same-family PassedPawnCreated rows order deterministically, PassedPawnCreated does not own capture-event, advance, promotion-threat, promotion, or material-now meaning, and capped/refuted PassedPawnCreated rows produce no standalone text.

### PassedPawnCreated-6 ExplanationPlan

PassedPawnCreated-6 lets `ExplanationPlan` accept only selected uncapped Lead `Scene.PassedPawnCreated` Verdicts.

PassedPawnCreated-6 allowed claim key:

- `creates_passed_pawn`

PassedPawnCreated-6 forbidden claim keys:

- `unstoppable_pawn`
- `promotion_threat`
- `will_promote`
- `wins_endgame`
- `converts_advantage`
- `breaks_through`
- `creates_pressure`
- `takes_initiative`
- `best_move`
- `only_move`
- `forced`

Support, Context, Blocked, capped, and refuted `Scene.PassedPawnCreated` rows create no standalone claim.

ExplanationPlan boundary:

- `creates_passed_pawn` is the only bounded claim key opened by this slice.
- `Scene.PassedPawnCreated` cannot lower promotion-threat, will-promote, unstoppable-pawn, winning-endgame, conversion, breakthrough, pressure, initiative, best-move, only-move, or forced meaning.
- `ExplanationPlan` must not repair incomplete proof, convert Support or Context into standalone text, or make capped/refuted rows speak.

Completion standard: PassedPawnCreated-6 closes when only selected uncapped Lead PassedPawnCreated Verdicts lower to `creates_passed_pawn`, every listed forbidden claim key remains unavailable, and Support, Context, Blocked, capped, and refuted PassedPawnCreated rows produce no standalone claim.

### PassedPawnCreated-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

PassedPawnCreated-7 allowed deterministic wording:

- `{route} creates a passed pawn on {target}.`

PassedPawnCreated-7 forbidden renderer wording:

- `unstoppable`
- `will promote`
- `wins`
- `winning endgame`
- `converts`
- `breaks through`
- `takes the initiative`
- `creates pressure`
- `best move`
- `only move`
- `forces`

Renderer boundary:

- `DeterministicRenderer` must not read `Story`, `Verdict`, `PassedPawnCreatedProof`, `BoardFacts`, `EngineCheck`, `proofFailures`, or source rows.
- The renderer may use only the selected `ExplanationPlan` route SAN, target, allowed claim key, strength, and forbidden wording.
- Support, Context, Blocked, capped, refuted, no-claim, and malformed PassedPawnCreated plans must produce no deterministic text.

Completion standard: PassedPawnCreated-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead PassedPawnCreated as `{route} creates a passed pawn on {target}.`, rejects Support, Context, Blocked, capped, refuted, no-claim, and malformed plans, and emits none of the forbidden unstoppable, promotion, winning, conversion, breakthrough, pressure, initiative, best-move, only-move, or forced wording.

### PassedPawnCreated-8 LLM Smoke

PassedPawnCreated-8 reuses only the existing 8B LLM smoke boundary.

PassedPawnCreated-8 LLM smoke input:

- `renderedText`
- `claimKey`
- `strength`
- forbidden wording
- `Rephrase only. Do not add chess facts.`

PassedPawnCreated-8 forbidden LLM smoke inputs:

- raw `Story`
- raw `PassedPawnCreatedProof`
- `PassedPawnObservation`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`

PassedPawnCreated-8 forbidden LLM smoke additions:

- new move
- new line
- promotion claim
- unstoppable claim
- winning claim
- conversion claim
- pressure claim

LLM smoke boundary:

- LLM smoke may check only selected PassedPawnCreated `RenderedLine` data and the bounded `ExplanationPlan` forbidden wording.
- LLM smoke must not read `Story`, `PassedPawnCreatedProof`, `PassedPawnObservation`, `BoardFacts`, `EngineCheck`, raw PV, `proofFailures`, source rows, or public route payloads.
- LLM smoke may not add promotion, unstoppable, winning, conversion, pressure, initiative, best-move, only-move, forced, new-move, or new-line meaning.

Completion standard: PassedPawnCreated-8 closes when LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for selected PassedPawnCreated RenderedLine; it rejects raw Story, raw PassedPawnCreatedProof, PassedPawnObservation, BoardFacts, EngineCheck, raw PV, proofFailures, new moves, new lines, and promotion, unstoppable, winning, conversion, or pressure claims.

### PassedPawnCreated Closeout Hard Cleanup

`PassedPawnObservation`, `PassedPawnCreatedProof`, `Scene.PassedPawnCreated`, and `creates_passed_pawn` remain separate authority homes.

`PassedPawnObservation` observes same-board passed-pawn structure only.
`PassedPawnCreatedProof` proves only the before/after passed-pawn change for one legal move on the exact board.
`Scene.PassedPawnCreated` owns only the public Story identity for the bounded newly-created passer event.
`creates_passed_pawn` owns only bounded downstream speech for selected uncapped Lead Verdicts.

`Scene.PassedPawnCreated` owns no PawnBreak, PawnCapture, PawnAdvance, PawnStop, PromotionThreat, or Promotion meaning.
`Scene.PassedPawnCreated` owns no Material, Hanging, Defense, DiscoveredAttack, Pin, RemoveGuard, or Skewer meaning.

`unstoppable`, `promotion threat`, `will promote`, `conversion`, `winning`, `breakthrough`, `initiative`, and `pressure` remain forbidden wording only, not live PassedPawnCreated authority.

PassedPawnCreated duplicate checks:

- one chess meaning: one legal move creates one newly passed pawn on the exact after-board.
- one proof home: `PassedPawnCreatedProof`.
- one Story label: `Scene.PassedPawnCreated`.
- one speech key: `creates_passed_pawn`.
- one detailed live authority document: `StoryInteractionLaw.md`.

README, SSOT, Architecture, Contract, and Manifest may summarize PassedPawnCreated only.
Renderer and LLM smoke wording must remain no stronger than `creates_passed_pawn`.
Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PassedPawnCreated Closeout closes when PassedPawnObservation, PassedPawnCreatedProof, Scene.PassedPawnCreated, and creates_passed_pawn remain separate authority homes; PassedPawnCreated owns no PawnBreak, PawnCapture, PawnAdvance, PawnStop, PromotionThreat, Promotion, Material, Hanging, Defense, or Line / Defender tactic meaning; unstoppable, promotion-threat, will-promote, conversion, winning, breakthrough, initiative, and pressure wording remains forbidden only; one chess meaning, one proof home, one Story label, one speech key, and one detailed live authority document remain true; summary docs stay summaries only; renderer and LLM smoke remain no stronger than creates_passed_pawn; and public route `200`, production API, and public/user-facing LLM narration remain closed.

### PSBNC-0 Pawn Structure / Break Neighborhood Closeout Charter

PSBNC-0 closes the Pawn Structure / Break Neighborhood as hard cleanup and
interaction audit only.

Core sentence:

Pawn Structure / Break Neighborhood closes as three narrow proof-backed event slices. PSBNC opens no new chess meaning.

PSBNC-0 closes only already-open narrow meanings:

- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PassedPawnCreated`

PSBNC-0 opens no new:

- Story label
- proof home
- Story writer
- claim key
- renderer wording
- LLM behavior
- public route `200`
- production API
- public/user-facing LLM narration

Authority separation:

- `BoardFacts.PawnLever`, `PawnBreakProof`, `Scene.PawnBreak`, and `challenges_pawn` remain separate authority homes.
- `PawnCaptureProof`, `Scene.PawnCapture`, and `captures_rival_pawn` remain separate authority homes.
- `PassedPawnObservation`, `PassedPawnCreatedProof`, `Scene.PassedPawnCreated`, and `creates_passed_pawn` remain separate authority homes.

Interaction audit:

- The three Story labels must not steal each other's claims.
- A pawn-captures-rival-pawn event does not automatically own passed-pawn creation.
- A newly-created passer event does not own the pawn-capture event.
- Pawn contact/challenge stays in the PawnBreak home.
- Pawn-captures-rival-pawn event stays in the PawnCapture home.
- Newly-created passer event stays in the PassedPawnCreated home.
- CaptureResult and `Scene.Material` keep material meaning.
- PromotionThreat and Promotion keep promotion meaning.
- PawnAdvance and PawnStop keep passed-pawn advance and stop meaning.

Duplication audit:

- one direct pawn-contact meaning: `PawnBreakProof` -> `Scene.PawnBreak` -> `challenges_pawn`.
- one pawn-captures-rival-pawn meaning: `PawnCaptureProof` -> `Scene.PawnCapture` -> `captures_rival_pawn`.
- one newly-created passer meaning: `PassedPawnCreatedProof` -> `Scene.PassedPawnCreated` -> `creates_passed_pawn`.
- one detailed live authority document: `StoryInteractionLaw.md`.

Renderer and LLM smoke remain no stronger than the selected bounded claim key.
Summary documents may summarize PSBNC-0 only and must not duplicate this
interaction audit.

Completion standard: PSBNC-0 closes when PawnBreakProof / Scene.PawnBreak / challenges_pawn, PawnCaptureProof / Scene.PawnCapture / captures_rival_pawn, and PassedPawnObservation / PassedPawnCreatedProof / Scene.PassedPawnCreated / creates_passed_pawn remain separate; the three Story labels do not steal each other's claims; capture events do not automatically own passed-pawn creation; passed-pawn creation does not own capture events; pawn contact/challenge stays in the PawnBreak home; pawn-captures-rival-pawn event stays in the PawnCapture home; newly-created passer event stays in the PassedPawnCreated home; no new Story, proof home, writer, claim key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration opens; summary docs stay summaries only; and `git diff --check`, targeted commentary tests, and docs authority tests are clean.

### PSBNC-1 Scope Audit

PSBNC-1 audits the open scope for the Pawn Structure / Break Neighborhood.

Core sentence:

Three pawn-structure event slices are open. Broad pawn-structure interpretation remains closed.

PSBNC-1 open positive Stories are exactly:

- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PassedPawnCreated`

PSBNC-1 does not open:

- broad PawnTactic
- broad pawn structure advantage
- passed pawn strategy
- breakthrough
- open file
- weak square / weakness
- pawn majority change
- wins space
- wins pawn
- wins material
- promotion threat
- unstoppable pawn
- conversion
- initiative
- pressure
- best move / only move / forced
- public route `200`
- production API
- public/user-facing LLM narration

Scope audit rules:

- `Scene.PawnBreak` may speak only bounded direct pawn contact through `challenges_pawn`.
- `Scene.PawnCapture` may speak only bounded pawn-captures-rival-pawn event through `captures_rival_pawn`.
- `Scene.PassedPawnCreated` may speak only bounded newly-created passer event through `creates_passed_pawn`.
- No PSBNC row may classify itself as broad PawnTactic, pawn-structure advantage, strategy, breakthrough, open-file, weakness, pawn-majority, space-gain, material-gain, promotion-threat, unstoppable, conversion, initiative, pressure, best, only, or forced meaning.
- Broad pawn-structure interpretation remains closed even when the same move also has capture, material, promotion, or passed-pawn observations.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PSBNC-1 closes when the only open Pawn Structure / Break Neighborhood positive Stories are Scene.PawnBreak, Scene.PawnCapture, and Scene.PassedPawnCreated; broad PawnTactic, broad pawn structure advantage, passed pawn strategy, breakthrough, open file, weak square / weakness, pawn majority change, wins space, wins pawn, wins material, promotion threat, unstoppable pawn, conversion, initiative, pressure, best move, only move, forced, public route `200`, production API, and public/user-facing LLM narration remain closed; and summary docs do not duplicate this detailed scope audit.

### PSBNC-2 Duplication Audit

PSBNC-2 audits naming and ownership duplication for the three open Pawn
Structure / Break Neighborhood event meanings.

PSBNC-2 duplication audit:

- one chess meaning, one proof home
- one Story label per public chess meaning
- one speech key per allowed public claim
- one detailed live authority document

Mapping:

direct rival-pawn lever/contact:

- proof home = `PawnBreakProof`
- Story label = `Scene.PawnBreak`
- speech key = `challenges_pawn`

pawn captures rival pawn:

- proof home = `PawnCaptureProof`
- Story label = `Scene.PawnCapture`
- speech key = `captures_rival_pawn`

newly-created passed pawn:

- observation = `PassedPawnObservation`
- proof home = `PassedPawnCreatedProof`
- Story label = `Scene.PassedPawnCreated`
- speech key = `creates_passed_pawn`

Forbidden duplication:

- `PawnBreakProof` must not act as capture proof.
- `PawnCaptureProof` must not act as material or passed-pawn proof.
- `PassedPawnCreatedProof` must not act as capture or pawn-break proof.
- Speech keys must not duplicate.
- No helper, source row, proof score, renderer wording, or LLM wording may become a second proof home, Story label, or speech key for these meanings.

Boundary notes:

- `PassedPawnObservation` remains observation only, not proof home, Story label, or speech key.
- `CaptureResult` and `Scene.Material` remain the material proof and Story homes.
- `BoardFacts.PawnLever` remains an observation input below `PawnBreakProof`.
- `StoryInteractionLaw.md` remains the only detailed live authority document for PSBNC duplication law.

Completion standard: PSBNC-2 closes when direct rival-pawn lever/contact, pawn captures rival pawn, and newly-created passed pawn each have exactly one proof home, one Story label, and one speech key; PassedPawnObservation stays observation only for newly-created passer proof; PawnBreakProof does not become capture proof; PawnCaptureProof does not become material or passed-pawn proof; PassedPawnCreatedProof does not become capture or pawn-break proof; speech keys remain unique; and summary docs do not duplicate this detailed duplication audit.

### PSBNC-3 StoryTable Interaction Audit

PSBNC-3 collides the three Pawn Structure / Break Neighborhood rows with existing open rows.

PSBNC-3 target rows:

- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PassedPawnCreated`
- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- `Scene.Material`
- `Tactic.Hanging`
- `Scene.Defense`
- Line / Defender tactics

StoryTable interaction audit rules:

- input order must stay stable
- each claim home must stay in its own home
- `Scene.PawnCapture` must not own the `Scene.PassedPawnCreated` claim
- `Scene.PassedPawnCreated` must not own the `Scene.PawnCapture` event
- `Scene.PawnBreak` must not own capture or passed-pawn creation meaning
- actual material change now remains `Scene.Material` home
- promotion-next meaning remains `Scene.PromotionThreat` home
- actual promotion meaning remains `Scene.Promotion` home
- only the selected uncapped Lead may carry an allowed claim into `ExplanationPlan`
- Support, Context, Blocked, capped, and refuted rows must have no standalone text

Interaction notes:

- A pawn capture that also creates a passer may produce both rows, but the
  capture event remains `Scene.PawnCapture` and the created-passer event remains
  `Scene.PassedPawnCreated`.
- A direct rival-pawn contact row may coexist with capture, passer creation,
  material, pawn-advance, pawn-stop, promotion, defense, hanging, and
  line/defender rows, but it keeps only `challenges_pawn`.
- `Scene.Material` remains the home for actual material change now, including
  pawn captures that have a bounded `CaptureResult`.
- `Scene.PromotionThreat` remains the home for next-move promotion meaning.
- `Scene.Promotion` remains the home for actual promotion meaning.
- EngineCheck Supports, Caps, and Refutes may support, cap, or block an already
  existing row only; they do not create a second StoryTable claim home.

Completion standard: PSBNC-3 closes when StoryTable order is stable across input order; Scene.PawnBreak, Scene.PawnCapture, and Scene.PassedPawnCreated keep their claim homes against Scene.PawnAdvance, Scene.PawnStop, Scene.PromotionThreat, Scene.Promotion, Scene.Material, Tactic.Hanging, Scene.Defense, and Line / Defender tactics; PawnCapture never owns the passed-pawn-created claim; PassedPawnCreated never owns the pawn-capture event; PawnBreak never owns capture or passer creation meaning; actual material change now remains Scene.Material home; promotion-next meaning remains Scene.PromotionThreat home; actual promotion meaning remains Scene.Promotion home; only the selected uncapped Lead may carry an allowed claim into ExplanationPlan; and Support, Context, Blocked, capped, and refuted rows produce no standalone text.

### PSBNC-4 Downstream Boundary Audit

PSBNC-4 audits only the downstream boundaries for the three Pawn Structure / Break Neighborhood slices.

PSBNC-4 downstream targets:

- `ExplanationPlan`
- `DeterministicRenderer`
- LLM smoke

PSBNC-4 boundary rules:

- `ExplanationPlan` input is only the selected uncapped Lead `Verdict`
- `DeterministicRenderer` input is `ExplanationPlan` only
- LLM smoke input reuses only the existing 8B boundary

PSBNC-4 allowed claim keys:

- `challenges_pawn`
- `captures_rival_pawn`
- `creates_passed_pawn`

PSBNC-4 forbidden claim keys:

- `opens_file`
- `weakens_structure`
- `breaks_through`
- `wins_space`
- `wins_pawn`
- `wins_material`
- `promotion_threat`
- `unstoppable_pawn`
- `converts_advantage`
- `creates_pressure`
- `takes_initiative`
- `best_move`
- `only_move`
- `forced`

Downstream notes:

- `Scene.PawnBreak` may lower only `challenges_pawn`.
- `Scene.PawnCapture` may lower only `captures_rival_pawn`.
- `Scene.PassedPawnCreated` may lower only `creates_passed_pawn`.
- The renderer must not infer open-file, weakness, breakthrough, space,
  material, promotion, unstoppable-pawn, conversion, pressure, initiative,
  best-move, only-move, or forced meaning from any of the three selected claims.
- LLM smoke receives only renderedText, claimKey, strength, forbidden wording,
  and `Rephrase only. Do not add chess facts.` from the existing 8B boundary.
- Raw Story, Proof, BoardFacts, EngineCheck, raw PV, proofFailures, source rows,
  and diagnostics remain outside renderer and LLM smoke input.
- Renderer and LLM smoke wording must remain no stronger than the selected claim.

Completion standard: PSBNC-4 closes when ExplanationPlan receives only selected uncapped Lead Verdicts for Scene.PawnBreak, Scene.PawnCapture, and Scene.PassedPawnCreated; DeterministicRenderer receives only ExplanationPlan; LLM smoke reuses only the existing 8B boundary; the only allowed claim keys are challenges_pawn, captures_rival_pawn, and creates_passed_pawn; opens_file, weakens_structure, breaks_through, wins_space, wins_pawn, wins_material, promotion_threat, unstoppable_pawn, converts_advantage, creates_pressure, takes_initiative, best_move, only_move, and forced remain forbidden as claim keys; renderer and LLM smoke text stays no stronger than each selected claim; and summary docs do not duplicate this detailed downstream boundary audit.

### PSBNC-5 Forbidden Wording Audit

PSBNC-5 audits forbidden wording for the three Pawn Structure / Break Neighborhood slices.

PSBNC-5 forbidden live-authority wording:

- opens file
- opens position
- weakens structure
- breakthrough
- breaks through
- wins space
- wins pawn
- wins material
- creates passed pawn, unless selected `Scene.PassedPawnCreated`
- promotion threat, unless selected `Scene.PromotionThreat`
- unstoppable
- conversion
- initiative
- pressure
- best move
- only move
- forced

Forbidden wording may exist only as forbidden wording, negative corpus, or docs saying it remains closed.
It must not become proof home, Story label, claim key, renderer output, LLM-added fact, public value, or public route payload.

Audit notes:

- `Scene.PawnBreak` renderer and LLM smoke may say only bounded direct pawn
  challenge/contact wording; it must not add file, position-opening, weakness,
  breakthrough, space, pawn-win, material-win, promotion, unstoppable,
  conversion, initiative, pressure, best-move, only-move, or forced claims.
- `Scene.PawnCapture` renderer and LLM smoke may say only bounded rival-pawn
  capture wording; it must not add material, passed-pawn creation, file,
  weakness, breakthrough, initiative, pressure, best-move, only-move, or forced
  claims.
- `Scene.PassedPawnCreated` renderer and LLM smoke may say only bounded
  created-passer wording; it must not add capture ownership, promotion threat,
  unstoppable-pawn, conversion, breakthrough, material, initiative, pressure,
  best-move, only-move, or forced claims.
- `creates_passed_pawn` is allowed only as the selected `Scene.PassedPawnCreated`
  claim key and selected created-passer renderer/LLM wording.
- Promotion-threat wording is outside PSBNC and is allowed only when the selected
  home is `Scene.PromotionThreat`; PSBNC rows must not borrow it.
- Forbidden wording may appear in `ForbiddenWording`, negative corpus fixture
  descriptions, and closed-scope documentation only.
- Public route payloads remain closed; forbidden wording must not reach public
  route `200` JSON or user-facing narration.

Completion standard: PSBNC-5 closes when forbidden wording remains non-authoritative: it may exist only as forbidden wording, negative corpus, or docs saying it remains closed; it does not become a proof home, Story label, claim key, renderer output, LLM-added fact, public value, or public route payload for Scene.PawnBreak, Scene.PawnCapture, or Scene.PassedPawnCreated; creates-passed-pawn wording appears only for selected Scene.PassedPawnCreated; promotion-threat wording appears only for selected Scene.PromotionThreat and is not borrowed by PSBNC rows; and summary docs do not duplicate this detailed forbidden wording audit.

### PSBNC-6 Documentation Simplification Audit

PSBNC-6 documentation simplification audit:

- detailed Pawn Structure / Break Neighborhood closeout authority lives only in `StoryInteractionLaw.md`.
- `README.md`, `ChessCommentarySSOT.md`, `ChessModelArchitecture.md`, `ChessModelContract.md`, and `LegacyPruneManifest.md` remain summary-only.
- `AGENTS.md` keeps durable operator guardrails only.
- retired root docs must not return.
- legacy reset archive documents are historical reference only and not acceptance sources.

PSBNC-6 summary-only documents:

- `README.md`: non-authoritative pointer only.
- `ChessCommentarySSOT.md`: non-authoritative pointer only.
- `ChessModelArchitecture.md`: non-authoritative pointer only.
- `ChessModelContract.md`: non-authoritative pointer only.
- `LegacyPruneManifest.md`: pruning summary only, not detailed law.

PSBNC-6 forbidden documentation duplication:

- no PSBNC detailed checklist in summary docs.
- no closeout stage law in `AGENTS.md`.
- no detailed PSBNC law in `LegacyPruneManifest.md`.
- no retired root docs as live authority.
- no legacy reset archive acceptance source.

Completion standard: PSBNC-6 closes when detailed PSBNC closeout authority appears only in `StoryInteractionLaw.md`; summary docs keep only non-authoritative pointers; `AGENTS.md` remains durable guardrails only; retired root docs remain absent; and legacy reset archive documents remain historical reference only, not acceptance sources.

### PSBNC-7 Runtime Boundary Audit

PSBNC-7 runtime boundary audit:

- test helpers are not runtime authority.
- runtime helpers must not become new authority.
- fixture names are not Story family names.
- negative corpus helpers are not production concepts.
- forbidden wording checks must target public overclaim wording, not arbitrary internal field names.
- runtime source must not contain closeout-only terminology.

PSBNC-7 forbids new runtime authority:

- no new Story family.
- no new proof home.
- no new Story writer.
- no new claim key.
- no new renderer template.
- no new LLM behavior.
- no public route `200`.
- no production API.
- no public/user-facing LLM narration.

PSBNC-7 allowed closeout changes:

- tests may audit the boundary.
- documentation law may record the audit in `StoryInteractionLaw.md`.
- runtime code changes are allowed only if an audit exposes leakage or duplicated authority.

PSBNC-7 runtime source guard:

- runtime source must not contain `PSBNC-`.
- runtime source must not contain `closeout`.
- runtime source must not contain `Runtime Boundary Audit`.
- runtime source must not contain `Pawn Structure / Break Neighborhood Closeout`.
- runtime source must not contain `fixture map`.
- runtime source must not contain `negative corpus`.

Completion standard: PSBNC-7 closes when helper names remain test-local, runtime helpers do not become new authority, fixture names read only as fixtures, negative corpus helpers stay out of runtime, forbidden wording checks stay bounded to public overclaim wording, runtime source contains no closeout-only terms, and no new Story family, proof home, Story writer, claim key, renderer template, LLM behavior, public route `200`, production API, or public/user-facing LLM narration opens.

### PSBNC Final Completion Standard

PSBNC final completion standard:

- `Scene.PawnBreak`, `Scene.PawnCapture`, and `Scene.PassedPawnCreated` remain the only open Pawn Structure / Break Neighborhood positive slices.
- each slice has exactly one proof path, one Story label, and one speech key.
- no slice owns another slice's chess meaning.
- Material, PromotionThreat, Promotion, PawnAdvance, PawnStop, Hanging, Defense, and Line / Defender tactic homes keep their meanings.
- forbidden broad pawn-structure wording remains forbidden only.
- StoryTable ordering is input-order stable across neighborhood collisions.
- non-selected, capped, refuted, Support, Context, and Blocked rows produce no standalone downstream text.
- ExplanationPlan, Renderer, and LLM smoke remain bounded to selected Lead Verdict data.
- detailed authority lives only in `StoryInteractionLaw.md`.
- summary docs remain summaries only.
- public route `200`, production API, and public/user-facing LLM narration remain closed.

PSBNC final verification:

- targeted commentary runtime tests pass.
- docs authority tests pass.
- `git diff --check` passes.

Final PSBNC closeout opens no new Story family, proof home, Story writer, claim key, renderer template, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad PawnTactic, broad pawn-structure advantage, passed pawn strategy, breakthrough, open file, weak square, wins space, wins pawn, wins material, promotion threat, unstoppable pawn, conversion, initiative, pressure, best move, only move, forced, public route `200`, production API, or public/user-facing LLM narration.

## File Opened Neighborhood

### FileOpened-0 Charter

FileOpened-0 opens the narrow vertical slice immediately after the Pawn Structure / Break Neighborhood.

Core sentence:

FileOpenedProof proves only the exact open-file event. Scene.FileOpened may speak only that the origin file is now open, not pressure, rook activity, weakness, breakthrough, or strategy.

FileOpened-0 opens only:

- narrow `Scene.FileOpened`
- `FileOpenedProof`
- legal pawn move
- origin file was not open before because the moving pawn occupied it
- exact after-board has no pawns of either side on that file
- exact-board replay
- selected uncapped Lead Verdict lowering to `opens_file`
- deterministic wording no stronger than the origin file becoming open
- LLM smoke through the existing bounded rephrase-only contract

FileOpened-0 does not open:

- rook activity
- file control
- pressure
- initiative
- attack
- weak pawn
- weak square
- weakens structure
- breakthrough
- material gain
- passed pawn creation
- pawn majority change
- best move
- only move
- forced
- public route `200`
- production API
- public/user-facing LLM narration

FileOpenedProof proves:

- same-board proof is present
- the move is legal
- the moving piece is a pawn
- moving side and rival side
- moving pawn identity
- origin square and destination square
- origin file
- the pawn leaves its origin file
- the origin file was not open before because the moving pawn occupied it
- exact after-board replay is present
- no white pawn remains on the origin file after the move
- no black pawn remains on the origin file after the move
- the opened file is exactly the pawn origin file

FileOpenedProof does not prove:

- why opening the file matters
- file control
- rook activity
- pressure
- initiative
- attack
- weakness
- breakthrough
- material gain
- passed-pawn creation
- pawn-majority change
- best, only, or forced move status

Named writer: `SceneFileOpened`.

### FileOpened-1 Proof Home

FileOpened-1 pins `FileOpenedProof` as the proof home for the exact before/after
event only. `FileOpenedProof` is not a public Story. BoardFacts file
observations may help observe files, but they do not own this event proof and
must not replace `FileOpenedProof`.

First runtime scope:

- ordinary non-promotion pawn move
- ordinary pawn capture, only when the origin file becomes open
- ordinary quiet pawn move, only if the origin file becomes open

Closed runtime scope:

- en passant
- promotion
- file opened by non-pawn move
- file opened on the destination file
- half-open file claim
- rook lift or rook activity
- file control
- attack line
- weakness
- breakthrough
- pressure or initiative
- material gain
- passed-pawn creation

`FileOpenedProof.complete` requires:

- same-board proof
- legal pawn move
- non-promotion move
- ordinary pawn move or capture
- not en passant
- moving side and rival side
- moving pawn identity
- origin square and destination square
- origin file
- before-board origin file contains the moving pawn
- exact after-board replay
- after-board has no white pawn on the origin file
- after-board has no black pawn on the origin file
- opened file equals the pawn origin file

`SceneFileOpened` writer conditions:

- `WriterOpen = true`
- complete `FileOpenedProof`
- complete `StoryProof`
- same-board legal replay
- legal ordinary pawn move
- opened file is the pawn origin file
- exact after-board contains no pawns on the opened file
- no tactic
- no plan
- writer is `SceneFileOpened`
- `EngineCheck` does not `Refute`
- no `CaptureResult`, PawnBreakProof, PawnCaptureProof, PassedPawnCreatedProof, PromotionThreatProof, PromotionProof, DefenseProof, LineProof, PinProof, RemoveGuardProof, SkewerProof, or MultiTargetProof ownership

FileOpened-0 Story identity:

- `scene = Scene.FileOpened`
- `tactic = None`
- `plan = None`
- `side = FileOpenedProof.side`
- `rival = FileOpenedProof.rivalSide`
- `target = destination square`
- `anchor = origin square`
- `route = opening move`
- `openedFile = origin file`
- `routeSan = SAN for the legal move`

### FileOpened-2 SceneFileOpened Writer

FileOpened-2 pins `SceneFileOpened` as the only named writer for
`Scene.FileOpened`.

Writer conditions:

- complete `StoryProof`
- complete `FileOpenedProof`
- same-board legal replay
- legal ordinary pawn move
- opened file is the pawn origin file
- after-board contains no pawns on the opened file
- writer is `SceneFileOpened`
- `EngineCheck` does not `Refute`

Story identity:

- `scene = FileOpened`
- `tactic = None`
- `plan = None`
- `side = moving side`
- `rival = rival side`
- `target = destination square`
- `anchor = pawn origin square`
- `route = pawn move`
- `openedFile = origin file`

`SceneFileOpened` must not create:

- `Scene.Material` claim
- `Scene.PassedPawnCreated` claim
- PawnBreak meaning
- PawnCapture meaning
- rook activity
- pressure
- weakness
- strategy meaning

StoryTable orders an existing `Scene.FileOpened`; it does not create it. If a same-move higher-priority opened claim home such as material, pawn capture, passed-pawn creation, promotion, defense, hanging, or line/defender tactic is also selected, `Scene.FileOpened` may become Support and must not create standalone text.

ExplanationPlan may lower only selected uncapped Lead `Scene.FileOpened` Verdicts to `opens_file`.

Renderer input is `ExplanationPlan` only. The deterministic renderer may say only:

- `{route} opens the {origin-file}-file.`

LLM smoke input is only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`

### FileOpened-3 Negative Corpus

An open-file event is not a pressure, weakness, rook, or strategy claim.
Complete `FileOpenedProof` or silence.

FileOpened-3 must close:

- legal move 아님
- same-board proof 없음
- moving piece가 pawn이 아님
- after-board replay 없음
- origin file에 다른 pawn이 남아 있음
- same-side pawn이 origin file에 남아 있음
- rival pawn이 origin file에 남아 있음
- destination file이 open된 것을 origin-file open으로 착각
- half-open file only
- en passant
- promotion
- non-pawn capture that opens file
- pawn move가 material gain을 claim하려 함
- pawn move가 passed pawn created를 claim하려 함
- open file을 pressure, initiative, weakness, breakthrough로 말하려 함
- rook activity or file control wording 유입

`SceneFileOpened` must stay silent for every FileOpened-3 negative unless
complete `FileOpenedProof` proves the exact origin-file open event on the exact
after-board. Downstream wording must remain no stronger than
`{route} opens the {origin-file}-file.`

### FileOpened-4 EngineCheck Reuse

FileOpened-4 reuses only the existing `EngineCheck` sidecar.

FileOpened-4 rules:

- `EngineCheck` cannot create `Scene.FileOpened`.
- `Supports` creates no new claim.
- `Caps` suppresses standalone FileOpened speech or weakens it to bounded
  relation-only evidence.
- `Refutes` blocks the FileOpened Story.
- `Unknown` creates no engine expression.

FileOpened-4 forbidden engine wording:

- engine says
- eval number
- best move
- only move
- file control because engine likes it
- pressure
- initiative
- weakness
- breakthrough
- wins material
- strategy

`EngineCheck` may attach only to an already existing same-board
`SceneFileOpened` Story whose `FileOpenedProof` still binds the exact origin
file event and whose row has no sibling proof-home contamination.

### FileOpened-5 StoryTable Integration

FileOpened-5 collides `Scene.FileOpened` with existing rows and keeps each
claim in its proof home.

Collision targets:

- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PassedPawnCreated`
- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- `Scene.Material`
- Hanging
- Defense
- Line / Defender tactics
- `Scene.FileOpened`

FileOpened-5 verification:

- input order is stable.
- `Scene.FileOpened` does not own a Material claim.
- `Scene.FileOpened` does not create a PassedPawnCreated claim.
- `Scene.FileOpened` does not own a PawnBreak or PawnCapture event.
- actual material change now remains `Scene.Material` home.
- pawn contact/challenge remains `Scene.PawnBreak` home.
- pawn-captures-rival-pawn event remains `Scene.PawnCapture` home.
- newly-created passer remains `Scene.PassedPawnCreated` home.
- promotion-next meaning remains `Scene.PromotionThreat` home.
- actual promotion meaning remains `Scene.Promotion` home.
- capped or refuted `Scene.FileOpened` has no standalone text.

`Scene.FileOpened` may coexist with other rows, but it owns only the
origin-file-now-open event.

### FileOpened-6 ExplanationPlan

FileOpened-6 lowers only a selected uncapped `Lead` Verdict for
`Scene.FileOpened`.

Allowed claim key:

- `opens_file`

Forbidden claim keys:

- `controls_file`
- `uses_open_file`
- `rook_activity`
- `creates_pressure`
- `takes_initiative`
- `weakens_structure`
- `creates_weakness`
- `breaks_through`
- `wins_space`
- `wins_pawn`
- `wins_material`
- `creates_passed_pawn`
- `promotion_threat`
- `best_move`
- `only_move`
- `forced`

Support, Context, Blocked, capped, and refuted `Scene.FileOpened` rows have no
standalone claim.

`opens_file` speaks only the exact after-board file state. It must not be
interpreted as pressure, control, weakness, strategy, or advantage.

### FileOpened-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

Allowed deterministic wording:

- `{route} opens the {file}-file.`

Forbidden renderer wording:

- takes control of the file
- opens a route for the rook
- creates pressure
- takes the initiative
- weakens the structure
- creates a weakness
- breaks through
- wins space
- wins a pawn
- wins material
- creates a passed pawn
- best move
- only move
- forces

Renderer boundary:

- raw `Story` is forbidden.
- raw `FileOpenedProof` is forbidden.
- direct `BoardFacts` input is forbidden.
- direct `EngineCheck` input is forbidden.
- `proofFailures` are forbidden.
- source rows are forbidden.

`DeterministicRenderer` may render `Scene.FileOpened` only from an
`ExplanationPlan` that already carries selected uncapped Lead `opens_file`.
It must not read, repair, infer, rank, or strengthen chess meaning.

### FileOpened-8 LLM Smoke

FileOpened-8 reuses only the existing 8B LLM smoke boundary.

LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Forbidden LLM smoke input:

- raw `Story`
- raw `FileOpenedProof`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`
- source rows
- new move
- new line

Forbidden LLM smoke output:

- file control
- rook activity
- pressure
- initiative
- weakness
- breakthrough
- material
- passed pawn
- promotion
- best move
- only move
- forced
- strategy claim

LLM smoke may rephrase only the bounded open-file event. It must not add why
the open file matters.

Completion standard: FileOpened-0 closes when one legal pawn move that leaves its origin file can become narrow `Scene.FileOpened` only when exact after-board replay proves no pawn of either side remains on that origin file; straight pawn moves, non-pawn moves, files still containing another pawn, missing same-board proof, incomplete StoryProof, missing or incomplete `FileOpenedProof`, writerless rows, contaminated rows, capped/refuted/support/context/blocked rows, and overclaim wording stay silent; `FileOpenedProof`, `Scene.FileOpened`, and `opens_file` remain the only proof home, Story label, and speech key for this meaning; PawnBreak, PawnCapture, PassedPawnCreated, Material, PromotionThreat, Promotion, Defense, Hanging, and Line / Defender tactics keep their meanings; and rook activity, file control, pressure, initiative, attack, weakness, breakthrough, material gain, passed-pawn creation, pawn-majority change, best move, only move, forced, public route `200`, production API, and public/user-facing LLM narration remain closed.

### FileOpened Closeout Hard Cleanup

FileOpened closeout opens no new chess meaning. It confirms only the already
opened exact origin-file-now-open event.

Closeout authority audit:

- `FileOpenedProof` owns the proof home.
- `Scene.FileOpened` owns the Story label.
- `opens_file` owns the speech key.
- These three authorities are not interchangeable.
- BoardFacts may observe file state, but does not own this event proof.
- EngineCheck may support, cap, refute, or remain unknown only for an existing
  Story, and cannot create `Scene.FileOpened`.

Closeout collision audit:

- `Scene.FileOpened` owns no PawnBreak, PawnCapture, PassedPawnCreated,
  PawnAdvance, PawnStop, PromotionThreat, or Promotion meaning.
- `Scene.FileOpened` owns no Material, Hanging, Defense, Line, or Defender
  tactic meaning.
- Actual material change remains `Scene.Material`.
- Pawn contact remains `Scene.PawnBreak`.
- Capturing a rival pawn remains `Scene.PawnCapture`.
- Newly-created passer remains `Scene.PassedPawnCreated`.
- Promotion-next meaning remains `Scene.PromotionThreat`.
- Actual promotion meaning remains `Scene.Promotion`.

Closeout downstream audit:

- `opens_file` says only that the exact after-board has no pawns of either side
  on the moving pawn's origin file.
- Renderer and LLM smoke wording must stay no stronger than
  `{route} opens the {origin-file}-file.`
- Support, Context, Blocked, capped, and refuted rows have no standalone
  FileOpened text.
- File control, rook activity, pressure, initiative, weakness, breakthrough,
  wins-material, passed-pawn, promotion, best-move, only-move, forced, and
  strategy wording remain forbidden as live FileOpened meaning.

Closeout documentation audit:

- Detailed FileOpened authority lives only in `StoryInteractionLaw.md`.
- SSOT, Architecture, Contract, README, and Manifest may contain summary-only
  non-authoritative pointers.
- There is one chess meaning, one proof home, one Story label, one speech key,
  and one live detailed authority document.

Closeout public-surface audit:

- public route `200` remains closed.
- production API remains closed.
- public/user-facing LLM narration remains closed.
- The existing 8B LLM smoke path remains a bounded internal smoke test only.

Completion standard: FileOpened closes when one legal ordinary pawn move can
produce a bounded open-file event only when `FileOpenedProof` proves the pawn
origin file has no pawns on the exact after-board; all pressure, control, rook
activity, weakness, breakthrough, material, passed-pawn, promotion,
best/only/forced, strategy, public route, production API, and
public/user-facing LLM surfaces remain closed; targeted runtime tests pass;
docs authority tests pass; and `git diff --check` passes.

### FPSNC-0 File / Pawn Structure Neighborhood Closeout Charter

FPSNC-0 closes the File / Pawn Structure Neighborhood as hard cleanup and
interaction audit only.

Core sentence:

File / Pawn Structure Neighborhood closes as four narrow proof-backed event slices. FPSNC opens no new chess meaning.

FPSNC-0 closes only already-open narrow meanings:

- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PassedPawnCreated`
- `Scene.FileOpened`

FPSNC-0 opens no new:

- Story label
- proof home
- Story writer
- claim key
- renderer wording
- LLM behavior
- public route `200`
- production API
- public/user-facing LLM narration

Authority separation:

- `PawnBreakProof`, `Scene.PawnBreak`, and `challenges_pawn` remain separate authority homes.
- `PawnCaptureProof`, `Scene.PawnCapture`, and `captures_rival_pawn` remain separate authority homes.
- `PassedPawnObservation`, `PassedPawnCreatedProof`, `Scene.PassedPawnCreated`, and `creates_passed_pawn` remain separate authority homes.
- `FileOpenedProof`, `Scene.FileOpened`, and `opens_file` remain separate authority homes.

Interaction audit:

- The four Story labels must not steal each other's claims.
- `Scene.FileOpened` owns no file control, rook activity, pressure, weakness, breakthrough, strategy, or advantage meaning.
- `Scene.PawnBreak` does not own FileOpened meaning.
- `Scene.PawnCapture` does not automatically own PassedPawnCreated or FileOpened meaning.
- `Scene.PassedPawnCreated` does not automatically own PawnCapture or FileOpened events.
- Pawn contact/challenge stays in the PawnBreak home.
- Pawn-captures-rival-pawn event stays in the PawnCapture home.
- Newly-created passer event stays in the PassedPawnCreated home.
- Origin-file-now-open event stays in the FileOpened home.
- `CaptureResult` and `Scene.Material` keep material meaning.
- PromotionThreat and Promotion keep promotion meaning.

Scope audit:

Four pawn/file event slices are open. Broad pawn-structure and file interpretation remains closed.

Closed broad meanings include broad PawnTactic, pawn-structure advantage,
passed-pawn strategy, file control, rook activity, pressure, initiative,
weakness, breakthrough, wins space, wins pawn, wins material, best move, only
move, forced move, public route `200`, production API, and public/user-facing
LLM narration.

Duplication audit:

- one direct pawn-contact meaning: `PawnBreakProof` -> `Scene.PawnBreak` -> `challenges_pawn`.
- one pawn-captures-rival-pawn meaning: `PawnCaptureProof` -> `Scene.PawnCapture` -> `captures_rival_pawn`.
- one newly-created passer meaning: `PassedPawnCreatedProof` -> `Scene.PassedPawnCreated` -> `creates_passed_pawn`.
- one origin-file-now-open meaning: `FileOpenedProof` -> `Scene.FileOpened` -> `opens_file`.
- one detailed live authority document: `StoryInteractionLaw.md`.

Renderer and LLM smoke remain no stronger than the selected bounded claim key.
Summary documents may summarize FPSNC-0 only and must not duplicate this
interaction audit.

Completion standard: FPSNC-0 closes when PawnBreakProof / Scene.PawnBreak / challenges_pawn, PawnCaptureProof / Scene.PawnCapture / captures_rival_pawn, PassedPawnObservation / PassedPawnCreatedProof / Scene.PassedPawnCreated / creates_passed_pawn, and FileOpenedProof / Scene.FileOpened / opens_file remain separate; the four Story labels do not steal each other's claims; FileOpened owns no file control, rook activity, pressure, weakness, breakthrough, strategy, or advantage meaning; PawnBreak does not own FileOpened meaning; PawnCapture does not automatically own PassedPawnCreated or FileOpened meaning; PassedPawnCreated does not automatically own PawnCapture or FileOpened events; no new Story, proof home, writer, claim key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration opens; summary docs stay summaries only; and `git diff --check`, targeted commentary tests, and docs authority tests are clean.

### FPSNC-1 Scope Audit

FPSNC-1 audits the open scope for the File / Pawn Structure Neighborhood.

Core sentence:

Four exact event slices are open. Broad interpretation remains closed.

FPSNC-1 open positive Stories are exactly:

- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PassedPawnCreated`
- `Scene.FileOpened`

FPSNC-1 does not open:

- broad PawnTactic
- broad pawn structure advantage
- file control
- rook activity
- rook lift
- open-file strategy
- passed pawn strategy
- breakthrough
- weak square
- weak pawn
- weakens structure
- pawn majority change
- wins space
- wins pawn
- wins material
- promotion threat
- unstoppable pawn
- conversion
- initiative
- pressure
- attack
- best move / only move / forced
- public route `200`
- production API
- public/user-facing LLM narration

Scope audit rules:

- `Scene.PawnBreak` may speak only bounded direct pawn contact through `challenges_pawn`.
- `Scene.PawnCapture` may speak only bounded pawn-captures-rival-pawn event through `captures_rival_pawn`.
- `Scene.PassedPawnCreated` may speak only bounded newly-created passer event through `creates_passed_pawn`.
- `Scene.FileOpened` may speak only bounded origin-file-now-open event through `opens_file`.
- No FPSNC row may classify itself as broad PawnTactic, pawn-structure advantage, file strategy, passed-pawn strategy, file control, rook activity, rook lift, breakthrough, weakness, pawn-majority, space-gain, material-gain, promotion-threat, unstoppable, conversion, initiative, pressure, attack, best, only, or forced meaning.
- Broad interpretation remains closed even when the same move also has pawn contact, pawn capture, passed-pawn creation, file opening, material, promotion, defense, hanging, or line/defender observations.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: FPSNC-1 closes when the only open File / Pawn Structure Neighborhood positive Stories are Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated, and Scene.FileOpened; broad PawnTactic, broad pawn structure advantage, file control, rook activity, rook lift, open-file strategy, passed pawn strategy, breakthrough, weak square, weak pawn, weakens structure, pawn majority change, wins space, wins pawn, wins material, promotion threat, unstoppable pawn, conversion, initiative, pressure, attack, best move, only move, forced, public route `200`, production API, and public/user-facing LLM narration remain closed; and summary docs do not duplicate this detailed scope audit.

### FPSNC-2 Authority / Duplication Audit

FPSNC-2 audits naming, proof ownership, Story ownership, speech ownership, and
document authority for the four open File / Pawn Structure event meanings.

FPSNC-2 duplication audit:

- one chess meaning, one proof home
- one Story label per public chess meaning
- one speech key per allowed public claim
- one detailed live authority document

Mapping:

direct rival-pawn lever/contact:

- proof home = `PawnBreakProof`
- Story label = `Scene.PawnBreak`
- speech key = `challenges_pawn`

pawn captures rival pawn:

- proof home = `PawnCaptureProof`
- Story label = `Scene.PawnCapture`
- speech key = `captures_rival_pawn`

newly-created passed pawn:

- observation = `PassedPawnObservation`
- proof home = `PassedPawnCreatedProof`
- Story label = `Scene.PassedPawnCreated`
- speech key = `creates_passed_pawn`

origin file now has no pawns:

- proof home = `FileOpenedProof`
- Story label = `Scene.FileOpened`
- speech key = `opens_file`

Forbidden duplication:

- `PawnBreakProof` must not act as capture, passer, or file-open proof.
- `PawnCaptureProof` must not act as material, passer, or file-open proof.
- `PassedPawnCreatedProof` must not act as capture, pawn-break, or file-open proof.
- `FileOpenedProof` must not act as pawn-break, capture, passer, control, rook, or weakness proof.
- Speech keys must not duplicate.
- No helper, source row, proof score, renderer wording, or LLM wording may become a second proof home, Story label, or speech key for these meanings.

Boundary notes:

- `PassedPawnObservation` remains observation only, not public Story, speech key, or standalone proof home.
- `CaptureResult` and `Scene.Material` remain the material proof and Story homes.
- `BoardFacts.PawnLever` remains an observation input below `PawnBreakProof`.
- BoardFacts file observations may help observe file state, but they do not own `FileOpenedProof`.
- `StoryInteractionLaw.md` remains the only detailed live authority document for FPSNC duplication law.

Completion standard: FPSNC-2 closes when direct rival-pawn lever/contact, pawn captures rival pawn, newly-created passed pawn, and origin-file-now-has-no-pawns each have exactly one proof home, one Story label, and one speech key; PassedPawnObservation stays observation only for newly-created passer proof; PawnBreakProof does not become capture, passer, or file-open proof; PawnCaptureProof does not become material, passer, or file-open proof; PassedPawnCreatedProof does not become capture, pawn-break, or file-open proof; FileOpenedProof does not become pawn-break, capture, passer, control, rook, or weakness proof; speech keys remain unique; and summary docs do not duplicate this detailed duplication audit.

### FPSNC-3 Cross-Slice Collision Fixtures

FPSNC-3 adds complex same-board collision fixtures without opening new meaning.

These fixtures are collision probes, not permission for automatic merged
claims. If a fixture is physically impossible under the current proof laws,
or if one slice's required proof is absent on the board, that slice stays
silent. In particular, PawnBreak requires its own direct non-capturing
rival-pawn contact proof, FileOpened requires exact origin-file-open proof,
PawnCapture requires its own pawn-captures-rival-pawn proof, and
PassedPawnCreated requires its own newly-created passed-pawn proof.

FPSNC-3 required fixture candidates:

- pawn move creates direct pawn contact and opens origin file
- pawn capture opens origin file
- pawn capture also creates a passed pawn
- pawn move creates passed pawn and opens origin file
- pawn move creates direct contact and newly-created passer
- material capture that is also PawnCapture and FileOpened
- FileOpened candidate that is only half-open and must stay silent
- FileOpened candidate with another pawn still on origin file and must stay silent

FPSNC-3 verification:

- each row keeps its own proof home
- each row keeps its own Story label
- each row keeps its own speech key
- no row automatically upgrades into another meaning without its proof
- actual material change now remains Scene.Material home
- FileOpened speaks only exact origin-file-open state
- PassedPawnCreated speaks only newly-created passer state
- PawnCapture speaks only pawn-captures-rival-pawn event
- PawnBreak speaks only direct pawn contact/challenge event

For collision rows, the proof home remains the only current authority for that
row's public chess meaning:

- PawnBreakProof / Scene.PawnBreak / challenges_pawn may speak only direct
  rival-pawn contact or challenge.
- PawnCaptureProof / Scene.PawnCapture / captures_rival_pawn may speak only a
  pawn capturing a rival pawn.
- PassedPawnObservation plus PassedPawnCreatedProof / Scene.PassedPawnCreated /
  creates_passed_pawn may speak only a newly-created passed pawn.
- FileOpenedProof / Scene.FileOpened / opens_file may speak only that the
  moving pawn's origin file now has no pawns.
- Material proof / Scene.Material remains the home for actual material change;
  PawnCapture and FileOpened do not take that claim.

No FPSNC-3 fixture opens broad PawnTactic, broad pawn-structure advantage, file
control, rook activity, rook lift, open-file strategy, passed pawn strategy,
breakthrough, weak square, weak pawn, weakens structure, pawn majority change,
wins space, wins pawn, wins material, promotion threat, unstoppable pawn,
conversion, initiative, pressure, attack, best move, only move, forced, public
route `200`, production API, or public/user-facing LLM narration.

Completion standard: FPSNC-3 closes when the required same-board collision fixtures prove that each open File / Pawn Structure row keeps its own proof home, Story label, and speech key; no row automatically upgrades into another meaning without its own proof; material change remains Scene.Material; FileOpened speaks only exact origin-file-open state; PassedPawnCreated speaks only newly-created passer state; PawnCapture speaks only pawn-captures-rival-pawn event; PawnBreak speaks only direct pawn contact/challenge event; broad interpretation remains closed; summary docs stay summaries only; and `git diff --check`, targeted commentary tests, and docs authority tests are clean.

### FPSNC-4 StoryTable Interaction Audit

FPSNC-4 collides the four File / Pawn Structure event rows with existing StoryTable rows without opening new meaning.

This audit is StoryTable-only hard cleanup. It does not create a new Story,
proof home, speech key, renderer template, LLM behavior, public route `200`,
production API, or user-facing narration. Each row may compete for ordering,
but competition never transfers proof ownership or public claim ownership.

FPSNC-4 collision targets:

- Scene.PawnBreak
- Scene.PawnCapture
- Scene.PassedPawnCreated
- Scene.FileOpened
- Scene.PawnAdvance
- Scene.PawnStop
- Scene.PromotionThreat
- Scene.Promotion
- Scene.Material
- Tactic.Hanging
- Scene.Defense
- Line / Defender tactics

FPSNC-4 verification:

- input order remains stable
- each claim keeps its existing home
- PawnBreak does not own FileOpened, PawnCapture, or PassedPawnCreated meaning
- PawnCapture does not own FileOpened, PassedPawnCreated, or Material meaning
- PassedPawnCreated does not own FileOpened, PawnCapture, PromotionThreat, or Promotion meaning
- FileOpened does not own PawnBreak, PawnCapture, PassedPawnCreated, or Material meaning
- actual material change now remains Scene.Material home
- promotion-next meaning remains Scene.PromotionThreat home
- actual promotion meaning remains Scene.Promotion home
- capped or refuted rows have no standalone text
- Support, Context, and Blocked rows have no standalone text

Claim-home boundaries:

- Scene.PawnBreak may expose only challenges_pawn from PawnBreakProof.
- Scene.PawnCapture may expose only captures_rival_pawn from PawnCaptureProof.
- Scene.PassedPawnCreated may expose only creates_passed_pawn from
  PassedPawnCreatedProof.
- Scene.FileOpened may expose only opens_file from FileOpenedProof.
- Scene.Material remains the only home for actual material change now.
- Scene.PromotionThreat remains the only home for immediate next-promotion
  meaning.
- Scene.Promotion remains the only home for actual promotion meaning.
- Existing Hanging, Defense, Line, Pin, RemoveGuard, and Skewer rows keep their
  own proof homes and speech keys.

No FPSNC-4 StoryTable collision opens broad pawn-structure interpretation,
file control, rook activity, open-file strategy, passed pawn strategy,
breakthrough, weak square, weak pawn, weakens structure, pawn majority change,
wins space, wins pawn, wins material, promotion threat beyond
Scene.PromotionThreat, unstoppable pawn, conversion, initiative, pressure,
attack, best move, only move, forced, public route `200`, production API, or
public/user-facing LLM narration.

Completion standard: FPSNC-4 closes when StoryTable collision fixtures prove that the four File / Pawn Structure rows keep input-order-stable roles against existing pawn, material, hanging, defense, and line/defender rows; every public claim remains in its existing proof home, Story label, and speech key; actual material change now remains Scene.Material; promotion-next meaning remains Scene.PromotionThreat; actual promotion meaning remains Scene.Promotion; capped and refuted rows produce no standalone text; Support, Context, and Blocked rows produce no standalone text; broad interpretation remains closed; summary docs stay summaries only; and `git diff --check`, targeted commentary tests, and docs authority tests are clean.

### FPSNC-5 ExplanationPlan Boundary Audit

FPSNC-5 audits ExplanationPlan lowering for the four File / Pawn Structure event rows without opening new meaning.

ExplanationPlan input is selected uncapped Lead Verdict only.

FPSNC-5 allowed claim keys:

- challenges_pawn
- captures_rival_pawn
- creates_passed_pawn
- opens_file

FPSNC-5 forbidden claim keys:

- controls_file
- uses_open_file
- rook_activity
- creates_pressure
- takes_initiative
- weakens_structure
- creates_weakness
- breaks_through
- wins_space
- wins_pawn
- wins_material
- promotion_threat
- unstoppable_pawn
- converts_advantage
- best_move
- only_move
- forced

FPSNC-5 verification:

- Support, Context, Blocked, capped, and refuted rows have no standalone claim
- non-selected rows have no standalone claim
- selected Lead lowers only to its own claim key
- selected Lead must not lower to a sibling claim key

Boundary rules:

- Scene.PawnBreak lowers only to challenges_pawn when it is the selected
  uncapped Lead Verdict.
- Scene.PawnCapture lowers only to captures_rival_pawn when it is the selected
  uncapped Lead Verdict.
- Scene.PassedPawnCreated lowers only to creates_passed_pawn when it is the
  selected uncapped Lead Verdict.
- Scene.FileOpened lowers only to opens_file when it is the selected uncapped
  Lead Verdict.
- Non-selected File / Pawn Structure rows may remain internal StoryTable rows,
  but they must not carry a standalone claim key or deterministic rendered
  text.
- Support, Context, and Blocked rows are role or collision information only.
  They must not lower into standalone claim keys.
- Engine-capped and engine-refuted rows remain internal diagnostics or blocked
  outcomes only. They must not lower into standalone claim keys.
- No FPSNC row may lower into a sibling FPSNC key.
- No FPSNC row may lower into file control, open-file use, rook activity,
  pressure, initiative, weakness, breakthrough, space, material, promotion,
  unstoppable-pawn, conversion, best, only, or forced meaning.

FPSNC-5 does not open a new Story label, proof home, claim key, renderer
template, LLM behavior, public route `200`, production API, or
public/user-facing LLM narration. It audits only the downstream handoff from
the already selected Verdict into the bounded ExplanationPlan claim.

Completion standard: FPSNC-5 closes when ExplanationPlan accepts only selected uncapped Lead Verdict input for Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated, and Scene.FileOpened; Support, Context, Blocked, capped, refuted, and non-selected rows have no standalone claim; each selected Lead lowers only to challenges_pawn, captures_rival_pawn, creates_passed_pawn, or opens_file from its own Story home; sibling keys and forbidden broad keys remain closed; summary docs stay summaries only; and `git diff --check`, targeted commentary tests, and docs authority tests are clean.

### FPSNC-6 Renderer Boundary Audit

FPSNC-6 audits deterministic renderer wording for the four File / Pawn Structure event rows without opening new meaning.

Renderer input is ExplanationPlan only.

FPSNC-6 allowed bounded templates:

- {route} challenges the pawn on {target}.
- {route} captures the pawn on {target}.
- {route} creates a passed pawn on {target}.
- {route} opens the {file}-file.

FPSNC-6 forbidden renderer wording:

- controls the file
- uses the open file
- activates the rook
- creates pressure
- takes the initiative
- weakens the structure
- creates a weakness
- breaks through
- wins space
- wins a pawn
- wins material
- will promote
- is unstoppable
- best move
- only move
- forces

FPSNC-6 verification:

- renderer does not read raw Story
- renderer does not read raw proof
- renderer does not read BoardFacts, EngineCheck, or proofFailures
- malformed plans produce no text
- capped, refuted, Support, Context, and Blocked plans produce no text

Renderer boundary rules:

- `DeterministicRenderer` accepts `ExplanationPlan` only.
- `DeterministicRenderer` must not accept raw `Story`, raw proof sidecars,
  `BoardFacts`, `EngineCheck`, `proofFailures`, source rows, or raw engine
  evidence.
- Renderer text is expression only. It does not create, repair, rank, select,
  prove, or strengthen chess meaning.
- `Scene.PawnBreak` may render only the bounded direct pawn-contact template.
- `Scene.PawnCapture` may render only the bounded pawn-captures-rival-pawn
  template.
- `Scene.PassedPawnCreated` may render only the bounded newly-created passer
  template.
- `Scene.FileOpened` may render only the bounded origin-file-open template.
- Wrong claim keys, sibling claim keys, missing route, missing SAN, missing
  target, missing anchor, mismatched evidence line, non-empty secondary target,
  empty forbidden wording, debug-only plans, or malformed plans must produce no
  text.
- Support, Context, Blocked, capped, and refuted plans must produce no text.

FPSNC-6 does not open file control, open-file use, rook activity, pressure,
initiative, weakness, breakthrough, space, material, promotion,
unstoppable-pawn, best-move, only-move, forced, public route `200`,
production API, or public/user-facing LLM narration.

Completion standard: FPSNC-6 closes when DeterministicRenderer accepts only ExplanationPlan input for Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated, and Scene.FileOpened; emits only `{route} challenges the pawn on {target}.`, `{route} captures the pawn on {target}.`, `{route} creates a passed pawn on {target}.`, or `{route} opens the {file}-file.`; does not read raw Story, raw proof, BoardFacts, EngineCheck, proofFailures, source rows, or raw engine evidence; malformed, capped, refuted, Support, Context, and Blocked plans produce no text; forbidden broad renderer wording remains closed; summary docs stay summaries only; and `git diff --check`, targeted commentary tests, and docs authority tests are clean.

### FPSNC-7 LLM Smoke Boundary Audit

FPSNC-7 reuses only the existing 8B LLM smoke boundary for the four File / Pawn Structure event rows.

LLM may rephrase only the selected bounded event. It must not explain why the event matters.

FPSNC-7 LLM input:

- renderedText
- claimKey
- strength
- forbidden wording
- Rephrase only. Do not add chess facts.

FPSNC-7 forbidden LLM input:

- raw Story
- raw PawnBreakProof
- raw PawnCaptureProof
- raw PassedPawnCreatedProof
- raw FileOpenedProof
- PassedPawnObservation
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- source rows

FPSNC-7 forbidden LLM output:

- new move
- new line
- file control
- rook activity
- pressure
- initiative
- weakness
- breakthrough
- material
- passed pawn, unless selected claimKey = creates_passed_pawn
- open file, unless selected claimKey = opens_file
- promotion
- unstoppable
- conversion
- best move
- only move
- forced
- strategy claim

LLM smoke boundary rules:

- The only admitted prompt contract fields are renderedText, claimKey,
  strength, forbidden wording, and `Rephrase only. Do not add chess facts.`
- LLM smoke must not receive raw `Story`, `PawnBreakProof`,
  `PawnCaptureProof`, `PassedPawnCreatedProof`, `FileOpenedProof`,
  `PassedPawnObservation`, `BoardFacts`, `EngineCheck`, raw PV,
  `proofFailures`, source rows, or proof failure text.
- LLM smoke may echo or lightly rephrase only the selected bounded event from
  the deterministic rendered text.
- LLM smoke must not add a new move, a new line, a cause, an evaluation, a
  tactic, a plan, or a strategic explanation.
- Passed-pawn wording is allowed only when the selected claimKey is
  creates_passed_pawn.
- Open-file wording is allowed only when the selected claimKey is opens_file.
- `challenges_pawn` and `captures_rival_pawn` must not become passed-pawn,
  open-file, material, weakness, breakthrough, promotion, conversion,
  pressure, initiative, best-move, only-move, forced, or strategy speech.
- `creates_passed_pawn` must not explain why the passed pawn matters.
- `opens_file` must not explain why the file matters.

FPSNC-7 does not open new LLM behavior, public/user-facing LLM narration,
production API, public route `200`, strategy claims, or any broad
pawn-structure or file interpretation.

Completion standard: FPSNC-7 closes when LLM smoke may rephrase only the selected bounded event and must not explain why the event matters; it reuses only the existing 8B prompt contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`; it rejects raw Story, raw PawnBreakProof, raw PawnCaptureProof, raw PassedPawnCreatedProof, raw FileOpenedProof, PassedPawnObservation, BoardFacts, EngineCheck, raw PV, proofFailures, source rows, new moves, new lines, file control, rook activity, pressure, initiative, weakness, breakthrough, material, disallowed passed-pawn wording, disallowed open-file wording, promotion, unstoppable, conversion, best-move, only-move, forced, and strategy claims; summary docs stay summaries only; and `git diff --check`, targeted commentary tests, and docs authority tests are clean.

### FPSNC-8 Forbidden Wording Audit

FPSNC-8 audits forbidden wording for the four File / Pawn Structure event rows without opening new chess meaning.

Forbidden wording may exist only as forbidden wording, negative corpus, or docs saying it remains closed.

FPSNC-8 forbidden live-authority wording:

- controls file
- file control
- rook activity
- rook lift
- pressure
- initiative
- weak square
- weak pawn
- weakens structure
- breakthrough
- breaks through
- wins space
- wins pawn
- wins material
- promotion threat, unless selected Scene.PromotionThreat
- passed pawn, unless selected Scene.PassedPawnCreated or existing PawnAdvance/PawnStop bounded claim
- open file, unless selected Scene.FileOpened
- unstoppable
- conversion
- best move
- only move
- forced

FPSNC-8 verification:

- forbidden wording does not become proof home
- forbidden wording does not become Story label
- forbidden wording does not become claim key
- forbidden wording does not become renderer output
- forbidden wording does not become LLM-added fact
- forbidden wording does not become public value
- forbidden wording does not become public route payload

Forbidden wording boundary rules:

- `Scene.PawnBreak`, `Scene.PawnCapture`, `Scene.PassedPawnCreated`, and
  `Scene.FileOpened` keep their allowed speech keys only:
  challenges_pawn, captures_rival_pawn, creates_passed_pawn, and opens_file.
- `Scene.PromotionThreat` may own only its existing bounded promotion-next
  event wording. FPSNC-8 opens no new promotion threat wording.
- Existing `Scene.PawnAdvance` and `Scene.PawnStop` may keep only their
  existing bounded passed-pawn event wording. FPSNC-8 opens no passed-pawn
  strategy, conversion, promotion, or unstoppable-pawn meaning.
- `Scene.FileOpened` may keep only the bounded origin-file-open event wording.
  It does not own file control, open-file use, rook activity, rook lift,
  pressure, weakness, breakthrough, space, material, or strategy.
- Forbidden wording must not become a proof home, Story label, claim key,
  deterministic renderer template, LLM-added fact, `Verdict.values` public
  value, public route payload, production API response, or user-facing LLM
  narration.

FPSNC-8 does not open broad pawn structure advantage, file control, rook
activity, rook lift, pressure, initiative, weak square, weak pawn, structure
weakness, breakthrough, space, material gain, promotion threat expansion,
passed pawn strategy, unstoppable pawn, conversion, best move, only move,
forced move, public route `200`, production API, or public/user-facing LLM
narration.

Completion standard: FPSNC-8 closes when forbidden wording may exist only as forbidden wording, negative corpus, or docs saying it remains closed; controls file, file control, rook activity, rook lift, pressure, initiative, weak square, weak pawn, weakens structure, breakthrough, breaks through, wins space, wins pawn, wins material, disallowed promotion-threat wording, disallowed passed-pawn wording, disallowed open-file wording, unstoppable, conversion, best-move, only-move, and forced wording do not become proof homes, Story labels, claim keys, renderer output, LLM-added facts, public values, public route payloads, production API responses, or user-facing LLM narration; summary docs stay summaries only; and `git diff --check`, targeted commentary tests, and docs authority tests are clean.

### FPSNC-9 Documentation Simplification Audit

FPSNC-9 documentation simplification audit:

- detailed File / Pawn Structure Neighborhood closeout authority lives only in `StoryInteractionLaw.md`.
- `README.md`, `ChessCommentarySSOT.md`, `ChessModelArchitecture.md`, `ChessModelContract.md`, and `LegacyPruneManifest.md` remain summary-only.
- `AGENTS.md` keeps durable operator guardrails only.
- retired root docs must not return.
- legacy reset archive documents are historical reference only and not acceptance sources.

FPSNC-9 summary-only documents:

- `README.md`: non-authoritative pointer only.
- `ChessCommentarySSOT.md`: non-authoritative pointer only.
- `ChessModelArchitecture.md`: non-authoritative pointer only.
- `ChessModelContract.md`: non-authoritative pointer only.
- `LegacyPruneManifest.md`: pruning summary only, not detailed law.

FPSNC-9 forbidden documentation duplication:

- no FPSNC detailed checklist in summary docs.
- no closeout stage law in `AGENTS.md`.
- no detailed FPSNC law in `LegacyPruneManifest.md`.
- no retired root docs as live authority.
- no legacy reset archive acceptance source.

Documentation simplification rules:

- `StoryInteractionLaw.md` owns FPSNC-0 through FPSNC-10 and FPSNC final detailed charters,
  scope audits, duplication audits, fixture requirements, StoryTable
  interaction rules, ExplanationPlan boundaries, renderer boundaries, LLM smoke
  boundaries, forbidden wording rules, and completion standards.
- Summary docs may state only that File / Pawn Structure Neighborhood closes as
  four narrow proof-backed event slices and that broad pawn-structure and file
  interpretation remains closed.
- Summary docs must not copy FPSNC stage headings, stage checklists, fixture
  lists, allowed claim-key lists, forbidden claim-key lists, renderer template
  lists, LLM input lists, forbidden wording lists, or completion standards.
- `AGENTS.md` remains the durable operator guide. It may point to
  `StoryInteractionLaw.md`, but it must not add FPSNC closeout stage law.
- `LegacyPruneManifest.md` remains a pruning and summary document. It must not
  become a second FPSNC law source.
- Retired root docs remain absent, and legacy reset archive documents remain
  historical reference only.

Completion standard: FPSNC-9 closes when detailed FPSNC closeout authority appears only in `StoryInteractionLaw.md`; `README.md`, `ChessCommentarySSOT.md`, `ChessModelArchitecture.md`, `ChessModelContract.md`, and `LegacyPruneManifest.md` keep only non-authoritative pointers; `AGENTS.md` remains durable guardrails only; retired root docs remain absent; legacy reset archive documents remain historical reference only and not acceptance sources; summary docs do not duplicate FPSNC detailed checklists; and `git diff --check` plus docs authority tests are clean.

### FPSNC-10 Runtime Boundary Audit

FPSNC-10 runtime boundary audit:

- test helpers are not runtime authority.
- runtime helpers must not become new authority.
- fixture names are not Story family names.
- negative corpus helpers are not production concepts.
- forbidden wording checks must target public overclaim wording, not arbitrary internal field names.
- runtime source must not contain closeout-only terminology.

FPSNC-10 forbids new runtime authority:

- no new Story family.
- no new proof home.
- no new Story writer.
- no new claim key.
- no new renderer template.
- no new LLM behavior.
- no public route `200`.
- no production API.
- no public/user-facing LLM narration.

FPSNC-10 allowed closeout changes:

- tests may audit the boundary.
- documentation law may record the audit in `StoryInteractionLaw.md`.
- runtime code changes are allowed only if an audit exposes leakage or duplicated authority.

FPSNC-10 runtime source guard:

- runtime source must not contain `FPSNC-`.
- runtime source must not contain `closeout`.
- runtime source must not contain `Runtime Boundary Audit`.
- runtime source must not contain `File / Pawn Structure Neighborhood Closeout`.
- runtime source must not contain `fixture map`.
- runtime source must not contain `negative corpus`.

Completion standard: FPSNC-10 closes when helper names remain test-local; runtime helper names do not become Story families, proof homes, Story writers, claim keys, renderer templates, LLM behavior, public route `200`, production API, or public/user-facing LLM narration; runtime source contains no FPSNC closeout terminology; closeout changes remain limited to tests and `StoryInteractionLaw.md` unless an audit exposes leakage or duplicated authority; summary docs stay summaries only; and `git diff --check`, targeted commentary tests, and docs authority tests are clean.

### FPSNC Final Completion Standard

FPSNC final completion standard:

- `Scene.PawnBreak`, `Scene.PawnCapture`, `Scene.PassedPawnCreated`, and `Scene.FileOpened` remain the only open File / Pawn Structure Neighborhood positive slices.
- each slice has exactly one proof path, one Story label, and one speech key.
- no slice owns another slice's chess meaning.
- Material, PromotionThreat, Promotion, PawnAdvance, PawnStop, Hanging, Defense, and Line / Defender tactic homes keep their meanings.
- forbidden broad pawn/file wording remains forbidden only.
- StoryTable ordering is input-order stable across neighborhood collisions.
- non-selected, capped, refuted, Support, Context, and Blocked rows produce no standalone downstream text.
- ExplanationPlan, Renderer, and LLM smoke remain bounded to selected Lead Verdict data.
- detailed authority lives only in `StoryInteractionLaw.md`.
- summary docs remain summaries only.
- public route `200`, production API, and public/user-facing LLM narration remain closed.

FPSNC final verification:

- targeted commentary runtime tests pass.
- docs authority tests pass.
- `git diff --check` passes.

Final FPSNC closeout opens no new Story family, proof home, Story writer, claim key, renderer template, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad PawnTactic, broad pawn-structure advantage, file control, rook activity, rook lift, open-file strategy, passed pawn strategy, breakthrough, weak square, weak pawn, weakens structure, pawn majority change, wins space, wins pawn, wins material, promotion threat, unstoppable pawn, conversion, initiative, pressure, attack, best move, only move, forced, public route `200`, production API, or public/user-facing LLM narration.

## Pawn Blocking / Fixed Pawn Neighborhood

### PBFN-0 Charter

Current scope is the first Pawn Blocking / Fixed Pawn Neighborhood vertical slice.

First positive scope is not broad pawn-structure weakness.

First positive scope:

a legal move blocks one rival pawn from advancing one square on the exact board

Player-facing bounded wording:

This move blocks the opponent's pawn advance square.

Core sentence:

PawnBlockProof proves the exact pawn-block event. Scene.PawnBlock may speak only that event, not weakness, fixation, restriction advantage, or strategic clamp.

PBFN-0 opens only:

- narrow `Scene.PawnBlock`
- `PawnBlockProof`
- legal move
- one rival pawn identified
- rival pawn next advance square identified
- that square is occupied by the moving side after the move
- the block is exact-board replayed
- same-board proof
- selected Verdict after bounded pawn-block wording

PBFN-0 does not open:

- weak pawn claim
- fixed pawn as strategic weakness
- permanent immobility
- backward pawn
- isolated pawn
- passed pawn stop
- creates blockade
- binds the structure
- wins tempo
- creates pressure
- restricts opponent
- breakthrough prevention
- best move / only move
- public route `200`
- production API
- public/user-facing LLM narration

Authority split:

- `PawnBlockProof` proves only the exact rival-pawn next-square block event on
  the exact board.
- `Scene.PawnBlock` owns only the bounded public Story identity for that block
  event.
- The speech key for this slice may say only that the move blocks the rival
  pawn's next advance square.
- BoardFacts may observe pieces, squares, legal movement, pawn fronts, or
  blockers, but it does not own this public event proof.
- StoryTable may order an existing `Scene.PawnBlock`; it must not create one.
- ExplanationPlan may lower only a selected uncapped Lead `Scene.PawnBlock`
  Verdict to bounded pawn-block wording.
- Renderer and LLM smoke may phrase only the bounded pawn-block event after the
  downstream stages explicitly open.

PBFN-0 must stay silent for:

- illegal move
- missing same-board proof
- missing exact-board replay
- no rival pawn identified
- no rival pawn next advance square identified
- rival pawn next advance square is not occupied by the moving side after the
  move
- the occupied square is not the identified rival pawn's one-square advance
  square
- multiple rival pawns without one identified block event
- writerless or contaminated rows
- weakness, fixation, restriction, pressure, blockade, tempo, best-move,
  only-move, or breakthrough-prevention wording

PBFN-0 does not create a fixed-pawn strategic diagnosis. "Fixed" may describe
only the exact immediate occupancy of the rival pawn's next square after the
move, not permanent immobility, weakness, blockade, or advantage.

Completion standard: PBFN-0 closes when only a legal same-board move that leaves the moving side occupying one identified rival pawn's next advance square can become narrow `Scene.PawnBlock` through `PawnBlockProof`; illegal moves, missing same-board proof, missing replay, missing rival pawn identity, missing next-square identity, wrong occupied square, ambiguous multi-pawn blocks, writerless rows, and contaminated rows stay silent; weakness, fixed-pawn strategic weakness, permanent immobility, backward-pawn, isolated-pawn, passed-pawn stop, blockade, structure bind, tempo, pressure, restriction advantage, breakthrough prevention, best/only, public route `200`, production API, and public/user-facing LLM narration remain closed; and detailed PBFN authority lives only in `StoryInteractionLaw.md`.

### PBFN-1 PawnBlockProof

PBFN-1 opens only `PawnBlockProof` as the proof home for the first
Pawn Blocking / Fixed Pawn Neighborhood slice.

`PawnBlockProof` proves:

- blocking side
- rival side
- legal move identity
- moving piece identity
- origin square
- destination square
- blocked rival pawn identity
- blocked pawn square
- blocked pawn next advance square
- next advance square occupied after move
- occupying piece belongs to blocking side
- exact after-board replay
- same-board proof
- block was created by this move

PBFN-1 first scope:

- ordinary direct one-square pawn block only
- the moving piece lands on the rival pawn's next advance square

PBFN-1 closed scope:

- pre-existing block not created by the move
- diagonal pawn contact
- pawn capture
- passed pawn stop claim
- permanent fixed pawn claim
- backward pawn claim
- isolated pawn claim
- weakness claim
- blockade strategy
- restriction / pressure / initiative
- best move / only move
- material claim

Proof-home boundary:

- `PawnBlockProof` is not a public `Story`.
- `PawnBlockProof` must not create `Scene.PawnBlock`.
- `PawnBlockProof` must not create a Verdict, ExplanationPlan, renderer text,
  LLM input, public route payload, or production API response.
- `PawnStopProof` remains the passed-pawn next-square stop home.
- `PawnBlockProof` does not replace `PawnStopProof`.
- `PawnBreakProof` remains the pawn contact/challenge home.
- `PawnBlockProof` does not replace `PawnBreakProof`.
- `PawnBlockProof` must not own passed-pawn stop, pawn contact, pawn capture,
  permanent fixation, backward pawn, isolated pawn, weakness, blockade,
  restriction, pressure, initiative, best-move, only-move, or material meaning.

Completion standard: PBFN-1 closes when `PawnBlockProof` proves only the blocking side, rival side, legal move identity, moving piece identity, origin square, destination square, blocked rival pawn identity, blocked pawn square, blocked pawn next advance square, after-board occupancy by the blocking side, exact after-board replay, same-board proof, and block-created-by-this-move for an ordinary direct move landing on the rival pawn's next advance square; pre-existing blocks, diagonal pawn contact, pawn capture, passed-pawn stop, permanent fixed-pawn, backward-pawn, isolated-pawn, weakness, blockade strategy, restriction, pressure, initiative, best/only, and material meanings remain closed; `PawnBlockProof` is not a public Story; `PawnStopProof` and `PawnBreakProof` keep their separate homes; and no Story writer, Verdict, ExplanationPlan, renderer, LLM, public route `200`, production API, or public/user-facing LLM narration opens.

### PBFN-2 Scene.PawnBlock Writer

PBFN-2 opens only `ScenePawnBlock` as the named writer for `Scene.PawnBlock`.

PBFN-2 writer conditions:

- complete StoryProof
- complete `PawnBlockProof`
- same-board legal replay
- legal move creates the block
- blocked piece is rival pawn
- blocked square is that rival pawn's next advance square
- writer = `ScenePawnBlock`
- EngineCheck does not Refute

PBFN-2 Story identity:

- scene = `PawnBlock`
- tactic = None
- plan = None
- side = blocking side
- rival = rival side
- target = blocked pawn next advance square
- anchor = moving piece origin square
- route = blocking move

PBFN-2 forbidden:

- `ScenePawnBlock` must not create a PawnStop claim.
- `ScenePawnBlock` must not create a PawnBreak claim.
- `ScenePawnBlock` must not create a PassedPawnCreated claim.
- `ScenePawnBlock` must not create a FileOpened claim.
- `ScenePawnBlock` must not create weakness, fixed-pawn, blockade, or strategy meaning.

PBFN-2 downstream boundary:

- PBFN-2 does not open ExplanationPlan.
- PBFN-2 does not open renderer text.
- PBFN-2 does not open LLM input.
- PBFN-2 does not open public route `200`, production API, or public/user-facing LLM narration.

Completion standard: PBFN-2 closes when `ScenePawnBlock` writes only exact complete `PawnBlockProof` rows into `Scene.PawnBlock` with no tactic, no plan, blocking side, rival side, target as the blocked pawn next advance square, anchor as the moving piece origin square, route as the blocking move, no EngineCheck Refutes lead, and no sibling PawnStop, PawnBreak, PassedPawnCreated, FileOpened, weakness, fixed-pawn, blockade, or strategy meaning; downstream ExplanationPlan, renderer, LLM, public route `200`, production API, and public/user-facing LLM narration remain closed.

### PBFN-3 Negative Corpus

PBFN-3 must stay silent for:

- legal move missing
- same-board proof missing
- blocked pawn missing
- blocked piece is not a pawn
- blocked pawn is not the rival side
- blocked pawn next advance square cannot be calculated
- next advance square is empty on the after-board
- occupying piece is not proven to belong to the blocking side
- block is not newly created by the move
- pre-existing blocked pawn
- diagonal pawn contact only
- capture event
- passed pawn stop home
- promotion
- promotion threat
- file opened
- material gain
- weak pawn wording
- fixed pawn wording
- blockade wording
- restriction wording

PBFN-3 standard:

- A pawn block is not a weakness claim.
- A pawn block is not a strategy claim.
- A pawn block is not a passed-pawn stop claim.
- Complete `PawnBlockProof` or silence.
- `ScenePawnBlock` must not open ExplanationPlan, renderer text, LLM input,
  public route `200`, production API, or public/user-facing LLM narration.

Completion standard: PBFN-3 closes when the negative corpus proves that illegal moves, missing same-board proof, missing blocked pawn identity, non-pawn blockers, same-side pawns, missing next-square calculation, empty after-board next squares, unproven occupying side, pre-existing blocks, diagonal contact, captures, passed-pawn stops, promotion, promotion threats, file-opened events, material-gain events, and weakness, fixed-pawn, blockade, or restriction wording all stay silent under `PawnBlockProof`, `ScenePawnBlock`, `StoryTable`, and downstream ExplanationPlan boundaries; a pawn block remains only the exact legal same-board event that one rival pawn's next advance square is occupied by the moving side after the move.

### PBFN-4 EngineCheck Reuse

PBFN-4 reuses only the existing `EngineCheck` boundary.

PBFN-4 rules:

- `EngineCheck` cannot create `Scene.PawnBlock`.
- Supports does not create a new claim.
- Caps suppresses standalone claim speech or weakens it to the bounded selected claim when a downstream claim is open.
- Refutes makes the `Scene.PawnBlock` Story Blocked.
- Unknown creates no engine expression.

PBFN-4 forbidden:

- engine says
- eval numbers
- best move
- only move
- wins pawn
- restricts opponent
- creates pressure
- strategic blockade

PBFN-4 downstream boundary:

- PBFN-4 does not open ExplanationPlan for `Scene.PawnBlock`.
- PBFN-4 does not open renderer text for `Scene.PawnBlock`.
- PBFN-4 does not open LLM input for `Scene.PawnBlock`.
- PBFN-4 does not open public route `200`, production API, or public/user-facing LLM narration.

Completion standard: PBFN-4 closes when `EngineCheck` attaches only to an already complete same-board `ScenePawnBlock` Story, cannot produce `Scene.PawnBlock` from engine evidence alone, Supports adds no chess claim, Caps suppresses or bounds downstream speech without opening PawnBlock wording, Refutes blocks the PawnBlock row, Unknown creates no engine expression, and engine-says, eval-number, best/only, wins-pawn, restriction, pressure, or strategic-blockade wording remains closed.

### PBFN-5 StoryTable Integration

PBFN-5 collides `Scene.PawnBlock` with existing rows.

PBFN-5 collision targets:

- `Scene.PawnStop`
- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PassedPawnCreated`
- `Scene.FileOpened`
- `Scene.PawnAdvance`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- `Scene.Material`
- Hanging
- Defense
- Line tactics
- `Scene.PawnBlock`

PBFN-5 verifies:

- input order stability
- `Scene.PawnBlock` does not own a PawnStop claim.
- `Scene.PawnBlock` does not own PawnBreak contact or challenge.
- `Scene.PawnBlock` does not own a Material claim.
- `Scene.PawnBlock` does not create a PassedPawnCreated claim.
- `Scene.PawnBlock` does not create a FileOpened claim.
- actual passed-pawn next-square stop stays in `Scene.PawnStop`.
- pawn contact or challenge stays in `Scene.PawnBreak`.
- capped or refuted `Scene.PawnBlock` has no standalone text.

PBFN-5 StoryTable boundary:

- `Scene.PawnBlock` yields to every already-open claim home.
- `Scene.PawnBlock` may remain selected only as the exact pawn-block event.
- `Scene.PawnBlock` selection still does not open ExplanationPlan, renderer text,
  LLM input, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: PBFN-5 closes when StoryTable ordering is stable across input order, `Scene.PawnBlock` yields to every already-open claim home, PawnStop, PawnBreak, PawnCapture, PassedPawnCreated, FileOpened, PawnAdvance, PromotionThreat, Promotion, Material, Hanging, Defense, and Line tactics keep their own proof and speech homes, and capped or refuted PawnBlock rows produce no standalone text.

### PBFN-6 ExplanationPlan

PBFN-6 opens only `ExplanationPlan` for selected uncapped `Scene.PawnBlock` Lead Verdicts.

PBFN-6 allowed claim key:

- `blocks_pawn`

PBFN-6 forbidden claim keys:

- `stops_passed_pawn`
- `challenges_pawn`
- `captures_rival_pawn`
- `creates_passed_pawn`
- `opens_file`
- `wins_pawn`
- `wins_material`
- `weakens_structure`
- `fixes_pawn`
- `creates_blockade`
- `restricts_opponent`
- `creates_pressure`
- `takes_initiative`
- `best_move`
- `only_move`
- `forced`

PBFN-6 input boundary:

- `ExplanationPlan.fromSelected` accepts only selected uncapped Lead `Scene.PawnBlock` Verdicts.
- The Story must still be `Scene.PawnBlock`, `tactic = None`, `plan = None`, `writer = ScenePawnBlock`, and same-board proof-backed.
- `target` remains the blocked rival pawn next advance square.
- `anchor` remains the moving piece origin square.
- `route` remains the legal blocking move.
- `evidenceLine` is only that route.

PBFN-6 no-standalone boundary:

- Support, Context, Blocked, capped, and refuted `Scene.PawnBlock` rows have no standalone claim.
- EngineCheck Supports does not create a new claim; it may accompany the already selected uncapped Lead claim only.
- EngineCheck Caps suppresses standalone claim output.
- EngineCheck Refutes keeps the row Blocked.

PBFN-6 downstream boundary:

- PBFN-6 does not open deterministic renderer text, LLM narration, public route `200`, production API, weakness wording, fixed-pawn wording, blockade wording, restriction wording, pressure wording, best-move wording, only-move wording, or forced wording.
- `blocks_pawn` may mean only: this move blocks the rival pawn's next advance square on the exact after-board.
- `blocks_pawn` may not mean PawnStop, PawnBreak, PawnCapture, PassedPawnCreated, FileOpened, material gain, weakness, fixed pawn, blockade strategy, restriction, pressure, initiative, best move, only move, or forced move.

Completion standard: PBFN-6 closes when `ExplanationPlan.fromSelected` admits only selected uncapped Lead `Scene.PawnBlock` Verdicts, emits only `blocks_pawn`, rejects sibling pawn, material, weakness, blockade, restriction, pressure, best, only, and forced claim keys, and keeps Support, Context, Blocked, capped, and refuted PawnBlock rows without standalone claims.

### PBFN-7 Deterministic Renderer

PBFN-7 opens deterministic renderer text only from bounded PawnBlock ExplanationPlan.

PBFN-7 renderer input boundary:

- Renderer input is `ExplanationPlan` only.
- Renderer does not accept Story, Verdict, PawnBlockProof, BoardFacts, EngineCheck, engine eval, or source rows.
- Renderer may phrase only selected uncapped Lead `Scene.PawnBlock` plans with `claimKey = blocks_pawn`.
- Support, Context, Blocked, capped, refuted, malformed, or sibling-claim plans have no standalone renderer text.

PBFN-7 allowed renderer templates:

- `{route} blocks the pawn on {blockedPawnSquare}.`
- `{route} blocks the pawn from advancing.`

PBFN-7 forbidden renderer wording:

- `fixes the pawn`
- `weakens the pawn`
- `creates a weakness`
- `creates a blockade`
- `stops the passed pawn`
- `restricts Black`
- `takes space`
- `creates pressure`
- `wins a tempo`
- `best move`
- `only move`
- `forces`

PBFN-7 downstream boundary:

- PBFN-7 does not open LLM narration, public route `200`, production API, weakness claim, fixed-pawn claim, blockade strategy, restriction, pressure, initiative, best move, only move, or forced move.
- Renderer text may mean only: this move blocks the rival pawn's next advance square on the exact after-board.
- Renderer text may not mean PawnStop, PawnBreak, PawnCapture, PassedPawnCreated, FileOpened, material gain, weakness, fixed pawn, blockade strategy, restriction, pressure, initiative, best move, only move, or forced move.

Completion standard: PBFN-7 closes when DeterministicRenderer accepts only a bounded `blocks_pawn` ExplanationPlan, emits only bounded pawn-block text, rejects Support, Context, Blocked, capped, refuted, malformed, or sibling-claim plans, keeps forbidden pawn weakness, fixed-pawn, blockade, restriction, pressure, tempo, best, only, and force wording absent, and leaves LLM narration closed.

### PBFN-8 LLM Smoke

PBFN-8 reuses only the existing 8B LLM smoke boundary for selected bounded PawnBlock RenderedLine rephrases.

PBFN-8 allowed LLM smoke input:

- `renderedText`
- `claimKey`
- `strength`
- forbidden wording
- `Rephrase only. Do not add chess facts.`

PBFN-8 forbidden LLM smoke input:

- raw Story
- raw PawnBlockProof
- PawnStopProof
- BoardFacts
- EngineCheck
- raw PV
- proofFailures

PBFN-8 forbidden LLM smoke additions:

- new move
- new line
- weakness claim
- blockade claim
- restriction claim
- pressure claim
- initiative claim
- material claim
- passed-pawn claim

PBFN-8 downstream boundary:

- PBFN-8 does not open public/user-facing LLM narration, public route `200`, production API, raw proof input, raw engine input, or any new chess meaning.
- LLM smoke may echo or rephrase only the already rendered bounded pawn-block event.
- LLM smoke may not add PawnStop, PawnBreak, PawnCapture, PassedPawnCreated, FileOpened, material, weakness, fixed pawn, blockade strategy, restriction, pressure, initiative, best move, only move, forced move, new move, or new line.

Completion standard: PBFN-8 closes when LlmNarrationSmoke accepts only the renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` prompt contract for bounded `blocks_pawn`, rejects raw Story, PawnBlockProof, PawnStopProof, BoardFacts, EngineCheck, raw PV, proofFailures, new moves, new lines, weakness, blockade, restriction, pressure, initiative, material, and passed-pawn additions, and keeps public narration and production API closed.

### PBFNC-0 Pawn Blocking / Fixed Pawn Neighborhood Closeout

PBFNC-0 opens no new chess meaning.

Pawn Blocking / Fixed Pawn Neighborhood is closed with one narrow public event: a move blocks one rival pawn from advancing. It does not open fixed-pawn, weak-pawn, blockade, restriction, or pressure meaning.

PBFNC-0 closeout authority separation:

- proof home = `PawnBlockProof`
- Story label = `Scene.PawnBlock`
- writer = `ScenePawnBlock`
- speech key = `blocks_pawn`
- detailed authority document = `StoryInteractionLaw.md`

PBFNC-0 duplication audit:

- one chess meaning, one proof home
- one Story label per public chess meaning
- one speech key per allowed public claim
- one detailed live authority document

PBFNC-0 ownership audit:

- `Scene.PawnBlock` does not own `Scene.PawnStop` meaning.
- `Scene.PawnBlock` does not own `Scene.PawnBreak` meaning.
- `Scene.PawnBlock` does not own `Scene.PawnCapture` meaning.
- `Scene.PawnBlock` does not own `Scene.PassedPawnCreated` meaning.
- `Scene.PawnBlock` does not own `Scene.FileOpened` meaning.
- `Scene.PawnBlock` does not own Material, Hanging, Defense, Line tactic, or Defender tactic meaning.

PBFNC-0 closed wording audit:

- fixed pawn
- weak pawn
- blockade
- restriction
- pressure
- initiative
- best move
- only move

Closed wording may appear only as forbidden wording, negative corpus, or docs saying it remains closed.

Renderer and LLM smoke wording must be no stronger than `blocks_pawn`.

Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PBFNC-0 closes when PawnBlockProof, Scene.PawnBlock, ScenePawnBlock, and blocks_pawn remain separated by layer; exactly one already-open public event remains open; PawnBlock owns no PawnStop, PawnBreak, PawnCapture, PassedPawnCreated, FileOpened, Material, Hanging, Defense, Line, or Defender tactic meaning; fixed-pawn, weak-pawn, blockade, restriction, pressure, initiative, best-move, and only-move wording remains forbidden or closed only; renderer and LLM smoke output stays no stronger than blocks_pawn; detailed authority stays in StoryInteractionLaw.md; summary docs remain summaries; and public route `200`, production API, and public/user-facing LLM narration stay closed.

### PBFNC-1 Scope Audit

PBFNC-1 verifies that no broader fixed-pawn or strategic structure claim is open.

PBFNC-1 open positive Story labels:

- `Scene.PawnBlock` only

PBFNC-1 open proof homes:

- `PawnBlockProof` only

PBFNC-1 open speech keys:

- `blocks_pawn` only

PBFNC-1 closed Story labels:

- `Scene.FixedPawn`
- `Scene.WeakPawn`
- `Scene.Blockade`
- `Scene.Restriction`

PBFNC-1 closed speech keys:

- `fixed_pawn`
- `weak_pawn`
- `creates_blockade`
- `restricts_opponent`
- `creates_pressure`
- `takes_initiative`

PawnBlock is a move-created block event only.

No broader fixed-pawn or strategic structure claim is open.

Completion standard: PBFNC-1 closes when `Scene.PawnBlock`, `PawnBlockProof`, and `blocks_pawn` are the only open Pawn Blocking / Fixed Pawn Neighborhood Story label, proof home, and speech key; `Scene.FixedPawn`, `Scene.WeakPawn`, `Scene.Blockade`, `Scene.Restriction`, `fixed_pawn`, `weak_pawn`, `creates_blockade`, `restricts_opponent`, `creates_pressure`, and `takes_initiative` remain closed; and PawnBlock remains only a move-created block event, not a broader fixed-pawn or strategic structure claim.

### PBFNC-2 Authority Duplication Audit

PBFNC-2 verifies one chess meaning, one home.

PBFNC-2 authority separation:

- `PawnBlockProof` = exact diagnostic proof home
- `Scene.PawnBlock` = public Story label
- `blocks_pawn` = bounded speech key
- `StoryInteractionLaw.md` = detailed authority document

PBFNC-2 forbidden authority duplication:

- `PawnBlockProof` must not be used as a public Story.
- `Scene.PawnBlock` must not be used as a proof home.
- `blocks_pawn` must not be used as proof or Story identity.
- README, SSOT, Architecture, Contract, and Manifest must not duplicate detailed stage law.
- `BoardFacts`, `PieceContact`, `PawnStopProof`, and `PawnBreakProof` must not replace PawnBlock authority.

PBFNC-2 docs authority rule:

- detailed authority is only in `StoryInteractionLaw.md`
- other live docs keep summary only
- docs authority tests must catch detailed-law duplication

Completion standard: PBFNC-2 closes when `PawnBlockProof`, `Scene.PawnBlock`, `blocks_pawn`, and `StoryInteractionLaw.md` each keep one job; `PawnBlockProof` is not public Story, `Scene.PawnBlock` is not proof home, `blocks_pawn` is not proof or Story identity; README, SSOT, Architecture, Contract, and Manifest contain summary only; `BoardFacts`, `PieceContact`, `PawnStopProof`, and `PawnBreakProof` do not replace PawnBlock authority; and docs authority tests fail on duplicated detailed stage law outside `StoryInteractionLaw.md`.

### PBFNC-3 Cross-Meaning Collision Audit

PBFNC-3 collides PawnBlock with existing rows.

PBFNC-3 collision targets:

- `Scene.PawnStop`
- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PassedPawnCreated`
- `Scene.FileOpened`
- `Scene.PawnAdvance`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- `Scene.Material`
- `Tactic.Hanging`
- `Scene.Defense`
- Line / Defender tactics
- `Scene.PawnBlock`

PBFNC-3 ownership verification:

- PawnBlock does not own PawnStop claim.
- PawnBlock does not own PawnBreak contact/challenge.
- PawnBlock does not own PawnCapture event.
- PawnBlock does not own PassedPawnCreated event.
- PawnBlock does not own FileOpened event.
- PawnBlock does not own Material claim.
- PawnBlock does not own Hanging, Defense, Line, or Defender tactic meaning.

PBFNC-3 home criteria:

- actual passed-pawn next-square stop = `Scene.PawnStop` home
- pawn contact/challenge = `Scene.PawnBreak` home
- pawn capture = `Scene.PawnCapture` home
- new passer = `Scene.PassedPawnCreated` home
- open file = `Scene.FileOpened` home
- material change now = `Scene.Material` home

Completion standard: PBFNC-3 closes when collision fixtures prove `Scene.PawnBlock` stays only the narrow move-created block event; `Scene.PawnStop`, `Scene.PawnBreak`, `Scene.PawnCapture`, `Scene.PassedPawnCreated`, `Scene.FileOpened`, `Scene.PawnAdvance`, `Scene.PromotionThreat`, `Scene.Promotion`, `Scene.Material`, `Tactic.Hanging`, `Scene.Defense`, and Line / Defender tactic rows keep their own proof homes, Story labels, and speech keys; and PawnBlock never owns those claims or emits standalone text for them.

### PBFNC-4 StoryTable Stability Audit

PBFNC-4 verifies PawnBlock inside StoryTable.

PBFNC-4 StoryTable verification:

- input order is stable
- `Scene.PawnBlock` receives deterministic role under the same conditions
- existing stronger homes keep their own meaning
- invalid PawnBlock rows are Blocked
- EngineCheck Refutes blocks PawnBlock Story
- EngineCheck Supports creates no new claim
- EngineCheck Caps suppresses standalone PawnBlock text or weakens it within the bounded claim
- EngineCheck Unknown creates no engine expression
- Support, Context, Blocked, capped, and refuted rows produce no standalone downstream text

PBFNC-4 forbidden StoryTable wording:

- engine says
- eval number
- best move
- only move
- restriction
- pressure
- initiative

Completion standard: PBFNC-4 closes when StoryTable ordering is input-order stable; PawnBlock receives deterministic role under the same row set; stronger existing homes keep their own meaning; invalid, refuted, capped, Support, Context, and Blocked PawnBlock rows do not produce standalone downstream text; Supports adds no claim beyond `blocks_pawn`; Unknown adds no engine expression; and engine-says, eval-number, best-move, only-move, restriction, pressure, and initiative wording remain forbidden.

### PBFNC-5 Downstream Boundary Audit

PBFNC-5 verifies ExplanationPlan, Renderer, and LLM smoke for PawnBlock.

PBFNC-5 downstream input:

- selected uncapped Lead Verdict only

PBFNC-5 allowed claim key:

- `blocks_pawn`

PBFNC-5 forbidden claim keys:

- `fixed_pawn`
- `weak_pawn`
- `creates_blockade`
- `restricts_opponent`
- `creates_pressure`
- `takes_initiative`
- `stops_passed_pawn`
- `challenges_pawn`
- `captures_rival_pawn`
- `creates_passed_pawn`
- `opens_file`
- `wins_material`
- `best_move`
- `only_move`
- `forced`

PBFNC-5 renderer allowed levels:

- `{route} blocks the pawn on {target}.`
- `{route} blocks the pawn from advancing.`

PBFNC-5 renderer and LLM forbidden wording:

- fixed pawn
- weak pawn
- blockade
- restriction
- pressure
- initiative
- strategy
- best move
- only move
- forces
- wins tempo

Completion standard: PBFNC-5 closes when only selected uncapped Lead PawnBlock Verdicts lower to ExplanationPlan; the only allowed claim key is `blocks_pawn`; fixed_pawn, weak_pawn, creates_blockade, restricts_opponent, creates_pressure, takes_initiative, stops_passed_pawn, challenges_pawn, captures_rival_pawn, creates_passed_pawn, opens_file, wins_material, best_move, only_move, and forced remain forbidden; renderer output is no stronger than `{route} blocks the pawn on {target}.` or `{route} blocks the pawn from advancing.`; and Renderer plus LLM smoke reject fixed-pawn, weak-pawn, blockade, restriction, pressure, initiative, strategy, best-move, only-move, forces, and wins-tempo wording.

### PBFNC-6 Forbidden Wording Authority Audit

PBFNC-6 verifies that forbidden wording does not become live PawnBlock authority.

PBFNC-6 forbidden wording:

- fixed pawn
- weak pawn
- blockade
- restriction
- restricts
- pressure
- initiative
- space advantage
- strategic clamp
- permanent weakness
- best move
- only move
- forced

PBFNC-6 allowed locations:

- negative corpus
- forbidden wording list
- LLM smoke rejection list
- closeout cleanup checklist

PBFNC-6 forbidden authority locations:

- public claim key
- Story label
- proof home
- writer name
- renderer template
- positive summary sentence
- README, SSOT, Architecture, Contract, or Manifest detailed authority

Completion standard: PBFNC-6 closes when fixed pawn, weak pawn, blockade, restriction, restricts, pressure, initiative, space advantage, strategic clamp, permanent weakness, best move, only move, and forced appear only in negative corpus, forbidden wording lists, LLM smoke rejection lists, or closeout cleanup checklists; none become public claim keys, Story labels, proof homes, writer names, renderer templates, positive summary sentences, or detailed authority in README, SSOT, Architecture, Contract, or Manifest.

### PBFNC-7 Documentation Simplification Audit

PBFNC-7 verifies that PBFNC closeout detail has one live documentation home.

PBFNC-7 detailed authority:

- `StoryInteractionLaw.md` only

PBFNC-7 summary-only documents:

- `README.md`
- `ChessCommentarySSOT.md`
- `ChessModelArchitecture.md`
- `ChessModelContract.md`
- `LegacyPruneManifest.md`

PBFNC-7 forbidden documentation duplication:

- summary docs must not copy detailed PBFNC stage law
- AGENTS.md must not change unless durable operator rules change
- docs tests must reject detailed PBFNC law outside `StoryInteractionLaw.md`

PBFNC-7 legacy and retired authority guard:

- legacy reset archive docs are historical reference only
- retired root authority docs must not return

Completion standard: PBFNC-7 closes when the PBFNC closeout anchor and detailed PBFNC stage law live only in `StoryInteractionLaw.md`; README, SSOT, Architecture, Contract, and Manifest contain only the existing summary; AGENTS.md contains no PBFNC detailed law; summary docs reject copied PBFNC stage detail; legacy reset archive docs are not live authority; and retired root authority docs do not return.

### PBFNC-8 Runtime Boundary Audit

PBFNC-8 verifies that PawnBlock closeout does not open public or raw runtime surfaces.

PBFNC-8 still closed:

- public route `200`
- production API
- public/user-facing LLM narration
- raw BoardFacts output
- raw Story output
- raw PawnBlockProof output
- raw EngineCheck output
- raw PV
- proofFailures public payload

PBFNC-8 route boundary:

- `/api/commentary/render` remains fail-closed
- `/internal/commentary/render-local-probe` remains fail-closed

PBFNC-8 runtime handoff boundary:

- Renderer input is `ExplanationPlan` only
- LLM smoke input fields are renderedText, claimKey, strength, and forbidden wording only
- `Verdict.values` keeps its fixed public-safe shape

Completion standard: PBFNC-8 closes when `/api/commentary/render` and `/internal/commentary/render-local-probe` stay registered only as fail-closed tombstones with no `200`; production API and public/user-facing LLM narration remain closed; raw BoardFacts, raw Story, raw PawnBlockProof, raw EngineCheck, raw PV, and proofFailures never become public payload; Renderer accepts only ExplanationPlan; LLM smoke receives only renderedText, claimKey, strength, and forbidden wording contract fields; and `Verdict.values` keeps its fixed public-safe numeric shape without raw diagnostics.

### PBFNC Final Completion Standard

PBFNC final completion standard:

- `Scene.PawnBlock` is the only open Pawn Blocking / Fixed Pawn Neighborhood positive Story
- `PawnBlockProof`, `Scene.PawnBlock`, and `blocks_pawn` keep separate proof-home, Story-label, and speech-key authority
- PawnBlock owns no PawnStop, PawnBreak, PawnCapture, PassedPawnCreated, or FileOpened meaning
- PawnBlock owns no Material, Hanging, Defense, or Line / Defender tactic meaning
- fixed pawn, weak pawn, blockade, restriction, pressure, and initiative remain outside live positive authority
- StoryTable input order is stable
- non-selected, capped, and refuted rows produce no standalone text
- renderer and LLM wording is no stronger than `blocks_pawn`
- detailed authority lives only in `StoryInteractionLaw.md`
- summary docs remain summaries only
- public route, production API, and public/user-facing LLM narration remain closed

PBFNC final verification:

- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

## King Check Neighborhood

### CheckGiven-0 Charter

CheckGiven-0 opens only the first King Check Neighborhood vertical slice.

First positive scope:

- a legal move gives check to the rival king on the exact after-board

Player-facing bounded wording:

- This move gives check.

Core sentence:

CheckGivenProof proves only the exact legal checking-move event. Scene.CheckGiven may speak only that event, not checkmate, mate threat, king safety, attack, initiative, pressure, force, best move, winning, or no-counterplay meaning.

CheckGiven-0 opens only:

- narrow `Scene.CheckGiven`
- `CheckGivenProof`
- legal move
- checking side
- rival side
- rival king square after the move
- after-board rival king is in check
- exact-board legal replay
- same-board proof
- selected Verdict after bounded gives-check wording only

CheckGiven-0 does not open:

- mate threat
- checkmate
- king safety
- unsafe king
- attack
- initiative
- pressure
- forced move
- best move
- only move
- winning
- decisive
- no counterplay
- public route `200`
- production API
- public/user-facing LLM narration

CheckGiven-0 authority split:

- `CheckGivenProof` proves only that one legal same-board move leaves the rival king in check.
- `Scene.CheckGiven` owns only the bounded public Story identity for that event.
- `gives_check` owns only the speech key for the bounded checking-move event.
- BoardFacts may observe legal checking moves and check counts, but it does not create a public Story.
- StoryTable may order an existing `Scene.CheckGiven`; it must not create one.
- ExplanationPlan may lower only a selected uncapped Lead `Scene.CheckGiven` Verdict to `gives_check` after its stage opens.
- Renderer and LLM smoke may phrase only the bounded checking event after their downstream stages explicitly open.

CheckGiven-0 stays silent for:

- illegal move
- missing same-board proof
- missing exact-board replay
- after-board rival king not in check
- missing rival king square
- incomplete StoryProof
- incomplete CheckGivenProof
- writerless or contaminated rows
- mate-threat, checkmate, king-safety, attack, pressure, initiative, forced, best-move, only-move, winning, decisive, or no-counterplay wording

Completion standard: CheckGiven-0 closes when only a legal same-board move that leaves the rival king in check can become narrow `Scene.CheckGiven` through `CheckGivenProof`; illegal moves, missing same-board proof, missing replay, missing rival king identity, after-board no-check states, incomplete proof, writerless rows, and contaminated rows stay silent; mate threat, checkmate, king safety, unsafe king, attack, initiative, pressure, forced, best, only, winning, decisive, no-counterplay, public route `200`, production API, and public/user-facing LLM narration remain closed; and detailed CheckGiven authority lives only in `StoryInteractionLaw.md`.

### CheckGiven-1 CheckGivenProof

CheckGiven-1 opens only `CheckGivenProof` as the proof home for the first King Check Neighborhood slice.

CheckGiven-1 proves:

- checking side
- rival side
- legal move identity
- moving piece identity when available
- origin square
- destination square
- rival king square after the move
- after-board rival king is in check
- exact after-board replay
- same-board proof
- check was produced by this legal move

CheckGiven-1 first scope:

- any legal move whose exact after-board has the rival king in check
- ordinary check, discovered check, double check, and promotion check are allowed only as gives-check events
- no public distinction between check types

CheckGiven-1 closed scope:

- checkmate claim
- mate threat claim
- king-safety diagnosis
- unsafe-king claim
- attack claim
- initiative / pressure
- forced reply
- best move / only move
- winning / decisive
- no counterplay

CheckGiven-1 proof-home boundary:

- CheckGivenProof is not a public Story.
- CheckGivenProof must not create `Scene.CheckGiven`.
- CheckGivenProof must not create a Verdict, ExplanationPlan, renderer text, LLM input, public route payload, or production API response.
- BoardFacts legal check counts remain observations only.
- CheckGivenProof does not replace `LineProof`, `PinProof`, `SkewerProof`, `RemoveGuardProof`, Material proof, `DefenseProof`, or pawn proof homes.
- CheckGivenProof must not own mate, checkmate, king safety, attack, pressure, initiative, forced, best, only, winning, decisive, or no-counterplay meaning.

Completion standard: CheckGiven-1 closes when `CheckGivenProof` proves only the checking side, rival side, legal move identity, origin, destination, rival king square, after-board rival-king-in-check state, exact after-board replay, same-board proof, and check-produced-by-this-move for one legal move; checkmate, mate threat, king safety, unsafe king, attack, pressure, initiative, forced, best, only, winning, decisive, and no-counterplay meanings remain closed; CheckGivenProof is not a public Story; and no new Story writer, Verdict, ExplanationPlan, renderer, LLM, public route `200`, production API, or public/user-facing LLM narration opens.

### CheckGiven-2 Scene.CheckGiven Writer

CheckGiven-2 opens only `SceneCheckGiven` as the named writer for `Scene.CheckGiven`.

CheckGiven-2 writer conditions:

- complete StoryProof
- complete CheckGivenProof
- same-board legal replay
- legal move gives check on the exact after-board
- rival king square is identified
- after-board rival king is in check
- writer = `SceneCheckGiven`
- EngineCheck does not Refute

CheckGiven-2 Story identity:

- scene = `CheckGiven`
- tactic = None
- plan = None
- side = checking side
- rival = checked king side
- target = rival king square after the move
- anchor = moving piece origin square
- route = legal checking move

CheckGiven-2 forbidden writer ownership:

- SceneCheckGiven must not create mate threat.
- SceneCheckGiven must not create checkmate.
- SceneCheckGiven must not create king safety or unsafe-king meaning.
- SceneCheckGiven must not create attack, pressure, initiative, force, best-move, only-move, winning, decisive, or no-counterplay meaning.
- SceneCheckGiven must not steal Line, Pin, Skewer, RemoveGuard, Material, Defense, Hanging, Fork, or pawn meanings.

CheckGiven-2 downstream boundary:

- CheckGiven-2 does not open ExplanationPlan.
- CheckGiven-2 does not open renderer text.
- CheckGiven-2 does not open LLM input.
- CheckGiven-2 does not open public route `200`, production API, or public/user-facing LLM narration.

Completion standard: CheckGiven-2 closes when `SceneCheckGiven` writes only exact complete `CheckGivenProof` rows into `Scene.CheckGiven` with no tactic, no plan, checking side, rival side, target as the rival king square, anchor as the moving piece origin square, route as the legal checking move, no EngineCheck Refutes lead, and no mate, checkmate, king-safety, attack, pressure, initiative, forced, best, only, winning, decisive, no-counterplay, sibling tactic, material, defense, or pawn meaning; downstream ExplanationPlan, renderer, LLM, public route `200`, production API, and public/user-facing LLM narration remain closed.

### CheckGiven-3 Negative Corpus

CheckGiven-3 builds the negative corpus for `CheckGiven`.

CheckGiven-3 must stay silent for:

- legal move missing
- illegal move
- same-board proof missing
- exact-board replay missing
- after-board rival king not in check
- rival king square missing
- route mismatch
- stale before/after board
- incomplete StoryProof
- incomplete CheckGivenProof
- writerless row
- contaminated sidecar row
- BoardFacts check count without bound move proof
- engine line showing check without an existing Story
- raw PV checking move without an existing Story
- checkmate wording
- mate-threat wording
- king-safety wording
- attack wording
- initiative wording
- pressure wording
- forced wording
- best-move wording
- only-move wording
- winning / decisive / no-counterplay wording

CheckGiven-3 standard:

- A legal checking move is not a mate threat claim.
- A legal checking move is not a checkmate claim.
- A legal checking move is not a king-safety diagnosis.
- A legal checking move is not an attack, pressure, initiative, best-move, only-move, forced, winning, decisive, or no-counterplay claim.
- Complete CheckGivenProof or silence.
- SceneCheckGiven must not open ExplanationPlan, renderer text, LLM input, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: CheckGiven-3 closes when the negative corpus proves that illegal moves, missing same-board proof, missing replay, missing rival king identity, no after-board check, route mismatch, stale board proof, incomplete StoryProof, incomplete CheckGivenProof, writerless rows, contaminated rows, BoardFacts-only check counts, engine-only checking lines, raw PV checks, and mate-threat, checkmate, king-safety, attack, pressure, initiative, forced, best, only, winning, decisive, or no-counterplay wording all stay silent under CheckGivenProof, SceneCheckGiven, StoryTable, and downstream ExplanationPlan boundaries; CheckGiven remains only the exact legal same-board event that the move gives check.

### CheckGiven-4 EngineCheck Reuse

CheckGiven-4 reuses only the existing `EngineCheck` boundary for `Scene.CheckGiven`.

CheckGiven-4 rules:

- EngineCheck cannot create `Scene.CheckGiven`.
- Supports does not create a new claim.
- Caps suppresses standalone claim speech or keeps downstream speech bounded to `gives_check`.
- Refutes makes the `Scene.CheckGiven` Story Blocked.
- Unknown creates no engine expression.
- Engine evidence must bind to the same Story route and same legal line.

CheckGiven-4 forbidden engine wording:

- engine says this is check
- eval number
- best move
- only move
- forced check
- winning check
- decisive check
- mate threat
- checkmate
- raw PV explanation
- no counterplay

CheckGiven-4 downstream boundary:

- CheckGiven-4 does not open ExplanationPlan for `Scene.CheckGiven`.
- CheckGiven-4 does not open renderer text for `Scene.CheckGiven`.
- CheckGiven-4 does not open LLM input for `Scene.CheckGiven`.
- CheckGiven-4 does not open public route `200`, production API, or public/user-facing LLM narration.

Completion standard: CheckGiven-4 closes when EngineCheck attaches only to an already complete same-board `SceneCheckGiven` Story, cannot produce `Scene.CheckGiven` from engine evidence alone, Supports adds no chess claim, Caps suppresses or bounds downstream speech without opening CheckGiven wording early, Refutes blocks the CheckGiven row, Unknown creates no engine expression, and engine-says, eval-number, best, only, forced, winning, decisive, mate-threat, checkmate, raw-PV, and no-counterplay wording remain closed.

### CheckGiven-5 StoryTable Integration

CheckGiven-5 integrates `Scene.CheckGiven` into StoryTable only as an already existing proof-backed Story.

CheckGiven-5 collision targets:

- `Scene.CheckGiven`
- `Scene.Material`
- `Tactic.Hanging`
- `Tactic.Fork`
- `Scene.Defense`
- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`
- open pawn/file scenes:
- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Scene.PromotionThreat`
- `Scene.Promotion`
- `Scene.PawnBreak`
- `Scene.PawnCapture`
- `Scene.PassedPawnCreated`
- `Scene.FileOpened`
- `Scene.PawnBlock`

CheckGiven-5 verifies:

- input order stability
- Scene.CheckGiven does not own mate threat.
- Scene.CheckGiven does not own checkmate.
- Scene.CheckGiven does not own king safety.
- Scene.CheckGiven does not own attack, pressure, initiative, force, best, only, winning, decisive, or no-counterplay meaning.
- Existing material, defense, line/defender, hanging, fork, and pawn/file claim homes keep their own proof and speech homes.
- Capped or refuted Scene.CheckGiven has no standalone text.
- Support, Context, and Blocked CheckGiven rows do not lower to standalone text.

CheckGiven-5 StoryTable boundary:

- StoryTable may select an existing complete `Scene.CheckGiven` row.
- StoryTable must not create `Scene.CheckGiven`.
- Scene.CheckGiven may lead only as the exact checking-move event.
- Selection still does not open renderer text, LLM input, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: CheckGiven-5 closes when StoryTable ordering is stable across input order, Scene.CheckGiven keeps only the bounded legal-checking-move event, every already-open claim home keeps its own proof and speech ownership, Scene.CheckGiven never steals mate, checkmate, king-safety, attack, pressure, initiative, force, best, only, winning, decisive, no-counterplay, material, defense, line/defender, hanging, fork, or pawn/file meaning, and capped, refuted, Support, Context, or Blocked CheckGiven rows produce no standalone text.

### CheckGiven-6 ExplanationPlan

CheckGiven-6 opens only `ExplanationPlan` for selected uncapped Lead `Scene.CheckGiven` Verdicts.

CheckGiven-6 allowed claim key:

- `gives_check`

CheckGiven-6 forbidden claim keys:

- `mate_threat`
- `checkmate`
- `king_unsafe`
- `attacks_king`
- `creates_attack`
- `creates_pressure`
- `takes_initiative`
- `forces_reply`
- `best_move`
- `only_move`
- `winning`
- `decisive`
- `no_counterplay`

CheckGiven-6 input boundary:

- ExplanationPlan.fromSelected accepts only selected uncapped Lead `Scene.CheckGiven` Verdicts.
- The Story must still be `Scene.CheckGiven`, tactic = None, plan = None, writer = `SceneCheckGiven`, and same-board proof-backed.
- target remains the rival king square after the move.
- anchor remains the moving piece origin square.
- route remains the legal checking move.
- evidenceLine is only that route.

CheckGiven-6 no-standalone boundary:

- Support, Context, Blocked, capped, and refuted `Scene.CheckGiven` rows have no standalone claim.
- EngineCheck Supports does not create a new claim; it may accompany the already selected uncapped Lead claim only.

CheckGiven-6 downstream boundary:

- CheckGiven-6 does not open renderer text.
- CheckGiven-6 does not open LLM input.
- CheckGiven-6 does not open public route `200`, production API, or public/user-facing LLM narration.

Completion standard: CheckGiven-6 closes when `ExplanationPlan.fromSelected` lowers only selected uncapped Lead `Scene.CheckGiven` Verdicts to `gives_check` with target as the rival king square, anchor as the moving piece origin square, route and evidenceLine as the legal checking move, complete same-board CheckGivenProof still bound to the Story, Support, Context, Blocked, capped, and refuted rows silent, EngineCheck Supports adding no new claim, forbidden mate-threat, checkmate, king-unsafe, king-attack, attack-creation, pressure, initiative, forced-reply, best, only, winning, decisive, and no-counterplay claim keys closed, and renderer, LLM, public route `200`, production API, and public/user-facing LLM narration still closed.

### CheckGiven-7 DeterministicRenderer

CheckGiven-7 opens deterministic renderer text only from bounded `CheckGiven` `ExplanationPlan`.

CheckGiven-7 renderer input boundary:

- Renderer input is `ExplanationPlan` only.
- Renderer does not accept Story, Verdict, CheckGivenProof, BoardFacts, EngineCheck, engine eval, raw PV, proofFailures, or source rows.
- Renderer may phrase only selected uncapped Lead `Scene.CheckGiven` plans with claimKey = `gives_check`.
- Support, Context, Blocked, capped, refuted, malformed, or sibling-claim plans have no standalone renderer text.

CheckGiven-7 allowed renderer templates:

- `{route} gives check.`
- `{route} checks the king.`

CheckGiven-7 forbidden renderer wording:

- threatens mate
- checkmate
- mates
- the king is unsafe
- starts an attack
- creates pressure
- takes the initiative
- forces
- best move
- only move
- winning
- decisive
- no counterplay
- engine says

CheckGiven-7 downstream boundary:

- CheckGiven-7 does not open LLM narration, public route `200`, production API, mate threat, checkmate, king safety, attack, pressure, initiative, forced, best move, only move, winning, decisive, or no-counterplay meaning.
- Renderer text may mean only: this legal move gives check to the rival king on the exact after-board.

Completion standard: CheckGiven-7 closes when `DeterministicRenderer` accepts only a bounded `gives_check` `ExplanationPlan`, emits only bounded check-given text, rejects Support, Context, Blocked, capped, refuted, malformed, or sibling-claim plans, keeps forbidden mate, checkmate, king-safety, attack, pressure, initiative, force, best, only, winning, decisive, no-counterplay, and engine wording absent, and leaves public/user-facing LLM narration closed.

### CheckGiven-8 LLM Smoke

CheckGiven-8 reuses only the existing 8B LLM smoke boundary for selected bounded CheckGiven `RenderedLine` rephrases.

CheckGiven-8 allowed LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

CheckGiven-8 forbidden LLM smoke input:

- raw Story
- raw CheckGivenProof
- BoardFacts
- EngineCheck
- engine eval
- raw PV
- proofFailures
- source rows

CheckGiven-8 forbidden LLM smoke additions:

- new move
- new line
- mate threat
- checkmate
- king safety
- unsafe king
- attack
- initiative
- pressure
- forced reply
- best move
- only move
- winning
- decisive
- no counterplay
- engine mention

CheckGiven-8 downstream boundary:

- CheckGiven-8 does not open public/user-facing LLM narration, public route `200`, production API, raw proof input, raw engine input, or any new chess meaning.
- LLM smoke may echo or rephrase only the already rendered bounded `gives_check` event.
- LLM smoke may not add mate, checkmate, king-safety, attack, pressure, initiative, force, best, only, winning, decisive, no-counterplay, new move, new line, or engine explanation.

Completion standard: CheckGiven-8 closes when `LlmNarrationSmoke` accepts only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for bounded `gives_check`; rejects raw Story, CheckGivenProof, BoardFacts, EngineCheck, raw PV, proofFailures, new moves, new lines, mate, checkmate, king-safety, attack, pressure, initiative, forced, best, only, winning, decisive, no-counterplay, and engine additions; and keeps public narration and production API closed.

### CheckGiven-9 Closeout / Hard Cleanup

CheckGiven-9 opens no new chess meaning.

CheckGiven-9 closes only the already-open narrow event:

a legal move gives check to the rival king on the exact after-board

CheckGiven-9 authority audit:

- CheckGivenProof owns the proof home.
- `Scene.CheckGiven` owns the Story label.
- SceneCheckGiven owns the named writer.
- `gives_check` owns the speech key.
- These authorities are not interchangeable.
- BoardFacts legal check counts and check observations remain observations only.
- EngineCheck may support, cap, refute, or remain unknown only for an existing `Scene.CheckGiven` Story, and cannot create one.

CheckGiven-9 duplication audit:

- one chess meaning: legal move gives check
- one proof home: CheckGivenProof
- one Story label: `Scene.CheckGiven`
- one named writer: SceneCheckGiven
- one speech key: `gives_check`
- one detailed live authority document: `StoryInteractionLaw.md`

CheckGiven-9 collision audit:

- `Scene.CheckGiven` owns no mate threat.
- `Scene.CheckGiven` owns no checkmate.
- `Scene.CheckGiven` owns no king safety or unsafe-king meaning.
- `Scene.CheckGiven` owns no attack, pressure, initiative, force, best, only, winning, decisive, or no-counterplay meaning.
- `Scene.CheckGiven` owns no Material, Hanging, Fork, Defense, Line, Defender, or pawn/file meaning.
- Existing opened Story labels keep their proof homes, Story labels, and speech keys.

CheckGiven-9 downstream audit:

- `gives_check` says only that the legal move gives check on the exact after-board.
- Renderer and LLM smoke wording must stay no stronger than bounded `gives_check`.
- Support, Context, Blocked, capped, and refuted rows have no standalone CheckGiven text.
- Raw proof failures, raw engine eval, raw PV, source rows, and BoardFacts check counts never become public wording.

CheckGiven-9 documentation audit:

- Detailed CheckGiven stage law, negative corpus, closeout checklist, forbidden wording, and duplication audit live only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, and Manifest may summarize only if docs tests require it.
- AGENTS.md remains unchanged unless durable operator rules change.

CheckGiven-9 public-surface audit:

- public route `200` remains closed.
- production API remains closed.
- public/user-facing LLM narration remains closed.
- Existing LLM smoke remains bounded internal smoke only.

Completion standard: CheckGiven-9 closes when CheckGivenProof, `Scene.CheckGiven`, SceneCheckGiven, and `gives_check` remain separated by layer; exactly one narrow public event is open; CheckGiven owns no mate, checkmate, king-safety, attack, pressure, initiative, force, best, only, winning, decisive, no-counterplay, Material, Hanging, Fork, Defense, Line, Defender, or pawn/file meaning; renderer and LLM smoke output stays no stronger than `gives_check`; detailed authority stays only in `StoryInteractionLaw.md`; summary docs remain summaries; public route `200`, production API, and public/user-facing LLM narration stay closed; targeted runtime tests pass; docs authority tests pass when touched; and git diff --check passes.

### CheckEscaped-0 Charter

CheckEscaped-0 opens only the second King Check Neighborhood vertical slice.

First positive scope:

before-board side king is in check, the side plays a legal move, and the exact after-board side king is not in check

Player-facing bounded wording:

This move gets out of check.

Core sentence:

CheckEscapedProof proves only the exact legal check-escape event. Scene.CheckEscaped may speak only that event, not how the check was escaped, not checkmate avoidance, not king safety, not defense quality, not forced move, not best move, and not winning or decisive meaning.

CheckEscaped-0 opens only:

- narrow `Scene.CheckEscaped`
- `CheckEscapedProof`
- legal move
- escaping side
- rival side
- side king square before the move
- before-board side king is in check
- side king square after the move
- after-board side king is not in check
- exact-board legal replay
- same-board proof
- selected Verdict after bounded check-escape wording only

CheckEscaped-0 does not open:

- `Scene.KingMovedOutOfCheck`
- `Scene.CheckBlocked`
- `Scene.CheckingPieceCaptured`
- `king_escapes_check`
- `blocks_check`
- `captures_checker`
- mate threat
- checkmate
- avoids mate
- king safety
- safe king
- unsafe king
- defense success beyond escaping check
- refutes attack
- forced move
- only move
- best move
- winning
- decisive
- no counterplay
- public route `200`
- production API
- public/user-facing LLM narration

CheckEscaped-0 authority split:

- `CheckEscapedProof` proves only that one legal same-board move changes the moving side from in check before the move to not in check after the move.
- `Scene.CheckEscaped` owns only the bounded public Story identity for that check-escape event.
- `escapes_check` owns only the speech key for the bounded check-escape event.
- BoardFacts may observe check state, legal moves, king squares, check counts, and legal replay, but it does not own this public event proof.
- Escape method details such as king move, interposition, or checking-piece capture may be internal proof detail only. They are not public Story labels and not speech keys.
- StoryTable may order an existing `Scene.CheckEscaped`; it must not create one.
- ExplanationPlan may lower only a selected uncapped Lead `Scene.CheckEscaped` Verdict to `escapes_check`.
- Renderer and LLM smoke may phrase only the bounded check-escape event; public/user-facing LLM narration remains closed.

CheckEscaped-0 stays silent for:

- illegal move
- missing same-board proof
- missing exact-board replay
- before-board side king not in check
- after-board side king still in check
- missing before king square
- missing after king square
- missing legal move identity
- incomplete StoryProof
- incomplete CheckEscapedProof
- writerless or contaminated rows
- claims that the move was forced, best, only, winning, decisive, or no-counterplay
- claims that the king is now safe beyond the exact not-in-check state
- claims that the move blocks check, captures the checker, or moves the king as standalone public meaning
- mate-threat, checkmate, avoids-mate, king-safety, defense-success, refutes-attack, pressure, initiative, forced, best-move, only-move, winning, decisive, or no-counterplay wording

CheckEscaped-0 duplication audit:

- one chess meaning: legal move gets out of check
- one proof home: CheckEscapedProof
- one Story label: `Scene.CheckEscaped`
- one named writer: SceneCheckEscaped
- one speech key: `escapes_check`
- one detailed live authority document: `StoryInteractionLaw.md`

Completion standard: CheckEscaped-0 closes when only a legal same-board move from a before-board check state to an exact after-board non-check state can become narrow `Scene.CheckEscaped` through `CheckEscapedProof`; illegal moves, missing same-board proof, missing replay, before-board non-check states, after-board still-check states, missing king identity, incomplete proof, writerless rows, and contaminated rows stay silent; `Scene.KingMovedOutOfCheck`, `Scene.CheckBlocked`, `Scene.CheckingPieceCaptured`, `king_escapes_check`, `blocks_check`, `captures_checker`, mate threat, checkmate, avoids mate, king safety, safe king, defense success, refutes attack, forced, best, only, winning, decisive, no-counterplay, public route `200`, production API, and public/user-facing LLM narration remain closed; and detailed CheckEscaped authority lives only in `StoryInteractionLaw.md`.

### CheckEscaped-1 CheckEscapedProof

CheckEscaped-1 opens only `CheckEscapedProof` as the proof home for the second King Check Neighborhood slice.

CheckEscaped-1 proves:

- escaping side
- rival side
- legal move identity
- moving piece identity when available
- origin square
- destination square
- side king square before the move
- before-board side king is in check
- side king square after the move
- after-board side king is not in check
- exact before-board state
- exact after-board replay
- same-board proof
- check escape was produced by this legal move

CheckEscaped-1 first scope:

- any legal move that starts with the moving side in check and ends with that same side not in check on the exact after-board
- king move, interposition, checking-piece capture, discovered counter-check, and promotion escape are allowed only as internal proof shapes for the single escape event
- no public distinction between escape methods

CheckEscaped-1 closed scope:

- public `Scene.KingMovedOutOfCheck`
- public `Scene.CheckBlocked`
- public `Scene.CheckingPieceCaptured`
- `king_escapes_check`
- `blocks_check`
- `captures_checker`
- checkmate avoidance
- mate threat
- king-safety diagnosis
- safe-king claim
- defense-success claim
- forced reply
- best move / only move
- winning / decisive
- no counterplay

Proof-home boundary:

- `CheckEscapedProof` is not a public Story.
- `CheckEscapedProof` must not create `Scene.CheckEscaped`.
- `CheckEscapedProof` must not create a Verdict, ExplanationPlan, renderer text, LLM input, public route payload, or production API response.
- BoardFacts check state and legal replay remain observations only.
- `CheckEscapedProof` does not replace `CheckGivenProof`, `DefenseProof`, `LineProof`, `PinProof`, `SkewerProof`, `RemoveGuardProof`, Material proof, or pawn proof homes.
- Escape-method detail may be diagnostic only. It must not become public Story identity, speech key, renderer wording, or LLM input.
- `CheckEscapedProof` must not own mate, checkmate, king safety, attack, pressure, initiative, forced, best, only, winning, decisive, or no-counterplay meaning.

Completion standard: CheckEscaped-1 closes when `CheckEscapedProof` proves only the escaping side, rival side, legal move identity, origin, destination, before king square, before-board in-check state, after king square, after-board not-in-check state, exact after-board replay, same-board proof, and check-escape-produced-by-this-move for one legal move; `Scene.KingMovedOutOfCheck`, `Scene.CheckBlocked`, `Scene.CheckingPieceCaptured`, `king_escapes_check`, `blocks_check`, `captures_checker`, checkmate avoidance, mate threat, king safety, safe-king, defense-success, forced, best, only, winning, decisive, and no-counterplay meanings remain closed; `CheckEscapedProof` is not a public Story; and no new Story writer, Verdict, ExplanationPlan, renderer, LLM, public route `200`, production API, or public/user-facing LLM narration opens.

### CheckEscaped-2 Scene.CheckEscaped Writer

CheckEscaped-2 opens only `SceneCheckEscaped` as the named writer for `Scene.CheckEscaped`.

CheckEscaped-2 writer conditions:

- complete StoryProof
- complete `CheckEscapedProof`
- same-board legal replay
- legal move starts from a before-board check state for the moving side
- legal move leaves that same side not in check on the exact after-board
- before king square is identified
- after king square is identified
- writer = `SceneCheckEscaped`
- EngineCheck does not Refute

CheckEscaped-2 Story identity:

- scene = `CheckEscaped`
- tactic = None
- plan = None
- side = escaping side
- rival = checking side / rival side
- target = side king square after the move
- anchor = moving piece origin square
- route = legal escape move

CheckEscaped-2 forbidden writer ownership:

- `SceneCheckEscaped` must not create `Scene.KingMovedOutOfCheck`.
- `SceneCheckEscaped` must not create `Scene.CheckBlocked`.
- `SceneCheckEscaped` must not create `Scene.CheckingPieceCaptured`.
- `SceneCheckEscaped` must not create `king_escapes_check`, `blocks_check`, or `captures_checker`.
- `SceneCheckEscaped` must not create mate threat, checkmate, avoids-mate, king-safety, safe-king, unsafe-king, defense-success, refutes-attack, force, best-move, only-move, winning, decisive, or no-counterplay meaning.
- `SceneCheckEscaped` must not steal `Scene.CheckGiven`, Line, Pin, Skewer, RemoveGuard, Material, Defense, Hanging, Fork, or pawn meanings.

CheckEscaped-2 downstream boundary:

- CheckEscaped-2 does not open ExplanationPlan.
- CheckEscaped-2 does not open renderer text.
- CheckEscaped-2 does not open LLM input.
- CheckEscaped-2 does not open public route `200`, production API, or public/user-facing LLM narration.

Completion standard: CheckEscaped-2 closes when `SceneCheckEscaped` writes only exact complete `CheckEscapedProof` rows into `Scene.CheckEscaped` with no tactic, no plan, escaping side, rival side, target as the escaping side king square after the move, anchor as the moving piece origin square, route as the legal escape move, no EngineCheck Refutes lead, and no public king-moved, check-blocked, checker-captured, mate, checkmate, king-safety, defense-success, pressure, initiative, forced, best, only, winning, decisive, no-counterplay, sibling CheckGiven, tactic, material, defense, or pawn meaning; downstream ExplanationPlan, renderer, LLM, public route `200`, production API, and public/user-facing LLM narration remain closed.

### CheckEscaped-3 Negative Corpus

CheckEscaped-3 builds the negative corpus for `CheckEscaped`.

CheckEscaped-3 must stay silent for:

- legal move missing
- illegal move
- same-board proof missing
- exact-board replay missing
- before-board side king not in check
- after-board side king still in check
- side king missing before the move
- side king missing after the move
- route mismatch
- stale before/after board
- incomplete StoryProof
- incomplete CheckEscapedProof
- writerless row
- contaminated sidecar row
- BoardFacts in-check bit without bound move proof
- legal move count without bound move proof
- engine line showing an escape without an existing Story
- raw PV escape without an existing Story
- public king-moved-out-of-check wording
- public blocks-check wording
- public captures-checker wording
- checkmate-avoidance wording
- mate-threat wording
- king-safety wording
- safe-king wording
- defense-success wording
- refutes-attack wording
- initiative wording
- pressure wording
- forced wording
- best-move wording
- only-move wording
- winning / decisive / no-counterplay wording

CheckEscaped-3 standard:

- Escaping check is not checkmate avoidance.
- Escaping check is not a king-safety diagnosis.
- Escaping check is not proof that the move was forced, best, or only.
- Escaping check is not a defense-success claim beyond the exact not-in-check after-board state.
- Escape method detail is not a public Story.
- Complete `CheckEscapedProof` or silence.
- `SceneCheckEscaped` must not open ExplanationPlan, renderer text, LLM input, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: CheckEscaped-3 closes when the negative corpus proves that illegal moves, missing same-board proof, missing replay, before-board non-check states, after-board still-check states, missing king identity, route mismatch, stale board proof, incomplete StoryProof, incomplete CheckEscapedProof, writerless rows, contaminated rows, BoardFacts-only check state, legal-count-only input, engine-only escape lines, raw PV escapes, and public king-moved, blocks-check, captures-checker, mate-avoidance, checkmate, king-safety, safe-king, defense-success, refutes-attack, pressure, initiative, forced, best, only, winning, decisive, or no-counterplay wording all stay silent under `CheckEscapedProof`, `SceneCheckEscaped`, StoryTable, and downstream ExplanationPlan boundaries; CheckEscaped remains only the exact legal same-board event that the moving side gets out of check.

### CheckEscaped-4 EngineCheck Reuse

CheckEscaped-4 reuses only the existing `EngineCheck` boundary for `Scene.CheckEscaped`.

CheckEscaped-4 rules:

- EngineCheck cannot create `Scene.CheckEscaped`.
- Supports does not create a new claim.
- Caps suppresses standalone claim speech or keeps downstream speech bounded to `escapes_check`.
- Refutes makes the `Scene.CheckEscaped` Story Blocked.
- Unknown creates no engine expression.
- Engine evidence must bind to the same Story route and same legal line.

CheckEscaped-4 forbidden engine wording:

- engine says this escapes check
- eval number
- best move
- only move
- forced move
- forced escape
- winning escape
- decisive escape
- avoids mate
- checkmate defense
- raw PV explanation
- no counterplay

CheckEscaped-4 downstream boundary:

- CheckEscaped-4 does not open ExplanationPlan for `Scene.CheckEscaped`.
- CheckEscaped-4 does not open renderer text for `Scene.CheckEscaped`.
- CheckEscaped-4 does not open LLM input for `Scene.CheckEscaped`.
- CheckEscaped-4 does not open public route `200`, production API, or public/user-facing LLM narration.

Completion standard: CheckEscaped-4 closes when EngineCheck attaches only to an already complete same-board `SceneCheckEscaped` Story, cannot produce `Scene.CheckEscaped` from engine evidence alone, Supports adds no chess claim, Caps suppresses or bounds downstream speech without opening CheckEscaped wording early, Refutes blocks the CheckEscaped row, Unknown creates no engine expression, and engine-says, eval-number, best, only, forced, winning, decisive, avoids-mate, checkmate-defense, raw-PV, and no-counterplay wording remain closed.

### CheckEscaped-5 StoryTable Integration

CheckEscaped-5 integrates `Scene.CheckEscaped` into StoryTable only as an
already existing proof-backed Story.

CheckEscaped-5 collision targets:

- `Scene.CheckEscaped`
- `Scene.CheckGiven`
- `Scene.Material`
- Tactic.Hanging
- Tactic.Fork
- `Scene.Defense`
- Tactic.DiscoveredAttack
- Tactic.Pin
- Tactic.RemoveGuard
- Tactic.Skewer
- open pawn/file scenes:
  - `Scene.PawnAdvance`
  - `Scene.PawnStop`
  - `Scene.PromotionThreat`
  - `Scene.Promotion`
  - `Scene.PawnBreak`
  - `Scene.PawnCapture`
  - `Scene.PassedPawnCreated`
  - `Scene.FileOpened`
  - `Scene.PawnBlock`

CheckEscaped-5 verifies:

- input order stability.
- `Scene.CheckEscaped` does not own `Scene.CheckGiven` meaning.
- `Scene.CheckGiven` does not own `Scene.CheckEscaped` meaning.
- cross-check positions may have both proof-backed rows, but each row keeps its own proof home, Story label, and speech key.
- `Scene.CheckEscaped` does not own `Scene.KingMovedOutOfCheck`, `Scene.CheckBlocked`, or `Scene.CheckingPieceCaptured`.
- `Scene.CheckEscaped` does not own mate threat, checkmate, avoids-mate, king safety, safe king, attack, pressure, initiative, force, best, only, winning, decisive, or no-counterplay meaning.
- existing material, defense, line/defender, hanging, fork, CheckGiven, and pawn/file claim homes keep their own proof and speech homes.
- capped or refuted `Scene.CheckEscaped` has no standalone text.
- Support, Context, and Blocked CheckEscaped rows do not lower to standalone text.

CheckEscaped-5 StoryTable boundary:

- StoryTable may select an existing complete `Scene.CheckEscaped` row.
- StoryTable must not create `Scene.CheckEscaped`.
- `Scene.CheckEscaped` may lead only as the exact check-escape event.
- if CheckEscaped and CheckGiven both exist for a cross-check move, selection order must be deterministic and neither row may borrow the other's claim key.
- selection still does not open renderer text, LLM input, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: CheckEscaped-5 closes when StoryTable ordering is stable across input order, `Scene.CheckEscaped` keeps only the bounded legal check-escape event, `Scene.CheckGiven` keeps only the bounded legal gives-check event, cross-check rows remain separated by proof home, Story label, writer, and speech key, every already-open claim home keeps its own proof and speech ownership, `Scene.CheckEscaped` never steals public king-moved, check-blocked, checker-captured, mate, checkmate, king-safety, attack, pressure, initiative, force, best, only, winning, decisive, no-counterplay, material, defense, line/defender, hanging, fork, or pawn/file meaning, and capped, refuted, Support, Context, or Blocked CheckEscaped rows produce no standalone text.

### CheckEscaped-6 ExplanationPlan

CheckEscaped-6 opens only `ExplanationPlan` for selected uncapped Lead
`Scene.CheckEscaped` Verdicts.

CheckEscaped-6 allowed claim key:

- `escapes_check`

CheckEscaped-6 forbidden claim keys:

- `king_escapes_check`
- `blocks_check`
- `captures_checker`
- `gives_check`
- `avoids_mate`
- `mate_threat`
- `checkmate`
- `king_safe`
- `king_unsafe`
- `refutes_attack`
- `defends_position`
- `creates_attack`
- `creates_pressure`
- `takes_initiative`
- `forces_reply`
- `best_move`
- `only_move`
- `winning`
- `decisive`
- `no_counterplay`

CheckEscaped-6 input boundary:

- `ExplanationPlan.fromSelected` accepts only selected uncapped Lead `Scene.CheckEscaped` Verdicts.
- the Story must still be `Scene.CheckEscaped`, tactic = None, plan = None, writer = `SceneCheckEscaped`, and same-board proof-backed.
- `target` remains the escaping side king square after the move.
- `anchor` remains the moving piece origin square.
- `route` remains the legal escape move.
- `evidenceLine` is only that route.
- `CheckEscapedProof` must still bind before-board in-check state and exact after-board not-in-check state to the same Story route.
- escape method details must not lower to claim keys.

CheckEscaped-6 no-standalone boundary:

- Support, Context, Blocked, capped, and refuted `Scene.CheckEscaped` rows have no standalone claim.
- EngineCheck Supports does not create a new claim; it may accompany the already selected uncapped Lead claim only.
- EngineCheck Caps suppresses standalone claim output.
- EngineCheck Refutes keeps the row Blocked.
- CheckGiven rows may lower only to `gives_check`, never to `escapes_check`.

CheckEscaped-6 downstream boundary:

- CheckEscaped-6 does not open renderer text.
- CheckEscaped-6 does not open LLM input.
- CheckEscaped-6 does not open public route `200`, production API, or public/user-facing LLM narration.
- `escapes_check` may mean only: this legal move gets the moving side out of check on the exact after-board.
- `escapes_check` may not mean king safety, checkmate avoidance, defense success, forced move, only move, best move, winning, decisive, or no counterplay.

Completion standard: CheckEscaped-6 closes when `ExplanationPlan.fromSelected` lowers only selected uncapped Lead `Scene.CheckEscaped` Verdicts to `escapes_check` with target as the escaping side king square after the move, anchor as the moving piece origin square, route and evidenceLine as the legal escape move, complete same-board `CheckEscapedProof` still bound to the Story, Support, Context, Blocked, capped, and refuted rows silent, EngineCheck Supports adding no new claim, CheckGiven retaining only `gives_check`, forbidden king-moved, check-blocked, checker-captured, avoids-mate, checkmate, king-safe, king-unsafe, attack, pressure, initiative, forced-reply, best, only, winning, decisive, and no-counterplay claim keys closed, and renderer, LLM, public route `200`, production API, and public/user-facing LLM narration still closed.

### CheckEscaped-7 DeterministicRenderer

CheckEscaped-7 opens deterministic renderer text only from bounded
`CheckEscaped` `ExplanationPlan`.

CheckEscaped-7 renderer input boundary:

- Renderer input is `ExplanationPlan` only.
- Renderer does not accept Story, Verdict, CheckEscapedProof, CheckGivenProof, BoardFacts, EngineCheck, engine eval, raw PV, proofFailures, or source rows.
- Renderer may phrase only selected uncapped Lead `Scene.CheckEscaped` plans with claimKey = `escapes_check`.
- Support, Context, Blocked, capped, refuted, malformed, or sibling-claim plans have no standalone renderer text.
- Renderer must not inspect or phrase escape method details.

CheckEscaped-7 allowed renderer templates:

- `{route} gets out of check.`
- `{route} escapes the check.`

CheckEscaped-7 forbidden renderer wording:

- moves the king out of check
- blocks the check
- captures the checking piece
- gives check
- avoids mate
- prevents checkmate
- the king is safe
- refutes the attack
- defends everything
- creates pressure
- takes the initiative
- forces
- best move
- only move
- winning
- decisive
- no counterplay
- engine says

CheckEscaped-7 downstream boundary:

- CheckEscaped-7 does not open LLM narration, public route `200`, production API, public king-moved, check-blocked, checker-captured, gives-check, mate threat, checkmate, king safety, defense-success, attack, pressure, initiative, forced, best move, only move, winning, decisive, or no-counterplay meaning.
- Renderer text may mean only: this legal move gets the moving side out of check on the exact after-board.

Completion standard: CheckEscaped-7 closes when `DeterministicRenderer` accepts only a bounded `escapes_check` `ExplanationPlan`, emits only bounded check-escape text, rejects Support, Context, Blocked, capped, refuted, malformed, or sibling-claim plans, keeps forbidden escape-method, gives-check, mate, checkmate, king-safety, defense-success, attack, pressure, initiative, force, best, only, winning, decisive, no-counterplay, and engine wording absent, and leaves public/user-facing LLM narration closed.

### CheckEscaped-8 LLM Smoke

CheckEscaped-8 reuses only the existing 8B LLM smoke boundary for selected
bounded CheckEscaped `RenderedLine` rephrases.

CheckEscaped-8 allowed LLM smoke input:

- `renderedText`
- `claimKey`
- `strength`
- forbidden wording
- `Rephrase only. Do not add chess facts.`

CheckEscaped-8 forbidden LLM smoke input:

- raw Story
- raw CheckEscapedProof
- raw CheckGivenProof
- BoardFacts
- EngineCheck
- EngineLine
- EngineEval
- raw PV
- proofFailures
- source rows
- escape method diagnostics

CheckEscaped-8 forbidden LLM smoke additions:

- new move
- new line
- king moved out of check
- blocked the check
- captured the checker
- gives check
- mate threat
- checkmate
- avoids mate
- king safety
- safe king
- unsafe king
- refutes attack
- defense success
- initiative
- pressure
- forced reply
- best move
- only move
- winning
- decisive
- no counterplay
- engine mention

CheckEscaped-8 downstream boundary:

- CheckEscaped-8 does not open public/user-facing LLM narration, public route `200`, production API, raw proof input, raw engine input, or any new chess meaning.
- LLM smoke may echo or rephrase only the already rendered bounded check-escape event.
- LLM smoke may not add how the check was escaped, whether the move also gives check, mate, checkmate, king-safety, attack, pressure, initiative, force, best, only, winning, decisive, no-counterplay, new move, new line, or engine explanation.

Completion standard: CheckEscaped-8 closes when LlmNarrationSmoke accepts only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for bounded `escapes_check`; rejects raw Story, CheckEscapedProof, CheckGivenProof, BoardFacts, EngineCheck, EngineLine, EngineEval, raw PV, proofFailures, source rows, escape method diagnostics, new moves, new lines, public escape-method claims, gives-check additions, mate, checkmate, king-safety, attack, pressure, initiative, forced, best, only, winning, decisive, no-counterplay, and engine additions; and keeps public narration and production API closed.

### CheckEscaped-9 Closeout / Hard Cleanup

CheckEscaped-9 opens no new chess meaning.

CheckEscaped-9 closes only the already-open narrow event:

a legal move gets the moving side out of check on the exact after-board

CheckEscaped-9 authority audit:

- `CheckEscapedProof` owns the proof home.
- `Scene.CheckEscaped` owns the Story label.
- `SceneCheckEscaped` owns the named writer.
- `escapes_check` owns the speech key.
- These authorities are not interchangeable.
- BoardFacts check state, legal move facts, king squares, and legal replay remain observations only.
- Escape method details remain proof diagnostics only, not public Story labels or speech keys.
- EngineCheck may support, cap, refute, or remain unknown only for an existing `Scene.CheckEscaped` Story, and cannot create one.

CheckEscaped-9 duplication audit:

- one check-given meaning: `CheckGivenProof` -> `Scene.CheckGiven` -> `SceneCheckGiven` -> `gives_check`
- one check-escaped meaning: `CheckEscapedProof` -> `Scene.CheckEscaped` -> `SceneCheckEscaped` -> `escapes_check`
- no public `Scene.KingMovedOutOfCheck`
- no public `Scene.CheckBlocked`
- no public `Scene.CheckingPieceCaptured`
- no public `king_escapes_check`
- no public `blocks_check`
- no public `captures_checker`
- one detailed live authority document: `StoryInteractionLaw.md`

CheckEscaped-9 collision audit:

- `Scene.CheckEscaped` owns no `Scene.CheckGiven` meaning.
- `Scene.CheckGiven` owns no `Scene.CheckEscaped` meaning.
- Cross-check rows keep separate proof homes, Story labels, writers, and speech keys.
- `Scene.CheckEscaped` owns no mate threat.
- `Scene.CheckEscaped` owns no checkmate.
- `Scene.CheckEscaped` owns no king safety, safe-king, or unsafe-king meaning.
- `Scene.CheckEscaped` owns no public check-blocked, checker-captured, or king-moved meaning.
- `Scene.CheckEscaped` owns no attack, pressure, initiative, force, best, only, winning, decisive, or no-counterplay meaning.
- `Scene.CheckEscaped` owns no Material, Hanging, Fork, Defense, Line, Defender, or pawn/file meaning.
- Existing opened Story labels keep their proof homes, Story labels, writers, and speech keys.

CheckEscaped-9 downstream audit:

- `escapes_check` says only that the legal move gets the moving side out of check on the exact after-board.
- Renderer and LLM smoke wording must stay no stronger than bounded `escapes_check`.
- Support, Context, Blocked, capped, and refuted rows have no standalone CheckEscaped text.
- Raw proof failures, raw engine eval, raw PV, source rows, BoardFacts check state, and escape method diagnostics never become public wording.

CheckEscaped-9 documentation audit:

- Detailed CheckEscaped stage law, negative corpus, closeout checklist, forbidden wording, and duplication audit live only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, and Manifest may summarize only if docs tests require it.
- AGENTS.md remains unchanged unless durable operator rules change.
- Docs authority tests must continue to agree on the live documentation authority list.

CheckEscaped-9 public-surface audit:

- public route `200` remains closed.
- production API remains closed.
- public/user-facing LLM narration remains closed.
- Existing LLM smoke remains bounded internal smoke only.

Completion standard: CheckEscaped-9 closes when `CheckEscapedProof`, `Scene.CheckEscaped`, `SceneCheckEscaped`, and `escapes_check` remain separated by layer; exactly one narrow check-escape public event is open; CheckEscaped owns no CheckGiven, public escape-method, mate, checkmate, king-safety, defense-success, attack, pressure, initiative, force, best, only, winning, decisive, no-counterplay, Material, Hanging, Fork, Defense, Line, Defender, or pawn/file meaning; cross-check collisions remain stable and separated; renderer and LLM smoke output stays no stronger than `escapes_check`; detailed authority stays only in `StoryInteractionLaw.md`; summary docs remain summaries; public route `200`, production API, and public/user-facing LLM narration stay closed; targeted runtime tests pass; docs authority tests pass when touched; and `git diff --check` passes.

### KCIH-0 King Check Interaction Hardening Charter

KCIH-0 opens no new chess meaning.

KCIH-0 closes only interaction hardening for the already-open King Check Neighborhood rows:

- `Scene.CheckGiven`
- `Scene.CheckEscaped`

KCIH-0 opens no new:

- Story label
- proof home
- Story writer
- speech key
- renderer wording
- LLM behavior
- public route `200`
- production API
- public/user-facing LLM narration

Core sentence:

King Check Interaction Hardening proves that CheckGiven and CheckEscaped stay separate when they collide, especially on cross-check moves.

KCIH-0 authority map:

- `CheckGivenProof` -> `Scene.CheckGiven` -> `SceneCheckGiven` -> `gives_check`
- `CheckEscapedProof` -> `Scene.CheckEscaped` -> `SceneCheckEscaped` -> `escapes_check`

KCIH-0 does not open:

- mate threat
- checkmate
- avoids mate
- king safety
- safe king
- unsafe king
- attack
- pressure
- initiative
- forced move
- best move
- only move
- winning
- decisive
- no counterplay
- `Scene.KingMovedOutOfCheck`
- `Scene.CheckBlocked`
- `Scene.CheckingPieceCaptured`
- `king_escapes_check`
- `blocks_check`
- `captures_checker`

KCIH-0 runtime boundary:

- cross-check moves may produce both already-open rows only when each row has its own complete same-board proof.
- `Scene.CheckGiven` must not borrow `CheckEscapedProof`, `SceneCheckEscaped`, or `escapes_check`.
- `Scene.CheckEscaped` must not borrow `CheckGivenProof`, `SceneCheckGiven`, or `gives_check`.
- StoryTable may order existing rows only; it must not create either row.
- ExplanationPlan, renderer, and LLM smoke remain bounded to the selected row's existing speech key.
- non-lead, Support, Context, Blocked, capped, and refuted rows have no standalone King Check interaction text.

KCIH-0 documentation boundary:

- detailed KCIH authority lives only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, Manifest, and AGENTS.md must not duplicate the KCIH charter.
- docs authority tests must pin this section as the single detailed owner.

Completion standard: KCIH-0 closes when the hardening scope is limited to existing CheckGiven and CheckEscaped rows, no new meaning or downstream surface opens, CheckGiven and CheckEscaped stay separated by proof home, Story label, writer, and speech key in cross-check collisions, StoryTable ordering remains deterministic without creating rows, non-lead or capped/refuted rows produce no standalone interaction wording, forbidden mate, checkmate, avoids-mate, king-safety, safe-king, unsafe-king, attack, pressure, initiative, forced, best, only, winning, decisive, no-counterplay, king-moved, check-blocked, checker-captured, `king_escapes_check`, `blocks_check`, and `captures_checker` meanings remain closed, and detailed KCIH authority lives only in `StoryInteractionLaw.md`.

### KCIH-1 Authority / Duplication Audit

KCIH-1 audits proof, Story, writer, and speech ownership.

KCIH-1 opens no new chess meaning, Story label, proof home, writer, speech key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration.

KCIH-1 must confirm:

- `CheckGivenProof` owns only the legal move gives check proof home.
- `Scene.CheckGiven` owns only the gives-check Story label.
- `SceneCheckGiven` is the only named writer for `Scene.CheckGiven`.
- `gives_check` is the only speech key for CheckGiven.
- `CheckEscapedProof` owns only the legal move escapes check proof home.
- `Scene.CheckEscaped` owns only the escapes-check Story label.
- `SceneCheckEscaped` is the only named writer for `Scene.CheckEscaped`.
- `escapes_check` is the only speech key for CheckEscaped.

KCIH-1 must reject:

- CheckGiven proof attached to CheckEscaped rows
- CheckEscaped proof attached to CheckGiven rows
- CheckGiven writer creating CheckEscaped
- CheckEscaped writer creating CheckGiven
- `gives_check` emitted from CheckEscaped
- `escapes_check` emitted from CheckGiven
- duplicate public escape-method labels
- duplicate public escape-method speech keys

KCIH-1 no-go names remain closed:

- `Scene.KingMovedOutOfCheck`
- `Scene.CheckBlocked`
- `Scene.CheckingPieceCaptured`
- `king_escapes_check`
- `blocks_check`
- `captures_checker`

KCIH-1 duplication audit:

- one check-given meaning: legal move gives check on the exact after-board
- one check-escaped meaning: legal move escapes check on the exact after-board
- two separate proof homes: `CheckGivenProof` and `CheckEscapedProof`
- two separate Story labels: `Scene.CheckGiven` and `Scene.CheckEscaped`
- two separate named writers: `SceneCheckGiven` and `SceneCheckEscaped`
- two separate speech keys: `gives_check` and `escapes_check`
- zero public escape-method Story labels
- zero public escape-method speech keys

KCIH-1 downstream audit:

- ExplanationPlan must lower CheckGiven only to `gives_check`.
- ExplanationPlan must lower CheckEscaped only to `escapes_check`.
- Renderer and LLM smoke must remain no stronger than the selected row's existing speech key.
- Support, Context, Blocked, capped, and refuted rows have no standalone KCIH text.
- proofFailures, raw engine evidence, BoardFacts check state, and escape-method diagnostics remain internal.

KCIH-1 documentation boundary:

- detailed KCIH-1 authority lives only in `StoryInteractionLaw.md`.
- summary documents and AGENTS.md must not duplicate this audit.
- docs authority tests must pin the KCIH-1 markers as single-owner detail.

Completion standard: KCIH-1 closes when there is one check-given meaning, one check-escaped meaning, two separate proof homes, two separate Story labels, two separate writers, two separate speech keys, no cross-attached proof sidecars, no mixed writer identity, and no public escape-method duplicate.

### KCIH-2 Cross-Check Collision Fixtures

KCIH-2 adds only same-board interaction fixtures for existing rows.

KCIH-2 opens no new chess meaning, Story label, proof home, writer, speech key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration.

KCIH-2 required fixture categories:

- CheckGiven only
- CheckEscaped only
- cross-check move where the side escapes check and gives check
- CheckGiven row with contaminated CheckEscaped sidecar
- CheckEscaped row with contaminated CheckGiven sidecar
- capped CheckGiven next to uncapped CheckEscaped
- capped CheckEscaped next to uncapped CheckGiven
- refuted CheckGiven next to valid CheckEscaped
- refuted CheckEscaped next to valid CheckGiven

KCIH-2 verifies:

- cross-check may produce both existing rows only when each row has its own complete proof
- CheckGiven row keeps `gives_check`
- CheckEscaped row keeps `escapes_check`
- neither row borrows the other's proof, writer, target, anchor, route, or speech key
- contaminated rows are blocked or produce no standalone claim
- fixture order does not change the selected Verdict set

KCIH-2 does not open:

- double-check as public Story
- counter-check as public Story
- discovered check expansion
- escape method Story
- mate threat
- checkmate
- king safety
- forced / best / only / winning wording

KCIH-2 runtime boundary:

- fixtures may instantiate only existing `Scene.CheckGiven` and `Scene.CheckEscaped` rows.
- CheckGiven-only fixtures must not create CheckEscaped rows.
- CheckEscaped-only fixtures must not create CheckGiven rows.
- cross-check fixtures may produce both rows only from the same legal same-board move and only as separate proof-backed rows.
- mixed sidecars and mixed writer identities must be Blocked or silent.
- capped and refuted rows must not emit standalone claim text when a valid sibling row remains.
- selected Verdict output must be deterministic across fixture input order.

KCIH-2 documentation boundary:

- detailed KCIH-2 fixture law lives only in `StoryInteractionLaw.md`.
- summary documents and AGENTS.md must not duplicate fixture categories or closeout law.
- docs authority tests must pin KCIH-2 as single-owner detail.

Completion standard: KCIH-2 closes when cross-check and contamination fixtures prove CheckGiven and CheckEscaped can coexist only as separate proof-backed rows, every mixed sidecar or mixed writer row is rejected or silent, input order is stable, and no new public check subtype opens.

### KCIH-3 StoryTable Role Stability

KCIH-3 audits StoryTable behavior for existing King Check rows only.

KCIH-3 opens no new chess meaning, Story label, proof home, writer, speech key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration.

KCIH-3 verifies:

- StoryTable may order existing `Scene.CheckGiven` rows.
- StoryTable may order existing `Scene.CheckEscaped` rows.
- StoryTable must not create either row.
- input order does not change Lead / Support / Context / Blocked stability
- same-meaning duplicate CheckGiven rows collapse or order deterministically
- same-meaning duplicate CheckEscaped rows collapse or order deterministically
- cross-check rows remain separate meanings
- refuted rows are Blocked
- capped rows do not produce standalone text
- Support and Context rows do not produce standalone text

KCIH-3 collision targets:

- `Scene.CheckGiven`
- `Scene.CheckEscaped`
- `Scene.Material`
- Tactic.Hanging
- Tactic.Fork
- `Scene.Defense`
- Tactic.DiscoveredAttack
- Tactic.Pin
- Tactic.RemoveGuard
- Tactic.Skewer
- open pawn/file scenes

KCIH-3 must confirm:

- CheckGiven owns no CheckEscaped meaning.
- CheckEscaped owns no CheckGiven meaning.
- Material, Defense, Line/Defender, Hanging, Fork, and pawn/file rows keep their own proof and speech homes.
- King Check rows do not outrank other rows by inventing stronger chess meaning.

KCIH-3 StoryTable boundary:

- StoryTable may select, support, contextualize, or block existing King Check rows only after their writers and proof sidecars already exist.
- StoryTable must not create `CheckGivenProof`, `CheckEscapedProof`, `Scene.CheckGiven`, `Scene.CheckEscaped`, `SceneCheckGiven`, `SceneCheckEscaped`, `gives_check`, or `escapes_check`.
- same-meaning duplicate King Check rows may both appear only as deterministic Lead/Support ordering or may collapse by existing equality behavior; either outcome must be input-order stable.
- cross-check rows are sibling meanings, not duplicates.
- capped, refuted, Support, Context, and Blocked King Check rows have no standalone renderer or LLM smoke text.

KCIH-3 documentation boundary:

- detailed KCIH-3 StoryTable role law lives only in `StoryInteractionLaw.md`.
- summary documents and AGENTS.md must not duplicate KCIH-3 collision targets or closeout law.
- docs authority tests must pin KCIH-3 as single-owner detail.

Completion standard: KCIH-3 closes when StoryTable ordering is deterministic, existing King Check rows stay separated, sibling rows keep their own claim homes, and Support, Context, Blocked, capped, and refuted King Check rows have no standalone text.

### KCIH-4 EngineCheck / Cap / Refute Boundary

KCIH-4 audits EngineCheck reuse over existing King Check rows.

KCIH-4 opens no new chess meaning, Story label, proof home, writer, speech key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration.

KCIH-4 verifies:

- EngineCheck cannot create CheckGiven.
- EngineCheck cannot create CheckEscaped.
- EngineCheck attaches only to an already complete same-board Story.
- EngineCheck evidence binds to the same Story route and legal line.
- Supports adds no new claim.
- Caps suppresses standalone text or keeps speech bounded to the selected claim key.
- Refutes blocks the checked Story.
- Unknown creates no engine expression.

KCIH-4 forbidden engine wording:

- engine says
- eval number
- raw PV
- best move
- only move
- forced move
- winning
- decisive
- no counterplay
- mate threat
- checkmate
- safe king
- refutes attack

KCIH-4 must confirm:

- capped CheckGiven does not emit `gives_check` standalone text
- capped CheckEscaped does not emit `escapes_check` standalone text
- refuted CheckGiven is Blocked
- refuted CheckEscaped is Blocked
- engine-supported CheckGiven remains only `gives_check`
- engine-supported CheckEscaped remains only `escapes_check`

KCIH-4 EngineCheck boundary:

- EngineCheck is a support/cap/refute/unknown sidecar only after `SceneCheckGiven` or `SceneCheckEscaped` has already created a complete same-board Story with its own proof sidecar.
- EngineCheck must not create `CheckGivenProof`, `CheckEscapedProof`, `Scene.CheckGiven`, `Scene.CheckEscaped`, `SceneCheckGiven`, `SceneCheckEscaped`, `gives_check`, or `escapes_check`.
- EngineCheck must bind to the same Story route and legal line as the checked King Check Story.
- Engine-supported King Check rows remain bounded to their original selected claim key.
- capped, refuted, and Unknown EngineCheck rows produce no independent engine expression, eval expression, PV expression, or stronger chess claim.

KCIH-4 documentation boundary:

- detailed KCIH-4 EngineCheck boundary law lives only in `StoryInteractionLaw.md`.
- summary documents and AGENTS.md must not duplicate KCIH-4 forbidden wording or closeout law.
- docs authority tests must pin KCIH-4 as single-owner detail.

Completion standard: KCIH-4 closes when EngineCheck remains support/cap/refute only for already existing King Check Stories, never creates or strengthens a claim, and all engine wording, raw PV, eval, best/only/forced, winning, mate, and king-safety wording stays closed.

### KCIH-5 Downstream No-Overclaim Boundary

KCIH-5 audits ExplanationPlan, DeterministicRenderer, and LLM smoke boundaries for existing King Check rows.

KCIH-5 opens no new chess meaning, Story label, proof home, writer, speech key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration.

KCIH-5 verifies ExplanationPlan:

- selected uncapped Lead `Scene.CheckGiven` may lower only to `gives_check`
- selected uncapped Lead `Scene.CheckEscaped` may lower only to `escapes_check`
- CheckGiven never lowers to `escapes_check`
- CheckEscaped never lowers to `gives_check`
- Support, Context, Blocked, capped, and refuted rows produce no standalone claim

KCIH-5 verifies Renderer:

- Renderer input remains `ExplanationPlan` only
- CheckGiven renderer text stays no stronger than `{route} gives check.`
- CheckEscaped renderer text stays no stronger than `{route} gets out of check.`
- Renderer does not read Story, proof homes, BoardFacts, EngineCheck, proofFailures, raw PV, or source rows

KCIH-5 verifies LLM smoke:

- LLM smoke receives only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`
- LLM smoke does not receive raw Story, raw proof, BoardFacts, EngineCheck, raw PV, proofFailures, or source rows
- LLM smoke may not add new move, new line, mate, checkmate, king safety, attack, pressure, initiative, force, best, only, winning, decisive, no-counterplay, engine explanation, or escape method detail

KCIH-5 downstream boundary:

- ExplanationPlan is the only lowering boundary for selected King Check `Verdict` data.
- DeterministicRenderer accepts `ExplanationPlan` only and must not inspect raw Story, proof homes, BoardFacts, EngineCheck, proofFailures, raw PV, or source rows.
- DeterministicRenderer may render selected uncapped Lead `Scene.CheckGiven` only as `{route} gives check.`
- DeterministicRenderer may render selected uncapped Lead `Scene.CheckEscaped` only as `{route} gets out of check.`
- LLM smoke input is the bounded rendered line contract only: renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`
- LLM smoke remains a rephrase-only smoke check and must not become public/user-facing narration.
- downstream expression must not create, repair, rank, prove, strengthen, or cross-map CheckGiven and CheckEscaped meanings.

KCIH-5 documentation boundary:

- detailed KCIH-5 downstream no-overclaim law lives only in `StoryInteractionLaw.md`.
- summary documents and AGENTS.md must not duplicate KCIH-5 ExplanationPlan, Renderer, LLM smoke, or closeout law.
- docs authority tests must pin KCIH-5 as single-owner detail.

Completion standard: KCIH-5 closes when downstream expression is exactly bounded by `gives_check` or `escapes_check`, cross-claim lowering is impossible, non-Lead/capped/refuted rows stay silent, renderer input remains ExplanationPlan only, LLM smoke remains bounded rephrase-only smoke, and public narration remains closed.

### KCIH-6 Documentation / Public Surface Audit

KCIH-6 audits documentation and public surface only.

KCIH-6 opens no new chess meaning, Story label, proof home, writer, speech key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration.

KCIH-6 documentation rules:

- detailed KCIH authority lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may summarize only if docs tests require it
- AGENTS.md remains unchanged unless durable operator rules change
- docs tests must agree with the live authority document list
- no retired root authority document returns

KCIH-6 public surface rules:

- `/api/commentary/render` remains fail-closed
- `/internal/commentary/render-local-probe` remains fail-closed
- no public route returns `200`
- no production API opens
- no public/user-facing LLM narration opens
- proofFailures remain internal diagnostics only
- raw engine eval, raw PV, source rows, and proof text remain out of public values and downstream wording

KCIH-6 forbidden docs drift:

- detailed stage law duplicated outside `StoryInteractionLaw.md`
- KCIH described as opening mate, checkmate, king safety, attack, pressure, initiative, forced, best, only, winning, decisive, or no-counterplay meaning
- CheckGiven or CheckEscaped described as public route ready
- LLM smoke described as public narration

KCIH-6 audit boundary:

- KCIH documentation detail remains single-owned by `StoryInteractionLaw.md`; summary docs must not copy KCIH checklists, target lists, forbidden-wording lists, or completion law.
- route registration for `/api/commentary/render` and `/internal/commentary/render-local-probe` may exist only as fail-closed tombstone wiring.
- controller behavior must remain unavailable/no-commentary, not `200`, not rendered payload, and not an environment-switchable production API.
- frontend code must not call the commentary render routes or treat LLM smoke as user-facing narration.
- `proofFailures`, raw engine eval, raw PV, source rows, raw proof text, and proof failure text remain internal diagnostics and must not enter `Verdict.values`, renderer input, LLM input, public JSON, or downstream wording.

KCIH-6 documentation boundary:

- detailed KCIH-6 documentation/public-surface audit law lives only in `StoryInteractionLaw.md`.
- summary documents and AGENTS.md must not duplicate KCIH-6 public surface law or closeout law.
- docs authority tests must pin KCIH-6 as single-owner detail and must keep the live authority document list synchronized.

Completion standard: KCIH-6 closes when docs remain summary-only outside `StoryInteractionLaw.md`, public routes remain tombstones, production API remains closed, public/user-facing LLM narration remains closed, and no diagnostic or raw engine/proof/source text becomes public wording.

### KCIH-7 Closeout / Verification

KCIH-7 opens no new chess meaning.

KCIH-7 closes only King Check Interaction Hardening for the already-open `Scene.CheckGiven` and `Scene.CheckEscaped` rows.

KCIH-7 opens no new Story label, proof home, Story writer, speech key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration.

KCIH-7 final audit:

- `CheckGivenProof`, `Scene.CheckGiven`, `SceneCheckGiven`, and `gives_check` remain separated.
- `CheckEscapedProof`, `Scene.CheckEscaped`, `SceneCheckEscaped`, and `escapes_check` remain separated.
- CheckGiven owns no CheckEscaped meaning.
- CheckEscaped owns no CheckGiven meaning.
- cross-check rows may coexist only through separate complete proofs.
- escape method details remain diagnostic only.
- mate threat, checkmate, king safety, attack, pressure, initiative, forced, best, only, winning, decisive, and no-counterplay remain closed.
- Support, Context, Blocked, capped, and refuted rows produce no standalone text.
- Renderer and LLM smoke stay no stronger than the selected claim key.
- public route `200`, production API, and public/user-facing LLM narration remain closed.

KCIH-7 verification:

- `sbt "commentary/testOnly lila.commentary.chess.KingCheckInteractionHardeningTest"`
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

KCIH-7 documentation boundary:

- detailed KCIH-7 closeout law lives only in `StoryInteractionLaw.md`.
- summary documents and AGENTS.md must not duplicate KCIH-7 final audit or verification law.
- docs authority tests must pin KCIH-7 as single-owner detail.

Completion standard: KCIH-7 closes when King Check Interaction Hardening has no new Story, proof home, writer, speech key, renderer wording, LLM behavior, public route, production API, or public/user-facing narration; CheckGiven and CheckEscaped remain separate under cross-check, cap/refute, StoryTable, renderer, and LLM smoke collisions; targeted tests pass; docs authority tests pass when touched; and `git diff --check` passes.

### Checkmate-1 CheckmateProof

Checkmate-1 opens only `CheckmateProof` as the proof home for the third King
Check Neighborhood slice.

Checkmate-1 proves:

- mating side
- rival side
- legal move identity
- moving piece identity when available
- origin square
- destination square
- rival king square after the move
- after-board rival king is in check
- after-board rival side has no legal escape
- exact after-board replay
- same-board proof
- checkmate was produced by this legal move

Checkmate-1 first scope:

- any legal move whose exact after-board is checkmate against the rival side
- ordinary checkmate, discovered checkmate, double-check mate, promotion mate,
  and capture mate are allowed only as checkmate events
- no public distinction between mate delivery types

Checkmate-1 closed scope:

- mate threat
- mate in one before the move
- mate in N
- forced mate
- unavoidable mate
- winning evaluation
- decisive advantage
- best move / only move
- no counterplay
- king-safety diagnosis
- attack / pressure / initiative
- SAN `#` as proof

Proof-home boundary:

- `CheckmateProof` is not a public Story.
- `CheckmateProof` must not create `Scene.Checkmate`.
- `CheckmateProof` must not create a Verdict, ExplanationPlan, renderer text,
  LLM input, public route payload, or production API response.
- `CheckmateProof` does not replace `CheckGivenProof`.
- `CheckGivenProof` remains only the gives-check proof home.
- `CheckEscapedProof` remains only the escapes-check proof home.
- `CheckmateProof` does not replace Line, Pin, Skewer, RemoveGuard, Material,
  Defense, Hanging, Fork, or pawn proof homes.
- `CheckmateProof` must not own mate threat, forced mate, best, only, winning,
  decisive, no-counterplay, king safety, attack, pressure, or initiative
  meaning.

Completion standard: Checkmate-1 closes when `CheckmateProof` proves only the
mating side, rival side, legal move identity, origin, destination, rival king
square, after-board rival-king-in-check state, rival-side no-legal-escape
state, exact after-board replay, same-board proof, and
checkmate-produced-by-this-move for one legal move; mate threat, mate in N,
forced mate, unavoidable mate, winning, decisive, best, only, no-counterplay,
king-safety, attack, pressure, initiative, and SAN-only proof remain closed;
`CheckmateProof` is not a public Story; and no new Story writer, Verdict,
ExplanationPlan, renderer, LLM, public route `200`, production API, or
public/user-facing LLM narration opens.

### Checkmate-2 Scene.Checkmate Writer

Checkmate-2 opens only `SceneCheckmate` as the named writer for
`Scene.Checkmate`.

Checkmate-2 writer conditions:

- complete StoryProof
- complete `CheckmateProof`
- same-board legal replay
- legal move leaves rival king in check on the exact after-board
- legal move leaves rival side with no legal escape on the exact after-board
- rival king square is identified
- writer = `SceneCheckmate`
- EngineCheck does not Refute

Checkmate-2 Story identity:

- scene = `Checkmate`
- tactic = None
- plan = None
- side = mating side
- rival = mated side
- target = rival king square after the move
- anchor = moving piece origin square
- route = legal checkmating move

Checkmate-2 forbidden writer ownership:

- `SceneCheckmate` must not create mate threat.
- `SceneCheckmate` must not create mate in N.
- `SceneCheckmate` must not create forced mate.
- `SceneCheckmate` must not create best-move, only-move, winning, decisive, or
  no-counterplay meaning.
- `SceneCheckmate` must not create king-safety, attack, pressure, or initiative
  meaning.
- `SceneCheckmate` must not steal `Scene.CheckGiven` or `Scene.CheckEscaped`
  meaning.
- `SceneCheckmate` must not steal Line, Pin, Skewer, RemoveGuard, Material,
  Defense, Hanging, Fork, or pawn meanings.

Checkmate-2 downstream boundary:

- Checkmate-2 does not open ExplanationPlan.
- Checkmate-2 does not open renderer text.
- Checkmate-2 does not open LLM input.
- Checkmate-2 does not open public route `200`, production API, or
  public/user-facing LLM narration.

Completion standard: Checkmate-2 closes when `SceneCheckmate` writes only exact
complete `CheckmateProof` rows into `Scene.Checkmate` with no tactic, no plan,
mating side, rival side, target as the rival king square, anchor as the moving
piece origin square, route as the legal checkmating move, no EngineCheck
Refutes lead, and no mate-threat, mate-in-N, forced-mate, best, only, winning,
decisive, no-counterplay, king-safety, attack, pressure, initiative,
CheckGiven, CheckEscaped, sibling tactic, material, defense, or pawn meaning;
downstream ExplanationPlan, renderer, LLM, public route `200`, production API,
and public/user-facing LLM narration remain closed.

### Checkmate-3 Negative Corpus

Checkmate-3 builds the negative corpus for `Checkmate`.

Checkmate-3 must stay silent for:

- legal move missing
- illegal move
- same-board proof missing
- exact-board replay missing
- after-board rival king not in check
- after-board rival side has a legal escape
- stalemate
- side-to-move confusion
- rival king square missing
- route mismatch
- stale before/after board
- incomplete StoryProof
- incomplete CheckmateProof
- writerless row
- contaminated sidecar row
- `CheckGivenProof` without `CheckmateProof`
- BoardFacts check count without bound mate proof
- BoardFacts no-legal-move state without after-board check
- SAN `#` without proof
- engine line claiming mate without an existing Story
- raw PV mate without an existing Story
- mate-threat wording
- mate-in-N wording
- forced-mate wording
- best-move wording
- only-move wording
- winning / decisive / no-counterplay wording
- king-safety / attack / pressure / initiative wording

Checkmate-3 standard:

- Check is not checkmate.
- No legal moves is not checkmate unless the rival king is also in check.
- SAN `#` is not proof.
- Engine mate text is not proof.
- Checkmate is not a mate-threat claim.
- Checkmate is not best-move, only-move, winning-evaluation, or no-counterplay
  commentary.
- Complete `CheckmateProof` or silence.
- `SceneCheckmate` must not open ExplanationPlan, renderer text, LLM input,
  public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Checkmate-3 closes when the negative corpus proves that
illegal moves, missing same-board proof, missing replay, no after-board check,
rival legal escapes, stalemate, side-to-move errors, missing king identity,
route mismatch, stale board proof, incomplete StoryProof, incomplete
CheckmateProof, writerless rows, contaminated rows, CheckGiven-only proof,
BoardFacts-only check/no-move states, SAN-only mate marks, engine-only mate
lines, raw PV mate, and mate-threat, mate-in-N, forced-mate, best, only,
winning, decisive, no-counterplay, king-safety, attack, pressure, or initiative
wording all stay silent under CheckmateProof, SceneCheckmate, StoryTable, and
downstream ExplanationPlan boundaries; Checkmate remains only the exact legal
same-board event that the move checkmates the rival side.

### Checkmate-4 EngineCheck Reuse

Checkmate-4 reuses only the existing `EngineCheck` boundary for
`Scene.Checkmate`.

Checkmate-4 rules:

- EngineCheck cannot create `Scene.Checkmate`.
- Supports does not create a new claim.
- Caps suppresses standalone claim speech or keeps downstream speech bounded to
  `checkmates`.
- Refutes makes the `Scene.Checkmate` Story Blocked.
- Unknown creates no engine expression.
- Engine evidence must bind to the same Story route and same legal line.

Checkmate-4 forbidden engine wording:

- engine says mate
- mate score
- eval number
- raw PV
- best move
- only move
- forced mate
- mate in N
- winning
- decisive
- no counterplay

Checkmate-4 downstream boundary:

- Checkmate-4 does not open ExplanationPlan for `Scene.Checkmate`.
- Checkmate-4 does not open renderer text for `Scene.Checkmate`.
- Checkmate-4 does not open LLM input for `Scene.Checkmate`.
- Checkmate-4 does not open public route `200`, production API, or
  public/user-facing LLM narration.

Completion standard: Checkmate-4 closes when EngineCheck attaches only to an
already complete same-board `SceneCheckmate` Story, cannot produce
`Scene.Checkmate` from engine evidence alone, Supports adds no chess claim, Caps
suppresses or bounds downstream speech without opening Checkmate wording early,
Refutes blocks the Checkmate row, Unknown creates no engine expression, and
engine-says, mate-score, eval-number, raw-PV, best, only, forced-mate,
mate-in-N, winning, decisive, and no-counterplay wording remain closed.

### Checkmate-5 StoryTable Integration

Checkmate-5 integrates `Scene.Checkmate` into StoryTable only as an already existing
proof-backed Story.

Checkmate-5 collision targets:

- `Scene.Checkmate`
- `Scene.CheckGiven`
- `Scene.CheckEscaped`
- `Scene.Material`
- Tactic.Hanging
- Tactic.Fork
- `Scene.Defense`
- Tactic.DiscoveredAttack
- Tactic.Pin
- Tactic.RemoveGuard
- Tactic.Skewer
- open pawn/file scenes

Checkmate-5 verifies:

- input order stability
- `Scene.Checkmate` does not own `Scene.CheckGiven` meaning.
- `Scene.CheckGiven` does not own `Scene.Checkmate` meaning.
- `Scene.Checkmate` does not own `Scene.CheckEscaped` meaning.
- `Scene.CheckEscaped` does not own `Scene.Checkmate` meaning.
- A checkmating move may also be a checking move, but each row keeps its own
  proof home, Story label, writer, and speech key.
- `Scene.Checkmate` does not own mate threat, mate in N, forced mate, best,
  only, winning, decisive, no-counterplay, king safety, attack, pressure, or
  initiative meaning.
- Existing material, defense, line/defender, hanging, fork, CheckGiven,
  CheckEscaped, and pawn/file claim homes keep their own proof and speech
  homes.
- Capped or refuted `Scene.Checkmate` has no standalone text.
- Support, Context, and Blocked Checkmate rows do not lower to standalone text.

Checkmate-5 StoryTable boundary:

- StoryTable may select an existing complete `Scene.Checkmate` row.
- StoryTable must not create `Scene.Checkmate`.
- `Scene.Checkmate` may lead only as the exact checkmate event.
- Selection still does not open renderer text, LLM input, public route `200`,
  production API, or public/user-facing LLM narration.

Completion standard: Checkmate-5 closes when StoryTable ordering is stable
across input order, `Scene.Checkmate` keeps only the bounded legal checkmate
event, CheckGiven keeps only gives-check, CheckEscaped keeps only
escapes-check, every already-open claim home keeps its own proof and speech
ownership, `Scene.Checkmate` never steals mate-threat, mate-in-N, forced-mate,
best, only, winning, decisive, no-counterplay, king-safety, attack, pressure,
initiative, material, defense, line/defender, hanging, fork, or pawn/file
meaning, and capped, refuted, Support, Context, or Blocked Checkmate rows
produce no standalone text.

### Checkmate-6 ExplanationPlan

Checkmate-6 opens only `ExplanationPlan` for selected uncapped Lead
`Scene.Checkmate` Verdicts.

Checkmate-6 allowed claim key:

- `checkmates`

Checkmate-6 forbidden claim keys:

- `gives_check`
- `escapes_check`
- `mate_threat`
- `mate_in_one`
- `mate_in_n`
- `forced_mate`
- `best_move`
- `only_move`
- `winning`
- `decisive`
- `no_counterplay`
- `king_unsafe`
- `attacks_king`
- `creates_attack`
- `creates_pressure`
- `takes_initiative`
- `engine_says_mate`

Checkmate-6 input boundary:

- `ExplanationPlan.fromSelected` accepts only selected uncapped Lead
  `Scene.Checkmate` Verdicts.
- The Story must still be `Scene.Checkmate`, tactic = None, plan = None,
  writer = `SceneCheckmate`, and same-board proof-backed.
- `target` remains the rival king square after the move.
- `anchor` remains the moving piece origin square.
- `route` remains the legal checkmating move.
- `evidenceLine` is only that route.
- `CheckmateProof` must still bind after-board check and no-legal-escape state
  to the same Story route.
- CheckGiven and CheckEscaped rows must not lower to `checkmates`.

Checkmate-6 no-standalone boundary:

- Support, Context, Blocked, capped, and refuted `Scene.Checkmate` rows have no
  standalone claim.
- EngineCheck Supports does not create a new claim; it may accompany the
  already selected uncapped Lead claim only.
- EngineCheck Caps suppresses standalone claim output.
- EngineCheck Refutes keeps the row Blocked.

Checkmate-6 downstream boundary:

- Checkmate-6 does not open renderer text.
- Checkmate-6 does not open LLM input.
- Checkmate-6 does not open public route `200`, production API, or
  public/user-facing LLM narration.
- `checkmates` may mean only: this legal move checkmates the rival side on the
  exact after-board.
- `checkmates` may not mean mate threat, mate in N, forced mate, best move,
  only move, winning evaluation, decisive advantage, no counterplay, king
  safety, attack, pressure, initiative, or engine mate line.

Completion standard: Checkmate-6 closes when `ExplanationPlan.fromSelected`
lowers only selected uncapped Lead `Scene.Checkmate` Verdicts to `checkmates`
with target as the rival king square, anchor as the moving piece origin square,
route and evidenceLine as the legal checkmating move, complete same-board
`CheckmateProof` still bound to the Story, Support, Context, Blocked, capped,
and refuted rows silent, EngineCheck Supports adding no new claim, CheckGiven
retaining only `gives_check`, CheckEscaped retaining only `escapes_check`,
forbidden mate-threat, mate-in-N, forced-mate, best, only, winning, decisive,
no-counterplay, king-safety, attack, pressure, initiative, and engine-says-mate
claim keys closed, and renderer, LLM, public route `200`, production API, and
public/user-facing LLM narration still closed.

### Checkmate-7 DeterministicRenderer

Checkmate-7 opens deterministic renderer text only from bounded `Checkmate`
`ExplanationPlan`.

Checkmate-7 renderer input boundary:

- Renderer input is `ExplanationPlan` only.
- Renderer does not accept Story, Verdict, CheckmateProof, CheckGivenProof,
  CheckEscapedProof, BoardFacts, EngineCheck, engine eval, raw PV,
  proofFailures, SAN marks, or source rows.
- Renderer may phrase only selected uncapped Lead `Scene.Checkmate` plans with
  claimKey = `checkmates`.
- Support, Context, Blocked, capped, refuted, malformed, or sibling-claim plans
  have no standalone renderer text.

Checkmate-7 allowed renderer templates:

- `{route} is checkmate.`
- `{route} checkmates the king.`

Checkmate-7 forbidden renderer wording:

- threatens mate
- mate in one
- mate in N
- forced mate
- best move
- only move
- winning
- decisive
- no counterplay
- the king is unsafe
- starts an attack
- creates pressure
- takes the initiative
- engine says
- mate score

Checkmate-7 downstream boundary:

- Checkmate-7 does not open LLM narration, public route `200`, production API,
  mate threat, mate in N, forced mate, best move, only move, winning, decisive,
  no-counterplay, king safety, attack, pressure, initiative, or engine mate
  meaning.
- Renderer text may mean only: this legal move checkmates the rival side on the
  exact after-board.

Completion standard: Checkmate-7 closes when `DeterministicRenderer` accepts
only a bounded `checkmates` `ExplanationPlan`, emits only bounded checkmate
text, rejects Support, Context, Blocked, capped, refuted, malformed, or
sibling-claim plans, keeps forbidden mate-threat, mate-in-N, forced, best,
only, winning, decisive, no-counterplay, king-safety, attack, pressure,
initiative, engine, and mate-score wording absent, and leaves public/user-facing
LLM narration closed.

### Checkmate-8 LLM Smoke

Checkmate-8 reuses only the existing 8B LLM smoke boundary for selected bounded
Checkmate `RenderedLine` rephrases.

Checkmate-8 allowed LLM smoke input:

- `renderedText`
- `claimKey`
- `strength`
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Checkmate-8 forbidden LLM smoke input:

- raw Story
- raw CheckmateProof
- raw CheckGivenProof
- raw CheckEscapedProof
- BoardFacts
- EngineCheck
- EngineLine
- EngineEval
- raw PV
- proofFailures
- source rows
- SAN `#` diagnostics

Checkmate-8 forbidden LLM smoke additions:

- new move
- new line
- mate threat
- mate in one
- mate in N
- forced mate
- best move
- only move
- winning
- decisive
- no counterplay
- king safety
- unsafe king
- attack
- initiative
- pressure
- engine mention
- mate score
- why it is mate unless already in rendered text

Checkmate-8 downstream boundary:

- Checkmate-8 does not open public/user-facing LLM narration, public route
  `200`, production API, raw proof input, raw engine input, or any new chess
  meaning.
- LLM smoke may echo or rephrase only the already rendered bounded checkmate
  event.
- LLM smoke may not add mate-threat, mate-in-N, forced, best, only, winning,
  decisive, no-counterplay, king-safety, attack, pressure, initiative, new
  move, new line, or engine explanation.

Completion standard: Checkmate-8 closes when LlmNarrationSmoke accepts only
renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not
add chess facts.` for bounded `checkmates`; rejects raw Story, CheckmateProof,
CheckGivenProof, CheckEscapedProof, BoardFacts, EngineCheck, EngineLine,
EngineEval, raw PV, proofFailures, source rows, SAN-only diagnostics, new moves,
new lines, mate-threat, mate-in-N, forced, best, only, winning, decisive,
no-counterplay, king-safety, attack, pressure, initiative, and engine additions;
and keeps public narration and production API closed.

### Checkmate-9 Closeout / Hard Cleanup

Checkmate-9 opens no new chess meaning.

Close only the already-open narrow event:

a legal move checkmates the rival side on the exact after-board

Checkmate-9 authority audit:

- `CheckmateProof` owns the proof home.
- `Scene.Checkmate` owns the Story label.
- `SceneCheckmate` owns the named writer.
- `checkmates` owns the speech key.
- These authorities are not interchangeable.
- `CheckGivenProof`, `Scene.CheckGiven`, `SceneCheckGiven`, and `gives_check`
  remain the gives-check chain only.
- `CheckEscapedProof`, `Scene.CheckEscaped`, `SceneCheckEscaped`, and
  `escapes_check` remain the escapes-check chain only.
- BoardFacts check state, mate state, legal move facts, king squares, and legal
  replay remain observations only.
- SAN `#` remains notation for already-approved legal moves only.
- EngineCheck may support, cap, refute, or remain unknown only for an existing
  `Scene.Checkmate` Story, and cannot create one.

Checkmate-9 duplication audit:

- one check-given meaning: `CheckGivenProof` -> `Scene.CheckGiven` ->
  `SceneCheckGiven` -> `gives_check`
- one check-escaped meaning: `CheckEscapedProof` -> `Scene.CheckEscaped` ->
  `SceneCheckEscaped` -> `escapes_check`
- one checkmate meaning: `CheckmateProof` -> `Scene.Checkmate` ->
  `SceneCheckmate` -> `checkmates`
- no mate-threat meaning opened
- no mate-in-N meaning opened
- no forced-mate meaning opened
- one detailed live authority document: `StoryInteractionLaw.md`

Checkmate-9 collision audit:

- `Scene.Checkmate` owns no `Scene.CheckGiven` meaning.
- `Scene.CheckGiven` owns no `Scene.Checkmate` meaning.
- `Scene.Checkmate` owns no `Scene.CheckEscaped` meaning.
- `Scene.CheckEscaped` owns no `Scene.Checkmate` meaning.
- `Scene.Checkmate` owns no mate threat.
- `Scene.Checkmate` owns no mate in N.
- `Scene.Checkmate` owns no forced mate.
- `Scene.Checkmate` owns no best, only, winning, decisive, or no-counterplay
  meaning.
- `Scene.Checkmate` owns no king safety, unsafe-king, attack, pressure, or
  initiative meaning.
- `Scene.Checkmate` owns no Material, Hanging, Fork, Defense, Line, Defender,
  or pawn/file meaning.
- Existing opened Story labels keep their proof homes, Story labels, writers,
  and speech keys.

Checkmate-9 downstream audit:

- `checkmates` says only that the legal move checkmates the rival side on the
  exact after-board.
- Renderer and LLM smoke wording must stay no stronger than bounded
  `checkmates`.
- Support, Context, Blocked, capped, and refuted rows have no standalone
  Checkmate text.
- Raw proof failures, raw engine eval, raw PV, mate scores, source rows,
  BoardFacts mate state, and SAN marks never become public wording.

Checkmate-9 documentation audit:

- Detailed Checkmate stage law, negative corpus, closeout checklist, forbidden
  wording, and duplication audit live only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, and Manifest may summarize only if docs
  tests require it.
- AGENTS.md remains unchanged unless durable operator rules change.
- Docs authority tests must continue to agree on the live documentation
  authority list.

Checkmate-9 public-surface audit:

- public route `200` remains closed.
- production API remains closed.
- public/user-facing LLM narration remains closed.
- Existing LLM smoke remains bounded internal smoke only.

Checkmate-9 verification:

- Run targeted runtime tests:
  `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- Run Checkmate stage and closeout tests:
  `sbt "commentary/testOnly lila.commentary.chess.CheckmateStage1Test lila.commentary.chess.CheckmateStage2Test lila.commentary.chess.CheckmateStage3Test lila.commentary.chess.CheckmateStage4Test lila.commentary.chess.CheckmateStage5Test lila.commentary.chess.CheckmateStage6Test lila.commentary.chess.CheckmateStage7Test lila.commentary.chess.CheckmateStage8Test lila.commentary.chess.CheckmateCloseoutTest"`
- Run docs authority tests if docs authority summaries changed:
  `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- Always run cleanup:
  `git diff --check`

Completion standard: Checkmate-9 closes when `CheckmateProof`,
`Scene.Checkmate`, `SceneCheckmate`, and `checkmates` remain separated by layer;
exactly one narrow checkmate public event is open; Checkmate owns no CheckGiven,
CheckEscaped, mate-threat, mate-in-N, forced-mate, best, only, winning,
decisive, no-counterplay, king-safety, attack, pressure, initiative, Material,
Hanging, Fork, Defense, Line, Defender, or pawn/file meaning; renderer and LLM
smoke output stays no stronger than `checkmates`; detailed authority stays only
in `StoryInteractionLaw.md`; summary docs remain summaries; public route `200`,
production API, and public/user-facing LLM narration stay closed; targeted
runtime tests pass; docs authority tests pass when touched; and `git diff
--check` passes.

### KCNFC-0 King Check Neighborhood Final Closeout

KCNFC-0 opens no new chess meaning.

KCNFC-0 is final closeout and hardening for the already-open King Check
Neighborhood only. It audits exactly these chains:

- `CheckGivenProof` -> `Scene.CheckGiven` -> `SceneCheckGiven` -> `gives_check`
- `CheckEscapedProof` -> `Scene.CheckEscaped` -> `SceneCheckEscaped` ->
  `escapes_check`
- `CheckmateProof` -> `Scene.Checkmate` -> `SceneCheckmate` -> `checkmates`

KCNFC-0 adds no:

- proof home
- Story label
- Story writer
- speech key
- renderer wording
- LLM behavior
- public route `200`
- production API
- public/user-facing LLM narration

KCNFC-0 does not open:

- MateThreat
- mate in N
- forced mate
- king safety
- attack
- pressure
- initiative
- best move
- only move
- winning
- decisive
- no-counterplay

KCNFC-0 audit rules:

- CheckGiven owns only the legal gives-check event.
- CheckEscaped owns only the legal escapes-check event.
- Checkmate owns only the legal checkmates event on the exact after-board.
- A checkmating move may also satisfy CheckGiven proof, but `Scene.Checkmate`
  keeps `checkmates` and `Scene.CheckGiven` keeps `gives_check`.
- BoardFacts check state, mate state, SAN `+`, SAN `#`, legal move counts,
  engine evidence, proofFailures, renderer text, and LLM text never create or
  strengthen a King Check Story.
- Support, Context, Blocked, capped, and refuted King Check rows have no
  standalone text outside their already-open bounded downstream contracts.

KCNFC-0 documentation boundary:

- Detailed final closeout authority lives only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, Manifest, and AGENTS.md must not copy
  KCNFC-0 detailed law.
- Docs authority tests must continue to agree on live documentation authority.

Completion standard: KCNFC-0 closes when the final hardening scope is
audit-only, adds no proof home, Story label, writer, speech key, renderer
wording, LLM behavior, or public surface, the three King Check chains remain
separate and exhaustive for opened King Check meaning, MateThreat, mate in N,
forced mate, king safety, attack, pressure, initiative, best, only, winning,
decisive, no-counterplay, public route `200`, production API, and
public/user-facing LLM narration stay closed, and detailed authority lives only
in `StoryInteractionLaw.md`.

### KCNFC-1 Authority Separation

KCNFC-1 opens no new chess meaning.

KCNFC-1 audits only the three King Check chains:

- `CheckGivenProof` -> `Scene.CheckGiven` -> `SceneCheckGiven` -> `gives_check`
- `CheckEscapedProof` -> `Scene.CheckEscaped` -> `SceneCheckEscaped` ->
  `escapes_check`
- `CheckmateProof` -> `Scene.Checkmate` -> `SceneCheckmate` -> `checkmates`

KCNFC-1 must verify:

- CheckGiven owns only gives-check.
- CheckEscaped owns only escapes-check.
- Checkmate owns only actual checkmate.
- Checkmate is not stronger CheckGiven wording.
- CheckGiven is not Checkmate.
- CheckEscaped is not mate avoidance.
- No proof sidecar can be attached to the wrong Story.
- No writer can create the wrong Scene.
- No speech key can lower from the wrong Scene.

KCNFC-1 must reject:

- CheckGiven row with CheckmateProof
- Checkmate row with CheckGivenProof only
- CheckEscaped row with CheckmateProof
- Checkmate row created from SAN `#`
- Checkmate row created from BoardFacts-only or engine-only evidence

Completion standard: KCNFC-1 closes when all three chains remain one meaning,
one proof home, one Story label, one writer, one speech key, with no
cross-ownership.

### KCNFC-2 Collision Fixtures

KCNFC-2 opens no new chess meaning.

KCNFC-2 adds lightweight same-board fixtures for King Check collisions.

KCNFC-2 fixture categories:

- CheckGiven only
- CheckEscaped only
- Checkmate only
- cross-check: escapes check and gives check
- checkmate that also gives check
- stalemate: no legal move but not in check
- no-legal-move without check
- SAN `#` without proof
- engine mate line without Story
- contaminated sidecar rows

KCNFC-2 must verify:

- cross-check may produce CheckGiven and CheckEscaped only through separate
  complete proofs
- checkmate may coexist with CheckGiven only as separate proof-backed rows
- stalemate never becomes Checkmate
- no-legal-move without check never becomes Checkmate
- SAN `#`, raw PV, and engine mate text never create Story
- contaminated rows are blocked or silent

Completion standard: KCNFC-2 closes when collision fixtures prove King Check
rows do not borrow each other's proof, identity, claim key, renderer text, or
LLM wording.

### KCNFC-3 StoryTable Stability

KCNFC-3 opens no new chess meaning.

KCNFC-3 audits StoryTable behavior for King Check rows.

KCNFC-3 must verify:

- `StoryTable` may order existing CheckGiven, CheckEscaped, and Checkmate rows.
- `StoryTable` must not create any King Check Story.
- input order is stable
- duplicate same-meaning rows order deterministically or collapse according
  to existing rules
- Checkmate does not erase CheckGiven ownership
- CheckGiven does not upgrade to Checkmate
- CheckEscaped does not become mate avoidance
- Support / Context / Blocked rows produce no standalone text
- capped / refuted rows produce no standalone text

KCNFC-3 collision targets:

- CheckGiven
- CheckEscaped
- Checkmate
- Material
- Hanging / Fork
- Defense
- Line / Defender tactics
- open pawn/file scenes

Completion standard: KCNFC-3 closes when StoryTable ordering is
deterministic, King Check meanings remain separate, sibling Story families keep
ownership, and non-Lead/capped/refuted rows stay silent.

### KCNFC-4 Engine / Notation / Diagnostics Boundary

KCNFC-4 opens no new chess meaning.

KCNFC-4 audits non-Story inputs for the three King Check chains.

KCNFC-4 must verify:

- `EngineCheck` cannot create CheckGiven, CheckEscaped, or Checkmate.
- `EngineCheck` only supports, caps, or refutes existing Stories.
- raw engine eval, mate score, raw PV, and engine text never become public
  wording.
- SAN `+` and SAN `#` are notation only.
- `BoardFacts` check/mate/legal-move observations do not create Stories.
- proofFailures remain internal diagnostics only.
- source rows do not become public claim owners.

KCNFC-4 forbidden public wording:

- engine says
- mate score
- best move
- only move
- forced mate
- winning / decisive
- no counterplay
- safe king / unsafe king
- attack / pressure / initiative

Completion standard: KCNFC-4 closes when engine, notation, BoardFacts,
proofFailures, and source rows remain non-authoritative diagnostics or
observations and cannot create or strengthen King Check public claims.

### KCNFC-5 Downstream Boundary

KCNFC-5 opens no new chess meaning.

KCNFC-5 audits ExplanationPlan, Renderer, and LLM smoke only.

KCNFC-5 must verify:

- CheckGiven lowers only to `gives_check`.
- CheckEscaped lowers only to `escapes_check`.
- Checkmate lowers only to `checkmates`.
- no cross-claim lowering is possible.
- Renderer input remains `ExplanationPlan` only.
- Renderer text stays no stronger than the selected claim key.
- LLM smoke receives only:
  - renderedText
  - claimKey
  - strength
  - forbidden wording
  - `Rephrase only. Do not add chess facts.`
- LLM smoke cannot add new moves, lines, mate threat, mate in N, forced mate,
  engine explanation, king safety, attack, pressure, initiative, best, only,
  winning, decisive, or no-counterplay.

Completion standard: KCNFC-5 closes when downstream output is bounded to
`gives_check`, `escapes_check`, or `checkmates` only, and all non-selected,
capped, refuted, Support, Context, and Blocked rows remain silent.

### KCNFC-6 Docs / Public Surface

KCNFC-6 opens no new chess meaning.

KCNFC-6 audits documentation and public surface only.

KCNFC-6 docs rules:

- detailed KCNFC authority lives only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, and Manifest stay summary-only if touched.
- AGENTS.md remains unchanged unless durable operator rules change.
- docs authority tests must agree with the live authority list.

KCNFC-6 public surface rules:

- `/api/commentary/render` remains fail-closed.
- `/internal/commentary/render-local-probe` remains fail-closed.
- no public route `200`.
- no production API.
- no public/user-facing LLM narration.

Completion standard: KCNFC-6 closes when no detailed authority is duplicated
outside `StoryInteractionLaw.md` and all public surfaces remain closed.

### KCNFC-7 Final Verification

KCNFC-7 opens no new chess meaning.

KCNFC-7 final audit:

- CheckGiven chain remains separate.
- CheckEscaped chain remains separate.
- Checkmate chain remains separate.
- Checkmate is not CheckGiven plus stronger wording.
- CheckEscaped is not mate avoidance.
- MateThreat, mate in N, forced mate, king safety, attack, pressure,
  initiative, best, only, winning, decisive, and no-counterplay remain closed.
- SAN, BoardFacts, engine evidence, raw PV, proofFailures, and source rows do
  not create public claims.
- Renderer and LLM smoke remain bounded.
- public route `200`, production API, and public/user-facing LLM narration
  remain closed.

KCNFC-7 required verification:

- `sbt "commentary/testOnly lila.commentary.chess.KingCheckNeighborhoodCloseoutTest"`
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Completion standard: KCNFC-7 closes when King Check Neighborhood is
final-closed with exactly three public meanings, no new meaning opened by
hardening, all tests pass, and `git diff --check` passes.

### Stalemate-1 StalemateProof

Stalemate-1 opens only `StalemateProof` as a proof home.

Open only this proof meaning:

a legal move leaves the rival side not in check and with no legal moves on the
exact after-board

`StalemateProof` proves:

- stalemating side
- rival side
- legal move identity
- moving piece identity when available
- origin square
- destination square
- rival king square after the move
- after-board rival side is not in check
- after-board rival side has no legal moves
- exact after-board replay
- same-board proof
- stalemate was produced by this legal move

Stalemate-1 does not open:

- `Scene.Stalemate`
- `SceneStalemate`
- `stalemates`
- Story writer
- Verdict path
- ExplanationPlan
- renderer
- LLM narration
- public route `200`
- production API
- draw evaluation
- tablebase result
- blunder
- saved game
- thrown win
- winning / losing / decisive
- best / only / forced
- no counterplay

Boundary:

- `StalemateProof` is not a public Story.
- `StalemateProof` must not create `Scene.Stalemate`.
- BoardFacts may observe no-legal-move and check state, but does not own the
  public claim.
- `StalemateProof` does not replace `CheckmateProof`.
- `StalemateProof` does not prove draw evaluation, tablebase result, blunder,
  saved game, or thrown win.
- `StalemateProof.publicClaimAllowed` remains false.

Completion standard: Stalemate-1 closes when `StalemateProof` proves only
not-in-check plus no-legal-moves on the exact after-board, no Story writer,
Verdict, ExplanationPlan, renderer, LLM, public route, or production API opens,
and targeted tests plus `git diff --check` pass.

### Stalemate-2 Scene.Stalemate Writer

Stalemate-2 opens only `SceneStalemate` as the named writer for
`Scene.Stalemate`.

Stalemate-2 writer conditions:

- complete StoryProof
- complete `StalemateProof`
- same-board legal replay
- legal move leaves rival side not in check
- legal move leaves rival side with no legal moves
- rival king square is identified
- writer = `SceneStalemate`
- EngineCheck does not Refute

Stalemate-2 Story identity:

- scene = `Stalemate`
- tactic = None
- plan = None
- side = stalemating side
- rival = stalemated side
- target = rival king square after the move
- anchor = moving piece origin square
- route = legal stalemating move

Forbidden writer ownership:

- `SceneStalemate` must not create Checkmate.
- `SceneStalemate` must not create CheckGiven.
- `SceneStalemate` must not create CheckEscaped.
- `SceneStalemate` must not create draw-result, saved-game, thrown-win,
  blunder, best, only, forced, winning, losing, decisive, no-counterplay,
  engine, or tablebase meaning.

Stalemate-2 does not open:

- `stalemates` speech key
- ExplanationPlan
- renderer
- LLM narration
- public route `200`
- production API
- public/user-facing narration

Completion standard: Stalemate-2 closes when `SceneStalemate` writes only
complete `StalemateProof` rows into `Scene.Stalemate`, with no tactic, no plan,
stalemating side, rival side, target as the rival king square after the move,
anchor as the moving piece origin square, route as the legal stalemating move,
no EngineCheck Refutes lead, no Checkmate, CheckGiven, CheckEscaped,
draw-result, saved-game, thrown-win, blunder, best, only, forced, winning,
losing, decisive, no-counterplay, engine, or tablebase ownership, and no
downstream ExplanationPlan, renderer, LLM, public route, production API, or
public narration opens.

### Stalemate-3 Negative Corpus

Stalemate-3 opens only the negative corpus for the already opened
`StalemateProof` -> `Scene.Stalemate` path.

Stalemate-3 must stay silent for:

- illegal move
- missing same-board proof
- missing exact-board replay
- after-board rival side is in check
- after-board rival side has at least one legal move
- checkmate
- ordinary quiet move with legal replies
- side-to-move confusion
- missing rival king square
- incomplete StoryProof
- incomplete StalemateProof
- writerless row
- contaminated sidecar row
- BoardFacts no-legal-move without not-in-check proof
- SAN or result notation without proof
- engine/tablebase-only draw claim
- repetition / fifty-move / insufficient material / resignation / timeout
- draw, saves, blunder, thrown win, best, only, forced, winning, losing,
  decisive, no-counterplay wording

Stalemate-3 rules:

- No legal moves is not stalemate unless the rival side is not in check.
- Stalemate is not Checkmate.
- Stalemate is not a draw-evaluation explanation.
- Complete StalemateProof or silence.
- Stalemate-related blocked rows are not selected public Verdicts.
- EngineCheck, tablebase-looking evidence, SAN, result notation, and
  BoardFacts no-legal-move state cannot create or repair `Scene.Stalemate`.
- Stalemate-3 does not open the `stalemates` speech key, ExplanationPlan,
  renderer, LLM narration, public route `200`, production API, or
  public/user-facing narration.

Completion standard: Stalemate-3 closes when illegal moves, missing same-board
proof, missing exact-board replay, after-board check, rival legal replies,
checkmate, ordinary quiet moves, side-to-move confusion, missing rival king
square, incomplete StoryProof, incomplete StalemateProof, writerless rows,
contaminated sidecar rows, BoardFacts no-legal-move without not-in-check proof,
SAN/result notation without proof, engine/tablebase-only draw claims,
repetition, fifty-move, insufficient-material, resignation, timeout, and draw,
saves, blunder, thrown-win, best, only, forced, winning, losing, decisive, and
no-counterplay wording all stay silent, and the only positive path remains an
exact legal after-board where the rival side is not in check and has no legal
moves.

### Stalemate-4 EngineCheck Reuse

Stalemate-4 reuses only the existing `EngineCheck` boundary for already
existing complete `SceneStalemate` Stories.

Stalemate-4 rules:

- EngineCheck cannot create `Scene.Stalemate`.
- Supports creates no new claim.
- Caps suppresses standalone claim speech or keeps downstream speech bounded
  to stalemates where downstream speech is separately open.
- Refutes makes `Scene.Stalemate` Blocked.
- Unknown creates no engine expression.
- Engine evidence must bind to the same Story route and legal line.
- EngineCheck attaches only when the Story is `Scene.Stalemate`, writer is
  `SceneStalemate`, StoryProof is complete, `StalemateProof` is complete,
  the route is the legal stalemating move, and the proof is exact-board
  not-in-check plus no-legal-moves.
- EngineCheck cannot repair incomplete StoryProof, incomplete
  StalemateProof, writerless rows, route mismatch, illegal line mismatch, or
  engine/tablebase-only rows.

Forbidden engine wording:

- engine says draw
- tablebase draw
- eval number
- best move
- only move
- forced draw
- winning thrown away
- blunder
- decisive mistake

Stalemate-4 does not open:

- `stalemates` speech key
- draw claim
- tablebase claim
- blunder claim
- best / only / forced claim
- evaluation wording
- ExplanationPlan
- renderer
- LLM narration
- public route `200`
- production API
- public/user-facing narration

Completion standard: Stalemate-4 closes when EngineCheck attaches only to an
existing complete `SceneStalemate` Story and never creates or strengthens
stalemate, draw, tablebase, blunder, best/only/forced, or evaluation wording.

### Stalemate-5 StoryTable Integration

Integrate `Scene.Stalemate` only as an already existing proof-backed Story.

Stalemate-5 collision targets:

- `Scene.Stalemate`
- `Scene.Checkmate`
- `Scene.CheckGiven`
- `Scene.CheckEscaped`
- `Scene.Material`
- Hanging / Fork
- Defense
- Line / Defender tactics
- open pawn/file scenes

Stalemate-5 verifies:

- input order stability
- Stalemate does not own Checkmate.
- Checkmate does not own Stalemate.
- no-legal-move without check belongs only to Stalemate when not-in-check
  proof is complete
- no-legal-move with check belongs only to Checkmate when CheckmateProof is
  complete
- Stalemate owns no draw-evaluation, saved-game, blunder, thrown-win,
  tablebase, best, only, forced, winning, losing, decisive, or no-counterplay
  meaning
- capped/refuted/Support/Context/Blocked Stalemate rows have no standalone
  text

Stalemate-5 StoryTable rules:

- StoryTable may order only an already written `Scene.Stalemate` row.
- StoryTable must not create `Scene.Stalemate` from BoardFacts, EngineCheck,
  SAN, result notation, source rows, or sibling terminal rows.
- `Scene.Stalemate` remains separate from `Scene.Checkmate`, `Scene.CheckGiven`,
  and `Scene.CheckEscaped`.
- Checkmate proof sidecars cannot contaminate Stalemate rows.
- Stalemate proof sidecars cannot contaminate Checkmate, CheckGiven,
  CheckEscaped, Material, Tactic, Defense, pawn, or file rows.
- Non-Lead, capped, refuted, Support, Context, and Blocked Stalemate rows do
  not lower to ExplanationPlan, renderer, LLM, public route `200`, production
  API, or public/user-facing narration.

Stalemate-5 does not open:

- `stalemates` speech key
- draw evaluation
- saved-game claim
- blunder claim
- thrown-win claim
- tablebase claim
- best / only / forced claim
- winning / losing / decisive claim
- no-counterplay claim
- ExplanationPlan
- renderer
- LLM narration
- public route `200`
- production API
- public/user-facing narration

Completion standard: Stalemate-5 closes when StoryTable order is stable,
Checkmate and Stalemate remain separate, sibling claim homes keep ownership,
and non-Lead/capped/refuted rows stay silent.

### Stalemate-6 ExplanationPlan

Open only ExplanationPlan for selected uncapped Lead `Scene.Stalemate` Verdicts.

Allowed claim key: `stalemates` only.

Stalemate-6 forbidden claim keys:

- `checkmates`
- `gives_check`
- `escapes_check`
- `draws_game`
- `saves_game`
- `throws_win`
- `blunder`
- `tablebase_draw`
- `engine_says_draw`
- `best_move`
- `only_move`
- `forced`
- `winning`
- `losing`
- `decisive`
- `no_counterplay`

Stalemate-6 ExplanationPlan input boundary:

- `ExplanationPlan.fromSelected` accepts only selected uncapped Lead
  `Scene.Stalemate` Verdicts.
- Story must be `Scene.Stalemate`, `tactic = None`, `plan = None`, and
  `writer = SceneStalemate`.
- `target` is the rival king square after the move.
- `anchor` is the moving piece origin square.
- `route` is the legal stalemating move.
- `evidenceLine` is the route only.
- `StalemateProof` must still bind not-in-check plus no-legal-move
  after-board state.
- `StalemateProof` must still bind exact after-board replay, same-board proof,
  legal move identity, and stalemate produced by this legal move.

Stalemate-6 does not open:

- renderer
- LLM narration
- public route `200`
- production API
- public/user-facing narration
- draw evaluation
- saved-game claim
- blunder claim
- thrown-win claim
- tablebase claim
- engine-says-draw claim
- best / only / forced claim
- winning / losing / decisive claim
- no-counterplay claim

Renderer, LLM, public route, production API, and public narration remain closed.

Completion standard: Stalemate-6 closes when only selected uncapped Lead
Stalemate lowers to `stalemates`, all sibling/evaluation/draw/tablebase claim
keys remain closed, and Support/Context/Blocked/capped/refuted rows stay
silent.

### Stalemate-7 DeterministicRenderer

Open deterministic renderer text only from bounded Stalemate ExplanationPlan.

`DeterministicRenderer` input is `ExplanationPlan` only.

Stalemate-7 renderer must not accept or inspect:

- raw Story
- Verdict
- StalemateProof
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- source rows
- result notation
- tablebase input

Allowed Stalemate-7 templates:

- `{route} is stalemate.`
- `{route} stalemates the king.`

Stalemate-7 forbidden renderer wording:

- draws the game
- saves the game
- throws away the win
- blunder
- tablebase draw
- engine says
- best move
- only move
- forced
- winning / losing / decisive
- no counterplay

Stalemate-7 renderer rejects:

- Support rows
- Context rows
- Blocked rows
- capped rows
- refuted rows
- missing-claim plans
- sibling claim plans
- wrong-scene plans
- tactic-contaminated plans
- secondary-target-contaminated plans
- missing target, anchor, route, route SAN, or matching evidence line
- plans with empty forbidden wording

Stalemate-7 does not open:

- draw evaluation
- saved-game claim
- thrown-win claim
- blunder claim
- tablebase claim
- engine-says-draw claim
- best / only / forced claim
- winning / losing / decisive claim
- no-counterplay claim
- LLM narration
- public route `200`
- production API
- public/user-facing narration

Completion standard: Stalemate-7 closes when renderer emits only bounded
stalemate text from ExplanationPlan and all draw/eval/tablebase/best/only/
forced wording remains absent.

### Stalemate-8 LLM Smoke

Reuse only existing LLM smoke boundary for bounded Stalemate `RenderedLine`.

Allowed Stalemate-8 smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Stalemate-8 smoke must not accept or inspect:

- raw Story
- raw StalemateProof
- BoardFacts
- EngineCheck
- EngineLine / EngineEval
- raw PV
- proofFailures
- source rows
- result notation
- tablebase diagnostics

Stalemate-8 smoke must reject additions of:

- new move
- new line
- draw result explanation
- saves game
- throws win
- blunder
- tablebase
- engine mention
- best / only / forced
- winning / losing / decisive
- no counterplay
- checkmate
- check claim
- escape-check claim

Stalemate-8 does not open:

- raw Story input
- raw StalemateProof input
- BoardFacts input
- EngineCheck input
- EngineLine / EngineEval input
- raw PV input
- proofFailures input
- source-row input
- result-notation input
- tablebase-diagnostics input
- draw-result narration
- saved-game narration
- thrown-win narration
- blunder narration
- tablebase narration
- engine narration
- best / only / forced narration
- winning / losing / decisive narration
- no-counterplay narration
- sibling King Check narration
- public route `200`
- production API
- public/user-facing narration

Completion standard: Stalemate-8 closes when LLM smoke may only rephrase bounded
stalemates text and cannot add draw/eval/tablebase/engine/sibling King Check
facts.

### Stalemate-9 Closeout / Hard Cleanup

Stalemate-9 opens no new chess meaning.

Close only:

- a legal move leaves the rival side not in check and with no legal moves on
  the exact after-board

Authority audit:

- StalemateProof owns proof home.
- Scene.Stalemate owns Story label.
- SceneStalemate owns writer.
- `stalemates` owns speech key.
- These are not interchangeable.

Duplication audit:

- Checkmate = in check plus no legal escape.
- Stalemate = not in check plus no legal moves.
- CheckGiven = gives check.
- CheckEscaped = escapes check.
- No chain owns another chain.

Still closed:

- draw claim beyond stalemate
- saved game
- thrown win
- blunder
- tablebase
- engine says draw
- best / only / forced
- winning / losing / decisive
- no counterplay
- public route `200`
- production API
- public/user-facing LLM narration

Closeout verification:

- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`
- optional targeted Stalemate stage and closeout suites when present

Completion standard: Stalemate-9 closes when StalemateProof,
Scene.Stalemate, SceneStalemate, and `stalemates` remain separated; Checkmate
and Stalemate cannot steal each other; downstream text stays bounded; public
surfaces remain closed; tests pass; and `git diff --check` passes.

## ATIH Stage-0 Charter

Work name: Attack-Target Interaction Hardening.

ATIH-0 opens no new chess meaning.

ATIH-0 opens no new proof home, Story label, writer, speech key, renderer
wording, LLM narration behavior, public route `200`, or production API. It
hardens only interactions among already-open attack-target and material-contact
rows.

Locked existing chains:

- `QueenHitProof` -> `Tactic.QueenHit` -> `TacticQueenHit` -> `attacks_queen`
- `LoosePieceProof` -> `Tactic.Loose` -> `TacticLoose` -> `attacks_loose_piece`
- existing Hanging proof path -> `Tactic.Hanging` -> `TacticHanging` -> `can_win_piece`
- existing material proof path -> `Scene.Material` -> `SceneMaterial` -> `material_balance_changes`

ATIH-0 ownership rules:

- QueenHit owns only a queen-specific attack claim.
- Loose owns only an undefended non-king piece attack claim.
- Hanging owns only a capturable non-pawn, non-king target with bounded
  positive material proof.
- Material owns only actual material balance change from the existing material
  proof path.
- Attack-only QueenHit or Loose rows cannot create Hanging, Material, free
  piece, en prise, wins-piece, wins-material, or material-gain speech.
- Mixed QueenHit/Loose sidecars are contamination, not a combined public claim.

ATIH-0 still closed:

- wins queen
- traps queen
- queen is lost
- hanging from Loose
- material gain from attack-only rows
- free piece
- en prise
- underdefended
- overloaded defender
- pressure
- initiative
- tempo
- best move
- only move
- forced move
- decisive
- winning
- public route `200`
- production API
- public/user-facing LLM narration

Documentation rule: detailed ATIH authority lives only in
`StoryInteractionLaw.md`. README, SSOT, Architecture, Contract, and Manifest
may summarize only if docs tests require it. `AGENTS.md` remains unchanged.

Completion standard: ATIH-0 closes when QueenHit, Loose, Hanging, and Material
keep exactly their existing proof, Story, writer, and speech-key chains;
QueenHit and Loose attack-only rows cannot become Hanging or Material claims;
QueenHit/Loose mixed sidecars stay blocked; forbidden attack-target,
material-gain, tempo, pressure, initiative, best/only/forced,
decisive/winning, public route `200`, production API, and public/user-facing
LLM narration surfaces remain closed; targeted ATIH tests pass; docs authority
tests pass when touched; and `git diff --check` passes.

## ATIH Stage-1 Authority Inventory

ATIH-1 opens authority inventory tests only.

ATIH-1 opens no runtime meaning expansion, no proof home, no Story label, no
writer, no speech key, no renderer wording, no LLM narration behavior, no
public route `200`, and no production API.

Authority separation inventory:

- QueenHitProof is not Tactic.QueenHit.
- Tactic.QueenHit is not TacticQueenHit.
- TacticQueenHit is not attacks_queen.
- LoosePieceProof is not Tactic.Loose.
- Tactic.Loose is not TacticLoose.
- TacticLoose is not attacks_loose_piece.
- Tactic.Hanging is not Scene.Material.
- Scene.Material is not Tactic.Hanging.

Role inventory:

- QueenHit owns only queen-attacked meaning.
- Loose owns only undefended non-king piece attacked meaning.
- Hanging owns only its already-open bounded hanging/can-win-piece meaning.
- Material owns only actual material balance change now.

Creation boundary inventory:

- BoardFacts cannot create any of these Stories.
- EngineCheck cannot create any of these Stories.
- StoryTable cannot create any of these Stories.
- ExplanationPlan, Renderer, and LLM smoke cannot create, repair, rank, or strengthen these meanings.

ATIH-1 documentation rule: detailed authority inventory lives only in
`StoryInteractionLaw.md`. README, SSOT, Architecture, Contract, and Manifest
may summarize only if docs tests require it. `AGENTS.md` remains unchanged.

Completion standard: ATIH-1 closes when tests prove the QueenHit, Loose,
Hanging, and Material proof, Story-label, writer, and speech-key layers stay
separate; BoardFacts, EngineCheck, StoryTable, ExplanationPlan, Renderer, and
LLM smoke cannot create, repair, rank, or strengthen those meanings; no runtime
meaning expansion is introduced; detailed authority remains only in
`StoryInteractionLaw.md`; targeted ATIH tests pass; docs authority tests pass
when touched; and `git diff --check` passes.

## ATIH Stage-2 Collision Fixtures

ATIH-2 opens collision fixture corpus only.

ATIH-2 opens no public wording expansion, no new chess meaning, no proof home,
no Story label, no writer, no speech key, no renderer wording, no LLM narration
behavior, no public route `200`, and no production API.

Required collision fixture categories:

- QueenHit-only: legal move attacks rival queen, no material change.
- Loose-only: legal move attacks one undefended rival non-king non-queen piece, no material change.
- QueenHit plus Loose-looking overlap: rival queen is undefended and attacked.
- Hanging-only: existing hanging proof complete, no QueenHit/Loose speech steal.
- Material-only: actual material balance changes now, no attack-only claim required.
- Attack-only not Material: queen or loose piece attacked but no capture/material result.
- Material not Loose: capture/material change where loose-piece proof is incomplete.
- Loose not Hanging: target is attacked and undefended, but no bounded hanging capture proof.
- QueenHit not wins-queen: queen attacked but not proven lost/trapped.
- Capped/refuted rows for each chain.

Expected ownership:

- Queen target belongs to QueenHit-specific speech, not generic Loose speech,
  unless the implementation explicitly supports both as non-Lead and
  non-speaking. Preferred: QueenHit Lead, Loose silent or Support with no standalone text.
- Loose cannot lower to Hanging, Material, pressure, tempo, or wins-piece wording.
- Attack-only rows cannot produce Scene.Material.
- Actual material result remains Scene.Material home.
- Hanging remains bounded to the existing `can_win_piece` path and cannot be
  created from QueenHit or Loose attack-only proof.
- Capped rows produce no standalone rendered line.
- Refuted rows are blocked and produce no standalone rendered line.

ATIH-2 documentation rule: detailed collision fixture authority lives only in
`StoryInteractionLaw.md`. README, SSOT, Architecture, Contract, and Manifest
may summarize only if docs tests require it. `AGENTS.md` remains unchanged.

Completion standard: ATIH-2 closes when the collision fixture corpus proves
QueenHit-only, Loose-only, QueenHit plus Loose-looking overlap, Hanging-only,
Material-only, attack-only-not-Material, Material-not-Loose, Loose-not-Hanging,
QueenHit-not-wins-queen, and capped/refuted rows for each chain; QueenHit owns
queen attack speech, Loose owns only undefended non-king non-queen piece attack
speech, Hanging owns only already-open bounded hanging/can-win-piece meaning,
Material owns actual material balance change now, attack-only rows cannot
produce Scene.Material, actual material result remains Scene.Material home, no
public wording expansion is introduced, detailed authority remains only in
`StoryInteractionLaw.md`, targeted ATIH tests pass, docs authority tests pass
when touched, and `git diff --check` passes.

## ATIH Stage-3 StoryTable Stability

ATIH-3 opens StoryTable ordering hardening only.

ATIH-3 opens no new chess meaning, no proof home, no Story label, no writer, no
speech key, no renderer wording, no LLM narration behavior, no public route
`200`, and no production API.

Required StoryTable stability checks:

- input order stability across QueenHit, Loose, Hanging, and Material rows
- at most one Lead for the same route when meanings collide
- selected Lead is deterministic
- Support, Context, and Blocked rows have no standalone text.
- capped and refuted rows have no standalone text.
- sibling rows do not lend proof sidecars to each other.

Recommended collision priority for the same route:

- actual material balance change now may lead as `Scene.Material`
- complete Hanging may lead over weaker attack-only wording
- QueenHit owns queen-specific attack wording
- Loose owns non-queen undefended piece attack wording
- if QueenHit and Loose both match a queen target, QueenHit owns public wording

Forbidden StoryTable upgrades:

- StoryTable must not upgrade QueenHit to wins queen.
- StoryTable must not upgrade Loose to Hanging.
- StoryTable must not upgrade attack-only rows to Material.
- StoryTable must not use raw source rows, proofFailures, or EngineCheck to create a Lead.

ATIH-3 documentation rule: detailed StoryTable stability authority lives only
in `StoryInteractionLaw.md`. README, SSOT, Architecture, Contract, and
Manifest may summarize only if docs tests require it. `AGENTS.md` remains
unchanged.

Completion standard: ATIH-3 closes when StoryTable ordering is stable across
input order for QueenHit, Loose, Hanging, and Material rows; same-route
collisions produce at most one deterministic Lead; non-Lead, Support, Context,
Blocked, capped, and refuted rows produce no standalone text; sibling rows do
not lend proof sidecars or upgrade one another; QueenHit cannot become
wins-queen speech, Loose cannot become Hanging, attack-only rows cannot become
Material, raw source rows, proofFailures, and EngineCheck cannot create a Lead,
detailed authority remains only in `StoryInteractionLaw.md`, targeted ATIH
tests pass, docs authority tests pass when touched, and `git diff --check`
passes.

## ATIH Stage-4 EngineCheck Boundary

ATIH-4 opens EngineCheck interaction tests only.

ATIH-4 opens no new chess meaning, no proof home, no Story label, no writer, no
speech key, no renderer wording, no LLM narration behavior, no public route
`200`, and no production API.

EngineCheck interaction rules:

- EngineCheck supports, caps, and refutes only already existing proof-backed Stories.
- EngineCheck cannot create QueenHit.
- EngineCheck cannot create Loose.
- EngineCheck cannot create Hanging.
- EngineCheck cannot create Material.
- Supports creates no new claim.
- Caps suppresses or bounds already selected speech.
- Refutes blocks the affected Story.
- Unknown creates no engine expression.

Forbidden EngineCheck public wording:

- engine says
- eval number
- raw PV
- best move
- only move
- forced
- wins queen
- wins piece
- wins material
- pressure
- initiative
- decisive
- winning

Rule: EngineCheck may cap or refute a claim; it never broadens attack-only meaning into material or tactical certainty.

ATIH-4 documentation rule: detailed EngineCheck boundary authority lives only
in `StoryInteractionLaw.md`. README, SSOT, Architecture, Contract, and
Manifest may summarize only if docs tests require it. `AGENTS.md` remains
unchanged.

Completion standard: ATIH-4 closes when tests prove EngineCheck can support,
cap, or refute only already existing proof-backed QueenHit, Loose, Hanging, and
Material Stories; EngineCheck cannot create QueenHit, Loose, Hanging, or
Material; Supports creates no new claim; Caps suppresses or bounds already
selected speech; Refutes blocks the affected Story; Unknown creates no engine
expression; EngineCheck public wording cannot say engine says, eval number,
raw PV, best move, only move, forced, wins queen, wins piece, wins material,
pressure, initiative, decisive, or winning; EngineCheck never broadens
attack-only meaning into material or tactical certainty; detailed authority
remains only in `StoryInteractionLaw.md`; targeted ATIH tests pass; docs
authority tests pass when touched; and `git diff --check` passes.

## ATIH Stage-5 Downstream Boundary

ATIH-5 opens ExplanationPlan, Renderer, and LLM smoke hardening only.

ATIH-5 opens no new chess meaning, no proof home, no Story label, no writer, no
speech key, no new public wording family, no public route `200`, and no
production API.

Allowed ATIH claim keys:

- `attacks_queen`
- `attacks_loose_piece`
- `can_win_piece`
- `material_balance_changes`

ExplanationPlan boundary:

- ExplanationPlan accepts only selected uncapped Lead Verdicts for public claim lowering.
- ExplanationPlan rejects Support, Context, Blocked, capped, and refuted rows for public claim lowering.
- ExplanationPlan rejects sibling claim-key substitution.
- ExplanationPlan rejects incomplete proof sidecars and contaminated rows.
- Existing internal relation-only plans do not create public claim ownership or
  standalone rendered text.

Renderer boundary:

- Renderer input is ExplanationPlan only.
- Renderer emits only the selected claim key's bounded template.
- Renderer cannot inspect raw Story, proofs, BoardFacts, EngineCheck, raw PV, proofFailures, or source rows.

LLM smoke boundary:

- LLM smoke input is only renderedText, claimKey, strength, and forbidden wording.
- The LLM smoke instruction remains: `Rephrase only. Do not add chess facts.`
- LLM smoke must reject added material, queen-trap, hanging, pressure, tempo, best/only/forced, winning, or engine facts.

Forbidden downstream leaks:

- proofFailures must not become public text.
- raw engine eval/PV must not become public wording.
- source rows must not become public claim owners.

ATIH-5 documentation rule: detailed downstream boundary authority lives only in
`StoryInteractionLaw.md`. README, SSOT, Architecture, Contract, and Manifest
may summarize only if docs tests require it. `AGENTS.md` remains unchanged.

Completion standard: ATIH-5 closes when tests prove only `attacks_queen`,
`attacks_loose_piece`, `can_win_piece`, and `material_balance_changes` lower
from selected uncapped Lead Verdicts into public claim-bearing plans; Support,
Context, Blocked, capped, refuted, incomplete, contaminated, and sibling
claim-key-substituted rows produce no public claim and no standalone rendered
text; Renderer accepts only ExplanationPlan and emits only the bounded template
for the selected claim key; Renderer cannot inspect raw Story, proofs,
BoardFacts, EngineCheck, raw PV, proofFailures, or source rows; LLM smoke
receives only renderedText, claimKey, strength, forbidden wording, and the
`Rephrase only. Do not add chess facts.` instruction; LLM smoke rejects added
material, queen-trap, hanging, pressure, tempo, best/only/forced, winning, and
engine facts; proofFailures, raw engine eval/PV, and source rows never become
public text or public claim owners; detailed authority remains only in
`StoryInteractionLaw.md`; targeted ATIH tests pass; docs authority tests pass
when touched; and `git diff --check` passes.

## ATIH Stage-6 Docs / Public Surface

ATIH-6 opens documentation and public-surface audit only.

ATIH-6 opens no new chess meaning, no proof home, no Story label, no writer, no
speech key, no runtime meaning expansion, no public route `200`, and no
production API.

ATIH-6 docs rules:

- detailed ATIH authority lives only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, and Manifest remain summary-only if touched.
- `AGENTS.md` remains unchanged unless durable operator rules change.
- docs tests must prevent duplicated detailed ATIH law outside `StoryInteractionLaw.md`.

ATIH-6 public surface rules:

- `/api/commentary/render` remains fail-closed.
- `/internal/commentary/render-local-probe` remains fail-closed.
- no public route `200`.
- no production API.
- no public/user-facing LLM narration.

ATIH-6 verification rules:

- no route opens because attack-target hardening exists.
- no env switch or local probe returns production-style rendered payload.

Completion standard: ATIH-6 closes when detailed ATIH authority still lives
only in `StoryInteractionLaw.md`; README, SSOT, Architecture, Contract, and
Manifest remain summary-only if touched; `AGENTS.md` remains unchanged unless
durable operator rules change; docs tests prevent duplicated detailed ATIH law
outside `StoryInteractionLaw.md`; `/api/commentary/render` and
`/internal/commentary/render-local-probe` stay registered only as fail-closed
tombstones with no `200`; attack-target hardening opens no route, production
API, public/user-facing LLM narration, env switch, local-probe rendered payload,
or production-style rendered payload; targeted ATIH tests pass; docs authority
tests pass; and `git diff --check` passes.

## ATIH Stage-7 Closeout / Final Verification

ATIH-7 opens no new chess meaning.

ATIH-7 closes only Attack-Target Interaction Hardening.

ATIH-7 closeout audit:

- QueenHit remains queen-attacked only.
- Loose remains undefended non-king piece attacked only.
- Hanging remains its existing bounded hanging/can-win-piece meaning only.
- Material remains actual material balance change now only.
- No proof home, Story label, writer, or speech key is reused as another layer.
- No attack-only row creates material.
- No Loose row creates Hanging.
- No QueenHit row creates wins-queen or trap.
- No Material row is created from BoardFacts, EngineCheck, raw source, or renderer text.

ATIH-7 still closed:

- wins queen
- traps queen
- queen is lost
- hanging from Loose
- wins piece/material from attack-only rows
- free piece
- en prise
- underdefended
- overloaded defender
- pressure
- initiative
- tempo
- best / only / forced
- decisive / winning
- public route `200`
- production API
- public/user-facing LLM narration

ATIH-7 required verification:

- targeted ATIH test suite
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Completion standard: ATIH closes when QueenHit, Loose, Hanging, and Material
keep separate proof homes, Story labels, writers, speech keys, StoryTable roles,
ExplanationPlan claim keys, renderer templates, and LLM smoke boundaries; no new
meaning opens; public surfaces remain closed; targeted ATIH tests pass;
`ChessFoundationTest` passes; `ChessDocsAuthorityTest` passes; and
`git diff --check` passes.

## QueenHit Stage-1 Proof Home

QueenHit-1 opens exactly one proof home:

- proof home: `QueenHitProof`

QueenHit-1 does not open a public Story, `Tactic.QueenHit` writer,
`TacticQueenHit`, speech key, StoryTable Lead, ExplanationPlan, renderer, or
LLM smoke path.

QueenHitProof proves:

- attacking side
- rival side
- legal move identity
- origin square
- destination square
- rival queen square after the move
- attacking piece square after the move
- exact after-board replay
- same-board proof
- moving side attacks rival queen on the after-board
- queen-hit was produced or revealed by this legal move

QueenHit-1 must not open:

- public Story
- `Tactic.QueenHit`
- `TacticQueenHit`
- `attacks_queen`
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- material gain
- wins queen
- queen trap
- tempo

QueenHit-1 proof boundary:

- `QueenHitProof` is not public Story.
- BoardFacts may observe pieces, attacks, and queen location, but does not
  create QueenHit.
- Complete `QueenHitProof` or silence.
- `QueenHitProof.publicClaimAllowed` remains false.
- `QueenHitProof` must not attach to `Story`.
- `QueenHitProof` must not create or require `StoryProof`.
- `EngineCheck` does not consume QueenHitProof in Stage-1.
- Downstream layers must not mention QueenHitProof or produce QueenHit text.

QueenHit-1 negative corpus must keep silent:

- illegal move
- missing same-board proof
- missing exact after-board replay
- no rival queen after the move
- rival queen present but not attacked after the move
- BoardFacts attack observation without complete `QueenHitProof`
- any runtime `TacticQueenHit`, `attacks_queen`, QueenHit ExplanationPlan,
  renderer, LLM smoke, material-gain, wins-queen, queen-trap, or tempo surface

QueenHit-1 documentation rule:

- detailed QueenHit stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: QueenHit-1 closes when `QueenHitProof` proves only the
complete legal same-board after-board queen-hit evidence listed above; BoardFacts
remains observation only; no public Story, `TacticQueenHit`, `attacks_queen`,
StoryTable Lead, ExplanationPlan, Renderer, LLM smoke, material-gain,
wins-queen, queen-trap, or tempo path opens; targeted runtime tests pass; docs
authority tests pass when touched; and `git diff --check` passes.

## QueenHit Stage-2 Writer

QueenHit-2 opens only:

- named writer: `TacticQueenHit`
- Story label: `Tactic.QueenHit`

QueenHit-2 writer may create a Story only when:

- complete `StoryProof`
- complete `QueenHitProof`
- same-board legal replay
- legal route matches proof route
- rival queen target is present after the move
- moving side attacks that queen after the move
- writer = `TacticQueenHit`
- EngineCheck does not Refute

QueenHit-2 Story identity:

- scene = None, encoded as `Scene.Tactic` plus `Tactic.QueenHit`
- tactic = `QueenHit`
- plan = None
- side = attacking side
- rival = queen owner
- target = rival queen square after the move
- anchor = attacking piece square after the move
- route = legal queen-hitting move

QueenHit-2 does not open:

- Hanging
- Fork
- Skewer
- Pin
- RemoveGuard
- Material
- wins queen
- tempo
- queen trap
- best / only / forced
- downstream text
- `attacks_queen`
- ExplanationPlan claim mapping
- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

QueenHit-2 boundary:

- `TacticQueenHit` may attach only `QueenHitProof`; sibling proof homes must
  stay empty.
- `QueenHitProof` remains the proof home; `TacticQueenHit` owns only Story
  construction.
- BoardFacts observations cannot create QueenHit without `QueenHitProof`.
- EngineCheck may only block a QueenHit Story by Refutes; it must not create
  QueenHit, explain QueenHit, or strengthen QueenHit.
- StoryTable may keep the Stage-2 row as non-speaking context, but Stage-2
  opens no Lead speech path.
- ExplanationPlan, renderer, and LLM smoke must produce no QueenHit wording.

QueenHit-2 negative corpus must stay silent or blocked for:

- incomplete `StoryProof`
- incomplete `QueenHitProof`
- illegal move
- route mismatch between Story and proof
- missing same-board legal replay
- no rival queen after the move
- rival queen not attacked by moving side after the move
- EngineCheck Refutes
- writerless `Tactic.QueenHit`
- QueenHit proof attached to non-QueenHit row
- QueenHit writer copied onto Hanging, Fork, Skewer, Pin, RemoveGuard, or
  Material identity
- wins-queen, tempo, queen-trap, best, only, forced, material, or downstream
  wording

QueenHit-2 documentation rule:

- detailed QueenHit stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: QueenHit-2 closes when `TacticQueenHit` creates only
`Scene.Tactic` plus `Tactic.QueenHit` rows from complete `StoryProof` and
complete `QueenHitProof`, with side/rival/target/anchor/route bound to the
exact after-board queen attack; EngineCheck Refutes blocks the row; sibling
tactics and Material stay closed; wins-queen, tempo, queen-trap, best, only,
forced, ExplanationPlan claim mapping, renderer, LLM smoke, public route `200`,
production API, and public/user-facing LLM narration remain closed; targeted
runtime tests pass; docs authority tests pass when touched; and `git diff
--check` passes.

## QueenHit Stage-3 Negative Corpus

QueenHit-3 opens only:

- negative corpus for QueenHit false positives

QueenHit-3 must stay silent for:

- illegal move
- missing same-board proof
- missing exact after-board replay
- rival queen absent after move
- attacked piece is not queen
- queen belongs to moving side
- queen was attacked only on before-board
- queen hit already existed before the move and was not produced or revealed
  by the legal move
- attack line blocked on after-board
- pinned attacker cannot legally attack queen when the local proof requires
  legal attack
- attack exists only in raw notation or SAN
- BoardFacts-only attack without `QueenHitProof`
- EngineCheck-only queen pressure
- source row text saying queen hit
- incomplete StoryProof
- incomplete `QueenHitProof`
- writerless row
- contaminated sidecar row

QueenHit-3 forbidden wording:

- wins queen
- queen is lost
- queen trap
- tempo
- initiative
- pressure
- decisive
- winning
- best move
- only move
- forced move

QueenHit-3 rules:

- Attacking the queen is not material gain.
- Attacking the queen is not tempo unless a separate proof-backed slice opens
  it.
- `QueenHitProof` must prove a legal after-board attack on the rival queen, not
  just geometric contact.
- `QueenHitProof` must prove that the queen-hit was produced or revealed by
  this legal move, not merely still present after an unrelated move.
- SAN, raw notation, source text, BoardFacts attack observations, and
  EngineCheck evidence cannot create or repair QueenHit.
- A QueenHit sidecar on another row is contamination and must block, not
  reclassify that row.
- Stage-3 opens no new Story label, writer, StoryTable Lead admission,
  ExplanationPlan key, renderer template, LLM smoke behavior, public route
  `200`, production API, or public/user-facing LLM narration.

Completion standard: QueenHit-3 closes when illegal moves, missing same-board
proof, missing exact replay, absent rival queen, non-queen target, own queen,
before-board-only attack, pre-existing queen attack, blocked after-board line,
pinned illegal attack, raw notation/SAN-only evidence, BoardFacts-only attack,
EngineCheck-only pressure, source-row text, incomplete StoryProof, incomplete
`QueenHitProof`, writerless rows, and contaminated sidecars all stay silent or
blocked; wins-queen, queen-lost, queen-trap, tempo, initiative, pressure,
decisive, winning, best, only, and forced wording remain closed; attacking the
queen remains neither material gain nor tempo; targeted runtime tests pass;
docs authority tests pass when touched; and `git diff --check` passes.

## QueenHit Stage-4 EngineCheck Reuse

QueenHit-4 opens only:

- reuse existing `EngineCheck` boundary only

QueenHit-4 rules:

- EngineCheck cannot create `Tactic.QueenHit`.
- Supports creates no new claim.
- Caps suppresses standalone speech or keeps selected speech bounded to `attacks_queen`.
- Refutes makes QueenHit Blocked.
- Unknown creates no engine expression.
- Engine evidence must bind to the same Story route and legal line.

QueenHit-4 does not open:

- engine says
- eval number
- raw PV
- best move
- only move
- forced
- wins queen
- queen is trapped
- material gain
- public wording from proofFailures

QueenHit-4 boundary:

- `EngineCheck` may attach only to an already proof-backed
  `Tactic.QueenHit` row from `TacticQueenHit`.
- The receiving row must still carry complete `StoryProof`, complete
  `QueenHitProof`, clean QueenHit identity, and matching side/rival/target/
  anchor/route bindings.
- Engine-only evidence, stale evidence, wrong-route evidence, wrong-legal-line
  evidence, proofless copied rows, writerless rows, and contaminated sidecar
  rows cannot receive QueenHit EngineCheck authority.
- `Supports`, `Caps`, `Refutes`, and `Unknown` remain internal status
  diagnostics. They do not create renderer text, LLM input, public JSON, public
  route `200`, production API behavior, or public/user-facing narration.
- QueenHit still has no StoryTable Lead admission, ExplanationPlan claim key,
  deterministic renderer template, or LLM smoke path in Stage-4.

Completion standard: QueenHit-4 closes when EngineCheck may support, cap, or refute only an already proof-backed `Tactic.QueenHit`.

## QueenHit Stage-5 StoryTable Integration

QueenHit-5 opens only:

- StoryTable ordering for existing proof-backed `Tactic.QueenHit`

QueenHit-5 collision targets:

- `Scene.Material`
- `Tactic.Hanging`
- `Tactic.Fork`
- `Tactic.Skewer`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.DiscoveredAttack`
- `Scene.Defense`

QueenHit-5 rules:

- StoryTable may order QueenHit, not create it.
- Material capture result stays in `Scene.Material` or existing material home.
- Fork stays Fork when two targets are proven.
- Skewer stays Skewer when front/rear line proof is proven.
- Pin stays Pin when pinned-to-king proof is proven.
- RemoveGuard stays RemoveGuard when defender removal proof is proven.
- QueenHit owns only "the queen is attacked after this legal move."

QueenHit-5 rows that must not speak:

- Support
- Context
- Blocked
- capped
- refuted
- non-Lead

QueenHit-5 boundary:

- `StoryTable` may admit a clean standalone `Tactic.QueenHit` Lead only when it
  was already created by `TacticQueenHit` with complete `StoryProof`, complete
  `QueenHitProof`, same-board legal replay, exact after-board replay, clean
  side/rival/target/anchor/route bindings, and no refuting `EngineCheck`.
- `StoryTable` cannot repair missing proof, missing writer identity, copied
  sidecars, BoardFacts-only attacks, EngineCheck-only pressure, source-row
  wording, or proof failures.
- When QueenHit coexists with the collision targets above, the opened collision
  home remains the Lead if its own proof is complete. QueenHit stays bounded to
  the narrower fact that the queen is attacked after this legal move.
- Stage-5 does not open `ExplanationPlan`, renderer text, LLM smoke,
  `attacks_queen` speech, public route `200`, production API behavior, or
  public/user-facing LLM narration.

Completion standard: QueenHit-5 closes when StoryTable can order only existing proof-backed `Tactic.QueenHit`, yields to the listed proof homes, keeps Support/Context/Blocked/capped/refuted/non-Lead QueenHit rows without speech, targeted runtime tests pass, docs authority tests pass, and `git diff --check` passes.

## QueenHit Stage-6 ExplanationPlan

QueenHit-6 opens only:

- ExplanationPlan for selected uncapped Lead `Tactic.QueenHit` only

QueenHit-6 allowed claim key:

- `attacks_queen`

QueenHit-6 input boundary:

- selected Lead Verdict
- uncapped
- not refuted
- Story tactic = QueenHit
- writer = `TacticQueenHit`
- complete StoryProof
- complete `QueenHitProof`
- target = rival queen square
- anchor = attacking piece square
- route = legal queen-hitting move
- evidenceLine = route only

QueenHit-6 forbidden claim keys:

- wins_queen
- traps_queen
- gains_tempo
- wins_material
- best_move
- only_move
- forced_move
- decisive
- winning

QueenHit-6 rules:

- ExplanationPlan does not prove QueenHit.
- ExplanationPlan does not repair missing proof.
- Support/Context/Blocked/capped/refuted rows stay silent.

QueenHit-6 boundary:

- `ExplanationPlan` may lower only a selected uncapped Lead Verdict whose Story
  was already admitted by `TacticQueenHit`, already ordered by `StoryTable`,
  and still carries complete, clean `StoryProof` and `QueenHitProof`.
- The plan evidence line is exactly the Story route. It must not include raw PV,
  reply lines, source text, proof failures, BoardFacts diagnostics, or engine
  wording.
- Stage-6 does not open deterministic renderer text, LLM smoke, public route
  `200`, production API behavior, public/user-facing LLM narration, wins-queen,
  queen-trap, tempo, material gain, best/only/forced, decisive, or winning
  wording.

Completion standard: QueenHit-6 closes when only selected uncapped Lead `Tactic.QueenHit` Verdicts from `TacticQueenHit` lower to `attacks_queen` with target/anchor/route/evidenceLine bound to complete `QueenHitProof`, forbidden claim keys remain closed, Support/Context/Blocked/capped/refuted rows stay silent, targeted runtime tests pass, docs authority tests pass, and `git diff --check` passes.

## QueenHit Stage-7 DeterministicRenderer

QueenHit-7 opens only:

- renderer text from bounded QueenHit ExplanationPlan only

QueenHit-7 renderer input:

- ExplanationPlan only

QueenHit-7 allowed template:

- `"{route} attacks the queen on {target}."`

QueenHit-7 forbidden renderer input:

- raw Story
- raw `QueenHitProof`
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- source rows

QueenHit-7 forbidden wording:

- wins the queen
- traps the queen
- the queen is lost
- gains tempo
- wins material
- best move
- only move
- forced
- decisive
- winning
- engine says

QueenHit-7 rules:

- Renderer emits no text for Support/Context/Blocked/capped/refuted rows.
- Renderer emits no text for sibling claim keys.

QueenHit-7 boundary:

- The deterministic renderer reads `ExplanationPlan` only. It must not read raw
  Story, raw `QueenHitProof`, BoardFacts, EngineCheck, raw PV, proofFailures,
  source rows, or proof diagnostics.
- The renderer does not prove QueenHit, repair missing proof, select a Story,
  order a row, strengthen speech, or add engine expression.
- Stage-7 does not open LLM smoke, public route `200`, production API behavior,
  public/user-facing LLM narration, wins-queen, queen-trap, queen-lost, tempo,
  material gain, best/only/forced, decisive, or winning wording.

Completion standard: QueenHit-7 closes when the deterministic renderer emits only `"{route} attacks the queen on {target}."` from selected uncapped Lead QueenHit `ExplanationPlan` rows with `attacks_queen`, remains silent for Support/Context/Blocked/capped/refuted and sibling-claim rows, accepts no raw proof/story/engine/source input path, targeted runtime tests pass, docs authority tests pass, and `git diff --check` passes.

## QueenHit Stage-8 LLM Smoke

QueenHit-8 opens only:

- LLM smoke for bounded QueenHit `RenderedLine` only

QueenHit-8 allowed LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

QueenHit-8 allowed claimKey:

- `attacks_queen`

QueenHit-8 LLM must not add:

- wins queen
- queen trap
- queen is lost
- tempo
- material gain
- engine line
- best move
- only move
- forced move
- decisive
- winning
- new move
- new variation

QueenHit-8 rules:

- LLM only polishes.
- Verifier rejects overclaim.
- public/user-facing LLM narration remains closed.

QueenHit-8 boundary:

- LLM smoke receives only the bounded rendered-line contract. It must not
  receive raw Story, raw `QueenHitProof`, BoardFacts, EngineCheck, raw PV,
  proofFailures, source rows, queen-square diagnostics, attacking-piece
  diagnostics, exact after-board replay diagnostics, or same-board proof text.
- LLM smoke may echo or lightly rephrase only the already rendered bounded
  `attacks_queen` event.
- LLM smoke must not create QueenHit proof, repair missing proof, select a
  Story, order a row, add an engine expression, or become public narration.
- Stage-8 does not open public route `200`, production API behavior,
  public/user-facing LLM narration, wins-queen, queen-trap, queen-lost, tempo,
  material gain, engine-line, best/only/forced, decisive, winning, new-move, or
  new-variation speech.

Completion standard: QueenHit-8 closes when LLM smoke accepts only bounded QueenHit `RenderedLine` input with `attacks_queen`, uses only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`, rejects wins-queen, queen-trap, queen-lost, tempo, material-gain, engine-line, best/only/forced, decisive, winning, new-move, and new-variation additions, keeps public/user-facing LLM narration closed, targeted runtime tests pass, docs authority tests pass, and `git diff --check` passes.

## QueenHit Stage-9 Closeout / Hard Cleanup

QueenHit-9 opens no new chess meaning.

QueenHit-9 closes only narrow `Tactic.QueenHit`.

QueenHit-9 authority audit:

- `QueenHitProof` owns proof home.
- `Tactic.QueenHit` owns Story label.
- `TacticQueenHit` owns writer.
- `attacks_queen` owns speech key.
- These are not interchangeable.

QueenHit-9 duplication audit:

- QueenHit is not Hanging.
- QueenHit is not Fork.
- QueenHit is not Skewer.
- QueenHit is not Pin.
- QueenHit is not RemoveGuard.
- QueenHit is not Material.
- QueenHit is not Tempo.

QueenHit-9 still closed:

- wins queen
- traps queen
- queen is lost
- tempo
- initiative
- pressure
- material gain
- best / only / forced
- decisive / winning
- public route `200`
- production API
- public/user-facing LLM narration

QueenHit-9 hard cleanup rules:

- `QueenHitProof` remains diagnostic proof evidence until consumed by
  `TacticQueenHit`; it is not a Story label, writer, speech key, renderer input,
  LLM input, public payload, or production API payload.
- `Tactic.QueenHit` owns only the Story identity for a legal move that leaves
  the moving side attacking the rival queen on the exact after-board.
- `TacticQueenHit` owns only construction of clean proof-backed
  `Tactic.QueenHit` rows and must not write Hanging, Fork, Skewer, Pin,
  RemoveGuard, Material, Tempo, or any public queen-win claim.
- `attacks_queen` owns only bounded speech that the queen is attacked after the
  legal move. It does not imply winning, trapping, losing the queen, tempo,
  initiative, pressure, material gain, best move, only move, forced move,
  decisive, or winning.
- `StoryTable`, `ExplanationPlan`, `DeterministicRenderer`, and LLM smoke may
  pass only the selected bounded `attacks_queen` line. They must not create,
  repair, broaden, or merge QueenHit with sibling meanings.
- Public route `200`, production API behavior, and public/user-facing LLM
  narration remain closed.

QueenHit-9 required verification:

- targeted QueenHit stage and closeout tests
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Completion standard: QueenHit-9 closes when QueenHit has exactly one proof home (`QueenHitProof`), one Story label (`Tactic.QueenHit`), one writer (`TacticQueenHit`), and one speech key (`attacks_queen`); those owners remain separated; QueenHit is not Hanging, Fork, Skewer, Pin, RemoveGuard, Material, or Tempo; wins-queen, queen-trap, queen-lost, tempo, initiative, pressure, material-gain, best/only/forced, decisive/winning, public route `200`, production API, and public/user-facing LLM narration remain closed; targeted QueenHit stage and closeout tests pass; `ChessFoundationTest` passes; `ChessDocsAuthorityTest` passes; and `git diff --check` passes.

## Loose Piece Slice

### Loose-0 Charter

Loose-0 opens only the charter for the narrow `Tactic.Loose` slice.

Loose-0 meaning: after a legal move and exact after-board replay, the moving side attacks one undefended rival non-king piece.

Loose-0 proof home: `LoosePieceProof`.

Loose-0 writer: `TacticLoose`.

Loose-0 Story label: `Tactic.Loose`.

Loose-0 speech key: `attacks_loose_piece`.

Loose-0 player wording: "This move attacks an undefended piece."

Loose-0 first positive scope:

- legal move
- same-board legal replay
- exact after-board
- one rival non-king piece is identified
- moving side attacks that piece on the after-board
- rival side has no legal defender of that target on the after-board
- attack is bound to the legal route and after-board

Loose-0 must not open:

- Hanging
- Material
- wins piece
- wins material
- free piece
- en prise
- underdefended
- overloaded defender
- pressure
- initiative
- tempo
- best move
- only move
- forced move
- decisive
- winning
- public route `200`
- production API
- public/user-facing LLM narration

Loose-0 boundaries:

- `LoosePieceProof` owns only proof for the exact after-board loose-piece
  attack.
- `TacticLoose` owns only construction of clean proof-backed `Tactic.Loose`
  rows after the writer slice opens.
- `attacks_loose_piece` owns only bounded speech that the legal move attacks an
  undefended rival non-king piece.
- `PieceContact`, attack rows, guard rows, legal moves, SAN, EngineCheck,
  source rows, proofFailures, renderer text, and LLM text cannot create,
  repair, rank, or strengthen Loose.
- Loose is not Hanging, Material, underdefended, pressure, initiative, tempo,
  or a material-win claim.
- public route `200`, production API, and public/user-facing LLM narration
  remain closed.

Loose-0 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
- `AGENTS.md` remains unchanged

Completion standard: Loose-0 keeps loose-piece work at charter scope with exactly one planned proof home (`LoosePieceProof`), one planned Story label (`Tactic.Loose`), one planned writer (`TacticLoose`), and one planned speech key (`attacks_loose_piece`); the first positive scope stays limited to a legal same-board move that leaves the moving side attacking one undefended rival non-king piece on the exact after-board; Hanging, Material, wins-piece, wins-material, free-piece, en-prise, underdefended, overloaded-defender, pressure, initiative, tempo, best/only/forced, decisive/winning, public route `200`, production API, and public/user-facing LLM narration remain closed; detailed authority stays only in `StoryInteractionLaw.md`; AGENTS.md remains unchanged; docs authority tests pass when touched; and `git diff --check` passes.

### Loose-1 Proof Home

Loose-1 opens only:

- proof home: `LoosePieceProof`

LoosePieceProof proves:

- attacking side
- rival side
- legal move identity
- origin square
- destination square
- target piece identity
- target piece square after the move
- target piece is rival-owned
- target piece is not king
- attacking piece square after the move
- moving side attacks target on exact after-board
- rival side has zero legal defenders of the target square on exact after-board
- exact after-board replay
- same-board proof
- loose attack was produced or revealed by this legal move

Loose-1 must not open:

- public Story
- `Tactic.Loose`
- `TacticLoose`
- `attacks_loose_piece`
- capture proof
- material proof
- Hanging proof
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke

Loose-1 rules:

- `LoosePieceProof` is not public Story.
- BoardFacts may observe pieces, attacks, and defenders, but does not create Loose.
- Complete `LoosePieceProof` or silence.
- `LoosePieceProof.publicClaimAllowed` remains false.
- `LoosePieceProof` must not attach to `Story`.
- `LoosePieceProof` must not create or require `StoryProof`.
- `EngineCheck` does not consume `LoosePieceProof` in Stage-1.
- Downstream layers must not mention `LoosePieceProof` or produce Loose text.

Loose-1 negative corpus must keep silent:

- illegal move
- missing same-board proof
- missing exact after-board replay
- missing target piece identity
- target piece belongs to moving side
- target piece is king
- target piece is not attacked on the exact after-board
- rival side has a legal defender of the target square on the exact after-board
- loose attack already existed before the move and was not produced or revealed
  by the legal move
- BoardFacts attack or guard observation without complete `LoosePieceProof`
- any runtime `TacticLoose`, `attacks_loose_piece`, Loose ExplanationPlan,
  renderer, LLM smoke, capture-proof, material-proof, or Hanging-proof surface

Loose-1 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Loose-1 closes when `LoosePieceProof` proves only the complete legal same-board after-board loose-piece attack evidence listed above; BoardFacts remains observation only; no public Story, `Tactic.Loose`, `TacticLoose`, `attacks_loose_piece`, capture proof, material proof, Hanging proof, StoryTable Lead, ExplanationPlan, Renderer, or LLM smoke path opens; targeted runtime tests pass; docs authority tests pass when touched; and `git diff --check` passes.

### Loose-2 Writer

Loose-2 opens only:

- named writer: `TacticLoose`
- Story label: `Tactic.Loose`

Loose-2 writer may create a Story only when:

- complete `StoryProof`
- complete `LoosePieceProof`
- same-board legal replay
- legal route matches proof route
- target is one rival non-king piece
- target is attacked by moving side after the move
- target has no legal rival defender after the move
- writer = `TacticLoose`
- EngineCheck does not Refute

Loose-2 Story identity:

- scene = None, encoded as `Scene.Tactic` plus `Tactic.Loose`
- tactic = `Loose`
- plan = None
- side = attacking side
- rival = target owner
- target = loose piece square after the move
- anchor = attacking piece square after the move
- route = legal loose-piece-attacking move

Loose-2 must not open:

- `Tactic.Hanging`
- `Scene.Material`
- `Tactic.QueenHit`
- `Tactic.Fork`
- `Tactic.Skewer`
- `Tactic.RemoveGuard`
- captures piece
- wins piece
- wins material
- pressure
- tempo
- `attacks_loose_piece`
- StoryTable Lead
- ExplanationPlan
- Renderer
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Loose-2 boundary:

- `TacticLoose` may attach only `LoosePieceProof`; sibling proof homes must stay
  empty.
- `LoosePieceProof` remains the proof home; `TacticLoose` owns only Story
  construction.
- BoardFacts observations cannot create Loose without `LoosePieceProof`.
- EngineCheck may only block a Loose Story by Refutes; it must not create
  Loose, explain Loose, or strengthen Loose.
- StoryTable may keep the Stage-2 row as non-speaking Context, but Stage-2
  opens no Lead speech path.
- ExplanationPlan, renderer, and LLM smoke must produce no Loose wording.

Loose-2 negative corpus must stay silent or blocked for:

- incomplete `StoryProof`
- incomplete `LoosePieceProof`
- illegal move
- route mismatch between Story and proof
- missing same-board legal replay
- target not rival-owned
- target is king
- target not attacked by moving side after the move
- target has a legal rival defender after the move
- EngineCheck Refutes
- writerless `Tactic.Loose`
- Loose proof attached to non-Loose row
- Loose writer copied onto Hanging, Material, QueenHit, Fork, Skewer, or
  RemoveGuard identity
- captures-piece, wins-piece, wins-material, pressure, tempo, or downstream
  wording

Loose-2 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Loose-2 closes when `TacticLoose` creates only `Scene.Tactic` plus `Tactic.Loose` rows from complete `StoryProof` and complete `LoosePieceProof`, with side/rival/target/anchor/route bound to the exact after-board loose-piece attack; EngineCheck Refutes blocks the row; Hanging, Material, QueenHit, Fork, Skewer, and RemoveGuard stay closed; captures-piece, wins-piece, wins-material, pressure, tempo, `attacks_loose_piece`, StoryTable Lead, ExplanationPlan, renderer, LLM smoke, public route `200`, production API, and public/user-facing LLM narration remain closed; targeted runtime tests pass; docs authority tests pass when touched; and `git diff --check` passes.

### Loose-3 Negative Corpus

Loose-3 opens only:

- negative corpus for Loose false positives

Loose-3 must stay silent for:

- illegal move
- missing same-board proof
- missing exact after-board replay
- missing target piece
- target is king
- target belongs to moving side
- target is not attacked after the move
- target was attacked only on before-board
- attack line is blocked on after-board
- target has at least one legal rival defender
- defender map is missing or incomplete
- only raw notation says the piece is loose
- BoardFacts-only loose-looking row without `LoosePieceProof`
- EngineCheck-only pressure
- source row text saying loose piece
- incomplete StoryProof
- incomplete `LoosePieceProof`
- writerless row
- contaminated sidecar row

Loose-3 forbidden wording:

- hanging
- wins piece
- wins material
- free piece
- en prise
- underdefended
- overloaded
- pressure
- initiative
- tempo
- best / only / forced
- decisive / winning

Loose-3 rules:

- A loose piece is not a material win.
- A loose piece is not Hanging.
- A loose piece is not underdefended; first scope is zero legal defenders only.
- `TacticLoose` and StoryTable must treat defender evidence as complete only
  when the proof both says zero rival legal defenders and carries an empty
  rival-defender vector.
- Raw SAN, source text, EngineCheck evidence, and BoardFacts observations cannot
  create or repair `Tactic.Loose`.
- Blocked or contaminated Loose rows must not produce an allowed
  `ExplanationClaim`, rendered line, LLM prompt, public route `200`, production
  API result, or user-facing narration.

Loose-3 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Loose-3 closes when targeted runtime tests prove all listed false positives stay silent or blocked, forged or incomplete defender evidence cannot pass the Loose writer shape, raw notation/source/EngineCheck/BoardFacts-only rows cannot create Loose, incomplete StoryProof and incomplete `LoosePieceProof` cannot speak, writerless and contaminated sidecar rows cannot speak, forbidden wording remains unopened, Loose remains not Material, not Hanging, and not underdefended, detailed authority stays only in `StoryInteractionLaw.md`, targeted docs authority tests pass, and `git diff --check` passes.

### Loose-4 EngineCheck Reuse

Loose-4 opens only:

- reuse existing EngineCheck boundary only

Loose-4 rules:

- EngineCheck cannot create `Tactic.Loose`.
- Supports creates no new claim.
- Caps suppresses standalone speech or keeps selected speech bounded to `attacks_loose_piece`.
- Refutes makes Loose Blocked.
- Unknown creates no engine expression.
- Engine evidence must bind to the same Story route and legal line.
- EngineCheck may attach only to an already proof-backed `Tactic.Loose` row
  whose `StoryProof` and `LoosePieceProof` are complete and whose defender
  evidence proves zero rival legal defenders.
- EngineCheck cannot repair incomplete `LoosePieceProof`, incomplete
  `StoryProof`, writerless rows, sibling tactic rows, contaminated sidecars, or
  BoardFacts-only loose-looking rows.

Loose-4 must not open:

- engine says
- eval number
- raw PV
- best move
- only move
- forced
- wins piece
- wins material
- pressure
- initiative
- public wording from proofFailures

Loose-4 downstream boundary:

- EngineCheck status is internal supporting, capping, unknown, or refuting
  evidence for an existing Loose Story only.
- Supports, Caps, and Unknown must not create an allowed claim key, rendered
  line, LLM prompt, public route payload, production API response, or
  user-facing narration.
- Refutes blocks the Loose row and must not expose proofFailures, eval numbers,
  raw PV, or engine text publicly.
- If a separately admitted stage opens selected Loose speech, capped speech must stay no
  stronger than `attacks_loose_piece`.

Loose-4 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Loose-4 closes when EngineCheck may support, cap, or refute only an already proof-backed `Tactic.Loose`; EngineCheck cannot create Loose, cannot bind incomplete or contaminated Loose rows, cannot bind inconsistent defender evidence, and cannot attach wrong-route or non-legal-line evidence; Supports creates no new claim; Caps creates no standalone speech or remains bounded to `attacks_loose_piece` if selected speech is admitted by a separate stage; Refutes makes Loose Blocked; Unknown creates no engine expression; engine says, eval numbers, raw PV, best/only/forced, wins-piece, wins-material, pressure, initiative, and public proofFailures wording remain closed; targeted runtime tests pass; docs authority tests pass; and `git diff --check` passes.

### Loose-5 StoryTable Integration

Loose-5 opens only:

- StoryTable ordering for existing proof-backed `Tactic.Loose`

Loose-5 collision targets:

- `Scene.Material`
- `Tactic.Hanging`
- `Tactic.QueenHit`
- `Tactic.Fork`
- `Tactic.Skewer`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.DiscoveredAttack`
- `Scene.Defense`

Loose-5 rules:

- StoryTable may order Loose, not create it.
- Material capture result stays in `Scene.Material`.
- Hanging owns capture/material-loss wording when complete.
- QueenHit owns queen-specific attack wording.
- Fork owns two-target proof.
- Skewer owns front/rear slider proof.
- Pin owns pinned-to-king proof.
- RemoveGuard owns defender-removal proof.
- Loose owns only `attacks_loose_piece`: this move attacks an undefended non-king piece.
- Loose must not outrank a complete collision-target row by using loose-piece
  identity as a material, hanging, queen-specific, multi-target, line,
  pinned-to-king, defender-removal, discovered-attack, or defense claim.
- StoryTable must continue to block incomplete, writerless, refuted, and
  contaminated Loose rows.

Rows that must not speak:

- Support
- Context
- Blocked
- capped
- refuted
- non-Lead

Loose-5 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Loose-5 closes when StoryTable can select standalone complete proof-backed `Tactic.Loose` as Lead, keeps complete collision-target rows in their existing proof homes, never creates Loose, never lets Loose borrow material, hanging, queen-specific, two-target, skewer, pin, remove-guard, discovered-attack, or defense wording, and Support, Context, Blocked, capped, refuted, and non-Lead Loose rows produce no allowed claim, rendered line, LLM prompt, public route payload, production API response, or user-facing narration; targeted runtime tests pass; docs authority tests pass; and `git diff --check` passes.

### Loose-6 ExplanationPlan

Loose-6 opens only:

- ExplanationPlan for selected uncapped Lead `Tactic.Loose` only

Loose-6 allowed claim key:

- `attacks_loose_piece`

Loose-6 input boundary:

- selected Lead Verdict
- uncapped
- not refuted
- Story tactic = Loose
- writer = `TacticLoose`
- complete StoryProof
- complete `LoosePieceProof`
- target = loose piece square
- anchor = attacking piece square
- route = legal loose-piece-attacking move
- evidenceLine = route only

Loose-6 forbidden claim keys:

- hanging_piece
- wins_piece
- wins_material
- attacks_queen
- removes_defender
- gains_tempo
- creates_pressure
- best_move
- only_move
- forced_move
- decisive
- winning

Loose-6 rules:

- ExplanationPlan does not prove Loose.
- ExplanationPlan does not repair missing proof.
- Support/Context/Blocked/capped/refuted rows stay silent.
- A Loose-shaped row with a contaminated scene, tactic, writer, sidecar, target,
  anchor, route, StoryProof, or `LoosePieceProof` must not fall through into a
  sibling ExplanationPlan.
- `evidenceLine` is exactly the legal loose-piece-attacking route and no
  alternate engine, source, or notation line.

Loose-6 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Loose-6 closes when only selected uncapped Lead `Tactic.Loose` rows from `TacticLoose` with complete StoryProof and complete `LoosePieceProof` lower to one bounded `attacks_loose_piece` claim; target, anchor, route, and evidenceLine bind exactly to the loose proof; ExplanationPlan cannot prove Loose or repair missing proof; Support, Context, Blocked, capped, refuted, non-Lead, writerless, incomplete, and contaminated Loose-shaped rows stay silent; forbidden Loose claim keys remain closed; targeted runtime tests pass; docs authority tests pass; and `git diff --check` passes.

### Loose-7 DeterministicRenderer

Loose-7 opens only:

- renderer text from bounded Loose ExplanationPlan only

Loose-7 renderer input:

- ExplanationPlan only

Loose-7 allowed template:

- `{route} attacks the undefended piece on {target}.`

Loose-7 forbidden renderer input:

- raw Story
- raw `LoosePieceProof`
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- source rows

Loose-7 forbidden wording:

- wins the piece
- wins material
- hanging piece
- free piece
- en prise
- underdefended
- overload
- pressure
- initiative
- tempo
- best move
- only move
- forced
- decisive
- winning
- engine says

Loose-7 rules:

- Renderer emits no text for Support/Context/Blocked/capped/refuted rows.
- Renderer emits no text for sibling claim keys.
- DeterministicRenderer does not prove Loose, inspect raw proof sidecars, read
  BoardFacts, consume EngineCheck, or repair a missing ExplanationPlan.
- The rendered Loose line is bounded to the selected route SAN and target square
  already carried by `attacks_loose_piece`.

Loose-7 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Loose-7 closes when DeterministicRenderer accepts only a bounded selected Lead Loose ExplanationPlan with `attacks_loose_piece`, emits exactly `{route} attacks the undefended piece on {target}.`, accepts no raw Story, `LoosePieceProof`, BoardFacts, EngineCheck, raw PV, proofFailures, or source-row input, emits no text for Support, Context, Blocked, capped, refuted, non-Lead, malformed, or sibling-claim plans, avoids all forbidden Loose wording, targeted runtime tests pass, docs authority tests pass, and `git diff --check` passes.

### Loose-8 LLM Smoke

Loose-8 opens only:

- LLM smoke only for bounded Loose RenderedLine

Loose-8 allowed LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Loose-8 allowed claimKey:

- `attacks_loose_piece`

Loose-8 LLM must not add:

- wins piece
- wins material
- hanging
- free piece
- en prise
- underdefended
- overloaded
- pressure
- initiative
- tempo
- best move
- only move
- forced move
- decisive
- winning
- engine line
- new move
- new variation

Loose-8 rules:

- LLM only polishes.
- Verifier rejects overclaim.
- public/user-facing LLM narration remains closed.
- LLM smoke must not read raw Story, raw `LoosePieceProof`, BoardFacts,
  EngineCheck, raw PV, proofFailures, or source rows.
- LLM smoke may echo or rephrase only the already rendered bounded
  `attacks_loose_piece` line.

Loose-8 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Loose-8 closes when LLM smoke accepts only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for a bounded Loose `RenderedLine` with `attacks_loose_piece`; rejects raw Story, raw `LoosePieceProof`, BoardFacts, EngineCheck, raw PV, proofFailures, source rows, Support, Context, Blocked, mismatched, capped, and refuted inputs; rejects wins-piece, wins-material, hanging, free-piece, en-prise, underdefended, overloaded, pressure, initiative, tempo, best/only/forced, decisive, winning, engine-line, new-move, and new-variation additions; keeps public/user-facing LLM narration closed; targeted runtime tests pass; docs authority tests pass; and `git diff --check` passes.

### Loose-9 Closeout / Hard Cleanup

Loose-9 opens no new chess meaning.

Loose-9 closes only narrow `Tactic.Loose`.

Loose-9 authority audit:

- `LoosePieceProof` owns proof home.
- `Tactic.Loose` owns Story label.
- `TacticLoose` owns writer.
- `attacks_loose_piece` owns speech key.
- These are not interchangeable.

Loose-9 duplication audit:

- Loose is not Hanging.
- Loose is not Material.
- Loose is not QueenHit.
- Loose is not Fork.
- Loose is not Skewer.
- Loose is not Pin.
- Loose is not RemoveGuard.
- Loose is not Tempo.
- Loose is not Pressure.

Loose-9 still closed:

- hanging
- wins piece
- wins material
- free piece
- en prise
- underdefended
- overloaded defender
- pressure
- initiative
- tempo
- best / only / forced
- decisive / winning
- public route `200`
- production API
- public/user-facing LLM narration

Loose-9 hard cleanup rules:

- BoardFacts remains observation only and does not create Loose.
- `LoosePieceProof` remains a proof sidecar, not a public Story.
- `TacticLoose` remains the only writer for `Tactic.Loose`.
- StoryTable may order existing proof-backed Loose rows but must not create
  them or borrow sibling proof homes.
- ExplanationPlan, DeterministicRenderer, and LLM smoke remain downstream and
  bounded to `attacks_loose_piece`.
- Public route `200`, production API, and public/user-facing LLM narration stay
  closed.

Loose-9 required verification:

- targeted Loose stage and closeout tests
- `sbt "commentary/testOnly lila.commentary.chess.ChessFoundationTest"`
- `sbt "commentary/testOnly lila.commentary.docs.ChessDocsAuthorityTest"`
- `git diff --check`

Loose-9 documentation rule:

- detailed Loose stage law lives only in `StoryInteractionLaw.md`
- README, SSOT, Architecture, Contract, and Manifest may contain summary only
  if docs tests require it
- `AGENTS.md` remains unchanged

Completion standard: Loose-9 closes when Loose has exactly one proof home (`LoosePieceProof`), one Story label (`Tactic.Loose`), one writer (`TacticLoose`), and one speech key (`attacks_loose_piece`); those owners remain separated; Loose is not Hanging, Material, QueenHit, Fork, Skewer, Pin, RemoveGuard, Tempo, or Pressure; hanging, wins-piece, wins-material, free-piece, en-prise, underdefended, overloaded-defender, pressure, initiative, tempo, best/only/forced, decisive/winning, public route `200`, production API, and public/user-facing LLM narration remain closed; targeted Loose stage and closeout tests pass; `ChessFoundationTest` passes; `ChessDocsAuthorityTest` passes; and `git diff --check` passes.

## Proof And Interaction Law

| proof field | live class | raises | caps or blocks |
|---|---|---|---|
| boardProof | board truth | already-opened named Story rows only | zero blocks board-backed speech. |
| lineProof | line truth | already-opened line-backed rows only | zero blocks tactic and line-backed stories. |
| ownerProof | side truth | lead eligibility for opened writers only | high ownerProof with no side blocks lead. |
| anchorProof | anchor truth | opened structural rows only | high anchorProof with no anchor blocks lead. |
| routeProof | route truth | opened route-bearing rows only | high routeProof with no route blocks lead. |
| persistence | long pressure | internal planHeat only | immediate proof-backed tactic override can cap it. |
| immediacy | near-term force | internal tacticHeat only | no legal line caps it to non-speaking context. |
| forcing | move pressure | internal tacticHeat only | missing reply map blocks forcing wording. |
| conversionPrize | gain size | internal heat only | no gain identity blocks conversion wording. |
| counterplayRisk | rival resource | caps opened rows through public floor | above 70 currently blocks all Story lead eligibility. |
| kingHeat | king attack support | internal tacticHeat only | no legal line blocks mate or attack claim. |
| pieceSupport | piece support | internal heat only | no named piece blocks piece wording. |
| pawnSupport | pawn support | internal heat only | no named pawn blocks pawn wording. |
| sourceFit | source context | internal source/opening context only | source cannot replace board proof. |
| novelty | source context | internal novelty only | no source row blocks novelty wording. |
| clarity | explanation clarity | internal heat only | missing identity caps clarity to non-speaking context. |

Proof scores and heat fields never create a public Story. They may only rank,
cap, or diagnose an already-opened named writer row with proof-bearing identity.

## Nonlinear Rules

| rule | effect |
|---|---|
| Hard proof blocker | Missing side, target, anchor, route, rival, required legal line, or same-root proof sidecar is a hard public-output block. |
| Tactical override | An opposing proof-backed Tactic or Blunder at public floor blocks Plan lead only for an explicitly opened Plan writer. |
| Same-side tactic priority | A same-side proof-backed Tactic with tacticHeat at least 70 and lineProof at least 65 outranks only an explicitly opened Plan writer. |
| Source cap | Source and Opening context cannot become public Lead unless an explicit opened proof path permits that exact context row; they never outrank board-backed proof. |
| Engine cap | Engine context can cap or confirm wording only after Story identity and legal line are bound. |
| Board-only cap | BoardMood and BoardFacts without Story proof remain observation or diagnostics only and allow no public wording, advice, Verdict, or claim. |
| Blunder override | A proven blunder or losing tactic suppresses strategic praise about the same side. |
| Counterplay cap | Counterplay risk above 70 currently blocks public lead eligibility; conversion or plan escape requires an explicit proof-backed answered-resource slice. |
| Quiet fallback | Quiet remains non-speaking unless an explicit Quiet writer and proof path opens; absence of stronger Stories alone does not create public speech. |
| Render cap | Renderer may phrase only an ExplanationPlan lowered from a selected Verdict and cannot read raw Verdict, Story, proof, diagnostics, or route data directly. |

## Proof Interaction Law Reconciliation

PILFR reconciles this shared interaction law with the current proof-first
runtime. It opens no Story family and no runtime behavior.

PILFR keeps closed:

- `Scene.Plan` public speech
- `Scene.Source` public speech
- `Scene.Opening` public speech
- `Scene.Quiet` public speech
- broad mate-net speech
- conversion, compensation, pressure, initiative, best, only, forced, winning,
  decisive, and no-counterplay claims
- public route `200`
- production API
- public/user-facing LLM narration

PILFR proof authority:

- named writers remain the only positive Story admission path.
- proof fields may support, cap, order, or diagnose only already-opened named
  writer rows.
- source, opening, quiet, and plan-shaped rows remain non-speaking unless a live
  authority document opens a narrow proof-backed writer for the exact meaning.
- tactical override, source cap, quiet fallback, and counterplay cap are
  fail-closed shared gates for explicitly opened writers, not public claim owners.
- renderer input remains `ExplanationPlan` only.
- diagnostics remain internal only.

PILFR verification duties:

- a quiet row with complete identity but no opened writer stays non-speaking.
- a strategic plan row with no route stays blocked and produces no plan text.
- source/opening-shaped context cannot lead over board-backed proof and produces
  no source/opening text.
- high counterplay risk blocks standalone material or conversion speech.
- legal-escape king pressure may produce only already-opened bounded check
  wording, not mate-net, no-escape, or no-counterplay wording.

Completion standard: PILFR closes when `Proof And Interaction Law`,
`Nonlinear Rules`, `Proof-Deficit Logs`, and `Fixture Duties` no longer imply
that proof scores, source/opening context, BoardFacts, Quiet fallback, raw
Plan rows, or renderer direct Verdict access can create public speech; targeted
runtime reconciliation tests pass; docs authority tests pass; public route
`200`, production API, and public/user-facing LLM narration remain closed; and
`git diff --check` passes.

## Stage-0 Proof-Deficit Diagnostics Charter

Stage-0 opens internal-only proof-deficit diagnostics for blocked or non-lead
Story rows.

Stage-0 opens:

- internal diagnostic production for tests, debugging, and developer inspection
- one generic diagnostic shape for missing proof coordinates
- diagnostics over existing Story identity, StoryProof failures, existing proof
  sidecars, same-board legal replay status, EngineCheck status, and StoryTable
  row role

Stage-0 keeps closed:

- no new Story label
- no new proof home for chess meaning
- no new writer
- no new speech key
- no ExplanationPlan claim
- no renderer text
- no LLM input
- no public route `200`
- no production API
- no public/user-facing narration

Stage-0 diagnostic rules:

- diagnostics may explain absent proof coordinates only
- diagnostics must not repair missing proof
- diagnostics must not decide chess meaning
- diagnostics must not upgrade Support, Context, Blocked, capped, or refuted rows
- diagnostics must not enter `Verdict.values`
- diagnostics must not enter `ExplanationPlan`, renderer, LLM smoke, or
  commentary routes
- detailed diagnostics authority lives only in `StoryInteractionLaw.md`

Stage-0 forbidden diagnostic wording:

- engine says
- should speak
- safe to claim
- proves the tactic
- best move
- only move
- forced
- winning
- decisive
- public explanation
- narration-ready

Completion standard: Stage-0 Proof-Deficit Diagnostics closes when blocked or
non-lead Story rows can produce internal generic diagnostics for missing proof
coordinates, diagnostics do not create or upgrade chess meaning, diagnostics
stay absent from `Verdict.values`, `ExplanationPlan`, deterministic renderer,
LLM smoke, `/api/commentary/render`, and
`/internal/commentary/render-local-probe`, detailed diagnostics authority
appears only in `StoryInteractionLaw.md`, targeted runtime tests pass, docs
authority tests pass, and `git diff --check` passes.

## Stage-1 Generic Proof-Deficit Diagnostic Shape

Stage-1 opens one generic internal diagnostic data shape for proof deficits.

Stage-1 required generic fields:

- `storyIdentityLabel`
- `leadAllowed`
- `roleReason`
- `blockedBy`
- `boardFactsPresent`
- `proofCoordinates.root`
- `proofCoordinates.side`
- `proofCoordinates.rival`
- `proofCoordinates.target`
- `proofCoordinates.anchor`
- `proofCoordinates.route`
- `proofCoordinates.requiredLegalLine`
- `proofCoordinates.sameRootProofSidecar`
- `missingSidecar`
- `reason`

Blocked and non-lead diagnostics report `leadAllowed=false`. Capped Lead rows
may report `leadAllowed=true` only as the existing Verdict row state; their
diagnostic reason must be the strength limit, not a permission to speak.

Stage-1 keeps closed:

- no family-specific diagnostic type
- no Overload-specific, Pawn-specific, Line-specific, King-specific, or
  Material-specific shortcut
- no public JSON schema
- no renderer or LLM contract field
- no change to `Verdict.values` public-safe shape

Stage-1 proof authority:

- the shape reports only what existing upstream proof already knows
- missing coordinates remain missing
- existing proof homes remain the only authority for their chess meaning

Stage-1 forbidden diagnostic wording:

- diagnostic proves
- diagnostic creates
- diagnostic allows claim
- diagnostic selects Lead
- diagnostic explains to the user

Completion standard: Stage-1 Generic Proof-Deficit Diagnostic Shape closes when unrelated blocked Story families receive the same generic internal diagnostic shape, diagnostics fields remain absent from `Verdict.values`, no public route payload contains diagnostics, no family-specific diagnostic type or shortcut is introduced, detailed diagnostics authority appears only in `StoryInteractionLaw.md`, targeted runtime tests pass, docs authority tests pass, and `git diff --check` passes.

## Stage-2 Proof-Deficit Coordinate Extraction Rules

Stage-2 opens generic extraction of known and missing Story coordinates.

Stage-2 coordinate extraction may read existing Story identity only:

- scene / tactic / plan
- side
- rival
- target
- anchor
- route

Stage-2 may report existing proof sidecar presence only.

Stage-2 keeps closed:

- no inference of missing side, rival, target, anchor, or route from raw BoardFacts alone
- no inference from raw engine PV
- no inference from source rows
- no legal replay invented by diagnostics
- no family-specific repair path

Stage-2 proof authority:

- Story identity remains authoritative for public claim coordinates
- StoryProof remains authoritative for complete identity binding
- proof sidecars remain authoritative for family-specific proof completion
- diagnostics only mirrors present/missing state

Stage-2 forbidden diagnostic wording:

- likely target
- implied route
- probably same board
- engine confirms identity
- BoardFacts proves claim
- source proves claim

Completion standard: Stage-2 Proof-Deficit Coordinate Extraction Rules closes when side, rival, target, anchor, route, legal-line, and sidecar deficits each appear only as diagnostics, diagnostics do not fill missing coordinates from BoardFacts, engine PV, source rows, legal replay invention, or family repair paths, `Verdict.values` stays unchanged, public route payloads contain no diagnostics, detailed diagnostics authority appears only in `StoryInteractionLaw.md`, targeted runtime tests pass, docs authority tests pass, and `git diff --check` passes.

## Stage-3 StoryTable Diagnostic Attachment

Stage-3 opens internal attachment of proof-deficit diagnostics to
StoryTable-produced non-speaking rows.

Stage-3 diagnostic attachment may be available for:

- `Role.Blocked` rows
- `leadAllowed=false` rows
- `EngineCheck.Refutes` rows
- capped rows when speech is blocked or strength-limited
- Support and Context rows when they do not produce standalone text

Stage-3 keeps closed:

- StoryTable still orders only
- StoryTable does not create Stories
- StoryTable does not create proof
- StoryTable does not produce public explanation text
- Support, Context, Blocked, capped, and refuted rows still produce no
  standalone public text

Stage-3 proof authority:

- Verdict decides selected row status
- diagnostics explains why a row cannot speak, but cannot change the Verdict
- EngineCheck may support, cap, or refute only an existing Story and cannot
  become diagnostic proof authority

Stage-3 forbidden diagnostic wording:

- row should have led
- row nearly proves
- refuted but useful claim
- capped public wording
- support text
- context text

Completion standard: Stage-3 StoryTable Diagnostic Attachment closes when selected uncapped Lead behavior is unchanged, Support, Context, Blocked, capped, and refuted StoryTable rows can expose internal diagnostics while still producing no standalone public text unless an existing internal ExplanationPlan rule already permits a no-claim plan, StoryTable ordering is unchanged by diagnostics, diagnostics do not change `Verdict.values`, StoryTable still creates no Story, proof, or public explanation text, detailed diagnostics authority appears only in `StoryInteractionLaw.md`, targeted runtime tests pass, docs authority tests pass, and `git diff --check` passes.

## Stage-4 Proof-Deficit Downstream Boundary Guard

Stage-4 opens explicit guards proving diagnostics stop before expression
layers.

Stage-4 keeps closed:

- ExplanationPlan cannot consume diagnostics
- DeterministicRenderer cannot consume diagnostics
- LLM smoke cannot consume diagnostics
- public routes cannot expose diagnostics
- production API remains closed
- public/user-facing LLM narration remains closed

Stage-4 proof authority:

- ExplanationPlan public expression still accepts selected uncapped Lead Verdict
  data only
- renderer input remains ExplanationPlan only
- LLM smoke input remains renderedText, claimKey, strength, forbidden wording
  only
- LLM instruction remains: `Rephrase only. Do not add chess facts.`

Stage-4 forbidden diagnostic wording:

- use diagnostic reason in text
- explain why blocked
- tell user missing proof
- engine/proof failure wording
- raw PV wording
- source-row wording

Completion standard: Stage-4 Proof-Deficit Downstream Boundary Guard closes when diagnostic strings do not appear in rendered text, LLM smoke rejects diagnostic fields and diagnostic wording, public route tombstones still return unavailable/no-commentary rather than `200`, `Verdict.values` shape is unchanged, ExplanationPlan, DeterministicRenderer, and LLM smoke expose no diagnostic input path, detailed diagnostics authority appears only in `StoryInteractionLaw.md`, targeted runtime tests pass, docs authority tests pass, and `git diff --check` passes.

## Stage-5 Proof-Deficit Cross-Family Fixture Coverage

Stage-5 opens generic proof-deficit fixtures across already-open families.

The Stage-5 fixture set must include unrelated families so diagnostics are not
accidentally family-specific.

Stage-5 required fixture families:

- one tactic row with missing proof sidecar
- one pawn/file row with missing same-board or route proof
- one king/check row with missing legal replay or king coordinate
- one source/opening or quiet row blocked by board-backed proof rules
- one EngineCheck Refutes row
- one Support or Context row that must not speak

Stage-5 keeps closed:

- no new positive Story
- no new family admission
- no broad tactic, plan, strategy, conversion, pressure, initiative, best,
  only, forced, winning, or decisive claim
- no public/user-facing diagnostics

Stage-5 proof authority:

- every fixture uses existing proof homes and existing Story labels only
- diagnostics must report deficits from existing proof boundaries, not define
  new chess meaning

Stage-5 forbidden diagnostic wording:

- family-specific shortcut names as public claim owners
- almost Overload
- almost Material
- probably Checkmate
- any blocked-row text that could be user-facing

Completion standard: Stage-5 Proof-Deficit Cross-Family Fixture Coverage closes when targeted runtime diagnostics tests cover the required unrelated fixture families, each fixture uses existing proof homes and Story labels only, no fixture opens a positive Story or new family admission, blocked/refuted/non-speaking rows still do not produce standalone public text, diagnostics remain generic and internal, detailed diagnostics authority appears only in `StoryInteractionLaw.md`, `ChessFoundationTest` passes, docs authority tests pass when docs are touched, and `git diff --check` passes.

## Stage-6 Proof-Deficit Documentation Authority Boundary

Stage-6 opens detailed diagnostics stage law only in `StoryInteractionLaw.md`.

Summary-only mentions may be added to README, SSOT, Architecture, Contract, or Manifest only if docs tests require them.

Stage-6 keeps closed:

- `AGENTS.md` remains unchanged unless durable operator rules change
- no new live authority document
- no legacy doc authority
- no duplicated detailed checklist outside `StoryInteractionLaw.md`

Stage-6 proof authority:

- `StoryInteractionLaw.md` owns diagnostics charter, negative corpus, downstream boundary, and closeout
- summary docs may only say diagnostics are internal blocked-proof reporting

Stage-6 forbidden documentation wording:

- diagnostics as product feature
- diagnostics as public API
- diagnostics as renderer input
- diagnostics as LLM input
- diagnostics as acceptance for incomplete proof

Completion standard: Stage-6 Proof-Deficit Documentation Authority Boundary closes when detailed diagnostics law remains only in `StoryInteractionLaw.md`, summary docs do not duplicate diagnostics checklists, `AGENTS.md` remains unchanged unless durable operator rules change, no new live authority document or legacy authority appears, diagnostics are described only as internal blocked-proof reporting when summaries are required, docs authority tests pass, and `git diff --check` passes.

## Stage-7 Proof-Deficit Diagnostics Closeout Hard Cleanup

Stage-7 opens final closeout for internal proof-deficit diagnostics only.

Stage-7 keeps closed:

- no Story label
- no proof home for chess meaning
- no writer
- no speech key
- no ExplanationPlan claim
- no renderer text
- no LLM smoke expansion
- no public route `200`
- no production API
- no public/user-facing narration

Stage-7 closeout audit:

- one diagnostic shape only
- diagnostics are generic across opened Story families
- diagnostics never repair proof
- diagnostics never create Story
- diagnostics never change StoryTable order
- diagnostics never enter `Verdict.values`
- diagnostics never enter ExplanationPlan, renderer, LLM, or route payload
- detailed authority lives only in `StoryInteractionLaw.md`

Completion standard: Stage-7 Proof-Deficit Diagnostics Closeout Hard Cleanup closes when targeted diagnostics tests pass, `ChessFoundationTest` passes, docs authority tests pass, `git diff --check` passes, no new Story label, proof home, writer, speech key, ExplanationPlan claim, renderer text, LLM smoke expansion, public route `200`, production API, or public/user-facing narration is introduced, one generic diagnostic shape remains the only diagnostics shape, diagnostics stay generic across opened Story families, diagnostics do not repair proof, create Story, change StoryTable order, enter `Verdict.values`, or enter ExplanationPlan, renderer, LLM, or route payload, and detailed authority remains only in `StoryInteractionLaw.md`.

## Proof-Deficit Logs

Every blocked Story must report proof deficit, not only family pass/fail.
Validation output must explain which identity or proof tuple members are absent
and which board observations were present.

This diagnostic shape is internal validation, test, and debugging output only.
It is not API/public JSON, renderer input, LLM input, or user commentary. It
shows why the kernel cannot speak; it does not provide text that may be spoken.

Required blocked-story diagnostic shape:

```json
{
  "storyIdentityLabel": "...",
  "leadAllowed": false,
  "blockedBy": ["..."],
  "boardFactsPresent": ["..."],
  "proofCoordinates": {
    "root": "...",
    "side": "...",
    "target": "...",
    "anchor": "...",
    "route": "...",
    "rival": "...",
    "requiredLegalLine": "...",
    "sameRootProofSidecar": "..."
  },
  "missingSidecar": ["..."],
  "reason": "..."
}
```

Example:

```json
{
  "storyIdentityLabel": "closed Plan.OpenFile fixture row",
  "leadAllowed": false,
  "blockedBy": ["routeProof", "sameRootLine"],
  "boardFactsPresent": ["rook_open_file_entry"],
  "proofCoordinates": {
    "root": "current-position-root",
    "side": "White",
    "target": "open file entry square",
    "anchor": "rook square",
    "route": null,
    "rival": "Black",
    "requiredLegalLine": null,
    "sameRootProofSidecar": null
  },
  "missingSidecar": ["legal file-entry line", "same-root route proof"],
  "reason": "The row has an entry-square observation, but no same-root line proves usable file entry. Closed Plan rows remain diagnostic fixtures only."
}
```

This keeps false positives actionable. The fix should be to supply the missing
proof coordinate or keep the Story blocked, not to add another family-specific
shortcut.

## Fixture Duties

The interaction law must be tested with positions or synthetic Stories that
cover these failure modes:

- Opening context with a tactical refutation: tactic or blunder blocks
  source/opening context and no source/opening public speech is produced.
- Strategic plan with no route: plan support exists but public plan speech is
  blocked.
- Mate-net shape with legal escape: king pressure stays below mate wording and
  may only use already-opened bounded check wording.
- Material gain with compensation line: material Story is capped by
  counterplay/conversion proof.
- Source row matching the opening but board proof contradicting it: source stays
  context.
- Quiet position with no public floor Story: Quiet remains non-speaking unless
  an explicit Quiet writer and proof path opens.
