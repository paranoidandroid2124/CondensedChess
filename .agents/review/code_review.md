# Strategic Object Packet Review Guide

This checklist is for read-only review of one active packet.

Read first:

1. [AGENTS.md](/C:/Codes/CondensedChess/AGENTS.md)
2. [StrategicObjectModel.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/StrategicObjectModel.md)
3. [StrategicObjectRoadmap.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/StrategicObjectRoadmap.md)
4. active packet doc under `.agents/packets/`

## Review Goals

Check only the active packet boundary.

Do not ask whether a larger redesign is possible.
Ask whether the diff stays inside the packet and preserves rewrite doctrine.

## Fail Conditions

- packet scope widened without queue or packet update
- touched files escape the packet without justification
- raw board or primitive ingress reopens below the canonical boundary
- legacy topology, carrier, or helper forest is restored
- planner or renderer starts inventing semantics that belong upstream
- wording/prose work sneaks into a non-wording packet
- doc sync is missing after a behavior-relevant runtime contract change
- tests are missing for new runtime meaning
- build/test commands required by the packet were not run

## Rewrite Boundary Checks

- primitive boundary still centralized
- object/delta/cert/planner responsibilities still separated
- exact-slice and admission rules stay centralized, not copy-pasted
- no temporary rollout names in runtime module/file names
- runtime and test/tooling roles remain distinct

## Output Format

Return only:

- `PASS` or `FAIL`
- a short checklist
- concrete defect list with file and reason

Do not rewrite the packet.
Do not propose unrelated future work.
