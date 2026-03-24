# Prompt Surface Policy

This document is the current-state source of truth for prompt-bearing surfaces
in `modules/llm`.

It replaces older "prompt evolution" notes with the latest ownership model.

## Purpose

Prompt surfaces exist to polish or productize already computed commentary
surfaces. They are not a second canonical strategy engine.

## Families

### `PolishPrompt`

- Scope: Chronicle / Bookmaker body prose polish
- Ownership:
  - deterministic draft owns strategic content
  - prompt may realize the draft into cleaner prose
- Rules:
  - draft-grounded
  - slot-grounded when Bookmaker slots are present
  - no new topic introduction
  - no UI chrome recreation

### `ActiveStrategicPrompt`

- Scope: active-note wording polish
- Ownership:
  - deterministic active-note draft owns note existence
  - deterministic active-note draft owns canonical idea selection
  - prompt may polish wording only
- Rules:
  - preserve campaign owner, dominant idea, compensation family, theater, mode,
    concrete anchor, and forward-looking continuation
  - do not create a note where the deterministic path omitted one
  - if polish is empty / invalid / rejected, fall back to the deterministic
    draft

### `StrategicPuzzlePrompt`

- Scope: puzzle reveal / summary text
- Ownership:
  - puzzle pipeline owns the strategic draft
  - prompt may polish reveal wording for the puzzle product
- Rules:
  - draft-grounded
  - no invented moves, evaluations, engine language, or opening lore
  - concise reveal / summary only

## Canonical vs Optional

### Canonical generation

Canonical strategy and attach/omit logic live in deterministic code:

- Chronicle / Game Arc deterministic pipeline
- Bookmaker deterministic draft / slots / ledger
- Active deterministic note selection and fallback

### Optional polish

Optional prompt passes may improve wording after the canonical draft exists.

They must not:

- replace the strategic thesis
- change campaign owner or compensation contract
- create new commentary sections
- decide note existence on the signoff path

## Runtime Split

Current runtime defaults remain:

- `LLM_PROVIDER = openai`
- `LLM_PROVIDER_ACTIVE_NOTE = gemini`

Interpretation:

- provider split is a runtime polish-routing concern
- provider split is not a canonical strategy-selection concern
- signoff / eval path remains deterministic canonical first

## Commentary-Specific Rule

For commentary release-signoff:

- `ActiveStrategicPrompt` is an optional polish layer over a deterministic
  active-note draft
- older fresh-generation / independence framing is not the canonical release
  contract anymore
- the current release contract is:
  - deterministic first
  - optional polish second
  - deterministic fallback always survives

## Maintenance Rule

Update this file in the same change if any prompt-bearing surface changes:

- family role
- canonical vs optional ownership
- active-note provider semantics
- runtime provider split interpretation
- puzzle/commentary prompt boundary

Do not add chronological update logs here. Keep this document as the latest
ownership policy only.
