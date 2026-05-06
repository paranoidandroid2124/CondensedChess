# Board Facts Charter

This document is live authority for Stage 1.

Stage 1 name is `Board Facts`.

Board state observes. Story proves.

`Board Facts` is the first open observation layer after the closed kernel. It
records only small facts visible from the current board. It does not create
commentary, evaluate plans, prove tactics, choose a Story, or open any public
surface.

## Scope

Small board facts may be recorded only when they are directly visible from the current board and each recorded fact binds to the same board root.
Stage 1 facts are material for proof-bearing `Story` work; they are not public
claims.

Allowed observation families are chess-readable:

- Legal Moves
- Attacks
- Guards
- Lines
- Pawns
- Pieces
- King Safety
- Loose Piece
- Open File
- Pawn Lever

These names are observation names only. A named board fact is still only an observation.

## Stage 1 Slices

Stage 1 proceeds in six slices. Each slice may add only small current-board
observations. Each slice must name its forbidden public claims.

### Slice 1 - Board Facts index / facts.seen ledger

`facts.seen` is the internal ledger of what the board observation layer saw.
It is an observation list, not a claim list. Renderer, LLM, public route, and
frontend mock code must not read it directly as explanation input.

Completion standard:

- `facts.seen` is an observation ledger.
- `facts.seen` is not a public claim list.
- missing evidence logs are present.
- no public or renderer path reads `facts.seen` directly.

### Slice 2 - Piece facts

Allowed observations:

- attacked piece
- guarded piece
- attacked and unguarded piece
- loose piece observation
- attacked_piece_guard_map
- piece_under_attack
- attacked_unguarded_piece

Forbidden public claims:

- free piece
- hanging
- wins material
- can be taken
- blunder
- tactical target

Loose piece language is internal observation language only. It means no current
same-side guard was observed for the piece; it does not mean the piece can be
won.

### Slice 3 - Line facts

Allowed observations:

- pin-to-king line
- blocker
- ray
- file line
- rank line
- diagonal line
- x-ray shape

Forbidden public claims:

- pin wins material
- discovered attack works
- x-ray tactic
- skewer
- forced tactic

Line means geometry. It is not a tactic.

Slice 3 rows may name a line, ray, blocker, pin-to-king line, or x-ray shape
only as current-board geometry. They must not say that the blocker can move,
that the ray wins material, or that the x-ray shape is a tactic.

### Slice 4 - File facts

Allowed observations:

- open file
- semi-open file
- rook on file
- rook_open_file_entry
- open_file_observation
- semi_open_file_observation
- legal_file_entry_move
- file blocker
- file target square

Forbidden public claims:

- controls the file
- dominates the file
- file pressure is decisive
- rook invasion works
- opponent cannot contest the file

Open file is observation. File control is interpretation.

Slice 4 rows may name pawnless files, semi-open files for one side, rooks
standing on files, legal rook entry moves, file blockers, and entry target
squares. They must not say that the file is controlled, dominated, decisive,
usable, or uncontestable.

### Slice 5 - Pawn and square facts

Allowed observations:

- pawn lever
- pawn can challenge square
- pawn cannot currently challenge square
- pawn-safe square observation
- front blocker
- passed pawn observation
- isolated pawn observation
- backward pawn front square
- pawn_safe_square
- no_current_pawn_chase
- piece_reachable_square
- square_guard_map

Forbidden public claims:

- outpost
- weak square is the strategic key
- minority attack
- bad structure
- permanent weakness
- good plan
- fixed target

Weak square language, if it appears at all, is a candidate observation only.
It is not an outpost, strategic key, or move recommendation.

Slice 5 rows may name pawn contact, pawn-safe candidate squares, front
blockers, current pawn structure observations, legal reachable squares, and
square guard maps. They must not say that a square is an outpost, a permanent
weakness, a fixed target, a structure verdict, or a plan.

### Slice 6 - King ring facts and Board Facts closure

Allowed observations:

- king square
- king ring squares
- attacked king-ring squares
- king-ring defenders
- legal escape squares
- contact check observations
- line to king
- blocker near king

Forbidden public claims:

- king is unsafe
- mate net
- winning attack
- dangerous attack
- forced mate
- no escape

Slice 6 rows may name king squares, ring squares, current ring attacks,
current ring defenders, legal king moves, contact-check observations, lines to
the king, and nearby blockers. They must not say that the king is unsafe, that
an attack is dangerous or winning, that a mate net exists, that mate is forced,
or that there is no escape.

The closure gate checks every Stage 1 slice:

- every Board Fact is bound to the same board.
- every Board Fact has the needed side, piece, square, file, rank, line, or
  legal move coordinates.
- missing data stays `0`/silent.
- no Board Fact becomes a public claim.
- renderer, LLM, public route, and frontend mock paths remain closed.
- failure logs say why the system still cannot speak.

## Required Binding

Every Board Fact must identify the chess coordinates it uses:

- same board root
- same-board producer proof
- side
- piece and man, when a piece fact is recorded
- square, file, rank, or line, when geometry is recorded
- legal move, when the fact depends on move legality
- attack, guard, blocker, or pin-to-king relation, when the fact depends on
  board contact

