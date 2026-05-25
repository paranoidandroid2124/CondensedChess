# Strategic Claim Authority Source Window Review

rows=21
Acceptance rule: only verdict=admit_authority_row is source acceptance; release on rejected rows is diagnostic materialization.

- source-botvinnik-vidmar-1936-iqp-opening-inducement: scanned=1 diagnostics=admit_ready=1 blockers=none
  best=ply=16 verdict=admit_authority_row release=SupportedLocal source=iqp_inducement_probe scope=MoveLocal contract=subplan:iqp_inducement contractStatus=Releasable failures=- ownerFailures=- engine=near_top_multipv_contains_played_top=d5c4_gap=5cp primary=This sequence leaves an isolated pawn as the local target.
- source-evans-opsahl-1950: scanned=19 diagnostics=admit_ready=3, engine_top_move_disagrees=4, root_vocabulary_or_extraction_gap=12 blockers=engine:source_move_absent_from_multipv=4, owner:carlsbad_probe_missing=16
  best=ply=37 verdict=admit_authority_row release=SupportedLocal source=iqp_inducement_probe scope=MoveLocal contract=subplan:iqp_inducement contractStatus=Releasable failures=- ownerFailures=- engine=top_pv_matches_played primary=This sequence leaves an isolated pawn as the local target.
- source-salov-ljubojevic-1992-simplification-window: scanned=1 diagnostics=root_vocabulary_or_extraction_gap=1 blockers=owner:root_vocabulary_or_extraction_gap=1
  best=ply=71 verdict=reject_owner_missing release=- source=- scope=- contract=- contractStatus=- failures=- ownerFailures=- engine=top_pv_matches_played primary=-
