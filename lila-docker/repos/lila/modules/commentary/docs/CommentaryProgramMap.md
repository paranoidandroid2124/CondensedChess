# Commentary Program Map

This is the onboarding map for the current Chesstory commentary-analysis work.
It points to the live authority documents and summarizes the current operating
state.

Use this map to avoid stale or branch-external assumptions, but do not treat it
as approval to preserve duplicated implementation shapes. When cleanup or
deduplication is the task, inspect the current source and call sites first,
then simplify in place while keeping the runtime truth/trust boundary intact.
Adding a new helper, `Evidence`, `Support`, `Boundary`, `Contract`, or policy
object requires a live-code reason that an existing boundary cannot carry the
same responsibility.

## Current Status

Current work is a boundary redesign plus docs compression:

- claim authority is centralized under `analysis.claim`
- planner authority checks are delegated through `PlannerClaimAdmission`
- render release safety is centralized under `analysis.render.FragmentAuthority`
- public MoveReview wire is minimized around backend-owned structured
  diagnostics and the certified `moveReviewPlayerSurface`
- opening-family support rows now enter that player surface only through
  `OpeningFamilyCatalog` plus `OpeningFamilyClaimResolver` `SupportedLocal`
  admission, not through rendered prose or frontend reconstruction. They may
  expose bounded `openingBook` metadata from `OpeningReference` aggregates
  (ECO, total master-game count, and up to three SAN top moves), but never raw
  explorer/source/sample-game payloads.
- static opening coverage now comes from `openings.tsv` runtime rows plus
  tooling provenance reports, while preserving the same label-plus-FEN family
  proof boundary; the removed Scala broad-variation fixture floor is no longer
  treated as coverage authority. The pool is currently pruned to 1276 rows that
  replay against captured Lichess masters evidence as `master-backed`; the live
  audit found and removed 438 `not-found-in-masters` expansion rows. Same-EPD
  transposed endpoint aliases remain available to the family resolver even when
  canonical display lookup chooses one row.
- static opening coverage expansion is paused for provenance cleanup:
  `lila.commentary.tools.opening.OpeningPoolAudit` and
  `OpeningMasterDbAuditRunner` now classify parse issues, normalized endpoint
  duplicates/transpositions, and optional master DB evidence under
  `modules/commentaryTools/src/test`; `--base-url` selects the masters endpoint
  while the default remains Lichess. `--since`/`--until` are optional query
  window parameters for endpoints that accept them; current `/masters` live
  audit should normally run without dates because date-windowed master queries
  can return `HTTP 400`. Live runs can write replayable raw-response JSONL with
  `--write-evidence-cache`; fetch or parse failures are reported as
  `master-fetch-error`. Reports include `provenanceStatusCounts`, and
  `--only-status` narrows the output rows for cleanup triage. `--skip-rows`,
  `--max-rows`, and `--request-timeout-seconds` support chunked/resumable live
  audit. Rows remain unverified unless a live OAuth-backed master DB report or
  replayable JSONL evidence cache keyed by endpoint-stable `rowId` marks them
  `master-backed`
- opening route coverage is expanding through `opening_routes.tsv`
  descriptors while preserving legal replay plus target-mode proof gates.
  `OpeningRouteMiningRunner` is tooling-only support under
  `modules/commentaryTools/src/test`; it mines candidate knight routes from the
  master-backed `openings.tsv` pool, filters out one-ply generic development
  and repeated-square paths, and leaves low-support candidates deferred. The
  current runtime route catalog contains 52 descriptors: 48 knight-route rows
  plus 4 bishop fianchetto rows for Catalan, English, King's Indian, and
  Queen's Indian. Bishop rows may become exact target-fixation evidence only
  after the same legal replay, family/target allowlist, declared-target, and
  terminal target-mode gates as knight routes. Every route target is present in
  the corresponding `OpeningFamilyCatalog` target allowlist.
