# Story Interaction Law

Prepared on 2026-05-05 for the public layer above BoardMood.

## Law

`Story` is the first public chess unit, but a Story family name is not enough.
Every public Story must bind side, target, anchor, route, rival, required legal
line, and same-root proof sidecar. A family row below adds the chess-specific
facts that must support that binding. Missing side, target, anchor, route,
rival, required legal line, or same-root proof sidecar is a hard public-output
block. If a row does not need a move sequence, its required legal line is the
same-root legal replay proving the named position fact can be spoken.

Nonlinear interaction is explicit. A support fact can help a Story only when no
hard blocker applies. A blocker can cap or silence a Story even when several
support facts are present. A stronger tactical or blunder Story can override a
strategic Story. Engine context can confirm, cap, or contradict a Story, but it
does not speak without the Story identity tuple.

## Scene Law

| scene | class | required support | blockers and caps | public wording |
|---|---|---|---|---|
| Scene.Tactic | live forcing | concrete tactic family, legal line, target, attacker, defender or reply | missing line, missing tactic, illegal route, engine contradiction | tactic wording only inside the legal line. |
| Scene.Blunder | live forcing | engine swing or forced material/mate line, losing move identity, rival refutation | no legal refutation, shallow engine only, no previous-position comparison | blunder wording only with swing or forced line. |
| Scene.Material | live static | material count, target piece or asset, capture or trade identity if dynamic | compensation Story with proof, tactical override, unclear recapture | material wording only for named asset. |
| Scene.King | split public | king square, king-ring attacks, escape issue, route or line proof | legal defense exists, no route, no escape proof, engine contradiction | king safety wording, no mate wording without mate proof. |
| Scene.Defense | live response | rival threat, defensive move or resource, protected target | rival threat unproven, defense illegal, stronger tactic override | defense wording only against named threat. |
| Scene.Opening | context-only | opening label, current board match, known line source | board mismatch, tactical refutation, source-only claim, board-backed Story at public floor | opening context only, no truth override or lead over board-backed Story. |
| Scene.Pawns | split public | exact pawn feature, square/file, blocker, lever, guard or passer identity | tactic override, no pawn identity, engine contradiction for plan wording | pawn structure wording for named feature. |
| Scene.Plan | split public | one Plan row, affordance fact, route, anchor, proof | opposing tactic or blunder at public floor, missing route, engine contradiction | plan wording only as a plan, not as forced truth. |
| Scene.Pieces | split public | exact piece, mobility or route fact, target or anchor | no named piece, route illegal, tactical override | piece wording for named piece only. |
| Scene.Space | live static | controlled squares, safe mobility, space region, side identity | no same-board control, immediate tactic override | space wording only as board condition. |
| Scene.Initiative | conditional | repeated forcing moves, restricted rival replies, line proof | no forcing route, rival has equal resource, engine contradiction | initiative label only with forcing proof. |
| Scene.Convert | conditional | named advantage, legal route to named gain, rival defense | no route, no gain identity, counterplay risk high, engine contradiction | conversion wording only within proven gain. |
| Scene.Endgame | conditional | material shape, king/pawn route, promotion or fortress identity | middlegame tactics dominate, no route, tablebase or engine contradiction | endgame wording only for named shape. |
| Scene.Counterplay | conditional | resource against rival plan, target, route, timing | rival plan unproven, resource illegal, engine contradiction | counterplay wording only as named resource. |
| Scene.Source | context | source row, opening/game reference, same-board fit | board proof story at public floor, source mismatch | source context only; never leads over board proof. |
| Scene.Quiet | fallback | no other Story reaches public floor | any non-Quiet Story at public floor | quiet wording only as absence of stronger story. |

## Plan Law

Every Plan row is split public. A Plan can support `Scene.Plan`, but the plan
name cannot speak unless the row support is present and no hard blocker applies.

