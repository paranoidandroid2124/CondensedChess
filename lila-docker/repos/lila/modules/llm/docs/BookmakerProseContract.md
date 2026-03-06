# Bookmaker Prose Contract

Snapshot: `2026-03-06`

## Purpose

This contract defines the structure level for LLM-polished Bookmaker commentary.
It fixes the boundary between:

- UI-owned structure
- LLM-owned prose

The target level is `L2.5 hybrid`.

Meaning:

- the product owns sections, cards, and summary blocks
- the LLM owns only the commentary body prose
- the prose must be paragraph-structured, but it must not invent document chrome

## Ownership Model

UI-owned structure:

- `Strategic Signals`
- `Evidence Probes`
- `Authoring Evidence`
- `Alternative Options`
- eval toggle, preview board, move chips, and variation controls

LLM-owned structure:

- the main commentary body only
- paragraph boundaries
- sentence flow inside paragraphs
- connective tissue between facts already present in the draft

## Output Contract

The polished Bookmaker body must satisfy all of the following:

- Return plain prose only.
- Do not emit markdown headers, bullet lists, metadata wrappers, or JSON prose wrappers.
- Do not recreate UI section titles such as `Strategic Signals`, `Evidence Probes`, `Authoring Evidence`, or `Alternative Options`.
- Preserve paragraph boundaries from the draft when possible.
- Do not collapse a multi-paragraph draft into one wall of text.
- If branch labels such as `a)`, `b)`, or `c)` already exist in the draft, preserve them exactly.
- Do not invent new list structures.

## Paragraph Budget

For isolated-move / Bookmaker commentary:

- Target `2-4` short paragraphs.
- Keep each paragraph to `1-3` sentences.
- Prefer this order when supported by the draft:
- Paragraph 1: immediate move meaning and evaluation direction
- Paragraph 2: strategic consequence and opponent resource or plan
- Paragraph 3: optional concrete line, practical verdict, or evidence-backed nuance
- Paragraph 4: only if the draft already needs an extra supported paragraph

For full-game intro / conclusion / moment polishing using the same polish path:

- keep the same “plain prose only” rule
- preserve the draft paragraph rhythm instead of inventing new section structure

## Signal Triage

Not every structured signal belongs in prose.

The body should prioritize:

- the main move explanation
- the most important strategic consequence
- the most decision-relevant concrete line or practical note

Secondary evidence should stay in UI-owned blocks when available.

## Compatibility Rules

The contract does not weaken existing preservation rules:

- SAN token order must remain valid
- move numbering and marker style must remain valid
- eval tokens and variation labels must remain valid
- anchor tokens such as `[[MV_*]]`, `[[MK_*]]`, `[[EV_*]]`, and `[[VB_*]]` must remain exact

## Enforcement Points

- Prompt contract:
  - `modules/llm/src/main/scala/lila/llm/PolishPrompt.scala`
  - implementation note:
    - the full contract is documented here, but per-request polish / repair prompts
      should carry only a short structure reminder to reduce token cost
- Deterministic paragraph rendering:
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
- Post-processing that preserves paragraph breaks:
  - `modules/llm/src/main/scala/lila/llm/analysis/PostCritic.scala`
- Frontend UI-owned blocks:
  - `ui/analyse/src/bookmaker.ts`
  - `ui/analyse/css/_side.scss`

## Non-Goals

This contract does not ask the LLM to:

- build cards
- choose UI section ordering
- expose every helper signal in prose
- replace deterministic frontend structure
