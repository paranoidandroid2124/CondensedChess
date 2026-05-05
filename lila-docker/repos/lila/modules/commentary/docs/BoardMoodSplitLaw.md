# BoardMood Split Law

Prepared on 2026-05-05 for BoardMood scalar slots classified as `쪼갬`.

## Decision

The 65 split slots listed here are inactive in live BoardMood. Each old slot
writes `0/silent`. The old slot has no public meaning, no scalar speech, and no
plan speech.

Re-entry is allowed only through the exact smaller BoardMood fact named in the
tables below. Each fact must be derived from the current board by direct chess
observation: side, square, file, rank, piece, pawn, legal move, attack, defender,
blocker, pin to king, or promotion path. Engine evaluation, best-move judgment,
and claims that a tactic works are outside BoardMood.

BoardMood `plan_*` slots S192..S223 are not Story plans. A Story plan has side,
target, anchor, route, rival, and proof scores. BoardMood may hold only the
small affordance fact named here; it may not produce plan speech.

## Material

| Old slot | Exact smaller BoardMood fact | Current-board derivation rule | `0/silent` meaning | Public speech ban |
|---|---|---|---|---|
| S031 `material_imbalance` | `piece_role_tally` | Count non-king pieces and pawns by side and role on the current board. | No named material tally is emitted. | No material verdict, compensation claim, or imbalance sentence. |

## King

| Old slot | Exact smaller BoardMood fact | Current-board derivation rule | `0/silent` meaning | Public speech ban |
|---|---|---|---|---|
| S076 `white_overloaded_defender_count` | `white_king_ring_dual_guard` | Find White king-ring squares attacked by Black; name each White defender that guards two or more of those squares. | No named White king-ring dual guard. | No claim that a White defender is overloaded. |
| S077 `white_contact_check_threats` | `black_contact_check_on_white_king` | List legal Black checking moves whose checking piece lands adjacent to the White king. | No legal adjacent Black check is emitted. | No claim that Black has a sound contact check. |
| S079 `white_king_heat` | `white_king_ring_attack_map` | For each White king-ring square, list Black attackers and White defenders from the current board. | No White king-ring attack map is emitted. | No hot-king, unsafe-king, or attack-strength sentence. |
| S092 `black_overloaded_defender_count` | `black_king_ring_dual_guard` | Find Black king-ring squares attacked by White; name each Black defender that guards two or more of those squares. | No named Black king-ring dual guard. | No claim that a Black defender is overloaded. |
| S093 `black_contact_check_threats` | `white_contact_check_on_black_king` | List legal White checking moves whose checking piece lands adjacent to the Black king. | No legal adjacent White check is emitted. | No claim that White has a sound contact check. |
| S095 `black_king_heat` | `black_king_ring_attack_map` | For each Black king-ring square, list White attackers and Black defenders from the current board. | No Black king-ring attack map is emitted. | No hot-king, unsafe-king, or attack-strength sentence. |

## Pawn

| Old slot | Exact smaller BoardMood fact | Current-board derivation rule | `0/silent` meaning | Public speech ban |
|---|---|---|---|---|
| S102 `white_protected_passer_count` | `white_passed_pawn_pawn_guard` | Name each White passed pawn square and each White pawn that attacks that passed pawn square. | No White passed pawn with pawn guard is emitted. | No claim that White has a protected passer advantage. |
| S106 `white_break_chance_count` | `white_legal_pawn_lever` | Name each legal White pawn push or capture that contacts an enemy pawn on the same or adjacent file. | No legal White pawn lever is emitted. | No claim that White should break or has a good break. |
| S107 `white_blockaded_pawn_count` | `white_pawn_front_blocker` | Name each White pawn and the occupied square directly in front of it. | No White pawn front blocker is emitted. | No blockade-strength or pawn-stuck sentence. |
| S109 `white_pawn_support` | `white_pawn_guard_map` | For each White pawn, list friendly pawns and pieces attacking its square or next square. | No White pawn guard map is emitted. | No pawn-support score or structure praise. |
| S110 `white_pawn_risk` | `white_pawn_attack_map` | For each White pawn, list enemy pawn and piece attacks on its square and next square. | No White pawn attack map is emitted. | No weak-pawn, lost-pawn, or pawn-risk sentence. |
| S118 `black_protected_passer_count` | `black_passed_pawn_pawn_guard` | Name each Black passed pawn square and each Black pawn that attacks that passed pawn square. | No Black passed pawn with pawn guard is emitted. | No claim that Black has a protected passer advantage. |
| S122 `black_break_chance_count` | `black_legal_pawn_lever` | Name each legal Black pawn push or capture that contacts an enemy pawn on the same or adjacent file. | No legal Black pawn lever is emitted. | No claim that Black should break or has a good break. |
| S123 `black_blockaded_pawn_count` | `black_pawn_front_blocker` | Name each Black pawn and the occupied square directly in front of it. | No Black pawn front blocker is emitted. | No blockade-strength or pawn-stuck sentence. |
| S125 `black_pawn_support` | `black_pawn_guard_map` | For each Black pawn, list friendly pawns and pieces attacking its square or next square. | No Black pawn guard map is emitted. | No pawn-support score or structure praise. |
| S126 `black_pawn_risk` | `black_pawn_attack_map` | For each Black pawn, list enemy pawn and piece attacks on its square and next square. | No Black pawn attack map is emitted. | No weak-pawn, lost-pawn, or pawn-risk sentence. |

