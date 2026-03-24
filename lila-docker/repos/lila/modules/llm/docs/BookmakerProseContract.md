# Bookmaker Prose Contract

This document is the current-source contract for Bookmaker body prose.

It describes the live ownership boundary between deterministic Bookmaker
structure and optional prose polish.

## Purpose

Bookmaker is a hybrid surface:

- backend / frontend own structure
- the commentary body owns prose only

The prose contract exists so optional polish cannot recreate the product
structure or drift away from deterministic strategic content.

## Ownership Model

### UI / backend owned

- Bookmaker card / panel structure
- Strategic Signals rows
- Bookmaker ledger rows when present
- Decision compare surfaces
- Piece deployment blocks
- Evidence probe blocks
- Authoring evidence blocks
- Alternative option controls, chips, and boards

### Prose owned

- the main Bookmaker body only
- paragraph boundaries
- sentence flow inside paragraphs
- connective tissue between already computed facts

## Deterministic-First Rule

Current Bookmaker contract is deterministic first:

- deterministic draft prose is built before any LLM step
- optional polish may improve wording only
- ledger rows are computed, not prose-mined
- signoff / provider-none evaluation remains valid because Bookmaker still has
  a deterministic body path

## Output Contract

The Bookmaker body must:

- return plain prose only
- avoid headings, bullet lists, metadata wrappers, and JSON prose wrappers
- avoid recreating UI section titles
- preserve paragraph structure when possible
- avoid wall-of-text collapse
- preserve existing branch labels if they already exist in the draft
- avoid turning ledger rows into repeated body scaffolding
- keep at most one exact concrete line in the body unless the deterministic
  draft already justifies more

## Prose Priorities

The body should prioritize:

- the dominant strategic claim
- the main cause -> effect chain behind that claim
- the most decision-relevant concrete line, tension point, or practical note

The body should not expand into:

- full ledger restatements
- UI-card duplication
- free-form topic invention beyond the deterministic draft / slots

## Signal Triage

Not every structured signal belongs in prose.

Preferred prose payload:

- dominant claim
- strongest support chain
- one concrete deployment / line / counter-resource when materially helpful

Preferred structured payload:

- motif / stage / carry-over / prerequisites / conversion rows
- decision compare detail
- secondary routes and sidecar evidence

## Prompt / Polish Rules

`PolishPrompt` must follow this contract:

- draft-grounded
- slot-grounded when slot payload exists
- no new topic introduction
- no UI chrome recreation
- no ledger field enumeration

## Enforcement Points

Primary enforcement lives in:

- `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerPolishSlots.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookmakerStrategicLedgerBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/CommentaryPayloadNormalizer.scala`
- `modules/llm/src/main/LlmApi.scala`

## Relationship To Pipeline SSOT

This file is narrower than `CommentaryPipelineSSOT.md`.

Use:

- `CommentaryPipelineSSOT.md` for the full commentary pipeline
- `PromptSurfacePolicy.md` for prompt-family ownership
- this file for the Bookmaker body prose contract only

## Maintenance Rule

Update this file in the same change if any of the following changes:

- Bookmaker body prompt contract
- slot ownership
- ledger duplication rules
- paragraph / structure expectations
- deterministic-vs-polish ownership for Bookmaker prose

Do not append dated change logs here. Keep this file as the current Bookmaker
body contract only.
