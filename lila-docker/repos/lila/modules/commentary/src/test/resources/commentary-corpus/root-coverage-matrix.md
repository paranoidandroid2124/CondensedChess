# Root Coverage Matrix

`symmetry-covered?` means the schema has at least one corpus row, so the symmetry suite hits it through the full-vector mirror check.
`input-fail-covered?` reflects the strict `RootExtractor.fromFenFailClosed` boundary rather than permissive synthetic corpus extraction.

| schema | exact present? | near_miss present? | nasty_negative present? | symmetry-covered? | input-fail-covered? | notes |
| --- | --- | --- | --- | --- | --- | --- |
| `piece_on` | yes | no | no | yes | yes | seed exact only |
| `controlled_by` | yes | no | no | yes | yes | seed exact only |
| `pawn_controlled_by` | yes | yes | no | yes | yes | seed exact + near_miss |
| `contested` | yes | yes | no | yes | yes | seed exact + blocker near_miss |
| `open_file` | yes | no | no | yes | yes | seed exact only |
| `half_open_file` | yes | yes | no | yes | yes | seed exact + near_miss |
| `king_ring_square` | yes | no | no | yes | yes | seed exact only |
| `weak_square` | yes | yes | no | yes | yes | seed exact + near_miss |
| `outpost_square` | yes | yes | yes | yes | yes | edge-file exact; no-support and blocked/challenge negatives |
| `isolated_pawn` | yes | yes | no | yes | yes | seed exact + near_miss |
| `backward_pawn` | yes | yes | no | yes | yes | seed exact + blockade exact + near_miss |
| `doubled_file` | yes | yes | no | yes | yes | seed exact + near_miss |
| `passed_pawn` | yes | yes | no | yes | yes | seed exact + near_miss + h7 promotion-edge exact |
| `candidate_passer` | yes | yes | yes | yes | yes | d4 seed + same-rank support exact + a-file balance exact; passed-vs-candidate nasty |
| `fixed_pawn` | yes | no | no | yes | yes | seed exact only |
| `loose_piece` | yes | yes | no | yes | yes | seed exact + defended-queen exact + near_miss |
| `pinned_piece` | yes | yes | no | yes | yes | absolute exact + relative exact + near_miss |
| `overloaded_piece` | yes | yes | no | yes | yes | seed exact + near_miss |
| `trapped_piece` | yes | yes | yes | yes | yes | a2 exact + zero-legal-move exact; one-exit near_miss; king/pawn negatives |
| `xray_target` | yes | yes | no | yes | yes | seed exact + near_miss |
| `lever_available` | yes | no | no | yes | yes | seed exact only |
| `king_shelter_hole` | yes | yes | yes | yes | yes | polarity exact; no-access near_miss; out-of-regime nasty |
| `side_to_move` | yes | no | no | yes | yes | seed white-to-move exact |
| `castling_rights` | yes | no | no | yes | yes | KQkq seed + K-only asymmetry exact |
| `en_passant_state` | yes | no | no | yes | yes | d-file capture exact; h-file capture exact; legal none with raw ep square |
