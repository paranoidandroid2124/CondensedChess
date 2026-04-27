# Opening Source Storage

This document fixes the storage and source boundary for opening source
collection before any bulk import runs.

It covers only the opening source family. The committed
`opening-sources.jsonl` file is a shared source-manifest fixture for the
source-context corpus; this document constrains only rows whose `sourceFamily`
is `opening`. Motif, endgame-study, retrieval, game-context, renderer, API,
frontend, and live runtime integration are outside this document.

## Findings

- Opening source contracts are repo material.
- Raw source data and ECO-wide generated aggregates are local-only material.
- The committed corpus may contain small validator fixtures that use the same
  schema as bulk output.
- Opening move candidates are statistics or references only. They do not own
  current-board truth, optimal-move claims, forced lines, or result claims.
- Opening source rows may provide source-backed context, line identity,
  transposition status, and move statistics. They must not become Root, U,
  Object, Delta, Certification, or Sxx truth owners.
- ECO/source taxonomy names are preserved source labels. Human-facing
  display/context names live in separate alias rows and do not rewrite source
  identity, `positionKey`, move statistics, or source taxonomy.
- The 2013-01 Lichess game aggregate is pipeline smoke data only. Product
  trend statistics require recent, bucketed, provenance-tracked aggregates.

## Repo Material

The following files and code are allowed to be committed:

- opening rows in the shared source manifest schema and small manifest fixtures
- opening line, position, move-stat, theme, and reject fixture schemas
- opening sequence-context fixtures that describe move-order, pawn-break,
  development-lag, file-ownership, king-safety, transposition, compensation,
  or master/online divergence context without creating recommendations
- tiny validator fixtures under
  `modules/commentary/src/test/resources/commentary-corpus/`
- offline parser and index-builder source code when it is test/tooling code
- validator tests that fail closed on bad source, license, key, or candidate
  rows
- documentation for source policy, generated-output policy, and authority
  boundary

The repo fixture filenames are:

- `opening-sources.jsonl`
- `opening-lines.jsonl`
- `opening-positions.jsonl`
- `opening-move-stats.jsonl`
- `opening-themes.jsonl`
- `opening-aliases.jsonl`
- `opening-context-candidates.jsonl`
- `opening-reject-fixtures.jsonl`

Repo fixture rows must stay small enough to review by hand. They are contract
fixtures, not the ECO-wide index.

## Local-Only Material

The following material must not be committed:

- raw PGN downloads
- decompressed PGN files
- large public or private game databases
- binary opening books
- paid or restricted third-party source dumps
- local parser cache
- local source checksums produced during collection
- ECO-wide generated line, position, move-stat, and theme aggregates
- temporary replay logs and ingest reports

The allowed local roots are:

- `tmp/commentary-opening/raw/`
- `tmp/commentary-opening/cache/`
- `tmp/commentary-opening/generated/`
- `tmp/commentary-opening/reports/`
- `modules/commentary/.local/opening/`

The fallback ignored fixture-adjacent output root is:

- `modules/commentary/src/test/resources/commentary-corpus/generated-opening/`

Builders must refuse an output path outside these ignored roots unless a later
storage decision explicitly admits that target.

## Source Manifest

Each committed or local manifest row must identify the source and its storage
policy before parsing starts:

- `sourceId`
- `sourceFamily`, fixed to `opening` for rows governed by this document
- `sourceType`
- `sourceUse`
- `sourceName`
- `sourceVersion`
- `parserVersion`
- `sourceUrl`
- `licenseName`
- `licenseUrl`
- `licenseVerifiedAt`
- `redistribution`
- `derivedData`
- `attributionRequired`
- `shareAlikeRequired`, when the source license requires share-alike
- `attributionText`, when attribution is required
- `licenseNotice`, when attribution or share-alike must be preserved
- `rawStoragePolicy`
- `generatedStoragePolicy`
- `sourceChecksum`, when a stable source artifact exists
- `rawArtifacts`, for local bulk runs, with path and checksum
- `generatedArtifacts`, for local bulk runs, with path, checksum, and row count
- `aggregateUse`, for game aggregate rows: `pipeline_smoke`,
  `online_trend_stat`, or `master_reference_stat`
- `sourceScope`, for dated or curated game aggregate rows
  - `full_month` only when the complete monthly source file was processed
  - `partial_month` when a run stops on an accepted-game cap
  - `streamed_sample` when a run stops on a read cap or stream sample
