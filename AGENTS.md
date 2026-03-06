# Agent Instructions

For tasks touching the Chesstory commentary-analysis pipeline, use
`lila-docker/repos/lila/modules/llm/docs/CommentaryPipelineSSOT.md`
as the canonical audit.

Scope includes helper modules and consumption paths across:
- `strategic`
- `semantic`
- `endgame`
- `opening`
- `probe`
- `plan`
- `pawn`
- `structure`
- `threat`
- `practicality`
- `counterfactual`
- `authoring`
- outline / renderer / API / frontend

Do not redo the full `producer -> carrier/model -> builder -> outline -> renderer -> API -> frontend`
trace if the request is already answered by that SSoT.

Re-audit only if:
- the user explicitly asks for a fresh audit,
- code changed after the SSoT snapshot in
  `lila-docker/repos/lila/modules/llm/src/main`,
  `lila-docker/repos/lila/app/controllers/LlmController.scala`, or
  `lila-docker/repos/lila/ui/analyse/src`,
- or the task introduces a runtime path not covered by the SSoT.

When changing the audited pipeline, update the SSoT in the same change.
