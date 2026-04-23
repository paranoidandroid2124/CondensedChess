# Source Context Contract

This document freezes the offline source-context contract for opening,
motif, endgame-study, and retrieval reference data.

It does not open a live planner, renderer, API, or frontend path.

The contract is deliberately sidecar-only:

- source rows may provide context, statistics, detector examples, study
  applicability, or similar examples
- source rows must not become Root, U, Object, Delta, Certification, or Sxx
  truth owners
- any current-position claim still needs the existing exact-board carrier for
  its layer

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
- `motif-examples.jsonl`
- `motif-reject-fixtures.jsonl`
- `endgame-studies.jsonl`
- `endgame-study-fixtures.jsonl`
- `endgame-study-reject-fixtures.jsonl`
- `retrieval-examples.jsonl`

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
- `license`
- `redistribution`
- `derivedData`
- `attributionRequired`
- `sourceUrl`

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

An opening candidate is only a statistic or context reference. It must not be
called an optimal move, theoretical truth, forced line, result claim, or current
board proof.

Opening name without an exact `positionKey` fails closed.

Ambiguous transpositions must be downgraded rather than indexed as canonical
context.

## Motif

Motif source tags are examples only.

An admitted motif example must bind:

- `motifId`
- exact FEN
- exact detector carrier
- involved squares
- source tags, if any
- negative boundaries

A motif tag without detector carrier fails closed. A motif label alone must not
admit a projection band or current-position claim.

## Endgame Study

Endgame-study rows model known pattern applicability, not table outcomes.

Each study declares:

- `studyId`
- `names`
- `materialClass`
- side-to-move constraints
- required piece placement rules
- required relation rules
- allowed transforms such as file mirror
- candidate plans as context tokens only
- source references
- negative boundaries

Known study names are valid only after the row's exact material and placement
conditions are present.

The current executable fixture set covers Lucena, Philidor, and Vancura
rook-pawn-vs-rook contexts. Lucena context requires rook-pawn-vs-rook material
plus rook, pawn, king, and defender-king applicability rules. Philidor context
requires defender king and defender rook rank relation rules. Vancura context
requires rook-pawn edge-file placement, defender king corner proximity, and a
lateral checking-rook relation. A study name without exact material class is
rejected.

Other endgame-study names remain future rows until they carry the same exact
material, placement, relation, fixture, and reject-fixture coverage.

Study context must not declare win, draw, loss, or forced conversion.

## Retrieval

Retrieval rows provide similar examples only.

Each row declares:

- source reference
- exact FEN for the example
- similarity key
- similarity score
- tags
- explicit non-authoritative status

Retrieved text or snippet metadata must never become current-position truth.

## LLM Boundary

An LLM may draft schema templates, negative-boundary checklists, context tag
names, and renderer-safe phrasing fixtures for later review.

An LLM must not invent opening lines, move statistics, source license status,
motif lines, endgame-study applicability, or retrieved example facts.

## Executable Owner

The runnable owner is
`SourceContextCorpusTest`.

It currently checks:

- expected fixture files exist
- active and deferred family classification
- license/provenance rejection
- opening position-key and candidate leakage rejection
- transposition downgrade behavior
- motif detector-carrier requirement
- Lucena, Philidor, and Vancura applicability requirements
- endgame-study non-result boundary
- retrieval non-authoritative boundary
