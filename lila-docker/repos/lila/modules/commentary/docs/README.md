# Commentary Docs

This branch is a chess commentary model reset.

Only the documents in this directory root are live authority for current
commentary backend work. There is no live archive authority in this branch.

Live authority is exactly and exhaustively:

- `ChessCommentarySSOT.md`
- `BoardFacts.md`
- `BoardMoodCutLaw.md`
- `BoardMoodSplitLaw.md`
- `ChessModelArchitecture.md`
- `ChessModelContract.md`
- `ChessResetRationale.md`
- `LegacyPruneManifest.md`
- `README.md`
- `StoryInteractionLaw.md`
- `StoryResurrectionLaw.md`

`AGENTS.md`, docs tests, and manifest text must agree with this list. Any
mismatch is a no-go state, not a second source of authority.

## Current No-Go State

This reset is intentionally closed at the public boundary.

Line / Ray Slice is a closed baseline.
Line-0 opens only the charter for the first narrow line/ray proof slice.
Line-1 opens only `LineProof` as a narrow proof home.
Line-2 opens only the named `TacticDiscoveredAttack` writer for one narrow `Tactic.DiscoveredAttack` Story.
Line-3 opens only the negative corpus for the narrow `Tactic.DiscoveredAttack` slice.
Line-4 opens only existing `EngineCheck` reuse for existing `Tactic.DiscoveredAttack` Stories.
Line-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, and `Tactic.DiscoveredAttack` rows.
Line-6 opens only ExplanationPlan mapping for selected Lead `Tactic.DiscoveredAttack` Verdicts.
Line-7 opens only deterministic renderer text for selected `Tactic.DiscoveredAttack` ExplanationPlan.
Line-8 opens only LLM smoke for selected DiscoveredAttack ExplanationPlan and RenderedLine.
Line Closeout opens no new chess meaning; it audits only LineFact, LineProof, Tactic.DiscoveredAttack, speech key, renderer/LLM, docs authority, and closed public surfaces.
First Line scope: a legal move reveals one slider attack on one non-king material target.
LineFact observes geometry. LineProof binds the revealed line. Tactic.DiscoveredAttack may speak only after proof.
Geometry is not enough. Revealed attack proof or silence.
LineProof is not a public Story. LineFact is not a public Story.
Current implementation scope is Line / Defender Contact Neighborhood.
Pin-0 opens only the charter for the second narrow line/defender contact vertical slice.
Pin first positive scope is not a broad pin family.
Pin first scope: a legal move creates or reveals a line where one non-king piece is pinned to its king.
LineFact observes geometry. LineProof binds the line. PinProof proves the pinned relation. Tactic.Pin may speak only after proof.
Pin-0 opens only narrow Tactic.Pin, king-behind line relation, one non-king pinned target, a legal move that creates or reveals the pin relation, and bounded pin wording after selected Verdict only.
Pin-0 opens no broad LineTactic, broad AbsPin or RelPin family, Skewer, XRay public Story, RemoveGuard, DiscoveredAttack expansion, mate threat, king safety, winning material, decisive tactic, forced move, best move, cannot move wording, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.
Pin-1 opens only `PinProof` as a narrow proof home.
PinProof proves side creating the pin, pinned target, pinning slider, king behind target, legal pinning or revealing move, line kind, same-board proof, before/after relation, target non-king, target and king same side, and slider attacks through target toward king after move.
PinProof is not a public Story. LineFact and LineProof are not public Stories.
PinProof says no material gain, king unsafe, mate, pressure, or initiative; proofFailures stay out of renderer/LLM input.
Pin-2 opens only the named `TacticPin` writer for one narrow `Tactic.Pin` Story.
TacticPin requires complete StoryProof, complete PinProof, same-board legal replay, legal pinning or revealing move, pinned target, pinning slider, king-behind-target relation, writer identity, and no EngineCheck Refutes status.
Pin Story identity is tactic Pin, scene Tactic, pinning side, pinned-side rival, pinned target square, pinning slider or moved-piece anchor, and pinning/revealing route.
Pin-2 opens no Material claim, Defense ownership, RemoveGuard ownership, king target speech, broad AbsPin or RelPin family, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
Pin-3 opens only the negative corpus for the narrow `Tactic.Pin` slice.
Pin-3 keeps illegal moves, missing same-board proof, non-sliders, missing king-behind-target relation, wrong-side king relation, broken target-to-king line, king targets, extra blockers, stale pin-looking geometry, discovered-only lines, skewer-looking lines, and mate, king-safety, or pressure wording silent.
A line to a king is not enough. Complete pinned relation or silence.
Pin-4 opens only existing `EngineCheck` reuse for existing `Tactic.Pin` Stories.
EngineCheck cannot create Pin; Supports creates no new claim; Caps suppresses allowed claim or keeps downstream speech bounded when opened later; Refutes blocks the Pin Story; Unknown creates no engine expression.
Pin-4 opens no engine-says wording, best-move wording, only-move wording, winning tactic, forced win, raw PV explanation, eval number public value, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
Pin-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, and `Tactic.Pin` rows.
Pin-5 keeps selected Verdict stable across input order, prevents duplicate Lead for same-line DiscoveredAttack and Pin, keeps Pin from owning Material or king-safety claims, keeps actual material change in Scene.Material, and gives Defense no claim without complete ThreatProof and DefenseProof.
Pin-5 opens no new Pin proof home, new Story family, broad AbsPin or RelPin family, Material claim from Pin, king-safety claim from Pin, Defense claim from incomplete Defense rows, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
Pin-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Pin` Verdicts.
Pin-6 allows only the `pins_piece` claim key and forbids wins_material, king_unsafe, mate_threat, best_move, only_move, forced, decisive, creates_pressure, takes_initiative, and cannot_move.
Support, Context, Blocked, capped, and refuted Pin rows create no standalone claim.
Pin-6 opens no renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
Pin-7 opens only deterministic renderer text for selected `Tactic.Pin` ExplanationPlan.
Pin-7 renderer input is ExplanationPlan only and may render `{route} pins the piece on {target}.`
Pin-7 forbids cannot move, the king is unsafe, wins material, winning, decisive, best move, only move, forces, creates pressure, and threatens mate.
Pin-7 opens no raw Verdict, raw Story, PinProof, LineFact, LineProof, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
Pin-8 opens only LLM smoke for selected Pin ExplanationPlan and RenderedLine.
Pin-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`
Pin-8 forbids raw Story, raw PinProof, raw LineProof, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, mate, pressure, initiative, winning, and cannot-move claims.
Pin-8 opens no public/user-facing LLM narration, public route `200`, production API, raw proof repair, or engine explanation.
Pin Closeout opens no new chess meaning; it audits only LineFact, LineProof, PinProof, Tactic.Pin, speech key, renderer/LLM, docs authority, test-helper boundary, and closed public surfaces.
Pin Closeout confirms Pin owns no Material, Defense, DiscoveredAttack, Skewer, or RemoveGuard meaning; broad Line, Ray, XRay, and broad Pin family terms are not live authority; renderer/LLM wording stays no stronger than `pins_piece`; public route `200`, production API, and public/user-facing LLM narration remain closed.
RemoveGuard-0 opens only the charter for the third narrow line/defender contact vertical slice.
First RemoveGuard positive scope is not a broad remove-the-guard motif.
RemoveGuard first scope: a legal move removes one defender from one non-king material target.
First runtime positive path stays centered on defender capture when possible; deflection is allowed only when exact-board proof immediately after the same move shows the defender no longer guards the target.
BoardFacts observes guard relation. RemoveGuardProof proves the guard was removed. Tactic.RemoveGuard may speak only after proof.
RemoveGuard-0 opens only narrow Tactic.RemoveGuard, one non-king material target, one defender, one legal move that removes the defender guard relation, and bounded remove-guard wording after selected Verdict only.
RemoveGuard-0 opens no broad deflection tactic, overloaded defender theory, discovered attack expansion, Pin/Skewer/XRay expansion, material win claim, winning, decisive, forced, best move, only move, no defense, refutes defense, collapses position, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.
RemoveGuard-1 opens only `RemoveGuardProof` as a narrow proof home.
RemoveGuardProof proves side removing the guard, rival side, guarded target, removed defender, legal remove-guard move, target non-king material piece, defender guarded target before move, after move defender no longer guards target, same-board proof, and exact-board after-move relation.
RemoveGuard-1 permits DefenderCaptured first and GuardLineBlocked only when one legal move blocks a slider defender guard line and exact-board proof shows the defender no longer guards the target.
RemoveGuard-1 keeps opponent-reply deflection, sacrifice lure, overloaded defender, remove guard by long tactic sequence, and defender-cannot-defend general theory closed.
RemoveGuardProof is not a public Story and owns no material result; proofFailures stay out of renderer/LLM input.
RemoveGuard-1 opens no Tactic.RemoveGuard writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, material win claim, public route `200`, production API, or public/user-facing LLM narration.
RemoveGuard-2 opens only the named `TacticRemoveGuard` writer for one narrow `Tactic.RemoveGuard` Story.
TacticRemoveGuard requires complete StoryProof, complete RemoveGuardProof, same-board legal replay, legal remove-guard move, guarded target, removed defender that guarded the target before move, defender no longer guards target after move, writer identity, and no EngineCheck Refutes status.
RemoveGuard Story identity is tactic RemoveGuard, scene Tactic, guard-removing side, target/defender-side rival, guarded target square, removed-defender or moving-piece anchor, and remove-guard route.
RemoveGuard-2 opens no Scene.Material claim, Tactic.Hanging replacement, Defense refutation wording, material win claim, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
RemoveGuard-3 opens only the negative corpus for the narrow `Tactic.RemoveGuard` slice.
RemoveGuard-3 keeps illegal moves, missing same-board proof, king targets, non-guarding defenders, still-guarding defenders, broad claims from other defenders, material-gain claims, Pin/DiscoveredAttack/Skewer misclassification, opponent-reply deflection, overloaded-defender claims, and no-defense, wins-material, and best-move wording silent.
Removing one guard is not winning material. Complete guard-removal proof or silence.
RemoveGuard-3 opens no new proof home, new writer, StoryTable ordering change, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
RemoveGuard-4 opens only existing `EngineCheck` reuse for existing `Tactic.RemoveGuard` Stories.
EngineCheck cannot create RemoveGuard; Supports creates no new claim; Caps suppresses standalone claim or keeps downstream speech bounded when opened later; Refutes blocks the RemoveGuard Story; Unknown creates no engine expression.
RemoveGuard-4 opens no engine-says wording, best-move wording, only-move wording, winning tactic, raw PV explanation, eval number public value, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
RemoveGuard-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, and `Tactic.RemoveGuard` rows.
RemoveGuard-5 keeps selected Verdict stable across input order, keeps RemoveGuard from owning Material or Hanging claims, blocks incomplete Defense responses, prevents duplicate Lead for same-line Pin and RemoveGuard, and keeps actual material change now in Scene.Material.
RemoveGuard-5 opens no new proof home, new Story family, Material claim from RemoveGuard, Hanging claim from RemoveGuard, incomplete Defense response claim, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
RemoveGuard-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.RemoveGuard` Verdicts.
RemoveGuard-6 allows only the `removes_defender` claim key and forbids wins_material, target_is_hanging, no_defense, refutes_defense, best_move, only_move, forced, decisive, creates_pressure, and takes_initiative.
Support, Context, Blocked, capped, and refuted RemoveGuard rows create no standalone claim.
RemoveGuard-6 opens no renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
RemoveGuard-7 opens only deterministic renderer text for selected `Tactic.RemoveGuard` ExplanationPlan.
RemoveGuard-7 renderer input is ExplanationPlan only and may render `{route} removes the defender of the piece on {target}.`
RemoveGuard-7 forbids wins material, leaves it undefended, no defender remains, best move, only move, forces, decisive, refutes the defense, and creates pressure.
RemoveGuard-7 opens no raw Verdict, raw Story, RemoveGuardProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
RemoveGuard-8 opens only LLM smoke for selected RemoveGuard ExplanationPlan and RenderedLine.
RemoveGuard-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`
RemoveGuard-8 forbids raw Story, raw RemoveGuardProof, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, material-win, no-defense, pressure, and initiative claims.
RemoveGuard-8 opens no public/user-facing LLM narration, public route `200`, production API, raw proof repair, or engine explanation.
RemoveGuard Closeout opens no new chess meaning; it audits only BoardFacts guard relation, RemoveGuardProof, Tactic.RemoveGuard, speech key, renderer/LLM, docs authority, test-helper boundary, and closed public surfaces.
RemoveGuard Closeout confirms RemoveGuard owns no Material, Hanging, Defense, Pin, or DiscoveredAttack meaning; deflection, overload, no-defender, and wins-material terms are not live authority; renderer/LLM wording stays no stronger than `removes_defender`; public route `200`, production API, and public/user-facing LLM narration remain closed.
Line / Defender Interaction Hardening is a closed baseline.
Line/Defender hardening opens no new Story. It proves that existing line and defender Stories do not steal each other's meaning.
LDH-0 opens only existing Line/Defender rows interaction hardening, complex same-board fixtures, StoryTable role stability, downstream no-overclaim smoke, and the minimum StoryTable ordering fix if an existing DiscoveredAttack ordering bug is exposed.
LDH-0 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.
LDH-1 opens only the Fixture Map for complex same-board Line/Defender interaction fixtures.
LDH-1 fixtures must state same-board FEN, side to move, candidate legal lines, expected open rows, expected blocked rows, expected Lead/Support/Context/Blocked role, expected selected Verdict, and forbidden claims.
LDH-1 required fixture categories are DiscoveredAttack vs Pin, DiscoveredAttack vs RemoveGuard, Pin vs RemoveGuard, DiscoveredAttack + Pin + RemoveGuard same-board, Line/Defender row vs Material, Line/Defender row vs Hanging, Line/Defender row vs Defense, and EngineCheck Supports/Caps/Refutes over existing Line/Defender rows.
LDH-1 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.
LDH-2 opens only StoryTable role stability checks over existing Line/Defender rows.
LDH-2 verifies input-order stable selected Verdicts, no duplicate Lead for the same meaning, incomplete rows not Lead, refuted rows Blocked, capped rows without standalone claims, and separated ownership for same-line Pin/DiscoveredAttack, Pin/RemoveGuard, and DiscoveredAttack/RemoveGuard collisions.
LDH-2 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.
LDH-3 opens only Meaning Ownership Boundary checks over existing Line/Defender and collision rows.
LDH-3 fixes each row to its own meaning: DiscoveredAttack reveals one slider attack on one material target, Pin pins one non-king piece to its own king on a line, RemoveGuard removes one defender guard relation from one target, Scene.Material owns actual material balance change now, Tactic.Hanging owns capturable target with bounded material gain proof, and Scene.Defense owns complete ThreatProof plus DefenseProof preventing immediate material loss.
LDH-3 forbids RemoveGuard material-gain claims, Pin cannot-move or king-unsafe claims, DiscoveredAttack wins-material claims, Material line-tactic identity, and Defense stopped-threat wording without complete threat proof.
LDH-3 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.
LDH-4 opens only existing EngineCheck interaction checks over existing Line/Defender rows.
LDH-4 verifies Supports creates no new claim, Caps suppresses allowed claim or keeps downstream speech bounded, Refutes blocks the checked Story, and Unknown creates no engine-related expression.
LDH-4 forbids engine-says wording, raw PV explanation, eval number public value, best move, only move, forced line, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.
LDH-5 opens only close false-positive negative corpus tests over existing Line/Defender rows and already-open collision rows.
LDH-5 keeps line-open-without-attack, king targets, pin-looking lines without king-behind-target, still-guarding defenders, incomplete Material or Hanging proof after defender removal, incomplete DiscoveredAttack or Pin proof, wrong-board or stale same-board proof, route mismatch, engine-refuted plausible rows, and Skewer-looking relations silent.
LDH-5 rule: Looks like a line tactic is not enough. Existing complete proof or silence.
LDH-5 opens no new Story family, proof home, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.
LDH-6 opens only downstream boundary smoke over selected Lead Verdicts from existing Line/Defender rows.
LDH-6 verifies ExplanationPlan receives selected Verdict only, Renderer receives ExplanationPlan only, LLM smoke prompt carries only renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction, and Support, Context, Blocked, capped, and refuted rows create no standalone text.
LDH-6 forbids new renderer templates, new LLM behavior, and raw Story, Proof, or EngineCheck passing into renderer or LLM smoke.
LDH-6 opens no new Story family, proof home, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.
LDH-7 opens only diagnostics boundary smoke over existing Line/Defender rows, StoryTable, selected Verdict, ExplanationPlan, renderer, LLM smoke, and test-helper authority.
LDH-7 verifies proofFailures stay internal diagnostic only, raw proof text stays out of Verdict.values, EngineCheck text does not flow directly into ExplanationPlan, StoryTable debug relations do not become renderer wording, and test helpers do not become runtime authority.
LDH-7 opens no new Story family, proof home, renderer wording, LLM behavior, runtime authority helper, raw Story, raw Proof, raw EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.
LDH Closeout opens no chess meaning. It only audits the Line / Defender Interaction Hardening surface.
Completion standard: LDH closes as interaction hardening only, with no new Story family, no new proof home, no mixed LineFact, LineProof, PinProof, or RemoveGuardProof authority, no Line/Defender meaning theft, no Material, Hanging, or Defense claim-home invasion, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no public route 200, no production API, and no public/user-facing LLM narration.
Skewer Slice is the current scope.
Current implementation scope is Line / Defender Contact Neighborhood late vertical slice.
Skewer-0 opens only the charter for the fourth narrow line/defender contact vertical slice.
First Skewer positive scope is not a broad skewer tactic.
Skewer first scope: a legal move creates or reveals a slider attack on one front non-king material target, with a second non-king material target behind it on the same line.
LineFact observes geometry. SkewerProof proves the front-and-back target relation. Tactic.Skewer may speak only after proof.
Skewer-0 opens only narrow Tactic.Skewer, one slider, one front target, one rear target, front/rear target same-line relation, a legal move that creates or reveals the front/rear relation, and bounded skewer wording after selected Verdict only.
Skewer-0 opens no broad LineTactic, XRay public Story, Pin expansion, RemoveGuard expansion, material win claim, front piece must move, wins rear piece, forced line, best move, only move, winning, decisive, king safety, mate threat, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.
Skewer-1 opens only `SkewerProof` as a narrow proof home.
SkewerProof proves side creating the skewer, rival side, skewer slider, front target, rear target, legal skewer or revealing move, line kind, same-board proof, front target non-king material piece, rear target non-king material piece, front and rear target same rival side, after move slider attacks front target, rear target behind front target on the same ray, no extra blocker breaks the front-to-rear relation, and before move the skewer relation was absent or blocked.
SkewerProof is not a public Story. LineFact and LineProof are not public Stories.
SkewerProof says no material gain, front piece must move, or wins rear piece; proofFailures stay out of renderer/LLM input.
Skewer-1 opens no Tactic.Skewer writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration.
Skewer-2 opens only the named `TacticSkewer` writer for one narrow `Tactic.Skewer` Story.
TacticSkewer requires complete StoryProof, complete SkewerProof, same-board legal replay, legal skewer or revealing move, front target, rear target, slider, complete front-and-back line relation, writer identity, and no EngineCheck Refutes status.
Skewer Story identity is tactic Skewer, scene Tactic, skewer-creating side, front/rear target side as rival, front target square, rear target square as secondaryTarget, slider or moved-piece anchor, and skewer/revealing route.
TacticSkewer creates no Scene.Material claim, does not replace Tactic.Pin, and keeps rear-target king positions silent.
Skewer-2 opens no StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, Pin replacement, public route `200`, production API, or public/user-facing LLM narration.
Skewer-3 opens only the negative corpus for the narrow `Tactic.Skewer` slice.
Skewer-3 keeps illegal moves, missing same-board proof, non-sliders, missing front target, missing rear target, front or rear king targets, front/rear targets not on the same rival side, rear targets not behind the front target on the same line, extra blockers between front and rear target, DiscoveredAttack-only lines, Pin-looking positions, front-piece-must-move assumptions, and material-win, forced, or best-move wording silent.
Skewer-3 rule: Two pieces on a line is not enough. Complete front-and-back skewer proof or silence.
Skewer-3 opens no new proof home, new writer, StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, Scene.Material claim, Pin replacement, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration.
Skewer-4 opens only existing `EngineCheck` reuse for existing `Tactic.Skewer` Stories.
EngineCheck cannot create Skewer; Supports creates no new claim; Caps suppresses standalone claim or keeps downstream speech bounded when opened later; Refutes blocks the Skewer Story; Unknown creates no engine expression.
Skewer-4 opens no engine-says wording, best-move wording, only-move wording, forced-win wording, winning tactic, raw PV explanation, eval number public value, StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
Skewer-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` rows.
Skewer-5 keeps selected Verdict stable across input order, prevents duplicate Lead for DiscoveredAttack and Skewer, keeps Skewer from owning Material, Pin king-relation, or RemoveGuard defender-relation claims, keeps actual material change now in Scene.Material, and keeps incomplete front/rear relations silent so only complete existing rows remain.
Skewer-5 opens no new proof home, new Story family, broad LineTactic, broad XRay, Material claim from Skewer, DiscoveredAttack duplicate Lead from Skewer, Pin king relation from Skewer, RemoveGuard defender relation from Skewer, ExplanationPlan mapping, renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
Skewer-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Skewer` Verdicts.
Skewer-6 allows only the `skewers_piece_to_piece` claim key and forbids wins_material, wins_rear_piece, front_piece_must_move, best_move, only_move, forced, decisive, king_unsafe, mate_threat, creates_pressure, and takes_initiative.
Support, Context, Blocked, capped, and refuted Skewer rows create no standalone claim.
Skewer-6 opens no renderer wording, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
Skewer-7 opens only deterministic renderer text for selected `Tactic.Skewer` ExplanationPlan.
Skewer-7 renderer input is ExplanationPlan only and may render `{route} skewers the piece on {target} to the piece on {secondaryTarget}.`
Skewer-7 forbids wins material, wins the piece behind it, the front piece must move, best move, only move, forces, decisive, king is unsafe, threatens mate, and creates pressure.
Skewer-7 opens no raw Verdict, raw Story, SkewerProof, LineFact, LineProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.
Skewer-8 opens only LLM smoke for selected Skewer ExplanationPlan and RenderedLine.
Skewer-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`
Skewer-8 forbids raw Story, raw SkewerProof, raw LineProof, BoardFacts, EngineCheck, raw PV, proofFailures, new move, new line, material-win, forced, pressure, initiative, and mate claims.
Skewer-8 opens no public/user-facing LLM narration, public route `200`, production API, raw proof repair, or engine explanation.
Skewer Closeout opens no new chess meaning; it audits only LineFact, LineProof, SkewerProof, Tactic.Skewer, speech key, renderer/LLM, docs authority, test-helper boundary, and closed public surfaces.
Skewer Closeout confirms Skewer owns no Material, Hanging, Pin, DiscoveredAttack, RemoveGuard, or Defense meaning; front-piece-must-move, wins-rear-piece, wins-material, and forced-skewer terms are not live authority; renderer/LLM wording stays no stronger than `skewers_piece_to_piece`; public route `200`, production API, and public/user-facing LLM narration remain closed.
Skewer Closeout also audits meaning, authority, terminology, and document duplication without duplicating detailed rules outside StoryInteractionLaw.md.
Line / Defender closeout summaries are non-authoritative: `StoryInteractionLaw.md` owns LNC-0 through LNC-7 and final completion detail, while this document only records that `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` close as four narrow proof-backed slices with broad line/ray/XRay, pressure, initiative, material-win, public route `200`, production API, and public/user-facing LLM narration surfaces closed.
Current implementation scope is Pawn / Promotion Neighborhood closeout.
PawnAdvance-0 is owned by `StoryInteractionLaw.md`. It opens only narrow `Scene.PawnAdvance` over `PawnAdvanceProof` for an already-passed non-king pawn making a legal one-square non-capturing non-promotion advance and remaining passed on the exact after-board. The only bounded claim key is `advances_passed_pawn`; broad PawnTactic, `Tactic.PawnPush`, promotion threat, unstoppable pawn, winning endgame, conversion, pawn race, strategy, public route `200`, production API, and public/user-facing LLM narration remain closed.
PawnStop-0 is owned by `StoryInteractionLaw.md`. It opens only narrow `Scene.PawnStop` over `PawnStopProof` for a legal move directly stopping an already-passed pawn's next non-promotion advance square on the exact after-board. The only bounded claim key is `stops_pawn_advance`; broad pawn defense, promotion stop, permanent stop, tablebase draw, best defense, only move, endgame hold, conversion stopped, pawn race, king route, opposition, broad pawn strategy, public route `200`, production API, and public/user-facing LLM narration remain closed.
PawnStop Closeout opens no new chess meaning; it audits only `PassedPawnObservation`, `PawnStopProof`, `Scene.PawnStop`, `stops_pawn_advance`, renderer/LLM bounds, docs authority, and closed public surfaces. PawnStop Closeout confirms PawnStop owns no PawnAdvance, PromotionThreat, Promotion, PawnBreak, Defense, Material, Hanging, or Line/Defender tactic meaning; permanent stop, draw, tablebase, best defense, and only move remain forbidden wording, not live authority.
PromotionThreat-0 is owned by `StoryInteractionLaw.md`. It opens only narrow `Scene.PromotionThreat` over `PromotionThreatProof` for a legal pawn move that creates a legal next-move promotion route on the exact after-board. The only bounded claim key is `threatens_promotion_next`; actual Promotion, unstoppable pawn, cannot-be-stopped, winning endgame, conversion, tablebase, pawn race, best move, only move, forced win, no-counterplay, public route `200`, production API, and public/user-facing LLM narration remain closed.
Promotion-1 is owned by `StoryInteractionLaw.md`. It adds only `PromotionProof` as a diagnostic proof home for legal non-capturing pawn promotion on the exact board, including promotion square and promoted piece identity. `Scene.Promotion`, bounded promotion wording, capture promotion, material result, conversion, winning/endgame/tablebase claims, public route `200`, production API, and public/user-facing LLM narration remain closed.
Promotion-2 is owned by `StoryInteractionLaw.md`. It opens only the named `ScenePromotion` writer and `Scene.Promotion` Story identity for complete `PromotionProof`; PromotionThreat, Material, winning, conversion, tablebase, ExplanationPlan, renderer, LLM narration, public route `200`, and production API remain closed.
PIH-0 is owned by `StoryInteractionLaw.md`. It opens no new pawn meaning; it hardens existing `Scene.PawnAdvance` and `Scene.PawnStop` interactions so pawn rows do not steal promotion, conversion, defense, material, or tactic meaning, while public route `200`, production API, and public/user-facing LLM narration remain closed.
PIH-1 is owned by `StoryInteractionLaw.md`. It opens only the Pawn Interaction Hardening fixture map over already-open rows and keeps PromotionThreat, Promotion, PawnBreak, tablebase, endgame-result, public route `200`, production API, and public/user-facing LLM narration closed.
PIH-2 is owned by `StoryInteractionLaw.md`. It opens only StoryTable role stability checks over already-open pawn and collision rows, including input-order stability, duplicate Lead prevention, incomplete/refuted/capped row boundaries, and pawn rows not owning Defense, promotion, or conversion claims.
PIH-3 is owned by `StoryInteractionLaw.md`. It opens only Meaning Ownership Boundary checks proving each already-open pawn, material, defense, and line/defender row keeps its own claim and cannot steal promotion, conversion, endgame, defense, pawn progress, or sibling tactic meaning.
PIH-4 is owned by `StoryInteractionLaw.md`. It opens only EngineCheck interaction checks over already-open PawnAdvance and PawnStop rows, reusing existing Supports, Caps, Refutes, and Unknown boundaries without engine-owned claims, raw PV, eval numbers, best move, only move, tablebase-like authority, public route `200`, production API, or public/user-facing LLM narration.
PIH-5 is owned by `StoryInteractionLaw.md`. It opens only the close pawn false-positive negative corpus and enforces existing complete proof or silence for not-passed advances, missing exact after-board replay, stop-looking non-stops, long-term blockade, promotion-looking, tablebase-looking, opposition-looking, pawn-race-looking, route mismatch, stale/wrong-board, and refuted pawn rows.
PIH-6 is owned by `StoryInteractionLaw.md`. It opens only downstream boundary smoke for already-open PawnAdvance and PawnStop rows, sends only selected Lead Verdict data through ExplanationPlan, renderer, and LLM smoke, and keeps Support, Context, Blocked, capped, and refuted rows textless while forbidding promotion, unstoppable, winning, conversion, draw/hold, tablebase, best/only, forced, pressure, initiative, public route `200`, production API, and public/user-facing LLM narration.
PIH-7 is owned by `StoryInteractionLaw.md`. It opens only diagnostics boundary hardening for already-open PawnAdvance and PawnStop rows, keeping proofFailures, raw proof text, EngineCheck text, StoryTable debug relations, and test-helper terminology out of Verdict values, ExplanationPlan, renderer wording, LLM smoke, production API, public route `200`, and public/user-facing LLM narration.
PIH Closeout is owned by `StoryInteractionLaw.md`. It opens no new pawn meaning, Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration; it confirms PawnAdvance and PawnStop remain separated from each other, promotion, pawn-break, tablebase, race, king-route, opposition, Material, Defense, Hanging, and Line/Defender homes.
Pawn / Promotion closeout summaries are non-authoritative: `StoryInteractionLaw.md` owns PNC-0 through PNC-7 and final completion detail, while this document only records that `Scene.PawnAdvance`, `Scene.PawnStop`, `Scene.PromotionThreat`, and `Scene.Promotion` close as four narrow proof-backed slices; PawnBreak was not opened by PNC and is now governed only by the later PawnBreak-0 charter, while broad PawnTactic, pawn strategy, conversion, winning endgame, tablebase, pawn race, king route, opposition, public route `200`, production API, and public/user-facing LLM narration surfaces remain closed.
PawnBreak-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the Pawn Structure / Break Neighborhood charter; runtime opens only narrow `Scene.PawnBreak` through `PawnBreakProof` for a legal pawn move creating one direct rival-pawn lever on the exact board, with broad PawnTactic, pawn strategy, opens-position, breaks-through, passed-pawn, weakens-structure, wins-space, initiative, pressure, conversion, public route `200`, production API, and public/user-facing LLM narration closed.
PawnCapture-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the second Pawn Structure / Break Neighborhood charter; runtime opens only narrow `Scene.PawnCapture` through `PawnCaptureProof` for a legal pawn move capturing one rival pawn on the exact board, with material gain, wins-pawn, passed-pawn, open-file, structure advantage, breakthrough, initiative, pressure, conversion, public route `200`, production API, and public/user-facing LLM narration closed.
PassedPawnCreated-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the third Pawn Structure / Break Neighborhood charter; runtime opens only narrow `Scene.PassedPawnCreated` through `PassedPawnCreatedProof` for a legal non-promotion pawn move creating exactly one newly passed pawn on the exact after-board, with promotion threat, actual promotion, unstoppable passer, conversion, winning endgame, pawn race, tablebase, breakthrough, initiative, pressure, public route `200`, production API, and public/user-facing LLM narration closed.
PSBNC-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the Pawn Structure / Break Neighborhood closeout; it opens no new chess meaning and closes only `Scene.PawnBreak`, `Scene.PawnCapture`, and `Scene.PassedPawnCreated` as three narrow proof-backed event slices with separate proof homes and speech keys.
FileOpened-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the File Opened Neighborhood charter; runtime opens only narrow `Scene.FileOpened` through `FileOpenedProof` for a legal pawn move that leaves its origin file with no pawns on the exact after-board, with rook activity, file control, pressure, initiative, weakness, breakthrough, material gain, passed-pawn creation, pawn-majority change, best/only/forced, public route `200`, production API, and public/user-facing LLM narration closed.
FPSNC-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the File / Pawn Structure Neighborhood closeout; it opens no new chess meaning and closes only `Scene.PawnBreak`, `Scene.PawnCapture`, `Scene.PassedPawnCreated`, and `Scene.FileOpened` as four narrow proof-backed event slices with separate proof homes and speech keys; broad pawn-structure and file interpretation remains closed.
PBFNC-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the Pawn Blocking / Fixed Pawn Neighborhood closeout; it opens no new chess meaning and closes exactly one already-open narrow public event, `Scene.PawnBlock`, where a move blocks one rival pawn from advancing, with fixed-pawn, weak-pawn, blockade, restriction, pressure, initiative, best/only move, public route `200`, production API, and public/user-facing LLM narration closed.
Loose-0 summary is non-authoritative: `StoryInteractionLaw.md` owns the Loose Piece charter; the charter admits only narrow `Tactic.Loose` through `LoosePieceProof` and `TacticLoose` for a legal move that leaves the moving side attacking one undefended rival non-king piece on the exact after-board, with Hanging, Material, wins-piece, wins-material, free-piece, en-prise, underdefended, overloaded-defender, pressure, initiative, tempo, best/only/forced move, public route `200`, production API, and public/user-facing LLM narration closed.
Public route `200`, production API, and public/user-facing LLM narration remain closed.
Middlegame Interaction Hardening is a closed baseline.
Middlegame Interaction Hardening opens no chess meaning. It stress-tests already-open meanings.
MIH-0 opens no new Story family, proof home, renderer wording, public route, production API, or public/user-facing LLM narration.
MIH-0 opens only interaction hardening among existing Hanging, Fork, Material, and Defense rows.
MIH-0 opens complex middlegame fixture based role stability checks.
MIH-0 checks selected Verdict, ExplanationPlan, and renderer/LLM smoke boundary stability without opening new speech.
MIH-0 may apply only the minimum StoryTable ordering fix if an existing ordering bug is exposed.
MIH-1 opens only complex middlegame test fixtures for already-open Hanging, Fork, Material, and Defense rows.
Completion standard: Fixture Map names board, rows, roles, selected Verdict, and forbidden claims without opening new meaning.
MIH-2 opens only StoryTable role stability checks over existing Hanging, Fork, Material, and Defense rows.
Completion standard: Role Stability keeps selected Verdict deterministic, prevents duplicate Lead, blocks incomplete or refuted rows, and keeps capped rows from standalone strong claims without opening new meaning.
MIH-3 opens only the Material vs Defense collision rule over existing `Scene.Material` and `Scene.Defense` rows.
Completion standard: Material vs Defense collision selects actual material change now over prevented immediate loss, blocks speculative material loss, and keeps both public boundaries bounded.
MIH-4 opens only existing EngineCheck interaction checks over already-open Hanging, Fork, Material, and Defense rows.
Completion standard: EngineCheck Interaction reuses existing EngineCheck statuses, keeps Supports and Unknown non-speaking, suppresses or weakens Caps, blocks Refutes, and prevents engine eval, raw PV, engine-says, best-move, and eval-number public leakage.
MIH-5 opens only close false-positive negative corpus tests over already-open Hanging, Fork, Material, Defense, and EngineCheck rows.
Completion standard: Negative Corpus keeps close false positives silent unless complete proof exists, and no plausible-looking row may reach selected public output through StoryTable, ExplanationPlan, renderer, or LLM smoke boundaries.
MIH-6 opens only downstream boundary smoke over selected Verdict, existing ExplanationPlan, existing DeterministicRenderer, and existing LLM smoke.
Completion standard: Downstream Boundary Smoke passes only selected Lead Verdict data through existing ExplanationPlan, renderer, and LLM smoke boundaries, while non-Lead, capped, and refuted rows stay silent.
MIH-7 opens only diagnostics boundary smoke over already-open Hanging, Fork, Material, Defense, StoryTable, selected Verdict, ExplanationPlan, renderer, and LLM smoke.
Completion standard: Diagnostics Boundary keeps proofFailures, raw proof failure text, engine text, source row data, and StoryTable debug relations out of public meaning, Verdict.values, ExplanationPlan source inputs, renderer wording, and LLM smoke prompts.
MIH Closeout opens no chess meaning. It only audits the MIH hardening surface.
Completion standard: MIH closes as interaction hardening only, with no new Story family, no new proof home, no duplicate meaning owner, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no promoted test helper, no public route 200, no production API, and no public/user-facing LLM narration.
Material vs Defense is the first and highest-risk case because `Scene.Defense` prevents immediate material loss while `Scene.Material` describes material balance changing now.
Middlegame Interaction Hardening checks StoryTable role stability, selected Verdict stability, ExplanationPlan stability, and renderer/LLM smoke boundaries among already-open Hanging, Fork, Material, and Defense rows.
Same-board Material vs Defense collisions are resolved by actual material change now over prevented immediate loss, with speculative material loss blocked.
Defense-0 opened only the charter for the first narrow `Scene.Defense` slice.
Defense-1 opened only `ThreatProof`.
Defense-1 opens only `ThreatProof`.
Defense requires a threat. ThreatProof proves what must be stopped.
DefenseProof proves how the move stops it.
DefenseProof proves how a specific move stops a specific ThreatProof.
Defense is not no-counterplay.
Defense-2 opens only `DefenseProof`.
Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story.
Defense-4 opens only the Defense negative corpus.
Defense-looking false positives must stay silent without complete ThreatProof and complete DefenseProof.
Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories.
EngineCheck may support, cap, or refute an existing Defense Story, but it does not create Defense.
Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows.
StoryTable deterministically orders Hanging, Fork, Material, and Defense without creating new chess meaning.
Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts.
Defense ExplanationPlan creates no meaning stronger than the selected Verdict.
Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan.
Renderer text is no stronger than the Defense ExplanationPlan.
Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine.
LLM smoke does not make Defense text stronger.
Defense Slice Closeout opens no new chess meaning beyond the narrow `Scene.Defense` vertical slice.
Defense closes as a narrow proof-backed attacked-piece material-loss defense slice only.
Public route `200`, production API, and public/user-facing LLM narration remain closed.
The completed Stage 8, Fork-9, Material Slice Closeout, Defense-0, Defense-1, Defense-2, Defense-3, Defense-4, Defense-5, Defense-6, Defense-7, Defense-8, Defense-9, and Defense Slice Closeout scopes remain closed baselines.
Stage 1 Board Facts, Stage 2 Story Proof, and Stage 3 first narrow positive
Story are prerequisites. Stage 3 remains open only for Material proof kernel,
`Tactic.Hanging`, Hanging negative corpus, the narrow `Tactic.Fork` vertical
slice, the narrow `Scene.Material` writer, and Material negative corpus.
Stage 4 is named `Engine Check`. Stage 4 opens only internal EngineCheck
evidence, same-board and stale guards, `Tactic.Hanging` attachment, narrow `Tactic.Fork`
attachment, narrow `Scene.Material` EngineCheck reuse, false-positive corpus, and
conservative StoryTable diagnostics for existing `Tactic.Hanging`, narrow
`Tactic.Fork`, and single proof-backed `Scene.Material` Stories. Stage 5 is
named `Story Order` and opens only role ordering for existing
`Tactic.Hanging` Story rows and existing narrow `Tactic.Fork` Story rows;
Material-3 adds only single-row StoryTable admission for proof-backed
`Scene.Material`; Material-4 adds only the Material negative corpus;
Material-5 reuses only existing `EngineCheck` for `Scene.Material`;
Material-6 adds only StoryTable integration for existing Hanging, Fork, and
Material rows. Stage 5-1
assigns Lead, Support, Context, and
Blocked roles inside that slice. Stage 5-2 fixes deterministic ordering inputs
for those StoryTable rows. Stage 5-3 tightens close blockers and context
relations for those rows. Stage 5-4 keeps Verdict diagnostics out of public
numeric values and downstream public surfaces. Stage 5 closeout confirmed
Story ordering only and selected-Verdict handoff only. Stage 6-0 opens only
the Explanation Plan
charter and selected-Verdict speech boundary; it does not open sentences,
renderer, LLM, public route `200`, pedagogy, new Story families, or engine
explanation. Stage 6-1 opens only the Explanation Plan shape for one selected
`Tactic.Hanging` Lead Verdict. `allowedClaim` stays a structured key such as
`can_win_piece`; the first shape carries `bounded` strength and forbidden
wording, not public prose. Stage 6-2 opens only `Tactic.Hanging` allowed claim
mapping. Uncapped Lead Verdict only may carry an allowed claim key. Support,
Context, Blocked, and engine-capped Verdicts do not create standalone public
claims; `engineStrengthLimited` suppresses claim keys and strengthens
forbidden wording. Stage 6-3 opens only forbidden wording boundary.
Explanation Plan must carry the default forbidden wording set.
`Tactic.Hanging` remains bounded material tactic wording only, and
`engineStrengthLimited` strengthens forbidden wording without carrying a claim.
Stage 6-4 opens only Support and Context relation structure. Uncapped Lead
only carries an allowed claim. Support and Context create no standalone public
claim. Blocked remains debug-only relation structure, and proofFailures do not
feed relation wording.
Stage 6-5 opens only the selected Verdict input guard. Explanation Plan accepts
selected Verdict only. Raw BoardFacts, BoardMood, root atoms, CaptureResult,
EngineCheck, EngineEval, EngineLine, raw PV, proofFailures text, unselected
Story, unselected Verdict, and source rows remain forbidden inputs.
Material-7 opens only ExplanationPlan mapping for selected `Scene.Material`
Verdicts. It admits selected Verdict only, emits `material_balance_changes`
first for uncapped Lead only, and opens no Material renderer text, LLM smoke,
public route `200`, production API, winning, decisive, conversion, blunder,
best-move, forced-win, or no-counterplay claim.
Material-8 opens only deterministic renderer text for selected
`Scene.Material` ExplanationPlan. Renderer receives `ExplanationPlan` only and
may render bounded Material text such as `After {route}, White comes out ahead
in material.` It opens no LLM smoke, public route `200`, production API,
winning, decisive, blunder, forced, best-move, no-counterplay, engine-says,
conversion, technically-winning, or full-evaluation claim.
Material-9 opens only LLM smoke for selected Material ExplanationPlan and
RenderedLine. 8B Material Codex CLI input is limited to renderedText,
claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess
facts.` It reads no raw Verdict, Story, material proof, CaptureResult,
ExchangeResult, EngineCheck, BoardFacts, engine eval, raw PV, proofFailures,
or source row data, and opens no public/user-facing LLM narration, public
route `200`, production API, winning, decisive, forced, blunder, best-move,
conversion, or stronger claim.
Material Slice Closeout opens no new chess meaning beyond the narrow
`Scene.Material` vertical slice. `CaptureResult` remains the simple capture
and immediate bounded recapture proof home, `ExchangeResult` remains unopened,
`Scene.Material` owns only the Story label, and `material_change` remains
speech-claim vocabulary with runtime key `material_balance_changes`. The next
named slice remains `Scene.Defense`; Material does not open Defense, Conversion,
Winning, Plan, Strategy, or Blunder.
Stage 6 closeout confirms Explanation Plan only. Blocked, Support, Context,
engine-capped, and engine-refuted Verdicts create no allowed claim or public
claim. Stage 7 deterministic renderer may receive Explanation Plan only and
must not read raw Verdict, EngineCheck, CaptureResult, Board Facts, BoardMood,
raw PV, proofFailures text, source rows, or raw engine evidence directly.
Stage 7-0 opens only the Deterministic Renderer charter: `ExplanationPlan`
only input, deterministic template, `Tactic.Hanging` bounded claim phrasing,
forbidden wording check, no LLM, and no public route. Raw Verdict, Story,
Board Facts, CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV,
proofFailures text, source rows, user-level pedagogy, best-move wording,
engine-says wording, winning, decisive, forced, blunder, and free-piece
wording remain closed. Stage 7-1 opens only the Renderer input guard:
Renderer receives `ExplanationPlan` only and exposes no raw Verdict, Story,
BoardFacts, BoardMood, CaptureResult, EngineCheck, EngineEval, EngineLine, raw
PV, proofFailures, or source-row input. Stage 7-2 opens only the minimal
`Tactic.Hanging` template for the `can_win_piece` claim key; the text must not
exceed the ExplanationPlan claim key or evidenceLine. Stage 7-3 opens only
forbidden wording enforcement. Renderer output must not violate
`ExplanationPlan.forbiddenWording`; `win material` wording is allowed only when
`allowedClaim` is `CanWinPiece`, `winning position` remains forbidden, and
engine-strength-limited plans must fail strong wording output. Stage 7-4
opens only the no-standalone-text boundary. Renderer phrases only Lead plans
with an allowed claim; Support, Context, Blocked, capped no-claim, and
engine-refuted relation plans produce no text. Stage 7-5 opens only the
RenderedLine shape. `RenderedLine` owns no chess meaning, proof, or engine
data; RenderedLine is only the expression result of ExplanationPlan. Stage 7-6
opens only renderer baseline tests. Renderer output is no stronger than
ExplanationPlan; there is no new renderer wording, no new input, no public
route `200`, and no LLM narration. Stage 7 closeout confirms deterministic
renderer is closed as a template baseline. Stage 8 opens only 8A Mock narrator
and 8B Codex CLI prompt smoke test. Stage 8B Codex CLI prompt smoke may
receive renderedText, claimKey, strength, forbidden wording list, and the
instruction `Rephrase only. Do not add chess facts.` only. 8A Mock narrator may
receive ExplanationPlan and RenderedLine only. Production API validation
remains closed. Stage 8 must not read raw Verdict, Story, EngineCheck,
CaptureResult, Board Facts, BoardMood, raw PV, proofFailures text, or source
rows directly. Fork-8 opens only deterministic renderer text for selected Fork
ExplanationPlan, using route, target, and secondaryTarget already lowered from
the selected Verdict. Fork-9 opens only LLM smoke for selected Fork
ExplanationPlan and RenderedLine; 8B may receive renderedText, claimKey,
strength, forbidden wording list, and the instruction `Rephrase only. Do not
add chess facts.` only. It does not open public/user-facing Fork LLM narration,
public route `200`, production API, pedagogy, material claims, wins-queen
claims, engine PV commentary, best-move explanation, or sibling tactic
families. Stages 9-11 remain a dependency map, not permission to open those
systems in this branch checkpoint.