- `playEnvironment`, for online / OTB / mixed source separation
- `playerLevel`, `ratingSystem`, `minElo`, and `titlePolicy`, for
  master-reference source admission
- `timeScope`, `periodStart`, and `periodEnd`, for dated source windows
- `timeControlScope`, for classical / rapid / fast / mixed separation
- `perPositionSampleSizeThreshold`, for master-reference row admission
- `dedupePolicy`, for duplicate-game handling before aggregation
- `annotationPolicy`, for comment / engine-eval handling before aggregation
- `sourceYear` and `sourceMonth`, for game aggregate rows
- `yearMonth`, matching `sourceYear` and `sourceMonth`, for trend-stat rows
- `ratingBucket`, `timeControlBucket`, `variant`, and `ratedFilter`, for
  trend-stat aggregate rows
- `ratedOnly`, fixed to `true` for trend-stat rows in this opening lane
- `sampleSizeThreshold`, for trend-stat aggregate rows

Allowed `sourceType` values are:

- `ecoTaxonomy`
- `masterGameDb`
- `publicPgn`
- `aggregateGameDb`
- `trend_stat`
- `polyglotBook`
- `curatedLineSet`

Allowed `sourceUse` values for opening rows are:

- `taxonomy_reference`
- `pipeline_smoke`
- `online_trend`
- `master_reference`

Allowed `rawStoragePolicy` values are:

- `localOnly`
- `externalOnly`

Allowed `generatedStoragePolicy` values for ECO-wide output are:

- `localOnly`
- `localCache`

Unknown license, unknown redistribution, missing source id, missing source URL,
or a policy that would commit raw source data fails closed.

## Bulk Artifact Layout

Bulk import output uses the same row schemas as the committed fixtures, but it
is written only under ignored local roots:

- `tmp/commentary-opening/generated/<sourceId>/opening-lines.jsonl`
- `tmp/commentary-opening/generated/<sourceId>/opening-positions.jsonl`
- `tmp/commentary-opening/generated/<sourceId>/opening-move-stats.jsonl`
- `tmp/commentary-opening/generated/<sourceId>/opening-themes.jsonl`
- `tmp/commentary-opening/reports/<sourceId>/opening-rejects.jsonl`

The generated output must remain reproducible from the manifest plus local raw
input. A later lane may choose a deployable artifact format, but this document
does not admit any live runtime artifact.

## Game Aggregate Use

The existing `lichess-games-standard-2013-01` local aggregate and the committed
`lichess-games` fixture are pipeline smoke sources. They validate replay,
position matching, stat aggregation, and candidate-boundary checks. They must
not be presented as product-quality current trend data.

An online-trend source must declare:

- `sourceType`: `trend_stat`
- `sourceUse`: `online_trend`
- `aggregateUse`: `online_trend_stat`
- source month and year
- `yearMonth`
- rating bucket
- time-control bucket
- `variant`, fixed to `standard` for this opening lane
- rated/casual filter
- `ratedOnly`, fixed to `true`
- sample-size threshold
- parser version
- checksum and provenance in the source manifest
- local-only generated storage policy

Recent collection priority is:

- recent Lichess standard rated months, preferably from 2024 through 2026
- optional high-rated bucket
- optional rapid/classical and blitz split

Recent raw and generated artifacts are admitted only under ignored local roots.
The current recent trend source fixture names the March 2026 Lichess standard
rated all-bucket source; bucket-split aggregates remain a later collection
decision unless a local run emits separate source ids and manifests for each
bucket.

## Master Reference Use

The initial master-reference target is the Lichess Broadcast PGN dump family,
modeled as recent OTB/broadcast master-reference source context. All-time master
corpora remain deferred until a license-clean, provenance-clean source is
selected.

A master-reference source must declare:

- `sourceType`: `masterGameDb`
- `sourceUse`: `master_reference`
- `aggregateUse`: `master_reference_stat`
- `licenseName`: `CC-BY-SA-4.0`
- `attributionRequired`: `true`
- `shareAlikeRequired`: `true`
- `attributionText`
- `licenseNotice`
- `sourceScope`
- `playEnvironment`, initially `otb`
- `playerLevel`, initially `master`, with title/rating policy
- `ratingSystem`, initially `fide`
- `minElo` or `titlePolicy`
- `timeScope`, `periodStart`, and `periodEnd`
- `timeControlScope`, initially `classical_rapid`
- `perPositionSampleSizeThreshold`
- `dedupePolicy`
- `annotationPolicy`
- `parserVersion`
- `sourceChecksum`
- local-only raw and generated storage policy