- opening goal/prose coverage is expanding inside the existing `OpeningGoals`
  evaluator for Gruenfeld `...d5`, Slav/Semi-Slav `...e5`, Dutch `...Ne4`,
  Queen's Indian `...Ne4`, Bogo-Indian `...Ne4`, Catalan `dxc5` tension
  release, Open Catalan `c4` pawn recovery, Sicilian `...c5` c-pawn challenge,
  and King's Gambit `f4` break structures; those evaluations still flow only
  through `openingGoalEvaluation` into outline/explanation consumers and do not
  expand exact `central_break` authority beyond the d/e-pawn witness
- canonical docs stay as four files, but historical CTH logs are compressed
  into current-state rules and summary tables

Do not use branch-external or branch-removed documents as authority for this
worktree.

## Live Coverage Map

The current motif and authority vocabulary is intentionally layered. A raw
motif name, tactical detector id, semantic source id, relation witness, and
proof family do not carry the same release authority.

| layer | live source | current coverage | authority role |
| --- | --- | --- | --- |
| raw motif model | `modules/commentaryCore/src/main/scala/lila/commentary/model/Motif.scala` | 55 `Motif` case classes | detector/model vocabulary only; not public authority by itself |
| tactical pattern detectors | `modules/commentaryCore/src/main/scala/lila/commentary/analysis/tactical/TacticalPatternDetectors.scala` | 9 detector ids | board-pattern support; named mate ids may narrow a top-PV `MateNet` practical label, but only after the analyzer-owned relation witness |
| relation witness inventory | `MoveReviewExchangeAnalyzer.RelationKind` plus `RelationObservationCatalog` | 23 implemented relation kinds, 0 deferred relation kinds | public `strategic_relation` rows are limited to implemented descriptors with analyzer-owned board replay |
| semantic observation ids | `StrategicObservationIds.SemanticObservationId` | 25 ids | normalized semantic ids; relation admission uses only catalog descriptors |
| evidence source ids | `StrategicObservationIds.EvidenceSourceId` | 110 ids | support/proof vocabulary; source-shaped strings cannot mint relation authority |
| plan taxonomy | `PlanTaxonomy` | 35 plan kinds, 10 ranked themes plus `Unknown` | planning/proof-family vocabulary; a plan label is not exact-board proof |

The deferred relation set is therefore not the whole motif map. It is currently
empty: relation-shaped motif families that already exist in nearby model or
detector vocabulary remain non-public until they receive a public relation
witness, source, semantic id, and catalog descriptor.

| deferred relation | current fallback | graduation requirement |
| --- | --- | --- |
| none | n/a | n/a |

Current five-motif decision:

| relation motif | current decision | live authority |
| --- | --- | --- |
| `trapped_piece` | public when board-backed | implemented `Trapped piece` supported-local row, plus catalog relation metadata, only from the reviewed legal move attacking an explicit bound non-pawn/non-king target that has no safe legal escape |
| `zwischenzug` | public when board-backed | implemented `Zwischenzug` supported-local row, plus catalog relation metadata, only from the reviewed legal move giving direct check while a legal non-pawn capture on the explicit bound recapture square was available and the reviewed move did not take that square |
| `domination` | public when board-backed | implemented `Domination` supported-local row, plus catalog relation metadata, only from the reviewed legal move attacking an explicit bound same/lower-value non-pawn/non-king target whose pseudo-escape squares are all controlled and whose legal escapes all fail |
| `stalemate_trap` | public when PV-backed | implemented `Draw resource` relation only from legal replay ending in actual stalemate with draw-stable engine score |
| `perpetual_check` | public when PV-backed | implemented `Draw resource` relation only from legal replay proving a repeated checking cycle, repeated position key, and draw-stable engine score |

The MoveReview relation row cap is a display bound, not a proof downgrade.
PV-backed draw-resource rows sort first. The strict witness-only board relations
`trapped_piece`, `domination`, and `zwischenzug` sort ahead of generic
line/tactical relation rows once they have passed the analyzer witness boundary,
so a rich relation position does not hide the exact-board motifs behind softer
catalog rows.

`trapped_piece` is an exact board relation now, not a raw motif fallback. The
first replayed move must be the reviewed move, the moved attacker must attack an
explicit bound target, invalid explicit targets close the witness, the target
must be a higher-value enemy non-pawn/non-king piece, and every legal move by
that piece must still leave it attacked or unavailable. Generic `PieceActivity`
`isTrapped`, raw `trapped_piece` motif text, and helper notation do not mint the
relation.

