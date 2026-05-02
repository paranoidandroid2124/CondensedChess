# Commentary Backend Seam Contract

This document freezes the first backend request/response seam for structured
commentary output.

It opens the narrow public backend controller transport at
`POST /api/commentary/render` and a separate non-production local-probe
transport at `POST /internal/commentary/render-local-probe`. It does not open
live source integration, production cache persistence, public completed-probe
transport, or model-authored prose generation.

Short form: public route stays narrow; local Stockfish probe payloads are
internal/non-production handoff only; no source live integration.

## Request

`CommentaryRequest` is the stable backend intake shape:

- `currentFen`
- optional `beforeFen`
- optional `playedMove`
- `nodeId`
- `ply`
- optional `enginePacket`
- optional `debug` field is ignored by the public render entrypoint

The public transport accepts only this request shape.
`CommentaryPublicJsonTransport` normalizes JSON field names by lowercasing and
removing separators before matching. Its guard rejects completed-probe,
root/child probe, probe request, candidate-line assembly, branch id, parent
id/prefix, cache key, source row, caller-supplied/internal proof id/proves,
and raw probe/cache-shaped fields before `CommentaryRequest` decoding. A
caller cannot smuggle those fields through separator or case variants at top
level or nested inside `enginePacket`; they are not interpreted as future
transport hints and do not appear in bad-request JSON.

`currentFen` is mandatory and must parse through the existing exact-board
fail-closed extraction path. `beforeFen` and `playedMove` are all-or-nothing:
if one is supplied without the other, the request is invalid. When both are
supplied, the transition must parse through the existing fail-closed delta
path and the request `currentFen` is the after-position.

`enginePacket` is a typed
`CertificationEngineRuntimeIntake.RuntimeEnginePacket`. It is accepted only as
runtime input to the existing certification intake boundary. The backend seam
does not accept `EngineEvidencePacket` directly and does not expose raw engine
PV, eval, mate score, centipawns, or engine labels as render evidence.
Valid runtime-intake fields such as `engineConfigFingerprint` and `pvLines`
remain accepted by the public transport guard as typed certification intake;
candidate-line `engineFingerprint`, raw PV/line/probe fields, probe/cache
fields, branch/parent ids, source rows, internal payloads, and caller proof
fields remain rejected request input.

The request does not accept raw selector claims, raw source rows, opening
fixture JSON, or `OpeningContextCandidate` JSON from the frontend.
It also does not accept candidate-line multi-packet handoff JSON. The sanitized
candidate-line handoff is an internal `lila.commentary.line` boundary only and
is not part of `CommentaryRequest`. Internal candidate probe plans, probe
requests, cache keys, cache entries, parent prefixes, and expanded-budget flags
are likewise not request fields. Controlled adapter probe-result payloads,
cache lookup results, and cache write candidates are also internal-only and are
not request fields. `CandidateLineProofCache` facade handles and
`CandidateLineAssemblyProvider` input are likewise internal-only and are not
accepted from request JSON. `CandidateRootProbeIntegration` input/result
objects are also internal-only and are not accepted from request JSON.
`CandidateChildProbeIntegration` input/result objects are also internal-only
and are not accepted from request JSON.
Caller-supplied public `completedProbe` payloads are likewise not accepted by
the public route. Completed root/child probe payloads can enter only through
server-owned internal provider boundaries after exact request binding,
including the non-production local-probe transport that wraps a clean
move request subset (`currentFen`, `nodeId`, `ply`, and optional paired
`beforeFen` / `playedMove` only) beside a completed-probe payload and then
calls `CommentaryBackendSeam.renderInternal`. Nested `enginePacket`, `debug`,
and future public transport fields are rejected before request decoding.

## Response

`CommentaryResponse` is the stable backend output shape:

- `status`
- `render`
- `noCommentary`
- optional `internal`

