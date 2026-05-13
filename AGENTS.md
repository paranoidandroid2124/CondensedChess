# Agent Instructions

For Chesstory commentary work on this branch, treat the backend as a
proof-first chess-story kernel. It is not the full commentary product yet.

AGENTS.md is a concise operator guide. Do not append full slice history,
completion reports, detailed stage laws, or repeated closeout checklists here.
Detailed slice authority lives in `StoryInteractionLaw.md`; this file keeps
only durable guardrails, current-scope pointers, and no-go rules.

## Live Documentation Authority

Live commentary documentation authority is exactly and exhaustively:

- `lila-docker/repos/lila/modules/commentary/docs/ChessCommentarySSOT.md`
- `lila-docker/repos/lila/modules/commentary/docs/ChessModelArchitecture.md`
- `lila-docker/repos/lila/modules/commentary/docs/ChessModelContract.md`
- `lila-docker/repos/lila/modules/commentary/docs/ChessResetRationale.md`
- `lila-docker/repos/lila/modules/commentary/docs/BoardFacts.md`
- `lila-docker/repos/lila/modules/commentary/docs/BoardMoodCutLaw.md`
- `lila-docker/repos/lila/modules/commentary/docs/BoardMoodSplitLaw.md`
- `lila-docker/repos/lila/modules/commentary/docs/StoryInteractionLaw.md`
- `lila-docker/repos/lila/modules/commentary/docs/StoryResurrectionLaw.md`
- `lila-docker/repos/lila/modules/commentary/docs/LegacyPruneManifest.md`
- `lila-docker/repos/lila/modules/commentary/docs/README.md`

`AGENTS.md`, `modules/commentary/docs/README.md`, and
`LegacyPruneManifest.md` must agree on this list. Any mismatch is a no-go
state, not a second source of authority.

Documents under
`lila-docker/repos/lila/modules/commentary/docs/legacy-pre-semantic-reset/`
are historical reference only. They do not grant current runtime authority,
selection authority, renderer authority, public claim ownership, or test
acceptance.

Retired root authority documents must not return:

- `CommentaryCoreSSOT.md`
- `SemanticModelArchitecture.md`
- `LegacyArchiveIndex.md`
- `CommentaryFrontendBridgeContract.md`

## Branch Direction

Product north-star philosophy:

- Engine is the truth oracle.
- Backend is the proof and pedagogy system.
- LLM is the narrator.

Operational slogan:

- Board Facts observes.
- Story Proof binds.
- Positive Story Writer permits.
- Engine Check supports, caps, or refutes.
- StoryTable orders.
- Verdict decides.
- Explanation Plan bounds speech.
- Renderer phrases.
- LLM only polishes.
- Verifier rejects overclaim.

The LLM does not judge chess. Engine truth, exact-board validation, legal
replay, proof sidecars, and `StoryTable` decide what can be said. LLM phrasing
comes after selected `Verdict` data only, lowered through `ExplanationPlan` and
`RenderedLine` contracts.

Raw engine numbers, raw engine text, source rows, and proof failure text are
never public claim owners. Backend policy owns proof, Story selection,
arbitration, and pedagogy.

## Authority Chain

The live public chess meaning chain is:

`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`

No other public path owns current chess meaning. ExplanationPlan, renderer,
and LLM smoke are downstream expression layers only. They must not create,
repair, rank, prove, or strengthen chess meaning.

Core proof boundary:

- feature is not a claim
- claim is not a public `Story`
- public `Story` requires proof-bearing identity

Implementation must open in this order only:

`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `ExplanationPlan` -> Renderer -> LLM smoke

Do not implement downstream product stages before earlier authority stages are
proven.

Forbidden dependency shortcuts:

- Renderer before Story proof sidecar is forbidden.
- LLM before Explanation IR is forbidden.
- Strategy before tactical/material proof is forbidden.
- Pedagogy before causal arbitration is forbidden.
- Personalization before stable Story taxonomy is forbidden.

## Current Scope Handling

The active slice and closed baselines are owned by `StoryInteractionLaw.md`.
When continuing work:

- read the current slice section in `StoryInteractionLaw.md`
- keep AGENTS.md unchanged unless the durable operator rules change
- add detailed stage rules and closeout criteria only to `StoryInteractionLaw.md`
- summarize scope in README/SSOT/Architecture/Contract only when the live
  documentation needs a concise manual summary

Recent narrow baselines include the material-contact neighborhood and the
line/defender neighborhood. Their exact status, proof requirements, negative
corpora, renderer/LLM boundaries, and closeout rules live in
`StoryInteractionLaw.md`.

## Naming And Duplication

Prefer chess-player-friendly names, but only when the proof boundary stays
clear. Every new name must classify as exactly one of:

- board observation
- proof home
- Story label
- speech key / wording

Do not introduce workflow, stage, audit, fixture, gate, checklist, or report
names as production concepts. Runtime modules and tests should be named for the
actual proof home, writer, Story behavior, or invariant they verify. Stage
numbers and process labels belong in operator instructions or closeout notes,
not in runtime class names, helper APIs, public keys, or durable test concepts.

Authority consolidation is mandatory. Duplication audits are mandatory in every
closeout:

- `One chess meaning, one home`
- `One observation family, one owner`
- `One public claim, one proof path`
- one Story label per public chess meaning
- one speech key per allowed public claim
- one live document for detailed authority

Before adding a type, module, row, field, or docs-authority name, ask:

- Is this chess meaning truly new?
- Can an existing Fact or proof home carry it?
- Would this create a second owner for the same board phenomenon?
- Would a later Story proof know which input to trust?
- Is this an observation, proof home, Story label, or speech key?

New names are the last resort. Prefer extending existing owners such as
`PieceContact`, `LineFact`, `CaptureResult`, `LineProof`, or the active proof
home when the meaning already lives there.

## Opened Meaning Boundaries

Only named, proof-backed writers may create Stories. Proof scores, Board Facts,
engine evidence, source context, renderer text, and LLM text never create a
Story by themselves.

The currently opened narrow Story labels are the ones explicitly admitted by
`StoryInteractionLaw.md`, including the material-contact and line/defender
vertical slices. Broad families remain closed unless that file explicitly opens
a smaller proof-backed slice.

Examples of closed broad meanings unless explicitly opened by live law:

- broad LineTactic, XRay, broad deflection, overload
- pressure, initiative, compensation, strategy
- king safety, mate threat, no-counterplay
- winning, decisive, forced, best move, only move
- public/user-facing LLM narration
- production API and public route `200`

## Public Surface No-Go

Public route no-go:

Public routes remain fail-closed tombstones until an explicit public-surface
contract opens them. Do not return `200`, rendered payloads, environment
switches, frontend mocks, or user-facing LLM narration from:

- `/api/commentary/render`
- `/internal/commentary/render-local-probe`

Renderer input is `ExplanationPlan` only. LLM smoke input is only the bounded
prompt contract admitted by `StoryInteractionLaw.md`, such as rendered text,
claim key, strength, forbidden wording, and the instruction:

`Rephrase only. Do not add chess facts.`

No `BoardMood` Sxxx expansion or re-entry is allowed unless a live authority
document and docs test explicitly open a smaller exact chess fact with
same-board producer proof.

Forbidden-name no-go: do not add new core model, type, module, or
docs-authority names that recreate retired selector, pipeline, score-vector,
or versioned authority patterns.

## Engine And Diagnostics

EngineCheck may support, cap, or refute only an already existing Story. It
must not create a Story, choose a Story, write a Verdict, feed a renderer
directly, feed an LLM directly, or become public truth.

proofFailures are internal diagnostics only. They may be used for tests and
debugging, but must not become public JSON, renderer input, or LLM input.

`Verdict.values` keeps its fixed public-safe shape. Raw proof failures,
EngineCheck diagnostics, engine text, raw PV, and source-row data stay out of
public values and downstream wording.

## Renderer And Notation

Renderer and LLM output must be no stronger than the selected
`ExplanationPlan`.

Player-facing move notation defaults to SAN. `Line` endpoints, engine move
coordinates, and replay coordinates are internal proof bindings only. SAN formats already-approved legal moves only.
check or mate marks in SAN do not create Story, Proof, Verdict,
ExplanationPlan, or public claims.

## Documentation Hygiene

Do not duplicate detailed stage law across live documents.

- `StoryInteractionLaw.md` owns slice charters, proof conditions, negative
  corpus, downstream boundaries, closeout standards, and duplication audits.
- `README.md`, SSOT, Architecture, Contract, and Manifest may summarize only.
- `AGENTS.md` points to authority and states durable operator guardrails only.

If a rule appears in more than one live document, one copy must be the detailed
authority and the other copies must explicitly be summaries.

Do not add documentation, prose, or marker tests for `StoryInteractionLaw.md`,
README, SSOT, Architecture, Contract, Manifest, or AGENTS.md. Documentation is
reviewed manually at closeout when needed. Runtime invariants must be tested in
runtime tests, not by parsing documentation prose.

## Verification Discipline

For runtime behavior changes, run targeted commentary tests. For documentation
changes, perform manual closeout review when needed; do not add or run
documentation/prose/marker tests. Always run `git diff --check` before claiming
cleanup is complete.

Older branch notes or closeout records that mention docs authority tests are
historical only. They are not current verification instructions.