`zwischenzug` is exact-board now, but deliberately narrow: the first replayed
move must be the reviewed move, the explicit target set must be valid and
non-empty, the side to move must have had a legal non-pawn capture on that
target square before the move, the reviewed move must not be that recapture, and
the reviewed move must give direct check from the moved piece. Raw
`Zwischenzug(...)` helper notation can still be sanitized to `move-order
caution`, but it does not mint the relation.

`domination` is exact-board now, but deliberately below `trapped_piece`: higher-value
targets are handled by the trapped-piece witness, line pins/skewers remain line
relations, and raw `Domination(...)` or `domination` motif text is only sanitizer
input. The public relation requires an explicit valid target, a replayed attacker
that attacks the target, complete control of the target's pseudo-escape squares,
and no legal target move that escapes the pressure.

Draw-resource relations are not ordinary motif promotions. `stalemate_trap`
has a public relation path only when bounded legal replay reaches an actual
stalemate terminal after the played PV entry move and the engine score remains
draw-stable with no mate score. `perpetual_check` has a public relation path
only when bounded legal replay starts with the played checking move, shows at
least three checks by the same side, returns to a repetition-compatible position
key, and the engine score remains draw-stable with no mate score. That bounded
line can come from the top PV or from a validated root `ProbeResult` reply PV
whose FEN and probed/candidate move bind to the reviewed position and played
move. Raw
`stalemate_trap` and `perpetual_check` motif text and helper notation remain
PV-only support and do not emit public motif prose.

## Document Roles

Read the documents in this order:

1. `CommentaryProgramMap.md`
   - onboarding, current status, active frontier, document navigation
2. `CommentaryPipelineSSOT.md`
   - canonical runtime path and module ownership
3. `CommentaryTruthBoundary.md`
   - chess-truth signoff and no-go failures
4. `CommentaryTrustBoundary.md`
   - trust-risk map, CTH summary, authority ladder, defer rationale

## Live Architecture Boundary

Current authority is internal and MoveReview-first:

- `ProofContractRules` defines proof-family eligibility.
- `ClaimAuthorityResolver` resolves `CertifiedOwner`, `SupportedLocal`,
  `DiagnosticOnly`, or `Suppressed`.
- `PlannerClaimAdmission` connects planner plans/inputs to that resolver.
- `MoveReviewExchangeAnalyzer` owns bounded legal PV replay for favorable
  exchange witnesses, queen-trade/simplification geometry, exact-slice branch
  keys/continuation terms, IQP inducement prefixes, quiet/central-break
  best-defense metadata consumers, L3 threat first-step evidence, and shared
  semantic observations. Owner witnesses that describe the reviewed move must
  receive `playedMove`; the top PV first move is replay evidence, not identity.
  Defender-trade owner visibility is typed-context plus replay proof, not raw
  defender/trade prose. Other implemented relation support also carries
  analyzer-owned typed relation details for king-attack, material-target, line,
  and lure-and-win geometry, so later consumers can
  reuse the same legal replay/attack-defense witness without introducing
  motif-local parsers. A relation witness with non-empty typed details must
  match its relation kind before semantic emission, and semantic consumers cross
  that boundary through the analyzer-owned relation projection rather than raw
  witness fields. Defender/bad-piece owner seed and transition terms are
  expanded through the analyzer relation projection, keeping policy code from
  reading relation facts or focus squares directly. Branch-key and `branch:*` fact formatting is
  also analyzer-owned through shared branch helpers rather than policy-local UCI
  slicing. Deferred relation motifs, when present, stay in the same catalog
  inventory with required witness and fallback-lane metadata, but only
  implemented board-replayed descriptors can emit relation authority. Legacy
  motif-prefix, theme-keyword, canonical motif-term, and motif delta prose
  consume that catalog boundary for softer non-relation text or raw helper
  suppression, including PV-only tags such as `stalemate_trap` and
  `perpetual_check`, and witness-only tags such as `trapped_piece`,
  `domination`, and `zwischenzug`. Legacy plan evidence still falls back to
  `key-square restriction` when old domination motifs appear, but public
  relation authority requires the board witness. User-facing helper notation is
  rewritten or suppressed through the same fallback boundary; threat-summary
  labels consume the same fallback instead of raw relation motif names.
  Strategy-pack/structure-arc piece-activity evidence no longer turns generic
  trapped-piece activity into a relation fallback term.
  The final sanitizer removes deferred relation motif terms and witness-only raw
  relation tags from cached or legacy strategy-pack evidence lists while
  preserving softer fallback terms.
  Deferred tags and witness-only raw relation tags do not raise the generic
  context beat into high-tension tactical tone or pass as generic motif-prefix
  signals. Generic
  fact-corroboration helpers also check the relation catalog boundary before
  treating a motif as board-supported.
