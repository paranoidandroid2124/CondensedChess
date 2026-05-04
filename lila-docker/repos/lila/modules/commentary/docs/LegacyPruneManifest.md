# Legacy Prune Manifest

Prepared on 2026-05-04 for the `BoardMood -> Story -> StoryTable -> Verdict`
reset.

Authority: `docs/ChessCommentarySSOT.md`, `docs/ChessModelArchitecture.md`,
and `docs/ChessModelContract.md`.

This branch has already pruned the legacy commentary authority tree. The live
commentary surface under `modules/commentary` is now limited to:

- `src/main/scala/lila/commentary/chess/**`
- `src/main/scala/lila/commentary/root/**`
- `src/test/scala/lila/commentary/chess/**`
- `src/test/scala/lila/commentary/docs/**`
- `docs/*.md`

Deleted legacy areas include the old public facade, API transports,
certification, claim, delta, candidate-line, projection, render, selection,
source-context adapter, strategic-object, witness, validation, diagnostic test,
and legacy expectation-corpus authority paths.

The remaining live docs are:

- `ChessCommentarySSOT.md`
- `ChessModelArchitecture.md`
- `ChessModelContract.md`
- `LegacyPruneManifest.md`
- `README.md`

Legacy names such as `CommentaryCoreSSOT.md`, `SemanticModelArchitecture.md`,
and `LegacyArchiveIndex.md` must not return as root docs authority.
