# BoardMood Cut Law

Prepared for the `BoardMood -> Story -> StoryTable -> Verdict` reset.

This document closes every BoardMood S001..S223 slot classified as `버림`.
These slots are not kept as BoardMood live facts. In this branch they are
always `0`/silent and cannot be promoted into public chess meaning.

If one of these chess ideas is spoken at all, the authority is
`StoryResurrectionLaw.md`, not BoardMood.

## Cut Slots

The complete `버림` list is:

- S013-S014
- S072
- S078
- S088
- S094
- S104
- S111
- S120
- S127
- S131
- S142
- S147
- S158
- S164
- S174-S175
- S180
- S190-S191
- S201
- S214-S217
- S219-S223

Expanded by current slot name:

- S013 `board_hash_lo`
- S014 `board_hash_hi`
- S072 `white_open_file_exposure`
- S078 `white_mate_net_pressure`
- S088 `black_open_file_exposure`
- S094 `black_mate_net_pressure`
- S104 `white_chain_base_count`
- S111 `white_pawn_structure_score`
- S120 `black_chain_base_count`
- S127 `black_pawn_structure_score`
- S131 `white_piece_mobility_score`
- S142 `white_route_clarity`
- S147 `black_piece_mobility_score`
- S158 `black_route_clarity`
- S164 `white_conversion_prize`
- S174 `white_counterplay`
- S175 `white_tactical_score`
- S180 `black_conversion_prize`
- S190 `black_counterplay`
- S191 `black_tactical_score`
- S201 `plan_reroute`
- S214 `plan_race`
- S215 `plan_trade`
- S216 `plan_simplify`
- S217 `plan_keep_pieces`
- S219 `plan_prophy`
- S220 `plan_counterplay`
- S221 `plan_initiative`
- S222 `plan_king_convert`
- S223 `plan_convert`

## Removal Reasons

S013-S014 are hashes, not chess facts. A hash can identify or compare an
encoded position, but it does not say anything public about material, legality,
king safety, pawn structure, threats, or plans. BoardMood must not expose hash
content as chess meaning, so both slots stay `0`/silent.

S078 and S094 are mate-net pressure values. "Pressure" is an interpretation, not
a direct board observation. If BoardMood guessed it from nearby signals, it
would create false positives around legal escapes, quiet defenses, and non-mate
attacks. Only smaller observable facts such as king ring squares, enemy attacks,
safe escapes, contact checks, and legal check counts may survive elsewhere.

S072 and S088 are open-file exposure values. Exposure is a king-safety or
targetability claim, not a file observation. BoardMood may name open files,
semi-open files, file blockers, target squares, and legal rook entry moves, but
it must not decide that a side is exposed on that file.

S104 and S120 name chain-base counts, but the label is too interpretive for this
layer. A pawn can be counted by file, isolation, doubling, passed status, fixed
status, levers, support, risk, or blockade through smaller facts. Declaring a
"base" is a structural reading that depends on which chain matters to a story,
so these slots stay absent.

S111 and S127 are pawn-structure scores. Scores compress many judgments into a
single value and hide their cause. They are not safe board observations and
would invite false positives when a weak pawn feature is balanced by a separate
strength. BoardMood may carry small pawn facts, not a pawn verdict.

S131 and S147 are piece-mobility scores. BoardMood may carry counts such as
piece mobility, safe mobility, loose pieces, hanging pieces, pins, overloaded
pieces, trapped pieces, and x-ray targets. A combined score is an evaluation
choice, not a fact, so it cannot become public chess meaning.

S142 and S158 are route-clarity values. Route clarity depends on a selected
piece, destination, path, timing, and opponent response. Without those concrete
anchors, BoardMood would be inventing a plan-like reading from a static board.
The slots stay `0`/silent.

S164 and S180 are conversion-prize values. "Conversion" says a side can turn an
edge into a result or durable gain. That requires proof beyond static board
facts and must not be manufactured by BoardMood. Observable tactical or material
facts can remain elsewhere; the prize label is removed.

S174 and S190 are counterplay values. Counterplay is a narrative judgment about
whether the worse or defending side has practical chances. BoardMood can expose
checks, captures, threats, mobility, and pawn breaks as small facts, but it must
not label those facts as counterplay.

S175 and S191 are tactical scores. Tactics must be represented only by narrow
observable counts or proof-backed story facts. A tactical score blends forcing
moves, threats, captures, motifs, and conversion assumptions into one value,
which is too broad and too easy to overstate.

S201 is `plan_reroute`. Rerouting is not a board fact unless a specific piece,
route, and destination are bound to a story. BoardMood does not create that
story from a scalar flag, so this slot stays `0`/silent.

S214-S217 are `plan_race`, `plan_trade`, `plan_simplify`, and
`plan_keep_pieces`. These are plan choices, not board observations. They require
move intent, legal continuation, and purpose. BoardMood may carry material,
mobility, king, pawn, and threat facts that make such a story possible, but it
must not announce the plan itself.

S219-S223 are `plan_prophy`, `plan_counterplay`, `plan_initiative`,
`plan_king_convert`, and `plan_convert`. These names are especially dangerous
because they sound like public chess claims. Prophylaxis, initiative,
counterplay, king conversion, and conversion require a concrete story and proof.
As BoardMood scalars they would be broad guesses, so all five are cut.

Proof, source, and engine-related meanings are not created by BoardMood. If a
claim needs proof, provenance, or engine support, BoardMood cannot substitute a
scalar for it. In this branch a missing proof-bearing value is simply absent:
the corresponding cut slot remains `0`/silent.

## Why This Does Not Create Holes

Cutting these slots does not remove any required public chess meaning. Values
that can safely survive already enter through `살림` slots or through `쪼갬`
slots as smaller observations. If a value cannot be expressed that way, this
branch treats it as absent rather than guessing.

The rule is deliberately fail-closed:

- A hash is not chess meaning.
- Heat, score, pressure, completion, conversion, and counterplay labels are not
  direct observations.
- Plan labels are not BoardMood facts.
- Proof, source, and engine meanings cannot be invented here.

Therefore each `버림` slot is closed as `0`/silent, not left as a placeholder.

## Self-Review

This document blocks keeping a slot because it sounds useful, because someone
might want it, or because a single number would be convenient. The only allowed
survival path is current, small, board-observable meaning in `살림` or `쪼갬`.

Review questions and answers:

- Does any listed `버림` slot remain eligible for public chess meaning? No.
- Does any slot stay as a placeholder for a missing producer? No. It is
  `0`/silent.
- Does any score-like or pressure-like slot remain because it might be useful?
  No. Scores and pressure labels are removed unless reduced to smaller
  observations elsewhere.
- Does BoardMood create proof, source, or engine-backed meaning? No.
- If a desired meaning cannot be represented by an existing `살림` or `쪼갬`
  observation, what happens? It is absent.
