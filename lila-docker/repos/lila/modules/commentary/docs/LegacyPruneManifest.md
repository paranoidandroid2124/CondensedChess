# Legacy Prune Manifest

Prepared on 2026-05-04 for the `BoardMood -> Story -> StoryTable -> Verdict`
reset.

Authority is exactly the live root docs listed in `docs/README.md`:
`docs/ChessCommentarySSOT.md`, `docs/ChessModelArchitecture.md`,
`docs/ChessModelContract.md`, `docs/ChessResetRationale.md`,
`docs/BoardFacts.md`,
`docs/BoardMoodCutLaw.md`, `docs/BoardMoodSplitLaw.md`,
`docs/StoryInteractionLaw.md`, `docs/StoryResurrectionLaw.md`,
`docs/LegacyPruneManifest.md`, and `docs/README.md`.

This branch has already pruned the legacy commentary authority tree. The live
commentary surface under `modules/commentary` is now limited to:

- `src/main/scala/lila/commentary/chess/**`
- `src/main/scala/lila/commentary/root/**`
- `src/test/scala/lila/commentary/chess/**`
- `src/test/scala/lila/commentary/docs/**`
- `docs/*.md`

Pruned legacy authority areas include certification, claim, delta,
candidate-line, projection, render, selection, source-context adapter,
strategic-object, witness, validation, diagnostic test, and legacy
expectation-corpus authority paths.

The old public facade and API transport authority is closed, not replaced.
`/api/commentary/render` and `/internal/commentary/render-local-probe` remain
registered only as fail-closed tombstones. No `200`, rendered payload,
environment switch, frontend mock, or route test can open public commentary.

The remaining live docs are:

- `ChessCommentarySSOT.md`
- `BoardFacts.md`
- `BoardMoodCutLaw.md`
- `BoardMoodSplitLaw.md`
- `ChessModelArchitecture.md`
- `ChessModelContract.md`
- `ChessResetRationale.md`
- `LegacyPruneManifest.md`
- `README.md`
- `StoryInteractionLaw.md`
- `StoryResurrectionLaw.md`

Legacy names such as `CommentaryCoreSSOT.md`, `SemanticModelArchitecture.md`,
`LegacyArchiveIndex.md`, and `CommentaryFrontendBridgeContract.md` must not
return as root docs authority.

## Closed Reset State

The prune does not open a replacement public path. Controllers, facades, API
transports, renderer-facing claim paths, default runtime FEN to public
`Verdict`, broad `BoardMood` Sxxx expansion/re-entry, and `Story` proof writers
stay closed until prerequisite laws and tests exist. Stage 1 `Board Facts`
admits only small current-board observations below public claims.

`BoardMood` does not expand beyond `48` bits, `256` scalars, and `3,328` total
values in this checkpoint. Split/cut re-entry requires a named law and
same-board producer proof.

Existing failures around lower facts, scaffold paths, and renderer non-upgrade
remain evidence for the reset boundary only. They are not acceptance for public
runtime commentary.

Stage 3 charter authority lives in `StoryInteractionLaw.md`. Legacy material
does not open `Tactic.Hanging`, any other positive Story family, renderer, LLM,
or public route.

Stage 4 charter authority lives in `StoryInteractionLaw.md`. Legacy engine
wrappers, PV text, best-move explanations, and raw eval carriers do not open
public truth. Stage 4-1 admits only `EngineCheck`, `EngineLine`, and
`EngineEval` as internal evidence shape for existing `Tactic.Hanging` Stories.
Stage 4-2 admits only same-board and stale guards over that shape; legacy
engine lines from another FEN, route-mismatched PVs, stale data, eval-only
rows, and PV-only rows remain diagnostic only.
Stage 4-3 attaches EngineCheck only to `Tactic.Hanging`; legacy engine support
does not create winning, best-move, decisive, PV-explanation, or public-eval
claims.

