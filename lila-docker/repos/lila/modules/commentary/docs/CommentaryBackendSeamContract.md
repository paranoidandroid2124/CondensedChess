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

## Pipeline Seam

`CommentaryBackendSeam` owns the first stable backend composition boundary:

`exact-board intake -> optional certification runtime intake -> selection -> outline builder -> renderer`

The seam validates `currentFen` through fail-closed strategic-object
extraction. Optional transition input validates through fail-closed delta
extraction. Optional engine input is normalized only through
`CertificationEngineRuntimeIntake`; stale, wrong-node, wrong-FEN, shallow,
incomplete, malformed, or illegal-PV packets are rejected there and are not
converted into public render evidence.

The first implementation keeps claim production injectable inside the backend
module and defaults to no claims. Claim production receives only sanitized
exact-board extraction, optional transition extraction, node identity, and
certification-intake status metadata; it does not receive the raw request
object, raw played-move string, or raw engine packet. This avoids broad fallback narration while
preserving the stable request/response shape for the future producer path. The
seam still composes the current frozen selector, outline builder, and renderer
contracts.

## Boundary Rules

The backend seam must not:

- expose raw engine as rendered evidence
- expose source context as current-position truth
- preserve or publish blocked claims as public blocks
- upgrade `wordingStrengthCap`
- accept raw selector claims from the request
- accept `RuntimeEnginePacket` as anything other than certification runtime
  intake
- accept `EngineEvidencePacket` directly
- accept raw source rows as truth
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
- no admitted claim returns `noCommentary`
- blocked/internal metadata is hidden by default
- debug/internal mode is explicitly separate and server-owned
- JSON request/response round trip
