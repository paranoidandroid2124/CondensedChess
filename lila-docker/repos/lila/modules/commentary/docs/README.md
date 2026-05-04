# Commentary Docs

This branch is a chess commentary model reset.

Only the documents in this directory root are live authority for current
commentary backend work. There is no live archive authority in this branch.

Live authority:

- `ChessCommentarySSOT.md`
- `ChessModelArchitecture.md`
- `ChessModelContract.md`
- `ChessResetRationale.md`
- `LegacyPruneManifest.md`
- `README.md`

Authority summary:

- `ChessCommentarySSOT.md` defines the single public chess meaning chain:
  `BoardMood` -> `Story` -> `StoryTable` -> `Verdict`.
- `ChessModelArchitecture.md` describes the HCE-style deterministic chess
  model architecture.
- `ChessModelContract.md` is the shape, naming, family, and deletion contract.
- `ChessResetRationale.md` records why the reset exists and which old failure
  modes the new model must not repeat.
- `LegacyPruneManifest.md` records what was pruned and what is intentionally
  preserved during the reset.
