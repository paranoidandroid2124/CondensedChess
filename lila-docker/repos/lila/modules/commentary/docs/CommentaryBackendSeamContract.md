# Commentary Backend Seam Contract

This document freezes the first backend request/response seam for structured
commentary output.

It does not open frontend wiring, polished product UI, live source integration,
or model-authored prose generation.

Short form: no frontend wiring, no source live integration.

## Request

`CommentaryRequest` is the stable backend intake shape:

- `currentFen`
- optional `beforeFen`
- optional `playedMove`
- `nodeId`
- `ply`
- optional `enginePacket`
- optional `debug` field is ignored by the public render entrypoint

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

The request does not accept raw selector claims, raw source rows, opening
fixture JSON, or `OpeningContextCandidate` JSON from the frontend.
It also does not accept candidate-line multi-packet handoff JSON. The sanitized
candidate-line handoff is an internal `lila.commentary.line` boundary only and
is not part of `CommentaryRequest`.

## Response

`CommentaryResponse` is the stable backend output shape:

- `status`
- `render`
- `noCommentary`
- optional `internal`

`render` is a `CommentaryRender` JSON payload using the frozen renderer
contract: public blocks, evidence refs, boundaries, wording, and render status.
Frontend consumers must not rerank, reinterpret, revive blocked claims, merge
source vectors, infer evidence, or upgrade wording strength from this payload.
If public-safe prepared variation evidence is present, the response carries
only the renderer-owned `RenderVariationEvidence` subset. Internal
`PreparedVariationDebug`, raw engine packets, raw PV details, source rows, and
raw source snippets remain non-public.
Public-safe defender-resource, failed-tempting-move, release-risk, hold,
conversion, simplification, and persistence evidence may cross the seam only
as bounded structured line-proof fields. The seam does not convert them into
book prose, recommendations, best-defense claims, forced-line claims, or
result claims.
Internal candidate-line root/child packets, branch probes, parent prefixes,
raw UCI PV input, engine fingerprints, and child packet debug fields do not
cross the public seam.
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
exact-board producer with an empty sanitized higher-evidence handoff. A private
`EvidenceClaimHandoff` may carry already-bounded `CertificationExtraction`,
typed `StrategyProjectionAdmissionResult` values, legacy projection claim
candidates, and normalized `SourceContextCandidate` values. It is not a
frontend/request payload and it is not a live source lookup path.
The private `CandidateLinePacketHandoff` sits outside this claim-production
handoff for now: it can normalize internal root/child packet evidence into
`CandidateLineEvidence`, but the backend seam does not yet consume it, lower it
to `PreparedVariationEvidence`, or expose it through controller/frontend
transport.
Certification claims are emitted only from same-current-root certified
extractions with same-binding exact-board support and a non-result public
family. Projection claims are emitted only from admitted
`StrategyProjectionAdmissionResult` values produced by
`StrategyProjectionAdmission.admit`; the result must bind the source root to
the current root, carry same-binding exact lower carrier refs, expose only
allowed projection evidence kinds, keep an exact scope, and remain under the
qualified wording cap. Legacy claim-shaped projection candidates remain
fail-closed and ignored. Source context is converted only through
`SourceContextClaimBoundary` and remains context/support only. Raw engine
packets, raw source rows, prepared variation evidence, Sxx labels, broad chess
concepts, and candidate lines still cannot create claims at this producer
boundary.

Explicit injected `claimProvider` instances still override the default producer
for contract tests and future controlled producer paths. The default producer
emits Delta claims only when the transition after-state is the same root state
as the supplied current extraction; stale or cross-board delta input is
suppressed at the producer boundary. Claim production receives only sanitized
exact-board extraction, optional transition extraction, node identity,
certification-intake status metadata, and optional private sanitized handoff;
it does not receive the raw request object, raw played-move string, raw source
row, or raw engine packet. This avoids broad fallback narration while
preserving the stable request/response shape. The seam still composes the
current frozen selector, outline builder, and renderer contracts.

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
result by canonical id, owner, and anchor.

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

- `modules/commentary/src/main/scala/lila/commentary/api/CommentaryBackendSeam.scala`
- `modules/commentary/src/test/scala/lila/commentary/api/CommentaryBackendSeamContractTest.scala`

The scaffold validates:

- valid exact-board request returns structured `CommentaryRender`
- malformed FEN fails closed
- stale or wrong-node engine packet does not render engine evidence
- absent, rejected, or accepted-with-empty engine intake cannot unlock
  engine-certified claims
- accepted engine intake can carry engine-certified claims only through matching
  bounded evidence refs
- source/opening context remains context-only
- public-safe prepared variation evidence survives only as structured public
  line-proof fields
- defender-resource and failed-tempting-move evidence preserve role/result
  shape without leaking internal proof packets or becoming truth owners
- internal/debug variation fields do not leak into public or debug responses
- internal candidate-line root/child packets and raw PV fields do not leak into
  public backend/render JSON
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
