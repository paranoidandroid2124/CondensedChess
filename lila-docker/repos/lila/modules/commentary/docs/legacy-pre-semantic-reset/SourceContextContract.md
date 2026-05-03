# Source Context Contract

This document freezes the source-context contract for opening, motif,
endgame-study, and retrieval reference data.

It does not open raw source ingestion, a live planner, renderer prose, API
controller wiring, or frontend wiring. The runtime adapter boundary is
`SourceContextAdapter`, which normalizes source-family inputs into
`SourceContextCandidate`. The runtime handoff is then either direct
`SourceContextCandidate` to `CommentaryClaim` conversion through
`SourceContextClaimBoundary`, or the same conversion via the backend private
`EvidenceClaimHandoff.sourceContext` field. That backend field accepts only
already-normalized candidates; it is not a raw source-row payload, live lookup,
controller contract, or frontend input. `SourceContextCandidate.candidateId`
must be public-safe because it can become a public render `claimId`; source,
debug, result, player, event, URL, or raw metadata must stay in non-public
source rows and must not be encoded into candidate ids. Selection, outline, and
renderer safety rules still decide whether that prepared context is emitted.
Source family internals remain upstream of that normalized handoff.

The contract is deliberately sidecar-only:

- source rows may provide context, statistics, detector examples, study
  applicability, or similar examples
- source rows must not become Root, U, Object, Delta, Certification, or Sxx
  truth owners
- source rows must not become prepared variation evidence or line-proof owners
- opening, motif, endgame-study, and retrieval rows may link to public-safe line-test proof
  ids only as `opening-line-test:*:context`, `motif-line-test:*:context`, or
  `endgame-line-test:*:context`, or `retrieval-line-test:*:context` references;
  the proof itself must still come from a separately admitted exact-board claim
- any current-position claim still needs the existing exact-board carrier for
  its layer
- selector output may use source rows only as `contextOnly` or as context after
  an exact-board lead is selected
- source context cannot beat a current exact-board lead and cannot upgrade
  `wordingStrengthCap`
- selector fallback may emit source rows only when their source-family contract
  is satisfied; `source_context_only` alone is a non-authoritative marker, but
  `ambiguous_transposition`, `forbidden_shortcut`, or `no_board_reason`
  prevents selected context output

## Active Families

| Family | Role | Authority level |
| --- | --- | --- |
| `opening` | opening name, line, position index, move statistic, and theme context | reference/context/statistic only |
| `motif` | exact-board detector examples and source tags | detector carrier required; tags are example-only |
| `endgameStudy` | known study pattern context such as Lucena, Philidor, and Vancura | exact applicability context only |
| `retrieval` | similar historical or educational examples | non-authoritative example only |

## Deferred Families

Game-context source input is deferred to a separate lane.

Clock, rating, time-control, repetition, tournament, study-chapter, draw-offer,
and human-risk context are not part of this contract.

Endgame result-service source input is deferred.

Server-side result-table storage, probing, result-distance fields, and remote
runtime fallback are not part of this contract.

Endgame-study rows are not result or conversion evidence. They may only say
that an exact board satisfies a named pattern's material and placement
conditions. Win, draw, loss, forced conversion, or forced-line claims require a
later certification or result-service lane.

## Stored Fixtures

The executable fixture set lives under
`modules/commentary/src/test/resources/commentary-corpus/`:

- `opening-sources.jsonl`
- `opening-lines.jsonl`
- `opening-positions.jsonl`
- `opening-move-stats.jsonl`
- `opening-themes.jsonl`
- `opening-aliases.jsonl`
- `opening-context-candidates.jsonl`
- `motif-examples.jsonl`
- `motif-reject-fixtures.jsonl`
- `endgame-studies.jsonl`
- `endgame-study-fixtures.jsonl`
- `endgame-study-reject-fixtures.jsonl`
- `retrieval-examples.jsonl`
- `retrieval-reject-fixtures.jsonl`

The validator lives under
`modules/commentary/src/test/scala/lila/commentary/source/`.

No bulk external data is committed by this contract. Large PGN, puzzle,
opening, or example indexes must remain external input or local/generated
cache until a later storage policy explicitly admits a derived artifact.

## Source Manifest Rules

Each source manifest row declares:

- `sourceId`
- `sourceFamily`
- `sourceType`
- `sourceUse`, for opening rows
- `license`
- `redistribution`
- `derivedData`
- `attributionRequired`
- `shareAlikeRequired`, `attributionText`, and `licenseNotice`, when required
  by the source license