`render` is a `CommentaryRender` JSON payload using the frozen renderer
contract: public blocks, block phrase capability, evidence refs, boundaries,
wording, and render status.
Frontend consumers must not rerank, reinterpret, revive blocked claims, merge
source vectors, infer evidence, or upgrade wording strength from this payload.
`RenderText.publicText` may be present only after the renderer's
`PublicSurfaceTemplate` consumes a closed `PublicPhrase`; role labels,
evidence ids, and raw `PublicClaim.text` are not public prose.
If public-safe prepared variation evidence is present, the response carries
only the renderer-owned `RenderVariationEvidence` subset. Internal
`PreparedVariationDebug`, raw engine packets, raw PV details, source rows, and
raw source snippets remain non-public.
Renderer-owned public `RenderVariationEvidence.proofId` may remain in the
response as the stable public line-proof identifier. This does not allow
caller-supplied or internal request `proofId` / `proves` fields, and the lower
prepared `proves` token is not public response payload.
Public-safe defender-resource, failed-tempting-move, release-risk, hold,
conversion, simplification, and persistence evidence may cross the seam only
as bounded structured line-proof fields. The seam does not convert them into
book prose, recommendations, best-defense claims, forced-line claims, or
result claims.
Internal candidate-line root/child packets, branch probes, parent prefixes,
raw UCI PV input, engine fingerprints, cache keys, cache entries, probe plan
requests, and child packet debug fields do not cross the public seam.
Opening sequence context may cross the seam only as source-context evidence
refs and selected context metadata. Raw opening source rows, move-order rows,
move-stat rows, frequencies, and source snippets are not response fields.
`opening-line-test:*:context` may identify a contextual link to a public-safe
line proof, but the proof must be rendered from the exact-board claim's
`RenderVariationEvidence`, not from the source row.
Motif line context follows the same separation. `motif-line-context:*` and
`motif-line-test:*:context` may cross only as source-context evidence refs
after detector carrier parity. Raw motif rows, detector packets, source tags,
and internal proof/debug packets are not response fields. The seam does not
convert motif context into threat prose, forcing prose, recommendations, mate,
perpetual, result, best-move, or engine claims.
Endgame technique context follows the same separation. `endgame-technique:*`
and `endgame-line-test:*:context` may cross only as source-context evidence
refs after exact study applicability. Raw endgame rows, placement evidence,
relation evidence, candidate plans, outcome fields, tablebase-style verdicts,
and internal proof/debug packets are not response fields. The seam does not
convert endgame context into win, draw, loss, forced-conversion,
tablebase/Syzygy/WDL/DTZ/DTM, oracle, recommendation, best-move, or engine
claims.
Retrieval illustration context follows the same separation.
`retrieval-illustration:*` and `retrieval-line-test:*:context` may cross only
as source-context evidence refs. Raw retrieval rows, snippets, player/event/
result metadata, game URLs, citation metadata, display candidates, and
internal proof/debug packets are not response fields. The seam does not
convert retrieved examples into current-position proof, recommendations,
verdicts, game-result evidence, best-move, forced-line, theory, oracle, or
engine claims.

`CommentaryResponseStatus` values are:

- `rendered`
- `contextOnly`
- `noCommentary`
- `invalidRequest`

For valid requests, `status` mirrors `render.status`. Invalid exact-board
input returns `invalidRequest` plus a silent `CommentaryRender` with
`RenderStatus.NoCommentary`.

## Public And Internal Metadata

The default response is public-safe.

The public controller calls only `CommentaryPublicJsonTransport.renderJson`,
which parses `CommentaryRequest` and calls `CommentaryBackendSeam.render`; it
does not call `renderDebug`. Caller-supplied request `debug` is ignored by the
public route and never enables `internal`.

The local-probe controller calls only `CommentaryLocalProbeJsonTransport` and
is gated to non-production mode. Its request wrapper is not a public
`CommentaryRequest` extension: the public transport still rejects the same
`completedProbe` shape. The local-probe transport returns the same public
`CommentaryResponse` shape only when public local-probe line evidence is tied
to a visible block; otherwise it returns silent `noCommentary`. It does not
accept nested `enginePacket` or `debug`, and it does not expose raw root/child
packets, branch ids, engine fingerprints, cache keys, or debug metadata.
In production mode, the controller rejects the route through an empty-body
`Open` action before the JSON body parser is installed.

Public `render.suppressions` is empty by default. Blocked do-not-say material,
suppression reasons, rejected raw-engine shortcuts, and invalid-input reasons
may appear only in `internal` through the server-owned debug entrypoint
(`renderDebug`). Caller-supplied request `debug` does not enable internal
metadata on the public `render` entrypoint.

