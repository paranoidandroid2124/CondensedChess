# Commentary Trust Boundary

This document defines what user-facing MoveReview prose and UI may trust. It is
kept short so cleanup and producer refactoring are not blocked by stale rollout
details.

## Core Rule

User-facing chess meaning may only come from typed evidence admitted by the
producer/planner/local-fact path. Presentation layers may format, arrange, and
hide content, but they do not create claim authority.

Untrusted as public chess authority:

- renderer prose;
- source prose;
- row labels;
- opening or endgame names;
- diagnostic strings;
- planner scene names;
- frontend CSS classes or tags;
- author-question labels without supporting evidence;
- cached commentary text.

Trusted only when typed and current-position-backed:

- board geometry and legal move state;
- checked SAN/UCIs and replay refs;
- PV/eval/probe/tablebase results;
- analyzer packets;
- admitted local facts and CausalFrame evidence.

## Risk Gates

Risk gates may suppress, demote, or defer a surface. They do not authorize
tactical, plan, timing, forced, alternative, opening, endgame, compensation, or
result claims by themselves.

When a risk gate closes a claim, the preferred follow-up is producer/evidence
repair. Sanitizer or renderer closure is a safety stop, not completion.

## Renderer And Frontend

Renderer and frontend code may:

- render admitted text;
- show scene structure;
- display board/eval/replay cues from typed refs;
- keep support and diagnostic details out of the main conclusion;
- fail closed when a payload is malformed.

Renderer and frontend code must not:

- parse row text or labels to infer chess meaning;
- promote `probeRows` or `authorRows` to main conclusions;
- turn opening/endgame labels into plan or technique truth;
- create "best", "forced", "attack", "conversion", "draw", or "win" claims;
- choose a new claim family from diagnostics or source strings;
- alter checked line order or repeated SAN meaning.

`MoveReviewPlayerSurface` field choice is part of trust:

- `summaryRows` read as main reasons.
- `advancedRows` read as plan/follow-up direction.
- `probeRows` and `authorRows` read as support or questions.

If a backend claim appears in the wrong field, fix the backend surface mapping
rather than compensating in frontend copy.

## Claim Strength

Use the weakest public strength that matches the evidence:

- public conclusion: current-position evidence fully supports the claim family;
- weak claim: evidence supports the idea but not decisive language;
- support-only fact: useful context but not a conclusion;
- diagnostic: developer trace only;
- silence: malformed, stale, or unsupported.

Do not treat fail-closed behavior as the end of the work when a true weaker
claim can be recovered from existing producer/analyzer/probe/model assets.

## Disallowed Legacy Flow

These flows stay outside the trust boundary:

- prose -> claim;
- diagnostic string -> claim;
- opening/endgame label -> current-position truth;
- row label -> authority;
- frontend tag/class -> chess meaning;
- risk gate -> positive explanation;
- source name -> evidence;
- cached sentence -> evidence.

If such a flow is found, classify the issue first: no evidence, wrong family,
incomplete evidence, renderer/frontend overreach, missing producer skill,
move-order dependency, objective/practical mismatch, overgeneralized theme, or
wrong surface tier. Then repair or demote the claim at the typed evidence path.
