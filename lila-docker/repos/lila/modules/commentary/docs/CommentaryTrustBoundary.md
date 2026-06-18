# Commentary Trust Boundary

This is a trust-risk reference for user-facing MoveReview prose and UI. It is
not a complete rulebook. Live code, typed payloads, replay evidence, and corpus
behavior are stronger working evidence.

## Core Rule

User-facing chess meaning should come from typed evidence admitted by the
producer, planner, local-fact, or CausalFrame path. Presentation layers may
format, arrange, and hide content; they should not create claim families or
claim strength.

Untrusted as public chess evidence:

- renderer or cached prose;
- source prose;
- row labels;
- opening or endgame names by themselves;
- diagnostic strings;
- planner scene names;
- frontend CSS classes or tags;
- author-question labels without resolved evidence.

Trusted only when current-position-backed:

- board geometry and legal move state;
- checked SAN/UCIs and replay refs;
- PV/eval/probe/tablebase data;
- analyzer packets;
- admitted local facts and CausalFrame evidence.

## Risk Gates

Risk gates may suppress, demote, or defer a surface. They do not authorize
tactical, plan, timing, forced, alternative, opening, endgame, compensation, or
result claims by themselves.

When a risk gate closes a claim, repair the producer or evidence path if a true
weaker explanation should exist. Fail-closed behavior is a safety stop, not a
quality win.

## Surface Strength

Use the weakest public strength supported by the payload:

- public conclusion: current-position evidence supports the family and strength;
- weak claim: evidence supports the idea but not decisive wording;
- support-only fact: useful context but not a conclusion;
- diagnostic: developer trace only;
- silence: malformed, stale, or unsupported.

Field placement matters:

- `summaryRows` read as main reasons.
- `advancedRows` read as plans or follow-up direction.
- `probeRows` and `authorRows` read as support, checks, or unresolved questions.

If a backend claim appears in the wrong field, fix the backend surface mapping
rather than compensating in frontend copy.

## Common Trust Risks

Treat these as diagnostic prompts, not as a complete checklist:

- no evidence;
- wrong claim family;
- incomplete evidence;
- renderer or frontend overreach;
- missing producer skill;
- move-order dependency;
- objective/practical mismatch;
- overgeneralized theme;
- wrong surface tier.

Recurring examples include forcedness without alternative proof, tactical labels
without current-move geometry, line-consequence claims without replay binding,
plan claims from broad context only, and opening/endgame labels promoted to
current-position technique.

## Renderer And Frontend

Renderer and frontend code may render admitted text, show typed replay/board
cues, keep diagnostics out of conclusions, and fail closed on malformed payloads.

They should not:

- parse row text or labels to infer chess meaning;
- promote `probeRows` or `authorRows` to main conclusions;
- turn opening/endgame labels into plan or technique truth;
- create best, forced, attack, conversion, draw, or win claims;
- choose a new claim family from diagnostics or source strings;
- alter checked-line order or repeated SAN meaning.

## Legacy Flows To Avoid

These flows stay outside the trust boundary:

- prose -> claim;
- diagnostic string -> claim;
- opening/endgame label -> current-position truth;
- row label -> evidence;
- frontend tag/class -> chess meaning;
- risk gate -> positive explanation;
- source name -> evidence;
- cached sentence -> evidence.

Fix these at the typed evidence path, or lower the surface strength.
