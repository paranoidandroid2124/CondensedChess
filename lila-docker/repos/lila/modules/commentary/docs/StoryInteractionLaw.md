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
target, anchor, route, or rival. Legal line and same-board proof are evidence
for the Story identity, not a second identity home.

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
line, and same-root proof sidecar. A family row below adds the chess-specific
facts that must support that binding. Missing side, target, anchor, route,
rival, required legal line, or same-root proof sidecar is a hard public-output
block. If a row does not need a move sequence, its required legal line is the
same-root legal replay proving the named position fact can be spoken.

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
renderer, LLM, or public route.

The following remain forbidden: renderer opening, LLM narration, public route
`200`, Board Facts direct public claim, Proof score alone as Lead, StoryProof
alone as Lead, and positive Story families other than `Tactic.Hanging`.

Opening `Tactic.Hanging` does not open `Scene.Material`, `Tactic.Fork`,
`Scene.Defense`, `Scene.Plan`, or any Plan row. Opening `Scene.Material` does
not open `Scene.Blunder`, `Scene.Convert`, winning claims, conversion claims,
or decisive-advantage wording.

Board Facts direct public claim, Proof score alone as Lead, StoryProof alone as
Lead, and positive Story families other than `Tactic.Hanging` are forbidden in
the first Stage 3 scope.

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
ply or a bounded line, same-board proof, and missing evidence. It must not say
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

- selected Verdict only

Forbidden input:

- raw BoardFacts
- BoardMood
- root atoms
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
| Tactic.Skewer | line piece, valuable front target, rear target | legal checking or attack line | front target can escape with tempo |
| Tactic.Xray | `white_xray_line` or `black_xray_line` | legal uncovering or pressure line | screen not movable, target defended |
| Tactic.Fork | attacker, two targets | legal move to fork square | fork square unsafe or targets can respond |
| Tactic.Discover | screened line and moving piece | legal discovered move | moving piece illegal or line target absent |
| Tactic.RemoveGuard | guard-capture move | legal capture of guard and follow-up target | guard not sole defender |
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
| Tactic.InBetween | forcing intermezzo target | legal in-between move | rival can ignore without loss |
| Tactic.Clear | clearance move and line piece | legal clearance route | cleared line has no target |
| Tactic.Decoy | target lure square | legal forcing move to lure | target can decline safely |
| Tactic.Deflect | defender and target | legal deflection move | defender not needed or recaptures |
| Tactic.Tempo | forcing move with gained turn | legal move and rival reply restriction | rival has equal or stronger forcing move |

## Proof And Interaction Law

| proof field | live class | raises | caps or blocks |
|---|---|---|---|
| boardProof | board truth | every non-source Story | zero blocks board-backed speech. |
| lineProof | line truth | tactics, conversion, initiative, trade, race | zero blocks tactic and line-backed stories. |
| ownerProof | side truth | lead eligibility | high ownerProof with no side blocks lead. |
| anchorProof | anchor truth | structural and plan Stories | high anchorProof with no anchor blocks lead. |
| routeProof | route truth | plans, tactics, conversion | high routeProof with no route blocks lead. |
| persistence | long pressure | planHeat | immediate tactic override can cap it. |
| immediacy | near-term force | tacticHeat | no legal line caps it to context. |
| forcing | move pressure | tacticHeat | missing reply map blocks forcing wording. |
| conversionPrize | gain size | tacticHeat and planHeat | no gain identity blocks conversion wording. |
| counterplayRisk | rival resource | caps plan and conversion | above 70 blocks plan lead. |
| kingHeat | king attack support | tacticHeat | no legal line blocks mate or attack claim. |
| pieceSupport | piece support | planHeat | no named piece caps piece wording. |
| pawnSupport | pawn support | planHeat | no named pawn caps pawn wording. |
| sourceFit | source context | source/opening context | source cannot replace board proof. |
| novelty | source context | novelty wording | no source row blocks novelty wording. |
| clarity | explanation clarity | planHeat | missing identity caps clarity to context. |

## Nonlinear Rules

| rule | effect |
|---|---|
| Hard proof blocker | Missing side, target, anchor, route, rival, required legal line, or same-root proof sidecar is a hard public-output block. |
| Tactical override | An opposing Tactic or Blunder at public floor blocks Plan lead. |
| Same-side tactic priority | A same-side Tactic with tacticHeat at least 70 and lineProof at least 65 outranks Plan. |
| Source cap | Source and Opening context cannot lead over a board-backed Story at public floor. |
| Engine cap | Engine context can cap or confirm wording only after Story identity and legal line are bound. |
| Board-only cap | BoardMood facts without Story proof allow observed/possible wording only, not advice or verdict. |
| Blunder override | A proven blunder or losing tactic suppresses strategic praise about the same side. |
| Counterplay cap | Counterplay risk above 70 blocks conversion and plan lead unless the Story proves the rival resource is answered. |
| Quiet fallback | Quiet can lead only when every non-Quiet Story is below public floor. |
| Render cap | Render may only verbalize selected Verdicts and cannot repair missing identity or proof. |

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
  "story": "...",
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
  "story": "Plan.OpenFile",
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
  "reason": "The rook has an entry-square observation, but no same-root line proves usable file entry."
}
```

This keeps false positives actionable. The fix should be to supply the missing
proof coordinate or keep the Story blocked, not to add another family-specific
shortcut.

## Fixture Duties

The interaction law must be tested with positions or synthetic Stories that
cover these failure modes:

- Opening context with a tactical refutation: tactic or blunder overrides
  source/opening speech.
- Strategic plan with no route: plan support exists but public plan speech is
  blocked.
- Mate-net shape with legal escape: king pressure is capped below mate wording.
- Material gain with compensation line: material Story is capped by
  counterplay/conversion proof.
- Source row matching the opening but board proof contradicting it: source stays
  context.
- Quiet position with no public floor Story: Quiet may lead.