## Piece

| Old slot | Exact smaller BoardMood fact | Current-board derivation rule | `0/silent` meaning | Public speech ban |
|---|---|---|---|---|
| S128 `white_minor_activity` | `white_minor_legal_moves` | For each White knight and bishop, count legal destinations and name occupied target squares it attacks. | No White minor legal-move fact is emitted. | No minor-piece activity score or praise. |
| S129 `white_rook_activity` | `white_rook_file_reach` | For each White rook, name its file, pawn occupancy on that file, and legal vertical destinations. | No White rook file-reach fact is emitted. | No rook-activity score or file-control sentence. |
| S130 `white_queen_activity` | `white_queen_legal_moves` | For each White queen, count legal destinations and name occupied target squares it attacks. | No White queen legal-move fact is emitted. | No queen-activity score or attacking claim. |
| S133 `white_bad_bishop_count` | `white_bishop_pawn_color_map` | For each White bishop, record bishop square color, friendly pawns on that color, and blocked diagonal squares. | No White bishop color-map fact is emitted. | No bad-bishop label or strategic verdict. |
| S137 `white_hanging_piece_count` | `white_attacked_piece_guard_map` | For each attacked White non-king piece, list legal enemy captures and friendly defenders of that square. | No White attacked-piece guard map is emitted. | No hanging-piece, winning-material, or capture verdict. |
| S143 `white_piece_coordination` | `white_shared_target_attack` | Name each square attacked by two or more White non-pawn pieces. | No White shared-target attack is emitted. | No coordination score or team-piece praise. |
| S144 `black_minor_activity` | `black_minor_legal_moves` | For each Black knight and bishop, count legal destinations and name occupied target squares it attacks. | No Black minor legal-move fact is emitted. | No minor-piece activity score or praise. |
| S145 `black_rook_activity` | `black_rook_file_reach` | For each Black rook, name its file, pawn occupancy on that file, and legal vertical destinations. | No Black rook file-reach fact is emitted. | No rook-activity score or file-control sentence. |
| S146 `black_queen_activity` | `black_queen_legal_moves` | For each Black queen, count legal destinations and name occupied target squares it attacks. | No Black queen legal-move fact is emitted. | No queen-activity score or attacking claim. |
| S149 `black_bad_bishop_count` | `black_bishop_pawn_color_map` | For each Black bishop, record bishop square color, friendly pawns on that color, and blocked diagonal squares. | No Black bishop color-map fact is emitted. | No bad-bishop label or strategic verdict. |
| S153 `black_hanging_piece_count` | `black_attacked_piece_guard_map` | For each attacked Black non-king piece, list legal enemy captures and friendly defenders of that square. | No Black attacked-piece guard map is emitted. | No hanging-piece, winning-material, or capture verdict. |
| S159 `black_piece_coordination` | `black_shared_target_attack` | Name each square attacked by two or more Black non-pawn pieces. | No Black shared-target attack is emitted. | No coordination score or team-piece praise. |

## Tactic

