# Opening Source Storage

This document fixes the storage and source boundary for opening source
collection before any bulk import runs.

It covers only the opening source family. Motif, endgame-study, retrieval,
game-context, renderer, API, frontend, and live runtime integration are outside
this document.

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

## Repo Material

The following files and code are allowed to be committed:

- opening source manifest schema and small manifest fixtures
- opening line, position, move-stat, theme, and reject fixture schemas
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
- `sourceFamily`, fixed to `opening`
- `sourceType`
- `sourceName`
- `sourceVersion`
- `sourceUrl`
- `licenseName`
- `licenseUrl`
- `licenseVerifiedAt`
- `redistribution`
- `derivedData`
- `attributionRequired`
- `rawStoragePolicy`
- `generatedStoragePolicy`
- `sourceChecksum`, when a stable source artifact exists

Allowed `sourceType` values are:

- `ecoTaxonomy`
- `masterGameDb`
- `publicPgn`
- `aggregateGameDb`
- `polyglotBook`
- `curatedLineSet`

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
- `candidateRole`
- `negativeBoundaries`

`candidateRole` may describe a row as a source statistic or reference
candidate. It must not describe the move as the strongest move, a forced move,
an objective verdict, or a current-board proof.

Rejected field names include:

- `bestMove`
- `theoryTruth`
- `forcedLine`
- `objectiveResult`
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
- move statistics without sample size or source scope
- candidate rows that use optimal, forced, result, or truth wording
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
