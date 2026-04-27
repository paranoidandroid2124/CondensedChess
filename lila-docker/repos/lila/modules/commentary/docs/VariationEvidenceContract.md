# Public-Safe Variation Evidence Contract

This document freezes the current public-safe prepared variation evidence
contract.

It does not open book-style prose, controller route wiring, product frontend
UI, raw engine PV exposure, raw source-row exposure, or model-authored
commentary.

## Boundary

Prepared variation evidence is a line-proof object. It is not raw engine
evidence and it is not source context.

The live runtime names are:

- `CandidateLineEvidence`
- `CandidateBranchId`
- `CandidateBranchRole`
- `CandidateLineProvenance`
- `CandidateLineProvenanceKind`
- `CandidateLineReplayStatus`
- `CandidateLineTiming`
- `CandidateLineSearch`
- `CandidateLinePreparedTarget`
- `CandidateLineNormalizationInput`
- `CandidateLineRawLine`
- `CandidateLineNormalizer`
- `CandidateLinePacket`
- `CandidateLineChildPacket`
- `CandidateLinePacketHandoffInput`
- `CandidateLinePacketHandoff`
- `CandidateLineEvidenceLowering`
- `CandidateLineEvidenceLowering.Policy`
- `CandidateLineEvidenceLowering.Binding`
- `PreparedVariationEvidence`
- `PreparedVariationBoundary`
- `PreparedVariationDebug`
- `VariationMove`
- `VariationMoveRole`
- `VariationProofPurpose`
- `VariationEvidenceRole`
- `VariationTestResult`
- `VariationSurfaceAllowance`
- `PlanVariationEvidence`
- `RenderVariationEvidence`
- `RenderVariationBoundary`
- `RenderVariationMove`

`CandidateLineEvidence` is an internal branch/probe evidence object in
`lila.commentary.line`. It is not a public prose object and is not renderer or
backend output. It preserves candidate-line exploration identity before any
future lowering into `PreparedVariationEvidence`: branch id, optional parent
branch id, branch role, candidate rank, MultiPV index, start FEN, node id, ply,
side to move, SAN line, UCI line, requested and realized depth, MultiPV count,
freshness window, engine fingerprint, replay legality, provenance kind, source
hint refs, surface allowance, wording cap, explicit failure reasons when
rejected, and an optional prepared-variation target link.

Candidate line roles are:

- `root_candidate`
- `alternative`
- `defender_resource`
- `defender_reply`
- `failed_candidate`
- `premature`
- `release_risk`

`failed_candidate`, `premature`, and `release_risk` are internal/deferred
candidate-line roles at this boundary. They cannot become public line evidence
or lead recommendations from `CandidateLineEvidence`.

Candidate line provenance kinds are:

- `engine_root`
- `engine_child`
- `source_hint`
- `cache`

Only engine-root, engine-child, and cache provenance may be proof-owner
eligible after legal replay, freshness, depth, and public-surface checks pass.
`source_hint` may record opening, motif, or endgame prioritization/context refs
only as non-proof metadata. Retrieval is not a candidate-line provenance kind
and cannot be represented as a current-position candidate source.

`CandidateLineNormalizer` is the backend-owned exact-board line replay
boundary. It consumes an exact start FEN, node id, ply, raw UCI PV line
fixtures, search depth/MultiPV/freshness fields, engine fingerprint, branch
metadata, and provenance metadata. It parses and replays every UCI move from
the exact FEN through the repo chess model, produces SAN from the resulting
legal move object, preserves normalized legal UCI moves, and returns internal
`CandidateLineEvidence` only. Frontend SAN, if ever present, is display hint
material only and is not authority.

Normalizer failures remain internal candidate evidence with explicit failure
reasons and `legalReplay=illegal` when replay fails. The normalizer rejects or
marks non-public malformed UCI, illegal or truncated PV sequences, invalid
start FEN, stale freshness windows, depth below the floor of 16, root MultiPV
mismatch, and duplicate root first moves when branch diversity is required.
Raw UCI PV input does not lower to `PreparedVariationEvidence`, does not create
`CommentaryClaim`, and does not reach renderer/backend public JSON.