- `sourceUrl`

Opening online-trend rows additionally declare `aggregateUse`, `sourceYear`,
`sourceMonth`, `yearMonth`, `ratingBucket`, `timeControlBucket`, `variant`,
`ratedFilter`, `ratedOnly`, `sampleSizeThreshold`, `parserVersion`,
`sourceChecksum`, and local-only raw/generated storage policy.

Opening master-reference rows additionally declare `aggregateUse`,
`sourceScope`, `playEnvironment`, `playerLevel`, `ratingSystem`, `minElo` or
`titlePolicy`, `timeScope`, `periodStart`, `periodEnd`, `timeControlScope`,
`perPositionSampleSizeThreshold`, `dedupePolicy`, `annotationPolicy`,
`parserVersion`, `sourceChecksum`, `rawStoragePolicy`, and
`generatedStoragePolicy`. Both storage policies must keep raw and generated bulk
artifacts local-only.

Allowed active `sourceFamily` values are only:

- `opening`
- `motif`
- `endgameStudy`
- `retrieval`

Game-context and endgame result-service family inputs are rejected by this
schema and deferred to separate lanes. Unknown families are rejected.

Unknown license, unknown redistribution, missing source id, or unsupported
derived-data status fails closed.

## Opening

Opening rows model source-backed context, not chess truth.

The schema separates:

- ECO/name/variation taxonomy in `opening-lines.jsonl`
- move-order lines as UCI move lists
- normalized `positionKey` rows in `opening-positions.jsonl`
- transposition state and downgraded ambiguous positions
- move statistics in `opening-move-stats.jsonl`
- theme, plan, and break references in `opening-themes.jsonl`
- display and context aliases in `opening-aliases.jsonl`

An opening candidate is only a statistic or context reference. It must not be
called an optimal move, theoretical truth, forced line, result claim, or current
board proof.

Opening name without an exact `positionKey` fails closed.

Ambiguous transpositions must be downgraded rather than indexed as canonical
context.

ECO/source names are canonical taxonomy labels, not necessarily human-facing
copy. Human-facing names are separate display/context aliases. Alias rows may
suggest copy such as `Open Catalan`, `Spanish Opening`, `QGD Exchange`,
`Carlsbad context`, `Slow Italian`, or `Najdorf Sicilian`, but only when they
reference an exact taxonomy row and preserve its source name, canonical name,
family, and variation.

Aliases are non-authoritative display/context references. They must not rewrite
`positionKey`, taxonomy, move statistics, candidate moves, current-position
truth, optimal-move claims, forced lines, or result claims. Closed-Spanish
wording requires a taxonomy variation that actually supports Closed Spanish.

The 2013-01 Lichess game aggregate is a pipeline smoke source. It proves local
replay, position matching, and statistic aggregation, but it is not
product-quality trend data.

Online-trend aggregates require recent, bucketed, provenance-tracked metadata:

- `sourceType` fixed to `trend_stat`
- `sourceUse` fixed to `online_trend`
- `aggregateUse` fixed to `online_trend_stat`
- source month and year
- `yearMonth` matching the month and year
- rating bucket
- time-control bucket
- standard variant
- rated/casual filter
- `ratedOnly` fixed to true for this opening lane
- sample-size threshold
- parser version
- checksum/provenance
- local-only generated artifact storage

Collection priority for local-only runs is recent Lichess standard rated
months, preferably 2024-2026, with optional high-rated and time-control bucket
splits. Raw PGN/zst material and generated move-stat artifacts stay under
ignored opening roots and must not be committed.

Master-reference aggregates are separate from online trend. The initial admitted
source target is recent Lichess Broadcast PGN data as OTB/broadcast
master-reference context. The source must be `masterGameDb` with `sourceUse`
`master_reference` and `aggregateUse` `master_reference_stat`. It must preserve
CC BY-SA 4.0 attribution and share-alike metadata through `attributionText` and
`licenseNotice`, and raw/bulk generated artifacts remain local-only.

The initial master-reference time-control scope is `classical_rapid`. Blitz-only
and bullet-only source windows are excluded or downgraded to a future
online/fast bucket. A master-reference source must declare OTB/online scope,
player-level policy, rating/title policy, source date range, duplicate handling,
annotation handling, and per-position sample-size threshold. Missing metadata
fails closed.