`StoryInteractionLaw.md` owns the Stage 3 charter. This README only summarizes
the current scope: backend Material proof evidence, the named
`Tactic.Hanging` writer, the narrow `Tactic.Fork` proof/writer vertical slice,
and the narrow `Scene.Material` writer are open; other positive families,
public/user-facing Fork LLM
narration, and public route `200` are still closed.

`StoryInteractionLaw.md` owns the Stage 4 charter. This README only summarizes
the current scope: Story comes first, and engine evidence may check, cap, or
refute only an existing `Tactic.Hanging`, narrow `Tactic.Fork`, or narrow
`Scene.Material` Story. Engine eval, engine line, reply line, and checked move
data cannot create a Story, rank a Story, write a `Verdict`, feed a renderer,
feed an LLM, or become public truth.

`StoryInteractionLaw.md` owns the Stage 5 charter. This README only summarizes
the current scope: StoryTable may assign roles among existing
`Tactic.Hanging` Story rows and existing narrow `Tactic.Fork` Story rows.
Material-3 adds only single-row StoryTable admission for proof-backed
`Scene.Material`; Material-4 adds only the Material negative corpus;
Material-5 reuses only existing `EngineCheck` for `Scene.Material`;
Material-6 adds only StoryTable integration for existing Hanging, Fork, and
Material rows. It does
not create Stories, open another positive family, or turn engine eval, Board
Facts, `CaptureResult`, or MultiTargetProof into direct public claims.
This README summarizes Stage 5-1 only: complete Hanging rows may lead or
support, refuted/incomplete/captureless rows are blocked, unknown engine checks
create no engine claim, and roles do not open renderer or LLM.
This README summarizes Stage 5-2 only: ordering may use role eligibility,
publicStrength, Story identity, writer presence, and blocked status; it must
not use raw engine eval, raw PV text, proofFailures text, Board Facts row
count, `CaptureResult` text, renderer wording, or input order.
This README summarizes Stage 5-3 only: close Hanging blockers are enforced,
Quiet remains fallback-only, and Source/Opening context cannot outrank
board-backed Hanging; plan, blunder, defense, extra counterplay, and strategy
relations remain closed.
This README summarizes Stage 5-4 only: Verdict values keep their fixed shape,
proofFailures and EngineCheck diagnostics stay out of values,
engineStrengthLimited stays internal, Verdict is not public text, and renderer,
LLM, and public route remain closed.
This README summarizes Stage 5 closeout only: Stage 5 closed as StoryTable
ordering over existing Hanging rows, with no new chess meaning or public
surface, and Stage 6 may receive only selected Verdict data rather than raw
facts, engine sidecars, or capture evidence.