`CommentaryInternalMetadata` may carry:

- non-public `RenderSuppression` entries
- certification runtime intake status for a supplied `RuntimeEnginePacket`
- invalid request reason

Internal metadata is diagnostic only. It is not a public commentary source and
cannot be used by a frontend to revive blocked claims.
Engine intake reasons are sanitized reason keys only; raw engine validation
strings, eval values, PV details, mate scores, centipawns, depth diagnostics,
and engine configuration details are not serialized in the response.
Prepared variation debug fields are treated the same way: they are not
serialized in public `render`, and the current internal metadata shape does not
expose them either.

## Pipeline Seam

`CommentaryBackendSeam` owns the first stable backend composition boundary:

`exact-board intake -> optional certification runtime intake -> evidence claim producer -> selection -> outline builder -> renderer`

The seam validates `currentFen` through fail-closed strategic-object
extraction. Optional transition input validates through fail-closed delta
extraction and rejects full-FEN clock mismatches rather than accepting
root-equivalent stale endpoints. Optional engine input is normalized only through
`CertificationEngineRuntimeIntake`; stale, wrong-node, wrong-FEN, shallow,
incomplete, malformed, or illegal-PV packets are rejected there and are not
converted into public render evidence.

The first exact-board slice is `ExactBoardClaimProducer`. It is deliberately
narrow: it may produce only bounded Object claims from sanitized current
exact-board extraction and bounded Delta claims from sanitized transition
extraction. It does not consume source rows, raw engine packets, raw engine
PV/eval, projection evidence, prepared variation evidence, or candidate lines.