Stage 5 charter authority lives in `StoryInteractionLaw.md`. Legacy selection,
ranking, and renderer-facing claim paths do not open Story Order authority.
Stage 5 admits only role ordering for existing `Tactic.Hanging` Story rows; it
does not create Stories, open new positive families, or promote
engine eval, Board Facts, or `CaptureResult` into public claims.
Stage 5-1 Hanging Role Rules also live in `StoryInteractionLaw.md`; legacy
support/context buckets do not become public sentence authority.
Stage 5-2 Deterministic Ordering also lives in `StoryInteractionLaw.md`; legacy
selector ranking, raw engine ordering, raw PV ordering, proof-failure text
ordering, and renderer wording order do not return.
Stage 5-3 Conflict and Block Rules also live in `StoryInteractionLaw.md`;
legacy conflict override systems, plan suppression, blunder override, defense
relations, and strategy suppression do not return.
Stage 5-4 Verdict Diagnostic Boundary also lives in `StoryInteractionLaw.md`;
legacy renderer input paths, LLM input paths, public route payloads, and
diagnostic-to-public-value promotion do not return.
Stage 5 closeout also lives in `StoryInteractionLaw.md`; legacy downstream
handoffs from raw Board Facts, CaptureResult, EngineCheck, engine eval, or PV
text do not return.