`StoryInteractionLaw.md` owns the Stage 6 charter. This README only summarizes
the current scope: Stage 6 is named `Explanation Plan`, with `Explanation IR`
as a parenthetical data-shape label. Stage 6-0 receives selected Verdict data
only to bound claim, evidence, strength, role, support/context relation, and
forbidden wording; raw board facts, engine sidecars, source rows, diagnostics,
renderer wording, and LLM wording remain outside the input boundary.
This README summarizes Stage 6-1 only: one selected `Tactic.Hanging` Lead
Verdict may become an internal `ExplanationPlan` shape with role, scene,
tactic, side, target, anchor, route, allowedClaim, evidenceLine, strength,
forbiddenWording, and empty supportContextLinks. It still writes no sentence.
This README summarizes Stage 6-2 only: uncapped `Tactic.Hanging` Lead
Verdicts may map to a safe structured claim key, while Support, Context,
Blocked, and engine-capped Verdicts create no standalone public claim.
`engineStrengthLimited` suppresses claim keys and tightens forbidden wording.
This README summarizes Stage 6-3 only: Explanation Plan carries the default
forbidden wording set for downstream renderer and LLM layers. `Tactic.Hanging`
remains bounded material tactic wording only, and `engineStrengthLimited`
strengthens forbidden wording without carrying a claim.
This README summarizes Stage 6-4 only: Support and Context enter Explanation
Plan as structured relations to Lead. Support, Context, Blocked, and
engine-capped Verdicts create no standalone public claim. Blocked remains
debug-only relation structure, and proofFailures text does not feed relation
wording.
This README summarizes Stage 6-5 only: Explanation Plan receives selected
Verdict only. It accepts no raw proof material, unselected Story, unselected
Verdict, source row, or proofFailures wording.
This README summarizes Material-7 only: selected `Scene.Material` Verdicts may
lower into ExplanationPlan data with bounded strength, forbidden wording, and
the first emitted `material_balance_changes` claim key for uncapped Lead only.
Support, Context, Blocked, capped, and engine-refuted Material plans create no
standalone public claim. Material proof, `CaptureResult`, `ExchangeResult`,
`EngineCheck`, `BoardFacts`, raw PV, proofFailures, source rows, Material
renderer text, LLM smoke, public route `200`, production API, winning,
decisive, conversion, blunder, best-move, forced-win, and no-counterplay remain
closed.
This README summarizes Material-8 only: deterministic renderer text may phrase
selected Material ExplanationPlan through `ExplanationPlan` input only. The
first route template is `After {route}, White comes out ahead in material.`
The text must be no stronger than the Material ExplanationPlan and must not say
winning, decisive, blunder, forced, best move, no counterplay, engine says,
conversion, or technically winning.
This README summarizes Stage 6 closeout only: Stage 6 closes with Explanation
Plan only, without renderer, LLM, public route, user-facing prose, or pedagogy.
Blocked, Support, Context, engine-capped, and engine-refuted Verdicts create
no allowed claim, and Stage 7 deterministic renderer may receive Explanation
Plan only.

