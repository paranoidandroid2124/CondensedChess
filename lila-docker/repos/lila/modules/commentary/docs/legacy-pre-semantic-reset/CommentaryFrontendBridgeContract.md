# Commentary Frontend Bridge Contract

`CommentaryFrontendBridgeContract` freezes the minimal frontend bridge and the
first display-only analyse product surface.

It opens an analyse-side adapter for `CommentaryRender` transport, a compact
move explanation panel that consumes that adapter, and an optional
non-production local Stockfish probe adapter that can supply completed root/
child probe payloads to the internal backend handoff. It does not open source
live integration, LLM prose generation, local chess meaning creation, or
frontend authority over commentary truth.

Short form: display only. The frontend may show backend-prepared public text
and public-safe line notation; it may not create commentary.

## Adapter Scope

The first bridge lives in:

- `ui/analyse/src/chesstory/commentaryBridge.ts`

The first display-only product surface lives in:

- `ui/analyse/src/chesstory/moveExplanation.ts`
- `ui/analyse/src/chesstory/moveExplanationView.ts`

The optional local-probe adapter lives in:

- `ui/analyse/src/chesstory/localProbe.ts`

The executable scaffold lives in:

- `ui/analyse/tests/commentaryBridge.test.ts`
- `ui/analyse/tests/moveExplanation.test.ts`
- `ui/analyse/tests/localProbe.test.ts`

The bridge exposes stable adapter functions:

- `buildCommentaryRequest`
- `decodePublicCommentaryRender`
- `fetchCommentaryRender`

These are request/response helpers only. They are not a view component and do
not own presentation layout.

`moveExplanation.ts` and `moveExplanationView.ts` are presentation consumers
only. They do not own request schema, truth production, ranking, admission,
suppression, SAN generation, or prose generation. `localProbe.ts` may run the
existing local ceval engine to obtain root `MultiPV 3` and first-two-candidate
child `MultiPV 2` UCI lines at the frozen depth budget, but it may only package
them as internal completed-probe input. The backend still replays exact FEN/UCI
and is the SAN/proof authority.

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
wording-strength requests, completed-probe payloads, debug/internal toggles,
or generated text. Completed root/child probe payloads are internal backend
provider inputs only and not public controller/API route fields. The optional
local-probe path sends them only to
`POST /internal/commentary/render-local-probe`, never through
`buildCommentaryRequest` or `POST /api/commentary/render`.

If `beforeFen` or `playedMove` is missing, the adapter omits both. The backend
remains the authority for transition validation.

`enginePacket` is passed only as certification runtime intake for the backend
seam. The bridge must not display raw engine packet fields as text or public
evidence.

## Response

`decodePublicCommentaryRender` consumes the backend `CommentaryResponse` and
returns either:

- a public render payload copied from public `CommentaryRender` fields, or
- an empty state.

Public fields are:

- response/render status
- render `schemaVersion`
- public render blocks, copied only as role, claim id, render text,
  wording strength, evidence ids, variation evidence ids, boundaries,
  non-authoritative marker, and phrase capability
- block `wordingStrength`
- block `evidenceIds`
- block `variationEvidenceIds`, when present
- public evidence refs
- public prepared variation evidence, when present
- public boundaries
- public wording cap
- public `forbiddenTerms` metadata carried by render text and wording

The renderer may emit closed backend-owned book-style public text on a block
when the server-side annotation writer has produced it and the block phrase
capability authorizes `line_commentary`. The bridge preserves public text only
when the copied phrase capability, block role, block wording strength, and
render wording cap still allow it. It preserves variation evidence ids only
when the same phrase capability authorizes public line commentary. It does not
invent fallback text for prose-empty blocks.

When `render.variationEvidence` is present, the bridge may preserve only the
renderer-owned `RenderVariationEvidence` fields: proof id, bound claim id,
owner/defender/anchor/route/scope binding, renderer-public line role, move
role, public SAN move/line fields, tested/reply/resource SAN line fields, test
result, proof purpose, wording cap, and surface allowance. The bridge must not
copy start FEN, UCI PV, provenance refs, replay/freshness boundary fields,
depth, MultiPV, engine fingerprints, cache keys, branch ids, or raw move UCI
because those are lower proof/debug inputs, not public display data. The bridge
must drop variation-evidence entries whose
role is not one of the renderer-public `RenderLineRole` keys: `resource`,
`caution`, `hold`, `conversion`, `pressure`, or `simplification`. It must not
expose lower prepared roles such as `failed_tempting_move`, and it must not
expose the lower prepared `proves` token. It also drops entries whose
`surfaceAllowance` is not `public_line`; `boundary_only` evidence may remain a
backend boundary concept but is not public display payload. If the backend
omits `render.variationEvidence`, or if every variation-evidence entry is
dropped, the decoded public output keeps the existing omitted/empty behavior.

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

The frontend bridge must not turn `RenderVariationEvidence`, role labels, or
evidence ids into book-style prose, best-move claims, forced-line claims,
result claims, or engine/oracle proof. It may only pass through or hide
backend-prepared public fields, including backend-owned `RenderText.publicText`
that is still authorized by phrase capability.
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

## Display-Only Product Surface

The move explanation surface posts to the existing public endpoint by default:

- `POST /api/commentary/render`

When the explicit local probe mode is enabled, it may instead call:

- `POST /internal/commentary/render-local-probe`

That internal route receives a wrapper `{ request, completedProbe }`; the
nested `request` must still be the clean `CommentaryRequest` subset. The
frontend may construct this provider only when the server-provided analyse
configuration marks local probing as available and the URL explicitly requests
local probe mode. If local probe construction fails, or if the internal
response has no public line evidence tied to a visible block, the surface
remains silent rather than requesting fallback public prose.

It must call the bridge through `fetchCommentaryRender` and
`buildCommentaryRequest` semantics. Its current-node identity is:

- `currentFen = ctrl.node.fen`
- `nodeId = ctrl.path || "root"`
- `ply = ctrl.node.ply`

It may include `beforeFen` and `playedMove` only as a pair when the previous
node exists and the current node carries a played move.

Late or overlapping responses must not overwrite a newer current-node state.
`invalidRequest`, `noCommentary`, `hidden`, `negative_only`, stale-node, and
request-error states remain quiet public output.

The view may render:

- backend-prepared block `RenderText.publicText`, in backend block order, only
  after phrase capability survives bridge validation
- public SAN notation from decoded public variation evidence when the evidence
  is tied to the block, has `surfaceAllowance = public_line`, and the block
  phrase capability allows `line_commentary`

The view must not render role-label prose or fallback captions. The view must not render proof ids,
claim ids, evidence labels, internal ids, boundaries, UCI-only raw PV, depth,
eval, engine labels, cache/probe fields, or debug/internal metadata.

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
