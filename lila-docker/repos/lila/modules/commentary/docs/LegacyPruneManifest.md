# Legacy Prune Manifest

Prepared on 2026-05-04 for the `BoardMood -> Story -> StoryTable -> Verdict`
reset.

Authority is exactly the live root docs listed in `docs/README.md`:
`docs/ChessCommentarySSOT.md`, `docs/ChessModelArchitecture.md`,
`docs/ChessModelContract.md`, `docs/ChessResetRationale.md`,
`docs/BoardFacts.md`,
`docs/BoardMoodCutLaw.md`, `docs/BoardMoodSplitLaw.md`,
`docs/StoryInteractionLaw.md`, `docs/StoryResurrectionLaw.md`,
`docs/LegacyPruneManifest.md`, and `docs/README.md`.

This branch has already pruned the legacy commentary authority tree. The live
commentary surface under `modules/commentary` is now limited to:

- `src/main/scala/lila/commentary/chess/**`
- `src/main/scala/lila/commentary/root/**`
- `src/test/scala/lila/commentary/chess/**`
- `src/test/scala/lila/commentary/docs/**`
- `docs/*.md`

Pruned legacy authority areas include certification, claim, delta,
candidate-line, projection, render, selection, source-context adapter,
strategic-object, witness, validation, diagnostic test, and legacy
expectation-corpus authority paths.

The old public facade and API transport authority is closed, not replaced.
`/api/commentary/render` and `/internal/commentary/render-local-probe` remain
registered only as fail-closed tombstones. No `200`, rendered payload,
environment switch, frontend mock, or route test can open public commentary.

The remaining live docs are:

- `ChessCommentarySSOT.md`
- `BoardFacts.md`
- `BoardMoodCutLaw.md`
- `BoardMoodSplitLaw.md`
- `ChessModelArchitecture.md`
- `ChessModelContract.md`
- `ChessResetRationale.md`
- `LegacyPruneManifest.md`
- `README.md`
- `StoryInteractionLaw.md`
- `StoryResurrectionLaw.md`

Legacy names such as `CommentaryCoreSSOT.md`, `SemanticModelArchitecture.md`,
`LegacyArchiveIndex.md`, and `CommentaryFrontendBridgeContract.md` must not
return as root docs authority.

## Closed Reset State

The prune does not open a replacement public path. Controllers, facades, API
transports, renderer-facing claim paths, default runtime FEN to public
`Verdict`, broad `BoardMood` Sxxx expansion/re-entry, and `Story` proof writers
stay closed until prerequisite laws and tests exist. Stage 1 `Board Facts`
admits only small current-board observations below public claims.

`BoardMood` does not expand beyond `48` bits, `256` scalars, and `3,328` total
values in this checkpoint. Split/cut re-entry requires a named law and
same-board producer proof.

Existing failures around lower facts, scaffold paths, and renderer non-upgrade
remain evidence for the reset boundary only. They are not acceptance for public
runtime commentary.

Stage 3 charter authority lives in `StoryInteractionLaw.md`. Legacy material
does not open `Tactic.Hanging`, any other positive Story family, renderer, LLM,
or public route.
