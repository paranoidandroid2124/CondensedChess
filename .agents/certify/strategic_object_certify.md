# Strategic Object Certification Guide

This checklist is for exact-board and admission certification of one active
packet.

Read first:

1. [AGENTS.md](/C:/Codes/CondensedChess/AGENTS.md)
2. [StrategicObjectModel.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/StrategicObjectModel.md)
3. [StrategicObjectRoadmap.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/StrategicObjectRoadmap.md)
4. [CommentaryTrustHardening.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md) when trust behavior changes
5. active packet doc under `.agents/packets/`

## Certification Goals

Judge semantic correctness, not style.

The certifier must decide whether the packet's exact-board contract holds on:

- object admission
- delta witness
- counterpart admissibility
- certification burden
- planner ownership or question admission
- promotion / defer verdict, if the packet touches readiness

## Core Rules

- exact-board evidence only
- nasty cases matter more than pretty positives
- if exact-position support is missing, prefer `defer`, `support-only`, or
  `blocked`
- separate verified board truth from conjectural extension

## Failure Classes

- false positive on a listed nasty row
- weak witness accepted as if it were exact
- broad-overlap counterpart admitted without packet-required burden
- planner/question admission opened from uncertified or support-only state
- promotion granted without enough exact-board resistance
- packet claims success while one of its own exit criteria is still false

## Output Format

Return only:

- `PASS` or `FAIL`
- row-level findings
- promotion/defer verdict if relevant
- one short sentence on whether retry is plausible or the packet is blocked