`CandidateLinePacketHandoff` is the internal sanitized multi-packet boundary
for candidate-line evidence. It accepts one root `CandidateLinePacket` bound to
the current FEN, node id, and ply, plus zero or more `CandidateLineChildPacket`
values tied to a parent candidate branch id and parent UCI prefix. The handoff
normalizes root and child packets only through `CandidateLineNormalizer`.
Root FEN/node/ply binding failure fails closed and yields no candidate-line
evidence. Duplicate root branch ids also fail closed before child parent
binding because branch identity would otherwise be ambiguous. A child packet
may attach only when its parent branch id exists among the normalized root
candidates, its role is `defender_resource` or `defender_reply`, its node/ply
binding matches the declared parent prefix, and its start FEN equals the exact
board reached after that prefix. Child failures are preserved as rejected
internal `CandidateLineEvidence` and do not discard valid root evidence.
Source-hint provenance remains `source_hint` through the handoff and therefore
non-proof. The handoff is not a public API shape, does not schedule probes,
does not use cache, does not lower to prepared variation evidence, and does not
create claims or prose.

`CandidateLineEvidenceLowering` is the deterministic internal boundary that
lowers sanitized `CandidateLineEvidence` into public-safe
`PreparedVariationEvidence` after a caller supplies the exact-board claim
binding. It does not normalize SAN, validate packets, orchestrate probes,
perform cache lookup, create claims, write prose, or expose raw packet fields.
Lowering is fail-closed and freezes this policy:

- depth floor: `16`
- preferred target depth: `18`
- strong/cache target depth: `20`
- root MultiPV default target: `3`
- accepted root MultiPV range: `2-3`
- root MultiPV hard cap: `3`
- child MultiPV default: `1`
- accepted child MultiPV range: `1-2`
- child MultiPV hard cap: `2`
- child defender replies are considered only for the top `2` root candidates
  by default
- rank `3` root candidates lower only as context/alternative lines unless the
  lowering policy explicitly allows child proof for the third root

Root rank `1` lowers as the main candidate line. Root rank `2` lowers as an
alternative candidate line. Root rank `3` lowers as a context candidate line.
Rank `1` child `defender_resource` / `defender_reply` evidence lowers as
defender-resource evidence for its eligible parent line. Rank `2` child
evidence may lower as a second defender-resource line only for an eligible
top-two root candidate and only when public-line eligible.
`failed_candidate`, `premature`, and `release_risk` roles remain
internal/deferred and do not lower. `source_hint` provenance remains
non-proof metadata and cannot become a proof-owned
`PreparedVariationEvidence`. Retrieval remains outside current-position
candidate proof.

The current prepared variation shape is sufficient for V3d. Branch id, rank,
and MultiPV are consumed inside the lowering policy; public output carries the
stable proof id, legal SAN/UCI line, bounded depth/MultiPV boundary,
claim-binding metadata, and bounded provenance ref only. Public response shape
is unchanged. The public proof id must be a lowering-owned stable line id; it
must not encode `CandidateBranchId`, parent branch id, rank, or MultiPV index.
Lowering must also fail closed when the supplied binding provenance is
`RawEngine`, `SourceContext`, unbound, or mismatched to the supplied owner,
anchor, route, or scope.

Selection may admit prepared variation evidence only when it is already bound
to an admitted exact-board claim. The binding must match claim id, owner,
anchor, route, scope, and defender when defender is present.

Source-context and raw-engine claims may not carry prepared variation evidence
as a truth owner. Such rows are suppressed or kept internal before outline.
Opening source/context may carry a non-authoritative
`opening-line-test:*:context` reference to a prepared proof id, but that link
is not provenance and cannot make the source row a proof owner. The prepared
variation proof must remain bound to a separately admitted exact-board claim.
Motif source/context follows the same rule for `motif-line-test:*:context`:
the motif row may help name an attacked target, restricted piece, defender
resource, failed resource, held resource, or partial resource only after exact
detector carrier parity exists, but the prepared line proof still belongs to
the separately admitted exact-board claim.
Endgame-study source/context follows the same rule for
`endgame-line-test:*:context`: the study row may frame technique such as
opposition, checking distance, rook activity, bridge/third-rank/side-checking
setup, method/exception, defender resource, or hold line only after exact
material/placement/relation/side-to-move applicability exists. The prepared
line proof still belongs to the separately admitted exact-board claim and must
not become a win, draw, loss, forced-conversion, tablebase, Syzygy, WDL, DTZ,
DTM, or oracle claim.
Retrieval source/context follows the same rule for
`retrieval-line-test:*:context`: the retrieved row may frame a comparable
line, similar plan sequence, theme example, or bounded citation context, but
it is optional illustration only. It cannot transfer claims from the retrieved
game to the current board, cannot use player/event/result metadata as a
recommendation or verdict, and cannot own `PreparedVariationEvidence`. The
prepared line proof must remain bound to a separately admitted exact-board
claim.

