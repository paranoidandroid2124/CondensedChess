# Root Index Freeze

This document freezes the canonical index layout for the `2891`-dimensional
root-state vector.

It exists so that:

- extractor output
- corpus expectations
- debug traces
- witness input contracts

all refer to the same immutable bit identities.

Changing this file after extractor work begins is a migration event, not a
refactor.

## Canonical Local Orderings

### Color Order

| Value | Color |
| ---: | --- |
| `0` | white |
| `1` | black |

### Piece Order

| Value | Piece |
| ---: | --- |
| `0` | pawn |
| `1` | knight |
| `2` | bishop |
| `3` | rook |
| `4` | queen |
| `5` | king |

### File Order

| Value | File |
| ---: | --- |
| `0` | a |
| `1` | b |
| `2` | c |
| `3` | d |
| `4` | e |
| `5` | f |
| `6` | g |
| `7` | h |

### Square Order

The canonical square order is little-endian rank-file:

`a1 = 0, b1 = 1, ..., h1 = 7, a2 = 8, ..., h8 = 63`

### Pawn-Square Order

Pawn-scoped schemas use the legal pawn-rank basis only:

- ranks `2..7`
- files `a..h`
- total `48` indices per color

Canonical order:

`a2 = 0, b2 = 1, ..., h2 = 7, a3 = 8, ..., h7 = 47`

## Global Schema Offsets

| Schema | Start | Size | End |
| --- | ---: | ---: | ---: |
| `piece_on(c,p,s)` | 0 | 768 | 767 |
| `controlled_by(c,s)` | 768 | 128 | 895 |
| `pawn_controlled_by(c,s)` | 896 | 128 | 1023 |
| `contested(s)` | 1024 | 64 | 1087 |
| `open_file(f)` | 1088 | 8 | 1095 |
| `half_open_file(c,f)` | 1096 | 16 | 1111 |
| `king_ring_square(c,s)` | 1112 | 128 | 1239 |
| `weak_square(c,s)` | 1240 | 128 | 1367 |
| `outpost_square(c,s)` | 1368 | 128 | 1495 |
| `isolated_pawn(c,s)` | 1496 | 96 | 1591 |
| `backward_pawn(c,s)` | 1592 | 96 | 1687 |
| `doubled_file(c,f)` | 1688 | 16 | 1703 |
| `passed_pawn(c,s)` | 1704 | 96 | 1799 |
| `candidate_passer(c,s)` | 1800 | 96 | 1895 |
| `fixed_pawn(c,s)` | 1896 | 96 | 1991 |
| `loose_piece(c,s)` | 1992 | 128 | 2119 |
| `pinned_piece(c,s)` | 2120 | 128 | 2247 |
| `overloaded_piece(c,s)` | 2248 | 128 | 2375 |
| `trapped_piece(c,s)` | 2376 | 128 | 2503 |
| `xray_target(c,s)` | 2504 | 128 | 2631 |
| `lever_available(c,s)` | 2632 | 96 | 2727 |
| `king_shelter_hole(c,s)` | 2728 | 128 | 2855 |
| `side_to_move` | 2856 | 2 | 2857 |
| `castling_rights` | 2858 | 16 | 2873 |
| `en_passant_state` | 2874 | 17 | 2890 |

## Index Formulas

### Color-Square Schemas (`128`)

Used by:

- `controlled_by`
- `pawn_controlled_by`
- `king_ring_square`
- `weak_square`
- `outpost_square`
- `loose_piece`
- `pinned_piece`
- `overloaded_piece`
- `trapped_piece`
- `xray_target`
- `king_shelter_hole`

Formula:

`index = start + 64 * colorIndex + squareIndex`

### Color-Piece-Square Schema (`768`)

Used by:

- `piece_on`

Formula:

`index = start + 384 * colorIndex + 64 * pieceIndex + squareIndex`

### Color-Pawn-Square Schemas (`96`)

Used by:

- `isolated_pawn`
- `backward_pawn`
- `passed_pawn`
- `candidate_passer`
- `fixed_pawn`
- `lever_available`

Formula:

`index = start + 48 * colorIndex + pawnSquareIndex`

### Color-File Schemas (`16`)

Used by:

- `half_open_file`
- `doubled_file`

Formula:

`index = start + 8 * colorIndex + fileIndex`

### Neutral-Square Schema (`64`)

Used by:

- `contested`

Formula:

`index = start + squareIndex`

### Neutral-File Schema (`8`)

Used by:

- `open_file`

Formula:

`index = start + fileIndex`

## Auxiliary One-Hot Ordering

Ontology-level aux identity is one-hot even if a runtime implementation later
packs it more tightly.

### Side To Move (`2856..2857`)

| Index | State |
| ---: | --- |
| `2856` | `white_to_move` |
| `2857` | `black_to_move` |

### Castling Rights (`2858..2873`)

Castling rights use the mask ordering:

- `K = 1`
- `Q = 2`
- `k = 4`
- `q = 8`

Index rule:

`index = 2858 + rightsMask`

Examples:

| Rights | Mask | Index |
| --- | ---: | ---: |
| `-` | 0 | 2858 |
| `K` | 1 | 2859 |
| `Q` | 2 | 2860 |
| `KQ` | 3 | 2861 |
| `k` | 4 | 2862 |
| `KQkq` | 15 | 2873 |

### En Passant State (`2874..2890`)

The canonical one-hot meaning is:

- `2874 = none`
- `2875..2882 = white_can_capture_on_file_a..h`
- `2883..2890 = black_can_capture_on_file_a..h`

The color here is the side that can perform the en passant capture.

Examples:

| State | Index |
| --- | ---: |
| `none` | 2874 |
| `white_can_capture_on_file_d` | 2878 |
| `black_can_capture_on_file_e` | 2887 |

## Worked Examples

| Atom | Formula result |
| --- | --- |
| `piece_on(white,king,e1)` | `0 + 384*0 + 64*5 + 4 = 324` |
| `weak_square(white,d5)` | `1240 + 64*0 + 35 = 1275` |
| `fixed_pawn(black,c6)` | `1896 + 48*1 + 34 = 1978` |
| `side_to_move = white_to_move` | `2856` |
| `castling_rights = KQkq` | `2873` |

## Freeze Rules

- do not reorder schemas once extractor work begins
- do not reorder local index conventions once corpus rows exist
- do not replace canonical one-hot aux identity with packed-runtime identity
- if a migration becomes unavoidable, version the corpus and explicitly remap
  the old indices
