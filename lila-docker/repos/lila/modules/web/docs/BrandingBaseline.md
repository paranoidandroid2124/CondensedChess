# Branding Baseline (Phase 0)

Date: 2026-02-18

## Scope

- Goal axis: brand separation first.
- Pass scope: core surfaces only (`landing`, `analysis`, `header/nav`, top-level shell).

## Baseline snapshots

- Previous branding pass touched files: `34` (`git show --stat -1 e809325a`).
- jQuery direct DOM call occurrences (`$('`) in first-party analysis/site/lib: `123`.
- Key file line counts:
  - `ui/analyse/src/bookmaker.ts`: `532` lines
  - `ui/site/src/topBar.ts`: `118` lines
  - `modules/web/src/main/ui/layout.scala`: `199` lines

## Branding fingerprint baseline

Command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/brand_fingerprint_check.ps1
```

Current baseline result: `PASS` (all forbidden runtime patterns at zero matches in target paths).

## Notes

- This document is the reference point for Phase 1+ checks.
- Legal/open-source pages are excluded from branding-removal targets.
