# Historical U-Primary Discriminator Inventory

This document records the historical exact-discriminator redesign ideas that
were explored for the former `7` blocked `U-primary` rows.

It remains useful as a board-anchored archive of narrowing attempts, even
though the current branch now discharges those `U` blockers by rehome or
host-shell conversion rather than by forcing all seven rows to survive as live
`U-primary` exact witnesses.

Nothing here is an admitted live runtime contract yet.

## Scope

The historical blocked `U-primary` rows were:

- `opening-tempo`
- `middlegame-positional`
- `transition-liquidation`
- `endgame-race`
- `closed center`
- `fixed chain`
- `central tension`

The redesign attempts mattered because one of two defects kept appearing:

- no exact discriminator has yet been narrowed enough to mint a runtime id
- a reviewed narrowing proposal already failed and needs a cleaner redesign

## Design Rules

- Every candidate below must remain exact-board and deterministic.
- No candidate may admit from move count, prose, planner state, or engine score.
- Phase rows must not over-admit upper families such as initiative, plan race,
  transition bridge, conversion funnel, or promotion race.
- Structural rows must not collapse into broad strategic value language such as
  clamp, king safety, or initiative.
- Runtime ids below are redesign candidates only.

## Phase Cluster Candidates

| Blocked row | Candidate runtime id | Anchor | Candidate exact bundle | What it tries to preserve | Main risk to watch |
| --- | --- | --- | --- | --- | --- |
| `opening-tempo` | `opening_development_regime` | `board` | undeveloped minor-piece reserve on at least one side, central setup still unresolved, and no admitted transition/endgame regime | exact opening-phase witness without using move count or initiative wording | too close to generic opening narration or castling-choice storytelling |
| `middlegame-positional` | `dual_sector_contested_contact_state` | `board` | both sides already show post-opening non-pawn deployment away from the home rank, and `contested_sectors = {s1, s2}` only, with each sector containing at least one `contested(square)` root fact and the sector geometry carried in payload rather than admitted as an open-ended regime | exact middlegame witness with a narrower dual-sector board signal rather than evaluative activity or open-ended posture narration | still a coarse two-axis proxy unless nasty negatives cut out opening transpositions and quiet one-sector tension |
| `transition-liquidation` | `compressed_reciprocal_exchange_front_state` | `board` | at least one live reciprocal exchange front exists on the current board, multiple currently attackable non-pawn pieces already sit on or directly support the same contact corridor, and the surviving material picture is compressed enough that the corridor is phase-defining rather than ordinary middlegame contact | exact transition witness without drifting into `MoveLocal`, `TransitionBridge`, or generic liquidation wording | `compressed enough` is still not a hard board-local predicate unless the compression gate is made exact |
| `endgame-race` | `dual_run_endgame_trigger` | `board` | low-material regime plus dual race-relevant run resources or king-run corridors for both sides, without yet certifying conversion or promotion outcome | exact race-trigger witness without collapsing into `promotion race`, `PasserComplex`, or `ConversionFunnel` | over-admitting any low-material passer position |

## Structural Cluster Candidates

| Blocked row | Candidate runtime id | Anchor | Candidate exact bundle | What it tries to preserve | Main risk to watch |
| --- | --- | --- | --- | --- | --- |
| `closed center` | `central_lock_front_state` | `board` | at least two central files participate in a mutually blocked pawn front, with the relevant central-file geometry carried in payload rather than in the anchor itself | broad ordinary closed-center truth without demanding one special barrier motif | still too motif-shaped or too tied to one lock pattern |
| `fixed chain` | `supported_fixed_chain_segment_state` | `square` | the anchor square belongs to a same-color pawn segment of length `>= 2` with backward diagonal support and fixed forward contact at its frontier, with the rest of the segment carried in payload and branched clusters excluded | true chain segment truth without reconstructing a whole pawn-cluster topology | too local for the broad row or still too loose on non-chain clusters |
| `central tension` | `central_contact_front_state` | `sector` | the central sector contains a live contact front with at least two distinct `contested(s)` squares, both colors contribute current control or occupation to that same front, and payload carries the front squares, occupiers, and local control relations while allowing mixed pawn/piece contact rather than one pawn-pair motif | broad central-contact truth pinned to exact front geometry rather than resource/evaluation language | still depends on a canonical central-sector mask and front-connectivity rule to stay exact |

## Why The Earlier Narrowings Failed