Coverage scope is explicit. `full_month` means the local runner reached
end-of-input for the monthly source artifact. `partial_month` means the run
stopped on an accepted-game cap. `streamed_sample` means the run stopped on a
read cap or sample stream. A local aggregate must use the narrower scope when
the complete month was not actually processed.

Master-reference rows are source-backed statistics or context references only.
They must not become current-position truth, optimal-move claims, theoretical
truth, forced lines, result claims, or engine evidence.

When a master-reference source and an online-trend source both cover a
position, synthesis must keep the `sourceUse` boundary visible. The default
educational policy is to prefer `master_reference` for teaching/reference
statistics and use `online_trend` as secondary trend context. The two rankings
must not be merged into one candidate order. `pipeline_smoke` remains validator
and pipeline smoke data only. Strongest-move, objective, forced-line, and result
claims still require engine or certified exact-board evidence.

The local aggregate ingest may add generated-row provenance fields such as
`sourceUse`, `aggregateUse`, `positionSampleSize`,
`perPositionSampleSizeThreshold`, `sampleConfidence`, and
`currentPositionTruth=false`. These fields document source scope and confidence;
they do not change candidate authority, and they are not renderer/API inputs in
this contract.

### Opening Consumption Candidate

The source-consumption contract is a structured candidate only. It does not
generate renderer-ready prose and does not connect to a live adapter. A valid
`OpeningContextCandidate` carries:

- `positionKey`
- `openingIdentity`
- optional display/context alias
- `primaryReferenceStats`
- `secondaryTrendStats`
- `sequenceContexts`
- `confidence`
- `boundaries`
- `sourceRefs`

`openingIdentity` preserves the source taxonomy row: line id, source id, ECO,
source name, canonical name, family, and variation. Display and context aliases
are non-authoritative rows attached to that identity; they do not change the
position key, taxonomy, source id, or move-stat source.

Source selection is fixed:

- primary: `master_reference`
- secondary: `online_trend`
- product-rejected: `pipeline_smoke`
- identity-only: `taxonomy_reference`

`primaryReferenceStats` may contain only `master_reference_stat` rows.
`secondaryTrendStats` may contain only `online_trend_stat` rows. Their rankings
are never merged. If both sources cover a position and disagree, the candidate
may carry a source-context disagreement boundary only.

`sequenceContexts` are structured context for book-style sequence explanation.
Allowed roles are `move_order`, `pawn_break`, `development_lag`,
`file_ownership`, `king_safety`, `transposition`, `compensation`, and
`master_online_divergence`. Each sequence context must carry
`opening_sequence_context_only`. A line-test link must carry
`line_test_link_is_not_proof` and may point only at a public-safe variation
proof id prepared elsewhere. The opening context row does not own that proof,
does not contribute proof provenance, and does not make a recommendation.

Confidence is bounded by the master source's
`perPositionSampleSizeThreshold`. Master-reference rows below that threshold
are weak or suppressed; this is expected because the current local
master-reference aggregate has many below-threshold rows. Online-trend rows
without an admitted master-reference row are secondary-only context. High or
usable confidence remains a source-statistic confidence, not chess truth.

Specific game, player, event, and game URL mentions are deferred to retrieval.
`master_reference_stat` is an aggregate source statistic and must not cite a
particular game.

Opening consumption fails closed when any required exact position key,
taxonomy row, source-use metadata, candidate boundary, or alias taxonomy link is
missing. Strongest-move, theory-truth, forced-line, result, engine, or
result-service wording rejects the candidate. The same forbidden wording policy
applies to sequence context refs, linked proof ids, and sequence boundaries.

## Motif

Motif source tags are examples only.

An admitted motif example must bind:

- `motifId`
- `displayName`
- `sourceId`
- exact FEN
- exact detector carrier
  - `ref`
  - `descriptorId`
  - `anchor`
  - `carrierKind`
- involved squares
- source tags, if any
- negative boundaries
- `authority`
- `currentPositionClaim=false`
- `sourceRefs`

A motif tag without detector carrier fails closed. A motif label alone must not
admit a projection band or current-position claim.

The frozen source-context motif storage schema is `MotifExample` plus
`MotifCarrierRef`. Runtime consumption goes through `SourceContextAdapter` into
`SourceContextCandidate`; it does not create a `CommentaryCore` facade,
synthesis path, renderer prose path, controller payload, or frontend contract.

`MotifCarrierRef.ref` must be shaped as:

- `motif-detector-carrier:<motif-example-row-id>`

Each accepted example must also include exactly one matching motif source ref:

- `motif-example:<motif-example-row-id>`

This parity mirrors the selector fallback boundary and prevents a source row
from borrowing a detector from another example or another board.
Additional `motif-example:*` refs on the same row fail closed.

Runtime-prepared motif line context may add only source-context refs shaped as:

- `motif-line-context:<role>:<context-ref>`
- `motif-line-test:<prepared-proof-id>:context`

Allowed line-context roles are `attacked_target`, `restricted_piece`,
`pinned_defender`, `overloaded_defender`, `trapped_piece`, `natural_resource`,
`failed_resource`, `held_resource`, and `partial_resource`.

These refs may describe what the source context is about, such as an attacked
target, restricted piece, pinned or overloaded defender, natural resource,
failed resource, held resource, or partial resource. They are not proof,
provenance, current-board truth, Sxx admission, or renderer prose. If a motif
line context links to a prepared proof id, its boundary must include both
`motif_line_context_only` and `line_test_link_is_not_proof`; the actual
`PreparedVariationEvidence` must be owned by a separately admitted exact-board
claim. Source-context motif claims must carry no prepared variation evidence of
their own.

Both the top-level `MotifExample` object and the nested `MotifCarrierRef`
object reject unknown fields. This is deliberate: source-context rows may not
smuggle truth, engine, oracle, result, best-move, forced-line, or Sxx projection
fields through ignored JSON members.

Accepted motif examples are executable exact-board fixtures. The test owner
verifies that each declared carrier still resolves through the current backend:

- `u_witness` carriers must be active `UScopeContract` descriptors and must be
  emitted by `UWitnessExtractor` for the row FEN
- `root_atom` carriers must be active root schemas and must be emitted by
  `RootExtractor` for the row FEN
- `certified_line` carriers must be active `CertificationScopeContract`
  families and must produce a non-rejected certification candidate for the row
  FEN; engine/best-defense evidence is still required elsewhere before any
  certified mate, draw, result, forced-line, or best-move claim can exist

Current admitted motif ids are:

- `loose_piece`
  - detector: `loose_piece_target_state`
  - carrier kind: `u_witness`
- `pin`
  - detector: `pin`
  - carrier kind: `u_witness`
- `fork`
  - detector: `fork`
  - carrier kind: `u_witness`
- `skewer`
  - detector: `skewer`
  - carrier kind: `u_witness`
- `overload`
  - detector: `overload`
  - carrier kind: `u_witness`
- `trapped_piece`
  - detector: `trapped_piece`
  - carrier kind: `root_atom`
- `mate_net`
  - detector: `MateNetCertification`
  - carrier kind: `certified_line`
- `perpetual_check`
  - detector: `PerpetualCheckHolding`
  - carrier kind: `certified_line`

`mate_net` and `perpetual_check` are certification-only motif mappings. Their
source rows may provide a human-facing pattern name only after the exact
certification carrier exists. The source row still does not claim mate, draw,
result, forced line, or best move. The runtime `SourceContextAdapter` remains
fail-closed for these motifs until the normalized input can carry an exact
certification carrier rather than a string `carrierKind` label alone.

Deferred or helper-required motif ids are:

- `discovered_attack`
- `deflection`
- `back_rank`
- `clearance`
- `interference`

These may appear only as rejected or future-helper material until a current
branch exact detector/helper contract exists. `back_rank_mate` is rejected as a
tag-only/result-style shortcut under this contract.

Motif rows must reject:

- tag-only examples
- wrong-board or cross-example detector refs
- mismatched `motifId` / detector-carrier pairs
- unknown motif ids
- line contexts without exact source/carrier parity
- line-test links without `line_test_link_is_not_proof`
- unknown fields
- missing `involvedSquares`
- missing `negativeBoundaries`
- truth, best, forced, result, engine, or oracle wording
- `mate_net` without `MateNetCertification`
- `perpetual_check` without `PerpetualCheckHolding`
- deferred/helper-required motif ids accepted without their future helper

Human-facing `displayName` values are renderer-safe candidates only. They are
not renderer output, not wording-strength authority, and not current-position
truth.

## Endgame Study

Endgame-study rows model known pattern applicability, not table outcomes.

Each study declares:

- `studyId`
- `displayName`
- `names`
- `materialClass`
- side-to-move constraints
- required piece placement rules
- required relation rules
- allowed transforms such as file mirror
- candidate plans as context tokens only
- source references
- `authority=endgame_study_context`
- `outcomeClaim=none`
- negative boundaries

Known study names are valid only after the row's exact material and placement
conditions are present. `materialClass`, `sideToMove`, `placementRules`, and
`relationRules` are checked against exact FEN-derived evidence before the row
can become context. Candidate plans are context tokens only; they are not best
moves, result verdicts, forced-conversion claims, tablebase truth, WDL/DTZ/DTM
claims, or Sxx admission.

Accepted fixture rows bind their source context to exact-board evidence refs:

- `endgame-study:<studyId>:applicable`
- `endgame-study-applicability:<fixture-id>`

Runtime adapter intake must not manufacture applicability from a free fixture
id. Endgame source input is admissible only after the exact fixture
material/placement/relation/side-to-move checks have already passed; the
prepared exact ref then carries `scope=exact_endgame_applicability` and
`route=<studyId>`. Unverified or scope-less fixture ids fail closed before
selection.

Runtime-prepared endgame technique context may add only source-context refs
shaped as:

- `endgame-technique:<role>:<context-ref>`
- `endgame-line-test:<prepared-proof-id>:context`

Allowed technique roles are `opposition`, `checking_distance`,
`rook_activity`, `pawn_ending_transition_risk`, `wrong_exchange`,
`bridge_setup`, `third_rank_setup`, `side_checking_setup`,
`method_exception`, `defender_resource`, and `hold_line`.

These refs frame technique only: opposition, checking distance, rook activity
or passivity, pawn-ending transition risk, wrong exchange, bridge setup,
third-rank setup, side-checking setup, method/exception, defender resource, or
hold line. They are not result truth, tablebase truth, forced conversion,
provenance, current-board truth, or renderer prose. If an endgame technique
context links to a prepared proof id, its boundary must include both
`endgame_technique_context_only` and `line_test_link_is_not_proof`; the actual
`PreparedVariationEvidence` must be owned by a separately admitted exact-board
claim. Source-context endgame claims must carry no prepared variation evidence
of their own.

Reject fixtures must also carry exact clean FENs; malformed boards are not
allowed to count as negative evidence.

The current executable fixture set covers seven accepted context rows:

- `lucena_rook_pawn`
- `philidor_rook_pawn`
- `vancura_rook_pawn`
- `basic_opposition_kpk`
- `distant_opposition_kpk`
- `wrong_rook_pawn_bishop`
- `rook_behind_passed_pawn`

Lucena, Philidor, and Vancura remain the existing rook-pawn-vs-rook rows.
Their subcontexts are admitted only as names or candidate-plan context tokens:
bridge building for Lucena, third-rank defense for Philidor, and checking from
the side for Vancura.

The newly admitted rows use simple exact-board evidence helpers:

- basic opposition: king-pawn-vs-king material plus same-line kings with one
  empty intervening square
- distant opposition: king-pawn-vs-king material plus same-line kings with an
  odd distant gap and an empty line between the kings
- wrong rook pawn: bishop + rook pawn vs king material, rook pawn on seventh,
  defender king near the promotion corner, and a pawn-side bishop color not
  matching that corner
- rook active behind passed pawn: rook-pawn-vs-rook material with the stronger
  rook behind the only passed pawn on the pawn file. Rook-pawn-vs-rook material
  requires one rook for the pawn side and one rook for the defender.

Candidate audit classification:

| Candidate | Classification | Contract status |
| --- | --- | --- |
| Lucena | existing board evidence sufficient | admitted |
| Philidor | existing board evidence sufficient | admitted |
| Vancura | existing board evidence sufficient | admitted |
| bridge building / Lucena subcontext | existing board evidence sufficient | admitted as Lucena context token |
| third-rank defense / Philidor subcontext | existing board evidence sufficient | admitted as Philidor context token |
| checking from side / Vancura subcontext | existing board evidence sufficient | admitted as Vancura context token |
| basic opposition | needs simple placement/relation helper | admitted |
| distant opposition | needs simple placement/relation helper | admitted |
| wrong rook pawn | needs simple placement/relation helper | admitted |
| rook pawn + wrong bishop | needs simple placement/relation helper | admitted through `wrong_rook_pawn_bishop` |
| rook active-behind-passed-pawn | needs simple placement/relation helper | admitted |
| outside passer | needs calculation/certification | deferred |
| fortress pattern | needs calculation/certification | deferred |
| rook on seventh | needs calculation/certification | deferred |
| triangulation | needs calculation/certification | deferred |
| corresponding squares | needs calculation/certification | deferred |
| shouldering | needs calculation/certification | deferred |
| breakthrough | needs calculation/certification | deferred |
| reserve tempo | needs calculation/certification | deferred |