Missing or unproven data stays `0`/silent. No missing field may be inferred by
the renderer, LLM, public route, or a later `Story` row.

## Allowed Observations

Stage 1 may say internally that a board fact exists:

- the knight on e5 is attacked by Black
- the c-file has a rook entry square for White
- the d5 square has no current opposing pawn lever
- several squares around the black king are attacked by White
- a piece is attacked and not currently guarded
- a pin-to-king line is present on the same board
- a slider, blocker, and second piece share an x-ray shape on one line
- a rook has a legal entry move to a pawnless or semi-open file square
- a square is currently not challenged by opposing pawns
- a king-ring square is attacked and also has a named defender

Each row must keep its side, piece, square, file, rank, line, or legal move
binding. If the binding cannot be recorded, the fact is silent.

## Runtime Surface

Strict `BoardFacts.fromFen` and internal `BoardFacts.fromPosition` expose
current-board observations through `facts.seen`.

`facts.seen` may contain:

- `LegalMove` rows with side, piece, and line
- `Attack` rows with attacker and target
- `Guard` rows with guard and target
- `PieceUnderAttack` rows with piece and attackers
- `GuardedPiece` rows with piece and guards
- `AttackedUnguardedPiece` rows with piece and attackers
- `LoosePieceObservation` rows with piece only
- `LineObservation` rows with file, rank, or diagonal geometry between pieces
- `Ray` rows with side, slider piece, line kind, line, and blockers
- `LineBlocker` rows with side, slider piece, blocker, line kind, and line
- `XRayShape` rows with side, slider piece, screen, target, line kind, and line
- `Pin` rows with side, king, pinned piece, attacker, and line
- `PawnLever` rows with side, pawn, target, and line
- `PawnChallenge` rows with side, pawn, challenged square, and line
- `PawnCannotChallengeSquare` rows with side, square, and opposing pawn side
- `PawnSafeSquareObservation` rows with side, square, and opposing pawn side
- `NoCurrentPawnChase` rows with side, square, and opposing pawn side
- `FrontBlocker` rows with side, pawn, blocker, square, and line
- `PassedPawnObservation` rows with side and pawn
- `IsolatedPawnObservation` rows with side and pawn
- `BackwardPawnFrontSquare` rows with side, pawn, square, and line
- `PieceReachableSquare` rows with side, piece, square, and legal line
- `SquareGuardMap` rows with side, square, and guarding pieces
- `OpenFile` rows with file and legal rook entry lines
- `OpenFileObservation` rows with file
- `SemiOpenFileObservation` rows with side and file
- `RookOnFile` rows with side, rook, and file
- `LegalFileEntryMove` rows with side, piece, file, and legal line
- `RookOpenFileEntry` rows with side, rook, file, and legal line
- `FileBlocker` rows with side, file, and blocker piece
- `FileTargetSquare` rows with side, file, square, and legal line
- `KingSquare` rows with side and king
- `KingRingSquare` rows with side, king, and ring square
- `KingRingAttack` rows with king side, king square, attacked ring square, and
  attacking piece
- `KingRingDefender` rows with side, king, ring square, and defender
- `LegalEscapeSquare` rows with side, king, square, and legal line
- `ContactCheckObservation` rows with king side, king, checking piece, and legal
  line
- `LineToKing` rows with side, king, line piece, line kind, line, and blockers
- `BlockerNearKing` rows with side, king, blocker, line piece, line kind, and
  line
- `MissingEvidence` rows listing missing evidence

Manual or untrusted `BoardFacts` assembly must produce empty `facts.seen`
observations and a missing-evidence log. The expected missing evidence includes
same-board producer proof, same board root, board header, legal moves, attacks,
pieces, pawns, or piece list.

## Public Claim Ban

Open file, pin, weak square, loose piece, and pawn lever are board fact names,
not public claim names. Giving a chess name to a fact does not make it safe to
speak.

Stage 1 must not say or enable these public claims:

- the knight is free
- controls the c-file
- the outpost is strategically central
- the king is unsafe
- this is a good plan
- counterplay is stopped

Those claims require `Story` proof with side, target, anchor, route, rival,
required legal line, and same-root proof sidecar. `Board Facts` cannot supply
that proof by name, count, heat, or scalar value.

No renderer, LLM, public route, template, frontend mock, or API transport may read Board Facts directly as commentary.
Board facts may flow only into later same-root proof work after that work has
its own live authority and tests.

## Failure Logs

Failure logs must say which evidence is missing before any Story can speak.
The coordinate names should remain chess-facing:

```json
{
  "stage": "Board Facts",
  "fact": "open_file_entry_square",
  "root": "current-position-root",
  "side": "White",
  "piece": "rook",
  "square": null,
  "file": "c",
  "rank": null,
  "line": null,
  "legalMove": null,
  "missing": ["entry square", "legal move", "same-board producer proof"]
}
```

The log is diagnostic only. It does not soften the public-output block.

## Exit Standard

Stage 1 is complete only when small board facts are recorded accurately, names
read like chess, absent data stays `0`/silent, and no path exists from these
facts to public commentary. Any fact that lacks side, piece, square, line,
legal move, or same-board producer proof remains silent or diagnostic-only.