`StoryInteractionLaw.md` owns the Stage 7-0 charter. This README only
summarizes the current scope: Stage 7 is named `Deterministic Renderer`, and
Stage 7-0 opens only `ExplanationPlan` input, deterministic template,
`Tactic.Hanging` bounded claim phrasing, forbidden wording check, no LLM, and
no public route. It does not open raw Verdict, Story, Board Facts,
CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV, proofFailures
text, source rows, user-level pedagogy, engine PV explanation, best-move
explanation, engine-says wording, winning, decisive, forced, blunder, or
free-piece wording.

`StoryInteractionLaw.md` owns the Stage 7-1 input guard. This README only
summarizes the current scope: Stage 7-1 opens only the Renderer input guard.
Renderer receives `ExplanationPlan` only, exposes no raw Verdict, Story,
BoardFacts, BoardMood, CaptureResult, EngineCheck, EngineEval, EngineLine, raw
PV, proofFailures, or source-row input, and cannot create a sentence without
an `ExplanationPlan`.

`StoryInteractionLaw.md` owns the Stage 7-2 template. This README only
summarizes the current scope: Stage 7-2 opens only the minimal
`Tactic.Hanging` template for the `can_win_piece` claim key, with Lead,
bounded strength, non-debug, route, target, evidenceLine, and forbidden
wording present. The text must not exceed the ExplanationPlan claim key or
evidenceLine.