Reject coverage includes label-only rows, wrong material class, missing
placement or relation evidence, unsupported transforms such as rank mirroring,
unknown truth fields, result/win/draw/loss outcome claims, forced-conversion or
tablebase/oracle candidate-plan wording, and rows without exact material or
applicability rules. Nasty negatives include wrong rook ownership, defender-owned
wrong bishops, loose Lucena rook geometry, and Vancura-shaped side-rank rooks
that are not actually checking from the side.
Runtime technique context additionally rejects deferred study tokens such as
`outside_passer`, `fortress_pattern`, `rook_on_seventh`, `triangulation`,
`corresponding_squares`, `shouldering`, `breakthrough`, and `reserve_tempo`;
result/oracle/forced-conversion wording; line-test links without
`line_test_link_is_not_proof`; and any attempt to use technique context as
line-proof ownership.

## Retrieval

Retrieval rows provide similar examples only.

The retrieval source manifest must remain a derived example index. It must not
declare opening `sourceUse` roles such as `master_reference`, must not declare
`aggregateUse`, and must not carry aggregate statistic authority.

Each row declares:

- `retrievalId`
- `sourceId`
- `sourceRef`
  - must be shaped as `retrieval-example:*`
- `exampleKind`
- exact `fen` for the example
- normalized `positionKey`
- `sideToMove`
- typed `similarityKey`
- `similarityScore`
- `similarityKind`
- `matchedFeatures`
- `sourceQuality`
- optional `gameMetadata`
  - `players`
  - `event`
  - `date`
  - `round`
  - `result`
  - `url`
- `licenseNotice`
- `attributionText`
- `snippetRole`
- `currentPositionClaim=false`
- `authority=retrieval_example`
- `tags`
- `negativeBoundaries`

Retrieved text or snippet metadata must never become current-position truth.

Allowed `similarityKind` values are:

- `exact_position`
- `opening_context`
- `motif_context`
- `endgame_study_context`
- `mixed_feature_context`

Similarity keys are generated source-context keys, not proof keys. They are a
closed typed schema by `similarityKind`, not an arbitrary JSON bag. They may
store example-side features such as exact normalized position identity,
canonical opening family or alias context, material signature, phase context,
pawn-structure tokens, motif detector-carrier refs, and endgame-study
applicability refs. `exact_position` keys must match the row `positionKey`;
opening pawn-structure tokens must match evidence derived from the example FEN;
motif carrier refs and endgame applicability refs must resolve to current local
fixture refs and match the declared motif/study plus the row FEN when
cross-source validation is available. `mixed_feature_context` inherits every
family-specific FEN/ref binding check for the feature families it declares;
declaring an opening, motif, or endgame-study family without its required
structure/carrier/applicability evidence fails closed. `exact_position` rows
that carry optional opening-family features inherit the same FEN-bound
structure check.
Similarity keys must not store raw engine
scores, tablebase verdicts, oracle fields, Sxx fields, result/outcome fields,
or current-position proof claims.

Retrieval may say only:

- a similar example exists
- the example shares opening, motif, endgame-study, or source features
- the row is a source-backed reference example

Retrieval must not say:

- best move
- theory truth
- forced line
- result or outcome prediction
- engine, tablebase, or oracle proof
- famous-player recommendation
- current-position proof

Citation metadata may be stored on retrieval rows, but player, event, round,
date, result, and URL display remains deferred until a later renderer/display
contract explicitly admits display-safe citation. The `result` field is game
metadata only; it is not a current-position verdict and must not influence
claim admission. If a row carries citation metadata, license and attribution
fields must be present.

Runtime-prepared retrieval illustration context may add only source-context refs
shaped as:

- `retrieval-illustration:<role>:<context-ref>`
- `retrieval-line-test:<prepared-proof-id>:context`

Allowed illustration roles are `comparable_line`, `similar_plan_sequence`,
`theme_example`, and `citation_context`.

