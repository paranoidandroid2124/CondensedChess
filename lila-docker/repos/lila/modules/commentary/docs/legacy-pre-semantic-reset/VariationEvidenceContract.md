# Public-Safe Variation Evidence Contract

This document freezes the current public-safe prepared variation evidence
contract.

It opens only the first closed renderer-side English line comments from
`LineCommentaryPlan`. It does not open controller route wiring, product
frontend UI, raw engine PV exposure, raw source-row exposure, or model-authored
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
- `CandidateProbePlan`
- `CandidateProbeRequest`
- `CandidateProbeRole`
- `CandidateProbeBudget`
- `CandidateProbeSource`
- `CandidateProbeCacheKey`
- `CandidateProbeCacheEntry`
- `CandidateProbeAssemblyResult`
- `CandidateProbeResultLine`
- `CandidateProbeResultPayload`
- `CandidateProbeCacheLookupResult`
- `CandidateProbeCacheWriteCandidate`
- `CandidateLineProofCache`
- `CandidateProbeControlledAdapter`
- `CandidateProbeControlledAdapter.Input`
- `CandidateProbeControlledAdapter.AssemblyResult`
- `CandidateLineAssemblyProvider`
- `CandidateLineAssemblyProvider.Input`
- `CandidateRootProbeIntegration`
- `CandidateRootProbeIntegration.Input`
- `CandidateRootProbeIntegration.Result`
- `CandidateChildProbeIntegration`
- `CandidateChildProbeIntegration.Input`
- `CandidateChildProbeIntegration.Result`
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
- `PlanAnnotationSelection`
- `PlanAnnotationFrame`
- `PlanAnnotationFrameKind`
- `PlanAnnotationStrength`
- `BookAnnotationPlanner`
- `BookAnnotationPlan`
- `BookAnnotationUnit`
- `BookAnnotationBoundary`
- `BookAnnotationWordingRules`
- `LineCommentaryPlanner`
- `LineCommentaryPlan`
- `LineNote`
- `LineNoteKind`
- `LineNoteMeaning`
- `LineSupport`
- `LineCaution`
- `LineCommentaryDetail`
- `LineCommentaryBoundary`
- `EnglishLineCommentary`
- `EnglishLineComment`
- `EnglishLineCommentaryWriter`
- `RenderLineRole`
- `RenderVariationEvidence`
- `RenderVariationMove`
- `CandidateLineSelection`

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

`CandidateProbePlan` is the internal orchestration/cache boundary. It does
not run Stockfish, call frontend WASM, perform live source lookup, create
`CommentaryClaim`, lower to `PreparedVariationEvidence`, or write prose. The
probe policy is centralized in `CandidateProbeBudget.Default`: root candidate
probe `MultiPV 3`, default target depth `18`, public floor `16`, strong/cache
target depth `20`, child defender-resource probe `MultiPV 2`, and default
child probes only for root ranks `1` and `2`. Rank `3` remains context by
default; its child probe may appear only from an explicit expanded budget or a
revalidated cache hit at the strong/cache target depth `20`. The planner is
two-stage: root request first, then child requests derived from already
normalized, public-line-eligible root `CandidateLineEvidence` by replaying the
parent candidate prefix to compute the child start FEN and ply.

`CandidateProbeCacheKey` and `CandidateProbeCacheEntry` are internal proof-cache
boundary types. A cache key must bind the normalized full FEN, variant,
node/ply consumption binding, engine fingerprint, target depth, floor depth,
realized depth, MultiPV, probe role, parent branch id/prefix and parent root
rank for child probes, normalized first move when applicable, and freshness
window. A cache hit is
not proof by itself: it must be revalidated against the current request for
FEN, variant, node/ply, freshness, floor depth, MultiPV, engine fingerprint,
role, parent branch/prefix, and parent root rank before it can be consumed as candidate-line
evidence provenance. Rank `3` cache reuse requires the cache key's target depth
to match the strong/cache target depth `20`; target-depth-18 cache entries do
not open the default rank-3 child path. Source rows, source context, and
retrieval cannot populate the proof cache or become current-position candidate
branch proof.