`StoryInteractionLaw.md` owns the Stage 7-3 forbidden wording enforcement.
This README only summarizes the current scope: Stage 7-3 opens only forbidden
wording enforcement. Renderer output must not violate
`ExplanationPlan.forbiddenWording`; `win material` wording is allowed only
when `allowedClaim` is `CanWinPiece`, `winning position` remains forbidden,
engine-strength-limited plans fail strong wording output, and plans with no
allowedClaim or debugOnly true produce no output.

`StoryInteractionLaw.md` owns the Stage 7-4 no-standalone-text boundary. This
README only summarizes the current scope: Stage 7-4 opens only the
no-standalone-text boundary. Renderer phrases only Lead plans with an allowed
claim, and Support, Context, Blocked, capped no-claim, and engine-refuted
relation plans produce no text.

`StoryInteractionLaw.md` owns the Stage 7-5 RenderedLine shape. This README
only summarizes the current scope: Stage 7-5 opens only the RenderedLine
shape. `RenderedLine` owns no chess meaning, proof, or engine data, and
RenderedLine is only the expression result of ExplanationPlan. It must not
include CaptureResult, EngineCheck, BoardFacts, proofFailures, raw route
analysis, or source-row material.

`StoryInteractionLaw.md` owns the Stage 7-6 renderer baseline tests. This
README only summarizes the current scope: Stage 7-6 opens only renderer
baseline tests. Renderer output is no stronger than ExplanationPlan, and there
is no new renderer wording, no new input, no public route `200`, and no LLM
narration.

