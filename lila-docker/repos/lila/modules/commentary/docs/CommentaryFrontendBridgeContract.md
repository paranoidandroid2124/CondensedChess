# Commentary Frontend Bridge Contract

`CommentaryFrontendBridgeContract` freezes the minimal frontend bridge before
any product UI rewrite.

It opens only a disposable analyse-side adapter/hook for `CommentaryRender`
transport. It does not open a new analysis panel, polished UI, frontend
rewrite, source live integration, LLM prose generation, or local chess meaning
creation.

Short form: no product UI.

## Adapter Scope

The first bridge lives in:

- `ui/analyse/src/chesstory/commentaryBridge.ts`

The executable scaffold lives in:

- `ui/analyse/tests/commentaryBridge.test.ts`

The bridge exposes stable adapter functions:

- `buildCommentaryRequest`
- `buildCompletedProbeBridgePayload`
- `decodePublicCommentaryRender`
- `fetchCommentaryRender`

These are request/response helpers only. They are not a view component and do
not own presentation layout.

## Request

`buildCommentaryRequest` may send only the backend `CommentaryRequest` fields:

- `currentFen`
- optional paired `beforeFen`
- optional paired `playedMove`
- `nodeId`
- `ply`
- optional `enginePacket`

The frontend request must not include selector claims, source rows,
`OpeningContextCandidate`, opening fixture JSON, blocked/suppression hints,
wording-strength requests, debug/internal toggles, or generated text.

If `beforeFen` or `playedMove` is missing, the adapter omits both. The backend
remains the authority for transition validation.

`enginePacket` is passed only as certification runtime intake for the backend
seam. The bridge must not display raw engine packet fields as text or public
evidence.

## Completed-Probe Payload Helper

`buildCompletedProbeBridgePayload` is a completed-probe payload helper only.
It prepares a sanitized JSON-safe object for a future local engine execution
handoff, but it is not a `CommentaryRequest` field, not a public controller or
API route field, not product UI, not Stockfish execution, and not SAN
authority.
It is not a public controller or API route field and not SAN authority.

The helper may copy only typed completed-probe bridge fields:

- exact current node identity: `currentFen`, `nodeId`, `ply`, and `variant`
- engine fingerprint
- optional budget shape
- probe requests
- completed root probe payload
- completed child probe payloads
- UCI line arrays
- rank, MultiPV index, and MultiPV count
- requested and realized depth
- `generatedAt`, `maxAgeMillis`, and `completed`
- child parent branch id, parent UCI prefix, and parent root rank

The helper strips display, raw engine, source, debug, and prose-like material:
SAN hints, eval/centipawn/mate fields, raw PV strings or text, best-move
fields, display engine labels, source rows, retrieval snippets, debug/internal
fields, prose, recommendations, verdicts, result fields, and theory fields.

The helper performs only basic payload-shape checks. It does not validate chess
legality and it does not validate or produce SAN. Backend exact-FEN UCI replay
and SAN generation remain authoritative. The helper fails closed by returning
`null` when exact node identity or required probe binding is missing, root
MultiPV is not `3`, child MultiPV is not `2`, realized depth is below `16`, a
probe is not completed, or a UCI line is empty or malformed-looking.

## Response

`decodePublicCommentaryRender` consumes the backend `CommentaryResponse` and
returns either:

- a public render payload copied from public `CommentaryRender` fields, or
- an empty state.

Public fields are:

- response/render status
- render `schemaVersion`
- public render blocks, copied only as role, claim id, render text,
  wording strength, evidence ids, variation evidence ids, boundaries, and
  non-authoritative marker
- block `wordingStrength`
- block `evidenceIds`
- block `variationEvidenceIds`, when present
- public evidence refs
- public prepared variation evidence, when present
- public boundaries
- public wording cap
- public `forbiddenTerms` metadata carried by render text and wording

The renderer may emit closed backend-owned book-style public text on a block
when the server-side annotation writer has produced it. The bridge preserves a
block when it has public text, public evidence ids, or public variation
evidence ids. It does not invent fallback text for prose-empty blocks.

When `render.variationEvidence` is present, the bridge may preserve only the
renderer-owned `RenderVariationEvidence` fields: proof id, bound claim id,
start FEN, owner/defender/anchor/route/scope binding, renderer-public line
role, move role, SAN/UCI
line arrays, public move-pair fields, tested/reply/resource line fields, test
result, proof purpose, bounded provenance refs, boundary fields, wording cap,
and surface allowance. The bridge must drop variation-evidence entries whose
role is not one of the renderer-public `RenderLineRole` keys: `resource`,
`caution`, `hold`, `conversion`, `pressure`, or `simplification`. It must not
expose lower prepared roles such as `failed_tempting_move`, and it must not
expose the lower prepared `proves` token. If the backend omits
`render.variationEvidence`, or if every variation-evidence entry is dropped,
the decoded public output keeps the existing omitted/empty behavior.

Internal fields are not public:

- `internal`
- internal suppressions
- engine intake status and reason
- invalid request reason
- any blocked/suppressed claim metadata
- raw candidate-line/probe/cache fields such as `CandidateLineEvidence`,
  `branchId`, `parentBranchId`, `engineConfigFingerprint`, `cacheKey`,
  `rawLines`, or `pvLines`

The bridge must not expose `internal` on the public display payload.

## Forbidden Frontend Responsibilities

The frontend bridge must not rank claims.

The frontend bridge must not admit claims.

The frontend bridge must not revive suppressed or blocked claims.

The frontend bridge must not promote source, opening, motif, endgame-study, or
retrieval context to current-position truth.

The frontend bridge must not merge `master_reference` and `online_trend`
rankings.

The frontend bridge must not render raw engine eval, PV, centipawn, mate,
depth, or engine labels as commentary.

The frontend bridge must not turn `RenderVariationEvidence` into book-style
prose, best-move claims, forced-line claims, result claims, or engine/oracle
proof. It may only pass through or hide backend-prepared public fields,
including backend-owned `RenderText.publicText`.
Resource and caution line roles remain structured evidence only; the bridge
must not turn them into a recommendation, best-defense claim, or result claim.
Opening sequence context and `opening-line-test:*:context` refs remain
pass-through source context only. The bridge must not render raw opening rows,
frequencies, move-order rows, theory claims, or source-derived
recommendations.

The frontend bridge must not upgrade wording.

The frontend bridge must not create best-move, theory-truth, forced-line,
result, winning, drawing, or oracle wording.

The frontend bridge may show or hide backend render blocks only.

## Status Handling

The bridge treats these states as silent public output:

- backend `invalidRequest`
- response `noCommentary = true`
- render `status = noCommentary`
- wording `maxStrength = hidden`
- wording `maxStrength = negative_only`

`contextOnly` may display backend context blocks only as non-authoritative
context. The bridge must not add theory, best-move, forced-line, result, or
engine wording around those blocks.

## Stale Node Handling

`fetchCommentaryRender` binds a request to the current exact node identity:

- `currentFen`
- `nodeId`
- `ply`

After the response returns, the adapter checks the current node identity again.
If `currentFen`, `nodeId`, or `ply` changed, the response is discarded as
`stale_node` and no public blocks are exposed.

Wrong-ply, stale-node, and engine mismatch states are fail-closed display
states. They must not trigger local reinterpretation.

## Future UI Rewrite Boundary

The bridge is intentionally replaceable. A future frontend rewrite may consume
the same backend `CommentaryResponse`, but it must still treat
`CommentaryRender` as final display data and must not move ranking, admission,
suppression, source truth, or engine interpretation into the frontend.