`CandidateLineProofCache` is the stable internal facade for candidate-line
proof-cache reads and writes. It is not the generic eval, source, or retrieval
cache and has no production persistence backend at this boundary. Reads return
`CandidateProbeCacheLookupResult` only after the stored entry revalidates
through the existing `CandidateProbeCacheEntry` / request logic. Writes accept
only provider/adapter-owned receipts for accepted engine-probe results whose
key exactly matches the completed probe payload; arbitrary caller-constructed
`CandidateProbeCacheWriteCandidate` values are not a facade write API.
Source-context, retrieval, source-row, revalidated-cache, stale, shallow,
wrong-FEN, wrong-variant, wrong-node, wrong-ply, wrong-engine, wrong-depth,
wrong-MultiPV, wrong-role, and wrong-parent-prefix candidates fail closed
before storage. Cache hits are consumption provenance only and are not rewritten
as new writes. The deterministic in-memory implementation exists for contract
tests and internal facade wiring only, while production persistence remains
deferred.

`CandidateProbeAssemblyResult` is the internal bridge from accepted probe
packets to the existing `CandidateLinePacketHandoff`. Invalid root packet
binding yields no candidate-line evidence. Invalid child packet evidence is
preserved as rejected child evidence and does not discard valid root
candidates. A rank `1` main candidate without a valid defender-resource child
is insufficient for strong line explanation promotion. No raw cache packet,
probe request, branch plan, engine fingerprint, parent prefix, or cache key is
public output.

`CandidateProbeControlledAdapter` is the internal adapter over completed
external probe results and revalidated cache hits. It does not run Stockfish or
WASM, perform source lookup, persist cache entries, create `CommentaryClaim`,
or write prose. It accepts stable internal probe-result payloads, cache lookup
results, and an optional lowering binding; converts accepted root and child
payloads into `CandidateLinePacket` / `CandidateLineChildPacket`; delegates all
SAN replay and branch binding to `CandidateLinePacketHandoff`; and may lower
the resulting sanitized `CandidateLineEvidence` to
`PreparedVariationEvidence` only through `CandidateLineEvidenceLowering`.
Missing or invalid root input yields no candidate-line evidence and no cache
write. Missing, invalid, stale, shallow, wrong-FEN, wrong-engine, or
wrong-parent child input is omitted or preserved only as rejected child
evidence while valid root evidence remains. Accepted completed probe results
emit provider-owned cache receipts backed by `CandidateProbeCacheWriteCandidate`
values with the full V3e `CandidateProbeCacheKey`; cache hits never emit cache
writes. Cache lookup
payloads must match the effective revalidated request and the cache entry key,
so a strong target-depth-20 cache key cannot be paired with a target-depth-18
payload. Rank `3` child cache reuse requires strong/cache target depth `20`
unless the caller supplied an explicit expanded budget. Direct child probe
payloads are rechecked against the actual parent root rank and active budget at
the adapter boundary; a caller-supplied rank `3` child request cannot open the
default direct-probe path or emit a proof-cache write. Source-context,
retrieval, and source-row cache entries cannot become proof-owned
candidate-line evidence.

`CandidateLineAssemblyProvider` is the internal-only assembly boundary around
the controlled adapter. It accepts exact current context
(`currentFen`, `nodeId`, `ply`, `variant`), server-owned engine fingerprint,
budget, completed root/child probe payloads, an optional internal
`CandidateLineProofCache` facade for cache lookup, and an optional explicit
`CandidateLineEvidenceLowering.Binding` when prepared evidence is expected. It
derives the frozen root `MultiPV 3` request and child `MultiPV 2` requests from
`CandidateProbePlan`, reads valid proof-cache hits through the facade when
present, does not expose direct root/child cache-hit input fields, delegates
assembly to `CandidateProbeControlledAdapter`, and
stores only through the returned provider/adapter-owned cache receipt. Callers
do not pass probe requests directly through this provider. It returns only
`CandidateProbeControlledAdapter.AssemblyResult`. The provider does not execute
Stockfish, call WASM, perform source lookup, does not add a production
persistence backend, does not persist cache in a production backend, does not
wire controller or public request fields, does not open product frontend UI,
does not create `CommentaryClaim`, does not write prose, and does not expose
raw PV/cache/probe internals publicly.