The default seam now calls `EvidenceClaimProducer`, which composes the
exact-board producer with a sanitized higher-evidence handoff. If optional
engine input is accepted by `CertificationEngineRuntimeIntake`, the resulting
same-root `CertificationEvidenceBundle` may be extracted into a
`CertificationExtraction` and passed through that handoff. Rejected, stale,
wrong-node, wrong-FEN, illegal-PV, or empty engine intake contributes no
higher claim. A private `EvidenceClaimHandoff` may carry already-bounded
`CertificationExtraction`,
typed descriptor-certified `StrategyProjectionAdmissionResult` values, legacy
projection claim candidates, and normalized `SourceContextCandidate` values.
Legacy validation-scaffold projection admissions are not public-lowerable. It is not a
frontend/request payload and it is not a live source lookup path.
The private `CandidateLinePacketHandoff` stays outside claim production: it can
normalize internal root/child packet evidence into `CandidateLineEvidence`, but
candidate-line evidence still cannot create `CommentaryClaim`.
The private `CandidateProbePlan` / cache / controlled-adapter boundary also
sits outside the public seam. It can plan root and child candidate probe
requests, revalidate cache hits, convert completed internal probe/cache
payloads into `CandidateLinePacketHandoff`, return cache write candidates for a
caller to store later, and lower sanitized line evidence to
`PreparedVariationEvidence` only when given an explicit same-claim binding. It
does not execute Stockfish, call frontend WASM, perform live source lookup,
persist cache entries, create claims, write prose, or add request/response
fields.
`CandidateLineProofCache` is the internal proof-cache facade for this path. It
is not a public seam field, not the generic eval/source/retrieval cache, and
not a production persistence backend. It reads only by exact candidate probe
request/key binding and returns `CandidateProbeCacheLookupResult` after
existing key/entry revalidation. It writes only probe-origin
entries through the controlled adapter/provider-owned receipt path, does not
accept arbitrary caller-constructed `CandidateProbeCacheWriteCandidate` values,
rejects source/retrieval/revalidated-cache or forged writes before storage, and
does not rewrite cache hits as writes. Production persistence remains deferred.
`CandidateLineAssemblyProvider` is the internal-only provider boundary around
that plan/cache/adapter path. It accepts exact current context, engine
fingerprint, budget, typed completed probe payloads, an optional internal
proof-cache facade for cache lookup, and an optional explicit lowering binding;
it derives root and child requests itself, reads valid cache hits through the
facade when present, does not expose direct root/child cache-hit input fields,
delegates to `CandidateProbeControlledAdapter`, and writes back only through
the returned provider/adapter-owned cache receipt. It returns only a
`CandidateProbeControlledAdapter.AssemblyResult`. It does not execute
Stockfish, call WASM, perform live source lookup, does not add production cache
persistence, does not wire controller routes, does not open product frontend
UI, does not create claims, does not write prose, and does not expose raw
probe/cache/PV internals through the public seam.
`CandidateRootProbeIntegration` is the internal-only root probe integration
boundary for completed root probe payloads and valid proof-cache root hits. It
derives the root request from exact current FEN/node/ply/variant plus the
server-owned engine fingerprint, delegates to
`CandidateLineAssemblyProvider`, and returns only internal assembly plus
unsatisfied child defender-resource probe requests. The default child requests
are limited to accepted root ranks `1` and `2`, child `MultiPV 2`, target
depth `18`, and floor `16`; rank `3` is not scheduled by the default root
integration path. It does not execute Stockfish/WASM, accept public JSON, wire
controllers or product UI, create claims, write prose, or expose raw root
payloads, branch ids, parent prefixes, engine fingerprints, cache keys, or
probe internals through the seam. Invalid or source/retrieval-owned root input
fails closed to no child requests, no prepared evidence, and no cache write.
`CandidateChildProbeIntegration` is the internal-only child probe integration
boundary for the root stage's planned child work. It consumes exact current
context, a valid completed root payload or proof-cache root hit, completed
child probe payloads, and optional proof-cache facade reads only through the
existing provider, controlled adapter, normalizer, packet handoff, proof-cache,
and lowering boundaries. It returns internal assembly plus satisfied and
unsatisfied child requests. Default live child requests remain root ranks `1`
and `2` only at child `MultiPV 2`, target depth `18`, and floor `16`; rank `3`
direct child payloads remain rejected by default while strong target-depth-20
cache hits may still satisfy the existing cache-only path. Child payloads must
match the internally planned request for FEN, node, ply, parent branch/prefix,
parent root rank, role, target/floor depth, engine fingerprint, freshness,
completion, legal replay, and complete child rank/MultiPV policy. Invalid
child input is local: valid root evidence remains, the child is omitted, no
child proof lowers, no child cache write is committed, and the corresponding
request remains unsatisfied. It does not execute Stockfish/WASM, accept public
JSON, wire controllers or product UI, create claims, write prose, or expose
raw child payloads, parent prefixes, branch ids, engine fingerprints, cache
keys, cache writes, provider names, or adapter internals through the seam.
The backend seam has a private internal candidate-line assembly side handoff.
A server-owned `CandidateProbeControlledAdapter.AssemblyResult` may be supplied
internally only after normal claim production has run and engine-certification
fail-closed filtering has been applied; it is not stored on
`CommentaryPipelineInput`, is not visible to `claimProvider`, and cannot mutate
provider-visible state before claims are produced. The seam consumes only its
already-lowered `PreparedVariationEvidence` and attaches those proofs to
existing filtered produced or injected claims whose ids match `boundClaimId`.
Missing assembly, empty prepared evidence, or unmatched proof ids leave claim
production and rendering unchanged. Raw `CandidateLineEvidence`, probe
payloads, cache entries, cache keys, branch ids, parent prefixes, engine
fingerprints, and cache write candidates are not consumed by selection and do
not cross the public seam.
Certification claims are emitted only from same-current-root certified
extractions with same-binding exact-board support and a non-result public
family. Projection claims are emitted only from admitted
`StrategyProjectionAdmissionResult` values produced by the descriptor-certified
runtime path (`StrategyProjectionAdmissionProducer`) and marked with
`DescriptorCertifiedRuntime` authority; the result must bind the source root to
the current root, carry same-binding exact lower carrier refs, expose only
allowed projection evidence kinds, keep an exact scope, and remain under the
qualified wording cap. Legacy claim-shaped projection candidates and
legacy-validation-scaffold admissions remain fail-closed and ignored. Source context is converted only through
`SourceContextClaimBoundary` and remains context/support only. Raw engine
packets, raw source rows, prepared variation evidence, Sxx labels, broad chess
concepts, candidate lines, and candidate-line assembly results still cannot
create claims at this producer boundary.