`StoryInteractionLaw.md` owns the Stage 7 closeout pass. This README only
summarizes the current scope: deterministic renderer is closed as a template
baseline. Stage 8 LLM Narration may receive deterministic text and
ExplanationPlan only, and Stage 8 must not read raw Verdict, Story,
EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV, proofFailures text,
or source rows directly.

`StoryInteractionLaw.md` owns the Stage 8 prompt smoke. This README only
summarizes the current scope: Stage 8 opens only 8A Mock narrator and 8B Codex
CLI prompt smoke test. LLM narration behavior smoke may rephrase deterministic
text from RenderedLine only in 8B, while 8A receives ExplanationPlan and
RenderedLine only. It must respect the forbidden wording checker, must not
strengthen deterministic text, and must not add a move, line, tactic, plan,
engine mention, or chess meaning absent from ExplanationPlan. Production API
validation remains closed.

`StoryInteractionLaw.md` owns Fork-8 Deterministic Renderer for Fork. This
README only summarizes the current scope: Fork deterministic renderer text may
phrase only a selected Fork ExplanationPlan with route, target,
secondaryTarget, bounded strength, `forks_two_targets`, and forbidden wording
present. It must not read raw Verdict, Story, MultiTargetProof, EngineCheck,
CaptureResult, BoardFacts, BoardMood, raw PV, proofFailures, or source rows,
and it does not open Fork LLM smoke itself, public route `200`, production API,
material claims, wins-queen claims, engine-says wording, best-move wording, or
sibling tactic families.