`CandidateRootProbeIntegration` is the internal root-intake boundary for a
completed root probe payload from a future local executor or a revalidated
proof-cache root hit. It does not execute Stockfish or WASM and does not open
controller, public request, product frontend, claim, or prose paths. It
derives the frozen root request from exact current FEN/node/ply/variant and
backend-owned engine fingerprint, delegates assembly to
`CandidateLineAssemblyProvider`, and returns only the internal assembly plus
the unsatisfied next child probe requests. The default next requests are
defender-resource probes for accepted root ranks `1` and `2` only, at
`MultiPV 2`, target depth `18`, and floor `16`, with parent branch and prefix
derived from sanitized root `CandidateLineEvidence`. Rank `3` is not scheduled
by the default root integration path; it remains cache/expanded-budget only.
Root payloads must bind to exact FEN/node/ply/variant/engine policy, contain
exactly three distinct root candidates for root `MultiPV 3`, meet freshness and
depth floor policy, and replay legally through the backend normalizer. Invalid,
source-owned, retrieval-owned, stale, shallow, wrong-binding, duplicate-first-
move, malformed, or over/under-count root input fails closed to no child
requests, no prepared evidence, and no cache write.

`CandidateChildProbeIntegration` is the internal child-intake boundary for the
next stage after a valid root probe payload or proof-cache root hit. It
consumes exact current context, completed child probe payloads, and optional
proof-cache facade reads only through `CandidateLineAssemblyProvider`,
`CandidateProbeControlledAdapter`, `CandidateLinePacketHandoff`,
`CandidateLineNormalizer`, `CandidateLineProofCache`, and
`CandidateLineEvidenceLowering`. The boundary returns internal assembly plus
the planned child requests that were satisfied or remain unsatisfied. Default
live child scheduling remains limited to root ranks `1` and `2`, child
`MultiPV 2`, target depth `18`, and floor `16`; rank `3` direct child probe
payloads are rejected by default. A valid strong target-depth-20 proof-cache
child hit may satisfy rank `3` without opening the default live path and
without emitting a new write. Accepted child payloads must match the internally
planned child request exactly: child start FEN, node id, ply, parent branch id,
parent prefix, parent root rank, role, engine fingerprint, target/floor depth,
freshness, completed flag, legal UCI replay, and complete child rank/MultiPV
`1/1` and `2/2` policy. Invalid child payloads fail closed locally: valid root
evidence remains, invalid child evidence is omitted from the integration
result, no prepared variation evidence is emitted for that child, no child
cache write is committed, and the planned request remains unsatisfied. Prepared
variation evidence still requires an explicit lowering binding. The boundary
does not execute Stockfish/WASM, wire controllers or product UI, create
`CommentaryClaim`, write prose, or expose child payloads, parent prefixes,
branch ids, engine fingerprints, cache keys, cache writes, provider names, or
adapter internals publicly.

Public analyse input must not send completed-probe payloads to the commentary
route. Completed root/child probe payloads are internal provider inputs only;
they remain outside `CommentaryRequest`, controller/API route input, product
UI, and frontend SAN authority. The backend normalizer remains the exact-FEN
UCI replay and SAN source of truth.

`CandidateLineEvidenceLowering` is the deterministic internal boundary that
lowers sanitized `CandidateLineEvidence` into public-safe
`PreparedVariationEvidence` after a caller supplies the exact-board claim
binding. It does not normalize SAN, validate packets, orchestrate probes,
perform cache lookup, create claims, write prose, or expose raw packet fields.
Lowering is fail-closed and freezes this policy:

- depth floor: `16`
- preferred target depth: `18`
- strong/cache target depth: `20`
- root MultiPV public-safe target: exactly `3`
- accepted root MultiPV range: `3`
- root MultiPV hard cap: `3`
- child MultiPV public-safe target: exactly `2`
- accepted child MultiPV range: `2`
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

The current prepared variation shape remains sufficient through the current
internal seam. Branch id, rank, MultiPV, probe-plan fields, cache fields, UCI
PV, start FEN, boundary/depth data, and provenance refs are consumed inside
internal line/probe boundaries. Public output carries only the stable proof id,
claim-binding metadata, renderer-public role, legal SAN move/line fields,
restrained test result, proof purpose, wording cap, and surface allowance.
Public response shape is intentionally narrower than lower prepared evidence.
The public proof id must be a lowering-owned stable line id; it must not encode
`CandidateBranchId`, parent branch id, rank, or MultiPV index.
Lowering must also fail closed when the supplied binding provenance is
`RawEngine`, `SourceContext`, unbound, mismatched to the supplied owner,
anchor, route, or scope, or uses an unsafe internal provenance id such as a
branch id, cache key, or probe payload token.

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

`PreparedVariationEvidence` is a lower prepared-evidence object and may retain
internal `VariationEvidenceRole` values plus the internal `proves` token for
selection, planning, and test authority. The public renderer boundary is
stricter: `RenderVariationEvidence` exposes a renderer-public `RenderLineRole`
and does not expose `proves`, start FEN, UCI PV, provenance refs,
boundary/depth/MultiPV data, engine fingerprints, cache keys, branch ids, or
raw move UCI.

The public render shape may expose only:

- proof id
- bound claim id
- owner / defender / anchor / route / scope
- renderer-public line role:
  - `resource`
  - `caution`
  - `hold`
  - `conversion`
  - `pressure`
  - `simplification`
- move role
- legal SAN line
- played move, candidate move, defender resource, and continuation as
  SAN-only public move fields
- tested move, tested line, reply line, and resource line as SAN-only public
  move fields
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
- proof purpose
- wording cap
- public surface allowance:
  - `public_line`
  - `boundary_only`

The renderer emits these as structured fields only. It does not generate
book-style annotation prose from them. Public line rendering requires
`public_line`; `boundary_only` may be carried through selection/outline for
future capped use but is not emitted as a public line proof by the current
renderer. Internal `defender_resource` maps to public `resource`; internal
`failed_tempting_move`, `premature_move`, and `release_risk` map to public
`caution`; internal `persistence` maps to public `pressure`.

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
  public-safe by id shape, free of internal branch/cache/probe tokens, and
  match the same owner, anchor, route, and scope as the proof
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
| `defender_resource` with `resource_works` | the resource works inside the shown line | drawn/result/tablebase claim |
| `hold` with `defensive_hold` | the defense holds inside the shown line | drawn/result/tablebase claim |
| `failed_tempting_move` or `premature_move` with `move_premature` | the tested move is premature or fails in the shown line | main recommendation, best move, forced loss |
| `release_risk` with `releases_counterplay` | the tested move releases counterplay in the shown line | losing/winning/result claim |
| `conversion` or `simplification` with `simplifies`/`converts` | the shown line simplifies or converts a bounded advantage in restrained terms | forced conversion, winning/result/oracle claim |
| `persistence` with `pressure_persists` | pressure persists after the shown continuation | best line, forced pressure, engine proof |

## Runtime Handoff

The current public variation-evidence path is:

`CommentaryClaim.variationEvidence -> ClaimSelector -> CommentaryOutline.variationEvidence -> CommentaryPlan.variationEvidence -> CommentaryRender.variationEvidence -> commentaryBridge`

The current language-neutral annotation handoff path is:

`CandidateLineSelection -> CommentaryOutline.annotationSelections -> CommentaryOutlineBuilder -> CommentaryPlan.annotationSelections`

The current renderer-side annotation planning path is:

`CommentaryPlan -> BookAnnotationPlanner -> BookAnnotationPlan`

The current renderer-side line commentary skeleton path is:

`BookAnnotationPlan -> LineCommentaryPlanner -> LineCommentaryPlan`

The current renderer-side English line commentary path is:

`LineCommentaryPlan -> EnglishLineCommentaryWriter -> EnglishLineCommentary -> PublicPhrase -> PublicSurfaceTemplate -> CommentaryRenderer.render primary RenderText.publicText`

`CommentaryOutlineBuilder` copies selected prepared variation evidence without
adding meaning. `CommentaryRenderer` exposes only the public-safe render subset
and filters unsafe or malformed plan variation evidence. At this boundary the
internal `VariationEvidenceRole` is mapped to renderer-public `RenderLineRole`
keys and the prepared `proves` token is dropped from the public payload. The
analyse `commentaryBridge` may preserve that public render subset when it is
present and `surfaceAllowance = public_line`; `boundary_only` stays non-display
boundary evidence. The bridge and view may show SAN line notation only when the
rendered block's phrase capability allows `line_commentary`. They must not add
prose, role-label captions, run source lookup, execute Stockfish/WASM, or carry
raw candidate-line/probe/cache/debug fields, internal role keys, or prepared
proof tokens.

`CommentaryBackendSeam` may receive an internal
`CandidateProbeControlledAdapter.AssemblyResult` as an attach-only side input
that is not carried on `CommentaryPipelineInput` and is not visible to
`claimProvider`. The seam produces claims and applies engine-certification
fail-closed filtering before invoking the assembly provider. It then attaches
only already-lowered `PreparedVariationEvidence` to existing filtered claims by
matching `boundClaimId`; it does not consume raw `CandidateLineEvidence`, probe
payloads, cache keys, cache writes, branch ids, parent prefixes, or engine
fingerprints as selection input. Missing assembly or assembly without prepared
proofs leaves the existing backend path unchanged. `CommentaryBackendSeam`
serializes only `CommentaryRender`; internal debug fields remain unavailable
through the public response.

`CandidateLineSelection` is the post-outline line-explanation selector. It
consumes already selected claims/context and already public-safe
`PreparedVariationEvidence`; `ClaimSelector` uses its public result to fill
`CommentaryOutline.variationEvidence`. It does not create `CommentaryClaim`,
replay or normalize SAN, run probes, consult cache/source lookup, write prose,
or expose raw branch/cache/probe/debug handles. Strong primary line explanation
requires both a candidate-style proof and a defender-resource/defender-reply
proof bound to the same selected exact-board lead claim. Candidate-only proof
remains weak context and is not emitted as public outline, plan, renderer, or
backend variation evidence. The public set is the primary proof, companion
defender/resource proofs, and selected support/negative proofs only when a
strong primary exists. Source-context, retrieval, and raw-engine owned proofs
cannot own line selection. Source line-test refs from opening, motif,
endgame-study, or retrieval may annotate only proof ids already selected from
an exact-board claim; retrieval remains illustrative only. Failed tempting,
premature, and release-risk proofs are negative/support only and cannot become
the primary recommendation. If no safe line set exists, the selector returns
empty selection rather than a fallback one-move caption.
For the annotation slice, the selector also emits a strong-only
`PlanAnnotationSelection` summary. It carries claim id, primary proof id,
companion/support/negative proof ids, coarse `PlanAnnotationFrame` source
frames (`opening`, `motif`, `endgameStudy`, `retrieval`), raw source line-test
ref ids as metadata, `PlanAnnotationStrength.Strong`, and the selected wording
cap. It does not parse source ref strings into chess meaning, introduce prose
templates, or open renderer/API/frontend wiring. Candidate-only weak lines emit
no annotation handoff, and support/negative proof ids may appear only behind an
existing strong primary.
`PublicVariationEvidenceSafety` is the package-private selector-side helper
shared by `ClaimSelector`, `CandidateLineSelection`, and
`BookAnnotationPlanner` for public proof-id safety, prepared proof safety,
claim binding, and source line-test proof-id parsing. Internal
branch/cache/probe-shaped proof ids must fail the same way in claim selection,
source-context line-test admission, line selection, and renderer-side
annotation planning.