Explicit injected `claimProvider` instances still override the default producer
for contract tests and future controlled producer paths. The default producer
emits Delta claims only when the transition after-state is the same root state
as the supplied current extraction; stale or cross-board delta input is
suppressed at the producer boundary. Claim production receives only sanitized
exact-board extraction, optional transition extraction, node identity,
certification-intake status metadata, and optional private sanitized handoff;
it does not receive the raw request object, raw played-move string, raw source
row, raw engine packet, or candidate-line assembly result. This avoids broad
fallback narration while preserving the stable request/response shape. The seam
still composes the current frozen selector, outline builder, and renderer
contracts.

## Boundary Rules

The backend seam must not:

- expose raw engine as rendered evidence
- create claims from accepted engine intake status alone
- expose source context as current-position truth
- preserve or publish blocked claims as public blocks
- upgrade `wordingStrengthCap`
- accept raw selector claims from the request
- accept `RuntimeEnginePacket` as anything other than certification runtime
  intake
- accept `EngineEvidencePacket` directly
- accept raw source rows as truth
- expose raw motif source rows, detector rows, or internal motif proof packets
- expose raw endgame-study rows, placement/relation evidence, outcome fields,
  or internal endgame proof packets
- expose raw retrieval rows, snippets, player/event/result metadata, citation
  metadata, display candidates, or internal retrieval-linked proof packets
- accept or serialize candidate-line root/child packet handoff fields
- accept or serialize candidate probe plan/cache internals
- accept or serialize controlled adapter probe payloads, cache lookup payloads,
  or cache write candidates
- accept or serialize `CandidateLineProofCache` facade handles
- accept or serialize `CandidateLineAssemblyProvider` input
- accept or serialize `CandidateRootProbeIntegration` input or result fields
- accept or serialize `CandidateChildProbeIntegration` input or result fields
- merge `master_reference` and `online_trend` opening rankings
- create best-move, theory-truth, forced-line, result, winning, drawing, or
  oracle wording
- require a frontend to rerank or reinterpret the payload

`OpeningContextCandidate` remains structured context only. Aliases are display
labels only. Move statistics remain source statistics/context references only.
Specific game, player, event, and URL citation remains retrieval-lane material.

Engine E can be mentioned or carried only after bounded Certification evidence
is already admitted by the backend pipeline and then survives renderer
evidence filtering. The seam does not render raw eval, PV, centipawn, mate, or
engine search fields. An accepted engine packet is not sufficient by itself:
`EngineCertification` refs may pass the seam only when they match a bounded
engine evidence ref produced by the accepted `CertificationEngineRuntimeIntake`
result by canonical id, owner, anchor, route, scope, and required purpose set.

## noCommentary

When no claim is admitted, when public render sections are empty, or when
input is invalid, the response is silent:

- no public blocks
- no public evidence refs
- no fallback broad chess narration
- `noCommentary = true`

Debug mode may carry the internal reason separately, but the public render
remains silent.

## Executable Validation

Executable validation lives in:

- `app/controllers/Commentary.scala`
- `src/test/scala/controllers/CommentaryTest.scala`
- `modules/commentary/src/main/scala/lila/commentary/api/CommentaryPublicJsonTransport.scala`
- `modules/commentary/src/main/scala/lila/commentary/api/CommentaryBackendSeam.scala`
- `modules/commentary/src/test/scala/lila/commentary/api/CommentaryPublicJsonTransportContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineProofCacheContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateLineAssemblyProviderContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateRootProbeIntegrationContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/line/CandidateChildProbeIntegrationContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/api/CandidateLinePipelineContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/api/CommentaryBackendSeamContractTest.scala`

The scaffold validates:

- valid exact-board request returns structured `CommentaryRender`
- the public `POST /api/commentary/render` route maps to the thin controller
- the public transport decodes only `CommentaryRequest` JSON and calls the public
  `render` entrypoint
- caller-supplied `debug = true` does not expose `internal`
- public transport bad-request JSON is sanitized and does not echo missing fields,
  parse errors, or blocked probe/cache/source internals
- probe/cache/internal-shaped fields, including separator and case variants at
  top level or nested under `enginePacket`, are rejected before they can be
  silently reinterpreted
- valid typed `RuntimeEnginePacket` JSON containing `engineConfigFingerprint`
  and `pvLines` is not rejected by the public transport guard when it carries
  no public caller-supplied certification claims