| Blocked row | Rejected proposal | Why it failed |
| --- | --- | --- |
| `closed center` | `closed_center_barrier_state` | too motif-specific; under-fired on ordinary closed-center positions |
| `fixed chain` | `fixed_chain_state` | too loose; admitted branched fixed-pawn clusters and required richer topology than current roots support |
| `central tension` | `central_pawn_contact_state` | too pawn-pair-specific; under-fired on broader central contact states |

The phase rows failed differently:

- they remained admitted as legacy `U-primary` witnesses
- but no exact lower discriminator has been narrowed enough to justify a new
  runtime id

## Historical Audit Verdict

Before the rehome discharge, the redesign-candidate status was:

Accepted as redesign candidates:

- `opening-tempo` -> `opening_development_regime`
- `endgame-race` -> `dual_run_endgame_trigger`
- `closed center` -> `central_lock_front_state`
- `fixed chain` -> `supported_fixed_chain_segment_state`
- `central tension` -> `central_contact_front_state`

Still too weak to freeze:

- `middlegame-positional` -> `dual_sector_contested_contact_state`
  - now narrower, but still a coarse two-axis proxy that needs nasty negatives
    against opening transpositions and quiet one-sector tension
- `transition-liquidation` -> `compressed_reciprocal_exchange_front_state`
  - now narrower, but `compressed enough` is still not yet a hard board-local
    predicate

These two remained open redesign ideas rather than accepted candidate slices.
`central_contact_front_state` joins the accepted redesign-candidate set, but it
remains non-live until the central-sector mask and front-connectivity predicate
are frozen canonically.

## Adversarial Re-Audit Outcome

After a second adversarial pass, the blocker picture is sharper:

- `middlegame-positional` does not merely need a narrower name
  - under the current branch roots/helpers, it still lacks a clean `U-primary`
    exact discriminator
  - `contested(s)` exists only at square granularity, while
    `contested_sectors` remains payload geometry rather than a certified lower
    witness
  - unless the branch freezes a canonical sector mask plus a true
    contact-front connectivity predicate, this row stays a phase proxy or must
    split into a narrower atom

- `transition-liquidation` does not merely need a nastier negative corpus
  - the current candidate still depends on `compressed enough`, which remains a
    comparative threshold rather than a hard board-local predicate
  - current roots/helpers do not expose a clean transition-compression helper
    that could replace that threshold
  - this row stays blocked until a new exact lower contract is minted

- `central tension` is the only one of the three that survives as a stable
  candidate-layer freeze
  - `central_contact_front_state` is chess-logically narrower than the legacy
    placeholder
  - but it still remains non-live because the current branch has not yet frozen
    a canonical `central-sector mask` or `front-connectivity` helper

## Required Future Helper Work

The current re-audit implies the following minimum future work before any of
the remaining blocked rows can become live:

| Row | Minimum future helper work | Why |
| --- | --- | --- |
| `middlegame-positional` | canonical sector mask plus a certified contact-front connectivity predicate, or an explicit split into a narrower atom | current branch only exposes square-level `contested(s)` and payload-only `contested_sectors` |
| `transition-liquidation` | a new exact lower transition-compression contract | no current root/helper replaces the vague `compressed enough` gate |
| `central tension` | canonical `central-sector mask` and `front-connectivity` helper | the candidate is distinct enough, but still infrastructure-blocked |

## Candidate Admission Conditions

Each row may become live only if all of the following are satisfied:

1. one candidate exact slice is chosen over the alternatives
2. the chosen slice is expressible from the current root-state layer plus
   admitted lower helpers
3. near-miss and nasty-negative corpora exist
4. the slice has a negative boundary that prevents drift into neighboring upper
   families
5. the runtime id is narrower than the inventory label when necessary

## Current Status

- the current branch no longer treats these rows as active blocked `U-primary`
  witnesses
- phase rows were discharged by rehome above `U`
- `closed center` and `fixed chain` were discharged by conversion to
  `U-attached` host-shell vocabulary
- `central tension` was discharged from `U` by rehome to object-side
  continuity ownership
- none of the candidate runtime ids in this document are admitted live on the
  current branch

## Intended Use

- use this file as historical context when re-evaluating whether any discharged
  row should later regain a live exact slice
- do not treat these candidate ids as active contracts
- when one candidate is accepted, update the row section in
  [Witnesses61.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/Witnesses61.md),
  [DescriptorOwnershipMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DescriptorOwnershipMatrix.md),
  and [DecisionFreezeLedger.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DecisionFreezeLedger.md)
  in the same change