Blitz-only or bullet-only source windows are not master-reference input in this
contract. They must be excluded or admitted later as a separate online/fast
bucket. Mixed OTB/online source input must use an explicit mixed scope; an
accidental mixed source fails closed.

Master-reference move stats are source-backed statistics or context references.
They may support phrasing such as common master-reference choice in the named
source scope. They do not prove the strongest move, theory truth, a forced line,
an engine result, or a current-position verdict.

When master-reference and online-trend aggregates cover the same position, their
rankings stay separate. The default educational source policy is:

- use `master_reference` as the primary educational reference statistic
- use `online_trend` only as secondary trend/context evidence

Opening sequence context is stored as context metadata, not as product prose.
It may link to a public-safe variation proof id only through a context boundary
such as `line_test_link_is_not_proof`; raw opening rows, raw move-stat rows,
and source frequencies remain outside renderer/backend public payloads.
`pipeline_smoke` is validator data only, and `taxonomy_reference` is identity
only; neither source use can produce product candidates.
- keep `pipeline_smoke` excluded from product trend/reference reporting
- require engine or certification evidence for strongest-move, objective,
  forced-line, or result claims

Comparison reports are local-only audit artifacts under
`tmp/commentary-opening/reports/<comparison-id>/`. They may list common
positions, source-only positions, disagreement examples, and low-confidence
rows, but they do not create a combined ranking.

## Opening Consumption Contract

The backend consumption shape is a structured `OpeningContextCandidate`, not
renderer-ready prose. The executable fixture lives in
`opening-context-candidates.jsonl` and is validated only by test/tooling code.
No runtime adapter, synthesis, renderer, API, or frontend path is opened by
this storage contract.

Source selection is fixed as:

- `master_reference` is the primary educational reference statistic source
- `online_trend` is secondary trend/context evidence
- `pipeline_smoke` is excluded from product consumption
- `taxonomy_reference` supplies identity only

The candidate shape is:

- `positionKey`
- `openingIdentity`, preserving source taxonomy row id, ECO, source name,
  canonical name, family, and variation
- optional display/context alias, preserving the exact alias row
- `primaryReferenceStats`, only from `master_reference_stat`
- `secondaryTrendStats`, only from `online_trend_stat`
- `confidence`
- `boundaries`
- `sourceRefs`

Confidence is sample-gated. A master-reference row below its
`perPositionSampleSizeThreshold` is weak or suppressed rather than presented as
usable educational evidence. The current master-reference aggregate contains
many below-threshold rows, so consumers must expect low-confidence or
suppressed candidates even when a source row exists. Online-trend rows alone
may create only a secondary-only candidate. High or usable confidence still
means source-backed context only; it never creates strongest-move, theoretical,
forced-line, result, engine, or result-service evidence.

Master-reference and online-trend rankings must remain separate vectors. A
later consumer may report a source-context disagreement, but it must not merge
the two rankings into one ordering. Specific game, event, player, or game-URL
mentions are not carried by `master_reference_stat`; they are deferred to a
retrieval/citation layer.

Opening consumption fails closed when:

- there is no exact `positionKey`
- there is no taxonomy row for the opening identity
- an alias references no exact taxonomy row or rewrites source taxonomy
- only `pipeline_smoke` rows are available
- a source row lacks `sourceUse` or other manifest metadata required by this
  contract
- a move-stat row uses strongest-move, theory, forced-line, result, engine, or
  result-service wording
- a stat/candidate row carries specific-game citation fields

## Master Reference Aggregate Ingest

The local broadcast master ingest runner lives under test tooling:

- `modules/commentary/src/test/tooling/opening/broadcast_master_ingest.py`

It is an offline parser/indexer, not live runtime code. It reads a local
Lichess Broadcast PGN or PGN.zst artifact and writes only ignored local output:

- `tmp/commentary-opening/raw/<sourceId>/...`
- `tmp/commentary-opening/generated/<sourceId>/source-manifest.json`
- `tmp/commentary-opening/generated/<sourceId>/opening-move-stats.jsonl`
- `tmp/commentary-opening/reports/<sourceId>/summary.json`
- `tmp/commentary-opening/reports/<sourceId>/rejects.jsonl`
- `tmp/commentary-opening/reports/<sourceId>/dedupe.jsonl`
- `tmp/commentary-opening/reports/<sourceId>/annotation-report.json`
- `tmp/commentary-opening/reports/<sourceId>/position-sample-distribution.json`
- `tmp/commentary-opening/reports/<sourceId>/opening-family-coverage.json`
- `tmp/commentary-opening/reports/<sourceId>/low-sample-rows.jsonl`