| plan | required affordance | required proof | blockers and caps |
|---|---|---|---|
| Plan.Minority | `queenside_minority_lever` | route, anchor, pawnSupport | no legal lever, tactical override, engine contradiction |
| Plan.Majority | `wing_pawn_majority` | target, anchor, pawnSupport | no passer route, rival blockade, tactical override |
| Plan.CenterBreak | `center_pawn_lever` | route, line or source, pawnSupport | lever illegal, center tactic loses material |
| Plan.FlankBreak | `flank_pawn_lever` | route, anchor, pawnSupport | king exposure without proof, lever illegal |
| Plan.Storm | `king_flank_pawn_step` | route, kingHeat, line if forcing | own king weak, no target, engine contradiction |
| Plan.Expansion | `space_gain_pawn_step` | route, clarity, pawnSupport | overextension tactic, no safe square |
| Plan.Cramp | `piece_mobility_clamp` | target, anchor, persistence | rival break exists, no named clamped piece |
| Plan.Outpost | `pawn_safe_outpost_square` | piece route, anchor, pieceSupport | enemy pawn can chase, no occupying or reachable piece |
| Plan.BadPiece | `boxed_piece` | target piece, route, clarity | no improvement route, tactical override |
| Plan.Reroute | StoryResurrectionLaw S201 | piece route, destination, proof | no legal path, no destination reason |
| Plan.Bishops | `bishop_pair_or_color` | target color, anchor, pieceSupport | closed diagonals without route, tactic override |
| Plan.Blockade | `passer_blockade_square` | blocker route, anchor, line if dynamic | blocker illegal, pawn can advance with gain |
| Plan.OpenFile | `rook_open_file_entry` | rook route, entry square, pieceSupport | entry square controlled with tactic, no rook |
| Plan.Seventh | `rook_seventh_targets` | rook route, targets, line if forcing | no seventh target, rook trapped |
| Plan.ColorBind | `fixed_color_weak_squares` | target color, anchor, persistence | opposite-color counterplay, no fixed weakness |
| Plan.WeakSquare | `pawn_safe_weak_square` | target square, route, pieceSupport | no reachable piece, enemy pawn can contest |
| Plan.Isolani | `isolated_pawn_front_square` | target pawn, blockade or lever route | dynamic break solves it, no attack route |
| Plan.BackwardPawn | `backward_pawn_front_square` | target pawn, anchor, route | pawn can advance safely, no pressure route |
| Plan.HangingPawns | `hanging_pawn_pair` | target pair, lever or blockade route | pair can advance with tempo, tactic override |
| Plan.ChainBase | `pawn_chain_base` | base pawn, attack route, anchor | no attack route, counterplay higher |
| Plan.PasserMake | `candidate_passer_lever` | pawn route, blocker identity, line if dynamic | lever illegal, rival blockade proof |
| Plan.PasserBlock | `passer_block_move` | blocker route, passed pawn target | blocker illegal, pawn promotes by force |
| Plan.Race | StoryResurrectionLaw S214 | both routes, tempo count, proof | tempo not proven, engine contradiction |
| Plan.Trade | StoryResurrectionLaw S215 | pieces, recapture chain, purpose | recapture unclear, tactic loses material |
| Plan.Simplify | StoryResurrectionLaw S216 | exchange path, preserved edge, proof | edge unproven, rival tactic after exchange |
| Plan.KeepPieces | StoryResurrectionLaw S217 | refused trade, preserved resource, proof | resource unproven, trade is forced |
| Plan.Overload | `dual_target_defender` | two targets, defender, legal test | no legal test, defender not tied to targets |
| Plan.Prophy | StoryResurrectionLaw S219 | rival threat, preventing move, proof | threat unproven, prevention illegal |
| Plan.Counterplay | StoryResurrectionLaw S220 | resource, rival plan, timing, proof | rival plan absent, resource too slow |
| Plan.Initiative | StoryResurrectionLaw S221 | forcing route, restricted replies, proof | quiet move breaks sequence, engine contradiction |
| Plan.KingConvert | StoryResurrectionLaw S222 | king route, target gain, proof | mate/material route absent, defense works |
| Plan.Convert | StoryResurrectionLaw S223 | advantage, route to gain, proof | no gain identity, high counterplay risk |

## Tactic Law

Every Tactic row is live forcing only when it has a legal line. Motif facts from
BoardMood can support a tactic, but they cannot prove it.