## Public Fields

The public render shape may expose only:

- proof id
- bound claim id
- start FEN
- owner / defender / anchor / route / scope
- evidence role:
  - `defender_resource`
  - `failed_tempting_move`
  - `release_risk`
  - `hold`
  - `conversion`
  - `persistence`
  - `premature_move`
  - `simplification`
- move role
- legal SAN and UCI line
- played move, candidate move, defender resource, and continuation when
  prepared as public-safe move pairs
- tested move, tested line, reply line, and resource line when prepared as
  public-safe move pairs
- restrained test result:
  - `resource_works`
  - `resource_fails`
  - `releases_counterplay`
  - `does_not_restore_counterplay`
  - `defensive_hold`
  - `move_premature`
  - `simplifies`
  - `converts`
  - `pressure_persists`
- structured proof token
- proof purpose
- bounded provenance refs that match the same owner / anchor / route / scope
- depth floor, realized depth, MultiPV, freshness checked, legal replay
  checked, and baseline checked flags
- wording cap
- public surface allowance:
  - `public_line`
  - `boundary_only`

The renderer emits these as structured fields only. It does not generate
book-style annotation prose from them. Public line rendering requires
`public_line`; `boundary_only` may be carried through selection/outline for
future capped use but is not emitted as a public line proof by the current
renderer.

## Internal Fields

`PreparedVariationDebug` is internal-only. It may carry diagnostic handles such
as variation hash, engine configuration fingerprint, raw packet id, or raw line
index. None of those fields may be serialized into public `CommentaryRender` or
`CommentaryResponse`.

Raw engine packets, raw `EngineEvidencePacket`, raw PV packets, centipawn or
mate scores, source rows, and source snippets remain outside the public
variation evidence contract.

`VariationSurfaceAllowance.internal_only` is also non-public. A proof marked
internal-only may remain available to internal diagnostic code but must be
suppressed before public outline/render output.

## Safety Rules

A prepared variation proof is public-safe only when:

- `publicSafe=true`
- start FEN is present
- SAN and UCI line arrays are non-empty and the same length
- depth floor is positive and realized depth meets it
- MultiPV is positive
- freshness and legal replay checks passed
- surface allowance is not `internal_only`
- provenance refs are present, bounded, not `RawEngine`, not `SourceContext`,
  and match the same owner, anchor, route, and scope as the proof
- the proof token avoids forbidden overclaim vocabulary
- selection binding matches the selected claim

The renderer applies an additional public surface check: only `public_line`
proofs become `RenderVariationEvidence`.

Forbidden public proof wording includes best, forced, winning, drawing, drawn,
result, oracle, engine, raw PV, eval, and theory-truth claims.

The allowed proof purposes are:

- `holds`
- `fails`
- `releases_counterplay`
- `simplifies`
- `preserves_pressure`
- `denies_resource`

These purposes bound what a future prose renderer may say. They do not create
best-move, forced-line, result, winning, drawn, tablebase, or oracle truth.

## Defender Resources And Failed Temptations

Prepared line evidence may model a bounded test, not only a positive move:

- `defender_resource`: a legal reply/resource line that shows whether the
  opponent's resource works, fails, holds, or does not restore counterplay.
- `failed_tempting_move`: a natural or tempting move whose prepared reply line
  shows why it is wrong, premature, or releases counterplay.
- `release_risk`: a tested line showing that a move releases counterplay.
- `hold`: a tested defensive line that demonstrates a defensive hold in
  restrained terms.
- `conversion` / `simplification`: a bounded line that shows simplification or
  conversion route, without result wording.
- `persistence`: a line showing that pressure persists after the tested reply.

Failed tempting and premature-move evidence is negative/support evidence. By
itself it cannot become a main recommendation or lead claim. It may be carried
only as selected safe evidence for a separately admitted exact-board claim.