These refs may say only that the retrieved row is a comparable example, shares
a plan sequence, demonstrates a theme, or carries bounded citation context.
They are not proof, provenance, current-board truth, recommendation, verdict,
game-result evidence, or renderer prose. If retrieval illustration context
links to a prepared proof id, its boundary must include both
`retrieval_illustration_context_only` and `line_test_link_is_not_proof`; the
actual `PreparedVariationEvidence` must be owned by a separately admitted
exact-board claim. Source-context retrieval claims must carry no prepared
variation evidence of their own. `displayCandidate` remains false/deferred
until a renderer citation contract explicitly admits display-safe citation.

Retrieval examples fail closed when:

- `sourceId` or `sourceRef` is missing
- `retrievalId` or `sourceRef` carries best, truth, result, forced, engine,
  oracle, Sxx, or current-position-proof wording
- `fen` or `positionKey` is missing
- `positionKey` does not match the normalized example FEN
- `sideToMove` does not match the example FEN
- similarity is below the frozen retrieval floor
- citation metadata is present without license and attribution text
- `sourceRef` duplicates an accepted row, or the normalized position,
  side-to-move, similarity kind, and similarity-key evidence duplicate an
  accepted row under a different source ref
- retrieved snippet, feature, or tag wording carries best, truth, result,
  forced, engine, oracle, current-position-proof language, or coded
  result/proof tokens such as WDL, DTZ, DTM, `1-0`, `0-1`, or `1/2-1/2`
- game result is used as a current-position claim
- illustration context uses result/verdict/recommendation/display wording
- line-test links omit `line_test_link_is_not_proof`
- `displayCandidate=true`
- opening similarity has the same opening family but lacks FEN-matched structure
  evidence
- motif similarity has a tag but no exact known detector-carrier ref, or borrows
  a detector carrier for a different motif/FEN
- exact-position similarity points at a different normalized position key
- exact-position similarity carries an opening structure token not evidenced by
  the example FEN
- endgame-study similarity references unknown or borrowed applicability
  evidence for a different study/FEN
- mixed-feature similarity declares opening, motif, or endgame-study labels
  without the required FEN-bound structure, detector carrier, or applicability
  evidence
- `currentPositionClaim=true`
- `negativeBoundaries` omits `retrieval_non_authoritative`
- unknown top-level, citation, or similarity-key fields appear

## Selector Fallback Boundary

The selection runtime may use these rows only as `contextOnly` when no stronger
exact-board lead exists, or as bounded context/support after an exact-board
lead is selected.

Prepared variation evidence remains outside the source-context adapter and
claim-boundary path. Source-context candidates carrying line-proof or raw PV
meaning are suppressed before outline rather than converted into board truth.

Opening fallback requires an active canonical `opening-position:*:canonical`
source ref and remains non-authoritative. `opening-position:*:ambiguous` rows
are downgraded or suppressed with `ambiguous_transposition`.

Motif fallback requires a `motif-example:*` source ref whose motif id matches
an exact `motif-detector-carrier:*` evidence ref. Optional
`motif-line-context:*` and `motif-line-test:*:context` refs remain
source-context evidence only. A motif source tag without that carrier, with a
mismatched carrier, with truth/Sxx/result/engine shortcut wording, with an
unsafe line-context boundary, or with a deferred/helper-required motif id such
as `back_rank_mate`, is suppressed with `forbidden_shortcut` and
`no_board_reason`.

Endgame-study fallback requires an `endgame-study:*:applicable` source ref
whose study id matches an exact `endgame-study-applicability:*` material and
placement carrier. Runtime-prepared candidates may bind a Phase 4 fixture id to
its study id only through exact carrier route metadata with
`scope=exact_endgame_applicability`; unbound, unverified, or scope-less fixture
ids do not satisfy selector fallback. Result-language, tablebase, or
forced-conversion wording fails closed. Optional `endgame-technique:*` and
`endgame-line-test:*:context` refs remain source-context evidence only.

Retrieval fallback requires a `retrieval-example:*` source ref, remains
non-authoritative reference context, and carries
`retrieval_non_authoritative`. Current-position truth or truth-promotion
retrieval ids are suppressed. Optional `retrieval-illustration:*` and
`retrieval-line-test:*:context` refs remain source-context evidence only; they
do not allow claim transfer from a retrieved game to the current board.

## Retrieval Quality Tooling

The local-only quality index builder lives under
`modules/commentary/src/test/tooling/retrieval/retrieval_quality_index.py`.
It reads only committed tiny source-context fixtures and writes generated
artifacts under the ignored root `tmp/commentary-opening/retrieval/`:

- `retrieval-index.jsonl`
- `retrieval-rejects.jsonl`
- `retrieval-summary.json`

Quality artifacts are quality/test outputs only. They are not runtime adapter,
synthesis, renderer, API, or frontend inputs. The quality builder must keep
generated rows non-authoritative, preserve citation metadata as metadata, and
fail closed for missing source refs, citation rows without attribution/license,
duplicate source refs, duplicate normalized similarity evidence, low
similarity, same-opening side/material mismatches, truth/result wording in
snippets, and `currentPositionClaim=true`.
Accepted quality rows require `licenseNotice` and `attributionText`. When the
quality tooling reads committed reject fixtures, generated reject rows preserve the
fixture `rejectReason` as the report `reason` and keep the quality validator's
local reason as `validatorReason`.

The quality summary carries a `qualityReport` for source-quality comparison:

- `qualityDecision=context_quality_pass` means accepted rows are usable only
  as local example/citation candidates, not as display prose or current-board
  claims.
- `highQualityExampleCount` counts accepted examples with strong local
  similarity scores and required exact evidence bindings.
- `lowConfidenceSuppressedCount` counts fail-closed rows that must not become
  display candidates.
- `displayCandidateCount` remains `0` until a later renderer citation contract
  explicitly admits display-safe citation.
- `nastyNegativeResults` records rejected false-positive classes: same-opening
  wrong structure, motif without exact carrier, side-to-move mismatch,
  duplicate examples, missing attribution/license, truth leakage, and low
  similarity.
- Quality buckets are computed from the validator reason. For committed reject
  fixtures, fixture-facing `reason` remains the authored `rejectReason`, while
  `validatorReason` is used for live quality buckets so the report cannot
  overstate a boundary that did not reach the matching validator path.
- `consumptionPolicy` freezes retrieval as an optional citation/example layer:
  default prose must not mention specific games, player names, events, or
  results unless a later display-safe citation contract allows it, and
  retrieval never outranks master-reference statistics, source statistics, or
  certification.

GameContext, practicality, tablebase, and endgame result-service source
families remain deferred or rejected outside this contract. Renderer output
must not convert source fallback into current-position truth.

## Generated Surface Boundary

Generation tools may draft schema templates, negative-boundary checklists,
context tag names, and renderer-safe phrasing fixtures for later review.

Generation tools must not invent opening lines, move statistics, source
license status, motif lines, endgame-study applicability, or retrieved example
facts.

## Executable Owner

The runnable source-context owners are:

- `SourceContextCorpusTest`
- `SourceContextAdapterContractTest`
- `SourceContextClaimBoundaryTest`
- `OpeningSourceToolingTest`
- `OpeningSourceConsumptionTest`
- `retrieval_quality_index_test.py`

They currently check:

- expected fixture files exist
- source-family inputs normalize only through `SourceContextAdapter`, and
  forbidden family claims are rejected before selection
- prepared runtime source-context candidates convert only to source-context
  `CommentaryClaim`s capped at `context_only`
- `SourceContextCandidate` is the normalized handoff shape; selection does not
  consume raw opening, motif, endgame-study, or retrieval rows
- active and deferred family classification
- license/provenance rejection
- opening position-key and candidate leakage rejection
- opening master-reference, online-trend, and pipeline-smoke consumption roles
- opening display/context alias taxonomy preservation
- 2013 game aggregate smoke-source boundary and trend-stat metadata rejection
- transposition downgrade behavior
- motif detector-carrier requirement, strict source/carrier parity, motif
  line-context/source proof-link containment, nested unknown-field rejection,
  and current backend extractor parity
- Lucena, Philidor, Vancura, basic opposition, distant opposition, wrong rook
  pawn, and rook-behind-passed-pawn applicability requirements
- endgame-study non-result boundary
- retrieval typed schema, exact `positionKey` / `sideToMove` parity,
  similarity-kind requirements, source-quality values, citation metadata
  containment, license/attribution requirements for citations, duplicate
  `sourceRef` rejection, low-similarity rejection, truth-wording rejection,
  game-result metadata containment, same-opening wrong-structure rejection,
  motif-tag-only rejection, illustration line-test containment, display
  candidate deferral, unknown-field rejection, and non-authoritative boundary
- retrieval quality indexing, citation/provenance, similarity, and local-only
  reporting