- `StrategicSemanticObservationPipeline` emits typed semantic observations
  through minority-attack and a catalog-driven relation producer.
  `StrategicSemanticObservationContext` owns the normalized reviewed move, exact
  target extraction, one bounded top-PV replay, and one extracted
  relation-witness set; the relation producer consumes the implemented catalog
  set from that shared context instead of replaying or filtering locally.
  Replay-backed relation observations require the reviewed `playedMove` and do not
  infer it from the engine top line. Selector use of these observations does
  not create proof authority by itself. Deferred relation motifs stay in the
  same catalog inventory as non-public rows with required witness shape and
  defer reason; they have no implemented descriptor, selector evidence, or
  frontend authority token until a board-replayed witness graduates them.
  Implemented descriptors also own bounded public target fallback over
  relation-specific focus, keeping surface projection policy out of the payload
  builder. Any public relation carrier must preserve relation-specific focus
  before it can reach the public row, and selector merge does not synthesize
  relation focus or relation target from generic focus/target squares; carriers
  without a selected relation kind stay silent, and selected carriers must match
  the descriptor-owned source/semantic/witness admission triple before they can
  reach the public row. Generic `targetSquare` metadata is not analyzer target
  evidence.
  `StrategicIdeaEvidence` preserves relation identity and relation focus only
  for implemented catalog kinds; unknown or uncataloged relation names are stripped
  before selector candidates are built.
- `QuestionFirstCommentaryPlanner` ranks questions; it does not own proof
  authority.
- `FragmentAuthority` decides renderer release safety.
- `CommentaryApi` and frontend code consume typed payloads only.

## Active Frontier

Open for maintenance:

- exact-board promoted slices already covered by proof contracts
- source/test tooling that improves exact witness quality
- trigger hardening that removes generic string overlap, unknown-subtype
  fail-open, or blocked line-geometry without expanding public authority
- relation-witness hardening that centralizes legal replay plus attack/defense
  evidence in `MoveReviewExchangeAnalyzer`, routes semantic producer access
  through `StrategicSemanticObservationContext`, and keeps new motif rows
  selector/support-only until a proof contract admits them; deferred motif
  families are tracked in the relation inventory but remain non-public and
  uncataloged until they receive a board-replayed witness
- MoveReview player-surface relation metadata that exposes cataloged relation
  support as `strategic_relation` advanced rows without creating proof authority
- docs and package cleanup that preserves the current authority boundary

Closed unless a new audit explicitly opens them:

- broad heavy-piece/local-bind/global-squeeze expansion
- Track 5 lesson authority
- Chronicle/Active runtime reopening; their remaining planner, compression, and evaluation helpers have been completely cleaned up and removed from the workspace
- public API/frontend wire expansion except audited typed diagnostics or
  payload minimization that does not create product authority
- support-only or deferred carrier promotion

## Strategic Expansion Naming Boundary

Expansion labels are planning vocabulary, not runtime module names or authority
tokens. Before reopening a closed strategic asset, map it onto four stable
axes:

| planning label | stable boundary | implementation rule |
| --- | --- | --- |
| broad heavy-piece/local-bind/global-squeeze expansion | split into resource/route restriction assets such as `LocalFileEntryBind`, `CounterplayAxisSuppression`, `ProphylacticRestraint`, `RouteNetworkBindProof`, `TwoAxisBindProof`, and `HeavyPieceLocalBindValidation` | do not add positive public authority to broad or negative-lane helpers; new release paths need a typed exact-slice proof or typed contract carrier plus `ClaimAuthorityResolver` admission |
| restricted-defense conversion expansion | `RestrictedDefenseConversionProof` typed contract plus `ClaimAuthorityResolver` admission | keep public wording bounded to technical conversion support after a checked best defense; do not promote generic conversion prose, compensation, or winning-plan claims |
| outpost expansion | `OutpostOccupation` exact-slice under `OutpostEntrenchment` | keep public wording bounded to a knight occupying the named outpost after legal reviewed-move replay, top-PV identity, `Fact.Outpost`, and stable branch proof; do not promote broad outpost tags or route-access evidence |
| B7/B8 broad expansion | historical frontier/coverage shorthand | keep `B7`/`B8` names in guard/test diagnostics only; do not introduce proof families, sources, packages, or product rows with those labels |
| broad color-complex expansion | `ColorComplexSqueeze` exact-slice family through `color_complex_squeeze_probe`; generic `color_complex_clamp` remains selector/support evidence | do not promote generic color-complex prose, coordinates, or minor-piece words into authority |
| mobility-cage expansion | catch-all still closed; nearest live evidence is the cataloged `trapped_piece`/`domination` mobility-restriction relation witnesses plus route denial assets | choose a concrete witness family before implementation; do not create a catch-all mobility-cage module |
| Track 5 lesson authority | scoped takeaway only through `MoveReviewScopedTakeaway` | do not use Track names or lesson labels as runtime authority; broad lesson authority stays closed |
| Chronicle/Active runtime reopening | completely removed from the workspace | do not consume `GameChronicle*`, Active-note DTOs, or active branch/thread carriers in released MoveReview truth/signoff |

Admission-unit planning follows the same boundary: the catalog may queue only
plan-kind work units that already resolve through a public runtime contract.
Current cataloged families include static/backward target fixation, local-file
entry, color-complex flank clamp, outpost occupation, IQP inducement,
simplification, defender/queen/bad-piece exchange ownership, and central-break
timing. Runtime-only proof families such as `target_focused_coordination` remain
covered by contract/surface tests rather than by a `PlanTaxonomy.PlanKind`
admission unit.

New strategic work should use domain/proof names, not rollout or breadth names.
Acceptable names describe the chess asset and proof boundary, for example
`LocalFileEntryBind`, `CounterplayAxisSuppression`,
`ColorComplexSqueeze`, `OutpostOccupation`, or a cataloged relation witness. Avoid `broad`,
`global`, `Track`, `Frontier`, `B7`, `B8`, `Active`, and `Chronicle` in new
runtime source modules, proof families, public authority tokens, or product row
kinds unless they are documenting legacy/test-only boundaries.

Color-complex has an explicit exact-board contract. It is closed to generic
`color_complex_squeeze` source packets and opens only through
`color_complex_squeeze_probe`, where the board proves that a friendly bishop or
knight attacks the opponent-owned semantic weak square and the packet branch is
proven/stable. The packet contract consumes a typed exact-slice proof, not
generic coordinate/minor-piece strings:

| proof family | proof source | status | certified | supported local | failure |
| --- | --- | --- | --- | --- | --- |
| `color_complex_squeeze` | `color_complex_squeeze_probe` | `Releasable` | true | true | `color_complex_authority_closed` |

## Verification Expectations

For package/naming/boundary work:

- compile and run targeted tests serially
- keep runtime and test/tooling boundaries separate
- update the relevant canonical doc in the same change
- report unverified work as boundary cleanup only

For claim authority or trust behavior:

- include claim/proof tests
- include planner tests when admission changes
- include renderer or parity tests when release text changes
- include color-complex tests when deferred authority is touched

## Maintenance Triggers

Re-audit the runtime docs only when:

- the user asks for a fresh audit
- code changed after the relevant snapshot in commentary runtime, controller,
  API, or analyse frontend paths
- a task introduces a runtime path not covered by the current SSoT

When changing the audited pipeline, update `CommentaryPipelineSSOT.md`.
When changing trust-relevant behavior, update `CommentaryTrustBoundary.md`.
When changing truth/signoff behavior, update `CommentaryTruthBoundary.md`.