`StoryInteractionLaw.md` owns Fork-9 LLM Smoke for Fork. This README only
summarizes the current scope: Fork LLM smoke may receive selected Fork
ExplanationPlan and RenderedLine. 8B may receive renderedText, claimKey,
strength, forbidden wording list, and the instruction `Rephrase only. Do not
add chess facts.` only. It must not read raw Verdict, Story, MultiTargetProof,
EngineCheck, CaptureResult, BoardFacts, BoardMood, EngineEval, EngineLine,
engine eval, raw PV, proofFailures, or source rows. Production API,
public/user-facing LLM narration, public route `200`, pedagogy, engine PV
commentary, best-move explanation, material-win wording, wins-queen wording,
and sibling tactic families remain closed.

`StoryInteractionLaw.md` owns the Fork Slice Closeout Pass. This README only
summarizes the current scope: Fork closeout opens no sibling tactic,
`Scene.Defense`, Plan, Strategy, public route, production API, or
public/user-facing LLM narration. Fork does not open `Scene.Material` by
implication; Material-3 opens only the narrow named `Scene.Material` writer.
MultiTargetProof does not replace CaptureResult, StoryProof, EngineCheck, or
StoryTable.

`StoryInteractionLaw.md` owns Material-0 and Material-1. This README only
summarizes the current scope: Material-0 fixes `Scene.Material` as a scene
Story label, not a tactic and not proof. Material-1 reuses `CaptureResult` for
the first simple capture and immediate bounded recapture proof home decision.
`material_change` is the first Material speech claim key, but Material writer,
StoryTable role rules, ExplanationPlan mapping, renderer text, LLM smoke,
public route `200`, production API, winning, decisive, blunder, conversion,
best-move, forced, no-counterplay, engine-says, and full-evaluation claims
remain closed.
`StoryInteractionLaw.md` owns Material-2. This README only summarizes that
Material-2 extends `CaptureResult` with captured pieces and bounded exchange
sequence proof fields. It calculates material result as proof only and does
not create a public Story, sentence, Material writer, ExplanationPlan mapping,
renderer text, LLM input, winning-position, decisive-advantage, conversion,
blunder, best-move, or forced-line claim.
`StoryInteractionLaw.md` owns Material-3. This README only summarizes that
Material-3 opens `StoryWriter.SceneMaterial` as the named Material writer over
complete `CaptureResult` proof, complete StoryProof, same-board proof, legal
line, known material result, and no EngineCheck Refutes result. It opens no
ExplanationPlan mapping, renderer text, LLM input, public route `200`,
production API, winning, decisive, blunder, conversion, best-move, forced,
no-counterplay, engine-says, or full-evaluation claims.
`StoryInteractionLaw.md` owns Material-4. This README only summarizes that
Material-4 adds the Material negative corpus. Legal-line missing, same-board
missing, erased material result, unclear exchange result, king target, zero
material result, EngineCheck Refutes, incomplete StoryProof, incomplete
material proof, tactic-writer duplication, Hanging/Fork auto-duplication, and
high Proof score only produce no Lead or Blocked. It opens no
ExplanationPlan mapping, renderer text, LLM input, public route `200`,
production API, winning, decisive, blunder, conversion, best-move, forced,
no-counterplay, engine-says, or full-evaluation claims.
`StoryInteractionLaw.md` owns Material-5. This README only summarizes that
Material-5 reuses existing `EngineCheck` for `Scene.Material`. It may support,
cap, or refute an already existing Material Story when same-board proof, same
Story route, same legal line, and fresh or depth evidence are present. It
creates no Material Story, public engine truth, PV explanation, best-move
explanation, winning claim, or `MaterialEngineCheck` duplicate type.

`StoryInteractionLaw.md` owns Material-6. This README only summarizes that
Material-6 adds StoryTable integration for existing Hanging, Fork, and Material
rows. Refuted, incomplete, writerless, or material-proof-missing Material rows
are Blocked; Hanging, Fork, and Material can compete for Lead; Support and
Context remain non-sentence roles. StoryTable creates no Material Story, raw
engine eval does not rank Material, material proof text and renderer wording
remain non-public, and Material does not open conversion or winning.

`StoryInteractionLaw.md` owns Material-7. This README only summarizes that
Material-7 opens ExplanationPlan mapping for selected `Scene.Material`
Verdicts. Allowed Material claim keys are `material_balance_changes`,
`line_leaves_material_gain`, and `exchange_leaves_side_ahead`; the first
emitted key is `material_balance_changes`. It reads no material proof,
`CaptureResult`, `ExchangeResult`, `EngineCheck`, `BoardFacts`, raw PV,
proofFailures, or source row input, and it opens no Material renderer text,
LLM smoke, public route, production API, winning, decisive, conversion,
blunder, best-move, forced-win, or no-counterplay claim.

`StoryInteractionLaw.md` owns Material-8. This README only summarizes that
Material-8 opens deterministic renderer text for selected `Scene.Material`
ExplanationPlan. Renderer input remains `ExplanationPlan` only, the first
route template is `After {route}, White comes out ahead in material.`, and the
text must not exceed the Material ExplanationPlan or use winning, decisive,
blunder, forced, best-move, no-counterplay, engine-says, conversion, or
technically-winning wording.

`StoryInteractionLaw.md` owns Material-9. This README only summarizes that
Material-9 opens LLM smoke for selected Material ExplanationPlan and
RenderedLine. 8B receives only renderedText, claimKey, strength, forbidden
wording, and `Rephrase only. Do not add chess facts.` Material LLM smoke must
reject new moves, new lines, new tactics, new plans, engine mentions, winning,
decisive, forced, blunder, best-move, conversion, and stronger claims.

