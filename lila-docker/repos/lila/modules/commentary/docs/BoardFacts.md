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

Each row must keep its side, piece, square, file, rank, line, or legal move
binding. If the binding cannot be recorded, the fact is silent.

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