Stage 6 charter authority lives in `StoryInteractionLaw.md`. Legacy outline,
renderer, LLM, public prose, source-row, proof-failure, and engine-explanation
paths do not return. Stage 6-0 admits only selected Verdict speech bounds for
Explanation Plan data; it does not revive raw BoardMood, root atoms,
CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV, source rows, or
wording from renderer or LLM layers as input authority.
Stage 6-1 opens only the Explanation Plan shape for one selected
`Tactic.Hanging` Lead Verdict. `allowedClaim` stays a structured key such as
`can_win_piece`; the first shape carries `bounded` strength and forbidden
wording, not public prose. Legacy full-sentence generation, user-facing prose,
engine-says wording, best-move wording, winning wording, decisive wording, and
public eval do not return.
Stage 6-2 opens only `Tactic.Hanging` allowed claim mapping. Uncapped Lead
Verdict only may carry an allowed claim key. Support, Context, Blocked, and
engine-capped Verdicts do not create standalone public claims.
`engineStrengthLimited` suppresses claim keys and strengthens forbidden
wording; legacy free-piece, blunder, winning, decisive, forced-win, best-move,
no-counterplay, and engine-approved claim paths do not return.
Stage 6-3 opens only forbidden wording boundary. Explanation Plan must carry
the default forbidden wording set. `Tactic.Hanging` remains bounded material
tactic wording only, and `engineStrengthLimited` strengthens forbidden wording
without carrying a claim.
Legacy king-safety, file-control, outpost, strategic-key, conversion, and
mate-net wording paths do not return.
Stage 6-4 opens only Support and Context relation structure. Uncapped Lead
only carries an allowed claim. Support, Context, Blocked, and engine-capped
Verdicts create no standalone public claim. Blocked remains debug-only
relation structure, and proofFailures do not feed relation wording. Legacy
Support standalone sentences, Context standalone sentences, Blocked debug text
as user explanation, and proofFailures wording do not return.
Stage 6-5 opens only the selected Verdict input guard. Raw BoardFacts,
BoardMood, root atoms, CaptureResult, EngineCheck, EngineEval, EngineLine, raw
PV, proofFailures text, unselected Story, unselected Verdict, and source rows
do not return as Explanation Plan inputs.
Stage 6 closeout confirms Explanation Plan only. Blocked, Support, Context,
engine-capped, and engine-refuted Verdicts create no allowed claim or public
claim. Stage 7 deterministic renderer may receive Explanation Plan only; raw
Verdict, EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV,
proofFailures text, source rows, and raw engine evidence do not return as
renderer inputs.
Stage 7-0 charter authority lives in `StoryInteractionLaw.md`. Legacy
renderer, outline, source-row, proof-failure, raw evidence, LLM, public route,
pedagogy, engine PV explanation, best-move explanation, engine-says, winning,
decisive, forced, blunder, and free-piece wording paths do not return. Stage
7-0 admits only `ExplanationPlan` input, deterministic template,
`Tactic.Hanging` bounded claim phrasing, forbidden wording check, no LLM, and
no public route.
Stage 7-1 input guard authority lives in `StoryInteractionLaw.md`. Legacy
renderer entrypoints from raw Verdict, Story, BoardFacts, BoardMood,
CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV, proofFailures, or
source rows do not return. Renderer receives `ExplanationPlan` only.
Stage 7-2 minimal template authority lives in `StoryInteractionLaw.md`.
Legacy broad renderer templates, stronger tactical wording, engine wording,
best-move wording, and non-Hanging claim text do not return unless a later
live section explicitly reopens a named tactic template. Only the
`can_win_piece` claim key may become first deterministic `Tactic.Hanging`
text, bounded by ExplanationPlan evidenceLine.
Stage 7-3 forbidden wording enforcement authority lives in
`StoryInteractionLaw.md`. Legacy renderer wording that violates
`ExplanationPlan.forbiddenWording` does not return. `win material` wording is
allowed only when `allowedClaim` is `CanWinPiece`; `winning position`,
engine-strength-limited strong wording output, missing-claim output, and
debug-only output do not return.
Stage 7-4 no-standalone-text authority lives in `StoryInteractionLaw.md`.
Legacy Support, Context, Blocked, capped no-claim, and engine-refuted relation
renderer text does not return. Renderer phrases only Lead plans with an
allowed claim.
Stage 7-5 RenderedLine shape authority lives in `StoryInteractionLaw.md`.
Legacy renderer payloads with CaptureResult, EngineCheck, BoardFacts,
proofFailures, raw route analysis, source rows, proof ownership, or engine data
ownership do not return. `RenderedLine` owns no chess meaning, proof, or
engine data; RenderedLine is only the expression result of ExplanationPlan.
Stage 7-6 renderer baseline authority lives in `StoryInteractionLaw.md`.
Stage 7-6 opens only renderer baseline tests. Legacy renderer baseline gaps do
not return as permission for stronger wording, direct Verdict or EngineCheck
input, engine mentions, best-move wording, blunder wording, free-piece wording,
decisive wording, forced wording, or winning-position wording. Renderer output
is no stronger than ExplanationPlan.
Stage 7 closeout authority lives in `StoryInteractionLaw.md`. Deterministic
renderer is closed as a template baseline. Legacy renderer, LLM, public route,
pedagogy, new Story family, direct Verdict input, direct Story input, direct
EngineCheck input, direct CaptureResult input, and direct Board Facts input do
not return. Stage 8 LLM Narration may receive deterministic text and
ExplanationPlan only.
Fork-8 deterministic renderer authority lives in `StoryInteractionLaw.md`.
Legacy broad Fork renderer templates do not return. The only reopened Fork
renderer text is the selected Fork ExplanationPlan template over route, target,
and secondaryTarget; it does not reopen material, wins-queen, best-move,
engine-says, decisive, forced, no-counterplay, blunder, production LLM
narration, or public route paths.
Fork-9 LLM smoke authority lives in `StoryInteractionLaw.md`. Legacy broad
Fork LLM paths do not return. The only reopened Fork LLM surface is prompt
smoke over selected Fork ExplanationPlan and RenderedLine; raw Verdict, Story,
MultiTargetProof, EngineCheck, BoardFacts, engine eval, raw PV,
proofFailures, production API, and public route paths do not return.
Fork Slice Closeout authority lives in `StoryInteractionLaw.md`. Legacy broad
Fork family expansion does not return. PawnFork, Skewer, QueenHit, Tempo,
Scene.Defense, Plan, Strategy, production API, public route, and
public/user-facing LLM paths remain closed by the closeout. Fork does not open
Scene.Material by implication; Material-3 separately opens only the narrow
named Scene.Material writer.
Material-0 and Material-1 authority lives in `StoryInteractionLaw.md`.
`Scene.Material` is fixed as a scene Story label, not a tactic and not proof.
The first Material proof-home decision reuses `CaptureResult` for simple
capture and immediate bounded recapture. Legacy material evaluation,
conversion, winning, decisive, blunder, best-move, forced, no-counterplay,
engine-says, production API, public route, and broad Material renderer paths do
not return.
Material-2 authority lives in `StoryInteractionLaw.md`. `CaptureResult` may
carry captured pieces and bounded exchange sequence proof fields, but legacy
material sentences, public Story creation, renderer text, LLM input,
winning-position, decisive-advantage, conversion, blunder, best-move, and
forced-line paths do not return.
Material-3 authority lives in `StoryInteractionLaw.md`. The named
`SceneMaterial` writer may create one narrow proof-backed `Scene.Material`
Story, but legacy Material ExplanationPlan mapping, renderer text, LLM input,
public route, production API, winning, decisive, blunder, conversion,
best-move, forced, no-counterplay, engine-says, and full-evaluation paths do
not return.
Material-4 authority lives in `StoryInteractionLaw.md`. The Material negative
corpus keeps material-looking false positives silent: legal-line missing,
same-board missing, erased material result, unclear exchange result, king
target, zero material result, EngineCheck Refutes, incomplete StoryProof,
incomplete material proof, tactic-writer duplication, Hanging/Fork
auto-duplication, and high Proof score only do not return legacy Material
speech.
Material-5 authority lives in `StoryInteractionLaw.md`. Existing EngineCheck
may support, cap, or refute only an existing Material Story with same-board
proof, same Story route, same legal line, and fresh or depth evidence. Legacy
engine-created Material, public engine truth, PV explanation, best-move
explanation, winning claim, and MaterialEngineCheck duplicate type paths do not
return.
Material-6 authority lives in `StoryInteractionLaw.md`. StoryTable may
deterministically order only existing Hanging, Fork, and Material rows.
Refuted, incomplete, writerless, or material-proof-missing Material rows are
Blocked; Support and Context remain non-sentence roles. Legacy StoryTable
Story creation, raw-engine-eval ranking, material proof text, renderer wording,
conversion, and winning paths do not return.
Material-7 authority lives in `StoryInteractionLaw.md`. Selected Material
Verdicts may lower only to ExplanationPlan data. Legacy direct material proof
input, CaptureResult or ExchangeResult input, EngineCheck input, BoardFacts
input, raw PV input, proofFailures wording, source row input, Material renderer
text, LLM smoke, public route, production API, winning-position,
decisive-advantage, conversion, blunder, best-move, forced-win, and
no-counterplay paths do not return.
Material-8 authority lives in `StoryInteractionLaw.md`. Deterministic Material
renderer text may phrase only selected Material ExplanationPlan input. Legacy
raw Verdict input, Story input, direct material proof input, CaptureResult or
ExchangeResult input, EngineCheck input, BoardFacts input, raw PV input,
proofFailures wording, source row input, LLM smoke, public route, production
API, winning, decisive, blunder, forced, best-move, no-counterplay,
engine-says, conversion, and technically-winning wording paths do not return.
Material-9 authority lives in `StoryInteractionLaw.md`. Material LLM smoke may
receive only selected Material ExplanationPlan and RenderedLine. 8B Material
Codex CLI input is limited to renderedText, claimKey, strength, forbidden
wording, and `Rephrase only. Do not add chess facts.` Legacy raw Verdict,
Story, material proof, CaptureResult, ExchangeResult, EngineCheck, BoardFacts,
engine eval, raw PV, proofFailures, source row, public/user-facing LLM
narration, public route, production API, winning, decisive, forced, blunder,
best-move, conversion, and stronger-claim paths do not return.
Material Slice Closeout authority lives in `StoryInteractionLaw.md`. Legacy
Defense, Conversion, Winning, Plan, Strategy, Blunder, best-move,
no-counterplay, full-engine-PV, public route, production API, and
public/user-facing LLM paths do not return through Material. `ExchangeResult`
and `MaterialEngineCheck` were not created in this slice. Legacy alternate
Material proof-to-text structures do not return; Material reuses the existing
proof home, Story writer, EngineCheck, StoryTable, ExplanationPlan, Renderer,
and LLM smoke skeleton.
Stage 8 prompt smoke authority lives in `StoryInteractionLaw.md`. Stage 8 opens
only 8A Mock narrator and 8B Codex CLI prompt smoke test. Legacy LLM renderer
paths, production API validation, raw proof inputs, engine mentions, best-move
wording, forced wording, winning wording, decisive wording, blunder wording,
free-piece wording, new moves, new lines, new tactics, and new plans do not
return.