`BookAnnotationPlanner` consumes `CommentaryPlan` only. It may create a
`BookAnnotationUnit` only from an unblocked selected main exact-board admitted
claim with a same-claim strong `PlanAnnotationSelection`, a public-line
candidate/root primary proof, and at least one public-line defender-resource or
defender-reply companion proof. Support and negative proof ids may be carried
only on that strong unit. Candidate-only, defender-only, source-only,
retrieval-only, blocked, `boundary_only`, `internal_only`, raw-provenance,
unsafe-proof-id, missing-proof, and unmatched source-frame cases fail closed to
`BookAnnotationBoundary`. Retrieval frames are illustrative metadata and are
not authoritative. The planner emits structured SAN/UCI/resource/reply/proof
metadata and wording caps only; it does not emit final English prose, phrase
templates, controller/API fields, frontend UI, source lookup, Stockfish/WASM
execution, or cache persistence.

`LineCommentaryPlanner` consumes `BookAnnotationPlan` only. It may create
structured `LineNote` values for the main SAN line, an already-carried
defensive resource/reply line, and restrained line-result notes only from this
closed role/result table:

| `BookAnnotationUnit.proofRole` | `BookAnnotationUnit.testResult` | `LineNoteMeaning` |
| --- | --- | --- |
| `persistence` | `pressure_persists` | `pressure_persists` |
| `defender_resource` | `does_not_restore_counterplay` | `does_not_restore_counterplay` |
| `defender_resource` | `resource_fails` | `resource_fails` |
| `defender_resource` | `resource_works` | `resource_works` |
| `hold` | `defensive_hold` | `defensive_hold` |
| `simplification` | `simplifies` | `simplifies` |
| `conversion` | `simplifies` | `simplifies` |
| `conversion` | `converts` | `converts` |

Empty unit sets, wording caps below `qualified_support`, empty main lines,
unsupported results, and role/result mismatches do not create fallback notes.
Defensive-resource notes remain structural only and do not create result
meaning by themselves. Source frames remain metadata from the book annotation
layer and are not parsed into line meaning. They may attach only coarse
non-authoritative `LineContext` hints (`opening`, `pattern`, `endgame`,
`example`) to notes that already exist; source-only, authoritative, or
malformed frames must not create notes or source prose, and raw `sourceRefIds`
are not exposed. V7d carries support/caution line skeletons only from
already-admitted public-safe prepared support/negative lines behind an admitted
strong unit. The book annotation layer preserves only explicit SAN/UCI,
tested, reply, and resource line payloads in `supportingLines` /
`cautionLines`; missing detail line data fails closed for that detail. The line
planner may create `supporting_line` and `caution` notes only from those
structured details, never from support/negative ids alone. Caution skeletons
remain separate support-only notes, cannot become primary line/result notes,
and use no natural/tempting wording. The planner does not emit final English
prose, phrase templates,
controller/API fields, frontend UI, live source lookup, Stockfish/WASM
execution, cache persistence, or lower-layer meaning.

`EnglishLineCommentaryWriter` consumes `LineCommentaryPlan` only. It groups
notes by annotation id and emits at most one compact English comment per
annotation when a usable `main_line` note and compatible `line_result` note are
present. The writer uses only the closed `LineNoteMeaning` phrase table for
line results, may append support/caution sentences only behind an existing
main comment and only when the detail note is bound to the same primary proof
id as the main/result pair, ignores source contexts for prose, preserves SAN
tokens including Black-move ellipses, and emits no fallback caption for missing
main/result notes or unsupported meanings. It must not consume raw
`CommentaryClaim`, `PreparedVariationEvidence`, engine/PV, source rows,
probe/cache objects, or lower Object/Delta/Certification/Sxx state.

## Executable Validation