- public transport rejects caller-supplied `enginePacket.claims` and rejects
  baseline packets unless the public request itself is transition-bound
- malformed FEN fails closed
- stale or wrong-node engine packet does not render engine evidence
- absent, rejected, or accepted-with-empty engine intake cannot unlock
  engine-certified claims
- accepted engine intake can carry engine-certified claims only through matching
  bounded evidence refs with full id, owner, anchor, route, scope, and required
  purpose binding
- source/opening context remains context-only
- public-safe prepared variation evidence survives only as structured public
  line-proof fields
- the internal-only candidate-line assembly provider derives controlled
  adapter inputs from exact context plus completed typed probe/cache payloads,
  rejects public JSON input shape, and defers execution, cache persistence,
  controller wiring, product frontend UI, and prose
- the internal-only root probe integration boundary accepts valid completed
  root payloads or proof-cache root hits, returns rank `1` and `2` child
  requests only by default, emits prepared evidence only with explicit binding,
  rejects invalid root payloads fail-closed, prevents source/retrieval/root
  hint provenance from owning root proof, does not rewrite cache hits as
  writes, and keeps raw root payloads and internal branch ids out of public
  backend/render JSON
- the internal-only child probe integration boundary accepts completed child
  payloads or proof-cache child hits only when they match the root stage's
  planned child requests, returns satisfied and unsatisfied request sets,
  preserves valid root evidence when children are missing or invalid, rejects
  default rank `3` live child payloads while preserving strong depth-20 cache
  reuse, emits child writes only for accepted completed child probes, and keeps
  child probe/cache internals out of public backend/render JSON
- completed-probe helper payloads are accepted only when every supplied root
  and child request matches the server-derived request key for FEN, node, ply,
  role, parent branch/prefix, requested/target/floor depth, MultiPV, engine
  fingerprint, and policy binding; forged, missing, or per-line MultiPV
  mismatches fail closed before prepared evidence is attached
- internal candidate-line assembly can attach already-lowered prepared
  variation evidence to matching existing claims without creating claims
- the proof-cache facade stores only probe-origin adapter/provider write
  candidates, revalidates reads through the existing key/entry logic, rejects
  source/retrieval/revalidated-cache and forged writes, preserves the rank-3
  target-depth-20 cache rule, and keeps cache hits from emitting new writes
- sanitized candidate root `MultiPV 3` payloads and first-two-root child
  defender-resource `MultiPV 2` payloads can pass through the controlled
  adapter, exact-FEN SAN normalization, lowering, backend seam attachment,
  selection, outline, renderer, and public response only as bound
  `RenderVariationEvidence`
- public `RenderVariationEvidence` carries SAN-only public line fields and
  omits start FEN, UCI PV, provenance refs, replay/freshness boundary fields,
  depth, MultiPV, branch ids, cache keys, engine fingerprints, and raw move UCI
- claim production cannot observe candidate-line assembly
- claim production runs before the candidate-line assembly provider
- missing assembly or assembly without prepared evidence leaves existing
  backend behavior unchanged
- mismatched prepared variation evidence from the internal assembly is
  suppressed before public render
- defender-resource and failed-tempting-move evidence preserve role/result
  shape without leaking internal proof packets or becoming truth owners
- internal/debug variation fields do not leak into public or debug responses
- internal candidate-line root/child packets and raw PV fields do not leak into
  public backend/render JSON
- internal candidate probe plan/cache fields do not leak into public
  backend/render JSON
- internal controlled adapter probe payload, cache lookup, and cache write
  fields do not leak into public backend/render JSON
- no admitted or safe exact-board claim returns `noCommentary`
- blocked/internal metadata is hidden by default
- debug/internal mode is explicitly separate and server-owned
- JSON request/response round trip
- default exact-board claim production for material/piece-inventory,
  immediate-check, last-move transition, capture transition, and pawn
  transition facts
- stale full-FEN transition clocks and cross-board producer delta input cannot
  create Delta claims
- engine packet alone does not create a claim
- typed admitted projection results can create bounded Projection claims, while
  rejected results, Sxx labels, claim-shaped candidates, missing/wrong lower
  carriers, broad concept scopes, and forbidden wording remain fail-closed
