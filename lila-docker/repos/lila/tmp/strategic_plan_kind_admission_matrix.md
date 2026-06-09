# Strategic Plan Kind Admission Matrix

Snapshot date: 2026-05-17

Scope: all 35 `PlanTaxonomy.PlanKind` values on the current local `master`
worktree. This is a tooling-only coverage snapshot. It does not widen runtime
release, does not add runtime gates, and does not use source ids as runtime
policy.

Basis:

- `modules/commentary/docs/CommentaryProgramMap.md`
- `modules/commentary/docs/CommentaryPipelineSSOT.md`
- `modules/commentary/docs/CommentaryTrustBoundary.md`
- `modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlanTaxonomy.scala`
- `modules/commentaryCore/src/main/scala/lila/commentary/analysis/claim/ProofContractRules.scala`
- current source/authority snapshots under `tmp/strategic_claim_*`

Priority legend:

- `P0_open`: already has a live exact owner path; maintain or add fixed source
  rows only through the existing proof stack.
- `P1_next`: best next admission candidate because a runtime contract or proof
  catalog already exists.
- `P2_probe`: plausible only after a centralized witness/materializer is built
  or generalized.
- `P3_defer`: keep deferred until adjacent safer families settle.
- `P4_later`: support-only or domain-support work, not current admission.
- `P5_last`: highest-risk abstract/attack/endgame families; keep closed.
- `X_tactical`: tactical-first/backend-only, not strategic owner admission.

## Summary

| bucket | count | rows |
| --- | ---: | --- |
| Direct PlanKind `Releasable` contracts | 9 | `static_weakness_fixation`, `backward_pawn_targeting`, `iqp_inducement`, `central_break_timing`, `simplification_window`, `defender_trade`, `queen_trade_shield`, `bad_piece_liquidation`, `outpost_entrenchment` |
| PlanKind `BackendOnly` tactical contracts | 4 | `forcing_tactical_shot`, `defender_overload`, `clearance_break`, `battery_pressure` |
| PlanKind `Deferred` contracts | 22 | all remaining subplans |
| Deferred plan kinds with a separate runtime proof-contract route | 5 | `prophylaxis_restraint`, `break_prevention`, `key_square_denial`, `rook_file_transfer`, `open_file_pressure` |
| Deferred PlanKind with public exact runtime source witness | 1 | `flank_clamp -> color_complex_squeeze` via `source-botvinnik-vidmar-1936-flank-clamp` |
| Best immediate next admission units | 3 | `open_file_pressure -> half_open_file_pressure`, `prophylaxis_restraint -> counterplay_restraint`, `outpost_entrenchment` |

## Matrix