## Wording Cap Table

| Evidence role/result | Public wording ceiling | Forbidden upgrade |
| --- | --- | --- |
| `defender_resource` with `resource_fails` or `does_not_restore_counterplay` | the resource fails or does not restore counterplay inside the shown line | best defense, forced refutation, winning/result claim |
| `defender_resource` with `resource_works` or `defensive_hold` | the resource works/holds in the shown line | drawn/result/tablebase claim |
| `failed_tempting_move` or `premature_move` with `move_premature` | the tested move is premature or fails in the shown line | main recommendation, best move, forced loss |
| `release_risk` with `releases_counterplay` | the tested move releases counterplay in the shown line | losing/winning/result claim |
| `conversion` or `simplification` with `simplifies`/`converts` | the shown line simplifies or converts a bounded advantage in restrained terms | forced conversion, winning/result/oracle claim |
| `persistence` with `pressure_persists` | pressure persists after the shown continuation | best line, forced pressure, engine proof |

## Runtime Handoff

The current path is:

`CommentaryClaim.variationEvidence -> ClaimSelector -> CommentaryOutline.variationEvidence -> CommentaryPlan.variationEvidence -> CommentaryRender.variationEvidence`

`CommentaryOutlineBuilder` copies selected prepared variation evidence without
adding meaning. `CommentaryRenderer` exposes only the public-safe render subset
and filters unsafe or malformed plan variation evidence.

`CommentaryBackendSeam` serializes only `CommentaryRender`; internal debug
fields remain unavailable through the public response.

## Executable Validation

Executable validation lives in:

- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineEvidenceContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineNormalizerContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineEvidenceLoweringPolicyTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/selection/PreparedVariationEvidenceContractTest.scala`

The tests prove:

- internal candidate line evidence can represent root alternatives, child
  defender branches, two defender replies, branch identity, candidate rank,
  MultiPV index, SAN/UCI lines, depth, freshness, replay status, provenance,
  and a prepared-variation target link without becoming public render output
- the backend line normalizer converts legal exact-FEN UCI PVs into SAN/UCI
  `CandidateLineEvidence`, preserves branch identity/rank/MultiPV/parent
  metadata, and fails closed for malformed UCI, illegal sequences, invalid FEN,
  stale evidence, shallow depth, MultiPV mismatch, duplicate root first moves,
  and source-hint-only provenance
- the sanitized multi-packet handoff accepts a bound root packet, attaches
  child defender-resource and defender-reply packets only through exact
  parent-prefix FEN validation, rejects missing-parent and wrong-child-FEN
  packets locally, and leaves public backend/render JSON free of raw packet and
  raw PV fields
- the lowering policy admits depth `16`, rejects depth `15`, requires root
  MultiPV `2-3`, rejects root MultiPV `1` or above the hard cap, lowers root
  ranks `1/2/3` as main/alternative/context line evidence, lowers at most two
  child defender replies for eligible top-two root candidates, ignores rank
  `3` child evidence unless explicitly allowed, rejects duplicate root ranks
  or MultiPV indexes, rejects source-hint, raw-engine/source-context/unbound
  binding provenance, non-public, failed, premature, and release-risk candidate
  evidence, keeps public proof ids free of internal branch ids, and keeps
  raw/internal candidate-line fields out of public backend/render JSON
- safe prepared variation evidence survives selection into outline and plan
- unsafe raw-style variation evidence is suppressed before outline
- outline does not infer new claim meaning
- renderer and backend public output do not expose debug fields
- source context and raw engine evidence do not become board truth owners
- motif source context can link to line tests only as context after exact
  detector carrier parity; it cannot own `PreparedVariationEvidence`
- endgame-study context can link to line tests only as context after exact
  applicability; it cannot own `PreparedVariationEvidence` or result truth
- retrieval context can link to line tests only as illustrative context; it
  cannot transfer current-position proof, recommendation, verdict, game
  result, or citation-display meaning from the retrieved row
- defender-resource evidence survives only when exact-board legal, fresh,
  depth-bounded, and provenance-bound
- failed tempting move evidence cannot become a main recommendation by itself
- wording caps and proof tokens prevent best, forced, result, and oracle
  overclaims
- internal-only or stale line evidence is suppressed before public render
