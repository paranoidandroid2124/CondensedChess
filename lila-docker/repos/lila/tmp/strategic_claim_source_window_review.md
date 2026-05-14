# Strategic Claim Authority Source Window Review

rows=76

- source-alekhine-bogoljubow-1936-iqp-inducement: scanned=1 diagnostics=admit_ready=1 blockers=none
  best=ply=20 verdict=admit_authority_row release=SupportedLocal source=iqp_inducement_probe scope=MoveLocal contract=subplan:iqp_inducement contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=A local reading is that this sequence leaves an isolated pawn as the local target.
- source-aronian-andreikin-2014-defender-trade: scanned=1 diagnostics=admit_ready=1 blockers=none
  best=ply=33 verdict=admit_authority_row release=SupportedLocal source=defender_trade scope=MoveLocal contract=subplan:defender_trade contractStatus=Releasable failures=- ownerFailures=- engine=near_top_multipv_contains_played_top=c2b1_gap=15cp primary=A local reading is that this exchange removes a defender on the local branch.
- source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation: scanned=1 diagnostics=admit_ready=1 blockers=none
  best=ply=19 verdict=admit_authority_row release=CertifiedOwner source=exact_target_fixation scope=MoveLocal contract=subplan:static_weakness_fixation contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=This changes the position by fixing d6 as the target.
- source-botvinnik-vidmar-1936: scanned=15 diagnostics=engine_multipv_only_source_move=1, engine_top_move_disagrees=3, root_vocabulary_or_extraction_gap=11 blockers=engine:source_move_absent_from_multipv=3, engine:source_move_multipv_only=1, owner:iqp_not_induced=15
  best=ply=20 verdict=reject_owner_missing release=- source=- scope=- contract=- contractStatus=- failures=- ownerFailures=iqp:not_induced engine=near_top_multipv_contains_played_top=a7a6_gap=6cp primary=-
- source-botvinnik-vidmar-1936-iqp-multipv-screen: scanned=1 diagnostics=root_vocabulary_or_extraction_gap=1 blockers=owner:iqp_not_induced=1
  best=ply=31 verdict=reject_owner_missing release=- source=- scope=- contract=- contractStatus=- failures=- ownerFailures=iqp:not_induced engine=near_top_multipv_contains_played_top=b3a4_gap=6cp primary=-
- source-botvinnik-vidmar-1936-iqp-opening-inducement: scanned=1 diagnostics=admit_ready=1 blockers=none
  best=ply=16 verdict=admit_authority_row release=SupportedLocal source=iqp_inducement_probe scope=MoveLocal contract=subplan:iqp_inducement contractStatus=Releasable failures=- ownerFailures=- engine=near_top_multipv_contains_played_top=h7h6_gap=4cp primary=A local reading is that this sequence leaves an isolated pawn as the local target.
- source-capablanca-golombek-1939: scanned=15 diagnostics=admit_ready=1, engine_top_move_disagrees=4, root_vocabulary_or_extraction_gap=10 blockers=engine:source_move_absent_from_multipv=4, owner:carlsbad_probe_missing=14
  best=ply=45 verdict=admit_authority_row release=SupportedLocal source=iqp_inducement_probe scope=MoveLocal contract=subplan:iqp_inducement contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=A local reading is that this sequence leaves an isolated pawn as the local target.
- source-capablanca-golombek-1939-iqp-inducement: scanned=1 diagnostics=admit_ready=1 blockers=none
  best=ply=45 verdict=admit_authority_row release=SupportedLocal source=iqp_inducement_probe scope=MoveLocal contract=subplan:iqp_inducement contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=A local reading is that this sequence leaves an isolated pawn as the local target.
- source-carlsen-anand-2014-g6: scanned=4 diagnostics=admit_ready=1, root_vocabulary_or_extraction_gap=3 blockers=owner:root_vocabulary_or_extraction_gap=3
  best=ply=15 verdict=admit_authority_row release=SupportedLocal source=queen_trade_shield scope=MoveLocal contract=subplan:queen_trade_shield contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=A local reading is that this exchange moves the game into the queenless branch.
- source-evans-opsahl-1950: scanned=19 diagnostics=admit_ready=3, root_vocabulary_or_extraction_gap=16 blockers=owner:carlsbad_probe_missing=16
  best=ply=25 verdict=admit_authority_row release=CertifiedOwner source=carlsbad_fixed_target_probe scope=PositionLocal contract=subplan:backward_pawn_targeting contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=The key strategic fact here is that c6 is the fixed target.
- source-evans-opsahl-1950-iqp-inducement: scanned=1 diagnostics=admit_ready=1 blockers=none
  best=ply=33 verdict=admit_authority_row release=SupportedLocal source=iqp_inducement_probe scope=MoveLocal contract=subplan:iqp_inducement contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=A local reading is that this sequence leaves an isolated pawn as the local target.
- source-kramnik-anand-2001: scanned=6 diagnostics=tactical_first_source=6 blockers=tactical:first=6
  best=ply=27 verdict=reject_tactical_first release=- source=- scope=- contract=- contractStatus=- failures=- ownerFailures=- engine=top_pv_matches_played primary=-
- source-kramnik-anand-2001-iqp-opening-inducement: scanned=1 diagnostics=engine_top_move_disagrees=1 blockers=engine:source_move_absent_from_multipv=1, owner:iqp_not_induced=1
  best=ply=14 verdict=reject_owner_missing release=- source=- scope=- contract=- contractStatus=- failures=- ownerFailures=iqp:not_induced engine=pv_available_top_differs:b7b5 primary=-
- source-maderna-palermo-1955-static-weakness-fixation: scanned=1 diagnostics=root_vocabulary_or_extraction_gap=1 blockers=owner:root_vocabulary_or_extraction_gap=1
  best=ply=17 verdict=reject_owner_missing release=- source=- scope=- contract=- contractStatus=- failures=- ownerFailures=- engine=top_pv_matches_played primary=The move has to happen now because otherwise f3d2 is demanded immediately.
- source-najdorf-sergeant-1939-iqp-inducement: scanned=1 diagnostics=admit_ready=1 blockers=none
  best=ply=23 verdict=admit_authority_row release=SupportedLocal source=iqp_inducement_probe scope=MoveLocal contract=subplan:iqp_inducement contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=A local reading is that this sequence leaves an isolated pawn as the local target.
- source-salov-ljubojevic-1992-simplification-window: scanned=1 diagnostics=admit_ready=1 blockers=none
  best=ply=71 verdict=admit_authority_row release=CertifiedOwner source=simplification_window scope=MoveLocal contract=subplan:simplification_window contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=This trade keeps the same local edge on d5.
- source-tartakower-capablanca-1924: scanned=6 diagnostics=tactical_first_source=6 blockers=tactical:first=6
  best=ply=17 verdict=reject_tactical_first release=- source=- scope=- contract=- contractStatus=- failures=- ownerFailures=- engine=top_pv_matches_played primary=-