| PlanKind | PlanTheme | ProofContract status | Runtime contract | proofFamily / proofSource candidate | Existing carrier can open? | New materializer / witness? | Mode | Priority | Next admission unit |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `opening_development` | `opening_principles` | `Deferred` | none | `opening_relation` / `opening_relation_claim` | partial domain translator only | yes, exact move-linked opening relation proof | deferred/support-only | `P4_later` | none; keep support-only |
| `prophylaxis_restraint` | `restriction_prophylaxis` | `Deferred` | `runtime:counterplay_restraint` | `counterplay_restraint` / `prophylactic_move` | yes, named prevented-resource carrier | natural exact witness/source rows still needed | SupportedLocal target; current natural pass admitted 0 | `P1_next` | `counterplay_restraint` |
| `break_prevention` | `restriction_prophylaxis` | `Deferred` | `runtime:neutralize_key_break` | `neutralize_key_break` / `counterplay_axis_suppression` | yes, `BreakClampMaterializer` plus `BreakPreventionWitness` | no new materializer for clean route clamps | SupportedLocal open only after SourceReview admits the exact source row; tactical-first veto applies | `P0_open` | `neutralize_key_break`; current source fixtures are review-only |
| `key_square_denial` | `restriction_prophylaxis` | `Deferred` | partial remap only | `half_open_file_pressure` or `counterplay_restraint` / `local_file_entry_bind` or `prophylactic_move` | partial, only after exact file-entry or named-resource certification | yes for standalone key-square owner | public SupportedLocal only after typed remap into an existing runtime proof contract | `P2_probe` | `local_file_entry_bind` |
| `outpost_entrenchment` | `piece_redeployment` | `Releasable` | `subplan:outpost_entrenchment` | `outpost_entrenchment` / `outpost_entrenchment` | yes, exact outpost occupation row is live | no for current exact slice; source rows still need branch proof and stable persistence | SupportedLocal open only with typed outpost owner proof; not CertifiedOwner | `P1_next` | `outpost_entrenchment`; no admitted source fixture yet |
| `worst_piece_improvement` | `piece_redeployment` | `Deferred` | none | `worst_piece_improvement` / `worst_piece_improvement` | no | yes, exact before/after piece-quality witness | deferred/support-only | `P4_later` | `piece_redeployment_quality` |
| `rook_file_transfer` | `piece_redeployment` | `Deferred` | `runtime:half_open_file_pressure` when converted to file-entry proof | `half_open_file_pressure` / `local_file_entry_bind` or `rook_file_transfer` | partial through existing file-entry lane | yes for pure rook-transfer ownership | public SupportedLocal only through `half_open_file_pressure` / `local_file_entry_bind` | `P2_probe` | `open_file_pressure` |
| `bishop_reanchor` | `piece_redeployment` | `Deferred` | none | `bishop_reanchor` / `bishop_reanchor` | no | yes, exact bishop route and persistence witness | deferred/support-only | `P4_later` | `piece_redeployment_bishop_reanchor` |
| `open_file_pressure` | `piece_redeployment` | `Deferred` | `runtime:half_open_file_pressure` | `half_open_file_pressure` / `local_file_entry_bind` or `open_file_pressure` | yes, bounded B4 file-plus-entry lane | source expansion only inside certified file-entry pair | public SupportedLocal only after typed file-entry proof | `P1_next` | `half_open_file_pressure` |
| `flank_clamp` | `space_clamp` | `Deferred` | `runtime:color_complex_squeeze` | `color_complex_squeeze` / `color_complex_squeeze_probe` | yes, exact Botvinnik-Vidmar source row now projects through the typed color-complex proof | no for the runtime color-complex slice; standalone flank clamp still needs release-risk proof | CertifiedOwner open only through the color-complex runtime proof contract | `P0_open` | `color_complex_squeeze`; `source-botvinnik-vidmar-1936-flank-clamp` is public, broad flank-clamp remains closed |
| `central_space_bind` | `space_clamp` | `Deferred` | none | `central_space_bind` / `central_space_bind` | no | yes, route-chain/space-bind materializer | deferred/support-only | `P4_later` | route-network bind after B6 |
| `mobility_suppression` | `space_clamp` | `Deferred` | none | `mobility_suppression` / `mobility_suppression` | no | yes, mobility-cage and escape-resource falsification | deferred/support-only | `P5_last` | bounded color-complex / mobility |
| `static_weakness_fixation` | `weakness_fixation` | `Releasable` | `subplan:static_weakness_fixation` | `static_weakness_fixation` / `exact_target_fixation` | yes | no for current exact slice | CertifiedOwner open only after owner and tactical gates pass | `P0_open` | `static_weakness_fixation`; current B21/Boleslavsky rows fail closed |
| `minority_attack_fixation` | `weakness_fixation` | `Deferred` | no separate live slice | `minority_attack_fixation` / `carlsbad_fixed_target_probe` | partial, absorbed under Carlsbad target proof | not recommended until distinct exact owner survives | support-only/absorbed under Carlsbad/backward-pawn targeting | `P3_defer` | `backward_pawn_targeting` |
| `backward_pawn_targeting` | `weakness_fixation` | `Releasable` | `subplan:backward_pawn_targeting` | `backward_pawn_targeting` / `carlsbad_fixed_target_probe` | yes | no for current Carlsbad lane | CertifiedOwner open only after position-probe and owner gates pass | `P0_open` | `backward_pawn_targeting`; current Evans source remains non-contract MoveDelta evidence |
| `iqp_inducement` | `weakness_fixation` | `Releasable` | `subplan:iqp_inducement` | `iqp_inducement` / `iqp_inducement_probe` | yes | no for current SupportedLocal lane | SupportedLocal open | `P0_open` | `iqp_inducement` |
| `central_break_timing` | `pawn_break_preparation` | `Releasable` | `subplan:central_break_timing` | `central_break_timing` / `central_break_timing` | partial, contract path exists but current source row is witness-missing | exact central-break timing source witness still needed before public source admission | review-only for current source fixture; SupportedLocal open only after typed witness admission | `P1_next` | `central_break_timing` |
| `wing_break_timing` | `pawn_break_preparation` | `Deferred` | none | `wing_break_timing` / `wing_break_timing` | no | yes, flank-break transform/release witness | deferred/support-only | `P3_defer` | `wing_break_timing` |
| `tension_maintenance` | `pawn_break_preparation` | `Deferred` | none | `tension_maintenance` / `tension_maintenance` | no | yes, keep-tension and conversion/rival arbitration | deferred/support-only | `P4_later` | `tension_maintenance` |
| `simplification_window` | `favorable_exchange` | `Releasable` | `subplan:simplification_window` | `simplification_window` / `simplification_window` plus transformation sources | yes | no for current same-task simplification | SupportedLocal open for current same-task public rows | `P0_open` | `simplification_window`; Botvinnik-Vidmar source row is public, while source Salov-Ljubojevic remains owner-missing |
| `defender_trade` | `favorable_exchange` | `Releasable` | `subplan:defender_trade`; `runtime:trade_key_defender` remains blocked | `defender_trade` / `defender_trade` or `exchange_forcing_delta` | yes for SupportedLocal | no for SupportedLocal; yes for future CertifiedOwner defender-trade | SupportedLocal open only with typed defender-trade owner proof | `P0_open` | `defender_trade`; Aronian-Andreikin remains owner-missing |
| `queen_trade_shield` | `favorable_exchange` | `Releasable` | `subplan:queen_trade_shield` | `queen_trade_shield` / `queen_trade_shield` | yes | no for current exact slice | SupportedLocal open | `P0_open` | `queen_trade_shield` |
| `bad_piece_liquidation` | `favorable_exchange` | `Releasable` | `subplan:bad_piece_liquidation` | `bad_piece_liquidation` / `bad_piece_liquidation` | partial, contract path exists but current pilot source row is witness-missing | exact bad-piece before/after source witness still needed before public source admission | review-only for current pilot fixture; SupportedLocal open only after typed witness admission | `P1_next` | `bad_piece_liquidation`; pilot source row is deferred |
| `rook_pawn_march` | `flank_infrastructure` | `Deferred` | none | `rook_pawn_march` / `rook_pawn_march` | no | yes, flank-space/progress witness | deferred/support-only | `P4_later` | `rook_pawn_march` |
| `hook_creation` | `flank_infrastructure` | `Deferred` | none | `hook_creation` / `hook_creation` | no | yes, hook and file-opening witness | deferred/support-only | `P5_last` | `hook_creation` |
| `rook_lift_scaffold` | `flank_infrastructure` | `Deferred` | none | `rook_lift_scaffold` / `rook_lift_scaffold` | no | yes, rook-lift route and threat arbitration | deferred/support-only | `P5_last` | `rook_lift_scaffold` |
| `simplification_conversion` | `advantage_transformation` | `Deferred` | none | `simplification_conversion` / `simplification_conversion` | partial truth-role support only | yes, distinct conversion handoff witness | deferred/support-only | `P3_defer` | simplification window then conversion |
| `passer_conversion` | `advantage_transformation` | `Deferred` | none | `passer_conversion` / `passer_conversion` | no | yes, passer conversion and fortress checks | deferred/support-only | `P4_later` | `passer_conversion` |
| `passed_pawn_manufacture` | `advantage_transformation` | `Deferred` | none | `passed_pawn_manufacture` / `passed_pawn_manufacture` | no | yes, structure-transition best-defense proof | deferred/support-only | `P3_defer` | `passed_pawn_manufacture` |
| `invasion_transition` | `advantage_transformation` | `Deferred` | none | `invasion_transition` / `invasion_transition` | partial domain transition support only | yes, invasion route/square materializer | deferred/support-only | `P4_later` | `invasion_transition` |
| `opposite_bishops_conversion` | `advantage_transformation` | `Deferred` | none | `opposite_bishops_conversion` / `opposite_bishops_conversion` | no | yes, endgame oracle/progress/fortress discrimination | deferred/support-only | `P5_last` | `opposite_bishops_conversion` |
| `forcing_tactical_shot` | `immediate_tactical_gain` | `BackendOnly` | backend-only tactical truth | `forcing_tactical_shot` / `forcing_tactical_shot` | tactical truth only | no strategic materializer | tactical-first | `X_tactical` | none |
| `defender_overload` | `immediate_tactical_gain` | `BackendOnly` | backend-only tactical truth | `defender_overload` / `defender_overload` | tactical motif support only | no strategic materializer | tactical-first | `X_tactical` | none |
| `clearance_break` | `immediate_tactical_gain` | `BackendOnly` | backend-only tactical truth | `clearance_break` / `clearance_break` | tactical motif support only | no strategic materializer | tactical-first | `X_tactical` | none |
| `battery_pressure` | `immediate_tactical_gain` | `BackendOnly` | backend-only tactical truth | `battery_pressure` / `battery_pressure` | tactical motif support only | no strategic materializer | tactical-first | `X_tactical` | none |

## Admission Recommendation

The next structural admission work should stay on existing certified boundaries:

1. `open_file_pressure -> half_open_file_pressure`: reuse the existing
   local-file-entry proof contract and add natural source candidates only after
   exact file-plus-entry witness survives.
2. `outpost_entrenchment`: reuse the existing typed outpost occupation packet
   and add source rows only after branch proof, stable persistence, and
   rival/relabel controls survive.
3. `prophylaxis_restraint -> counterplay_restraint`: runtime carrier exists,
   but the first natural pass admitted 0 rows; refine named-resource witness
   and blockers before another source hunt.

Do not open broad `space_clamp`, `flank_infrastructure`, or
`advantage_transformation` families from this snapshot. Those remain deferred
behind exact-board burden and release-risk taxonomy.