`StoryInteractionLaw.md` owns Material Slice Closeout. This README only
summarizes that the closeout opened no sibling family or stronger result path.
`CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`, `Scene.Material`,
and `material_change` keep distinct homes, and `Scene.Defense` remains the
next named slice. Material reuses the existing proof home, Story writer,
EngineCheck, StoryTable, ExplanationPlan, Renderer, and LLM smoke skeleton
before adding any new structure.

- Public route no-go: `/api/commentary/render` and
  `/internal/commentary/render-local-probe` are registered only as fail-closed
  tombstones until an explicit public-surface contract exists. No `200`,
  rendered payload, environment switch, or frontend mock can open them.
- `BoardMood` no-go: no expansion beyond `48` bits, `256` scalars, and `3,328`
  total values. Split/cut re-entry requires a named law and same-board producer
  proof; closed, cut, and split slots otherwise stay `0`/silent.
- Board Facts no-go: open file, pin, weak square, loose piece, pawn lever,
  attacked piece, king-ring attack, and legal move facts are observations only.
  They are not public claims and must not bypass `Story`.
- Story Proof no-go: `StoryProof` records the minimum proof bundle and missing
  evidence, but only the named `Tactic.Hanging` writer, narrow `Tactic.Fork`
  writer, and narrow `Scene.Material` writer may open positive Story families
  in this checkpoint. Numeric `Proof` scores may rank blocked/context
  `Verdict` rows only. They cannot set `leadAllowed=true` or produce
  `Role.Lead` without the named writer, complete StoryProof, same-board proof,
  and family proof: `CaptureResult` for Hanging or Material, or
  `MultiTargetProof` for Fork.
- Proof no-go: missing side, target, anchor, route, rival, required legal line,
  or same-root proof sidecar is a hard public-output block, not weak scoring,
  deferred work, or renderer repair.
- Engine Check no-go: `EngineCheck`, `EngineLine`, and `EngineEval` are
  internal evidence only. Same-board proof, checked move, engine line, reply
  line, eval before, eval after, depth or freshness, status, and missing
  evidence are diagnostics, not public claim ownership.
- Stage 4-2 guard no-go: engine evidence must bind to the same board, the same
  Story route, and the same legal line. Different-FEN engine lines,
  route-mismatched engine lines, stale engine data, depth-missing engine data,
  eval-only input without a Story, and PV-only input without a Story remain
  diagnostic only.
- Stage 4-3 status no-go: EngineCheck first attached only to
  `Tactic.Hanging`; Fork-5 reuses the same sidecar for existing narrow
  `Tactic.Fork` Stories. Status is exactly `Unknown`, `Supports`, `Caps`, or
  `Refutes`. `Supports` cannot become winning, best-move, decisive, PV
  explanation, or public truth; `Caps` forbids strong expression; `Refutes`
  blocks the Story.
- Stage 7-0 Deterministic Renderer no-go: the current scope may fix
  `ExplanationPlan` only input, deterministic template, `Tactic.Hanging`
  bounded claim phrasing, and forbidden wording checks only. It does not open
  `Tactic.Fork`, Material renderer wording, `Scene.Defense`, Plan, Strategy,
  pedagogical advice, LLM, public route `200`, engine PV commentary,
  best-move explanation, engine explanation, or raw proof input.
- Stage 7-1 Renderer Input Guard no-go: Renderer may accept
  `ExplanationPlan` only. It must not expose `fromVerdict`, `fromStory`,
  `fromBoardFacts`, `fromEngineCheck`, or any raw proof-material renderer
  entrypoint.
- Stage 7-2 Minimal Tactic.Hanging Template no-go: Renderer may render only
  the `can_win_piece` claim key for a Lead, bounded, non-debug
  `Tactic.Hanging` ExplanationPlan with route, target, evidenceLine, and
  forbidden wording. Other claim keys, stronger wording, raw proof input, LLM,
  and public route `200` remain closed.
- Stage 7-3 Forbidden Wording Enforcement no-go: Renderer must reject
  deterministic text that violates `ExplanationPlan.forbiddenWording`. `win
  material` wording is allowed only when `allowedClaim` is `CanWinPiece`;
  winning-position, best-move, engine-says, strategic, conversion, mate-net,
  and other forbidden wording remains closed.
- Stage 7-4 No Text for Support / Context / Blocked no-go: Renderer may phrase
  only Lead plans with an allowed claim. Support, Context, Blocked, debug-only,
  capped no-claim, and engine-refuted relation plans produce no text.
- Stage 7-5 Rendered Line Shape no-go: `RenderedLine` may carry text, claim
  key, strength, and forbidden-check result only. It must not carry
  CaptureResult, EngineCheck, BoardFacts, proofFailures, raw route analysis,
  source rows, proof ownership, engine data ownership, or new chess meaning.
- Stage 7-6 Renderer Baseline Tests no-go: baseline tests may prove renderer
  output is no stronger than ExplanationPlan. They must not add renderer
  wording, new inputs, relation narration, public route `200`, or LLM
  narration.
- Stage 7 Closeout Pass no-go: closeout may audit only that deterministic
  renderer is the sole Stage 7 opening. It must not open LLM narration, public
  route `200`, pedagogy, new Story families, new renderer inputs, or a new
  markdown authority file.
- Stage 8 Prompt Smoke no-go: only 8A Mock narrator and 8B Codex CLI prompt
  smoke test are open. 8A receives ExplanationPlan and RenderedLine only. 8B
  receives renderedText, claimKey, strength, forbidden wording list, and the
  instruction `Rephrase only. Do not add chess facts.` only. Production API
  validation, public route `200`, pedagogy, natural-language verifier, raw
  proof input, and new chess meaning remain closed.
- Renderer boundary no-go: deterministic templates may phrase
  `ExplanationPlan` only. LLM renderers remain closed, public route `200`
  remains closed, and no renderer may create, repair, or upgrade chess
  meaning.
- Old-doc no-go: `CommentaryCoreSSOT.md`, `SemanticModelArchitecture.md`,
  `LegacyArchiveIndex.md`, and `CommentaryFrontendBridgeContract.md` must not
  return as root authority.
- Forbidden-name no-go: new core model, type, module, or docs-authority names
  must not use `Semantic`, non-pawn `Candidate`, `Certification`, `Object`,
  `Delta`, `Selector`, `Pipeline`, `Gate`, `ScoreVector`, or version suffixes.
- Stage order no-go: implementation opens only
  `observation` -> `proof sidecar` -> `Story` -> `Verdict` ->
  `Explanation IR` -> Renderer -> LLM narration smoke. Downstream product
  stages stay closed until the earlier authority stage is proven.
- LLM no-go: Engine truth, exact-board validation, legal replay, proof
  sidecars, and `StoryTable` decide what can be said. LLM narration behavior
  smoke may rephrase only deterministic text bounded by ExplanationPlan and
  must remain closed and must not judge chess outside that prompt smoke scope.

Current implementation blockers are documented, not excused: Stage 2 enforces
the full public-output tuple before lead candidacy, and Stage 3 opens only the
named `Tactic.Hanging` writer over positive `CaptureResult`. Stage 4-1 records
engine evidence shape only; Stage 5 role ordering consumes only existing
`Tactic.Hanging` Story rows; Stage 6 closeout confirms Explanation Plan only,
Stage 7-0 opens only the Deterministic Renderer charter, Stage 7-1 opens only
the Renderer input guard over `ExplanationPlan`, and Stage 7-2 opens only the
minimal `Tactic.Hanging` template. Stage 7-3 opens only forbidden wording
enforcement. Stage 7-4 opens only the no-standalone-text boundary. Stage 7-5
opens only the RenderedLine shape. Stage 7-6 opens only renderer baseline
tests. Stage 7 closeout confirms deterministic renderer is closed as a
template baseline. Stage 8 opens only 8A Mock narrator and 8B Codex CLI prompt
smoke test.
`Scene.Opening` is context-only and must not lead over a board-backed `Story`.
Old failing tests proved lower/scaffold/renderer non-upgrade; they did not
prove default runtime FEN to rendered commentary.

Authority summary:

- `ChessCommentarySSOT.md` defines the single public chess meaning chain:
  `BoardMood` -> `Story` -> `StoryTable` -> `Verdict`.
- `BoardFacts.md` is the Stage 1 charter for board observations that remain
  below public claims.
- `BoardMoodCutLaw.md` closes BoardMood slots that have no live chess fact.
- `BoardMoodSplitLaw.md` closes broad BoardMood slots unless they re-enter as
  smaller exact chess facts.
- `ChessModelArchitecture.md` describes the HCE-style deterministic chess
  model architecture.
- `ChessModelContract.md` is the shape, naming, family, and deletion contract.
- `ChessResetRationale.md` records why the reset exists and which old failure
  modes the new model must not repeat.
- `LegacyPruneManifest.md` records what was pruned and what is intentionally
  preserved during the reset.
- `StoryInteractionLaw.md` defines upper-layer Story family classification and
  nonlinear support, blocker, cap, and override rules.
- `StoryResurrectionLaw.md` defines when a cut BoardMood idea may be spoken by
  a proof-backed Story instead.