The runner must not overclaim coverage. `sourceScope` is `full_month` only when
the run reaches end-of-input for the monthly source artifact. Capped reads are
`streamed_sample`; accepted-game caps are `partial_month`.

Generated move-stat rows carry local metadata:

- `sourceUse`: `master_reference`
- `aggregateUse`: `master_reference_stat`
- `candidateKind`: `statistical_reference`
- `currentPositionTruth`: `false`
- `positionSampleSize`
- `perPositionSampleSizeThreshold`
- `sampleConfidence`: `below_threshold` or `meets_threshold`

The runner strips or ignores comments, NAGs, clock comments, engine-eval
comments, and textual advice comments before aggregation. Only legal mainline
moves count. The annotation report records contamination counts; annotations
must not become move-stat truth.

The runner admits only recent broadcast standard games with valid results,
broadcast provenance, classical/rapid time controls, and title/rating policy
metadata. Fast games, missing time controls, missing player-level metadata,
duplicate game ids / normalized PGN hashes, setup/FEN games, and non-standard
variants fail closed into reports.

## Opening Lines

Opening line rows carry taxonomy and move-order identity:

- `lineId`
- `sourceId`
- `ecoCode`
- `openingFamily`
- `openingVariation`
- `names`
- `moveOrder`
- `lineKey`
- `sourceRefs`
- `negativeBoundaries`

Move order must replay legally from the initial position. A name or ECO code
without a replayed position sequence is not enough for admission.

## Opening Positions

Position rows are keyed by normalized board state:

- `positionKey`
- `sourceId`
- `lineIds`
- `fen`
- `ply`
- `sideToMove`
- `castlingRights`
- `enPassantSquare`
- `halfmoveClockPolicy`
- `fullmoveNumberPolicy`
- `transpositionState`
- `sourceRefs`
- `negativeBoundaries`

`positionKey` must be transposition-safe and derived from normalized FEN fields
that affect legal move generation and opening context. A transposition that
cannot be assigned unambiguously must be rejected or downgraded; it must not be
indexed as canonical opening context.

## Move Statistics

Move-stat rows carry source-backed frequency and result statistics:

- `positionKey`
- `sourceId`
- `move`
- `sampleSize`
- `frequency`
- `resultStats`
- `statScope`
- `sourceRefs`
- `candidateKind`
- `negativeBoundaries`

`candidateKind` may describe a row as a source statistic or reference
candidate. It must not describe the move as the strongest move, a forced move,
an objective verdict, or a current-board proof.

Rejected field names include:

- `bestMove`
- `bestLine`
- `theoryTruth`
- `forcedLine`
- `forcedContinuation`
- `objectiveResult`
- `objectiveVerdict`
- `engineVerdict`
- `engineProof`
- `oracleVerdict`

## Opening Themes

Theme rows carry context labels only:

- `positionKey`
- `sourceId`
- `themeId`
- `planRefs`
- `breakRefs`
- `sourceRefs`
- `negativeBoundaries`

Plans and breaks are source-backed context references. They must not become
certified current-position claims without the existing exact-board evidence
needed by the consuming layer.

## Opening Aliases

Alias rows separate source taxonomy from human-facing copy:

- `sourceName`
- `canonicalName`
- `openingFamily`
- `openingVariation`
- `displayName`
- `contextName`
- `aliasKind`
- `sourceRefs`
- `negativeBoundaries`

Allowed `aliasKind` values are:

- `display_alias`
- `context_alias`
- `structure_alias`

Alias rows must reference an exact taxonomy row. The source name, canonical
name, family, and variation must match that taxonomy row. A missing taxonomy
row fails closed.

Aliases may be renderer-facing candidates in a later lane, but this document
does not connect them to renderer runtime. They do not change `positionKey`,
opening source identity, source taxonomy, move stats, or candidate moves.

Alias wording must not contain authority terms such as optimal-move, forced,
result, engine, or result-service wording. Closed-Spanish wording is accepted
only for a taxonomy variation that explicitly supports that context.

## Alias Catalog Work Order

Alias catalog expansion is curated by opening family. Do not automatically
alias every variation.

The current local taxonomy output contains 3,690 opening-line rows. The first
curation pass uses this order:

1. Spanish / Italian / Open Games
2. Sicilian
3. French / Caro-Kann / Scandinavian / Alekhine
4. QGD / QGA / Slav / Semi-Slav / Catalan
5. Indian Defenses
6. Flank / English / Reti / Dutch / Benoni / Modern / Pirc
7. Gambits / irregular / rare systems
8. Final audit / dedupe / packaging

Rows are eligible for an alias only when:

- an exact source taxonomy row exists
- `sourceName`, `canonicalName`, `openingFamily`, and `openingVariation`
  preserve the source taxonomy identity
- the alias is common, stable, and short enough to be display/context copy
- the alias maps to one reviewed taxonomy row, not to a whole family by
  implication
- source references identify the exact row that admits the alias
- negative boundaries state that the alias is not position truth, move-stat
  truth, source-taxonomy rewrite, optimal-move claim, forced line, or result
  claim

Use `display_alias` for human shorthand such as `Open Catalan` or `Najdorf
Sicilian`.

Use `context_alias` for source-backed opening context such as `Catalan with
...dxc4` or `Ruy Lopez setup`.

Use `structure_alias` only when the exact taxonomy row supports a named
structure or plan context, such as `Carlsbad context` for QGD Exchange. The
structure alias still does not certify the current board structure by itself.

Skip or reject alias candidates when:

- the source taxonomy row is missing
- the candidate phrase can name multiple source rows without an exact row
  decision
- the phrase depends on a transposition-sensitive move order not represented
  by the row
- the candidate tries to rename the source family or variation
- the wording implies an optimal move, forced continuation, result, engine
  verdict, or current-position proof
- the nickname is too colloquial, promotional, or unsupported by source
  taxonomy
- a structure alias would require board evidence outside the opening row

Repo material may include accepted hand-reviewed alias fixtures, reject
fixtures, validator code, tests, and this documentation. Local-only material
may include generated candidate lists, review scratchpads, dedupe reports, and
bulk alias coverage reports under ignored opening roots. Candidate lists are
not repo artifacts until each row has been curated into a small fixture.

The verification set for alias catalog work is:

- `sbt "commentary/testOnly lila.commentary.source.SourceContextCorpusTest lila.commentary.source.opening.OpeningSourceToolingTest lila.commentary.source.opening.OpeningSourceConsumptionTest"`
- `sbt "commentary/testOnly lila.commentary.CommentaryCoreBoundaryTest"`
- `git diff --check`
- `git status --short --untracked-files=all`
- `git status --short --ignored --untracked-files=all tmp/commentary-opening modules/commentary/.local/opening modules/commentary/src/test/resources/commentary-corpus/generated-opening`

## Parser And Builder Boundary

`OpeningIndexBuilder` and any PGN or book parser are offline tooling until a
later runtime decision exists.

Allowed placement:

- `modules/commentary/src/test/scala/lila/commentary/source/opening/`
- a dedicated test/tooling package with the same runtime isolation

Disallowed placement for the first collection lane:

- `modules/commentary/src/main/...`
- renderer packages
- API controllers
- frontend code

The builder may read local-only raw data, emit ignored local aggregates, and
write small reject fixtures only when the user explicitly asks for fixture
updates.

## Fail-Closed Checks

Opening validation must reject:

- unknown license or redistribution status
- missing `sourceId`
- non-opening `sourceFamily`
- raw source data marked as commit-allowed
- generated ECO-wide aggregate output outside ignored local roots
- opening name without exact `positionKey`
- alias row without an exact source taxonomy row
- alias row that rewrites source taxonomy fields or includes current-position
  or move-stat fields
- alias wording that claims optimal, forced, result, engine, or truth authority
- move statistics without sample size or source scope
- candidate rows that use optimal, forced, result, or truth wording
- trend-stat aggregate rows missing month/year, rating bucket, time-control
  bucket, variant, rated/casual filter, sample threshold, parser version,
  checksum/provenance, or local-only generated storage
- transposition ambiguity admitted as canonical context
- LLM-created opening lines, statistics, source license facts, or master-game
  facts

## Authority Boundary

Opening source artifacts may enter later certification or synthesis only as
opening context, taxonomy, source reference, or statistic input.

They cannot certify:

- current-board correctness
- tactical or strategic truth
- result outcome
- forced conversion
- renderer wording
- API response shape
- frontend behavior

Any such use needs a later explicit integration decision and exact-board
evidence from the appropriate owner.