| Old slot | Exact smaller BoardMood fact | Current-board derivation rule | `0/silent` meaning | Public speech ban |
|---|---|---|---|---|
| S163 `white_see_best_gain` | `white_capture_target_map` | For each legal White capture, name the from square, target square, captured role, capturing role, and enemy recaptures on that target. | No White capture target map is emitted. | No exchange win, best capture, or tactic verdict. |
| S168 `white_xray_discovery_count` | `white_xray_line` | Name each White line piece, screened piece, and enemy target aligned on one clear file, rank, or diagonal after the screen. | No White x-ray line is emitted. | No discovered-attack or tactic-working sentence. |
| S169 `white_overload_deflect_count` | `white_dual_target_guard` | Name each enemy defender that guards two White-attacked target squares. | No White dual-target guard is emitted. | No overload, deflection, or winning-target claim. |
| S170 `white_remove_guard_count` | `white_guard_capture_move` | Name each legal White capture of an enemy piece that currently defends another named target square. | No White guard-capture move is emitted. | No remove-the-guard tactic claim. |
| S171 `white_back_rank_pressure` | `white_back_rank_line` | Name Black king back-rank square, legal White rook or queen checking line to that rank, and Black flight squares. | No White back-rank line is emitted. | No back-rank mate, pressure, or winning attack claim. |
| S172 `white_promotion_threat` | `white_promotion_step` | Name each White pawn on the seventh rank with its legal promotion move or capture square. | No White promotion step is emitted. | No promotion threat or race verdict. |
| S173 `white_defender_shortage` | `white_target_attack_guard_count` | For each Black target square attacked by White, count White attackers and Black defenders of that square. | No White target attack-guard count is emitted. | No defender-shortage or tactic success claim. |
| S179 `black_see_best_gain` | `black_capture_target_map` | For each legal Black capture, name the from square, target square, captured role, capturing role, and enemy recaptures on that target. | No Black capture target map is emitted. | No exchange win, best capture, or tactic verdict. |
| S184 `black_xray_discovery_count` | `black_xray_line` | Name each Black line piece, screened piece, and enemy target aligned on one clear file, rank, or diagonal after the screen. | No Black x-ray line is emitted. | No discovered-attack or tactic-working sentence. |
| S185 `black_overload_deflect_count` | `black_dual_target_guard` | Name each enemy defender that guards two Black-attacked target squares. | No Black dual-target guard is emitted. | No overload, deflection, or winning-target claim. |
| S186 `black_remove_guard_count` | `black_guard_capture_move` | Name each legal Black capture of an enemy piece that currently defends another named target square. | No Black guard-capture move is emitted. | No remove-the-guard tactic claim. |
| S187 `black_back_rank_pressure` | `black_back_rank_line` | Name White king back-rank square, legal Black rook or queen checking line to that rank, and White flight squares. | No Black back-rank line is emitted. | No back-rank mate, pressure, or winning attack claim. |
| S188 `black_promotion_threat` | `black_promotion_step` | Name each Black pawn on the second rank with its legal promotion move or capture square. | No Black promotion step is emitted. | No promotion threat or race verdict. |
| S189 `black_defender_shortage` | `black_target_attack_guard_count` | For each White target square attacked by Black, count Black attackers and White defenders of that square. | No Black target attack-guard count is emitted. | No defender-shortage or tactic success claim. |

## Plan Affordance

