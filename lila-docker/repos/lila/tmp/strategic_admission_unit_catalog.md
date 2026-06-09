# Strategic Admission Unit Catalog

This is the repeatable proof shape for future strategic admission-unit work.
Each pass is 1 plan kind, 1 proof source, 1 controlled positive, 3 negative
controls, 2-5 natural source candidates, and 0-2 authority rows.

Authority defaults:

- CertifiedOwner requires PV1 source-move agreement.
- SupportedLocal may use near-top MultiPV only when exact proof packet,
  planner authority, tactical veto, and claim-only surfaces all pass.
- Tactical-first remains absolute over strategic prose.
- Runtime contracts must not contain source witness ids.

| plan kind | review group | proof source | proof family | surface authority | source candidates | max authority rows | contract status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `static_weakness_fixation` | `B:static_weakness_fixation` | `exact_target_fixation` | `static_weakness_fixation` | `CertifiedOwner+SupportedLocal` | `2-5` | `2` | `Releasable` |
| `backward_pawn_targeting` | `B:carlsbad_fixed_target` | `carlsbad_fixed_target_probe` | `backward_pawn_targeting` | `CertifiedOwner+SupportedLocal` | `2-5` | `2` | `Releasable` |
| `break_prevention` | `A:break_prevention` | `counterplay_axis_suppression` | `neutralize_key_break` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `prophylaxis_restraint` | `A:prophylaxis_restraint` | `prophylactic_move` | `counterplay_restraint` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `open_file_pressure` | `A:open_file_pressure` | `local_file_entry_bind` | `half_open_file_pressure` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `key_square_denial` | `A:key_square_denial` | `local_file_entry_bind` | `half_open_file_pressure` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `rook_file_transfer` | `A:rook_file_transfer` | `local_file_entry_bind` | `half_open_file_pressure` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `flank_clamp` | `A:flank_clamp` | `color_complex_squeeze_probe` | `color_complex_squeeze` | `CertifiedOwner+SupportedLocal` | `2-5` | `2` | `Releasable` |
| `outpost_entrenchment` | `A:outpost_entrenchment` | `outpost_entrenchment` | `outpost_entrenchment` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `iqp_inducement` | `C:iqp_inducement` | `iqp_inducement_probe` | `iqp_inducement` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `simplification_window` | `C:simplification_window` | `simplification_window` | `simplification_window` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `defender_trade` | `C:defender_trade` | `defender_trade` | `defender_trade` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `queen_trade_shield` | `C:queen_trade_boundary` | `queen_trade_shield` | `queen_trade_shield` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `bad_piece_liquidation` | `C:bad_piece_liquidation` | `bad_piece_liquidation` | `bad_piece_liquidation` | `SupportedLocal` | `2-5` | `2` | `Releasable` |
| `central_break_timing` | `A:central_break_timing` | `central_break_timing` | `central_break_timing` | `SupportedLocal` | `2-5` | `2` | `Releasable` |

Documentation targets:

- `tmp/strategic_claim_source_witnesses.md`
- `modules/commentary/docs/CommentaryTrustBoundary.md`
- `tmp/strategic_claim_authority_surface_review.md`