Executable validation lives in:

- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineEvidenceContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineNormalizerContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateProbeOrchestrationCacheContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateProbeControlledAdapterContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineProofCacheContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineAssemblyProviderContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateRootProbeIntegrationContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateChildProbeIntegrationContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineEvidenceLoweringPolicyTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/selection/PreparedVariationEvidenceContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/selection/CandidateLineSelectionContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/render/annotation/BookAnnotationPlannerContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/render/annotation/LineCommentaryPlannerContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/render/annotation/EnglishLineCommentaryContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/api/CandidateLinePipelineContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/api/CommentaryBackendSeamContractTest.scala`
- `ui/analyse/tests/commentaryBridge.test.ts`

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
- the probe orchestration/cache boundary requests root `MultiPV 3` at target
  depth `18` with floor `16`, creates child defender-resource requests for
  ranks `1` and `2` only by default, allows rank `3` child requests only by
  explicit expanded budget or revalidated strong/cache target-depth-20 cache
  hit, rejects shallow/stale/invalid-rank roots before child scheduling,
  requires a main-candidate defender resource before strong line explanation
  promotion, rejects invalid root assembly while preserving valid roots when a
  child fails, binds cache keys to
  FEN/variant/node/ply/engine/depth/MultiPV/role/parent-prefix/parent-root-rank/freshness,
  rejects stale or mismatched cache hits, prevents source/retrieval proof-cache
  population, and keeps probe/cache internals out of public JSON
- the controlled adapter converts accepted root and child probe payloads or
  revalidated cache hits into handoff packets, emits provider-owned cache
  receipts only for accepted completed probe results, lets valid cache hits
  substitute for missing child probes, keeps rank `3` child cache reuse behind the
  strong/cache target-depth-20 or explicit-expanded-budget gate, preserves
  valid root evidence when a child fails, rejects source/retrieval cache
  entries as proof owners, and keeps raw probe/cache internals out of public
  backend/render JSON
- the proof-cache facade stores only through provider/adapter-owned receipts
  for accepted probe-origin candidates, reads them back as typed lookup results
  only after existing key/entry revalidation, rejects
  source/retrieval/revalidated-cache and forged writes before storage, fails
  closed for stale or mismatched entries, preserves the rank-3 target-depth-20
  cache rule, does not expose arbitrary candidate writes, and does not rewrite
  cache hits as writes
- the internal-only assembly provider requires exact current context, engine
  fingerprint, budget, typed completed probe inputs, and an optional internal
  proof-cache facade, derives root/child requests itself, reads cache hits only
  through that facade, delegates to the controlled adapter, preserves root `MultiPV 3`, child
  `MultiPV 2`, rank-3 child guardrails, emits prepared evidence only with an
  explicit binding, writes proof-cache entries only through the returned
  provider/adapter-owned receipt, and defers Stockfish/WASM execution,
  production cache persistence, controller wiring, product frontend UI, and
  prose
- the root probe integration boundary accepts a valid completed root payload
  or valid proof-cache root hit, produces three public-line-eligible root
  evidences, exposes unsatisfied rank `1` and `2` child defender-resource
  requests at child `MultiPV 2`, emits prepared evidence only when an explicit
  lowering binding is supplied, does not rewrite cache hits as writes, fails
  closed for wrong FEN/node/ply/variant/engine, stale, depth-15, root
  MultiPV/count 2 or 4, duplicate first moves, malformed or illegal PV, and
  source/retrieval/root-hint provenance, and keeps raw root payloads and
  internal branch ids out of public backend/render JSON
- the child probe integration boundary accepts valid completed child payloads
  or proof-cache child hits only when they match the internally planned child
  requests, returns satisfied and unsatisfied child request sets, preserves
  valid root evidence when a child is missing or invalid, rejects rank `3` live
  child payloads by default while preserving the strong depth-20 cache path,
  emits child cache writes only for accepted completed child probes, lowers
  child evidence only with explicit binding, and keeps child probe/cache
  internals out of public backend/render JSON
- the lowering policy admits depth `16`, rejects depth `15`, requires root
  MultiPV `3`, rejects root MultiPV below or above the hard cap, lowers root
  ranks `1/2/3` as main/alternative/context line evidence, lowers at most two
  child defender replies only at child MultiPV `2` for eligible top-two root
  candidates, ignores rank `3` child evidence unless explicitly allowed,
  rejects duplicate root ranks or MultiPV indexes, rejects source-hint,
  raw-engine/source-context/unbound binding provenance, non-public, failed,
  premature, and release-risk candidate evidence, keeps public proof ids free
  of internal branch ids, and keeps raw/internal candidate-line fields out of
  public backend/render JSON
- safe strong prepared variation evidence survives selection into outline and
  plan
- cross-family line selection promotes a strong primary line only from a
  selected exact-board claim with both candidate and defender-resource proof,
  filters public outline/plan/render/backend variation evidence to the selected
  strong set, keeps candidate-only proof weak/context and non-public, blocks
  source/raw-engine proof ownership, allows source line-test refs only as
  annotations of already selected exact-board proof ids, carries the
  language-neutral annotation handoff through outline and plan without reranking
  or source-string chess parsing, keeps retrieval illustrative, keeps failed
  tempting/premature/release-risk proof
  negative/support only, returns empty selection when no safe line set exists,
  and strips internal debug handles from its public model result
- renderer-side annotation planning consumes only `CommentaryPlan`, creates a
  structured unit only for a strong exact-board selected primary with
  candidate/root and defender-resource proof, records fail-closed boundaries
  for candidate-only, defender-only, blocked, unsafe, internal, source-only,
  and unmatched frame cases, keeps retrieval frames illustrative, prevents
  failed tempting/premature/release-risk primary units, and emits no final
  English prose or phrase fields
- renderer-side line commentary skeleton planning consumes only
  `BookAnnotationPlan`, emits structured main-line, defensive-resource, and
  restrained line-result notes without final prose fields, and stays silent
  for empty units, low wording caps, empty lines, and unsupported results
- renderer-side English line commentary consumes only `LineCommentaryPlan`,
  emits compact primary comments from a closed phrase table, appends
  support/caution sentences only behind an existing main comment, keeps
  unsupported/caution-only/support-only inputs silent, and keeps forbidden
  terms plus source/internal/proof ids out of public comment text
- internal backend candidate-line assembly carries prepared variation evidence
  from the controlled adapter into backend render output only as public
  `RenderVariationEvidence` attached to an existing matching claim
- the internal candidate-line pipeline accepts sanitized root `MultiPV 3`
  payloads plus child `MultiPV 2` defender-resource payloads for the first two
  root candidates, normalizes them to SAN from exact FENs, lowers them through
  the public-safe binding, attaches them to a real backend claim, and keeps
  stale, shallow, illegal, duplicate-root, wrong-node, source-owned, and
  mismatched-binding cases quiet
- the minimal frontend bridge preserves public `RenderVariationEvidence` when
  present, including structured backend line blocks that have public evidence
  ids but no prose text yet, and omits raw candidate-line/probe/cache fields
- public analyse input does not send completed-probe payloads to the commentary
  route; completed root/child payloads remain internal provider inputs only
  and outside `buildCommentaryRequest`, the public controller/API route, and
  frontend SAN authority. The optional non-production local-probe adapter may
  run local Stockfish/ceval to produce root `MultiPV 3` and child `MultiPV 2`
  UCI payloads, but those payloads enter only through the internal local-probe
  transport and are replayed/normalized by the backend before any public
  `RenderVariationEvidence` exists
- claim production cannot observe the internal candidate-line assembly side
  input
- claim production and engine-certification fail-closed filtering happen before
  the candidate-line assembly provider runs
- missing internal candidate-line assembly, assembly without prepared evidence,
  mismatched binding, and assembly with no claims do not create public line
  output or new claims
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