| Old slot | Exact smaller BoardMood fact | Current-board derivation rule | `0/silent` meaning | Public speech ban |
|---|---|---|---|---|
| S192 `plan_minority` | `queenside_minority_lever` | Count a-, b-, and c-file pawns by side and name a legal pawn lever by the side with fewer queenside pawns. | No queenside minority lever is emitted. | No minority-attack plan speech. |
| S193 `plan_majority` | `wing_pawn_majority` | Count pawns by side on each wing and name the wing where one side has more pawns. | No wing pawn majority is emitted. | No majority-plan or passer-plan speech. |
| S194 `plan_center_break` | `center_pawn_lever` | Name each legal d- or e-pawn push or capture that contacts an enemy center pawn. | No center pawn lever is emitted. | No center-break plan speech. |
| S195 `plan_flank_break` | `flank_pawn_lever` | Name each legal a-, b-, c-, f-, g-, or h-pawn push or capture that contacts an enemy flank pawn. | No flank pawn lever is emitted. | No flank-break plan speech. |
| S196 `plan_storm` | `king_flank_pawn_step` | Identify the enemy king flank and name legal pawn pushes by the other side on files adjacent to that king. | No king-flank pawn step is emitted. | No pawn-storm plan speech. |
| S197 `plan_expansion` | `space_gain_pawn_step` | Name each legal pawn push that occupies an empty square on the enemy half of the board. | No space-gain pawn step is emitted. | No expansion-plan or space-advantage speech. |
| S198 `plan_cramp` | `piece_mobility_clamp` | Name each enemy non-king piece with two or fewer legal destinations and the opposing attacks on those destinations. | No piece mobility clamp is emitted. | No cramp-plan or domination speech. |
| S199 `plan_outpost` | `pawn_safe_outpost_square` | Name each square in the enemy half occupied or legally reachable by a knight or bishop, defended by a pawn, and not attackable by enemy pawns. | No pawn-safe outpost square is emitted. | No outpost-plan or piece-route speech. |
| S200 `plan_bad_piece` | `boxed_piece` | Name each non-king piece with two or fewer legal destinations and occupied blockers on its normal movement lines. | No boxed piece is emitted. | No improve-piece plan or bad-piece verdict. |
| S202 `plan_bishops` | `bishop_pair_or_color` | Record bishop count by side and whether opposing bishops sit on same-colored or opposite-colored squares. | No bishop pair or color fact is emitted. | No bishop-plan or color-complex speech. |
| S203 `plan_blockade` | `passer_blockade_square` | Name each passed pawn and the square directly in front of it, occupied or legally reachable by an enemy piece. | No passer blockade square is emitted. | No blockade-plan speech. |
| S204 `plan_open_file` | `rook_open_file_entry` | Name each pawnless file, rook on that file, and legal rook entry square on that file. | No rook open-file entry is emitted. | No open-file plan speech. |
| S205 `plan_seventh` | `rook_seventh_targets` | Name each rook on the opponent seventh rank and enemy pawns or king on that rank. | No rook seventh-rank target is emitted. | No seventh-rank plan speech. |
| S206 `plan_color_bind` | `fixed_color_weak_squares` | Name weak squares of one color that cannot be attacked by enemy pawns and are near fixed pawns on that color. | No fixed-color weak square set is emitted. | No color-bind plan speech. |
| S207 `plan_weak_square` | `pawn_safe_weak_square` | Name each square not attackable by enemy pawns, defended by the other side, and occupied or legally reachable by a non-pawn piece. | No pawn-safe weak square is emitted. | No weak-square plan speech. |
| S208 `plan_isolani` | `isolated_pawn_front_square` | Name each pawn with no friendly pawn on adjacent files and the square directly in front of it. | No isolated-pawn front square is emitted. | No isolani plan or structure verdict. |
| S209 `plan_backward_pawn` | `backward_pawn_front_square` | Name each pawn behind friendly adjacent pawns whose front square is controlled by an enemy pawn. | No backward-pawn front square is emitted. | No backward-pawn plan or weakness speech. |
| S210 `plan_hanging_pawns` | `hanging_pawn_pair` | Name adjacent friendly pawns on the same rank with no friendly pawn behind either pawn on adjacent files. | No hanging-pawn pair is emitted. | No hanging-pawns plan or weakness speech. |
| S211 `plan_chain_base` | `pawn_chain_base` | Name the rear pawn of each diagonal friendly pawn chain and enemy attacks on that base square. | No pawn-chain base is emitted. | No chain-base plan speech. |
| S212 `plan_passer_make` | `candidate_passer_lever` | Name each pawn whose legal push or capture would remove the last enemy pawn blocker on its file or adjacent files. | No candidate_passer lever is emitted. | No passer-making plan speech. |
| S213 `plan_passer_block` | `passer_block_move` | Name each enemy passed pawn and each legal move by the other side to occupy the square directly in front of it. | No passer block move is emitted. | No passer-block plan speech. |
| S218 `plan_overload` | `dual_target_defender` | Name a defender that guards two target squares attacked by the other side, with the two targets named. | No dual-target defender is emitted. | No overload-plan or tactic speech. |

## Re-Entry Law

The inactive old slots remain `0/silent`. They cannot regain meaning under their
old names. A smaller BoardMood fact may enter only if its name is exactly one of
the table entries above and its value names concrete current-board chess
identity.

Every allowed fact has the same boundary:

- It names side, piece, pawn, square, file, rank, legal move, attack, defender,
  blocker, pin to king, or promotion path.
- It is derived from the current board without engine evaluation or best-move
  judgment.
- Its zero value means silent, absent, or not proven by the direct observation.
- It does not say that a tactic works.
- It does not say that a plan exists, is good, or should be played.
- Public chess speech still belongs to Story after Story binds side, target,
  anchor, route, rival, and proof scores.

## Self-Review

False positive risks checked in this law:

- Count-to-verdict drift: material, activity, king heat, and pawn support can
  sound like evaluations. The replacement facts name only board inventory,
  attacks, guards, legal moves, and blockers.
- Motif-to-tactic drift: x-ray, overload, remove-guard, back-rank, promotion,
  and capture rows can sound like working tactics. The replacement facts stop at
  named lines, targets, guards, captures, and promotion steps.
- Affordance-to-plan drift: `plan_*` rows can sound like Story plans. The plan
  family is explicitly limited to small affordance facts and bans public plan
  speech from BoardMood.