| tactic | required support | required line | blockers and caps |
|---|---|---|---|
| Tactic.Loose | loose target and attacker | legal capture or pressure line | target defended or recapture equalizes |
| Tactic.Hanging | attacked piece guard map | legal capture and recapture map | defender recaptures favorably |
| Tactic.AbsPin | pinned king line | legal line piece, pinned piece, king behind | line blocked or pinned piece can legally move |
| Tactic.RelPin | pinned valuable target line | legal line piece, screen, target | screen can move with gain or target not valuable |
| Tactic.Skewer | line piece, valuable front target, rear target | legal checking or attack line | front target can escape with tempo |
| Tactic.Xray | `white_xray_line` or `black_xray_line` | legal uncovering or pressure line | screen not movable, target defended |
| Tactic.Fork | attacker, two targets | legal move to fork square | fork square unsafe or targets can respond |
| Tactic.Discover | screened line and moving piece | legal discovered move | moving piece illegal or line target absent |
| Tactic.RemoveGuard | guard-capture move | legal capture of guard and follow-up target | guard not sole defender |
| Tactic.Overload | dual-target guard | legal test of one target | defender can cover both or recapture |
| Tactic.BackRank | back-rank line and flight squares | legal rook or queen check line | king has escape or interposition |
| Tactic.MateNet | king ring, checks, escapes | mate proof or decisive checking line | any legal defense not answered |
| Tactic.SafeCheck | checking move and safe attacker | legal check with reply map | checking piece lost without gain |
| Tactic.PawnFork | pawn move attacks two targets | legal pawn move | target can move with stronger threat |
| Tactic.PawnPush | promotion or tempo pawn step | legal pawn move and reply map | blocker or capture stops it |
| Tactic.Trap | piece mobility clamp and target | legal chase or no-escape line | escape square or counter-threat exists |
| Tactic.QueenHit | queen target and attacker | legal attack or tempo line | queen has stronger reply |
| Tactic.KingOpen | king line or shelter break | legal capture/push/check line | king remains defended or line closes |
| Tactic.Promote | promotion step | legal promotion route | stop square or capture refutes |
| Tactic.InBetween | forcing intermezzo target | legal in-between move | rival can ignore without loss |
| Tactic.Clear | clearance move and line piece | legal clearance route | cleared line has no target |
| Tactic.Decoy | target lure square | legal forcing move to lure | target can decline safely |
| Tactic.Deflect | defender and target | legal deflection move | defender not needed or recaptures |
| Tactic.Tempo | forcing move with gained turn | legal move and rival reply restriction | rival has equal or stronger forcing move |

## Proof And Interaction Law

| proof field | live class | raises | caps or blocks |
|---|---|---|---|
| boardProof | board truth | every non-source Story | zero blocks board-backed speech. |
| lineProof | line truth | tactics, conversion, initiative, trade, race | zero blocks tactic and line-backed stories. |
| ownerProof | side truth | lead eligibility | high ownerProof with no side blocks lead. |
| anchorProof | anchor truth | structural and plan Stories | high anchorProof with no anchor blocks lead. |
| routeProof | route truth | plans, tactics, conversion | high routeProof with no route blocks lead. |
| persistence | long pressure | planHeat | immediate tactic override can cap it. |
| immediacy | near-term force | tacticHeat | no legal line caps it to context. |
| forcing | move pressure | tacticHeat | missing reply map blocks forcing wording. |
| conversionPrize | gain size | tacticHeat and planHeat | no gain identity blocks conversion wording. |
| counterplayRisk | rival resource | caps plan and conversion | above 70 blocks plan lead. |
| kingHeat | king attack support | tacticHeat | no legal line blocks mate or attack claim. |
| pieceSupport | piece support | planHeat | no named piece caps piece wording. |
| pawnSupport | pawn support | planHeat | no named pawn caps pawn wording. |
| sourceFit | source context | source/opening context | source cannot replace board proof. |
| novelty | source context | novelty wording | no source row blocks novelty wording. |
| clarity | explanation clarity | planHeat | missing identity caps clarity to context. |

## Nonlinear Rules

| rule | effect |
|---|---|
| Hard proof blocker | Missing side, target, anchor, route, rival, required legal line, or same-root proof sidecar is a hard public-output block. |
| Tactical override | An opposing Tactic or Blunder at public floor blocks Plan lead. |
| Same-side tactic priority | A same-side Tactic with tacticHeat at least 70 and lineProof at least 65 outranks Plan. |
| Source cap | Source and Opening context cannot lead over a board-backed Story at public floor. |
| Engine cap | Engine context can cap or confirm wording only after Story identity and legal line are bound. |
| Board-only cap | BoardMood facts without Story proof allow observed/possible wording only, not advice or verdict. |
| Blunder override | A proven blunder or losing tactic suppresses strategic praise about the same side. |
| Counterplay cap | Counterplay risk above 70 blocks conversion and plan lead unless the Story proves the rival resource is answered. |
| Quiet fallback | Quiet can lead only when every non-Quiet Story is below public floor. |
| Render cap | Render may only verbalize selected Verdicts and cannot repair missing identity or proof. |

## Fixture Duties

The interaction law must be tested with positions or synthetic Stories that
cover these failure modes:

- Opening context with a tactical refutation: tactic or blunder overrides
  source/opening speech.
- Strategic plan with no route: plan support exists but public plan speech is
  blocked.
- Mate-net shape with legal escape: king pressure is capped below mate wording.
- Material gain with compensation line: material Story is capped by
  counterplay/conversion proof.
- Source row matching the opening but board proof contradicting it: source stays
  context.
- Quiet position with no public floor Story: Quiet may lead.
